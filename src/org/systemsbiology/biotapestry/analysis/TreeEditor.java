/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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
import java.util.SortedSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;

/****************************************************************************
**
** A Class
*/

public class TreeEditor {
  
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
  private String root_;
  private HashMap<String, Set<String>> childrenForNode_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.  Illegal argument if network is not a tree
  */

  public TreeEditor(Set<String> nodes, Set<Link> links) {
    nodes_ = nodes;
    childrenForNode_ = new HashMap<String, Set<String>>();
    root_ = (nodes.isEmpty()) ? null : processTree(nodes, links, childrenForNode_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Figure out the additions and subtractions involved in getting to the
  ** new tree.
  */

  public boolean getDifferences(Set<String> newNodes, Set<Link> newLinks, List<String> addedNodes, List<String> removedNodes) {
   
    HashMap<String,Set<String>> newChildrenForNode = new HashMap<String,Set<String>>();
    String newRoot = (newNodes.isEmpty()) ? null : processTree(newNodes, newLinks, newChildrenForNode);
    if (root_ == null) {
      addedNodes.addAll(newNodes);
    } else if (newRoot == null) {
      removedNodes.addAll(nodes_);
    } else if (!newRoot.equals(root_)) {
      addedNodes.addAll(newNodes);      
      removedNodes.addAll(nodes_);
    } else {
      kidCompare(childrenForNode_, newChildrenForNode, root_, removedNodes, addedNodes);
    }
    return (!addedNodes.isEmpty() || !removedNodes.isEmpty());
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
  ** Illegal argument if network is not a tree
  */

  private String processTree(Set<String> nodes, Set<Link> links, Map<String, Set<String>> childrenForNode) {
    HashSet<String> working = new HashSet<String>(nodes);
    Iterator<Link> lit = links.iterator();
    while (lit.hasNext()) {
      Link link = lit.next();
      Set<String> linksForSrc = childrenForNode.get(link.getSrc());
      if (linksForSrc == null) {
        linksForSrc = new TreeSet<String>();
        childrenForNode.put(link.getSrc(), linksForSrc);
      }
      String target = link.getTrg();
      linksForSrc.add(target);
      if (!working.contains(target)) {
        throw new IllegalArgumentException();
      }
      working.remove(target);
    }
    if (working.size() != 1) {
      throw new IllegalArgumentException();
    }
    String root = working.iterator().next();
    depthSearch(root, childrenForNode, nodes);
    return (root);
  }
  
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  private List depthSearch(String root, Map links, Set nodes) {       
    List retval = new ArrayList();
    if (root != null) {
      searchGutsDepth(root, links, retval);
    }
    if (retval.size() != nodes.size()) {
      throw new IllegalArgumentException();
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(String vertexID, Map links, List results) {
    if (vertexID == null) {
      return;
    }
    if (results.contains(vertexID)) {
      throw new IllegalArgumentException();
    }
    results.add(vertexID);
    Set kids = (Set)links.get(vertexID);
    if (kids == null) {
      return;
    }
    Iterator eit = kids.iterator();
    while (eit.hasNext()) {
      String targ = (String)eit.next();
      searchGutsDepth(targ, links, results);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Compare kids for new and old nodes
  */

  private void kidCompare(Map oldChildren, Map newChildren, String vertexID, List<String> deletedNodes, List<String> newNodes) {
    SortedSet oldKids = (SortedSet)oldChildren.get(vertexID);
    SortedSet newKids = (SortedSet)newChildren.get(vertexID);
    
    TreeSet oldWorking = (oldKids == null) ? new TreeSet() : new TreeSet(oldKids);
    TreeSet<String> newWorking = (newKids == null) ? new TreeSet() : new TreeSet(newKids);

    while (!oldWorking.isEmpty() || !newWorking.isEmpty()) {
      
      //
      // Nobody left, just chew through remaining lists:
      //
      if (oldWorking.isEmpty()) {
        String newNode = newWorking.first();
        newWorking.remove(newNode);
        newNodes.add(newNode);
        gatherAllChildren(newNode, newChildren, newNodes);
        continue;
      } else if (newWorking.isEmpty()) {
        String oldNode = (String)oldWorking.first();
        oldWorking.remove(oldNode);
        deletedNodes.add(oldNode);
        gatherAllChildren(oldNode, oldChildren, deletedNodes);
        continue;
      } 
      
      //
      // Work with first element in each
      //
      
      String newFirst = (String)newWorking.first();
      String oldFirst = (String)oldWorking.first();

      //
      // If equal, do recursive.  If unequal, toss away lower item, 
      // since the lists are sorted:
      //
      
      if (newFirst.equals(oldFirst)) {
        newWorking.remove(newFirst);
        oldWorking.remove(oldFirst);
        kidCompare(oldChildren, newChildren, newFirst, deletedNodes, newNodes);
      } else if (newFirst.compareTo(oldFirst) < 0) {
        String newNode = (String)newWorking.first();
        newWorking.remove(newNode);
        newNodes.add(newNode);
        gatherAllChildren(newNode, newChildren, newNodes);
      } else {
        String oldNode = (String)oldWorking.first();
        oldWorking.remove(oldNode);
        deletedNodes.add(oldNode);
        gatherAllChildren(oldNode, oldChildren, deletedNodes);
      } 
    }
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Gather up all the children for a node, add to given list
  */

  private void gatherAllChildren(String vertexID, Map children, List listToFill) {
    SortedSet kids = (SortedSet)children.get(vertexID);
    if (kids == null) {
      return;
    }
    Iterator kit = kids.iterator();
    while (kit.hasNext()) {
      String kVertID = (String)kit.next();
      listToFill.add(kVertID);
      gatherAllChildren(kVertID, children, listToFill);
    }
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
