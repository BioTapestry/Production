/*
**    Copyright (C) 2003-2007 Institute for Systems Biology 
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
** outward in a spiral
*/

public class PatternPlacerSpiral {
  
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
  
  public static final int CW = 0;
  public static final int RIGHT_ONLY = 1;
   
  public static final int UP = 10;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private PatternGrid grid_;
  private Pattern pattern_;
  private Point initialPoint_;
  private int padding_;
  private int leftColumn_;
  private int mode_;
  private int direction_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PatternPlacerSpiral(PatternGrid grid, Pattern pattern, Point initialPoint,
                             int mode, int direction) {
    grid_ = grid;
    pattern_ = pattern;
    initialPoint_ = (Point)initialPoint.clone();
    mode_ = mode;
    direction_ = direction;
    if (direction_ != UP) {
      throw new IllegalArgumentException();
    }
    if ((mode_ != CW) && (mode_ != RIGHT_ONLY)) {
      throw new IllegalArgumentException();
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Place the pattern
  */
  
  public Point placePattern() {
    Point retval = locatePattern();
    grid_.place(pattern_, retval.x, retval.y);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Sink an already located pattern
  */
  
  public void sinkPattern(Point leftCorner) {
    grid_.place(pattern_, leftCorner.x, leftCorner.y);
    return;
  }  

  /***************************************************************************
  **
  ** Find location for pattern (but do not place it); return the upper left corner point
  */
  
  public Point locatePattern() {

    //
    // Spiral the pattern out from the target point
    //
     
    SpiralState ss;
    if (mode_ == CW) {
      ss = new CircleState(initialPoint_);
    } else { // if (mode_ == RIGHT_ONLY) {
      ss = new SweepState(initialPoint_);
    }
    if (grid_.emptyIntersection(pattern_, initialPoint_.x, initialPoint_.y)) {
      return (new Point(initialPoint_));
    }
    Point fillPoint = new Point();
    while (true) {
      ss.next(fillPoint);
      if (grid_.emptyIntersection(pattern_, fillPoint.x, fillPoint.y)) {
        return (new Point(fillPoint));
      }
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
  
  private static abstract class SpiralState {
    protected Point currPt_;
    abstract Point next(Point toFill);
  }
    
  private static class CircleState extends SpiralState {
    private int runLen_;
    private int runRemains_;
    private int runPhase_;
    private int runIndex_;
    private int[] patternX_;
    private int[] patternY_;
    
    CircleState(Point origin) {
      currPt_ = (Point)origin.clone();
      patternX_ = new int[] {0, 1, 0, -1};
      patternY_ = new int[] {-1, 0, 1, 0};
      runLen_ = 1;
      runRemains_ = 1;
      runIndex_ = 0;
      runPhase_ = 0;
    }    
 
    Point next(Point toFill) {
      if (runRemains_ == 0) {
        runIndex_ = (runIndex_ + 1) % 4;
        if (runPhase_ == 1) {
          runLen_++;
          runPhase_ = 0;
        } else {
          runPhase_ = 1;
        }
        runRemains_ = runLen_ - 1;
      } else {
        runRemains_--;
      }
      currPt_.x = currPt_.x + patternX_[runIndex_];
      currPt_.y = currPt_.y + patternY_[runIndex_]; 
      toFill.x = currPt_.x;
      toFill.y = currPt_.y;
      return (toFill);
    }
  }
    
  private static class SweepState extends SpiralState {
    private int phase;
    private int sign;
    private int radius;
    private int remaining;
    
    SweepState(Point origin) {
      currPt_ = (Point)origin.clone();
      phase = 0;
      sign = -1;
      radius = 1;
      remaining = 0;
    }
    
    Point next(Point toFill) {
      int[] delta = new int[2];
      int pad = (sign == -1) ? 0 : 1;
      switch (phase) {
        case 0:  // one step down or up
          delta[0] = 0;
          delta[1] = 1 * sign;
          phase = 1;
          remaining = radius + pad;
          break;
        case 1: // step right by radius
          delta[0] = 1;
          delta[1] = 0;
          if (--remaining == 0) {
            phase = 2;
            remaining = 2 * (radius + pad);
          }
          break;
        case 2:  // step up/down by 2 * (radius + pad)
          delta[0] = 0;
          delta[1] = -1 * sign;
          if (--remaining == 0) {
            phase = 3;
            remaining = radius + pad;
          }          
          break;
        case 3:
          delta[0] = -1;
          delta[1] = 0;
          if (--remaining == 0) {
            phase = 0;
            if (sign == 1) {
              radius += 2;
            }
            sign *= -1;
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
      
      currPt_.x = currPt_.x + delta[0];
      currPt_.y = currPt_.y + delta[1]; 
      toFill.x = currPt_.x;
      toFill.y = currPt_.y;
      return (toFill);
    }
  }
  
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    
    SpiralState ss = new CircleState(new Point(0, 0));
    Point fillPoint = new Point();
    for (int i = 0; i < 50; i++) {
      System.out.println(ss.next(fillPoint));
    }
    System.out.println("-------------------------------------");
    
    ss = new SweepState(new Point(0, 0));
    fillPoint = new Point();
    for (int i = 0; i < 50; i++) {
      System.out.println(ss.next(fillPoint));
    }    
    
    
  }    
}
