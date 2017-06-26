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

package org.systemsbiology.biotapestry.cmd;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;

/***************************************************************************
** 
** These commands handle mouse events for the group display
*/

public class GroupPanelCommands  {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  //
  // Operational modes:
  //
  
  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Mode {  
    CLICK_TO_NAVIGATE,
    ;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ClickHandler currentClickHandler_;
  private HarnessBuilder hb_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public GroupPanelCommands(HarnessBuilder hb) {
    hb_ = hb;
    currentClickHandler_ = new ClickHandler();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Main entry point
  */
   
  public void processMouseClick(String modelID, String proxyID, Integer time, String regionID, StaticDataAccessContext dacx) {
    currentClickHandler_.handleMouseClick(modelID, proxyID, time, regionID, dacx);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Mode Handler
  */     
    
  public class ClickHandler { 
    
    //
    // This handler does things differently, since it is _always_ running (unless another mode handler is installed), 
    // and we do not want a long-term control flow. SO the click launches a new flow, instead of sending the click info
    // to an already running flow.
    //
  
    public void handleMouseClick(String modelID, String proxyID, Integer time, String regionID, StaticDataAccessContext dacx) {
      HarnessBuilder.PreHarness ph = hb_.buildHarness(FlowMeister.OtherFlowKey.GROUP_NODE_CLICK);
      SetCurrentModel.StepState agis = (SetCurrentModel.StepState)ph.getCmdState();
      String genomeID;
      if (proxyID != null) {
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(proxyID);
        if (dip.isSingle()) {
          genomeID = dip.getFirstProxiedKey();
        } else {
          genomeID = dip.getKeyForTime(time.intValue(), true);
        }
      } else {
        genomeID = modelID; 
      }
      agis.setPreloadForGroup(genomeID, regionID);
      hb_.runHarness(ph);
      return;
    }
  }
}
