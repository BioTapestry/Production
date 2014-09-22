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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;

/****************************************************************************
**
** Leaf of a cluster tree
*/

public class ClusterLeaf extends ClusterNode {
  
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
  
  private boolean[] inputs_;
  private String nodeID_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterLeaf(String nodeID, SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, Integer> sourceOrder) {
    super(sourceOrder.size());
    
    nodeID_ = nodeID;
    inputs_ = new boolean[sourceOrder.size()];
 
    Set<String> sources = nps.getSources(nodeID);
    Iterator<String> sit = sources.iterator();
    while (sit.hasNext()) {
      String src = sit.next();
      Integer index = sourceOrder.get(src);
      inputs_[index.intValue()] = true;
      vector_[index.intValue()] = 1.0;
    }
    normalize();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Answer if the given leaf is from an identical base cluster
  */

  public boolean identicalCluster(ClusterLeaf other) {
    
    int inputSize = inputs_.length;
    for (int i = 0; i < inputSize; i++) {
      if (this.inputs_[i] != other.inputs_[i]) {
        return (false);
      }
    }
    return (true);
  }
  
 /***************************************************************************
  ** 
  ** Return the number of active inputs
  */

  public int numSources() {
    int retval = 0;
    int inputSize = inputs_.length;
    for (int i = 0; i < inputSize; i++) {
      if (inputs_[i]) {
        retval++;
      }
    }
    return (retval);
  }   
  
 /***************************************************************************
  ** 
  ** Add to the in-order traversal
  */

  public void appendToClusterList(List<String> clist) {
    clist.add(nodeID_);
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Add to the chunked in-order traversal. Actually gotta use the above function!
  */

  public void appendToChunkedClusterList(List<ClusterBuilder.IntAnnotatedList> clist) {
    throw new IllegalStateException();
  }

  /***************************************************************************
  ** 
  ** Get the left kid
  */

  public ClusterNode getLeftChild() {
    return (this);
  }
  
  /***************************************************************************
  ** 
  ** Get the right kid
  */

  public ClusterNode getRightChild() {
    return (this);
  }
  
  /***************************************************************************
  ** 
  ** Get a hashable vec
  */

  public HashableVec getHashableVec() {
    return (new HashableVec(inputs_));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
         
  public static class HashableVec {

    private boolean[] inputs_;
    
    HashableVec(boolean[] inputs) {
      inputs_ = inputs;
    }

    public int hashCode() {
      int retval = 0;
      int inputSize = inputs_.length;
      for (int i = 0; i < inputSize; i++) {
        if (inputs_[i]) {
          retval += i % 7;
        }
      }
      return (retval);
    }

    public String toString() {
      return ("HashableVec: " + inputs_);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof HashableVec)) {
        return (false);
      }
      
      HashableVec otherHV = (HashableVec)other;
      int inputSize = this.inputs_.length;
      int ois = otherHV.inputs_.length;
            
      if (ois != inputSize) {
        return (false);
      }
 
      for (int i = 0; i < inputSize; i++) {
        if (this.inputs_[i] != otherHV.inputs_[i]) {
          return (false);
        }
      }
      return (true);
    } 
  }
}
