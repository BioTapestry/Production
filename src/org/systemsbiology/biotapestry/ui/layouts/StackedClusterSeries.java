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
import java.util.Map;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Represents a stack of ClusterSeries
*/

public class StackedClusterSeries {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private ArrayList<ClusterSeries> allSeries_;
  private ClusterSeries.GlobalTrackAssignment gta_;
  private HashMap<Integer, Map<String, Point2D>> stackedTraceStarts_;
  private HashMap<String, Integer> srcToStack_;
  private double traceOffset_;
  private Rectangle stackBounds_;
  private Rectangle stackNodeOnlyBounds_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public StackedClusterSeries(double traceOffset) {
    allSeries_ = new ArrayList<ClusterSeries>();
    traceOffset_ = traceOffset;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get the stack bounds
  */

  public Rectangle getStackBounds() {
    return (stackBounds_);
  }
  
  /***************************************************************************
  ** 
  ** Get the stack node-only bounds
  */

  public Rectangle getStackNodeOnlyBounds() {
    return (stackNodeOnlyBounds_);
  }
 
  /***************************************************************************
  ** 
  ** Get the source order
  */

  public SortedMap<Integer, String> getSourceOrder() {
    return (gta_.globalTrackToSrc);
  }
  
  /***************************************************************************
  ** 
  ** Prep the stack
  */

  @SuppressWarnings("unused")
  public void prep(int rows, Map<Integer, List<GeneAndSatelliteCluster>> allClusters, 
                   GenomeSubset subset, SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx,
                   int traceMult, SortedMap<Integer, String> previousSrcToTrack) {
   
 //   int numClust = allClusters.size();
 //   int maxPerRow = numClust / rows;
    
    int currRow = 0;
    int currCol = 0;
    ClusterSeries currSeries = new ClusterSeries(true, traceMult);
    allSeries_.add(currSeries);
    Iterator<List<GeneAndSatelliteCluster>> acvit = allClusters.values().iterator();
    while (acvit.hasNext()) {
      List<GeneAndSatelliteCluster> forRow = acvit.next();
      for (int i = 0; i < forRow.size(); i++) {
        GeneAndSatelliteCluster sc = forRow.get(i);     
        currSeries.addSourceCluster(currCol++, sc);
      }
      if (acvit.hasNext()) {
        currRow++;
        currCol = 0;
        currSeries = new ClusterSeries(true, traceMult);
        allSeries_.add(currSeries);
      }
    }
    
    int numSeries = allSeries_.size();
    
    HashMap<String, Point> targets = new HashMap<String, Point>();
    HashMap<String, Point> sources = new HashMap<String, Point>();
    HashMap<Point, Integer> order = new HashMap<Point, Integer>();    
    
    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      nextSeries.preprocessForAnnotate(subset, targets, sources, order, i, nps);
    }
    //
    // Need sourceID -> Stack Location
    //
    
    srcToStack_ = new HashMap<String, Integer>();
    Iterator<String> skit = sources.keySet().iterator();
    while (skit.hasNext()) {
      String linkID = skit.next(); 
      Linkage link = nps.getLinkage(linkID);
      Point pt = sources.get(linkID);
      String srcID = link.getSource();
      srcToStack_.put(srcID, new Integer(pt.y));
    }
    // Without genome subsets, the above caught everybody.  Now we need
    // to look at targets too since not all srcs live in our stack anymore!
    int fakecount = -1;
    Iterator<String> tkit = targets.keySet().iterator();
    while (tkit.hasNext()) {
      String linkID = tkit.next(); 
      Linkage link = nps.getLinkage(linkID);
      String srcID = link.getSource();
      if (srcToStack_.get(srcID) == null) {   
        srcToStack_.put(srcID, new Integer(fakecount--));
      }
    }

    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      nextSeries.prepForStackPass1(nps, rcx, targets, sources, order, i);  
    }
    
    //
    // Get global track numbers built.  First thing is to allocate tracks
    // for external sources.  Use previously determined ordering first:
    //

    gta_ = new ClusterSeries.GlobalTrackAssignment();
    int externalTrack = 0;
    HashSet<String> externalSources = new HashSet<String>(subset.externalSourceOnly);
    externalSources.addAll(subset.externalBothSrcTarget);
    
    TreeSet<String> stillToDo = new TreeSet<String>(externalSources);  // consistent but arbitrary...
    Iterator<Integer> pstit = previousSrcToTrack.keySet().iterator();
    while (pstit.hasNext()) {
      Integer trackNum = pstit.next();
      String checkSrc = previousSrcToTrack.get(trackNum);
      if (externalSources.contains(checkSrc)) {
        Integer track = new Integer(externalTrack++);
        gta_.globalSrcToTrack.put(checkSrc, track);
        gta_.globalTrackToSrc.put(track, checkSrc);
        gta_.currMaxTrack++;
        stillToDo.remove(checkSrc);
      }     
    }
    Iterator<String> stdit = stillToDo.iterator();
    while (stdit.hasNext()) {
      String srcID = stdit.next();
      Integer track = new Integer(externalTrack++);
      gta_.globalSrcToTrack.put(srcID, track);
      gta_.globalTrackToSrc.put(track, srcID);
      gta_.currMaxTrack++; 
    }

    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      nextSeries.calcGlobalTrackNumbers(nps, gta_);
    }
    
    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      nextSeries.prepForStackPass2(nps, targets, sources, order, i, gta_);  
    }

    return;
  } 
     
  /***************************************************************************
  ** 
  ** Locate the Stacked cluster series
  */
  
  public void locate(Point2D center, SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx, LayoutCompressionFramework lcf) {
   
    Point2D gridCenter = (Point2D)center.clone();
    UiUtil.forceToGrid(gridCenter, UiUtil.GRID_SIZE);
    Point2D currentPlacement = (Point2D)gridCenter.clone();
 
    ArrayList<Rectangle> lcfVals = (lcf == null) ? null : new ArrayList<Rectangle>();
    ArrayList<Map<String, Double>> holdYTraces = new ArrayList<Map<String, Double>>();
    int numSeries = allSeries_.size();
    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      double traceTop = currentPlacement.getY();
      //
      // The error below gets stuck into the series here, where the top does not
      // reflect compressed size.  This results in left-end trace starts not reflecting
      // compressed geometry!
      //
      double traceBottom = nextSeries.setTrackPlacementForStacked(traceTop);
      holdYTraces.add(nextSeries.getDeferredSrcToTraceY());
      currentPlacement.setLocation(currentPlacement.getX(), traceBottom + nextSeries.getTraceOffset());
      //
      // 11/28/12: Note that in the maximum compression option, this height is not going to be correct,
      // so we end up with traces in the wrong location!
      //
      double heightOfCluster = nextSeries.getMaximumHeight(nps, rcx);
      Point2D rightEnd = nextSeries.locate(currentPlacement, nps, rcx, new Double(heightOfCluster));     
      currentPlacement.setLocation(currentPlacement.getX(), currentPlacement.getY() + heightOfCluster);
      Rectangle csBounds = new Rectangle((int)currentPlacement.getX(), (int)traceTop, 
                                         (int)(rightEnd.getX() - currentPlacement.getX()), 
                                         (int)(heightOfCluster + (traceBottom - traceTop)));
      if (stackBounds_ == null) {
        stackBounds_ = (Rectangle)csBounds.clone();
      } else if (csBounds != null) {
        Bounds.tweakBounds(stackBounds_, csBounds);
      }
  
      if (lcf != null) {
        lcfVals.add(csBounds);
      }
    }
    
    stackNodeOnlyBounds_ = getPlacedNodeOnlyBounds(nps, rcx);
 
    //
    // Having located each series, we need to build a scaffold for the inter-stack
    // link points on the left margin:
    //
    
    double maxRight = Double.NEGATIVE_INFINITY;
    stackedTraceStarts_ = new HashMap<Integer, Map<String, Point2D>>();
    for (int i = 0; i < numSeries; i++) { 
      HashMap<String, Point2D> tracesPerRow = new HashMap<String, Point2D>();
      stackedTraceStarts_.put(new Integer(i), tracesPerRow);
      Map<String, Double> yTraces = holdYTraces.get(i);
      Iterator<String> ytkit = yTraces.keySet().iterator();
      while (ytkit.hasNext()) {
        String srcID = ytkit.next();
        Double yTrace = yTraces.get(srcID);
        Integer trackNum = gta_.globalSrcToTrack.get(srcID);
        Point2D rightForTrack = new Point2D.Double(ClusterSeries.trackXVal(currentPlacement.getX(), trackNum, gta_, traceOffset_), yTrace.doubleValue());
        tracesPerRow.put(srcID, rightForTrack);
        if (rightForTrack.getX() > maxRight) {
          maxRight = rightForTrack.getX();
        }
      }
    }
    
    if (lcf != null) {
      lcf.initialize(lcfVals);
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Get the bounds of JUST the nodes:
  */
  
  private Rectangle getPlacedNodeOnlyBounds(SpecialtyLayoutEngine.NodePlaceSupport nps, StaticDataAccessContext rcx) {    
    Rectangle retval = null;
    int numSeries = allSeries_.size();
    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      Rectangle nsBounds = nextSeries.getNodeOnlyBounds(nps, rcx);
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
  ** get sources coming from a given row
  */
  
  private Set<String> sourcesForRow(int sourceRow) {      
    HashSet<String> onlySrcs = new HashSet<String>();
    Iterator<String> s2sit = srcToStack_.keySet().iterator();
    while (s2sit.hasNext()) {
      String srcID = s2sit.next();
      Integer stackForSrc = srcToStack_.get(srcID);
      if (stackForSrc.intValue() == sourceRow) {
        onlySrcs.add(srcID);
      }
    }
    return (onlySrcs);
  } 

  /***************************************************************************
  ** 
  ** Handle link routing for stacked layouts 
  */

  @SuppressWarnings("unused")
  public void routeLinksForStack(GenomeSubset subset, SpecialtyInstructions sp,
                                 SpecialtyLayoutEngine.NodePlaceSupport nps,
                                 StaticDataAccessContext rcx, 
                                 SpecialtyLayoutEngine.GlobalSLEState gsles, Rectangle nodeBounds) {    

    HashMap<String, SuperSrcRouterPointSource> traceDefs = new HashMap<String, SuperSrcRouterPointSource>();
    Map<String, Integer> extraGrowthChanges = sp.extraGrowthChanges;    
    Map<String, Integer> lengthChanges = sp.lengthChanges;
 
    int numSeries = allSeries_.size();
 
    //
    // No sources?  No links! No links?  No routing!
    // NOPE! A set of standalone GASCs with internal links WILL need routing! This is too early! (V6 not laying out GASC-only links)
    
    // if (srcToStack_.isEmpty()) {
    //    return;
    // }
    
    //
    // Find the fake lowest stack source:
    //
    
    int lowest = -1;
    if (!srcToStack_.isEmpty()) {
      TreeSet<Integer> sorted = new TreeSet<Integer>(srcToStack_.values());
      lowest = sorted.first().intValue();
       
      //
      // Set up the SuperSrcRouterPointSource for any external inputs the first time we see it:
      //
         
      Iterator<String> asit1 = subset.getSourcesHeadedIn().iterator();
      while (asit1.hasNext()) {
        String srcID = asit1.next();
        for (int i = 0; i < numSeries; i++) {
          ClusterSeries nextSeries = allSeries_.get(i);
          if (nextSeries.hasTrackForSource(srcID)) {       
            SuperSrcRouterPointSource corners = new SuperSrcRouterPointSource(srcID, true, true);
            traceDefs.put(srcID, corners);          
            //
            // Tricky!  For x value, we need the GLOBAL tracknum.  For y value, we need
            // the local tracknum for the Cluster series!
            //
            Integer trackNumObj = gta_.globalSrcToTrack.get(srcID);
            Double trackVal = new Double(ClusterSeries.trackXVal(nextSeries.getBasePos().getX(), trackNumObj, gta_, traceOffset_));
            Double trackY = nextSeries.getTrackY(new Integer(nextSeries.getTrackForSource(srcID))); 
            corners.initForExternal(trackVal, trackY, stackedTraceStarts_, i);          
            break;
          }
        }
      }
    }
    
    //
    // Handle each row's links. THIS does INTERNAL GASC links as well!
    //
    
    for (int i = 0; i < numSeries; i++) {
      ClusterSeries nextSeries = allSeries_.get(i);
      Set<String> sources = sourcesForRow(i);
      nextSeries.routeLinksForStackPass1(subset, nps, rcx, lengthChanges, extraGrowthChanges, 
                                         sp, traceDefs, i, stackedTraceStarts_, 
                                         srcToStack_, sources, gta_);
    }
   
    //
    // **Now** we can quit if we are only doing internal GASC links!
    //
    
    if (srcToStack_.isEmpty()) {
      return;
    }
       
    // i is the source row, and must begin below 0 for external inputs:
    
    for (int i = lowest; i < numSeries; i++) {
      //
      // Go upwards, moving backwards:
      //
      int jstrt = (i < 0) ? 0 : i;
      for (int j = jstrt; j >= 0; j--) {
        ClusterSeries nextSeries = allSeries_.get(j);
        Set<String> sources = sourcesForRow(i);
        nextSeries.routeLinksForStackPass2(nps, rcx, lengthChanges, extraGrowthChanges, 
                                           sp, traceDefs, j, i, sources);
      }
      //
      // Go downwards, moving forward:
      //
      for (int j = jstrt + 1; j < numSeries; j++) {
        ClusterSeries nextSeries = allSeries_.get(j);
        Set<String> sources = sourcesForRow(i);
        nextSeries.routeLinksForStackPass2(nps, rcx, lengthChanges, extraGrowthChanges, 
                                           sp, traceDefs, j, i, sources);
      }
    }
    
    Set<String> inSrcs = subset.getSourcesHeadedIn();
    Set<String> outSrcs = subset.getSourcesHeadedOut();
    
    //
    // Border points for inbound sources:
    //
    
    Iterator<String> isit = inSrcs.iterator();
    while (isit.hasNext()) {
      String srcID = isit.next();
      SuperSrcRouterPointSource inForSrc = traceDefs.get(srcID);
      SpecialtyLayoutLinkData sin = sp.getLinkPointsForSrc(srcID);
      inForSrc.stackedSetupForInboundSources(sin, nps, nodeBounds);
    }
    
    //
    // Finish up with sources that are heading out the door:
    //
           
    Iterator<String> osit = outSrcs.iterator();
    while (osit.hasNext()) {
      String srcID = osit.next();
      SuperSrcRouterPointSource outForSrc = traceDefs.get(srcID);
      SpecialtyLayoutLinkData sin = sp.getOrStartLinkPointsForSrc(srcID);
      outForSrc.stackedSetupForOutboundSources(sin, nps, rcx, nodeBounds);
      Set<String> outLinks = subset.getLinksHeadedOutForSource(srcID);     
      Iterator<String> goit = outLinks.iterator();
      Point2D fakie = null;
      List<String> usePoint = sin.getLinkList();
      if (usePoint.isEmpty()) {
        fakie = sin.getInitRoute().get(0).getPoint();
      } else {
        fakie = sin.getPositionList(usePoint.get(0)).get(0).getPoint();
      }
      while (goit.hasNext()) {
        String linkID = goit.next();
        if (sin.haveLink(linkID)) {
          throw new IllegalStateException();
        }
        sin.startNewLink(linkID);
        //
        // Note that the point here is used in the boundary calculations!  So setting it to "BOTTOM_BORDER" when
        // it is not going out the bottom just makes the region grow for no particular reason!  I believe 
        // it actually does not matter which point?  Is it not dropped??
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)fakie.clone()));         
      }
    } 
    
    for (int i = 0; i < numSeries; i++) {
      Set<String> sources = sourcesForRow(i);
      Iterator<String> sfrit = sources.iterator();
      while (sfrit.hasNext()) {
        String srcID = sfrit.next();
        SuperSrcRouterPointSource outForSrc = traceDefs.get(srcID);
        SpecialtyLayoutLinkData sin = sp.getOrStartLinkPointsForSrc(srcID);
        outForSrc.establishLeftPoints(sin);
      }
    }
 
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

} 
