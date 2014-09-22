/*
**    Copyright (C) 2003-2006 Institute for Systems Biology 
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

import java.util.ArrayList;
import java.util.List;

/****************************************************************************
**
** Core Node of a cluster tree
*/

public class ClusterParent extends ClusterNode {
  
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
  
  private ArrayList<ClusterNode> children_;
  private boolean pure_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterParent(int numSources, boolean pure) {
    super(numSources);
    children_ = new ArrayList<ClusterNode>();
    pure_ = pure;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Build the cluster from the given children
  */

  public void addChildren(List<ClusterNode> children) {
    
    int vecSize = vector_.length;

    int numKids = children.size();    
    if (!pure_ && (numKids != 2)) {
      throw new IllegalArgumentException();
    }

    //
    // We want to order the children to make the
    // two grandkids with the higher similarity to
    // be traversed contiguously
    //

    if (!pure_) {
      ClusterNode left = children.get(0);
      ClusterNode right = children.get(1);
      
      double inner = getInnerSimilarity(left, right);
      double outer = getOuterSimilarity(left, right);   
      if (outer > inner) {
        children_.add(right);      
        children_.add(left);
      } else {
        children_.add(left);      
        children_.add(right);
      }
    } else {
      children_.addAll(children);
    }
        
    for (int i = 0; i < numKids; i++) {
      ClusterNode node = children_.get(i);
      double[] kidVector = node.getVector();
      for (int j = 0; j < vecSize; j++) {
        vector_[j] += kidVector[j];
      }
    }
    double den = (double)vecSize;
    for (int j = 0; j < vecSize; j++) {
      vector_[j] /= den;
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the left kid
  */

  public ClusterNode getLeftChild() {
    return ((pure_) ? this : children_.get(0));
  }
  
  /***************************************************************************
  ** 
  ** Get the right kid
  */

  public ClusterNode getRightChild() {
    return ((pure_) ? this : children_.get(1));
  }     
  
  /***************************************************************************
  ** 
  ** Add to the in-order traversal
  */

  public void appendToClusterList(List<String> clist) {
    int numKids = children_.size();    
    for (int i = 0; i < numKids; i++) {
      ClusterNode node = children_.get(i);
      node.appendToClusterList(clist);
    }
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Add to the chunked in-order traversal
  */

  public void appendToChunkedClusterList(List<ClusterBuilder.IntAnnotatedList> clist) {

    int numKids = children_.size();
    ClusterBuilder.IntAnnotatedList addList;
    if (pure_) {
      ClusterLeaf leaf = (ClusterLeaf)children_.get(0);
      addList = new ClusterBuilder.IntAnnotatedList(leaf.numSources());
      clist.add(addList);
      for (int i = 0; i < numKids; i++) {
        leaf = (ClusterLeaf)children_.get(i);
        leaf.appendToClusterList(addList.getList());
      }
    } else {
      for (int i = 0; i < numKids; i++) {
        ClusterNode node = children_.get(i);
        node.appendToChunkedClusterList(clist);
      }
    }
    return;
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  ** 
  ** Get inner similarity
  */

  public static double getInnerSimilarity(ClusterNode left, ClusterNode right) {
    ClusterNode leftRight = left.getRightChild();
    ClusterNode rightLeft = right.getLeftChild(); 
    return (leftRight.similarity(rightLeft));
  }   
  
  /***************************************************************************
  ** 
  ** Get outer similarity
  */

  public static double getOuterSimilarity(ClusterNode left, ClusterNode right) {
    ClusterNode leftLeft = left.getLeftChild();
    ClusterNode rightRight = right.getRightChild(); 
    return (leftLeft.similarity(rightRight));
  }    
}
