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
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;

/****************************************************************************
**
** For coloring a graph
*/

public class GraphColorer {
  
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
  
  private Set<String> nodes_;
  private HashMap<String, Set<String>> linksForNode_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GraphColorer(Set<String> nodes, Set<Link> links) {
    nodes_ = nodes;
    linksForNode_ = new HashMap<String, Set<String>>();
    Iterator<Link> lit = links.iterator();
    while (lit.hasNext()) {
      Link link = lit.next();
      Set<String> linksForSrc = linksForNode_.get(link.getSrc());
      if (linksForSrc == null) {
        linksForSrc = new HashSet<String>();
        linksForNode_.put(link.getSrc(), linksForSrc);
      }
      linksForSrc.add(link.getTrg());
    }   
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Color the graph, using a greedy algorithm
  */

  public int color(Map<String, Integer> colored) {
    
    //
    // Build a set of uncolored vertices, and an assignment of colors
    //
    
    HashSet<String> uncolored = new HashSet<String>(nodes_);
    TreeSet<Integer> colors = new TreeSet<Integer>();
    HashSet<String> assigned = new HashSet<String>();
    colored.clear();
    
    if (uncolored.isEmpty()) {
      return (0);
    }
 
    while (!uncolored.isEmpty()) {
      assigned.clear();
      assignNextColor(assigned, uncolored);
      Integer nextColor = getNextColor(colors);
      Iterator<String> ait = assigned.iterator();
      while (ait.hasNext()) {
        String node = ait.next();
        colored.put(node, nextColor);
        uncolored.remove(node);
      }
    }
    
    return (colors.size());
  }

  /***************************************************************************
  ** 
  ** Get the next color
  */

  private Integer getNextColor(TreeSet<Integer> colors) {
    if (colors.isEmpty()) {
      Integer first = new Integer(0);
      colors.add(first);
      return (first);
    }
    Integer last = colors.last();
    Integer next = new Integer(last.intValue() + 1);
    colors.add(next);
    return (next);
  }
  
  /***************************************************************************
  ** 
  ** Assign the next color
  */

  private void assignNextColor(Set<String> assigned, Set<String> uncolored) {
    Iterator<String> ucit = uncolored.iterator();
    while (ucit.hasNext()) {
      String node = ucit.next();
      boolean canAdd = true;
      Set<String> links = linksForNode_.get(node);      
      if (links != null) {
        Iterator<String> lit = links.iterator();
        while (lit.hasNext()) {
          String targ = lit.next();
          if (assigned.contains(targ)) {
            canAdd = false;
            break;
          }
        }
      }
      if (canAdd) {
        assigned.add(node);
      }
    }
  }
}
