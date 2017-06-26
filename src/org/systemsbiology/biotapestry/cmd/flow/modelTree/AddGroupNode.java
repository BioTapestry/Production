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

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.ExpansionChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.ui.dialogs.TreeGroupNodeCreationDialogFactory;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding a tree group node
*/

public class AddGroupNode extends AbstractControlFlow {

    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddGroupNode() {
    name =  "command.AddGroupNode";
    desc = "command.AddGroupNode";
    mnem =  "command.AddGroupNodeMnem";
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
    } else if (key.modType == XPlatModelNode.ModelType.DYNAMIC_PROXY)  {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.isNotDynamicInstance());
  }

  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    AddGroupNodeState ans = (AddGroupNodeState)cms;
    if (qbom.getLabel().equals("queBombNameMatch")) {
      return (ans.queBombCheckNameMatch(qbom));
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new AddGroupNodeState(dacx));  
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
        AddGroupNodeState ans = new AddGroupNodeState(cfh); 
        next = ans.stepGetCreationDialog();
      } else {
        AddGroupNodeState ans = (AddGroupNodeState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepGetCreationDialog")) {
          next = ans.stepGetCreationDialog();
        } else if (ans.getNextStep().equals("stepExtractNewNodeInfo")) {
          next = ans.stepExtractNewNodeInfo(last);
        } else if (ans.getNextStep().equals("addNewGroupNode")) {   
          next = ans.addNewGroupNode();     
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
        
  public static class AddGroupNodeState extends AbstractStepState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private String newName;  
    private TreeNode popupNode_;
    private List<String> currNames_;
 
    /***************************************************************************
    **
    ** Constructor
    */
      
    public AddGroupNodeState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepGetCreationDialog";
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
      
    public AddGroupNodeState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepGetCreationDialog";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
        
    public void setPreload(Genome genome, Genome popupModelAncestor, TreeNode popupNode) {
      popupNode_ = popupNode;
      return;
    }
 
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombCheckNameMatch(RemoteRequest qbom) {
      
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String nameResult = qbom.getStringArg("nameResult");
      
      if (DataUtil.containsKey(currNames_, nameResult)) {
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
    ** Get dialog to create new genome instance
    */ 
      
    private DialogAndInProcessCmd stepGetCreationDialog() {
      String newName = buildNewName();
      TreeGroupNodeCreationDialogFactory.BuildArgs ba = 
        new TreeGroupNodeCreationDialogFactory.BuildArgs(newName);
      TreeGroupNodeCreationDialogFactory nocdf = new TreeGroupNodeCreationDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractNewNodeInfo";
      return (retval);
    } 
 
    /***************************************************************************
    **
    ** Build a new name
    */ 
       
    private String buildNewName() {
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy(); 
      currNames_ = nt.getChildNames(popupNode_);
      String newFormat = dacx_.getRMan().getString("groupNode.nameFormat");
      int count = 1;
      while (true) {
        String testName = MessageFormat.format(newFormat, Integer.valueOf(count++));
        if (!DataUtil.containsKey(currNames_, testName)) {
          return (testName);
        }
      }
    }

    /***************************************************************************
    **
    ** Extract new genome instance info from the creation dialog
    */ 
       
    private DialogAndInProcessCmd stepExtractNewNodeInfo(DialogAndInProcessCmd cmd) {
         
      TreeGroupNodeCreationDialogFactory.CreateRequest crq = 
        (TreeGroupNodeCreationDialogFactory.CreateRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      } 
      
      newName = crq.nameResult;
      
      nextStep_ = "addNewGroupNode";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    } 

    /***************************************************************************
    **
    ** Add a new genome instance
    */  
       
    private DialogAndInProcessCmd addNewGroupNode() { 
          
      //
      // Undo/Redo support
      //

      UndoSupport support = uFac_.provideUndoSupport("undo.addTreeGroupNode", dacx_);
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH); // We stay in the same place, so don't issue tree change events...
 
      VirtualModelTree vmTree = uics_.getTree();

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //
 
      ExpansionChange ec = (new TreeSupport(uics_)).buildExpansionChange(true, nt);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(ec));      

      //
      // We are adding the new node.  This causes the selection path to be dropped,
      // which means that unless we let people who want to know that they should
      // ignore the selection change, we will be cycling thru null genome and
      // back again, losing things like current selections and overlay settings!
      //
       
      vmTree.setIgnoreSelection(true);
      NavTree.NodeAndChanges nac = nt.addNode(NavTree.Kids.GROUP_NODE, newName, popupNode_, null, null, null, dacx_);
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
     
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, true);
      ec.selected = nt.mapAPath(ec.selected, nac.ntc, true);

      //
      // Tell the world our node structure has changed, make sure we select the
      // parent of the new submodel and expand the path to it.
      //
      
      vmTree.getTreeModel().nodeStructureChanged(popupNode_);
      vmTree.setIgnoreSelection(false);
       
      TreeNode[] tn = ((DefaultMutableTreeNode)popupNode_).getPath();
      TreePath tp = new TreePath(tn);      
      vmTree.setTreeSelectionPath(tp);

      //
      // Expand all the paths to keep the tree as it was.
      //
      
      vmTree.expandTreePath(tp);
      Iterator<TreePath> plit = holdExpanded.iterator();
      while (plit.hasNext()) {
        tp = plit.next();
        vmTree.expandTreePath(tp); 
      }      

      //
      // Collect up the expansion state after the change and add it to the undo
      // set so that redos will restore tree expansion correctly. Also needs mapping
      // to make it consistent with nav change tree representation.
      //

      ec = (new TreeSupport(uics_)).buildExpansionChange(false, nt);
      ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, false);
      ec.selected = nt.mapAPath(ec.selected, nac.ntc, false);      
      support.addEdit(new ExpansionChangeCmd(ec));      

      support.finish();
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);

      //
      // DONE!
      //
       
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }     
  }
}
