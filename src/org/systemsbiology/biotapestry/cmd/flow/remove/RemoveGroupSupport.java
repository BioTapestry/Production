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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMembership;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of commands to delete things
*/

public class RemoveGroupSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor.  Do not use.  This thing is now stateless, so make
  ** everything static for the time being during command refactoring.
  */ 
  
  private RemoveGroupSupport() {
  }

  /***************************************************************************
  **
  ** Delete a group from the model
  */  
  
  public static boolean deleteGroupFromModel(String groupKey,UIComponentSource uics, 
                                             StaticDataAccessContext rcx, TabSource tSrc, 
                                             UndoSupport support, boolean keepEmptyMemOnly, UndoFactory uFac) {                                           
    //
    // * If the genome is not a genome instance, we have an illegal argument
    // * Need to delete all the nodes in the group, and all linkages to or
    //   from the group.
    // * If the group is included in a child model, it needs to be deleted from
    //   there too.
    // * If the group is deleted from the top instance, group properties need
    //   to be deleted.
    // * If the group is deleted from the top instance, the timecourse mappings
    //   need to be deleted.
    // * Send out model change and data changed events.

    //
    // Figure out if we are dealing with the root, a topInstance, or a subset.
    //
    
    boolean isRoot;
    boolean isDynamic;
    
    if (rcx.currentGenomeIsAnInstance()) {
      isRoot = false;
      isDynamic = rcx.currentGenomeIsADynamicInstance();
    } else {
      isRoot = true;
      isDynamic = false;
    }
    
    if (isRoot) {
      throw new IllegalArgumentException();
    }    
    
    Layout.PadNeedsForLayout padNeeds = rcx.getFGHO().getLocalNetModuleLinkPadNeeds(rcx.getCurrentGenomeID());
    
    
    //
    // Undo/Redo support
    //
    
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.groupDelete", rcx);
    }  

    List<NavTree.GroupNodeMapEntry> navMapEntries = new ArrayList<NavTree.GroupNodeMapEntry>();
    
    
    //
    // For dynamic instances, just throwing out the group (and any subgroups) is
    // enough (as well as for child models): we are done.
    //
    
    if (isDynamic) {
      deleteDynamicGroup(uics, rcx, tSrc, groupKey, support, navMapEntries);
    } else {
      deleteStaticGroup(uics, rcx, tSrc, groupKey, support, keepEmptyMemOnly, navMapEntries);
    }
    
    dropGroupNavMapRegionRefs(uics, rcx, tSrc, support, navMapEntries); 

    ModificationCommands.repairNetModuleLinkPadsLocally(padNeeds, rcx, false, support);
 
    if (localUndo) {support.finish();}
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Drop region refs from group map
  ** 
  */
  
  private static boolean dropGroupNavMapRegionRefs(UIComponentSource uics, StaticDataAccessContext rcx,
                                                   TabSource tSrc, UndoSupport support, 
                                                   List<NavTree.GroupNodeMapEntry> navMapEntries) {
       
    NavTree nt = rcx.getGenomeSource().getModelHierarchy();    
    List<NavTreeChange> ntcs = nt.dropGroupNodeModelReferences(navMapEntries);
    for (NavTreeChange ntc : ntcs) {
      NavTreeChangeCmd cmd = new NavTreeChangeCmd(rcx, ntc);
      support.addEdit(cmd);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Delete a sub group from the parent group
  */  
  
  public static boolean deleteSubGroupFromModel(UIComponentSource uics, String groupKey, 
                                                StaticDataAccessContext rcx, TabSource tSrc, 
                                                UndoSupport support, UndoFactory uFac) {                          

    //
    // HOW THIS WORKS WITH CHILD MODEL
    //
    // Principle of Least Damage:
    //
    // Parent subgroup is activated     --> deactivated, i.e. deleted
    // Child subgroup must be activated --> deactivated, i.e. deleted
    //
    // Parent subgroup is included --> deleted
    // Child subgroup is activated --> deleted
    //
    // Parent subgroup is included --> deleted
    // Child subgroup is included  --> deleted    
    
    //
    // The difference with deleting a regular group is that we do not
    // need to delete any nodes or links.
    //
    
    //
    // Figure out if we are dealing with the root, a topInstance, or a subset.
    //
    
    boolean isRoot;
    boolean isDynamic;
    GenomeInstance inst;
    
    if (rcx.currentGenomeIsAnInstance()) {
      inst = rcx.getCurrentGenomeAsInstance();
      isRoot = false;
      isDynamic = rcx.currentGenomeIsADynamicInstance();
    } else {
      inst = null;
      isRoot = true;
      isDynamic = false;
    }
    
    if (isRoot) {
      throw new IllegalArgumentException();
    }    
    
    //
    // Undo/Redo support
    //
    
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.groupDelete", rcx);
    }    
    
    List<NavTree.GroupNodeMapEntry> navMapEntries = new ArrayList<NavTree.GroupNodeMapEntry>();
    
    //
    // For dynamic instances, just throwing out the group (and any subgroups) is
    // enough (as well as for child models): we are done.
    //
    
    if (isDynamic) {
      deleteDynamicSubGroup(rcx, tSrc, groupKey, support, navMapEntries);
    } else {
      deleteStaticSubGroup(rcx, tSrc, groupKey, support, navMapEntries);
    }
    
    
    //
    // Have dynamic proxies make sure that we have legal extra node states
    //
    
    boolean needGeneral = false;
    Iterator<DynamicInstanceProxy> dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteIllegalExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(rcx, pca[i]));
        needGeneral = true;
      }
    }
   
    //
    // When deleting a subset from a static model, there could be dynamic models
    // below it with extra nodes referencing it.  Catch these cases.
    // (Fixes BT-02-09-09:1)
    
    if (!isDynamic) {
      dit = rcx.getGenomeSource().getDynamicProxyIterator();
      while (dit.hasNext()) {
        DynamicInstanceProxy dip = dit.next();
        if (dip.instanceIsAncestor(inst)) {
          ProxyChange[] pca = dip.deleteExtraNodesForGroup(Group.getBaseID(groupKey));       
          for (int i = 0; i < pca.length; i++) {
            support.addEdit(new ProxyChangeCmd(rcx, pca[i]));
            needGeneral = true;
          }
        }
      }
    }
    
    dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteOrphanedExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(rcx, pca[i]));
        needGeneral = true;
      }
    } 
    
    dropGroupNavMapRegionRefs(uics, rcx, tSrc, support, navMapEntries); 
    
    RemoveNode.proxyPostExtraNodeDeletionSupport(uics, rcx, support);
   
    if (needGeneral) {
      support.addEvent(new GeneralChangeEvent(tSrc.getCurrentTab(), GeneralChangeEvent.ChangeType.UNSPECIFIED_CHANGE));
    }
 
    if (localUndo) {support.finish();}
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Delete a dynamic group from the model
  */  
  
  private static void deleteDynamicGroup(UIComponentSource uics, StaticDataAccessContext rcx, 
                                         TabSource tSrc, String groupKey, UndoSupport support, 
                                         List<NavTree.GroupNodeMapEntry> navMapEntries) {  
       
    String proxID = ((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID();
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if ((proxID != dip.getID()) && dip.proxyIsAncestor(proxID)) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          StaticDataAccessContext rcxK = new StaticDataAccessContext(rcx, kid);
          deleteGroupDetails(uics, rcxK, tSrc, inherit, support, navMapEntries);
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }
        }
        // FIX ME??? We do not delete extra nodes for group, like we
        // do for static deletions.  Was this an oversight?
      }
    } 
    
    // Now delete from the instance:
    
    deleteGroupDetails(uics, rcx, tSrc, groupKey, support, navMapEntries);
    DynamicInstanceProxy dip = rcx.getGenomeSource().getDynamicProxy(proxID);
    NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers((DynamicGenomeInstance)rcx.getCurrentGenome());
    if (nmca != null) {
      for (int i = 0; i < nmca.length; i++) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, nmca[i]);
        support.addEdit(gcc);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a group from the model
  */  
  
  private static void deleteStaticGroup(UIComponentSource uics, StaticDataAccessContext rcx,
                                        TabSource tSrc, String groupKey, UndoSupport support, boolean keepEmptyMemOnly,
                                        List<NavTree.GroupNodeMapEntry> navMapEntries) {
                                  
    // * Need to delete all the nodes in the group, and all linkages to or
    //   from the group.
    // * If the group is included in a child model, it needs to be deleted from
    //   there too.
    // * If the group is deleted from the top instance, group properties need
    //   to be deleted.
    // * If the group is deleted from the top instance, the timecourse mappings
    //   need to be deleted.
    // * Send out model change and data changed events.
                                               
    //
    // Figure out the linkages that impinge on our group, and get rid of them:
    //

    HashSet<GenomeItemInstance> deadSet = new HashSet<GenomeItemInstance>();
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance li = (LinkageInstance)lit.next();
      GroupMembership memb = rcx.getCurrentGenomeAsInstance().getLinkGroupMembership(li); 
      if (memb.contains(groupKey)) {
        deadSet.add(li);
      }
    }
    Iterator<GenomeItemInstance> dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      LinkageInstance li = (LinkageInstance)dsit.next();
      RemoveLinkage.doLinkDelete(li.getID(), rcx, support);  // FIX ME!! Eventing!!
    }

    //
    // Do the same with genes and nodes:
    //
    
    deadSet.clear();
    Iterator<Node> nit = rcx.getCurrentGenome().getNodeIterator();
    while (nit.hasNext()) {
      NodeInstance ni = (NodeInstance)nit.next();
      GroupMembership memb = rcx.getCurrentGenomeAsInstance().getNodeGroupMembership(ni);
      if (memb.contains(groupKey)) {
        deadSet.add(ni); 
      }
    }
    
    Iterator<Gene> git = rcx.getCurrentGenome().getGeneIterator();
    while (git.hasNext()) {
      NodeInstance gi = (NodeInstance)git.next();
      GroupMembership memb = rcx.getCurrentGenomeAsInstance().getNodeGroupMembership(gi);
      if (memb.contains(groupKey)) {
        deadSet.add(gi); 
      }
    }
       
    dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      NodeInstance ni = (NodeInstance)dsit.next();
      RemoveNode.doNodeDelete(uics, tSrc, ni.getID(), rcx, support, null, keepEmptyMemOnly); 
    }
    
    //
    // Grab this info now so we can delete the properties later:
    
    Set<String> subsets = rcx.getCurrentGenomeAsInstance().getGroup(groupKey).getSubsets(rcx.getCurrentGenomeAsInstance());
    
    //
    // All groups in child models should now be empty after the above operations,
    // so now just delete the empty groups!
    //


    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    boolean needGeneral = false;
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rcx.getCurrentGenomeAsInstance())) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        StaticDataAccessContext rcxK = new StaticDataAccessContext(rcx, kid);
        if (kid.getGroup(inherit) != null) {        
          deleteGroupDetails(uics, rcxK, tSrc, inherit, support, navMapEntries);
        }
        ProxyChange[] pca = dip.deleteExtraNodesForGroup(Group.getBaseID(groupKey));       
        for (int i = 0; i < pca.length; i++) {
          support.addEdit(new ProxyChangeCmd(rcxK, pca[i]));
          needGeneral = true;
        }
        NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
        if (nmca != null) {
          for (int i = 0; i < nmca.length; i++) {
            NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxK, nmca[i]);
            support.addEdit(gcc);
          }
        }       
      }
    }
    
    // Check for modules emptying out:
    
    RemoveNode.proxyPostExtraNodeDeletionSupport(uics, rcx, support);   
    
    if (needGeneral) {
      support.addEvent(new GeneralChangeEvent(tSrc.getCurrentTab(), GeneralChangeEvent.ChangeType.UNSPECIFIED_CHANGE));
    }
    
    Iterator<GenomeInstance> dit = rcx.getGenomeSource().getInstanceIterator();
    while (dit.hasNext()) {
      GenomeInstance gi = dit.next();
      if ((gi != rcx.getCurrentGenome()) && rcx.getCurrentGenomeAsInstance().isAncestor(gi)) {
        String inherit = Group.buildInheritedID(groupKey, gi.getGeneration());
        if (gi.getGroup(inherit) != null) {
          StaticDataAccessContext rcxI = new StaticDataAccessContext(rcx, gi);
          deleteGroupDetails(uics, rcxI, tSrc, inherit, support, navMapEntries);
        }
      }
    }
    
    // Now delete from the instance:
    
    deleteGroupDetails(uics, rcx, tSrc, groupKey, support, navMapEntries);
    
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
    
    Iterator<String> ssit = subsets.iterator();
    while (ssit.hasNext()) {
      String subkey = ssit.next();
      doTopInstanceGroupCleanup(rcx, subkey, tSrc, support);
    } 
    doTopInstanceGroupCleanup(rcx, groupKey, tSrc, support);
    return;
  }  

  /***************************************************************************
  **
  ** Delete a dynamic subgroup from the model
  */  
  
  private static void deleteDynamicSubGroup(StaticDataAccessContext rcx, TabSource tSrc, String subKey, 
                                            UndoSupport support, List<NavTree.GroupNodeMapEntry> navMapEntries) {
                                       
    String proxID = ((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID();
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if ((proxID != dip.getID()) && dip.proxyIsAncestor(proxID)) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(subKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          StaticDataAccessContext rcxK = new StaticDataAccessContext(rcx, kid);
          deleteSubGroupDetails(rcxK, tSrc, inherit, support, navMapEntries);          
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }
        }
      }
    } 
    
    // Now delete from the instance:

    DynamicInstanceProxy dip = rcx.getGenomeSource().getDynamicProxy(proxID);
    deleteSubGroupDetails(rcx, tSrc, subKey, support, navMapEntries); 
    NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers((DynamicGenomeInstance)rcx.getCurrentGenome());
    if (nmca != null) {
      for (int i = 0; i < nmca.length; i++) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, nmca[i]);
        support.addEdit(gcc);
      }
    }
 
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a subgroup from the model
  */  
  
  private static void deleteStaticSubGroup(StaticDataAccessContext rcx, TabSource tSrc, 
                                           String groupKey, UndoSupport support, 
                                           List<NavTree.GroupNodeMapEntry> navMapEntries) {
    //
    // Delete from dynamic children, then static children, then from parent instance:
    //
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rcx.getCurrentGenomeAsInstance())) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          StaticDataAccessContext rcxK = new StaticDataAccessContext(rcx, kid);
          deleteSubGroupDetails(rcxK, tSrc, inherit, support, navMapEntries);
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }         
        }
      }
    }
    Iterator<GenomeInstance> dit = rcx.getGenomeSource().getInstanceIterator();
    while (dit.hasNext()) {
      GenomeInstance gi = dit.next();
      if ((gi != rcx.getCurrentGenome()) && rcx.getCurrentGenomeAsInstance().isAncestor(gi)) {
        String inherit = Group.buildInheritedID(groupKey, gi.getGeneration());
        if (gi.getGroup(inherit) != null) {
          StaticDataAccessContext rcxI = new StaticDataAccessContext(rcx, gi);
          deleteSubGroupDetails(rcxI, tSrc, inherit, support, navMapEntries);
        }
      }
    }

    // Now delete from the instance:
    
    deleteSubGroupDetails(rcx, tSrc, groupKey, support, navMapEntries);
 
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
    
    doTopInstanceGroupCleanup(rcx, groupKey, tSrc, support);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a subgroup from the model
  */  
  
  private static void deleteSubGroupDetails(StaticDataAccessContext rcx, TabSource tSrc, String subKey, UndoSupport support,  
                                            List<NavTree.GroupNodeMapEntry> navMapEntries) {
    GenomeChange[] retval = rcx.getCurrentGenomeAsInstance().removeSubGroup(subKey);
    if (retval != null) {
      for (int i = 0; i < retval.length; i++) {
        if (retval[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(retval[i]);
          support.addEdit(gcc);
          support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        }              
      }
    }
       
    GroupSettingChange gsc = rcx.getGSM().dropGroupVisibility(rcx.getCurrentGenomeID(), subKey);  
    if (gsc != null) {
      GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(rcx, gsc);
      support.addEdit(gscc);
    }
    
    //
    // Any group node nav maps that need to know about the group will be informed. Note that
    // we have traditionally used a proxied dynamic instance hack for doing this work, so we need
    // to back that out here:
    //
    
    String cgid = rcx.getCurrentGenomeID();
    String proxID = null;
    if (DynamicInstanceProxy.isDynamicInstance(cgid)) {
      proxID = DynamicInstanceProxy.extractProxyID(cgid);
      cgid = null;
    }
    
    NavTree.GroupNodeMapEntry forDel = 
      new NavTree.GroupNodeMapEntry(null, tSrc.getCurrentTab(), cgid, proxID, null, subKey);
    navMapEntries.add(forDel);
    
    
    
    return;
  }

  /***************************************************************************
  **
  ** Delete a group from the model
  */  
  
  private static void deleteGroupDetails(UIComponentSource uics, DataAccessContext rcx, TabSource tSrc, 
                                         String groupKey, UndoSupport support, 
                                         List<NavTree.GroupNodeMapEntry> navMapEntries) {
    
    //
    // If there are any modules attached to the group, delete them too:
    //
    
    Map<String, Set<String>> moduleMap = rcx.getCurrentGenomeAsInstance().findModulesOwnedByGroup(groupKey);
    if (!moduleMap.isEmpty()) {
      NetOverlayController noc = uics.getNetOverlayController();
      Iterator<String> mmkit = moduleMap.keySet().iterator();
      while (mmkit.hasNext()) {
        String nokey = mmkit.next();
        Set<String> modSet = moduleMap.get(nokey);
        Iterator<String> msit = modSet.iterator();
        while (msit.hasNext()) {
          String modID = msit.next();
          OverlaySupport.deleteNetworkModule(uics, rcx, nokey, modID, support);
        }
      }
      noc.cleanUpDeletedModules(moduleMap, support, rcx);
    }
    
    //
    // Now remove the empty group
    //
    
    GenomeChange[] retval = rcx.getCurrentGenomeAsInstance().removeEmptyGroup(groupKey);
    if (retval != null) {
      for (int i = 0; i < retval.length; i++) {
        if (retval[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(retval[i]);
          support.addEdit(gcc);
          support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        }              
      }
    }
           
    GroupSettingChange gsc = rcx.getGSM().dropGroupVisibility(rcx.getCurrentGenomeID(), groupKey);  
    if (gsc != null) {
      GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(rcx, gsc);
      support.addEdit(gscc);
    }
    
    //
    // Any group node nav maps that need to know about the group will be informed. Note that
    // we have traditionally used a proxied dynamic instance hack for doing this work, so we need
    // to back that out here:
    //
    
    String cgid = rcx.getCurrentGenomeID();
    String proxID = null;
    if (DynamicInstanceProxy.isDynamicInstance(cgid)) {
      proxID = DynamicInstanceProxy.extractProxyID(cgid);
      cgid = null;
    }

    NavTree.GroupNodeMapEntry forDel = 
      new NavTree.GroupNodeMapEntry(null, tSrc.getCurrentTab(), cgid, proxID, null, groupKey);
    navMapEntries.add(forDel);
  
    return;
  }  

  /***************************************************************************
  **
  ** If we have the top instance, delete the group properties and the mappings!
  */  
  
  private static void doTopInstanceGroupCleanup(StaticDataAccessContext rcx, String key, TabSource tSrc, UndoSupport support) {   
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
                 
    GenomeInstance parent = rcx.getCurrentGenomeAsInstance().getVfgParent();
    if (parent != null) {
      return;
    }
            
    String topInstID = rcx.getCurrentGenomeID();
    Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
    while (layit.hasNext()) {
      Layout layout = layit.next();
      if (layout.haveGroupMetadataDependency(topInstID, key)) {
        Layout.PropChange lpc = layout.dropGroupMetadataDependency(topInstID, key);
        if (lpc != null) {
          PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
          support.addEdit(pcc);        
        }
      } 
      if (layout.getTarget().equals(topInstID)) {
        Layout.PropChange[] lpc = new Layout.PropChange[1];             
        lpc[0] = layout.removeGroupProperties(key);        
        if (lpc[0] != null) {
          PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
          support.addEdit(pcc);
          support.addEvent(new LayoutChangeEvent(layout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        }
      }
    }
    
    doTopInstanceGroupMapCleanup(rcx, key, tSrc, support);
    
    return;
  }
  
  /***************************************************************************
  **
  ** If we have the top instance, delete the mappings!
  */  
  
  public static void doTopInstanceGroupMapCleanup(StaticDataAccessContext rcx, String key, TabSource tSrc, UndoSupport support) {   
    //
    // Now if we have the top instance, delete the group mappings!
    //
                    
    GenomeInstance parent = rcx.getCurrentGenomeAsInstance().getVfgParent();
    if (parent != null) {
      return;
    }
    
    TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
    TimeCourseDataMaps tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
    if (tcd != null) {
      TimeCourseChange tchg = tcdm.dropGroupMap(key);
      if (tchg != null) {
        TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(tchg, false);
        support.addEdit(cmd);
        support.addEvent(new GeneralChangeEvent(tSrc.getCurrentTab(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      }
    }
    
    TemporalInputRangeData tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
    if (tird != null) {
      TemporalInputChange tichg = tird.dropGroupMap(key);
      if (tichg != null) {
        TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(rcx, tichg, false);
        support.addEdit(cmd);
        support.addEvent(new GeneralChangeEvent(tSrc.getCurrentTab(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      }
    }
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Following an instruction install, pitch any group-related data that is
  ** orphaned.
  */  
 
   public static void cleanupDanglingGroupData(StaticDataAccessContext rcx, UndoSupport support) {  
     //
     // Go through the time course region maps, temporal input region maps,
     // and region visibility settings, and delete any that are no longer needed.
     //
          
     //
     // Remember: Group IDs are globally unique
     //
     
     HashSet<String> liveGroups = new HashSet<String>();
     
     Iterator<GenomeInstance> giit = rcx.getGenomeSource().getInstanceIterator();
     while (giit.hasNext()) {
       GenomeInstance gi = giit.next();
       Iterator<Group> git = gi.getGroupIterator();
       while (git.hasNext()) {
         Group grp = git.next();
         liveGroups.add(Group.getBaseID(grp.getID()));
       }
     }
     
     //
     // Drop dangling time course group maps:
     //
     
     HashSet<String> deadGroups = new HashSet<String>();
     TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
     TimeCourseDataMaps tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
     if (tcd != null) {
       Iterator<String> tcgmkit = tcdm.getGroupMapKeys();
       while (tcgmkit.hasNext()) {
         String key = tcgmkit.next();  
         if (!liveGroups.contains(key)) {
           deadGroups.add(key);
         }
       }  
     }
     
     Iterator<String> dgit = deadGroups.iterator();
     while (dgit.hasNext()) {
       String dgKey = dgit.next();
       TimeCourseChange tcc = tcdm.dropGroupMap(dgKey);
       TimeCourseChangeCmd tccc = new TimeCourseChangeCmd(tcc);
       support.addEdit(tccc);
     }
     
     //
     // Drop dangling temporal input group maps:
     //
     
     deadGroups.clear();
     TemporalInputRangeData tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
     if (tird != null) {
       Iterator<String> kit = tird.getGroupMapKeys();
       while (kit.hasNext()) {
         String key = kit.next();  
         if (!liveGroups.contains(key)) {
           deadGroups.add(key);
         }
       }  
     }
     
     dgit = deadGroups.iterator();
     while (dgit.hasNext()) {
       String dgKey = dgit.next();
       TemporalInputChange tic = tird.dropGroupMap(dgKey);
       TemporalInputChangeCmd ticc = new TemporalInputChangeCmd(rcx, tic);
       support.addEdit(ticc);
     } 
     
     cleanupDanglingVisibility(rcx, support);     
  
     return;
   } 
   
  /***************************************************************************
  **
  ** Following an instruction install, pitch any group-related data that is
  ** orphaned.
  */  
 
   public static void cleanupDanglingVisibility(StaticDataAccessContext rcx, UndoSupport support) {  

     GroupSettingSource gsm = rcx.getGSM();
     
     HashSet<String> liveGroups = new HashSet<String>();     

     Iterator<GenomeInstance> giit = rcx.getGenomeSource().getInstanceIterator();
     while (giit.hasNext()) {
       GenomeInstance gi = giit.next();
       liveGroups.clear();
       Iterator<Group> git = gi.getGroupIterator();
       while (git.hasNext()) {
         Group grp = git.next();
         liveGroups.add(grp.getID());
       }
       gsm.dropOrphanedVisibilities(gi.getID(), rcx, liveGroups, support);
     }     
     
     Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
     while (dpit.hasNext()) {
       DynamicInstanceProxy dip = dpit.next();
       Iterator<Group> grit = dip.getGroupIterator();
       liveGroups.clear();
       while (grit.hasNext()) {
         Group grp = grit.next();
         liveGroups.add(grp.getID());
       }
       gsm.dropOrphanedVisibilitiesForProxy(dip.getID(), rcx, liveGroups, support);
     }     
 
     return;
   }    
}
