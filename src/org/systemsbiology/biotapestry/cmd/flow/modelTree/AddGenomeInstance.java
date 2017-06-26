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
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ExpansionChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.GenomeInstanceCreationDialogFactory;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding a new genome instance model
*/

public class AddGenomeInstance extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddGenomeInstance() {
    name =  "command.AddInstance";
    desc = "command.AddInstance";
    icon = "CreateSubmodel24.gif";
    mnem =  "command.AddInstanceMnem";
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
    } else if ((key.modType == XPlatModelNode.ModelType.DYNAMIC_INSTANCE) || 
               (key.modType == XPlatModelNode.ModelType.DYNAMIC_PROXY))  {
      return (false);
    } else if (key.modType == XPlatModelNode.ModelType.SUPER_ROOT) {
      return (false);
    } else if (key.modType == XPlatModelNode.ModelType.DB_GENOME) {
      return (true);     
    } else {
      NavTree nt = dacx.getGenomeSource().getModelHierarchy();
      TreeNode node = nt.resolveNode(key);
      return (nt.ancestorIsStatic(node));
    }
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
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new AddGenomeInstanceState(dacx));  
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
        AddGenomeInstanceState ans = new AddGenomeInstanceState(cfh);      
        next = ans.stepGetCreationDialog();
      } else {
        AddGenomeInstanceState ans = (AddGenomeInstanceState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepGetCreationDialog")) {
          next = ans.stepGetCreationDialog();
        } else if (ans.getNextStep().equals("stepExtractNewInstanceInfo")) {
          next = ans.stepExtractNewInstanceInfo(last);
        } else if (ans.getNextStep().equals("addNewGenomeInstance")) {   
          next = ans.addNewGenomeInstance();     
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
        
  public static class AddGenomeInstanceState extends AbstractStepState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private String newName;
    private boolean timeBounded; 
    private int minTime;
    private int maxTime;
    private Genome parent;
    private boolean isPopup;   
    private TreeSupport treeSupp_;
    private Genome popupModelAncestor_;
    private TreeNode popupNode_;
 
    /***************************************************************************
    **
    ** Constructor
    */
      
    public AddGenomeInstanceState(StaticDataAccessContext dacx) {
      super(dacx);
      parent = null;
      isPopup = false;
      nextStep_ = "stepGetCreationDialog";
      treeSupp_ = null; // new TreeSupport(uics_); NO uics_ yet...
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
      
    public AddGenomeInstanceState(ServerControlFlowHarness cfh) {
      super(cfh);
      parent = null;
      isPopup = false;
      nextStep_ = "stepGetCreationDialog";
      treeSupp_ = new TreeSupport(uics_);
    }
    
    /***************************************************************************
    **
    ** Add cfh in if StepState was pre-built
    */
     
    @Override
    public void stockCfhIfNeeded(ServerControlFlowHarness cfh) {
      if (cfh_ != null) {
        return;
      }
      super.stockCfhIfNeeded(cfh);
      treeSupp_ = new TreeSupport(uics_);
      return;
    }

    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupModelAncestor_ = popupModelAncestor; // NEVER null
      popupNode_ = popupNode;
      parent = popupModelAncestor;
      isPopup = true;
      return;
    }    
    
    /***************************************************************************
    **
    ** Get dialog to create new genome instance
    */ 
      
    private DialogAndInProcessCmd stepGetCreationDialog() { 

      if (!isPopup) {
        UiUtil.fixMePrintout("NO this is wrong");
        parent = dacx_.getCurrentGenome();
      }
      
      //
      // Figure out if this is time bounded
      //
      
      boolean timeBounded = false;
      boolean suggested = false;
      int minTime = -1;
      int maxTime = -1;
      if (parent instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)parent;
        if (gi.hasTimeBounds()) {
          timeBounded = true;
          minTime = gi.getMinTime();
          maxTime = gi.getMaxTime();
        }
      } else { // root genome 
        suggested = true;
        TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
        if ((tcd != null) && tcd.haveDataEntries()) {
          timeBounded = true;            
          minTime = tcd.getMinimumTime(); 
          maxTime = tcd.getMaximumTime(); 
        }
      }

      GenomeInstanceCreationDialogFactory.GenomeInstanceBuildArgs ba = 
        new GenomeInstanceCreationDialogFactory.GenomeInstanceBuildArgs(dacx_.getGenomeSource().getUniqueModelName(dacx_.getRMan()), 
                                                                        timeBounded, minTime, maxTime, suggested);
      GenomeInstanceCreationDialogFactory nocdf = new GenomeInstanceCreationDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractNewInstanceInfo";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract new genome instance info from the creation dialog
    */ 
       
    private DialogAndInProcessCmd stepExtractNewInstanceInfo(DialogAndInProcessCmd cmd) {
         
      GenomeInstanceCreationDialogFactory.GenomeInstanceCreationRequest crq = 
        (GenomeInstanceCreationDialogFactory.GenomeInstanceCreationRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      } 
      
      newName = crq.newName;
      timeBounded = crq.timeBounded; 
      minTime = crq.minTime;
      maxTime = crq.maxTime;
      
      nextStep_ = "addNewGenomeInstance";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    } 

    /***************************************************************************
    **
    ** Add a new genome instance
    */  
       
    private DialogAndInProcessCmd addNewGenomeInstance() { 
          
      //
      // Undo/Redo support
      //

      UndoSupport support = uFac_.provideUndoSupport("undo.addGenomeInstance", dacx_);
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH); // We stay in the same place, so don't issue tree change events...
 
      VirtualModelTree vmTree = uics_.getTree();

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //
 
      ExpansionChange ec = treeSupp_.buildExpansionChange(true, nt);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(ec));      

      //
      // Make the change
      //

      GenomeInstance vfg;
      if (isPopup) {
        if ((popupModelAncestor_ == null) || (parent != popupModelAncestor_)) {
          throw new IllegalStateException();
        }
        vfg = (popupModelAncestor_ instanceof GenomeInstance) ? (GenomeInstance)popupModelAncestor_ : null;
      } else {
        // This is where BT-10-27-09:6 crashed, because creating a model from dialogs changes the model,
        // and selection target is holding onto the stale model reference.  We switched to using ID tag
        // for the fix.
        if (parent != dacx_.getGenomeSource().getGenome(vmTree.getSelectionTargetID())) {
          System.err.println("handle stale reference!");
          throw new IllegalStateException();
        }
        vfg = (parent instanceof GenomeInstance) ? (GenomeInstance)parent : null;
        popupNode_ = vmTree.getSelectionNode();
      }
      String parID = (vfg == null) ? null : vfg.getID();
      String nextKey = dacx_.getGenomeSource().getNextKey();
      GenomeInstance gi = new GenomeInstance(dacx_, newName, nextKey, parID, 
                                             timeBounded, minTime, maxTime);
      DatabaseChange dc = dacx_.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc)); 
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), nextKey, ModelChangeEvent.MODEL_ADDED));       
      if (vfg == null) {
        String nextloKey = dacx_.getGenomeSource().getNextKey();
        Layout lo = new Layout(nextloKey, nextKey);
        dc = dacx_.getLayoutSource().addLayout(nextloKey, lo);
        support.addEdit(new DatabaseChangeCmd(dacx_, dc));        
      }

      //
      // We are adding the new node.  This causes the selection path to be dropped,
      // which means that unless we let people who want to know that they should
      // ignore the selection change, we will be cycling thru null genome and
      // back again, losing things like current selections and overlay settings!
      //
       
      vmTree.setIgnoreSelection(true);
      NavTree.Kids kidType = (parID == null) ? NavTree.Kids.ROOT_INSTANCE : NavTree.Kids.STATIC_CHILD_INSTANCE;
      NavTree.NodeAndChanges nac = nt.addNode(kidType, newName, popupNode_, new NavTree.ModelID(nextKey), null, null, dacx_);
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

      ec = treeSupp_.buildExpansionChange(false, nt);
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
