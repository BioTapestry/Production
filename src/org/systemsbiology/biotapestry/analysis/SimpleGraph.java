/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** A graph.  Note this is a refactoring to divorce the graph datatype from 
** the Genome network and Linkage and Node dependencies.
*/

public class SimpleGraph implements Cloneable {
  
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
  
  private HashSet<String> allNodes_;
  private HashSet<SignedLink> allEdges_;
  private ArrayList<String> nodeOrder_;
  private ArrayList<SignedLink> edgeOrder_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public SimpleGraph(SimpleAllPathsResult allPaths) {
    
    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<SignedLink>();
    edgeOrder_ = null;
    nodeOrder_ = null;    
  
    Iterator<String> ni = allPaths.getNodes();
    Iterator<SignedLink> li = allPaths.getLinks();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      SignedLink link = li.next();      
      allEdges_.add(link.clone());
    }
  }
 
  /***************************************************************************
  **
  ** Constructor
  */

  public SimpleGraph(Set<String> nodes, Set<SignedLink> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<SignedLink>();
    edgeOrder_ = null;
    nodeOrder_ = null;    

    Iterator<String> ni = nodes.iterator();
    Iterator<SignedLink> li = links.iterator();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      SignedLink link = li.next();
      allEdges_.add(link.clone());
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor.  Used to create a depth-first order that 
  ** retains original sibling order.  If link appears multiple
  ** times, the order is based on first appearance.
  */

  public SimpleGraph(List<String> nodes, List<SignedLink> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<SignedLink>();
    edgeOrder_ = new ArrayList<SignedLink>();
    nodeOrder_ = new ArrayList<String>();
  
    nodeOrder_.addAll(nodes);
    allNodes_.addAll(nodes);    

    Iterator<SignedLink> li = links.iterator();
    while (li.hasNext()) {
      SignedLink link = li.next();
      if (!allEdges_.contains(link)) {
        allEdges_.add(link.clone());
        edgeOrder_.add(link.clone());
      }      
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  public SimpleGraph clone() {
    try {
      SimpleGraph newVal = (SimpleGraph)super.clone();
      
      newVal.allNodes_ = new HashSet<String>(this.allNodes_);
      newVal.nodeOrder_ = (this.nodeOrder_ == null) ? null : new ArrayList<String>(this.nodeOrder_);
      newVal.allEdges_ = new HashSet<SignedLink>();
      Iterator<SignedLink> aeit = this.allEdges_.iterator();
      while (aeit.hasNext()) {
        SignedLink nextLink = aeit.next();
        newVal.allEdges_.add(nextLink.clone());
      }
      
      newVal.edgeOrder_ = null;
      if (this.edgeOrder_ != null) {
        newVal.edgeOrder_ = new ArrayList<SignedLink>();
        Iterator<SignedLink> eoit = this.edgeOrder_.iterator();
        while (eoit.hasNext()) {
          SignedLink nextLink = eoit.next();
          newVal.edgeOrder_.add(nextLink.clone());
        }
      }
      return (newVal);            
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }  
   
  /***************************************************************************
  ** 
  ** Calculate the intersection of this graph and the given graph
  */

  public SimpleGraph intersection(SimpleGraph other) {
    HashSet<String> nodeInter = new HashSet<String>();  
    DataUtil.intersection(this.allNodes_, other.allNodes_, nodeInter);
    HashSet<SignedLink> edgeInter = new HashSet<SignedLink>();  
    DataUtil.intersection(this.allEdges_, other.allEdges_, edgeInter);
    return (new SimpleGraph(nodeInter, edgeInter));
  }
  
 /***************************************************************************
  ** 
  ** Calculate the intersection of this graph and the given graph
  */

  public SimpleGraph union(SimpleGraph other) {
    HashSet<String> nodeInter = new HashSet<String>();  
    DataUtil.union(this.allNodes_, other.allNodes_, nodeInter);
    HashSet<SignedLink> edgeInter = new HashSet<SignedLink>();  
    DataUtil.union(this.allEdges_, other.allEdges_, edgeInter);
    return (new SimpleGraph(nodeInter, edgeInter));
  }
  
  /***************************************************************************
  ** 
  ** Get the number of nodes
  */

  public int nodeCount() {
    return (allNodes_.size());
  }
  
  
  /***************************************************************************
  ** 
  ** Get the out degree of a node
  */

  public int outDegree(String nodeName) {
    Map<String, Set<SignedLink>> outEdges = calcOutboundEdges(allEdges_);
    Set<SignedLink> edgesOut = outEdges.get(nodeName);
    if (edgesOut == null) {
      return (0);
    } else {
      return edgesOut.size();
    }
  }
  
  /***************************************************************************
  ** 
  ** Get the in degree of a node
  */

  public int inDegree(String nodeName) {
    Map<String, Set<SignedLink>> inEdges = calcInboundEdges(allEdges_);
    Set<SignedLink> edgesIn = inEdges.get(nodeName);
    if (edgesIn == null) {
      return (0);
    } else {
      return (edgesIn.size());
    }
  }

  /***************************************************************************
  ** 
  ** Get network roots
  */

  public Set<String> networkRoots() {   
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    return (buildRootList(allNodes_, allEdges_));
  }
  
  /***************************************************************************
  ** 
  ** Topo sort
  */

  public Map<String, Integer> topoSort(boolean compress) {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Assign all roots to level 0, add them to return list at that level.
    // Delete them and edges from them.  Recalulate root list and continue.
    //
    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Set<String> currentNodes = new HashSet<String>(allNodes_);
    //
    // Deep copy:
    //
    Set<SignedLink> currentEdges = new HashSet<SignedLink>();
    Iterator<SignedLink> li = allEdges_.iterator();
    while (li.hasNext()) {
      SignedLink link = li.next();
      currentEdges.add(link.clone());
    }    
    
    Map<String, Set<SignedLink>> outEdges = calcOutboundEdges(currentEdges);
    Set<String> rootNodes = buildRootList(currentNodes, currentEdges);
    
    int level = 0;
    while (!rootNodes.isEmpty()) {
      Integer ilevel = new Integer(level++);
      Iterator<String> rit = rootNodes.iterator();
      while (rit.hasNext()) {
        String nodeID = rit.next();
        retval.put(nodeID, ilevel);
        outEdges.remove(nodeID);
        currentNodes.remove(nodeID);
      }
      currentEdges = invertOutboundEdges(outEdges);
      rootNodes = buildRootList(currentNodes, currentEdges);
    }
    
    if (compress) {
      contractTopoSort(retval);
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** See if there is a cycle
  */

  public boolean foundACycle() {
    //
    // Do until roots are exhausted
    //
    HashSet visited = new HashSet();
    Map outEdges = calcOutboundEdges(allEdges_); 
    HashMap currSearchDepth = new HashMap();

    TreeSet<String> needToVisit = new TreeSet<String>(allNodes_);
    while (!needToVisit.isEmpty()) {    
      String topNode = needToVisit.first();
      if (cycleFinderGuts(topNode, visited, outEdges, 0, currSearchDepth)) {
        return (true);
      }
      needToVisit.removeAll(visited);
    }
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Topo sort of a dag.  We make this repeatable by running through all node
  ** lists in lexicographic order.
  */

  public Map<String, Integer> repeatableDagTopoSort() {
    
    //
    // Assign all roots to level 0, add them to return list at that level.
    // Delete them and edges from them.  Recalulate root list and continue.
    //
    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Set currentNodes = new HashSet(allNodes_);
    
    //
    // Deep copy:
    //
    Set currentEdges = new HashSet();
    Iterator li = allEdges_.iterator();
    while (li.hasNext()) {
      Link link = (Link)li.next();
      currentEdges.add(new Link(link));
    }    
    
    Map outEdges = calcOutboundEdgesWithLex(currentEdges);
    SortedSet rootNodes = buildRootListWithLex(currentNodes, currentEdges);
    
    int level = 0;
    while (!rootNodes.isEmpty()) {
      Integer ilevel = new Integer(level++);
      Iterator rit = rootNodes.iterator();
      while (rit.hasNext()) {
        String nodeID = (String)rit.next();
        retval.put(nodeID, ilevel);
        outEdges.remove(nodeID);
        currentNodes.remove(nodeID);
      }
      currentEdges = invertOutboundEdges(outEdges);
      rootNodes = buildRootListWithLex(currentNodes, currentEdges);
    }
    return (retval);
  }
         
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  public List depthSearch() {
    //
    // Do until roots are exhausted
    //
    HashSet visited = new HashSet();
    
    Set rootNodes = buildRootList(allNodes_, allEdges_);
    Map outEdges = calcOutboundEdges(allEdges_); 

    List retval = new ArrayList();
    if (edgeOrder_ != null) {
      HashSet seenRoots = new HashSet();
      Iterator nit = nodeOrder_.iterator();
      while (nit.hasNext()) {
        String currNode = (String)nit.next();
        if (!rootNodes.contains(currNode)) {
          continue;
        }
        boolean gottaLink = false;
        Iterator eit = edgeOrder_.iterator();
        while (eit.hasNext()) {
          Link link = (Link)eit.next();
          String src = link.getSrc();
          if (!currNode.equals(src)) {
            continue;
          }
          if (seenRoots.contains(src)) {
            continue;
          }
          seenRoots.add(src);
          gottaLink = true;
          searchGutsDepth(src, visited, outEdges, 0, edgeOrder_, retval);
        }
        if (!gottaLink) {
          visited.add(currNode);
          retval.add(new QueueEntry(0, currNode));
        }
      }
    } else {
      Iterator rit = rootNodes.iterator();
      while (rit.hasNext()) {
        searchGutsDepth((String)rit.next(), visited, outEdges, 0, null, retval);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Breadth-First Search
  */

  public List breadthSearch() {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Do until roots are exhausted
    //
    HashSet visited = new HashSet();
    ArrayList queue = new ArrayList();
    List retval = new ArrayList();
    
    Set rootNodes = buildRootList(allNodes_, allEdges_);
    Map outEdges = calcOutboundEdges(allEdges_);    
    
    Iterator rit = rootNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, (String)rit.next()));
    }
    searchGutsBreadth(visited, queue, outEdges, retval);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** A topo sort map gives column for each node.  Invert that to give a list
  ** of nodes for each column.
  */
  
  public int invertTopoSort(Map<String, Integer> topoSort, Map<Integer, List<String>> invert) {
        
    Iterator<String> kit = topoSort.keySet().iterator();
    int maxLevel = -1;
    while (kit.hasNext()) {
      String key = kit.next();
      Integer level = topoSort.get(key);
      int currLev = level.intValue();
      if (currLev > maxLevel) {
        maxLevel = currLev;
      }
      List<String> nodeList = invert.get(level);
      if (nodeList == null) {
        nodeList = new ArrayList<String>();
        invert.put(level, nodeList);
      }
      nodeList.add(key);
    }
    return (maxLevel);
  }
  
  /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<String> topoSortToPartialOrdering(Map<String, Integer> topoSort) {
    
    ArrayList<String> retval = new ArrayList<String>();
    TreeMap<Integer, List<String>> invert = new TreeMap<Integer, List<String>>();
    invertTopoSort(topoSort, invert);    
    Iterator<List<String>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<String> listForLevel = kit.next();
      retval.addAll(listForLevel);
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<String> topoSortToRepeatablePartialOrdering(Map<String, Integer> topoSort) {
    
    ArrayList<String> retval = new ArrayList<String>();
    TreeMap<Integer, List<String>> invert = new TreeMap<Integer, List<String>>();
    invertTopoSort(topoSort, invert);    
    Iterator<List<String>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<String> listForLevel = kit.next();
      TreeSet<String> sorted = new TreeSet<String>(listForLevel);
      retval.addAll(sorted);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Return all the paths that go from the source to the target, up to the
  ** specified depth limit.  When the path count exceeds the maxCount, the
  ** depth will be curtailed.
  **
  ** This is the start of refactoring to get path generation divorced from
  ** the genome code!
  */
  
  public SimpleAllPathsResult getAllPaths(String sourceID, String targetID, int maxDepth) {  
            
    Map edges = calcOutboundEdges(allEdges_);
  
    SimpleAllPathsResult retval = new SimpleAllPathsResult(sourceID, targetID);
    SimplePathTracker tracker;
    
    //
    // Handle the case of a simple (single hop only!  FIX ME!) autoregulatory feedback loop:
    //
    
    if (sourceID.equals(targetID)) {
      tracker = simpleAutoregulation(sourceID, targetID, maxDepth, edges);   
    } else {
      tracker = depthSearch(sourceID, targetID, maxDepth, edges);
    }
    retval.setTracker(tracker);
    // Impose ordering on the results
    SimpleGraph gs = new SimpleGraph(retval);
    List ordering = gs.breadthSearch();
    Iterator oit = ordering.iterator();
    int depth = 0;
    while (oit.hasNext()) {
      SimpleGraph.QueueEntry qe = (SimpleGraph.QueueEntry)oit.next();
      retval.setDepth(qe.name, depth++);
    }
    retval.setDepth(targetID, depth);
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Generate a tracker for the simple autoregulation case
  */

  private SimplePathTracker simpleAutoregulation(String source, String target, int maxDepth, Map edges) {
    if (!source.equals(target)) {
      throw new IllegalArgumentException();
    }
    
    SimplePathTracker retval = new SimplePathTracker();
    if (maxDepth < 1) {
      return (retval);
    }

    HashSet outEdges = (HashSet)edges.get(source);
    if (outEdges == null) {
      return (retval);
    }
    
    Iterator eit = outEdges.iterator();
    while (eit.hasNext()) {
      SignedLink link = (SignedLink)eit.next();
      if (link.getTrg().equals(source)) {
        SimplePath currPath = new SimplePath();
        currPath.addSimpleLoopLink(link);
        retval.addNewPath(currPath);
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  private SimplePathTracker depthSearch(String source, String target, int maxDepth, Map edges) {
    HashSet visited = new HashSet();
    SimplePath currPath = new SimplePath();
    SimplePathTracker retval = new SimplePathTracker();
    searchGutsDepth(source, target, visited, 0, maxDepth, currPath, retval, edges);
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, String targetID, HashSet visited, 
                               int depth, int maxDepth, SimplePath currPath, 
                               SimplePathTracker tracker, Map edges) {
                                 
    //
    // Do a depth first search from the source node, maintaining a path
    // definition as we go.  If the path length exceeds the maximum depth,
    // we stop. We use the current path for loop detection.  If we used a visited
    // list, we may not catch cases where the previous visit stopped due to
    // maximum depth.  That could be fixed if we cache the depth in the visited
    // list.
    //                                 

    if (depth++ > maxDepth) {
      return;
    }
    if (vertexID.equals(targetID)) {
      tracker.addNewPath((SimplePath)currPath.clone());
      return;
    }
    HashSet outEdges = (HashSet)edges.get(vertexID);
    if (outEdges == null) {
      return;
    }
    Iterator eit = outEdges.iterator();
    while (eit.hasNext()) {
      SignedLink link = (SignedLink)eit.next();
      boolean ok = currPath.addLink(link);
      if (!ok) {
        continue;  // loop detected; do not recurse
      }    
      searchGutsDepth(link.getTrg(), targetID, visited, depth, 
                      maxDepth, currPath, tracker, edges);
      currPath.pop();
    }
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public class QueueEntry {
    public int depth;
    public String name;
 
    QueueEntry(int depth, String name) {
      this.depth = depth;
      this.name = name;
    }
    
    public String toString() {
      return (name + " depth = " + depth);
    }
    
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Build a root list
  */

  private Set<String> buildRootList(Set<String> nodes, Set<SignedLink> edges) {
  
    HashSet<String> retval = new HashSet<String>();
    retval.addAll(nodes);
    
    Iterator<SignedLink> ei = edges.iterator();
    while (ei.hasNext()) {
      SignedLink link = ei.next();
      String trg = link.getTrg();
      retval.remove(trg);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a root list with lexicographic sorting
  */

  private SortedSet buildRootListWithLex(Set nodes, Set edges) {
  
    TreeSet retval = new TreeSet();
    retval.addAll(nodes);
    
    Iterator ei = edges.iterator();
    while (ei.hasNext()) {
      Link link = (Link)ei.next();
      String trg = link.getTrg();
      retval.remove(trg);
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Build map from node to outbound edges
  */

  private Map<String, Set<SignedLink>> calcOutboundEdges(Set<SignedLink> edges) {
    
    HashMap<String, Set<SignedLink>> retval = new HashMap<String, Set<SignedLink>>();
    Iterator<SignedLink> li = edges.iterator();

    while (li.hasNext()) {
      SignedLink link = li.next();
      String src = link.getSrc();      
      Set<SignedLink> forSrc = retval.get(src);
      if (forSrc == null) {
        forSrc = new HashSet<SignedLink>();
        retval.put(src, forSrc);
      }
      forSrc.add(link);  
    }
    return (retval);
  }
  
     
  /***************************************************************************
  **
  ** Build map from node to inbound edges
  */

  private Map<String, Set<SignedLink>> calcInboundEdges(Set<SignedLink> edges) {
    
    HashMap<String, Set<SignedLink>> retval = new HashMap<String, Set<SignedLink>>();
    Iterator<SignedLink> li = edges.iterator();

    while (li.hasNext()) {
      SignedLink link = li.next();
      String trg = link.getTrg();
      String src = link.getSrc();      
      Set<SignedLink> forTrg = retval.get(trg);
      if (forTrg == null) {
        forTrg = new HashSet<SignedLink>();
        retval.put(trg, forTrg);
      }
      forTrg.add(link);  
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build map from node to outbound edges
  */

  private Map calcOutboundEdgesWithLex(Set edges) {
    
    HashMap retval = new HashMap();
    Iterator li = edges.iterator();

    while (li.hasNext()) {
      Link link = (Link)li.next();
      String src = link.getSrc();      
      TreeSet forSrc = (TreeSet)retval.get(src);
      if (forSrc == null) {
        forSrc = new TreeSet();
        retval.put(src, forSrc);
      }
      forSrc.add(link);  
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** The old title of this routine is "invertOutboundEdges()", but I sure don't
  ** see any inversion happening here. It is just flattening the set!:
  */

  private Set<SignedLink> invertOutboundEdges(Map<String, Set<SignedLink>> outEdges) {
    
    HashSet<SignedLink> retval = new HashSet<SignedLink>();
    Iterator<String> ki = outEdges.keySet().iterator();

    while (ki.hasNext()) {
      String src = ki.next();
      Set<SignedLink> links = outEdges.get(src);
      Iterator<SignedLink> sit = links.iterator();
      while (sit.hasNext()) {   
        SignedLink link = sit.next();
        retval.add(link.clone());
      }
    }
 
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, HashSet visited, Map edgesFromSrc,
                                  int depth, List edgeOrder, List results) {

    if (visited.contains(vertexID)) {
      return;
    }
    visited.add(vertexID);
    results.add(new QueueEntry(depth, vertexID));
    HashSet outEdges = (HashSet)edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return;
    }
    
    if (edgeOrder != null) {
      Iterator eit = edgeOrder.iterator();
      while (eit.hasNext()) {
        Link link = (Link)eit.next();
        if (!vertexID.equals(link.getSrc())) {
          continue;
        }
        String targ = link.getTrg();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    } else {
      Iterator eit = outEdges.iterator();
      while (eit.hasNext()) {
        Link link = (Link)eit.next();
        String targ = link.getTrg();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    }
    return;
  }
  
  
  /***************************************************************************
  ** 
  ** Guts of (depth-first search) cycle finder:
  */

  private boolean cycleFinderGuts(String vertexID, HashSet<String> visited, Map<String, Set<SignedLink>> edgesFromSrc, 
                                  int depth, Map<String, Integer> currSearchDepth) {

    visited.add(vertexID);
    Set<SignedLink> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return (false);
    }
    currSearchDepth.put(vertexID, new Integer(depth));

    Iterator<SignedLink> eit = outEdges.iterator();
    while (eit.hasNext()) {
      SignedLink link = eit.next();
      String targ = link.getTrg();
      Integer prevDepth = currSearchDepth.get(targ);
      if ((prevDepth != null) && (prevDepth.intValue() < depth)) {
        currSearchDepth.remove(vertexID);
        return (true);
      }
      if (!visited.contains(targ)) {
        if (cycleFinderGuts(targ, visited, edgesFromSrc, depth + 1, currSearchDepth)) {
          currSearchDepth.remove(vertexID);
          return (true);
        }        
      }
    }
    currSearchDepth.remove(vertexID);
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Breadth-First Search guts
  */

  private void searchGutsBreadth(HashSet<String> visited, ArrayList<QueueEntry> queue, Map<String, Set<SignedLink>> edgesFromSrc, List<QueueEntry> results) {

    while (queue.size() > 0) {
      QueueEntry curr = queue.remove(0);
      if (visited.contains(curr.name)) {
        continue;
      }
      visited.add(curr.name);
      results.add(curr);

      Set<SignedLink> outEdges = edgesFromSrc.get(curr.name);
      if (outEdges == null) {
        continue;
      }
      Iterator<SignedLink> oit = outEdges.iterator();
      while (oit.hasNext()) { 
        queue.add(new QueueEntry(curr.depth + 1, oit.next().getTrg()));
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Contract the topo sort by moving nodes as far downstream as possible without
  ** breaking the partial ordering.
  */

  private void contractTopoSort(Map topoSort) {
    
    //
    // Make a list of nodes for each level.  Starting at the highest level,
    // get a node, and go through all the outbound links from that node.
    // Get the minimum level of all targets, and then move the node to
    // level min - 1.
    //
    // Iterate this process until no more changes can occur.
    //
    
    HashMap nodesAtLevel = new HashMap();
    int maxLevel = invertTopoSort(topoSort, nodesAtLevel);    
    
    if (maxLevel == -1) {  // nothing to do
      return;
    }
    
    Map outEdges = calcOutboundEdges(allEdges_);    
    
    while (true) {
      boolean changed = false;
      for (int i = maxLevel; i >= 0; i--) {
        List nodeList = (List)nodesAtLevel.get(new Integer(i));
        List listCopy = new ArrayList(nodeList);
        int numNodes = nodeList.size();
        for (int j = 0; j < numNodes; j++) {
          String currNode = (String)listCopy.get(j);
          Set targsForNode = (Set)outEdges.get(currNode);
          int min = getMinLevel(targsForNode, topoSort, i, maxLevel);
          if (min > i + 1) {
            List higherNodeList = (List)nodesAtLevel.get(new Integer(min - 1));
            higherNodeList.add(currNode);
            nodeList.remove(currNode);
            topoSort.put(currNode, new Integer(min - 1));
            changed = true;
          }
        }
      }
      if (!changed) {
        return;
      }
    }
  }  
  
  /***************************************************************************
  ** 
  ** Get the minimum level of all the target nodes
  */

  private int getMinLevel(Set targs, Map topoSort, int currLevel, int maxLevel) {
    if (targs == null) {
      return (currLevel);
    }
    int min = maxLevel;    
    Iterator trgit = targs.iterator();
    while (trgit.hasNext()) {
      Link link = (Link)trgit.next();
      Integer level = (Integer)topoSort.get(link.getTrg());
      int currLev = level.intValue();
      if (min > currLev) {
        min = currLev;
      }
    }
    return (min);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
