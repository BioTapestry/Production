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

package org.systemsbiology.biotapestry.cmd.flow.editData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditGroupProperties;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.PerturbExpressionEntryDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseEntryDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseMappingDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseRegionMappingTableDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseSetupDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle time course data operations
*/

public class TimeCourse extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Action {
    EDIT("nodePopup.EditTimeCourse", "nodePopup.EditTimeCourse", "FIXME24.gif", "nodePopup.EditTimeCourseMnem", null),
    EDIT_PERT("nodePopup.ManagePerturbedTimeCourses", "nodePopup.ManagePerturbedTimeCourses", "FIXME24.gif", "nodePopup.ManagePerturbedTimeCoursesMnem", null),
    EDIT_MAP("genePopup.EditTimeCourseMap", "genePopup.EditTimeCourseMap", "FIXME24.gif", "genePopup.EditTimeCourseMapMnem", null),
    DELETE("genePopup.DeleteTimeCourse", "genePopup.DeleteTimeCourse", "FIXME24.gif", "genePopup.DeleteTimeCourseMnem", null),
    DELETE_MAP("genePopup.DeleteTimeCourseMap", "genePopup.DeleteTimeCourseMap", "FIXME24.gif", "genePopup.DeleteTimeCourseMapMnem", null),
    EDIT_REGION_MAP("groupPopup.RegionMap", "groupPopup.RegionMap", "FIXME24.gif", "groupPopup.RegionMapMnem", null),
    DELETE_REGION_MAP("groupPopup.DeleteRegionMap", "groupPopup.DeleteRegionMap", "FIXME24.gif", "groupPopup.DeleteRegionMapMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    Action(String name, String desc, String icon, String mnem, String accel) {
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
  
  private Action action_;
  private String groupID_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TimeCourse(Action action) {
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TimeCourse(Action action, EditGroupProperties.GroupArg pargs) {
    this(action);
    if ((action != Action.DELETE_REGION_MAP) && (action != Action.EDIT_REGION_MAP)) {
      throw new IllegalArgumentException();
    }
    if (pargs.getForDeferred()) {
      throw new IllegalArgumentException();
    }
    groupID_ = pargs.getGroupID();
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    TimeCourseData tcd;
    TimeCourseDataMaps tcdm;
    switch (action_) {
      case EDIT:
      case EDIT_MAP:
      case EDIT_REGION_MAP:
        return (true);
      case EDIT_PERT:   
        tcd = rcx.getExpDataSrc().getTimeCourseData();
        tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
        return (tcdm.haveDataForNode(tcd, GenomeItemInstance.getBaseID(inter.getObjectID()), rcx.getGenomeSource()));
      case DELETE:      
        tcd = rcx.getExpDataSrc().getTimeCourseData();
        tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
        return ((tcd != null) && tcdm.haveDataForNode(tcd, GenomeItemInstance.getBaseID(inter.getObjectID()), rcx.getGenomeSource()));
      case DELETE_MAP:      
        tcd = rcx.getExpDataSrc().getTimeCourseData();
        tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
        return ((tcd != null) && tcdm.haveCustomMapForNode(GenomeItemInstance.getBaseID(inter.getObjectID()))); 
      case DELETE_REGION_MAP:         
        tcd = rcx.getExpDataSrc().getTimeCourseData();
        tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
        return ((tcd != null) && tcdm.haveCustomMapForRegion(Group.getBaseID(groupID_)));
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
    return (new StepState(action_, groupID_, dacx));
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
        ans = new StepState(action_, groupID_, cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {

    private Action myAction_;
    private Intersection inter_;
    private String myGroupID_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(Action action, String groupID, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      myGroupID_ = groupID;
    }
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(Action action, String groupID, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      myGroupID_ = groupID;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      if ((myAction_ == Action.DELETE_REGION_MAP) || (myAction_ == Action.EDIT_REGION_MAP)) {
        return;
      }
      inter_ = inter;
      return;
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
      boolean isDone;
      switch (myAction_) {
        case EDIT:           
          isDone = editData();
          break;
        case EDIT_PERT:           
          isDone = editPertData();
          break;
        case DELETE:           
          isDone = deleteData();
          break;  
        case EDIT_MAP:           
          isDone = editMap();
          break;     
        case DELETE_MAP:           
          isDone = deleteMap();
          break;  
        case DELETE_REGION_MAP:           
          isDone = deleteRegionMap();
          break; 
        case EDIT_REGION_MAP:           
          isDone = editRegionMap();
          break;            
        default:
          throw new IllegalStateException();
      }
      if (isDone) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } else {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));      
      }
    }
    
    /***************************************************************************
    **
    ** Edit data
    */ 
       
    private boolean editData() { 
      String id = inter_.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      ResourceManager rMan = dacx_.getRMan();
      id = GenomeItemInstance.getBaseID(id);
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();

      UiUtil.fixMePrintout("Getting null ptr here trying to edit data");
      if (!tcd.hasGeneTemplate()) { 
        TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeSourceSetupDialogWrapper(uics_, dacx_, uFac_);
        if (tcsd == null) {
          return (false);
        }
        tcsd.setVisible(true);
        if (!tcd.hasGeneTemplate()) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tcdNoTemplate.message"), 
                                        rMan.getString("tcdNoTemplate.title"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        } 
      }

      //
      // If we have no data keys (including default), we need to force custom
      // map generation:
      //
        
      List<TimeCourseDataMaps.TCMapping> keys = tcdm.getTimeCourseTCMDataKeysWithDefault(id, dacx_.getGenomeSource());
      boolean waiting = ((keys == null) || keys.isEmpty());
      if (waiting) {
        while (waiting) {                               
          TimeCourseMappingDialog tcmd = new TimeCourseMappingDialog(uics_, tcd, tcdm, node.getName(), id);
          tcmd.setVisible(true);
          if (!tcmd.haveResult()) {
            return (false);
          }
          UndoSupport support = uFac_.provideUndoSupport("undo.tcmd", dacx_);    
          List<TimeCourseDataMaps.TCMapping> entries = tcmd.getEntryList();
          TimeCourseChange tcc = tcdm.addTimeCourseTCMMap(id, entries, true);
          if (tcc != null) {
            support.addEdit(new TimeCourseChangeCmd(dacx_, tcc));
          } 
          support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
          support.finish();
          keys = tcdm.getCustomTCMTimeCourseDataKeys(id);
          waiting = ((keys == null) || keys.isEmpty());
          if (waiting) {
            JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                          rMan.getString("tcdNoKey.message"), 
                                          rMan.getString("tcdNoKey.title"),
                                          JOptionPane.ERROR_MESSAGE);
          }              
        }
      }

      TimeCourseEntryDialog tced = new TimeCourseEntryDialog(uics_, dacx_, keys, false, uFac_);
      tced.setVisible(true);
      return (true);
    }
    
    /***************************************************************************
    **
    ** Edit perturbed  data
    */ 

    private boolean editPertData() { 
      String id = inter_.getObjectID();
      id = GenomeItemInstance.getBaseID(id);
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      List<TimeCourseDataMaps.TCMapping> keys = tcdm.getTimeCourseTCMDataKeysWithDefault(id, dacx_.getGenomeSource());
      PerturbExpressionEntryDialog tced = PerturbExpressionEntryDialog.perturbExpressionEntryDialogWrapper(uics_, dacx_, keys, uFac_);
      if (tced != null) {
        tced.setVisible(true);
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Delete  data
    */ 

    private boolean deleteData() { 
      String id = inter_.getObjectID();
      id = GenomeItemInstance.getBaseID(id);
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      if (tcd != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteTCDat", dacx_);
        List<TimeCourseDataMaps.TCMapping> mapped = tcdm.getTimeCourseTCMDataKeysWithDefault(id, dacx_.getGenomeSource());
        mapped = (mapped == null) ? new ArrayList<TimeCourseDataMaps.TCMapping>() : TimeCourseDataMaps.TCMapping.cloneAList(mapped);
        Iterator<TimeCourseDataMaps.TCMapping> mit = mapped.iterator();
        while (mit.hasNext()) {
          TimeCourseDataMaps.TCMapping tcm = mit.next();
          TimeCourseChange[] changes = tcdm.dropMapsTo(tcm.name);
          for (int j = 0; j < changes.length; j++) {
            support.addEdit(new TimeCourseChangeCmd(dacx_, changes[j]));
          }
          TimeCourseChange tcc = tcd.dropGene(tcm.name);
          support.addEdit(new TimeCourseChangeCmd(dacx_, tcc)); 
        }
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
        support.finish();
      }
      return (true);
    }
  
    /***************************************************************************
    **
    ** Delete  map
    */ 
       
    private boolean deleteMap() {
      String id = inter_.getObjectID();
      id = GenomeItemInstance.getBaseID(id);
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      if (tcd != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteTCM", dacx_);          
        TimeCourseChange tchg = tcdm.dropDataKeys(id);
        if (tchg != null) {
          TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(dacx_, tchg, false);
          support.addEdit(cmd);
          support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
          support.finish();
        }
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Edit map
    */ 
       
    private boolean editMap() {
      String id = inter_.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      id = GenomeItemInstance.getBaseID(id);
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      TimeCourseMappingDialog tcmd = new TimeCourseMappingDialog(uics_, dacx_, node.getName(), id);
      tcmd.setVisible(true);
      if (!tcmd.haveResult()) {
        return (false);
      }
      UndoSupport support = uFac_.provideUndoSupport("undo.tcmd", dacx_);    
      List<TimeCourseDataMaps.TCMapping> entries = tcmd.getEntryList();
      TimeCourseChange tcc = tcdm.addTimeCourseTCMMap(id, entries, true);
      if (tcc != null) {
        support.addEdit(new TimeCourseChangeCmd(dacx_, tcc));
      } 
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();
      return (true);
    }
    
    /***************************************************************************
    **
    ** Delete region map
    */ 
       
    private boolean deleteRegionMap() { 
      String gid = Group.getBaseID(myGroupID_);      
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      if (tcd != null) {
        TimeCourseChange tchg = tcdm.dropGroupMap(gid);
        if (tchg != null) {
          UndoSupport support = uFac_.provideUndoSupport("undo.deleteTCRM", dacx_); 
          TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(dacx_, tchg, false);
          support.addEdit(cmd);
          support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
          support.finish();
        }
      }
      return (true);
    }
   
    /***************************************************************************
    **
    ** Edit region map
    */ 
       
    private boolean editRegionMap() { 
      String gid = Group.getBaseID(myGroupID_);     
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      if (!tcd.hasGeneTemplate()) { 
        TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeSourceSetupDialogWrapper(uics_, dacx_, uFac_);
        if (tcsd == null) {
          return (false);
        }
        tcsd.setVisible(true);
        if (!tcd.hasGeneTemplate()) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tcdNoTemplate.message"), 
                                        rMan.getString("tcdNoTemplate.title"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        } 
      }

      GenomeInstance gi = dacx_.getCurrentGenomeAsInstance();
      gid = Group.getBaseID(gid);
      GenomeInstance root = gi.getVfgParentRoot();      
      Group group = (root == null) ? gi.getGroup(gid) : root.getGroup(gid);          
      TimeCourseRegionMappingTableDialog tcrmd = new TimeCourseRegionMappingTableDialog(uics_, dacx_, gid, group.getName(), uFac_);
      tcrmd.setVisible(true);
      return (true);
    }
  }
}
