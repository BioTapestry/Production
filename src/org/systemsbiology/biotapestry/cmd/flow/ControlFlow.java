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

package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Point;


import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.ui.Intersection;

/****************************************************************************
**
** Interfaces for control flows
*/

public interface ControlFlow { 
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx);
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last);
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cmds);
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds);
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last);
  public boolean isEnabled(CheckGutsCache cache);   
  public boolean isValid(Intersection inter, boolean singleSegment, boolean canSplit, DataAccessContext rcx, UIComponentSource uics);
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext rcx, UIComponentSource uics);
  
  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  public boolean externallyEnabled();
  
  /***************************************************************************
  **
  ** Answer if we can be pushed
  ** 
  */
 
  public boolean canPush(int pushCondition);

  /***************************************************************************
  **
  ** Signals we are reverse pushed (enabled when others disabled)
  */
 
  public boolean reversePush(int pushCondition);

  public String getName();
  public String getDesc();
  public String getIcon();
  public String getMnem();
  public String getAccel();
 
  
  /***************************************************************************
  **
  ** Interface for flows that support popup toggle states
  */
    
  public interface FlowForPopToggle {
    public boolean shouldCheck(CmdSource cSrc);
  }
  
  /***************************************************************************
  **
  ** Interface for flows that support Main toggle states
  */
    
  public interface FlowForMainToggle {
    public void directCheck(boolean isActive, CmdSource cSrc);
  }
  
  
  /***************************************************************************
  **
  ** Interface for optional arguments
  */
    
  public interface OptArgs {
    public String getURLArgs();
  }
}