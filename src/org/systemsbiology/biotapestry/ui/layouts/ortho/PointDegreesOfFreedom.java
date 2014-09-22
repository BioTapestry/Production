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

package org.systemsbiology.biotapestry.ui.layouts.ortho;

import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.util.Vector2D;


/****************************************************************************
**
** Degrees of freedom for a point
*/

public class PointDegreesOfFreedom implements Cloneable {

  public DegreeOfFreedom xDOF;
  public DegreeOfFreedom yDOF;
  public Vector2D requiredDir;
  public Point2D point;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public PointDegreesOfFreedom(Point2D point, DegreeOfFreedom xDOF, DegreeOfFreedom yDOF, Vector2D requiredDir) {          
    this.point = (point != null) ? (Point2D)point.clone() : null;
    this.xDOF = xDOF;
    this.yDOF = yDOF;
    this.requiredDir = requiredDir;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Gotta be able to clone to achieve a pile of strategies
  */
  
  public PointDegreesOfFreedom clone() {
    try {       
      PointDegreesOfFreedom retval = (PointDegreesOfFreedom)super.clone();
      retval.point = (this.point != null) ? (Point2D)this.point.clone() : null;
      retval.xDOF = this.xDOF.clone();
      retval.yDOF = this.yDOF.clone();
      retval.requiredDir = (this.requiredDir != null) ? this.requiredDir.clone() : null;
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
}
