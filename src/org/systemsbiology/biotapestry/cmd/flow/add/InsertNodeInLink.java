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

package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveLinkage;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.GroupMembership;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.Layout.PadNeedsForLayout;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NodeInsertionDirective;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle inserting a node into a link
*/

public class InsertNodeInLink extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean doGene_;

  public static final int STOP_PROCESSING = 0;
  public static final int NO_AMBIGUITY    = 1;
  public static final int USE_SOURCE      = 2;
  public static final int USE_TARGET      = 3;  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public InsertNodeInLink(boolean doGene) {
    doGene_ = doGene;
    name = (doGene) ? "linkPopup.insertGene" : "linkPopup.insertNode";
    desc = (doGene) ? "linkPopup.insertGene" : "linkPopup.insertNode";
    mnem = (doGene) ? "linkPopup.insertGeneMnem" : "linkPopup.insertNodeMnem";             
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!isSingleSeg || !canSplit) {
      return (false);
    }
    return (rcx.currentGenomeIsRootDBGenome());
  }

  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    StepState ans = (StepState)cms;
    if (qbom.getLabel().equals("queBombNameMatch")) {
      return (ans.queBombNameMatch(qbom));
    } else if (qbom.getLabel().equals("queBombNameMatchForGeneCreate")) {
      return (ans.queBombNameMatchForGeneCreate(qbom));
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new StepState(doGene_, dacx));  
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalArgumentException();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();      
        } else if (ans.getNextStep().equals("stepGenNodeInfoDialog")) {
          next = ans.stepGenNodeInfoDialog();      
        } else if (ans.getNextStep().equals("stepBuildNodeCreationInfo")) {
          next = ans.stepBuildNodeCreationInfo(last);           
        } else if (ans.getNextStep().equals("stepDoTheInsertion")) {
          next = ans.stepDoTheInsertion();      
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
  ** Add a new node inserted into a linkage.
  */  
 
  private static void addNewNodeInsertedIntoLinkage(Node legacyNodeField, StaticDataAccessContext rcxR,
                                              boolean doGene, int resolution, Set<String> resolved, 
                                              int x, int y, Intersection intersect,
                                              UndoSupport support, Map<String, Set<String>> neededLinksPerGenomeInstance,
                                              BTProgressMonitor monitor,
                                              double startFrac, double endFrac) 
                                              throws AsynchExitRequestException {   

    ArrayList<DirectFixupInfo> directFixups = new ArrayList<DirectFixupInfo>();
    HashMap<String, String> preMidLinks = new HashMap<String, String>();
    HashMap<String, String> postMidLinks = new HashMap<String, String>();
    LinkSegmentID oneSeg = intersect.segmentIDFromIntersect();

    BusProperties bp = rcxR.getCurrentLayout().getLinkProperties(intersect.getObjectID());
    BusProperties origRootLp = bp.clone(); 

    Point2D nodeLoc = new Point2D.Double();
    Node useNode = addNewNodeIntoLinkageForRoot(legacyNodeField, rcxR, doGene, x, y, oneSeg, resolved, bp,
                                                directFixups, preMidLinks, postMidLinks, nodeLoc, support);

    //
    // Insert the node into every root instance that has a corresponding link instance
    // 
    
    HashMap<String, Map<String, String>> linkInstanceToNodePerGenome = new HashMap<String, Map<String, String>>();
    HashMap<String, Map<String, Map<String, String>>> oldToNewForFanOutPerGenome = new HashMap<String, Map<String, Map<String, String>>>();
    HashMap<String, Map<String, Map<String, String>>> oldToNewForFanInPerGenome = new HashMap<String, Map<String, Map<String, String>>>();
    HashSet<String> processed = new HashSet<String>();
    int pCount = 0;
    
    double currProg = startFrac;
 
    Iterator<GenomeInstance> iit = rcxR.getGenomeSource().getInstanceIterator();
    int totalCount = 0;
    int rootCount = 0;
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      totalCount++;
      if (gi.getVfgParent() == null) {
        rootCount++;
      }
    }
    double rootFrac = (rootCount == 0) ? 0.0 : (double)rootCount / (double)totalCount;
    double allRootFrac = ((endFrac - startFrac) * rootFrac);
    double perRootFrac = (rootCount == 0) ? 0.0 : allRootFrac / rootCount;
    double allRootMax = startFrac + allRootFrac;

    Iterator<GenomeInstance> it = rcxR.getGenomeSource().getInstanceIterator();    
    while (it.hasNext()) {
      GenomeInstance gi = it.next();
      pCount++;
      StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxR, gi);
      
      insertNodeInRootInstance(legacyNodeField, rcxR, rcxI, useNode, resolved, 
                               linkInstanceToNodePerGenome,
                               oldToNewForFanOutPerGenome,
                               oldToNewForFanInPerGenome,             
                               neededLinksPerGenomeInstance, processed, directFixups,
                               preMidLinks, postMidLinks, oneSeg, origRootLp, nodeLoc,           
                               doGene, resolution, support);
      currProg += perRootFrac;
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
         }
      }
    } 

    if (monitor != null) {
      monitor.updateProgress((int)(allRootMax * 100.0));
    }    

    //
    // Propagate to all subset models.  FIX ME!  Do progress report...
    //
    
    StaticDataAccessContext pulledOut = rcxR.getContextForRoot();
    addNewNodeInsertedIntoLinkageForSubsets(pulledOut, processed, pCount,
                                            linkInstanceToNodePerGenome,
                                            oldToNewForFanOutPerGenome,
                                            oldToNewForFanInPerGenome,            
                                            support);
    
    // Deletions and direct link fixups:
    
    addNewNodeInsertedIntoLinkageKillAndClean(rcxR, resolved, directFixups, support);

    if (monitor != null) {
      monitor.updateProgress((int)(endFrac * 100.0));
    }
    
    //
    // These no longer make sense:
    //
    
    rcxR.getGenomeSource().clearAllDynamicProxyCaches();
        
    //
    // Finally do relevant change events
    //
    
    //support.addEvent(new ModelChangeEvent(targetGenome_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    //support.addEvent(new LayoutChangeEvent(layout id stuff, LayoutChangeEvent.UNSPECIFIED_CHANGE));    
    
    return; 
  }
    
  /***************************************************************************
  **
  ** Add a new node inserted into a linkage: root model install
  */  
 
  private static Node addNewNodeIntoLinkageForRoot(Node legacyNodeField, StaticDataAccessContext rcxR,
                                            boolean doGene, int x, int y, 
                                            LinkSegmentID oneSeg, Set<String> resolved,
                                            BusProperties bp,
                                            List<DirectFixupInfo> directFixups, 
                                            Map<String, String> preMidLinks, Map<String, String> postMidLinks, Point2D nodeLoc,
                                            UndoSupport support) {

    LinkSegment segGeom = bp.getSegmentGeometryForID(oneSeg, rcxR, true);
    Vector2D travel = segGeom.getRun();
    
    //
    // Add the new node to the root
    //
    
    GenomeChange gc;
    if (doGene) {
      gc = rcxR.getCurrentGenomeAsDBGenome().addGeneWithExistingLabel((Gene)legacyNodeField);
    } else {
      gc = rcxR.getCurrentGenomeAsDBGenome().addNodeWithExistingLabel(legacyNodeField);
    }
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
      support.addEdit(gcc);
    }
    
    INodeRenderer nRend = NodeProperties.buildRenderer(legacyNodeField.getNodeType());
    Point2D insertionPt = new Point2D.Double(x, y);
    UiUtil.forceToGrid(insertionPt, UiUtil.GRID_SIZE);
    NodeInsertionDirective pir = nRend.getInsertionDirective(travel, insertionPt);        
    double xCoord = UiUtil.forceToGridValue(x + pir.offset.getX(), UiUtil.GRID_SIZE);
    double yCoord = UiUtil.forceToGridValue(y + pir.offset.getY(), UiUtil.GRID_SIZE);
    nodeLoc.setLocation(xCoord, yCoord);
    NodeProperties np = new NodeProperties(rcxR.getColorResolver(), legacyNodeField.getNodeType(), 
                                            legacyNodeField.getID(), xCoord, yCoord, false); 
    np.setOrientation(pir.orientation);
    // Match node to link color:
    np.setColor(bp.getColorName());
    if (legacyNodeField.getNodeType() == DBNode.INTERCELL) {  // FIX ME
      np.setSecondColor(bp.getColorName());
    }
    //BOGUS!  FIXME!
    String genomeKey = rcxR.getCurrentGenomeID();
    String loTarg = rcxR.getCurrentLayout().getTarget();
    if (!loTarg.equals(genomeKey)) {
      throw new IllegalStateException();
    }
    Layout.PropChange[] lpc = new Layout.PropChange[1]; 
    NodeProperties newProps = new NodeProperties(np, legacyNodeField.getNodeType());
    lpc[0] = rcxR.getCurrentLayout().setNodeProperties(legacyNodeField.getID(), newProps);
    if (lpc != null) {
      PropChangeCmd pcc = new PropChangeCmd(rcxR, lpc);
      support.addEdit(pcc);
    }        
    
    //
    // For each linkage, we add two new linkages to the root network
    //
     
    String firstLinkID = null;
    Iterator<String> rit = resolved.iterator();
    boolean haveToMid = false;
    while (rit.hasNext()) {
      String linkID = rit.next();
      Linkage rootLink = rcxR.getCurrentGenome().getLinkage(linkID);
      String trgID = rootLink.getTarget();
      String midID = legacyNodeField.getID();
      if (!haveToMid) {
        String srcID = rootLink.getSource();
        firstLinkID = rcxR.getNextKey();
        DBLinkage newLinkage = new DBLinkage(rcxR, null, firstLinkID, srcID, midID, 
                                             Linkage.NONE, pir.landingPad, rootLink.getLaunchPad()); 
        gc = ((DBGenome)rcxR.getCurrentGenome()).addLinkWithExistingLabel(newLinkage);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
          support.addEdit(gcc);
        }
        preMidLinks.put(srcID, firstLinkID);
        haveToMid = true;
      }
      String newLinkID = rcxR.getNextKey();      
      DBLinkage newLinkage2 = new DBLinkage(rcxR, null, newLinkID, midID, trgID, rootLink.getSign(), 
                                            rootLink.getLandingPad(), pir.launchPad);
      String origName = rootLink.getName();
      if ((origName != null) && !origName.trim().equals("")) {
        newLinkage2.setName(origName);
      }
      gc = ((DBGenome)rcxR.getCurrentGenome()).addLinkWithExistingLabel(newLinkage2);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
        support.addEdit(gcc);
      }
      postMidLinks.put(linkID, newLinkID);
    }
    
    HashMap<String, NodeInsertionDirective> directFixupLinks = new HashMap<String, NodeInsertionDirective>();
    Layout.PropChange[] changes = rcxR.getCurrentLayout().supportLinkNodeInsertion(oneSeg, resolved, 
                                                                      legacyNodeField.getID(), 
                                                                      firstLinkID, postMidLinks,
                                                                      rcxR,
                                                                      directFixupLinks, pir);
    directFixups.add(new DirectFixupInfo(rcxR, directFixupLinks));
    support.addEdit(new PropChangeCmd(rcxR, changes));
     
    return (legacyNodeField); 
  }  
  
  /***************************************************************************
  **
  ** Add a new node inserted into a linkage: per root genome instance
  */  
 
  private static void insertNodeInRootInstance(Node legacyNodeField, 
                                        StaticDataAccessContext rcxR, StaticDataAccessContext rcxI,
                                        Node useNode, Set<String> resolved, 
                                        Map<String, Map<String, String>> linkInstanceToNodePerGenome,
                                        Map<String, Map<String, Map<String, String>>> oldToNewForFanOutPerGenome,
                                        Map<String, Map<String, Map<String, String>>> oldToNewForFanInPerGenome,
                                        Map<String, Set<String>> neededLinksPerGenomeInstance,
                                        Set<String> processed, List<DirectFixupInfo> directFixups,
                                        Map<String, String> preMidLinks, Map<String, String> postMidLinks, LinkSegmentID oneSeg,
                                        BusProperties origRootLp, Point2D nodeLoc,           
                                        boolean doGene, int resolution,
                                        UndoSupport support) {
    if (rcxI.getCurrentGenomeAsInstance().getVfgParent() != null) {
      return;
    }
    String giID = rcxI.getCurrentGenomeID();

    //
    // Track which mid node goes with each existing link instance.  Track per
    // genome instance because we use this for subset propagation
    //

    Map<String, String> linkInstanceToNode = linkInstanceToNodePerGenome.get(giID);
    if (linkInstanceToNode == null) {
      linkInstanceToNode = new HashMap<String, String>();
      linkInstanceToNodePerGenome.put(giID, linkInstanceToNode);
    }

    //
    // Inverse map: remember the existing linkages going though each node.
    // Don't need this for propagation.
    //

    HashMap<String, Set<String>> nodeToExistingLinkages = new HashMap<String, Set<String>>();

    //
    // Old to new link map for links outbound from mid node.  Only
    // valid if fan-in is <= 1.
    //

    Map<String, Map<String, String>> oldToNewForFanOut = oldToNewForFanOutPerGenome.get(giID);
    if (oldToNewForFanOut == null) {
      oldToNewForFanOut = new HashMap<String, Map<String, String>>();
      oldToNewForFanOutPerGenome.put(giID, oldToNewForFanOut);
    }
    
    //
    // Old to new for links inbound to mid node.  Valid for fan-out is <=1.
    //
    
    Map<String, Map<String, String>> oldToNewForFanIn = oldToNewForFanInPerGenome.get(giID);
    if (oldToNewForFanIn == null) {
      oldToNewForFanIn = new HashMap<String, Map<String, String>>();
      oldToNewForFanInPerGenome.put(giID, oldToNewForFanIn);
    }    
    
    //
    // Remember links added at this layer for propagation down:
    //
    
    HashMap<String, List<String>> propLinksForMid = new HashMap<String, List<String>>(); 
    Vector2D zeroOffset = new Vector2D(0.0, 0.0);
    
    HashMap<String, NodeInsertionDirective> directFixupLinksForGilo = new HashMap<String, NodeInsertionDirective>();
    

    directFixups.add(new DirectFixupInfo(rcxI, directFixupLinksForGilo));
        
    Map<String, BusProperties.RememberProps> rememberProps = rcxI.getCurrentLayout().buildRememberProps(rcxI);    
    HashMap<String, Map<String, String>> linkBuiltFromSource = new HashMap<String, Map<String, String>>();
    processed.add(giID);
    Iterator<String> resit = resolved.iterator();
    while (resit.hasNext()) {
      String linkID = resit.next();
      insertNodeInRootInstancePerResolvedLink(legacyNodeField, rcxR, rcxI,
                                              linkID, useNode, zeroOffset,
                                              linkInstanceToNode, 
                                              nodeToExistingLinkages,
                                              propLinksForMid,
                                              oldToNewForFanIn,
                                              oldToNewForFanOut, 
                                              linkBuiltFromSource,
                                              preMidLinks, postMidLinks,
                                              doGene, resolution, support);
    }
   
    //
    // Handle layout for new links, going by new nodes.  If fan in is 1, we try
    // to inherit layout from root.  If not, we punt:
    //

    Iterator<String> ntelit = nodeToExistingLinkages.keySet().iterator();
    while (ntelit.hasNext()) {
      String newNodeID = ntelit.next();
      Set<String> existing = nodeToExistingLinkages.get(newNodeID);
      Map<String, String> fanInLinks = oldToNewForFanIn.get(newNodeID);
      Map<String, String> fanOutLinks = oldToNewForFanOut.get(newNodeID);
      int fanIn = (fanInLinks == null) ? 0 : (new HashSet<String>(fanInLinks.values())).size();
      boolean didLayout = false;

      Layout.InheritedLinkNodeInsertionResult result = null;
      if (fanIn == 1) {
        String loFirstLinkID = fanInLinks.values().iterator().next();
        result = rcxI.getCurrentLayout().supportInheritedLinkNodeInsertion(oneSeg, existing,
                                                               origRootLp, nodeLoc,                  
                                                               rcxR, rcxI,
                                                               newNodeID, loFirstLinkID, 
                                                               fanOutLinks, directFixupLinksForGilo);
        if (result != null) {
          support.addEdit(new PropChangeCmd(rcxI, result.changes));
          didLayout = true;
        }
      }

      //
      // Make the needed pad changes:
      //

      if (result != null) {
        InvertedSrcTrg ist = new InvertedSrcTrg(rcxI.getCurrentGenome());
        LinkSupport.specialtyPadChanges(result.padChanges, rcxI, support, ist);
      }

      //
      // Make the node big enough to handle all fan-ins:
      //

      if (fanIn > 1) {
        int currPads = useNode.getPadCount();
        int nodeType = useNode.getNodeType();
        int padInc = DBNode.getPadIncrement(nodeType);
        // no pad inc means it cannot be grown:
        if (padInc != 0) {
          // assume shared launch/landing namespaces (conservative)
          int needed = fanIn + 1;
          int defPad = DBNode.getDefaultPadCount(nodeType);
          if (needed > defPad) {
            needed = defPad + (int)UiUtil.forceToGridValueMax(needed - defPad, padInc);  // modulo pad inc
            if (needed > currPads) { 
              GenomeChange gc;
              if (nodeType == Node.GENE) {        
                 gc = rcxI.getCurrentGenome().changeGeneSize(useNode.getID(), needed);
              } else {
                 gc = rcxI.getCurrentGenome().changeNodeSize(useNode.getID(), needed);
              }
              if (gc != null) {
                GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
                support.addEdit(gcc);
              }
            }
          }
        }
      }

      //
      // Fallback layout if the first pass did not work
      //

      if (!didLayout) {
        HashMap<String, BusProperties.RememberProps> mappedRememberProps;
        if (fanIn == 1) {
          mappedRememberProps = new HashMap<String, BusProperties.RememberProps>();
          // Throw away the mapping via mid node:
          HashMap<String, String> combinedOldToNew = new HashMap<String, String>();
          Iterator<Map<String, String>> vit = oldToNewForFanOut.values().iterator();
          while (vit.hasNext()) {          
            Map<String, String> perNode = vit.next();
            combinedOldToNew.putAll(perNode);
          }                   
          Iterator<String> kit = rememberProps.keySet().iterator();
          while (kit.hasNext()) {
            String linkID = kit.next();
            BusProperties.RememberProps rp = rememberProps.get(linkID);
            rp.remapLinks(combinedOldToNew);
            String mappedID = combinedOldToNew.get(linkID);
            mappedRememberProps.put((mappedID == null)? linkID : mappedID, rp);
          }          
        } else {
          mappedRememberProps = null;
        }
        Layout.PropChange lochange = rcxI.getCurrentLayout().bestFitNodePlacementForInsertion(rcxR.getCurrentLayout(), newNodeID, rcxI);          
        if (lochange != null) {
          support.addEdit(new PropChangeCmd(rcxI, lochange));
        }
        Set<String> needAuto = neededLinksPerGenomeInstance.get(giID);
        if (needAuto == null) {
          needAuto = new HashSet<String>();
          neededLinksPerGenomeInstance.put(giID, needAuto);        
        }
        List<String> plfm = propLinksForMid.get(newNodeID);        
        multiFanInPadFixup(plfm, newNodeID, rcxI, support);
        Iterator<String> plfmit = plfm.iterator();
        while (plfmit.hasNext()) {
          String lid = plfmit.next();
          needAuto.add(lid);
          LayoutLinkSupport.autoAddCrudeLinkProperties(rcxI, lid, support, mappedRememberProps);
        }
      }      
    }

    return; 
  }
  
  /***************************************************************************
  **
  ** Find best node assignment for multi-fan ins.
  */  
 
  private static void multiFanInPadFixup(List<String> linksForNewNode, String newNodeID, StaticDataAccessContext rcx, UndoSupport support) {  
  
    Map<String, PadConstraints> padConstraints = new HashMap<String, PadConstraints>();
    int numL = linksForNewNode.size();
    for (int i = 0; i < numL; i++) {
      String linkID = linksForNewNode.get(i);
      PadConstraints pc = new PadConstraints();
      padConstraints.put(linkID, pc); 
    }  
 
    HashSet<Node> newNodeInstances = new HashSet<Node>();
    Node newNodeInstance = rcx.getCurrentGenome().getNode(newNodeID);
    newNodeInstances.add(newNodeInstance);
    LayoutLinkSupport.wigglePadCore(newNodeInstances.iterator(), rcx, padConstraints, support);
    return;
  }
 
  /***************************************************************************
  **
  ** Add a new node inserted into a linkage: submodel support
  */  
 
  private static void addNewNodeInsertedIntoLinkageForSubsets(StaticDataAccessContext rcx, Set<String> processed, int pCount,
                                                       Map<String, Map<String, String>> linkInstanceToNodePerGenome,
                                                       Map<String, Map<String, Map<String, String>>> oldToNewForFanOutPerGenome,
                                                       Map<String, Map<String, Map<String, String>>> oldToNewForFanInPerGenome,             
                                                       UndoSupport support) {
    //
    // Propagate to all subset models.  Multiple passes needed to handle deep
    // subset hierarchies:
    //
     
    while (processed.size() < pCount) {
      Iterator<GenomeInstance> it = rcx.getGenomeSource().getInstanceIterator();    
      while (it.hasNext()) {
        GenomeInstance git = it.next();
        String myID = git.getID(); 
        if (processed.contains(myID)) {  // Skip what we have done...
          continue;
        }
        GenomeInstance parent = git.getVfgParent();
        String parentID = parent.getID();
        if (!processed.contains(parentID)) { // Skip if parent not done...
          continue;
        }
        StaticDataAccessContext rcxP = new StaticDataAccessContext(rcx, myID);
        GenomeInstance rootInstance = git.getVfgParentRoot();
        String rootParentID = rootInstance.getID();
        processed.add(myID);
        Map<String, String> linkInstanceToNode = linkInstanceToNodePerGenome.get(rootParentID);
        Map<String, Map<String, String>> oldToNewForFanOut = oldToNewForFanOutPerGenome.get(rootParentID);
        Map<String, Map<String, String>> oldToNewForFanIn = oldToNewForFanInPerGenome.get(rootParentID);  
        ArrayList<String> linksToProp = new ArrayList<String>();
        Iterator<Linkage> lit = git.getLinkageIterator();
        while (lit.hasNext()) {
          LinkageInstance li = (LinkageInstance)lit.next();
          String nid = linkInstanceToNode.get(li.getID());
          if (nid != null) {
            if (git.getNode(nid) == null) {
              NodeInstance ni = (NodeInstance)parent.getNode(nid);
              PropagateSupport.addNewNodeToSubsetInstance(rcxP, ni, support);          
            }
            if (oldToNewForFanOut != null) {
              Map<String, String> fanOutMap = oldToNewForFanOut.get(nid);
              if (fanOutMap != null) {
                String newLinkID = fanOutMap.get(li.getID());
                if (newLinkID != null) {
                  linksToProp.add(newLinkID);
                }
              }
            }
            if (oldToNewForFanIn != null) {
              Map<String, String> fanInMap = oldToNewForFanIn.get(nid);
              if (fanInMap != null) {
                String newLinkID = fanInMap.get(li.getID());
                if (newLinkID != null) {
                  linksToProp.add(newLinkID);
                }
              }
            }            
          }
        }
        int numPL = linksToProp.size();
        for (int i = 0; i < numPL; i++) {
          String linkID = linksToProp.get(i);
          LinkageInstance li = (LinkageInstance)parent.getLinkage(linkID);
          PropagateSupport.addNewLinkToSubsetInstance(rcxP, li, support);
        }   
      }
    }
   
    return; 
  } 
  
  /***************************************************************************
  **
  ** Add a new node inserted into a linkage: Deletions and final fixups
  */  
 
  private static void addNewNodeInsertedIntoLinkageKillAndClean(StaticDataAccessContext rcxLT,
                                                         Set<String> resolved, List<DirectFixupInfo> directFixups,
                                                         UndoSupport support) {    
    //
    // Now that we are done using the existing linkages to figure out where
    // to propagate, we destroy them.
    //
    
    RemoveLinkage.deleteLinkSetFromModel(resolved, rcxLT, support);
    
    //
    // Any node that was inserted in a start drop now needs to have a direct link
    // inbound instead.  Do this fixup
    //
    Iterator<DirectFixupInfo> dfit = directFixups.iterator();
    while (dfit.hasNext()) {
      DirectFixupInfo dfi = dfit.next();
      Iterator<String> doit = dfi.doLinks.keySet().iterator();
      while (doit.hasNext()) {
        String linkID = doit.next();
        NodeInsertionDirective nid = dfi.doLinks.get(linkID);
        Layout.PropChange pc = dfi.rcx.getCurrentLayout().makeLinkageDirectForLink(linkID, nid, dfi.rcx);
        if (pc != null) {
          support.addEdit(new PropChangeCmd(dfi.rcx, pc));
        }  
      }
    }
    
    return; 
  }    
 
  /***************************************************************************
  **
  ** Add a new node inserted into a linkage: per root genome instance resolved link
  */  
 
  private static void insertNodeInRootInstancePerResolvedLink(Node legacyNodeField, 
                                                       StaticDataAccessContext rcxR, StaticDataAccessContext rcxI,
                                                       String linkID, Node useNode,
                                                       Vector2D zeroOffset,
                                                       Map<String, String> linkInstanceToNode,
                                                       Map<String, Set<String>> nodeToExistingLinkages,
                                                       Map<String, List<String>> propLinksForMid,
                                                       Map<String, Map<String, String>> oldToNewForFanIn,
                                                       Map<String, Map<String, String>> oldToNewForFanOut,     
                                                       Map<String, Map<String, String>> linkBuiltFromSource,
                                                       Map<String, String> preMidLinks, Map<String, String> postMidLinks,
                                                       boolean doGene, int resolution,
                                                       UndoSupport support) {
  
    ArrayList<LinkageInstance> newLinksToAdd = new ArrayList<LinkageInstance>();
    HashMap<String, Set<Integer>> generatedINums = new HashMap<String, Set<Integer>>();
    HashMap<String, Map<String, String>> linkBuiltToTarget = new HashMap<String, Map<String, String>>();
    Iterator<Linkage> lit = rcxI.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance li = (LinkageInstance)lit.next();
      String liid = li.getID();
      if (!GenomeItemInstance.getBaseID(liid).equals(linkID)) {
        continue;
      }
      //
      // Now have a linkage instance that is derived from the inserted root
      //
      NodeInstance src = (NodeInstance)rcxI.getCurrentGenome().getNode(li.getSource());
      NodeInstance trg = (NodeInstance)rcxI.getCurrentGenome().getNode(li.getTarget());
      GroupMembership groupMemb = 
        (resolution == USE_SOURCE) ? rcxI.getCurrentGenomeAsInstance().getNodeGroupMembership(src) : rcxI.getCurrentGenomeAsInstance().getNodeGroupMembership(trg);
      if (groupMemb.mainGroups.isEmpty()) { // should be caught earlier!
        throw new IllegalStateException();
      }
      
      String grpID = groupMemb.mainGroups.iterator().next();
      Group group = rcxI.getCurrentGenomeAsInstance().getGroup(grpID);
      String nid;
      if (group.instanceIsInGroup(useNode.getID())) {
        Node niFromGroup = group.getInstanceInGroup(useNode.getID(), rcxI.getCurrentGenomeAsInstance());
        nid = niFromGroup.getID();
      } else {
        NodeInstance ni = PropagateSupport.propagateNodeNoLayout(doGene, rcxI, (DBNode)legacyNodeField, group, support, null);          
        nid = ni.getID();
        // Add it to all the subgroups too:          
        Iterator<String> grit = groupMemb.subGroups.iterator();
        while (grit.hasNext()) {
          grpID = grit.next();
          group = rcxI.getCurrentGenomeAsInstance().getGroup(grpID);
          GroupChange grc = group.addMember(new GroupMember(nid), rcxI.getCurrentGenomeID());
          if (grc != null) {
            GroupChangeCmd grcc = new GroupChangeCmd(rcxI, grc);
            support.addEdit(grcc);
          }
        }
        //
        // Now add the layout stuff:
        //
        // Note we will only know the final node location once we figure out links.  Use zero for now.
        
        PropagateSupport.propagateNodeLayoutProps(rcxI, useNode, ni, rcxR.getCurrentLayout(), zeroOffset, support);
      }
      //
      // Track link through node, node for link:
      //
      linkInstanceToNode.put(liid, nid);
      Set<String> existingThruNode = nodeToExistingLinkages.get(nid);
      if (existingThruNode == null) {
        existingThruNode = new HashSet<String>();
        nodeToExistingLinkages.put(nid, existingThruNode);
      }
      existingThruNode.add(liid);

      //
      // New mid-node is now down for the linkage.  Propagate the linkage itself.  Both
      // halves are only built if we haven't already built them.
      //

      Linkage rootLink = rcxR.getCurrentGenome().getLinkage(linkID);
      String midID = linkInstanceToNode.get(liid);
      String srcID = src.getID();
      String trgID = trg.getID();
      NodeInstance mid = (NodeInstance)rcxI.getCurrentGenome().getNode(midID);
      String preLinkID = preMidLinks.get(rootLink.getSource());
      DBLinkage preLink = (DBLinkage)rcxR.getCurrentGenome().getLinkage(preLinkID);    
      String postLinkID = postMidLinks.get(linkID);
      DBLinkage postLink = (DBLinkage)rcxR.getCurrentGenome().getLinkage(postLinkID);
      List<String> propLinks = propLinksForMid.get(midID);
      if (propLinks == null) {
        propLinks = new ArrayList<String>();
        propLinksForMid.put(midID, propLinks);
      }
      Map<String, String> fanInLinks = oldToNewForFanIn.get(midID);
      if (fanInLinks == null) {
        fanInLinks = new HashMap<String, String>();
        oldToNewForFanIn.put(midID, fanInLinks);
      }        
      Map<String, String> fanOutLinks = oldToNewForFanOut.get(midID);
      if (fanOutLinks == null) {
        fanOutLinks = new HashMap<String, String>();
        oldToNewForFanOut.put(midID, fanOutLinks);
      }          
      //
      // Need to keep from creating duplicate links to and from the midNode:
      // Using these maps to track all the original linkID->single new linkID cases
      //

      Map<String, String> lbfs = linkBuiltFromSource.get(midID);
      if (lbfs == null) {
        lbfs = new HashMap<String, String>();
        linkBuiltFromSource.put(midID, lbfs);
      }
       
      Map<String, String> lbtt = linkBuiltToTarget.get(midID);
      if (lbtt == null) {
        lbtt = new HashMap<String, String>();
        linkBuiltToTarget.put(midID, lbtt);
      }          

      //
      // Create the links if they are fresh.  Since we are iterating links, we
      // don't actually add them until we finish iterating.  Need to accumulate
      // new instance IDs to keep from having duplicate instance numbers since
      // they are not yet added:
      //

      if (!lbfs.keySet().contains(srcID)) {
        Set<Integer> excludeSet = generatedINums.get(preLinkID);
        if (excludeSet == null) {
          excludeSet = new HashSet<Integer>();
          generatedINums.put(preLinkID, excludeSet);
        }
        int instanceCount = rcxI.getCurrentGenomeAsInstance().getNextLinkInstanceNumberWithExclusion(preLinkID, excludeSet);
        excludeSet.add(new Integer(instanceCount));
        int srcInstance = GenomeItemInstance.getInstanceID(src.getInstance());
        int midInstance = GenomeItemInstance.getInstanceID(mid.getInstance());                        
        LinkageInstance newLink = new LinkageInstance(rcxI, preLink, instanceCount, srcInstance, midInstance);
        newLink.setLaunchPad(li.getLaunchPad());
        propLinks.add(newLink.getID());
        newLinksToAdd.add(newLink);  // can't add yet; we are iterating
        lbfs.put(srcID, newLink.getID());
        fanInLinks.put(liid, newLink.getID());
      } else {
        fanInLinks.put(liid, lbfs.get(srcID));
      }
      if (!lbtt.keySet().contains(trgID)) {
        Set<Integer> excludeSet = generatedINums.get(postLinkID);
        if (excludeSet == null) {
          excludeSet = new HashSet<Integer>();
          generatedINums.put(postLinkID, excludeSet);
        }          
        int instanceCount = rcxI.getCurrentGenomeAsInstance().getNextLinkInstanceNumberWithExclusion(postLinkID, excludeSet);
        excludeSet.add(new Integer(instanceCount));
        int midInstance = GenomeItemInstance.getInstanceID(mid.getInstance());
        int trgInstance = GenomeItemInstance.getInstanceID(trg.getInstance());
        LinkageInstance newLink = new LinkageInstance(rcxI, postLink, instanceCount, midInstance, trgInstance);
        newLink.setLandingPad(li.getLandingPad());
        propLinks.add(newLink.getID());
        newLinksToAdd.add(newLink); // can't add yet; we are iterating
        lbtt.put(trgID, newLink.getID());
        fanOutLinks.put(liid, newLink.getID());
      } else {
        fanOutLinks.put(liid, lbtt.get(trgID));
      }
    } // for each matching inherited link

    // Do this after we have iterated through the existing links

    int numToAdd = newLinksToAdd.size();
    for (int i = 0; i < numToAdd; i++) {        
      LinkageInstance newLink = newLinksToAdd.get(i);
      GenomeChange gc = rcxI.getCurrentGenomeAsInstance().addLinkage(newLink);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
        support.addEdit(gcc);
      }
    }
  
    return; 
  }  
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupPointCmdState, BackgroundWorkerOwner {
    
    private boolean myDoGene_;
    private Intersection inter_;
    private AddNodeSupport ans_;
    private Point pt_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doGene, StaticDataAccessContext dacx) {
      super(dacx);
      myDoGene_ = doGene;
      ans_ = new AddNodeSupport(doGene, uics_, dacx_);
      nextStep_ = "stepBiWarning";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      inter_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setPopupPoint(Point2D ptp) {
      pt_ = new Point();
      uics_.getZoomTarget().transformClick(ptp.getX(), ptp.getY(), pt_);
      return;
    }
       
    /***************************************************************************
    **
    ** Process a QueBomb
    */
  
    public RemoteRequest.Result queBombNameMatch(RemoteRequest qbom) {
      return (ans_.queBombNameMatch(qbom));   
    }
    
    /***************************************************************************
    **
    ** Process a QueBomb
    */
   
    public RemoteRequest.Result queBombNameMatchForGeneCreate(RemoteRequest qbom) {
     return (ans_.queBombNameMatchForGeneCreate(qbom));   
    } 
    
    
    /***************************************************************************
    **
    ** Warn of build instructions
    */
      
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }    
      nextStep_ = "stepGenNodeInfoDialog";
      return (daipc);     
    }
   
    /***************************************************************************
    **
    ** Generate dialog to get node info from user
    */
         
    private DialogAndInProcessCmd stepGenNodeInfoDialog() {   
      TaggedSet currentNetMods = dacx_.getOSO().getCurrentNetModules();  
      DialogAndInProcessCmd daipc = ans_.getRootCreationDialog(cfh_, dacx_, currentNetMods.set, this);
      nextStep_ = "stepBuildNodeCreationInfo";
      return (daipc);
    }
   
    /***************************************************************************
    **
    ** Step 2: Build node creation info 
    */
    
    private DialogAndInProcessCmd stepBuildNodeCreationInfo(DialogAndInProcessCmd lastDaipc) {       
      ans_.extractNewRootNodeInfo(lastDaipc);
      if (ans_.getNci() == null) {
        ans_.clearCandidate();
      } else {
        ans_.createNewNodeForRoot(myDoGene_);
      }
      nextStep_ = "stepDoTheInsertion";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));
    }
  
    /***************************************************************************
    **
    ** Finish the job
    */ 
       
    private DialogAndInProcessCmd stepDoTheInsertion() {

      String undoStr = (myDoGene_) ? "undo.insertGeneInLink" : "undo.insertNodeInLink";
      //
      // FIX ME:  It could be argued that inserting a node into a link that is within
      // a single module should automatically add it to the module.  At the moment,
      // module addition is NOT handled
      //
      
      HashSet<String> resolved = new HashSet<String>();
      int resolution = addNewNodeInsertedIntoLinkageForegroundPrep(inter_, resolved, dacx_);           
      if (resolution == STOP_PROCESSING) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
        
      UndoSupport support = uFac_.provideUndoSupport(undoStr, dacx_);          
      uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);
        
      //
      // We may need to do lots of link relayout operations.  This MUST occur on a background
      // thread!
      //
      NodeInsertionRunner runner = 
         new NodeInsertionRunner(ans_.getNewNode(), dacx_, myDoGene_, resolution, resolved, inter_, pt_, support);
      BackgroundWorkerClient bwc;   
      if (!uics_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(uics_, dacx_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(uics_, dacx_, this, runner, support);
      }
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done
      return (daipc);
    }
      
    /***************************************************************************
    **
    ** Add a new node inserted into a linkage: foreground thread prep work
    */  
   
    private int addNewNodeInsertedIntoLinkageForegroundPrep(Intersection intersect, 
                                                            Set<String> resolved, StaticDataAccessContext rcx) {   
      LinkSegmentID oneSeg = intersect.segmentIDFromIntersect();
  
      //
      // Do the insertion on the root model:
      //
      
      BusProperties bp = rcx.getCurrentLayout().getLinkProperties(intersect.getObjectID());
      resolved.addAll(bp.resolveLinkagesThroughSegment(oneSeg));
      
      //
      // Figure out if we have ambigity:
      //
      
      int resolution = resolveNewGroupMembership(uics_, resolved, rcx);
      return (resolution);
    }
    
    /***************************************************************************
    **
    ** Figure out which region to send the new node to, if there is ambiguity:
    */  
   
    private int resolveNewGroupMembership(UIComponentSource uics, Set<String> linkSet, StaticDataAccessContext rcx) { 
      Iterator<GenomeInstance> it = rcx.getGenomeSource().getInstanceIterator();    
      while (it.hasNext()) {
        GenomeInstance gi = it.next();
        if (gi.getVfgParent() != null) {  // Look only at root instances
          continue;
        }
        Iterator<Linkage> lit = gi.getLinkageIterator();
        while (lit.hasNext()) {
          LinkageInstance li = (LinkageInstance)lit.next();
          String liid = li.getID();
          if (!linkSet.contains(GenomeItemInstance.getBaseID(liid))) {
            continue;
          }
          //
          // Now have a linkage instance that is derived from the inserted root
          //
          NodeInstance src = (NodeInstance)gi.getNode(li.getSource());
          NodeInstance trg = (NodeInstance)gi.getNode(li.getTarget());
          GroupMembership srcGroupMemb = gi.getNodeGroupMembership(src);        
          GroupMembership trgGroupMemb = gi.getNodeGroupMembership(trg);
          if ((srcGroupMemb.mainGroups.size() != 1) || (trgGroupMemb.mainGroups.size() != 1)) {
            JOptionPane.showMessageDialog(uics.getTopFrame(), rcx.getRMan().getString("insertNode.groupConfusion"), 
                                          rcx.getRMan().getString("insertNode.groupConfusionTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (STOP_PROCESSING);
          }
          String srcGrpID = srcGroupMemb.mainGroups.iterator().next();
          String trgGrpID = trgGroupMemb.mainGroups.iterator().next();       
          if (!srcGrpID.equals(trgGrpID)) {
            int result = JOptionPane.showOptionDialog(uics.getTopFrame(), rcx.getRMan().getString("insertNode.chooseRegionDestination"),
                                                      rcx.getRMan().getString("insertNode.chooseRegionDestinationTitle"),
                                                      JOptionPane.DEFAULT_OPTION, 
                                                      JOptionPane.QUESTION_MESSAGE, 
                                                      null, new Object[] {
                                                        rcx.getRMan().getString("insertNode.source"),
                                                        rcx.getRMan().getString("insertNode.target"),
                                                        rcx.getRMan().getString("dialogs.cancel"),        
                                                      }, rcx.getRMan().getString("insertNode.target"));
            if (result == JOptionPane.CLOSED_OPTION) {
              return (STOP_PROCESSING);
            } else if (result == 0) {
              return (USE_SOURCE);
            } else if (result == 1) {
              return (USE_TARGET);
            } else {
              return (STOP_PROCESSING);
            } 
          }
        }
      }
      return (NO_AMBIGUITY);
    }
    
    /***************************************************************************
    **
    ** We can do a background thread in the desktop version
    ** 
    */
  
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }        
   
    public void cleanUpPreEnable(Object result) {
      return;
    }
   
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(uics_, dacx_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    } 
    
    public void handleCancellation() {
      return;
    }   
  }
  
  /***************************************************************************
  **
  ** Background multi-layout link runner
  */ 
    
  private static class NodeInsertionRunner extends BackgroundWorker {
    
    private UndoSupport support_;
    private ArrayList<LayoutLinkSupport.GlobalLinkRequest> requestList_;
    private int resolution_;
    private Set<String> resolved_;
    private Point pt_;
    private Intersection intersected_;
    private boolean doGene_;
    private Node legacyNodeField_; 
    private StaticDataAccessContext rcx_;
        
    public NodeInsertionRunner(Node legacyNodeField, StaticDataAccessContext rcx, 
                               boolean doGene, int resolution, Set<String> resolved, 
                               Intersection intersected, Point pt, UndoSupport support) {
      super(new LinkRouter.RoutingResult());
      support_ = support;
      requestList_ = new ArrayList<LayoutLinkSupport.GlobalLinkRequest>();
      pt_ = pt;
      intersected_ = intersected;
      doGene_ = doGene;
      resolved_ = resolved;
      resolution_ = resolution;
      legacyNodeField_ = legacyNodeField;
      rcx_ = rcx;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      
      HashMap<String, Set<String>> neededLinksPerGenomeInstance = new HashMap<String, Set<String>>();
      Map<String, PadNeedsForLayout> globalPadNeeds = rcx_.getFGHO().getGlobalNetModuleLinkPadNeeds();
       
      addNewNodeInsertedIntoLinkage(legacyNodeField_, rcx_, doGene_, 
                                    resolution_, resolved_, pt_.x, pt_.y, intersected_, 
                                    support_, neededLinksPerGenomeInstance, this, 0.0, 0.2);      
        
      Iterator<String> nlpgikit = neededLinksPerGenomeInstance.keySet().iterator();
      while (nlpgikit.hasNext()) {
        String giID = nlpgikit.next();
        LayoutLinkSupport.GlobalLinkRequest glr = new LayoutLinkSupport.GlobalLinkRequest();
        glr.genome = rcx_.getGenomeSource().getGenome(giID);
        glr.gSrc = rcx_.getGenomeSource();
        glr.layout = rcx_.getLayoutSource().getLayoutForGenomeKey(giID);
        glr.badLinks = neededLinksPerGenomeInstance.get(giID);
        requestList_.add(glr);
      }      
         
      LayoutOptions lopt = new LayoutOptions(rcx_.getLayoutOptMgr().getLayoutOptions());
      LinkRouter.RoutingResult result = LayoutLinkSupport.relayoutLinksGlobally(rcx_, requestList_, support_, lopt, this, 0.2, 1.0);
      ModificationCommands.repairNetModuleLinkPadsGlobally(rcx_, globalPadNeeds, false, support_);
      return (result);
    }
    
    public Object postRunCore() {
      int numReq = requestList_.size();
      for (int i = 0; i < numReq; i++) {
        LayoutLinkSupport.GlobalLinkRequest glr = requestList_.get(i);
        support_.addEvent(new LayoutChangeEvent(glr.layout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      }
      return (null);
    } 
  }
  
  /***************************************************************************
  **
  ** Used for fixup
  */
  
  private static class DirectFixupInfo {    
    StaticDataAccessContext rcx;
    Map<String, NodeInsertionDirective> doLinks;
    
    DirectFixupInfo(StaticDataAccessContext rcx, Map<String, NodeInsertionDirective> doLinks) {
      this.rcx = rcx;
      this.doLinks = doLinks;
    }    
  } 
}
