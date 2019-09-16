/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.Grid;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.TaggedComparableInteger;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
**
** Route grid links
*/

public class GridLinkRouter {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  //
  // EAST = Target is EAST of SOURCE
  // ADJ = ADJACENT columns
  
  public final static int GRID_LINK_E_ADJ  = 0;
  public final static int GRID_LINK_E_JUMP = 1;
  public final static int GRID_LINK_SE     = 2;  
  public final static int GRID_LINK_S_ADJ  = 3;
  public final static int GRID_LINK_S_JUMP = 4;
  public final static int GRID_LINK_SW     = 5;  
  public final static int GRID_LINK_W_ADJ  = 6;
  public final static int GRID_LINK_W_JUMP = 7;
  public final static int GRID_LINK_NW     = 8;  
  public final static int GRID_LINK_N_ADJ  = 9;
  public final static int GRID_LINK_N_JUMP = 10;
  public final static int GRID_LINK_NE     = 11;  
  public final static int GRID_LINK_STATIC = 12;
  public final static int GRID_LINK_NE_IMMED = 13; 

  public static final int NO_LINK_TYPE               = -1;  
  public static final int CORE_FEEDBACK              = 0;
  public static final int FEEDBACK_ONTO_FAN_IN       = 1;
  public static final int CORE_TO_FANOUT             = 2;
  public static final int INTERNAL_FAN_IN            = 3;
  public static final int FAN_IN_TO_CORE             = 4;
  public static final int CORE_JUMPING               = 5;
  public static final int FAN_OUT_FEEDBACK_TO_CORE   = 6;
  public static final int FAN_OUT_FEEDBACK_TO_FAN_IN = 7;
  public static final int INTERNAL_FAN_OUT           = 8;
  
  public static final int INBOUND_TO_FAN_IN          = 9;
  public static final int INBOUND_TO_CORE            = 10;
  public static final int INBOUND_TO_FAN_OUT         = 11;  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private TrackedGrid grid_;
  private GeneAndSatelliteCluster gasc_;
  private BTState appState_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  public static class ClassifiedLink implements Cloneable {

    public String linkID;
    public int type;
    
    public ClassifiedLink(String linkID, int type) {
      this.linkID = linkID;
      this.type = type;
    }
    
    @Override
    public ClassifiedLink clone() {
      try {
        return ((ClassifiedLink)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    
  }
  
  public static class RCRowCompare implements Comparable<RCRowCompare>, Cloneable {
    
    private TrackedGrid.RCTrack myTrack_;
    private String src_;
    
    public RCRowCompare(TrackedGrid.RCTrack myTrack, String src) {
      myTrack_ = myTrack;
      src_ = src;
    }
    
    @Override
    public RCRowCompare clone() {
      try {
        RCRowCompare retval = (RCRowCompare)super.clone();
        retval.myTrack_ = (this.myTrack_ == null) ? null : this.myTrack_.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    

    public String getSrc() {
      return (src_);
    }
    
    public int compareTo(RCRowCompare other) {
      return (myTrack_.rowCompare(other.myTrack_));
    }
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public GridLinkRouter(BTState appState, TrackedGrid grid, GeneAndSatelliteCluster sc) {
    appState_ = appState;
    grid_ = grid;
    gasc_ = sc;
  }  
   
  /***************************************************************************
  ** 
  ** Copy Constructor to use when cloning a GeneAndSatelliteCluster ONLY.
  */
        
  public GridLinkRouter(GridLinkRouter other, GeneAndSatelliteCluster sc) {
    this.appState_ = other.appState_;
    this.grid_ = (other.grid_ == null) ? null : other.grid_.clone();
    this.gasc_ = sc;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Set up the inbound links
  */

  public Map<String, SpecialtyLayoutLinkData> layoutInboundLinks(Map<String, List<ClassifiedLink>> linksPerSource, 
                                                                 SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                 Map<String, PadCalculatorToo.PadResult> workingPads,
                                                                 Map<String, GridRouterPointSource> pointSources,
                                                                 GridLinkRouter jumperSourceGrid, 
                                                                 Map<String, GridRouterPointSource> jumperPointSources,
                                                                 Set<String> firstSources,
                                                                 Set<String> coreJumpersAlsoToCore, boolean useDDO, 
                                                                 List<String> bottomJumpers) {
    //
    // Now do each on a per-source basis
    //
    
    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    
    if (linksPerSource.size() == 0) {
      return (retval);
    }

    //
    // If first sources are specified, do them first (avoids core-jumper collisions)
    //
    
    // This helps enforce VfG/VfA stacked layout consistency.
    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
    TreeSet<String> olps = new TreeSet<String>(cc);    
    olps.addAll(linksPerSource.keySet());
    Iterator<String> lpsit = olps.iterator();
   
    
    if ((firstSources != null) && !firstSources.isEmpty()) {
      while (lpsit.hasNext()) {
        String src = lpsit.next();
        if (!firstSources.contains(src)) {
          continue;
        }
        List<ClassifiedLink> perSrc = linksPerSource.get(src);
        SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(src);
        int bottomJumpSkipSources = olps.size() - bottomJumpers.indexOf(src);
        fanInboundLinksForSource(src, perSrc, nps, workingPads, sin, 
                                 pointSources, jumperSourceGrid, jumperPointSources, 
                                 coreJumpersAlsoToCore, useDDO, 
                                 bottomJumpers.contains(src), bottomJumpSkipSources);
        retval.put(src, sin);
      }    
      // we restock the iterator here for below!!
      olps = new TreeSet<String>(cc);    
      olps.addAll(linksPerSource.keySet());
      lpsit = olps.iterator();      
    }
    
    while (lpsit.hasNext()) {
      String src = lpsit.next();
      if ((firstSources != null) && firstSources.contains(src)) {
        continue;
      }
      List<ClassifiedLink> perSrc = linksPerSource.get(src);
      SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(src);
      int bottomJumpSkipSources = olps.size() - bottomJumpers.indexOf(src);
      fanInboundLinksForSource(src, perSrc, nps, workingPads, sin, 
                               pointSources, jumperSourceGrid, jumperPointSources, 
                               coreJumpersAlsoToCore, useDDO, 
                               bottomJumpers.contains(src), bottomJumpSkipSources);
      retval.put(src, sin);
    }
 
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Get top reservation count
  */
   
  public int getTopReservations() {
    return (grid_.getTopReservations()); 
  }

  /***************************************************************************
  ** 
  ** Get bottom reservation count (in reservation order)
  */
   
  public List<String> getBottomReservations() {
    return (grid_.getBottomReservations()); 
  }
  
  /***************************************************************************
  ** 
  ** Route feedbacks from a fanout
  */
   
  public Map<String, SpecialtyLayoutLinkData> feedbackRoutingForFanout(Set<String> links, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                       DataAccessContext irx, 
                                                                       Map<String, PadCalculatorToo.PadResult> workingPads,
                                                                       Map<String, GridRouterPointSource> pointSources, String coreID) { 
     
    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(coreID);
    retval.put(coreID, sin);
    Grid.RowAndColumn srcRC = grid_.findPositionRandC(coreID);
    
    TrackedGrid.TrackPosRC lastPos = null;
    TrackedGrid.RCTrack feedTrack = null;    
    boolean firstLink = true;
    // Note ordering by pads, since the sin.startNewLink() order is crucial!
    Iterator<List<String>> psit = orderedByPads(links, nps, irx, workingPads, true);
    while (psit.hasNext()) {
      List<String> perPad = psit.next();    
      int numPerLo = perPad.size();
      for (int i = 0; i < numPerLo; i++) {
        String linkID = perPad.get(i);      
        Linkage link = nps.getLinkage(linkID);
        String trg = link.getTarget();
        int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);       
        int sign = link.getSign();
        sin.startNewLink(linkID);
        GridRouterPointSource grps = pointSources.get(coreID);
        if (grps == null) {
          int srcPad = gasc_.getCurrentLaunchPad(link, workingPads);
          TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(0, coreID);
          TrackedGrid.RCTrack rootLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, srcPad, coreID, linkID, false, Linkage.NONE, srcRC.row); 
          TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(rootLoc); 
          grps = new GridRouterPointSource(appState_, grid_, rootPos, srcRC, coreID);
          pointSources.put(coreID, grps);
        }        
        if (firstLink) {          
          GridRouterPointSource.PointAndPath pap = grps.getSimpleFeedbackPoint(); 
          sin.addPositionListToLink(linkID, pap.path);
          sin.addPositionToLink(linkID, pap.pos);
          feedTrack = pap.pos.getRCTrack();
          firstLink = false;
        } else {
          sin.addPositionToLink(linkID, lastPos.clone());
        }
        TrackedGrid.RCTrack lastLoc = grid_.inheritRCTrackNewColMidline(feedTrack, trgPad, trg, linkID, true, sign, TrackedGrid.RCTrack.NO_TRACK);
        lastPos = new TrackedGrid.TrackPosRC(lastLoc); 
        sin.addPositionToLink(linkID, lastPos.clone());
      } 
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Set up the outbound links.
  */

  public Map<String, SpecialtyLayoutLinkData> layoutOutboundLinks(Set<String> outboundLinks, String coreID,
                                                                   SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                   Map<String, PadCalculatorToo.PadResult> workingPads,
                                                                   Map<String, GridRouterPointSource> pointSources, 
                                                                   List<RCRowCompare> srcOrder) {          

    //
    // I don't care anything about how many outbound links there are for a source;
    // just that there is at least one.
    //

    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();

    HashSet<String> seenSources = new HashSet<String>();
    Iterator<String> olit = outboundLinks.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      if (seenSources.contains(src)) {
        continue;
      }
      seenSources.add(src);
      SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(src);
      retval.put(src, sin);
      boolean isCore = (coreID.equals(src));
      fanOutboundLinksForSource(link, workingPads, sin, pointSources, srcOrder, isCore);
    }
    return (retval);
  }  
   
  /***************************************************************************
  ** 
  ** Set up the links for a grid
  */

  public boolean layoutInternalLinks(Map<String, List<GridLinkRouter.ClassifiedLink>> linksPerSource, 
                                     SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     Map<String, PadCalculatorToo.PadResult> workingPads,
                                     Map<String, GridRouterPointSource> pointSources, List<RCRowCompare> traceOrder, 
                                     String coreID, int srcSkip, Map<String, SpecialtyLayoutLinkData> plans) {  
    //
    // Now do each on a per-source basis
    //
       
    if (linksPerSource.size() == 0) {
      return (false);
    }

    boolean retval = false;
    Iterator<String> lpsit = linksPerSource.keySet().iterator();    
    while (lpsit.hasNext()) {
      String src = lpsit.next();
      List<GridLinkRouter.ClassifiedLink> perSrc = linksPerSource.get(src);
      int targCount = nps.getTargets(src).size();
      SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(src);
      boolean coreStatus = fanInternalLinksForSource(src, perSrc, targCount, nps, workingPads, sin, pointSources, traceOrder, srcSkip);
      if ((coreID != null) && coreID.equals(src)) {
        retval = coreStatus;
      }
      plans.put(src, sin);
    }    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Convert parameterized links to absolute
  */

  public Map<String, SpecialtyLayoutLinkData> convertInternalLinks(Map<String, SpecialtyLayoutLinkData> perSrcPoints, Point2D upperLeft, 
                                                                   SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                                   DataAccessContext irx, String coreID) {

    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    Iterator<String> mit = perSrcPoints.keySet().iterator();
    while (mit.hasNext()) {
      String srcID = mit.next();
      SpecialtyLayoutLinkData sin = perSrcPoints.get(srcID);
      SpecialtyLayoutLinkData sinCopy = sin.clone();
      grid_.convertToPoints(sinCopy, upperLeft, nps, irx, coreID, 0.0);     
      retval.put(srcID, sinCopy);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Convert parameterized links to absolute
  */

  public SpecialtyLayoutLinkData convertInboundLinks(String srcID, Map<String, SpecialtyLayoutLinkData> perSrcPoints, 
                                                     Point2D upperLeft, 
                                                     SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                     DataAccessContext irx, String coreID) {

    SpecialtyLayoutLinkData sin = perSrcPoints.get(srcID);
    if (sin == null) {  // no inbound linkd into the fan
      return (null);
    }
    SpecialtyLayoutLinkData sinCopy = sin.clone();
    grid_.convertToPoints(sinCopy, upperLeft, nps, irx, coreID, 0.0);     
    return (sinCopy);
  } 
  
  /***************************************************************************
  ** 
  ** Convert parameterized links to absolute point coordinates.
  */

  public SpecialtyLayoutLinkData convertAndFixInboundLinks(String srcID, Map<String, SpecialtyLayoutLinkData> perSrcPoints, 
                                                           Point2D upperLeft, 
                                                           SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                           DataAccessContext irx, String coreID, double fixVal) {

    SpecialtyLayoutLinkData sin = perSrcPoints.get(srcID);
    if (sin == null) {  // no inbound link into the fan
      return (null);
    }
    SpecialtyLayoutLinkData sinCopy = sin.clone();
    grid_.convertToPoints(sinCopy, upperLeft, nps, irx, coreID, fixVal);     
    return (sinCopy);
  }
  
  /***************************************************************************
  ** 
  ** Used for core jumpers, where two grids convert one list.  Find the last converted point.
  */

  public Point2D getLastConvertedPoint(SpecialtyLayoutLinkData sin) {
    return (grid_.getLastConvertedPoint(sin));
  }   

  /***************************************************************************
  ** 
  ** Convert parameterized links to absolute
  */

  public List<SpecialtyLayoutLinkData.TrackPos> convertPositionListToPoints(List<SpecialtyLayoutLinkData.TrackPos> pointList, Point2D upperLeft, 
                                                                            SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                                            DataAccessContext irx, String coreID) {
    ArrayList<SpecialtyLayoutLinkData.TrackPos> plCopy = new ArrayList<SpecialtyLayoutLinkData.TrackPos>();
    int npl = pointList.size();
    for (int i = 0; i < npl; i++) {
      SpecialtyLayoutLinkData.TrackPos tp = pointList.get(i);       
      plCopy.add(tp.clone());
    }    
    grid_.convertPositionListToPoints(plCopy, upperLeft, nps, irx, coreID);     
    return (plCopy);
  }
  
  /***************************************************************************
  ** 
  ** Convert parameterized links to absolute
  */

  public Point2D convertPositionToPoint(TrackedGrid.TrackPosRC tprc, Point2D upperLeft, 
                                        SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                        DataAccessContext irx, String coreID) {
    return (grid_.convertPositionToPoint(tprc, upperLeft, nps, irx, coreID));     
  }  


  /***************************************************************************
  ** 
  ** Order the links by pad
  */

  public Iterator<List<String>> orderedByPads(Set<String> linkIDs, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                              DataAccessContext irx, 
                                              Map<String, PadCalculatorToo.PadResult> workingPads, boolean reverse) {
    TreeMap<Integer, List<String>> padSorted = new TreeMap<Integer, List<String>>();
    Iterator<String> feedIt = linkIDs.iterator();
    int sign = (reverse) ? -1 : 1;
    while (feedIt.hasNext()) {
      String linkID = feedIt.next();
      Linkage feedLink = nps.getLinkage(linkID);
      String targID = feedLink.getTarget();
      int landing = gasc_.getCurrentLandingPad(feedLink, workingPads, true);
      Vector2D padOffset = TrackedGrid.landingPadToOffset(appState_, landing, nps, irx, targID, Linkage.POSITIVE);
      Integer padKey = new Integer(sign * (int)(padOffset.getX() / UiUtil.GRID_SIZE));
      List<String> perPad = padSorted.get(padKey);
      if (perPad == null) {
        perPad = new ArrayList<String>();
        padSorted.put(padKey, perPad);
      }
      perPad.add(linkID);
    }
    return (padSorted.values().iterator());    
  }  

  /***************************************************************************
  ** 
  ** Classify fan link directions: (ADJ = adjacent)
  */
    
  public int calcGridLinkType(TrackedGrid grid, SpecialtyLayoutEngine.NodePlaceSupport nps, String src, String trg) {
    
    
    Grid.RowAndColumn srcRC = grid.findPositionRandC(src);
    Grid.RowAndColumn trgRC = grid.findPositionRandC(trg);
    
    if (srcRC.row == trgRC.row) {
      if (srcRC.col == trgRC.col) {
        return (GRID_LINK_STATIC);
      } else if (srcRC.col > trgRC.col) {
        if (srcRC.col == (trgRC.col + 1)) {
          return (GRID_LINK_W_ADJ);
        } else {
          return (GRID_LINK_W_JUMP);
        }
      } else { //(srcRC.col < trgRC.col)
        if (srcRC.col == (trgRC.col - 1)) {
          return (GRID_LINK_E_ADJ);
        // Fix for ISSUE #253
        } else if (grid.canEnterOnLeft(src, trg, nps)) {
          return (GRID_LINK_E_ADJ);
        } else {
          return (GRID_LINK_E_JUMP);
        }
      }
    } else if (srcRC.row > trgRC.row) {
      if (srcRC.col == trgRC.col) {
        if (srcRC.row == (trgRC.row + 1)) {
          return (GRID_LINK_N_ADJ);
        } else {
          return (GRID_LINK_N_JUMP);
        }      
      } else if (srcRC.col > trgRC.col) {
        return (GRID_LINK_NW); 
      } else { //(srcRC.col < trgRC.col) 
        if ((srcRC.row == (trgRC.row + 1)) && (srcRC.col == (trgRC.col - 1))) {
          //
          // We are only allowed to take advantage of the NE_IMMED case if the
          // corner is empty of a node and there is only one link involved:
          //
          String corner = grid_.getContents(srcRC.row, trgRC.col);
          int linkCount = nps.getLinksBetween(src, trg).size();
          if ((corner != null) || (linkCount > 1)) {
            return (GRID_LINK_NE);         
          } else {
            return (GRID_LINK_NE_IMMED);
          }
        } else {
          return (GRID_LINK_NE);
        }      
      } 
    } else {  // (srcRC.row < trgRC.row)
      if (srcRC.col == trgRC.col) {
        if (srcRC.row == (trgRC.row - 1)) {
          return (GRID_LINK_S_ADJ);
        } else {
          return (GRID_LINK_S_JUMP);
        }   
      } else if (srcRC.col > trgRC.col) {
        return (GRID_LINK_SW);  
      } else { //(srcRC.col < trgRC.col)
        return (GRID_LINK_SE);  
      }
    }
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  
  /***************************************************************************
  ** 
  ** Get landing pad assignments.  For when we do not have a grid in hand! (grid may be null)
  */
    
  public static int landForAGrid(GridLinkRouter glr, String nodeID, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                 Set<Integer> usedPads, int landDirection, boolean canEnterLeft, boolean forceTop) {
    INodeRenderer rend = nps.getNodeProperties(nodeID).getRenderer();
       
    // Intercells/slashes ignore the force to top argument (Don't want it coming in on
    // top unless we have to). Everybody else pays attention:
    
    if (canEnterLeft) {
      if (forceTop) {
        canEnterLeft = (rend.ignoreForceTop()) ? true : false;
        landDirection = (rend.ignoreForceTop()) ? landDirection : GridLinkRouter.GRID_LINK_S_ADJ;  
      }
    }
    
    //
    // If we can enter on the left, do so unless that is used.
    // EXCEPT if we have a developing elbow case.  In that instance,
    // the link is going to elbow up into the bottom, and we do NOT
    // want the left pad!
    //
    
    if (canEnterLeft && (landDirection != GRID_LINK_NE_IMMED))  {
      Integer leftObj = Integer.valueOf(rend.getLeftPad());
      if (!usedPads.contains(leftObj)) {
        usedPads.add(leftObj);
        return (leftObj.intValue());
      }
    }
  
    //
    // Used to be based on simple inbound count.  But that made long bubbles
    // even if one of the inbounds was allowed to enter from the bottom.  More
    // complex now, but better:
    //
    
    int needed = nps.inboundLinkCount(nodeID);
    if ((needed == 3) && (glr != null) && (landDirection != GRID_LINK_NE_IMMED)) {  // only bad case; > 3 and need extension anyway...
      Set<String> srcs = nps.getSources(nodeID);
      Iterator<String> sit = srcs.iterator();
      while (sit.hasNext()) {
        String src = sit.next();
        if (glr.grid_.contains(src)) {
          int linkDirection = glr.calcGridLinkType(glr.grid_, nps, src, nodeID); 
          if (linkDirection == GRID_LINK_NE_IMMED) {
            needed = 2;
            break;
          }
        }
      } 
    } 
     
    Node node = nps.getNode(nodeID);
    switch (landDirection) {
      case GRID_LINK_E_JUMP:
      case GRID_LINK_SE:
      case GRID_LINK_S_ADJ:
      case GRID_LINK_S_JUMP:
      case GRID_LINK_SW:
      case GRID_LINK_W_ADJ:
      case GRID_LINK_W_JUMP:
      case GRID_LINK_NW:
      case GRID_LINK_STATIC:
        return (rend.getBestTopPad(node, usedPads, NodeProperties.HORIZONTAL_GROWTH, needed, true));
      case GRID_LINK_N_ADJ: 
      case GRID_LINK_N_JUMP: 
      case GRID_LINK_NE:
        return (rend.getBestTopPad(node, usedPads, NodeProperties.HORIZONTAL_GROWTH, needed, true)); 
      case GRID_LINK_NE_IMMED: 
        int botPad = rend.getBottomPad(node);
        usedPads.add(new Integer(botPad));
        return (botPad);
      case GRID_LINK_E_ADJ:
        // those able to enter on left caught above
        return (rend.getBestTopPad(node, usedPads, NodeProperties.HORIZONTAL_GROWTH, needed, true)); 
      default:
        throw new IllegalArgumentException();
    }
  }
   
  /***************************************************************************
  ** 
  ** Get landing pad assignments for this
  */
   
  public int landForThisGrid(String nodeID, SpecialtyLayoutEngine.NodePlaceSupport nps, Set<Integer> usedPads, 
                             int landDirection, boolean canEnterLeft, boolean forceTop) { 
    return (landForAGrid(this, nodeID, nps, usedPads, landDirection, canEnterLeft, forceTop));
  }
  
  /***************************************************************************
  ** 
  ** Answer if assigning landing is consistent with direct landing:
  */
    
  public static boolean landOKForDirect(String nodeID, SpecialtyLayoutEngine.NodePlaceSupport nps, int landing) {
    INodeRenderer rend = nps.getNodeProperties(nodeID).getRenderer();
    return (rend.landOKForDirect(landing));
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Set up the inbound links for a fan
  */

  private void fanInboundLinksForSource(String src, List<ClassifiedLink> perSrc,
                                        SpecialtyLayoutEngine.NodePlaceSupport nps,
                                        Map<String, PadCalculatorToo.PadResult> workingPads,
                                        SpecialtyLayoutLinkData sin,
                                        Map<String, GridRouterPointSource> pointSources,
                                        GridLinkRouter jumperSourceGrid, 
                                        Map<String, GridRouterPointSource> jumperPointSources,
                                        Set<String> coreJumpersAlsoToCore, boolean useDDO, 
                                        boolean bottomJump, int bottomJumpSkipSources) {  

    //
    // If the target count exceeds the internal count, external outbounds will
    // be needed.  If more than one of a type is needed, we will need to process
    // in appropriate order
    //
    
    TreeMap<Integer, List<String>> linksPerType = new TreeMap<Integer, List<String>>();
    int numPerSrc = perSrc.size();
    for (int i = 0; i < numPerSrc; i++) {
      ClassifiedLink cLink = perSrc.get(i);
      Integer typeObj = new Integer(cLink.type);
      List<String> perType = linksPerType.get(typeObj);
      if (perType == null) {
        perType = new ArrayList<String>();
        linksPerType.put(typeObj, perType);
      }
      perType.add(cLink.linkID);
    }
    
    //
    // Note ddo will be null for target-only GASclusters!
    //
        
    Iterator<Integer> lptit = linksPerType.keySet().iterator();    
    while (lptit.hasNext()) {
      Integer typeObj = lptit.next();
      List<String> perType = linksPerType.get(typeObj);
      switch (typeObj.intValue()) {
        case INBOUND_TO_FAN_IN:
          planInboundToFanLinks(src, perType, nps, workingPads, sin, pointSources, useDDO);
          break;
        case INBOUND_TO_CORE:
          planInboundToCoreLinks(src, perType, nps, workingPads, sin, pointSources, useDDO);
          break;
        case INBOUND_TO_FAN_OUT:
          planInboundToFanLinks(src, perType, nps, workingPads, sin, pointSources, useDDO);
          break;
        case CORE_JUMPING: 
          planCoreJumperLinks(src, perType, nps, workingPads, sin, 
                               pointSources, jumperSourceGrid, jumperPointSources, 
                               coreJumpersAlsoToCore, useDDO, bottomJump, bottomJumpSkipSources); 
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    return;
  }   

  /***************************************************************************
  ** 
  ** Set up the internal links for a fan
  */

  private boolean fanInternalLinksForSource(String src, List<ClassifiedLink> perSrc, int targCount,
                                            SpecialtyLayoutEngine.NodePlaceSupport nps,
                                            Map<String, PadCalculatorToo.PadResult> workingPads,
                                            SpecialtyLayoutLinkData sin, 
                                            Map<String, GridRouterPointSource> pointSources, 
                                            List<RCRowCompare> traceOrder, int srcSkip) {  

    //
    // If the target count exceeds the internal count, external outbounds will
    // be needed.  If more than one of a type is needed, we will need to process
    // in appropriate order
    //

    TreeMap<Integer, List<String>> linksPerType = new TreeMap<Integer, List<String>>();
    int numPerSrc = perSrc.size();
    for (int i = 0; i < numPerSrc; i++) {
      ClassifiedLink cLink = perSrc.get(i);
      Integer typeObj = new Integer(cLink.type);
      List<String> perType = linksPerType.get(typeObj);
      if (perType == null) {
        perType = new ArrayList<String>();
        linksPerType.put(typeObj, perType);
      }
      perType.add(cLink.linkID);
    }
    
    boolean coreIsFanned = false;
    Iterator<Integer> lptit = linksPerType.keySet().iterator();    
    while (lptit.hasNext()) {
      Integer typeObj = lptit.next();
      List<String> perType = linksPerType.get(typeObj);
      switch (typeObj.intValue()) {
        case CORE_FEEDBACK:
        case FEEDBACK_ONTO_FAN_IN:
          break;
        case CORE_TO_FANOUT:
          coreIsFanned = routeCoreToFanOutLinks(src, perType, targCount, nps, workingPads, sin, pointSources, srcSkip);
          break;
        case FAN_IN_TO_CORE:
          routeFaninToCoreLinks(src, perType, targCount, nps, workingPads, sin, pointSources, traceOrder);
          break;
        case INTERNAL_FAN_IN: 
          routeInternalFanLinks(src, perType, targCount, nps, workingPads, sin, pointSources, false, srcSkip);
          break;
        case CORE_JUMPING:
        case FAN_OUT_FEEDBACK_TO_CORE:
        case FAN_OUT_FEEDBACK_TO_FAN_IN:
          break;
        case INTERNAL_FAN_OUT:
          routeInternalFanLinks(src, perType, targCount, nps, workingPads, sin, pointSources, false, srcSkip);
          break;
      }
    }
 
    return (coreIsFanned);
  } 
  
  /***************************************************************************
  ** 
  ** Set up the internal links for a fan
  */

  private boolean routeInternalFanLinks(String src, List<String> links, int trgCount, 
                                        SpecialtyLayoutEngine.NodePlaceSupport nps,
                                        Map<String, PadCalculatorToo.PadResult> workingPads,
                                        SpecialtyLayoutLinkData sin, 
                                        Map<String, GridRouterPointSource> pointSources, 
                                        boolean coreSrc, int srcSkip) {  
    
    //
    // Get the internal grid locations and figure out the directions.  Per direction,
    // sort by column:
    //

    HashSet<String> directLinks = new HashSet<String>();
    HashSet<String> elbowLinks = new HashSet<String>();
    Grid.RowAndColumn srcRC = grid_.findPositionRandC(src);
    TreeMap<LinkLayoutOrdering, List<Linkage>> linkOrdering = new TreeMap<LinkLayoutOrdering, List<Linkage>>();
    int srcPad = 0;
    String aLink = null;
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      Linkage link = nps.getLinkage(linkID);
      srcPad = gasc_.getCurrentLaunchPad(link, workingPads);
      aLink = linkID;
      String ltrg = link.getTarget();
      Grid.RowAndColumn trgRC = grid_.findPositionRandC(ltrg);
      int linkType = calcGridLinkType(grid_, nps, src, ltrg);
      int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
      if ((linkType == GRID_LINK_E_ADJ) && landOKForDirect(ltrg, nps, trgPad)) {
        directLinks.add(linkID);
      }
      if (linkType == GRID_LINK_NE_IMMED) {
        elbowLinks.add(linkID);
      }
      // Note ordering, since the sin.startNewLink() order is crucial!
      int trgType = nps.getNode(ltrg).getNodeType();
      LinkLayoutOrdering key = new LinkLayoutOrdering(srcRC, trgRC, nps, ltrg, trgPad, trgType);
      List<Linkage> linksPerOrder = linkOrdering.get(key);
      if (linksPerOrder == null) {
        linksPerOrder = new ArrayList<Linkage>();
        linkOrdering.put(key, linksPerOrder);
      }
      linksPerOrder.add(link);
    }
    
    //
    // If just one direct link, nothing left to do:
    //
    
    if ((trgCount == 1) && (numLinks == 1) && (directLinks.size() == 1)) {  // nothing to do...
      return (false);
    }

    //
    // Do in sorted order:
    //

    GridRouterPointSource grps = pointSources.get(src);
    if (grps == null) {
      int reserveCol = (coreSrc) ? srcRC.col : srcRC.col + 1;
      TrackedGrid.TrackSpec colSpec;
      // srcSkip is typically the number of corejumpers, i.e. the number of fan-out inputs
      // coming over from the fan-in. We want the core trace to be between these jumpers
      // and the inputs going straight to the fan-out.
      if (coreSrc && (srcSkip != 0)) {
        colSpec = grid_.reserveSkippedColumnTrack(reserveCol, src, srcSkip);
      } else {
        colSpec = grid_.reserveColumnTrack(reserveCol, src);
      }
      TrackedGrid.RCTrack rootLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, srcPad, src, aLink, false, Linkage.NONE, srcRC.row); 
      TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(rootLoc); 
      grps = new GridRouterPointSource(appState_, grid_, rootPos, srcRC, src);
      pointSources.put(src, grps);
    }
    Iterator<LinkLayoutOrdering> loit = linkOrdering.keySet().iterator();
    while (loit.hasNext()) {
      LinkLayoutOrdering pdKey = loit.next();
      List<Linkage> linksPerOrder = linkOrdering.get(pdKey);
      int numPerLo = linksPerOrder.size();
      for (int i = 0; i < numPerLo; i++) {
        Linkage link = linksPerOrder.get(i);
        String linkID = link.getID();
        sin.startNewLink(linkID);
        if (directLinks.contains(linkID)) {
          sin.addPositionToLink(linkID, grps.getRootPos().clone());
        } else if (elbowLinks.contains(linkID)) {
          String trg = link.getTarget();
          int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
          int sign = link.getSign();
          Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
          TrackedGrid.TrackPosRC pos = grps.getBottomElbowPoint(srcRC.row, trgRC.col, srcPad, trgPad, trg, linkID, sign);
          // If nobody is headed out the door, getting the root pos installed is not needed.  But if the link
          // is continuing out, the root position is needed to make sure the elbow link is correctly placed:
          sin.addPositionToLink(linkID, grps.getRootPos().clone());
          sin.addPositionToLink(linkID, pos);
        } else {            
          String trg = link.getTarget();
          int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
          int sign = link.getSign();
          Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
          // Don't want presence of core in the grid to mess up fan-out routing:
          boolean onLeft = (coreSrc) ? grid_.firstOnLeft(trg, null) : grid_.canEnterOnLeft(src, trg, nps);
          onLeft = onLeft && landOKForDirect(trg, nps, trgPad);
          GridRouterPointSource.PointAndPath pap = grps.getLastPoint(onLeft, trgRC.row, trgRC.col, trgPad, trg, linkID, sign);
          sin.addPositionListToLink(linkID, pap.path);
          sin.addPositionToLink(linkID, pap.pos);
        }
      }
    }
    
    return (true);
  }  

  /***************************************************************************
  ** 
  ** Set up the links from a fan-in to a core
  */

  private void routeFaninToCoreLinks(String src, List<String> links, int trgCount, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     Map<String, PadCalculatorToo.PadResult> workingPads,
                                     SpecialtyLayoutLinkData sin, Map<String, GridRouterPointSource> pointSources, 
                                     List<RCRowCompare> traceOrder) {

    //
    // If there are multiple links from the source to the target, order them by landing pad.  IF pads
    // are equal, break tie by linkID.
    //
    
    TreeMap<TaggedComparableInteger, List<Linkage>> linkOrdering = new TreeMap<TaggedComparableInteger, List<Linkage>>();
    String target = null;
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      Linkage link = nps.getLinkage(linkID);
      int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
      target = link.getTarget();
      TaggedComparableInteger key = new TaggedComparableInteger(new Integer(trgPad), linkID);
      List<Linkage> linksPerOrder = linkOrdering.get(key);
      if (linksPerOrder == null) {
        linksPerOrder = new ArrayList<Linkage>();
        linkOrdering.put(key, linksPerOrder);
      }
      linksPerOrder.add(link);
    }     

    //
    // For the source, figure out if it is on the right boundary of the grid.  If it is, we have
    // it easy: a simple dogleg link.  If not, we need to either drop off the rightmost existing
    // link corner, or route above the nodes to the right of us.
    //
    
    if (!grid_.contains(src)) {
      return;
    }
    
    // Issue #251 hotspot:
    Grid.RowAndColumn srcRC = grid_.findPositionRandC(src);
    String rightmost = grid_.rightmostForRow(srcRC.row, target);
    boolean isDirect = rightmost.equals(src) && (trgCount == 1);
    TrackedGrid.RCTrack sortTrack = null;
    boolean firstLink = true;
    boolean gotDptc = false;
    // Note ordering, since the sin.startNewLink() order is crucial!
    Iterator<TaggedComparableInteger> loit = linkOrdering.keySet().iterator();
    TrackedGrid.TrackPosRC lastPos = null;
    while (loit.hasNext()) {
      TaggedComparableInteger pdKey = loit.next();
      List<Linkage> linksPerPad = linkOrdering.get(pdKey);
      int numPerLo = linksPerPad.size();
      for (int i = 0; i < numPerLo; i++) {
        Linkage link = linksPerPad.get(i);      
        String linkID = link.getID();
        String trg = link.getTarget();
        int srcPad = gasc_.getCurrentLaunchPad(link, workingPads);
        int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
        int sign = link.getSign();
        sin.startNewLink(linkID);
        GridRouterPointSource grps = pointSources.get(src);
        if (grps == null) {
          TrackedGrid.TrackPosRC rootPos;
          if (isDirect) {
            TrackedGrid.RCTrack rootLoc = 
              grid_.buildRCTrackForDualMidline(appState_, trgPad, trg, true, srcPad, 
                                               src, false, sign, linkID, TrackedGrid.RCTrack.NO_TRACK, srcRC.row); 
            rootPos = new TrackedGrid.TrackPosRC(rootLoc);
          } else {
            TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(srcRC.col + 1, src);
            TrackedGrid.RCTrack rootLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, srcPad, src, linkID, false, Linkage.NONE, srcRC.row); 
            rootPos = new TrackedGrid.TrackPosRC(rootLoc);            
          }
          grps = new GridRouterPointSource(appState_, grid_, rootPos, srcRC, src);
          pointSources.put(src, grps);
        }
        if (firstLink) {
          if (isDirect) {
            TrackedGrid.TrackPosRC rootPos = grps.getRootPos();
            sin.addPositionToLink(linkID, rootPos.clone());
            sortTrack = rootPos.getRCTrack().clone();
          } else {
             // Issue #251 hotspot:
            GridRouterPointSource.PointAndPath pap = grps.getDepartingPointToCore(trgPad, trg, linkID, sign);
            sin.addPositionListToLink(linkID, pap.path);
            sin.addPositionToLink(linkID, pap.pos);
            sortTrack = pap.pos.getRCTrack().clone();
            gotDptc = true;
          }          
          firstLink = false;
        } else {
          sin.addPositionToLink(linkID, lastPos.clone());
        }
        TrackedGrid.RCTrack lastLoc = grid_.inheritRCTrackNewColMidline(sortTrack, trgPad, trg, linkID, true, sign, TrackedGrid.RCTrack.NO_TRACK);
        lastPos = new TrackedGrid.TrackPosRC(lastLoc); 
        sin.addPositionToLink(linkID, lastPos);
        if (gotDptc) {
          grps.updateLastDepartingPointToCore(lastPos);
        }
      }
    }

    if (sortTrack != null) {
      RCRowCompare rcr = new RCRowCompare(sortTrack, src);
      traceOrder.add(rcr);
    }
    return;    
  } 

  /***************************************************************************
  ** 
  ** Set up the outbound links for a fanout.  Very much like the fan-in to
  ** core case
  */

  private void fanOutboundLinksForSource(Linkage link, Map<String, PadCalculatorToo.PadResult> padChanges,
                                         SpecialtyLayoutLinkData sin, 
                                         Map<String, GridRouterPointSource> pointSources, List<RCRowCompare> srcOrder, boolean isCore) { 
    //
    // For the source, figure out if it is on the right boundary of the grid.  If it is, we have
    // it easy: a simple point.  If not, we need to either drop off the rightmost existing
    // link corner, or route above the nodes to the right of us.
    //
    
    String src = link.getSource();
    if (!grid_.contains(src)) {
      return;
    }
    
    //
    // Core position is bogus, so we never consider it direct:
    //
    
    Grid.RowAndColumn srcRC = grid_.findPositionRandC(src);
     
    boolean isDirect = false;
    if (!isCore) {
      String rightmost = grid_.rightmostForRow(srcRC.row, null);
      isDirect = rightmost.equals(src);
    }
    
    String linkID = link.getID();
    int srcPad = gasc_.getCurrentLaunchPad(link, padChanges);
    sin.startNewLink(linkID);
    GridRouterPointSource grps = pointSources.get(src);
    if (grps == null) {
      TrackedGrid.TrackPosRC rootPos;
      // Currently reserving, even if direct, due to the creation of initial corner perhaps??
      // Note this reserves vertical space though it is not needed...
      TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(srcRC.col + 1, src);
      TrackedGrid.RCTrack rootLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, srcPad, src, linkID, false, Linkage.NONE, srcRC.row); 
      rootPos = new TrackedGrid.TrackPosRC(rootLoc);            
      grps = new GridRouterPointSource(appState_, grid_, rootPos, srcRC, src);
      pointSources.put(src, grps);
    }  

    TrackedGrid.RCTrack sortTrack;    
    if (isDirect) {
      TrackedGrid.TrackPosRC rootPos = grps.getRootPos();
      sin.addPositionToLink(linkID, rootPos.clone());
      sortTrack = rootPos.getRCTrack().clone();
    } else {
      GridRouterPointSource.PointAndPath pap = grps.getDepartingPointToOutbound();
      sin.addPositionListToLink(linkID, pap.path);
      sin.addPositionToLink(linkID, pap.pos);
      sortTrack = pap.pos.getRCTrack().clone();
    }
    
    RCRowCompare rcr = new RCRowCompare(sortTrack, src);

    srcOrder.add(rcr);
    return;
  }     
 
  /***************************************************************************
  ** 
  ** Set up the links from core to fan-out
  */

  private boolean routeCoreToFanOutLinks(String src, List<String> links, int trgCount, 
                                         SpecialtyLayoutEngine.NodePlaceSupport nps,
                                         Map<String, PadCalculatorToo.PadResult> workingPads,
                                         SpecialtyLayoutLinkData sin, Map<String, GridRouterPointSource> pointSources, int srcSkip) {
    

    return (routeInternalFanLinks(src, links, trgCount, nps, workingPads, sin, pointSources, true, srcSkip));         
  }  

  /***************************************************************************
  ** 
  ** Set up the inbound to fan links
  */

  private void planInboundToFanLinks(String src, List<String> links,
                                     SpecialtyLayoutEngine.NodePlaceSupport nps,
                                     Map<String, PadCalculatorToo.PadResult> workingPads,
                                     SpecialtyLayoutLinkData sin, 
                                     Map<String, GridRouterPointSource> pointSources, boolean useDDO) {  
    
    //
    // Sort the targets from either the upper left (for links coming in from above) or
    // lower left.
    //
    
    boolean comingFromTop = (useDDO && (gasc_.getDDOracle() != null)) ? gasc_.getDDOracle().comingFromTop(src, gasc_) : true;
    
    TreeMap<LinkLayoutOrdering, List<Linkage>> linkOrdering = new TreeMap<LinkLayoutOrdering, List<Linkage>>();
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      Linkage link = nps.getLinkage(linkID);
      String ltrg = link.getTarget();
      Grid.RowAndColumn trgRC = grid_.findPositionRandC(ltrg);
      int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true); 
      boolean onLeft = grid_.firstOnLeft(ltrg, null);
      onLeft = onLeft && landOKForDirect(ltrg, nps, trgPad);
      int trgType = nps.getNode(ltrg).getNodeType();
      
      LinkLayoutOrdering key = new LinkLayoutOrdering(trgRC, nps, ltrg, trgPad, onLeft, trgType, comingFromTop);      
      List<Linkage> linksPerOrder = linkOrdering.get(key);
      if (linksPerOrder == null) {
        linksPerOrder = new ArrayList<Linkage>();
        linkOrdering.put(key, linksPerOrder);
      }
      linksPerOrder.add(link);
    }
    
    //
    // Do in sorted order:
    //

    Iterator<LinkLayoutOrdering> loit = linkOrdering.keySet().iterator();
    while (loit.hasNext()) {
      // Note ordering, since the sin.startNewLink() order is crucial!
      LinkLayoutOrdering pdKey = loit.next();
      List<Linkage> linksPerOrder = linkOrdering.get(pdKey);
      int numPerLo = linksPerOrder.size();
      for (int i = 0; i < numPerLo; i++) {
        Linkage link = linksPerOrder.get(i);      
        String linkID = link.getID();
        String trg = link.getTarget();
        sin.startNewLink(linkID);
        GridRouterPointSource grps = pointSources.get(src);
        if (grps == null) {
          TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(0, src);
          Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
          TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(trgRC.row, src);
          // This is the ROOT POINT for entering into the grid!
          TrackedGrid.RCTrack inboundLoc = grid_.buildRCTrack(appState_, rowSpec, colSpec);
          //
          // If we are entering from the left, we don't specify the inbound column:
          //
          if (useDDO && (gasc_.getDDOracle() != null)) {
            inboundLoc = grid_.buildRCTrack(inboundLoc, TrackedGrid.RCTrack.X_FLOATS);
          }
          TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(inboundLoc); 
          grps = new GridRouterPointSource(appState_, grid_, rootPos, src);
          pointSources.put(src, grps);
        }
        int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
        int sign = link.getSign();
        Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
        boolean onLeft = grid_.firstOnLeft(trg, null);
        onLeft = onLeft && landOKForDirect(trg, nps, trgPad);
        GridRouterPointSource.PointAndPath pap = grps.getLastPointForInbound(onLeft, trgRC.row, trgRC.col, trgPad, trg, linkID, sign, comingFromTop);  
        sin.addPositionListToLink(linkID, pap.path);
        sin.addPositionToLink(linkID, pap.pos);
      }
    }
    
    return;
  }  

  /***************************************************************************
  ** 
  ** Set up the inbound links to a core
  */

  private void planInboundToCoreLinks(String src, List<String> links, 
                                      SpecialtyLayoutEngine.NodePlaceSupport nps,
                                      Map<String, PadCalculatorToo.PadResult> workingPads,
                                      SpecialtyLayoutLinkData sin, 
                                      Map<String, GridRouterPointSource> pointSources, boolean useDDO) {
  
    boolean comingFromTop = (useDDO && (gasc_.getDDOracle() != null)) ? gasc_.getDDOracle().comingFromTop(src, gasc_) : true;
    
    //
    // If there are multiple links from the source to the target, order them by landing pad:
    //
    
    TreeMap<Integer, List<Linkage>> linkOrdering = new TreeMap<Integer, List<Linkage>>();
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      Linkage link = nps.getLinkage(linkID);
      // Note no core link pad changes are here yet, but we are asking for an order estimate (true)
      int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
      Integer key = new Integer(trgPad);
      List<Linkage> linksPerOrder = linkOrdering.get(key);
      if (linksPerOrder == null) {
        linksPerOrder = new ArrayList<Linkage>();
        linkOrdering.put(key, linksPerOrder);
      }
      linksPerOrder.add(link);
    }    

    //
    // Get a row that is along the top of the fan-in grid.
    // For the source, figure out if it is on the right boundary of the grid.  If it is, we have
    // it easy: a simple dogleg link.  If not, we need to either drop off the rightmost existing
    // link corner, or route above the nodes to the right of us.
    //
    
    boolean isFirst = true;
    TrackedGrid.RCTrack dropTrack = null;
    TrackedGrid.TrackPosRC lastPos = null;
    Iterator<Integer> loit = linkOrdering.keySet().iterator();
    // Note ordering, since the sin.startNewLink() order is crucial!
    while (loit.hasNext()) {
      Integer pdKey = loit.next();
      List<Linkage> linksPerPad = linkOrdering.get(pdKey);
      int numPerLo = linksPerPad.size();
      for (int i = 0; i < numPerLo; i++) {
        Linkage link = linksPerPad.get(i);      
        String linkID = link.getID();
        String trg = link.getTarget();

        int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
        
        int sign = link.getSign();
        sin.startNewLink(linkID);
        GridRouterPointSource grps = pointSources.get(src);
        if (grps == null) {          
          //this is where complex inbounds to core are getting their first corner at the upper left:
          TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(0, src);
          TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(0, src);
          TrackedGrid.RCTrack inboundLoc = grid_.buildRCTrack(appState_, rowSpec, colSpec);
          TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(inboundLoc); 
          grps = new GridRouterPointSource(appState_, grid_, rootPos, src);
          pointSources.put(src, grps);
        }
        if (isFirst) {
          isFirst = false;
          GridRouterPointSource.PointAndPath pap = grps.getPointForInboundDirectToCore(comingFromTop);
          sin.addPositionListToLink(linkID, pap.path);
          sin.addPositionToLink(linkID, pap.pos);
          dropTrack = pap.pos.getRCTrack().clone();
        } else {
          sin.addPositionToLink(linkID, lastPos.clone()); 
        }
        TrackedGrid.RCTrack lastLoc = grid_.inheritRCTrackNewColMidline(dropTrack, trgPad, trg, linkID, true, sign, TrackedGrid.RCTrack.NO_TRACK);
        lastPos = new TrackedGrid.TrackPosRC(lastLoc); 
        sin.addPositionToLink(linkID, lastPos);      
      }
    } 

    return;    
  }  

  /***************************************************************************
  ** 
  ** Set up the core jumping links
  */

  private void planCoreJumperLinks(String src, List<String> links,
                                   SpecialtyLayoutEngine.NodePlaceSupport nps,
                                   Map<String, PadCalculatorToo.PadResult> workingPads,
                                   SpecialtyLayoutLinkData sin, 
                                   Map<String, GridRouterPointSource> pointSources, 
                                   GridLinkRouter jumperSourceGrid, 
                                   Map<String, GridRouterPointSource> jumperPointSources,
                                   Set<String> coreJumpersAlsoToCore, boolean useDDO, 
                                   boolean isABottom, int bottomJumpSkipSources) {  
    
    boolean comingFromTop = (useDDO && (gasc_.getDDOracle() != null)) ? gasc_.getDDOracle().comingFromTop(src, gasc_) : true;  
    
    //
    // Sort the targets from the upper left:
    //
    
    TreeMap<LinkLayoutOrdering, List<Linkage>> linkOrdering = new TreeMap<LinkLayoutOrdering, List<Linkage>>();
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      Linkage link = nps.getLinkage(linkID);
      String ltrg = link.getTarget();
      if (!grid_.contains(ltrg)) {
        continue;
      }
      Grid.RowAndColumn trgRC = grid_.findPositionRandC(ltrg);
      int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true); 
      boolean onLeft = grid_.firstOnLeft(ltrg, null);
      onLeft = onLeft && landOKForDirect(ltrg, nps, trgPad);
      int trgType = nps.getNode(ltrg).getNodeType();
      
      LinkLayoutOrdering key = new LinkLayoutOrdering(trgRC, nps, ltrg, trgPad, onLeft, trgType, true);      
      List<Linkage> linksPerOrder = linkOrdering.get(key);
      if (linksPerOrder == null) {
        linksPerOrder = new ArrayList<Linkage>();
        linkOrdering.put(key, linksPerOrder);
      }
      linksPerOrder.add(link);
    }
    
    //
    // Do in sorted order.  This routing involves getting two grids involved.  We need to
    // route out of the fan-in grid and into the fan-out grid!
    //
 
    boolean isFirst = true;
    // Note ordering, since the sin.startNewLink() order is crucial!
    Iterator<LinkLayoutOrdering> loit = linkOrdering.keySet().iterator();
    while (loit.hasNext()) {
      LinkLayoutOrdering pdKey = loit.next();
      List<Linkage> linksPerOrder = linkOrdering.get(pdKey);
      int numPerLo = linksPerOrder.size();
      for (int i = 0; i < numPerLo; i++) {
        Linkage link = linksPerOrder.get(i);
        // This gets, e.g. the corner point over right the gene to attach the jumper link to:
        if (isFirst) {
          jumperSourceGrid.routeCoreJumperLinksOutOfFanIn(src, link, sin, jumperPointSources, 
                                                          coreJumpersAlsoToCore, comingFromTop);
        }
        String linkID = link.getID();
        String trg = link.getTarget();
        // This works with the first link:
        if (!sin.haveLink(linkID)) {
          sin.startNewLink(linkID);
        }
        GridRouterPointSource grps = pointSources.get(src);
        if (grps == null) {
          // Fix for Issue #249, bottom-of-fan-in core jumpers get reserved on far left tracks:
          TrackedGrid.TrackSpec colSpec;
          if (isABottom) {
            colSpec = grid_.reserveSkippedColumnTrack(0, src, bottomJumpSkipSources);
          } else {
            colSpec = grid_.reserveColumnTrack(0, src);
          }
          Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
          TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(trgRC.row, src);
          TrackedGrid.RCTrack inboundLoc = grid_.buildRCTrack(appState_, rowSpec, colSpec);
          TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(inboundLoc); 
          grps = new GridRouterPointSource(appState_, grid_, rootPos, src);
          pointSources.put(src, grps);
        }
        int trgPad = gasc_.getCurrentLandingPad(link, workingPads, true);
        int sign = link.getSign();
        Grid.RowAndColumn trgRC = grid_.findPositionRandC(trg);
        boolean onLeft = grid_.firstOnLeft(trg, null);
        onLeft = onLeft && landOKForDirect(trg, nps, trgPad);
        GridRouterPointSource.PointAndPath pap = grps.getLastPointForInbound(onLeft, trgRC.row, trgRC.col, trgPad, trg, linkID, sign, true);
        // core jumps need an extra floating point
        if (isFirst) {
          if (!pap.path.isEmpty()) {
            TrackedGrid.TrackPosRC extraPos = pap.path.get(0);
            TrackedGrid.RCTrack extraLoc = grid_.buildRCTrack(extraPos.getRCTrack(), TrackedGrid.RCTrack.Y_FLOATS);
            sin.addPositionToLink(linkID, new TrackedGrid.TrackPosRC(extraLoc));
          }
          isFirst = false;
        }
        sin.addPositionListToLink(linkID, pap.path);
        sin.addPositionToLink(linkID, pap.pos);
      }
    }
    return;    
  } 
  
  /***************************************************************************
  ** 
  ** Set up the core jumping links outbound from fan-in:
  */

  private void routeCoreJumperLinksOutOfFanIn(String src, Linkage link,
                                              SpecialtyLayoutLinkData sin, Map<String, GridRouterPointSource> pointSources, 
                                              Set<String> coreJumpersAlsoToCore, boolean comingFromTop) {  
   
    String linkID = link.getID();
    sin.startNewLink(linkID);
    GridRouterPointSource grps = pointSources.get(src);
    if (grps == null) {
      TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(0, src);
      TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(0, src);
      TrackedGrid.RCTrack inboundLoc = grid_.buildRCTrack(appState_, rowSpec, colSpec);
      TrackedGrid.TrackPosRC rootPos = new TrackedGrid.TrackPosRC(inboundLoc); 
      grps = new GridRouterPointSource(appState_, grid_, rootPos, src);
      pointSources.put(src, grps);
    }
     
    if (coreJumpersAlsoToCore.contains(src)) {
      GridRouterPointSource.PointAndPath pap = grps.hangJumperOffDirectToCore();
      sin.addPositionListToLink(linkID, pap.path);
      sin.addPositionToLink(linkID, pap.pos);
    } else {
      GridRouterPointSource.PointAndPath pap = grps.getPointForInboundDirectToCore(comingFromTop);
      sin.addPositionListToLink(linkID, pap.path);
      sin.addPositionToLink(linkID, pap.pos);
    }
    return;    
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static class LinkLayoutOrdering implements Comparable<LinkLayoutOrdering> {

    public double deltaRow;
    public double deltaCol;
    public int padNum;
    public int targType;
    private SpecialtyLayoutEngine.NodePlaceSupport npsx;
    public String targNodeID;

    // Used for inbound links:  Add 1 to make sure guys in column 0 are not considered to be
    // in the west!
    LinkLayoutOrdering(Grid.RowAndColumn trgRowCol, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                       String targNodeID, int padNum, 
                       boolean onLeft, int targType, boolean startOnTop) {
      this.deltaRow = (startOnTop) ? trgRowCol.row : trgRowCol.row - Integer.MAX_VALUE;
      if (onLeft) {
        this.deltaRow += 0.5;
      }
      this.deltaCol = trgRowCol.col + 1;
      this.padNum = padNum;
      this.targType = targType;
      this.npsx = nps;
      this.targNodeID = targNodeID;      
    }    
    
    LinkLayoutOrdering(Grid.RowAndColumn srcRowCol, Grid.RowAndColumn trgRowCol, SpecialtyLayoutEngine.NodePlaceSupport nps,
                       String targNodeID, int padNum, int targType) {
      this.deltaRow = trgRowCol.row - srcRowCol.row;
      this.deltaCol = trgRowCol.col - srcRowCol.col;
      this.padNum = padNum;
      this.targType = targType;
      this.npsx = nps;
      this.targNodeID = targNodeID;       
    }

    @Override
    public int hashCode() {
      return ((int)Math.round(deltaRow + deltaCol + padNum + targType));
    }

    @Override
    public String toString() {
      return ("LinkLayoutOrdering: " + deltaRow + " " + deltaCol + " " + padNum + " " + targType);
    }
        
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof LinkLayoutOrdering)) {
        return (false);
      }
      
      LinkLayoutOrdering otherLLO = (LinkLayoutOrdering)other; 

      return ((this.deltaCol == otherLLO.deltaCol) &&
              (this.deltaRow == otherLLO.deltaRow) &&              
              (this.padNum == otherLLO.padNum) &&
              (this.targType == otherLLO.targType));      
    } 

    //
    // We do in the order NE, NW, SE, SW:
    //    
    
    private int quadrantOrder() {
      if (deltaRow <= 0.0) {
        return ((deltaCol > 0.0) ? 0 : 1);
      } else {
        return ((deltaCol > 0.0) ? 2 : 3);
      }
    } 
    
    //
    // Links entering on the bottom of a node is the same as shifting it down a
    // row.  Links entering on the left as a direct also get shifted down. 
    //    
    
    //private int padShift() {
    //  return (0);  // FIX ME! Implement this!!!
    //}    
    
    public int compareTo(LinkLayoutOrdering other) {
      
      if (this.equals(other)) {
        return (0);
      }

      //
      // Remember: Return neg if this is less than other
      //
            
      //
      // We do in the order NE, NW, SE, SW:
      //
      
      int myQuadrant = this.quadrantOrder();
      int otherQuadrant = other.quadrantOrder();
      if (myQuadrant != otherQuadrant) {
        return (myQuadrant - otherQuadrant);
      }
      
      //
      // Same quadrant.  Farther row always comes second:
      //
      
      if (this.deltaRow != other.deltaRow) {
        return ((Math.abs(this.deltaRow) > Math.abs(other.deltaRow)) ? 1 : -1);
      }
      
      //
      // Same quadrant and row.  Farther col usually comes second, EXCEPT if:
      // 1) nearer col is one to the right of the source
      // 2) nearer col pad is direct
      // 3) we are in the SE quadrant
      //      
      
      if (this.deltaCol != other.deltaCol) {        
        if (myQuadrant == 2) {
          double myDeltaAbs = Math.abs(this.deltaCol);
          double otherDeltaAbs = Math.abs(other.deltaCol);
          double nearest = Math.min(myDeltaAbs, otherDeltaAbs);
          if (nearest == 1) {
            LinkLayoutOrdering whichLLO = (myDeltaAbs == 1) ? this : other;
            boolean isDirect = landOKForDirect(whichLLO.targNodeID, whichLLO.npsx, whichLLO.padNum);
            if (isDirect) {
              return ((whichLLO == this) ? 1 : -1);
            }
          }
        }
        return ((Math.abs(this.deltaCol) > Math.abs(other.deltaCol)) ? 1 : -1);
      }
      
      //
      // Only thing left is padNum.  If the two target types are not the same (is that
      // possible if we survive all the way here?), we punt!
      //
      
      if (this.targType != other.targType) {
        throw new IllegalStateException();
      }
      
      INodeRenderer rend = npsx.getNodeProperties(this.targNodeID).getRenderer();
      return (rend.comparePads(this.padNum, other.padNum));
    }
  }
} 
