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

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Module removal tasks
*/

public class RemoveModuleOrPart extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ModAction {
    REMOVE_ALL("modulePopup.removeThisNetworkModule", "modulePopup.removeThisNetworkModule", "FIXME24.gif", "modulePopup.removeThisNetworkModuleMnem", null),
    REMOVE_REGION("modulePopup.DeleteOneBlock", "modulePopup.DeleteOneBlock", "FIXME24.gif", "modulePopup.DeleteOneBlockMnem", null),
    DETACH_FROM_GROUP("modulePopup.detachModuleFromGroup", "modulePopup.detachModuleFromGroup", "FIXME24.gif", "modulePopup.detachModuleFromGroupMnem", null),
    ;
  
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ModAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }  
     
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private ModAction action_;
  private HardwiredEnabledArgs her_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RemoveModuleOrPart(ModAction action, HardwiredEnabledArgs her) {
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
    her_ = her;
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
    StepState retval = new StepState(action_, dacx);
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
    if (action_ == ModAction.REMOVE_REGION) {
      return (her_.getEnabled());
    } else if (action_ == ModAction.DETACH_FROM_GROUP) {  
      // Can ony detach from group if it attached:
      String moduleID = inter.getObjectID();
      String overlayKey = rcx.getOSO().getCurrentOverlay();
      NetworkOverlay nov = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID()).getNetworkOverlay(overlayKey);     
      NetModule nmod = nov.getModule(moduleID);
      boolean canBeAttached = !rcx.currentGenomeIsRootDBGenome();
      return (canBeAttached && (nmod.getGroupAttachment() != null));     
    }
    return (true);
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
    private ModAction myAction_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ModAction myAction, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = myAction;
      nextStep_ = "stepToRemove";
    }
    
    /***************************************************************************
    **
    ** Set the popup params
    */ 
        
    public void setIntersection(Intersection inter) {
      intersect_ = inter;
      return;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
      String ovrKey = dacx_.getOSO().getCurrentOverlay();
      String netModKey = intersect_.getObjectID();
      UndoSupport support;
      switch (myAction_) {
        case REMOVE_ALL:
          NetOverlayController noc = uics_.getNetOverlayController();
          support = uFac_.provideUndoSupport("undo.removeThisNetworkModule", dacx_);
          noc.dropACurrentModule(netModKey, support, dacx_);
          OverlaySupport.deleteNetworkModule(uics_, dacx_, ovrKey, netModKey, support);
          // This resets the current module options now that the module is gone...
          noc.updateModuleOptions(support, dacx_);
          support.finish();
          break;
        case REMOVE_REGION:        
          support = uFac_.provideUndoSupport("undo.removeNetworkModuleRegion", dacx_);
          NetModuleFree.IntersectionExtraInfo iexi = (NetModuleFree.IntersectionExtraInfo)intersect_.getSubID();
          OverlaySupport.deleteNetworkModuleRegion(dacx_, ovrKey, netModKey, iexi, support);
          break;
        case DETACH_FROM_GROUP:
          support = uFac_.provideUndoSupport("undo.detachModuleFromGroup", dacx_);
          NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();
          NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
          NetModule nmod = novr.getModule(netModKey);
          NetModuleChange nmc = nmod.detachFromGroup(owner.getID(), owner.overlayModeForOwner(), ovrKey);
          if (nmc != null) {
            support.addEdit(new NetOverlayChangeCmd(dacx_, nmc));
            support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
            support.finish();
          }
          break;         
        default:
          throw new IllegalStateException();
      }     
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }
 
  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class HardwiredEnabledArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"enabled"};  
      classes_ = new Class<?>[] {Boolean.class};  
    }

    public boolean getEnabled() {
      return (Boolean.parseBoolean(getValue(0)));
    }
  
    public HardwiredEnabledArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public HardwiredEnabledArgs(boolean enabled) {
      super();
      setValue(0, Boolean.toString(enabled));
      bundle();
    }
  }  
}
