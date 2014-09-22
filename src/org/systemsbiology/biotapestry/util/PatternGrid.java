/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/****************************************************************************
**
** A class for laying out patterns
*/

public class PatternGrid {
  
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
  
  private HashMap pattern_;
  private ArrayList rects_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PatternGrid() {
    pattern_ = new HashMap();
    rects_ = new ArrayList();
  }
  
  /***************************************************************************
  **
  ** Copy Constructor (deep - except shared Strings)
  */

  public PatternGrid(PatternGrid other) {
    this.pattern_ = new HashMap();
    Iterator kit = other.pattern_.keySet().iterator();
    while (kit.hasNext()) {
      Point pt = (Point)kit.next();
      String gval = (String)other.pattern_.get(pt);
      this.pattern_.put(new Point(pt), gval);
    }
    
    this.rects_ = new ArrayList();
    int numRect = other.rects_.size();
    for (int i = 0; i < numRect; i++) {
      RectWithID rect = (RectWithID)other.rects_.get(i);
      this.rects_.add(new RectWithID(rect));
    } 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Fill in the given value.  String cannot be null!
  */
  
  public void fill(int x, int y, String val) {
    if (val == null) {
      throw new IllegalArgumentException();
    }
    Point pt = new Point(x, y);
    pattern_.put(pt, val);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the value at the given position
  */
  
  public String getValue(int x, int y) {
    Point pt = new Point(x, y);
    
    String retval = (String)pattern_.get(pt);
    if (retval != null) {
      return (retval);
    }
    
    int numRect = rects_.size();
    for (int i = 0; i < numRect; i++) {
      RectWithID rectwid = (RectWithID)rects_.get(i);
      Rectangle rect = rectwid.rect;
      if ((x >= rect.x) && (y >= rect.y) && (x < (rect.x + rect.width)) && (y < (rect.y + rect.height))) {
        return (rectwid.id);
      }
    }
    return (null);
  } 
  
  /***************************************************************************
  **
  ** Get the position of the given value, which must not be null.  If value
  ** occurs multiple times, which one returned is indeterminate.
  */
  
  public Point getLocation(String value) {
    if (value == null) {
      throw new IllegalArgumentException();
    }
    Iterator kit = pattern_.keySet().iterator();
    while (kit.hasNext()) {
      Point pt = (Point)kit.next();
      String gval = (String)pattern_.get(pt);
      if (value.equals(gval)) {
        return (pt);
      }
    }
    
    int numRect = rects_.size();
    for (int i = 0; i < numRect; i++) {
      RectWithID rect = (RectWithID)rects_.get(i);
      if (value.equals(rect.id)) {
        return (new Point(rect.rect.x, rect.rect.y));
      }
    } 
    
    return (null);
  }
  
  /***************************************************************************
  **
  ** Fill in the given box, but only if val is not null
  */
  
  public void conditionalFill(int x, int y, String val) {
    if (val == null) {
      return;
    }
    fill(x, y, val);
    return;
  }  
   
  /***************************************************************************
  **
  ** Insert the pattern into the grid, placing the pattern's upper left corner at x, y
  ** If there is a collision, the new value overwrites the old; so do an intersection
  ** test first to avoid this.
  */
  
  public void place(Pattern pat, int px, int py) {

    int maxX = pat.getWidth();
    int maxY = pat.getHeight();
    
    if (pat.isFilledBox()) {
      if ((maxX > 0) && (maxY > 0)) {
        String val = pat.getValue(0, 0);
        if (val != null) {
          Rectangle rect = new Rectangle(px, py, maxX, maxY);
          RectWithID rwid = new RectWithID(val, rect);
          rects_.add(0, rwid);
        }
      }
      //
      // Entries in the pattern map take precedence, so any intersection
      // needs to be nulled out:
      //
      Point pt = new Point(0, 0);
      for (int x = 0; x < maxX; x++) {
        for (int y = 0; y < maxY; y++) {
          pt.setLocation(px + x, py + y);
          if (pattern_.containsKey(pt)) {
            pattern_.remove(pt);
          }
        }
      }       
      return;
    }
   
    for (int x = 0; x < maxX; x++) {
      for (int y = 0; y < maxY; y++) {
        conditionalFill(px + x, py + y, pat.getValue(x, y));
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Like place, but returns a new grid instead of overwriting this one.
  */
  
  public PatternGrid nondestructivePlace(Pattern pat, int px, int py) {
    PatternGrid retval = new PatternGrid(this);
    retval.place(pat, px, py);
    return (retval);
  }

  /***************************************************************************
  **
  ** Answer if the given intersection is empty (i.e. patterns mutually exclusive)
  */
  
  public boolean emptyIntersection(Pattern pat, int px, int py) {
    int maxX = pat.getWidth();
    int maxY = pat.getHeight();
    
    //
    // Quick kill of rectangle intersections:
    
    if (pat.isFilledBox() && pattern_.isEmpty()) {
      Rectangle pRect = new Rectangle(px, py, maxX, maxY);
      int numRect = rects_.size();
      for (int i = 0; i < numRect; i++) {
        RectWithID rect = (RectWithID)rects_.get(i);
        if (rect.rect.intersects(pRect)) {
          return (false);
        }
      }
      return (true);
    }   

    for (int x = 0; x < maxX; x++) {
      for (int y = 0; y < maxY; y++) {
        String patValue = pat.getValue(x, y);
        String myValue = getValue(px + x, py + y);
        if ((patValue != null) && (myValue != null)) {
          return (false);
        }
      }
    }    
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Get the minimum and maximum bounds of the given range (inclusive).  If none,
  ** (i.e. unoccupied in that range) return null.  If argument is null, returns
  ** for entire grid
  */
  
  public MinMax getMinMaxYForRange(MinMax xRange) {
    //
    // Go through the keys for the given range and return the
    // minimum.
    //
    
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    
    Iterator kit = pattern_.keySet().iterator();
    while (kit.hasNext()) {
      Point pt = (Point)kit.next();
      String gval = (String)pattern_.get(pt);
      if (gval == null) {
        continue;
      }
      if ((xRange == null) || ((pt.x >= xRange.min) && (pt.x <= xRange.max))) {
        if (pt.y < minValue) {
          minValue = pt.y;
        }
        if (pt.y > maxValue) {
          maxValue = pt.y;
        }
      }
    }
    
    int numRect = rects_.size();
    for (int i = 0; i < numRect; i++) {
      RectWithID rectwid = (RectWithID)rects_.get(i);
      Rectangle rect = rectwid.rect;
      if (rect.y < minValue) {
        minValue = rect.y;
      }
      int hiVal = rect.y + rect.height - 1;
      if (hiVal > maxValue) {
        maxValue = hiVal;
      } 
    }
 
    if ((minValue == Integer.MAX_VALUE) || (maxValue == Integer.MIN_VALUE)) {
      return (null);
    } else {
      return (new MinMax(minValue, maxValue));
    }
  } 
  
  /***************************************************************************
  **
  ** Get the minimum and maximum bounds of the given range (inclusive).  If none,
  ** (i.e. unoccupied in that range) return null.  If argument is null, returns
  ** for entire grid
  */
  
  public MinMax getMinMaxXForRange(MinMax yRange) {
    //
    // Go through the keys for the given range and return the
    // minimum.
    //
    
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    
    Iterator kit = pattern_.keySet().iterator();
    while (kit.hasNext()) {
      Point pt = (Point)kit.next();
      String gval = (String)pattern_.get(pt);
      if (gval == null) {
        continue;
      }
      if ((yRange == null) || ((pt.y >= yRange.min) && (pt.y <= yRange.max))) {
        if (pt.x < minValue) {
          minValue = pt.x;
        }
        if (pt.x > maxValue) {
          maxValue = pt.x;
        }
      }
    }
    
    int numRect = rects_.size();
    for (int i = 0; i < numRect; i++) {
      RectWithID rectwid = (RectWithID)rects_.get(i);
      Rectangle rect = rectwid.rect;
      if (rect.x < minValue) {
        minValue = rect.x;
      }
      int hiVal = rect.x + rect.width - 1;
      if (hiVal > maxValue) {
        maxValue = hiVal;
      } 
    }
    
    if ((minValue == Integer.MAX_VALUE) || (maxValue == Integer.MIN_VALUE)) {
      return (null);
    } else {
      return (new MinMax(minValue, maxValue));
    }
  }

  
  /***************************************************************************
  **
  ** This is bogus glue code to generate a Pattern from this grid.  Needed for
  ** placement algorithms that only know how to locate Patterns in PatternGrids.
  */  
    
  public Pattern generatePattern() {
    MinMax yRange = getMinMaxYForRange(null);
    MinMax xRange = getMinMaxXForRange(null);
    if ((yRange == null) || (xRange == null)) {
      return (null);
    }
    Pattern retval = new Pattern(xRange.max - xRange.min + 1, yRange.max - yRange.min + 1);
    int yVal = 0;
    for (int i = yRange.min; i <= yRange.max; i++) {
      int xVal = 0;
      for (int j = xRange.min; j <= xRange.max; j++) {
        String val = getValue(j, i);
        if (val != null) {
          retval.fill(xVal, yVal, val);
        }
        xVal++;
      }
      yVal++;
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Standard toString()
  */
  
  public String toString() {
    StringBuffer retval = new StringBuffer();
    MinMax mmY = getMinMaxYForRange(null);
    MinMax mmX = getMinMaxXForRange(null);
    retval.append("X range = ");
    retval.append((mmX != null) ? mmX.min : 0);
    retval.append(" ");
    retval.append((mmX != null) ? mmX.max : 0);
    retval.append(" Y range = ");
    retval.append((mmY != null) ? mmY.min : 0);
    retval.append(" ");
    retval.append((mmY != null) ? mmY.max : 0);
    retval.append("\n");
    
    if ((mmY == null) || (mmX == null)) {
      return (retval.toString());
    }
    
    for (int i = mmY.min; i <= mmY.max; i++) {
      for (int j = mmX.min; j <= mmX.max; j++) {    
        String val = getValue(j, i);
        retval.append((val == null) ? ' ' : val.charAt(val.length() - 1));
      }
      retval.append("\n");
    }
    
    return (retval.toString());
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
  
  private static class RectWithID {
    String id;
    Rectangle rect;
    
    RectWithID(String id, Rectangle rect) {
      this.id = id;
      this.rect = (Rectangle)rect.clone();
    }
    
    RectWithID(RectWithID other) {
      this.id = other.id;
      this.rect = (Rectangle)other.rect.clone();
    }   
  }
}
