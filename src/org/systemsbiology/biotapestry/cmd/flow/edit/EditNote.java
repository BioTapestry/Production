/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
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
  
  public EditNote(BTState appState, boolean forEdit) {
    super(appState);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    EditNoteState retval = new EditNoteState(appState_, dacx);
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
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
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
        
  public static class EditNoteState implements DialogAndInProcessCmd.PopupCmdState {
     
    private Note noteToEdit;
    private NoteProperties noteProps;
    private DataAccessContext dacx_;
    
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditNoteState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepGetNoteEditDialog";
      dacx_ = dacx;
    }
  
    /***************************************************************************
    **
    ** Preload init
    */ 
      
    public void setIntersection(Intersection intersect) {
      String noteID = intersect.getObjectID();
      this.noteToEdit = dacx_.getGenome().getNote(noteID);
      this.noteProps = dacx_.getLayout().getNoteProperties(noteID);
    } 
    
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetNoteEditDialog() {    
      NotePropertiesDialogFactory.NotePropBuildArgs ba = new NotePropertiesDialogFactory.NotePropBuildArgs(noteToEdit, noteProps);
      NotePropertiesDialogFactory npdd = new NotePropertiesDialogFactory(cfh);
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
   
      UndoSupport support = new UndoSupport(appState_, "undo.noteEdit");
      GenomeChange gc = dacx_.getGenome().changeNote(noteToEdit, crq.nameResult, crq.textResult, crq.interactiveResult);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }

      NoteProperties newProps = noteProps.clone();
      newProps.setColor(crq.colorResult);
      newProps.setFontOverride(crq.fontOverrideResult);
      newProps.setJustification(crq.justResult);
      
      Layout.PropChange lpc = dacx_.getLayout().replaceNoteProperties(noteToEdit.getID(), newProps);
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      }
      support.finish();
      
      //
      // DONE!
      //
      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }  
}
