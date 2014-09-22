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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;

import org.systemsbiology.biotapestry.util.SimpleLink;

/****************************************************************************
**
** A class for doing crossing reduction.  See Battista et. al. pp 281 -283
*/

public class CrossingReducer {
  
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

  public CrossingReducer() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Reduce the crossings
  */

  public Map<String, Integer> reduceCrossings(Map<String, Integer> row1Nodes, Map<String, Integer> row2Nodes, Set<SimpleLink> links) {
    
    //
    // Invert: create a sorted map of node position to node
    //
    
    TreeMap<Integer, String> nodeAtPos = new TreeMap<Integer, String>();
    Iterator<String> r2it = row2Nodes.keySet().iterator();
    while (r2it.hasNext()) {
      String node = r2it.next();
      Integer pos = row2Nodes.get(node);
      nodeAtPos.put(pos, node);
    }
    
    HashMap<String, Integer> retval = new HashMap<String, Integer>(row2Nodes);
 
    //
    // Calculate crossing numbers:
    //
    
    Map<NodeTuple, Integer> crossNums = calcCrossingNumbers(row1Nodes, retval, links);

    int totalCrossings = calcTotalGraphCrossings(retval, crossNums);
    
    Set<Integer> posKeys = nodeAtPos.keySet();    
    while (true) {
      Iterator<Integer> napit1 = posKeys.iterator();
      while (napit1.hasNext()) {
        Integer pos1 = napit1.next();
        Iterator<Integer> napit2 = posKeys.iterator();
        while (napit2.hasNext()) {
          Integer pos2 = napit2.next();
          if (pos2.compareTo(pos1) <= 0) {
            continue;
          }
          String node1 = nodeAtPos.get(pos1);
          String node2 = nodeAtPos.get(pos2);
          NodeTuple tup1 = new NodeTuple(node1, node2);
          Integer crossNum1 = crossNums.get(tup1);
          NodeTuple tup2 = new NodeTuple(node2, node1);
          Integer crossNum2 = crossNums.get(tup2);
          if (crossNum1.compareTo(crossNum2) > 0) {
            swapNodes(node1, node2, retval, nodeAtPos);
          }
        }
      }
      int newTotalCrossings = calcTotalGraphCrossings(retval, crossNums);
      if (newTotalCrossings >= totalCrossings) {
        return (retval);
      }
      totalCrossings = newTotalCrossings;
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
  
  /***************************************************************************
  ** 
  ** Calculate crossing numbers
  */

  private Map<NodeTuple, Integer> calcCrossingNumbers(Map<String, Integer> row1Nodes, Map<String, Integer> row2Nodes, Set<SimpleLink> links) {

    HashMap<NodeTuple, Integer> retval = new HashMap<NodeTuple, Integer>();
    
    Iterator<String> r2it1 = row2Nodes.keySet().iterator();    
    while (r2it1.hasNext()) {
      String node1 = r2it1.next();
      Iterator<String> r2it2 = row2Nodes.keySet().iterator();
      while (r2it2.hasNext()) {
        String node2 = r2it2.next();
        Integer crossNumber = new Integer(calcCrossingNumber(node1, node2, row1Nodes, links));
        NodeTuple tup = new NodeTuple(node1, node2);
        retval.put(tup, crossNumber);
      }
    }
    
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate a crossing number
  */

  private int calcCrossingNumber(String node1, String node2, Map<String, Integer> row1Nodes, Set<SimpleLink> links) {
    if (node1.equals(node2)) {
      return (0);
    }
    
    //
    // For each link (w, node1), get the count of links (z, node2), where col(w) < col(z)
    //
    
    int retval = 0;
    Iterator<SimpleLink> lit = links.iterator();
    while (lit.hasNext()) {
      SimpleLink link1 = lit.next();
      if (!link1.getTrg().equals(node1)) {
        continue;
      }
      Integer link1SrcLocObj = row1Nodes.get(link1.getSrc());
      if (link1SrcLocObj == null) {
        continue;
      }
      int link1SrcLoc = link1SrcLocObj.intValue();
      Iterator<SimpleLink> lit2 = links.iterator();
      while (lit2.hasNext()) {
        SimpleLink link2 = lit2.next();
        if (!link2.getTrg().equals(node2)) {
          continue;
        }
        Integer link2SrcLocObj = row1Nodes.get(link2.getSrc());
        if (link2SrcLocObj == null) {
          continue;
        }
        int link2SrcLoc = link2SrcLocObj.intValue();        
        if (link2SrcLoc < link1SrcLoc) {
          retval++;      
        }
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Calculate the number of crossings in the graph
  */

  private int calcTotalGraphCrossings(Map<String, Integer> row2Nodes, Map<NodeTuple, Integer> crossNums) {
    int retval = 0;
    Iterator<String> r2it1 = row2Nodes.keySet().iterator();
    while (r2it1.hasNext()) {
      String node1 = r2it1.next();
      Integer node1Pos = row2Nodes.get(node1); 
      Iterator<String> r2it2 = row2Nodes.keySet().iterator();
      while (r2it2.hasNext()) {
        String node2 = r2it2.next();
        Integer node2Pos = row2Nodes.get(node2);
        if (node1Pos.compareTo(node2Pos) >= 0) {
          continue;
        }
        NodeTuple tup = new NodeTuple(node1, node2);
        int crossNum = crossNums.get(tup).intValue();
        retval += crossNum;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Calculate the floor for the optimum crossing count
  */

  @SuppressWarnings("unused")
  private int calcOptimumFloor(Map<String, Integer> row2Nodes, Map<NodeTuple, Integer> crossNums) {
    
    int retval = 0;
    Iterator<String> r2it1 = row2Nodes.keySet().iterator();
    int node1Count = 0;
    while (r2it1.hasNext()) {
      String node1 = r2it1.next();
      Iterator<String> r2it2 = row2Nodes.keySet().iterator();
      int node2Count = 0;
      while (r2it2.hasNext()) {
        String node2 = r2it2.next();
        if (node2Count >= node1Count) {  // only do one permutation of tuple
          break;
        }
        NodeTuple tup1 = new NodeTuple(node1, node2);
        int crossNum1 = crossNums.get(tup1).intValue();
        NodeTuple tup2 = new NodeTuple(node2, node1);
        int crossNum2 = crossNums.get(tup2).intValue();        
        retval += Math.min(crossNum1, crossNum2);
        node2Count++;
      }
      node1Count++;
    }
    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Swap the position of the two nodes
  */

  private void swapNodes(String node1, String node2, Map<String, Integer> row2Nodes, SortedMap<Integer, String> nodeAtPos) {
    
    Integer n1pos = row2Nodes.get(node1);
    Integer n2pos = row2Nodes.get(node2);
    row2Nodes.put(node1, n2pos);
    row2Nodes.put(node2, n1pos);
    nodeAtPos.put(n1pos, node2);
    nodeAtPos.put(n2pos, node1);
    return;    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static class NodeTuple {

    public String node1;
    public String node2;

    public NodeTuple(String node1, String node2) {
      this.node1 = node1;
      this.node2 = node2;
    }

    public int hashCode() {
      return (node1.hashCode() + (2 * node2.hashCode()));
    }

    public String toString() {
      return ("[" + node1 + "," + node2 + "]");
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof NodeTuple)) {
        return (false);
      }
      NodeTuple otherTuple = (NodeTuple)other;
      return (this.node1.equals(otherTuple.node1) && this.node2.equals(otherTuple.node2));
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
    
    CrossingReducer cr = new CrossingReducer();
    HashMap<String, Integer> row1Nodes = new HashMap<String, Integer>();
    row1Nodes.put("a", new Integer(0));
    row1Nodes.put("b", new Integer(1));
    row1Nodes.put("c", new Integer(2));
    row1Nodes.put("d", new Integer(3));
    row1Nodes.put("e", new Integer(4));
    row1Nodes.put("f", new Integer(5));
    row1Nodes.put("g", new Integer(6));
    
    HashMap<String, Integer> row2Nodes = new HashMap<String, Integer>();
    row2Nodes.put("p", new Integer(0));
    row2Nodes.put("q", new Integer(2));
    row2Nodes.put("u", new Integer(4));
    row2Nodes.put("r", new Integer(6));
    
    Iterator<String> r2it = row2Nodes.keySet().iterator();
    System.out.println("Before:");
    while (r2it.hasNext()) {
      String node = r2it.next();
      Integer nodePos = row2Nodes.get(node);
      System.out.println("Node = " + node + " pos = " + nodePos);
    }    
      
    HashSet<SimpleLink> links = new HashSet<SimpleLink>();
    links.add(new SimpleLink("a", "p"));
    links.add(new SimpleLink("a", "u"));
    links.add(new SimpleLink("b", "q"));
    links.add(new SimpleLink("c", "q"));
    links.add(new SimpleLink("c", "r"));
    links.add(new SimpleLink("d", "p"));
    links.add(new SimpleLink("d", "q"));
    links.add(new SimpleLink("d", "r"));
    links.add(new SimpleLink("e", "u"));
    links.add(new SimpleLink("f", "u"));
    links.add(new SimpleLink("g", "q"));
    links.add(new SimpleLink("g", "u"));
    
    Map<String, Integer> permuted = cr.reduceCrossings(row1Nodes, row2Nodes, links);
    Iterator<String> pit = permuted.keySet().iterator();
    System.out.println("After:");
    while (pit.hasNext()) {
      String node = pit.next();
      Integer nodePos = permuted.get(node);
      System.out.println("Node = " + node + " pos = " + nodePos);
    }
  }  
}
