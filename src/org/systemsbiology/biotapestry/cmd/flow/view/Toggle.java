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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
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
  
  public Toggle(BTState appState, ToggleAction action) {
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
  ** Toggle support for popup checks
  ** 
  */
  
  public boolean shouldCheck() {
    BTState.SearchModifiers sm = appState_.getSearchModifiers();
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) { 
    switch (action_) {
      case NET_MOD_CONTENT:
        NetOverlayController noc = appState_.getNetOverlayController();
        return (noc.maskingIsActive());
      case GROUP_TOGGLE:
      case SELECT_QUERY_NODE:
      case APPEND_SELECT: 
        return (true);
      case SELECT_LINKS: 
        return (!appState_.getSUPanel().linksAreHidden(rcx));
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
    private ToggleAction myAction_;
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
    
    public StepState(BTState appState, ToggleAction action, DataAccessContext dacx) {
      appState_ = appState;
      myAction_ = action;
      nextStep_ = "stepToggle";
      dacx_ = dacx;
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
          NetOverlayController noc = appState_.getNetOverlayController();
          UndoSupport support = new UndoSupport(appState_, "undo.toggleModuleViz");
          noc.toggleModContentDisplay(modID, support, dacx_);
          support.finish();
          break;
        case GROUP_TOGGLE:
          groupToggle();
          break; 
        case SELECT_QUERY_NODE: 
          BTState.SearchModifiers sm = appState_.getSearchModifiers();
          sm.includeQueryNode = !sm.includeQueryNode;
          break;
        case APPEND_SELECT:          
          sm = appState_.getSearchModifiers();
          sm.appendToCurrent = !sm.appendToCurrent;
          break;       
        case SELECT_LINKS:
          sm = appState_.getSearchModifiers();
          sm.includeLinks = !sm.includeLinks;
          break;
        case PAD_BUBBLES:
          appState_.getSUPanel().toggleTargetBubbles();
          break;
        case MOD_PARTS:
          appState_.toggleModuleComponents();
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
      Genome genome = dacx_.getGenome();
      if (!(genome instanceof GenomeInstance)) {
        throw new IllegalStateException();
      }
      Group group = ((GenomeInstance)genome).getGroup(intersect_.getObjectID());
      String groupRef = group.getID();
      GroupSettingManager gsm = (GroupSettingManager)dacx_.gsm;
      GroupSettings.Setting groupViz = gsm.getGroupVisibility(genome.getID(), groupRef, dacx_);
      GroupSettings.Setting newSetting = (groupViz == GroupSettings.Setting.ACTIVE) ? GroupSettings.Setting.INACTIVE : GroupSettings.Setting.ACTIVE;
      GroupSettingChange gsc = gsm.setGroupVisibility(genome.getID(), groupRef, newSetting);
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState_, dacx_, gsc);
        UndoSupport support = new UndoSupport(appState_, "undo.regionToggle");
        support.addEdit(gscc);
        support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
      }       
      return;
    }
  }  
}
