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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.GenomeInstanceCopyDialog;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle copying a genome instance model
*/

public class CopyGenomeInstance extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CopyGenomeInstance() {
    name = "treePopup.CopyInstance";
    desc = "treePopup.CopyInstance";     
    mnem = "treePopup.CopyInstanceMnem";
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
    } else if (key.modType == XPlatModelNode.ModelType.DB_GENOME) {
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
        if (ans.getNextStep().equals("copyGenomeInstance")) {
          next = ans.copyGenomeInstance();      
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
        
    private TreeNode popupNode_;
    private XPlatModelNode.NodeKey key_;
    private NavTree.NavNode popupNavNode_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "copyGenomeInstance";
    }
     
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreload(Genome popupModel, Genome popupModelAncestor, TreeNode popupNode) {
      popupNode_ = popupNode;
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      key_ = nt.treeNodeToXPlatKey((DefaultMutableTreeNode)popupNode_, dacx_);
      popupNavNode_ = new NavTree.NavNode(nt.getNodeIDObj(popupNode_), key_, nt.getNodeName(popupNode_), nt.getNodeIDObj(popupNode_.getParent()), dacx_);
      return;
    }
 
    /***************************************************************************
    **
    ** Copy genome instance
    */  
       
    private DialogAndInProcessCmd copyGenomeInstance() { 
   
      XPlatModelNode.ModelType modType = key_.getModType();
      
      if ((modType == XPlatModelNode.ModelType.SUPER_ROOT) || (modType == XPlatModelNode.ModelType.DB_GENOME)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      dacx_.getRMan().getString("instructWarning.message"), 
                                      dacx_.getRMan().getString("instructWarning.title"),
                                        JOptionPane.WARNING_MESSAGE);
      }
 
     
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();  
      String origName = nt.getNodeName(popupNode_);  
      boolean hasKids = (popupNode_.getChildCount() != 0);
      
      GenomeInstanceCopyDialog gicd = new GenomeInstanceCopyDialog(uics_, origName, hasKids);   
      gicd.setVisible(true);
      if (!gicd.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      String newName = gicd.getName();
      boolean doRecursive = gicd.isRecursive();
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.copyGenomeToSibling", dacx_);
      copyGenomeInstanceToNewSibling(newName, doRecursive, support);
      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      support.finish();
    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
         
    /***************************************************************************
    **
    ** Guts of copying genome instance
    */
    
    private void copyGenomeInstanceToNewSibling(String newName, boolean doRecursive, UndoSupport support) {
   
      VirtualModelTree vmTree = uics_.getTree();
      
      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //
  
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      ExpansionChange ec = (new TreeSupport(uics_)).buildExpansionChange(true, nt);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(ec)); 
      int sibIndex = nt.getNewSiblingIndex(popupNode_);
          
      //
      // Figure out the set of instances to copy:
      //
      
      UiUtil.fixMePrintout("Seeing non-recursive copy placing copied vfa BELOW itself.");
      HashMap<NavTree.NavNode, List<NavTree.NavNode>> children = new HashMap<NavTree.NavNode, List<NavTree.NavNode>>();
         
      children.put(popupNavNode_, new ArrayList<NavTree.NavNode>());
      if (doRecursive) {
        List<NavTree.NavNode> pnl = nt.getPreorderNodeListing(popupNode_);
        Iterator<NavTree.NavNode> oit = pnl.iterator();    
        while (oit.hasNext()) {
          NavTree.NavNode navNode = oit.next();
          if (navNode.equals(popupNavNode_)) {
            continue;
          }
          NavTree.NodeID ntnid = navNode.getParent();
          NavTree.NavNode parNode = nt.navNodeForNodeID(ntnid, dacx_);
          List<NavTree.NavNode> kids = children.get(parNode);
          if (kids == null) {
            kids = new ArrayList<NavTree.NavNode>();
            children.put(parNode, kids);
          }
          kids.add(navNode);
        }
      }
  
      //
      // Do the copying. First set up ID maps.
      //
      
      NavTree.NavNode parNode = nt.navNodeForNodeID(nt.getNodeIDObj(popupNode_.getParent()), dacx_);      
      ArrayList<NavTreeChange> navTreeChanges = new ArrayList<NavTreeChange>();
      HashMap<String, IDMaps> mapsPerRootInstance = new HashMap<String, IDMaps>();
         
      recursiveSingleNodeCopy(parNode, popupNavNode_, children, nt, ec, true, navTreeChanges,  mapsPerRootInstance, new Integer(sibIndex), newName, support);
      
      //
      // Tell the world our node structure has changed, make sure we select the
      // parent of the new submodel and expand the path to it.
      //      
  
      vmTree.getTreeModel().nodeStructureChanged(popupNode_);
      TreeNode[] tn = ((DefaultMutableTreeNode)popupNode_).getPath();
      TreePath tp = new TreePath(tn);      
      vmTree.setTreeSelectionPath(tp);
  
      //
      // Expand all the paths to keep the tree as it was.
      //
  
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
  
      NavTreeChange ntc = navTreeChanges.get(navTreeChanges.size() - 1);
      ec = (new TreeSupport(uics_)).buildExpansionChange(false, nt);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
      ec.selected = nt.mapAPath(ec.selected, ntc, false);      
      support.addEdit(new ExpansionChangeCmd(ec)); 
          
      //
      // The new models need to have region maps duplicated. Before group nodes appeared, the user
      // was either duplicating one top level instance, or models below it. With group nodes, we 
      // now might be duplicating multiple top-level instances.
      //
      
      GenomeSource src = dacx_.getGenomeSource();
      Iterator<String> riit = mapsPerRootInstance.keySet().iterator();
      while (riit.hasNext()) {
        String gid = riit.next();
        postRootInstanceMapOperations(mapsPerRootInstance.get(gid), (GenomeInstance)src.getGenome(gid), support);
      }
     
      //
      // Layouts need to have note properties either augmented or replaced to handle new
      // note IDs.  Augmented if we are copying below the root, which doubles the number
      // of notes we need to create.  Need to do the same to overlays
      //
      
      riit = mapsPerRootInstance.keySet().iterator();
      while (riit.hasNext()) {
        String gid = riit.next();
        postLayoutOperations(mapsPerRootInstance.get(gid), (GenomeInstance)src.getGenome(gid), newName, support);
      }
      return;     
    }  
    
    /***************************************************************************
    **
    ** With possible duplications of multiple root instances now that we have group nodes,
    ** stuff that was done only once needs to be done for each copied instance.
    ** 
    */
    
    private void postRootInstanceMapOperations(IDMaps maps, GenomeInstance rootInstance, UndoSupport support) {
    
      //
      // The new models need to have region maps duplicated. Before group nodes appeared, the user
      // was either duplicating one top level instance, or models below it. With group nodes, we 
      // now might be duplicating multiple top-level instances.
      //
      
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, rootInstance.getID());
      
      Iterator<Group> grit = rootInstance.getGroupIterator();
      while (grit.hasNext()) {
        Group origGroup = grit.next();
        String oldID = origGroup.getID();
        String newID = maps.groupIDMap.get(oldID);
        if (newID == null) {
          newID = oldID;
        }
        TemporalInputChange tic = tird.copyTemporalRangeGroupMapForDuplicateGroup(oldID, newID, maps.modelIDMap);
        if (tic != null) {
          support.addEdit(new TemporalInputChangeCmd(rcx, tic));      
        }
        TimeCourseChange tcc = tcdm.copyTimeCourseGroupMapForDuplicateGroup(oldID, newID, maps.modelIDMap);
        if (tcc != null) {
          support.addEdit(new TimeCourseChangeCmd(tcc));      
        }            
      }
      return;
    }
    
    /***************************************************************************
    **
    ** With possible duplications of multiple root instances now that we have group nodes,
    ** stuff that was done only once needs to be done for each copied instance.
    ** 
    */
    
    private void postLayoutOperations(IDMaps maps, GenomeInstance rootInstance, String newName, UndoSupport support) {
      //
      // Layouts need to have note properties either augmented or replaced to handle new
      // note IDs.  Augmented if we are copying below the root, which doubles the number
      // of notes we need to create.  Need to do the same to overlays
      //
      ;
      boolean doAdd = !maps.topModelIsRoot;
      String loTarg = maps.modelIDMap.get(rootInstance.getID());
          
      Iterator<Layout> loit = dacx_.getLayoutSource().getLayoutIterator();
      while (loit.hasNext()) {
        Layout lo = loit.next();
        if (lo.getTarget().equals(loTarg)) {
          StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, loTarg);
          Layout.PropChange[] changes = lo.mapNoteProperties(maps.noteIDMap, doAdd);
          if (changes.length > 0) {
            support.addEdit(new PropChangeCmd(rcx, changes));
          }

          changes = lo.mapOverlayProperties(maps.ovrIDMap, maps.modIDMap, maps.modLinkIDMap, doAdd, rcx);
          if (changes.length > 0) {
            support.addEdit(new PropChangeCmd(rcx, changes));
          }        
          new DataLocator(uics_.getGenomePresentation(), rcx).setTitleLocation(support, loTarg, newName);        
        }
      }   
      return;
    }
 
    /***************************************************************************
    **
    ** Support for genome instance copy
    */
    
    private String recursiveGenomeInstanceCopy(NavTree.NavNode targetParent, NavTree.NavNode nodeToDup,
                                               Map<NavTree.NavNode, List<NavTree.NavNode>> children, 
                                               NavTree nt, ExpansionChange ec, 
                                               boolean isFirst, List<NavTreeChange> navTreeChanges, 
                                               Map<String, IDMaps> mapsPerRootInstance, 
                                               Integer sibIndex, String nameReplace, UndoSupport support) { 
      
      
      NavTree.Kids nodeType = nodeToDup.getType();
      if ((nodeType != NavTree.Kids.ROOT_INSTANCE) && (nodeType != NavTree.Kids.STATIC_CHILD_INSTANCE)) {
        throw new IllegalArgumentException();
      }
      
      String currToCopy = nodeToDup.getModelID();  
      GenomeInstance oldGI = (GenomeInstance)dacx_.getGenomeSource().getGenome(currToCopy);
      String oldParentID = nt.getGenomeModelAncestorID(nt.nodeForNodeIDObj(nodeToDup.getNodeID()).getParent());
      if (oldParentID == null) {
        throw new IllegalStateException();
      }
      GenomeInstance oldRootGI = oldGI.getVfgParentRoot();
      oldRootGI = (oldRootGI == null) ? oldGI : oldRootGI;
     
      String newParentID = nt.getGenomeModelAncestorID(nt.nodeForNodeIDObj(targetParent.getNodeID()));
      if (newParentID == null) {
        throw new IllegalStateException();
      }
      Genome newParGen = dacx_.getGenomeSource().getGenome(newParentID);
      GenomeInstance newParGI = (newParGen instanceof GenomeInstance) ? (GenomeInstance)newParGen : null;
      
      IDMaps maps;
      
      //
      // Three possible cases:
      // 1) Root instance is being copied, and this is the root instance
      // 2) Root instance is being copied, but this is below the root instance
      // 3) Root instance is not being copied, and this is below the root instance
      //
      int casenum;
      String oldRootGIID = oldRootGI.getID();
      if (oldRootGIID.equals(currToCopy)) {
        casenum = 1;
      } else {
        maps = mapsPerRootInstance.get(oldRootGIID);
        if (maps != null) {
          if (maps.topModelIsRoot) {
            casenum = 2;
          } else {
            casenum = 3;
          }
        } else {
          casenum = 3;
        }
      }
      
      switch (casenum) {
        case 1:
          if (newParGI != null) {
            throw new IllegalStateException();
          }
          newParentID = null;
          maps = new IDMaps();
          maps.topModelIsRoot = true;
          mapsPerRootInstance.put(currToCopy, maps);
          break;
        case 2:
          maps = mapsPerRootInstance.get(oldRootGIID);
          if (maps == null) {
            throw new IllegalStateException();
          }

          break;  
        case 3:
          maps = new IDMaps();
          maps.topModelIsRoot = false;
          mapsPerRootInstance.put(oldParentID, maps);      
          break;
        default:
          throw new IllegalStateException();
      }      
      
      String nextKey = dacx_.getGenomeSource().getNextKey();
      ArrayList<ImageChange> imageChanges = new ArrayList<ImageChange>();
      String newName = (nameReplace == null) ? oldGI.getName() : nameReplace;
      GenomeInstance gi = new GenomeInstance(oldGI, newName, newParentID, 
                                             nextKey, maps.groupIDMap,  maps.noteIDMap, 
                                             maps.ovrIDMap,  maps.modIDMap, maps.modLinkIDMap, imageChanges, uics_.getImageMgr());
        
      maps.modelIDMap.put(currToCopy, nextKey);
      DatabaseChange dc = dacx_.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), gi.getID(), ModelChangeEvent.MODEL_ADDED));       
      if (newParentID == null) { // Root instance needs a new layout
        ArrayList<Layout> newLayouts = new ArrayList<Layout>();
        Iterator<Layout> loit = dacx_.getLayoutSource().getLayoutIterator();
        while (loit.hasNext()) {
          Layout lo = loit.next();
          if (lo.getTarget().equals(currToCopy)) {
            String nextloKey = dacx_.getGenomeSource().getNextKey();
            // Notes in layout will be fixed by top caller using note ID map!
            // Same with overlays and modules!
            Layout nlo = new Layout(lo, nextloKey, nextKey, maps.groupIDMap);
            newLayouts.add(nlo);
          }
        }
        int numNew = newLayouts.size();
        for (int i = 0; i < numNew; i++) {
          Layout lo = newLayouts.get(i);
          dc = dacx_.getLayoutSource().addLayout(lo.getID(), lo);
          support.addEdit(new DatabaseChangeCmd(dacx_, dc));        
        }
      }
      
      //
      // Gotta happen after genome is added, so undo occurs in correct order:
      //
      int numIC = imageChanges.size();
      for (int i = 0; i < numIC; i++) {
        ImageChange ic = imageChanges.get(i);
        if (ic != null) {
          support.addEdit(new ImageChangeCmd(dacx_, ic));
        }
      }    
      
      NavTree.Kids kidType = (newParentID == null) ? NavTree.Kids.ROOT_INSTANCE : NavTree.Kids.STATIC_CHILD_INSTANCE;
      
      TreeNode parNode = nt.nodeForNodeIDObj(targetParent.getNodeID());
      NavTree.NodeAndChanges nac = nt.addNode(kidType, newName, parNode, new NavTree.ModelID(gi.getID()), null, sibIndex, uics_.getRMan()); 
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
      navTreeChanges.add(nac.ntc);
      
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      
      if (isFirst) {
        ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, true);
        ec.selected = nt.mapAPath(ec.selected, nac.ntc, true);
        isFirst = false;
      }
     
      NavTree.NavNode newTarg = nt.navNodeForNodeID(nt.getNodeIDObj(nac.node), dacx_);
      recursiveKidsCopy(newTarg, nodeToDup, children, nt, ec, navTreeChanges,  mapsPerRootInstance, support);
      
      return (nextKey);
    }
    
    /***************************************************************************
    **
    ** Support for dynamic proxy duplication
    */
    
    private void recursiveDynamicProxyCopy(NavTree.NavNode targetParent, NavTree.NavNode nodeToDup,
                                           Map<NavTree.NavNode, List<NavTree.NavNode>> children, 
                                           NavTree nt, ExpansionChange ec, 
                                           boolean isFirst, List<NavTreeChange> navTreeChanges, 
                                           Map<String, IDMaps> mapsPerRootInstance,
                                           Integer sibIndex, String nameReplace, UndoSupport support) { 
      
      NavTree.Kids nodeType = nodeToDup.getType();
      if ((nodeType != NavTree.Kids.DYNAMIC_SUM_INSTANCE) && (nodeType != NavTree.Kids.DYNAMIC_SLIDER_INSTANCE)) {
        throw new IllegalArgumentException();
      }
     
      String currToCopy = nodeToDup.getProxyID();
      DynamicInstanceProxy dipToCopy = dacx_.getGenomeSource().getDynamicProxy(currToCopy);
      GenomeInstance oldLowestStatic = dipToCopy.getStaticVfgParent();
      GenomeInstance oldRootInstance = oldLowestStatic.getVfgParentRoot();
      oldRootInstance = (oldRootInstance == null) ? oldLowestStatic : oldRootInstance;
      
      IDMaps maps = mapsPerRootInstance.get(oldRootInstance.getID());
      if (maps == null) {
        maps = new IDMaps();
        maps.topModelIsRoot = false;
        mapsPerRootInstance.put(oldRootInstance.getID(), maps);
      }  
       
      String parentModelID;
      NavTree.Kids targParType = targetParent.getType();     
      switch (targParType) {
        case ROOT_INSTANCE: 
        case STATIC_CHILD_INSTANCE:
          parentModelID = targetParent.getModelID();
          break;
        case DYNAMIC_SUM_INSTANCE:
          parentModelID = dacx_.getGenomeSource().getDynamicProxy(targetParent.getProxyID()).getAnInstance().getID();
          break;
        case GROUP_NODE:
          parentModelID = nt.getGenomeModelAncestorID(nt.nodeForNodeIDObj(targetParent.getNodeID()));
          break;
        case HIDDEN_ROOT:
        case ROOT_MODEL: 
        case DYNAMIC_SLIDER_INSTANCE:
        default:
          throw new IllegalStateException();
      }
      
      String nextKey = dacx_.getGenomeSource().getNextKey();
      ArrayList<ImageChange> imageChanges = new ArrayList<ImageChange>();
      
      String newName = (nameReplace == null) ? dipToCopy.getName() : nameReplace;
      DynamicInstanceProxy dipCopy = 
         new DynamicInstanceProxy(dipToCopy, newName, parentModelID, nextKey, maps.groupIDMap, maps.noteIDMap, 
                                  maps.ovrIDMap, maps.modIDMap, maps.modLinkIDMap, imageChanges, uics_.getImageMgr());
  
  
      maps.modelIDMap.put(currToCopy, nextKey);
      DatabaseChange dc = dacx_.getGenomeSource().addDynamicProxyExistingLabel(nextKey, dipCopy);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc)); 
      // Can't do this here; need to have note locations defined first:
      //new DataLocator(cView_.getSUPanel()).setTitleLocation(support, newVfgParentID, newName);
      
      //
      // This has to happen after the proxy is added!
      //
      
      int numIC = imageChanges.size();
      for (int i = 0; i < numIC; i++) {
        ImageChange ic = imageChanges.get(i);
        if (ic != null) {
          ic.proxyKey = nextKey;
          support.addEdit(new ImageChangeCmd(dacx_, ic));
        }
      }    
      
      NavTree.NodeAndChanges nac;
      TreeNode parNode = nt.nodeForNodeIDObj(targetParent.getNodeID());    
      if (nodeType == NavTree.Kids.DYNAMIC_SUM_INSTANCE) {
        List<String> newNodes = dipCopy.getProxiedKeys();
        if (newNodes.size() != 1) {
          throw new IllegalStateException();
        }
        String key = newNodes.iterator().next();
        nac = nt.addNode(NavTree.Kids.DYNAMIC_SUM_INSTANCE, dipCopy.getProxiedInstanceName(key), parNode,new NavTree.ModelID(key), null, sibIndex, uics_.getRMan());
      } else {
        nac = nt.addNode(NavTree.Kids.DYNAMIC_SLIDER_INSTANCE, dipCopy.getName(), parNode, null, dipCopy.getID(), sibIndex, uics_.getRMan());
      }
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
      navTreeChanges.add(nac.ntc);
         
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      
      if (isFirst) {
        ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, true);
        ec.selected = nt.mapAPath(ec.selected, nac.ntc, true);
        isFirst = false;
      }
   
      NavTree.NavNode newTarg = nt.navNodeForNodeID(nt.getNodeIDObj(nac.node), dacx_);      
      recursiveKidsCopy(newTarg, nodeToDup, children, nt, ec, navTreeChanges,  mapsPerRootInstance, support);
      return;
    }      

    /***************************************************************************
    **
    ** Support for group node copy
    */
    
    private void recursiveGroupNodeCopy(NavTree.NavNode targetParent, NavTree.NavNode nodeToDup,
                                        Map<NavTree.NavNode, List<NavTree.NavNode>> children, 
                                        NavTree nt, ExpansionChange ec, 
                                        boolean isFirst, List<NavTreeChange> navTreeChanges, 
                                        Map<String, IDMaps> mapsPerRootInstance, 
                                        Integer sibIndex, String nameReplace, UndoSupport support) { 
      
      
      NavTree.Kids nodeType = nodeToDup.getType();
      if (nodeType != NavTree.Kids.GROUP_NODE) {
        throw new IllegalArgumentException();
      }
      TreeNode parNode = nt.nodeForNodeIDObj(targetParent.getNodeID());
      // Images are not copied here:
      String newName = (nameReplace == null) ? nodeToDup.getName() : nameReplace;
      
      TreeNode groupNodeToCopy = nt.resolveNavNode(nodeToDup);
      NavTree.NodeAndChanges nac = nt.copyGroupNode(newName, groupNodeToCopy, parNode, sibIndex);
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
      navTreeChanges.add(nac.ntc);
      
      UiUtil.fixMePrintout("Gotta copy map entries for group node: MTGNNNONOtesMoreSubPreDupPoDupOK.btp");
            
      int numIC = nac.icsl.size();
      for (int i = 0; i < numIC; i++) {
        ImageChange ic = nac.icsl.get(i);
        if (ic != null) {
          support.addEdit(new ImageChangeCmd(dacx_, ic));
        }
      }    
     
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      
      if (isFirst) {
        ec.expanded = nt.mapAllPaths(ec.expanded, nac.ntc, true);
        ec.selected = nt.mapAPath(ec.selected, nac.ntc, true);
        isFirst = false;
      }
    
      NavTree.NavNode newTarg = nt.navNodeForNodeID(nt.getNodeIDObj(nac.node), dacx_);    
      recursiveKidsCopy(newTarg, nodeToDup, children, nt, ec, navTreeChanges,  mapsPerRootInstance, support);
 
      return;
    }
    
    /***************************************************************************
    **
    ** Apply correct recursion function to all child nodes:
    */
    
    private void recursiveKidsCopy(NavTree.NavNode targetParent, NavTree.NavNode navNode, Map<NavTree.NavNode, List<NavTree.NavNode>> children, 
                                   NavTree nt, ExpansionChange ec, 
                                   List<NavTreeChange> navTreeChanges, 
                                   Map<String, IDMaps> mapsPerRootInstance,
                                   UndoSupport support) { 

 
      List<NavTree.NavNode> kidsToCopy = children.get(navNode);
      if (kidsToCopy != null) {
        int numKtC = kidsToCopy.size();
        for (int i = 0; i < numKtC; i++) {
          NavTree.NavNode kidNode = kidsToCopy.get(i);
          recursiveSingleNodeCopy(targetParent, kidNode, children, nt, ec, false, navTreeChanges, mapsPerRootInstance, null, null, support);
        }
      } 
      return;
    }
    
    /***************************************************************************
    **
    ** Apply correct recursion function to a node:
    */
    
    private void recursiveSingleNodeCopy(NavTree.NavNode targetParent, NavTree.NavNode nodeToDup, 
                                         Map<NavTree.NavNode, List<NavTree.NavNode>> children, 
                                         NavTree nt, ExpansionChange ec, 
                                         boolean isFirst, List<NavTreeChange> navTreeChanges, 
                                         Map<String, IDMaps> mapsPerRootInstance,
                                         Integer sibIndex, String nameReplace, UndoSupport support) { 

      switch (nodeToDup.getType()) {
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:
          recursiveGenomeInstanceCopy(targetParent, nodeToDup, children, nt, ec, isFirst, 
                                      navTreeChanges, mapsPerRootInstance, sibIndex, nameReplace, support);   
          break;
        case DYNAMIC_SUM_INSTANCE: // Can be under a static model
        case DYNAMIC_SLIDER_INSTANCE:
          recursiveDynamicProxyCopy(targetParent, nodeToDup, children, nt, ec, isFirst, 
                                    navTreeChanges, mapsPerRootInstance, sibIndex, nameReplace, support);
          
          break;
        case GROUP_NODE:
          recursiveGroupNodeCopy(targetParent, nodeToDup, children, nt, ec, isFirst, 
                                 navTreeChanges, mapsPerRootInstance, sibIndex, nameReplace, support);  
          break;
        case HIDDEN_ROOT:
        case ROOT_MODEL:  
        default:
          throw new IllegalStateException();
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Bundle up maps for cleaner signatures
  */
  
  
  private static class IDMaps {
    
    boolean topModelIsRoot;
    HashMap<String, String> groupIDMap;
    HashMap<String, String> modelIDMap;
    HashMap<String, String> noteIDMap;
    HashMap<String, String> ovrIDMap;
    HashMap<String, String> modIDMap;
    HashMap<String, String> modLinkIDMap;
     
    IDMaps() {
      topModelIsRoot = false;
      groupIDMap = new HashMap<String, String>();
      modelIDMap = new HashMap<String, String>();
      noteIDMap = new HashMap<String, String>();
      ovrIDMap = new HashMap<String, String>();
      modIDMap = new HashMap<String, String>();
      modLinkIDMap = new HashMap<String, String>();
    }
  }
  
  
}
