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

package org.systemsbiology.biotapestry.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.freerender.DrawTree;
import org.systemsbiology.biotapestry.ui.freerender.DrawTreeModelDataSource;
import org.systemsbiology.biotapestry.ui.layouts.ortho.FixOrthoOptions;
import org.systemsbiology.biotapestry.ui.layouts.ortho.TreeStrategy;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.QuadTree;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.xml.sax.Attributes;

/****************************************************************************
**
** Contains the information needed to layout a linkage
*/

public abstract class LinkProperties implements Cloneable, CornerOracle {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /**
  *** Possible segment relationships:
  **/
  
  public static final int SAME      = 0;
  public static final int ANCESTOR  = 1;
  public static final int DECENDANT = 2;
  public static final int SIBLING   = 3;
  public static final int OTHER     = 4;
   
  /**
  *** Possible orientations:
  **/

  public static final int LEFT  = 1;
  public static final int RIGHT = 2;
  public static final int UP    = 3;
  public static final int DOWN  = 4;
  
  public static final String LEFT_STR_  = "left";
  public static final String RIGHT_STR_ = "right";
  public static final String UP_STR_    = "up";
  public static final String DOWN_STR_  = "down";  
  
  /**
  *** Possible Line Styles:
  **/
  
  public static final int NONE     = -1;
  public static final int SOLID    = 0;
  public static final int THIN     = 1;
  public static final int DASH     = 2;
  public static final int THINDASH = 3;
  
  public static final String NONE_KEY     = "NONE";
  public static final String SOLID_KEY    = "SOLID";
  public static final String THIN_KEY     = "THIN";
  public static final String DASH_KEY     = "DASH";
  public static final String THINDASH_KEY = "THINDASH"; 
 
  /*
  ** Topology repair outcomes:
  */
  
  public static final int NOTHING_TO_REPAIR = 0;
  public static final int ALL_REPAIRED      = 1;
  public static final int SOME_REPAIRED     = 2;
  public static final int COULD_NOT_REPAIR  = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int REPAIR_CAP_ = 10000;
  private static final int PENULTIMATE_REPAIR_ = REPAIR_CAP_ - 1;
  
  private static final double INTERSECT_TOL = 5.0;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  protected Point2D textPos_;
  protected int textDir_;
  protected String textDirTag_;
  
  protected Object myRenderer_;
  
  protected SuggestedDrawStyle drawStyle_;  
  protected String srcTag_;   
  protected Map<String, LinkSegment> segments_;
  protected List<LinkBusDrop> drops_;
  protected UniqueLabeller labels_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS (this is absract...)
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** (Almost) null Constructor.  Child MUST init srcTag
  */

  protected LinkProperties(Object renderer)  {
    segments_ = new HashMap<String, LinkSegment>();
    drops_ = new ArrayList<LinkBusDrop>();
    labels_ = new UniqueLabeller();
    drawStyle_ = SuggestedDrawStyle.buildFromLegacy(SOLID);
    drawStyle_.setColorName("black");
    myRenderer_ = renderer;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  protected LinkProperties(LinkProperties other) {
    copySupport(other);
  }
 
  /***************************************************************************
  **
  ** UI-based Constructor
  */

  protected LinkProperties(float tx, float ty, String color, String src, Object renderer) {
    textPos_ = new Point2D.Float(tx, ty);
    textDir_ = LEFT;
    textDirTag_ = null;
    segments_ = new HashMap<String, LinkSegment>();
    drops_ = new ArrayList<LinkBusDrop>();
    labels_ = new UniqueLabeller();
    myRenderer_ = renderer;
    drawStyle_ = SuggestedDrawStyle.buildFromLegacy(SOLID);
    drawStyle_.setColorName(color);
    srcTag_ = src;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Break tree for new source
  */
  
  public LinkProperties breakTreePortionToNewSource(LinkSegmentID segID, Set<String> resolved, 
                                                    String newSourceID, Object extraInfo) {
    LinkProperties newProps = null;
    if (segID.isDirect()) {
      newProps = this.clone();
      newProps.srcTag_ = newSourceID;
    } else if (segID.isForDrop()) {
      LinkBusDrop drop = getDrop(segID);
      int dropType = drop.getDropType();
      if (dropType == LinkBusDrop.START_DROP) {
        newProps = this.clone();
        newProps.srcTag_ = newSourceID;
      } else {
        newProps = deriveDirectLink(newSourceID, drop, extraInfo);
      }
    } else {
      LinkSegment seg = getSegment(segID.getLinkSegTag());
      HashMap<String, String> identity = new HashMap<String, String>();
      Iterator<String> rit = resolved.iterator();
      while (rit.hasNext()) {
        String res = rit.next();
        identity.put(res, res);
      }
      newProps = newTreeBelowSegment(seg, newSourceID, null, identity, extraInfo);
    }
    return (newProps);
  }    

  /***************************************************************************
  **
  ** Shift the single given segment, modifying given parent, child, and siblings accordingly.
  */

  public void shiftSegment(String shiftID, Vector2D shift, String parent, List<String> siblings, List<String> children) {
    double dx = shift.getX();
    double dy = shift.getY();
    LinkSegment shiftSeg = getSegment(shiftID);
    shiftSeg.shiftBoth(dx, dy);
    if (parent != null) {
      LinkSegment parentSeg = getSegment(parent);
      if (parentSeg.isDegenerate()) {
        parentSeg.shiftStart(dx, dy);
      } else {
        parentSeg.shiftEnd(dx, dy);
      }
    }
    int size = siblings.size();
    for (int i = 0; i < size; i++) {
      String segID = siblings.get(i);
      LinkSegment seg = getSegment(segID);
      seg.shiftStart(dx, dy);
    }
    size = children.size();
    for (int i = 0; i < size; i++) {
      String segID = children.get(i);
      LinkSegment seg = getSegment(segID);
      seg.shiftStart(dx, dy);
    }
    return;
  }

  /***************************************************************************
  **
  ** Shift the straight run of segments, modifying given parent, and attached segments.
  */

  public void shiftStraightSegmentRunFromList(List<SegmentWithKids> straight, Vector2D shift, SegmentWithKids rootSK) {

    shiftStraightSegmentRunFromListCore(straight, shift, rootSK);
    dropAllZeroSegments();
    return;
  }

  
  /***************************************************************************
  **
  ** Shift the straight runs of segments, modifying given parent, and attached segments.
  */

  public void shiftStraightSegmentRunFromLists(List<SegmentWithKids> straight, List<SegmentWithKids> straight2, 
                                               Vector2D shift, SegmentWithKids rootSK) {
    
    //
    // Handle single-list cases (easy):
    //
                                                 
    if ((straight != null) && (straight2 == null)) {
      shiftStraightSegmentRunFromList(straight, shift, rootSK);
      return;
    } else if ((straight == null) && (straight2 != null)) {
      shiftStraightSegmentRunFromList(straight2, shift, rootSK);
      return;      
    } else if ((straight == null) && (straight2 == null)) {
      throw new IllegalArgumentException();
    }

    //
    // The key here is avoiding double-shifting of the first segment of the
    // second list.  We can skip the first segment of the second list if
    // there is more than one, but if there is only one, we need to still
    // handle the shift of the far endpoint and all segments attached to
    // it
    //
    
    shiftStraightSegmentRunFromListCore(straight, shift, rootSK);
    
    if (straight2.size() > 1) {
      ArrayList<SegmentWithKids> reduced = new ArrayList<SegmentWithKids>(straight2);
      reduced.remove(0);
      shiftStraightSegmentRunFromListCore(reduced, shift, rootSK);
      dropAllZeroSegments();
      return;
    }
    
    //
    // Just need to shift kids and end of lone first segment:
    //
    
    double dx = shift.getX();
    double dy = shift.getY();   
    SegmentWithKids swk = straight2.get(0);
    swk.segment.shiftEnd(dx, dy);    
    List<String> children = swk.getChildrenIDs();
    int size = children.size();
    for (int i = 0; i < size; i++) {
      String segID = children.get(i);
      LinkSegment seg = getSegment(segID);
      seg.shiftStart(dx, dy);
    }    

    dropAllZeroSegments();
    return;
  }  
  

  /***************************************************************************
  **
  ** Shift the straight run of segments, modifying given parent, and attached segments.
  */

  public void shiftStraightSegmentRun(List<String> straight, Vector2D shift, String parent, List<String> attached) {
    double dx = shift.getX();
    double dy = shift.getY();
    //
    // The straight run:
    //
    int numStr = straight.size();
    for (int i = 0; i < numStr; i++) {
      String segID = straight.get(i);
      LinkSegment shiftSeg = getSegment(segID);
      shiftSeg.shiftBoth(dx, dy);
    }
    
    //
    // The parent:
    //
    
    if (parent != null) {
      LinkSegment parentSeg = getSegment(parent);
      if (parentSeg.isDegenerate()) {
        parentSeg.shiftStart(dx, dy);
      } else {
        parentSeg.shiftEnd(dx, dy);
      }
    }
    
    //
    // All attached segments:
    //
    
    int size = attached.size();
    for (int i = 0; i < size; i++) {
      String segID = attached.get(i);
      LinkSegment seg = getSegment(segID);
      seg.shiftStart(dx, dy);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Clean up a jagged tee intersection
  */

  public boolean cleanupJaggedTee(double[] shifts, SegmentWithKids rootSK,
                                  List<SegmentWithKids> firstRun, List<SegmentWithKids> secondRun,
                                  SegmentWithKids eliminate) {
                                    
    if (shifts[0] != 0.0) {
      Vector2D currShift = new Vector2D(eliminate.segment.getRun());
      currShift.scale(shifts[0]);      
      shiftStraightSegmentRunFromList(firstRun, currShift, rootSK);
    }      
                              
    if (shifts[1] != 0.0) {
      Vector2D currShift = new Vector2D(eliminate.segment.getRun());
      currShift.scale(shifts[1]);
      shiftStraightSegmentRunFromList(secondRun, currShift, rootSK);
    }                                    
    
    //
    // Ditch the zero-length segments:
    //
    
    dropAllZeroSegments();
    return (true);
  }

  /***************************************************************************
  **
  ** Drop all zero-length segments:
  */

  public boolean dropAllZeroSegments() {
    
    ArrayList<LinkSegment> deadList = new ArrayList<LinkSegment>();
    
    Iterator<LinkSegment> sit = getSegments();
    int deadSize = 0;
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();    
      if (ls.isDegenerate() || (ls.getLength() != 0.0)) {
        continue;
      }
      deadList.add(ls);
      deadSize++;
    }
    
    for (int i = 0; i < deadSize; i++) {
      dropZeroSegment(deadList.get(i));
    }
    
    return (deadSize > 0);
  }
  
  /***************************************************************************
  **
  ** Drop a zero-length segment:
  */

  public void dropZeroSegment(LinkSegment eliminate) {
        
    if (eliminate.isDegenerate() || (eliminate.getLength() != 0.0)) {
      throw new IllegalArgumentException();
    }

    String elimID = eliminate.getID();
    if (elimID == null) {
      throw new IllegalArgumentException();
    }
    
    labels_.removeLabel(elimID);
    segments_.remove(elimID);
        
    String parent = eliminate.getParent();
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String myparent = ls.getParent();
      if (myparent == null) {
        continue;
      }
      if (myparent.equals(elimID)) {
        ls.setParent(parent);
      }
    }

    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      String ref = nextDrop.getConnectionTag();
      if (elimID.equals(ref)) {
        nextDrop.setConnectionTag(parent);
        LinkSegment parSeg = getSegment(parent);
        nextDrop.setConnectionSense(parSeg.isDegenerate() ? 
          LinkBusDrop.CONNECT_TO_START : LinkBusDrop.CONNECT_TO_END);
      }
    } 
    return;
  } 
  
  /***************************************************************************
  **
  ** Clean up a staggered run
  */

  public boolean cleanupStaggeredRun(double shift, SegmentWithKids rootSK,
                                     List<SegmentWithKids> straightRun, SegmentWithKids eliminate) {
                                       
    if (shift != 0.0) {
      Vector2D currShift = new Vector2D(eliminate.segment.getRun());
      currShift.scale(-shift);
      shiftStraightSegmentRunFromList(straightRun, currShift, rootSK);
    } 
    
    //
    // Ditch the zero-length segments:
    //
    
    dropAllZeroSegments();    

    return (true);
  }  
  
  /***************************************************************************
  **
  ** Compression
  */

  public void compress(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, Rectangle bounds) {   
    if (textPos_ != null) {
      shiftSupport(textPos_, emptyRows, emptyCols, bounds, -1.0, 1);    
    }
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      segmentShiftSupport(seg, emptyRows, emptyCols, bounds, -1.0, 1);
    }
    shiftDropEndsForExpandCompressOps(emptyRows, emptyCols, bounds, -1.0, 1);
    return;    
  }
  
  /***************************************************************************
  **
  ** Expansion
  */

  public void expand(SortedSet<Integer> newRows, SortedSet<Integer> newCols, int mult) {
    if (textPos_ != null) {
      shiftSupport(textPos_, newRows, newCols, null, 1.0, mult);    
    }
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      segmentShiftSupport(seg, newRows, newCols, null, 1.0, mult);
    }
    shiftDropEndsForExpandCompressOps(newRows, newCols, null, 1.0, mult);
    return;    
  }  

  /***************************************************************************
  **
  ** FullShift
  */

  public void fullShift(double dx, double dy) {
    if (textPos_ != null) {
      textPos_.setLocation(textPos_.getX() + dx, textPos_.getY() + dy);
    }
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      seg.shiftBoth(dx, dy);
    }
    shiftDropEnds(dx, dy, null);
    return;    
  }
  
  /***************************************************************************
  **
  ** FullShift
  */

  public void shiftPerMap(Map<Point2D, Point2D> mappedPositions, boolean forceToGrid) {
    if (textPos_ != null) {
      Point2D mapped = mappedPositions.get(textPos_);
      if (mapped != null) {
        textPos_.setLocation(mapped.getX(), mapped.getY());
      }
    }
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      Point2D oldPoint = seg.getStart();
      Point2D mapped = mappedPositions.get(oldPoint);
      if (mapped != null) {
        seg.setStart((Point2D)mapped.clone());
      }
      if (!seg.isDegenerate()) {
        oldPoint = seg.getEnd();
        mapped = mappedPositions.get(oldPoint);
        if (mapped != null) {
          seg.setEnd((Point2D)mapped.clone());
        }
      }
    }
    moveDropEndsPerMap(mappedPositions, forceToGrid);
    return;    
  }  
 
  /***************************************************************************
  **
  ** shift selected segments
  */

  public void shiftSelectedSegmentsAndDrops(double dx, double dy, LinkFragmentShifts fragShifts) {

    Iterator<String> sit = segments_.keySet().iterator();
    while (sit.hasNext()) {
      String segID = sit.next();
      LinkSegment seg = segments_.get(segID);
      LinkFragmentData lfd = fragShifts.segmentStarts.get(segID);
      if (lfd != null) {
        seg.shiftStart(dx, dy);
        if (lfd.expComp != null) {
          seg.shiftStart(lfd.expComp.getX(), lfd.expComp.getY());
        }
      }
      lfd = fragShifts.segmentEnds.get(segID);
      if (lfd != null) {
        seg.shiftEnd(dx, dy);
        if (lfd.expComp != null) {
          seg.shiftEnd(lfd.expComp.getX(), lfd.expComp.getY());
        }
      }
    }
    shiftSelectedDropEnds(dx, dy, fragShifts);
    return;    
  } 
  
  /***************************************************************************
  **
  ** Compression or expansion of FRAGMENTS belonging to a specific owner.
  ** 
  */

  public static void expandCompressSelectedSegmentsAndDrops(SortedSet<Integer> rows, SortedSet<Integer> cols, Rectangle bounds, 
                                                            boolean expand, int mult, LinkFragmentShifts fragShifts) { 
       
    double sign = (expand) ? 1.0 : -1.0;
    int modMult = (expand) ? mult : 1;
        
    Iterator<LinkFragmentData> sit = fragShifts.segmentStarts.values().iterator();
    while (sit.hasNext()) {
      LinkFragmentData lfd = sit.next();
      lfd.expandContract(rows,  cols, bounds, sign, modMult);
    }
    
    sit = fragShifts.segmentEnds.values().iterator();
    while (sit.hasNext()) {
      LinkFragmentData lfd = sit.next();
      lfd.expandContract(rows,  cols, bounds, sign, modMult);
    }
    
    sit = fragShifts.drops.values().iterator();
    while (sit.hasNext()) {
      LinkFragmentData lfd = sit.next();
      lfd.expandContract(rows,  cols, bounds, sign, modMult);
    }
    
    if (fragShifts.doSource != null) {
      fragShifts.doSource.expandContract(rows,  cols, bounds, sign, modMult);     
    }
    return;    
  }
  
  
  /***************************************************************************
  **
  ** Get back a segment ID for the point. (Brain-dead intersection)
  */
  
  public LinkSegmentID simpleIntersect(Point2D point) {
    Iterator<String> sit = segments_.keySet().iterator();
    while (sit.hasNext()) {
      String segKey = sit.next();
      LinkSegment seg = segments_.get(segKey);
      if (seg.getStart().equals(point)) {       
        LinkSegmentID lsid = LinkSegmentID.buildIDForType(segKey, LinkSegmentID.SEGMENT);
        lsid.tagIDWithEndpoint(LinkSegmentID.START);
        return (lsid);
      }
      if (!seg.isDegenerate()) {
        if (seg.getEnd().equals(point)) {
          LinkSegmentID lsid = LinkSegmentID.buildIDForType(segKey, LinkSegmentID.SEGMENT);
          lsid.tagIDWithEndpoint(LinkSegmentID.END);
          return (lsid);
        }
      }  
    }
    return (null);
  } 
 
  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public DistancedLinkSegID intersectBusSegment(DataAccessContext icx,
                                                Point2D pt,                                       
                                                Set<String> omittedDrops, 
                                                double intersectTol) {

    GenomeSource gSrc = icx.getGenomeSource();
    Genome genome = icx.getGenome();
    ThickInfo ti = maxThickForIntersect(gSrc, genome, icx.oso, intersectTol);  // kinda cheap!
    int upperBound = ti.maxThick;
    boolean twoPass = ti.twoPass;
    double upperTol = (double)upperBound / 2.0;
    TreeMap<Double, DistancedLinkSegID> bestFits = new TreeMap<Double, DistancedLinkSegID>();
    
    //
    // Handle the direct case:
    //
    
    if (isDirect()) {
      String targRef = getTargetDrop().getTargetRef();
      if ((omittedDrops != null) && omittedDrops.contains(targRef)) {
        return (null);
      }
      if (!linkIsInModel(gSrc, genome, icx.oso, targRef)) {
        return (null);
      }            
      LinkSegment fakeDirect = getDirectLinkPath(icx);
      return (twoPassCheckSegmentIntersect(icx, fakeDirect, pt, LinkSegmentID.DIRECT_LINK, targRef,
                                           intersectTol, upperTol, twoPass, false));
    } 
    
    //
    // Check for drop intersections.  If none, we will still have built up the
    // set of active segments to check:
    //
    
    HashSet<LinkSegment> set = new HashSet<LinkSegment>();    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      if (omittedDrops != null) {
        if ((dropRef != null) && omittedDrops.contains(dropRef)) {
          continue;
        }
      }
      if ((dropRef != null) && !linkIsInModel(gSrc, genome, icx.oso, dropRef)) {
        continue;
      }        
      LinkSegment dropSeg;
      int dropType;
      if (dropRef == null) {
        dropSeg = getSourceDropSegment(icx, true);
        dropType = LinkSegmentID.START_DROP;
      } else {
        dropSeg = getTargetDropSegment(drop, icx);
        dropType = LinkSegmentID.END_DROP;
      }
      DistancedLinkSegID retval = twoPassCheckSegmentIntersect(icx, dropSeg, pt, dropType, dropRef,
                                                               intersectTol, upperTol, twoPass, false); 
      if (retval != null) {
        bestFits.put(new Double(retval.distance), retval);
      }
      set.addAll(getSegmentsToRoot(drop));      
    }
    
    //
    // Check for intersections of active segments.  This is done in two passes.
    // The first pass catches true intersections of segments, with tagging of
    // endpoints.  Second pass only catches endpoints, so that points on the
    // outside of sharp corners get caught, in a fashion secondary to true line
    // intersections:
    //
    
    Iterator<LinkSegment> sit = set.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (!seg.isDegenerate()) {
        String segID = seg.getID();
        DistancedLinkSegID retval = twoPassCheckSegmentIntersect(icx, seg, pt, LinkSegmentID.SEGMENT, segID,
                                                                 intersectTol, upperTol, twoPass, false);
        if (retval != null) {
          bestFits.put(new Double(retval.distance), retval);
        }
      }
    }

    if (!bestFits.isEmpty()) {
      return (bestFits.get(bestFits.firstKey()));
    }
    
    //
    // Last chance to catch corner intersections on the outside of sharp corners:
    //
    
    sit = set.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (!seg.isDegenerate()) {
        String segID = seg.getID();
        DistancedLinkSegID retval = twoPassCheckSegmentIntersect(icx, seg, pt, LinkSegmentID.SEGMENT, segID,
                                                                 intersectTol, upperTol, twoPass, true);
        if (retval != null) {
          bestFits.put(new Double(retval.distance), retval);
        }
      }
    }
    if (!bestFits.isEmpty()) {
      return (bestFits.get(bestFits.firstKey()));
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Figure out the maxiumum thickness possible for an intersection
  */
  
  protected ThickInfo maxThickForIntersectSupport(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, 
                                                  boolean forModules, double intersectTol) {

    int defaultThick = SuggestedDrawStyle.trueThick(getDrawStyle().getThickness(), forModules);
    int maxThick = defaultThick;

    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      String dropRef = bd.getTargetRef();
      if ((dropRef != null) && !linkIsInModel(gSrc, genome, oso, dropRef)) {
        continue;
      }
           
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        PerLinkDrawStyle plds = bd.getDrawStyleForLink();
        SuggestedDrawStyle sdsFl = (plds == null) ? null : plds.getDrawStyle();        
        if (sdsFl != null) {
          if (sdsFl.getThickness() != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
            int nextThick = SuggestedDrawStyle.trueThick(sdsFl.getThickness(), forModules);
            if (nextThick > maxThick) {
              maxThick = nextThick;
            }
          }
        }
      }
      SuggestedDrawStyle sds = bd.getSpecialDropDrawStyle();
      if (sds != null) {
        if (sds.getThickness() != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
          int nextThick = SuggestedDrawStyle.trueThick(sds.getThickness(), forModules);
          if (nextThick > maxThick) {
            maxThick = nextThick;
          }
        }
      }
    }  
    
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      SuggestedDrawStyle sds = seg.getSpecialDrawStyle();
      if (sds != null) {
        if (sds.getThickness() != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
          int nextThick = SuggestedDrawStyle.trueThick(sds.getThickness(), forModules);
          if (nextThick > maxThick) {
            maxThick = nextThick;
          }
        }
      }
    }
     
    ThickInfo retval = new ThickInfo();
    retval.maxThick = maxThick;
    retval.twoPass = ((maxThick > SuggestedDrawStyle.drawThick(forModules)) && (maxThick > intersectTol * 2.0));
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Get the draw style for the given link.  May be null.
  */
  
  public PerLinkDrawStyle getDrawStyleForLinkage(String linkID) {    
    LinkBusDrop drop = getTargetDrop(linkID);
    return (drop.getDrawStyleForLink());
  }  
  
  /***************************************************************************
  **
  ** Set the draw style for the given linkage.  May be null.
  */
  
  public void setDrawStyleForLinkage(String linkID, PerLinkDrawStyle plds) {
    LinkBusDrop drop = getTargetDrop(linkID);
    drop.setDrawStyleForLink(plds);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the segment geometry the parent of any segmentID.  This handles all the specialized
  ** drop calculations.  Note the segment is "fake", just containing the geometry.  Null
  ** is returned for direct or start drop LSIDs.  The root seg is ignored; it's child get
  ** the start drop; it gets the start drop too.
  */
  
  public LinkSegment getSegmentGeometryForParent(LinkSegmentID segID, boolean forceToGrid, DataAccessContext icx) {
    
    if (segID.isDirect()) {
      return (null);
    } else if (segID.isForStartDrop()) {
      return (null);  
    } else if (segID.isForEndDrop()) {
      LinkBusDrop bd = getTargetDrop(segID);
      String lsId = bd.getConnectionTag();
      LinkSegment dropParent = getSegment(lsId);
      String dropParentID = dropParent.getID();
      int sense = bd.getConnectionSense();
      if (sense == LinkBusDrop.CONNECT_TO_START) {
        String metaParent = dropParent.getParent();
        if (metaParent != null) {
          dropParentID = metaParent;
        } else {
          return (getSourceDropSegment(icx, forceToGrid));
        }
      }
      return (new LinkSegment(getSegment(dropParentID)));
    } else {
      LinkSegment thisSeg = getSegment(segID);
      String linkSegParent = thisSeg.getParent();
      if (thisSeg.isDegenerate() || (linkSegParent == null)) {
        return (getSourceDropSegment(icx, forceToGrid));
      } else {
        return (new LinkSegment(getSegment(linkSegParent)));
      }    
    }
  }  

  /***************************************************************************
  **
  ** Check for intersection with a rectangle.
  */
  
  public List<LinkSegmentID> intersectBusSegmentsWithRect(DataAccessContext icx,
                                                          OverlayStateOracle oso,
                                                          Rectangle2D testRect,
                                                          boolean includeRoot) {  
  
    return (intersectBusSegmentsWithShape(icx, oso, testRect, includeRoot));
  } 
  
  /***************************************************************************
  **
  ** Check for intersection with a shape.
  */
  
  public List<LinkSegmentID> intersectBusSegmentsWithShape(DataAccessContext icx,
                                                           OverlayStateOracle oso,
                                                           Shape testShape,
                                                           boolean includeRoot) {

    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
    
    //
    // Handle the direct case:
    //
    
    if (isDirect()) {
      String targRef = getTargetDrop().getTargetRef();
      if ((targRef != null) && !linkIsInModel(icx.getGenomeSource(), icx.getGenome(), oso, targRef)) {
        return (retval);
      }
      LinkSegment fakeDirect = getDirectLinkPath(icx);
      LinkSegmentID result = checkForSegmentInShape(fakeDirect, testShape, 
                                                    LinkSegmentID.DIRECT_LINK, targRef);
      if (result != null) {
        retval.add(result);
      }
      return (retval);
    } 
    
    //
    // Check for drop intersections.  If none, we will still have built up the
    // set of active segments to check:
    //
    
    HashSet<LinkSegment> set = new HashSet<LinkSegment>();    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      if ((dropRef != null) && !linkIsInModel(icx.getGenomeSource(), icx.getGenome(), oso, dropRef)) {
        continue;
      }
      LinkSegment dropSeg;
      int dropType;
      if (dropRef == null) {
        dropSeg = getSourceDropSegment(icx, true);
        dropType = LinkSegmentID.START_DROP;
      } else {
        dropSeg = getTargetDropSegment(drop, icx);
        dropType = LinkSegmentID.END_DROP;
      }
      LinkSegmentID result = checkForSegmentInShape(dropSeg, testShape, dropType, dropRef);
      if (result != null) {
        retval.add(result);
      }
      set.addAll(getSegmentsToRoot(drop));      
    }
    
    //
    // Check for intersections of active segments.
    //
    
    Iterator<LinkSegment> sit = set.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (!seg.isDegenerate()) {
        String segID = seg.getID();
        LinkSegmentID result = checkForSegmentInShape(seg, testShape, LinkSegmentID.SEGMENT, segID);
        if (result != null) {
          retval.add(result);
        }
      }
    }
    
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Move the linkage by the given amount.  Start point can be null. 
  */
  
  public void moveBusLinkSegments(LinkSegmentID[] segIDs, Point2D strt, double dx, 
                                  double dy) {
    int num = segIDs.length;
    // We do not move direct links
    if ((num == 1) && segIDs[0].isDirect()) {
      return;
    }
    HashSet<SegCookie> seenCookies = new HashSet<SegCookie>();
    for (int i = 0; i < num; i++) {
      moveBusLinkCore(segIDs[i], strt, dx, dy, seenCookies);
    }
    return;
  }

/***************************************************************************
  **
  ** Add a link segment.  Used to build a single-path tree step-by-step.
  */
  
  public void addSegmentInSeries(LinkSegment newSeg) {
    
    if (drops_.size() != 2) {
      throw new IllegalStateException();
    }
    
    //
    // If we do not have any segments, we need to add a degenerate
    // root segment and make it a parent of the incoming segment (unless
    // the incoming segment is degenerate itself).  We also need to fixup 
    // the bus drops to connect to the new segment.
    //
    
    if (segments_.size() == 0) {
      LinkSegment rootSeg = new LinkSegment(newSeg.getStart(), null);
      addSegment(rootSeg);
      String rootID = rootSeg.getID();
      boolean rootOnly = newSeg.isDegenerate();
      String addID = null;
      if (!rootOnly) {
        LinkSegment addSeg = new LinkSegment(newSeg);
        addSeg.setID(null);
        addSeg.setParent(rootID);
        addSegment(addSeg);
        addID = addSeg.getID();
      }

      Iterator<LinkBusDrop> dit = drops_.iterator();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        if (drop.getDropType() == LinkBusDrop.START_DROP) {
          drop.setConnectionSense(LinkBusDrop.CONNECT_TO_START);
          drop.setConnectionTag(rootID);
        } else {
          if (rootOnly) {
            drop.setConnectionSense(LinkBusDrop.CONNECT_TO_START);
            drop.setConnectionTag(rootID);
          } else {
            drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
            drop.setConnectionTag(addID);
          }
        }
      }
      
      return;
    }      
          
    //
    // If we do have segments, go to the end drop to find the parent
    // segment, and glue the new segment into that location.
    //
    
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }

      String tag = drop.getConnectionTag();
      LinkSegment addSeg = new LinkSegment(newSeg);
      addSeg.setID(null);
      addSeg.setParent(tag);
      addSegment(addSeg);
      drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
      drop.setConnectionTag(addSeg.getID());
    }    
 
    return;
  }
  
  /***************************************************************************
  **
  ** Add a Link Segment
  */
  
  public void addSegment(LinkSegment newSeg) {
    if (newSeg.getID() == null) {
      newSeg.setID(labels_.getNextLabel());
    } else {
      if (!labels_.addExistingLabel(newSeg.getID())) {
        System.err.println(newSeg.getID() + " is not unique");
        throw new IllegalArgumentException();
      }
    }    
    segments_.put(newSeg.getID(), newSeg);
    return;
  }
 
  /***************************************************************************
  **
  ** Answer if we only have a direct path (no corners at all)
  */
  
  public boolean isDirect() {
    return (segments_.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Get the ordered list, leaf to root, of SegmentID starting a the given end drop
  ** Degenerate root is dropped; have IDs of end and start drops included
  */
  
  public List<LinkSegmentID> getSegmentIDsToRootForEndDrop(LinkBusDrop drop) {   
    return (getSegmentIDsToRootForEndDrop(drop, false));
  }
  
  /***************************************************************************
  **
  ** Get the ordered list, leaf to root, of SegmentID starting a the given end drop
  ** Degenerate root is optionally dropped; have IDs of end and start drops included
  */
  
  private List<LinkSegmentID> getSegmentIDsToRootForEndDrop(LinkBusDrop drop, boolean includeDegen) { 
    
    if (drop.getDropType() != LinkBusDrop.END_DROP) {
      throw new IllegalArgumentException();
    }
    
    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
   
    if (isDirect()) {
      List<String> allLinks = getLinkageList();
      String linkRef = allLinks.get(0);
      retval.add(LinkSegmentID.buildIDForDirect(linkRef));
      return (retval);
    }
    
    retval.add(LinkSegmentID.buildIDForEndDrop(drop.getTargetRef()));  
    
    List<LinkSegment> segsToRoot = getSegmentsToRoot(drop);
    int segSize = segsToRoot.size();
    for (int j = 0; j < segSize; j++) {
      LinkSegment seg = segsToRoot.get(j);
      if (!includeDegen && seg.isDegenerate()) {  // Don't care about degenerate root
        continue;
      }
      retval.add(LinkSegmentID.buildIDForSegment(seg.getID()));
    }
    
    retval.add(LinkSegmentID.buildIDForStartDrop());

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of linkages that this bus is used for
  */
  
  public List<String> getLinkageList() {
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      LinkBusDrop drop = drops.next();
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        retval.add(drop.getTargetRef());
      }
    }
    return (retval);
  }  
   
 /***************************************************************************
  **
  ** Get the segments back to the root from the drop
  */
  
  public List<LinkSegment> getSegmentsToRoot(LinkBusDrop drop) {
    String segID = drop.getConnectionTag();
    //
    // With no-segment buses, the segID will be null
    //
    if (segID == null) {
      return (new ArrayList<LinkSegment>());
    }
    LinkSegment need = segments_.get(segID);   
    if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) &&
        (drop.getDropType() == LinkBusDrop.END_DROP)) {
      LinkSegment parent = segments_.get(need.getParent());
      if (parent != null) {  // may only be 1 point in degenerate bus
        need = parent;
      }
    }
    return (getSegmentsToRoot(need));
  }
  
  /***************************************************************************
  **
  ** Get the segments back to the root, including this segment
  */
  
  public List<LinkSegment> getSegmentsToRoot(LinkSegment seg) {
    //
    // Just crank up the tree:
    //
    ArrayList<LinkSegment> retval = new ArrayList<LinkSegment>();
    LinkSegment curr = seg;
    retval.add(curr);
    String parent = curr.getParent();    
    while (parent != null) {
      curr = segments_.get(parent);
      retval.add(curr);
      parent = curr.getParent();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of segment points going back up to the root:
  */
  
  public Set<Point2D> getPointsToRoot(LinkSegmentID segID) {
    
    HashSet<Point2D> retval = new HashSet<Point2D>();    
    if (segID.isDirect() || segID.isForStartDrop()) {
      return (retval);
    }
    
    List<LinkSegment> s2r;
    if (segID.isForSegment()) {
      s2r = getSegmentsToRoot(getSegment(segID));
    } else {
      s2r = getSegmentsToRoot(getDrop(segID));
    }
    
    //
    // Just crank up the tree:
    //
 
    int numS = s2r.size();
    for (int i = 0; i < numS; i++) {
      LinkSegment seg = s2r.get(i);
      retval.add((Point2D)seg.getStart().clone());
    }
  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of segment points going down to all leaves through the given segment:
  */
  
  public Set<Point2D> getAllPointsToLeaves(LinkSegmentID segID) {
    
    HashSet<Point2D> retval = new HashSet<Point2D>();    
    if (segID.isDirect()) {
      return (retval);
    }
    if (segID.isForStartDrop()) {
      return (getAllTreePoints());
    }
    if (segID.isForEndDrop()) {
      return (retval);
    }
    
    String targSeg = segID.getLinkSegTag();
   
    //
    // Just crank up the tree:
    //
    
    Set<String> linksThru = resolveLinkagesThroughSegment(segID);
    Iterator<LinkBusDrop> bdit = getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop drop = bdit.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        continue;
      }
      if (!linksThru.contains(ref)) {
        continue;
      }
      List<LinkSegment> s2r = getSegmentsToRoot(drop);     
      int numS = s2r.size();
      for (int i = 0; i < numS; i++) {
        LinkSegment seg = s2r.get(i);
        retval.add((Point2D)seg.getEnd().clone());
        if (seg.getID().equals(targSeg)) {
          break;
        }
        retval.add((Point2D)seg.getStart().clone());
      }
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of all segment points in the tree:
  */
  
  public Set<Point2D> getAllTreePoints() {
    HashSet<Point2D> retval = new HashSet<Point2D>();
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      retval.add((Point2D)seg.getStart().clone());
      if (!seg.isDegenerate()) {
        retval.add((Point2D)seg.getEnd().clone());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Given a link segmentID, fill in two sets.  One with all the points
  ** below the segment (including the endpoint), and one with all the points
  ** not in that set (points back to root, plus all sibling and cousin points):
  */
  
  public void splitTreePoints(LinkSegmentID lsid, Set<Point2D> below, Set<Point2D> notBelow) {
    below.clear();
    notBelow.clear();
    below.addAll(getAllPointsToLeaves(lsid));
    notBelow.addAll(getAllTreePoints());
    notBelow.removeAll(below);
    return;
  }

  /***************************************************************************
  **
  ** Get the segment geometry for any segmentID.  This handles all the specialized
  ** drop calculations.  Note the segment is "fake", just containing the geometry:
  */
  
  public LinkSegment getSegmentGeometryForID(LinkSegmentID segID, DataAccessContext icx, boolean forceToGrid) {
    
    if (segID.isDirect()) {
      if (!isDirect()) {
        throw new IllegalArgumentException();
      }
      return (getDirectLinkPath(icx));
    } else if (segID.isForStartDrop()) {
      return (getSourceDropSegment(icx, forceToGrid));
    } else if (segID.isForEndDrop()) {
      LinkBusDrop drop = getTargetDrop(segID);
      return (getTargetDropSegment(drop, icx));
    } else {
      LinkSegment seg = getSegment(segID);
      return (new LinkSegment(seg));
    }
  } 
  
 /***************************************************************************
  **
  ** Get the segment with the given Segment ID
  */
  
  public LinkSegment getSegment(LinkSegmentID lsid) {
    if (!lsid.isForSegment()) {
      throw new IllegalArgumentException();
    }
    String id = lsid.getLinkSegTag();
    return (segments_.get(id));
  }    
   
  /***************************************************************************
  **
  ** Get the drop for the specified target from an end drop
  */
  
  public LinkBusDrop getTargetDrop(LinkSegmentID segID) {
    if (segID.isDirect()) {
      return (getTargetDrop());
    } else if (segID.isForEndDrop()) {
      String targID = segID.getEndDropLinkRef();
      return (getTargetDrop(targID));
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the drop for the specified link
  */
  
  public LinkBusDrop getTargetDrop(String linkID) {
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      String tref = bd.getTargetRef();
      if ((tref != null) && (tref.equals(linkID))) {
        return (bd);
      }
    }
    throw new IllegalArgumentException();
  }   
  
 /***************************************************************************
  **
  ** Get the target for a single drop tree
  */
  
  public LinkBusDrop getTargetDrop() {
    if (!isSingleDropTree()) {
      throw new IllegalStateException();
    }
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      String tref = bd.getTargetRef();
      if (tref != null) {
        return (bd);
      }
    }
    throw new IllegalArgumentException();
  }    
  
  /***************************************************************************
  **
  ** Answer if we only have a single path
  */
  
  public boolean isSingleDropTree() {
    return (drops_.size() == 2);
  }
  
  /***************************************************************************
  **
  ** Get the drops
  */  
  
  public Iterator<LinkBusDrop> getDrops() {
    return (drops_.iterator());   
  } 
  
  /***************************************************************************
  **
  ** Return a properties that has been pruned to a smaller set of links
  */

  public LinkProperties buildReducedProperties(Set<String> remainingLinks) {    
    //
    // Kinda a hack:
    //
    LinkProperties retval = this.clone();
    HashMap<String, String> dummyLinkMap = new HashMap<String, String>();
    Iterator<String> sit = remainingLinks.iterator();
    while (sit.hasNext()) {
      String linkID = sit.next();
      dummyLinkMap.put(linkID, linkID);
    }
    HashMap<String, String> dummyNodeMap = new HashMap<String, String>();
    dummyNodeMap.put(srcTag_, srcTag_);
    retval.mapToNewLink(dummyLinkMap, dummyNodeMap);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Use to copy props to new link.
  */

  public void mapToNewLink(Map<String, String> linkMap, Map<String, String> nodeMap) {
    srcTag_ = nodeMap.get(srcTag_);
    //
    // Not all drops are needed in the new layout:
    //
    ArrayList<LinkBusDrop> newDrops = new ArrayList<LinkBusDrop>();
    int size = drops_.size();
    for (int i = 0; i < size; i++) {
      LinkBusDrop bd = drops_.get(i);
      if (bd.mapToNewLink(linkMap, nodeMap)) {
        newDrops.add(bd);
      } 
    }
    drops_ = newDrops;
    
    //
    // Prune segments to get rid of unused ones:
    //
    
    removeDanglingSegments();
    return;
    
  }
  
  /***************************************************************************
  **
  ** Get rid of dangling link segments
  */

  public void removeDanglingSegments() {    
    HashSet<String> deadSet = new HashSet<String>(segments_.keySet());
    int size = drops_.size();    
    for (int i = 0; i < size; i++) {
      LinkBusDrop bd = drops_.get(i);
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        List<LinkSegment> seg2root = getSegmentsToRoot(bd);
        int segSize = seg2root.size();
        for (int j = 0; j < segSize; j++) {
          String id = seg2root.get(j).getID();
          if (deadSet.contains(id)) {
            deadSet.remove(id);
          }
        }
      }
    }
    //
    // With the dead set in hand, shift bus drops to the end
    // of a parent segment if needed.
    //
    
    for (int i = 0; i < size; i++) {
      LinkBusDrop bd = drops_.get(i);
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        if (bd.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
          String connectTag = bd.getConnectionTag();
          if (deadSet.contains(connectTag)) {
            LinkSegment lseg = getSegment(connectTag);
            String parent = lseg.getParent();
            if (parent == null) {
              throw new IllegalStateException();
            }
            LinkSegment pseg = getSegment(parent);
            if (!pseg.isDegenerate()) {
              bd.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
            }
            bd.setConnectionTag(parent);
          }
        }
      }
    }
    
    Iterator<String> dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      String dead = dsit.next();
      labels_.removeLabel(dead);
      segments_.remove(dead);
    }
 
    return;
  }   

 /***************************************************************************
  **
  ** Get LinkBusDrop from a segment ID
  */
  
  public LinkBusDrop getDrop(LinkSegmentID segID) {
    if (!segID.isForDrop()) {
      throw new IllegalArgumentException();
    }

    boolean isForStart = segID.isForStartDrop();
    boolean isForEnd = segID.isForEndDrop();
    String idDropRef = (isForEnd) ? segID.getEndDropLinkRef() : null;
        
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      int dropType = drop.getDropType();
      if (isForStart && (dropType == LinkBusDrop.START_DROP)) {
        return (drop);
      } else if (isForEnd && (dropType == LinkBusDrop.END_DROP) && dropRef.equals(idDropRef)) {
        return (drop);
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Merge another tree with only one path in it
  */
  
  public void mergeSinglePathTree(LinkProperties spTree, DataAccessContext icx) {
    //
    // We need to add the given single-path tree into our tree, where it is
    // probable that the single path shares the geometry of part of our
    // tree.
    //
    
    //
    // If we do not match on source, or argument has more than one path,
    // we punt!
    //
    
    if ((!spTree.getSourceTag().equals(getSourceTag())) ||
        (!spTree.isSingleDropTree())) {
      throw new IllegalArgumentException();
    }
    
    if (spTree.isDirect()) {
      spTree.splitNoSegmentBus(icx);
    }
    if (isDirect()) {
      splitNoSegmentBus(icx);
    }    

    //
    // We need to match segments.  First thing we do is shift as needed.
    // NOTE: To fix BT-06-15-05:7, we need to shift to match BEFORE we
    // do chopping.  Otherwise, chopping doesn't happen unless start
    // geometry is untouched.
    //
    
    //
    // WJRL 4/11/08 The problem with this is that two identical trees, where
    // there is an additional link attached into the root drop of one will
    // totally throw off the matching algorithm.  This needs a complete overhaul.
    // 
  
    LinkSegment myRoot = getRootSegment();    
    LinkSegment hisRoot = spTree.getRootSegment();
       
    Point2D myStrt = myRoot.getStart();
    Point2D hisStrt = hisRoot.getStart();    
    double delX = 0.0;
    double delY = 0.0;
    if (!myStrt.equals(hisStrt)) {
      delX = myStrt.getX() - hisStrt.getX();
      delY = myStrt.getY() - hisStrt.getY();
      Iterator<LinkSegment> sit = spTree.getSegments();
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        seg.shiftBoth(delX, delY);
      }
    }    
    
    //
    // Even if the path geometry matches, we may have new segment endpoints.
    // We will fold in new endpoints by looking for intersections in both
    // source and target, and add the endpoints we find.
    //
    
    chop(spTree);
    spTree.chop(this);
    
    //
    // Get the terminal bus drop:
    //
    
    LinkBusDrop spDrop = spTree.getTargetDrop();

    //
    // Get the path to the root.  If that path has one segment (the root),
    // then all we do is add the bus drop to our list and that's it:
    //

    List<LinkSegment> pathToRoot = spTree.getSegmentsToRoot(spDrop);
    int pathSize = pathToRoot.size();
    if (pathSize == 1) {
      LinkBusDrop drop = generateBusDrop(spDrop, myRoot.getID(), LinkBusDrop.END_DROP,
                                         LinkBusDrop.CONNECT_TO_START);
      drop.transferSpecialStyles(spDrop);      
      addDrop(drop);
      return;
    }
 
    //
    // Go from root to drop, matching segments as we go.  Do not include the
    // root!  When we can no longer match segments, just add them.  Note we 
    // have to move backwards:
    //

    String lastMatch = myRoot.getID();
    boolean matching = true;  
    for (int i = pathSize - 2; i >= 0; i--) {
      LinkSegment spTreeSeg = pathToRoot.get(i);
      //
      // Matching operations
      //
      if (matching) {
        Point2D slpStrt = spTreeSeg.getStart();
        Point2D slpEnd = spTreeSeg.getEnd();
        Iterator<LinkSegment> vit = getSegments();
        boolean hadMatch = false;
        while (vit.hasNext()) {
          LinkSegment bpSeg = vit.next();
          // Skip root segment
          if (bpSeg.isDegenerate()) {
            continue;
          }
          Point2D bpStrt = bpSeg.getStart();
          Point2D bpEnd = bpSeg.getEnd();
          // Endpoints and parents match
          if (((lastMatch.equals(bpSeg.getParent()))) &&
              (bpStrt.equals(slpStrt)) && (bpEnd.equals(slpEnd))) {
            hadMatch = true;
            lastMatch = bpSeg.getID();
            break;      
          }
        }
        if (!hadMatch) {
          matching = false;
        }
      }
      //
      // Non-matching operations
      //
      if (!matching) {
        String newId = labels_.getNextLabel();        
        spTreeSeg.setID(newId);
        spTreeSeg.setParent(lastMatch);
        segments_.put(newId, spTreeSeg);
        lastMatch = newId;
      }
    }
    
    //
    // Done with segments, so just add the final bus drop:
    //
    
    LinkBusDrop drop = generateBusDrop(spDrop, lastMatch, LinkBusDrop.END_DROP,
                                       LinkBusDrop.CONNECT_TO_END);
    drop.transferSpecialStyles(spDrop);    
    addDrop(drop);

    return;
  }
  
  /***************************************************************************
  **
  ** Chop up a tree at the intersections
  */
  
  public void chop(LinkProperties other) {
    //
    // Crank through the other segments.  Take each segment endpoint and
    // see if it intersects any of our segments.  Chop segment if it does.
    // Then chop our drops if they have an intersection: BUT we can't know,
    // since we don't know precise endpoint of drop!  FIX ME!!
    //
    
    ArrayList<ChopRequest> chopRequests = new ArrayList<ChopRequest>();
    
    Iterator<LinkSegment> osit = other.getSegments();
    while (osit.hasNext()) {
      LinkSegment seg = osit.next();
      Point2D testPoint = (seg.isDegenerate()) ? seg.getStart() : seg.getEnd();
      Vector2D trav = seg.getRun();
      Iterator<LinkSegment> msit = this.getSegments();
      while (msit.hasNext()) {
        LinkSegment mseg = msit.next();
        Vector2D travM = mseg.getRun();
        // An attempt to deal with BT-03-23-05:11:
        if ((trav != null) && (travM != null) && 
            (trav.isZero() || travM.isZero() || !trav.equals(travM))) {
          continue;     
        }
        ChopRequest chp = intersectForChop(mseg, testPoint);
        if (chp != null) {
          chopRequests.add(chp);
        }
      }
    }
    
    //
    // Now process the chop requests
    //
    
    int chopNum = chopRequests.size();
    for (int i = 0; i < chopNum; i++) {
      ChopRequest cr = chopRequests.get(i);
      if (cr.segmentID == null) {  // thrown out below
        continue;
      }
      LinkSegment ls = getSegment(cr.segmentID);
      LinkSegment[] newSegs = ls.split(cr.splitPt);
      if (newSegs.length == 1) {  // Endpoint intersected; no split occurred SHOULD NOT HAPPEN
        throw new IllegalStateException();
      }
      replaceSegment(ls, newSegs[0], newSegs[1]);
      //
      // Once a segments is replaced, pending chop request that reference the
      // thrown away segment will fail.  Modify pending requests.
      //
      for (int j = (i + 1); j < chopNum; j++) {
        ChopRequest cr2 = chopRequests.get(j);
        if (cr2.segmentID == null) {  // thrown out below
          continue;
        }
        if (cr2.segmentID.equals(cr.segmentID)) {
          ChopRequest chp = intersectForChop(newSegs[0], cr2.splitPt);
          if (chp != null) {
            cr2.segmentID = newSegs[0].getID();
          } else {
            chp = intersectForChop(newSegs[1], cr2.splitPt);
            if (chp != null) {
              cr2.segmentID = newSegs[1].getID();
            } else {
              cr2.segmentID = null;  //Ignore it now... Will this happen?
            }
          }
        }
        
      }
    }
    
    return;    
  }
  
 /***************************************************************************
  **
  ** Check for chop intersection
  */
  
  private ChopRequest intersectForChop(LinkSegment mseg, Point2D testPoint) {  
    if (mseg.intersectsStart(testPoint, 0.01) != null) {
      return (null);
    }
    if (mseg.isDegenerate()) {
      return (null);
    }
    if (mseg.intersectsEnd(testPoint, 0.01) != null) {
      return (null);
    }
    if (mseg.intersects(testPoint, 0.01) != null) {
      return (new ChopRequest(testPoint, mseg.getID()));
    }
    return (null);
  }  
  

  /***************************************************************************
  **
  ** Given a list of segments, return them in a list that goes parent->child.
  ** Throws an illegal argument if they do not form such a chain.  Can vote
  ** to omit root segment, if it is present
  */
  
  public List<LinkSegment> orderSegmentsParentToChild(List<LinkSegment> segments, boolean omitRoot) {
      
    int numSegs = segments.size();
    if (numSegs == 0) {
      return (new ArrayList<LinkSegment>());
    }
    
    //
    // Get set of IDs:
    //    
    
    HashSet<String> segIDs = new HashSet<String>();
    for (int i = 0; i < numSegs; i++) {
      LinkSegment seg = segments.get(i);
      segIDs.add(seg.getID());
    }
    
    //
    // If more than one parent is not in the set, we have an error.
    // If a parent shows up more than once (siblings) we have an error.
    // If nobody shows up as the top parent, we have an error.
    //
    
    LinkSegment firstSeg = null;
    HashSet<String> seenParents = new HashSet<String>();    
    boolean rootIsParent = false;
    
    for (int i = 0; i < numSegs; i++) {
      LinkSegment seg = segments.get(i);
      String parent = seg.getParent();
      if (parent == null) {
        if (rootIsParent) {
          throw new IllegalArgumentException();
        }
        rootIsParent = true;
      } else {
        if (seenParents.contains(parent)) {
          throw new IllegalArgumentException();
        }
        seenParents.add(parent);
      }
      
      if ((parent == null) || (!segIDs.contains(parent))) {
        if (firstSeg != null) {
          throw new IllegalArgumentException();
        }
        firstSeg = seg;
      }
    }
    if (firstSeg == null) {  // if segments were circular (shouldn't happen)
      throw new IllegalArgumentException();
    }    
    
    //
    // Crank thru the list, looking for each child in turn:
    //
    
    String currParent = firstSeg.getID();
    ArrayList<LinkSegment> retval = new ArrayList<LinkSegment>();
    retval.add(firstSeg);
        
    while (retval.size() < numSegs) {
      for (int i = 0; i < numSegs; i++) {
        LinkSegment seg = segments.get(i);
        String parent = seg.getParent();
        if ((parent != null) && currParent.equals(parent)) {
          retval.add(seg);
          currParent = seg.getID();
          break;
        }
      }    
    }
    
    if (rootIsParent && omitRoot) {
      retval.remove(0);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Given a link segment, find the chain of segments it belongs to that
  ** supports the identical linkage set
  */
  
  public List<LinkSegment> findHomologousLinkChain(LinkSegment seedSegment, boolean omitRoot) {
       
    //
    // For each end drop, crawl back to the root and
    // tag each segment with the link ID.  Also record
    // that info for the seedSegment.
    //
    
    String seedID = seedSegment.getID();
    HashSet<String> seedLinks = new HashSet<String>();

    HashMap<String, Set<String>> segIDToLinks = new HashMap<String, Set<String>>();
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String rootRef = drop.getTargetRef();
      if (rootRef == null) {
        continue;
      }      
      List<LinkSegment> segsToRoot = getSegmentsToRoot(drop);
      int segSize = segsToRoot.size();
      for (int j = 0; j < segSize; j++) {
        String id = segsToRoot.get(j).getID();      
        Set<String> linksForSeg = segIDToLinks.get(id);
        if (linksForSeg == null) {
          linksForSeg = new HashSet<String>();
          segIDToLinks.put(id, linksForSeg);
        }
        linksForSeg.add(rootRef);
        if (id.equals(seedID)) {
          seedLinks.add(rootRef);
        }
      }
    }
    
    //
    // Find all the segments that match the link set of the seed segment
    //
    
    ArrayList<LinkSegment> matches = new ArrayList<LinkSegment>();
    Iterator<String> stlit = segIDToLinks.keySet().iterator();
    while (stlit.hasNext()) {
      String id = stlit.next();
      Set<String> linksForSeg = segIDToLinks.get(id);
      if (linksForSeg.equals(seedLinks)) {
        matches.add(getSegment(id));
      }
    }
    
    List<LinkSegment> pToC = orderSegmentsParentToChild(matches, omitRoot);     
    return (pToC);
  }

  
  /***************************************************************************
  **
  ** Get a split point
  */
  
  public Point2D getSplitPoint(LinkSegmentID segID, Point2D splitPtIn, DataAccessContext icx) { 
    Point2D splitPt = (Point2D)splitPtIn.clone();
    LinkSegment lseg = getSegmentGeometryForID(segID, icx, true);
    double frac = lseg.fractionOfRun(splitPtIn);
    splitPt = lseg.pointAtFraction(frac);
    return (splitPt);     
  }  
 
  /***************************************************************************
  **
  ** Find a link segment that supports the given set of links
  */
  
  public LinkSegmentID findSegmentSupportingLinkSet(BusProperties rootBus, 
                                                    LinkSegmentID rootMatchingSegID,
                                                    DataAccessContext rcxRoot,
                                                    Set<String> linksToMatch, DataAccessContext icxi) {
 
    DataAccessContext icx = new DataAccessContext(icxi, rcxRoot.getGenome(), rcxRoot.getLayout());
    LinkSegment rootMatchingSeg = rootBus.getSegmentGeometryForID(rootMatchingSegID, icx, true);

    HashSet<String> allLinks = new HashSet<String>(getLinkageList());
    boolean rootDropOK = allLinks.equals(linksToMatch);
     
    //
    // For no-segment trees, we all go through the only ID:
    //
    
    if (isDirect()) {
      if (!rootDropOK) {
        throw new IllegalStateException();
      }
      String linkRef = allLinks.iterator().next();
      return (LinkSegmentID.buildIDForDirect(linkRef));
    }
    
    LinkSegment myRoot = getRootSegment();
    String myRootID = myRoot.getID();

    //
    // For each end drop, crawl back to the root to record 
    // the segments it uses.  Build up the map of all links passing
    // through each segment.
    //

    HashMap<String, Set<String>> segIDToLinks = new HashMap<String, Set<String>>();
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String rootRef = drop.getTargetRef();
      if (rootRef == null) {
        continue;
      }      
      List<LinkSegment> segsToRoot = getSegmentsToRoot(drop);
      int segSize = segsToRoot.size();
      for (int j = 0; j < segSize; j++) {
        String id = segsToRoot.get(j).getID();      
        Set<String> linksForSeg = segIDToLinks.get(id);
        if (linksForSeg == null) {
          linksForSeg = new HashSet<String>();
          segIDToLinks.put(id, linksForSeg);
        }
        linksForSeg.add(rootRef);
      }
    }

    //
    // Find a segment that matches the given set exactly.  If there are more
    // than one, come up with the best match to the segment used in the root
    // layout
    //
    
    HashSet<String> retvals = new HashSet<String>();
    Iterator<String> stlit = segIDToLinks.keySet().iterator();
    while (stlit.hasNext()) {
      String id = stlit.next();
      Set<String> linksForSeg = segIDToLinks.get(id);
      if (linksForSeg.equals(linksToMatch)) {
        retvals.add(id);
      }
    }

    //
    // If no "real" segments match, check if the drop segment works ok:
    //
    
    if (retvals.isEmpty()) {
      if (rootDropOK) {  // Should not happen (if start drop is OK, root segment should be OK too...)
        throw new IllegalStateException();
      }
      if (linksToMatch.size() == 1) {
        String matchLink = linksToMatch.iterator().next();
        return (LinkSegmentID.buildIDForEndDrop(matchLink));
      } else {
        return (null);
      }
    }      

    //
    // If only one segment matches, go for it:
    //
    
    if (retvals.size() == 1) {
      String segID = retvals.iterator().next();
      if (segID.equals(myRootID)) {
        if (!rootDropOK) {
          throw new IllegalStateException();
        }
        return (LinkSegmentID.buildIDForStartDrop());
      } else {
        return (LinkSegmentID.buildIDForSegment(segID));
      }
    }

    //
    // Organize the candidates parent to child:
    //
    
    ArrayList<LinkSegment> chainMembers = new ArrayList<LinkSegment>();
    Iterator<String> rit = retvals.iterator();
    while (rit.hasNext()) {
      String id = rit.next();
      chainMembers.add(getSegment(id));
    }    
    List<LinkSegment> myChain = orderSegmentsParentToChild(chainMembers, true);
    
    //
    // Look for a direct translating match:
    //
    
    int mcNum = myChain.size();
    for (int i = 0; i < mcNum; i++) {
      LinkSegment currSeg = myChain.get(i);
      if (currSeg.isSimpleTranslation(rootMatchingSeg)) {
        return (LinkSegmentID.buildIDForSegment(currSeg.getID()));
      }            
    }

    //
    // Look for a direct scaled match:
    //
    
    for (int i = 0; i < mcNum; i++) {
      LinkSegment currSeg = myChain.get(i);
      if (currSeg.isSimpleScaling(rootMatchingSeg)) {
        return (LinkSegmentID.buildIDForSegment(currSeg.getID()));
      }            
    }    
    
    List<LinkSegment> rootChain = rootBus.findHomologousLinkChain(rootMatchingSeg, true);       
    int numRC = rootChain.size();
    
    //
    // Look for a homologous translating match:
    //
    
    for (int j = 0; j < numRC; j++) {
      LinkSegment currRootSeg = rootChain.get(j);
      for (int i = 0; i < mcNum; i++) {
        LinkSegment currSeg = myChain.get(i);
        if (currSeg.isSimpleTranslation(currRootSeg)) {
          return (LinkSegmentID.buildIDForSegment(currSeg.getID()));
        }            
      }
    }
 
    //
    // Look for a homologous scaled match:
    //

    for (int j = 0; j < numRC; j++) {
      LinkSegment currRootSeg = rootChain.get(j);
      for (int i = 0; i < mcNum; i++) {
        LinkSegment currSeg = myChain.get(i);
        if (currSeg.isSimpleScaling(currRootSeg)) {
          return (LinkSegmentID.buildIDForSegment(currSeg.getID()));
        }            
      }
    }    

    //
    // Give up. Send back the highest segment in the chain
    //
    
    if (rootDropOK) {
      return (LinkSegmentID.buildIDForStartDrop());
    } else {
      LinkSegment topSeg = myChain.get(0);
      return (LinkSegmentID.buildIDForSegment(topSeg.getID()));
    }
  }
  
  /***************************************************************************
  **
  ** Find a link segment that best hosts a label following a new link layout
  */
  
  public LinkSegmentID findBestSegmentForLabel(GenomeSource gSrc, Set<String> linksToMatch, int cardinality, Genome genome, OverlayStateOracle oso) {
 
    HashSet<String> allLinks = new HashSet<String>(getLinkageList());
     
    //
    // For no-segment trees, we all go through the only ID:
    //
    
    if (isDirect()) {
      String linkRef = allLinks.iterator().next();
      return (LinkSegmentID.buildIDForDirect(linkRef));
    }
    
    Set<String> labelLinks = getLabelLinkages(gSrc, genome, oso);
   
    //
    // Find a segment that matches the given set exactly.  If there is more
    // than one, try to match the cardinality.  If there is none, try to
    // find the segment with the largest set of conforming links, with no
    // unconforming links.  If that does not work, try to find a segment (or drop) that
    // matches the underlying set of links providing the label contents.  If
    // that does not work, use the root drop.
    //
    
    HashMap<LinkSegmentID, Integer> cardinalities = new HashMap<LinkSegmentID, Integer>();
    HashSet<LinkSegmentID> conforming = new HashSet<LinkSegmentID>();
    HashSet<LinkSegmentID> bestFit = new HashSet<LinkSegmentID>();
    HashSet<LinkSegmentID> matchDefLinks = new HashSet<LinkSegmentID>();
    int minDiff = Integer.MAX_VALUE;
    
    Iterator<String> sit = segments_.keySet().iterator();
    while (sit.hasNext()) {
      String segKey = sit.next();
      LinkSegment seg = segments_.get(segKey);
      if (!seg.isDegenerate()) {
        LinkSegmentID lsid = LinkSegmentID.buildIDForType(segKey, LinkSegmentID.SEGMENT);
        minDiff = collectSegmentCandidates(linksToMatch, labelLinks, lsid, cardinalities,
                                           conforming, bestFit, matchDefLinks, minDiff, null, seg);
      }
    }
    
    //
    // Add drops to the pile:
    //
    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String ref = drop.getTargetRef();       
      LinkSegmentID lsid = (ref == null) ? LinkSegmentID.buildIDForType(null, LinkSegmentID.START_DROP)
                                         : LinkSegmentID.buildIDForType(ref, LinkSegmentID.END_DROP);
      minDiff = collectSegmentCandidates(linksToMatch, labelLinks, lsid, cardinalities,
                                         conforming, bestFit, matchDefLinks, minDiff, drop, null);
    }

    //
    // Look for single results:
    //
    
    HashSet<LinkSegmentID> checkCard = null;
    if (conforming.size() == 1) {
      return (conforming.iterator().next());
    } else if (conforming.size() > 1) {
      checkCard = conforming;
    } else if (bestFit.size() == 1) {
      return (bestFit.iterator().next());
    } else if (bestFit.size() > 1) {
      checkCard = bestFit;
    } else if (matchDefLinks.size() == 1) {
      return (matchDefLinks.iterator().next());
    } else if (matchDefLinks.size() > 1) { 
      checkCard = matchDefLinks;
    }
 
    //
    // Go with cardinality match:
    //

    if (checkCard != null) {
      int minConformDiff = Integer.MAX_VALUE;
      LinkSegmentID bestCand = null;
      Iterator<LinkSegmentID> cit = checkCard.iterator();
      while (cit.hasNext()) {
        LinkSegmentID lsid = cit.next();
        Integer cardVal = cardinalities.get(lsid);
        int diff = Math.abs(cardVal.intValue() - cardinality);
        if (diff == 0) {
          return (lsid);
        }
        if (diff < minConformDiff) {
          minConformDiff = diff;
          bestCand = lsid;
        }
      }
      return (bestCand);
    }    
       
    return (LinkSegmentID.buildIDForStartDrop());  
  }  
  
  /***************************************************************************
  **
  ** We are handed a link segment ID.  From this, figure out what underlying 
  ** linkages are passing through the given link segment.
  *
  *
  // 10/29/13 Ditch this. We are trying to remove genome arguments from layout operations,
  // and this is just not needed
  
  public Set<String> resolveLinkagesThroughSegment(LinkSegmentID segID, Genome genome) {
    //
    // We are handed a genome; this is currently just a legacy argument, and you
    // will note it is not used.  When used with module linkages, this would
    // mean we would need to do different things with the argument, sticking
    // part of these ops in the subclasses.  I tend to want to toss the argument,
    // but there is something to be said to use it to see if the linkages actually
    // exist in the model in question (not a requirement if it is in the layout).
    // But right now the code works fine without this check...so skip it for
    // now but don't toss out the argument quite yet.  WJRL 6/8/09
    //
    
    // WJRL 3/16/12: Useful to have the reduced version, so provide that as well,
    // plus this signature for legacy purposes and maybe the check discussed above!
    
    return (resolveLinkagesThroughSegment(segID));
  }  
  
  /***************************************************************************
  **
  ** We are handed a link segment ID.  From this, figure out what underlying 
  ** linkages are passing through the given link segment.
  */
  
  public Set<String> resolveLinkagesThroughSegment(LinkSegmentID segID) {
     
    HashSet<String> retval = new HashSet<String>();
    
    //
    // If the segment is just a drop, we have either one linkage to return,
    // or all linkages, depending on the end.  Handle this case.
    //
    
    if (segID.isForStartDrop() || segID.isDirect()) {
      retval.addAll(getLinkageList());
      return (retval);
    }
    
    if (segID.isForEndDrop()) {
      retval.add(segID.getEndDropLinkRef());
      return (retval);
    }

    String segTag = segID.getLinkSegTag();
    
    //
    // Segment is internal.  For each end drop, crawl back to the root to see 
    // if it passes through the given segment.
    //

    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (isSegmentAnAncestor(segTag, drop)) {
        retval.add(drop.getTargetRef());        
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** We are handed a pile of link segments.  From this, figure out what underlying 
  ** linkages are passing through the entire collection of link segments.
  */
  
  public Set<String> resolveLinkagesThroughSegments(Set<LinkSegmentID> segIDs, InvertedLinkProps ilp) {
     
    HashSet<String> retval = new HashSet<String>();
    
    //
    // If the segment is just a drop, we have either one linkage to return,
    // or all linkages, depending on the end.  Handle this case.
    //
    
    HashSet<String> lsTags = new HashSet<String>();
    Iterator<LinkSegmentID> lsit = segIDs.iterator();
    while (lsit.hasNext()) {
      LinkSegmentID segID = lsit.next();
      if (segID.isForStartDrop() || segID.isDirect()) {
        retval.addAll(getLinkageList());
        return (retval);
      }
      if (segID.isForEndDrop()) {
        retval.add(segID.getEndDropLinkRef());
      } else if (segID.isForSegment()) {
        lsTags.add(segID.getLinkSegTag());
      }
    }
 
    Iterator<String> lstit = lsTags.iterator();
    while (lstit.hasNext()) {
      String lsTag = lstit.next();
      retval.addAll(ilp.linksThruSeg(lsTag));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Tell us what the relationship is between the two segments
  */
  
  public int segRelationship(LinkSegmentID segID, LinkSegmentID otherSegID) {
   
    if (segID.equals(otherSegID)) {
      return (SAME);
    }
 
    if (segID.isForStartDrop()) {
      return (DECENDANT);
    }
    
    if (otherSegID.isForStartDrop()) {
      return (ANCESTOR);
    }
     
    LinkSegmentID myParent = getSegmentIDForParent(segID);
    LinkSegmentID otherParent = getSegmentIDForParent(otherSegID);

    if (myParent.isForStartDrop()) {
      return (SIBLING);
    }   
    
    if (myParent.equals(otherParent)) {
      return (SIBLING);
    }
    
    if (segID.isForEndDrop() && otherSegID.isForEndDrop()) {
      return (OTHER);
    }

    if (otherParent.equals(segID)) {
      return (DECENDANT);
    }
    
    if (myParent.equals(otherSegID)) {
      return (ANCESTOR);
    }
    
    if (myParent.isForStartDrop()) {
      return (DECENDANT);
    }
    
    if (otherParent.isForStartDrop()) {
      return (ANCESTOR);
    }
    //
    // At this point, we are guaranteed to be working with two internal
    // segments since we are starting from parents.
    //
    
    if (otherSegID.isForSegment()) {
      String myPLST = myParent.getLinkSegTag();
      String oLST = otherSegID.getLinkSegTag();
    
      String nextID = myPLST;
      do {
        LinkSegment curr = segments_.get(nextID);
        if (oLST.equals(curr.getID())) {
          return (ANCESTOR);
        }
        nextID = curr.getParent();
      } while (nextID != null);
    }
 
    
    if (segID.isForSegment()) {
      String myLST = segID.getLinkSegTag();
      String oPLST = otherParent.getLinkSegTag();    
      String nextID = oPLST;
      do {
        LinkSegment curr = segments_.get(nextID);
        if (myLST.equals(curr.getID())) {
          return (DECENDANT);
        }
        nextID = curr.getParent();
      } while (nextID != null);
    }
    
    return (OTHER);
  }  
  
  /***************************************************************************
  **
  ** Find a link segment that best hosts a label following a new link layout
  */
  
  public int collectSegmentCandidates(Set<String> linksToMatch, Set<String> labelLinks, LinkSegmentID lsid, 
                                      Map<LinkSegmentID, Integer> cardinalities,
                                      Set<LinkSegmentID> conforming, Set<LinkSegmentID> bestFit, 
                                      Set<LinkSegmentID> matchDefLinks, int minDiff, LinkBusDrop drop, LinkSegment seg) {
    
    Set<String> resLinks = resolveLinkagesThroughSegment(lsid);
    int myCard = (seg != null) ? getSegmentsToRoot(seg).size() : getSegmentsToRoot(drop).size() + 1;
    
    if (resLinks.equals(linksToMatch)) {
      conforming.add(lsid);
      cardinalities.put(lsid, new Integer(myCard));
    } else {
      // L = links to match; R = res links
      // size(L U R) = size(L)
      // want size(L - R) to be minimized
      boolean haveAFit = false;
      HashSet<String> union = new HashSet<String>(linksToMatch);
      union.addAll(resLinks);
      if (union.size() == linksToMatch.size()) {
        HashSet<String> diff = new HashSet<String>(linksToMatch);
        diff.removeAll(resLinks);
        int numDiff = diff.size();
        if (numDiff < minDiff) {
          bestFit.clear();
          bestFit.add(lsid);
          minDiff = numDiff;
          cardinalities.put(lsid, new Integer(myCard));
          haveAFit = true;
        } else if (numDiff == minDiff) {
          bestFit.add(lsid);          
          cardinalities.put(lsid, new Integer(myCard));
          haveAFit = true;
        }
      }
      if (!haveAFit) {
        if (resLinks.equals(labelLinks)) {
          matchDefLinks.add(lsid);
          cardinalities.put(lsid, new Integer(myCard));            
        }
      }
    }
 
    return (minDiff);  
  }      
 

 /***************************************************************************
  **
  ** Get a list of path points for a single-path bus:
  */

  public List<Point2D> getPointListForSinglePath() { 
    if (drops_.size() != 2) {
      throw new IllegalStateException();
    }
    
    LinkBusDrop targDrop = null;
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      if (nextDrop.getDropType() == LinkBusDrop.END_DROP) {
        targDrop = nextDrop;
        break;
      }
    }
    if (targDrop == null) {
      throw new IllegalStateException();
    }
    
    //
    // Get points back to root
    //
    
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    List<LinkSegment> pathToRoot = getSegmentsToRoot(targDrop);
    Iterator<LinkSegment> sit = pathToRoot.iterator();
    while (sit.hasNext()) {
      LinkSegment segToRoot = sit.next();
      if (segToRoot.isDegenerate()) {
        retval.add(segToRoot.getStart());
      } else {
        retval.add(segToRoot.getEnd());
      }
    }

    Collections.reverse(retval);
    return (retval);
  } 
  
/***************************************************************************
  **
  ** Get a list of link segment IDs for a single-path bus:
  */

  public List<LinkSegmentID> getSegIDListForSinglePath() { 
    if (drops_.size() != 2) {
      throw new IllegalStateException();
    }
    
    LinkBusDrop targDrop = null;
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      if (nextDrop.getDropType() == LinkBusDrop.END_DROP) {
        targDrop = nextDrop;
        break;
      }
    }
    if (targDrop == null) {
      throw new IllegalStateException();
    }
    
    //
    // Get segment IDs back to root
    //
    
    List<LinkSegmentID> retval = getSegmentIDsToRootForEndDrop(targDrop);  
    Collections.reverse(retval);
    return (retval);
  }     

  /***************************************************************************
  **
  ** Answers if the corner point has simple connectivity (one inbound segment
  ** and one outbound segment).
  */
  
  public boolean hasSimpleConnectivity(LinkSegmentID segID) {

    //
    // Sanity checks
    //
    
    if (isDirect()) {  // should not happen
      return (false);
    }
    
    if (!segID.isTaggedWithEndpoint()) {
      return (false);
    }    
    
    //
    // Dealing with links.  Go get the segment.  If we are asking
    // about the start, go see if there are any other siblings with
    // the same parent, or bus drops off the parent end.  If we are 
    // asking about the end, see if we have more than one child, or
    // a bus drop off the end.
    //
 
    if (segID.isForSegment()) {
      LinkSegment seg = getSegment(segID);
      if (!seg.isDegenerate()) {
        if (segID.endEndpointIsTagged()) {
          return (!hasMultipleChildren(seg));
        }
        if (segID.startEndpointIsTagged()) {
          return (!hasSiblings(seg));
        }
      } else {  // degenerate
        return (!hasMultipleChildren(seg));
      }
    }
    
    //
    // Intersected drop case.  For a start drop, multiple children
    // of the root mess us up.  For an end drop, multiple children
    // of our parent mess us up.
    //
    
    if (segID.isForStartDrop()) {
      LinkSegment root = getRootSegment();
      return (!hasMultipleChildren(root));
    }
    
    LinkBusDrop drop = getTargetDrop(segID);
    if (drop == null) {  // should not happen...
      return (false);
    }
    String parentID = drop.getConnectionTag();
    LinkSegment seg = getSegment(parentID);
    return (!hasMultipleChildren(seg));
  }
  
 
  /***************************************************************************
  **
  ** Answers if this link tree is degenerate, i.e. removing the one remaining
  ** point leaves no link segments at all
  */
  
  public LinkBusDrop treeIsDegenerate() {
    //
    // To be completely degenerate, there can only be one degenerate root
    // segment, a start drop, and an end drop.
    //
    
    if ((segments_.size() != 1) || (drops_.size() != 2)) {
      return (null);
    }
    LinkSegment seg = segments_.values().iterator().next();
    if (!seg.isDegenerate()) {
      throw new IllegalStateException();
    }
    
    Iterator<LinkBusDrop> dit = getDrops();
    boolean haveStart = false;
    boolean haveEnd = false;
    LinkBusDrop retval = null;
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        haveStart = true;
      } else if (drop.getDropType() == LinkBusDrop.END_DROP) {
        haveEnd = true;
        retval = drop;
      }
    }
    if (haveStart && haveEnd) {
      return (retval);
    } else {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Normalize segment ID for simple connected case. This is shifting a tagged
  ** endpoint to always refer to the start point of a segment.
  */
  
  private LinkSegmentID normalizeSimpleConnectedSegmentID(LinkSegmentID segID) {
    //
    // Shift the link point reference so that we are always dealing with
    // a start point.  **The leaf case where this cannot be done has
    // already been handled.**
    //
    
    // This identifies the case where the LinkSegmentID has a tagged endpoint
    // which corresponds to the attachment point to the node.
    if (segID.isBusNodeConnection()) {
      throw new IllegalArgumentException();
    }
    
    // We'd better have an endpoint tagged, or what are we doing here?
    if (!segID.isTaggedWithEndpoint()) {
      throw new IllegalArgumentException();
    } 
 
    LinkSegment root = getRootSegment();
    String rootID = root.getID();
    
    //
    // Link ID transformations
    //
    
    if (segID.isForSegment()) {
      String linkRef = segID.getLinkSegTag();
      //
      // The start endpoint is the one that is tagged, so we are happy!
      // No change needed, BUT we want to map root segment ID to its lone child instead, so wait on that case.
      //
      if (segID.startEndpointIsTagged() && !linkRef.equals(rootID)) {
        return (segID);
      }
       
      //
      // At this point, we are guaranteed to have only one link child (we have simple connectivity).
      // Find that kid, and use it as the normalized ID, with its START point tagged. 
      //
      
      Iterator<LinkSegment> sit = segments_.values().iterator();
      while (sit.hasNext()) {
        LinkSegment nextSeg = sit.next();
        if (segID.equals(nextSeg.getID())) {
          continue;
        }
        String nextParent = nextSeg.getParent();
        if (nextParent == null) {
          continue;
        }
        if (nextParent.equals(linkRef)) {
          LinkSegmentID retval = LinkSegmentID.buildIDForSegment(nextSeg.getID());
          retval.tagIDWithEndpoint(LinkSegmentID.START);
          return (retval);
        }
      }
      throw new IllegalArgumentException();
    }      
      
    //
    // Start drop case.  We do NOT return the degenerate root segment! We return its
    // lone child instead.
    //

    if (segID.isForStartDrop()) {
      LinkBusDrop drop = getRootDrop();
      String rootTag = drop.getConnectionTag();
      Iterator<LinkSegment> sit = segments_.values().iterator();
      while (sit.hasNext()) {
        LinkSegment nextSeg = sit.next();
        String nextParent = nextSeg.getParent();
        if (nextParent == null) {
          continue;
        }
        if (nextParent.equals(rootTag)) {
          LinkSegmentID retval = LinkSegmentID.buildIDForSegment(nextSeg.getID());
          retval.tagIDWithEndpoint(LinkSegmentID.START);
          return (retval);
        }
      }
    }
    
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Identify the leaf case, where we need to eliminate the place where 
  ** an end drop connects to a terminal link. If segID is for an end drop, it
  ** returns the parent true segment tag.  If it is for a segment with a tagged
  ** endpoint, and it parents an end drop, it returns the segment tag.
  */
  
  public String haveLeafCase(LinkSegmentID segID) {
    
    //
    // Leaf cases are where the single child is a drop.
    //

    // SegID identifies the drop, return the parent segment tag.
    if (segID.isForEndDrop()) {
      LinkBusDrop drop = getDrop(segID);   
      return (drop.getConnectionTag());
    }
    
    // SegID identifies the parent of the drop, return that SegID
    if (segID.isForSegment() && segID.endEndpointIsTagged()) {
      String linkRef = segID.getLinkSegTag();
      Iterator<LinkBusDrop> dit = getDrops();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        if ((drop.getDropType() == LinkBusDrop.END_DROP) &&
            (drop.getConnectionTag().equals(linkRef))) {
          return (linkRef);
        }
      }
    }      
    return (null);
  }    

 
  
  /***************************************************************************
  **
  ** Make a single drop tree into a direct link
  */
  
  public void makeDirect() {
    
    //
    // First of all, if we are not a single drop tree, we have no business being here.
    //
    
    if (!isSingleDropTree()) {    
      throw new IllegalArgumentException();
    }
    
    //
    // Note that the way we are doing this, the special line style of the parent
    // is the one that survives.  Maintain this approach when going to direct
    // link too.
    //
    
    clearOutAllSegments();
    
    Iterator<LinkBusDrop> dit = getDrops();   
    while (dit.hasNext()) {
      LinkBusDrop currDrop = dit.next();
      if (currDrop.getDropType() == LinkBusDrop.START_DROP) {
        SuggestedDrawStyle sds = currDrop.getSpecialDropDrawStyle();
        if (sds != null) {
          SuggestedDrawStyle sdsM = getDrawStyle();
          setDrawStyle(sdsM.mergeOverrides(sds));
        } 
      }
      currDrop.setConnectionSense(LinkBusDrop.NO_SEGMENT_CONNECTION);
      currDrop.setConnectionTag(null);
      currDrop.setDrawStyleForDrop(null);
        // NOTE per-link style is still being retained...
    }
    
    return;
  }  

  /***************************************************************************
  **
  ** Dump All segments, including labels!
  */
  
  protected void clearOutAllSegments() {  
    Iterator<String> kit = segments_.keySet().iterator();
    while (kit.hasNext()) {
      String deadID = kit.next();
      labels_.removeLabel(deadID);
    }
    segments_.clear();
    return;
  }
  
  /***************************************************************************
  **
  ** Remove a useless corner, i.e. the parent has ONE kid, and the kid is in the
  ** same direction.
  */
  
  public void removeUselessCorner(LinkSegmentID segID, InvertedLinkProps ilp) {
    
    //
    // Remember, we are not handling the direct case here, since it has no corners at all, much
    // less useless ones!
    //
    
    //
    // Note that the way we are doing this, the special line style of the parent
    // is the one that survives.  Maintain this approach when going to direct
    // link too.
    //
    
    //
    // If removing the one remaining corner takes us to a non-segment state,
    // we need to handle that conversion. Remember, a direct link has two link 
    // drops but NO segments, including no degenerate segment root segment. 
    //
    
    LinkBusDrop drop = treeIsDegenerate(); // == one degenerate segment only.
    if (drop != null) {
      clearOutAllSegments();
      if (segID.isForSegment()) {
        ilp.removeSegment(segID.getLinkSegTag());
      }
      if (drops_.size() != 2) {
        throw new IllegalStateException();
      }
      Iterator<LinkBusDrop> dit = getDrops();   
      while (dit.hasNext()) {
        LinkBusDrop currDrop = dit.next();
        if (currDrop.getDropType() == LinkBusDrop.START_DROP) {
          SuggestedDrawStyle sds = currDrop.getSpecialDropDrawStyle();
          if (sds != null) {   
            SuggestedDrawStyle sdsM = getDrawStyle();
            setDrawStyle(sdsM.mergeOverrides(sds));
          }
        }
        currDrop.setConnectionSense(LinkBusDrop.NO_SEGMENT_CONNECTION);
        currDrop.setConnectionTag(null);
        currDrop.setDrawStyleForDrop(null);
        // NOTE per-link style is still being retained...
      }
      //
      // Geometry can stay the same!
      return;
    }

    //
    // First handle throwing out a useless point that starts an END drop:
    //
    
  
    //
    // If the segment ID we receive identifies a bus drop intersection, 
    // convert that into an internal drop case (i.e. we will have the segment
    // tag of the true segment).
    //
    
   
    
    // Identifies where the kid is an end drop.
    String leafResult = haveLeafCase(segID);
    if (leafResult != null) {
      LinkSegment seg = segments_.get(leafResult);
      String parentID = seg.getParent();
      if (parentID == null) { // Can't be null, since we handled the degenerate case already!
        throw new IllegalArgumentException();
      }
      LinkSegment parentSeg = segments_.get(parentID);
      String segFLID = seg.getID();
      // We always connect to the end, unless it is degenerate and doesn't have an end:
      int sense = (parentSeg.isDegenerate()) ? LinkBusDrop.CONNECT_TO_START : LinkBusDrop.CONNECT_TO_END;
      Point2D newStart = (parentSeg.isDegenerate()) ? parentSeg.getStart() : parentSeg.getEnd();
      Iterator<LinkBusDrop> dit = getDrops();
      while (dit.hasNext()) {
        LinkBusDrop drop2 = dit.next();
        if (drop2.getDropType() == LinkBusDrop.START_DROP) {
          continue;
        }
        String conTag = drop2.getConnectionTag();
        if (conTag.equals(segFLID)) {
          drop2.setConnectionTag(parentID);
          drop2.setConnectionSense(sense);
          String linkID = drop2.getTargetRef();
          ilp.reparentDrop(linkID, segID.getLinkSegTag(), parentID);
          ilp.setGeometry(LinkSegmentID.buildIDForEndDrop(linkID), newStart);
        }
      }    
      // In the leaf case, the final segment is dropped, and ALL the drops are reparented at the parentID (though, being useless, 
      // there should only be one drop!)
      labels_.removeLabel(segFLID);
      segments_.remove(segFLID);
      ilp.removeSegment(segFLID);
      ilp.registerRemoval(segFLID, parentID);
      return;
    }
    
    //
    // We get the segmentID normalized; it will always have the start point tagged.
    // Why do we care? Because it is shifting the deletion downstream to ditch the
    // child and move everybody onto an extended parent segment.
    //
    segID = normalizeSimpleConnectedSegmentID(segID); 
    
    //
    // throw out the child segment, fix up bus drops as needed.
    // If our parent is null, we must be the root, which should have
    // been previously handled.
    //
    
    LinkSegment seg = getSegment(segID);
    String parentID = seg.getParent();
    if (parentID == null) {
      throw new IllegalArgumentException();
    }
    LinkSegment parentSeg = segments_.get(parentID);
    reparentChildrenAndDelete(seg, parentSeg.isDegenerate(), ilp);
    ilp.registerRemoval(seg.getID(), parentID);
    return;
  }  

  /***************************************************************************
  **
  ** Remove a corner
  */
  
  public void removeCorner(LinkSegmentID segID) {
    
    //
    // First of all, if we do not have simple connectivity, we have no business
    // being here.
    //
    
    if (!hasSimpleConnectivity(segID)) {
      throw new IllegalArgumentException();
    }
    
    //
    // Note that the way we are doing this, the special line style of the parent
    // is the one that survives.  Maintain this approach when going to direct
    // link too.
    //
    
    //
    // If removing the one remaining corner takes us to a non-segment state,
    // we need to handle that conversion
    //
    
    LinkBusDrop drop = treeIsDegenerate();
    if (drop != null) {
      clearOutAllSegments();
      if (drops_.size() != 2) {
        throw new IllegalStateException();
      }
      Iterator<LinkBusDrop> dit = getDrops();   
      while (dit.hasNext()) {
        LinkBusDrop currDrop = dit.next();
        if (currDrop.getDropType() == LinkBusDrop.START_DROP) {
          SuggestedDrawStyle sds = currDrop.getSpecialDropDrawStyle();
          if (sds != null) {   
            SuggestedDrawStyle sdsM = getDrawStyle();
            setDrawStyle(sdsM.mergeOverrides(sds));
          }
        }
        currDrop.setConnectionSense(LinkBusDrop.NO_SEGMENT_CONNECTION);
        currDrop.setConnectionTag(null);
        currDrop.setDrawStyleForDrop(null);
        // NOTE per-link style is still being retained...
      }
      return;
    }

    //
    // If the segment ID we receive identifies a bus drop intersection, 
    // convert that into an internal drop case.
    //
    
    String leafResult = haveLeafCase(segID);
    if (leafResult != null) {
      LinkSegment seg = segments_.get(leafResult);
      String parentID = seg.getParent();
      if (parentID == null) {
        throw new IllegalArgumentException();
      }
      LinkSegment parentSeg = segments_.get(parentID);
      handleLeafCase(seg, parentSeg.isDegenerate());
      return;
    }
    
    segID = normalizeSimpleConnectedSegmentID(segID); 
    
    //
    // throw out the child segment, fix up bus drops as needed.
    // If our parent is null, we must be the root, which should have
    // been previously handled.
    //
    
    LinkSegment seg = getSegment(segID);
    String parentID = seg.getParent();
    if (parentID == null) {
      throw new IllegalArgumentException();
    }
    LinkSegment parentSeg = segments_.get(parentID);
    reparentChildrenAndDelete(seg, parentSeg.isDegenerate(), null);
    return;
  }
  
  /***************************************************************************
  **
  ** Remove a corner
  */
  
  public boolean dropZeroLengthSeg(LinkSegmentID segID) {
    
    //
    // First of all, if we have simple connectivity, we have no business
    // being here.
    //
    
    if (hasSimpleConnectivity(segID)) {
      throw new IllegalArgumentException();
    }
    
    //
    // Note that the way we are doing this, the special line style of the parent
    // is the one that survives.  Maintain this approach when going to direct
    // link too.
    //
    
    //
    // If removing the one remaining corner takes us to a non-segment state,
    // we need to handle that conversion
    //
    /*
    LinkBusDrop drop = treeIsDegenerate();
    if (drop != null) {
      clearOutAllSegments();
      if (drops_.size() != 2) {
        throw new IllegalStateException();
      }
      Iterator dit = getDrops();   
      while (dit.hasNext()) {
        LinkBusDrop currDrop = (LinkBusDrop)dit.next();
        if (currDrop.getDropType() == LinkBusDrop.START_DROP) {
          SuggestedDrawStyle sds = currDrop.getSpecialDropDrawStyle();
          if (sds != null) {   
            SuggestedDrawStyle sdsM = getDrawStyle();
            setDrawStyle(sdsM.mergeOverrides(sds));
          }
        }
        currDrop.setConnectionSense(LinkBusDrop.NO_SEGMENT_CONNECTION);
        currDrop.setConnectionTag(null);
        currDrop.setDrawStyleForDrop(null);
        // NOTE per-link style is still being retained...
      }
      return;
    }
   */
    
    //
    // Note:  We could have a degenerate case where the root segment has children, but
    // lies on the launch pad.  That cannot be repaired.  We could also have a case where
    // a segment end lies on a landing pad, and has children.  That can also not be fixed!
    //
    
    // Simple connectivity case CAN be fixed, but that was done elsewhere
    if (segID.isForStartDrop()) {
      return (false);
    }
       
    //
    // If the segment ID we receive identifies a bus drop intersection, 
    // convert that into an internal drop case.
    //
     
    String leafResult = haveLeafCase(segID);
    if (leafResult != null) {
      LinkSegment seg = segments_.get(leafResult);
      if (hasMultipleChildren(seg)) { // Zero-length drop has siblings->we can't dump it!
        return (false);
      }
      String parentID = seg.getParent();
      if (parentID == null) {
        return (false);
        //throw new IllegalArgumentException();
      }
      LinkSegment parentSeg = segments_.get(parentID);
      handleLeafCase(seg, parentSeg.isDegenerate());
      return (true);
    }
    
    segID = normalizeSimpleConnectedSegmentID(segID); 
    
    //
    // throw out the child segment, fix up bus drops as needed.
    // If our parent is null, we must be the root, which should have
    // been previously handled.
    //
    
    LinkSegment seg = getSegment(segID);
    String parentID = seg.getParent();
    if (parentID == null) {
      return (false);
      //throw new IllegalArgumentException();
    }
    LinkSegment parentSeg = segments_.get(parentID);
    reparentChildrenAndDelete(seg, parentSeg.isDegenerate(), null);
    return (true);
  }
 
  /***************************************************************************
  **
  ** Get the root segment
  */
  
  public LinkSegment getRootSegment() {
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment test = sit.next();
      if (test.getParent() == null) {
        return (test);
      }
    }
    System.err.println("No root segment " + this.getSourceTag());
    throw new IllegalStateException();
  }
  

  /***************************************************************************
  **
  ** Add a Drop
  */
  
  public void addDrop(LinkBusDrop newDrop) {
    drops_.add(newDrop);
    return;
  }
  
  /***************************************************************************
  **
  ** Replace a Link Segment with one or two segments
  */
  
  public String[] replaceSegment(LinkSegment oldSeg, 
                                 LinkSegment newSeg1, LinkSegment newSeg2) {
          
    //
    // Unlike the SingleLinkProperties, LinkSegments are not held in an
    // ordered list.  We need to:
    // 1) Remove existing link
    // 2) Get the two new links ready, remembering their newly assigned IDs.
    // 3) Fixup parents. The guy matching the old end is the child
    //    of the other.  Anybody referencing the old link now references
    //    the new end.
    // 4) Fixup drops.  Anybody drop referencing the old start needs to
    //    reference the new start; anybody referencing the old end needs 
    //    to reference the new end.
    // 5) Add the two new links
    //                              

    String oldID = oldSeg.getID();
    labels_.removeLabel(oldID);
    segments_.remove(oldID);
    String newId1 = labels_.getNextLabel();
    newSeg1.setID(newId1);
    newSeg1.setParent(oldSeg.getParent());
    String newId2 = labels_.getNextLabel();
    newSeg2.setID(newId2);
    newSeg2.setParent(newId1);
    
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      String parent = seg.getParent();
      if ((parent != null) && parent.equals(oldID)) {
        seg.setParent(newId2);
      }
    }
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getConnectionTag().equals(oldID)) {
        int sense = drop.getConnectionSense();
        drop.setConnectionTag((sense == LinkBusDrop.CONNECT_TO_END) ? newId2 : newId1);
      }
    }
    
    segments_.put(newId1, newSeg1);
    segments_.put(newId2, newSeg2);
    String[] retval = new String[2];
    retval[0] = newId1;
    retval[1] = newId2;
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Support for writing to XML
  **
  */
  
  public void writeXMLSupport(PrintWriter out, Indenter ind) {
    if (textPos_ != null) {
      out.print("labelX=\"");
      out.print(textPos_.getX());
      out.print("\" labelY=\"");
      out.print(textPos_.getY());
      out.print("\" ");
    }
    if (textDirTag_ != null) {
      out.print("labelDir=\"");
      out.print(textDirTag_);
      out.print("\" ");
    }
 
    out.print("src=\"");
    out.print(srcTag_);
    out.println("\" >");

    ind.up();    
    drawStyle_.writeXML(out, ind);
      
    ind.indent(); 
    out.println("<segments>");
    ind.up();
    Iterator<LinkSegment> segs = segments_.values().iterator();
    while (segs.hasNext()) {
      LinkSegment link = segs.next();
      link.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</segments>");
    ind.indent();    
    out.println("<drops>");
    ind.up();
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      LinkBusDrop drop = drops.next();
      drop.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</drops>");
    return;
  }     
  
  /***************************************************************************
  **
  ** Get the renderer.  This has gotten bogus. The different renderers do
  ** not have much in common at the moment.  Caller is going to cast the 
  ** stupid thing anyway....
  */
  
  public Object getRenderer() {
    return (myRenderer_);
  }  
  
  /***************************************************************************
  **
  ** Set the draw style
  */
  
  public void setDrawStyle(SuggestedDrawStyle drawStyle) {
    drawStyle_ = drawStyle;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the draw style (never null)
  */
  
  public SuggestedDrawStyle getDrawStyle() {
    return (drawStyle_);
  }  

  /***************************************************************************
  **
  ** Clone
  */

  public LinkProperties clone() {
    try {
      LinkProperties retval = (LinkProperties)super.clone();
      retval.copySupport(this);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
  
  /***************************************************************************
  **
  ** Set the color
  */
  
  public void setColor(String colorName) {
    drawStyle_.setColorName(colorName);
    return;
  }
  
  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    if (drawStyle_.getColorName().equals(oldID)) {
      drawStyle_.setColorName(newID);
      retval = true;
    }
    if (replaceCustomColors(oldID, newID)) {
      retval = true;
    }
    return (retval);
  }  
  

  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor(ColorResolver cRes) {
    return (cRes.getColor(drawStyle_.getColorName()));
  }

  /***************************************************************************
  **
  ** Get the color name
  */
  
  public String getColorName() {
    return (drawStyle_.getColorName());
  }
  
  /***************************************************************************
  **
  ** Get the text position
  */
  
  public Point2D getTextPosition() {
    return (textPos_);
  }
  
  /***************************************************************************
  **
  ** Set the text position
  */
  
  public void setTextPosition(Point2D newPos) {
    textPos_ = (newPos == null) ? null : (Point2D)newPos.clone();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the text direction
  */
  
  public int getTextDirection() {
    return (textDir_);
  }  

  /***************************************************************************
  **
  ** Get the style
  */
  
  public void setTextDirection(int dir) {
    textDir_ = dir;
    switch (textDir_) {
      case LEFT:
        textDirTag_ = "";
        break;
      case RIGHT:
        textDirTag_ = "right";
        break;
      case UP:
        textDirTag_ = "up";
        break;
      case DOWN:
        textDirTag_ = "down";
        break;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }  
    
  /***************************************************************************
  **
  ** Answer if we have a drop for the specified link
  */
  
  public boolean haveTargetDrop(String linkID) {
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      String tref = bd.getTargetRef();
      if ((tref != null) && (tref.equals(linkID))) {
        return (true);
      }
    }
    return (false);
  }  
 
  /***************************************************************************
  **
  ** Get the segment with the given ID
  */
  
  public LinkSegment getSegment(String id) {
    return (segments_.get(id));
  }
  
  /***************************************************************************
  **
  ** Get the segment count
  */
  
  public int getSegmentCount() {
    return (segments_.size());
  }    
  
  /***************************************************************************
  **
  ** Answers if the given point is a corner point
  */
  
  public boolean isCornerPoint(Point2D point) {
    
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (seg.getStart().equals(point)) {
        return (true);
      }
      if ((seg.getEnd() != null) && seg.getEnd().equals(point)) {
        return (true);
      }
    }
    return (false);
  }    
  
  /***************************************************************************
  **
  ** Answer if this link has a non-orthogonal segment
  */
  
  public Set<LinkSegmentID> getNonOrthoSegments(DataAccessContext icx) {
    HashSet<LinkSegmentID> retval = new HashSet<LinkSegmentID>();
    
    if (isDirect()) {
      LinkSegment directSeg = getDirectLinkPath(icx);
      Point2D startPt = directSeg.getStart();
      Point2D endPt = directSeg.getEnd();
      if ((startPt.getX() != endPt.getX()) && (startPt.getY() != endPt.getY())) {
        retval.add(LinkSegmentID.buildIDForDirect(getSingleLinkage()));
      }
      return (retval);
    }
    
    if (srcDropIsNonOrtho(icx)) {
      retval.add(LinkSegmentID.buildIDForStartDrop());
    }
    
   Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        if (targetDropIsNonOrtho(bd, icx)) {
          retval.add(LinkSegmentID.buildIDForEndDrop(bd.getTargetRef()));          
        }
      }
    }
   
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (!seg.isOrthogonal()) {
        retval.add(LinkSegmentID.buildIDForSegment(seg.getID()));
      }
    }
    
    return (retval);
  }
  
 /***************************************************************************
  **
  ** Get the single linkages that this bus is used for
  */
  
  public String getSingleLinkage() {
    if (drops_.size() != 2) {
      throw new IllegalStateException();
    }
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      LinkBusDrop drop = drops.next();
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        return (drop.getTargetRef());
      }
    }
    throw new IllegalStateException();
  } 
  
 /***************************************************************************
  **
  ** Get the source tag
  */
  
  public String getSourceTag() {
    return (srcTag_);
  }
  
  /***************************************************************************
  **
  ** Get the reference tag
  */
  
  public String getReferenceTag() {
    return (srcTag_);
  }  
    
  
 /***************************************************************************
  **
  ** Get the segment ID the parent of any segmentID.
  */
  
  public LinkSegmentID getSegmentIDForParent(LinkSegmentID segID) {
    
    if (segID.isDirect()) {
      return (null);
    } else if (segID.isForStartDrop()) {
      return (null);  
    } else if (segID.isForEndDrop()) {
      LinkBusDrop bd = getTargetDrop(segID);
      String lsId = bd.getConnectionTag();
      LinkSegment dropParent = getSegment(lsId);
      String dropParentID = dropParent.getID();
      int sense = bd.getConnectionSense();
      if (sense == LinkBusDrop.CONNECT_TO_START) {
        String metaParent = dropParent.getParent();
        if (metaParent != null) {
          dropParentID = metaParent;
        } else {
          return (LinkSegmentID.buildIDForStartDrop());
        }
      }
      return (LinkSegmentID.buildIDForSegment(dropParentID));
    } else {
      LinkSegment thisSeg = getSegment(segID);
      String linkSegParent = thisSeg.getParent();
      if (thisSeg.isDegenerate() || (linkSegParent == null)) {
        return (LinkSegmentID.buildIDForStartDrop());
      } else {
        return (LinkSegmentID.buildIDForSegment(linkSegParent));
      }    
    }
  }  
   
  
  /***************************************************************************
  **
  ** Get a list of LinkSegmentIDs for ALL link segments
  */
  
  public List<LinkSegmentID> getAllBusLinkSegments(DataAccessContext icx,
                                                   boolean includeRoot) {
    
    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
    GenomeSource gSrc = icx.getGenomeSource();
    Genome genome = icx.getGenome();
        
    if (isDirect()) {
      String targRef = getTargetDrop().getTargetRef();
      if (!linkIsInModel(gSrc, genome, icx.oso, targRef)) {
        return (retval);
      }
      retval.add(LinkSegmentID.buildIDForType(targRef, LinkSegmentID.DIRECT_LINK));
      return (retval);
    }
    
    //
    // Check for drop intersections.  If none, we will still have built up the
    // set of active segments to check:
    //
    
    HashSet<LinkSegment> set = new HashSet<LinkSegment>();    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      if ((dropRef != null) && !linkIsInModel(gSrc, genome, icx.oso, dropRef)) {
        continue;
      }
      int dropType = (dropRef == null) ? LinkSegmentID.START_DROP : LinkSegmentID.END_DROP;
      retval.add(LinkSegmentID.buildIDForType(dropRef, dropType));
      set.addAll(getSegmentsToRoot(drop));      
    }
    
    //
    // Active segments
    //
    
    Iterator<LinkSegment> sit = set.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();  
      if (seg.isDegenerate() && !includeRoot) {
        continue;
      }
      String segID = seg.getID();
      retval.add(LinkSegmentID.buildIDForType(segID, LinkSegmentID.SEGMENT));
    }      
 
    return (retval);
  }    
    
  /***************************************************************************
  **
 ** Get a list of LinkSegmentIDs for one link
  */
  
  public List<LinkSegmentID> getBusLinkSegmentsForOneLink(DataAccessContext icx, String linkID) { 
    
    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
    GenomeSource gSrc = icx.getGenomeSource();
    Genome genome = icx.getGenome();
   
    if (isDirect()) {
      String targRef = getTargetDrop().getTargetRef();
      if (!targRef.equals(linkID)) {
        return (retval);
      }
      if (!linkIsInModel(gSrc, genome, icx.oso, targRef)) {
        return (retval);
      }
      retval.add(LinkSegmentID.buildIDForType(targRef, LinkSegmentID.DIRECT_LINK));
      return (retval);
    }
    
    //
    // Check for drop intersections.  If none, we will still have built up the
    // set of active segments to check:
    //
    
    HashSet<LinkSegment> set = new HashSet<LinkSegment>();    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      if ((dropRef != null) && (!dropRef.equals(linkID))) {
        continue;
      } 
      if ((dropRef != null) && !linkIsInModel(gSrc, genome, icx.oso, dropRef)) {
        continue;
      }
      int dropType = (dropRef == null) ? LinkSegmentID.START_DROP : LinkSegmentID.END_DROP;
      retval.add(LinkSegmentID.buildIDForType(dropRef, dropType));
      set.addAll(getSegmentsToRoot(drop));      
    }
    
    //
    // Active segments
    //
    
    Iterator<LinkSegment> sit = set.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();  
      String segID = seg.getID();
      retval.add(LinkSegmentID.buildIDForType(segID, LinkSegmentID.SEGMENT));
    }      
 
    return (retval);
  }      
  
  /***************************************************************************
  **
  ** Get the draw style for the given segment.  May be null.
  */
  
  public SuggestedDrawStyle getDrawStyleForID(LinkSegmentID segID) {
    
    if (segID.isDirect()) {
      if (!isDirect()) {
        throw new IllegalArgumentException();
      }
      LinkBusDrop targDrop = getTargetDrop();
      // Can a singleton drop even have a special style?  Isn't that
      // covered by the link properties???
      return (targDrop.getSpecialDropDrawStyle());
    } else if (segID.isForStartDrop()) {
      LinkBusDrop drop = getRootDrop();
      return (drop.getSpecialDropDrawStyle());
    } else if (segID.isForEndDrop()) {
      LinkBusDrop drop = getTargetDrop(segID);
      return (drop.getSpecialDropDrawStyle());
    } else {
      LinkSegment seg = getSegment(segID);
      return (seg.getSpecialDrawStyle()); 
    }
  }  
  
  /***************************************************************************
  **
  ** Set the draw style for the given segment.  May be null.
  */
  
  public void setDrawStyleForID(LinkSegmentID segID, SuggestedDrawStyle sds) {
    
    if (segID.isDirect()) {
      if (!isDirect()) {
        throw new IllegalArgumentException();
      }
      LinkBusDrop targDrop = getTargetDrop();
      // Can a singleton drop even have a special style?  Isn't that
      // covered by the link properties???
      targDrop.setDrawStyleForDrop(sds);
    } else if (segID.isForStartDrop()) {
      LinkBusDrop drop = getRootDrop();
      drop.setDrawStyleForDrop(sds);
    } else if (segID.isForEndDrop()) {
      LinkBusDrop drop = getTargetDrop(segID);
      drop.setDrawStyleForDrop(sds);
    } else {
      LinkSegment seg = getSegment(segID);
      seg.setDrawStyle(sds); 
    }   
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the start drop for the tree root
  */
  
  public LinkBusDrop getRootDrop() {
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      if (bd.getDropType() == LinkBusDrop.START_DROP) {
        return (bd);
      }
    }
    throw new IllegalStateException();
  }    
   
  /***************************************************************************
  **
  ** Get an Iterator over the segments
  */
  
  public Iterator<LinkSegment> getSegments() {
    return (segments_.values().iterator());
  }  
  
  /***************************************************************************
  **
  ** Build a map that provides links associated with link segment start/end
  ** point pairs.
  */
  
  public Map<LinkPlacementGrid.PointPair, List<String>> buildSegmentToLinksMap() {

    HashMap<LinkPlacementGrid.PointPair, List<String>> retval = new HashMap<LinkPlacementGrid.PointPair, List<String>>();

    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String tref = drop.getTargetRef();
      if (tref == null) {
        continue;
      }
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        if (isSegmentAnAncestor(seg.getID(), drop)) {
          Point2D start = seg.getStart();
          Point2D end = seg.getEnd();
          LinkPlacementGrid.PointPair pair = new LinkPlacementGrid.PointPair(start, end);
          List<String> links = retval.get(pair);
          if (links == null) {
            links = new ArrayList<String>();
            retval.put(pair, links);
          }
          links.add(tref);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if the given segment is an ancestor of the given drop.
  */
  
  public boolean isSegmentAnAncestor(String segTag, LinkBusDrop drop) {
    if (drop.getDropType() == LinkBusDrop.START_DROP) {
      return (false);
    }
    String nextID = drop.getConnectionTag();
    do {
      LinkSegment curr = segments_.get(nextID);
      if (segTag.equals(curr.getID())) {
        return (true);
      }
      nextID = curr.getParent();
    } while (nextID != null);
    return (false);
  }
  
  /***************************************************************************
  **
  ** Merge a single link into this one at a given segment
  */
  
  public void mergeSingleToTreeAtSegment(LinkProperties slp, DataAccessContext icx, LinkSegmentID sid) {

    if (!slp.isSingleDropTree()) {
      throw new IllegalArgumentException();
    } 

    //
    // May need to split a no-seg bus in two before adding new segment!
    //    
    
    if (isDirect()) {
      if (sid != null) {
        throw new IllegalArgumentException();
      }
      splitNoSegmentBus(icx);
    }    
    
    // If no segment is provided, we glue it to the root drop.
    
    if (sid == null) {
      sid = LinkSegmentID.buildIDForStartDrop();
      sid.tagIDWithEndpoint(LinkSegmentID.END);
    }     
    
    //
    // Do some sanity checking
    //
    
    sourceSanityCheck(icx.getGenome(), slp);
  
    //
    // Figure out where we are going to connect up the new segment.  If we
    // have a real segment, we are in great shape.  If we have a drop, get
    // the parent segment instead.
    //
    
    SegResolve segr = segmentResolution(sid);
    LinkBusDrop targDrop = slp.getTargetDrop();
    
    //
    // If single has NO links, get the target drop and install it.
    //

    if (slp.isDirect() || (slp.getSegmentCount() == 1)) {
      LinkBusDrop drop = generateBusDrop(targDrop, segr.seg.getID(), LinkBusDrop.END_DROP, segr.connectEnd);
      drop.transferSpecialStyles(targDrop);
      addDrop(drop);
      return;
    }
    
    //
    // The incoming properties has its first segment (and the root segment
    // that we are dropping) basically starting at the intersection point.
    // The start drop (that we ignore) is going from the intersection point
    // to the link source pad.
    //
   
    //
    // Get all the segments in reverse order from the incoming set:
    //
    
    LinkBusDrop singleTarg = slp.getTargetDrop();
    List<LinkSegment> segsToRoot = slp.getSegmentsToRoot(singleTarg);
    int strNum = segsToRoot.size();
    segsToRoot.remove((strNum--) - 1);  // Ditch the root segment
    Collections.reverse(segsToRoot);

    String currParent = segr.seg.getID();
    String newId = null;
    
    for (int i = 0; i < strNum; i++) {
      LinkSegment lseg = segsToRoot.get(i);
      LinkSegment newLseg = new LinkSegment(lseg);
      newId = labels_.getNextLabel();        
      newLseg.setID(newId);
      newLseg.setParent(currParent);
      if (i == 0) {
        newLseg.setStart((Point2D)segr.ourConnectPos.clone());
      }
      //
      // FIX ME??? Do we want to propagate a link-based special style
      // if we are extending off of a segment that is currently rendered
      // due to that style??
      //
      // OBSOLETE:
      //int segSpecial = newLseg.getSpecialStyle();
      // Toss special if it matches target, else keep it:
      //if ((segSpecial != LinkProperties.NONE) && (segSpecial == stdStyle)) {
      //  newLseg.setSpecialStyle(LinkProperties.NONE);
      //} else if (slpStyle != stdStyle) {
      //  newLseg.setSpecialStyle(slpStyle);
      //}      
      segments_.put(newId, newLseg);
      currParent = newId;
    }
    
    // int dropStyle = (slpStyle == getLineStyle()) ? LinkProperties.NONE : slpStyle;
    LinkBusDrop drop = generateBusDrop(targDrop, newId, LinkBusDrop.END_DROP,
                                       LinkBusDrop.CONNECT_TO_END);// , dropStyle);
    drop.transferSpecialStyles(targDrop);
    addDrop(drop); 
    
    return;
  }   
   
  /***************************************************************************
  **
  ** Answer which segment is closest, providing distance and closest point.
  ** We omit checks with given segmentIDs; return null if nobody is closest
  */

  public ClosestAnswer findClosestSegment(Point2D pt, List<String> omit) {
    ClosestAnswer retval = new ClosestAnswer();
    Iterator<String> sit = segments_.keySet().iterator();
    retval.distance = Double.POSITIVE_INFINITY;
    while (sit.hasNext()) {
      String nextKey = sit.next();
      LinkSegment seg = segments_.get(nextKey);
      if (!omit.contains(seg.getID())) {
        Point2D closest = seg.getClosestPoint(pt);
        if (closest == null) {
          continue;
        }
        Vector2D ortho = new Vector2D(closest, pt);
        double offset = ortho.length();
        if (offset < retval.distance) {
          retval.distance = offset;
          retval.segID = seg.getID();
          retval.point = closest;
        }
      }
    }
    return ((retval.point == null) ? null : retval);
  }
  
  /***************************************************************************
  **
  ** Return a copy of the segments
  */

  public Map<String, LinkSegment> copySegments() {
    HashMap<String, LinkSegment> retval = new HashMap<String, LinkSegment>();
    Iterator<String> sit = segments_.keySet().iterator();
    while (sit.hasNext()) {
      String nextKey = sit.next();
      LinkSegment seg = segments_.get(nextKey);
      retval.put(nextKey, new LinkSegment(seg));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** restore segments to match the given copy.  We reuse the segments in
  ** the segmentsWithKids, since that is using references to the original.
  */

  public void restoreSegments(Map<String, LinkSegment> oldSegments, SegmentWithKids rootSK) {
    clearOutAllSegments();
    Iterator<String> sit = oldSegments.keySet().iterator();
    while (sit.hasNext()) {
      String nextKey = sit.next();
      LinkSegment seg = oldSegments.get(nextKey);
      Point2D segEnd = seg.getEnd();
      SegmentWithKids swk = rootSK.getSwkChild(nextKey);
      LinkSegment restore = swk.segment;
      restore.setBoth((Point2D)seg.getStart().clone(), (segEnd == null) ? null : (Point2D)segEnd.clone());
      restore.setParent(seg.getParent());
      segments_.put(nextKey, restore);
      if (!labels_.addExistingLabel(nextKey)) {
        throw new IllegalStateException();
      }
    }
    return;
  }
 
 /***************************************************************************
  **
  ** Split an existing drop into a link segment and a drop, using the given
  ** point as the split
  */
  
  public void splitADrop(LinkBusDrop drop, Point2D endpt) {
                               
    //
    // From the drop we find out which segment we are parented by, and which
    // end.  We get that segment, and the correct point, and build a new
    // segment with the two points in hand.  We parent the new segment to the
    // existing segment, and change the drop connection to the new segment.
    //                             
    
    if (drop.getDropType() == LinkBusDrop.START_DROP) {
      reparentTree(endpt);
      return;
    }
    
    String newId = labels_.getNextLabel();    
    String segID = drop.getConnectionTag();
    LinkSegment seg = segments_.get(segID);
    int sense = drop.getConnectionSense();
    Point2D strt = (sense == LinkBusDrop.CONNECT_TO_END) ? seg.getEnd() : seg.getStart();
    strt = (Point2D)strt.clone();
    endpt = (Point2D)endpt.clone();
    
    LinkSegment newSeg = new LinkSegment(newId, segID, strt, endpt);
    SuggestedDrawStyle sds = drop.getSpecialDropDrawStyle();
    if (sds != null) {
      newSeg.setDrawStyle(sds.clone());
    }

    segments_.put(newId, newSeg);    

    drop.setConnectionTag(newId);
    drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);

    return;
  }
  
  /***************************************************************************
  **
  ** Reparent the tree, making the given point the new root.
  */
  
  public void reparentTree(Point2D splitPt) {                               
    //
    // If we split the source drop, we need to reparent the tree to the
    // new split point.
    // 1) build up a new degenerate root segment.
    // 2) Take the existing root segment, give it a parent, and change the
    // start to the split point and the end to the old point.
    // 3) Any drop from the start of the old root goes from the end of it EXCEPT:
    // 4) The start drop goes from the beginning of the new root segment
    //

    LinkSegment oldRootSeg = getRootSegment();
    String oldRootId = oldRootSeg.getID();
    String newId = labels_.getNextLabel();    
    LinkSegment newRootSeg = new LinkSegment(newId, null, splitPt, null);
    segments_.put(newId, newRootSeg); 

    Point2D end = oldRootSeg.getStart(); 
    oldRootSeg.setStart(splitPt);
    oldRootSeg.setEnd(end);    
    oldRootSeg.setParent(newId);
    
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        drop.setConnectionTag(newId);
      } else {      
        String segID = drop.getConnectionTag();
        if (segID.equals(oldRootId) && 
            (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START)) {
          drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
        }
      }
    }
    return;
  }    
 
  /***************************************************************************
  **
  ** Generate all IDs for the tree
  */
  
  public List<LinkSegmentID> generateAllIDs() {                               

    ArrayList<LinkSegmentID> idList = new ArrayList<LinkSegmentID>();  
    if (isDirect()) {
      List<String> allLinks = getLinkageList();
      String linkRef = allLinks.get(0);
      idList.add(LinkSegmentID.buildIDForDirect(linkRef));
    } else {
 
      Iterator<LinkBusDrop> dit = drops_.iterator();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();     
        if (drop.getDropType() == LinkBusDrop.START_DROP) {
         idList.add(LinkSegmentID.buildIDForStartDrop());
        } else {
          String linkRef = drop.getTargetRef();
          idList.add(LinkSegmentID.buildIDForEndDrop(linkRef));
        }
      }
 
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        idList.add(LinkSegmentID.buildIDForSegment(seg.getID()));
      }
    }
    return (idList);
  }
  
  /***************************************************************************
  **
  ** Get all segment geometries (except for the degenerate root segment if specified)
  */
  
  public Map<LinkSegmentID, LinkSegment> getAllSegmentGeometries(DataAccessContext icx, boolean noDegen) {                               
 
    List<LinkSegmentID> idList = generateAllIDs();
    HashMap<LinkSegmentID, LinkSegment> allGeoms = new HashMap<LinkSegmentID, LinkSegment>();
    Iterator<LinkSegmentID> idit = idList.iterator();
    while (idit.hasNext()) {
      LinkSegmentID lsid = idit.next();
      if (lsid.isForSegment()) {
        LinkSegment seg = getSegment(lsid);
        if (noDegen && seg.isDegenerate()) {  // Don't care about degenerate root
          continue;
        }
      }
      LinkSegment fakeSeg = getSegmentGeometryForID(lsid, icx, false);
      allGeoms.put(lsid, fakeSeg);
    }
    return (allGeoms);
  }
   
  /***************************************************************************
  **
  ** Returns a measurement of the non-orthogonality of the link tree.  Has
  ** value of 0.0 for orthogonal trees. 
  */
  
  public double getNonOrthogonalArea(DataAccessContext icx) {  
    Map<LinkSegmentID, LinkSegment> geoms = getAllSegmentGeometries(icx, true); 
    double retval = 0.0;   
    Iterator<LinkSegment> git = geoms.values().iterator();
    while (git.hasNext()) {
      LinkSegment fakeSeg = git.next(); 
      retval += fakeSeg.getNonOrthogonalArea();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Returns a measurement of the non-orthogonality of the link tree.  Has
  ** value of 0.0 for orthogonal trees. 
  */
  
  public static double getNonOrthogonalArea(Map<LinkSegmentID, LinkSegment> geoms) {  
    double retval = 0.0;   
    Iterator<LinkSegment> git = geoms.values().iterator();
    while (git.hasNext()) {
      LinkSegment fakeSeg = git.next(); 
      retval += fakeSeg.getNonOrthogonalArea();
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Support split
  */
  
  public LinkSegmentID[] linkSplitSupport(LinkSegmentID segID, Point2D pt) {  
     //
    // If the link to be split is an internal link, we need to add it to the
    // pile and fixup all the references.  If we are splitting a drop, we need
    // to modify the drop and add a new internal link.
    //                                   
    
    LinkSegmentID[] retval = new LinkSegmentID[2];
    if (segID.isDirect()) {
      LinkSegment newRoot = new LinkSegment((Point2D)pt.clone(), null);
      addSegmentInSeries(newRoot);
      retval[0] = LinkSegmentID.buildIDForStartDrop();
      retval[1] = getRootSegmentID();
      return (retval);
    } else if (segID.isForDrop()) {
      LinkBusDrop drop = getDrop(segID);
      splitADrop(drop, pt);
      String dropRef = drop.getTargetRef();
      if (dropRef == null) {
        retval[0] = LinkSegmentID.buildIDForStartDrop();
        retval[1] = getRootSegmentID();
      } else {        
        retval[1] = LinkSegmentID.buildIDForEndDrop(dropRef);
        drop = getDrop(retval[1]);
        retval[0] = LinkSegmentID.buildIDForSegment(drop.getConnectionTag());
      }
    } else {
      LinkSegment seg = getSegment(segID);
      if (seg == null) {
        System.err.println("SEGMENT NOT FOUND " + segID);       
      }
      if (seg.isDegenerate()) { // trying to split a single point
        return (null);
      }
      LinkSegment[] newSegs = seg.split(pt);
      if (newSegs.length == 1) {  // Endpoint intersected; no split occurred
        return (null);
      }
      String[] newIDs = replaceSegment(seg, newSegs[0], newSegs[1]);
      retval[0] = LinkSegmentID.buildIDForSegment(newIDs[0]);
      retval[1] = LinkSegmentID.buildIDForSegment(newIDs[1]);
    }
    return (retval);
    
  }
  
  /***************************************************************************
  **
  ** Get the root seg ID
  */
  
  public LinkSegmentID getRootSegmentID() { 
    return ((isDirect()) ? null : LinkSegmentID.buildIDForSegment(getRootSegment().getID()));
  }  
  
  /***************************************************************************
  **
  ** Split a no segment bus in half
  */
  
  public void splitNoSegmentBus(DataAccessContext icx) {
    LinkSegment directSeg = getDirectLinkPath(icx);
   
    Vector2D dropRun = new Vector2D(directSeg.getStart(), directSeg.getEnd());
    dropRun.scale(0.5);
    Point2D halfPoint = dropRun.add(directSeg.getStart());
    UiUtil.forceToGrid(halfPoint, UiUtil.GRID_SIZE);
    
    LinkSegment newRoot = new LinkSegment(halfPoint, null);
    addSegmentInSeries(newRoot);
    
    return;  
  } 
  
  /***************************************************************************
  **
  ** Repair a non-orthogonal link segment
  */  
 
  //
  // Debugging note.  To see the results of a particular strategy, uncomment the
  // hackCount stuff and do undos followed by a new orthogonalization step.  Note
  // the need for the static variable (BusProps are cloned for undo/redo!)
  //private static int hackCount_ = 0;
  //private static boolean getOut_ = false;
  
  public boolean fixNonOrtho(LinkSegmentID segID, DataAccessContext icx,
                             boolean minCorners, String overID, BTProgressMonitor monitor) throws AsynchExitRequestException {
       
    //
    // DOFs->Options->Abstract Strategies->Concrete Plans->Low Level Commands, if plan determined to work
    //
  
    LinkSegment noSeg = getSegmentGeometryForID(segID, icx, false);
    if (noSeg.isOrthogonal() || noSeg.isDegenerate()) {
      return (false);     
    }  
  
    LinkRouter router = new LinkRouter();   
    LinkPlacementGrid initialGrid = initGridForOrthoFix(router, icx, overID, monitor);  
    // To look at the grid we are working with: initialGrid.debugPatternGrid(true);
   // initialGrid.debugPatternGrid(false);
    
    
    TreeMap<TreeStrategy.PlanRanking, List<TreeStrategy.StratAndVar>> rankOptions = 
      new TreeMap<TreeStrategy.PlanRanking, List<TreeStrategy.StratAndVar>>();
    double startRank = getNonOrthogonalArea(icx);
    
    FixOrthoOptions.FixOrthoTreeInfo foti = generateTreeInfo(icx);
    FixOrthoOptions fixOps = new FixOrthoOptions(segID, foti);
    Iterator<TreeStrategy> foit = fixOps.getStrategies();
    //int count = 0;
    while (foit.hasNext()) {
      TreeStrategy tstr = foit.next();
      if (monitor != null) {
        if (!monitor.keepGoing()) {
          throw new AsynchExitRequestException();
        }
      }  
      int varNum = tstr.generatePlans(icx, this, foti);
      //System.out.println("we have variations " + varNum);
      for (int i = 0; i < varNum; i++) {
        // ON LARGE TREES, this is taking ~220ms per pass. So it slows us down...
        if (!tstr.canApply(i, initialGrid, icx, this, foti, overID)) {
          //System.out.println("no apply " + tstr);
          // Plan cannot be applied to grid
          continue;
        }
        TreeStrategy.PlanRanking rankObj = tstr.getRanking(i, minCorners, this, icx, foti);
        if (rankObj.distRank == 0.0) {
          //System.out.println("no dist " + tstr);
          // Seeing plans with Zero distance, unclear why.  FIX ME!
          continue;
        }
        // If we are trying to repair a link with other non-ortho links next to it,
        // wild solutions are possible that make the overall solution worse.  NEVER
        // allow the situation to get worse!
        if (rankObj.nonOrthoArea > startRank) {
          //System.out.println("wild Thing! " + tstr);
          continue;
        }
        
        //getOut_ = false;
        // Use hackCount_ framework to step thru strategies using undo and then repeat ortho step
        //if (count == hackCount_) {
          //System.out.println("now on count " + hackCount_);
        List<TreeStrategy.StratAndVar> forRank = rankOptions.get(rankObj);
        if (forRank == null) {
          forRank = new ArrayList<TreeStrategy.StratAndVar>();
          rankOptions.put(rankObj, forRank);
        }
        forRank.add(new TreeStrategy.StratAndVar(tstr, i));
        //hackCount_++;
        //getOut_ = true;
       // break;
        // Variations are designed to slide segments until they can be applied.  If we
        // find a successful variation, skip the rest!
        if (varNum > 1) {
          //System.out.println("success on " + i + " of " + varNum);
          break;
        }
       // }
        //count++;
      }
      //if (getOut_) {
     //   break;
      //}
    }
    
    if (rankOptions.isEmpty()) {
      return (false);
    }
    
    TreeStrategy.PlanRanking bestRank = rankOptions.firstKey();
    List<TreeStrategy.StratAndVar> bestGuys = rankOptions.get(bestRank);
    TreeStrategy.StratAndVar bestOfBest = bestGuys.get(0);
    bestOfBest.strat.applyPlan(bestOfBest.varNum, this, foti);
    return (true);
  }
 
  
  /***************************************************************************
  **
  ** For multiple tree repair results:
  */  
 
  public static int mergeRepairStates(int oldState, int newState) {
    switch (oldState) {
      case NOTHING_TO_REPAIR:
        switch (newState) {
          case NOTHING_TO_REPAIR:
            return (NOTHING_TO_REPAIR);
          case ALL_REPAIRED:
            return (ALL_REPAIRED);
          case SOME_REPAIRED:
            return (SOME_REPAIRED);
          case COULD_NOT_REPAIR:
            return (COULD_NOT_REPAIR);
          default:
            throw new IllegalArgumentException();
        }
      case ALL_REPAIRED:
        switch (newState) {
          case NOTHING_TO_REPAIR:
            return (ALL_REPAIRED);
          case ALL_REPAIRED:
            return (ALL_REPAIRED);
          case SOME_REPAIRED:
            return (SOME_REPAIRED);
          case COULD_NOT_REPAIR:
            return (SOME_REPAIRED);
          default:
            throw new IllegalArgumentException();
        }
      case SOME_REPAIRED:
        switch (newState) {
          case NOTHING_TO_REPAIR:
            return (SOME_REPAIRED);
          case ALL_REPAIRED:
            return (SOME_REPAIRED);
          case SOME_REPAIRED:
            return (SOME_REPAIRED);
          case COULD_NOT_REPAIR:
            return (SOME_REPAIRED);
          default:
            throw new IllegalArgumentException();
        }
      case COULD_NOT_REPAIR:
        switch (newState) {
          case NOTHING_TO_REPAIR:
            return (COULD_NOT_REPAIR);
          case ALL_REPAIRED:
            return (SOME_REPAIRED);
          case SOME_REPAIRED:
            return (SOME_REPAIRED);
          case COULD_NOT_REPAIR:
            return (COULD_NOT_REPAIR);
          default:
            throw new IllegalArgumentException();
        }
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Repair a link tree where geometry is hiding true topology of tree.  Force
  ** topology to match:
  */  
 
  public int repairLinkTree(DataAccessContext icx,
                            BTProgressMonitor monitor, double minFrac, double maxFrac, QuadTree quad) throws AsynchExitRequestException {   
    boolean found = false;
    // Just in case the repair operation is not going to converge, put a cap on
    // the loop:
    double inc = (maxFrac - minFrac) / (double)REPAIR_CAP_;
    double currFrac = minFrac;
    for (int j = 0; j < REPAIR_CAP_; j++) {
      // FYI 8/20/13: Each tree taking 4-5000 ms to process on 6.7K node 28.4K link network; mostly in overlap intersection testing.
      // Seeing largest times on first few trees
      List<RepairRequest> repReq = findOverlapErrors(icx, quad);  
      int numRR = repReq.size();
      if (numRR == 0) {
        return ((j == 0) ? NOTHING_TO_REPAIR : ALL_REPAIRED);
      }
      for (int i = 0; i < numRR; i++) {
        RepairRequest rr = repReq.get(i);
        if (rr.isUseless) { 
          LinkSegmentID rmCor = rr.myID.clone();
          rmCor.tagIDWithEndpoint(LinkSegmentID.END);
          if (hasSimpleConnectivity(rmCor)) { 
            removeCorner(rmCor);
          } else {
            if (!dropZeroLengthSeg(rmCor)) {
              return (COULD_NOT_REPAIR);
            }
          }
          found = true;
          break;
        // FYI 8/20/13: This is NOT a time sink for topo repair on 6.7K node 28.4K link network
        } else if (repairOverlapErrors(icx, rr)) {
          found = true;
          break;
        }
      }
      currFrac += inc;
      if (monitor != null) {
        boolean keepGoing = monitor.updateProgress((int)(currFrac * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
      if (j == PENULTIMATE_REPAIR_) {
        System.err.println("Repair is not converging");
      }
    }
    
    return ((found) ? SOME_REPAIRED : COULD_NOT_REPAIR);
  }
  
  /***************************************************************************
  **
  ** Find overlap errors
  */
  
  private List<RepairRequest> findOverlapErrors(DataAccessContext icx, QuadTree quad) {

    ArrayList<RepairRequest> retval = new ArrayList<RepairRequest>();
    if (isDirect()) {
      return (retval);
    }
    
    HashMap<LinkSegmentID, LinkSegment> geom = new HashMap<LinkSegmentID, LinkSegment>();
    HashMap<LinkSegmentID, LinkSegmentID> parents = new HashMap<LinkSegmentID, LinkSegmentID>();
    HashMap<LinkSegmentID, Set<LinkSegmentID>> kids = new HashMap<LinkSegmentID, Set<LinkSegmentID>>();
    
    LinkSegmentID rootID = LinkSegmentID.buildIDForType(null, LinkSegmentID.START_DROP);   
    LinkSegment rootSeg = getSegmentGeometryForID(rootID, icx, false);
    if (rootSeg.getLength() == 0.0) {
      RepairRequest request = new RepairRequest(rootID);
      retval.add(request);
      return (retval);
    }
    geom.put(rootID, rootSeg);
 
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String tRef = drop.getTargetRef();
      if (tRef == null) {
        continue;
      }
      LinkSegmentID dropID = LinkSegmentID.buildIDForType(tRef, LinkSegmentID.END_DROP);   
      LinkSegment seg = getSegmentGeometryForID(dropID, icx, false);
      if (seg.getLength() == 0.0) {
        RepairRequest request = new RepairRequest(dropID);
        retval.add(request);
        return (retval);
      }
      geom.put(dropID, seg);
      
      String ctag = drop.getConnectionTag();
      LinkSegmentID parSegID;
      if (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
        LinkSegment parSeg = getSegment(ctag);
        if (parSeg.isDegenerate()) {
          parSegID = rootID;
        } else {
          parSegID = LinkSegmentID.buildIDForType(parSeg.getParent(), LinkSegmentID.SEGMENT);
        }
      } else {
        parSegID = LinkSegmentID.buildIDForType(ctag, LinkSegmentID.SEGMENT);
      }
      parents.put(dropID, parSegID);
      Set<LinkSegmentID> kidSet = kids.get(parSegID);
      if (kidSet == null) {
        kidSet = new HashSet<LinkSegmentID>();
        kids.put(parSegID, kidSet);
      }
      kidSet.add(dropID);
    }
    
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      if (seg.isDegenerate()) {
        continue;
      }
      LinkSegmentID segID = LinkSegmentID.buildIDForType(seg.getID(), LinkSegmentID.SEGMENT);
      if (seg.getLength() == 0.0) {        
        RepairRequest request = new RepairRequest(segID);
        retval.add(request);
        return (retval);
      }
      
      geom.put(segID, seg);
      LinkSegment parSeg = getSegment(seg.getParent());
      LinkSegmentID parSegID;
      if (parSeg.isDegenerate()) {
        parSegID = rootID;
      } else {
        parSegID = LinkSegmentID.buildIDForType(parSeg.getID(), LinkSegmentID.SEGMENT);
      }
      parents.put(segID, parSegID);
      Set<LinkSegmentID> kidSet = kids.get(parSegID);
      if (kidSet == null) {
        kidSet = new HashSet<LinkSegmentID>();
        kids.put(parSegID, kidSet);
      }
      kidSet.add(segID);
    }
   
    Set<LinkSegmentID> allSegs = geom.keySet();
    HashMap<LinkSegmentID, List<QuadTree.TaggedPoint>> findTags = null;
    HashSet<LinkSegmentID> crookedSegs = null;
    
    if (quad != null) {
      MinMax rangeX = new MinMax();
      rangeX.init();
      MinMax rangeY = new MinMax();
      rangeY.init();
           
      Iterator<LinkSegmentID> asito = allSegs.iterator();
      while (asito.hasNext()) {
        LinkSegmentID lsido = asito.next();
        LinkSegment segGeom = geom.get(lsido);    
        rangeX.update((int)UiUtil.forceToGridValue(segGeom.getStart().getX(), UiUtil.GRID_SIZE));
        rangeX.update((int)UiUtil.forceToGridValue(segGeom.getEnd().getX(), UiUtil.GRID_SIZE));
        rangeY.update((int)UiUtil.forceToGridValue(segGeom.getStart().getY(), UiUtil.GRID_SIZE));
        rangeY.update((int)UiUtil.forceToGridValue(segGeom.getEnd().getY(), UiUtil.GRID_SIZE));
      } 
   
      int height = rangeY.max - rangeY.min;
      int width = rangeX.max - rangeX.min;
      Rectangle rect = new Rectangle(rangeX.min - 1000, rangeY.min - 1000, width + 2000, height + 2000);
      quad.initialize(rect);
      findTags = new HashMap<LinkSegmentID, List<QuadTree.TaggedPoint>>();
      crookedSegs = new HashSet<LinkSegmentID>();
  
      Point holder = new Point();
      asito = allSegs.iterator();
      while (asito.hasNext()) {
        LinkSegmentID lsido = asito.next();
        LinkSegment outerGeom = geom.get(lsido);
        Point2D start = outerGeom.getStart();
        Point2D end = outerGeom.getEnd();
        double osx = start.getX();
        double osy = start.getY();
        double oex = end.getX();
        double oey = end.getY();
        if (!((osx == oex) || (osy == oey))) {
          crookedSegs.add(lsido);
        } else {
          holder.setLocation(osx, osy);
          quad.binThePoint(holder);
          int sx = (int)holder.getX();
          int sy = (int)holder.getY();
          
          holder.setLocation(oex, oey);
          quad.binThePoint(holder);
          int ex = (int)holder.getX();
          int ey = (int)holder.getY();
          List<QuadTree.TaggedPoint> tagged = quad.addPoints(sx, ex, sy, ey, lsido);
          findTags.put(lsido, tagged);
        }
      }
    } 
    
    //
    // Wrong!  Children can point overlap their parent, and reverse too
    //
    // Siblings can overlap at their start points:
    // 
        
    Iterator<LinkSegmentID> asito = allSegs.iterator();
    while (asito.hasNext()) {
      LinkSegmentID lsido = asito.next();
      LinkSegment outerGeom = geom.get(lsido);
      LinkSegmentID outerParent = parents.get(lsido);      
      Set<LinkSegmentID> outerKids = kids.get(lsido);
      
      Iterator<LinkSegmentID> asiti;
      if (findTags != null) {
        HashSet<LinkSegmentID> reducedSegs = new HashSet<LinkSegmentID>();
        List<QuadTree.TaggedPoint> checkSegs = findTags.get(lsido);
        if (checkSegs != null) {
          List<QuadTree.TaggedPoint> collideTags = quad.getCollisions(checkSegs);
          Iterator<QuadTree.TaggedPoint> ctit = collideTags.iterator();
          while (ctit.hasNext()) {
            QuadTree.TaggedPoint tp = ctit.next();
            reducedSegs.add((LinkSegmentID)tp.data);
          }
        }
        reducedSegs.addAll(crookedSegs);
        asiti = reducedSegs.iterator();
      } else {
        asiti = allSegs.iterator();
      }
  
      while (asiti.hasNext()) {
        LinkSegmentID lsidi = asiti.next();
        if (lsidi.equals(lsido)) {
          continue;
        }
        LinkSegment innerGeom = geom.get(lsidi);
        LinkSegmentID innerParent = parents.get(lsidi);
        Set<LinkSegmentID> innerKids = kids.get(lsidi);
        // Two sibs can share start point
        // Parent can share end with child start
        // Child can share start with parent end
        int sharedOK = LinkSegment.NO_SHARED;
        if ((outerParent != null) && outerParent.equals(innerParent)) {
          sharedOK = LinkSegment.SHARED_STARTS;
        } else if ((outerKids != null) && outerKids.contains(lsidi)) {
          sharedOK = LinkSegment.THIS_END_OTHER_START_SHARED;
        } else if ((innerKids != null) && innerKids.contains(lsido)) {
          sharedOK = LinkSegment.THIS_START_OTHER_END_SHARED;
        }
        LinkSegment.OverlapResult ovr = outerGeom.overlaps(innerGeom, sharedOK);
        if (ovr != null) {
          RepairRequest request = new RepairRequest(lsido, lsidi, ovr);
          retval.add(request);
        }
      }
    } 
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the deepest non-ortho link segment
  */
  
  public LinkSegmentID getDeepestNonOrtho(DataAccessContext icx, Set<LinkSegmentID> skipIDs) {
   
    //
    // The current implementation of fixing a segment is recursive.  So one
    // change will mod the whole tree, and can fix several things at once.
    // So we need to check back each time.  Also, start with the lowest
    // segments in the tree and work up:
    //
    
    Set<LinkSegmentID> nonOrtho = getNonOrthoSegments(icx);
    if (nonOrtho.isEmpty()) {
      return (null);
    }
    Genome genome = icx.getGenome();
    if (isDirect()) {
      LinkSegmentID dirID = LinkSegmentID.buildIDForDirect(getALinkID(genome));
      return ((skipIDs.contains(dirID)) ? null : dirID);     
    }
    TreeMap<Integer, SortedSet<LinkSegmentID>> depths = new TreeMap<Integer, SortedSet<LinkSegmentID>>(Collections.reverseOrder());
    Iterator<LinkSegmentID> noit = nonOrtho.iterator();
    while (noit.hasNext()) {
      LinkSegmentID lsid = noit.next();
      if (skipIDs.contains(lsid)) {
        continue;
      }
      int myDist = getSegmentDepth(lsid);
      Integer mdVal = new Integer(myDist);
      SortedSet<LinkSegmentID> perD = depths.get(mdVal);
      if (perD == null) {
        perD = new TreeSet<LinkSegmentID>();
        depths.put(mdVal, perD);
      }
      perD.add(lsid);
    }
    
    if (depths.isEmpty()) {
      return (null);
    }   
    SortedSet<LinkSegmentID> deep = depths.get(depths.firstKey());
    return (deep.first());  
  }
  
  /***************************************************************************
  **
  ** Get the depth of the segment
  */
  
  public int getSegmentDepth(LinkSegmentID myID) {
    return ((myID.isForDrop()) ? getSegmentsToRoot(getDrop(myID)).size()
                               : getSegmentsToRoot(getSegment(myID)).size());
  } 
  
  /***************************************************************************
  **
  ** Get the DOF for the given point.  Note that this is referring to the
  ** _endpoint_ of the link segment specified by the lsid:
  */
  
  public CornerDoF getCornerDoF(LinkSegmentID lsid, DataAccessContext icx, boolean doDrops) {
    
    
    //
    // startGeom is set if lsid is for the start drop.  
    // matchSeg is the segment, if working with an actual segment
    // null returned if working with end drop!
    //
    
    
    LinkSegment startGeom = null;
    LinkSegment matchSeg = null;
    if (!lsid.isForSegment()) {
      if (lsid.isForStartDrop()) {
        startGeom = getSegmentGeometryForID(lsid, icx, false);
      } else {
        return (null);
      }
    } else {
      matchSeg = getSegment(lsid);
    }
    
    LinkSegment useSeg = (startGeom == null) ? matchSeg : startGeom;
  
    //
    // Note this causes null return on the zero-length root segment:
    //
        
    if (useSeg.getLength() == 0.0) {
      return (null);
    }
    
    CornerDoF retval = new CornerDoF(lsid);
    retval.runVector = useSeg.getRun();
    retval.inboundIsCanonical =  retval.runVector.isCanonical();
    if (!retval.inboundIsCanonical) {
      retval.runVector = retval.runVector.canonical().normalized();  
    }    
    retval.backupVector = retval.runVector.scaled(-1.0);
    retval.normVector = retval.runVector.normal();
    retval.antiNormVector = retval.normVector.scaled(-1.0);
    
    //
    // Backup point depends on inbound segment.  Here, null means you can't back up!
    //
    
    if (retval.inboundIsCanonical) {
      if (matchSeg == null) {
        retval.backupPoint = null;
        retval.backDrop = true;
      } else {
        String parent = matchSeg.getParent();
        if (parent == null) {
          retval.backupPoint = null;
        } else {
          retval.backupPoint = LinkSegmentID.buildIDForSegment(parent); 
          LinkSegment backSeg = getSegment(retval.backupPoint);
          Point2D backGeom = backSeg.getStart();
          Vector2D inboundVec = (new Vector2D(backGeom, useSeg.getStart())).normalized();
          // FIX ME?  Why are we not using the inbound canonical information in this circumstance?
          retval.backupPointIsInBackupDir = inboundVec.equals(retval.runVector);     
        }  
      }
    }
      
    String matchID = (startGeom == null) ? matchSeg.getID() : getRootSegment().getID();
       
    // Get the kid segments:
       
    ArrayList<LinkSegment> kidSegs = new ArrayList<LinkSegment>();
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String parent = ls.getParent();
      if (parent == null) {
        continue;
      }
      if (parent.equals(matchID)) {
        if (ls.getLength() == 0.0) {
          return (null);
        }   
        kidSegs.add(ls);
      }   
    }
    
    // Get the kid drops:
  
    ArrayList<LinkSegment> kidDrops = new ArrayList<LinkSegment>();   
    if (doDrops) {
      Iterator<LinkBusDrop> bdit = getDrops();
      while (bdit.hasNext()) {
        LinkBusDrop bd = bdit.next();
        if (bd.getDropType() == LinkBusDrop.START_DROP) {
          continue;
        }
        LinkSegment bdseg = getTargetDropSegment(bd, icx);
        String lsId = bd.getConnectionTag();
        LinkSegment dropParent = getSegment(lsId);
        String dropParentID = dropParent.getID();
        int sense = bd.getConnectionSense();
        if (sense == LinkBusDrop.CONNECT_TO_START) {
          dropParentID = dropParent.getParent();
          if (dropParentID == null) {
            dropParentID = getRootSegment().getID();
          }
        }
        if (matchID.equals(dropParentID)) {
          if (bdseg.getLength() == 0.0) {
            return (null);
          }  
          kidDrops.add(bdseg);
        }
      }
    }
 
    // Figure out the DOFs.
    
    double normMin = Double.POSITIVE_INFINITY;
    double antiNormMin = Double.POSITIVE_INFINITY;
    double runMin = Double.POSITIVE_INFINITY;
    double backupMin = Double.POSITIVE_INFINITY;
    
    Iterator<LinkSegment> ksit = kidSegs.iterator();
    while (ksit.hasNext()) {
      LinkSegment ls = ksit.next();
      Vector2D kidRun = ls.getRun();
      if (kidRun.equals(retval.backupVector)) {       
        if (!retval.inboundIsCanonical) {
          double length = ls.getLength();
          if (length < backupMin) {
            backupMin = length;
            retval.backupPoint = LinkSegmentID.buildIDForSegment(ls.getID());
          } 
        }
      } else if (kidRun.equals(retval.runVector)) {
        double length = ls.getLength();
        if (length < runMin) {
          runMin = length;
          retval.runPoint = LinkSegmentID.buildIDForSegment(ls.getID());
        }       
      } else if (kidRun.equals(retval.normVector)) {
        double length = ls.getLength();
        if (length < normMin) {
          normMin = length;
          retval.normPoint = LinkSegmentID.buildIDForSegment(ls.getID());
        }
      } else if (kidRun.equals(retval.antiNormVector)) {
        double length = ls.getLength();
        if (length < antiNormMin) {
          antiNormMin = length;
          retval.antiNormPoint = LinkSegmentID.buildIDForSegment(ls.getID());
        }        
      } 
    }    

    if (doDrops) {
      Iterator<LinkSegment> kdit = kidDrops.iterator();
      while (kdit.hasNext()) {
        LinkSegment ls = kdit.next();
        Vector2D kidRun = ls.getRun();
        if (kidRun.equals(retval.runVector)) {
          retval.runDrop = true;
        }
        if (kidRun.equals(retval.normVector)) {
          retval.normDrop = true;
        }
        if (kidRun.equals(retval.antiNormVector)) {
          retval.antiDrop = true; 
        }
      }
    }
     
    return (retval.clone()); // Want copies of end points...
  } 

  /***************************************************************************
  **
  ** repair one overlap error
  */
  
  private boolean repairOverlapErrors(DataAccessContext icx,
                                      RepairRequest request) {
 
    LinkSegment.OverlapResult ovrRes = request.ovrRes;
  
    LinkSegment mySeg = getSegmentGeometryForID(request.myID, icx, false);
    LinkSegment otherSeg = getSegmentGeometryForID(request.otherID, icx, false);
    int myDist = getSegmentDepth(request.myID);
    int otherDist = getSegmentDepth(request.otherID);
    
    //
    // Deal with cases where I am overlapped by the start point of my decendents or
    // an unrelated segment, or by the end point of my ancestor. (or reverse the my/other case)
    //
    
    if (((request.relation == SIBLING) || (request.relation == DECENDANT) || (request.relation == OTHER)) && 
        (ovrRes.myOverlapByStart != null)) {  
      Point2D splitPoint = otherSeg.getStart();
      boolean intersectsMyStart = splitPoint.equals(mySeg.getStart());
      boolean intersectsMyEnd = splitPoint.equals(mySeg.getEnd());
      if (intersectsMyStart) {
        return (false);
      }
      //
      // If we are dealing with an "OTHER", we only want moves that get higher up tree.  Otherwise,
      // we could get infinite cycling as the fix moves up then back down...
      //
      if ((request.relation == OTHER) && (myDist > otherDist)) {
        return (false);
      }
          
      boolean needMeSplit = !intersectsMyEnd;
      LinkSegmentID attachID;
      if (needMeSplit) {
        LinkSegmentID[] res = linkSplitSupport(request.myID, splitPoint);
        attachID = res[0].clone();
      } else {
        attachID = request.myID.clone();      
      }
      attachID.tagIDWithEndpoint(LinkSegmentID.END);
      LinkSegmentID reattachID = request.otherID.clone();
      reattachID.tagIDWithEndpoint(LinkSegmentID.START);
      moveSegmentOnTree(reattachID, attachID);
      return (true);
      
    } else if ((request.relation == ANCESTOR) && (ovrRes.myOverlapByEnd != null)) {
      Point2D splitPoint = otherSeg.getEnd();
      boolean intersectsMyStart = splitPoint.equals(mySeg.getStart());
      boolean intersectsMyEnd = splitPoint.equals(mySeg.getEnd());
      if (intersectsMyEnd) {
        return (false);
      }
      boolean needMeSplit = !intersectsMyStart;  
      LinkSegmentID reattachID;
      if (needMeSplit) {
        LinkSegmentID[] res = linkSplitSupport(request.myID, splitPoint);
        reattachID = res[0].clone();
      } else {
        reattachID = request.myID.clone();      
      }
      reattachID.tagIDWithEndpoint(LinkSegmentID.START);
      LinkSegmentID attachID = request.otherID.clone();
      attachID.tagIDWithEndpoint(LinkSegmentID.END);
      moveSegmentOnTree(reattachID, attachID);
      return (true);
    } else if ((request.relation == DECENDANT) && (ovrRes.otherOverlapByEnd != null)) { 
      Point2D splitPoint = mySeg.getEnd();
      boolean intersectsOtherStart = splitPoint.equals(otherSeg.getStart());
      boolean intersectsOtherEnd = splitPoint.equals(otherSeg.getEnd());
      if (intersectsOtherStart) {
        // FIXME? We are punting here...but is it the case that for full
        // robustness we need to handle this case if I am degenerate too?
        return (false);
      }
      boolean needOtherSplit = !intersectsOtherEnd;  
      LinkSegmentID attachID;
      if (needOtherSplit) {
        LinkSegmentID[] res = linkSplitSupport(request.otherID, splitPoint);
        attachID = res[1].clone();
      } else {
        attachID = request.otherID.clone();      
      }
      attachID.tagIDWithEndpoint(LinkSegmentID.START);
      LinkSegmentID reattachID = request.myID.clone();
      reattachID.tagIDWithEndpoint(LinkSegmentID.END);
      moveSegmentOnTree(reattachID, attachID);
      return (true);
 
    } else if (((request.relation == SIBLING) || (request.relation == ANCESTOR) || (request.relation == OTHER)) &&
               (ovrRes.otherOverlapByStart != null)) {
      Point2D splitPoint = mySeg.getStart();
      boolean intersectsOtherStart = splitPoint.equals(otherSeg.getStart());
      boolean intersectsOtherEnd = splitPoint.equals(otherSeg.getEnd());
      if (intersectsOtherStart) {      
        // FIXME? We are punting here...but is it the case that for full
        // robustness we need to handle this case if I am degenerate too?
        return (false);
      }
      //
      // If we are dealing with an "OTHER", we only want moves that get higher up tree.  Otherwise,
      // we could get infinite cycling as the fix moves up then back down...
      //
      if ((request.relation == OTHER) && (otherDist > myDist)) {
        return (false);
      }
          
      boolean needOtherSplit = !intersectsOtherEnd;  
      LinkSegmentID attachID;
      if (needOtherSplit) {
        LinkSegmentID[] res = linkSplitSupport(request.otherID, splitPoint);
        attachID = res[0].clone();
      } else {
        attachID = request.otherID.clone();      
      }
      attachID.tagIDWithEndpoint(LinkSegmentID.END);
      LinkSegmentID reattachID = request.myID.clone();
      reattachID.tagIDWithEndpoint(LinkSegmentID.START);
      moveSegmentOnTree(reattachID, attachID);
      return (true);
 
    } else if (((request.relation == DECENDANT) || (request.relation == OTHER)) && 
               (ovrRes.nonEndOverlap != null)) {  
      if ((request.relation == OTHER) && (otherDist > myDist)) {
        return (false);
      }          
      //
      // Gotta split me and keep the top, split him and keep the bottom:
      //
      LinkSegmentID[] res = linkSplitSupport(request.otherID, ovrRes.nonEndOverlap);
      LinkSegmentID attachID = res[1].clone();
      LinkSegmentID[] res2 = linkSplitSupport(request.myID, ovrRes.nonEndOverlap);
      LinkSegmentID reattachID = res2[0].clone();
   
      attachID.tagIDWithEndpoint(LinkSegmentID.START);
      reattachID.tagIDWithEndpoint(LinkSegmentID.END);
      moveSegmentOnTree(reattachID, attachID);
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Generate needed tree info for orthogonalization
  */  
 
  private FixOrthoOptions.FixOrthoTreeInfo generateTreeInfo(DataAccessContext icx) {
    FixOrthoOptions.FixOrthoTreeInfo retval = new FixOrthoOptions.FixOrthoTreeInfo(); 
    // public Map cornerDofs; // segID -> LinkProperties.CornerDOF
    // public Map parentIDs;  // segID -> parent segID
    // public Map forceDirs;  // segID -> FixOrthoOptions.ForceDirsAndMatches
    // public Map segGeoms;   // segID -> fake seg with geom
    List<LinkSegmentID> allIDs = getAllBusLinkSegments(icx, true);
    Iterator<LinkSegmentID> aiit = allIDs.iterator();
    while (aiit.hasNext()) {
      LinkSegmentID lsid = aiit.next();
      retval.allSegIDs.add(lsid);
      CornerDoF cdof = getCornerDoF(lsid, icx, true);
      if (cdof != null) {  // == null for weird corner cases: should really handle this!
        retval.cornerDofs.put(lsid, cdof);
      } 
      LinkSegment noSeg = getSegmentGeometryForID(lsid, icx, false);
      retval.segGeoms.put(lsid, noSeg);
      LinkSegmentID psid = getSegmentIDForParent(lsid);
      if (psid != null) {
        retval.parentIDs.put(lsid, psid);
      }
      FixOrthoOptions.ForceDirsAndMatches forceDirs = fixNonOrthoForDirs(lsid, noSeg, icx.getGenome(), icx.getLayout());
      retval.forceDirs.put(lsid, forceDirs);
    }
    retval.prepare(this);
    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Get forced directions for directs and for drops
  */  
 
  private FixOrthoOptions.ForceDirsAndMatches fixNonOrthoForDirs(LinkSegmentID segID, LinkSegment noSeg, Genome genome, Layout lo) {
    
    boolean isDirect = segID.isDirect();
    boolean forStart = segID.isForStartDrop();
    boolean forEnd = segID.isForEndDrop();    
    int type;
    if (isDirect) {
      type =  FixOrthoOptions.ForceDirsAndMatches.DIRECT;
    } else if (forStart) {
      type =  FixOrthoOptions.ForceDirsAndMatches.START_DROP;
    } else if (forEnd) {
      type =  FixOrthoOptions.ForceDirsAndMatches.END_DROP;
    } else {
      type =  FixOrthoOptions.ForceDirsAndMatches.NONE;
    }
 
    FixOrthoOptions.ForceDirsAndMatches retval = new FixOrthoOptions.ForceDirsAndMatches(type, segID); 
    boolean needSrcDir = forStart || isDirect;
    boolean needTrgDir = forEnd || isDirect;    
    if (needSrcDir) {
      retval.forceDirs[0] = getSourceForcedDir(genome, lo);
      retval.matches[0] = noSeg.getRun().equals(retval.forceDirs[0]);
    } 
    if (needTrgDir) {
      retval.forceDirs[1] = getTargetForcedDir(genome, lo, segID, isDirect);
      retval.matches[1] = noSeg.getRun().equals(retval.forceDirs[1]);
    }
      
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Merge a full tree into this one at a given segment
  */
  
  private SegResolve segmentResolution(LinkSegmentID sid) {  
    
    //
    // Figure out where we are going to connect up the new segment.  If we
    // have a real segment, we are in great shape.  If we have a drop, get
    // the parent segment instead.
    //
    
    SegResolve retval = new SegResolve();
    
    if (sid.isForSegment()) {
      retval.seg = getSegment(sid);
      if (sid.startEndpointIsTagged()) {
        retval.connectEnd = LinkBusDrop.CONNECT_TO_START;
        retval.ourConnectPos = (Point2D)retval.seg.getStart().clone();
        String parentID = retval.seg.getParent();
        if (parentID != null) {
          retval.seg = segments_.get(parentID);
          if (!retval.seg.isDegenerate()) {
            retval.connectEnd = LinkBusDrop.CONNECT_TO_END;
            retval.ourConnectPos = (Point2D)retval.seg.getEnd().clone();
          }
        }
      } else if (sid.endEndpointIsTagged()) {
        retval.connectEnd = LinkBusDrop.CONNECT_TO_END;
        // We are seing degenerate retval.segs.....
        retval.ourConnectPos = (retval.seg.isDegenerate()) ? (Point2D)retval.seg.getStart().clone() : (Point2D)retval.seg.getEnd().clone();
      } else {
        System.err.println("BAD SEGID " + sid);
        throw new IllegalArgumentException();
      }
    } else if (sid.isForStartDrop()) {
      LinkBusDrop drop = getRootDrop();
      retval.seg = getSegment(drop.getConnectionTag());
      retval.connectEnd = LinkBusDrop.CONNECT_TO_START;
      retval.ourConnectPos = (Point2D)retval.seg.getStart().clone();
    } else if (sid.isForEndDrop()) {
      LinkBusDrop drop = getTargetDrop(sid);
      retval.seg = getSegment(drop.getConnectionTag());
      retval.ourConnectPos = (Point2D)retval.seg.getStart().clone();
      retval.connectEnd = drop.getConnectionSense();
      if (retval.connectEnd == LinkBusDrop.CONNECT_TO_START) {
        String parentID = retval.seg.getParent();
        if (parentID != null) {
          retval.seg = segments_.get(parentID);
          if (!retval.seg.isDegenerate()) {
            retval.connectEnd = LinkBusDrop.CONNECT_TO_END;
            retval.ourConnectPos = (Point2D)retval.seg.getEnd().clone();
          }
        }
      } else if (retval.connectEnd == LinkBusDrop.CONNECT_TO_END) {
        retval.ourConnectPos = (Point2D)retval.seg.getEnd().clone();
      } else {
        System.err.println("BAD SEGID " + sid);
        throw new IllegalArgumentException();
      }
    } else {
      System.err.println("BAD SEGID " + sid);
      throw new IllegalArgumentException();
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Merge a full tree into this one at a given segment
  */
  
  public void mergeTreeToTreeAtSegment(LinkProperties obp, LinkSegmentID sid) {

    //
    // Figure out where we are going to connect up the new segment.  If we
    // have a real segment, we are in great shape.  If we have a drop, get
    // the parent segment instead.
    //
    
    SegResolve segr = segmentResolution(sid);
       
    //
    // If tree has NO links, get the target drop and install it.
    //

    if (obp.isDirect()) { // || (obp.getSegmentCount() == 1)) {
      LinkBusDrop targDrop = obp.getTargetDrop();
      LinkBusDrop drop = generateBusDrop(targDrop, segr.seg.getID(), LinkBusDrop.END_DROP, segr.connectEnd);
      drop.transferSpecialStyles(targDrop);
      addDrop(drop);
      return;
    }
    
    //
    // Fold the new segments in.  Gotta assign new IDs and map from the old.
    // Note we need to take the old degenerate root and make it into a full fledged
    // connection to us.
    //
    
    HashMap<String, String> segIDMap = new HashMap<String, String>();
    Iterator<LinkSegment> sit = obp.getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      String oldID = seg.getID();
      String newID = labels_.getNextLabel();
      segIDMap.put(oldID, newID);
    }
    sit = obp.getSegments();
    String rootTag = null;
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      String oldID = seg.getID();
      String newID = segIDMap.get(oldID);
      seg.setID(newID);
      String oldParent = seg.getParent();
      if (oldParent == null) {
        rootTag = oldID;
        seg.setParent(segr.seg.getID());
        seg.setEnd((Point2D)seg.getStart().clone());
        seg.setStart((Point2D)segr.ourConnectPos.clone());
      } else {
        seg.setParent(segIDMap.get(oldParent));
      }
      segments_.put(newID, seg);
    }    
    
    Iterator<LinkBusDrop> dit = obp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String oldTag = drop.getConnectionTag();
      drop.setConnectionTag(segIDMap.get(oldTag));
      if (oldTag.equals(rootTag) && drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
        drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
      }
      addDrop(drop); 
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Merge a set of replacement trees into this one at the given segments.  The deal here is
  ** that we already have drops and segments for the links the new piece is replacing.
  ** We need to get rid of those, but if we did that first, we might lose the segment
  ** that we are gluing into!
  */
  
  public void mergeReplacementSubTreesToTreeAtSegment(List<GlueJob> glueJobs, Genome genome) {
     
    HashSet<String> refLinks = new HashSet<String>();
    int numJobs = glueJobs.size();
    for (int i = 0; i < numJobs; i++) {
      GlueJob currJob = glueJobs.get(i);
      LinkProperties obp = currJob.newProps;
      LinkSegmentID sid = currJob.taggedWhip;
    
      //
      // Figure out where we are going to connect up the new segment.  If we
      // have a real segment, we are in great shape.  If we have a drop, get
      // the parent segment instead.
      //

      SegResolve segr = segmentResolution(sid);
       
      //
      // If tree has NO links, we balk:
      //

      if (obp.isDirect()) {
        throw new IllegalArgumentException();
      }

      //
      // Fold the new segments in.  Gotta assign new IDs and map from the old.
      // Note we need to take the old degenerate root and make it into a full fledged
      // connection to us.
      //

      HashMap<String, String> segIDMap = new HashMap<String, String>();
      Iterator<LinkSegment> sit = obp.getSegments();
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        String oldID = seg.getID();
        String newID = labels_.getNextLabel();
        segIDMap.put(oldID, newID);
      }
      sit = obp.getSegments();
      String rootTag = null;
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        String oldID = seg.getID();
        String newID = segIDMap.get(oldID);
        seg.setID(newID);
        String oldParent = seg.getParent();
        if (oldParent == null) {
          rootTag = oldID;
          seg.setParent(segr.seg.getID());
          seg.setEnd((Point2D)seg.getStart().clone());
          seg.setStart((Point2D)segr.ourConnectPos.clone());
        } else {
          seg.setParent(segIDMap.get(oldParent));
        }
        segments_.put(newID, seg);
      }
 
    
      //
      // Get rid of the existing drops matching the ones
      // we are merging in.  First build a list of drops
      // we are ditching:
      //

      HashSet<String> replacingLinks = new HashSet<String>();
      Iterator<LinkBusDrop> dit = obp.getDrops();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        if (drop.getDropType() == LinkBusDrop.START_DROP) {
          continue;
        }
        replacingLinks.add(drop.getTargetRef());
      }

      //
      // Track the segments that parented the drops
      // as we kill the drops off:
      //

      
      Iterator<String> rlit = replacingLinks.iterator();
      while (rlit.hasNext()) {
        String linkID = rlit.next();
        dit = getDrops();
        while (dit.hasNext()) {
          LinkBusDrop nextDrop = dit.next();
          String tRef = nextDrop.getTargetRef();
          if ((tRef != null) && tRef.equals(linkID)) {
            refLinks.add(nextDrop.getConnectionTag());
            drops_.remove(nextDrop);
            break;
          }
        }
      }
    
      //
      // Add in new drops:
      //

      dit = obp.getDrops();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        if (drop.getDropType() == LinkBusDrop.START_DROP) {
          continue;
        }
        String oldTag = drop.getConnectionTag();
        drop.setConnectionTag(segIDMap.get(oldTag));
        if (oldTag.equals(rootTag) && drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
          drop.setConnectionSense(LinkBusDrop.CONNECT_TO_END);
        }
        addDrop(drop); 
      }
    }
    
    //
    // Now that we have glued all the new stuff in, and fixed up the drops, 
    // kill off all dangling segments:
    //
    
    //
    // Note this assumes we only connect to the start of the degenerate 
    // root segment of the tree:
    //

    Iterator<String> reflit = refLinks.iterator();
    while (reflit.hasNext()) {
      String nextID = reflit.next();
      do {
        LinkSegment curr = segments_.get(nextID);
        if (curr == null) {  // My parent has already been deleted...
          break;
        }
        boolean mustKeep = false;
        Iterator<LinkBusDrop> dit = getDrops();
        while (dit.hasNext()) {
          LinkBusDrop drop = dit.next();
          if (isSegmentAnAncestor(nextID, drop)) {
            mustKeep = true;
            break;        
          }
        }
        //
        // Don't need to manage parents; all children must be gone:
        //
        if (!mustKeep) {
          labels_.removeLabel(nextID);
          segments_.remove(nextID);
        }
        nextID = curr.getParent();
      } while (nextID != null);    
    }
  
    return;
  } 
 
  
  /***************************************************************************
  **
  ** Relocate an internal segment to another parent.  DITCH THIS
  */

  public boolean moveSegmentOnTree(LinkSegment moveSeg, LinkSegment targSeg, boolean useEnd) {  
    return (moveSegmentOnTreeGuts(moveSeg, false, null, targSeg, false, null, useEnd));
  }
  
  /***************************************************************************
  **
  ** Relocate an internal segment to another parent
  */

  public boolean moveSegmentOnTree(LinkSegmentID moveSegID, LinkSegmentID targSegID) {  
    
    LinkSegment moveSeg = null;
    LinkSegment targSeg = null;
    boolean isDrop = false;
    String dropID = null;
    boolean targIsDrop = false;
    String targDropID = null;

    if (moveSegID.isForSegment()) {
      moveSeg = getSegment(moveSegID);
    } else if (moveSegID.isForStartDrop()) {
      isDrop = true;
      dropID = null;
    } else if (moveSegID.isForEndDrop()) {
      isDrop = true;
      dropID = moveSegID.getEndDropLinkRef();
    } else {
      throw new IllegalArgumentException();
    }
    
    if (targSegID.isForSegment()) {
      targSeg = getSegment(targSegID);
    } else if (targSegID.isForStartDrop()) {
      targIsDrop = true;
      targDropID = null;
    } else if (targSegID.isForEndDrop()) {
      targIsDrop = true;
      targDropID = targSegID.getEndDropLinkRef();
    } else {
      throw new IllegalArgumentException();
    }
    
    boolean toEnd = targSegID.endEndpointIsTagged();
    
    return (moveSegmentOnTreeGuts(moveSeg, isDrop, dropID,
                                  targSeg, targIsDrop, targDropID, toEnd));     
  }
 
  /***************************************************************************
  **
  ** Relocate an internal segment to another parent. Used ONLY for simple specialty layout case where the
  ** relocates are from a root segment drop to a segment end. Also uses efficient cache for bus drop lookup
  */

  public boolean moveSegmentOnTreeSpecialtyBatch(LinkSegmentID moveSegID, LinkSegmentID targSegID,  Map<String, LinkBusDrop> lbdm) {  
    
    LinkSegment moveSeg = null;
    LinkSegment targSeg = null;
    boolean isDrop = false;
    String dropID = null;
    boolean targIsDrop = false;
    String targDropID = null;

    if (moveSegID.isForSegment()) {
      moveSeg = getSegment(moveSegID);
    } else if (moveSegID.isForStartDrop()) {
      isDrop = true;
      dropID = null;
    } else if (moveSegID.isForEndDrop()) {
      isDrop = true;
      dropID = moveSegID.getEndDropLinkRef();
    } else {
      throw new IllegalArgumentException();
    }
    
    if (targSegID.isForSegment()) {
      targSeg = getSegment(targSegID);
    } else if (targSegID.isForStartDrop()) {
      targIsDrop = true;
      targDropID = null;
    } else if (targSegID.isForEndDrop()) {
      targIsDrop = true;
      targDropID = targSegID.getEndDropLinkRef();
    } else {
      throw new IllegalArgumentException();
    }
    
    boolean toEnd = targSegID.endEndpointIsTagged();
    return (simpleMoveSegmentOnTreeBatchGuts(moveSeg, isDrop, dropID,
                                              targSeg, targIsDrop, targDropID, toEnd, lbdm));     
  }

  /***************************************************************************
  **
  ** We are handed a linkage ID for a link being deleted.  We need to remove 
  ** support for it from the link tree, including any orphaned internal links.
  */
  
  public void removeLinkSupport(String linkageID) {

    //
    // Find the drop that is supporting the link, and delete it.
    // Then go to the referenced internal link, see if any other
    // end drops pass through it, and delete it if not.  Continue up
    // the tree in this fashion.  

    String refLink = null;
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      String tRef = nextDrop.getTargetRef();
      if ((tRef != null) && tRef.equals(linkageID)) {
        refLink = nextDrop.getConnectionTag();
        drops_.remove(nextDrop);
        break;
      }
    }
    
    if (isDirect()) {
      return;
    }    
        
    //
    // Note this assumes we only connect to the start of the degenerate 
    // root segment of the tree:
    //

    String nextID = refLink;
    do {
      LinkSegment curr = segments_.get(nextID);
      boolean mustKeep = false;
      dit = getDrops();
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        if (isSegmentAnAncestor(nextID, drop)) {
          mustKeep = true;
          break;        
        }
      }
      //
      // Don't need to manage parents; all children must be gone:
      //
      if (!mustKeep) {
        labels_.removeLabel(nextID);
        segments_.remove(nextID);
      }
      nextID = curr.getParent();
    } while (nextID != null);
    
    return;
  }
  
  /***************************************************************************
  **
  ** We are handed a linkage ID for a link being deleted.  We need to remove 
  ** support for it from the link tree, including any orphaned internal links.
  */
  
  public void removeLotsaLinksSupport(Set<String> linkageIDs, InvertedLinkProps ilp) {

    //
    // Find the drop that is supporting the link, and delete it.
    // Then go to the referenced internal link, see if any other
    // end drops pass through it, and delete it if not.  Continue up
    // the tree in this fashion.  

    Iterator<String> lidit = linkageIDs.iterator();
    while (lidit.hasNext()) {
      String linkageID = lidit.next();
      LinkBusDrop lbp = ilp.getDropForLink(linkageID);
      drops_.remove(lbp);
      ilp.removeDrop(linkageID);
    }
    
    if (isDirect()) {
      return;
    }    
        
  
    HashSet<String> deadSeg = new HashSet<String>(segments_.keySet());
    Set<String> remSeg = ilp.remainingSegments();
    deadSeg.removeAll(remSeg);
    
    //
    // Kill the dead segments:
    //
    
    Iterator<String> dsit = deadSeg.iterator();
    while (dsit.hasNext()) {
      String segID = dsit.next();
      labels_.removeLabel(segID);
      segments_.remove(segID);
    }
     
    return;
  }
  
  
  /***************************************************************************
  **
  ** Build segment tree
  */
  
  public SegmentWithKids buildSegmentTree(DataAccessContext icx) {

    if (isDirect()) {
      return (null);
    }
    
    //
    // Start with the source drop:
    //
    
    LinkSegment topSegment = getSourceDropSegment(icx, true);
    SegmentWithKids retval = new SegmentWithKids(topSegment);
    HashMap<String, SegmentWithKids> skMap = new HashMap<String, SegmentWithKids>();
    LinkSegment rootSeg = getRootSegment();
    SegmentWithKids rootsk = new SegmentWithKids(rootSeg);
    retval.kids.add(rootsk);
    skMap.put(rootSeg.getID(), rootsk);
    
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String parent = ls.getParent();
      if (parent == null) {
        continue;
      }
      SegmentWithKids mySk = skMap.get(ls.getID());
      if (mySk == null) {
        mySk = new SegmentWithKids(ls);
        skMap.put(ls.getID(), mySk);
      }
      SegmentWithKids parentSK = skMap.get(parent);
      if (parentSK == null) {
        parentSK = new SegmentWithKids(getSegment(parent));
        skMap.put(parent, parentSK);
      }
      if (!parentSK.kids.contains(mySk)) {
        parentSK.kids.add(mySk);
      }
    }
    
    Iterator<LinkBusDrop> bdit = getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop bd = bdit.next();
      if (bd.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      LinkSegment bdseg = getTargetDropSegment(bd, icx);
      String lsId = bd.getConnectionTag();
      LinkSegment dropParent = getSegment(lsId);
      int sense = bd.getConnectionSense();
      if (sense == LinkBusDrop.CONNECT_TO_START) {
        String dropParentID = dropParent.getParent();
        if (dropParentID != null) {
          dropParent = getSegment(dropParentID);
        }
      }
      SegmentWithKids parentSK = skMap.get(dropParent.getID());
      bdseg.setParent(dropParent.getID());
      parentSK.kids.add(new SegmentWithKids(bdseg));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the child seg IDs (both for drops and segs)
  */
  
  public List<LinkSegmentID> getChildSegs(LinkSegmentID lsid) {
    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
    if (lsid.isDirectOrEndDrop()) {
      return (retval);
    }
    String matchID;
    if (lsid.isForStartDrop()) {
      matchID = getRootSegment().getID();   
    } else {
      matchID = getSegment(lsid).getID();
    }
    
    // Get the kid segments:
       
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String parent = ls.getParent();
      if (parent == null) {
        continue;
      }
      if (parent.equals(matchID)) {
        retval.add(LinkSegmentID.buildIDForSegment(ls.getID()));
      }
    }
    
    // Get the kid drops:

    Iterator<LinkBusDrop> bdit = getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop bd = bdit.next();
      if (bd.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      } 
      String lsId = bd.getConnectionTag();
      LinkSegment dropParent = getSegment(lsId);
      String dropParentID = dropParent.getID();
      int sense = bd.getConnectionSense();
      if (sense == LinkBusDrop.CONNECT_TO_START) {
        String metaParent = dropParent.getParent();
        if (metaParent != null) {
          dropParentID = metaParent;
        }
      }
      if (matchID.equals(dropParentID)) {
        retval.add(LinkSegmentID.buildIDForEndDrop(bd.getTargetRef()));
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the breadth-first listing of the tree targets (link IDS)
  */
  
  public List<String> getBreadthFirstOrder() {
    
    ArrayList<String> retval = new ArrayList<String>();

    Map<LinkSegmentID, SortedSet<LinkSegmentID>> tree = invertTree();  
    
    if (tree == null) {
      LinkBusDrop bd = getTargetDrop();
      retval.add(bd.getTargetRef());
      return (retval);
    }
      
    ArrayList<LinkSegmentID> searchList = new ArrayList<LinkSegmentID>();
    searchList.add(LinkSegmentID.buildIDForSegment(getRootSegment().getID()));
    while (!searchList.isEmpty()) {
      LinkSegmentID nextID = searchList.remove(0);
      SortedSet<LinkSegmentID> kids = tree.get(nextID);
      Iterator<LinkSegmentID> kit = kids.iterator();
      while (kit.hasNext()) {
        LinkSegmentID kidID = kit.next();
        if (kidID.isForDrop()) {
          retval.add(kidID.getEndDropLinkRef());
        } else {
          searchList.add(kidID);
        }
      }
      
    }   
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Get the depth-first listing of the tree targets (link IDS)
  */
  
  public String getDepthFirstDebug(DataAccessContext icx) {
    
    StringBuffer retval = new StringBuffer();

    Map<LinkSegmentID, SortedSet<LinkSegmentID>> tree = invertTree();  
    
    if (tree == null) {
      LinkBusDrop bd = getTargetDrop();
      retval.append(bd.getTargetRef());
      return (retval.toString());
    }
      
    ArrayList<LinkSegmentID> searchList = new ArrayList<LinkSegmentID>();
    searchList.add(LinkSegmentID.buildIDForSegment(getRootSegment().getID()));
    while (!searchList.isEmpty()) {
      LinkSegmentID nextID = searchList.remove(0);
      LinkSegment lseg = getSegment(nextID);
      int depth = getSegmentsToRoot(lseg).size() - 1;
      for (int i = 0; i < depth; i++) {
        retval.append("   ");
      }
      if (lseg.isDegenerate()) {
        retval.append(lseg.getStart());
      } else {
        retval.append(lseg.getStart() + " - " + lseg.getEnd() + " " + lseg.getRun());
      }
      retval.append("[");
      retval.append(lseg.getID());
      retval.append("]\n");
      SortedSet<LinkSegmentID> kids = tree.get(nextID);
      Iterator<LinkSegmentID> kit = kids.iterator();
      while (kit.hasNext()) {
        LinkSegmentID kidID = kit.next();
        if (kidID.isForDrop()) {
          for (int i = 0; i <= depth; i++) {
            retval.append("   ");
          }
          retval.append(kidID.getEndDropLinkRef());
          retval.append("->");
          LinkSegment mySeg = getSegmentGeometryForID(kidID, icx, false);
          retval.append(mySeg.getRun());
          retval.append("\n");
        } else {         
          searchList.add(0, kidID);
        }
      }
      
    }   
    return (retval.toString());    
  }  
  
 
  /***************************************************************************
  **
  ** Get a mapping of all segments to links thru the segment.
  */
  
  public Map<LinkSegmentID, Set<String>> getFullSegmentToLinkMap() {  
  
    HashMap<LinkSegmentID, Set<String>> retval = new HashMap<LinkSegmentID, Set<String>>();
    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String linkID = drop.getTargetRef();
      if (drop.getTargetRef() == null) {
        continue;
      }
      List<LinkSegmentID> losid = getSegmentIDsToRootForEndDrop(drop, true);
      int numID = losid.size();
      HashSet<String> oneLink = new HashSet<String>();
      oneLink.add(linkID);
      retval.put(losid.get(0), oneLink);
      for (int i = 1; i < numID; i++) {
        LinkSegmentID lsid = losid.get(i);
        Set<String> forLsid = retval.get(lsid);
        if (forLsid == null) {
          forLsid = new HashSet<String>();
          retval.put(lsid, forLsid);
        }
        forLsid.add(linkID);
      }
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Do a tree invert to allow tree operations
  */
  
  public Map<LinkSegmentID, SortedSet<LinkSegmentID>> invertTree() {
    
    if (isDirect()) {
      return (null);
    }
    
    HashMap<LinkSegmentID, SortedSet<LinkSegmentID>> retval = new HashMap<LinkSegmentID, SortedSet<LinkSegmentID>>();

    // Get the kid segments:
       
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String parent = ls.getParent();
      
      if (parent == null) {
        continue;
      }
      LinkSegmentID kidID = LinkSegmentID.buildIDForSegment(ls.getID());
      LinkSegmentID parentID = LinkSegmentID.buildIDForSegment(parent);
      SortedSet<LinkSegmentID> kids = retval.get(parentID);
      if (kids == null) {
        kids = new TreeSet<LinkSegmentID>();
        retval.put(parentID, kids);
      }
      kids.add(kidID);
    }
     
    
    // Get the kid drops:

    Iterator<LinkBusDrop> bdit = getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop bd = bdit.next();
      if (bd.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      } 
      String lsId = bd.getConnectionTag();
      LinkSegment dropParent = getSegment(lsId);
      String dropParentID = dropParent.getID();
      int sense = bd.getConnectionSense();
      if (sense == LinkBusDrop.CONNECT_TO_START) {
        String metaParent = dropParent.getParent();
        if (metaParent != null) {
          dropParentID = metaParent;
        }
      }
      LinkSegmentID kidID = LinkSegmentID.buildIDForEndDrop(bd.getTargetRef());
      LinkSegmentID parentID = LinkSegmentID.buildIDForSegment(dropParentID);
      SortedSet<LinkSegmentID> kids = retval.get(parentID);
      if (kids == null) {
        kids = new TreeSet<LinkSegmentID>();
        retval.put(parentID, kids);
      }
      kids.add(kidID);
    }
    return (retval);
  }  
  

  /***************************************************************************
  **
  ** Support for debug
  **
  */
  
  public String toString() {
    return (" textPos = " + textPos_ + " textDirTag = " + textDirTag_ +
            " srcTag = " + srcTag_ + " segments = " + segments_ + " drops = " + drops_ + " labels = " + labels_);
  }
  
  /***************************************************************************
  **
  ** Support for debug
  **
  */
  
  public boolean emptyLabels() {
    return (labels_.isEmpty());
  } 
  
  /***************************************************************************
  **
  ** Return a copy of the drops
  */

  public List<LinkBusDrop> copyDrops() {
    ArrayList<LinkBusDrop> retval = new ArrayList<LinkBusDrop>();
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      retval.add(nextDrop.clone());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** restore drops to match the given copy
  */

  public void restoreDrops(List<LinkBusDrop> oldDrops) {
    drops_.clear();
    Iterator<LinkBusDrop> dit = oldDrops.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      drops_.add(nextDrop.clone());
    } 
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // ABSTRACT METHODS TO BE IMPLEMENTED
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Init a grid for fixing non-ortho links
  */  
  
  protected abstract LinkPlacementGrid initGridForOrthoFix(LinkRouter router, DataAccessContext icx, 
                                                           String overID, BTProgressMonitor monitor) 
                                                             throws AsynchExitRequestException;  
  
  /***************************************************************************
  **
  ** Return a new link properties that is a direct link to the given target
  */  
  
  public abstract LinkProperties deriveDirectLink(String newSource, LinkBusDrop keepDrop, Object extraInfo);  
  
  /***************************************************************************
  **
  ** Get a fake "link segment" for a direct path that goes to the pads.
  */
  
  public abstract LinkSegment getDirectLinkPath(DataAccessContext icx);  
  
 /***************************************************************************
  **
  ** Get the start drop segment for this link tree
  */
  
  protected abstract LinkSegment getSourceDropSegment(DataAccessContext icx, boolean forceToGrid);
  
  /***************************************************************************
  **
  ** Get a target drop segment
  */
  
  protected abstract LinkSegment getTargetDropSegment(LinkBusDrop drop, DataAccessContext icx);

  /***************************************************************************
  **
  ** Answer if the source drop is non-orthogonal
  */
  
  protected abstract boolean srcDropIsNonOrtho(DataAccessContext icx);

  /***************************************************************************
  **
  ** Answer if the target drop is non-orthogonal
  */
  
  protected abstract boolean targetDropIsNonOrtho(LinkBusDrop drop, DataAccessContext icx);
  
  /***************************************************************************
  **
  ** Answer if the given link is in the model
  */
  
  public abstract boolean linkIsInModel(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, String linkID); 
  
  /***************************************************************************
  **
  ** Abstracted source check
  */
  
  protected abstract void sourceSanityCheck(Genome genome, LinkProperties other);
  
  /***************************************************************************
  **
  ** Get link label.  May be null
  */
  
  protected abstract String getLinkLabel(Genome genome, String linkID);
   
  /***************************************************************************
  **
  ** Get link target.
  */
  
  public abstract String getLinkTarget(Genome genome, String linkID);
    
  /***************************************************************************
  **
  ** Get custom bus drops:
  */  
  
  protected abstract LinkBusDrop generateBusDrop(LinkBusDrop spDrop, 
                                                 String ourConnection, 
                                                 int dropType, int connectionEnd);

  /***************************************************************************
  **
  ** Get custom bus drops:
  */  
  
  protected abstract LinkBusDrop generateBusDrop(String targRef,
                                                 String ourConnection, 
                                                 int dropType, int connectionEnd, Object extraInfo);  
  
  /***************************************************************************
  **
  ** Get a quick kill thickess:
  */  
  
  protected abstract ThickInfo maxThickForIntersect(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, double intersectTol);
  
  protected static class ThickInfo {
    int maxThick;
    boolean twoPass;
  } 
  
  /***************************************************************************
  **
  ** Handle source move operations:
  */  
  
  protected abstract void moveSource(Object extraInfo);
    
  
  /***************************************************************************
  **
  ** Write the item to XML
  */
  
  public abstract void writeXML(PrintWriter out, Indenter ind);
  
  /***************************************************************************
  **
  ** Shift drop ends
  */
  
  protected abstract void shiftDropEnds(double dx, double dy, Vector2D sideDir);
 
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  protected abstract void moveDropEndsPerMap(Map<Point2D, Point2D> mappedPositions, boolean forceToGrid);
  
   
  /***************************************************************************
  **
  ** Shift drop ends
  */
  
  protected abstract void shiftSelectedDropEnds(double dx, double dy, LinkFragmentShifts shift);
  
  /***************************************************************************
  **
  ** Shift drop ends
  */
  
  protected abstract void shiftDropEndsForExpandCompressOps(SortedSet<Integer> newRows, SortedSet<Integer> newCols, Rectangle bounds, double sign, int mult);  
  
  /***************************************************************************
  **
  ** Get a forced source direction
  */
  
  protected abstract Vector2D getSourceForcedDir(Genome genome, Layout lo);
  
  /***************************************************************************
  **
  ** Get a forced target direction
  */
  
  protected abstract Vector2D getTargetForcedDir(Genome genome, Layout lo, LinkSegmentID segID, boolean isDirect);
  
  /***************************************************************************
  **
  ** Answer of we can relocate an internal segment to another parent
  */
  
  public abstract boolean canRelocateSegmentOnTree(LinkSegmentID moveID);
  
  
  /***************************************************************************
  **
  ** Get some link ID...
  */
  
  public abstract String getALinkID(Genome genome);
  
  /***************************************************************************
  **
  ** Shift the end location.  Note that multiple ends terminating in the
  ** same target will get shifted.
 
  
  public abstract void shiftEnd(Layout layout, Genome genome, 
                                String target, double dx, double dy);
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Support for XML loading
  */  
   
  protected void loadFromXML(String color, 
                             String ourSource, String lineStyle,
                             String txtX, String txtY, String txtDir) throws IOException {
    
    if ((txtX != null) && (txtY != null)) {
      float tx = 0.0F;
      float ty = 0.0F;
      try {
        tx = Float.parseFloat(txtX);
        ty = Float.parseFloat(txtY);
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
      textPos_ = new Point2D.Float(tx, ty);
    }

    textDir_ = LEFT;
    if (txtDir != null) {
      txtDir = txtDir.trim();
      if (txtDir.equals("")) {
        textDir_ = LEFT;     
      } else if (txtDir.equalsIgnoreCase("left")) {
        textDir_ = LEFT;
      } else if (txtDir.equalsIgnoreCase("right")) {
        textDir_ = RIGHT;
      } else if (txtDir.equalsIgnoreCase("up")) {
        textDir_ = UP;
      } else if (txtDir.equalsIgnoreCase("down")) {
        textDir_ = DOWN;
      } else {
        throw new IOException();
      }
    }   
    textDirTag_ = txtDir;

    setLegacyStyle(color, lineStyle);
        
    segments_ = new HashMap<String, LinkSegment>();
    drops_ = new ArrayList<LinkBusDrop>();
    labels_ = new UniqueLabeller();
    
    srcTag_ = ourSource;
  }
  
  /***************************************************************************
  **
  ** Support for legacy style resolution
  */
  
  protected void setLegacyStyle(String color, String lineStyle) {
    //
    // Legacy only; this is now loaded in using a subsequent element...
    //
    if (color != null) {
      if (lineStyle == null) {
        lineStyle = "";
      }
      drawStyle_ = SuggestedDrawStyle.buildFromLegacy(mapFromStyleTag(lineStyle));
      drawStyle_.setColorName(color);
    }    
    return;
  }  
   
  /***************************************************************************
  **
  ** Get all the child segments of this segment
  */
  
  protected List<LinkSegment> getChildSegments(LinkSegment seg) {
    
    ArrayList<LinkSegment> retval = new ArrayList<LinkSegment>();
    String segID = seg.getID();

    //
    // Check segments
    //
    
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment nextSeg = sit.next();
      if (seg == nextSeg) {
        continue;
      }
      String nextParent = nextSeg.getParent();
      if (nextParent == null) {
        continue;
      }
      if (nextParent.equals(segID)) {
        retval.add(nextSeg);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get all the child drops of this segment
  */
  
  protected List<LinkBusDrop> getChildDrops(LinkSegment seg, List<LinkSegment> childSegs) {
    
    ArrayList<LinkBusDrop> retval = new ArrayList<LinkBusDrop>();
    String segID = seg.getID();
    int numKids = childSegs.size();
    
    //
    // Check drops.  Drops from our end, or from
    // the start of any of our children, 
    // count as a sibling.
    //
    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String conTag = drop.getConnectionTag();
      int sense = drop.getConnectionSense();
      if ((sense == LinkBusDrop.CONNECT_TO_END) && (conTag.equals(segID))) {
        retval.add(drop);
      } else if (sense == LinkBusDrop.CONNECT_TO_START) {
        for (int i = 0; i < numKids; i++) {
          LinkSegment kidSeg = childSegs.get(i);
          if (kidSeg.getID().equals(conTag)) {
            retval.add(drop);
          }          
        }
        // Drop attached to start of degenerate root:
        if (seg.isDegenerate() && (conTag.equals(segID))) {
          retval.add(drop);
        }            
      } 
    }
    return (retval);
  }  
  
 
  /***************************************************************************
  **
  ** Relocate an internal segment to another parent
  */

  protected boolean moveSegmentOnTreeGuts(LinkSegment moveSeg, boolean isDrop, String dropID,
                                          LinkSegment targSeg, boolean targIsDrop, String targDropID, 
                                          boolean toEnd) {          
  
    //
    // 1) Find the target segment of the tree
    // 2) Change the parent of the segment to the target
    // 3) Change the start point to the target
    // 4) Dangling ends without drops need to be tossed out 
    //
    
    //
    // If the target segment is a drop, we need to resolve that to its link parent,
    //

    if (targIsDrop) {
      if (targDropID == null) {
        targSeg = getRootSegment();
        toEnd = false;
      } else {
        Iterator<LinkBusDrop> drit = getDrops();
        while (drit.hasNext()) {
          LinkBusDrop drop = drit.next();
          String ref = drop.getTargetRef();
          if (ref == null) continue;
          if (ref.equals(targDropID)) {
            targSeg = getSegment(drop.getConnectionTag());
            toEnd = (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_END);
            break;
          }
        }
      }
    }                                     
                    
    //                       
    // If the move segment is trying to reparent to one of its decendants,
    // we gotta bag it!
    //
    
    if (!isDrop) {
      List<LinkSegment> path = getSegmentsToRoot(targSeg);
      Iterator<LinkSegment> pit = path.iterator();
      String msid = moveSeg.getID();
      while (pit.hasNext()) {
        LinkSegment ls = pit.next();
        if (msid.equals(ls.getID())) {
          return (false);
        }
      }
    }
                  
    //
    // Figure out the merge point and force it to the end point (except root)
    //
                                     
    Point2D mergePt;
    int sense;
    if (!toEnd) {  // i.e. is to start
      String parent = targSeg.getParent();
      if (parent != null) {
        targSeg = getSegment(parent);
        if (targSeg.isDegenerate()) {
          mergePt = targSeg.getStart();
          sense = LinkBusDrop.CONNECT_TO_START;
        } else {
          mergePt = targSeg.getEnd();
          sense = LinkBusDrop.CONNECT_TO_END;
        }
      } else {
        mergePt = targSeg.getStart();
        sense = LinkBusDrop.CONNECT_TO_START;
      }
    } else {
      mergePt = targSeg.getEnd();
      sense = LinkBusDrop.CONNECT_TO_END;
    }
        
    if (!isDrop) {
      String parentID = targSeg.getID();   
      moveSeg.setParent(parentID);
      moveSeg.setStart((Point2D)mergePt.clone());
    } else {
      if (dropID == null) {
        return (false);  // Makes no sense to relocate root drop
      }
      Iterator<LinkBusDrop> drit = getDrops();
      while (drit.hasNext()) {
        LinkBusDrop drop = drit.next();
        String ref = drop.getTargetRef();
        if (ref == null) continue;
        if (ref.equals(dropID)) {
          drop.setConnectionTag(targSeg.getID());
          drop.setConnectionSense(sense);
          break;
        }
      }
    }  

    //
    // Go through all the remaining bus drops find their paths to the root.
    // Only retain those segments that get on that list.
    //
   
    HashSet<String> discardedSegments = new HashSet<String>();
    Iterator<LinkSegment> lsit = getSegments();
    while (lsit.hasNext()) {
      LinkSegment ls = lsit.next();
      discardedSegments.add(ls.getID());
    }

    Iterator<LinkBusDrop> drit = getDrops();
    while (drit.hasNext()) {
      LinkBusDrop drop = drit.next();
      List<LinkSegment> path = this.getSegmentsToRoot(drop);
      Iterator<LinkSegment> pit = path.iterator();
      while (pit.hasNext()) {
        LinkSegment ls = pit.next();
        discardedSegments.remove(ls.getID());
      }
    }

    Iterator<String> dsit = discardedSegments.iterator();
    while (dsit.hasNext()) {
      String lsid = dsit.next();
      labels_.removeLabel(lsid);
      segments_.remove(lsid);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Relocate an internal segment to another parent. Used ONLY for simple specialty layout case where the
  ** relocates are from a root segment drop to a segment end. Also uses efficient cache for bus drop lookup
  */

  protected boolean simpleMoveSegmentOnTreeBatchGuts(LinkSegment moveSeg, boolean isDrop, String dropID,
                                                     LinkSegment targSeg, boolean targIsDrop, String targDropID, 
                                                     boolean toEnd, Map<String, LinkBusDrop> lbdm) {          
  
    //
    // 1) Find the target segment of the tree
    // 2) Change the parent of the segment to the target
    // 3) Change the start point to the target
    // 4) Dangling ends without drops need to be tossed out 
    //
    
    //
    // If the target segment is a drop, we need to resolve that to its link parent. Note that we have
    // specified the start drop as the target if we are going to glue to the root segment!
    //
    
    if (targIsDrop) {
      if (targDropID == null) {
        targSeg = getRootSegment();
        toEnd = false;
      } else {
        Iterator<LinkBusDrop> drit = getDrops();
        while (drit.hasNext()) {
          LinkBusDrop drop = drit.next();
          String ref = drop.getTargetRef();
          if (ref == null) continue;
          if (ref.equals(targDropID)) {
            targSeg = getSegment(drop.getConnectionTag());
            toEnd = (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_END);
            break;
          }
        }
      }
    }                                     
                    
    //                       
    // If the move segment is trying to reparent to one of its decendants,
    // we gotta bag it!
    //
    
    if (!isDrop) {
      List<LinkSegment> path = getSegmentsToRoot(targSeg);
      Iterator<LinkSegment> pit = path.iterator();
      String msid = moveSeg.getID();
      while (pit.hasNext()) {
        LinkSegment ls = pit.next();
        if (msid.equals(ls.getID())) {
          return (false);
        }
      }
    }
                  
    //
    // Figure out the merge point and force it to the end point (except root)
    //
                                     
    Point2D mergePt;
    int sense;
    if (!toEnd) {  // i.e. is to start
      String parent = targSeg.getParent();
      if (parent != null) {
        targSeg = getSegment(parent);
        if (targSeg.isDegenerate()) {
          mergePt = targSeg.getStart();
          sense = LinkBusDrop.CONNECT_TO_START;
        } else {
          mergePt = targSeg.getEnd();
          sense = LinkBusDrop.CONNECT_TO_END;
        }
      } else {
        mergePt = targSeg.getStart();
        sense = LinkBusDrop.CONNECT_TO_START;
      }
    } else {
      mergePt = targSeg.getEnd();
      sense = LinkBusDrop.CONNECT_TO_END;
    }
        
    if (!isDrop) {
      String parentID = targSeg.getID();   
      moveSeg.setParent(parentID);
      moveSeg.setStart((Point2D)mergePt.clone());
    } else {
      if (dropID == null) {
        return (false);  // Makes no sense to relocate root drop
      }
      LinkBusDrop drop = lbdm.get(dropID);
      drop.setConnectionTag(targSeg.getID());
      drop.setConnectionSense(sense);
    } 
    return (true);
  }
  
  /***************************************************************************
  **
  ** Make a fast lookup for drops
  */

  public Map<String, LinkBusDrop> prepForBatchMoves() {          
    Map<String, LinkBusDrop> retval = new HashMap<String, LinkBusDrop>();
    Iterator<LinkBusDrop> drit = getDrops();
    while (drit.hasNext()) {
      LinkBusDrop drop = drit.next();
      String ref = drop.getTargetRef();
      if (ref == null) continue;
      retval.put(ref, drop);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Shift the straight run of segments, modifying given parent, and attached segments.
  */

  protected void shiftStraightSegmentRunFromListCore(List<SegmentWithKids> straight, Vector2D shift, SegmentWithKids rootSK) {
    
    ArrayList<String> straightSegs = new ArrayList<String>();
    ArrayList<String> attached = new ArrayList<String>();
    String parentID = null;
     
    int numStr = straight.size();
    if (numStr == 0) {
      throw new IllegalArgumentException();
    }
    int last = numStr - 1;
    SegmentWithKids swk = straight.get(0);
    straightSegs.add(swk.segment.getID());
    parentID = swk.getParentID();
    attached.addAll(swk.getSiblingIDs(rootSK));
    for (int i = 0; i < numStr; i++) {
      List<String> children = swk.getChildrenIDs();
      if (i < last) {
        SegmentWithKids nextSwk = straight.get(i + 1);
        String nextID = nextSwk.segment.getID();
        straightSegs.add(nextID);
        children.remove(nextID);
        swk = nextSwk;
      }
      attached.addAll(children);
    }
    shiftStraightSegmentRun(straightSegs, shift, parentID, attached);
    
    return;
  }  
    
  
  /***************************************************************************
  **
  ** Get the one fixed point for a drop (e.g. the parent segment point)
  */
  
  protected Point2D getFixedDropPoint(LinkSegmentID segID) {
    if (!segID.isForDrop() || segID.isDirect()) {
      throw new IllegalArgumentException();
    }
    
    LinkBusDrop drop;
    if (segID.isForStartDrop()) {
      drop = getRootDrop();
    } else {
      drop = getTargetDrop(segID);
    }
    String parent = drop.getConnectionTag();
    LinkSegment parentSeg = getSegment(parent);
    
    int type = drop.getConnectionSense();
    if (type == LinkBusDrop.CONNECT_TO_END) {
      return ((Point2D)parentSeg.getEnd().clone());
    } else if (type == LinkBusDrop.CONNECT_TO_START) {
      return ((Point2D)parentSeg.getStart().clone());     
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Move the linkage by the given amount.  Start point can be null. 
  */
  
  protected void moveBusLinkCore(LinkSegmentID segID, Point2D strt, double dx, 
                                 double dy, HashSet<SegCookie> cookies) {
   
    boolean forDrop = segID.isForDrop();
    LinkSegment seg = (forDrop) ? null : getSegment(segID.getLinkSegTag());
       
    //
    // If we intersect a bus, we just move the one real point.
    // If we intersect the end of a real segment, we just move that point
    // (and any other equal points in the bus).  Otherwise, we move
    // both points (and matching points):
    //

    if (forDrop) {
      Point2D matching = getFixedDropPoint(segID);
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment tseg = sit.next();
        Point2D endpt = tseg.getEnd();
        SegCookie sc = new SegCookie(tseg.getID(), false);
        if ((endpt != null) && endpt.equals(matching) && !cookies.contains(sc)) {
          tseg.shiftEnd(dx, dy);
          cookies.add(sc);
        }
        sc = new SegCookie(tseg.getID(), true);        
        if (tseg.getStart().equals(matching) && !cookies.contains(sc)) {
          tseg.shiftStart(dx, dy);
          cookies.add(sc);
        }       
      } 
      // FIX ME GETTING NULL PTRS HERE MOVING AFTER BUS SEGMENT SPLIT
      
      // The Link segID include end intersect tags these days already!
      // THIS IS WHERE BT-03-03-11:1 crashes too!    
    } else if (segID.endEndpointIsTagged() || ((strt != null) && (seg.intersectsEnd(strt, INTERSECT_TOL) != null))) {
      if (!segID.endEndpointIsTagged()) {
        System.err.println("Unexpected non-tagged endpoint");
      }
      if (!seg.isDegenerate()) {  // May have segIDs with tagged endpoints on degenerate segment?       
        Point2D matching = (Point2D)seg.getEnd().clone();
        Iterator<LinkSegment> sit = getSegments();
        while (sit.hasNext()) {
          LinkSegment tseg = sit.next();
          Point2D endpt = tseg.getEnd();
          SegCookie sc = new SegCookie(tseg.getID(), false);
          if ((endpt != null) && endpt.equals(matching) && !cookies.contains(sc)) {
            tseg.shiftEnd(dx, dy);
            cookies.add(sc);
          }
          sc = new SegCookie(tseg.getID(), true);
          if (tseg.getStart().equals(matching) && !cookies.contains(sc)) {
            tseg.shiftStart(dx, dy); 
            cookies.add(sc);
          }       
        }
      }
      
    // The Link segID include end intersect tags these days already!
    } else if (segID.startEndpointIsTagged() || ((strt != null) && (seg.intersectsStart(strt, INTERSECT_TOL) != null))) {
      if (!segID.startEndpointIsTagged()) {
        System.err.println("Unexpected non-tagged start point");
      }
      Point2D matching = (Point2D)seg.getStart().clone();
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment tseg = sit.next();
        Point2D endpt = tseg.getEnd();
        SegCookie sc = new SegCookie(tseg.getID(), false);
        if ((endpt != null) && endpt.equals(matching) && !cookies.contains(sc)) {
          tseg.shiftEnd(dx, dy);
          cookies.add(sc);
        }
        sc = new SegCookie(tseg.getID(), true);
        if (tseg.getStart().equals(matching) && !cookies.contains(sc)) {
          tseg.shiftStart(dx, dy);
          cookies.add(sc);
        }        
      }
    } else if (seg.getEnd() != null) {  
      Point2D matching1 = (Point2D)seg.getStart().clone();
      Point2D matching2 = (Point2D)seg.getEnd().clone();
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment tseg = sit.next();
        Point2D endpt = tseg.getEnd();
        SegCookie sc = new SegCookie(tseg.getID(), false);
        if (((endpt != null) && !cookies.contains(sc)) && 
            (endpt.equals(matching1) || endpt.equals(matching2))) {
          tseg.shiftEnd(dx, dy);
          cookies.add(sc);
        }
        sc = new SegCookie(tseg.getID(), true);
        if (!cookies.contains(sc) && (tseg.getStart().equals(matching1) ||
                                      tseg.getStart().equals(matching2))) {
          tseg.shiftStart(dx, dy);
          cookies.add(sc);
        }
      }
    } else {  
      Point2D matching1 = (Point2D)seg.getStart().clone();
      Iterator<LinkSegment> sit = getSegments();
      while (sit.hasNext()) {
        LinkSegment tseg = sit.next();
        SegCookie sc = new SegCookie(tseg.getID(), true);
        if (!cookies.contains(sc) && (tseg.getStart().equals(matching1))) {
          tseg.shiftStart(dx, dy);
          cookies.add(sc);
        }      
      }
    }
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Replace colors assigned to components
  */
  
  protected boolean replaceCustomColors(String oldID, String newID) {
    boolean retval = false;
    
    Iterator<LinkSegment> sit = getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      SuggestedDrawStyle sds = ls.getSpecialDrawStyle();
      if (sds != null) {
        String colorTag = sds.getColorName();
        if ((colorTag != null) && colorTag.equals(oldID)) {
          sds.setColorName(newID);
          retval = true;       
        }
      } 
    }
    
    Iterator<LinkBusDrop> bdit = getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop bd = bdit.next();      
      SuggestedDrawStyle sds = bd.getSpecialDropDrawStyle();
      if (sds != null) {
        String colorTag = sds.getColorName();
        if ((colorTag != null) && colorTag.equals(oldID)) {
          sds.setColorName(newID);
          retval = true;       
        }
      }      
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        PerLinkDrawStyle plds = bd.getDrawStyleForLink();
        if (plds != null) {
          SuggestedDrawStyle plsds = plds.getDrawStyle();
          String colorTag = plsds.getColorName();
          if ((colorTag != null) && colorTag.equals(oldID)) {
            plsds.setColorName(newID);
            retval = true;       
          }
        }       
      }      
    }
    return (retval);
  }   
  
  
 /***************************************************************************
  **
  ** To handle the leaf case, we need to dump the terminal link, not change
  ** the parent at all, and reparent the drop.
  */
  
  protected void handleLeafCase(LinkSegment seg, boolean parentIsDegenerate) {
    
    //
    // Reparent drop.  If parent is degenerate, they need to go to the start.
    //

    String segID = seg.getID();
    int sense = (parentIsDegenerate) ? LinkBusDrop.CONNECT_TO_START : LinkBusDrop.CONNECT_TO_END;
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String conTag = drop.getConnectionTag();
      if (conTag.equals(segID)) {
        drop.setConnectionTag(seg.getParent());
        drop.setConnectionSense(sense);
      }
    }    
    
    labels_.removeLabel(segID);
    segments_.remove(segID);
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if the given segment has siblings, either links or bus drops
  */
  
  protected boolean hasSiblings(LinkSegment seg) {
    //
    // Assumption: there is only one root, which
    // has no siblings.
    //
    String parentID = seg.getParent();
    if (parentID == null) {
      return (false);
    }
    String segID = seg.getID();
    //
    // Check segments
    //
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment nextSeg = sit.next();
      if (seg == nextSeg) {
        continue;
      }

      String nextParent = nextSeg.getParent();
      if (nextParent == null) {
        continue;
      }
      if (nextParent.equals(parentID)) {
        return (true);
      }
    }
    //
    // Check drops.  Drops from the end of our parent, or
    // the start of us, count as a sibling.
    //
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String conTag = drop.getConnectionTag();
      if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_END) &&
          (conTag.equals(parentID))) {
        return (true);
      }
      if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) &&
          (conTag.equals(segID))) {
        return (true);
      }
    }
    return (false);  
  }         

  /***************************************************************************
  **
  ** Answer if the given segment has multiple children, either links or bus drops
  */
  
  protected boolean hasMultipleChildren(LinkSegment seg) {
    String segID = seg.getID();
    String childID = null;
    int childCount = 0;
    //
    // Check segments
    //
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment nextSeg = sit.next();
      if (seg == nextSeg) {
        continue;
      }
      String nextParent = nextSeg.getParent();
      if (nextParent == null) {
        continue;
      }
      if (nextParent.equals(segID)) {
        if (childID != null) {
          return (true);
        }
        childID = nextSeg.getID();
        childCount++;
      }
    }
    
    //
    // Check drops.  Drops from our end, or from
    // the start of our one child (if it exists), 
    // count as a sibling.
    //
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String conTag = drop.getConnectionTag();
      if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_END) &&
          (conTag.equals(segID))) {
        childCount++;
        if (childCount > 1) {
          return (true);
        }
      }
      if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) &&
          (childID != null) && (conTag.equals(childID))) {
        return (true);
      }
      // Fix for BT-05-24-05:1 - need to detect a drop off the start
      // of degenerate root as a child
      if ((drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) &&
          seg.isDegenerate() && (conTag.equals(segID))) {
        childCount++;
        if (childCount > 1) {
          return (true);
        }            
      } 
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Reparent the bus and link children to the parent, and eliminate this
  ** segment while moving the endpoint of the parent.
  */
  
  protected void reparentChildrenAndDelete(LinkSegment seg, boolean parentIsDegenerate, InvertedLinkProps ilp) {
    String parentID = seg.getParent();
    String segID = seg.getID();
    if (parentID == null) {
      throw new IllegalArgumentException();
    }
    LinkSegment parentSeg = segments_.get(parentID);
    LinkSegment replacement = (parentIsDegenerate) ? 
      new LinkSegment(parentSeg, seg.getEnd()) : new LinkSegment(parentSeg, seg);
    Point2D newStart = (replacement.isDegenerate()) ? replacement.getStart() : replacement.getEnd();
      
    //
    // Reassign all children to the parent.
    //
    
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment nextSeg = sit.next();
      if (seg == nextSeg) {
        continue;
      }
      String nextParent = nextSeg.getParent();
      if (nextParent == null) {
        continue;
      }
      if (nextParent.equals(segID)) {
        nextSeg.setParent(parentID);
        if (ilp != null) {
          String nsid = nextSeg.getID();
          ilp.reparentSegment(nsid, segID, parentID);
          ilp.setGeometry(LinkSegmentID.buildIDForSegment(nsid), newStart);
        }
      }
    }
    
    //
    // Reparent drops.  If parent is degenerate, they need to go to the start.
    //
    
    int sense = (parentIsDegenerate) ? LinkBusDrop.CONNECT_TO_START : LinkBusDrop.CONNECT_TO_END;
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        continue;
      }
      String conTag = drop.getConnectionTag();
      if (conTag.equals(segID)) {
        drop.setConnectionTag(seg.getParent());
        drop.setConnectionSense(sense);
        if (ilp != null) {
          String linkID = drop.getTargetRef();
          ilp.reparentDrop(linkID, segID, parentID);
          ilp.setGeometry(LinkSegmentID.buildIDForEndDrop(linkID), newStart);
        }
      }
    }    
    
    labels_.removeLabel(segID);
    segments_.remove(segID);
    segments_.put(parentID, replacement);
    if (ilp != null) {
      ilp.removeSegment(segID);
      ilp.setGeometry(LinkSegmentID.buildIDForSegment(parentID), replacement);
    }
    return;
  } 
    
  
  /***************************************************************************
  **
  ** Get the linkages providing the displayed label
  */
  
  protected Set<String> getLabelLinkages(GenomeSource gSrc, Genome genome, OverlayStateOracle oso) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<LinkBusDrop> dit = getDrops();   
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String busID = drop.getTargetRef();  // may be null
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        if (!linkIsInModel(gSrc, genome, oso, busID)) {
          continue;
        }
        String newLabel = getLinkLabel(genome, busID);
        if ((newLabel != null) && (!newLabel.trim().equals(""))) {
          retval.add(busID);
        }
      }
    }
    return (retval);
  }   
     
  
  /***************************************************************************
  **
  ** Check for segment and endpoint intersection, using two-pass approach.
  ** Two pass is intended to handle trees with varying thicknesses above the
  ** base intersection tolerance.  First pass checks at maximum possible
  ** thickness and second pass refines to actual thickness. 
  */
  
  protected DistancedLinkSegID twoPassCheckSegmentIntersect(DataAccessContext icx, LinkSegment toCheck, Point2D pt, 
                                                            int type, String idTag, double intersectTol, double upperTol, 
                                                            boolean twoPass, boolean endsOnly) {    
    boolean needSecond = false;
    double checkDist = intersectTol;
    if (twoPass) {
      DistancedLinkSegID dlLsid = checkSegmentIntersect(toCheck, pt, type, idTag, upperTol, endsOnly);
      if (dlLsid != null) {
        int segThick = DrawTree.getSegThick(icx, (DrawTreeModelDataSource)this.myRenderer_, dlLsid.segID, this); // kinda expensive!
        double segTol = (double)segThick / 2.0;
        if (segTol < intersectTol) {
          segTol = intersectTol;
        }
        if (segTol < upperTol) {  // refine it down to the actual thickness
          checkDist = segTol;
          needSecond = true;
        } else {
          return (dlLsid);  // Don't bother; our first answer is good enough
        }
      }
    }

    if (!twoPass || needSecond) {
      return (checkSegmentIntersect(toCheck, pt, type, idTag, checkDist, endsOnly));      
    }
    
    return (null);    
  }  
 
  /***************************************************************************
  **
  ** Check for segment and endpoint intersection:
  */
  
  protected DistancedLinkSegID checkSegmentIntersect(LinkSegment toCheck, Point2D pt, 
                                                     int type, String idTag, 
                                                     double intersectTol, boolean endsOnly) {
    LinkSegmentID retval = null;
    Double haveIntersect = (endsOnly) ? null : toCheck.intersects(pt, intersectTol);
    Double haveStart = toCheck.intersectsStart(pt, intersectTol); 
    Double haveEnd = toCheck.intersectsEnd(pt, intersectTol);
    if ((!endsOnly && (haveIntersect != null)) || (haveEnd != null) || (haveStart != null)) {
      retval = LinkSegmentID.buildIDForType(idTag, type);     
    } else {
      return (null);
    }
    
    Double bestPt = null;
    if ((haveEnd != null) || (haveStart != null)) {
      if (haveStart == null) {
        bestPt = haveEnd;
        retval.tagIDWithEndpoint(LinkSegmentID.END);
      } else if (haveEnd == null) {
        bestPt = haveStart;
        retval.tagIDWithEndpoint(LinkSegmentID.START);
      } else {
        String tagIt = (haveStart.doubleValue() < haveEnd.doubleValue()) ? LinkSegmentID.START : LinkSegmentID.END;
        bestPt = (haveStart.doubleValue() < haveEnd.doubleValue()) ? haveStart : haveEnd;
        retval.tagIDWithEndpoint(tagIt);
      }
    }
    // Either bestPt or haveIntersect are non-null, or both.
    double useDist;
    if (bestPt == null) {
      useDist = haveIntersect.doubleValue();
    } else if (haveIntersect == null) { // i.e. off the end...
      useDist = bestPt.doubleValue();
    } else {
      useDist = Math.min(haveIntersect.doubleValue(), bestPt.doubleValue());
    }
    
    return ((retval == null) ? null : new DistancedLinkSegID(retval, useDist));  
  }
  
  /***************************************************************************
  **
  ** Check for segment intersection in a box:
  */
  
  protected LinkSegmentID checkForSegmentInBox(LinkSegment toCheck, Rectangle2D testRect, 
                                             int type, String idTag) {
    return (checkForSegmentInShape(toCheck, testRect, type, idTag));
  }
  
  /***************************************************************************
  **
  ** Check for segment intersection in a shape.  Note for non-convex simple
  ** shapes, this is just checking the two endpoints:
  */
  
  protected LinkSegmentID checkForSegmentInShape(LinkSegment toCheck, Shape testShape, 
                                               int type, String idTag) {
    if (!toCheck.insideShape(testShape)) {
      return (null);
    }
    return (LinkSegmentID.buildIDForType(idTag, type));
  }    
     
  /***************************************************************************
  **
  ** Support for compression/expansion
  **
  */
  
  protected static void shiftSupport(Point2D loc, SortedSet<Integer> rows, 
                                     SortedSet<Integer> cols, Rectangle bounds, double sign, int mult) {
    if (bounds != null) {                            
      double minX = (double)bounds.x;
      double maxX = (double)(bounds.x + bounds.width);
      double minY = (double)bounds.y;
      double maxY = (double)(bounds.y + bounds.height);    
      double locY = loc.getY();
      double locX = loc.getX();
      if ((locY < minY) || (locY > maxY) || (locX < minX) || (locX > maxX)) {
        return;
      }                    
    }
    double dVal = UiUtil.GRID_SIZE * (double)mult;
    
    Iterator<Integer> rit = rows.iterator();
    double rowDelta = 0.0;
    while (rit.hasNext()) {
      Integer row = rit.next();
      if ((row.intValue() * UiUtil.GRID_SIZE) < loc.getY()) {
        rowDelta += dVal;
      }
    }
    double colDelta = 0.0;
    Iterator<Integer> cit = cols.iterator();
    while (cit.hasNext()) {
      Integer col = cit.next();
      if ((col.intValue() * UiUtil.GRID_SIZE) < loc.getX()) {
        colDelta += dVal;
      }
    }
    double newX = loc.getX() + (colDelta * sign);
    double newY = loc.getY() + (rowDelta * sign);
    loc.setLocation(newX, newY);
    return;
  }
  
 /***************************************************************************
  **
  ** More support for compression/expansion
  **
  */
  
  protected void segmentShiftSupport(LinkSegment seg, SortedSet<Integer> rows, 
                                     SortedSet<Integer> cols, Rectangle bounds, double sign, int mult) {
    Point2D lsStart = seg.getStart();
    Point2D lsNewStart = (Point2D)lsStart.clone();
    shiftSupport(lsNewStart, rows, cols, bounds, sign, mult);
    seg.shiftStart(lsNewStart.getX() - lsStart.getX(), lsNewStart.getY() - lsStart.getY());
    Point2D lsEnd = seg.getEnd();
    if (lsEnd == null) {
      return;
    }
    Point2D lsNewEnd = (Point2D)lsEnd.clone();
    shiftSupport(lsNewEnd, rows, cols, bounds, sign, mult);
    seg.shiftEnd(lsNewEnd.getX() - lsEnd.getX(), lsNewEnd.getY() - lsEnd.getY()); 
    return;
  }
  
  /***************************************************************************
  **
  ** Support for copying
  **
  */
  
  protected void copySupport(LinkProperties other) {
    this.textPos_ = (other.textPos_ != null) ? (Point2D)other.textPos_.clone() : null;
    this.textDir_ = other.textDir_;
    this.textDirTag_ = other.textDirTag_;
    this.myRenderer_ = other.myRenderer_;
    this.drawStyle_ = other.drawStyle_.clone();
    this.srcTag_ = other.srcTag_;
    
    this.segments_ = new HashMap<String, LinkSegment>();
    Iterator<String> sit = other.segments_.keySet().iterator();
    while (sit.hasNext()) {
      String nextKey = sit.next();
      LinkSegment seg = other.segments_.get(nextKey);
      this.segments_.put(nextKey, seg.clone());
    }
    
    this.drops_ = new ArrayList<LinkBusDrop>();
    Iterator<LinkBusDrop> dit = other.drops_.iterator();
    while (dit.hasNext()) {
      LinkBusDrop nextDrop = dit.next();
      this.drops_.add(nextDrop.clone());
    }
    this.labels_ = new UniqueLabeller(other.labels_);    
 
    return;
  }
  
  /***************************************************************************
  **
  ** Get the segment ID to associate with a label
  */
  
  protected LinkSegmentID getSegmentIDForLabel(DataAccessContext icx, OverlayStateOracle oso) {

    //
    // Gotta be DBGenome or a root instance:
    //
    
    Genome genome = icx.getGenome();
    
    if (genome instanceof GenomeInstance) {
      if (((GenomeInstance)genome).getVfgParent() != null) {
        throw new IllegalArgumentException();
      }
    }
   
    //
    // If there is no label, there is no segment!
    //
    
    Set<String> labelLinks = getLabelLinkages(icx.getGenomeSource(), genome, oso);
    if (labelLinks.isEmpty()) {
      return (null);
    }
    
    if (textPos_ == null) {
      return (null);
    }

    //
    // Handle the direct case:
    //
    
    if (isDirect()) {
      return (LinkSegmentID.buildIDForType(getTargetDrop().getTargetRef(), LinkSegmentID.DIRECT_LINK));
    } 
    
    //
    // Find the closest link segment.  Do drops first:
    //
   
    double minDist = Double.POSITIVE_INFINITY;
    LinkSegmentID minSegID = null;
    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String dropRef = drop.getTargetRef();
      LinkSegment dropSeg;
      int dropType;
      if (dropRef == null) {
        dropSeg = getSourceDropSegment(icx, true);
        dropType = LinkSegmentID.START_DROP;
      } else {
        dropSeg = getTargetDropSegment(drop, icx);
        dropType = LinkSegmentID.END_DROP;
      }
      
      double dist = dropSeg.getDistance(textPos_);
      if (dist < minDist) {
        minSegID = LinkSegmentID.buildIDForType(dropRef, dropType);
        minDist = dist;
      } 
    }
    
    Iterator<String> sit = segments_.keySet().iterator();
    while (sit.hasNext()) {
      String segKey = sit.next();
      LinkSegment seg = segments_.get(segKey);
      if (!seg.isDegenerate()) {
        double dist = seg.getDistance(textPos_);
        if (dist < minDist) {
          minSegID = LinkSegmentID.buildIDForType(segKey, LinkSegmentID.SEGMENT);
          minDist = dist;
        }
      }
    }
        
    return (minSegID);
  } 
  
  /***************************************************************************
  **
  ** When inserting a new node into a tree segment, we want a new link properties
  ** matching the old below the break, and a new drop going into the new node (if newLinkToNodeID != null)
  */
  
  protected LinkProperties newTreeBelowSegment(LinkSegment oldSeg, String newSourceID, 
                                               String newLinkToNodeID, Map<String, String> linkMap, Object extraInfo) { 

    LinkProperties retval = this.clone();
    retval.srcTag_ = newSourceID;

    //
    // FIX ME! Figure out how to handle label loss:
    // this.textPos_ = (other.textPos_ == null) ? null : (Point2D)other.textPos_.clone();
    // this.textDir_ = other.textDir_;
    // this.textDirTag_ = other.textDirTag_;        

    //
    // Get rid of all the other links not going through the segment:
    //
    
    List<String> allLinks = getLinkageList();
    Set<String> keepLinks = linkMap.keySet();
    int linkNum = allLinks.size();
    for (int i = 0; i < linkNum; i++) {
      String linkID = allLinks.get(i);
      if (!keepLinks.contains(linkID)) {
        retval.removeLinkSupport(linkID);
      }
    }
      
    //
    // Get rid of all the segments above us.  Make us into the
    // root segment:
    //
    
    String oldSegID = oldSeg.getID();  
    LinkSegment matchingToOld = retval.getSegment(oldSegID);
    List<LinkSegment> toRoot = retval.getSegmentsToRoot(matchingToOld);
    int tRNum = toRoot.size();
    for (int i = 0; i < tRNum; i++) {
      LinkSegment segToRoot = toRoot.get(i);
      if (!segToRoot.getID().equals(oldSegID)) {
        retval.labels_.removeLabel(segToRoot.getID());
        retval.segments_.remove(segToRoot.getID());
      }
    }
    matchingToOld.setParent(null);
    matchingToOld.setStart(matchingToOld.getEnd());
    matchingToOld.setEnd(null);
                   
    //
    // Change the starting bus drop to be to the new source.  If the
    // chopped segment was referenced by any drops, they need to
    // do a connect to start now.
    //
    
    Iterator<LinkBusDrop> dit = retval.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        drop.setConnectionSense(LinkBusDrop.CONNECT_TO_START);
        drop.setConnectionTag(oldSegID);
        drop.setDrawStyleForDrop(matchingToOld.getSpecialDrawStyle());
      } else if (drop.getConnectionTag().equals(oldSegID)) {
        drop.setConnectionSense(LinkBusDrop.CONNECT_TO_START);
      }
    }
   
    //
    // Change the link IDs to match the new links:
    //
    
    Iterator<LinkBusDrop> drit = retval.getDrops();
    while (drit.hasNext()) {
      LinkBusDrop drop = drit.next();
      String targRef = drop.getTargetRef();
      if (targRef != null) {
        String mappedRef = linkMap.get(targRef);
        drop.setTargetRef(mappedRef);
      }
    }
    
    //
    // On THIS bus (not the return value) add a new drop to the new node
    //
    
    if (newLinkToNodeID != null) {
      String myRootSegID = getRootSegment().getID();
      String parentSeg = oldSeg.getParent();
      int sense = (myRootSegID.equals(parentSeg)) ? LinkBusDrop.CONNECT_TO_START : LinkBusDrop.CONNECT_TO_END;    
      SuggestedDrawStyle segStyle = oldSeg.getSpecialDrawStyle();
      LinkBusDrop newDrop = generateBusDrop(newLinkToNodeID, parentSeg, 
                                            LinkBusDrop.END_DROP, sense, extraInfo); 
      newDrop.setDrawStyleForDrop(segStyle);
      addDrop(newDrop);
    }
    
    retval.moveSource(extraInfo);
    
         
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  protected static void buildFromXMLSupport(String elemName, Attributes attrs, 
                                            LinkProperties emptyProp, 
                                            FactoryWhiteboard board, String xmlTag) throws IOException {  
    String src = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "src", true);
    String color = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "color", false);
    String line = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "line", false);
    String txtX = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "labelX", false);
    String txtY = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "labelY", false);
    String txtDir = AttributeExtractor.extractAttribute(elemName, attrs, xmlTag, "labelDir", false);
    emptyProp.loadFromXML(color, src, line, txtX, txtY, txtDir);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get tag directions
  **
  */
  
  public static Set<String> labelDirections() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(LEFT_STR_);
    retval.add(RIGHT_STR_);
    retval.add(UP_STR_); 
    retval.add(DOWN_STR_);     
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** map direction values
  */

  public static String mapDirectionToTag(int val) {
    switch (val) {
      case LEFT:
        return (LEFT_STR_);
      case RIGHT:
        return (RIGHT_STR_);
      case UP:
        return (UP_STR_);
      case DOWN:
        return (DOWN_STR_);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** map direction strings to values
  */

  public static int mapFromDirectionTag(String tag) {
    if (tag.equals(LEFT_STR_)) {
      return (LEFT);
    } else if (tag.equals(RIGHT_STR_)) {
      return (RIGHT);
    } else if (tag.equals(UP_STR_)) {
      return (UP);
    } else if (tag.equals(DOWN_STR_)) {
      return (DOWN);
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map legacy style tags to styles
  */

  public static int mapFromStyleTag(String lineStyle) {
    lineStyle = lineStyle.trim();
    if (lineStyle.equalsIgnoreCase("solid")) {
      return (SOLID);
    } else if (lineStyle.equalsIgnoreCase("thin")) {
      return (THIN);
    } else if (lineStyle.equalsIgnoreCase("dash")) {
      return (DASH);
    } else if (lineStyle.equalsIgnoreCase("thindash")) {
      return (THINDASH);
    } else if (lineStyle.equalsIgnoreCase("")) {
      return (SOLID);
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Used to track segments
  */
  
  private class SegCookie {
    String segID;
    boolean isStart;
    
    SegCookie(String segID, boolean isStart) {
      this.segID = segID;
      this.isStart = isStart;
    }
    
    public int hashCode() {
      int startInc = (isStart) ? 1 : 0;
      return ((segID == null) ? startInc : segID.hashCode() + startInc);
    }  
  
    public String toString() {
      return ("Seg cookie: " + segID + " isStart = " + isStart);
    } 
 
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof SegCookie)) {
        return (false);
      }
      SegCookie otherSC = (SegCookie)other;
      if (this.isStart != otherSC.isStart) {
        return (false);
      }
      if (this.segID == null) {
        return (otherSC.segID == null);
      } else {
        return (this.segID.equals(otherSC.segID));
      }
    }
  }

 /***************************************************************************
  **
  ** Directive to glue a subtree at a given segment
  ** 
  */
  
  public static class GlueJob {
    
    public LinkProperties newProps; 
    public LinkSegmentID taggedWhip;  

    public GlueJob(LinkProperties newProps, LinkSegmentID taggedWhip) {
      this.newProps = newProps;
      this.taggedWhip = taggedWhip;
    }
  }  
    
  /***************************************************************************
  **
  ** Holds answer about closest point
  */
  
  public static class ClosestAnswer {    
    double distance;
    String segID;
    Point2D point;
  }
  
  /***************************************************************************
  **
  ** Holds answer about closest point
  */
  
  public static class DistancedLinkSegID {    
    public LinkSegmentID segID;
    public double distance;
    
    DistancedLinkSegID(LinkSegmentID segID, double distance) {
      this.segID = segID;
      this.distance = distance;
    }
  }
 
  /***************************************************************************
  **
  ** Used to resolve connections
  */
  
  private class SegResolve {
    LinkSegment seg;
    int connectEnd;
    Point2D ourConnectPos;  
  }
  
  /***************************************************************************
  **
  ** Represents a pending chop request
  */
  
  private class ChopRequest {    

    Point2D splitPt;
    String segmentID;
    
    ChopRequest(Point2D splitPt, String segmentID) {
      this.splitPt = splitPt;
      this.segmentID = segmentID;
    }
  }   
  
  /***************************************************************************
  **
  ** Represents a repair request
  */
  
  protected class RepairRequest {    

    int relation;
    LinkSegmentID myID;
    LinkSegmentID otherID;
    LinkSegment.OverlapResult ovrRes;
    boolean isUseless;
   
    RepairRequest(LinkSegmentID myID, LinkSegmentID otherID, 
                  LinkSegment.OverlapResult ovrRes) {     
      relation = segRelationship(myID, otherID);
      this.myID = myID;
      this.otherID = otherID;
      this.ovrRes = ovrRes;
      this.isUseless = false;
    }
    
    RepairRequest(LinkSegmentID myID) {     
      this.myID = myID;
      this.isUseless = true;
    }   
  } 
  
  /***************************************************************************
  **
  ** Info needed to record shifts of fragments of links
  **
  */  
  
  public static class LinkFragmentShifts {    
    HashMap<String, LinkFragmentData> segmentStarts;
    HashMap<String, LinkFragmentData> segmentEnds;
    HashMap<String, LinkFragmentData> drops;
    LinkFragmentData doSource;
  
    LinkFragmentShifts() {
      segmentStarts = new HashMap<String, LinkFragmentData>();
      segmentEnds = new HashMap<String, LinkFragmentData>();
      drops = new HashMap<String, LinkFragmentData>();
    }
    
    public String toString() {
      return ("LinkFragmentShifts " + segmentStarts + " " + segmentEnds + " " + drops + " " + doSource);
    }   
  } 
  
  /***************************************************************************
  **
  ** Info needed to record expansions or compressions of fragments of links
  */  
  
  public static class LinkFragmentData {    
    Point2D location;
    Vector2D expComp;
    
    public LinkFragmentData(Point2D location) {
      this.location = (Point2D)location.clone();
    }
       
    public void expandContract(SortedSet<Integer> rows, SortedSet<Integer> cols, Rectangle bounds, double sign, int mult) {
      Point2D lsNewStart = (Point2D)location.clone();
      shiftSupport(lsNewStart, rows, cols, bounds, sign, mult);
      expComp = new Vector2D(lsNewStart.getX() - location.getX(), lsNewStart.getY() - location.getY());
      return;
    } 
  }
  
  /***************************************************************************
  **
  ** Info for deciding how much we can shift a corner during layout recovery
  */
  
  public static class CornerDoF implements Cloneable {
    public boolean inboundIsCanonical;
    public LinkSegmentID backupPoint;
    public LinkSegmentID runPoint;
    public LinkSegmentID normPoint;
    public LinkSegmentID antiNormPoint;
    public Vector2D backupVector;
    public Vector2D runVector;
    public Vector2D normVector;
    public Vector2D antiNormVector;
    public LinkSegmentID myID;
    public boolean runDrop;
    public boolean backDrop;
    public boolean normDrop;
    public boolean antiDrop;
    public boolean backupPointIsInBackupDir;
    
    public CornerDoF(LinkSegmentID myID) {
      this.myID = myID;      
    }
       
    public CornerDoF clone() {
      try {
        CornerDoF retval = (CornerDoF)super.clone();
        retval.myID = (this.myID == null) ? null : (LinkSegmentID)this.myID.clone();
        retval.runPoint = (this.runPoint == null) ? null : (LinkSegmentID)this.runPoint.clone();
        retval.backupPoint = (this.backupPoint == null) ? null : (LinkSegmentID)this.backupPoint.clone();        
        retval.normPoint = (this.normPoint == null) ? null : (LinkSegmentID)this.normPoint.clone();
        retval.antiNormPoint = (this.antiNormPoint == null) ? null : (LinkSegmentID)this.antiNormPoint.clone();
        retval.runVector = (this.runVector == null) ? null : (Vector2D)this.runVector.clone();        
        retval.backupVector = (this.backupVector == null) ? null : (Vector2D)this.backupVector.clone();
        retval.normVector = (this.normVector == null) ? null : (Vector2D)this.normVector.clone();
        retval.antiNormVector = (this.antiNormVector == null) ? null : (Vector2D)this.antiNormVector.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    public String toString() {
      return ("CornerDoF: myID [" + myID + "] normPoint [" + normPoint + "] antiNormPoint = [" + antiNormPoint
              + "] runPoint = [" + runPoint  + "] backupPoint = [" + backupPoint  
              + "] normVector = [" + normVector  + "] antiNormVector = [" + antiNormVector
              + "] runVector = [" + runVector  + "] backupVector = [" + backupVector
              + "] inboundIsCanonical = " + inboundIsCanonical 
              + " runDrop = " + runDrop + " normDrop = " + normDrop + " antiDrop = " + antiDrop);
    } 
  }  
}
