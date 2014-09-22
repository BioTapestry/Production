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

package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Color;
import java.awt.Point;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NotePropertiesDialogFactory;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Adding nodes
*/

public class AddNote extends AbstractControlFlow {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNote(BTState appState) {
    super(appState);
    name =  "command.AddNote";
    desc = "command.AddNote";
    icon = "DrawNewNote24.gif";
    mnem =  "command.AddNoteMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
    
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.genomeNotNull());
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
        AddNoteState ans = new AddNoteState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepGetNoteCreationDialog();
      } else {
        AddNoteState ans = (AddNoteState)last.currStateX;
        if (ans.getNextStep().equals("stepExtractNewNoteInfo")) {
          next = ans.stepExtractNewNoteInfo(last);
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();      
        } else if (ans.getNextStep().equals("stepPlaceNewNote")) {   
          next = ans.stepPlaceNewNote();          
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
  ** Called as new X, Y info comes in to place the node.
  */
  
  @Override
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    AddNoteState ans = (AddNoteState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = "stepPlaceNewNote"; 
    return (retval);
  }

  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class AddNoteState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Note newNote;
    private String newNoteColor;   
    private FontManager.FontOverride newNoteFont;
    private int justification;
    private ServerControlFlowHarness cfh;
    private int x;
    private int y; 
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext dacx_;
    
    public String getNextStep() {
      return (nextStep_);
    }  
    
    /***************************************************************************
    **
    ** Constructor
    */ 
    
    public AddNoteState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      dacx_ = dacx;
    }
      
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (false);
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      return (false);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.OBJECT_FLOATER);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      String noteID = newNote.getID();
      NoteProperties nprop = new NoteProperties(appState_, flay, noteID, newNoteColor, 0.0, 0.0);
      if (newNoteFont != null) {
        nprop.setFontOverride(newNoteFont);
      }
      nprop.setJustification(justification);
      flay.setNoteProperties(noteID, nprop);
      return;
    }
  
    public Object getFloater(int x, int y) {
      return (newNote);
    }
    
    public Color getFloaterColor() {
      return (null); // This handler uses setFloaterProps, not this
    }
    
    /***************************************************************************
    **
    ** Get user info to create new note
    */ 
      
    private DialogAndInProcessCmd stepGetNoteCreationDialog() {    
      NotePropertiesDialogFactory.NotePropBuildArgs ba = new NotePropertiesDialogFactory.NotePropBuildArgs(null, null);
      NotePropertiesDialogFactory npdd = new NotePropertiesDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = npdd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractNewNoteInfo";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract new note info from the creation dialog
    */ 
       
    private DialogAndInProcessCmd stepExtractNewNoteInfo(DialogAndInProcessCmd cmd) {
         
      NotePropertiesDialogFactory.NoteRequest crq = (NotePropertiesDialogFactory.NoteRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }      
 
      String noteID = dacx_.getGenome().getNextNoteKey();
      newNote = new Note(noteID, crq.nameResult, crq.interactiveResult);
      newNote.setText(crq.textResult);

      newNoteColor = crq.colorResult;   
      newNoteFont = crq.fontOverrideResult;
      justification = crq.justResult;
      nextStep_ = "stepSetToMode";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    } 

    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.ADD_NOTE;
      return (retval);
    }
        
    /***************************************************************************
    **
    ** Locate a new note
    */  
   
    private DialogAndInProcessCmd stepPlaceNewNote() {
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState_, "undo.addNote");
      GenomeChange gc = dacx_.getGenome().addNoteWithExistingLabel(newNote);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
      }
      
      //
      // Propagate note properties to all interested layouts
      //
         
      NoteProperties np = new NoteProperties(appState_, dacx_.getLayout(), newNote.getID(), newNoteColor, x, y);
      if (newNoteFont != null) {
        np.setFontOverride(newNoteFont);
      }
      np.setJustification(justification);
   
      Layout.PropChange[] lpc = new Layout.PropChange[1];
      lpc[0] = dacx_.getLayout().setNoteProperties(newNote.getID(), np.clone());
      if (lpc[0] != null) {
        PropChangeCmd pcc = new PropChangeCmd(appState_, dacx_, lpc);
        support.addEdit(pcc);
      }
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      support.finish();
      
      //
      // DONE!
      //
      
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }    
  }   
}
