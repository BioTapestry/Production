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

package org.systemsbiology.biotapestry.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/****************************************************************************
**
** Class for slicing up rectangles by other rectangles 
*/

public class SlicedRectangle {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public enum MinMaxOpt {MIN_X, MIN_Y, MAX_X, MAX_Y};
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
      
  private int minX;
  private int minY;
  private int maxX;
  private int maxY;    
    
  private int[] xCuts;
  private int[] yCuts;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public SlicedRectangle(Rectangle2D toSlice, Rectangle2D interRect) {
    //
    // These guys are on the grid, so ints can be used:
    //
    MinMax targX = new MinMax((int)toSlice.getMinX(), (int)toSlice.getMaxX());
    MinMax interX = new MinMax((int)interRect.getMinX(), (int)interRect.getMaxX());
    List<MinMax> sliceX = sliceDim(targX, interX);
    
    MinMax targY = new MinMax((int)toSlice.getMinY(), (int)toSlice.getMaxY());
    MinMax interY = new MinMax((int)interRect.getMinY(), (int)interRect.getMaxY());
    List<MinMax> sliceY = sliceDim(targY, interY);
    
    int numX = sliceX.size();
    int numY = sliceY.size();
    xCuts = new int[numX + 1];
    yCuts = new int[numY + 1];      
    
    for (int x = 0; x < numX; x++) {
      MinMax xCut = (MinMax)sliceX.get(x);
      xCuts[x] = xCut.min;
      if (x == (numX - 1)) {
        xCuts[numX] = xCut.max;
      }
    }
    for (int y = 0; y < numY; y++) {
      MinMax yCut = (MinMax)sliceY.get(y);
      yCuts[y] = yCut.min;
      if (y == (numY - 1)) {
        yCuts[numY] = yCut.max;
      }
    }
    minX = interX.min;
    minY = interY.min;
    maxX = interX.max;
    maxY = interY.max;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return a list of all possible slice plan tuples
  */    
    
  public List<MinMaxOpt> getFirstSliceOptions() {
    ArrayList<MinMaxOpt> options = new ArrayList<MinMaxOpt>();
    if (xCuts.length > 2) {
      if (minX != xCuts[0]) {
        options.add(MinMaxOpt.MIN_X);
      }
      if (maxX != xCuts[xCuts.length - 1]) {
        options.add(MinMaxOpt.MAX_X);
      }
    }
    if (yCuts.length > 2) {
      if (minY != yCuts[0]) {
        options.add(MinMaxOpt.MIN_Y);
      }
      if (maxY != yCuts[yCuts.length - 1]) {
        options.add(MinMaxOpt.MAX_Y);
      }
    }
    return (options);
  }   
  
  /***************************************************************************
  **
  ** Return a list of all possible slice plan tuples
  */    
    
  public List<SlicePlanTuple> getFullSlicePlans() {
    List<MinMaxOpt> options = getFirstSliceOptions();
    Collections.sort(options);
    ArrayList<SlicePlanTuple> retval = new ArrayList<SlicePlanTuple>();
    ArrayList<List<MinMaxOpt>> perms = permute(options);
    int numPerm = perms.size();
    for (int i = 0; i < numPerm; i++) {
      retval.add(new SlicePlanTuple(perms.get(i)));
    }    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return a list of all minimal rectangles created by the slicing
  */    
    
  public List<Rectangle2D> toList() {
    ArrayList<Rectangle2D> retval = new ArrayList<Rectangle2D>();
    int numX = xCuts.length - 1;      
    for (int x = 0; x < numX; x++) {
      int numY = yCuts.length - 1;
      for (int y = 0; y < numY; y++) {
        int minx4r = xCuts[x];
        int miny4r = yCuts[y];
        Rectangle2D nextRect = new Rectangle2D.Double(minx4r, miny4r, xCuts[x + 1] - minx4r, yCuts[y + 1] - miny4r);
        retval.add(nextRect);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Slice along the given axis.
  */    
    
  public SlicedRectangleSplit slice(MinMaxOpt which) {
    Rectangle2D first;
    Rectangle2D second;
    switch (which) {
      case MIN_X:
        first = new Rectangle2D.Double(xCuts[0], yCuts[0], minX - xCuts[0], yCuts[yCuts.length - 1] - yCuts[0]);
        second = new Rectangle2D.Double(minX, yCuts[0], xCuts[xCuts.length - 1] - minX, yCuts[yCuts.length - 1] - yCuts[0]);
        break;
      case MIN_Y:
        first = new Rectangle2D.Double(xCuts[0], yCuts[0], xCuts[xCuts.length - 1] - xCuts[0], minY - yCuts[0]);
        second = new Rectangle2D.Double(xCuts[0], minY, xCuts[xCuts.length - 1] - xCuts[0], yCuts[yCuts.length - 1] - minY);
        break;
      case MAX_X:
        first = new Rectangle2D.Double(xCuts[0], yCuts[0], maxX - xCuts[0], yCuts[yCuts.length - 1] - yCuts[0]);
        second = new Rectangle2D.Double(maxX, yCuts[0], xCuts[xCuts.length - 1] - maxX, yCuts[yCuts.length - 1] - yCuts[0]);
        break;
      case MAX_Y:
        first = new Rectangle2D.Double(xCuts[0], yCuts[0], xCuts[xCuts.length - 1] - xCuts[0], maxY - yCuts[0]);
        second = new Rectangle2D.Double(xCuts[0], maxY, xCuts[xCuts.length - 1] - xCuts[0], yCuts[yCuts.length - 1] - maxY);
        break;
      default:
        throw new IllegalArgumentException();
    }
    return (new SlicedRectangleSplit(first, second));
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Crappy way to permute.  Returns a list of lists.
  */
  
  private static ArrayList<List<MinMaxOpt>> permute(List<MinMaxOpt> remainingVals) {
    ArrayList<List<MinMaxOpt>> retval = new ArrayList<List<MinMaxOpt>>();
    int numVals = remainingVals.size();
    if (numVals == 1) {
      retval.add(remainingVals);
      return (retval);
    }
    for (int i = 0; i < numVals; i++) {
      ArrayList<MinMaxOpt> minusOne = new ArrayList<MinMaxOpt>(remainingVals);
      MinMaxOpt takeOut = minusOne.remove(i);
      ArrayList<List<MinMaxOpt>> theRest = permute(minusOne);
      int numRest = theRest.size();
      for (int j = 0; j < numRest; j++) {
        List<MinMaxOpt> nextRest = theRest.get(j);
        nextRest.add(0, takeOut);
        retval.add(nextRest);
      }     
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Slice a dimension.  May return one, two, or three MinMax vals to indicate
  ** how to slice the dimension
  */
  
  private static List<MinMax> sliceDim(MinMax sliceBounds, MinMax interBounds) {
    ArrayList<MinMax> retval = new ArrayList<MinMax>();
    MinMax.SetRel sliceEval = sliceBounds.evaluate(interBounds);
    switch (sliceEval) {  
      case EQUALS:
      case IS_DISJOINT:
      case IS_PROPER_SUPERSET:
        retval.add(sliceBounds.clone());
        return (retval);
      case IS_PROPER_SUBSET:
        boolean equalMin = (interBounds.min == sliceBounds.min);
        boolean equalMax = (interBounds.max == sliceBounds.max);
        if (equalMin && equalMax) {
          throw new IllegalStateException();
        }
        if (equalMin) {
          retval.add(new MinMax(interBounds.min, interBounds.max));
          retval.add(new MinMax(interBounds.max, sliceBounds.max));
        } else if (equalMax) {
          retval.add(new MinMax(sliceBounds.min, interBounds.min));
          retval.add(new MinMax(interBounds.min, interBounds.max));
        } else {
          retval.add(new MinMax(sliceBounds.min, interBounds.min));
          retval.add(new MinMax(interBounds.min, interBounds.max));
          retval.add(new MinMax(interBounds.max, sliceBounds.max));          
        }
        return (retval);
      case INTERSECTS:
        boolean interLower = (interBounds.min < sliceBounds.min);
        if (interLower) {
          retval.add(new MinMax(interBounds.min, sliceBounds.min));
          retval.add(new MinMax(sliceBounds.min, interBounds.max));
          retval.add(new MinMax(interBounds.max, sliceBounds.max));
        } else {
          retval.add(new MinMax(sliceBounds.min, interBounds.min));
          retval.add(new MinMax(interBounds.min, sliceBounds.max));
          retval.add(new MinMax(sliceBounds.max, interBounds.max));          
        }
        return (retval);
      default:
        throw new IllegalStateException();
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
    
    Rectangle2D toSlice = new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0);
    Rectangle2D interRect = new Rectangle2D.Double(60.0, 40.0, 30.0, 20.0);
    SlicedRectangle sr = new SlicedRectangle(toSlice, interRect);
    List<MinMaxOpt> options = sr.getFirstSliceOptions();
    int numOpt = options.size();
    for (int i = 0; i < numOpt; i++) {
      MinMaxOpt opt = options.get(i);
      SlicedRectangleSplit srs = sr.slice(opt);
      System.out.println("first " + srs.first);
      System.out.println("second " + srs.second);
    }
    return;
  }  
   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static class SlicePlanTuple {
    List<MinMaxOpt> plan_;
    
    SlicePlanTuple(List<MinMaxOpt> plan) {
      plan_ = plan;
    }
    
    public Iterator<MinMaxOpt> getPlan() {
      return (plan_.iterator());
    }
  }
  
 public static class SlicedRectangleSplit {
    public Rectangle2D first;
    public Rectangle2D second;
    
    SlicedRectangleSplit(Rectangle2D first, Rectangle2D second) {
      this.first = first;
      this.second = second;
    }
  }   
}
