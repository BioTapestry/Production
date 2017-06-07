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

import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.dialogs.DevelopmentSpecDialog;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;

/****************************************************************************
**
** Handle launching the interaction worksheet
*/

public class LaunchBuilder extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean forPopup_;
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
  
  public LaunchBuilder(boolean forPopup, BuilderPluginArg args) {
    forPopup_ = forPopup;
    whichPlugin_ = args.getBuilderIndex();
    name = (forPopup_) ? "linkPopup.launchWorksheetForLink" : "command.LaunchWorksheet";
    desc = (forPopup_) ? "linkPopup.launchWorksheetForLink" : "command.LaunchWorksheet";
    mnem = (forPopup_) ? "linkPopup.launchWorksheetForLinkMnem" : "command.LaunchWorksheetMnem";  
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
    if (forPopup_) {
      throw new IllegalStateException();
    }
    if (cache.noModel()) {
      return (false);
    }
    return (cache.timeCourseNotEmpty());
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!forPopup_) {
      throw new IllegalStateException();
    }
    if (!isSingleSeg) {
      return (false);
    }    
    return (true);
  }             
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) { 
    if (!forPopup_) {
      throw new IllegalStateException();
    }
    return (new StepState(forPopup_, whichPlugin_, dacx));
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
        StepState ans = new StepState(forPopup_, whichPlugin_, cfh);
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, GraphSearcher.CriteriaJudge {
    
    private Intersection intersect_;
    private int myBuilderIndex_;  
    private boolean myForPop_;
    private List<ModelBuilder.Request> requestList_;   
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forPop, int whichPlugin, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
      myForPop_ = forPop;
      myBuilderIndex_ = whichPlugin;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forPop, int whichPlugin, ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
      myForPop_ = forPop;
      myBuilderIndex_ = whichPlugin;
    }

    /***************************************************************************
    **
    ** GraphSearcher.CriteriaJudge interface
    */ 
    
    public boolean stopHere(String nodeID) {
      if (intersect_.getObjectID().equals(nodeID)) {
        return (false);
      } 
      ModelBuilderPlugIn stub = uics_.getPlugInMgr().getBuilderPlugin(myBuilderIndex_); 
      ModelBuilder.IDMapper tracker = stub.getIDMapper();
      return (tracker.getIDForNode(GenomeItemInstance.getBaseID(nodeID)) != null);          
    }
    
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      if (!myForPop_) {
        throw new IllegalStateException();
      }   
      intersect_ = inter;
      return;
    } 
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setReqList(List<ModelBuilder.Request> requestList) {
      if (!myForPop_) {
        throw new IllegalStateException();
      }   
      requestList_ = requestList;
      return;
    } 
   
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {
      
      CommonView cview = uics_.getCommonView();      
      ModelBuilderPlugIn stub = uics_.getPlugInMgr().getBuilderPlugin(myBuilderIndex_);
      
      if (myForPop_) {
        if (requestList_ == null) {
          BusProperties bp = dacx_.getCurrentLayout().getLinkProperties(intersect_.getObjectID());
          LinkSegmentID segID = intersect_.segmentIDFromIntersect();
          Set<String> linksThru = bp.resolveLinkagesThroughSegment(segID);
          ModelBuilder.Resolver mdr = stub.getResolver();
          requestList_ = mdr.resolve(dacx_.getCurrentGenome(), linksThru);
        }
        if (cview.builderDashboardIsPresent()) {
          ModelBuilder.Dashboard dashboard = cview.getBuilderDashboard();
          dashboard.pullToFront();
          if (requestList_ != null) {
            dashboard.selectModelRequests(requestList_);
          }
        } else {         
          ModelBuilder.Dashboard dashboard = stub.getDashboard();
          cview.installBuilderDashboard(dashboard);
          if (requestList_ != null) {
            dashboard.queueModelRequests(requestList_);
          }
          dashboard.launch();
        } 
      } else { // Just launch worksheet
        if (cview.builderDashboardIsPresent()) {
          ModelBuilder.Dashboard dashboard = cview.getBuilderDashboard();
          dashboard.pullToFront();
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
        }
        // FIXME: Does this apply to the link case as well???
        TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
        if (!tcd.hierarchyIsSet()) {    
          boolean done = DevelopmentSpecDialog.assembleAndApplyHierarchy(uics_, dacx_, uFac_);  
          if (!done) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
          }
        }
        ModelBuilder.Dashboard dashboard = stub.getDashboard();
        cview.installBuilderDashboard(dashboard);
        dashboard.launch(); 
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
