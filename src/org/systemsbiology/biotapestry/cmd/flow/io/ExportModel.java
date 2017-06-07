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


package org.systemsbiology.biotapestry.cmd.flow.io;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Json export
*/

public class ExportModel extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private BTState appState_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public ExportModel(BTState appState) {
    appState_ = appState;
    name = "command.ExportJSON";
    desc = "command.ExportJSON";
    mnem = "command.ExportJSONMnem";
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (true);  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx, appState_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
  
    while (true) {
      StepState ans;
      if (last == null) {
        throw new IllegalArgumentException();
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState {

    private String genomeID_;
    private double zoom_;
    private String holdKey_;
    private boolean swapOut_;
    private BTState appState_;

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx, BTState appState) {
      super(dacx);
      nextStep_ = "stepToProcess";
      appState_ = appState;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh, BTState appState) {
      super(cfh);
      nextStep_ = "stepToProcess";
      appState_ = appState;
    }
 
    /***************************************************************************
    **
    ** Set the  params
    */ 
        
    public void setParams(String genomeID) {
      String currGenome = dacx_.getCurrentGenomeID();
      zoom_ = dacx_.getZoomTarget().getZoomFactor();
      if ((currGenome != null) && currGenome.equals(genomeID)) {
        swapOut_ = false;
        return;
      }
      genomeID_ = genomeID;     
      holdKey_ = currGenome;
      swapOut_ = true;
      return;
     }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
    	Map<String,Object> modelMap = null;
    	
      try {
        if (swapOut_) {
          String layoutID = dacx_.getLayoutSource().mapGenomeKeyToLayoutKey(genomeID_);
          appState_.setGraphLayout(layoutID);
          // Catch selection clear to undo queue the first time through:        
          UndoSupport fakeSupport_ = uFac_.provideUndoSupport("neverClosed", dacx_);
          appState_.setGenome(genomeID_, fakeSupport_, dacx_);
        }
        
        modelMap = uics_.getSUPanel().exportModelMap(uics_, dacx_, true, 1.0, null);
        
        if (swapOut_) {
          String layoutID = dacx_.getLayoutSource().mapGenomeKeyToLayoutKey(holdKey_);
          appState_.setGraphLayout(layoutID);
          appState_.setGenomeForUndo(holdKey_, dacx_);
        }
        dacx_.getZoomTarget().setZoomFactor(zoom_);       
      } catch (IOException ioe) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));
    }
  }
}
