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

package org.systemsbiology.biotapestry.cmd.flow.view;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle various selection operations
*/

public class Modules extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ModAction {
    DROP_FROM_CURRENT("modulePopup.dropFromCurrent", "modulePopup.dropFromCurrent", "FIXME24.gif", "modulePopup.dropFromCurrentMnem", null),
    SET_AS_SINGLE("modulePopup.setCurrent", "modulePopup.setCurrent", "FIXME24.gif", "modulePopup.setCurrentMnem", null),
    ;

    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ModAction(String name, String desc, String icon, String mnem, String accel) {
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
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private ModAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Modules(ModAction action) {
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
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    TaggedSet nmSet = rcx.getOSO().getCurrentNetModules();
    return (nmSet.set.size() > 1);
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(action_, dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        throw new IllegalStateException();
      } else { 
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepView")) {
        next = ans.stepView();
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
    private ModAction myAction_;   
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ModAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepView";
    }
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ModAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepView";
    }

    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepView() {
      String netModKey = intersect_.getObjectID();  
      NetOverlayController noc = uics_.getNetOverlayController();
      UndoSupport support;
      switch (myAction_) {
        case DROP_FROM_CURRENT:
          support = uFac_.provideUndoSupport("undo.hideOneVisibleModule", dacx_);
          noc.dropACurrentModule(netModKey, support, dacx_);     
          break;
        case SET_AS_SINGLE:  
          support = uFac_.provideUndoSupport("undo.setSingleVisibleModule", dacx_);
          noc.setToSingleCurrentModule(netModKey, support, dacx_);
          break;   
        default:
          throw new IllegalStateException();
      }
      support.finish();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }  
}
