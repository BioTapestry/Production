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

import java.util.Set;
import java.util.HashSet;

/****************************************************************************
**
** A Class
*/

public class Pattern {
  
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
  
  private String[][] pattern_;
  private int height_;
  private int width_;
  private String val_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public Pattern(int w, int h) {
    if ((w <= 0) || (h <= 0)) {
      throw new IllegalArgumentException();
    }
    pattern_ = new String[w][h];
  }  
  
  
  /***************************************************************************
  **
  ** Constructor for huge, uniform patterns
  */

  public Pattern(int w, int h, String val) {
    if ((w <= 0) || (h <= 0) || (val == null)) {
      throw new IllegalArgumentException();
    }
    height_ = h;
    width_ = w;
    val_ = val;
  }    
 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  /***************************************************************************
  **
  ** Answer if the pattern is a uniform solid box
  */
  
  public boolean isFilledBox() {
    if (pattern_ == null) {
      return (true);
    }

    int height = getHeight();
    int width = getWidth();    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) { 
        if (pattern_[x][y] == null) {
          return (false);
        }
      }
    }
    if (getValues().size() > 1) {
      return (false);
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Get the pattern height
  */
  
  public int getHeight() {
    return ((pattern_ != null) ? pattern_[0].length : height_);
  }
  
  /***************************************************************************
  **
  ** Get the pattern width
  */
  
  public int getWidth() {
    return ((pattern_ != null) ? pattern_.length : width_);
  }
  
  /***************************************************************************
  **
  ** Get the pattern height min and max
  */
  
  public MinMax getHeightRange() {
    int height = getHeight();

    if (pattern_ == null) {
      return (new MinMax(0, height - 1));      
    }

    int width = getWidth();
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
       
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {      
        if (pattern_[x][y] != null) {
          if (y < minValue) {
            minValue = y;
          }
          if (y > maxValue) {
            maxValue = y;
          }
        }
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
  ** Get the pattern width min and max
  */
  
  public MinMax getWidthRange() {    
    int width = getWidth();    
    if (pattern_ == null) {
      return (new MinMax(0, width - 1));      
    }    
    
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    int height = getHeight();

    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {      
        if (pattern_[x][y] != null) {
          if (x < minValue) {
            minValue = x;
          }
          if (x > maxValue) {
            maxValue = x;
          }
        }
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
  ** Fill in the given box
  */
  
  public void fill(int x, int y, String val) {
    if (pattern_ == null) {
      throw new IllegalStateException();
    }
    pattern_[x][y] = val;
    return;
  }
  
  /***************************************************************************
  **
  ** Fill the whole pattern
  */
  
  public void fillAll(String val) {
    if (pattern_ == null) {
      if (val == null) {
        throw new IllegalArgumentException();
      }
      val_ = val;
      return;
    }
   
    int height = getHeight();
    int width = getWidth();
    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        pattern_[x][y] = val;
      }
    }    
    return;
  }

  
  /***************************************************************************
  **
  ** Get the value
  */
  
  public String getValue(int x, int y) {
    if (pattern_ == null) {
      if ((x < 0) || (y < 0) || (x >= width_) || (y >= height_)) {
        throw new IllegalArgumentException();
      }
      return (val_);
    }
    return (pattern_[x][y]);
  }  

  /***************************************************************************
  **
  ** Get the set of contents
  */
  
  public Set<String> getValues() {
    HashSet<String> retval = new HashSet<String>();
    if (pattern_ == null) {
      if (val_ == null) {
        throw new IllegalStateException();
      }
      retval.add(val_);
    }
 
    int height = getHeight();
    int width = getWidth();
    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        String val = pattern_[x][y];
        if (val != null) {
          retval.add(val);
        }
      }
    }    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Standard toString()
  */
  
  public String toString() {
    StringBuffer retval = new StringBuffer();
    int height = getHeight();
    int width = getWidth();
    
 
    for (int y = 0; y < height; y++) {   
      for (int x = 0; x < width; x++) {  
        String val = (pattern_ == null)? val_ : pattern_[x][y];
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
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
