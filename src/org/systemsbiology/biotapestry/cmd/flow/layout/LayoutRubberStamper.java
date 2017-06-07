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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.GridElement;
import org.systemsbiology.biotapestry.analysis.GridGrower;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.analysis.PlacementElement;
import org.systemsbiology.biotapestry.analysis.RectangleFitter;
import org.systemsbiology.biotapestry.analysis.TopLeftPacker;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusDrop;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutDataSource;
import org.systemsbiology.biotapestry.ui.LayoutDerivation;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.freerender.LinkageFree;
import org.systemsbiology.biotapestry.ui.layouts.LayoutFailureTracker;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.PatternPlacerSpiral;
import org.systemsbiology.biotapestry.util.PointAndVec;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** A class for propagating shrunken rubber stamped copies of the root layout
** into submodels
*/

public class LayoutRubberStamper {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  
  public enum Approx {
    NO_APPROX_,
    X_APPROX_ ,
    Y_APPROX_;
  };
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private RSData rsd_;
  private ERILData eril_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LayoutRubberStamper() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set rubber stamp data
  */

  public void setRSD(RSData rsd) {
    rsd_ = rsd;
  } 
  
  /***************************************************************************
  **
  ** Set expand data
  */

  public void setERIL(ERILData eril) {
    eril_ = eril;
  }  
    
  /***************************************************************************
  **
  ** Rubber stamp the root layout
  */
  
  public LinkRouter.RoutingResult rubberStampLayout() throws AsynchExitRequestException {

    if (rsd_ == null) {
      throw new IllegalStateException();
    }
    LinkRouter.RoutingResult res;
    boolean strictOKGroups = false;
    //
    // When coming in through CSV load, not keeping a layout tosses out EVERYTHING, so overlay recovery
    // is pointless in that case.  I believe it is useful when rebuilding via dialog. 
    //
    if (!rsd_.keepLayout) {  // i.e. reorganize regions...
      res = freshLayout(strictOKGroups);
    } else {
    //
    // This is the only route used in a CSV load that retains the layout.  Module shape recovery is important
    // here because regions can get shoved around!
    //
      res = incrementalLayout(strictOKGroups);                             
    } 
    
    return (res);
  }
  
  /***************************************************************************
  **
  ** Synchronize to the root layout
  */
  
  public LinkRouter.RoutingResult synchronizeToRootLayout(boolean directCopy, Set<String> targets) throws AsynchExitRequestException {

    if (rsd_ == null) {
      throw new IllegalStateException();
    }
    LinkRouter.RoutingResult res;
    boolean strictOKGroups = false;
    if (directCopy) {
      res = directCopy();
    } else if (!rsd_.keepLayout) {  // i.e. reorganize regions...
      res = freshLayout(strictOKGroups);
    } else {
      res = synchToExistingLayout(targets, strictOKGroups);                             
    }   
    return (res);
  }   
  
  /***************************************************************************
  **
  ** Given a list of LayoutDataSources, this method answers what nodes in the
  ** root genome do not yet have a layout definition
  */
  
  public void undefinedLayout(DataAccessContext rcx, LayoutDerivation ld, Set<String> undefinedNodes, Set<String> undefinedLinks, boolean dropFGOnly) { 
 
    //
    // Build the node map:
    //
    
    Genome genome = rcx.getGenomeSource().getRootDBGenome();
    
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      undefinedNodes.add(node.getID());
    }
   
    int numLds = ld.numDirectives();
    HashMap<String, LayoutDataSource> placedBy = new HashMap<String, LayoutDataSource>();
    HashMap<String, String> placedByInstance = new HashMap<String, String>();
    HashMap<String, Set<String>> usedGIs = new HashMap<String, Set<String>>();
    
    HashMap<String, Point2D> nodePositions = new HashMap<String, Point2D>();
    HashSet<String> allNodeSet = new HashSet<String>();      
    for (int i = 0; i < numLds; i++) {    
      LayoutDataSource lds = ld.getDirective(i);
      Map<String, String> nodeMap = nodesPerDirective(rcx, lds, usedGIs, allNodeSet, placedBy, placedByInstance, null);
      
      //
      // We are not building a new layout, so we need to record the new node locations for
      // the link calculations later:
      //
      String modelID = lds.getModelID();
      Layout lo = rcx.getLayoutSource().getLayoutForGenomeKey(modelID);
      Iterator<String> nmit = nodeMap.keySet().iterator();
      while (nmit.hasNext()) {
        String baseID = nmit.next();
        String nodeID = nodeMap.get(baseID);
        Point2D nodeLoc = lo.getNodeProperties(nodeID).getLocation();
        nodePositions.put(baseID, nodeLoc);
      }
    }
    
    undefinedNodes.removeAll(allNodeSet);
    
    HashSet<String> orphanedLinks = buildOrphans(genome, null);
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      String src = link.getSource();
      String trg = link.getTarget();
      LayoutDataSource slds = placedBy.get(src);
      LayoutDataSource tlds = placedBy.get(trg);
      //
      // Both ends got placed from the same model, so if there is a link instance
      // between the two models, we use it.
      //
      if ((slds != null) && (tlds != null)) {
        if (findFoldInExactMatch(rcx.getGenomeSource(), placedByInstance, slds, tlds, link, src, trg) != null) {
          orphanedLinks.remove(linkID);
          continue;
        }
      }
        
      //
      // Try to find an instance of the link where the source and target nodes in the root have the
      // same relationship as a source and target in an instance
      //
      
      if (findFoldInRelativeMatch(rcx.getGenomeSource(), rcx.getLayoutSource(), null, nodePositions, ld, link, src, trg) != null) {
        orphanedLinks.remove(linkID);
        continue;
      }
    }
    
    //
    // If asked to drop elements only found in top intance, do that now:
    //
    
    if (dropFGOnly) {
      HashSet<String> origUndefinedNodes = new HashSet<String>(undefinedNodes); 
      undefinedNodes.clear();
      Iterator<GenomeInstance> mit = rcx.getGenomeSource().getInstanceIterator();
      while (mit.hasNext()) {
        GenomeInstance gi = mit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }    
        Iterator<String> unit = origUndefinedNodes.iterator();
        while (unit.hasNext()) {
          String udnID = unit.next();
          Set<String> niids = gi.getNodeInstances(udnID);
          if (!niids.isEmpty()) {
            undefinedNodes.add(udnID);
          }
        }
        Iterator<String> olit = orphanedLinks.iterator();
        while (olit.hasNext()) {
          String udlID = olit.next();
          Set<String> liids = gi.returnLinkInstanceIDsForBacking(udlID);
          if (!liids.isEmpty()) {
            undefinedLinks.add(udlID);
          }
        }        
      }
    } else {   
      undefinedLinks.clear();
      undefinedLinks.addAll(orphanedLinks);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Given a LayoutDataSource, this method answers what nodes in the
  ** root genome will be defined by the definition
  */
  
  public Set<String> definedByLayout(LayoutDataSource lds, StaticDataAccessContext rcx) { 
 
    //
    // Build the node map:
    //
    
    HashSet<String> defined = new HashSet<String>();
    Group useGroup = ((GenomeInstance)rcx.getCurrentGenome()).getGroup(lds.getGroupID());
    Iterator<GroupMember> mit = useGroup.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember meb = mit.next();
      String nodeID = meb.getID();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(nodeID);
      if (np == null) {  // for partial layouts...
        continue;
      }        
      defined.add(baseID);
    }

    return (defined);
  }
  
  /***************************************************************************
  **
  ** Uses genome instance layouts to reorganize the root.  Assumes a dbLayoutTransaction
  ** is in effect; support only for model changes
  */
  
  public LinkRouter.RoutingResult upwardCopy(StaticDataAccessContext rcxRt,    
                                             LayoutDerivation ld,
                                             Map<String, BusProperties.RememberProps> rememberMap,
                                             Layout.OverlayKeySet loModKeys, 
                                             Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery,
                                             BTProgressMonitor monitor, 
                                             double startFrac, double maxFrac, UndoSupport support) 
                                             throws AsynchExitRequestException {
                                                                                
    if (monitor != null) {
      if (!monitor.updateProgress((int)(startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }

    //
    // No directives, no layout change:
    //
    
    int numGp = ld.numDirectives();
    if (numGp == 0) {
      if (monitor != null) {
        if (!monitor.updateProgress((int)(maxFrac * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
      return (new LinkRouter.RoutingResult());   
    }
 
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = ld.getOverlayOption();    
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) { 
      rcxRt.getCurrentLayout().convertAllModulesToMemberOnly(loModKeys, rcxRt);
    }
 
    PadCalculatorToo.UpwardPadSyncData upsd = new PadCalculatorToo.UpwardPadSyncData();

    HashMap<String, LayoutDataSource> placedBy = new HashMap<String, LayoutDataSource>();
    HashMap<String, String> placedByInstance = new HashMap<String, String>();
    HashMap<String, Set<String>> usedGIs = new HashMap<String, Set<String>>();
    HashMap<LayoutDataSource, Layout> scaledLayouts = new HashMap<LayoutDataSource, Layout>();
    HashMap<String, String> linkMap = new HashMap<String, String>();  // stays empty
    
    //
    // Make a copy of the root layout before messing with it:
    //
    
    Layout rloCopy = new Layout(rcxRt.getCurrentLayout());

    //
    // Place the nodes in the order specified by the group preferences:
    //
    
    double currProg = startFrac;
    double maxAfterPref = (maxFrac - startFrac) * 0.5;
    double perPrefFrac = (numGp == 0) ? 0.0 : ((maxAfterPref - startFrac) / numGp);     
    
    
    HashSet<String> allNodeSet = new HashSet<String>();      
    for (int i = 0; i < numGp; i++) {    
      LayoutDataSource lds = ld.getDirective(i);
      Map<String, String> nodeMap = nodesPerDirective(rcxRt, lds, usedGIs, allNodeSet, placedBy, placedByInstance, upsd);
      StaticDataAccessContext rcxLDS = new StaticDataAccessContext(rcxRt, lds.getModelID());
      rcxLDS.setLayout(rcxRt.getLayoutSource().getLayoutForGenomeKey(lds.getModelID()));   
      rcxLDS.setLayout(scaleLayout(lds, rcxLDS, scaledLayouts, monitor, currProg, currProg + perPrefFrac));
      //
      // Root layout will have relocated nodes after this:
      //
      // Root layout retains all overlay properties, and has nothing to learn from the instance layout, so
      // we provide triple-null maps for overlay copying directives:
      //
      rcxRt.getCurrentLayout().extractPartialLayout(nodeMap, linkMap, null, null, null, null, false, true, null, rcxRt, rcxLDS);
      
      currProg += perPrefFrac;
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      } 
    }
    
    //
    // Links.  For each link in the root, find out the source and target.  If the two were
    // placed by the same model, we use the linkage instance for those two nodes, if it
    // exists, to layout the link.  If not, we try to use a link instance from the source
    // node.  If that fails...
    //
    
    HashSet<String> orphanedLinks = buildOrphans(rcxRt.getCurrentGenome(), rcxRt.getCurrentLayout());  
    
    Iterator<Linkage> lit = rcxRt.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      LayoutDataSource slds = placedBy.get(src);
      LayoutDataSource tlds = placedBy.get(trg);
      //
      // Both ends got placed from the same model, so if there is a link instance
      // between the two models, we use it.
      //
      if ((slds != null) && (tlds != null)) {
        if (foldInExactMatch(rcxRt, placedByInstance, slds, tlds,
                             scaledLayouts, link, src, trg, orphanedLinks, upsd)) {
          continue;
        }
      }
        
      //
      // Try to find an instance of the link where the source and target nodes in the root have the
      // same relationship as a source and target in an instance
      //
      
      if (foldInRelativeMatch(rcxRt, ld, scaledLayouts, link, src, trg, orphanedLinks, upsd)) {
        continue;
      }
    }
    
    //
    // I think we want to do this in a separate second pass, to give all the imported
    // links the chance to get things organized first:
    //
    
    lit = rcxRt.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (!orphanedLinks.contains(linkID)) {
        continue;
      }
      String src = link.getSource();
      String trg = link.getTarget();
      //
      // If no imports are working, see if the existing root link can serve:
      //     
      recoverFromOldLayout(rcxRt, rloCopy, link, src, trg, orphanedLinks, upsd); 
    }    

    if (monitor != null) {
      if (!monitor.updateProgress((int)(maxAfterPref * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }     
      
    Iterator<String> oit = orphanedLinks.iterator();
    while (oit.hasNext()) {
      String orphanID = oit.next();
      LayoutLinkSupport.autoAddCrudeLinkProperties(rcxRt, orphanID, null, rememberMap);
    }
    
    if (ld.getSwitchPads()) {
      (new SyncSupport(rcxRt)).upwardSyncAllLinkPads((DBGenome)rcxRt.getCurrentGenome(), upsd, support, ld.getForceUnique(), orphanedLinks);
    }
    
    LayoutOptions options = rcxRt.getLayoutOptMgr().getLayoutOptions();
    boolean strictOKGroups = false;
    LayoutLinkSupport.relayoutLinks(rcxRt, null, options, false, 
                                    orphanedLinks, monitor, maxAfterPref, maxFrac, strictOKGroups);

    //
    // Move module shapes around:
    //
    
    if ((moduleShapeRecovery != null) && (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
      rcxRt.getCurrentLayout().shiftModuleShapesPerParams(loModKeys, moduleShapeRecovery, rcxRt);
    }         
    
    //
    // Install the directives in the layout:
    //
    
    LayoutDerivation ldcopy = ld.clone();
    rcxRt.getCurrentLayout().setDerivation(ldcopy);
    
    if (monitor != null) {
      if (!monitor.updateProgress((int)(maxFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }     
    
    return (new LinkRouter.RoutingResult());
  }  

  /***************************************************************************
  **
  ** Expand a group layout for upward copying
  */
  
  public Layout expandGroupForUpwardCopy(StaticDataAccessContext rcx, String groupID,
                                         double fracV, double fracH,
                                         BTProgressMonitor monitor, 
                                         double startFrac, double endFrac) throws AsynchExitRequestException {

    double fullFrac = endFrac - startFrac;
    double frac1 = fullFrac * 0.33;
    double eFrac1 = startFrac + frac1;
    double frac2 = fullFrac * 0.33;
    double eFrac2 = eFrac1 + frac2;
    
    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(rcx, linkColors);                                         
                                                                          
    HashSet<String> allGroups = new HashSet<String>();
    allGroups.add(groupID);   
       
    //
    // Build separate layouts for each group
    //

    HashMap<String, Layout> layouts = new HashMap<String, Layout>();
    HashMap<String, Rectangle> oldRegionBounds = new HashMap<String, Rectangle>();

    // Ignoring overlays:
    buildGroupLayouts(rcx, layouts, oldRegionBounds, allGroups, null,
                      false, false, null, null, null, false, monitor, startFrac, eFrac1);

    StaticDataAccessContext rcx4G = new StaticDataAccessContext(rcx);
    rcx4G.setLayout(layouts.get(groupID));
    TreeSet<Integer> insertRows = new TreeSet<Integer>();
    TreeSet<Integer> insertCols = new TreeSet<Integer>();
    // ignoring overlays:
    rcx4G.getCurrentLayout().chooseExpansionRows(rcx4G, fracV, fracH, null, null, insertRows, insertCols, false, monitor); 
    rcx4G.getCurrentLayout().expand(rcx4G, insertRows, insertCols, 1, false, null, null, null, monitor, eFrac1, eFrac2);
    return (rcx4G.getCurrentLayout());
  }    
  
  /***************************************************************************
  **
  ** Expand the root instance layout
  */
  
  public void expandRootInstanceLayout(double fracV, double fracH) throws AsynchExitRequestException {
    
    eril_.regExp = new StandardExpander(fracV, fracH); 
    eril_.ultimateFailures = null; 
    generalizedExpandRootInstanceLayout(false);  
    return;
  }
  
  /***************************************************************************
  **
  ** Expand the root instance layout, using a customized region expander.  Returns the overall
  ** shift of the network that occurs to line it back up.
  */
  
  public Vector2D generalizedExpandRootInstanceLayout(boolean strictOKGroups) throws AsynchExitRequestException {

    if (eril_ == null) {
      throw new IllegalStateException();
    }
    double fullFrac = eril_.endFrac - eril_.startFrac;
    double frac1 = fullFrac * 0.25;
    double eFrac1 = eril_.startFrac + frac1;
    double frac2 = fullFrac * 0.25;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.25;
    double eFrac3 = eFrac2 + frac3;
    
    //
    // Eliminate flakiness by repairing topology before starting!  Ignore all failure
    // info -> just keep going anyway!
    //
    
    eril_.rcx.getCurrentLayout().repairAllTopology(eril_.rcx, null, eril_.monitor, eril_.startFrac, eril_.startFrac);    
      
    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(eril_.rcx, linkColors);
    
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = eril_.options.overlayOption;    
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) {     
      eril_.rcx.getCurrentLayout().convertAllModulesToMemberOnly(eril_.loModKeys, eril_.rcx);
    }

    //
    // Use ordering to get allGroups and crossTuples:
    //
                                  
    HashSet<String> crossRegion = new HashSet<String>();
    HashSet<String> allGroups = new HashSet<String>();
    HashMap<String, Link> crossTuples = new HashMap<String, Link>();
    doGroupOrdering((GenomeInstance)eril_.rcx.getCurrentGenome(), crossRegion, allGroups, crossTuples); 
        
    //
    // Build list of exemptions:
    //
    
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid exceptionGrid = router.initGrid(eril_.rcx, null, INodeRenderer.STRICT, eril_.monitor);
    Map<String, Set<String>> exempt = exceptionGrid.buildExemptions();        
        
    //
    // Collect up the cross-region link paths:
    //
    
    CrossLinkRecovery recovery = 
      extractRecoveryPathsFromOldLayout(eril_.rcx, crossTuples.keySet(),
                                        null, exempt, eril_.regExp.getChopDeparts(), eril_.regExp.getChopArrives());
       
    //
    // Build separate layouts for each group
    //

    HashMap<String, Layout> layouts = new HashMap<String, Layout>();
    HashMap<String, Rectangle> oldRegionBounds = new HashMap<String, Rectangle>();
    HashMap<String, Rectangle> newRegionBounds = new HashMap<String, Rectangle>();
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts = 
      new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
        
    ClaimedAndUnclaimed cauSlices = buildGroupLayouts(eril_.rcx, layouts, oldRegionBounds, allGroups, eril_.loModKeys, false, 
                                                      false, null, null, moduleLinkFragShifts, true, eril_.monitor, eril_.startFrac, eFrac1);
    String nameOwner = cauSlices.whoOwnName();
    HashMap<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapes = 
      new HashMap<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>>();
     
    //
    // Collect up the cross-region link paths:
    //
    
    buildCrossPathRegionAssociations(eril_.rcx, oldRegionBounds, crossTuples, recovery, null);
      
    eril_.regExp.setup(eril_.rcx, oldRegionBounds, oldToNewShapes, layouts, moduleLinkFragShifts, eril_.loModKeys, recovery, eril_.monitor);
       
    int numG = allGroups.size();
    double fracInc = (numG == 0) ? frac2 : (frac2 / numG);
    double cFrac = eFrac1;    
    Iterator<String> agit = allGroups.iterator();
    while (agit.hasNext()) {
      String grpID = agit.next();
      if ((eril_.groups == null) || eril_.groups.contains(grpID)) {
       eril_.regExp.expandTheRegion(grpID, cFrac,  cFrac + fracInc);
      }
      cFrac += fracInc;
      newRegionBounds.put(grpID, eril_.regExp.getGroupBounds(grpID));
    }
    
    clipGroupLayouts(newRegionBounds, layouts, oldToNewShapes);
    
    //
    // Find out the paddings we need around each region:
    // 
    
    // FIX ME!!!!! BREAKS with single group expansions!!!
    Map<String, Integer> paddings = findNewRegionPaddings(crossTuples, UiUtil.GRID_SIZE_INT, recovery, eril_.groups);    
          
    //
    // Merge separate layouts into one:
    // 
    
    Map<String, BusProperties.RememberProps> rememberProps = eril_.rcx.getCurrentLayout().dropProperties(eril_.rcx);
    // At the moment, if no groups, don't toss ANY module info, since we have no unclaimed slices...
 
    if (!allGroups.isEmpty()) {
      eril_.rcx.getCurrentLayout().reduceToUnclaimedSlices(eril_.loModKeys, cauSlices.unclaimedSlices); 
    }          
    Map<Object, Vector2D> deltas = mergeExpandedGroupLayouts(eril_.rcx.getCurrentLayout(), newRegionBounds, 
                                                             oldRegionBounds, layouts, paddings, 
                                                             null, moduleLinkFragShifts, true, nameOwner);
    eril_.regExp.setDeltas(deltas);
    
    //
    // Relayout the unclaimed slices to fit new layout geometry:
    //
    if ((eril_.loModKeys != null) && !eril_.loModKeys.isEmpty()) {
      NetModuleShapeFixer nmsf = new NetModuleShapeFixer();
      Point2D center = eril_.rcx.getCurrentLayout().getLayoutCenter(eril_.rcx, false, null, null, null, null);

      nmsf.pinModuleShapesForGroups(oldToNewShapes, cauSlices.claimedSlices, cauSlices.unclaimedSlices, deltas, center); 
      if (!allGroups.isEmpty()) {
        eril_.rcx.getCurrentLayout().shiftUnclaimedSlices(eril_.loModKeys, cauSlices.unclaimedSlices); 
      }
    }
 
    //
    // Still need to pin guys that were not expanded.  But they might have shifted, so we
    // need deltas:
    //
   
    if (eril_.groups != null) {
      Iterator<String> lit = allGroups.iterator();
      while (lit.hasNext()) {
        String gpID = lit.next();
        if (!eril_.groups.contains(gpID)) {
          Vector2D delta = deltas.get(gpID);
          pinUnchangedRecoveryPaths(eril_.rcx.getCurrentLayout(), gpID, recovery, delta);
        }
      }
    }
    
    Map<String, LinkPlacementGrid.RecoveryDataForSource> shiftedRecoveryPaths = 
      modifyCrossLinksForGrowthOrShrinkage(eril_.rcx, recovery, newRegionBounds, deltas, paddings);
    
    LinkRouter.RoutingResult res = layoutSomeLinksCore(recovery.recoveredLinks, eril_.rcx, eril_.options,
                                                       linkColors, eril_.monitor, eFrac2, eFrac3, 
                                                       rememberProps, shiftedRecoveryPaths, strictOKGroups);
   
    HashSet<String> remaining = new HashSet<String>(crossTuples.keySet());
    remaining.removeAll(recovery.recoveredLinks);
    
    LayoutFailureTracker.reportFailedLinks(res.failedLinks);
    // Even failed links get crude layout; must be dropped if trying again:
    Iterator<String> fit = res.failedLinks.iterator();
    while (fit.hasNext()) {
      String failID = fit.next();
      eril_.rcx.getCurrentLayout().removeLinkProperties(failID);
    }
    remaining.addAll(res.failedLinks);
    
    if (remaining.size() > 0) {    
      LinkRouter.RoutingResult resF = layoutSomeLinksCore(remaining, eril_.rcx, eril_.options, linkColors, 
                                                          eril_.monitor, eFrac3, eril_.endFrac, rememberProps, null, strictOKGroups);
      if ((eril_.ultimateFailures != null) && !resF.failedLinks.isEmpty()) {
        eril_.ultimateFailures.addAll(resF.failedLinks);
      }
    }
    LayoutFailureTracker.postLayoutReport(eril_.rcx);         
    Layout rootLayout = eril_.rcx.getLayoutSource().getRootLayout();
    // BT-03-01-12:1 This first shift for netmodule link drops is here!
    Vector2D diff = eril_.rcx.getCurrentLayout().alignToLayout(rootLayout, eril_.rcx, false, null, null, eril_.padNeeds);
  //  LayoutFailureTracker.postLayoutReport(gi, layout, frc);
    return (diff);
  }  
  
  
  /***************************************************************************
  **
  ** Compress the root instance layout
  */
  
  public void compressRootInstanceLayout(double fracV, double fracH) throws AsynchExitRequestException {
    
    if ((eril_ == null) || (eril_.regExp != null) || (eril_.ultimateFailures != null)) {
      throw new IllegalStateException();
    }
    
    double fullFrac = eril_.endFrac - eril_.startFrac;
    double frac1 = fullFrac * 0.1;
    double eFrac1 = eril_.startFrac + frac1;
    double frac1a = fullFrac * 0.1;
    double eFrac1a = eFrac1 + frac1a;        
    double frac2 = fullFrac * 0.2;
    double eFrac2 = eFrac1a + frac2;    
    double frac3 = fullFrac * 0.3;
    double eFrac3 = eFrac2 + frac3;
    double frac4 = fullFrac * 0.2;
    double eFrac4 = eFrac3 + frac4;
   
    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(eril_.rcx, linkColors);
    
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = eril_.options.overlayCpexOption;    
    if (overlayOption == NetOverlayProperties.CPEX_LAYOUT_TO_MEMBER_ONLY) {
      eril_.rcx.getCurrentLayout().convertAllModulesToMemberOnly(eril_.loModKeys, eril_.rcx);
    }   
    
    eril_.rcx.getCurrentLayout().dropUselessCorners(eril_.rcx, null, eril_.startFrac, eFrac1, eril_.monitor); 
                                              
    //
    // Use ordering to get allGroups and crossTuples:
    //
                                  
    HashSet<String> crossRegion = new HashSet<String>();
    HashSet<String> allGroups = new HashSet<String>();
    HashMap<String, Link> crossTuples = new HashMap<String, Link>();
    doGroupOrdering((GenomeInstance)eril_.rcx.getCurrentGenome(), crossRegion, allGroups, crossTuples);
      
    //
    // Build list of exemptions (features in the existing layout that would not be
    // allowed in a fresh layout, but which we keep anyway):
    //
    
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid exceptionGrid = router.initGrid(eril_.rcx, null, INodeRenderer.STRICT, eril_.monitor);
    Map<String, Set<String>> exempt = exceptionGrid.buildExemptions();    
    
    //
    // Collect up the cross-region link paths:
    //    
       
    CrossLinkRecovery recovery = extractRecoveryPathsFromOldLayout(eril_.rcx, crossTuples.keySet(), null, exempt, null, null);    
    
    //
    // Build separate layouts for each group.  Collect up pieces of overlay modules that lay outside
    // any group.
    //
   
    HashMap<String, Layout> layouts = new HashMap<String, Layout>();
    HashMap<String, Rectangle> oldRegionBounds = new HashMap<String, Rectangle>();
    HashMap<String, Rectangle> newRegionBounds = new HashMap<String, Rectangle>(); 
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts = 
      new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
    
    ClaimedAndUnclaimed cauSlices = buildGroupLayouts(eril_.rcx, layouts, oldRegionBounds, allGroups, eril_.loModKeys, 
                                                      false, false, null, null, moduleLinkFragShifts, true, 
                                                      eril_.monitor, eFrac1, eFrac1a);
    String nameOwner = cauSlices.whoOwnName();
    HashMap<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapes = 
      new HashMap<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>>();
    
    //
    // Collect up the cross-region link paths:
    //
    
    buildCrossPathRegionAssociations(eril_.rcx, oldRegionBounds, crossTuples, recovery, null);
           
    int numG = allGroups.size();
    double fracInc = (numG == 0) ? frac2 : (frac2 / numG);
    double cFrac = eFrac1a;
    Iterator<String> agit = allGroups.iterator();
    while (agit.hasNext()) {
      String grpID = agit.next();
      Group grp = ((GenomeInstance)eril_.rcx.getCurrentGenome()).getGroup(grpID);
      StaticDataAccessContext rcxG = new StaticDataAccessContext(eril_.rcx);
      rcxG.setLayout(layouts.get(grpID)); 
      if ((eril_.groups == null) || eril_.groups.contains(grpID)) {
        TreeSet<Integer> emptyRows = new TreeSet<Integer>();
        TreeSet<Integer> emptyCols = new TreeSet<Integer>();
        Rectangle bounds = oldRegionBounds.get(grpID);
        //
        // Compression selection needs to occur on regions of the whole layout, to account
        // for cross-region links.  Compression occurs on separate group layouts:
        //
        eril_.rcx.getCurrentLayout().chooseCompressionRows(eril_.rcx, fracV, fracH, bounds, false, eril_.loModKeys, emptyRows, emptyCols, eril_.monitor); 
        HashMap<String, Map<String, List<NetModuleProperties.TaggedShape>>> oldToNewShapesPerGroup = 
          new HashMap<String, Map<String, List<NetModuleProperties.TaggedShape>>>();
        oldToNewShapes.put(grpID, oldToNewShapesPerGroup);
       
        rcxG.getCurrentLayout().compress(rcxG, emptyRows, emptyCols, null, oldToNewShapesPerGroup, moduleLinkFragShifts, grpID, eril_.monitor, cFrac, cFrac + fracInc);
        expandOrCompressRecoveryPaths(false, rcxG.getCurrentLayout(), grpID, emptyRows, emptyCols, 1, recovery);
      }
      cFrac += fracInc;
      Rectangle newBounds = rcxG.getCurrentLayout().getLayoutBoundsForGroup(grp, rcxG, true, true);
      newRegionBounds.put(grpID, newBounds);
    }
    
    //
    // Find out the paddings we need around each region:
    // 
    
    Map<String, Integer> paddings = new HashMap<String, Integer>(); //findNewRegionPaddings(crossTuples, UiUtil.GRID_SIZE_INT, recovery);    
            
    //
    // Merge separate layouts into one.  First reduce overlays to member-only modules and unclaimed slices.
    // 
    
    Map<String, BusProperties.RememberProps> rememberProps = eril_.rcx.getCurrentLayout().dropProperties(eril_.rcx);
    // At the moment, if no groups, don't toss ANY module info, since we have no unclaimed slices...
    if (!allGroups.isEmpty()) {
      eril_.rcx.getCurrentLayout().reduceToUnclaimedSlices(eril_.loModKeys, cauSlices.unclaimedSlices); 
    }
    int baseGroupOrder = eril_.rcx.getCurrentLayout().getTopGroupOrder(); 
    HashMap<Object, Vector2D> deltas = new HashMap<Object, Vector2D>();
    agit = allGroups.iterator();
    while (agit.hasNext()) {
      String grpID = agit.next();
      Layout grpLo = layouts.get(grpID);
      Rectangle obounds = oldRegionBounds.get(grpID);      
      Rectangle nbounds = newRegionBounds.get(grpID);
      double delx = obounds.x - nbounds.x;
      double dely = obounds.y - nbounds.y;
      Vector2D delta = new Vector2D(delx, dely);
      boolean doName = ((nameOwner != null) && nameOwner.equals(grpID));
      eril_.rcx.getCurrentLayout().mergeLayoutOfRegion(grpLo, delta, true, doName, baseGroupOrder, moduleLinkFragShifts, grpID);
      deltas.put(grpID, delta);    
    }
        
    //
    // Relayout the unclaimed slices to fit new layout geometry:
    //
    if ((eril_.loModKeys != null) && !eril_.loModKeys.isEmpty()) {
      NetModuleShapeFixer nmsf = new NetModuleShapeFixer();
      Point2D center = eril_.rcx.getCurrentLayout().getLayoutCenter(eril_.rcx, false, null, null, null, null);

      nmsf.pinModuleShapesForGroups(oldToNewShapes, cauSlices.claimedSlices, cauSlices.unclaimedSlices, deltas, center); 
      if (!allGroups.isEmpty()) {
        eril_.rcx.getCurrentLayout().shiftUnclaimedSlices(eril_.loModKeys, cauSlices.unclaimedSlices); 
      }
    }
 
    //
    // Still need to pin guys that were not compressed:
    //
   
    if (eril_.groups != null) {
      Iterator<String> lit = allGroups.iterator();
      while (lit.hasNext()) {
        String gpID = lit.next();
        if (!eril_.groups.contains(gpID)) {
          Vector2D delta = deltas.get(gpID);
          pinUnchangedRecoveryPaths(eril_.rcx.getCurrentLayout(), gpID, recovery, delta);
        }
      }
    }
 
  //  Map deltas = mergeExpandedGroupLayouts(gi, layout, newRegionBounds, 
                                         //  oldRegionBounds, layouts, paddings, null);
    
   
    Map<String, LinkPlacementGrid.RecoveryDataForSource> shiftedRecoveryPaths = 
      modifyCrossLinksForGrowthOrShrinkage(eril_.rcx, recovery, newRegionBounds, deltas, paddings);
    
    //
    // Layout the cross links:
    //

    boolean strictOKGroups = false;
    LinkRouter.RoutingResult res = layoutSomeLinksCore(recovery.recoveredLinks, eril_.rcx, eril_.options,
                                                       linkColors,eril_.monitor, eFrac2, eFrac3, 
                                                       rememberProps, shiftedRecoveryPaths, strictOKGroups); 
    
    Set<String> remaining = new HashSet<String>(crossTuples.keySet());
    remaining.removeAll(recovery.recoveredLinks);
   // Even failed links get crude layout; must be dropped if trying again:
    Iterator<String> fit = res.failedLinks.iterator();
    while (fit.hasNext()) {
      String failID = fit.next();
      eril_.rcx.getCurrentLayout().removeLinkProperties(failID);
    }
    remaining.addAll(res.failedLinks);

    if (remaining.size() > 0) {
      layoutSomeLinksCore(remaining, eril_.rcx, eril_.options, linkColors, 
                         eril_.monitor, eFrac2, eFrac3, rememberProps, null, strictOKGroups);
    }     
   
    //
    // Choose what we can compress.  Note we are treating regions as opaque here!  Only do this
    // if all groups are being compressed!
    //
    
    if ((eril_.groups == null) || eril_.groups.equals(allGroups)) {
      TreeSet<Integer> useEmptyRows = new TreeSet<Integer>();
      TreeSet<Integer> useEmptyCols = new TreeSet<Integer>();
      eril_.rcx.getCurrentLayout().chooseCompressionRows(eril_.rcx, fracV, fracH, null, true, eril_.loModKeys, useEmptyRows, useEmptyCols, eril_.monitor);
      eril_.rcx.getCurrentLayout().compress(eril_.rcx, useEmptyRows, useEmptyCols, null, null, null, null, eril_.monitor, eFrac3, eFrac4);
    }
    
    //
    // Line up with root:
    //
    
    Layout rootLayout = eril_.rcx.getLayoutSource().getRootLayout();
    eril_.rcx.getCurrentLayout().alignToLayout(rootLayout, eril_.rcx, false, null, null, eril_.padNeeds);
    
    return;
  }    

  /***************************************************************************
  **
  ** Answers if the model set can be synchronized by direct copy
  */
  
  public boolean directCopyOptionAllowed(StaticDataAccessContext rcx) {
    Iterator<GenomeInstance> git = rcx.getGenomeSource().getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (!gi.isRootInstance()) {
        continue;
      }
      if (!gi.hasZeroOrOneInstancePerNode()) {
        return (false);
      }
    }
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Look for exact link layout matches
  */
  
  private boolean foldInExactMatch(StaticDataAccessContext rcxR, Map<String, String> placedByInstance,
                                   LayoutDataSource slds, LayoutDataSource tlds, Map<LayoutDataSource, Layout> scaledLayouts, 
                                   Linkage link, String src, String trg, Set<String> orphanedLinks,
                                   PadCalculatorToo.UpwardPadSyncData upsd)  {
     
    FoldDirections fd = findFoldInExactMatch(rcxR.getGenomeSource(), placedByInstance, slds, tlds, link, src, trg);
    String linkID = link.getID();
    if (fd != null) { 
      Layout sslo = scaledLayouts.get(slds);   
      Layout lo = (sslo == null) ? rcxR.getLayoutSource().getLayoutForGenomeKey(slds.getModelID()) : sslo;
      upsd.setForSource(src, fd.giLink.getLaunchPad(), 1); 
      upsd.setForTarget(linkID, trg, fd.giLink.getLandingPad());          
      foldInLinkMatch(rcxR, lo, fd.iid, fd.giLinkSrc, link, src, orphanedLinks);
      return (true);
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Answer if we can fold in an exact match
  */
  
  private FoldDirections findFoldInExactMatch(GenomeSource gSrc, Map<String, String> placedByInstance,
                                              LayoutDataSource slds, LayoutDataSource tlds, 
                                              Linkage link, String src, String trg)  {
 
    String sModelID = slds.getModelID();

    if (!slds.overlayCompatable(tlds)) {
      return (null);
    }
    
    GenomeInstance gi = (GenomeInstance)gSrc.getGenome(sModelID);
    String srcID = placedByInstance.get(src);
    String trgID = placedByInstance.get(trg);
    String linkID = link.getID();
    Set<String> liids = gi.returnLinkInstanceIDsForBacking(linkID);
    Iterator<String> liit = liids.iterator();
    while (liit.hasNext()) {
      String iid = liit.next();
      Linkage giLink = gi.getLinkage(iid);
      String giLinkSrc = giLink.getSource();
      String giLinkTrg = giLink.getTarget();
      if (giLinkSrc.equals(srcID) && giLinkTrg.equals(trgID)) {
        return (new FoldDirections(iid, giLink, giLinkSrc, slds));
      }
    }
    return (null);
  } 
  
  /***************************************************************************
  **
  ** Look for relative link layout matches.  Though the source and target may
  ** have been placed from different kid layouts, if the relative positioning 
  ** is maintained, we can use a link instance from some other model layout
  */
  
  private FoldDirections findFoldInRelativeMatch(GenomeSource gSrc, LayoutSource lSrc, 
                                                 Layout modifiedRootLayout,
                                                 Map<String, Point2D> nodePositions, 
                                                 LayoutDerivation ld,                                   
                                                 Linkage link, String src, String trg)  {


    //
    // Figure out the offset that needs to be accomodated:
    //

    Point2D startLoc;
    Point2D endLoc;
    if (modifiedRootLayout != null) {
      NodeProperties snp = modifiedRootLayout.getNodeProperties(src);
      startLoc = snp.getLocation();
      NodeProperties tnp = modifiedRootLayout.getNodeProperties(trg);
      endLoc = tnp.getLocation();
    } else {
      startLoc = nodePositions.get(src);
      endLoc = nodePositions.get(trg);
      if ((startLoc == null) || (endLoc == null)) {
        return (null);
      }
    }    
    Vector2D rootOffset = new Vector2D(endLoc, startLoc);
    
    //
    // Go through the directives and look for a link instance that
    // matches:
    //
    
    String linkID = link.getID();
    HashSet<String> seenGenomes = new HashSet<String>(); 
    int numld = ld.numDirectives();
    for (int i = 0; i < numld; i++) { 
      LayoutDataSource lds = ld.getDirective(i);
      if (lds.isScaled()) {
        continue;
      }
      String modelID = lds.getModelID();
      
      if (seenGenomes.contains(modelID)) {
        continue;
      }
      seenGenomes.add(modelID);
      GenomeInstance gi = (GenomeInstance)gSrc.getGenome(modelID);
      Layout lo = lSrc.getLayoutForGenomeKey(modelID);
      Set<String> liids = gi.returnLinkInstanceIDsForBacking(linkID);
      Iterator<String> liit = liids.iterator();
      while (liit.hasNext()) {
        String iid = liit.next();
        Linkage giLink = gi.getLinkage(iid);
        String giLinkSrc = giLink.getSource();
        String giLinkTrg = giLink.getTarget();    

        NodeProperties snp = lo.getNodeProperties(giLinkSrc);
        startLoc = snp.getLocation();
        NodeProperties tnp = lo.getNodeProperties(giLinkTrg);
        endLoc = tnp.getLocation();
        Vector2D instanceOffset = new Vector2D(endLoc, startLoc);    
        Vector2D diff = rootOffset.add(instanceOffset.scaled(-1.0));
        if (diff.length() < 1.0E-1) {
          return (new FoldDirections(iid, giLink, giLinkSrc, lds));
        }
      }
    }
        
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Look for relative link layout matches.  Though the source and target may
  ** have been placed from different kid layouts, if the relative positioning 
  ** is maintained, we can use a link instance from some other model layout
  */
  
  private boolean foldInRelativeMatch(StaticDataAccessContext rcxRt,
                                      LayoutDerivation ld, Map<LayoutDataSource, Layout> scaledLayouts,                                    
                                      Linkage link, String src, String trg, Set<String> orphanedLinks,
                                      PadCalculatorToo.UpwardPadSyncData upsd)  {


    FoldDirections fd = findFoldInRelativeMatch(rcxRt.getGenomeSource(), rcxRt.getLayoutSource(), rcxRt.getCurrentLayout(), null, ld, link, src, trg);
    String linkID = link.getID();
    if (fd != null) { 
      Layout slo = scaledLayouts.get(fd.lds);
      Layout lo = (slo == null) ? rcxRt.getLayoutSource().getLayoutForGenomeKey(fd.lds.getModelID()) : slo;
      upsd.setForSource(src, fd.giLink.getLaunchPad(), 1); 
      upsd.setForTarget(linkID, trg, fd.giLink.getLandingPad());          
      foldInLinkMatch(rcxRt, lo, fd.iid, fd.giLinkSrc, link, src, orphanedLinks);
      return (true);
    }
    return (false);    
  }
  
  /***************************************************************************
  **
  ** Recover links from the original layout
  */
  
  private boolean recoverFromOldLayout(StaticDataAccessContext rcx, Layout origLayout,
                                       Linkage link, String src, String trg, Set<String> orphanedLinks,
                                       PadCalculatorToo.UpwardPadSyncData upsd)  {

    String linkID = link.getID();
    NodeProperties osnp = origLayout.getNodeProperties(src);
    NodeProperties otnp = origLayout.getNodeProperties(trg);
    if ((osnp == null) || (otnp == null)) {
      return (false);
    }
    Point2D origStartLoc = osnp.getLocation();
    Point2D origEndLoc = otnp.getLocation();    
 
    NodeProperties nsnp = rcx.getCurrentLayout().getNodeProperties(src);
    Point2D newStartLoc = nsnp.getLocation();
    NodeProperties ntnp = rcx.getCurrentLayout().getNodeProperties(trg);
    Point2D newEndLoc = ntnp.getLocation();
    Vector2D origOffset = new Vector2D(origEndLoc, origStartLoc); 
    Vector2D newOffset = new Vector2D(newEndLoc, newStartLoc);    
    Vector2D diff = origOffset.add(newOffset.scaled(-1.0));
    if (diff.length() > (UiUtil.GRID_SIZE * 3.0)) {
      return (false); 
    }
    
    BusProperties bp = origLayout.getLinkProperties(linkID);
    LinkBusDrop keepDrop = bp.getTargetDrop(linkID);
    BusProperties spTree = new BusProperties(bp, (BusDrop)keepDrop);    
    Vector2D offset = new Vector2D(origStartLoc, newStartLoc); 
    spTree = new BusProperties(spTree, src, linkID, offset);
    if (upsd != null) {
      upsd.setForSource(src, link.getLaunchPad(), 1); 
      upsd.setForTarget(linkID, trg, link.getLandingPad());
    }
   
    rcx.getCurrentLayout().foldInNewProperty(link, spTree, rcx);
    if (orphanedLinks != null) {
      orphanedLinks.remove(linkID);
    }
    return (true);
  }
  
 
  /***************************************************************************
  **
  ** Recover links from the original layout
  */
  
  @SuppressWarnings("unused")
  private List<Point2D> extractPathFromOldLayout(Layout newLayout, Layout origLayout,
                                                 Linkage link, String src, String trg)  {

    String linkID = link.getID();
    NodeProperties osnp = origLayout.getNodeProperties(src);
    NodeProperties otnp = origLayout.getNodeProperties(trg);
    if ((osnp == null) || (otnp == null)) {
      return (null);
    }
    Point2D origStartLoc = osnp.getLocation();
    Point2D origEndLoc = otnp.getLocation();    
 
    NodeProperties nsnp = newLayout.getNodeProperties(src);
    Point2D newStartLoc = nsnp.getLocation();
    NodeProperties ntnp = newLayout.getNodeProperties(trg);
    Point2D newEndLoc = ntnp.getLocation();
    Vector2D origOffset = new Vector2D(origEndLoc, origStartLoc); 
    Vector2D newOffset = new Vector2D(newEndLoc, newStartLoc);    
    Vector2D diff = origOffset.add(newOffset.scaled(-1.0));
    if (diff.length() > (UiUtil.GRID_SIZE * 3.0)) {
      return (null); 
    }
    
    BusProperties bp = origLayout.getLinkProperties(linkID);
    if (bp == null) {
      return (null);
    }
    
    LinkBusDrop keepDrop = bp.getTargetDrop(linkID);
    BusProperties spTree = new BusProperties(bp, (BusDrop)keepDrop);    
    Vector2D offset = new Vector2D(origStartLoc, newStartLoc); 
    spTree = new BusProperties(spTree, src, linkID, offset);
    return (spTree.getPointListForSinglePath());
  }  

  /***************************************************************************
  **
  ** Recover link geometry from the original layout
  */
  
  private CrossLinkRecovery extractRecoveryPathsFromOldLayout(StaticDataAccessContext rcxB, Set<String> todo,
                                                              GenericBPSource bps, Map<String, Set<String>> exemptions, 
                                                              Map<String, Map<String, EdgeMove>> chopDeparts, 
                                                              Map<String, Map<String, EdgeMove>> chopArrives) {
    
    if (bps == null) {
      bps = new GenericBPSource(rcxB.getCurrentLayout());
    }
    Vector2D offset = new Vector2D(0.0, 0.0);
    HashSet<String> recoveredLinks = new HashSet<String>();
    Iterator<String> tdit = todo.iterator();
    HashMap<String, PerSrcData> perSrcMap = new HashMap<String, PerSrcData>();
    while (tdit.hasNext()) {
      String linkID = tdit.next();
      Linkage link = rcxB.getCurrentGenome().getLinkage(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      BusProperties bp = bps.getLinkProperties(linkID);
      if (bp == null) {
        continue;
      }
      if (bp.isDirect()) {
        continue;
      }
      
      NodeProperties osnp = rcxB.getCurrentLayout().getNodeProperties(src);
      NodeProperties otnp = rcxB.getCurrentLayout().getNodeProperties(trg);
      if ((osnp == null) || (otnp == null)) {
        continue;
      }
 
      PerSrcData psd = perSrcMap.get(src);
      if (psd == null) {
        Map<String, EdgeMove> perSrcCD = (chopDeparts == null) ? null : chopDeparts.get(src);
        Map<String, EdgeMove> perSrcCA = (chopArrives == null) ? null : chopArrives.get(src);        
        psd = new PerSrcData(bp, perSrcCD, perSrcCA);
        perSrcMap.put(src, psd);
      }
   
      LinkBusDrop keepDrop = bp.getTargetDrop(linkID);
      BusProperties spTree = new BusProperties(bp, (BusDrop)keepDrop); 
      spTree = new BusProperties(spTree, src, linkID, offset);
      
      PerLinkData pld = psd.perLinkMap.get(linkID);
      if (pld != null) {
        throw new IllegalStateException();
      }
      pld = new PerLinkData(spTree);
      psd.perLinkMap.put(linkID, pld);
      recoveredLinks.add(linkID);
    }
    return (new CrossLinkRecovery(perSrcMap, recoveredLinks, exemptions));
  }
  
  /***************************************************************************
  **
  ** Fold in a link layout match
  */
  
  private void foldInLinkMatch(StaticDataAccessContext rcxRoot, Layout lo,        
                               String iid, String giLinkSrc, 
                               Linkage link, String src, Set<String> orphanedLinks)  {
    
 
    String linkID = link.getID();

    BusProperties bp = lo.getLinkProperties(iid);
    LinkBusDrop keepDrop = bp.getTargetDrop(iid);
    NodeProperties np = lo.getNodeProperties(giLinkSrc);
    NodeProperties rnps = rcxRoot.getCurrentLayout().getNodeProperties(src);
    Point2D startLoc = rnps.getLocation();   
    Vector2D offset = new Vector2D(np.getLocation(), startLoc);       
    BusProperties spTree = new BusProperties(bp, (BusDrop)keepDrop);
    spTree = new BusProperties(spTree, src, linkID, offset);
    
    rcxRoot.getCurrentLayout().foldInNewProperty(link, spTree, rcxRoot);
    orphanedLinks.remove(linkID);
    return;
  }
  
  /***************************************************************************
  **
  ** Detemines node layout sources for a Layout directive
  */
  
  private Map<String, String> nodesPerDirective(DataAccessContext rcx, LayoutDataSource lds,
                                                HashMap<String, Set<String>> usedGIs, HashSet<String> allNodeSet, 
                                                HashMap<String, LayoutDataSource> placedBy,
                                                HashMap<String, String> placedByInstance, PadCalculatorToo.UpwardPadSyncData upsd) {
       
    // Rearrange layout source info for later link use:
    String modelID = lds.getModelID();
    String groupID = lds.getGroupID();
    Set<String> usedGroupsForGI = usedGIs.get(modelID);
    if (usedGroupsForGI == null) {
      usedGroupsForGI = new HashSet<String>();
      usedGIs.put(modelID, usedGroupsForGI);
    }
    usedGroupsForGI.add(groupID);

    GenomeInstance gi = (GenomeInstance)rcx.getGenomeSource().getGenome(modelID);   
    Layout lo = rcx.getLayoutSource().getLayoutForGenomeKey(modelID);

    HashMap<String, String> nodeMap = new HashMap<String, String>(); 
    if (lds.isNodeSpecific()) {  // Hardwired to a set of nodes
      Iterator<String> nit = lds.getNodes();
      while (nit.hasNext()) {
        String nodeID = nit.next();
        String baseID = GenomeItemInstance.getBaseID(nodeID);
        NodeProperties np = lo.getNodeProperties(nodeID);
        if ((np != null) && !allNodeSet.contains(baseID)) {
          allNodeSet.add(baseID);
          nodeMap.put(baseID, nodeID);  // this only lives for this one (outer) loop iteration...
          placedBy.put(baseID, lds);
          placedByInstance.put(baseID, nodeID); // ID only valid in context of model spec'ed by placedBy map.
          // Use this to choose source pad...
          if (upsd != null) {
            Integer srcPad = gi.getSourcePad(nodeID);
            if (srcPad != null) {
              upsd.setForSource(baseID, srcPad.intValue(), Integer.MAX_VALUE);
            }
          }
        }
      }
    } else {
      Group useGroup = gi.getGroup(groupID);
      Iterator<GroupMember> mit = useGroup.getMemberIterator();
      while (mit.hasNext()) {
        GroupMember meb = mit.next();
        String nodeID = meb.getID();
        String baseID = GenomeItemInstance.getBaseID(nodeID);
        NodeProperties np = lo.getNodeProperties(nodeID);
        if (np == null) {  // for partial layouts...
          continue;
        }
        if (!allNodeSet.contains(baseID)) {
          allNodeSet.add(baseID);
          nodeMap.put(baseID, nodeID);
          placedBy.put(baseID, lds);
          placedByInstance.put(baseID, nodeID); // ID only valid in context of model spec'ed by placedBy map.
          // Use this to choose source pad...
          if (upsd != null) {
            Integer srcPad = gi.getSourcePad(nodeID);
            if (srcPad != null) {
              upsd.setForSource(baseID, srcPad.intValue(), Integer.MAX_VALUE);
            }
          }
        }
      }
    }
    return (nodeMap);
  }
  
  /***************************************************************************
  **
  ** Scale layout if needed, else return the original:
  */
  
  private Layout scaleLayout(LayoutDataSource lds, StaticDataAccessContext rcx,
                            Map<LayoutDataSource, Layout> scaledLayouts, BTProgressMonitor monitor, 
                             double startFrac, double maxFrac) throws AsynchExitRequestException {
    
    
    StaticDataAccessContext rcxR = new StaticDataAccessContext(rcx);
    Layout retval = rcx.getCurrentLayout();

    int xOff = lds.getXOffset();
    int yOff = lds.getYOffset();
    int xSca = lds.getXScale();
    int ySca = lds.getYScale();

    if ((xOff != 0) || (yOff != 0)) {
      retval = new Layout(retval);
      rcxR.setLayout(retval);
      rcxR.getCurrentLayout().shiftLayout(new Vector2D(xOff, yOff), null, rcxR);
      scaledLayouts.put(lds, retval);
    }  

    if ((xSca != 0.0) || (ySca != 0.0)) {
      retval = expandGroupForUpwardCopy(rcxR, lds.getGroupID(), xSca, ySca,
                                        monitor, startFrac, maxFrac);
      scaledLayouts.put(lds, retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build orphans, optionally clear out given layout if not null
  */
  
  private HashSet<String> buildOrphans(Genome rootGenome, Layout rootLayout) {   
    HashSet<String> orphanedLinks = new HashSet<String>();
    Iterator<Linkage> lrit = rootGenome.getLinkageIterator();
    while (lrit.hasNext()) {
      Linkage linkR = lrit.next();
      String rootLinkIDR = linkR.getID();
      if (rootLayout != null) {
        rootLayout.removeLinkProperties(rootLinkIDR);
      }
      orphanedLinks.add(rootLinkIDR);
    }
    return (orphanedLinks);
  }
  
  /***************************************************************************
  **
  ** Use direct copy to synchronize layouts
  */
  
  private LinkRouter.RoutingResult directCopy() throws AsynchExitRequestException {
    
    //
    // This is called if we have at most one copy of each node in the child.  (Note this
    // implies we have at most one copy of each link.)
    //
                                                
    if (rsd_.monitor != null) {
      if (!rsd_.monitor.updateProgress((int)(rsd_.startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    } 
    
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = rsd_.options.overlayOption;    
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) {     
      rsd_.instRcx.getCurrentLayout().convertAllModulesToMemberOnly(rsd_.loModKeys, rsd_.instRcx);
    }

    //
    // Build the node map:
    //

    HashMap<String, String> nodeMap = new HashMap<String, String>();    
    Iterator<Node> nit = rsd_.instRcx.getCurrentGenome().getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      NodeProperties np = rsd_.rootRcx.getCurrentLayout().getNodeProperties(baseID);
      if (np == null) {  // for partial layouts...
        continue;
      }
      nodeMap.put(nodeID, baseID);
    }
    
    HashMap<String, String> linkMap = new HashMap<String, String>();
    Iterator<Linkage> glit = rsd_.instRcx.getCurrentGenome().getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String linkID = link.getID();
      String baseID = GenomeItemInstance.getBaseID(linkID);        
      BusProperties lp = rsd_.rootRcx.getCurrentLayout().getLinkProperties(baseID);
      if (lp == null) {  // for partial layouts...
        continue;
      }
      linkMap.put(linkID, baseID);
    }
    
    //
    // Doing downward synching, the instance layout still has all associated overlay 
    // info, so we don't need to make that part of the extraction. Thus, triple 
    // null maps provided for overlay transfer directives:
    //
    
    rsd_.instRcx.getCurrentLayout().extractPartialLayout(nodeMap, linkMap, null,null, null, null, false, true, null, rsd_.instRcx, rsd_.rootRcx);
    
    //
    // Move module shapes around:
    //
    
    if ((rsd_.moduleShapeRecovery != null) && (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
      rsd_.instRcx.getCurrentLayout().shiftModuleShapesPerParams(rsd_.loModKeys, rsd_.moduleShapeRecovery, rsd_.instRcx);
    }     
 
    if (rsd_.monitor != null) {
      if (!rsd_.monitor.updateProgress((int)(rsd_.maxFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }     
    
    return (new LinkRouter.RoutingResult());
  }  
  
 
  /***************************************************************************
  **
  ** Rubber stamp the root layout
  */
  
  private LinkRouter.RoutingResult freshLayout(boolean strictOKGroups) throws AsynchExitRequestException {
                                  
    double fullFrac = rsd_.maxFrac - rsd_.startFrac;
    double frac1 = fullFrac * 0.25;
    double eFrac1 = rsd_.startFrac + frac1;
    double frac2 = fullFrac * 0.25;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.25;
    double eFrac3 = eFrac2 + frac2;

    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(rsd_.rootRcx, linkColors); 
    if ((rsd_.monitor != null) && !rsd_.monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    } 
    
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = rsd_.options.overlayOption;  
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) {     
      rsd_.instRcx.getCurrentLayout().convertAllModulesToMemberOnly(rsd_.loModKeys, rsd_.instRcx);
    }
  
    //
    // For each group in the genome instance, make a layout copy of the
    // root layout that contains just those nodes in the group, plus all
    // root interactions between nodes in the group.  Do a 100% compression
    // on this layout copy.  Then do a topo sort of the resulting groups,
    // creating a final layout that fits these groups together.
    //

    //
    // Topo sort the groups:
    //
                                  
    HashSet<String> crossRegion = new HashSet<String>();
    HashSet<String> allGroups = new HashSet<String>();
    HashMap<String, Link> crossTuples = new HashMap<String, Link>();
    SortedMap<Integer, List<String>> regionsByColumn = doGroupOrdering((GenomeInstance)rsd_.instRcx.getCurrentGenome(), crossRegion, allGroups, crossTuples);
    //
    // Build separate layouts for each:
    //
   
    HashMap<String, Layout> layouts = new HashMap<String, Layout>();
    HashMap<String, Rectangle> loBounds = new HashMap<String, Rectangle>();
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts = 
      new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
    
    // Note these are not squashed even if we are squashing:
    // Extraction of overlay info is pointless going down from root to instance:
    StaticDataAccessContext rcx2m = new StaticDataAccessContext(rsd_.instRcx);
    rcx2m.setLayout(rsd_.rootRcx.getCurrentLayout());
    buildGroupLayouts(rcx2m, layouts, loBounds, allGroups, rsd_.loModKeys, 
                      true, false, null, null, moduleLinkFragShifts, false, rsd_.monitor, rsd_.startFrac, eFrac1);
    StaticDataAccessContext rcxul = new StaticDataAccessContext(rsd_.instRcx);
    rcxul.setLayout((rsd_.options.inheritanceSquash) ? new Layout(rsd_.instRcx.getCurrentLayout()) : rsd_.instRcx.getCurrentLayout());       
    LinkRouter.RoutingResult res = iterativeFreshMerge(rcxul, crossRegion, loBounds, layouts, regionsByColumn,
                                                       rsd_.options, linkColors, rsd_.loModKeys, moduleLinkFragShifts,
                                                       rsd_.monitor, eFrac1, eFrac2, rsd_.rememberProps, strictOKGroups);
                                                           
    
    if (rsd_.options.inheritanceSquash) {

      //
      // Squash groups:
      //

      HashMap<String, Layout> newLayouts = new HashMap<String, Layout>();
      HashMap<String, Rectangle> newLoBounds = new HashMap<String, Rectangle>();
      HashSet<String> oneGroup = new HashSet<String>();
      HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> squModuleLinkFragShifts = 
        new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
      
      int numG = allGroups.size();
      double fracInc = frac3 / numG;
      double cFrac = eFrac2;
      double cfrac1 = fracInc * 0.33;
      double ceFrac1 = cFrac + cfrac1;
      double cfrac2 = fracInc * 0.33;
      double ceFrac2 = ceFrac1 + cfrac2;

      
      Iterator<String> agit = allGroups.iterator();
      while (agit.hasNext()) {
        String grpID = agit.next();
        StaticDataAccessContext rcxss = new StaticDataAccessContext(rsd_.instRcx);
        rcxss.setLayout(new Layout(rcxul.getCurrentLayout())); 
        // Don't want useless corners to mess up the squash:
        rcxss.getCurrentLayout().dropUselessCorners(rcxss, null, cFrac, ceFrac1, rsd_.monitor);
        Rectangle bounds = loBounds.get(grpID);
        boundedSquash(rcxss, bounds, rsd_.monitor, ceFrac1, ceFrac2);
        oneGroup.clear();
        oneGroup.add(grpID);
        // Am keeping overlay info.  Is this what we want?
        buildGroupLayouts(rcxss, newLayouts, newLoBounds, oneGroup, rsd_.loModKeys, 
                          false, false, null, null, squModuleLinkFragShifts,
                          true, rsd_.monitor, ceFrac2, cFrac + fracInc);
        cFrac += fracInc;
      }

      //
      // Second pass at layout.  First pass created an expanded graph.  We then squashed group
      // rectangles.  This can mess up cross-region links, but the squashed core layout is 
      // valid, with allowances for having space for the cross-region links.  We extracted the
      // region layouts above, and merge them again.
      //

      //
      // Merge separate layouts into one:
      // 
      
      StaticDataAccessContext rcxtl = new StaticDataAccessContext(rsd_.instRcx);
      rcxtl.setLayout(new Layout(rsd_.instRcx.getCurrentLayout())); 
      LinkRouter.RoutingResult res2 = iterativeFreshMerge(rcxtl, crossRegion, newLoBounds, newLayouts, regionsByColumn,
                                                          rsd_.options, linkColors, rsd_.loModKeys, moduleLinkFragShifts,
                                                          rsd_.monitor, eFrac3, rsd_.maxFrac, rsd_.rememberProps, strictOKGroups);
      // Don't squash if it fails and the non-squash is OK
      if (((res2.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) ||
          ((res.linkResult & LinkRouter.LAYOUT_PROBLEM) != 0x00)) {
     // if ((res2.linkResult == LinkRouter.LAYOUT_OK) || (res.linkResult != LinkRouter.LAYOUT_OK)) {
        rsd_.instRcx.getCurrentLayout().replaceContents(rcxtl.getCurrentLayout());
      } else {
        rsd_.instRcx.getCurrentLayout().replaceContents(rcxul.getCurrentLayout());       
      }      
    }
    
    //
    // Move module shapes around:
    //
    
    if ((rsd_.moduleShapeRecovery != null) && (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
      rsd_.instRcx.getCurrentLayout().shiftModuleShapesPerParams(rsd_.loModKeys, rsd_.moduleShapeRecovery, rsd_.instRcx);
    } 
    
    Layout.PadNeedsForLayout padNeedsForLayout = rsd_.globalPadNeeds.get(rsd_.instRcx.getCurrentLayoutID());
    rsd_.instRcx.getCurrentLayout().alignToLayout(rsd_.rootRcx.getCurrentLayout(), rsd_.instRcx, false, null, null, padNeedsForLayout);
    
    return (res);
  }
  
  /***************************************************************************
  **
  ** Order the groups
  */
  
  private SortedMap<Integer, List<String>> doGroupOrdering(GenomeInstance gi, Set<String> crossRegion, 
                                                           Set<String> allGroups, Map<String, Link> crossTuples) {
   
    //
    // Need a set of all groups:
    //
  
    Iterator<Group> rit = gi.getGroupIterator();    
    while (rit.hasNext()) {
      Group grp = rit.next();
      if (grp.isASubset(gi)) {
        continue;
      }
      String grpID = grp.getID();
      allGroups.add(grpID);
    }
    
    //
    // Special quick kill: One group? We are done!
    //
    
    if (allGroups.size() == 1) {
      TreeMap<Integer, List<String>> regionsByColumn = new TreeMap<Integer, List<String>>();
      ArrayList<String> forGrp = new ArrayList<String>();
      forGrp.addAll(allGroups);
      regionsByColumn.put(new Integer(0), forGrp);
      return (regionsByColumn);
    }
       
    //
    // Cross-region links will determine topo sort:
    //   
    
    HashMap<String, GenomeInstance.GroupTuple> xrTups = new HashMap<String, GenomeInstance.GroupTuple>();
    crossRegion.addAll(gi.getCrossRegionLinks(xrTups));
    Iterator<String> lit = xrTups.keySet().iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      GenomeInstance.GroupTuple tup = xrTups.get(linkID);
      Link cfl = new Link(tup.getSourceGroup(), tup.getTargetGroup());
      crossTuples.put(linkID, cfl);
    } 
    
    //
    // Topo sort the groups:
    //
    
    SortedMap<Integer, List<String>> regionsByColumn = sortRegions(allGroups, crossTuples);
    return (regionsByColumn);
  }
 

  /***************************************************************************
  **
  ** Do an incremental layout
  */
  
  private LinkRouter.RoutingResult incrementalLayout(boolean strictOKGroups) throws AsynchExitRequestException {
                                   
                                   
    double fullFrac = rsd_.maxFrac - rsd_.startFrac;
    double frac1 = fullFrac * 0.25;
    double eFrac1 = rsd_.startFrac + frac1;
    double frac2 = fullFrac * 0.25;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.25;
    double eFrac3 = eFrac2 + frac2; 

    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = rsd_.options.overlayOption;   
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) {     
      rsd_.instRcx.getCurrentLayout().convertAllModulesToMemberOnly(rsd_.loModKeys, rsd_.instRcx);
    }     
 
    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(rsd_.rootRcx, linkColors);
    buildLinkColorsMap(rsd_.instRcx, linkColors);
    
    HashSet<String> crossRegion = new HashSet<String>();
    HashSet<String> allGroups = new HashSet<String>();
    HashMap<String, Link> crossTuples = new HashMap<String, Link>();
    // Want the link info, but don't care about the ordering....
    doGroupOrdering((GenomeInstance)rsd_.instRcx.getCurrentGenome(), crossRegion, allGroups, crossTuples); 
       
    //
    // Build separate layouts for each region.  Do this for the root layout and the
    // current instance layout
    //
    
    //
    // Some groups may not yet exist in the current instance layout.  Figure out who exists:
    //

    HashMap<String, GroupProperties> gpMemory = new HashMap<String, GroupProperties>();
    HashSet<String> existingGroups = new HashSet<String>();    
    Iterator<String> agit = allGroups.iterator();
    while (agit.hasNext()) {
      String grpID = agit.next();
      GroupProperties gp = rsd_.instRcx.getCurrentLayout().getGroupProperties(grpID);
      if (gp != null) {
        existingGroups.add(grpID);
        gpMemory.put(grpID, new GroupProperties(gp));
      }
    }    
    
    //
    // Stuff from root:
    //
    
    HashMap<String, Layout> layoutsFromRoot = new HashMap<String, Layout>();
    HashMap<String, Rectangle> loFrRtBounds = new HashMap<String, Rectangle>();
    Point2D origin = new Point2D.Double(0.0, 0.0);
    
    
    StaticDataAccessContext rcx2m = new StaticDataAccessContext(rsd_.instRcx);
    rcx2m.setLayout(rsd_.rootRcx.getCurrentLayout());  
    buildGroupLayouts(rcx2m, layoutsFromRoot, loFrRtBounds, 
                      allGroups, rsd_.loModKeys, true, rsd_.options.inheritanceSquash, gpMemory, null, 
                      null, false, rsd_.monitor, rsd_.startFrac, eFrac1);

    
    //
    // Stuff already there:
    //
 
    HashMap<String, Layout> layoutsFromInstance = new HashMap<String, Layout>();
    HashMap<String, Rectangle> loFrInBounds = new HashMap<String, Rectangle>();
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts = 
      new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
    
    buildGroupLayouts(rsd_.instRcx, layoutsFromInstance, loFrInBounds, 
                      existingGroups, rsd_.loModKeys, false, false, null, null, 
                      moduleLinkFragShifts, true, rsd_.monitor, eFrac1, eFrac2);    
                          
    //
    // Go through each region.  Make sure each new node and fully contained link has a properties.
    //
     
    HashMap<String, Rectangle> newRegionBounds = new HashMap<String, Rectangle>();
    int numG = allGroups.size();
    double fracInc = (numG == 0) ? frac3 : (frac3 / numG);
    double cFrac = eFrac2;    
    agit = allGroups.iterator();
    while (agit.hasNext()) {
      String grpID = agit.next();
      Group grp = ((GenomeInstance)rsd_.instRcx.getCurrentGenome()).getGroup(grpID);    
      Layout grloRoot = layoutsFromRoot.get(grpID);
      StaticDataAccessContext rcxli = new StaticDataAccessContext(rsd_.instRcx);
      rcxli.setLayout(layoutsFromInstance.get(grpID));
      if (rcxli.getCurrentLayout() == null) {
        rcxli.setLayout(new Layout("junkID", rsd_.instRcx.getCurrentGenomeID()));
        layoutsFromInstance.put(grpID, rcxli.getCurrentLayout());
      }
      addNewProperties(grloRoot, rcxli, grp, origin, rsd_.options,
                       linkColors, rsd_.monitor, cFrac, cFrac + fracInc, rsd_.rememberProps, strictOKGroups);
      cFrac += fracInc; 
      Rectangle grpBounds = rcxli.getCurrentLayout().getLayoutBoundsForGroup(grp, rcxli, true, true);
      newRegionBounds.put(grpID, grpBounds);
    }
    
    //
    // When doing recovery, we want to generate exemptions for links that break the rules to
    // begin with.  Find the exemptions:
    //
    // NOTE: The revised gi does not have a layout that covers new links or nodes, so we only grid the
    // existing set:
    //
    LinkRouter router = new LinkRouter();
    HashSet<String> useNodes = new HashSet<String>();
    Iterator<Node> ncit = rsd_.instRcx.getCurrentGenome().getNodeIterator();
    while (ncit.hasNext()) {
      Node node = ncit.next();
      String nodeID = node.getID();
      if (rsd_.origLayout.getNodeProperties(nodeID) != null) {
        useNodes.add(nodeID);
      }
    }
    HashSet<String> skipLinks = new HashSet<String>();
    Iterator<Linkage> lit = rsd_.instRcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (rsd_.origLayout.getLinkProperties(linkID) == null) {
        skipLinks.add(linkID);
      }
    }
    StaticDataAccessContext rcx2o = new StaticDataAccessContext(rsd_.instRcx);
    rcx2o.setLayout(rsd_.origLayout);  
    LinkPlacementGrid exceptionGrid = router.initLimitedGrid(rcx2o, useNodes, skipLinks, INodeRenderer.STRICT);    
    Map<String, Set<String>> exempt = exceptionGrid.buildExemptions();          
       
    LinkRouter.RoutingResult retval = iterativeIncrementalMerge(rsd_.instRcx, crossRegion, crossTuples,
                                                                newRegionBounds,
                                                                rsd_.savedRegionBounds, layoutsFromInstance,
                                                                rsd_.options,
                                                                linkColors, rsd_.origLayout, false, null, rsd_.loModKeys, 
                                                                rsd_.monitor, eFrac3, rsd_.maxFrac, 
                                                                rsd_.rememberProps, exempt, null, null, 
                                                                moduleLinkFragShifts, strictOKGroups);
    
        
    //
    // Move module shapes around:
    //
    
    if ((rsd_.moduleShapeRecovery != null) && (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
      rsd_.instRcx.getCurrentLayout().shiftModuleShapesPerParams(rsd_.loModKeys, rsd_.moduleShapeRecovery, rsd_.instRcx);
    } 
    
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Synch to the root layout, but keep existing region relationships.  Note that
  ** this is a lot like incrementalLayout, but here we accept the root layout
  ** without question.  IncrementalLayout only uses links and nodes if they fit
  ** correct node/link routing semantics.
  */
  
  private LinkRouter.RoutingResult synchToExistingLayout(Set<String> targets, boolean strictOKGroups) throws AsynchExitRequestException {
                                   
                                   
    double fullFrac = rsd_.maxFrac - rsd_.startFrac;
    double frac1 = fullFrac * 0.25;
    double eFrac1 = rsd_.startFrac + frac1;
    double frac2 = fullFrac * 0.25;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.25;
    double eFrac3 = eFrac2 + frac2; 
    
    HashMap<String, String> linkColors = new HashMap<String, String>();
    buildLinkColorsMap(rsd_.rootRcx, linkColors);
    buildLinkColorsMap(rsd_.instRcx, linkColors);
   
    //
    // Start handling module recovery steps:
    // 
    
    int overlayOption = rsd_.options.overlayOption;    
    if (overlayOption == NetOverlayProperties.RELAYOUT_TO_MEMBER_ONLY) { 
      rsd_.instRcx.getCurrentLayout().convertAllModulesToMemberOnly(rsd_.loModKeys, rsd_.instRcx);
    }
    
    HashSet<String> crossRegion = new HashSet<String>();
    HashSet<String> allGroups = new HashSet<String>();
    HashMap<String, Link> crossTuples = new HashMap<String, Link>();
    // Want the link info, but don't care about the ordering....
    doGroupOrdering((GenomeInstance)rsd_.instRcx.getCurrentGenome(), crossRegion, allGroups, crossTuples); 
       
    //
    // Build separate layouts for each region.  Do this for the root layout.
    //
   
    HashMap<String, Layout> layoutsToUse = new HashMap<String, Layout>();
    HashMap<String, Rectangle> loBounds = new HashMap<String, Rectangle>();
    
    //
    // Regions being synced get copies from the root.
    //
    
    HashMap<String, GroupProperties> gpMemory = new HashMap<String, GroupProperties>();
    Iterator<String> tgit = targets.iterator();
    while (tgit.hasNext()) {
      String grpID = tgit.next();
      GroupProperties gp = rsd_.origLayout.getGroupProperties(grpID);
      if (gp != null) {
        gpMemory.put(grpID, new GroupProperties(gp));
      }
    }
    
    //
    // To do squash right, need to select compress based on cross-links being retained, then compress
    // using that selection, and apply the same compression results to the recovered cross-links!
    //
    
    StaticDataAccessContext rcxRLGI = new StaticDataAccessContext(rsd_.instRcx);
    rcxRLGI.setLayout(rsd_.rootRcx.getCurrentLayout());
    
    HashMap<String, Empties> emptiesForGroup = null;
    
    if (rsd_.options.inheritanceSquash) {
      emptiesForGroup = new HashMap<String, Empties>();
      tgit = targets.iterator();
      while (tgit.hasNext()) {
        String grpID = tgit.next();
        Empties empties = new Empties();
        emptiesForGroup.put(grpID, empties);
        chooseReducedRootCompression(rcxRLGI, grpID, empties.emptyRows, empties.emptyCols, rsd_.monitor); 
      }
    }
 
    //     layout.chooseCompressionRows(gi, frc, fracV, fracH, bounds, false, emptyRows, emptyCols, monitor); 
     //   grpLo.compress(gi, emptyRows, emptyCols, null, monitor, cFrac, cFrac + fracInc);
    

    buildGroupLayouts(rcxRLGI, layoutsToUse, loBounds, 
                      targets, rsd_.loModKeys, true, rsd_.options.inheritanceSquash, gpMemory, 
                      emptiesForGroup, null, false, rsd_.monitor, rsd_.startFrac, eFrac1);

    allGroups.removeAll(targets);
    
    //
    // Non-synced regions are getting copies from existing layout:
    //
   
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts = 
      new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
    StaticDataAccessContext rcxOLGI = new StaticDataAccessContext(rsd_.instRcx);
    rcxOLGI.setLayout(rsd_.origLayout); // <--- The "origLayout" here is a copy from the database. So need to provide a layout source wrapper for it.
    rcxOLGI.setLayoutSource(new LocalLayoutSource(rsd_.origLayout, rsd_.instRcx.getGenomeSource()));
    buildGroupLayouts(rcxOLGI, layoutsToUse, loBounds, 
                      allGroups, rsd_.loModKeys, false, false, null, null, 
                      moduleLinkFragShifts, true, rsd_.monitor, eFrac1, eFrac2);        
    
    //
    // When doing recovery, we want to generate exemptions for links that break the rules to
    // begin with.  Find the exemptions:
    //
    
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid exceptionGrid = router.initGrid(rcxOLGI, null, INodeRenderer.STRICT, rsd_.monitor);
    Map<String, Set<String>> exempt = exceptionGrid.buildExemptions();
    
    //
    // We want to build a recovery framework for those cross-region links that are ending in a region
    // that is being laid out to match the root.  We will try to reuse as much of the root link layout as we can.
    
    HashMap<String, Layout> ctTargRecoveryLo = new HashMap<String, Layout>();
    Map<String, Rectangle> rootBounds = new HashMap<String, Rectangle>();
    buildCrossLinkLayouts(rcxRLGI, ctTargRecoveryLo, crossTuples, targets, rootBounds, 
                          rsd_.options.inheritanceSquash, emptiesForGroup, rsd_.monitor, eFrac2, eFrac3);

    //
    // Now do the merge:
    // 
     
    LinkRouter.RoutingResult retval = iterativeIncrementalMerge(rsd_.instRcx, crossRegion, crossTuples,
                                                                loBounds,
                                                                rsd_.savedRegionBounds, 
                                                                layoutsToUse, rsd_.options,
                                                                linkColors, rsd_.origLayout, true, targets,
                                                                rsd_.loModKeys, rsd_.monitor, eFrac3, rsd_.maxFrac, rsd_.rememberProps, 
                                                                exempt, ctTargRecoveryLo, 
                                                                rootBounds, moduleLinkFragShifts, strictOKGroups);
    //
    // Move module shapes around:
    //
    
    if ((rsd_.moduleShapeRecovery != null) && (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
      rsd_.instRcx.getCurrentLayout().shiftModuleShapesPerParams(rsd_.loModKeys, rsd_.moduleShapeRecovery, rsd_.instRcx);
    }    
    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get the set of link IDs for links from the given source into the given target region
  */
  
  private Set<String> linksIntoTargetRegion(GenomeInstance gi, String srcID, Map<String, Link> crossTuples, String trgRegID) {
    
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> ctkit = crossTuples.keySet().iterator();    
    while (ctkit.hasNext()) {
      String linkID = ctkit.next();
      Linkage link = gi.getLinkage(linkID);
      if (!link.getSource().equals(srcID)) {
        continue;
      }
      Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!    
      String linkTargRegion = cfl.getTrg();
      if (trgRegID.equals(linkTargRegion)) {
        retval.add(linkID);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Map LSIDs into points.
  */
  
  private Point2D lsidToPoint(LinkSegmentID lsid, LinkProperties bp) {
    if (bp.isDirect() || lsid.isForEndDrop()) {
      throw new IllegalArgumentException();
    }
    Point2D retval;
    if (lsid.isForStartDrop()) {
      LinkSegment lseg = bp.getRootSegment();
      retval = lseg.getStart();
    } else {
      LinkSegment lseg = bp.getSegment(lsid);
      retval = lseg.getEnd();
    }
    return (retval);          
  } 
  
 /***************************************************************************
  **
  ** Map LSIDs into a list of points.
  */
  
  private List<Point2D> lsidToPointList(LinkSegmentID lsid, LinkProperties bp, StaticDataAccessContext rcx) {
    if (bp.isDirect() || lsid.isForEndDrop()) {
      throw new IllegalArgumentException();
    }
    
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    LinkSegment useSeg;
    if (lsid.isForStartDrop()) {
      useSeg = bp.getSegmentGeometryForID(lsid, rcx, false);
    } else {
      useSeg = bp.getSegment(lsid);
    }
    double length = useSeg.getLength();
    double numPts = length / UiUtil.GRID_SIZE;
    double weightInc = 1.0 / numPts;
    Point2D start = useSeg.getStart();
    Point2D end = useSeg.getEnd();
    if (start.equals(end)) {
      retval.add(start);
      return (retval);
    }
    ArrayList<Point2D> pts = new ArrayList<Point2D>();
    pts.add(start);
    pts.add(end);
    double[] weights = new double[2];
    weights[0] = 0.0;
    boolean haveEnd = false;
    while (weights[0] <= 1.0) {
      weights[1] = 1.0 - weights[0];
      Point2D combo = AffineCombination.combination(pts, weights, UiUtil.GRID_SIZE);
      if (combo.equals(end)) {
        haveEnd = true;
      }
      retval.add(combo);
      weights[0] += weightInc;
    }
    if (!haveEnd) {
      retval.add(end);
    }
    return (retval);          
  }   
  
  /***************************************************************************
  **
  ** Given the set of links into a target region, get back the LinkSegmentIDs that
  ** form the best place to cut them for gluing into the recovery tree.
  */
  
  private Set<LinkSegmentID> getTargetChopSegments(StaticDataAccessContext rcx, LinkProperties bp,
                                                   Rectangle bounds, Set<String> linksToTarg) {
    

    double bMinX = bounds.getMinX();
    double bMinY = bounds.getMinY();
    double bMaxX = bounds.getMaxX();
    double bMaxY = bounds.getMaxY();
    Point2D[] boundPts = new Point2D[4];
    boundPts[0] = new Point2D.Double(bMinX, bMinY);
    boundPts[1] = new Point2D.Double(bMaxX, bMinY);
    boundPts[2] = new Point2D.Double(bMaxX, bMaxY);
    boundPts[3] = new Point2D.Double(bMinX, bMaxY);

    // We don't handle direct stuff, so split it in half if we have it (we are working with copied layouts,
    // so this should be cool):
    if (bp.isDirect()) {
      bp.splitNoSegmentBus(rcx);
    }

    HashSet<LinkSegmentID> retval = new HashSet<LinkSegmentID>();   
    Iterator<String> lttit = linksToTarg.iterator();
    while (lttit.hasNext()) {
      String linkID = lttit.next();
      LinkBusDrop keepDrop = bp.getTargetDrop(linkID); 
      List<LinkSegmentID> sid2root = bp.getSegmentIDsToRootForEndDrop(keepDrop);
      int numSeg = sid2root.size(); 
      LinkSegmentID lastPoint = null;
      LinkSegmentID breakPoint = null;
      for (int i = 1; i < numSeg; i++) {  // skip the target drop
        LinkSegmentID lsid = sid2root.get(i);
        Point2D chkLoc = lsidToPoint(lsid, bp);
        double clx = chkLoc.getX();
        double cly = chkLoc.getY();
       // if (!bounds.contains(chkLoc)) {
        if (!((clx > bMinX) && (clx < bMaxX) && (cly > bMinY) && (cly < bMaxY))) { // point on bounds->outside!            
          breakPoint = lsid;
          break;
        }
        lastPoint = lsid;
      }
           
      //  NOW CUT THEM AT THE BOUNDARY!!!!!
      
      if ((breakPoint != null) && (lastPoint != null)) {
        Point2D interLoc = null;
        Point2D line1pt1 = lsidToPoint(breakPoint, bp);
        Point2D line1pt2 = lsidToPoint(lastPoint, bp);
        for (int i = 0; i < 4; i++) {
          int endIndex = (i + 1) % 4; 
          interLoc = UiUtil.lineIntersection(line1pt1, line1pt2, boundPts[i], boundPts[endIndex]);
          if (interLoc != null) {
            break;
          }
        }
        if (interLoc != null) {
          LinkProperties.DistancedLinkSegID dsnlsid = bp.intersectBusSegment(rcx, interLoc, null, 0.1);
          if ((dsnlsid != null) && !dsnlsid.segID.isTaggedWithEndpoint()) {
            LinkSegmentID[] splitIDs = bp.linkSplitSupport(dsnlsid.segID, interLoc);            
            if (splitIDs != null) { 
              breakPoint = splitIDs[0];
            }
          }
        }
      }
            
      if (breakPoint == null) {
        breakPoint = LinkSegmentID.buildIDForStartDrop();
      }
      retval.add(breakPoint);
    }
    return (retval);
    
  }  

  
  /***************************************************************************
  **
  ** Answer if the point is in a target region
  */
  
  private boolean segInATargetRegion(Point2D chkLoc, Map<String, Rectangle> allBounds, Set<String> targRegions) {
    Iterator<String> trit = targRegions.iterator();
    while (trit.hasNext()) {
      String trid = trit.next();
      Rectangle bounds = allBounds.get(trid);
      if (bounds.contains(chkLoc)) {
        return (true);
      }      
    }
    return (false);     
  }
  
 
  /***************************************************************************
  **
  ** Parameterize the inbound links in terms of the region boundaries
  */
  
  private List<WhipCandidate> parameterizeSourceWhips(LinkProperties bp, Rectangle bounds, 
                                                      Set<String> linksForSource, 
                                                      Map<String, Rectangle> allBounds, 
                                                      Set<String> targRegions, 
                                                      StaticDataAccessContext rcx) {
    //
    // For each point on the source tree that is outside the target region, parameterize
    // it in terms of RegionBasedPositions
    
   
    ArrayList<WhipCandidate> retval = new ArrayList<WhipCandidate>();
    if (bp.isDirect()) {
      // We should have split any direct stuff up prior to getting here!
      System.err.println("Unexpected direct linkProp");
      return (retval);
    }
    
    HashMap<LinkSegmentID, List<WhipCandidate>> whipTracker = new HashMap<LinkSegmentID, List<WhipCandidate>>();
    
    Iterator<String> lttit = linksForSource.iterator();
    String linkID = null;
    while (lttit.hasNext()) {
      linkID = lttit.next();
     
      LinkBusDrop keepDrop = bp.getTargetDrop(linkID);       
      List<LinkSegmentID> sid2root = bp.getSegmentIDsToRootForEndDrop(keepDrop);
      int numSeg = sid2root.size();
      for (int i = numSeg - 1; i > 0; i--) {
        LinkSegmentID lsid = sid2root.get(i);
        if (lsid.isForEndDrop()) {
          // FIX ME?? why an end drop?
          continue;
        }
        List<WhipCandidate> whipsForKey = whipTracker.get(lsid);
        if (whipsForKey != null) {
          int numWhips = whipsForKey.size();
          for (int j = 0; j < numWhips; j++) {
            WhipCandidate wc = whipsForKey.get(j);
            wc.addSupportedLink(linkID);
          }
          continue;
        }
        List<Point2D> chkList = lsidToPointList(lsid, bp, rcx);
        int numChk = chkList.size();
        for (int j = 1; j < numChk; j++) { // Skip first point!
          Point2D chkLoc = chkList.get(j);
          if (segInATargetRegion(chkLoc, allBounds, targRegions)) {
            continue;
          }
          RegionBasedPosition rbp = closestPointOnBoundary(chkLoc, bounds);
          WhipCandidate wc = new WhipCandidate(chkLoc, lsid, rbp, linkID);
          whipsForKey = new ArrayList<WhipCandidate>();
          whipsForKey.add(wc);
          whipTracker.put(lsid, whipsForKey);
          retval.add(wc);
        }
      }
    }
    if (retval.isEmpty()) {
      LinkSegmentID lsid = LinkSegmentID.buildIDForStartDrop();
      Point2D chkLoc = lsidToPoint(lsid, bp);
      RegionBasedPosition rbp = closestPointOnBoundary(chkLoc, bounds);
      retval.add(new WhipCandidate(chkLoc, lsid, rbp, linkID));
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Merge the two trees together
  */
  
  private void doTreeMerge(String srcID, StaticDataAccessContext rcx, LinkProperties origBp,
                           Map<String, Map<LinkSegmentID, TreeSpliceData>> mergedSegsPerSource, Map<String, TreeMergePerRegion> allTmprs) {
  
    Map<LinkSegmentID, TreeSpliceData> mergedSegs = mergedSegsPerSource.get(srcID);
    if (mergedSegs == null) {      
      mergedSegs = new HashMap<LinkSegmentID, TreeSpliceData>();
      mergedSegsPerSource.put(srcID, mergedSegs);
    }

    //
    // For each target stub, find the closest source whip in parameter space.  Then chop that
    // piece off the tree and glue it into the recovery tree.
    //
        
    ArrayList<LinkProperties.GlueJob> glueJobs = new ArrayList<LinkProperties.GlueJob>();
    ArrayList<TreeSpliceData> stubTsds = new ArrayList<TreeSpliceData>();
    Iterator<String> atkit = allTmprs.keySet().iterator();
    while (atkit.hasNext()) {
      String trgGrpID = atkit.next();
      TreeMergePerRegion tmpr = allTmprs.get(trgGrpID);
      Iterator<LinkSegmentID> stubit = tmpr.targStubs.keySet().iterator();
      while (stubit.hasNext()) {
        LinkSegmentID targLsid = stubit.next();
        Point2D dicePt = tmpr.dicePts.get(targLsid);
        RegionBasedPosition targStubRbp = tmpr.targStubs.get(targLsid);
        LinkProperties.DistancedLinkSegID dswhipSeg = origBp.intersectBusSegment(rcx, dicePt, null, UiUtil.GRID_SIZE);
        //
        // Bug BT-03-16-10:5 Saw a crash here with whipSeg equals Zero.  Could not reproduce with
        // other fixes added.
        //
        if (dswhipSeg.segID.startEndpointIsTagged()) {
          if (dswhipSeg.segID.isForSegment()) {
            LinkSegment lseg = origBp.getSegment(dswhipSeg.segID);
            String parent = lseg.getParent();
            if (parent != null) {
              origBp.getSegment(parent);
              dswhipSeg.segID = LinkSegmentID.buildIDForSegment(parent);
              dswhipSeg.segID.tagIDWithEndpoint(LinkSegmentID.END);
            }
          } else if (dswhipSeg.segID.isForEndDrop()) {
            LinkBusDrop drop = origBp.getDrop(dswhipSeg.segID);
            dswhipSeg.segID = LinkSegmentID.buildIDForSegment(drop.getConnectionTag());
            dswhipSeg.segID.tagIDWithEndpoint(LinkSegmentID.END);
            if (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
              throw new IllegalStateException();  //FIX ME
            }
          }
        }
        LinkSegment lseg = origBp.getSegmentGeometryForID(dswhipSeg.segID, rcx, false);
        String aLinkID = tmpr.linksToTarg.iterator().next();
        Linkage aLink = rcx.getCurrentGenome().getLinkage(aLinkID);
        String aTrgID = aLink.getTarget();
        //
        // 9/13/12: Crash: lseg here is zero length, null run!  Make sure we have no null ptr,
        // BUT this is then creating a TreeSpliceData from a degenerate segment.  With a sudsequent
        // check in LinkProperties, however, the system (seems to) cruise through the problem.
        //
        Vector2D run = lseg.getRun();
        boolean isCanonical = (run == null) ? false : run.isCanonical();
        TreeSpliceData stubTsd = new TreeSpliceData(isCanonical, run, targStubRbp, trgGrpID, aTrgID);
        Set<String> resolved = tmpr.bp.resolveLinkagesThroughSegment(targLsid);
        LinkProperties newProps = tmpr.bp.breakTreePortionToNewSource(targLsid, resolved, null, null); 
        LinkSegmentID taggedWhip = dswhipSeg.segID.clone();
        glueJobs.add(new LinkProperties.GlueJob(newProps, taggedWhip));
        stubTsds.add(stubTsd);
      } 
    }
    
    //
    // Note that we don't actually want to start hacking away at the link tree before
    // we have figured out everything that needs to happen, then do it in one step,
    // so we don't chop off pieces needed by the next stubs...
    //
    
    origBp.mergeReplacementSubTreesToTreeAtSegment(glueJobs, rcx.getCurrentGenome());
    int numGlue = glueJobs.size();
    for (int i = 0; i < numGlue; i++) {
      BusProperties.GlueJob glueJob = glueJobs.get(i);
      TreeSpliceData stubTsd = stubTsds.get(i);
      HashSet<String> resolved = new HashSet<String>(glueJob.newProps.getLinkageList());
      LinkSegmentID whipSeg = glueJob.taggedWhip.clone();
      whipSeg.clearTaggedEndpoint();
      tagForRecovery(resolved, whipSeg, origBp, mergedSegs, stubTsd);
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Tag segs downstream of the merge splice to make sure they get defined wrt target node 
  */
  
  private void tagForRecovery(Set<String> resolved, LinkSegmentID srcLsid, LinkProperties origBp, 
                              Map<LinkSegmentID, TreeSpliceData> mergedSegs, TreeSpliceData stubTsd) {
    
    //
    // Gather up the segs that were handled this way, so that they will
    // be defined wrt target nodes only in the recovery phase:
    //
    
    LinkSegmentID lastLsid = null;
    Iterator<String> rit = resolved.iterator();
    while (rit.hasNext()) {
      String linkID = rit.next();
      LinkBusDrop keepDrop = origBp.getTargetDrop(linkID);       
      List<LinkSegmentID> sid2root = origBp.getSegmentIDsToRootForEndDrop(keepDrop);
      int num2root = sid2root.size();
      for (int i = 0; i < num2root; i++) {
        LinkSegmentID lsid = sid2root.get(i);
        if (lsid.equals(srcLsid)) {
          break;
        }
        mergedSegs.put(lsid, new TreeSpliceData(stubTsd.trgGrpID, stubTsd.trgNodeID));
        lastLsid = lsid;
      }
    }
    // Replace the last one with the stub:
    if (lastLsid != null) {
      mergedSegs.put(lastLsid, stubTsd);
    }  
    return;
  }    

  /***************************************************************************
  **
  ** Find the best source whip to use for the given target stub...
  */
  
  private WhipCandidate bestWhipToStubSplice(RegionBasedPosition targStubRbp, List<WhipCandidate> sourceWhips) {
    
    //
    // A whip on the same side as the stub beats all others.  If no whips on the
    // same side, the corners are best.  If no corners, check the two adjacent sides.
    // Finally check the opposite side.  Within a side, the smallest offset is best.
    // With the same offset, the one that matches the position along the side is
    // best.  With a tie, ordered by lsid.
    //
    RegionBasedSorter rbs = new RegionBasedSorter(targStubRbp, sourceWhips);
    if (rbs.hasNext()) {
      return (rbs.next());
    }
    return (null);
  }    
 
  
  /***************************************************************************
  **
  ** Synthesize a set of new link trees that glue the structure within a copied target
  ** region into the layout as a whole.
  */
  
  private void synthMergedTrees(StaticDataAccessContext rcx,
                                Map<String, Link> crossTuples, Set<String> srcsToDo, 
                                Map<String, Layout> targetLayouts, Map<String, Rectangle> rootBounds,
                                Map<String, Rectangle> targetBounds, Map<String, LinkProperties> synthLayouts, 
                                Map<String, Map<LinkSegmentID, TreeSpliceData>> mergedSegsPerSource) {
     
    //
    // Loop for each source.  For each source, gather up the trees for each target and glue them
    // into the original source tree, dropping the original path:
    //
    
    HashMap<String, Set<String>> linksToTargForTarg = new HashMap<String, Set<String>>();
    Iterator<String> sit = srcsToDo.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      //
      // Fix for BT-03-17-10:2. Without this clearance, links from different sources begin
      // to accumulate and regions that have no links from the current source will retain
      // previous links from previous sources.  The result are trees with drop for links
      // that are not even from the right source.  Lots of bad things then happen downstream.
      //
      linksToTargForTarg.clear();
      
      Set<String> linksForSource = rcx.getCurrentGenome().getOutboundLinks(srcID);
      
      LinkProperties origBp = rcx.getCurrentLayout().getBusForSource(srcID).clone();
      if (origBp.isDirect()) {
        // Gotta have SOME point to work with if the link in direct.  Since we are working
        // here with a clone, this crappy kludge is to just split the bus in half.  
        // FIX ME? IS there a better way to do this?
        origBp.splitNoSegmentBus(rcx);
      }
       
      Iterator<String> grit = targetLayouts.keySet().iterator();  // target groups
      while (grit.hasNext()) {
        String trgGrpID = grit.next();
        Set<String> linksToTarg = linksIntoTargetRegion((GenomeInstance)rcx.getCurrentGenome(), srcID, crossTuples, trgGrpID);
        if (linksToTarg.isEmpty()) {
          continue;
        }
        linksToTargForTarg.put(trgGrpID, linksToTarg);
      }

      // Get the set of links inbound to each target region for the source.  For each,
      // merge trees.
      
      HashMap<String, TreeMergePerRegion> allTmprs = new HashMap<String, TreeMergePerRegion>();
      Iterator<String> glrit = linksToTargForTarg.keySet().iterator();  // target groups
      while (glrit.hasNext()) {
        String trgGrpID = glrit.next();
        Set<String> linksToTarg = linksToTargForTarg.get(trgGrpID);
        StaticDataAccessContext rcx4TR = new StaticDataAccessContext(rcx);
        rcx4TR.setLayout(targetLayouts.get(trgGrpID));
        Rectangle rootBoundsForTrgReg = rootBounds.get(trgGrpID);
        Rectangle targBoundsForTrgReg = targetBounds.get(trgGrpID);
        String aLinkID = linksToTarg.iterator().next();
        LinkProperties bp = rcx4TR.getCurrentLayout().getLinkProperties(aLinkID);
        //
        // Quick fix for BT-12-03-09:1.  If we have multiple instances of an inbound
        // link from multiple instances of the source, only one instance is present.
        // Skip the whole operation for those orphaned instances.
        //
        List<String> actualLinks = bp.getLinkageList();
        if (!actualLinks.contains(aLinkID)) {
          continue;
        }
         
        Set<LinkSegmentID> targChopSegs = getTargetChopSegments(rcx4TR, bp, rootBoundsForTrgReg, linksToTarg);
        // Build a parametric description of each emerging stub:
        HashMap<LinkSegmentID, RegionBasedPosition> targStubs = new HashMap<LinkSegmentID, RegionBasedPosition>();
        Iterator<LinkSegmentID> tcsit = targChopSegs.iterator();
        while (tcsit.hasNext()) {
          LinkSegmentID segID = tcsit.next();
          Point2D nextPt = lsidToPoint(segID, bp);
          RegionBasedPosition rbp = closestPointOnBoundary(nextPt, rootBoundsForTrgReg);
          targStubs.put(segID, rbp);
        }
        // Build parametric description of each available whip:
        List<WhipCandidate> sourceWhips = parameterizeSourceWhips(origBp, targBoundsForTrgReg, linksForSource, 
                                                                  targetBounds, linksToTargForTarg.keySet(), rcx);
        //
        // Find the best place to splice, and chop up the whips.  We get back a list of best point per stub:
        //
        Map<LinkSegmentID, Point2D> dicePts = sliceAndDiceWhips(origBp, sourceWhips, targStubs, rcx);
        TreeMergePerRegion tmpr = new TreeMergePerRegion(bp, dicePts, targStubs, linksToTarg);
        allTmprs.put(trgGrpID, tmpr);
      }     
      // Find the closest whip to each stub; glue them together:
      doTreeMerge(srcID, rcx, origBp, mergedSegsPerSource, allTmprs); 
      synthLayouts.put(srcID, origBp);
    }   
    return;
  }
  
  /***************************************************************************
  **
  ** Find the best place to chop up the whips, and do the chopping.  Return the
  ** chosen points:
  */
  
  private Map<LinkSegmentID, Point2D> sliceAndDiceWhips(LinkProperties bp, List<WhipCandidate> sourceWhips, 
                                                        Map<LinkSegmentID, RegionBasedPosition> targStubs, StaticDataAccessContext rcx) {
    
    HashMap<LinkSegmentID, Point2D> retval = new HashMap<LinkSegmentID, Point2D>();
    Iterator<LinkSegmentID> stubit = targStubs.keySet().iterator();
    while (stubit.hasNext()) {
      LinkSegmentID targLsid = stubit.next();
      RegionBasedPosition targStubRbp = targStubs.get(targLsid);  
      WhipCandidate whipCand = bestWhipToStubSplice(targStubRbp, sourceWhips);
      LinkProperties.DistancedLinkSegID dsnlsid = bp.intersectBusSegment(rcx, whipCand.point, null, UiUtil.GRID_SIZE);
      boolean didSplit = false;
      if ((dsnlsid != null) && !dsnlsid.segID.isTaggedWithEndpoint()) {
        LinkSegmentID[] splitIDs = bp.linkSplitSupport(dsnlsid.segID, whipCand.point);            
        if (splitIDs != null) { 
          Point2D nextPt = lsidToPoint(splitIDs[0], bp);
          retval.put(targLsid, nextPt);
          didSplit = true;
        }
      }
      if (!didSplit) {
        retval.put(targLsid, whipCand.point);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Go through the necessary boundary/resizing operations to get it so we can
  ** layout inter-region links.
  */
  
  private LinkRouter.RoutingResult iterativeIncrementalMerge( // Get this arg list back under control! Sheesh!
                                                             StaticDataAccessContext rcxI,
                                                             Set<String> crossRegion,
                                                             Map<String, Link> crossTuples,
                                                             Map<String, Rectangle> newRegionBounds,
                                                             Map<String, Rectangle> savedRegionBounds, 
                                                             Map<String, Layout> layoutsFromInstance,
                                                             LayoutOptions options,
                                                             Map<String, String> linkColors,
                                                             Layout origLayout,
                                                             boolean recoveryMerge,
                                                             Set<String> changingGroups,
                                                             Layout.OverlayKeySet loModKeys,
                                                             BTProgressMonitor monitor, 
                                                             double startFrac, double endFrac,
                                                             Map<String, BusProperties.RememberProps> rememberProps, 
                                                             Map<String, Set<String>> exemptions, 
                                                             Map<String, Layout> syncLayouts, 
                                                             Map<String, Rectangle> rootRegionBounds, 
                                                             Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts, 
                                                             boolean strictOKGroups) 
                                                             throws AsynchExitRequestException {
                                                 
    //
    // Figure out the links that are not already handled by the layout, and
    // add them here:
    //

    HashSet<String> todo = new HashSet<String>();
    HashSet<String> srcsToDo = new HashSet<String>();
    Iterator<String> ctit = crossRegion.iterator();
    while (ctit.hasNext()) {
      String linkID = ctit.next();
      if (rcxI.getCurrentLayout().getLinkProperties(linkID) == null) {
        todo.add(linkID);
        Linkage link = rcxI.getCurrentGenome().getLinkage(linkID);
        srcsToDo.add(link.getSource());
      }
    }
    
    //
    // If we need to steal link layouts for cross-links from the root layout, we do that
    // now, building up synthesized layouts for recovery:
    //   
    //
    // This is where we deal with whips and stubs!
    //
    
    StaticDataAccessContext rcxO = new StaticDataAccessContext(rcxI);
    rcxO.setLayout(origLayout);
    
    GenericBPSource gbps = null;
    Map<String, Map<LinkSegmentID, TreeSpliceData>> mergedSegsPerSource = null;
    if (syncLayouts != null) {
      Map<String, LinkProperties> synthLayouts = new HashMap<String, LinkProperties>();
      mergedSegsPerSource = new HashMap<String, Map<LinkSegmentID, TreeSpliceData>>();
      synthMergedTrees(rcxO, crossTuples, srcsToDo, syncLayouts, 
                       rootRegionBounds, savedRegionBounds, synthLayouts, mergedSegsPerSource);
      gbps = new GenericBPSource(synthLayouts, rcxI.getCurrentGenome());
    } 
    
    CrossLinkRecovery recovery = extractRecoveryPathsFromOldLayout(rcxO, crossTuples.keySet(), gbps, exemptions, null, null);
    
    //
    // Collect up the cross-region link paths:
    //
    buildCrossPathRegionAssociations(rcxO, savedRegionBounds, crossTuples, recovery, mergedSegsPerSource);
    
    //
    // Figure out the padding between regions 
    //
                              
    int numRegions = layoutsFromInstance.size();
    int numCross = crossRegion.size();
    double borderSizeD = ((double)numCross / (double)numRegions) * 2.0;
    int borderSizeBase = (int)Math.ceil((borderSizeD == 0.0) ? 2.0 : borderSizeD);
    
    //
    // Divide up our progress allocation
    //

    //
    // WJRL 9/26/08:  Disabling the iteration.  Practical experience shows that this
    // causes layout propagation to blow up.  We need to create a highly targeted
    // analysis of the failures and address them in a very focused manner, instead of
    // expanding whole region layouts.
    //    
       
    // magic numbers; too big, and we can run out of memory    
    //
    // The above adds extra even when nothing has changed.  Don't do that!
    //
    int[][] passes = {{0, 0}, //{{borderSizeBase, 0}, 
                      {0, 1},  //{borderSizeBase, 1}, 
                      {2 * borderSizeBase, 0}, 
                      {3 * borderSizeBase, 0}, 
                      {3 * borderSizeBase, 1},
                      {3 * borderSizeBase, 2}};
                     
    int numPasses = 1; // passes[0].length;  THIS is a fast way to eliminate the iteration...

    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / numPasses;
    
    //
    // Loop to change bordersize:
    //
    
    Layout holdFirstLayout = null;
    HashMap<String, Rectangle> holdFirstBounds = null;
    LinkRouter.RoutingResult holdFirstRes = null;
    boolean basePass = true;

    for (int passNum = 0; passNum < numPasses; passNum++) {     
      //
      // Get temporary copies for layout and bounds:
      //
      StaticDataAccessContext rcxU = new StaticDataAccessContext(rcxI);
      rcxU.setLayout(new Layout("temp", rcxI.getCurrentGenomeID()));
      HashMap<String, Rectangle> useBounds = new HashMap<String, Rectangle>();
      copyResultsBack(rcxI.getCurrentLayout(), rcxU.getCurrentLayout(), newRegionBounds, useBounds); 

      int border = passes[passNum][0];
      int mult = passes[passNum][1]; 

      LinkRouter.RoutingResult res = incrementalMerges(todo, crossTuples, border, mult, rcxU, origLayout, useBounds,
                                                       savedRegionBounds, layoutsFromInstance, options, linkColors,  
                                                       recovery, recoveryMerge, changingGroups,
                                                       loModKeys, moduleLinkFragShifts, 
                                                       monitor, currProg, currProg + progInc, rememberProps, 
                                                       gbps, strictOKGroups);
      currProg += progInc;
      if ((res.linkResult & LinkRouter.LAYOUT_PROBLEM) != 0x00) {

        // Link routing can fail when two links are inbound onto the same pad.
        // We don't want to try to rectify this with a region growth approach,
        // so see if it is the case.
        if (basePass) {
          basePass = false;
          Set<String> targCollide = rcxI.getCurrentGenome().hasLinkTargetPadCollisions();
          if (!targCollide.isEmpty()) {
            copyResultsBack(rcxU.getCurrentLayout(), rcxI.getCurrentLayout(), useBounds, newRegionBounds);
            return (res);
          } else {
            holdFirstLayout = rcxU.getCurrentLayout();
            holdFirstBounds = useBounds;
            holdFirstRes = res;
          }
        } 
      } else {
        copyResultsBack(rcxU.getCurrentLayout(), rcxI.getCurrentLayout(), useBounds, newRegionBounds);
        return (res);
      }
    }

    copyResultsBack(holdFirstLayout, rcxI.getCurrentLayout(), holdFirstBounds, newRegionBounds); // zero-expansion version...
    return (holdFirstRes);    
  } 
  
  
  /***************************************************************************
  **
  ** Do an incremental merge with specified boundary and non-reversable multiplier
  */
  
  private LinkRouter.RoutingResult incrementalMerges( // Get this arg list back under control too! Sheesh!
                                                     Set<String> todo, 
                                                     Map<String, Link> crossTuples, 
                                                     int boundary, int mult, 
                                                     StaticDataAccessContext rcxB, 
                                                     Layout origLayout, 
                                                     Map<String, Rectangle> newRegionBounds,
                                                     Map<String, Rectangle> savedRegionBounds,
                                                     Map<String, Layout> layoutsFromInstance,
                                                     LayoutOptions options,
                                                     Map<String, String> linkColors, 
                                                     CrossLinkRecovery recovery, boolean recoveryMerge, 
                                                     Set<String> changingGroups, 
                                                     Layout.OverlayKeySet loModKeys,
                                                     Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts,
                                                     BTProgressMonitor monitor, 
                                                     double startFrac, double endFrac, 
                                                     Map<String, BusProperties.RememberProps> rememberProps, GenericBPSource bps, 
                                                     boolean strictOKGroups) throws AsynchExitRequestException {
 
                                   
    //
    // We have laid out each region to handle all internal links.  Now merge them together
    // with a specified boundary size and a specified non-reversable expansion
    //

    double fullFrac = endFrac - startFrac;
    double frac1 = fullFrac * 0.33;
    double eFrac1 = startFrac + frac1;
    double frac2 = fullFrac * 0.33;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.33;
    double eFrac3 = eFrac2 + frac3;    
    
    //
    // Do non-rev expansion here!
    //
                                                       
    Map<String, Layout> useLayouts = layoutsFromInstance; 
    Map<String, Rectangle> useBounds = newRegionBounds;
    Map<String, Integer> useBoundaries = new HashMap<String, Integer>();
    
    Iterator<String> git = layoutsFromInstance.keySet().iterator();
    while (git.hasNext()) {
      String gpID = git.next(); 
      useBoundaries.put(gpID, new Integer(boundary * UiUtil.GRID_SIZE_INT));  // Booooogus! 
    }
                   
    if (mult > 0) {
      useLayouts = new HashMap<String, Layout>();
      useBounds = new HashMap<String, Rectangle>();
      Set<String> lfiSet = layoutsFromInstance.keySet();
      int size = lfiSet.size();
      double currProg = startFrac;
      double progInc = frac1 / size;      
      Iterator<String> lit = lfiSet.iterator();
      while (lit.hasNext()) {
        String gpID = lit.next();
        Group grp = ((GenomeInstance)rcxB.getCurrentGenome()).getGroup(gpID);   
        StaticDataAccessContext rcxET = new StaticDataAccessContext(rcxB);
        rcxET.setLayout(new Layout(layoutsFromInstance.get(gpID)));
        useLayouts.put(gpID, rcxET.getCurrentLayout());
        Rectangle bounds = newRegionBounds.get(gpID);
        useBounds.put(gpID, bounds);
        if (false /* FIX ME: gpID not a target or source*/) {
          continue;
        }
        TreeSet<Integer> insertRows = new TreeSet<Integer>();
        TreeSet<Integer> insertCols = new TreeSet<Integer>();
     
        rcxET.getCurrentLayout().chooseExpansionRows(rcxET, 1.0, 1.0, null, loModKeys, insertRows, insertCols, false, monitor);
        rcxET.getCurrentLayout().expand(rcxET, insertRows, insertCols, mult, true, null, moduleLinkFragShifts, gpID, monitor, currProg, currProg + progInc);
        expandOrCompressRecoveryPaths(true, rcxET.getCurrentLayout(), gpID, insertRows, insertCols, mult, recovery);     
        currProg += progInc;
        bounds = rcxET.getCurrentLayout().getLayoutBoundsForGroup(grp, rcxET, true, true);        
        useBounds.put(gpID, bounds);  
      }                                                    
    }
    
    //
    // Find out the paddings we need around each region:
    // 
    
    //  Map paddings = findNewRegionPaddings(crossTuples, UiUtil.GRID_SIZE_INT, recovery);    
       
    //
    // Merge separate layouts into one:
    // 
    
    ArrayList<PlacementElement> fixedElements = new ArrayList<PlacementElement>(); 
    Map<Object, Vector2D> deltas = mergeExpandedGroupLayouts(rcxB.getCurrentLayout(), useBounds, 
                                                             savedRegionBounds, useLayouts, useBoundaries, fixedElements, 
                                                             moduleLinkFragShifts, false, null);
    
    //
    // Still need to pin guys that were not expanded:
    //
   
    Set<String> lfiSet = layoutsFromInstance.keySet();
    Iterator<String> lit = lfiSet.iterator();
    while (lit.hasNext()) {
      String gpID = lit.next();
      Vector2D delta = deltas.get(gpID);
      pinUnchangedRecoveryPaths(rcxB.getCurrentLayout(), gpID, recovery, delta);
    }
    
    
    if (recoveryMerge) {
      recoveryMerge(recovery, rcxB, origLayout, deltas, changingGroups, bps);
    }    
       
    //
    // Place newly created regions.  Existing regions are fixed, new regions float.
    //
    
    Set<String> allRegions = useBounds.keySet();
    Set<String> fixedRegions = savedRegionBounds.keySet();

    ArrayList<PlacementElement> floatingElems = new ArrayList<PlacementElement>();
    Iterator<String> arit = allRegions.iterator();
    while (arit.hasNext()) {
      String gid = arit.next();
      if (fixedRegions.contains(gid)) {
        continue;
      }
      Rectangle newBounds = (Rectangle)useBounds.get(gid).clone();
      UiUtil.forceToGrid(newBounds, UiUtil.GRID_SIZE);
      PlacementElement newPe = new PlacementElement(newBounds, gid);
      floatingElems.add(newPe);
    }

    RectangleFitter rfit = new RectangleFitter();
    ArrayList<Link> links = new ArrayList<Link>(crossTuples.values());
    rfit.placeElements(fixedElements, floatingElems, links, boundary, monitor);

    int baseGroupOrder = rcxB.getCurrentLayout().getTopGroupOrder(); 
    int flnum = floatingElems.size();
    for (int i = 0; i < flnum; i++) {
      PlacementElement pe = floatingElems.get(i);
      Rectangle preShift = useBounds.get(pe.id);
      double deltaX = UiUtil.forceToGridValue(pe.rect.x - preShift.x, UiUtil.GRID_SIZE);      
      double deltaY = UiUtil.forceToGridValue(pe.rect.y - preShift.y, UiUtil.GRID_SIZE);
      Vector2D delta = new Vector2D(deltaX, deltaY);
      Layout currLO = useLayouts.get(pe.id);
      rcxB.getCurrentLayout().mergeLayoutOfRegion(currLO, delta, false, false, baseGroupOrder, moduleLinkFragShifts, pe.id);
    } 
    
    // If there are no cross-region links, we are done without needing to do layouts
    if (todo.isEmpty()) {
      if (monitor != null) {
        if (!monitor.updateProgress((int)(endFrac * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
      return (new LinkRouter.RoutingResult());
    }
    
    HashSet<String> stillToDo = new HashSet<String>(todo);
    HashSet<String> stillToDoWithRecovery = new HashSet<String>(recovery.recoveredLinks);
    
    Map<String, LinkPlacementGrid.RecoveryDataForSource> shiftedRecoveryPaths = 
      modifyCrossLinksForGrowthOrShrinkage(rcxB, recovery, useBounds, deltas, useBoundaries);

    stillToDo.removeAll(stillToDoWithRecovery);
    LinkRouter.RoutingResult res1 = layoutSomeLinksCore(stillToDoWithRecovery, rcxB, options,
                                                       linkColors, monitor, eFrac1, eFrac2,
                                                       rememberProps, shiftedRecoveryPaths, strictOKGroups);
    // Even failed links get crude layout; must be dropped if trying again:
    Iterator<String> fit = res1.failedLinks.iterator();
    while (fit.hasNext()) {
      String failID = fit.next();
      rcxB.getCurrentLayout().removeLinkProperties(failID);
    }
    stillToDo.addAll(res1.failedLinks);
       
    LinkRouter.RoutingResult res = layoutSomeLinksCore(stillToDo, rcxB, options,
                                                       linkColors, monitor, eFrac2, eFrac3,
                                                       rememberProps, null, strictOKGroups);       
    return (res);
  }
  
  
  /***************************************************************************
  **
  ** Merge existing link recovery definition with modified layout.  Don't need to do this
  ** for expansion or compression, or incremental layout, since VfA layout is basically the
  ** same.  But for syncing with root VfG layout, differences could be huge.
  */
  
  private void recoveryMerge(CrossLinkRecovery recovery, StaticDataAccessContext rcxB, Layout origLayout,
                             Map<Object, Vector2D> deltas, Set<String> changingGroups, GenericBPSource bps) {
    
    //
    // For each region, we need to compare the new region network layout with the
    // original definition, and change the recovery info to match
    //
 
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      NodeProperties osnp = origLayout.getNodeProperties(srcID);
      PerSrcData psd = recovery.perSrcMap.get(srcID);

      Iterator<LinkSegmentID> ppit = psd.perPointMap.keySet().iterator();
      while (ppit.hasNext()) {
        LinkSegmentID lsid = ppit.next();
        PerPointData ppd = psd.perPointMap.get(lsid);
        if (ppd.fixedLinkID != null) {
          Point2D oldLoc;
          LinkProperties nbp = rcxB.getCurrentLayout().getLinkProperties(ppd.fixedLinkID);
          //
          // Fix for BT-03-19-10:1.  Cannot use origianl properties if we have run through
          // a synth trees step.  Use that instead!
          //
          LinkProperties obp = (bps != null) ? bps.getLinkProperties(ppd.fixedLinkID) : origLayout.getLinkProperties(ppd.fixedLinkID);
          if (lsid.isForStartDrop()) {
            LinkSegment lseg = obp.getRootSegment();
            oldLoc = lseg.getStart();
          } else {
            LinkSegment lseg = obp.getSegment(lsid);
            oldLoc = lseg.getEnd();
            // We get cases where the segment for the lsid is the single-point root seg:
            if (oldLoc == null) {
              oldLoc = lseg.getStart();
            }
          }
          Point2D movedLoc = (Point2D)oldLoc.clone();
          if (deltas != null) {
            Vector2D delta = deltas.get(ppd.regionID);
            if (delta != null) {
              movedLoc = delta.add(oldLoc);
            }            
          }
          LinkProperties.DistancedLinkSegID dsnlsid = nbp.intersectBusSegment(rcxB, movedLoc, null, 0.1);
          boolean didSplit = false;
          if ((dsnlsid != null) && !dsnlsid.segID.isTaggedWithEndpoint()) {
            LinkSegmentID[] splitIDs = nbp.linkSplitSupport(dsnlsid.segID, movedLoc);            
            if (splitIDs != null) { 
              ppd.newRef = splitIDs[0];
              ppd.dof = null;
              didSplit = true;
             }
          }
          if (!didSplit && (dsnlsid == null)) {
            ppd.refNodeID = srcID;  // FIX ME!!! Maybe better re: target!
            ppd.offset = new Vector2D(osnp.getLocation(), movedLoc);
            ppd.fixedLinkID = null;
            Group grp = ((GenomeInstance)rcxB.getCurrentGenome()).getGroupForNode(srcID, GenomeInstance.ALWAYS_MAIN_GROUP);
            ppd.regionID = grp.getID();
            psd.assocRegions.add(grp.getID());
          }
        } else if ((ppd.regionBased != null) && (ppd.regionBased.partialDefinition != null)) {
          if ((changingGroups == null) || changingGroups.contains(ppd.regionID)) {
            ppd.regionBased.partialDefinition = null;  // FIX ME??
          }
        }
      }
    } 
    return;
  }
    
  /***************************************************************************
  **
  ** Go through the necessary boundary/resizing operations to get it so we can
  ** layout interregion links.
  */
  
  private LinkRouter.RoutingResult iterativeFreshMerge(StaticDataAccessContext rcxI,
                                                       Set<String> crossRegion,
                                                       Map<String, Rectangle> loBounds,
                                                       Map<String, Layout> layouts,
                                                       SortedMap<Integer, List<String>> regionsByColumn,
                                                       LayoutOptions options,
                                                       Map<String, String> linkColors, 
                                                       Layout.OverlayKeySet loModKeys,
                                                       Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts,
                                                       BTProgressMonitor monitor, 
                                                       double startFrac, double endFrac,
                                                       Map<String, BusProperties.RememberProps> rememberProps, boolean strictOKGroups)
                                                       throws AsynchExitRequestException {
 
    //
    // Figure out the links that are not already handled by the layout, and
    // add them here:
    //

    HashSet<String> todo = new HashSet<String>();
    Iterator<String> ctit = crossRegion.iterator();
    while (ctit.hasNext()) {
      String linkID = ctit.next();
      if (rcxI.getCurrentLayout().getLinkProperties(linkID) == null) {
        todo.add(linkID);
      }
    }

    //
    // If the layout works right away, we are done!
    //
    
    //
    // Figure out the padding between regions 
    //
                              
    int numRegions = layouts.size();
    int numCross = crossRegion.size();
    double borderSizeD = ((double)numCross / (double)numRegions) * 2.0;
    int borderSizeBase = (int)Math.ceil((borderSizeD == 0.0) ? 2.0 : borderSizeD);

    //
    // WJRL 9/18/08:  Disabling the iteration.  Practical experience shows that this
    // causes layout propagation to blow up.  We need to create a highly targeted
    // analysis of the failures and address them in a very focused manner, instead of
    // expanding whole region layouts.
    //
    
    // magic numbers; too big, and we can run out of memory 
    int[][] passes = {{borderSizeBase, 0}, 
                      {borderSizeBase, 1}, 
                      {2 * borderSizeBase, 0}, 
                      {3 * borderSizeBase, 0}, 
                      {3 * borderSizeBase, 1},
                      {3 * borderSizeBase, 2}};                      
    int numPasses = 1; // passes[0].length;  THIS is a fast way to eliminate the iteration...
   
    
    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / numPasses;    
    
    //
    // Try growing the borders first to see if that does the trick (FIX ME: better
    // feedback on why this expensive process is failing would be VERY useful!)
    // If it fails, then hit the suspect source and target regions with non-reversable
    // expansions.
    // 
    
    //
    // Note: DO NOT do reversable region expansions.  This is because the collapse of the
    // regions following the successful layout will not guarantee that interregion
    // links will be maintained.
    //    

    //
    // Loop to change bordersize:
    //
    
    Layout holdFirstLayout = null;
    HashMap<String, Rectangle> holdFirstBounds = null;
    LinkRouter.RoutingResult holdFirstRes = null;
    boolean basePass = true;
       
    for (int passNum = 0; passNum < numPasses; passNum++) {     
      //
      // Get temporary copies for layout and bounds:
      //
      StaticDataAccessContext rcxU = new StaticDataAccessContext(rcxI);
      rcxU.setLayout(new Layout("temp", rcxI.getCurrentGenomeID()));
      HashMap<String, Rectangle> useBounds = new HashMap<String, Rectangle>();
      copyResultsBack(rcxI.getCurrentLayout(), rcxU.getCurrentLayout(), loBounds, useBounds); 
      
    
      int border = passes[passNum][0];
      int mult = passes[passNum][1];
      
      LinkRouter.RoutingResult res = freshMerges(todo, border, mult, rcxU, useBounds, layouts,
                                                 regionsByColumn, options,
                                                 linkColors, loModKeys, moduleLinkFragShifts, monitor, 
                                                 currProg, currProg + progInc, rememberProps, strictOKGroups);
      currProg += progInc;
       
      //if (res.linkResult != LinkRouter.LAYOUT_OK) {
      if ((res.linkResult & LinkRouter.LAYOUT_PROBLEM) != 0x00) {        
        // Link routing can fail when two links are inbound onto the same pad.
        // We don't want to try to rectify this with a region growth approach,
        // so see if it is the case.
        if (basePass) {
          basePass = false;
          Set<String> targCollide = rcxI.getCurrentGenome().hasLinkTargetPadCollisions();
          if (!targCollide.isEmpty()) {
            copyResultsBack(rcxU.getCurrentLayout(), rcxI.getCurrentLayout(), useBounds, loBounds);
            return (res);
          } else {
            holdFirstLayout = rcxU.getCurrentLayout();
            holdFirstBounds = useBounds;
            holdFirstRes = res;
          }
        } 
      } else {
        copyResultsBack(rcxU.getCurrentLayout(), rcxI.getCurrentLayout(), useBounds, loBounds);
        return (res);
      }
    }
    
    copyResultsBack(holdFirstLayout, rcxI.getCurrentLayout(), holdFirstBounds, loBounds); // zero-expansion version...
    return (holdFirstRes);    
  }      
  
  /***************************************************************************
  **
  ** Do an incremental merge with specified boundary and non-reversable multiplier
  */
  
  private LinkRouter.RoutingResult freshMerges(Set<String> todo, int boundary, int mult, 
                                               StaticDataAccessContext rcxO, 
                                               Map<String, Rectangle> loBounds, 
                                               Map<String, Layout> layouts,
                                               SortedMap<Integer, List<String>> regionsByColumn,
                                               LayoutOptions options,
                                               Map<String, String> linkColors, 
                                               Layout.OverlayKeySet loModKeys, 
                                               Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts,
                                               BTProgressMonitor monitor, 
                                               double startFrac, double endFrac, 
                                               Map<String, BusProperties.RememberProps> rememberProps, boolean strictOKGroups)
                                                 throws AsynchExitRequestException {
 

    double fullFrac = endFrac - startFrac;
    double frac1 = fullFrac * 0.50;
    double eFrac1 = startFrac + frac1;
    double frac2 = fullFrac * 0.50;
    double eFrac2 = eFrac1 + frac2;      

    //
    // Do non-rev expansion here!
    //
                                                       
    Map<String, Layout> useLayouts = layouts; 
    Map<String, Rectangle> useBounds = loBounds;
                                                                                                             
    if (mult > 0) {
      useLayouts = new HashMap<String, Layout>();
      useBounds = new HashMap<String, Rectangle>();
      Set<String> lSet = layouts.keySet();
      int size = lSet.size();
      double currProg = startFrac;
      double progInc = frac1 / size;    
      
      Iterator<String> lit = lSet.iterator();
      while (lit.hasNext()) {
        String gpID = lit.next();
        Group grp = rcxO.getCurrentGenomeAsInstance().getGroup(gpID);
        StaticDataAccessContext rcxE = new StaticDataAccessContext(rcxO);
        rcxE.setLayout(new Layout(layouts.get(gpID)));
        useLayouts.put(gpID, rcxE.getCurrentLayout());
        Rectangle bounds = loBounds.get(gpID);
        useBounds.put(gpID, bounds);
        if (false /* FIX ME: gpID not a target or source*/) {
          continue;
        }
        TreeSet<Integer> insertRows = new TreeSet<Integer>();
        TreeSet<Integer> insertCols = new TreeSet<Integer>();
        
        rcxE.getCurrentLayout().chooseExpansionRows(rcxE, 1.0, 1.0, null, loModKeys, insertRows, insertCols, false, monitor);
        rcxE.getCurrentLayout().expand(rcxE, insertRows, insertCols, mult, true, null, moduleLinkFragShifts, gpID, monitor, currProg, currProg + progInc);
        currProg += progInc;
        bounds = rcxE.getCurrentLayout().getLayoutBoundsForGroup(grp, rcxE, true, true);        
        useBounds.put(gpID, bounds);        
      }                                                    
    }                   
    
    //
    // Note: DO NOT do reversable region expansions.  This is because the collapse of the
    // regions following the successful layout will not guarantee that interregion
    // links will be maintained.
    // 
    
    mergeGroupLayouts(rcxO.getCurrentLayout(), regionsByColumn, useLayouts, useBounds, moduleLinkFragShifts, boundary, monitor);
    
    // If there are no cross-region links, we are done without needing to do layouts
    if (todo.isEmpty()) {
      if (monitor != null) {
        if (!monitor.updateProgress((int)(endFrac * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
      return (new LinkRouter.RoutingResult());
    } 

    LinkRouter.RoutingResult res = 
      layoutSomeLinksCore(todo, rcxO, options,
                          linkColors, monitor, eFrac1, eFrac2, rememberProps, null, strictOKGroups);
    return (res);
  }
 
  /***************************************************************************
  **
  ** Remember link colors for when we redraw them.
  */
  
  private void buildLinkColorsMap(StaticDataAccessContext rcx, Map<String, String> colorMap) {

    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      BusProperties lp = rcx.getCurrentLayout().getLinkProperties(link.getID());
      if (lp != null) {
        colorMap.put(link.getID(), lp.getColorName());
      }
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Modify original cross-link geometry to handle growth
  */
  
  private Map<String, LinkPlacementGrid.RecoveryDataForSource> modifyCrossLinksForGrowthOrShrinkage(StaticDataAccessContext rcxB, CrossLinkRecovery recovery, 
                                                                                                    Map<String, Rectangle> newRegionBounds, 
                                                                                                    Map<Object, Vector2D> deltas, Map<String, Integer> paddings) {

    HashMap<String, LinkPlacementGrid.RecoveryDataForSource> retval = new HashMap<String, LinkPlacementGrid.RecoveryDataForSource>();
    HashMap<String, Rectangle> guaranteed = new HashMap<String, Rectangle>();
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      LinkPlacementGrid.RecoveryDataForSource rdfs = new LinkPlacementGrid.RecoveryDataForSource();
      retval.put(srcID, rdfs);
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      Map<LinkSegmentID, SortedSet<LinkSegmentID>> segmentToKidsMap = psd.fullTree.invertTree();
      LinkSegmentID rootSegID = psd.fullTree.getRootSegmentID();
      rdfs.setTreeTopology(segmentToKidsMap, rootSegID, psd.fullTree.isDirect() ? psd.fullTree.getSingleLinkage() : null);
      Set<String> exempt = recovery.exemptions.get(srcID);
      rdfs.setExemptions(exempt);
      Iterator<String> fsit = psd.perLinkMap.keySet().iterator();
      while (fsit.hasNext()) {
        String linkID = fsit.next();
        PerLinkData pld = psd.perLinkMap.get(linkID);
        rdfs.setSegListPerLink(linkID, pld.segIDList);
        rdfs.setBoundPointPerLink(linkID, pld.srcBoundPt);
      }
      Iterator<LinkSegmentID> ppit = psd.perPointMap.keySet().iterator();
      while (ppit.hasNext()) {
        LinkSegmentID lsid = ppit.next();
        PerPointData ppd = psd.perPointMap.get(lsid);
        if (ppd.fixedLinkID != null) {
          BusProperties bp = rcxB.getCurrentLayout().getLinkProperties(ppd.fixedLinkID);
          if (ppd.newRef != null) {
            lsid = ppd.newRef;  // Using a fixed-up split in the new layout
          }
          Point2D newLoc;
          if (bp == null) {
            // FIX ME: This should _never_ happen, but we _are_ seeing this trying to rebuild
            // an existing wkst network!
            // Seems to happen when one signal targets two bubbles, one in each of two targ regions, and one bubble has been dropped
            newLoc = new Point2D.Double(0.0, 0.0);
          } else if (lsid.isForStartDrop()) {
            LinkSegment lseg = bp.getRootSegment();
            newLoc = lseg.getStart();
          } else {
            LinkSegment lseg = bp.getSegment(lsid);
            if (lseg != null) {
              newLoc = lseg.getEnd();
            } else {
              // FIX ME: This should _never_ happen!
              newLoc = new Point2D.Double(0.0, 0.0);
            }
          }
          rdfs.putPointData(lsid, new LinkPlacementGrid.RecoveryPointData(newLoc, ppd.originalOrthoInbound, ppd.originalInboundRun, ppd.dof));
        } else if (ppd.refNodeID != null) {
          //
          // Precise new region bounds can be a little shaky.  If we have link points that
          // we said were within old bounds, make sure that we have new bounds to cover this, since
          // we are about to locate links based on these bounds, and we do not want to change
          // or overlap link ordering.
          //
          
          Rectangle guarBds = guaranteed.get(ppd.regionID);
          if (guarBds == null) {       
            Rectangle newBds = newRegionBounds.get(ppd.regionID);
            Vector2D delta = deltas.get(ppd.regionID);
            Integer paddingObj = paddings.get(ppd.regionID);
            int padding = (paddingObj == null) ? 0 : paddingObj.intValue();
            guarBds = new Rectangle(newBds.x - padding + (int)delta.getX(), newBds.y - padding + (int)delta.getY(), 
                                    newBds.width + (2 * padding), newBds.height + (2 * padding));
            guaranteed.put(ppd.regionID, guarBds);
          }
         
          NodeProperties np = rcxB.getCurrentLayout().getNodeProperties(ppd.refNodeID);
          Point2D loc = np.getLocation();
          Point2D newLoc = ppd.offset.add(loc);
          guarBds.add(newLoc);
          rdfs.putPointData(lsid, new LinkPlacementGrid.RecoveryPointData(newLoc, ppd.originalOrthoInbound, ppd.originalInboundRun, ppd.dof));
        }
      }
    }
     
    //
    // Region-based stuff is done on a second pass since it may need previously pinned points
    //
    
    rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      LinkPlacementGrid.RecoveryDataForSource rdfs = retval.get(srcID);
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      Iterator<LinkSegmentID> ppit = psd.perPointMap.keySet().iterator();
      while (ppit.hasNext()) {
        LinkSegmentID lsid = ppit.next();
        PerPointData ppd = psd.perPointMap.get(lsid); 
        if (ppd.regionBased != null) {
          Point2D newLoc = getNewRegionBasedLoc(guaranteed, newRegionBounds, ppd.regionBased, ppd.regionID, deltas);
          if (newLoc == null) {
            continue;
          }
          Point2D partialPos = null;
          if (ppd.regionBased.partialDefinitionByNodeID != null) {
            String nodeID = ppd.regionBased.partialDefinitionByNodeID;
            int padNum = ppd.regionBased.partialPadDefinition;
            NodeProperties np = rcxB.getCurrentLayout().getNodeProperties(nodeID); 
            Vector2D lpo;
            if (ppd.regionBased.partialIsLaunch) {
              lpo =  np.getRenderer().getLaunchPadOffset(padNum, rcxB.getCurrentGenome().getNode(nodeID), rcxB);
            } else {
              lpo =  np.getRenderer().getLandingPadOffset(padNum, rcxB.getCurrentGenome().getNode(nodeID), Linkage.NONE, rcxB);
            }
            partialPos = lpo.add(np.getLocation());
          } else if (ppd.regionBased.partialDefinition != null) {
            LinkPlacementGrid.RecoveryPointData rpd = rdfs.getRpd(ppd.regionBased.partialDefinition);
            if (rpd != null) {
              partialPos = rpd.point;
            }
          }
          if (partialPos != null) {
            if (ppd.regionBased.regionBasedApprox == Approx.Y_APPROX_) {
              newLoc.setLocation(newLoc.getX(), partialPos.getY());
            } else if (ppd.regionBased.regionBasedApprox == Approx.X_APPROX_) {
              newLoc.setLocation(partialPos.getX(), newLoc.getY());
            }
          }
          UiUtil.forceToGrid(newLoc, UiUtil.GRID_SIZE);
          rdfs.putPointData(lsid, new LinkPlacementGrid.RecoveryPointData(newLoc, ppd.originalOrthoInbound, ppd.originalInboundRun, ppd.dof));
        }
      }
    }
    
    //
    // For region relayout, we need to move the region chops around to match the region resize:
    //
    
    rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      LinkPlacementGrid.RecoveryDataForSource rdfs = retval.get(srcID);
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      rdfs.setDepartureRegionChops(psd.departChops);
      rdfs.setArrivalRegionChops(psd.arriveChops);
      if (psd.departChops != null) {
        Iterator<String> dcit = psd.departChops.keySet().iterator();
        while (dcit.hasNext()) {
          String linkID = dcit.next();
          EdgeMove em = psd.departChops.get(linkID);
          Point2D rbChop = getNewRegionBasedLoc(guaranteed, newRegionBounds, em.regPos, em.regionID, deltas);
          UiUtil.forceToGrid(rbChop, UiUtil.GRID_SIZE);
          //
          // Without this tweak inwards, we get cases where recovery does not
          // proceed.  Perhaps this point is driven by region, other point not,
          // and they are not well positioned wrt each other in close proximity?
          // Need to investigate....         
          em.pas.point = em.pas.outboundSideVec().scaled(-UiUtil.GRID_SIZE).add(rbChop);
        }
      }
      if (psd.arriveChops != null) {
        Iterator<String> acit = psd.arriveChops.keySet().iterator();
        while (acit.hasNext()) {
          String linkID = acit.next();
          EdgeMove em = psd.arriveChops.get(linkID);
          Point2D rbChop = getNewRegionBasedLoc(guaranteed, newRegionBounds, em.regPos, em.regionID, deltas);
          UiUtil.forceToGrid(rbChop, UiUtil.GRID_SIZE);
          em.pas.point = em.pas.outboundSideVec().scaled(-UiUtil.GRID_SIZE).add(rbChop);
        }
      }
    }
 
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Do calcs to derive new region-based location:
  */
  
  private Point2D getNewRegionBasedLoc(Map<String, Rectangle> guaranteed, 
                                       Map<String, Rectangle> newRegionBounds, 
                                       RegionBasedPosition rbp, String regionID, 
                                       Map<Object, Vector2D> deltas) {        
    Rectangle newBds = guaranteed.get(regionID);
    boolean gotGuar = (newBds != null);
    if (!gotGuar) {  
      newBds = newRegionBounds.get(regionID);
    }
    //
    // If an existing region disappears because it no longer contains interactions, region-based
    // calculations that depend on it cannot proceed.  The smart thing to do would involve having
    // a second-best fallback to rely on (FIX ME!).  In the meantime, to avoid the null pointer
    // exception, we just punt if we don't have anything to work with.
    //        
    if (newBds == null) {
      return (null);
    }
    Point2D newOrigin = new Point2D.Double(newBds.getX(), newBds.getY());
    if (!gotGuar) {  // FIX ME? What about padding too??
      Vector2D delta = deltas.get(regionID);
      newOrigin = delta.add(newOrigin);
    }
    double newX = rbp.regionBasedDef.origin.getX() * newBds.getWidth();
    double newY = rbp.regionBasedDef.origin.getY() * newBds.getHeight();
    Vector2D toPtOnBounds = new Vector2D(newX, newY);
    Point2D newLocOnBounds = toPtOnBounds.add(newOrigin);
    Point2D newLoc = rbp.regionBasedDef.offset.add(newLocOnBounds);
    return (newLoc);
  }
  
  /***************************************************************************
  **
  ** Figure out who we associate each cross path point with:
  */
  
  private void buildCrossPathRegionAssociations(StaticDataAccessContext rcx, 
                                                Map<String, Rectangle> oldRegionBounds, 
                                                Map<String, Link> crossTuples, 
                                                CrossLinkRecovery recovery, 
                                                Map<String, Map<LinkSegmentID, TreeSpliceData>> mergedSegsPerSource) {
    
 
    buildClaimedCrossPathRegionAssociations(rcx, oldRegionBounds, crossTuples, recovery, mergedSegsPerSource);
    buildRemainingCrossPathRegionAssociations(rcx, oldRegionBounds, crossTuples, recovery);
    findSourceRegionBoundingPoints(crossTuples, recovery);
    
    buildChoppieRegionAssociations(oldRegionBounds, recovery);
    
    return;   
  }
  
  /***************************************************************************
  **
  ** Figure out who we associate each cross path point with:
  */
  
  private void buildClaimedCrossPathRegionAssociations(StaticDataAccessContext rcx, 
                                                       Map<String, Rectangle> oldRegionBounds, 
                                                       Map<String, Link> crossTuples, 
                                                       CrossLinkRecovery recovery, 
                                                       Map<String, Map<LinkSegmentID, TreeSpliceData>> mergedSegsPerSource) {

    Set<String> ctLinks = crossTuples.keySet();
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      Map<LinkSegmentID, TreeSpliceData> mergedSegs = (mergedSegsPerSource == null) ? null : mergedSegsPerSource.get(srcID);
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      TreeSet<String> orderedKeys = new TreeSet<String>(psd.perLinkMap.keySet());  // Arbitrary, but fixed...
      Iterator<String> fsit = orderedKeys.iterator();    
      while (fsit.hasNext()) {
        String linkID = fsit.next(); 
        Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!
        String srcRegID = cfl.getSrc();
        String trgRegID = cfl.getTrg();
        Linkage linkage = rcx.getCurrentGenome().getLinkage(linkID);
        String linkSrc = linkage.getSource();
        String linkTrg = linkage.getTarget();
        PerLinkData pld = psd.perLinkMap.get(linkID);
        List<LinkSegmentID> existingSegs = pld.spTree.getSegIDListForSinglePath();    
        int numSegs = existingSegs.size();
        //
        // First pass: Capture the points in the source or target region.
        // Add blank entries for those that don't match:
        //
        for (int i = 0; i < (numSegs - 1); i++) { // Ignore target drop (numSegs - 1)
          LinkSegmentID nextSeg = existingSegs.get(i);
          pld.segIDList.add(nextSeg);
 
          //
          // Get the per point data out there if it is not already:
          //
          
          PerPointData ppd = psd.perPointMap.get(nextSeg);
          if (ppd == null) {
            ppd = new PerPointData();
            LinkSegment lseg = pld.spTree.getSegmentGeometryForID(nextSeg, rcx, false);
            ppd.originalLoc = lseg.getEnd();
            ppd.originalOrthoInbound = lseg.getRun().isCanonical();
            ppd.originalInboundRun = lseg.getRun().clone();
            ppd.dof = psd.fullTree.getCornerDoF(nextSeg, rcx, false);
            psd.perPointMap.put(nextSeg, ppd);
            ppd.refNodeID = null;
          }
          
          //
          // If the point was added via a synthesized tree, it needs to be referenced
          // to the target location:
          //          
          
          if (mergedSegs != null) {
            TreeSpliceData tsd = mergedSegs.get(nextSeg);
            if (tsd != null){
              if (!tsd.isBoundary) {
                ppd.regionID = tsd.trgGrpID;
                ppd.refNodeID = tsd.trgNodeID;
                psd.assocRegions.add(tsd.trgGrpID);
                ppd.originalLoc = null; // Bogus, and not used in refNodeCases...
              } else { // is a boundary pt
                //ppd.dof = ????? what the heck are we going to do with this point??
                ppd.originalOrthoInbound = tsd.originalOrthoInbound;
                ppd.originalInboundRun = tsd.originalInboundRun;
                ppd.regionID = tsd.trgGrpID;
                ppd.refNodeID = tsd.trgNodeID;
                psd.assocRegions.add(tsd.trgGrpID);
                //ppd.regionBased = tsd.spliceRbp;
              }            
            }
          }

          //
          // If the segment also belongs to a link that is in a given region, we can exactly
          // define the point.
          //          
     
          Set<String> linksThru = psd.fullTree.resolveLinkagesThroughSegment(nextSeg);       
          Iterator<String> ltit = linksThru.iterator();
          while (ltit.hasNext()) {
            String linkThruID = ltit.next();
            if (!ctLinks.contains(linkThruID)) {
              ppd.fixedLinkID = linkThruID;
              ppd.regionID = srcRegID;
              break;
            }
          } 

          //
          // If nobody has claimed the point as being in a source/target region, do that now:
          //
          
          if ((ppd.fixedLinkID == null) && (ppd.refNodeID == null)) {
            Iterator<String> orbit = oldRegionBounds.keySet().iterator();    
            while (orbit.hasNext()) {
              String key = orbit.next();
              Rectangle rect = oldRegionBounds.get(key);
              // rect contains() appears to not include pts. on the boundary.  DIY:
              // if (rect.contains(ppd.originalLoc)) {
              double orbMinX = rect.getMinX();
              double orbMinY = rect.getMinY();
              double orbMaxX = rect.getMaxX();
              double orbMaxY = rect.getMaxY();
              double olx = ppd.originalLoc.getX();
              double oly = ppd.originalLoc.getY();
              if ((olx >= orbMinX) && (olx <= orbMaxX) && (oly >= orbMinY) && (oly <= orbMaxY)) {             
                String addID = null;
                if (key.equals(srcRegID)) {
                  addID = linkSrc;
                } else if (key.equals(trgRegID)) {
                  addID = linkTrg;
                }
                //
                // If we are working with merged segments, then points inside the bounds of a region
                // that are not synthed should not be referenced to a target in the region, since 
                // the positional relationship is bogus 
                //                
                if ((addID != null) && (mergedSegs == null)) {
                  ppd.regionID = key;
                  ppd.refNodeID = addID;
                  psd.assocRegions.add(key);
                  break;
                }
              }
            }
          }
        }
      }
    }
    return;   
  }  
  
  /***************************************************************************
  **
  ** Figure out who we associate each cross path point with:
  */
  
  private void buildRemainingCrossPathRegionAssociations(StaticDataAccessContext rcx, 
                                                         Map<String, Rectangle> oldRegionBounds, 
                                                         Map<String, Link> crossTuples, CrossLinkRecovery recovery) {
    
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      TreeSet<String> orderedKeys = new TreeSet<String>(psd.perLinkMap.keySet());  // Arbitrary, but fixed...
      Iterator<String> fsit = orderedKeys.iterator();
      while (fsit.hasNext()) {
        String linkID = fsit.next();    
        Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!
        String srcRegID = cfl.getSrc();
        String trgRegID = cfl.getTrg();
        PerLinkData pld = psd.perLinkMap.get(linkID);
        int numSegs = pld.segIDList.size();
        for (int i = 0; i < numSegs; i++) {
          LinkSegmentID nextSeg = pld.segIDList.get(i);          
          PerPointData ppd = psd.perPointMap.get(nextSeg);
          if ((ppd.refNodeID == null) && (ppd.fixedLinkID == null) && (ppd.regionBased == null)) {               
            ppd.regionID = findClosestRegion(ppd.originalLoc, oldRegionBounds, srcRegID, trgRegID);
            Rectangle rect = oldRegionBounds.get(ppd.regionID);
            ppd.regionBased = closestPointOnBoundary(ppd.originalLoc, rect);
            fixDoFIfPossible(srcID, nextSeg, psd, ppd, rcx);   
            psd.assocRegions.add(ppd.regionID);
          }
        }
      }
    }
    return;   
  }
  
  /***************************************************************************
  **
  ** Figure out who we associate each cross path point with:
  */
  
  private void buildChoppieRegionAssociations(Map<String, Rectangle> oldRegionBounds, CrossLinkRecovery recovery) {
    
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      if (psd.departChops != null) {
        Iterator<String> dcit = psd.departChops.keySet().iterator();
        while (dcit.hasNext()) {
          String linkID = dcit.next();
          EdgeMove em = psd.departChops.get(linkID);
          Rectangle rect = oldRegionBounds.get(em.regionID);
          em.regPos = closestPointOnBoundary(em.getEdgePoint(), rect);
         // fixDoFIfPossible(srcID, nextSeg, psd, ppd, genome,lo, frc);   
          psd.assocRegions.add(em.regionID);
        }
      }
      if (psd.arriveChops != null) {
        Iterator<String> acit = psd.arriveChops.keySet().iterator();
        while (acit.hasNext()) {
          String linkID = acit.next();
          EdgeMove em = psd.arriveChops.get(linkID);
          Rectangle rect = oldRegionBounds.get(em.regionID);
          em.regPos = closestPointOnBoundary(em.getEdgePoint(), rect);
         // fixDoFIfPossible(srcID, nextSeg, psd, ppd, genome,lo, frc);   
          psd.assocRegions.add(em.regionID);
        }
      }
    }
    return;   
  }
 
  /***************************************************************************
  **
  ** Find out which points are the first ones outside the source region.
  ** When the source region layout is completely changed, this gives us
  ** the point when we can start to pick up the pieces to recover the
  ** rest of the link...
  */
  
  private void findSourceRegionBoundingPoints(Map<String, Link> crossTuples, CrossLinkRecovery recovery) {
    
    //
    // For each target, walk up the link until we hit a point that is defined by
    // a fixed linkID or reference node that is inside the source region.
    //
    
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      Set<String> keys = psd.perLinkMap.keySet();
      Iterator<String> kit = keys.iterator();    
      while (kit.hasNext()) {
        String linkID = kit.next();    
        Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!
        String srcRegID = cfl.getSrc();
        PerLinkData pld = psd.perLinkMap.get(linkID);
        pld.srcBoundPt = null;
        int numSegs =  pld.segIDList.size();
        LinkSegmentID lastSeg = null;
        for (int i = numSegs - 1; i >= 0; i--) {
          LinkSegmentID nextSeg = pld.segIDList.get(i);
          PerPointData ppd = psd.perPointMap.get(nextSeg);
          if ((ppd.fixedLinkID != null) || (ppd.refNodeID != null)) {
            if (srcRegID.equals(ppd.regionID)) {
              pld.srcBoundPt = (lastSeg == null) ? nextSeg : lastSeg;
              break;
            }
          }
          lastSeg = nextSeg;
        }
        if (pld.srcBoundPt == null) {
          pld.srcBoundPt = lastSeg;
        }
      }
    }
    return;   
  }    
  
  /***************************************************************************
  **
  ** Like the above, but we find the last point before entering the target region

  
  private void findTargetRegionBoundingPoints(Map crossTuples, CrossLinkRecovery recovery) {
    
    //
    // For each target, walk up the link until we hit a point that is not defined by
    // a reference node that is inside the target region.
    //
    
    Iterator rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = (String)rpit.next();
      PerSrcData psd = (PerSrcData)recovery.perSrcMap.get(srcID);
      Set keys = psd.perLinkMap.keySet();
      Iterator kit = keys.iterator();    
      while (kit.hasNext()) {
        String linkID = (String)kit.next();    
        Link cfl = (Link)crossTuples.get(linkID);  // Src, Trg actually region IDs!
        String trgRegID = cfl.getTrg();
        PerLinkData pld = (PerLinkData)psd.perLinkMap.get(linkID);
        pld.trgBoundPt = null;
        int numSegs =  pld.segIDList.size();
        LinkSegmentID lastSeg = null;
        for (int i = numSegs - 1; i >= 0; i--) {
          LinkSegmentID nextSeg = (LinkSegmentID)pld.segIDList.get(i);
          PerPointData ppd = (PerPointData)psd.perPointMap.get(nextSeg);
          if ((ppd.refNodeID != null) && trgRegID.equals(ppd.regionID)) {
            pld.trgBoundPt = (lastSeg == null) ? nextSeg : lastSeg;
            break;
          }
          lastSeg = nextSeg;
        }
        if (pld.trgBoundPt == null) {
          pld.trgBoundPt = lastSeg;
        }
      }
    }
    return;   
  }  

  /***************************************************************************
  **
  ** Find out if a floating point has one DOF fixed by another end of the
  ** segment.
  */
  
  private void fixDoFIfPossible(String src, LinkSegmentID lsid, PerSrcData psd, PerPointData myPpd, StaticDataAccessContext rcx) {

    List<LinkSegmentID> kidSegs = psd.fullTree.getChildSegs(lsid);
    int numKs = kidSegs.size();
    for (int i = 0; i < numKs; i++) {
      LinkSegmentID nextSeg = kidSegs.get(i);
      LinkSegment geomSeg =  psd.fullTree.getSegmentGeometryForID(nextSeg, rcx, false);
      Vector2D kidRun = geomSeg.getRun();
      if ((kidRun != null) && kidRun.isCanonical()) {
        if (((kidRun.getY() == 0.0) && (myPpd.regionBased.regionBasedApprox == Approx.Y_APPROX_)) ||
            ((kidRun.getX() == 0.0) && (myPpd.regionBased.regionBasedApprox == Approx.X_APPROX_))) {
          if (nextSeg.isForEndDrop()) {
            String linkID = nextSeg.getEndDropLinkRef(); 
            Linkage link = rcx.getCurrentGenome().getLinkage(linkID);              
            myPpd.regionBased.partialDefinitionByNodeID = link.getTarget();
            myPpd.regionBased.partialPadDefinition = link.getLandingPad();
            myPpd.regionBased.partialIsLaunch = false;
          } else {
            myPpd.regionBased.partialDefinition = nextSeg;
          }
          return;
        }
      }
    }
    
    LinkSegment myGeom = psd.fullTree.getSegmentGeometryForID(lsid, rcx, false);

    if (myGeom != null) {
      Vector2D myRun = myGeom.getRun();
      if ((myRun != null) && myRun.isCanonical()) {
        if (((myRun.getY() == 0.0) && (myPpd.regionBased.regionBasedApprox == Approx.Y_APPROX_)) ||
            ((myRun.getX() == 0.0) && (myPpd.regionBased.regionBasedApprox == Approx.X_APPROX_))) {
          if (lsid.isForStartDrop()) {
            myPpd.regionBased.partialDefinitionByNodeID = src;   
            myPpd.regionBased.partialPadDefinition = rcx.getCurrentGenome().getSourcePad(src).intValue();
            myPpd.regionBased.partialIsLaunch = true;
          } else {
            myPpd.regionBased.partialDefinition = psd.fullTree.getSegmentIDForParent(lsid);
          }
          
          return;
        }
      }
    }
    return;   
  }       
      
   
  /***************************************************************************
  **
  ** Find closest region
  */
  
  private String findClosestRegion(Point2D nextPt, Map<String, Rectangle> oldRegionBounds, String srcRegID, String trgRegID) {
    Iterator<String> orbit = oldRegionBounds.keySet().iterator();
    double minDist = Double.POSITIVE_INFINITY;
    double srcDist = Double.POSITIVE_INFINITY;
    double trgDist = Double.POSITIVE_INFINITY;
    String retval = null;
    while (orbit.hasNext()) {
      String key = orbit.next();
      Rectangle rect = oldRegionBounds.get(key);    
      double dist = distanceToRegion(nextPt, rect);
      if (dist < minDist) {
        minDist = dist;
        retval = key;
      }
      if (key.equals(srcRegID)) {
        srcDist = dist;
      }
      if (key.equals(trgRegID)) {
        trgDist = dist;
      }
    }
    //
    // In a tie, we go with source or target region (source trumps targ)
    //
    if (minDist == trgDist) {
      retval = trgRegID;
    }
    if (minDist == srcDist) {
      retval = srcRegID;
    }
    return (retval);
  }    

  /***************************************************************************
  **
  ** Get the distance of a point to a region:
  */
  
  private double distanceToRegion(Point2D nextPt, Rectangle regionRect) {
    if (regionRect.contains(nextPt)) {
      return (0.0);
    }
    Rectangle2D bounds2D = new Rectangle2D.Double(regionRect.getX(), regionRect.getY(), 
                                                  regionRect.getWidth(), regionRect.getHeight());  
    bounds2D.add(nextPt);
    double deltaW = bounds2D.getWidth() - regionRect.getWidth();
    double deltaH = bounds2D.getHeight() - regionRect.getHeight();
    return (Math.sqrt((deltaW * deltaW) + (deltaH * deltaH)));
  }  
  
  /***************************************************************************
  **
  ** Get the closest point on the region boundary to the given point, with offset vector.
  */
  
  private RegionBasedPosition closestPointOnBoundary(Point2D nextPt, Rectangle regionRect) {
    Point2D origin = new Point2D.Double(regionRect.getX(), regionRect.getY());
    Point2D urpt = new Point2D.Double(regionRect.getX() + regionRect.getWidth(), regionRect.getY());
    Point2D llpt = new Point2D.Double(regionRect.getX(), regionRect.getY() + regionRect.getHeight());
    Point2D lrpt = new Point2D.Double(regionRect.getX() + regionRect.getWidth(), regionRect.getY() + regionRect.getHeight());
     
    PointAndVec minPav;
    
    
    PointAndVec pavTop = closestPointOnSegment(origin, urpt, nextPt);
    double minDist = pavTop.offset.length();
    minPav = pavTop;
    
    PointAndVec pavBot = closestPointOnSegment(llpt, lrpt, nextPt);
    double currDist = pavBot.offset.length();
    if (currDist < minDist) {
      minDist = currDist;
      minPav = pavBot; 
    }
   
    PointAndVec pavLeft = closestPointOnSegment(origin, llpt, nextPt);
    currDist = pavLeft.offset.length();
    if (currDist < minDist) {
      minDist = currDist;
      minPav = pavLeft;
    }
       
    PointAndVec pavRight = closestPointOnSegment(urpt, lrpt, nextPt);
    currDist = pavRight.offset.length();
    if (currDist < minDist) {
      minDist = currDist;
      minPav = pavRight; 
    }
    
    Approx approxDir = Approx.NO_APPROX_;
    // Yes, these are ==
    if ((minPav == pavTop) || (minPav == pavBot)) { 
      approxDir = Approx.X_APPROX_;
    } else {
      approxDir = Approx.Y_APPROX_;     
    }
        
    double normX = (minPav.origin.getX() - regionRect.getX()) / regionRect.getWidth();
    double normY = (minPav.origin.getY() - regionRect.getY()) / regionRect.getHeight(); 
    RegionBasedPosition retval = new RegionBasedPosition();
    retval.regionBasedDef = new PointAndVec(new Point2D.Double(normX, normY), minPav.offset);   
    retval.regionBasedApprox = approxDir;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the closest point on the segment to the given point, with offset vector
  */
  
  private PointAndVec closestPointOnSegment(Point2D startPt, Point2D endPt, Point2D targ) {
    Vector2D run = new Vector2D(startPt, endPt);
    double length = run.length();
    run = run.normalized();
    Vector2D toPt = new Vector2D(startPt, targ);
    double runDot = run.dot(toPt);
    PointAndVec pav; 
    if (runDot < 0.0) {
      Vector2D offset = new Vector2D(startPt, targ);      
      pav = new PointAndVec((Point2D)startPt.clone(), offset);
    } else if (runDot > length) {
      Vector2D offset = new Vector2D(endPt, targ);      
      pav = new PointAndVec((Point2D)endPt.clone(), offset);
    } else {
      Vector2D delta = run.scaled(runDot);
      Point2D closePt = delta.add(startPt);
      Vector2D offset = new Vector2D(closePt, targ);      
      pav = new PointAndVec(closePt, offset);      
    }
    return (pav);
  }    
   
  /***************************************************************************
  **
  ** Apply corresponding region expansions to the cross region links.
  */
  
  static void expandOrCompressRecoveryPaths(boolean doExpand, Layout lo,
                                            String grpID, SortedSet<Integer> rows, 
                                            SortedSet<Integer> cols, int mult, CrossLinkRecovery recovery) {

    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      if (psd.assocRegions.contains(grpID)) {
        BusProperties treeCopy = psd.fullTree.clone();
        if (doExpand) {
          treeCopy.expand(rows, cols, mult); 
        } else {
          treeCopy.compress(rows, cols, null);    
        }
        Iterator<LinkSegmentID> ppit = psd.perPointMap.keySet().iterator();    
        while (ppit.hasNext()) {
          LinkSegmentID segID = ppit.next();
          PerPointData ppd = psd.perPointMap.get(segID);
          if (ppd.refNodeID != null) {
            if (ppd.regionID.equals(grpID)) {
              Point2D pt;
              if (segID.isForStartDrop()) {
                 LinkSegment lseg = treeCopy.getRootSegment();
                 pt = lseg.getStart();
              } else {
                 LinkSegment lseg = treeCopy.getSegment(segID);
                 pt = lseg.getEnd();
              }
              NodeProperties np = lo.getNodeProperties(ppd.refNodeID);
              Point2D loc = np.getLocation();
              ppd.offset = new Vector2D(loc, pt);
            }         
          }
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Fix cross region link definitions in the case of unchanged links
  */
  
  private void pinUnchangedRecoveryPaths(Layout lo, String grpID, CrossLinkRecovery recovery, Vector2D delta) {

    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      if (psd.assocRegions.contains(grpID)) {
        BusProperties treeCopy = psd.fullTree.clone();
        Iterator<LinkSegmentID> ppit = psd.perPointMap.keySet().iterator();    
        while (ppit.hasNext()) {
          LinkSegmentID segID = ppit.next();
          PerPointData ppd = psd.perPointMap.get(segID);
          if ((ppd.refNodeID != null) && (ppd.offset == null)) {
            if (ppd.regionID.equals(grpID)) {
              Point2D pt;
              if (segID.isForStartDrop()) {
                 LinkSegment lseg = treeCopy.getRootSegment();
                 pt = lseg.getStart();
              } else {
                 LinkSegment lseg = treeCopy.getSegment(segID);
                 pt = lseg.getEnd();
              }
              NodeProperties np = lo.getNodeProperties(ppd.refNodeID);
              Point2D loc = np.getLocation();
              if (delta != null) {
                loc = delta.scaled(-1.0).add(loc);
              }
              ppd.offset = new Vector2D(loc, pt);
            }         
          }
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle growth of region.
  */
  
  private Map<Object, Vector2D> calcRegionGrowthOffsets(Map<String, Rectangle> newRegionBounds, 
                                                        Map<String, Rectangle> savedRegionBounds, 
                                                        List<PlacementElement> fixedElements, 
                                                        Map<String, Integer> newPaddings) {

    //
    // Figure out how much each region needs to grow, and calculate the growth 
    //
    
    ArrayList<GridGrower.GeneralizedGridElement> gridElements = new ArrayList<GridGrower.GeneralizedGridElement>();
    Iterator<String> nrbit = newRegionBounds.keySet().iterator();
    while (nrbit.hasNext()) {
      String nextKey = nrbit.next();
      Rectangle newBounds = newRegionBounds.get(nextKey);
      Rectangle savedBounds = savedRegionBounds.get(nextKey);  // FIX ME: need old->new ID mapping?
      if (savedBounds == null) {  // Not present in original layout
        continue;
      }
      int hDiff = newBounds.height - savedBounds.height;
      int wDiff = newBounds.width - savedBounds.width;
      if (hDiff < 0) hDiff = 0;
      if (wDiff < 0) wDiff = 0;
      Integer newPaddingObj = newPaddings.get(nextKey);
      int newPadding = (newPaddingObj != null) ? newPaddingObj.intValue() : 0;
      hDiff += (2 * newPadding);
      wDiff += (2 * newPadding);
      GridElement ge = new GridElement(savedBounds, nextKey, wDiff, hDiff);
      gridElements.add(ge);
    }
    
    GridGrower grower = new GridGrower();    
    List<GridGrower.GeneralizedGridElement> grown = grower.growGrid(gridElements, GridGrower.GrowthTypes.MIN_GROWTH_CENTERED);
    
    HashMap<Object, Vector2D> retval = new HashMap<Object, Vector2D>();
    
    int geSize = gridElements.size();
    for (int i = 0; i < geSize; i++) {
      GridElement origGe = (GridElement)gridElements.get(i);
      GridElement newGe = (GridElement)grown.get(i);
      Rectangle newBounds = newRegionBounds.get(origGe.id);
      Integer newPaddingObj = newPaddings.get(origGe.id);
      int newPadding = (newPaddingObj != null) ? newPaddingObj.intValue() : 0;
      double deltaX = (origGe.rect.getX() - newBounds.x) + (newGe.rect.getX() - origGe.rect.getX()) + newPadding;
      deltaX = UiUtil.forceToGridValue(deltaX, UiUtil.GRID_SIZE);
      double deltaY = (origGe.rect.getY() - newBounds.y) + (newGe.rect.getY() - origGe.rect.getY()) + newPadding;      
      deltaY = UiUtil.forceToGridValue(deltaY, UiUtil.GRID_SIZE);
      Vector2D delta = new Vector2D(deltaX, deltaY);
      retval.put(origGe.id, delta);
      
      if (fixedElements != null) {
        //Rectangle newBounds = (Rectangle)newRegionBounds.get(origGe.id);
        // are forcings really needed??
        int x = (int)UiUtil.forceToGridValue(newBounds.x + (int)deltaX, UiUtil.GRID_SIZE);
        int y = (int)UiUtil.forceToGridValue(newBounds.y + (int)deltaY, UiUtil.GRID_SIZE);
        int w = (int)UiUtil.forceToGridValue(newBounds.width, UiUtil.GRID_SIZE);
        int h = (int)UiUtil.forceToGridValue(newBounds.height, UiUtil.GRID_SIZE);
        Rectangle fixedRect = new Rectangle(x, y, w, h);
        PlacementElement newPe = new PlacementElement(fixedRect, origGe.id.toString());
        fixedElements.add(newPe);
      }
    }
    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Sort the regions based on cross region links
  */
  
  private SortedMap<Integer, List<String>> sortRegions(Set<String> allGroups, Map<String, Link> linkTuples) {   
  
    HashSet<String> regs = new HashSet<String>(allGroups);
    HashSet<Link> links = new HashSet<Link>();    
    Iterator<String> xrit = linkTuples.keySet().iterator();
    while (xrit.hasNext()) {
      String linkID = xrit.next();
      Link cfl = linkTuples.get(linkID);
      links.add(cfl);
      CycleFinder cf = new CycleFinder(regs, links);
      if (cf.hasACycle()) {
        links.remove(cfl);
      }
    }   
        
    GraphSearcher searcher = new GraphSearcher(regs, links);    
    Map<String, Integer> topoSort = searcher.topoSort(false);
    
    //
    // Invert the map
    //
    
    TreeMap<Integer, List<String>> invert = new TreeMap<Integer, List<String>>();
    searcher.invertTopoSort(topoSort, invert);
    return (invert);
  }
    
  /***************************************************************************
  **
  ** Squash the layout for the group
  */
  
  private void squash(StaticDataAccessContext rcx,
                      Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts, 
                      String grpID, 
                      BTProgressMonitor monitor, double startFrac, double endFrac) throws AsynchExitRequestException { 
         
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor); 
    SortedSet<Integer> emptyRows = routerGrid.getEmptyRows(null, false, monitor);
    SortedSet<Integer> emptyCols = routerGrid.getEmptyColumns(null, false, monitor);      
    rcx.getCurrentLayout().compress(rcx, emptyRows, emptyCols, null, null, moduleLinkFragShifts, grpID, monitor, startFrac, endFrac);  
    return;
  }
  
  /***************************************************************************
  **
  ** Squash the layout for the group, using precalulated region bounds to make much
  ** more efficient
  */
  
  private void boundedSquash(StaticDataAccessContext rcx, Rectangle bounds, BTProgressMonitor monitor, 
                             double startFrac, double endFrac) throws AsynchExitRequestException {
        
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor); 

    SortedSet<Integer> emptyRows;
    SortedSet<Integer> emptyCols;   
    
    if (bounds != null) {
      MinMax xBounds = new MinMax(bounds.x, bounds.x + bounds.width);
      MinMax yBounds = new MinMax(bounds.y, bounds.y + bounds.height);
      emptyRows = routerGrid.getEmptyRowsWithYBounds(xBounds, yBounds, false, monitor);
      emptyCols = routerGrid.getEmptyColumnsWithXBounds(yBounds, xBounds, false, monitor);
    } else {
      emptyRows = routerGrid.getEmptyRows(null, false, monitor);
      emptyCols = routerGrid.getEmptyColumns(null, false, monitor);      
    }
    
    rcx.getCurrentLayout().compress(rcx, emptyRows, emptyCols, bounds, null, null, null, monitor, startFrac, endFrac);
    return;
  }  
  
  /***************************************************************************
  **
  ** Squash the bounds

  
  private void squashRectangle(SortedSet emptyRows, SortedSet emptyCols, Rectangle bounds) {
   
    //
    // rows
    //
    int originReduction = 0;
    int dimReduction = 0;
   
    int min = bounds.y;
    int max = bounds.y + bounds.height;
    
    Iterator erit = emptyRows.iterator();
    while (erit.hasNext()) {
      Integer rowObj = (Integer)erit.next();
      int row = rowObj.intValue() * UiUtil.GRID_SIZE_INT;
      if (row < min) {
        originReduction += UiUtil.GRID_SIZE_INT;
      } else if (row < max) {
        dimReduction += UiUtil.GRID_SIZE_INT;
      }
    }
    
    bounds.y -= originReduction;
    bounds.height -= dimReduction;
    
    //
    // cols
    //
    
    originReduction = 0;
    dimReduction = 0;
   
    min = bounds.x;
    max = bounds.x + bounds.width;
    
    Iterator ecit = emptyCols.iterator();
    while (ecit.hasNext()) {
      Integer colObj = (Integer)ecit.next();
      int col = colObj.intValue() * UiUtil.GRID_SIZE_INT;
      if (col < min) {
        originReduction += UiUtil.GRID_SIZE_INT;
      } else if (col < max) {
        dimReduction += UiUtil.GRID_SIZE_INT;
      }
    }
    
    bounds.x -= originReduction;
    bounds.width -= dimReduction; 

    return;
  }  

  /***************************************************************************
  **
  ** Build a layout that contains all the elements needed to synchronize a region
  ** in a child model, i.e. all the nodes and links, plus sources and links for
  ** inbound links!
  */
  
  private void chooseReducedRootCompression(StaticDataAccessContext rcxRLGI,
                                            String grpID,
                                            SortedSet<Integer> useEmptyRows, SortedSet<Integer> useEmptyCols,
                                            BTProgressMonitor monitor)
                                              throws AsynchExitRequestException {  
    //
    // Build separate layouts for each:
    //
   
    Genome rootGenome = rcxRLGI.getGenomeSource().getRootDBGenome();
    GenomeInstance gi = (GenomeInstance)rcxRLGI.getCurrentGenome();
    
 //   String giID = gi.getID(); Wrong! Not needed!
    HashMap<String, String> nodeMap = new HashMap<String, String>();
    HashMap<String, String> linkMap = new HashMap<String, String>();
    Group grp = gi.getGroup(grpID);
    if (grp.isASubset(gi)) { 
      throw new IllegalArgumentException(); // should not happen
    }
    
    Iterator<GroupMember> mit = grp.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      nodeMap.put(baseID, baseID);
    }

    HashSet<String> ignoreNodes = new HashSet<String>();
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String linkID = link.getID();
      String baseID = GenomeItemInstance.getBaseID(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      if (!grp.isInGroup(trg, gi)) {
        continue;
      }
      linkMap.put(baseID, baseID);
      if (!grp.isInGroup(src, gi)) {
        String baseSrc = GenomeItemInstance.getBaseID(src);
        nodeMap.put(baseSrc, baseSrc);
        ignoreNodes.add(baseSrc);
      }
    }   
  
    //
    // Choice of compression rows from root can't use the root overlay info in a useful fashion,
    // so we don't extract it: overlay maps are null, null, null:
    // FIX ME: We have a problem though, in that the compression choice does not account for overlay needs.
    // One possibility would be to install overlay info before compressing.  Would probably need to be
    // sliced overlay geometry that is isolated to just the region undergoing compression.
    // See issue BT-08-07-09:1
    //
    
    Layout newLayout = new Layout("junkID", rootGenome.getID()); // Was giID); , but I think that is wrong....
    StaticDataAccessContext rcx = new StaticDataAccessContext(rcxRLGI, rootGenome, newLayout);
    rcx.getCurrentLayout().extractPartialLayout(nodeMap, linkMap, null, null, null, null, false, false, null, rcx, rcxRLGI);     
    rcx.getCurrentLayout().chooseCompressionRows(rcx, 1.0, 1.0, null, false, null, useEmptyRows, useEmptyCols, monitor, ignoreNodes);
    return;   
  }
  
  /***************************************************************************
  **
  ** Figure out how modules are sliced up by group boundaries.
  */
  
  private void sliceAndDiceModulesForGroups(StaticDataAccessContext rcx,
                                            Set<String> allGroups, Layout.OverlayKeySet loModKeys, 
                                            Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                                            Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices, 
                                            Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts) {
      
    Point2D center = rcx.getCurrentLayout().getLayoutCenter(rcx, false, null, null, null, null);
       
    //
    // Gather up group bounds:
    //
    
    GenomeInstance gi = (GenomeInstance)rcx.getCurrentGenome();
    HashMap<String, Rectangle> forcedBounds = new HashMap<String, Rectangle>();
    HashMap<String, Integer> groupZOrder = new HashMap<String, Integer>();
    Iterator<String> agit = allGroups.iterator(); 
    while (agit.hasNext()) {
      String grpID = agit.next();
      groupZOrder.put(grpID, new Integer(rcx.getCurrentLayout().getGroupProperties(grpID).getOrder()));
      Group grp = gi.getGroup(grpID);
      if (grp.isASubset(gi)) { 
        throw new IllegalArgumentException(); // should not happen
      }
      Rectangle bounds = rcx.getCurrentLayout().getLayoutBoundsForGroup(grp, rcx, false, true);
      if (bounds != null) {
        UiUtil.forceToGrid(bounds, UiUtil.GRID_SIZE);
        forcedBounds.put(grpID, bounds);     
      }      
    }
   
    NetModuleShapeFixer nms = new NetModuleShapeFixer();
    
    //
    // Slice all the modules for the bounds:
    //

    Map<NetModule.FullModuleKey, NetModuleProperties.SliceResult> sliceResults = 
      rcx.getCurrentLayout().sliceModulesByAllBounds(forcedBounds, groupZOrder, loModKeys, rcx);
 
    // Bundle up per-group ownership and unclaimed:
    
    Iterator<NetModule.FullModuleKey> slit = sliceResults.keySet().iterator();
    while (slit.hasNext()) {
      NetModule.FullModuleKey fullKey = slit.next();
      NetModuleProperties.SliceResult sr = sliceResults.get(fullKey);
      //
      // Owned slices:
      //
      Iterator<String> oit = sr.ownedByGroup.keySet().iterator();
      while (oit.hasNext()) {
        String ownerGrpID = oit.next();
        
        Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> perGroup = claimedSlices.get(ownerGrpID);
        if (perGroup == null) {
          perGroup = new HashMap<NetModule.FullModuleKey, Layout.DicedModuleInfo>();
          claimedSlices.put(ownerGrpID, perGroup);
        }
        Layout.DicedModuleInfo shapesPerKey = perGroup.get(fullKey);
        //
        // By sticking the existing key into the diced module info, this results in
        // an identity map on full keys (which is appropriate for keeping the current
        // overlay/module set:
        //
        if (shapesPerKey == null) {
          shapesPerKey = new Layout.DicedModuleInfo(fullKey);
          perGroup.put(fullKey, shapesPerKey);
        }
        List<NetModuleProperties.TaggedShape> shapes = sr.ownedByGroup.get(ownerGrpID);
        shapesPerKey.dicedShapes.addAll(shapes);
      }
            
      //
      // Unclaimed slices are defined in terms of claimed slices:
      //
          
      ArrayList<NetModuleProperties.TaggedShape> allUnclaimed = new ArrayList<NetModuleProperties.TaggedShape>();
      allUnclaimed.addAll(sr.unclaimed);
      int numMult = sr.multiClaimed.size();
      for (int i = 0; i < numMult; i++) {
        NetModuleProperties.MultiClaim mc = sr.multiClaimed.get(i);       
        allUnclaimed.add(new NetModuleProperties.TaggedShape(null, mc.shape, mc.isName));
      }
     
      List<NetModuleShapeFixer.RectResolution> relocInfo = nms.getModuleRelocInfoForRegions(claimedSlices, allUnclaimed, fullKey, center);     
      Layout.DicedModuleInfo shapesPerKey = new Layout.DicedModuleInfo(fullKey);
      shapesPerKey.dicedRectByRects.addAll(relocInfo);
      unclaimedSlices.put(fullKey, shapesPerKey);
    }
    
    //
    // Assign module link fragments to the different bounds here:
    //
        
    if (moduleLinkFragShifts != null) {
      moduleLinkFragShifts.putAll(rcx.getCurrentLayout().sliceModuleLinksByAllBounds(forcedBounds, groupZOrder, loModKeys));
    }
 
    return;
  }   
  
  
  /***************************************************************************
  **
  ** Build separate layouts for each group from a master layout
  */
  
  private ClaimedAndUnclaimed buildGroupLayouts(StaticDataAccessContext rcxB,
                                                Map<String, Layout> layouts, Map<String, Rectangle> loBounds, 
                                                Set<String> allGroups, Layout.OverlayKeySet loModKeys,
                                                boolean baseIsRoot, boolean doSquash,
                                                Map<String, GroupProperties> gpMemory,
                                                Map<String, Empties> emptiesForGroups, 
                                                Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts, boolean keepOverlays,
                                                BTProgressMonitor monitor, double startFrac, double endFrac)
                                                  throws AsynchExitRequestException {

    FullGenomeHierarchyOracle fgho = rcxB.getFGHO();
    
    //
    // Slice and dice the modules at the group boundaries so they can be manipulated and
    // then merged independently.
   
    Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices = null; 
    Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices = null;
    if (!baseIsRoot) {
      if ((loModKeys != null) && !loModKeys.isEmpty()) {
        claimedSlices = new HashMap<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>>();
        unclaimedSlices = new HashMap<NetModule.FullModuleKey, Layout.DicedModuleInfo>();
        sliceAndDiceModulesForGroups(rcxB, allGroups, loModKeys, claimedSlices, unclaimedSlices, moduleLinkFragShifts);
      }
    }
 
    //
    // Build separate layouts for each:
    //    

    GenomeInstance gi = (GenomeInstance)rcxB.getCurrentGenome();
    HashMap<String, Integer> desiredOrders = new HashMap<String, Integer>();
    TreeSet<Integer> seenOrders = new TreeSet<Integer>();
    String giID = gi.getID();
    Iterator<String> agit = allGroups.iterator();
    int count = 0;
    
    while (agit.hasNext()) {
      String grpID = agit.next();
      HashMap<String, String> nodeMap = new HashMap<String, String>();
      HashMap<String, String> linkMap = new HashMap<String, String>();
      Group grp = gi.getGroup(grpID);
      if (grp.isASubset(gi)) { 
        throw new IllegalArgumentException(); // should not happen
      }
      HashSet<Point2D> pts = new HashSet<Point2D>();
      Iterator<GroupMember> mit = grp.getMemberIterator();
      while (mit.hasNext()) {
        GroupMember mem = mit.next();
        String nodeID = mem.getID();
        String baseID = (baseIsRoot) ? GenomeItemInstance.getBaseID(nodeID) : nodeID;
        NodeProperties np = rcxB.getCurrentLayout().getNodeProperties(baseID);
        if (np == null) {  // for partial layouts...
          continue;
        }
        nodeMap.put(nodeID, baseID);
        Point2D location = np.getLocation();
        pts.add(location);
      }
      // FIX ME?  Best way to handle empty groups?
      Point2D center = (pts.isEmpty()) ? new Point2D.Double() 
                                       : AffineCombination.combination(pts, UiUtil.GRID_SIZE);
    
      Set<String> groupNodes = grp.areInGroup(gi);
   
      Iterator<Linkage> glit = gi.getLinkageIterator();
      while (glit.hasNext()) {
        Linkage link = glit.next();
        String linkID = link.getID();
        String baseID = (baseIsRoot) ? GenomeItemInstance.getBaseID(linkID) : linkID;        
        BusProperties lp = rcxB.getCurrentLayout().getLinkProperties(baseID);
        if (lp == null) {  // for partial layouts...
          continue;
        }
        String src = link.getSource();
        String trg = link.getTarget();
        if (!groupNodes.contains(src)) {
          continue;
        }
        if (!groupNodes.contains(trg)) {
          continue;
        }
        linkMap.put(linkID, baseID);
      }
      
      // When building a reduced layout for just a single region, we need to retain
      // any overlay data for modules associated with the region.  Generate maps
      // that are used for this purpose          
      
      // NOTE THE modLinkIDMap BEING EMPTY->NO MODULE LINKS PULLED!!!
      
      HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap = null;
      HashMap<String, String> modLinkIDMap = null;
      if (keepOverlays) {
        keyMap = new HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey>();
        modLinkIDMap = new HashMap<String, String>();
        fgho.fillMapsForGroupExtraction(gi, grpID, keyMap);
      }
      
      //
      // Start building the layout:
      //
  
      StaticDataAccessContext rcx4G = new StaticDataAccessContext(rcxB);
      rcx4G.setLayout(new Layout("junkID", giID));
      GroupProperties newProp = null;
      if (baseIsRoot) {
        newProp = new GroupProperties(count++, grpID, center, 0, rcxB.getColorResolver());
        if (gpMemory != null) {
          GroupProperties memory = gpMemory.get(grpID);
          if (memory != null) {
            newProp.setColor(true, memory.getColorTag(true));
            newProp.setColor(false, memory.getColorTag(false));
            Integer wantOrder = new Integer(memory.getOrder());
            if (!seenOrders.contains(wantOrder)) {
              desiredOrders.put(grpID, wantOrder);
              seenOrders.add(wantOrder);
            }
          }
        }
      } else {
        GroupProperties baseProp = rcxB.getCurrentLayout().getGroupProperties(grpID);
        if (baseProp != null) {
          newProp = new GroupProperties(baseProp);
          Integer wantOrder = new Integer(newProp.getOrder());
          if (!seenOrders.contains(wantOrder)) {
            desiredOrders.put(grpID, wantOrder);
            seenOrders.add(wantOrder);
          }
        } else {
          newProp = new GroupProperties(count++, grpID, center, 0, rcxB.getColorResolver());
        }
      }
      rcx4G.getCurrentLayout().setGroupProperties(grpID, newProp);

      //
      // Transfer properties for subgroups if they are needed:
      //

      
      Set<String> allsubs = grp.getSubsets(gi);
      Iterator<String> asit = allsubs.iterator();
      while (asit.hasNext()) {
        String subGrpID = asit.next();
        GroupProperties newSubProp = null;
        if (baseIsRoot) {
          newSubProp = new GroupProperties(subGrpID, 1, center, newProp.getOrder(), rcx4G.getColorResolver());
        } else {
          GroupProperties subProp = rcxB.getCurrentLayout().getGroupProperties(subGrpID);
          newSubProp = new GroupProperties(subProp);
          newSubProp.setOrder(newProp.getOrder());
        }
        rcx4G.getCurrentLayout().setGroupProperties(subGrpID, newSubProp);
      }
      Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> diceMap = (claimedSlices == null) ? null : claimedSlices.get(grpID);
      // true arg preserves previous semantics, but do we want that if baseIsRoot?
     
      rcx4G.getCurrentLayout().extractPartialLayout(nodeMap, linkMap, null, keyMap, modLinkIDMap, diceMap, true, false, null, rcx4G, rcxB);
      // 7/28/08: Why squash with no accomodation for cross links??
      // 9/19/08: Because this is currently used to squash new incremental additions in that mode.
      // Else, we use the columns and rows provided to squash, which do account for cross links
      // See BT-08-07-09:1 concerning how we should do squash semantics.
      if (doSquash) {
        if ((emptiesForGroups == null) || (emptiesForGroups.get(grpID) == null)) {
          squash(rcx4G, moduleLinkFragShifts, grpID, monitor, startFrac, endFrac);
        } else {
          Empties empties = emptiesForGroups.get(grpID);
          rcx4G.getCurrentLayout().compress(rcx4G, empties.emptyRows, empties.emptyCols, null, null, moduleLinkFragShifts, grpID, monitor, startFrac, endFrac);  
        }
      }

      //
      // Move the tag to the lower left corner:
      //
      
      
      if (baseIsRoot) {
        rcx4G.getCurrentLayout().groupTagToCorner(grp, rcx4G, true);
      }
           
      layouts.put(grpID, rcx4G.getCurrentLayout());
      Rectangle bounds = rcx4G.getCurrentLayout().getLayoutBoundsForGroup(grp, rcx4G, true, true);
      
      if (bounds != null) {
        UiUtil.forceToGrid(bounds, UiUtil.GRID_SIZE);
      }
      loBounds.put(grpID, bounds);
    }
    
    //
    // Gotta make sure that groups are assigned an order that does not collide:
    //
    
    int topWanted = (seenOrders.isEmpty()) ? 0 : seenOrders.last().intValue();
    Iterator<String> lfgit = layouts.keySet().iterator();
    while (lfgit.hasNext()) {
      String grpID = lfgit.next();
      Integer order = desiredOrders.get(grpID);
      int installOrder = (order == null) ? topWanted++ : order.intValue();
      Layout lo2i = layouts.get(grpID);
      GroupProperties gp = lo2i.getGroupProperties(grpID);
      gp.setOrder(installOrder);
    }

    return (new ClaimedAndUnclaimed(claimedSlices, unclaimedSlices));
  }
  
  /***************************************************************************
  **
  ** When syncing layouts from the root into children, the routing of cross-region
  ** links in synced target regions is based upon recovery of the routing from the
  ** root.  Build the layouts to support this:
  */
  
  private void buildCrossLinkLayouts(StaticDataAccessContext rcxRLI,
                                     Map<String, Layout> layouts, Map<String, Link> crossTuples, 
                                     Set<String> targetGroups, Map<String, Rectangle> loBounds,
                                     boolean doSquash, Map<String, Empties> emptiesForGroups,
                                     BTProgressMonitor monitor, double startFrac, double endFrac)
                                       throws AsynchExitRequestException {

    //
    // Build separate layouts for each:
    //
    
    GenomeInstance gi = rcxRLI.getCurrentGenomeAsInstance();
    String giID = gi.getID();
    
    Iterator<String> tgit = targetGroups.iterator();
    while (tgit.hasNext()) {
      String trgGroup = tgit.next();
      HashMap<String, String> nodeMap = new HashMap<String, String>();
      HashMap<String, String> linkMap = new HashMap<String, String>();
      Iterator<String> ctkit = crossTuples.keySet().iterator();    
      while (ctkit.hasNext()) {
        String linkID = ctkit.next();
        Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!    
        String linkTargRegion = cfl.getTrg();
        if (!trgGroup.equals(linkTargRegion)) {
          continue;
        }
        Linkage link = gi.getLinkage(linkID);
        
        String srcNode = link.getSource();
        String baseID = GenomeItemInstance.getBaseID(srcNode);
        nodeMap.put(srcNode, baseID);
        
        String trgNode = link.getTarget();
        baseID = GenomeItemInstance.getBaseID(trgNode);
        nodeMap.put(trgNode, baseID);
        
        baseID = GenomeItemInstance.getBaseID(linkID);        
        linkMap.put(linkID, baseID);
      }  
      
      //
      // Extracting overlay info from the rootLayout has no use going down to the instance, ergo
      // we provide the null, null, null map arguments.
      // FIXME: Note that compression would best be calculated by leaving space for any modules
      // in the child models!
      // See issue BT-08-07-09:1
      //
      Layout newLayout = new Layout("junkID", giID);
      StaticDataAccessContext rcx4g = new StaticDataAccessContext(rcxRLI, gi, newLayout);
       
      rcx4g.getCurrentLayout().extractPartialLayout(nodeMap, linkMap, null, null, null, null, false, false, null, rcx4g, rcxRLI);
      // Need to squash to match region squashing      
      if (doSquash) {
        if ((emptiesForGroups == null) || (emptiesForGroups.get(trgGroup) == null)) {
          squash(rcx4g, null, null, monitor, startFrac, endFrac);
        } else {
          Empties empties = emptiesForGroups.get(trgGroup);
          rcx4g.getCurrentLayout().compress(rcx4g, empties.emptyRows, empties.emptyCols, null, null, null, null, monitor, startFrac, endFrac);  
        }
      }
      layouts.put(trgGroup, rcx4g.getCurrentLayout()); 
      
      //
      // Generate a set of bounds around the target nodes and intralinks.  Note
      // we do not want to include source node which is included in this layout!
      //
      
      HashSet<String> trgNodes = new HashSet<String>();
      HashSet<String> rootTrgNodes = new HashSet<String>();
      Group group = gi.getGroup(trgGroup);
      Iterator<GroupMember> mit = group.getMemberIterator();
      while (mit.hasNext()) {
        GroupMember mem = mit.next();
        String nodeID = mem.getID();
        trgNodes.add(nodeID);
        rootTrgNodes.add(GenomeItemInstance.getBaseID(nodeID));
      }
      
      HashSet<String> trgLinks = new HashSet<String>();
      Iterator<Linkage> lit = gi.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        if (trgNodes.contains(link.getSource()) && trgNodes.contains(link.getTarget())) {
          trgLinks.add(GenomeItemInstance.getBaseID(link.getID()));
        }
      }
      Genome genome = rcxRLI.getGenomeSource().getRootDBGenome();
      StaticDataAccessContext rcx2 = new StaticDataAccessContext(rcxRLI, genome, rcxRLI.getCurrentLayout());
     
      if (doSquash && (emptiesForGroups != null) && (emptiesForGroups.get(trgGroup) != null)) {
        rcx2.setLayout(new Layout(rcxRLI.getCurrentLayout()));
        Empties empties = emptiesForGroups.get(trgGroup);
        rcx2.getCurrentLayout().compress(rcx2, empties.emptyRows, empties.emptyCols, null, null, null, null, monitor, endFrac, endFrac); 
      } else {
        rcx2.setLayout(rcxRLI.getCurrentLayout());
      }
      Rectangle bounds = rcx2.getCurrentLayout().getPartialBounds(rootTrgNodes, true, true, trgLinks, null, false, rcx2);
      if (bounds != null) {
        UiUtil.forceToGrid(bounds, UiUtil.GRID_SIZE);
        loBounds.put(trgGroup, bounds);
      }
    }
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Add new properties for a group for incremental layout
  */
  
  private void addNewProperties(Layout groupLayout, StaticDataAccessContext rcxTLGI,
                                Group group,
                                Point2D origin,
                                LayoutOptions options,
                                Map<String, String> linkColors,
                                BTProgressMonitor monitor, 
                                double startFrac, double maxFrac, 
                                Map<String, BusProperties.RememberProps> rememberProps, boolean strictOKGroups) 
                                throws AsynchExitRequestException {                                  
                                  
    int count = 0;  // FIX ME!! Wrong color....
      
    //
    // If the group is not already placed in the layout, do that now: (does this really
    // ever happen???)
    //
    
    String grpID = group.getID();
    GroupProperties gprop = rcxTLGI.getCurrentLayout().getGroupProperties(grpID);
    Point2D labLoc;
    if (gprop == null) {
      // FIX ME!  Never called??
      rcxTLGI.getCurrentLayout().setGroupProperties(grpID, new GroupProperties(count++, grpID, origin, rcxTLGI.getCurrentLayout().getTopGroupOrder() + 1, rcxTLGI.getColorResolver()));
      labLoc = origin;
    } else {
      labLoc = gprop.getLabelLocation();
    }
    

    //
    // Figure out the nodes not already placed in the layout, and figure out the centroid
    // in the inherited layout for the whole group
    //
    
    HashSet<String> missingNodes = new HashSet<String>();
    HashSet<String> refNodes = new HashSet<String>();
    Iterator<GroupMember> mit = group.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      if (rcxTLGI.getCurrentLayout().getNodeProperties(nodeID) == null) {
        missingNodes.add(nodeID);
      } else {
        refNodes.add(nodeID);
      }
    }
    
    //
    // If nothing is missing, we skip to link checks:
    //
    
    Vector2D finalShift = new Vector2D(0.0, 0.0); 
    if (missingNodes.size() != 0) {
      Point2D newCentroid = getCentroid(missingNodes, groupLayout);    

      //
      // Figure out where the new centroid should go:
      //

      Vector2D labelTotal;
      if (refNodes.size() != 0) {
        Vector2D shiftCentroid = shiftNewNodesWithPolarCoords(refNodes, missingNodes, newCentroid, 
                                                              groupLayout, rcxTLGI.getCurrentLayout());
        shiftCentroid.forceToGrid(UiUtil.GRID_SIZE);

        //
        // Figure out the nearest location that can accomodate the desired location:
        //

        finalShift = placeWithGrid(rcxTLGI, groupLayout, missingNodes, refNodes, shiftCentroid);
        finalShift.forceToGrid(UiUtil.GRID_SIZE);

        Vector2D originToCent = new Vector2D(origin, newCentroid);
        labelTotal = originToCent.add(finalShift);
        labelTotal.forceToGrid(UiUtil.GRID_SIZE);

      } else {
        labelTotal = new Vector2D(labLoc, newCentroid);
        labelTotal.forceToGrid(UiUtil.GRID_SIZE);
        double xmov = labelTotal.getX();
        double ymov = labelTotal.getY();
        rcxTLGI.getCurrentLayout().moveGroup(grpID, xmov, ymov);                
      }

      //
      // Set property locations based on this placement:
      //

      Iterator<String> mnit = missingNodes.iterator();
      while (mnit.hasNext()) {
        String nodeID = mnit.next();
        NodeProperties props = groupLayout.getNodeProperties(nodeID).clone();
        Point2D orig = props.getLocation();
        Point2D shift = finalShift.add(orig);
        UiUtil.forceToGrid(shift.getX(), shift.getY(), shift, UiUtil.GRID_SIZE);
        props.setLocation(shift);
        rcxTLGI.getCurrentLayout().setNodeProperties(nodeID, props);
      }
    } else {  // have exactly the same set of points
      if (refNodes.size() > 0) {
        Point2D oldCentroid = getCentroid(refNodes, groupLayout); 
        Point2D newCentroid = getCentroid(refNodes, rcxTLGI.getCurrentLayout()); 
        finalShift = new Vector2D(oldCentroid, newCentroid);
        finalShift.forceToGrid(UiUtil.GRID_SIZE);
      } else {
        finalShift = new Vector2D(0.0, 0.0);
      }
    }
    
    //
    // Figure out the links that are not already handled by the layout, and
    // add them here:
    //
    
    Set<String> todo = getNeededLinksForGroup(rcxTLGI, group);
    
    if (!todo.isEmpty()) {
      inheritConsistentLinks(rcxTLGI, group, groupLayout, todo, missingNodes, finalShift);    
    }
    
    //
    // If any link has not yet been figured out, draw it fresh here.  Expand the target, then
    // contract it:
    //    

    todo = getNeededLinksForGroup(rcxTLGI, group);
    layoutSomeLinks(todo, rcxTLGI, options,
                    linkColors, monitor, startFrac, maxFrac, true, rememberProps, strictOKGroups);   
    return;
  }  

  /***************************************************************************
  **
  ** Inherit consistent links
  */
  
  private void inheritConsistentLinks(StaticDataAccessContext rcxTLGI,
                                      Group group, Layout groupLayout,
                                      Set<String> missingLinks, 
                                      Set<String> missingNodes, Vector2D shift) {

    LinkRouter router = new LinkRouter();
    StaticDataAccessContext rcxGL = new StaticDataAccessContext(rcxTLGI);
    rcxGL.setLayout(groupLayout);
        
    StaticDataAccessContext rcxLK = new StaticDataAccessContext(rcxTLGI);
    rcxLK.setLayout(new Layout("bogus", rcxTLGI.getCurrentGenomeID()));
    // Don't care about overlay property transfer, so we have the triple-null map
    rcxLK.getCurrentLayout().extractPartialLayoutForGroup(group, null, rcxLK, rcxTLGI, null, null, null);
    LinkPlacementGrid routerGrid;
    try {
      routerGrid = router.initGrid(rcxLK, null, INodeRenderer.STRICT, null);
    } catch (AsynchExitRequestException ex) {
      throw new IllegalStateException(); // can never happen since monitor == null here
    } 
    
    //
    // If both ends of a link have the same relative position in the target, we can
    // use the existing props with just a shift, as long as it can be laid down in
    // a legal fashion on the grid.  We want to gather up all the links covered by
    // a single property while doing this...

    HashMap<String, Set<String>> linksForSrc = new HashMap<String, Set<String>>();
    HashMap<String, Vector2D> shiftForSrc = new HashMap<String, Vector2D>();
    Iterator<String> mlit = missingLinks.iterator();
    while (mlit.hasNext()) {
      String linkID = mlit.next();
      Linkage link = rcxTLGI.getCurrentGenome().getLinkage(linkID);
      Vector2D checkShift = nodesOkForLinkInherit(link, rcxLK, groupLayout, missingNodes, shift);
      String src = link.getSource();
      if (checkShift == null) {
        continue;
      } else {
        shiftForSrc.put(src, checkShift);  // last pass sets it; should all be the same
      }
      Set<String> linkSet = linksForSrc.get(src);
      if (linkSet == null) {
        linkSet = new HashSet<String>();
        linksForSrc.put(src, linkSet);
      }
      
      linkSet.add(linkID);
    }
          

    Set<String> existingLinks = getExistingLinksForGroup(rcxTLGI, group);
    // crank through all links needing properties....
    Iterator<String> lfsit = linksForSrc.keySet().iterator();
    while (lfsit.hasNext()) {
      String srcID = lfsit.next();
      Set<String> linkSet = linksForSrc.get(srcID);
      String firstLink = linkSet.iterator().next();
      LinkProperties props = groupLayout.getLinkProperties(firstLink);
      LinkageFree render = (LinkageFree)props.getRenderer();
      //
      // We remove shared segments of link tree from overlay check
      //
      Map<LinkPlacementGrid.PointPair, List<String>> ppMap = render.getPointPairMap(props, null, rcxGL);
      Vector2D okShift = shiftForSrc.get(srcID);
      Set<LinkPlacementGrid.PointPair> dropSet = prunePointPairMap(ppMap, existingLinks, okShift);

      //
      // Reduce, shift to target frame, and merge to target layout if
      // we can place it ok:
      //
 
      // Have to limit the inherited links to those that are surviving!
 
      props = props.buildReducedProperties(linkSet);
      props.fullShift(okShift.getX(), okShift.getY());
      Iterator<String> lsit = linkSet.iterator();
      HashSet<String> survivingLinks = new HashSet<String>();
      while (lsit.hasNext()) {
        String linkID = lsit.next();
        if (props.haveTargetDrop(linkID)) {
          rcxLK.getCurrentLayout().setLinkProperties(linkID, (BusProperties)props);
          survivingLinks.add(linkID);
        }
      }
      if (survivingLinks.isEmpty()) {
        continue;
      }

      if (render.canRenderToPlacementGrid(props, routerGrid, dropSet, null, rcxLK)) {
        mergeInheritedLinks(survivingLinks, srcID, props, rcxTLGI);
        routerGrid.dropLink(srcID, rcxLK.getCurrentGenome(), null);
        render.renderToPlacementGrid(props, routerGrid, null, null, rcxLK);
      } 
    }
    return;
  } 

  /***************************************************************************
  **
  ** Merge inherited links
  */
  
  private void mergeInheritedLinks(Set<String> linkSet, String srcID, LinkProperties props, StaticDataAccessContext rcx) {
       
    Iterator<String> lsit = linkSet.iterator();
    while (lsit.hasNext()) {
      String linkID = lsit.next();
      Linkage currLink = rcx.getCurrentGenome().getLinkage(linkID);
      LinkBusDrop keepDrop = props.getTargetDrop(linkID);
      BusProperties spTree = new BusProperties((BusProperties)props, (BusDrop)keepDrop);
      Vector2D offset = new Vector2D(0.0, 0.0);
      spTree = new BusProperties(spTree, srcID, linkID, offset);   
      rcx.getCurrentLayout().foldInNewProperty(currLink, spTree, rcx);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Check if we can inherit
  */
  
  private Vector2D nodesOkForLinkInherit(Linkage link, StaticDataAccessContext rcx,
                                         Layout sourceLayout, 
                                         Set<String> missingNodes, Vector2D shift) {
                                       
    //
    // If both nodes are new to the layout, it is a given that they are OK:
    //

    String src = link.getSource();
    String trg = link.getTarget();
    
    boolean srcMissing = missingNodes.contains(src);
    boolean trgMissing = missingNodes.contains(trg);
    
    if (srcMissing && trgMissing) {
      return (shift);
    }

    //
    // If one or both nodes are present, it is OK as long as the relative position
    // of the two in the target layout is the same:
    //
    
    NodeProperties np = sourceLayout.getNodeProperties(src);
    Point2D origSrcPos = np.getLocation();
    
    Point2D finalSrcPos;
    if (srcMissing) {
      finalSrcPos = shift.add(origSrcPos);
    } else {
      finalSrcPos = rcx.getCurrentLayout().getNodeProperties(src).getLocation();
    }
    
    np = sourceLayout.getNodeProperties(trg);
    Point2D origTrgPos = np.getLocation();
    
    Point2D finalTrgPos;
    if (trgMissing) {
      finalTrgPos = shift.add(origTrgPos);
    } else {
      finalTrgPos = rcx.getCurrentLayout().getNodeProperties(trg).getLocation();
    }    
        
    Vector2D origDelta = new Vector2D(origSrcPos, origTrgPos);
    Vector2D finalDelta = new Vector2D(finalSrcPos, finalTrgPos);
    Vector2D retval = new Vector2D(origSrcPos, finalSrcPos);
        
    return (origDelta.equals(finalDelta) ? retval : null);

  }
  
 /***************************************************************************
  **
  ** Search the map to return only pairs that support preexisting links
  */
  
  private Set<LinkPlacementGrid.PointPair> prunePointPairMap(Map<LinkPlacementGrid.PointPair, List<String>> ppMap, 
                                                             Set<String> existingLinks, Vector2D shift) {
    
    HashSet<LinkPlacementGrid.PointPair> retval = new HashSet<LinkPlacementGrid.PointPair>();
    Iterator<LinkPlacementGrid.PointPair> kit = ppMap.keySet().iterator();
    while (kit.hasNext()) {
      LinkPlacementGrid.PointPair pp = kit.next();
      List<String> links = ppMap.get(pp);
      int lnum = links.size();
      for (int i = 0; i < lnum; i++) {
        String linkID = links.get(i);
        if (existingLinks.contains(linkID)) {
          pp.shift(shift);
          retval.add(pp);
          break;
        }
      }
    }
    return (retval);
  }
      
  /***************************************************************************
  **
  ** Determine the link properties we are lacking in the given group
  */
  
  private Set<String> getNeededLinksForGroup(StaticDataAccessContext rcx, Group group) {
    HashSet<String> retval = new HashSet<String>();
    Set<String> allLinks = getAllLinksForGroup((GenomeInstance)rcx.getCurrentGenome(), group);
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String linkID = alit.next();
      if (rcx.getCurrentLayout().getLinkProperties(linkID) == null) {
        retval.add(linkID);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Determine the link properties existing in the given group
  */
  
  private Set<String> getExistingLinksForGroup(StaticDataAccessContext rcx, Group group) {
    HashSet<String> retval = new HashSet<String>();
    Set<String> allLinks = getAllLinksForGroup((GenomeInstance)rcx.getCurrentGenome(), group);
    Iterator<String> alit = allLinks.iterator();
    while (alit.hasNext()) {
      String linkID = alit.next();
      if (rcx.getCurrentLayout().getLinkProperties(linkID) != null) {
        retval.add(linkID);
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Determine the links belonging to the given group
  */
  
  private Set<String> getAllLinksForGroup(GenomeInstance gi, Group group) {       
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      if (!group.isInGroup(src, gi)) {
        continue;
      }
      if (!group.isInGroup(trg, gi)) {
        continue;
      }
      retval.add(link.getID());
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Copy temporary results
  */
  
  private void copyResultsBack(Layout sourceLayout, Layout targetLayout, 
                               Map<String, Rectangle> sourceBounds, Map<String, Rectangle> targetBounds) {

    targetLayout.replaceContents(sourceLayout);
    Iterator<String> sbkit = sourceBounds.keySet().iterator();
    while (sbkit.hasNext()) {
      String key = sbkit.next();
      Rectangle currBounds = sourceBounds.get(key);
      targetBounds.put(key, new Rectangle(currBounds));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Merge group layouts
  */
  
  private void mergeGroupLayouts(Layout instanceLayout, 
                                 SortedMap<Integer, List<String>> regionsByColumn, 
                                 Map<String, Layout> layouts, 
                                 Map<String, Rectangle> loBounds, 
                                 Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts,
                                 int borderSize, BTProgressMonitor monitor)
                                   throws AsynchExitRequestException {


    int baseGroupOrder = instanceLayout.getTopGroupOrder();                                
    boolean doDown = (regionsByColumn.size() > 1);                               
    Iterator<Integer> rbcit = regionsByColumn.keySet().iterator();
    int startX = 0;
    while (rbcit.hasNext()) {
      Integer col = rbcit.next();
      List<String> grpList = regionsByColumn.get(col);
      int glSize = grpList.size();
      TopLeftPacker tlp = new TopLeftPacker();
      ArrayList<PlacementElement> packElems = new ArrayList<PlacementElement>();
      for (int i = 0; i < glSize; i++) {
        String gpID = grpList.get(i);
        Rectangle currBounds = loBounds.get(gpID);
        packElems.add(new PlacementElement((Rectangle)currBounds.clone(), gpID));
      }
      HashMap<String, Point> points = new HashMap<String, Point>();
      int maxX = (doDown) ? tlp.placeElementsDownward(packElems, points, borderSize * UiUtil.GRID_SIZE_INT, monitor)
                          : tlp.placeElements(packElems, points, borderSize * UiUtil.GRID_SIZE_INT, monitor);
      
      for (int i = 0; i < glSize; i++) {
        String gpID = grpList.get(i);
        Layout currLO = layouts.get(gpID);
        Rectangle currBounds = loBounds.get(gpID);
        Point topLeft = points.get(gpID);
        
        //
        // Merge group layout into full layout:
        //
        
        Vector2D toOrigin = new Vector2D(-currBounds.x, -currBounds.y);
        Vector2D offset = new Vector2D(startX + topLeft.x, topLeft.y);
        Vector2D total = toOrigin.add(offset);
        instanceLayout.mergeLayoutOfRegion(currLO, total, false, false, baseGroupOrder, moduleLinkFragShifts, gpID);
        
        //
        // Shift bounds for later use:
        //
        
        currBounds.x += (int)total.getX();
        currBounds.y += (int)total.getY();
        
        if ((monitor != null) && !monitor.keepGoing()) {
          throw new AsynchExitRequestException();
        }
        
        //
        // Shift label to lower left corner:
        //
        
       //Point2D labLoc = instanceLayout.getGroupProperties(gpID).getLabelLocation();
       // Vector2D labToOrigin = new Vector2D(labLoc, origin);
       // Vector2D labelOffset = new Vector2D(0.0, max.height);   
       // Vector2D labelTotal = offset.add(labelOffset).add(labToOrigin);
        //instanceLayout.moveGroup(gpID, gi, labelTotal.getX(), labelTotal.getY());
        
        // FIX ME!!!!!! This is BROKEN!!!!
        
        //instanceLayout.moveGroup(gpID, gi, -currBounds.width / 2.0, currBounds.height / 2.0);
      }
      startX += maxX;
    }
    return;
  }

  
  /***************************************************************************
  **
  ** Figure out how much to pad the regions
  */
  
  private Map<String, Integer> findNewRegionPaddings(Map<String, Link> crossTuples, int padPerLink, 
                                                     CrossLinkRecovery recovery, Set<String> groups) {

    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    
    //
    // We need to provide new paddings when an inbound link segment gains
    // a "kink".  This should be done iteratively, since the padding can
    // drive whether something kinks or not, and we don't know about
    // what kinks we get until after the padding is selected....
    // Instead, we do a conservative estimate, assuming that every inbound
    // link segment that has two ends defined w.r.t. different regions may
    // be kinked.
    //
    
   
    Iterator<String> rpit = recovery.perSrcMap.keySet().iterator();    
    while (rpit.hasNext()) {
      String srcID = rpit.next();
      PerSrcData psd = recovery.perSrcMap.get(srcID);
      HashSet<LinkSegmentID> seenMixedSegs = new HashSet<LinkSegmentID>();
      Iterator<String> fsit = psd.perLinkMap.keySet().iterator();
      while (fsit.hasNext()) {
        String linkID = fsit.next();    
        Link cfl = crossTuples.get(linkID);  // Src, Trg actually region IDs!
        String srcRegID = cfl.getSrc();
        String trgRegID = cfl.getTrg();
        // Actually, we could have shifts in a non-expanded region!
        if ((groups != null) && !(groups.contains(srcRegID) || groups.contains(trgRegID))) {
          continue;
        }
        String parentReg = null;
        PerLinkData pld = psd.perLinkMap.get(linkID);
        int numSegs = pld.segIDList.size();
        for (int i = 0; i < numSegs; i++) {
          LinkSegmentID nextSeg = pld.segIDList.get(i);
          PerPointData ppd = psd.perPointMap.get(nextSeg);
          String pointRegion = (ppd.regionID == null) ? srcRegID : ppd.regionID;
          if (pointRegion.equals(trgRegID)) {
            boolean iAmMixed = false;
            if (!seenMixedSegs.contains(nextSeg)) {
              if (parentReg == null) {
                if (!pointRegion.equals(srcRegID)) {
                  iAmMixed = true;
                }
              } else if (!parentReg.equals(pointRegion)) {
                iAmMixed = true;
              }
            }
            if (iAmMixed) {
              seenMixedSegs.add(nextSeg);
              Integer countObj = retval.get(trgRegID);
              if (countObj == null) {
                retval.put(trgRegID, new Integer(padPerLink));
              } else {
                retval.put(trgRegID, new Integer(countObj.intValue() + padPerLink));
              }      
            }
          }
          parentReg = pointRegion;
        }
      }
    }    
     
    return (retval);
  }   
 
  
  /***************************************************************************
  **
  ** Merge expanded group layouts
  */
  
  private Map<Object, Vector2D> mergeExpandedGroupLayouts(Layout instanceLayout, 
                                        Map<String, Rectangle> newRegionBounds, 
                                        Map<String, Rectangle> savedRegionBounds,
                                        Map<String, Layout> layouts, 
                                        Map<String, Integer> regionPads, 
                                        List<PlacementElement> fixedElements,
                                        Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts,
                                        boolean foldOverlays, String nameOwner) {

    Map<Object, Vector2D> deltas = calcRegionGrowthOffsets(newRegionBounds, savedRegionBounds, fixedElements, regionPads);
    
    int baseGroupOrder = instanceLayout.getTopGroupOrder();
    Iterator<String> lit = layouts.keySet().iterator();    
    while (lit.hasNext()) {
      String key = lit.next();
      Layout grpLo = layouts.get(key);
      Vector2D delta = deltas.get(key);
      // Only existing regions have deltas; that is all we care about for now:
      if (delta != null) {
        boolean doName = ((nameOwner != null) && nameOwner.equals(key));
        instanceLayout.mergeLayoutOfRegion(grpLo, delta, foldOverlays, doName, baseGroupOrder, moduleLinkFragShifts, key);
      }
    }
    return (deltas);
  } 
  
  /***************************************************************************
  **
  ** Clip group layouts to ensure that overlay bounds do not go ouside of group bounds.
  */
  
  private void clipGroupLayouts(Map<String, ? extends Rectangle> newRegionBounds, Map<String, Layout> layouts, 
                                Map<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapes) {
    Iterator<String> lit = layouts.keySet().iterator();    
    while (lit.hasNext()) {
      String key = lit.next();
      Layout grpLo = layouts.get(key);
      Rectangle2D nrb = newRegionBounds.get(key);
      Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> oldToNewForGroup = oldToNewShapes.get(key);
      grpLo.clipExpandedModulesToGroup(nrb, oldToNewForGroup);
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Add some links
  */
  
  private LinkRouter.RoutingResult layoutSomeLinks(Set<String> toDo, StaticDataAccessContext rcxT,
                                                   LayoutOptions options,
                                                   Map<String, String> linkColors, 
                                                   BTProgressMonitor monitor,
                                                   double startFrac, double endFrac, 
                                                   boolean doExpandContract, 
                                                   Map<String, BusProperties.RememberProps> rememberProps, 
                                                   boolean strictOKGroups) 
                                                   throws AsynchExitRequestException {

                                 
    if (toDo.isEmpty()) {
      if (monitor != null) {
       if (!monitor.updateProgress((int)(endFrac * 100.0))) {
         throw new AsynchExitRequestException();
       }
      }
      return (new LinkRouter.RoutingResult());
    }
    
    //
    // If not doing expansion, one pass is enough:
    //
    
    if (!doExpandContract) {
      return (layoutSomeLinksCore(toDo, rcxT, options,
                                  linkColors, monitor, startFrac, endFrac, rememberProps, null, strictOKGroups));
    }

    double fullFrac = endFrac - startFrac;
    double frac1 = fullFrac * 0.33;
    double eFrac1 = startFrac + frac1;
    double frac2 = fullFrac * 0.33;
    double eFrac2 = eFrac1 + frac2;
    double frac3 = fullFrac * 0.33;
    double eFrac3 = eFrac2 + frac3; 
  
    //
    // If we get success without expansion, we are done.  We are also done if expansion is
    // not going to help (failure due to target pad collisions).
    //
    
    Map<String, Layout.OverlayKeySet> allKeys = rcxT.getFGHO().fullModuleKeysPerLayout(); 
    Layout.OverlayKeySet loModKeys = allKeys.get(rcxT.getCurrentLayoutID());       
    
    StaticDataAccessContext rcxTe = new StaticDataAccessContext(rcxT);
    rcxTe.setLayout(new Layout(rcxT.getCurrentLayout()));
    LinkRouter.RoutingResult res = 
      layoutSomeLinksCore(toDo, rcxTe, options, linkColors, 
                          monitor, startFrac, eFrac1, rememberProps, null, strictOKGroups);
    if ((res.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) {
    //if (res.linkResult == LinkRouter.LAYOUT_OK) {
      rcxT.getCurrentLayout().replaceContents(rcxTe.getCurrentLayout());
      return (res);
    }
    
    Set<String> targCollide = rcxT.getCurrentGenome().hasLinkTargetPadCollisions();
    if (!targCollide.isEmpty()) {
      rcxT.getCurrentLayout().replaceContents(rcxTe.getCurrentLayout());
      return (res);
    }    

    //
    // This is going to be tough.  Start by expanding the layout using reversable expansions.
    // Keep expanding until we do ok or hit our limit:   
    //
 
    int maxPasses = 3;  // magic number; too big, and we can run out of memory   
    double currProg = eFrac1;
    double progFrac = frac2 / maxPasses;
    double progFracFrac = progFrac / 2.0;
    
    
    
    for (int mult = 1; mult <= maxPasses; mult++) {
      StaticDataAccessContext rcxE = new StaticDataAccessContext(rcxT);
      rcxE.setLayout(new Layout(rcxT.getCurrentLayout()));
      TreeSet<Integer> insertRows = new TreeSet<Integer>();
      TreeSet<Integer> insertCols = new TreeSet<Integer>();
      rcxE.getCurrentLayout().chooseExpansionRows(rcxE, 1.0, 1.0, null, loModKeys, insertRows, insertCols, true, monitor);
      Layout.ExpansionReversal er = rcxE.getCurrentLayout().expand(rcxE, insertRows, insertCols, mult, 
                                                       true, null, null, null, monitor, currProg, currProg + progFracFrac);
      currProg += progFracFrac;
      
      LinkRouter.RoutingResult eres = layoutSomeLinksCore(toDo, rcxE, options, 
                                                          linkColors, monitor, 
                                                          currProg, currProg + progFracFrac, 
                                                          rememberProps, null, strictOKGroups);
      currProg += progFracFrac;
      if ((eres.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) { 
        rcxE.getCurrentLayout().reverseExpansion(rcxE, er, monitor, currProg, eFrac3);
        rcxT.getCurrentLayout().replaceContents(rcxE.getCurrentLayout());
        return (eres);
      }
    }    
    
    //
    // That didn't work, so try an non-reversable expansion:
    //
    
    currProg = eFrac2;
    progFrac = frac3 / maxPasses;
    progFracFrac = progFrac / 2.0;
    
    for (int mult = 1; mult <= maxPasses; mult++) {
      StaticDataAccessContext rcxE = new StaticDataAccessContext(rcxT);
      rcxE.setLayout(new Layout(rcxT.getCurrentLayout()));
      TreeSet<Integer> insertRows = new TreeSet<Integer>();
      TreeSet<Integer> insertCols = new TreeSet<Integer>();
      rcxE.getCurrentLayout().chooseExpansionRows(rcxE, 1.0, 1.0, null, loModKeys, insertRows, insertCols, false, monitor);
      rcxE.getCurrentLayout().expand(rcxE, insertRows, insertCols, mult, false, null, null, null, monitor, currProg, currProg + progFracFrac);
      currProg += progFracFrac;
      LinkRouter.RoutingResult eres = layoutSomeLinksCore(toDo, rcxE, options,
                                                          linkColors, monitor, currProg, currProg + progFracFrac,
                                                          rememberProps, null, strictOKGroups);
      currProg += progFracFrac;
      if ((eres.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) {        
        rcxT.getCurrentLayout().replaceContents(rcxE.getCurrentLayout());
        return (eres);
      }
    }

    rcxT.getCurrentLayout().replaceContents(rcxTe.getCurrentLayout()); // zero-expansion version...
    return (res);  
  }
  
  /***************************************************************************
  **
  ** Add some links
  */
  
  private LinkRouter.RoutingResult layoutSomeLinksCore(Set<String> toDo, StaticDataAccessContext rcxT,
                                                       LayoutOptions options,
                                                       Map<String, String> linkColors, 
                                                       BTProgressMonitor monitor,
                                                       double startFrac, double endFrac,
                                                       Map<String, BusProperties.RememberProps> rememberProps, 
                                                       Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                                       boolean strictOKGroups) 
                                                       throws AsynchExitRequestException {  
    //
    // Take snapshot before adding temporary links:
    //
    
    LinkRouter router = new LinkRouter(); 
    router.initGrid(rcxT, null, INodeRenderer.STRICT, monitor);
    
    //
    // Lay links out automatically.
    //
    
    Iterator<String> xrit = toDo.iterator();
    while (xrit.hasNext()) {
      String linkID = xrit.next();
      LayoutLinkSupport.autoAddCrudeLinkProperties(rcxT, linkID, null, rememberProps);
    }
    
    //
    // Clean up link layout.
    //
        
    HashSet<String> needColors = new HashSet<String>();
    LinkRouter.RoutingResult retval = router.multiPassLayout(toDo, rcxT,
                                                             options, monitor, needColors, startFrac, 
                                                             endFrac, recoveryDataMap, strictOKGroups); 

    Iterator<String> ncit = needColors.iterator();
    while (ncit.hasNext()) {
      String linkID = ncit.next();
      String linkCol = linkColors.get(linkID);
      if (linkCol == null) {
        linkCol = linkColors.get(GenomeItemInstance.getBaseID(linkID));
        if (linkCol == null) {
          continue;
        }
      }
      BusProperties lp = rcxT.getCurrentLayout().getLinkProperties(linkID);
      lp.setColor(linkCol);
    }
 
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Figure out where to put a new node in a group
  */
  
  @SuppressWarnings("unused")
  private Point2D shiftNewNodeWithAffineCombo(Layout groupLayout, String newNode, GenomeInstance gi, 
                                              Group group, Layout targetLayout) {

    ArrayList<Point2D> origPoints = new ArrayList<Point2D>();
    ArrayList<Point2D> newPoints = new ArrayList<Point2D>();    
    Point2D origPoint = null;
    
    Iterator<GroupMember> mit = group.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      NodeProperties nProp = targetLayout.getNodeProperties(nodeID);
      NodeProperties oProp = groupLayout.getNodeProperties(nodeID);      
      if (nProp != null) {
        if (!nodeID.equals(newNode)) {
          origPoints.add(oProp.getLocation());          
          newPoints.add(nProp.getLocation());
        } else {
          throw new IllegalArgumentException();
        }
      } else if (nodeID.equals(newNode)) {
        origPoint = oProp.getLocation();
      }
    }
    
    if (origPoint == null) {
      throw new IllegalArgumentException();
    }
    
    if (origPoints.size() < 3) {
      // go to backup plan...
      return (new Point2D.Double(0.0, 0.0));
    }
    
    List<Double> weights = AffineCombination.getWeights(origPoints, origPoint);
    
    if (weights == null) {
      // go to backup plan...
      return (new Point2D.Double(0.0, 0.0));
    } else {
      int numWeights = weights.size();
      double[] weightArray = new double[numWeights];
      for (int i = 0; i < numWeights; i++) {
        weightArray[i] = weights.get(i).doubleValue();
      }
      return (AffineCombination.combination(newPoints, weightArray, UiUtil.GRID_SIZE));
    }
  }

  /***************************************************************************
  **
  ** Figure out the cohort nodes of the given node.
  */
  
  @SuppressWarnings("unused")
  private Set<String> prepareNodeCohort(String nodeID, GenomeInstance gi) {

    HashSet<String> retval = new HashSet<String>();
    
    //
    // Figure out the parents and children of the node:
    //
    
    HashSet<String> parentIDs = new HashSet<String>();
    HashSet<String> childIDs = new HashSet<String>();
    HashSet<String> siblingIDs = new HashSet<String>();    
    
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      if (src.equals(nodeID)) {
        childIDs.add(trg);
      }
      if (trg.equals(nodeID)) {
        parentIDs.add(src);
      }
    }
    
    //
    // Figure out the siblings of the node:
    //

    if (!parentIDs.isEmpty()) {
      glit = gi.getLinkageIterator();
      while (glit.hasNext()) {
        Linkage link = glit.next();
        String src = link.getSource();
        String trg = link.getTarget();
        if (parentIDs.contains(src)) {
          siblingIDs.add(trg);
        }
      }
    }
    
    retval.addAll(parentIDs);
    retval.addAll(childIDs);
    retval.addAll(siblingIDs);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Return the set of given nodes that are already in the layout, and are also
  ** in the given group
  */
  
  @SuppressWarnings("unused")
  private Set<String> reduceNodeSetForGroup(Set<String> nodeSet, String newNode, GenomeInstance gi, Group group, Layout targetLayout) {

    HashSet<String> retval = new HashSet<String>();

    Iterator<GroupMember> mit = group.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      if (!nodeSet.contains(nodeID)) {
        continue;
      }
      NodeProperties nProp = targetLayout.getNodeProperties(nodeID);
      if (nProp != null) {
        retval.add(nodeID);
      }
    }
    
    return (retval);
  }

  /***************************************************************************
  **
  ** Figure out where to put new nodes in a group
  */
  
  private Point2D getCentroid(Set<String> newNodes, Layout groupLayout) {

    //
    // Get centroid of new nodes:
    //
    HashSet<Point2D> newNodePoints = new HashSet<Point2D>();
    Iterator<String> nnit = newNodes.iterator();
    while (nnit.hasNext()) {
      String newNode = nnit.next();
      Point2D origPoint = groupLayout.getNodeProperties(newNode).getLocation();
      if (origPoint == null) {
        throw new IllegalArgumentException();
      }
      newNodePoints.add(origPoint);
    }
    // Note we don't have true centroid if we have coincident points! 
    return (AffineCombination.combination(newNodePoints, 0.0));  
  }
  
  /***************************************************************************
  **
  ** Figure out where to put new nodes in a group
  */
  
  private Vector2D shiftNewNodesWithPolarCoords(Set<String> refNodes, Set<String> newNodes, Point2D newNodesCentroid, 
                                                Layout groupLayout, Layout targetLayout) {
                                               
    //
    // Get original and new positions of reference points:
    //
                                                
    HashSet<Point2D> origRefPoints = new HashSet<Point2D>();
    HashSet<Point2D> newRefPoints = new HashSet<Point2D>();
    Iterator<String> mit = refNodes.iterator();
    while (mit.hasNext()) {
      String nodeID = mit.next();
      NodeProperties nProp = targetLayout.getNodeProperties(nodeID);
      NodeProperties oProp = groupLayout.getNodeProperties(nodeID);      
      if ((nProp == null) || newNodes.contains(nodeID)) {
        throw new IllegalArgumentException();
      }
      origRefPoints.add(oProp.getLocation());          
      newRefPoints.add(nProp.getLocation());
    }

    Point2D finalPos = AffineCombination.radialPointPosition(origRefPoints, newNodesCentroid, newRefPoints);

    return (new Vector2D(newNodesCentroid, finalPos));
  } 

  /***************************************************************************
  **
  ** Find a nearby location that does not collide with the existing layout
  */
  
  private Vector2D placeWithGrid(StaticDataAccessContext rcxTLGI,
                                 Layout groupLayout, Set<String> missingNodes, Set<String> refNodes,
                                 Vector2D seedVec) {

    LinkRouter router = new LinkRouter();
    HashSet<String> skipLinks = new HashSet<String>();
    Iterator<Linkage> lit = rcxTLGI.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (rcxTLGI.getCurrentLayout().getLinkProperties(linkID) == null) {
        skipLinks.add(linkID);
      }
    }
 
    LinkPlacementGrid targLinkGrid = router.initLimitedGrid(rcxTLGI, refNodes, skipLinks, INodeRenderer.STRICT);
    PatternGrid targGrid = targLinkGrid.extractPatternGrid(true);
    
    StaticDataAccessContext rcxGL = new StaticDataAccessContext(rcxTLGI);
    rcxGL.setLayout(groupLayout);
    PatternGrid newGrid = groupLayout.partialFillPatternGridWithNodes(rcxGL, missingNodes);
    Pattern pat = newGrid.generatePattern();
    if (pat == null) {
      return (seedVec);
    }

    MinMax yRange = newGrid.getMinMaxYForRange(null);
    MinMax xRange = newGrid.getMinMaxXForRange(null);

    Point2D initialCorner = new Point2D.Double(xRange.min * 10.0, yRange.min * 10.0);
    Point2D prefCorner = seedVec.add(initialCorner);

    UiUtil.forceToGrid(prefCorner.getX(), prefCorner.getY(), prefCorner, UiUtil.GRID_SIZE); 
    Point startPt = new Point((int)prefCorner.getX() / 10, (int)prefCorner.getY() / 10);
    Point startPt10 = new Point((int)prefCorner.getX(), (int)prefCorner.getY());

    PatternPlacerSpiral pps = new PatternPlacerSpiral(targGrid, pat, startPt, 
                                                      PatternPlacerSpiral.RIGHT_ONLY, PatternPlacerSpiral.UP);
    Point retval = pps.locatePattern();
    pps.sinkPattern(retval);
    Point2D retval10 = new Point2D.Double(retval.getX() * 10.0, retval.getY() * 10.0); 
    return (seedVec.add(new Vector2D(startPt10, retval10)));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Needed data for expand opts
  */
  
  public static class ERILData { 
  
    StaticDataAccessContext rcx;
    Set<String> groups;
    RegionExpander regExp;
    Set<String> ultimateFailures; 
    Layout.PadNeedsForLayout padNeeds; 
    Layout.OverlayKeySet loModKeys;
    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery; 
    LayoutOptions options;
    BTProgressMonitor monitor; 
    double startFrac;
    double endFrac;
 
    
    // For compression
    public ERILData(StaticDataAccessContext rcx,
                    Set<String> groups,
                    RegionExpander regExp,
                    Set<String> ultimateFailures, 
                    Layout.PadNeedsForLayout padNeeds, 
                    Layout.OverlayKeySet loModKeys, 
                    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery, 
                    LayoutOptions options,
                    BTProgressMonitor monitor, 
                    double startFrac, double endFrac) {
      this.rcx = rcx;
      this.groups = groups;
      this.regExp = regExp;
      this.ultimateFailures = ultimateFailures; 
      this.padNeeds = padNeeds; 
      this.loModKeys = loModKeys;
      this.moduleShapeRecovery = moduleShapeRecovery; 
      this.options = options;
      this.monitor = monitor; 
      this.startFrac = startFrac;
      this.endFrac = endFrac;  
    }
    
    public ERILData(StaticDataAccessContext rcx,
                    Set<String> groups,
                    Layout.PadNeedsForLayout padNeeds, 
                    Layout.OverlayKeySet loModKeys, 
                    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery, 
                    LayoutOptions options,
                    BTProgressMonitor monitor, 
                    double startFrac, double endFrac) {
      this.rcx = rcx;
      this.groups = groups;
      this.regExp = null;
      this.ultimateFailures = null; 
      this.padNeeds = padNeeds; 
      this.loModKeys = loModKeys;
      this.moduleShapeRecovery = moduleShapeRecovery; 
      this.options = options;
      this.monitor = monitor; 
      this.startFrac = startFrac;
      this.endFrac = endFrac;  
   }
  }

  /***************************************************************************
  **
  ** Needed data for LRS opts
  */
  
  public static class RSData { 
    
    StaticDataAccessContext rootRcx;
    StaticDataAccessContext instRcx;
    LayoutOptions options;
    Layout.OverlayKeySet loModKeys; 
    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery;
    BTProgressMonitor monitor;
    Map<String, Rectangle> savedRegionBounds;
    Layout origLayout;
    boolean keepLayout; 
    double startFrac;
    double maxFrac;
    Map<String, BusProperties.RememberProps> rememberProps; 
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds;
 
    public RSData(StaticDataAccessContext rootRcx, StaticDataAccessContext instRcx, LayoutOptions options,
                  Layout.OverlayKeySet loModKeys, 
                  Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery,
                  BTProgressMonitor monitor, Map<String, Rectangle> savedRegionBounds, 
                  Layout origLayout,
                  boolean keepLayout, double startFrac, 
                  double maxFrac, Map<String, BusProperties.RememberProps> rememberProps, 
                  Map<String, Layout.PadNeedsForLayout> globalPadNeeds) {
      
      this.rootRcx = rootRcx;
      this.instRcx = instRcx; 
      this.options = options;
      this.loModKeys = loModKeys; 
      this.moduleShapeRecovery = moduleShapeRecovery;
      this.monitor = monitor;
      this.savedRegionBounds = savedRegionBounds;
      this.origLayout = origLayout;
      this.keepLayout = keepLayout; 
      this.startFrac = startFrac;
      this.maxFrac = maxFrac;
      this.rememberProps = rememberProps; 
      this.globalPadNeeds =  globalPadNeeds;  
    }
  }

  /***************************************************************************
  **
  ** Classes that do region expansions extend this
  */
  
  public static abstract class RegionExpander {
   
    protected StaticDataAccessContext rcx;
    protected Map<String, Rectangle> oldRegionBounds; 
    protected Map<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapes; 
    protected Map<String, Layout> layouts;
    protected Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts; 
    protected Layout.OverlayKeySet loModKeys; 
    protected CrossLinkRecovery recovery; 
    protected BTProgressMonitor monitor;
    protected HashMap<Object, Vector2D> deltas;
    protected Map<String, Map<String, EdgeMove>> chopDeparts;
    protected Map<String, Map<String, EdgeMove>> chopArrives;
     
    protected RegionExpander() {
    }
     
    protected RegionExpander(Map<String, Map<String, EdgeMove>> chopDeparts, 
                             Map<String, Map<String, EdgeMove>> chopArrives) {
      this.chopDeparts = chopDeparts;
      this.chopArrives = chopArrives;
    }
  
    // set per execution
    protected HashMap<String, Map<String, List<NetModuleProperties.TaggedShape>>> oldToNewShapesPerGroup;     
    protected Rectangle oldBounds;
    protected Layout grpLo;
    protected TreeSet<Integer> insertRows;
    protected TreeSet<Integer> insertCols;
        
    public void setup(StaticDataAccessContext rcx, Map<String, Rectangle> oldRegionBounds, 
                      Map<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapes, 
                      Map<String, Layout> layouts, 
                      Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts, 
                      Layout.OverlayKeySet loModKeys, CrossLinkRecovery recovery, BTProgressMonitor monitor) { 
      this.rcx = rcx; 
      this.oldRegionBounds = oldRegionBounds;
      this.oldToNewShapes = oldToNewShapes;
      this.layouts = layouts; 
      this.moduleLinkFragShifts = moduleLinkFragShifts;
      this.loModKeys = loModKeys; 
      this.recovery = recovery; 
      this.monitor = monitor;    
    }
    
    public void expandTheRegion(String grpID, double startFrac, double endFrac) throws AsynchExitRequestException {  
      
      oldToNewShapesPerGroup = new HashMap<String, Map<String, List<NetModuleProperties.TaggedShape>>>();
      oldToNewShapes.put(grpID, oldToNewShapesPerGroup);
      
      oldBounds = oldRegionBounds.get(grpID);
      grpLo = layouts.get(grpID);
      insertRows = new TreeSet<Integer>();
      insertCols = new TreeSet<Integer>();
      //
      // Expansion selection need to occur on regions of the whole layout, to account
      // for cross-region links.  Expansion occurs on separate group layouts:
      //
      customExpandTheRegion(grpID, startFrac, endFrac);
      expandOrCompressRecoveryPaths(true, grpLo, grpID, insertRows, insertCols, 1, recovery); 
      return;
    }
    
    public void setDeltas(Map<Object, Vector2D> deltas) {
      this.deltas = new HashMap<Object, Vector2D>(deltas);
      return;
    }
    
    public Vector2D getDelta(String grpID) {
      if (deltas == null) {
        return (null);
      }
      return (deltas.get(grpID));
    }
    
    public Map<String, Map<String, EdgeMove>> getChopDeparts() {
      return (chopDeparts);
    }
      
    public Map<String, Map<String, EdgeMove>> getChopArrives() {
      return (chopArrives);
    }
 
    protected abstract void customExpandTheRegion(String grpID, double startFrac, double endFrac) throws AsynchExitRequestException;
    public abstract Rectangle getGroupBounds(String grpID);
  }
  
  /***************************************************************************
  **
  ** For defining region edge chops for region-based specialty layouts
  ** 
  */  
  
  public static class EdgeMove  {

    public UiUtil.PointAndSide pas;
    public String srcID;
    public LinkSegmentID lsid;
    public String regionID;
    RegionBasedPosition regPos;
       
    public EdgeMove(UiUtil.PointAndSide pas, String srcID, String regionID) {
      this.pas  = pas;
      this.srcID = srcID;
      this.lsid = null;
      this.regionID = regionID;
    }
    
    public Vector2D getEdgeDirectionOutbound() {
      return (pas.outboundSideVec());
    }
    
    public Point2D getEdgePoint() {
      return (pas.point);
    }  
    
    public String toString() {
      return ("EdgeMove: pas:" + pas + " src = " + srcID + " lsid = " + lsid + " regID = " + regionID + " regPos = " + regPos);
    }  
    
    
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static class StandardExpander extends RegionExpander {
  
    private double fracV_;
    private double fracH_;

    
    StandardExpander(double fracV, double fracH) {
      fracV_ = fracV;
      fracH_ = fracH;
    }
   
    protected void customExpandTheRegion(String grpID, double startFrac, double endFrac) throws AsynchExitRequestException {
      
      rcx.getCurrentLayout().chooseExpansionRows(rcx, fracV_, fracH_, oldBounds, loModKeys, insertRows, insertCols, false, monitor);
      StaticDataAccessContext rcxG = new StaticDataAccessContext(rcx); 
      rcxG.setLayout(grpLo);
      rcxG.getCurrentLayout().expand(rcxG, insertRows, insertCols, 1, false, oldToNewShapesPerGroup, moduleLinkFragShifts, grpID, monitor, startFrac, endFrac);
      return;
    }
    
    public Rectangle getGroupBounds(String grpID) {
   
      Group grp = ((GenomeInstance)rcx.getCurrentGenome()).getGroup(grpID);
      grpLo = layouts.get(grpID);
      StaticDataAccessContext rcxG = new StaticDataAccessContext(rcx);
      rcxG.setLayout(grpLo);
      return (rcxG.getCurrentLayout().getLayoutBoundsForGroup(grp, rcxG, true, true));
    }
  }
   
  /***************************************************************************
  **
  ** For transmitting link fold-in information
  ** 
  */
  
  private class FoldDirections {
    String iid;
    Linkage giLink; 
    String giLinkSrc;
    LayoutDataSource lds;
    
    FoldDirections(String iid, Linkage giLink, String giLinkSrc, LayoutDataSource lds) {
      this.iid = iid;
      this.giLink = giLink;
      this.giLinkSrc = giLinkSrc;
      this.lds = lds;
    }

  }
 
  /***************************************************************************
  **
  ** For transmitting cross-link recovery information.  Public because of RegionExpander interface.
  ** 
  */
  
  public class CrossLinkRecovery {
    Map<String, PerSrcData> perSrcMap;
    Set<String> recoveredLinks;
    Map<String, Set<String>> exemptions;
 
    CrossLinkRecovery(Map<String, PerSrcData> perSrcMap, Set<String> recoveredLinks, Map<String, Set<String>> exemptions) {
      this.perSrcMap = perSrcMap;
      this.recoveredLinks = recoveredLinks;
      this.exemptions = exemptions;      
    }
    
    //
    // Glue tree data for select target regions into this CLR:
    //
    
    
    //void merge(Map targetClrs) {
      //
      // 
    //}
   
  }
  
  private class PerSrcData {
    BusProperties fullTree;
    HashMap<LinkSegmentID, PerPointData> perPointMap;
    HashMap<String, PerLinkData> perLinkMap;
    HashSet<String> assocRegions;
    Map<String, EdgeMove> departChops;
    Map<String, EdgeMove> arriveChops;
 
    PerSrcData(BusProperties fullTree, Map<String, EdgeMove> departChops, Map<String, EdgeMove> arriveChops) {
      this.fullTree = fullTree;
      this.perPointMap = new HashMap<LinkSegmentID, PerPointData>();      
      this.perLinkMap = new HashMap<String, PerLinkData>();
      this.assocRegions = new HashSet<String>();
      this.departChops = departChops;
      this.arriveChops = arriveChops;      
    }
  }  
  
  private class PerLinkData {
    LinkProperties spTree;
    ArrayList<LinkSegmentID> segIDList;
    LinkSegmentID srcBoundPt;
   // LinkSegmentID trgBoundPt;    
 
    PerLinkData(BusProperties spTree) {
      this.spTree = spTree;
      this.segIDList = new ArrayList<LinkSegmentID>();
      this.srcBoundPt = null;
    //  this.trgBoundPt = null;      
    }
  }
  
  private class PerPointData {
    String regionID;
    Point2D originalLoc;
    boolean originalOrthoInbound;
    Vector2D originalInboundRun;
    LinkProperties.CornerDoF dof;
   // mode for fixed points
    String fixedLinkID;
    LinkSegmentID newRef;
    // mode for src/targ points
    Vector2D offset;
    String refNodeID;
    // mode for other points
    RegionBasedPosition regionBased;

    PerPointData() {
    }
  }  
  
  private static class RegionBasedPosition {
    
    public enum Position {
      TOP,
      TOP_RIGHT,
      RIGHT,
      BOTTOM_RIGHT,
      BOTTOM,
      BOTTOM_LEFT,
      LEFT,
      TOP_LEFT;
    };
    
    PointAndVec regionBasedDef;
    Approx regionBasedApprox;
    LinkSegmentID partialDefinition;   
    String partialDefinitionByNodeID;   
    int partialPadDefinition;
    boolean partialIsLaunch;
    
    RegionBasedPosition() {
      regionBasedApprox = Approx.NO_APPROX_;
    }  
    
    public String toString() {
      return ("RegionBasedPosition: regionBasedDef = " + regionBasedDef  + " and other data...");  
    }  
    
    int distanceBin(RegionBasedPosition other) { // between 0 and 4
      Position myPosition = this.getPosition();
      Position otherPosition = other.getPosition();
      int diff =  Math.abs(myPosition.ordinal() - otherPosition.ordinal());
      if (diff > 4) {
        diff = (8 - diff);
      }
      return (diff);
    }
    
    boolean goClockwise(RegionBasedPosition other) {
      Position myPosition = this.getPosition();
      int bin = distanceBin(other);
      if (bin == 0) {
        throw new IllegalArgumentException();
      }
      return ((bin + myPosition.ordinal()) > 7);
    }  
    
    double normDistance(RegionBasedPosition other) {
      int bin = distanceBin(other);
      if (bin == 0) {
        if (this.isOnCorner()) {
          double myDot = this.cornerDot(true);
          double otherDot = other.cornerDot(true);
          return (Math.abs(myDot - otherDot));
        } else {
          double myEdge = this.alongEdge(true);
          double otherEdge = other.alongEdge(true);
          return (Math.abs(myEdge - otherEdge));
        }
      } else if (bin == 4) {
         double clockDist = 0.0;
         if (this.isOnCorner()) {
           clockDist = this.cornerDot(false) + other.cornerDot(true);
         } else {
           clockDist = this.alongEdge(false) + other.alongEdge(true);
         }  
         double counterDist = 0.0;
         if (this.isOnCorner()) {
           counterDist = this.cornerDot(true) + other.cornerDot(false);
         } else {
           counterDist = this.alongEdge(true) + other.alongEdge(false);
         }           
         return (Math.max(clockDist, counterDist) + 3.0);
      } else {
        boolean goClock = goClockwise(other);
        double dist = (this.isOnCorner()) ? this.cornerDot(!goClock) : this.alongEdge(!goClock);
        dist += (bin - 1);
        dist += (other.isOnCorner()) ? other.cornerDot(goClock) : other.alongEdge(goClock);
        return (dist);
      }
    } 
    
    double alongEdge(boolean doClockwise) {  // between 0 and 1
      Position myPos = getPosition();
      double clockDiff;
      switch (myPos) {
        case TOP: 
          clockDiff = regionBasedDef.origin.getX();
          break;
        case RIGHT:
          clockDiff = regionBasedDef.origin.getY();
          break;
        case BOTTOM:
          clockDiff = 1.0 - regionBasedDef.origin.getX();
          break;
        case LEFT:
          clockDiff = 1.0 - regionBasedDef.origin.getY();
          break;
        default:
          throw new IllegalStateException();
      }
      return ((doClockwise) ? clockDiff : 1.0 - clockDiff);
    }    
     
    double cornerDot(boolean doClockwise) {  // between 0 and 1
      Position myPos = getPosition();
      Vector2D testVec;
      switch (myPos) {
        case TOP_RIGHT: 
          testVec =new Vector2D(1.0, 0.0);
          break;
        case BOTTOM_RIGHT:
          testVec = new Vector2D(0.0, 1.0);
          break;
        case BOTTOM_LEFT:
          testVec = new Vector2D(-1.0, 0.0);
          break;
        case TOP_LEFT:
          testVec = new Vector2D(0.0, -1.0);
          break;
        default:
          throw new IllegalStateException();
      }
      Vector2D normVec = regionBasedDef.offset;
      if (normVec.isZero()) {
        return (0.0);
      }
      normVec = normVec.normalized();
      double clockDot = testVec.dot(normVec);
      return ((doClockwise) ? clockDot : 1.0 - clockDot);
    }
      
    boolean isOnCorner() {
      Position myPos = getPosition();
      return ((myPos == Position.TOP_RIGHT) || (myPos == Position.BOTTOM_RIGHT) || (myPos == Position.BOTTOM_LEFT) || (myPos == Position.TOP_LEFT));
    }
    
    Position getPosition() {
      Point2D myPoint = this.regionBasedDef.origin;
      double myX = myPoint.getX();
      if (Math.abs(myX) < 1.0E-3) {
        myX = 0.0;
      }
      if (Math.abs(1.0 - myX) < 1.0E-3) {
        myX = 1.0;
      }      
      double myY = myPoint.getY();
      if (Math.abs(myY) < 1.0E-3) {
        myY = 0.0;
      }
      if (Math.abs(1.0 - myY) < 1.0E-3) {
        myY = 1.0;
      }            
      if (myX == 0.0) {
        if (myY == 0.0) {
          return (Position.TOP_LEFT);
        } else if (myY == 1.0) {
          return (Position.BOTTOM_LEFT);
        } else {
          return (Position.LEFT);
        } 
      } else if (myX == 1.0) {
        if (myY == 0.0) {
          return (Position.TOP_RIGHT);
        } else if (myY == 1.0) {
          return (Position.BOTTOM_RIGHT);
        } else {
          return (Position.RIGHT);
        }
      } else {
        if (myY == 0.0) {
          return (Position.TOP);
        } else if (myY == 1.0) {
          return (Position.BOTTOM);
        } else {
          throw new IllegalStateException(); // inside, not on the edge...
        }         
      } 
    }
  }  
  
  private static class RegionBasedSorter implements Iterator<WhipCandidate> {
    TreeMap<Integer, SortedMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>>> bins;
    int currIndex = 0;
    ArrayList<WhipCandidate> results;
    
    RegionBasedSorter(RegionBasedPosition home, List<WhipCandidate> candidates) {
      bins = new TreeMap<Integer, SortedMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>>>();
        
      int numCand = candidates.size();
      for (int i = 0; i < numCand; i++) {
        WhipCandidate wc = candidates.get(i);            
        LinkSegmentID lsid = wc.lsid;
        RegionBasedPosition other = wc.rbp;
        
        int bin = home.distanceBin(other);
        //
        // Let's say that somebody just off to the side is as good as somebody
        // straight out, and let the distance measure sort things out:
        //
        if (bin == 1) {
          bin = 0;
        }
        Integer binObj = new Integer(bin);
        SortedMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>> rankings = bins.get(binObj); 
        if (rankings == null) {
          rankings = new TreeMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>>();
          bins.put(binObj, rankings);
        }
        Double distObj = new Double(other.regionBasedDef.offset.length());
        SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>> perDist = rankings.get(distObj); 
        if (perDist == null) {
          perDist = new TreeMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>();
          rankings.put(distObj, perDist);
        }
        Double circumObj = new Double(home.normDistance(other));
        SortedMap<LinkSegmentID, WhipCandidate> lastMap = perDist.get(circumObj); 
        if (lastMap == null) {
          lastMap = new TreeMap<LinkSegmentID, WhipCandidate>();
          perDist.put(circumObj, lastMap);
        }
        lastMap.put(lsid, wc);  // LSID ordering is arbitrary but consistent...
      }
      
      results = new ArrayList<WhipCandidate>();
      Iterator<SortedMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>>> binit = bins.values().iterator();
      while (binit.hasNext()) {
        SortedMap<Double, SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>> rankings = binit.next();
        Iterator<SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>>> rankit = rankings.values().iterator();
        while (rankit.hasNext()) {
          SortedMap<Double, SortedMap<LinkSegmentID, WhipCandidate>> perDist = rankit.next();
          Iterator<SortedMap<LinkSegmentID, WhipCandidate>> finit = perDist.values().iterator();
          while (finit.hasNext()) {
            SortedMap<LinkSegmentID, WhipCandidate> lastMap = finit.next();
            Iterator<WhipCandidate> lastit = lastMap.values().iterator();
            while (lastit.hasNext()) {
              WhipCandidate result = lastit.next();
              results.add(result);
            }
          } 
        }
      }
    }
    
    public boolean hasNext() {
      return (currIndex < results.size());
    }

    public WhipCandidate next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return (results.get(currIndex++)); 
    }
 
    public void remove() {
      throw new UnsupportedOperationException();
    }
 
  }    

  /***************************************************************************
  **
  ** Generic link tree source
  ** 
  */
  
  private class GenericBPSource {
    Layout useLayout;
    Map<String, LinkProperties> useMap;
    Genome genome;
 
    GenericBPSource(Layout useLayout) {
      this.useLayout = useLayout;
      this.useMap = null;
    }
    
    GenericBPSource(Map<String, LinkProperties> useMap, Genome genome) {
      this.useLayout = null;
      this.useMap = useMap;
      this.genome = genome;
    }
    
    BusProperties getLinkProperties(String linkID) {
      if (useLayout != null) {
        return (useLayout.getLinkProperties(linkID));
      } else {
        Linkage link = genome.getLinkage(linkID);
        return ((BusProperties)useMap.get(link.getSource()));
      }
    }
  }
  
  /***************************************************************************
  **
  ** Data to remember for a tree splice
  ** 
  */
  
  private class WhipCandidate {
    Point2D point;
    LinkSegmentID lsid;
    RegionBasedPosition rbp;
    HashSet<String> links;
 
    WhipCandidate(Point2D point, LinkSegmentID lsid, RegionBasedPosition rbp, String currLink) {
      this.point = point;
      this.lsid = lsid;
      this.rbp = rbp;
      this.links = new HashSet<String>();
      this.links.add(currLink);
    }
    
    void addSupportedLink(String linkID) {
      links.add(linkID);
      return;
    }
  
    public String toString() {
      return ("WhipCandidate: point = " + point + " lsid = " + lsid + 
              " rbp = " + rbp + " links = " + links);  
    }
  }
  
  /***************************************************************************
  **
  ** Data to remember for a tree splice
  ** 
  */
  
  private class TreeSpliceData {
    boolean isBoundary;
    boolean originalOrthoInbound;
    Vector2D originalInboundRun;
    RegionBasedPosition spliceRbp;
    String trgGrpID;
    String trgNodeID;
    // FIX ME: ALSO hold Corner DOF for boundary pt?
 
    TreeSpliceData(String trgGrpID, String trgNodeID) {
      this.isBoundary = false;
      this.trgGrpID = trgGrpID;
      this.trgNodeID = trgNodeID;
    }
    
    TreeSpliceData(boolean originalOrthoInbound, Vector2D originalInboundRun, 
                   RegionBasedPosition spliceRbp, String trgGrpID, String trgNodeID) {
      this.isBoundary = true;      
      this.originalOrthoInbound = originalOrthoInbound;
      this.originalInboundRun = originalInboundRun;
      this.spliceRbp = spliceRbp;
      this.trgGrpID = trgGrpID;
      this.trgNodeID = trgNodeID;
    }
  
    public String toString() {
      return ("TreeSpliceData: isBoundary = " + isBoundary + " originalOrthoInbound = " + originalOrthoInbound + 
              " originalInboundRun = " + originalInboundRun + " spliceRbp = " + spliceRbp + "trgGrpID = " + trgGrpID +
              " trgNodeID = " + trgNodeID);  
    } 
  }
  
  private class TreeMergePerRegion {
    LinkProperties bp;
    Map<LinkSegmentID, Point2D> dicePts;
    Map<LinkSegmentID, RegionBasedPosition> targStubs; 
    //String trgGrpID;
    Set<String> linksToTarg;
    
    TreeMergePerRegion(LinkProperties bp, Map<LinkSegmentID, Point2D> dicePts, 
                       Map<LinkSegmentID, RegionBasedPosition> targStubs, Set<String> linksToTarg) {
      this.bp = bp;
      this.dicePts = dicePts;
      this.targStubs = targStubs;
      //this.trgGrpID = trgGrpID;
      this.linksToTarg = linksToTarg;  
    }
    
  }
  
  private class Empties {
    TreeSet<Integer> emptyRows;
    TreeSet<Integer> emptyCols;
    
    Empties() {
      emptyRows = new TreeSet<Integer>();
      emptyCols = new TreeSet<Integer>();
    }    
  }  
 
  class ClaimedAndUnclaimed {
    Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices;
    Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices;
    
    ClaimedAndUnclaimed(Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                        Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices) {
      this.claimedSlices = claimedSlices;
      this.unclaimedSlices = unclaimedSlices;
    } 
        
    String whoOwnName() {  
      if (claimedSlices != null) {
        Iterator<String> csit = claimedSlices.keySet().iterator();
        while (csit.hasNext()) {
          String grpID = csit.next();
          Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> diceMap = claimedSlices.get(grpID);
          Iterator<NetModule.FullModuleKey> dmit = diceMap.keySet().iterator();
          while (dmit.hasNext()) {
            NetModule.FullModuleKey oldKey = dmit.next();
            Layout.DicedModuleInfo dmi = diceMap.get(oldKey);
            int numDS = dmi.dicedShapes.size();
            for (int i = 0; i < numDS; i++) {
              NetModuleProperties.TaggedShape ts = dmi.dicedShapes.get(i);
              if (ts.isName) {
                return (grpID);
              }
            }
          }
        }
      }
      return (null);
    }    
  }
}
