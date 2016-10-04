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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.freerender.MultiSubID;

/****************************************************************************
**
** Describes a selection intersection with a screen object
*/

public class Intersection {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int IS_NONE   = 0;
  public static final int IS_GENE   = 1;  
  public static final int IS_NODE   = 2;
  public static final int IS_LINK   = 3;  
  public static final int IS_NOTE   = 4;  
  public static final int IS_GROUP  = 5;  
  public static final int IS_MODULE = 6;    
  public static final int IS_MODULE_LINK_TREE = 7;  
  public static final int IS_OVERLAY = 8;  // Actually represents a non-intersection with a current overlay displayed, for popups only    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String objectID_;
  private Object subID_;
  private double distance_;
  private boolean canMerge_;
  private ArrayList<PadVal> padCand_;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public Intersection(String objectID, Object subID, double distance) {
    objectID_ = objectID;
    subID_ = subID;
    distance_ = distance;
    canMerge_ = false;
    padCand_ = null;
  }
    
  /***************************************************************************
  **
  ** Constructor
  */

  public Intersection(String objectID, Object subID, double distance, boolean canMerge) {
    objectID_ = objectID;
    subID_ = subID;
    distance_ = distance;
    canMerge_ = canMerge;
    padCand_ = null;
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public Intersection(Intersection other, Object subID) {
    this.objectID_ = other.objectID_;
    this.subID_ = subID;
    this.distance_ = other.distance_;
    this.canMerge_ = other.canMerge_;
    padCand_ = null;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public Intersection(String objectID, List<PadVal> padCand) {
    objectID_ = objectID;
    subID_ = padCand.get(0);
    distance_ = 0.0;
    canMerge_ = false;
    padCand_ = new ArrayList<PadVal>(padCand);
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public Intersection(Intersection other) {
    this.objectID_ = other.objectID_;
    if (other.subID_ != null) {
      if (other.subID_ instanceof PadVal) {
        this.subID_ = new PadVal((PadVal)other.subID_);
        this.padCand_ = new ArrayList<PadVal>();
        int numO = other.padCand_.size();
        for (int i = 0; i < numO; i++) {
          this.padCand_.add(new PadVal(other.padCand_.get(i)));
        }
      } else {
        throw new IllegalArgumentException();  // Can't use copy constructor
      }
    }
    this.distance_ = other.distance_;
    this.canMerge_ = other.canMerge_;
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public boolean canCopy() {
    return ((subID_ == null) || (subID_ instanceof PadVal));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Get the object ID
  */
  
  public String getObjectID() {
    return (objectID_);
  }

  /***************************************************************************
  **
  ** Get the subID (may be null if we are referring to the entire object)
  */
  
  public Object getSubID() {
    return (subID_);
  }
  
  /***************************************************************************
  **
  ** Get the distance
  */
  
  public double getDistance() {
    return (distance_);
  }
  
  /***************************************************************************
  **
  ** Get the pad candidate list
  */
  
  public List<PadVal> getPadCand() {
    return (padCand_);
  }
  
  /***************************************************************************
  **
  ** Answer if two intersections can be merged
  */
  
  public boolean canMerge() {
    return (canMerge_);
  }
  
  /***************************************************************************
  **
  ** ToString replacement
  */  
  
  public String toString() {
    return ("ID = " + objectID_ + " sub = " + subID_ + 
            " distance = " + distance_ + " canMerge = " + canMerge_);
  }
  

  /***************************************************************************
  **
  ** Get Segment ID from intersection.
  */
  
  public LinkSegmentID segmentIDFromIntersect() {
    MultiSubID sub = (MultiSubID)this.getSubID();
    if (sub == null) {
      return (null);
    }
    if (sub.getParts().size() != 1) {
      throw new IllegalArgumentException();
    }
    return ((LinkSegmentID)sub.getParts().iterator().next());
  }

  /***************************************************************************
  **
  ** Get Segment IDs from intersection.
  */
  
  public LinkSegmentID[] segmentIDsFromIntersect() {
    MultiSubID sub = (MultiSubID)this.getSubID();
    return (segmentIDsFromMultiSubID(sub));
  }
  
  /***************************************************************************
  **
  ** Get Segment IDs from multisubID.
  */
  
  public static LinkSegmentID[] segmentIDsFromMultiSubID(MultiSubID sub) {
    if (sub == null) {
      return (null);
    }
    LinkSegmentID[] retval = new LinkSegmentID[sub.getParts().size()];
    Iterator<LinkSegmentID> spit = sub.getParts().iterator();
    int count = 0;
    while (spit.hasNext()) {
      retval[count++] = spit.next();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge two intersections from linkages.
  */
  
  public static Intersection mergeIntersect(Intersection first, Intersection second) {
    if (!first.getObjectID().equals(second.getObjectID())) {
      throw new IllegalArgumentException();
    }
    MultiSubID sub1 = (MultiSubID)first.getSubID();
    MultiSubID sub2 = (MultiSubID)second.getSubID();
    if ((sub1 == null) || (sub2 == null)) {
      return (new Intersection(first.getObjectID(), null, 0.0, true));
    }
    MultiSubID retSub = new MultiSubID();
    retSub.getParts().addAll(sub1.getParts());
    retSub.merge(sub2);
    return (new Intersection(first.getObjectID(), retSub, 0.0, true));
  }
  
  /***************************************************************************
  **
  ** Check if we can process the subID
  */
  
  public static boolean isLinkageSubID(Intersection inter) {
    return (inter.getSubID() instanceof MultiSubID);
  }

  /***************************************************************************
  **
  ** Copy the intersection produced for a Linkage
  */
  
  public static Intersection copyIntersection(Intersection inter) {
    MultiSubID msid = (MultiSubID)inter.getSubID();
    MultiSubID newMsid = (msid != null) ? new MultiSubID(msid) : null;
    return (new Intersection(inter, newMsid));
  }
  
  /***************************************************************************
  **
  ** Intersect complement for two intersections from linkages.
  */
  
  public static Intersection intersectComplement(Intersection first, 
                                          Intersection second) {
    if (!first.getObjectID().equals(second.getObjectID())) {
      throw new IllegalArgumentException();
    }
    //
    // Segments in both sets are dropped from the result
    //
    
    MultiSubID sub1 = (MultiSubID)first.getSubID();
    MultiSubID sub2 = (MultiSubID)second.getSubID();
    
    MultiSubID retSub = sub1.intersectComplement(sub2);
    return (new Intersection(first.getObjectID(), retSub, 0.0, true));
  }  
  
  /***************************************************************************
  **
  ** Intersect two intersections from linkages.
  */
  
  public static Intersection intersection(Intersection first, Intersection second) {
                          
    if (!first.getObjectID().equals(second.getObjectID())) {
      throw new IllegalArgumentException();
    }
 
    MultiSubID sub1 = (MultiSubID)first.getSubID();
    MultiSubID sub2 = (MultiSubID)second.getSubID();
    
    MultiSubID retSub = sub1.intersection(sub2);
    return (new Intersection(first.getObjectID(), retSub, 0.0, true));
  } 
  
  /***************************************************************************
  **
  ** Check if we can process the subID
  */
  
  public static boolean isGroupSubID(Intersection inter) {
    return (inter.getSubID() instanceof LabelSubID);
  }
  
  /***************************************************************************
  **
  ** Extract label ID
  */
  
  public static String getLabelID(Intersection inter) {
    return (((LabelSubID)inter.getSubID()).groupID);
  }
  
  /***************************************************************************
  **
  ** Copy the intersection produced for a Group
  */
  
  public static Intersection copyGroupIntersection(Intersection inter) {
    LabelSubID lsid = (LabelSubID)inter.getSubID();
    LabelSubID newLsid = (lsid != null) ? new LabelSubID(lsid.groupID) : null;
    return (new Intersection(inter, newLsid));
  }
  
  /***************************************************************************
  **
  ** Build a set of Label intersections
  */
  
  public static Intersection[] getLabelIntersections(GenomeInstance gi, Group group) {
    ArrayList<Intersection> retvalHold = new ArrayList<Intersection>();
    String groupID = group.getID();
    retvalHold.add(new Intersection(groupID, new LabelSubID(groupID), 0.0));
    Set<String> subsets = group.getSubsets(gi);
    Iterator<String> sidit = subsets.iterator();
    while (sidit.hasNext()) {
      String subid = sidit.next();
      retvalHold.add(new Intersection(groupID, new LabelSubID(subid), 0.0));
    }
    Intersection[] retval = new Intersection[retvalHold.size()];
    retvalHold.toArray(retval);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Fill out a full intersection with a full set of link segment IDs.
  */
  
  public static Intersection fullIntersection(GenomeItem item, DataAccessContext rcx, boolean includeRoot) {
                                         
    MultiSubID retSub = null;                             
    BusProperties bp = rcx.getLayout().getLinkProperties(item.getID());
    List<LinkSegmentID> segs = bp.getAllBusLinkSegments(rcx, includeRoot);    
    Iterator<LinkSegmentID> sit = segs.iterator();
    while (sit.hasNext()) {
      LinkSegmentID lsid = sit.next();
      MultiSubID si = new MultiSubID(lsid);
      if (retSub == null) {
        retSub = si;
      } else {
        retSub.merge(si);
      }                                
    }
    return (new Intersection(item.getID(), retSub, 0.0, true));           
  }  

  /***************************************************************************
  **
  ** Fill out an intersection with a full set of link segment IDs for a given path
  */
  
  public static Intersection pathIntersection(GenomeItem item, DataAccessContext rcx) {
    MultiSubID retSub = null;                             
    BusProperties bp = rcx.getLayout().getLinkProperties(item.getID());
    // Previous saw a problem here on path display reselection. Is this still the case? 
    List<LinkSegmentID> segs = bp.getBusLinkSegmentsForOneLink(rcx, item.getID());    
    Iterator<LinkSegmentID> sit = segs.iterator();
    while (sit.hasNext()) {
      LinkSegmentID lsid = sit.next();
      MultiSubID si = new MultiSubID(lsid);
      if (retSub == null) {
        retSub = si;
      } else {
        retSub.merge(si);
      }                                
    }
    return (new Intersection(item.getID(), retSub, 0.0, true));
  }

  /***************************************************************************
  **
  ** Fill out an intersection with the given set of segment IDs
  */
  
  public static Intersection intersectionForSegmentIDs(String linkID, Set<LinkSegmentID> segIDs) {    
    MultiSubID retSub = null;
    Iterator<LinkSegmentID> sit = segIDs.iterator();
    while (sit.hasNext()) {
      LinkSegmentID lsid = sit.next();
      MultiSubID si = new MultiSubID(lsid);
      if (retSub == null) {
        retSub = si;
      } else {
        retSub.merge(si);
      }                                
    }
    return (new Intersection(linkID, retSub, 0.0, true));
  }
  
  /***************************************************************************
  **
  ** Converting set of nodeIDs to a list of Intersections
  */

  public static List<Intersection> nodeIDsToInters(Set<String> nodeIDs) {
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    Iterator<String> onit = nodeIDs.iterator();
    while (onit.hasNext()) {
       String nodeID = onit.next();
       retval.add(new Intersection(nodeID, null, 0.0));
    }
    return (retval);      
  }
  
  
  /***************************************************************************
  **
  ** Converting set of linkIDs to a list of Intersections
  */

  public static List<Intersection> linkIDsToInters(Set<String> linkIDs, String genomeID, DataAccessContext rcx) {    
    HashMap<String, Intersection> srcToInter = new HashMap<String, Intersection>();
    Genome genome = rcx.getGenomeSource().getGenome(genomeID);
    Layout lo = rcx.getLayout();
    Iterator<String> olit = linkIDs.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      HashSet<LinkSegmentID> segIDs = new HashSet<LinkSegmentID>();
      int idType = (lo.getLinkProperties(linkID).isDirect()) ? LinkSegmentID.DIRECT_LINK : LinkSegmentID.END_DROP;
      LinkSegmentID dropSegID = LinkSegmentID.buildIDForType(linkID, idType);
      segIDs.add(dropSegID);
      Linkage link = genome.getLinkage(linkID);
      String srcID = link.getSource();
      Intersection inter = srcToInter.get(srcID);
      if (inter == null) {
        inter = Intersection.intersectionForSegmentIDs(linkID, segIDs);
      } else {
        String oldLink = inter.getObjectID();
        Intersection second = Intersection.intersectionForSegmentIDs(oldLink, segIDs);
        inter = Intersection.mergeIntersect(inter, second);
      }
      srcToInter.put(srcID, inter);
    }
 
    ArrayList<Intersection> retval = new ArrayList<Intersection>(srcToInter.values()); 
    return (retval);      
  }

  /***************************************************************************
  **
  ** Used for pad characterization
  */   
  
  public static class PadVal {
    public int padNum;
    public boolean okStart;
    public boolean okEnd;
    public double distance;
    
    public PadVal() {
    }
    
    public PadVal(PadVal other) {
      this.padNum = other.padNum;
      this.okStart = other.okStart;
      this.okEnd = other.okEnd;
      this.distance = other.distance;
    }
    
    public String toString() {
      return ("PadVal padNum = " + padNum + " okStart = " + okStart + 
              " okEnd = " + okEnd + " distance = " + distance);
    }
  }
  
  /***************************************************************************
  **
  ** Used for added intersection characterization
  */   
  
  public static class AugmentedIntersection {
    public Intersection intersect;
    public int type;
    
    public AugmentedIntersection(Intersection intersect, int type) {
      this.intersect = intersect;
      this.type = type;
    }
  }
  
  /***************************************************************************
  **
  ** Used to hold reference to a group label
  */
   
   public static class LabelSubID {
     public String groupID;
   
     public LabelSubID(String groupID) {
       this.groupID = groupID;
     }
   } 
  
  
  
}
