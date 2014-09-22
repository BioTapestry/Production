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

import java.util.Map;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** This class handles details of extracting point locations for
** e.g. fan-in and fan-outs
*/

public class MetaClusterPointSource {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int FOR_PURE_TARG   = 0;
  public static final int FOR_INBOUND_SRC = 1;    
  public static final int FOR_INTRA_SRC   = 2;
  public static final int FOR_ALWAYS_BELOW  = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Point2D dropForRow_;
  private boolean srcIsCarriedOver_; //  target usage
  private boolean dropIsCarriedDown_; //  target usage
  private boolean startForSource_;

  private SuperSrcRouterPointSource lmp_;
  private int type_;
  private double targetTraceY_;
  private double traceDistance_;
  private Map<String, Integer> inboundTraces_; 
  private Point2D base_;
  private Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> inboundsPerSrc_;
  private boolean abNeedsHoriz_;
  private boolean deferredPlacementOnly_;
  private String srcID_;
  private boolean needsTraceYUpdate_;
  private Point2D lastLeftPoint_;
  private Point2D targetDropRoot_;
  private Point2D targetRowDrop_;
  private boolean singleRow_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */

  public MetaClusterPointSource(String srcID, SuperSrcRouterPointSource lmp, int usage) {
    srcID_ = srcID;
    lmp_ = lmp;
    type_ = usage;
    deferredPlacementOnly_ = false;
    singleRow_ = false;
    
    if (usage == FOR_INTRA_SRC) {
      startForSource_ = true;
      dropForRow_ = null;
      lastLeftPoint_ = null;
      needsTraceYUpdate_ = true;
    } else if (usage == FOR_INBOUND_SRC) {
      startForSource_ = true;
      dropForRow_ = null;
      lastLeftPoint_ = null;
      needsTraceYUpdate_ = true;
    } else if (usage == FOR_PURE_TARG) {
      targetDropRoot_ = null;
      targetRowDrop_ = null;
      lastLeftPoint_ = null; 
      srcIsCarriedOver_ = false;
      needsTraceYUpdate_ = false;
    } else if (usage == FOR_ALWAYS_BELOW) {
      startForSource_ = true;
      dropForRow_ = null;
      lastLeftPoint_ = null;
      needsTraceYUpdate_ = false;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** A function
  */
  
  public void closeOutComplexCluster(SpecialtyLayoutLinkData.TrackPos finalForClust) { 
    if (type_ == FOR_PURE_TARG) { 
      lastLeftPoint_ = (Point2D)finalForClust.getPoint().clone();
    } else {
      lastLeftPoint_ = null;
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** A function
  */
  
  public void updateDropPoint(Point2D finalInbound) { 
    dropForRow_ = (Point2D)finalInbound.clone();
    return;   
  }
  
  /***************************************************************************
  **
  ** A function
  */
  
  public void updateRightmostDropPoint(Point2D finalRightmost) { 
    lastLeftPoint_ = (Point2D)finalRightmost.clone();
    return;   
  }
   
  /***************************************************************************
  **
  ** Answers if the bus feed proceeds horizontally (targs) or vertically (srces);
  */
  
  public boolean busFeedIsHorizontal() {
    return ((type_ == FOR_PURE_TARG) || ((type_ == FOR_ALWAYS_BELOW) && !abNeedsHoriz_));
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public int getType() {
    return (type_);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getComplexInterfacePoint() {
    return ((type_ == FOR_PURE_TARG) ? (Point2D)targetRowDrop_.clone() : (Point2D)dropForRow_.clone());
  }
  
  /***************************************************************************
  **
  ** Pre path for simple or no-fan blocks:
  */
 
  public void buildPrePath(SpecialtyLayoutLinkData sin, String linkID, 
                           Point2D lastPoint, boolean firstForClust, 
                           SpecialtyLayoutEngine.NodePlaceSupport nps, 
                           DataAccessContext rcx,
                           boolean extendingVertically, boolean firstFromTop) {
    switch (type_) {
      case FOR_PURE_TARG:
        doPureTargetTipAccounting(sin, linkID, lastPoint.getX(), nps, rcx);
        return;
      case FOR_INBOUND_SRC:
      case FOR_INTRA_SRC:
        doInboundTipAccounting(sin, linkID, lastPoint, firstForClust, nps, rcx, extendingVertically, firstFromTop);
        return;
      case FOR_ALWAYS_BELOW:  // Always uses deferred....
      default:
        throw new IllegalStateException();
    }
  } 
    
  /***************************************************************************
  **
  ** Pre path for complex fan blocks:
  */
  
  public void fanBlockBuildPrePath(SpecialtyLayoutLinkData sin, String linkID, 
                                   Point2D lastPoint, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                   DataAccessContext rcx) {
    switch (type_) {
      case FOR_PURE_TARG:       
        doPureTargetTipAccounting(sin, linkID, lastPoint.getX(), nps, rcx);
        return;
      case FOR_INBOUND_SRC:
      case FOR_INTRA_SRC:
        doInboundTipAccountingForBlock(sin, linkID, lastPoint, nps, rcx);
        return;
      case FOR_ALWAYS_BELOW:   // Always uses deferred....
      default:
        throw new IllegalStateException();
    }
  }   
  
  /***************************************************************************
  **
  ** A function
  */
  
  public double getVerticalDropX(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext rcx) {
    switch (type_) {
      case FOR_INBOUND_SRC:
        Point2D icDrop = getFirstDropFromTraces();
        return (icDrop.getX());
      case FOR_INTRA_SRC:
        Point2D intBr = lmp_.getInternalBranch(nps, rcx);
        return ((intBr != null) ? intBr.getX() : 0.0);
      case FOR_ALWAYS_BELOW:
        icDrop = getFirstDropFromTraces();
        return (icDrop.getX());
      case FOR_PURE_TARG:     
      default:       
        throw new IllegalStateException();
    }
  }  
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getTargetDropRoot() {  
    return (targetDropRoot_);
  } 
    
  /***************************************************************************
  **
  ** A function
  */
    
  public void initializeTargetDropRoot(Point2D pt) {  
    targetDropRoot_ = (Point2D)pt.clone();
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void initTargetDropRow(double y, boolean singleRow) { 
    targetRowDrop_ = new Point2D.Double(targetDropRoot_.getX(), y);
    singleRow_ = singleRow;
    dropIsCarriedDown_ = false;
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void closeOutTargetDropRow() {
    if (dropIsCarriedDown_) {
      lmp_.updateMainHotTip((Point2D)targetRowDrop_.clone());
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** A function
  */
  
  public void updateMainHotTip(Point2D pt) {
    lmp_.updateMainHotTip(pt);
    return;
  }
   
  /***************************************************************************
  **
  ** A function
  */
  
  public double getTargetTraceY() {
    return (targetTraceY_);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void setTargetTraceY(double y) {
    targetTraceY_ = y;
    return;
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean needsTraceYUpdate() {
    return (needsTraceYUpdate_);
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public double getTraceDistance() {
    return (traceDistance_);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean hasInboundTraces() {
    return (inboundTraces_ != null);
  }
  
  /***************************************************************************
  **
  ** A function
  */
  
  public Map<String, Integer> getInboundTraces() {
    return (inboundTraces_);
  }
  
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getFirstDropFromTraces() {
    Integer inTrace = inboundTraces_.get(srcID_);
    double traceColVal = inTrace.doubleValue();
    double dropX = base_.getX() + (traceColVal * traceDistance_);
    return (new Point2D.Double(dropX, targetTraceY_));
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void setInboundParams(double traceDistance, Map<String, Integer> inboundTraces, 
                               Point2D base, Map<String, 
                               List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> inboundsPerSrc) {
    traceDistance_ = traceDistance;
    inboundTraces_ = inboundTraces; 
    if (UiUtil.forceToGridValue(base.getX(), UiUtil.GRID_SIZE) != base.getX()) {
      throw new IllegalArgumentException();
    }
    base_ = base;
    inboundsPerSrc_ = inboundsPerSrc;    
    return;
  }  
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void setStackedParams(double traceDistance, Point2D base, 
                               Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> inboundsPerSrc, 
                               Map<String, Integer> inboundTraces) { //, boolean needsHoriz) {
    if (UiUtil.forceToGridValue(base.getX(), UiUtil.GRID_SIZE) != base.getX()) {
      throw new IllegalArgumentException();
    }
    traceDistance_ = traceDistance;
    base_ = base;
    inboundsPerSrc_ = inboundsPerSrc;
    inboundTraces_ = inboundTraces;
    abNeedsHoriz_ = true; //!needsHoriz;
    needsTraceYUpdate_ = abNeedsHoriz_;    
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public void setForDeferredPlacementOnly(boolean deferred) {
    deferredPlacementOnly_ = deferred;
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public boolean isForDeferredOnly() {
    return (deferredPlacementOnly_);
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners> initInboundsPerSrcForDeferred(String coreID, String srcID) {
    if (!deferredPlacementOnly_) {
      throw new IllegalStateException();
    }
    List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>> inbounds = inboundsPerSrc_.get(srcID);
    if (inbounds == null) {
      inbounds = new ArrayList<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>();
      inboundsPerSrc_.put(srcID, inbounds);
    }
    GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners> perClust = 
      new GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>(coreID);
    inbounds.add(perClust);
    return (perClust);
  } 
  
  /***************************************************************************
  **
  ** A function
  */
  
  public void setIntraParams(double traceDistance) {
    traceDistance_ = traceDistance;
    inboundTraces_ = null; 
    base_ = null;
    inboundsPerSrc_ = null;    
    return;
  }      
  
  /***************************************************************************
  **
  ** A function
  */
  
  private void doPureTargetTipAccounting(SpecialtyLayoutLinkData sin, String linkID, double lastX, 
                                         SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext rcx) {  
    Point2D hotTip = lmp_.getMainHotTip();
    if (hotTip == null) {
      lmp_.startOutboundBranch(sin, linkID, nps, rcx);
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(targetDropRoot_));
      if (!singleRow_) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(targetRowDrop_));
      }
      lmp_.updateMainHotTip((Point2D)targetRowDrop_.clone());
      srcIsCarriedOver_ = true;
      dropIsCarriedDown_ = true;
    } else if (!srcIsCarriedOver_) {    
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(targetDropRoot_));
      if (!singleRow_) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(targetRowDrop_));
      }
      lmp_.updateMainHotTip((Point2D)targetRowDrop_.clone());
      srcIsCarriedOver_ = true;
      dropIsCarriedDown_ = true;
    } else if (!dropIsCarriedDown_) {
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
      if (!singleRow_) {
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(targetRowDrop_));
      }
      lmp_.updateMainHotTip((Point2D)targetRowDrop_.clone());
      dropIsCarriedDown_ = true;
    } else if (lastLeftPoint_ != null) {
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)lastLeftPoint_.clone()));
    }
    lastLeftPoint_ = new Point2D.Double(lastX, targetRowDrop_.getY());
    return;
  }    
  
  /***************************************************************************
  **
  ** A function
  */
  
  public void closeOutCluster() {
    if (type_ != FOR_PURE_TARG) {
      lastLeftPoint_ = null;
    }
    return;
  } 
    
  /***************************************************************************
  **
  ** A function
  */
  
  public Point2D getLastLeftPoint() {
    return (lastLeftPoint_);
  }
    
  /***************************************************************************
  **
  ** A function
  */
  
  private void establishTopDrop(SpecialtyLayoutLinkData sin, String linkID, Point2D lastCoord, 
                                SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                DataAccessContext rcx, boolean extendingVertically) {
    Point2D hotTip = lmp_.getMainHotTip();
    if (hotTip == null) {
      lmp_.startOutboundBranch(sin, linkID, nps, rcx);
    } else {
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(hotTip));
    }
    Point2D icDrop = getFirstDropFromTraces();
    //Point2D lasPre = ((LinkRouter.TrackPos)(prePath.get(prePath.size() - 1))).getPoint();
    //if (icDrop.getX() < lasPre.getX()) {
    //  System.err.println("firstDrop is backwards");
    //}
    Point2D topDrop = new Point2D.Double(icDrop.getX(), lmp_.getTrace().getY());
    sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)topDrop.clone()));
    lmp_.updateMainHotTip((Point2D)topDrop.clone());
    boolean invert = (icDrop.getY() < topDrop.getY());
    if (extendingVertically && invert) {
      dropForRow_ = (Point2D)topDrop.clone();  // Kinda a hack; needs to be ~at the bottom of the target.
    } else {
      dropForRow_ = (Point2D)icDrop.clone();      
    }
    sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
    if (extendingVertically) {
      lastLeftPoint_ = new Point2D.Double(dropForRow_.getX(), lastCoord.getY());
      dropForRow_ = (Point2D)lastLeftPoint_.clone();
    } else {
      lastLeftPoint_ = new Point2D.Double(lastCoord.getX(), dropForRow_.getY());
    }
    return;   
  }
    
  /***************************************************************************
  **
  ** Handles building an internal branch (src is in same supercluster)
  */
 
  private void establishInternalBranch(SpecialtyLayoutLinkData sin, String linkID, Point2D lastCoord, 
                                       SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                       DataAccessContext rcx, boolean extendingVertically) {
    lmp_.startInternalBranch(sin, linkID, nps, rcx);
    Point2D internalBranch = lmp_.getInternalBranch(nps, rcx);
    sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)internalBranch.clone()));
    if (!extendingVertically) {
      dropForRow_ = new Point2D.Double(internalBranch.getX(), targetTraceY_);
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = new Point2D.Double(lastCoord.getX(), dropForRow_.getY());
    } else {
      dropForRow_ = new Point2D.Double(internalBranch.getX(), lastCoord.getY());  
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = (Point2D)dropForRow_.clone();      
    }
    return;
  }      
  
  /***************************************************************************
  **
  ** Get the inbound tip for a new cluster
  */
  
  private void establishInboundTipForAddedCluster(SpecialtyLayoutLinkData sin, String linkID, 
                                                  Point2D newTipPt,
                                                  SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                  DataAccessContext rcx, 
                                                  boolean extendingVertically, boolean firstFromTop) {
    sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
    Point2D internalBranch = lmp_.getInternalBranch(nps, rcx);
    double dropX = ((type_ == FOR_INBOUND_SRC) || ((type_ == FOR_ALWAYS_BELOW) && abNeedsHoriz_)) ? getFirstDropFromTraces().getX() : internalBranch.getX();
    if (extendingVertically) {
      dropForRow_ = new Point2D.Double(dropX, newTipPt.getY()); 
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = (Point2D)dropForRow_.clone();
    } else if (firstFromTop && (targetTraceY_ != newTipPt.getY())) {
      // if we have a fan out, we will need to bring down a drop point from the top
      // even if not doing vertical extension
      //  if (entryFromTop) {
      //    prePath.add(new LinkRouter.TrackPos((Point2D)dropForRow_.clone()));
      //  }
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      dropForRow_ = new Point2D.Double(dropX, newTipPt.getY());
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = new Point2D.Double(newTipPt.getX(), newTipPt.getY());
    } else {
      dropForRow_ = new Point2D.Double(dropX, targetTraceY_);
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = new Point2D.Double(newTipPt.getX(), targetTraceY_);
    }     
 
    return;   
  } 
    
  /***************************************************************************
  **
  ** For regular fan-ins, this just involves extending the lastLeftPoint over
  ** for each added link.  The row drop stays the same.  For simple fan-ins,
  ** we need to grow the row drop, and not move right (unless the link is
  ** direct, and not into a fan-in node):
  */
    
  private void extendInboundTipForCluster(SpecialtyLayoutLinkData sin, String linkID, Point2D newTipPt, boolean extendingVertically) { 
    if (extendingVertically) { // Adding vertically
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)dropForRow_.clone()));
      lastLeftPoint_ = new Point2D.Double(lastLeftPoint_.getX(), newTipPt.getY());
      dropForRow_ = (Point2D)lastLeftPoint_.clone();
    } else { // Adding horizontally
      if (lastLeftPoint_ == null) { 
        throw new IllegalStateException();
      }  
       sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)lastLeftPoint_.clone()));
      lastLeftPoint_ = new Point2D.Double(newTipPt.getX(), lastLeftPoint_.getY());
    }
    sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)lastLeftPoint_.clone()));
    return;   
  } 
    
  /***************************************************************************
  **
  ** This handles routing for non-block inbounds (simple and no fan)
  */
  
  private void doInboundTipAccounting(SpecialtyLayoutLinkData sin, String linkID, 
                                      Point2D lastPoint, boolean firstForClust, 
                                      SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                      DataAccessContext rcx,
                                      boolean extendingVertically, boolean firstFromTop) { 
    
    if (type_ == FOR_PURE_TARG) {
      throw new IllegalStateException();
    }
    
    //
    // First time for source.  Either need to get main bus established or the internal branch:
    //
    
    if (startForSource_) {
      if ((type_ == FOR_INBOUND_SRC) || ((type_ == FOR_ALWAYS_BELOW) && abNeedsHoriz_)) {
        establishTopDrop(sin, linkID, lastPoint, nps, rcx, extendingVertically);
      } else {
        establishInternalBranch(sin, linkID, lastPoint, nps, rcx, extendingVertically);
      }  
      startForSource_ = false;
      
    //
    // New cluster
    //
      
    } else if (firstForClust) {
      establishInboundTipForAddedCluster(sin, linkID, lastPoint, nps, rcx, extendingVertically, firstFromTop);
    //
    // Keep working on a cluster, moving right (or vertically)
    //
    
    } else {
      extendInboundTipForCluster(sin, linkID, lastPoint, extendingVertically);
    }
    return;
  } 
    
  /***************************************************************************
  **
  ** This handles routing from vertical traces
  */
 
  private void doInboundTipAccountingForBlock(SpecialtyLayoutLinkData sin, String linkID, 
                                              Point2D lastPoint, 
                                              SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                              DataAccessContext rcx) { 
    
    if (type_ == FOR_PURE_TARG) {
      throw new IllegalStateException();
    }
    
    //
    // First time for source.  Either need to get main bus established or the internal branch:
    //
    
    if (startForSource_) {
      if ((type_ == FOR_INBOUND_SRC) || ((type_ == FOR_ALWAYS_BELOW) && abNeedsHoriz_)) {
        establishTopDrop(sin, linkID, lastPoint, nps, rcx, false);
      } else {
        establishInternalBranch(sin, linkID, lastPoint, nps, rcx, false);
      }
      startForSource_ = false;
      
    //
    // New cluster
    //
      
    } else {
      establishInboundTipForAddedCluster(sin, linkID, lastPoint, nps, rcx, false, false);
    }
    return;
  }   
}
 