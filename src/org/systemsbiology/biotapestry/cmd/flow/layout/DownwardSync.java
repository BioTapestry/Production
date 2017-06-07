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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.SuperAdd;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.BuildSupport;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.SpecialSegmentTracker;
import org.systemsbiology.biotapestry.ui.dialogs.SyncLayoutChoicesDialog;
import org.systemsbiology.biotapestry.ui.dialogs.SyncLayoutTargetDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Downward Layout Sync
*/

public class DownwardSync extends AbstractControlFlow {

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
  
  public DownwardSync() {
    name = "command.SyncLayouts"; 
    desc = "command.SyncLayouts"; 
    mnem = "command.SyncLayoutsMnem"; 
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
    if (!cache.genomeIsRoot()) {
      return (false);
    }     
    return (cache.moreThanOneModel());
  }
  
  /***************************************************************************
  **
  ** Handle the sync
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
        
  public static class StepState extends AbstractStepState implements BackgroundWorkerOwner {
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
    }
   
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }
    
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      return;
    }     
       
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(uics_, dacx_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    }        

    /***************************************************************************
    **
    ** Extract and install model data
    */ 
        
    private DialogAndInProcessCmd stepDoIt() {
      StaticDataAccessContext rcxR = dacx_.getContextForRoot();
      
      SyncLayoutTargetDialog sltd = new SyncLayoutTargetDialog(uics_, rcxR);
      sltd.setVisible(true);
      if (!sltd.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      List<SuperAdd.SuperAddPair> targets = sltd.getSuperAddPairs(); 
      
      boolean offerDirect = (new LayoutRubberStamper()).directCopyOptionAllowed(rcxR);
      SyncLayoutChoicesDialog slcd = new SyncLayoutChoicesDialog(uics_, offerDirect, rcxR);
      slcd.setVisible(true);
      if (!slcd.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      boolean directCopy = slcd.directCopy();
      boolean doCompress = slcd.doCompress();
      boolean keepGroups = slcd.keepGroups();
      boolean swapPads = slcd.swapPads();
      int overlayOption = slcd.getOverlayOption();
      
      UndoSupport support = uFac_.provideUndoSupport("undo.syncLayouts", rcxR);
      SyncLayoutRunner runner = 
        new SyncLayoutRunner(rcxR, directCopy, doCompress, keepGroups, swapPads, overlayOption, targets, support, this);
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(uics_, rcxR, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
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
    
    /***************************************************************************
    **
    ** Synchronize all layouts to the root layout
    */  
   
    public LinkRouter.RoutingResult synchronizeAllLayouts(StaticDataAccessContext rcxR,
                                                          UndoSupport support,
                                                          LayoutOptions options,
                                                          Point2D center,
                                                          boolean directCopy,
                                                          boolean keepGroups,
                                                          boolean switchPads,
                                                          List<SuperAdd.SuperAddPair> targets, 
                                                          BTProgressMonitor monitor) 
                                                          throws AsynchExitRequestException {
  
      Map<String, Layout.OverlayKeySet> globalModKeys = rcxR.getFGHO().fullModuleKeysPerLayout();
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = rcxR.getFGHO().getGlobalNetModuleLinkPadNeeds();
      
      //
      // Figure out which models and groups we are processing:
      //
      
      HashMap<String, Set<String>> grpsForModel = new HashMap<String, Set<String>>();
      int numTargs = targets.size();
      for (int i = 0; i < numTargs; i++) {
        SuperAdd.SuperAddPair sap = targets.get(i);
        Set<String> grps = grpsForModel.get(sap.genomeID);
        if (grps == null) {
          grps = new HashSet<String>();
          grpsForModel.put(sap.genomeID, grps);
        }
        grps.add(sap.groupID);
      }
      Set<String> doModels = grpsForModel.keySet();     
   
      if (switchPads) {
        (new SyncSupport(dacx_)).syncAllLinkPads(support, grpsForModel);
      }
      
      
      //
      // Iterate through all root instances, ditch existing layout, and rubber stamp
      // a new layout:
      //
                                           
      LayoutRubberStamper lrs = new LayoutRubberStamper();
      
      Map<String, BusProperties.RememberProps> rootRemember = rcxR.getCurrentLayout().buildRememberProps(rcxR);
      Map<String, SpecialSegmentTracker> rootSpecials = rcxR.getCurrentLayout().rememberSpecialLinks();
  
      //
      // Get root instance count first, and use to set progress values:
      //
      
      int instanceCount = 0;
      Iterator<GenomeInstance> iit = rcxR.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }
        instanceCount++;
      }
      
      double currStart = 0.0;
      double perInstance = 1.0 / instanceCount;
      double currFinal = currStart + perInstance; 
        
      //
      // Now change each layout with the rubber stamper:
      //
  
      LinkRouter.RoutingResult finalRes = new LinkRouter.RoutingResult();
      iit = rcxR.getGenomeSource().getInstanceIterator();
 
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if ((monitor != null) && !monitor.keepGoing()) {
          throw new AsynchExitRequestException();
        } 
        
        if (!gi.isRootInstance()) {
          continue;
        }
        
        String gid = gi.getID();
        if (!doModels.contains(gid)) {
          continue;
        }
        Set<String> doGrps = grpsForModel.get(gid);
        StaticDataAccessContext rcxT = new StaticDataAccessContext(rcxR, gi);  
        
        Layout origLayout = new Layout(rcxT.getCurrentLayout());
        Layout.OverlayKeySet loModKeys = globalModKeys.get(rcxT.getCurrentLayoutID());   
        
       
        
        Layout.SupplementalDataCoords sdc = null;
        if (!directCopy) {
          sdc = rcxT.getCurrentLayout().getSupplementalCoords(rcxT, loModKeys);
        }
        
        //
        // Retain what is needed for module link pad fixups:
        //
        
        
        Layout.PadNeedsForLayout padFixups = rcxT.getCurrentLayout().findAllNetModuleLinkPadRequirements(rcxT);  
        
        //
        // Gather module recovery info:
        //
        
        Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = 
          rcxT.getCurrentLayout().getModuleShapeParams(rcxT, loModKeys, center);
           
        //
        // If we have been asked to save region info, gather that up now:
        //
  
        Map<String, Rectangle> savedRegionBounds = (keepGroups) ? (new BuildSupport(uics_, rcxT, uFac_)).saveRegionBounds(rcxT) : null;
              
        DatabaseChange dc = rcxT.getLayoutSource().startLayoutUndoTransaction(rcxT.getCurrentLayoutID());
        
        try {      
          Map<String, BusProperties.RememberProps> rememberProps = rcxT.getCurrentLayout().buildInheritedRememberProps(rootRemember, gi);
          Map<String, SpecialSegmentTracker> specials = rcxT.getCurrentLayout().buildInheritedSpecialLinks(rootSpecials, gi);      
          if (directCopy || keepGroups) {
            rcxT.getCurrentLayout().dropNodeAndLinkProperties(rcxT);
          } else {
            rcxT.getCurrentLayout().dropProperties(rcxT);  
          }
                   
          LayoutRubberStamper.RSData rsd = 
            new LayoutRubberStamper.RSData(rcxR, rcxT, options,
                                           loModKeys, moduleShapeRecovery, monitor, savedRegionBounds, origLayout,
                                           keepGroups, currStart, currFinal, rememberProps, globalPadNeeds);
          lrs.setRSD(rsd);
          LinkRouter.RoutingResult rr = lrs.synchronizeToRootLayout(directCopy, doGrps);
  
          finalRes.merge(rr);
  
          if (!directCopy) {      
            rcxT.getCurrentLayout().applySupplementalDataCoords(sdc, rcxT, loModKeys);
          }
  
          //
          // Get special link segments back up to snuff:
          //
  
          if (specials != null) {
            rcxT.getCurrentLayout().restoreSpecialLinks(specials);
          }
          
          //
          // Handle module link pad repairs:
          //
          
          Map<String, Boolean> orpho = rcxT.getCurrentLayout().orphansOnlyForAll(false);
          rcxT.getCurrentLayout().repairAllNetModuleLinkPadRequirements(rcxT, padFixups, orpho);
   
          dc = rcxT.getLayoutSource().finishLayoutUndoTransaction(dc);
          support.addEdit(new DatabaseChangeCmd(rcxT, dc));
          currStart = currFinal;
          currFinal = currStart + perInstance;
        } catch (AsynchExitRequestException ex) {
          rcxT.getLayoutSource().rollbackLayoutUndoTransaction(dc);
          throw ex;
        }
      } 
         
      return (finalRes);
    }
  }
  
  /***************************************************************************
  **
  ** Background layout
  */ 
    
  private static class SyncLayoutRunner extends BackgroundWorker {
    
    private StaticDataAccessContext rcxR_;    
    private UndoSupport support_;
    private boolean doCompress_;
    private boolean keepGroups_;
    private boolean directCopy_;
    private boolean swapPads_;
    private int overlayOption_;
    private List<SuperAdd.SuperAddPair> targets_;
    private StepState ss_;
    
    public SyncLayoutRunner(StaticDataAccessContext rcxR,
                            boolean directCopy,
                            boolean doCompress, boolean keepGroups, boolean swapPads,
                            int overlayOption, List<SuperAdd.SuperAddPair> targets, UndoSupport support, StepState ss) {
      super(new LinkRouter.RoutingResult()); 
      rcxR_ = rcxR;
      doCompress_ = doCompress;
      keepGroups_ = keepGroups;
      directCopy_ = directCopy;
      swapPads_ = swapPads;
      overlayOption_ = overlayOption;
      targets_ = targets;
      support_ = support; 
      ss_ = ss;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      LayoutOptions lopt = new LayoutOptions(rcxR_.getLayoutOptMgr().getLayoutOptions());
      lopt.inheritanceSquash = doCompress_;
      lopt.overlayOption = overlayOption_;
      LinkRouter.RoutingResult res = ss_.synchronizeAllLayouts(rcxR_,
                                                               support_, lopt, rcxR_.getZoomTarget().getRawCenterPoint(), directCopy_, 
                                                               keepGroups_, swapPads_, targets_, this);
      return (res);
    }
    
    public Object postRunCore() {
      Iterator<Layout> lit = rcxR_.getLayoutSource().getLayoutIterator();
      String rootID = rcxR_.getDBGenome().getID();
      while (lit.hasNext()) {
        Layout lo = lit.next();
        String targ = lo.getTarget();
        if (!targ.equals(rootID)) {
          support_.addEvent(new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        }
      }
      return (null);
    }      
  } 
}
