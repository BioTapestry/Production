/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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
** Tiny little support class used as keys to map CSV entities to internal
** IDS following a CSV load.
*/

public class NodeRegionModelNameTuple implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String nodeName_;
  private String regionName_;
  private String modelName_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public NodeRegionModelNameTuple(String nodeName, String regionName, String modelName) {
    nodeName_ = nodeName;
    regionName_ = regionName;
    modelName_ = modelName;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get node name
  */

  public String getNodeName() {
    return (nodeName_);
  } 
  
  /***************************************************************************
  ** 
  ** Get region name (null for top model)
  */

  public String getRegionName() {
    return (regionName_);
  } 
  
  /***************************************************************************
  ** 
  ** Get the model Name
  */

  public String getModelName() {
    return (modelName_);
  } 
  
  /***************************************************************************
  ** 
  ** Clone support
  */
  
  public Object clone() {
    try {
      // Strings don't need cloning:
      NodeRegionModelNameTuple retval = (NodeRegionModelNameTuple)super.clone();
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
    return (nodeName_.hashCode() + ((regionName_ == null) ? 0 : regionName_.hashCode()) + modelName_.hashCode());
  }

  /***************************************************************************
  ** 
  ** Get the to string
  */
  
  public String toString() {
    return ("NodeRegionModelNameTuple: node = " + nodeName_ + " region = " + regionName_ + " model = " + modelName_);
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
    if (!(other instanceof NodeRegionModelNameTuple)) {
      return (false);
    }
    NodeRegionModelNameTuple otherTup = (NodeRegionModelNameTuple)other;
    
    if (!this.nodeName_.equals(otherTup.nodeName_)) {
      return (false);
    }
    
    if (!this.modelName_.equals(otherTup.modelName_)) {
      return (false);
    }
    
    if (this.regionName_ == null) {
      return (otherTup.regionName_ == null);
    }   
   
    return (this.regionName_.equals(otherTup.regionName_));
  }   
}
