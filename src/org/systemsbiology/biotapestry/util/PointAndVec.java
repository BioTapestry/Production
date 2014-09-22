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

package org.systemsbiology.biotapestry.util;

import java.awt.geom.Point2D;

/****************************************************************************
**
** For defining points wrt changing region boundaries
*/

public class PointAndVec implements Cloneable {
  
  public Point2D origin;
  public Vector2D offset;

  public PointAndVec(Point2D origin, Vector2D offset) {
    this.origin = origin;
    this.offset = offset;
  }

  public String toString() {
    return ("PointAndVec: pt = " + origin + " vec = " + offset);  
  }
  
  public PointAndVec clone() { 
    try {
      PointAndVec retval = (PointAndVec)super.clone();
      retval.origin = (Point2D)this.origin.clone();
      retval.offset = (this.offset == null) ? null : (Vector2D)this.offset.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
}
