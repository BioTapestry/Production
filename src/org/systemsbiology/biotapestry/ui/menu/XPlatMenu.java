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

import java.util.ArrayList;
import java.util.Iterator;

/****************************************************************************
**
** Cross-Platform Menu
*/

public class XPlatMenu extends XPlatMenuItem implements XPlatGenericMenu {

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
 
  public ArrayList<XPlatMenuItem> items_;
  private String name_;
  private char mnem_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMenu() {
    this(null, Character.MIN_VALUE, null, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMenu(String name, char mnem) {
    this(name, mnem, null, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMenu(String name, char mnem, String tag, String condition) {
    super(XPType.MENU, tag, condition);
    name_ = name;
    mnem_ = mnem;  
    items_ = new ArrayList<XPlatMenuItem>();
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMenu(String name, char mnem, String tag) {
    this(name, mnem, tag, null);
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
  ** Get the mnem
  */ 
   
  public char getMnem() {
    return (mnem_);
  }
 
  /***************************************************************************
  **
  ** Add a Menu
  */ 
   
  public void addItem(XPlatMenuItem xpmi) {
    items_.add(xpmi);
    return;
  }
  
  /***************************************************************************
  **
  ** Menu Iterator
  */ 
   
  public Iterator<XPlatMenuItem> getItems() {
    return (items_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the size
  */ 
   
  public int getSize() {
    return (items_.size());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  @Override
  public String toString() {
    return ("XPlatMenu: " + name_);
  }
}
