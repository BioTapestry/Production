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

package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of a module link or corner point
*/

public class RemoveModuleLinkOrPoint extends AbstractControlFlow {

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private boolean forPoint_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RemoveModuleLinkOrPoint(boolean forPoint) {
    forPoint_ = forPoint;  
    name = (forPoint_) ? "linkPointPopup.Delete" : "modulelinkPopup.LinkDelete";
    desc = (forPoint_) ? "linkPointPopup.Delete" : "modulelinkPopup.LinkDelete";     
    mnem = (forPoint_) ? "linkPointPopup.DeleteMnem": "modulelinkPopup.LinkDeleteMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!forPoint_) {
      return (true);
    }
    String oid = inter.getObjectID();
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(rcx.getOSO().getCurrentOverlay());
    NetModuleLinkageProperties nmp = nop.getNetModuleLinkagePropertiesFromTreeID(oid);
    LinkSegmentID segID = inter.segmentIDFromIntersect();
    if (segID.isBusNodeConnection()) {
      return (false);
    } else if (!nmp.hasSimpleConnectivity(segID)) {
      return (false);             
    } else {
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(forPoint_, dacx);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepToRemove")) {
          next = ans.stepToRemove();     
        } else if (ans.getNextStep().equals("stepToRemovePoint")) {
          next = ans.stepToRemovePoint();        
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forPoint, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = (forPoint) ? "stepToRemovePoint" : "stepToRemove";
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
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
        
      String overlayKey = dacx_.getOSO().getCurrentOverlay();
      UndoSupport support = uFac_.provideUndoSupport("undo.moduleLinkDelete", dacx_);
      String treeID = intersect_.getObjectID();
      LinkSegmentID segID = intersect_.segmentIDFromIntersect();
      NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(overlayKey);
      NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      Set<String> resolved = nmlp.resolveLinkagesThroughSegment(segID);
      if (OverlaySupport.deleteNetworkModuleLinkageSet(dacx_, overlayKey, resolved, support)) {
        uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);
        support.finish();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
         
    private DialogAndInProcessCmd stepToRemovePoint() {       
      String overlayKey = dacx_.getOSO().getCurrentOverlay();
      String treeID = intersect_.getObjectID();
      NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(overlayKey);
      NetModuleLinkageProperties nmp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      LinkSegmentID segID = intersect_.segmentIDFromIntersect();
      Layout.PropChange[] lpc = new Layout.PropChange[1];
      lpc[0] = dacx_.getCurrentLayout().deleteCornerForNetModuleLinkTree(nmp, segID, overlayKey, dacx_);          
      if (lpc[0] != null) {
        UndoSupport support = uFac_.provideUndoSupport("undo.deleteCorner", dacx_);
        PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
        support.addEdit(mov);
        uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);
        support.finish();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
