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

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.AllowReparentOptimizeDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle non-ortho Links
*/

public class OptimizeLink extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean forPopup_;
  private boolean allowReparent_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public OptimizeLink(BTState appState, boolean forPopup, boolean allowReparent) {
    super(appState); 
    forPopup_ = forPopup;
    allowReparent_ = allowReparent;
    if (forPopup_) {
      name = "linkPopup.SegOptimize"; 
      desc = "linkPopup.SegOptimize"; 
      mnem =  "linkPopup.SegOptimizeMnem"; 
    } else { // If used for global fixes:
      name = "command.OptimizeLinks"; 
      desc = "command.OptimizeLinks"; 
      mnem =  "command.OptimizeLinksMnem"; 
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
    // FIX ME!!!! Not valid if not orthogonal (or is non-segmented single)
    //if (slp.hasSegments() && slp.getNonOrthoSegments(genome, lo, frc).isEmpty()) {      
  
    String oid = inter.getObjectID();
    if (rcx.getGenome() instanceof GenomeInstance) {
      if (rcx.getGenomeAsInstance().getVfgParent() != null) {
        return (false);
      }
    }      
    LinkProperties lp = rcx.getLayout().getLinkProperties(oid);
    if (lp.isDirect()) {
      return (false);
    } 
    LinkSegmentID[] ids = inter.segmentIDsFromIntersect();
    if (ids.length != 1) {
      return (false);
    }                     
    if (ids[0].isForEndDrop()) {
      return (false);
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
    return (new StepState(appState_, forPopup_, allowReparent_, dacx));  
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
        StepState ans = new StepState(appState_, forPopup_, allowReparent_, cfh.getDataAccessContext());
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
    private boolean myForPopup_;
    private DataAccessContext rcxT_;
    private boolean myAllowReparent_;
         
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean forPopup, boolean allowReparent, DataAccessContext dacx) {
      appState_ = appState;
      myForPopup_ = forPopup;
      myAllowReparent_ = allowReparent;
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
      if (!myForPopup_) {
        throw new IllegalStateException();
      }
      String interID = inter.getObjectID();
      LinkProperties lp = rcxT_.getLayout().getLinkProperties(interID);
      LinkSegmentID[] ids = inter.segmentIDsFromIntersect();
      if (ids.length != 1) {
        throw new IllegalStateException();
      }                    
      Genome genome = rcxT_.getGenome();
      HashSet<String> linkID = new HashSet<String>();
      linkID.add(lp.getALinkID(genome));  // Just need one; the optimizer works on the whole tree          
      //
      // When launched from tree, only optimize the downstream links:
      //
      HashSet<Point2D> below = new HashSet<Point2D>();
      HashSet<Point2D> notBelow = new HashSet<Point2D>();  // This is what we care about; the "frozen" points!
      lp.splitTreePoints(ids[0], below, notBelow);
      // We have to add the segment's end point as well to keep the ortho optimize
      // algorithm away from messing with this segment!
      if (ids[0].isForSegment()) {
        LinkSegment seg = lp.getSegment(ids[0]);
        if (!seg.isDegenerate()) {
          notBelow.add((Point2D)seg.getEnd().clone());
        }
      }            
      args_ = new Object[3];
      args_[0] = linkID;
      args_[1] = notBelow;
      args_[2] = new Boolean(myAllowReparent_);
      return;
    }
     
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepDoIt() {
      Set<String> links = (args_ == null) ? null : (Set<String>)args_[0];
      Set<Point2D> frozen = (args_ == null) ? new HashSet<Point2D>() : (Set<Point2D>)args_[1];
      Boolean allowReparentObj = (args_ == null) ? null : (Boolean)args_[2];
            
      boolean allowReparent;
      if (allowReparentObj == null) {      
        AllowReparentOptimizeDialog arpd = new AllowReparentOptimizeDialog(appState_);
        arpd.setVisible(true);
        if (!arpd.haveResult()) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this)); 
        }
        allowReparent = arpd.allowReparenting();
      } else {
        allowReparent = allowReparentObj.booleanValue();
      }
       
      //
      // Before starting, we need to clear selections.  This automatically
      // installs a selection undo!  If selections stick around, they are
      // invalid after the layout changes stuff around!
      //
      
      SUPanel sup = appState_.getSUPanel();
      if (sup.hasASelection()) {
        sup.selectNone(appState_.getUndoManager(), rcxT_);
        sup.drawModel(false);
      }
   
      String undoString = (links == null) ? "undo.optimizeLinks" : "undo.optimizeSingleTree"; 
      UndoSupport support = new UndoSupport(appState_, undoString);
      OptimizeRunner runner = new OptimizeRunner(appState_, links, frozen, allowReparent, support, rcxT_);
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
    
    public void handleCancellation() {
      return;
    }     
        
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(appState_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    }  
  }   
    
  /***************************************************************************
  **
  ** Background optimizer
  */ 
    
  private static class OptimizeRunner extends BackgroundWorker {
    
    private BTState myAppState_; 
    private UndoSupport support_;
    private Set<String> links_;
    private Set<Point2D> frozen_;
    private boolean allowReroutes_;
    private String loKey_;
    private DataAccessContext rcx_;
    
    public OptimizeRunner(BTState appState, Set<String> links, Set<Point2D> frozen, 
                          boolean allowReroutes, UndoSupport support, DataAccessContext rcx) {
      super(new LinkRouter.RoutingResult());
      myAppState_ = appState;
      links_ = links;
      allowReroutes_ = allowReroutes;
      frozen_ = frozen;
      support_ = support;
      rcx_ = rcx;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      LinkRouter.RoutingResult result = LayoutLinkSupport.optimizeLinks(myAppState_, links_, frozen_, rcx_, support_, allowReroutes_, this, 0.0, 1.0);
      return (result);
    }
    
    public Object postRunCore() {
      support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
      return (null);
    } 
  }
}
