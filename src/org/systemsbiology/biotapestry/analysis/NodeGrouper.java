/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;

/****************************************************************************
**
** Group second-class and third-class nodes with first-class nodes
*/

public class NodeGrouper {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashMap<String, ClassedNode> allNodes_;
  private HashSet<Link> allEdges_;
  private HashMap<String, Set<String>> nonSubsetTargets_;
  private ArrayList<String> nodeRanking_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.  Node ranking is partial order; rightmost targets are first!
  */

  public NodeGrouper(GenomeSubset subset, List<String> nodeRanking) {
    
    allNodes_ = new HashMap<String, ClassedNode>();
    allEdges_ = new HashSet<Link>();
    nodeRanking_ = new ArrayList<String>(nodeRanking);
    Genome genome = subset.getBaseGenome();
    
    //
    // Crank through the genome and gather up the nodes into
    // the three classes:
    //
    
    Iterator<String> nit = subset.getNodeIterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Node node = genome.getNode(nodeID);
      int nodeType = node.getNodeType();
      int classVal;
      if (nodeType == Node.GENE) {
        classVal = 0;
      } else if ((nodeType == Node.BOX) || (nodeType == Node.BARE)) {
        classVal = 1;
      } else {
        classVal = 2;
      }
      ClassedNode cnode = new ClassedNode(classVal, nodeID);
      allNodes_.put(nodeID, cnode);
    }
    
    //
    // Build the links, record sources and targets
    //
    
    Iterator<String> lit = subset.getLinkageIterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = genome.getLinkage(linkID);
      String trg = link.getTarget();
      String src = link.getSource();
      Link cfl = new Link(src, trg);
      allEdges_.add(cfl);
      ClassedNode cnSource = allNodes_.get(src);
      cnSource.targets.add(trg);
      ClassedNode cnTarg = allNodes_.get(trg);
      cnTarg.sources.add(src);      
    }
    
    //
    // In subset cases, it is INCORRECT to say that a node has all paths
    // to a core or a fanout if it also has links going OUT OF THE SUBSET
    // as well.  Record these out-of-band targets:
    //
    
    nonSubsetTargets_ = new HashMap<String, Set<String>>();
    
    if (!subset.isCompleteGenome()) {
      Set<String> subNodes = allNodes_.keySet();
      Iterator<Linkage> glit = genome.getLinkageIterator();
      while (glit.hasNext()) {
        Linkage link = glit.next();
        String trg = link.getTarget();
        String src = link.getSource();
        if (subNodes.contains(trg)) {
          continue;
        }
        if (!subNodes.contains(src)) {
          continue;
        }
        Set<String> nst = nonSubsetTargets_.get(src);
        if (nst == null) {
          nst = new HashSet<String>();
          nonSubsetTargets_.put(src, nst);
        }
        nst.add(trg);       
      }    
    }
  }
  
  /***************************************************************************
  **
  ** Constructor for test
  */

  public NodeGrouper(List<String> nodes, List<Link> links, Map<String, Integer> classes, List<String> nodeRanking) {
    
    allNodes_ = new HashMap<String, ClassedNode>();
    allEdges_ = new HashSet<Link>();
    nodeRanking_ = new ArrayList<String>(nodeRanking);
    
    //
    // Crank through and gather up the nodes into
    // the three classes:
    //
    
    Iterator<String> nit = nodes.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Integer classValObj = classes.get(nodeID);
      ClassedNode cnode = new ClassedNode(classValObj.intValue(), nodeID);
      allNodes_.put(nodeID, cnode);
    }
    
    //
    // Build the links, record sources and targets
    //
  
    Iterator<Link> lit = links.iterator();
    while (lit.hasNext()) {
      Link cfl = lit.next();
      allEdges_.add(cfl);
      String src = cfl.getSrc();
      String trg = cfl.getTrg();
      ClassedNode cnSource = allNodes_.get(src);
      cnSource.targets.add(trg);
      ClassedNode cnTarg = allNodes_.get(trg);
      cnTarg.sources.add(src);      
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Build the groups
  */

  public Map<String, GroupElement> buildGroups() {
    
    HashMap<String, GroupElement> retval = new HashMap<String, GroupElement>();
    
    // First pass for genes, then box/bare, then bubs and others:
    for (int j = 0; j < 3; j++) {
      
      assignToOwnGroup(j, retval);
      int lastSize = -1;
      int currSize = retval.size();
      
      // Keep making passes until we make no more progress:
      while (lastSize != currSize) {
        //
        // Go and find out which nodes belong to the fan-ins of existing groups:
        //
        tagInboundForClass(retval);    
        //
        // Assign those guys to the appropriate groups:
        //
        assignUniqueInbounds(retval);

        //
        // Go and figure out which nodes belong in the fan-out of class 0 sources:
        //
        tagOutboundForClass(retval);    
        
        //
        // Assign those guys to the appropriate groups:
        //
        assignOutbounds(retval);


        resetNodes();          
        lastSize = currSize;
        currSize = retval.size();
      }
    }
     
    //
    // Shift to inbound if OK as a final step:
    //
    
    Iterator<String> rit = retval.keySet().iterator();
    while (rit.hasNext()) {
      String nodeID = rit.next();
      GroupElement ge = retval.get(nodeID);
      if (shouldShiftToInputs(nodeID, ge.group, retval)) {
        ge.side = GroupElement.Sides.INBOUND;
      }
    }
  
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get terminal targets using groups.  A group that has outbound links to other groups
  ** cannot be a terminal group 
  */
  
  public List<String> findTerminalTargetsByGroups(Map<String, GroupElement> groups) {

    ArrayList<String> retval = new ArrayList<String>();
    HashSet<String> coreSources = new HashSet<String>();
    HashMap<String, Set<String>> targetsPerGroup = new HashMap<String, Set<String>>();
    Iterator<String> gkit = groups.keySet().iterator();
    while (gkit.hasNext()) {
      String nodeID = gkit.next();
      GroupElement nodeInfo = groups.get(nodeID);
      String group = nodeInfo.group;
      Set<String> gpg = targetsPerGroup.get(group);
      if (gpg == null) {
        gpg = new HashSet<String>();
        targetsPerGroup.put(group, gpg);
      }
      gpg.addAll(nodeInfo.targets);
      // Any link out of the core disqualifies
      if (!nodeInfo.targets.isEmpty() && group.equals(nodeID)) {
        coreSources.add(group);
      }
    }
   
    Iterator<String> tgit = targetsPerGroup.keySet().iterator();
    while (tgit.hasNext()) {
      String group = tgit.next();
      if (coreSources.contains(group)) {
        continue;
      }
      boolean canAdd = true;
      Set<String> gpg = targetsPerGroup.get(group);
      Iterator<String> git = gpg.iterator();      
      while (git.hasNext()) {
        String trg = git.next();
        GroupElement nodeInfo = groups.get(trg);
        if (!nodeInfo.group.equals(group)) {
          canAdd = false;
          break;
        }
      }
      if (canAdd) {
        retval.add(group);
      }
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static class GroupElement {
    public String nodeID;
    public String group;
    public Sides side;
    public HashSet<String> sources;
    public HashSet<String> targets; 
    
    public enum Sides {INBOUND, OUTBOUND, CORE};
    
    GroupElement(String nodeID, String group, Sides side, Set<String> sources, Set<String> targets) {
      this.nodeID = nodeID;
      this.group = group;
      this.side = side;
      this.sources = new HashSet<String>(sources);
      this.targets = new HashSet<String>(targets);      
    }
    
    public String toString() {
      return ("id = " + nodeID +
              " group = " + group +
              " side = " + side +  
              " src = " + sources +
              " tar = " + targets);                  
    }        
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Reset the nodes:
  */

  private void resetNodes() {  
    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = (String)nodeRanking_.get(i);
      ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);
      cnode.reset();
    }
    return;
  }    

  /***************************************************************************
  ** 
  ** Assign to own group:
  */

  private void assignToOwnGroup(int currClassLevel, Map<String, GroupElement> groupAssignments) {  

    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = (String)nodeRanking_.get(i);
      ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);
      if (cnode.classLevel != currClassLevel) {
        continue;
      }   
      if (groupAssignments.get(cNodeID) == null) {
        GroupElement elem = new GroupElement(cNodeID, cNodeID, GroupElement.Sides.CORE, cnode.sources, cnode.targets);
        groupAssignments.put(cNodeID, elem);
      }
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Assign to groups based on unique targets:
  */

  private void assignUniqueInbounds(Map<String, GroupElement> groupAssignments) {  

    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = (String)nodeRanking_.get(i);
      if (groupAssignments.get(cNodeID) == null) {
        ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);
        String unique = cnode.getUniqueInboundTag();
        if (unique != null) {
          HashSet<String> seen = new HashSet<String>();
          HashSet<String> seen2 = new HashSet<String>();
          if (allPathsToCore(cNodeID, unique, groupAssignments, seen)) {
            GroupElement elem = new GroupElement(cNodeID, unique, GroupElement.Sides.INBOUND, cnode.sources, cnode.targets);
            groupAssignments.put(cNodeID, elem);
          } else if (allPathsToFanOut(cNodeID, unique, groupAssignments, seen2)) {
            GroupElement elem = new GroupElement(cNodeID, unique, GroupElement.Sides.OUTBOUND, cnode.sources, cnode.targets);
            groupAssignments.put(cNodeID, elem);
          }
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Assign to groups based on outbound
  */

  private void assignOutbounds(Map<String, GroupElement> groupAssignments) {  

    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = (String)nodeRanking_.get(i);
      ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);
      if (cnode.outbounds != null) {        
        if (groupAssignments.get(cNodeID) == null) {
          GroupElement elem = new GroupElement(cNodeID, cnode.outbounds, GroupElement.Sides.OUTBOUND, cnode.sources, cnode.targets);       
          groupAssignments.put(cNodeID, elem);
        }
      }
    }
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Tag outbounds for class:
  */

  private void tagOutboundForClass(Map<String, GroupElement> groupAssignments) {  
    
    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = (String)nodeRanking_.get(i);
     
      GroupElement myGroup = (GroupElement)groupAssignments.get(cNodeID);
      if (myGroup == null) {  // Not in a group; nothing to do
        continue;
      }
       
      if (myGroup.side != GroupElement.Sides.CORE) {
        continue;
      }

      ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);      
      if (cnode.outbounds != null) {  // already tagged
        continue;
      }
        
      cnode.outbounds = myGroup.group;
      Iterator<String> trit = cnode.targets.iterator();
      while (trit.hasNext()) {
        String targ = trit.next();
        tagOutbound(targ, myGroup.group, groupAssignments);
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Tag an outbound node
  */

  private void tagOutbound(String nodeID, String groupID, Map<String, GroupElement> groupAssignments) {

    GroupElement myGroup = (GroupElement)groupAssignments.get(nodeID);
    if ((myGroup != null) && !myGroup.group.equals(groupID)) {  // already in another group
      return;
    }
 
    ClassedNode cnode = (ClassedNode)allNodes_.get(nodeID);    
    if (cnode.outbounds != null) {  // already tagged
      return; 
    }    
    
    cnode.outbounds = groupID;
    Iterator<String> trit = cnode.targets.iterator();
    while (trit.hasNext()) {
      String targ = trit.next();
      tagOutbound(targ, groupID, groupAssignments);
    }
    return;
  }
   
  /***************************************************************************
  ** 
  ** All paths from given node must terminate in checkval
  */

  private boolean allPathsChecker(String nodeID, String groupID, Map<String, GroupElement> groupAssignments, 
                                  Set<String> seen, GroupElement.Sides checkVal, boolean noDangerNodes) {

    GroupElement myGroup = (GroupElement)groupAssignments.get(nodeID);
    if ((myGroup != null) && !myGroup.group.equals(groupID)) {  // hit another group
      return (false);
    }
    
    if (noDangerNodes && (nonSubsetTargets_.get(nodeID) != null)) {
      return (false);
    }
    
    if ((myGroup != null) && (myGroup.side == checkVal)) {
      return (true);
    }

    if (seen.contains(nodeID)) {
      return (true);
    }
    seen.add(nodeID);
    
    ClassedNode cnode = allNodes_.get(nodeID);
   
    String unique = cnode.getUniqueInboundTag();
    if ((unique == null) || !unique.equals(groupID)) {
      return (false);
    }
 
    Iterator<String> trit = cnode.targets.iterator();
    if (!trit.hasNext()) {
      return (false);  // Dead-ends not allowed!
    }
    while (trit.hasNext()) {
      String targ = trit.next();
      if (!allPathsChecker(targ, groupID, groupAssignments, seen, checkVal, noDangerNodes)) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  ** 
  ** A path from given node goes to given node
  */

  private boolean aPathChecker(String nodeID, String groupID, Map<String, GroupElement> groupAssignments, Set<String> seen, String toNode) {

    GroupElement myGroup = groupAssignments.get(nodeID);
    if ((myGroup != null) && !myGroup.group.equals(groupID)) {  // hit another group
      return (false);
    }
    
    if (seen.contains(nodeID)) {
      return (false);
    }
    seen.add(nodeID);
    
    ClassedNode cnode = allNodes_.get(nodeID);
  
    Iterator<String> trit = cnode.targets.iterator();
    if (!trit.hasNext()) {
      return (false);  // Dead-ends not allowed!
    }
    while (trit.hasNext()) {
      String targ = trit.next();
      if (targ.equals(toNode)) {
        return (true);
      }
      if (aPathChecker(targ, groupID, groupAssignments, seen, toNode)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Guys who are on an outbound side who have a path to their core
  ** via group nodes, but not on a path from the core via group nodes,
  ** can be shifted to the inbound side.
  */

  private boolean shouldShiftToInputs(String nodeID, String groupID, Map<String, GroupElement> groupAssignments) {
    
    GroupElement myGroup = groupAssignments.get(nodeID);
    if (myGroup.side != GroupElement.Sides.OUTBOUND) {
      return (false);
    }

    if (aPathChecker(nodeID, groupID, groupAssignments, new HashSet<String>(), myGroup.group)) {
      if (!aPathChecker(myGroup.group, groupID, groupAssignments, new HashSet<String>(), nodeID)) {
        return (true);
      }
    }
    
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** All paths from given node must go to core
  */

  private boolean allPathsToCore(String nodeID, String groupID, Map<String, GroupElement> groupAssignments, Set<String> seen) {
    if (!allPathsChecker(nodeID, groupID, groupAssignments, seen, GroupElement.Sides.CORE, true)) {
      return (false);
    }
    return (nonSubsetTargets_.get(nodeID) == null);
  }
 
  /***************************************************************************
  ** 
  ** All paths from given node must go to fanout
  */

  private boolean allPathsToFanOut(String nodeID, String groupID, Map<String, GroupElement> groupAssignments, Set<String> seen) {
    if (!allPathsChecker(nodeID, groupID, groupAssignments, seen, GroupElement.Sides.OUTBOUND, true)) {
      return (false);
    }
    return (nonSubsetTargets_.get(nodeID) == null);
  }
 
  /***************************************************************************
  ** 
  ** Tag inbounds for class:
  */

  private void tagInboundForClass(Map<String, GroupElement> groupAssignments) {  
    int numNodes = nodeRanking_.size();
    for (int i = 0; i < numNodes; i++) {
      String cNodeID = nodeRanking_.get(i);
      
      //
      // If node is not already in a group, skip it:
      //
      
      GroupElement myGroup = groupAssignments.get(cNodeID);
      if (myGroup == null) {
        continue;
      }
      
      //
      // If we have already been here before, we skip it:
      //
      
      ClassedNode cnode = (ClassedNode)allNodes_.get(cNodeID);
      if (cnode.inboundsContains(myGroup.group)) {
        continue;
      }

      cnode.addToInbounds(myGroup.group);
              
      //
      // Propagate to sources:
      //
      
      Iterator<String> sit = cnode.sources.iterator();
      while (sit.hasNext()) {
        String src = sit.next();
        tagInbound(src, myGroup.group, groupAssignments);
      }
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** Tag an inbound node
  */

  private void tagInbound(String nodeID, String groupID, Map<String, GroupElement> groupAssignments) {
   
    //
    // We don't propagate through nodes already in a different group:
    //

    GroupElement myGroup = groupAssignments.get(nodeID);
    if ((myGroup != null) && !myGroup.group.equals(groupID)) {  // already in another group
      return;
    }     
          
    ClassedNode cnode = allNodes_.get(nodeID); 
        
    //
    // We don't propagate tags we have already seen:
    //
    
    if (cnode.inboundsContains(groupID)) {
      return;
    }
    cnode.addToInbounds(groupID);
    
    //
    // Propagate to our sources.
    //
   
    Iterator<String> sit = cnode.sources.iterator();
    while (sit.hasNext()) {
      String src = sit.next();
      tagInbound(src, groupID, groupAssignments);
    }
    return;
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static class ClassedNode {
    int classLevel;
    String name;
    private HashSet<String> inbounds_;    
    String outbounds;
    HashSet<String> sources;
    HashSet<String> targets;
 
    ClassedNode(int classLevel, String name) {
      this.classLevel = classLevel;
      this.name = name;
      inbounds_ = new HashSet<String>();
      outbounds = null;
      sources = new HashSet<String>();
      targets = new HashSet<String>();      
    }
    
    boolean inboundsContains(String group) {
      return (inbounds_.contains(group));
    }
    
    void addToInbounds(String group) {
      inbounds_.add(group);
      return;
    }
 
    //
    // May return null:
    //
    
    String getUniqueInboundTag() {    
      if (inbounds_.size() == 1) {
        return (inbounds_.iterator().next());
      }
      return (null);
    }
    
    void reset() {    
      inbounds_.clear();
      outbounds = null;
      return;
    }

    public String toString() {
      return ("cl = " + classLevel +
              " name = " + name +
              " ipc = " + inbounds_ +           
              " opc = " + outbounds +          
              " src = " + sources +
              " tar = " + targets);            
      
    }    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */
  
  public static void main(String[] argv) {
    
    ArrayList<String> nodes = new ArrayList<String>();
    nodes.add("A");
    nodes.add("B");
    nodes.add("C");
    nodes.add("D");
    nodes.add("E");
    nodes.add("F");
    nodes.add("g");    
    nodes.add("h");
    nodes.add("i");    
    nodes.add("j");
    nodes.add("k");
    nodes.add("l");    
    
    HashMap<String, Integer> classes = new HashMap<String, Integer>();
    classes.put("A", new Integer(0));
    classes.put("B", new Integer(0));
    classes.put("C", new Integer(0));
    classes.put("D", new Integer(0));
    classes.put("E", new Integer(0));
    classes.put("F", new Integer(0));
    classes.put("g", new Integer(1));    
    classes.put("h", new Integer(2));
    classes.put("i", new Integer(2));
    classes.put("j", new Integer(2));
    classes.put("k", new Integer(1));
    classes.put("l", new Integer(2));    

    ArrayList<Link> links = new ArrayList<Link>();
    links.add(new Link("g", "A"));
    links.add(new Link("g", "h"));    
    links.add(new Link("A", "h"));
    links.add(new Link("h", "B"));
    links.add(new Link("h", "C"));
    links.add(new Link("C", "i"));
    links.add(new Link("B", "i"));
    links.add(new Link("C", "j"));
    links.add(new Link("j", "D"));
    links.add(new Link("i", "E"));
    links.add(new Link("i", "F"));
    links.add(new Link("k", "l"));
    
    GraphSearcher gs = new GraphSearcher(new HashSet<String>(nodes), new HashSet<Link>(links));
    Map<String, Integer> queue = gs.topoSort(false);
    List<String> nodeRanking = gs.topoSortToPartialOrdering(queue);
    Collections.reverse(nodeRanking);
    System.out.println(nodeRanking);
     
    NodeGrouper ngr = new NodeGrouper(nodes, links, classes, nodeRanking);   
    Map<String, GroupElement> groups = ngr.buildGroups();
    System.out.println(groups);
    return;
  }     
}
