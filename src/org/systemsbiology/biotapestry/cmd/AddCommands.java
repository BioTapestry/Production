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

package org.systemsbiology.biotapestry.cmd;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of commands to add things
*/

public class AddCommands {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor:
  */ 
  
  private AddCommands() {
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
     
  /***************************************************************************
  **
  ** Support for adding into net modules.  NOTE: The node must already have been
  ** added to the model so it's there when we check for group membership!
  **
  */  
 
  public static void drawIntoNetModuleSupport(Node newNode, List<String> modCandidates, StaticDataAccessContext rcx, String overlayKey, UndoSupport support) { 
    //
    // If instructed to add to current net module, do so here:
    //
    if ((modCandidates != null) && !modCandidates.isEmpty()) {
      NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID());
      NetworkOverlay nol = owner.getNetworkOverlay(overlayKey);
      int numMod = modCandidates.size();
      for (int i = 0; i < numMod; i++) {
        String moduleID =  modCandidates.get(i);      
        NetModule nmod = nol.getModule(moduleID);
        String attachedGroup = nmod.getGroupAttachment();
        if (attachedGroup != null) {
          GenomeInstance gi = (GenomeInstance)rcx.getCurrentGenome();
          Group group = gi.getGroup(attachedGroup); 
          if (!group.isInGroup(newNode.getID(), gi)) {
            continue;
          }
        }        
        NetModuleChange nmc = owner.addMemberToNetworkModule(overlayKey, nmod, newNode.getID());
        if (nmc != null) {
          NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, nmc);
          support.addEdit(gcc);
        }
      }
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Automatically add a derived link to the root model.
  */  
 
  public static Linkage autoAddLinkToRoot(StaticDataAccessContext rcx, 
                                          String srcID, String targID, int sign,
                                          UndoSupport support, boolean allowDuplicates, 
                                          String oldID, PadConstraints padLimits, 
                                          int evidenceLevel, InvertedSrcTrg ist) {

    DBGenome genome = rcx.getDBGenome();
    //
    // If we already have a link between the source and the target, we DO NOT
    // add another one, unless it has opposite sign, unless we specify that we allow duplicates
    //

    if (!allowDuplicates) {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        DBLinkage link = (DBLinkage)lit.next();
        String src = link.getSource();
        String trg = link.getTarget();
        int testSign = link.getSign();
        if (src.equals(srcID) && trg.equals(targID) && (testSign == sign)) {
          return (null);
        }
      }
    }
     
    PadCalculatorToo pcalc = new PadCalculatorToo();
    PadCalculatorToo.PadResult pads = pcalc.padCalcFast(rcx.getGenomeSource(), srcID, targID, padLimits, true, ist);
    String linkID = (oldID == null) ? genome.getNextKey() : oldID;
    DBLinkage newLinkage = new DBLinkage(rcx, null, linkID, srcID, targID, sign, pads.landing, pads.launch);
    if (evidenceLevel != Linkage.LEVEL_NONE) {
      newLinkage.setTargetLevel(evidenceLevel);
    }
    GenomeChange gc = genome.addLinkWithExistingLabel(newLinkage);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
      support.addEdit(gcc);
    }
    if (ist != null) {
      ist.addNewLink(newLinkage);
    }
      
    return (newLinkage); 
  } 

  /***************************************************************************
  **
  ** Automatically add a derived link to the root model.
  */  
 
  public static Linkage autoAddOldOrNewLinkToRoot(StaticDataAccessContext rcx, 
                                                  String srcID, String targID, int sign,
                                                  DBGenome oldGenome, UndoSupport support, 
                                                  boolean allowDuplicates, 
                                                  String oldID, PadConstraints padLimits, 
                                                  int evidenceLevel, InvertedSrcTrg ist) {
    return (autoAddOldOrNewLinkToRoot(rcx, null, srcID, targID, sign, oldGenome, support, 
                                      allowDuplicates, oldID, padLimits, true, evidenceLevel, ist));   
  }  
  
  /***************************************************************************
  **
  ** Automatically add a derived link to the root model.
  */  
 
  public static Linkage autoAddOldOrNewLinkToRoot(StaticDataAccessContext rcx, 
                                                  String newName, String srcID, String targID, int sign,
                                                  DBGenome oldGenome, UndoSupport support, 
                                                  boolean allowDuplicates, 
                                                  String oldID, PadConstraints padLimits, 
                                                  boolean nodesCanGrow, int evidenceLevel, InvertedSrcTrg ist) {

    DBGenome genome = rcx.getDBGenome();   
    //
    // If we already have a link between the source and the target, we DO NOT
    // add another one, unless it has opposite sign, unless we specify that we allow duplicates
    //

    if (!allowDuplicates) {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        DBLinkage link = (DBLinkage)lit.next();
        String src = link.getSource();
        String trg = link.getTarget();
        int testSign = link.getSign();
        if (src.equals(srcID) && trg.equals(targID) && (testSign == sign)) {
          return (null);
        }
      }
    }
    
    Linkage oldLink = null;
    if (oldID != null) {      
      oldLink = oldGenome.getLinkage(oldID);
    }
     
    PadCalculatorToo pcalc = new PadCalculatorToo();
    PadCalculatorToo.PadResult pads = pcalc.padCalcFast(rcx.getGenomeSource(), srcID, targID, padLimits, nodesCanGrow, ist);
    String linkID = (oldID == null) ? genome.getNextKey() : oldID;
    DBLinkage newLinkage;
    if (oldLink == null) {
      newLinkage = new DBLinkage(rcx, newName, linkID, srcID, targID, sign, pads.landing, pads.launch);
      if (evidenceLevel != Linkage.LEVEL_NONE) {
        newLinkage.setTargetLevel(evidenceLevel);
      }
    } else {
      if (newName != null) {
        throw new IllegalArgumentException();
      }
      newLinkage = new DBLinkage((DBLinkage)oldLink);
      if (evidenceLevel != Linkage.LEVEL_NONE) {
        newLinkage.setTargetLevel(evidenceLevel);
      }      
    }
    
    GenomeChange gc = genome.addLinkWithExistingLabel(newLinkage);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
      support.addEdit(gcc);
    }
    if (ist != null) {
      ist.addNewLink(newLinkage);
    }
    
    return (newLinkage); 
  }   

  /***************************************************************************
  **
  ** Creates a junk layout, which is going to be thrown out.  The layout template
  ** feature operates on the assumption that a layout already exists, so we need to
  ** do this first when creating denovo layouts from the template engine.
  */  
 
  public static void junkLayout(StaticDataAccessContext rcx, UndoSupport support) {      

    Genome genome = rcx.getCurrentGenome();
    String loKey = rcx.getCurrentLayoutID();
    Map<String, Layout.OverlayKeySet> allKeys = rcx.getFGHO().fullModuleKeysPerLayout();
    Layout.OverlayKeySet loModKeys = allKeys.get(loKey);   
 
    DatabaseChange dc = (support != null) ? rcx.getLayoutSource().startLayoutUndoTransaction(loKey) : null;
    
    Layout.SupplementalDataCoords sdc = rcx.getCurrentLayout().getSupplementalCoords(rcx, loModKeys);
    rcx.getCurrentLayout().dropProperties(rcx);    
    dc = (support != null) ? rcx.getLayoutSource().finishLayoutUndoTransaction(dc) : null;
    if (dc != null) {
      support.addEdit(new DatabaseChangeCmd(rcx, dc));
    }
   
    //
    // Now layout the nodes:
    //

    int numNodes = genome.getFullNodeCount();
    int cols = (int)Math.ceil(Math.sqrt(numNodes));
    Iterator<Node> anit = genome.getAllNodeIterator();
    int rowCount = 0;
    int colCount = 0;
    while (anit.hasNext()) {
      Node node = anit.next();
      Point2D loc = new Point2D.Double(colCount++ * 200.0, rowCount * 200.0);
      if (colCount > cols) {
        colCount = 0;
        rowCount++;
      }
      UiUtil.forceToGrid(loc, UiUtil.GRID_SIZE);
      String nodeID = node.getID();
      int nodeType = node.getNodeType();
      NodeProperties np = new NodeProperties(rcx.getColorResolver(), nodeType, nodeID, loc.getX(), loc.getY(), false);
      Layout.PropChange[] lpc = new Layout.PropChange[1];    
      lpc[0] = rcx.getCurrentLayout().setNodeProperties(nodeID, np);
      if ((lpc != null) && (support != null)) {
        PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
        support.addEdit(pcc);
      }
    }
    
    HashSet<String> newLinks = new HashSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      newLinks.add(link.getID());
    }
    LayoutLinkSupport.gridlessLinkLayoutPrep(rcx, newLinks, null);
    
    //
    // Now layout the links:
    //
   /* 
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      LayoutLinkSupport.autoAddCrudeLinkProperties(appState, genome, lo, linkID, support, null);
    }
    */
    dc = (support != null) ? rcx.getLayoutSource().startLayoutUndoTransaction(loKey) : null;
    rcx.getCurrentLayout().applySupplementalDataCoords(sdc, rcx, loModKeys);
    dc = (support != null) ? rcx.getLayoutSource().finishLayoutUndoTransaction(dc) : null;
    if (dc != null) {
      support.addEdit(new DatabaseChangeCmd(rcx, dc));
    }
 
    return;
  }  
 
  /***************************************************************************
  **
  ** Handle details of net module fixups
  */  
 
  public static void finishNetModPadFixups(StaticDataAccessContext rcxRoot, Layout.PadNeedsForLayout rootFixups, 
                                           StaticDataAccessContext rcxVfa, Layout.PadNeedsForLayout padFixups, UndoSupport support) {  

    
    //
    // Root layout:
    //
    if (rootFixups != null) {
      Map<String, Boolean> orpho = rcxRoot.getCurrentLayout().orphansOnlyForAll(false);
      Layout.PropChange[] padLpc = rcxRoot.getCurrentLayout().repairAllNetModuleLinkPadRequirements(rcxRoot, rootFixups, orpho);
      if ((padLpc != null) && (padLpc.length != 0)) {
        PropChangeCmd padC = new PropChangeCmd(rcxRoot, padLpc);
        support.addEdit(padC);
        support.addEvent(new LayoutChangeEvent(rcxRoot.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
      }  
    }
    //
    // Instance layout:
    //
    Map<String, Boolean> orpho = rcxVfa.getCurrentLayout().orphansOnlyForAll(false);
    Layout.PropChange[] padLpc = rcxVfa.getCurrentLayout().repairAllNetModuleLinkPadRequirements(rcxVfa, padFixups, orpho);
    if ((padLpc != null) && (padLpc.length != 0)) {
      PropChangeCmd padC = new PropChangeCmd(rcxVfa, padLpc);
      support.addEdit(padC);
      support.addEvent(new LayoutChangeEvent(rcxVfa.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
    } 
    return;
  }
}
