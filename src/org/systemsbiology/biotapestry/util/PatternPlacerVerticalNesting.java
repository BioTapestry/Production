/*
**    Copyright (C) 2003-2004 Institute for Systems Biology 
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

import java.awt.Point;

/****************************************************************************
**
** Does the best fit of the given pattern to the given grid by moving the pattern
** only vertically
*/

public class PatternPlacerVerticalNesting {
  
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
  
  private PatternGrid grid_;
  private Pattern pattern_;
  private boolean onTop_;
  private int padding_;
  private int leftColumn_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PatternPlacerVerticalNesting(PatternGrid grid, Pattern pattern, int leftColumn, int padding, boolean onTop) {
    grid_ = grid;
    pattern_ = pattern;
    onTop_ = onTop;
    padding_ = padding;
    leftColumn_ = leftColumn;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Place the pattern; return the upper left corner point
  */
  
  public Point placePattern() {
    Point retval = locatePattern();
    grid_.place(pattern_, leftColumn_, retval.y);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Sink an already located pattern
  */
  
  public void sinkPattern(Point leftCorner) {
    grid_.place(pattern_, leftColumn_, leftCorner.y);
    return;
  }  

  /***************************************************************************
  **
  ** Find location for pattern (but do not place it); return the upper left corner point
  */
  
  public Point locatePattern() {

    //
    // Slide pattern vertically from "infinity" (+/-, depending on "onTop")
    // until it nests up against the current pattern.  If the pattern is
    // fully left or right of the current pattern, center it on the current
    // pattern centerline.
    //
    
    MinMax yRange = grid_.getMinMaxYForRange(null);
    int topY = yRange.min - pattern_.getHeight() - 5;
    int botY = yRange.max + 5;
    int midY = ((yRange.max + yRange.min) / 2) - (pattern_.getHeight() / 2);
    int currY;
    int sign;
    int end;
    if (onTop_) {
      currY = topY;
      sign = 1;
      end = botY;
    } else {
      currY = botY;
      sign = -1;
      end = topY;
    }
    
    while (true) {
      if (!grid_.emptyIntersection(pattern_, leftColumn_, currY)) {
        int placeRow = currY - (sign * padding_);
        return (new Point(leftColumn_, placeRow));
      }
      if (currY == end) {
        return (null);  //new Point(leftColumn_, midY));
      }
      currY += sign;
    }
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
