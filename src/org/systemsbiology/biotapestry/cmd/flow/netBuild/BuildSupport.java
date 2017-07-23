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


package org.systemsbiology.biotapestry.cmd.flow.netBuild;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.OldPadMapper;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.cmd.flow.add.GroupCreationSupport;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSupport;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionInstance;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltMotif;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltMotifPair;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltProtoMotif;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.SpecialSegmentTracker;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.ui.layouts.NetModuleLinkExtractor;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Support for network building
*/

public class BuildSupport {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private BSData bsd_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BuildSupport(UIComponentSource uics, UndoFactory uFac) {
    uics_ = uics;
    uFac_ = uFac;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BuildSupport(UIComponentSource uics, BSData bsd, UndoFactory uFac) {
    uics_ = uics;
    bsd_ = bsd;
    uFac_ = uFac;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Reset data
  */ 
  
  public void resetBSD(BSData bsd) {
    bsd_ = bsd;
  }
  
  /***************************************************************************
  **
  ** Get the desired slice mode
  */  
 
  public int getSliceMode(JFrame topWindow) {  
    ResourceManager rMan = uics_.getRMan();
    Vector<ChoiceContent> choices = new Vector<ChoiceContent>(); 
    ChoiceContent defaultSlice = 
      new ChoiceContent(rMan.getString("sliceMode." + TimeCourseData.SLICE_BY_REGIONS_TAG),
                                       TimeCourseData.SLICE_BY_REGIONS);    
    choices.add(defaultSlice);
    choices.add(new ChoiceContent(rMan.getString("sliceMode." + TimeCourseData.SLICE_BY_TIMES_TAG),
                                  TimeCourseData.SLICE_BY_TIMES));
    Object[] choicesArray = choices.toArray();

    ChoiceContent sliceChoice = 
      (ChoiceContent)JOptionPane.showInputDialog(topWindow, 
                                                 rMan.getString("sliceMode.chooseSlice"), 
                                                 rMan.getString("sliceMode.chooseSliceTitle"),     
                                                 JOptionPane.QUESTION_MESSAGE, null, 
                                                 choicesArray, defaultSlice);
    if (sliceChoice == null) {
      return (TimeCourseData.NO_SLICE);
    }

    return (sliceChoice.val);
  }
   
  /***************************************************************************
  **
  ** Build out a full model tree for interaction worksheet network builds 
  */  
 
  public boolean buildOutModelTreeForNetworkBuild(StaticDataAccessContext dacx, boolean doSums, int sliceMode, 
                                                  Map<String, Set<String>> neighbors, 
                                                  Map<String, TimeCourseData.RootInstanceSuggestions> modelToSuggestion, 
                                                  UndoSupport support) {

    
    if (sliceMode == TimeCourseData.NO_SLICE) {
      return (false);
    }
    
    StaticDataAccessContext rcxR = dacx.getContextForRoot();
                
    //
    // Figure out the regions and times for importing:
    //

    TimeAxisDefinition tad = rcxR.getExpDataSrc().getTimeAxisDefinition();
    TimeCourseData tcd = rcxR.getExpDataSrc().getTimeCourseData();
    List<TimeCourseData.RootInstanceSuggestions> sugg = tcd.getRootInstanceSuggestions(sliceMode, neighbors);
   
    //
    // Get hold of the tree model:
    //
    
    NavTree nt = dacx.getGenomeSource().getModelHierarchy();
    DefaultTreeModel dtm = uics_.getTree().getTreeModel();
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot();
    ArrayList<NavTreeChange> ntcs = new ArrayList<NavTreeChange>();
    ExpansionChange ec = uics_.getTree().doUndoExpansionChange(support, dacx, true); 
    

    //
    // Crank through the additions:
    //

    Iterator<TimeCourseData.RootInstanceSuggestions> sit = sugg.iterator();
    while (sit.hasNext()) {
      TimeCourseData.RootInstanceSuggestions ris = sit.next();
      //
      // Create genome root instance, add to database, add a layout for it:
      //
      String nextKey = rcxR.getNextKey();
      GenomeInstance gi = new GenomeInstance(rcxR, ris.heavyToString(tad, uics_.getRMan()), nextKey, null);
      modelToSuggestion.put(nextKey, ris);
      gi.setTimes(ris.minTime, ris.maxTime);
      DatabaseChange dc = rcxR.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(rcxR, dc));  
      String nextloKey = rcxR.getNextKey();
      Layout lo = new Layout(nextloKey, nextKey);
      dc = rcxR.getLayoutSource().addLayout(nextloKey, lo);
      support.addEdit(new DatabaseChangeCmd(rcxR, dc));
      
      //
      // Add the node to the nav tree for it:
      //
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);

      TreeNode parNode = nt.nodeForModel(dacx.getGenomeSource().getRootDBGenome().getID());
      NavTree.NodeAndChanges nac = nt.addNode(NavTree.Kids.ROOT_INSTANCE, ris.heavyToString(tad, uics_.getRMan()), 
                                              parNode, new NavTree.ModelID(nextKey), null, null, uics_.getRMan());
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
      support.addEdit(new NavTreeChangeCmd(rcxR, nac.ntc));
      ntcs.add(nac.ntc); 
      //
      // Create the regions needed inside each root instance:
      //
      int count = 0;
      Iterator<String> rit = ris.regions.iterator();
      ArrayList<String> addedRegions = new ArrayList<String>();
      StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxR, gi, lo);
      while (rit.hasNext()) {
        String region = rit.next();
        count++;
        //
        // Build a group for each region in the current instance:
        //
        Point2D groupCenter = new Point2D.Double(400.0 * count, 500.0);
        addedRegions.add(buildRegion(rcxI, region, groupCenter, support));
      }      
      //
      // Beneath each root instance, (maybe) create a summed and (below it) a per-time
      // dynamic proxy
      //
      
      String dipName = null;
      DynamicInstanceProxy dipS = null;
      
      if (doSums) {
        nextKey = rcxR.getGenomeSource().getNextKey();
        String sumFormat = uics_.getRMan().getString("toolCmd.summedFormat");
        dipName = MessageFormat.format(sumFormat, new Object[] {ris.heavyToString(tad, uics_.getRMan())});     
        dipS = new DynamicInstanceProxy(rcxR, dipName, nextKey, gi, true, ris.minTime, ris.maxTime, false, null); 
        dc = rcxR.getGenomeSource().addDynamicProxyExistingLabel(nextKey, dipS);
        support.addEdit(new DatabaseChangeCmd(rcxR, dc));
        new DataLocator(uics_.getGenomePresentation(), rcxR).setTitleLocation(support, gi.getID(), dipName);
        List<String> newNodes = dipS.getProxiedKeys();
        if (newNodes.size() != 1) {
          throw new IllegalStateException();
        }
        String key = newNodes.iterator().next();
        parNode = nt.nodeForModel(gi.getID());
        nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
        nac = nt.addNode(NavTree.Kids.DYNAMIC_SUM_INSTANCE, dipS.getProxiedInstanceName(key), parNode, new NavTree.ModelID(key), null, null, uics_.getRMan());
        nt.setSkipFlag(NavTree.Skips.NO_FLAG);
        support.addEdit(new NavTreeChangeCmd(rcxR, nac.ntc));
        ntcs.add(nac.ntc);
        StaticDataAccessContext rcxD = new StaticDataAccessContext(rcxR, key);
        
        Iterator<String> arit = addedRegions.iterator();
        while (arit.hasNext()) {
          String regionKey = arit.next();
          autoAddGroupToDynamicInstance(rcxD, regionKey, support);       
        } 
      }
      
      // Per-time point:
      
      nextKey = rcxR.getGenomeSource().getNextKey();
      String displayUnits = tad.unitDisplayString();
      String perTimeFormat = uics_.getRMan().getString("toolCmd.perTimeFormat");
      dipName = MessageFormat.format(perTimeFormat, new Object[] {ris.heavyToString(tad, uics_.getRMan()), displayUnits});
      GenomeInstance par = (dipS == null) ? gi : dipS.getAnInstance();      
      DynamicInstanceProxy dipH = 
        new DynamicInstanceProxy(rcxR, dipName, nextKey, par, false, ris.minTime, ris.maxTime, false, null); 
      dc = rcxR.getGenomeSource().addDynamicProxyExistingLabel(nextKey, dipH);
      support.addEdit(new DatabaseChangeCmd(rcxR, dc));
      new DataLocator(uics_.getGenomePresentation(), rcxR).setTitleLocation(support, gi.getID(), dipName);
      GenomeInstance parent = dipH.getVfgParent();
      String parentID = parent.getID();
      parNode = nt.nodeForModel(parentID);
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
      nac = nt.addNode(NavTree.Kids.DYNAMIC_SLIDER_INSTANCE, dipName, parNode, null, nextKey, null, uics_.getRMan());
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
      support.addEdit(new NavTreeChangeCmd(rcxR, nac.ntc));
      ntcs.add(nac.ntc);
      
      DynamicGenomeInstance anInstance = dipH.getAnInstance();
      StaticDataAccessContext rcxP = new StaticDataAccessContext(rcxR, anInstance);
      Iterator<String> arit = addedRegions.iterator();
      while (arit.hasNext()) {
        String regionKey = arit.next();
        autoAddGroupToDynamicInstance(rcxP, regionKey, support);       
      } 
    }
      
    //
    // Add the nodes to the nav tree.  Map the pre-change expansion state to match
    // the tree info held in the NavTreeChange, and back-fill the expansion change
    // already submitted to undo support.
    //
       
    if (sugg.size() > 0) {
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
      dtm.nodeStructureChanged(rootNode);
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
    }
   
    //
    // Expand everybody out
    //
      
    nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
    List<TreePath> nonleafPaths = nt.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      uics_.getTree().expandTreePath(tp);
    } 

    //
    // Have to set selection first, do that the expansion change records the
    // correct selection path
    //

    DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
    TreeNode[] tn = rootChild.getPath();
    TreePath tp = new TreePath(tn);
    uics_.getTree().setTreeSelectionPath(tp);
    nt.setSkipFlag(NavTree.Skips.NO_FLAG);      

    //
    // Get tree copies from before and after change and fix the maps for expansion
    // xhange.

    int ntcSize = ntcs.size();
    if (ntcSize > 0) {
      NavTreeChange ntc = ntcs.get(0);
      ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
      ec.selected = nt.mapAPath(ec.selected, ntc, true);

      ntc = ntcs.get(ntcSize - 1);
      uics_.getTree().doRedoExpansionChange(support, rcxR, ntc, true);
    }
        
     // FIX ME??? Use events instead of direct calls.
    rcxR.getGenomeSource().clearAllDynamicProxyCaches();   
    
    return (true);
  }
 
  /***************************************************************************
  **
  ** Add group to dynamic instance
  */  
 
  private void autoAddGroupToDynamicInstance(StaticDataAccessContext rcxI, String startID, UndoSupport support) { 
    GenomeInstance parent = rcxI.getCurrentGenomeAsInstance().getVfgParent();
    int genCount = parent.getGeneration();    
    String inherit = Group.buildInheritedID(startID, genCount);
    Group subsetGroup = parent.getGroup(inherit);
    // Do we really need both the undom AND the support args??
    GroupCreationSupport handler = new GroupCreationSupport(uics_, uFac_);
    handler.addNewGroupToSubsetInstance(rcxI, subsetGroup, support);
    return;
  }
  
  /***************************************************************************
  **
  ** Propagate a root down using instructions
  */  
 
  public boolean propagateRootUsingInstructions(StaticDataAccessContext rcxR, StaticDataAccessContext rcxO) throws AsynchExitRequestException {
                                                                                       
    if (rcxO.getCurrentGenome().isEmpty()) {
      bsd_.keepLayout = false;
    }

    Map<String, Layout.OverlayKeySet> allKeys = rcxO.getFGHO().fullModuleKeysPerLayout();    
    Layout.OverlayKeySet loModKeys = allKeys.get(rcxO.getCurrentLayoutID());
      
    //
    // Gather info needed to reorganize the overlays:
    //
    
    
    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = 
      rcxO.getCurrentLayout().getModuleShapeParams(rcxO, loModKeys, bsd_.center);
    
    //
    // Get the set:
    //
      
    InstanceInstructionSet iis = rcxO.getInstructSrc().getInstanceInstructionSet(rcxO.getCurrentGenomeID());
        
    //
    // Save important legacy info:
    //
    
    HashMap<String, String> legacyGroups = new HashMap<String, String>();
    Iterator<Group> git = rcxO.getCurrentGenomeAsInstance().getGroupIterator();
    while (git.hasNext()) {
      Group grp = git.next();
      legacyGroups.put(grp.getName(), grp.getID());
    }

    Set<String> crossRegion = rcxO.getCurrentGenomeAsInstance().getCrossRegionLinks(null);
    
    bsd_.liidm.recordLegacyInstancesForRegions(rcxO.getCurrentGenomeAsInstance());
    
    Map<String, Rectangle> savedRegionBounds = (bsd_.keepLayout) ? saveRegionBounds(rcxO) : null;
        
    //
    // Get a copy of the Layout:
    //

    Layout copiedLo = new Layout(rcxO.getCurrentLayout());
    
    //
    // Completely kill off the existing genome (after making a local copy)
    //
    
    GenomeInstance oldGi = rcxO.getCurrentGenomeAsInstance().clone();

    StaticDataAccessContext rcxFrozen = new StaticDataAccessContext(rcxO, oldGi, copiedLo);
    
    String gID = rcxO.getCurrentGenomeID();
  //  String lID = rcxO.getLayoutID();
    // This creates EMPTY copies of BOTH the GenomeInstance AND the layout!
    bsd_.support.addEdit(new DatabaseChangeCmd(rcxO, rcxO.getGenomeSource().dropInstanceNetworkOnly(gID)));
      
    //
    // The gutting of the above means that we need to create a new RenderingContext with the gutted copies!
    //
    
    StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxO, gID);

    //
    // Extract regions and create them 
    //

    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    HashSet<String> regions = new HashSet<String>();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      regions.add(bii.getSourceRegionID());
      if (bii.hasTargetRegionID()) {
        regions.add(bii.getTargetRegionID());
      }
    }
    
    HashMap<String, String> regionMap = new HashMap<String, String>();
    bsd_.subsetRegionCache.put(rcxI.getCurrentGenomeID(), regionMap);

    //
    // Add groups as needed.
    //

    // FIX ME!!! Gotta drop group toggle settings and timecourse mappings for dropped groups!
    HashMap<String, GroupProperties> stashGrpProp = new HashMap<String, GroupProperties>();
    HashMap<String, String> newToOldRegionMap = new HashMap<String, String>();
    Iterator<String> rit = regions.iterator();
    while (rit.hasNext()) {
      String region = rit.next();
      InstanceInstructionSet.RegionInfo ri = iis.getRegionForAbbreviation(region);
      String gid = legacyGroups.get(ri.name);
      String regionKey = (gid == null) ? rcxR.getNextKey() : gid;
      Group newGroup = new Group(uics_.getRMan(), regionKey, ri.name);
      regionMap.put(region, regionKey);
      if (gid != null) {
        newToOldRegionMap.put(regionKey, gid);
      }
      
      //
      // This happens if we are adding a new region to a model, e.g. while
      // modifying VFA network from dialog box and adding to the regions.
      // This was >slapped< in to fix a crash, and labeled a "horrible hack".
      // But it seems to fix the problem.  FIXME?  Are there any issues here
      // that need to be more fully investigated?
      // 9/10/13: Actually, it is happening always, since groups are stripped out of
      // the model before we re-populate it. Correct??
      //
      
      if (rcxI.getCurrentGenomeAsInstance().getGroup(newGroup.getID()) == null) {
        GenomeChange gc = rcxI.getCurrentGenomeAsInstance().addGroupWithExistingLabel(newGroup); 
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
          bsd_.support.addEdit(gcc);
        }
      }
      if (!bsd_.keepLayout || (gid == null)) {
        // FIX ME! Color is going to be wrong
        int groupCount = rcxI.getCurrentGenomeAsInstance().groupCount();
        stashGrpProp.put(regionKey, new GroupProperties(groupCount, regionKey, bsd_.center, 0, rcxI.getColorResolver()));
      }
    }
    
    //
    // Set up the region mapping:
    //
       
    bsd_.liidm.setRegionMapping(newToOldRegionMap);     

    //
    // Extract nodes and create them.
    //
    
    //
    // For each instance instr, get base instruction.  Use this to look up the
    // motif entry in the protopair list, and extract the nodeIDs from there!
    //
    
    //
    // Caching the mapping rootVfgID->instrBaseID->tuple->lists of node/gene/links
    // for later use by subset models to build from instructions
    //

    Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>> instructionCache = new HashMap<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>>();
    bsd_.subsetCache.put(rcxI.getCurrentGenomeID(), instructionCache);
    HashMap<String, List<RegionInstance>> instanceMap = new HashMap<String, List<RegionInstance>>();
    HashMap<String, List<RegionTupleInstance>> linkInstanceMap = new HashMap<String, List<RegionTupleInstance>>();

    //
    // Two passes: we rebuild retained items first, maintaining IDs, before building
    // new items:
    //

    PIData pid = new PIData(bsd_.createdPairs, rcxR, rcxI,
                            oldGi, regionMap, instanceMap, linkInstanceMap, 
                            bsd_.support, instructionCache, bsd_.liidm, true); 
    
    Map<String, List<DialogBuiltMotifPair>> quickMap = buildQuickMap(iis, pid);
    
    //
    // Note the use of oldGi here! Because the current GI has been stripped of all contents!
    // Problem only shows up when adding to an existing model!
    //
    Map<String, Integer> topLinkInstanceNums = oldGi.getTopLinkInstanceNumberMap();
    Map<String, Integer> topNodeInstanceNums = oldGi.getTopNodeInstanceNumberMap();
    PadCalculatorToo.PadCache padCache = oldGi.getFullPadMap();
    Map<String, Map<String, Integer>> instanceForGroup = oldGi.getInstanceForNodeInGroupMap();
    
    iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      processInstruction(bii, pid, quickMap, topLinkInstanceNums, topNodeInstanceNums, padCache, instanceForGroup);
    }
    
    iit = iis.getInstructionIterator();
    pid.existingOnly = false;
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      processInstruction(bii, pid, quickMap, topLinkInstanceNums, topNodeInstanceNums, padCache, instanceForGroup);
    }    
    //
    // Build the maps we need to transfer all the overlay info across the layout, for
    // ALL of our child submodels too!
    //
   
    HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap = new HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey>(); 
    HashMap<String, String> modIDMap = new HashMap<String, String>(); 
    HashMap<String, String> modLinkIDMap = new HashMap<String, String>(); 
    
    Map<String, Layout.OverlayKeySet> allKeysWithEmpties = rcxI.getFGHO().fullModuleKeysPerLayout(true);
    Layout.OverlayKeySet loModKeysWithEmpties = allKeysWithEmpties.get(rcxI.getCurrentLayoutID());
    rcxI.getCurrentLayout().fillOverlayIdentityMaps(loModKeysWithEmpties, keyMap, modIDMap, modLinkIDMap);

    //
    // Fixup overlay members and group refs
    //
    
    recoverMappedOverlaysForInstance(rcxI.getCurrentGenomeAsInstance(), oldGi, bsd_.liidm.getInstanceNodeMapping(), newToOldRegionMap);

    DatabaseChange dc = rcxI.getLayoutSource().startLayoutUndoTransaction(rcxI.getCurrentLayoutID());
 
    try {
    
      // Drop crossRegion stuff from instance link map:
      
      Map<String, String> ilm = bsd_.liidm.getInstanceLinkMapping();
      if (bsd_.keepLayout) {
        ilm = new HashMap<String, String>(ilm);
        Iterator<String> ilmkit = bsd_.liidm.getInstanceLinkMapping().keySet().iterator();
        while (ilmkit.hasNext()) {
          String linkID = ilmkit.next();
          if (crossRegion.contains(linkID)) {
            ilm.remove(linkID);
          }
        }
      }

      Map<String, SpecialSegmentTracker> specials = (bsd_.keepLayout) ? copiedLo.rememberSpecialLinks() : null;
      HashMap<String, BusProperties.RememberProps> rememberProps = new HashMap<String, BusProperties.RememberProps>();
            
      rcxI.getCurrentLayout().transferLayoutFromLegacy(bsd_.liidm.getInstanceNodeMapping(), 
                                           ilm, newToOldRegionMap, keyMap, modLinkIDMap, null,
                                           !bsd_.keepLayout, rememberProps, rcxI, rcxFrozen);

      //
      // Transfer stashed group properties to new layout.  Only have stashed for
      // new groups:
      //

      Iterator<String> sgpit = stashGrpProp.keySet().iterator();
      while (sgpit.hasNext()) {
        String regionKey = sgpit.next();
        GroupProperties gp = stashGrpProp.get(regionKey);
        // TopOrder here only legal with non-subregions!
        if (gp.getLayer() != 0) {
          throw new IllegalStateException();
        }
        int order = rcxI.getCurrentLayout().getTopGroupOrder() + 1;
        gp.setOrder(order);
        rcxI.getCurrentLayout().setGroupProperties(regionKey, gp);
      }

      LayoutRubberStamper.RSData rsd = 
        new LayoutRubberStamper.RSData(rcxR, rcxI, bsd_.options,
                                       loModKeys, moduleShapeRecovery, bsd_.monitor, savedRegionBounds, copiedLo, bsd_.keepLayout, 
                                       bsd_.startFrac, bsd_.maxFrac, rememberProps, bsd_.globalPadNeeds);        
      LayoutRubberStamper lrs = new LayoutRubberStamper();
      lrs.setRSD(rsd);
      lrs.rubberStampLayout();
           
      //
      // Get special link segments back up to snuff:
      //

      if (specials != null) {
        rcxI.getCurrentLayout().restoreSpecialLinks(specials);
      }
      
      //
      // Hide the minor node names, if requested:
      //
      
      if (bsd_.hideNames) {
        rcxI.getCurrentLayout().hideAllMinorNodeNames(rcxI);
      }
     
      dc = rcxI.getLayoutSource().finishLayoutUndoTransaction(dc);
      bsd_.support.addEdit(new DatabaseChangeCmd(rcxI, dc));
      
      //
      // Make sure nodes are long enough:
      //
      
      fixNodeLengths(rcxI, bsd_.support);
      
      //
      // Fill out the node maps:
      //

      if (bsd_.nodeIDMap != null) {
        Iterator<Node> anit = rcxI.getCurrentGenome().getAllNodeIterator();
        String modelID = rcxI.getCurrentGenomeID();
        String modelName = DataUtil.normKey(rcxI.getCurrentGenome().getName());
        while (anit.hasNext()) {
          Node node = anit.next();
          String nodeID = node.getID();
          Group grp = rcxI.getCurrentGenomeAsInstance().getGroupForNode(nodeID, GenomeInstance.ALWAYS_MAIN_GROUP);
          String grpName = grp.getInheritedTrueName(rcxI.getCurrentGenomeAsInstance());
          NodeRegionModelNameTuple tup = new NodeRegionModelNameTuple(node.getName(), grpName, modelName);
          ModelNodeIDPair pair = new ModelNodeIDPair(modelID, nodeID);
          bsd_.nodeIDMap.put(tup, pair);
        }
      }

      return (true);
    } catch (AsynchExitRequestException ex) {
      rcxI.getLayoutSource().rollbackLayoutUndoTransaction(dc);
      throw ex;
    }
  } 

  /***************************************************************************
  **
  ** In the good old days, we could figure out supplemental data coords
  ** just before rebuilding the root instance.  But with overlays, all
  ** model levels need to be coherent at once, so we need to do this
  ** before any changes, and after everything is done!
  */  
 
  public Map<String, Layout.SupplementalDataCoords> buildSdcCache(Map<String, Layout.OverlayKeySet> globalKeys, DataAccessContext dacx) {
    
    HashMap<String, Layout.SupplementalDataCoords> retval = new HashMap<String, Layout.SupplementalDataCoords>();
    Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (gi.isRootInstance()) {
        String genomeID = gi.getID();
        Layout lo = dacx.getLayoutSource().getLayoutForGenomeKey(genomeID);
        Layout.OverlayKeySet loModKeys = globalKeys.get(lo.getID());
        StaticDataAccessContext rcx2 = new StaticDataAccessContext(dacx, gi, lo);
        Layout.SupplementalDataCoords sdc = lo.getSupplementalCoords(rcx2, loModKeys);
        retval.put(genomeID, sdc);       
      }
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Apply the cached SDC info to the layouts
  */  
 
  public void processSdcCache(Map<String, Layout.SupplementalDataCoords> sdcCache, 
                              Map<String, Layout.OverlayKeySet> globalKeys, StaticDataAccessContext dacx, UndoSupport support) {
    
    Iterator<String> sdit = sdcCache.keySet().iterator();
    while (sdit.hasNext()) {
      String genomeID = sdit.next();
      Layout.SupplementalDataCoords sdc = sdcCache.get(genomeID);
      Layout lo = dacx.getLayoutSource().getLayoutForGenomeKey(genomeID);
      DatabaseChange dc = dacx.getLayoutSource().startLayoutUndoTransaction(lo.getID());    
      Layout.OverlayKeySet loModKeys = globalKeys.get(lo.getID());
      lo.applySupplementalDataCoords(sdc, dacx, loModKeys);
      dc = dacx.getLayoutSource().finishLayoutUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(dacx, dc));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Populate a subset using instructions
  */  
 
  public boolean populateSubsetUsingInstructions(StaticDataAccessContext rcxO,
                                                 UndoSupport support, LegacyInstanceIdMapper liidm, 
                                                 Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap,
                                                 Map<String, Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>>> subsetCache, 
                                                 Map<String, Map<String, String>> subsetRegionCache) {
                                          
    //
    // Get the set:
    //                                       
                                                 
    InstanceInstructionSet iis = rcxO.getInstructSrc().getInstanceInstructionSet(rcxO.getCurrentGenomeID());
    if (iis == null) {
      return (true);
    }
    
    GenomeInstance rootInstance = rcxO.getCurrentGenomeAsInstance().getVfgParentRoot();
    GenomeInstance parent = rcxO.getCurrentGenomeAsInstance().getVfgParent();
    
    String gID = rcxO.getCurrentGenomeID();
  
    //
    // Completely kill off the existing genome guts AND the layout that goes with it!
    //
    
    GenomeInstance oldGi = rcxO.getCurrentGenomeAsInstance().clone();
    support.addEdit(new DatabaseChangeCmd(rcxO, rcxO.getGenomeSource().dropInstanceNetworkOnly(gID)));
    
    //
    // The gutting of the above means that we need to create a new RenderingContext with the gutted copies!
    //
    StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxO, gID);

    //
    // Extract regions and inherit them
    //

    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    HashSet<String> regions = new HashSet<String>();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      regions.add(bii.getSourceRegionID());
      if (bii.hasTargetRegionID()) {
        regions.add(bii.getTargetRegionID());
      }
    }  

    Map<String, String> regionMap = subsetRegionCache.get(rootInstance.getID());       
    Iterator<String> rit = regions.iterator();
    while (rit.hasNext()) {
      String region = rit.next();
      String useGroup = regionMap.get(region);
      int generation = rcxI.getCurrentGenomeAsInstance().getGeneration() - 1;
      Group parentGroup = parent.getGroup(Group.buildInheritedID(useGroup, generation));
      GroupCreationSupport handler = new GroupCreationSupport(uics_, uFac_);
      handler.addNewGroupToSubsetInstance(rcxI, parentGroup, support);
    }
   
    //
    // Fixup overlay members and group refs
    //
    
    recoverMappedOverlaysForInstance(rcxI.getCurrentGenomeAsInstance(), oldGi, liidm.getInstanceNodeMapping(), liidm.getRegionMapping());
    
    //
    // Process each instruction
    //
     
    Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>> instructionCache = subsetCache.get(rootInstance.getID());
    iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      processInstructionForSubset(bii, rcxI, parent, support, instructionCache);
    }
    
    //
    // Fill out the node maps:
    //
      
    if (nodeIDMap != null) {
      Iterator<Node> anit = rcxI.getCurrentGenome().getAllNodeIterator();
      String modelID = rcxI.getCurrentGenomeID();
      String modelName = DataUtil.normKey(rcxI.getCurrentGenome().getName());
      while (anit.hasNext()) {
        Node node = anit.next();
        String nodeID = node.getID();
        Group grp = rcxI.getCurrentGenomeAsInstance().getGroupForNode(nodeID, GenomeInstance.ALWAYS_MAIN_GROUP);
        String grpName = grp.getInheritedTrueName(rcxI.getCurrentGenomeAsInstance());
        NodeRegionModelNameTuple tup = new NodeRegionModelNameTuple(node.getName(), grpName, modelName);
        ModelNodeIDPair pair = new ModelNodeIDPair(modelID, nodeID);
        nodeIDMap.put(tup, pair);
      }
    }
 
    return (true);
  }   

  /***************************************************************************
  **
  ** Process an instruction
  */  
 
  private void processInstruction(BuildInstructionInstance bii, PIData pid, 
                                   Map<String, List<DialogBuiltMotifPair>> quickMap,
                                   Map<String, Integer> topLinkInstanceNums, 
                                   Map<String, Integer> topNodeInstanceNums, 
                                   PadCalculatorToo.PadCache padCache, 
                                   Map<String, Map<String, Integer>> instanceForGroup) {  

    String baseID = bii.getBaseID();
    Map<GenomeInstance.GroupTuple, SubsetCacheValues> tupleMap = pid.instructionCache.get(baseID);
    if (tupleMap == null) {
      tupleMap = new HashMap<GenomeInstance.GroupTuple, SubsetCacheValues>();
      pid.instructionCache.put(baseID, tupleMap);
    }
    HashSet<String> srcRegionNodes = new HashSet<String>();
    HashSet<String> targRegionNodes = new HashSet<String>(); 
    HashMap<String, Integer> linkTupleMap = new HashMap<String, Integer>();
    
    List<DialogBuiltMotifPair> ldbmp = quickMap.get(baseID);
    Iterator<DialogBuiltMotifPair> ldit = ldbmp.iterator();
    while (ldit.hasNext()) {
      DialogBuiltMotifPair dbmp = ldit.next(); 
      pIGuts(bii, pid, tupleMap, srcRegionNodes, targRegionNodes, 
             linkTupleMap, dbmp, topLinkInstanceNums, 
             topNodeInstanceNums, padCache, instanceForGroup);
    }
    return;
  }

  /***************************************************************************
  **
  ** Build quick access map 
  */  
 
  private Map<String, List<DialogBuiltMotifPair>> buildQuickMap(InstanceInstructionSet iis, PIData pid) {  
    
    Iterator<DialogBuiltMotifPair> nmit = pid.createdPairs.iterator();
      
    HashMap<String, DialogBuiltMotifPair> toDbmp = new HashMap<String, DialogBuiltMotifPair>();
    while (nmit.hasNext()) {
      DialogBuiltMotifPair dbmp = nmit.next();
      String dbmpID = dbmp.instructionID;
      if (toDbmp.get(dbmpID) != null) {
        throw new IllegalStateException();
      }
      toDbmp.put(dbmpID, dbmp);
    }
    
    //
    // The mapping of the base ID to a DialogBuiltMotifPair is not unique when there are multiple instances of a
    // base instruction:
    //
    
    HashMap<String, List<DialogBuiltMotifPair>> baseToDbmp = new HashMap<String, List<DialogBuiltMotifPair>>();
    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      String baseID = bii.getBaseID();
      List<DialogBuiltMotifPair> forBase = baseToDbmp.get(baseID);
      if (forBase == null) {
        forBase = new ArrayList<DialogBuiltMotifPair>();
        baseToDbmp.put(baseID, forBase);
      }
      DialogBuiltMotifPair dbmp = toDbmp.get(baseID);
      if (dbmp == null) {
        throw new IllegalStateException();
      } 
      forBase.add(dbmp);
    }
    return (baseToDbmp);
  }
  
  /***************************************************************************
  **
  ** Process an instruction
  */  
 
  private void pIGuts(BuildInstructionInstance bii, PIData pid, 
                             Map<GenomeInstance.GroupTuple, SubsetCacheValues> tupleMap,
                             HashSet<String> srcRegionNodes, 
                             HashSet<String> targRegionNodes, 
                             HashMap<String, Integer> linkTupleMap, 
                             DialogBuiltMotifPair dbmp,
                             Map<String, Integer> topLinkInstanceNums, 
                             Map<String, Integer> topNodeInstanceNums,
                             PadCalculatorToo.PadCache padCache,
                             Map<String, Map<String, Integer>> instanceForGroup) {  

    //
    // Figure out the nodes in the source region and the nodes in the target region
    //
    
    srcRegionNodes.clear();
    targRegionNodes.clear();
    DialogBuiltMotif dbm = dbmp.real;
    boolean sourceOnly = !dbm.hasTarget();
    // 
    dbm.getRegionAssignments(srcRegionNodes, targRegionNodes, linkTupleMap);
    GenomeInstance.GroupTuple tuple;
    String srcReg = bii.getSourceRegionID();
    String trgReg = null;
    // Source-only cases use the source as both halves of the tuple:
    if (sourceOnly) {
      tuple = new GenomeInstance.GroupTuple(srcReg, srcReg);
    } else {
      trgReg = bii.getTargetRegionID();
      tuple = new GenomeInstance.GroupTuple(srcReg, trgReg);
    }
    SubsetCacheValues scv = tupleMap.get(tuple);
    if (scv == null) {
      scv = new SubsetCacheValues();
      tupleMap.put(tuple, scv);
    } 
    //
    // Iterate through nodes FROM THE MOTIF in the source region:
    //
    String srcRegKey = pid.regionMap.get(srcReg);
    Iterator<String> snit = srcRegionNodes.iterator();
    while (snit.hasNext()) {
      String srcNodeID = snit.next();
      String legacyInstance = pid.liidm.getLegacyInstanceForRegion(srcNodeID, srcRegKey);
      //
      // Skip node if it didn't exist previously, and we are only processing existing, or vice versa:
      //
      if (((legacyInstance == null) && pid.existingOnly) ||
          ((legacyInstance != null) && !pid.existingOnly)) {
        continue;
      } 
      processNode(srcNodeID, legacyInstance, pid, srcRegKey, scv, instanceForGroup, topNodeInstanceNums);
    }
    //
    // Handle cases that are not just source only We are only looping though nodes FROM THE MOTIF
    //
    if (!sourceOnly) {
      String trgRegKey = pid.regionMap.get(trgReg);
      Iterator<String> tnit = targRegionNodes.iterator();
      while (tnit.hasNext()) {
        String trgNodeID = tnit.next();
        String legacyInstance = pid.liidm.getLegacyInstanceForRegion(trgNodeID, trgRegKey);
        //
        // Skip node if it didn't exist previously, and we are only processing existing, or vice versa:
        //
        if (((legacyInstance == null) && pid.existingOnly) ||
            ((legacyInstance != null) && !pid.existingOnly)) {
          continue;
        }          
        processNode(trgNodeID, legacyInstance, pid, trgRegKey, scv, instanceForGroup, topNodeInstanceNums);
      }
      pIGutsForLink(pid, linkTupleMap, srcRegKey, trgRegKey, scv, topLinkInstanceNums, padCache, instanceForGroup);
    }
    return;
  }

  /***************************************************************************
  **
  ** Process node in an instruction
  */  
 
  private void processNode(String nodeID, String legacyInstance, PIData pid, String regKey, 
                           SubsetCacheValues scv,
                           Map<String, Map<String, Integer>> instanceForGroup, Map<String, Integer> topNodeInstanceNums) {    
  
    String instanceID = getInstanceForRegion(regKey, nodeID, pid.instanceMap);
    // only add it if it hasn't already been added by previous instructions
    if (instanceID == null) {
      Node node = pid.rcxR.getCurrentGenome().getNode(nodeID);
      Group region = pid.rcxI.getCurrentGenomeAsInstance().getGroup(regKey);
      NodeInstance ni = PropagateSupport.propagateOldOrNewNodeNoLayout((node.getNodeType() == Node.GENE), 
                                                                       pid.rcxI, pid.oldGi, (DBNode)node, region, 
                                                                       pid.support, legacyInstance, topNodeInstanceNums);
      if (node.getNodeType() == Node.GENE) {
        scv.genes.add(ni.getID());
      } else {
        scv.nodes.add(ni.getID());
      }
      recordInstanceForRegion(regKey, nodeID, ni.getInstance(), pid.instanceMap, instanceForGroup);
      pid.liidm.recordInstanceForRegion(regKey, nodeID, ni.getInstance());
    // If it has already been added, only add it to the subset cache values
    } else {
      NodeInstance ni = (NodeInstance)pid.rcxI.getCurrentGenome().getNode(GenomeItemInstance.getCombinedID(nodeID, instanceID));
      if (ni.getNodeType() == Node.GENE) {
        scv.genes.add(ni.getID());
      } else {
        scv.nodes.add(ni.getID());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Process an instruction
  */  
 
  private void pIGutsForLink(PIData pid, HashMap<String, Integer> linkTupleMap, 
                                    String srcRegKey, String trgRegKey, SubsetCacheValues scv, 
                                    Map<String, Integer> topInstanceNums, 
                                    PadCalculatorToo.PadCache padCache, 
                                    Map<String, Map<String, Integer>> instanceForGroup) {  
     
    //
    // Iterate through links.  May be multiple links per motif, and each link
    // may span different regions:
    //
    Iterator<String> kit = linkTupleMap.keySet().iterator();
    while (kit.hasNext()) {
      String linkID = kit.next();
      String legacyInstance = pid.liidm.getLegacyInstanceForRegionTuple(linkID, srcRegKey, trgRegKey);
      //
      // Skip link if it didn't exist previously, and we are only processing existing, or vice versa:
      //
      if (((legacyInstance == null) && pid.existingOnly) ||
          ((legacyInstance != null) && !pid.existingOnly)) {
        continue;
      }
      
      int tupType = linkTupleMap.get(linkID).intValue();
      GenomeInstance.GroupTuple grpTup = null;
      switch (tupType) {
        case DialogBuiltMotif.SOURCE_SOURCE:
          grpTup = new GenomeInstance.GroupTuple(srcRegKey, srcRegKey);
          break;
        case DialogBuiltMotif.SOURCE_TARGET:
          grpTup = new GenomeInstance.GroupTuple(srcRegKey, trgRegKey);
          break;
        case DialogBuiltMotif.TARGET_TARGET:
          grpTup = new GenomeInstance.GroupTuple(trgRegKey, trgRegKey);
          break;
        case DialogBuiltMotif.TARGET_SOURCE:
          grpTup = new GenomeInstance.GroupTuple(trgRegKey, srcRegKey);
          break;              
        default:
          throw new IllegalStateException();
      }
      String linkInstanceID = getLinkInstanceForRegionTuple(grpTup, linkID, pid.linkInstanceMap);
      // only add it if it hasn't already been added by previous instructions
      if (linkInstanceID == null) {
        Linkage link = pid.rcxR.getCurrentGenome().getLinkage(linkID);
        LinkageInstance newLink = PropagateSupport.propagateOldOrNewLinkageNoLayout(pid.rcxI, pid.oldGi, (DBLinkage)link, 
                                                                                    pid.rcxR, grpTup, pid.support, legacyInstance, 
                                                                                    topInstanceNums, padCache, instanceForGroup);
        scv.links.add(newLink.getID());
        recordLinkInstanceForRegionTuple(grpTup, linkID, newLink.getInstance(), pid.linkInstanceMap);
        pid.liidm.recordInstanceForRegionTuple(srcRegKey, trgRegKey, linkID, newLink.getInstance());
      // If it has already been added, only add it to the subset cache values
      } else {
        LinkageInstance newLink = (LinkageInstance)pid.rcxI.getCurrentGenome().getLinkage(GenomeItemInstance.getCombinedID(linkID, linkInstanceID));
        scv.links.add(newLink.getID());
      }
    }
    return;
  } 

  /***************************************************************************
  **
  ** Process an instruction for a subset
  */  
 
  private void processInstructionForSubset(BuildInstructionInstance bii,
                                           StaticDataAccessContext rcxT,
                                           GenomeInstance parent,
                                           UndoSupport support, 
                                           Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>> instructionCache) {  

  //  String key = gi.getID();
    String baseID = bii.getBaseID();
    Map<GenomeInstance.GroupTuple, SubsetCacheValues> tupleMap = instructionCache.get(baseID);
    if (tupleMap == null) {
      throw new IllegalStateException();
    }
    String srcReg = bii.getSourceRegionID();
    boolean sourceOnly = !bii.hasTargetRegionID();
    GenomeInstance.GroupTuple tuple;
    // Source-only cases use the source as both halves of the tuple:
    if (sourceOnly) {
      tuple = new GenomeInstance.GroupTuple(srcReg, srcReg);
    } else {
      String trgReg = bii.getTargetRegionID();
      tuple = new GenomeInstance.GroupTuple(srcReg, trgReg);
    }
    SubsetCacheValues scv = tupleMap.get(tuple);
    int size = scv.nodes.size();
    for (int i = 0; i < size; i++) {
      String nodeID = scv.nodes.get(i);
      NodeInstance ni = (NodeInstance)parent.getNode(nodeID);
      PropagateSupport.addNewNodeToSubsetInstance(rcxT, ni, support);
    }
    size = scv.genes.size();
    for (int i = 0; i < size; i++) {
      String geneID = scv.genes.get(i);
      GeneInstance gene = (GeneInstance)parent.getNode(geneID);
      PropagateSupport.addNewNodeToSubsetInstance(rcxT, gene, support);
    }
    size = scv.links.size();
    for (int i = 0; i < size; i++) {
      String linkID = scv.links.get(i);
      LinkageInstance li = (LinkageInstance)parent.getLinkage(linkID);      
      PropagateSupport.addNewLinkToSubsetInstance(rcxT, li, support); 
    }
    return;
  }
  
  /***********************************************************************
  **
  ** Create the root model from instructions.  Caller must finish the support call.
  */  
 
  public LinkRouter.RoutingResult buildRootFromInstructions(StaticDataAccessContext dacx) throws AsynchExitRequestException {

    DatabaseChange dc = dacx.getInstructSrc().setBuildInstructions(bsd_.instructions);
    bsd_.support.addEdit(new DatabaseChangeCmd(dacx, dc));
    
    return (buildRootFromInstructionsNoRootInstall(dacx));
  }
  
  /***********************************************************************
  **
  ** Create the root model from instructions.  Caller must finish the support call.
  */  
 
  public LinkRouter.RoutingResult buildRootFromInstructionsNoRootInstall(StaticDataAccessContext dacx) throws AsynchExitRequestException {
     
    //ModelChangeEvent mcev = null;

    double myTime = bsd_.maxFrac - bsd_.startFrac;
    double timeSlice = myTime / 6.0;
    double end0 = bsd_.startFrac + (1.0 * timeSlice);
    double end1 = bsd_.startFrac + (2.0 * timeSlice);
    double end2 = bsd_.startFrac + (3.0 * timeSlice);
    double end3 = bsd_.startFrac + (4.0 * timeSlice);
    double end4 = bsd_.startFrac + (5.0 * timeSlice);
    double end5 = bsd_.maxFrac;
    
    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(bsd_.startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    } 
      
    int iSize = bsd_.instructions.size();
   
    boolean wasEmpty = dacx.getCurrentGenome().isEmpty();
    
    Map<String, Layout.OverlayKeySet> globalKeys = dacx.getFGHO().fullModuleKeysPerLayout();    
    Layout.OverlayKeySet loModKeys = globalKeys.get(dacx.getCurrentLayoutID());
    Layout.PadNeedsForLayout localPadNeeds = bsd_.globalPadNeeds.get(dacx.getCurrentLayoutID());
    
    Layout.SupplementalDataCoords sdc = dacx.getCurrentLayout().getSupplementalCoords(dacx, loModKeys);   
    Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = 
      dacx.getCurrentLayout().getModuleShapeParams(dacx, loModKeys, bsd_.center); 
    //
    // Go through the instructions and make list of pseudo motifs to generate
    //
    ArrayList<DialogBuiltMotifPair> protoPairs = new ArrayList<DialogBuiltMotifPair>();
    HashMap<String, Integer> typeTracker = new HashMap<String, Integer>();    
    for (int i = 0; i < iSize; i++) {
      BuildInstruction bc = bsd_.instructions.get(i);
      try {
        bc.addMotifs(protoPairs, typeTracker);
      } catch (IllegalStateException isex) {
        for (int j = 0; j <= i; j++) {
          BuildInstruction bcx = bsd_.instructions.get(j);
          System.err.println(bcx);
        }        
      }
    }
  
    //
    // Find matches in the existing genome, converting pseudo-motifs into
    // true motifs as we go:
    //
    
    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(end0 * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }    
    
    // Is the old pad mapper no longer needed since we just copy old links now?
    OldPadMapper oldPads = (bsd_.keepLayout) ? new OldPadMapper() : null;
    extractDialogBuiltMotifs((DBGenome)dacx.getCurrentGenome(), protoPairs, oldPads);
    
    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(end1 * 100.0))) {
        throw new AsynchExitRequestException();
      }
    } 
    
    //
    // 9/24/16: With modules now being first-class elements, we must drop them on a rebuild
    // until the BuildInstruction infrastructure supports them.
    //
    
    DBGenome root = (DBGenome)dacx.getCurrentGenome();
    List<DBGeneRegion> emptyRegions = new ArrayList<DBGeneRegion>();
    Iterator<Gene> git = root.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      if (gene.getNumRegions() != 0) {
        GenomeChange gc = root.changeGeneRegions(gene.getID(), emptyRegions);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
          bsd_.support.addEdit(gcc);        
        }
      }
    }
 
    //
    // Get the list of surviving nodes and links.  Kill off those items
    // from the root AND SUBMODELS that do not survive the cut.
    //
  
    HashSet<String> deadNodes = new HashSet<String>();
    HashSet<String> deadLinks = new HashSet<String>();
    collectUnusedItems(dacx.getCurrentGenome(), protoPairs, deadNodes, deadLinks); 
    RemoveSupport.deleteNodesAndLinksFromModel(uics_, deadNodes, deadLinks, dacx, bsd_.support, null, true, uFac_);
      
    //
    // Get a copy of the Layout:
    //
    
    Layout copiedLo = new Layout(dacx.getCurrentLayout());
 
    //
    // Completely kill off the existing genome, while holding a copy of the existing genome:
    //

    DBGenome oldGenome = ((DBGenome)dacx.getCurrentGenome()).clone();
    StaticDataAccessContext rcxFrozen = new StaticDataAccessContext(dacx, oldGenome, copiedLo);
    
    Map<String, Integer> oldTypes = (bsd_.keepLayout) ? null : rememberTypes((DBGenome)dacx.getCurrentGenome());
    // This is dropping the old layout from the database, but we copied it above.
    bsd_.support.addEdit(new DatabaseChangeCmd(dacx, dacx.dropRootNetworkOnly()));
    
    //
    // Make all the new nodes by cranking though pseudo-motifs.
    //

    if (bsd_.newNodeToOldNode == null) {
      bsd_.newNodeToOldNode = new HashMap<String, String>();
    }
    if (bsd_.newLinksToOldLinks == null) {
      bsd_.newLinksToOldLinks = new HashMap<String, String>();
    }
    HashMap<String, Integer> newTypesByID = new HashMap<String, Integer>();
    HashMap<String, PadConstraints> padConstraints = new HashMap<String, PadConstraints>();
    ArrayList<DialogBuiltMotif> newMotifs = new ArrayList<DialogBuiltMotif>();
    if (bsd_.createdPairs == null) {
      bsd_.createdPairs = new ArrayList<DialogBuiltMotifPair>();
    }
    
    InvertedSrcTrg ist = new InvertedSrcTrg(dacx.getCurrentGenome());
    
    buildItemsFromMotifs((DBGenome)dacx.getCurrentGenome(), oldGenome, dacx, protoPairs, bsd_.createdPairs,
                         bsd_.newNodeToOldNode, bsd_.newLinksToOldLinks, 
                         newTypesByID, newMotifs, oldPads, 
                         padConstraints, ist, bsd_.support);
    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(end2 * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }  
   
    //
    // Build the maps we need to transfer all the overlay info across the layout
    //
   
    HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap = new HashMap<NetModule.FullModuleKey, NetModule.FullModuleKey>(); 
    HashMap<String, String> modIDMap = new HashMap<String, String>(); 
    HashMap<String, String> modLinkIDMap = new HashMap<String, String>(); 
    Map<String, Layout.OverlayKeySet>  allKeysWithEmpties = dacx.getFGHO().fullModuleKeysPerLayout(true);
    Layout.OverlayKeySet loModKeysWithEmpties = allKeysWithEmpties.get(dacx.getCurrentLayoutID());
    copiedLo.fillOverlayIdentityMaps(loModKeysWithEmpties, keyMap, modIDMap, modLinkIDMap);
    
    //
    // Recover overlays based on maps of surviving members
    //  
 
    recoverMappedOverlays((DBGenome)dacx.getCurrentGenome(), oldGenome, bsd_.newNodeToOldNode);

    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(end3 * 100.0))) {
        throw new AsynchExitRequestException();
      }
    } 

    //
    // Pull over the pieces of the layout for legacy items:
    //
    
    HashMap<String, BusProperties.RememberProps> rememberLinkProps = new HashMap<String, BusProperties.RememberProps>();
      
    DatabaseChange dc = dacx.getLayoutSource().startLayoutUndoTransaction(dacx.getCurrentLayoutID());
    
    dacx.getCurrentLayout().transferLayoutFromLegacy(bsd_.newNodeToOldNode, bsd_.newLinksToOldLinks, null, 
                                              keyMap, modLinkIDMap, null, !bsd_.keepLayout, rememberLinkProps, dacx, rcxFrozen);
    
    
    dc = dacx.getLayoutSource().finishLayoutUndoTransaction(dc);
    bsd_.support.addEdit(new DatabaseChangeCmd(dacx, dc));
 
    if (bsd_.monitor != null) {
      if (!bsd_.monitor.updateProgress((int)(end4 * 100.0))) {
        throw new AsynchExitRequestException();
      }
    } 
     
    //
    // Get the old layout into a pattern grid
    //
    
   // PatternGrid oldGrid = (keepLayout && haveExtract) ? 
   //                         newLayout.fillPatternGridWithNodes(genome, frc) : null;
    
    //
    // Stash old colors of nodes for reuse, even if we discard layout:
    //
    
    HashMap<String, String> colorMap = new HashMap<String, String>();
    Iterator<String> nit = bsd_.newNodeToOldNode.keySet().iterator();
    while (nit.hasNext()) {
      String key = nit.next();
      String oldNode = bsd_.newNodeToOldNode.get(key);
      NodeProperties np = copiedLo.getNodeProperties(oldNode);
      String oldColor = np.getColorName();
      colorMap.put(key, oldColor);
      if (!bsd_.keepLayout) {
        newTypesByID.put(key, oldTypes.get(oldNode));
      }
    }
  
    //
    // We now need layout for the nodes and links that do not have legacy
    // counterparts.  Make lists of links that have no legacy:
    //
  
    HashSet<String> nonLegacyLinks = new HashSet<String>();
    Iterator<String> n2okit = bsd_.newLinksToOldLinks.keySet().iterator();
    while (n2okit.hasNext()) {
      String key = n2okit.next();
      if ((bsd_.newLinksToOldLinks.get(key) == null) || !bsd_.keepLayout) {   
        nonLegacyLinks.add(key);
      }
    }
    
    //
    // Now do layout.  NewTypesByID indicates new nodes to layout (with type info),
    // nonLegacyLinks tells us new links, motifs let us group them together.
    //
  
    LinkRouter.RoutingResult layoutResult;

    if (bsd_.keepLayout && !wasEmpty) {
      // No new stuff? Nothing to do!
      if (!newTypesByID.isEmpty() || !nonLegacyLinks.isEmpty()) {
        layoutResult = LayoutLinkSupport.autoLayoutHierarchical(dacx, bsd_.center, newTypesByID, 
                                                                nonLegacyLinks, bsd_.size, 
                                                                bsd_.support,
                                                                newMotifs, true,
                                                                colorMap, padConstraints, bsd_.options, bsd_.monitor, 
                                                                end4, end5, sdc, rememberLinkProps);
        // Fix lengths of nodes to handle all links.  FIXME: Not accounting for submodels with
        // big pad lengths!
        fixNodeLengths(dacx, bsd_.support);
      } else {
        layoutResult = new LinkRouter.RoutingResult();
      }
    } else {
      //
      // Specialty layout assumes we are working off an existing layout, so create a junk one:
      //

      AddCommands.junkLayout(dacx, bsd_.support); 
       
      GenomeSubset subset = new GenomeSubset(dacx, bsd_.center);
      ArrayList<GenomeSubset> sList = new ArrayList<GenomeSubset>();
      sList.add(subset);
  
      NetModuleLinkExtractor.SubsetAnalysis sa = (new NetModuleLinkExtractor()).analyzeForMods(sList, null, null);
      SpecialtyLayoutEngine sle = new SpecialtyLayoutEngine(sList, dacx, bsd_.specLayout, sa, bsd_.center, bsd_.params, true, bsd_.hideNames);
      sle.setModuleRecoveryData(localPadNeeds, moduleShapeRecovery);
      layoutResult = sle.specialtyLayout(bsd_.support, bsd_.monitor, end4, end5);
    }
    bsd_.support.addEvent(new ModelChangeEvent(dacx.getGenomeSource().getID(), dacx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    bsd_.support.addEvent(new LayoutChangeEvent(dacx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
 
    //
    // Fill out the node maps:
    //
      
    if (bsd_.nodeIDMap != null) {
      Iterator<Node> anit = dacx.getCurrentGenome().getAllNodeIterator();
      String modelID = dacx.getCurrentGenomeID();
      String modelName = bsd_.dbGenomeCSVName;
      while (anit.hasNext()) {
        Node node = anit.next();
        String nodeID = node.getID();
        NodeRegionModelNameTuple tup = new NodeRegionModelNameTuple(node.getName(), null, modelName);
        ModelNodeIDPair pair = new ModelNodeIDPair(modelID, nodeID);
        bsd_.nodeIDMap.put(tup, pair);
      }
    } 
    return (layoutResult);
  }
  
  /***************************************************************************
  **
  ** Recover overlay/module across a rebuilt network.
  */
  
  private void recoverMappedOverlays(DBGenome genome, DBGenome oldGenome, Map<String, String> newNodeToOldNode) {  
    //
    // Gotta invert the map:
    //    
    HashMap<String, String> oldNodeToNew = new HashMap<String, String>();    
    Iterator<String> nmit = newNodeToOldNode.keySet().iterator();
    while (nmit.hasNext()) {
      String newNode = nmit.next();
      String oldNode = newNodeToOldNode.get(newNode);
      if (oldNode != null) {
        oldNodeToNew.put(oldNode, newNode);
      }
    }        

    genome.recoverMappedModuleMembers(oldGenome, oldNodeToNew);
    return;
  }  
  
  /***************************************************************************
  **
  ** Recover overlay/module across a rebuilt network.
  */
  
  private void recoverMappedOverlaysForInstance(GenomeInstance genome, GenomeInstance oldGenome, 
                                                Map<String, String> newNodeToOldNode, Map<String, String> newGroupToOldGroup) {  
    //
    // Gotta invert the map:
    //    
    HashMap<String, String> oldNodeToNew = new HashMap<String, String>();    
    Iterator<String> nmit = newNodeToOldNode.keySet().iterator();
    while (nmit.hasNext()) {
      String newNode = nmit.next();
      String oldNode = newNodeToOldNode.get(newNode);
      if (oldNode != null) {
        oldNodeToNew.put(oldNode, newNode);
      }
    }      
    
    //
    // Gotta invert the map:
    //    
    HashMap<String, String> oldGroupToNew = new HashMap<String, String>();    
    Iterator<String> ngit = newGroupToOldGroup.keySet().iterator();
    while (ngit.hasNext()) {
      String newGrp = ngit.next();
      String oldGrp = newGroupToOldGroup.get(newGrp);
      if (oldGrp != null) {
        oldGroupToNew.put(oldGrp, newGrp);
      }
    }      
    genome.recoverMappedModuleMembers(oldGenome, oldNodeToNew, oldGroupToNew);
    return;
  }   
  
  /***************************************************************************
  **
  ** Get nodes to the correct size for all incoming links
  */  
 
  private void fixNodeLengths(StaticDataAccessContext dacx, UndoSupport support) {
    Genome genome = dacx.getCurrentGenome();
    Iterator<Node> nit = genome.getAllNodeIterator();
    Layout lo = dacx.getCurrentLayout();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      int currPads = node.getPadCount();
      PadCalculatorToo.PadResult padreq = genome.getNodePadRequirements(node, lo);
      if (padreq.landing > currPads) {
        GenomeChange gc;
        if (node.getNodeType() == Node.GENE) {        
           gc = genome.changeGeneSize(nodeID, padreq.landing);
        } else {
           gc = genome.changeNodeSize(nodeID, padreq.landing);
        }
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
          support.addEdit(gcc);
        }
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Build a region
  */  
 
  public String buildRegion(StaticDataAccessContext rcxI,
                            String region, Point2D groupCenter, UndoSupport support) {  
      
    // want key to be unique in parent address space!                             
    String groupKey = rcxI.getNextKey();
    
    Group newGroup = new Group(uics_.getRMan(), groupKey, region);
    GenomeChange gc = rcxI.getCurrentGenomeAsInstance().addGroupWithExistingLabel(newGroup); 
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
      support.addEdit(gcc);
    }
    
    int groupCount = rcxI.getCurrentGenomeAsInstance().groupCount();
    int order = rcxI.getCurrentLayout().getTopGroupOrder() + 1;
    Layout.PropChange lpc = 
      rcxI.getCurrentLayout().setGroupProperties(groupKey, new GroupProperties(groupCount, groupKey, groupCenter, order, rcxI.getColorResolver()));
    if (lpc != null) {
      PropChangeCmd pcc = new PropChangeCmd(rcxI,  new Layout.PropChange[] {lpc});
      support.addEdit(pcc);
    }
    
    
    TimeCourseDataMaps tcdm = rcxI.getDataMapSrc().getTimeCourseDataMaps();
    ArrayList<GroupUsage> mapped = new ArrayList<GroupUsage>();
    mapped.add(new GroupUsage(region, null));
    TimeCourseChange tcc = tcdm.setTimeCourseGroupMap(groupKey, mapped, true);      
    if (tcc != null) {
      support.addEdit(new TimeCourseChangeCmd(tcc, false));
    }    

    return (groupKey);
  }
  
  /***************************************************************************
  **
  ** Build the new items specified by the motifs in the list
  ** 
  */  
 
  private void buildItemsFromMotifs(DBGenome genome, DBGenome oldGenome, StaticDataAccessContext dacx,
                                     List<DialogBuiltMotifPair> pairList, List<DialogBuiltMotifPair> createdPairs, 
                                     Map<String, String> newNodeToOldNode, Map<String, String> newLinksToOldLinks, 
                                     Map<String, Integer> newTypesByID, List<DialogBuiltMotif> newMotifs,
                                     OldPadMapper opm,
                                     Map<String, PadConstraints> padConstraintSaver, 
                                     InvertedSrcTrg ist,
                                     UndoSupport support) {
                                                 
    //
    // The pair list gives us the protomotifs along with a real motif of existing
    // items.  Create a new empty pair list.  Go through the new list, create nodes and
    // links as we go, cascading new creations into matching motifs further down the
    // line, and recording the mapping from existing to new as we go.
    //
                                     
    // Create new pair list from existing
      
    int size = pairList.size();
    for (int i = 0; i < size; i++) {                  
      DialogBuiltMotifPair existingPair = pairList.get(i);
      DialogBuiltMotifPair createdPair = new DialogBuiltMotifPair();
      createdPair.instructionID = existingPair.instructionID;
      createdPair.proto = existingPair.proto.clone();
      createdPair.real = existingPair.real.emptyCopy();
      createdPairs.add(createdPair);
    }
   
    DialogBuiltProtoMotif.BuildData bd = 
      new DialogBuiltProtoMotif.BuildData(dacx, genome, oldGenome,
                                          createdPairs, pairList, newNodeToOldNode, 
                                          newLinksToOldLinks, 
                                          newTypesByID, opm, 
                                          padConstraintSaver, ist, true);
    
    //
    // Crank list, creating new guys for empty slots, cascading results to blank
    // matches further down, and filling in maps on guys who are on the existing list.
    // We do this in two passes; first to recreate retained elements ****with the same
    // database IDs******; second to create new elements.
    //

    for (int i = 0; i < size; i++) {                  
      DialogBuiltMotifPair createdPair = createdPairs.get(i);
      DialogBuiltMotifPair existingPair = pairList.get(i);
      createdPair.proto.generateRealMotif(createdPair, existingPair.real, bd, support);
      newMotifs.add(createdPair.real);
    }

    bd.setExistingOnly(false);
    for (int i = 0; i < size; i++) {                  
      DialogBuiltMotifPair createdPair = createdPairs.get(i);
      DialogBuiltMotifPair existingPair = pairList.get(i);
      createdPair.proto.generateRealMotif(createdPair, existingPair.real, bd, support);
      newMotifs.add(createdPair.real);
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Find the motifs from the genome that match the new motifs.
  ** 
  */  
 
  private boolean extractDialogBuiltMotifs(DBGenome genome, List<DialogBuiltMotifPair> protoPairs, OldPadMapper oldPads) {
    boolean retval = false;
    int size = protoPairs.size();
    
    InvertedSrcTrg ist = new InvertedSrcTrg(genome);
    
    for (int j = 0; j < size; j++) {
      //
      // Find the node matching the source name:
      //
      DialogBuiltMotifPair pair = protoPairs.get(j);
      DialogBuiltProtoMotif dbpm = pair.proto;
      int srcType = dbpm.getSourceType();
      String srcName = dbpm.getSourceName();
      Node oldNode = DialogBuiltProtoMotif.nodeMatchingNameAndType(genome, srcName, srcType, ist);
      if (oldNode != null) {
        retval = true;
      }
      dbpm.patternMatchMotif(genome, oldNode, pair, protoPairs, oldPads, ist);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find the motifs from the genome that match the new motifs.
  ** 
  */  
 
  private void collectUnusedItems(Genome genome, List<DialogBuiltMotifPair> protoPairs, Set<String> deadNodes, Set<String> deadLinks) {
    HashSet<String> liveNodes = new HashSet<String>();
    HashSet<String> liveLinks = new HashSet<String>();
    int size = protoPairs.size();
    for (int j = 0; j < size; j++) {
      //
      // Find the node matching the source name:
      //
      DialogBuiltMotifPair pair = protoPairs.get(j);
      DialogBuiltMotif dbm = pair.real;
      dbm.getExistingNodesAndLinks(liveNodes, liveLinks);
    }

    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene node = git.next();
      String nid = node.getID();
      if (!liveNodes.contains(nid)) {
        deadNodes.add(nid);
      }
    } 
    
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nid = node.getID();
      if (!liveNodes.contains(nid)) {
        deadNodes.add(nid);
      }
    }   
   
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String lid = link.getID();
      if (!liveLinks.contains(lid)) {
        deadLinks.add(lid);
      }
    } 
    return;
  } 
  /***************************************************************************
  **
  ** Remember types of old nodes
  ** 
  */  
 
  private Map<String, Integer> rememberTypes(DBGenome genome) {
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene node = git.next();
      retval.put(node.getID(), new Integer(node.getNodeType()));
    }
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      retval.put(node.getID(), new Integer(node.getNodeType()));
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get instance number for region
  */  
 
  private String getInstanceForRegion(String regionID, String nodeID, Map<String, List<RegionInstance>> instanceMap) {
    
    List<RegionInstance> riList = instanceMap.get(nodeID);
    if (riList == null) {
      return (null);
    }
    int regNum = riList.size();
    for (int i = 0; i < regNum; i++) {
      RegionInstance ri = riList.get(i);
      if (ri.regionID.equals(regionID)) {
        return (ri.instance);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Record instance number for region
  */  
 
  private void recordInstanceForRegion(String regionID, String nodeID, String instance, 
                                              Map<String, List<RegionInstance>> instanceMap,  
                                              Map<String, Map<String, Integer>> instanceForGroup) {
    
    List<RegionInstance> riList = instanceMap.get(nodeID);
    if (riList == null) {
      riList = new ArrayList<RegionInstance>();
      instanceMap.put(nodeID, riList);
    }
    riList.add(new RegionInstance(regionID, instance));
    
    Map<String, Integer> forGrp = instanceForGroup.get(regionID);
    if (forGrp == null) {
      forGrp = new HashMap<String, Integer>();
      instanceForGroup.put(regionID, forGrp);
    }
    try {
      forGrp.put(nodeID, Integer.parseInt(instance));
    } catch ( NumberFormatException nfex) {
      throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get instance number for region
  */  
 
  private String getLinkInstanceForRegionTuple(GenomeInstance.GroupTuple grpTup, String linkID, Map<String, List<RegionTupleInstance>> instanceMap) {
    
    List<RegionTupleInstance> riList = instanceMap.get(linkID);
    if (riList == null) {
      return (null);
    }
    int regNum = riList.size();
    for (int i = 0; i < regNum; i++) {
      RegionTupleInstance ri = riList.get(i);
      if (ri.srcRegionID.equals(grpTup.getSourceGroup()) &&
          ri.targRegionID.equals(grpTup.getTargetGroup())) {
        return (ri.instance);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Record link instance number for region tuple
  */  
 
  private void recordLinkInstanceForRegionTuple(GenomeInstance.GroupTuple grpTup, String linkID, String instance, 
                                                Map<String, List<RegionTupleInstance>> instanceMap) {
    
    List<RegionTupleInstance> riList = instanceMap.get(linkID);
    if (riList == null) {
      riList = new ArrayList<RegionTupleInstance>();
      instanceMap.put(linkID, riList);
    }
    riList.add(new RegionTupleInstance(grpTup.getSourceGroup(), grpTup.getTargetGroup(), instance));
    return;
  }  

  /***************************************************************************
  **
  ** Record region bounds for saved layouts
  */  
 
  public Map<String, Rectangle> saveRegionBounds(StaticDataAccessContext rcx) {
  
    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    Iterator<Group> git = rcx.getCurrentGenomeAsInstance().getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      String groupRef = group.getID();
      GroupProperties gp = rcx.getCurrentLayout().getGroupProperties(groupRef);
      if (gp.getLayer() != 0) {
        continue;
      }
      Rectangle bounds = rcx.getCurrentLayout().getLayoutBoundsForGroup(group, rcx, true, false);
      retval.put(groupRef, bounds);
    }
    return (retval);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Required data
  */ 
    
  public static class PIData {
  
    List<DialogBuiltMotifPair> createdPairs;
    StaticDataAccessContext rcxR; 
    StaticDataAccessContext rcxI;
    //DBGenome dbg; 
    //GenomeInstance gi; 
    GenomeInstance oldGi;
    Map<String, String> regionMap; 
    Map<String, List<RegionInstance>> instanceMap; 
    Map<String, List<RegionTupleInstance>> linkInstanceMap; 
   // Layout lor;
    UndoSupport support; 
    Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>> instructionCache; 
    LegacyInstanceIdMapper liidm; 
    boolean existingOnly;
  
    public PIData(List<DialogBuiltMotifPair> createdPairs,
                  StaticDataAccessContext rcxR, StaticDataAccessContext rcxI,
                  GenomeInstance oldGi,
                  Map<String, String> regionMap, Map<String, List<RegionInstance>> instanceMap, 
                  Map<String, List<RegionTupleInstance>> linkInstanceMap, 
                  UndoSupport support, Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>> instructionCache, 
                  LegacyInstanceIdMapper liidm, boolean existingOnly) { 
     
      this.createdPairs = createdPairs;
      this.rcxR = rcxR;
      this.rcxI = rcxI;
      this.oldGi = oldGi;
      this.regionMap = regionMap;
      this.instanceMap = instanceMap;
      this.linkInstanceMap = linkInstanceMap;
      this.support = support;
      this.instructionCache = instructionCache;
      this.liidm = liidm;
      this.existingOnly = existingOnly;
    }
  }
 
  /***************************************************************************
  **
  ** Required data
  */ 
    
  public static class BSData {
    List<BuildInstruction> instructions;
    Point2D center;
    Dimension size;
    boolean keepLayout;
    boolean hideNames;
    LayoutOptions options;
    UndoSupport support;
    BTProgressMonitor monitor;
    double startFrac; 
    double maxFrac;
    List<DialogBuiltMotifPair> createdPairs;
    Map<String, String> newNodeToOldNode; 
    Map<String, String> newLinksToOldLinks;
    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap;
    String dbGenomeCSVName;
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds;
    SpecialtyLayout specLayout;
    SpecialtyLayoutEngineParams params;
    
    LegacyInstanceIdMapper liidm;
    Map<String, Map<String, String>> subsetRegionCache;
    Map<String, Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>>> subsetCache;
 
    public BSData(List<BuildInstruction> instructions, 
                    Point2D center, 
                    Dimension size,
                    boolean keepLayout,
                    boolean hideNames,
                    LayoutOptions options,
                    UndoSupport support, BTProgressMonitor monitor,
                    double startFrac, double maxFrac,
                    List<DialogBuiltMotifPair> createdPairs,
                    Map<String, String> newNodeToOldNode, Map<String, String> newLinksToOldLinks,
                    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap, 
                    String dbGenomeCSVName, 
                    Map<String, Layout.PadNeedsForLayout> globalPadNeeds,    
                    SpecialtyLayout specLayout,
                    SpecialtyLayoutEngineParams params) {
         
    this.instructions = instructions;
    this.center = center;
    this.size = size;
    this.keepLayout = keepLayout;
    this.hideNames = hideNames;
    this.options = options;
    this.support = support;
    this.monitor = monitor;
    this.startFrac = startFrac;
    this.maxFrac = maxFrac;
    this.createdPairs = createdPairs;
    this.newNodeToOldNode = newNodeToOldNode;
    this.newLinksToOldLinks = newLinksToOldLinks;
    this.nodeIDMap = nodeIDMap;
    this.dbGenomeCSVName = dbGenomeCSVName;
    this.globalPadNeeds = globalPadNeeds;
    this.specLayout = specLayout;
    this.params = params;
   } 

   public BSData(List<DialogBuiltMotifPair> createdPairs,
                 Point2D center, 
                 Dimension size,
                 UndoSupport support,
                 LayoutOptions options,
                 BTProgressMonitor monitor,
                 Map<String, Map<String, Map<GenomeInstance.GroupTuple, SubsetCacheValues>>> subsetCache,
                 Map<String, Map<String, String>> subsetRegionCache,
                 LegacyInstanceIdMapper liidm,
                 Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap,
                 Map<String, Layout.PadNeedsForLayout> globalPadNeeds,                          
                 boolean keepLayout,
                 boolean hideNames,
                 double startFrac, double maxFrac) {
     
    this.center = center;
    this.size = size;
    this.keepLayout = keepLayout;
    this.hideNames = hideNames;
    this.options = options;
    this.support = support;
    this.monitor = monitor;
    this.startFrac = startFrac;
    this.maxFrac = maxFrac;
    this.createdPairs = createdPairs;
    this.nodeIDMap = nodeIDMap;
    this.globalPadNeeds = globalPadNeeds;
    // Used for submodel case
    this.liidm = liidm;
    this.subsetRegionCache = subsetRegionCache;
    this.subsetCache = subsetCache;
   }
        
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to map node instances to regions
  */ 
    
  private static class RegionInstance {

    String regionID;
    String instance;
    
    RegionInstance(String regionID, String instance) {
      this.regionID = regionID;
      this.instance = instance;
    }
    
    public String toString() {
      return ("RegionInstance: " + regionID + " " + instance);
    }
  }
  
  /***************************************************************************
  **
  ** Used to map link instances to region tuples
  */ 
    
  private static class RegionTupleInstance {

    String srcRegionID;
    String targRegionID;
    String instance;
    
    RegionTupleInstance(String srcRegionID, String targRegionID, String instance) {
      this.srcRegionID = srcRegionID;
      this.targRegionID = targRegionID;      
      this.instance = instance;
    }
  } 
  
  /***************************************************************************
  **
  ** Used to map link instances from previous incarnation to the present one.
  */ 
    
  public static class LegacyInstanceIdMapper {

    private Map<String, String> rootNodeMap_;
    private Map<String, String> rootLinkMap_;
    private Map<String, String> regionKeyMap_;
    private Map<String, List<RegionInstance>> legacyNodeInstanceMap_;
    private Map<String, List<RegionTupleInstance>> legacyLinkInstanceMap_;
    private Map<String, String> instanceNodeMap_;
    private Map<String, String> instanceLinkMap_;
    
    public LegacyInstanceIdMapper(Map<String, String> rootNodeMap, Map<String, String> rootLinkMap) {
      this.rootNodeMap_ = rootNodeMap;
      this.rootLinkMap_ = rootLinkMap;
      this.regionKeyMap_ = new HashMap<String, String>();
      this.legacyNodeInstanceMap_ = new HashMap<String, List<RegionInstance>>();
      this.legacyLinkInstanceMap_ = new HashMap<String, List<RegionTupleInstance>>();
      this.instanceNodeMap_ = new HashMap<String, String>();
      this.instanceLinkMap_ = new HashMap<String, String>();      
    }
    
    /***************************************************************************
    **
    ** Record mapping of new to old regions
    */  

    void setRegionMapping(Map<String, String> newToOldRegionMap) {
      regionKeyMap_ = newToOldRegionMap;
      return;
    }
    
    /***************************************************************************
    **
    ** Get mapping of new to old regions
    */  

    Map<String, String> getRegionMapping() {
      return (regionKeyMap_);
    } 
    
    /***************************************************************************
    **
    ** Get instance node mapping
    */  

    Map<String, String> getInstanceNodeMapping() {
      return (instanceNodeMap_);
    }
    
    /***************************************************************************
    **
    ** Get instance link mapping
    */  

    Map<String, String> getInstanceLinkMapping() {
      return (instanceLinkMap_);
    }    
    
    /***************************************************************************
    **
    ** Used for preserving incremental layout for root instances
    */  

    void recordLegacyInstancesForRegions(GenomeInstance gi) {  

      //
      // Record the instance id for each node 
      //                                                

      HashMap<String, String> regionForInstance = new HashMap<String, String>();
      Iterator<Group> git = gi.getGroupIterator();
      while (git.hasNext()) {
        Group grp = git.next();
        String regionID = grp.getID();
        Iterator<GroupMember> mit = grp.getMemberIterator();
        while (mit.hasNext()) {
          GroupMember gm = mit.next();
          String gmID = gm.getID();
          String instanceID = Integer.toString(GenomeItemInstance.getInstanceID(gmID));
          String baseID = GenomeItemInstance.getBaseID(gmID);

          List<RegionInstance> riList = legacyNodeInstanceMap_.get(baseID);
          if (riList == null) {
            riList = new ArrayList<RegionInstance>();
            legacyNodeInstanceMap_.put(baseID, riList);
          }
          riList.add(new RegionInstance(regionID, instanceID));

          regionForInstance.put(gmID, regionID);  // reverse map useful below!
        }
      }

      //
      // THERE CAN BE MORE THAN ONE LINK INSTANCE FOR A TUPLE!!!!!
      //

      Iterator<Linkage> lit = gi.getLinkageIterator();
      while (lit.hasNext()) {
        LinkageInstance link = (LinkageInstance)lit.next();
        String srcID = link.getSource();
        String trgID = link.getTarget();
        String srcRegion = regionForInstance.get(srcID);
        String trgRegion = regionForInstance.get(trgID);
        String linkID = link.getID();
        String instanceID = Integer.toString(GenomeItemInstance.getInstanceID(linkID));
        String baseID = GenomeItemInstance.getBaseID(linkID);

        List<RegionTupleInstance> riList = legacyLinkInstanceMap_.get(baseID);
        if (riList == null) {
          riList = new ArrayList<RegionTupleInstance>();
          legacyLinkInstanceMap_.put(baseID, riList);
        }
        riList.add(new RegionTupleInstance(srcRegion, trgRegion, instanceID));
      }    
      return;
    }  
  
    /***************************************************************************
    **
    ** Get legacy instance for region
    */  

     String getLegacyInstanceForRegion(String nodeID, String newRegKey) {

      //
      // Get new old region key from new region key:
      //

      String oldKey = regionKeyMap_.get(newRegKey);
      if (oldKey == null) {
        return (null);      
      }

      //
      // Find the legacy instance:
      //

      List<RegionInstance> riList = legacyNodeInstanceMap_.get(nodeID);
      if (riList == null) {
        return (null);
      }
      int regNum = riList.size();
      for (int i = 0; i < regNum; i++) {
        RegionInstance ri = riList.get(i);
        if (ri.regionID.equals(oldKey)) {
          return (ri.instance);
        }
      }
      return (null);    
    }
     

     
  /***************************************************************************
  **
  ** Record instance number for region
  */  
 
    void recordInstanceForRegion(String regionID, String nodeID, String instance) {
    
      String legacyNodeID = rootNodeMap_.get(nodeID);      
      String legacyInstance = getLegacyInstanceForRegion(nodeID, regionID);
      if ((legacyNodeID == null) || (legacyInstance == null)) {
        return;
      }
      String oldCombined = GenomeItemInstance.getCombinedID(legacyNodeID, legacyInstance);
      String newCombined = GenomeItemInstance.getCombinedID(nodeID, instance);      
      instanceNodeMap_.put(newCombined, oldCombined);
      return;
    }
    
    /***************************************************************************
    **
    ** Get legacy link instance for region tuple
    */  

    String getLegacyInstanceForRegionTuple(String linkID, String srcRegionID, String trgRegionID) {

      //
      // Get new old region keys from new region keys:
      //

      String oldSrcKey = regionKeyMap_.get(srcRegionID);
      if (oldSrcKey == null) {
        return (null);      
      }

      //
      // Find the legacy instance:
      //

      List<RegionTupleInstance> riList = legacyLinkInstanceMap_.get(linkID);
      if (riList == null) {
        return (null);
      }
      int regNum = riList.size();
      for (int i = 0; i < regNum; i++) {
        RegionTupleInstance rti = riList.get(i);
        if (rti.srcRegionID.equals(srcRegionID) && rti.targRegionID.equals(trgRegionID)) {
          return (rti.instance);
        }
      }
      return (null);    
    }
     
  /***************************************************************************
  **
  ** Record instance number for region
  */  
 
    void recordInstanceForRegionTuple(String srcRegionID, String trgRegionID,
                                      String linkID, String instance) {
 
      String legacyLinkID = rootLinkMap_.get(linkID);
      String legacyInstance = getLegacyInstanceForRegionTuple(linkID, srcRegionID, trgRegionID);
      if ((legacyLinkID == null) || (legacyInstance == null)) {
        return;
      }      
      String oldCombined = GenomeItemInstance.getCombinedID(legacyLinkID, legacyInstance);
      String newCombined = GenomeItemInstance.getCombinedID(linkID, instance);      
      instanceLinkMap_.put(newCombined, oldCombined);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Used to cache object IDs for instructions
  */ 
    
  public static class SubsetCacheValues {

    ArrayList<String> nodes;
    ArrayList<String> genes;
    ArrayList<String> links;
    
    SubsetCacheValues() {
      nodes = new ArrayList<String>();
      genes = new ArrayList<String>();
      links = new ArrayList<String>();
    }
    
    public String toString() {
      return ("SubsetCacheValues: nodes = " + nodes + " genes = " + genes + " links = " + links); 
    }
    
  }  
}
