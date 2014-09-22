/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.ui.menu;

/****************************************************************************
**
** Cross-Platform Menu Item
*/

public abstract class XPlatMenuItem {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public enum XPType {MENU, SEPARATOR, ACTION, CHECKBOX_ACTION, MENU_PLACEHOLDER};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  protected XPType type_;
  protected String tag_;
  protected String condition_;
  protected Boolean isEnabled_;
  protected Boolean enabled_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMenuItem(XPType xpt, String tag, String condition) {
    type_ = xpt;
    tag_ = tag;
    condition_ = condition;
    isEnabled_ = null;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the type
  */ 
   
  public XPType getType() {
    return (this.type_);
  }
  
  /***************************************************************************
  **
  ** Get the (optional) tag.  May be null
  */ 
   
  public String getTag() {
    return (this.tag_);
  }
  
  /***************************************************************************
  **
  ** Get the (optional) condition.  May be null
  */ 
   
  public String getCondition() {
    return (this.condition_);
  }
  
  /***************************************************************************
  **
  ** Get the (optional) enabled state.  May be null
  */ 
  
  public Boolean getEnabled() {
	  return (this.isEnabled_);
  }
   
  public Boolean getIsEnabled() {
	  return (this.isEnabled_);  
  }
  
  /***************************************************************************
  **
  ** Set the (optional) enabled state.
  */ 
   
  public void setEnabled(boolean isEnabled) {
    isEnabled_ = Boolean.valueOf(isEnabled);
    return;
  }
}
