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

public class XPlatToggleAction extends XPlatAbstractAction {

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
 
  protected Boolean isChecked_;
  protected String radioFamily_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, boolean fakie, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, tag, null, false, null);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition, boolean withIcon, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, tag, condition, withIcon, null);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  } 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, null, null, false, null);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, boolean withIcon, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, null, null, withIcon, null);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, tag, condition, false, null);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  } 
   
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatToggleAction(FlowMeister flom, ResourceManager rMan, FlowMeister.FlowKey actionKey, String tag, String condition, ControlFlow.OptArgs args, String radioFamily) {
    super(XPType.CHECKBOX_ACTION, rMan, flom, actionKey, tag, condition, false, args);
    isChecked_ = null;
    radioFamily_ = radioFamily;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Get the (optional) starts off checked state.  May be null
  */ 
   
  public Boolean getChecked() {
    return (isChecked_);
  }
  
  public Boolean getIsChecked() {
	    return (isChecked_);
	  }
  
  /***************************************************************************
  **
  ** Set the (optional) starts off checked state.
  */ 
   
  public void setChecked(boolean isChecked) {
    isChecked_ = Boolean.valueOf(isChecked);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the (optional) radio family tag.  May be null
  */ 
   
  public String getRadioFamily() {
    return (radioFamily_);
  } 
}
