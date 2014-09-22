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

import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;

/****************************************************************************
**
** Cross-Platform Keypress action
*/

public class XPlatKeypressAction {

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
 
  private FlowMeister.KeyFlowKey actionKey_;
  private ControlFlow.OptArgs optArgs_;
  private HashSet<String> keyStrokeTags_;
  private String actionMapTag_;
  private int event_;
  private int mask_;
  private boolean blockable_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatKeypressAction(FlowMeister.KeyFlowKey actionKey, Set<String> keyStrokeTags, String actionMapTag, boolean blockable) {
    actionKey_ = actionKey;
    keyStrokeTags_ = new HashSet<String>(keyStrokeTags);
    actionMapTag_ = actionMapTag;
    optArgs_ = null;
    blockable_ = blockable;
  }
    
  /***************************************************************************
  **
  ** Constructor 
  */ 
    
  public XPlatKeypressAction(FlowMeister.KeyFlowKey actionKey, int event, int mask, String actionMapTag, boolean blockable) {
    actionKey_ = actionKey;
    keyStrokeTags_ = null;
    event_ = event;
    mask_ = mask;
    actionMapTag_ = actionMapTag;
    optArgs_ = null;
    blockable_ = blockable;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the key
  */ 
   
  public FlowMeister.KeyFlowKey getKey() {
    return (actionKey_);
  }
  
  /***************************************************************************
  **
  ** Answer if we use stroke tags
  */ 
   
  public boolean hasStrokeTags() {
    return (keyStrokeTags_ != null);
  }
  
  /***************************************************************************
  **
  ** Get stroke tags
  */ 
   
  public Set<String> getStrokeTags() {
    return (keyStrokeTags_);
  }
  
  /***************************************************************************
  **
  ** Get action map key
  */ 
   
  public String getActionMapTag() {
    return (actionMapTag_);
  }
  
  /***************************************************************************
  **
  ** Get event
  */ 
   
  public int getEvent() {
    return (event_);
  }
 
  /***************************************************************************
  **
  ** Get mask
  */ 
   
  public int getMask() {
    return (mask_);
  }
  
  /***************************************************************************
  **
  ** Can the key be blocked?
  */ 
   
  public boolean isBlockable() {
    return (blockable_);
  }
    
  /***************************************************************************
  **
  ** Get the (may be null) Action argument
  */ 
   
  public ControlFlow.OptArgs getActionArg() {
    return (optArgs_);
  }   
}
