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


package org.systemsbiology.biotapestry.cmd.flow.view;

import java.awt.Rectangle;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.TaggedSet;

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
         return (appState_.getZoomTarget().haveCurrentSelectionForBounds());  
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, action_, dacx);
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
        ans = new StepState(appState_, action_, cfh.getDataAccessContext());
      } else { 
        ans = (StepState)last.currStateX;
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
    private ZoomAction myAction_;
    private String nextStep_;
    private DataAccessContext rcxT_;
    private BTState appState_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, ZoomAction action, DataAccessContext dacx) {
      appState_ = appState;
      myAction_ = action;
      nextStep_ = "stepZoom";
      rcxT_ = dacx;
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
          appState_.getZoomCommandSupport().zoomToAllModels();
          break;
        case CURRENT_MODEL:
          appState_.getZoomCommandSupport().zoomToModel();
          break; 
        case WORKSPACE:   
          appState_.getZoomCommandSupport().zoomToFullWorksheet();
          break;
        case SELECTED:
          appState_.getZoomCommandSupport().zoomToSelected();
          break; 
        case CURRENT_SELECTED:
          appState_.getZoomCommandSupport().zoomToCurrentSelected();
          break;
        case CENTER_PREV_SELECTED: 
          if (appState_.getZoomTarget().haveCurrentSelectionForBounds()) {
            appState_.getZoomCommandSupport().centerToPreviousSelected();
          } else {
            appState_.getZoomCommandSupport().zoomToPreviousSelected();
          }
          appState_.getVirtualZoom().handleZoomToCurrentButton();
          break;
        case CENTER_NEXT_SELECTED: 
          if (appState_.getZoomTarget().haveCurrentSelectionForBounds()) {
            appState_.getZoomCommandSupport().centerToNextSelected();
          } else {
            appState_.getZoomCommandSupport().zoomToNextSelected();
          }
          appState_.getVirtualZoom().handleZoomToCurrentButton();
          break;
        case ZOOM_CURRENT_MOD: 
          zoomToCurrentMod();
          break;
        case ZOOM_MOD_POPUP: 
          zoomToModPopup();
          break;          
        case ZOOM_IN: 
          appState_.getZoomCommandSupport().bumpZoomWrapper('+');
          break;  
        case ZOOM_OUT: 
          appState_.getZoomCommandSupport().bumpZoomWrapper('-');
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
      Zoom.zoomToCurrentMod(appState_, rcxT_);
      return;
    } 
    
    /***************************************************************************
    **
    ** Lots to do for popup zoom
    */
    
    private void zoomToModPopup() {
      String modID = intersect_.getObjectID();
      String ovrKey = rcxT_.oso.getCurrentOverlay();
      TaggedSet modKeys = new TaggedSet();
      modKeys.set.add(modID);
      NetOverlayOwner noo = rcxT_.getCurrentOverlayOwner();
      Rectangle bounds = rcxT_.getLayout().getLayoutBoundsForNetModules(noo, ovrKey, modKeys, false, rcxT_);
      appState_.getZoomCommandSupport().zoomToRect(bounds);         
      return;        
    }
   
    /***************************************************************************
    **
    ** Lots to do for group zoom
    */
    
    private void zoomToGroup() {   
      Zoom.zoomToGroup(appState_, intersect_.getObjectID(), rcxT_);
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Exported version of zoom to group
  */  
   
  public static void zoomToGroup(BTState appState, String groupID, DataAccessContext rcxT) {   

    if (!(rcxT.getGenome() instanceof GenomeInstance)) {
      throw new IllegalStateException();
    }
    GenomeInstance gi = rcxT.getGenomeAsInstance();  
    Group group = gi.getGroup(groupID);
    if (group == null) {
      throw new IllegalStateException();
    }
    // Use the root instance for this calculation!
    GenomeInstance rootInstance = (gi.isRootInstance()) ? gi : gi.getVfgParentRoot();
    Group grp = rootInstance.getGroup(Group.getBaseID(groupID)); 
    DataAccessContext rcx2 = new DataAccessContext(rcxT, rootInstance);  
    Rectangle bounds = rcx2.getLayout().getLayoutBoundsForGroup(grp, rcx2, true, false);     
    appState.getZoomCommandSupport().zoomToRect(bounds);
    return;
  }
 
  /***************************************************************************
  **
  ** Exported version of current module zoom
  */
  
  public static void zoomToCurrentMod(BTState appState, DataAccessContext rcxT) {
    String ovrKey = rcxT.oso.getCurrentOverlay();
    TaggedSet modKeys = rcxT.oso.getCurrentNetModules();
    if (modKeys.set.size() < 1) {
      return; // should not happen...
    }
    NetOverlayOwner noo = rcxT.getCurrentOverlayOwner();
    boolean showLinksToo = (modKeys.set.size() > 1);
 
    Rectangle bounds = rcxT.getLayout().getLayoutBoundsForNetModules(noo, ovrKey, modKeys, showLinksToo, rcxT);
    appState.getZoomCommandSupport().zoomToRect(bounds);         
    return;
  } 
}
