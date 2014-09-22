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

package org.systemsbiology.biotapestry.cmd.flow.modelTree;

import java.util.ArrayList;

import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.ui.dialogs.DynSingleModelPropDialog;
import org.systemsbiology.biotapestry.ui.dialogs.SingleInstanceModelPropDialog;
import org.systemsbiology.biotapestry.ui.dialogs.SingleModelPropDialog;

/****************************************************************************
**
** Handle editing model properties
*/

public class EditModelProps extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditModelProps(BTState appState) {
    super(appState);
    name = "treePopup.EditModelProperties";
    desc = "treePopup.EditModelProperties"; 
    mnem = "treePopup.EditModelPropertiesMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled for a model tree case
  ** 
  */
  
  @Override
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext dacx) {
    if (!appState_.getIsEditor() || (key == null)) {
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
      if (last == null) {
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.getNextStep().equals("editProps")) {
          next = ans.editProps();      
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
        
  public static class StepState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private String nextStep_;    
    private BTState appState_;
    private Genome popupTarget_;
    private TreeNode popupNode_;
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
      nextStep_ = "editProps";
      dacx_ = dacx;
    }
      
    /***************************************************************************
    **
    ** for preload
    */ 
        
    public void setPreload(Genome popupTarget, TreeNode popupNode) {
      popupTarget_ = popupTarget;
      popupNode_ = popupNode;
      return;
    }
 
    /***************************************************************************
    **
    ** Edit the properties
    */  
       
    private DialogAndInProcessCmd editProps() { 
      NavTree nav = dacx_.getGenomeSource().getModelHierarchy();
      if (popupTarget_ instanceof DynamicGenomeInstance) {
        // Do dynamic edit
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)popupTarget_;
        DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(dgi.getProxyID());
        // Get the parent proxy (if present)
        //
        // Bogus.  Single-instance dynamic proxies are represented in the
        // tree node by their dynamic instance ID, not the proxy ID.  This
        // workaround gets out the dynamic proxy ID.  YUK!  FIX ME!
        //
        TreeNode parentNode = popupNode_.getParent();
        String proxyID = nav.getDynamicProxyID(parentNode);
        if (proxyID == null) {
          String gid = nav.getGenomeID(parentNode);
          Genome gip = dacx_.getGenomeSource().getGenome(gid);
          if (gip instanceof DynamicGenomeInstance) {
            DynamicGenomeInstance dgip = (DynamicGenomeInstance)gip;
            proxyID = dgip.getProxyID();
          }
        }
        DynamicInstanceProxy parentProx = 
          (proxyID != null) ? dacx_.getGenomeSource().getDynamicProxy(proxyID) : null;
        // Get the child proxies
        ArrayList<DynamicInstanceProxy> childList = new ArrayList<DynamicInstanceProxy>();
        int childCount = popupNode_.getChildCount();
        for (int i = 0; i < childCount; i++) {
          proxyID = nav.getDynamicProxyID(popupNode_.getChildAt(i));
          DynamicInstanceProxy childProx = dacx_.getGenomeSource().getDynamicProxy(proxyID);
          childList.add(childProx);
        }

        DynSingleModelPropDialog dsmpd = 
          new DynSingleModelPropDialog(appState_, dacx_, dip, parentProx, childList, nav, popupNode_);
        dsmpd.setVisible(true);

      } else if (popupTarget_ instanceof DBGenome) {
        SingleModelPropDialog smpd = new SingleModelPropDialog(appState_, dacx_);
        smpd.setVisible(true);
      } else {
        SingleInstanceModelPropDialog simpd = new SingleInstanceModelPropDialog(appState_, dacx_, popupTarget_.getID());
        simpd.setVisible(true);
      }
 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
  }       
}
