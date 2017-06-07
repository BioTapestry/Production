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

package org.systemsbiology.biotapestry.ui.layouts.ortho;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.InvertedLinkProps;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** A way of achieving orthogonality, looking at whole tree
*/

public class TreeStrategy implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<LinkSegStrategy> segStrategies_;
  private ArrayList<StrategyVariation> variations_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public TreeStrategy() {
    segStrategies_ = new ArrayList<LinkSegStrategy>();
    variations_ = new ArrayList<StrategyVariation>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Gotta be able to clone to achieve a pile of strategies
  */
  
  @Override
  public TreeStrategy clone() {
    try {       
      TreeStrategy retval = (TreeStrategy)super.clone();
      
      retval.segStrategies_ = new ArrayList<LinkSegStrategy>();
      int numSS = this.segStrategies_.size();
      for (int i = 0; i < numSS; i++) {        
        LinkSegStrategy strat = this.segStrategies_.get(i);
        retval.segStrategies_.add(strat.clone());
      }
  
      retval.variations_ = new ArrayList<StrategyVariation>();
      int numPl = this.variations_.size();
      for (int i = 0; i < numPl; i++) {        
        StrategyVariation sv = this.variations_.get(i);
        retval.variations_.add(sv.clone());
      }
      
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  } 
  
  /***************************************************************************
  **
  ** Add a segment strategy
  */
  
  public void addSegStrategy(LinkSegStrategy strat) {
    segStrategies_.add(strat);
    return;
  }
  
  /***************************************************************************
  **
  ** Merge two strategies
  */
  
  public void mergeSegStrategies(TreeStrategy other) {
    segStrategies_.addAll(other.segStrategies_);
    return;
  }
   
  /***************************************************************************
  **
  ** Generate the plan for the grid
  */
  
  public int generatePlans(DataAccessContext icx, 
                           LinkProperties lp, FixOrthoOptions.FixOrthoTreeInfo foti) {

        
    //
    // One guy can have variations.  We do not do multi-variation 
    // combinations! Note we need to run the strategy to find the count,
    // but only if we find we need to:
    //
    
    boolean needCount = false;
    Iterator<LinkSegStrategy> tsit = segStrategies_.iterator();
    while (tsit.hasNext()) {
      LinkSegStrategy str = tsit.next();
      if (str.haveVariations()) {
        needCount = true;
        break;
      }
    }
  
    int count = 1;
    if (needCount) {
      LinkProperties countingCopy = lp.clone();
      HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg> countingSplitMap = new HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg>();
      StrategyVariation countingSv = new StrategyVariation();
      tsit = segStrategies_.iterator();
      while (tsit.hasNext()) {
        LinkSegStrategy str = tsit.next();
        LinkSegmentID lsid = str.getSegID();
        countingSv.segIDs.add(lsid);
        //
        // Original segments may be fragmented by splits, so we need to track that:
        //
        FixOrthoPlan.SplitSeg split = countingSplitMap.get(lsid);
        if (split == null) {
          split = new FixOrthoPlan.SplitSeg(lsid);
          countingSplitMap.put(lsid, split);       
        }
        LinkSegmentID useForGeom = (split.end == null) ? split.start : split.end;
        LinkSegment planSeg = countingCopy.getSegmentGeometryForID(useForGeom, icx, false);

        if (str.haveVariations()) {
          int nextCount = str.getVarNum(planSeg.getStart(), planSeg.getEnd());
          if (count > 1) {
            throw new IllegalStateException();
          }
          count = nextCount;
        }
        
        FixOrthoPlan fop = str.getPlan(planSeg.getStart(), planSeg.getEnd(), 0, 2);
        countingSv.plans.add(fop);
        countingSv.splits.add(split.clone());
        countingSv.applyFOP(fop, lsid, split, countingCopy, foti);
      }
    }
 
    for (int i = 0; i < count; i++) {
      LinkProperties workingCopy = lp.clone();
      HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg> splitMap = new HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg>();
      StrategyVariation sv = new StrategyVariation();
      tsit = segStrategies_.iterator();
      while (tsit.hasNext()) {
        LinkSegStrategy str = tsit.next();
        LinkSegmentID lsid = str.getSegID();
        sv.segIDs.add(lsid);
        //
        // Original segments may be fragmented by splits, so we need to track that:
        //
        FixOrthoPlan.SplitSeg split = splitMap.get(lsid);
        if (split == null) {
          split = new FixOrthoPlan.SplitSeg(lsid);
          splitMap.put(lsid, split);       
        }
        LinkSegmentID useForGeom = (split.end == null) ? split.start : split.end;
        LinkSegment planSeg = workingCopy.getSegmentGeometryForID(useForGeom, icx, false);

        FixOrthoPlan fop = str.getPlan(planSeg.getStart(), planSeg.getEnd(), i, count - 1);
        sv.plans.add(fop);
        sv.splits.add(split.clone());
        sv.applyFOP(fop, lsid, split, workingCopy, foti);
      }
      variations_.add(sv);
    }
    return (count);
  }
  
  /***************************************************************************
  **
  ** Answer if this plan can be applied to the grid
  */
  
  public boolean canApply(int varNum, LinkPlacementGrid grid, 
                          DataAccessContext icx, LinkProperties lp, 
                          FixOrthoOptions.FixOrthoTreeInfo foti, String overID) {  
 
    //
    // Collect up the segments that fail even before we begin:
    //

    Set<Line2D> skipThese = crappySegments(grid,icx, lp, null, false, overID); 

    LinkProperties workingCopy = lp.clone();
    StrategyVariation sv = variations_.get(varNum);
    
    sv.applyPlanGuts(workingCopy, foti);
    Set<Line2D> killerSeg = crappySegments(grid, icx, workingCopy, skipThese, true, overID);
    return (killerSeg.isEmpty());
  }
  
  
  /***************************************************************************
  **
  ** Get failed segments
  */
  
  private Set<Line2D> crappySegments(LinkPlacementGrid grid, DataAccessContext icx, 
                                     LinkProperties lp, Set<Line2D> skipSegs,
                                     boolean returnOnFirst, String overID) {
   
    Genome genome = icx.getCurrentGenome();
    //
    // During the BIG NETWORK test, this call would take 12-13 seconds. Using the InvertedLinkProps, 
    // and using the mapped set, it is now down to ~200ms. Need to try to improve this
    //

    HashMap<Point2D, Set<Line2D>> mapOLines = null;
    if (skipSegs != null) {
      mapOLines = new HashMap<Point2D, Set<Line2D>>();
      Iterator<Line2D> ssit = skipSegs.iterator();
      while (ssit.hasNext()) {
        UiUtil.addLineToMappedSet(ssit.next(), mapOLines);
      }      
    }
 
    HashSet<Line2D> retval = new HashSet<Line2D>();
    String src = lp.getSourceTag();
    InvertedLinkProps ilp = new InvertedLinkProps(lp, icx, true);
    Map<LinkSegmentID, LinkSegment> segGeoms = ilp.getGeometryMap();
    HashSet<LinkSegmentID> justOne = new HashSet<LinkSegmentID>();
    Iterator<LinkSegmentID> sgkit = segGeoms.keySet().iterator();  
    while (sgkit.hasNext()) {
      LinkSegmentID segID = sgkit.next();
      LinkSegment geom = segGeoms.get(segID);
      Vector2D run = geom.getRun();
      // This is not a test of zero or crooked segments.  Skip those if they exist.
      if ((run == null) || run.isZero() || !run.isCanonical()) {  // Zero or not straight?  That's OK!
        continue;
      } 
      boolean isStart = segID.isForStartDrop() || segID.isDirect();
      boolean isEnd = segID.isForEndDrop() || segID.isDirect();
      justOne.clear();
      justOne.add(segID);
      Set<String> throughLinks = lp.resolveLinkagesThroughSegments(justOne, ilp);
      String trg = null;      
      HashSet<String> okGroups = null;
      // Could have a move that shifts first (real) segment endpoint back through the launch
      // pad.  We allow lots of launch sloppiness in the grid test, so catch it here:
      
      if (isStart) {
        String aLink = throughLinks.iterator().next();
        if ((overID == null) && crappyStart(genome, icx.getCurrentLayout(), src, geom, mapOLines, aLink, retval)) {
          continue;
        }
      }
      
      if (throughLinks.size() == 1) {
        String trgLinkID = throughLinks.iterator().next();
        if ((overID == null) && (genome instanceof GenomeInstance)) {
          okGroups = new HashSet<String>();
          GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(trgLinkID);
          okGroups.add(tup.getSourceGroup());
          okGroups.add(tup.getTargetGroup());
        }
        if (overID == null) {
          Linkage trgLink = genome.getLinkage(trgLinkID);
          trg = trgLink.getTarget();
          LinkSegment modGeom = crappyTarg(genome, icx.getCurrentLayout(), geom, trgLinkID, segGeoms, segID);
          if (modGeom == null) {
            continue;
          } else {
            geom = modGeom;
          }
        }
        // Group IDs are used as a cheat to represent module blocks in the overlay case.  To prevent
        // modules from allowing passthroughs, this set must be non-null but empty!
        if (overID != null) {
          okGroups = new HashSet<String>();
        }
      }
      
      Line2D equivLine = new Line2D.Double((Point2D)geom.getStart().clone(), (Point2D)geom.getEnd().clone());
      if (mapOLines != null) {
        // There are some indications on the web that Line2D does not override equals! (bug 5057070?)
        // Anyway, set contains sure doesn't work!
        if (UiUtil.lineInMappedSet(equivLine, mapOLines)) {
          continue;
        }
      }
        
      if (!grid.orthoPlanAllowed(icx.getGenomeSource(), src, geom, trg, okGroups, isStart, isEnd)) { 
        retval.add(equivLine);
        if (returnOnFirst) {
          return (retval);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Deal with crappy start nodes
  */
  
  private boolean crappyStart(Genome genome, Layout layout, String src, 
                              LinkSegment geom, Map<Point2D, Set<Line2D>> mapOLines, String aLink, Set<Line2D> crappySegs) {
    Node srcNode = genome.getNode(src);
    NodeProperties np = layout.getNodeProperties(src);    
    INodeRenderer render = np.getRenderer();    
    Linkage link = genome.getLinkage(aLink);
    Vector2D launchDir = render.getDepartureDirection(link.getLaunchPad(), srcNode, layout);
    Vector2D run = geom.getRun(); 
    if (!run.equals(launchDir)) {
      Line2D equivLine = new Line2D.Double((Point2D)geom.getStart().clone(), (Point2D)geom.getEnd().clone());
      if (mapOLines != null) {
        // There are some indications on the web that Line2D does not override equals! (bug 5057070?)
        // Anyway, set contains sure doesn't work!
        if (!UiUtil.lineInMappedSet(equivLine, mapOLines)) {
          crappySegs.add(equivLine);
          return (true);
        }
      } else {
        crappySegs.add(equivLine);
        return (true);
      }
    }
    return (false);  
  } 
 
  /***************************************************************************
  **
  ** Deal with crappy targ nodes; sends back modified geom!
  */
  
  private LinkSegment crappyTarg(Genome genome, Layout layout,
                                 LinkSegment geom, String trgLinkID, Map<LinkSegmentID, LinkSegment> segGeoms, 
                                 LinkSegmentID segID) {
  
    //
    // Tweak inbound final segments to shorten slightly:
    //
    Linkage link = genome.getLinkage(trgLinkID);
    String trgID = link.getTarget();
    Node trgNode = genome.getNode(trgID);
    NodeProperties np = layout.getNodeProperties(trgID);    
    INodeRenderer render = np.getRenderer();
    Vector2D terminalDir = render.getArrivalDirection(link.getLandingPad(), trgNode, layout);
    LinkSegmentID termID = (segID.isDirect()) ? LinkSegmentID.buildIDForDirect(trgLinkID) : LinkSegmentID.buildIDForEndDrop(trgLinkID);
    LinkSegment termGeom = segGeoms.get(termID);
    Point2D terminalPt = termGeom.getEnd();
    double length = termGeom.getLength();
    double offGrid = length - UiUtil.forceToGridValueMin(length, UiUtil.GRID_SIZE);
    // If dealing with a too-tiny end drop, we just skip it:
    if (segID.equals(termID) && (length < (offGrid + UiUtil.GRID_SIZE))) {
      return (null);
    }
    Point2D lastGrid = terminalDir.scaled(-offGrid).add(terminalPt);
    Point2D lastSafe = terminalDir.scaled(-UiUtil.GRID_SIZE).add(lastGrid);
    UiUtil.forceToGrid(lastSafe, UiUtil.GRID_SIZE);

    LinkSegment retGeom = geom.clone();
    if (geom.getRun().equals(terminalDir) && (geom.intersects(lastSafe, 1.0E-6) != null)) {
      retGeom.setEnd(lastSafe);
    }

    return (retGeom);
  } 

  /***************************************************************************
  **
  ** Debug
  */
  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("TreeStrategy:\n");
    int numVar = variations_.size();
    for (int i = 0; i < numVar; i++) {
      StrategyVariation sv = variations_.get(i);
      buf.append("*************************");
      buf.append(sv.toString());
      buf.append("\n");
    }
    if (numVar == 0) {
      Iterator<LinkSegStrategy> tsit = segStrategies_.iterator();
      while (tsit.hasNext()) {
        LinkSegStrategy str = tsit.next();
        buf.append("------------");
        buf.append(str.toString());
        buf.append("\n");
      }
    }
    return (buf.toString());
  } 

  /***************************************************************************
  **
  ** Get a goodness ranking for the plan
  */
  
  public PlanRanking getRanking(int varNum, boolean minCorners, LinkProperties lp,
                                DataAccessContext icx, FixOrthoOptions.FixOrthoTreeInfo foti) {
    
    // Generate modified tree
    LinkProperties lpc = lp.clone();
    StrategyVariation sv = variations_.get(varNum);
    List<FixOrthoPlan.SplitSeg> splitList = sv.applyPlanGuts(lpc, foti);

    //
    // Create map of original segIDs to splits:
    //
    
    HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg> segToSplit = new HashMap<LinkSegmentID, FixOrthoPlan.SplitSeg>();
    int numSeg = sv.segIDs.size();
    for (int i = 0; i < numSeg; i++) {
      LinkSegmentID lsid = sv.segIDs.get(i);
      FixOrthoPlan.SplitSeg ss = splitList.get(i);
      segToSplit.put(lsid, ss);
    }
    
    //
    // Find number of splits:
    //
    int splitCount = 0;
    int numSP = splitList.size();
    for (int i = 0; i < numSP; i++) {        
      FixOrthoPlan.SplitSeg sps = splitList.get(i);
      splitCount += sps.splitCount();
    }
    
    //
    // Generate the geometries for both the original and modified trees:
    //
    
    Map<LinkSegmentID, LinkSegment> origGeoms = lp.getAllSegmentGeometries(icx, false);
    Map<LinkSegmentID, LinkSegment> modGeoms = lpc.getAllSegmentGeometries(icx, false);  
    
    //
    // Find non-ortho swept area of the modified tree:
    //
       
    double nonOrthoArea = LinkProperties.getNonOrthogonalArea(modGeoms);
        
    double distanceDiff = 0.0;
    Iterator<LinkSegmentID> stskit = segToSplit.keySet().iterator();
    while (stskit.hasNext()) {
      LinkSegmentID lsid = stskit.next();
      FixOrthoPlan.SplitSeg ss = segToSplit.get(lsid);
      int nextCount = ss.splitCount();
      LinkSegment origSegGeom = origGeoms.get(lsid);
      if (nextCount == 0) {        
        LinkSegment modSegGeom = modGeoms.get(ss.start);
        distanceDiff += origSegGeom.getEndpointDistanceSum(modSegGeom);
      } else if (nextCount == 1) {
        LinkSegment modSegGeomSt = modGeoms.get(ss.start);
        double startDiff = origSegGeom.getEndpointDistanceSum(modSegGeomSt);
        LinkSegment modSegGeomEnd = modGeoms.get(ss.end);
        double endDiff = origSegGeom.getEndpointDistanceSum(modSegGeomEnd);  
        distanceDiff += Math.min(startDiff, endDiff);
      } else {
        LinkSegment modSegGeom = modGeoms.get(ss.middle);
        distanceDiff += origSegGeom.getEndpointDistanceSum(modSegGeom);        
      }
    }
    PlanRanking retval = new PlanRanking(minCorners, splitCount, distanceDiff, nonOrthoArea);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply the plan
  */
  
  public void applyPlan(int varNum, LinkProperties lp, FixOrthoOptions.FixOrthoTreeInfo foti) {
    StrategyVariation sv = variations_.get(varNum);
    sv.applyPlanGuts(lp, foti);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Utility holder:
  */  
      
  public static class StratAndVar  {
    
    public int varNum;
    public TreeStrategy strat;
    
    public StratAndVar(TreeStrategy strat, int varNum) {
      this.strat = strat;
      this.varNum = varNum;
    }  
  }
  
  
  /***************************************************************************
  **
  ** Rankings for plans:
  */  
      
  public static class PlanRanking implements Comparable<PlanRanking> {
    
    public int splitCount;
    public boolean minCorners;
    public double distRank;
    public double nonOrthoArea;

    public PlanRanking(boolean minCorners, int splitCount, double distRank, double nonOrthoArea) {
      this.minCorners = minCorners;
      this.splitCount = splitCount;
      this.distRank = distRank;
      this.nonOrthoArea = nonOrthoArea;
    }
    
    public int hashCode() {
      return ((new Boolean(minCorners)).hashCode() + splitCount + (int)Math.round(distRank) + (int)Math.round(nonOrthoArea));
    }

    public String toString() {
      return ("PlanRanking: splits = " + splitCount + " distRank = " + distRank + " nonOrthoArea = " + nonOrthoArea);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof PlanRanking)) {
        return (false);
      }
      return (this.compareTo((PlanRanking)other) == 0);
    } 

    public int compareTo(PlanRanking other) {
      //
      // Here is a conundrum.  Say we have two slightly crooked segments in
      // a mostly horizontal line.  If we do a single, non-split move, the
      // midpoint is shoved to one end, goes ortho, and the non-orth of that
      // one side is eliminated.  But we still have a crooked line!
      //
      // BUT big time oddball non-split moves can swamp out a simple, easy, 
      // one split deal.  So we do not always want to choose the 0-split case either!
      //
      
      if (this.nonOrthoArea != other.nonOrthoArea) {
        return ((this.nonOrthoArea < other.nonOrthoArea) ? -1 : 1);
      }   
      
      //
      // Moving this to the bottom makes things with more corners:
      //
      
      if (minCorners) {      
        // Maybe better to make a sliding tradeoff between a split and a distance!     
        if (this.splitCount != other.splitCount) {
          return ((this.splitCount < other.splitCount) ? -1 : 1);
        }
      }
      
      if (this.distRank == 0) {
        if (other.distRank != 0) {
          System.err.println("Bogus distance rank " + this);
          return (100);
        }
      }
      if (other.distRank == 0) {
        if (this.distRank != 0) {
          System.err.println("Bogus distance rank " + other);
          return (-100);
        }
      }
    
      if (this.distRank != other.distRank) {
        return ((this.distRank < other.distRank) ? -1 : 1);
      } 
      
      if (!minCorners) {      
        if (this.splitCount != other.splitCount) {
          return ((this.splitCount < other.splitCount) ? -1 : 1);
        }
      }
            
      return (0);
    }
  }
  
  /****************************************************************************
  **
  ** A variation on a strategy
  */

  private static class StrategyVariation implements Cloneable {

    ArrayList<FixOrthoPlan> plans;
    ArrayList<LinkSegmentID> segIDs;
    ArrayList<FixOrthoPlan.SplitSeg> splits;

    public StrategyVariation() {
      plans = new ArrayList<FixOrthoPlan>();
      segIDs = new ArrayList<LinkSegmentID>();
      splits = new ArrayList<FixOrthoPlan.SplitSeg>();
    }

    @Override
    public StrategyVariation clone() {
      try {       
        StrategyVariation retval = (StrategyVariation)super.clone();

        retval.plans = new ArrayList<FixOrthoPlan>();
        int numPl = this.plans.size();
        for (int i = 0; i < numPl; i++) {        
          FixOrthoPlan fop = this.plans.get(i);
          retval.plans.add(fop.clone());
        }

        retval.segIDs = new ArrayList<LinkSegmentID>();
        int numSID = this.segIDs.size();
        for (int i = 0; i < numSID; i++) {        
          LinkSegmentID lsid = this.segIDs.get(i);
          retval.segIDs.add(lsid.clone());
        }

        retval.splits = new ArrayList<FixOrthoPlan.SplitSeg>();
        int numSP = this.splits.size();
        for (int i = 0; i < numSP; i++) {        
          FixOrthoPlan.SplitSeg sps = this.splits.get(i);
          retval.splits.add(sps.clone());
        }

        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }

    /***************************************************************************
    **
    ** Apply the plan
    */

    List<FixOrthoPlan.SplitSeg> applyPlanGuts(LinkProperties lp, FixOrthoOptions.FixOrthoTreeInfo foti) {
      // Splits are modified as we go.  Use a working copy so the plan
      // can be applied more than once!
      ArrayList<FixOrthoPlan.SplitSeg> workingList = new ArrayList<FixOrthoPlan.SplitSeg>();
      int numSP = splits.size();
      for (int i = 0; i < numSP; i++) {        
        FixOrthoPlan.SplitSeg sps = this.splits.get(i);
        workingList.add(sps.clone());
      }
      int numPlan = plans.size();
      for (int i = 0; i < numPlan; i++) {
        FixOrthoPlan fop = plans.get(i);
        LinkSegmentID segID = segIDs.get(i);
        FixOrthoPlan.SplitSeg split = workingList.get(i);
        applyFOP(fop, segID, split, lp, foti);
      }
      return (workingList);
    }

    /***************************************************************************
    **
    ** Apply the plan
    */

    void applyFOP(FixOrthoPlan fop, LinkSegmentID segID, FixOrthoPlan.SplitSeg split,
                  LinkProperties lp, FixOrthoOptions.FixOrthoTreeInfo foti) {
      Iterator<FixOrthoPlan.OrthoCommand> cmdit = fop.getCommands();
      while (cmdit.hasNext()) {
        FixOrthoPlan.OrthoCommand oc = cmdit.next();
        LinkSegment geom = foti.segGeoms.get(segID);
        oc.apply(lp, split, geom);
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Debug string
    */
    
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("StrategyVariation:\n");
      int numCmd = plans.size();
      for (int i = 0; i < numCmd; i++) {
        FixOrthoPlan fop = plans.get(i);
        buf.append("------------");
        buf.append(segIDs.get(i).toString());
        buf.append("\n");
        buf.append(fop.toString());
      }
      return (buf.toString());
    } 
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
}
