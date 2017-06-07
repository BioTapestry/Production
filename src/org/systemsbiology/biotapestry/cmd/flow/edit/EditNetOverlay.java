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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.OverlayDisplayChangeEvent;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NetOverlayPropertiesDialogFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle editing network overlays
*/

public class EditNetOverlay extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditNetOverlay(boolean forPopup) {
    name =  (forPopup) ? "netOverlayPopup.Edit" : "command.EditNetworkOverlay";
    desc = (forPopup) ? "netOverlayPopup.Edit" : "command.EditNetworkOverlay";
    mnem =  (forPopup) ? "netOverlayPopup.EditMnem" : "command.EditNetworkOverlayMnem";
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {  
    return (new EditOverlayState(dacx));
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
        EditOverlayState ans = new EditOverlayState(cfh);    
        next = ans.stepGetPropsEditDialog();
      } else {
        EditOverlayState ans = (EditOverlayState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepGetPropsEditDialog")) {
          next = ans.stepGetPropsEditDialog();      
        } else if (ans.getNextStep().equals("stepExtractAndInstallPropsData")) {
          next = ans.stepExtractAndInstallPropsData(last);      
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
        
  public static class EditOverlayState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
       
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditOverlayState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepGetPropsEditDialog";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditOverlayState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepGetPropsEditDialog";
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
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetPropsEditDialog() { 
      NetOverlayPropertiesDialogFactory.NetOverlayPropsArgs ba = new NetOverlayPropertiesDialogFactory.NetOverlayPropsArgs(dacx_);
      NetOverlayPropertiesDialogFactory nopd = new NetOverlayPropertiesDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractAndInstallPropsData";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepExtractAndInstallPropsData(DialogAndInProcessCmd cmd) {
      NetOverlayPropertiesDialogFactory.NetOverlayPropsRequest crq = 
        (NetOverlayPropertiesDialogFactory.NetOverlayPropsRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }  
      
      NetOverlayOwner owner = dacx_.getGenomeSource().getOverlayOwnerFromGenomeKey(dacx_.getCurrentGenomeID());
      NetworkOverlay novr = owner.getNetworkOverlay(dacx_.getOSO().getCurrentOverlay());
      NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(dacx_.getOSO().getCurrentOverlay());
      
      UndoSupport support = uFac_.provideUndoSupport("undo.overlayProp", dacx_);
      boolean submit = false;
        
      if (crq.submitModel) {
        int ownerMode = owner.overlayModeForOwner();
        NetworkOverlayChange noc = novr.changeNameAndDescription(crq.nameResult, crq.descResult, owner.getID(), ownerMode);
        support.addEdit(new NetOverlayChangeCmd(dacx_, noc));
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        submit = true;
      }
     
      if (crq.submitLayout) {
        NetOverlayProperties changedProps = nop.clone();
        changedProps.setType(crq.typeResult);
        changedProps.setHideLinks(crq.hideLinksResult);
        Layout.PropChange lpc = dacx_.getCurrentLayout().replaceNetOverlayProperties(dacx_.getOSO().getCurrentOverlay(), changedProps);     
        if (lpc != null) {
          PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
          support.addEdit(mov);
          support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
          support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.OVERLAY_TYPE_CHANGE));
          submit = true;
        }     
      }
        
      //
      // If we start hiding links, we gotta deselect them:
      //
      
      if (crq.startHiding) {
        SelectionChangeCmd.Bundle bundle = uics_.getSUPanel().dropLinkSelections(uics_, true, uFac_, dacx_);
        if ((bundle != null) && (bundle.cmd != null)) {
          support.addEdit(bundle.cmd);
          support.addEvent(bundle.event);
          submit = true;
        }
      }
        
      if (submit) {
        support.finish();
      }

      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this); // Done  
      return (daipc);
    } 
  }  
}
