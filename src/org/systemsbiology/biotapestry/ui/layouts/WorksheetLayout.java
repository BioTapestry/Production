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
import java.util.Vector;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
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
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

/****************************************************************************
**
** Structured layout for interaction worksheet networks
*/

public class WorksheetLayout implements SpecialtyLayout {
  
  /***************************************************************************
  **
  ** Target Ordering Options
  */ 
   
  public enum TargTypes {
    TARGS_BY_TIME("targByTime"),
    TARGS_BY_ALPHA("targByAlpha"),
    TARGS_BY_INPUTS("targByInputs"),
    TARGS_BY_SOURCE_ORDER("targBySource"),
    TARGS_BY_SOURCE_ORDER_NO_DEGREE("targBySourceNoDegree");
   
    private String tag_;
     
    TargTypes(String tag) {
      this.tag_ = tag;  
    }
     
    public EnumChoiceContent<TargTypes> generateCombo(BTState appState) {
      return (new EnumChoiceContent<TargTypes>(appState.getRMan().getString("worksheetLayout." + tag_), this));
    }
   
    public static Vector<EnumChoiceContent<TargTypes>> getChoices(BTState appState) {
      Vector<EnumChoiceContent<TargTypes>> retval = new Vector<EnumChoiceContent<TargTypes>>();
      TimeCourseData tcd = appState.getDB().getTimeCourseData();    
      boolean allowByTime = tcd.haveData();
      for (TargTypes tt: values()) {
        if ((tt == TARGS_BY_TIME) && !allowByTime) {
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
    SRCS_BY_SINGLE("srcsBySingle");
   
    private String tag_;
     
    SrcTypes(String tag) {
      this.tag_ = tag;  
    }
     
    public EnumChoiceContent<SrcTypes> generateCombo(BTState appState) {
      return (new EnumChoiceContent<SrcTypes>(appState.getRMan().getString("worksheetLayout." + tag_), this));
    }
   
    public static Vector<EnumChoiceContent<SrcTypes>> getChoices(BTState appState) {
      Vector<EnumChoiceContent<SrcTypes>> retval = new Vector<EnumChoiceContent<SrcTypes>>();
      TimeCourseData tcd = appState.getDB().getTimeCourseData();    
      boolean allowByTime = tcd.haveData();
      for (SrcTypes st: values()) {
        if ((st == SRCS_BY_TIME) && !allowByTime) {
          continue;
        }
        if (st == SRCS_BY_SINGLE) {
          continue;
        }
        retval.add(st.generateCombo(appState));    
      }
      return (retval);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private static final double INTER_TARGET_PAD_ = 100.0;  
  private static final double LAST_TRACK_TO_TARGET_PAD_ = 200.0;
  private static final double LAST_CLUST_TO_TRACK_PAD_ = 20.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final SrcTypes SINGLE_CLUSTER_MODE = SrcTypes.SRCS_BY_SINGLE; 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private boolean forDiagonal_;    
  private SpecialtyLayoutData sld_;
  
  private ClusterSeries series_;
  private TreeMap<Integer, Set<String>> tracesPerRow_;
  private HashMap<Integer, Double> basePerRow_;
  private int numRows_;
  private List<GeneAndSatelliteCluster> termClusters_;
  private double rightBound_;
  
  private TreeMap<Integer, List<GeneAndSatelliteCluster>> clustPerRow_;
  private double rightmostTrack_;
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

  public WorksheetLayout(BTState appState, boolean forDiagonal) {
    appState_ = appState;
    forDiagonal_ = forDiagonal;
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
    WorksheetLayout retval = new WorksheetLayout(appState_, forDiagonal_);
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
    return ((forDiagonal_) ? "command.WorksheetDiagLayout" : "command.WorksheetLayout");
  }  
     
  /***************************************************************************
  **
  ** Get the mnemonic string for the layout
  */
  
  public String getMenuMnemonicTag() {
    return ((forDiagonal_) ? "command.WorksheetDiagLayoutMnem" : "command.WorksheetLayoutMnem");
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
  ** Figure out the node positions
  */
  
  public void layoutNodes(BTProgressMonitor monitor) throws AsynchExitRequestException {

    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
    TreeSet<String> nodeSet = new TreeSet<String>(cc);  
    nodeSet.addAll(DataUtil.setFromIterator(sld_.subset.getNodeIterator()));   
    if (nodeSet.isEmpty()) {
      return;
    }
    TreeSet<Link> linkSet = new TreeSet<Link>();

    WorksheetLayoutParams wlp = (WorksheetLayoutParams)sld_.param;
 
    TargTypes targGroups = wlp.targGroups;
    int targSize = wlp.targSize;
    SrcTypes srcGroups = wlp.srcGroups;
    int srcSize = wlp.srcSize;
    double traceOffset = wlp.traceMult * UiUtil.GRID_SIZE;
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    } 
      
    TreeSet<String> orderedLinkSet = new TreeSet<String>(cc);    
    orderedLinkSet.addAll(DataUtil.setFromIterator(sld_.subset.getLinkageIterator()));
    Iterator<String> lit = orderedLinkSet.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = sld_.nps.getLinkage(linkID);
      String trg = link.getTarget();
      if ((sld_.pureTargets != null) && sld_.pureTargets.contains(trg)) {
        continue;
      }
      String src = link.getSource();
      Link cfl = new Link(src, trg);
      linkSet.add(cfl);
      CycleFinder cf = new CycleFinder(sld_.nodeSet, linkSet);
      if (cf.hasACycle()) {
        linkSet.remove(cfl);
      }      
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
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
 
    //
    // Find terminal clusters
    //
    
    List<String> ttGroups = ng.findTerminalTargetsByGroups(groups);
    int numTClust = ttGroups.size();
    termClusters_ = GeneAndSatelliteCluster.fillTargetClustersByGroups(sld_.appState, ttGroups, groups, sld_.nps, sld_.rcx,
                                                                       traceOffset, true, false, false);
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }       
    
    //
    // Find target cluster assignment based on...:
    //       
    
    RowBuilder builder = new RowBuilder(sld_.appState);
    SortedMap<Integer, List<GeneAndSatelliteCluster>> tClustRows = null;
    boolean deferred = false;
    if (targGroups == TargTypes.TARGS_BY_TIME) {
      tClustRows = builder.assignRowsByTime(termClusters_, targSize, null);
    } else if (targGroups == TargTypes.TARGS_BY_ALPHA) {
      tClustRows = builder.assignRowsByAlpha(termClusters_, sld_.nps, targSize, null);
    } else if (targGroups == TargTypes.TARGS_BY_INPUTS) {
      tClustRows = builder.assignRowsByInputs(termClusters_, sld_.nps, targSize, true, null);
    } else if ((targGroups == TargTypes.TARGS_BY_SOURCE_ORDER) || 
               (targGroups == TargTypes.TARGS_BY_SOURCE_ORDER_NO_DEGREE)) {
      deferred = true;
    } else {
      throw new IllegalArgumentException();
    }
    
    //
    // Get the listing of sources. 
    //
    
    ArrayList<GeneAndSatelliteCluster> sClustList = new ArrayList<GeneAndSatelliteCluster>();
    findSources(sld_.nps, termClusters_, groups, sClustList, traceOffset, wlp.textToo);
    int numSClust = sClustList.size(); 

    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    } 
    
    //
    // Find cluster assignment based on...:
    //
        
    series_ = new ClusterSeries(false, wlp.traceMult);
    if (srcGroups == SrcTypes.SRCS_BY_TIME) {
      series_ = buildClusterSeriesByTime(sClustList, wlp.traceMult);
    } else if (srcGroups == SrcTypes.SRCS_BY_SORT) {
      series_ = buildClusterSeriesBySort(sClustList, queue, srcSize, wlp.traceMult);
    } else if (srcGroups == SrcTypes.SRCS_BY_SINGLE) {
      series_ = buildClusterSeriesBySingle(sClustList, wlp.traceMult);
    }
    
    //
    // Glue in pre and post nodes that feed into and out of the
    // subset:
    //
    
    series_.addPreAndPost(sld_.subset);
         
    //
    // Build up the cluster series, then finish preparing the GASClusters:
    
    series_.prepForSingle(sld_.subset, sld_.nps, sld_.rcx);

    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }   
    
    //
    // Center is not necessarily on the grid.  Force it:
    //
    
    Point2D center = (Point2D)sld_.subset.getPreferredCenter().clone();
    UiUtil.forceToGrid(center, UiUtil.GRID_SIZE);
    
    //
    // Get the cluster series laid out:
    //
    
    Point2D rightEnd = series_.locate(center, sld_.nps, sld_.rcx, null); 
    List<String> srcOrder = series_.getSourceTrackOrder();
    if (deferred) {    
      boolean ignoreDegree = (targGroups == TargTypes.TARGS_BY_SOURCE_ORDER_NO_DEGREE);
      tClustRows = builder.assignRowsBySourceOrder(termClusters_, sld_.nps, targSize, true, null, ignoreDegree, srcOrder);
    }
    TreeMap<Integer, String> so = new TreeMap<Integer, String>();
    Iterator<String> soit = srcOrder.iterator();
    int count = 0;
    while (soit.hasNext()) {
      so.put(new Integer(count++), soit.next());
    }
    sld_.results.setSourceOrder(so);
    
    numRows_ = tClustRows.size();
    double rightX = rightEnd.getX();    
    int trackCount = series_.trackCount();
    rightmostTrack_ = rightX + LAST_CLUST_TO_TRACK_PAD_;
    if (numRows_ > 1) {
      rightmostTrack_ += (trackCount * 20.0);
    } else {
      int rmlo = series_.getRightmostOutboundLinkOrderMax();
      rightmostTrack_ += (rmlo * 20.0);
    }
    
    //
    // Lay out targets a row at a time:
    //
    
    clustPerRow_ = new TreeMap<Integer, List<GeneAndSatelliteCluster>>();
    tracesPerRow_ = new TreeMap<Integer, Set<String>>();
    basePerRow_ = new HashMap<Integer, Double>();
    rightBound_ = placeTargetClusterNodes(sld_.subset, series_, 
                                          tClustRows, clustPerRow_, 
                                          tracesPerRow_, basePerRow_, traceOffset, rightmostTrack_, monitor);
    
    //
    // Reorder pads and get new target sizes as needed:
    //
       
    srcOrder = series_.getSourceTrackOrder(); 
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = termClusters_.get(i); 
      tc.calcPadChanges(sld_.subset, srcOrder, sld_.nps, sld_.results.lengthChanges, sld_.results.extraGrowthChanges);
    }
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }     
        
    //
    // Get the orientation up to speed:
    //
    
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = termClusters_.get(i); 
      tc.calcOrientChanges(sld_.nps, sld_.lo, sld_.results.orientChanges);
    }
    for (int i = 0; i < numSClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i);
      sc.calcOrientChanges(sld_.nps, sld_.lo, sld_.results.orientChanges);
    }       
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }

    sld_.gASCs.addAll(termClusters_);
    sld_.gASCs.addAll(sClustList); 
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
    // Figure out the traces into each row, per source:
    //

    Map<Integer, Map<String, Double>> traceOrderPerRow = calcTracesPerRow(series_, tracesPerRow_, basePerRow_, numRows_, monitor);
       
    //
    // Time to start routing the links!  Get the series up to speed first.  Pad changes happen here for source GASes:
    //
    
    HashMap<String, SuperSrcRouterPointSource> traceDefs = new HashMap<String, SuperSrcRouterPointSource>();
    series_.routeLinks(sld_.subset, sld_.nps, sld_.rcx, sld_.results.lengthChanges, 
                       sld_.results.extraGrowthChanges, sld_.results, traceDefs);
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    } 
      
 
    //
    // Targets need internal link routing:
    //
    
    targetInternalRouting(termClusters_, sld_.results, monitor); 
    
    //
    // Lay out target links:
    //
    
    layoutTargetLinks(sld_.subset, series_, traceDefs, 
                      sld_.results, clustPerRow_, traceOrderPerRow, gsles.getInvertedSrcTrg(), numRows_, 
                      rightmostTrack_, rightBound_, monitor);
    
    
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
    sld_.results.colorStrategy = sld_.param.getColorDecision();
    sld_.results.bubblesOn = sld_.param.showBubbles();
    sld_.results.checkColorOverlap = sld_.param.checkColorOverlaps();
    return;
  }
    
  /***************************************************************************
  **
  ** Layout links to target clusters:
  */
  
  private void layoutTargetLinks(GenomeSubset subset, ClusterSeries series,
                                 Map<String, SuperSrcRouterPointSource> traceDefs, 
                                 SpecialtyInstructions si, 
                                 SortedMap<Integer, List<GeneAndSatelliteCluster>> clustPerRow,
                                 Map<Integer, Map<String, Double>> traceOrderPerRow, InvertedSrcTrg ist,
                                 int numRows, double rightmostTrack, double rightBound, BTProgressMonitor monitor) throws AsynchExitRequestException {
  
    //
    // We do this one source at a time:
    //
    
    Iterator<String> lmpkit = traceDefs.keySet().iterator();
    while (lmpkit.hasNext()) {
      String srcID = lmpkit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      
      //
      // For each source, get the point source and the link result list:
      //
      
      SuperSrcRouterPointSource lmp = traceDefs.get(srcID);
      if (lmp.getTrace() == null) {
        continue;
      }
      
      SpecialtyLayoutLinkData sin = si.getLinkPointsForSrc(srcID);
      if (sin == null) {  // First point for this source...
        sin = new SpecialtyLayoutLinkData(srcID);
        si.setLinkPointsForSrc(srcID, sin);
      }
      
      //
      // Build up the meta source for the target:
      //

      MetaClusterPointSource lps = new MetaClusterPointSource(srcID, lmp, MetaClusterPointSource.FOR_PURE_TARG);     
      int tracknum = series.getTrackIndexForSource(srcID);
      
      //
      // Get the target drop root calculated:
      //
      
      // Note that the lowest numbered tracknum ends up furthest to the right:     
      double tdrX = (numRows > 1) ? rightmostTrack - (tracknum * 20.0) : rightmostTrack;
      lps.initializeTargetDropRoot(new Point2D.Double(tdrX, lmp.getTrace().getY()));

      //
      // Crank thru each row of target clusters:
      //
      
      Iterator<Integer> cphit = clustPerRow.keySet().iterator();
      
      if (clustPerRow.isEmpty()) { 
        double yVal = lmp.getTrace().getY(); 
        lps.initTargetDropRow(yVal, (numRows == 1));
        lps.setTargetTraceY(yVal);  // FIX ME: combine with above
        Set<String> outLinks = subset.getLinksHeadedOutForSource(srcID);
        if (!outLinks.isEmpty()) {
          handleExternalTargetLinks(outLinks, sin, lps, rightBound);
        }          
      } else {
        while (cphit.hasNext()) {
          Integer rowNum = cphit.next();
          double yVal = 0.0;
          //
          // If we have just one target row, we just use trace values directly:
          //
          if (numRows > 1) {
            Map<String, Double> srcToY = traceOrderPerRow.get(rowNum);
            if (srcToY == null) {
              continue;
            }
            Double srcY = srcToY.get(srcID);
            if (srcY == null) {
              continue;
            }
            yVal = srcY.doubleValue();
          } else {
            yVal = lmp.getTrace().getY(); 
          }

          //
          // Update the MetaClusterPointSource for the new row:
          //

          lps.initTargetDropRow(yVal, (numRows == 1));
          lps.setTargetTraceY(yVal);  // FIX ME: combine with above

          //
          // For each cluster in the row, do the link routing:
          //

          List<GeneAndSatelliteCluster> perRow = clustPerRow.get(rowNum);
          int numPerRow = perRow.size();
          for (int i = 0; i < numPerRow; i++) {
            if ((monitor != null) && !monitor.keepGoing()) {
              throw new AsynchExitRequestException();
            } 
            GeneAndSatelliteCluster tc = perRow.get(i);
            tc.inboundLinkRouting(srcID, sld_.nps, sld_.rcx, sin, lps, false);
          }

          if (rowNum.intValue() == 0) {
            Set<String> outLinks = subset.getLinksHeadedOutForSource(srcID);
            if (!outLinks.isEmpty()) {
              handleExternalTargetLinks(outLinks, sin, lps, rightBound);
            }          
          }

          lps.closeOutTargetDropRow();
        }
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Handle link creation for guys heading out the door:
  */
  
  private void handleExternalTargetLinks(Set<String> outLinks, SpecialtyLayoutLinkData sin,
                                         MetaClusterPointSource lps, double rightBound) {
    boolean dontCare = false;
    Iterator<String> goit = outLinks.iterator();
    boolean isFirst = true;
    while (goit.hasNext()) {
      String linkID = goit.next();
      if (sin.haveLink(linkID)) {
        throw new IllegalStateException();
      }
      sin.startNewLink(linkID);
      if (isFirst) {
        lps.buildPrePath(sin, linkID, lps.getTargetDropRoot(), dontCare, sld_.nps, sld_.rcx, dontCare, dontCare);
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.getLastLeftPoint()));
        isFirst = false;
      }    
      Point2D launch = new Point2D.Double(rightBound, lps.getLastLeftPoint().getY()); 
      sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(launch));
      sin.setBorderPosition(SpecialtyLayoutLinkData.TOP_BORDER, (Point2D)launch.clone());
      sin.setBorderPosition(SpecialtyLayoutLinkData.BOTTOM_BORDER, (Point2D)launch.clone());
      sin.setBorderPosition(SpecialtyLayoutLinkData.LEFT_BORDER, (Point2D)launch.clone());
      sin.setBorderPosition(SpecialtyLayoutLinkData.RIGHT_BORDER, (Point2D)launch.clone());
    }
    return;  
  }
 
  /***************************************************************************
  **
  ** Targets need internal link routing too:
  */
  
  private void targetInternalRouting(List<GeneAndSatelliteCluster> termClusters,
                                     SpecialtyInstructions si,
                                     BTProgressMonitor monitor) throws AsynchExitRequestException {
  
    int numTClust = termClusters.size();
    for (int i = 0; i < numTClust; i++) {
      GeneAndSatelliteCluster tc = termClusters.get(i);
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      } 
      Map<String, SpecialtyLayoutLinkData> sinMap = tc.internalLinkRoutingForTarget(sld_.nps, sld_.rcx);
      Iterator<String> smit = sinMap.keySet().iterator();
      while (smit.hasNext()) {
        String srcID = smit.next();
        SpecialtyLayoutLinkData sin = sinMap.get(srcID);
        if (si.hasLinkPointsForSrc(srcID)) {
          SpecialtyLayoutLinkData sinlp = si.getLinkPointsForSrc(srcID);
          sinlp.merge(sin);
        } else {
          si.setLinkPointsForSrc(srcID, sin);
        }
      }
    }
    return;
  }

  
  /***************************************************************************
  **
  ** Calculate traces per row, per source:
  */
  
  private Map<Integer, Map<String, Double>> calcTracesPerRow(ClusterSeries series, SortedMap<Integer, Set<String>> tracesPerRow, 
                                                             Map<Integer, Double> basePerRow, int numRows, 
                                                             BTProgressMonitor monitor) throws AsynchExitRequestException {
 
    //
    // Figure out the traces into each row, per source:
    //
    
    Iterator<Integer> tprkit = tracesPerRow.keySet().iterator(); 
    HashMap<Integer, Map<String, Double>> traceOrderPerRow = new HashMap<Integer, Map<String, Double>>();     
    while (tprkit.hasNext()) {
      Integer row = tprkit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      } 
      Set<String> traces = tracesPerRow.get(row);
      Double baseForThisRow = basePerRow.get(row);
      //
      // FIX ME??? Is above pen order calc reusable instead of doing this?
      
      TreeMap<Integer, String> sortByTrace = new TreeMap<Integer, String>(Collections.reverseOrder());
      Iterator<String> trit = traces.iterator();    
      while (trit.hasNext()) {
        String srcID = trit.next();
        if (series.hasTrackForSource(srcID)) {
          int tracknum = series.getTrackForSource(srcID);
          sortByTrace.put(new Integer(tracknum), srcID);
        }
      }
      
      //
      // Set the y values of traces for a row; only done if more than one row:
      //
      if (numRows > 1) {
        HashMap<String, Double> srcToY = new HashMap<String, Double>();
        traceOrderPerRow.put(row, srcToY);
        Iterator<Integer> sbit = sortByTrace.keySet().iterator();
        int currTrace = 0;
        while (sbit.hasNext()) {
          Integer traceNum = sbit.next();
          String srcID = sortByTrace.get(traceNum);
          Double yObj = new Double(baseForThisRow.doubleValue() - ((currTrace++) * 20.0));
          srcToY.put(srcID, yObj);
        }
      }
    }
    return (traceOrderPerRow);
  }
 
  /***************************************************************************
  **
  ** Put down the target cluster nodes:
  */
  
  private double placeTargetClusterNodes(GenomeSubset subset, ClusterSeries series, 
                                         SortedMap<Integer, List<GeneAndSatelliteCluster>> tClustRows, 
                                         SortedMap<Integer, List<GeneAndSatelliteCluster>> clustPerRow, 
                                         SortedMap<Integer, Set<String>> tracesPerRow,                                       
                                         Map<Integer, Double> basePerRow, 
                                         double traceOffset,
                                         double rightmostTrack, BTProgressMonitor monitor) throws AsynchExitRequestException {
  
    Set<String> allSrcs = series.getAllSources(); 
    int numRows = tClustRows.size();
    double rightY = series.getLowestTraceY();
   
    //
    // Lay out targets a row at a time:
    //
    
    Point2D base = new Point2D.Double(rightmostTrack + LAST_TRACK_TO_TARGET_PAD_, rightY);
    double currY = rightY;
    double maxXVal = base.getX();

    Iterator<Integer> hit = tClustRows.keySet().iterator();
    int currRowNum = 0;
    while (hit.hasNext()) {
      Integer rowKey = hit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }         
      //
      // Keyed by row number, not row key:
      //
      
      List<GeneAndSatelliteCluster> perRow = tClustRows.get(rowKey);
      int numPerRow = perRow.size();
      Integer currRowNumObj = new Integer(currRowNum);
      clustPerRow.put(currRowNumObj, perRow);
      
      //
      // Calculate traces needed in each row:
      //
      
      Set<String> traces = tracesPerRow.get(currRowNumObj);
      if (traces == null) {
        traces = new HashSet<String>();
        tracesPerRow.put(currRowNumObj, traces);
      }
      for (int i = 0; i < numPerRow; i++) {
        GeneAndSatelliteCluster tc = perRow.get(i);  
        Iterator<String> asit = allSrcs.iterator();
        while (asit.hasNext()) {
          String srcID = asit.next();
          if (tc.isATarget(srcID)) {
            traces.add(srcID);
          }
        }        
      }
      
      //
      // If we have external targets, the first row gets all those traces too:
      //
      
      if (currRowNum == 0) {
        traces.addAll(subset.getSourcesHeadedOut());
      }
           
      //
      // Sort the traces going into each row to mirror the sort in the cluster series:
      //
      
      TreeMap<Integer, String> sortByTrace = new TreeMap<Integer, String>(Collections.reverseOrder());
      Iterator<String> trit = traces.iterator();    
      while (trit.hasNext()) {
        String srcID = trit.next();
        if (series.hasTrackForSource(srcID)) {
          int tracknum = series.getTrackForSource(srcID);
          sortByTrace.put(new Integer(tracknum), srcID);
        }
      }
       
      //
      // Get the height of each target row.  At the same time, let the target
      // know about the input trace order (which it needs before you can
      // find the height):
      //
      
      ArrayList<String> traceOrder = new ArrayList<String>(sortByTrace.values());
      double maxTargetHeightForRow = 0.0;  // Minimum space needed...      
      for (int i = 0; i < numPerRow; i++) {
        GeneAndSatelliteCluster tc = perRow.get(i);        
        tc.orderByTraceOrder(traceOrder, sld_.nps);
        GeneAndSatelliteCluster.ClusterDims oh = tc.getClusterDims(sld_.nps, sld_.rcx);
        double needs = oh.getFullHeightIncrement();
        if (needs > maxTargetHeightForRow) {
          maxTargetHeightForRow = needs;
        }        
      }
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      } 
      
      //
      // Place each target row:
      //
      Double maxTargetHeightForRowObj = new Double(maxTargetHeightForRow);
      double allTraceOffset = traces.size() * traceOffset;
      // only jag down (makes top target trace below bottom input trace) if we have multiple target rows:
      double bphBase = (numRows > 1) ? (currY + allTraceOffset) : currY;
      currY += allTraceOffset;
      double xVal = base.getX();
      basePerRow.put(currRowNumObj, new Double(bphBase));      
      for (int i = 0; i < numPerRow; i++) {
        GeneAndSatelliteCluster tc = perRow.get(i);
        Point2D tcbase = new Point2D.Double(xVal, currY);
        tc.locateAsTarget(tcbase, sld_.nps, sld_.rcx, maxTargetHeightForRowObj);
        double width = tc.getClusterDims(sld_.nps, sld_.rcx).width + INTER_TARGET_PAD_;
        xVal += width;
      }
      currY += maxTargetHeightForRow;
      if (xVal > maxXVal) {
        maxXVal = xVal;
      }
      currRowNum++;
    }
    return (maxXVal);
  }
 
  /***************************************************************************
  **
  ** Get the parameter dialog
  */
  
  public SpecialtyLayoutEngineParamDialogFactory.BuildArgs getParameterDialogBuildArgs(Genome genome, String selectedID, boolean forSubset) {
    SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType lot = (forDiagonal_) ?
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType.DIAGONAL :
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs.LoType.WORKSHEET; 
    return (new SpecialtyLayoutEngineParamDialogFactory.BuildArgs(appState_, genome, lot, forSubset));
  }
  
  /***************************************************************************
  **
  ** Get the undo string for the layout
  */
  
  public String getUndoString() {
    return ((forDiagonal_) ? "undo.worksheetDiagLayout" : "undo.worksheetLayout");
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
  
  public static WorksheetLayoutParams getDefaultParams(boolean forSubset, boolean forDiagonal) {
    WorksheetLayoutParams retval = new WorksheetLayoutParams();
    retval.targGroups = TargTypes.TARGS_BY_INPUTS;
    retval.targSize = 30;
    retval.srcGroups = (forDiagonal) ? SrcTypes.SRCS_BY_SINGLE : SrcTypes.SRCS_BY_SORT; 
    retval.srcSize = (forDiagonal) ? 1 : 25; 
    retval.assignColorMethod = (forSubset) ? ColorTypes.KEEP_COLORS : ColorTypes.COLOR_BY_CYCLE;
    retval.overlayOption = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    retval.showBubbles = true;
    retval.checkColorOverlap = false;  
    retval.overlayOption = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    retval.traceMult = 2;
    retval.textToo = true;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle case where time couse data has disappeared; we need to go back to
  ** default values:
  */
  
  public static void forceParamsAsNeeded(BTState appState, WorksheetLayoutParams params) {
    boolean allowByTime = appState.getDB().getTimeCourseData().haveData();
    if (!allowByTime) {
      if (params.targGroups == TargTypes.TARGS_BY_TIME) {
        params.targGroups = TargTypes.TARGS_BY_INPUTS;
      }
      if (params.srcGroups == SrcTypes.SRCS_BY_TIME) {
        params.srcGroups = SrcTypes.SRCS_BY_SORT;
      }        
    }
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class WorksheetLayoutParams implements SpecialtyLayoutEngineParams, Cloneable {
    public TargTypes targGroups;
    public int targSize;
    public SrcTypes srcGroups;
    public int srcSize;
    public ColorTypes assignColorMethod;
    public boolean showBubbles;
    public boolean checkColorOverlap;
    public int overlayOption;
    public boolean textToo;
    public int traceMult;
  
    public WorksheetLayoutParams clone() {
      try {
        WorksheetLayoutParams retval = (WorksheetLayoutParams)super.clone();
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
  ** Find the nodes that are sources.
  */
  
  private SortedSet<String> findSources(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                        List<GeneAndSatelliteCluster> termClusters, 
                                        Map<String, NodeGrouper.GroupElement> groups, 
                                        List<GeneAndSatelliteCluster> sClustList, double traceOffset, boolean textToo) {

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
      GeneAndSatelliteCluster sc = new GeneAndSatelliteCluster(sld_.appState, srcID, false, traceOffset, false, textToo);
      sc.prepFromGroupsPhaseOne(sld_.nps, groups, false);
      sClustList.add(sc);
    }    

    return (sources);
  }
  
  /***************************************************************************
  **
  ** Assign source columns by first expression time
  */
  
  private ClusterSeries buildClusterSeriesByTime(List<GeneAndSatelliteCluster> sClustList, int traceMult) {

    TimeCourseData tcd = sld_.appState.getDB().getTimeCourseData();    
    ClusterSeries series = new ClusterSeries(false, traceMult);
    int numClust = sClustList.size();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      int firstTime = tcd.getFirstExpressionTime(srcID);
      series.addSourceCluster(firstTime, sc);
    }
    
    return (series);
  }
  
  /***************************************************************************
  **
  ** Assign all sources to a single SSC
  */
  
  private ClusterSeries buildClusterSeriesBySingle(List<GeneAndSatelliteCluster> sClustList, int traceMult) {
    
    ClusterSeries series = new ClusterSeries(false, traceMult);
    int numClust = sClustList.size();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      series.addSourceCluster(0, sc);
    }
    
    return (series);
  }  
  
  /***************************************************************************
  **
  ** Assign target rows by sort
  */
  
  private ClusterSeries buildClusterSeriesBySort(List<GeneAndSatelliteCluster> sClustList, Map<String, Integer> topoSort, int maxSize, int traceMult) {

    //
    // Figure out depth assignments to stay within maximum:
    //
    
    int numClust = sClustList.size();
    
    TreeMap<Integer, Integer> numPerDepth = new TreeMap<Integer, Integer>();    
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      Integer depth = topoSort.get(srcID);      
      Integer perDepth = numPerDepth.get(depth);
      if (perDepth == null) {
        numPerDepth.put(depth, new Integer(1));
      } else {
        int newPerDepth = perDepth.intValue() + 1;
        numPerDepth.put(depth, new Integer(newPerDepth));
      }
    }
    
    HashMap<Integer, Integer> baseKeys = new HashMap<Integer, Integer>();
    int currKey = 0;
    Iterator<Integer> kit = numPerDepth.keySet().iterator();
    while (kit.hasNext()) {
      Integer depth = kit.next();
      Integer max = numPerDepth.get(depth);
      baseKeys.put(depth, new Integer(currKey));
      int numKeys = (max.intValue() / maxSize) + 1;      
      currKey += numKeys;
    }

    //
    // Now assign each to a jiggered depth:
    //
    
    ClusterSeries series = new ClusterSeries(false, traceMult);       
    HashMap<Integer, Integer> seenPerDepth = new HashMap<Integer, Integer>();
    for (int i = 0; i < numClust; i++) {
      GeneAndSatelliteCluster sc = sClustList.get(i); 
      String srcID = sc.getCoreID();
      Integer depth = topoSort.get(srcID);
      Integer perDepth = seenPerDepth.get(depth);
      int keyOffset;
      if (perDepth == null) {
        seenPerDepth.put(depth, new Integer(1));
        keyOffset = 0;
      } else {
        int newPerDepth = perDepth.intValue() + 1;
        seenPerDepth.put(depth, new Integer(newPerDepth));
        keyOffset = newPerDepth / maxSize;
      }
      Integer keyBase = baseKeys.get(depth);
      int keyValue = keyBase.intValue() + keyOffset;
      series.addSourceCluster(keyValue, sc);
    }
    
    return (series);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
}
