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


package org.systemsbiology.biotapestry.cmd.flow.io;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Json export for a model of a path 
*/

public class ExportPathModel extends AbstractControlFlow {
  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
   
  public ExportPathModel(BTState appState) {
    super(appState);
    name = "command.ExportPathJSON";
    desc = "command.ExportPathJSON";
    mnem = "command.ExportPathJSONMnem";
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, dacx);
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {

    private String nextStep_;
    private BTState appState_;
    private String genomeID_;
    private DataAccessContext dacx_;
    private double zoom_;
    private String holdKey_;
    private boolean swapOut_;

    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
 
    /***************************************************************************
    **
    ** Set the  params
    */ 
        
    public void setParams(String genomeID) {
      String currGenome = dacx_.getGenomeID();
      zoom_ = appState_.getZoomTarget().getZoomFactor();
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
          String layoutID = appState_.getLayoutMgr().getLayout(genomeID_);
          appState_.setGraphLayout(layoutID);
          // Catch selection clear to undo queue the first time through:        
          UndoSupport fakeSupport_ = new UndoSupport(appState_, "neverClosed");
          appState_.setGenome(genomeID_, fakeSupport_, dacx_);
        }
        
        modelMap = appState_.getSUPanel().exportModelMap(true, 1.0, null, appState_);
        
        if (swapOut_) {
          String layoutID = appState_.getLayoutMgr().getLayout(holdKey_);
          appState_.setGraphLayout(layoutID);
          appState_.setGenomeForUndo(holdKey_, dacx_);
        }
        appState_.getZoomTarget().setZoomFactor(zoom_);       
      } catch (IOException ioe) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, modelMap));
    }
  }
}
