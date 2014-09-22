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

package org.systemsbiology.biotapestry.analysis;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A class describing varying rectangles
*/
  
public class GridElement implements Comparable<GridElement>, GridGrower.GeneralizedGridElement {

  public Rectangle2D rect;
  public Object id;
  public double deltaX;
  public double deltaY;

  public GridElement(Rectangle2D rect, Object id, double deltaX, double deltaY) {
    this.rect = rect;
    this.id = id;
    this.deltaX = deltaX;
    this.deltaY= deltaY;
  }
  
  public GridElement(GridElement other) {
    this.rect = (Rectangle2D)other.rect.clone();
    this.id = other.id; // That's right...we are sharing the reference!  Can't clone a generic object....
    this.deltaX = other.deltaX;
    this.deltaY= other.deltaY;
  } 
  
  public GridElement(Point2D pt, Object id, double frac) {
    this.rect = new Rectangle2D.Double(pt.getX(), pt.getY(), UiUtil.GRID_SIZE * frac, UiUtil.GRID_SIZE * frac);
    this.id = id;
  }
  
  public void setDeltas(int delCol, int delRow) {
    this.deltaX = delCol * UiUtil.GRID_SIZE;
    this.deltaY = delRow * UiUtil.GRID_SIZE;
    return;
  }

  public int hashCode() {
    return (rect.hashCode());
  }

  public String toString() {
    return ("GridElement: " + rect + " " + id + " " + deltaX + " " + deltaY);
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof GridElement)) {
      return (false);
    }
    return (this.compareTo((GridElement)other) == 0);
  } 

  public int compareTo(GridElement other) {

    double areaThis = area(this.rect);
    double areaOther = area(other.rect);

    if (areaThis != areaOther) {
      return ((areaThis > areaOther) ? 1 : -1);
    }

    double distanceThis = distanceToOrigin(this.rect);
    double distanceOther = distanceToOrigin(other.rect);

    if (distanceThis != distanceOther) {
      return ((distanceThis > distanceOther) ? 1 : -1);
    }      

    return (this.id.toString().compareTo(other.id.toString()));
  }

  private double area(Rectangle2D r) {
    return (r.getHeight() * r.getWidth());
  }

  private double distanceToOrigin(Rectangle2D r) {
    return (Math.sqrt((r.getX() * r.getX()) + (r.getY() * r.getY())));
  } 
}
 
