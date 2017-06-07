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


package org.systemsbiology.biotapestry.cmd.flow.link;

import java.awt.Point;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding a corner point
*/

public class Divide extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private boolean forModules_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Divide(boolean forModules) {
    forModules_ = forModules; 
    name = (forModules) ? "modulelinkPopup.Divide" : "linkPopup.Divide";
    desc = (forModules) ? "modulelinkPopup.Divide" : "linkPopup.Divide";
    mnem =  (forModules) ? "modulelinkPopup.DivideMnem" : "linkPopup.DivideMnem";
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(forModules_, dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    return (isSingleSeg);
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
        throw new IllegalArgumentException();
      } else { 
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepDivide")) {
        next = ans.stepDivide();
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
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupPointCmdState {
    
    private Point2D popPoint_;
    private boolean myForModules_;
    private Intersection intersect_;  
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forModules, StaticDataAccessContext dacx) {
      super(dacx);
      myForModules_ = forModules;
      nextStep_ = "stepDivide";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forModules, ServerControlFlowHarness cfh) {
      super(cfh);
      myForModules_ = forModules;
      nextStep_ = "stepDivide";
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setPopupPoint(Point2D popPoint) {
      popPoint_ = popPoint;
      return;
    }
 
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepDivide() {
      Point pt = new Point();
      dacx_.getZoomTarget().transformClick(popPoint_.getX(), popPoint_.getY(), pt);
      Layout.PropChange[] lumv = new Layout.PropChange[1];
      LinkSegmentID segID = intersect_.segmentIDFromIntersect();
      Point2D forcedSplit = new Point2D.Double();         
      UiUtil.forceToGrid(pt.getX(), pt.getY(), forcedSplit, UiUtil.GRID_SIZE);
      String interID = intersect_.getObjectID();
      LinkProperties lp;
      String ovrKey = null;
      if (myForModules_) {
        ovrKey = dacx_.getOSO().getCurrentOverlay();
        lp = dacx_.getCurrentLayout().getNetOverlayProperties(ovrKey).getNetModuleLinkagePropertiesFromTreeID(interID);
      } else {
        lp = dacx_.getCurrentLayout().getLinkProperties(interID);
      }
      lumv[0] = dacx_.getCurrentLayout().splitBusLink(segID, forcedSplit, lp, ovrKey, dacx_);
      if (lumv[0] != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.linkDivide", dacx_);
        PropChangeCmd lmov = new PropChangeCmd(dacx_, lumv);
        support.addEdit(lmov);
        uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);
        support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
      }       
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }  
}
