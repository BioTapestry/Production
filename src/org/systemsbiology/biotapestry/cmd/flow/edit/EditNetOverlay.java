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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
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
  
  public EditNetOverlay(BTState appState, boolean forPopup) {
    super(appState);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {  
    return (new EditOverlayState(appState_, dacx));
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
        EditOverlayState ans = new EditOverlayState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepGetPropsEditDialog();
      } else {
        EditOverlayState ans = (EditOverlayState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
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
        
  public static class EditOverlayState implements DialogAndInProcessCmd.PopupCmdState {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext rcxT_;
       
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditOverlayState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepGetPropsEditDialog";
      rcxT_ = dacx;
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
    ** Next step...
    */ 
      
    public String getNextStep() {
      return (nextStep_);
    }
       
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetPropsEditDialog() { 
      NetOverlayPropertiesDialogFactory.NetOverlayPropsArgs ba = new NetOverlayPropertiesDialogFactory.NetOverlayPropsArgs(appState_);
      NetOverlayPropertiesDialogFactory nopd = new NetOverlayPropertiesDialogFactory(cfh);
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
      
      NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
      NetworkOverlay novr = owner.getNetworkOverlay(appState_.getCurrentOverlay());
      NetOverlayProperties nop = rcxT_.getLayout().getNetOverlayProperties(appState_.getCurrentOverlay());
      
      UndoSupport support = new UndoSupport(appState_, "undo.overlayProp");
      boolean submit = false;
        
      if (crq.submitModel) {
        int ownerMode = owner.overlayModeForOwner();
        NetworkOverlayChange noc = novr.changeNameAndDescription(crq.nameResult, crq.descResult, owner.getID(), ownerMode);
        support.addEdit(new NetOverlayChangeCmd(appState_, rcxT_, noc));
        support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        submit = true;
      }
     
      if (crq.submitLayout) {
        NetOverlayProperties changedProps = nop.clone();
        changedProps.setType(crq.typeResult);
        changedProps.setHideLinks(crq.hideLinksResult);
        Layout.PropChange lpc = rcxT_.getLayout().replaceNetOverlayProperties(appState_.getCurrentOverlay(), changedProps);     
        if (lpc != null) {
          PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, lpc);
          support.addEdit(mov);
          support.addEvent(new LayoutChangeEvent(appState_.getLayoutKey(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
          support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.OVERLAY_TYPE_CHANGE));
          submit = true;
        }     
      }
        
      //
      // If we start hiding links, we gotta deselect them:
      //
      
      if (crq.startHiding) {
        SelectionChangeCmd.Bundle bundle = appState_.getSUPanel().dropLinkSelections(true, null, rcxT_);
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
