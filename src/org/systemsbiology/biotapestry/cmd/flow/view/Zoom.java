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

import java.awt.Rectangle;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Handle various Zoom operations
*/

public class Zoom extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ZoomAction {
    ALL_MODELS("command.ZoomToAllModels", "command.ZoomToAllModels", "ZoomToAllModels24.gif", "command.ZoomToAllModelsMnem", null),
    CURRENT_MODEL("command.ZoomToModel", "command.ZoomToModel", "ZoomToModel24.gif", "command.ZoomToModelMnem", null),
    WORKSPACE("command.ZoomToShowWorkspace", "command.ZoomToShowWorkspace", "FIXME24.gif", "command.ZoomToShowWorkspaceMnem", null),
    SELECTED("command.ZoomToSelected", "command.ZoomToSelected", "ZoomToAllSelections24.gif", "command.ZoomToSelectedMnem", null),
    CURRENT_SELECTED("command.ZoomToCurrentSelected", "command.ZoomToCurrentSelected", "ZoomToSelected24.gif", "command.ZoomToCurrentSelectedMnem", null),
    CENTER_PREV_SELECTED("command.CenterOnPrevSelected", "command.CenterOnPrevSelected", "CenterOnPrevSelected24.gif", "command.CenterOnPrevSelectedMnem", null),
    CENTER_NEXT_SELECTED("command.CenterOnNextSelected", "command.CenterOnNextSelected", "CenterOnNextSelected24.gif", "command.CenterOnNextSelectedMnem", null),
    ZOOM_CURRENT_MOD("command.ZoomToCurrentNetworkModule", "command.ZoomToCurrentNetworkModule", "FIXME24.gif", "command.ZoomToCurrentNetworkModuleMnem", null),
    ZOOM_IN("command.ZoomIn", "command.ZoomIn", "ZoomIn24.gif", "command.ZoomInMnem", "command.ZoomInAccel"),
    ZOOM_OUT("command.ZoomOut", "command.ZoomOut", "ZoomOut24.gif", "command.ZoomOutMnem", "command.ZoomOutAccel"),
    ZOOM_MOD_POPUP("modulePopup.zoomToModule", "modulePopup.zoomToModule", "FIXME24.gif", "modulePopup.zoomToModuleMnem", null),
    ZOOM_TO_GROUP("groupPopup.ZoomToGroup", "groupPopup.ZoomToGroup", "FIXME24.gif", "groupPopup.ZoomToGroupMnem", null),
    ;
  
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ZoomAction(String name, String desc, String icon, String mnem, String accel) {
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
  
  private ZoomAction action_;
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
  
  public Zoom(BTState appState, ZoomAction action) {
    appState_ = appState;
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
  ** Answer if we can be pushed
  */
 
  @Override
  public boolean canPush(int pushCondition) {
    if ((action_ == ZoomAction.ZOOM_IN) || (action_ == ZoomAction.ZOOM_OUT)) {
      return ((pushCondition & MainCommands.ALLOW_NAV_PUSH) == 0x00);
    } else {
      return (super.canPush(pushCondition));
    }
  }
 
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override
   public boolean isEnabled(CheckGutsCache cache) {
     switch (action_) {
       case ALL_MODELS:
       case CURRENT_MODEL:
         return (cache.genomeNotEmpty() || cache.genomeHasModule());
       case WORKSPACE:
         return (true);
       case SELECTED:
         return (cache.haveASelection());
       case CURRENT_SELECTED:
         return ((new StaticDataAccessContext(appState_)).getZoomTarget().haveCurrentSelectionForBounds());  // YUK
       case CENTER_PREV_SELECTED:
       case CENTER_NEXT_SELECTED:
         return (cache.haveASelection() && cache.hasMultiSelections());
       case ZOOM_CURRENT_MOD: 
       case ZOOM_IN: 
       case ZOOM_OUT: 
       case ZOOM_TO_GROUP: 
       case ZOOM_MOD_POPUP: 
       default:
         throw new IllegalStateException();
     }
   }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if ((action_ == ZoomAction.ZOOM_TO_GROUP) || (action_ == ZoomAction.ZOOM_MOD_POPUP)) {
      return (true);
    } else {
      throw new IllegalStateException();
    }
  }
    
  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  @Override
  public boolean externallyEnabled() {
    return ((action_ == ZoomAction.ZOOM_CURRENT_MOD) ||
            (action_ == ZoomAction.ZOOM_IN) ||
            (action_ == ZoomAction.ZOOM_OUT));
  }
   
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(action_, dacx, new DynamicDataAccessContext(appState_));
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
        ans = new StepState(action_, cfh, new DynamicDataAccessContext(appState_));
      } else { 
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepZoom")) {
        next = ans.stepZoom();
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
    private ZoomAction myAction_;
    private DynamicDataAccessContext rcxT_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ZoomAction action, StaticDataAccessContext dacx, DynamicDataAccessContext ddacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepZoom";
      rcxT_ = ddacx;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ZoomAction action, ServerControlFlowHarness cfh, DynamicDataAccessContext ddacx) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepZoom";
      rcxT_ = ddacx;
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
       
    private DialogAndInProcessCmd stepZoom() {
      switch (myAction_) {
        case ALL_MODELS:
          uics_.getZoomCommandSupport().zoomToAllModels();
          break;
        case CURRENT_MODEL:
          uics_.getZoomCommandSupport().zoomToModel();
          break; 
        case WORKSPACE:   
          uics_.getZoomCommandSupport().zoomToFullWorksheet();
          break;
        case SELECTED:
          uics_.getZoomCommandSupport().zoomToSelected();
          break; 
        case CURRENT_SELECTED:
          uics_.getZoomCommandSupport().zoomToCurrentSelected();
          break;
        case CENTER_PREV_SELECTED: 
          if (dacx_.getZoomTarget().haveCurrentSelectionForBounds()) {
            uics_.getZoomCommandSupport().centerToPreviousSelected();
          } else {
            uics_.getZoomCommandSupport().zoomToPreviousSelected();
          }
          uics_.getVirtualZoom().handleZoomToCurrentButton();
          break;
        case CENTER_NEXT_SELECTED: 
          if (dacx_.getZoomTarget().haveCurrentSelectionForBounds()) {
            uics_.getZoomCommandSupport().centerToNextSelected();
          } else {
            uics_.getZoomCommandSupport().zoomToNextSelected();
          }
          uics_.getVirtualZoom().handleZoomToCurrentButton();
          break;
        case ZOOM_CURRENT_MOD: 
          zoomToCurrentMod();
          break;
        case ZOOM_MOD_POPUP: 
          zoomToModPopup();
          break;          
        case ZOOM_IN: 
          uics_.getZoomCommandSupport().bumpZoomWrapper('+');
          break;  
        case ZOOM_OUT: 
          uics_.getZoomCommandSupport().bumpZoomWrapper('-');
          break;
        case ZOOM_TO_GROUP: 
          zoomToGroup();
          break;  
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Lots to do for current module zoom
    */
    
    private void zoomToCurrentMod() {
      Zoom.zoomToCurrentMod(uics_, rcxT_);
      return;
    } 
    
    /***************************************************************************
    **
    ** Lots to do for popup zoom
    */
    
    private void zoomToModPopup() {
      String modID = intersect_.getObjectID();
      String ovrKey = rcxT_.getOSO().getCurrentOverlay();
      TaggedSet modKeys = new TaggedSet();
      modKeys.set.add(modID);
      NetOverlayOwner noo = rcxT_.getCurrentOverlayOwner();
      Rectangle bounds = rcxT_.getCurrentLayout().getLayoutBoundsForNetModules(noo, ovrKey, modKeys, false, rcxT_);
      uics_.getZoomCommandSupport().zoomToRect(bounds);         
      return;        
    }
   
    /***************************************************************************
    **
    ** Lots to do for group zoom
    */
    
    private void zoomToGroup() {   
      Zoom.zoomToGroup(uics_, intersect_.getObjectID(), rcxT_);
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Exported version of zoom to group
  */  
   
  public static void zoomToGroup(UIComponentSource uics, String groupID, DataAccessContext rcxT) {   

    if (!rcxT.currentGenomeIsAnInstance()) {
      throw new IllegalStateException();
    }
    GenomeInstance gi = rcxT.getCurrentGenomeAsInstance();  
    Group group = gi.getGroup(groupID);
    if (group == null) {
      throw new IllegalStateException();
    }
    // Use the root instance for this calculation!
    GenomeInstance rootInstance = (gi.isRootInstance()) ? gi : gi.getVfgParentRoot();
    Group grp = rootInstance.getGroup(Group.getBaseID(groupID)); 
    StaticDataAccessContext rcx2 = new StaticDataAccessContext(rcxT, rootInstance);  
    Rectangle bounds = rcx2.getCurrentLayout().getLayoutBoundsForGroup(grp, rcx2, true, false);     
    uics.getZoomCommandSupport().zoomToRect(bounds);
    return;
  }
 
  /***************************************************************************
  **
  ** Exported version of current module zoom
  */
  
  public static void zoomToCurrentMod(UIComponentSource uics, DataAccessContext rcxT) {
    String ovrKey = rcxT.getOSO().getCurrentOverlay();
    TaggedSet modKeys = rcxT.getOSO().getCurrentNetModules();
    if (modKeys.set.size() < 1) {
      return; // should not happen...
    }
    NetOverlayOwner noo = rcxT.getCurrentOverlayOwner();
    boolean showLinksToo = (modKeys.set.size() > 1);
 
    Rectangle bounds = rcxT.getCurrentLayout().getLayoutBoundsForNetModules(noo, ovrKey, modKeys, showLinksToo, rcxT);
    uics.getZoomCommandSupport().zoomToRect(bounds);         
    return;
  } 
}
