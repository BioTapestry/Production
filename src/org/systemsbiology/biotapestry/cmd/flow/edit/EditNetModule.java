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

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NetModulePropertiesDialogFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle editing network modules
*/

public class EditNetModule extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditNetModule(BTState appState) {
    super(appState);
    name = "modulePopup.editModuleProps";
    desc = "modulePopup.editModuleProps";
    mnem = "modulePopup.editModulePropsMnem";
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {  
    return (new StepState(appState_, dacx));
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
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    private String modID_;
    private DataAccessContext dacx_;
 
       
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepGetPropsEditDialog";
      dacx_ = dacx;
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
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      modID_ = intersect_.getObjectID(); 
      return;
    }

    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetPropsEditDialog() {  
      NetModulePropertiesDialogFactory.NetModulePropsArgs ba = 
        new NetModulePropertiesDialogFactory.NetModulePropsArgs(appState_, dacx_.getGenome(), modID_);
      NetModulePropertiesDialogFactory nopd = new NetModulePropertiesDialogFactory(cfh);
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
      NetModulePropertiesDialogFactory.NetModulePropsRequest crq = 
        (NetModulePropertiesDialogFactory.NetModulePropsRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
      
      //
      // Gather up all the state we need:
      //
      
      NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();
      int ownerMode = owner.overlayModeForOwner();
      String ovrKey = dacx_.oso.getCurrentOverlay(); 
      NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
      NetOverlayProperties nop = dacx_.getLayout().getNetOverlayProperties(ovrKey);
      NetModule nmod = novr.getModule(modID_);
      NetModuleProperties nmop = nop.getNetModuleProperties(modID_);
      Layout.NetModuleLinkPadRequirements padNeeds = dacx_.getLayout().findNetModuleLinkPadRequirements(owner.getID(), ovrKey, modID_, dacx_);
      HashSet<String> existingMembers = new HashSet<String>();
      Iterator<NetModuleMember> memit = nmod.getMemberIterator();
      while (memit.hasNext()) {
        NetModuleMember nmm = memit.next();
        existingMembers.add(nmm.getID());
      }    
      //
      // Start the operation:
      //
             
      UndoSupport support = new UndoSupport(appState_, "undo.netModProp");
      boolean submit = false;
      
      if (crq.submit || crq.submitTags || crq.submitNV) {
     
        NetModuleChange nmc = nmod.changeNameAndDescription(crq.nameToSubmit, crq.descToSubmit, owner.getID(), ownerMode, ovrKey);
        support.addEdit(new NetOverlayChangeCmd(appState_, dacx_, nmc));
        if (crq.submitTags) {
          nmc = nmod.replaceAllTags(crq.newTags, dacx_.getGenomeID(), ownerMode, ovrKey);
          support.addEdit(new NetOverlayChangeCmd(appState_, dacx_, nmc));
        }
        if (crq.submitNV) {
          nmc = nmod.replaceAllNVPairs(crq.newNvpl, dacx_.getGenomeID(), ownerMode, ovrKey);
          support.addEdit(new NetOverlayChangeCmd(appState_, dacx_, nmc));
        }
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        submit = true;
      }
    
      List<Rectangle2D> convert = null;
      if (crq.typeValChange) {   
        if ((crq.typeVal == NetModuleProperties.MEMBERS_ONLY) && existingMembers.isEmpty()) {
          throw new IllegalStateException();
        }
        convert = nmop.convertShapes(crq.typeVal, existingMembers, dacx_);
      }  
        
      if (crq.colorChange || (convert != null) || crq.fontChange || crq.doLinks || crq.brkChg || crq.hideLabelChg || crq.fadeChg) {
        NetModuleProperties changedProps = nmop.clone();
        changedProps.setColorTag(crq.color1);    
        changedProps.setFillColorTag(crq.color2);
        if (convert != null) {
          changedProps.replaceTypeAndShapes(crq.typeVal, convert);
        }
  
        if (crq.brkChg) {
          changedProps.trimAndSetLineBreakDef(crq.newLineBrkDef, crq.untrimmed);       
        }
             
        if (crq.hideLabelChg) {
          changedProps.setHidingLabel(crq.hideLabel);
        }
        
        if (crq.fadeChg) {
          changedProps.setNameFadeMode(crq.newFade);
        }
   
        //
        // Handle font overrrides:
        //
        if (crq.fontChange) {
          changedProps.setFontOverride(crq.newFont);
        }
        
        
        Layout.PropChange lpc = dacx_.getLayout().replaceNetModuleProperties(modID_, changedProps, ovrKey);     
        if (lpc != null) {
          PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
          support.addEdit(mov);
          support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
          submit = true;
        }
        if (crq.doLinks) {
          Layout.PropChange[] lpcs = dacx_.getLayout().applySameColorToModuleAndLinks(ovrKey, modID_, crq.color1, false, null, dacx_);
          if (lpcs.length > 0) {
            PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpcs);
            support.addEdit(mov);     
            support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
            submit = true;
          }
        }
      }
    
      NetModule.FullModuleKey fmk = new NetModule.FullModuleKey(ownerMode, owner.getID(), ovrKey, modID_);
      Layout.PropChange[] lpcs = dacx_.getLayout().fixNetModuleLinkPads(fmk, padNeeds, dacx_, false);
      if (lpcs.length > 0) {
        PropChangeCmd lpc = new PropChangeCmd(appState_, dacx_, lpcs);
        support.addEdit(lpc);     
        support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        submit = true;
      }     
      
      if (submit) {
        support.finish();
      }
      
      // Either done or looping:
      DialogAndInProcessCmd daipc;
      if (crq.isForApply()) {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.APPLY_PROCESSED, this); // Keep going
        nextStep_ = "stepExtractAndInstallPropsData";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this); // Done
      }    
      return (daipc);
    } 
  }  
}
