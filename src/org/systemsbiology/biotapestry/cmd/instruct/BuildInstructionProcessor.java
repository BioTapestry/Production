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


package org.systemsbiology.biotapestry.cmd.instruct;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.BuildSupport;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveGroupSupport;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSupport;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Handles build instruction routing and processing
*/

public class BuildInstructionProcessor {
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
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
  
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private PIData pid_;
  private PIHData pihd_;
  private PISIFData pis_;
  private TabSource tSrc_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BuildInstructionProcessor(UIComponentSource uics, DataAccessContext dacx, TabSource tSrc, UndoFactory uFac) {
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    tSrc_ = tSrc;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Install build data
  */
  
  public void installPIData(PIData pid) {
    pid_ = pid;
    return;
  }
  
  /***************************************************************************
  **
  ** Install build data
  */
  
  public void installPIHData(PIHData pihd) {
    pihd_ = pihd;
    return;
  }
  
  /***************************************************************************
  **
  ** Install build data
  */
  
  public void installPISIFData(PISIFData pis) {
    pis_ = pis;
    return;
  }

  /***************************************************************************
  **
  ** Get the instructions for the genome
  */
  
  public List<BuildInstruction> getInstructions(StaticDataAccessContext dacx) {
    String key = dacx.getCurrentGenomeID();
    ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
    if (dacx.currentGenomeIsRootDBGenome()) {
      Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
      while (biit.hasNext()) {
        retval.add(biit.next());
      }
    } else {
      HashMap<String, BuildInstruction> biMap = new HashMap<String, BuildInstruction>();
      HashMap<String, BuildInstruction> newBiMap = new HashMap<String, BuildInstruction>();      
      Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
      while (biit.hasNext()) {
        BuildInstruction bi = biit.next();
        String bid = bi.getID();
        biMap.put(bid, bi);
        BuildInstruction newBi = bi.clone();
        newBi.setRegions(new InstructionRegions());
        newBiMap.put(bid, newBi);
        retval.add(newBi);
      }
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(key);
      if (iis != null) {
        Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
        while (iit.hasNext()) {
          BuildInstructionInstance bii = iit.next();
          String biid = bii.getBaseID();
          BuildInstruction newBi = newBiMap.get(biid);
          if (newBi == null) {
            throw new IllegalStateException();
          }
          InstructionRegions ireg = newBi.getRegions();
          ireg.addRegionTuple(new InstructionRegions.RegionTuple(bii.getSourceRegionID(), 
                                                                 bii.getTargetRegionID())); 
        }
      }      
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Export the instructions for the genome
  */
  
  public void exportInstructions(PrintWriter out, StaticDataAccessContext dacx) {
     
    //
    // Models:
    //
      
    out.println("\"# Model Commands\"");
      
    //
    // Gotta write out in tree order:
    //
    
    ArrayList<String> modelOrder = new ArrayList<String>();
  
    //
    // Dump the instances out in tree order to preserve it:
    //
    
    List<String> ordered = dacx.getGenomeSource().getModelHierarchy().getPreorderListing(false);
    Iterator<String> oit = ordered.iterator();    
    while (oit.hasNext()) {
      String gkey = oit.next();
      if (DynamicInstanceProxy.isDynamicInstance(gkey)) {
        continue;
      }
      modelOrder.add(gkey);
    }
 
    int nmo = modelOrder.size();
    for (int i = 0; i < nmo; i++) {
      String modelID = modelOrder.get(i);
      Genome genome = dacx.getGenomeSource().getGenome(modelID);
      out.print("\"model\",\"");
      if (i == 0) {
        out.print("root");
        out.print("\"");
        out.println();
      } else {
        GenomeInstance gi = (GenomeInstance)genome;
        out.print(gi.getName());
        out.print("\",");      
        GenomeInstance pgi = gi.getVfgParent();
        if (pgi == null) {
          out.println("\"root\"");
        } else {
          out.print("\"");
          out.print(pgi.getName());
          out.println("\"");         
        }       
      }
    }
    
    //
    // Regions:
    //
    
    out.println();
    out.println("\"# Region Commands\""); 
    for (int i = 1; i < nmo; i++) {
      String modID = modelOrder.get(i);
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(modID);
      if (iis != null) {
        GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(modID);
        Iterator<InstanceInstructionSet.RegionInfo> rit = iis.getRegionIterator();
        while (rit.hasNext()) {
          InstanceInstructionSet.RegionInfo ri = rit.next();
          out.print("\"region\",\"");
          out.print(gi.getName());
          out.print("\",\"");
          out.print(ri.name);
          out.print("\",\"");
          out.print(ri.abbrev);
          out.println("\"");            
        }
      }
    }
    
    //
    // Instructions:
    //

    out.println();
    out.println("\"# Root Model Interaction Commands\"");
    StringBuffer buf = new StringBuffer();
    Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      out.println(bi.toCSVString(buf, "root"));
    }
     
    out.println();
    out.println("\"# Instance Interaction Commands\"");   
    for (int i = 1; i < nmo; i++) {
      String modelID = modelOrder.get(i);
      GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(modelID);
      String modelName = gi.getName();
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(modelID);
      if (iis != null) {
        Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
        while (iit.hasNext()) {
          BuildInstructionInstance bii = iit.next();
          bii.getBaseID();
          out.println(bii.toCSVString(dacx_, buf, modelName));
        }
      }
      out.println();
    }
    return;
  }

  /***************************************************************************
  **
  ** preProcess the instructions for the genome
  */
  
  public BuildChanges preProcess(StaticDataAccessContext dacx, List<BuildInstruction> buildCmds) {
 
    //
    // If we are doing the full genome model, things are a little
    // easier.  Look for new instructions, deletions, edits, 
    // duplicates, etc.
    //
    
    if (dacx.currentGenomeIsRootDBGenome()) {      
      BuildChanges retval = new BuildChanges();        
      retval.changedParentInstances = null;
      retval.changedChildInstances = null;
      retval.changedSiblingCousinInstances = null;
      retval.needDeletionType = false;
    
      ArrayList<BuildInstruction> oldList = new ArrayList<BuildInstruction>();
      Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
      while (biit.hasNext()) {
        oldList.add(biit.next());
      }    
   
      Changes changes = compareInstructions(oldList, buildCmds);
      retval.haveNewDuplicates = changes.haveDuplicates;
      retval.haveNewLoneDuplicates = changes.haveLoneDuplicates;
      // Too much complexity in tracking superfluous lone nodes.
      //changes.newLoneSupers.removeAll(changes.oldLoneSupers);
      //retval.newLoneSupers = changes.newLoneSupers;
      return (retval);
    }
    
    //
    // Non-root cases.  First transfer the build commands to instruction
    // holders, where instructions a grouped based on sharing the same
    // core instruction, and an analysis of region requirements is created.
    //
    
    List<InstructionHolder> held = transferToHolders(buildCmds);
    ArrayList<BuildInstruction> rootCmds = new ArrayList<BuildInstruction>();
    InstanceInstructionSet iis = new InstanceInstructionSet(dacx.getCurrentGenomeID());
    ArrayList<DuplicateInstructionTracker.CoreCount> emptyCounts = new ArrayList<DuplicateInstructionTracker.CoreCount>();
    // This is where the instructions, as abtracted into holders, are
    // resolved into the needed root instructions and instance instructions.
    // We then scan for changes.  This process is repeated later during actual
    // processing....
    resolveHolders(held, rootCmds, iis, emptyCounts, dacx);      
    BuildChanges retval = scanForChanges(dacx, rootCmds, iis);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Look for newly-introduced core mismatches on instructions
  */
  
  public List<MatchChecker> findInstanceMismatches(StaticDataAccessContext dacx, List<BuildInstruction> buildCmds) {
    
    //
    // Build up data structure from original commands:
    //
    
    HashMap<String, MatchChecker> checks = new HashMap<String, MatchChecker>();
    Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      String bid = bi.getID();
      MatchChecker mc = new MatchChecker(bid, bi);
      checks.put(bid, mc);
    }
    
    //
    // Compare new to old, looking for differences:
    //
    
    int size = buildCmds.size();
    for (int i = 0; i < size; i++) {
      BuildInstruction bi = buildCmds.get(i);
      String bid = bi.getID();
      if (bid.equals("")) {
        continue;
      }
      MatchChecker mc = checks.get(bid);
      if (mc == null) {
        throw new IllegalStateException();
      }
      if (bi.sameDefinition(mc.original)) {
        mc.sameCores.add(new Integer(i));
      } else {
        int dcNum = mc.differentCores.size();
        boolean haveMatch = false;
        for (int j = 0; j < dcNum; j++) {
          CoreInfo ci = mc.differentCores.get(j);
          if (ci.core.sameDefinition(bi)) {
            ci.indices.add(new Integer(i));
            haveMatch = true;
            break;
          }
        }
        if (!haveMatch) {
          mc.differentCores.add(new CoreInfo(i, bi));
        }
      }  
    }
    
    //
    // Summarize the results:
    //
    
    ArrayList<MatchChecker> retval = new ArrayList<MatchChecker>();
    Iterator<String> kit = checks.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      MatchChecker mc = checks.get(key);
      int dcSize = mc.differentCores.size();
      int scSize = mc.sameCores.size();
      
      //
      // If everybody is the same as their originals, we are happy:
      //
      if (dcSize == 0) {
        continue;
      }
      //
      // If everybody has changed in a single consistent fashion,
      // we are happy:
      //
      if ((scSize == 0) && (dcSize == 1)) {
        continue;
      }
      //
      // We have a difference, so add the checker to the results.
      //
      retval.add(mc);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Process the instructions for the genome.  This is the route to network
  ** building from a particular genome-specific dialog....
  */
  
  public LinkRouter.RoutingResult processInstructions(StaticDataAccessContext rcxR) throws AsynchExitRequestException {
                                                        
    //
    // Multiple copies of a base instruction can be collapsed into a single base instruction
    // with separate instances, as long as (s,t) tuples are distinct.  If we have a duplicated
    // tuple, we need separate base instructions.
    //     
    //
    // Inbound instructions with multi-tuple region sets need to be turned into separate
    // buildInstructionInstances.
    //                                                        
                                                        
    double startFrac = 0.1;
    double maxFrac = 1.0;
    int genCount = rcxR.getGenomeSource().getGenomeCount();
    double perPass = (maxFrac - startFrac) / genCount;
    double midFrac = startFrac + perPass;    
    
    //
    // Figure out all module pad needs before we get started:
    //
    
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = rcxR.getFGHO().getGlobalNetModuleLinkPadNeeds();
    Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> emptyModNeeds = rcxR.getFGHO().stockUpMemberOnlyModules();
    Map<String, Layout.OverlayKeySet> allKeys = rcxR.getFGHO().fullModuleKeysPerLayout();    
    Map<String, Layout.SupplementalDataCoords> sdcCache = new BuildSupport(uics_, tSrc_, uFac_).buildSdcCache(allKeys, rcxR);
    
    boolean doRegions = !pid_.genomeID.equals(rcxR.getDBGenome().getID());
    LinkRouter.RoutingResult result = new LinkRouter.RoutingResult();
    if (!doRegions) {
      int numBC = pid_.buildCmds.size();
      for (int i = 0; i < numBC; i++) {
        BuildInstruction bi = pid_.buildCmds.get(i);
        if (bi.getID().equals("")) {
          bi.setID(rcxR.getInstructSrc().getNextInstructionLabel());
        }
      }
      
      ArrayList<DialogBuiltMotifPair> createdPairs = new ArrayList<DialogBuiltMotifPair>();
      HashMap<String, String> newNodeToOldNode = new HashMap<String, String>();
      HashMap<String, String> newLinksToOldLinks = new HashMap<String, String>();           
      
      BuildSupport.BSData bsd = new BuildSupport.BSData(pid_.buildCmds, pid_.center, pid_.size, pid_.keepLayout, pid_.hideNames,
                                                        pid_.options, pid_.support, pid_.monitor, startFrac, midFrac, 
                                                        createdPairs, newNodeToOldNode, newLinksToOldLinks, 
                                                        null, null, globalPadNeeds, pid_.specLayout, pid_.params);
      BuildSupport bs = new BuildSupport(uics_, bsd, tSrc_, uFac_);
      result = bs.buildRootFromInstructions(rcxR);
                             
      applyRootChanges(pid_.buildCmds, rcxR, pid_.support, createdPairs, pid_.center, pid_.size,
                       pid_.options, pid_.monitor, pid_.keepLayout, pid_.hideNames, 
                       newNodeToOldNode, newLinksToOldLinks, globalPadNeeds, midFrac, maxFrac);      

    } else { 
      InstanceInstructionSet iisOld = rcxR.getInstructSrc().getInstanceInstructionSet(pid_.genomeID);
      if (iisOld == null) {
        iisOld = new InstanceInstructionSet(pid_.genomeID);
      }
      
      List<InstructionHolder> held = transferToHolders(pid_.buildCmds);
      ArrayList<BuildInstruction> rootCmds = new ArrayList<BuildInstruction>();
      InstanceInstructionSet iis = new InstanceInstructionSet(pid_.genomeID);
      ArrayList<DuplicateInstructionTracker.CoreCount> emptyCounts = new ArrayList<DuplicateInstructionTracker.CoreCount>();
      resolveHolders(held, rootCmds, iis, emptyCounts, rcxR);
      if (pid_.genomeID == null) { //<- old code seemed to account for this possibility, but should be illegal?
        throw new IllegalStateException();
      }
      StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxR, pid_.genomeID);     
      killDeadSubGroups(rcxI, pid_.support);           
      result = propagateChanges(iisOld, iis, rcxR, rcxI, rootCmds, startFrac, midFrac, maxFrac, emptyCounts, globalPadNeeds);
    }

    ModificationCommands.repairEmptiedMemberOnlyModules(rcxR, emptyModNeeds, pid_.support); 
    ModificationCommands.repairNetModuleLinkPadsGlobally(rcxR, globalPadNeeds, false, pid_.support);

    (new BuildSupport(uics_, tSrc_, uFac_)).processSdcCache(sdcCache, allKeys, rcxR, pid_.support);
    
    rcxR.getGenomeSource().clearAllDynamicProxyCaches();
    RemoveGroupSupport.cleanupDanglingGroupData(rcxR, pid_.support);
    
    return (result);
  }

  /***************************************************************************
  **
  ** Process the instructions for the genome.  This is the route to building
  ** via the genome-wide CSV load...
  */
  
  public LinkRouter.RoutingResult processInstructionsForFullHierarchy(StaticDataAccessContext rcxR) throws AsynchExitRequestException {  

    LinkRouter.RoutingResult result = new LinkRouter.RoutingResult();
      
    //
    // Figure out all module pad needs before we get started:
    //
    
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = rcxR.getFGHO().getGlobalNetModuleLinkPadNeeds();
    Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> emptyModNeeds = rcxR.getFGHO().stockUpMemberOnlyModules(); 
    Map<String, Layout.OverlayKeySet> allKeys = rcxR.getFGHO().fullModuleKeysPerLayout();    
    Map<String, Layout.SupplementalDataCoords> sdcCache = (new BuildSupport(uics_, tSrc_, uFac_)).buildSdcCache(allKeys, rcxR);
  
    double perPass = (pihd_.maxFrac - pihd_.startFrac) / pihd_.processOrder.size();
    double currStart = pihd_.startFrac;
    double currFinish = currStart + perPass;

    //
    // At first glance, we should not need to kill off subgroups.  Subgroups in models 
    // that survive the CSV install are dropped since they are not specified as groups.
    // BUT we may have a case where a subgroup that needs to be killed has the same name
    // as a newly specified subgroup, which would be bad.
    //
      
    killAllSubGroups(rcxR, pihd_.support);    
    
    if (pihd_.processOrder.size() == 0) {
      return (result);
    }
    
    //
    // Do root first:
    //
    String rootID = pihd_.processOrder.get(0);
    ArrayList<DialogBuiltMotifPair> createdPairs = new ArrayList<DialogBuiltMotifPair>();
    if (pihd_.newNodeToOldNode == null) {
      pihd_.newNodeToOldNode = new HashMap<String, String>();
    }
    HashMap<String, String> newLinksToOldLinks = new HashMap<String, String>();
    BuildSupport bs = new BuildSupport(uics_, tSrc_, uFac_);
    
    List<BuildInstruction> rootCmds = pihd_.buildCmds.get(rootID);
    BuildSupport.BSData bsd = new BuildSupport.BSData(rootCmds, pihd_.center, pihd_.size, pihd_.keepLayout, pihd_.hideMinorNames,
                                                      pihd_.options, pihd_.support, pihd_.monitor, currStart, currFinish, 
                                                      createdPairs, pihd_.newNodeToOldNode, newLinksToOldLinks, 
                                                      pihd_.nodeIDMap, pihd_.dbGenomeCSVName, globalPadNeeds, pihd_.specLayout, pihd_.params);    
    bs.resetBSD(bsd);
    result = bs.buildRootFromInstructions(rcxR);
    currStart = currFinish;
    currFinish = currStart + perPass; 
    
    //
    // Now do instances:
    //
    
    HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>> subsetCache = 
        new HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>>();
    HashMap<String, Map<String, String>> subsetRegionCache = new HashMap<String, Map<String, String>>();
    Map<String, BuildSupport.LegacyInstanceIdMapper> liidmMap = new HashMap<String, BuildSupport.LegacyInstanceIdMapper>();
        
    int pNum = pihd_.processOrder.size();
    for (int i = 1; i < pNum; i++) {
      String genomeID = pihd_.processOrder.get(i);
      InstanceInstructionSet iisOld = rcxR.getInstructSrc().getInstanceInstructionSet(genomeID);
      if (iisOld == null) {
        iisOld = new InstanceInstructionSet(genomeID);
      }
      List<BuildInstruction> cmds = pihd_.buildCmds.get(genomeID);
      List<InstructionHolder> held = transferToHolders(cmds);
      InstanceInstructionSet iis = new InstanceInstructionSet(genomeID);
      ArrayList<DuplicateInstructionTracker.CoreCount> emptyCounts = new ArrayList<DuplicateInstructionTracker.CoreCount>();
      resolveHolders(held, new ArrayList<BuildInstruction>(), iis, emptyCounts, rcxR);  // don't need root commands...
      List<InstanceInstructionSet.RegionInfo> regionList = pihd_.regions.get(genomeID);
      iis.replaceRegions(regionList);
      DatabaseChange dc = rcxR.getInstructSrc().setInstanceInstructionSet(genomeID, iis);
      pihd_.support.addEdit(new DatabaseChangeCmd(rcxR, dc));

      StaticDataAccessContext rcxI = new StaticDataAccessContext(rcxR, genomeID); 
      if (rcxI.getCurrentGenomeAsInstance().getVfgParent() == null) {
        BuildSupport.LegacyInstanceIdMapper liidm = new BuildSupport.LegacyInstanceIdMapper(pihd_.newNodeToOldNode, newLinksToOldLinks); 
        liidmMap.put(genomeID, liidm);    
        BuildSupport.BSData bsd2 = new BuildSupport.BSData(createdPairs, pihd_.center,  pihd_.size, pihd_.support, pihd_.options, 
                                                           pihd_.monitor, subsetCache, subsetRegionCache, liidm, pihd_.nodeIDMap,
                                                           globalPadNeeds, pihd_.keepLayout, pihd_.hideMinorNames, currStart, currFinish); 
        bs.resetBSD(bsd2);
        bs.propagateRootUsingInstructions(rcxR, rcxI, tSrc_);
      } else {
        GenomeInstance pgi = rcxI.getCurrentGenomeAsInstance().getVfgParentRoot();
        BuildSupport.LegacyInstanceIdMapper liidm = liidmMap.get(pgi.getID());
        bs.resetBSD(null);
        bs.populateSubsetUsingInstructions(rcxI, pihd_.support, liidm, pihd_.nodeIDMap,
                                           subsetCache, subsetRegionCache);
      }
      
      currStart = currFinish;
      currFinish = currStart + perPass;     
    }
    
     
    RemoveSupport.fixupDeletionsInNonInstructionModels(uics_, tSrc_, rcxR, pihd_.support, uFac_);
    ModificationCommands.repairEmptiedMemberOnlyModules(rcxR, emptyModNeeds, pihd_.support); 
    ModificationCommands.repairNetModuleLinkPadsGlobally(rcxR, globalPadNeeds, false, pihd_.support);    
    
    
    // All keys can be stale after possible module deletions.  Do it again:
    allKeys = rcxR.getFGHO().fullModuleKeysPerLayout(); 
    bs.resetBSD(null);
    bs.processSdcCache(sdcCache, allKeys, rcxR, pihd_.support);
    rcxR.getGenomeSource().clearAllDynamicProxyCaches();
    RemoveGroupSupport.cleanupDanglingGroupData(rcxR, pihd_.support);  

    return (result);
  }
  
  /***************************************************************************
  **
  ** Process the instructions for the genome
  */
  
  public LinkRouter.RoutingResult processInstructionsForSIF(StaticDataAccessContext rcxR) throws AsynchExitRequestException {  
                                                      
    double midFrac = (pis_.maxFrac - pis_.startFrac) / 2.0; 
    if ((pis_.monitor != null) && !pis_.monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }

    BuildSupport.BSData bsd = new BuildSupport.BSData(pis_.buildCmds, pis_.center, pis_.size, pis_.keepLayout, pis_.hideNames,
                                                      pis_.options, pis_.support, pis_.monitor, pis_.startFrac, midFrac, 
                                                      pis_.createdPairs, pis_.newNodeToOldNode, pis_.newLinksToOldLinks, 
                                                      null, null, pis_.globalPadNeeds, pis_.specLayout, pis_.params);
    
    BuildSupport bs = new BuildSupport(uics_, bsd, tSrc_, uFac_);
    LinkRouter.RoutingResult result = bs.buildRootFromInstructions(rcxR);
    if ((pis_.monitor != null) && !pis_.monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    applyRootChanges(pis_.buildCmds, rcxR, pis_.support, pis_.createdPairs, pis_.center, pis_.size,
                     pis_.options, pis_.monitor, pis_.keepLayout, pis_.hideNames, 
                     pis_.newNodeToOldNode, pis_.newLinksToOldLinks, pis_.globalPadNeeds, midFrac, pis_.maxFrac);
    if ((pis_.monitor != null) && !pis_.monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    ModificationCommands.repairEmptiedMemberOnlyModules(rcxR,  pis_.emptyModNeeds, pis_.support); 
    ModificationCommands.repairNetModuleLinkPadsGlobally(rcxR, pis_.globalPadNeeds, false, pis_.support);
    
    bs.resetBSD(null);
    bs.processSdcCache(pis_.sdcCache, pis_.allKeys, rcxR, pis_.support);
    rcxR.getGenomeSource().clearAllDynamicProxyCaches();    
    return (result);
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  /////////////////////////////////////////////////-/////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to report on changes
  */ 
    
  public static final class BuildChanges {
       
    public List<?> deletions_AM_I_USED;
    public boolean needDeletionType;
    public Set<String> changedParentInstances;
    public Set<String> changedChildInstances;
    public Set<String> changedSiblingCousinInstances; 
    public boolean haveNewDuplicates;
    public boolean haveNewLoneDuplicates;
    //public Set newLoneSupers;
    
    public BuildChanges() {
    }
  }
  
  /***************************************************************************
  **
  ** Used to report on changes
  */ 

  public static class MatchChecker {

    public String instanceID;
    public BuildInstruction original;
    public List<Integer> sameCores;
    public List<CoreInfo> differentCores;
    
    MatchChecker(String instanceID, BuildInstruction original) {
      this.instanceID = instanceID;
      this.original = original;
      this.sameCores = new ArrayList<Integer>();  // list of indices
      this.differentCores = new ArrayList<CoreInfo>(); // list of CoreInfo
    }
  }      
  
  public static class CoreInfo {

    public List<Integer> indices;
    public BuildInstruction core; 
    
    CoreInfo(int index, BuildInstruction core) {
      this.indices = new ArrayList<Integer>();
      this.indices.add(new Integer(index));
      this.core = core;
    }
    
   CoreInfo(BuildInstruction core) {
      this.indices = new ArrayList<Integer>();
      this.core = core;
    }   
    
  } 
  
  /***************************************************************************
  **
  ** Used to hold info on instances to process
  */ 
    
  public static class InstanceToProcess {

    static final int CHILD             = 0;
    static final int PARENT            = 1;
    static final int SELF              = 2;    
    static final int COUSIN_OR_SIBLING = 3;
    
    String id;
    int type;
    
    InstanceToProcess(String id, int type) {
      this.id = id;
      this.type = type;
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply changes.  This is for the single-genome dialog based build
  */
  
  private LinkRouter.RoutingResult propagateChanges(InstanceInstructionSet iisOld, InstanceInstructionSet iisNew, 
                                                    StaticDataAccessContext rcxR, StaticDataAccessContext rcxI,
                                                    List<BuildInstruction> rootCmds, 
                                                    double startFrac, double midFrac, double maxFrac, 
                                                    List<DuplicateInstructionTracker.CoreCount> emptyCounts, 
                                                    Map<String, Layout.PadNeedsForLayout> globalPadNeeds)
                                                    throws AsynchExitRequestException {

    
    //
    // Install the new instructions into the root:
    //
    
    DatabaseChange dc = rcxR.getInstructSrc().setBuildInstructions(rootCmds);
    pid_.support.addEdit(new DatabaseChangeCmd(rcxR, dc));
    iisNew.replaceRegions(pid_.regions);      
    
    //
    // Figure out how instances change:
    //
    
    ChangesWithRegions cwr = calcInstanceChanges(iisOld, iisNew);
    List<InstanceToProcess> procList = buildProcessingList(false, rcxI);
    HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>> subsetCache = 
        new HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>>();
    HashMap<String, Map<String, String>> subsetRegionCache = new HashMap<String, Map<String, String>>();
    int pSize = procList.size();
    
    //
    // Make changes to all instruction lists
    //

    for (int i = 0; i < pSize; i++) {
      InstanceToProcess itp = procList.get(i);
      switch (itp.type) {
        case InstanceToProcess.SELF:
          dc = rcxI.getInstructSrc().setInstanceInstructionSet(itp.id, iisNew);
          pid_.support.addEdit(new DatabaseChangeCmd(rcxI, dc));
          break;
        case InstanceToProcess.PARENT:
          StaticDataAccessContext rcxP = new StaticDataAccessContext(rcxI, itp.id);
          changeParent(rcxP, rootCmds, cwr, pid_.globalDelete, pid_.support);
          break;
        case InstanceToProcess.CHILD:
          StaticDataAccessContext rcxC = new StaticDataAccessContext(rcxI, itp.id);
          changeChild(rcxC, cwr, pid_.support);
          break;
        case InstanceToProcess.COUSIN_OR_SIBLING:
          StaticDataAccessContext rcxCCS = new StaticDataAccessContext(rcxI, itp.id);
          changeCousinOrSibling(rcxCCS, rootCmds, pid_.support);
          break;
        default:
          throw new IllegalStateException();
      }
    }
        
    //
    // Optimize instruction lists to remove duplicates introduced:
    //
    
    DuplicateInstructionTracker dit = new DuplicateInstructionTracker();
    List<DuplicateInstructionTracker.CoreData> analysis = dit.analyzeAllCores(rcxI, emptyCounts);  
    List<BuildInstruction> newCmds = dit.optimize(analysis, pid_.support, rcxI);
    if (newCmds != null) {
      rootCmds = newCmds;
    }

    //
    // Do actual build and layout of root:
    //
    
    ArrayList<DialogBuiltMotifPair> createdPairs = new ArrayList<DialogBuiltMotifPair>();
    HashMap<String, String> newNodeToOldNode = new HashMap<String, String>();
    HashMap<String, String> newLinksToOldLinks = new HashMap<String, String>();
    
    BuildSupport.BSData bsd = new BuildSupport.BSData(rootCmds, pid_.center, pid_.size, pid_.keepLayout, pid_.hideNames, pid_.options, 
                                                      pid_.support, pid_.monitor, startFrac, midFrac, 
                                                      createdPairs, newNodeToOldNode,
                                                      newLinksToOldLinks, null, null, globalPadNeeds, pid_.specLayout, pid_.params);
    BuildSupport bs = new BuildSupport(uics_, bsd, tSrc_, uFac_);   
    LinkRouter.RoutingResult result = bs.buildRootFromInstructionsNoRootInstall(rcxR);  
      
    //
    // Build new models out of new instructions:
    //
    
    double perPass = (pSize == 0) ? (maxFrac - midFrac) : ((maxFrac - midFrac) / pSize);
    double currStart = midFrac;
    double currFinish = currStart + perPass;
    Map<String, BuildSupport.LegacyInstanceIdMapper> liidmMap = new HashMap<String, BuildSupport.LegacyInstanceIdMapper>();
    
    for (int i = 0; i < pSize; i++) {
      InstanceToProcess itp = procList.get(i);      
      StaticDataAccessContext rcxITP = new StaticDataAccessContext(rcxR, itp.id);
      InstanceInstructionSet iis = rcxITP.getInstructSrc().getInstanceInstructionSet(itp.id);
      
      if (iis != null) {
        if (rcxITP.getCurrentGenomeAsInstance().getVfgParent() == null) {
          BuildSupport.LegacyInstanceIdMapper liidm = new BuildSupport.LegacyInstanceIdMapper(newNodeToOldNode, newLinksToOldLinks); 
          liidmMap.put(rcxITP.getCurrentGenomeID(), liidm);
          BuildSupport.BSData bsd2 = new BuildSupport.BSData(createdPairs, pid_.center,  pid_.size, pid_.support, pid_.options, 
                                                             pid_.monitor, subsetCache, subsetRegionCache, liidm, null,
                                                             globalPadNeeds, pid_.keepLayout, pid_.hideNames, currStart, currFinish); 
          bs.resetBSD(bsd2);
          bs.propagateRootUsingInstructions(rcxR, rcxITP, tSrc_);
        } else {
          GenomeInstance pgi =  rcxITP.getCurrentGenomeAsInstance().getVfgParentRoot();
          BuildSupport.LegacyInstanceIdMapper liidm = liidmMap.get(pgi.getID());
          bs.resetBSD(null);
          bs.populateSubsetUsingInstructions(rcxITP, pid_.support, liidm, null,
                                             subsetCache, subsetRegionCache);
        }
      }
      currStart = currFinish;
      currFinish = currStart + perPass;
    }
        
    RemoveSupport.fixupDeletionsInNonInstructionModels(uics_, tSrc_, rcxR, pid_.support, uFac_);
    
    return (result);
  }    

  /***************************************************************************
  **
  ** Apply root changes
  */
  
  private void applyRootChanges(List<BuildInstruction> rootCmds, StaticDataAccessContext rcxR, 
                                UndoSupport support, List<DialogBuiltMotifPair> createdPairs,
                                Point2D center, Dimension size,
                                LayoutOptions options, BTProgressMonitor monitor,
                                boolean keepLayout, boolean hideNames, Map<String, String> newNodeToOldNode, Map<String, String> newLinksToOldLinks,
                                Map<String, Layout.PadNeedsForLayout> globalPadNeeds, 
                                double startFrac, double maxFrac)
                                throws AsynchExitRequestException {
    //
    // The only change that counts are root deletions.  Edits resolve during rebuild.
    //

    List<GenomeInstance> rootList = new ArrayList<GenomeInstance>();
    Iterator<GenomeInstance> giit = rcxR.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance currGi = giit.next();
      StaticDataAccessContext rcxCG = new StaticDataAccessContext(rcxR, currGi);
      applyRootDeletions(rcxCG, rootCmds, support);
      if (currGi.getVfgParent() == null) {
        rootList.add(currGi);
      }
    }
    
    //
    // Go through each root instance and do a rebuild of it and kids
    //
    BuildSupport bs = new BuildSupport(uics_, tSrc_, uFac_);

    Map<String, BuildSupport.LegacyInstanceIdMapper> liidmMap = new HashMap<String, BuildSupport.LegacyInstanceIdMapper>();
    int numRoot = rootList.size();
    for (int j = 0; j < numRoot; j++) {
      GenomeInstance gi = rootList.get(j);
      StaticDataAccessContext dacxI = new StaticDataAccessContext(rcxR, gi);
      List<InstanceToProcess> procList = buildProcessingList(true, dacxI);    
      HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>> subsetCache = 
        new HashMap<String, Map<String, Map<GenomeInstance.GroupTuple, BuildSupport.SubsetCacheValues>>>();
      HashMap<String, Map<String, String>> subsetRegionCache = new HashMap<String, Map<String, String>>();
      int pSize = procList.size();
    
      double perPass = (pSize == 0) ? (maxFrac - startFrac) : ((maxFrac - startFrac) / pSize);
      double currStart = startFrac;
      double currFinish = currStart + perPass;

      for (int i = 0; i < pSize; i++) {
        InstanceToProcess itp = procList.get(i);
        StaticDataAccessContext rcxITP = new StaticDataAccessContext(rcxR, itp.id);
        InstanceInstructionSet iis = rcxITP.getInstructSrc().getInstanceInstructionSet(itp.id);
        if (iis != null) {
          switch (itp.type) {
            case InstanceToProcess.SELF:
              BuildSupport.LegacyInstanceIdMapper liidm = new BuildSupport.LegacyInstanceIdMapper(newNodeToOldNode, newLinksToOldLinks); 
              liidmMap.put(itp.id, liidm);
              BuildSupport.BSData bsd = new BuildSupport.BSData(createdPairs, center,  size, support, options, 
                                                                monitor, subsetCache, subsetRegionCache, liidm, null,
                                                                globalPadNeeds, keepLayout, hideNames, currStart, currFinish);
              bs.resetBSD(bsd);
              bs.propagateRootUsingInstructions(rcxR, rcxITP, tSrc_);
              break;
            case InstanceToProcess.CHILD:
              GenomeInstance pgi = rcxITP.getCurrentGenomeAsInstance().getVfgParentRoot();
              liidm = liidmMap.get(pgi.getID());
              bs.resetBSD(null);
              bs.populateSubsetUsingInstructions(rcxITP, support, liidm, null, subsetCache, subsetRegionCache);
              break;
            case InstanceToProcess.PARENT:  
            case InstanceToProcess.COUSIN_OR_SIBLING:
            default:
              throw new IllegalStateException();
          }
        }
        currStart = currFinish;
        currFinish = currStart + perPass;
      }
    }
    
    
    //
    // In theory, subset models without instructions should have had deletions applied
    // during root build.  But is it the case that intervening root instances with
    // instructions may have rebuilt above and introduced new changes?  Let's be safe:
    //
     
    RemoveSupport.fixupDeletionsInNonInstructionModels(uics_, tSrc_, rcxR, support, uFac_);    
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply changes to a child
  */
  
  private void changeChild(StaticDataAccessContext dacx, ChangesWithRegions cwr, UndoSupport support) {

    //
    // Don't care about additions, but deleted instructions and
    // regions must be deleted
    //
    InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(dacx.getCurrentGenomeID());
    if (iis == null) {
      return;
    } else {
      iis = new InstanceInstructionSet(iis);
    }    
    
    Iterator<InstanceInstructionSet.RegionInfo> drit = cwr.deletedRegions.iterator();
    while (drit.hasNext()) {
      InstanceInstructionSet.RegionInfo ri = drit.next();  
      iis.deleteRegion(ri.abbrev);
    }
    Iterator<BuildInstructionInstance> dit = cwr.deletedInstruct.iterator();
    while (dit.hasNext()) {
      BuildInstructionInstance bii = dit.next();  
      iis.deleteInstruction(bii);
    }
    
    DatabaseChange dc = dacx.getInstructSrc().setInstanceInstructionSet(dacx.getCurrentGenomeID(), iis);
    support.addEdit(new DatabaseChangeCmd(dacx, dc));
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply changes to a parent
  */
  
  private void changeParent(StaticDataAccessContext dacx, List<BuildInstruction> rootCmds, ChangesWithRegions cwr, 
                            boolean forceDelete, UndoSupport support) {

    //
    // Instructions added to child must be added to parents, unless
    // they are already there.  Same with regions.  Deletions only
    // applied if root instruction is deleted or we have a force levels
    // flag.  
    //

    //
    // Find existing base instructions:
    //
    
    HashSet<String> cmdIDs = new HashSet<String>();
    int numBC = rootCmds.size();
    for (int i = 0; i < numBC; i++) {
      BuildInstruction bi = rootCmds.get(i);
      cmdIDs.add(bi.getID());
    }
    
    //
    // Record my regions and instructions:
    //
    
    InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(dacx.getCurrentGenomeID());
    if (iis == null) {
      iis = new InstanceInstructionSet(dacx.getCurrentGenomeID());
    } else {
      iis = new InstanceInstructionSet(iis);      
    }

    HashSet<InstanceInstructionSet.RegionInfo> myRegions = new HashSet<InstanceInstructionSet.RegionInfo>();
    Iterator<InstanceInstructionSet.RegionInfo> rit = iis.getRegionIterator();
    while (rit.hasNext()) {
      InstanceInstructionSet.RegionInfo regI = rit.next();
      // FIX ME!!! Abbrev not enough info to go on.
      myRegions.add(new InstanceInstructionSet.RegionInfo(regI));
    }
    
    HashSet<BuildInstructionInstance> myInstruct = new HashSet<BuildInstructionInstance>();
    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      myInstruct.add(bii);
    }    
    
    //
    // Add regions that my kid has that I do not have.  Same for instructions:
    //
 
    Iterator<InstanceInstructionSet.RegionInfo> arit = cwr.addedRegions.iterator();
    while (arit.hasNext()) {
      InstanceInstructionSet.RegionInfo ri = arit.next();
      if (!myRegions.contains(ri)) {
        iis.addRegion(new InstanceInstructionSet.RegionInfo(ri));
      }
    }
    
    Iterator<BuildInstructionInstance> ait = cwr.addedInstruct.iterator();
    while (ait.hasNext()) {
      BuildInstructionInstance bii = ait.next();
      if (!myInstruct.contains(bii)) {
        iis.addInstruction(new BuildInstructionInstance(bii));
      }
    }    
    
    //
    // Delete instructions that have been deleted from the root, or if
    // we are forced to delete:
    //
    
    if (forceDelete) {
      Iterator<InstanceInstructionSet.RegionInfo> drit = cwr.deletedRegions.iterator();
      while (drit.hasNext()) {
        InstanceInstructionSet.RegionInfo ri = drit.next();
        iis.deleteRegion(ri.abbrev);
      }
    }
    Iterator<BuildInstructionInstance> dit = cwr.deletedInstruct.iterator();
    while (dit.hasNext()) {
      BuildInstructionInstance bii = dit.next();
      if (forceDelete) {
        iis.deleteInstruction(bii);
      }
    }    

    HashSet<String> deadSet = new HashSet<String>();
    Iterator<BuildInstructionInstance> isit = iis.getInstructionIterator();
    while (isit.hasNext()) {
      BuildInstructionInstance bii = isit.next();
      String baseID = bii.getBaseID();
      if (!cmdIDs.contains(baseID)) {
        deadSet.add(baseID);
      }
    }
    Iterator<String> dsit = deadSet.iterator();
    while (dsit.hasNext()) {
      String bid = dsit.next();
      iis.deleteInstructions(bid);
    }
    
    DatabaseChange dc = dacx.getInstructSrc().setInstanceInstructionSet(dacx.getCurrentGenomeID(), iis);
    support.addEdit(new DatabaseChangeCmd(dacx, dc));    
    
    return;
  }  

  /***************************************************************************
  **
  ** Change a cousin
  */
  
  private void changeCousinOrSibling(StaticDataAccessContext dacx, List<BuildInstruction> rootCmds, UndoSupport support) {
    
    InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(dacx.getCurrentGenomeID());
    if (iis == null) {
      return;
    }
    applyRootDeletions(dacx, rootCmds, support);      
    return;
  }  

  /***************************************************************************
  **
  ** Apply root deletions to instances
  */
  
  private void applyRootDeletions(StaticDataAccessContext dacx, List<BuildInstruction> rootCmds, UndoSupport support) {
    
    //
    // Find existing base instructions:
    //
    
    HashSet<String> cmdIDs = new HashSet<String>();
    int numBC = rootCmds.size();
    for (int i = 0; i < numBC; i++) {
      BuildInstruction bi = rootCmds.get(i);
      cmdIDs.add(bi.getID());
    }
    
    //
    // Record dead instructions:
    //
    
    InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(dacx.getCurrentGenomeID());
    if (iis == null) {
      return;
    } else {
      iis = new InstanceInstructionSet(iis);
    }
    
    HashSet<BuildInstructionInstance> deadInstruct = new HashSet<BuildInstructionInstance>();
    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      if (!cmdIDs.contains(bii.getBaseID())) {
        deadInstruct.add(bii);
      }
    }    
    
    Iterator<BuildInstructionInstance> dit = deadInstruct.iterator();
    while (dit.hasNext()) {
      BuildInstructionInstance bii = dit.next();
      iis.deleteInstruction(bii);
    }
    
    DatabaseChange dc = dacx.getInstructSrc().setInstanceInstructionSet(dacx.getCurrentGenomeID(), iis);
    support.addEdit(new DatabaseChangeCmd(dacx, dc));
    
    return;
  }  

  /***************************************************************************
  **
  ** Figure out changes to instances
  */
  
  private ChangesWithRegions calcInstanceChanges(InstanceInstructionSet iisOld, InstanceInstructionSet iisNew) {
    
    ChangesWithRegions cwr = new ChangesWithRegions();

    //
    // Figure region changes
    //
    
    HashSet<InstanceInstructionSet.RegionInfo> oldRegions = new HashSet<InstanceInstructionSet.RegionInfo>();
    Iterator<InstanceInstructionSet.RegionInfo> ori = iisOld.getRegionIterator();
    while (ori.hasNext()) {
      InstanceInstructionSet.RegionInfo regI = ori.next();
      oldRegions.add(new InstanceInstructionSet.RegionInfo(regI));
    }
      
    HashSet<InstanceInstructionSet.RegionInfo> newRegions = new HashSet<InstanceInstructionSet.RegionInfo>();
    Iterator<InstanceInstructionSet.RegionInfo> nri = iisNew.getRegionIterator();
    while (nri.hasNext()) {
      InstanceInstructionSet.RegionInfo regI = nri.next();
      newRegions.add(new InstanceInstructionSet.RegionInfo(regI));
    }      

    cwr.deletedRegions.addAll(oldRegions);
    cwr.deletedRegions.removeAll(newRegions);

    cwr.addedRegions.addAll(newRegions);
    cwr.addedRegions.removeAll(oldRegions);    

    //
    // Figure instruction changes
    //

    HashSet<BuildInstructionInstance> oldInstruct = new HashSet<BuildInstructionInstance>();
    Iterator<BuildInstructionInstance> oii = iisOld.getInstructionIterator();
    while (oii.hasNext()) {
      BuildInstructionInstance bii = oii.next();
      oldInstruct.add(bii);
    }
      
    HashSet<BuildInstructionInstance> newInstruct = new HashSet<BuildInstructionInstance>();
    Iterator<BuildInstructionInstance> nii = iisNew.getInstructionIterator();
    while (nii.hasNext()) {
      BuildInstructionInstance bii = nii.next();
      newInstruct.add(bii);
    }    
    
    cwr.deletedInstruct.addAll(oldInstruct);
    cwr.deletedInstruct.removeAll(newInstruct);

    cwr.addedInstruct.addAll(newInstruct);
    cwr.addedInstruct.removeAll(oldInstruct);    
        
    return (cwr);
  }

/*******************************************************************************
**
** Notes on instructions coalesing and dividing:

S = field the same   D = field is different
-----------------------
S S D -> S S S 

  57 a->b X->Y   >>>   57 a->b X->Y
  57 a->b Y->Z   >>>   57 a->b X->Y

  Either change second instruction to a new ID (e.g. 58), or 
  grab another existing instruction with same core.

S S D -> S D S 

  57 a->b X->Y   >>>   57 a->b X->Y
  57 a->b Y->Z   >>>   57 c->d X->Yo

  Change second instruction to new ID (e.g. 58), or ask user
  if they want to force all 57's to new core (or error out).

S S D -> S D D 

  57 a->b X->Y   >>>   57 a->b X->Y
  57 a->b Y->Z   >>>   57 c->d Y->Z

  Same as above.
-----------------------
D S S -> D D S 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 a->b X->Y   >>>   58 c->d X->Y

  Nothing special...

D S S -> D S D 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 a->b X->Y   >>>   58 a->b Y->Z

  Could drop 58, if nobody else required a double interaction.

D S S -> D D D 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 a->b X->Y   >>>   58 c->d Y->Z

  Nothing special...
-----------------------
D D S -> D S S 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 c->d X->Y   >>>   58 a->b X->Y

  Nothing special, though we should warn on the new duplicate instruction.
  Other models can collapse to all 57 refs if there is no conflict.

D D S -> D S D 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 c->d X->Y   >>>   58 a->b Y->Z

  Could collapse to 57, if no other model needed 58 to handle duplicate
  cases.
-----------------------
D D D -> D S S 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 c->d Y->Z   >>>   58 a->b X->Y

  Nothing special, though we should warn on the new duplicate instruction.

D D D -> D S D 

  57 a->b X->Y   >>>   57 a->b X->Y
  58 c->d Y->Z   >>>   58 a->b Y->Z

  Could collapse to 57, if no other model needed 58 to handle duplicate
  cases.
  
*/  
 
  /***************************************************************************
  **
  ** Resolve holders into root instructions and instance instructions.
  */
  
  private void resolveHolders(List<InstructionHolder> holders, List<BuildInstruction> rootCmds, 
                              InstanceInstructionSet iis, List<DuplicateInstructionTracker.CoreCount> emptyCounts,
                              StaticDataAccessContext dacx) {
    //
    // For unassigned region tuples, see if we can add them into existing root
    // instructions.  If not, create a new root instruction.  If we have dups,
    // create as many new root instructions as needed.
    //
    
    int hSize = holders.size();
    for (int i = 0; i < hSize; i++) {
      InstructionHolder ihold = holders.get(i);
      if (ihold.unassigned.getNumTuples() != 0) {
        InstructionRegions ua = ihold.unassigned;
        Iterator<InstructionRegions.RegionTuple> rtit = ua.getRegionTuples();
        while (rtit.hasNext()) {
          InstructionRegions.RegionTuple tup = rtit.next();
          pushToAssigned(ihold, tup, dacx);
        }
      }
      ihold.unassigned = new InstructionRegions();  // Drop unassigned...
    }
        
    //
    // Everybody is assigned.  Go through and create new roots, and new instance instr:
    //
    
    for (int i = 0; i < hSize; i++) {
      InstructionHolder ihold = holders.get(i);
      Iterator<String> arkit = ihold.assignedRegions.keySet().iterator();
      //
      // When we are dealing with a preexisting instruction from the root with an ID
      // already assigned, it will be added here because the holder has an assigned
      // region for it; For completely new instructions with
      // no ID yet and no tuples, they will be added below
      //
      while (arkit.hasNext()) {
        String key = arkit.next();
        InstructionRegions iReg = ihold.assignedRegions.get(key);
        BuildInstruction abi = ihold.coreInstruct.clone();
        abi.setID(key);
        rootCmds.add(abi);
        if (iReg.getNumTuples() == 0) {
          throw new IllegalStateException();
        }
        Iterator<InstructionRegions.RegionTuple> irit = iReg.getRegionTuples();
        while (irit.hasNext()) {
          InstructionRegions.RegionTuple tup = irit.next();
          BuildInstructionInstance bii = 
            new BuildInstructionInstance(key, tup.sourceRegion, tup.targetRegion); //, null, LinkageInstance.ACTIVE);
          iis.addInstruction(bii);
        }
      }
         
      //
      // If we are holding a previously assigned instruction with no regions assigned
      // to it, we need blank instructions equal to the count
      //
      
      Iterator<String> aekit = ihold.assignedEmpty.keySet().iterator();
      int totalNeeded = 0;
      HashSet<String> available = new HashSet<String>();
      while (aekit.hasNext()) {
        String key = aekit.next();
        Integer needed = ihold.assignedEmpty.get(key);
        InstructionRegions iReg = ihold.assignedRegions.get(key);
        if (iReg == null) {
          available.add(key);
        }
        totalNeeded += needed.intValue();
      }
      
      int remainingEmpties = totalNeeded + ihold.emptyUnassigned;      
      DuplicateInstructionTracker.CoreCount count = 
        new DuplicateInstructionTracker.CoreCount(ihold.coreInstruct.clone(), remainingEmpties);
      emptyCounts.add(count);
       
      Iterator<String> avit = available.iterator();
      while (avit.hasNext()) {
        String key = avit.next();
        if (remainingEmpties == 0) {
          break;
        }
        BuildInstruction abi = ihold.coreInstruct.clone();
        abi.setID(key);
        rootCmds.add(abi);
        remainingEmpties--;
      }
      
      
      //
      // If we are holding an instruction with no regions assigned to it, we need to
      // still create a blank root instruction.
      //
      
      for (int j = 0; j < remainingEmpties; j++) {
        String instrID = dacx.getInstructSrc().getNextInstructionLabel();      
        BuildInstruction abi = ihold.coreInstruct.clone();
        abi.setID(instrID);
        rootCmds.add(abi);
      }      
      
    }
    return;
  }

  
  /***************************************************************************
  **
  ** Assign an unassigned tuple
  */
  
  private void pushToAssigned(InstructionHolder ihold, InstructionRegions.RegionTuple tup, StaticDataAccessContext dacx) {
    //
    // Drop it in the first IR that accommodates it.  If none accommodate it, make a new
    // entry.
    //
    
    
    Iterator<String> arkit = ihold.assignedRegions.keySet().iterator();
    while (arkit.hasNext()) {
      String key = arkit.next();
      InstructionRegions iReg = ihold.assignedRegions.get(key);
      if (!iReg.hasTuple(tup)) {
        iReg.addRegionTuple(new InstructionRegions.RegionTuple(tup));
        return;
      }
    }
    
    //
    // Need new entry
    //
    
    String instrID = dacx.getInstructSrc().getNextInstructionLabel();
    InstructionRegions newIR = new InstructionRegions();
    newIR.addRegionTuple(new InstructionRegions.RegionTuple(tup));
    ihold.assignedRegions.put(instrID, newIR);
    return;
  }
    
  /***************************************************************************
  **
  ** Transfer instructions to holders
  */
  
  private List<InstructionHolder> transferToHolders(List<BuildInstruction> buildCmds) {  
    
    TreeMap<Integer, InstructionHolder> trackIndex = new TreeMap<Integer, InstructionHolder>();
    HashMap<BuildInstruction.BIWrapper, InstructionHolder> ihMap = new HashMap<BuildInstruction.BIWrapper, InstructionHolder>();
    
    int count = 0;
    int bcSize = buildCmds.size();
    for (int i = 0; i < bcSize; i++) {
      BuildInstruction bi = buildCmds.get(i);
      BuildInstruction.BIWrapper biw = new BuildInstruction.BIWrapper(bi);
      InstructionHolder ihold = ihMap.get(biw);
      if (ihold != null) {
        if (!ihold.coreInstruct.sameDefinition(bi)) {
          throw new IllegalArgumentException();
        }
        mergeInstructions(ihold, bi);
      } else {
        InstructionHolder nih = new InstructionHolder(bi);
        ihMap.put(biw, nih);
        trackIndex.put(new Integer(count++), nih);
      }  
    }
    
    ArrayList<InstructionHolder> held = new ArrayList<InstructionHolder>(trackIndex.values());  
    return (held);
  }
  
  /***************************************************************************
  **
  ** Merge an instruction into a holder
  */
  
  private void mergeInstructions(InstructionHolder ihold, BuildInstruction bi) {
    //
    // If the instruction has no ID, add regions to the unassigned set.  If it
    // does have an ID, add to correct assigned set. 
    //
    
    InstructionRegions biir = bi.getRegions();
    if (biir == null) {
      throw new IllegalArgumentException();
    }
 
    String instrID = bi.getID();
    if (instrID.equals("")) {
      if (biir.getNumTuples() == 0) {
        ihold.emptyUnassigned++;
      } else {
        ihold.unassigned.merge(biir);
      }
    } else {
      if (biir.getNumTuples() == 0) {
        Integer emptyCount = ihold.assignedEmpty.get(instrID);
        int newCount = (emptyCount == null) ? 1 : emptyCount.intValue() + 1;
        ihold.assignedEmpty.put(instrID, new Integer(newCount));
      } else {
        InstructionRegions toCheck = ihold.assignedRegions.get(instrID);
        if (toCheck == null) {
          toCheck = new InstructionRegions(biir);
          ihold.assignedRegions.put(instrID, toCheck);
        } else {
           toCheck.mergeUniquely(biir, ihold.unassigned);
        }
        if (toCheck.hasDuplicates()) {  // Should be caught above...    
          throw new IllegalStateException();
        }
      }
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Scan for changes
  */
  
  private BuildChanges scanForChanges(StaticDataAccessContext dacx, List<BuildInstruction> newList, 
                                      InstanceInstructionSet newIis) {

    BuildChanges retval = new BuildChanges();        
    retval.changedParentInstances = new HashSet<String>();
    retval.changedChildInstances = new HashSet<String>();
    //retval.newLoneSupers = new HashSet();
    retval.changedSiblingCousinInstances = new HashSet<String>();   
    retval.needDeletionType = false;
    
    //
    // Compare the root instruction to look for basic adds, deletes,
    // edits, and new duplicates:
    //
    
    ArrayList<BuildInstruction> oldList = new ArrayList<BuildInstruction>();

    Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
    while (biit.hasNext()) {
      oldList.add(biit.next());
    }    
    
    Changes changes = compareInstructions(oldList, newList);
    retval.haveNewDuplicates = changes.haveDuplicates;
    retval.haveNewLoneDuplicates = changes.haveLoneDuplicates;
    
    // Too much complexity detecting superfluous lone nodes.
    //if (!changes.newLoneSupers.isEmpty()) {
    //  InstanceInstructionSet oldIis = db.getInstanceInstructionSet(myKey);
    //  Set origSupers = getSuperfluousLoneNodesWithRegionContext(oldIis, changes.oldLoneSupers, oldList);
    //  Set remainingSupers = getSuperfluousLoneNodesWithRegionContext(newIis, changes.newLoneSupers, newList);
    //  remainingSupers.removeAll(origSupers);
    //  retval.newLoneSupers = remainingSupers;  
    //}
      
    //
    // See if we have a situation where we need to find out if
    // the user needs to tell us the deletion mode:
    //

    
    retval.needDeletionType = false;
    if (dacx.getCurrentGenomeAsInstance().getVfgParent() != null) {
      InstanceInstructionSet oldIis = dacx.getInstructSrc().getInstanceInstructionSet(dacx.getCurrentGenomeID());
      if (regionLost(oldIis, newIis, newList)) {
        retval.needDeletionType = true;
      }
    }
    
    //
    // Sort out what changes go where:
    //
    
    List<InstanceToProcess> procList = buildProcessingList(false, dacx);
    int pSize = procList.size();
    for (int i = 0; i < pSize; i++) {
      InstanceToProcess itp = procList.get(i);
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(itp.id);
      if ((itp.type != InstanceToProcess.SELF) && changeIsImportant(iis, changes)) {
        switch (itp.type) {
          case InstanceToProcess.PARENT:
            retval.changedParentInstances.add(itp.id);
            break;
          case InstanceToProcess.CHILD:
            retval.changedChildInstances.add(itp.id);
            break;
          case InstanceToProcess.COUSIN_OR_SIBLING:
            retval.changedSiblingCousinInstances.add(itp.id);
            break;
          case InstanceToProcess.SELF:
          default:
            throw new IllegalStateException();
        }
      }
    }
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Compare instructions and look for changes
  */
  
  private Changes compareInstructions(List<BuildInstruction> oldList, List<BuildInstruction> newList) {
    //
    // Look for additions, deletions, and edits:
    //
    
    HashMap<String, BuildInstruction> oldInstr = new HashMap<String, BuildInstruction>();
    int oSize = oldList.size();
    for (int i = 0; i < oSize; i++) {
      BuildInstruction bi = oldList.get(i);
      oldInstr.put(bi.getID(), bi);
    }
    Set<String> oldIDs = oldInstr.keySet();

    HashMap<String, BuildInstruction> newInstr = new HashMap<String, BuildInstruction>();
    int nSize = newList.size();
    for (int i = 0; i < nSize; i++) {
      BuildInstruction bi = newList.get(i);
      newInstr.put(bi.getID(), bi);
    }
    Set<String> newIDs = newInstr.keySet();    
    
    Changes retval = new Changes();
    
    retval.deletions.addAll(oldIDs);
    retval.deletions.removeAll(newIDs);

    retval.additions.addAll(newIDs);
    retval.additions.removeAll(oldIDs);    
        
    HashSet<String> sharedIDs = new HashSet<String>(oldIDs);
    sharedIDs.retainAll(newIDs);
    
    Iterator<String> siit = sharedIDs.iterator();
    while (siit.hasNext()) {
      String key = siit.next();
      BuildInstruction obi = oldInstr.get(key);
      BuildInstruction nbi = newInstr.get(key);      
      if (!obi.equals(nbi)) {
        retval.edits.add(key);
      }
    }

    retval.haveDuplicates = false;
    Set<String> newDups = getDuplicates(newList);
    if (!newDups.isEmpty()) {
      Set<String> oldDups = getDuplicates(oldList);
      newDups.removeAll(oldDups);
      if (!newDups.isEmpty()) {
        retval.haveDuplicates = true;
      }
    }

    //
    // We have the ability to detect superfluous nodes, 
    // but we cannot make the decision based on local
    // analysis.  E.g. deleting submodels may then make
    // lone nodes superfluous at the root level.  Skip
    // this analysis.
    //
    //retval.newLoneSupers = getSuperfluousLoneNodes(newList);
    // if (!retval.newLoneSupers.isEmpty()) {
    //  retval.oldLoneSupers = getSuperfluousLoneNodes(oldList);
    //}
    
    retval.haveLoneDuplicates = false;
    Set<String> newLoneDups = getDuplicatedLoneNodes(newList);
    if (!newLoneDups.isEmpty()) {
      Set<String> oldLoneDups = getDuplicatedLoneNodes(oldList);
      newLoneDups.removeAll(oldLoneDups);
      if (!newLoneDups.isEmpty()) {
        retval.haveLoneDuplicates = true;
      }
    }    
    
    return (retval);
  }

  /***************************************************************************
  **
  ** look for duplicates.  That are NOT lone nodes.
  */
  
  private Set<String> getDuplicates(List<BuildInstruction> biList) {

    int biSize = biList.size();
    
    //
    // Look for new duplicate instructions:
    //
    
    HashSet<String> retval = new HashSet<String>();
    for (int i = 0; i < biSize; i++) {
      BuildInstruction bi1 = biList.get(i);
      if (bi1 instanceof LoneNodeBuildInstruction) {
        continue;
      }
      for (int j = 0; j < biSize; j++) {
        if (j == i) {
          continue;
        }
        BuildInstruction bi2 = biList.get(j);
        if (bi2 instanceof LoneNodeBuildInstruction) {
          continue;
        }
        if (bi1.sameDefinition(bi2)) {
          retval.add(bi1.getID());
          retval.add(bi2.getID());
        }
      }
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** look for lone nodes that are not needed.  Currently unused.  We let these pass.
  */
  
  @SuppressWarnings("unused")
  private Set<String> getSuperfluousLoneNodes(List<BuildInstruction> biList) {

    int biSize = biList.size();
 
    HashSet<String> loneNodes = new HashSet<String>();
    HashSet<String> otherNodes = new HashSet<String>();
    for (int i = 0; i < biSize; i++) {
      BuildInstruction bi = biList.get(i);
      if (bi instanceof LoneNodeBuildInstruction) {
        loneNodes.addAll(DataUtil.normalizeList(new ArrayList<String>(bi.getNamedNodes())));
      } else {
        otherNodes.addAll(DataUtil.normalizeList(new ArrayList<String>(bi.getNamedNodes())));
      }
    }
    HashSet<String> retval = new HashSet<String>(loneNodes);
    retval.retainAll(otherNodes);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** A lone node may not be superfluous if it is responsible for placing a
  ** lone node into a region by itself!
  ** Currently unused.  We let these pass.  If we told the user, the global
  ** nature of determining superfluous lone nodes could lead to unintended deletions
  ** at the root level.
  */
  
  @SuppressWarnings("unused")
  private Set<String> getSuperfluousLoneNodesWithRegionContext(InstanceInstructionSet iis, Set<String> loneCands, List<BuildInstruction> buildInst) {

    if (iis == null) {
      return (new HashSet<String>());
    }
    
    //
    // Get a map of regions to nodes for non-lone instructions.  Go through 
    // the lone nodes, and find out if they target any region not in the map.
    // 
    
    // map ID->instruction
    HashMap<String, BuildInstruction> biMap = new HashMap<String, BuildInstruction>();      
    Iterator<BuildInstruction> biit = buildInst.iterator();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      String bid = bi.getID();
      biMap.put(bid, bi);
    }
    
    HashMap<String, Set<String>> regionMap = new HashMap<String, Set<String>>();
    HashMap<String, Set<String>> loneRegionMap = new HashMap<String, Set<String>>();    
    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      String biid = bii.getBaseID();
      BuildInstruction bi = biMap.get(biid);
      HashMap<String, Set<String>> useMap = (bi instanceof LoneNodeBuildInstruction) ? loneRegionMap : regionMap;
      List<String> srcRegionNodes = DataUtil.normalizeList(new ArrayList<String>(bi.getNamedSourceRegionNodes()));
      String srcReg = bii.getSourceRegionID();
      Set<String> nodesPerReg = useMap.get(srcReg);
      if (nodesPerReg == null) {
        nodesPerReg = new HashSet<String>();
        useMap.put(srcReg, nodesPerReg);
      }
      nodesPerReg.addAll(srcRegionNodes);
      
      List<String> trgRegionNodes = DataUtil.normalizeList(new ArrayList<String>(bi.getNamedTargetRegionNodes()));
      String trgReg = bii.getTargetRegionID();
      if (trgReg == null) {
        continue;
      }
      nodesPerReg = useMap.get(trgReg);
      if (nodesPerReg == null) {
        nodesPerReg = new HashSet<String>();
        useMap.put(trgReg, nodesPerReg);
      }
      nodesPerReg.addAll(trgRegionNodes);
    }
      
    //
    // For every region holding lone nodes, see if the set of lone nodes has entries not present in
    // the set of lone nodes.  If so, we can remove those nodes from the set of candidates.  The set
    // at the end are the useless lone nodes.
    //
    
    HashSet<String> retval = new HashSet<String>(loneCands);
    Iterator<String> lrmkit = loneRegionMap.keySet().iterator();
    while (lrmkit.hasNext()) {
      String regID = lrmkit.next();
      Set<String> lones = loneRegionMap.get(regID); 
      Set<String> nonLones = regionMap.get(regID);      
      if (nonLones == null) {
        retval.removeAll(lones);
        continue;
      }
      HashSet<String> diffs = new HashSet<String>(lones);
      diffs.removeAll(nonLones);
      retval.removeAll(diffs);
    }
    return (retval);
  }   
  
   
  /***************************************************************************
  **
  ** look for lone nodes that are not duplicated:
  */
  
  private Set<String> getDuplicatedLoneNodes(List<BuildInstruction> biList) {
    int biSize = biList.size();
    
    //
    // Look for new duplicate instructions:
    //
    
    HashSet<String> retval = new HashSet<String>();
    for (int i = 0; i < biSize; i++) {
      BuildInstruction bi1 = biList.get(i);
      if (!(bi1 instanceof LoneNodeBuildInstruction)) {
        continue;
      }
      for (int j = 0; j < biSize; j++) {
        if (j == i) {
          continue;
        }
        BuildInstruction bi2 = biList.get(j);
        if (!(bi1 instanceof LoneNodeBuildInstruction)) {
          continue;
        }
        if (bi1.sameDefinition(bi2)) {
          retval.add(bi1.getID());
          retval.add(bi2.getID());
        }
      }
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find out if changes make a difference
  */
  
  private boolean changeIsImportant(InstanceInstructionSet iis, Changes changes) {
    if (iis == null) { // FIX ME! Too general.
      return (true);
    }
    
    Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
    while (iit.hasNext()) {
      BuildInstructionInstance bii = iit.next();
      String biid = bii.getBaseID();
      if (changes.deletions.contains(biid)) {
        return (true);
      }
      if (changes.edits.contains(biid)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Find out if we are losing a tuple from an instruction
  */
  
  private boolean regionLost(InstanceInstructionSet oldIis, 
                             InstanceInstructionSet newIis, List<BuildInstruction> newInst) {
    if (oldIis == null) {
      return (false);
    }
    
    Iterator<BuildInstructionInstance> oiit = oldIis.getInstructionIterator();
    while (oiit.hasNext()) {
      BuildInstructionInstance obii = oiit.next();
      boolean stillAround = false;
      Iterator<BuildInstructionInstance> niit = newIis.getInstructionIterator();
      while (niit.hasNext()) {
        BuildInstructionInstance nbii = niit.next();
        if (nbii.equals(obii)) {
          stillAround = true;
          break;
        }
      }
      if (!stillAround) {
        String id = obii.getBaseID();
        int nSize = newInst.size();
        for (int i = 0; i < nSize; i++) {
          BuildInstruction bi = newInst.get(i);
          String bid = bi.getID();
          if ((bid != null) && bid.equals(id)) {
            return (true);
          }
        }
      }
    }
    return (false);
  }  
  

  /***************************************************************************
  **
  ** Build an ordered list, so parents come before children
  */
  
  private List<InstanceToProcess> buildProcessingList(boolean onlySelfAndKids, StaticDataAccessContext dacx) {
    ArrayList<InstanceToProcess> retval = new ArrayList<InstanceToProcess>();

    Iterator<GenomeInstance> giit = dacx.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance currGi = giit.next();
      buildProcessingListCore(dacx.getCurrentGenomeAsInstance(), currGi, retval, onlySelfAndKids);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build an ordered list, so parents come before children
  */
  
  private void buildProcessingListCore(GenomeInstance orig, GenomeInstance gi, 
                                       List<InstanceToProcess> theList, boolean onlySelfAndKids) {
    String giID = gi.getID();
    int size = theList.size();
    for (int i = 0; i < size; i++) {
      InstanceToProcess itp = theList.get(i);
      if (itp.id.equals(giID)) {
        return;
      }
    }
    GenomeInstance parent = gi.getVfgParent();
    if (parent != null) {
      buildProcessingListCore(orig, parent, theList, onlySelfAndKids);
    }
    if (giID.equals(orig.getID())) {
      theList.add(new InstanceToProcess(giID, InstanceToProcess.SELF));
    } else if (gi.isAncestor(orig) && !onlySelfAndKids) {
      theList.add(new InstanceToProcess(giID, InstanceToProcess.PARENT));
    } else if (orig.isAncestor(gi)) {
      theList.add(new InstanceToProcess(giID, InstanceToProcess.CHILD));
    } else if (!onlySelfAndKids) {
      theList.add(new InstanceToProcess(giID, InstanceToProcess.COUSIN_OR_SIBLING));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Any models with subgroups that are driven by instructions, or models
  ** that are decendants of these models (including dynamic submodels), need 
  ** to have subgroups killed off before applying instructions.
  */
  
  private void killDeadSubGroups(StaticDataAccessContext rcxC, UndoSupport support) {
    Set<String> models = getInstructionDependentRootInstances(rcxC);
    //
    // Handle case where we are newly installing instructions in a model:
    //
    
   // if (rcxC.getGenome() != null) { // <- Based on direct read of legacy code, but should never happen!
      
    GenomeInstance rootGI = rcxC.getCurrentGenomeAsInstance().getVfgParentRoot();
    if (rootGI == null) {
      models.add(rcxC.getCurrentGenomeID());
    } else {
      models.add(rootGI.getID());
    }  
    
    Iterator<String> mit = models.iterator();
    while (mit.hasNext()) {
      String id = mit.next();
      GenomeInstance gi = (GenomeInstance)rcxC.getGenomeSource().getGenome(id);
      StaticDataAccessContext rcxS = new StaticDataAccessContext(rcxC, gi);
      Set<String> subs = gi.getAllSubsets();
      Iterator<String> sit = subs.iterator();
      while (sit.hasNext()) {
        String subID = sit.next();
        RemoveGroupSupport.deleteSubGroupFromModel(uics_, subID, rcxS, tSrc_, support, uFac_);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** KIll off all subgroups
  ** Note that UndoManager argument is probably superfluous given non-null support
  */
  
  private void killAllSubGroups(StaticDataAccessContext rcxC, UndoSupport support) {

    HashSet<String> models = new HashSet<String>();
    
    //
    // Get models 
    //
    
    Iterator<GenomeInstance> iit = rcxC.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (gi.getVfgParent() != null) {
        continue;
      }
      models.add(gi.getID());
    }
    
    Iterator<String> mit = models.iterator();
    while (mit.hasNext()) {
      String id = mit.next();
      GenomeInstance gi = (GenomeInstance)rcxC.getGenomeSource().getGenome(id);
      StaticDataAccessContext rcxS = new StaticDataAccessContext(rcxC, gi);
      Set<String> subs = gi.getAllSubsets();
      Iterator<String> sit = subs.iterator();
      while (sit.hasNext()) {
        String subID = sit.next();
        RemoveGroupSupport.deleteSubGroupFromModel(uics_, subID, rcxS, tSrc_, support, uFac_);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the set of model IDs for those models that are driven by instructions
  ** or which inherit from models that are driven by instructions.
  */
  
  private Set<String> getInstructionDependentRootInstances(StaticDataAccessContext dacx) {
                                                    
    HashSet<String> models = new HashSet<String>();
    
    //
    // Get models using instructions.  Can also handle the root-driven case
    // here (everybody depends).
    //
    
    Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (gi.getVfgParent() != null) {
        continue;
      }
      String giid = gi.getID();
      if (dacx.getInstructSrc().getInstanceInstructionSet(giid) != null) {
        models.add(giid);
      }
    }
    
    return (models);
  }

  /***************************************************************************
  **
  ** Get the set of model IDs for those models that are driven by instructions
  ** or which inherit from models that are driven by instructions.
  */
  
  @SuppressWarnings("unused")
  private void getInstructionDependentModelsAndProxies(Set<String> models, Set<String> proxies, StaticDataAccessContext dacx) {
                                                    
    
    //
    // Get models using instructions.  Can also handle the root-driven case
    // here (everybody depends).
    //
    
    boolean rootUses = dacx.getInstructSrc().haveBuildInstructions();
    HashSet<String> pending = new HashSet<String>();
    HashSet<String> done = new HashSet<String>();
    
    Iterator<GenomeInstance> iit = dacx.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      String giid = gi.getID();
      if (rootUses || (dacx.getInstructSrc().getInstanceInstructionSet(giid) != null)) {
        models.add(giid);
        done.add(giid);
      } else if (gi.getVfgParent() == null) {
        done.add(giid);
      } else {
        pending.add(giid);
      }
    }
    
    //
    // Now find models that are children of models with instructions.
    //
    
    while (pending.size() > 0) {
      Iterator<String> pit = pending.iterator();
      while (pit.hasNext()) {
        String pendID = pit.next();
        GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(pendID);
        GenomeInstance parent = gi.getVfgParent();
        if (parent == null) {
          continue;
        }
        String pID = parent.getID();
        if (done.contains(pID)) {
          if (models.contains(pID)) {
            models.add(pendID);
          }
          pending.remove(pendID);
          done.add(pendID);
          break;
        }
      }  
    }

    Iterator<DynamicInstanceProxy> dpit = dacx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      GenomeInstance gi = dip.getStaticVfgParent();
      if (models.contains(gi.getID())) {
        proxies.add(dip.getID());
      }
    }
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Data we need
  */ 
    
  public static class PIData {
  
    String genomeID;
    List<BuildInstruction> buildCmds; 
    Point2D center; 
    Dimension size;
    boolean keepLayout; 
    LayoutOptions options;
    boolean hideNames; 
    UndoSupport support; 
    BTProgressMonitor monitor;
    SpecialtyLayout specLayout;
    SpecialtyLayoutEngineParams params;
    List<InstanceInstructionSet.RegionInfo> regions;
    boolean globalDelete;
    
    
    
    public PIData(String genomeID,
                  List<BuildInstruction> buildCmds, 
                  List<InstanceInstructionSet.RegionInfo> regions, Point2D center, 
                  Dimension size,
                  boolean keepLayout, boolean hideNames, LayoutOptions options, 
                  UndoSupport support, BTProgressMonitor monitor,
                  boolean globalDelete, SpecialtyLayout specLayout,
                  SpecialtyLayoutEngineParams params) {
      
      this.genomeID = genomeID;
      this.buildCmds = buildCmds;
      this.center = center;
      this.size = size;
      this.keepLayout = keepLayout;
      this.options = options;
      this.hideNames = hideNames;
      this.support =  support;
      this.monitor = monitor;
      this.specLayout = specLayout;
      this.params = params;
      this.regions = regions;
      this.globalDelete = globalDelete;
    }
  }
  
  /***************************************************************************
  **
  ** Data we need
  */ 
    
  public static class PISIFData {
  
    List<BuildInstruction> buildCmds; 
    Point2D center; 
    Dimension size;
    boolean keepLayout; 
    LayoutOptions options;
    boolean hideNames; 
    UndoSupport support; 
    BTProgressMonitor monitor;
    double startFrac; 
    double maxFrac; 
    SpecialtyLayout specLayout;
    SpecialtyLayoutEngineParams params;
    ArrayList<DialogBuiltMotifPair> createdPairs;
    HashMap<String, String> newNodeToOldNode;
    HashMap<String, String> newLinksToOldLinks;
    FullGenomeHierarchyOracle fgho;
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds;
    Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> emptyModNeeds;
    Map<String, Layout.OverlayKeySet> allKeys;    
    Map<String, Layout.SupplementalDataCoords> sdcCache;

    public PISIFData(UIComponentSource uics,
                     TabSource tSrc,
                     StaticDataAccessContext dacx,
                     UndoFactory uFac,
                     List<BuildInstruction> buildCmds, 
                     Point2D center, 
                     Dimension size,  
                     boolean keepLayout, boolean hideNames, LayoutOptions options, 
                     UndoSupport support, BTProgressMonitor monitor,
                     double startFrac, double maxFrac, SpecialtyLayout specLayout,
                     SpecialtyLayoutEngineParams params) { 
      
      createdPairs = new ArrayList<DialogBuiltMotifPair>();
      newNodeToOldNode = new HashMap<String, String>();
      newLinksToOldLinks = new HashMap<String, String>();
      
      this.buildCmds = buildCmds;
      this.center = center;
      this.size = size;
      this.keepLayout = keepLayout;
      this.options = options;
      this.hideNames = hideNames;
      this.support =  support;
      this.monitor = monitor;
      this.startFrac = startFrac;
      this.maxFrac =  maxFrac;
      this.specLayout = specLayout;
      this.params = params;
      
      //
      // Figure out all module pad needs before we get started:
      //

      globalPadNeeds = dacx.getFGHO().getGlobalNetModuleLinkPadNeeds();
      emptyModNeeds = dacx.getFGHO().stockUpMemberOnlyModules();
      allKeys = dacx.getFGHO().fullModuleKeysPerLayout();    
      sdcCache = (new BuildSupport(uics, tSrc, uFac)).buildSdcCache(allKeys, dacx);
      
    }
  }
 
  /***************************************************************************
  **
  ** Data we need
  */ 
    
  public static class PIHData {
  
    List<String> processOrder; 
    Map<String, List<BuildInstruction>> buildCmds; 
    Map<String, List<InstanceInstructionSet.RegionInfo>> regions;
    Point2D center; 
    Dimension size;
    boolean keepLayout; 
    LayoutOptions options;
    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap; 
    String dbGenomeCSVName; 
    Map<String, String> newNodeToOldNode;
    boolean hideMinorNames; 
    UndoSupport support; 
    BTProgressMonitor monitor;
    double startFrac; 
    double maxFrac; 
    SpecialtyLayout specLayout;
    SpecialtyLayoutEngineParams params;
  
     public PIHData(List<String> processOrder, Map<String, List<BuildInstruction>> buildCmds, 
                    Map<String, List<InstanceInstructionSet.RegionInfo>> regions,
                    Point2D center, Dimension size,
                    boolean keepLayout, LayoutOptions options,
                    Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap, 
                    String dbGenomeCSVName, Map<String, String> newNodeToOldNode,
                    boolean hideMinorNames, UndoSupport support, BTProgressMonitor monitor,
                    double startFrac, double maxFrac, SpecialtyLayout specLayout,
                    SpecialtyLayoutEngineParams params) {
       
      this.processOrder = processOrder;
      this.buildCmds =  buildCmds;
      this.regions = regions;
      this.center =  center;
      this.size = size;
      this.keepLayout =  keepLayout;
      this.options = options;
      this.nodeIDMap =  nodeIDMap;
      this.dbGenomeCSVName =  dbGenomeCSVName;
      this.newNodeToOldNode = newNodeToOldNode;
      this.hideMinorNames =  hideMinorNames;
      this.support =  support;
      this.monitor = monitor;
      this.startFrac =  startFrac;
      this.maxFrac =  maxFrac;
      this.specLayout = specLayout;
      this.params = params; 
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to hold pending instructions
  */ 
    
  private static class InstructionHolder {

    BuildInstruction coreInstruct;
    InstructionRegions unassigned;
    HashMap<String, InstructionRegions> assignedRegions;
    HashMap<String, Integer> assignedEmpty;
    int emptyUnassigned;
    
    //
    // Whether region is assigned depends on if the instruction has an ID
    //
    
    InstructionHolder(BuildInstruction instruct) {
      assignedRegions = new HashMap<String, InstructionRegions>();
      assignedEmpty = new HashMap<String, Integer>();
      String instrID = instruct.getID();
      InstructionRegions iReg = new InstructionRegions(instruct.getRegions());
      if (instrID.equals("")) {
        unassigned = iReg;
        emptyUnassigned = (iReg.getNumTuples() == 0) ? 1 : 0;    
      } else {
        unassigned = new InstructionRegions();
        emptyUnassigned = 0;
        if (iReg.getNumTuples() > 0) {
          assignedRegions.put(instrID, iReg);
        } else {
          assignedEmpty.put(instrID, new Integer(1));
        }
      }
      coreInstruct = instruct.clone();
      coreInstruct.setID(null);
      coreInstruct.setRegions(null);
    }
    
    @Override
    public String toString() {
      StringBuffer retval = new StringBuffer();
      retval.append("InstructionHolder: instruction = ");
      retval.append(coreInstruct);
      retval.append("\nunassigned: \n");
      retval.append(unassigned);
      retval.append("\n");
      Iterator<String> mit = assignedRegions.keySet().iterator();
      while (mit.hasNext()) {
        String key = mit.next();
        retval.append("unempty instrID: ");
        retval.append(key);
        retval.append(" : ");
        retval.append(assignedRegions.get(key));
        retval.append("\n");          
      }
      Iterator<String> eit = assignedEmpty.keySet().iterator();
      while (eit.hasNext()) {
        String key = eit.next();
        retval.append("empty instrID: ");
        retval.append(key);
        retval.append(" : ");
        retval.append(assignedEmpty.get(key));
        retval.append("\n");          
      }      
      retval.append("\nempty unassigned: \n");
      retval.append(emptyUnassigned);
      retval.append("\n");  
      return (retval.toString());
    }      
  }
  
  /***************************************************************************
  **
  ** Used to hold differences between instruction sets
  */ 
    
  private static class Changes {

    HashSet<String> additions;
    HashSet<String> deletions;
    HashSet<String> edits;
    boolean haveDuplicates;
    boolean haveLoneDuplicates;
    //Set newLoneSupers;
    //Set oldLoneSupers;    
    
    Changes() {
      additions = new HashSet<String>();
      deletions = new HashSet<String>();
      edits = new HashSet<String>();
      haveDuplicates = false;
      haveLoneDuplicates = false;
      //newLoneSupers = new HashSet();
      //oldLoneSupers = new HashSet();      
    }
  }
  
  /***************************************************************************
  **
  ** Used to hold region differences between instruction sets
  */ 
    
  private static class ChangesWithRegions {

    HashSet<InstanceInstructionSet.RegionInfo> addedRegions;
    HashSet<InstanceInstructionSet.RegionInfo> deletedRegions;
    HashSet<BuildInstructionInstance> addedInstruct;
    HashSet<BuildInstructionInstance> deletedInstruct;    
    
    ChangesWithRegions() {
      addedRegions = new HashSet<InstanceInstructionSet.RegionInfo>();
      deletedRegions = new HashSet<InstanceInstructionSet.RegionInfo>();
      addedInstruct = new HashSet<BuildInstructionInstance>();
      deletedInstruct = new HashSet<BuildInstructionInstance>();    
    }
  }
}