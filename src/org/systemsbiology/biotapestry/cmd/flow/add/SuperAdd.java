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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.SuperAddTargetDialog;
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
  
  public SuperAdd(BTState appState) {
    super(appState);  
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, dacx);
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
      if (ans.getNextStep().equals("stepState")) {
        next = ans.stepState();
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
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepState";
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
       
    private DialogAndInProcessCmd stepState() {
      String nodeID = intersect_.getObjectID();
      Node node = dacx_.getGenome().getNode(nodeID);
      if (node != null) {
        SuperAddTargetDialog satd = new SuperAddTargetDialog(appState_, dacx_, nodeID);
        satd.setVisible(true);
        if (!satd.haveResult()) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
        List<SuperAddPair> superAddPairs = satd.getSuperAddPairs();
        superAddForNode(node.getID(), superAddPairs);
        appState_.getSUPanel().drawModel(false);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Super add the given node into the specified target model/group pairs.
    */  
   
    private boolean superAddForNode(String nodeID, List<SuperAddPair> superAddPairs) {
  
      DataAccessContext rcxR = dacx_.getContextForRoot(); // root genome

      String rootNodeID = GenomeItemInstance.getBaseID(nodeID);
  
      //
      // Build a map of groups per model.  Keyset is our models to hit:
      //
      
      HashMap<String, Set<String>> groupsPerModel = new HashMap<String, Set<String>>();
      int numSAP = superAddPairs.size();
      for (int i = 0; i < numSAP; i++) {
        SuperAddPair sap = superAddPairs.get(i);
        Set<String> groupSet = groupsPerModel.get(sap.genomeID);
        if (groupSet == null) {
          groupSet = new HashSet<String>();
          groupsPerModel.put(sap.genomeID, groupSet);
        }
        groupSet.add(sap.groupID);
      }
      
      //
      // Make a top-down ordering of models:
      //
      
      Set<String> models = groupsPerModel.keySet();
      ArrayList<String> orderedModelsToHit = new ArrayList<String>();
      NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
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
      
      UndoSupport support = new UndoSupport(appState_, "undo.superAddNode");      
       
      int numOM = orderedModelsToHit.size();
      for (int i = 0; i < numOM; i++) {
        String gKey = orderedModelsToHit.get(i);        
        DataAccessContext rcxT = new DataAccessContext(rcxR, gKey);
        GenomeInstance target = rcxR.getGenomeAsInstance();
        boolean needLayout = (target.getVfgParent() == null);
        Set<String> groupSet = groupsPerModel.get(gKey);
        
        //
        // Figure out the position offset to use if a root instance:
        //      
        
        Iterator<String> gsit = groupSet.iterator();
        while (gsit.hasNext()) {
          String groupID = gsit.next();
          if (needLayout) {
            Point2D existCentroid = rcxT.getLayout().getApproxCenterForGroup(target, groupID, false);
            Point2D existOrigCentroid = rcxR.getLayout().getApproxCenterForGroup(target, groupID, true);
            if ((existCentroid == null) || (existOrigCentroid == null)) {
              GroupProperties gp = rcxT.getLayout().getGroupProperties(groupID);
              existCentroid = gp.getLabelLocation();      
              existOrigCentroid = rcxR.getLayout().getApproxCenter(rcxR.getGenome().getAllNodeIterator());
            }
            Group targGroup = target.getGroup(groupID);
            Vector2D offset;
            if ((existCentroid != null) && (existOrigCentroid != null)) {
              offset = new Vector2D(existOrigCentroid, existCentroid);       
            } else {
              offset = new Vector2D(0.0, 0.0);
            }
            Node node = rcxR.getGenome().getNode(rootNodeID);
            PropagateSupport.propagateNode(appState_, (node.getNodeType() == Node.GENE), rcxT, (DBNode)node, rcxR.getLayout(), offset, targGroup, support);  
          } else {
            GenomeInstance rpgi = target.getVfgParentRoot();
            int iNum = rpgi.getInstanceForNodeInGroup(rootNodeID, Group.getBaseID(groupID));
            GenomeInstance pgi = target.getVfgParent();
            String instanceKey = GenomeItemInstance.getCombinedID(rootNodeID, Integer.toString(iNum));
            NodeInstance pNode = (NodeInstance)pgi.getNode(instanceKey);
            PropagateSupport.addNewNodeToSubsetInstance(appState_, rcxT, pNode, support);
          }
        }
      }
   
      support.finish();
      return (true);
    }   
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
    
    public SuperAddPair(String genomeID, String groupID) {
      this.genomeID = genomeID;
      this.groupID = groupID;
    }
  }   
}
