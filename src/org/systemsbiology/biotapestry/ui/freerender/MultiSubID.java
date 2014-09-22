/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.freerender;

import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.Mergeable;

/***************************************************************************
**
** Used to hold multiple references to subparts
*/
  
public class MultiSubID implements Mergeable {
  private HashSet parts;

  public MultiSubID() {
    this.parts = new HashSet();
  }

  public MultiSubID(MultiSubID other) {
    this.parts = new HashSet();
    Iterator oit = other.parts.iterator();
    while (oit.hasNext()) {
      LinkSegmentID lsid = (LinkSegmentID)oit.next();
      this.parts.add(new LinkSegmentID(lsid));
    }
  }

  public MultiSubID(LinkSegmentID lsid) {
    parts = new HashSet();
    parts.add(lsid);      
  }
  
  public MultiSubID(Set lsidSet) {
    parts = new HashSet();
    parts.addAll(lsidSet);      
  }
   
  public Set getParts() {
    return (this.parts);
  }
  
  public String toString() {
    return (this.parts.toString());
  }        

  public int hashCode() {
    return (this.parts.hashCode());
  }

  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof MultiSubID)) {
      return (false);
    }
    MultiSubID otherMS = (MultiSubID)other;
    return (this.parts.equals(otherMS.parts));
  }

  public boolean intersects(Mergeable other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof MultiSubID)) {
      return (false);  
    }
    MultiSubID otherMS = (MultiSubID)other;    
    HashSet intersect = new HashSet(this.parts);
    intersect.retainAll(otherMS.parts);
    return (intersect.size() != 0);
  }

  public MultiSubID intersectComplement(MultiSubID other) {
    //
    // Union:
    //
    HashSet retset = new HashSet(parts);
    retset.addAll(other.parts);

    //
    // Intersection:
    //

    HashSet inter = new HashSet(parts);
    inter.retainAll(other.parts);

    //
    // Symmetric difference:
    //

    retset.removeAll(inter);
    MultiSubID retval = new MultiSubID();
    retval.parts = retset;
    return (retval);
  }
  
  public MultiSubID intersection(MultiSubID other) { 
    //
    // Intersection:
    //
    HashSet inter = new HashSet(parts);
    inter.retainAll(other.parts);
    MultiSubID retval = new MultiSubID();
    retval.parts = inter;
    return (retval);
  }  
 
  public void merge(MultiSubID other) {
    parts.addAll(other.parts);     
    return;
  }

}  
