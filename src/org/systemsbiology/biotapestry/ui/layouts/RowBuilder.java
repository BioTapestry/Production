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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.analysis.ClusterBuilder;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;

/****************************************************************************
**
** Utilities for row building
*/

public class RowBuilder {

  private BTState appState_;
   
  public RowBuilder(BTState appState) {
    appState_ = appState;
  }
  
    
  /***************************************************************************
  **
  ** Assign target rows by previous position calculation
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> buildRowsByAssignment(List<GeneAndSatelliteCluster> sClustList, 
                                                                                 SortedMap<Integer, List<String>> assignments) {

    //
    // Figure out depth assignments to stay within maximum:
    //
    
    int numClust = sClustList.size();
    
    HashMap<String, GeneAndSatelliteCluster> idToGasc= new HashMap<String, GeneAndSatelliteCluster>();    
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      idToGasc.put(srcID, sc);
    }
    
    TreeMap<Integer, List<GeneAndSatelliteCluster>> retval = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    Iterator<Integer> aksit = assignments.keySet().iterator();
    while (aksit.hasNext()) {
      Integer rowNum = aksit.next();
      List<GeneAndSatelliteCluster> gascList = new ArrayList<GeneAndSatelliteCluster>();
      retval.put(rowNum, gascList);
      List<String> perRow = assignments.get(rowNum);
      int numPer = perRow.size();
      for (int i = 0; i < numPer; i++) {
        String coreID = perRow.get(i);
        GeneAndSatelliteCluster sc = idToGasc.get(coreID);
        gascList.add(sc);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Assign target rows by sort
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> buildClusterSeriesBySort(List<GeneAndSatelliteCluster> sClustList, 
                                                                                    Map<String, Integer> topoSort, int maxSize, Integer startKey) {

    //
    // Figure out depth assignments to stay within maximum:
    //
    
    int numClust = sClustList.size();
    
    TreeMap<Integer, Integer> numPerDepth = new TreeMap<Integer, Integer>();    
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      Integer depth = topoSort.get(srcID);      
      Integer perDepth = numPerDepth.get(depth);
      if (perDepth == null) {
        numPerDepth.put(depth, new Integer(1));
      } else {
        int newPerDepth = perDepth.intValue() + 1;
        numPerDepth.put(depth, new Integer(newPerDepth));
      }
    }
    
    HashMap<Integer, Integer> baseKeys = new HashMap<Integer, Integer>();
    int currKey = (startKey == null) ? 0 : startKey.intValue();
    Iterator<Integer> kit = numPerDepth.keySet().iterator();
    while (kit.hasNext()) {
      Integer depth = kit.next();
      Integer max = numPerDepth.get(depth);
      baseKeys.put(depth, new Integer(currKey));
      int numKeys = (max.intValue() / maxSize) + 1;      
      currKey += numKeys;
    }

    //
    // Now assign each to a jiggered depth:
    //
    
    TreeMap<Integer, List<GeneAndSatelliteCluster>> retval = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    HashMap<Integer, Integer> seenPerDepth = new HashMap<Integer, Integer>();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      Integer depth = topoSort.get(srcID);
      Integer perDepth = seenPerDepth.get(depth);
      int keyOffset;
      if (perDepth == null) {
        seenPerDepth.put(depth, new Integer(1));
        keyOffset = 0;
      } else {
        int newPerDepth = perDepth.intValue() + 1;
        seenPerDepth.put(depth, new Integer(newPerDepth));
        keyOffset = newPerDepth / maxSize;
      }
      Integer keyBase = (Integer)baseKeys.get(depth);
      int keyValue = keyBase.intValue() + keyOffset;
      Integer keyValueObj = new Integer(keyValue);
      List<GeneAndSatelliteCluster> perLev = retval.get(keyValueObj);
      if (perLev == null) {
        perLev = new ArrayList<GeneAndSatelliteCluster>();
        retval.put(keyValueObj, perLev);
      }
      perLev.add(sc);
    }
    
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Assign rows by first expression time
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> assignRowsByTime(List<GeneAndSatelliteCluster> geneClusters, int max, Integer startKey) {  
  
    Database db = appState_.getDB();
    TimeCourseData tcd = db.getTimeCourseData();
    TreeMap<Integer, List<GeneAndSatelliteCluster>> tClustRows = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    
    int numTClust = geneClusters.size();
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = geneClusters.get(i); 
      int firstTime = tcd.getFirstExpressionTime(tc.getCoreID());
      Integer rowObj = new Integer(firstTime);
      List<GeneAndSatelliteCluster> perTime = tClustRows.get(rowObj);
      if (perTime == null) {
        perTime = new ArrayList<GeneAndSatelliteCluster>();
        tClustRows.put(rowObj, perTime);
      }
      perTime.add(tc);
    }
    
    TreeMap<Integer, List<GeneAndSatelliteCluster>> retval = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    Iterator<List<GeneAndSatelliteCluster>> ntcit = tClustRows.values().iterator();
    int count = (startKey == null) ? 0 : startKey.intValue() * max;
    while (ntcit.hasNext()) {
      List<GeneAndSatelliteCluster> perName = ntcit.next();
      int numPN = perName.size();
      for (int i = 0; i < numPN; i++) {
        GeneAndSatelliteCluster tc = perName.get(i);
        int key = count++ / max;
        Integer rowObj = new Integer(key);
        List<GeneAndSatelliteCluster> perTime = retval.get(rowObj);
        if (perTime == null) {
          perTime = new ArrayList<GeneAndSatelliteCluster>();
          retval.put(rowObj, perTime);
        }
        perTime.add(tc);
      }
    }
    return (retval);
    
  }
  
  /***************************************************************************
  **
  ** Assign gene rows alphabetically
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> assignRowsByAlpha(List<GeneAndSatelliteCluster> geneClusters,
                                                                             SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                             int max, Integer startKey) {  

    TreeMap<String, List<GeneAndSatelliteCluster>> nameToGAS = new TreeMap<String, List<GeneAndSatelliteCluster>>();
    int numTClust = geneClusters.size();
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)geneClusters.get(i);
      String core = tc.getCoreID();
      String coreName = nps.getNode(core).getName().toUpperCase();
      List<GeneAndSatelliteCluster> perName = nameToGAS.get(coreName);
      if (perName == null) {
        perName = new ArrayList<GeneAndSatelliteCluster>();
        nameToGAS.put(coreName, perName);
      }
      perName.add(tc);
    }           
    
    TreeMap<Integer, List<GeneAndSatelliteCluster>> tClustRows = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    Iterator<List<GeneAndSatelliteCluster>> ntcit = nameToGAS.values().iterator();
    int count = (startKey == null) ? 0 : startKey.intValue() * max;
    while (ntcit.hasNext()) {
      List<GeneAndSatelliteCluster> perName = ntcit.next();
      int numPN = perName.size();
      for (int i = 0; i < numPN; i++) {
        GeneAndSatelliteCluster tc = perName.get(i);
        int key = count++ / max;
        Integer rowObj = new Integer(key);
        List<GeneAndSatelliteCluster> perTime = tClustRows.get(rowObj);
        if (perTime == null) {
          perTime = new ArrayList<GeneAndSatelliteCluster>();
          tClustRows.put(rowObj, perTime);
        }
        perTime.add(tc);
      }
    }
    return (tClustRows);
  }  

  /***************************************************************************
  **
  ** Assign genes rows by inputs
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> assignRowsByInputs(List<GeneAndSatelliteCluster> geneClusters,
                                                                              SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                              int max, boolean doChunked, 
                                                                              Integer startKey) {  
    //
    // Working to force full-net VfA region layout to match VfG stacked layout.  To stay
    // consistent, we want to order the nodeIDs (":#" suffix will not change order as long
    // as we stay in region!
    //

    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
    TreeSet<String> targetNodes = new TreeSet<String>(cc);    
    HashMap<String, GeneAndSatelliteCluster> coreToGAS = new HashMap<String, GeneAndSatelliteCluster>();
    int numTClust = geneClusters.size();
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = geneClusters.get(i);
      String core = tc.getCoreID();
      coreToGAS.put(core, tc);
      targetNodes.add(core);
    }  
 
    TreeMap<Integer, List<GeneAndSatelliteCluster>> tClustRows = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    ClusterBuilder cb = new ClusterBuilder(nps, targetNodes);
    // FYI: 8/20/13 Still taking about 4 seconds on my 6.7K node 28.4K link test network:
    cb.buildClustersFaster(); 
      
    if (doChunked) {
      List<ClusterBuilder.IntAnnotatedList> clusterOrder = cb.getChunkedOrdering();
      int coSize = clusterOrder.size();
      int currRow = (startKey == null) ? 0 : startKey.intValue();
      int currIndex = 0;
      while (currIndex < coSize) {
        ClusterBuilder.IntAnnotatedList chunkList = clusterOrder.get(currIndex);
        int clSize = chunkList.getList().size();
        if (clSize >= max) {
          currRow = assignLargeCluster(chunkList.getList(), tClustRows, coreToGAS, currRow, max);
          currIndex++;
        } else {
          currIndex = assignSmallClusterSeries(clusterOrder, currIndex, tClustRows, coreToGAS, currRow, max);
          currRow++;
        }
      }
    } else {
      List<String> clusterOrder = cb.getOrdering();
      int coSize = clusterOrder.size();
      int base = (startKey == null) ? 0 : startKey.intValue();
      for (int i = 0; i < coSize; i++) {
        String coreID = clusterOrder.get(i);
        GeneAndSatelliteCluster tc = coreToGAS.get(coreID);
        int key = (i / max) + base;
        Integer rowObj = new Integer(key);
        List<GeneAndSatelliteCluster> perRow = tClustRows.get(rowObj);
        if (perRow == null) {
          perRow = new ArrayList<GeneAndSatelliteCluster>();
          tClustRows.put(rowObj, perRow);
        }
        perRow.add(tc);
      }           
    }
       
    return (tClustRows);
  }  
  
  /***************************************************************************
  **
  ** Assign genes rows by source order
  */
  
  public SortedMap<Integer, List<GeneAndSatelliteCluster>> assignRowsBySourceOrder(List<GeneAndSatelliteCluster> geneClusters,
                                                                                   SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                                   int max, 
                                                                                   boolean doChunked, 
                                                                                   Integer startKey,
                                                                                   boolean ignoreDegree,
                                                                                   List<String> srcOrder) {  
    //
    // Working to force full-net VfA region layout to match VfG stacked layout.  To stay
    // consistent, we want to order the nodeIDs (":#" suffix will not change order as long
    // as we stay in region!
    //
    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
    TreeSet<String> targetNodes = new TreeSet<String>(cc);    
    HashMap<String, Set<String>> inboundLinkMap = new HashMap<String, Set<String>>();
    HashMap<String, GeneAndSatelliteCluster> coreToGAS = new HashMap<String, GeneAndSatelliteCluster>();
    int numTClust = geneClusters.size();
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = geneClusters.get(i);
      String core = tc.getCoreID();
      Set<String> inboundLinks = tc.getInboundLinks();
      inboundLinkMap.put(core, inboundLinks);
      coreToGAS.put(core, tc);
      targetNodes.add(core);
    }          
 
    TreeMap<Integer, List<GeneAndSatelliteCluster>> tClustRows = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
  //  ClusterBuilder cb = new ClusterBuilder(genome, targetNodes, inboundLinkMap);    
  //  cb.buildClusters(); 
      
    
    SortedSet<GraphSearcher.SourcedNodeDegree> srcsNodes = new GraphSearcher(nps).nodeDegreeSetWithSource(srcOrder, ignoreDegree);
    
    if (doChunked) {
      GraphSearcher.SourcedNodeDegree lastSND = null;
      List<GraphSearcher.SourcedNodeDegree> clusterOrder = new ArrayList<GraphSearcher.SourcedNodeDegree>(srcsNodes);
      int coSize = clusterOrder.size();
      int currRow = (startKey == null) ? 0 : startKey.intValue();
      int currIndex = 0;
      for (int i = 0; i < coSize; i++) {
        GraphSearcher.SourcedNodeDegree snd = clusterOrder.get(i);
        GeneAndSatelliteCluster tc = coreToGAS.get(snd.getNode());
        if (tc == null) {
          continue;
        }
        if (currIndex > max) {
          currIndex = 0;
          currRow++;
        } else if (lastSND != null) {
          if ((lastSND.getDegree() < 4) && (snd.getDegree() > 8)) {
            currIndex = 0;
            currRow++;
          }
        }
        Integer rowObj = new Integer(currRow);
        List<GeneAndSatelliteCluster> perRow = tClustRows.get(rowObj);
        if (perRow == null) {
          perRow = new ArrayList<GeneAndSatelliteCluster>();
          tClustRows.put(rowObj, perRow);
        }
        perRow.add(tc);
        currIndex++;
        lastSND = snd;
      }           
    } else { 
      List<GraphSearcher.SourcedNodeDegree> clusterOrder = new ArrayList<GraphSearcher.SourcedNodeDegree>(srcsNodes);
      int coSize = clusterOrder.size();
      int base = (startKey == null) ? 0 : startKey.intValue();
      for (int i = 0; i < coSize; i++) {
        GraphSearcher.SourcedNodeDegree snd = clusterOrder.get(i);
        GeneAndSatelliteCluster tc = coreToGAS.get(snd.getNode());
        if (tc == null) {
          continue;
        }
        int key = (i / max) + base;
        Integer rowObj = new Integer(key);
        List<GeneAndSatelliteCluster> perRow = tClustRows.get(rowObj);
        if (perRow == null) {
          perRow = new ArrayList<GeneAndSatelliteCluster>();
          tClustRows.put(rowObj, perRow);
        }
        perRow.add(tc);
      }           
    } 
       
    return (tClustRows);
  }  
   
  /***************************************************************************
  **
  ** Do assignment for a large cluster
  */
  
  private int assignLargeCluster(List<String> chunkList, SortedMap<Integer, List<GeneAndSatelliteCluster>> tClustRows, 
                                 Map<String, GeneAndSatelliteCluster> coreToGAS, int currRow, int max) {  
    int clSize = chunkList.size();
    if (clSize < max) {
      throw new IllegalArgumentException();
    }
    int balanced;
    int extra;
    if (clSize > max) {
      int numRows = (clSize / max) + 1;
      balanced = clSize / numRows;
      extra = clSize - (balanced * numRows);
    } else {
      balanced = clSize;
      extra = 0;
    }

    int perRow = 1;
    int extraPerRow = 0;
    for (int j = 0; j < clSize; j++) {
      String coreID = chunkList.get(j);
      GeneAndSatelliteCluster tc = coreToGAS.get(coreID);
      Integer rowObj = new Integer(currRow);
      List<GeneAndSatelliteCluster> perTime = tClustRows.get(rowObj);
      if (perTime == null) {
        perTime = new ArrayList<GeneAndSatelliteCluster>();
        tClustRows.put(rowObj, perTime);
      }
      perTime.add(tc);         
      if (perRow >= balanced) {
        if ((extra > 0) && (extraPerRow == 0)) {
          extra--;
          extraPerRow++;
          perRow++;
        } else {
          currRow++;
          perRow = 1;
          extraPerRow = 0;
        }
      } else {
        perRow++;
      }

    }
    currRow++;
    return (currRow);
  }    
  
  /***************************************************************************
  **
  ** Do assignment for a series of small clusters
  */
  
  private int assignSmallClusterSeries(List<ClusterBuilder.IntAnnotatedList> clusterOrder, int currIndex, 
                                       SortedMap<Integer, List<GeneAndSatelliteCluster>> tClustRows, 
                                       Map<String, GeneAndSatelliteCluster> coreToGAS, int currRow, int max) {  

    //
    // Get a group of candidates that fit in a line.  Order by number of inputs:
    //
    int coSize = clusterOrder.size();
    int perRow = 0;
    TreeMap<Integer, List<ClusterBuilder.IntAnnotatedList>> bySize = new TreeMap<Integer, List<ClusterBuilder.IntAnnotatedList>>();
    while ((perRow <= max) && (currIndex < coSize)) {
      ClusterBuilder.IntAnnotatedList chunkList = clusterOrder.get(currIndex);
      int clSize = chunkList.getList().size();
      if ((perRow + clSize) <= max) {
        int sourceCount = chunkList.getVal();
        Integer scObj = new Integer(sourceCount);
        List<ClusterBuilder.IntAnnotatedList> cand = bySize.get(scObj);
        if (cand == null) {
          cand = new ArrayList<ClusterBuilder.IntAnnotatedList>();
          bySize.put(scObj, cand);
        }
        cand.add(chunkList);
        currIndex++;
      }
      perRow += clSize;
    }
    
    //
    // Add so the elements with the most numerous inputs go first:
    //
    
    ArrayList<Integer> revList = new ArrayList<Integer>(bySize.keySet());
    Collections.reverse(revList);
    int numRev = revList.size();
    for (int i = 0; i < numRev; i++) {
      Integer scObj = (Integer)revList.get(i);
      List<ClusterBuilder.IntAnnotatedList> cand = bySize.get(scObj);
      int numCand = cand.size();
      for (int j = 0; j < numCand; j++) {
        ClusterBuilder.IntAnnotatedList chunkList = cand.get(j);
        int clSize = chunkList.getList().size();
        for (int k = 0; k < clSize; k++) {
          String coreID = chunkList.getList().get(k);
          GeneAndSatelliteCluster tc = coreToGAS.get(coreID);
          Integer rowObj = new Integer(currRow);
          List<GeneAndSatelliteCluster> perTime = tClustRows.get(rowObj);
          if (perTime == null) {
            perTime = new ArrayList<GeneAndSatelliteCluster>();
            tClustRows.put(rowObj, perTime);
          }
          perTime.add(tc);
        }
      }
    }  
    return (currIndex);
  }  
}
