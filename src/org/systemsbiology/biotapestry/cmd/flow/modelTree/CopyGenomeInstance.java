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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ExpansionChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
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
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.GenomeInstanceCopyDialog;
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
  
  public CopyGenomeInstance(BTState appState) {
    super(appState);    
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
  public boolean isTreeEnabled(XPlatModelNode.NodeKey key, DataAccessContext dacx) {
    if (!appState_.getIsEditor() || (key == null)) {
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
        
  public static class StepState implements DialogAndInProcessCmd.ModelTreeCmdState {
    
    private String nextStep_;    
    private BTState appState_;
    private TreeSupport treeSupp_;
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
      nextStep_ = "copyGenomeInstance";
      treeSupp_ = new TreeSupport(appState_);
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
    ** Copy genome instance
    */  
       
    private DialogAndInProcessCmd copyGenomeInstance() { 
   
      if (!(popupTarget_ instanceof GenomeInstance)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      dacx_.rMan.getString("instructWarning.message"), 
                                      dacx_.rMan.getString("instructWarning.title"),
                                        JOptionPane.WARNING_MESSAGE);
        }
 
        GenomeInstance gi = (GenomeInstance)popupTarget_;
        String origName = gi.getName();
        
        //
      // See if we are dealing with a dynamic proxy:
      //
      
      DynamicInstanceProxy popupDip = null;
      if (popupTarget_ instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)popupTarget_;
        popupDip = dacx_.getGenomeSource().getDynamicProxy(dgi.getProxyID());
        origName = popupDip.getName();
      }
      
      //
      // See if we have kids:
      //
      
      boolean hasKids = false;
      if (popupDip == null) {   // static checks        
        Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
        while (iit.hasNext()) {
          GenomeInstance testGi = iit.next();
          if (gi == testGi) {
            continue;
          }
          if (gi.isAncestor(testGi)) {
            hasKids = true;
            break;
          }
        }        
      }
      
      if (!hasKids) {  // still looking, go to dynamic checks  
        Iterator<DynamicInstanceProxy> dit = dacx_.getGenomeSource().getDynamicProxyIterator();
        while (dit.hasNext()) {
          DynamicInstanceProxy dip = dit.next();
          if (popupDip != null) { // copy base is dynamic
            if (popupDip == dip) {
              continue;
            }
            if (dip.proxyIsAncestor(popupDip.getID())) {
              hasKids = true;
              break;
            }
          } else {
            if (dip.instanceIsAncestor(gi)) {
              hasKids = true;
              break;
            }
          }
        }
      }
           
      GenomeInstanceCopyDialog gicd = new GenomeInstanceCopyDialog(appState_, origName, hasKids);             
      
      
      gicd.setVisible(true);
      if (!gicd.haveResult()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      String newName = gicd.getName();
      boolean doRecursive = gicd.isRecursive();
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState_, "undo.copyGenomeToSibling");
      copyGenomeInstanceToNewSibling(newName, doRecursive, popupDip, support);
      support.finish();
    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
         
    /***************************************************************************
    **
    ** Guts of copying genome instance
    */
    
    private void copyGenomeInstanceToNewSibling(String newName, boolean doRecursive, 
                                                DynamicInstanceProxy proxyBase, UndoSupport support) {
   
      VirtualModelTree vmTree = appState_.getTree();
      
      GenomeInstance vfg = (GenomeInstance)popupTarget_;
      
      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //
  
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      ExpansionChange ec = treeSupp_.buildExpansionChange(true, dacx_);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec)); 
      int sibIndex = nt.getNewSiblingIndex(popupNode_);
          
      //
      // Figure out the set of instances to copy:
      //
  
      HashMap<String, List<String>> children = new HashMap<String, List<String>>();
      children.put(vfg.getID(), new ArrayList<String>());
      if (doRecursive) {
        List<String> ordered = nt.getPreorderListing(popupNode_, dacx_);
        Iterator<String> oit = ordered.iterator();    
        while (oit.hasNext()) {
          String gkey = oit.next();
          GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(gkey);
          if (gi == vfg) {
            continue;
          }
          GenomeInstance currParent = gi.getVfgParent();
          String currParentID = currParent.getID();
          List<String> kids = children.get(currParentID);
          if (kids == null) {
            kids = new ArrayList<String>();
            children.put(currParentID, kids);
          }
          kids.add(gkey);
        }
      }
  
      //
      // Do the copying.
      //
      
      ArrayList<NavTreeChange> navTreeChanges = new ArrayList<NavTreeChange>();
      GenomeInstance parent = vfg.getVfgParent();
      HashMap<String, String> groupIDMap = new HashMap<String, String>();
      HashMap<String, String> modelIDMap = new HashMap<String, String>();
      HashMap<String, String> noteIDMap = new HashMap<String, String>();
      HashMap<String, String> ovrIDMap = new HashMap<String, String>();
      HashMap<String, String> modIDMap = new HashMap<String, String>();
      HashMap<String, String> modLinkIDMap = new HashMap<String, String>();
    
      String topCopy = null;
      if (proxyBase == null) {
        topCopy = recursiveGenomeInstanceCopy(vfg, parent, newName, children, nt, ec, true, 
                                              navTreeChanges, groupIDMap, modelIDMap, noteIDMap, 
                                              ovrIDMap, modIDMap, modLinkIDMap, new Integer(sibIndex), support);
      } else {
        recursiveDynamicProxyCopy(proxyBase, parent, newName, children, nt, ec, true, 
                                  navTreeChanges, groupIDMap, modelIDMap, noteIDMap, 
                                  ovrIDMap, modIDMap, modLinkIDMap, new Integer(sibIndex), support);
      }
         
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
      ec = treeSupp_.buildExpansionChange(false, dacx_);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
      ec.selected = nt.mapAPath(ec.selected, ntc, false);      
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec)); 
          
      //
      // The new models need to have group maps duplicated:
      //
      
      TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      
      boolean isRoot = vfg.isRootInstance();
      GenomeInstance rootInstance = (isRoot) ? vfg : vfg.getVfgParentRoot();
      Iterator<Group> grit = rootInstance.getGroupIterator();
      while (grit.hasNext()) {
        Group origGroup = grit.next();
        String oldID = origGroup.getID();
        String newID = groupIDMap.get(oldID);
        if (newID == null) {
          newID = oldID;
        }
        TemporalInputChange tic = tird.copyTemporalRangeGroupMapForDuplicateGroup(oldID, newID, modelIDMap);
        if (tic != null) {
          support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic));      
        }
        TimeCourseChange tcc = tcd.copyTimeCourseGroupMapForDuplicateGroup(oldID, newID, modelIDMap);
        if (tcc != null) {
          support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));      
        }            
      }
      
      //
      // Layouts need to have note properties either augmented or replaced to handle new
      // note IDs.  Augmented if we are copying below the root, which doubles the number
      // of notes we need to create.  Need to do the same to overlays
      //
      
      boolean doAdd = !isRoot;
      String loTarg = (isRoot) ? topCopy : rootInstance.getID();
          
      Iterator<Layout> loit = dacx_.lSrc.getLayoutIterator();
      while (loit.hasNext()) {
        Layout lo = loit.next();
        if (lo.getTarget().equals(loTarg)) {
          Layout.PropChange[] changes = lo.mapNoteProperties(noteIDMap, doAdd);
          if (changes.length > 0) {
            support.addEdit(new PropChangeCmd(appState_, dacx_, changes));
          }
          DataAccessContext rcx = new DataAccessContext(appState_, loTarg, lo);
          changes = lo.mapOverlayProperties(ovrIDMap, modIDMap, modLinkIDMap, doAdd, rcx);
          if (changes.length > 0) {
            support.addEdit(new PropChangeCmd(appState_, rcx, changes));
          }        
          new DataLocator(appState_, dacx_).setTitleLocation(support, vfg.getID(), newName);        
        }
      }    
     
      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
     
      return;     
    }  
    
    /***************************************************************************
    **
    ** Support for genome instance copy
    */
    
    private String recursiveGenomeInstanceCopy(GenomeInstance vfg, GenomeInstance newParent, 
                                               String newName,
                                               Map<String, List<String>> children, NavTree nt, ExpansionChange ec, 
                                               boolean isFirst, List<NavTreeChange> navTreeChanges, Map<String, String> groupIDMap, 
                                               Map<String, String> modelIDMap, Map<String, String> noteIDMap, 
                                               Map<String, String> ovrIDMap, Map<String, String> modIDMap, Map<String, String> modLinkIDMap,
                                               Integer sibIndex, UndoSupport support) {    
      String currToCopy = vfg.getID();
      String newParentID = (newParent == null) ? null : newParent.getID();
      String nextKey = dacx_.getGenomeSource().getNextKey();
      ArrayList<ImageChange> imageChanges = new ArrayList<ImageChange>();
      GenomeInstance gi = new GenomeInstance(vfg, newName, newParentID, 
                                             nextKey, groupIDMap, noteIDMap, 
                                             ovrIDMap, modIDMap, modLinkIDMap, imageChanges);
  
      
      modelIDMap.put(currToCopy, nextKey);
      DatabaseChange dc = dacx_.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));
      support.addEvent(new ModelChangeEvent(gi.getID(), ModelChangeEvent.MODEL_ADDED));       
      if (newParentID == null) { // Root instance needs a new layout
        ArrayList<Layout> newLayouts = new ArrayList<Layout>();
        Iterator<Layout> loit = dacx_.lSrc.getLayoutIterator();
        while (loit.hasNext()) {
          Layout lo = loit.next();
          if (lo.getTarget().equals(currToCopy)) {
            String nextloKey = dacx_.getGenomeSource().getNextKey();
            // Notes in layout will be fixed by top caller using note ID map!
            // Same with overlays and modules!
            Layout nlo = new Layout(lo, nextloKey, nextKey, groupIDMap);
            newLayouts.add(nlo);
          }
        }
        int numNew = newLayouts.size();
        for (int i = 0; i < numNew; i++) {
          Layout lo = newLayouts.get(i);
          dc = dacx_.lSrc.addLayout(lo.getID(), lo);
          support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));        
        }
      }
      
      //
      // Gotta happen after genome is added, so undo occurs in correct order:
      //
      int numIC = imageChanges.size();
      for (int i = 0; i < numIC; i++) {
        ImageChange ic = imageChanges.get(i);
        if (ic != null) {
          support.addEdit(new ImageChangeCmd(appState_, dacx_, ic));
        }
      }    
      
      NavTreeChange ntc;
      if (sibIndex == null) {
        ntc = nt.addNode(newName, newParentID, nextKey);
      } else {
        ntc = nt.addNodeAtIndex(newName, newParentID, nextKey, sibIndex.intValue());
      }
      support.addEdit(new NavTreeChangeCmd(appState_, dacx_, ntc));
      navTreeChanges.add(ntc);
      
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      
      if (isFirst) {
        ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
        ec.selected = nt.mapAPath(ec.selected, ntc, true);
        isFirst = false;
      }
     
      List<String> kidsToCopy = children.get(currToCopy);
      if (kidsToCopy != null) {
        int numKtC = kidsToCopy.size();
        for (int i = 0; i < numKtC; i++) {
          String kidID = kidsToCopy.get(i);
          if (DynamicInstanceProxy.isDynamicInstance(kidID)) {
            String proxKey = DynamicInstanceProxy.extractProxyID(kidID);
            DynamicInstanceProxy kidDP = dacx_.getGenomeSource().getDynamicProxy(proxKey);
            String newKidName = kidDP.getName(); // same as the old name below the top level
            recursiveDynamicProxyCopy(kidDP, gi, newKidName, children, nt, ec, isFirst, 
                                      navTreeChanges, groupIDMap, modelIDMap, noteIDMap, 
                                      ovrIDMap, modIDMap, modLinkIDMap, null, support);
          } else {
            GenomeInstance kidGI = (GenomeInstance)dacx_.getGenomeSource().getGenome(kidID);
            String newKidName = kidGI.getName(); // same as the old name below the top level
            recursiveGenomeInstanceCopy(kidGI, gi, newKidName, children, nt, ec, isFirst, 
                                        navTreeChanges, groupIDMap, modelIDMap, noteIDMap, 
                                        ovrIDMap, modIDMap, modLinkIDMap, null, support);    
          }
        }
      }
      return (nextKey);
    }
    
    
    /***************************************************************************
    **
    ** Support for dynamic proxy duplication
    */
    
    private void recursiveDynamicProxyCopy(DynamicInstanceProxy dip, GenomeInstance newParent, 
                                           String newName,
                                           Map<String, List<String>> children, NavTree nt, ExpansionChange ec, 
                                           boolean isFirst, List<NavTreeChange> navTreeChanges, Map<String, String> groupIDMap, 
                                           Map<String, String> modelIDMap, Map<String, String> noteIDMap, 
                                           Map<String, String> ovrIDMap, Map<String, 
                                           String> modIDMap, Map<String, String> modLinkIDMap,
                                           Integer sibIndex, UndoSupport support) { 
          
      String currToCopy = dip.getID();
      String newVfgParentID = newParent.getID();
      String nextKey = dacx_.getGenomeSource().getNextKey();
      ArrayList<ImageChange> imageChanges = new ArrayList<ImageChange>();
      
      DynamicInstanceProxy dipCopy = 
         new DynamicInstanceProxy(dip, newName, newVfgParentID, nextKey, groupIDMap, noteIDMap, 
                                  ovrIDMap, modIDMap, modLinkIDMap, imageChanges);
  
  
      modelIDMap.put(currToCopy, nextKey);
      DatabaseChange dc = dacx_.getGenomeSource().addDynamicProxyExistingLabel(nextKey, dipCopy);
      support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc)); 
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
          support.addEdit(new ImageChangeCmd(appState_, dacx_, ic));
        }
      }    
      
      NavTreeChange ntc;
      if (dipCopy.isSingle()) {
        List<String> newNodes = dipCopy.getProxiedKeys();
        if (newNodes.size() != 1) {
          throw new IllegalStateException();
        }
        String key = newNodes.iterator().next();
        if (sibIndex == null) {
          ntc = nt.addNode(dipCopy.getProxiedInstanceName(key), newVfgParentID, key);
        } else {
          ntc = nt.addNodeAtIndex(dipCopy.getProxiedInstanceName(key), newVfgParentID, key, sibIndex.intValue());
        }
      } else {
        if (sibIndex == null) {
          ntc = nt.addProxyNode(dipCopy.getName(), newVfgParentID, dipCopy.getID());    
        } else {
          ntc = nt.addProxyNodeAtIndex(dipCopy.getName(), newVfgParentID, dipCopy.getID(), sibIndex.intValue());
        }
      }
      support.addEdit(new NavTreeChangeCmd(appState_, dacx_, ntc));
      navTreeChanges.add(ntc);
         
      //
      // Note that these changes are now being made to the ExpansionChange that has already
      // been entered into the UndoSupport!
      //
      
      if (isFirst) {
        ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
        ec.selected = nt.mapAPath(ec.selected, ntc, true);
        isFirst = false;
      }
     
      List<String> kidsToCopy = children.get(dip.getFirstProxiedKey());
      if (kidsToCopy != null) {
        GenomeInstance par = dipCopy.getAnInstance();
        int numKtC = kidsToCopy.size();
        for (int i = 0; i < numKtC; i++) {
          String kidID = kidsToCopy.get(i);
          String proxKey = DynamicInstanceProxy.extractProxyID(kidID);
          DynamicInstanceProxy kidDIP = dacx_.getGenomeSource().getDynamicProxy(proxKey);
          String newKidName = kidDIP.getName(); // same as the old name below the top level
          recursiveDynamicProxyCopy(kidDIP, par, newKidName, children, nt, ec, isFirst, 
                                    navTreeChanges, groupIDMap, modelIDMap, noteIDMap, 
                                    ovrIDMap, modIDMap, modLinkIDMap, null, support);    
        }
      }
      return;
    }      
  }
}
