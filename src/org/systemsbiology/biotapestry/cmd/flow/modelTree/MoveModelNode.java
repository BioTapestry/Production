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

import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.ExpansionChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle moving a model node
*/

public class MoveModelNode extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean doLower_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MoveModelNode(BTState appState, boolean doLower) {
    super(appState);
    name = (doLower) ? "treePopup.MoveNodeDown" : "treePopup.MoveNodeUp";
    desc = (doLower) ? "treePopup.MoveNodeDown" : "treePopup.MoveNodeUp"; 
    mnem = (doLower) ? "treePopup.MoveNodeDownMnem" : "treePopup.MoveNodeUpMnem";
    doLower_ = doLower;
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
    } else if (key.modType == XPlatModelNode.ModelType.DB_GENOME) {
      return (false);
    } else {
      NavTree nt = dacx.getGenomeSource().getModelHierarchy();
      TreeNode node = nt.resolveNode(key, dacx);
      if (node == null) {
        return (false);
      }
      return (nt.canShiftNode((DefaultMutableTreeNode)node, doLower_));
    }
  }
    
  /***************************************************************************
   **
   ** For programmatic preload
   ** 
   */ 
    
   @Override
   public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
     StepState retval = new StepState(appState_, doLower_, dacx);
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
        if (ans.getNextStep().equals("moveNode")) {
          next = ans.moveNode();      
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
    private TreeSupport treeSupp_;
    private boolean myDoLower_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean doLower, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "moveNode";
      treeSupp_ = new TreeSupport(appState_);
      myDoLower_ = doLower;
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
    ** add dynamic genome instance
    */  
       
    private DialogAndInProcessCmd moveNode() { 
      VirtualModelTree vmTree = appState_.getTree();     

      //
      // Undo/Redo support
      //
        
      String whichKey = (myDoLower_) ? "undo.moveModelDownInTree" : "undo.moveModelUpInTree";       
      UndoSupport support = new UndoSupport(appState_, whichKey);       

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //

      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      ExpansionChange ec = treeSupp_.buildExpansionChange(true, dacx_);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec)); 
      
      //
      // Do actual move
      //

      NavTreeChange ntc = nt.shiftNode((DefaultMutableTreeNode)popupNode_, myDoLower_);
      if (ntc == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this)); 
      }       
      NavTreeChangeCmd ntcc = new NavTreeChangeCmd(appState_, dacx_, ntc);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
      ec.selected = nt.mapAPath(ec.selected, ntc, true);       
      support.addEdit(ntcc);

      TreeNode[] tn = ((DefaultMutableTreeNode)popupNode_).getPath();
      TreePath tp = new TreePath(tn);
      vmTree.setTreeSelectionPath(tp);

      //
      // Expand all the paths to keep the tree as it was.
      //

      Iterator<TreePath> plit = holdExpanded.iterator();
      while (plit.hasNext()) {
        tp = plit.next();
        if (nt.isPathPresent(tp)) {
          vmTree.expandTreePath(tp);
        }
      }
      
      //
      // FIX ME!  This is makeshift.  Node will still be visible, but pops
      // to the bottom of the view.  Put without it, we scroll all the way
      // to the top.
      //
      
      vmTree.scrollTreePathToVisible(new TreePath(tn));

      //
      // Collect up the expansion state after the change and add it to the undo
      // set so that redos will restore tree expansion correctly. Also needs mapping
      // to make it consistent with nav change tree representation.
      //

      ec = treeSupp_.buildExpansionChange(false, dacx_);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
      ec.selected = nt.mapAPath(ec.selected, ntc, false);      
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec));

      support.finish();
   
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
  }       
}
