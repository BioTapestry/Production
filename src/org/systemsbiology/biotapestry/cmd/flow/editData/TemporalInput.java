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
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditGroupProperties;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData.HoldForBuild;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputMappingDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputRegionMappingDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle time course data operations
*/

public class TemporalInput extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Action {
    EDIT("nodePopup.EditTemporalInput", "nodePopup.EditTemporalInput", "FIXME24.gif", "nodePopup.EditTemporalInputMnem", null),
    EDIT_MAP("genePopup.EditTemporalInputMap", "genePopup.EditTemporalInputMap", "FIXME24.gif", "genePopup.EditTemporalInputMapMnem", null),
    DELETE("genePopup.DeleteTemporalInput", "genePopup.DeleteTemporalInput", "FIXME24.gif", "genePopup.DeleteTemporalInputMnem", null),
    DELETE_MAP("genePopup.DeleteTemporalInputMap", "genePopup.DeleteTemporalInputMap", "FIXME24.gif", "genePopup.DeleteTemporalInputMapMnem", null),
    EDIT_REGION_MAP("groupPopup.InputRegionMap", "groupPopup.InputRegionMap", "FIXME24.gif", "groupPopup.InputRegionMapMnem", null),
    DELETE_REGION_MAP("groupPopup.DeleteInputRegionMap", "groupPopup.DeleteInputRegionMap", "FIXME24.gif", "groupPopup.DeleteInputRegionMapMnem", null), 
    TEMPORAL_INPUT_DERIVE("command.TemporalInputDerive", "command.TemporalInputDerive", "FIXME24.gif", "command.TemporalInputDeriveMnem", null),
    TEMPORAL_INPUT_DROP_ALL("command.TemporalInputDropAll", "command.TemporalInputDropAll", "FIXME24.gif", "command.TemporalInputDropAllMnem", null),

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
  
  public TemporalInput(Action action) {
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
  
  public TemporalInput(Action action, EditGroupProperties.GroupArg pargs) {
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
    TemporalInputRangeData tird;
    switch (action_) {
      case EDIT: 
      case EDIT_MAP:
      case EDIT_REGION_MAP:
        return (true);
      case DELETE:
        tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
        return ((tird != null) && tird.haveDataForNode(GenomeItemInstance.getBaseID(inter.getObjectID()), rcx.getGenomeSource()));
      case DELETE_MAP:     
        tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
       return ((tird != null) && tird.haveCustomMapForNode(GenomeItemInstance.getBaseID(inter.getObjectID())));
      case DELETE_REGION_MAP:
        tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();    
        return ((tird != null) && tird.haveCustomMapForRegion(Group.getBaseID(groupID_)));
      case TEMPORAL_INPUT_DERIVE:
      case TEMPORAL_INPUT_DROP_ALL:
        return (false);
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
    if (action_ == Action.TEMPORAL_INPUT_DERIVE) {
        return (cache.haveTimeCourseData() && cache.genomeNotEmpty()); 
    } else if (action_ == Action.TEMPORAL_INPUT_DROP_ALL) {
        return (cache.haveTemporalInputData()); 
    } else {
      return (false);
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
        case TEMPORAL_INPUT_DERIVE:           
          isDone = temporalInputDerive();
          break; 
       case TEMPORAL_INPUT_DROP_ALL:   
          isDone = temporalInputDelete();
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
      id = GenomeItemInstance.getBaseID(id);
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      DBGenome rootGenome = dacx_.getDBGenome();
      boolean hasInputs = rootGenome.nodeHasInputs(id);
      List<String> targets = null;
      if (!hasInputs) {
        targets = rootGenome.getNodeTargetNames(id, true);

        if (targets.isEmpty()) {
          int result = JOptionPane.showConfirmDialog(uics_.getTopFrame(), dacx_.getRMan().getString("tirdNoTargets.message"),
                                                     dacx_.getRMan().getString("tirdNoTargets.title"),
                                                     JOptionPane.YES_NO_OPTION);

          if ((result == -1) || (result == 1)) {
            return (false);
          }
        }
      }

      //
      // If we have no data keys (including default), we need to force custom
      // map generation:
      //

      List<String> keys = tird.getTemporalInputRangeEntryKeysWithDefault(id, dacx_.getGenomeSource());
      boolean waiting = ((keys == null) || keys.isEmpty());
      if (waiting) {
        while (waiting) {                   
          TemporalInputMappingDialog timd = new TemporalInputMappingDialog(uics_, dacx_, uFac_, node.getName(), id);
          timd.setVisible(true);
          if (timd.wasCancelled()) {
            return (false);
          }
          keys = tird.getCustomTemporalInputRangeEntryKeys(id);
          waiting = ((keys == null) || keys.isEmpty());
          if (waiting) {
            ResourceManager rMan = dacx_.getRMan();
            JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                          rMan.getString("tirdNoKey.message"), 
                                          rMan.getString("tirdNoKey.title"),
                                          JOptionPane.ERROR_MESSAGE);
          }    
        }
      }        

      if (!hasInputs) {
        ResourceManager rMan = dacx_.getRMan();
        int result = JOptionPane.showConfirmDialog(uics_.getTopFrame(), rMan.getString("tirdNoInputs.message"),
                                                   rMan.getString("tirdNoInputs.title"),
                                                   JOptionPane.YES_NO_OPTION);

        if ((result == -1) || (result == 1)) {
          return (false);
        }
      }
      TemporalInputDialog tid = TemporalInputDialog.temporalInputDialogWrapper(uics_, dacx_, tSrc_, keys, false, uFac_);
      if (tid != null) {
        tid.setVisible(true);
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
      String baseID = GenomeItemInstance.getBaseID(id);
      TemporalInputMappingDialog timd = new TemporalInputMappingDialog(uics_, dacx_, uFac_, node.getName(), baseID);
      timd.setVisible(true);
      return (true);
    }
   
    /***************************************************************************
    **
    ** Edit region map
    */ 
       
    private boolean editRegionMap() {    
      String gid = Group.getBaseID(myGroupID_);
      GenomeInstance gi = dacx_.getCurrentGenomeAsInstance();
      GenomeInstance root = gi.getVfgParentRoot();
      Group group = (root == null) ? gi.getGroup(gid) : root.getGroup(gid);          
      TemporalInputRegionMappingDialog tirmd = 
        new TemporalInputRegionMappingDialog(uics_, dacx_, uFac_, gid, group.getName());
      tirmd.setVisible(true);
      return (true);
    }      
    
    /***************************************************************************
    **
    ** Delete region map
    */ 
       
    private boolean deleteRegionMap() {      
      String gid = Group.getBaseID(myGroupID_);
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      if (tird != null) {
        TemporalInputChange tichg = tird.dropGroupMap(gid);
        if (tichg != null) {
          UndoSupport support = uFac_.provideUndoSupport("undo.deleteTIRM", dacx_);           
          TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(dacx_, tichg, false);
          support.addEdit(cmd);
          support.addEvent(new GeneralChangeEvent(0, dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
          support.finish();          
        }
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private boolean deleteMap() {
      String id = inter_.getObjectID();
      String baseID = GenomeItemInstance.getBaseID(id);
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      if (tird != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteTIM", dacx_);
        TemporalInputChange chg1 = tird.dropDataEntryKeys(baseID);
        if (chg1 != null) {
          support.addEdit(new TemporalInputChangeCmd(dacx_, chg1, false));
        }
        TemporalInputChange chg2 = tird.dropDataSourceKeys(baseID);
        if (chg2 != null) {
          support.addEdit(new TemporalInputChangeCmd(dacx_, chg2, false));
        }
        if ((chg1 != null) || (chg1 != null)) {
          support.addEvent(new GeneralChangeEvent(0, dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
          support.finish();
        }          
      }
      return (true);    
    }
  
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private boolean deleteData() {
      String id = inter_.getObjectID();
      String baseID = GenomeItemInstance.getBaseID(id);
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      if (tird != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteTIDat", dacx_);
        List<String> mapped = tird.getTemporalInputRangeEntryKeysWithDefault(baseID, dacx_.getGenomeSource());
        mapped = (mapped == null) ? new ArrayList<String>() : new ArrayList<String>(mapped);
        Iterator<String> mit = mapped.iterator();
        while (mit.hasNext()) {
          String name = mit.next();
          // FIX ME:  Drop dangling maps too????
          TemporalInputChange[] changes = tird.dropMapsTo(name);
          for (int j = 0; j < changes.length; j++) {
            support.addEdit(new TemporalInputChangeCmd(dacx_, changes[j]));
          }
          TemporalInputChange tcc = tird.dropEntry(name);
          support.addEdit(new TemporalInputChangeCmd(dacx_, tcc)); 
        }
        support.addEvent(new GeneralChangeEvent(0, dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
        support.finish();
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private boolean temporalInputDerive() {
      DatabaseChange dc = null;
      UndoSupport support = uFac_.provideUndoSupport("undo.deriveTemporalInputData", dacx_);
      TemporalInputRangeData nowTird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      TemporalInputRangeData tird;
      if (nowTird.haveMaps()) {
        tird = nowTird.extractOnlyMaps();    
      } else {
        tird = new TemporalInputRangeData(dacx_);
        tird.buildMapsFromTCDMaps(dacx_);
      }
      
      List<HoldForBuild> hfbs = tird.prepForBuildFromTCD(dacx_);
      if (hfbs == null) {
        ResourceManager rMan = dacx_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tirdDerive.oneToManyMessage"), 
                                      rMan.getString("tirdDerive.oneToManyMessageTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false); 
      }
      dc = dacx_.getTemporalRangeSrc().startTemporalInputUndoTransaction();
      dacx_.getTemporalRangeSrc().setTemporalInputRangeData(tird);    
      tird.buildFromTCD(dacx_, hfbs);   
      dc = dacx_.getTemporalRangeSrc().finishTemporalInputUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      support.addEvent(new GeneralChangeEvent(0, dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      support.finish();
      //
      // There was previously a question here about needing to clear proxy caches and
      // refresh the current model. The above GeneralChangeEvent handles this.
      //
      return (true);    
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private boolean temporalInputDelete() {
      UndoSupport support = uFac_.provideUndoSupport("undo.dropAllTemporalInputData", dacx_);
      DatabaseChange dc = dacx_.getTemporalRangeSrc().startTemporalInputUndoTransaction();
      dacx_.getTemporalRangeSrc().setTemporalInputRangeData(new TemporalInputRangeData(dacx_));     
      dc = dacx_.getTemporalRangeSrc().finishTemporalInputUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      support.addEvent(new GeneralChangeEvent(0, dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      support.finish();
      //
      // There was previously a question here about needing to clear proxy caches and
      // refresh the current model. The above GeneralChangeEvent handles this.
      //
      return (true);    
    }
  }
}
