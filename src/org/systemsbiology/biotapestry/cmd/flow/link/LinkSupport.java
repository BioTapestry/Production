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


package org.systemsbiology.biotapestry.cmd.flow.link;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
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
import org.systemsbiology.biotapestry.util.ResourceManager;
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
  
  public static boolean sourcePadIsClear(BTState appState, Genome genome, String id, int padNum, boolean shared) {
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
        ResourceManager rMan = appState.getRMan();
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("addLink.onlyOneSourcePad"), 
                                      rMan.getString("addLink.placementErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      if (shared && lnk.getTarget().equals(id) && (lnk.getLandingPad() == padNum)) {
        ResourceManager rMan = appState.getRMan();
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
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
  
   public static void specialtyPadChanges(BTState appState, Map<String, PadCalculatorToo.PadResult> padChanges, 
                                          DataAccessContext rcx, // GenomeSource gSrc, String genomeID, 
                                          UndoSupport support, InvertedSrcTrg ist) {
     //
     // Wiggle pad assignments based on node locations:
     //
     
     Iterator<String> pcit = padChanges.keySet().iterator();
     while (pcit.hasNext()) {
       String linkID = pcit.next();
       PadCalculatorToo.PadResult pres = padChanges.get(linkID);
       Linkage link = rcx.getGenome().getLinkage(linkID);
       // Have to make changes to all shared source links! (YUK)
       String src = link.getSource();
       HashSet<String> allLinks = new HashSet<String>();
       Set<String> forSrc = ist.outboundLinkIDs(src);
       Iterator<String> alit = forSrc.iterator();
       while (alit.hasNext()) {
         String nlID = alit.next(); 
         Linkage nextLink = rcx.getGenome().getLinkage(nlID);
         if (pres.launch != nextLink.getLaunchPad()) {
           allLinks.add(nextLink.getID());
         }
       }
       //
       // Starting Version 6, this routine is used for genome instances as well.
       // So we now gotta make sure _all_ subset models are kept consistent!
       //
  
       changeLinkSourceCore(appState, pres.launch, rcx, support, allLinks);
           
       link = rcx.getGenome().getLinkage(linkID);  // may have changed in above operation: handle is stale        
       if (pres.landing != link.getLandingPad()) {      
         changeLinkTargetCore(appState, pres.landing, rcx, support, linkID);
       }
     }
     return;
   }  
 
  /***************************************************************************
  **
  ** Swap link pads
  */  
 
  public static boolean swapLinkPads(BTState appState, Intersection cro, Intersection inter, 
                                     Intersection.PadVal newPad, String id, DataAccessContext rcx, 
                                  //   Layout layout, GenomeSource gSrc, String genomeID,
                                     SwapType swapMode, String myLinkId) {
    
    //
    // Fix for BT-10-28-09:1. Figure out if we need to mess with source/target swap 
    // issues  When doing a target swap, don't swap sources unless pad namespaces
    // are shared.  Same with source swap!
    //
    
    INodeRenderer nodeRenderer = rcx.getLayout().getNodeProperties(id).getRenderer();
    boolean swapNodeShared = nodeRenderer.sharedPadNamespaces();

    //
    // If we are doing a target swap to a target pad, find everybody who lands 
    // there, and swap them all.  If doing a target swap to a source pad, 
    // do for sources.
    //
    
    Linkage myLink = rcx.getGenome().getLinkage(myLinkId);
    int oldPad = (swapMode == SwapType.TARGET_SWAP) ? myLink.getLandingPad() : myLink.getLaunchPad();
       
    HashSet<String> targLinksToSwapForward = new HashSet<String>();
    HashSet<String> targLinksToSwapBack = new HashSet<String>();    
    String aSrcLinkToSwapForward = (swapMode == SwapType.SOURCE_SWAP) ? myLinkId : null;  
    String aSrcLinkToSwapBack = null;      

    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
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
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.swapLinkPads"); 
    
    Iterator<String> tfit = targLinksToSwapForward.iterator();
    while (tfit.hasNext()) {
      String linkID = tfit.next();
      changeLinkTargetCore(appState, newPad.padNum, rcx, support, linkID);      
    }
    Iterator<String> tbit = targLinksToSwapBack.iterator();
    while (tbit.hasNext()) {
      String linkID = tbit.next();
      changeLinkTargetCore(appState, oldPad, rcx, support, linkID);      
    }
    if (aSrcLinkToSwapForward != null) {
      Set<String> allLinks = rcx.getLayout().getSharedItems(aSrcLinkToSwapForward);
      changeLinkSourceCore(appState, newPad.padNum, rcx, support, allLinks);
    }
    if (aSrcLinkToSwapBack != null) {
      Set<String> allLinks = rcx.getLayout().getSharedItems(aSrcLinkToSwapBack);
      changeLinkSourceCore(appState, oldPad, rcx, support, allLinks);
    }    
    support.finish();     
    return (true); 
  }  

  /***************************************************************************
  **
  ** Change the link target. Shared because it is used by a couple different control flows
  */  
 
  public static boolean changeLinkTarget(BTState appState, Intersection cro, Intersection inter, 
                                         List<Intersection.PadVal> padCand, String id, DataAccessContext rcxT) {
    

    int numCand = padCand.size();
    Intersection.PadVal winner = null;
    
    for (int i = 0; i < numCand; i++) {
      Intersection.PadVal pad = padCand.get(i);
      if (!pad.okEnd) {
        continue;
      }  
      if (!targPadIsClear(rcxT.getGenome(), rcxT.getLayout(), id, pad.padNum)) {
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
    
    UndoSupport support = new UndoSupport(appState, "undo.changeLinkTarget");     
    
    //
    // We reject unless the target we are intersecting matches the
    // link we are trying to relocate.
    //

    String lid = cro.getObjectID();  // Remember... this may not be the link we want!
    BusProperties bp = rcxT.getLayout().getLinkProperties(lid);
    LinkSegmentID[] linkIDs = cro.segmentIDsFromIntersect();
    Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);
    String myLinkId = throughSeg.iterator().next();
    Linkage link = rcxT.getGenome().getLinkage(myLinkId);
    if (!link.getTarget().equals(id)) {
      return (false);
    }
        
    changeLinkTargetCore(appState, winner.padNum, rcxT, support, myLinkId);   

    support.finish();     
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Change the link target: core routine
  */  
 
  public static void changeLinkTargetCore(BTState appState, int padNum, DataAccessContext rcx, 
                                          UndoSupport support, String myLinkId) {
    
    Linkage link = rcx.getGenome().getLinkage(myLinkId);
    if (rcx.getGenome() instanceof GenomeInstance) {
      GenomeInstance rootInstance = rcx.getGenomeAsInstance();
      Iterator<GenomeInstance> giit = rcx.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (rootInstance.isAncestor(gi)) { 
          Linkage childLink = gi.getLinkage(myLinkId);
          if (childLink != null) {
            GenomeChange undo = gi.changeLinkageTarget(childLink, padNum);
            if (support != null) {
              support.addEdit(new GenomeChangeCmd(appState, rcx, undo));
              ModelChangeEvent mcev = new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
              support.addEvent(mcev);
            }
          }
        }
      }
    } else {
      GenomeChange undo = rcx.getGenome().changeLinkageTarget(link, padNum);
      if (support != null) {
        support.addEdit(new GenomeChangeCmd(appState, rcx, undo));                
        ModelChangeEvent mcev = new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    }
    return; 
  }  
  
  /***************************************************************************
  **
  ** Change the link source
  */  
 
  public static boolean changeLinkSource(BTState appState, Intersection cro, Intersection inter, 
                                          List<Intersection.PadVal> padCand, String id, DataAccessContext rcx) {
    
    int numCand = padCand.size();
    Intersection.PadVal winner = null;
    
    for (int i = 0; i < numCand; i++) {
      Intersection.PadVal pad = padCand.get(i);
      if (!pad.okStart) {
        continue;
      }  
      if (!sourcePadIsClear(rcx.getGenome(), rcx.getLayout(), id, pad.padNum)) {
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
    Set<String> allLinks = rcx.getLayout().getSharedItems(lid);
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      Linkage nextLink = rcx.getGenome().getLinkage(nextLinkID);
      if (!nextLink.getSource().equals(id)) {
        ResourceManager rMan = appState.getRMan(); 
        JOptionPane.showMessageDialog(appState.getTopFrame(),
                                      rMan.getString("linkSrcChange.useNodeSwitch"),
                                      rMan.getString("linkSrcChange.useNodeSwitchTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState, "undo.changeLinkSource");         
      
    changeLinkSourceCore(appState, winner.padNum, rcx, support, allLinks);    
    support.finish();    
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Change the link source
  */  
 
  public static void changeLinkSourceCore(BTState appState, int padNum, DataAccessContext rcxG,
                                          UndoSupport support, Set<String> allLinks) {
   
    //
    // Have to change source pad of all linkages sharing the start
    //
    
    GenomeInstance rootInstance = null;
    if (rcxG.getGenome() instanceof GenomeInstance) {
      rootInstance = rcxG.getGenomeAsInstance();
    }
    
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      Linkage nextLink = rcxG.getGenome().getLinkage(nextLinkID);
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
                support.addEdit(new GenomeChangeCmd(appState, rcxG, undo));
                ModelChangeEvent mcev = new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
                support.addEvent(mcev);
              }
            }
          }
        }
      } else {
        GenomeChange undo = rcxG.getGenome().changeLinkageSource(nextLink, padNum);
        if (support != null) {
          support.addEdit(new GenomeChangeCmd(appState, rcxG, undo));                
          ModelChangeEvent mcev = new ModelChangeEvent(rcxG.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
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

    String ovrID = rcx.oso.getCurrentOverlay();
    
    if (rcx.getGenome() instanceof GenomeInstance) {
      GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
      if (parent != null) {
        return (false);
      }
    }      
    
    if (forModules) {
      if (type == IS_FOR_TARGET) {
        String targDrop = rcx.getLayout().segmentSynonymousWithModLinkTargetDrop(oid, ids[0], ovrID);
        if (targDrop == null) {
          return (false);
        }
        HashSet<String> allLinks = new HashSet<String>();
        allLinks.add(targDrop);
        if (!haveModLinkDOFs(rcx, allLinks, ovrID)) {
          return (false);
        }
      } else if (type == IS_FOR_SOURCE) {
        Set<String> allLinks = rcx.getLayout().segmentSynonymousWithModLinkStartDrop(oid, ids[0], rcx.oso.getCurrentOverlay());
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
      if (rcx.getLayout().segmentSynonymousWithTargetDrop(oid, ids[0]) == null) {
        return (false);
      }
      if (!haveTargetDOFs(rcx, oid)) {
        return (false);
      }
    } else if (type == IS_FOR_SOURCE) {
      if (!rcx.getLayout().segmentSynonymousWithStartDrop(oid, ids[0])) {
        return (false);
      }
      if (!haveSourceDOFs(rcx, oid)) {
        return (false);
      }
    } else if (type == IS_FOR_SWAP) {
      String linkID = rcx.getLayout().segmentSynonymousWithTargetDrop(oid, ids[0]);
      boolean startOK = rcx.getLayout().segmentSynonymousWithStartDrop(oid, ids[0]);
      if ((linkID == null) && !startOK) {
        return (false);
      }
      
      boolean haveDOF = false;
      if ((linkID != null) && haveTargetDOFs(rcx, oid)) {
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
          Linkage link = rcx.getGenome().getLinkage(linkID);
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
    Linkage link = rcx.getGenome().getLinkage(linkID);
    String source = link.getSource();
    NodeProperties np = rcx.getLayout().getNodeProperties(source);
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
    Linkage link = rcx.getGenome().getLinkage(linkID);
    String target = link.getTarget();
    NodeProperties np = rcx.getLayout().getNodeProperties(target);
    Node targNode = rcx.getGenome().getNode(target);
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
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getGenomeID()); 
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
    NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(linkSrc);
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    
    Genome useGenome = rcx.getGenome();
    if (useGenome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)useGenome;
      GenomeInstance rootGI = gi.getVfgParentRoot();
      useGenome = (rootGI == null) ? gi : rootGI;
    }
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    DataAccessContext rcxU = new DataAccessContext(rcx, useGenome);
    int pads =  nmp.getRenderer().getPadCountForModule(dip, useGroups, mod, ovrKey, nmp, rcxU);
    return (pads > 2);
  } 
}
