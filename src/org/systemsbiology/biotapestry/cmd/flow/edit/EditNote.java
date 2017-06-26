/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


package org.systemsbiology.biotapestry.cmd.flow.edit;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NotePropertiesDialogFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Editing Notes
*/

public class EditNote extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditNote(boolean forEdit) {
    name =  (forEdit) ? "notePopup.Edit" : "command.AddNote";
    desc = (forEdit) ? "notePopup.Edit" : "command.AddNote";
    icon = "DrawNewNote24.gif";
    mnem =  (forEdit) ? "notePopup.EditMnem" : "command.AddNoteMnem";  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    EditNoteState retval = new EditNoteState(dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalStateException();
      } else {
        EditNoteState ans = (EditNoteState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepGetNoteEditDialog")) {
          next = ans.stepGetNoteEditDialog();
        } else if (ans.getNextStep().equals("stepExtractAndInstallNoteProps")) {
          next = ans.stepExtractAndInstallNoteProps(last);      
        } else {
          throw new IllegalStateException();
        }
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class EditNoteState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
     
    private Note noteToEdit;
    private NoteProperties noteProps;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditNoteState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepGetNoteEditDialog";
    }
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditNoteState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepGetNoteEditDialog";
    }
  
    /***************************************************************************
    **
    ** Preload init
    */ 
      
    public void setIntersection(Intersection intersect) {
      String noteID = intersect.getObjectID();
      this.noteToEdit = dacx_.getCurrentGenome().getNote(noteID);
      this.noteProps = dacx_.getCurrentLayout().getNoteProperties(noteID);
    } 
    
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetNoteEditDialog() {    
      NotePropertiesDialogFactory.NotePropBuildArgs ba = new NotePropertiesDialogFactory.NotePropBuildArgs(noteToEdit, noteProps);
      NotePropertiesDialogFactory npdd = new NotePropertiesDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = npdd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractAndInstallNoteProps";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract and install note properties
    */ 
       
    private DialogAndInProcessCmd stepExtractAndInstallNoteProps(DialogAndInProcessCmd cmd) {
         
      NotePropertiesDialogFactory.NoteRequest crq = (NotePropertiesDialogFactory.NoteRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
   
      UndoSupport support = uFac_.provideUndoSupport("undo.noteEdit", dacx_);
      GenomeChange gc = dacx_.getCurrentGenome().changeNote(noteToEdit, crq.nameResult, crq.textResult, crq.interactiveResult);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
        support.addEdit(gcc);
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }

      NoteProperties newProps = noteProps.clone();
      newProps.setColor(crq.colorResult);
      newProps.setFontOverride(crq.fontOverrideResult);
      newProps.setJustification(crq.justResult);
      
      Layout.PropChange lpc = dacx_.getCurrentLayout().replaceNoteProperties(noteToEdit.getID(), newProps);
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      }
      support.finish();
      
      //
      // DONE!
      //
      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }  
}
