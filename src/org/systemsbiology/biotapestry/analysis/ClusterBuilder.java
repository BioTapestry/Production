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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Builds Clusters of targets with similar inputs
*/

public class ClusterBuilder {
  
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
  
  private HashMap<String, Integer> sourceOrder_;
  private int numSources_;
  private ArrayList<ClusterLeaf> leaves_;
  private ArrayList<ClusterParent> clusters_;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterBuilder(SpecialtyLayoutEngine.NodePlaceSupport nps, SortedSet<String> targetNodes) {
    
    //
    // Build the standard source order:
    //
        
    sourceOrder_ = new HashMap<String, Integer>();
    int currIndex = 0;
    Iterator<Linkage> lit = nps.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      Integer index = sourceOrder_.get(src);
      if (index == null) {
        sourceOrder_.put(src, new Integer(currIndex++));
      } 
    }
 
    //
    // Build the cluster leaves:
    //
    
    leaves_ = new ArrayList<ClusterLeaf>();
    Iterator<String> tnit = targetNodes.iterator();
    while (tnit.hasNext()) {
      String trgID = tnit.next();
      ClusterLeaf leaf = new ClusterLeaf(trgID, nps, sourceOrder_);
      leaves_.add(leaf);
    }
    
    clusters_ = new ArrayList<ClusterParent>();
    numSources_ = sourceOrder_.size();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Build the clusters
  */

  public void buildClustersFaster() {
    
    //
    // First pass quick kill.  Build up clusters of identical leaves
    //

    HashMap<ClusterLeaf.HashableVec, List<ClusterLeaf>> matcher = new  HashMap<ClusterLeaf.HashableVec, List<ClusterLeaf>>();
    
    int numLeaves = leaves_.size();
    for (int i = 0; i < numLeaves; i++) {
      ClusterLeaf currLeaf = leaves_.get(i);
      ClusterLeaf.HashableVec hv = currLeaf.getHashableVec();
      List<ClusterLeaf> matched = matcher.get(hv);
      if (matched == null) {
        matched = new ArrayList<ClusterLeaf>();
        matcher.put(hv, matched);
      }
      matched.add(currLeaf);
    }
    
    Iterator<List<ClusterLeaf>> mvit = matcher.values().iterator();
    while (mvit.hasNext()) {
      List<ClusterLeaf> matched = mvit.next();
      ArrayList<ClusterNode> kidList = new ArrayList<ClusterNode>(matched);
      ClusterParent cp = new ClusterParent(numSources_, true);
      cp.addChildren(kidList);
      clusters_.add(cp);
    }
    
    //
    // Go through the clusters and find the two with the largest
    // similarity measure.  Combine those two clusters.  Continue
    // until we are left with just one cluster.
    //

    // Move into an index so we can use index keys
    
    int count = 0;
    HashMap<Integer, ClusterParent> index = new HashMap<Integer, ClusterParent>();
    Iterator<ClusterParent> cito = clusters_.iterator();
    while (cito.hasNext()) {   
      ClusterParent currNode = cito.next();
      index.put(new Integer(count++), currNode);
    }
    
    // Find the n x n similarity measures
    
    TreeMap<Double, List<Integer[]>> ranker = new TreeMap<Double, List<Integer[]>>(Collections.reverseOrder());
    Iterator<Integer> ikit = index.keySet().iterator();
    while (ikit.hasNext()) {
      Integer key = ikit.next();
      ClusterParent currNode = index.get(key);
      Iterator<Integer> ikiti = index.keySet().iterator();
      while (ikiti.hasNext()) {
        Integer keyi = ikiti.next();
        if (keyi.intValue() <= key.intValue()) {
          continue;
        }
        ClusterParent innerNode = index.get(keyi);
        double sim = currNode.similarity(innerNode);
        Double rKey = new Double(sim);
        List<Integer[]> forRank = ranker.get(rKey);
        if (forRank == null) {
          forRank = new ArrayList<Integer[]>();
          ranker.put(rKey, forRank);          
        }
        Integer[] pair = new Integer[2];
        pair[0] = key;
        pair[1] = keyi;
        forRank.add(pair);
      }
    }    

    //
    // Take the best measure off the top, combine the two clusters.  We remember
    // dead clusters, but do not remove them from the similarity rank right away.
    // We do it lazily:
    //
    
    HashSet<Integer> deadKeys = new HashSet<Integer>();

    ArrayList<Integer[]> holder = new ArrayList<Integer[]>();
    
    if (ranker.isEmpty()) {
      return;
    }
    
    while (true) {
      Integer newMerge = null;
      ClusterParent cp = null;
      
      //
      // Find the best live pair and coalesce. Return a cleaned list to the 
      // rankings.
      //
      
      while (newMerge == null) {
        Double firstKey = ranker.firstKey();
        List<Integer[]> firstList = ranker.get(firstKey);
        int numFL = firstList.size();
        boolean first = true;
        holder.clear();
  
        for (int i = 0; i < numFL; i++) {
          Integer[] nextGuy = firstList.get(i);
          if (deadKeys.contains(nextGuy[0]) || deadKeys.contains(nextGuy[1])) {
            continue;
          }
          if (first) {
            ArrayList<ClusterNode> bestClusters = new ArrayList<ClusterNode>();
            bestClusters.add(index.get(nextGuy[0]));
            bestClusters.add(index.get(nextGuy[1]));
            deadKeys.add(nextGuy[0]);
            deadKeys.add(nextGuy[1]);
            index.remove(nextGuy[0]);
            index.remove(nextGuy[1]);
            cp = new ClusterParent(numSources_, false);
            cp.addChildren(bestClusters);
            newMerge = new Integer(count++);
            index.put(newMerge, cp);
            first = false;
          } else {
            holder.add(nextGuy);          
          }
        }
        if (!holder.isEmpty()) {
          firstList.clear();
          firstList.addAll(holder);
        } else {
          ranker.remove(firstKey);
        }
      }
      
      //
      // One cluster -> done!
      if (index.size() == 1) {
        break;
      }
      
      //
      // Calculate the new similarity ranks with the nee cluster.
        
      Iterator<Integer> ikiti = index.keySet().iterator();
      while (ikiti.hasNext()) {
        Integer keyi = ikiti.next();
        if (keyi.intValue() == newMerge.intValue()) {
          continue;
        }
        ClusterParent innerNode = index.get(keyi);
        double sim = cp.similarity(innerNode);
        Double rKey = new Double(sim);
        List<Integer[]> forRank = ranker.get(rKey);
        if (forRank == null) {
          forRank = new ArrayList<Integer[]>();
          ranker.put(rKey, forRank);          
        }
        Integer[] pair = new Integer[2];
        pair[0] = newMerge;
        pair[1] = keyi;
        forRank.add(pair);
      }   
    }
    clusters_.clear();
    clusters_.add(index.values().iterator().next());
    return;
  }

  /***************************************************************************
  ** 
  ** Get an in-order traversal of the clusters
  */

  public List<String> getOrdering() {
    
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<ClusterParent> cluit = clusters_.iterator();
    while (cluit.hasNext()) {
      ClusterParent nextNode = cluit.next();
      nextNode.appendToClusterList(retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get an in-order traversal of the clusters.  The return value is a
  ** list of lists, chunked into identical leaves.
  */

  public List<IntAnnotatedList> getChunkedOrdering() {
    
    ArrayList<IntAnnotatedList> retval = new ArrayList<IntAnnotatedList>();
    Iterator<ClusterParent> cluit = clusters_.iterator();
    while (cluit.hasNext()) {
      ClusterParent nextNode = cluit.next();
      nextNode.appendToChunkedClusterList(retval);
    }
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public static class IntAnnotatedList {
    
    private int val_;
    private ArrayList<String> list_;
    
    IntAnnotatedList(int val) {
      val_ = val;
      list_ = new ArrayList<String>();
    } 
    
    public int getVal() {
      return (val_);
    }
    
    public List<String> getList() {
      return (list_);
      
    }   
  }
}
