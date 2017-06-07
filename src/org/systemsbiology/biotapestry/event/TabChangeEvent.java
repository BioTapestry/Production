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

/****************************************************************************
**
** Used to signal changes in the tabs
*/

public class TabChangeEvent implements ChangeEvent {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public enum Change {NO_CHANGE, UNSPECIFIED_CHANGE, RENAME, DELETE, ADD, SWITCHED}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private String tabKey_;
  private String oldTabKey_;
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
  
  public TabChangeEvent(String tabKey, Change changeType) {
    tabKey_ = tabKey;
    oldTabKey_ = null;
    changeType_ = changeType;
  }

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public TabChangeEvent(String tabKey, String oldTabKey, Change changeType) {
    tabKey_ = tabKey;
    oldTabKey_ = oldTabKey;
    changeType_ = changeType;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the tab key
  */ 
  
  public String getTabKey() {
    return (tabKey_);
  }

  /***************************************************************************
  **
  ** Get the proxy key
  */ 
  
  public String getOldTabKey() {
    return (oldTabKey_);
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
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TabChangeEvent)) {
      return (false);
    }
    TabChangeEvent otherMCE = (TabChangeEvent)other;

    if (this.changeType_ != otherMCE.changeType_) {
      return (false);
    }
    
    if (!this.tabKey_.equals(otherMCE.tabKey_)) {
      return (false);
    }
    
    if (this.oldTabKey_ == null) {
      return (otherMCE.oldTabKey_ == null);
    } else if (otherMCE.oldTabKey_ == null) {
      return (this.oldTabKey_ == null);
    }   
    return (this.oldTabKey_.equals(otherMCE.oldTabKey_));
  }     
}
