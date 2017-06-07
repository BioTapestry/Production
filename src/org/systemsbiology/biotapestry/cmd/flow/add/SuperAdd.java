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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.SuperAddTargetDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle super add
*/

public class SuperAdd extends AbstractControlFlow {

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
  
  public SuperAdd() {  
    name = "nodePopup.SuperAdd" ;
    desc = "nodePopup.SuperAdd";
    mnem = "nodePopup.SuperAddMnem";
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
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
        throw new IllegalArgumentException();
      } else { 
        ans = (StepState)last.currStateX;
      }
      ans.stockCfhIfNeeded(cfh);
      if (ans.getNextStep().equals("stepState")) {
        next = ans.stepState();
      } else if (ans.getNextStep().equals("stepDataExtract")) {      
        next = ans.stepDataExtract(last);
      } else if (ans.getNextStep().equals("stepToWarn")) {
          next = ans.stepToWarn();
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
    
    private Intersection intersect_; 
    private String nodeID;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepToWarn";
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepToWarn";
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
       
    private DialogAndInProcessCmd stepState() {
      nodeID = intersect_.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(nodeID);
      if (node != null) {  
        SuperAddTargetDialogFactory.SuperAddBuildArgs ba = 
          new SuperAddTargetDialogFactory.SuperAddBuildArgs(nodeID);
        SuperAddTargetDialogFactory dgcdf = new SuperAddTargetDialogFactory(cfh_);
        ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
        nextStep_ = "stepDataExtract";
        return (retval);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Handle the control flow for drawing into subset instance
    */
         
     private DialogAndInProcessCmd stepDataExtract(DialogAndInProcessCmd cmd) {
       SuperAddTargetDialogFactory.SuperAddTargetRequest crq = (SuperAddTargetDialogFactory.SuperAddTargetRequest)cmd.cfhui;   
       if (!crq.haveResults()) {
         DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
         return (retval);
       } 
       List<SuperAddPair> superAddPairs = crq.sapResult;
       superAddForNode(dacx_, nodeID, superAddPairs, null, uFac_);
       uics_.getSUPanel().drawModel(false);
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
       return (retval);
     } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToWarn() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      } 
      nextStep_ = "stepState";   
      return (daipc); 
    }
  }
   
  /***************************************************************************
  **
  ** Super add the given node into the specified target model/group pairs.
  */  
 
  public static boolean superAddForNode(StaticDataAccessContext dacx, 
                                        String nodeID, List<SuperAddPair> superAddPairs, 
                                        UndoSupport support, UndoFactory uFac) {

    StaticDataAccessContext rcxR = dacx.getContextForRoot(); // root genome

    String rootNodeID = GenomeItemInstance.getBaseID(nodeID);

    //
    // Build a map of groups per model.  Keyset is our models to hit:
    //
    
    HashMap<String, Set<String>> groupsPerModel = new HashMap<String, Set<String>>();
    HashMap<String, Map<String, Set<String>>> overlaysPerModel = new HashMap<String, Map<String, Set<String>>>();
    HashMap<String, Set<String>> subGroupsPerModel = new HashMap<String, Set<String>>();
    
    int numSAP = superAddPairs.size();
    for (int i = 0; i < numSAP; i++) {
      SuperAddPair sap = superAddPairs.get(i);
      Set<String> groupSet = groupsPerModel.get(sap.genomeID);
      if (groupSet == null) {
        groupSet = new HashSet<String>();
        groupsPerModel.put(sap.genomeID, groupSet);
      }
      groupSet.add(sap.groupID);
      if (sap.modMem != null) {
        overlaysPerModel.put(sap.genomeID, sap.modMem);
      }
      if (sap.subGroups != null) {
        subGroupsPerModel.put(sap.genomeID, sap.subGroups);
      }
    }
    
    //
    // Make a top-down ordering of models:
    //
    
    Set<String> models = groupsPerModel.keySet();
    ArrayList<String> orderedModelsToHit = new ArrayList<String>();
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      if (models.contains(gkey)) {
        orderedModelsToHit.add(gkey);
      }
    }

    //
    // Undo/Redo support
    //
    
    boolean closeSupport = false;
    if (support == null) {
      closeSupport = true;
      support = uFac.provideUndoSupport("undo.superAddNode", rcxR);
    }
     
    int numOM = orderedModelsToHit.size();
    for (int i = 0; i < numOM; i++) {
      String gKey = orderedModelsToHit.get(i);        
      StaticDataAccessContext rcxT = new StaticDataAccessContext(rcxR, gKey);
      // changing rcxR to rcxT here fixes issue 197:
      GenomeInstance target = rcxT.getCurrentGenomeAsInstance();
      boolean needLayout = (target.getVfgParent() == null);
      Set<String> groupSet = groupsPerModel.get(gKey);
      
      //
      // Figure out the position offset to use if a root instance:
      //      
      
      Iterator<String> gsit = groupSet.iterator();
      while (gsit.hasNext()) {
        String groupID = gsit.next();
        String newNodeID;
        if (needLayout) {
          Point2D existCentroid = rcxT.getCurrentLayout().getApproxCenterForGroup(target, groupID, false);
          Point2D existOrigCentroid = rcxR.getCurrentLayout().getApproxCenterForGroup(target, groupID, true);
          if ((existCentroid == null) || (existOrigCentroid == null)) {
            GroupProperties gp = rcxT.getCurrentLayout().getGroupProperties(groupID);
            existCentroid = gp.getLabelLocation();      
            existOrigCentroid = rcxR.getCurrentLayout().getApproxCenter(rcxR.getCurrentGenome().getAllNodeIterator());
          }
          Group targGroup = target.getGroup(groupID);
          Vector2D offset;
          if ((existCentroid != null) && (existOrigCentroid != null)) {
            offset = new Vector2D(existOrigCentroid, existCentroid);       
          } else {
            offset = new Vector2D(0.0, 0.0);
          }
          Node node = rcxR.getCurrentGenome().getNode(rootNodeID);
          newNodeID = PropagateSupport.propagateNode((node.getNodeType() == Node.GENE), rcxT, (DBNode)node, rcxR.getCurrentLayout(), offset, targGroup, support);
         
          //
          // Handle (optional) adding to subgroups. Note this ONLY happens in the root instance models!
          //         
          Set<String> sGrps = subGroupsPerModel.get(gKey);
          if (sGrps != null) {
            for (String subg : sGrps) {
              AddNodeToSubGroup.addNodeToSubGroup(rcxT, target, subg, newNodeID, support, uFac);
            }
          }
 
        } else {
          GenomeInstance rpgi = target.getVfgParentRoot();
          int iNum = rpgi.getInstanceForNodeInGroup(rootNodeID, Group.getBaseID(groupID));
          GenomeInstance pgi = target.getVfgParent();
          String instanceKey = GenomeItemInstance.getCombinedID(rootNodeID, Integer.toString(iNum));
          NodeInstance pNode = (NodeInstance)pgi.getNode(instanceKey);
          PropagateSupport.addNewNodeToSubsetInstance(rcxT, pNode, support);
          newNodeID = instanceKey;
        }
        
        //
        // Handle (optional) module adding:
        //
        
        Map<String, Set<String>> modMem = overlaysPerModel.get(gKey);
        if (modMem != null) {
          for (String ovrKey : modMem.keySet()) {
            NetworkOverlay nol = target.getNetworkOverlay(ovrKey);
            Set<String> mods = modMem.get(ovrKey);
            for (String modKey : mods) {
              NetModule nmod = nol.getModule(modKey);
              NetModuleChange nmc = target.addMemberToNetworkModule(ovrKey, nmod, newNodeID);
              if (nmc != null) {
                NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxT, nmc);
                support.addEdit(gcc);
              }
            }
          }
        }        
      }
    }
 
    if (closeSupport) {
      support.finish();
    }
    return (true);
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Represents a super add target
  */
  
  public static class SuperAddPair {   
    public String genomeID;
    public String groupID;
    public Map<String, Set<String>> modMem;
    public Set<String> subGroups;
    
    public SuperAddPair(String genomeID, String groupID, Map<String, Set<String>> modMem, Set<String> subGroups) {
      this.genomeID = genomeID;
      this.groupID = groupID;
      this.modMem = modMem;
      this.subGroups = subGroups;
    }
  }   
}
