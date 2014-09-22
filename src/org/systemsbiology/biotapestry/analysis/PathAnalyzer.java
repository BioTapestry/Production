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

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;

/****************************************************************************
**
** A Class
*/

public class PathAnalyzer {
  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PathAnalyzer() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return the Ids of the network roots, i.e. nodes with no inputs!
  */
  
  public Set<String> getNetworkRoots(String genomeID, GenomeSource gSrc) {  
      
    Genome genome = gSrc.getGenome(genomeID);                                  
    HashSet<String> netNodes  = new HashSet<String>();
    HashSet<String> netTargets = new HashSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      netNodes.add(src);
      String trg = link.getTarget();
      netNodes.add(trg);
      netTargets.add(trg);
    }
    
    netNodes.removeAll(netTargets);
    return (netNodes);
  }
  
  /***************************************************************************
  **
  ** Return all the paths that go from the source to the target, up to the
  ** specified depth limit.  When the path count exceeds the maxCount, the
  ** depth will be curtailed:
  */
  
  public AllPathsResult getAllPaths(String genomeID, String sourceID, 
                                    String targetID, int maxDepth, 
                                    GenomeSource gSrc) {  
      
    Genome genome = gSrc.getGenome(genomeID);                                  
    HashMap<String, Set<Linkage>> edges = new HashMap<String, Set<Linkage>>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      Set<Linkage> linkList = edges.get(src);
      if (linkList == null) {
        linkList = new HashSet<Linkage>();
        edges.put(src, linkList);
      }
      linkList.add(link);
    }
    AllPathsResult retval = new AllPathsResult(sourceID, targetID);
    PathTracker tracker;
    //
    // Handle the case of a simple (single hop only!  FIX ME!) autoregulatory feedback loop:
    //
    if (sourceID.equals(targetID)) {
      tracker = simpleAutoregulation(sourceID, targetID, maxDepth, edges, gSrc, genomeID);   
    } else {
      tracker = depthSearch(sourceID, targetID, maxDepth, edges, gSrc, genomeID);
    }
    retval.setTracker(tracker);
    // Impose ordering on the results
    GraphSearcher gs = new GraphSearcher(genome, retval);
    List<GraphSearcher.QueueEntry> ordering = gs.breadthSearch();
    Iterator<GraphSearcher.QueueEntry> oit = ordering.iterator();
    int depth = 0;
    while (oit.hasNext()) {
      GraphSearcher.QueueEntry qe = oit.next();
      retval.setDepth(qe.name, depth++);
    }
    retval.setDepth(targetID, depth);
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Generate a tracker for the simple autoregulation case
  */

  private PathTracker simpleAutoregulation(String source, String target, int maxDepth, 
                                           HashMap<String, Set<Linkage>> edges, GenomeSource gSrc, String gKey) {
    if (!source.equals(target)) {
      throw new IllegalArgumentException();
    }
    
    PathTracker retval = new PathTracker();
    if (maxDepth < 1) {
      return (retval);
    }

    Set<Linkage> outEdges = edges.get(source);
    if (outEdges == null) {
      return (retval);
    }
    
    Iterator<Linkage> eit = outEdges.iterator();
    while (eit.hasNext()) {
      Linkage link = eit.next();
      if (link.getTarget().equals(source)) {
        Path currPath = new Path();
        currPath.addSimpleLoopLink(link, gSrc, gKey);
        retval.addNewPath(currPath);
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  private PathTracker depthSearch(String source, String target, int maxDepth, 
                                  HashMap<String, Set<Linkage>> edges, GenomeSource src, String gKey) {
    HashSet<String> visited = new HashSet<String>();
    Path currPath = new Path();
    PathTracker retval = new PathTracker();
    searchGutsDepth(source, target, visited, 0, maxDepth, currPath, retval, edges, src, gKey);
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, String targetID, HashSet<String> visited, 
                               int depth, int maxDepth, Path currPath, 
                               PathTracker tracker, HashMap<String, Set<Linkage>> edges, GenomeSource gSrc, String gKey) {
                                 
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
      tracker.addNewPath(new Path(currPath));
      return;
    }
    /*
    if (visited.contains(vertexID)) {
      Set tails = tracker.getTails(vertexID);
      Iterator tait = tails.iterator();
      while (tait.hasNext()) {
        Path pathTail = (Path)tait.next();
        Path fullPath = currPath.mergeTail(pathTail);
        if (fullPath == null) {  // makes a loop
          continue;
        }
        tracker.addNewPath(fullPath);
      }
      return;
    }
     */
    //visited.add(vertexID);
    Set<Linkage> outEdges = edges.get(vertexID);
    if (outEdges == null) {
      return;
    }
    Iterator<Linkage> eit = outEdges.iterator();
    while (eit.hasNext()) {
      Linkage link = eit.next();
      boolean ok = currPath.addLink(link, gSrc, gKey);
      if (!ok) {
        continue;  // loop detected; do not recurse
      }    
      searchGutsDepth(link.getTarget(), targetID, visited, depth, 
                      maxDepth, currPath, tracker, edges, gSrc, gKey);
      currPath.pop();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Return all the Nodes that are upstream of the given node in a map that
  ** provides hop distance
  */
  
  public AllPathsResult getAllPathsOld(String genomeID, String sourceID, 
                                    String targetID, int maxDepth, GenomeSource gSrc) {  
    Set<String> fanIn = getAllSources(genomeID, targetID, sourceID, maxDepth, gSrc);
    return (pruneToSingleSource(genomeID, targetID, sourceID, fanIn, gSrc));
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Return all the Nodes that are upstream of the given node in a map that
  ** provides hop distance
  */
  
  private Set<String> getAllSources(String genomeID, String targetID, 
                                    String sourceID, int maxDepth, GenomeSource gSrc) {
    TreeMap<String, Integer> processed = new TreeMap<String, Integer>();
    TreeMap<String, Integer> pending = new TreeMap<String, Integer>();
    
    Genome genome = gSrc.getGenome(genomeID);
    pending.put(targetID, new Integer(0));
    
    //
    // Find all the linkages that impinge on the target.  Add their
    // sources to the set, and check those impinging linkages.  If the
    // source is already in our set, we do not check again (breaking loops).
    // Continue until we hit sources with no inbound links.
    //
    
    while (!pending.isEmpty()) {
      String nid = pending.firstKey();
      Integer depth = pending.remove(nid);
      Integer depthP1 = new Integer(depth.intValue() + 1);      
      pending.remove(nid);      
      processed.put(nid, depth);
      if (depth.intValue() == maxDepth) {
 
        continue;
      }      
      if (!nid.equals(sourceID)) {
        Iterator<Linkage> lid = genome.getLinkageIterator();
        while (lid.hasNext()) {
          Linkage link = lid.next();
          if (!link.getTarget().equals(nid)) {
            continue;
          }
          String srcID = link.getSource();
          Integer srcDepth = processed.get(srcID);          
          boolean isProcessed = (srcDepth != null);
          boolean isPending = (pending.get(srcID) != null);
          if (isProcessed || isPending) {
            continue;
          }
          pending.put(srcID, depthP1);        
        }
      }
    }
    return (processed.keySet());
  }

  /***************************************************************************
  **
  ** Given a fan-in of all inputs to a node, and a specified source, prune the
  ** fan in to only contain those links and nodes that create a path from
  ** the source to the target.
  */
  
  private AllPathsResult pruneToSingleSource(String genomeID, String targetID, 
                                             String sourceID, Set<String> fanIn, GenomeSource gSrc) {

    AllPathsResult retval = new AllPathsResult(sourceID, targetID);
    TreeSet<String> pending = new TreeSet<String>();
    
    Genome genome = gSrc.getGenome(genomeID);
    if (!fanIn.contains(sourceID)) {
      throw new IllegalArgumentException();
    }
    pending.add(sourceID);
    
    //
    // Find all linkages emerging from the source.  If it hits somebody
    // in the fan-in, we add that node and link to the list, and add it to
    // the process list.  Keep going until the process list is empty.
    //
    
    while (!pending.isEmpty()) {
      String nid = pending.first();
      pending.remove(nid);      
      Iterator<Linkage> lid = genome.getLinkageIterator();
      while (lid.hasNext()) {
        Linkage link = lid.next();
        // Not from source?  Skip it.
        if (!link.getSource().equals(nid)) {
          continue;
        }
        // Target not in fan-in subnet?  Skip it.
        String currTargID = link.getTarget();
        if (!fanIn.contains(currTargID)) {
          continue;
        }
        // Target is the source?  Skip it; it's useless.
        if (link.getTarget().equals(sourceID)) {
          continue;
        }
        // Source is the target?  Skip it; it's useless.
        if (link.getSource().equals(targetID)) {
          continue;
        }        
        
        retval.addLink(link.getID());   
        retval.addNode(nid);        
        boolean isProcessed = (retval.getDepth(currTargID) != null);
        boolean isPending = pending.contains(currTargID);
        if (isProcessed || isPending) {
          continue;
        }
        pending.add(currTargID);
      }
    }
    //
    // Make sure target gets in:
    //
    retval.addNode(targetID);
    
    GraphSearcher gs = new GraphSearcher(genome, retval);
    List<GraphSearcher.QueueEntry> bfres = gs.breadthSearch();
    Iterator<GraphSearcher.QueueEntry> bit = bfres.iterator();
    int count = 0;
    while (bit.hasNext()) {
      GraphSearcher.QueueEntry qe = bit.next();
      retval.setDepth(qe.name, count++); //qe.depth);
    }
    retval.setDepth(targetID, count); // replace depth to force it to the end
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
