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

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle non-ortho Links
*/

public class LayoutNonOrthoLink extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean minCorners_;
  private boolean wholeTree_;
  private boolean forModules_;
  private boolean forPopup_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LayoutNonOrthoLink(boolean wholeTree, boolean minCorners, boolean forPopup, boolean forModules) {
    minCorners_ = minCorners;
    forPopup_ = forPopup;
    wholeTree_ = wholeTree;
    forModules_ = forModules;
    //
    // This guy gets used everywhere!
    // Note that forModules has no effect on the naming.
    //
    if (forPopup_) {
      if (wholeTree_) { 
        name = (minCorners_) ? "linkPopup.fixAllNonOrthoSegmentsMinSplit" : "linkPopup.fixAllNonOrthoSegmentsMinShift";
        desc = (minCorners_) ? "linkPopup.fixAllNonOrthoSegmentsMinSplit" : "linkPopup.fixAllNonOrthoSegmentsMinShift";
        mnem = (minCorners_) ? "linkPopup.fixAllNonOrthoSegmentsMinSplitMnem": "linkPopup.fixAllNonOrthoSegmentsMinShiftMnem";      
      } else {        
        name = (minCorners_) ? "linkPopup.fixNonOrthoMinSplit" : "linkPopup.fixNonOrthoMinShift";
        desc = (minCorners_) ? "linkPopup.fixNonOrthoMinSplit" : "linkPopup.fixNonOrthoMinShift";
        mnem = (minCorners_) ? "linkPopup.fixNonOrthoMinSplitMnem" : "linkPopup.fixNonOrthoMinShiftMnem";        
      }      
    } else { // If used for global fixes:
      name = (minCorners_) ? "command.RepairAllNonOrthoLinksMinSplit" : "command.RepairAllNonOrthoLinksMinShift"; 
      desc = (minCorners_) ? "command.RepairAllNonOrthoLinksMinSplit" : "command.RepairAllNonOrthoLinksMinShift"; 
      mnem = (minCorners_) ? "command.RepairAllNonOrthoLinksMinSplitMnem" : "command.RepairAllNonOrthoLinksMinShiftMnem"; 
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
    if (forPopup_) {
      throw new IllegalStateException();
    }
    return (cache.genomeNotNull());
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
    String oid = inter.getObjectID();
    
    // Allow the operation on incomplete VfN-layer trees:
    if (!forModules_ && rcx.currentGenomeIsAnInstance() && !rcx.currentGenomeIsRootInstance()) {
      rcx = new StaticDataAccessContext(rcx, rcx.getRootInstanceFOrCurrentInstance());
    }
    
    if (wholeTree_) {   
      LinkProperties lp;
      if (forModules_) {
        String ovrKey = rcx.getOSO().getCurrentOverlay();
        lp = rcx.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
      } else {
        lp = rcx.getCurrentLayout().getLinkProperties(oid);
      }   
      Set<LinkSegmentID> nonOrtho = lp.getNonOrthoSegments(rcx);
      return (!nonOrtho.isEmpty());
    } else {
      LinkSegmentID segID = inter.segmentIDFromIntersect();
      LinkProperties lp;
      if (forModules_) {
        String ovrKey = rcx.getOSO().getCurrentOverlay();
        lp = rcx.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
      } else {
        lp = rcx.getCurrentLayout().getLinkProperties(oid);
      }
      LinkSegment noSeg = lp.getSegmentGeometryForID(segID, rcx, false);
      return (!noSeg.isOrthogonal());
    }
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new StepState(wholeTree_, minCorners_, forPopup_, forModules_, dacx));  
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
        StepState ans = new StepState(wholeTree_, minCorners_, forPopup_, forModules_, cfh);
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {

    private Object[] args_;
    private boolean myMinCorners_;
    private boolean myForPopup_;
    private boolean myWholeTree_;
    private boolean myForModules_;
    private StaticDataAccessContext rcxT_;
         
       
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean wholeTree, boolean minCorners, boolean forPopup, boolean forModules, ServerControlFlowHarness cfh) {
      super(cfh);
      myMinCorners_ = minCorners;
      myForPopup_ = forPopup;
      myWholeTree_ = wholeTree;
      myForModules_ = forModules;
      // Allow the operation on incomplete VfN-layer trees:
      if (!myForModules_ && dacx_.currentGenomeIsAnInstance() && !dacx_.currentGenomeIsRootInstance()) {
        rcxT_ = new StaticDataAccessContext(dacx_, dacx_.getRootInstanceFOrCurrentInstance());
      } else {   
        rcxT_ = dacx_;
      }
      nextStep_ = "stepDoIt";
    }

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean wholeTree, boolean minCorners, boolean forPopup, boolean forModules, StaticDataAccessContext dacx) {
      super(dacx);
      myMinCorners_ = minCorners;
      myForPopup_ = forPopup;
      myWholeTree_ = wholeTree;
      myForModules_ = forModules;
      // Allow the operation on incomplete VfN-layer trees:
      if (!myForModules_ && dacx.currentGenomeIsAnInstance() && !dacx.currentGenomeIsRootInstance()) {
        rcxT_ = new StaticDataAccessContext(dacx, dacx.getRootInstanceFOrCurrentInstance());
      } else {   
        rcxT_ = dacx_;
      }
      nextStep_ = "stepDoIt";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      if (!myForPopup_) {
        throw new IllegalStateException();
      }
      String interID = inter.getObjectID();
      if (myWholeTree_) {     
        LinkProperties lp;
        if (myForModules_) {
          String ovrKey = rcxT_.getOSO().getCurrentOverlay();
          lp = rcxT_.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          lp = rcxT_.getCurrentLayout().getLinkProperties(interID);
        }
        args_ = new Object[4];
        args_[0] = new Boolean(myMinCorners_);
        args_[1] = lp;
        args_[2] = null;
        args_[3] = (myForModules_) ? rcxT_.getOSO().getCurrentOverlay() : null;
      } else {
        LinkProperties lp;
        if (myForModules_) {
          String ovrKey = rcxT_.getOSO().getCurrentOverlay();
          lp = rcxT_.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          lp = rcxT_.getCurrentLayout().getLinkProperties(interID);
        }
        LinkSegmentID id = inter.segmentIDFromIntersect();
        args_ = new Object[4];
        args_[0] = new Boolean(myMinCorners_);
        args_[1] = lp;
        args_[2] = id;
        args_[3] = (myForModules_) ? rcxT_.getOSO().getCurrentOverlay() : null;    
      }
      return;
    }
     
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepDoIt() {
      LinkProperties forOne = null; 
      LinkSegmentID onlyLsid = null;
      boolean useForMin = myMinCorners_;
      String overID = null;  
      
      if (args_ != null) {
        useForMin =  ((Boolean)args_[0]).booleanValue();  
        forOne = (LinkProperties)args_[1];  
        onlyLsid = (LinkSegmentID)args_[2];       
        overID = (String)args_[3];       
      }
      
      //
      // Before starting, we need to clear selections.  This automatically
      // installs a selection undo!
      //
      
      SUPanel sup = uics_.getSUPanel();
      if (sup.hasASelection()) {
        sup.selectNone(uics_, uFac_, rcxT_);
        sup.drawModel(false);
      }

      UndoSupport support;
      if (forOne == null) {
        support = uFac_.provideUndoSupport("undo.repairAllNonOrtho", rcxT_); 
      } else if (onlyLsid == null) {
        support = uFac_.provideUndoSupport("undo.fixAllNonOrthoForTree", rcxT_);
      } else {
        support = uFac_.provideUndoSupport("undo.fixNonOrtho", rcxT_);
      }
  
      Genome genome = rcxT_.getCurrentGenome();
      RepairNonOrthoRunner runner = 
        new RepairNonOrthoRunner(rcxT_, genome, useForMin, forOne, onlyLsid, overID, support, this);
      BackgroundWorkerClient bwc;     
      if (!uics_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(uics_, rcxT_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(uics_, rcxT_, this, runner, support);
      }
      runner.setClient(bwc);
      bwc.launchWorker();
            // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                   : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      return (daipc);
    }
    
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }
    
    public void cleanUpPreEnable(Object result) {
       return;
    }
    
    public void cleanUpPostRepaint(Object result) {
      ResourceManager rMan = rcxT_.getRMan();
      Layout.OrthoRepairInfo ori = (Layout.OrthoRepairInfo)result;
 // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>   // myGenomePre_.clearSelections(genomeKey_, layout_, support);
      if (ori.failCount > 0) {
        if (ori.singleSeg) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("fixNonOrthoSeg.failWarning"), 
                                        rMan.getString("fixNonOrthoSeg.failWarningTitle"),
                                        JOptionPane.WARNING_MESSAGE);         
        } else {
          boolean someOK = ((ori.chgs != null) && (ori.chgs.length != 0));

          String form = rMan.getString((someOK) ? "fixNonOrtho.partialFail" : "fixNonOrtho.fullFail");
          form = UiUtil.convertMessageToHtml(form);
          Integer failNum = new Integer(ori.failCount);
          String failMsg = MessageFormat.format(form, new Object[] {failNum});    
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString(failMsg), 
                                        rMan.getString("fixNonOrtho.failWarningTitle"),
                                        JOptionPane.WARNING_MESSAGE);
        }
      }      
      return;
    }
   
    public void handleCancellation() {
      return;
    }  
    
    /***************************************************************************
    **
    ** Handle details of fixing non-ortho links:
    */
  
    private Layout.OrthoRepairInfo fixAllNonOrtho(StaticDataAccessContext rcx, 
                                                  boolean minCorners, 
                                                  String overID,
                                                  UndoSupport support,                                              
                                                  BTProgressMonitor monitor, 
                                                  double startFrac, double maxFrac)
                                                    throws AsynchExitRequestException {
      
      List<Intersection> nonOrtho = rcx.getCurrentLayout().getNonOrthoIntersections(rcx, overID);
      Layout.OrthoRepairInfo ori = rcx.getCurrentLayout().fixAllNonOrthoForLayout(nonOrtho, rcx, minCorners, 
                                                                      overID, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(rcx, ori.chgs);
        support.addEdit(emptyC);
      }  
      return (ori);
    } 
    
    /***************************************************************************
    **
    ** Handle details of fixing non-ortho links:
    */
  
    private Layout.OrthoRepairInfo fixAllNonOrthoForTree(LinkProperties lp, StaticDataAccessContext rcx, 
                                                         boolean minCorners,
                                                         String ovrKey,
                                                         UndoSupport support, BTProgressMonitor monitor, 
                                                         double startFrac, double maxFrac)
                                                           throws AsynchExitRequestException { 
      Layout.OrthoRepairInfo ori = rcx.getCurrentLayout().fixAllNonOrthoForTree(lp, rcx, minCorners, 
                                                                    ovrKey, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(rcx, ori.chgs);
        support.addEdit(emptyC);
      }  
      return (ori);
    }
    
     /***************************************************************************
    **
    ** Handle details of fixing non-ortho links:
    */
  
    private Layout.OrthoRepairInfo fixNonOrtho(LinkProperties lp, LinkSegmentID lsid, StaticDataAccessContext rcx,
                                               boolean minCorners, 
                                               String ovrKey,
                                               UndoSupport support, BTProgressMonitor monitor, 
                                               double startFrac, double maxFrac)
                                                 throws AsynchExitRequestException { 
      Layout.OrthoRepairInfo ori = rcx.getCurrentLayout().fixNonOrtho(lp, lsid, rcx, minCorners, ovrKey, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(rcx, ori.chgs);
        support.addEdit(emptyC);
      }  
      return (ori);
    }
  }   
    
  /***************************************************************************
  **
  ** Repair non-ortho links
  */ 
    
  private static class RepairNonOrthoRunner extends BackgroundWorker {
    
    private Genome genome_;    
    private UndoSupport support_;
    private String loKey_;
    private boolean minCorners_;
    private LinkProperties forOne_;
    private LinkSegmentID onlyLsid_;
    private String overID_;
    private StepState ss_;
    private StaticDataAccessContext dacx_;
    
    public RepairNonOrthoRunner(StaticDataAccessContext dacx, Genome genome, 
                                boolean minCorners, LinkProperties forOne, 
                                LinkSegmentID onlyLsid, 
                                String overID, UndoSupport support, StepState ss) {
      super(new Layout.OrthoRepairInfo(0, false));
      dacx_ = dacx;
      support_ = support;
      genome_ = genome;
      minCorners_ = minCorners;
      forOne_ = forOne;
      onlyLsid_ = onlyLsid;
      overID_ = overID;
      ss_ = ss;
    }
    
    public Object runCore() throws AsynchExitRequestException {      
      loKey_ = dacx_.getLayoutSource().mapGenomeKeyToLayoutKey(genome_.getID());
      Layout lo = dacx_.getLayoutSource().getLayout(loKey_);
      Layout.OrthoRepairInfo ori;
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, genome_, lo);  
      if (forOne_ == null) {       
        ori = ss_.fixAllNonOrtho(rcx, minCorners_, overID_, support_, this, 0.01, 1.0); 
      } else if (onlyLsid_ == null) {
        ori = ss_.fixAllNonOrthoForTree(forOne_, rcx, minCorners_, overID_, support_, this, 0.01, 1.0);     
      } else {       
        ori = ss_.fixNonOrtho(forOne_, onlyLsid_, rcx, minCorners_, overID_, support_, this, 0.01, 1.0);
      }
      return (ori);
    }

    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    }  
  }
}
