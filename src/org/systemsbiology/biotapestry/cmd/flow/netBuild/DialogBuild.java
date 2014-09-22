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

package org.systemsbiology.biotapestry.cmd.flow.netBuild;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.dialogs.BuildNetworkDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.RegionInheritDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.RegionSetupDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParamDialogFactory;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle building network from dialog
*/

public class DialogBuild extends AbstractControlFlow {

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
  
  public DialogBuild(BTState appState) {
    super(appState);
    name =  "command.BuildFromDialog";
    desc =  "command.BuildFromDialog";
    mnem =  "command.BuildFromDialogMnem";
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
    return (cache.isNotDynamicInstance()); 
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
        ans = new StepState(appState_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.cfh == null) {
        ans.cfh = cfh;
      }
      if (ans.getNextStep().equals("stepPreprocessRegions")) {
        next = ans.stepPreprocessRegions();
      } else if (ans.getNextStep().equals("stepProcessEmptyRegionSetup")) {
        next = ans.stepProcessEmptyRegionSetup(last);        
      } else if (ans.getNextStep().equals("stepProcessEmptyInheritRegionSetup")) {
        next = ans.stepProcessEmptyInheritRegionSetup(last);                 
      } else if (ans.getNextStep().equals("stepLaunchDialog")) {
        next = ans.stepLaunchDialog();         
      } else if (ans.getNextStep().equals("stepExtractAndBuildNetwork")) {
        next = ans.stepExtractAndBuildNetwork(last);      
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
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    StepState ans = (StepState)cms;
    if (qbom.getLabel().equals("queBombInstanceMismatch")) {
      return (ans.queBombInstanceMismatch(qbom));
    } else if (qbom.getLabel().equals("queBombPreProcess")) {
      return (ans.queBombPreProcess(qbom));
    } else if (qbom.getLabel().equals("queBombLayoutValues")) {
      return (ans.queBombLayoutValues(qbom));      
      
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, BackgroundWorkerOwner {
    
    private String nextStep_;    
    private BTState appState_;
    private BuildInstructionProcessor bip_;
    private ServerControlFlowHarness cfh;
    private DataAccessContext rcxT_;
    private DataAccessContext rcxR_;
    private BuildNetworkDialogFactory.DesktopDialog horribleHack_;  
    private Genome parent;
    private List<InstanceInstructionSet.RegionInfo> working;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepPreprocessRegions";
      rcxT_ = dacx;
    }
 
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombLayoutValues(RemoteRequest qbom) {
      SpecialtyLayoutEngine.SpecialtyType strat = (SpecialtyLayoutEngine.SpecialtyType)qbom.getObjectArg("strategy");     
      SpecialtyLayout specLayout = SpecialtyLayoutEngine.getLayout(strat, appState_); 
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout.getParameterDialogBuildArgs(rcxT_.getGenome(), null, false);
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      result.setObjAnswer("specLoParams", ba);
      return (result);
    }
    
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombInstanceMismatch(RemoteRequest qbom) {
      List<BuildInstruction> buildCmds = (List<BuildInstruction>)qbom.getObjectArg("buildCmds");
      List<BuildInstructionProcessor.MatchChecker> mismatch = bip_.findInstanceMismatches(rcxT_, buildCmds);
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      result.setObjAnswer("instMismatch", mismatch);
      return (result);
    }
    
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombPreProcess(RemoteRequest qbom) {
      List<BuildInstruction> buildCmds = (List<BuildInstruction>)qbom.getObjectArg("buildCmds");
      BuildInstructionProcessor.BuildChanges bChanges = bip_.preProcess(rcxT_, buildCmds); 
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      result.setObjAnswer("bChanges", bChanges);
      return (result);
    }
 
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepPreprocessRegions() {
      rcxR_ = rcxT_.getContextForRoot();
      parent = null;
      working = null;
      if (!(rcxT_.getGenome() instanceof DBGenome)) {
        InstanceInstructionSet iis = rcxT_.getInstructSrc().getInstanceInstructionSet(rcxT_.getGenomeID());
        parent = rcxT_.getGenomeAsInstance().getVfgParent();
        if (parent != null) {
          InstanceInstructionSet piis = rcxT_.getInstructSrc().getInstanceInstructionSet(parent.getID());
          ArrayList<InstanceInstructionSet.RegionInfo> parentList = new ArrayList<InstanceInstructionSet.RegionInfo>();
          if (piis != null) {
            Iterator<InstanceInstructionSet.RegionInfo>prit = piis.getRegionIterator();  
            while (prit.hasNext()) {
              parentList.add(prit.next());
            }
          }
          working = new ArrayList<InstanceInstructionSet.RegionInfo>();
          if ((iis != null) && (iis.getRegionCount() > 0)) {
            Iterator<InstanceInstructionSet.RegionInfo> rit = iis.getRegionIterator();  
            while (rit.hasNext()) {
              working.add(rit.next());
            }
          }
          if (working.isEmpty() && !parentList.isEmpty()) {
            RegionInheritDialogFactory.BuildArgs ba = 
              new RegionInheritDialogFactory.BuildArgs(rcxT_.getGenome(), parentList, working);
            RegionInheritDialogFactory mddf = new RegionInheritDialogFactory(cfh);   
            ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
            nextStep_ = "stepProcessEmptyInheritRegionSetup";
            return (retval);
          }
          if (working.isEmpty()) {
            RegionSetupDialogFactory.BuildArgs ba = 
              new RegionSetupDialogFactory.BuildArgs(rcxT_.getGenome(), working);
            RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh);   
            ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
            nextStep_ = "stepProcessEmptyRegionSetup";
            return (retval);
          } 
        } else if ((iis == null) || (iis.getRegionCount() == 0)) {
          RegionSetupDialogFactory.BuildArgs ba = 
            new RegionSetupDialogFactory.BuildArgs(rcxT_.getGenome(), working);
          RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh);   
          ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
          nextStep_ = "stepProcessEmptyRegionSetup";
          return (retval);
        } else {
          working = new ArrayList<InstanceInstructionSet.RegionInfo>();
          Iterator<InstanceInstructionSet.RegionInfo> rit = iis.getRegionIterator();  
          while (rit.hasNext()) {
            working.add(rit.next());
          }
        }
      }
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepLaunchDialog";
      return (daipc);     
    }
 
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepLaunchDialog() {  
 
     bip_ = new BuildInstructionProcessor(appState_);
     List<BuildInstruction> instruct = bip_.getInstructions(rcxT_);
      
     BuildNetworkDialogFactory.BuildArgs ba = new BuildNetworkDialogFactory.BuildArgs(rcxT_.getGenome(), parent, working, instruct);
     BuildNetworkDialogFactory bndf = new BuildNetworkDialogFactory(cfh);
     ServerControlFlowHarness.Dialog cfhd = bndf.getDialog(ba);
     DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
     nextStep_ = "stepExtractAndBuildNetwork";
     return (retval);
   } 
     
   /***************************************************************************
   **
   ** Extract and install region data
   */ 
       
   private DialogAndInProcessCmd stepProcessEmptyRegionSetup(DialogAndInProcessCmd cmd) {
     RegionSetupDialogFactory.RegionSetupRequest crq = (RegionSetupDialogFactory.RegionSetupRequest)cmd.cfhui;   
     if (!crq.haveResults()) {
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
       return (retval);
     }
     working = crq.regionResult;
     DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
     nextStep_ = "stepLaunchDialog";
     return (daipc);
   }

   /***************************************************************************
   **
   ** Extract and install region data
   */ 
       
   private DialogAndInProcessCmd stepProcessEmptyInheritRegionSetup(DialogAndInProcessCmd cmd) {
     RegionInheritDialogFactory.RegionInheritRequest crq = (RegionInheritDialogFactory.RegionInheritRequest)cmd.cfhui;   
     if (!crq.haveResults()) {
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
       return (retval);
     }
     working = crq.regionResult;
     if (working.isEmpty()) {
       RegionSetupDialogFactory.BuildArgs ba = 
         new RegionSetupDialogFactory.BuildArgs(rcxT_.getGenome(), working);
       RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh);   
       ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
       nextStep_ = "stepProcessEmptyRegionSetup";
       return (retval);
     } 
     DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
     nextStep_ = "stepLaunchDialog";
     return (daipc);
   }
 
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepExtractAndBuildNetwork(DialogAndInProcessCmd cmd) {
      BuildNetworkDialogFactory.RegionInfoRequest crq = (BuildNetworkDialogFactory.RegionInfoRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
      
      horribleHack_ = crq.horribleHack;
            
      //
      // Build it:
      //
      
      SpecialtyLayoutEngineParams params;
      if (crq.params != null) {
        params = crq.params;
      } else {
        LayoutOptionsManager lom = appState_.getLayoutOptMgr();
        params = lom.getLayoutParams(crq.strat); 
      }    
      
      UndoSupport support = new UndoSupport(appState_, "undo.buildRootFromInstructions");
      SpecialtyLayout specLayout = SpecialtyLayoutEngine.getLayout(crq.strat, appState_); 
      
      BuildRunner runner = new BuildRunner(appState_, rcxT_, rcxR_, crq.buildCmds, crq.workingRegions,
                                           appState_.getCanvasCenter(), appState_.getCanvasSize(), crq.keepLayout,
                                           appState_.getLayoutOptMgr().getLayoutOptions(), 
                                           crq.globalDelete, support, specLayout, params);
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      
      runner.setClient(bwc);
      bwc.launchWorker();
  
      // Either done or looping:
      DialogAndInProcessCmd daipc;
      if (crq.isForApply()) {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.APPLY_ON_THREAD, this); // Keep going
        nextStep_ = "stepExtractAndBuildNetwork";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      }
      return (daipc);  
    }
        
    /***************************************************************************
    **
    ** Routines to manage background threads
    ** 
    */ 
    
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }    
  
    public void cleanUpPreEnable(Object result) {
      // FIXME This will need to be revisited when we actually implement the web editor:
      horribleHack_.enableControls();
      return;
    }
  
    public void handleCancellation() {
      return;
    }     
  
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(appState_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      Genome toCheck = rcxT_.getGenomeSource().getGenome(rcxT_.getGenomeID());
      if ((toCheck != null) && !toCheck.isEmpty()) {
        appState_.getZoomCommandSupport().zoomToWorksheetCenter();
      }
      LayoutLinkSupport.offerColorFixup(appState_, rcxT_, (LinkRouter.RoutingResult)result);
      return;
    }  
  }  
  
  /***************************************************************************
  **
  ** Background Build runner
  */ 
    
  private static class BuildRunner extends BackgroundWorker {
    private String myGenomeID_;
    private DataAccessContext rcxR_;
    private UndoSupport support_;
    private List<BuildInstruction> myBuildCmds_;
    private List<InstanceInstructionSet.RegionInfo> myRegions_;
    private Point2D myCenter_;
    private Dimension mySize_;
    private boolean myKeepLayout_;
    private boolean myGlobalDelete_;
    private LayoutOptions myOptions_;
    private BTState myAppState_;
    SpecialtyLayout specLayout_;
    SpecialtyLayoutEngineParams params_;
    
    public BuildRunner(BTState appState, DataAccessContext rcxT, DataAccessContext rcxR,
                       List<BuildInstruction> buildCmds, List<InstanceInstructionSet.RegionInfo> workingRegions, Point2D center, 
                       Dimension size, boolean keepLayout, 
                       LayoutOptions options, boolean globalDelete,
                       UndoSupport support,  
                       SpecialtyLayout specLayout,
                       SpecialtyLayoutEngineParams params) {
      super(new LinkRouter.RoutingResult());
      myAppState_ = appState;
      myGenomeID_ = rcxT.getGenomeID();
      rcxR_ = rcxR;
      myBuildCmds_ = buildCmds;
      myRegions_ = workingRegions;
      myCenter_ = center;
      mySize_ = size;
      myKeepLayout_ = keepLayout; 
      myOptions_ = options;
      myGlobalDelete_ = globalDelete;
      support_ = support;
      specLayout_ = specLayout;
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      BuildInstructionProcessor bip = new BuildInstructionProcessor(myAppState_);
      BuildInstructionProcessor.PIData pid = new BuildInstructionProcessor.PIData(myGenomeID_, myBuildCmds_, myRegions_,
                                                                                  myCenter_, mySize_,  myKeepLayout_, false, myOptions_, 
                                                                                  support_, this, myGlobalDelete_, specLayout_, params_);
      bip.installPIData(pid);
      LinkRouter.RoutingResult res = bip.processInstructions(rcxR_);      
      return (res);
    }
    
    public Object postRunCore() {
      myAppState_.getZoomTarget().fixCenterPoint(true, support_, false);
      if (myAppState_.modelIsOutsideWorkspaceBounds()) {
        Rectangle allBounds = myAppState_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(myAppState_, rcxR_)).setWorkspaceToModelBounds(support_, allBounds);
      }    
      return (null);
    }           
  }   
}
