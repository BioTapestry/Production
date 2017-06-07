/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

public class SimpleGraph<T extends Link> implements Cloneable {
  
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
  private HashSet<T> allEdges_;
  private ArrayList<String> nodeOrder_;
  private ArrayList<T> edgeOrder_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */
  
  @SuppressWarnings("unchecked")
  public SimpleGraph(SimpleAllPathsResult<T> allPaths) {
    
    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<T>();
    edgeOrder_ = null;
    nodeOrder_ = null;    
  
    Iterator<String> ni = allPaths.getNodes();
    Iterator<T> li = allPaths.getLinks();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      T link = li.next();      
      allEdges_.add((T)link.clone());
    }
  }
 
  /***************************************************************************
  **
  ** Constructor
  */

  @SuppressWarnings("unchecked")
  public SimpleGraph(Set<String> nodes, Set<T> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<T>();
    edgeOrder_ = null;
    nodeOrder_ = null;    

    Iterator<String> ni = nodes.iterator();
    Iterator<T> li = links.iterator();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      T link = li.next();
      allEdges_.add((T)link.clone());
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor.  Used to create a depth-first order that 
  ** retains original sibling order.  If link appears multiple
  ** times, the order is based on first appearance.
  */
  
  @SuppressWarnings("unchecked")
  public SimpleGraph(List<String> nodes, List<T> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<T>();
    edgeOrder_ = new ArrayList<T>();
    nodeOrder_ = new ArrayList<String>();
  
    nodeOrder_.addAll(nodes);
    allNodes_.addAll(nodes);    

    Iterator<T> li = links.iterator();
    while (li.hasNext()) {
      T link = li.next();
      if (!allEdges_.contains(link)) {
        allEdges_.add((T)link.clone());
        edgeOrder_.add((T)link.clone());
      }      
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  @Override
  @SuppressWarnings("unchecked")
  public SimpleGraph<T> clone() {
    try {
      SimpleGraph<T> newVal = (SimpleGraph<T>)super.clone();
      
      newVal.allNodes_ = new HashSet<String>(this.allNodes_);
      newVal.nodeOrder_ = (this.nodeOrder_ == null) ? null : new ArrayList<String>(this.nodeOrder_);
      newVal.allEdges_ = new HashSet<T>();
      Iterator<T> aeit = this.allEdges_.iterator();
      while (aeit.hasNext()) {
        T nextLink = aeit.next();
        newVal.allEdges_.add((T)nextLink.clone());
      }
      
      newVal.edgeOrder_ = null;
      if (this.edgeOrder_ != null) {
        newVal.edgeOrder_ = new ArrayList<T>();
        Iterator<T> eoit = this.edgeOrder_.iterator();
        while (eoit.hasNext()) {
          T nextLink = eoit.next();
          newVal.edgeOrder_.add((T)nextLink.clone());
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

  public SimpleGraph<T> intersection(SimpleGraph<T> other) {
    HashSet<String> nodeInter = new HashSet<String>();  
    DataUtil.intersection(this.allNodes_, other.allNodes_, nodeInter);
    HashSet<T> edgeInter = new HashSet<T>();  
    DataUtil.intersection(this.allEdges_, other.allEdges_, edgeInter);
    return (new SimpleGraph<T>(nodeInter, edgeInter));
  }
  
 /***************************************************************************
  ** 
  ** Calculate the intersection of this graph and the given graph
  */

  public SimpleGraph<T> union(SimpleGraph<T> other) {
    HashSet<String> nodeInter = new HashSet<String>();  
    DataUtil.union(this.allNodes_, other.allNodes_, nodeInter);
    HashSet<T> edgeInter = new HashSet<T>();  
    DataUtil.union(this.allEdges_, other.allEdges_, edgeInter);
    return (new SimpleGraph<T>(nodeInter, edgeInter));
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
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(allEdges_);
    Set<T> edgesOut = outEdges.get(nodeName);
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
    Map<String, Set<T>> inEdges = calcInboundEdges(allEdges_);
    Set<T> edgesIn = inEdges.get(nodeName);
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
  
  @SuppressWarnings("unchecked")
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
    Set<T> currentEdges = new HashSet<T>();
    Iterator<T> li = allEdges_.iterator();
    while (li.hasNext()) {
      T link = li.next();
      currentEdges.add((T)link.clone());
    }    
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(currentEdges);
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
    HashSet<String> visited = new HashSet<String>();
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(allEdges_); 
    HashMap<String, Integer> currSearchDepth = new HashMap<String, Integer>();

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

  @SuppressWarnings("unchecked")  
  public Map<String, Integer> repeatableDagTopoSort() {
    
    //
    // Assign all roots to level 0, add them to return list at that level.
    // Delete them and edges from them.  Recalulate root list and continue.
    //
    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Set<String> currentNodes = new HashSet<String>(allNodes_);
    
    //
    // Deep copy:
    //
    Set<T> currentEdges = new HashSet<T>();
    Iterator<T> li = allEdges_.iterator();
    while (li.hasNext()) {
      T link = li.next();
      currentEdges.add((T)link.clone());
    }    
    
    Map<String, SortedSet<T>> outEdges = calcOutboundEdgesWithLex(currentEdges);
    SortedSet<String> rootNodes = buildRootListWithLex(currentNodes, currentEdges);
    
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
      rootNodes = buildRootListWithLex(currentNodes, currentEdges);
    }
    return (retval);
  }
         
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  public List<QueueEntry> depthSearch() {
    //
    // Do until roots are exhausted
    //
    HashSet<String> visited = new HashSet<String>();
    
    Set<String> rootNodes = buildRootList(allNodes_, allEdges_);
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(allEdges_); 

    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    if (edgeOrder_ != null) {
      HashSet<String> seenRoots = new HashSet<String>();
      Iterator<String> nit = nodeOrder_.iterator();
      while (nit.hasNext()) {
        String currNode = nit.next();
        if (!rootNodes.contains(currNode)) {
          continue;
        }
        boolean gottaLink = false;
        Iterator<T> eit = edgeOrder_.iterator();
        while (eit.hasNext()) {
          T link = eit.next();
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
      Iterator<String> rit = rootNodes.iterator();
      while (rit.hasNext()) {
        searchGutsDepth(rit.next(), visited, outEdges, 0, null, retval);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Breadth-First Search
  */

  public List<QueueEntry> breadthSearch() {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Do until roots are exhausted
    //
    HashSet<String> visited = new HashSet<String>();
    ArrayList<QueueEntry> queue = new ArrayList<QueueEntry>();
    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    
    Set<String> rootNodes = buildRootList(allNodes_, allEdges_);
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(allEdges_);    
    
    Iterator<String> rit = rootNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, rit.next()));
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
  
  public SimpleAllPathsResult<T> getAllPaths(String sourceID, String targetID, int maxDepth) {  
            
    Map<String, SortedSet<T>> edges = calcOutboundEdges(allEdges_);
  
    SimpleAllPathsResult<T> retval = new SimpleAllPathsResult<T>(sourceID, targetID);
    SimplePathTracker<T> tracker;
    
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
    SimpleGraph<T> gs = new SimpleGraph<T>(retval);
    List<QueueEntry> ordering = gs.breadthSearch();
    Iterator<QueueEntry> oit = ordering.iterator();
    int depth = 0;
    while (oit.hasNext()) {
      QueueEntry qe = oit.next();
      retval.setDepth(qe.name, depth++);
    }
    retval.setDepth(targetID, depth);
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Generate a tracker for the simple autoregulation case
  */

  private SimplePathTracker<T> simpleAutoregulation(String source, String target, int maxDepth, Map<String, SortedSet<T>> edges) {
    if (!source.equals(target)) {
      throw new IllegalArgumentException();
    }
    
    SimplePathTracker<T> retval = new SimplePathTracker<T>();
    if (maxDepth < 1) {
      return (retval);
    }

    Set<T> outEdges = edges.get(source);
    if (outEdges == null) {
      return (retval);
    }
    
    Iterator<T> eit = outEdges.iterator();
    while (eit.hasNext()) {
      T link = eit.next();
      if (link.getTrg().equals(source)) {
        SimplePath<T> currPath = new SimplePath<T>();
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

  private SimplePathTracker<T> depthSearch(String source, String target, int maxDepth, Map<String, SortedSet<T>> edges) {
    SimplePath<T> currPath = new SimplePath<T>();
    SimplePathTracker<T> retval = new SimplePathTracker<T>();
    searchGutsDepth(source, target, 0, maxDepth, currPath, retval, edges);
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, String targetID,
                               int depth, int maxDepth, SimplePath<T> currPath, 
                               SimplePathTracker<T> tracker, Map<String, SortedSet<T>> edges) {
                                 
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
      tracker.addNewPath(currPath.clone());
      return;
    }
    Set<T> outEdges = edges.get(vertexID);
    if (outEdges == null) {
      return;
    }
    Iterator<T> eit = outEdges.iterator();
    while (eit.hasNext()) {
      T link = eit.next();
      boolean ok = currPath.addLink(link);
      if (!ok) {
        continue;  // loop detected; do not recurse
      }    
      searchGutsDepth(link.getTrg(), targetID, depth, 
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
    
    @Override
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

  private Set<String> buildRootList(Set<String> nodes, Set<T> edges) {
  
    HashSet<String> retval = new HashSet<String>();
    retval.addAll(nodes);
    
    Iterator<T> ei = edges.iterator();
    while (ei.hasNext()) {
      T link = ei.next();
      String trg = link.getTrg();
      retval.remove(trg);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a root list with lexicographic sorting
  */

  private SortedSet<String> buildRootListWithLex(Set<String> nodes, Set<T> edges) {
  
    TreeSet<String> retval = new TreeSet<String>();
    retval.addAll(nodes);
    
    Iterator<T> ei = edges.iterator();
    while (ei.hasNext()) {
      T link = ei.next();
      String trg = link.getTrg();
      retval.remove(trg);
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Build map from node to outbound edges
  */

  private Map<String, SortedSet<T>> calcOutboundEdges(Set<T> edges) {
    
    HashMap<String, SortedSet<T>> retval = new HashMap<String, SortedSet<T>>();
    Iterator<T> li = edges.iterator();

    while (li.hasNext()) {
      T link = li.next();
      String src = link.getSrc();      
      SortedSet<T> forSrc = retval.get(src);
      if (forSrc == null) {
        forSrc = new TreeSet<T>();
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

  private Map<String, Set<T>> calcInboundEdges(Set<T> edges) {
    
    HashMap<String, Set<T>> retval = new HashMap<String, Set<T>>();
    Iterator<T> li = edges.iterator();

    while (li.hasNext()) {
      T link = li.next();
      String trg = link.getTrg();     
      Set<T> forTrg = retval.get(trg);
      if (forTrg == null) {
        forTrg = new HashSet<T>();
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

  private Map<String, SortedSet<T>> calcOutboundEdgesWithLex(Set<T> edges) {
    
    HashMap<String, SortedSet<T>> retval = new HashMap<String, SortedSet<T>>();
    Iterator<T> li = edges.iterator();

    while (li.hasNext()) {
      T link = li.next();
      String src = link.getSrc();      
      SortedSet<T> forSrc = retval.get(src);
      if (forSrc == null) {
        forSrc = new TreeSet<T>();
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

  @SuppressWarnings("unchecked")
  private Set<T> invertOutboundEdges(Map<String, SortedSet<T>> outEdges) {
    
    HashSet<T> retval = new HashSet<T>();
    Iterator<String> ki = outEdges.keySet().iterator();

    while (ki.hasNext()) {
      String src = ki.next();
      SortedSet<T> links = outEdges.get(src);
      Iterator<T> sit = links.iterator();
      while (sit.hasNext()) {   
        T link = sit.next();
        retval.add((T)link.clone());
      }
    }
 
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, HashSet<String> visited, Map<String, SortedSet<T>> edgesFromSrc,
                                  int depth, List<T> edgeOrder, List<QueueEntry> results) {

    if (visited.contains(vertexID)) {
      return;
    }
    visited.add(vertexID);
    results.add(new QueueEntry(depth, vertexID));
    Set<T> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return;
    }
    
    if (edgeOrder != null) {
      Iterator<T> eit = edgeOrder.iterator();
      while (eit.hasNext()) {
        T link = eit.next();
        if (!vertexID.equals(link.getSrc())) {
          continue;
        }
        String targ = link.getTrg();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    } else {
      Iterator<T> eit = outEdges.iterator();
      while (eit.hasNext()) {
        T link = eit.next();
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

  private boolean cycleFinderGuts(String vertexID, HashSet<String> visited, Map<String, SortedSet<T>> edgesFromSrc, 
                                  int depth, Map<String, Integer> currSearchDepth) {

    visited.add(vertexID);
    Set<T> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return (false);
    }
    currSearchDepth.put(vertexID, new Integer(depth));

    Iterator<T> eit = outEdges.iterator();
    while (eit.hasNext()) {
      T link = eit.next();
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

  private void searchGutsBreadth(HashSet<String> visited, ArrayList<QueueEntry> queue, Map<String, SortedSet<T>> edgesFromSrc, List<QueueEntry> results) {

    while (queue.size() > 0) {
      QueueEntry curr = queue.remove(0);
      if (visited.contains(curr.name)) {
        continue;
      }
      visited.add(curr.name);
      results.add(curr);

      Set<T> outEdges = edgesFromSrc.get(curr.name);
      if (outEdges == null) {
        continue;
      }
      Iterator<T> oit = outEdges.iterator();
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

  private void contractTopoSort(Map<String, Integer> topoSort) {
    
    //
    // Make a list of nodes for each level.  Starting at the highest level,
    // get a node, and go through all the outbound links from that node.
    // Get the minimum level of all targets, and then move the node to
    // level min - 1.
    //
    // Iterate this process until no more changes can occur.
    //
    
    HashMap<Integer, List<String>> nodesAtLevel = new HashMap<Integer, List<String>>();
    int maxLevel = invertTopoSort(topoSort, nodesAtLevel);    
    
    if (maxLevel == -1) {  // nothing to do
      return;
    }
    
    Map<String, SortedSet<T>> outEdges = calcOutboundEdges(allEdges_);    
    
    while (true) {
      boolean changed = false;
      for (int i = maxLevel; i >= 0; i--) {
        List<String> nodeList = nodesAtLevel.get(Integer.valueOf(i));
        List<String> listCopy = new ArrayList<String>(nodeList);
        int numNodes = nodeList.size();
        for (int j = 0; j < numNodes; j++) {
          String currNode = listCopy.get(j);
          SortedSet<T> targsForNode = outEdges.get(currNode);
          int min = getMinLevel(targsForNode, topoSort, i, maxLevel);
          if (min > i + 1) {
            List<String> higherNodeList = nodesAtLevel.get(Integer.valueOf(min - 1));
            higherNodeList.add(currNode);
            nodeList.remove(currNode);
            topoSort.put(currNode, Integer.valueOf(min - 1));
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

  private int getMinLevel(Set<T> targs, Map<String, Integer> topoSort, int currLevel, int maxLevel) {
    if (targs == null) {
      return (currLevel);
    }
    int min = maxLevel;    
    Iterator<T> trgit = targs.iterator();
    while (trgit.hasNext()) {
      T link = trgit.next();
      Integer level = topoSort.get(link.getTrg());
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
