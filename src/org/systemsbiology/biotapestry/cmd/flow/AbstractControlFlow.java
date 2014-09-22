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

package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Point;

import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.ui.Intersection;

/****************************************************************************
**
** Abstract base class
*/

public abstract class AbstractControlFlow implements ControlFlow {

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
 
  protected BTState appState_;
  protected String name;
  protected String desc;
  protected String icon;
  protected String mnem;
  protected String accel;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AbstractControlFlow(BTState appState) {
    appState_ = appState;
    icon = "FIXME24.gif";
    accel = null;  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Required resource strings
  ** 
  */
     
  public String getName(){
    return (name);
  }
   
  public String getDesc() {
    return (desc);
  }
   
  public String getIcon() {
    return (icon);
  }
   
  public String getMnem(){
    return (mnem);
  }
   
  public String getAccel() {
    return (accel);
  }
  
  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  public boolean externallyEnabled() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
    
  public boolean isEnabled(CheckGutsCache cache) {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
    
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
   return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a model tree case
  ** 
  */
  
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext rcx) {
    return (true); 
  }

  /***************************************************************************
  **
  ** Answer if we can be pushed
  ** 
  */
 
  public boolean canPush(int pushCondition) {
    return (true); // Default can always be pushed:
  }
   
  /***************************************************************************
  **
  ** Signals we are reverse pushed (enabled when others disabled)
  */
 
  public boolean reversePush(int pushCondition) {
    return (false);
  }    

  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
   
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    throw new IllegalArgumentException();
  }
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    throw new IllegalStateException();
  } 
  
  /***************************************************************************
  **
  ** Called as new X, Y info comes in to place the node.
  */
  
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
   
  public abstract DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last);
  
  /***************************************************************************
  **
  ** Final visualization step
  ** 
  */
  
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last) {
    return (new VisualChangeResult(true));
  }
}
