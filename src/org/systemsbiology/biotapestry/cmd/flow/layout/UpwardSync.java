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

import java.awt.geom.Point2D;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutDerivation;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.SpecialSegmentTracker;
import org.systemsbiology.biotapestry.ui.dialogs.LayoutPropagationDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerControlManager;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Upward Layout Sync
*/

public class UpwardSync extends AbstractControlFlow  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
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
  
  public UpwardSync() {
    name = "command.ApplyKidLayouts"; 
    desc = "command.ApplyKidLayouts"; 
    mnem = "command.ApplyKidLayoutsMnem"; 
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
    if (!cache.genomeNotNull()) {
      return (false);
    }
    return (cache.moreThanOneModel());
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(cfh);
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
      DBGenome dbg = dacx_.getDBGenome();
      boolean showOverlayOpts = (dbg.getNetworkOverlayCount() > 0);    
      LayoutPropagationDialog propDialog = new LayoutPropagationDialog(uics_, dacx_, showOverlayOpts);
      propDialog.setVisible(true);
      if (!propDialog.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      LayoutDerivation ld = propDialog.getLayoutDerivation();       
   //   Layout lor = dacx_.lSrc.getLayoutForGenomeKey(dbg.getID());
      UndoSupport support = uFac_.provideUndoSupport("undo.applyKidLayouts", dacx_);
      UpwardLayoutSynch uls = new UpwardLayoutSynch();   
      uls.synchronizeRootLayoutFromChildrenAsynch(uics_, dacx_, support, ld, dacx_.getZoomTarget().getRawCenterPoint(), uics_.getCommonView());
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));      
         
      /*
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState_, bwo_, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      return (daipc);*/
    }
  }
  
  /***************************************************************************
  **
  ** Wrapper for upward layout propagation
  */
  
  public static class UpwardLayoutSynch implements BackgroundWorkerOwner {
    
    private BackgroundWorkerControlManager bwcm_;
    private UIComponentSource myUics_;
    private StaticDataAccessContext dacxR_;
    private boolean cancelled_;
    
    public void synchronizeRootLayoutFromChildrenAsynch(UIComponentSource myUics, StaticDataAccessContext dacx,
                                                        UndoSupport support, LayoutDerivation ld, Point2D center, 
                                                        BackgroundWorkerControlManager bwcm) { 
      
      myUics_ = myUics;
      bwcm_ = bwcm;
      cancelled_ = false;
      
      // Issue #187: Can run this command from any level.
      dacxR_ = dacx.getContextForRoot();
      
      UpwardLayoutSynchRunner runner = 
        new UpwardLayoutSynchRunner(dacxR_, support, ld, center);
      //
      // We may need to do lots of link relayout operations.  This MUST occur on a background
      // thread!
      // 

      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(myUics_, dacxR_, bwcm_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      return;
    }
      

    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }
    
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      cancelled_ = true;
      return;
    }
    
    public boolean wasCancelled() {
      return (cancelled_);
    }
        
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(myUics_, dacxR_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    }
  }  
 
  /***************************************************************************
  **
  ** Background upward layout propagation worker
  */ 
    
  private static class UpwardLayoutSynchRunner extends BackgroundWorker {

    private StaticDataAccessContext rcx_;
    private UndoSupport support_;
    private LayoutDerivation ld_;
    private Point2D center_;

    public UpwardLayoutSynchRunner(StaticDataAccessContext dacx,
                                   UndoSupport support, LayoutDerivation ld, Point2D center) {
      super(new LinkRouter.RoutingResult());
      rcx_ = dacx;
      support_ = support;
      ld_ = ld;
      center_ = center;
    }
    
    public Object runCore() throws AsynchExitRequestException {     
      LinkRouter.RoutingResult result = synchronizeRootLayoutFromChildren(rcx_,
                                                                          support_,
                                                                          ld_, center_,
                                                                          this, 0.0, 1.0);
      
      rcx_.getGenomeSource().clearAllDynamicProxyCaches();
      return (result);
    }
    
    public Object postRunCore() {
      return (null);
    } 
  } 
  
  /***************************************************************************
  **
  ** Synchronize root from children
  */  
 
  private static LinkRouter.RoutingResult synchronizeRootLayoutFromChildren(StaticDataAccessContext rcxR,
                                                                            UndoSupport support,
                                                                            LayoutDerivation ld,
                                                                            Point2D center,
                                                                            BTProgressMonitor monitor,
                                                                            double startFrac, double maxFrac) 
                                                                            throws AsynchExitRequestException {


    LayoutRubberStamper lrs = new LayoutRubberStamper();
    String lorKey = rcxR.getCurrentLayoutID();
    
    Map<String, BusProperties.RememberProps> rootRemember = rcxR.getCurrentLayout().buildRememberProps(rcxR);
    Map<String, SpecialSegmentTracker> rootSpecials = rcxR.getCurrentLayout().rememberSpecialLinks();

    //
    // Figure out what we need to do with overlays:
    //
    
    Layout.PadNeedsForLayout padFixups = rcxR.getCurrentLayout().findAllNetModuleLinkPadRequirements(rcxR); 
    Map<String, Layout.OverlayKeySet> globalModKeys = rcxR.getFGHO().fullModuleKeysPerLayout();    
    Layout.OverlayKeySet loModKeys = globalModKeys.get(lorKey);   
    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = rcxR.getCurrentLayout().getModuleShapeParams(rcxR, loModKeys, center);    
   
    //
    // Now change each layout with the rubber stamper:
    //

    
    LinkRouter.RoutingResult rr = null;
    DatabaseChange dc = null;
    if (support != null) {
      dc = rcxR.getLayoutSource().startLayoutUndoTransaction(lorKey);
    }
    try {      
      rr = lrs.upwardCopy(rcxR, ld, rootRemember,
                          loModKeys, moduleShapeRecovery, monitor, startFrac, maxFrac, support); 

      //
      // Get special link segments back up to snuff:
      //

      if (rootRemember != null) {
        rcxR.getCurrentLayout().restoreLabelLocations(rootRemember, rcxR, null);
      }
          
      if (rootSpecials != null) {
        rcxR.getCurrentLayout().restoreSpecialLinks(rootSpecials);
      }      

      if (support != null) {
        dc = rcxR.getLayoutSource().finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(rcxR, dc));
      }
    } catch (AsynchExitRequestException ex) {
      if (support != null) rcxR.getLayoutSource().rollbackLayoutUndoTransaction(dc);
      throw ex;
    }
    
    // Need to do this AFTER layout trans, since it does its own support changes 
    ModificationCommands.repairNetModuleLinkPadsLocally(padFixups, rcxR, false, support);      
    return (rr);
  }
}
