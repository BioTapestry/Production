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

import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.NodeInstance;

/****************************************************************************
**
** Analyzes and applies special properties to link segments across link
** relayouts
*/

public class SpecialSegmentTracker {
  
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
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashMap<Set<String>, SuggestedDrawStyle> specialLinks_;
  private HashMap<String, SuggestedDrawStyle> specialDrops_;  
  private SuggestedDrawStyle rootDropIsSpecial_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public SpecialSegmentTracker() {
    specialLinks_ = new HashMap<Set<String>, SuggestedDrawStyle>();
    specialDrops_ = new HashMap<String, SuggestedDrawStyle>();    
    rootDropIsSpecial_ = null;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out which segments need special link properties  
  */
  
  public void analyzeSpecialLinks(BusProperties bp) {
    Iterator<LinkSegment> sit = bp.getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      SuggestedDrawStyle sds = seg.getSpecialDrawStyle();
      if (sds != null) {
        LinkSegmentID sID = LinkSegmentID.buildIDForSegment(seg.getID());     
        Set<String> lThruS = bp.resolveLinkagesThroughSegment(sID);
        specialLinks_.put(lThruS, sds.clone());
      }
    }
 
    Iterator<LinkBusDrop> dit = bp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      SuggestedDrawStyle sds = drop.getSpecialDropDrawStyle();
      if (sds == null) {
        continue;
      }
      String ref = drop.getTargetRef();
      if (ref == null) {
        rootDropIsSpecial_ = sds.clone();
      } else {
        specialDrops_.put(ref, sds.clone());
      }
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Build a tracker that inherits its info from a tracker for the root layout
  */
  
  public SpecialSegmentTracker buildInheritedSpecialLinks(BusProperties bp, NodeInstance src) {
    SpecialSegmentTracker retval = new SpecialSegmentTracker();
    
    Iterator<LinkSegment> sit = bp.getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      LinkSegmentID sID = LinkSegmentID.buildIDForSegment(seg.getID());     
      Set<String> lThruS = bp.resolveLinkagesThroughSegment(sID);
      Set<String> reducedLThruS = reduceToBaseIDs(lThruS);
      SuggestedDrawStyle rsds = specialLinks_.get(reducedLThruS);
      if (rsds != null) {
        retval.specialLinks_.put(reducedLThruS, rsds.clone());
      }
    }
    
    if (rootDropIsSpecial_ != null) {
      retval.rootDropIsSpecial_ = rootDropIsSpecial_.clone();
    }    
 
    Iterator<LinkBusDrop> dit = bp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String ref = drop.getTargetRef();
      if (ref != null) {
        String reduced = GenomeItemInstance.getBaseID(ref);
        SuggestedDrawStyle sds = specialDrops_.get(reduced);
        if (sds != null) {
          retval.specialDrops_.put(ref, sds.clone());
        }
      }
    }
       
    return (retval);
  }

  /***************************************************************************
  **
  ** Answer if we have any useful info:
  */
  
  public boolean hasSpecialLinks() {
    return ((rootDropIsSpecial_ != null) || !specialLinks_.isEmpty() || !specialDrops_.isEmpty());
  }
  
  /***************************************************************************
  **
  ** Reapply which links need special link properties. 
  */
  
  public void reapplySpecialLinks(BusProperties bp) {
    
    Iterator<LinkSegment> sit = bp.getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      LinkSegmentID sID = LinkSegmentID.buildIDForSegment(seg.getID());     
      Set<String> lThruS = bp.resolveLinkagesThroughSegment(sID);
      SuggestedDrawStyle sds = specialLinks_.get(lThruS);
      if (sds != null) {
        seg.setDrawStyle(sds.clone());
      }
    }
 
    Iterator<LinkBusDrop> dit = bp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        if (rootDropIsSpecial_ != null) {
          drop.setDrawStyleForDrop(rootDropIsSpecial_.clone());
        }        
      } else {
        SuggestedDrawStyle sds = specialDrops_.get(ref);
        if (sds != null) {
          drop.setDrawStyleForDrop(sds.clone());
        }
      }
    }
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Make a set of baseIDs from a set of instanceIDs:
  */
  
  private Set<String> reduceToBaseIDs(Set<String> instances) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> iit = instances.iterator();
    while (iit.hasNext()) {
      String linkID = iit.next();
      String redID = GenomeItemInstance.getBaseID(linkID);
      retval.add(redID);
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
