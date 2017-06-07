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

package org.systemsbiology.biotapestry.cmd.flow.modelTree;

import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle model node setting ops
*/

public class SettingOps extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SettingAction {
    OVERLAY_FIRST("treePopup.SetCurrOverlayFirst", "treePopup.SetCurrOverlayFirst", "FIXME.gif", "treePopup.SetCurrOverlayFirstMnem", null),
    STARTUP_VIEW("treePopup.MakeStartupView", "treePopup.MakeStartupView", "FIXME.gif", "treePopup.MakeStartupViewMnem", null)  
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    SettingAction(String name, String desc, String icon, String mnem, String accel) {
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
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private SettingAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SettingOps(SettingAction action) {
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a model tree case
  ** 
  */
  
  @Override
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext dacx, UIComponentSource uics) {
    if (key == null) {
      return (false);
    }  
    boolean readOnly = !uics.getIsEditor();
    switch (action_) {
      case OVERLAY_FIRST:      
        if (readOnly) {
          return (false);
        }  
        boolean enableScoff = false;
        if (key != null) {
          NavTree nt = dacx.getGenomeSource().getModelHierarchy();
          TreeNode node = nt.resolveNode(key);
          if ((node == null) || (nt.getGroupNodeID(node) != null)) {
            return (enableScoff);
          }
          NetOverlayOwner noo = key.getOverlayOwner(dacx);
          boolean showOverlayOpts = (noo.getNetworkOverlayCount() > 0);
          // We have to be launching the popup menu on the actual model that is being displayed.
          boolean poppedSelection = key.currentModelMatch(dacx);
          enableScoff = (showOverlayOpts && poppedSelection);
        }
        return (enableScoff);   
      case STARTUP_VIEW: 
        if (readOnly) {
          return (false);
        }
        return (true);  
      default:
        throw new IllegalStateException();
    }     
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
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
        ans = new StepState(action_, cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
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
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.ModelTreeCmdState {

    private SettingAction myAction_;
    private Genome popupModel_;
    private TreeNode popupNode_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupModel_ = popupModel; // May be null if popup is a group node
      popupNode_ = popupNode;
      return;
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {          
      switch (myAction_) {
        case OVERLAY_FIRST:      
          NetOverlayController noc = uics_.getNetOverlayController();
          noc.setCurrentAsFirst(dacx_);
          break;   
        case STARTUP_VIEW:         
          UndoSupport support = uFac_.provideUndoSupport("undo.makeStartupView", dacx_);
          UiUtil.fixMePrintout("WAIT! What about a group node as startup???");
          DatabaseChange dc = dacx_.getGenomeSource().setStartupView(new StartupView(popupModel_.getID(), null, null, null,null));
          DatabaseChangeCmd dcc = new DatabaseChangeCmd(dacx_, dc);
          support.addEdit(dcc);
          support.finish();
          break;           
        default:
          throw new IllegalStateException();
      }     
      uics_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
