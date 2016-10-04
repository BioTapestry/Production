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

package org.systemsbiology.biotapestry.cmd;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.Layout.PropChange;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of commands to add things
*/

public class ModificationCommands {
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor.  Now stateless; make all ops static for now.
  */ 
  
  private ModificationCommands() {
  }
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Reset the members in a net module
  */  
 
  public static boolean resetModuleMembers(BTState appState, Set<String> nodeIDs, DataAccessContext rcx, String moduleID, String overlayKey) {
      
    //
    // Changing module geometry may need module link pad fixups:
    //
    

    Layout.PadNeedsForLayout padFixups = rcx.getLayout().findAllNetModuleLinkPadRequirements(rcx);  
    
    NetOverlayOwner owner = rcx.getCurrentOverlayOwner();
    NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
    NetModule nmod = nov.getModule(moduleID);    
    
    //
    // Cannot delete the node if the module is auto node and the 
    // node is the last one!
    //
    
    if (nodeIDs.isEmpty()) {
      NetOverlayProperties noProps = rcx.getLayout().getNetOverlayProperties(overlayKey);
      NetModuleProperties nmp = noProps.getNetModuleProperties(moduleID);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        ResourceManager rMan = appState.getRMan();
        JOptionPane.showMessageDialog(appState.getTopFrame(), rMan.getString("deleteNodefromMod.lastOne"), 
                                      rMan.getString("deleteNodefromMod.lastOneTitle"),
                                      JOptionPane.ERROR_MESSAGE);

        return (false);
      }
    }
    
    HashSet<String> toDelete = new HashSet<String>();
    HashSet<String> currentMem = new HashSet<String>();
    Iterator<NetModuleMember> mit = nmod.getMemberIterator();
    while (mit.hasNext()) {
      NetModuleMember nmm = mit.next();
      String nodeID = nmm.getID();
      if (!nodeIDs.contains(nodeID)) {
        toDelete.add(nodeID);
      }
      currentMem.add(nodeID);
    }
    HashSet<String> toAdd = new HashSet<String>(nodeIDs);
    toAdd.removeAll(currentMem);
       
    UndoSupport support = new UndoSupport(appState, "undo.modifyNetModuleMembers");    
    Iterator<String> tait = toAdd.iterator();
    while (tait.hasNext()) {
      String addID = tait.next();
      NetModuleChange nmc = owner.addMemberToNetworkModule(overlayKey, nmod, addID);
      if (nmc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcx, nmc);
        support.addEdit(gcc);
      }
    }
    Iterator<String> tdit = toDelete.iterator();
    while (tdit.hasNext()) {
      String delID = tdit.next();
      NetModuleChange nmc = owner.deleteMemberFromNetworkModule(overlayKey, nmod, delID);
      if (nmc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState, rcx, nmc);
        support.addEdit(gcc);
      }
    }    
        
    //
    // Complete the fixups:
    //
    
    AddCommands.finishNetModPadFixups(appState, null, null, rcx, padFixups, support);        
    
    support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));   
    support.finish();    
    return (true);
  }  
    
  /***************************************************************************
  **
  ** When doing instruction builds, member-only modules may lose all members,
  ** and need to be converted to survive.
  */

  public static void repairEmptiedMemberOnlyModules(BTState appState, DataAccessContext dacx,
                                                    Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> emptyModGeom, 
                                                    UndoSupport support) {  
    
    Iterator<String> akit = emptyModGeom.keySet().iterator();
    while (akit.hasNext()) {
      String layoutID = akit.next();
      Layout lo = dacx.lSrc.getLayout(layoutID);
      DataAccessContext rcx = new DataAccessContext(dacx, dacx.getGenomeSource().getGenome(lo.getTarget()), lo);
      Layout.PropChange[] emptyLpc = lo.repairAllEmptyMemberOnlyNetModules(emptyModGeom, rcx);
      if ((emptyLpc != null) && (emptyLpc.length != 0)) {
        PropChangeCmd emptyC = new PropChangeCmd(appState, rcx, emptyLpc);
        support.addEdit(emptyC);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Repair pad needs for given genome.  Note that since this does fixups of
  ** all pads in a LAYOUT, you don't want to call this ALSO for submodels
  ** multiple times...
  ** 9/12/12: At this time, orphans only arg is true for genome (db and instance) expansions, and
  ** for DBGenome compressions.  That is the sign that caller is trying to do consistent pad
  ** mods for the links, thus only last-ditch orphans need attention.  But consider hoisting 
  ** the orphansOnlyForAll() call up to be the caller responsibility for better granularity
  ** on a per-overlay basis.
  */

  public static void repairNetModuleLinkPadsLocally(BTState appState, Layout.PadNeedsForLayout padFixups, 
                                                    DataAccessContext rcx, boolean orphansOnly, UndoSupport support) {  
    Map<String, Boolean> orpho = rcx.getLayout().orphansOnlyForAll(orphansOnly);
    Layout.PropChange[] padLpc = rcx.getLayout().repairAllNetModuleLinkPadRequirements(rcx, padFixups, orpho);
    if ((padLpc != null) && (padLpc.length != 0)) {
      PropChangeCmd padC = new PropChangeCmd(appState, rcx, padLpc);
      // One of the problems uncovered in issue #187, used with null support...
      if (support != null) { // used in preview mode....
        support.addEdit(padC);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Repair net module link pads.  9/12/12: At this time, orphans only arg
  ** is always being given as false...consider hoisting the orphansOnlyForAll()
  ** call up to be the caller responsibility for better granularity?
  */  
 
  public static void repairNetModuleLinkPadsGlobally(BTState appState, DataAccessContext dacx, Map<String, Layout.PadNeedsForLayout> globalNeeds,
                                                     boolean orphansOnly, UndoSupport support) {
    
    Iterator<String> akit = globalNeeds.keySet().iterator();
    while (akit.hasNext()) {
      String layoutID = akit.next();
      Layout lo = dacx.lSrc.getLayout(layoutID);
      Layout.PadNeedsForLayout needsForLayout = globalNeeds.get(layoutID);      
      if (needsForLayout != null) {
        Map<String, Boolean> orpho = lo.orphansOnlyForAll(orphansOnly);
        DataAccessContext rcx = new DataAccessContext(dacx, dacx.getGenomeSource().getGenome(lo.getTarget()), lo);
        PropChange[] pca = lo.repairAllNetModuleLinkPadRequirements(rcx, needsForLayout, orpho); 
        if ((pca != null) && (pca.length != 0)) {
          PropChangeCmd pcc = new PropChangeCmd(appState, rcx, pca);
          support.addEdit(pcc);
          LayoutChangeEvent lcev = new LayoutChangeEvent(layoutID, LayoutChangeEvent.UNSPECIFIED_CHANGE);
          support.addEvent(lcev);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** When a (base) node name is changed, all instances of the node in any other
  ** layout that have multi-line break definitions need to be modified!
  */  
 
  public static void changeNodeNameBreaks(BTState appState, DataAccessContext dacx, String nodeID, Genome genome, 
                                           LineBreaker.LineBreakChangeSteps steps, 
                                           String untrimmed,
                                           UndoSupport support) {  
    
    String baseID = GenomeItemInstance.getBaseID(nodeID);
    //
    // We skip the node triggering the change; it has ben fixed already
    //
    String genomeToSkip = genome.getID();
    String nodeToSkip = nodeID;
    
    HashSet<String> fixedLayouts = new HashSet<String>();
    Iterator<Layout> loit = dacx.lSrc.getLayoutIterator();
    while (loit.hasNext()) {
      Layout lo = loit.next();
      String loTarg = lo.getTarget();
      Genome gen = dacx.getGenomeSource().getGenome(loTarg);
      DataAccessContext rcx = new DataAccessContext(dacx, gen, lo);    
      Set<String> nodesToFix;
      boolean checkForLocal = false;
      
      if (gen instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)gen;
        nodesToFix = gi.getNodeInstances(baseID);
        checkForLocal = true;
      } else {
        nodesToFix = new HashSet<String>();
        nodesToFix.add(baseID);
      }   
      if (nodesToFix.isEmpty()) {
        continue;
      }
      
      Iterator<String> nit = nodesToFix.iterator();
      while (nit.hasNext()) {
        String nextNodeID = nit.next();
        if (loTarg.equals(genomeToSkip) && nextNodeID.equals(nodeToSkip)) {
          continue;
        }      
        // Local override names ignore global changes:
        if (checkForLocal) {
          NodeInstance ni = (NodeInstance)gen.getNode(nextNodeID);    
          String overName = ni.getOverrideName();
          if (overName != null) {
            continue;
          }
        }
        
        NodeProperties nextProp = lo.getNodeProperties(nextNodeID);
        if (nextProp.getLineBreakDef() != null) {
          NodeProperties changedProps = nextProp.clone();          
          changedProps.applyLineBreakMod(steps, untrimmed);
          Layout.PropChange[] lpc = new Layout.PropChange[1]; 
          lpc[0] = lo.replaceNodeProperties(nextProp, changedProps);
          if (lpc[0] != null) {
            PropChangeCmd pcc = new PropChangeCmd(appState, rcx, lpc);
            support.addEdit(pcc);   
            fixedLayouts.add(lo.getID());
          }
        }
      }     
    }

    Iterator<String> flit = fixedLayouts.iterator();
    while (flit.hasNext()) {
      String loName = flit.next();    
      LayoutChangeEvent lcev = new LayoutChangeEvent(loName, LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);
    }
    
    return;
  }
}
