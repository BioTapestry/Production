/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/****************************************************************************
**
** Utility for bounds creation
*/

public class Bounds {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public Bounds() {
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

 
  /***************************************************************************
  **
  ** Init the range with new data
  */
  
  public static Rectangle2D initBoundsWithPoint(Point2D point) {
    Rectangle2D retval = new Rectangle2D.Double(point.getX(), point.getY(), 0.0, 0.0);
    return (retval);    
  }  

  /***************************************************************************
  **
  ** Tweak the range with new Point data
  */
  
  public static void tweakBoundsWithPoint(Rectangle2D currBounds, Point2D point) {
    currBounds.add(point);
    return;
  }

  /***************************************************************************
  **
  ** Tweak the range with new data
  */
  
  public static void tweakBounds(Rectangle currBounds, Rectangle newBounds) {
    if ((currBounds.width == 0) && (currBounds.height == 0)) {
      currBounds.x = newBounds.x;
      currBounds.y = newBounds.y;
      currBounds.width = newBounds.width;
      currBounds.height = newBounds.height;
      return;
    }
    
    tweakBoundsCore(currBounds, newBounds);
    return;    
  }
  
  /***************************************************************************
  **
  ** Tweak the range with new data
  */
  
  private static void tweakBoundsCore(Rectangle currBounds, Rectangle newBounds) {
    int currMaxX = currBounds.width + currBounds.x;    
    int currMaxY = currBounds.height + currBounds.y;
    int newMaxX = newBounds.width + newBounds.x;    
    int newMaxY = newBounds.height + newBounds.y;
    if (newBounds.x < currBounds.x) {
      currBounds.x = newBounds.x;
    }
    if (newBounds.y < currBounds.y) {
      currBounds.y = newBounds.y;
    }    
    if (newMaxX > currMaxX) {
      currBounds.width = newMaxX - currBounds.x;
    } else {
      currBounds.width = currMaxX - currBounds.x;
    }
    if (newMaxY > currMaxY) {
      currBounds.height = newMaxY - currBounds.y;
    } else {
      currBounds.height = currMaxY - currBounds.y;
    }
    return;    
  } 
  
  /***************************************************************************
  **
  ** Pad the range by the specified amounts
  */
  
  public static void padBounds(Rectangle currBounds, int xPad, int yPad) {
    currBounds.x -= xPad;
    currBounds.y -= yPad;
    currBounds.width += (2 * xPad);
    currBounds.height += (2 * yPad);
    return;    
  }
  
  /***************************************************************************
  **
  ** Pad the range by the specified amounts
  */
  
  public static void padBounds(Rectangle currBounds, int tPad, int bPad, int lPad, int rPad) {
    currBounds.x -= lPad;
    currBounds.y -= tPad;
    currBounds.width += (lPad + rPad);
    currBounds.height += (tPad + bPad);
    return;    
  }  
  
  /***************************************************************************
  **
  ** Answer if the given point lies within the bounds
  */
  
  public static boolean intersects(Rectangle bounds, int x, int y) {
    int maxX = bounds.width + bounds.x;    
    int maxY = bounds.height + bounds.y;
    return ((x >= bounds.x) && (y >= bounds.y) && (x <= maxX) && (y <= maxY));
  }
  
  /***************************************************************************
  **
  ** Answer if the given point lies within the bounds
  */
  
  public static boolean intersects(Rectangle2D bounds, double x, double y) {
    return (bounds.outcode(x, y) == 0);
  }
  
  /***************************************************************************
  **
  ** Answer if the given point lies within the bounds
  */
  
  public static boolean intersects(double minX, double minY, double maxX, 
                                   double maxY, double x, double y) {
    return ((x >= minX) && (y >= minY) && (x <= maxX) && (y <= maxY));
  }  
}
