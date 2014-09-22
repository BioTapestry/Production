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


package org.systemsbiology.biotapestry.cmd.flow.settings;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.dialogs.ColorEditorDialog;
import org.systemsbiology.biotapestry.ui.dialogs.DevelopmentSpecDialog;
import org.systemsbiology.biotapestry.ui.dialogs.DisplayOptionsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.FontDialog;
import org.systemsbiology.biotapestry.ui.dialogs.LayoutParametersDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputTableManageDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeAxisSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseTableManageDialog;


/****************************************************************************
**
** Handle various settings.  FIXME!!!!!
*/

public class DoSettings extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SettingAction {
    DISPLAY("command.SetDisplayOpts", "command.SetDisplayOpts", "FIXME24.gif", "command.SetDisplayOptsMnem", null),
    FONTS("command.SetFont", "command.SetFont", "FIXME.gif", "command.SetFontMnem", null),
    COLORS("command.EditColors", "command.EditColors", "FIXME24.gif", "command.EditColorsMnem", null),
    AUTO_LAYOUT("command.SetAutolayoutOpts", "command.SetAutolayoutOpts", "FIXME24.gif", "command.SetAutolayoutOptsMnem", null), 
    TIME_AXIS("command.DefineTimeAxis", "command.DefineTimeAxis", "FIXME24.gif", "command.DefineTimeAxisMnem", null),
    PERTURB("command.PerturbManage", "command.PerturbManage", "FIXME24.gif", "command.PerturbManageMnem", null),
    TIME_COURSE_SETUP("command.TimeCourseSetup", "command.TimeCourseSetup", "FIXME24.gif", "command.TimeCourseSetupMnem", null),
    TIME_COURSE_MANAGE("command.TimeCourseManage", "command.TimeCourseManage", "FIXME24.gif", "command.TimeCourseManageMnem", null),
    REGION_HIERARCHY("command.TimeCourseRegionHierarchy", "command.TimeCourseRegionHierarchy", "FIXME24.gif", "command.TimeCourseRegionHierarchyMnem", null),
    TEMPORAL_INPUT_MANAGE("command.TemporalInputManage", "command.TemporalInputManage", "FIXME24.gif", "command.TemporalInputManageMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    SettingAction(String name, String desc, String icon, String mnem, String accel) {
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
  
  private SettingAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DoSettings(BTState appState, SettingAction action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case DISPLAY:
      case COLORS:
      case FONTS:
      case TIME_AXIS:
      case TIME_COURSE_SETUP:
      case TIME_COURSE_MANAGE:
      case TEMPORAL_INPUT_MANAGE:
      case PERTURB:
        return (true);
      case AUTO_LAYOUT:
        return (cache.isRootOrRootInstance());
      case REGION_HIERARCHY:  
        return (cache.timeCourseHasTemplate());      
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
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
      if (ans.getNextStep().equals("stepToProcess")) {
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

    private String nextStep_;
    private SettingAction myAction_;
    private BTState appState_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, SettingAction action, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
           
      switch (myAction_) {
        case DISPLAY:      
          DisplayOptionsDialog dod = new DisplayOptionsDialog(appState_, dacx_);
          dod.setVisible(true);   
          break;  
        case COLORS:      
          ColorEditorDialog ced = new ColorEditorDialog(appState_, dacx_, null);
          ced.setVisible(true);
          break; 
        case FONTS:      
          FontDialog fd = new FontDialog(appState_, dacx_, appState_.getLayoutKey());
          fd.setVisible(true);
          break;
        case AUTO_LAYOUT:
          LayoutParametersDialog lpd = new LayoutParametersDialog(appState_);
          lpd.setVisible(true);
          if (lpd.haveResult()) {
            LayoutOptionsManager lom = appState_.getLayoutOptMgr();
            lom.setLayoutOptions(lpd.getOptions());
            lom.setWorksheetLayoutParams(lpd.getWorksheetParams());
            lom.setStackedBlockLayoutParams(lpd.getStackedBlockParams());
            lom.setDiagonalLayoutParams(lpd.getDiagonalParams());
            lom.setHaloLayoutParams(lpd.getHaloParams());
          }
          break;
        case TIME_AXIS:      
          TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(appState_, dacx_);
          tasd.setVisible(true);
          break;
        case PERTURB:
          appState_.getCommonView().launchPerturbationsManagementWindow(new PertFilterExpression(PertFilterExpression.ALWAYS_OP), dacx_);
          break;
        case TIME_COURSE_MANAGE:      
          TimeCourseTableManageDialog tctmd = new TimeCourseTableManageDialog(appState_, dacx_);
          tctmd.setVisible(true);
          break;
        case TIME_COURSE_SETUP:
          TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeSourceSetupDialogWrapper(appState_, dacx_);
          if (tcsd == null) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          tcsd.setVisible(true);
          break;
        case REGION_HIERARCHY:
          DevelopmentSpecDialog.assembleAndApplyHierarchy(appState_, dacx_);
          break;
        case TEMPORAL_INPUT_MANAGE: 
          TemporalInputTableManageDialog titmd = new TemporalInputTableManageDialog(appState_, dacx_);
          titmd.setVisible(true);
          break;
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
