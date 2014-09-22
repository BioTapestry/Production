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

public class PatternPlacerVerticalFit {
  
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
  private int targetRow_;
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

  public PatternPlacerVerticalFit(PatternGrid grid, Pattern pattern, 
                                  int targetRow, int leftColumn) {
    grid_ = grid;
    pattern_ = pattern;
    targetRow_ = targetRow;
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

    //
    // Keep sliding the pattern up until we can place it.  Record the
    // distance from the target row.  Do the same in the other
    // direction.  Take the minimum offset in either direction.

    int bestBelow = Integer.MAX_VALUE;
    int currY = targetRow_;
    while (true) {
      if (grid_.emptyIntersection(pattern_, leftColumn_, currY)) {
        bestBelow = currY;
        break;
      }
      currY--;
    }
    
    if (bestBelow == Integer.MAX_VALUE) {
      throw new IllegalStateException();
    }
    
    if (bestBelow == targetRow_) {
      grid_.place(pattern_, leftColumn_, targetRow_);
      return (new Point(leftColumn_, targetRow_));
    }
    
    int maxY = targetRow_ + (targetRow_ - bestBelow);
    int bestHigh = Integer.MIN_VALUE;
    currY = targetRow_;
    while (currY < maxY) {  // not <=, ties go to plavement above
      if (grid_.emptyIntersection(pattern_, leftColumn_, currY)) {
        bestHigh = currY;
        break;
      }
      currY++;
    }
    
    int bestRow = (bestHigh == Integer.MIN_VALUE) ? bestBelow : bestHigh;
      
    grid_.place(pattern_, leftColumn_, bestRow);
    return (new Point(leftColumn_, bestRow));
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
