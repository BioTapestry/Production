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

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.PertMappingDialog;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle perturbation data operations
*/

public class PerturbData extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum InfoType {
    EDIT("genePopup.EditPert", "genePopup.EditPert", "FIXME24.gif", "genePopup.EditPertMnem", null),
    PERT_MAP("genePopup.EditPertMap", "genePopup.EditPertMap", "FIXME24.gif", "genePopup.EditPertMnem", null),
    DELETE("genePopup.DeletePert", "genePopup.DeletePert", "FIXME24.gif", "genePopup.DeletePertMnem", null),
    DELETE_PERT_MAP("genePopup.DeletePertMap", "genePopup.DeletePertMap", "FIXME24.gif", "genePopup.DeletePertMapMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    InfoType(String name, String desc, String icon, String mnem, String accel) {
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
  
  private InfoType action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PerturbData(InfoType action) {
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
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
      case EDIT:
      case DELETE:
      case PERT_MAP:
        return (true);
      case DELETE_PERT_MAP:
        String baseID = GenomeItemInstance.getBaseID(inter.getObjectID());      
        PerturbationDataMaps pdms = rcx.getDataMapSrc().getPerturbationDataMaps(); 
        return (pdms.haveCustomMapForNode(baseID)); 
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext daxc) {  
    return (new StepState(action_, daxc));
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
        ans = new StepState(action_, cfh);
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

    private InfoType myAction_;
    private Intersection inter_;
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(InfoType action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(InfoType action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }

    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) { 
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
          isDone = editPert();
          break;
        case DELETE:           
          isDone = deletePert();
          break;  
        case PERT_MAP:           
          isDone = editPertMap();
          break;     
        case DELETE_PERT_MAP:           
          isDone = deletePertMap();
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
    ** Edit perturbation data
    */ 
       
    private boolean editPert() { 
      String id = inter_.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      id = GenomeItemInstance.getBaseID(id);
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
      DBGenome dbgenome = dacx_.getGenomeSource().getRootDBGenome();

      //
      // If we have no data keys (including default), we need to force custom
      // map generation:
      //

      List<String> keys = pdms.getDataEntryKeysWithDefault(dbgenome, id, pd);
      if (keys.isEmpty()) {
        while (keys.isEmpty()) {       
          if (!doItGuts(pdms, node, id)) {
            return (false);
          }
          List<String> cek = pdms.getCustomDataEntryKeys(id);
          if (cek != null) {
            keys.addAll(cek);
          }
        }
      }
      
      PertFilterExpression pfe = null;
      int numKeys = keys.size();
      for (int i = 0; i < numKeys; i++) {
        String key = keys.get(i);
        PertFilter targFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, key);
        if (pfe == null) {
          pfe = new PertFilterExpression(targFilter);
        } else {
          pfe = new PertFilterExpression(PertFilterExpression.Op.OR_OP, pfe, targFilter);
        }
      }
      uics_.getCommonView().launchPerturbationsManagementWindow(pfe, dacx_, uics_, uFac_);
      return (true);
    }
    
    /***************************************************************************
    **
    ** Delete perturbation data
    */ 

    private boolean deletePert() { 
      String id = inter_.getObjectID();
      id = GenomeItemInstance.getBaseID(id);
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
      DBGenome dbgenome = dacx_.getGenomeSource().getRootDBGenome();
      
      if (pd != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteQPCRDat", dacx_);
        List<String> mapped = pdms.getDataEntryKeysWithDefault(dbgenome, id, pd);
        mapped = (mapped == null) ? new ArrayList<String>() : new ArrayList<String>(mapped);
        Iterator<String> mit = mapped.iterator();
        while (mit.hasNext()) {
          String entryID = mit.next();
          PertDataChange[] changes = pdms.dropDanglingMapsForEntry(entryID);
          support.addEdits(PertDataChangeCmd.wrapChanges(changes));       
          PertFilter targFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, entryID);
          PertFilterExpression pfe = new PertFilterExpression(targFilter);
          PertDataChange dc = pd.dropPertDataForFilter(pfe);
          support.addEdit(new PertDataChangeCmd(dc));
        }
        support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
        support.finish();

      }
      return (true);
    }
  
    /***************************************************************************
    **
    ** Delete perturbation map
    */ 
       
    private boolean deletePertMap() {
      String id = inter_.getObjectID();
      id = GenomeItemInstance.getBaseID(id);
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
      
      if (pd != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteQPCRM", dacx_);          
        PertDataChange chg1 = pdms.dropDataEntryKeys(id);
        if (chg1 != null) {
          support.addEdit(new PertDataChangeCmd(chg1, false));
        }
        PertDataChange chg2 = pdms.dropDataSourceKeys(id);
        if (chg2 != null) {
          support.addEdit(new PertDataChangeCmd(chg2, false));
        }
        if ((chg1 != null) || (chg1 != null)) {
          support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
          support.finish();
        }            
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Edit perturbation map
    */ 
       
    private boolean editPertMap() {
      String id = inter_.getObjectID();     
      Node node = dacx_.getCurrentGenome().getNode(id);
      id = GenomeItemInstance.getBaseID(id);
      PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
      return (doItGuts(pdms, node, id));
    }
    
    /***************************************************************************
    **
    ** Edit perturbation map guts
    */  
  
    private boolean doItGuts(PerturbationDataMaps pdms, Node node, String id) {
      List<String> entries = pdms.getCustomDataEntryKeys(id);
      List<String> srcEntries = pdms.getCustomDataSourceKeys(id);
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      
      PertMappingDialog qmd = new PertMappingDialog(uics_, pd, node.getName(), entries, srcEntries);
      qmd.setVisible(true);
      if (qmd.haveResult()) {     
        UndoSupport support = uFac_.provideUndoSupport("undo.qmd", dacx_);
        // FIXME A List of Objects; underlying code needs a parameterized type
        List eList = qmd.getEntryList();
        PertDataChange pdc = pdms.setEntryMap(id, eList);
        PertDataChangeCmd pdcc = new PertDataChangeCmd(pdc);
        support.addEdit(pdcc);

        // FIXME A List of Objects; underlying code needs a parameterized type
        List sList = qmd.getSourceList();
        pdc = pdms.setSourceMap(id, sList);
        pdcc = new PertDataChangeCmd(pdc);
        support.addEdit(pdcc);
        GeneralChangeEvent gcev = new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE);
        support.addEvent(gcev);
        support.finish();
        return (true);
      }
      return (false);
    }
  }
}
