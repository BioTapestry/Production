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

import java.awt.Rectangle;
import java.util.Set;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.analysis.CycleFinder;    
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Represents a cluster of Source Clusters
*/

public class SuperSourceCluster {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int NEVER_FLIP_ORDER  = 0;
  public static final int ALWAYS_FLIP_ORDER = 1;
  public static final int FLIP_IF_SOURCE_IS_FROM_LEFT = 2;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final double RIGHT_X_PAD_PER_COL_ = 60.0;  
  private static final double RIGHT_X_PAD_PER_STACKED_COL_ = 40.0;
  private static final double RIGHT_X_PAD_FOR_INTERNAL_TRACES_ = 60.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private TreeMap<Integer, List<GeneAndSatelliteCluster>> memberClusters_;
  private HashSet<String> inboundLinks_;
  private HashSet<String> outboundLinks_;
  private HashSet<String> intraClusterLinks_;
  private HashSet<String> interClusterLinks_;
  private ArrayList<GeneAndSatelliteCluster> stackOrder_;
  private ArrayList<String> sourceOrderForPads_;  
  private HashMap<String, GeneAndSatelliteCluster> idToClust_;
  private HashMap<String, Integer> inboundTraces_;
  private HashMap<String, Double> outboundTraces_;
  private HashMap<Integer, Map<String, Integer>> internalOrderPerColumn_;  
  private HashMap<String, Double> internalTraces_;    
  private Point2D base_;
  private boolean isPrepped_;
  private GeneAndSatelliteCluster.DropDirectionOracle ddo_;
  private boolean isStacked_;
  private int stackedDirectOffset_;
  private double traceOffset_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public SuperSourceCluster(boolean isStacked, double traceOffset) {
    memberClusters_ = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    inboundLinks_ = new HashSet<String>();
    outboundLinks_ = new HashSet<String>();
    intraClusterLinks_ = new HashSet<String>();
    interClusterLinks_ = new HashSet<String>();
    idToClust_ = new HashMap<String, GeneAndSatelliteCluster>();  
    inboundTraces_ = new HashMap<String, Integer>();   
    outboundTraces_ = new HashMap<String, Double>();
    internalOrderPerColumn_ = new HashMap<Integer, Map<String, Integer>>();
    internalTraces_ = new HashMap<String, Double>();     
    stackOrder_ = new ArrayList<GeneAndSatelliteCluster>();    
    sourceOrderForPads_ = new ArrayList<String>();        
    isPrepped_ = false;
    isStacked_ = isStacked;
    traceOffset_ = traceOffset;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Get the bounds of JUST the nodes:
  */
  
  public Rectangle getNodeOnlyBounds(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    Rectangle retval = null;
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      Rectangle nsBounds = sc.getNodeOnlyBounds(nps, irx);
      if (retval == null) {
        retval = (Rectangle)nsBounds.clone();
      } else if (nsBounds != null) {
        Bounds.tweakBounds(retval, nsBounds);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Add another source cluster.
  */

  public void addSourceCluster(GeneAndSatelliteCluster newClust) {
    if (isPrepped_) {
      throw new IllegalStateException();
    }
    List<GeneAndSatelliteCluster> currClust;
    if (memberClusters_.isEmpty()) {
      currClust = new ArrayList<GeneAndSatelliteCluster>();
      memberClusters_.put(new Integer(0), currClust);
    } else {
      currClust = memberClusters_.get(memberClusters_.firstKey());
    }
    currClust.add(newClust);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Prepare the cluster
  */

  public void prep(GenomeSubset subset, SpecialtyLayoutEngine.NodePlaceSupport nps) {    
    if (isPrepped_ || memberClusters_.isEmpty()) {
      throw new IllegalStateException();
    }
    
    Genome baseGenome = subset.getBaseGenome();
    
    HashSet<String> clustsForTopoSort = new HashSet<String>();
    HashMap<String, String> srcClusts = new HashMap<String, String>();
    HashMap<String, String> trgClusts = new HashMap<String, String>();
    HashSet<String> interFeeds = new HashSet<String>();
    //
    // Everybody starts out in the first memberClusters_:
    //
    List<GeneAndSatelliteCluster> currClust = memberClusters_.get(memberClusters_.firstKey());
    int numCC = currClust.size();
    for (int i = 0; i < numCC; i++) {
      GeneAndSatelliteCluster sc = currClust.get(i);
      idToClust_.put(sc.getCoreID(), sc);
      clustsForTopoSort.add(sc.getCoreID());
      Set<String> relatedLinks = sc.getRelatedLinks(nps);
      Iterator<String> lit = relatedLinks.iterator();
      while (lit.hasNext()) {
        String linkID = lit.next();
        Linkage link = baseGenome.getLinkage(linkID);
        boolean isIn = sc.linkIsInbound(link);
        boolean isOut = sc.linkIsOutbound(link);
        //
        // Intercluster feedbacks can be both in and out:
        //
        if (isIn || isOut) {
          if (isIn) {
            inboundLinks_.add(linkID);
            trgClusts.put(linkID, sc.getCoreID());
          }
          if (isOut) {
            outboundLinks_.add(linkID);
            srcClusts.put(linkID, sc.getCoreID());
          }
          if (isIn && isOut) {
            interFeeds.add(linkID);            
          }
        } else if (sc.linkIsInternal(link)) {
          intraClusterLinks_.add(linkID);
        }
      }
    }
    
    //
    // The intersection of the inbound and outbound links is actually the
    // set of intercluster links.  Find it and remove those links from the
    // inbound and outbound sets to create the actual sets.
    //
    
    interClusterLinks_.addAll(inboundLinks_);
    interClusterLinks_.retainAll(outboundLinks_);     
    inboundLinks_.removeAll(interClusterLinks_);
    outboundLinks_.removeAll(interClusterLinks_);

    //
    // For a topo sort of the nodes, we just care about the
    // inter-cluster links.  We still need to eliminate
    // loops before sorting:
    // 
    
    HashSet<Link> links = new HashSet<Link>();
    
    Iterator<String> icit = interClusterLinks_.iterator();
    while (icit.hasNext()) {
      String linkID = icit.next();
      String src = srcClusts.get(linkID);
      String trg = trgClusts.get(linkID);
      Link cfl = new Link(src, trg);
      links.add(cfl);
      CycleFinder cf = new CycleFinder(clustsForTopoSort, links);
      if (cf.hasACycle()) {
        links.remove(cfl);
        interFeeds.add(linkID);
      }
    }

    //
    // Feedbacks in the cluster are routed externally:
    //
    
    interClusterLinks_.removeAll(interFeeds);
    inboundLinks_.addAll(interFeeds);
    outboundLinks_.addAll(interFeeds);
    
    GraphSearcher searcher = new GraphSearcher(clustsForTopoSort, links);    
    Map<String, Integer> topoSort = searcher.topoSort(false);
    TreeMap<Integer, List<String>> invert = new TreeMap<Integer, List<String>>();
    searcher.invertTopoSort(topoSort, invert);

    memberClusters_.clear();    
    Iterator<Integer> ivit = invert.keySet().iterator();
    while (ivit.hasNext()) {
      Integer key = ivit.next();
      List<String> perCol = invert.get(key);
      List<GeneAndSatelliteCluster> clustsForCol = new ArrayList<GeneAndSatelliteCluster>();
      memberClusters_.put(key, clustsForCol);
      int numPer = perCol.size();
      for (int i = 0; i < numPer; i++) {
        String scID = perCol.get(i);
        clustsForCol.add(idToClust_.get(scID));
      }
    }
    
    //
    // Assign outbound link order based on stacking order
    //
    
    TreeMap<String, List<GeneAndSatelliteCluster>> nameOrder = new TreeMap<String, List<GeneAndSatelliteCluster>>(String.CASE_INSENSITIVE_ORDER);
    Iterator<Integer> mcit = memberClusters_.keySet().iterator();
    while (mcit.hasNext()) {
      Integer mcKey = mcit.next();
      List<GeneAndSatelliteCluster> clustList = memberClusters_.get(mcKey);
      int clNum = clustList.size();
      for (int i = 0; i < clNum; i++) {
        GeneAndSatelliteCluster sc = clustList.get(i);
        String sid = sc.getCoreID();
        Node srcNode = baseGenome.getNode(sid);
        String srcName = srcNode.getName();
        List<GeneAndSatelliteCluster> srcs = nameOrder.get(srcName);
        if (srcs == null) {
          srcs = new ArrayList<GeneAndSatelliteCluster>();
          nameOrder.put(srcName, srcs);
        }
        srcs.add(sc);
      }
      
      Iterator<String> noit = nameOrder.keySet().iterator();
      while (noit.hasNext()) {
        String noKey = noit.next();
        List<GeneAndSatelliteCluster> srcs = nameOrder.get(noKey);
        int numSrc = srcs.size();
        for (int i = 0; i < numSrc; i++) {
          GeneAndSatelliteCluster sc = srcs.get(i);
          stackOrder_.add(sc);
        }
      }
      nameOrder.clear();
    }
    isPrepped_ = true;
    return;
  }  

  /***************************************************************************
  ** 
  ** All all associated links
  */

  public Set<String> getAssociatedLinks(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    HashSet<String> retval = new HashSet<String>();
    Iterator<Integer> mcit = memberClusters_.keySet().iterator();
    while (mcit.hasNext()) {
      Integer mcKey = mcit.next();
      List<GeneAndSatelliteCluster> clustList = memberClusters_.get(mcKey);
      int clNum = clustList.size();
      for (int i = 0; i < clNum; i++) {
        GeneAndSatelliteCluster sc = clustList.get(i);
        retval.addAll(sc.getRelatedLinks(nps));
      }
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Answers if the given link is inbound to this supercluster 
  */
  
  public boolean linkIsInbound(Linkage link) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    String linkID = link.getID();
    return (inboundLinks_.contains(linkID));
  }  
  
  /***************************************************************************
  ** 
  ** Answers if the given link is outbound from this supercluster
  */

  public boolean linkIsOutbound(Linkage link) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    String linkID = link.getID();
    return (outboundLinks_.contains(linkID));    
  }
  
  /***************************************************************************
  ** 
  ** Get the outbound link order
  */

  public int getOutboundLinkOrder(Linkage link) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    int count = 0;
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      if (sc.linkIsOutbound(link)) {
        return (count + sc.getOutboundLinkOrder(link));
      } else {
        count += sc.getOutboundLinkOrderMax();
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  ** 
  ** Get the outbound link order maximum
  */

  public int getOutboundLinkOrderMax() {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    int count = 0;
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      count += sc.getOutboundLinkOrderMax();
    }
    return (count);
  }  
  
  /***************************************************************************
  ** 
  ** Answers if the given link is internal to this supercluster
  */

  public boolean linkIsInternal(Linkage link) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    String linkID = link.getID();
    return (intraClusterLinks_.contains(linkID) || interClusterLinks_.contains(linkID));
  }
  
  /***************************************************************************
  ** 
  ** Order the inbound links
  */
  
  public void orderInboundTraces(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean baseAtTop, LinkAnnotatedSuper las, 
                                 SortedMap<Integer, String> topFeeds, SortedMap<Integer, String> bottomFeeds, 
                                 Map<String, Integer> seriesTraces, LinkAnnotatedSuper prevLas) {
    
    //
    // The rightmost inbound links are from external feedbacks (first from top, then
    // below the main link traces.  Then come the links from the immediately previous 
    // super cluster, followed by links from distantly past super clusters.
    //
    // Note that the scheme for assigning traces for the alternating scheme uses negative
    // numbers to get the traces coming in to the left of the cluster base.  That's great, but
    // not wanted for the stacked case, where we just want a nice positive set of numbers, with
    // the base of the cluster reflecting that!
    //
    
    //
    // Feedbacks:
    //
 
    int currTrace = 0;    
    Set<String> bottomSrcs = new HashSet<String>();
    
    if (!isStacked_) {
      Iterator<Integer> bit = bottomFeeds.keySet().iterator();
      while (bit.hasNext()) {
        Integer bVal = bit.next();
        String srcID = bottomFeeds.get(bVal);
        if (isASource(srcID, nps, las.backwardInputs)) {
          inboundTraces_.put(srcID, new Integer(currTrace--));
          bottomSrcs.add(srcID);
        }
      }
      //
      // Invert top order:
      //

      Set<Integer> tfk = topFeeds.keySet();
      ArrayList<Integer> revList = new ArrayList<Integer>(tfk);
      Collections.reverse(revList);
      Iterator<Integer> tfit = revList.iterator();    
      while (tfit.hasNext()) {
        Integer tVal = tfit.next();
        String srcID = topFeeds.get(tVal);
        if (isASource(srcID, nps, las.backwardInputs)) {
          inboundTraces_.put(srcID, new Integer(currTrace--));
        }
      } 
    }
    
    //
    // Now we do feeds from the immediately preceeding cluster, which are handled
    // specially
    //
    
    TreeMap<Integer, String> immedFeeds = new TreeMap<Integer, String>(Collections.reverseOrder());
    int firstTrace = Integer.MAX_VALUE;
    if (prevLas != null) {
      if (isStacked_) {
        throw new IllegalArgumentException();
      }
      Map<String, Integer> prevOutTraces = prevLas.ssc.outTraceOrder(nps, !baseAtTop, prevLas);
      TreeSet<Integer> forMax = new TreeSet<Integer>(prevOutTraces.values());
      firstTrace = (forMax.isEmpty()) ? Integer.MAX_VALUE : forMax.last().intValue();
      Iterator<String> iit = las.immediateInputs.iterator();
      while (iit.hasNext()) {
        String linkID = iit.next();
        Linkage link = nps.getLinkage(linkID);
        String srcID = link.getSource();
        Integer prevOutTrace = prevOutTraces.get(srcID);
        immedFeeds.put(prevOutTrace, srcID);
      }
    }

    //
    // We need to reserve slots for immediately previous traces that are not inbound.  Does
    // not apply to stacked case
    //
    
    int useTrace = currTrace;
    int minUseTrace = useTrace;
    Iterator<Integer> ikit = immedFeeds.keySet().iterator();
    while (ikit.hasNext()) {
      Integer iVal = ikit.next();
      String srcID = immedFeeds.get(iVal);
      int immedTrace = iVal.intValue();
      useTrace = currTrace - Math.abs(firstTrace - immedTrace);
      inboundTraces_.put(srcID, new Integer(useTrace--));
      if (useTrace < minUseTrace) {
        minUseTrace = useTrace;
      }
    }    
    currTrace = minUseTrace;
    
    //
    // Finally assign inputs from the distant past
    //
    
    TreeMap<Integer, String> past = new TreeMap<Integer, String>();
    addToPastForSet(las.pastInputs, nps, seriesTraces, past);
    addToPastForSet(las.afterRowSources, nps, seriesTraces, past);
    addToPastForSet(las.beforeRowSources, nps, seriesTraces, past);
    // With stacked cases, we don't care about the previous LAS and don't
    // have it in hand.  But still need to account for immediate inputs!
    if (isStacked_) {
      addToPastForSet(las.immediateInputs, nps, seriesTraces, past);
      addToPastForSet(las.backwardInputs, nps, seriesTraces, past);
    }  
     
    if (!isStacked_) {
      Iterator<String> pvit = past.values().iterator();
      while (pvit.hasNext()) {
        String srcID = pvit.next();
        inboundTraces_.put(srcID, new Integer(currTrace--)); 
      }
    } else {
      GeneAndSatelliteCluster gas = this.stackOrder_.get(0);
      Set<String> needForFan = gas.srcsToFanin(nps);
      Set<String> needForCore = gas.srcsToCore(nps);
      Iterator<String> pvit = past.values().iterator();
      HashMap<String, Integer> bogoOther = new HashMap<String, Integer>();
      HashMap<String, Integer> bogoIn = new HashMap<String, Integer>();
      int bogoOtherCount = 0;
      int bogoInCount = 0;
      // This approach segregates left drop for core and fan from others, while also reversing order!
      // Get the needForCore or Fanin stuff placed leftward:
      while (pvit.hasNext()) {
        String srcID = pvit.next();            
        if ((gas.useLeftDropsForStackedCore() && needForCore.contains(srcID)) || needForFan.contains(srcID)) {     
          bogoIn.put(srcID, new Integer(bogoInCount--));
        } else {
          bogoOther.put(srcID, new Integer(bogoOtherCount--));
        }
      }
      stackedDirectOffset_ = currTrace - bogoInCount;
      Iterator<String> biit = bogoIn.keySet().iterator();
      while (biit.hasNext()) {
        String srcID = biit.next();
        Integer bogoInVal = bogoIn.get(srcID);
        inboundTraces_.put(srcID, new Integer(bogoInVal.intValue() - bogoInCount)); 
      }
      Iterator<String> bbit = bogoOther.keySet().iterator();
      while (bbit.hasNext()) {
        String srcID = bbit.next();
        Integer bogoOtherVal = bogoOther.get(srcID);
        inboundTraces_.put(srcID, new Integer(stackedDirectOffset_ + (bogoOtherVal.intValue() - bogoOtherCount))); 
      }
      currTrace -= (bogoInCount + bogoOtherCount);
    }
   
    //
    // Source ordering for pads:
    //
    
    TreeMap<Integer, String> invertMap = new TreeMap<Integer, String>();
    TreeMap<Integer, String> fromBelowInvertMap = new TreeMap<Integer, String>();
    Iterator<String> itit = inboundTraces_.keySet().iterator();
    while (itit.hasNext()) {
      String srcID = itit.next();
      Integer iVal = inboundTraces_.get(srcID);
      if (baseAtTop && bottomSrcs.contains(srcID)) {
        fromBelowInvertMap.put(new Integer(-iVal.intValue()), srcID);
      } else {
        invertMap.put(iVal, srcID);
      }
    }
    
    //
    // Figure out internal link ordering.
    //

    ArrayList<String> internalSrcOrder = new ArrayList<String>();
    ArrayList<Integer> mcks = new ArrayList<Integer>(memberClusters_.keySet());
    if (!baseAtTop) {
      Collections.reverse(mcks);
    }
    Iterator<Integer> mcit = mcks.iterator();
    while (mcit.hasNext()) {
      Integer mcKey = mcit.next();
      Map<String, Integer> perCol = internalOrderPerColumn_.get(mcKey);
      TreeMap<Integer, String> sortByOrder = new TreeMap<Integer, String>();
      Iterator<String> pcit = perCol.keySet().iterator();
      while (pcit.hasNext()) {
        String srcID = pcit.next();
        Integer assign = perCol.get(srcID);
        sortByOrder.put(assign, srcID);
      }
      ArrayList<String> ioByCol = new ArrayList<String>(sortByOrder.values());
      if (baseAtTop) {
        Collections.reverse(ioByCol);
      }
      internalSrcOrder.addAll(ioByCol);
    }    
    
    //
    // Prune internal ordering:  NOTE!  What this means is
    // that there is a single ordering used for pads for the entire
    // ssc.  Note that guys that are feedbacks will get feedback ordering
    // even for gasClusters downstream.  Actually, pad ordering needs
    // to be on a per-gasCluster basis!  FIX ME!
    //   
    
    HashSet<String> imVals = new HashSet<String>(invertMap.values());
    HashSet<String> fbImVals = new HashSet<String>(fromBelowInvertMap.values());    
    ArrayList<String> allOuts = new ArrayList<String>();
    int outNum = internalSrcOrder.size();
    for (int j = 0; j < outNum; j++) {
      String src = internalSrcOrder.get(j);
      if (!imVals.contains(src) && !fbImVals.contains(src)) {
        allOuts.add(src);
      }
    }

    //
    // Which side the internal links go in viz the inbound links
    // depends on whether we are on the top or the bottom:
    //

    sourceOrderForPads_ = new ArrayList<String>();
    if (baseAtTop) {
      sourceOrderForPads_.addAll(fromBelowInvertMap.values());
      sourceOrderForPads_.addAll(invertMap.values());
      sourceOrderForPads_.addAll(allOuts);      
    } else {
      sourceOrderForPads_.addAll(allOuts);
      sourceOrderForPads_.addAll(invertMap.values());   
    }
 
    return;
  }
  
  /***************************************************************************
  ** 
  ** Add to past set:
  */
  
  public void addToPastForSet(Set<String> inputSet, SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, Integer> seriesTraces, SortedMap<Integer, String> past) {  
  
    Iterator<String> pait = inputSet.iterator();
    while (pait.hasNext()) {
      String linkID = pait.next();
      Linkage link = nps.getLinkage(linkID);
      String srcID = link.getSource();
      Integer seriesTrace = seriesTraces.get(srcID);
      if (seriesTrace == null) {
        // Safety valve.  Shouldn't happen...
        seriesTrace = new Integer(-25);
      }
      past.put(seriesTrace, srcID);
    }
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get the outbound traceOrder:
  */
  
  public Map<String, Integer> outTraceOrder(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean baseAtTop, LinkAnnotatedSuper las) {  
  
    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    ArrayList<String> outSrcs = new ArrayList<String>();    
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      outSrcs.addAll(sc.getOutboundSourceOrder());
    }
    
    if (!baseAtTop) {
      Collections.reverse(outSrcs);
    }
    int numSrc = outSrcs.size();
    int ocount = 0;
    for (int i = 0; i < numSrc; i++) {        
      String outSrc = outSrcs.get(i);
      if (needsOutboundDrop(outSrc, nps, las)) {
        retval.put(outSrc, new Integer(ocount++));
      }
    } 
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Order feedback traces (in the Y direction)
  */
  
  public void orderFeedbackTraces(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean baseAtTop, 
                                  SortedMap<Integer, String> upperFeeds, 
                                  SortedMap<Integer, String> lowerFeeds, 
                                  LinkAnnotatedSuper las) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }
    
    //
    //  Calc ALL internal trace locations, not just feedbacks
    //
    
    int feedCount = 0;
    TreeMap<Integer, String> sortedFeeds = new TreeMap<Integer, String>();
    Iterator<Integer> mcit = memberClusters_.keySet().iterator();
    while (mcit.hasNext()) {
      Integer mcKey = mcit.next();
      List<GeneAndSatelliteCluster> clustPerColumn = memberClusters_.get(mcKey);
      List<String> outSrcs = new ArrayList<String>();
      List<GeneAndSatelliteCluster> ordered = orderForColumn(stackOrder_, clustPerColumn);
      int num = ordered.size();
      for (int i = 0; i < num; i++) {
        GeneAndSatelliteCluster sc = ordered.get(i);    
        outSrcs.addAll(sc.getOutboundSourceOrder());
      }
      int numSrc = outSrcs.size();
 
      //
      // Calc internal trace locations.
      //

      HashMap<String, Integer> perCol = new HashMap<String, Integer>();
      internalOrderPerColumn_.put(mcKey, perCol);      
      int icount = 0;
      for (int i = 0; i < numSrc; i++) {        
        String outSrc = outSrcs.get(i);
        if (needsInterclusterDrop(outSrc, nps, las)) {
          perCol.put(outSrc, new Integer(icount++));
        }
        if (isASource(outSrc, nps, las.backwardOutputs)) {
          sortedFeeds.put(new Integer(feedCount++), outSrc);
        }
        if (isASource(outSrc, nps, las.beforeRowTargets)) {
          sortedFeeds.put(new Integer(feedCount++), outSrc);
        }
        if (isASource(outSrc, nps, las.afterRowTargets)) {
          sortedFeeds.put(new Integer(feedCount++), outSrc);
        }
      }
    }
    
    //
    // Assign feedback locations
    //
    
    int sign;
    int feedKey;
    SortedMap<Integer, String> dest;
    if (baseAtTop) {
      sign = 1;
      feedKey = (lowerFeeds.isEmpty()) ? 0 : lowerFeeds.lastKey().intValue();    
      dest = lowerFeeds; 
    } else {
      sign = -1;
      feedKey = (upperFeeds.isEmpty()) ? 0 : upperFeeds.firstKey().intValue();    
      dest = upperFeeds;      
    }
        
    Iterator<Integer> sfit = sortedFeeds.keySet().iterator();
    while (sfit.hasNext()) {
      Integer feed = sfit.next();
      String feedSrc = sortedFeeds.get(feed);
      feedKey += sign;
      dest.put(new Integer(feedKey), feedSrc);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the ordered list of clusters per column (a messy hack...)
  */
  
  private List<GeneAndSatelliteCluster> orderForColumn(List<GeneAndSatelliteCluster> allOrdered, List<GeneAndSatelliteCluster> clustPerColumn) {
    ArrayList<GeneAndSatelliteCluster> retval = new ArrayList<GeneAndSatelliteCluster>();
    
    int numCpc = clustPerColumn.size();    
    int numOrd = allOrdered.size();
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = allOrdered.get(i);
      String srcID = sc.getCoreID();
      for (int j = 0; j < numCpc; j++) {
        GeneAndSatelliteCluster sct = clustPerColumn.get(j);
        if (sct.getCoreID().equals(srcID)) {
          retval.add(sc);
          break;
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get height
  */
  
  public double getHeight(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, double traceDistance) {
 
    int numOrd = stackOrder_.size();    
    double rowY = 0.0;
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      GeneAndSatelliteCluster.ClusterDims oh = sc.getClusterDims(nps, irx);
      rowY += oh.getFullHeightIncrement();
    }
    return (rowY);
  }
  
  /***************************************************************************
  ** 
  ** Locate the super cluster
  */
  
  public Point2D locate(Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps,
                        DataAccessContext irx, 
                        boolean baseAtTop, Map<String, Boolean> isOnTop, double traceDistance,
                        LinkAnnotatedSuper las, Map<String, Double> lastOutboundTraces, 
                        LinkAnnotatedSuper nextLas, double minSeparation, 
                        Double matchMax) {
    if (!isPrepped_) {
      throw new IllegalStateException();
    }

    // sets base_ too:
    double colX;
    if (isStacked_) {
      colX = locateStackedLeftEdge(basePos, traceDistance);
    } else {
      colX = locateLeftEdge(basePos, traceDistance, lastOutboundTraces, isOnTop, baseAtTop, las);
    }
    
    //
    // Figure out x positions:
    //    

    HashMap<String, Integer> colPerSource = new HashMap<String, Integer>();
    HashMap<Integer, Double> xPosPerCol = new HashMap<Integer, Double>(); 
    HashMap<String, Double> prelimInternalTraces = new HashMap<String, Double>();
    
    int colCount = 0;   
    Iterator<Integer> mcit = memberClusters_.keySet().iterator();
    while (mcit.hasNext()) {
      Integer mcKey = mcit.next();
      List<GeneAndSatelliteCluster> clustPerColumn = memberClusters_.get(mcKey);
      Integer colObj = new Integer(colCount++);
      ArrayList<String> outSrcs = new ArrayList<String>();
      double maxWidth = 0.0;
      List<GeneAndSatelliteCluster> ordered = orderForColumn(stackOrder_, clustPerColumn);
      int num = ordered.size();
      for (int i = 0; i < num; i++) {
        GeneAndSatelliteCluster sc = ordered.get(i);    
        colPerSource.put(sc.getCoreID(), colObj);
        double width = sc.getClusterDims(nps, irx).width;
        if (width > maxWidth) {
          maxWidth = width;
        }
        outSrcs.addAll(sc.getOutboundSourceOrder());
      }
      xPosPerCol.put(colObj, new Double(colX));
      colX += maxWidth + ((isStacked_) ? RIGHT_X_PAD_PER_STACKED_COL_ : RIGHT_X_PAD_PER_COL_);
 
      //
      // Calc internal trace locations.  Do backwards if base is at bottom
      //
   
      
      // These are the guys heading out from us:
      boolean stackedNeedsInterDrop = false;
      Map<String, Integer> perCol = internalOrderPerColumn_.get(mcKey);
      int maxAssign = 0;
      Iterator<String> pcit = perCol.keySet().iterator();
      while (pcit.hasNext()) {
        String srcID = pcit.next();
        stackedNeedsInterDrop = stackedNeedsInterDrop || justIntraClusterDrop(srcID, nps, las);
        Integer assign = perCol.get(srcID);
        int assignVal = assign.intValue();
        if (assignVal > maxAssign) {
          maxAssign = assignVal;
        }
      }
      
      if (!isStacked_ || stackedNeedsInterDrop) {
      
        //
        // This is calculating the x requirements for the traces emerging from
        // a column of GASCs within our stack.
        // Note that any conservative width estimate for fan-out width
        // (reflected in a large maxWidth) causes these trace assignments
        // to be far to the right of where they could be!
        // 
        // Note for stacked usage, this would create unneeded internal traces,
        // since we only have one GASC and no routing is needed to others in 
        // stack.  BUT if the stacked output is just intercluster, we need the
        // point to launch the link correctly
        //
      
        double oldColX = colX;
        double rightColX = colX + ((double)maxAssign * traceDistance);      
        colX += ((double)maxAssign * traceDistance) + ((isStacked_) ? 0.0 : RIGHT_X_PAD_FOR_INTERNAL_TRACES_);

        pcit = perCol.keySet().iterator();
        while (pcit.hasNext()) {
          String srcID = pcit.next();
          Integer assign = perCol.get(srcID);
          double traceX = (baseAtTop) ? (oldColX + (assign.doubleValue() * traceDistance))
                                      : (rightColX - (assign.doubleValue() * traceDistance));
          prelimInternalTraces.put(srcID, new Double(traceX));
        }
      }
    }
      

    //
    // Figure out Y:
    //
    
    int numOrd = stackOrder_.size();    
    double totalHeight = 0.0;
    HashMap<String, Double> yPosPerRow = new HashMap<String, Double>();
    HashMap<String, Double> extraBumps = new HashMap<String, Double>();
    Double zeroD = new Double(0.0);
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      String scCoreID = sc.getCoreID();
      GeneAndSatelliteCluster.ClusterDims oh = sc.getClusterDims(nps, irx);
      double nextHeight = oh.getFullHeightIncrement();
      Double extraBump;
      if ((matchMax != null) && (matchMax.doubleValue() > nextHeight)) {
        totalHeight += matchMax.doubleValue();
        extraBump = new Double(matchMax.doubleValue() - nextHeight);
      } else {
        totalHeight += nextHeight;
        extraBump = zeroD;
      }     
      yPosPerRow.put(scCoreID, new Double(totalHeight));
      extraBumps.put(scCoreID, extraBump);
    }
    
    //
    // Invert as needed.  Retval is the upper right point for custers above the bus, lower right
    // fot clusters below.
    //
    
    double retval;
    double baseY = base_.getY();
    if (!baseAtTop) { // Base at the bottom
      baseY -= totalHeight; // treated as TopY
      retval = baseY;
    } else {
      retval = baseY + totalHeight; // treated as BottomY
    }
    
    //
    // Figure out cluster Y placement, subject to a final X offset:
    //
    
    double nextYPos = 0.0;
    HashMap<String, Point2D> relativePos = new HashMap<String, Point2D>();
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      String scCoreID = sc.getCoreID();      
      Integer colAssignObj = colPerSource.get(scCoreID);            
      double xPos = xPosPerCol.get(colAssignObj).doubleValue();
      double extraBump = extraBumps.get(scCoreID).doubleValue();      
      Point2D pos = new Point2D.Double(xPos, baseY + nextYPos + extraBump);
      relativePos.put(scCoreID, pos);
      nextYPos = yPosPerRow.get(scCoreID).doubleValue();           
    } 

    //
    // Locate the outbound drops, subject to a final X offset:
    //
        
    Map<String, Integer> outTraces = outTraceOrder(nps, baseAtTop, las);    
    
    HashMap<String, Double> prelimTraces = new HashMap<String, Double>();   
    double maxX = colX;
    
    Iterator<String> otit = outTraces.keySet().iterator();
    while (otit.hasNext()) {
      String srcID = otit.next();
      Integer count = outTraces.get(srcID);
      //
      // Note that this is where outbound traces may be incorrectly placed to the left
      // of a complex fanout origin if the width value for the cluster is undervalued!
      // Outbound feeds would then have a slight backup overlap.
      //
      double traceX = colX + (count.doubleValue() * traceDistance);
      prelimTraces.put(srcID, new Double(traceX));
      if (traceX > maxX) {
        maxX = traceX;
      }
    }
         
    //
    // Figure out if we need to do a wholesale rightward shift of this SSC.
    // This might occur because the next SSC to be placed is dependent on
    // our location, and if there is not enough space for it to have all
    // inbound drops not collide with the SSC to the left of it, the best
    // place to fix that problem is to shift ourselves to the right.
    // (^^^^^^^^ this is discussing the alternating layout strategy)
    //

    double offsetX = 0.0;    
    if (nextLas != null) {
      // Note this is calculated on the NEXT las!
      // SO baseAtTop NEEDS TO BE NEGATED!!!
      MinXAndLeftShift mals = nextLas.ssc.minXAndShiftNeeded(traceDistance, prelimTraces, isOnTop, !baseAtTop, nextLas);
      if (mals != null) {
        if (mals.minX < (basePos.getX() + minSeparation)) {
          offsetX = basePos.getX() - mals.minX + traceDistance + minSeparation;
        }
      }
    }

    //
    // Final placement, including necessary offset:
    //
    
    Iterator<String> rpit = relativePos.keySet().iterator();
    while (rpit.hasNext()) {
      String scCoreID = rpit.next();
      Point2D sscPos = relativePos.get(scCoreID);
      GeneAndSatelliteCluster sc = idToClust_.get(scCoreID);
      Point2D finalPos = new Point2D.Double(sscPos.getX() + offsetX, sscPos.getY());
      // Final pos here is on the ~left-hand end of the core node:
      sc.locateAsSource(finalPos, nps, irx);
    }     
    
    Iterator<String> ptit = prelimTraces.keySet().iterator();
    while (ptit.hasNext()) {
      String outSrc = ptit.next();
      Double traceXObj = prelimTraces.get(outSrc);
      Double finalTraceX = new Double(traceXObj.doubleValue() + offsetX);
      outboundTraces_.put(outSrc, finalTraceX);
    }        
    
    Iterator<String> pitit = prelimInternalTraces.keySet().iterator();
    while (pitit.hasNext()) {
      String srcID = pitit.next();
      Double traceXObj = prelimInternalTraces.get(srcID);
      Double finalTraceX = new Double(traceXObj.doubleValue() + offsetX);
      internalTraces_.put(srcID, finalTraceX);
    }
    
    maxX += offsetX;
    return (new Point2D.Double(maxX, retval));
  }

  /***************************************************************************
  ** 
  ** Get the outbound traces
  */
  
  public Map<String, Double> getOutboundTraces() {
    return (outboundTraces_);
  }
  
  /***************************************************************************
  ** 
  ** Get inbound link count
  */
  
  public int getInboundLinkCount() {
    return (inboundLinks_.size());
  }  
  
  /***************************************************************************
  ** 
  ** Get all the sources in the series
  */
  
  public Set<String> getAllSources() {
    HashSet<String> retval = new HashSet<String>();
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      retval.addAll(sc.getAllSources());
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Get all the inputs in the series
  */
  
  public Set<String> getAllInputs() {
    HashSet<String> retval = new HashSet<String>();
    int numStack = stackOrder_.size();
    for (int i = 0; i < numStack; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      retval.addAll(sc.getInputs());
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Answer if source is present in the set of links
  */
  
  public boolean isASource(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps, Set<String> linkIDs) {  
    Iterator<String> icit = linkIDs.iterator();
    while (icit.hasNext()) {
      String linkID = icit.next();
      Linkage link = nps.getLinkage(linkID);
      String chkSrcID = link.getSource();
      if (chkSrcID.equals(srcID)) {
        return (true);
      }
    }
    return (false);
  }
  
   /***************************************************************************
  ** 
  ** Answer if we need JUST an intercluster drop
  */
  
  public boolean justIntraClusterDrop(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps, LinkAnnotatedSuper las) {

    if (isASource(srcID, nps, las.afterRowTargets)) {
      return (false);
    }  
    if (isASource(srcID, nps, las.beforeRowTargets)) {
      return (false);
    }
    if (isASource(srcID, nps, las.backwardOutputs)) {
      return (true);
    }
    if (isASource(srcID, nps, intraClusterLinks_)) {
      return (true);
    }   
    return (false);
  }
 
  /***************************************************************************
  ** 
  ** Answer if we need an intercluster drop
  */
  
  public boolean needsInterclusterDrop(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps, LinkAnnotatedSuper las) {
    //
    // We need an intercluster drop if we have an intercluster target, or
    // we have a feedback to a previous cluster.
    //

    if (isASource(srcID, nps, interClusterLinks_)) {
      return (true);
    }
    if (isASource(srcID, nps, las.backwardOutputs)) {
      return (true);
    }
    if (isASource(srcID, nps, las.afterRowTargets)) {
      return (true);
    }  
    if (isASource(srcID, nps, las.beforeRowTargets)) {
      return (true);
    }  
    
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Answer if we need an outbound drop
  */
  
  public boolean needsOutboundDrop(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps, LinkAnnotatedSuper las) {
    //
    // We need an outbound drop if we have an outbound link.
    //
    
    if (isASource(srcID, nps, las.forwardOutputs)) {
      return (true);
    }
    if (isASource(srcID, nps, las.immediateOutputs)) {
      return (true);
    }  
    if (isASource(srcID, nps, las.afterRowTargets)) {
      return (true);
    }  
    if (isASource(srcID, nps, las.beforeRowTargets)) {
      return (true);
    }  
    
    
    return (false);    
  }  
  
  /***************************************************************************
  ** 
  ** Handle outbound link routing preparation, *****and all internal links******
  */
  
  public void startOutboundLinks(SpecialtyLayoutEngine.NodePlaceSupport nps,
                                 DataAccessContext irx, 
                                 SpecialtyInstructions si, 
                                 Map<Integer, Double> trackToY, 
                                 Map<String, Integer> srcToTrack,
                                 Map<String, Double> srcToFeedbackY, boolean baseAtTop,
                                 Map<String, SuperSrcRouterPointSource> traceDefs, 
                                 LinkAnnotatedSuper las, 
                                 boolean forStackedClusters) {
    
    //
    // Get the GeneAndSatelliteCluster to do their internal links; build outbound point
    // sources for each discrete source:
    //
    
    HashSet<String> seenSrcs = new HashSet<String>();
    int numOrd = stackOrder_.size();
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      Map<String, SpecialtyLayoutLinkData> sinMap = sc.routeInternalLinks(nps, irx);
      Iterator<String> smit = sinMap.keySet().iterator();
      while (smit.hasNext()) {
        String srcID = smit.next();
        SpecialtyLayoutLinkData sin = sinMap.get(srcID);
        if (si.hasLinkPointsForSrc(srcID)) {
          SpecialtyLayoutLinkData sinlp = si.getLinkPointsForSrc(srcID);
          sinlp.merge(sin);
        } else {
          si.setLinkPointsForSrc(srcID, sin);
        }
      }
      Iterator<String> asi = sc.getAllSourceIterator();
      while (asi.hasNext()) {
        seenSrcs.add(asi.next());
      }
      
      sc.routeOutboundLinks(nps, irx, internalTraces_, outboundTraces_, traceDefs,
                            baseAtTop, srcToTrack, 
                            trackToY, srcToFeedbackY, forStackedClusters);
    }
     
    //
    // Do internal links:
    //
    

    Map<String, List<String>> perSrc = targetsPerSourceInStackOrder(nps);
    Iterator<String> psit = perSrc.keySet().iterator();
    while (psit.hasNext()) {
      String srcID = psit.next();
      if (!seenSrcs.contains(srcID)) {
        continue;
      }
      
      SuperSrcRouterPointSource outForSrc = traceDefs.get(srcID);             
      SpecialtyLayoutLinkData sin = si.getOrStartLinkPointsForSrc(srcID);
 
      MetaClusterPointSource lps = new MetaClusterPointSource(srcID, outForSrc, MetaClusterPointSource.FOR_INTRA_SRC);
      lps.setIntraParams(traceOffset_);
      List<String> trgs = perSrc.get(srcID);
      int numT = trgs.size();
      for (int i = 0; i < numT; i++) {
        String scCoreID = trgs.get(i); 
        GeneAndSatelliteCluster sc = idToClust_.get(scCoreID);
        boolean topGASC = (stackOrder_.get(0) == sc);
        sc.inboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
      }        
    }
     
    return;    
  }

 /***************************************************************************
  ** 
  ** Create a list of intercluster targets per source, in stack order
  */
  
  public Map<String, List<String>> targetsPerSourceInStackOrder(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    
    HashMap<String, SortedMap<Integer, String>> prepMap = new HashMap<String, SortedMap<Integer, String>>();
    
    int numOrd = stackOrder_.size();
    Iterator<String> icit = interClusterLinks_.iterator();
    while (icit.hasNext()) {
      String linkID = icit.next();
      Linkage link = nps.getLinkage(linkID); 
      String srcID = link.getSource();
      SortedMap<Integer, String> tm = prepMap.get(srcID);
      if (tm == null) {
        tm = new TreeMap<Integer, String>();
        prepMap.put(srcID, tm);
      }
      boolean seenSrc = false;
      for (int i = 0; i < numOrd; i++) {
        GeneAndSatelliteCluster sc = stackOrder_.get(i);
        //
        // Only recording links that are downward from the source.
        // Feedbacks handled later
        //
        if (!seenSrc) {
          if (sc.linkIsOutbound(link)) {
            seenSrc = true;
          }
          continue;
        }
        
        String clustSrcID = sc.getCoreID();
        if (sc.linkIsInbound(link)) { //clustSrcID.equals(trgID)) {
          tm.put(new Integer(i), clustSrcID);
          break;
        }
      }    
    }
        
    HashMap<String, List<String>> retval = new HashMap<String, List<String>>();
    Iterator<String> pmkit = prepMap.keySet().iterator();
    while (pmkit.hasNext()) {
      String srcID = pmkit.next();
      SortedMap<Integer, String> tm = prepMap.get(srcID);
      ArrayList<String> trgList = new ArrayList<String>(tm.values());
      retval.put(srcID, trgList);
    }
    
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Do the second pass of gascluster prep
  */
  
  public void finishClusterPrep(SpecialtyLayoutEngine.NodePlaceSupport nps,
                                DataAccessContext irx, 
                                boolean baseAtTop, Map<String, Boolean> isOnTop, 
                                Map<String, Integer> srcToCol, int myColumn) {

    int numOrd = stackOrder_.size();    

    
    // Get the list of possible ssc-internal sources:
    Set<String> allMySources = getAllSources();        
    
    // Knows which direction links are coming from (top or bottom)
    ddo_ = new GeneAndSatelliteCluster.DropDirectionOracle(isOnTop, srcToCol, baseAtTop, stackOrder_, allMySources, myColumn);
 
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = stackOrder_.get(i);
      sc.prepPhaseTwo(nps, irx, ddo_);
    }
    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Handle inbound link routing for all the gasclusters.  THIS IS WHERE PAD CHANGES TO THE
  ** CORE ARE HAPPENING!!!!
  */
  
  public void routeInboundLinks(GenomeSubset subset, 
                                SpecialtyLayoutEngine.NodePlaceSupport nps,
                                DataAccessContext irx, 
                                Map<String, Integer> lengthChanges, 
                                Map<String, Integer> extraGrowthChanges,  
                                SpecialtyInstructions si, 
                                Map<String, SuperSrcRouterPointSource> allOutsPerSource, 
                                Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> deferredPerSource, 
                                boolean baseAtTop, 
                                Map<String, Boolean> isOnTop,
                                Map<String, Double> deferredSrcToTraceY,
                                int stackRow, 
                                Map<String, Integer> srcToStack) {

    //
    // For each element in stack order, get the inbound links
    // to it, and build off each inbound trace.
    
    ArrayList<GeneAndSatelliteCluster> useList = stackOrder_;
    ArrayList<String> soList = sourceOrderForPads_;
    if (!baseAtTop) {
      useList = new ArrayList<GeneAndSatelliteCluster>(stackOrder_);
      Collections.reverse(useList);
    } else {
      soList = new ArrayList<String>(sourceOrderForPads_);
      Collections.reverse(soList);            
    }
    ArrayList<GeneAndSatelliteCluster> antiSenseUseList = new ArrayList<GeneAndSatelliteCluster>(useList);
    Collections.reverse(antiSenseUseList);

    //
    // Pad changes
    //
    
    int numOrd = useList.size();
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = useList.get(i);
      sc.calcPadChanges(subset, soList, nps, lengthChanges, extraGrowthChanges);
    } 
    
    //
    // Collect up the sources, then iterate thru them in row order: 
    //
    
    HashSet<String> allSrcs = new HashSet<String>();
    for (int i = 0; i < numOrd; i++) {
      GeneAndSatelliteCluster sc = useList.get(i); 
      allSrcs.addAll(sc.getInputs());
    }
    
    // Get the list of possible ssc-internal sources:
    //Set allMySources = getAllSources();    

    Iterator<String> asit = allSrcs.iterator();    
    while (asit.hasNext()) {
      String srcID = asit.next();
      SuperSrcRouterPointSource outForSrc;
      if (srcToStack == null) {
        outForSrc = allOutsPerSource.get(srcID);      
      } else {
        Integer srcRow = srcToStack.get(srcID);
        if (srcRow.intValue() == stackRow) {
          outForSrc = allOutsPerSource.get(srcID);      
        } else {
          outForSrc = null;
        }      
      }     
 
      //
      // If we are dealing with e.g. feedback, we just plan the terminal routing of the link
      // instead of placing it.  These become deferred:
      
      Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> deferredTerminalPaths;
      SpecialtyLayoutLinkData sin;

      // This means the output has not yet been placed!
      if (isStacked_) {
        deferredTerminalPaths = deferredPerSource;
        sin = null;    
      } else {
        if (outForSrc == null) {
          deferredTerminalPaths = deferredPerSource;
          sin = null;                        
        } else {
          deferredTerminalPaths = null;
          sin = si.getOrStartLinkPointsForSrc(srcID);
        }
      }
      MetaClusterPointSource lps;
      if (isStacked_) {  
        GeneAndSatelliteCluster sc = useList.get(0);
        if (sc.linkIDsPerCluster(srcID, nps).isEmpty()) {
          continue;
        }
        lps = new MetaClusterPointSource(srcID, outForSrc, MetaClusterPointSource.FOR_ALWAYS_BELOW); 
        Double defTraceY = deferredSrcToTraceY.get(srcID);
        lps.setTargetTraceY(defTraceY.doubleValue());
        lps.setForDeferredPlacementOnly(true);
       // boolean useLeftDrops = sc.useLeftDropsForStacked(baseGenome);
        lps.setStackedParams(traceOffset_, base_, deferredTerminalPaths, inboundTraces_);
      } else {
        lps = new MetaClusterPointSource(srcID, outForSrc, MetaClusterPointSource.FOR_INBOUND_SRC);
        lps.setInboundParams(traceOffset_, inboundTraces_, base_, deferredTerminalPaths);
        lps.setForDeferredPlacementOnly(outForSrc == null);
      }     
      boolean linkFromTop = ddo_.comingFromTop(srcID, null);
      ArrayList<GeneAndSatelliteCluster> useForSource = (linkFromTop == baseAtTop) ? useList : antiSenseUseList;
     
      for (int i = 0; i < numOrd; i++) {
        GeneAndSatelliteCluster sc = useForSource.get(i);
        //
        // Only want to do inbound link routing from a src if there is an inbound link from it.
        // Maybe should hoist this loop for better efficiency...this is kinda a hack to put it here.
        //
        boolean doIt = false;
        Iterator<String> ilit = inboundLinks_.iterator();
        while (ilit.hasNext()) {
          Linkage link = nps.getLinkage(ilit.next());
          if (!link.getSource().equals(srcID)) {
            continue;
          }
          if (sc.linkIsInbound(link)) {
            doIt = true;
            break;
          }
        }
        if (doIt) {
          boolean topGASC = (stackOrder_.get(0) == sc);
          sc.inboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
        }
      }
    }     

    return;
  }
  
  /***************************************************************************
  ** 
  ** Finish building inbound deferred links
  */
  
  public void finishInboundDeferredLinks(SpecialtyLayoutEngine.NodePlaceSupport nps,
                                         DataAccessContext rcx,
                                         SpecialtyInstructions si, 
                                         Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> inboundsPerSrc,
                                         Map<String, SuperSrcRouterPointSource> allOutsPerSource,
                                         double traceDistance,
                                         boolean baseAtTop,
                                         LinkAnnotatedSuper las,
                                         int currStackedClustRow, 
                                         Set<String> onlySrcs, int flipOrder) {


    //
    // Now that the links for downstream sources have been started, we finish them
    // off
    //

    // A link coming back in for feedback should not be adding points
    // onto already laid out forward links:    
    
    HashSet<String> skipLinks = new HashSet<String>();
    skipLinks.addAll(interClusterLinks_);
    skipLinks.addAll(intraClusterLinks_);    
 
   
    Iterator<String> ipsit = inboundsPerSrc.keySet().iterator();    
    while (ipsit.hasNext()) {
      String srcID = ipsit.next();
      // Useful for stacked cluster series:
      if ((onlySrcs != null) && !onlySrcs.contains(srcID)) {
        continue;
      }
      //
      // For stacked clusters, we are always doing deferred inbounds.  Otherwise,
      // it is limited to more exclusive sets of sources:
      //
      if (!isStacked_) {
        if (!isASource(srcID, nps, las.backwardInputs) &&
            !isASource(srcID, nps, las.afterRowSources) &&
            !isASource(srcID, nps, las.beforeRowSources)) {  
          continue;
        }
      }
      SuperSrcRouterPointSource outForSrc = allOutsPerSource.get(srcID);
      boolean needMainDrop = true;
      Point2D lastDrop = new Point2D.Double();
      
      if (outForSrc.isForStackedCluster()) {
        outForSrc.setCurrStackRow(currStackedClustRow);
      }
      
      SpecialtyLayoutLinkData sin = si.getOrStartLinkPointsForSrc(srcID);
   
      // Note ordering, since the sin.startNewLink() order in the finishDeferred..() is crucial!
      List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>> inbounds = inboundsPerSrc.get(srcID);
      ArrayList<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>> useList = 
        new ArrayList<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>(inbounds);
      //
      // With stacked cases, we want to REVERSE the ordering unless we are in the
      // same row as the source, and to the left of it (since the deferred ordering
      // assumes right-to-left trace creation!
      //
      boolean sourceIsLeft = !las.backwardSources(nps).contains(srcID);
      if ((flipOrder == ALWAYS_FLIP_ORDER) || 
          ((flipOrder == FLIP_IF_SOURCE_IS_FROM_LEFT) && sourceIsLeft)) {
        Collections.reverse(useList);
      }
 
      Iterator<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>> ibsit = useList.iterator();
      while (ibsit.hasNext()) {
        GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners> perClust = ibsit.next();
        if (isStacked_) {
          outForSrc.finishDeferredStackedInbounds(srcID, perClust, nps, rcx,
                                                  skipLinks, sin, lastDrop);         
        } else {
          needMainDrop = outForSrc.finishDeferredInbounds(srcID, perClust, nps, rcx, skipLinks, sin,
                                                          inboundTraces_, traceDistance, base_,
                                                          needMainDrop, lastDrop);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Figure out required trace offsets
  */
  
  public InboundOffsets calcTraceOffsets(Map<String, Double> lastOutboundTraces) {

    //
    // Analyze the previous SSC to find the leftmost outbound trace and the rightmost
    // trace inbound to us:
    //
    
    Set<String> inboundSrcs = inboundTraces_.keySet();
    InboundOffsets retval = new InboundOffsets();
    retval.lowestPrevious = null;
    retval.lowestPreviousX = null;    
    retval.highestPrevious = null;
    retval.highestPreviousX = null;        
    retval.highestInbound = null;
    retval.highestInboundX = null;
    
    if (lastOutboundTraces != null) {
      Iterator<String> lobit = lastOutboundTraces.keySet().iterator();
      while (lobit.hasNext()) {
        String traceSrc = lobit.next();
        Double xForTrace = lastOutboundTraces.get(traceSrc);
        if ((retval.lowestPreviousX == null) || (retval.lowestPreviousX.doubleValue() > xForTrace.doubleValue())) {
          retval.lowestPreviousX = xForTrace;
          retval.lowestPrevious = traceSrc;
        }
        if ((retval.highestPreviousX == null) || (retval.highestPreviousX.doubleValue() < xForTrace.doubleValue())) {
          retval.highestPreviousX = xForTrace;
          retval.highestPrevious = traceSrc;
        }        
        if (inboundSrcs.contains(traceSrc)) {
          if ((retval.highestInboundX == null) || (retval.highestInboundX.doubleValue() < xForTrace.doubleValue())) {
            retval.highestInboundX = xForTrace;
            retval.highestInbound = traceSrc;
          }
        }
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Figure out the minimum X we would need to accomodate the given inbound trace profile,
  ** as well as the left shift. Returns null if no requirements
  */
  
  private MinXAndLeftShift minXAndShiftNeeded(double traceDistance, Map<String, Double> lastOutboundTraces, 
                                              Map<String, Boolean> isOnTop, boolean baseAtTop, 
                                              LinkAnnotatedSuper las) {
     
    Set<String> inboundSrcs = inboundTraces_.keySet();
    InboundOffsets inOffs = calcTraceOffsets(lastOutboundTraces);

    //
    // Find out the rightmost of all our traces.  This will represent the needed shift
    // from the rightmost outbound trace of the previous SSC.
    //
    
    Iterator<Integer> itit = inboundTraces_.values().iterator();
    Integer highest = null;
    Integer lowest = null;    
    while (itit.hasNext()) {
      Integer traceNum = itit.next();
      if ((highest == null) || (highest.intValue() < traceNum.intValue())) {
        highest = traceNum;
      }
      if ((lowest == null) || (lowest.intValue() > traceNum.intValue())) {
        lowest = traceNum;
      }      
    }
    
    MinXAndLeftShift retval = new MinXAndLeftShift();    
    double inboundCol = (highest != null) ? highest.doubleValue() : 0.0;
    retval.leftShift = (isStacked_) ? 0.0 : inboundCol * traceDistance;
    
    //
    // Figure out the cross-centerline feedback count:
    //
    MinMax allFeedGap = (new MinMax()).init();
    int allFeeds = 0;
    int feedbacksFromBottom = 0;
    if (!isStacked_) {      
      int feedbacksFromTop = 0; 
      Iterator<String> isit = inboundSrcs.iterator();
      while (isit.hasNext()) {
        String srcID = isit.next();
        boolean isFeedback = las.backwardSources.contains(srcID);
        if (isFeedback) {
          boolean srcOnTop = isOnTop.get(srcID).booleanValue();
          if (srcOnTop) {
            feedbacksFromTop++;
          } else {
            feedbacksFromBottom++;
          }
          Integer its = inboundTraces_.get(srcID);
          allFeedGap.update(its.intValue());
        }
      }
      allFeeds = feedbacksFromTop + feedbacksFromBottom;
    }
      
    //
    // First case.  We have a trace from the immediately preceeding SSC.  Our base is set to
    // make that the rightmost inbound trace.
    //

    if (inOffs.highestInbound != null) {
      Integer highestInboundTrace = (Integer)inboundTraces_.get(inOffs.highestInbound); 
      double baseDelta = traceDistance * highestInboundTrace.doubleValue();
      retval.baseX = inOffs.highestInboundX.doubleValue() - baseDelta;
    //
    // No immediate inbound trace.  But if we have feedbacks coming in across the centerline,
    // make sure that the leftmost feedback is one track to the right of the rightmost previous
    // trace.
    } else if ((inOffs.highestPrevious != null) && (allFeeds > 0)) { 
      double feedShift;
      if (baseAtTop) {
        feedShift = (double)(allFeedGap.max - allFeedGap.min + 1) * traceDistance;
      } else {
        feedShift = (double)feedbacksFromBottom * traceDistance;
      }
      retval.baseX = inOffs.highestPreviousX.doubleValue() - retval.leftShift + feedShift;
    //
    // No immediate inbound trace.  Our base is set to make the rightmost inbound trace
    // drop just before the leftmost previous trace (reduces visual confusion).
    //      
    } else if ((inOffs.lowestPrevious != null) && !inboundTraces_.isEmpty()) {     
      retval.baseX = inOffs.lowestPreviousX.doubleValue() - retval.leftShift - traceDistance;
    } else {
      return (null); 
    }

    retval.minX = retval.baseX + (traceDistance * lowest.doubleValue());
    return (retval);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static class InboundOffsets {
    public String lowestPrevious;
    public Double lowestPreviousX; 
    public String highestPrevious;
    public Double highestPreviousX;     
    public String highestInbound;
    public Double highestInboundX;
  }
  
  public static class MinXAndLeftShift {
    public double minX;
    public double baseX;    
    public double leftShift;    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Locate the super cluster left edge for stacked case.  Sets base_ in the process.
  ** Remember, the base is the "zero point" for the GASC placement, the returned value handles
  ** wiggles for space for the the inbound traces.
  */
  
  private double locateStackedLeftEdge(Point2D basePos, double traceDistance) {
     base_ = (Point2D)basePos.clone();
     if (stackedDirectOffset_ != 0) {
       base_.setLocation(base_.getX() + (2.0 * traceDistance), base_.getY()); // tweak...
     }
     return (base_.getX() + (stackedDirectOffset_  * traceDistance) + RIGHT_X_PAD_PER_STACKED_COL_);  
  } 
  
  /***************************************************************************
  ** 
  ** Locate the super cluster left edge.  Sets base_ in the process
  */
  
  private double locateLeftEdge(Point2D basePos, double traceDistance, Map<String, Double> lastOutboundTraces, 
                                Map<String, Boolean> isOnTop, boolean baseAtTop, LinkAnnotatedSuper las) {
    //
    // Need to find my X.  Take the rightmost track of the previous cluster
    // that is inbound to me.  Find its order amongst my inbounds.  Base
    // is displaced back to the left by that amount.  If this case is not
    // met, we need to make sure our rightmost inbound trace is dropping down
    // one track to the left of the leftmost previous trace.
    // PLUS the leftmost feedback coming in across the centerline is one track
    // to the right of the rightmost trace of the previous cluster.
     
    MinXAndLeftShift mxs = minXAndShiftNeeded(traceDistance, lastOutboundTraces, isOnTop, baseAtTop, las);
    double leftShift = 0.0;

    if (mxs != null) {
      base_ = new Point2D.Double(mxs.baseX, basePos.getY());
      leftShift = mxs.leftShift;
    } else {
      base_ = (Point2D)basePos.clone();
    }
    
    if (UiUtil.forceToGridValue(base_.getX(), UiUtil.GRID_SIZE) != base_.getX()) {
      throw new IllegalArgumentException();
    }
    
    
    return (base_.getX() + leftShift + ((isStacked_) ? RIGHT_X_PAD_PER_STACKED_COL_ : RIGHT_X_PAD_PER_COL_));            
  }  
} 
