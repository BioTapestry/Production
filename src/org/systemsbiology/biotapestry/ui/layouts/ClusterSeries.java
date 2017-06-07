/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Set;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Represents a series of SuperSourceClusters
*/

public class ClusterSeries {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final double MIN_CLUSTER_SEPARATION_ = 100.0;  
  private static final double FEED_PAD_ = 80.0;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Point2D basePos_;
  private HashSet<String> preSources_;
  private HashSet<String> postTargets_;
  private TreeMap<Integer, LinkAnnotatedSuper> superClusters_;
  private HashMap<String, Integer> srcToTrack_;
  private TreeMap<Integer, String> trackToSrc_;  
  private HashMap<Integer, Integer> baseForCluster_;
  private HashMap<Integer, Double> trackToY_;
  private HashMap<String, Boolean> isOnTop_;
  private HashMap<String, Integer> srcToCol_;
  private HashMap<String, Double> srcToFeedbackY_;
  private HashMap<String, Double> deferredStackSrcToY_;
  private TreeMap<Integer, String> topFeedOrder_;  
  private TreeMap<Integer, String> botFeedOrder_;  
  private double noTracksMinY_;
  private boolean isStacked_;
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
        
  public ClusterSeries(boolean isStacked, int traceMult) {
    superClusters_ = new TreeMap<Integer, LinkAnnotatedSuper>(); 
    srcToTrack_ = new HashMap<String, Integer>();
    trackToSrc_ = new TreeMap<Integer, String>();
    trackToY_ = new HashMap<Integer, Double>();    
    baseForCluster_ = new HashMap<Integer, Integer>();
    topFeedOrder_ = new TreeMap<Integer, String>();   
    botFeedOrder_ = new TreeMap<Integer, String>();       
    srcToFeedbackY_ = new HashMap<String, Double>();
    deferredStackSrcToY_ = new HashMap<String, Double>();
    isOnTop_ = new HashMap<String, Boolean>();
    srcToCol_ = new HashMap<String, Integer>();
    isStacked_ = isStacked;
    preSources_ = new HashSet<String>();
    postTargets_ = new HashSet<String>();
    traceOffset_ = UiUtil.GRID_SIZE * traceMult;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the trace offset
  */

  public double getTraceOffset() {
    return (traceOffset_);
  }  
   
  /***************************************************************************
  ** 
  ** Add pre and post nodes.
  */

  public void addPreAndPost(GenomeSubset subset) {
    if (isStacked_) {
      throw new IllegalStateException();
    }   
    preSources_.addAll(subset.externalSourceOnly);
    preSources_.addAll(subset.externalBothSrcTarget);
    postTargets_.addAll(subset.externalTargetOnly);
    postTargets_.addAll(subset.externalBothSrcTarget);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Get everybody we care about for in and out traces
  */
  
  public Set<String> justSourcesOfInterest() {
    
    HashSet<String> retval = new HashSet<String>();
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      retval.addAll(las.ssc.getAllSources());
      retval.addAll(las.ssc.getAllInputs());
    }      
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Add another cluster.
  */

  public void addSourceCluster(int index, GeneAndSatelliteCluster newClust) {
    Integer key = new Integer(index);
    LinkAnnotatedSuper las = superClusters_.get(key);
    if (las == null) {
      SuperSourceCluster ssc = new SuperSourceCluster(isStacked_, traceOffset_);
      las = new LinkAnnotatedSuper(ssc);
      superClusters_.put(key, las);
    }
    las.ssc.addSourceCluster(newClust);
    return;
  }  

  /***************************************************************************
  ** 
  ** Used to build link trace framework for stacked series:
  */

  public Map<String, Double> getDeferredSrcToTraceY() {
    return ((isStacked_) ? deferredStackSrcToY_ : srcToFeedbackY_);
  } 
  
  /***************************************************************************
  ** 
  ** Prep the series.
  */

  public void prepForStackPass1(SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx, 
                                Map<String, Point> targets, Map<String, Point> sources, Map<Point, Integer> order, int myRow) {
    
    //
    // For each super cluster, we need to figure out the distant inputs, the
    // immediate inputs, the distant outputs, and the immediate outputs.  We also
    // need to figure out the feedback outputs and feedback inputs.  First pass
    // is to just assign inputs and outputs; we refine these in the next pass
    //
    
    annotateLASForStack(nps, targets, sources, order, myRow);
    
    //
    // Figure out which side of the centerline each source lies on (Build the map: srcID->[top or bottom])
    //
    
    assignCenterlineLocalOnly();
      
    //
    // With the above mappings in hand, we now have enough information to
    // finish building our GASClusters:
    //
    
    finishClusterPrep(nps, rcx);    
       
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Prep the series.
  */

  @SuppressWarnings("unused")
  public void prepForStackPass2(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                Map<String, Point> targets, Map<String, Point> sources, 
                                Map<Point, Integer> order, int myRow, GlobalTrackAssignment gta) {
    
    //
    // Need to assign track numbers
    //
       
    srcToTrack_ = new HashMap<String, Integer>(); //gta.globalSrcToTrack);
    trackToSrc_ = new TreeMap<Integer, String>(); //gta.globalTrackToSrc); 
    
    //
    // For stacked cases, we are only running traces for the
    // sources that MUST be there, not everybody!
    //
    
    TreeMap<Integer, String> minimizedTracks = new TreeMap<Integer, String>();
    Set<String> soi = justSourcesOfInterest();
    Iterator<String> sit = soi.iterator();
    while (sit.hasNext()) {
      String src = sit.next();
      Integer track = gta.globalSrcToTrack.get(src);
      minimizedTracks.put(track, src);
    }
       
    int count = 0;
    Iterator<String> mtrit = minimizedTracks.values().iterator();
    while (mtrit.hasNext()) {
      String src = mtrit.next();
      Integer trackNum = new Integer(count++);
      srcToTrack_.put(src, trackNum);
      trackToSrc_.put(trackNum, src);
    }
    
    int currMaxTrack = (trackToSrc_.isEmpty()) ? 0 : trackToSrc_.lastKey().intValue();
    
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      baseForCluster_.put(clustNum, new Integer(currMaxTrack));     
    }      
 
    prepCommon(nps, currMaxTrack);
    return;
  } 
 
  /***************************************************************************
  ** 
  ** Prep the series for a single row
  */

  public void prepForSingle(GenomeSubset subset, SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx) {
        
    HashMap<String, Point> targets = new HashMap<String, Point>();
    HashMap<String, Point> sources = new HashMap<String, Point>();
    HashMap<Point, Integer> order = new HashMap<Point, Integer>();       
    preprocessForAnnotate(subset, targets, sources, order, 0, nps);
 
    //
    // For each super cluster, we need to figure out the distant inputs, the
    // immediate inputs, the distant outputs, and the immediate outputs.  We also
    // need to figure out the feedback outputs and feedback inputs.  First pass
    // is to just assign inputs and outputs; we refine these in the next pass
    //
    
    annotateLASForStack(nps, targets, sources, order, 0);
    
    //
    // Figure out which side of the centerline each source lies on (Build the map: srcID->[top or bottom])
    //
    
    assignCenterlineLocalOnly();
      
    //
    // With the above mappings in hand, we now have enough information to
    // finish building our GASClusters:
    

    finishClusterPrep(nps, rcx);    
       
    //
    // Need to assign track numbers
    //
    
    int currMaxTrack = assignTrackNumbersLocalOnly(nps);
     
    prepCommon(nps, currMaxTrack);
    return;
  }
  
  
  /***************************************************************************
  ** 
  ** Prep the series.  Common code for single and stacked Cluster Series
  */

  private void prepCommon(SpecialtyLayoutEngine.NodePlaceSupport nps, int currMaxTrack) {
    
  
    //
    // Revise to constant height for always below:
    // FIXME: this is redundant for multi-stack cases, but not for single always-below case
       
    if (isStacked_) {
      Iterator<Integer> bit = baseForCluster_.keySet().iterator();
      Integer cmt = new Integer(currMaxTrack);
      while (bit.hasNext()) {
        Integer clustNum = bit.next();
        baseForCluster_.put(clustNum, cmt);   
      }
    }

    //
    // Assign tracks to feedback traces.
    //
    
    boolean addBottom = (isStacked_) ? true : false;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);      
      las.ssc.orderFeedbackTraces(nps, addBottom, topFeedOrder_, botFeedOrder_, las);
      if (!isStacked_) {
        addBottom = !addBottom;
      }
    }        
    
    //
    // Let the super clusters figure out the order of their inbound links:
    //

    addBottom = (isStacked_) ? true : false;
    LinkAnnotatedSuper lastLas = null;
    scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.ssc.orderInboundTraces(nps, addBottom, las, topFeedOrder_, botFeedOrder_, 
                                 srcToTrack_, lastLas);
      
      // We DO NOT want to mess with inbound trace ordering based on previous ordering
      // in the stacked case
      if (!isStacked_) {
        lastLas = las;
        addBottom = !addBottom;
      }
    }
    
    return;
  } 
 
  /***************************************************************************
  ** 
  ** Need to assign centerlines
  */

  private void assignCenterlineLocalOnly() {  
    
    //
    // Figure out which side of the centerline each source lies on (Build the map: srcID->[top or bottom])
    //
 
    boolean onBottom = (isStacked_) ? true : false;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer currClustNum = scit.next();
      LinkAnnotatedSuper currLas = superClusters_.get(currClustNum);
      Set<String> allSources = currLas.ssc.getAllSources();
      Iterator<String> asit = allSources.iterator();
      Boolean onTopObj = new Boolean(!onBottom);
      while (asit.hasNext()) {
        String srcID = asit.next();
        isOnTop_.put(srcID, onTopObj);
        srcToCol_.put(srcID, currClustNum);
      }
      if (!isStacked_) {
        onBottom = !onBottom;
      }
    }
    return;
  }
    
      
  /***************************************************************************
  ** 
  ** Need to assign track numbers
  */

  private int assignTrackNumbersLocalOnly(SpecialtyLayoutEngine.NodePlaceSupport nps) {  
    GlobalTrackAssignment gta = new GlobalTrackAssignment();  
    srcToTrack_ = gta.globalSrcToTrack;
    trackToSrc_ = gta.globalTrackToSrc;    
    return (buildGlobalTrackNumbers(nps, false, true, gta));
  }
  
  /***************************************************************************
  ** 
  ** Need to assign track numbers
  */

  private int buildGlobalTrackNumbers(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean reverse, boolean trackBase, GlobalTrackAssignment gta) {
   
    boolean addBottom = (isStacked_) ? true : false;
  
    //
    // If we have sources out of the superset, they get added here:
    // BUT ONLY USE THIS IF NOT STACKED
    //
    
    if (!isStacked_) {
      int externalTrack = 0;
      Iterator<String> oit = preSources_.iterator();
      while (oit.hasNext()) {
        String srcID = oit.next();
        Integer track = new Integer(externalTrack++);
        gta.globalSrcToTrack.put(srcID, track);
        gta.globalTrackToSrc.put(track, srcID);
        gta.currMaxTrack++;
      }
    }
 
    //
    // Go backwards if asked
    //
    TreeSet<Integer> revSC = (!reverse) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
    revSC.addAll(superClusters_.keySet());
    
    Iterator<Integer> scit = revSC.iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      if (trackBase) { // only done for local cases
        int base = (addBottom) ? gta.currMaxTrack : gta.currMinTrack;
        baseForCluster_.put(clustNum, new Integer(base));
      }
      
      assignForSet(las.immediateOutputs, nps, las, addBottom, 
                   gta.currMaxTrack, gta.currMinTrack, gta.globalSrcToTrack, gta.globalTrackToSrc);
      assignForSet(las.forwardOutputs, nps, las, addBottom, 
                   gta.currMaxTrack, gta.currMinTrack, gta.globalSrcToTrack, gta.globalTrackToSrc);
      assignForSet(las.afterRowTargets, nps, las, addBottom, 
                   gta.currMaxTrack, gta.currMinTrack, gta.globalSrcToTrack, gta.globalTrackToSrc);
      
      int orderMax = las.ssc.getOutboundLinkOrderMax();
      if (isStacked_) {
        assignBackwardsForSetToMain(las.backwardOutputs, nps, las, addBottom, 
                                    gta.currMaxTrack, gta.currMinTrack, orderMax, gta.globalSrcToTrack, gta.globalTrackToSrc);
        assignBackwardsForSetToMain(las.beforeRowTargets, nps, las, addBottom, 
                                    gta.currMaxTrack, gta.currMinTrack, orderMax, gta.globalSrcToTrack, gta.globalTrackToSrc);
        assignBackwardsForSetToMain(las.afterRowTargets, nps, las, addBottom, 
                                    gta.currMaxTrack, gta.currMinTrack, orderMax, gta.globalSrcToTrack, gta.globalTrackToSrc);
      }
    
      if (addBottom) {
        gta.currMaxTrack += orderMax;
        addBottom = (isStacked_) ? true : false;
      } else {
        gta.currMinTrack -= orderMax;
        addBottom = true;        
      }
    }
    return (gta.currMaxTrack);
  }

  /***************************************************************************
  ** 
  ** Need to assign track numbers
  */

  public int calcGlobalTrackNumbers(SpecialtyLayoutEngine.NodePlaceSupport nps, GlobalTrackAssignment gta) {
    return (buildGlobalTrackNumbers(nps, true, false, gta));
  }
 
  /***************************************************************************
  ** 
  ** Assign backwards tracks to the main trace
  */

  private void assignBackwardsForSetToMain(Set<String> linkSet, SpecialtyLayoutEngine.NodePlaceSupport nps, LinkAnnotatedSuper las, 
                                           boolean addBottom, int currMaxTrack, int currMinTrack, 
                                           int orderMax, Map<String, Integer> srcToTrack, 
                                           Map<Integer, String> trackToSrc) {
   
    Iterator<String> oit = linkSet.iterator();
    while (oit.hasNext()) {
      String linkID = oit.next();
      Linkage link = nps.getLinkage(linkID);
      String srcID = link.getSource();
      // If it is not just for backwards, the trace has already been assigned!
      if (!las.pureBackwardOutputSources.contains(srcID)) {
        continue;
      }
      int outOrder = las.ssc.getOutboundLinkOrder(link);
      // With stacked, we want nice 180 degree reverse turns more than anything!
      if (isStacked_) {
        outOrder = orderMax - outOrder;
      }
      int newTrack = (addBottom) ? currMaxTrack + outOrder : currMinTrack - orderMax + outOrder;
      Integer track = new Integer(newTrack);
      srcToTrack.put(srcID, track);
      trackToSrc.put(track, srcID);
    }
    return;   
  }
  
  /***************************************************************************
  ** 
  ** Assign tracks for set
  */

  private void assignForSet(Set<String> linkSet, SpecialtyLayoutEngine.NodePlaceSupport nps, LinkAnnotatedSuper las, 
                            boolean addBottom, int currMaxTrack, int currMinTrack, 
                            Map<String, Integer> srcToTrack, Map<Integer, String> trackToSrc) {
    int orderMax = las.ssc.getOutboundLinkOrderMax();
    Iterator<String> oit = linkSet.iterator();
    while (oit.hasNext()) {
      String linkID = oit.next();
      Linkage link = nps.getLinkage(linkID);
      String srcID = link.getSource();
      int outOrder = las.ssc.getOutboundLinkOrder(link);
      // With stacked, we want nice 180 degree reverse turns more than anything!
      if (isStacked_) {
        outOrder = orderMax - outOrder;
      }
      int newTrack = (addBottom) ? currMaxTrack + outOrder : currMinTrack - orderMax + outOrder;
      Integer track = new Integer(newTrack);
      srcToTrack.put(srcID, track);
      trackToSrc.put(track, srcID);        
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Prep the series.
  */

  public void preprocessForAnnotate(GenomeSubset subset, Map<String, Point> targets, 
                                    Map<String, Point> sources, 
                                    Map<Point, Integer> order, int myRow, 
                                    SpecialtyLayoutEngine.NodePlaceSupport nps) {
     
    Genome baseGenome = subset.getBaseGenome();
    
    int count = 0;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.ssc.prep(subset, nps);
      order.put(new Point(clustNum.intValue(), myRow), new Integer(count++));
    }
       
    //
    // For each super cluster, we need to figure out the distant inputs, the
    // immediate inputs, the distant outputs, and the immediate outputs.  We also
    // need to figure out the feedback outputs and feedback inputs.  First pass
    // is to just assign inputs and outputs; we refine these in the next pass
    //
    scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      Set<String> linksForSsc = las.ssc.getAssociatedLinks(nps);
        
      Iterator<String> lit = linksForSsc.iterator();
      while (lit.hasNext()) {
        String linkID = lit.next();
        Linkage link = baseGenome.getLinkage(linkID);    
        if (las.ssc.linkIsInternal(link)) {
          continue;
        }
        int myNum = clustNum.intValue();
        if (las.ssc.linkIsInbound(link)) {
          las.allInputs.add(linkID);          
          targets.put(linkID, new Point(myNum, myRow));
        }
        if (las.ssc.linkIsOutbound(link)) {
          las.allOutputs.add(linkID);
          sources.put(linkID, new Point(myNum, myRow));
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Prep the series.
  */

  public void annotateLASForStack(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, Point> targets, Map<String, Point> sources, Map<Point, Integer> order, int myRow) {
         
    //
    // For each super cluster, we need to figure out the distant inputs, the
    // immediate inputs, the distant outputs, and the immediate outputs.  We also
    // need to figure out the feedback outputs and feedback inputs.  First pass
    // is to just assign inputs and outputs; we refine these in the next pass
    //
    
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      Point clustPt = new Point(clustNum.intValue(), myRow);
      int currClust = order.get(clustPt).intValue();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.srcRow = myRow;
      las.srcCol = currClust;
      Set<String> allInputs = las.allInputs;
      Set<String> allOutputs = las.allOutputs;
      // Don't need long-term anymore, just toss:
      las.allInputs = null;
      las.allOutputs = null;
      Iterator<String> aiit = allInputs.iterator();
      while (aiit.hasNext()) {
        String linkID = aiit.next();
        Point ptObj = sources.get(linkID);      
        // This happens with genome subsets and sources outside the current subset
        if (ptObj == null) {
          las.beforeRowSources.add(linkID);
          continue;
        }    
        int srcNum = order.get(ptObj).intValue();
        if (ptObj.y < myRow) {
          las.beforeRowSources.add(linkID);
        } else if (ptObj.y > myRow) {
          las.afterRowSources.add(linkID);
        } else if (srcNum == (currClust - 1)) {
          las.immediateInputs.add(linkID);   
        } else if (srcNum < currClust) {
          las.pastInputs.add(linkID);
        } else if (srcNum >= currClust) {
          las.backwardInputs.add(linkID);
          Linkage link = nps.getLinkage(linkID);
          las.backwardSources.add(link.getSource());
        } else {
          throw new IllegalStateException();
        }  
      }
      
      //
      // Start with all output sources, will end up with just those
      // completely devoted to feedback.
      //
      Iterator<String> aoit = allOutputs.iterator();
      while (aoit.hasNext()) {
        String linkID = aoit.next();
        Linkage link = nps.getLinkage(linkID);
        las.pureBackwardOutputSources.add(link.getSource());
      }
      
      aoit = allOutputs.iterator();
      while (aoit.hasNext()) {
        String linkID = aoit.next();
        Linkage link = nps.getLinkage(linkID);
        Point ptObj = targets.get(linkID);       
        // This happens with genome subsets and targets outside the current subset
        if (ptObj == null) {
          las.afterRowTargets.add(linkID);
          continue;
        }
        int trgNum = order.get(ptObj).intValue();
        if (ptObj.y < myRow) {
          las.beforeRowTargets.add(linkID);
        } else if (ptObj.y > myRow) {
          las.afterRowTargets.add(linkID);
        } else if (trgNum == (currClust + 1)) {
          las.immediateOutputs.add(linkID);
          las.pureBackwardOutputSources.remove(link.getSource());
        } else if (trgNum > currClust) {
          las.forwardOutputs.add(linkID);
          las.pureBackwardOutputSources.remove(link.getSource());
        } else if (trgNum <= currClust) {
          las.backwardOutputs.add(linkID);
        } else {
          throw new IllegalStateException();
        }      
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the maxiumum height of the series:
  */
  
  public double getMaximumHeight(SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx) {
  
    double maxHeight = Double.NEGATIVE_INFINITY;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer nextClustNum = scit.next();
      LinkAnnotatedSuper currLas = superClusters_.get(nextClustNum);
      double height = currLas.ssc.getHeight(nps, rcx, traceOffset_);
      if (height > maxHeight) {
        maxHeight = height;
      }
    }
    return (maxHeight);
  }
  
  
  /***************************************************************************
  ** 
  ** Get the bounds of JUST the nodes:
  */
  
  public Rectangle getNodeOnlyBounds(SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx) {
    Rectangle retval = null;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer nextClustNum = scit.next();
      LinkAnnotatedSuper currLas = superClusters_.get(nextClustNum);
      Rectangle nsBounds = currLas.ssc.getNodeOnlyBounds(nps, rcx);
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
  ** Get location of the cluster series
  */
  
  public Point2D getBasePos() {
    return (basePos_); 
  }
  
  /***************************************************************************
  ** 
  ** Locate the cluster series
  */
  
  public Point2D locate(Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx, Double matchMax) {
   
    //
    // Lay them out:
    //
 
    basePos_ = (Point2D)basePos.clone();
    double baseX = basePos.getX();
    double baseY = basePos.getY();    
    boolean addBottom = (isStacked_) ? true : false;
    double currX = baseX;
    double minYVal = baseY;
    double maxYVal = baseY;
    
    Map<String, Double> lastOutboundTraces = null;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    Integer currClustNum = (scit.hasNext()) ? scit.next() : null;
    while (currClustNum != null) {
      Integer nextClustNum = (scit.hasNext()) ? scit.next() : null;
      LinkAnnotatedSuper currLas = superClusters_.get(currClustNum);
      LinkAnnotatedSuper nextLas = 
        (nextClustNum != null) ? superClusters_.get(nextClustNum) : null;      
      Integer baseObj = baseForCluster_.get(currClustNum);
      int baseVal = baseObj.intValue();
      double sign = (baseVal < 0) ? -1.0 : 1.0;
      Point2D pos;
      LinkAnnotatedSuper useForNext;
      double useForMin;
      if (isStacked_) { 
        pos = new Point2D.Double(currX, baseY);
        useForNext = null;
        useForMin = 0.0;
      } else {
        // This handles the center bus getting bigger as we go right:
        pos = new Point2D.Double(currX, baseY + (baseVal * traceOffset_) + (sign * 100.0));
        useForNext = nextLas;
        useForMin = MIN_CLUSTER_SEPARATION_;
      }
      Point2D right = currLas.ssc.locate(pos, nps, rcx, addBottom, 
                                         isOnTop_, traceOffset_, currLas, lastOutboundTraces, 
                                         useForNext, useForMin, matchMax);
      if (addBottom) {
        if (right.getY() > maxYVal) {
          maxYVal = right.getY();
        }
      } else {
        if (right.getY() < minYVal) {
          minYVal = right.getY();
        }        
      }
      
      currX = right.getX();
      // Stacked guys do not care about last outbound traced:
      if (!isStacked_) {
        addBottom = !addBottom;
        lastOutboundTraces = currLas.ssc.getOutboundTraces();
      }     
      currClustNum = nextClustNum;
    }
    
    //
    // With stacked series, we calc this before this step:
    //
    
    if (!isStacked_) {
      setTrackPlacementCore(baseY, minYVal, maxYVal, FEED_PAD_);
    }
    
    return (new Point2D.Double(currX, baseY));
  }
  
  /***************************************************************************
  ** 
  ** Track Placement
  */
  
  public double setTrackPlacementForStacked(double baseY) {
    
    if (!isStacked_) {
      throw new IllegalStateException();
    }
    
    //
    // 11/28/12: V6 rollout imminent! But note that for fully compressed stack layouts,
    // we get the traceOffset_ (e.g. 10) right for squashed rows, but the absolute placement
    // depends on the baseY coming in to reflect the fully squashed row height (bad news!).
    // 
    
    double maxTrackY = Double.NEGATIVE_INFINITY;
    Iterator<Integer> trit = trackToSrc_.keySet().iterator();
    while (trit.hasNext()) {
      Integer trackNum = trit.next();
      int trackNumVal = trackNum.intValue();
      double trackY = baseY + (trackNumVal * traceOffset_);
      trackToY_.put(trackNum, new Double(trackY));
      if (trackY > maxTrackY) {
        maxTrackY = trackY;
      }
    }
   
    if (trackToY_.isEmpty()) {
      noTracksMinY_ = baseY;
    }
    
    //
    // transfer the feedbacks into the full stack:
    //    
 
    Iterator<Integer> bfkit = botFeedOrder_.keySet().iterator();
    while (bfkit.hasNext()) {
      Integer pos = bfkit.next();
      String srcID = botFeedOrder_.get(pos);
      Integer trackNum = srcToTrack_.get(srcID);
      Double trackY = trackToY_.get(trackNum);
      deferredStackSrcToY_.put(srcID, trackY);
    }
    
    //
    // In cases where we are stacked, we need to have traces for EVERY source we care about!
    //
    
    Iterator<String> stit = srcToTrack_.keySet().iterator();
    while (stit.hasNext()) {
      String srcID = stit.next();
      Integer trackNum = srcToTrack_.get(srcID);
      Double trackY = trackToY_.get(trackNum);
      deferredStackSrcToY_.put(srcID, trackY);
    }
  
    return ((maxTrackY == Double.NEGATIVE_INFINITY) ? baseY : maxTrackY);
    
  }  
  
  /***************************************************************************
  ** 
  ** Track Placement
  */
  
  private double setTrackPlacementCore(double baseY, double minYVal, double maxYVal, double pad) {
      
    if (isStacked_) {
      throw new IllegalStateException();
    }
    
    //
    // Figure out the track placement:
    //
   
    double maxTrackY = Double.NEGATIVE_INFINITY;
    Iterator<Integer> trit = trackToSrc_.keySet().iterator();
    while (trit.hasNext()) {
      Integer trackNum = trit.next();
      int trackNumVal = trackNum.intValue();
      double trackY = baseY + (trackNumVal * traceOffset_);
      trackToY_.put(trackNum, new Double(trackY));
      if (trackY > maxTrackY) {
        maxTrackY = trackY;
      }
    }
    
    if (trackToY_.isEmpty()) {
      noTracksMinY_ = minYVal;
    }
    
    //
    // Figure out the feedback placement:
    //    
    
    double veryTop = minYVal - pad;
    double veryBottom = maxYVal + pad;
      
    Iterator<Integer> tfkit = topFeedOrder_.keySet().iterator();
    while (tfkit.hasNext()) {
      Integer pos = tfkit.next();
      String srcID = topFeedOrder_.get(pos);    
      double feedY = veryTop + (pos.doubleValue() * traceOffset_); 
      srcToFeedbackY_.put(srcID, new Double(feedY));
    }
   
      
    Iterator<Integer> bfkit = botFeedOrder_.keySet().iterator();
    while (bfkit.hasNext()) {
      Integer pos = bfkit.next();
      String srcID = botFeedOrder_.get(pos);  
      double feedY = veryBottom + (pos.doubleValue() * traceOffset_);
      srcToFeedbackY_.put(srcID, new Double(feedY));
    }
    
    return (maxTrackY);
  }
 
  /***************************************************************************
  ** 
  ** Finish cluster preparations
  */
  
  public void finishClusterPrep(SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx) {

    boolean baseAtTop = (isStacked_) ? true : false;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.ssc.finishClusterPrep(nps, rcx, baseAtTop, isOnTop_, srcToCol_, clustNum.intValue()); 
      if (scit.hasNext()) {
        if (!isStacked_) {
           baseAtTop = !baseAtTop;
        }      
      }
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Handle link routing.
  */
  
  public void routeLinks(GenomeSubset subset, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                         StaticDataAccessContext rcx, 
                         Map<String, Integer> lengthChanges,
                         Map<String, Integer> extraGrowthChanges, SpecialtyInstructions si, 
                         Map<String, SuperSrcRouterPointSource> traceDefs) {    

    if (isStacked_) {
      throw new IllegalStateException();
    }
      
    //
    // First pass:
    //
    
    Iterator<String> asit = subset.getSourcesHeadedIn().iterator();
    while (asit.hasNext()) {
      String srcID =asit.next();
      SuperSrcRouterPointSource corners = new SuperSrcRouterPointSource(srcID, false, false);
      traceDefs.put(srcID, corners);
      Integer tracknum = srcToTrack_.get(srcID);
      Double trackVal = new Double(basePos_.getX() - (tracknum.doubleValue() * traceOffset_));
      Double trackY = trackToY_.get(tracknum); 
      corners.initForExternal(trackVal, trackY, null, 0);
    }
  
    boolean addBottom = false;
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.deferredTerminalPaths = new HashMap<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>>();
 
      las.ssc.routeInboundLinks(subset, nps, rcx, lengthChanges, extraGrowthChanges,
                                si, traceDefs, las.deferredTerminalPaths, addBottom, isOnTop_, 
                                getDeferredSrcToTraceY(), 0, null);
      // This does INTERNAL LINKS in the GASCs as well!
      las.ssc.startOutboundLinks(nps, rcx, si, trackToY_, srcToTrack_, 
                                 getDeferredSrcToTraceY(), addBottom, traceDefs, las, false);
      if (scit.hasNext()) {
        addBottom = !addBottom;
      }
    }
    
    //
    // Go back through and finish placing feedbacks.  Move backwards!
    //
    
    ArrayList<Integer> revList = new ArrayList<Integer>(superClusters_.keySet());
    Collections.reverse(revList);
    scit = revList.iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.ssc.finishInboundDeferredLinks(nps, rcx, si, 
                                         las.deferredTerminalPaths, traceDefs, traceOffset_, 
                                         addBottom, las, 0, null, SuperSourceCluster.NEVER_FLIP_ORDER);
      addBottom = !addBottom;
    }    
    
    return;
  }

  /***************************************************************************
  ** 
  ** Handle link routing first pass for stacked usage. Note with stacked usage,
  ** we defer ALL link creation until after all the SSCs have been generate.
  */
  
  @SuppressWarnings("unused")
  public void routeLinksForStackPass1(GenomeSubset subset, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                      StaticDataAccessContext rcx, 
                                      Map<String, Integer> lengthChanges, 
                                      Map<String, Integer> extraGrowthChanges, 
                                      SpecialtyInstructions si, 
                                      Map<String, SuperSrcRouterPointSource> traceDefs, 
                                      int stackRow, 
                                      Map<Integer, Map<String, Point2D>> globalTraceFrame, 
                                      Map<String, Integer> sourceToStack, 
                                      Set<String> rowSources, GlobalTrackAssignment gta) {    
    if (!isStacked_) {
      throw new IllegalStateException();
    }

    //
    // First pass:
    //
     
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.deferredTerminalPaths = new HashMap<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>>();
   
      las.ssc.routeInboundLinks(subset, nps, rcx, lengthChanges, extraGrowthChanges,
                                si, traceDefs,
                                las.deferredTerminalPaths, true, isOnTop_, 
                                getDeferredSrcToTraceY(), stackRow, sourceToStack);
      // This does INTERNAL LINKS in the GASCs as well!
      las.ssc.startOutboundLinks(nps, rcx, si, trackToY_, srcToTrack_, 
                                 getDeferredSrcToTraceY(), true, traceDefs, las, true);
    }
    
    //
    // Get all the point sources intialized with stack origin info:
    //
    
    Iterator<String> asit = rowSources.iterator();
    while (asit.hasNext()) {
      String srcID = asit.next();
      SuperSrcRouterPointSource corners = traceDefs.get(srcID);    
      corners.setStackOriginRow(stackRow, globalTraceFrame);
      corners.setCurrStackRow(stackRow);
    }

    //
    // Even the forward links are deferred, so we need to get them placed in another step.  By deferring,
    // we have the actual drop X values we need in hand at this point.
    //
    scit = superClusters_.keySet().iterator();
    HashSet<String> onlySrcs = new HashSet<String>();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      las.ssc.finishInboundDeferredLinks(nps, rcx, si, 
                                         las.deferredTerminalPaths, traceDefs, traceOffset_, true, las, 
                                         stackRow, onlySrcs, SuperSourceCluster.ALWAYS_FLIP_ORDER);
      onlySrcs.addAll(las.forwardSources(nps));
    }
       
    return;
  }
  
  /***************************************************************************
  ** 
  ** Handle link routing second pass for stack usage.  Note with stacked usage,
  ** we defer ALL link creation until after all the SSCs have been generated.
  */
 
  @SuppressWarnings("unused")
  public void routeLinksForStackPass2(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                      StaticDataAccessContext rcx,
                                      Map<String, Integer> lengthChanges, 
                                      Map<String, Integer> extraGrowthChanges, 
                                      SpecialtyInstructions si, 
                                      Map<String, SuperSrcRouterPointSource> traceDefs,
                                      int ourRow, int sourceRow, 
                                      Set<String> sourceRowSources) {    


    if (!isStacked_) {
      throw new IllegalStateException();
    }
    
    //
    // Go back through and finish placing rightwards.  Move backwards only if we are in the 
    // same row as the source row and need to finish to the left of the source
    //
    
    ArrayList<Integer> keyList = new ArrayList<Integer>(superClusters_.keySet());
    if (ourRow == sourceRow) {
      Collections.reverse(keyList);
    }
       
    // In pass 2, we are only placing sources from a particular stack row for each call:    
    
    Set<String> onlySrcs = (ourRow == sourceRow) ? new HashSet<String>() : new HashSet<String>(sourceRowSources);
  
    Iterator<Integer> scit = keyList.iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      // Gotta catch feedbacks back into our own cluster, so this goes before call:
      if (ourRow == sourceRow) {
        onlySrcs.addAll(las.backwardSources(nps));     
      }
      las.ssc.finishInboundDeferredLinks(nps, rcx, si, 
                                         las.deferredTerminalPaths, traceDefs, traceOffset_, true, las, 
                                         ourRow, onlySrcs, SuperSourceCluster.FLIP_IF_SOURCE_IS_FROM_LEFT);
    }    
    
    return;
  }
   
  /***************************************************************************
  ** 
  ** Answer if we have a trace order
  */
  
  public boolean hasTrackForSource(String srcID) {
    return (srcToTrack_.get(srcID) != null);
  }
  
  /***************************************************************************
  ** 
  ** Get trace order
  */
  
  public int getTrackForSource(String srcID) {
    Integer val = srcToTrack_.get(srcID);
    if (val == null) {
      throw new IllegalArgumentException();
    }
    return (val.intValue());
  }
  
  /***************************************************************************
  ** 
  ** Get trace order
  */
  
  public int getTrackIndexForSource(String srcID) {
    List<String> stOrder = getSourceTrackOrder();
    return (stOrder.indexOf(srcID));
  }  

  /***************************************************************************
  ** 
  ** Get list of sources in trace order
  */
  
  public List<String> getSourceTrackOrder() {
    return (new ArrayList<String>(trackToSrc_.values()));
  }   

  /***************************************************************************
  ** 
  ** Get track count
  */
  
  public int trackCount() {
    return (srcToTrack_.size());
  }   
  
  /***************************************************************************
  ** 
  ** Get trace Y
  */
  
  public Double getTrackY(Integer trackNum) {
    return trackToY_.get(trackNum);
  }  
  
  /***************************************************************************
  ** 
  ** Get lowest trace Y
  */
  
  public double getLowestTraceY() {
    double retval = Double.NEGATIVE_INFINITY;
    Iterator<Double> ttyit = trackToY_.values().iterator();
    while (ttyit.hasNext()) {
      Double ty = ttyit.next();
      double tyval = ty.doubleValue();
      if (tyval > retval) {
        retval = tyval;
      }
    }
    if (retval == Double.NEGATIVE_INFINITY) {
      retval = noTracksMinY_;
    }
    
    return (retval);
  }  
  
  
  /***************************************************************************
  ** 
  ** The the trace count coming off the rightmost clust
  */
    
  public int getRightmostOutboundLinkOrderMax() {
    if (superClusters_.keySet().isEmpty()) {
      return (0);
    }
    Integer rightClustNum = superClusters_.lastKey();
    LinkAnnotatedSuper las = superClusters_.get(rightClustNum);
    return (las.ssc.getOutboundLinkOrderMax());    
  } 
  
  /***************************************************************************
  ** 
  ** Set of all sources
  */
  
  public Set<String> getAllSources() {  
    if (isStacked_) {
      throw new IllegalStateException();
    }
    HashSet<String> allSources = new HashSet<String>();
    Iterator<Integer> scit = superClusters_.keySet().iterator();
    while (scit.hasNext()) {
      Integer clustNum = scit.next();
      LinkAnnotatedSuper las = superClusters_.get(clustNum);
      allSources.addAll(las.ssc.getAllSources());
    }
    
    //
    // if we have pre-sources, they count as sources as well:
    //
    
    allSources.addAll(preSources_);
    
    return (allSources);
  }
  
 /***************************************************************************
  ** 
  ** Map tracknum to X value
  */
  
  public static double trackXVal(double baseX, Integer trackNum, GlobalTrackAssignment gta, double traceOffset) {      
    return (baseX - (((double)gta.currMaxTrack - (double)trackNum.intValue() + 1) * traceOffset)); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static class GlobalTrackAssignment {
    public int currMaxTrack;
    public int currMinTrack;    
    public HashMap<String, Integer> globalSrcToTrack;
    public TreeMap<Integer, String> globalTrackToSrc;  
 
       
    public GlobalTrackAssignment() {     
      globalSrcToTrack = new HashMap<String, Integer>();
      globalTrackToSrc = new TreeMap<Integer, String>();    
      currMaxTrack = 0;
      currMinTrack = -1;
    }
  }  
} 
