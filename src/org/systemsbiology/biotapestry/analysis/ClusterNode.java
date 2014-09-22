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

import java.util.List;

/****************************************************************************
**
** Node of a cluster tree
*/

public abstract class ClusterNode {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  protected double[] vector_;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ClusterNode(int numSources) {
    vector_ = new double[numSources]; 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  ** 
  ** Get the vector
  */

  public double[] getVector() {
    return (vector_);
  }
  
  /***************************************************************************
  ** 
  ** Return the similarity measure
  */

  public double similarity(ClusterNode other) {
    return (this.dot(other));
  }
  
 /***************************************************************************
  ** 
  ** Add to the in-order traversal
  */

  public abstract void appendToClusterList(List<String> clist);  

 /***************************************************************************
  ** 
  ** Add to the chunked in-order traversal
  */

  public abstract void appendToChunkedClusterList(List<ClusterBuilder.IntAnnotatedList> clist); 
  
  /***************************************************************************
  ** 
  ** Get the left kid
  */

  public abstract ClusterNode getLeftChild();
  
  /***************************************************************************
  ** 
  ** Get the right kid
  */

  public abstract ClusterNode getRightChild();
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Calculate the dot product
  */
  
  protected double dot(ClusterNode other) {
    int vectorSize = vector_.length;
    double sum = 0.0;
    for (int i = 0; i < vectorSize; i++) {
      sum += (this.vector_[i] * other.vector_[i]);
    }
    return (sum);
  }
  
  /***************************************************************************
  **
  ** Normalize this vector
  */
  
  protected void normalize() {
    double len = length();
    if (len == 0.0) {
      return;
    }
    int vectorSize = vector_.length;
    for (int i = 0; i < vectorSize; i++) {
      vector_[i] /= len;
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Calculate the length
  */
  
  protected double length() {
    return Math.sqrt(this.dot(this));
  }
  
}
