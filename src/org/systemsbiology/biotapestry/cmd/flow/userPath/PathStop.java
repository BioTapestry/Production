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

package org.systemsbiology.biotapestry.cmd.flow.userPath;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.UserTreePathStopCreationDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Handle Stepping in user tree path
*/

public class PathStop extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private boolean doAdd_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathStop(BTState appState, boolean doAdd) {
    super(appState);
    name =  (doAdd) ? "command.TreePathAddStop" : "command.TreePathDeleteStop";
    desc = (doAdd) ? "command.TreePathAddStop" : "command.TreePathDeleteStop";
    icon = (doAdd) ? "TreePathAddStop24.gif" : "TreePathDeleteStop24.gif";
    mnem =  (doAdd) ? "command.TreePathAddStopMnem" : "command.TreePathDeleteStopMnem";
    doAdd_ = doAdd; 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  @Override
  public boolean externallyEnabled() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(appState_, doAdd_, cfh.getDataAccessContext());
        next = ans.stepToProcess();    
      } else {
        throw new IllegalStateException();
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {
    
    private boolean doAdd;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean doAdd, DataAccessContext dacx) {
      appState_ = appState;
      this.doAdd = doAdd;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
      if (doAdd) {
        doAdd();
      } else {
        doRemove();
      }
      appState_.getPathControls().handlePathButtons();      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
    
    
    /***************************************************************************
    **
    ** Command
    */ 
   
    public void doAdd() {
      UserTreePathStopCreationDialog scd = new UserTreePathStopCreationDialog(appState_);
      scd.setVisible(true);
      if (!scd.haveResult()) {
        return;
      }
      String addToPathKey = scd.getChosenPath();
      String insertMode = scd.getInsertionMode();
      appState_.getPathController().addAStop(addToPathKey, insertMode, dacx_);
      appState_.getPathControls().handlePathButtons();
      return;
    }
  
  
    /***************************************************************************
    **
    ** Command
    */ 

    public void doRemove() {
      if (appState_.getPathController().pathHasOnlyOneStop()) {
        ResourceManager rMan = appState_.getRMan(); 
        int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                               rMan.getString("treePathDeleteStop.oneStopWarningMessage"), 
                               rMan.getString("treePathDeleteStop.oneStopWarningMessageTitle"),
                               JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }
      appState_.getPathController().deleteCurrentStop(dacx_);
      appState_.getPathControls().handlePathButtons();   
      return;
    }
  }  
}
