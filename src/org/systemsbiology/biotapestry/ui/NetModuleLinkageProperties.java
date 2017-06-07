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

package org.systemsbiology.biotapestry.ui;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import java.util.TreeMap;
import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleLinkageFree;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Contains the information needed to layout a tree of network module linkages
*/

public class NetModuleLinkageProperties extends LinkProperties implements Cloneable {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;  // Module links have global IDs!
  private String ovrKey_;  // Knows overlay too!
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Simple constructor
  */

  public NetModuleLinkageProperties(String id, String ovrKey) {
    super(new NetModuleLinkageFree());
    this.id_ = id;
    this.ovrKey_ = ovrKey;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */  

  public NetModuleLinkageProperties(NetModuleLinkageProperties other) {
    super(other);
    this.id_ = other.id_;
    this.ovrKey_ = other.ovrKey_;
  }
   
  /***************************************************************************
  **
  ** For UI-based drawing
  */   
  
  public NetModuleLinkageProperties(String myID, String ovrKey, String srcID, String linkID, 
                                    Point2D start, Point2D end, Vector2D startOff, Vector2D endOff) {
    super(new NetModuleLinkageFree());
    this.srcTag_ = srcID;
    id_ = myID;
    ovrKey_ = ovrKey;
    //
    // Add the first two bus drops to get the ball rolling:
    //
    
    NetModuleBusDrop bd = new NetModuleBusDrop((Point2D)start.clone(), startOff, null, null, 
                                                NetModuleBusDrop.START_DROP, 
                                                NetModuleBusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
    
    bd = new NetModuleBusDrop((Point2D)end.clone(), endOff,
                              linkID, null, NetModuleBusDrop.END_DROP, 
                              NetModuleBusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
  }

  /***************************************************************************
  **
  ** Copy Constructor with ID change
  */  

  public NetModuleLinkageProperties(NetModuleLinkageProperties other, String newID, 
                                    String newOvrKey, Map<String, String> modIDMap, Map<String, String> modLinkIDMap) {
    super(other);
    this.id_ = newID;
    this.ovrKey_ = newOvrKey;
    this.srcTag_ = (modIDMap == null) ? null : modIDMap.get(this.srcTag_);
    if (this.srcTag_ == null) {
      throw new IllegalStateException();
    }
    
    this.drops_.clear();
    Iterator<LinkBusDrop> dit = other.drops_.iterator();
    while (dit.hasNext()) {
      NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
      this.drops_.add(new NetModuleBusDrop(nextDrop, modLinkIDMap));
    }    
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return a new link properties that is a direct link to the given target
  */   
  
  public LinkProperties deriveDirectLink(String newSource, LinkBusDrop keepDrop, Object extraInfo) {
    NetModuleLinkageProperties retval = this.clone();
    retval.srcTag_ = newSource;
    retval.clearOutAllSegments();
    retval.drops_.clear();
    
    DirectLinkExtraInfo dlei = (DirectLinkExtraInfo)extraInfo;
    retval.id_ = dlei.treeID;
    
    //
    // Add the two bus drops
    //
    
    NetModuleBusDrop bd = new NetModuleBusDrop(dlei.startPt, dlei.toStartSide, null, null, NetModuleBusDrop.START_DROP, NetModuleBusDrop.NO_SEGMENT_CONNECTION);
    retval.drops_.add(bd);
    
    NetModuleBusDrop knmbd = (NetModuleBusDrop)keepDrop;
    
    bd = new NetModuleBusDrop(knmbd.getEnd(0.0), knmbd.getOffset(), keepDrop.getTargetRef(), null, NetModuleBusDrop.END_DROP, NetModuleBusDrop.NO_SEGMENT_CONNECTION);
    retval.drops_.add(bd);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  @Override
  public NetModuleLinkageProperties clone() { 
    NetModuleLinkageProperties retval = (NetModuleLinkageProperties)super.clone();
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Get my ID
  */ 
  
  public String getID() {
    return (id_);
  } 
  
  /***************************************************************************
  **
  ** Get my Overlay ID
  */  
  
  public String getOverlayID() {
    return (ovrKey_);
  }   
  
  /***************************************************************************
  **
  ** Write the module property to XML
  **
  */  
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<nModLinkProp id=\"");
    out.print(id_);
    out.print("\" ");  
    writeXMLSupport(out, ind);
    ind.down().indent();
    out.println("</nModLinkProp>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get a fake "link segment" for a direct path that goes to the pads.
  */  
  
  public LinkSegment getDirectLinkPath(DataAccessContext icx) {
    
    NetModuleBusDrop targDrop = (NetModuleBusDrop)getTargetDrop();    
    NetModuleBusDrop srcDrop = (NetModuleBusDrop)getRootDrop();
    
    Point2D sLoc  = srcDrop.getEnd((UiUtil.GRID_SIZE / 2.0) + 1.0); 
    Point2D eLoc =  targDrop.getEnd((UiUtil.GRID_SIZE / 2.0) + 1.0);    
    LinkSegment retval = new LinkSegment(sLoc, eLoc);
    return (retval);      
  }  
  
 /***************************************************************************
  **
  ** Get the start drop segment for this link tree
  */ 
  
  protected LinkSegment getSourceDropSegment(DataAccessContext icx, boolean forceToGrid) {

    NetModuleBusDrop drop = (NetModuleBusDrop)getRootDrop();
    // This is where we nudge the source point towards the module side
    Point2D srcLoc = drop.getEnd((UiUtil.GRID_SIZE / 2.0) + 1.0);
    if (forceToGrid) {
      UiUtil.forceToGrid(srcLoc.getX(), srcLoc.getY(), srcLoc, UiUtil.GRID_SIZE);
    }  
    LinkSegment rootSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? rootSeg.getStart() : rootSeg.getEnd();
    return (new LinkSegment(srcLoc, segPt));
  }
  
  /***************************************************************************
  **
  ** Get a target drop segment
  */  
  
  protected LinkSegment getTargetDropSegment(LinkBusDrop drop, DataAccessContext icx) {
    String trg = drop.getTargetRef();
    if (trg == null) {
      throw new IllegalStateException();
    }
    // This is where we nudge the target point towards the module side
    Point2D termLoc = ((NetModuleBusDrop)drop).getEnd((UiUtil.GRID_SIZE / 2.0) + 1.0);
    LinkSegment trgSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? trgSeg.getStart() : trgSeg.getEnd();
    return (new LinkSegment(segPt, termLoc)); 
  }

  /***************************************************************************
  **
  ** Answer if the source drop is non-orthogonal
  */  
  
  protected boolean srcDropIsNonOrtho(DataAccessContext icx) {
    LinkSegment seg = getSegmentGeometryForID(LinkSegmentID.buildIDForStartDrop(), icx, false);
    return (!seg.getRun().isCanonical());
  }

  /***************************************************************************
  **
  ** Answer if the target drop is non-orthogonal
  */  
  
  protected boolean targetDropIsNonOrtho(LinkBusDrop drop, DataAccessContext icx) {
    LinkSegment seg = getSegmentGeometryForID(LinkSegmentID.buildIDForDrop(drop.getTargetRef()), icx, false);
    return (!seg.getRun().isCanonical());
  }
 
  /***************************************************************************
  **
  ** Get custom bus drop
  */    
  
  protected LinkBusDrop generateBusDrop(LinkBusDrop spDrop, String ourConnection, 
                                        int dropType, int connectionEnd) {
    NetModuleBusDrop nmbd = (NetModuleBusDrop)spDrop;
    return (new NetModuleBusDrop(nmbd.getEnd(0.0), nmbd.getOffset(), spDrop.getTargetRef(), ourConnection, dropType, connectionEnd));
  } 
  
 /***************************************************************************
  **
  ** Build custom drop type:
  */  

  protected LinkBusDrop generateBusDrop(String targetRef, String ourConnection, 
                                        int dropType, int connectionEnd, Object extraInfo) {
    DirectLinkExtraInfo dlei = (DirectLinkExtraInfo)extraInfo;
    return (new NetModuleBusDrop(dlei.startPt, dlei.toStartSide, targetRef, ourConnection, dropType, connectionEnd));
  } 

 /***************************************************************************
  **
  ** Get a quick kill thickess:
  */ 
  
  protected ThickInfo maxThickForIntersect(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, double intersectTol) {
    return (maxThickForIntersectSupport(gSrc, genome, oso, true, intersectTol));
  }  
  
  /***************************************************************************
  **
  ** Handle source move operations:
  */  
  
  protected void moveSource(Object extraInfo) {
    DirectLinkExtraInfo dlei = (DirectLinkExtraInfo)extraInfo;
    this.id_ = dlei.treeID; 
    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)dit.next();
      if (drop.getDropType() == NetModuleBusDrop.START_DROP) {
        drop.setEnd((Point2D)dlei.startPt.clone(), dlei.toStartSide);
        break;
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer of we can relocate an internal segment to another parent.  Always
  ** can, since we can move it to another launch pad (as long as there is
  ** space on the module: caller needs to check that!
  */ 
  
  public boolean canRelocateSegmentOnTree(LinkSegmentID moveID) {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if the given link is in the model
  */ 
  
  public boolean linkIsInModel(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, String linkID) {  
    TaggedSet currMods = oso.getCurrentNetModules();
    if ((currMods == null) || (currMods.set == null) || currMods.set.isEmpty()) {
      return (false);
    }
    NetOverlayOwner noo = gSrc.getOverlayOwnerFromGenomeKey(genome.getID());
    NetworkOverlay ovr = noo.getNetworkOverlay(ovrKey_);
    NetModuleLinkage nml = ovr.getLinkage(linkID);
    String src = nml.getSource();
    String trg = nml.getTarget();    
    return (currMods.set.contains(src) && (currMods.set.contains(trg)));
  }
  
  /***************************************************************************
  **
  ** Abstracted source check
  */  
  
  protected void sourceSanityCheck(Genome genome, LinkProperties other) {  
    return;
  }
  
  /***************************************************************************
  **
  ** Get link label.  May be null
  */  
  
  protected String getLinkLabel(Genome genome, String linkID) {
    return (null);  
  }   
 
  /***************************************************************************
  **
  ** Get link target.
  */  
  
  public String getLinkTarget(Genome genome, String linkID) {
    NetworkOverlay ovr = genome.getNetworkOverlay(ovrKey_);
    NetModuleLinkage nml = ovr.getLinkage(linkID);
    return (nml.getTarget());
  } 
  
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  protected void shiftDropEnds(double dx, double dy, Vector2D sideDir) {
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
      Point2D dLoc = nextDrop.getEnd(0.0);
      if (dLoc == null) {
        // Why should this happen??
        continue;
      }
      nextDrop.shiftEnd(dx, dy, sideDir);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Init a grid for fixing non-ortho links
  */  
  
  protected LinkPlacementGrid initGridForOrthoFix(LinkRouter router, DataAccessContext rcx, 
                                                  String overID, BTProgressMonitor monitor) throws AsynchExitRequestException { 
    List<String> doNotRender = getLinkageList();  
    return (router.initGridForModules(rcx, new HashSet<String>(doNotRender), overID, monitor));
  } 
      
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  protected void moveDropEndsPerMap(Map<Point2D, Point2D> mappedPositions, boolean forceToGrid) {
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
      Point2D dLoc = nextDrop.getEnd(0.0);
      if (dLoc == null) {
        // Why should this happen??
        continue;
      }     
      if (forceToGrid) {
        dLoc = (Point2D)dLoc.clone();
        UiUtil.forceToGrid(dLoc, UiUtil.GRID_SIZE);
      }
      Point2D mapped = mappedPositions.get(dLoc);
      if (mapped != null) {
        nextDrop.setEnd((Point2D)mapped.clone(), nextDrop.getOffset());
      } else {
        Point2D minKey = null;
        double minDist = Double.POSITIVE_INFINITY;
        Iterator<Point2D> kit = mappedPositions.keySet().iterator();
        while (kit.hasNext()) {
          Point2D key = kit.next();
          double dist = key.distance(dLoc);
          if (dist < minDist) {
            minKey = key;
            minDist = dist;
          }
        }
        System.err.println("fallback with errval: " + minDist + " " + minKey + " " + dLoc);
        nextDrop.setEnd((Point2D)mappedPositions.get(minKey).clone(), nextDrop.getOffset());
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Assign segments to groups based on bounds
  */  
  
  public Map<String, LinkFragmentShifts> mapSegmentsToBounds(Map<String, Rectangle> boundsForGroups, Map<String, Integer> groupZOrder) { 
    
    TreeMap<Integer, String> sorted = new TreeMap<Integer, String>(Collections.reverseOrder());   
    Iterator<String> kit = groupZOrder.keySet().iterator();
    while (kit.hasNext()) {
      String grpID = kit.next();
      Integer order = groupZOrder.get(grpID);
      sorted.put(order, grpID);
    }
    //
    // If there are competing claims for ownership, we need to base on z-order.
    // Invert the z-order map, assign based on that order, and don't reassign
    
    HashSet<String> assignedSegStarts = new HashSet<String>();
    HashSet<String> assignedSegEnds = new HashSet<String>();
    HashSet<String> assignedDrops = new HashSet<String>();
    boolean assignedSrc = false;
    
    HashMap<String, LinkFragmentShifts> retval = new HashMap<String, LinkFragmentShifts>();
    Iterator<String> bfgit = sorted.values().iterator();
    while (bfgit.hasNext()) {
      String grpID = bfgit.next();
      Rectangle rect = boundsForGroups.get(grpID);
      if (rect == null) {  // Should not happen, but keys not from boundsForGroups map...
        continue;
      }
      LinkFragmentShifts shiftForGroup = new LinkFragmentShifts();
      retval.put(grpID, shiftForGroup);
      
      Iterator<String> sit = segments_.keySet().iterator();
      while (sit.hasNext()) {
        String nextKey = sit.next();
        LinkSegment seg = segments_.get(nextKey);
        if (!assignedSegStarts.contains(nextKey)) {
          if (rect.contains(seg.getStart())) {
            LinkFragmentData lfd = new LinkFragmentData(seg.getStart());
            shiftForGroup.segmentStarts.put(nextKey, lfd);
            assignedSegStarts.add(nextKey);
          }
        }
        if (!assignedSegEnds.contains(nextKey)) {
          if (!seg.isDegenerate() && rect.contains(seg.getEnd())) {
            LinkFragmentData lfd = new LinkFragmentData(seg.getEnd());
            shiftForGroup.segmentEnds.put(nextKey, lfd);
            assignedSegEnds.add(nextKey);
          }
        }
      }
   
      Iterator<LinkBusDrop> dit = drops_.iterator();
      while (dit.hasNext()) {
        NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
        String targRef = nextDrop.getTargetRef();
        if ((targRef == null) && assignedSrc) {
          continue;
        }
        if (rect.contains(nextDrop.getEnd(0.0))) {
          LinkFragmentData lfd = new LinkFragmentData(nextDrop.getEnd(0.0));
          if (targRef == null) {
            shiftForGroup.doSource = lfd;
            assignedSrc = true;
          } else if (!assignedDrops.contains(targRef)) {
            shiftForGroup.drops.put(targRef, lfd);
            assignedDrops.add(targRef);
          }        
        }
      }
    }
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  protected void shiftSelectedDropEnds(double dx, double dy, LinkFragmentShifts fragShifts) {
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
      String targRef = nextDrop.getTargetRef();
      LinkFragmentData lfd = (targRef == null) ? fragShifts.doSource : (LinkFragmentData)fragShifts.drops.get(targRef);
      if (lfd != null) {
        Point2D dLoc = nextDrop.getEnd(0.0);
        if (dLoc == null) {
          // Why should this happen??
          continue;
        }
        nextDrop.shiftEnd(dx, dy, null);
        if (lfd.expComp != null) {
          nextDrop.shiftEnd(lfd.expComp.getX(), lfd.expComp.getY(), null);
        }
      }
    }
    return;
  }
 
 /***************************************************************************
  **
  ** Fill in the rows and columns we use.  We do not calc pad ends; that is
  ** done during module calculations
  */  

  public void needRowsAndCols(SortedSet<Integer> needRows, SortedSet<Integer> needCols) {   
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      Point2D segPt = seg.getStart();
      needCols.add(new Integer((int)segPt.getX() / UiUtil.GRID_SIZE_INT));
      needRows.add(new Integer((int)segPt.getY() / UiUtil.GRID_SIZE_INT));
      segPt = seg.getEnd();
      if (segPt != null) {
        needCols.add(new Integer((int)segPt.getX() / UiUtil.GRID_SIZE_INT));
        needRows.add(new Integer((int)segPt.getY() / UiUtil.GRID_SIZE_INT));
      }
    }    
    return;   
  } 
 
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  protected void shiftDropEndsForExpandCompressOps(SortedSet<Integer> newRows, SortedSet<Integer> newCols, Rectangle bounds, double sign, int mult) {
    Iterator<LinkBusDrop> dit = drops_.iterator();
    while (dit.hasNext()) {
      NetModuleBusDrop nextDrop = (NetModuleBusDrop)dit.next();
      Point2D dLoc = nextDrop.getEnd(0.0);
      if (dLoc == null) {
        continue;
      }
      Point2D newLoc = (Point2D)dLoc.clone();
      shiftSupport(newLoc, newRows, newCols, bounds, sign, mult);
      nextDrop.setEnd(newLoc, null);
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Shift the end location for the given link
  */ 
  
  public void shiftEnd(String linkID, double dx, double dy, Vector2D sideDir) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref != null) {
        if (ref.equals(linkID)) {
          drop.shiftEnd(dx, dy, sideDir);
          return;
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the end location for the given link.  Point sent in must be cloned and gridded
  */ 
  
  public void setTargetEnd(String linkID, Point2D newTarget, Vector2D sideDir) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref != null) {
        if (ref.equals(linkID)) {
          drop.setEnd(newTarget, sideDir);
          return;
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the end location for the given link.
  */ 
  
  public Point2D getTargetEnd(String linkID, double doOffset) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref != null) {
        if (ref.equals(linkID)) {
          return (drop.getEnd(doOffset));
        }
      }
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Set the start location for the tree.  Point sent in must be cloned and gridded
  */  
  
  public void setSourceStart(Point2D newSource, Vector2D sideDir) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        drop.setEnd(newSource, sideDir);
        return;
      }
    }
    return;
  }  
  
 /***************************************************************************
  **
  ** Get the start location for the tree.
  */  
  
  public Point2D getSourceStart(double doOffset) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        return (drop.getEnd(doOffset));
      }
    }
    return (null);
  }  
    
  /***************************************************************************
  **
  ** Shift the start location.
  */  
  
  public void shiftStart(double dx, double dy, Vector2D sideDir) {
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)drops.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        drop.shiftEnd(dx, dy, sideDir);
        return;
      }
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Get some link ID for the 
  */
  
  public String getALinkID(Genome genome) {    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      if (bd.getDropType() != BusDrop.START_DROP) {
        String retval = bd.getTargetRef();   
        return (retval);
      }
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** Get a forced source direction
  */
  
  protected Vector2D getSourceForcedDir(Genome genome, Layout lo) {
    NetModuleBusDrop drop = (NetModuleBusDrop)getDrop(LinkSegmentID.buildIDForStartDrop());
    return (drop.getOffset().scaled(-1.0));
  }
  
  /***************************************************************************
  **
  ** Get a forced target direction
  */
  
  protected Vector2D getTargetForcedDir(Genome genome, Layout lo, LinkSegmentID segID, boolean isDirect) {
    String idDropRef;
    if (segID.isForEndDrop()) {
      idDropRef = segID.getEndDropLinkRef();
    } else if (segID.isDirect()) {
      idDropRef = segID.getDirectLinkRef();
    } else {
      throw new IllegalArgumentException();
    }
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)dit.next();
      String dropRef = drop.getTargetRef();
      int dropType = drop.getDropType();
      if ((dropType == LinkBusDrop.END_DROP) && dropRef.equals(idDropRef)) {
        return (drop.getOffset().clone());
      }
    }
    throw new IllegalArgumentException();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

 /***************************************************************************
  **
  ** Info needed just for net module links
  */ 
      
  public static class DirectLinkExtraInfo {
    public String treeID;
    public Point2D startPt;
    public Vector2D toStartSide;
    
    
    public DirectLinkExtraInfo(String treeID, Point2D startPt, Vector2D toStartSide) { //, Vector2D toEndSide) {
      this.treeID = treeID;
      this.startPt = startPt;
      this.toStartSide = toStartSide;
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetModuleLinkagePropertiesWorker extends AbstractFactoryClient {
    
    public NetModuleLinkagePropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nModLinkProp");
      installWorker(new LinkSegment.LinkSegmentWorker(whiteboard), new MySegmentGlue());
      installWorker(new NetModuleBusDrop.NetModuleBusDropWorker(whiteboard), new MyBusDropGlue());
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());     
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nModLinkProp")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netModLinkProps = buildFromXML(elemName, attrs);
        retval = board.netModLinkProps;
      }
      return (retval);     
    }
    
    private NetModuleLinkageProperties buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "nModLinkProp", "id", true);
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
      NetModuleLinkageProperties newProp = new NetModuleLinkageProperties(id, board.netOvrProps.getReference());
      LinkProperties.buildFromXMLSupport(elemName, attrs, newProp, board, "nModLinkProp");
      return (newProp);
    }
  }
  
  public static class MySegmentGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleLinkageProperties nmProps = board.netModLinkProps;
      LinkSegment seg = board.linkSegment;
      nmProps.addSegment(seg);
      return (null);
    }
  }  
  
  public static class MyBusDropGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleLinkageProperties nmProps = board.netModLinkProps;
      NetOverlayProperties nop = board.netOvrProps;
      NetModuleBusDrop newDrop = board.nmBusDrop;
      Layout lo = board.layout;
      nmProps.addDrop(newDrop);     
      if (newDrop.getDropType() == LinkBusDrop.END_DROP) {
        String targetTag = newDrop.getTargetRef();
        lo.tieNetModuleLinkToProperties(targetTag, nmProps.getID(), nop.getReference());
      }
      return (null);
    }
  }
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleLinkageProperties nmProps = board.netModLinkProps;
      SuggestedDrawStyle suggSty = board.suggSty;
      nmProps.setDrawStyle(suggSty);
      return (null);
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
}
