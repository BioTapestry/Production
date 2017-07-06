/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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

/****************************************************************************
**
** Used to signal general changes
*/

public class GeneralChangeEvent implements ChangeEvent {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public enum ChangeType {
    NO_CHANGE, UNSPECIFIED_CHANGE, MODEL_DATA_CHANGE, PERTURB_DATA_CHANGE
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ChangeType changeType_;
  private String tabID_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public GeneralChangeEvent(int dummy, String tabID, ChangeType changeType) {
    tabID_ = tabID;
    changeType_ = changeType;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the tab ID
  */ 
  
  public String getTabID() {
    return (tabID_);
  } 
  
  /***************************************************************************
  **
  ** Get the change type
  */ 
  
  public ChangeType getChangeType() {
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
    if (!(other instanceof GeneralChangeEvent)) {
      return (false);
    }
    GeneralChangeEvent otherGCE = (GeneralChangeEvent)other;
    if (!this.tabID_.equals(otherGCE.tabID_)) {
      return (false);
    }
    
    return (this.changeType_ == otherGCE.changeType_);
  }     
}
