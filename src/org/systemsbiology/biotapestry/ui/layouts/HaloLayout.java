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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.Vector;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** Bipartite Specialty Layout 
*/

public class HaloLayout implements SpecialtyLayout {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC ENUMS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Start options 
  */ 
   
  public enum StartSeed {
     
    SELECTED_START("selectedStart"),
    GREEDY_START("greedyStart"),
    MEDIAN_START("medianStart");
   
    private String tag_;
     
    StartSeed(String tag) {
      this.tag_ = tag;  
    }
     
    public EnumChoiceContent<StartSeed> generateCombo(BTState appState) {
      return (new EnumChoiceContent<StartSeed>(appState.getRMan().getString("haloLayout." + tag_), this));
    }
   
    public static Vector<EnumChoiceContent<StartSeed>> getChoices(BTState appState, boolean haveSelected) {
      Vector<EnumChoiceContent<StartSeed>> retval = new Vector<EnumChoiceContent<StartSeed>>();
      for (StartSeed ss: values()) {
        if ((ss == SELECTED_START) && !haveSelected) {
          continue;
        }
        retval.add(ss.generateCombo(appState));    
      }
      return (retval);
    }
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int NON_GENE_SOURCE_PAD_ = 2;
  private static final int FIRST_ROW_PAD_ = 1;
  private static final int MAX_SRC_STACKING_ = 10;
  private static final double SRC_OFFSET_X_ = 160.0;  
  private static final double OFFSET_X_ = 250.0;  
  private static final double TRACE_OFFSET_X_ = 50.0;
  private static final double PER_TRACE_ = 30.0;
  private static final int MIN_TRACE_DIFF_ = (int)(90.0 / PER_TRACE_);
  private static final double VERTICAL_CLUSTER_PAD_ = 40.0;
   
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
   
  private BTState appState_;
  private SpecialtyLayoutData sld_;
  
  //
  // Keeps track of which source trace is used in each cluster run.
  //
    
  private HashMap<String, Map<Integer, Integer>> geneSrcToTraceNumberPerClusterRun_;
  private HashMap<String, Map<Integer, Integer>> nonGeneSrcToTraceNumberPerClusterRun_;
  
  private HashMap<String, List<GeneAndSatelliteCluster>> geneSrcToClusters_;
  private HashMap<String, List<GeneAndSatelliteCluster>> nonGeneSrcToClusters_;
  
  private HashMap<Integer, Point2D> clusterRunStarts_;
  private HashMap<Integer, Map<Integer, Integer>> clusterRunSpecialPads_;
  private HashMap<String, int[]> clusterToClusterRunNumber_;
  private HashMap<String, Integer> sourceToTrace_;
  private Point2D traceBoundary_;
  
  private List<GeneAndSatelliteCluster> termClusters_; 
  private Set<String> geneSources_;
  private Set<String> nonGeneSources_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HaloLayout(BTState appState) {
    appState_ = appState;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Fork for a subset
  */
  
  public SpecialtyLayout forkForSubset(SpecialtyLayoutData sld) {
    HaloLayout retval = new HaloLayout(appState_);
    retval.sld_ = sld;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answer if the setup works OK.  Error message if a problem, else null if OK
  */
  
  public String setUpIsOK() {
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the menu name for the layout
  */
  
  public String getMenuNameTag() {
    return ("command.HaloLayout");
  }  
     
  /***************************************************************************
  **
  ** Get the mnemonic string for the layout
  */
  
  public String getMenuMnemonicTag() {
    return ("command.HaloLayoutMnem");
  }
  
  /***************************************************************************
  **
  ** Answer if the given selection is valid
  */
  
  public boolean selectionIsValid(Genome genome, String selected) {
    Set<String> geneSources = findGeneSources(genome);
    return (geneSources.contains(selected));
  } 
  
  /***************************************************************************
  **
  ** Answer if the network topology can be handled by this layout
  */
  
  public int topologyIsHandled(DataAccessContext rcx) {
    
    Genome genome = rcx.getGenome();
    GenomeSubset subset = new GenomeSubset(appState_, genome.getID(), new Point2D.Double());  
    List<GeneAndSatelliteCluster> termClusters = GeneAndSatelliteCluster.findTerminalTargets(appState_, subset, false, UiUtil.GRID_SIZE, false); // last two args do not matter here...
   
    Map<String, Point2D> fakeNolo = new HashMap<String, Point2D>();
    InvertedSrcTrg ist = new InvertedSrcTrg(genome);
    Map<String, PadCalculatorToo.PadResult> fakePC= new HashMap<String, PadCalculatorToo.PadResult>();
    SpecialtyLayoutEngine.NodePlaceSupport nps = new SpecialtyLayoutEngine.NodePlaceSupport(genome, rcx.getLayout(), fakeNolo, fakePC, ist);
    
    GeneAndSatelliteCluster.fillTargetClusters(termClusters, nps, rcx);
    
    Set<String> gs = findGeneSources(genome);
    Set<String> ngs = findNonGeneSources(genome);    
    
    HashSet<String> handledNodes = new HashSet<String>();
    int numClust = termClusters.size();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster tc = termClusters.get(i);
      if (!tc.okForHalo(nps)) {
        return (TOPOLOGY_NOT_HANDLED_STOP);
      }
      handledNodes.addAll(tc.allNodesInCluster());
    }
    
    handledNodes.addAll(gs);
    handledNodes.addAll(ngs);
    
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (!handledNodes.contains(node.getID())) {
        return (TOPOLOGY_NOT_HANDLED_OK);
      }
    }
    return (TOPOLOGY_HANDLED);
  }   

  /***************************************************************************
  **
  ** Figure out the node positions
  */
  
  public void layoutNodes(BTProgressMonitor monitor) throws AsynchExitRequestException {

    // "KEEP COLORS" because we will be providing a pre-filled map to use:
    sld_.results.colorStrategy = ColorTypes.KEEP_COLORS;
    HaloLayoutParams hlParams = (HaloLayoutParams)sld_.param;
    Genome baseGenome = sld_.genome;
     
    double traceOffset = (double)hlParams.traceMult * UiUtil.GRID_SIZE;
    
    //
    // Get the listing of terminal targets, gene sources, and non-gene sources.
    //
    // NOTE: The cluster map will include singleton nodes!
    //
 
    termClusters_ = GeneAndSatelliteCluster.findTerminalTargets(sld_.appState, sld_.subset, false, traceOffset, hlParams.textToo);
    GeneAndSatelliteCluster.fillTargetClusters(termClusters_, sld_.nps, sld_.rcx);
    HashMap<String, GeneAndSatelliteCluster> clusterMap = new HashMap<String, GeneAndSatelliteCluster>();
    int numClust = termClusters_.size();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster tc = termClusters_.get(i);
      clusterMap.put(tc.getCoreID(), tc);
    }
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
   
    geneSources_ = findGeneSources(baseGenome);

    nonGeneSources_ = findNonGeneSources(baseGenome);
    Iterator<String> ngsit = nonGeneSources_.iterator();
    while (ngsit.hasNext()) {
      String nonGene = ngsit.next();
      calcNonGeneSrcPadChanges(nonGene, baseGenome, sld_.results.padChanges);
    }
    HashMap<String, Integer> nonGeneSizeMap = new HashMap<String, Integer>();
    
    DataAccessContext bgc = new DataAccessContext(sld_.rcx, baseGenome, sld_.rcx.getLayout());
    nonGeneSizes(nonGeneSources_, bgc, nonGeneSizeMap);
    
    double traceWidth = geneSources_.size() * TRACE_OFFSET_X_;
    Iterator<Integer> smit = nonGeneSizeMap.values().iterator();
    while (smit.hasNext()) {
      Integer extra = smit.next();
      traceWidth += ((2.0 * extra.doubleValue()) * TRACE_OFFSET_X_);       
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    //
    // Figure out which gene source goes first: the one with the most target
    // clusters.  Also fill out maps of source to clusters.
    //
    
    geneSrcToClusters_ = new HashMap<String, List<GeneAndSatelliteCluster>>();
    nonGeneSrcToClusters_ = new HashMap<String, List<GeneAndSatelliteCluster>>();
    HashSet<String> singletonClusters = new HashSet<String>();
    List<TaggedByCount> rankedSources = rankAllSourcesByTargetClusters(geneSources_, nonGeneSources_,
                                                                       termClusters_, geneSrcToClusters_, 
                                                                       nonGeneSrcToClusters_, singletonClusters);
    HashMap<String, List<GeneAndSatelliteCluster>> allSrcToClusters = new HashMap<String, List<GeneAndSatelliteCluster>>(geneSrcToClusters_);
    allSrcToClusters.putAll(nonGeneSrcToClusters_);
    if (allSrcToClusters.isEmpty()) {
      // NO!  Handle pure singleton network too!
      sld_.results = null;
      return;
    }
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    int rsNum = rankedSources.size();
    ArrayList<String> remainingSrcs = new ArrayList<String>();
    for (int i = 0; i < rsNum; i++) {
      TaggedByCount tbc = rankedSources.get(i);
      remainingSrcs.add(tbc.id);
    }
    
    String startSrc;
    switch (hlParams.startType) {
      case GREEDY_START:
        startSrc = remainingSrcs.get(remainingSrcs.size() - 1);
        break;
      case MEDIAN_START:
        startSrc = remainingSrcs.get(remainingSrcs.size() / 2);
        break;
      case SELECTED_START:
        if ((hlParams.selected != null) && remainingSrcs.contains(hlParams.selected)) {
          startSrc = hlParams.selected;
        } else {  // fall back to greedy
          startSrc = remainingSrcs.get(remainingSrcs.size() - 1);          
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    remainingSrcs.remove(startSrc);
    
    //
    // Keeps track of what cluster run each cluster is in
    //
    
    clusterToClusterRunNumber_ = new HashMap<String, int[]>();
    
    //
    // Keeps track of which source trace is used in each cluster run.
    //
    
    geneSrcToTraceNumberPerClusterRun_ = new HashMap<String, Map<Integer, Integer>>();
    nonGeneSrcToTraceNumberPerClusterRun_ = new HashMap<String, Map<Integer, Integer>>();
    
    //
    // Keeps track of the start point of each cluster run, and pads needed for some traces:
    //
    
    clusterRunStarts_ = new HashMap<Integer, Point2D>();
    clusterRunSpecialPads_ = new HashMap<Integer, Map<Integer, Integer>>();
    
    //
    // Order the initial run of target clusters
    //
    
    int currClustRun = 0;   
    List<GeneAndSatelliteCluster> currClusters = allSrcToClusters.get(startSrc);
    ArrayList<TaggedByCount> clustCounts = new ArrayList<TaggedByCount>();
    ArrayList<TaggedByCount> sourceCounts = new ArrayList<TaggedByCount>();    
    orderInitialTargetClusters(currClusters, allSrcToClusters, clustCounts, sourceCounts);
    recordClusterRunNumber(clusterToClusterRunNumber_, clustCounts, currClustRun);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    //
    // Assign source leads based on this ordering
    //
    
    List<String> srcLeads = orderInitialSourceLeads(baseGenome, clusterMap, clustCounts, sourceCounts, nonGeneSources_);
    int numCurr = currClusters.size();
    for (int i = 0; i < numCurr; i++) {
      GeneAndSatelliteCluster clust = currClusters.get(i);
      clust.calcPadChanges(sld_.subset, srcLeads, sld_.nps, sld_.results.lengthChanges, sld_.results.extraGrowthChanges);
    }
    recordTraceForClusterRun(geneSrcToTraceNumberPerClusterRun_, 
                             nonGeneSrcToTraceNumberPerClusterRun_, 
                             srcLeads, currClustRun, nonGeneSources_);    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    //
    // Start assigning the main source traces:
    //
    
    sourceToTrace_ = new HashMap<String, Integer>();
    HashMap currentSourceToTrace = new HashMap();
    TreeMap<Integer, String> traceToSource = new TreeMap<Integer, String>();
    Integer firstTrace = new Integer(0);
    sourceToTrace_.put(startSrc, firstTrace);
    traceToSource.put(firstTrace, startSrc);

  
    assignSourceTraces(sourceToTrace_, traceToSource, currentSourceToTrace, srcLeads, nonGeneSizeMap, currClusters, allSrcToClusters);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    
    //
    // Center is not necessarily on the grid.  Force it:
    //
    
    Point2D center = (Point2D)sld_.subset.getPreferredCenter().clone();
    UiUtil.forceToGrid(center, UiUtil.GRID_SIZE);
      
    //
    // Do initial placement:
    //
    
    int runNum = sourcesInboundToRun(currClusters);
    Point2D runStart = new Point2D.Double();
    Point2D traceStart = new Point2D.Double();
    
    HashSet<String> srcs = new HashSet<String>();
    for (int i = 0; i < sourceCounts.size(); i++) {
      String srcID = sourceCounts.get(i).id;
      if (geneSources_.contains(srcID)) {
        srcs.add(srcID);
      }
    }
    HashMap<Integer, Integer> specialTracePads = new HashMap<Integer, Integer>();
    clusterRunSpecialPads_.put(new Integer(0), specialTracePads);
    HashMap<String, Integer> srcLocs = new HashMap<String, Integer>();
    tracePaddingFromSources(srcs, 0, geneSrcToTraceNumberPerClusterRun_, specialTracePads, srcLocs);   
    
    placeTargetClusters(center, clustCounts, 0, clusterMap, clusterRunSpecialPads_, runNum, baseGenome, sld_.lo, sld_.results.nodeLocations, runStart, traceStart);
    clusterRunStarts_.put(new Integer(0), traceStart);
    traceBoundary_ = (Point2D)traceStart.clone();

    DataAccessContext bgc4 = new DataAccessContext(sld_.rcx, baseGenome, sld_.rcx.getLayout());
    placeSources(srcs, 0, runStart, traceWidth,
                 geneSrcToTraceNumberPerClusterRun_, clusterRunStarts_, 
                 srcLocs, clusterRunSpecialPads_, bgc4, sld_.results.nodeLocations);
    
    placeNonGeneSources(center, traceBoundary_, nonGeneSrcToTraceNumberPerClusterRun_, sourceToTrace_, sld_.results.nodeLocations);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }    

    Map remainingClusters = tossPlacedClusters(currClusters, allSrcToClusters);    
    int currTrace = 1; // 0 is placed
    currClustRun++;
    
    //
    // Loop until we are done placing all the target clusters:
    //
 
    while (!remainingClusters.isEmpty()) {
      Integer maxTrace = (Integer)traceToSource.lastKey();
      String currSource = null;
      if (maxTrace.intValue() >= currTrace) {
        currSource = (String)traceToSource.get(new Integer(currTrace));
      } else {
        switch (hlParams.startType) {
          case GREEDY_START:
            currSource = remainingSrcs.get(remainingSrcs.size() - 1);
            break;
          case MEDIAN_START:
          case SELECTED_START:
            currSource = remainingSrcs.get(remainingSrcs.size() / 2);
            break;
          default:
            throw new IllegalArgumentException();
        }
      }
      remainingSrcs.remove(currSource);  
      List nextClusters = (List)remainingClusters.get(currSource);
      if ((nextClusters != null) && !nextClusters.isEmpty()) {
        srcLeads = orderNewSources(nextClusters, remainingClusters, sourceToTrace_);
        srcs.clear();
        srcs.addAll(srcLeads);
        srcs.removeAll(nonGeneSources_);  // _only_ gene sources 
        assignSourceTraces(sourceToTrace_, traceToSource, currentSourceToTrace, srcLeads, nonGeneSizeMap, nextClusters, remainingClusters);
        clustCounts = new ArrayList();
        sourceCounts = new ArrayList();        
        orderFollowingTargetClusters(nextClusters, remainingClusters, currentSourceToTrace, clustCounts);
        recordClusterRunNumber(clusterToClusterRunNumber_, clustCounts, currClustRun);
        srcLeads = orderFollowingSourceLeads(baseGenome, clusterMap, clustCounts, nonGeneSources_);
        int numNext = nextClusters.size();
        for (int i = 0; i < numNext; i++) {
          GeneAndSatelliteCluster clust = (GeneAndSatelliteCluster)nextClusters.get(i);
          clust.calcPadChanges(sld_.subset, srcLeads, sld_.nps, sld_.results.lengthChanges, sld_.results.extraGrowthChanges);
        }
        recordTraceForClusterRun(geneSrcToTraceNumberPerClusterRun_, 
                                 nonGeneSrcToTraceNumberPerClusterRun_,
                                 srcLeads, currClustRun, nonGeneSources_);
        runNum = sourcesInboundToRun(nextClusters);
        
        specialTracePads = new HashMap();
        Integer ccRunObj = new Integer(currClustRun);
        clusterRunSpecialPads_.put(ccRunObj, specialTracePads);
        srcLocs.clear();
        tracePaddingFromSources(srcs, currClustRun, geneSrcToTraceNumberPerClusterRun_, specialTracePads, srcLocs);   
 
        Point2D lastRunStart = (Point2D)runStart.clone();
        runStart = new Point2D.Double();
        traceStart = new Point2D.Double();
        placeTargetClusters(lastRunStart, clustCounts, currClustRun, clusterMap, 
                            clusterRunSpecialPads_, runNum, baseGenome, sld_.lo, 
                            sld_.results.nodeLocations, runStart, traceStart);
        clusterRunStarts_.put(ccRunObj, traceStart);
        DataAccessContext bgc3 = new DataAccessContext(sld_.rcx, baseGenome, sld_.rcx.getLayout());
        placeSources(srcs, currClustRun++, traceBoundary_, traceWidth,
                     geneSrcToTraceNumberPerClusterRun_, clusterRunStarts_, 
                     srcLocs, clusterRunSpecialPads_,
                     bgc3, sld_.results.nodeLocations);
        placeNonGeneSources(center, traceBoundary_, nonGeneSrcToTraceNumberPerClusterRun_, sourceToTrace_, sld_.results.nodeLocations);
        if ((monitor != null) && !monitor.keepGoing()) {
          throw new AsynchExitRequestException();
        }
        remainingClusters = tossPlacedClusters(nextClusters, remainingClusters);
      }
      currTrace++;
    }
    
    DataAccessContext bgc2 = new DataAccessContext(sld_.rcx, baseGenome, sld_.rcx.getLayout());
    placeSingletonClusters(runStart, singletonClusters, clusterMap, bgc2, sld_.results.nodeLocations);

    
    //
    // Original trace width was a conservative estimate.  Tighten thing up now
    // that we know the true story.
    //
    
    if (!traceToSource.isEmpty()) {
      Integer minTrace = traceToSource.firstKey();
      Integer maxTrace = traceToSource.lastKey();
      double actualWidth = ((double)(maxTrace.intValue() - minTrace.intValue())) * TRACE_OFFSET_X_;
      double traceFixup = traceWidth - actualWidth - (2.0 * TRACE_OFFSET_X_);
      if (traceFixup != 0.0) {
        Iterator<String> pit = sld_.results.nodeLocations.keySet().iterator();
        while (pit.hasNext()) {
          String src = pit.next();
          if (geneSources_.contains(src)) {
            Point2D loc = sld_.results.nodeLocations.get(src);
            loc.setLocation(loc.getX() + traceFixup, loc.getY());
          }
        }
      }   
    }
    
    //
    // Note that with this layout, the sources are not in clusters (they have no inputs, they
    // have no fanOuts!
    //
    
    sld_.gASCs.addAll(termClusters_);  
    return;
  }
  
  /***************************************************************************
  **
  ** Figure out the link routes
  */
  
  public void routeLinks(SpecialtyLayoutEngine.GlobalSLEState gsles, BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    if (sld_.results == null) {
      return;
    }
    
    Iterator<String> sit = geneSrcToClusters_.keySet().iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      SpecialtyLayoutLinkData spec = buildLinkSegmentsToClusters(srcID, geneSrcToClusters_,
                                                                   clusterRunStarts_,
                                                                   clusterRunSpecialPads_,
                                                                   clusterToClusterRunNumber_,
                                                                   geneSrcToTraceNumberPerClusterRun_,
                                                                   sourceToTrace_, traceBoundary_, 
                                                                   sld_.genome, sld_.lo,
                                                                   sld_.results.nodeLocations, sld_.results.padChanges);
      sld_.results.setLinkPointsForSrc(srcID, spec);
    }
    
    Iterator ngstcit = nonGeneSrcToClusters_.keySet().iterator();
    while (ngstcit.hasNext()) {
      String srcID = (String)ngstcit.next();
      SpecialtyLayoutLinkData spec = buildLinkSegmentsToClusters(srcID, nonGeneSrcToClusters_,
                                                                   clusterRunStarts_,
                                                                   clusterRunSpecialPads_,
                                                                   clusterToClusterRunNumber_,
                                                                   nonGeneSrcToTraceNumberPerClusterRun_,
                                                                   sourceToTrace_, traceBoundary_, 
                                                                   sld_.genome, sld_.lo,
                                                                   sld_.results.nodeLocations, sld_.results.padChanges);
      sld_.results.setLinkPointsForSrc(srcID, spec);
    } 
    
    
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
 
    return;
  }
  
  /***************************************************************************
  **
  ** Assign colors
  */
  
  public void assignColors(BTProgressMonitor monitor) throws AsynchExitRequestException {
    if (sld_.results == null) {
      return;
    }
    assignColors(geneSrcToTraceNumberPerClusterRun_, 
                 nonGeneSrcToTraceNumberPerClusterRun_,
                 sld_.genome, termClusters_, 
                 geneSources_, nonGeneSources_, sld_.results.nodeLocations,
                 sld_.results.nodeColors, sld_.results.linkColors);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the parameter dialog
  */
  
  public SpecialtyLayoutEngineParamDialogFactory.BuildArgs getParameterDialogBuildArgs(Genome genome, String selectedID, boolean forSubset) {
    return (new SpecialtyLayoutEngineParamDialogFactory.BuildArgs(appState_, genome,  
                                                                  SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType.HALO, 
                                                                  selectedID, this));
  }
  
  /***************************************************************************
  **
  ** Get the undo string for the layout
  */
  
  public String getUndoString() {
    return ("undo.haloLayout");
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

 
  /***************************************************************************
  **
  ** Get default params
  */
  
  public static HaloLayoutParams getDefaultParams() {
    HaloLayoutParams retval = new HaloLayoutParams();
    retval.startType = StartSeed.GREEDY_START;
    retval.selected = null;
    retval.overlayOption = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    retval.traceMult = 2;
    retval.textToo = true;
    retval.assignColorMethod = ColorTypes.KEEP_COLORS;
    retval.showBubbles = true;
    retval.checkColorOverlap = false;  
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  
  public static class HaloLayoutParams implements SpecialtyLayoutEngineParams, Cloneable {
    public StartSeed startType; 
    public String selected;
    public int overlayOption;
    public int traceMult;
    public boolean textToo;  
    public ColorTypes assignColorMethod;
    public boolean showBubbles;
    public boolean checkColorOverlap;
     
    public HaloLayoutParams clone() {
      try {
        HaloLayoutParams retval = (HaloLayoutParams)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public int getOverlayRelayoutOption() {
      return (overlayOption);
    } 

    public ColorTypes getColorDecision() {
      return (assignColorMethod);
    }
    
    public boolean showBubbles() {
      return (showBubbles);
    }
    
    public boolean checkColorOverlaps() {
      return (checkColorOverlap);    
    }   
 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Assign colors
  */
  
  private void calcNonGeneSrcPadChanges(String ngSrc, Genome genome, Map<String, PadCalculatorToo.PadResult> padChanges) {   
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String lsource = link.getSource();
      if (lsource.equals(ngSrc)) {
        PadCalculatorToo.PadResult pres = padChanges.get(link.getID());
        if (pres != null) {
          pres.launch = NON_GENE_SOURCE_PAD_;
        } else {
          pres = new PadCalculatorToo.PadResult(NON_GENE_SOURCE_PAD_, link.getLandingPad());
          padChanges.put(link.getID(), pres);
        }
      }
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Assign colors
  */
  
  private void assignColors(Map srcToTraceNumberPerClusterRun,
                            Map nonGeneSrcToTraceNumberPerClusterRun,
                            Genome genome, List clusters, 
                            Set geneSources, Set nonGeneSources, Map placement,
                            Map nodeColors, Map linkColors) {
    
    //
    // Need to build up a top-down list of sources
    //
    
    ArrayList srcTopToBottom = new ArrayList();
    ArrayList junkList = new ArrayList();
    Iterator git = geneSources.iterator();
    while (git.hasNext()) {
      String gene = (String)git.next();
      Point2D ptLoc = (Point2D)placement.get(gene);
      if (ptLoc == null) {  // FIX ME! If network does not match inferelator criteria, some srcs not placed.
        junkList.add(gene);
        continue;
      }
      TaggedByCount tbc = new TaggedByCount(gene, (int)Math.round(ptLoc.getY()));
      srcTopToBottom.add(tbc);
    }    
    Collections.sort(srcTopToBottom);
    
    //
    // A hack to cover non-placed sources.  FIX ME!
    //
    
    if (!srcTopToBottom.isEmpty()) {
      TaggedByCount genetc = (TaggedByCount)srcTopToBottom.get(0);
      int newMin = genetc.count - 1;
      int numJ = junkList.size();
      for (int i = 0; i < numJ; i++) {
        String gene = (String)junkList.get(i);
        TaggedByCount tbc = new TaggedByCount(gene, newMin);
        srcTopToBottom.add(0, tbc);
      }
    }
    
    //
    // Need to build a min/max cluster# map for each source.
    //    
      
    HashMap minMaxClustPerSrc = new HashMap();
    Iterator sit = srcToTraceNumberPerClusterRun.keySet().iterator();
    while (sit.hasNext()) {
      String srcID = (String)sit.next();
      MinMax mm = new MinMax(Integer.MAX_VALUE, Integer.MIN_VALUE);
      minMaxClustPerSrc.put(srcID, mm);
      Map perSrc = (Map)srcToTraceNumberPerClusterRun.get(srcID);
      Iterator psit = perSrc.keySet().iterator();
      while (psit.hasNext()) {
        Integer currClust = (Integer)psit.next();
        int clustNum = currClust.intValue();
        if (clustNum < mm.min) {
          mm.min = clustNum;
        }
        if (clustNum > mm.max) {
          mm.max = clustNum;
        }
      }
    }
    
    // Add in same info for non-gene sources:
    
    Iterator ngsit = nonGeneSrcToTraceNumberPerClusterRun.keySet().iterator();
    while (ngsit.hasNext()) {
      String srcID = (String)ngsit.next();
      MinMax mm = new MinMax(Integer.MAX_VALUE, Integer.MIN_VALUE);
      minMaxClustPerSrc.put(srcID, mm);
      Map perSrc = (Map)nonGeneSrcToTraceNumberPerClusterRun.get(srcID);
      Iterator psit = perSrc.keySet().iterator();
      while (psit.hasNext()) {
        Integer currClust = (Integer)psit.next();
        int clustNum = currClust.intValue();
        if (clustNum < mm.min) {
          mm.min = clustNum;
        }
        if (clustNum > mm.max) {
          mm.max = clustNum;
        }
      }
    }
    
    Database db = sld_.appState.getDB();
    int numCol = db.getNumColors();
    
    HashMap minMaxClustPerColor = new HashMap();
    
    int currCol = 0;
    Iterator ngit = nonGeneSources.iterator();
    while (ngit.hasNext()) {
      String nonGene = (String)ngit.next();
      MinMax mms = (MinMax)minMaxClustPerSrc.get(nonGene);
      // Another hack for non-standard sources:
      if (mms == null) {
        mms = new MinMax(0, 0);
      }
      String nextCol = db.getGeneColor(currCol);
      MinMax mmc = (MinMax)minMaxClustPerColor.get(nextCol);
      if ((mmc == null) || (mms.min > mmc.max)) {
        nodeColors.put(nonGene, nextCol);
        minMaxClustPerColor.put(nextCol, mms);        
        currCol = ((currCol + 1) % numCol);
        continue;
      }
      // FIX ME!  Assuming we do not have too many non-gene colors!
      
    }
     
    int numSrc = srcTopToBottom.size();
    for (int i = 0; i < numSrc; i++) {
      TaggedByCount gene = (TaggedByCount)srcTopToBottom.get(i);
      String srcID = gene.id;
      MinMax mms = (MinMax)minMaxClustPerSrc.get(srcID);
      // Another hack for non-standard sources:
      if (mms == null) {
        mms = new MinMax(0, 0);
      }
      String nextCol = db.getGeneColor(currCol);
      MinMax mmc = (MinMax)minMaxClustPerColor.get(nextCol);
      if ((mmc == null) || (mms.min > mmc.max)) {
        nodeColors.put(srcID, nextCol);
        minMaxClustPerColor.put(nextCol, mms);        
        currCol = ((currCol + 1) % numCol);
        continue;
      }
      
      //
      // Find a color that has run out:
      //
      
      int checkCol = (currCol + 1) % numCol;
      for (int j = 0; j < (numCol - 1); j++) {
        nextCol = db.getGeneColor(checkCol);
        mmc = (MinMax)minMaxClustPerColor.get(nextCol);
        if ((mmc == null) || (mms.min > mmc.max)) {
          nodeColors.put(srcID, nextCol);
          minMaxClustPerColor.put(nextCol, mms);        
          currCol = ((currCol + 1) % numCol);
          break;
        }
        checkCol = ((checkCol + 1) % numCol);
      }
      
      //
      // No luck (FIXME: Could go to resolution of traces within cluster runs)
      // 
     
      if (checkCol == currCol) {
        nextCol = db.getGeneColor(checkCol);
        nodeColors.put(srcID, nextCol);
        MinMax mmCombo = new MinMax(mmc.min, mms.max);
        minMaxClustPerColor.put(nextCol, mmCombo);        
        currCol = ((currCol + 1) % numCol);
      }
    }
    
    Iterator lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = (Linkage)lit.next();
      String lsource = link.getSource();
      if (geneSources.contains(lsource) || nonGeneSources.contains(lsource)) {
        String color = (String)nodeColors.get(lsource);
        linkColors.put(link.getID(), color);
      }
    }
    
    int numClust = clusters.size();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusters.get(i);
      tc.colorTheCluster(genome, nodeColors, linkColors);
    }
    return;   
  }        
 
  /***************************************************************************
  **
  ** Create a new map just holding remaining clusters
  */
  
  private Map<String, List<GeneAndSatelliteCluster>> tossPlacedClusters(List<GeneAndSatelliteCluster> placedClusters, 
                                                                        Map<String, List<GeneAndSatelliteCluster>> srcToClust) {

    int pnum = placedClusters.size();
    HashMap<String, List<GeneAndSatelliteCluster>> retval = new HashMap<String, List<GeneAndSatelliteCluster>>();
    Iterator<String> kit = srcToClust.keySet().iterator();
    while (kit.hasNext()) {
      String srcID = kit.next();
      List<GeneAndSatelliteCluster> oldClusts = srcToClust.get(srcID);
      ArrayList<GeneAndSatelliteCluster> newClusts = new ArrayList<GeneAndSatelliteCluster>();
      
      int onum = oldClusts.size();
      for (int i = 0; i < onum; i++) {
        GeneAndSatelliteCluster oldClust = oldClusts.get(i);
        boolean ditch = false;
        for (int j = 0; j < pnum; j++) {
          GeneAndSatelliteCluster clust = placedClusters.get(j);
          if (clust.getCoreID().equals(oldClust.getCoreID())) {
            ditch = true;
            break;
          }
        }
        if (!ditch) {
          newClusts.add(oldClust);
        }
      }
      if (!newClusts.isEmpty()) {
        retval.put(srcID, newClusts);
      }
    }
    return (retval);   
  }        

  /***************************************************************************
  **
  ** Calculate source location based on pad offset.
  */
  
  
  private Point2D sourcePos(String srcID, Point2D basePos, DataAccessContext rcx) { 
    INodeRenderer rend = rcx.getLayout().getNodeProperties(srcID).getRenderer();
    Node srcNode = rcx.getGenome().getNode(srcID);

    Vector2D offset = rend.getLaunchPadOffset(0, srcNode, rcx);
    offset.scale(-1.0);
    Point2D srcLoc = offset.add(basePos);
    UiUtil.forceToGrid(srcLoc.getX(), srcLoc.getY(), srcLoc, UiUtil.GRID_SIZE);
    return (srcLoc);
  } 

  /***************************************************************************
  **
  ** Calculate size of all non-genes, in terms of runs
  */
  
  private void nonGeneSizes(Set<String> ngSrcID, DataAccessContext rcx, Map<String, Integer> sizeMap) {
    Iterator<String> ngit = ngSrcID.iterator();
    while (ngit.hasNext()) {
      String nonGene = ngit.next();
      if (!sizeMap.containsKey(nonGene)) {
        int size = nonGeneSize(nonGene, rcx);
        sizeMap.put(nonGene, new Integer(size));
      }
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Calculate size of a non-gene, in terms of runs
  */
  
  private int nonGeneSize(String srcID, DataAccessContext rcx) { 
    INodeRenderer rend = rcx.getLayout().getNodeProperties(srcID).getRenderer();
    Node srcNode = rcx.getGenome().getNode(srcID);
    // not placed yet, so absolute location is bogus, but size is not:
    Rectangle bounds  = rend.getBounds(srcNode, rcx, null);   
    int numTracks = (int)Math.ceil((((double)bounds.width) / 2.0) / TRACE_OFFSET_X_);
    return (numTracks);
  }  
  
  /***************************************************************************
  **
  ** Figure out the trace padding needed
  */
  
  private void tracePaddingFromSources(Set<String> srcs, int clusterRun,
                                       Map<String, Map<Integer, Integer>> srcToTraceNumberPerClusterRun,
                                       Map<Integer, Integer> specialTracePads, Map<String, Integer> srcLocs) {
    //
    // Sources that are at or closer than 2 traces need to be offset left.  Otherwise,
    // they can be stacked directly above each other.  Once we have accumulated 
    // MAX_SRC_STACKING_ offsets, we reset the sources to the right margin.  This
    // requires the trace placement to be bumped by two traces worth.
    //
      
    //
    // X offsets depend on the actual number of srcs we have:
    //
    
    TreeMap<Integer, String> orderedSrcs = new TreeMap<Integer, String>();
    Iterator<String> sit = srcs.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();    
      Map<Integer, Integer> perSrc = srcToTraceNumberPerClusterRun.get(srcID);
      Integer traceNum = perSrc.get(new Integer(clusterRun));
      orderedSrcs.put(traceNum, srcID);
    }
        
    boolean first = true;
    int xPos = 0;
    Iterator<Integer> osit = orderedSrcs.keySet().iterator();
    int lastTraceNum = Integer.MIN_VALUE;
    while (osit.hasNext()) {
      Integer traceNum = osit.next();
      String srcID = orderedSrcs.get(traceNum);
      if (first) {
        srcLocs.put(srcID, new Integer(xPos--));
        lastTraceNum = traceNum.intValue();
        first = false;
        continue;
      }
      
      int currTraceNum = traceNum.intValue();
      int traceDiff = currTraceNum - lastTraceNum;
      if (traceDiff <= MIN_TRACE_DIFF_) {  // need to offset or reset to right with special trace pad
        if (-xPos == MAX_SRC_STACKING_) {
          xPos = 0;
          specialTracePads.put(new Integer(currTraceNum - 1), new Integer((MIN_TRACE_DIFF_ + 1) - traceDiff));
        } 
      } else {  // need to stack and reset without trace pad
        xPos = 0;
      }
      srcLocs.put(srcID, new Integer(xPos--));
      lastTraceNum = currTraceNum;
    } 
    return;
  }    
  
  /***************************************************************************
  **
  ** Place sources
  */
  
  private void placeSources(Set srcs, int clusterRun, Point2D basePt, double traceOffset,
                            Map srcToTraceNumberPerClusterRun, 
                            Map<Integer, Point2D> clusterRunStarts, Map srcLocs, Map clusterRunSpecialPads,
                            DataAccessContext rcx,
                            Map placement) {
    double startX = basePt.getX() - traceOffset;
    
    Integer crObj = new Integer(clusterRun);
    Iterator sit = srcs.iterator();
    while (sit.hasNext()) {
      String srcID = (String)sit.next();
      Map perSrc = (Map)srcToTraceNumberPerClusterRun.get(srcID);
      Integer traceNum = (Integer)perSrc.get(crObj);
      double runY = getClusterRunTraceY(clusterRunStarts, clusterRunSpecialPads, clusterRun, traceNum.intValue());
      Integer xOff = (Integer)srcLocs.get(srcID);
      double srcX = startX + (xOff.doubleValue() * SRC_OFFSET_X_);
      Point2D pt = new Point2D.Double(srcX, runY);
      Point2D ptSrc = sourcePos(srcID, pt, rcx);       
      placement.put(srcID, ptSrc);
    } 
    return;
  }  
  
 /***************************************************************************
  **
  ** Place non gene sources
  */
  
  private void placeNonGeneSources(Point2D basePt, Point2D traceBoundary,
                                   Map nonGeneSrcToTraceNumberPerClusterRun, 
                                   Map sourceToTrace, 
                                   Map placement) {
    
    Iterator sit = nonGeneSrcToTraceNumberPerClusterRun.keySet().iterator();
    while (sit.hasNext()) {
      String srcID = (String)sit.next();
      Point2D placePt = (Point2D)placement.get(srcID);
      Integer trace = (Integer)sourceToTrace.get(srcID);
      if ((placePt == null) && (trace != null)) {
        double traceX = getMainTraceX(srcID, sourceToTrace, traceBoundary).doubleValue();
        placePt = new Point2D.Double(traceX, basePt.getY());
        placement.put(srcID, placePt);
      }
    }
    return;
  }    
  
 
  /***************************************************************************
  **
  ** Place targetClusters
  */
  
  private void placeTargetClusters(Point2D startPoint, List clustCounts, int runNum,
                                   Map clusterMap, Map clusterRunSpecialPads, int traceCount,
                                   Genome genome, Layout lo,
                                   Map placement, Point2D runStart, Point2D traceStart) {
    double startX = startPoint.getX();

    double maxNodeSpace = (FIRST_ROW_PAD_ * UiUtil.GRID_SIZE);  // Minimum space needed...
    int num = clustCounts.size();
    for (int i = 0; i < num; i++) {    
      TaggedByCount tbc = (TaggedByCount)clustCounts.get(i);
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(tbc.id);
      GeneAndSatelliteCluster.ClusterDims oh = tc.getClusterDims(sld_.nps, sld_.rcx);
      double needs = oh.getFullHeightIncrement();
      if (needs > maxNodeSpace) {
        maxNodeSpace = needs;
      }
    }
    Double maxNodeSpaceObj = new Double(maxNodeSpace);
      
    double traceY = getClusterRunTraceYRequirements(clusterRunSpecialPads, runNum, traceCount);
    traceY += startPoint.getY() + VERTICAL_CLUSTER_PAD_;

    
    int tcount = 0;      
    for (int i = 0; i < num; i++) {    
      TaggedByCount tbc = (TaggedByCount)clustCounts.get(i);
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(tbc.id);
      Point2D base = new Point2D.Double(startX + ((double)tcount * OFFSET_X_), traceY);
      tc.locateAsTarget(base, sld_.nps, sld_.rcx, maxNodeSpaceObj);
      tcount++;
    }
    traceStart.setLocation(startX, traceY);
    double startY = traceY + maxNodeSpace; 
    runStart.setLocation(startX, startY);
    return;   
  } 
  
  /***************************************************************************
  **
  ** Place singleton clusters
  */
  
  private void placeSingletonClusters(Point2D startPoint, Set singletonClusters,
                                      Map clusterMap, DataAccessContext rcx,
                                      Map placement) {
    double startX = startPoint.getX(); 
    TreeSet fixedOrder = new TreeSet(singletonClusters);
    
    double maxNodeSpace = (FIRST_ROW_PAD_ * UiUtil.GRID_SIZE);  // Minimum space needed...
    Iterator sit = fixedOrder.iterator();
    while (sit.hasNext()) {
      String singID = (String)sit.next();
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(singID);
      GeneAndSatelliteCluster.ClusterDims oh = tc.getClusterDims(sld_.nps, sld_.rcx);
      double needs = oh.getFullHeightIncrement();
      if (needs > maxNodeSpace) {
        maxNodeSpace = needs;
      }
    }
    Double maxNodeSpaceObj = new Double(maxNodeSpace);
    
    double startY = startPoint.getY() + VERTICAL_CLUSTER_PAD_;
  
    Genome genome = rcx.getGenome();
    double currX = startX;
    sit = fixedOrder.iterator();
    while (sit.hasNext()) {
      String singID = (String)sit.next();
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(singID);
      Point2D base = new Point2D.Double(currX, startY);
      tc.locateAsTarget(base, sld_.nps, sld_.rcx, maxNodeSpaceObj);
      INodeRenderer rend = rcx.getLayout().getNodeProperties(singID).getRenderer();
      Node singNode = genome.getNode(singID);
      // This works OK since there will be NO pad changes on this singleton:
      double width = rend.getWidth(singNode, rcx);
      int numOff = (int)Math.ceil(width / OFFSET_X_);
      currX += (numOff * OFFSET_X_);
    }
    return;   
  } 
 
  /***************************************************************************
  **
  ** Find the genes that are sources and not targets
  */
  
  private Set<String> findGeneSources(Genome genome) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Gene> nit = genome.getGeneIterator();
    while (nit.hasNext()) {
      Gene gene = nit.next();
      String geneID = gene.getID();
      int tc = genome.getTargetCount(geneID);
      if (tc != 0) {
        int sc = genome.getSourceCount(geneID);
        if (sc == 0) {
          retval.add(geneID);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find the non-genes that are sources
  */
  
  private Set<String> findNonGeneSources(Genome genome) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      int tc = genome.getTargetCount(nodeID);
      if (tc != 0) {
        int sc = genome.getSourceCount(nodeID);
        if (sc == 0) {
          retval.add(nodeID);
        }
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Rank all sources by how many target clusters they target
  */
  
  private List<TaggedByCount> rankAllSourcesByTargetClusters(Set<String> srcGenes, Set<String> srcNonGenes,
                                                             List<GeneAndSatelliteCluster> allClusters, 
                                                             Map<String, List<GeneAndSatelliteCluster>> srcToClust, 
                                                             Map<String, List<GeneAndSatelliteCluster>> nonGeneSrcToClusters, 
                                                             Set<String> singletonClusters) {
    ArrayList retval = new ArrayList();
    int num = allClusters.size();
    singletonClusters.clear();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = allClusters.get(i);
      singletonClusters.add(clust.getCoreID());
    }
    
    HashSet allSrc = new HashSet(srcGenes);
    allSrc.addAll(srcNonGenes);
    Iterator<String> sit = allSrc.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      Map<String, List<GeneAndSatelliteCluster>> targMap = (srcNonGenes.contains(srcID)) ? nonGeneSrcToClusters : srcToClust;
      TaggedByCount tbc = new TaggedByCount(srcID);
      retval.add(tbc);
      for (int i = 0; i < num; i++) {
        GeneAndSatelliteCluster clust = allClusters.get(i);
        if (clust.isATarget(srcID)) {
          tbc.increment();
          List<GeneAndSatelliteCluster> clusts = targMap.get(srcID);
          if (clusts == null) {
            clusts = new ArrayList<GeneAndSatelliteCluster>();
            targMap.put(srcID, clusts);
          }
          clusts.add(clust);
          singletonClusters.remove(clust.getCoreID());
        }     
      }
    }
    Collections.sort(retval);
    return (retval);
  } 

  /***************************************************************************
  **
  ** Having found the target clusters for a seed source, we rank them, based on
  ** the sum of the non-placed target clusters targeted by all the sources
  ** into the cluster.
  */
  
  private void orderInitialTargetClusters(List<GeneAndSatelliteCluster> currClusters, 
                                          Map<String, List<GeneAndSatelliteCluster>> srcToClust, 
                                          List<TaggedByCount> clustCounts, List<TaggedByCount> sourceCounts) {
    //
    // Go through all clusters.  For each source, figure out the clusters it is targeting.
    // If a cluster is not in the current set, add the cluster key to a set (so we do not
    // double count clusters that share multiple inputs with us).  The size of the set
    // is our ranking.
    //
    // We also record the count of future clusters targeted by each source.
    //
    
    HashSet runClusts = new HashSet();
    int num = currClusters.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = currClusters.get(i);
      runClusts.add(clust.getCoreID());
    }
    
    HashMap<String, Set> srcToNonCurrClusts = new HashMap<String, Set>();
    
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = currClusters.get(i);
      HashSet clustSet = new HashSet();
      Iterator<String> sit = clust.getInputs().iterator();
      while (sit.hasNext()) {
        String sID = sit.next();
        List<GeneAndSatelliteCluster> clusts = srcToClust.get(sID);
        if (clusts == null) {
          continue;
        }
        int cNum = clusts.size();
        for (int j = 0; j < cNum; j++) {
          GeneAndSatelliteCluster oClust = clusts.get(j);
          Set<String> currSrcToNonCurrClust = srcToNonCurrClusts.get(sID);
          if (currSrcToNonCurrClust == null) {
            currSrcToNonCurrClust = new HashSet<String>();
            srcToNonCurrClusts.put(sID, currSrcToNonCurrClust); 
          }          
          if (!runClusts.contains(oClust.getCoreID())) {  // Don't count current run
            currSrcToNonCurrClust.add(oClust.getCoreID());
            clustSet.add(oClust.getCoreID());
          }
        }
      }
      TaggedByCount tbc = new TaggedByCount(clust.getCoreID(), clustSet.size());
      clustCounts.add(tbc);
    }
    
    Iterator<String> kit = srcToNonCurrClusts.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      Set nonCurr = srcToNonCurrClusts.get(key);
      sourceCounts.add(new TaggedByCount(key, nonCurr.size()));
    }
    
    Collections.sort(clustCounts); // Future clusters that are siblings of each current cluster
    Collections.sort(sourceCounts);  // Future clusters targeted by source
    return;    
  } 
  
  /***************************************************************************
  **
  ** We have an ordering of clusters in a cluster run.  Order the sources
  ** entering into the cluster run.
  */
  
  private List<String> orderInitialSourceLeads(Genome genome, Map<String, GeneAndSatelliteCluster> clusterMap, 
                                               List<TaggedByCount> clustCounts, 
                                               List<TaggedByCount> sourceCounts, Set<String> nonGeneSources) {
    //
    // Take the rightmost cluster. Sources will be ordered based on their
    // total cluster count.
    //    
    
    ArrayList retval = new ArrayList();
    int num = clustCounts.size();
    for (int i = num - 1; i >= 0; i--) {
      TaggedByCount tbc = clustCounts.get(i);
      GeneAndSatelliteCluster tc = clusterMap.get(tbc.id);
      int sNum = sourceCounts.size();
      for (int j = sNum - 1; j >= 0; j--) {      
        TaggedByCount stbc = sourceCounts.get(j);
        if (tc.isATarget(stbc.id) && (!retval.contains(stbc.id))) {
          retval.add(stbc.id);
        }
      }
      Iterator<String> ngsit = nonGeneSources.iterator();
      while (ngsit.hasNext()) {
        String nonGene = ngsit.next();
        if (tc.isATarget(nonGene) && (!retval.contains(nonGene))) {
          retval.add(nonGene);
        }
      }
    }
    
    for (int i = 0; i < num; i++) {
      TaggedByCount tbc = (TaggedByCount)clustCounts.get(i);
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(tbc.id);
      tc.orderByTraceOrder(retval, sld_.nps);
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Count the sources inbound to the run
  */
  
  private int sourcesInboundToRun(List currClusters) {
    HashSet srcs = new HashSet();    
    int num = currClusters.size();    
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = (GeneAndSatelliteCluster)currClusters.get(i);
      Iterator sit = clust.getInputs().iterator();
      while (sit.hasNext()) {
        String sID = (String)sit.next();
        srcs.add(sID);
      }
    }
    return (srcs.size());
  }  

  /***************************************************************************
  **
  ** Assign the source traces
  */
  
  private void assignSourceTraces(Map sourceToTrace, SortedMap traceToSource, 
                                  Map currentSourceToTrace, List newSources, Map nonGeneSizes, 
                                  List nextClusters, Map remainingClusters) {
    Integer lastKey = (Integer)traceToSource.lastKey();
    int currTrace = lastKey.intValue() + 1;
    
    Map downstreamClusters = tossPlacedClusters(nextClusters, remainingClusters);

    //
    // FIX ME!  Adding useless space if non-gene nodes not next to each other!
    //
    
    Set nonGeneKeys = nonGeneSizes.keySet();
    int num = newSources.size();
    for (int i = 0; i < num; i++) {
      String src = (String)newSources.get(i);
      currentSourceToTrace.put(src, new Integer(i));
      // No trace is needed if gene source is all done in the current row, though
      // it is added to the currentSourceToTrace map to handle need of current cluster placement!
      if (!nonGeneKeys.contains(src)) {
        List lowerRows = (List)downstreamClusters.get(src);
        if ((lowerRows == null) || lowerRows.isEmpty()) {
          continue;
        }
      }
      Integer ngSize = (Integer)nonGeneSizes.get(src);
      int traceExtra = (ngSize == null) ? 0 : ngSize.intValue();
      // Extra trace width only added if we don't have enough space
      // for the nonGene node:
      if (traceExtra != 0) {
        Integer lastNonTrace = null;
        Integer lastNonWidth = null;
        Iterator tsit = traceToSource.keySet().iterator();
        while (tsit.hasNext()) {
          Integer trace = (Integer)tsit.next();
          String lastSrc = (String)traceToSource.get(trace);
          if (nonGeneKeys.contains(lastSrc)) {
            lastNonTrace = trace;
            lastNonWidth = (Integer)nonGeneSizes.get(lastSrc);
          }
        }
        if (lastNonTrace != null) {
          int traceDelta = currTrace - lastNonTrace.intValue();
          int needWidth = lastNonWidth.intValue() + traceExtra;
          traceExtra = needWidth - traceDelta;
          if (traceExtra < 0) {
            traceExtra = 0;
          }
        }
        currTrace += traceExtra;
      }
      Integer newTrace = new Integer(currTrace);
      sourceToTrace.put(src, newTrace);
      traceToSource.put(newTrace, src);
      currTrace += (1 + traceExtra);      
    }
    return;
  }  

  /***************************************************************************
  **
  ** We order the target clusters in a cluster run, with the rightmost cluster
  ** going to the cluster with the lowest (average?) link column assignment.  New
  ** sources are added last.
  */
  
  private List orderNewSources(List currClusters, Map srcToClust, Map sourceToTrace) {
    
    ArrayList sourceCounts = new ArrayList();
    HashSet runClusts = new HashSet();
    int num = currClusters.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = (GeneAndSatelliteCluster)currClusters.get(i);
      runClusts.add(clust.getCoreID());
    }
    
    HashMap newSrcToNonCurrClusts = new HashMap();    
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = (GeneAndSatelliteCluster)currClusters.get(i);
      Iterator sit = clust.getInputs().iterator();
      while (sit.hasNext()) {
        String sID = (String)sit.next();
        if (sourceToTrace.keySet().contains(sID)) {
          continue;  // Not new...
        }
        List clusts = (List)srcToClust.get(sID);
        if (clusts == null) {
          continue;
        }
        int cNum = clusts.size();
        for (int j = 0; j < cNum; j++) {
          GeneAndSatelliteCluster oClust = (GeneAndSatelliteCluster)clusts.get(j);
          Set currSrcToNonCurrClust = (Set)newSrcToNonCurrClusts.get(sID);
          if (currSrcToNonCurrClust == null) {
            currSrcToNonCurrClust = new HashSet();
            newSrcToNonCurrClusts.put(sID, currSrcToNonCurrClust); 
          }          
          if (!runClusts.contains(oClust.getCoreID())) {  // Don't count current run
            currSrcToNonCurrClust.add(oClust.getCoreID());
          }
        }
      }
    }
    
    Iterator kit = newSrcToNonCurrClusts.keySet().iterator();
    while (kit.hasNext()) {
      String key = (String)kit.next();
      Set nonCurr = (Set)newSrcToNonCurrClusts.get(key);
      sourceCounts.add(new TaggedByCount(key, nonCurr.size()));
    }
    
    Collections.sort(sourceCounts);  // Future clusters targeted by source
    
    ArrayList retval = new ArrayList();
    int snum = sourceCounts.size();
    for (int i = snum - 1; i >= 0; i--) {
      TaggedByCount tbc = (TaggedByCount)sourceCounts.get(i);
      retval.add(tbc.id);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** We order the target clusters in a cluster run, with the rightmost cluster
  ** going to the cluster with the lowest (average?) link column assignment. 
  */
  
  private void orderFollowingTargetClusters(List currClusters, Map srcToClust, 
                                            Map sourceToTrace, List clustCounts) {
        
    int num = currClusters.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = (GeneAndSatelliteCluster)currClusters.get(i);
      Iterator sit = clust.getInputs().iterator();
      int sum = 0;
      int count = 0;
      while (sit.hasNext()) {
        String sID = (String)sit.next();
        Integer trace = (Integer)sourceToTrace.get(sID);
        if (trace == null) {  // If source is illegal and not being handled...
          continue;
        }
        sum += trace.intValue();
        count++;
      }
      clustCounts.add(new TaggedByCount(clust.getCoreID(), (int)Math.round((double)sum / (double)count)));
    }
    
    Collections.sort(clustCounts); 
    return;
  }
  
  /***************************************************************************
  **
  ** We have an ordering of clusters in a cluster run. Position penultimate
  ** node accordingly
  */
  
  private List orderFollowingSourceLeads(Genome genome, Map clusterMap, List clustCounts, Set nonGeneSources) {
    ArrayList retval = new ArrayList();
    int num = clustCounts.size();
    for (int i = num - 1; i >= 0; i--) {
      TaggedByCount tbc = (TaggedByCount)clustCounts.get(i);
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(tbc.id);
      Iterator iit = tc.getInputs().iterator();
      while (iit.hasNext()) {
        String srcID = (String)iit.next();
        if (!retval.contains(srcID)) {
          retval.add(srcID);
        }
      }
      Iterator ngsit = nonGeneSources.iterator();
      while (ngsit.hasNext()) {
        String nonGene = (String)ngsit.next();
        if (tc.isATarget(nonGene) && (!retval.contains(nonGene))) {
          retval.add(nonGene);
        }
      }
    }
    
    for (int i = 0; i < num; i++) {
      TaggedByCount tbc = (TaggedByCount)clustCounts.get(i);
      GeneAndSatelliteCluster tc = (GeneAndSatelliteCluster)clusterMap.get(tbc.id);
      tc.orderByTraceOrder(retval, sld_.nps);
    }

    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Record Cluster Number Run per cluster ID
  */
  
  private void recordClusterRunNumber(Map<String, int[]> clusterToClusterRunNumber, List<TaggedByCount> clustOrder, int currRun) {
    Integer currRunObj = new Integer(currRun);
    int num = clustOrder.size();
    for (int i = 0; i < num; i++) {
      TaggedByCount clust = clustOrder.get(i);
      int[] pos = new int[2];
      pos[0] = currRun;
      pos[1] = i;
      clusterToClusterRunNumber.put(clust.id, pos);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Record Trace number for sources for each cluster run
  */
  
  private void recordTraceForClusterRun(Map<String, Map<Integer, Integer>> srcToTraceNumberPerClusterRun, 
                                        Map<String, Map<Integer, Integer>> nonGeneSrcToTraceNumberPerClusterRun,          
                                        List<String> srcLeads, int currRun, Set<String> nonGeneSources) {
    Integer currRunObj = new Integer(currRun);
    int num = srcLeads.size();
    int count = 0;
    for (int i = num - 1; i >= 0; i--) {
    //for (int i = 0; i < num; i++) {
      String srcID = srcLeads.get(i);
      Map<String, Map<Integer, Integer>> targMap = (nonGeneSources.contains(srcID)) ? nonGeneSrcToTraceNumberPerClusterRun
                                                                                    : srcToTraceNumberPerClusterRun;
      Map perSrc = (Map)targMap.get(srcID);
      if (perSrc == null) {
        perSrc = new HashMap();
        targMap.put(srcID, perSrc);
      }
      perSrc.put(currRunObj, new Integer(count++));  // FIX ME: Reverse??
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the X of the mainline trace
  */
  
  private Double getMainTraceX(String srcID, Map sourceToTrace, Point2D traceBoundary) {
    Integer traceNum = (Integer)sourceToTrace.get(srcID);
    if (traceNum == null) {
      double retval = UiUtil.forceToGridValue(traceBoundary.getX() - (1.0 * TRACE_OFFSET_X_) + UiUtil.GRID_SIZE, UiUtil.GRID_SIZE);
      return (new Double(retval));
    }
    double retval = UiUtil.forceToGridValue(traceBoundary.getX() - (traceNum.doubleValue() * TRACE_OFFSET_X_), UiUtil.GRID_SIZE); 
    return (new Double(retval));
  }

  /***************************************************************************
  **
  ** Get the full y requirements for all the traces for a run
  */
  
  private double getClusterRunTraceYRequirements(Map clusterRunSpecialPads, int runNum, int maxTrace) {
    Integer runObj = new Integer(runNum);
    Map specialPads = (Map)clusterRunSpecialPads.get(runObj);
    int sum = 0;
    for (int i = 0; i < maxTrace; i++) {
      Integer specPad = (Integer)specialPads.get(new Integer(i));
      if (specPad != null) {
        sum += specPad.intValue();
      } else {
        sum++;
      }
    }
    return ((double)sum * PER_TRACE_);
  }    
 
  /***************************************************************************
  **
  ** Get the Y of a source trace for a given cluster run
  */
  
  private double getClusterRunTraceY(Map<Integer, Point2D> clusterRunStarts, Map clusterRunSpecialPads, 
                                     int runNum, int traceNum) {
    Integer runObj = new Integer(runNum);
    Point2D runBase = clusterRunStarts.get(runObj);
    Map specialPads = (Map)clusterRunSpecialPads.get(runObj);
    int sum = 0;
    for (int i = 0; i < traceNum; i++) {
      Integer specPad = (Integer)specialPads.get(new Integer(i));
      if (specPad != null) {
        sum += specPad.intValue();
      } else {
        sum++;
      }
    }
    double startY = runBase.getY() - ((double)sum * PER_TRACE_);
    return (startY);
  }  

  /***************************************************************************
  **
  ** Build the link segments for the given source
  */
  
  private SpecialtyLayoutLinkData buildLinkSegmentsToClusters(String srcID, Map<String, List<GeneAndSatelliteCluster>> srcToClusters,
                                                                Map clusterRunStarts,
                                                                Map clusterRunSpecialPads,
                                                                Map clusterToClusterRunNumber,
                                                                Map srcToTraceNumberPerClusterRun, 
                                                                Map sourceToTrace, Point2D traceBoundary, 
                                                                Genome genome, Layout lo,
                                                                Map placement, Map padChanges) {
    
    
    
    SpecialtyLayoutLinkData retval = new SpecialtyLayoutLinkData(srcID);
  
    //
    // Start at the nearest cluster and work outward
    //
    
    Map perSrc = (Map)srcToTraceNumberPerClusterRun.get(srcID);
    Double traceXObj = getMainTraceX(srcID, sourceToTrace, traceBoundary);
    double traceX = traceXObj.doubleValue();
    
    HashMap clustMap = new HashMap();
    ArrayList ordering = new ArrayList();
    List<GeneAndSatelliteCluster> clusts = srcToClusters.get(srcID);
    int num = clusts.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = clusts.get(i);
      clustMap.put(clust.getCoreID(), clust);
      int[] pos = (int[])clusterToClusterRunNumber.get(clust.getCoreID());
      if (pos != null) {
        TaggedByPair tbp = new TaggedByPair(clust.getCoreID(), pos[0], pos[1]);
        ordering.add(tbp);
      }
    }
    Collections.sort(ordering);
    
    
    //
    // Each cluster may have multiple traces inbound:
    //
    
    int onum = ordering.size();
    int currClustRun = Integer.MIN_VALUE;
    double runY = Double.NEGATIVE_INFINITY;
    
    GeneAndSatelliteCluster.LinkPlacementState lps = new GeneAndSatelliteCluster.LinkPlacementState();
    lps.isRoot = true;
    lps.runPt = null;
    lps.lastAttach = null;
    lps.lastRun = null;
    
    Point2D runBase = null;

    num = ordering.size();
    for (int i = 0; i < num; i++) {
      TaggedByPair clust = (TaggedByPair)ordering.get(i);
      GeneAndSatelliteCluster cluster = (GeneAndSatelliteCluster)clustMap.get(clust.id);
     
      lps.isRunStart = false;
      if (currClustRun != clust.val1) {
        currClustRun = clust.val1;
        lps.isRunStart = true;
        Integer traceNum = (Integer)perSrc.get(new Integer(currClustRun));
        runY = getClusterRunTraceY(clusterRunStarts, clusterRunSpecialPads, 
                                   currClustRun, traceNum.intValue());
      
        lps.runPt = new Point2D.Double(traceX, runY);
        runBase = (Point2D)clusterRunStarts.get(new Integer(clust.val1));
      }
     // Point2D clustPt = new Point2D.Double(runBase.getX() + ((double)clust.val2 * OFFSET_X_), runY); 
      MetaClusterPointSource mcps = new MetaClusterPointSource(srcID, null, MetaClusterPointSource.FOR_PURE_TARG);
      mcps.setTargetTraceY(runY);
      cluster.finalLinkRouting(srcID, sld_.nps, sld_.rcx, mcps, retval, lps);      
    }
   
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** For sorting by integers
  */  
  
  private static class TaggedByCount implements Comparable<TaggedByCount> {
    
    String id;
    int count;

    TaggedByCount(String id) {
      this.id = id;
      this.count = 0;
    }    
    
    TaggedByCount(String id, int count) {
      this.id = id;
      this.count = count;
    }
    
    public int compareTo(TaggedByCount other) {
      int diff = this.count - other.count;
      if (diff != 0) {
        return (diff);
      }
      return (this.id.compareTo(other.id));
    }
    
    public String toString() {
      return ("TaggedByCount: " + id + " : " + count);
    }
    
    void increment() {
      count++;
      return;
    }
    
  }
  
  /***************************************************************************
  **
  ** For sorting by pairs of integers
  */
  
  private static class TaggedByPair implements Comparable<TaggedByPair> {
    
    String id;
    int val1;
    int val2;

    TaggedByPair(String id) {
      this.id = id;
      this.val1 = 0;
      this.val2 = 0;
    }    
    
    TaggedByPair(String id, int val1, int val2) {
      this.id = id;
      this.val1 = val1;
      this.val2 = val2;      
    }
    
    public int compareTo(TaggedByPair other) {
      int diff1 = this.val1 - other.val1;
      if (diff1 != 0) {
        return (diff1);
      }
      int diff2 = this.val2 - other.val2;
      if (diff2 != 0) {
        return (diff2);
      }      
      return (this.id.compareTo(other.id));
    }
    
    public String toString() {
      return ("TaggedByPair: " + id + " : " + val1 + " , " + val2);
    }
  }
}
