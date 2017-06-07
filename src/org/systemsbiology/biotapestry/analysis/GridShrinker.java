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

package org.systemsbiology.biotapestry.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.HashSet;
import java.awt.geom.Rectangle2D;

import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A class for shrinking an interlocking grid of rectangles
*/

public class GridShrinker {
  
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
 
  public enum Modes {GAPS_ONLY};
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GridShrinker() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Shrink the grid
  */

  @SuppressWarnings("unused")
  public List<GridElement> shrinkGrid(List<GridElement> gridElements, Modes mode) {
    //
    // Make sure ids are unique, while sorting the grid elements and creating a
    // working map:
    //
    
    HashMap<Object, Rectangle2D> workingMap = new HashMap<Object, Rectangle2D>();
    HashSet<Object> seen = new HashSet<Object>();
    int geSize = gridElements.size();
    for (int i = 0; i < geSize; i++) {
      GridElement ge = gridElements.get(i);
      if (seen.contains(ge.id)) {
        throw new IllegalArgumentException();
      }
      seen.add(ge.id);
      workingMap.put(ge.id, new Rectangle2D.Double(ge.rect.getX(), ge.rect.getY(), 
                                                   ge.rect.getWidth() + ge.deltaX, 
                                                   ge.rect.getHeight() + ge.deltaY)); 
    }
    
    SortedSet<Integer> emptyX = findEmptyX(workingMap);
    SortedSet<Integer> emptyY = findEmptyY(workingMap);

    ArrayList<GridElement> retval = new ArrayList<GridElement>();
    for (int i = 0; i < geSize; i++) {
      GridElement ge = gridElements.get(i);
      Rectangle2D currTarg = workingMap.get(ge.id);
      retval.add(buildNewElement(ge, currTarg, emptyX, emptyY)); 
    }    
    
    return (retval);
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
  
  /***************************************************************************
  ** 
  ** Find the empty X values
  */

  private SortedSet<Integer> findEmptyX(Map<Object, Rectangle2D> workingElements) {

    TreeSet<Integer> occupied = new TreeSet<Integer>();
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int gridStep = (int)UiUtil.GRID_SIZE;    
    
    Iterator<Object> wekit = workingElements.keySet().iterator();
    while (wekit.hasNext()) {
      Object key = wekit.next();
      Rectangle2D currTarg = workingElements.get(key);
      double ctMinX = UiUtil.forceToGridValueMax(currTarg.getX(), UiUtil.GRID_SIZE);
      double ctMaxX = UiUtil.forceToGridValueMax(currTarg.getX() + currTarg.getWidth(), UiUtil.GRID_SIZE);
      if (ctMinX > ctMaxX) {
        throw new IllegalArgumentException();
      }
      int lowBound = (int)ctMinX;
      int topBound = (int)ctMaxX;

      if (lowBound < minX) {
        minX = lowBound;
      }
      if (topBound > maxX) {
        maxX = topBound;
      }      
      for (int i = lowBound; i <= topBound; i += gridStep) {
        occupied.add(new Integer(i));
      }
    }

    //
    // Now step from min to max and build up the list of missing (unoccupied) x values
    //
    
    TreeSet<Integer> retval = new TreeSet<Integer>();
    if ((minX != Integer.MAX_VALUE) && (maxX != Integer.MIN_VALUE)) {
      for (int i = minX; i <= maxX; i += gridStep) {
        Integer intval = new Integer(i);
        if (!occupied.contains(intval)) {
          retval.add(intval);
        }
      }
    }
    return (retval);
  }
    
  /***************************************************************************
  ** 
  ** Find the empty Y values
  */

  private SortedSet<Integer> findEmptyY(Map<Object, Rectangle2D> workingElements) {

    TreeSet<Integer> occupied = new TreeSet<Integer>();
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    int gridStep = (int)UiUtil.GRID_SIZE;    
    
    Iterator<Object> wekit = workingElements.keySet().iterator();
    while (wekit.hasNext()) {
      Object key = wekit.next();
      Rectangle2D currTarg = workingElements.get(key);
      double ctMinY = UiUtil.forceToGridValueMax(currTarg.getY(), UiUtil.GRID_SIZE);
      double ctMaxY = UiUtil.forceToGridValueMax(currTarg.getY() + currTarg.getHeight(), UiUtil.GRID_SIZE);
      if (ctMinY > ctMaxY) {
        throw new IllegalArgumentException();
      }
      int lowBound = (int)ctMinY;
      int topBound = (int)ctMaxY;

      if (lowBound < minY) {
        minY = lowBound;
      }
      if (topBound > maxY) {
        maxY = topBound;
      }      
      for (int i = lowBound; i <= topBound; i += gridStep) {
        occupied.add(new Integer(i));
      }
    }

    //
    // Now step from min to max and build up the list of missing (unoccupied) Y values
    //
    
    TreeSet<Integer> retval = new TreeSet<Integer>();
    if ((minY != Integer.MAX_VALUE) && (maxY != Integer.MIN_VALUE)) {
      for (int i = minY; i <= maxY; i += gridStep) {
        Integer intval = new Integer(i);
        if (!occupied.contains(intval)) {
          retval.add(intval);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Build a new element
  */

  private GridElement buildNewElement(GridElement existing, Rectangle2D newRect,
                                      SortedSet<Integer> emptyX, SortedSet<Integer> emptyY) {  


    //
    // Figure out the empty rows (cols) less than the start, and move the rectangle
    // up (left) by that amount.
    //
    
    double cornerX = newRect.getX();
    double cornerY = newRect.getY();

    Iterator<Integer> rit = emptyY.iterator();
    double rowDelta = 0.0;
    while (rit.hasNext()) {
      Integer row = rit.next();
      if (row.intValue() < cornerY) {
        rowDelta += UiUtil.GRID_SIZE;
      }
    }
    
    Iterator<Integer> cit = emptyX.iterator();
    double colDelta = 0.0;
    while (cit.hasNext()) {
      Integer col = cit.next();
      if (col.intValue() < cornerX) {
        colDelta += UiUtil.GRID_SIZE;
      }
    }
    
    double newX = cornerX - colDelta;
    double newY = cornerY - rowDelta;

    newRect.setRect(newX, newY, newRect.getWidth(), existing.rect.getHeight());
    UiUtil.force2DToGrid(newRect, UiUtil.GRID_SIZE);
    GridElement newElem = new GridElement(newRect, existing.id, 0.0, 0.0);
    return (newElem);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

}
