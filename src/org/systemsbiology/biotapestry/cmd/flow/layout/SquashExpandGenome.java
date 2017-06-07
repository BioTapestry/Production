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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.SpecialSegmentTracker;
import org.systemsbiology.biotapestry.ui.dialogs.CompressExpandDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle expand and compress layout
*/

public class SquashExpandGenome extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean doCompress_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SquashExpandGenome(boolean doCompress, boolean isForGroups) {
    doCompress_ = doCompress;
    if (isForGroups) {
      name = (doCompress) ? "groupPopup.CompressGroup" : "groupPopup.ExpandGroup";
      desc = (doCompress) ? "groupPopup.CompressGroup" : "groupPopup.ExpandGroup";
      mnem = (doCompress) ? "groupPopup.CompressGroupMnem" : "groupPopup.ExpandGroupMnem";            
    } else {
      name = (doCompress) ? "command.SquashRoot" : "command.ExpandRoot";
      desc = (doCompress) ? "command.SquashRoot" : "command.ExpandRoot";
      mnem = (doCompress) ? "command.SquashRootMnem" : "command.ExpandRootMnem";
    }
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
    return ((doCompress_) ? cache.canSquash() : cache.canStretch());
  }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    Group group = rcx.getCurrentGenomeAsInstance().getGroup(inter.getObjectID());
    if (rcx.getCurrentGenomeAsInstance().getVfgParent() == null) { 
      int memCount = group.getMemberCount();
      return (memCount != 0); 
    } else { 
      return (false); 
    }       
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new CompressExpandState(doCompress_, dacx));  
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
        CompressExpandState ans = new CompressExpandState(doCompress_, cfh);     
        next = ans.stepGetCompressExpandDialog();
      } else {
        CompressExpandState ans = (CompressExpandState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepGetCompressExpandDialog")) {
          next = ans.stepGetCompressExpandDialog();      
        } else if (ans.getNextStep().equals("stepExtractPercentAndExecute")) {
          next = ans.stepExtractPercentAndExecute(last);      
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
        
  public static class CompressExpandState extends AbstractStepState 
                                          implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {
     
    private boolean myDoCompress_;
    private String groupID;
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public CompressExpandState(boolean doCompress, StaticDataAccessContext dacx) {
      super(dacx);
      myDoCompress_ = doCompress;
      nextStep_ = "stepGetCompressExpandDialog";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public CompressExpandState(boolean doCompress, ServerControlFlowHarness cfh) {
      super(cfh);
      myDoCompress_ = doCompress;
      nextStep_ = "stepGetCompressExpandDialog";
    }
      
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      this.groupID = intersect.getObjectID();
      return;
    }
    
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetCompressExpandDialog() { 
      boolean showOverlayOpts = (dacx_.currentGenomeIsAnInstance()) && (dacx_.getCurrentGenome().getNetworkModuleCount() > 0);
      CompressExpandDialogFactory.BuildArgs ba = new CompressExpandDialogFactory.BuildArgs(myDoCompress_, showOverlayOpts, dacx_);
      CompressExpandDialogFactory cedf = new CompressExpandDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);
      nextStep_ = "stepExtractPercentAndExecute";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepExtractPercentAndExecute(DialogAndInProcessCmd cmd) {
      
      CompressExpandDialogFactory.PercentsRequest crq = (CompressExpandDialogFactory.PercentsRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }  

      boolean showOverlayOpts = (dacx_.currentGenomeIsAnInstance()) && (dacx_.getCurrentGenome().getNetworkModuleCount() > 0);
          
      LayoutOptions options = new LayoutOptions(dacx_.getLayoutOptMgr().getLayoutOptions());
      options.overlayCpexOption = (showOverlayOpts) ? crq.overlayOption : NetOverlayProperties.NO_CPEX_LAYOUT_OPTION;

      UndoSupport support = uFac_.provideUndoSupport((myDoCompress_) ? "undo.squashRoot" : "undo.expandRoot", dacx_);
      CompressExpandRunner runner = 
        new CompressExpandRunner(this, dacx_, groupID, crq.retvalH, crq.retvalV, options, support, myDoCompress_); 
      BackgroundWorkerClient bwc;   
      if (!uics_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(uics_, dacx_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(uics_, dacx_, this, runner, support);
      }
      runner.setClient(bwc);
      bwc.launchWorker();     
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                   : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      return (daipc);
    }
   
    /***************************************************************************
    **
    ** We can do a background thread in the desktop version
    ** 
    */
  
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }        
   
    public void cleanUpPreEnable(Object result) {
      uics_.getZoomCommandSupport().zoomToModel();
      return;
    }
   
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(uics_, dacx_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    } 
    
    public void handleCancellation() {
      return;
    }
    
      /***************************************************************************
  **
  ** Squeeze useless space out of a network
  */  
  
   LinkRouter.RoutingResult squashGenome(StaticDataAccessContext rcx, Set<String> groups, double fracH, double fracV, UndoSupport support, 
                                         LayoutOptions options, BTProgressMonitor monitor, 
                                         double startFrac, double endFrac) throws AsynchExitRequestException {

     double fullFrac = endFrac - startFrac;
     double frac1 = fullFrac * 0.10;
     double eFrac1 = startFrac + frac1;
     double frac2 = fullFrac * 0.90;

     Point2D wsCenter = rcx.getZoomTarget().getRawCenterPoint();
     
     //
     // In the case of root genome, the compression step also hits the modules and
     // links, and in _theory_ should not need any fixup.  But all bets are off
     // for root instances, where per-group compression is not carrying overlay
     // modules along for the ride (except for group-attached mods, but even then
     // links are not handled).  So do a fixup step!
     
     Layout.PadNeedsForLayout padNeeds = rcx.getFGHO().getLocalNetModuleLinkPadNeeds(rcx.getCurrentGenomeID());
  
     //
     // Compress:
     //
     
     DatabaseChange dc = rcx.getLayoutSource().startLayoutUndoTransaction(rcx.getCurrentLayoutID());
     
     try {
       
       Map<String, Layout.OverlayKeySet> allKeys = rcx.getFGHO().fullModuleKeysPerLayout(); 
       Layout.OverlayKeySet loModKeys = allKeys.get(rcx.getCurrentLayoutID());   
       Point2D center = rcx.getCurrentLayout().getLayoutCenterAllOverlays(rcx, loModKeys);
       if (center == null) {
         center = wsCenter;
       }
       Layout.SupplementalDataCoords sdc = rcx.getCurrentLayout().getSupplementalCoordsAllOverlays(rcx, loModKeys);
       Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = 
         rcx.getCurrentLayout().getModuleShapeParams(rcx, loModKeys,center); 
       
       //
       // Useless corners just pointlessly reduce compression.  Get rid of them:
       //

       rcx.getCurrentLayout().dropUselessCorners(rcx, null, startFrac, eFrac1, monitor);

       //
       // Get a copy of the Layout, render to a grid, get candidate columns:
       //

       Map<String, SpecialSegmentTracker> specials = null;
       boolean orphanedPadsOnly;
       if (rcx.currentGenomeIsRootDBGenome()) {
         TreeSet<Integer> useEmptyRows = new TreeSet<Integer>();
         TreeSet<Integer> useEmptyCols = new TreeSet<Integer>();            
         rcx.getCurrentLayout().chooseCompressionRows(rcx, fracV, fracH, null, false, loModKeys, useEmptyRows, useEmptyCols, monitor);
         rcx.getCurrentLayout().compress(rcx, useEmptyRows, useEmptyCols, null, null, null, null, monitor, eFrac1, endFrac);
         orphanedPadsOnly = true;
       } else {
         specials = rcx.getCurrentLayout().rememberSpecialLinks();
         LayoutRubberStamper lrs = new LayoutRubberStamper();
         LayoutRubberStamper.ERILData eril = 
           new LayoutRubberStamper.ERILData(rcx, groups, padNeeds, 
                                            loModKeys, moduleShapeRecovery, options, monitor, eFrac1, endFrac);
         lrs.setERIL(eril);
         lrs.compressRootInstanceLayout(fracV, fracH);
         orphanedPadsOnly = false;
       }
       
       if (center != null) {
         rcx.getCurrentLayout().recenterLayout(center, rcx, true, true, true, null, null, null, loModKeys, (orphanedPadsOnly) ? null : padNeeds);
       }

       rcx.getCurrentLayout().applySupplementalDataCoords(sdc, rcx, loModKeys);

       //
       // Get special link segments back up to snuff:
       //

       if (specials != null) {
         rcx.getCurrentLayout().restoreSpecialLinks(specials);
       }
       
       dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
       support.addEdit(new DatabaseChangeCmd(rcx, dc));
       
       //
       // Fix net module link pads:
       //
       
       // DBGenome ops modify pads in a consistent fashion, so only do for orphans
       ModificationCommands.repairNetModuleLinkPadsLocally(padNeeds, rcx, orphanedPadsOnly, support); // Do after; does its own support calls...
       
       UiUtil.fixMePrintout("FIX ME? This call happens in the expand genome case, but not here. Added as a comment for now. An oversight??");
       // rcx.getZoomTarget().fixCenterPoint(true, support, false);
       
       LayoutChangeEvent lcev = new LayoutChangeEvent(rcx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
       support.addEvent(lcev);
       return (new LinkRouter.RoutingResult());
     } catch (AsynchExitRequestException ex) {
       rcx.getLayoutSource().rollbackLayoutUndoTransaction(dc);
       throw ex;
     }
   }
   
   /***************************************************************************
   **
   ** Expand the genome
   */  
  
   LinkRouter.RoutingResult expandGenome(StaticDataAccessContext rcx, Set<String> groups, double fracH, double fracV, UndoSupport support, 
                                         LayoutOptions options, BTProgressMonitor monitor, 
                                         double startFrac, double endFrac) throws AsynchExitRequestException {
     
      
     
     
     double fullFrac = endFrac - startFrac;
     double frac1 = fullFrac * 0.10;
     double eFrac1 = startFrac + frac1;
     double frac2 = fullFrac * 0.90;
     
     Point2D wsCenter = rcx.getZoomTarget().getRawCenterPoint();
     
     //
     // In the case of root genome, the expansion step also hits the modules and
     // links, and in _theory_ should not need any fixup.  But all bets are off
     // for root instances, where per-group compression is not carrying overlay
     // modules along for the ride (except for group-attached mods, but even then
     // links are not handled).  So do a fixup step!
     
     Layout.PadNeedsForLayout padNeeds = rcx.getFGHO().getLocalNetModuleLinkPadNeeds(rcx.getCurrentGenomeID());
     
     DatabaseChange dc = rcx.getLayoutSource().startLayoutUndoTransaction(rcx.getCurrentLayoutID());
     
     try {
       Map<String, Layout.OverlayKeySet> allKeys = rcx.getFGHO().fullModuleKeysPerLayout();
       Layout.OverlayKeySet loModKeys = allKeys.get(rcx.getCurrentLayoutID());   
       Point2D center = rcx.getCurrentLayout().getLayoutCenterAllOverlays(rcx, loModKeys);
       if (center == null) {
         center = wsCenter;
       }
       Layout.SupplementalDataCoords sdc = rcx.getCurrentLayout().getSupplementalCoordsAllOverlays(rcx, loModKeys);
       Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = 
         rcx.getCurrentLayout().getModuleShapeParams(rcx, loModKeys, center);   
       
       //
       // Useless corners just pointlessly reduce expansion.  Get rid of them:
       //

       rcx.getCurrentLayout().dropUselessCorners(rcx, null, startFrac, eFrac1, monitor);        

       //
       // Get a copy of the Layout, render to a grid, get candidate columns:
       //

       Map<String, SpecialSegmentTracker> specials = null;
       boolean orphanedPadsOnly;
       if (rcx.currentGenomeIsRootDBGenome()) {
         TreeSet<Integer> useExpandRows = new TreeSet<Integer>();
         TreeSet<Integer> useExpandCols = new TreeSet<Integer>();
         rcx.getCurrentLayout().chooseExpansionRows(rcx, fracV, fracH, null, loModKeys, useExpandRows, useExpandCols, false, monitor);
         rcx.getCurrentLayout().expand(rcx, useExpandRows, useExpandCols, 1, false, null, null, null, monitor, eFrac1, endFrac);
         orphanedPadsOnly = true;
       } else {
         specials = rcx.getCurrentLayout().rememberSpecialLinks();
         LayoutRubberStamper lrs = new LayoutRubberStamper();
         LayoutRubberStamper.ERILData eril = 
           new LayoutRubberStamper.ERILData(rcx, groups, null, null,
                                            padNeeds, loModKeys, moduleShapeRecovery, options, monitor, eFrac1, endFrac);
         
         lrs.setERIL(eril);
         lrs.expandRootInstanceLayout(fracV, fracH);
         // Set to false means relatively loose pad behavior on stretches! 
         // But if we work to maintain consistent pads, set to true "stretches" the pads in a better fashion...
         // We are going to go the orphans only route here to try to make for better expansions.  We have an
         // open question standing as to whether we want to hoist the orphans-only per overly set-up to this level
         // (it is currently capable of that granularity down in mc.repairNetModuleLinkPadsLocally(), but we
         // are doing a blanket setting here.)
         // BTW, note that companion compression method is still doing a false setting here...
         orphanedPadsOnly = true;
         // Note on 3/5/12, if false, the "closest pad" search uses the original pre-expansion location
         // as the starting point for the "closest pad".  That can get nice behavior sometimes, and crappy results
         // other times. FIXME Get this done better!    
       }
       
       if (center != null) {
         rcx.getCurrentLayout().recenterLayout(center, rcx, true, true, true, null, null, null, loModKeys, (orphanedPadsOnly) ? null : padNeeds);
       }

       rcx.getCurrentLayout().applySupplementalDataCoords(sdc, rcx, loModKeys);

       //
       // Get special link segments back up to snuff:
       //

       if (specials != null) {
         rcx.getCurrentLayout().restoreSpecialLinks(specials);
       }
       
       //
       // Fix net module link pads:
       //
         
       dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
       support.addEdit(new DatabaseChangeCmd(rcx, dc));
       // Need to do this AFTER layout trans, since it does its own support changes 
       ModificationCommands.repairNetModuleLinkPadsLocally(padNeeds, rcx, orphanedPadsOnly, support); 
      
       rcx.getZoomTarget().fixCenterPoint(true, support, false);
       LayoutChangeEvent lcev = new LayoutChangeEvent(rcx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
       support.addEvent(lcev);
       return (new LinkRouter.RoutingResult());
     } catch (AsynchExitRequestException ex) {
       rcx.getLayoutSource().rollbackLayoutUndoTransaction(dc);
       throw ex;
     }
   }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
  }
  
  /***************************************************************************
  **
  ** Background compression / expansion
  */ 
     
   private static class CompressExpandRunner extends BackgroundWorker {   
     private String groupID_;
     private double fracH_;
     private double fracV_;
     private UndoSupport support_;
     private boolean doCompress_;
     private LayoutOptions options_;
     private SquashExpandGenome.CompressExpandState seg_;
     private StaticDataAccessContext rcx_;
     
     public CompressExpandRunner(SquashExpandGenome.CompressExpandState seg, StaticDataAccessContext rcx, String groupID, 
                                 double fracH, double fracV, LayoutOptions options,
                                 UndoSupport support, boolean doCompress) {
       super(new LinkRouter.RoutingResult());
       seg_ = seg;
       support_ = support;
       fracH_ = fracH;
       fracV_ = fracV;
       rcx_ = rcx;
       groupID_ = groupID;
       doCompress_ = doCompress;
       options_ = options;
     }
     
     public Object runCore() throws AsynchExitRequestException {
       HashSet<String> groupIDSet = null;
       if (groupID_ != null) {
         groupIDSet = new HashSet<String>();
         groupIDSet.add(groupID_);
       }
       LinkRouter.RoutingResult myResult;
       if (doCompress_) {         
         myResult = seg_.squashGenome(rcx_, groupIDSet, fracH_, fracV_, support_, options_, this, 0.0, 1.0);
       } else {
         myResult = seg_.expandGenome(rcx_, groupIDSet, fracH_, fracV_, support_, options_, this, 0.0, 1.0);
       }
       return (myResult);
     }

     public Object postRunCore() {
       if (!doCompress_) {
         if (rcx_.modelIsOutsideWorkspaceBounds()) {
           Rectangle allBounds = rcx_.getZoomTarget().getAllModelBounds();
           (new WorkspaceSupport(rcx_)).setWorkspaceToModelBounds(support_, allBounds);
         }     
       }
       support_.addEvent(new LayoutChangeEvent(rcx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
       return (null);
     }  
   }
}
