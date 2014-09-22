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

package org.systemsbiology.biotapestry.ui;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
**
** Extensive operations on LinkProperties have to find children of segments,
** segments parenting drops, and fast drop access. Build these structures
** here. 
*/

public class InvertedLinkProps {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private HashMap<String, Set<String>> segmentKids_;
  private HashMap<String, Set<String>> dropKids_; 
  private HashMap<String, LinkBusDrop> targetDrops_;
  private HashMap<String, Set<String>> descendedDrops_;
  private HashMap<String, Set<String>> ascendingPaths_;
  private LinkBusDrop startDrop_;
  private LinkSegment rootSeg_;
  private Map<LinkSegmentID, LinkSegment> geom_;
  private LinkProperties myProps_;
  private HashMap<String, String> allGone_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
 /***************************************************************************
  ** 
  ** Constructor
  */
        
  public InvertedLinkProps(LinkProperties lp, DataAccessContext icx, boolean noDegen) {
    this(lp);
    geom_ = lp.getAllSegmentGeometries(icx, noDegen);     
  }
   
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public InvertedLinkProps(LinkProperties lp, DataAccessContext icx) {
    this(lp, icx, false);   
  }
  
  /***************************************************************************
  ** 
  ** Constructor when geometry not needed
  */
        
  public InvertedLinkProps(LinkProperties lp) {
    
    myProps_ = lp;
    segmentKids_ = new HashMap<String, Set<String>>();
    dropKids_ = new HashMap<String, Set<String>>(); 
    targetDrops_ = new HashMap<String, LinkBusDrop>();
    descendedDrops_ = new HashMap<String, Set<String>>();
    ascendingPaths_ = new HashMap<String, Set<String>>();
    allGone_ = new HashMap<String, String>();
    
    int numSegs = lp.getSegmentCount();
       
    Iterator<LinkSegment> sit = lp.getSegments();
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      String parent = ls.getParent();
      if (parent == null) {
        rootSeg_ = ls;
      } else {
        Set<String> kids = segmentKids_.get(parent);
        if (kids == null) {
          kids = new HashSet<String>();
          segmentKids_.put(parent, kids);
        }
        kids.add(ls.getID());
      }
    }
    
    Iterator<LinkBusDrop> bdit = lp.getDrops();
    while (bdit.hasNext()) {
      LinkBusDrop bd = bdit.next();
      String tref = bd.getTargetRef();
      // NOTE We are NOT adding the start drop to the list of kids for
      // the root segment!
      if (bd.getDropType() == LinkBusDrop.START_DROP) {
        startDrop_ = bd;
      } else {
        if (numSegs > 0) {
          String segParent = bd.getConnectionTag();
          Set<String> kids = dropKids_.get(segParent);
          if (kids == null) {
            kids = new HashSet<String>();
            dropKids_.put(segParent, kids);
          }
          kids.add(tref);
        }
        targetDrops_.put(tref, bd);
        
        Set<String> aps = ascendingPaths_.get(tref);
        if (aps == null) {
          aps = new HashSet<String>();
          ascendingPaths_.put(tref, aps);
        }        
        List<LinkSegment> s2r = lp.getSegmentsToRoot(bd);
        int numToRoot = s2r.size();
        for (int i = 0; i < numToRoot; i++) {
          LinkSegment next = s2r.get(i);
          String segID = next.getID();
          Set<String> dff = descendedDrops_.get(segID);
          if (dff == null) {
            dff = new HashSet<String>();
            descendedDrops_.put(segID, dff);
          }
          dff.add(tref);
          aps.add(segID);
        }
      }
    }   
    geom_ = null;  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answers if orthogonal
  */
  
 
  public boolean isOrthogonal() {
    Iterator<LinkSegmentID> lit = geom_.keySet().iterator();
    while (lit.hasNext()) {
      LinkSegmentID lsid = lit.next();
      LinkSegment ls = geom_.get(lsid);
      if (lsid.isForSegment() && lsid.getLinkSegTag().equals(rootSeg_.getID())) {
        continue;
      }
      if (!isOrtho(ls.getRun())) {
        return (false);
      }
    }
    return (true);    
  }
  
  /***************************************************************************
  **
  ** Answers if given vector is normalized orthogonal
  */
  
  private boolean isOrtho(Vector2D vector) {
    int vecX = (int)vector.getX();
    int vecY = (int)vector.getY();
    if (((vecX == -1) && (vecY == 0)) ||
        ((vecX == 0) && (vecY == 1)) ||
        ((vecX == 1) && (vecY == 0)) ||
        ((vecX == 0) && (vecY == -1))) {
      return (true);
    } else {
      return (false);
    }
  }  
  
  /***************************************************************************
  **
  ** Get parent seg, with start drop parenting the root segment
  */
  
  public LinkSegmentID getParentSeg(LinkSegmentID lsid) {
    if (lsid.isDirect()) {
      return (null);
    } 
    if (lsid.isForStartDrop()) {
      return (null);
    } else if (lsid.isForSegment()) {
      String segTag = lsid.getLinkSegTag();
      if (segTag.equals(rootSeg_.getID())) {
        return (LinkSegmentID.buildIDForStartDrop());
      }
      return (LinkSegmentID.buildIDForSegment(myProps_.getSegment(lsid).getParent()));
    } else if (lsid.isForEndDrop()) {
      LinkBusDrop ltd = targetDrops_.get(lsid.getLinkSegTag());
      return (LinkSegmentID.buildIDForSegment(ltd.getConnectionTag()));
    }
    throw new IllegalArgumentException();
  }
 
  /***************************************************************************
  **
  ** Get start drop
  */
  
  public LinkBusDrop getStartDrop() {
    return (startDrop_); 
  }
  
  /***************************************************************************
  **
  ** Get root seg (may be null if direct);
  */
  
  public LinkSegment getRootSegment() {
    return (rootSeg_); 
  }

  /***************************************************************************
  **
  ** Segment kids for segment
  */
  
  public Set<String> getSegmentKids(String segID) {
    Set<String> retval = segmentKids_.get(segID);
    return ((retval == null) ? new HashSet<String>() : retval); 
  }
  
  /***************************************************************************
  **
  ** Drop kids for segment
  */
  
  public Set<String> getDropKids(String segID) {
    Set<String> retval = dropKids_.get(segID);
    return ((retval == null) ? new HashSet<String>() : retval); 
  }
  
  /***************************************************************************
  **
  ** Get all the kids. This works all the way from the start drop, i.e. the
  ** start drop kid is the root segment, and the root segment does NOT have
  ** the start drop as a kid.
  */
  
  public Set<LinkSegmentID> getAllKids(LinkSegmentID lsid) {
    if (lsid.isDirect()) {
      return (null);
    } 
    HashSet<LinkSegmentID> retval = new HashSet<LinkSegmentID>();
    if (lsid.isForStartDrop()) {
      retval.add(LinkSegmentID.buildIDForSegment(rootSeg_.getID()));        
    } else if (lsid.isForSegment()) {
      String segTag = lsid.getLinkSegTag();  
      Set<String> sk = segmentKids_.get(segTag);
      if (sk != null) {
        Iterator<String> kit = sk.iterator();
        while (kit.hasNext()) {
          retval.add(LinkSegmentID.buildIDForSegment(kit.next()));
        }
      }
      Set<String> dk = dropKids_.get(segTag);
      if (dk != null) {
        Iterator<String> kit = dk.iterator();
        while (kit.hasNext()) {
          retval.add(LinkSegmentID.buildIDForEndDrop(kit.next()));
        }
      }
    } else if (lsid.isForEndDrop()) {
      // Nothing to return!
    }
    return(retval);
  }
  
  /***************************************************************************
  **
  ** Get drop for link ID
  */
  
  public LinkBusDrop getDropForLink(String linkID) {
    return (targetDrops_.get(linkID));
  }
  
  /***************************************************************************
  **
  ** Reparent a segment
  */
  
  public void reparentSegment(String kidID, String oldParent, String newParent) {
    Set<String> kids = segmentKids_.get(oldParent);
    kids.remove(kidID);
    
    kids = segmentKids_.get(newParent);
    if (kids == null) {
      kids = new HashSet<String>();
      segmentKids_.put(newParent, kids);
    }
    kids.add(kidID);
    return;
  }
  
  /***************************************************************************
  **
  ** Remove a segment
  */
  
  public void removeSegment(String segID) {
    Set<String> kids = segmentKids_.remove(segID);
    if ((kids != null) && !kids.isEmpty()) {
      throw new IllegalStateException();
    }
    kids = dropKids_.remove(segID);
    if ((kids != null) && !kids.isEmpty()) {
      throw new IllegalStateException();
    }
    if (segID.equals(rootSeg_)) {
      rootSeg_ = null;
    }
    return;
  }

  /***************************************************************************
  **
  ** Reparent a drop
  */
  
  public void reparentDrop(String linkID, String oldParent, String newParent) {
    Set<String> kids = dropKids_.get(oldParent);
    kids.remove(linkID);
    
    kids = dropKids_.get(newParent);
    if (kids == null) {
      kids = new HashSet<String>();
      dropKids_.put(newParent, kids);
    }
    kids.add(linkID);
    return;
  }
  
  /***************************************************************************
  **
  ** Remove a drop. WARNING! Currently incomplete in cleanup.
  */
  
  public void removeDrop(String linkID) {
    targetDrops_.remove(linkID);
    Set<String> aps = ascendingPaths_.get(linkID);
    Iterator<String> apsit = aps.iterator();
    while (apsit.hasNext()) {
      String segID = apsit.next();
      Set<String> dff = descendedDrops_.get(segID);
      dff.remove(linkID);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Return the set of links thru the seg
  */
  
  public Set<String> linksThruSeg(String segID) {
    return (descendedDrops_.get(segID));
  } 
  
  /***************************************************************************
  **
  ** Return the set of remaining required segments (those still with drops after
  ** some drops have been removed);
  */
  
  public Set<String> remainingSegments() {
    
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> ddksit = descendedDrops_.keySet().iterator();
    while (ddksit.hasNext()) {
      String segID = ddksit.next();
      Set<String> dff = descendedDrops_.get(segID);
      if (!dff.isEmpty()) {
        retval.add(segID);
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get "Fake" geometry map
  */
  
  public Map<LinkSegmentID, LinkSegment> getGeometryMap() {
    return (geom_);
  } 
  
  /***************************************************************************
  **
  ** Get "Fake" geometry
  */
  
  public LinkSegment getGeometry(LinkSegmentID lsid) {
    return (geom_.get(lsid));
  } 

  /***************************************************************************
  **
  ** Reset "Fake" geometry
  */
  
  public void setGeometry(LinkSegmentID lsid, Point2D newStart) {
    LinkSegment oldGeom = geom_.get(lsid);
 
    LinkSegment newGeom = new LinkSegment(newStart, oldGeom.getEnd());
    geom_.put(lsid, newGeom);
    return;
  }
  
  /***************************************************************************
  **
  ** Reset "Fake" geometry
  */
  
  public void setGeometry(LinkSegmentID lsid, LinkSegment newParentGeom) {
    LinkSegment oldGeom = geom_.get(lsid);
    Point2D newStart = (newParentGeom.isDegenerate()) ? newParentGeom.getStart() : newParentGeom.getEnd();
    LinkSegment newGeom = new LinkSegment(newStart, oldGeom.getEnd());
    geom_.put(lsid, newGeom);
    return;
  } 
  
  /***************************************************************************
  **
  ** Register removal so we can track stale IDs to new IDs
  */
  
  public void registerRemoval(String oldSeg, String newSeg) {
    allGone_.put(oldSeg, newSeg);
    return;
  } 
  
  /***************************************************************************
  **
  ** Convert stale IDs to new IDs
  */
  
  public String convertStaleIDs(LinkSegmentID oldSeg) {
    String lastUseless = oldSeg.getLinkSegTag();
    while (true) {
      String replacementParent = allGone_.get(lastUseless);
      if (replacementParent == null) {
        break;
      }
      lastUseless = replacementParent;
    }
    return (lastUseless);
  }
} 
