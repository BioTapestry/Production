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

package org.systemsbiology.biotapestry.cmd.flow.editData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditGroupProperties;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputMappingDialog;
import org.systemsbiology.biotapestry.ui.dialogs.TemporalInputRegionMappingDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
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
  
  public TemporalInput(BTState appState, Action action) {
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
  ** Constructor 
  */ 
  
  public TemporalInput(BTState appState, Action action, EditGroupProperties.GroupArg pargs) {
    this(appState, action);
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    TemporalInputRangeData tird;
    switch (action_) {
      case EDIT: 
      case EDIT_MAP:
      case EDIT_REGION_MAP:
        return (true);
      case DELETE:
        tird = appState_.getDB().getTemporalInputRangeData();
        return ((tird != null) && tird.haveDataForNode(GenomeItemInstance.getBaseID(inter.getObjectID())));
      case DELETE_MAP:     
        tird = appState_.getDB().getTemporalInputRangeData();
       return ((tird != null) && tird.haveCustomMapForNode(GenomeItemInstance.getBaseID(inter.getObjectID())));
      case DELETE_REGION_MAP:
        tird = appState_.getDB().getTemporalInputRangeData();    
        return ((tird != null) && tird.haveCustomMapForRegion(Group.getBaseID(groupID_)));
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
    return (new StepState(appState_, action_, groupID_, dacx));
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
        ans = new StepState(appState_, action_, groupID_, cfh.getDataAccessContext());
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {

    private String nextStep_;
    private Action myAction_;
    private BTState appState_;
    private Intersection inter_;
    private String myGroupID_;
    private DataAccessContext dacx_;
    
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, Action action, String groupID, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      myGroupID_ = groupID;
      dacx_ = dacx;
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
      Node node = dacx_.getGenome().getNode(id);
      id = GenomeItemInstance.getBaseID(id);
      TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
      DBGenome rootGenome = dacx_.getDBGenome();
      boolean hasInputs = rootGenome.nodeHasInputs(id);
      List<String> targets = null;
      if (!hasInputs) {
        targets = rootGenome.getNodeTargetNames(id, true);

        if (targets.isEmpty()) {
          int result = JOptionPane.showConfirmDialog(appState_.getTopFrame(), dacx_.rMan.getString("tirdNoTargets.message"),
                                                     dacx_.rMan.getString("tirdNoTargets.title"),
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

      List<String> keys = tird.getTemporalInputRangeEntryKeysWithDefault(id);
      boolean waiting = ((keys == null) || keys.isEmpty());
      if (waiting) {
        while (waiting) {                   
          TemporalInputMappingDialog timd = new TemporalInputMappingDialog(appState_, dacx_, node.getName(), id);
          timd.setVisible(true);
          if (timd.wasCancelled()) {
            return (false);
          }
          keys = tird.getCustomTemporalInputRangeEntryKeys(id);
          waiting = ((keys == null) || keys.isEmpty());
          if (waiting) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                          rMan.getString("tirdNoKey.message"), 
                                          rMan.getString("tirdNoKey.title"),
                                          JOptionPane.ERROR_MESSAGE);
          }    
        }
      }        

      if (!hasInputs) {
        ResourceManager rMan = appState_.getRMan();
        int result = JOptionPane.showConfirmDialog(appState_.getTopFrame(), rMan.getString("tirdNoInputs.message"),
                                                   rMan.getString("tirdNoInputs.title"),
                                                   JOptionPane.YES_NO_OPTION);

        if ((result == -1) || (result == 1)) {
          return (false);
        }
      }
      TemporalInputDialog tid = TemporalInputDialog.temporalInputDialogWrapper(appState_, dacx_, keys, false);
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
      Node node = dacx_.getGenome().getNode(id);
      String baseID = GenomeItemInstance.getBaseID(id);
      TemporalInputMappingDialog timd = new TemporalInputMappingDialog(appState_, dacx_, node.getName(), baseID);
      timd.setVisible(true);
      return (true);
    }
   
    /***************************************************************************
    **
    ** Edit region map
    */ 
       
    private boolean editRegionMap() {    
      String gid = Group.getBaseID(myGroupID_);
      GenomeInstance gi = dacx_.getGenomeAsInstance();
      GenomeInstance root = gi.getVfgParentRoot();
      Group group = (root == null) ? gi.getGroup(gid) : root.getGroup(gid);          
      TemporalInputRegionMappingDialog tirmd = 
        new TemporalInputRegionMappingDialog(appState_, dacx_, gid, group.getName());
      tirmd.setVisible(true);
      return (true);
    }      
    
    /***************************************************************************
    **
    ** Delete region map
    */ 
       
    private boolean deleteRegionMap() {      
      String gid = Group.getBaseID(myGroupID_);
      TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
      if (tird != null) {
        TemporalInputChange tichg = tird.dropGroupMap(gid);
        if (tichg != null) {
          UndoSupport support = new UndoSupport(appState_, "undo.deleteTIRM");           
          TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(appState_, dacx_, tichg, false);
          support.addEdit(cmd);
          support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
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
      TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
      if (tird != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.deleteTIM");
        TemporalInputChange chg1 = tird.dropDataEntryKeys(baseID);
        if (chg1 != null) {
          support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, chg1, false));
        }
        TemporalInputChange chg2 = tird.dropDataSourceKeys(baseID);
        if (chg2 != null) {
          support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, chg2, false));
        }
        if ((chg1 != null) || (chg1 != null)) {
          support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
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
      TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
      if (tird != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.deleteTIDat");
        List<String> mapped = tird.getTemporalInputRangeEntryKeysWithDefault(baseID);
        mapped = (mapped == null) ? new ArrayList<String>() : new ArrayList<String>(mapped);
        Iterator<String> mit = mapped.iterator();
        while (mit.hasNext()) {
          String name = mit.next();
          // FIX ME:  Drop dangling maps too????
          TemporalInputChange[] changes = tird.dropMapsTo(name);
          for (int j = 0; j < changes.length; j++) {
            support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, changes[j]));
          }
          TemporalInputChange tcc = tird.dropEntry(name);
          support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tcc)); 
        }
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
        support.finish();
      }
      return (true);
    }   
  }
}
