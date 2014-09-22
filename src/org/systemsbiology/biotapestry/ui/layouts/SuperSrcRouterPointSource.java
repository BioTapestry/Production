/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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
import java.util.Map;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Used for getting link corners for a super source cluster
*/

public class SuperSrcRouterPointSource {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private String srcID_;
  private Point2D root_;
  private boolean rootPlaced_;
  private SpecialtyLayoutLinkData.TrackPos internalBranch_;
  private boolean internalBranchPlaced_;    
  private SpecialtyLayoutLinkData.TrackPos outCorner_;
  private boolean outCornerPlaced_;    
  private Point2D trace_;
  private boolean tracePlaced_;
  private Point2D feedTrace_;
  private boolean feedTracePlaced_;    
  private Point2D mainHotTip_;
  private Point2D feedbackHotTip_;
  private boolean baseAtTop_;

  private boolean prePointsPlaced_;  
  private List<SpecialtyLayoutLinkData.TrackPos> prePoints_;
  private GeneAndSatelliteCluster departureGaS_;
  private boolean forStackedClusters_;
  private int currStackRow_;
  private int stackOriginRow_;
  private TreeMap<Integer, Point2D> stackTraces_;
  private Map<Integer, Map<String, Point2D>> globalTraceFrame_;
  private Integer hotStackRow_;
  
  
//                        T---------
//                        |
//          R------I------O
//                 |
//           ------F
//
// R = root
// I = internal branch
// O = out corner
// T = trace
// F = feedtrace
// hot tips migrate out as more links are added
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */ 
  SuperSrcRouterPointSource(String srcID, boolean baseAtTop, boolean forStackedClusters) {
    srcID_ = srcID;
    baseAtTop_ = baseAtTop;
    prePointsPlaced_ = false;
    rootPlaced_ = false;
    internalBranchPlaced_ = false;
    outCornerPlaced_ = false;
    tracePlaced_ = false;
    forStackedClusters_ = forStackedClusters;
    if (forStackedClusters_) {
      stackTraces_ = new TreeMap<Integer, Point2D>();
    }
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void init(GeneAndSatelliteCluster sc, 
                   SpecialtyLayoutEngine.NodePlaceSupport nps,
                   DataAccessContext irx, 
                   Double traceX, Double track,
                   Map<String, Integer> srcToTrack, Map<Integer, Double> trackToY, 
                   Map<String, Double> srcToFeedbackY, 
                   SpecialtyLayoutLinkData sin, TrackedGrid grid) {
    
    departureGaS_ = sc;
    //
    // Now that we have fan-outs, a complex path may precede any of these
    // standard superSourceCluster points:
    //
    if (sin != null) {
      List<String> links = sin.getLinkList();
      SpecialtyLayoutLinkData.NoZeroList positions = sin.getPositionList(links.get(0));
      prePoints_ = new ArrayList<SpecialtyLayoutLinkData.TrackPos>();
      positions.appendTo(prePoints_);
    }
   
    if ((sin == null) && srcID_.equals(sc.getCoreID())) {
      root_ = sc.linkTreeRoot(nps, irx);
      if (traceX != null) {
        Point2D ibp = new Point2D.Double(traceX.doubleValue(), root_.getY()); 
        internalBranch_ = new SpecialtyLayoutLinkData.TrackPos(ibp);
      }
      if (track != null) {
        Point2D ocp = new Point2D.Double(track.doubleValue(), root_.getY()); 
        outCorner_ = new SpecialtyLayoutLinkData.TrackPos(ocp);
      }
    } else {
      root_ = null;
      if (sin == null) {
        Point2D srcLoc = nps.getPosition(srcID_);
        if (traceX != null) {
          Point2D ibp = new Point2D.Double(traceX.doubleValue(), srcLoc.getY()); 
          internalBranch_ = new SpecialtyLayoutLinkData.TrackPos(ibp);
        }
        if (track != null) {
          Point2D ocp = new Point2D.Double(track.doubleValue(), srcLoc.getY()); 
          outCorner_ = new SpecialtyLayoutLinkData.TrackPos(ocp);
        }
      } else {
        TrackedGrid.TrackPosRC lastPt = (TrackedGrid.TrackPosRC)prePoints_.get(prePoints_.size() - 1);
        TrackedGrid.RCTrack tprc = lastPt.getRCTrack();
        if (traceX != null) {
          internalBranch_ = new TrackedGrid.TrackPosRC(grid.buildRCTrack(tprc, TrackedGrid.RCTrack.X_FIXED, traceX.doubleValue()));
        }
        if (track != null) {
          outCorner_ = new TrackedGrid.TrackPosRC(grid.buildRCTrack(tprc, TrackedGrid.RCTrack.X_FIXED, track.doubleValue()));
        }
      }
    }
    if (track != null) {
      Integer trackNum = srcToTrack.get(srcID_);
      if (trackNum != null) {
        Double trackY = trackToY.get(trackNum);
        if (trackY != null) {         
          trace_ = new Point2D.Double(track.doubleValue(), trackY.doubleValue());
        }
      }
    }   
    Double feedY = srcToFeedbackY.get(srcID_);
    if (feedY != null) {
      // Just a backup if we have no internal branch.  But we _better_ have one!
      double feedX = 0.0;
      if (internalBranch_ != null) {
        if (internalBranch_.needsConversion()) {
          TrackedGrid.TrackPosRC ibrc = (TrackedGrid.TrackPosRC)internalBranch_;
          feedX = ibrc.getRCTrack().getFixedValue(TrackedGrid.RCTrack.X_FIXED);
        } else {
          feedX = internalBranch_.getPoint().getX();
        }
      }
      feedTrace_ = new Point2D.Double(feedX, feedY.doubleValue()); 
    }
    return;
  }
    
  /***************************************************************************
  **
  ** A function
  */
    
  public void initForExternal(Double track, Double trackY, Map<Integer, Map<String, Point2D>> globalTraceFrame, int inRow) {  
    trace_ = new Point2D.Double(track.doubleValue(), trackY.doubleValue());
    Point2D ocp = (Point2D)trace_.clone();
    outCorner_ = new SpecialtyLayoutLinkData.TrackPos(ocp); 
    
    if (forStackedClusters_) {
      stackOriginRow_ = inRow;
      currStackRow_ = inRow;
      globalTraceFrame_ = globalTraceFrame;
      hotStackRow_ = null;
      Integer key = new Integer(inRow);    
      Point2D myTraceForTrg = (Point2D)trace_.clone();
      stackTraces_.put(key, myTraceForTrg);
    }
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
    
  public Point2D startInternalBranch(SpecialtyLayoutLinkData sin, String linkID, 
                                     SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    Point2D hotTip = null;
    
    if (!prePointsPlaced_ && (prePoints_ != null)) {
      List<SpecialtyLayoutLinkData.TrackPos> conv = departureGaS_.convertDeparturePath(prePoints_, nps, irx);  
      sin.addPositionListToLink(linkID, conv);
      prePointsPlaced_ = true;
    }
   
    if (!rootPlaced_) {
      if (root_ != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(root_));
      rootPlaced_ = true;
    } else {
      if (root_ != null) hotTip = root_;
    }
    if (!internalBranchPlaced_) {
      if (internalBranch_ != null) {
        Point2D ibp = convertToPoint(internalBranch_, nps, irx);
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(ibp));
      }
      internalBranchPlaced_ = true;
    } else {
      if (internalBranch_ != null) {        
        hotTip = convertToPoint(internalBranch_, nps, irx);
      }
    }
    return (hotTip);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D convertToPoint(SpecialtyLayoutLinkData.TrackPos tpos, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                DataAccessContext irx) {  
    Point2D convIB;
    if (tpos.needsConversion()) {
      TrackedGrid.TrackPosRC ibrc = (TrackedGrid.TrackPosRC)tpos;
      convIB = departureGaS_.convertDeparturePoint(ibrc, nps, irx);          
    } else {
      convIB = tpos.getPoint();
    }
    return ((Point2D)convIB.clone());    
  }
      
  /***************************************************************************
  **
  ** A function
  */
  
  public String getSrcID() {
    return (srcID_);
  }
     
  /***************************************************************************
  **
  ** A function
  */
  
  public void updateMainHotTip(Point2D mainHotTip) {
    mainHotTip_ = mainHotTip;
    return;
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getMainHotTip() {
    return (mainHotTip_);
  }     
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void setStackOriginRow(int stackOriginRow, Map<Integer, Map<String, Point2D>> globalTraceFrame) {
    if (!forStackedClusters_) {
      throw new IllegalStateException();
    }
    stackOriginRow_ = stackOriginRow;
    currStackRow_ = stackOriginRow;
    globalTraceFrame_ = globalTraceFrame;
    hotStackRow_ = null;
    initTraceForRow(stackOriginRow);
    return;
  }
      
  /***************************************************************************
  **
  ** A function
  */
  
  public int getCurrStackRow() {
    return (currStackRow_);
  }
      
  /***************************************************************************
  **
  ** A function
  */
  
  public int getStackOriginRow() {
    return (stackOriginRow_);
  }
      
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean isForStackedCluster() {
    return (forStackedClusters_);
  }    
     
  /***************************************************************************
  **
  ** A function
  */
   
  public void setCurrStackRow(int currStackRow) {
    if (!forStackedClusters_) {
      throw new IllegalStateException();
    }
    currStackRow_ = currStackRow;
    initTraceForRow(currStackRow);
    return;
  }
  
   /***************************************************************************
  **
  ** A function
  */
   
  private void initTraceForRow(int stackRow) {
    Integer key = new Integer(stackRow);
    if (stackTraces_.get(key) != null) {
      return;
    }
    Map<String, Point2D> traceForTrgRow = globalTraceFrame_.get(key);
    Point2D traceForTrg = traceForTrgRow.get(srcID_);
    if (traceForTrg != null) {
      Point2D myTraceForTrg = (Point2D)traceForTrg.clone();
      stackTraces_.put(key, myTraceForTrg);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** A function
  */
  
   public void updateHotStackRow() {
     hotStackRow_ = new Integer(currStackRow_);
     return;
   }
   
  /***************************************************************************
  **
  ** A function
  */
  
   public void resetHotStackRowToOrigin() {
     hotStackRow_ = new Integer(stackOriginRow_);
     return;
   }
      
  /***************************************************************************
  **
  ** A function
  */
     
   public Integer getHotStackRow() {
    return (hotStackRow_);
  }
     
  /***************************************************************************
  **
  ** A function
  */
   
  public void updateFeedbackHotTip(Point2D feedbackHotTip) {
    feedbackHotTip_ = feedbackHotTip;
    return;
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getFeedbackHotTip() {
    return (feedbackHotTip_);
  }
     
  /***************************************************************************
  **
  ** A function
  */
   
  public Point2D getInternalBranch(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    return ((internalBranch_ == null) ? null : convertToPoint(internalBranch_, nps, irx));
  }  
      
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getTrace() {
    return (trace_);
  }
      
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getFeedTrace() {
    return (feedTrace_);
  } 
     
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getCurrentStackedTrace() {
    Integer key = new Integer(currStackRow_);
    Point2D retval = stackTraces_.get(key);
    return (retval);
  }
     
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getMinStackedTrace() {
    Point2D retval = stackTraces_.get(stackTraces_.firstKey());
    return (retval);
  }
 
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getMaxStackedTrace() {
    Point2D retval = stackTraces_.get(stackTraces_.lastKey());
    return (retval);
  }
   
  /***************************************************************************
  **
  ** A function
  */
  
  public Integer getMaxStackedTraceKey() {
    return (new TreeSet<Integer>(globalTraceFrame_.keySet()).last());
  }
     
  /***************************************************************************
  **
  ** The initial stack trace is the point out in the vertical stack bus
  ** living in the initial source row.
  */
  
  public Point2D getInitialStackedTrace() {
    Integer key = new Integer(stackOriginRow_);
    Point2D retval = stackTraces_.get(key);
    return (retval);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getLastStackedTrace() {
    if (stackTraces_.isEmpty()) {
      return (null);
    }
    return (stackTraces_.get(hotStackRow_));
  }  
  
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean haveAStackedTrace() {
    return (!stackTraces_.isEmpty());
  }
   
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean isBaseAtTop() {
    return (baseAtTop_);
  }
      
  /***************************************************************************
  **
  ** Start the outbound branch.  Note that in e.g. complex fan out cases, this
  ** just involves gluing the prepoints into the link!
  ** 
  */
  
  public Point2D startOutboundBranch(SpecialtyLayoutLinkData sin, String linkID, 
                                     SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     DataAccessContext irx) {
    Point2D hotTip = null;
    
    //
    // This is the crucial stuff for complex fan outs:
    
    //Point2D ppChk = null;
    
    if (!prePointsPlaced_ && (prePoints_ != null)) {
      List<SpecialtyLayoutLinkData.TrackPos> conv = departureGaS_.convertDeparturePath(prePoints_, nps, irx);  
      sin.addPositionListToLink(linkID, conv);
      //ppChk = conv.get(conv.size() - 1).getPoint();
      prePointsPlaced_ = true;
    }
  
    if (!rootPlaced_) {
      if (root_ != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(root_));
      rootPlaced_ = true;
    } else {
      if (root_ != null) hotTip = root_;
    }
    
    if (!internalBranchPlaced_) {
      if (hotTip != null) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        hotTip = null;
      }
      if (internalBranch_ != null) {
        Point2D ibp = convertToPoint(internalBranch_, nps, irx);
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(ibp));
      }
      internalBranchPlaced_ = true;
    } else {
      if (internalBranch_ != null) {        
        hotTip = convertToPoint(internalBranch_, nps, irx);
      }
    }
 
    //Point2D ocpChk = null;
    
    if (!outCornerPlaced_) {
      if (hotTip != null) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        hotTip = null;
      }
      if (outCorner_ != null) {
        Point2D ibp = convertToPoint(outCorner_, nps, irx);
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(ibp));
        //ocpChk = ibp;
      }      
      outCornerPlaced_ = true;
    } else {
      if (outCorner_ != null) {        
        hotTip = convertToPoint(outCorner_, nps, irx);
        //ocpChk = hotTip;
      }
    }
    if (!tracePlaced_) {
      if (hotTip != null) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        hotTip = null;
      }
      if (trace_ != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(trace_));
      tracePlaced_ = true;
    } else {
       if (trace_ != null) hotTip = trace_;
       if (hotTip != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
    }

    return (hotTip);
  }    
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D startFeedbackBranch(SpecialtyLayoutLinkData sin, String linkID,
                                     SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     DataAccessContext irx) {
    Point2D hotTip = null;
    
    if (!prePointsPlaced_ && (prePoints_ != null)) {
      List<SpecialtyLayoutLinkData.TrackPos> conv = departureGaS_.convertDeparturePath(prePoints_, nps, irx);  
      sin.addPositionListToLink(linkID, conv);
      prePointsPlaced_ = true;
    }        

    if (!rootPlaced_) {
      if (root_ != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(root_));
      rootPlaced_ = true;
    } else {
      if (root_ != null) hotTip = root_;
    }
    if (!internalBranchPlaced_) {
      if (hotTip != null) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        hotTip = null;
      }
      if (internalBranch_ != null) {
        Point2D ibp = convertToPoint(internalBranch_, nps, irx);
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(ibp));
      }
      internalBranchPlaced_ = true;
    } else {
      if (internalBranch_ != null) {        
        hotTip = convertToPoint(internalBranch_, nps, irx);
      }
    }  
    if (!feedTracePlaced_) {
      if (hotTip != null) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        hotTip = null;
      }
      if (feedTrace_ != null) sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(feedTrace_));
      feedTracePlaced_ = true;
    } else {
      if (feedTrace_ != null) hotTip = feedTrace_;
    }
    return (hotTip);
  }
  
  /***************************************************************************
  ** 
  ** Code for routing in a stacked context.  THIS IS USED FOR SOURCES LEAVING
  ** THE STACK TO GO ELSEWHERE
  ** MISSING A LAYOUT POINT? THIS IS THE PLACE TO LOOK!
  */

  public void stackedSetupForOutboundSources(SpecialtyLayoutLinkData sin, 
                                             SpecialtyLayoutEngine.NodePlaceSupport nps,
                                             DataAccessContext irx, 
                                             Rectangle bounds) {
         
    Point2D backTip = getFeedbackHotTip();
    Integer hotRow = getHotStackRow();
    Point2D useForRight = null;
    int debugPath = -1;
    //
    // The border-splicing operation handles the boundary
    // track assignments, and this tweak just gets in the way:
    //
    double borderTweak = 0.0;
    bounds = UiUtil.rectFromRect2D(UiUtil.padTheRect(bounds, 2.0 * UiUtil.GRID_SIZE));
       
    if (backTip == null) { // No starting infrastructure!
      debugPath = 0;
      sin.startInitRoute();
      startOutboundBranch(sin, null, nps, irx);
      SpecialtyLayoutLinkData.NoZeroList initRoute = sin.getInitRoute(); 
      Point2D ob = initRoute.get(initRoute.size() - 1).getPoint();
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(ob, false), SpecialtyLayoutLinkData.INIT_POINT);
      Point2D initStack = (Point2D)getInitialStackedTrace().clone();
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(initStack, false), SpecialtyLayoutLinkData.INIT_TRACE);
      sin.setBorderPosition(SpecialtyLayoutLinkData.LEFT_BORDER, initStack);
      useForRight = ob;
      Point2D bottomPt = new Point2D.Double(initStack.getX(), bounds.getMaxY() + borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.BOTTOM_BORDER, bottomPt);
      Point2D topPt = new Point2D.Double(initStack.getX(), bounds.getMinY() - borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.TOP_BORDER, topPt);
      if (haveAStackedTrace()) {
        Point2D topStack = (Point2D)getMinStackedTrace().clone();
        sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(topStack, true), SpecialtyLayoutLinkData.MIN_TRACE);
        Point2D botStack = (Point2D)getMaxStackedTrace().clone();
        sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(botStack, true), SpecialtyLayoutLinkData.MAX_TRACE);
      } 
    } else if (hotRow != null) { // made it out to left bus!
      debugPath = 1;
      Point2D initStack = (Point2D)getInitialStackedTrace().clone();
      sin.setBorderPosition(SpecialtyLayoutLinkData.LEFT_BORDER, initStack);
      useForRight = initStack;
      Point2D topStack = (Point2D)getMinStackedTrace().clone();
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(topStack, true), SpecialtyLayoutLinkData.MIN_TRACE);
      Point2D topPt = new Point2D.Double(initStack.getX(), bounds.getMinY() - borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.TOP_BORDER, topPt);
     
      Point2D botStack = (Point2D)getMaxStackedTrace().clone();
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(botStack, true), SpecialtyLayoutLinkData.MAX_TRACE);
      Point2D botPt = new Point2D.Double(initStack.getX(), bounds.getMaxY() + borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.BOTTOM_BORDER, botPt);
 
    } else {  // somewhere in initial row, but never made it to the stack traces
      debugPath = 2; 
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(backTip, true), SpecialtyLayoutLinkData.LEFT_TIP);   
      Point2D initStack = (Point2D)getInitialStackedTrace().clone();
      useForRight = (Point2D)getTrace().clone();
      sin.setBorderPosition(SpecialtyLayoutLinkData.LEFT_BORDER, initStack);
      sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(initStack, false), SpecialtyLayoutLinkData.INIT_TRACE);
      Point2D topPt = new Point2D.Double(initStack.getX(), bounds.getMinY() - borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.TOP_BORDER, topPt);
      Point2D botPt = new Point2D.Double(initStack.getX(), bounds.getMaxY() + borderTweak);
      sin.setBorderPosition(SpecialtyLayoutLinkData.BOTTOM_BORDER, botPt); 
      if (haveAStackedTrace()) {
        Point2D topStack = (Point2D)getMinStackedTrace().clone();
        sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(topStack, true), SpecialtyLayoutLinkData.MIN_TRACE);
        Point2D botStack = (Point2D)getMaxStackedTrace().clone();
        sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(botStack, true), SpecialtyLayoutLinkData.MAX_TRACE);
      }      
    }

    sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(useForRight, false), SpecialtyLayoutLinkData.RIGHT_TIP);
    Point2D rightPt = new Point2D.Double(bounds.getMaxX() + borderTweak, useForRight.getY());
    sin.setBorderPosition(SpecialtyLayoutLinkData.RIGHT_BORDER, rightPt); 
    LayoutFailureTracker.recordBordersAndExits(this, sin, debugPath);
      
    return;
  }
  
   /***************************************************************************
  ** 
  ** Code for routing in a stacked context.  THIS IS USED FOR SOURCES COMING IN
  ** FROM ANOTHER SOURCE
  */

  public void stackedSetupForInboundSources(SpecialtyLayoutLinkData sin,
                                            SpecialtyLayoutEngine.NodePlaceSupport nps,
                                            Rectangle bounds) {
          
    double borderTweak = 0.0;
    bounds = UiUtil.rectFromRect2D(UiUtil.padTheRect(bounds, 2.0 * UiUtil.GRID_SIZE));
    
    Point2D initStack = (Point2D)getInitialStackedTrace().clone();
    sin.setBorderPosition(SpecialtyLayoutLinkData.LEFT_BORDER, initStack);     
    Point2D rightPt = new Point2D.Double(bounds.getMaxX() + borderTweak, initStack.getY());
    sin.setBorderPosition(SpecialtyLayoutLinkData.RIGHT_BORDER, rightPt);
    Point2D bottomPt = new Point2D.Double(initStack.getX(), bounds.getMaxY() + borderTweak);
    sin.setBorderPosition(SpecialtyLayoutLinkData.BOTTOM_BORDER, bottomPt);
    Point2D topPt = new Point2D.Double(initStack.getX(), bounds.getMinY() - borderTweak);
    sin.setBorderPosition(SpecialtyLayoutLinkData.TOP_BORDER, topPt);  
    return;
  }
   
  /***************************************************************************
  ** 
  ** The boundary splicer needs to have info on EVERY trace hitting the left
  ** margin, regardless of whether it is inbound or outbound.  Insure this is
  ** available!
  */

  public void establishLeftPoints(SpecialtyLayoutLinkData sin) {         
    Integer hotRow = getHotStackRow();
    if (hotRow != null) { // made it out to left bus!      
      Point2D initStack = (Point2D)getInitialStackedTrace().clone();
      if (!sin.hasExitFramework(SpecialtyLayoutLinkData.INIT_TRACE)) {
        sin.setExitFramework(new SpecialtyLayoutLinkData.PlacedPoint(initStack, false), SpecialtyLayoutLinkData.INIT_TRACE);
      }
    }
    return;
  }
 
  /***************************************************************************
  ** 
  ** Do final inbound link routing
  */

  public boolean finishDeferredInbounds(String srcID, List<GeneAndSatelliteCluster.InboundCorners> perClust,
                                        SpecialtyLayoutEngine.NodePlaceSupport nps,
                                        DataAccessContext irx, 
                                        Set<String> skipLinks,
                                        SpecialtyLayoutLinkData sin,
                                        Map<String, Integer> inboundTraces, double traceDistance, Point2D base,
                                        boolean needMainDrop, Point2D lastDrop) {
    int pcNum = perClust.size();
    for (int i = 0; i < pcNum; i++) {
      // Ordering in perClust is important since the sin.startNewLink() order is crucial!
      GeneAndSatelliteCluster.InboundCorners ic = perClust.get(i);
      // A link coming back in for feedback should not be adding points
      // onto already laid out forward links:
      if (skipLinks.contains(ic.linkID)) {
        continue;
      }
      sin.startNewLink(ic.linkID);   
      if (ic.needDrop) {
        if (needMainDrop) {
          Integer inTrace = inboundTraces.get(srcID);
          double traceColVal = inTrace.doubleValue();
          double dropX = base.getX() + (traceColVal * traceDistance);
          Point2D hotTip = getFeedbackHotTip();
          if (hotTip == null) {
            startFeedbackBranch(sin, ic.linkID, nps, irx);
          } else {
            sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
          }
          Point2D topDrop = new Point2D.Double(dropX, getFeedTrace().getY());
          lastDrop.setLocation(topDrop);         
          updateFeedbackHotTip((Point2D)topDrop.clone());
          needMainDrop = false;
        }
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)lastDrop.clone()));
        Point2D drop = new Point2D.Double(lastDrop.getX(), ic.dropY);
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(drop));
        lastDrop.setLocation(drop);
      } else if (ic.prevPoint != null) {
        sin.addPositionToLink(ic.linkID, ic.prevPoint);
      }
      sin.addPositionListToLink(ic.linkID, ic.finalPath);
    }
    
    return (needMainDrop);
  }
  
  /***************************************************************************
  ** 
  ** Do final inbound link routing
  */

  public void finishDeferredStackedInbounds(String srcID, List<GeneAndSatelliteCluster.InboundCorners> perClust,
                                            SpecialtyLayoutEngine.NodePlaceSupport nps,
                                            DataAccessContext irx,
                                            Set<String> skipLinks,
                                            SpecialtyLayoutLinkData sin,
                                            Point2D lastDrop) {
    int pcNum = perClust.size();
    for (int i = 0; i < pcNum; i++) {
      GeneAndSatelliteCluster.InboundCorners ic = perClust.get(i);
      // A link coming back in for feedback should not be adding points
      // onto already laid out forward links:
      if (skipLinks.contains(ic.linkID)) {
        continue;
      }
      // Note ordering is important, since the sin.startNewLink() order is crucial!
      sin.startNewLink(ic.linkID);
      // guys inside a fan with multiple inputs do not need a drop!
      if (ic.needDrop) {
        finishStackedDeferred(ic, sin, nps, irx, lastDrop);
      }
      if (ic.prevPoint != null) {
        sin.addPositionToLink(ic.linkID, ic.prevPoint);
      }
      sin.addPositionListToLink(ic.linkID, ic.finalPath);
    }  
    return;
  }
  
  /***************************************************************************
  ** 
  ** Do final deferred routing for stacked clusters
  */

  private void finishStackedDeferred(GeneAndSatelliteCluster.InboundCorners ic, 
                                     SpecialtyLayoutLinkData sin,
                                     SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     DataAccessContext irx,
                                     Point2D lastDrop) {
       
    //
    // Stacked clusters do not use the separate single series feedback 
    // link.  Just the usual outbound branch.  If the hot tip is not
    // begun, start it.
    // If we are not in the right row, then get there.
    //
    
    int cs = getCurrStackRow();
    int so = getStackOriginRow();
    double dropX = ic.finalPath.get(0).getPoint().getX(); 
     
    Point2D hotTip = getMainHotTip();   
    Point2D feedHotTip = getFeedbackHotTip();   
    if ((hotTip == null) && (feedHotTip == null)) { // No starting infrastructure:
      // After this call, complex fan out points have been added to the link path:
      startOutboundBranch(sin, ic.linkID, nps, irx);
      if (cs == so) { // First point going into same row
        Point2D ft = getCurrentStackedTrace();
        // Pure feedback clusters generate no trace, but do generate a feedTrace.
        // Use that if we need to:
        Point2D out = getTrace();
        Point2D feed = getFeedTrace();
        Point2D startDrop;
        if ((out == null) && (feed != null)) {
          startDrop = new Point2D.Double(feed.getX(), ft.getY());
          sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(startDrop));
        } else {
          startDrop = new Point2D.Double(out.getX(), ft.getY());       
        }
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(startDrop));       
        Point2D nextDrop = new Point2D.Double(dropX, ft.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(nextDrop));
        lastDrop.setLocation(nextDrop);
        // Moving right....
        if (dropX > startDrop.getX()) {
          updateFeedbackHotTip((Point2D)startDrop.clone());
          updateMainHotTip((Point2D)lastDrop.clone());
        } else { // Moving left....
          updateFeedbackHotTip((Point2D)lastDrop.clone());
          updateMainHotTip((Point2D)startDrop.clone());
        }
        return;       
      } else { // First point into another row:
        Point2D currStack = getInitialStackedTrace();
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(currStack));
        updateHotStackRow();
        currStack = getCurrentStackedTrace();
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(currStack));
        Point2D topDrop = new Point2D.Double(dropX, currStack.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
        lastDrop.setLocation(topDrop);
        updateMainHotTip((Point2D)topDrop.clone());      
        return;
      }
    // If we are still in the original row, we either like the forward
    // tip or the backward tip:
    } else if (cs == so) {
      if (dropX > hotTip.getX()) {
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip)); 
        Point2D topDrop = new Point2D.Double(dropX, hotTip.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
        lastDrop.setLocation(topDrop);
        updateMainHotTip((Point2D)topDrop.clone());
      } else {
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(feedHotTip)); 
        Point2D topDrop = new Point2D.Double(dropX, feedHotTip.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
        lastDrop.setLocation(topDrop);
        updateFeedbackHotTip((Point2D)topDrop.clone());
      }
      return;
    }
  
    //
    // Dealing with remote rows following init:
    //
    
    Integer hotRow = getHotStackRow();
    if (hotRow != null) {
      if (hotRow.intValue() == cs) {
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
        Point2D topDrop = new Point2D.Double(dropX, hotTip.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
        lastDrop.setLocation(topDrop);
        updateFeedbackHotTip((Point2D)topDrop.clone());
        updateMainHotTip((Point2D)topDrop.clone());
        return;
      } else {
        Point2D initStack = getInitialStackedTrace();
        Point2D currStack = getCurrentStackedTrace();
        Point2D lastStack = getLastStackedTrace();
        // If now heading down and changing direction...
        if (currStack.getY() > initStack.getY()) { // heading down...
          if (lastStack.getY() < initStack.getY()) { // changing direction!
            resetHotStackRowToOrigin();
            lastStack = getLastStackedTrace();
          }
        }        
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(lastStack));
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(currStack));
        Point2D topDrop = new Point2D.Double(dropX, currStack.getY());
        sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
        lastDrop.setLocation(topDrop);
        updateMainHotTip((Point2D)topDrop.clone());
        updateHotStackRow();
        return;
      } 
    } else {
      // Since we are leaving, note we need to use the FEED hotTip!
      sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(feedHotTip));
      Point2D currStack = getInitialStackedTrace();
      sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(currStack));
      currStack = getCurrentStackedTrace();
      sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(currStack));
      Point2D topDrop = new Point2D.Double(dropX, currStack.getY());
      sin.addPositionToLink(ic.linkID, new SpecialtyLayoutLinkData.TrackPos(topDrop));
      lastDrop.setLocation(topDrop);
      updateMainHotTip((Point2D)topDrop.clone());
      updateHotStackRow();
    }
    return;        
  } 
}

