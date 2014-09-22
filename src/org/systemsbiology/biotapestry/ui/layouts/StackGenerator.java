/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Build a stack order from existing layout
*/

public class StackGenerator {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
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
        
  public StackGenerator() {
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build a stacked layout order from existing layout
  */
  
  public SortedMap<Integer, List<String>> buildStackOrder(Set<String> gascCores, DataAccessContext icx) {    
    //
    // Get bounds of all the GASC cores and track row gaps:
    //
    TreeMap<Integer, Integer> rowUsage = new TreeMap<Integer, Integer>();    
    HashMap<String, Rectangle2D> coreBounds = new HashMap<String, Rectangle2D>();
    Iterator<String> gcit = gascCores.iterator();
    int maxY = Integer.MIN_VALUE;
    Genome genome = icx.getGenome();
    while (gcit.hasNext()) {
      String gascCore = gcit.next(); 
      NodeProperties np = icx.getLayout().getNodeProperties(gascCore);
      INodeRenderer rend = np.getRenderer();
      Node node = genome.getNode(gascCore);
      Rectangle2D currRect = rend.getBounds(node, icx, null);
      coreBounds.put(gascCore, currRect);
      maxY = doRowAccounting(currRect, rowUsage, maxY);
    }
    
    //
    // Find the "holes" in the row usage.  If no zeros, we return null!
    // Actually, just make it one big row!
    //
    
    SortedSet<Integer> holes = findHoles(rowUsage, maxY);
    if (holes == null) {
      return (null);
    }
    
    SortedMap<Integer, List<String>> rows = buildRows(holes, coreBounds);
       
    SortedMap<Integer, List<String>> retval = sortRows(rows, coreBounds);
    return (retval);
  }  
  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Record row usage
  */
  
  private int doRowAccounting(Rectangle2D currRect, SortedMap<Integer, Integer> rowUsage, int maxY) {
    int startY = ((int)UiUtil.forceToGridValueMin(currRect.getMinY(), UiUtil.GRID_SIZE)) / UiUtil.GRID_SIZE_INT;
    int endY =  ((int)UiUtil.forceToGridValueMax(currRect.getMaxY(), UiUtil.GRID_SIZE)) / UiUtil.GRID_SIZE_INT;
    for (int i = startY; i <= endY; i++) {
      Integer rowKey = new Integer(i);
      Integer count = rowUsage.get(rowKey);
      if (count == null) {
        count = new Integer(1);
      } else {
        count = new Integer(count.intValue() + 1);
      }
      rowUsage.put(rowKey, count);
    }
    return ((endY > maxY) ? endY : maxY);
  }
  
  /***************************************************************************
  **
  ** Find empty rows
  */
  
  private SortedSet<Integer> findHoles(SortedMap<Integer, Integer> rowUsage, int maxY) {
    TreeSet<Integer> retval = new TreeSet<Integer>();
    boolean first = true;
    int counter = 0;
    Iterator<Integer> ruit = rowUsage.keySet().iterator();
    while (ruit.hasNext()) {
      Integer row = ruit.next();
      int rowVal = row.intValue();
      if (first) {
        counter = rowVal;
        first = false;
        continue;
      }
      counter++;
      if (rowVal != counter) {
        retval.add(new Integer(rowVal - 1));
        counter = rowVal;
      }
    }
    //if (retval.isEmpty()) {
    //  return (null);
    //} else {
    retval.add(new Integer(maxY));      
    //}
        
    return (retval); //retval.isEmpty() ? null : retval);
  }
  
  /***************************************************************************
  **
  ** Bin cores into rows
  */
  
  private SortedMap<Integer, List<String>> buildRows(Set<Integer> holes, Map<String, Rectangle2D> coreBounds) {
    TreeMap<Integer, List<String>> retval = new TreeMap<Integer, List<String>>();
    
    Iterator<String> cit = coreBounds.keySet().iterator();
    while (cit.hasNext()) {
      String coreID = cit.next();
      Rectangle2D bounds = coreBounds.get(coreID);
      int coreY = ((int)UiUtil.forceToGridValueMin(bounds.getMinY(), UiUtil.GRID_SIZE)) / UiUtil.GRID_SIZE_INT;
      Iterator<Integer> hit = holes.iterator();
      while (hit.hasNext()) {
        Integer row = hit.next();
        if (coreY < row.intValue()) {
         List<String> perRow = retval.get(row);
          if (perRow == null) {
            perRow = new ArrayList<String>();
            retval.put(row, perRow);
          }
          perRow.add(coreID);  
          break;
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Sort rows by X
  */
  
  private SortedMap<Integer, List<String>> sortRows(SortedMap<Integer, List<String>> rows, Map<String, Rectangle2D> coreBounds) {
    TreeMap<Integer, SortedMap<Integer, SortedSet<String>>> holder = new TreeMap<Integer, SortedMap<Integer, SortedSet<String>>>();
    
    int rowCount = 0;
    Iterator<Integer> rit = rows.keySet().iterator();
    while (rit.hasNext()) {
      Integer rowVal = rit.next();
      SortedMap<Integer, SortedSet<String>> sorted = new TreeMap<Integer, SortedSet<String>>();
      holder.put(new Integer(rowCount++), sorted);
      List<String> perRow = rows.get(rowVal);
      Iterator<String> prit = perRow.iterator();
      while (prit.hasNext()) {
        String coreID = prit.next();
        Rectangle2D bounds = coreBounds.get(coreID);
        int coreX = ((int)UiUtil.forceToGridValueMin(bounds.getMinX(), UiUtil.GRID_SIZE)) / UiUtil.GRID_SIZE_INT;
        Integer coreXObj = new Integer(coreX);
        SortedSet<String> perPos = sorted.get(coreXObj);
        if (perPos == null) {
          perPos = new TreeSet<String>();
          sorted.put(coreXObj, perPos);
        }
        perPos.add(coreID);
      }
    }
    
    TreeMap<Integer, List<String>> retval = new TreeMap<Integer, List<String>>();
    Iterator<Integer> hit = holder.keySet().iterator();
    while (hit.hasNext()) {
      Integer rowVal = hit.next();
      List<String> perRow = new ArrayList<String>();
      retval.put(rowVal, perRow);
      SortedMap<Integer, SortedSet<String>> sorted = holder.get(rowVal);
      Iterator<SortedSet<String>> svit = sorted.values().iterator();
      while (svit.hasNext()) {
        SortedSet<String> ppSet = svit.next();
        perRow.addAll(ppSet);
      }
    }
    return (retval);
  }
} 
