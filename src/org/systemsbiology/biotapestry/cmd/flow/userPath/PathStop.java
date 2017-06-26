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

package org.systemsbiology.biotapestry.cmd.flow.userPath;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
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
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathStop(boolean doAdd, BTState appState) {
    appState_ = appState;
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
        StepState ans = new StepState(doAdd_, cfh);
        ans.setAppState(appState_);
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
        
  public static class StepState extends AbstractStepState {
    
    private boolean doAdd;
    private DynamicDataAccessContext ddacx_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doAdd, StaticDataAccessContext dacx) {
      super(dacx);
      this.doAdd = doAdd;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doAdd, ServerControlFlowHarness cfh) {
      super(cfh);
      this.doAdd = doAdd;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** We have to use appState for the moment...
    */ 
    
    public void setAppState(BTState appState) {
      // We ignore the static context we are being handed, and use what we are provided here!
      ddacx_ = new DynamicDataAccessContext(appState);
      return;
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
      uics_.getPathControls().handlePathButtons();      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
    
    
    /***************************************************************************
    **
    ** Command
    */ 
   
    public void doAdd() {
      UserTreePathStopCreationDialog scd = new UserTreePathStopCreationDialog(uics_, dacx_);
      scd.setVisible(true);
      if (!scd.haveResult()) {
        return;
      }
      String addToPathKey = scd.getChosenPath();
      String insertMode = scd.getInsertionMode();
      uics_.getPathController().addAStop(addToPathKey, insertMode, ddacx_, uFac_);
      uics_.getPathControls().handlePathButtons();
      return;
    }
  
  
    /***************************************************************************
    **
    ** Command
    */ 

    public void doRemove() {
      if (uics_.getPathController().pathHasOnlyOneStop()) {
        ResourceManager rMan = dacx_.getRMan(); 
        int ok = JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                               rMan.getString("treePathDeleteStop.oneStopWarningMessage"), 
                               rMan.getString("treePathDeleteStop.oneStopWarningMessageTitle"),
                               JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }
      uics_.getPathController().deleteCurrentStop(ddacx_, uFac_);
      uics_.getPathControls().handlePathButtons();   
      return;
    }
  }  
}
