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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
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
import org.systemsbiology.biotapestry.util.UndoFactory;
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
  
  public DialogBuild() {
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
   
  @Override
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
        ans = new StepState(cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
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
        
  public static class StepState extends AbstractStepState implements BackgroundWorkerOwner {
      
    private BuildInstructionProcessor bip_;
    private StaticDataAccessContext rcxR_;
    private BuildNetworkDialogFactory.DesktopDialog horribleHack_;  
    private Genome parent;
    private List<InstanceInstructionSet.RegionInfo> working;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepPreprocessRegions";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepPreprocessRegions";
    }
    
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombLayoutValues(RemoteRequest qbom) {
      SpecialtyLayoutEngine.SpecialtyType strat = (SpecialtyLayoutEngine.SpecialtyType)qbom.getObjectArg("strategy");     
      SpecialtyLayout specLayout = SpecialtyLayoutEngine.getLayout(strat, dacx_); 
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout.getParameterDialogBuildArgs(uics_, dacx_.getCurrentGenome(), null, false);
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
      List<BuildInstructionProcessor.MatchChecker> mismatch = bip_.findInstanceMismatches(dacx_, buildCmds);
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
      BuildInstructionProcessor.BuildChanges bChanges = bip_.preProcess(dacx_, buildCmds); 
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      result.setObjAnswer("bChanges", bChanges);
      return (result);
    }
 
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepPreprocessRegions() {
      rcxR_ = dacx_.getContextForRoot();
      parent = null;
      working = null;
      if (!dacx_.currentGenomeIsRootDBGenome()) {
        InstanceInstructionSet iis = dacx_.getInstructSrc().getInstanceInstructionSet(dacx_.getCurrentGenomeID());
        parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        if (parent != null) {
          InstanceInstructionSet piis = dacx_.getInstructSrc().getInstanceInstructionSet(parent.getID());
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
              new RegionInheritDialogFactory.BuildArgs(dacx_.getCurrentGenome(), parentList, working);
            RegionInheritDialogFactory mddf = new RegionInheritDialogFactory(cfh_);   
            ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
            nextStep_ = "stepProcessEmptyInheritRegionSetup";
            return (retval);
          }
          if (working.isEmpty()) {
            RegionSetupDialogFactory.BuildArgs ba = 
              new RegionSetupDialogFactory.BuildArgs(dacx_.getCurrentGenome(), working);
            RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh_);   
            ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
            nextStep_ = "stepProcessEmptyRegionSetup";
            return (retval);
          } 
        } else if ((iis == null) || (iis.getRegionCount() == 0)) {
          RegionSetupDialogFactory.BuildArgs ba = 
            new RegionSetupDialogFactory.BuildArgs(dacx_.getCurrentGenome(), working);
          RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh_);   
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
 
     bip_ = new BuildInstructionProcessor(uics_, dacx_, tSrc_, uFac_);
     List<BuildInstruction> instruct = bip_.getInstructions(dacx_);
      
     BuildNetworkDialogFactory.BuildArgs ba = new BuildNetworkDialogFactory.BuildArgs(dacx_.getCurrentGenome(), parent, working, instruct);
     BuildNetworkDialogFactory bndf = new BuildNetworkDialogFactory(cfh_);
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
         new RegionSetupDialogFactory.BuildArgs(dacx_.getCurrentGenome(), working);
       RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(cfh_);   
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
        LayoutOptionsManager lom = dacx_.getLayoutOptMgr();
        params = lom.getLayoutParams(crq.strat); 
      }    
      
      UndoSupport support = uFac_.provideUndoSupport("undo.buildRootFromInstructions", rcxR_);
      SpecialtyLayout specLayout = SpecialtyLayoutEngine.getLayout(crq.strat, dacx_); 
      
      Point2D cc = dacx_.getWorkspaceSource().getWorkspace().getCanvasCenter();
      Dimension csz = dacx_.getWorkspaceSource().getWorkspace().getCanvasSize();
      
      BuildRunner runner = new BuildRunner(uics_, dacx_, rcxR_, tSrc_, uFac_, crq.buildCmds, crq.workingRegions,
                                           cc, csz, crq.keepLayout, dacx_.getLayoutOptMgr().getLayoutOptions(), 
                                           crq.globalDelete, support, specLayout, params);
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(uics_, rcxR_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      
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
      (new LayoutStatusReporter(uics_, dacx_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      Genome toCheck = dacx_.getGenomeSource().getGenome(dacx_.getCurrentGenomeID());
      if ((toCheck != null) && !toCheck.isEmpty()) {
        uics_.getZoomCommandSupport().zoomToWorksheetCenter();
      }
      LayoutLinkSupport.offerColorFixup(uics_, dacx_, (LinkRouter.RoutingResult)result, uFac_);
      return;
    }  
  }  
  
  /***************************************************************************
  **
  ** Background Build runner
  */ 
    
  private static class BuildRunner extends BackgroundWorker {
    private UIComponentSource uics_;
    private UndoFactory uFac_;
    private TabSource tSrc_;
    
    private String myGenomeID_;
    private StaticDataAccessContext rcxR_;
    private UndoSupport support_;
    private List<BuildInstruction> myBuildCmds_;
    private List<InstanceInstructionSet.RegionInfo> myRegions_;
    private Point2D myCenter_;
    private Dimension mySize_;
    private boolean myKeepLayout_;
    private boolean myGlobalDelete_;
    private LayoutOptions myOptions_;
    SpecialtyLayout specLayout_;
    SpecialtyLayoutEngineParams params_;
    
    public BuildRunner(UIComponentSource uics, StaticDataAccessContext rcxT, StaticDataAccessContext rcxR, TabSource tSrc, UndoFactory uFac,
                       List<BuildInstruction> buildCmds, List<InstanceInstructionSet.RegionInfo> workingRegions, Point2D center, 
                       Dimension size, boolean keepLayout, 
                       LayoutOptions options, boolean globalDelete,
                       UndoSupport support,  
                       SpecialtyLayout specLayout,
                       SpecialtyLayoutEngineParams params) {
      super(new LinkRouter.RoutingResult());
      uics_ = uics;
      uFac_ = uFac;
      tSrc_ = tSrc;
      myGenomeID_ = rcxT.getCurrentGenomeID();
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
      BuildInstructionProcessor bip = new BuildInstructionProcessor(uics_, rcxR_, tSrc_, uFac_);
      BuildInstructionProcessor.PIData pid = new BuildInstructionProcessor.PIData(myGenomeID_, myBuildCmds_, myRegions_,
                                                                                  myCenter_, mySize_,  myKeepLayout_, false, myOptions_, 
                                                                                  support_, this, myGlobalDelete_, specLayout_, params_);
      bip.installPIData(pid);
      LinkRouter.RoutingResult res = bip.processInstructions(rcxR_);      
      return (res);
    }
    
    public Object postRunCore() {
      rcxR_.getZoomTarget().fixCenterPoint(true, support_, false);
      if (rcxR_.modelIsOutsideWorkspaceBounds()) {
        Rectangle allBounds = rcxR_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(rcxR_)).setWorkspaceToModelBounds(support_, allBounds);
      }    
      return (null);
    }           
  }   
}
