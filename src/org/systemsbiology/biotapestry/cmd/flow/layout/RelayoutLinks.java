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

import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle laying out links
*/

public class RelayoutLinks extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean doFull_;
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
  
  public RelayoutLinks(BTState appState, boolean doFull, boolean forPopup) {
    super(appState);
    if (forPopup) {
      name =  "linkPopup.SegLayout"; 
      desc =  "linkPopup.SegLayout"; 
      mnem =  "linkPopup.SegLayoutMnem"; 
    } else {
      name = (doFull) ? "command.RelayoutLinks" : "command.RelayoutDiagLinks"; 
      desc = (doFull) ? "command.RelayoutLinks" : "command.RelayoutDiagLinks"; 
      mnem = (doFull) ? "command.RelayoutLinksMnem" : "command.RelayoutDiagLinksMnem";   
    }
    doFull_ = doFull;
    forPopup_ = forPopup;
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
    return (cache.isRootOrRootInstance());
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
    if (rcx.getGenome() instanceof GenomeInstance) {
      if (rcx.getGenomeAsInstance().getVfgParent() != null) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new RelayoutState(appState_, doFull_, forPopup_, dacx));  
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
        RelayoutState ans = new RelayoutState(appState_, doFull_, forPopup_, cfh.getDataAccessContext());
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
        
  public static class RelayoutState implements DialogAndInProcessCmd.PopupCmdState, 
                                               BackgroundWorkerOwner {

    private String nextStep_; 
    private DataAccessContext rcxT_;
    private BTState appState_;
    private boolean myDoFull_;
    private boolean myForPopup_;
    private Set<String> linkIDs_;
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public RelayoutState(BTState appState, boolean doFull, boolean forPopup, DataAccessContext dacx) {
      appState_ = appState;
      myDoFull_ = doFull;
      myForPopup_ = forPopup;
      rcxT_ = dacx;
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
      BusProperties bp = rcxT_.getLayout().getLinkProperties(inter.getObjectID());
      LinkSegmentID[] ids = inter.segmentIDsFromIntersect();
      if (ids.length != 1) {
        throw new IllegalStateException();
      }          
      Set<String> linkIDs = bp.resolveLinkagesThroughSegment(ids[0]);
      linkIDs_ = new HashSet<String>(linkIDs);
      return;
    }
        
    /***************************************************************************
    **
    ** Do it
    */ 
        
    private DialogAndInProcessCmd stepDoIt() {
      if (!myDoFull_ && !myForPopup_) {
        //
        // WJRL 5/1/08:  Used to be that relayout diag links did the entire tree that had
        // a crooked link.  This at least just does the links that are crooked, though this
        // currently also ditches all ortho segments upstream of the crooked ones that contain
        // the same links
        //
        linkIDs_ = rcxT_.getLayout().getLimitedNonOrthoLinks(rcxT_);
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
      
      appState_.getCommonView().disableControls();
      String undoString;
      if (myDoFull_ && !myForPopup_) {
        undoString = "undo.relayoutLinks";
      } else if (!myDoFull_ && !myForPopup_) {
        undoString = "undo.relayoutDiagLinks"; 
      } else {
        undoString = "undo.relayoutLinksThruSegment"; 
      }
      
      UndoSupport support = new UndoSupport(appState_, undoString);         
      LayoutRunner runner = new LayoutRunner(appState_, myDoFull_, linkIDs_, support, rcxT_);
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
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
  
    public void handleCancellation() {
      return;
    }  
  
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(appState_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      LayoutLinkSupport.offerColorFixup(appState_, rcxT_, (LinkRouter.RoutingResult)result);
      return;
    }  
  }
  
  /***************************************************************************
  **
  ** Background layout runner
  */ 
    
  private static class LayoutRunner extends BackgroundWorker {
    
    private BTState myAppState_;
    private boolean doFull_;
    private UndoSupport support_;
    private Set<String> links_;
    private String loKey_;
    private DataAccessContext rcx_;
    
    public LayoutRunner(BTState appState, boolean doFull, Set<String> links, UndoSupport support, DataAccessContext rcx) {
      super(new LinkRouter.RoutingResult());      
      myAppState_ = appState;
      doFull_ = doFull;
      rcx_ = rcx;
      links_ = links;
      support_ = support;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      LayoutOptions lopt = new LayoutOptions(myAppState_.getLayoutOptMgr().getLayoutOptions());
      LinkRouter.RoutingResult res = LayoutLinkSupport.relayoutLinks(myAppState_, rcx_, support_, lopt, doFull_, links_, this, 0.0, 1.0, false);
      return (res);
    }
    
    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    }
  } 
}
