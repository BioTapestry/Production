/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;


/****************************************************************************
**
** Tiny little support class used to identify internal IDs of CSV entities
** following a CSV load.
*/

public class ModelNodeIDPair implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String modelID_;
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

  public ModelNodeIDPair(String modelID, String nodeID) {
    modelID_ = modelID;
    nodeID_ = nodeID;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get node ID
  */

  public String getNodeID() {
    return (nodeID_);
  } 
  
  /***************************************************************************
  ** 
  ** Get the model ID
  */

  public String getModelID() {
    return (modelID_);
  } 
  
  /***************************************************************************
  ** 
  ** Clone support
  */
  
  public Object clone() {
    try {
      // Strings don't need cloning:
      ModelNodeIDPair retval = (ModelNodeIDPair)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  ** 
  ** Get the hashCode
  */
  
  public int hashCode() {
    return (nodeID_.hashCode() + modelID_.hashCode());
  }

  /***************************************************************************
  ** 
  ** Get the to string
  */
  
  public String toString() {
    return ("ModelNodeIDPair: nodeID = " + nodeID_ + " modelID = " + modelID_);
  }

  /***************************************************************************
  ** 
  ** Standard equals
  */
  
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof ModelNodeIDPair)) {
      return (false);
    }
    ModelNodeIDPair otherPair = (ModelNodeIDPair)other;
    
    if (!this.nodeID_.equals(otherPair.nodeID_)) {
      return (false);
    }
    
    return (this.modelID_.equals(otherPair.modelID_));
  }
}
