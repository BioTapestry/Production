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

package org.systemsbiology.biotapestry.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Collections;
import java.awt.Rectangle;
import java.awt.Point;

import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** Uses the Bottom-Left rectangle packing algorithm (Baker, Coffman, Rivest,
** 1980) to pack rectangles (except we pack rightwards from the
** top left, with a fixed height and a variable width);
*/

public class TopLeftPacker {
  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TopLeftPacker() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Place the rectangles
  */

  public int placeElements(List<PlacementElement> elements, Map<String, Point> pointResults, int padding,
                           BTProgressMonitor monitor)
                             throws AsynchExitRequestException {
    
    //
    // Sort the rectangles by decreasing width.  Build a pattern for each.
    //
    
    HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();
    HashMap<String, PlacementElement> fullSizeElems = new HashMap<String, PlacementElement>();
    SortedSet<PlacementElement> queue = sortElements(elements, patterns, padding, fullSizeElems, true);
    
    //
    // Figure out the maximum height.  Assume we can pack a square; the
    // overflow will be to the right.
    //
    
    int area = getMinimumArea(elements, padding) / (UiUtil.GRID_SIZE_INT * UiUtil.GRID_SIZE_INT);
    int maxHeight = getMaxHeight(elements, padding) / UiUtil.GRID_SIZE_INT;
    int gridHeight = (int)Math.ceil(Math.sqrt(area));
    if (maxHeight > gridHeight) {
      gridHeight = maxHeight;
    }
    
    //
    // Make the grid:
    // 
    
    PatternGrid grid = new PatternGrid();    
    
    //
    // Place elements:
    //
    
    int maxX = Integer.MIN_VALUE;
    Iterator<PlacementElement> qit = queue.iterator();
    while (qit.hasNext()) {
      PlacementElement elem = qit.next();
      Pattern pat = patterns.get(elem.id);
      int maxY = gridHeight - elem.rect.height;
      boolean placed = false; 
      
      for (int x = 0; (x < Integer.MAX_VALUE) && !placed; x++) {
        for (int y = 0; y <= maxY; y++) {
          if (grid.emptyIntersection(pat, x, y)) {
            placed = true;
            grid.place(pat, x, y);
            int topX = x + elem.rect.width;
            if (topX > maxX) {
              maxX = topX;
            }
            Point resPt = new Point((x * UiUtil.GRID_SIZE_INT) + padding,
                                    (y * UiUtil.GRID_SIZE_INT) + padding);
            pointResults.put(elem.id, resPt);
            PlacementElement pe = fullSizeElems.get(elem.id);
            pe.rect.x = resPt.x;
            pe.rect.y = resPt.y;
            break;
          }
          
          if ((monitor != null) && !monitor.keepGoing()) {
            throw new AsynchExitRequestException();
          }          
        }
      }
      if (!placed) {
        throw new IllegalStateException();
      }
    }
    
    //
    // Center the elements:
    //
    
    balanceElements(fullSizeElems, pointResults, padding);    

    return (maxX * UiUtil.GRID_SIZE_INT);
  }
  
  /***************************************************************************
  ** 
  ** Place the rectangles downward
  */

  public int placeElementsDownward(List elements, Map pointResults, int padding,
                                   BTProgressMonitor monitor)
                                     throws AsynchExitRequestException {
    
    //
    // Sort the rectangles by decreasing height.  Build a pattern for each.
    //
    
    HashMap patterns = new HashMap();
    HashMap fullSizeElems = new HashMap();
    SortedSet queue = sortElements(elements, patterns, padding, fullSizeElems, false);
    
    //
    // Figure out the maximum height.  Assume we can pack a square; the
    // overflow will be to the right.
    //
    
    int area = getMinimumArea(elements, padding) / (UiUtil.GRID_SIZE_INT * UiUtil.GRID_SIZE_INT);
    int maxWidth = getMaxWidth(elements, padding) / UiUtil.GRID_SIZE_INT;
    int gridWidth = (int)Math.ceil(Math.sqrt(area));
    if (maxWidth > gridWidth) {
      gridWidth = maxWidth;
    }
    
    //
    // Make the grid:
    // 
    
    PatternGrid grid = new PatternGrid();    
    
    //
    // Place elements:
    //
    
    int maxXAll = Integer.MIN_VALUE;
    Iterator qit = queue.iterator();
    while (qit.hasNext()) {
      PlacementElement elem = (PlacementElement)qit.next();
      Pattern pat = (Pattern)patterns.get(elem.id);
      int maxX = gridWidth - elem.rect.width;
      boolean placed = false; 
      for (int y = 0; (y < Integer.MAX_VALUE) && !placed; y++) {
        for (int x = 0; x <= maxX; x++) {
          if (grid.emptyIntersection(pat, x, y)) {
            placed = true;
            grid.place(pat, x, y);
            int topX = x + elem.rect.width;
            if (topX > maxXAll) {
              maxXAll = topX;
            }
            Point resPt = new Point((x * UiUtil.GRID_SIZE_INT) + padding,
                                    (y * UiUtil.GRID_SIZE_INT) + padding);
            pointResults.put(elem.id, resPt);
            PlacementElement pe = (PlacementElement)fullSizeElems.get(elem.id);
            pe.rect.x = resPt.x;
            pe.rect.y = resPt.y;
            break;
          }
          if ((monitor != null) && !monitor.keepGoing()) {
            throw new AsynchExitRequestException();
          }
        }
      }
      if (!placed) {
        throw new IllegalStateException();
      }
    }
    
    //
    // Center the elements:
    //
    
    //balanceElements(fullSizeElems, pointResults, padding);    

    return (maxXAll * UiUtil.GRID_SIZE_INT);
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
  ** Return the minimum required area
  */

  private int getMinimumArea(List elements, int padding) {
    int totalArea = 0;
    int num = elements.size();
    for (int i = 0; i < num; i++) {
      PlacementElement pe = (PlacementElement)elements.get(i);
      totalArea += ((pe.rect.width + (2 * padding)) * (pe.rect.height + (2 * padding)));
    }
    return (totalArea);
  } 
  
  /***************************************************************************
  ** 
  ** Return the maximum element height
  */

  private int getMaxHeight(List elements, int padding) {
    int max = Integer.MIN_VALUE;
    int num = elements.size();
    for (int i = 0; i < num; i++) {
      PlacementElement pe = (PlacementElement)elements.get(i);
      if ((pe.rect.height + (2 * padding)) > max) {
        max = pe.rect.height + (2 * padding);
      }
    }
    return (max);
  }  
  
  /***************************************************************************
  ** 
  ** Return the maximum element width
  */

  private int getMaxWidth(List elements, int padding) {
    int max = Integer.MIN_VALUE;
    int num = elements.size();
    for (int i = 0; i < num; i++) {
      PlacementElement pe = (PlacementElement)elements.get(i);
      if ((pe.rect.width + (2 * padding)) > max) {
        max = pe.rect.width + (2 * padding);
      }
    }
    return (max);
  }
  
  /***************************************************************************
  ** 
  ** Sort the elements and build patterns
  */

  private SortedSet<PlacementElement> sortElements(List<PlacementElement> elements, Map<String, Pattern> patterns, int padding, 
                                                   Map<String, PlacementElement> fullSizeElems, boolean widthFirst) {
    
    TreeSet<PlacementElement> retval = new TreeSet<PlacementElement>(Collections.reverseOrder());
    
    int num = elements.size();
    for (int i = 0; i < num; i++) {
      PlacementElement elem = elements.get(i);
      Rectangle rectCopy = (Rectangle)elem.rect.clone();
      fullSizeElems.put(elem.id, new PlacementElement(rectCopy, elem.id, widthFirst));
      Rectangle rect = new Rectangle(elem.rect.x / UiUtil.GRID_SIZE_INT - padding / UiUtil.GRID_SIZE_INT, 
                                     elem.rect.y / UiUtil.GRID_SIZE_INT - padding / UiUtil.GRID_SIZE_INT, 
                                     (elem.rect.width / UiUtil.GRID_SIZE_INT) + (2 * padding / UiUtil.GRID_SIZE_INT), 
                                     (elem.rect.height / UiUtil.GRID_SIZE_INT) + (2 * padding / UiUtil.GRID_SIZE_INT));
      retval.add(new PlacementElement(rect, elem.id, widthFirst));      
      Pattern pat = new Pattern(rect.width, rect.height, elem.id);
      if (patterns.get(elem.id) != null) {
        throw new IllegalArgumentException();
      }
      patterns.put(elem.id, pat);
    }
  
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Balance elements about a centerline
  */

  private void balanceElements(Map elementMap, Map pointResults, int padding) {
    
    ArrayList ranges = new ArrayList();
    Iterator ekit = elementMap.keySet().iterator();
    while (ekit.hasNext()) {
      String id = (String)ekit.next();
      PlacementElement pe = (PlacementElement)elementMap.get(id);
      ranges.add(new MinMax(pe.rect.x, pe.rect.x + pe.rect.width));
    }
    SortedSet gaps = findGaps(ranges);
    if (gaps.isEmpty()) {
      return;
    }
    gaps.add(new Integer(Integer.MAX_VALUE));    
    Map bins = binIntoRanges(gaps, elementMap);
    Map shifts = getShiftsForBins(bins);
    shiftBins(bins, shifts, pointResults);
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Find gaps in a series of ranges
  */

  private SortedSet findGaps(List ranges) {
   
    TreeSet gapSet = new TreeSet();
    int num = ranges.size();
    for (int i = 0; i < num; i++) {
      MinMax mm = (MinMax)ranges.get(i);
      for (int j = mm.min; j <= mm.max; j++) {
        gapSet.add(new Integer(j));
      }
    }
    int minAll = ((Integer)gapSet.first()).intValue();
    int maxAll = ((Integer)gapSet.last()).intValue();
    TreeSet retval = new TreeSet();
    for (int i = minAll; i <= maxAll; i++) {
      Integer testInt = new Integer(i);
      if (!gapSet.contains(testInt)) {
        retval.add(testInt);
      }
    }    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Bin elements by gaps
  */

  private Map binIntoRanges(SortedSet gaps, Map elementMap) {
   
    HashMap retval = new HashMap();
    Iterator ekit = elementMap.keySet().iterator();
    while (ekit.hasNext()) {
      String id = (String)ekit.next();
      PlacementElement pe = (PlacementElement)elementMap.get(id);
      Integer lastX = new Integer(Integer.MIN_VALUE);
      Iterator git = gaps.iterator();
      while (git.hasNext()) {
        Integer gapX = (Integer)git.next();
        int testX = gapX.intValue();
        if ((pe.rect.x > lastX.intValue()) && (pe.rect.x < gapX.intValue())) {
          ArrayList bin = (ArrayList)retval.get(lastX);
          if (bin == null) {
            bin = new ArrayList();
            retval.put(lastX, bin);
          }
          bin.add(pe);
        }
        lastX = gapX;
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Get a shift for each bin
  */

  private Map getShiftsForBins(Map bins) {
    
    //
    // Calculate bin heights and maximum bin height:
    //
    
    HashMap sizes = new HashMap();
    Iterator bkit = bins.keySet().iterator();
    int minAll = Integer.MAX_VALUE;
    int maxAll = Integer.MIN_VALUE;
    while (bkit.hasNext()) {
      Integer id = (Integer)bkit.next();
      ArrayList list = (ArrayList)bins.get(id);
      int num = list.size();
      PlacementElement pe = (PlacementElement)list.get(0);
      MinMax range = new MinMax(pe.rect.y, pe.rect.y + pe.rect.height);
      for (int i = 1; i < num; i++) {
        pe = (PlacementElement)list.get(i);
        if (pe.rect.y < range.min) {
          range.min = pe.rect.y;
        }
        if (pe.rect.y + pe.rect.height > range.max) {
          range.max = pe.rect.y + pe.rect.height;
        }
      }
      sizes.put(id, range);
      if (range.min < minAll) {
        minAll = range.min;
      }
      if (range.max > maxAll) {
        maxAll = range.max;
      }
    }
    
    //
    // Now calculate shifts:
    //
    
    int maxSize = maxAll - minAll;
    
    HashMap retval = new HashMap();
    bkit = bins.keySet().iterator();
    while (bkit.hasNext()) {
      Integer id = (Integer)bkit.next();
      MinMax range = (MinMax)sizes.get(id);
      int rangeSize = range.max - range.min;
      int shift = (int)UiUtil.forceToGridValue(((maxSize - rangeSize) / 2) - (range.min - minAll), UiUtil.GRID_SIZE);
      Integer shiftObj = new Integer(shift);
      retval.put(id, shiftObj);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Shift each bin
  */

  private void shiftBins(Map bins, Map shifts, Map pointResults) {
    
    Iterator bkit = bins.keySet().iterator();
    while (bkit.hasNext()) {
      Integer id = (Integer)bkit.next();
      ArrayList list = (ArrayList)bins.get(id);
      Integer shift = (Integer)shifts.get(id);
      int shiftY = shift.intValue();
      int num = list.size();
      for (int i = 0; i < num; i++) {
        PlacementElement pe = (PlacementElement)list.get(i);
        Point result = (Point)pointResults.get(pe.id);
        Point newPoint = new Point(result.x, result.y + shiftY);
        pointResults.put(pe.id, newPoint);
      }
    }
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

}
