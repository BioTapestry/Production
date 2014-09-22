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

public abstract class XPlatAbstractAction extends XPlatMenuItem implements XPlatToolBar.BarAction {

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
 
  protected FlowMeister.FlowKey actionKey_;
  protected String name_;
  protected String desc_;
  protected String icon_;
  protected Character mnem_;
  protected Character accel_;
  protected ControlFlow.OptArgs optArgs_;
  protected String optArgsAsURL_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  protected XPlatAbstractAction(XPType xpt, ResourceManager rMan, FlowMeister flom, FlowMeister.FlowKey actionKey, String tag, String condition, boolean withIcon, ControlFlow.OptArgs args) {
    super(xpt, tag, condition);
    actionKey_ = actionKey;
    ControlFlow cf = flom.getControlFlow(actionKey, args);
    name_ = rMan.getString(cf.getName());
    if (withIcon) {
      desc_ = rMan.getString(cf.getDesc());
      icon_ = cf.getIcon();
   } else {
      String mnemStr = cf.getMnem();
      if (mnemStr != null) {
        char mnemC = rMan.getChar(mnemStr);
        mnem_ = new Character(mnemC);
      }
    }
    String accelStr = cf.getAccel();
    if (accelStr != null) {
      char accelC = rMan.getChar(accelStr);
      accel_ = new Character(accelC);
    }
    optArgs_ = args;
    optArgsAsURL_ = (optArgs_ == null) ? null : optArgs_.getURLArgs();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

   /***************************************************************************
   **
   ** Get properties
   */   
   
   public String getName() {
     return (name_);
   }
   
   public String getDesc() {
     return (desc_);
   }
   
   public String getIcon() {
     return (icon_);
     
   }
   public Character getMnem() {
     return (mnem_);
     
   }
   public Character getAccel() {
     return (accel_);      
   }  
   
  /***************************************************************************
  **
  ** Get the key
  */ 
   
  public FlowMeister.FlowKey getKey() {
    return (actionKey_);
  }
  
  public FlowMeister.FlowKey.FlowType getKeyType() {
	    return (actionKey_.getFlowType());
  }
  
  /***************************************************************************
  **
  ** Get the (may be null) Action argument
  */ 
   
  public ControlFlow.OptArgs getActionArg() {
    return (optArgs_);
  }   
  
  public String getActionArgAsURLArgs() {
    return (optArgsAsURL_);
  }
}
