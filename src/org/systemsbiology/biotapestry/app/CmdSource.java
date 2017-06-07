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

package org.systemsbiology.biotapestry.app;

import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.GroupPanelCommands;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateDown;

/****************************************************************************
**
** Working to get the kitchen-sink dependency on BTState object across the
** program. This provides command sources.
*/

public class CmdSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // EXPORTED CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Persistent modifiers for search 
  */ 
    
  public static class SearchModifiers  {
    public boolean includeLinks;
    public boolean appendToCurrent;
    public boolean includeQueryNode;                  
  
    SearchModifiers() {
     // Init to legacy options
      includeLinks = true;
      includeQueryNode = true;
      appendToCurrent = false;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private BTState appState_;
  private MainCommands mcmd_;
  private FlowMeister flom_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  CmdSource(BTState appState, UIComponentSource uics) {
    appState_ = appState;
    flom_ = new FlowMeister(appState_, uics);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /****************************************************************************
  **
  ** Get Main Commands
  */  
  
  public MainCommands getMainCmds() {
    return (mcmd_);
  }
  
  /****************************************************************************
  **
  ** set Main Commands
  */  
  
  public void setMainCmds(MainCommands mcmd) {
    mcmd_ = mcmd;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the Flow meister
  */
  
  public FlowMeister getFloM() {
    return (flom_);  
  }  

  /****************************************************************************
  **
  ** Get the panel commands
  */  
   
  public PanelCommands getPanelCmds() {
    return (appState_.getPanelCmdsX());
  }
  
  /****************************************************************************
  **
  ** Get Group Panel Commands
  */  
  
  public GroupPanelCommands getGroupPanelCmds() {
    return (appState_.getGroupPanelCmdsX());
  }
  
  /****************************************************************************
  **
  ** set Group Panel Commands
  */  
  
  public void setGroupPanelCmds(GroupPanelCommands gpCmds) {
    appState_.setGroupPanelCmdsX(gpCmds);
    return;
  }

  /***************************************************************************
  **
  ** Return last down prop target
  */
  
  public PropagateDown.DownPropState getDownProp() {
    return (appState_.getDownProp());
  }
  
  /****************************************************************************
  **
  ** set Panel Commands
  */  
  
  public void setPanelCmds(PanelCommands pCmds) {
    appState_.setPanelCmdsX(pCmds);
    return;
  }
  
  /***************************************************************************
  **
  ** Are we doing pulldown?
  */
  
  public boolean getAmPulling() {
    return (appState_.getAmPulling());  
  }   

  /***************************************************************************
  **
  ** Set if we doing pulldown
  */
  
  public void setAmPulling(boolean amPulling) {
    appState_.setAmPulling(amPulling);
    return;
  }
  
   /****************************************************************************
   **
   ** Get Pop Commands
   */  
   
   public PopCommands getPopCmds() {
     return (appState_.getPopCmdsX());  
   }
   
  /****************************************************************************
  **
  ** set Pop Commands
  */  
  
  public void setPopCmds(PopCommands popCmds) {
    appState_.setPopCmdsX(popCmds);
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if a change has occurred since last clear.  FIX ME: We don't
  ** account for undone/redone changes.
  */
  
  public boolean hasAnUndoChange() {
    return (appState_.hasAnUndoChange());
  }
   
  /***************************************************************************
  **
  ** Used to get the undo manager
  */
  
  public UndoManager getUndoManager() {
    return (appState_.getUndoManager());
  }  
  
  /***************************************************************************
  **
  ** Bump up undo count
  */
  
  public void bumpUndoCount() {
    appState_.bumpUndoCount();
    return;
  }   

  /***************************************************************************
  **
  ** Clear the change tracking
  */
  
  public void clearUndoTracking() {
    appState_.clearUndoTracking();
    return;
  }  
  
  /***************************************************************************
  **
  ** Search modifiers
  */
  
  public CmdSource.SearchModifiers getSearchModifiers() {
    return (appState_.getSearchModifiersX());
  }
  
  /***************************************************************************
  **
  ** Set the state to show module components.
  */
  
  public void toggleModuleComponents() {
    appState_.toggleModuleComponentsX();
    return;
  }
  
}
