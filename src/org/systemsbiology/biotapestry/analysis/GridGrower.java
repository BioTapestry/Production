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

package org.systemsbiology.biotapestry.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.HashSet;
import java.awt.geom.Rectangle2D;

import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A class for growing an interlocking grid of rectangles
*/

public class GridGrower {
  
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

  /***************************************************************************
  **
  ** Growth Types
  */ 
   
  public enum GrowthTypes {FULL_GROWTH, MIN_GROWTH_PINNED_ORIGIN, MIN_GROWTH_CENTERED};
  
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

  public GridGrower() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Grow the grid
  */

  public List<GeneralizedGridElement> growGrid(List<GeneralizedGridElement> gridElements, GrowthTypes mode) {
    //
    // Make sure ids are unique, while sorting the grid elements and creating a
    // working map:
    //
    
    HashMap<Object, Rectangle2D> workingMap = new HashMap<Object, Rectangle2D>();
    TreeSet<GridElement> elems = new TreeSet<GridElement>();
    HashSet<Object> seen = new HashSet<Object>();
    int geSize = gridElements.size();
    for (int i = 0; i < geSize; i++) {
      GeneralizedGridElement gge = gridElements.get(i);
      if (gge instanceof CenteredGridElement) {    
        CenteredGridElement cge = (CenteredGridElement)gge;
        if (seen.contains(cge.getID())) {
          throw new IllegalArgumentException();
        }
        seen.add(cge.getID());
        elems.add(cge.minGridElement);
        elems.add(cge.maxGridElement);
        workingMap.put(cge.minGridElement.id, (Rectangle2D)cge.minGridElement.rect.clone());
        workingMap.put(cge.maxGridElement.id, (Rectangle2D)cge.maxGridElement.rect.clone());
      } else { 
        GridElement ge = (GridElement)gge;
        if (seen.contains(ge.id)) {
          throw new IllegalArgumentException();
        } 
        seen.add(ge.id);
        elems.add(ge);
        workingMap.put(ge.id, (Rectangle2D)ge.rect.clone());
      }
    }
    
    Iterator<GridElement> eit = elems.iterator();
    while (eit.hasNext()) {
      GridElement elem = eit.next();
      growX(elem, workingMap);
      growY(elem, workingMap);
    }
    
    ArrayList<GeneralizedGridElement> retval = new ArrayList<GeneralizedGridElement>();
    for (int i = 0; i < geSize; i++) {
      GeneralizedGridElement gge = (GeneralizedGridElement)gridElements.get(i);
      if (gge instanceof CenteredGridElement) {    
        CenteredGridElement cge = (CenteredGridElement)gge;
        Rectangle2D currTargi = (Rectangle2D)workingMap.get(cge.minGridElement.id);
        GridElement gei = buildNewElement(cge.minGridElement, currTargi, mode);
        Rectangle2D currTarga = (Rectangle2D)workingMap.get(cge.maxGridElement.id);
        GridElement gea = buildNewElement(cge.maxGridElement, currTarga, mode);
        retval.add(new CenteredGridElement(gei, gea));
      } else { 
        GridElement ge = (GridElement)gge;
        Rectangle2D currTarg = (Rectangle2D)workingMap.get(ge.id);
        retval.add(buildNewElement(ge, currTarg, mode)); 
      }
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
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Return the remaining growth needed in X:
  */

  private double remainingXGrowth(GridElement orig, Rectangle2D current) {
    double target = (orig.rect.getWidth() + orig.deltaX) - current.getWidth();
    return ((target > 0.0) ? target : 0.0);
  }  

  /***************************************************************************
  ** 
  ** Return the remaining growth needed in Y:
  */

  private double remainingYGrowth(GridElement orig, Rectangle2D current) {
    double target = (orig.rect.getHeight() + orig.deltaY) - current.getHeight();
    return ((target > 0.0) ? target : 0.0);
  }  
  
  /***************************************************************************
  ** 
  ** Grow the grid as needed to accomodate the larger element:
  */

  private void growX(GridElement growElement, Map<Object, Rectangle2D> workingElements) {
    //
    // Compare the grow element to the current state:
    //
    
    Object targID = growElement.id;
    Rectangle2D workingTarget = (Rectangle2D)workingElements.get(targID);
    double remaining = remainingXGrowth(growElement, workingTarget);
    if (remaining == 0.0) {
      return;
    }
    
    //
    // Shove every coordinate larger than the rightmost boundary of the
    // growing element outwards by the remaining amount.  Any coordinate
    // between the left and right boundaries of the rectangle get
    // moved by a prorated amount:
    //
    
    double minX = workingTarget.getX();
    double deltaX = workingTarget.getWidth();
    double maxX = minX + deltaX; 
    
    Iterator<Object> wekit = workingElements.keySet().iterator();
    while (wekit.hasNext()) {
      Object key = wekit.next();
      Rectangle2D currTarg = workingElements.get(key);
      double ctMinX = currTarg.getX();
      double ctMaxX = ctMinX + currTarg.getWidth();
      double newMin;
      double newMax;
      if (ctMinX <= minX) {
        newMin = ctMinX;
      } else if (ctMinX >= maxX) {
        newMin = ctMinX + remaining;
      } else {
        double ratio = (ctMinX - minX) / deltaX;
        newMin = ctMinX + (ratio * remaining);
      }
      UiUtil.forceToGridValue(newMin, UiUtil.GRID_SIZE);
      if (ctMaxX <= minX) {
        newMax = ctMaxX;
      } else if (ctMaxX >= maxX) {
        newMax = ctMaxX + remaining;
      } else {
        double ratio = (ctMaxX - minX) / deltaX;
        newMax = ctMaxX + (ratio * remaining);
      }
      UiUtil.forceToGridValue(newMax, UiUtil.GRID_SIZE);
      double newWidth = newMax - newMin;
      currTarg.setRect(newMin, currTarg.getY(), newWidth, currTarg.getHeight());
    }
    return;
  }
    
    
  /***************************************************************************
  ** 
  ** Grow the grid as needed to accomodate the larger element:
  */

  private void growY(GridElement growElement, Map<Object, Rectangle2D> workingElements) {
    //
    // Compare the grow element to the current state:
    //
    
    Object targID = growElement.id;
    Rectangle2D workingTarget = workingElements.get(targID);
    double remaining = remainingYGrowth(growElement, workingTarget);
    if (remaining == 0.0) {
      return;
    }
    
    //
    // Shove every coordinate larger than the rightmost boundary of the
    // growing element outwards by the remaining amount.  Any coordinate
    // between the left and right boundaries of the rectangle get
    // moved by a prorated amount:
    //
    
    double minY = workingTarget.getY();
    double deltaY = workingTarget.getHeight();
    double maxY = minY + deltaY; 
    
    Iterator<Object> wekit = workingElements.keySet().iterator();
    while (wekit.hasNext()) {
      Object key = wekit.next();
      Rectangle2D currTarg = workingElements.get(key);
      double ctMinY = currTarg.getY();
      double ctMaxY = ctMinY + currTarg.getHeight();
      double newMin;
      double newMax;
      if (ctMinY <= minY) {
        newMin = ctMinY;
      } else if (ctMinY >= maxY) {
        newMin = ctMinY + remaining;
      } else {
        double ratio = (ctMinY - minY) / deltaY;
        newMin = ctMinY + (ratio * remaining);
      }
      UiUtil.forceToGridValue(newMin, UiUtil.GRID_SIZE);      
      if (ctMaxY <= minY) {
        newMax = ctMaxY;
      } else if (ctMaxY >= maxY) {
        newMax = ctMaxY + remaining;
      } else {
        double ratio = (ctMaxY - minY) / deltaY;
        newMax = ctMaxY + (ratio * remaining);
      }
      UiUtil.forceToGridValue(newMax, UiUtil.GRID_SIZE);      
      double newHeight = newMax - newMin;
      currTarg.setRect(currTarg.getX(), newMin, currTarg.getWidth(), newHeight);
    }    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Answer if all targets are in lower layers
  */

  private GridElement buildNewElement(GridElement existing, Rectangle2D newRect, GrowthTypes mode) {  
    switch (mode) {
      case FULL_GROWTH:
        break;
      case MIN_GROWTH_PINNED_ORIGIN:
        newRect.setRect(newRect.getX(), newRect.getY(), 
                        existing.rect.getWidth() + existing.deltaX, 
                        existing.rect.getHeight() + existing.deltaY);
        break;
      case MIN_GROWTH_CENTERED:
        double centerX = newRect.getCenterX();
        double centerY = newRect.getCenterY();
        double originX = centerX - (existing.rect.getWidth() + existing.deltaX) / 2.0;
        double originY = centerY - (existing.rect.getHeight() + existing.deltaY) / 2.0;
        newRect.setRect(originX, originY, 
                        existing.rect.getWidth() + existing.deltaX, 
                        existing.rect.getHeight() + existing.deltaY);
        UiUtil.force2DToGrid(newRect, UiUtil.GRID_SIZE);
        break;
    default:
        throw new IllegalArgumentException();
    }
      
    double actualX = newRect.getWidth() - existing.rect.getWidth();
    double actualY = newRect.getHeight() - existing.rect.getHeight();         
    GridElement newElem = new GridElement(newRect, existing.id, actualX, actualY);
    return (newElem);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER INTERFACES
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public interface GeneralizedGridElement  {
    
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
