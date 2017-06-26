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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
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
  
  public WorkspaceOps(SpaceAction action) {
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
   
  @Override
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
        StepState ans = new StepState(action_, cfh);
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
        
  public static class StepState extends AbstractStepState {
    
    private SpaceAction myAction_;

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SpaceAction action, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
      myAction_ = action;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SpaceAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
      myAction_ = action;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {  
      switch (myAction_) {
        case RESIZE:
          boolean emptyNoOverlay = dacx_.getDBGenome().isEmpty() && !dacx_.getFGHO().overlayExists();
          ResizeWorkspaceDialog rwd = new ResizeWorkspaceDialog(uics_, dacx_.getWorkspaceSource().getWorkspace(), dacx_.getZoomTarget(), emptyNoOverlay, Workspace.PADDING);
          rwd.setVisible(true);
          if (!rwd.haveResult()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
          }
          UndoSupport support = uFac_.provideUndoSupport("undo.resizeWorkspace", dacx_);
          (new WorkspaceSupport(dacx_)).setWorkspace(rwd.getResults(), support);
          uics_.getZoomCommandSupport().zoomToFullWorksheet();
          uics_.getSUPanel().drawModel(false);
          break;
        case SHIFT_ALL:
          Rectangle allBounds = dacx_.getZoomTarget().getAllModelBounds();
          int modelCenterX = allBounds.x + (allBounds.width / 2);
          int modelCenterY = allBounds.y + (allBounds.height / 2);
          Workspace wsp = dacx_.getWorkspaceSource().getWorkspace();
          Dimension currDims = wsp.getCanvasSize();
          int wsCornerX = modelCenterX - (currDims.width / 2);
          int wsCornerY = modelCenterY - (currDims.height / 2);        
          support = uFac_.provideUndoSupport("undo.shiftAllToWorkspace", dacx_);
          (new WorkspaceSupport(dacx_)).setWorkspace(new Rectangle(wsCornerX, wsCornerY, currDims.width, currDims.height), support);
          uics_.getZoomCommandSupport().zoomToFullWorksheet();
          uics_.getSUPanel().drawModel(false);
          break;
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
