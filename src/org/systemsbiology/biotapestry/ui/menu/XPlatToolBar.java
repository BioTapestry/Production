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

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;

/****************************************************************************
**
** Cross-Platform Tool Bar
*/

public class XPlatToolBar implements XPlatGenericMenu {

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
 
  public ArrayList<BarMember> actions_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatToolBar() {
    actions_ = new ArrayList<BarMember>();
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Add an Item
  */ 
   
  public void addItem(BarMember xpm) {
    actions_.add(xpm);
    return;
  }
  
  /***************************************************************************
  **
  ** Item Iterator
  */ 
   
  public Iterator<BarMember> getItems() {
    return (actions_.iterator());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // MARKER INTERFACE
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  public interface BarMember { 
    public String getCondition();
    public XPlatMenuItem.XPType getType();
    public String getTag();
  } 
  
  public interface BarAction extends BarMember { 
    public FlowMeister.FlowKey getKey();
    public String getIcon();
  }
    
}
