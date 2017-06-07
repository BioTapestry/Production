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
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Link Topology Repair
*/

public class LinkRepair extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
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
  
  public LinkRepair(boolean forModules, boolean forPopup) {
    forModules_ = forModules;
    forPopup_ = forPopup;
    // Modules/non-mod have no effect on naming:
    if (forPopup) {
      name = "linkPopup.TopoRepair"; 
      desc = "linkPopup.TopoRepair"; 
      mnem = "linkPopup.TopoRepairMnem"; 
    } else {
      name = "command.AllLinkTopologyRepair"; 
      desc = "command.AllLinkTopologyRepair"; 
      mnem = "command.AllLinkTopologyRepairMnem";   
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
    return (cache.canLayoutLinks());
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
    
    // Allow the operation on incomplete VfN-layer trees:
    if (!forModules_ && rcx.currentGenomeIsAnInstance() && !rcx.currentGenomeIsRootInstance()) {
      rcx = new StaticDataAccessContext(rcx, rcx.getRootInstanceFOrCurrentInstance());
    }
      
    String oid = inter.getObjectID();
    LinkProperties lp;
    if (forModules_) {
      String ovrKey = rcx.getOSO().getCurrentOverlay();
      lp = rcx.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
    } else {
      lp = rcx.getCurrentLayout().getLinkProperties(oid);
    }
    return (!lp.isDirect());
  }
  
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new RelayoutState(forModules_, forPopup_, dacx));  
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
        RelayoutState ans = new RelayoutState(forModules_, forPopup_, cfh);
        next = ans.stepDoIt();
      } else {
        RelayoutState ans = (RelayoutState)last.currStateX;
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
        
  public static class RelayoutState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {

    private Intersection intersect_;
    private StaticDataAccessContext rcxT_;
    private boolean myForModules_;
    private boolean myForPopup_;
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public RelayoutState(boolean forModules, boolean forPopup, StaticDataAccessContext dacx) {
      super(dacx);
      myForModules_ = forModules;
      myForPopup_ = forPopup;
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
    
    public RelayoutState(boolean forModules, boolean forPopup, ServerControlFlowHarness cfh) {
      super(cfh);
      myForModules_ = forModules;
      myForPopup_ = forPopup;
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
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
        
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
        
    private DialogAndInProcessCmd stepDoIt() {
    
      LinkProperties forOne = null;
      String overID = null;
      
      if (myForPopup_) { 
        String interID = intersect_.getObjectID();
        if (myForModules_) {
          overID = rcxT_.getOSO().getCurrentOverlay();
          forOne = rcxT_.getCurrentLayout().getNetOverlayProperties(overID).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          forOne = rcxT_.getCurrentLayout().getLinkProperties(interID);
        }
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
 
      UndoSupport support = uFac_.provideUndoSupport((forOne == null) ? "undo.allLinksTopoRepair" : "undo.linkTopoRepair", rcxT_);
      RepairTopologyRunner runner = new RepairTopologyRunner(rcxT_, forOne, overID, support);   
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(uics_, rcxT_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
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
       return;
    }
    
    public void cleanUpPostRepaint(Object result) {
      Layout.TopoRepairInfo tri = (Layout.TopoRepairInfo)result;    
      if (tri.convergenceProblem()) {
        ResourceManager rMan = rcxT_.getRMan();
        String msg;
        if (tri.topoFix == BusProperties.SOME_REPAIRED) {
          msg = rMan.getString("linkTopoRepair.partialConvergenceFailWarning");
        } else {
          msg = rMan.getString("linkTopoRepair.convergenceFailWarning");
        }         
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      msg,
                                      rMan.getString("linkTopoRepair.convergenceFailWarningTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      return;
    }
   
    public void handleCancellation() {
      return;
    } 
  }
  
  /***************************************************************************
  **
  ** Repair link topology flaws
  */ 
    
  private static class RepairTopologyRunner extends BackgroundWorker {
    
    private UndoSupport support_;
    private LinkProperties forOne_;
    private String overID_;
    private StaticDataAccessContext rcx_;
    
    public RepairTopologyRunner(StaticDataAccessContext rcx,
                                LinkProperties forOne, String overID, UndoSupport support) {
      super(new Layout.OrthoRepairInfo(0, false));
      support_ = support;
      rcx_ = rcx;
      forOne_ = forOne;
      overID_ = overID;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      Layout.TopoRepairInfo tri;
      if (forOne_ == null) {     
        tri = rcx_.getCurrentLayout().repairAllTopology(rcx_, overID_, this, 0.0, 1.0);     
      } else {
        tri = rcx_.getCurrentLayout().repairTreeTopology(forOne_, rcx_, overID_, this, 0.0, 1.0);
      }
      if (tri.haveAChange()) {
        int numChg = tri.changes.size();
        for (int i = 0; i < numChg; i++) {
          PropChangeCmd pcc = new PropChangeCmd(rcx_, tri.changes.get(i));
          support_.addEdit(pcc);
        }
      }         
      return (tri);
    }

    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(rcx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    }  
  }
}
