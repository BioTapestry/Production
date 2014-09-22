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


package org.systemsbiology.biotapestry.embedded;

import java.util.HashSet;

import java.util.Set;
import org.systemsbiology.biotapestry.event.ChangeEvent;

/****************************************************************************
**
** Used to notify of node and link selections to a listener that is using 
** an embedded BioTapestry panel
*/

public class ExternalSelectionChangeEvent implements ChangeEvent {
  
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
  
  private String modelID_;
  private HashSet linkSelections_;
  private HashSet nodeSelections_;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public ExternalSelectionChangeEvent(String modelID, Set nodes, Set links) {
    modelID_ = modelID;
    nodeSelections_ = new HashSet(nodes);
    linkSelections_ = new HashSet(links);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the selected model ID (note: can be null)
  */ 
  
  public String getSelectedModel() {
    return (modelID_);
  }

  /***************************************************************************
  **
  ** Get the link selections
  */ 
  
  public Set getLinkSelections() {
    return (new HashSet(linkSelections_));
  }

  /***************************************************************************
  **
  ** Get the node selections
  */ 
  
  public Set getNodeSelections() {
    return (new HashSet(nodeSelections_));
  }
}
