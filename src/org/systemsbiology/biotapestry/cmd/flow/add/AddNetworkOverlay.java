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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.util.HashSet;
import java.util.Iterator;

import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayOwnerChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayOwnerChange;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NetOverlayCreationDialogFactory;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleAlphaBuilder;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding a network overlay
*/

public class AddNetworkOverlay extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNetworkOverlay() {
    super();
    name =  "command.AddNetworkOverlay";
    desc = "command.AddNetworkOverlay";
    icon = "CreateOverlay24.gif";
    mnem =  "command.AddNetworkOverlayMnem";
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
    return (cache.genomeNotNull());
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
        AddOverlayState ans = new AddOverlayState(cfh);
        ans.setNextStep("stepGetOverlayCreationDialog");
        next = ans.stepGetOverlayCreationDialog();
      } else {
        AddOverlayState ans = (AddOverlayState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepExtractNewOverlayInfo")) {
          next = ans.stepExtractNewOverlayInfo(last);
        } else if (ans.getNextStep().equals("maybeLetUserDecide")) {
          next = ans.maybeLetUserDecide();      
        } else if (ans.getNextStep().equals("handleVizResponse")) {   
          next = ans.handleVizResponse(last);    
        } else if (ans.getNextStep().equals("addNewNetworkOverlay")) {   
          next = ans.addNewNetworkOverlay();     
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
  ** Running State: Kinda needs cleanup!
  */
        
  public static class AddOverlayState extends AbstractStepState {
    
    private String newName;
    private NetOverlayProperties.OvrType overType;
    private boolean hideLinks;
    private boolean changeViz;  
    private NetOverlayOwner owner_;
    private String nextNextStep;  
     
    public AddOverlayState(ServerControlFlowHarness cfh) {
      super(cfh);
    }
  
    /***************************************************************************
    **
    ** Get dialog to create new overlay
    */ 
      
    private DialogAndInProcessCmd stepGetOverlayCreationDialog() {    
      HashSet<String> existingNames = new HashSet<String>();
      owner_ = dacx_.getCurrentOverlayOwner();
      Iterator<NetworkOverlay> oit = owner_.getNetworkOverlayIterator();
      while (oit.hasNext()) {
        NetworkOverlay no = oit.next();
        existingNames.add(no.getName());
      }
         
      NetOverlayCreationDialogFactory.NetOverlayBuildArgs ba = new NetOverlayCreationDialogFactory.NetOverlayBuildArgs(existingNames, dacx_);
      NetOverlayCreationDialogFactory nocdf = new NetOverlayCreationDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractNewOverlayInfo";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract new overlay info from the creation dialog
    */ 
       
    private DialogAndInProcessCmd stepExtractNewOverlayInfo(DialogAndInProcessCmd cmd) {
         
      NetOverlayCreationDialogFactory.NetOverlayCreationRequest crq = (NetOverlayCreationDialogFactory.NetOverlayCreationRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      } 
      
      newName = crq.nameResult;
      overType = crq.typeResult;
      hideLinks = crq.hideLinksResult; 
  
      nextStep_ = "maybeLetUserDecide";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    } 

    /***************************************************************************
    **
    ** Let user know if masking is going to occur, let them change viz:
    */  
    
    private DialogAndInProcessCmd maybeLetUserDecide() {
      
      //
      // If the user is adding an opaque overlay to a model with something in it, 
      // and the intensity is fully masking, suggest they turn the intensity down
      // 
       
      changeViz = false;
      NetModuleFree.CurrentSettings overSettings = dacx_.getOSO().getCurrentOverlaySettings();
      if ((overType == NetOverlayProperties.OvrType.OPAQUE) && 
          ((overSettings.intersectionMask != NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED) ||
           (overSettings.intersectionMask != NetModuleFree.CurrentSettings.NOTHING_MASKED))) {
        if (!dacx_.getCurrentGenome().isEmpty()) {
          String message = UiUtil.convertMessageToHtml(dacx_.getRMan().getString("overlayAdded.resetViz"));
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, message, dacx_.getRMan().getString("overlayAdded.resetVizTitle"));
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
          nextStep_ = "handleVizResponse";
          nextNextStep = "addNewNetworkOverlay";
          return (retval);
        }
      }
       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "addNewNetworkOverlay"; 
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Collect up the viz answer, and change viz if user indicates
    */   
  
    private DialogAndInProcessCmd handleVizResponse(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.CANCEL) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        changeViz = (changeVizVal == SimpleUserFeedback.YES);
        nextStep_ = nextNextStep;
      }
      return (retval);
    }
   
    /***************************************************************************
    **
    ** Add a new overlay.
    */  
       
    private DialogAndInProcessCmd addNewNetworkOverlay() {            
      UndoSupport support = uFac_.provideUndoSupport("undo.newNetworkOverlay", dacx_);
      String viewID = dacx_.getNextKey();
      NetworkOverlay nmView = new NetworkOverlay(dacx_, viewID, newName, null);
      NetworkOverlayOwnerChange gc = owner_.addNetworkOverlay(nmView);
      if (gc != null) {
        NetOverlayOwnerChangeCmd gcc = new NetOverlayOwnerChangeCmd(dacx_, gc);
        support.addEdit(gcc);
      }
       
      NetOverlayProperties noProps = new NetOverlayProperties(viewID, this.overType, hideLinks);
      Layout.PropChange pc = dacx_.getCurrentLayout().setNetOverlayProperties(viewID, noProps);
      if (pc != null) {
        support.addEdit(new PropChangeCmd(dacx_, pc));
      }    
       
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), owner_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE)); 
      
      //
      // FIX ME! This viz change stuff belongs in visibility result??
      // 
      uics_.getNetOverlayController().setCurrentOverlay(viewID, support, dacx_);
      support.finish();
      if (changeViz) {
        uics_.getNetOverlayController().setSliderValue(NetModuleAlphaBuilder.MINIMAL_ALL_MEMBER_VIZ);
      }
       
      //
      // DONE!
      //
       
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }     
  }
}
