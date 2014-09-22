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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.dialogs.GroupAssignmentDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle propagating elements down the model tree
*/

public class PropagateDown extends AbstractControlFlow {

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
  
  public PropagateDown(BTState appState) {
    super(appState);  
    name = "command.PropagateDown";
    desc =  "command.PropagateDown";
    icon = "Propagate24.gif";
    mnem = "command.PropagateDownMnem";
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.canPropagateDown());
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
        ans = new StepState(appState_);
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {
    
    private String nextStep_;    
    private BTState appState_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState) {
      appState_ = appState;
      nextStep_ = "stepState";
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepState() {
      Database db = appState_.getDB();
      if (db.haveBuildInstructions()) {
        ResourceManager rMan = appState_.getRMan(); 
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("instructWarning.PropDownMessage"), 
                                      rMan.getString("instructWarning.PropDownTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      String genomeKey = appState_.getGenome();
      String layoutKey = appState_.getLayoutKey();    
      Map<String, Intersection> selections = appState_.getSUPanel().getSelections();
      ArrayList<Intersection> toDown = new ArrayList<Intersection>(selections.values());
      if (toDown.size() > 0) {
        propagateDown(appState_, genomeKey, layoutKey, toDown);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Propagate the selected items down from the full genome to the VfA
    */  
   
    private boolean propagateDown(BTState appState, String genomeKey, String layoutKey, List<Intersection> intersections) {
  
      //double offset = (double)width;
      Database db = appState.getDB();
      Genome genome = db.getGenome();
      if (!genome.getID().equals(genomeKey)) {  // Only works for root genome
        return (false);
      }
      DataAccessContext rcxR = new DataAccessContext(appState, genome, db.getLayout(layoutKey));
      
      //
      // The first question is, which VfG to propagate to?  If there is only
      // one, there is no question.  If there is more than one, we need to
      // offer the user a dialog box.
      //
  
      
      
      Iterator<GenomeInstance> iit = rcxR.getGenomeSource().getInstanceIterator();
      ArrayList<GenomeInstance> roots = new ArrayList<GenomeInstance>();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (gi.getVfgParent() == null) {
          roots.add(gi);
        }
      }
      int numRoots = roots.size();
      if (numRoots == 0) {
        throw new IllegalStateException();
      }
      
      DataAccessContext rcxVfA;
      if (numRoots == 1) {
        rcxVfA = new DataAccessContext(appState, roots.get(0), appState.getLayoutForGenomeKey(roots.get(0).getID()));
      } else {
        int useIndex = 0;
        ArrayList<ChoiceMap> targsToSort = new ArrayList<ChoiceMap>();
        for (int i = 0; i < numRoots; i++) {
          GenomeInstance gi = roots.get(i);
          ChoiceMap cmap = new ChoiceMap(gi.getName(), gi);
          targsToSort.add(cmap);
        }
        Collections.sort(targsToSort);
        ChoiceMap[] targets = new ChoiceMap[numRoots];
        targsToSort.toArray(targets);
        DownPropState down = appState.getDownProp();
        for (int i = 0; i < numRoots; i++) {
          ChoiceMap cmap = targsToSort.get(i);
          if ((down.lastGI != null) && ((GenomeInstance)cmap.item).getID().equals(down.lastGI)) {
            useIndex = i;
          }
        }
        ResourceManager rMan = appState.getRMan();
        ChoiceMap targetChoice = 
          (ChoiceMap)JOptionPane.showInputDialog(appState.getTopFrame(), 
                                                 rMan.getString("propagate.PropChoose"), 
                                                 rMan.getString("propagate.PropTitle"),     
                                                 JOptionPane.QUESTION_MESSAGE, null, 
                                                 targets, targets[useIndex]);
        if (targetChoice == null) {
          return (false);
        }
        
        GenomeInstance target = (GenomeInstance)targetChoice.item;
        rcxVfA = new DataAccessContext(appState, target, appState.getLayoutForGenomeKey(target.getID()));
        down.lastGI = target.getID();
      }
      
      //
      // Now we know where we are going.  Build up lists of genes and
      // nodes that are going down.
      //
      
      Iterator<Intersection> init = intersections.iterator();
      ArrayList<Node> geneList = new ArrayList<Node>();
      ArrayList<Node> nodeList = new ArrayList<Node>();
      while (init.hasNext()) {
        Intersection inter = init.next();
        String id = inter.getObjectID();
        Gene gene = genome.getGene(id);
        if (gene != null) {
          geneList.add(gene);
          continue;
        }
        Node node = genome.getNode(id);
        if (node != null) {
          nodeList.add(node);
          continue;
        }
      }
  
      //
      // Adding in a node may mess up existing net module linkages.  Record exisiting
      // state before changing anything:
      //
      
      Layout.PadNeedsForLayout padFixups = rcxVfA.getLayout().findAllNetModuleLinkPadRequirements(rcxVfA);    
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState, "undo.propagateDown");      
      
      // Next step is to figure out what groups the nodes are going into.  If
      // the node already is instanced in a group of the VFG, it must go into a
      // new group.  All nodes in this batch go into the same group.
      //
      // For each gene and node, find out if it lives in a group already.  If so, 
      // that group is out.  If nothing is left, they can only create a new group.
      //
   
      Group selectedGroup = null;
      if ((nodeList.size() > 0) || (geneList.size() > 0)) {
        Iterator<Group> grit = rcxVfA.getGenomeAsInstance().getGroupIterator();
        ArrayList<Group> groupList = new ArrayList<Group>();
        while (grit.hasNext()) {
          // Cut out groups that are subsets
          Group group = grit.next();
          if (group.isASubset(rcxVfA.getGenomeAsInstance())) {
            continue;
          }
          boolean present = false;
          Iterator<Node> glit = geneList.iterator();
          while (glit.hasNext()) {
            Node gene = glit.next();
            if (group.instanceIsInGroup(gene.getID())) {
              present = true;
              break;
            }
          }
          if (present) continue;
          Iterator<Node> nlit = nodeList.iterator();
          while (nlit.hasNext()) {
            Node node = nlit.next();
            if (group.instanceIsInGroup(node.getID())) {
              present = true;
              break;
            }
          }
          if (!present) {
            groupList.add(group);
          }
        }
  
        //
        // If the group list has no members, the user must specify a group to
        // continue.  If we have a list, then they can choose.
        //
      
        boolean newGroup = false;
        String selectedGroupID = null;
        boolean wasForcedDirect = false;
  
        Iterator<Node> git = geneList.iterator();
        Iterator<Node> nit = nodeList.iterator();
        Rectangle approxBounds = rcxR.getLayout().getApproxBounds(nit, git, rcxR);
        git = geneList.iterator();
        nit = nodeList.iterator();      
        Point2D defaultCenter = rcxR.getLayout().getApproxCenter(nit, git);
        if (defaultCenter == null) {  // should not happen...
          defaultCenter = new Point2D.Double(approxBounds.getCenterX(), approxBounds.getCenterY());
        }
        
        Point2D directCenter = null;            
        if (rcxVfA.getGenomeAsInstance().hasZeroOrOneInstancePerNodeAfterAdditions(geneList, nodeList)) {
          directCenter = (Point2D)defaultCenter.clone();
        }      
        
        if (groupList.size() == 0) {
          GroupCreationSupport handler = 
            new GroupCreationSupport(appState, rcxVfA, true, approxBounds,defaultCenter, directCenter);
          selectedGroup = handler.handleCreation(support);
          if (selectedGroup == null) {
            return (false);
          }
          selectedGroupID = selectedGroup.getID();
          newGroup = true;
          wasForcedDirect = handler.wasForcedDirect();
        } else {
          GroupAssignmentDialog gad = new GroupAssignmentDialog(appState, rcxVfA, support,groupList, approxBounds, directCenter);
          gad.setVisible(true);
          selectedGroupID = gad.getResult();
          if (selectedGroupID == null) {
            return (false);
          }
          selectedGroup = rcxVfA.getGenomeAsInstance().getGroup(selectedGroupID);
          newGroup = gad.isNewGroup();
        }
  
        //
        // We have a list of guys and we have a target group.  We can now figure out
        // how to place the new nodes.  Two cases, depending on if the group is new
        // or not:
        //
        // New Group:
        // If forced direct, the vector is the zero vector
        // else:
        // 1) Get the centroid of all the nodes going into the new group
        // 2) Get the vector between the new group position provided by the user and the centroid.
        // 3) Add this vector to the original position to get the new position
        //
        // Existing Group:
        // 1) Get the centroid of all the nodes in the existing group.
        // 2) Get the centroid of all the nodes in the existing group in the original.
        // 3) Obtain the vector between the points in 3 and 2
        // 4) Add this vector to the original position to get the new position.
        //
  
        Vector2D offset = null;
   
        if (newGroup) {
          if (wasForcedDirect) {
            offset = new Vector2D(0.0, 0.0);
          } else {
            git = geneList.iterator();
            nit = nodeList.iterator();
            Point2D goingInCentroid = rcxR.getLayout().getApproxCenter(nit, git);        
            GroupProperties gp = rcxVfA.getLayout().getGroupProperties(selectedGroupID);
            if (goingInCentroid == null) {
              offset = new Vector2D(0.0, 0.0);
            } else {
              offset = new Vector2D(goingInCentroid, gp.getLabelLocation());
            }
          }
        } else {
          Point2D existCentroid = rcxVfA.getLayout().getApproxCenterForGroup(rcxVfA.getGenomeAsInstance(), selectedGroupID, false);
          Point2D existOrigCentroid = rcxR.getLayout().getApproxCenterForGroup(rcxVfA.getGenomeAsInstance(), selectedGroupID, true);
          if ((existCentroid == null) || (existOrigCentroid == null)) {
            offset = new Vector2D(0.0, 0.0);
          } else {
            offset = new Vector2D(existOrigCentroid, existCentroid);
          }
        }
              
        //
        // We are given a list of selected items that we are to propagate
        // down. Crank through the list and propagate.
        //
      
        Iterator<Node> glit = geneList.iterator();
        while (glit.hasNext()) {
          Gene gene = (Gene)glit.next();
          PropagateSupport.propagateNode(appState, true, rcxVfA, (DBGene)gene, rcxR.getLayout(), offset, selectedGroup, support);
        }
        Iterator<Node> nlit = nodeList.iterator();
        while (nlit.hasNext()) {
          Node node = nlit.next();
          PropagateSupport.propagateNode(appState, false, rcxVfA, (DBNode)node, rcxR.getLayout(), offset, selectedGroup, support);  
        }
      }
      
      //
      // Nodes first, then linkages, to insure our targets are there:
      //
      
      Set<String> dbLinkages = new HashSet<String>();
      init = intersections.iterator();
      while (init.hasNext()) {
        Intersection inter = init.next();
        String id = inter.getObjectID();
        Linkage link = genome.getLinkage(id);
        if (link != null) {
          dbLinkages.addAll(resolveLinkages(inter, rcxR.getLayout()));
        }
      }
      
      //
      // If we have no linkages, we are done:
      //
      
      if (dbLinkages.size() == 0) {
        AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
        support.finish();
        return (true);
      }
      
      //
      // Above loop goes first to eliminate multiple linkage creation from
      // multiple intersection references.  Now we figure out the group
      // tuple that is going to drive this operation.
      //
      
      GroupTupleResult tupResult = calculateGroupTwoTuples(rcxVfA.getGenomeAsInstance(), dbLinkages, selectedGroup);
      Set<String> surviving = tupResult.survivingLinks;
  
      //
      // Lots of things can go wrong:
      //
      
      ResourceManager rMan = appState.getRMan();    
      if (tupResult.result == GroupTupleResult.MISSING_ENDPOINT) {
        JOptionPane.showMessageDialog(appState.getTopFrame(), rMan.getString("propagate.MissingEndpoint"), 
                                      rMan.getString("propagate.MissingEndpointTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        if (surviving.isEmpty()) {
          AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
          support.finish();
          return (false);  // But note that nodes have propagated!
        }
      } else if (tupResult.result == GroupTupleResult.NO_COMMON_GROUPINGS) {
        JOptionPane.showMessageDialog(appState.getTopFrame(), rMan.getString("propagate.NoCommonGroupings"), 
                                      rMan.getString("propagate.NoCommonGroupingsTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
        support.finish();
        return (false);  // But note that nodes have propagated!      
      
      } else if (tupResult.result == GroupTupleResult.TARGET_GROUP_NOT_PRESENT) {
        JOptionPane.showMessageDialog(appState.getTopFrame(), rMan.getString("propagate.TargetGroupNotPresent"), 
                                      rMan.getString("propagate.TargetGroupNotPresentTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
        support.finish();
        return (false);  // But note that nodes have propagated! 
      }
          
      TreeSet<GenomeInstance.GroupTuple> groupTups = new TreeSet<GenomeInstance.GroupTuple>(tupResult.set);
  
      //
      // If one is left, we use it.  Also, if only one is left per link, we use that.  
      // If more than one is left, we let the user choose.
      //
     
      int numTups = groupTups.size();
      GenomeInstance.GroupTuple oneChosen = null;
      
      if (numTups == 1) {
        oneChosen = groupTups.iterator().next();
      } else if (tupResult.result != GroupTupleResult.SINGLE_TUPLE_PER_LINK) {
        Object[] values = new Object[numTups];
        Iterator<GenomeInstance.GroupTuple> gtit = groupTups.iterator();
        int count = 0;
        while (gtit.hasNext()) {
          GenomeInstance.GroupTuple gt = gtit.next();
          // FIX ME?? Message format
          Group sg = rcxVfA.getGenomeAsInstance().getGroup(gt.getSourceGroup());
          Group tg = rcxVfA.getGenomeAsInstance().getGroup(gt.getTargetGroup());        
          String desc = sg.getName() + " --> " + tg.getName();
          values[count++] = new ChoiceMap(desc, gt);
        }   
        ChoiceMap tupleChoice = 
          (ChoiceMap)JOptionPane.showInputDialog(appState.getTopFrame(), 
                                                 rMan.getString("propagate.TupleChoose"), 
                                                 rMan.getString("propagate.TupleTitle"),     
                                                 JOptionPane.QUESTION_MESSAGE, null, 
                                                 values, values[0]);
        if (tupleChoice == null) {
          AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
          support.finish();
          return (false); // But note that nodes have propagated!
        } else {
          oneChosen = (GenomeInstance.GroupTuple)tupleChoice.item;
        }
      }
          
      Iterator<String> sit = surviving.iterator();
      while (sit.hasNext()) {
        String lnkKey = sit.next();
        DBLinkage dbLink = (DBLinkage)genome.getLinkage(lnkKey);
        GenomeInstance.GroupTuple chosen = (oneChosen != null) ? oneChosen :
                                             (GenomeInstance.GroupTuple)tupResult.singleTuplePerLink.get(lnkKey);
        PropagateSupport.propagateLinkage(appState, rcxVfA, dbLink, rcxR, chosen, support);
      }
      AddCommands.finishNetModPadFixups(appState, null, null, rcxVfA, padFixups, support);
      support.finish();
      return (true);
    }
    
    /***************************************************************************
    **
    ** Given an intersection, we need to resolve out all the Linkages that
    ** are referenced.  Returns a set of strings.
    */  
   
    private Set<String> resolveLinkages(Intersection inter, Layout layout) {
      HashSet<String> allSet = new HashSet<String>();    
      BusProperties lp = layout.getLinkProperties(inter.getObjectID());
      LinkSegmentID[] segIDs = inter.segmentIDsFromIntersect();
      if (segIDs == null) {
        List<String> allLinks = lp.getLinkageList();  // Strings
        allSet.addAll(allLinks);
        return (allSet);
      }
      for (int i = 0; i < segIDs.length; i++) {
        LinkSegmentID segID = segIDs[i];
        Set<String> resolved = lp.resolveLinkagesThroughSegment(segID);
        allSet.addAll(resolved);
      }
      return (allSet);
    }

    /***************************************************************************
    **
    ** We are handed a pile of linkages.  We go through them and figure out the
    ** group 2-tuples everybody in the set can handle.  If there is no
    ** common group 2-tuple, we bag it.  If there is a single common 2-tuple,
    ** we take it (see BT-03-23-05:10).  Else, if we are provided a group X argument (telling us
    ** that nodes are being created in that group at the same time), we
    ** choose the (X, X) tuple automatically; if it is not present, we bag it.
    */  
  
    public static GroupTupleResult calculateGroupTwoTuples(GenomeInstance gi, Set<String> dbLinkages, Group group) {
  
      Set<GenomeInstance.GroupTuple> retval = null;
      int resultVal = GroupTupleResult.OK;
      boolean haveSomeResults = false;
      HashMap<String, GenomeInstance.GroupTuple> singleTuplePerLink = new HashMap<String, GenomeInstance.GroupTuple>();
      HashSet<String> surviving = new HashSet<String>();
      Iterator<String> sit = dbLinkages.iterator();
      while (sit.hasNext()) {
        String lnkKey = sit.next();
        Set<GenomeInstance.GroupTuple> forLink = gi.getNewConnectionTuples(lnkKey);
        if (forLink.isEmpty()) {
          resultVal = GroupTupleResult.MISSING_ENDPOINT;
        } else {
          if (forLink.size() == 1) {
            singleTuplePerLink.put(lnkKey, forLink.iterator().next());
          }
          haveSomeResults = true;
          surviving.add(lnkKey);
          if (retval == null) {
            retval = forLink;
          } else {
            retval.retainAll(forLink);
          }
        }
      }
  
      if (retval == null) {
        retval = new HashSet<GenomeInstance.GroupTuple>();
      }
      
      if (retval.size() == 0) {
        int zeroRet;
        Map<String, GenomeInstance.GroupTuple> retMap = null;
        if (haveSomeResults) {
          // FIXES BT-06-27-05:4 - was comparing to original link set size, not survivors  
          if (singleTuplePerLink.size() == surviving.size()) {
            zeroRet = GroupTupleResult.SINGLE_TUPLE_PER_LINK;
            retMap = singleTuplePerLink;
          } else {
            zeroRet = GroupTupleResult.NO_COMMON_GROUPINGS;
          }
        } else {
          zeroRet = GroupTupleResult.MISSING_ENDPOINT;
        }
        //
        // Have MISSING_ENDPOINT iff one or more results were empty 
        // Have haveSomeResults==true iff one or more results were not empty, (i.e. empty
        // set can only result from lack of shared tuples)
        //
        return (new GroupTupleResult(zeroRet, retval, surviving, retMap));
      }
      //
      // Following addresses issue BT-03-23-05:10
      //
      if (retval.size() == 1) {
        return (new GroupTupleResult(GroupTupleResult.OK, retval, surviving, null));
      }    
      
      if (group != null) {
        String grid = group.getID();
        GenomeInstance.GroupTuple testTuple = new GenomeInstance.GroupTuple(grid, grid);
        Iterator<GenomeInstance.GroupTuple> rit = retval.iterator();
        while (rit.hasNext()) {
          GenomeInstance.GroupTuple gt = rit.next();
          if (gt.equals(testTuple)) {
            retval = new HashSet<GenomeInstance.GroupTuple>();
            retval.add(gt);
            return (new GroupTupleResult(resultVal, retval, surviving, null));
          }
        }
   
        return (new GroupTupleResult(GroupTupleResult.TARGET_GROUP_NOT_PRESENT, new HashSet<GenomeInstance.GroupTuple>(), surviving, null));
      }
  
      return (new GroupTupleResult(resultVal, retval, surviving, null));
    }
   
    
    
  }

  /***************************************************************************
  **
  ** Used for choice mappings. FIXME REALLY Ancient History! Replace!
  */
  
  static class ChoiceMap implements Comparable<ChoiceMap> {
    
    String description;
    Object item;

    ChoiceMap(String desc, Object item) {
      this.description = desc;
      this.item = item;
    }
    
    public String toString() {
      return (description);
    }
    
    public int compareTo(ChoiceMap other) {
      return (this.description.compareToIgnoreCase(other.description));
    }
  }
  
  /***************************************************************************
  **
  ** Used for propagating links down 
  */
  
  private static class GroupTupleResult {

    static final int OK                  = 0;
    static final int MISSING_ENDPOINT    = 1;
    static final int NO_COMMON_GROUPINGS = 2;
    static final int TARGET_GROUP_NOT_PRESENT = 3;
    static final int SINGLE_TUPLE_PER_LINK    = 4;    

    int result;
    Set<GenomeInstance.GroupTuple> set;
    Set<String> survivingLinks;
    Map<String, GenomeInstance.GroupTuple> singleTuplePerLink;
    
    GroupTupleResult(int result, Set<GenomeInstance.GroupTuple> set, Set<String> survivingLinks, Map<String, GenomeInstance.GroupTuple> singleTuplePerLink) {
      this.set = set;
      this.result = result;
      this.survivingLinks = survivingLinks;
      this.singleTuplePerLink = singleTuplePerLink;      
    }
  }
  
  /***************************************************************************
  **
  ** Used to propagate layout down.  Remembers from invocation to invocation what
  ** the state is;
  */
  
  public static class DownPropState {
    public String lastGI;
    
    public DownPropState() {
      this.lastGI = null;
    } 
  }   
}
