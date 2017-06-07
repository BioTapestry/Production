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

package org.systemsbiology.biotapestry.cmd.flow.netBuild;

import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;

/****************************************************************************
**
** Handle launching the link drawing tracker
*/

public class LaunchLinkDrawTracker extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private int whichPlugin_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */
  
  public LaunchLinkDrawTracker(BuilderPluginArg args) {
    whichPlugin_ = args.getBuilderIndex();
    name =  "command.LaunchLinkDrawingTracker";
    desc =  "command.LaunchLinkDrawingTracker";
    mnem =  "command.LaunchLinkDrawingTrackerMnem";
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
   
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    if (cache.noModel()) {
      return (false);
    }
    return (cache.timeCourseNotEmpty());
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
        StepState ans = new StepState(whichPlugin_, cfh);
        next = ans.stepDoIt();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepDoIt")) {
          next = ans.stepDoIt();      
        } else {
          throw new IllegalStateException();
        }
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
    
    private int myBuilderIndex_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(int whichPlugin, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
      myBuilderIndex_ = whichPlugin;
    }
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(int whichPlugin, ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
      myBuilderIndex_ = whichPlugin;
    }

    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {
      ModelBuilderPlugIn stub = uics_.getPlugInMgr().getBuilderPlugin(myBuilderIndex_);
      
      CommonView cview = uics_.getCommonView();    
      if (!cview.haveWorksheetLinkDrawingTracker()) {
        ModelBuilder.LinkDrawingTracker ldt = stub.getTracker();
        cview.installLinkDrawingTracker(ldt);
        ldt.launch();
      }  
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
