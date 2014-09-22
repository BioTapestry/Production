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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
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
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Layout;
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
  
  public static boolean deleteGroupFromModel(BTState appState, String groupKey, DataAccessContext rcx,
                                             UndoSupport support, boolean keepEmptyMemOnly) {                                           
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
    
    if (rcx.getGenome() instanceof GenomeInstance) {
      isRoot = false;
      isDynamic = (rcx.getGenome() instanceof DynamicGenomeInstance);
    } else {
      isRoot = true;
      isDynamic = false;
    }
    
    if (isRoot) {
      throw new IllegalArgumentException();
    }    
    
    Layout.PadNeedsForLayout padNeeds = rcx.fgho.getLocalNetModuleLinkPadNeeds(rcx.getGenomeID());
    
    
    //
    // Undo/Redo support
    //
    
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = new UndoSupport(appState, "undo.groupDelete");
    }  

    //
    // For dynamic instances, just throwing out the group (and any subgroups) is
    // enough (as well as for child models): we are done.
    //
    
    if (isDynamic) {
      deleteDynamicGroup(appState, rcx, groupKey, support);
    } else {
      deleteStaticGroup(appState, rcx, groupKey, support, keepEmptyMemOnly);
    }
     
    ModificationCommands.repairNetModuleLinkPadsLocally(appState, padNeeds, rcx, false, support);
 
    if (localUndo) {support.finish();}
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Delete a sub group from the parent group
  */  
  
  public static boolean deleteSubGroupFromModel(BTState appState, String groupKey, DataAccessContext rcx, UndoSupport support) {                          

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
    
    if (rcx.getGenome() instanceof GenomeInstance) {
      inst = rcx.getGenomeAsInstance();
      isRoot = false;
      isDynamic = (rcx.getGenome() instanceof DynamicGenomeInstance);
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
      support = new UndoSupport(appState, "undo.groupDelete");
    }    

    //
    // For dynamic instances, just throwing out the group (and any subgroups) is
    // enough (as well as for child models): we are done.
    //
    
    if (isDynamic) {
      deleteDynamicSubGroup(appState, rcx, groupKey, support);
    } else {
      deleteStaticSubGroup(appState, rcx, groupKey, support);
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
        support.addEdit(new ProxyChangeCmd(appState, rcx, pca[i]));
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
            support.addEdit(new ProxyChangeCmd(appState, rcx, pca[i]));
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
        support.addEdit(new ProxyChangeCmd(appState, rcx, pca[i]));
        needGeneral = true;
      }
    } 
    
    RemoveNode.proxyPostExtraNodeDeletionSupport(appState, rcx, support);
   
    if (needGeneral) {
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));
    }
 
    if (localUndo) {support.finish();}
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Delete a dynamic group from the model
  */  
  
  private static void deleteDynamicGroup(BTState appState, DataAccessContext rcx, String groupKey, UndoSupport support) {  
       
    String proxID = ((DynamicGenomeInstance)rcx.getGenome()).getProxyID();
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if ((proxID != dip.getID()) && dip.proxyIsAncestor(proxID)) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          DataAccessContext rcxK = new DataAccessContext(rcx, kid);
          deleteGroupDetails(appState, rcxK, inherit, support);
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }
        }
        // FIX ME??? We do not delete extra nodes for group, like we
        // do for static deletions.  Was this an oversight?
      }
    } 
    
    // Now delete from the instance:
    
    deleteGroupDetails(appState, rcx, groupKey, support);
    DynamicInstanceProxy dip = rcx.getGenomeSource().getDynamicProxy(proxID);
    NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers((DynamicGenomeInstance)rcx.getGenome());
    if (nmca != null) {
      for (int i = 0; i < nmca.length; i++) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcx, nmca[i]);
        support.addEdit(gcc);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a group from the model
  */  
  
  private static void deleteStaticGroup(BTState appState, DataAccessContext rcx,
                                        String groupKey, UndoSupport support, boolean keepEmptyMemOnly) {
                                  
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
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance li = (LinkageInstance)lit.next();
      GroupMembership memb = rcx.getGenomeAsInstance().getLinkGroupMembership(li); 
      if (memb.contains(groupKey)) {
        deadSet.add(li);
      }
    }
    Iterator<GenomeItemInstance> dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      LinkageInstance li = (LinkageInstance)dsit.next();
      RemoveLinkage.doLinkDelete(appState, li.getID(), rcx, support);  // FIX ME!! Eventing!!
    }

    //
    // Do the same with genes and nodes:
    //
    
    deadSet.clear();
    Iterator<Node> nit = rcx.getGenome().getNodeIterator();
    while (nit.hasNext()) {
      NodeInstance ni = (NodeInstance)nit.next();
      GroupMembership memb = rcx.getGenomeAsInstance().getNodeGroupMembership(ni);
      if (memb.contains(groupKey)) {
        deadSet.add(ni); 
      }
    }
    
    Iterator<Gene> git = rcx.getGenome().getGeneIterator();
    while (git.hasNext()) {
      NodeInstance gi = (NodeInstance)git.next();
      GroupMembership memb = rcx.getGenomeAsInstance().getNodeGroupMembership(gi);
      if (memb.contains(groupKey)) {
        deadSet.add(gi); 
      }
    }
       
    dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      NodeInstance ni = (NodeInstance)dsit.next();
      RemoveNode.doNodeDelete(appState, ni.getID(), rcx, support, null, keepEmptyMemOnly); 
    }
    
    //
    // Grab this info now so we can delete the properties later:
    
    Set<String> subsets = rcx.getGenomeAsInstance().getGroup(groupKey).getSubsets(rcx.getGenomeAsInstance());
    
    //
    // All groups in child models should now be empty after the above operations,
    // so now just delete the empty groups!
    //


    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    boolean needGeneral = false;
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rcx.getGenomeAsInstance())) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        DataAccessContext rcxK = new DataAccessContext(rcx, kid);
        if (kid.getGroup(inherit) != null) {        
          deleteGroupDetails(appState, rcxK, inherit, support);
        }
        ProxyChange[] pca = dip.deleteExtraNodesForGroup(Group.getBaseID(groupKey));       
        for (int i = 0; i < pca.length; i++) {
          support.addEdit(new ProxyChangeCmd(appState, rcxK, pca[i]));
          needGeneral = true;
        }
        NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
        if (nmca != null) {
          for (int i = 0; i < nmca.length; i++) {
            NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcxK, nmca[i]);
            support.addEdit(gcc);
          }
        }       
      }
    }
    
    // Check for modules emptying out:
    
    RemoveNode.proxyPostExtraNodeDeletionSupport(appState, rcx, support);   
    
    if (needGeneral) {
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));
    }
    
    Iterator<GenomeInstance> dit = rcx.getGenomeSource().getInstanceIterator();
    while (dit.hasNext()) {
      GenomeInstance gi = dit.next();
      if ((gi != rcx.getGenome()) && rcx.getGenomeAsInstance().isAncestor(gi)) {
        String inherit = Group.buildInheritedID(groupKey, gi.getGeneration());
        if (gi.getGroup(inherit) != null) {
          DataAccessContext rcxI = new DataAccessContext(rcx, gi);
          deleteGroupDetails(appState, rcxI, inherit, support);
        }
      }
    }
    
    // Now delete from the instance:
    
    deleteGroupDetails(appState, rcx, groupKey, support);
    
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
    
    Iterator<String> ssit = subsets.iterator();
    while (ssit.hasNext()) {
      String subkey = ssit.next();
      doTopInstanceGroupCleanup(appState, rcx, subkey, support);
    } 
    doTopInstanceGroupCleanup(appState, rcx, groupKey, support);
    return;
  }  

  /***************************************************************************
  **
  ** Delete a dynamic subgroup from the model
  */  
  
  private static void deleteDynamicSubGroup(BTState appState, DataAccessContext rcx,
                                            String subKey, UndoSupport support) {
                                       
    String proxID = ((DynamicGenomeInstance)rcx.getGenome()).getProxyID();
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if ((proxID != dip.getID()) && dip.proxyIsAncestor(proxID)) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(subKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          DataAccessContext rcxK = new DataAccessContext(rcx, kid);
          deleteSubGroupDetails(appState, rcxK, inherit, support);          
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }
        }
      }
    } 
    
    // Now delete from the instance:

    DynamicInstanceProxy dip = rcx.getGenomeSource().getDynamicProxy(proxID);
    deleteSubGroupDetails(appState, rcx, subKey, support); 
    NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers((DynamicGenomeInstance)rcx.getGenome());
    if (nmca != null) {
      for (int i = 0; i < nmca.length; i++) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcx, nmca[i]);
        support.addEdit(gcc);
      }
    }
 
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a subgroup from the model
  */  
  
  private static void deleteStaticSubGroup(BTState appState, DataAccessContext rcx, String groupKey, UndoSupport support) {
    //
    // Delete from dynamic children, then static children, then from parent instance:
    //
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rcx.getGenomeAsInstance())) {
        // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(groupKey, kid.getGeneration());
        if (kid.getGroup(inherit) != null) {
          DataAccessContext rcxK = new DataAccessContext(rcx, kid);
          deleteSubGroupDetails(appState, rcxK, inherit, support);
          NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
          if (nmca != null) {
            for (int i = 0; i < nmca.length; i++) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcxK, nmca[i]);
              support.addEdit(gcc);
            }
          }         
        }
      }
    }
    Iterator<GenomeInstance> dit = rcx.getGenomeSource().getInstanceIterator();
    while (dit.hasNext()) {
      GenomeInstance gi = dit.next();
      if ((gi != rcx.getGenome()) && rcx.getGenomeAsInstance().isAncestor(gi)) {
        String inherit = Group.buildInheritedID(groupKey, gi.getGeneration());
        if (gi.getGroup(inherit) != null) {
          DataAccessContext rcxI = new DataAccessContext(rcx, gi);
          deleteSubGroupDetails(appState, rcxI, inherit, support);
        }
      }
    }

    // Now delete from the instance:
    
    deleteSubGroupDetails(appState, rcx, groupKey, support);
 
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
    
    doTopInstanceGroupCleanup(appState, rcx, groupKey, support);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a subgroup from the model
  */  
  
  private static void deleteSubGroupDetails(BTState appState, DataAccessContext rcx, String subKey, UndoSupport support) {
    GenomeChange[] retval = rcx.getGenomeAsInstance().removeSubGroup(subKey);
    if (retval != null) {
      for (int i = 0; i < retval.length; i++) {
        if (retval[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcx, retval[i]);
          support.addEdit(gcc);
          support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        }              
      }
    }
       
    GroupSettingChange gsc = rcx.gsm.dropGroupVisibility(rcx.getGenomeID(), subKey);  
    if (gsc != null) {
      GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState, rcx, gsc);
      support.addEdit(gscc);
    }
    return;
  }

  /***************************************************************************
  **
  ** Delete a group from the model
  */  
  
  private static void deleteGroupDetails(BTState appState, DataAccessContext rcx, String groupKey, UndoSupport support) {
    
    //
    // If there are any modules attached to the group, delete them too:
    //
    
    Map<String, Set<String>> moduleMap = rcx.getGenomeAsInstance().findModulesOwnedByGroup(groupKey);
    if (!moduleMap.isEmpty()) {
      NetOverlayController noc = appState.getNetOverlayController();
      Iterator<String> mmkit = moduleMap.keySet().iterator();
      while (mmkit.hasNext()) {
        String nokey = mmkit.next();
        Set<String> modSet = moduleMap.get(nokey);
        Iterator<String> msit = modSet.iterator();
        while (msit.hasNext()) {
          String modID = msit.next();
          OverlaySupport.deleteNetworkModule(appState, rcx, nokey, modID, support);
        }
      }
      noc.cleanUpDeletedModules(moduleMap, support, rcx);
    }
    
    //
    // Now remove the empty group
    //
    
    GenomeChange[] retval = rcx.getGenomeAsInstance().removeEmptyGroup(groupKey);
    if (retval != null) {
      for (int i = 0; i < retval.length; i++) {
        if (retval[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcx, retval[i]);
          support.addEdit(gcc);
          support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        }              
      }
    }
    
    GroupSettingManager gsm = (GroupSettingManager)rcx.gsm;          
    GroupSettingChange gsc = gsm.dropGroupVisibility(rcx.getGenomeID(), groupKey);  
    if (gsc != null) {
      GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState, rcx, gsc);
      support.addEdit(gscc);
    }
    return;
  }  

  /***************************************************************************
  **
  ** If we have the top instance, delete the group properties and the mappings!
  */  
  
  private static void doTopInstanceGroupCleanup(BTState appState, DataAccessContext rcx, 
                                                String key, UndoSupport support) {   
    //
    // Now if we have the top instance, delete the group properties and the mappings!
    //
                 
    GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
    if (parent != null) {
      return;
    }
            
    String topInstID = rcx.getGenomeID();
    Iterator<Layout> layit = rcx.lSrc.getLayoutIterator();
    while (layit.hasNext()) {
      Layout layout = layit.next();
      if (layout.haveGroupMetadataDependency(topInstID, key)) {
        Layout.PropChange lpc = layout.dropGroupMetadataDependency(topInstID, key);
        if (lpc != null) {
          PropChangeCmd pcc = new PropChangeCmd(appState, rcx, lpc);
          support.addEdit(pcc);        
        }
      } 
      if (layout.getTarget().equals(topInstID)) {
        Layout.PropChange[] lpc = new Layout.PropChange[1];             
        lpc[0] = layout.removeGroupProperties(key);        
        if (lpc[0] != null) {
          PropChangeCmd pcc = new PropChangeCmd(appState, rcx, lpc);
          support.addEdit(pcc);
          support.addEvent(new LayoutChangeEvent(layout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        }
      }
    }
    
    doTopInstanceGroupMapCleanup(appState, rcx, key, support);
    
    return;
  }
  
  /***************************************************************************
  **
  ** If we have the top instance, delete the mappings!
  */  
  
  public static void doTopInstanceGroupMapCleanup(BTState appState, DataAccessContext rcx, 
                                                  String key, UndoSupport support) {   
    //
    // Now if we have the top instance, delete the group mappings!
    //
                    
    GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
    if (parent != null) {
      return;
    }
    
    TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
    if (tcd != null) {
      TimeCourseChange tchg = tcd.dropGroupMap(key);
      if (tchg != null) {
        TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(appState, rcx, tchg, false);
        support.addEdit(cmd);
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      }
    }
    
    TemporalInputRangeData tird = rcx.getExpDataSrc().getTemporalInputRangeData();
    if (tird != null) {
      TemporalInputChange tichg = tird.dropGroupMap(key);
      if (tichg != null) {
        TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(appState, rcx, tichg, false);
        support.addEdit(cmd);
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      }
    }
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Following an instruction install, pitch any group-related data that is
  ** orphaned.
  */  
 
   public static void cleanupDanglingGroupData(BTState appState, DataAccessContext rcx, UndoSupport support) {  
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
     if (tcd != null) {
       Iterator<String> tcgmkit = tcd.getGroupMapKeys();
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
       TimeCourseChange tcc = tcd.dropGroupMap(dgKey);
       TimeCourseChangeCmd tccc = new TimeCourseChangeCmd(appState, rcx, tcc);
       support.addEdit(tccc);
     }
     
     //
     // Drop dangling temporal input group maps:
     //
     
     deadGroups.clear();
     TemporalInputRangeData tird = rcx.getExpDataSrc().getTemporalInputRangeData();
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
       TemporalInputChangeCmd ticc = new TemporalInputChangeCmd(appState, rcx, tic);
       support.addEdit(ticc);
     } 
     
     cleanupDanglingVisibility(appState, rcx, support);     
  
     return;
   } 
   
  /***************************************************************************
  **
  ** Following an instruction install, pitch any group-related data that is
  ** orphaned.
  */  
 
   public static void cleanupDanglingVisibility(BTState appState, DataAccessContext rcx, UndoSupport support) {  

     GroupSettingManager gsm = appState.getGroupMgr();
     
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
