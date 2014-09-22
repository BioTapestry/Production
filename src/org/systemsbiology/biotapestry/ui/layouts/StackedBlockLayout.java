/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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
import java.util.TreeMap;
import java.util.Vector;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Collections;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.analysis.NodeGrouper;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** Structured layout for stacked blocks
*/

public class StackedBlockLayout implements SpecialtyLayout {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC ENUMS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Target Ordering Options
  */ 
  
  public enum TargTypes {
    TARGS_BY_TIME("targByTime"),
    TARGS_BY_ALPHA("targByAlpha"),
    TARGS_BY_INPUTS("targByInputs"),
    TARGS_BY_SOURCE_ORDER("targBySource"),
    TARGS_BY_SOURCE_ORDER_NO_DEGREE("targBySourceNoDegree"),
    ;
  
    private String tag_;
    
    TargTypes(String tag) {
      this.tag_ = tag;  
    }
    
    public EnumChoiceContent<TargTypes> generateCombo(BTState appState) {
      return (new EnumChoiceContent<TargTypes>(appState.getRMan().getString("worksheetLayout." + tag_), this));
    }
  
    public static Vector<EnumChoiceContent<TargTypes>> getChoices(BTState appState, boolean forSubset) {
      Vector<EnumChoiceContent<TargTypes>> retval = new Vector<EnumChoiceContent<TargTypes>>();
      Database db = appState.getDB();
      TimeCourseData tcd = db.getTimeCourseData();    
      boolean allowByTime = tcd.haveData();
      for (TargTypes tt: values()) {
        if ((tt == TARGS_BY_TIME) && !allowByTime) {
          continue;
        }
        if (((tt == TARGS_BY_SOURCE_ORDER) || (tt == TARGS_BY_SOURCE_ORDER_NO_DEGREE)) && forSubset) {
          continue;
        }
        retval.add(tt.generateCombo(appState));    
      }
      return (retval);
    }
  }

  /***************************************************************************
  **
  ** Source Ordering Options
  */  
  
  public enum SrcTypes {
    SRCS_BY_TIME("srcsByTime"),
    SRCS_BY_SORT("srcsBySort"),
    SRCS_BY_CURR_POSITION("srcsByCurrPos");
  
    private String tag_;
    
    SrcTypes(String tag) {
      this.tag_ = tag;  
    }
    
    public EnumChoiceContent<SrcTypes> generateCombo(BTState appState) {
      return (new EnumChoiceContent<SrcTypes>(appState.getRMan().getString("worksheetLayout." + tag_), this));
    }
  
    public static Vector<EnumChoiceContent<SrcTypes>> getChoices(BTState appState, boolean isForGlobal) {
      Vector<EnumChoiceContent<SrcTypes>> retval = new Vector<EnumChoiceContent<SrcTypes>>();
      Database db = appState.getDB();
      TimeCourseData tcd = db.getTimeCourseData();    
      boolean allowByTime = tcd.haveData();
      for (SrcTypes st: values()) {
        if ((st == SRCS_BY_TIME) && !allowByTime) {
          continue;
        }
        //
        // FIX ME??
        //
        // Releasing for V6, this seems to be arbitrary?  Why is this not
        // allowed?  Don't change it at the last minute, but think hard as
        // to why this was restricted!  12/07/12 WJRL: I have a vague recollection
        // of there being a REALLY good reason for this!
        //
        if ((st == SRCS_BY_CURR_POSITION) && isForGlobal) {
          continue;
        }
        retval.add(st.generateCombo(appState));    
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Answer if source positioning overrides other parameters
    */
     
    public boolean srcPositioningOverrides() {
      return (this == SRCS_BY_CURR_POSITION);
    }   
  }
  
  /***************************************************************************
  **
  * Compression Options
   */  
   
   public enum CompressTypes {
     COMPRESS_NORMAL("compressNormal"),
     COMPRESS_PROVIDE_STRING_SPACE("compressStringWide"), 
     COMPRESS_NARROW_TRACES("compressTrace"),
     COMPRESS_FULL_SQUEEZE("compressFull");
   
     private String tag_;
     
     CompressTypes(String tag) {
       this.tag_ = tag;  
     }
     
     public EnumChoiceContent<CompressTypes> generateCombo(BTState appState) {
       return (new EnumChoiceContent<CompressTypes>(appState.getRMan().getString("stackedLayout." + tag_), this));
     }
   
     public static Vector<EnumChoiceContent<CompressTypes>> getChoices(BTState appState, boolean isForGlobal) {
       Vector<EnumChoiceContent<CompressTypes>> retval = new Vector<EnumChoiceContent<CompressTypes>>();
       for (CompressTypes ct: values()) {
         if ((ct == COMPRESS_FULL_SQUEEZE) && !isForGlobal) {
           continue;
         }
         retval.add(ct.generateCombo(appState));    
       }
       return (retval);
     }
   } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private SpecialtyLayoutData sld_;  
  private StackedClusterSeries stacked_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public StackedBlockLayout(BTState appState) {
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
    StackedBlockLayout retval = new StackedBlockLayout(appState_);
    retval.sld_ = sld;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the menu name for the layout
  */
  
  public String getMenuNameTag() {
    return ("command.StackedBlockLayout");
  }  
     
  /***************************************************************************
  **
  ** Get the mnemonic string for the layout
  */
  
  public String getMenuMnemonicTag() {
    return ("command.StackedBlockLayoutMnem");
  }  
   
  /***************************************************************************
  **
  ** Answer if the given selection is valid
  */
  
  public boolean selectionIsValid(Genome genome, String selected) {
    // We don't care.  Nobody is valid.
    return (false);
  }    
  
  /***************************************************************************
  **
  ** Answer if the network topology can be handled by this layout
  */
  
  public int topologyIsHandled(DataAccessContext rcx) {  
    return (TOPOLOGY_HANDLED);  // Everybody is handled
  }
  
  /***************************************************************************
  **
  ** Answer if the setup works OK.  Error message if a problem, else null if OK
  */
  
  public String setUpIsOK() { 
    
    StackedBlockLayoutParams wlp = (StackedBlockLayoutParams)sld_.param;
    if (wlp.grouping != SrcTypes.SRCS_BY_CURR_POSITION) {
      return (null);
    }
    
    GASCResults clustResults = prepClusters();
    if (clustResults == null) {
      return (null);
    }
    ArrayList<GeneAndSatelliteCluster> allClusters = new ArrayList<GeneAndSatelliteCluster>(clustResults.srcClusters);
    allClusters.addAll(clustResults.trgClusters);    
    
    HashSet<String> gascIDs = new HashSet<String>();
    int numAC = allClusters.size();
    for (int i = 0; i < numAC; i++) {
      GeneAndSatelliteCluster sc = allClusters.get(i); 
      gascIDs.add(sc.getCoreID());        
    }
     
    StackGenerator sg = new StackGenerator();
    SortedMap<Integer, List<String>> clust = sg.buildStackOrder(gascIDs, sld_.rcx);
    if (clust == null) {
      return (appState_.getRMan().getString("stackedLayout.ambiguousRows"));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Prepare node cluster results
  */
  
  private GASCResults prepClusters() {

    Genome baseGenome = sld_.genome;
    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
    TreeSet<String> nodeSet = new TreeSet<String>(cc);  
    nodeSet.addAll(DataUtil.setFromIterator(sld_.subset.getNodeIterator()));   
    if (nodeSet.isEmpty()) {
      return (null);
    }
    
    TreeSet<Link> linkSet = new TreeSet<Link>();    
    
    //
    // One expects that a layout of a region with all the same elements as the Full Genome model
    // will match the stacked layout of the full model.  But that depends completely on how we
    // break cycles, and thus the order if the linkages.  So order them!  Since in the GenomeInstance
    // the linkIDs have ":#" suffixes, this should match for a single-region only layout.  Beyond
    // that, we will have to get smarter.
    //
 
    TreeSet<String> orderedLinkSet = new TreeSet<String>(cc);    
    orderedLinkSet.addAll(DataUtil.setFromIterator(sld_.subset.getLinkageIterator()));
    Iterator<String> lit = orderedLinkSet.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next(); 
      Linkage link = baseGenome.getLinkage(linkID);
      String trg = link.getTarget();
      if ((sld_.pureTargets != null) && sld_.pureTargets.contains(trg)) {
        continue;
      }
      String src = link.getSource();
      Link cfl = new Link(src, trg);
      linkSet.add(cfl);
      CycleFinder cf = new CycleFinder(nodeSet, linkSet);
      if (cf.hasACycle()) {
        linkSet.remove(cfl);
      }      
    }
    
    //
    // Figure out the node groupings:
    //

    GraphSearcher gs = new GraphSearcher(nodeSet, linkSet);    
    Map<String, Integer> queue = gs.topoSort(false);
    List<String> nodeRanking = gs.topoSortToPartialOrdering(queue);
    Collections.reverse(nodeRanking);
    NodeGrouper ng = new NodeGrouper(sld_.subset, nodeRanking);
    Map<String, NodeGrouper.GroupElement> groups = ng.buildGroups();
    
    //
    // Figure out if we have pure core groups (i.e. no fan-in or fan-out nodes).
    // This happens with e.g. SIF input files. We can skip steps if that is the case.
    //
    
    sld_.nps.pureCoreNetwork = true;
    Iterator<String> gkit = groups.keySet().iterator();
    while (gkit.hasNext()) {
      String nodeID = gkit.next();
      NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
      if (nodeInfo.side != NodeGrouper.GroupElement.Sides.CORE) {
        sld_.nps.pureCoreNetwork = false;
        break;
      }
    }
    
    StackedBlockLayoutParams wlp = (StackedBlockLayoutParams)sld_.param;
    double traceOffset = UiUtil.GRID_SIZE * wlp.traceMult();
    
    List<String> ttGroups = ng.findTerminalTargetsByGroups(groups);
    List<GeneAndSatelliteCluster> termXClusters = GeneAndSatelliteCluster.fillTargetClustersByGroups(sld_.appState, ttGroups, groups,                                                              
                                                                                                     sld_.nps, sld_.rcx, traceOffset, 
                                                                                                     false, wlp.spaceForText());
    ArrayList<GeneAndSatelliteCluster> sClustList = new ArrayList<GeneAndSatelliteCluster>();
    findSources(baseGenome, termXClusters, groups, sClustList, wlp.spaceForText(), traceOffset);
    return (new GASCResults(sClustList, termXClusters, queue));
  }
     
  /***************************************************************************
  **
  ** Figure out the node positions
  */
  
  public void layoutNodes(BTProgressMonitor monitor) throws AsynchExitRequestException {

    sld_.results.setOrigNodeBounds(sld_.subset.getOrigBounds());
    sld_.results.setOrigModuleRect(sld_.subset.getOrigModuleBounds());
    sld_.results.setModuleID(sld_.subset.getModuleID()); // Will be non-null if layout is overlay-driven
 
    StackedBlockLayoutParams wlp = (StackedBlockLayoutParams)sld_.param;

    SrcTypes grouping = wlp.grouping;
    int rowSize = wlp.rowSize;
    double traceOffset = UiUtil.GRID_SIZE * wlp.traceMult();
   
    Set<String> nodeSet = DataUtil.setFromIterator(sld_.subset.getNodeIterator());   
    if (nodeSet.isEmpty()) {
      sld_.results = null;
      return;
    }
   
    GASCResults clustResults = prepClusters();
        
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }     
    
    //
    // User can choose to intersperse pure targets with sources, or keep them apart: 
    //
    
    ArrayList<GeneAndSatelliteCluster> allClusters = new ArrayList<GeneAndSatelliteCluster>(clustResults.srcClusters);
    if (!wlp.targsBelow || (grouping == SrcTypes.SRCS_BY_CURR_POSITION)) {
      allClusters.addAll(clustResults.trgClusters);    
    }
    int numClust = allClusters.size();
    
    RowBuilder builder = new RowBuilder(sld_.appState);
    SortedMap<Integer, List<GeneAndSatelliteCluster>> clustRows;
  
    if (grouping == SrcTypes.SRCS_BY_CURR_POSITION) {      
      HashSet<String> gascIDs = new HashSet<String>();
      int numAC = allClusters.size();
      for (int i = 0; i < numAC; i++) {
        GeneAndSatelliteCluster sc = allClusters.get(i); 
        gascIDs.add(sc.getCoreID());        
      }      
      StackGenerator sg = new StackGenerator(); 
      SortedMap<Integer, List<String>> clust = sg.buildStackOrder(gascIDs, sld_.rcx);
      if (clust == null) {
        throw new IllegalStateException();
      }
      clustRows = builder.buildRowsByAssignment(allClusters, clust);    
    } else if (grouping == SrcTypes.SRCS_BY_TIME) {
      clustRows = builder.assignRowsByTime(allClusters, rowSize, null);
    } else if (grouping == SrcTypes.SRCS_BY_SORT) {
      clustRows = builder.buildClusterSeriesBySort(allClusters, clustResults.queue, rowSize, null);
    } else {
      throw new IllegalArgumentException();
    }
  
    //
    // Find target cluster assignment based on...:
    //
      
    if (wlp.targsBelow && (grouping != SrcTypes.SRCS_BY_CURR_POSITION)) {
      TargTypes targGroups = wlp.targGrouping;
      Integer nextKey = (clustRows.isEmpty()) ? null : new Integer(clustRows.lastKey().intValue() + 1);
      if (targGroups == TargTypes.TARGS_BY_TIME) {
        clustRows.putAll(builder.assignRowsByTime(clustResults.trgClusters, rowSize, nextKey));
      } else if (targGroups == TargTypes.TARGS_BY_ALPHA) {
        clustRows.putAll(builder.assignRowsByAlpha(clustResults.trgClusters, sld_.nps, rowSize, nextKey));
      } else if (targGroups == TargTypes.TARGS_BY_INPUTS) {
        clustRows.putAll(builder.assignRowsByInputs(clustResults.trgClusters, sld_.nps, rowSize, true, nextKey));
 
      } else if ((targGroups == TargTypes.TARGS_BY_SOURCE_ORDER) || 
                 (targGroups == TargTypes.TARGS_BY_SOURCE_ORDER_NO_DEGREE)) {        
        boolean ignoreDegree = (targGroups == TargTypes.TARGS_BY_SOURCE_ORDER_NO_DEGREE);
        // These two modes require us to know the order of the source nodes so that we can place the target nodes. Since
        // the actual final layout occurs in one step, we need to hack the process by making a copy of the existing
        // source nodes, doing the full layout process, recording the order that results, then throwing the laid out
        // copy away and just using the order results. Not the most efficient, but the easiest to implement at this
        // point...
        //
        StackedClusterSeries bogoStacked = new StackedClusterSeries(traceOffset);
        SortedMap<Integer, List<GeneAndSatelliteCluster>> clustRowsCopy = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
        for (Integer key : clustRows.keySet()) {
          List<GeneAndSatelliteCluster> perRow = clustRows.get(key);
          List<GeneAndSatelliteCluster> perRowCopy = new ArrayList<GeneAndSatelliteCluster>();
          clustRowsCopy.put(key, perRowCopy);
          int numPerRow = perRow.size();
          for (int i = 0; i < numPerRow; i++) {
            GeneAndSatelliteCluster gasc = perRow.get(i);
            perRowCopy.add(i, gasc.clone());          
          }
        }
        int bogoRows = clustRowsCopy.size();
        SpecialtyLayoutEngine.NodePlaceSupport bogoCopy = new SpecialtyLayoutEngine.NodePlaceSupport(sld_.nps);
        bogoStacked.prep(bogoRows, clustRowsCopy, sld_.subset, bogoCopy, sld_.rcx, wlp.traceMult(), sld_.existingOrder);
        List<String> srcOrder = new ArrayList<String>(bogoStacked.getSourceOrder().values());
        // And we do not care anymore about the bogo layout; we have the srcOrder we need. Actual full layout occurs below...
        clustRows.putAll(builder.assignRowsBySourceOrder(clustResults.trgClusters, sld_.nps, rowSize, true, nextKey, ignoreDegree, srcOrder));    
      } else {
        throw new IllegalArgumentException();
      }
      allClusters.addAll(clustResults.trgClusters);
      numClust = allClusters.size();
    }
    
    int numRows = clustRows.size();

    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }       
        
    stacked_ = new StackedClusterSeries(traceOffset);
    stacked_.prep(numRows, clustRows, sld_.subset, sld_.nps, sld_.rcx, wlp.traceMult(), sld_.existingOrder);
    sld_.results.setSourceOrder(stacked_.getSourceOrder());

    //
    // Center is not necessarily on the grid.  Force it:
    //
       
    Point2D center = (Point2D)sld_.subset.getPreferredCenter().clone();
    UiUtil.forceToGrid(center, UiUtil.GRID_SIZE);
    LayoutCompressionFramework lcf = (wlp.compressPerRow()) ? new LayoutCompressionFramework() : null;
    stacked_.locate(center, sld_.nps, sld_.rcx, lcf);
    sld_.results.lcf = lcf;

    //
    // With stack placed, get bounds.  THESE ARE NOT JUST NODE BOUNDS!  Includes some core linkages,
    // do NOT consider equal to orig node bounds!
    //
    
    sld_.results.setPlacedStackBounds(stacked_.getStackBounds());
    sld_.results.setPlacedStackNodeOnlyBounds(stacked_.getStackNodeOnlyBounds());
     
    //
    // Get the orientation up to speed:
    //
    
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster tc = allClusters.get(i); 
      tc.calcOrientChanges(sld_.nps, sld_.lo, sld_.results.orientChanges);
    } 

    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    sld_.gASCs = allClusters;
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
    
    //
    // Get the links handled:
    //
    
    stacked_.routeLinksForStack(sld_.subset, sld_.results, sld_.nps, sld_.rcx, gsles, stacked_.getStackBounds());
        
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
   
    return;
  } 
  
  /***************************************************************************
  **
  ** Assign colors (Not really; actual color calcs occur in LayoutLinkSupport.doColorOperations()
  ** inside LayoutLinkSupport.runSpecialtyLinkPlacement());
  */
  
  public void assignColors(BTProgressMonitor monitor) throws AsynchExitRequestException {  
    if (sld_.results == null) {
      return;
    }   
    sld_.results.colorStrategy = sld_.param.getColorDecision();
    sld_.results.bubblesOn = sld_.param.showBubbles();
    sld_.results.checkColorOverlap = sld_.param.checkColorOverlaps();
    
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
    SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType lot = SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType.STACKED; 
    return (new SpecialtyLayoutEngineParamDialogFactory.BuildArgs(appState_, genome, lot, forSubset));
  }
  
  /***************************************************************************
  **
  ** Get the undo string for the layout
  */
  
  public String getUndoString() {
    return ("undo.stackedBlockLayout");
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
  
  public static StackedBlockLayoutParams getDefaultParams(boolean forSubset) {
    StackedBlockLayoutParams retval = new StackedBlockLayoutParams();
    retval.targsBelow = true;
    retval.grouping = SrcTypes.SRCS_BY_SORT;
    retval.targGrouping = TargTypes.TARGS_BY_INPUTS;
    retval.rowSize = 10;
    retval.assignColorMethod = (forSubset) ? ColorTypes.KEEP_COLORS : ColorTypes.COLOR_BY_CYCLE;
    retval.overlayOption = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    retval.compressType = CompressTypes.COMPRESS_NORMAL;
    retval.showBubbles = true;
    retval.checkColorOverlap = false;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle case where time course data has disappeared; we need to go back to
  ** default values:
  */
  
  public static void forceParamsAsNeeded(BTState appState, StackedBlockLayoutParams params) {
    TimeCourseData tcd = appState.getDB().getTimeCourseData();    
    boolean allowByTime = tcd.haveData();
    if (!allowByTime) {
      if (params.targGrouping == TargTypes.TARGS_BY_TIME) {
        params.targGrouping = TargTypes.TARGS_BY_INPUTS;
      } 
      if (params.grouping == SrcTypes.SRCS_BY_TIME) {
        params.grouping = SrcTypes.SRCS_BY_SORT;
      } 
      
    }
    return;
  } 
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static class GASCResults {
    List<GeneAndSatelliteCluster> srcClusters;
    List<GeneAndSatelliteCluster> trgClusters;
    Map<String, Integer> queue;
    
    GASCResults(List<GeneAndSatelliteCluster> sClustList, List<GeneAndSatelliteCluster> termXClusters, Map<String, Integer> queue) {
      srcClusters = sClustList;
      trgClusters = termXClusters; 
      this.queue = queue;
    }   
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class StackedBlockLayoutParams implements SpecialtyLayoutEngineParams, Cloneable {
    public boolean targsBelow;
    public SrcTypes grouping;
    public TargTypes targGrouping;
    public int rowSize;
    public ColorTypes assignColorMethod;
    public boolean showBubbles;
    public boolean checkColorOverlap;
    public int overlayOption;
    public CompressTypes compressType;
    
    public StackedBlockLayoutParams clone() {
      try {
        StackedBlockLayoutParams retval = (StackedBlockLayoutParams)super.clone();
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

    public boolean spaceForText() {
      return (compressType == CompressTypes.COMPRESS_PROVIDE_STRING_SPACE);
    }
       
    public int traceMult() {
      if ((compressType == CompressTypes.COMPRESS_NARROW_TRACES) || (compressType == CompressTypes.COMPRESS_FULL_SQUEEZE)) {
        return (1);
      } else {
        return (2);
      }
    } 
    
    public boolean compressPerRow() {
      return (compressType == CompressTypes.COMPRESS_FULL_SQUEEZE);
    }  
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Find the nodes that are sources.
  */
  
  private SortedSet<String> findSources(Genome genome, List<GeneAndSatelliteCluster> termClusters, 
                                        Map<String, NodeGrouper.GroupElement> groups, List<GeneAndSatelliteCluster> sClustList, 
                                        boolean textToo, double traceOffset) {

    //
    // Get the set of targets:
    //
    
    HashSet<String> targets = new HashSet<String>();
    int numTerm = termClusters.size();
    for (int i = 0; i < numTerm; i++) {
      GeneAndSatelliteCluster clust = termClusters.get(i);
      targets.add(clust.getCoreID());
    }
      
    //
    // If not a target, you are a source:
    //
    
    TreeSet<String> sources = new TreeSet<String>();
    Iterator<NodeGrouper.GroupElement> gvit = groups.values().iterator();
    while (gvit.hasNext()) {
      NodeGrouper.GroupElement groupCore = gvit.next();
      if (!targets.contains(groupCore.group)) {
        sources.add(groupCore.group);
      }
    }
     
    Iterator<String> psit = sources.iterator();
    while (psit.hasNext()) {
      String srcID = psit.next();
      GeneAndSatelliteCluster sc = new GeneAndSatelliteCluster(sld_.appState, srcID, true, traceOffset, false, textToo);
      sc.prepFromGroupsPhaseOne(sld_.nps, groups, false);
      sClustList.add(sc);
    }    

    return (sources);
  }
}
