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

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle various selection operations
*/

public class Toggle extends AbstractControlFlow implements ControlFlow.FlowForPopToggle {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ToggleAction {
    NET_MOD_CONTENT("modulePopup.toggleShowContents", "modulePopup.toggleShowContents", "FIXME24.gif", "modulePopup.toggleShowContentsMnem", null),
    GROUP_TOGGLE("groupPopup.Toggle", "groupPopup.Toggle", "FIXME24.gif", "groupPopup.ToggleMnem", null),
    SELECT_QUERY_NODE("nodePopup.ToggleIncludeQueryNode", "nodePopup.ToggleIncludeQueryNode", "FIXME24.gif", "nodePopup.ToggleIncludeQueryNodeMnem", null),
    APPEND_SELECT("nodePopup.ToggleAddToCurrentSelections", "nodePopup.ToggleAddToCurrentSelections", "FIXME24.gif", "nodePopup.ToggleAddToCurrentSelectionsMnem", null),
    SELECT_LINKS("nodePopup.ToggleIncludeLinks", "nodePopup.ToggleIncludeLinks", "FIXME24.gif", "nodePopup.ToggleIncludeLinksMnem", null),
    PAD_BUBBLES("command.ToggleBubbles", "command.ToggleBubbles", "ShowPads24.gif", "command.ToggleBubblesMnem", null),
    MOD_PARTS("command.ToggleModuleComponent", "command.ToggleModuleComponent", "ShowModuleComps24.gif", "command.ToggleModuleCompMnem", null),
    ;
   
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ToggleAction(String name, String desc, String icon, String mnem, String accel) {
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
  
  private ToggleAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Toggle(ToggleAction action) {
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
  ** Toggle support for popup checks
  ** 
  */
  
  public boolean shouldCheck(CmdSource cSrc) {
    CmdSource.SearchModifiers sm = cSrc.getSearchModifiers();
    switch (action_) {
      case SELECT_LINKS:
         return (sm.includeLinks);
      case SELECT_QUERY_NODE:
         return (sm.includeQueryNode);  
      case APPEND_SELECT:
         return (sm.appendToCurrent);
      default:
        throw new IllegalStateException();    
    }
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override 
  public boolean isEnabled(CheckGutsCache cache) {
    if (action_ == ToggleAction.PAD_BUBBLES) {
      return (cache.canShowBubbles());
    } else if (action_ == ToggleAction.MOD_PARTS) {
      throw new IllegalStateException(); 
    } else {
      return (false);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if we can be pushed
  ** 
  */
 
  @Override
  public boolean canPush(int pushCondition) {
    if ((action_ == ToggleAction.PAD_BUBBLES) || (action_ == ToggleAction.MOD_PARTS)) {
      return ((pushCondition & MainCommands.ALLOW_NAV_PUSH) == 0x00);
    } else {
      return (super.canPush(pushCondition));
    }
  }
  
  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  @Override
  public boolean externallyEnabled() {
    return (action_ == ToggleAction.MOD_PARTS);
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) { 
    switch (action_) {
      case NET_MOD_CONTENT:
        NetOverlayController noc = uics.getNetOverlayController();
        return (noc.maskingIsActive());
      case GROUP_TOGGLE:
      case SELECT_QUERY_NODE:
      case APPEND_SELECT: 
        return (true);
      case SELECT_LINKS: 
        return (!uics.getGenomePresentation().linksAreHidden(rcx));
      case PAD_BUBBLES: 
      default:
        throw new IllegalStateException();
    }
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
        ans = new StepState(action_, cfh);
      } else { 
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToggle")) {
        next = ans.stepToggle();
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
    private ToggleAction myAction_; 
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ToggleAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToggle";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ToggleAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToggle";
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
       
    private DialogAndInProcessCmd stepToggle() {
      switch (myAction_) {
        case NET_MOD_CONTENT:
          String modID = intersect_.getObjectID(); 
          NetOverlayController noc = uics_.getNetOverlayController();        
          UndoSupport support = uFac_.provideUndoSupport("undo.toggleModuleViz", dacx_);
          noc.toggleModContentDisplay(modID, support, dacx_);
          support.finish();
          break;
        case GROUP_TOGGLE:
          groupToggle();
          break; 
        case SELECT_QUERY_NODE: 
          CmdSource.SearchModifiers sm = cmdSrc_.getSearchModifiers();
          sm.includeQueryNode = !sm.includeQueryNode;
          break;
        case APPEND_SELECT:          
          sm = cmdSrc_.getSearchModifiers();
          sm.appendToCurrent = !sm.appendToCurrent;
          break;       
        case SELECT_LINKS:
          sm = cmdSrc_.getSearchModifiers();
          sm.includeLinks = !sm.includeLinks;
          break;
        case PAD_BUBBLES:
          uics_.getSUPanel().toggleTargetBubbles();
          break;
        case MOD_PARTS:
          cmdSrc_.toggleModuleComponents();
          break;          
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private void groupToggle() { 
      if (!dacx_.currentGenomeIsAnInstance()) {
        throw new IllegalStateException();
      }
      Group group = dacx_.getCurrentGenomeAsInstance().getGroup(intersect_.getObjectID());
      String groupRef = group.getID();
      GroupSettingSource gsm = dacx_.getGSM();
      GroupSettings.Setting groupViz = gsm.getGroupVisibility(dacx_.getCurrentGenomeID(), groupRef, dacx_);
      GroupSettings.Setting newSetting = (groupViz == GroupSettings.Setting.ACTIVE) ? GroupSettings.Setting.INACTIVE : GroupSettings.Setting.ACTIVE;
      GroupSettingChange gsc = gsm.setGroupVisibility(dacx_.getCurrentGenomeID(), groupRef, newSetting);
      if (gsc != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.regionToggle", dacx_);
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(dacx_, gsc);
        support.addEdit(gscc);
        support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
      }       
      return;
    }
  }  
}
