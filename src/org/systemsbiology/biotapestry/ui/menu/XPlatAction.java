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

import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Cross-Platform Menu Item
*/

public class XPlatAction extends XPlatAbstractAction {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey) {
    super(XPType.ACTION, rMan, flom, actionKey, null, null, false, null);
  }

  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, boolean withIcon) {
    super(XPType.ACTION, rMan, flom, actionKey, null, null, withIcon, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag) {
    super(XPType.ACTION, rMan, flom, actionKey, tag, null, false, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition) {
    super(XPType.ACTION, rMan, flom, actionKey, tag, condition, false, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition, boolean withIcon) {
    super(XPType.ACTION, rMan, flom, actionKey, tag, condition, withIcon, null);
  }
  
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition,ControlFlow.OptArgs args) {
    super(XPType.ACTION, rMan, flom, actionKey, tag, condition, false, args);
  }
   
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition, boolean withIcon, ControlFlow.OptArgs args) {
    super(XPType.ACTION, rMan, flom, actionKey, tag, condition, withIcon, args);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  @Override
  public String toString() {
    return ("XPlatAction: " + actionKey_);
  }

}
