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


package org.systemsbiology.biotapestry.cmd.flow.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GlobalChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.GlobalChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.dialogs.ColorEditorDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.DevelopmentSpecDialog;
import org.systemsbiology.biotapestry.ui.dialogs.DisplayOptionsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.FontDialog;
import org.systemsbiology.biotapestry.ui.dialogs.LayoutParametersDialog;
import org.systemsbiology.biotapestry.ui.dialogs.RegionTopologyDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputTableManageDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeAxisSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseTableManageDialog;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

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
    REGION_TOPOLOGY("command.TimeCourseRegionTopology", "command.TimeCourseRegionTopology", "FIXME24.gif", "command.TimeCourseRegionTopologyMnem", null),
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
  
  public DoSettings(SettingAction action) {
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
      case REGION_TOPOLOGY:
        return (cache.haveTimeCourseData());      
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
    return (new StepState(action_, dacx));  
  }
  
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
        ans = new StepState(action_, cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else if (ans.getNextStep().equals("stepInstallColors")) {
        next = ans.stepInstallColors(last);      
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

    private SettingAction myAction_;
    private List<ColorDeletionListener> cdls_;
    private HashMap<String, NamedColor> origColors_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      cdls_ = new ArrayList<ColorDeletionListener>();
      origColors_ = new HashMap<String, NamedColor>();
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      cdls_ = new ArrayList<ColorDeletionListener>();
      origColors_ = new HashMap<String, NamedColor>();
    }
    
    /***************************************************************************
    **
    ** Temp Hack to set listeners
    */ 
       
    public void setListeners(List<ColorDeletionListener> colorDeletionListeners) {
      if (colorDeletionListeners != null) {
        cdls_.addAll(colorDeletionListeners); 
      }
      return;
    }
   
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
           
      switch (myAction_) {
        case DISPLAY:      
          DisplayOptionsDialog dod = new DisplayOptionsDialog(uics_, dacx_, hBld_, tSrc_, uFac_);
          dod.setVisible(true);   
          break;  
        case COLORS:      
          return (handleGetColorDialog());
        case FONTS:
          UiUtil.fixMePrintout("One current layout for a global resource? Huh?");
          FontDialog fd = new FontDialog(uics_, dacx_, dacx_.getCurrentLayoutID(), uFac_);
          fd.setVisible(true);
          break;
        case AUTO_LAYOUT:
          LayoutParametersDialog lpd = new LayoutParametersDialog(uics_, dacx_);
          lpd.setVisible(true);
          if (lpd.haveResult()) {
            LayoutOptionsManager lom = dacx_.getLayoutOptMgr();
            lom.setLayoutOptions(lpd.getOptions());
            lom.setWorksheetLayoutParams(lpd.getWorksheetParams());
            lom.setStackedBlockLayoutParams(lpd.getStackedBlockParams());
            lom.setDiagonalLayoutParams(lpd.getDiagonalParams());
            lom.setHaloLayoutParams(lpd.getHaloParams());
          }
          break;
        case TIME_AXIS:      
          TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics_, dacx_, dacx_.getMetabase(), tSrc_, uFac_, true);
          tasd.setVisible(true);
          break;
        case PERTURB:
          uics_.getCommonView().launchPerturbationsManagementWindow(new PertFilterExpression(PertFilterExpression.Op.ALWAYS_OP), dacx_, uics_, uFac_);
          break;
        case TIME_COURSE_MANAGE:      
          TimeCourseTableManageDialog tctmd = new TimeCourseTableManageDialog(uics_, dacx_, tSrc_, uFac_);
          tctmd.setVisible(true);
          break;
        case TIME_COURSE_SETUP:
          TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeCourseSetupDialogWrapper(uics_, dacx_, tSrc_, uFac_);
          if (tcsd == null) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          tcsd.setVisible(true);
          break;
        case REGION_HIERARCHY:
          TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
          DevelopmentSpecDialog.assembleAndApplyHierarchy(uics_, tcd, uFac_, dacx_);
          break;
        case TEMPORAL_INPUT_MANAGE: 
          TemporalInputTableManageDialog titmd = new TemporalInputTableManageDialog(uics_, dacx_, tSrc_, uFac_);
          titmd.setVisible(true);
          break;
        case REGION_TOPOLOGY:
          RegionTopologyDialog rtd = RegionTopologyDialog.regionTopoDialogWrapper(uics_, dacx_, tSrc_, uFac_);
          if (rtd != null) {
            rtd.setVisible(true);
          }
          break;
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  
    /***************************************************************************
    **
    ** On to show color dialog
    */
             
    private DialogAndInProcessCmd handleGetColorDialog() {
      ColorResolver cRes = dacx_.getColorResolver();
      Iterator<String> cit = cRes.getColorKeys();
      ArrayList<NamedColor> colorList = new ArrayList<NamedColor>();   
      while (cit.hasNext()) {
        String colorKey = cit.next();
        NamedColor col = cRes.getNamedColor(colorKey);
        colorList.add(new NamedColor(col));
        origColors_.put(colorKey, new NamedColor(col));
      }
      Collections.sort(colorList);
      Set<String> doNotDelete = cRes.cannotDeleteColors();
      
      ColorEditorDialogFactory.BuildArgs ba = new ColorEditorDialogFactory.BuildArgs(colorList, doNotDelete, cdls_, dacx_.getColorResolver());
      ColorEditorDialogFactory cedf = new ColorEditorDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);
      nextStep_ = "stepInstallColors";
      return (retval);
    }
  
    /***************************************************************************
    **
    ** Apply our UI values to the colors
    ** 
    */
    
    private DialogAndInProcessCmd stepInstallColors(DialogAndInProcessCmd last) {
           
      //
      // Build new map, Figure out the deleted colors:
      //
          
      ColorEditorDialogFactory.ColorsRequest crq = (ColorEditorDialogFactory.ColorsRequest)last.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }

      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.colorDialog", dacx_);
   
      HashMap<String, NamedColor> newColors = new HashMap<String, NamedColor>();
      Iterator<NamedColor> cit = crq.colors.iterator();
      while (cit.hasNext()) {
        NamedColor color = cit.next();
        newColors.put(color.key, color);
      }
     
      Set<String> newKeys = newColors.keySet();
      HashSet<String> intersect = new HashSet<String>(newKeys);
      intersect.retainAll(origColors_.keySet());
      
      HashSet<String> deleted = new HashSet<String>(origColors_.keySet());
      deleted.removeAll(intersect);
      
      //
      // Crank thru the deleted colors and change them to black:
      //
      
      Iterator<Layout> lit = dacx_.getLayoutSource().getLayoutIterator();
      while (lit.hasNext()) {
        Layout lo = lit.next();
        Iterator<String> delit = deleted.iterator();
        while (delit.hasNext()) {
          String key = delit.next();
          //
          // Important!  Must not replace with a deletable color!
          //
          Layout.PropChange[] lpc = lo.replaceColor(key, "black");   
          if (lpc != null) {
            PropChangeCmd pcc = new PropChangeCmd(dacx_, lpc);
            support.addEdit(pcc);
          }
        }
  
        LayoutChangeEvent lcev = new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(lcev);      
      }
     
      //
      // Update the color map:
      //
      
      GlobalChange gc = dacx_.getColorResolver().updateColors(newColors);   
      GlobalChangeCmd gcc = new GlobalChangeCmd(dacx_, gc);
      support.addEdit(gcc);
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.UNSPECIFIED_CHANGE));
  
      //
      // Inform listeners:
      //
      
      if ((cdls_ != null) && !cdls_.isEmpty()) {
        HashMap<String, String> colorMap = new HashMap<String, String>();
        Iterator<String> delit = deleted.iterator();
        while (delit.hasNext()) {
          String key = delit.next();
          colorMap.put(key, "black");
        }
           
        Iterator<String> ocksit = origColors_.keySet().iterator();
        while (ocksit.hasNext()) {
          String key = ocksit.next();
          if (deleted.contains(key)) {
            continue;
          }
          NamedColor nnc = newColors.get(key);
          NamedColor onc = origColors_.get(key);
          if (!onc.equals(nnc)) {
            colorMap.put(key, key); // Not deleted, but changed.
          }
        }

        Iterator<ColorDeletionListener> cdlit = cdls_.iterator();
        while (cdlit.hasNext()) {
          ColorDeletionListener cdl = cdlit.next();
          cdl.colorReplacement(colorMap);
        } 
      }
      
      //
      // Finish undo support:
      //
      
      support.finish();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
