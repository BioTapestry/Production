/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.event;

import org.systemsbiology.biotapestry.nav.NavTree;

/****************************************************************************
**
** Used to signal changes in tree nodes (currently group nodes)
*/

public class TreeNodeChangeEvent implements ChangeEvent {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public enum Change {NO_CHANGE, GROUP_NODE_CHANGE};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private NavTree.NodeID nodeKey_;
  private Change changeType_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public TreeNodeChangeEvent(NavTree.NodeID nodeKey, Change changeType) {
    nodeKey_ = nodeKey;
    changeType_ = changeType;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the node ID
  */ 
  
  public NavTree.NodeID getNodeID() {
    return (nodeKey_);
  }
  
  /***************************************************************************
  **
  ** Get the change type
  */ 
  
  public Change getChangeType() {
    return (changeType_);
  } 

  /***************************************************************************
  **
  ** Standard equals
  */     
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TreeNodeChangeEvent)) {
      return (false);
    }
    TreeNodeChangeEvent otherLCE = (TreeNodeChangeEvent)other;
    if (this.nodeKey_ == null) {
      return (otherLCE.nodeKey_ == null);
    }
    if (!this.nodeKey_.equals(otherLCE.nodeKey_)) {
      return (false);
    }
    
    return (this.changeType_ == otherLCE.changeType_);
  }   
}
