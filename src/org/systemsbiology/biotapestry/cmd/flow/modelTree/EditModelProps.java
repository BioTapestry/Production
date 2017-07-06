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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.ui.dialogs.DynSingleModelPropDialog;
import org.systemsbiology.biotapestry.ui.dialogs.GroupNodePropertiesDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.SingleInstanceModelPropDialog;
import org.systemsbiology.biotapestry.ui.dialogs.SingleModelPropDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

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
  
  public EditModelProps() {
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
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext dacx, UIComponentSource uics) {
    if (!uics.getIsEditor() || (key == null)) {
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
   public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
     StepState retval = new StepState(dacx);
     return (retval);
   }
 
  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    StepState ans = (StepState)cms;
    if (qbom.getLabel().equals("queBombCheckSiblingNameMatch")) {
      return (ans.queBombCheckSiblingNameMatch(qbom));
    } else {
      throw new IllegalArgumentException();
    }
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
        if (ans.getNextStep().equals("editProps")) {
          next = ans.editProps(); 
        } else if (ans.getNextStep().equals("stepExtractGroupNodeEditInfo")) {
          next = ans.stepExtractGroupNodeEditInfo(last);
        } else if (ans.getNextStep().equals("editGroupNode")) {   
          next = ans.editGroupNode();          
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private Genome popupModel_;
    private Genome popupModelAncestor_;
    private TreeNode popupNode_;
    private String editedGroupNodeName_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "editProps";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "editProps";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupModel_ = popupModel; // May be null if popup is a group node
      popupModelAncestor_ = popupModelAncestor; // NEVER null
      popupNode_ = popupNode;
      return;
    }
 
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombCheckSiblingNameMatch(RemoteRequest qbom) {
      
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String nameResult = qbom.getStringArg("nameResult");
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy(); 
      Set<String> neighborNames = nt.getSiblingNames(popupNode_);
      
      if (DataUtil.containsKey(neighborNames, nameResult)) {
        String message = dacx_.getRMan().getString("groupNode.dupName");
        String title = dacx_.getRMan().getString("groupNode.dupNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        result.setSimpleUserFeedback(suf);
        result.setDirection(RemoteRequest.Progress.STOP);
        return (result); 
      }
      
      if (nameResult.trim().equals("")) {
        String message = dacx_.getRMan().getString("groupNode.emptyName");
        String title = dacx_.getRMan().getString("groupNode.emptyNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        result.setSimpleUserFeedback(suf);
        result.setDirection(RemoteRequest.Progress.STOP);
        return (result); 
      }
      
      return (result);   
    }

    /***************************************************************************
    **
    ** Get dialog to edit group node properties
    */ 
      
    private DialogAndInProcessCmd getGroupEditDialog(String currName) {
      GroupNodePropertiesDialogFactory.BuildArgs ba = 
        new GroupNodePropertiesDialogFactory.BuildArgs(currName);
      GroupNodePropertiesDialogFactory nocdf = new GroupNodePropertiesDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractGroupNodeEditInfo";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** With the addition of group nodes, we need to dig down layers to find our child proxies:
    */ 
   
    private void recursiveChildProxies(NavTree nav, TreeNode currNode, List<DynamicInstanceProxy> childList) {
      int childCount = currNode.getChildCount();
      for (int i = 0; i < childCount; i++) {
        TreeNode kidNode = currNode.getChildAt(i);
        NavTree.NodeID nid = nav.getNodeIDObj(kidNode);
        NavTree.NavNode nn = nav.navNodeForNodeID(nid, dacx_);
        if (nn.getType() == NavTree.Kids.GROUP_NODE) {
          recursiveChildProxies(nav, kidNode, childList);
        } else {
          String proxyID = nn.getProxyID();
          DynamicInstanceProxy childProx = dacx_.getGenomeSource().getDynamicProxy(proxyID);
          childList.add(childProx);
        }
      }
      return;
    }

    /***************************************************************************
    **
    ** Edit the properties
    */  
       
    private DialogAndInProcessCmd editProps() { 
      NavTree nav = dacx_.getGenomeSource().getModelHierarchy();
      
      UiUtil.fixMePrintout("Why are we creating a ModelProperties that is never used???");
      ModelProperties myProps = null;
      
      NavTree.NavNode navNode = nav.navNodeForNode(popupNode_);
      switch (navNode.getType()) {
        case ROOT_MODEL:
          myProps = new ModelProperties(popupModel_.getID(), popupModel_.getName(),
                                        popupModel_.getLongName(), popupModel_.getDescription(), popupModel_);
          SingleModelPropDialog smpd = new SingleModelPropDialog(uics_, dacx_, uFac_);
          smpd.setVisible(true);
          break;
        case ROOT_INSTANCE: 
        case STATIC_CHILD_INSTANCE:
          myProps = new ModelProperties(popupModel_.getID(), null, popupModel_.getLongName(),
                                        popupModel_.getDescription(), popupModel_);
          SingleInstanceModelPropDialog simpd = new SingleInstanceModelPropDialog(uics_, dacx_, popupModel_.getID(), nav, popupNode_, tSrc_, uFac_);
          simpd.setVisible(true);
          break;
        case DYNAMIC_SUM_INSTANCE:
        case DYNAMIC_SLIDER_INSTANCE:
          // Do dynamic edit
          DynamicGenomeInstance dgi = (DynamicGenomeInstance)popupModel_;
          DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(dgi.getProxyID());
          // Get the parent proxy (if present)
     
          DynamicInstanceProxy parentProx = null;
          if (nav.ancestorIsDynamicSum(popupNode_)) {    
            NavTree.NavNode parNode = nav.getDynamicSumAncestor(popupNode_);
            parentProx = dacx_.getGenomeSource().getDynamicProxy(parNode.getProxyID());
          }
            
          // Get the child proxies
          ArrayList<DynamicInstanceProxy> childList = new ArrayList<DynamicInstanceProxy>();
          recursiveChildProxies(nav, popupNode_, childList);
          myProps = new ModelProperties(dgi.getID(), dgi.getName(), null, dgi.getDescription(), popupModel_, dip,
                                        parentProx, childList, nav);
  
          UiUtil.fixMePrintout("Model name not changing in tree!!!");
          DynSingleModelPropDialog dsmpd = 
            new DynSingleModelPropDialog(uics_, tSrc_, dacx_, dip, parentProx, childList, nav, popupNode_, uFac_);
          dsmpd.setVisible(true);
          break;     
        case GROUP_NODE:
          return (getGroupEditDialog(nav.getNodeName(popupNode_)));
        case HIDDEN_ROOT:
        default:
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this)); 
      }
 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
 
    /***************************************************************************
    **
    ** Extract edited group node info
    */ 
       
    private DialogAndInProcessCmd stepExtractGroupNodeEditInfo(DialogAndInProcessCmd cmd) {
         
      GroupNodePropertiesDialogFactory.CreateRequest crq = (GroupNodePropertiesDialogFactory.CreateRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }    
      editedGroupNodeName_ = crq.nameResult;    
      nextStep_ = "editGroupNode";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Add a new genome instance
    */  
       
    private DialogAndInProcessCmd editGroupNode() { 
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();   
      //
      // Undo/Redo support
      //

 
      UndoSupport support = uFac_.provideUndoSupport("undo.changeGroupNodeName", dacx_);
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH); // We stay in the same place, so don't issue tree change events...
      NavTreeChange ntc = nt.setNodeName(popupNode_, editedGroupNodeName_);    
      support.addEdit(new NavTreeChangeCmd(dacx_, ntc)); 
      support.finish();
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
      //
      // DONE!
      //
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    } 
  } 
  
  ////////////////////
  // ModelProperties
  ////////////////////
  //
  // Wrapper class for Model Properties values
  //
  public static class ModelProperties {
	  public String modelID;
	  public String modelName;
	  public String longName;
	  public String description;
	  public Genome popupModel;
	  public DynamicInstanceProxy dip;
	  public DynamicInstanceProxy parentProxy;
	  public ArrayList<DynamicInstanceProxy> childList;
	  public NavTree nt;
	  
	  
	  public ModelProperties(
		  String modelID, String longName, String modelName, String description, Genome popupModel
		  ,DynamicInstanceProxy dip,DynamicInstanceProxy parentProxy
		  ,ArrayList<DynamicInstanceProxy> childList,NavTree nt
	  ) {
		  this.modelID = modelID;
		  this.modelName = modelName;
		  this.longName = longName;
		  this.description = description;
		  this.popupModel = popupModel;
		  this.dip = dip;
		  this.parentProxy = parentProxy;
		  this.childList = childList;
		  this.nt = nt;
	  }
	  
	  public ModelProperties(String modelID, String longName, String modelName, String description, Genome popupModel) {
		  this(modelID,longName,modelName,description,popupModel,null,null,null,null);
	  }
	  
	  public ModelProperties() {
		  this(null,null,null,null,null,null,null,null,null);
	  }

  }
}
