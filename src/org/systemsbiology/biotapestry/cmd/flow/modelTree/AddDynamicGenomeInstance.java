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

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
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
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.dialogs.DynamicInstanceCreationDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding a dynamic genome instance model
*/

public class AddDynamicGenomeInstance extends AbstractControlFlow {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddDynamicGenomeInstance() {
    name = "treePopup.AddDynamicInstance";
    desc = "treePopup.AddDynamicInstance";     
    mnem = "treePopup.AddDynamicInstanceMnem";
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
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext rcx, UIComponentSource uics) {
    if (!uics.getIsEditor() || (key == null)) {
      return (false);
    } else if (key.modType == XPlatModelNode.ModelType.DB_GENOME) {    
      return (false);  
    } else if (key.modType == XPlatModelNode.ModelType.DYNAMIC_INSTANCE) {  // i.e. not DYNAMIC_PROXY!!
      return (true);            
    } else if (key.modType == XPlatModelNode.ModelType.GENOME_INSTANCE) {
      TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
      return (tcd.haveDataEntries());
    } else if (key.modType == XPlatModelNode.ModelType.GROUPING_ONLY) {
      TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
      if (!tcd.haveDataEntries()) {
        return (false);
      }
      NavTree nt = rcx.getGenomeSource().getModelHierarchy();
      TreeNode node = nt.resolveNode(key);
      return ((nt.ancestorIsDynamicSum(node) || nt.ancestorIsStatic(node)) && !nt.ancestorIsRoot(node));
    } else {
      return (false);
    }
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
        if (ans.getNextStep().equals("addDynamicGenomeInstance")) {
          next = ans.addDynamicGenomeInstance();      
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
    
    private Genome popupModelAncestor_;
    private TreeNode popupNode_;

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "addDynamicGenomeInstance";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupModelAncestor_ = popupModelAncestor; // NEVER null
      popupNode_ = popupNode;
      return;
    }
    
    /***************************************************************************
    **
    ** add dynamic genome instance
    */  
       
    private DialogAndInProcessCmd addDynamicGenomeInstance() { 
   
      //
      // Gotta be an instance for dynamic submodels
      //
      GenomeInstance vfg = 
        (popupModelAncestor_ instanceof GenomeInstance) ? (GenomeInstance)popupModelAncestor_ : null;
      if (vfg == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }

      //
      // Figure out properties from time course data
      //

      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      int minHour = tcd.getMinimumTime(); // No longer used?  (Now must have times set in parent...)
      int maxHour = tcd.getMaximumTime(); // as above...
      boolean preferSum = true;
      if (popupModelAncestor_ instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)popupModelAncestor_;
        DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(dgi.getProxyID());      
        minHour = dip.getMinimumTime();
        maxHour = dip.getMaximumTime();
        preferSum = false;
      } else if (popupModelAncestor_ instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)popupModelAncestor_;
        if (gi.hasTimeBounds()) {
          minHour = gi.getMinTime();
          maxHour = gi.getMaxTime();
        } else {
          ResourceManager rMan = dacx_.getRMan(); 
          JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                        rMan.getString("treePopup.noParentTimesError"),
                                        rMan.getString("treePopup.noParentTimesErrorTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
      }

      //
      // Let user set properties, or cancel
      //

      DynamicInstanceCreationDialog dicd = 
        new DynamicInstanceCreationDialog(uics_, dacx_, minHour, maxHour, preferSum);
      dicd.setVisible(true);
      if (!dicd.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }

      //
      // Undo/Redo support
      //

      UndoSupport support = uFac_.provideUndoSupport("undo.addDynamicGenomeInstance", dacx_);

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //

      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH); // We stay in the same place, so don't issue tree change events...     
      
      ExpansionChange ec = (new TreeSupport(uics_)).buildExpansionChange(true, nt);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(ec));

      //
      // Create the proxy, add it to the database
      //

      String nextKey = dacx_.getGenomeSource().getNextKey();      
      DynamicInstanceProxy dip = 
        new DynamicInstanceProxy(dacx_, dicd.getModName(), nextKey, 
                                 vfg, !dicd.isPerTime(), dicd.getMinTime(), dicd.getMaxTime(), 
                                 dicd.getShowDiff(), dicd.getSimKey()); 
      DatabaseChange dc = dacx_.getGenomeSource().addDynamicProxyExistingLabel(nextKey, dip);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), nextKey, ModelChangeEvent.DYNAMIC_MODEL_ADDED));
      new DataLocator(uics_.getGenomePresentation(), dacx_).setTitleLocation(support, vfg.getID(), dicd.getName());

      //
      // Add the nodes to the nav tree.  Map the pre-change expansion state to match
      // the tree info held in the NavTreeChange, and back-fill the expansion change
      // already submitted to undo support.
      //

      NavTree.NodeAndChanges nac;
      if (dip.isSingle()) {
        List<String> newNodes = dip.getProxiedKeys();
        if (newNodes.size() != 1) {
          throw new IllegalStateException();
        }
        String key = newNodes.iterator().next();
        nac = nt.addNode(NavTree.Kids.DYNAMIC_SUM_INSTANCE, dip.getProxiedInstanceName(key), popupNode_, new NavTree.ModelID(key), null, null, uics_.getRMan());      
      } else {
        nac = nt.addNode(NavTree.Kids.DYNAMIC_SLIDER_INSTANCE, dip.getName(), popupNode_, null, dip.getID(), null, uics_.getRMan());
      }
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
      ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, true);
      ec.selected = nt.mapAPath(ec.selected, nac.ntc, true);  

      //
      // Tell the world our node structure has changed, make sure we select the
      // parent of the new submodel and expand the path to it.
      //

      VirtualModelTree vmTree = uics_.getTree();
      vmTree.getTreeModel().nodeStructureChanged(popupNode_);
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

      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();

      support.finish();
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
   
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
  }       
}
