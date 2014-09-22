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
** By moving the pattern only vertically, tries to place the pattern to
** minimize height growth
*/

public class PatternPlacerMinimizeHeight {
  
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
  private int leftColumn_;
  private int stepSize_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PatternPlacerMinimizeHeight(PatternGrid grid, Pattern pattern, int leftColumn) {
    this(grid, pattern, leftColumn, 1);
  } 
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PatternPlacerMinimizeHeight(PatternGrid grid, Pattern pattern, 
                                     int leftColumn, int stepSize) {
    grid_ = grid;
    pattern_ = pattern;
    leftColumn_ = leftColumn;
    stepSize_ = stepSize;
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
    // Figure out the range of placement options, from nested on top
    // to nested on the bottom.  Try to place it between these two
    // if possible.  Moving top to bottom, place it in the first slot
    // that does not grow the height.  If none exists, place it nested
    // on the bottom.  If no bounds at all, place it at the origin.
    //
    
    //
    // Get the bounds of the whole grid.  If none, we are done.
    //
    
    MinMax minMax = grid_.getMinMaxYForRange(null);
    if (minMax == null) {
      grid_.place(pattern_, leftColumn_, 0);
      return (new Point(leftColumn_, 0));
    }
    
    //
    // Find out the placement range for the columns of interest
    //
    
    MinMax patColRange = pattern_.getWidthRange();
    if (patColRange == null) {  // why place an empty pattern?
      throw new IllegalStateException();
    }
    int minCol = leftColumn_ + patColRange.min;
    int maxCol = leftColumn_ + patColRange.max;
    MinMax minMaxForCols = grid_.getMinMaxYForRange(new MinMax(minCol, maxCol));

    MinMax patRowRange = pattern_.getHeightRange();
    if (patRowRange == null) {  // why place an empty pattern?
      throw new IllegalStateException();
    }
    
    //
    // Nothing to collide with: place it right at the top
    //
    
    if (minMaxForCols == null) {
      int placeRow = minMax.min - patRowRange.min;
      grid_.place(pattern_, leftColumn_, placeRow);
      return (new Point(leftColumn_, placeRow));
    }
      
    //
    // Start sliding it down from the top possible position, going with the first that
    // does not grow the height upward.
    //

    int currY = minMaxForCols.min - pattern_.getHeight() + patRowRange.min;
    while (true) {
      if (grid_.emptyIntersection(pattern_, leftColumn_, currY)) {
        PatternGrid testGrid = grid_.nondestructivePlace(pattern_, leftColumn_, currY);
        MinMax testMinMax = testGrid.getMinMaxYForRange(null);
        if (testMinMax == null) {
          throw new IllegalStateException();
        }
        if (testMinMax.min >= minMax.min) {
          grid_.place(pattern_, leftColumn_, currY);
          return (new Point(leftColumn_, currY));          
        }
      }
      currY += stepSize_;
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
