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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
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
  
  public LayoutNonOrthoLink(BTState appState, boolean wholeTree, boolean minCorners, boolean forPopup, boolean forModules) {
    super(appState); 
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    if (!forPopup_) {
      throw new IllegalStateException();
    }
    if (!isSingleSeg) {
      return (false);
    }
    String oid = inter.getObjectID();
    
    // Allow the operation on incomplete VfN-layer trees:
    if (!forModules_ && rcx.genomeIsAnInstance() && !rcx.genomeIsRootInstance()) {
      rcx = new DataAccessContext(rcx, rcx.getRootInstance());
    }
    
    if (wholeTree_) {   
      LinkProperties lp;
      if (forModules_) {
        String ovrKey = rcx.oso.getCurrentOverlay();
        lp = rcx.getLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
      } else {
        lp = rcx.getLayout().getLinkProperties(oid);
      }   
      Set<LinkSegmentID> nonOrtho = lp.getNonOrthoSegments(rcx);
      return (!nonOrtho.isEmpty());
    } else {
      LinkSegmentID segID = inter.segmentIDFromIntersect();
      LinkProperties lp;
      if (forModules_) {
        String ovrKey = rcx.oso.getCurrentOverlay();
        lp = rcx.getLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
      } else {
        lp = rcx.getLayout().getLinkProperties(oid);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new StepState(appState_, wholeTree_, minCorners_, forPopup_, forModules_, dacx));  
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
        StepState ans = new StepState(appState_, wholeTree_, minCorners_, forPopup_, forModules_, cfh.getDataAccessContext());
        next = ans.stepDoIt();
      } else {
        StepState ans = (StepState)last.currStateX;
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {

    private Object[] args_;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext rcxT_;
    private boolean myMinCorners_;
    private boolean myForPopup_;
    private boolean myWholeTree_;
    private boolean myForModules_;
         
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean wholeTree, boolean minCorners, boolean forPopup, boolean forModules, DataAccessContext dacx) {
      appState_ = appState;      
      myMinCorners_ = minCorners;
      myForPopup_ = forPopup;
      myWholeTree_ = wholeTree;
      myForModules_ = forModules;
      // Allow the operation on incomplete VfN-layer trees:
      if (!myForModules_ && dacx.genomeIsAnInstance() && !dacx.genomeIsRootInstance()) {
        rcxT_ = new DataAccessContext(dacx, dacx.getRootInstance());
      } else {   
        rcxT_ = dacx;
      }
      nextStep_ = "stepDoIt";
    }
    
    /***************************************************************************
    **
    ** Next step...
    */ 
     
    public String getNextStep() {
      return (nextStep_);
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
          String ovrKey = rcxT_.oso.getCurrentOverlay();
          lp = rcxT_.getLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          lp = rcxT_.getLayout().getLinkProperties(interID);
        }
        args_ = new Object[4];
        args_[0] = new Boolean(myMinCorners_);
        args_[1] = lp;
        args_[2] = null;
        args_[3] = (myForModules_) ? rcxT_.oso.getCurrentOverlay() : null;
      } else {
        LinkProperties lp;
        if (myForModules_) {
          String ovrKey = rcxT_.oso.getCurrentOverlay();
          lp = rcxT_.getLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          lp = rcxT_.getLayout().getLinkProperties(interID);
        }
        LinkSegmentID id = inter.segmentIDFromIntersect();
        args_ = new Object[4];
        args_[0] = new Boolean(myMinCorners_);
        args_[1] = lp;
        args_[2] = id;
        args_[3] = (myForModules_) ? rcxT_.oso.getCurrentOverlay() : null;    
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
      
      SUPanel sup = appState_.getSUPanel();
      if (sup.hasASelection()) {
        sup.selectNone(appState_.getUndoManager(), rcxT_);
        sup.drawModel(false);
      }

      UndoSupport support;
      if (forOne == null) {
        support = new UndoSupport(appState_, "undo.repairAllNonOrtho"); 
      } else if (onlyLsid == null) {
        support = new UndoSupport(appState_, "undo.fixAllNonOrthoForTree");
      } else {
        support = new UndoSupport(appState_, "undo.fixNonOrtho");
      }
  
      Genome genome = rcxT_.getGenome();
      RepairNonOrthoRunner runner = 
        new RepairNonOrthoRunner(appState_, genome, useForMin, forOne, onlyLsid, overID, support, this);
      BackgroundWorkerClient bwc;     
      if (!appState_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, support);
      }
      runner.setClient(bwc);
      bwc.launchWorker();
            // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
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
      ResourceManager rMan = appState_.getRMan();
      Layout.OrthoRepairInfo ori = (Layout.OrthoRepairInfo)result;
 // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>   // myGenomePre_.clearSelections(genomeKey_, layout_, support);
      if (ori.failCount > 0) {
        if (ori.singleSeg) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("fixNonOrthoSeg.failWarning"), 
                                        rMan.getString("fixNonOrthoSeg.failWarningTitle"),
                                        JOptionPane.WARNING_MESSAGE);         
        } else {
          boolean someOK = ((ori.chgs != null) && (ori.chgs.length != 0));

          String form = rMan.getString((someOK) ? "fixNonOrtho.partialFail" : "fixNonOrtho.fullFail");
          form = UiUtil.convertMessageToHtml(form);
          Integer failNum = new Integer(ori.failCount);
          String failMsg = MessageFormat.format(form, new Object[] {failNum});    
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
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
  
    private Layout.OrthoRepairInfo fixAllNonOrtho(BTState appState, DataAccessContext rcx, 
                                                  boolean minCorners, 
                                                  String overID,
                                                  UndoSupport support,                                              
                                                  BTProgressMonitor monitor, 
                                                  double startFrac, double maxFrac)
                                                    throws AsynchExitRequestException {
      
      List<Intersection> nonOrtho = rcx.getLayout().getNonOrthoIntersections(rcx, overID);
      Layout.OrthoRepairInfo ori = rcx.getLayout().fixAllNonOrthoForLayout(nonOrtho, rcx, minCorners, 
                                                                      overID, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(appState, rcx, ori.chgs);
        support.addEdit(emptyC);
      }  
      return (ori);
    } 
    
    /***************************************************************************
    **
    ** Handle details of fixing non-ortho links:
    */
  
    private Layout.OrthoRepairInfo fixAllNonOrthoForTree(BTState appState, LinkProperties lp, DataAccessContext rcx, 
                                                         boolean minCorners,
                                                         String ovrKey,
                                                         UndoSupport support, BTProgressMonitor monitor, 
                                                         double startFrac, double maxFrac)
                                                           throws AsynchExitRequestException { 
      Layout.OrthoRepairInfo ori = rcx.getLayout().fixAllNonOrthoForTree(lp, rcx, minCorners, 
                                                                    ovrKey, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(appState, rcx, ori.chgs);
        support.addEdit(emptyC);
      }  
      return (ori);
    }
    
     /***************************************************************************
    **
    ** Handle details of fixing non-ortho links:
    */
  
    private Layout.OrthoRepairInfo fixNonOrtho(BTState appState, LinkProperties lp, LinkSegmentID lsid, DataAccessContext rcx,
                                               boolean minCorners, 
                                               String ovrKey,
                                               UndoSupport support, BTProgressMonitor monitor, 
                                               double startFrac, double maxFrac)
                                                 throws AsynchExitRequestException { 
      Layout.OrthoRepairInfo ori = rcx.getLayout().fixNonOrtho(lp, lsid, rcx, minCorners, ovrKey, monitor, startFrac, maxFrac); 
      if ((ori.chgs != null) && (ori.chgs.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(appState, rcx, ori.chgs);
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
    
    private BTState myAppState_;
    private Genome genome_;    
    private UndoSupport support_;
    private String loKey_;
    private boolean minCorners_;
    private LinkProperties forOne_;
    private LinkSegmentID onlyLsid_;
    private String overID_;
    private StepState ss_;
    
    public RepairNonOrthoRunner(BTState appState, Genome genome, 
                                boolean minCorners, LinkProperties forOne, 
                                LinkSegmentID onlyLsid, 
                                String overID, UndoSupport support, StepState ss) {
      super(new Layout.OrthoRepairInfo(0, false));
      support_ = support;
      myAppState_ = appState;
      genome_ = genome;
      minCorners_ = minCorners;
      forOne_ = forOne;
      onlyLsid_ = onlyLsid;
      overID_ = overID;
      ss_ = ss;
    }
    
    public Object runCore() throws AsynchExitRequestException {      
      loKey_ = myAppState_.getLayoutMgr().getLayout(genome_.getID());
      Layout lo = myAppState_.getDB().getLayout(loKey_);
      Layout.OrthoRepairInfo ori;
      DataAccessContext rcx = new DataAccessContext(myAppState_, genome_, lo);  
      if (forOne_ == null) {       
        ori = ss_.fixAllNonOrtho(myAppState_, rcx, minCorners_, overID_, support_, this, 0.01, 1.0); 
      } else if (onlyLsid_ == null) {
        ori = ss_.fixAllNonOrthoForTree(myAppState_, forOne_, rcx, minCorners_, overID_, support_, this, 0.01, 1.0);     
      } else {       
        ori = ss_.fixNonOrtho(myAppState_, forOne_, onlyLsid_, rcx, minCorners_, overID_, support_, this, 0.01, 1.0);
      }
      return (ori);
    }

    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    }  
  }
}
