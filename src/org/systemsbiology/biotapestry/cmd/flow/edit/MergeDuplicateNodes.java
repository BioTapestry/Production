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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.SuperAdd;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNode;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.Layout.PropChange;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle merging nodes that are duplicates
*/

public class MergeDuplicateNodes extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MergeDuplicateNodes() {
    name = "nodePopup.MergeNodes";
    desc = "nodePopup.MergeNodes";
    mnem = "nodePopup.MergeNodesMnem";             
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    CheckGutsCache cache = new CheckGutsCache(uics, rcx, CheckGutsCache.Checktype.GENERAL);
    return (cache.haveANodeSelection() && cache.genomeIsRoot());
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new StepState(dacx));  
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalArgumentException();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("mergeDups")) {
          next = ans.mergeDups();          
        } else if (ans.getNextStep().equals("stepToDeny")) {
          next = ans.stepToDeny();
        } else if (ans.getNextStep().equals("stepToWarn")) {
          next = ans.stepToWarn();       
        } else if (ans.getNextStep().equals("stepToDataWarn")) {
          next = ans.stepToDataWarn();
        } else if (ans.getNextStep().equals("stepToPrep")) {
          next = ans.stepToPrep();
        } else if (ans.getNextStep().equals("stepToHandlePartialResponse")) {
          next = ans.stepToHandlePartialResponse(last);        
        } else if (ans.getNextStep().equals("stepToHandleDataResponse")) {
          next = ans.stepToHandleDataResponse(last);    
        } else if (ans.getNextStep().equals("stepWillExit")) {
          next = ans.stepWillExit();  
        } else {
          throw new IllegalStateException();
        }
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.MultiSelectCmdState, DialogAndInProcessCmd.PopupPointCmdState {
     
    private Set<String> nodes_;
    private Set<String> genes_;
    private Map<String, Layout.PadNeedsForLayout> globalPadNeeds_;
    private HashMap<String, String> mergeMap_;
    private Map<String, List<FullGenomeHierarchyOracle.NodeUsage>> nuMap_;
    private Intersection inter_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepToDeny";
    }
    
    /***************************************************************************
    ** 
    ** for preload
    */ 
      
    public void setMultiSelections(Set<String> genes, Set<String> nodes, Set<String> links) {
      nodes_ = nodes;
      genes_ = genes;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      inter_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setPopupPoint(Point2D ptp) {
      return;
    }
 
    /***************************************************************************
    **
    ** We will only proceed if nobody is a gene, and same type, and there is more than one.
    */ 
       
    private DialogAndInProcessCmd stepToDeny() {
  
      //
      // The gottaGene case should not happen, since we do not make this command part of the popup menu for genes!
      //
      
      Node node = dacx_.getCurrentGenome().getNode(inter_.getObjectID());
      int popType = node.getNodeType();
      boolean gottaGene = (popType == Node.GENE);
      if (!genes_.isEmpty() || gottaGene) {
        ResourceManager rMan = dacx_.getRMan();
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("mergeWarning.geneDeny"), rMan.getString("mergeWarning.geneDenyTitle"));
        nextStep_ = "stepWillExit";
        return (new DialogAndInProcessCmd(suf, this)); 
      }
      
      //
      // If there is one selection, and it is the same node as the popup, we are outta here.
      //
      
      if ((nodes_.size() == 1) && nodes_.contains(inter_.getObjectID())) {
        ResourceManager rMan = dacx_.getRMan();
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("mergeWarning.singleDeny"), rMan.getString("mergeWarning.singleDenyTitle"));
        nextStep_ = "stepWillExit";
        return (new DialogAndInProcessCmd(suf, this)); 
      }

      //
      // If the genes are mixed types, we will not allow. Without this, nodes in lower models continue to look like
      // their former self (is that a rendering problem or a type problem? Don't care, just won't allow)
      //
     
      for (String nodeID : nodes_) {
        Node testNode = dacx_.getCurrentGenome().getNode(nodeID);
        if (popType != testNode.getNodeType()) {
          ResourceManager rMan = dacx_.getRMan();
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("mergeWarning.mixedTypeDeny"), rMan.getString("mergeWarning.mixedTypeDenyTitle"));
          nextStep_ = "stepWillExit";
          return (new DialogAndInProcessCmd(suf, this));           
        }
      }

      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToWarn";   
      return (daipc); 
    }  
    
    /***************************************************************************
    **
    ** Done
    */
           
    private DialogAndInProcessCmd stepWillExit() { 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
             
    /***************************************************************************
    **
    ** Warn user of underlying instruction list
    */ 
       
    private DialogAndInProcessCmd stepToWarn() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan();
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, rMan.getString("instructWarning.changeMessage"), rMan.getString("instructWarning.changeTitle"));     
        daipc = new DialogAndInProcessCmd(suf, this);      
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      } 
      nextStep_ = "stepToPrep";   
      return (daipc); 
    }
    
    /***************************************************************************
    **
    ** Does user want to cancel?
    */   
   
    private DialogAndInProcessCmd stepToHandlePartialResponse(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.NO) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "stepToDataWarn"; 
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Does user want to cancel?
    */   
   
    private DialogAndInProcessCmd stepToHandleDataResponse(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.NO) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "mergeDups"; 
      }
      return (retval);
    }

    /***************************************************************************
    **
    ** We are NOT deleting any underlying data tables when merged nodes are deleted. Tell
    ** the user to handle it themselves if they want
    */ 
       
    private DialogAndInProcessCmd stepToDataWarn() {     
      boolean warnUser = false;
      HashSet<String> deadSet = new HashSet<String>(mergeMap_.keySet());
      for (String deadID : deadSet) {
        String deadName = dacx_.getCurrentGenome().getNode(deadID).getRootName();
        TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
        TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();  
        PerturbationData pd = dacx_.getExpDataSrc().getPertData();
        PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
        if (((tcd != null) && tcd.haveDataForNodeOrName(deadID, deadName, dacx_.getDataMapSrc())) ||
            ((tird != null) && tird.haveDataForNodeOrName(deadID, deadName)) ||
            ((pd != null) && pd.haveDataForNode(deadID, null, pdms))) {
          warnUser = true;
          break;
        }
      }     
  
      DialogAndInProcessCmd daipc;
      if (warnUser) {
        ResourceManager rMan = dacx_.getRMan();
        String message = UiUtil.convertMessageToHtml(rMan.getString("mergeWarning.dataMessage"));
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, rMan.getString("mergeWarning.dataTitle"));
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToHandleDataResponse";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "mergeDups";
      }   
      return (daipc); 
    }
    
   /***************************************************************************
   **
   ** Invert map: who is merging into given targets?
   */ 
      
    private Map<String, Set<String>> invertMergeMap(Map<String, String> origMap) {    
      HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
      for (String fromNode : origMap.keySet()) {
        String toNode = origMap.get(fromNode);
        Set<String> fromNodes = retval.get(toNode);
        if (fromNodes == null) {
          fromNodes = new HashSet<String>();
          retval.put(toNode, fromNodes);
        }
        fromNodes.add(fromNode);
      }
      return (retval);
    }
      
    /***************************************************************************
    **
    ** Merge data maps
    */ 
       
    private void mergeTCMaps(UndoSupport support) {
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      if (((tcd == null) || !tcd.haveDataEntries()) && !tcdm.haveData()) {
        return;
      }
      boolean aChange = false;
      boolean needDefaultToo = false;
     
      Map<String, Set<String>> fromForTo = invertMergeMap(mergeMap_);
      for (String toNode : fromForTo.keySet()) {
        List<TimeCourseDataMaps.TCMapping> mapped = tcdm.getCustomTCMTimeCourseDataKeys(toNode);
        TimeCourseDataMaps.TCMapping tcmd = tcdm.getTimeCourseDefaultMap(toNode, dacx_.getGenomeSource());
        HashSet<TimeCourseDataMaps.TCMapping> mapSet = new HashSet<TimeCourseDataMaps.TCMapping>();
        if (mapped != null) {
          mapSet.addAll(mapped);
        }
        Set<String> fromNodes = fromForTo.get(toNode);
        for (String fromNode : fromNodes) {
          List<TimeCourseDataMaps.TCMapping> tcml = tcdm.getCustomTCMTimeCourseDataKeys(fromNode);
          if ((tcml != null) && !tcml.isEmpty()) {
            mapSet.addAll(tcml);
            needDefaultToo = true;
          }
        }
        if ((tcmd != null) && needDefaultToo) {
          mapSet.add(tcmd);
        }
        if (!mapSet.isEmpty()) {
          TimeCourseChange tcc = tcdm.addTimeCourseTCMMap(toNode, new ArrayList<TimeCourseDataMaps.TCMapping>(mapSet), true);
          if (tcc != null) {
            support.addEdit(new TimeCourseChangeCmd(dacx_, tcc));
            aChange = true;
          }
        } else {
          TimeCourseChange tchg = tcdm.dropDataKeys(toNode);
          if (tchg != null) {
            TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(dacx_, tchg, false);
            support.addEdit(cmd);
            aChange = true;
          }
        }
      }
      if (aChange) {
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      }
      return;
    } 
    
    /***************************************************************************
    **
    ** Get the default target name for the node ID.  May be null.
    */
    
    public String getDefaultMap(String nodeId) {
      Node node = dacx_.getCurrentGenome().getNode(nodeId);      
      if (node == null) { // for when node has been already deleted...
        throw new IllegalStateException();
      }      
      String nodeName = node.getRootName();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (null);
      } 
      return (nodeName);
    }

    /***************************************************************************
    **
    ** Merge data maps
    */ 
       
    private void mergeTIRDMaps(UndoSupport support) {
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      if (!tird.haveData()) {
        return;
      }
      boolean aChange = false;
      boolean needDefaultSToo = false;
      boolean needDefaultEToo = false;
      Map<String, Set<String>> fromForTo = invertMergeMap(mergeMap_);
      for (String toNode : fromForTo.keySet()) {
        List<String> mappedS = tird.getCustomTemporalInputRangeSourceKeys(toNode);
        String seDef = tird.getTemporalInputRangeDefaultMap(toNode, dacx_.getGenomeSource());
        List<String> mappedE = tird.getCustomTemporalInputRangeEntryKeys(toNode);
 
        HashSet<String> mapSetS = new HashSet<String>();
        if (mappedS != null) {
          mapSetS.addAll(mappedS);
        }
        HashSet<String> mapSetE = new HashSet<String>();
        if (mappedE != null) {
          mapSetE.addAll(mappedE);
        }
        
        Set<String> fromNodes = fromForTo.get(toNode);
        for (String fromNode : fromNodes) {
          List<String> tirS = tird.getCustomTemporalInputRangeSourceKeys(fromNode);
          if ((tirS != null) && !tirS.isEmpty()) {
            mapSetS.addAll(tirS);
            needDefaultSToo = true;
          }
          List<String> tirE = tird.getCustomTemporalInputRangeEntryKeys(fromNode);
          if ((tirE != null) && !tirE.isEmpty()) {
            mapSetE.addAll(tirE);
            needDefaultEToo = true;
          } 
        }

        if ((seDef != null) && needDefaultSToo) {
          mapSetS.add(seDef);
        }
        if ((seDef != null) && needDefaultEToo) {
          mapSetE.add(seDef);
        }

        if (!mapSetS.isEmpty() || !mapSetE.isEmpty()) {
          TemporalInputChange[] tic = tird.addTemporalInputRangeMaps(toNode, new ArrayList<String>(mapSetE), new ArrayList<String>(mapSetS));
          for (int i = 0; i < tic.length; i++) {
            support.addEdit(new TemporalInputChangeCmd(dacx_, tic[i]));
          }
          aChange = true;
        } 
        if (mapSetE.size() == 0) {
          TemporalInputChange tic = tird.dropDataEntryKeys(toNode);
          if (tic != null) {
            support.addEdit(new TemporalInputChangeCmd(dacx_, tic, false));
            aChange = true;
          }
        }   
        if (mapSetS.size() == 0) {
          TemporalInputChange tic = tird.dropDataSourceKeys(toNode);
          if (tic != null) {
            support.addEdit(new TemporalInputChangeCmd(dacx_, tic, false));
            aChange = true;
          }
        }
      }
      if (aChange) {
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Merge data maps
    */ 
       
    private void mergePertMaps(UndoSupport support) {
         
      PerturbationData pd = dacx_.getExpDataSrc().getPertData();
      PerturbationDataMaps pdms = dacx_.getDataMapSrc().getPerturbationDataMaps();
     
      boolean aChange = false;
      boolean needDefaultSToo = false;
      boolean needDefaultEToo = false;
      Map<String, Set<String>> fromForTo = invertMergeMap(mergeMap_);
      for (String toNode : fromForTo.keySet()) {
        List<String> srcEntries = pdms.getCustomDataSourceKeys(toNode);
        String sDef = getDefaultMap(toNode);
        if (!pd.getSourceNameSet().contains(sDef)) {
          sDef = null;
        }
        String eDef = getDefaultMap(toNode);
        if (!pd.getTargetSet().contains(eDef)) {
          eDef = null;
        }
        
        List<String> entries = pdms.getCustomDataEntryKeys(toNode);
   
        HashSet<String> mapSetS = new HashSet<String>();
        if (srcEntries != null) {
          mapSetS.addAll(srcEntries);
        }
        HashSet<String> mapSetE = new HashSet<String>();
        if (entries != null) {
          mapSetE.addAll(entries);
        }
        Set<String> fromNodes = fromForTo.get(toNode);
        for (String fromNode : fromNodes) {
          List<String> tirS = pdms.getCustomDataSourceKeys(fromNode);
          if ((tirS != null) && !tirS.isEmpty()) {
            mapSetS.addAll(tirS);
            needDefaultSToo = true;
          }
          List<String> tirE = pdms.getCustomDataEntryKeys(fromNode);
          if ((tirE != null) && !tirE.isEmpty()) {
            mapSetE.addAll(tirE);
            needDefaultEToo = true;
          }
        }

        if ((sDef != null) && needDefaultSToo) {
          mapSetS.add(sDef);
        }
        if ((eDef != null) && needDefaultEToo) {
          mapSetE.add(eDef);
        }

        if (!mapSetS.isEmpty()) {
          PertDataChange pdc = pdms.setSourceMap(toNode, new ArrayList<String>(mapSetS));
          if (pdc != null) {
            PertDataChangeCmd pdcc = new PertDataChangeCmd(dacx_, pdc);
            support.addEdit(pdcc);
            aChange = true;
          }
        } else {
          PertDataChange pdc = pdms.dropDataSourceKeys(toNode);
          if (pdc != null) {
            PertDataChangeCmd pdcc = new PertDataChangeCmd(dacx_, pdc);
            support.addEdit(pdcc);
            aChange = true; // Not strictly true: if it was empty before, it is still empty, so no change....
          }
        }
    
        if (!mapSetE.isEmpty()) {
          PertDataChange pdc = pdms.setEntryMap(toNode, new ArrayList<String>(mapSetE));
          if (pdc != null) {
            PertDataChangeCmd pdcc = new PertDataChangeCmd(dacx_, pdc);
            support.addEdit(pdcc);
            aChange = true;
          }
        } else {         
          PertDataChange pdc = pdms.dropDataEntryKeys(toNode);
          if (pdc != null) {
            PertDataChangeCmd pdcc = new PertDataChangeCmd(dacx_, pdc);
            support.addEdit(pdcc);
            aChange = true; // Not strictly true: if it was empty before, it is still empty, so no change.... 
          }
        }
      }
      if (aChange) {
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      }
      return;
    } 
    
 
    /***************************************************************************
    **
    ** Prepare merge list
    */
      
    private DialogAndInProcessCmd stepToPrep() {  
      //
      // Root-level module layout can change due to loss of merged nodes. Record state to fix: 
      //
      
      globalPadNeeds_ = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();    
  
      //
      // The user-pulled-down node has to be last man standing. If it was not selected, add it to the pile
      // of nodes to process:
      //
      
      String lastNodeID  = inter_.getObjectID();
      if (!nodes_.contains(lastNodeID)) {
        nodes_ = new HashSet<String>(nodes_);
        nodes_.add(lastNodeID);
      }
      
      //
      // For each node, we need to find the instances in all submodels. Also, let's track
      // what overlay modules the various nodes belong to.
      //
 
      DBGenome dbg = dacx_.getDBGenome();
      Map<String, Map<String, Set<String>>> allNodesModMem = new HashMap<String, Map<String, Set<String>>>();
      HashSet<String> rootOnly = new HashSet<String>();
      
      ArrayList<FullGenomeHierarchyOracle.NodeUsage> nus = new ArrayList<FullGenomeHierarchyOracle.NodeUsage>();
      Iterator<String> nit = nodes_.iterator();
      while (nit.hasNext()) {
        String nodeID = nit.next();
        List<FullGenomeHierarchyOracle.NodeUsage> usages = dacx_.getFGHO().getAllNodeInstances(nodeID);
        if (usages.isEmpty()) {
          rootOnly.add(nodeID);
        } else {
          nus.addAll(usages);
        }
        Map<String, Set<String>> modMem = dbg.getModuleMembership(nodeID);
        allNodesModMem.put(nodeID, modMem);
      }
      
      nuMap_ = findModels(nus);
      
      //
      // Build a graph where a link indicates two nodes appear in the same group somewhere in
      // the hierarchy:
      //
  
      HashSet<String> allNodes = new HashSet<String>();
      HashSet<LexiLink> links = new HashSet<LexiLink>();
      for (String mid : nuMap_.keySet()) {
        List<FullGenomeHierarchyOracle.NodeUsage> nupm = nuMap_.get(mid);
        findLinksForModel(nupm, links, allNodes);
      }
      
      //
      // Generate a complete graph:
      //
      
      Set<LexiLink> fullSetOfLinks = fullGraph(allNodes);
      
      //
      // Generate a graph where there is a link for every potential merge:
      //
      
      fullSetOfLinks.removeAll(links);
      SortedSet<LexiLink> reduced = new TreeSet<LexiLink>(fullSetOfLinks);
      
      mergeMap_ = new HashMap<String, String>();
      while (!reduced.isEmpty()) {
        LexiLink merge = reduced.first();
        mergeMap_.put(merge.getTrg(), merge.getSrc());
        // Collapse the nodes:
        allNodes.remove(merge.getTrg());
        // Collapse the links:
        SortedSet<LexiLink> nextLinks = new TreeSet<LexiLink>();
        for (LexiLink lexi : links) {
          if (merge.getTrg().equals(lexi.getSrc())) {
            nextLinks.add(new LexiLink(merge.getSrc(), lexi.getTrg()));
          } else if (merge.getTrg().equals(lexi.getTrg())) {
            nextLinks.add(new LexiLink(lexi.getSrc(), merge.getSrc()));
          } else {
            nextLinks.add(lexi);
          }
        }
        fullSetOfLinks = fullGraph(allNodes);
        fullSetOfLinks.removeAll(nextLinks);
        reduced = new TreeSet<LexiLink>(fullSetOfLinks);
      }
      
      //
      // Nodes that exist in *just* the root model need to be added in!
      //    
      
      HashSet<String> handled = new HashSet<String>(mergeMap_.keySet());
      handled.addAll(mergeMap_.values());
      boolean emptyMerge = mergeMap_.isEmpty();
      String mergeTo = (emptyMerge) ? nodes_.iterator().next() : mergeMap_.values().iterator().next();        
      Iterator<String> nit2 = nodes_.iterator();
      while (nit2.hasNext()) {
        String nodeID = nit2.next();
        if (nodeID.equals(mergeTo)) {
          continue;
        }
        if (!handled.contains(nodeID) && rootOnly.contains(nodeID)) {
          mergeMap_.put(nodeID, mergeTo); 
        }
      }
      
      //
      // The node that the user used to launch the merge must be one of the nodes left standing. If it is scheduled for
      // elimination, we swap it with somebody else:
      //

      if (mergeMap_.keySet().contains(lastNodeID)) {
        String gottaSwap = mergeMap_.get(lastNodeID);
        HashMap<String, String> swappedMergeMap = new HashMap<String, String>();
        for (String mid : mergeMap_.keySet()) {
          String checkie = mergeMap_.get(mid);
          if (mid.equals(lastNodeID)) {
            swappedMergeMap.put(checkie, lastNodeID);
          } else if (checkie.equals(gottaSwap)) {
            swappedMergeMap.put(mid, lastNodeID);
          }
        }
        mergeMap_ = swappedMergeMap;
      }
      
      //
      // Figure out if the merge is "partial", i.e. the set of n selected nodes maps down
      // to more than one final node.
      //
     
      HashSet<String> results = new HashSet<String>();
      for (String nodeID : nodes_) {
        String converted = mergeMap_.get(nodeID);
        if (converted == null) {
          results.add(nodeID);
        } else {
          results.add(converted);
        }
      }   
 
      if (results.size() > 1) {
        ResourceManager rMan = dacx_.getRMan();
        String message = UiUtil.convertMessageToHtml(rMan.getString("mergeWarning.partialMerge"));
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, rMan.getString("mergeWarning.partialMergeTitle"));
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
        nextStep_ = "stepToHandlePartialResponse";
        return (retval); 
      }
      
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToDataWarn"; 
      return (daipc);
    }
   
    /***************************************************************************
    **
    ** OK, we have created the merge map. We are ready to roll!
    */
      
    private DialogAndInProcessCmd mergeDups() {
      
      DBGenome dbg = dacx_.getDBGenome();
      HashSet<String> changingGs = new HashSet<String>(); 
      HashSet<String> changingLOs = new HashSet<String>(); 
      
      //
      // Record all the links in and out of the merge targets now so we can use them for merging duplicate
      // links in the end
      //
      
      HashSet<String> mergeToNodes = new HashSet<String>(mergeMap_.values());
      mergeToNodes.addAll(mergeMap_.keySet());
      HashSet<String> mergeLinkCandidates = new HashSet<String>();
      
      Iterator<Linkage> lit = dbg.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        if (mergeToNodes.contains(link.getSource()) || mergeToNodes.contains(link.getTarget())) {
          mergeLinkCandidates.add(link.getID());
        }
      }
 
      //
      // Figure out how to create new instances of the merge nodes in all the submodels:
      //
      
      HashMap<String, List<SuperAdd.SuperAddPair>> sapMap = new HashMap<String, List<SuperAdd.SuperAddPair>>();
      for (String merged : mergeMap_.keySet()) {
        List<SuperAdd.SuperAddPair> sapList = new ArrayList<SuperAdd.SuperAddPair>();
        sapMap.put(merged, sapList);
      } 
      
      Iterator<GenomeInstance> giit = dacx_.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        List<FullGenomeHierarchyOracle.NodeUsage> nusi = nuMap_.get(gi.getID());
        for (FullGenomeHierarchyOracle.NodeUsage nu : nusi) {
          String nodeSwitch = mergeMap_.get(nu.baseID);
          if (nodeSwitch != null) {    
            SuperAdd.SuperAddPair sap = new SuperAdd.SuperAddPair(gi.getID(), nu.groupID, nu.modMem, nu.subGroups);
            List<SuperAdd.SuperAddPair> sapList = sapMap.get(nu.baseID);
            sapList.add(sap);
            changingGs.add(gi.getID());
          }  
        }
      }
      
      //
      // What links will change? Those with mapped sources or targets:
      //
      
      ArrayList<String> srcChanges = new ArrayList<String>();
      ArrayList<String> trgChanges = new ArrayList<String>();     
      Iterator<Linkage> lit2 = dbg.getLinkageIterator();
      while (lit2.hasNext()) {
        Linkage link = lit2.next();
        String srcSwitch = mergeMap_.get(link.getSource());
        if (srcSwitch != null) {
          srcChanges.add(link.getID());
        }
        String trgSwitch = mergeMap_.get(link.getTarget());
        if (trgSwitch != null) {
          trgChanges.add(link.getID());
        }
      }    
      
      //
      // Start the edits:
      //
        
      UndoSupport support = uFac_.provideUndoSupport("undo.mergeNodes", dacx_);
      
      //
      // Add new nodes to instances. The SuperAddPairs also say which modules the new nodes need
      // to go into to retain the current module membership.
      //
        
      for (String toMerge : sapMap.keySet()) { 
        List<SuperAdd.SuperAddPair> sapList = sapMap.get(toMerge);
        String mergeTo = mergeMap_.get(toMerge);
        SuperAdd.superAddForNode(dacx_, mergeTo, sapList, support, uFac_);
      }

      //
      // Root model changes for links:
      //

      Layout rlo = dacx_.getCurrentLayout();
      for (String sCh : srcChanges) {
        Linkage link = dbg.getLinkage(sCh);
        String srcSwitch = mergeMap_.get(link.getSource());
        BusProperties bp = rlo.getLinkPropertiesForSource(srcSwitch);
        int lp = (bp != null) ? bp.getLaunchPad(dbg, rlo) : 0;
        GenomeChange gc = dbg.changeLinkageSourceNode(link, srcSwitch, lp);
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
        changingGs.add(dbg.getID());
      }
      for (String tCh : trgChanges) {
        Linkage link = dbg.getLinkage(tCh);
        String trgSwitch = mergeMap_.get(link.getTarget());
        PadCalculatorToo pcalc = new PadCalculatorToo();
        PadCalculatorToo.PadResult pads = pcalc.padCalc(dacx_.getGenomeSource(), link.getSource(), trgSwitch, null, false);
        GenomeChange gc = dbg.changeLinkageTargetNode(link, trgSwitch, pads.landing);
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
        changingGs.add(dbg.getID());
      } 
     
      //
      // Find what switched links are doing in the root instances. For each link with a changing
      // node, we need to find the node instance in the same region that it is changing to. Note that
      // this analysis must be done AFTER NODE CREATION, since we are looking for the new instances
      // that have been created.
      //
          
      HashSet<String> allChanges = new HashSet<String>(srcChanges);
      allChanges.addAll(trgChanges);     
     
      HashMap<String, Map<String, String>> sourcePlans = new HashMap<String, Map<String, String>>();
      HashMap<String, Map<String, String>> targPlans = new HashMap<String, Map<String, String>>();
      HashMap<String, Map<String, String>> nodeMoves = new HashMap<String, Map<String, String>>();
         
      giit = dacx_.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (!gi.isRootInstance()) { // Node group membership testing is only robust in root instances.
          continue;
        }
        HashMap<String, String> newSources = new HashMap<String, String>();
        sourcePlans.put(gi.getID(), newSources);
        HashMap<String, String> newTargs = new HashMap<String, String>();
        targPlans.put(gi.getID(), newTargs);
        HashMap<String, String> newMoves = new HashMap<String, String>();
        nodeMoves.put(gi.getID(), newMoves);
        for (String aCh : allChanges) {       
          Set<String> liSet = gi.returnLinkInstanceIDsForBacking(aCh); 
          for (String linkInstID : liSet) {       
            GenomeInstance.GroupTuple groupTup = gi.getRegionTuple(linkInstID);
            Linkage gilink = gi.getLinkage(linkInstID);
            String source = gilink.getSource();
            String srcSwitch = mergeMap_.get(GenomeItemInstance.getBaseID(source));
            if (srcSwitch != null) {
              int siNum = gi.getInstanceForNodeInGroup(srcSwitch, groupTup.getSourceGroup());
              String moved = GenomeItemInstance.getCombinedID(srcSwitch, Integer.toString(siNum));
              newSources.put(linkInstID, moved);
              newMoves.put(source, moved); 
            }
            String targ = gilink.getTarget();
            String trgSwitch = mergeMap_.get(GenomeItemInstance.getBaseID(targ));
            if (trgSwitch != null) {
              int trNum = gi.getInstanceForNodeInGroup(trgSwitch, groupTup.getTargetGroup());
              String moved = GenomeItemInstance.getCombinedID(trgSwitch, Integer.toString(trNum));
              newTargs.put(linkInstID, moved);
              newMoves.put(targ, moved);             
            }
          }
        }
      }

      //
      // Linkage resets for instances:
      //
 
      giit = dacx_.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        String gID = ((gi.isRootInstance()) ? gi : gi.getVfgParentRoot()).getID();
        Map<String, String> newSources = sourcePlans.get(gID);
        Map<String, String> newTargs = targPlans.get(gID); 
        DataAccessContext rcxi = new StaticDataAccessContext(dacx_, gi.getID());
        for (String lkey : newSources.keySet()) {
          Linkage childLink = gi.getLinkage(lkey);
          if (childLink != null) { // Those darn VfNs!
            String newSrc = newSources.get(lkey);
            GenomeChange gc = gi.changeLinkageSourceNode(childLink, newSrc, childLink.getLaunchPad());
            GenomeChangeCmd gcc = new GenomeChangeCmd(rcxi, gc);
            support.addEdit(gcc);
            changingGs.add(gi.getID());
          }
        }
        for (String lkey : newTargs.keySet()) {
          Linkage childLink = gi.getLinkage(lkey);
          if (childLink != null) { // Those darn VfNs!
            String newTrg = newTargs.get(lkey);
            GenomeChange gc = gi.changeLinkageTargetNode(childLink, newTrg, childLink.getLandingPad());
            GenomeChangeCmd gcc = new GenomeChangeCmd(rcxi, gc);
            support.addEdit(gcc);
            changingGs.add(gi.getID());
          }
        }
      }
  
      //
      // Repair root layout:
      //
      
      for (String toMerge : mergeMap_.keySet()) { 
        String mergeTo = mergeMap_.get(toMerge);
        BusProperties bpO = rlo.getLinkPropertiesForSource(toMerge);
        if (bpO == null) {
          continue;
        }
        LinkSegmentID rsid = (bpO.isDirect()) ? LinkSegmentID.buildIDForDirect(bpO.getLinkageList().get(0)) : LinkSegmentID.buildIDForStartDrop();
        LinkSegmentID dest = null;
        BusProperties bp = rlo.getLinkPropertiesForSource(mergeTo);
        if (bp != null) {
          if (bp.isDirect()) {
            PropChange pc = rlo.splitDirectLinkInHalf(mergeTo, dacx_); 
            PropChangeCmd pcc = new PropChangeCmd(dacx_, pc);
            support.addEdit(pcc);
            changingLOs.add(rlo.getID());
          }
         
          dest = bp.getRootSegmentID();
          dest.tagIDWithEndpoint(LinkSegmentID.START);
        }
        Layout.PropChange[] pca = rlo.supportLinkSourceBreakoff(rsid, 
                                                                new HashSet<String>(bpO.getLinkageList()), 
                                                                mergeTo, dest);
        if ((pca != null) && (pca.length != 0)) {
          PropChangeCmd pcc = new PropChangeCmd(dacx_, pca);
          support.addEdit(pcc);
          changingLOs.add(rlo.getID());
        }
      }
          
      //
      // Repair instance layouts:
      //
 
      giit = dacx_.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (!gi.isRootInstance()) { // only need layouts for root instances.
          continue;
        }
        DataAccessContext rcxi = new StaticDataAccessContext(dacx_, gi.getID());
        Layout lo = rcxi.getCurrentLayout();
        Map<String, String> newMoves = nodeMoves.get(gi.getID());
        for (String nkey : newMoves.keySet()) {
          NodeProperties copyProp = lo.getNodeProperties(nkey);
          String newNode = newMoves.get(nkey);
          NodeProperties oldProp = lo.getNodeProperties(newNode);
          NodeProperties newProp = new NodeProperties(newNode, copyProp);
          PropChange pc = lo.replaceNodeProperties(oldProp, newProp);
          PropChangeCmd pcc = new PropChangeCmd(rcxi, pc);
          support.addEdit(pcc);
          changingLOs.add(lo.getID());
        } 
       
        //
        // The link properties holds a reference to the source node. So if we are changing
        // the source ID, we need to fix that as well:
        //
        
        Map<String, String> newSources = sourcePlans.get(gi.getID());
        for (String lkey : newSources.keySet()) {
          BusProperties lProp = lo.getLinkProperties(lkey);
          String newSrc = newSources.get(lkey);
          BusProperties repl = new BusProperties(lProp, newSrc);
          PropChange pc = lo.replaceLinkProperties(lProp, repl);
          PropChangeCmd pcc = new PropChangeCmd(rcxi, pc);
          support.addEdit(pcc);
        }
      }
      
      mergeTCMaps(support);
      mergeTIRDMaps(support);
      mergePertMaps(support);
 
      //
      // Get rid of all the orphaned nodes, top to bottom. NOTE THE NULL MAP for 
      // specifying whether data tables are being deleted. NO DATA TABLES ARE 
      // GETTING DELETED!
      //
      
      for (String merged : mergeMap_.keySet()) {
        RemoveNode.deleteNodeFromModelCore(uics_, merged, dacx_, support, null, false, uFac_);
      }
      
      //
      // Use the list of links that start/end on the node merge targets prior to the merge 
      // to now merge duplicate links: 
      //
      
      HashSet<String> done = new HashSet<String>();
      for (String linkID : mergeLinkCandidates) {
        if (!done.contains(linkID)) {
          Set<String> elim = MergeDuplicateLinks.mergeDuplicateLinks(linkID, dacx_, support, uFac_);     
          done.addAll(elim);
        }
      }
      
      uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);   
      if (globalPadNeeds_ != null) {
        ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds_, false, support);
      }  

      for (String gid : changingGs) {
        ModelChangeEvent mce = new ModelChangeEvent(dacx_.getGenomeSource().getID(), gid, ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mce); 
      }
      
      for (String lid : changingLOs) {
        LayoutChangeEvent lcev = new LayoutChangeEvent(lid, LayoutChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(lcev);
      }

      support.finish();     
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
   
    /***************************************************************************
    **
    ** Generate the map
    */
      
    private Map<String, List<FullGenomeHierarchyOracle.NodeUsage>> findModels(List<FullGenomeHierarchyOracle.NodeUsage> nus) {
      HashMap<String, List<FullGenomeHierarchyOracle.NodeUsage>> retval = new HashMap<String, List<FullGenomeHierarchyOracle.NodeUsage>>();
      for (FullGenomeHierarchyOracle.NodeUsage nu : nus) {
        List<FullGenomeHierarchyOracle.NodeUsage> forMod = retval.get(nu.modelID);
        if (forMod == null) {
          forMod = new ArrayList<FullGenomeHierarchyOracle.NodeUsage>();
          retval.put(nu.modelID, forMod);
        }
        forMod.add(nu);   
      }
      return (retval);
    }
 
    /***************************************************************************
    **
    ** Generate a full graph
    */
      
    private Set<LexiLink> fullGraph(Set<String> nodes) {
       
      HashSet<LexiLink> retval = new HashSet<LexiLink>();
      for (String node1 : nodes) {
        for (String node2 : nodes) {
          if (!node1.equals(node2)) {
            retval.add(new LexiLink(node1, node2));
          }
        }
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Generate the list
    */
      
    private void findLinksForModel(List<FullGenomeHierarchyOracle.NodeUsage> nus, Set<LexiLink> links, Set<String> allNodes) {
    
      //
      // Get the set of baseIDs in each group of the model:
      //
      
      HashMap<String, Set<String>> nodesPerGroup = new HashMap<String, Set<String>>();
   
      for (FullGenomeHierarchyOracle.NodeUsage nu : nus) {
        Set<String> forGrp = nodesPerGroup.get(nu.groupID);
        if (forGrp == null) {
          forGrp = new HashSet<String>();
          nodesPerGroup.put(nu.groupID, forGrp);
        }
        forGrp.add(nu.baseID);
        allNodes.add(nu.baseID);
      }
      
      //
      // For each group, create a clique:
      //
      
      for (String grp : nodesPerGroup.keySet()) {
        Set<String> forGrp = nodesPerGroup.get(grp);
        for (String node1 : forGrp) {
          for (String node2 : forGrp) {
            if (!node1.equals(node2)) {
              links.add(new LexiLink(node1, node2));
            }
          }
        }
      }
      return;
    } 
  }

  /***************************************************************************
  **
  ** Ordered link
  */
        
  public static class LexiLink extends Link  {
    public LexiLink(String src, String trg) {
      super(((src == null) || (src.compareTo(trg) < 0)) ? src : trg, ((src == null) || (src.compareTo(trg) < 0)) ? trg : src);
    }
  }
}
