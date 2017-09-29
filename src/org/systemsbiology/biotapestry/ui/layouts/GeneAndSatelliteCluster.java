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

import java.awt.Rectangle;
import java.util.Set;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.analysis.NodeGrouper;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.freerender.GeneFree;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltGeneralMotif;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltMotif;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.analysis.Link;      
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.ui.RectangularTreeEngine;
import org.systemsbiology.biotapestry.ui.Grid;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.TaggedComparableInteger;

/***************************************************************************
**
** Represent genes with associated flunkie nodes
*/

public class GeneAndSatelliteCluster implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double LINK_ROOT_X_OFFSET_ = 40.0;   
  private static final double STACK_X_OFFSET_ = 80.0;  
  private static final double STACK_Y_DELTA_  = 50.0;
  
  private static final int BUBBLE_PAD_INCR_ = 3;
  private static final int BUBBLE_VERTICAL_INCR_ = 6;
  private static final int FIRST_BUBBLE_OFFSET_ = 12;
  private static final int PEN_TOP_DROP_Y_OFFSET_ = -3;
  private static final int PEN_DROP_OFFSET_ = -3;
  
  //
  // If we understimate the cluster width, we get backtracks on oubound
  // link traces. Toss in a little extra to handle minor errors:
  //
  
  private static final double COMPLEX_FANOUT_WIDTH_HACK_ = 20.0;
  private static final double FAN_GRID_PAD_ = 10.0;

  // If every inbound link is to the core, we have no fan-in.
  // If every fan-in node has one target, and it is the core,
  // and the node is tiny (bubble, slash, intercell), and it
  // has at most two links and none are from
  // the same source, it is simple.
  // Else it is complex.
  // The simple fan-in uses the orginal halo layout fan-in strategy.
  // Note this makes a single text or box node a complex fan in!
  
  private static final int NO_FAN_IN_      = 0;
  private static final int SIMPLE_FAN_IN_  = 1;
  private static final int COMPLEX_FAN_IN_ = 2;
  
  private static final int IS_CORE_    = 0;
  private static final int IS_FAN_IN_  = 1;
  private static final int IS_FAN_OUT_ = 2;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<String> feedbacks_;
  private HashSet<String> outputNodes_;  
  private HashSet<String> internalLinks_;
  private HashSet<String> fanInNodes_;
  private HashSet<String> fanOutNodes_;
  private HashSet<String> outboundLinks_;
  private HashSet<String> inboundLinks_;
  private boolean hasDirect_;

  private String coreID_;
   
  // Keys list all fan-in nodes; map to set of source ID for each node   
  private Map<String, Set<String>> penultimate_;
  // All inputs (srcs) into cluster, but not within cluster  
  private Set<String> inputs_;
  // Map of integer to pen ID
  private SortedMap<Integer, String> penOrder_;
  private HashMap<String, Set<String>> toCoreFromPen_;
 
  private TrackedGrid fanOutTrackedGrid_;  
  private TrackedGrid fanInTrackedGrid_;  
  private int fanInType_;
  private HashMap<String, PadCalculatorToo.PadResult> fanInPadPlans_;
  private Map<String, SpecialtyLayoutLinkData> fanInLinkPlans_;
  private Map<String, SpecialtyLayoutLinkData> inboundLinkPlans_;
  private GridLinkRouter fanInGlr_;
  
  private HashMap<String, PadCalculatorToo.PadResult> fanOutPadPlans_;
  private Map<String, SpecialtyLayoutLinkData> fanOutLinkPlans_;
  private Map<String, SpecialtyLayoutLinkData> outboundLinkPlans_;
  private Map<String, SpecialtyLayoutLinkData> feedbackLinkPlans_;
  private GridLinkRouter fanOutGlr_;
  private Map<String, SpecialtyLayoutLinkData> fanOutInboundLinkPlans_;
  private HashMap<String, List<GridLinkRouter.ClassifiedLink>> coreJumpers_;
  private HashSet<String> coreJumpersAlsoToCore_;
  private ArrayList<GridLinkRouter.RCRowCompare> srcOrder_;
  private ArrayList<GridLinkRouter.RCRowCompare> fanInTraceOrder_;
  private HashMap<String, Set<Integer>> padMap_;
  private DropDirectionOracle ddo_;
  private int numFOT_;
  private boolean coreLinkIsFanned_;
  private boolean isStacked_;
  private ClusterDims clusterDims_;
  private double traceOffset_;
  private boolean isTarget_;
  private HashMap<String, Integer> multiInCoreEstimates_;
  private boolean textToo_;
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
        
  public GeneAndSatelliteCluster(BTState appState, String coreID, boolean isStacked, 
                                 double traceOffset, boolean isTarget, boolean textToo) {
    
    appState_ = appState;
    coreID_ = coreID;
    penultimate_ = new HashMap<String, Set<String>>();
    toCoreFromPen_ = new HashMap<String, Set<String>>();
    inputs_ = new HashSet<String>();
    penOrder_ = new TreeMap<Integer, String>();    
   
    feedbacks_ = new HashSet<String>();
    outputNodes_ = new HashSet<String>();
    
    internalLinks_= new HashSet<String>();
    outboundLinks_= new HashSet<String>();
    inboundLinks_= new HashSet<String>();   
    fanInNodes_= new HashSet<String>();
    fanOutNodes_ = new HashSet<String>();
    fanInType_ = NO_FAN_IN_;
    padMap_ = new HashMap<String, Set<Integer>>();
    isStacked_ = isStacked;
    clusterDims_ = null;
    traceOffset_ = traceOffset;
    isTarget_ = isTarget;
    textToo_ = textToo;
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Clone. Only works correctly if the DropDirectionOracle has not bee set. (The DDO contains
  ** a list of GaSCs, so that woulds need to be a follow-on step....
  */

  @Override
  public GeneAndSatelliteCluster clone() {
    try {
      GeneAndSatelliteCluster retval = (GeneAndSatelliteCluster)super.clone();
      
      retval.feedbacks_ = (this.feedbacks_ == null) ? null : new HashSet<String>(this.feedbacks_);
      retval.outputNodes_ = (this.outputNodes_ == null) ? null : new HashSet<String>(this.outputNodes_);
      retval.internalLinks_ = (this.internalLinks_ == null) ? null : new HashSet<String>(this.internalLinks_);
      retval.fanInNodes_ = (this.fanInNodes_ == null) ? null : new HashSet<String>(this.fanInNodes_);
      retval.fanOutNodes_ = (this.fanOutNodes_ == null) ? null : new HashSet<String>(this.fanOutNodes_);
      retval.outboundLinks_ = (this.outboundLinks_ == null) ? null : new HashSet<String>(this.outboundLinks_);
      retval.inboundLinks_ = (this.inboundLinks_ == null) ? null : new HashSet<String>(this.inboundLinks_);

      if (this.penultimate_ != null) {
        retval.penultimate_ = new HashMap<String, Set<String>>();
        for (String key : this.penultimate_.keySet()) {
          Set<String> nextSet = this.penultimate_.get(key);
          retval.penultimate_.put(key, new HashSet<String>(nextSet));
        }
      } 
      
      retval.inputs_ = (this.inputs_ == null) ? null : new HashSet<String>(this.inputs_); 

      retval.penOrder_ = (this.penOrder_ == null) ? null : new TreeMap<Integer, String>(this.penOrder_);
      
      if (this.toCoreFromPen_ != null) {
        retval.toCoreFromPen_ = new HashMap<String, Set<String>>();
        for (String key : this.toCoreFromPen_.keySet()) {
          Set<String> nextSet = this.toCoreFromPen_.get(key);
          retval.toCoreFromPen_.put(key, new HashSet<String>(nextSet));
        }
      } 
  
      retval.fanOutTrackedGrid_ = (this.fanOutTrackedGrid_ == null) ? null : this.fanOutTrackedGrid_.clone();
      retval.fanInTrackedGrid_ = (this.fanInTrackedGrid_ == null) ? null : this.fanInTrackedGrid_.clone();
   
      if (this.fanInPadPlans_ != null) {
        retval.fanInPadPlans_ = new HashMap<String, PadCalculatorToo.PadResult>();
        for (String key : this.fanInPadPlans_.keySet()) {
          retval.fanInPadPlans_.put(key, this.fanInPadPlans_.get(key).clone());
        }
      } 
      if (this.fanInLinkPlans_ != null) {
        retval.fanInLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.fanInLinkPlans_.keySet()) {
          retval.fanInLinkPlans_.put(key, this.fanInLinkPlans_.get(key).clone());
        }
      }
      if (this.inboundLinkPlans_ != null) {
        retval.inboundLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.inboundLinkPlans_.keySet()) {
          retval.inboundLinkPlans_.put(key, this.inboundLinkPlans_.get(key).clone());
        }
      }
      if (this.fanOutPadPlans_ != null) {
        retval.fanOutPadPlans_ = new HashMap<String, PadCalculatorToo.PadResult>();
        for (String key : this.fanInPadPlans_.keySet()) {
          retval.fanOutPadPlans_.put(key, this.fanOutPadPlans_.get(key).clone());
        }
      }
      if (this.fanOutLinkPlans_ != null) {
        retval.fanOutLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.fanOutLinkPlans_.keySet()) {
          retval.fanOutLinkPlans_.put(key, this.fanOutLinkPlans_.get(key).clone());
        }
      }
      if (this.outboundLinkPlans_ != null) {
        retval.outboundLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.outboundLinkPlans_.keySet()) {
          retval.outboundLinkPlans_.put(key, this.outboundLinkPlans_.get(key).clone());
        }
      }
      if (this.feedbackLinkPlans_ != null) {
        retval.feedbackLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.feedbackLinkPlans_.keySet()) {
          retval.feedbackLinkPlans_.put(key, this.feedbackLinkPlans_.get(key).clone());
        }
      }
      if (this.fanOutInboundLinkPlans_ != null) {
        retval.fanOutInboundLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
        for (String key : this.fanOutInboundLinkPlans_.keySet()) {
          retval.fanOutInboundLinkPlans_.put(key, this.fanOutInboundLinkPlans_.get(key).clone());
        }
      }
      
      // 4/22/16: This is a duplication of the above, removed:
    //  if (this.fanOutInboundLinkPlans_ != null) {
    //    retval.fanOutInboundLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
    //    for (String key : this.fanOutInboundLinkPlans_.keySet()) {
    //      retval.fanOutInboundLinkPlans_.put(key, this.fanOutInboundLinkPlans_.get(key).clone());
    //    }
    //  }

      if (this.coreJumpers_ != null) {
        retval.coreJumpers_ = new HashMap<String, List<GridLinkRouter.ClassifiedLink>>();
        for (String key : this.coreJumpers_.keySet()) {
          List<GridLinkRouter.ClassifiedLink> coreJumpList = this.coreJumpers_.get(key);
          List<GridLinkRouter.ClassifiedLink> coreJumpListCopy = new ArrayList<GridLinkRouter.ClassifiedLink>();
          retval.coreJumpers_.put(key, coreJumpListCopy);
          int ns = coreJumpList.size();
          for (int i = 0; i < ns; i++) {
            coreJumpListCopy.add(coreJumpList.get(i).clone());
          }
        }  
      }
  
      retval.coreJumpersAlsoToCore_ = (this.coreJumpersAlsoToCore_ == null) ? null : new HashSet<String>(this.coreJumpersAlsoToCore_);
  
      if (this.srcOrder_ != null) {
        retval.srcOrder_ = new ArrayList<GridLinkRouter.RCRowCompare>();
        int ns = this.srcOrder_.size();
        for (int i = 0; i < ns; i++) {
          retval.srcOrder_.add(this.srcOrder_.get(i).clone());
        }
      }
      if (this.fanInTraceOrder_ != null) {
        retval.fanInTraceOrder_ = new ArrayList<GridLinkRouter.RCRowCompare>();
        int ns = this.fanInTraceOrder_.size();
        for (int i = 0; i < ns; i++) {
          retval.fanInTraceOrder_.add(this.fanInTraceOrder_.get(i).clone());
        }
      }
     
      if (this.padMap_ != null) {
        retval.padMap_ = new HashMap<String, Set<Integer>>();
        for (String key : this.padMap_.keySet()) {
          Set<Integer> nextSet = this.padMap_.get(key);
          retval.padMap_.put(key, new HashSet<Integer>(nextSet));
        }
      } 
  
      if (ddo_ != null) {
        throw new IllegalStateException();
      }

      retval.clusterDims_ = (this.clusterDims_ == null) ? null : clusterDims_.clone();
  
      retval.multiInCoreEstimates_ = (this.multiInCoreEstimates_ == null) ? null : new HashMap<String, Integer>(this.multiInCoreEstimates_);
  
      retval.fanInGlr_ = (this.fanInGlr_ == null) ? null : new GridLinkRouter(this.fanInGlr_, retval);
      retval.fanOutGlr_ = (this.fanOutGlr_ == null) ? null : new GridLinkRouter(this.fanOutGlr_, retval);
   
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  
  /***************************************************************************
  ** 
  ** Get the bounds of JUST the nodes:
  */
  
  public Rectangle getNodeOnlyBounds(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
   
    Rectangle2D useRect = TrackedGrid.layoutBounds(nps, irx, coreID_, textToo_);   
    Rectangle retval = UiUtil.rectFromRect2D(useRect);
    Point2D place = nps.getPosition(coreID_);
    retval.setLocation((int)place.getX(), (int)place.getY());
    
    Iterator<String> fiit = fanInNodes_.iterator();
    collectFanBounds(fiit, nps, irx, retval);
  
    Iterator<String> foit = fanOutNodes_.iterator();
    collectFanBounds(foit, nps, irx, retval);
    
  
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Aggregates node bounds in the given rectangle
  */

  private void collectFanBounds(Iterator<String> foit, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, Rectangle retval) {  
    while (foit.hasNext()) {
      String key = foit.next();
      Rectangle2D useRect = TrackedGrid.layoutBounds(nps, irx, key, textToo_);
      Rectangle tgb = UiUtil.rectFromRect2D(useRect);
      Point2D place = nps.getPosition(key);
      tgb.setLocation((int)place.getX(), (int)place.getY());
      Bounds.tweakBounds(retval, tgb);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Returns set of nodes (IDs) in cluster
  */

  public Set<String> allNodesInCluster() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(coreID_);
    retval.addAll(fanInNodes_);
    retval.addAll(fanOutNodes_);
    return (retval);
  }    
  
  /***************************************************************************
  ** 
  ** Answer if node is in cluster
  */

  public boolean isInCluster(String nodeID) {      
    if (coreID_.equals(nodeID)) {
      return (true);
    } else if (fanInNodes_.contains(nodeID)) {
      return (true);
    } else if (fanOutNodes_.contains(nodeID)) {
      return (true);
    }
    return (false);
  }  
 
  /***************************************************************************
  ** 
  ** Get the core ID for the cluster
  */

  public String getCoreID() {
    return (coreID_);
  }
  
  
  /***************************************************************************
  ** 
  ** Do we use left drops?
  */

  public boolean useLeftDropsForStackedCore() {
    return (fanInType_ == SIMPLE_FAN_IN_);
  }
  
  /***************************************************************************
  ** 
  ** Do we need horizontal traces on the inbound side?
  */

  private boolean needsHorizontalTracesToFanInOrCore(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    if (isTarget_) {
      return (fanInType_ == COMPLEX_FAN_IN_);
    } else if (isStacked_) {
      return (fanInType_ == COMPLEX_FAN_IN_);
    } else {
      return (((fanInType_ != SIMPLE_FAN_IN_) || //<-if set to == COMPLEX, we are messed in general layout stacking! 
              ((coreJumpers_ != null) && !coreJumpers_.isEmpty()) ||
              ((fanInType_ == SIMPLE_FAN_IN_) && hasInboundLinksToFanOut(nps))));
    }
  }
  
  /***************************************************************************
  ** 
  ** Do we need horizontal traces on the outbound side?
  */

  private boolean needsHorizontalTracesToFanOut(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    if (isTarget_) {
      return (false);
    } else if (isStacked_) {
      return (false);
    } else {
      return (((fanInType_ != SIMPLE_FAN_IN_) || //<-if set to == COMPLEX, we are messed in general layout stacking! 
              ((coreJumpers_ != null) && !coreJumpers_.isEmpty()) ||
              ((fanInType_ == SIMPLE_FAN_IN_) && hasInboundLinksToFanOut(nps))));
    }
  }

  /***************************************************************************
  ** 
  ** Are there links inbound to the fan OUT side?
  */

  public boolean hasInboundLinksToFanOut(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    if ((fanOutNodes_ == null) || fanOutNodes_.isEmpty()) {
      return (false);
    }
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      if (linkToFanOut(linkID, nps)) {
        return (true);
      }
    }
    return (false);
  }
 
  /***************************************************************************
  ** 
  ** Get the inbound link IDs
  */

  public Set<String> getInboundLinks() {
    return (inboundLinks_);
  }  

  /***************************************************************************
  ** 
  ** In original target-only use, existing trace ordering sets fan-in ordering:
  */
  
  public void orderByTraceOrder(List<String> orderedSources, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    int numSrc = orderedSources.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = orderedSources.get(i);
      Set<String> pens = pensAsTargets(srcID);
      Iterator<String> pit = pens.iterator();
      while (pit.hasNext()) {
         String pen = pit.next();
         orderPenultimate(pen, nps); 
      } 
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Locate the GASC.  Note that basePos is on the upper left of the cluster. "As Target" means we are not doing
  ** any fan-out placement in this routine, so it is usable for both source and target nodes; the former get an added step.
  ** This places the nodes, based on previously calculated ClusterDims. No link routing is done here.
  */
  
  public void locateAsTarget(Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, Double maxHeight) {   

    //
    // Any fan-ins not placed are located now:
    //
    
    ClusterDims oh = getClusterDims(nps, irx);
    
    //
    // Traces needed along the top push stuff down, which is what the oh.horizTraceAddition does.
    // 
    //
    // We want the bottoms of the clusters to match. So we push the cluster down (extraBump) if it is less than the
    // maximum cluster height for a row:
    //
    
    double nextHeight = oh.height + oh.horizTraceAddition;
    double extraBump = 0.0;
    if ((maxHeight != null) && (maxHeight.doubleValue() > nextHeight)) {
      extraBump = maxHeight.doubleValue() - nextHeight;
    }
    
    //
    // Find the rectangle needed by the core node. As a rectangle, with origin at the node placement point,
    // extra tells us how far to the right the core is being placed.
    //
    
    Point2D srcPt;
    Rectangle2D useRect = TrackedGrid.layoutBounds(nps, irx, coreID_, textToo_);
    double extra = -useRect.getX();
 
    //
    // Figure out where the core node is placed, using a relative offset from the basePos.
    //
    // We actually need a horizontal trace component when we are a pure target only with complex fan-ins;
    //
    
    if (fanInType_ != COMPLEX_FAN_IN_) {
      // Core node placement based on needed rect (extra for X), vertically based on needed trace height, the ClusterDim calc of
      // core height, and the bump to make everybody even:
      Vector2D extraOffset = new Vector2D(extra, oh.horizTraceAddition + oh.offsetToGascPlacement + extraBump);
      if (fanInType_ == SIMPLE_FAN_IN_) {
        orderFanInOrphans(nps); // Order before placement!
        srcPt = extraOffset.add(basePos);
        // Fan in is placed WRT the core location:
        locateSimpleFanIns(srcPt, nps, irx);
      } else { // NO FAN_IN
        srcPt = extraOffset.add(basePos);                
      }
    } else { // COMPLEX FAN IN
      // Fan-in Y is: (base position)
      // Complex fan in placed WRT top left point.
      Point2D fanPos = new Point2D.Double(basePos.getX(), basePos.getY() + oh.horizTraceAddition + oh.offsetToFanInTop + extraBump);
      placeFan(fanInTrackedGrid_, fanPos, nps);
      // core placement X depends on core node width (extra) plus tracked grid width:
      Vector2D extraOffset = new Vector2D(extra + fanInTrackedGrid_.getWidth(), oh.horizTraceAddition + oh.offsetToGascPlacement + extraBump);
      srcPt = extraOffset.add(basePos);
    }
    
    UiUtil.forceToGrid(srcPt, UiUtil.GRID_SIZE);
    nps.setPosition(coreID_, srcPt);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get Y needed for pen (penultimate) nodes, i.e. nodes in a simple fan-in.  Make sure orderFanInOrphans called first!
  */
  
  public double spaceForPenNodes(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    int penNum = penOrder_.size();
    if (penNum == 0) {
      return (0.0);
    }   
    Set<String> toCore = linksToCore(nps);
    int dirCount = toCore.size();
    dirCount += 2;  // pad it...
    int firstBub = (dirCount > FIRST_BUBBLE_OFFSET_) ? dirCount : FIRST_BUBBLE_OFFSET_; 
    
    double bubY = UiUtil.forceToGridValue((firstBub * UiUtil.GRID_SIZE) + 
                                          ((penNum - 1) * (BUBBLE_VERTICAL_INCR_ * UiUtil.GRID_SIZE)), 
                                          UiUtil.GRID_SIZE);     
    return (bubY);
  }
  
  /***************************************************************************
  ** 
  ** Does the node location calculations for a single node in a simple fan-in.
  */
  
  private Point2D simpleFanPosCalc(int orderNum, String penID, Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    int padNum = penIDToPadnum(nps, penID);
    Vector2D offset = landingPadToOffset(padNum, nps, irx, coreID_); 
    Point2D padPoint = offset.add(basePos);
   
    Set<String> toCore = linksToCore(nps);
    int dirCount = toCore.size();  
    dirCount += 2;  // pad it...
    int firstBub = (dirCount > FIRST_BUBBLE_OFFSET_) ? dirCount : FIRST_BUBBLE_OFFSET_; 
      
    double bubY = UiUtil.forceToGridValue(padPoint.getY() - 
                                         (firstBub * UiUtil.GRID_SIZE) - 
                                         (orderNum * (BUBBLE_VERTICAL_INCR_ * UiUtil.GRID_SIZE)), UiUtil.GRID_SIZE);     
    padPoint.setLocation(padPoint.getX(), bubY);
    return (padPoint);
  }
  
  /***************************************************************************
  ** 
  ** Turns the nodes in a simple fan in (usually a bubble) to white, links to black. Currently only used in
  ** "Halo" layout, and coloring non-bubble nodes white is not advised.
  */
  
  public void colorTheCluster(Genome genome, Map<String, String> nodeColors, Map<String, String> linkColors) {

    Set<String> penKeys = penultimate_.keySet();

    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String lsource = link.getSource();
      if (penKeys.contains(lsource)) {
        nodeColors.put(lsource, "white");
        linkColors.put(link.getID(), "black");
      }
    }
    nodeColors.put(coreID_, "black");
    return;
  }

  /***************************************************************************
  ** 
  ** This is where pad changes get set down.  NOTE that core changes >>only occur here!
  */
  
  public void calcPadChanges(GenomeSubset subset, List<String> traceOrder, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                             Map<String, Integer> nodeLengthChanges, Map<String, Integer> extraGrowthChanges) {
    Genome baseGenome = subset.getBaseGenome();
    Node node = baseGenome.getNode(coreID_);    
    boolean isGeneCore = (node.getNodeType() == Node.GENE);
    boolean doCorePads = true;
    if (isGeneCore) {
      Gene gene = baseGenome.getGene(coreID_);
      // no changing of core if we have subregions!
      if (gene.getNumRegions() != 0) {
        doCorePads = false;
      }
    }
    int currPad = 0;

    //
    // Fan in side:
    //
    
    // This does internal fan-in pad changes:
    if (fanInType_ == COMPLEX_FAN_IN_) {
      mergePrecalcPads(fanInNodes_, fanInPadPlans_, nps);
    }
    
    //
    // This does the core changes.  THIS IS WHERE CORE PADS GET CHANGED!!!
    //
    
    if (isGeneCore) {
      if (doCorePads) {
        currPad = calcPadChangesForCore(subset, traceOrder, nps);
      } else {      
        if (!fanInNodes_.isEmpty() && (fanInType_ == COMPLEX_FAN_IN_)) {
          currPad = changeFanToCorePads(fanInNodes_, nps, currPad, fanInPadPlans_, true, null, true);    
        }
      }     
    } else {
      calcPadChangesForNonGeneCore(subset, traceOrder, nps);
    }
 
    //
    // If no core, we still need to wiggle simple fan in nodes:
    //
    
    if ((fanInType_ == SIMPLE_FAN_IN_) && (!doCorePads)) {
      currPad = simpleFanInPadChangesForFan(subset, traceOrder, nps, null, currPad); 
    }

    //
    // Fan out side:
    //
    
    if (!fanOutNodes_.isEmpty()) {
      // This does internal fan pad changes:
      mergePrecalcPads(fanOutNodes_, fanOutPadPlans_, nps);
    }
 
    // outbound pads:
    
    changeOutboundPads(nps, isGeneCore);

    //
    // Figure out length changes:
    //
    
    // Genes:
    if ((isGeneCore) && (doCorePads) && (currPad < 0)) {
      Gene gene = baseGenome.getGene(coreID_);
      int currPads = gene.getPadCount();
      if ((DBGene.DEFAULT_PAD_COUNT - currPads) > currPad) {
        nodeLengthChanges.put(coreID_, new Integer(DBGene.DEFAULT_PAD_COUNT - currPad));
      }
    }
    
    //
    // All other nodes:
    //
    
    Iterator<String> pmkit = padMap_.keySet().iterator(); 
    while (pmkit.hasNext()) {
      String key = pmkit.next();
      Node fNode = baseGenome.getNode(key);
      Set<Integer> usedPads = padMap_.get(key);
      int minUsed = Integer.MAX_VALUE;
      Iterator<Integer> upit = usedPads.iterator();
      while (upit.hasNext()) {
        Integer usedPad = upit.next();
        int upVal = usedPad.intValue();
        if (upVal < minUsed) {
          minUsed = upVal;
        }
      }
      if (minUsed >= 0) {  // no extension needed
        continue;
      }
      int nodeType = fNode.getNodeType();
      int padInc = DBNode.getPadIncrement(nodeType);
      // no pad inc means it cannot be grown:
      if (padInc == 0) {
        continue;
      }
      
      minUsed = -(int)UiUtil.forceToGridValueMax(-minUsed, padInc);
      int currPads = fNode.getPadCount();
      int defPad = DBNode.getDefaultPadCount(nodeType); 
      int needed = defPad - minUsed;
      if (needed > currPads) {
        nodeLengthChanges.put(key, new Integer(needed));
      }
    }
    
    //
    // Figure out if we need to tweak growth direction:
    //
    
    Iterator<String> fiit = fanInNodes_.iterator();
    while (fiit.hasNext()) {
      String key = fiit.next();
      needExtraGrowthChange(key, baseGenome, nodeLengthChanges, extraGrowthChanges, true);
    }
    Iterator<String> foit = fanOutNodes_.iterator();
    while (foit.hasNext()) {
      String key = foit.next();
      needExtraGrowthChange(key, baseGenome, nodeLengthChanges, extraGrowthChanges, false);
    }    
    needExtraGrowthChange(coreID_, baseGenome, nodeLengthChanges, extraGrowthChanges, false);    
 
    return;
  }  
  
  
  /***************************************************************************
  ** 
  ** Figure out if we need to tweak growth direction:
  */
  
  public void needExtraGrowthChange(String key, Genome genome,
                                    Map<String, Integer> nodeLengthChanges, Map<String, Integer> extraGrowthChanges, boolean isFanIn) {    
    Node fNode = genome.getNode(key);
    int nodeType = fNode.getNodeType();
    int currPads = fNode.getPadCount();
    Integer newPads = nodeLengthChanges.get(key);
    int needPads = (newPads == null) ? currPads : newPads.intValue();
    int defaultPads = DBNode.getDefaultPadCount(nodeType);
    if ((needPads <= defaultPads) || !NodeProperties.usesGrowth(nodeType)) {
      return;
    }
    int growthType = NodeProperties.HORIZONTAL_GROWTH;
    if ((fanInType_ == SIMPLE_FAN_IN_) && isFanIn) {
      growthType = NodeProperties.VERTICAL_GROWTH;
    }
    extraGrowthChanges.put(key, new Integer(growthType));      
    return;
  }    

  /***************************************************************************
  ** 
  ** Do final inbound link routing (original target-only uses) - halo layout
  */

  public void finalLinkRouting(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps,
                               DataAccessContext irx, 
                               MetaClusterPointSource mcps, SpecialtyLayoutLinkData specIn, LinkPlacementState lps) {
    
    List<String> linksPerClust = linkIDsPerClusterOldStyle(srcID, nps);
    //
    // Have to sort the possible multiple links per cluster 
    // to go left-to-right to build this correctly:
    //

    List<Point2D> dropPts = null;
    TreeMap<Double, List<String>> sortByX = new TreeMap<Double, List<String>>();
    HashMap<String, List<Point2D>> holdDrops = new HashMap<String, List<Point2D>>();
    
    Set<String> linksToCore = linksToCore(nps);
    int numlpc = linksPerClust.size();
    for (int j = 0; j < numlpc; j++) {
      String linkID = linksPerClust.get(j);
      if (linksToCore.contains(linkID)) {
        dropPts = directLinkDropPoints(linkID, nps, irx, mcps); 
      } else {
        dropPts = doLinkToDropPoints(linkID, nps, irx, mcps);
      }
      double firstX = dropPts.get(0).getX();
      Double firstXKey = new Double(firstX);
      List<String> linksPerX = sortByX.get(firstXKey);
      if (linksPerX == null) {
        linksPerX = new ArrayList<String>();
        sortByX.put(firstXKey, linksPerX);
      }
      linksPerX.add(linkID);
      holdDrops.put(linkID, dropPts);
    }

    boolean firstDrop = true;
    Iterator<Double> sbxit = sortByX.keySet().iterator();
    while (sbxit.hasNext()) {
      Double xVal = sbxit.next();
      List<String> linksPerX = sortByX.get(xVal);
      int numLinks = linksPerX.size();
      for (int i = 0; i < numLinks; i++) {
        String linkID = linksPerX.get(i);
        specIn.startNewLink(linkID);
        dropPts = holdDrops.get(linkID);
        if (!firstDrop) {
          specIn.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.lastAttach));
          specIn.addPositionListToLink(linkID, SpecialtyLayoutLinkData.TrackPos.convertPointList(dropPts));
          lps.lastAttach = dropPts.get(0);
          continue;
        }
        firstDrop = false;
        if (lps.isRoot) {
          lps.isRoot = false;
          specIn.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.runPt));
          lps.lastRun = lps.runPt;
        } else if (lps.isRunStart) {
          lps.isRunStart = false;
          specIn.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.lastRun));
          specIn.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.runPt));
          lps.lastRun = lps.runPt;
        } else {
          specIn.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lps.lastAttach));
        }
        specIn.addPositionListToLink(linkID, SpecialtyLayoutLinkData.TrackPos.convertPointList(dropPts));
        lps.lastAttach = dropPts.get(0);
      }
    }
    List<String> lastLinkList = sortByX.get(sortByX.lastKey());
    String lastLink = lastLinkList.get(lastLinkList.size() - 1);
    dropPts = holdDrops.get(lastLink);
    lps.lastAttach = dropPts.get(0);
    return;
  }

  /***************************************************************************
  ** 
  ** Fix me - get this formalized
  */

  public Map<String, SpecialtyLayoutLinkData> internalLinkRoutingForTarget(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {      
    if (fanInType_ == COMPLEX_FAN_IN_) { 
      Point2D upperLeftFanInPt = getClusterDims(nps, irx).getFanInCorner(nps);
      return (fanInGlr_.convertInternalLinks(fanInLinkPlans_, upperLeftFanInPt, nps, irx, coreID_));
    }
    return (new HashMap<String, SpecialtyLayoutLinkData>());
  }

  /***************************************************************************
  ** 
  ** Handle inbound link routing for the gascluster. THIS IS METHOD THAT GETS INBOUND LINKS ROUTED,
  ** including inbound links going straight over to the fan-out.
  */
  
  public void inboundLinkRouting(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                 DataAccessContext irx, 
                                 SpecialtyLayoutLinkData sin, 
                                 MetaClusterPointSource lps, boolean topGASC) {

    List<String> linksPerClust = linkIDsPerCluster(srcID, nps);
    if ((linksPerClust == null) || linksPerClust.isEmpty())  {
      return;
    }
    
    boolean linkFromTop = (ddo_ == null) ? true : ddo_.comingFromTop(srcID, this);
 
    if (fanInType_ != COMPLEX_FAN_IN_) {
      //
      // Make sure we have a starting Y:
      //
      if (lps.needsTraceYUpdate()) {
        Map<String, Integer>  inbound = getInboundSourceOrder(nps, nps.getPadChanges());
        double cdropY = inboundTraceY(srcID, false, inbound, nps, irx);
        lps.setTargetTraceY(cdropY);
      }
      // Note that this handles fanout inbound routing as well!!!
      simpleFanInInboundLinkRouting(linksPerClust, srcID, nps, irx, sin, lps, linkFromTop, topGASC);
    } else {      
      boolean fanOutDone = false;
      // This is done because the link points need to be created first to avoid link backtracks!
      if (linkFromTop) {
        fanOutInboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
        fanOutDone = true;
      }
      complexFanInInboundLinkRouting(srcID, nps, irx, sin, lps, true);
      if (!fanOutDone) {
        fanOutInboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
      }
    }
    
    return;
  }  


  /***************************************************************************
  ** 
  ** A function
  */
  
  public void calcOrientChanges(SpecialtyLayoutEngine.NodePlaceSupport nps, Layout lo, Map<String, Integer> orientChanges) {
    HashSet<String> allNodes = new HashSet<String>();
    allNodes.add(coreID_);
    allNodes.addAll(fanInNodes_);
    allNodes.addAll(fanOutNodes_);    
    
    Iterator<String> finit = allNodes.iterator();
    while (finit.hasNext()) {
      String nodeID = finit.next();
      NodeProperties np = lo.getNodeProperties(nodeID);
      Node node = nps.getNode(nodeID);
      int orient = np.getOrientation();
      if (NodeProperties.getOrientTypeCount(node.getNodeType()) == 0) {
        continue;
      } else if ((fanInType_ == SIMPLE_FAN_IN_) && fanInNodes_.contains(nodeID)) {
        if (orient != NodeProperties.DOWN) {
          orientChanges.put(nodeID, new Integer(NodeProperties.DOWN));
        }      
      } else if (orient != NodeProperties.RIGHT) {
        orientChanges.put(nodeID, new Integer(NodeProperties.RIGHT));
      }      
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** get an iterator over all the sources
  */

  public Iterator<String> getAllSourceIterator() {
    return (outputNodes_.iterator());
  }
  
  /***************************************************************************
  ** 
  ** get sources to core
  */
  
  public Set<String> srcsToCore(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    return (srcsForLinks(nps, linksToCore(nps))); 
  }
  
  /***************************************************************************
  ** 
  ** get links to core
  */
  
  public Set<String> linksToCore(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    HashSet<String> linksToCore = new HashSet<String>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String targetID = link.getTarget();
      if (targetID.equals(coreID_)) {
        linksToCore.add(linkID);
      }
    }
    return (linksToCore);
  }
  
  /***************************************************************************
  ** 
  ** Get sources of multi-links to core:
  */
  
  public Set<String> multiSrcsToCore(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    HashMap<String, Integer> srcToCount = new HashMap<String, Integer>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String targetID = link.getTarget();
      if (targetID.equals(coreID_)) {
        String srcID = link.getSource();
        Integer count = srcToCount.get(srcID);
        if (count == null) {
          count = new Integer(1);
        } else {
          count = new Integer(count.intValue() + 1);
        }
        srcToCount.put(srcID, count);
      }
    }
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> s2ckit = srcToCount.keySet().iterator();
    while (s2ckit.hasNext()) {
      String srcID = s2ckit.next();
      Integer count = srcToCount.get(srcID);
      if (count.intValue() > 1) {
        retval.add(srcID);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get sources to Fanin
  */
  
  public Set<String> srcsToFanin(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    return (srcsForLinks(nps, linksToFanin(nps)));
  }
  
 /***************************************************************************
  ** 
  ** Get sources for the given links
  */
  
  private Set<String> srcsForLinks(SpecialtyLayoutEngine.NodePlaceSupport nps, Set<String> links) { 
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> ilit = links.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String linkSrcID = link.getSource();      
      retval.add(linkSrcID);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get links to Fanin
  */
  
  public Set<String> linksToFanin(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    HashSet<String> srcsToFanin = new HashSet<String>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String targetID = link.getTarget();
      if (fanInNodes_.contains(targetID)) {
        srcsToFanin.add(linkID);
      }
    }
    return (srcsToFanin);
  }
 
  /***************************************************************************
  ** 
  ** get sources to fanout
  */
  
  private Set<String> srcsToFanout(SpecialtyLayoutEngine.NodePlaceSupport nps) { 
    HashSet<String> srcsToFanout = new HashSet<String>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String targetID = link.getTarget();
      String linkSrcID = link.getSource(); 
      if (fanOutNodes_.contains(targetID)) {
        srcsToFanout.add(linkSrcID);
      }
    }
    return (srcsToFanout);
  }

  /***************************************************************************
  ** 
  ** get the height, width, and offsets
  */

  public ClusterDims getClusterDims(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    if (clusterDims_ != null) {
      return (clusterDims_);
    }

    Node node = nps.getNode(coreID_);
    NodeProperties np = nps.getNodeProperties(coreID_);
    INodeRenderer rend = np.getRenderer();
    double coreHeight = rend.getGlyphHeightForLayout(node, irx);
    
    //
    // For genes with regions, whole height increases.
    //
    
    boolean hasRegions = false; 
    
    //
    // Top to placement says how far from the top of the core glyph do we go to get to node placement point. Note this
    // is 0.0 for genes, since placement point is at the top left.
    //
    double topToPlacement;
    if (node.getNodeType() == Node.GENE) {
      hasRegions = ((Gene)node).getNumRegions() != 0;
      if (hasRegions && (node.getName().length() > GeneFree.SHORTIE_CHAR)) {
        coreHeight += UiUtil.GRID_SIZE * 4.0;
      }
      topToPlacement = 0.0;
    } else {
      // Fix for Issue #255. We need to pad the core height a tiny bit for non-gene cores
      coreHeight += UiUtil.GRID_SIZE * 2.0;
      topToPlacement = coreHeight / 2.0;
      topToPlacement = UiUtil.forceToGridValueMax(topToPlacement, UiUtil.GRID_SIZE);
    }
    
    //
    // Figure out the space needed for traces going above the GAScluster, both over the
    // fan-in and over the fan-out.
    //
    
    double inTraces = 0.0;
    // I.e., a target or stacked, with a complex fan-in, OR
    // NOT a simple fanin (includes no fan in), OR has corejumpers, OR
    // a simple fan-in with links going to a fan-out:
    if (needsHorizontalTracesToFanInOrCore(nps)) {     
      if (fanInType_ == COMPLEX_FAN_IN_) {
        Set<String> l2fi = linksToFanin(nps);
        Set<String> l2c = linksToCore(nps);
        HashSet<String> all = new HashSet<String>(l2fi);
        all.addAll(l2c);
        // 04-22-16: Previously, we were removing choppable links from the reservation count, but this
        // is WRONG, since the chopped links will still have empty tracks anyway (they are RESERVED after all).
        //  Set<String> canChopLinks = srcsForLinks(nps, canChop(nps, new ArrayList<String>(all), null));
        int gottaHopIn = fanInGlr_.getTopReservations();
        //   gottaHopIn -= canChopLinks.size();
        inTraces = (UiUtil.GRID_SIZE * (gottaHopIn));    
      } else {
        int gottaHopIn = srcsToCore(nps).size();
        inTraces = (traceOffset_ * (gottaHopIn));
      }
      inTraces += traceOffset_;  // Padding...sigh...
    }
    
    //
    // In stacked layout cases, the outTraces is just the size of the fan out top traces. 
    // If we need horizontal traces (e.g. diagonal layouts) we bump the traces down to below the 
    // tracks coming in from sources. We bump it further to be below traces to fan in.
    // 7/06/16: I think this reverse engineers what is going on in diagonal/alternating case:
    // ---------------------------
    // Traces from exterior sources to fan out
    // ---------------------------
    // Traces to fan in (in traces)
    // ---------------------------
    // Fan out tracks (top reservations)
    // ---------------------------
    //
    
    double outTraces = 0.0;
    double topRes = 0.0;
    if (!fanOutNodes_.isEmpty()) {
      int gottaHopOut = fanOutGlr_.getTopReservations();
      outTraces = (UiUtil.GRID_SIZE * (gottaHopOut));
      // THIS DOES NOT APPLY TO STACKED LAYOUTS!
      if (needsHorizontalTracesToFanOut(nps)) {
        topRes = outTraces; // Records the top reservations we need to handle ONLY diagonal, alternating (not stacked) layouts
        int gottaHopOver = srcsToFanout(nps).size();
        outTraces += (traceOffset_ * (gottaHopOver));
        outTraces += inTraces;
        inTraces += (traceOffset_ * (gottaHopOver));
      }
    }

    int launchPad = TrackedGrid.launchForGrid(coreID_, nps);    
    Vector2D gridLaunchPadOffset = TrackedGrid.launchPadToOffset(nps, irx, launchPad, coreID_);
      
    //
    // Figure out if we need to make sure there is enough
    // space for a simple autofeedback:
    //
    
    boolean needFeedMin = false;
    Iterator<String> ilit = internalLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);      
      String src = link.getSource();
      String trg = link.getTarget();
      if (src.equals(coreID_) && src.equals(trg)) {
        needFeedMin = true;
        break;
      }
    }
    
    //
    // FanIn height calculation, which is increased if we have a multi-link comb
    // that needs its own dedicated track:
    //
    
    double fanIn = 0.0;
    double multiLink = 0.0;
    if (fanInType_ == SIMPLE_FAN_IN_) {
      orderFanInOrphans(nps);
      fanIn = spaceForPenNodes(nps);
    } else {
      if (!fanInNodes_.isEmpty()) {
        fanIn = fanInTrackedGrid_.getHeight();
      } else { // This is for the non-fan-in case:
        if (node.getNodeType() != Node.GENE) {
          fanIn = 20.0; // Non-gene padding!
        }
        //
        // Special case!  We need to inflate the height for stacked cases where we
        // have multiple-link sources and no fan-out.  This is
        // because they need to branch right above the gene:
        //
        if (isStacked_ && fanOutNodes_.isEmpty()) {
          Set<String> ms2c = multiSrcsToCore(nps);
          int multiSrcs = ms2c.size();
          if (multiSrcs != 0) {
            multiLink = multiSrcs * UiUtil.GRID_SIZE;
          }
        }
      }
    }
    
    //
    // Responsive to Issue 237. If a gene has regions, the final drops go willy-nilly, and
    // we need to make sure everybody has their own track. Also need to do the extra drop 
    // allocation with multi-link combs in a complex fan-in. COMBINE WITH ABOVE CASE?
    //
    
    if (isStacked_ && ((hasRegions) || (fanInType_ == COMPLEX_FAN_IN_))) {
      Set<String> ms2c = multiSrcsToCore(nps);
      int multiSrcs = ms2c.size();
      if (multiSrcs != 0) {
        multiLink = multiSrcs * 2.0 * UiUtil.GRID_SIZE;
      }
    }
 
    //
    // Figure placement of the fan-out block. Placement here (vOffset) is with respect to core launch point.
    //
    
    double fanOut = 0.0;
    double vOffset = 0.0;
    if (!fanOutNodes_.isEmpty()) {
      fanOut = fanOutTrackedGrid_.getHeight();
      vOffset = fanOutTrackedGrid_.getExactVerticalOffset(nps, irx, coreID_);
    }
    
    // Simple fan ins don't have a grid, will not be used!
    // For grids, this says where to place the fan in RELATIVE TO THE CORE NODE PLACEMENT!
    // Note that upperLeftFanInOffset is used in the ClusterDims.getFanInCorner() call, which is used to place
    // link >>tracks<< coming in over the fan-in. HOWEVER, actual fan-in placement uses oh.horizTraceAddition + oh.offsetToFanInTop
    
    Vector2D upperLeftFanInOffset = (fanInTrackedGrid_ == null) ? null
                                                                : new Vector2D(-fanInTrackedGrid_.getWidth(),
                                                                               -fanInTrackedGrid_.getHeight());

    Rectangle2D useRect = TrackedGrid.layoutBounds(nps, irx, coreID_, textToo_);
    double width = UiUtil.forceToGridValueMax(useRect.getWidth(), UiUtil.GRID_SIZE);
    double offsetToFanInTop = 0.0;
    
    //
    // Fix for Issue #237. This gets the fan in LINKS correctly placed
    //
    
    if (upperLeftFanInOffset != null) {
      upperLeftFanInOffset = upperLeftFanInOffset.add(new Vector2D(useRect.getX(), useRect.getY()));
    }

    double bottomOfFanIn = offsetToFanInTop + fanIn;
    double offsetToGascPlacement = bottomOfFanIn + topToPlacement;
    double bottomOfCore = offsetToGascPlacement - topToPlacement + coreHeight;
    double coreLaunch = offsetToGascPlacement + gridLaunchPadOffset.getY();
    // The addition of topRes addresses Issue #249. The true fan out top needs to include the top reservations
    // in the diagonal layout case.
    double fanOutTop = coreLaunch - vOffset - topRes;
    double fanOutBottom = fanOutTop + fanOut;
    
    double feedTop = (needFeedMin) ? coreLaunch - STACK_Y_DELTA_ : coreLaunch;
    // Continue special case for multi-link sources:
    feedTop -= multiLink;
       
    // If fan out is taller, gotta shift everybody down:
    
    double downshift = 0.0;
     
    if (fanOutTop < offsetToFanInTop) {
      downshift = offsetToFanInTop - fanOutTop;
    }
    if (feedTop < offsetToFanInTop) {
      if (feedTop < fanOutTop) {
        downshift = offsetToFanInTop - feedTop;
      }
    }
    if (downshift > 0.0) {
      feedTop += downshift;
      offsetToFanInTop += downshift;
      bottomOfFanIn += downshift;
      offsetToGascPlacement += downshift;
      bottomOfCore += downshift;
      coreLaunch += downshift;
      fanOutTop += downshift;
      fanOutBottom += downshift;
    }
    
    double traceTopIn = offsetToFanInTop - inTraces;
    double traceTopOut = fanOutTop - outTraces;
    double traceTop = (traceTopIn < traceTopOut) ? traceTopIn : traceTopOut; 
    if (feedTop < traceTop) {
      traceTop = feedTop;
    }

    //
    // Height is maximum of core bottom or fanOutBottom. *Total* height for the cluster is
    // the height *plus* the trace addition on the top:
    //
    
    double horizTraceAddition = -traceTop;
    double height = (bottomOfCore > fanOutBottom) ? bottomOfCore : fanOutBottom;  
    
    //
    // Previously made the height divisible by 2 to still end up on grid (2.0 * GRID_SIZE)
    // But then the genes end up wiggling up and down by GRID_SIZE due to the pad.  JUST
    // don't divide by 2 without regridding!  
    // Note that result will end up with easily ~20 extra pad, since intraces
    // is padded:
    //
    
    height = UiUtil.forceToGridValueMax(height, UiUtil.GRID_SIZE);
   
    //
    // Fan-out offset support:
    //
  
    int numRow = (fanOutTrackedGrid_ == null) ? 0 : fanOutTrackedGrid_.getNumRows();
    double multiRowShift = UiUtil.GRID_SIZE * (numFOT_ + ((coreLinkIsFanned_) ? 1.0 : 0.0));
    Vector2D targOffset = new Vector2D(STACK_X_OFFSET_, -vOffset);
    Vector2D fullOffset = targOffset.add(gridLaunchPadOffset);
    Vector2D linkShiftOffset = new Vector2D((numRow > 1) ? multiRowShift : 0.0, 0.0);  
      
    if (!fanOutNodes_.isEmpty()) {
      width += (fanOutTrackedGrid_.getWidth() + COMPLEX_FANOUT_WIDTH_HACK_);
    }
 
    if (!fanInNodes_.isEmpty()) {
      // Simple fan ins don't have a grid!
      if (fanInType_ == COMPLEX_FAN_IN_) {
        width += fanInTrackedGrid_.getWidth() + FAN_GRID_PAD_;
      }
    } 
    
    //
    // Padding to right of gene going over to fan out grid:
    //
    
    width += linkShiftOffset.getX();
    
    // Make sure still end up on grid points:
    
    width = UiUtil.forceToGridValueMax(width, UiUtil.GRID_SIZE);
  
    
   // Complex Fan-in placement by: basePos.getY() + oh.horizTraceAddition + oh.offsetToFanInTop + [extraBump (to even out the bottom of the row)]
    
    clusterDims_ = new ClusterDims(height, offsetToFanInTop, fanOutTop, offsetToGascPlacement, horizTraceAddition, 
                                   gridLaunchPadOffset, upperLeftFanInOffset, 
                                   fullOffset, linkShiftOffset, vOffset, width);
    

    return (clusterDims_);
   
  }  
  
  /***************************************************************************
  ** 
  ** Get the number of inbound links
  */

  public int getInboundLinkCount(Genome genome) {
    int retval = 0;
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (linkIsInbound(link)) {  
        retval++;
      }
    }
    return (retval);
  }
   
  /***************************************************************************
  ** 
  ** For a given source, get ordering of links (may be more than one).  If supplied
  ** source is null, we get the link order for all sources.
  */

  public List<String> getLinkOrder(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, PadCalculatorToo.PadResult> padChanges, String srcID) {

    List<String> retval = new ArrayList<String>();    
    List<String> linkOrder = getInboundLinkOrder(nps, padChanges);
    int numl = linkOrder.size();
    for (int i = 0; i < numl; i++) {
      String linkID = linkOrder.get(i);
      Linkage link = nps.getLinkage(linkID);
      String currSrcID = link.getSource();
      if ((srcID != null) && !srcID.equals(currSrcID)) {
        continue;
      }
      retval.add(linkID);
    }
    return (retval);
  }      

  /***************************************************************************
  ** 
  ** A function
  */

  public Set<String> getInputs() {
    return (inputs_);
  }   
  
  /***************************************************************************
  ** 
  ** A function
  */

  public boolean inputSetMatches(Set<String> inputs) {
    return (inputs_.equals(inputs));
  }   

  /***************************************************************************
  ** 
  ** A function
  */
  
  public boolean isATarget(String srcID) {
    return (inputs_.contains(srcID)); 
  }  
  
  /***************************************************************************
  ** 
  ** Answers if the given link is inbound to this cluster
  */

  public boolean linkIsInbound(Linkage link) {
    return (inboundLinks_.contains(link.getID()));
  }  
  
  /***************************************************************************
  ** 
  ** Answers if the given link is outbound from this cluster
  ** (Note: Feedbacks from fanout nodes back into fan-in or core
  ** are considered outbound.  We are not responsible for link routing.   
  */

  public boolean linkIsOutbound(Linkage link) {
    return (outboundLinks_.contains(link.getID()));
  }
  
  /***************************************************************************
  ** 
  ** Answers if the given link is internal to this cluster
  */

  public boolean linkIsInternal(Linkage link) {
    return (internalLinks_.contains(link.getID()));
  }  

  /***************************************************************************
  ** 
  ** Get the outbound source order
  */

  public List<String> getOutboundSourceOrder() {  
    if (srcOrder_.isEmpty()) {
      ArrayList<String> retval = new ArrayList<String>();
      retval.add(coreID_);
      return (retval);
    } else {
      ArrayList<String> retval = new ArrayList<String>();
      int numSo = srcOrder_.size();
      for (int i = 0; i < numSo; i++) {
        retval.add(srcOrder_.get(i).getSrc());
      }
      return (retval);
    }
  } 
  
  /***************************************************************************
  ** 
  ** Get all sources (not ordered).  Can be called before we have figured
  ** out the order:
  */

  public List<String> getAllSources() {  
    return (new ArrayList<String>(outputNodes_));
  }  
  
  /***************************************************************************
  ** 
  ** Get the outbound link order maximum
  */

  public int getOutboundLinkOrderMax() {
    if (srcOrder_.isEmpty()) {
      return ((hasDirect_) ? 1 : 0);      
    } else {
      return (srcOrder_.size());
    }
  }
    
  /***************************************************************************
  ** 
  ** Get the outbound link order
  */

  public int getOutboundLinkOrder(Linkage link) {

    if (srcOrder_.isEmpty()) {
      return (0);
    }

    String lsource = link.getSource();
    List<String> ordered = getOutboundSourceOrder();
    int numo = ordered.size();
    for (int i = 0; i < numo; i++) {
      String srcID = ordered.get(i);
      if (srcID.equals(lsource)) {
        return (i);
      }
    }
    System.err.println("no outbound order " + lsource + " " + ordered);
    throw new IllegalArgumentException();
  }  
 
  /***************************************************************************
  ** 
  ** Return the set of link IDs that have some relevance to this GaSC 
  */

  public Set<String> getRelatedLinks(SpecialtyLayoutEngine.NodePlaceSupport nps) {  
    //
    // Figure out the links we own.
    //

    HashSet<String> allMyNodes = new HashSet<String>();
    allMyNodes.addAll(fanInNodes_);
    allMyNodes.addAll(fanOutNodes_);
    allMyNodes.add(coreID_);
    
    
    HashSet<String> allMyLinks = new HashSet<String>();
    Iterator<String> amit = allMyNodes.iterator();
    while (amit.hasNext()) {
      String nodeID = amit.next();
      allMyLinks.addAll(nps.outboundLinkIDs(nodeID));
      allMyLinks.addAll(nps.inboundLinkIDs(nodeID));     
    }
    
    return (allMyLinks);
  }
   
  /***************************************************************************
  ** 
  ** Build up the cluster from group definitions: phase 1
  */

  public void prepFromGroupsPhaseOne(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, NodeGrouper.GroupElement> groups, boolean pureCoreNet) {

    if (!pureCoreNet) {
      HashSet<String> potentialFanIns = new HashSet<String>(); 
      HashSet<String> potentialFanOuts = new HashSet<String>();
      Iterator<String> gkit = groups.keySet().iterator();
      while (gkit.hasNext()) {
        String nodeID = gkit.next();
        if (nodeID.equals(coreID_)) {
          continue;
        } 
        NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
        if (!nodeInfo.group.equals(coreID_)) {
          continue;
        }
        
        //
        // At this point, we are dealing with input or output nodes.
        //
        
        switch (nodeInfo.side) {
          case INBOUND:
            potentialFanIns.add(nodeID);
            break;
          case OUTBOUND:
            potentialFanOuts.add(nodeID);
            break;
          default:
            throw new IllegalStateException();
        }
      }
  
      //
      // Node paths that are on the fan-out side, but which have no ultimate 
      // outbound target, and no core or fan-out inputs, belong on the fan in side.
      // Nodes that just target remaining fanouts belong on the fan-out side.
      //
      
      Set<String> fodes = fanOutDeadEnds(potentialFanOuts, groups);
      potentialFanIns.addAll(fodes);
      potentialFanOuts.removeAll(fodes); 
      
      Set<String> foet = fanOutExclusiveTargets(potentialFanIns, potentialFanOuts, groups);
      potentialFanIns.removeAll(foet);
      potentialFanOuts.addAll(foet);     
  
      Iterator<String> fiit = potentialFanIns.iterator();
      while (fiit.hasNext()) {
        String nodeID = fiit.next();
        NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
        // Used to be that the sources coming out of the node grouper were complete.  With
        // the arrival of GenomeSubsets, that set is ONLY for the guys in the subset.  We
        // need the global sources!
        addPenultimate(nodeInfo.nodeID, nps.getNodeSources(nodeInfo.nodeID)); // WRONG nodeInfo.sources);
      }
      
      Iterator<String> foit = potentialFanOuts.iterator();
      while (foit.hasNext()) {
        String nodeID = foit.next();
        fanOutNodes_.add(nodeID);
      }
    }   
    
    Set<String> allMyLinks = getRelatedLinks(nps); 

    Iterator<String> lit = allMyLinks.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = nps.getLinkage(linkID);
      String lsource = link.getSource();
      String ltarg = link.getTarget();
      if (makeLinkInbound(link)) {
        inboundLinks_.add(linkID);
        inputs_.add(lsource);
      } else if (makeLinkOutbound(link)) {
        outboundLinks_.add(linkID);
        if (lsource.equals(coreID_)) {
          hasDirect_ = true;
        }
        outputNodes_.add(lsource);
        if (isLargeScaleFeedback(link)) { 
          inboundLinks_.add(linkID);
          inputs_.add(lsource);
        }
      } else if (makeLinkInternal(link)) {
        internalLinks_.add(linkID);
      }      
      if (lsource.equals(coreID_) && ltarg.equals(coreID_)) {
        feedbacks_.add(linkID);
      } 
    }
    
    multiInCoreEstimates_ = new HashMap<String, Integer>();
    multiInputCorePadOrdering(nps, multiInCoreEstimates_);             
    return;
  }
  
  /***************************************************************************
  ** 
  ** Build up the cluster from remainders
  */

  public void prepFromRemaindersPhaseOne(GenomeSubset subset, String node) {

    //
    // Figure out the links we own:
    //
    
    Genome baseGenome = subset.getBaseGenome();
    
    Iterator<String> lit = subset.getLinkageSuperSetIterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = baseGenome.getLinkage(linkID);
      
      String lsource = link.getSource();
      String ltarg = link.getTarget();
      if (makeLinkInbound(link)) {
        inboundLinks_.add(linkID);
        inputs_.add(lsource);
      } else if (makeLinkOutbound(link)) {
        outboundLinks_.add(linkID);
        if (lsource.equals(coreID_)) {
          hasDirect_ = true;
        }
        outputNodes_.add(lsource);
        if (isLargeScaleFeedback(link)) { 
          inboundLinks_.add(linkID);
          inputs_.add(lsource);
        }
      } else if (makeLinkInternal(link)) {
        internalLinks_.add(linkID);
      }      
      if (lsource.equals(coreID_) && ltarg.equals(coreID_)) {
        feedbacks_.add(linkID);
      } 
    }
    
    multiInCoreEstimates_ = new HashMap<String, Integer>();
    multiInputCorePadOrdering(new SpecialtyLayoutEngine.NodePlaceSupport(baseGenome), multiInCoreEstimates_);    
   
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get the DDO (may be null)
  */

  public DropDirectionOracle getDDOracle() {
    return (ddo_);
  }

  /***************************************************************************
  ** 
  ** Build up the cluster: second pass
  ** REMEMBER!  Links are laid out (parametrically) HERE long before they are 
  ** "routed" i.e. actually assembled with specific geometry 
  */

  public void prepPhaseTwo(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, GeneAndSatelliteCluster.DropDirectionOracle ddo) {
    
    fanOutTrackedGrid_ = new TrackedGrid(gridFanOuts(nps), nps, irx, coreID_, textToo_, true);    
    fanInTrackedGrid_ = new TrackedGrid(gridFanIns(nps), nps, irx, coreID_, textToo_);
    ddo_ = ddo;
       
    // FIX ME: Ditch this:
    normalizeSources();
       
    fanInType_ = classifyFanIn(nps);    
    fanInPadPlans_ = new HashMap<String, PadCalculatorToo.PadResult>();
    //
    // These are pure internal fan only pad changes:
    //
    
    changeInternalFanPads(fanInTrackedGrid_, fanInNodes_, nps, fanInPadPlans_);
    if (!fanOutNodes_.isEmpty()) {
      fanOutPadPlans_ = new HashMap<String, PadCalculatorToo.PadResult>();
      changeInternalFanPads(fanOutTrackedGrid_, fanOutNodes_, nps, fanOutPadPlans_);    
    }    
    
    //
    // These classified links have >>>NOT<<< been ordered using pad changes.  That step
    // MUST be done later, and in fact it is...
    
    //
    // One expects that a layout of a region with all the same elements as the Full Genome model
    // will match the stacked layout of the full model.  But that depends completely on how we
    // break cycles, and thus the order if the linkages.  So order them!  Since in the GenomeInstance
    // the linkIDs have ":#" suffixes, this should match for a single-region only layout.  Beyond
    // that, we will have to get smarter.
    //
    GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();

    TreeMap<String, List<GridLinkRouter.ClassifiedLink>> internalsWithFanInSrcs = new TreeMap<String, List<GridLinkRouter.ClassifiedLink>>(cc);
    TreeMap<String, List<GridLinkRouter.ClassifiedLink>> internalsWithFanOutSrcs = new TreeMap<String, List<GridLinkRouter.ClassifiedLink>>(cc);    
    classifyInternalLinks(nps, internalsWithFanInSrcs, internalsWithFanOutSrcs);
      
    TreeMap<String, List<GridLinkRouter.ClassifiedLink>> inboundsWithFanInTargs = new TreeMap<String, List<GridLinkRouter.ClassifiedLink>>(cc);
    TreeMap<String, List<GridLinkRouter.ClassifiedLink>> inboundsWithFanOutTargs = new TreeMap<String, List<GridLinkRouter.ClassifiedLink>>(cc);    
    classifyInboundLinks(nps, inboundsWithFanInTargs, inboundsWithFanOutTargs);
    
    numFOT_ = inboundsWithFanOutTargs.size();
      
    HashMap<String, GridRouterPointSource> fanInPointSources = new HashMap<String, GridRouterPointSource>();
    fanInTraceOrder_ = new ArrayList<GridLinkRouter.RCRowCompare>();

    if (fanInType_ == COMPLEX_FAN_IN_) { 
     
      
      fanInGlr_ = new GridLinkRouter(appState_, fanInTrackedGrid_, this);
      // Note this is where fan-in to core links get handled:
      fanInLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
      fanInGlr_.layoutInternalLinks(internalsWithFanInSrcs, nps, fanInPadPlans_, fanInPointSources, fanInTraceOrder_, null, 0, fanInLinkPlans_);
      // Don't have a source order at this point, but we must get pad plans set up:
      //
      // WATCH OUT!  Since core is part of complex fan-in, you would expect that inbound core
      // pad changes get planned here.  WRONG!  Does not actually happen until e.g. the 
      // SuperSourceCluster calls calcPadChanges just before it calls inboundLinkRouting!
      //       
      changeInboundPadsForFan(fanInNodes_, fanInTrackedGrid_, nps, fanInPadPlans_, null, ddo != null);
 
      inboundLinkPlans_ = fanInGlr_.layoutInboundLinks(inboundsWithFanInTargs, nps, fanInPadPlans_, fanInPointSources, null, null, null, null, true, new ArrayList<String>());
      Collections.sort(fanInTraceOrder_);
    }

    coreJumpers_ = new HashMap<String, List<GridLinkRouter.ClassifiedLink>>();
    coreJumpersAlsoToCore_ = new HashSet<String>();
    srcOrder_ = new ArrayList<GridLinkRouter.RCRowCompare>();
    ArrayList<String> jumperLinks = new ArrayList<String>();
    
    //
    // Start Handling fan out links 
    //
    
    if (!fanOutNodes_.isEmpty()) {    
      //
      // Core jumping links need to go inbound to the fanout:
      //
      Iterator<String> ifiskit = internalsWithFanInSrcs.keySet().iterator();
      while (ifiskit.hasNext()) {
        String srcID = ifiskit.next();
        List<GridLinkRouter.ClassifiedLink> perSrc = internalsWithFanInSrcs.get(srcID);
        int numPS = perSrc.size();
        for (int i = 0; i < numPS; i++) {
          GridLinkRouter.ClassifiedLink cLink = perSrc.get(i);
          if (cLink.type == GridLinkRouter.CORE_JUMPING) {
            List<GridLinkRouter.ClassifiedLink> coreJumpPerSrc = coreJumpers_.get(srcID);
            if (coreJumpPerSrc == null) {
              coreJumpPerSrc = new ArrayList<GridLinkRouter.ClassifiedLink>();
              coreJumpers_.put(srcID, coreJumpPerSrc);
            }
            coreJumpPerSrc.add(cLink.clone());
            jumperLinks.add(cLink.linkID);
          }
          if (cLink.type == GridLinkRouter.FAN_IN_TO_CORE) {
            coreJumpersAlsoToCore_.add(srcID);
          }
        }
      }
      Set<String> cjSet = coreJumpers_.keySet();
      coreJumpersAlsoToCore_.retainAll(cjSet);
      inboundsWithFanOutTargs.putAll(coreJumpers_);
      List<String> bottomJumpers = (fanInType_ == COMPLEX_FAN_IN_) ? fanInGlr_.getBottomReservations() : new ArrayList<String>();
      int numCJ = cjSet.size() - bottomJumpers.size();
    
      HashMap<String, GridRouterPointSource> pointSources = new HashMap<String, GridRouterPointSource>();
      fanOutGlr_ = new GridLinkRouter(appState_, fanOutTrackedGrid_, this);
      fanOutLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
      coreLinkIsFanned_ = fanOutGlr_.layoutInternalLinks(internalsWithFanOutSrcs, nps, fanOutPadPlans_, pointSources, 
                                                         new ArrayList<GridLinkRouter.RCRowCompare>(), coreID_, numCJ, 
                                                         fanOutLinkPlans_);
      changeInboundPadsForFan(fanOutNodes_, fanOutTrackedGrid_, nps, fanOutPadPlans_, null, true);
      changePadsForFanForJumperLinks(jumperLinks, fanOutNodes_, fanOutTrackedGrid_, nps, fanOutPadPlans_, null, true);
      // This is where core jumpers get laid out as well:
      // Note that when this is done, the plans are maps to LinkRouter.SpecialtyInputs!
      // The originating GASC has plans for the outbounds, the inbounds have info on the
      // inbounds.
          
      fanOutInboundLinkPlans_ = fanOutGlr_.layoutInboundLinks(inboundsWithFanOutTargs, nps, fanOutPadPlans_, pointSources, 
                                                              fanInGlr_, fanInPointSources, cjSet, coreJumpersAlsoToCore_, false, bottomJumpers);  // ddo not needed
      outboundLinkPlans_ = fanOutGlr_.layoutOutboundLinks(outboundLinks_, coreID_, nps, fanOutPadPlans_, pointSources, srcOrder_);
      feedbackLinkPlans_ = fanOutGlr_.feedbackRoutingForFanout(feedbacks_, nps, irx, fanOutPadPlans_, pointSources, coreID_);
      Collections.sort(srcOrder_);
    } else {
      outboundLinkPlans_ = new HashMap<String, SpecialtyLayoutLinkData>();
    }   
    return;
  }  

  /***************************************************************************
  ** 
  ** Locate the cluster
  */
  
  public void locateAsSource(Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
   
    locateAsTarget(basePos, nps, irx, null);      
    Point2D upperLeft = getClusterDims(nps, irx).getFanOutCorner(nps, true);
    if (upperLeft != null) {
      placeFan(fanOutTrackedGrid_, upperLeft, nps);
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Get the link tree root
  */
  
  public Point2D linkTreeRoot(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {

    int launch = 0;
    Iterator<String> olit = outboundLinks_.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      Linkage link = nps.getLinkage(linkID);
      if (link.getSource().equals(coreID_)) {
        launch = getCurrentLaunchPad(link, nps.getPadChanges());
        break;
      }
    }

    Vector2D offset = launchPadToOffset(launch, nps, irx, coreID_);
    Point2D basePos = nps.getPosition(coreID_);
    Point2D launchPt = offset.add(basePos);
    UiUtil.forceToGrid(launchPt, UiUtil.GRID_SIZE);
    Vector2D rootOff = new Vector2D(LINK_ROOT_X_OFFSET_, 0.0);
    return (rootOff.add(launchPt));  
  }
  
  
  /***************************************************************************
  ** 
  ** Handle outbound link routing.  
  */
  
  public void routeOutboundLinks(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                 DataAccessContext rcx,
                                 Map<String, Double> internalTraces, Map<String, Double> outboundTraces, 
                                 Map<String, SuperSrcRouterPointSource> traceDefs,
                                 boolean baseAtTop, Map<String, Integer> srcToTrack, 
                                 Map<Integer, Double> trackToY, Map<String, Double> srcToFeedbackY, 
                                 boolean forStackedClusters) {
    
    Iterator<String> asit = getAllSourceIterator();
    while (asit.hasNext()) {
      String srcID = asit.next();
      if (traceDefs.get(srcID) != null) {
        continue;
      }
      SuperSrcRouterPointSource corners = new SuperSrcRouterPointSource(srcID, baseAtTop, forStackedClusters);
      traceDefs.put(srcID, corners);
      Double traceX = internalTraces.get(srcID);
      Double track = outboundTraces.get(srcID);
      SpecialtyLayoutLinkData sin = outboundLinkPlans_.get(srcID);
      corners.init(this, nps, rcx, traceX, track, srcToTrack, trackToY, srcToFeedbackY, sin, fanOutTrackedGrid_);
    }
    
    return;
  }
         
  /***************************************************************************
  ** 
  ** Handle internal link routing.  Returns empty map if there are no internal links
  ** to place.
  */
  
  public Map<String, SpecialtyLayoutLinkData> routeInternalLinks(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    //
    // First off, let's find out what links need to be laid out for
    // each source
    // 
    
    Map<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    if (fanInType_ == COMPLEX_FAN_IN_) {
      Point2D upperLeftFanInPt = getClusterDims(nps, irx).getFanInCorner(nps);
      retval = fanInGlr_.convertInternalLinks(fanInLinkPlans_, upperLeftFanInPt, nps, irx, coreID_);
    }
    
    if (fanOutLinkPlans_ != null) {
      //
      // Issue #254: getFanOutCorner() says we want a false for the last argument for doing final link conversion:
      // REPLACE Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, true);
      //
      Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, false);   
      Map<String, SpecialtyLayoutLinkData> fanOutConv = fanOutGlr_.convertInternalLinks(fanOutLinkPlans_, upperLeftFanOutPt, nps, irx, coreID_);
      retval.putAll(fanOutConv);      
      Map<String, SpecialtyLayoutLinkData> feedbackConv = fanOutGlr_.convertInternalLinks(feedbackLinkPlans_, upperLeftFanOutPt, nps, irx, coreID_);
      SpecialtyLayoutLinkData.mergeMaps(retval, feedbackConv);
      Map<String, SpecialtyLayoutLinkData> jumpConv = fanOutConvertCoreJumpers(nps, irx);
      SpecialtyLayoutLinkData.mergeMaps(retval, jumpConv);
    }
    
    if (fanOutNodes_.isEmpty()) {
      SpecialtyLayoutLinkData sin = retval.get(coreID_);
      if (sin == null) {
        sin = new SpecialtyLayoutLinkData(coreID_);       
        retval.put(coreID_, sin);
      }
      simpleFeedbackRouting(sin, nps, irx);
    }
 
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Converts those elements of a departure path that are parameterized
  */
  
  public List<SpecialtyLayoutLinkData.TrackPos> convertDeparturePath(List<SpecialtyLayoutLinkData.TrackPos> pointList, 
                                                                     SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                                     DataAccessContext irx) {
    //
    // Issue #254: getFanOutCorner() says we want a false for the last argument for doing final link conversion:
    // REPLACE Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, true);
    //
    Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, false);
    return (fanOutGlr_.convertPositionListToPoints(pointList, upperLeftFanOutPt, nps, irx, coreID_));
  }
  
  /***************************************************************************
  ** 
  ** Converts a parmaeterized point
  */
  
  public Point2D convertDeparturePoint(TrackedGrid.TrackPosRC tprc, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                       DataAccessContext irx) {
    //
    // Issue #254: getFanOutCorner() says we want a false for the last argument for doing final link conversion:
    // REPLACE Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, true);
    //  
    Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, false);
    return (fanOutGlr_.convertPositionToPoint(tprc, upperLeftFanOutPt, nps, irx, coreID_));
  }  

  /***************************************************************************
  **
  ** Get terminal targets. 
  */
  
  public static List<GeneAndSatelliteCluster> findTerminalTargets(BTState appState, GenomeSubset subset, boolean omitNonGenesWithInputs, 
                                                                  double traceOffset, boolean textToo) {
    ArrayList<GeneAndSatelliteCluster> retval = new ArrayList<GeneAndSatelliteCluster>();
    //Iterator nit = subset.getNodeSuperSetIterator();
    Iterator<String> nit = subset.getNodeIterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      int tc = subset.getTargetCount(nodeID);
      if (tc == 0) {
        int nodeType = subset.getBaseGenome().getNode(nodeID).getNodeType();
        if ((nodeType != Node.GENE) && omitNonGenesWithInputs) {
          if (!subset.getNodeSources(nodeID).isEmpty()) {
            continue;
          }
        }
        retval.add(new GeneAndSatelliteCluster(appState, nodeID, false, traceOffset, true, textToo));
      }
    }

    return (retval);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Fill out target clusters
  */
  
  public static void fillTargetClusters(List<GeneAndSatelliteCluster> clusters, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                        DataAccessContext irx) {
    
    //
    // For each terminal target, find inputs that only hit
    // the target, and have inputs themselves.  Only relevant
    // for bubbles.  At the same time, figure out the
    // sources
    //
       
    int num = clusters.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = clusters.get(i);
      clust.fillTargetClusterGreedy(nps, irx);      
    }
    
    return;
  }  
  
 /***************************************************************************
  **
  ** Fill out target clusters
  */
  
  public static List<GeneAndSatelliteCluster> fillTargetClustersByGroups(BTState appState, List<String> clusters, 
                                                                         Map<String, NodeGrouper.GroupElement> groups, 
                                                                         SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                                         DataAccessContext irx, 
                                                                         double traceOffset, 
                                                                         boolean doPhaseTwo, boolean textToo, boolean isStacked) {
      
    //
    // For each terminal target, find inputs that only hit
    // the target, and have inputs themselves.  At the 
    // same time, figure out the sources
    //
    
    ArrayList<GeneAndSatelliteCluster> retval = new ArrayList<GeneAndSatelliteCluster>();
    int num = clusters.size();
    for (int i = 0; i < num; i++) {
      GeneAndSatelliteCluster clust = new GeneAndSatelliteCluster(appState, clusters.get(i), isStacked, traceOffset, true, textToo);
      retval.add(clust);     
      clust.prepFromGroupsPhaseOne(nps, groups, nps.pureCoreNetwork);
    }
    // In stacked cases, phase two happens later (targets are treated the same as sources,
    // and everybody gets done at that later time:
    if (doPhaseTwo) {
      for (int i = 0; i < num; i++) {
        GeneAndSatelliteCluster clust = retval.get(i);
        clust.prepPhaseTwo(nps, irx, null);
      }
    }
    return (retval);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static class LinkPlacementState {
    public boolean isRoot;
    public Point2D runPt;
    public Point2D lastAttach;
    public Point2D lastRun;
    public boolean isRunStart;  
  }  
  
  /***************************************************************************
  **
  ** For holding outbound traces
  */  
  
  public static class InboundCorners {
    
    public String linkID;
    public boolean needDrop;
    public double dropY;
    public SpecialtyLayoutLinkData.TrackPos prevPoint;
    public List<SpecialtyLayoutLinkData.TrackPos> finalPath;

    public InboundCorners(String linkID) {
      this.linkID = linkID;
      this.finalPath = new ArrayList<SpecialtyLayoutLinkData.TrackPos>();
    }    
  }
  
 
  public static class TagAnnotatedList<T> extends ArrayList<T> {
    
    public String tag;
    private static final long serialVersionUID = 1L;
    
    TagAnnotatedList(String tag) {
      super();
      this.tag = tag;
    }   
  }
 
  /***************************************************************************
  ** 
  ** For a given source ID, is a link coming at us from the top or bottom?  If
  ** we are in the same ssc, forward links always come from the top, while feedback
  ** loops depend on whether we are top or bottom
  ** around to above the target).  If in different ssc, regular links depend on
  ** if we are above or below the baseline, feedback links depend on whether
  ** the source is above or below the baseline.
  */
  
  public static class DropDirectionOracle {  

    private Map<String, Boolean> isOnTop_;
    private Map<String, Integer> srcToCol_;
    private boolean baseAtTop_; 
    private Set<String> allMySources_;
    private int myColumn_;
    private List<GeneAndSatelliteCluster> stackOrder_;
 
    public DropDirectionOracle(Map<String, Boolean> isOnTop, Map<String, Integer> srcToCol, boolean baseAtTop, 
                               List<GeneAndSatelliteCluster> stackOrder, Set<String> allMySources, int myColumn) {
      isOnTop_ = isOnTop;
      srcToCol_ = srcToCol;
      baseAtTop_ = baseAtTop;
      allMySources_ = allMySources;
      myColumn_ = myColumn;
      stackOrder_ = new ArrayList<GeneAndSatelliteCluster>(stackOrder);
    }        
     
    public boolean isBaseAtTop() {
      return (baseAtTop_);
    }
      
    public boolean comingFromTop(String srcID, GeneAndSatelliteCluster targClust) {
      if (allMySources_.contains(srcID)) {
        boolean seenTarg = false;
        int numOrd = stackOrder_.size();
        for (int i = 0; i < numOrd; i++) {
          GeneAndSatelliteCluster sc = stackOrder_.get(i); 
          Iterator<String> sit = sc.getAllSourceIterator();
          while (sit.hasNext()) {
            String srcIDCheck = sit.next();
            if (srcIDCheck.equals(srcID)) {
              return ((seenTarg) ? true : !baseAtTop_);
            }
          }
          seenTarg = ((targClust != null) && (sc == targClust));
        }        
        throw new IllegalArgumentException();
      } 

      //
      // Kinda a messy hack.  When we are operating on stacked clusters, the sources out
      // of our row are not in this mapping, and return null.  In those cases, the source
      // is always coming from the top, and we do not need to see if we are a feedback:
      //
      Integer srcCol = srcToCol_.get(srcID);
      if (srcCol == null) {
        return (true);
      }
      
      boolean isFeedback = (srcCol.intValue() > myColumn_); 
      if (!isFeedback) {
        return (baseAtTop_);
      }

      boolean srcOnTop = isOnTop_.get(srcID).booleanValue();
      return (srcOnTop);      
    }      
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Do final inbound link routing on simple fan-in (also no fan-in, correct??)
  */

  private void simpleFanInFinalLinkRoutingLinkCreation(SpecialtyLayoutLinkData sin, 
                                                       MetaClusterPointSource lps,
                                                       SortedMap<Double, List<String>> sortByCoord, Map<String, List<Point2D>> holdDrops, 
                                                       String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                                       DataAccessContext rcx, boolean noFan) {

    boolean entryFromTop = (ddo_ == null) ? true : ddo_.comingFromTop(srcID, this);
    
    //
    // If deferred, we add points to InboundCorners for later attachment:
    // Get the perClust list build up and ready to go:
    //
    
    boolean deferred = lps.isForDeferredOnly(); 
    TagAnnotatedList<InboundCorners> perClust = (deferred) ? lps.initInboundsPerSrcForDeferred(coreID_, srcID) : null;    
 
    boolean isFirst = true;
    SpecialtyLayoutLinkData.TrackPos lastForSrc = null;
    Iterator<Double> sbxit = sortByCoord.keySet().iterator();
    while (sbxit.hasNext()) {
      Double cVal = sbxit.next();
      List<String> linksPerC = sortByCoord.get(cVal);
      int numLinks = linksPerC.size();      
      for (int i = 0; i < numLinks; i++) {
        String linkID = linksPerC.get(i);
        if (deferred) {
          InboundCorners ic = new InboundCorners(linkID);
          // link order here matters!
          perClust.add(ic);
          boolean isToPen = false;
          if (!noFan) {
            String targNode = nps.getLinkage(linkID).getTarget();
            isToPen = penultimate_.keySet().contains(targNode);
          }
          List<Point2D> dropPts = holdDrops.get(linkID);
          List<SpecialtyLayoutLinkData.TrackPos> convertedDrops = SpecialtyLayoutLinkData.TrackPos.convertPointList(dropPts);
          SpecialtyLayoutLinkData.TrackPos finalForClust = convertedDrops.get(0);
          if (isFirst || isToPen) {
            ic.needDrop = true;
            ic.dropY = finalForClust.getPoint().getY();
          } else {
            ic.needDrop = false;
            ic.prevPoint = lastForSrc;
          }
          ic.finalPath.addAll(convertedDrops);
          int whichIndex = (isToPen) ? 0 : convertedDrops.size() - 1;
          // Fix added to address Issue #234:
          if (!penultimate_.isEmpty() && !isToPen) {
            whichIndex--;
          }
          lastForSrc = convertedDrops.get(whichIndex);
          lastForSrc = lastForSrc.clone();
        } else {
          //
          // Note how the non-deferred case calls buildPrePath to get the ball rolling on creating
          // any path needed prior to gluing our stuff in.  Not needed when we defer (as above)!
          //
          if (!sin.haveLink(linkID)) {
            sin.startNewLink(linkID);
          }
          List<Point2D> dropPts = holdDrops.get(linkID);
          List<SpecialtyLayoutLinkData.TrackPos> convertedDrops = SpecialtyLayoutLinkData.TrackPos.convertPointList(dropPts);
          SpecialtyLayoutLinkData.TrackPos finalForClust = convertedDrops.get(0);
          Point2D finalForClustPt = finalForClust.getPoint();
          boolean extendingVertically = !noFan;
          lps.buildPrePath(sin, linkID, finalForClustPt, isFirst, nps, rcx, extendingVertically, isFirst && entryFromTop);
          sin.addPositionListToLink(linkID, convertedDrops);     
        }
        isFirst = false;
      }
    }
    return;
  }    

  /***************************************************************************
  ** 
  ** Classify the inbound links (fan out, fan in)
  */
  
  private void classifyInboundLinks(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                    Map<String, List<GridLinkRouter.ClassifiedLink>> fanInTargs, 
                                    Map<String, List<GridLinkRouter.ClassifiedLink>> fanOutTargs) {
    
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      int trgMember = nodeFanMembership(trg);
      int linkType = getInboundLinkType(trgMember);
      Map<String, List<GridLinkRouter.ClassifiedLink>> workingMap;
      switch (trgMember) {
        case IS_FAN_OUT_: 
          workingMap = fanOutTargs;
          break;
        case IS_FAN_IN_:
        case IS_CORE_:
          workingMap = fanInTargs;
          break;
        default:
          throw new IllegalStateException();
      }
      List<GridLinkRouter.ClassifiedLink> perSrc = workingMap.get(src);
      if (perSrc == null) {
        perSrc = new ArrayList<GridLinkRouter.ClassifiedLink>();
        workingMap.put(src, perSrc);
      }      
      GridLinkRouter.ClassifiedLink cLink = new GridLinkRouter.ClassifiedLink(linkID, linkType);
      perSrc.add(cLink);
    }
    return;
  }   

  /***************************************************************************
  ** 
  ** Classify internal links
  */
  
  private void classifyInternalLinks(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                     Map<String, List<GridLinkRouter.ClassifiedLink>> fanInSrcs, 
                                     Map<String, List<GridLinkRouter.ClassifiedLink>> fanOutSrcs) {

    //
    // First off, let's find out what links need to be laid out for
    // each source
    //    
    
    Iterator<String> ilit = internalLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);      
      String src = link.getSource();
      String trg = link.getTarget();
      int srcMember = nodeFanMembership(src);
      int trgMember = nodeFanMembership(trg);
      int linkType = getInternalLinkType(srcMember, trgMember);
      Map<String, List<GridLinkRouter.ClassifiedLink>> workingMap;
      switch (srcMember) {
        case IS_CORE_:
        case IS_FAN_OUT_: 
          workingMap = fanOutSrcs;
          break;
        case IS_FAN_IN_:   
          workingMap = fanInSrcs;
          break;
        default:
          throw new IllegalStateException();
      }
      List<GridLinkRouter.ClassifiedLink> perSrc = workingMap.get(src);
      if (perSrc == null) {
        perSrc = new ArrayList<GridLinkRouter.ClassifiedLink>();
        workingMap.put(src, perSrc);
      }
      GridLinkRouter.ClassifiedLink cLink = new GridLinkRouter.ClassifiedLink(linkID, linkType);
      perSrc.add(cLink);
    }   
    return;
  }  
  
  /***************************************************************************
  ** 
  ** A function
  */

  private void addSources(Set<String> srcs) {
    inputs_.addAll(srcs);
    return;
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void addPenultimate(String penID, Set<String> sources) {
    if (sources == null) {
      sources = new HashSet<String>();
    }
    penultimate_.put(penID, sources);  // Note this includes inputs from other pens now...
    inputs_.addAll(sources);
    fanInNodes_.add(penID);
    return;
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void normalizeSources() {
    Set<String> kset = penultimate_.keySet();
    inputs_.removeAll(kset);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** A function
  */
  
  private List<Point2D> directLinkDropPoints(String linkID, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                             DataAccessContext irx, MetaClusterPointSource lps) {

    //
    // If we have no fan-in we drop down directly.
    // Else we find out the limits set by the lowest pen point.
    //

    ArrayList<Point2D> retval = new ArrayList<Point2D>();      
    Point2D targLoc = nps.getPosition(coreID_);
    int landing = getCurrentLandingPad(nps.getLinkage(linkID), nps.getPadChanges(), false);
    Vector2D padOffset = landingPadToOffset(landing, nps, irx, coreID_);
    
    //
    // In stacked cases, with multiple links from same source, we would like
    // to have a nice short height for the inter-link tie.  The following code
    // could do that, and we are generally safe due to the lack of inbound
    // links from a fan-in. BUT it would fail for cases where genes have defined
    // regions, which prevents pad changes.  There would be no guarantee that
    // the ties would not overlap if we set them to a single value!  The crappier
    // appearance is safer!
    //Point2D launch = getClusterDims(genome, lo, frc).getCoreLaunch(placement);
    //double multiPadY = launch.getY() - STACK_Y_DELTA_ + UiUtil.GRID_SIZE;
    //double traceY = (isStacked_) ? multiPadY : lps.getTargetTraceY();
   
    double traceY = lps.getTargetTraceY(); 
    if (fanInType_ != SIMPLE_FAN_IN_) {
      retval.add(new Point2D.Double(targLoc.getX() + padOffset.getX(), traceY));        
      return (retval);
    }
    
    //
    // Gotta do this since landing pads are no longer in simple linear order for non-genes:
    //
      
    Vector2D leftmostPenOffset = landingPadToOffset(leftmostPen(nps), nps, irx, coreID_);
    double whichOffset = leftmostPenOffset.getX() - padOffset.getX();
    int whichDirect = (int)(whichOffset / UiUtil.GRID_SIZE);
    //int whichDirect = leftmostPen(genome) - landing;
    String pen0 = penOrder_.get(new Integer(0));
    Point2D penLoc = nps.getPosition(pen0); 
    
    if (lps.busFeedIsHorizontal()) {  // classic target-only case
      int dropTrack = orderToSideDropTrack(nps, 0);
      // FIX ME:
      // This really does not route well into non-gene cores:
      if (nps.getNode(coreID_).getNodeType() == Node.GENE) {
        dropTrack = dropTrack - whichDirect;
      } else {
        dropTrack = 0;
      }
      Vector2D dropOffset = landingPadToOffset(dropTrack, nps, irx, coreID_);
      retval.add(new Point2D.Double(targLoc.getX() + dropOffset.getX(), traceY));
      retval.add(new Point2D.Double(targLoc.getX() + dropOffset.getX(), 
                                    penLoc.getY() + (whichDirect * UiUtil.GRID_SIZE)));      
    } else {
      double horizontalX = lps.getVerticalDropX(nps, irx);
      retval.add(new Point2D.Double(horizontalX,
                                    penLoc.getY() + (whichDirect * UiUtil.GRID_SIZE)));
    }
    retval.add(new Point2D.Double(targLoc.getX() + padOffset.getX(), 
                                  penLoc.getY() + (whichDirect * UiUtil.GRID_SIZE)));      
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private List<Point2D> doLinkToDropPoints(String linkID, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                           DataAccessContext irx, MetaClusterPointSource lps) {

    Linkage link = nps.getLinkage(linkID);    
    int landing = getCurrentLandingPad(link, nps.getPadChanges(), false);    
    String ltrg = link.getTarget();
        
    Point2D penLoc = nps.getPosition(ltrg);
    
    if (fanInType_ != COMPLEX_FAN_IN_) {
      Point2D targLoc = nps.getPosition(coreID_);
      int order = penToOrder(ltrg);
      if (landing == 0) {
        return (linkToTopDropPoints(order, lps, nps, irx, penLoc, targLoc));
      } else {
        return (linkToSideDropPoints(order, lps, nps, irx, penLoc, targLoc));
      }
    } else {
      Grid.RowAndColumn rac = fanInTrackedGrid_.findPositionRandC(ltrg);
      return (linkToComplexFanInDropPoints(rac.row, lps, penLoc)); 
    }
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private Set<String> pensAsTargets(String srcID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> kit = penultimate_.keySet().iterator();
    while (kit.hasNext()) {
      String pen = kit.next();
      Set<String> pSrc = penultimate_.get(pen);
      if (pSrc.contains(srcID)) {
        retval.add(pen);
      }
    }
    return (retval); 
  } 
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void orderParentPens(String penID) {
    Set<String> pSrcs = penultimate_.get(penID);
    Set<String> penKeys = penultimate_.keySet();
    HashSet<String> penParents = new HashSet<String>(penKeys);
    penParents.retainAll(pSrcs);
    if (penOrder_.values().contains(penID)) {
      return;  // Already placed!
    }
    
    bumpPenOrder(penID);
    Iterator<String> ppit = penParents.iterator();
    while (ppit.hasNext()) {
      String penParent = ppit.next();
      orderParentPens(penParent);
    }
 
    return;
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private boolean linkToFanOut(String linkID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    Linkage link = nps.getLinkage(linkID);
    String ltrg = link.getTarget();
    return (fanOutNodes_.contains(ltrg));
  }  
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void orderPenultimate(String penID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    //
    // This ordering is set based on external inputs.  Any pens
    // with no external inputs will not get placed unless we catch
    // it here.
    //
    if (!toCoreFromPen_.containsKey(penID)) {
      Set<String> allLinks = nps.getLinksBetween(penID, coreID_);
      toCoreFromPen_.put(penID, allLinks);
    }
    if (penOrder_.values().contains(penID)) {
      return;
    }
    orderParentPens(penID);
    return;
  }
  
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void bumpPenOrder(String penID) {
    if (penOrder_.isEmpty()) {
      penOrder_.put(new Integer(0), penID);
    } else {
      Integer last = penOrder_.lastKey();
      penOrder_.put(new Integer(last.intValue() + 1), penID);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the pad number for the given simple fan-in node:
  */
  
  private int penIDToPadnumNonGene(SpecialtyLayoutEngine.NodePlaceSupport nps, String penID) {
    Node node = nps.getNode(coreID_); 
    int needed = nps.inboundLinkCount(coreID_);
    NodeProperties np = nps.getNodeProperties(penID);
    INodeRenderer rend = np.getRenderer();
    TreeSet<Integer> usedPads = new TreeSet<Integer>();
    int inbounds = inboundLinks_.size();
    for (int i = 0; i < inbounds; i++) {
      rend.getBestTopPad(node, usedPads, NodeProperties.HORIZONTAL_GROWTH, needed, true);       
    }
 
    ArrayList<Integer> revKeys = new ArrayList<Integer>(penOrder_.keySet());
    Collections.sort(revKeys, Collections.reverseOrder());
    Iterator<Integer> pit = revKeys.iterator();
    while (pit.hasNext()) {
      Integer order = pit.next();
      String currPenID = penOrder_.get(order);
      Set<String> allLinks = toCoreFromPen_.get(currPenID);
      int numLinks = allLinks.size();
      int lastTop = 0;
      for (int i = 0; i < numLinks; i++) {
        lastTop = rend.getBestTopPad(node, usedPads, NodeProperties.HORIZONTAL_GROWTH, needed, true);       
      }      
      if (currPenID.equals(penID)) {
        return (lastTop);
      }
    }
    throw new IllegalArgumentException();
  }    
  
 
  /***************************************************************************
  ** 
  ** Get the pad number for the given simple fan-in node:
  */
  
  private int penIDToPadnum(SpecialtyLayoutEngine.NodePlaceSupport nps, String penID) {
    Node node = nps.getNode(coreID_);    
    if (node.getNodeType() != Node.GENE) {
      return (penIDToPadnumNonGene(nps, penID));
    }
    int currPad = DBGene.DEFAULT_PAD_COUNT - 2 - feedbacks_.size();
    Iterator<Integer> pit = penOrder_.keySet().iterator();
    while (pit.hasNext()) {
      Integer order = pit.next();
      String currPenID = penOrder_.get(order);
      Set<String> allLinks = toCoreFromPen_.get(currPenID);
      int numLinks = allLinks.size();
      if (currPenID.equals(penID)) {
        return (currPad);
      }
      currPad -= (numLinks < BUBBLE_PAD_INCR_) ? BUBBLE_PAD_INCR_ : numLinks;
    }
    throw new IllegalArgumentException();
  }  
  
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private int penToOrder(String penID) {
    Iterator<Integer> pit = penOrder_.keySet().iterator();
    while (pit.hasNext()) {
      Integer order = pit.next();
      String chkPenID = penOrder_.get(order);
      if (chkPenID.equals(penID)) {
        return (order.intValue());
      }
    }
    throw new IllegalStateException();
  }  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private int leftmostPen(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    Integer lastKey = penOrder_.lastKey();
    String lastPenID = penOrder_.get(lastKey);
    return (penIDToPadnum(nps, lastPenID));
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private int orderToTopDropTrack(SpecialtyLayoutEngine.NodePlaceSupport nps, int order) {
    int leftmost = leftmostPen(nps);
    int maxPen = penOrder_.size() - 1;
    if (order == maxPen) {
      return (leftmost);
    }
    return ((leftmost + PEN_DROP_OFFSET_ + 1) - (2 * (maxPen - order)));
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private int orderToSideDropTrack(SpecialtyLayoutEngine.NodePlaceSupport nps, int order) {
    int leftmost = leftmostPen(nps);
    int maxPen = penOrder_.size() - 1;
    return ((leftmost + PEN_DROP_OFFSET_) - (2 * (maxPen - order)));
  }  

  /***************************************************************************
  ** 
  ** A function
  */
  
  private List<Point2D> linkToTopDropPoints(int order, MetaClusterPointSource lps, SpecialtyLayoutEngine.NodePlaceSupport nps,
                                            DataAccessContext irx, Point2D penLoc, Point2D targLoc) {
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    //
    // The first case handles pure target routing (drops from an overhead horizontal bus) or
    // source routing (drops from a vertical bus on the left):
    //
    double nextToFinalX;
    if (lps.busFeedIsHorizontal()) {  // classic target-only case
      int dropTrack = orderToTopDropTrack(nps, order);
      Vector2D offset = landingPadToOffset(dropTrack, nps, irx, coreID_);
      retval.add(new Point2D.Double(targLoc.getX() + offset.getX(), lps.getTargetTraceY()));
      if (dropTrack == leftmostPen(nps)) {
        return (retval);
      }
      nextToFinalX = targLoc.getX() + offset.getX();
    } else {
      nextToFinalX = lps.getVerticalDropX(nps, irx);
    }
    retval.add(new Point2D.Double(nextToFinalX,
                                  penLoc.getY() + (PEN_TOP_DROP_Y_OFFSET_ * UiUtil.GRID_SIZE)));    
    retval.add(new Point2D.Double(penLoc.getX(), 
                                  penLoc.getY() + (PEN_TOP_DROP_Y_OFFSET_ * UiUtil.GRID_SIZE)));

    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private List<Point2D> linkToSideDropPoints(int order, MetaClusterPointSource lps, 
                                             SpecialtyLayoutEngine.NodePlaceSupport nps,
                                             DataAccessContext irx, Point2D penLoc, Point2D targLoc) {
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    //
    // The first case handles pure target routing (drops from an overhead horizontal bus) or
    // source routing (drops from a vertical bus on the left):
    //
    double finalX;
    
    if (lps.busFeedIsHorizontal()) {  // classic target-only case
      int dropTrack = orderToSideDropTrack(nps, order);
      Vector2D offset = landingPadToOffset(dropTrack, nps, irx, coreID_);
      retval.add(new Point2D.Double(targLoc.getX() + offset.getX(), lps.getTargetTraceY()));
      finalX = targLoc.getX() + offset.getX();
    } else {
      finalX = lps.getVerticalDropX(nps, irx);
    }
    retval.add(new Point2D.Double(finalX, penLoc.getY()));
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private List<Point2D> linkToComplexFanInDropPoints(int order, MetaClusterPointSource lps, Point2D penLoc) {
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    double offset = order * UiUtil.GRID_SIZE;
    retval.add(new Point2D.Double(penLoc.getX() - offset, lps.getTargetTraceY()));
    retval.add(new Point2D.Double(penLoc.getX() - offset, penLoc.getY()));
    return (retval);
  }  
  

  /***************************************************************************
  ** 
  ** For a given source, return list of link IDs inbound to cluster.  Deprecated.
  */
  
  private List<String> linkIDsPerClusterOldStyle(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    if (!inputs_.contains(srcID)) {
      return (null);
    }
    ArrayList<String> retval = new ArrayList<String>();
    Set<String> penKeys = penultimate_.keySet();
    Iterator<Linkage> lit = nps.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (!link.getSource().equals(srcID)) {
        continue;
      }
      String targID = link.getTarget();
      if (coreID_.equals(targID) || (penKeys.contains(targID) && !penKeys.contains(srcID))) {
        retval.add(link.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** For a given source, return list of link IDs inbound to cluster.
  */
  
  public List<String> linkIDsPerCluster(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      if (!link.getSource().equals(srcID)) {
        continue;
      }
      retval.add(linkID);
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get the ordering of inbound links
  */

  private List<String> getInboundLinkOrder(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, PadCalculatorToo.PadResult> padChanges) {
    
    //
    // Previously, pad assignments were (possibly) shuffled around based on inbound source
    // order.  Now we ask to get the inbound source order to finally use.
    //
    
    //
    // Before fan-ins were added, we just found the lowest target pad for each inbound source,
    // and returned that.  Now with fan-ins, we put those inbound links below it.
    //
    
    //
    // Start with the target itself:
    //
       
    TreeMap<Integer, List<String>> linksForCore = new TreeMap<Integer, List<String>>(); 
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      if (!link.getTarget().equals(coreID_)) {
        continue;
      }
      int targPad = getCurrentLandingPad(link, padChanges, false);
      Integer targPadObj = new Integer(targPad);
      List<String> linksForPad = linksForCore.get(targPadObj);
      if (linksForPad == null) {
        linksForPad = new ArrayList<String>();
        linksForCore.put(targPadObj, linksForPad);
      }
      linksForPad.add(linkID);
    }

    //
    // There may be inbound links directly into the fan-out.  These get ordered:
    //
    
    TreeMap<FanOrdering, List<String>> fanOutLinks = new TreeMap<FanOrdering, List<String>>();
    ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String targID = link.getTarget();
      if (!fanOutNodes_.contains(targID)) {
        continue;
      }
      int targPad = getCurrentLandingPad(link, padChanges, false);      
      Grid.RowAndColumn randc = fanOutTrackedGrid_.findPositionRandC(targID);    
      FanOrdering fi = new FanOrdering(randc, targPad);
      List<String> linksForPad = fanOutLinks.get(fi);
      if (linksForPad == null) {
        linksForPad = new ArrayList<String>();
        fanOutLinks.put(fi, linksForPad);
      }
      linksForPad.add(linkID);
    }

    //
    // Inbound links into the fan in:
    //
    
    TreeMap<FanOrdering, List<String>> fanInLinks = new TreeMap<FanOrdering, List<String>>();    
    if (fanInType_ == SIMPLE_FAN_IN_) {
      // FIX ME: This is a quickie hack!
      ilit = inboundLinks_.iterator();
      while (ilit.hasNext()) {
        String linkID = ilit.next();
        Linkage link = nps.getLinkage(linkID);
        String targID = link.getTarget();
        if (!fanInNodes_.contains(targID)) {
          continue;
        }
        FanOrdering fakeKey = new FanOrdering(new Grid.RowAndColumn(0, 0), 0);
        List<String> linksForPad = fanInLinks.get(fakeKey);
        if (linksForPad == null) {
          linksForPad = new ArrayList<String>();
          fanInLinks.put(fakeKey, linksForPad);
        }
        linksForPad.add(linkID);
      }      
    } else if (fanInType_ == COMPLEX_FAN_IN_) {
      ilit = inboundLinks_.iterator();
      while (ilit.hasNext()) {
        String linkID = ilit.next();
        Linkage link = nps.getLinkage(linkID);
        String targID = link.getTarget();
        if (!fanInNodes_.contains(targID)) {
          continue;
        }
        if (!fanInTrackedGrid_.contains(targID)) {
          System.err.println("what happened to " + targID);
          continue;
        }        
        int targPad = getCurrentLandingPad(link, padChanges, false);      
        Grid.RowAndColumn randc = fanInTrackedGrid_.findPositionRandC(targID);
        FanOrdering fi = new FanOrdering(randc, targPad);
        List<String> linksForPad = fanInLinks.get(fi);
        if (linksForPad == null) {
          linksForPad = new ArrayList<String>();
          fanInLinks.put(fi, linksForPad);
        }
        linksForPad.add(linkID);
      }
    }
    
    //
    // Merge:
    //
    
    List<String> retval = new ArrayList<String>();
    Iterator<List<String>> filit = fanInLinks.values().iterator();
    while (filit.hasNext()) {
      List<String> links = filit.next();
      retval.addAll(links);
    }
    Iterator<List<String>> lfcit = linksForCore.values().iterator();
    while (lfcit.hasNext()) {
      List<String> links = lfcit.next();
      retval.addAll(links);
    }    
    Iterator<List<String>> folit = fanOutLinks.values().iterator();    
    while (folit.hasNext()) {
      List<String> links = folit.next();
      retval.addAll(links);
    }    
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private Vector2D launchPadToOffset(int padNum, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String nodeID) {
    return (TrackedGrid.launchPadToOffset(nps, irx, padNum, nodeID));
  }  
   
  /***************************************************************************
  ** 
  ** A function
  */
  
  private Vector2D landingPadToOffset(int padNum, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String nodeID) { 
    return (TrackedGrid.landingPadToOffset(appState_, padNum, nps, irx, nodeID, Linkage.POSITIVE)); // sign dontCare
  }
  
  /***************************************************************************
  ** 
  ** Figure out any extra gene length needed to handle pad requirements
 
  
  private double calcExtraGeneLength(Genome genome) {
    
    Node node = genome.getNode(coreID_);    
    if (node.getNodeType() != Node.GENE) {
      return (0.0);
    }

    ArrayList<Linkage> linksToOrder = new ArrayList<Linkage>();
    ArrayList<Linkage> feedbacks = new ArrayList<Linkage>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String lsource = link.getSource();
      String ltrg = link.getTarget();        
      if (ltrg.equals(coreID_)) {
        if (lsource.equals(coreID_)) {
          feedbacks.add(link);
        } else {
          linksToOrder.add(link);
        }
      }      
    }
    
    int numLinks = linksToOrder.size();
    // Simple fan-ins triple the spacing:
    if (fanInType_ == SIMPLE_FAN_IN_) {
      numLinks += (2 * fanInNodes_.size());
    }
    int numFeeds = feedbacks.size();
    
    int currFeed = DBGene.DEFAULT_PAD_COUNT - 2 - numFeeds;          
    int currPad = (numLinks < currFeed) ? numLinks - 1 : currFeed;
    int minPad = currPad - numLinks + 1;
    
    //
    // If gene is already big, we do not shrink it, but use that size:
    //
      
    Gene gene = (Gene)node;
    int currPads = gene.getPadCount();
    int minExistingPad = DBGene.DEFAULT_PAD_COUNT - currPads;
    if (minExistingPad > 0) {
      minExistingPad = 0;
    }
    
    int retpad = (minPad < minExistingPad) ? minPad : minExistingPad;
    return (GeneFree.EXTRA_WIDTH_PER_PAD * -(double)retpad);
  }
  
  /***************************************************************************
  **
  ** Fill out target clusters.  Keep sucking in bubbles that ultimately only
  ** hit the target, even if it is by way of other bubbles.
  */
  
  private void fillTargetClusterGreedy(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    
    FanBuilder builder = new FanBuilder();
    HashMap<String, Set<String>> sources = new HashMap<String, Set<String>>();
    HashSet<Integer> okTypes = new HashSet<Integer>();
    okTypes.add(new Integer(Node.BUBBLE));
    Set<String> survivors = builder.buildFanIn(nps, coreID_, sources, okTypes);
    HashSet<String> emptySet = new HashSet<String>();
    
    Iterator<String> sit = survivors.iterator();
    while (sit.hasNext()) {
      String survivor = sit.next();
      Set<String> surviveSrcs = sources.get(survivor);
      addSources((surviveSrcs == null) ? emptySet : surviveSrcs);
      if (!survivor.equals(coreID_)) {
        addPenultimate(survivor, surviveSrcs);
      }
    }
    
    //
    // Used to be we ran thru all linkages to pull this info out. That was crap,
    // and we won't do it again!
    //
    
    HashSet<String> linksWeCare = new HashSet<String>();
    Iterator<String> finit = fanInNodes_.iterator();
    while (finit.hasNext()) {
      String nodeID = finit.next();
      linksWeCare.addAll(nps.inboundLinkIDs(nodeID));
      linksWeCare.addAll(nps.outboundLinkIDs(nodeID));    
    }
    Iterator<String> fonit = fanOutNodes_.iterator();
    while (fonit.hasNext()) {
      String nodeID = fonit.next();
      linksWeCare.addAll(nps.inboundLinkIDs(nodeID));
      linksWeCare.addAll(nps.outboundLinkIDs(nodeID));    
    }
    linksWeCare.addAll(nps.inboundLinkIDs(coreID_));
    linksWeCare.addAll(nps.outboundLinkIDs(coreID_));    
    
    Iterator<String> lit = linksWeCare.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = nps.getLinkage(linkID);
      String lsource = link.getSource();   
      if (makeLinkInbound(link)) {
        inboundLinks_.add(linkID);
        inputs_.add(lsource);
      } else if (makeLinkOutbound(link)) {
        outboundLinks_.add(linkID);
      } else if (makeLinkInternal(link)) {
        internalLinks_.add(linkID);
      }
    }    

    normalizeSources();
    
    fanInType_ = classifyFanIn(nps);
        
    // lo, frc null only if we are doing a gross topology check of network:
    if ((fanInType_ == COMPLEX_FAN_IN_) && nps.hasLayout() && (irx.getFrc() != null)) {
      fanInPadPlans_ = new HashMap<String, PadCalculatorToo.PadResult>();
      fanInTrackedGrid_ = new TrackedGrid(gridFanIns(nps), nps, irx, coreID_, textToo_);
      changeInternalFanPads(fanInTrackedGrid_, fanInNodes_, nps, fanInPadPlans_);
    }
    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Ask if Halo can handle
  */

  public boolean okForHalo(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    return (fanInNodes_.isEmpty() || (classifyFanIn(nps) != COMPLEX_FAN_IN_));
  }
  
  
  /***************************************************************************
  ** 
  ** Figure out the type of fan-in
  */

  private int classifyFanIn(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    //
    // If every inbound link is to the core, we have no fan-in.
    // If every fan-in node has one target, and it is the core,
    // and the node is tiny (bubble, slash, intercell), and it
    // has at most two links, and none are from
    // the same source, it is simple.
    // Else it is complex.
    //
    
    if (fanInNodes_.isEmpty()) {
      return (NO_FAN_IN_);
    }
    
    HashSet<String> seenNodes = new HashSet<String>();
    Iterator<String> ilit = internalLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String ltarg = link.getTarget();
      String lsrc = link.getSource();
      if (!fanInNodes_.contains(lsrc)) {
        continue;
      }
      if (!ltarg.equals(coreID_)) {
        return (COMPLEX_FAN_IN_);
      }
      if (seenNodes.contains(lsrc)) {
        continue;
      }
      seenNodes.add(lsrc);
      Node fanNode = nps.getNode(lsrc);
      INodeRenderer rend = nps.getNodeProperties(lsrc).getRenderer();
      if (!rend.simpleFanInOK()) {
        return (COMPLEX_FAN_IN_);
      }
      if (fanNode.getPadCount() > DBNode.getDefaultPadCount(fanNode.getNodeType())) {
        return (COMPLEX_FAN_IN_);  
      }
      int intoNode = nps.inboundLinkCount(lsrc);
      if (intoNode > 2) {
        return (COMPLEX_FAN_IN_);
      }
      int outOfNode = nps.outboundLinkCount(lsrc);
      if (outOfNode > 1) {
        return (COMPLEX_FAN_IN_);
      } 
      if (intoNode > nps.getSources(lsrc).size()) {
        return (COMPLEX_FAN_IN_);
      }
    }
    
    return (SIMPLE_FAN_IN_);
  }
  
  /***************************************************************************
  ** 
  ** Answers if the given link is inbound to this cluster
  */

  private boolean makeLinkInbound(Linkage link) {
    String ltarg = link.getTarget();
    String lsrc = link.getSource();
    boolean ownSource = fanInNodes_.contains(lsrc) || 
                        fanOutNodes_.contains(lsrc) || lsrc.equals(coreID_);
    boolean ownTarg = fanInNodes_.contains(ltarg) || 
                      fanOutNodes_.contains(ltarg) || ltarg.equals(coreID_);
    return (ownTarg && !ownSource);
  }  
  
  /***************************************************************************
  ** 
  ** Answers if the given link is outbound from this cluster
  ** (Note: Feedbacks from fanout nodes back into fan-in or core
  ** are considered outbound.  We are not responsible for link routing.   
  */

  private boolean makeLinkOutbound(Linkage link) {
    String ltarg = link.getTarget();
    String lsrc = link.getSource();
    boolean ownSourceFanInOrCore = fanInNodes_.contains(lsrc) || lsrc.equals(coreID_);     
    boolean ownSource = ownSourceFanInOrCore || fanOutNodes_.contains(lsrc);   
    boolean ownTargFanInOrCore = fanInNodes_.contains(ltarg) || ltarg.equals(coreID_);
    boolean ownTarg = ownTargFanInOrCore || fanOutNodes_.contains(ltarg);
    
    if (!ownSource) {
      return (false);
    }
    if (!ownTarg) {
      return (true);
    }
    if (ltarg.equals(coreID_) && lsrc.equals(coreID_)) {
      return (false);
    }
    // By now, I own the source, and the targ too.  Still outbound if the target is
    // in the fan-in or core, but the source is not in the fan-in or core.
    return (ownTargFanInOrCore && !ownSourceFanInOrCore);
  }
  
  /***************************************************************************
  ** 
  ** Answers if the given link is large-scale feedback
  */

  private boolean isLargeScaleFeedback(Linkage link) {
    if (!makeLinkOutbound(link)) {
      return (false);
    }  
    String ltarg = link.getTarget();
    boolean ownTarg = fanInNodes_.contains(ltarg) || ltarg.equals(coreID_);        
    return (ownTarg);
  }   

  /***************************************************************************
  ** 
  ** Answers if the given link is internal to this cluster
  */

  private boolean makeLinkInternal(Linkage link) {
    String ltarg = link.getTarget();
    String lsrc = link.getSource();
    boolean ownSourceFanInOrCore = fanInNodes_.contains(lsrc) || lsrc.equals(coreID_);     
    boolean ownSource = ownSourceFanInOrCore || fanOutNodes_.contains(lsrc);   
    boolean ownTargFanInOrCore = fanInNodes_.contains(ltarg) || ltarg.equals(coreID_);
    boolean ownTarg = ownTargFanInOrCore || fanOutNodes_.contains(ltarg);
    
    return (ownSource && ownTarg && !makeLinkOutbound(link));
  }  
 
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void orderFanInOrphans(SpecialtyLayoutEngine.NodePlaceSupport nps) {
    //
    // Any pens not placed are located now:
    //
    Iterator<String> pit = penultimate_.keySet().iterator();
    while (pit.hasNext()) {
      String penID = pit.next();
      if (!penOrder_.values().contains(penID)) {
        bumpPenOrder(penID);
      }
      if (!toCoreFromPen_.containsKey(penID)) {
        Set<String> allLinks = nps.getLinksBetween(penID, coreID_);
        toCoreFromPen_.put(penID, allLinks);
      }
    }
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Set the node locations of the nodes in a simple fan-in.
  */
  
  private void locateSimpleFanIns(Point2D basePos, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {

    Iterator<Integer> poit = penOrder_.keySet().iterator();
    while (poit.hasNext()) {
      Integer order = poit.next();
      String penID = penOrder_.get(order);
      int orderNum = order.intValue();
      Point2D pos = simpleFanPosCalc(orderNum, penID, basePos, nps, irx);
      UiUtil.forceToGrid(pos, UiUtil.GRID_SIZE);
      nps.setPosition(penID, pos); 
    }
    return;
  }

  /***************************************************************************
  ** 
  ** A function
  */
  
  private Grid gridFanIns(SpecialtyLayoutEngine.NodePlaceSupport nps) {

    HashSet<String> allNodes = new HashSet<String>();
    HashSet<Link> allLinks = new HashSet<Link>();
    HashSet<String> nodesToPlace = new HashSet<String>();    
    ArrayList<DialogBuiltMotif> motifList = new ArrayList<DialogBuiltMotif>();
 
    int count = 0;
    Iterator<String> ilit = internalLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      // Don't grid fan out nodes!
      if (fanInNodes_.contains(src)) {
        if (fanOutNodes_.contains(trg)) {
          // FIX ME!!  BOGUS HACK DBCM needs singleton support.
          DialogBuiltMotif dbm = new DialogBuiltGeneralMotif(src, src, Integer.toString(count++));
          motifList.add(dbm);
          nodesToPlace.add(src);
          allNodes.add(src);
          Link sLink = new Link(src, src);
          allLinks.add(sLink);
        } else {
          DialogBuiltMotif dbm = new DialogBuiltGeneralMotif(src, trg, Integer.toString(count++));
          motifList.add(dbm);
          nodesToPlace.add(src);
          nodesToPlace.add(trg);
          allNodes.add(src);
          allNodes.add(trg);
          Link sLink = new Link(src, trg);
          allLinks.add(sLink);
        }
      }
    }
    LayoutOptions options = new LayoutOptions();
    RectangularTreeEngine rte = new RectangularTreeEngine();
    Grid grid = rte.layoutFanInOutHier(allNodes, allLinks, nodesToPlace, motifList, options, null, false);
    // Ditch the core if present.  It just messes things up if it ends up in 
    // a row by itself (which can happen if we have dead-end nodes in the fan-in)
    if (grid.contains(coreID_)) {
      Grid.RowAndColumn rAndC = grid.getRowAndColumn(grid.findPosition(coreID_));
      grid.setCellValue(rAndC.row, rAndC.col, null);
      if (grid.rowIsEmpty(rAndC.row)) {
        grid.dropRow(rAndC.row);
      }
    }
    return (grid);
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private Grid gridFanOuts(SpecialtyLayoutEngine.NodePlaceSupport nps) {

    //
    // Reduce source, target maps to link candidates
    // 
    
    HashSet<String> allNodes = new HashSet<String>();
    HashSet<Link> allLinks = new HashSet<Link>();
    HashSet<String> nodesToPlace = new HashSet<String>();    
    ArrayList<DialogBuiltMotif> motifList = new ArrayList<DialogBuiltMotif>();
 
    int count = 0;
    Iterator<String> ilit = internalLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      
      if (fanOutNodes_.contains(src) || src.equals(coreID_)) {
        String trg = link.getTarget();
        DialogBuiltMotif dbm = new DialogBuiltGeneralMotif(src, trg, Integer.toString(count++));
        motifList.add(dbm);
        nodesToPlace.add(src);
        nodesToPlace.add(trg);
        allNodes.add(src);
        allNodes.add(trg);
        Link sLink = new Link(src, trg);        
        allLinks.add(sLink);
      }
    }
    LayoutOptions options = new LayoutOptions();
    RectangularTreeEngine rte = new RectangularTreeEngine();
    //
    // Here's a trick!  For years, it seemed OK to have the coreID located in the fan-out grid
    // according to its position in the topo sort.  BUT that was because it almost always ended
    // up in column 0!  Just before V6 release, got a case where it was in column 2, and other
    // stuff got in the way.  So now we have a flag that forces the core to be on the leftmost
    // end of the topo sort!
    //
    Grid grid = rte.layoutFanInOutHier(allNodes, allLinks, nodesToPlace, motifList, options, coreID_, true);
    return (grid);
  }  
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  private void placeFan(TrackedGrid grid, Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    grid.placeGrid(upperLeft, nps, coreID_);
    return;
  }

 /***************************************************************************
  ** 
  ** Transfer precalculated pad changes
  */
 
  private void mergePrecalcPads(Set<String> nodes, Map<String, PadCalculatorToo.PadResult> preCalcPadChanges, SpecialtyLayoutEngine.NodePlaceSupport nps) {    
    Iterator<String> kit = preCalcPadChanges.keySet().iterator();
    Map<String, PadCalculatorToo.PadResult> finalPadChanges = nps.getPadChanges();
    while (kit.hasNext()) {
      String linkID = kit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      String trg = link.getTarget();
      PadCalculatorToo.PadResult prePres = preCalcPadChanges.get(linkID);
      PadCalculatorToo.PadResult newPres = finalPadChanges.get(linkID);
      // Nobody else has changed this link, so we can potentially just use the preCalc.  But,
      // if somebody else has changed the launch pad for the source, and we don't own the
      // source, we cannot override that change.
      if (newPres == null) {
        if (nodes.contains(src)) {
          finalPadChanges.put(linkID, prePres);
        } else {
          int preAssigned = newLaunchPadForSource(src, nps);
          if (preAssigned != Integer.MAX_VALUE) {
            finalPadChanges.put(linkID, new PadCalculatorToo.PadResult(preAssigned, prePres.landing));
          } else {
           finalPadChanges.put(linkID, prePres);
          }
        }
      } else {
        int newLaunch = (nodes.contains(src)) ? prePres.launch : newPres.launch;
        int newLand = (nodes.contains(trg)) ? prePres.landing : newPres.landing;
        finalPadChanges.put(linkID, new PadCalculatorToo.PadResult(newLaunch, newLand));
      }
    }
    return;
  }
  
  
 /***************************************************************************
  ** 
  ** Figure out if somebody else sets the launch for another link when we don't
  ** own the source.
  */
 
  private int newLaunchPadForSource(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    Map<String, PadCalculatorToo.PadResult> finalPadChanges = nps.getPadChanges();
    Iterator<String> kit = finalPadChanges.keySet().iterator();
    while (kit.hasNext()) {
      String linkID = kit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      if (src.equals(srcID)) {
        PadCalculatorToo.PadResult newPres = finalPadChanges.get(linkID);
        return (newPres.launch);
      }
    }
    return (Integer.MAX_VALUE);
  }  
  
  /***************************************************************************
  ** 
  ** Records internal pad changes for the given fan grid.  No core adjustments, but does
  ** handle from core to fan
  */

  private void changeInternalFanPads(TrackedGrid grid, Set<String> fanElements, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                     Map<String, PadCalculatorToo.PadResult> workingPads) {
       
    Iterator<String> ltpit = internalLinks_.iterator();
    List<String> secondPass = changeInternalFanPadsOnePass(ltpit, grid, fanElements, nps, workingPads, true);
    Iterator<String> tpit = secondPass.iterator();
    changeInternalFanPadsOnePass(tpit, grid, fanElements, nps, workingPads, false);    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Records internal pad changes for the given fan grid.  No core adjustments, but does
  ** handle from core to fan
  */

  private List<String> changeInternalFanPadsOnePass(Iterator<String> linkit, TrackedGrid grid, 
                                                    Set<String> fanElements, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                    Map<String, PadCalculatorToo.PadResult> workingPads, boolean leftEnterOnly) {
    ArrayList<String> retval = new ArrayList<String>();
    while (linkit.hasNext()) {
      String linkID = linkit.next();
      boolean coreSrc = false;
      boolean coreTrg = false;
      
      //
      // Make sure src and trg are in the fan:
      //
      Linkage link = nps.getLinkage(linkID);
      String lsrc = link.getSource();
      if (!fanElements.contains(lsrc)) {
        if (lsrc.equals(coreID_)) {
          coreSrc = true;
        } else {
          continue;
        }
      }
      String ltrg = link.getTarget();
      if (!fanElements.contains(ltrg)) {
        if (ltrg.equals(coreID_)) {
          coreTrg = true;        
        } else {
          continue;
        }
      }

      if (!changeInternalFanPadsGuts(grid, nps, workingPads, linkID, lsrc, ltrg, coreSrc, coreTrg, leftEnterOnly)) {
        retval.add(linkID);
      }
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Records internal pad changes for the given fan grid.  No core adjustments, but does
  ** handle from core to fan
  */

  private boolean changeInternalFanPadsGuts(TrackedGrid grid,
                                            SpecialtyLayoutEngine.NodePlaceSupport nps,
                                            Map<String, PadCalculatorToo.PadResult> workingPads,
                                            String linkID, String lsrc, String ltrg, 
                                            boolean coreSrc, boolean coreTrg, 
                                            boolean leftEnterOnly) {
       
    int launch;
    int landing;
    GridLinkRouter checker = new GridLinkRouter(appState_, grid, this);

    if (coreTrg) {
      return (false);
    } else if (coreSrc) {
      launch = TrackedGrid.launchForGrid(lsrc, nps);
      boolean canGoLeft = grid.canEnterOnLeft(lsrc, ltrg, nps);
      if (leftEnterOnly && !canGoLeft) {
        return (false);
      }
      Set<Integer> usedPads = padMap_.get(ltrg);
      if (usedPads == null) {
        usedPads = new HashSet<Integer>();
        padMap_.put(ltrg, usedPads);
      }

      landing = checker.landForThisGrid(ltrg, nps, usedPads, GridLinkRouter.GRID_LINK_E_ADJ, canGoLeft, false);
    } else {
      //
      // Get the positions:
      //  
      int linkDirection = checker.calcGridLinkType(grid, nps, lsrc, ltrg); 
      launch = TrackedGrid.launchForGrid(lsrc, nps);
      boolean canGoLeft = grid.canEnterOnLeft(lsrc, ltrg, nps);
      if (leftEnterOnly && !(canGoLeft && (linkDirection == GridLinkRouter.GRID_LINK_E_ADJ))) {
        return (false);
      }
      Set<Integer> usedPads = padMap_.get(ltrg);
      if (usedPads == null) {
        usedPads = new HashSet<Integer>();
        padMap_.put(ltrg, usedPads);
      }

      landing = checker.landForThisGrid(ltrg, nps, usedPads, linkDirection, canGoLeft, false);      
    }
    doPadChange(linkID, workingPads, launch, landing);
    return (true);
  }  

  /***************************************************************************
  ** 
  ** Records pad changes from fan-in to the core:
  */

  private int changeFanToCorePads(Set<String> fanElements, SpecialtyLayoutEngine.NodePlaceSupport nps, int coreMin, 
                                  Map<String, PadCalculatorToo.PadResult> oldPadPlans, boolean skipCore, TreeSet<Integer> usedPads, boolean forGene) {
    
    //
    // Need to know the total count from each src into the core:
    //
    
    Iterator<String> ltpit = internalLinks_.iterator();
    HashMap<String, Integer> totalFromSources = new HashMap<String, Integer>();
    HashMap<String, SortedMap<TaggedComparableInteger, String>> origLinkOrdering = new HashMap<String, SortedMap<TaggedComparableInteger, String>>();
    while (ltpit.hasNext()) {
      String linkID = ltpit.next();
      Linkage link = nps.getLinkage(linkID);
      String lsrc = link.getSource();
      String ltrg = link.getTarget();      
      if (fanElements.contains(lsrc) && ltrg.equals(coreID_)) {
        Integer fromSrc = totalFromSources.get(lsrc);
        if (fromSrc == null) {
          totalFromSources.put(lsrc, new Integer(1));
        } else {
          totalFromSources.put(lsrc, new Integer(fromSrc.intValue() + 1));
        }
        SortedMap<TaggedComparableInteger, String> orderForSrc = origLinkOrdering.get(lsrc);
        if (orderForSrc == null) {
          orderForSrc = new TreeMap<TaggedComparableInteger, String>();
          origLinkOrdering.put(lsrc, orderForSrc);
        }
        int landing = getCurrentLandingPad(link, oldPadPlans, false);  // actually the original order
        orderForSrc.put(new TaggedComparableInteger(new Integer(landing), linkID), linkID);
      } 
    }

    ltpit = internalLinks_.iterator();
    int newCoreMin = coreMin;
    while (ltpit.hasNext()) {
      String linkID = ltpit.next();
      //
      // Make sure src and trg are where we want them:
      //
      
      Linkage link = nps.getLinkage(linkID);
      String lsrc = link.getSource();
      if (!fanElements.contains(lsrc)) {
        continue;
      }
      String ltrg = link.getTarget();
      if (!ltrg.equals(coreID_)) {
        continue;
      }
            
      int launch = TrackedGrid.launchForGrid(lsrc, nps);
      int landing = (skipCore) ? getCurrentLandingPad(link, oldPadPlans, false) 
                               : getFanToCorePad(link, nps, coreMin, totalFromSources, origLinkOrdering, usedPads, forGene);
      doPadChange(linkID, nps.getPadChanges(), launch, landing);
      if (landing < newCoreMin) {
        newCoreMin = landing;
      }
    }
    return (newCoreMin);
  }  

  /***************************************************************************
  ** 
  ** Get current pad
  */

  public int getCurrentLandingPad(Linkage link, Map<String, PadCalculatorToo.PadResult> padChanges, boolean useCoreEstimates) {  
    PadCalculatorToo.PadResult pres = padChanges.get(link.getID());
    if (pres != null) {
      return (pres.landing);
    }
    if (useCoreEstimates && (multiInCoreEstimates_ != null)) {
      Integer coreEst = multiInCoreEstimates_.get(link.getID());
      if (coreEst != null) {
        return (coreEst.intValue());
      }
    }
    return (link.getLandingPad());
  }
  
  /***************************************************************************
  ** 
  ** Get current pad
  */

  public int getCurrentLaunchPad(Linkage link, Map<String, PadCalculatorToo.PadResult> padChanges) {  
    PadCalculatorToo.PadResult pres = padChanges.get(link.getID());
    return ((pres == null) ? link.getLaunchPad() : pres.launch);
  }    
  
  /***************************************************************************
  ** 
  ** Make the needed pad changes
  */

  private void doPadChange(String linkID, Map<String, PadCalculatorToo.PadResult> padChanges, int launch, int landing) {  
    PadCalculatorToo.PadResult pres = padChanges.get(linkID);
    if (pres != null) {
      pres.launch = launch;
      pres.landing = landing;  
    } else { 
      pres = new PadCalculatorToo.PadResult(launch, landing);
      padChanges.put(linkID, pres);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Make the needed pad changes
  */

  private void changeOutboundPads(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean isGeneCore) {

    Iterator<String> olit = outboundLinks_.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      Linkage link = nps.getLinkage(linkID);
      String lsrc = link.getSource();
      if (isGeneCore && lsrc.equals(coreID_)) {
        continue;  
      }
      int launch = TrackedGrid.launchForGrid(lsrc, nps);
      int landing = getCurrentLandingPad(link, nps.getPadChanges(), false);
      doPadChange(linkID, nps.getPadChanges(), launch, landing);          
    }
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Make the needed pad changes >>>>>>>>>>>> EXCEPT for core target!!!!!!!!
  */

  private void changeInboundPadsForFan(Set<String> fanNodes, TrackedGrid fanGrid, 
                                       SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                       Map<String, PadCalculatorToo.PadResult> workingPads,
                                       List<String> sourceOrder, boolean forceTop) {

    HashMap<String, Set<String>> targs = new HashMap<String, Set<String>>();
    Iterator<String> ilit = inboundLinks_.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String ltrg = link.getTarget();
      if (ltrg.equals(coreID_)) {
        continue;
      }
      if (!fanNodes.contains(ltrg)) {
        continue;
      }
      Set<String> linksForTarg = targs.get(ltrg);
      if (linksForTarg == null) {
        linksForTarg = new HashSet<String>();
        targs.put(ltrg, linksForTarg);
      }
      linksForTarg.add(linkID);
    }

    Iterator<String> trit = targs.keySet().iterator();
    while (trit.hasNext()) {
      String ltrg = trit.next();
      Set<String> linksForTarg = targs.get(ltrg);
      if (fanNodes.contains(ltrg)) {
        inboundLandingForGrid(ltrg, fanGrid, linksForTarg, nps, workingPads, sourceOrder, forceTop);
      } else {
        System.err.println("Where is " + ltrg);
      }
    }
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Make the needed pad changes for the given link set
  */

  private void changePadsForFanForJumperLinks(List<String> links, Set<String> fanNodes, TrackedGrid fanGrid, 
                                              SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                              Map<String, PadCalculatorToo.PadResult> workingPads,
                                              List<String> sourceOrder, boolean forceTop) {

    HashMap<String, Set<String>> targs = new HashMap<String, Set<String>>();
    Iterator<String> ilit = links.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      Linkage link = nps.getLinkage(linkID);
      String ltrg = link.getTarget();
      Set<String> linksForTarg = targs.get(ltrg);
      if (linksForTarg == null) {
        linksForTarg = new HashSet<String>();
        targs.put(ltrg, linksForTarg);
      }
      linksForTarg.add(linkID);
    }

    Iterator<String> trit = targs.keySet().iterator();
    while (trit.hasNext()) {
      String ltrg = trit.next();
      Set<String> linksForTarg = targs.get(ltrg);
      if (fanNodes.contains(ltrg)) {
        inboundLandingForGrid(ltrg, fanGrid, linksForTarg, nps, workingPads, sourceOrder, forceTop);
      } else {
        System.err.println("Where is " + ltrg);
      }
    }
    return;
  }   

  /***************************************************************************
  ** 
  ** Figure out the inbound landing pad for grid nodes.
  */

  private void inboundLandingForGrid(String targID, TrackedGrid grid, Set<String> linksToTarg, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                     Map<String, PadCalculatorToo.PadResult> workingPads, List<String> sourceOrder, boolean forceTop) {
    //
    // Guys in interior come into the top pad(s!).
    // Guys in the first column get single landings on the left side.
    // Multiple landings use left first, then top pad(s!).  This
    // order determined by inbound trace order.
    //
    
    if (!grid.contains(targID)) {
      return;
    }
    GridLinkRouter checker = new GridLinkRouter(appState_, grid, this);
    boolean isFirst = grid.firstOnLeft(targID, null);
    
    Set<Integer> usedPads = padMap_.get(targID);
    if (usedPads == null) {
      usedPads = new HashSet<Integer>();
      padMap_.put(targID, usedPads);
    }
    
    //
    // Figure out all inbound sources:
    //
    
    HashSet<String> srcSet = new HashSet<String>();
    Iterator<String> ltit = linksToTarg.iterator();
    while (ltit.hasNext()) {
      String linkID = ltit.next();
      Linkage link = nps.getLinkage(linkID);
      String lsrc = link.getSource();
      srcSet.add(lsrc);
    }
    
    //checker
    // Bogus:  In the first pass, we do not know the source order.  So just
    // build an arbitrary one:
    //
    
    if (sourceOrder == null) {
      GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
      TreeSet<String> bogusSource = new TreeSet<String>(cc);
      bogusSource.addAll(srcSet);
      sourceOrder = new ArrayList<String>(bogusSource);
    }

    int numSrc = sourceOrder.size();
    for (int i = 0; i < numSrc; i++) {
      String srcID = sourceOrder.get(i);
      if (srcSet.contains(srcID)) {
        ltit = linksToTarg.iterator();
        while (ltit.hasNext()) {
          String linkID = ltit.next();
          Linkage link = nps.getLinkage(linkID);
          String lsrc = link.getSource();
          if (!lsrc.equals(srcID)) {
            continue;
          }
          int landing;
          if (isFirst) {
            landing = checker.landForThisGrid(targID, nps, usedPads, GridLinkRouter.GRID_LINK_E_ADJ, true, forceTop);    
            isFirst = false;
          } else {
            landing = checker.landForThisGrid(targID, nps, usedPads, GridLinkRouter.GRID_LINK_S_ADJ, false, false);
          }
          int launch = getCurrentLaunchPad(link, workingPads); 
          doPadChange(linkID, workingPads, launch, landing);            
        }
      }
    }

    return;
  }   
  
  /***************************************************************************
  ** 
  ** Simple grid launches;
  */
    
  private int launchForSimpleGrid(String nodeID, Genome genome) {
    int type = genome.getNode(nodeID).getNodeType();
    if (type == Node.BUBBLE) {
      return (2);
    } else {
      return (0);
    }
  }  
  
  /***************************************************************************
  ** 
  ** Simple grid landings
  */
    
  private int landForSimpleGrid(String nodeID, Genome genome) { 
    int type = genome.getNode(nodeID).getNodeType();
    if (type == Node.BUBBLE) {
      return (3);
    } else {
      return (0);
    }    
  }  
  
  /***************************************************************************
  ** 
  ** Simple grid landings
  */
    
  private int landForSimpleGridFromOutside(String nodeID, Genome genome, boolean highest) {
    int type = genome.getNode(nodeID).getNodeType();
    if (type == Node.BUBBLE) {
      return ((highest) ? 0 : 3);
    } else {
      return (0);
    }    
  }    
  
  
  /***************************************************************************
  ** 
  ** Report the node fan
  */

  private int nodeFanMembership(String nodeID) {
    if (nodeID.equals(coreID_)) {
      return (IS_CORE_);
    } else if (fanInNodes_.contains(nodeID)) {
      return (IS_FAN_IN_);   
    } else if (fanOutNodes_.contains(nodeID)) {
      return (IS_FAN_OUT_);   
    } else {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  ** 
  ** Categorize src/trg types:
  */

  private int getInternalLinkType(int srcMember, int trgMember) {  
    int linkType = GridLinkRouter.NO_LINK_TYPE;     
    if (srcMember == IS_CORE_) {
      if (trgMember == IS_CORE_) {
        linkType = GridLinkRouter.CORE_FEEDBACK;   
      } else if (trgMember == IS_FAN_IN_) {
        linkType = GridLinkRouter.FEEDBACK_ONTO_FAN_IN;   
      } else if (trgMember == IS_FAN_OUT_) {
        linkType = GridLinkRouter.CORE_TO_FANOUT;      
      }        
    } else if (srcMember == IS_FAN_IN_) {
      if (trgMember == IS_CORE_) {
        linkType = GridLinkRouter.FAN_IN_TO_CORE;     
      } else if (trgMember == IS_FAN_IN_) {
        linkType = GridLinkRouter.INTERNAL_FAN_IN;   
      } else if (trgMember == IS_FAN_OUT_) {
        linkType = GridLinkRouter.CORE_JUMPING;
      } 
    } else if (srcMember == IS_FAN_OUT_) {
      if (trgMember == IS_CORE_) {
        // feedback to core from fan-out; handled as external
        linkType = GridLinkRouter.FAN_OUT_FEEDBACK_TO_CORE;
      } else if (trgMember == IS_FAN_IN_) {
        // feedback to core from fan-out; handled as external 
        linkType = GridLinkRouter.FAN_OUT_FEEDBACK_TO_FAN_IN;
      } else if (trgMember == IS_FAN_OUT_) {
        linkType = GridLinkRouter.INTERNAL_FAN_OUT;
      }         
    }
      
    if (linkType == GridLinkRouter.NO_LINK_TYPE) {
      throw new IllegalStateException();
    }
    return (linkType);
  }
  
  /***************************************************************************
  ** 
  ** Categorize inbound link type:
  */

  private int getInboundLinkType(int trgMember) {  
    int linkType = GridLinkRouter.NO_LINK_TYPE;     
    if (trgMember == IS_CORE_) {
      linkType = GridLinkRouter.INBOUND_TO_CORE;   
    } else if (trgMember == IS_FAN_IN_) {
      linkType = GridLinkRouter.INBOUND_TO_FAN_IN;   
    } else if (trgMember == IS_FAN_OUT_) {
      linkType = GridLinkRouter.INBOUND_TO_FAN_OUT;      
    }        
    if (linkType == GridLinkRouter.NO_LINK_TYPE) {
      throw new IllegalStateException();
    }
    return (linkType);
  }
  
  /***************************************************************************
  ** 
  ** Do final inbound link routing for a simple fan-in (also no fan-in, correct??).
  ** Note this handles the links to the fan-out as well (but that step is done differently
  ** with complex fan-in).
  */

  private void simpleFanInInboundLinkRouting(List<String>linksPerClust, String srcID,  
                                             SpecialtyLayoutEngine.NodePlaceSupport nps,
                                             DataAccessContext irx, 
                                             SpecialtyLayoutLinkData sin, 
                                             MetaClusterPointSource lps,
                                             boolean linkFromTop, boolean topGASC) {  
    
    if (fanInType_ == COMPLEX_FAN_IN_) {
      throw new IllegalStateException();
    }

    TreeMap<Double, List<String>> sortByCoord = new TreeMap<Double, List<String>>();
    HashMap<String, List<Point2D>> holdDrops = new HashMap<String, List<Point2D>>();
    boolean useXCoord = lps.busFeedIsHorizontal() || (fanInType_ == NO_FAN_IN_);
    double sortSign = (!linkFromTop && !useXCoord) ? -1.0 : 1.0;
 
    Set<String> linksToCore = linksToCore(nps);
    int numlpc = linksPerClust.size();
    for (int j = 0; j < numlpc; j++) {
      String linkID = linksPerClust.get(j);
      List<Point2D> dropPts;
      if (linksToCore.contains(linkID)) {
        dropPts = directLinkDropPoints(linkID, nps, irx, lps);
      } else if (linkToFanOut(linkID, nps)) {
        continue;
      } else {
        dropPts = doLinkToDropPoints(linkID, nps, irx, lps);
      }
      Point2D firstPt = dropPts.get(0);
      double firstCoord = (useXCoord) ? firstPt.getX() : firstPt.getY();      
      Double firstCoordKey = new Double(sortSign * firstCoord);
      List<String> linksPerCoord = sortByCoord.get(firstCoordKey);
      if (linksPerCoord == null) {
        linksPerCoord = new ArrayList<String>();
        sortByCoord.put(firstCoordKey, linksPerCoord);
      }
      linksPerCoord.add(linkID);
      holdDrops.put(linkID, dropPts);
    }     

    boolean fanOutDone = false;

    if (linkFromTop) {
      fanOutInboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
      fanOutDone = true;
    } 
    
    simpleFanInFinalLinkRoutingLinkCreation(sin, lps, sortByCoord, holdDrops, srcID, nps, irx, (fanInType_ == NO_FAN_IN_));
    
    if (!fanOutDone) { 
      fanOutInboundLinkRouting(srcID, nps, irx, sin, lps, topGASC);
    }   
    lps.closeOutCluster();
      
    return;
  }

  /***************************************************************************
  ** 
  ** Answer if we do fan out
  */

  public boolean hasFanOut() {
    return (!fanOutNodes_.isEmpty()); 
  }
 
  /***************************************************************************
  ** 
  ** Do final inbound link routing for a complex fan-in.  ******** NOTE: This includes
  ** routing to the core!  After this is done, only fan-out inbounds are
  ** still needing work.
  */

  private void complexFanInInboundLinkRouting(String srcID,
                                              SpecialtyLayoutEngine.NodePlaceSupport nps,
                                              DataAccessContext irx,
                                              SpecialtyLayoutLinkData sin, 
                                              MetaClusterPointSource lps,
                                              boolean closeOut) {
    if (fanInType_ != COMPLEX_FAN_IN_) {
      throw new IllegalStateException();
    }
    
    boolean entryFromTop = (ddo_ == null) ? true : ddo_.comingFromTop(srcID, this);   
    
    //
    // Our destination is different if we are building for feedback (i.e. "deferred"):
    //
    
    boolean isDeferred = lps.isForDeferredOnly();
    GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners> perClust = 
      (isDeferred) ? lps.initInboundsPerSrcForDeferred(coreID_, srcID) : null;    
     
    Point2D upperLeftFanInPt = getClusterDims(nps, irx).getFanInCorner(nps);
       
    //
    // Get the tip to where we can use it:
    //
    
    SpecialtyLayoutLinkData dummySin = new SpecialtyLayoutLinkData(srcID);
    String dummyLink = "dummyLink";
    dummySin.startNewLink(dummyLink);
    
    SpecialtyLayoutLinkData converted;
    //
    // First case handles a drop from a horizontal source trace:
    //
    SpecialtyLayoutLinkData.TrackPos gluePos = null;
    double fixedX = 0.0;
    SpecialtyLayoutLinkData.TrackPos finalForClust = null;
    if (lps.getType() == MetaClusterPointSource.FOR_PURE_TARG) {
      converted = fanInGlr_.convertInboundLinks(srcID, inboundLinkPlans_, upperLeftFanInPt, nps, irx, coreID_);    
      if (converted == null) {
        return;
      }
      
      //
      // Build the glue point:
      //
            
      List<String> links = converted.getLinkList();
      String linkID = links.get(0);
      SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(linkID);
  
      finalForClust = convertedDrops.get(0);
      Point2D finalPt = finalForClust.getPoint();
      if (!isDeferred) {
        lps.fanBlockBuildPrePath(dummySin, dummyLink, finalPt, nps, irx);
      }
      Point2D interfacePt = lps.getComplexInterfacePoint();
      Point2D gluePt = new Point2D.Double(finalPt.getX(), interfacePt.getY());
      gluePos = new SpecialtyLayoutLinkData.TrackPos(gluePt);
      if (!isDeferred) {
        dummySin.addPositionToLink(dummyLink, gluePos);
      } 
    //
    // this handles a drop from a vertical trace on the left side:
    //
      
    } else {
      fixedX = lps.getVerticalDropX(nps, irx);
      converted = fanInGlr_.convertAndFixInboundLinks(srcID, inboundLinkPlans_, upperLeftFanInPt, nps, irx, coreID_, fixedX); 
      if ((converted == null) || (converted.getLinkList().size() == 0)) {
        return;
      }
      // Remember that in complex fan-ins, the core node is in the fan-in, and all links to the
      // core are being routed via fan-in calculations.  This is not really what we want for
      // stacked cases, where direct drops are best!
      // Given that complex fan-in calculations are designed to correctly handle the complex
      // case, check if a simplified direct approach works OK and chop off the complex prefix if we can.
      // Note that with mutiple links from the same source, we only want to chop off the one
      // that is cruising into the core.  The others need to be kept intact, since they are
      // connected directly into that one inbound.  If the first one is not the leftmost one, we
      // are screwed!
      
      // In stacked cases, with multiple links from same source, we would like
      // to have a nice short height for the inter-link tie.  The following commented code
      // could do that. BUT it would fail for cases where genes have defined
      // regions, which prevents pad changes.  There would be no guarantee that
      // the ties would not overlap if we set them to a single value!  The crappier
      // appearance is safer!  Plus core jumpers could mess us up too...
      
      List<String> links = converted.getLinkList();
      Set<String> canChopLinks = canChop(nps, links, srcID);
      if (!canChopLinks.isEmpty()) {
        
        //ClusterDims cd = getClusterDims(genome, lo, frc);
        //Point2D launch = cd.getCoreLaunch(placement);
        //double multiPadY = launch.getY() - STACK_Y_DELTA_ + UiUtil.GRID_SIZE;
        
        // WE HAVE TO ITERATE THROUGH THE links LIST AND SKIP, NOT THE canChopLinks SET, WHICH IS UNORDERED! 
        // Chop down the leftmost link.  Move EVERYBODY else down!
        Iterator<String> lit = links.iterator();
        boolean first = true;
        while (lit.hasNext()) {
          String linkID = lit.next();
          if (!canChopLinks.contains(linkID)) {
            continue;
          }
          SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(linkID);
          if (first) {
            first = false;
            int numPts = convertedDrops.size();
            for (int j = 0; j < numPts - 1; j++) {
              convertedDrops.remove(0);
            }
            finalForClust = convertedDrops.get(0);
            // Can't do this safely!
            //Point2D convPt = finalForClust.getPoint();
            //convPt.setLocation(convPt.getX(), multiPadY);
          } else {
            int numPts = convertedDrops.size();
            if (numPts != 2) {
              throw new IllegalStateException();
            }
            // Can't do this safely!
            //for (int j = 0; j < numPts; j++) {
            //  LinkRouter.TrackPos tp = (LinkRouter.TrackPos)convertedDrops.get(j);
            // Point2D convPt = tp.getPoint();
            //  convPt.setLocation(convPt.getX(), multiPadY);
            //}
          }
        }
      }
      if (lps.needsTraceYUpdate()) {        
        String linkID = links.get(0);
        SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(linkID);
        finalForClust = convertedDrops.get(0);
        if (!isDeferred) {
          lps.setTargetTraceY(finalForClust.getPoint().getY());          
        }
      }
      if (!isDeferred) {
        // Y does not matter for complex fan-ins:  (hacky---FIX ME)
        lps.fanBlockBuildPrePath(dummySin, dummyLink, new Point2D.Double(fixedX, 0.0), nps, irx); 
      }
    }
       
    //
    // Now need to glue the converted geometry into the true link plans:
    //
    
    List<String> links = converted.getLinkList();
    Point2D furthestNewDrop = null;  // used to be "lowest", but this is on top if drop comes from below!
    Point2D rightmostTopDrop = null;
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      InboundCorners ic = null;
      if (isDeferred) {
        ic = new InboundCorners(linkID);
        // link order here matters!
        perClust.add(ic);
      } else {
        if (!sin.haveLink(linkID)) {
          sin.startNewLink(linkID);
        }
      }

      SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(linkID);
      if (i == 0) {        
        if (isDeferred) {
          ic.needDrop = true; 
          ic.dropY = finalForClust.getPoint().getY();
          convertedDrops.appendTo(ic.finalPath);
        } else {
          sin.addPositionListToLinkNZ(linkID, dummySin.getPositionList(dummyLink));
          sin.addPositionListToLinkNZ(linkID, convertedDrops);
        }
       
        //
        // Need to update the top leftmost trace:
        //
        int numPts = convertedDrops.size();
        for (int j = 0; j < numPts; j++) {
          SpecialtyLayoutLinkData.TrackPos nextForLink = convertedDrops.get(j);
          Point2D nextPtForLink = nextForLink.getPoint();
          if (rightmostTopDrop == null) {
            rightmostTopDrop = (Point2D)nextPtForLink.clone();
          } else if ((nextPtForLink.getY() == rightmostTopDrop.getY()) &&
                     (nextPtForLink.getX() > rightmostTopDrop.getX())) {
            rightmostTopDrop = (Point2D)nextPtForLink.clone();
          }   
        }

        finalForClust = convertedDrops.get(0);
        // LowestNewDrop needs to be >highest< new drop if dealing with
        // external inbounds to a above-trace supersource cluster!
        
        if (finalForClust.getPoint().getX() == fixedX) {
          furthestNewDrop = (Point2D)finalForClust.getPoint().clone();
        }
        //
        // This stuff is used for pure targets:
        if (closeOut) lps.closeOutComplexCluster(gluePos);
      } else {
        if (isDeferred) {
          //
          // Crusing up the rows doing a feedback, we want to use the needDrop
          // mechanism, and toss out the first segment of the dogleg if the link
          // is originating from the leftmost main drop.  BUT that little cosmetic
          // tweak is all wrong for the stacked case, where "feedback" links are really
          // just regular out-of row inbound links!
          //
          convertedDrops.appendTo(ic.finalPath);
          finalForClust = convertedDrops.get(0);
          if ((finalForClust.getPoint().getX() == fixedX) && !isStacked_) { 
            ic.needDrop = true;
            if (convertedDrops.size() > 1) {
              finalForClust = convertedDrops.get(1);
              ic.finalPath.remove(0);
            }
            ic.dropY = finalForClust.getPoint().getY();
          } else {
            ic.needDrop = false;
            ic.prevPoint = null;
          }
        } else {  
          sin.addPositionListToLinkNZ(linkID, convertedDrops);
        }    
        //
        // Need to update the drop point for the bottom/top of the fan-in:
        //
        int numPts = convertedDrops.size();
        for (int j = 0; j < numPts; j++) {
          SpecialtyLayoutLinkData.TrackPos nextForLink = convertedDrops.get(j);
          Point2D nextPtForLink = nextForLink.getPoint();
          if (furthestNewDrop != null) {
            double yDiff = nextPtForLink.getY() - furthestNewDrop.getY();
            boolean replaceY = (entryFromTop) ? (yDiff > 0.0) : (yDiff < 0.0); 
            if ((nextPtForLink.getX() == furthestNewDrop.getX()) && replaceY) {
              furthestNewDrop = (Point2D)nextPtForLink.clone();
            }
          }
          if ((nextPtForLink.getY() == rightmostTopDrop.getY()) &&
              (nextPtForLink.getX() > rightmostTopDrop.getX())) {
            rightmostTopDrop = (Point2D)nextPtForLink.clone();
          }   
        }
      }
    }
    if (lps.getType() != MetaClusterPointSource.FOR_PURE_TARG) {  // FIX ME!!!
      if (furthestNewDrop != null) {
        lps.updateDropPoint(furthestNewDrop);
      }
      if (rightmostTopDrop != null) {
        lps.updateRightmostDropPoint(rightmostTopDrop);
      } 
    }
    return;
  } 

  /***************************************************************************
  ** 
  ** Remember that in complex fan-ins, the core node is in the fan-in, and all links to the
  ** core are being routed via fan-in calculations.  This is not really what we want for
  ** stacked cases, where direct drops are best!
  ** Given that complex fan-in calculations are designed to correctly handle the complex
  ** case, check for each link if a simplified direct approach works OK and chop off
  ** the complex prefix if we can:
  */

  private Set<String> canChop(SpecialtyLayoutEngine.NodePlaceSupport nps, List<String> links, String onlyForSrcID) {
    HashSet<String> retval = new HashSet<String>();
    if (fanInType_ != COMPLEX_FAN_IN_) {
      return (retval);
    }
    if (isStacked_) {
      HashMap<String, String> maybeRetval = new HashMap<String, String>();
      HashSet<String> seenSrcs = new HashSet<String>();
      int numLinks = links.size();
      for (int i = 0; i < numLinks; i++) {
        String linkID = links.get(i);
        Linkage link = nps.getLinkage(linkID);
        String target = link.getTarget();
        String src = link.getSource();
        if ((onlyForSrcID != null) && !src.equals(onlyForSrcID)) {
          continue;
        }
        if (seenSrcs.contains(src)) {
          if (maybeRetval.get(src) != null) {
            maybeRetval.remove(src);
          }
          continue;
        }
        seenSrcs.add(src);
        if (target.equals(coreID_)) {          
          maybeRetval.put(src, linkID);
        // If > 1, still need the left grid points to set up drops into lower rows!
        // If more than one target in first row, we keep the geometry to keep everything kosher!
        } else if (fanInNodes_.contains(target) && (fanInTrackedGrid_.getNumRows() == 1)) {
          Grid.RowAndColumn grac = fanInTrackedGrid_.findPositionRandC(target);
          if (grac.row != 0) {
            throw new IllegalStateException();
          }  
          maybeRetval.put(src, linkID);
        }
      }
      retval.addAll(maybeRetval.values());
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Handle routing of "Core jumpers" (fan-in direct to fan-out)
  */

  private Map<String, SpecialtyLayoutLinkData> fanOutConvertCoreJumpers(SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                                                        DataAccessContext irx) {

    HashMap<String, SpecialtyLayoutLinkData> retval = new HashMap<String, SpecialtyLayoutLinkData>();
    if (coreJumpers_ == null) {
      return (retval);
    }
        
    Iterator<String> cjsit = coreJumpers_.keySet().iterator();
    while (cjsit.hasNext()) {
      String srcID = cjsit.next();
      SpecialtyLayoutLinkData sin = new SpecialtyLayoutLinkData(srcID);
      fanOutConvertAJumper(srcID, nps, irx, sin);
      retval.put(srcID, sin);
    }  
    return (retval);
  }   
    
  /***************************************************************************
  ** 
  ** Handle routing of "Core jumpers" (fan-in direct to fan-out)
  */

  private void fanOutConvertAJumper(String srcID, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                    DataAccessContext irx, SpecialtyLayoutLinkData sin) {
    
    Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, false);
    if (upperLeftFanOutPt == null) {
      return;   
    }
    Point2D upperLeftFanInPt = getClusterDims(nps, irx).getFanInCorner(nps);
 
    //
    // The link plans have contributions from two grids.  Let them each have a shot:
    //

    SpecialtyLayoutLinkData converted = 
      fanInGlr_.convertInboundLinks(srcID, fanOutInboundLinkPlans_, upperLeftFanInPt, nps, irx, coreID_);
    if (converted == null || converted.getLinkList().isEmpty()) {
      return;
    }
    
    HashMap<String, SpecialtyLayoutLinkData> hackMap = new HashMap<String, SpecialtyLayoutLinkData>();
    hackMap.put(srcID, converted);               
    Point2D lastConvert = fanInGlr_.getLastConvertedPoint(converted);       
    double lastConvertY = (lastConvert == null) ? upperLeftFanOutPt.getY() : lastConvert.getY();  // safety valve only...
    
    converted = fanOutGlr_.convertAndFixInboundLinks(srcID, hackMap, upperLeftFanOutPt, nps, irx, coreID_, lastConvertY);
    if (converted == null || converted.getLinkList().isEmpty()) {
      return;
    }    
    
    //
    // Now need to glue the converted geometry into the true link plans:
    //
    
    List<String> links = converted.getLinkList();
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i);
      if (!sin.haveLink(linkID)) {
        sin.startNewLink(linkID);
      }
      SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(linkID); 
      sin.addPositionListToLinkNZ(linkID, convertedDrops);
    }
    return;
  } 

  /***************************************************************************
  ** 
  ** Do final inbound link routing direct to the fan out.
  */

  private void fanOutInboundLinkRouting(String srcID,
                                        SpecialtyLayoutEngine.NodePlaceSupport nps,
                                        DataAccessContext irx, 
                                        SpecialtyLayoutLinkData sin, 
                                        MetaClusterPointSource lps, boolean topGASC) {

    if (fanOutGlr_ == null) {
      return;
    }
    
    //
    // Our destination is different if we are building for feedback:
    //
    
    boolean isDeferred = lps.isForDeferredOnly();  
    GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners> perClust = 
      (isDeferred) ? lps.initInboundsPerSrcForDeferred(coreID_, srcID) : null;    
     
    Point2D upperLeftFanOutPt = getClusterDims(nps, irx).getFanOutCorner(nps, false);
    if (upperLeftFanOutPt == null) {
      return;   
    }
    
    SpecialtyLayoutLinkData converted =
      fanOutGlr_.convertInboundLinks(srcID, fanOutInboundLinkPlans_, upperLeftFanOutPt, nps, irx, coreID_);
    if (converted == null || converted.getLinkList().isEmpty()) {
      return;
    }
    
    //
    // FIXME?  Maybe handle direct drops to the top row of the fan out?  Look at fan-in case.
    // But maybe best handled through local link optimization after layout is complete...
    
    // ATTENTION!!! IMPORTANT NOTE!  If you are looking for code that calculates a way to eliminate
    // dog legs into the fan out, forget it!  As indicated above, we run an eliminateStaggeredRuns
    // step after completing the layout.  Problem is, this is only done to **full genome cases**, since
    // subset issues would need to be addressed.  This results in a difference between handling 
    // direct drops into fanout nodes in a VfG versus the VfA per-region version that does NOT 
    // go direct; it does the standard dog-leg! 11/26/12.
    //
      
    List<String> links = converted.getLinkList();
    String firstLinkID = links.get(0);
    SpecialtyLayoutLinkData.NoZeroList convertedDrops = converted.getPositionList(firstLinkID);
    SpecialtyLayoutLinkData.TrackPos finalForClust = convertedDrops.get(0);
    double yAtFanOut = finalForClust.getPoint().getY();
    
    //
    // We need to get the inbound traces heading directly into the fan-out over all the junk
    // hanging over the gene (both simple and complex fan-ins).
    // we need to create separate traces for each fan-out link.
    //
 
    //
    // For GASCs that are at the top of a superSourceCluster, we want the nice loop over that
    // follows the inbound traces.  For anybody else, we need to follow the official inbound
    // source order that is less pretty but handles multi-GASC clusters.
    //
    
    Map<String, Integer> inbound;
    if (topGASC && lps.hasInboundTraces()) {
      inbound = lps.getInboundTraces();
      boolean isBaseAtTop = (ddo_ == null) ? true : ddo_.isBaseAtTop();
      if (!isBaseAtTop) { 
        inbound = DataUtil.negateMapToIntegers(inbound);
      }
    } else {
      inbound = getInboundSourceOrder(nps, nps.getPadChanges());
    }
    
    double yOverCore;
    
    if (isStacked_) {
       yOverCore = yAtFanOut;     
    } else {
      yOverCore = inboundTraceY(srcID, true, inbound, nps, irx);
      if (lps.needsTraceYUpdate()) {  // Always true when fan-out is present, right?
        lps.setTargetTraceY(yOverCore);      
      }
    }
    
    //
    // In stacked cases, the above yOverCore value is too low (e.g. height
    // of fan-out.
    //
    
    if (isStacked_) {
      yOverCore = yAtFanOut;     
    }
        
    //
    // Gotta glue fanout into main drop.  How depends on whether we are dealing with
    // simple or complex fan-ins, and if we are feedback or not.
    
    SpecialtyLayoutLinkData dummySin = new SpecialtyLayoutLinkData(srcID);
    String dummyLink = "dummyLink";
    dummySin.startNewLink(dummyLink);

    Point2D hopOverPt = null;
    SpecialtyLayoutLinkData.TrackPos hopOver = null;
    if (yOverCore != yAtFanOut) {
      hopOverPt = new Point2D.Double(finalForClust.getPoint().getX(), yOverCore);
      hopOver = new SpecialtyLayoutLinkData.TrackPos(hopOverPt);
    } else {
      hopOverPt = finalForClust.getPoint();
      // note hopOver stays null...
    }
    
    InboundCorners preCalcIc = null;
     
    if (!isDeferred) {
      lps.fanBlockBuildPrePath(dummySin, dummyLink, hopOverPt, nps, irx);
    } else { // deferred cases still need a drop attachment
      preCalcIc = new InboundCorners(firstLinkID);
      preCalcIc.needDrop = true;
      preCalcIc.dropY = yOverCore;
      preCalcIc.prevPoint = null;                    
      if (hopOver != null) {
        preCalcIc.finalPath.add(hopOver);
      }
    }   
     
    //
    // Now need to glue the converted geometry into the true link plans:
    //
   
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      String linkID = links.get(i); 
      InboundCorners ic = null;
      if (isDeferred) {
        // link order here matters!
        ic = new InboundCorners(linkID);
        perClust.add(ic);
      } else {
        if (!sin.haveLink(linkID)) {
          sin.startNewLink(linkID);
        }
      }
      
      convertedDrops = converted.getPositionList(linkID);
      
      if (i == 0) {
        if (isDeferred) {
          ic.needDrop = preCalcIc.needDrop;
          ic.dropY = preCalcIc.dropY;
          ic.prevPoint = preCalcIc.prevPoint;
          ic.finalPath.addAll(preCalcIc.finalPath);          
          convertedDrops.appendTo(ic.finalPath);
        } else {
          sin.addPositionListToLinkNZ(linkID, dummySin.getPositionList(dummyLink));
          if (hopOver != null) {
            sin.addPositionToLink(linkID, hopOver);
          }
          sin.addPositionListToLinkNZ(linkID, convertedDrops);
        }
        lps.closeOutComplexCluster(finalForClust);
      } else {  // links after the first one...
        if (isDeferred) {
          ic.needDrop = false;
          ic.prevPoint = null;
          convertedDrops.appendTo(ic.finalPath);
        } else {  
          sin.addPositionListToLinkNZ(linkID, convertedDrops);
        }
      }
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** Ordering for multi-input links from same source.
  ** We need an early estimate for pad ordering of multiple inputs
  ** from the same source, so the links are created in the correct order.
  ** This is it. 
  */
  
  private void multiInputCorePadOrdering(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, Integer> existing) {

    Node node = nps.getNode(coreID_);
    if (node.getNodeType() == Node.GENE) {
      Gene gene = (Gene)node;
      // no changing of core if we have subregions!
      if (gene.getNumRegions() != 0) {
        return;
      }
    }
       
    TreeSet<String> orderThem = new TreeSet<String>(new MultiPadOrdering(nps));  
    HashMap<String, String> firstForSrc = new HashMap<String, String>();
    HashSet<String> processing = new HashSet<String>();
    Set<String> coreLinks = linksToCore(nps); 
    Iterator<String> tsit = coreLinks.iterator();
    while (tsit.hasNext()) {
      String linkID = tsit.next();
      Linkage link = nps.getLinkage(linkID);
      String src = link.getSource();
      if (processing.contains(src)) {
        orderThem.add(linkID);
      } else if (firstForSrc.containsKey(src)) {
        String prev = firstForSrc.get(src);
        orderThem.add(prev);
        orderThem.add(linkID);
        processing.add(src);
      } else {
        firstForSrc.put(src, linkID);
      }
    }
    
    int count = 0;
    Iterator<String> otit = orderThem.iterator();
    while (otit.hasNext()) {
      String linkID = otit.next();
      existing.put(linkID, new Integer(count++));      
    }
    return;
  }
 
  /***************************************************************************
  ** 
  ** Pad change code
  */
  
  private int calcPadChangesForCore(GenomeSubset subset, List<String> traceOrder, SpecialtyLayoutEngine.NodePlaceSupport nps) {

    Genome baseGenome = subset.getBaseGenome();
    //
    // Feedbacks from core tuck into the gene:
    //
  
    int currPad = DBGene.DEFAULT_PAD_COUNT - 2;
    Iterator<String> feedIt = feedbacks_.iterator();    
    while (feedIt.hasNext()) {
      String linkID = feedIt.next();
      Linkage feedLink = baseGenome.getLinkage(linkID);
      int launch = getCurrentLaunchPad(feedLink, nps.getPadChanges());
      int landing = currPad--;
      doPadChange(linkID, nps.getPadChanges(), launch, landing);
    } 

    //
    // Inputs from simple fan-ins are next:
    //
    
    if (fanInType_ == SIMPLE_FAN_IN_) {
      HashMap<String, Integer> pendingForCore = new HashMap<String, Integer>();
      currPad = simpleFanInPadChangesForFan(subset, traceOrder, nps, pendingForCore, currPad);
      Iterator<String> pit = pendingForCore.keySet().iterator();
      while (pit.hasNext()) {
        String linkID = pit.next();
        Integer pad = pendingForCore.get(linkID);
        Linkage link = baseGenome.getLinkage(linkID);
        int launch = getCurrentLaunchPad(link, nps.getPadChanges());
        int landing = pad.intValue();
        doPadChange(linkID, nps.getPadChanges(), launch, landing);
      }      
    }

    //
    // Now come links from external sources, in trace order.
    // First do setup:
    //
    
    int remainingLinks = 0;
    
    TreeMap<Integer, String> traceToSrc = new TreeMap<Integer, String>();
    HashMap<String, List<String>> srcToLinks = new HashMap<String, List<String>>();
    int numTrace = traceOrder.size();
    for (int i = 0; i < numTrace; i++) {
      String toSrc = traceOrder.get(i);
      traceToSrc.put(new Integer(i), toSrc);
      ArrayList<String> links = new ArrayList<String>();
      srcToLinks.put(toSrc, links);
    }    
     
    Iterator<String> ibit = inboundLinks_.iterator();
    while (ibit.hasNext()) {
      String linkID = ibit.next();
      Linkage link = baseGenome.getLinkage(linkID); 
      String trg = link.getTarget();
      if (!trg.equals(coreID_)) {
        continue;
      }
      String src = link.getSource();
      List<String> links = srcToLinks.get(src);
      if (links != null) {  // going to happen if we have non-bipartite topology?
        links.add(linkID);
        remainingLinks++;
      }
    }

    if (!fanInNodes_.isEmpty() && (fanInType_ == COMPLEX_FAN_IN_)) {    
      Iterator<String> ltpit = internalLinks_.iterator();
      while (ltpit.hasNext()) {
        String linkID = ltpit.next();
        Linkage link = baseGenome.getLinkage(linkID);
        String lsrc = link.getSource();
        String ltrg = link.getTarget();      
        if (fanInNodes_.contains(lsrc) && ltrg.equals(coreID_)) {
          remainingLinks++;
        }
      }
    }

    //
    // We prefer to land the links on the left side, if there is space.  So start
    // from the left if we can:
    //
    
    if (fanInType_ != SIMPLE_FAN_IN_) {
      if (currPad > remainingLinks) {
        currPad = remainingLinks;
      }
    }
    
    //
    // Now crank thru:
    //
 
    Iterator<Integer> tsit = traceToSrc.keySet().iterator();      
    while (tsit.hasNext()) {
      Integer tNum = tsit.next();
      String srcID = traceToSrc.get(tNum);
      List<String> links = srcToLinks.get(srcID);
      int numLink = links.size();   
      if (numLink > 1) {
        //
        // Gotta come up with a fixed ordering!
        //
        TreeSet<String> orderThem = new TreeSet<String>(new MultiPadOrdering(new SpecialtyLayoutEngine.NodePlaceSupport(baseGenome)));
        orderThem.addAll(links);
        links.clear();
        links.addAll(orderThem);
        // Since we are moving DOWN, we need to go backwards!
        Collections.reverse(links);
      }
      for (int i = 0; i < numLink; i++) {
        String linkID = links.get(i);
        Linkage link = baseGenome.getLinkage(linkID);
        int launch = getCurrentLaunchPad(link, nps.getPadChanges());
        int landing = currPad--;
        doPadChange(linkID, nps.getPadChanges(), launch, landing);
      }
    }

    //
    // Now come links from the fan-in:
    //
    
    if (!fanInNodes_.isEmpty() && (fanInType_ == COMPLEX_FAN_IN_)) {
      currPad = changeFanToCorePads(fanInNodes_, nps, currPad, fanInPadPlans_, false, null, true);    
    }
    
    return (currPad);
  }
  
  /***************************************************************************
  ** 
  ** Pad handling for simple fan-in.
  */
  
  private int simpleFanInPadChangesForFan(GenomeSubset subset, List<String> traceOrder, 
                                          SpecialtyLayoutEngine.NodePlaceSupport nps,
                                          Map<String, Integer> pendingForCore, int startPad) {
      
    if (fanInType_ != SIMPLE_FAN_IN_) {
      throw new IllegalStateException(); 
    } 
    
    Genome baseGenome = subset.getBaseGenome();
    //
    // Figure out the penID->core mapping
    //
    
    int retval;

    if (pendingForCore != null) {
      Iterator<Integer> pit = penOrder_.keySet().iterator();
      int currPad = startPad;
      while (pit.hasNext()) {
        Integer order = pit.next();
        String penID = penOrder_.get(order);
        currPad = penIDToPadnum(nps, penID);
        Set<String> allLinks = toCoreFromPen_.get(penID);
        Iterator<String> alit = allLinks.iterator();
        while (alit.hasNext()) {
          String linkID = alit.next();
          pendingForCore.put(linkID, new Integer(currPad--));
        }
      }
      retval = currPad;
    } else {
      retval = startPad;
    }
       
    Set<String> penKeys = penultimate_.keySet();
    int numTrace = traceOrder.size();

    //
    // For each bubble, find out the source that belongs on top:
    //
    
    HashMap<String, String> highestPerPen = new HashMap<String, String>();
    Iterator<String> penkit = penultimate_.keySet().iterator();
    while (penkit.hasNext()) {
      String penID = penkit.next();
      Set<String> pensrcs = penultimate_.get(penID);
      String maxSrc = null;
      Iterator<String> sit = pensrcs.iterator();
      while (sit.hasNext()) {
        String srcID = sit.next();
        for (int i = 0; i < numTrace; i++) {
          String toSrc = traceOrder.get(i);
          if (toSrc.equals(srcID)) {
            maxSrc = toSrc;
          }
        }
      }
      //
      // Bubbles with no inputs will have no maxSrc!
      //
      if (maxSrc != null) {
        highestPerPen.put(penID, maxSrc);
      }   
    }    
 
    //
    // Do the changes
    //

    
    Iterator<String> lit = subset.getLinkageSuperSetIterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = baseGenome.getLinkage(linkID);
      String lsource = link.getSource();
      String ltarg = link.getTarget();
      if (penKeys.contains(lsource)) {
        int launch = launchForSimpleGrid(lsource, baseGenome);
        // Bubble to core:
        if (ltarg.equals(coreID_)) {
          int landing = getCurrentLandingPad(link, nps.getPadChanges(), false);  // may change later...
          doPadChange(linkID, nps.getPadChanges(), launch, landing);
        // Bubble to bubble:
        } else if (penKeys.contains(ltarg)) {
          int landing = landForSimpleGrid(ltarg, baseGenome);
          doPadChange(linkID, nps.getPadChanges(), launch, landing);      
        }
      } else if (penKeys.contains(ltarg)) {
        int launch = getCurrentLaunchPad(link, nps.getPadChanges());
        // Outside to bubble:
        String highSrc = highestPerPen.get(ltarg);
        boolean isHighest = highSrc.equals(lsource);
        int landing = landForSimpleGridFromOutside(ltarg, baseGenome, isHighest);
        doPadChange(linkID,nps.getPadChanges(), launch, landing);
      }
    }

    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Figure out the pad changes needed 
  */
  
  private void calcPadChangesForNonGeneCore(GenomeSubset subset,
                                            List<String> sourceOrder, 
                                            SpecialtyLayoutEngine.NodePlaceSupport nps) {
       
    Genome baseGenome = subset.getBaseGenome();
    
    ArrayList<Linkage> linksToOrder = new ArrayList<Linkage>(); 
    ArrayList<Linkage> feedbacks = new ArrayList<Linkage>();
    Iterator<String> lit = subset.getLinkageSuperSetIterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = baseGenome.getLinkage(linkID);
      String lsource = link.getSource();
      String ltrg = link.getTarget();        
      if (ltrg.equals(coreID_)) {
        if (lsource.equals(coreID_)) {
          feedbacks.add(link);
        } else {
          linksToOrder.add(link);
        }
      }      
    }

    Set<Integer> usedPads = padMap_.get(coreID_);
    if (usedPads == null) {
      usedPads = new HashSet<Integer>();
      padMap_.put(coreID_, usedPads);
    }    
    
    int numLinks = linksToOrder.size();
    int numS = sourceOrder.size();
    int currLanding = Integer.MAX_VALUE;
    for (int i = 0; i < numS; i++) {
      String srcID = sourceOrder.get(i);
      for (int j = 0; j < numLinks; j++) {
        Linkage link = linksToOrder.get(j);
        String lsource = link.getSource();
        if (!lsource.equals(srcID)) {
          continue;
        }
        currLanding = GridLinkRouter.landForAGrid(null, coreID_, nps, usedPads, GridLinkRouter.GRID_LINK_S_ADJ, false, false);
        int launch = getCurrentLaunchPad(link, nps.getPadChanges());
        doPadChange(link.getID(), nps.getPadChanges(), launch, currLanding);
      }
    }
    
    //
    // Inputs from simple fan-ins are next:
    //
 
    if (fanInType_ == SIMPLE_FAN_IN_) {
      HashMap<String, Integer> pendingForCore = new HashMap<String, Integer>();
      simpleFanInPadChangesForFan(subset, sourceOrder, nps, pendingForCore, 0);  // currPad: don't care 
      Iterator<String> pit = pendingForCore.keySet().iterator();
      while (pit.hasNext()) {
        String linkID = pit.next();
        Integer pad = pendingForCore.get(linkID);
        Linkage link = baseGenome.getLinkage(linkID);
        int launch = getCurrentLaunchPad(link, nps.getPadChanges());
        int landing = pad.intValue();
        doPadChange(linkID, nps.getPadChanges(), launch, landing);
        usedPads.add(new Integer(landing));
      }
      // Issue 217 fixed here by handling complex fan-ins:
    } else if (fanInType_ == COMPLEX_FAN_IN_) {    
      changeFanToCorePads(fanInNodes_, nps, currLanding, fanInPadPlans_, false, new TreeSet<Integer>(usedPads), false); 
    }

    ArrayList<String> revFeed = new ArrayList<String>(feedbacks_);
    int fSize = revFeed.size();
    for (int i = fSize - 1; i >= 0; i--) {
      String linkID = revFeed.get(i);
      Linkage feedLink = baseGenome.getLinkage(linkID);
      int landing = GridLinkRouter.landForAGrid(null, coreID_, nps, usedPads, GridLinkRouter.GRID_LINK_S_ADJ, false, false);
      int launch = getCurrentLaunchPad(feedLink, nps.getPadChanges());
      doPadChange(linkID, nps.getPadChanges(), launch, landing); 
    } 

    return;
  }  
  /***************************************************************************
  ** 
  ** Get the y coord for inbound trace to a cluster
  */
  
  private double inboundTraceY(String srcID, boolean toFanout, Map<String, Integer> inboundOrder, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                               DataAccessContext irx) {
    
    Point2D targLoc = nps.getPosition(coreID_);
    if (targLoc == null) {
      throw new IllegalStateException();
    }

    ClusterDims oh = getClusterDims(nps, irx); 
  
    //
    // Baseline trace height:
    //
    
    //
    // Sometimes corejumpers go thru below the top of the fan-in.  Other
    // times they pop to the top and go over.  We find out who has reserved
    // fan in slots (which get laid out every 10, not 20)
    //
       
    double dropY = targLoc.getY() - oh.offsetToGascPlacement;
    
    if (needsHorizontalTracesToFanInOrCore(nps)) {      
      int gottaHop = (fanInGlr_ == null) ? 0 : fanInGlr_.getTopReservations();
      dropY -= (UiUtil.GRID_SIZE * (gottaHop + 1));    
    }
    
    //
    // Special case!  We need to inflate the height for stacked cases where we
    // have multiple-link sources and nothing else (fan-in, fan-out).  This is
    // because they need to branch right above the gene:
    //
    // Previous test included fanOutNodes_.isEmpty() but created a problem with multi-fan-in case:
    if (!toFanout && isStacked_ && fanInNodes_.isEmpty()) {
      Set<String> multiSrc = multiSrcsToCore(nps);
      if (multiSrc.contains(srcID)) {      
        GenomeItemInstance.DBAndInstanceConsistentComparator cc = new GenomeItemInstance.DBAndInstanceConsistentComparator();
        TreeSet<String> bogusSource = new TreeSet<String>(cc);
        bogusSource.addAll(multiSrc);
        ArrayList<String> sourceOrder = new ArrayList<String>(bogusSource);
        int offset = sourceOrder.indexOf(srcID);
        return (dropY + (offset * UiUtil.GRID_SIZE));
      }
    } 
  
    //
    // Simple no fanout case:
    //
    
    if (fanOutNodes_.isEmpty()) {
      Integer order = inboundOrder.get(srcID);
      if (order == null) {
        throw new IllegalStateException();
      } 
      dropY -= (order.doubleValue() * traceOffset_);
      dropY = UiUtil.forceToGridValue(dropY, UiUtil.GRID_SIZE);
      return (dropY);
    }
    
    //
    // Separate tracks to core and to fanouts:
    //
 
    Set<String> srcsToCore = srcsToCore(nps);
    Set<String> srcsToFanout = srcsToFanout(nps);
    Set<String> srcsToFanin = srcsToFanin(nps);
  
    //
    // No traces to fanout, we just go with the inbound order, and
    // we are done:
    //
  
    if (srcsToFanout.isEmpty()) {
      Integer order = inboundOrder.get(srcID);
      if (order == null) {
        throw new IllegalStateException();
      } 
      dropY -= (order.doubleValue() * traceOffset_);
      dropY = UiUtil.forceToGridValue(dropY, UiUtil.GRID_SIZE);
      return (dropY);
    }    
    
    //
    // OK, we have links to the fanout that we need to consider:
    //
    
    //
    // Invert the order map so sources are ordered by entry:
    //
    
    TreeMap<Integer, String> invMap = new TreeMap<Integer, String>();
    Iterator<String> ioit = inboundOrder.keySet().iterator();
    while (ioit.hasNext()) {
      String chkSrc = ioit.next();
      Integer chkOrder = inboundOrder.get(chkSrc);
      invMap.put(chkOrder, chkSrc);
    }
   
    //
    // Split the traces into two sets:
    //
      
    int finalTrace = 0;
    
    //
    // Non-fanout.  Final trace depends on what inbounds are headed to the core.
    //
       
    if (!toFanout) {
      if (needsHorizontalTracesToFanInOrCore(nps)) {    
        int coreCount = 0;
        Iterator<String> ivit = invMap.values().iterator();
        while (ivit.hasNext()) {
          String chkSrc = ivit.next();
          if (chkSrc.equals(srcID)) {
            finalTrace = coreCount;
            break;
          } else {
            if (srcsToCore.contains(chkSrc)) {
              coreCount++; 
            }
          }
        }
      }         
      //
      // Fanout case.  Need top hop over
      //  
    } else {
      if (needsHorizontalTracesToFanOut(nps)) {    
        int coreCount = 0;
        //
        // With complex fan-ins, ALL inbound links were accounted
        // for with the above needsHorizontalTracesToFanInOrCore anInGlr_.getTopReservations() code.
        // With others, we need to do our own accounting to get the
        // hop-over:
        //
        if (fanInType_ != COMPLEX_FAN_IN_) {
          Iterator<String> ivit = invMap.values().iterator();
          while (ivit.hasNext()) {
            String chkSrc = ivit.next();
            if (srcsToCore.contains(chkSrc)) {
              coreCount++;
            } else if (srcsToFanin.contains(chkSrc)) {
              coreCount++;
            }
          }
        } else {
          coreCount++;  // pad for complex fan in reservation case
        }
        int fanCount = 0;
        Iterator<String> ivit = invMap.values().iterator();
        while (ivit.hasNext()) {
          String chkSrc = ivit.next();
          if (chkSrc.equals(srcID)) {
            finalTrace = fanCount + coreCount;
            break;
          } else {
            if (srcsToFanout.contains(chkSrc)) {
              fanCount++; 
            }
          }
        }            
      }
    }
 
    dropY -= (finalTrace * traceOffset_);
    dropY = UiUtil.forceToGridValue(dropY, UiUtil.GRID_SIZE);  
    return (dropY);
  }  
  
  /***************************************************************************
  ** 
  ** Get the ordering of inbound sources
  */

  private Map<String, Integer> getInboundSourceOrder(SpecialtyLayoutEngine.NodePlaceSupport nps, Map<String, PadCalculatorToo.PadResult> padChanges) {

    HashSet<String> seenSrcs = new HashSet<String>();
    HashMap<String, Integer> normalized = new HashMap<String, Integer>();
    
    List<String> linkOrder = getInboundLinkOrder(nps, padChanges);
    int numl = linkOrder.size();
    int count = 0; 
    for (int i = 0; i < numl; i++) {
      String linkID = linkOrder.get(i);
      Linkage link = nps.getLinkage(linkID);
      String srcID = link.getSource();
      if (seenSrcs.contains(srcID)) {
        continue;
      }
      seenSrcs.add(srcID);
      normalized.put(srcID, new Integer(count++));
    }
    return (normalized);
  }
  
  /***************************************************************************
  ** 
  ** Answer if the given fan-out is reachable from the core
  */

  private boolean canReachFromCore(String nodeID, Map<String, NodeGrouper.GroupElement> groups, Set<String> seenNodes) {    
    NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
    Set<String> sources = nodeInfo.sources;
    
    while (true) {
      Iterator<String> sit = sources.iterator();
      while (sit.hasNext()) {
        String srcID = sit.next();
        if (srcID.equals(coreID_)) {
          return (true);
        }
        if (seenNodes.contains(srcID)) {
          continue;
        }
        seenNodes.add(srcID);
        nodeInfo = groups.get(srcID);
        if ((nodeInfo == null) || !nodeInfo.group.equals(coreID_)) {
          continue;
        }
        if (canReachFromCore(srcID, groups, seenNodes)) {
          return (true);
        }
      }
      return (false);
    }
  }
  
  /***************************************************************************
  ** 
  ** Find nodes in fan-out dead end paths
  */

  private Set<String> fanOutDeadEnds(Set<String> fanOuts, Map<String, NodeGrouper.GroupElement> groups) { 
    
    //
    // Figure out fanouts that have no targets and which are not reachable
    // from the cores:
    
    HashSet<String> noTargs = new HashSet<String>();
    HashSet<String> remainingFO = new HashSet<String>();
    Iterator<String> foit = fanOuts.iterator();
    while (foit.hasNext()) {
      String nodeID = foit.next();
      NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
      int targSize = nodeInfo.targets.size();
      if (targSize == 0) {
        HashSet<String> seenNodes = new HashSet<String>();
        if (!canReachFromCore(nodeID, groups, seenNodes)) {  
          noTargs.add(nodeID);
        } else {
          remainingFO.add(nodeID);
        }
      } else if (targSize == 1) { // autofeedback only counts as no target
        String targID = nodeInfo.targets.iterator().next();
        HashSet<String> seenNodes = new HashSet<String>();
        if (targID.equals(nodeID) && !canReachFromCore(nodeID, groups, seenNodes)) {
          noTargs.add(nodeID);
        } else {
          remainingFO.add(nodeID);
        }
      } else {
        remainingFO.add(nodeID);
      }
    }
    
    // no seed candidates; nothing left to do.  There are no dead ends!
    
    if (noTargs.isEmpty()) {
      return (noTargs);
    }
    
    //
    // Still at least one guy left who has no targets.  Anybody who targets just him
    // (and is not reachable) gets kicked out too.  We grow until we can't grow anymore:
    //
     
    int lastNumNT = -1;
    int numNT = noTargs.size();
    
    while (numNT != lastNumNT) {
      Iterator<String> lit = remainingFO.iterator();
      while (lit.hasNext()) {
        String nodeID = lit.next();
        HashSet<String> seenNodes = new HashSet<String>();
        if (canReachFromCore(nodeID, groups, seenNodes)) {  
          continue;
        }
        NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
        HashSet<String> testSet = new HashSet<String>(nodeInfo.targets);
        testSet.remove(nodeID);  // autofeedback
        testSet.remove(noTargs);
        if (testSet.isEmpty()) {
          noTargs.add(nodeID);
        }
      }
      lastNumNT = numNT;
      numNT = noTargs.size();
    }
    
    return (noTargs);
  }

  /***************************************************************************
  ** 
  ** Find nodes targeting just the fan out
  */

  private Set<String> fanOutExclusiveTargets(Set<String> fanIns, Set<String> fanOuts, Map<String, NodeGrouper.GroupElement> groups) {     
    HashSet<String> shiftToFanOut = new HashSet<String>();
    
    //
    // Decide final disposition of fan-ins.  Some may move to the
    // fanout side if they just target fanouts.  Others may move
    // to fanout if the are targets of the core.
    //
    
    int lastNumSTFO = -1;
    int numSTFO = shiftToFanOut.size();
    
    while (numSTFO != lastNumSTFO) {
      Iterator<String> lit = fanIns.iterator();
      while (lit.hasNext()) {
        String nodeID = lit.next();
        NodeGrouper.GroupElement nodeInfo = groups.get(nodeID);
        if (nodeInfo.targets.size() == 0) {
          continue;
        }
        HashSet<String> testSet = new HashSet<String>(nodeInfo.targets);
        testSet.removeAll(fanOuts);
        testSet.removeAll(shiftToFanOut);        
        testSet.remove(nodeID);
        if (testSet.isEmpty()) {
          shiftToFanOut.add(nodeID);
        }
        HashSet<String> seenNodes = new HashSet<String>();
        if (canReachFromCore(nodeID, groups, seenNodes)) {
          shiftToFanOut.add(nodeID);
        }
        
      }
      lastNumSTFO = numSTFO;
      numSTFO = shiftToFanOut.size();
    }
    
    return (shiftToFanOut);
  }
  
  /***************************************************************************
  ** 
  ** Handle simple core feedbacks
  */

  private void simpleFeedbackRouting(SpecialtyLayoutLinkData sin, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx) {
    
    Point2D srcLoc = nps.getPosition(coreID_);
    Point2D rootPt = linkTreeRoot(nps, irx); 
    double upY = rootPt.getY() - STACK_Y_DELTA_;

    //
    // Order by feedback pad.  Highest goes first:
    //
    
    boolean isFirst = true;
    Point2D lastPoint = null;
    Iterator<List<String>> psit = (new GridLinkRouter(appState_, null, this)).orderedByPads(feedbacks_, nps, irx, nps.getPadChanges(), true);
    while (psit.hasNext()) {
      List<String> perPad = psit.next();
      int numPP = perPad.size();
      for (int i = 0; i < numPP; i++) {
        String linkID = perPad.get(i);
        sin.startNewLink(linkID);
        if (isFirst) {
          isFirst = false;
          sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos((Point2D)rootPt.clone()));
          Point2D upPoint = new Point2D.Double(rootPt.getX(), upY);
          sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(upPoint));
        } else {
          sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(lastPoint));
        }
        Linkage feedLink = nps.getLinkage(linkID);
        int landing = getCurrentLandingPad(feedLink, nps.getPadChanges(), false);
        Vector2D padOff = landingPadToOffset(landing, nps, irx, coreID_);
        Point2D padPt = padOff.add(srcLoc);
        Point2D dropPoint = new Point2D.Double(padPt.getX(), upY);
        lastPoint = dropPoint;
        sin.addPositionToLink(linkID, new SpecialtyLayoutLinkData.TrackPos(dropPoint));
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the order into the core from the fan
  */

  private List<String> getFanToCoreSourceOrder() {  
    ArrayList<String> retval = new ArrayList<String>();
    int numSo = fanInTraceOrder_.size();
    for (int i = 0; i < numSo; i++) {
      retval.add(fanInTraceOrder_.get(i).getSrc());
    }
    return (retval);
  }  
    
  /***************************************************************************
  ** 
  ** Get the fan-in to core pad
  */

  private int getFanToCorePad(Linkage link, SpecialtyLayoutEngine.NodePlaceSupport nps, int currPad,
                              Map<String, Integer> totalFromSources, 
                              Map<String, SortedMap<TaggedComparableInteger, String>> origLinkOrdering, TreeSet<Integer> usedPads, boolean forGene) {   
    //
    // Need to reference back to the original planned order to keep correct left-to-right order.
    //
    
    
    String lsource = link.getSource();
    String linkID = link.getID();
    SortedMap<TaggedComparableInteger, String> orderForSrc = origLinkOrdering.get(lsource);
    Iterator<String> vit = orderForSrc.values().iterator();
    int count = 0;
    while (vit.hasNext()) {
      String currLinkID = vit.next();
      if (currLinkID.equals(linkID)) {
        break;
      }
      count++;
    }
    
    INodeRenderer rend = nps.getNodeProperties(coreID_).getRenderer();
    
    if ((fanInTraceOrder_ == null) || fanInTraceOrder_.isEmpty()) {
      int pad;
      if (forGene) {
        pad = currPad - count;
      } else {
        pad = rend.getBestTopPad(nps.getNode(coreID_), usedPads, NodeProperties.HORIZONTAL_GROWTH, count, true);
      }
      return (pad);
    }
    
    //
    // If we are working with genes, we get a nice linear order of pads. With non-genes, we need to ask the renderer, 
    // and keep track of what pads have been handed out:
    //
    
    List<String> ordered = getFanToCoreSourceOrder();
    int numo = ordered.size();
    for (int i = 0; i < numo; i++) {
      String srcID = ordered.get(i);
      Integer totFromSrc = totalFromSources.get(srcID);
      int tfs = totFromSrc.intValue();
      if (srcID.equals(lsource)) {
        int pad;
        if (forGene) {
          pad = currPad - tfs + count;
        } else {
          pad = rend.getBestTopPad(nps.getNode(coreID_), usedPads, NodeProperties.HORIZONTAL_GROWTH, count, true);
          usedPads.add(Integer.valueOf(pad));
        }
        return (pad);
      }
      currPad -= tfs;
    }
    throw new IllegalArgumentException();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /*
  ** need height, offset of height block from baseline, and offset of GAS
  ** location (e.g. gene location) from baseline.
  */
  
  
  public class ClusterDims {
    public double height;
    public double offsetToFanInTop;
    public double offsetToFanOutTop;
    public double offsetToGascPlacement;
    public double launchToFanOutTop;
    public Vector2D gridLaunchPadOffset;
    // How to get to the fan-in from the core placement point:
    public Vector2D upperLeftFanInOffset;
    private Vector2D fullOffset_;
    private Vector2D linkShiftOffset_;
    
    public double horizTraceAddition;
    public double width;
    
    public ClusterDims(double height, double offsetToFanInTop, double offsetToFanOutTop, 
                        double offsetToGascPlacement, double horizTraceAddition, 
                        Vector2D gridLaunchPadOffset, Vector2D upperLeftFanInOffset,
                        Vector2D fullOffset, Vector2D linkShiftOffset,
                        double launchToFanOutTop, double width) {
      this.height = height;
      this.offsetToFanInTop = offsetToFanInTop;
      this.offsetToFanOutTop = offsetToFanOutTop;
      this.offsetToGascPlacement = offsetToGascPlacement;
      this.gridLaunchPadOffset = (gridLaunchPadOffset == null) ? null : (Vector2D)gridLaunchPadOffset.clone();
      this.upperLeftFanInOffset = (upperLeftFanInOffset == null) ? null : (Vector2D)upperLeftFanInOffset.clone();
      this.horizTraceAddition = horizTraceAddition;
      this.launchToFanOutTop = launchToFanOutTop;
      this.width = width;
      fullOffset_ = (fullOffset == null) ? null : (Vector2D)fullOffset.clone();
      linkShiftOffset_ = (linkShiftOffset == null) ? null : (Vector2D)linkShiftOffset.clone();     
    } 
    
    /***************************************************************************
    **
    ** Clone
    */

    @Override
    public ClusterDims clone() {
      try {
        ClusterDims retval = (ClusterDims)super.clone();
        retval.gridLaunchPadOffset = (this.gridLaunchPadOffset == null) ? null : this.gridLaunchPadOffset.clone();
        retval.upperLeftFanInOffset = (this.upperLeftFanInOffset == null) ? null : this.upperLeftFanInOffset.clone();
        retval.fullOffset_ = (this.fullOffset_ == null) ? null : this.fullOffset_.clone();
        retval.linkShiftOffset_ = (this.linkShiftOffset_ == null) ? null : this.linkShiftOffset_.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }

 
    public double getFullHeightIncrement() {
      return (height + horizTraceAddition);
    } 
   
    public Point2D getCoreLaunch(Map<String, Point2D> positions) {
      Point2D corePt = positions.get(coreID_);     
      Point2D coreLaunchPt = gridLaunchPadOffset.add(corePt);
      return (coreLaunchPt);
    }     
    
    public Point2D getFanInCorner(SpecialtyLayoutEngine.NodePlaceSupport nps) {
      Point2D corePt = nps.getPosition(coreID_);
      Point2D upperLeftFanInPt = upperLeftFanInOffset.add(corePt);
      upperLeftFanInPt.setLocation(upperLeftFanInPt.getX(), upperLeftFanInPt.getY());
      return (upperLeftFanInPt);
    } 
    
    public Point2D getFanOutCorner(SpecialtyLayoutEngine.NodePlaceSupport nps, boolean withLinkShift) {
      Point2D srcPt = nps.getPosition(coreID_);
      Point2D preRetval = fullOffset_.add(srcPt);
      //
      // Here is the deal.  For some applications, we want to know where to put corner.  That application
      // needs a shift to make way for the links coming in from above.  But for final link conversion, 
      // that code treats the corner as the origin of the first inbound link, and we do not want that
      // shift introduced. (withLinkShift == false);
      //
      // If the core is branching to multiple targets, it will also need a column, so we add that too:
      //
      Point2D retval = (withLinkShift) ? linkShiftOffset_.add(preRetval) : preRetval;
      return (retval);
    } 
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
         
  private static class FanOrdering implements Comparable<FanOrdering> {

    Grid.RowAndColumn rowCol;
    int padNum;
    
    FanOrdering(Grid.RowAndColumn rowCol, int padNum) {
      this.rowCol = rowCol;
      this.padNum = padNum;
    }

    public int hashCode() {
      return (rowCol.col + rowCol.row + padNum);
    }

    public String toString() {
      return ("FanInbound: " + rowCol + " " + padNum);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof FanOrdering)) {
        return (false);
      }
      
      FanOrdering otherFI = (FanOrdering)other; 

      return ((this.rowCol.col == otherFI.rowCol.col) &&
              (this.rowCol.row == otherFI.rowCol.row) &&              
              (this.padNum == otherFI.padNum));            
    } 

    public int compareTo(FanOrdering other) {

      //
      // Remember: Return neg if this is less than other
      //
            
      //
      // The one in a higher column is always
      // greater:
      //
      
      if (this.rowCol.col > other.rowCol.col) {
        return (1);
      } else if (this.rowCol.col < other.rowCol.col) {
        return (-1);
      }
      
      //
      // Ok, the column is the same.  The one with the
      // greater row is _less_ than!
      
      if (this.rowCol.row > other.rowCol.row) {
        return (-1);
      } else if (this.rowCol.row < other.rowCol.row) {
        return (1);
      }      
      
      //
      // Same position.  The one with the higher pad number
      // is less than (i.e. it is counterclockwise...FIX ME)
      //
      
      if (this.padNum > other.padNum) {
        return (-1);
      } else if (this.padNum < other.padNum) {
        return (1);
      } else {
        return (0);
      }  
    }
  }
  
  //
  // Fixed ordering for single-source, multi-link inbounds
  
  private static class MultiPadOrdering implements Comparator<String> {
    
    private SpecialtyLayoutEngine.NodePlaceSupport nps_;
    
    public MultiPadOrdering(SpecialtyLayoutEngine.NodePlaceSupport nps) {
      nps_ = nps;
    }
  
    public int compare(String linkIDA, String linkIDB) {
 
      Linkage linkA = nps_.getLinkage(linkIDA);
      Linkage linkB = nps_.getLinkage(linkIDB);
      
      // Allows us to order sets from more than one source
      // all at once:
      String srcA = linkA.getSource();
      String srcB = linkB.getSource();
      
      if (!srcA.equals(srcB)) {
        return (srcA.compareTo(srcB));       
      }
           
      int signA = linkA.getSign();
      int signB = linkB.getSign();
      
      if (signA != signB) {
        return (signA - signB);
      }    
      // Arbitrary, but fixed:
      
      return (linkIDA.compareTo(linkIDB));
    }
  }
} 
