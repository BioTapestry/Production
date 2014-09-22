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

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
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

public class LinkRepair extends AbstractControlFlow implements BackgroundWorkerOwner {

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
  
  public LinkRepair(BTState appState, boolean forModules, boolean forPopup) {
    super(appState); 
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    if (!forPopup_) {
      throw new IllegalStateException();
    }
    if (!isSingleSeg) {
      return (false);
    }
    
    // Allow the operation on incomplete VfN-layer trees:
    if (!forModules_ && rcx.genomeIsAnInstance() && !rcx.genomeIsRootInstance()) {
      rcx = new DataAccessContext(rcx, rcx.getRootInstance());
    }
      
    String oid = inter.getObjectID();
    LinkProperties lp;
    if (forModules_) {
      String ovrKey = rcx.oso.getCurrentOverlay();
      lp = rcx.getLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(oid);
    } else {
      lp = rcx.getLayout().getLinkProperties(oid);
    }
    return (!lp.isDirect());
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
      ResourceManager rMan = appState_.getRMan();
      String msg;
      if (tri.topoFix == BusProperties.SOME_REPAIRED) {
        msg = rMan.getString("linkTopoRepair.partialConvergenceFailWarning");
      } else {
        msg = rMan.getString("linkTopoRepair.convergenceFailWarning");
      }         
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    msg,
                                    rMan.getString("linkTopoRepair.convergenceFailWarningTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    return;
  }
 
  public void handleCancellation() {
    return;
  } 
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new RelayoutState(appState_, forModules_, forPopup_, this, dacx));  
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
        RelayoutState ans = new RelayoutState(appState_, forModules_, forPopup_, this, cfh.getDataAccessContext());
        next = ans.stepDoIt();
      } else {
        RelayoutState ans = (RelayoutState)last.currStateX;
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
        
  public static class RelayoutState implements DialogAndInProcessCmd.PopupCmdState {

    private Intersection intersect_;
    private DataAccessContext rcxT_;
    private String nextStep_;    
    private BTState appState_;
    private boolean myForModules_;
    private boolean myForPopup_;
    private LinkRepair bwo_;
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public RelayoutState(BTState appState, boolean forModules, boolean forPopup, LinkRepair bwo, DataAccessContext dacx) {
      appState_ = appState;
      myForModules_ = forModules;
      myForPopup_ = forPopup;
      bwo_ = bwo;
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
          overID = rcxT_.oso.getCurrentOverlay();
          forOne = rcxT_.getLayout().getNetOverlayProperties(overID).getNetModuleLinkagePropertiesFromTreeID(interID);
        } else {
          forOne = rcxT_.getLayout().getLinkProperties(interID);
        }
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
 
      UndoSupport support = new UndoSupport(appState_, (forOne == null) ? "undo.allLinksTopoRepair" : "undo.linkTopoRepair");
      RepairTopologyRunner runner = new RepairTopologyRunner(appState_, rcxT_, forOne, overID, support);   
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState_, bwo_, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      return (daipc);
    }
  }
  
  /***************************************************************************
  **
  ** Repair link topology flaws
  */ 
    
  private static class RepairTopologyRunner extends BackgroundWorker {
    
    private BTState myAppState_;
    private UndoSupport support_;
    private LinkProperties forOne_;
    private String overID_;
    private DataAccessContext rcx_;
    
    public RepairTopologyRunner(BTState appState, DataAccessContext rcx,
                                LinkProperties forOne, String overID, UndoSupport support) {
      super(new Layout.OrthoRepairInfo(0, false));
      support_ = support;
      myAppState_ = appState;
      rcx_ = rcx;
      forOne_ = forOne;
      overID_ = overID;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      Layout.TopoRepairInfo tri;
      if (forOne_ == null) {     
        tri = rcx_.getLayout().repairAllTopology(rcx_, overID_, this, 0.0, 1.0);     
      } else {
        tri = rcx_.getLayout().repairTreeTopology(forOne_, rcx_, overID_, this, 0.0, 1.0, false);
      }
      if (tri.haveAChange()) {
        int numChg = tri.changes.size();
        for (int i = 0; i < numChg; i++) {
          PropChangeCmd pcc = new PropChangeCmd(myAppState_, rcx_, tri.changes.get(i));
          support_.addEdit(pcc);
        }
      }         
      return (tri);
    }

    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(rcx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    }  
  }
}
