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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.util.Map;

import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle centering current model
*/

public class CenterCurrent extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CenterCurrent() {
    name =  "command.CenterModelOnWorksheet";
    desc = "command.CenterModelOnWorksheet";
    mnem =  "command.CenterModelOnWorksheetMnem";
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.genomeNotEmpty());
  }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(cfh);
        next = ans.stepDoIt();    
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

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
    }
 
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {
                   
      Map<String, Layout.OverlayKeySet>  globalKeys = dacx_.getFGHO().fullModuleKeysPerLayout();    
      UndoSupport support = uFac_.provideUndoSupport("undo.centerCurrentLayout", dacx_);  
      Layout.OverlayKeySet loModKeys = globalKeys.get(dacx_.getCurrentLayoutID());   
  
      DatabaseChange dc = dacx_.getLayoutSource().startLayoutUndoTransaction(dacx_.getCurrentLayoutID());
      dacx_.getCurrentLayout().recenterLayout(dacx_.getZoomTarget().getRawCenterPoint(), dacx_,
                                       true, true, true, null, null, null, loModKeys, null);
      dc = dacx_.getLayoutSource().finishLayoutUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      LayoutChangeEvent lcev = new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);
      support.finish();  
      uics_.getZoomCommandSupport().zoomToFullWorksheet();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
