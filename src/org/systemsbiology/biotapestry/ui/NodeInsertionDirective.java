/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.util.List;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This contains details of how to handle node insertions into links
*/

public class NodeInsertionDirective {
  public int landingPad;
  public int launchPad;
  public int orientation;
  public Vector2D offset;
  public List<Point2D> landingCorners;
  public List<Point2D> launchCorners;
  
  public NodeInsertionDirective(int land, int launch) {
    landingPad = land;
    launchPad = launch;
    orientation = NodeProperties.RIGHT;
    offset = new Vector2D(0.0, 0.0);
    landingCorners = null;
    launchCorners = null;
  }
  
  public NodeInsertionDirective(int land, int launch, int orient) {
    landingPad = land;
    launchPad = launch;
    orientation = orient;
    offset = new Vector2D(0.0, 0.0);
    landingCorners = null;
    launchCorners = null;
  }
  
  public NodeInsertionDirective(int land, int launch, int orient, Vector2D offset, 
                                List<Point2D> landingCorners, List<Point2D> launchCorners) {
    landingPad = land;
    launchPad = launch;
    orientation = orient;
    this.offset = offset;
    this.landingCorners = landingCorners;
    this.launchCorners = launchCorners;
  }  
}