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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;

/****************************************************************************
**
** Provides info on the current menu state
*/

public class XPlatCurrentState {

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
 
  private HashMap<String, XPlatMenu> menuFills_;
  private HashMap<String, XPlatComboBox> comboFills_;
  private HashMap<String, String> mutableNames_;
  private HashMap<String, Boolean> conditionalStates_;
  private Map<FlowMeister.FlowKey, Boolean> fes_;
  private Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> modelTreeState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatCurrentState() { 
    menuFills_ = new HashMap<String, XPlatMenu>();
    comboFills_ = new HashMap<String, XPlatComboBox>();
    mutableNames_ = new HashMap<String, String>();
    conditionalStates_ = new HashMap<String, Boolean>();
    modelTreeState_ = new HashMap<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Some menu definitions have placeholder tags. Here are the current
  ** menu definitions to go there.
  */ 
   
  public void fillInMenu(String key, XPlatMenu fill) {
    menuFills_.put(key, fill);
    return;
  }
  
  /***************************************************************************
  **
  ** Some toolbar definitions have placeholder tags. Here are the current
  ** XPlatComboBoxes to go there.
  */ 
   
  public void fillInCombo(String key, XPlatComboBox fill) {
    comboFills_.put(key, fill);
    return;
  }
  
  /***************************************************************************
  **
  ** Some actions are tagged and have mutable names (e.g. undo redo).
  */ 
   
  public void setMutableActionName(String key, String name) {
    mutableNames_.put(key, name);
    return;
  }
  
  /***************************************************************************
  **
  ** The existence of some controls is conditional
  */ 
   
  public void setConditionalState(String key, boolean isOn) {
    conditionalStates_.put(key, Boolean.valueOf(isOn));
    return;
  }

  /***************************************************************************
  **
  ** Main menubar and toolbar states can change after every action. Here is the data 
  */ 
   
  public void mainCommandEnables(Map<FlowMeister.FlowKey, Boolean> fes) {
    fes_ = fes;
    return;
  }
  
  /***************************************************************************
  **
  ** Get menu for placeholder tag
  */ 
   
  public XPlatMenu getMenu(String key) {
    return (menuFills_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get comboBox for placeholder tag
  */ 
   
  public XPlatComboBox getCombo(String key) {
    return (comboFills_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get the enabled state for a flow key
  */ 
   
  public Boolean isEnabled(FlowMeister.FlowKey fkey) {
    Boolean retval = fes_.get(fkey);
    return ((retval == null) ? true : retval.booleanValue());
  }
    
  /***************************************************************************
  **
  ** Get enabled ModelTreeState
  */ 
   
  public Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> getModelTreeState() {
    return (modelTreeState_);
  }
  
  /***************************************************************************
  **
  ** Set enabled ModelTreeState
  */ 
   
  public void setModelTreeState(Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> treeState) {
    modelTreeState_ = treeState;
    return;
  }

  /***************************************************************************
  **
  ** Get the mutable name for a tagged action
  */ 
   
  public String getMutableActionName(String tag) {
    return (mutableNames_.get(tag));
  }
  
  
  /**
   * Public methods for JSONification
   * 
   * 
   * 
   * 
   * 
   * 
   * @return
   */
  
  
  public Map<String,XPlatMenu> getMenuFills() {
	  if(menuFills_ == null) {
		  return null;
	  }
	  return Collections.unmodifiableMap(menuFills_);
  }
  
  public Map<String,XPlatComboBox> getComboFills() {
	  if(comboFills_ == null) {
		  return null;
	  }
	  return Collections.unmodifiableMap(comboFills_);
  }
  
  public Map<String,String> getMutableNames() {
	  if(mutableNames_ == null) {
		  return null;
	  }
	  return Collections.unmodifiableMap(mutableNames_);
  }
  
  public Map<String,Boolean> getConditionalStates() {
    if(conditionalStates_ == null) {
      return null;
    }
    return Collections.unmodifiableMap(conditionalStates_);
  }
  
  public Map<FlowMeister.FlowKey,Boolean> getFlowEnabledStates() {
	  if(fes_ == null) {
		  return null;
	  }
	  
	  return Collections.unmodifiableMap(fes_);
  }
}
