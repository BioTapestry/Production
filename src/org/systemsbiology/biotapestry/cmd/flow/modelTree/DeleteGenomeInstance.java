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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathControllerChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.UserTreePathChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle deleting a genome instance model
*/

public class DeleteGenomeInstance extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean kidsOnly_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DeleteGenomeInstance(BTState appState, boolean kidsOnly) {
    super(appState);
    name = (kidsOnly) ? "treePopup.DeleteChildInstances" : "treePopup.DeleteInstance";
    desc = (kidsOnly) ? "treePopup.DeleteChildInstances" : "treePopup.DeleteInstance";
    mnem = (kidsOnly) ? "treePopup.DeleteChildInstancesMnem" : "treePopup.DeleteInstanceMnem";
    kidsOnly_ = kidsOnly;
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
    if (kidsOnly_) {
      NavTree nt = dacx.getGenomeSource().getModelHierarchy();
      TreeNode node = nt.resolveNode(key, dacx);
      return ((node != null) ? (node.getChildCount() > 0) : false);
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
     StepState retval = new StepState(appState_, kidsOnly_, dacx);
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
        if (ans.getNextStep().equals("deleteGenomeInstance")) {
          next = ans.deleteGenomeInstance();      
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
    private boolean myKidsOnly_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean kidsOnly, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "deleteGenomeInstance";
      treeSupp_ = new TreeSupport(appState_);
      myKidsOnly_ = kidsOnly;
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
    ** Delete genome instance
    */  
       
    private DialogAndInProcessCmd deleteGenomeInstance() { 
   
      VirtualModelTree vmTree = appState_.getTree(); 
      //
      // If we have the root, kill off everything.  Else, kill off all target and all
      // children.
      //
      
      GenomeInstance rootOne = 
        (popupTarget_ instanceof GenomeInstance) ? (GenomeInstance)popupTarget_ : null;

      if ((rootOne == null) && !myKidsOnly_) {
        // Don't allow root delete
        throw new IllegalStateException();
      }

      //
      // Undo/Redo support
      //
        
      UndoSupport support = new UndoSupport(appState_, (myKidsOnly_) ? "undo.deleteChildGenomeInstances" : "undo.deleteGenomeInstance");       

      //
      // Record the pre-change tree expansion state, and hold onto a copy of the expansion
      // info, since it will need to be mapped based on the nav tree undo info, and we
      // are going to use it to restore our tree after we are done.
      //

      UserTreePathController utpc = appState_.getPathController();
      NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
      nt.setSkipFlag(NavTree.SKIP_FINISH); // We handle selection change, so don't issue tree change events...     
      ExpansionChange ec = treeSupp_.buildExpansionChange(true, dacx_);
      List<TreePath> holdExpanded = ec.expanded;
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec));
      support.addEdit(new UserTreePathControllerChangeCmd(appState_, dacx_, utpc.recordStateForUndo(true)));
      
      //
      // Do actual deletion
      //
              
      //
      // If we are killing off all the kids of the root, there need to be multiple
      // calls, one for each root instance:
      //
      
      HashSet<String> deadOnes = new HashSet<String>();
      if (rootOne == null) {
        ArrayList<String> inList = new ArrayList<String>();
        Iterator<GenomeInstance> dii = dacx_.getGenomeSource().getInstanceIterator();
        while (dii.hasNext()) {
          inList.add(dii.next().getID());
        }
        Iterator<String> iit = inList.iterator();
        while (iit.hasNext()) {
          String giid = iit.next();
          if (deadOnes.contains(giid)) {
            continue;
          }
          GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(giid);
          if (gi.getVfgParent() == null) {
            deadOnes.addAll(DeleteGenomeInstance.deleteGenomeInstance(appState_, dacx_, gi.getID(), false, support));
          }
        }
      } else {
        deadOnes.addAll(DeleteGenomeInstance.deleteGenomeInstance(appState_, dacx_, rootOne.getID(), myKidsOnly_, support));
      }
      
      TreeNode parent = popupNode_.getParent(); // get it before we are pruned     
      NavTreeChange ntc;
      if (myKidsOnly_) {
        ntc = nt.deleteNodeChildren((DefaultMutableTreeNode)popupNode_, deadOnes);
      } else {
        ntc = nt.deleteNode((DefaultMutableTreeNode)popupNode_, deadOnes);
      }
      
      NavTreeChangeCmd ntcc = new NavTreeChangeCmd(appState_, dacx_, ntc);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
      ec.selected = nt.mapAPath(ec.selected, ntc, true);       
      support.addEdit(ntcc);

      // NavTree handles notification:
      //((DefaultTreeModel)PopupTree.this.getModel()).nodeStructureChanged(popupNode_);
      TreeNode pathBot = (myKidsOnly_) ? popupNode_ : parent;
      TreeNode[] tn = ((DefaultMutableTreeNode)pathBot).getPath();
      TreePath tp = new TreePath(tn);
      vmTree.setTreeSelectionPath(tp);

      //
      // Expand all the paths to keep the tree as it was.
      //

      vmTree.expandTreePath(tp);
      Iterator<TreePath> plit = holdExpanded.iterator();
      while (plit.hasNext()) {
        tp = plit.next();
        if (nt.isPathPresent(tp)) {
          vmTree.expandTreePath(tp);
        }
      }

      //
      // Collect up the expansion state after the change and add it to the undo
      // set so that redos will restore tree expansion correctly. Also needs mapping
      // to make it consistent with nav change tree representation.
      //

      ec = treeSupp_.buildExpansionChange(false, dacx_);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
      ec.selected = nt.mapAPath(ec.selected, ntc, false);
      support.addEdit(new UserTreePathControllerChangeCmd(appState_, dacx_, utpc.recordStateForUndo(false)));
      support.addEdit(new ExpansionChangeCmd(appState_, dacx_, ec));

    //  ModelChangeEvent mcev = new ModelChangeEvent(proxy_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE, true);
    //  evList.add(mcev);    
    //  mcev = new ModelChangeEvent(proxy_.getID(), ModelChangeEvent.PROPERTY_CHANGE, true);
    //  evList.add(mcev);

      support.finish();
      nt.setSkipFlag(NavTree.NO_FLAG);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); 
    }
  }
  
  
  /***************************************************************************
  **
  ** Handles deleting a Genome Instance
  */ 
    
  public static Set<String> deleteGenomeInstance(BTState appState, DataAccessContext dacx, String genomeRootKey, boolean justKids, UndoSupport support) {

    //
    // If we have the root, kill off everything.  Else, kill off all target and all
    // children.
    //
    
    StartupView sView = dacx.getGenomeSource().getStartupView();
    
    Genome genome = dacx.getGenomeSource().getGenome(genomeRootKey);
    //
    // If you want to kill off all the children of the DBGenome, this guys
    // needs to be called multiple times!
    //
    
    if (genome instanceof DBGenome) {
      // Don't allow root delete
      throw new IllegalStateException();
    }
    GenomeInstance rootOne = (GenomeInstance)genome;
    boolean rootOneIsRootInstance = (rootOne.getVfgParent() == null); 
    
    //
    // Crank through the database.  Look for all models that are children of
    // this model.  Plus, if the root dead GI is a vfg, we can kill off the
    // layouts for it too, and clear out the group setting manager at the same time.
    // We have to do these searches BEFORE we start killing stuff off, since dynamic
    // instances are using the underlying database to answer these questions.
    //

    HashSet<String> deadOnes = new HashSet<String>();
    Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (rootOne.isAncestor(gi)) {
        if (justKids && gi.getID().equals(genomeRootKey)) {
          continue;
        }
        deadOnes.add(gi.getID());
      }
    }

    HashSet<String> deadProxies = new HashSet<String>();
    HashSet<String> deadViz = new HashSet<String>();
    // look for actual target, if dynamic
    if (genome instanceof DynamicGenomeInstance) {
      String pid = ((DynamicGenomeInstance)genome).getProxyID();
      if (!justKids) {
        deadProxies.add(pid);
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(pid);
        deadViz.addAll(dip.getProxiedKeys());
      }
    }
    // look for dynamic target ancestors
    Iterator<DynamicInstanceProxy> pit = dacx.getGenomeSource().getDynamicProxyIterator();
    while (pit.hasNext()) {
      DynamicInstanceProxy dip = pit.next();
      if (rootOne.isAncestor(dip.getVfgParent())) {
        deadProxies.add(dip.getID());
        deadViz.addAll(dip.getProxiedKeys());
      }
    }
    
    //
    // Unless the instance being ditched is a top-level instance, we need to
    // kill off note and overlay properties from the layout for the child
    // instances:
    //
    
    if (!rootOneIsRootInstance || justKids) {
      Iterator<String> dit = deadOnes.iterator();
      while (dit.hasNext()) {
        String id = dit.next();
        Layout glo = dacx.lSrc.getLayoutForGenomeKey(id);
        GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(id);
        Iterator<Note> noteIt = gi.getNoteIterator();
        Iterator<NetworkOverlay> overIt = gi.getNetworkOverlayIterator();
        DataAccessContext dacx4l = new DataAccessContext(dacx, gi, glo);
        cleanUpLayouts(appState, dacx4l, noteIt, overIt, glo, support);
      }
      Iterator<String> dpit = deadProxies.iterator();
      while (dpit.hasNext()) {
        String pid = dpit.next();
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(pid);
        GenomeInstance gi = dip.getStaticVfgParent();
        if (!gi.isRootInstance()) {
          gi = gi.getVfgParentRoot();
        }
        Layout glo = dacx.lSrc.getLayoutForGenomeKey(gi.getID());
        Iterator<Note> noteIt = dip.getNoteIterator();
        Iterator<NetworkOverlay> overIt = dip.getNetworkOverlayIterator();
        DataAccessContext dacx4l = new DataAccessContext(dacx, gi, glo);
        cleanUpLayouts(appState, dacx4l, noteIt, overIt, glo, support);
      }     
    }
   
    //
    // Handle image references and path stops for genomes:
    //
    
    UserTreePathController utpc = appState.getPathController();
    Iterator<String> doit = deadOnes.iterator();
    while (doit.hasNext()) {
      String id = doit.next();
      GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(id);
      DataAccessContext dacx4g = new DataAccessContext(dacx, gi);
      ImageChange ic = gi.dropGenomeImage();
      if (ic != null) {        
        support.addEdit(new ImageChangeCmd(appState, dacx4g, ic));
      }
      UserTreePathChange[] chgs = utpc.dropStopsOnModel(id);
      for (int i = 0; i < chgs.length; i++) {
        UserTreePathChangeCmd cmd = new UserTreePathChangeCmd(appState, dacx4g, chgs[i]);
        support.addEdit(cmd);
      }
      if ((sView != null) && id.equals(sView.getModel())) {
        DatabaseChange dc = dacx4g.getGenomeSource().setStartupView(new StartupView());
        support.addEdit(new DatabaseChangeCmd(appState, dacx4g, dc));
        sView = null;
      }
    }
    
    Iterator<String> dpit = deadProxies.iterator();
    while (dpit.hasNext()) {
      String id = dpit.next();
      DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(id);
      ImageChange[] ich = dip.dropGenomeImages();
      if (ich != null) {
        int numIC = ich.length;
        for (int i = 0; i < numIC; i++) {
          ImageChange ic = ich[i];
          if (ic != null) {
            support.addEdit(new ImageChangeCmd(appState, dacx, ic));
          }
        }
      }
    }     
 
    //
    // Kill off instances:
    //

    GroupSettingManager gsm = (GroupSettingManager)dacx.gsm;
    doit = deadOnes.iterator();
    while (doit.hasNext()) {
      String id = doit.next();
      GroupSettingChange gsc = gsm.dropGroupVisibilities(id);
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState, dacx, gsc);
        support.addEdit(gscc);
      }
      // FIX ME?? Remove all group IDs from root model too??
      DatabaseChange[] dc = dacx.getGenomeSource().removeInstance(id);
      for (int i = 0; i < dc.length; i++) {
        DatabaseChangeCmd dcc = new DatabaseChangeCmd(appState, dacx, dc[i]);
        support.addEdit(dcc);
      }
    
      if (dacx.getInstructSrc().getInstanceInstructionSet(id) != null) {
        DatabaseChange riis = dacx.getInstructSrc().removeInstanceInstructionSet(id);
        DatabaseChangeCmd riiscc = new DatabaseChangeCmd(appState, dacx, riis);
        support.addEdit(riiscc);
      }
 
      support.addEvent(new ModelChangeEvent(id, ModelChangeEvent.MODEL_DROPPED));
    }

    //
    // Kill off any group viz for dynamic instances, as well as path stops:
    //

    Iterator<String> dvit = deadViz.iterator();
    while (dvit.hasNext()) {
      String id = dvit.next();
      GroupSettingChange gsc = gsm.dropGroupVisibilities(id);
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState, dacx, gsc);
        support.addEdit(gscc);
      }
      UserTreePathChange[] chgs = utpc.dropStopsOnModel(id);
      for (int i = 0; i < chgs.length; i++) {
        UserTreePathChangeCmd cmd = new UserTreePathChangeCmd(appState, dacx, chgs[i]);
        support.addEdit(cmd);
      }
      if ((sView != null) && id.equals(sView.getModel())) {
        DatabaseChange dc = dacx.getGenomeSource().setStartupView(new StartupView());
        support.addEdit(new DatabaseChangeCmd(appState, dacx, dc));
        sView = null;
      }
    }      

    //
    // Kill off dead proxies too:
    //

    dpit = deadProxies.iterator();
    while (dpit.hasNext()) {
      String id = dpit.next();
      DatabaseChange[] dc = dacx.getGenomeSource().removeDynamicProxy(id);
      for (int i = 0; i < dc.length; i++) {
        DatabaseChangeCmd dcc = new DatabaseChangeCmd(appState, dacx, dc[i]);
        support.addEdit(dcc);
      }
    }      

    //
    // Now delete layout, but only if it is a root instance:
    //
    
    if (rootOneIsRootInstance && !justKids) {
      HashSet<String> deadLayouts = new HashSet<String>();
      String deadOneID = rootOne.getID();
      Iterator<Layout> loit = dacx.lSrc.getLayoutIterator();
      while (loit.hasNext()) {
        Layout lo = loit.next();
        if (lo.haveModelMetadataDependency(deadOneID)) {
          Layout.PropChange lpc = lo.dropModelMetadataDependency(deadOneID);
          if (lpc != null) {
            PropChangeCmd pcc = new PropChangeCmd(appState, dacx, lpc);
            support.addEdit(pcc);        
          }
        }
        if (lo.getTarget().equals(deadOneID)) {
          // Obsolete: Only one layout per root instance!
          deadLayouts.add(lo.getID());
        }
      }      
      if (deadLayouts.size() != 1) {
        throw new IllegalStateException();
      }
      String lon = deadLayouts.iterator().next();
      DatabaseChange dc = dacx.lSrc.removeLayout(lon);
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(appState, dacx, dc);
      support.addEdit(dcc);          
    }
    
    //
    // Now tell the underlying data sets that any maps referencing this
    // model need to be dropped too:
    //

    TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
    if (tcd != null) {
      dpit = deadProxies.iterator();
      while (dpit.hasNext()) {
        String id = dpit.next();
        TimeCourseChange[] tcc = tcd.dropGroupMapsForProxy(id);
        support.addEdits(TimeCourseChangeCmd.wrapChanges(appState, dacx, tcc));
      }
    }
    TemporalInputRangeData tird = dacx.getExpDataSrc().getTemporalInputRangeData();
    if (tird != null) {
      dpit = deadProxies.iterator();
      while (dpit.hasNext()) {
        String id = dpit.next();
        TemporalInputChange[] tic = tird.dropGroupMapsForProxy(id);
        support.addEdits(TemporalInputChangeCmd.wrapChanges(appState, dacx, tic));
      }                
    }
    
    return (deadOnes);
  }
  
  /***************************************************************************
  **
  ** Handles cleaning up layout items for submodels
  */ 
     
  private static void cleanUpLayouts(BTState appState, DataAccessContext dacx, Iterator<Note> noteIt, Iterator<NetworkOverlay> overIt, Layout glo, UndoSupport support) {     
    while (noteIt.hasNext()) {
      Note note = noteIt.next();
      Layout.PropChange[] lpc = new Layout.PropChange[1];    
      lpc[0] = glo.removeNoteProperties(note.getID());
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(appState, dacx, lpc);
        support.addEdit(mov);        
      }
    }
    while (overIt.hasNext()) {
      NetworkOverlay ovr = overIt.next();
      Layout.PropChange[] lpc = new Layout.PropChange[1];    
      lpc[0] = glo.removeNetOverlayProperties(ovr.getID());
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(appState, dacx, lpc);
        support.addEdit(mov);        
      }
    }
    return;
  }
}
