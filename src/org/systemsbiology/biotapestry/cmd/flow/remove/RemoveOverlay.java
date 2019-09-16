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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayOwnerChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlayOwnerChange;
import org.systemsbiology.biotapestry.nav.UserTreePathChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Stepping in user tree path
*/

public class RemoveOverlay extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RemoveOverlay(BTState appState, boolean forPopup) {
    super(appState);  
    name = (forPopup) ? "netOverlayPopup.Delete" : "command.RemoveNetworkOverlay"; 
    desc = (forPopup) ? "netOverlayPopup.Delete" : "command.RemoveNetworkOverlay"; 
    icon = "FIXME24.gif";
    mnem = (forPopup) ? "netOverlayPopup.DeleteMnem" : "command.RemoveNetworkOverlayMnem"; 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  public boolean externallyEnabled() {
    return (true);
  }
  
  /***************************************************************************
   **
   ** For programmatic preload
   ** 
   */ 
    
   @Override
   public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
     StepState retval = new StepState(appState_, dacx);
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
      StepState ans;
      // Allowing the last DAIPC to be null fixes Issues #252
      if (last == null) {
        ans = new StepState(appState_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepToRemove")) {
        next = ans.stepToRemove();      
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepToRemove";
      dacx_= dacx;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      return;
    }
       
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {  
      UndoSupport support = new UndoSupport(appState_, "undo.removeNetworkOverlay");
      String ovrKey = dacx_.oso.getCurrentOverlay(); // Gotta record before it goes!
      appState_.getNetOverlayController().clearCurrentOverlay(support, dacx_);
      deleteNetworkOverlay(ovrKey, support);  
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
   
    /***************************************************************************
    **
    ** Delete the specified network overlay
    */  
  
    private void deleteNetworkOverlay(String ovrKey, UndoSupport support) {
     
      NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();    
      UserTreePathController utpc = appState_.getPathController();
      UserTreePathChange[] chgs = utpc.dropStopsOnOverlay(dacx_.getGenomeID(), ovrKey);
      for (int i = 0; i < chgs.length; i++) {
        UserTreePathChangeCmd cmd = new UserTreePathChangeCmd(appState_, dacx_, chgs[i]);
        support.addEdit(cmd);
      }    
     
      Layout.PropChange pc = dacx_.getLayout().removeNetOverlayProperties(ovrKey);
      if (pc != null) {
        support.addEdit(new PropChangeCmd(appState_, dacx_, pc));
      }  
        
      NetworkOverlayOwnerChange gc = owner.removeNetworkOverlay(ovrKey);
      if (gc != null) {
        NetOverlayOwnerChangeCmd gcc = new NetOverlayOwnerChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
      }
  
      support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));      
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      support.finish();    
      return;
    }
  }  
}
