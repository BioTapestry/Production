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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Bounds;

/****************************************************************************
**
** Data for laying out links produced by Specialty Layouts
*/

public class SpecialtyLayoutLinkData implements Cloneable {
  
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
  
  public final static int INIT_POINT  = 0;
  public final static int RIGHT_TIP   = 1;
  public final static int LEFT_TIP    = 2;
  public final static int INIT_TRACE  = 3;
  public final static int MIN_TRACE   = 4;
  public final static int MAX_TRACE   = 5;
  public final static int NUM_EXIT_FRAMEWORK = 6;

  public final static int TOP_BORDER    = 0;
  public final static int BOTTOM_BORDER = 1;
  public final static int LEFT_BORDER   = 2;
  public final static int RIGHT_BORDER  = 3;
  public final static int NUM_BORDERS   = 4;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
      
  private NoZeroList initRoute_;
  private HashMap<Integer, Point2D> borderPoints_;
  private HashMap<Integer, PlacedPoint> exitFramework_;
  private String srcID_;
  private HashMap<Point2D, Integer> chopPtToBoundary_;

  private ArrayList<String> linkIDs_;
  private HashMap<String, NoZeroList> pointsPerLink_;
  private HashSet<String> needsExitGlue_; 
  private boolean normalized_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public SpecialtyLayoutLinkData(String srcID) {
    linkIDs_ = new ArrayList<String>();
    pointsPerLink_ = new HashMap<String, NoZeroList>();
    borderPoints_ = new HashMap<Integer, Point2D>();
    exitFramework_ = new HashMap<Integer, PlacedPoint>();
    needsExitGlue_ = new HashSet<String>();
    chopPtToBoundary_ = new HashMap<Point2D, Integer>();
    initRoute_ = null;
    normalized_ = false;
    srcID_ = srcID;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Clone
  */  
  
  @Override
  public SpecialtyLayoutLinkData clone() {
    try {
      SpecialtyLayoutLinkData retval = (SpecialtyLayoutLinkData)super.clone();
      retval.linkIDs_ = new ArrayList<String>(this.linkIDs_);
      retval.needsExitGlue_ = new HashSet<String>(this.needsExitGlue_);
      retval.pointsPerLink_ = new HashMap<String, NoZeroList>();

      retval.initRoute_ = (this.initRoute_ != null) ? new NoZeroList() : null;
      retval.borderPoints_ = new HashMap<Integer, Point2D>();
      retval.exitFramework_ = new HashMap<Integer, PlacedPoint>();
      retval.chopPtToBoundary_ = new HashMap<Point2D, Integer> ();

      Iterator<String> pplit = this.pointsPerLink_.keySet().iterator();
      while (pplit.hasNext()) {
        String linkID = pplit.next();
        NoZeroList thisPtsPerLink = this.pointsPerLink_.get(linkID);
        NoZeroList retvalPtsPerLink = new NoZeroList();
        retval.pointsPerLink_.put(linkID, retvalPtsPerLink);
        int numL = thisPtsPerLink.size();
        for (int i = 0; i < numL; i++) {
          TrackPos tp = thisPtsPerLink.get(i);
          retvalPtsPerLink.add(tp.clone());
        }
      }

      if (retval.initRoute_ != null) {
        int numL = this.initRoute_.size();
        for (int i = 0; i < numL; i++) {
          TrackPos tp = (TrackPos)this.initRoute_.get(i);
          retval.initRoute_.add(tp.clone());
        }
      }

      Iterator<Integer> efkit = this.exitFramework_.keySet().iterator();
      while (efkit.hasNext()) {
        Integer role = efkit.next();
        PlacedPoint thisPP = this.exitFramework_.get(role);
        retval.exitFramework_.put(role, thisPP.clone());      
      }

      Iterator<Integer> epkit = this.borderPoints_.keySet().iterator();
      while (epkit.hasNext()) {
        Integer role = epkit.next();
        Point2D thisPt = this.borderPoints_.get(role);
        retval.borderPoints_.put(role, (Point2D)thisPt.clone());      
      }
      
      Iterator<Point2D> cpit = this.chopPtToBoundary_.keySet().iterator();
      while (cpit.hasNext()) {
        Point2D pt = cpit.next();
        Integer boundForChop = chopPtToBoundary_.get(pt);
        Point2D ptCopy = (Point2D)pt.clone();
        retval.chopPtToBoundary_.put(ptCopy, boundForChop);
      }

      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }    

  public void setNeedsExitGlue(String linkID) {
    needsExitGlue_.add(linkID);
    return;
  }   
  
  public String getSrcID() {
    return (srcID_);
  }
 
  public int numLinks() {
    return ((linkIDs_ == null) ? 0 : linkIDs_.size());
  }   
  
  public String getLink(int i) {
    return (linkIDs_.get(i));
  } 
  
  public boolean haveLink(String linkID) {
    return (linkIDs_.contains(linkID));
  }  

  public void setExitFramework(PlacedPoint pt, int role) {
    exitFramework_.put(new Integer(role), pt);
    return;
  } 
  
  public boolean hasExitFramework(int role) {
    return (exitFramework_.containsKey(new Integer(role)));
  }
  
  public PlacedPoint getExitFrameworkForDebug(int i) {
    return (exitFramework_.get(new Integer(i)));
  }
 
  public List<String> getLinkList() {
    return (linkIDs_);
  }     

  public NoZeroList getPositionList(String linkID) {
    return (pointsPerLink_.get(linkID));
  }  

  public NoZeroList getCleanedPositionList(String linkID) {
    if (!normalized_) {
      throw new IllegalStateException();
    }
    return (pointsPerLink_.get(linkID));
  } 

  public void shift(Vector2D shiftVec) {
    Iterator<NoZeroList> pplit = pointsPerLink_.values().iterator();
    while (pplit.hasNext()) {
      NoZeroList ptsList = pplit.next();
      int numL = ptsList.size();
      for (int i = 0; i < numL; i++) {
        TrackPos tp = ptsList.get(i);
        if (!tp.needsConversion()) {
          Point2D segPt = tp.getPoint();
          segPt.setLocation(shiftVec.add(segPt));
        }
      }
    }
    if (initRoute_ != null) {
      Iterator<TrackPos> irit = initRoute_.iterator();
      while (irit.hasNext()) {
        TrackPos tp = irit.next();
        if (!tp.needsConversion()) {
          Point2D segPt = tp.getPoint();
          segPt.setLocation(shiftVec.add(segPt));
        }
      }
    } 

    Iterator<Point2D> epit = borderPoints_.values().iterator();
    while (epit.hasNext()) {
      Point2D pt = epit.next();
      pt.setLocation(shiftVec.add(pt));
    }

    Iterator<PlacedPoint> efit = exitFramework_.values().iterator();
    while (efit.hasNext()) {
      PlacedPoint pt = efit.next();
      pt.point.setLocation(shiftVec.add(pt.point));
    }
    
    Iterator<Point2D> cpit = chopPtToBoundary_.keySet().iterator();
    HashMap<Point2D, Integer> newMap = new HashMap<Point2D, Integer>();
    while (cpit.hasNext()) {
      Point2D pt = cpit.next();
      Integer boundForChop = chopPtToBoundary_.get(pt);
      Point2D newKey = (Point2D)pt.clone();
      newKey.setLocation(shiftVec.add(pt));
      newMap.put(newKey, boundForChop);
    }
    chopPtToBoundary_.clear();
    chopPtToBoundary_.putAll(newMap);
    LayoutFailureTracker.recordShiftedBordersAndExits(this, shiftVec);
    return;
  }

  public boolean yesWeNeedIt(Integer side, Map<String, Set<Vector2D>> needArrivals, Map<String, Set<Vector2D>> needDepartures) {
    if ((needArrivals == null) || (needDepartures == null)) {
      return (true);
    }
    Set<Vector2D> forArr = needArrivals.get(srcID_);
    Set<Vector2D> forDep = needDepartures.get(srcID_);
    if (forArr == null) {
      forArr = new HashSet<Vector2D>();
    }
    if (forDep == null) {
      forDep = new HashSet<Vector2D>();
    }
    
    //
    // If something departs/arrives from that side, we need it!
    //
    switch (side.intValue()) {
      case TOP_BORDER:
        return ((forDep.contains(new Vector2D(0.0, 1.0)) || forArr.contains(new Vector2D(0.0, 1.0))));
      case BOTTOM_BORDER:
        return ((forDep.contains(new Vector2D(0.0, -1.0)) || forArr.contains(new Vector2D(0.0, -1.0))));
      case LEFT_BORDER:
        return ((forDep.contains(new Vector2D(1.0, 0.0)) || forArr.contains(new Vector2D(1.0, 0.0))));
      case RIGHT_BORDER:
        return ((forDep.contains(new Vector2D(-1.0, 0.0)) || forArr.contains(new Vector2D(-1.0, 0.0))));
      default:
        throw new IllegalArgumentException();
    }
  }
  
  public static int vecToBorder(Vector2D vec) {    
    if (vec.equals(new Vector2D(0.0, 1.0))) {
      return (TOP_BORDER);
    } else if (vec.equals(new Vector2D(0.0, -1.0))) {
      return (BOTTOM_BORDER);
    } else if (vec.equals(new Vector2D(1.0, 0.0))) {
      return (LEFT_BORDER);
    } else if (vec.equals(new Vector2D(-1.0, 0.0))) {
      return (RIGHT_BORDER);
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  public Rectangle2D getLinkBounds(Map<String, Set<Vector2D>> needArrivals, Map<String, Set<Vector2D>> needDepartures) {
    Rectangle2D retval = null;
    
    // If we are dealing with the source set, we need to look at the
    // initial route, not the fake pointlist!
    if (initRoute_ != null) {
      Iterator<TrackPos> pplit = initRoute_.iterator();
      TrackPos tp = pplit.next();
      Point2D segPt = tp.getPoint();
      if (segPt == null) {
        throw new IllegalStateException();
      }
      retval = Bounds.initBoundsWithPoint(segPt);
    } else {
      Iterator<NoZeroList> pplit = pointsPerLink_.values().iterator();
      while (pplit.hasNext()) {
        NoZeroList ptsList = pplit.next();
        int numL = ptsList.size();
        for (int i = 0; i < numL; i++) {
          TrackPos tp = ptsList.get(i);
          Point2D segPt = tp.getPoint();
          if (segPt == null) {
            throw new IllegalStateException();
          }
          if (retval == null) {
            retval = Bounds.initBoundsWithPoint(segPt);
          } else {
            Bounds.tweakBoundsWithPoint(retval, segPt);
          }
        }
      }
    }
   
    
    Iterator<Integer> efit = borderPoints_.keySet().iterator();
    while (efit.hasNext()) {
      Integer side = efit.next();    
      if (!yesWeNeedIt(side, needArrivals, needDepartures)) {
        continue;
      }     
      Point2D pt = borderPoints_.get(side);
      if (retval == null) {
        retval = Bounds.initBoundsWithPoint(pt);
      } else {
        Bounds.tweakBoundsWithPoint(retval, pt);
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Faced with external sources routing into genome subsets, we wish to retain those
  ** link pieces up to the boundaries of the subsets for later reassembly.
  ** This original approach was designed to work with MULTIPLE subsets that needed
  ** geometry recoveries.  With V6 release only supporting single-subset approaches
  ** in the absence of a full-overlay solution that rebuilds all inter-subset links,
  ** we came up with a less general approach below.  That said, this approach needs
  ** work to get it working correctly.
  **

  
  public void extractTreeTrunkRemainsOrigSortaWorked(BusProperties bp, Set links, Rectangle chopRect, Map segGeoms, Set seenSegs) {
    Iterator dit = bp.getDrops();
    ArrayList linkIDCandidates = new ArrayList();
    HashMap candPointsPerLink = new HashMap();
    while (dit.hasNext()) {
      BusDrop drop = (BusDrop)dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      ArrayList ptsToRoot = new ArrayList();
      String linkID = drop.getTargetRef();
      if (!links.contains(linkID)) {
        continue;
      }
      // This list SKIPS the root segment!
      List segIDs = bp.getSegmentIDsToRootForEndDrop(drop);
      boolean alwaysAdd = false;
      Iterator sit = segIDs.iterator();
      while (sit.hasNext()) {
        LinkSegmentID lsid = (LinkSegmentID)sit.next();
        if (lsid.isForEndDrop()) {
          LinkSegment fakeSeg = (LinkSegment)segGeoms.get(lsid);
          Point2D end = fakeSeg.getEnd();   
          Point2D start = fakeSeg.getStart();
          if (chopRect.contains(end) && chopRect.contains(start)) {
            continue;
          }
          UiUtil.PointAndSide pasIntersect = UiUtil.rectIntersection(chopRect, start, end);
          if (pasIntersect != null) {
            ptsToRoot.add(new TrackPos(pasIntersect.point));
            alwaysAdd = true;
            chopPtToBoundary_.put(pasIntersect.point.clone(), new Integer(rectSideToOurBorder(pasIntersect.side)));
            // Lack of adding to seenSegs will create link overlaps to start point
            // added in next pass.  BUT chopRect/intersection may be different 
            // for different subsets!  FIXME
          }
        } else if (lsid.isForStartDrop()) {
          LinkSegmentID rootID = bp.getRootSegmentID();
          Point2D end = bp.getSegment(rootID).getStart();
          ptsToRoot.add(new TrackPos((Point2D)end.clone()));
          seenSegs.add(rootID);
          needsExitGlue_.add(linkID);
          break; // done anyway, but let's be obvious about it!
        } else if (lsid.isDirect()) {
          return;
        } else {
          //
          // In saving the trunk, we toss points away until we have
          // _emerged_ from the terminal rectangle.
          LinkSegment seg = bp.getSegment(lsid);
          Point2D end = seg.getEnd();
          if (alwaysAdd) {  // have emerged...
            ptsToRoot.add(new TrackPos((Point2D)end.clone()));
            if (seenSegs.contains(lsid)) {
              break;
            }
            seenSegs.add(lsid);
          } else {
            Point2D start = seg.getStart();
            if (chopRect.contains(end) && chopRect.contains(start)) {
              continue;
            }
            UiUtil.PointAndSide pasIntersect = UiUtil.rectIntersection(chopRect, start, end);
            if (pasIntersect != null) {
              ptsToRoot.add(new TrackPos(pasIntersect.point));
              alwaysAdd = true;
              chopPtToBoundary_.put(pasIntersect.point.clone(), new Integer(rectSideToOurBorder(pasIntersect.side)));
              // ONLY WANT ONE CHOP per subset that EVERYBODY USES!
              // Lack of adding to seenSegs will create link overlaps to start point
              // added in next pass.  BUT chopRect/intersection may be different 
              // for different subsets!  FIXME
            }
          }
        }
      }
      linkIDCandidates.add(linkID);
      ArrayList ptsPerLink = new NoZeroList();
      candPointsPerLink.put(linkID, ptsPerLink);
      Collections.reverse(ptsToRoot);
      ptsPerLink.addAll(ptsToRoot);
    }
    // FIXME go for the minimum
    //
    // We may have several options heading into the chopRect to use.  But we only want
    // to keep one!  BUT it is crucial we do not orphan somebody!
    //
    linkIDs.addAll(linkIDCandidates);
    pointsPerLink_.putAll(candPointsPerLink);
    return;
  }
 
  /***************************************************************************
  **
  ** Faced with external sources routing into genome subsets, we wish to retain those
  ** link pieces up to the boundaries of the subsets for later reassembly.
  **
  */
  
  public void extractTreeTrunkRemains(BusProperties bp, Set<String> links, Rectangle chopRect, Map<LinkSegmentID, LinkSegment> segGeoms, Set<LinkSegmentID> seenSegs) {
    Iterator<LinkBusDrop> dit = bp.getDrops();
    ArrayList<String> linkIDCandidates = new ArrayList<String>();
    HashMap<String, NoZeroList> candPointsPerLink = new HashMap<String, NoZeroList>();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      ArrayList<TrackPos> ptsToRoot = new ArrayList<TrackPos>();
      String linkID = drop.getTargetRef();
      if (!links.contains(linkID)) {
        continue;
      }
      // This list SKIPS the root segment!
      List<LinkSegmentID> segIDs = bp.getSegmentIDsToRootForEndDrop(drop);
      boolean haveEmerged = false;
      Iterator<LinkSegmentID> sit = segIDs.iterator();
      while (sit.hasNext()) {
        LinkSegmentID lsid = sit.next();
        if (lsid.isForEndDrop()) {
          LinkSegment fakeSeg = segGeoms.get(lsid);
          Point2D end = fakeSeg.getEnd();   
          Point2D start = fakeSeg.getStart();
          // We have an end drop that is entirely inside the chop rect, so we are happy:
          if (chopRect.contains(end) && chopRect.contains(start)) {
            continue;
          }
          UiUtil.PointAndSide[] pasIntersect = UiUtil.rectIntersections(chopRect, start, end);
          if (pasIntersect != null) {
            // We have an intersect, so ass the point, map to boundary, and record emergence:
            ptsToRoot.add(new TrackPos(pasIntersect[pasIntersect.length - 1].point));
            haveEmerged = true;
            chopPtToBoundary_.put((Point2D)pasIntersect[pasIntersect.length - 1].point.clone(), 
                                  new Integer(rectSideToOurBorder(pasIntersect[pasIntersect.length - 1].side)));
            //
            // We are NOT adding to seenSegs!  This means that we might create overlapping links
            // back to the root on multiple passes.  BUT, if (when) we move to handling multiple
            // subsets, this will ensure that other subsets will have a chance to do their own
            // chopping.  Since we fix overlaps nowadays anyway, better to NOT add!
            //
            // Lack of adding to seenSegs will create link overlaps to start point
            // added in next pass.  BUT chopRect/intersection may be different 
            // for different subsets!  FIXME
          }
        } else if (lsid.isForStartDrop()) {
          LinkSegmentID rootID = bp.getRootSegmentID();
          seenSegs.add(rootID);        
          LinkSegment fakeSeg = (LinkSegment)segGeoms.get(lsid);
          Point2D start = fakeSeg.getStart();
          Point2D end = fakeSeg.getEnd();
          //
          // 11/27/12 Up until now, we included the endpoint of the start drop.  This is not really
          // a good idea, since it may be well inside the chopRect, and cause overlaps with the
          // new geometry.  Instead, do not add it if it is inside the chopRect!
          //
          if (!chopRect.contains(end)) {
            ptsToRoot.add(new TrackPos((Point2D)end.clone()));
          }
          UiUtil.PointAndSide[] pasIntersect = UiUtil.rectIntersections(chopRect, start, end);
          if (pasIntersect != null) {
            ptsToRoot.add(new TrackPos(pasIntersect[pasIntersect.length - 1].point));
            chopPtToBoundary_.put((Point2D)pasIntersect[pasIntersect.length - 1].point.clone(), 
                                  new Integer(rectSideToOurBorder(pasIntersect[pasIntersect.length - 1].side)));
          }
          needsExitGlue_.add(linkID);
          break; // done anyway, but let's be obvious about it!
        } else if (lsid.isDirect()) {
          return;
        } else {
          //
          // In saving the trunk, we toss points away until we have
          // _emerged_ from the terminal rectangle.  Even after we have emerged, if we re-enter, we
          // will toss away what we have saved after we emerge again.
          //
          LinkSegment seg = bp.getSegment(lsid);
          Point2D end = seg.getEnd();
          Point2D start = seg.getStart();
          if (chopRect.contains(end) && chopRect.contains(start)) {
            // have actually re-entered.  Should not happen...should have caught intersection on
            // re-entry below, but this should be a no-op then:
            if (haveEmerged) {
              haveEmerged = false;
              ptsToRoot.clear();
            }
            continue;
          }
          UiUtil.PointAndSide[] pasIntersect = UiUtil.rectIntersections(chopRect, start, end);
          if (pasIntersect != null) {
            // Previously emerged but have an intersection, so we are going to start over:
            if (haveEmerged) {
              haveEmerged = false;
              ptsToRoot.clear();               
            }   
            ptsToRoot.add(new TrackPos(pasIntersect[pasIntersect.length - 1].point));
            haveEmerged = true;
            chopPtToBoundary_.put((Point2D)pasIntersect[pasIntersect.length - 1].point.clone(), 
                                  new Integer(rectSideToOurBorder(pasIntersect[pasIntersect.length - 1].side)));
            //
            // We are NOT adding to seenSegs!  This means that we might create overlapping links
            // back to the root on multiple passes.  BUT, if (when) we move to handling multiple
            // subsets, this will ensure that other subsets will have a chance to do their own
            // chopping.  Since we fix overlaps nowadays anyway, better to NOT add!
            //
            // Lack of adding to seenSegs will create link overlaps to start point
            // added in next pass.  BUT chopRect/intersection may be different 
            // for different subsets!  FIXME
          } else if (haveEmerged) {
            // This is where we are saving points after we have emerged.  Note we save the END, then
            // choose to be done if already encountered:
            ptsToRoot.add(new TrackPos((Point2D)end.clone()));
            //
            // If we have already encountered the seg, we _stop_ backtracking up the tree:
            //
            if (seenSegs.contains(lsid)) {
              break;
            }
            seenSegs.add(lsid);
          }
        }
      }
      // Here is where the points we have saved get stashed for the root:
      linkIDCandidates.add(linkID);
      NoZeroList ptsPerLink = new NoZeroList();
      candPointsPerLink.put(linkID, ptsPerLink);
      Collections.reverse(ptsToRoot);
      ptsPerLink.addAll(ptsToRoot);
      LayoutFailureTracker.recordTreeTrunk(this, linkID, ptsPerLink);
    }
    
    //
    // FIXME!
    // We are accumulating here all the chop points.  But we later only take the
    // first point per boundary (see extractForAlienSplice()).  Then it appears we take
    // the first boundary (see entryRouteNeeded()) we find.  This is hardly the optimum
    // approach.  Should go for the MINIMUM distance trace to do the job, right??
    //
    //
    // We may have several options heading into the chopRect to use.  But we only want
    // to keep one!  BUT it is crucial we do not orphan somebody!
    //
    // Currently sending them all out, pruning is occurring downstream in a rather
    // arbitrary fashion....
    //
   
    linkIDs_.addAll(linkIDCandidates);
    if ((new HashSet<String>(linkIDs_)).size() != linkIDs_.size()) {
      throw new IllegalStateException();
    }
    pointsPerLink_.putAll(candPointsPerLink);
    return;
  }

  /***************************************************************************
  **
  ** Reverse of previous; keep the leaves, toss the root
  **
  */
  
  public void extractTreeLeafRemains(BusProperties bp, Rectangle chopRect, Map<LinkSegmentID, LinkSegment> segGeoms, 
                                     Set<String> ignoreLinks, boolean regionChopped) {
    Iterator<LinkBusDrop> dit = bp.getDrops();
    HashSet<LinkSegmentID> seenSegs = new HashSet<LinkSegmentID>();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      ArrayList<TrackPos> ptsToRoot = new ArrayList<TrackPos>();
      String linkID = drop.getTargetRef();
      if ((ignoreLinks != null) && ignoreLinks.contains(linkID)) {
        continue;
      }
      // This list SKIPS the root segment!
      List<LinkSegmentID> segIDs = bp.getSegmentIDsToRootForEndDrop(drop);
      Iterator<LinkSegmentID> sit = segIDs.iterator();
      while (sit.hasNext()) {
        LinkSegmentID lsid = sit.next();       
        if (lsid.isForEndDrop()) {
          // 11/27/12 Before this, we just skipped the segment.  But if the end drop is exiting
          // the chop, we need to use the intersection!
          boolean intersected = leafChopper(lsid, segGeoms, ptsToRoot, regionChopped, chopRect);
          if (intersected) {
            needsExitGlue_.add(linkID);
            break;
          }
        } else if (lsid.isForStartDrop()) {
          LinkSegmentID rootID = bp.getRootSegmentID();
          seenSegs.add(rootID);
          leafChopper(lsid, segGeoms, ptsToRoot, regionChopped, chopRect);
          needsExitGlue_.add(linkID);
          break; // done anyway, but let's be obvious about it!
        } else if (lsid.isDirect()) {
          return;
        } else {
          // Note that the only thing moved so far are the NODES, so only the start and end drops
          // need to resort to saved geometry:
          LinkSegment seg = bp.getSegment(lsid);
          Point2D end = seg.getEnd();
          ptsToRoot.add(new TrackPos((Point2D)end.clone()));
          if (seenSegs.contains(lsid)) {
            break;
          }
          seenSegs.add(lsid);
          boolean intersected = leafChopper(lsid, segGeoms, ptsToRoot, regionChopped, chopRect);
          if (intersected) {
            needsExitGlue_.add(linkID);
            break;
          }
        }
      }
      if (linkIDs_.contains(linkID)) {
        throw new IllegalStateException();
      }
      linkIDs_.add(linkID);
      NoZeroList ptsPerLink = new NoZeroList();
      pointsPerLink_.put(linkID, ptsPerLink);
      Collections.reverse(ptsToRoot);
      ptsPerLink.addAll(ptsToRoot);
      LayoutFailureTracker.recordTreeLeaf(this, linkID, ptsPerLink);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Chop operations for segments
  */
  
  private boolean leafChopper(LinkSegmentID lsid, Map<LinkSegmentID, LinkSegment> segGeoms, List<TrackPos> ptsToRoot, 
                              boolean regionChopped, Rectangle chopRect) {  
  
    LinkSegment fakeSeg = segGeoms.get(lsid);
    Point2D start = fakeSeg.getStart();
    Point2D end = fakeSeg.getEnd();
    ptsToRoot.add(new TrackPos((Point2D)end.clone()));
    UiUtil.PointAndSide[] pasIntersect = UiUtil.rectIntersections(chopRect, start, end);
    boolean retval = (pasIntersect != null);
    //
    // Comment from start drop case:
    // Note that if the source was originally outside the new chopRect, the link MUST be
    // glued to that point.  Otherwise, we are looking at formally creating an orthogonal routing
    // that may overlap existing tracing.
    //  
    // Comment from regular segment case:
    // With region chopping, we want the endpoint, not the cut.  For the associated side, the
    // pasIntersect is ordered end to start, so we want that one!
    //
    if (retval) {
      Point2D usePt = (regionChopped) ? (Point2D)end.clone() : pasIntersect[pasIntersect.length - 1].point;            
      ptsToRoot.add(new TrackPos(usePt)); 
      int useSideIndex = (regionChopped) ? 0 : pasIntersect.length - 1;
      chopPtToBoundary_.put((Point2D)usePt.clone(), 
                            new Integer(rectSideToOurBorder(pasIntersect[useSideIndex].side)));
    }  
    return (retval);
  }

  /***************************************************************************
  **
  ** Normalize the links
  */
  
  public void normalizeLinks() {
    if (normalized_) {
      return;
    }
    
    int numLink = linkIDs_.size();
    for (int i = 0; i < numLink; i++) {
      String linkID = linkIDs_.get(i);
      NoZeroList nzl = pointsPerLink_.get(linkID);
      nzl.clean();
    }
    
    //
    // Repair any ordering issues in the link listing:
    //
    
    linkIDs_ = new ArrayList<String>(recalculateOrder()); 
    if (linkIDs_.size() != numLink) {
      throw new IllegalStateException();
    }
   
    //
    // Easy kill.  If a link lower down dups a previous segment series,
    // we drop the duplicated portion.
    //
    
    for (int i = 0; i < numLink; i++) {
      String masterLink = linkIDs_.get(i);
      NoZeroList masterPts = pointsPerLink_.get(masterLink);
      int numMaster = masterPts.size();
      for (int j = i + 1; j < numLink; j++) {
        String checkLink = linkIDs_.get(j);
        NoZeroList checkPts = pointsPerLink_.get(checkLink);
        int k = 0;
        int n = 0;
        boolean runStarted = false;
        while (k < numMaster) {
          Point2D pt = masterPts.get(k).getPoint();
          if (!runStarted) {
            n = 0;
          }
          while (n < checkPts.size()) {
            Point2D chkPt = checkPts.get(n).getPoint();
            if (chkPt.equals(pt)) {
              if (runStarted) {
                checkPts.remove(n - 1);
                break;
              } else {
                runStarted = true;
                n++;
                break;
              }
            } else {
              runStarted = false;
              n++;
            }
          }
          k++;
        }    
      }  
    }
    normalized_ = true;
    return;
  }
  
  /***************************************************************************
  **
  ** We need to create an ordering of links that insures it can be built as
  ** a tree.  For each link A, from the list of the link's points, take the first point.  
  ** Then find another link B that includes this point an a position greater than 1.  
  ** If found, B is made a parent of A.  If not, link A will be a root link.  (Note that
  ** if there are multiple root links, they all should SHARE the first link point!)
  ** Gotta prevent cycles of course.  So a link C cannot be made the child of another
  ** link D, where D is already a decendant of link C. 
  ** 
  */
  
  private List<String> recalculateOrder() {

    HashSet<String> nodes = new HashSet<String>(linkIDs_);
    HashSet<Link> links = new HashSet<Link>();

    //
    // As the links should in general be ordered already, best to move
    // backwards through the list.
    //
    
    int numLink = linkIDs_.size();
    for (int i = numLink - 1; i >= 0; i--) {
      String linkID = linkIDs_.get(i);
      NoZeroList linkPts = pointsPerLink_.get(linkID);
      if (linkPts.isEmpty()) {
        throw new IllegalStateException();
      }
      Point2D firstPt = linkPts.get(0).getPoint();
      boolean lookingForParent = true;
      int j = numLink - 1;
      while (lookingForParent && (j >= 0)) {
        String testID = linkIDs_.get(j--);
        if (testID.equals(linkID)) {
          continue;
        }
        NoZeroList testPts = pointsPerLink_.get(testID);       
        if (testPts.isEmpty()) {
          throw new IllegalStateException();
        }
        int numTest = testPts.size();
        // Note we skip the first point!
        for (int k = 1; k < numTest; k++) {
          Point2D testPt = testPts.get(k).getPoint();
          if (firstPt.equals(testPt)) {
            HashSet<Link> testLinks = new HashSet<Link>(links);
            Link toAdd = new Link(linkID, testID);
            testLinks.add(toAdd);              
            CycleFinder cf = new CycleFinder(nodes, testLinks);
            if (!cf.hasACycle()) {
              links.add(toAdd);
              lookingForParent = false;
              break;
            }
          }
        }
      }
    }
    
    //
    // Now convert the parent map into a partial order of the links:
    //
    
    GraphSearcher gs = new GraphSearcher(nodes, links);
    List<String> linkList = gs.topoSortToPartialOrdering(gs.topoSort(false));
    Collections.reverse(linkList);
    return (linkList);
  }
  
  /***************************************************************************
  **
  ** Build the exit routes in the needed order:
  */
  
  public Map<String, RouteNeeds> buildNeededExitRoutes(List<RouteNeeds> needs, Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    HashMap<String, RouteNeeds> retval = new HashMap<String, RouteNeeds>();
    int numNeeds = needs.size();
    for (int i = 0; i < numNeeds; i++) {
      RouteNeeds rn = needs.get(i);
      buildNeededExitRoute(rn, solnPerBorder);
      retval.put(rn.linkID, rn);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the exit route:
  */
  
  private void buildNeededExitRoute(RouteNeeds rn, Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
            
    LinkBundleSplicer.SpliceSolution soln = (solnPerBorder == null) ? null : solnPerBorder.get(rn.border);

    rn.path = new NoZeroList();
    PlacedPoint ppi = exitFramework_.get(new Integer(INIT_POINT));
    if ((ppi != null) && !ppi.placed) {
      rn.path.addAll(initRoute_);
      ppi.placed = true;
    }
    if (rn.border.intValue() == LEFT_BORDER) {
      PlacedPoint ppl = exitFramework_.get(new Integer(LEFT_TIP));
      if (ppl != null) {
        if (!ppl.placed) { // If it exists and we care about it, it has been placed
          throw new IllegalStateException();
        }
        rn.path.add(new TrackPos(ppl.point));
      }
      PlacedPoint pps = exitFramework_.get(new Integer(INIT_TRACE));
      if (pps != null) {
        if (!pps.placed) {
          rn.path.add(new TrackPos(pps.point));
          pps.placed = true;
        }
      }
      Point2D lex = (Point2D)borderPoints_.get(new Integer(LEFT_BORDER));
      rn.path.add(new TrackPos(lex));
    }
    if (rn.border.intValue() == RIGHT_BORDER) {
      PlacedPoint ppr = exitFramework_.get(new Integer(RIGHT_TIP));
      if (ppr != null) {
        if (!ppr.placed) {
          rn.path.add(new TrackPos(ppr.point));
          ppr.placed = true;
        }
      }
      Point2D lex = borderPoints_.get(new Integer(RIGHT_BORDER));
      rn.path.add(new TrackPos(lex));
    }
    if (rn.border.intValue() == TOP_BORDER) {
      PlacedPoint ppl = exitFramework_.get(new Integer(LEFT_TIP));
      if (ppl != null) {
        if (!ppl.placed) { // If it exists and we care about it, it has been placed
          throw new IllegalStateException();
        }
        rn.path.add(new TrackPos(ppl.point));
      }
      PlacedPoint pps = exitFramework_.get(new Integer(INIT_TRACE));
      if (pps != null) {
        if (!pps.placed) {
          rn.path.add(new TrackPos(pps.point));
          pps.placed = true;
        }
      }
      PlacedPoint ppn = exitFramework_.get(new Integer(MIN_TRACE));
      if (ppn != null) {
        if (!ppn.placed) { // If it exists and we care about it, it has been placed
          throw new IllegalStateException();
        }
        rn.path.add(new TrackPos(ppn.point));
      }         
      Point2D lex = borderPoints_.get(new Integer(TOP_BORDER));
      rn.path.add(new TrackPos(lex));
    }
    if (rn.border.intValue() == BOTTOM_BORDER) {
      PlacedPoint ppl = exitFramework_.get(new Integer(LEFT_TIP));
      if (ppl != null) {
        if (!ppl.placed) { // If it exists and we care about it, it has been placed
          throw new IllegalStateException();
        }
        rn.path.add(new TrackPos(ppl.point));
      }
      PlacedPoint pps = exitFramework_.get(new Integer(INIT_TRACE));
      if (pps != null) {
        if (!pps.placed) {
          rn.path.add(new TrackPos(pps.point));
          pps.placed = true;
        }
      }
      PlacedPoint ppx = exitFramework_.get(new Integer(MAX_TRACE));
      if (ppx != null) {
        if (!ppx.placed) { // If it exists and we care about it, it has been placed
          throw new IllegalStateException();
        }
        rn.path.add(new TrackPos(ppx.point));
      }         
      Point2D lex = (Point2D)borderPoints_.get(new Integer(BOTTOM_BORDER));            
      rn.path.add(new TrackPos(lex));
    }
    
    if (soln != null) {
      Map<Integer, List<Point>> perExit = soln.getPaths(srcID_);
      if (perExit == null) {
        //
        // We see this when doing region layouts, and inter-region links refuse to
        // get recovered.  If that is the case, there was nothing to chop, and so
        // no paths to work with.  We try to fix it at the end by doing a final
        // relayout attempt, but in the meantime we have this.
        //
        System.err.println("buildNeededExitRoute: Unexpected null perExit");
        return;
      }
      List<Point> ptList;
      if (perExit.size() == 1) {
        ptList = perExit.values().iterator().next();
      } else {
        ptList = perExit.get(new Integer((int)(rn.coord.doubleValue() / UiUtil.GRID_SIZE)));
      }
      if (ptList != null) {
        int pathLen = ptList.size();
        for (int i = 0; i < pathLen; i++) {
          Point2D pt = (Point2D)ptList.get(i);
          Point2D scaled = new Point2D.Double(pt.getX() * UiUtil.GRID_SIZE, pt.getY() * UiUtil.GRID_SIZE);
          rn.path.add(new TrackPos(scaled));          
        } 
      } else {
        System.err.println("buildNeededExitRoute: Unexpected null ptList");
      }
    }
    LayoutFailureTracker.trackRouteNeeds(this, rn);

    return;
  }

  /***************************************************************************
  **
  ** Build the entry route as specified
  */

  private void buildNeededEntryRoute(RouteNeeds rn, Map<Integer, LinkBundleSplicer.SpliceSolution> spliceSolutionsPerBorder) {
    
    LinkBundleSplicer.SpliceSolution soln = (spliceSolutionsPerBorder == null) ? null : spliceSolutionsPerBorder.get(rn.border);

    rn.path = new NoZeroList();
      
    if (soln != null) {
      Map<Integer, List<Point>> perExit = soln.getPaths(srcID_);
      //
      // Previous comment said this was a BOGUS test!  But is it still?  Note
      // we now send out an error message if the case crops up.  Be on the lookout!
      //
      if (perExit != null) {
        List<Point> ptList;
        if (perExit.size() == 1) {
          ptList = perExit.values().iterator().next();
        } else {
          ptList = perExit.get(new Integer((int)(rn.coord.doubleValue() / UiUtil.GRID_SIZE)));
        }

        if (ptList != null) {
          int pathLen = ptList.size();
          for (int i = pathLen - 1; i >= 0; i--) {
            Point2D pt = (Point2D)ptList.get(i);
            Point2D scaled = new Point2D.Double(pt.getX() * UiUtil.GRID_SIZE, pt.getY() * UiUtil.GRID_SIZE);
            rn.path.add(new TrackPos(scaled));          
          } 
        }
      } else {
        System.err.println("buildNeededEntryRoute: Unexpected null perExit");
      }
    }

    if (rn.border.intValue() == LEFT_BORDER) { 
      Point2D lex = borderPoints_.get(new Integer(LEFT_BORDER));
      rn.path.add(new TrackPos(lex));
    } else if (rn.border.intValue() == RIGHT_BORDER) {
      Point2D lex = borderPoints_.get(new Integer(RIGHT_BORDER));
      rn.path.add(new TrackPos(lex));
      lex = borderPoints_.get(new Integer(LEFT_BORDER));
      rn.path.add(new TrackPos(lex));
    } else if (rn.border.intValue() == TOP_BORDER) {
      Point2D lex = borderPoints_.get(new Integer(TOP_BORDER));
      rn.path.add(new TrackPos(lex));
      lex = borderPoints_.get(new Integer(LEFT_BORDER));
      rn.path.add(new TrackPos(lex));
    } else if (rn.border.intValue() == BOTTOM_BORDER) {
      Point2D lex = borderPoints_.get(new Integer(BOTTOM_BORDER));
      rn.path.add(new TrackPos(lex));
      lex = borderPoints_.get(new Integer(LEFT_BORDER));
      rn.path.add(new TrackPos(lex));
    }
    return;
  }

  /***************************************************************************
  **
  ** Set a border position
  */
    
  public void setBorderPosition(int direction, Point2D position) {
    borderPoints_.put(new Integer(direction), (Point2D)position.clone());
    return;
  }

  /***************************************************************************
  **
  ** Get a border position
  */
    
  public Point2D getBorderPosition(int direction) {
    return (borderPoints_.get(new Integer(direction)));
  }
  
  /***************************************************************************
  **
  ** Get all the points that are aligned with left border position
  */
    
  public List<Point2D> getLeftBorderAlignedPositions() {
    Point2D lbp = borderPoints_.get(new Integer(LEFT_BORDER));
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    if (lbp == null) {
      PlacedPoint pps = exitFramework_.get(new Integer(INIT_TRACE));
      if (pps != null) {
        lbp = pps.point;
      } else {
        return (retval);
      }
    }
    double lbpx = lbp.getX(); 
    Iterator<NoZeroList> pplit = pointsPerLink_.values().iterator();
    while (pplit.hasNext()) {
      NoZeroList ptsList = pplit.next();
      int numL = ptsList.size();
      for (int i = 0; i < numL; i++) {
        TrackPos tp = ptsList.get(i);
        if (!tp.needsConversion()) {
          Point2D segPt = tp.getPoint();
          if (segPt.getX() == lbpx) {
            retval.add((Point2D)segPt.clone());
          }
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build onto an existing link
  */
    
  public void addPositionToLink(String linkID, TrackPos nextLoc) {
    normalized_ = false;
    if (linkID == null) {
      if (initRoute_ == null) {
        throw new IllegalStateException();
      }
      initRoute_.add(nextLoc);
      return;
    }    
    
    if (!linkIDs_.contains(linkID)) {
      throw new IllegalStateException();
    }
    NoZeroList nzl = pointsPerLink_.get(linkID);
    nzl.add(nextLoc);
    return;
  }
  
  /***************************************************************************
  **
  ** Build onto an existing link
  */
  
  public void addPositionListToLinkNZ(String linkID, NoZeroList nextLocs) {
    normalized_ = false;
    if (linkID == null) {
      if (initRoute_ == null) {
        throw new IllegalStateException();
      }
      initRoute_.addAll(nextLocs);
      return;
    }    
    if (!linkIDs_.contains(linkID)) {
      throw new IllegalStateException();
    }
    pointsPerLink_.get(linkID).addAll(nextLocs);
    return;
  }
  
 /***************************************************************************
  **
  ** Build onto an existing link
  */
  
  public void addPositionListToLink(String linkID, List<? extends TrackPos> nextLocs) {
    normalized_ = false;
    if (linkID == null) {
      if (initRoute_ == null) {
        throw new IllegalStateException();
      }
      initRoute_.addAll(nextLocs);
      return;
    }    
    if (!linkIDs_.contains(linkID)) {
      throw new IllegalStateException();
    }
    pointsPerLink_.get(linkID).addAll(nextLocs);
    return;
  }

  /***************************************************************************
  **
  ** Start the init route
  */
  
  public void startInitRoute() {
    normalized_ = false;
    initRoute_ = new NoZeroList();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the init route
  */
     
  public NoZeroList getInitRoute() {
    return (initRoute_);
  }  
 
  /***************************************************************************
  **
  ** Start a new route
  */

  public void startNewLink(String linkID) {
    normalized_ = false;
    if (linkIDs_.contains(linkID)) {
      throw new IllegalStateException();
    }
    linkIDs_.add(linkID);
    NoZeroList ptsPerLink = new NoZeroList();
    pointsPerLink_.put(linkID, ptsPerLink);
    return;
  }    

  /***************************************************************************
  **
  ** Merge
  */
   
  public void merge(SpecialtyLayoutLinkData other) {
    normalized_ = false;
    this.linkIDs_.addAll(other.linkIDs_);
    if ((new HashSet<String>(this.linkIDs_)).size() != this.linkIDs_.size()) {
      throw new IllegalStateException();
    }
    this.pointsPerLink_.putAll(other.pointsPerLink_);
    return;
  }
  
  /***************************************************************************
  **
  ** Find out what exit routes are required for OUR links that need exit
  ** glue (tagged with needsExitGlue), using border positions provided by the OTHER SLLD
  */

  public List<RouteNeeds> exitRouteNeeds(SpecialtyLayoutLinkData other, Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    ArrayList<RouteNeeds> needs = new ArrayList<RouteNeeds>();
    if (linkIDs_.isEmpty()) {
      return (needs);        
    }
    
    Iterator<String> negit = needsExitGlue_.iterator();
    while (negit.hasNext()) {
      String linkID = negit.next();
      boolean gotIt = false;
      NoZeroList need = pointsPerLink_.get(linkID);
      TrackPos tp = need.get(0);
      Integer boundary = chopPtToBoundary_.get(tp.getPoint());
  
      if ((solnPerBorder != null) && !solnPerBorder.isEmpty()) {
        Iterator<Integer> spbit = solnPerBorder.keySet().iterator();
        while (spbit.hasNext()) {
          Integer border = spbit.next();
          LinkBundleSplicer.SpliceSolution soln = solnPerBorder.get(border);
          if ((soln.getPaths(srcID_) != null) && boundary.equals(border)) {
            needs.add(new RouteNeeds(linkID, border, null));
            gotIt = true; 
            break;
          }
        }
      }
      if (!gotIt) {
        RouteNeeds rn = other.posToNeed(linkID, tp.getPoint());
        needs.add(rn);
      }
    }
    return (needs);
  }
  
  /***************************************************************************
  **
  ** Find out what exit routes are required for the point using border positions 
  ** provided by US
  */

  private RouteNeeds exitRouteNeeded(SpecialtyLayoutLinkData other, Point2D startPoint, Vector2D dir, 
                                     Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    
    String linkID = other.needsExitGlue_.iterator().next();     
    if (solnPerBorder != null) {
      Set<Integer> sbpk = solnPerBorder.keySet();
      if (sbpk.size() == 1) {
        return (new RouteNeeds(linkID, sbpk.iterator().next(), null));
      }     
    }
    RouteNeeds rn = vecToNeed(linkID, dir, startPoint);
    if (rn != null) {
      return (rn);
    }      
    rn = this.posToNeed(linkID, startPoint);    
    return (rn);
  }
 
  /***************************************************************************
  **
  ** Map vector to route need
  */

  private static RouteNeeds vecToNeed(String linkID, Vector2D dir, Point2D termPoint) {
    if (dir != null) {
      int canonical = dir.canonicalDir();
      switch (canonical) {
        case Vector2D.EAST:
          return (new RouteNeeds(linkID, new Integer(LEFT_BORDER), new Double(termPoint.getY())));
        case Vector2D.WEST:
          return (new RouteNeeds(linkID, new Integer(RIGHT_BORDER), new Double(termPoint.getY())));
        case Vector2D.SOUTH:
          return (new RouteNeeds(linkID, new Integer(TOP_BORDER), new Double(termPoint.getX())));
        case Vector2D.NORTH:
          return (new RouteNeeds(linkID, new Integer(BOTTOM_BORDER), new Double(termPoint.getX())));
        case Vector2D.NOT_CANONICAL:
          return (null);
        default:
          throw new IllegalArgumentException();
      }  
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Map position to route need.  Legacy backup-only stuff now that we are doing
  ** boundary-based splicing: 
  */

  private RouteNeeds posToNeed(String linkID, Point2D pos) {
    double min = Double.POSITIVE_INFINITY;
    Integer minPos = null;
    for (int i = 0; i < NUM_BORDERS; i++) {
      Point2D pt = getBorderPosition(i);
      double dSq = pt.distanceSq(pos);
      if (dSq < min) {
        min = dSq;
        minPos = new Integer(i);
      }
    }
    //
    // FIXME: Update from null arg.
    //
    return (new RouteNeeds(linkID, minPos, null));
  }

  /***************************************************************************
  **
  ** Find out what entry route is required.  The "other" has the entry data!
  */

  private RouteNeeds entryRouteNeeded(SpecialtyLayoutLinkData other, NoZeroList need, Vector2D dir, 
                                      Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    int numOther = other.linkIDs_.size();
    if (numOther == 0) {
      return (null);        
    }
    String linkID = other.linkIDs_.get(0);
    int numInNeed = need.size();
    if (numInNeed == 0) {
      // Should not happen...
      return (null);        
    }
    
    //
    // FIRST BORDER SOLUTION THAT HAS A PATH FOR THE SOURCE IS GETTING CHOSEN HERE!!!
    //
    
    if ((solnPerBorder != null) && !solnPerBorder.isEmpty()) {
      Iterator<Integer> spbit = solnPerBorder.keySet().iterator();
      while (spbit.hasNext()) {
        Integer border = spbit.next();
        LinkBundleSplicer.SpliceSolution soln = solnPerBorder.get(border);
        if (soln.getPaths(srcID_) != null) {
          return (new RouteNeeds(linkID, border, null));
        }
      }
    }
   
    TrackPos tp = need.get(numInNeed - 1);
    Point2D terminalPoint = tp.getPoint();
    //
    // Use terminal direction if available...
    //
    
    RouteNeeds rn = vecToNeed(linkID, dir, terminalPoint);
    if (rn != null) {
      return (rn);
    }  
    
    rn = other.posToNeed(linkID, terminalPoint);
    return (rn);
  }

  public Point2D extractForAlienSplice(SpecialtyLayoutLinkData other, int border) {    
    // Find _first_ of our links that supports an "other" link 
    int numMe = this.linkIDs_.size();
    for (int i = 0; i < numMe; i++) {
      String linkID = this.linkIDs_.get(i);
      if (other.linkIDs_.contains(linkID)) {
        NoZeroList myPts = this.pointsPerLink_.get(linkID);
        if (myPts.isEmpty()) {
          continue;
        }
        TrackPos lastPos = myPts.get(myPts.size() - 1);
        Point2D pt = lastPos.getPoint();
        // Note that we are taking the first point for a boundary, not necessarily
        // the best point.  FIXME?
        Integer ptBorder = this.chopPtToBoundary_.get(pt);
        if ((ptBorder != null) && (ptBorder.intValue() == border)) {
          return (pt);
        }
      }
    }
    return (null);
  }
  
  public Point2D extractForNonSubSplice(SpecialtyLayoutLinkData other, int border) {
    int numOther = other.linkIDs_.size();
    for (int i = 0; i < numOther; i++) {
      String linkID = other.linkIDs_.get(i);
      if (!this.linkIDs_.contains(linkID)) {
        throw new IllegalStateException();
      }
      NoZeroList otherPts = other.pointsPerLink_.get(linkID);
      TrackPos firstPos = otherPts.get(0);
      Point2D pt = firstPos.getPoint();
      Integer ptBorder = (Integer)other.chopPtToBoundary_.get(pt);
      if ((ptBorder != null) && (ptBorder.intValue() == border)) {
        return (pt);
      }
    } 
    return (null);
  }
  
  public void mergeToAlienStub(SpecialtyLayoutLinkData other, Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    // Find _first_ of our links that supports an "other" link 
    LayoutFailureTracker.recordPreMergeOperation(this, other, "mergeToAlienStub");
    int numMe = this.linkIDs_.size();
    NoZeroList saveData = new NoZeroList();
    for (int i = 0; i < numMe; i++) {
      String linkID = (String)this.linkIDs_.get(i);
      if (other.linkIDs_.contains(linkID)) {
        saveData.addAll(this.pointsPerLink_.get(linkID));
        break;
      }
    }
        
    int numOther = other.linkIDs_.size();
    for (int i = 0; i < numOther; i++) {
      String linkID = (String)other.linkIDs_.get(i);
      if (!this.linkIDs_.contains(linkID)) {
        //
        // FIX ME?
        // Saw this on region layout with inbound links from out of region!  Not
        // seen recently.  Ditch the exception and try to continue...
        //throw new IllegalStateException();
        System.err.println("Unexpected linkID mismatch: " + linkID + " in " + this.linkIDs_);
        continue;
      }
      //
      // Alien stub is one that has been retained from original layout and
      // originates from a source outside our layout subsets.  Note that
      // *this* is the alien stub.
      // The *other* is for the points handling the arrival of the source
      // into a subset.  Note that ONLY THE FIRST LINK NEEDS the glue!
      //
      // The first link on this link list for these inputs will have the
      // needed path info that should be transferred to the first link on
      // the other link list.
      NoZeroList exists = this.pointsPerLink_.get(linkID);
      NoZeroList merging = other.pointsPerLink_.get(linkID);
      RouteNeeds rn = (i == 0) ? entryRouteNeeded(other, saveData, null, solnPerBorder) : null;
      exists.clear();
      if (i == 0) {
        exists.addAll(saveData);
        if (rn != null) {
          other.buildNeededEntryRoute(rn, solnPerBorder);
          exists.addAll(rn.path);
        }        
      }
      exists.addAll(merging);        
    }
    LayoutFailureTracker.recordMergeOperation(this, "mergeToAlienStub");
    return;
  }

  /***************************************************************************
  **
  ** Glue the other SLLD onto us (this); we provide exit
  **  routes to stick on the front of those in need
  */

  public void mergeToStubs(SpecialtyLayoutLinkData other, Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder) {
    LayoutFailureTracker.recordPreMergeOperation(this, other, "mergeToStubs");
    // Find out what exit routes are required for OTHER links that need exit
    // glue (tagged with needsExitGlue), using border positions provided by US
    List<RouteNeeds> needs = other.exitRouteNeeds(this, solnPerBorder);
    // Build those routes using our positions, used as a prefix.
    Map<String, RouteNeeds> builtRoutes = buildNeededExitRoutes(needs, solnPerBorder);
    int numOther = other.linkIDs_.size();
    for (int i = 0; i < numOther; i++) {
      String linkID = (String)other.linkIDs_.get(i);
      if (!this.linkIDs_.contains(linkID)) {
        throw new IllegalStateException();
      }
      NoZeroList exists = this.pointsPerLink_.get(linkID);
      NoZeroList merging = other.pointsPerLink_.get(linkID);
      RouteNeeds rn = (RouteNeeds)builtRoutes.get(linkID);
      // Note that all our internal link points are still around, just those
      // outbound guys with stubs are ditched:
      exists.clear();
      if (rn != null) {
        exists.addAll(rn.path);
      }
      exists.addAll(merging);        
    }
    LayoutFailureTracker.recordMergeOperation(this, "mergeToStubs");
    return;
  }

  /***************************************************************************
  **
  ** Use existing link point data between subsets (net modules) to glue them together.
  ** Note that in V6, where multi-subset cases are handled exclusively with derived
  ** inter-module links, we are not needing this.
  */
  
  public void mergeToStubsWithBetween(SpecialtyLayoutLinkData other, SpecialtyLayoutLinkData between,
                                      Map<Integer, LinkBundleSplicer.SpliceSolution> srcSpliceSolnPerBorder) { 
    LayoutFailureTracker.recordPreMergeOperation(this, other, "mergeToStubsWithBetween");
    //
    // Get exit route needs.  We are exiting from THIS SLLD!
    //    
    //
    // Get exit route needs for between 
    //
    List<RouteNeeds> betNeeds = between.exitRouteNeeds(this, srcSpliceSolnPerBorder);
    //
    // Prune the needs down to a guy that fulfills our subset needs!
    //
    ArrayList<RouteNeeds> prunedNeeds = new ArrayList<RouteNeeds>();
    int numNeeds = betNeeds.size();
    for (int i = 0; i < numNeeds; i++) {
      RouteNeeds rn = betNeeds.get(i);
      if (other.needsExitGlue_.contains(rn.linkID)) {
        prunedNeeds.add(rn);
        break;
      }
    }
    //
    // FIXME: Note that there is no guarantee that the pruned needs above
    // gets a candidate?  This is a hack:
    //
    if (prunedNeeds.isEmpty()) {
      prunedNeeds.addAll(other.exitRouteNeeds(this, srcSpliceSolnPerBorder));      
    }
    // Build those routes using our positions, used as a prefix.
    Map<String, RouteNeeds> builtRoutes = buildNeededExitRoutes(prunedNeeds, srcSpliceSolnPerBorder);
    int numOther = other.linkIDs_.size();
    for (int i = 0; i < numOther; i++) {
      String linkID = other.linkIDs_.get(i);
      if (!this.linkIDs_.contains(linkID)) {
        throw new IllegalStateException();
      }
      NoZeroList exists = this.pointsPerLink_.get(linkID);
      NoZeroList merging = other.pointsPerLink_.get(linkID);
      RouteNeeds rn = builtRoutes.get(linkID);
      // Note that all our internal link points are still around, just those
      // outbound guys with stubs are ditched:
      exists.clear();
      if (rn != null) {
        exists.addAll(rn.path);
        NoZeroList betPts = between.pointsPerLink_.get(linkID);
        if (betPts != null) {
          exists.addAll(betPts);
        }
      }
      exists.addAll(merging);        
    }
    LayoutFailureTracker.recordMergeOperation(this, "mergeToStubsWithBetween");
    return;
  }
  
  /***************************************************************************
  **
  ** Use existing net **module** link point data between subsets (net modules) to glue them together.
  */
  
  public void mergeToStubsWithPoints(String srcID, SpecialtyLayoutLinkData other,
                                     Map<Integer, LinkBundleSplicer.SpliceSolution> srcSpliceSolnPerBorder, 
                                     Map<Integer, LinkBundleSplicer.SpliceSolution> trgSpliceSolnPerBorder,                                 
                                     NetModuleLinkExtractor.ExtractResultForTarg eRes) {
    LayoutFailureTracker.recordPreMergeOperation(this, other, "mergeToStubsWithPoints");
    //
    // Get exit route needs.  We are exiting from THIS SLLD!
    //

    RouteNeeds rn = null;
    int numBet = eRes.size();
    if (numBet != 0) {
      Point2D firstPt = eRes.getPoint(srcID, 0);
      rn = exitRouteNeeded(other, firstPt, eRes.getStartDirection(), srcSpliceSolnPerBorder);
      buildNeededExitRoute(rn, srcSpliceSolnPerBorder);
    }
    
    NoZeroList erTpList = new NoZeroList();
    for (int j = 0; j < numBet; j++) {
      erTpList.add(new TrackPos(eRes.getPoint(srcID, j)));
    }
    
    int numOther = other.linkIDs_.size();
    for (int i = 0; i < numOther; i++) {
      String linkID = (String)other.linkIDs_.get(i);
      if (!this.linkIDs_.contains(linkID)) {
        throw new IllegalStateException();
      }
      NoZeroList exists = this.pointsPerLink_.get(linkID);
      NoZeroList merging = other.pointsPerLink_.get(linkID);
      exists.clear();
      if ((rn != null) && rn.linkID.equals(linkID)) {
        exists.addAll(rn.path);
        exists.addAll(erTpList);
      }
      //
      // The merging points either take us from the topmost (first) left point
      // for the first link in the list, or just stubs out from a previously
      // drawn link.  Need to create a route into the OTHER SLLD to get from the
      // inbound net module link arrival pad to that first point.
      //
      RouteNeeds rnx = (i == 0) ? entryRouteNeeded(other, erTpList, eRes.getTerminalDirection(), trgSpliceSolnPerBorder) : null;
      if (rnx != null) {
        other.buildNeededEntryRoute(rnx, trgSpliceSolnPerBorder);
        exists.addAll(rnx.path);
      }           
      exists.addAll(merging);        
    }
    LayoutFailureTracker.recordMergeOperation(this, "mergeToStubsWithPoints");
    return;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("links: ");
    buf.append(linkIDs_);
    buf.append("\n");
    buf.append("ptsPer: ");
    buf.append(pointsPerLink_);
    return (buf.toString());
  } 

  public static void mergeMaps(Map<String, SpecialtyLayoutLinkData> target, Map<String, SpecialtyLayoutLinkData> source) {
    Iterator<String> skit = source.keySet().iterator();
    while (skit.hasNext()) {
      String srcID = skit.next();
      SpecialtyLayoutLinkData sinSrc = source.get(srcID);
      if (target.containsKey(srcID)) {
        SpecialtyLayoutLinkData sinTrg = target.get(srcID);
        sinTrg.merge(sinSrc);
      } else {
        target.put(srcID, sinSrc);
      }             
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class TrackPos implements Cloneable {

    protected Point2D point; 

    public TrackPos(Point2D point) {  // Null arg is legit for late binding
      this.point = (point == null) ? null : (Point2D)point.clone();
    } 
    
    @Override
    public TrackPos clone() {
      try {
        TrackPos retval = (TrackPos)super.clone();
        if (this.point != null) {
          retval.point = (Point2D)this.point.clone();
        }
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public boolean needsConversion() {
      return (point == null);
    }    
    
    public String toString() {
      return ("TrackPos: pt = " + point);
    }   

    public Point2D getPoint() {
      return (point);
    }
    
    public static List<TrackPos> convertPointList(List<Point2D> pointList) {
      ArrayList<TrackPos> retval = new ArrayList<TrackPos>();
      int plSize = pointList.size();
      for (int i = 0; i < plSize; i++) {
        Point2D point = pointList.get(i);
        retval.add(new TrackPos(point));
      }
      return (retval);
    }         
  }
  
  //
  // We have this annoying tendency to add zero-length segments to our
  // link definitions.  Easiest way out is to drop these dups:
  //
  
  
  public static class NoZeroList {
    
    private ArrayList<TrackPos> myList_;
        
    public NoZeroList() {
      myList_ = new ArrayList<TrackPos>();
    }
    
    public boolean add(TrackPos tpElem) {
      int mySize = myList_.size();
      TrackPos lastElem = (mySize == 0) ? null : myList_.get(mySize - 1);
      boolean doPointCheck = (lastElem != null) && 
                             (lastElem.getPoint() != null) && 
                             (tpElem.getPoint() != null);
      if (doPointCheck && lastElem.getPoint().equals(tpElem.getPoint())) {
        return (false);
      }
      
      if (doPointCheck && (tpElem.getPoint().getX() == 0.0) && (tpElem.getPoint().getY() == 0.0) ) {
        System.err.println("Seeing 0 point: " + tpElem.getPoint());
      }
      
      return (myList_.add(tpElem));
    }
    
    public TrackPos get(int i) {
      return (myList_.get(i));
    }
    
    public Iterator<TrackPos> iterator() {
      return (myList_.iterator());
    }
     
    public int size() {
      return (myList_.size());
    }
    
    public boolean isEmpty() {
      return (myList_.isEmpty());
    }
    
    public TrackPos remove(int i) {
      return (myList_.remove(i));
    }
    
    public void clear() {
      myList_.clear();
      return;
    }
     
    public void appendTo(Collection<TrackPos> elems) {
      elems.addAll(myList_);
      return;
    }    
    
    public boolean addAll(Collection<? extends TrackPos> elems) {
      boolean retval = false;
      Iterator<? extends TrackPos> eit = elems.iterator();
      while (eit.hasNext()) {
        TrackPos elem = eit.next();
        boolean result = add(elem);
        retval = retval || result;
      }
      return (retval);
    }  
      
    public boolean addAll(NoZeroList elems) {
      boolean retval = false;
      Iterator<TrackPos> eit = elems.iterator();
      while (eit.hasNext()) {
        TrackPos elem = eit.next();
        boolean result = add(elem);
        retval = retval || result;
      }
      return (retval);
    }    
    
    public void clean() {
      int currIndex = 0;
      TrackPos lastElem = null;
      while (currIndex < myList_.size()) {
        TrackPos currElem = (TrackPos)get(currIndex);
        boolean doPointCheck = (lastElem != null) && 
                               (lastElem.getPoint() != null) && 
                               (currElem.getPoint() != null);
        if (doPointCheck && lastElem.getPoint().equals(currElem.getPoint())) {
          myList_.remove(currIndex);
        } else {
          lastElem = currElem;
          currIndex++;
        }
      }
      return;
    }
  }

  //
  // Only public for debug requirements...
  //
  
  public static class RouteNeeds {   
    String linkID;
    Integer border;
    NoZeroList path;
    Double coord;
    
    RouteNeeds(String linkID, Integer border, Double coord) {
      this.linkID = linkID;
      this.border = border;
      this.coord = coord;
    }
    
    public String toString() {
      return ("RouteNeeds: linkID = " + linkID + " border: " + border + " coord: " + coord + " path: " + path);
    }
  } 
  
  public static class PlacedPoint implements Cloneable {   
    boolean placed;
    Point2D point;
    
    public PlacedPoint(Point2D point, boolean placed) {
      this.point = (Point2D)point.clone();
      this.placed = placed;
    }
    
    public String toString() {
      return ("PlacedPoint: pt = " + point + " placed: " + placed);
    }
    
    public PlacedPoint clone() {
      try {
        PlacedPoint retval = (PlacedPoint)super.clone();
        retval.point = (Point2D)this.point.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }     
  }
    
  /***************************************************************************
  **
  ** Map our borders to LinkBundleSplicer cases:
  */
  
  public static int borderToSpSide(int border) {    
    switch (border) {
      case TOP_BORDER:
        return (LinkBundleSplicer.TOP);
      case BOTTOM_BORDER:
        return (LinkBundleSplicer.BOTTOM);
      case LEFT_BORDER:
        return (LinkBundleSplicer.LEFT);
      case RIGHT_BORDER:
        return (LinkBundleSplicer.RIGHT);
      default:
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Map UiUtil rect sides to out borders:
  */
  
  public static int rectSideToOurBorder(int rectSide) {    
    switch (rectSide) {
      case UiUtil.TOP:
        return (TOP_BORDER);
      case UiUtil.BOTTOM:
        return (BOTTOM_BORDER);
      case UiUtil.LEFT:
        return (LEFT_BORDER);
      case UiUtil.RIGHT:
        return (RIGHT_BORDER);
      default:
        throw new IllegalArgumentException();
    }    
  }
}  
