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
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
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
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.GenomeInstanceCreationDialogFactory;
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
  
  public AddGenomeInstance(BTState appState) {
    super(appState);
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
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext dacx) {
    if (!appState_.getIsEditor() || (key == null)) {
      return (false);
    } else if ((key.modType == XPlatModelNode.ModelType.DYNAMIC_INSTANCE) || 
               (key.modType == XPlatModelNode.ModelType.DYNAMIC_PROXY))  {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
    
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.isNotDynamicInstance());
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new AddGenomeInstanceState(appState_, dacx));  
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
        AddGenomeInstanceState ans = new AddGenomeInstanceState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepGetCreationDialog();
      } else {
        AddGenomeInstanceState ans = (AddGenomeInstanceState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
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
        
  public static class AddGenomeInstanceState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private String newName;
    private boolean timeBounded; 
    private int minTime;
    private int maxTime;
    private Genome parent;
    private boolean isPopup;
    private ServerControlFlowHarness cfh;
    private String nextStep_;     
    private BTState appState_;
    private TreeSupport treeSupp_;
    private Genome popupTarget_;
    private TreeNode popupNode_;
    private DataAccessContext dacx_;
 
    /***************************************************************************
    **
    ** Constructor
    */
      
    public AddGenomeInstanceState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      parent = null;
      isPopup = false;
      nextStep_ = "stepGetCreationDialog";
      treeSupp_ = new TreeSupport(appState_);
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
    ** for preload
    */ 
        
    public void setPreload(Genome popupTarget, TreeNode popupNode) {
      popupTarget_ = popupTarget;
      parent = popupTarget_;
      popupNode_ = popupNode;
      isPopup = true;
      return;
    }
 
    /***************************************************************************
    **
    ** Get dialog to create new genome instance
    */ 
      
    private DialogAndInProcessCmd stepGetCreationDialog() { 

      if (!isPopup) {
        parent = dacx_.getGenome();
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
        if ((tcd != null) && tcd.haveData()) {
          timeBounded = true;            
          minTime = tcd.getMinimumTime(); 
          maxTime = tcd.getMaximumTime(); 
        }
      }

      GenomeInstanceCreationDialogFactory.GenomeInstanceBuildArgs ba = 
        new GenomeInstanceCreationDialogFactory.GenomeInstanceBuildArgs(dacx_.getGenomeSource().getUniqueModelName(), 
                                                                        timeBounded, minTime, maxTime, suggested);
      GenomeInstanceCreationDialogFactory nocdf = new GenomeInstanceCreationDialogFactory(cfh);
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
    ** Add a new overlay.
    */  
       
    private DialogAndInProcessCmd addNewGenomeInstance() { 
          
      //
      // Undo/Redo support
      //

      UndoSupport support = new UndoSupport(appState_, "undo.addGenomeInstance");
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.setSkipFlag(NavTree.SKIP_FINISH); // We stay in the same place, so don't issue tree change events...
 
      VirtualModelTree vmTree = appState_.getTree();

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //
 
      ExpansionChange ec = treeSupp_.buildExpansionChange(true, dacx_);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec));      

      //
      // Make the change
      //

      GenomeInstance vfg;
      if (isPopup) {
        if ((popupTarget_ == null) || (parent != popupTarget_)) {
          throw new IllegalStateException();
        }
        vfg = (popupTarget_ instanceof GenomeInstance) ? (GenomeInstance)popupTarget_ : null;
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
      String nextKey = dacx_.getGenomeSource().getNextKey();
      GenomeInstance gi = new GenomeInstance(appState_, newName, nextKey, (vfg == null) ? null : vfg.getID(), 
                                             timeBounded, minTime, maxTime);
      DatabaseChange dc = dacx_.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc)); 
      support.addEvent(new ModelChangeEvent(nextKey, ModelChangeEvent.MODEL_ADDED));       
      if (vfg == null) {
        String nextloKey = dacx_.getGenomeSource().getNextKey();
        Layout lo = new Layout(appState_, nextloKey, nextKey);
        dc = dacx_.lSrc.addLayout(nextloKey, lo);
        support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));        
      }

      //
      // We are adding the new node.  This causes the selection path to be dropped,
      // which means that unless we let people who want to know that they should
      // ignore the selection change, we will be cycling thru null genome and
      // back again, losing things like current selections and overlay settings!
      //
       
      vmTree.setIgnoreSelection(true);
      NavTreeChange ntc = nt.addNode(newName, (vfg == null) ? null : vfg.getID(), nextKey);
      support.addEdit(new NavTreeChangeCmd(appState_, dacx_, ntc));
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
      ec.selected = nt.mapAPath(ec.selected, ntc, true);

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

      ec = treeSupp_.buildExpansionChange(false, dacx_);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
      ec.selected = nt.mapAPath(ec.selected, ntc, false);      
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec));      

      support.finish();
      nt.setSkipFlag(NavTree.NO_FLAG);

      //
      // DONE!
      //
       
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }     
  }
}
