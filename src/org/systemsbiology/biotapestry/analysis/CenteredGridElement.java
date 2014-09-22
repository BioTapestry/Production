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

import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A class describing varying rectangles that expand around the centerline
*/
  
public class CenteredGridElement implements Comparable<CenteredGridElement>, GridGrower.GeneralizedGridElement {

  public GridElement minGridElement;
  public GridElement maxGridElement;

  public CenteredGridElement(CenteredGridElement other) {
    this.minGridElement = new GridElement(other.minGridElement);
    this.maxGridElement = new GridElement(other.maxGridElement);
  } 
  
  CenteredGridElement(GridElement otheri, GridElement othera) {
    this.minGridElement = otheri;
    this.maxGridElement = othera;
  } 
   
  public CenteredGridElement(Point2D pt) {
    Point2D minPt = (Point2D)pt.clone();
    Point2D maxPt = new Point2D.Double(pt.getX() + (UiUtil.GRID_SIZE * 0.5), pt.getY() + (UiUtil.GRID_SIZE * 0.5));
    this.minGridElement = new GridElement(minPt, minPt.clone(), 0.5);
    this.maxGridElement = new GridElement(maxPt, maxPt.clone(), 0.5);
  }
  
  public void setDeltas(int delColL, int delColR, int delRowU, int delRowL) {
    minGridElement.deltaX = delColL * UiUtil.GRID_SIZE;
    maxGridElement.deltaX = delColR * UiUtil.GRID_SIZE;
    minGridElement.deltaY = delRowU * UiUtil.GRID_SIZE;
    maxGridElement.deltaY = delRowL * UiUtil.GRID_SIZE;
    return;
  }
  
  public Point2D getID() {
    return ((Point2D)minGridElement.id);
  }
  
  public Point2D getPoint() {
    return (new Point2D.Double(this.maxGridElement.rect.getX(), this.maxGridElement.rect.getY()));
  }
    
  public void setID(Point2D id) {
    Point2D minPt = (Point2D)id.clone();
    Point2D maxPt = new Point2D.Double(id.getX() + (UiUtil.GRID_SIZE * 0.5), id.getY() + (UiUtil.GRID_SIZE * 0.5));
    minGridElement.id = minPt;
    maxGridElement.id = maxPt;
    return;
  }
  
  public int hashCode() {
    return (minGridElement.hashCode() + maxGridElement.hashCode());
  }

  public String toString() {
    return ("CenteredGridElement: " + minGridElement + " " + maxGridElement);
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof CenteredGridElement)) {
      return (false);
    }
    CenteredGridElement cgeo = (CenteredGridElement)other;
        
    if (!this.minGridElement.equals(cgeo.minGridElement)) {
      return (false);
    }
    return (this.maxGridElement.equals(cgeo.maxGridElement));
  } 

  public int compareTo(CenteredGridElement other) {

    int minCo = this.minGridElement.compareTo(other.minGridElement);
    if (minCo != 0) {
      return (minCo);
    }
    
    return (this.maxGridElement.compareTo(other.maxGridElement));
  }
}
 
