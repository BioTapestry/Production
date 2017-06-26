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


package org.systemsbiology.biotapestry.cmd.flow.link;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of commands to add things
*/

public class LinkSupport {
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor.  Now stateless; make all ops static for now.
  */ 
  
  private LinkSupport() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static final int IS_FOR_TARGET = 0;
  public static final int IS_FOR_SOURCE = 1;
  public static final int IS_FOR_SWAP   = 2;   
       
  public enum SwapType {NO_SWAP, TARGET_SWAP, SOURCE_SWAP, EITHER_SWAP};

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Answer if source pad can accept a link
  */  
  
  public static boolean sourcePadIsClear(UIComponentSource uics, DataAccessContext dacx, Genome genome, String id, int padNum, boolean shared) {
    //
    // Crank all the links looking for the node as source, and reject if the
    // launch pad matches ours.
    //
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      //
      // Only one source pad allowed.  If any other link emerges from us, we
      // must init the link off a tree corner, not the node.
      //
      if (lnk.getSource().equals(id)) { // && (lnk.getLaunchPad() == padNum)) {
        ResourceManager rMan = dacx.getRMan();
        JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                      rMan.getString("addLink.onlyOneSourcePad"), 
                                      rMan.getString("addLink.placementErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      if (shared && lnk.getTarget().equals(id) && (lnk.getLandingPad() == padNum)) {
        ResourceManager rMan = dacx.getRMan();
        JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                      rMan.getString("addLink.cannotShareSourcePad"), 
                                      rMan.getString("addLink.placementErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    return (true);  
  }
  
  /***************************************************************************
  **
  ** Answers if the given source pad can accept a new link.
  */  
  
  private static boolean sourcePadIsClear(Genome genome, Layout lo, String id, int padNum) {
    //
    // Always cool if pads namespaces are not shared:
    //
    NodeProperties np = lo.getNodeProperties(id);
    INodeRenderer rend = np.getRenderer();
    boolean shared = rend.sharedPadNamespaces();
    if (!shared) {
      return (true);
    }
    
    //
    // Crank all the links looking for the node as source, and reject if the
    // launch pad matches ours.
    //
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getTarget().equals(id) && (lnk.getLandingPad() == padNum)) {
        return (false);
      }
    }
    return (true);  
  }

  /***************************************************************************
  **
  ** Answers if the given target pad can accept a new link.
  */  
  
  private static boolean targPadIsClear(Genome genome, Layout lo, String id, int padNum) {
    //
    // Always cool if pads namespaces are not shared:
    //
    NodeProperties np = lo.getNodeProperties(id);
    INodeRenderer rend = np.getRenderer();
    boolean shared = rend.sharedPadNamespaces();
    if (!shared) {
      return (true);
    }  
    //
    // Crank all the links looking for the node as source, and reject if the
    // launch pad matches ours.
    //
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      //
      // No inbound link allowed if it is a source:
      //
      if (lnk.getSource().equals(id) && (lnk.getLaunchPad() == padNum)) {
        return (false);
      }
    }
    return (true);  
  }
   
  /***************************************************************************
  **
  ** Answer if target pad can accept a link
  */  
  
  public static boolean targPadIsClear(Genome genome, String id, int padNum, boolean shared) {
    if (!shared) {
      return (true);
    }
    //
    // Crank all the links looking for the node as source, and reject if the
    // launch pad matches ours.
    //
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      //
      // No inbound link allowed if it is a source:
      //
      if (lnk.getSource().equals(id) && (lnk.getLaunchPad() == padNum)) {
        return (false);
      }
    }
    return (true);  
  }  
  
  /***************************************************************************
  **
  ** Do pad swaps for specialty layouts
  */  
  
   public static void specialtyPadChanges(Map<String, PadCalculatorToo.PadResult> padChanges, 
                                          StaticDataAccessContext rcx,
                                          UndoSupport support, InvertedSrcTrg ist) {
     //
     // Wiggle pad assignments based on node locations:
     //
     
     Iterator<String> pcit = padChanges.keySet().iterator();
     while (pcit.hasNext()) {
       String linkID = pcit.next();
       PadCalculatorToo.PadResult pres = padChanges.get(linkID);
       Linkage link = rcx.getCurrentGenome().getLinkage(linkID);
       // Have to make changes to all shared source links! (YUK)
       String src = link.getSource();
       HashSet<String> allLinks = new HashSet<String>();
       Set<String> forSrc = ist.outboundLinkIDs(src);
       Iterator<String> alit = forSrc.iterator();
       while (alit.hasNext()) {
         String nlID = alit.next(); 
         Linkage nextLink = rcx.getCurrentGenome().getLinkage(nlID);
         if (pres.launch != nextLink.getLaunchPad()) {
           allLinks.add(nextLink.getID());
         }
       }
       //
       // Starting Version 6, this routine is used for genome instances as well.
       // So we now gotta make sure _all_ subset models are kept consistent!
       //
  
       changeLinkSourceCore(pres.launch, rcx, support, allLinks);
           
       link = rcx.getCurrentGenome().getLinkage(linkID);  // may have changed in above operation: handle is stale        
       if (pres.landing != link.getLandingPad()) {      
         changeLinkTargetCore(pres.landing, rcx, support, linkID);
       }
     }
     return;
   }  
 
  /***************************************************************************
  **
  ** Swap link pads
  */  
 
  public static boolean swapLinkPads(Intersection.PadVal newPad, String id, StaticDataAccessContext rcx, 
                                     SwapType swapMode, String myLinkId, UndoFactory uFac) {
    
    //
    // Fix for BT-10-28-09:1. Figure out if we need to mess with source/target swap 
    // issues  When doing a target swap, don't swap sources unless pad namespaces
    // are shared.  Same with source swap!
    //
    
    INodeRenderer nodeRenderer = rcx.getCurrentLayout().getNodeProperties(id).getRenderer();
    boolean swapNodeShared = nodeRenderer.sharedPadNamespaces();

    //
    // If we are doing a target swap to a target pad, find everybody who lands 
    // there, and swap them all.  If doing a target swap to a source pad, 
    // do for sources.
    //
    
    Linkage myLink = rcx.getCurrentGenome().getLinkage(myLinkId);
    int oldPad = (swapMode == SwapType.TARGET_SWAP) ? myLink.getLandingPad() : myLink.getLaunchPad();
       
    HashSet<String> targLinksToSwapForward = new HashSet<String>();
    HashSet<String> targLinksToSwapBack = new HashSet<String>();    
    String aSrcLinkToSwapForward = (swapMode == SwapType.SOURCE_SWAP) ? myLinkId : null;  
    String aSrcLinkToSwapBack = null;      

    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if ((swapMode == SwapType.TARGET_SWAP) || swapNodeShared) {
        String trg = link.getTarget();
        if (trg.equals(id)) {
          int land = link.getLandingPad();
          if (land == oldPad) {
            targLinksToSwapForward.add(link.getID());
          } else if (land == newPad.padNum) {
            targLinksToSwapBack.add(link.getID());
          }
        }
      }
      if ((swapMode == SwapType.SOURCE_SWAP) || swapNodeShared) {
        if (aSrcLinkToSwapBack == null) {
          String src = link.getSource();
          if (src.equals(id) && (link.getLaunchPad() == newPad.padNum)) {
            aSrcLinkToSwapBack = link.getID();
          }
        }
      }
    }
    
    //
    // If the target is a gene with modules, we cannot just swap pads if that
    // changes the module assignment! Source and target modules must be the same (or
    // both must be holders):
    //
    
    Node targNode = rcx.getCurrentGenome().getNode(id);
    if (targNode.getNodeType() == Node.GENE) {
      Gene targGene = (Gene)targNode;
      if (targGene.getNumRegions() > 0) {
        boolean passes = false;
        DBGeneRegion targRegOld = targGene.getRegionForPad(oldPad);
        DBGeneRegion targRegNew = targGene.getRegionForPad(newPad.padNum);
        if (targRegOld.isHolder() && targRegNew.isHolder()) {
          passes = true;
        } else if (targRegOld.getKey().equals(targRegNew.getKey())) {
          passes = true;
        }
        if (!passes) {
          return (false);
        }
      }
    }
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac.provideUndoSupport("undo.swapLinkPads", rcx); 
    
    Iterator<String> tfit = targLinksToSwapForward.iterator();
    while (tfit.hasNext()) {
      String linkID = tfit.next();
      changeLinkTargetCore(newPad.padNum, rcx, support, linkID);      
    }
    Iterator<String> tbit = targLinksToSwapBack.iterator();
    while (tbit.hasNext()) {
      String linkID = tbit.next();
      changeLinkTargetCore(oldPad, rcx, support, linkID);      
    }
    if (aSrcLinkToSwapForward != null) {
      Set<String> allLinks = rcx.getCurrentLayout().getSharedItems(aSrcLinkToSwapForward);
      changeLinkSourceCore(newPad.padNum, rcx, support, allLinks);
    }
    if (aSrcLinkToSwapBack != null) {
      Set<String> allLinks = rcx.getCurrentLayout().getSharedItems(aSrcLinkToSwapBack);
      changeLinkSourceCore(oldPad, rcx, support, allLinks);
    }    
    support.finish();     
    return (true); 
  }  

  /***************************************************************************
  **
  ** Change the link target. Shared because it is used by a couple different control flows
  */  
 
  public static boolean changeLinkTarget(Intersection cro, List<Intersection.PadVal> padCand, String id, 
                                         StaticDataAccessContext rcxT, UndoFactory uFac) {
    

    
    String lid = cro.getObjectID();  // Remember... this may not be the link we want!
    BusProperties bp = rcxT.getCurrentLayout().getLinkProperties(lid);
    LinkSegmentID[] linkIDs = cro.segmentIDsFromIntersect();
    // Only allowed to perform this op if there is only one link through the segment! 
    Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);
    String myLinkId = throughSeg.iterator().next();
    Linkage link = rcxT.getCurrentGenome().getLinkage(myLinkId);
    int targPad = link.getLandingPad();
    
    //
    // We reject unless the target we are intersecting matches the
    // link we are trying to relocate.
    //

    if (!link.getTarget().equals(id)) {
      return (false);
    }
    
    //
    // If the target is a gene with modules, we cannot just swap pads if that
    // changes the module assignment!
    //
    
    DBGeneRegion targReg = null;
    Gene targGene = null;
    boolean needModCheck = false;
    Node targNode = rcxT.getCurrentGenome().getNode(id);
    if (targNode.getNodeType() == Node.GENE) {
      targGene = (Gene)targNode;
      if (targGene.getNumRegions() > 0) {
        needModCheck = true;
        targReg = targGene.getRegionForPad(targPad);
        if ((targReg != null) && targReg.isHolder()) {
          targReg = null;
        }
      }
    }
    DBGeneRegion.DBRegKey trk = ((targReg == null) || targReg.isHolder()) ? null : targReg.getKey();

    
    int numCand = padCand.size();
    Intersection.PadVal winner = null;       
    for (int i = 0; i < numCand; i++) {
      Intersection.PadVal pad = padCand.get(i);
      if (!pad.okEnd) {
        continue;
      }  
      if (!targPadIsClear(rcxT.getCurrentGenome(), rcxT.getCurrentLayout(), id, pad.padNum)) {
        continue;
      }
      
      if (needModCheck) {
        DBGeneRegion chkReg = targGene.getRegionForPad(pad.padNum);
        DBGeneRegion.DBRegKey ctrk = ((chkReg == null) || chkReg.isHolder()) ? null : chkReg.getKey();
        if ((trk != null) && ((ctrk == null) || !ctrk.equals(trk))) {
          continue;
        } else if ((trk == null) && (ctrk != null))  {
          continue;
        }
      }

      if ((winner == null) || (winner.distance > pad.distance)) {
        winner = pad;
      }
    }
    
    if (winner == null) {
      return (false);
    }
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac.provideUndoSupport("undo.changeLinkTarget", rcxT);     
    changeLinkTargetCore(winner.padNum, rcxT, support, myLinkId);   
    support.finish();
    
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Change the link target gene module.
  */  
 
  public static boolean changeLinkTargetGeneModule(Intersection cro, List<Intersection.PadVal> padCand, String id, 
                                                   StaticDataAccessContext rcxT, UndoFactory uFac) {
    
    String lid = cro.getObjectID();  // Remember... this may not be the link we want!
    BusProperties bp = rcxT.getCurrentLayout().getLinkProperties(lid);
    LinkSegmentID[] linkIDs = cro.segmentIDsFromIntersect();
    // Only allowed to perform this op if there is only one link through the segment! 
    Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);
    String myLinkId = throughSeg.iterator().next();
    Linkage link = rcxT.getCurrentGenome().getLinkage(myLinkId);
    int targPad = link.getLandingPad();
    
    //
    // We reject unless the target we are intersecting matches the
    // link we are trying to relocate.
    //

    if (!link.getTarget().equals(id)) {
      return (false);
    }
    
    //
    // We must actually be switching modules, or we reject the change. Might be removing from all modules, however:
    //
    
    Node targNode = rcxT.getCurrentGenome().getNode(id);
    if (targNode.getNodeType() != Node.GENE) {
      return (false);
    }
    Gene targGene = (Gene)targNode;
    if (targGene.getNumRegions() == 0) {
      return (false);
    }
    //
    // This is the current region assignment:
    //
    
    DBGeneRegion targReg = targGene.getRegionForPad(targPad);
    if ((targReg != null) && targReg.isHolder()) {
      targReg = null;
    }
    DBGeneRegion.DBRegKey trk = (targReg == null) ? null : targReg.getKey();
    
    int numCand = padCand.size();
    Intersection.PadVal winner = null;       
    for (int i = 0; i < numCand; i++) {
      Intersection.PadVal pad = padCand.get(i);
      if (!pad.okEnd) {
        continue;
      }  
      if (!targPadIsClear(rcxT.getCurrentGenome(), rcxT.getCurrentLayout(), id, pad.padNum)) {
        continue;
      }
      
      //
      // New region assignment:
      //
      DBGeneRegion chkReg = targGene.getRegionForPad(pad.padNum);
      DBGeneRegion.DBRegKey ctrk = (chkReg == null) ? null : chkReg.getKey();
      if ((trk == null) && (ctrk == null)) { // Not moving in or out of module
        continue;
      }
      boolean moveInOrOut = (((trk != null) && (ctrk == null)) || ((trk == null) && (ctrk != null)));
      boolean moveBetween = (moveInOrOut) ? false : !trk.equals(ctrk);
      if (!moveInOrOut && !moveBetween) {
        continue;
      }

      if ((winner == null) || (winner.distance > pad.distance)) {
        winner = pad;
      }
    }
    
    if (winner == null) {
      return (false);
    }
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac.provideUndoSupport("undo.changeLinkTarget", rcxT);      
    switchLinkRegion(support, myLinkId, winner.padNum, rcxT);
    support.finish();     
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Move all links in all modules to new cis-reg region
  */
       
  public static void switchLinkRegion(UndoSupport support, String linkKey, int newPad, StaticDataAccessContext rcxT) {

    String baseLinkID = GenomeItemInstance.getBaseID(linkKey);
    StaticDataAccessContext rcxR = rcxT.getContextForRoot();
  
    //
    // Go through each link, get its PadOffset, reposition it 
    //
  
    changeLinkTargetCore(newPad, rcxR, support, baseLinkID);
      
    Iterator<GenomeInstance> iit = rcxT.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      for (String liid : gi.returnLinkInstanceIDsForBacking(baseLinkID)) {
        StaticDataAccessContext daci = new StaticDataAccessContext(rcxT, gi.getID());
        changeLinkTargetCore(newPad, daci, support, liid);
      }  
    }   
    return;
  }

  /***************************************************************************
  **
  ** Change the link target: core routine
  */  
 
  public static void changeLinkTargetCore(int padNum, StaticDataAccessContext rcx, 
                                          UndoSupport support, String myLinkId) {
    
    Linkage link = rcx.getCurrentGenome().getLinkage(myLinkId);
    if (rcx.currentGenomeIsAnInstance()) {
      GenomeInstance rootInstance = rcx.getCurrentGenomeAsInstance();
      Iterator<GenomeInstance> giit = rcx.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (rootInstance.isAncestor(gi)) { 
          Linkage childLink = gi.getLinkage(myLinkId);
          if (childLink != null) {
            GenomeChange undo = gi.changeLinkageTarget(childLink, padNum);
            if (support != null) {
              support.addEdit(new GenomeChangeCmd(undo));
              ModelChangeEvent mcev = new ModelChangeEvent(rcx.getGenomeSource().getID(), gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
              support.addEvent(mcev);
            }
          }
        }
      }
    } else {
      GenomeChange undo = rcx.getCurrentGenome().changeLinkageTarget(link, padNum);
      if (support != null) {
        support.addEdit(new GenomeChangeCmd(undo));                
        ModelChangeEvent mcev = new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    }
    return; 
  }  
  
  /***************************************************************************
  **
  ** Change the link source
  */  
 
  public static boolean changeLinkSource(UIComponentSource uics, Intersection cro,
                                         List<Intersection.PadVal> padCand, String id, 
                                         StaticDataAccessContext dacx, UndoFactory uFac) {
    
    int numCand = padCand.size();
    Intersection.PadVal winner = null;
    
    for (int i = 0; i < numCand; i++) {
      Intersection.PadVal pad = padCand.get(i);
      if (!pad.okStart) {
        continue;
      }  
      if (!sourcePadIsClear(dacx.getCurrentGenome(), dacx.getCurrentLayout(), id, pad.padNum)) {
        continue;
      }
      
      if ((winner == null) || (winner.distance > pad.distance)) {
        winner = pad;
      }
    }
    
    if (winner == null) {
      return (false);
    }
       
    //
    // We reject unless the target we are intersecting matches the
    // link we are trying to relocate.
    //

    String lid = cro.getObjectID();    
    Set<String> allLinks = dacx.getCurrentLayout().getSharedItems(lid);
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      Linkage nextLink = dacx.getCurrentGenome().getLinkage(nextLinkID);
      if (!nextLink.getSource().equals(id)) {
        ResourceManager rMan = dacx.getRMan(); 
        JOptionPane.showMessageDialog(uics.getTopFrame(),
                                      rMan.getString("linkSrcChange.useNodeSwitch"),
                                      rMan.getString("linkSrcChange.useNodeSwitchTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac.provideUndoSupport("undo.changeLinkSource", dacx);          
    changeLinkSourceCore(winner.padNum, dacx, support, allLinks);    
    support.finish();
    
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Change the link source
  */  
 
  public static void changeLinkSourceCore(int padNum, StaticDataAccessContext rcxG,
                                          UndoSupport support, Set<String> allLinks) {
   
    //
    // Have to change source pad of all linkages sharing the start
    //
    
    GenomeInstance rootInstance = null;
    if (rcxG.currentGenomeIsAnInstance()) {
      rootInstance = rcxG.getCurrentGenomeAsInstance();
    }
    
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      Linkage nextLink = rcxG.getCurrentGenome().getLinkage(nextLinkID);
      if (rootInstance != null) {
        Iterator<GenomeInstance> giit = rcxG.getGenomeSource().getInstanceIterator();
        while (giit.hasNext()) {
          GenomeInstance gi = giit.next();
          if (rootInstance.isAncestor(gi)) {
            Linkage childLink = gi.getLinkage(nextLinkID);
            // Test for null addresses bug BT-06-15-05:2
            if (childLink != null) {
              GenomeChange undo = gi.changeLinkageSource(childLink, padNum);
              if (support != null) {
                support.addEdit(new GenomeChangeCmd(undo));
                ModelChangeEvent mcev = new ModelChangeEvent(rcxG.getGenomeSource().getID(), gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
                support.addEvent(mcev);
              }
            }
          }
        }
      } else {
        GenomeChange undo = rcxG.getCurrentGenome().changeLinkageSource(nextLink, padNum);
        if (support != null) {
          support.addEdit(new GenomeChangeCmd(undo));                
          ModelChangeEvent mcev = new ModelChangeEvent(rcxG.getGenomeSource().getID(), rcxG.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
          support.addEvent(mcev);
        }
      }
    }     
    return; 
  } 
  
  /***************************************************************************
  **
  ** Common code for relocating source or target pad relocation: validity check
  */ 
  
  public static boolean amIValidForTargetOrSource(Intersection selected, int type, boolean forModules, DataAccessContext rcx) {
    
    LinkSegmentID[] ids = selected.segmentIDsFromIntersect();
    if (ids == null) {
      return (false);
    }
    if (ids.length != 1) {
      return (false);
    }
    
    String oid = selected.getObjectID();

    String ovrID = rcx.getOSO().getCurrentOverlay();
    
    if (rcx.currentGenomeIsAnInstance()) {
      GenomeInstance parent = rcx.getCurrentGenomeAsInstance().getVfgParent();
      if (parent != null) {
        return (false);
      }
    }      
    
    if (forModules) {
      if (type == IS_FOR_TARGET) {
        String targDrop = rcx.getCurrentLayout().segmentSynonymousWithModLinkTargetDrop(oid, ids[0], ovrID);
        if (targDrop == null) {
          return (false);
        }
        HashSet<String> allLinks = new HashSet<String>();
        allLinks.add(targDrop);
        if (!haveModLinkDOFs(rcx, allLinks, ovrID)) {
          return (false);
        }
      } else if (type == IS_FOR_SOURCE) {
        Set<String> allLinks = rcx.getCurrentLayout().segmentSynonymousWithModLinkStartDrop(oid, ids[0], rcx.getOSO().getCurrentOverlay());
        if (allLinks == null) {
          return (false);
        }
        if (!haveModLinkDOFs(rcx, allLinks, ovrID)) {
          return (false);
        }
      } else  {
        throw new IllegalArgumentException();
      }
      return (true);
    }
        
    //
    // The rest of this is for regular links, not modules:
    //
   

   
    //
    // Note: If we are trying to change a source or target pad, and it is the only
    // pad of that type, we punt.  If we are trying to swap, we cannot swap if the
    // source pad is for source only
    //
    
    
    if (type == IS_FOR_TARGET) {
      String linkID = rcx.getCurrentLayout().segmentSynonymousWithTargetDrop(oid, ids[0]);
      if (linkID == null) {
        return (false);
      }
      //
      // Issue #183. Was using OID, which is for a random link in the link tree. Need to use the
      // link into the target.
      //
      if (!haveTargetDOFs(rcx, linkID)) {
        return (false);
      }
    } else if (type == IS_FOR_SOURCE) {
      if (!rcx.getCurrentLayout().segmentSynonymousWithStartDrop(oid, ids[0])) {
        return (false);
      }
      if (!haveSourceDOFs(rcx, oid)) {
        return (false);
      }
    } else if (type == IS_FOR_SWAP) {
      String linkID = rcx.getCurrentLayout().segmentSynonymousWithTargetDrop(oid, ids[0]);
      boolean startOK = rcx.getCurrentLayout().segmentSynonymousWithStartDrop(oid, ids[0]);
      if ((linkID == null) && !startOK) {
        return (false);
      }
      
      boolean haveDOF = false;     
      //
      // Issue #183. Was using OID, which is for a random link in the link tree. Need to use the
      // link into the target.
      //
      if ((linkID != null) && haveTargetDOFs(rcx, linkID)) {
        haveDOF = true;
      }
      if (!haveDOF && startOK && haveSourceDOFs(rcx, oid)) {
        haveDOF = true;
      }
      
      if (!haveDOF) {
        return (false);
      }
      
      //
      // If we are an autofeedback, EITHER_SWAP doesn't cut it:
      //
      
      if ((linkID != null) && startOK) {
        if (!ids[0].isForEndDrop() && !ids[0].isForStartDrop()) {
          Linkage link = rcx.getCurrentGenome().getLinkage(linkID);
          String src = link.getSource();
          String trg = link.getTarget();
          if (src.equals(trg)) {
            return (false);
          }
        }
      }
      

    } else {
      throw new IllegalArgumentException();
    }  

    return (true);
  }
   
  
  /***************************************************************************
  **
  ** Checks that we have source pad degrees of freedom
  */ 
  
  private static boolean haveSourceDOFs(DataAccessContext rcx, String linkID) {
    Linkage link = rcx.getCurrentGenome().getLinkage(linkID);
    String source = link.getSource();
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(source);
    INodeRenderer rend = np.getRenderer();
    boolean shared = rend.sharedPadNamespaces();
    int launchMax = rend.getFixedLaunchPadMax();
    if ((launchMax == 1) && !shared) {
      return (false);
    }
    return (true);
  }  
  
 /***************************************************************************
  **
  ** Checks that we have target pad degrees of freedom
  */ 
  
  private static boolean haveTargetDOFs(DataAccessContext rcx, String linkID) {
    Linkage link = rcx.getCurrentGenome().getLinkage(linkID);
    String target = link.getTarget();
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(target);
    Node targNode = rcx.getCurrentGenome().getNode(target);
    INodeRenderer rend = np.getRenderer();
    boolean shared = rend.sharedPadNamespaces();
    int numPads = targNode.getPadCount();
    if ((numPads == 1) && !shared) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Checks that we have pad degrees of freedom
  */ 
  
  public static boolean haveModLinkDOFs(DataAccessContext rcx, Set<String> linkIDs, String ovrKey) {
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID()); 
    DynamicInstanceProxy dip = (owner.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)owner 
                                                                                             : null;          
    NetworkOverlay nov = owner.getNetworkOverlay(ovrKey);
    //
    // If none of the links we are dealing with is a feedback, we are cool:
    //
    Iterator<String> lit = linkIDs.iterator();
    boolean noFeedBacks = true;
    String linkSrc = null;
    while (lit.hasNext()) {
      String linkID = lit.next();
      NetModuleLinkage link = nov.getLinkage(linkID);
      linkSrc = link.getSource();
      if (linkSrc.equals(link.getTarget())) {
        noFeedBacks = false;
        break;
      }  
    }
    if (noFeedBacks) {
      return (true);
    }
    NetModule mod = nov.getModule(linkSrc);
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(linkSrc);
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    
    Genome useGenome = rcx.getCurrentGenome();
    if (rcx.currentGenomeIsAnInstance()) {
      GenomeInstance gi = rcx.getCurrentGenomeAsInstance();
      GenomeInstance rootGI = gi.getVfgParentRoot();
      useGenome = (rootGI == null) ? gi : rootGI;
    }
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    StaticDataAccessContext rcxU = new StaticDataAccessContext(rcx, useGenome);
    int pads =  nmp.getRenderer().getPadCountForModule(dip, useGroups, mod, nmp, rcxU);
    return (pads > 2);
  }
  
  /***************************************************************************
  **
  ** Repair links to keep everybody where they belong.
  */
       
  public static List<String> fixCisModLinks(UndoSupport support,
                                            Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla, 
                                            StaticDataAccessContext dacx, String theGeneID, boolean logFixes) {

    
    TreeSet<String> retval = (logFixes) ? new TreeSet<String>() : null;
    String baseID = GenomeItemInstance.getBaseID(theGeneID);
    DBGenome dbg = dacx.getDBGenome();
    Map<String, DBGeneRegion.LinkAnalysis> cla = gla.get(dbg.getID());
    DBGeneRegion.LinkAnalysis canonls = cla.get(baseID);
    Gene theGene = dbg.getGene(GenomeItemInstance.getBaseID(theGeneID));
    int numPads = theGene.getPadCount();
    int minPad = DBGene.DEFAULT_PAD_COUNT - numPads;
    int maxPad = DBGene.DEFAULT_PAD_COUNT - 1;
    
    //
    // Go through each link, if it has ORPHAN status, we need to get it in the bounds. If it is TRESSPASS status,
    // we need to get it out.
    //
   
    Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (!gi.isRootInstance()) { // Kids handled in the changeLinkTargetCore
        continue;
      }
      Map<String, DBGeneRegion.LinkAnalysis> glaI = gla.get(gi.getID()); 
      for (String geneID : glaI.keySet()) {
        if (!GenomeItemInstance.getBaseID(geneID).equals(baseID)) { // only care about instances of base gene
          continue;
        }
        StaticDataAccessContext daci = new StaticDataAccessContext(dacx, gi.getID());
        DBGeneRegion.LinkAnalysis lsi = glaI.get(geneID); 
        arrange(support, daci, lsi, canonls, minPad, maxPad, gi, retval);
      }
    }
    return ((retval == null) ? null : new ArrayList<String>(retval));
  }
  

  /***************************************************************************
  **
  ** Assign orphan links into the module based on the newly drawn canonical locations. Toss tresspassers out
  ** into non-module pads, preferably retaining relative order. If there is pileup due to cross region links,
  ** try to use empty space first before doubling up on pads.
  */
       
  public static void arrange(UndoSupport support, StaticDataAccessContext daci,
                             DBGeneRegion.LinkAnalysis lsiToFix,
                             DBGeneRegion.LinkAnalysis canonls, 
                             int minPad, int maxPad, GenomeInstance gi, SortedSet<String> fixLog) {
    
    //
    // Let's find where we have empty space!
    //
    
    ResourceManager rMan = daci.getRMan();
    SortedSet<Integer> freePads = new TreeSet<Integer>();
    freePads.add(Integer.valueOf(minPad));
    freePads.add(Integer.valueOf(maxPad));
    freePads = DataUtil.fillOutHourly(freePads);
    freePads.removeAll(canonls.padToReg.keySet());
    for (String lsikey : lsiToFix.status.keySet()) {
      DBGeneRegion.LinkModStatus lms = lsiToFix.status.get(lsikey);
      if (lms == DBGeneRegion.LinkModStatus.NON_MODULE) {
        int land = gi.getLinkage(lsikey).getLandingPad();
        freePads.remove(Integer.valueOf(land));
      }
    }
    
    //
    // If user has covered the entire gene with modules, then all the inbound links will be in modules
    // and we don't have to find a place outside a module for them to go. So we must have free pads if
    // we need them! We can only have trespassers if they are not in modules; links belonging in other
    // modules will already have a home.
    //
    
    boolean firstTime = true;
    Integer lastPad = null;
    
    for (String lsikey : lsiToFix.status.keySet()) {
      DBGeneRegion.LinkModStatus lms = lsiToFix.status.get(lsikey);
      if (lms == DBGeneRegion.LinkModStatus.ORPHANED) {
        DBGeneRegion.PadOffset poffi = canonls.offsets.get(GenomeItemInstance.getBaseID(lsikey));
        DBGeneRegion reg = canonls.keyToReg.get(poffi.regKey);
        int newPadi = reg.getStartPad() + poffi.offset;
        if (newPadi > reg.getEndPad()) {
          newPadi = reg.getEndPad();
        }       
        if (fixLog != null) {
          Linkage link = gi.getLinkage(lsikey);
          String targ = gi.getNode(link.getTarget()).getDisplayString(gi, true);
          String src = gi.getNode(link.getSource()).getDisplayString(gi, true);
          String regName = reg.getName();
          String msg = MessageFormat.format(rMan.getString("geneRegLinkFixup.orphaned"), new Object[] {src, targ, regName});  
          fixLog.add(msg);   
        }
        LinkSupport.changeLinkTargetCore(newPadi, daci, support, lsikey);
        
      } else if (lms == DBGeneRegion.LinkModStatus.TRESSPASS) {
        DBGeneRegion.PadOffset poffi = canonls.offsets.get(GenomeItemInstance.getBaseID(lsikey));
        if (poffi != null) {
          DBGeneRegion reg = canonls.keyToReg.get(poffi.regKey);
          int newPadi = reg.getStartPad() + poffi.offset;
          if (newPadi > reg.getEndPad()) {
            newPadi = reg.getEndPad();
          }
          if (fixLog != null) {
            Linkage link = gi.getLinkage(lsikey);
            String targ = gi.getNode(link.getTarget()).getDisplayString(gi, true);
            String src = gi.getNode(link.getSource()).getDisplayString(gi, true);
            String regName = reg.getName();
            DBGeneRegion.DBRegKey key = lsiToFix.padToReg.get(Integer.valueOf(link.getLandingPad()));
            DBGeneRegion fromReg = lsiToFix.keyToReg.get(key);
            String msg = MessageFormat.format(rMan.getString("geneRegLinkFixup.tresspass"), new Object[] {src, targ, fromReg.getName(), regName});  
            fixLog.add(msg);   
           }
           LinkSupport.changeLinkTargetCore(newPadi, daci, support, lsikey);
        } else if (!freePads.isEmpty()) {
          Integer usePad = freePads.first();
          freePads.remove(usePad);
          lastPad = usePad;

          if (fixLog != null) {
            Linkage link = gi.getLinkage(lsikey);
            String targ = gi.getNode(link.getTarget()).getDisplayString(gi, true);
            String src = gi.getNode(link.getSource()).getDisplayString(gi, true);
            DBGeneRegion.DBRegKey key = lsiToFix.padToReg.get(Integer.valueOf(link.getLandingPad()));
            DBGeneRegion fromReg = lsiToFix.keyToReg.get(key);
            String msg = MessageFormat.format(rMan.getString("geneRegLinkFixup.tresspassBanish"), new Object[] {src, targ, fromReg.getName()});  
            fixLog.add(msg);   
           }
           LinkSupport.changeLinkTargetCore(lastPad, daci, support, lsikey);      
        } else if (firstTime) { 
          throw new IllegalStateException();
        }
        firstTime = false;     
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Map may need to be iterated:
  */
       
  public static DBGeneRegion.DBRegKey transitiveGet(Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> theMap, DBGeneRegion.DBRegKey key) {
    if ((theMap == null) || (theMap.get(key) == null)) {
      return (key);
    }
    
    int count = 1000;
    DBGeneRegion.DBRegKey currKey = key;
    while (true) {
      DBGeneRegion.DBRegKey nextKey = theMap.get(currKey);
      if (nextKey == null) {
        return (currKey);
      }
      currKey = nextKey;
      if (count-- < 0) {
        throw new IllegalStateException();
      }
    } 
  }
  
  
  /***************************************************************************
  **
  ** Figure out pad squeeze map if links can no longer fit into smaller region without compression
  */
       
  private static Map<Integer, Integer> getPadSqueeze(SortedSet<Integer> padUse, int width) {  
    HashMap<Integer, Integer> retval = new HashMap<Integer, Integer>();
    if (padUse.last().intValue() < width) {
       for (Integer pad : padUse) {
         retval.put(pad, pad);
       }
       return (retval);
    } 
    ArrayList<Integer> indexed = new ArrayList<Integer>(padUse);
    int numi = indexed.size();
    int currSlot = width - 1;
    for (int i = numi - 1; i >= 0; i--) {
      Integer daPad = indexed.get(i);
      if (daPad.intValue() >= currSlot) {
        if (currSlot < 0) {
          throw new IllegalStateException();
        }
        retval.put(daPad, Integer.valueOf(currSlot--));
      } else {
        retval.put(daPad, daPad);
        currSlot = daPad - 1;
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Move links to keep everybody where they belong. Used elsewhere
  */
       
  public static void moveCisRegModLinks(UndoSupport support, List<DBGeneRegion> regs, 
                                        Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> o2nMap,
                                        Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla,
                                        Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr,                                       
                                        StaticDataAccessContext dacx, String theGeneID, 
                                        Set<String> skipLinks) {

    String baseID = GenomeItemInstance.getBaseID(theGeneID);
    DBGenome dbg = dacx.getDBGenome();
    Map<String, DBGeneRegion.LinkAnalysis> cla = gla.get(dbg.getID());
    DBGeneRegion.LinkAnalysis ls = cla.get(baseID);
    
    //
    // Map from region key to new region definition. We go through the old->new map if required.
    //
    
    Map<DBGeneRegion.DBRegKey, DBGeneRegion> newRegs = new HashMap<DBGeneRegion.DBRegKey, DBGeneRegion>();
    
    //
    // Allow erosion of blank-named regions to zero unless they have links into them.
    // Insertion and moves. Allow insertion to break up an unnamed region. If drawn, subsumes the links. If moved, can go
    // in middle of the gap, moving gap space to either side. If any links in the gap, they need to be
    // accommodated in the edges.
    //  
    
    // If region was renamed, the key changed (yeah, zat sux). We are provided a map to track this.
    for (DBGeneRegion reg : regs) {
      DBGeneRegion.DBRegKey currRegKey = reg.getKey();
      DBGeneRegion.DBRegKey useKey = transitiveGet(o2nMap, currRegKey);  
      newRegs.put(useKey, reg);
    }
    
    //
    // Go through each link, get its PadOffset, reposition it. If dealing with a deleted link, we skip it.
    //
    Map<DBGeneRegion.DBRegKey, Map<Integer, Integer>> padSqueezes = new HashMap<DBGeneRegion.DBRegKey, Map<Integer, Integer>>();
    
    for (String lkey : ls.offsets.keySet()) {
      if ((skipLinks != null) && skipLinks.contains(lkey)) {
        continue;
      }
      DBGeneRegion.PadOffset poff = ls.offsets.get(lkey);
      DBGeneRegion reg = newRegs.get(poff.regKey);
      
      if ((reg == null) && (o2nMap != null)) {   
        reg = newRegs.get(o2nMap.get(poff.regKey));
      }  
      
      if (reg == null) {
        // Should NOT Happen...
        continue;
      }
       
      SortedSet<Integer> padUse = lhr.get(poff.regKey);
      
      Map<Integer, Integer> padSqueeze = padSqueezes.get(reg.getKey());
      if (padSqueeze == null) {      
        padSqueeze = getPadSqueeze(padUse, reg.getWidth());
        padSqueezes.put(reg.getKey(), padSqueeze);
      }
      
      Integer offsetObj = padSqueeze.get(Integer.valueOf(poff.offset));
      int newPad = reg.getStartPad() + offsetObj.intValue();
      LinkSupport.changeLinkTargetCore(newPad, dacx, support, lkey);
      
      Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (!gi.isRootInstance()) { // Kids handled in the changeLinkTargetCore
          continue;
        }
        Map<String, DBGeneRegion.LinkAnalysis> glaI = gla.get(gi.getID()); 
        for (String geneID : glaI.keySet()) {
          if (!GenomeItemInstance.getBaseID(geneID).equals(baseID)) { // only care about instances of base gene
            continue;
          }
          DBGeneRegion.LinkAnalysis lsi = glaI.get(geneID);
          for (String linkID : lsi.offsets.keySet()) {
            String base = GenomeItemInstance.getBaseID(linkID);
            if (!base.equals(lkey)) { // only care about instances of base link
              continue;
            }
            DBGeneRegion.PadOffset poffi = lsi.offsets.get(linkID);
            Integer offsetObji = padSqueeze.get(Integer.valueOf(poffi.offset));
            int newPadi = reg.getStartPad() + offsetObji.intValue();
            StaticDataAccessContext daci = new StaticDataAccessContext(dacx, gi.getID());
            LinkSupport.changeLinkTargetCore(newPadi, daci, support, linkID);  
          }
        }
      }
    }
    return;
  }
}
