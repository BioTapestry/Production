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

package org.systemsbiology.biotapestry.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;

/****************************************************************************
**
** A Class
*/

public class GraphSearcher {
  
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
  private HashSet<Link> allEdges_;
  private ArrayList<String> nodeOrder_;
  private ArrayList<Link> edgeOrder_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(Genome gen, AllPathsResult allPaths) {
    
    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<Link>();
    edgeOrder_ = null;
    nodeOrder_ = null;    
  
    Iterator<String> ni = allPaths.getNodes();
    Iterator<String> li = allPaths.getLinks();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      String linkID = li.next();
      Linkage link = gen.getLinkage(linkID);      
      String trg = link.getTarget();
      String src = link.getSource();
      int sign = link.getSign();
      SignedLink cfl = new SignedLink(src, trg, sign);
      allEdges_.add(cfl);
    }
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    this(nps, false);    
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean invert) {
    
    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<Link>();
    edgeOrder_ = null;
    nodeOrder_ = null;    
  
    Iterator<Node> nit = nps.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      allNodes_.add(node.getID());
    }
    
    Iterator<Linkage> lit = nps.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String trg = link.getTarget();
      String src = link.getSource();
      int sign = link.getSign();
      SignedLink cfl = (invert) ? new SignedLink(trg, src, sign) : new SignedLink(src, trg, sign);
      allEdges_.add(cfl);
    }
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(Set<String> nodes, Set<Link> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<Link>();
    edgeOrder_ = null;
    nodeOrder_ = null;    

    Iterator<String> ni = nodes.iterator();
    Iterator<Link> li = links.iterator();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      Link link = li.next();
      allEdges_.add(link.clone());
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor.  Used to create a depth-first order that 
  ** retains original sibling order.  If link appears multiple
  ** times, the order is based on first appearance.
  */

  public GraphSearcher(List<String> nodes, List<Link> links) {

    allNodes_ = new HashSet<String>();
    allEdges_ = new HashSet<Link>();
    edgeOrder_ = new ArrayList<Link>();
    nodeOrder_ = new ArrayList<String>();

    Iterator<Link> li = links.iterator();
  
    nodeOrder_.addAll(nodes);
    allNodes_.addAll(nodes);    

    while (li.hasNext()) {
      Link link = li.next();
      if (!allEdges_.contains(link)) {
        edgeOrder_.add(link.clone());
      }
      allEdges_.add(link.clone());
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Nodes sorted by sources within groups of equal degree
  */

  public SortedSet<SourcedNodeDegree> nodeDegreeSetWithSource(List<String> sourceOrder, boolean ignoreDegree) {
 
    HashMap<String, Set<String>> allSrcs = new HashMap<String, Set<String>>();
    Iterator<Link> lit = allEdges_.iterator();
    while (lit.hasNext()) {
      Link nextLink = lit.next();
      String trg = nextLink.getTrg();
      Set<String> trgSources = allSrcs.get(trg);
      if (trgSources == null) {
        trgSources = new HashSet<String>();
        allSrcs.put(trg, trgSources);
      }
      trgSources.add(nextLink.getSrc());
    } 
    
    TreeSet<SourcedNodeDegree> retval = new TreeSet<SourcedNodeDegree>();

    Iterator<String> li = allSrcs.keySet().iterator();
    while (li.hasNext()) {
      String node = li.next();
      Set<String> trgSources = allSrcs.get(node);
      SourcedNodeDegree ndeg = new SourcedNodeDegree(node, sourceOrder, trgSources, ignoreDegree);
      retval.add(ndeg);
    }
    return (retval);
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
    Set<Link> currentEdges = new HashSet<Link>();
    Iterator<Link> li = allEdges_.iterator();
    while (li.hasNext()) {
      Link link = li.next();
      currentEdges.add(link.clone());
      }
      
    Map<String, Set<String>> outEdges = calcOutboundEdges(currentEdges);
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
  ** Depth-First Search
  */

  public List<QueueEntry> depthSearch() {
    //
    // Do until roots are exhausted
    //
    HashSet<String> visited = new HashSet<String>();
    
    Set<String> rootNodes = buildRootList(allNodes_, allEdges_);
    Map<String, Set<String>> outEdges = calcOutboundEdges(allEdges_); 

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
        Iterator<Link> eit = edgeOrder_.iterator();
        while (eit.hasNext()) {
          Link link = eit.next();
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
    Map<String, Set<String>> outEdges = calcOutboundEdges(allEdges_);    
    
    Iterator<String> rit = rootNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, rit.next()));
    }
    searchGutsBreadth(visited, queue, outEdges, retval, null);
    return (retval);
  }
  
  
  /***************************************************************************
  ** 
  ** Breadth-First Search
  */

  public List<QueueEntry> breadthSearchUntilStopped(Set<String> startNodes, CriteriaJudge judge) {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Do until roots are exhausted
    //
    
    HashSet<String> visited = new HashSet<String>();
    ArrayList<QueueEntry> queue = new ArrayList<QueueEntry>();
    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    
    Map<String, Set<String>> outEdges = calcOutboundEdges(allEdges_);    
    
    Iterator<String> rit = startNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, rit.next()));
    }
    searchGutsBreadth(visited, queue, outEdges, retval, judge);
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
      // Want reproducibility between VfA and Vfg layouts.  This is required.
      Collections.sort(listForLevel);
      retval.addAll(listForLevel);
    }
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  ** 
  ** With a given topo sort, force the given moveID to one end
  */

  public static Map<String, Integer> topoSortReposition(Map<String, Integer> origSort, String moveID, boolean moveMin) {
    
    Integer origCol = origSort.get(moveID);
    if (origCol == null) {
      return (new HashMap<String, Integer>(origSort));
    }
    
    int colVal = origCol.intValue();
    boolean colDup = false;
    int minVal = Integer.MAX_VALUE;
    int maxVal = Integer.MIN_VALUE;
    Iterator<String> oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      String key = oskit.next();
      if (key.equals(moveID)) {
        continue;
      }
      Integer checkCol = origSort.get(key);
      int chekVal = checkCol.intValue();
      if (chekVal < minVal) {
        minVal = chekVal;
      }
      if (chekVal > maxVal) {
        maxVal = chekVal;
      }
      if (chekVal == colVal) {
        colDup = true;
      }
    }
    
    int minMove;
    int maxMove;
    int moveCol;
    int inc;
    if (moveMin) {      
      if (colDup) { // to front, everybody up
        moveCol = minVal; 
        minMove = minVal;
        maxMove = maxVal;
        inc = 1;
      } else { // to front, guys below move up
        moveCol = (colVal < minVal) ? colVal : minVal; 
        minMove = minVal;
        maxMove = colVal - 1;
        inc = 1;
      }
    } else {
      if (colDup) { // to end, nobody moves
        moveCol = maxVal + 1;
        minMove = minVal - 1;
        maxMove = minVal - 1;
        inc = 0;       
      } else { // to end, guys above move down
        moveCol = (colVal > maxVal) ? colVal : maxVal;
        minMove = minVal - 1;
        maxMove = minVal - 1;
        inc = 0;      
      } 
    }
      
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
  
    oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      String key = oskit.next();
      if (key.equals(moveID)) {
        retval.put(moveID, new Integer(moveCol));
      } else {   
        Integer checkCol = origSort.get(key);
        int chekVal = checkCol.intValue();
        if ((chekVal >= minMove) && (chekVal <= maxMove)) {
          retval.put(key, new Integer(chekVal + inc));
        } else {
          retval.put(key, checkCol);
        }
      }
    }
    return (retval);
  }
  
  
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
  
  /****************************************************************************
  **
  ** A Class
  */
  
  public static class SourcedNodeDegree implements Comparable<SourcedNodeDegree> {
    
    private String node_;
    private int degree_;
    private boolean ignoreDegree_;
    private boolean[] srcs_;
    
    public SourcedNodeDegree(String node, List<String> srcOrder, Set<String> mySrcs, boolean ignoreDegree) {
      node_ = node;
      degree_ = mySrcs.size();
      ignoreDegree_ = ignoreDegree;
      int numSrc = srcOrder.size();
      srcs_ = new boolean[numSrc];
      for (int i = 0; i < numSrc; i++) {
        if (mySrcs.contains(srcOrder.get(i))) {
          srcs_[i] = true;
        }
      }
    }
 
    public String getNode() {
      return (node_);
    }
    
    public int getDegree() {
      return (degree_);
    }
    
    @Override
    public int hashCode() {
      return (node_.hashCode() + degree_);
    }
  
    @Override
    public String toString() {
      return (" node = " + node_ + " degree = " + degree_);
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNodeDegree)) {
        return (false);
      }
      SourcedNodeDegree otherDeg = (SourcedNodeDegree)other;    
      if (!ignoreDegree_ && (this.degree_ != otherDeg.degree_)) {
        return (false);
      }
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(SourcedNodeDegree otherDeg) {
      if (!ignoreDegree_ && (this.degree_ != otherDeg.degree_)) {
        // Higher degree is first:
        return (otherDeg.degree_ - this.degree_);
      }
      
      boolean iAmBigger = false;
      boolean heIsBigger = false;
      for (int i = 0; i < this.srcs_.length; i++) {

        if (this.srcs_[i]) {
          if (otherDeg.srcs_[i] == false) {
            iAmBigger = true;
            heIsBigger = false;
            break;
          }
        }
        if (otherDeg.srcs_[i]) {
          if (this.srcs_[i] == false) {
            iAmBigger = false;
            heIsBigger = true;
            break;
          } 
        }
      }
      if (iAmBigger) {
        return (-1);
      } else if (heIsBigger) {
        return (1);
      }
      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    } 
  }
  
  /***************************************************************************
  **
  ** Tell us when to stop looking
  */  
      
  public static interface CriteriaJudge {
    public boolean stopHere(String nodeID);  
  } 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build map from node to outbound edges
  */

  private Map<String, Set<String>> calcOutboundEdges(Set<Link> edges) {
    
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    Iterator<Link> li = edges.iterator();

    while (li.hasNext()) {
      Link link = li.next();
      String trg = link.getTrg();
      String src = link.getSrc();      
      Set<String> forSrc = retval.get(src);
      if (forSrc == null) {
        forSrc = new HashSet<String>();
        retval.put(src, forSrc);
      }
      forSrc.add(trg);  
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Build a root list
  */

  private Set<String> buildRootList(Set<String> nodes, Set<Link> edges) {
  
    HashSet<String> retval = new HashSet<String>();
    retval.addAll(nodes);
    
    Iterator<Link> ei = edges.iterator();
    while (ei.hasNext()) {
      Link link = ei.next();
      String trg = link.getTrg();
      retval.remove(trg);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Invert
  */

  private Set<Link> invertOutboundEdges(Map<String, Set<String>> outEdges) {
    
    HashSet<Link> retval = new HashSet<Link>();
    Iterator<String> ki = outEdges.keySet().iterator();

    while (ki.hasNext()) {
      String src = ki.next();
      Set<String> links = outEdges.get(src);
      Iterator<String> sit = links.iterator();
      while (sit.hasNext()) {
        String trg = sit.next();
        Link link = new Link(src, trg);
        retval.add(link);
      }
    }
 
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, HashSet<String> visited, Map<String, Set<String>> edgesFromSrc,
                              int depth, List<Link> edgeOrder, List<QueueEntry> results) {

    if (visited.contains(vertexID)) {
      return;
    }
    visited.add(vertexID);
    results.add(new QueueEntry(depth, vertexID));
    Set<String> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return;
    }
    
    if (edgeOrder != null) {
      Iterator<Link> eit = edgeOrder.iterator();
      while (eit.hasNext()) {
        Link link = eit.next();
        if (!vertexID.equals(link.getSrc())) {
          continue;
        }
        String targ = link.getTrg();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    } else {
      Iterator<String> eit = outEdges.iterator();
      while (eit.hasNext()) {
        String targ = eit.next();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Breadth-First Search guts
  */

  private void searchGutsBreadth(HashSet<String> visited, ArrayList<QueueEntry> queue, Map<String, Set<String>> edgesFromSrc, 
                                 List<QueueEntry> results, CriteriaJudge judge) {

    while (queue.size() > 0) {
      QueueEntry curr = queue.remove(0);
      if (visited.contains(curr.name)) {
        continue;
      }
      visited.add(curr.name);
      results.add(curr);
      
      if (judge != null) {
        if (judge.stopHere(curr.name)) {
          continue;
        }     
      }
      
      Set<String> outEdges = edgesFromSrc.get(curr.name);
      if (outEdges == null) {
        continue;
      }
      Iterator<String> oit = outEdges.iterator();
      while (oit.hasNext()) { 
        queue.add(new QueueEntry(curr.depth + 1, oit.next()));
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
    
    Map<String, Set<String>> outEdges = calcOutboundEdges(allEdges_);    
    
    while (true) {
      boolean changed = false;
      for (int i = maxLevel; i >= 0; i--) {
        List<String> nodeList = nodesAtLevel.get(new Integer(i));
        List<String> listCopy = new ArrayList<String>(nodeList);
        int numNodes = nodeList.size();
        for (int j = 0; j < numNodes; j++) {
          String currNode = listCopy.get(j);
          Set<String> targsForNode = outEdges.get(currNode);
          int min = getMinLevel(targsForNode, topoSort, i, maxLevel);
          if (min > i + 1) {
            List<String> higherNodeList = nodesAtLevel.get(new Integer(min - 1));
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

  private int getMinLevel(Set<String> targs, Map<String, Integer> topoSort, int currLevel, int maxLevel) {
    if (targs == null) {
      return (currLevel);
    }
    int min = maxLevel;    
    Iterator<String> trgit = targs.iterator();
    while (trgit.hasNext()) {
      String trg = trgit.next();
      Integer level = topoSort.get(trg);
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
