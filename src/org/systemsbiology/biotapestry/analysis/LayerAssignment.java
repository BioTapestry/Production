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
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;

import org.systemsbiology.biotapestry.util.SimpleLink;

/****************************************************************************
**
** A class for doing layer assignment using Coffman-Graham.  
** See Battista et. al. pp 272 - 278
*/

public class LayerAssignment {
  
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

  public LayerAssignment() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Return a map that gives layers for each node.
  */

  public Map<String, Integer> assignLayers(Map<String, Integer> nodeToLayer, Set<SimpleLink> links, int maxPerLayer, boolean isGreedy) {
    
    //
    // Invert: create a sorted map of node position to sets of nodes
    //
    
    TreeMap<Integer, Set<String>> nodesAtPos = new TreeMap<Integer, Set<String>>();
    HashSet<String> allNodes = new HashSet<String>();
    Iterator<String> nlit = nodeToLayer.keySet().iterator();
    while (nlit.hasNext()) {
      String node = nlit.next();
      Integer pos = nodeToLayer.get(node);
      Set<String> nodes = nodesAtPos.get(pos);
      if (nodes == null) {
        nodes = new HashSet<String>();
        nodesAtPos.put(pos, nodes);
      }
      nodes.add(node);
      allNodes.add(node);
    }
    
    Map<String, Integer> node2Label = assignLabels(nodesAtPos, links);
    
    Map<String, Set<String>> targsFromNode = targetsFromNode(links);

    int currLayer = 0;
    int numInLayer = 0;
    int highestLayer = 0;
    HashMap<String, Integer> nodesPlaced = new HashMap<String, Integer>();
    HashMap<String, Integer> highestTarget = new HashMap<String, Integer>();
    Set<String> eligible = initEligible(targsFromNode, allNodes);
     
    while (!eligible.isEmpty()) {
      String nodeWithHighest = null;
      Integer highestLabel = new Integer(Integer.MIN_VALUE);
      Iterator<String> elit = eligible.iterator();
      while (elit.hasNext()) {
        String nodeID = elit.next();

        Integer label = node2Label.get(nodeID);
        if (highestLabel.compareTo(label) < 0) {
          highestLabel = label;
          nodeWithHighest = nodeID;
        }
      }
      eligible.remove(nodeWithHighest);
      
      if (isGreedy) {
        processPlacement(nodeWithHighest, highestTarget, nodesPlaced, 
                         targsFromNode, currLayer);
        highestLayer = currLayer;
        numInLayer++;
        if ((numInLayer == maxPerLayer) || eligible.isEmpty()) {
          currLayer++; 
          numInLayer = 0;
        }
        fillEligible(nodesPlaced.keySet(), targsFromNode, highestTarget, currLayer, eligible, true);
      } else {
        if ((numInLayer >= maxPerLayer) || !targetsAreLower(nodeWithHighest, highestTarget, currLayer)) {
          currLayer++; 
          numInLayer = 0;
        }
        processPlacement(nodeWithHighest, highestTarget, nodesPlaced, 
                         targsFromNode, currLayer);
        highestLayer = currLayer;
        numInLayer++;
        fillEligible(nodesPlaced.keySet(), targsFromNode, highestTarget, currLayer, eligible, false);
      }
    }

    //
    // Reverse the layer assignment order
    //
    
    Iterator<String> npit = nodesPlaced.keySet().iterator();
    while (npit.hasNext()) {
      String nodekey = npit.next();
      Integer layer = nodesPlaced.get(nodekey);
      nodesPlaced.put(nodekey, new Integer(highestLayer - layer.intValue()));
    }
   
    return (nodesPlaced);
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
  ** Maintain the set of nodes eligible for placement:
  */

  private void processPlacement(String placedNode, Map<String, Integer> highestTarget,
                               Map<String, Integer> nodesPlaced, Map<String, Set<String>> targetsFromNode, 
                               int currentLayer) {
    //
    // When a node is placed, we put it in the nodesPlaced set, then go through all
    // the targets from node and remove the placed node from all targets.
    //
    
    nodesPlaced.put(placedNode, new Integer(currentLayer));
    Iterator<String> tfn = targetsFromNode.keySet().iterator();
    while (tfn.hasNext()) {
      String src = tfn.next();
      Set<String> targets = targetsFromNode.get(src);
      if (targets.remove(placedNode) && targets.isEmpty()) {
        highestTarget.put(src, new Integer(currentLayer));
      }
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Answer if all targets are in lower layers
  */

  private boolean targetsAreLower(String src, Map<String, Integer> highestTarget, int currentLayer) {
    Integer highest = highestTarget.get(src);
    if (highest == null) {
      return (true);
    }
    int highestVal = highest.intValue();
    if (highestVal > currentLayer) {
      throw new IllegalStateException();
    } else if (highestVal == currentLayer) {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  ** 
  ** Refill the eligible set
  */

  private void fillEligible(Set<String> placed, Map<String, Set<String>> targetsFromNode, Map<String, Integer> highestTarget, 
                            int currentLayer, Set<String> eligible, boolean belowCurrent) {
    Iterator<String> tfn = targetsFromNode.keySet().iterator();
    while (tfn.hasNext()) {
      String src = tfn.next();
      if (placed.contains(src)) {
        continue;
      }
      Set<String> targets = targetsFromNode.get(src);
      if (targets.isEmpty()) {
        if (belowCurrent) {
          Integer highest = highestTarget.get(src);
          if (highest.intValue() < currentLayer) {
            eligible.add(src);
          }
        } else {
          eligible.add(src);
        }
      }
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** Initialize the eligible set
  */

  private Set<String> initEligible(Map<String, Set<String>> targetsFromNode, Set<String> allNodes) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> anit = allNodes.iterator();
    while (anit.hasNext()) {
      String src = anit.next();
      Set<String> targets = targetsFromNode.get(src);
      if ((targets == null) || targets.isEmpty()) {
        retval.add(src);
      }
    }
    return (retval);
  }   

  /***************************************************************************
  ** 
  ** Assign labels to nodes.  Also build a map of link targets outbound from each node
  */

  private Map<String, Integer> assignLabels(Map<Integer, Set<String>> nodesAtPos, Set<SimpleLink> links) {
    
    HashMap<String, Integer> node2Label = new HashMap<String, Integer>();
    TreeMap<LabelSet, String> labelSet2Node = new TreeMap<LabelSet, String>();
 
    //
    // Label the nodes:
    //
    
    int currLabel = 0;
    Iterator<Integer> napit = nodesAtPos.keySet().iterator();
    while (napit.hasNext()) {
      Integer pos = napit.next();
      Set<String> nodes = nodesAtPos.get(pos);
      labelSet2Node.clear();
      Iterator<String> nit = nodes.iterator();
      //
      // Get a labelset for each node in the layer:
      //
      while (nit.hasNext()) {
        String node = nit.next();
        LabelSet lset = new LabelSet(node);
        Iterator<SimpleLink> lit = links.iterator();
        while (lit.hasNext()) {
          SimpleLink slink = lit.next();
          if (!slink.getTrg().equals(node)) {
            continue;
          }
          String src = slink.getSrc();
          Integer srcLabel = node2Label.get(src);
          if (srcLabel == null) { // feedback loop: ignore
            continue;
          }   
          lset.addLabel(srcLabel);
        }
        labelSet2Node.put(lset, node);
      }
      //
      // Labels are sorted by their set ordering; extract and assign
      //
      Iterator<LabelSet> lsit = labelSet2Node.keySet().iterator();
      while (lsit.hasNext()) {
        LabelSet lset = lsit.next();
        String node = labelSet2Node.get(lset);
        node2Label.put(node, new Integer(currLabel++));
      }
    }
    
    return (node2Label);
  }

  /***************************************************************************
  ** 
  ** Return a map of targets from each node
  */

  private Map<String, Set<String>> targetsFromNode(Set<SimpleLink> links) {
    
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    
    Iterator<SimpleLink> lit = links.iterator();
    while (lit.hasNext()) {
      SimpleLink slink = lit.next();
      String src = slink.getSrc();
      Set<String> targs = retval.get(src);
      if (targs == null) {
        targs = new HashSet<String>();
        retval.put(src, targs);
      }
      targs.add(slink.getTrg());
    }
    
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static class LabelSet implements Comparable<LabelSet> {

    private TreeSet<Integer> terms_;
    private String nodeID_; // used to force uniqueness

    public LabelSet(String nodeID) {
      terms_ = new TreeSet<Integer>(Collections.reverseOrder());
      nodeID_ = nodeID;
    }
    
    public void addLabel(Integer label) {
      terms_.add(label);
    }    

    public int hashCode() {
      return (terms_.hashCode() + nodeID_.hashCode());
    }

    public String toString() {
      return (terms_.toString() + " " + nodeID_);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof LabelSet)) {
        return (false);
      }
      return (this.compareTo((LabelSet)other) == 0);
    } 

    public int compareTo(LabelSet other) {
      Iterator<Integer> otherit = other.terms_.iterator();
      Iterator<Integer> thisit = this.terms_.iterator();
      while (thisit.hasNext()) {
        if (!otherit.hasNext()) {
          return (1);
        }
        Integer thisVal = thisit.next();
        Integer otherVal = otherit.next();
        int compare = thisVal.compareTo(otherVal);
        if (compare != 0) {
          return (compare);
        }
      }
      if (otherit.hasNext()) {
        return (-1);
      }
      return (this.nodeID_.compareTo(other.nodeID_));
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
    
    LayerAssignment la = new LayerAssignment();
    HashMap<String, Integer> node2Layer = new HashMap<String, Integer>();
    node2Layer.put("1", new Integer(0));
    node2Layer.put("2", new Integer(0));
    node2Layer.put("3", new Integer(1));
    node2Layer.put("4", new Integer(1));
    node2Layer.put("5", new Integer(2));
    node2Layer.put("6", new Integer(2));
    node2Layer.put("7", new Integer(2));
    node2Layer.put("8", new Integer(2));
    node2Layer.put("9", new Integer(3));
    node2Layer.put("10", new Integer(3));
    node2Layer.put("11", new Integer(4));
    node2Layer.put("12", new Integer(4));
    node2Layer.put("13", new Integer(5));
      
    HashSet<SimpleLink> links = new HashSet<SimpleLink>();
    links.add(new SimpleLink("1", "3"));
    links.add(new SimpleLink("1", "4"));
    links.add(new SimpleLink("3", "5"));
    links.add(new SimpleLink("3", "8"));
    links.add(new SimpleLink("3", "10"));    
    links.add(new SimpleLink("4", "6"));
    links.add(new SimpleLink("4", "7"));
    links.add(new SimpleLink("4", "8"));
    links.add(new SimpleLink("4", "9"));
    links.add(new SimpleLink("5", "9"));
    links.add(new SimpleLink("7", "11"));    
    links.add(new SimpleLink("8", "10"));
    links.add(new SimpleLink("9", "12"));
    links.add(new SimpleLink("10", "12"));
    links.add(new SimpleLink("10", "11"));
    links.add(new SimpleLink("11", "13"));
    links.add(new SimpleLink("12", "13"));
    
    Map<String, Integer> permuted = la.assignLayers(node2Layer, links, 3, false);
    Iterator<String> pit = permuted.keySet().iterator();
    System.out.println("After:");
    while (pit.hasNext()) {
      String node = pit.next();
      Integer nodePos = permuted.get(node);
      System.out.println("Node = " + node + " layer = " + nodePos);
    }
  }  
}
