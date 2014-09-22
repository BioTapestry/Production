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

import java.util.ArrayList;
import java.util.List;

/****************************************************************************
**
** A class for optimizing link paths
*/

public class SegmentWithKids {
    
  LinkSegment segment;
  ArrayList<SegmentWithKids> kids;

  SegmentWithKids(LinkSegment seg) {
    this.segment = seg;
    kids = new ArrayList<SegmentWithKids>();
  }

  String getParentID() {
    return (segment.getParent());
  }

  List<String> getChildrenIDs() {
    ArrayList<String> retval = new ArrayList<String>();
    int size = kids.size();
    for (int i = 0; i < size; i++) {
      SegmentWithKids child = kids.get(i);
      String kidID = child.segment.getID();
      if (kidID != null) {  // null ids for bus-drop segments
        retval.add(kidID);
      }
    }
    return (retval);
  }

  SegmentWithKids getSwkChild(String id) {
    if (id == null) {
      return (null);
    }
    if (id.equals(segment.getID())) {
      return (this);
    }
    int size = kids.size();
    for (int i = 0; i < size; i++) {
      SegmentWithKids child = kids.get(i);
      SegmentWithKids result = child.getSwkChild(id);
      if (result != null) {
        return (result);
      }
    }
    return (null);
  }

  List<String> getSiblingIDs(SegmentWithKids rootSwk) {
    ArrayList<String> retval = new ArrayList<String>();
    String parentID = segment.getParent();
    if (parentID == null) {
      return (retval);
    }
    SegmentWithKids parent = rootSwk.getSwkChild(parentID);
    int size = parent.kids.size();
    for (int i = 0; i < size; i++) {
      SegmentWithKids sibling = parent.kids.get(i);
      if (sibling == this) {
        continue;
      }
      String sibID = sibling.segment.getID();
      if (sibID != null) {  // null ids for bus-drop segments
        retval.add(sibID);
      }
    }
    return (retval);
  }
}
