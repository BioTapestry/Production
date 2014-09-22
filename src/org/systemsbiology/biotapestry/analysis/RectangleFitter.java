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
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.awt.Rectangle;
import java.awt.Point;

import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Given a collection of fixed linked rectangles, determine locations
** of other floating rectangles.
*/

public class RectangleFitter {
  
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

  public RectangleFitter() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Place the elements.  Placement elements in the floatingElements list
  ** are modified to new coordinates
  */

  public void placeElements(List fixedElements, List floatingElements, List links, 
                            int boundary, BTProgressMonitor monitor)  
                              throws AsynchExitRequestException {
    // FIX ME!!! Pay attention to boundary (not used)

    HashMap allElements = new HashMap();
    HashSet currFixed = new HashSet();
    HashSet currFloating = new HashSet();
    int numFix = fixedElements.size();
    for (int i = 0; i < numFix; i++) {
      PlacementElement pe = (PlacementElement)fixedElements.get(i);
      allElements.put(pe.id, pe);
      currFixed.add(pe.id);
    }
    int numFloat = floatingElements.size();
    for (int i = 0; i < numFloat; i++) {
      PlacementElement pe = (PlacementElement)floatingElements.get(i);
      allElements.put(pe.id, pe);
      currFloating.add(pe.id);
    }     

    int yBoundary = (currFixed.isEmpty()) ? 0 : getFixedBottom(currFixed, allElements); 
 
    //
    // Come up with a topo sort of all the elements. 
    //

    SortedMap sortedElems = sortElements(allElements, links);
    TreeMap reverseElems = new TreeMap(Collections.reverseOrder());
    reverseElems.putAll(sortedElems);
    
    //
    // Collect up parent and child sets for each element
    //

    HashMap parentSets = new HashMap();
    HashMap childSets = new HashMap();
    HashSet singletons = new HashSet();
    Iterator aekit = allElements.keySet().iterator();
    
    buildSets(aekit, links, parentSets, childSets, singletons);    
    
    //
    // Keep going until all the non-singleton floating are gone.
    //
 
    currFloating.removeAll(singletons);    
    while (!currFloating.isEmpty()) {
      // 
      // Place all elements with fixed parents, using topo sort
      // order.  Do multiple passes until we have no floats with
      // fixed parents.
      //

      if (!currFixed.isEmpty()) { 
        while (true) {
          boolean haveDoneAFix = false;
          Iterator skit = sortedElems.keySet().iterator();
          while (skit.hasNext()) {
            Integer key = (Integer)skit.next();
            List elemList = (List)sortedElems.get(key);
            int numElems = elemList.size();
            for (int i = 0; i < numElems; i++) {
              String elemID = (String)elemList.get(i);
              if (floatsWithFixedRelative(elemID, currFixed, currFloating, parentSets)) {
                fixFloaterWithParents(elemID, currFixed, parentSets, allElements, yBoundary);
                currFloating.remove(elemID);
                currFixed.add(elemID);
                yBoundary = getFixedBottom(currFixed, allElements);
                haveDoneAFix = true;
              }
            }
          }
          if (!haveDoneAFix) {
            break;
          }
        }
      }

      boolean needCandidate = !currFloating.isEmpty();
      
      //
      // If we are still working, go in the reverse direction and
      // add a floater with a fixed kid.
      //
   
      if (needCandidate && !currFixed.isEmpty()) {
        Iterator rkit = reverseElems.keySet().iterator();
        while (rkit.hasNext() && needCandidate) {
          Integer key = (Integer)rkit.next();
          List elemList = (List)sortedElems.get(key);
          int numElems = elemList.size();
          for (int i = 0; i < numElems; i++) {
            String elemID = (String)elemList.get(i); 
            if (floatsWithFixedRelative(elemID, currFixed, currFloating, childSets)) {
              fixFloaterWithChildren(elemID, currFixed, childSets, allElements, yBoundary);
              currFloating.remove(elemID);
              currFixed.add(elemID);
              yBoundary = getFixedBottom(currFixed, allElements);
              needCandidate = false;
              break;
            }
          }
        }
      }
      
      //
      // Still no luck, so just pick somebody to fix:
      //
     
      if (needCandidate) {
        Iterator rkit = reverseElems.keySet().iterator();
        while (rkit.hasNext() && needCandidate) {
          Integer key = (Integer)rkit.next();
          List elemList = (List)sortedElems.get(key);
          int numElems = elemList.size();
          for (int i = 0; i < numElems; i++) {
            String elemID = (String)elemList.get(i); 
            if (currFloating.contains(elemID)) {
              int leftEnd = (currFixed.isEmpty()) ? 0 : getFixedLeft(currFixed, allElements);
              PlacementElement peElem = (PlacementElement)allElements.get(elemID);
              Rectangle elemRect = peElem.rect;
              peElem.rect = new Rectangle(leftEnd - elemRect.width, yBoundary, elemRect.width, elemRect.height);
              currFloating.remove(elemID);
              currFixed.add(elemID);
              yBoundary = getFixedBottom(currFixed, allElements);
              needCandidate = false;
              break;
            }
          }
        }
      }
    }

    //
    // Now place the singletons below existing stuff
    //
    
    if (!singletons.isEmpty()) {
      int leftEnd = (currFixed.isEmpty()) ? 0 : getFixedLeft(currFixed, allElements);
      TopLeftPacker tlp = new TopLeftPacker();
      ArrayList packElems = new ArrayList();
      Iterator singit = singletons.iterator();
      while (singit.hasNext()) {
        String singID = (String)singit.next();
        if (currFixed.contains(singID)) {
          continue;
        }
        packElems.add(allElements.get(singID));
      }
      HashMap points = new HashMap();
      int maxX = (!currFixed.isEmpty()) ? tlp.placeElementsDownward(packElems, points, UiUtil.GRID_SIZE_INT, monitor)
                                        : tlp.placeElements(packElems, points, UiUtil.GRID_SIZE_INT, monitor);
      int leftX = leftEnd;
      singit = singletons.iterator();
      while (singit.hasNext()) {
        String singID = (String)singit.next();
        if (currFixed.contains(singID)) {
          continue;
        }
        Point topLeft = (Point)points.get(singID);
        PlacementElement peElem = (PlacementElement)allElements.get(singID);
        Rectangle elemRect = peElem.rect;
        peElem.rect = new Rectangle(leftX + topLeft.x, yBoundary + topLeft.y, elemRect.width, elemRect.height);
      }
    }
    
    return;
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
  ** Topo sort parents
  */  
  
  private SortedMap sortElements(Map allElements, List links) {   
  
    //
    // Nodes:
    //
    
    HashSet nodes = new HashSet(allElements.keySet());
    
    //
    // Links w/o cycles:
    //
    
    HashSet linkSet = new HashSet();    
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      Link cfl = (Link)links.get(i);
      linkSet.add(cfl);
      CycleFinder cf = new CycleFinder(nodes, linkSet);
      if (cf.hasACycle()) {
        linkSet.remove(cfl);
      }
    }   
    GraphSearcher searcher = new GraphSearcher(nodes, linkSet);    
    Map topoSort = searcher.topoSort(false);
    
    //
    // Invert the map
    //
    
    TreeMap invert = new TreeMap();
    searcher.invertTopoSort(topoSort, invert);
    return (invert);
  }
  
  /***************************************************************************
  **
  ** Answer if element is floating with a fixed relative
  */  
  
  private boolean floatsWithFixedRelative(String elemID, Set currFixed, 
                                        Set currFloating, Map relativeSets) {   
    if (!currFloating.contains(elemID)) {
      return (false);
    }
    
    Set relativeSet = (Set)relativeSets.get(elemID);
    if ((relativeSet == null) || relativeSet.isEmpty()) {
      return (false);
    }
    
    Iterator cfit = currFixed.iterator();
    while (cfit.hasNext()) {
      String cfID = (String)cfit.next();
      if (relativeSet.contains(cfID)) {
        return (true);
      }
    }
    
    return (false);
  }
    
  /***************************************************************************
  **
  ** Find the parent and child elements of the given element
  */
  
  private void getBoundingElements(String elemID, List links, Set parents, Set children) {
    
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      Link cfl = (Link)links.get(i);
      if (cfl.getSrc().equals(elemID)) {
        children.add(cfl.getTrg());
      }
      if (cfl.getTrg().equals(elemID)) {
        parents.add(cfl.getSrc());
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Collect up parent and child sets for each element
  */  

  private void buildSets(Iterator aekit, List links, Map parentSets, 
                         Map childSets, Set singletons) {
         
    while (aekit.hasNext()) {
      String elemID = (String)aekit.next();
      HashSet parents = new HashSet();
      HashSet children = new HashSet();
      getBoundingElements(elemID, links, parents, children);
      if (parents.isEmpty() && children.isEmpty()) {
        singletons.add(elemID);
      }
      if (!parents.isEmpty()) {
        parentSets.put(elemID, parents);
      }
      if (!children.isEmpty()) {
        childSets.put(elemID, children);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Fix a floater using previously fixed parents
  */
   
  private void fixFloaterWithParents(String elemID, Set currFixed, 
                                     Map parentSets, Map allElements, int yBoundary) {

    Set parentSet = (Set)parentSets.get(elemID);
    if ((parentSet == null) || parentSet.isEmpty()) {
      throw new IllegalArgumentException();
    }

    int parentMax = Integer.MIN_VALUE; 
    
    Iterator cfit = currFixed.iterator();
    while (cfit.hasNext()) {
      String cfID = (String)cfit.next();
      if (parentSet.contains(cfID)) {
        PlacementElement pe = (PlacementElement)allElements.get(cfID);
        int rightSide = pe.rect.x + pe.rect.width;
        if (rightSide > parentMax) {
          parentMax = rightSide;
        }
      }
    }
    
    if (parentMax == Integer.MIN_VALUE) {
      throw new IllegalArgumentException();
    }
    
    PlacementElement peElem = (PlacementElement)allElements.get(elemID);
    Rectangle elemRect = peElem.rect;
    peElem.rect = new Rectangle(parentMax, yBoundary, elemRect.width, elemRect.height);
    return;
  }
  
  /***************************************************************************
  **
  ** Fix a floater using previously fixed parents
  */
   
  private void fixFloaterWithChildren(String elemID, Set currFixed, 
                                      Map childSets, Map allElements, int yBoundary) {

    Set childSet = (Set)childSets.get(elemID);
    if ((childSet == null) || childSet.isEmpty()) {
      throw new IllegalArgumentException();
    }

    int childMin = Integer.MAX_VALUE; 
    
    Iterator cfit = currFixed.iterator();
    while (cfit.hasNext()) {
      String cfID = (String)cfit.next();
      if (childSet.contains(cfID)) {
        PlacementElement pe = (PlacementElement)allElements.get(cfID);
        int leftSide = pe.rect.x;
        if (leftSide < childMin) {
          childMin = leftSide;
        }
      }
    }
    
    if (childMin == Integer.MIN_VALUE) {
      throw new IllegalArgumentException();
    }
    
    PlacementElement peElem = (PlacementElement)allElements.get(elemID);
    Rectangle elemRect = peElem.rect;
    peElem.rect = new Rectangle(childMin - elemRect.width, yBoundary, elemRect.width, elemRect.height);
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Return the lower edge of the fixed elements
  */

  private int getFixedBottom(Set currFixed, Map allElements) {
    int max = Integer.MIN_VALUE;
    Iterator cfit = currFixed.iterator();
    while (cfit.hasNext()) {
      String cfid = (String)cfit.next();
      PlacementElement pe = (PlacementElement)allElements.get(cfid);
      int bottom = pe.rect.y + pe.rect.height;
      if (bottom > max) {
        max = bottom;
      }
    }
    return (max);
  } 
  
  /***************************************************************************
  ** 
  ** Return the left edge of the fixed elements
  */

  private int getFixedLeft(Set currFixed, Map allElements) {
    int min = Integer.MAX_VALUE;
    Iterator cfit = currFixed.iterator();
    while (cfit.hasNext()) {
      String cfid = (String)cfit.next();
      PlacementElement pe = (PlacementElement)allElements.get(cfid);
      int left = pe.rect.x;
      if (left < min) {
        min = left;
      }
    }
    return (min);
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

}
