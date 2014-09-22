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


package org.systemsbiology.biotapestry.ui.menu;

import java.util.ArrayList;
import java.util.Iterator;

/****************************************************************************
**
** Cross-Platform Combo Box
*/

public class XPlatComboBox {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public ArrayList<XPlatComboOption> items_;
  private String name_;
  private String condition_;
  private Boolean isEnabled_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatComboBox(String name) {
    this(name, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatComboBox(String name, String condition) {
    name_ = name;
    condition_ = condition;  
    items_ = new ArrayList<XPlatComboOption>();
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the name
  */ 
   
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Add a Menu
  */ 
   
  public void addItem(XPlatComboOption xpco) {
    items_.add(xpco);
    return;
  }
  
  /***************************************************************************
  **
  ** Menu Iterator
  */ 
   
  public Iterator<XPlatComboOption> getItems() {
    return (items_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the (optional) condition.  May be null
  */ 
   
  public String getCondition() {
    return (condition_);
  }
  
  /***************************************************************************
  **
  ** Get the (optional) enabled state.  May be null
  */ 
   
  public Boolean getEnabled() {
    return (isEnabled_);
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
