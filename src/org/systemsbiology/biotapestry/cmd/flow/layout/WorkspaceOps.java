/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.ui.dialogs.ResizeWorkspaceDialog;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle centering current model
*/

public class WorkspaceOps extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SpaceAction {
    RESIZE("command.ResizeWorksheet", "command.ResizeWorksheet", "FIXME24.gif", "command.ResizeWorksheetMnem", null),
    SHIFT_ALL("command.ShiftAllToWorkspace", "command.ShiftAllToWorkspace", "FIXME24.gif", "command.ShiftAllToWorkspaceMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    SpaceAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    } 
  
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private SpaceAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public WorkspaceOps(BTState appState, SpaceAction action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
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
    switch (action_) {
      case RESIZE:
        return (true); 
      case SHIFT_ALL:
        return (cache.moreThanOneModel()); 
      default:
        throw new IllegalStateException();
    }
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
        StepState ans = new StepState(appState_, action_, cfh.getDataAccessContext());
        next = ans.stepDoIt();    
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
    
    private String nextStep_;    
    private BTState appState_;
    private SpaceAction myAction_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, SpaceAction action, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepDoIt";
      myAction_ = action;
      dacx_ = dacx;
    }
 
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {  
      switch (myAction_) {
        case RESIZE:
          ResizeWorkspaceDialog rwd = new ResizeWorkspaceDialog(appState_, Workspace.PADDING);
          rwd.setVisible(true);
          if (!rwd.haveResult()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
          }
          UndoSupport support = new UndoSupport(appState_, "undo.resizeWorkspace");
          (new WorkspaceSupport(appState_, dacx_)).setWorkspace(rwd.getResults(), support);
          appState_.getZoomCommandSupport().zoomToFullWorksheet();
          appState_.getSUPanel().drawModel(false);
          break;
        case SHIFT_ALL:
          Rectangle allBounds = appState_.getZoomTarget().getAllModelBounds();
          int modelCenterX = allBounds.x + (allBounds.width / 2);
          int modelCenterY = allBounds.y + (allBounds.height / 2);
          Dimension currDims = appState_.getCanvasSize();
          int wsCornerX = modelCenterX - (currDims.width / 2);
          int wsCornerY = modelCenterY - (currDims.height / 2);        
          support = new UndoSupport(appState_, "undo.shiftAllToWorkspace");
          (new WorkspaceSupport(appState_, dacx_)).setWorkspace(new Rectangle(wsCornerX, wsCornerY, currDims.width, currDims.height), support);
          appState_.getZoomCommandSupport().zoomToFullWorksheet();
          appState_.getSUPanel().drawModel(false);
          break;
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
