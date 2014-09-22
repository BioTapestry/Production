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

package org.systemsbiology.biotapestry.ui;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.freerender.PlacementGridRenderer;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** A class for optimizing link paths
*/

public class LinkOptimizer {
  
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
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int USELESS_CORNERS_   = 0;
  private static final int ORTHO_RUNS_        = 1;  
  private static final int STAGGERED_RUNS_    = 2;
  private static final int SHORTER_PATHS_     = 3;
  private static final int NUM_OPTIMIZATIONS_ = 4;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private double[] difficultyFull_;
  private double[] difficultyPartial_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LinkOptimizer() {
    difficultyFull_ = new double[] {0.1, 0.6, 0.1, 0.2};
    difficultyPartial_ = new double[] {0.25, 0.75, 0.25};
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return available link optimization steps
  */
  
  public int[] getOptimizations(boolean includeShorterPaths) {
    int size = (includeShorterPaths) ?  NUM_OPTIMIZATIONS_ : NUM_OPTIMIZATIONS_ - 1;
    int[] retval = new int[size];
    int count = 0;
    for (int i = 0; i < LinkOptimizer.NUM_OPTIMIZATIONS_; i++) {
      if ((i == SHORTER_PATHS_) && !includeShorterPaths) {
        continue;
      }
      retval[count++] = i;
    }
    return (retval);
  }
 
 /***************************************************************************
  **
  ** Return relative difficulty
  */
  
  public double relativeDifficulty(int optType, boolean includeShorterPaths) {
    double[] use = (includeShorterPaths) ? difficultyFull_ : difficultyPartial_;
    return ((double)use.length * use[optType]);
  }
 
 /***************************************************************************
  **
  ** Optimize links.  No pinned points are supported for shorter paths
  */
  
  public void optimizeLinks(int optType, LinkProperties lp, LinkPlacementGrid grid, 
                            DataAccessContext icx,
                            String overID, Set<Point2D> pinnedPoints,
                            double startFrac, double maxFrac, BTProgressMonitor monitor) 
                            throws AsynchExitRequestException {
    //
    // There is no optimization to be done on direct links:
    //
    if (lp.isDirect()) {
      return;
    }
    switch (optType) {
      case USELESS_CORNERS_:
        eliminateUselessCorners(lp, grid, icx, overID, startFrac, maxFrac, monitor);
        return;
      case ORTHO_RUNS_:
        optimizeOrthoRuns(lp, grid, icx, overID, pinnedPoints, startFrac, maxFrac, monitor);   
        return;            
      case STAGGERED_RUNS_:
        eliminateStaggeredRuns(lp, grid, 4, icx, overID, pinnedPoints, startFrac, maxFrac, monitor);
        return;
      case SHORTER_PATHS_:
        if (!pinnedPoints.isEmpty()) {
          throw new IllegalArgumentException();
        }
        findShorterPaths(lp, grid, icx, overID, startFrac, maxFrac, monitor);
        return;    
      default:
        throw new IllegalArgumentException();  
    }
  }
  
  /***************************************************************************
  **
  ** Look for useless corners that can be eliminated
  */
  
  public boolean eliminateUselessCornersGridless(LinkProperties bp,
                                                 DataAccessContext icx,
                                                 String overID,
                                                 double startFrac, double maxFrac, BTProgressMonitor monitor) 
                                                 throws AsynchExitRequestException {
    
    //
    // If the bus is direct, it is not useless:
    //
                                        
    if (bp.isDirect()) {
      return (false);
    }
    
    InvertedLinkProps ilp = new InvertedLinkProps(bp, icx);
    if (!ilp.isOrthogonal()) {
      return (false);
    }
    double currProg = startFrac;
    double progDelta = (maxFrac - startFrac) / 100.0;
    boolean dropped = false;
     
    while (true) {
      if (bp.getSegmentCount() == 0) {
        if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return (dropped);
      }
      ArrayList<LinkSegmentID> useless = new ArrayList<LinkSegmentID>();
      getUselessCornersRecursive(useless, LinkSegmentID.buildIDForStartDrop(), ilp);
      if (useless.isEmpty()) {
        if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        } 
        return (dropped);
      }
      int numUseless = useless.size();
      for (int i = 0; i < numUseless; i++) {
        LinkSegmentID uselessParentSeg = useless.get(i);
        String lastUseless = ilp.convertStaleIDs(uselessParentSeg);
        LinkSegmentID uselessID = LinkSegmentID.buildIDForSegment(lastUseless);
        uselessID.tagIDWithEndpoint(LinkSegmentID.END);
        Set<LinkSegmentID> kids = ilp.getAllKids(uselessID);
        if (startsUselessCorner(uselessParentSeg, kids, ilp)) {
          // Sigh. The useless corner test returns false if handed a start drop ID;
          // it wants the root segment ID instead! But the removeUselessCorner expects
          // a LinkSegment for the start drop in order to remove the root segment. So
          // we need to do this last minute futzing around:
          LinkSegment uselessParent = bp.getSegment(lastUseless);
          if (uselessParent.isDegenerate()) {
            uselessID = LinkSegmentID.buildIDForStartDrop();
            uselessID.tagIDWithEndpoint(LinkSegmentID.END);
          }
          bp.removeUselessCorner(uselessID, ilp);
        }
      }   
      dropped = true;
      ilp = new InvertedLinkProps(bp, icx); // Segments dropped, so gotta rebuild...
      if (monitor != null) {
        currProg += progDelta;
        if (currProg > maxFrac) {
          currProg = maxFrac;
        }
        boolean keepGoing = monitor.updateProgress((int)(currProg * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
    }
  }  
 
  /***************************************************************************
  **
  ** Look for useless corners that can be eliminated. Though we now have a 
  ** less memory-intensive gridless version, grid-based elimination is still needed
  ** to work in concert with the other link optimization steps.
  */
  
  public void eliminateUselessCorners(LinkProperties bp, LinkPlacementGrid grid, DataAccessContext icx,
                                      String overID,
                                      double startFrac, double maxFrac, BTProgressMonitor monitor) 
                                      throws AsynchExitRequestException {
    
    //
    // If the bus has zero or one degenerate segment, it is not useless:
    //
                                        
    if (bp.isDirect() || (bp.getSegmentCount() == 1)) {
      if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
        throw new AsynchExitRequestException();
      }
      return;
    }
                                     
    PlacementGridRenderer pgRender = (PlacementGridRenderer)bp.getRenderer();                           
    SegmentWithKids rootSK = bp.buildSegmentTree(icx);
    if ((rootSK == null) || !isOrthogonal(rootSK)) {
      if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
        throw new AsynchExitRequestException();
      }
      return;
    }
    String src = bp.getSourceTag();
    double currProg = startFrac;
    double progDelta = (maxFrac - startFrac / 100.0);
    
    while (true) {
      if (bp.getSegmentCount() == 1) {
        if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return;
      }
      SegmentWithKids useless = getUselessCornerRecursive(rootSK, null);
      if (useless == null) {
        if ((monitor != null) && (!monitor.updateProgress((int)(maxFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return;
      }
      LinkSegment uselessSeg = useless.segment;
      LinkSegmentID uselessID;
      if (uselessSeg.isDegenerate()) {
         uselessID = LinkSegmentID.buildIDForStartDrop();
         uselessID.tagIDWithEndpoint(LinkSegmentID.END);
      } else {
         uselessID = LinkSegmentID.buildIDForSegment(uselessSeg.getID());
         uselessID.tagIDWithEndpoint(LinkSegmentID.END);
      }
      icx.getLayout().deleteLinkageCornerForTree(bp, uselessID, null, icx);      
      grid.dropLink(src, icx.getGenome(), overID);
      pgRender.renderToPlacementGrid(bp, grid, null, overID, icx);
      rootSK = bp.buildSegmentTree(icx); // Segments dropped, so gotta rebuild... 
      if (monitor != null) {
        currProg += progDelta;
        if (currProg > maxFrac) {
          currProg = maxFrac;
        }
        boolean keepGoing = monitor.updateProgress((int)(currProg * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
    }
  }

 /***************************************************************************
  **
  ** Eliminate staggered runs
  */
  
  public void eliminateStaggeredRuns(LinkProperties bp, LinkPlacementGrid grid, int staggerBound,
                                     DataAccessContext icx,
                                     String overID, Set<Point2D> pinnedPoints,
                                     double startFrac, double endFrac, BTProgressMonitor monitor) 
                                     throws AsynchExitRequestException {
                                                                    

    PlacementGridRenderer pgRender = (PlacementGridRenderer)bp.getRenderer();
    SegmentWithKids rootSK = bp.buildSegmentTree(icx);
    if ((rootSK == null) || !isOrthogonal(rootSK)) {
      return;
    } 
    Genome genome = icx.getGenome();
    
    String src = bp.getSourceTag();
    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / 300.0;

    HashSet<String> foundTurns = new HashSet<String>();    
    while (true) {
      StaggeredRunResult result = getStaggeredRunRecursive(rootSK, null, foundTurns, staggerBound, pinnedPoints);
      if (result == null) {
        if ((monitor != null) && (!monitor.updateProgress((int)(endFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return;
      }
      foundTurns.add(result.runStart.segment.getID());
      LinkSegmentID lsid = LinkSegmentID.buildIDForSegment(result.runStart.segment.getID());
      Set<String> throughLinks = bp.resolveLinkagesThroughSegment(lsid);
      String trg = null;
      HashSet<String> okGroups = null;
      if (throughLinks.size() == 1) {
        String trgLinkID = throughLinks.iterator().next();
        trg = bp.getLinkTarget(genome, trgLinkID);
        if ((overID == null) && (genome instanceof GenomeInstance)) {
          okGroups = new HashSet<String>();
          GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(trgLinkID);
          okGroups.add(tup.getSourceGroup());
          okGroups.add(tup.getTargetGroup());
        }
      }

      currProg += progInc;
      if (currProg > endFrac) {
        currProg = endFrac;
      }      
      if (monitor != null) {
        boolean keepGoing = monitor.updateProgress((int)(currProg * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }

      if (!rerouteAllowed(result, grid, src, trg, okGroups)) {
        continue;
      }
      
      SegmentWithKids elimSwk = 
        (result.firstDirection == LinkPlacementGrid.RIGHT) ? result.firstJunction.right
                                                           : result.firstJunction.left;
      bp.cleanupStaggeredRun(Math.abs(result.slideAmount * 10.0), rootSK, result.straightRun, 
                             elimSwk);
      grid.dropLink(src, genome, overID);
      pgRender.renderToPlacementGrid(bp, grid, null, overID, icx);
      rootSK = bp.buildSegmentTree(icx); // Segments dropped, so gotta rebuild. 
    }
  }

 /***************************************************************************
  **
  ** Handle the optimization by displacing orthogonal runs to reduce crossings
  ** or link ink.
  */
  
  public void optimizeOrthoRuns(LinkProperties bp, LinkPlacementGrid grid, DataAccessContext icx,
                                String overID, Set<Point2D> pinnedPoints,
                                double startFrac, double endFrac, BTProgressMonitor monitor)
                                throws AsynchExitRequestException {
    
    PlacementGridRenderer pgRender = (PlacementGridRenderer)bp.getRenderer();
    Genome genome = icx.getGenome();
    
    SegmentWithKids rootSK = bp.buildSegmentTree(icx);
    if ((rootSK == null) || !isOrthogonal(rootSK)) {
      if ((monitor != null) && (!monitor.updateProgress((int)(endFrac * 100.0)))) {
        throw new AsynchExitRequestException();
      }
      return;
    }
    
    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / 800.0; 
    
    String src = bp.getSourceTag();
    // Alas, seeing stack overflows in larger neworks, as this method is
    // using recursion.  Use the less-efficient method that is iterative:
    int currCount = grid.getLinkCrossingsForSource(src); //, crossStart);
    LinkPlacementGrid.InkValues currInkVal = grid.getLinkInk(src);
    DispResult currResult = new DispResult(currInkVal, currCount, 
                                           new Vector2D(0.0, 0.0), Double.POSITIVE_INFINITY);
    Map<String, LinkSegment> origSegments = bp.copySegments();
    List<LinkBusDrop> origDrops = bp.copyDrops();
    HashSet<String> foundTurns = new HashSet<String>();
    DispResult newResult = new DispResult();
    while (true) {
      OrthoRunResult result = getOrthogonalRunRecursive(rootSK, null, foundTurns, pinnedPoints);
      if (result == null) {
        if ((monitor != null) && (!monitor.updateProgress((int)(endFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return;
      }
      foundTurns.add(result.runStart.segment.getID());
      LinkSegmentID lsid = LinkSegmentID.buildIDForSegment(result.runStart.segment.getID());
      Set<String> throughLinks = bp.resolveLinkagesThroughSegment(lsid);
      String trg = null;      
      HashSet<String> okGroups = null;
      if (throughLinks.size() == 1) {
        String trgLinkID = throughLinks.iterator().next();
        trg = bp.getLinkTarget(genome, trgLinkID);
        if ((overID == null) && (genome instanceof GenomeInstance)) {
          okGroups = new HashSet<String>();
          GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(trgLinkID);
          okGroups.add(tup.getSourceGroup());
          okGroups.add(tup.getTargetGroup());
        }
      }

      List<SegmentWithKids> rightStraight = (result.rightRun == null) ? null : result.rightRun.straightRun;
      List<SegmentWithKids> leftStraight = (result.leftRun == null) ? null : result.leftRun.straightRun;      
 
      currProg += progInc;
      if (currProg > endFrac) currProg = endFrac;      
      if ((monitor != null) && !monitor.updateProgress((int)(currProg * 100.0))) {
        throw new AsynchExitRequestException();
      }      
      
      //
      // Order the list so we do the least displacement first:
      //
      
      int min = result.combinedBounds.min;
      int max = result.combinedBounds.max;
      ArrayList<Integer> toDo = new ArrayList<Integer>();
      int largest = (max > (-min)) ? max : -min;
      int currMin = -1;
      int currMax = 1;
      for (int i = 0; i <= largest; i++) {
        if (currMin >= min) {
          toDo.add(new Integer(currMin--));
        }
        if (currMax <= max) {
          toDo.add(new Integer(currMax++));
        }
      }
      
      int numDo = toDo.size();
      ArrayList<Vector2D> okDisp = new ArrayList<Vector2D>();
      ArrayList<Rectangle> usedBounds = new ArrayList<Rectangle>();
      for (int i = 0; i < numDo; i++) {            
        int disp = toDo.get(i).intValue();
        Rectangle checkedBounds = new Rectangle();
        Vector2D dispVec = displacementAllowed(result, grid, src, trg, okGroups, disp, checkedBounds);
        if (dispVec != null) {
          okDisp.add(dispVec);
          usedBounds.add(checkedBounds);
        }
      }
      
      //
      // For small subsets of the link tree, we only want to check a subset of the grid for
      // changes.  Profiling shows that getLinkInk() and dropLink() are the two insanely
      // expensive operations if we work using the whole grid to check even small changes.
      //
      
      Rectangle union = null;
      int numUB = usedBounds.size();
      if (numUB > 0) {
        for (int i = 0; i < numUB; i++) {            
          Rectangle used = usedBounds.get(i);
          if (union == null) {
            union = used;
          } else {
            union = union.union(used);
          }
        }
        union = UiUtil.rectFromRect2D(UiUtil.padTheRect(union, 2));
      }
      LinkPlacementGrid subGrid = (union == null) ? grid : grid.subset(union);
      
      //
      // Working on subgrid, we deal with delta results:
      //

      int currCountDelta = subGrid.getLinkCrossingsForSource(src);
      
      DispResult minResult;
      DispResult currResultDelta = null;
      if (union != null) {
        LinkPlacementGrid.InkValues currInkValDelta = subGrid.getLinkInk(src);
        currResultDelta = new DispResult(currInkValDelta, currCountDelta, 
                                         new Vector2D(0.0, 0.0), Double.POSITIVE_INFINITY);
        minResult = new DispResult(currResultDelta);
      } else {
        minResult = new DispResult(currResult);
      }
            
      currProg += progInc;
      if (currProg > endFrac) currProg = endFrac;      
      if ((monitor != null) && !monitor.updateProgress((int)(currProg * 100.0))) {
        throw new AsynchExitRequestException();
      }   
  
      int okDispNum = okDisp.size();
      boolean dropped = false;
      for (int i = 0; i < okDispNum; i++) {
        Vector2D dispVec = okDisp.get(i);
        bp.shiftStraightSegmentRunFromLists(rightStraight, leftStraight, dispVec, rootSK);
        subGrid.dropLink(src, genome, overID);
        dropped = true;
        pgRender.renderToPlacementGrid(bp, subGrid, null, overID, icx);
        int newCount = subGrid.getLinkCrossingsForSource(src);
        
        LinkPlacementGrid.InkValues newInkVal= subGrid.getLinkInk(src);
        double dispLen = dispVec.length(); 
        newResult.setValues(newInkVal, newCount, dispVec, dispLen);
        bp.restoreSegments(origSegments, rootSK);
        bp.restoreDrops(origDrops);
        currProg += progInc;
        if (currProg > endFrac) currProg = endFrac;      
        if ((monitor != null) && !monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }   
        haveReplacement(newResult, minResult, minResult);
      }
 
      DispResult fullMinResult = (currResultDelta != null) ? DispResult.merge(currResult, currResultDelta, minResult) : minResult;
           
      if (haveReplacement(fullMinResult, currResult, currResult)) {
        grid.dropLink(src, genome, overID);
        bp.shiftStraightSegmentRunFromLists(rightStraight, leftStraight, minResult.dispVec, rootSK);
        pgRender.renderToPlacementGrid(bp, grid, null, overID, icx);
        origSegments = bp.copySegments();
        origDrops = bp.copyDrops();
        rootSK = bp.buildSegmentTree(icx);
      } else if (dropped) {
        grid.dropLink(src, genome, overID);
        pgRender.renderToPlacementGrid(bp, grid, null, overID, icx);
      }
      currProg += progInc;
      if (currProg > endFrac) currProg = endFrac;      
      if ((monitor != null) && !monitor.updateProgress((int)(currProg * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
  }  

 /***************************************************************************
  **
  ** Answer if we have a new optimal displacement.  Updates third arg with
  ** new result if true and third arg is not null;
  */
  
  private boolean haveReplacement(DispResult newResult, 
                                  DispResult oldResult, DispResult replaceResult) {
    
    double smallDisp = 30.0;
    
    boolean fewerCrossings = (newResult.crossingCount < oldResult.crossingCount);

    if (fewerCrossings) {
      if (replaceResult != null) replaceResult.copy(newResult);
      return (true);
    }
    
    boolean equalCrossings = (newResult.crossingCount == oldResult.crossingCount);
    boolean lessInk = (newResult.inkVals.ink < oldResult.inkVals.ink);    

    if (equalCrossings && lessInk) {
      if (replaceResult != null) replaceResult.copy(newResult);
      return (true);
    }
       
    boolean sameInk = (newResult.inkVals.ink == oldResult.inkVals.ink);
    int totalNewCorners = newResult.inkVals.simpleCorners + newResult.inkVals.complexCorners;
    int totalOldCorners = oldResult.inkVals.simpleCorners + oldResult.inkVals.complexCorners;
    boolean fewerCorners = (totalNewCorners < totalOldCorners);
    
    if (equalCrossings && sameInk && fewerCorners) {
      if (replaceResult != null) replaceResult.copy(newResult);
      return (true);
    }
    
    boolean smallShift = (newResult.dispLen <= smallDisp);
    if (equalCrossings && smallShift && fewerCorners) {
      if (replaceResult != null) replaceResult.copy(newResult);
      return (true);
    }    

    return (false);
  }
 
 /***************************************************************************
  **
  ** Shorten up paths
  */
  
  public void findShorterPaths(LinkProperties bp, LinkPlacementGrid grid, 
                               DataAccessContext icx,
                               String overID,
                               double startFrac, double endFrac, BTProgressMonitor monitor) 
                               throws AsynchExitRequestException {
                                 
    //
    // Each successful shorten operation changes the tree, so we need to 
    // start again
    //
    
    PlacementGridRenderer pgRender = (PlacementGridRenderer)bp.getRenderer();
    String src = bp.getSourceTag();
    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / 8.0;
    
    //
    // Ideally, we would do this until we had no more sucessful operations.
    // However, to avoid infinite loops due to non-convergence, we'll cap
    // the loops to a fixed limit:
    //
    Genome genome = icx.getGenome();
    
    for (int i = 0; i < 8; i++) {  // escape hatch for non-convergence...
      if (!shortenAPath(bp, grid, icx, overID)) {
        if ((monitor != null) && (!monitor.updateProgress((int)(endFrac * 100.0)))) {
          throw new AsynchExitRequestException();
        }
        return;
      }
      grid.dropLink(src, genome, overID);
      pgRender.renderToPlacementGrid(bp, grid, null, overID, icx);
      currProg += progInc;
      if (currProg > endFrac) {
        currProg = endFrac;
      }
      if (monitor != null) {
        boolean keepGoing = monitor.updateProgress((int)(currProg * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
    }
  }
  
 /***************************************************************************
  **
  ** Look for shorter paths
  */
  
  private boolean shortenAPath(LinkProperties bp, LinkPlacementGrid grid,
                               DataAccessContext icx, String overID) {
                                 
    //
    // Start at targets.  Walk up segments until we get one that has siblings.  Find
    // out if there is some other segment on the tree significantly closer to the start
    // of the final segment than the length of the existing segment.
    //
                                 
    SegmentWithKids rootSK = bp.buildSegmentTree(icx);
    if ((rootSK == null) || !isOrthogonal(rootSK)) {
      return (false);
    }
    Map<String, SegmentWithKids> skMap = buildSKMap(rootSK);
    Map<String, List<String>> progress = buildProgressMap(rootSK);
    
    //
    // Lowest level branches going to drops:
    //
    
    String src = bp.getSourceTag();
    Genome genome = icx.getGenome();
    Iterator<LinkBusDrop> dit = bp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();      
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        List<String> branch = findLowestSegmentWithSiblings(bd, skMap);
        if (branch == null) {
          continue;
        }
        
        String highestSegID = branch.get(branch.size() - 1);
        LinkSegment highestSeg = bp.getSegment(highestSegID);
        String hsParent = highestSeg.getParent();
        if (hsParent != null) {
          List<String> kidList = progress.get(hsParent);
          kidList.remove(highestSegID);
        }
        String trg = bp.getLinkTarget(genome, bd.getTargetRef());
        HashSet<String> okGroups = null;
        if ((overID == null) && (genome instanceof GenomeInstance)) {
          okGroups = new HashSet<String>();
          GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(bd.getTargetRef());
          okGroups.add(tup.getSourceGroup());
          okGroups.add(tup.getTargetGroup());
        }    
        BridgeResult bridge = findClosestBridge(bp, grid, highestSeg, branch, src, trg, okGroups);
        if (bridge != null) {
          if (createClosestBridge(bridge, bp)) {
            return (true);
          }
        }
      }
    }

    //
    // Drops are done.  Now start working on shared paths higher up the tree
    //
    
    while (progress.size() > 0) {
      //
      // Find a segment with all children processed:
      //
      String nextSeg = null;
      Iterator<String> pkit = progress.keySet().iterator();
      while (pkit.hasNext()) {
        String key = pkit.next();
        List<String> kidList = progress.get(key);
        if (kidList.isEmpty()) {
          nextSeg = key;      
        }
      }
      if (nextSeg == null) {
        throw new IllegalStateException();
      }
      progress.remove(nextSeg);  
      List<String> branch = findNextHighestWithSiblings(nextSeg, skMap);
      if (branch == null) {
        continue;
      }
      String highestSegID = branch.get(branch.size() - 1);
      LinkSegment highestSeg = bp.getSegment(highestSegID);
      String hsParent = highestSeg.getParent();
      if (hsParent != null) {
        List<String> kidList = progress.get(hsParent);
        kidList.remove(highestSegID);
      }
      
      SegmentWithKids hsSwk = skMap.get(highestSegID);
      List<String> kids = getRealKids(hsSwk);
      branch.remove(branch.size() - 1);
      branch.addAll(kids);
      // FIX ME!  Null tup value here is LESS RESTRICTIVE than non-null (unlike null trg);
      BridgeResult bridge = findClosestBridge(bp, grid, highestSeg, branch, src, null, null);
      if (bridge != null) {
        if (createClosestBridge(bridge, bp)) {
          return (true);
        }
      }      
    }
    return (false);    
  }
  
 /***************************************************************************
  **
  ** Find the closest bridge.  Segment is branching from the tree.  Check along
  ** that segment to see if there is a closer point to reattach to
  */
  
  private BridgeResult findClosestBridge(LinkProperties bp, LinkPlacementGrid grid, LinkSegment checkSeg,
                                         List<String> ignore, String src, String trg, Set<String> okGroups) {
                                 
    double segLength = checkSeg.getLength();
    // DANGER!  Note that if these points end up not on the grid (through e.g. layout errors) the
    // loop below may never exit (points will not be equal, as they would if we were on a grid)
    Point2D startPt = checkSeg.getEnd();
    Point2D stopPt = checkSeg.getStart();
    if (startPt == null) {
      return (null);
    }
    
    BridgeResult retval = new BridgeResult();
    retval.segment = checkSeg;
    Vector2D checkBase = checkSeg.getRun().scaled(-10.0);
    int checkNum = 0;
    retval.segPoint = checkBase.scaled(checkNum).add(startPt);
    while (!retval.segPoint.equals(stopPt)) {
      retval.ca = bp.findClosestSegment(retval.segPoint, ignore);
      if ((retval.ca != null) && (retval.ca.distance < (segLength - ((double)checkNum * 10.0)))) {
        if ((retval.segPoint.getX() != retval.ca.point.getX()) &&
            (retval.segPoint.getY() != retval.ca.point.getY())) {
          retval.segPoint = checkBase.scaled(checkNum++).add(startPt);
          continue;
        }
        // Protect against zero vectors:
        if (retval.segPoint.equals(retval.ca.point)) {
          retval.segPoint = checkBase.scaled(checkNum++).add(startPt);
          continue;
        }        
        if (grid.routeAllowed(src, trg, retval.segPoint, retval.ca.point, okGroups, false)) {
          retval.splitMe = (checkNum != 0);
          LinkSegment him = bp.getSegment(retval.ca.segID);
          retval.splitHim = !retval.ca.point.equals(him.getStart()) && !retval.ca.point.equals(him.getEnd());
          return (retval);
        }
      }
      retval.segPoint = checkBase.scaled(checkNum++).add(startPt);
    }
    return (null);
  }  
 
 /***************************************************************************
  **
  ** Create the closest bridge
  */
  
  private boolean createClosestBridge(BridgeResult bridge, LinkProperties bp) {

    LinkSegment myBridge = bridge.segment;
    LinkSegment hisBridge = bp.getSegment(bridge.ca.segID);
    if (myBridge.isDegenerate()) { // FIX ME? Generalize??
      return (false);
    }    
       
    if (bridge.splitMe) {
      LinkSegment[] newSegs = myBridge.split(bridge.segPoint);   
      if (newSegs.length != 1) {  // Endpoint intersected when ==1: no split occurred
        bp.replaceSegment(myBridge, newSegs[0], newSegs[1]);
      }
      myBridge = newSegs[0];
    }

    boolean useEnd = true;
    if (bridge.splitHim) {
      LinkSegment[] newSegs = hisBridge.split(bridge.ca.point);
      if (newSegs.length != 1) {  // Endpoint intersected when ==1: no split occurred
        bp.replaceSegment(hisBridge, newSegs[0], newSegs[1]);
      }
      hisBridge = newSegs[0];
    } else if (hisBridge.isDegenerate()) {
      useEnd = false;
    } else {
      useEnd = bridge.ca.point.equals(hisBridge.getEnd());    
    }
    bp.moveSegmentOnTree(myBridge, hisBridge, useEnd);
    return (true);
    
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

 /***************************************************************************
  **
  ** Climb up from leaf until we hit a segment with siblings
  */
  
  private List<String> findLowestSegmentWithSiblings(LinkBusDrop bd, Map<String, SegmentWithKids> skMap) {

    ArrayList<String> retval = new ArrayList<String>();
    String conn = bd.getConnectionTag();
    SegmentWithKids swk = skMap.get(conn);
    int sibCount = swk.kids.size() - 1;
    if (sibCount < 0) {
      throw new IllegalStateException();
    }
    if (sibCount > 0) {
      return (null);
    }
    SegmentWithKids highestSeg = null;
    while (highestSeg == null) {
      LinkSegment seg = swk.segment;
      String parent = seg.getParent();
      if (parent == null) {
        return (null);
      }
      retval.add(swk.segment.getID());
      SegmentWithKids parentSwk = skMap.get(parent);
      sibCount = parentSwk.kids.size() - 1;
      if (sibCount < 0) {
        throw new IllegalStateException();
      }
      if (sibCount > 0) {
        highestSeg = swk;
      } else {
        swk = parentSwk;
      }
    }
    return (retval);
  }
  
 /***************************************************************************
  **
  ** Continue up until we hit the next segment up with siblings
  */
  
  private List<String> findNextHighestWithSiblings(String segID, Map<String, SegmentWithKids> skMap) {

    ArrayList<String> retval = new ArrayList<String>();
    SegmentWithKids swk = skMap.get(segID);
    SegmentWithKids highestSeg = null;
    while (highestSeg == null) {
      LinkSegment seg = swk.segment;
      String parent = seg.getParent();
      if (parent == null) {
        return (null);
      }
      retval.add(swk.segment.getID());
      SegmentWithKids parentSwk = skMap.get(parent);
      int sibCount = parentSwk.kids.size() - 1;
      if (sibCount < 0) {
        throw new IllegalStateException();
      }
      if (sibCount > 0) {
        highestSeg = swk;
      } else {
        swk = parentSwk;
      }
    }
    return (retval);
  }  

 /***************************************************************************
  **
  ** Wrapper for grid reroute check
  */
  
  public boolean rerouteAllowed(StaggeredRunResult result, LinkPlacementGrid grid,
                                String src, String trg, Set<String> okGroups) {
                                           
    LinkSegment startSeg;
    if (result.firstDirection == LinkPlacementGrid.LEFT) {
      startSeg = result.firstJunction.left.segment;
    } else {
      startSeg = result.firstJunction.right.segment;
    }
    Point2D rerouteStart = startSeg.getStart();      

    double offsetLen = startSeg.getLength();
    Vector2D offsetRun = startSeg.getRun();
    Vector2D scaled = offsetRun.scaled(-offsetLen);
    Point2D oldEnd; 
    if (result.thirdJunction.right == null) {
      oldEnd = result.thirdJunction.left.segment.getStart();
    } else {
      oldEnd = result.thirdJunction.right.segment.getStart();
    }
    Point2D newEnd = scaled.add(oldEnd);
      
    ArrayList<Point2D> branchAwayPts = new ArrayList<Point2D>();
    int numBA = result.branchAways.size();
    for (int i = 0; i < numBA; i++) {
      SegmentWithKids currSwk = result.branchAways.get(i);
      branchAwayPts.add(currSwk.segment.getStart());
    }
      
    ArrayList<Point2D> branchBackPts = new ArrayList<Point2D>(); 
    int numBB = result.branchBacks.size();
    for (int i = 0; i < numBB; i++) {
      SegmentWithKids currSwk = result.branchBacks.get(i);
      branchBackPts.add(currSwk.segment.getStart());
    }           
      
                
    return (grid.rerouteAllowed(src, rerouteStart, oldEnd, newEnd, trg, okGroups,
                                branchAwayPts, branchBackPts, false));                                 
  }  

 /***************************************************************************
  **
  ** Wrapper for grid displacement check
  */
  
  private Vector2D displacementAllowed(OrthoRunResult ortho, LinkPlacementGrid grid,
                                       String src, String trg, Set<String> okGroups,
                                       int displacement, Rectangle boundsUsed) {
                                       
    //
    // Figure out the single orthogonal run from the two parts.  No matter what,
    // the run "starts" at the extreme right point and ends at the extreme left:
    //
    
    Point2D start;
    Point2D end;
    if ((ortho.leftRun == null) && (ortho.rightRun == null)) {
      throw new IllegalArgumentException();
    } else if (ortho.leftRun == null) {
      start = ortho.rightRun.terminalPoint;
      end = ortho.rightRun.orthoStart.segment.getStart();
    } else if (ortho.rightRun == null) {
      start = ortho.leftRun.orthoStart.segment.getStart();
      end = ortho.leftRun.terminalPoint;
    } else {
      start = ortho.rightRun.terminalPoint;
      end = ortho.leftRun.terminalPoint;  
    }
    
    //
    // The above gets the undisplaced points.  Now displace them.
    //
    
    Vector2D fullRun = new Vector2D(start, end).normalized();
    Vector2D forward = LinkPlacementGrid.getTurn(fullRun, LinkPlacementGrid.RIGHT);
    forward.scale(displacement * 10.0);
    start = forward.add(start);
    end = forward.add(end);

    //
    // Now we create the lists of branch points
    //
    
    ArrayList<Point2D> shorterStarts = new ArrayList<Point2D>();
    ArrayList<Point2D> longerStarts = new ArrayList<Point2D>();    

    
    if (ortho.rightRun != null) {
      branchTransfer(ortho.rightRun, shorterStarts, longerStarts, displacement);
    }
    
    //
    // Always add in branch points for the entry into the ortho run:
    //
    
    LinkSegment parSeg = ortho.runStart.segment;
    Point2D parPt = (parSeg.isDegenerate()) ? parSeg.getStart() : parSeg.getEnd();    
    if (displacement > 0) {
      if (ortho.firstJunction.forward != null) {
        shorterStarts.add(ortho.firstJunction.forward.segment.getStart());
      }
      longerStarts.add(parPt);
    } else {
      shorterStarts.add(parPt);
      if (ortho.firstJunction.forward != null) {
        longerStarts.add(ortho.firstJunction.forward.segment.getStart());
      }      
    }

    if (ortho.leftRun != null) {
      branchTransfer(ortho.leftRun, shorterStarts, longerStarts, displacement);
    }
    
    //
    // Create the reduced list of lone longer points
    //
    
    ArrayList<Point2D> longerLoneStarts = new ArrayList<Point2D>();      
    int numLs = longerStarts.size();  
    for (int i = 0; i < numLs; i++) {
      Point2D longer = longerStarts.get(i);
      if (!shorterStarts.contains(longer)) {
        longerLoneStarts.add(longer);
      }
    }
    
    //
    // Finally, displace the shorter starts:
    //
    
    ArrayList<Point2D> dispShorterStarts = new ArrayList<Point2D>();
    int numSs = shorterStarts.size();
    for (int i = 0; i < numSs; i++) {
      Point2D shorter = shorterStarts.get(i);
      dispShorterStarts.add(forward.add(shorter));
    }
    
  
    //
    // As an optimization, find out the area that we are checking:
    //
    
    boundsUsed.setBounds(grid.displacementRange(start, end, forward,
                                                dispShorterStarts, longerLoneStarts));
                                
    //
    // We are done:
    //
    
    if (grid.displacementAllowed(src, start, end, forward, trg, okGroups,
                                 dispShorterStarts, longerLoneStarts, false)) {
      return (forward);
    } else {
      return (null);
    }
  }    
  
 /***************************************************************************
  **
  ** Transfer info for side branches
  */
  
  private void branchTransfer(OrthoSubrun ortho, List<Point2D> shorterStarts, 
                              List<Point2D> longerStarts, int displacement) {
   
    List<Point2D> targList = (displacement > 0) ? shorterStarts : longerStarts;
    List<SegmentWithKids> srcList = ortho.branchAways;
    int num = srcList.size();
    for (int i = 0; i < num; i++) {
      SegmentWithKids currSwk = srcList.get(i);
      targList.add(currSwk.segment.getStart());
    }
    targList = (displacement > 0) ? longerStarts : shorterStarts;
    srcList = ortho.branchBacks;
    num = srcList.size();
    for (int i = 0; i < num; i++) {
      SegmentWithKids currSwk = srcList.get(i);
      targList.add(currSwk.segment.getStart());
    }
    return;
  }

  /***************************************************************************
  **
  ** Look for jagged tees that can be eliminated
  */
  
  private int[] calculateSlides(SortedSet<Integer> slides1, SortedSet<Integer> slides2, 
                                int firstDirection, int jaggedOffset) {
    int[] retval = new int[2];
    int sign = (firstDirection == LinkPlacementGrid.LEFT) ? 1 : -1;
    int signedOffset = sign * jaggedOffset;
    Integer signedOffsetObj = new Integer(signedOffset);    
    if (slides1.contains(signedOffsetObj)) {
      retval[0] = signedOffset;
      retval[1] = 0;
      return (retval);
    } else if (slides2.contains(signedOffsetObj)) {
      retval[0] = 0;
      retval[1] = signedOffset;
      return (retval);
    } else {
      Iterator<Integer> s1it = slides1.iterator();
      while (s1it.hasNext()) {
        Integer s1 = s1it.next();
        int s1val = s1.intValue();
        Iterator<Integer> s2it = slides2.iterator();
        while (s2it.hasNext()) {
          Integer s2 = s2it.next();
          int s2val = s2.intValue();
          if (s1val + s2val == signedOffset) {
            retval[0] = s1val;
            retval[1] = s2val;
            return (retval);
          }
        }
      }
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Look for the useless corner pattern
  */
  
  private SegmentWithKids getUselessCornerRecursive(SegmentWithKids skids,
                                                    SegmentWithKids parent) {
    if (startsUselessCorner(skids, parent)) {
      return (skids);
    }
    int kidCount = skids.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = skids.kids.get(i);
      SegmentWithKids recurse = getUselessCornerRecursive(kidSk, skids);
      if (recurse != null) {
        return (recurse);
      }
    } 
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Look for the useless corner pattern
  */
  
  private void getUselessCornersRecursive(List<SegmentWithKids> allUseless,
                                          SegmentWithKids skids,
                                          SegmentWithKids parent) {
    if (startsUselessCorner(skids, parent)) {
      allUseless.add(skids);
    }
    int kidCount = skids.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = skids.kids.get(i);
      getUselessCornersRecursive(allUseless, kidSk, skids);
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Look for the useless corner pattern
  */
  
  private void getUselessCornersRecursive(List<LinkSegmentID> allUseless,
                                          LinkSegmentID lsid,
                                          InvertedLinkProps invlp) {
    
    Set<LinkSegmentID> kids = invlp.getAllKids(lsid);
    if (startsUselessCorner(lsid, kids, invlp)) {
      allUseless.add(lsid);
    }
    Iterator<LinkSegmentID> kit = kids.iterator();
    while (kit.hasNext()) {
      LinkSegmentID kidID = kit.next();
      getUselessCornersRecursive(allUseless, kidID, invlp);
    } 
    return;
  }

  /***************************************************************************
  **
  ** Check if the link tree is fully orthogonal
  */
  
  private boolean isOrthogonal(SegmentWithKids skids) {
    Vector2D run = skids.segment.getRun();
    if (!skids.segment.isDegenerate() && !isOrtho(run)) {
      return (false);
    }
    int kidCount = skids.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = skids.kids.get(i);
      if (!isOrthogonal(kidSk)) {
        return (false);
      }
    } 
    return (true);
  }  

  /***************************************************************************
  **
  ** Look for the staggered run pattern
  */
  
  private StaggeredRunResult getStaggeredRunRecursive(SegmentWithKids skids, 
                                                      SegmentWithKids parent, 
                                                      Set<String> foundTurns, int bound, Set<Point2D> pinnedPoints) {
    String segID = skids.segment.getID();
    if ((segID != null) && !foundTurns.contains(segID)) {
      StaggeredRunResult result = startsStaggeredRun(skids, parent, bound, pinnedPoints);
      if (result != null) {
        return (result);
      }
    }
    int kidCount = skids.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = skids.kids.get(i);
      StaggeredRunResult recurse = getStaggeredRunRecursive(kidSk, skids, foundTurns, bound, pinnedPoints);
      if (recurse != null) {
        return (recurse);
      }
    } 
    return (null);
  }
  
  /***************************************************************************
  **
  ** look for orthogonal run optimizations
  */
  
  private OrthoRunResult getOrthogonalRunRecursive(SegmentWithKids skids, 
                                                   SegmentWithKids parent, Set<String> foundTurns, Set<Point2D> pinnedPoints) {
    String segID = skids.segment.getID();
    if ((segID != null) && !foundTurns.contains(segID)) {
      OrthoRunResult result = startsOrthogonalRun(skids, parent, pinnedPoints);
      if (result != null) {
        return (result);
      }
    }
    int kidCount = skids.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = skids.kids.get(i);
      OrthoRunResult recurse = getOrthogonalRunRecursive(kidSk, skids, foundTurns, pinnedPoints);
      if (recurse != null) {
        return (recurse);
      }
    } 
    return (null);
  }  

  /***************************************************************************
  **
  ** Build a map to swks from link segment ID
  */
  
  private Map<String, SegmentWithKids> buildSKMap(SegmentWithKids swk) {
    HashMap<String, SegmentWithKids> retval = new HashMap<String, SegmentWithKids>();
    buildSKMapRecursive(swk, retval);
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Build a map to swks from link segment ID
  */
  
  private void buildSKMapRecursive(SegmentWithKids swk, Map<String, SegmentWithKids> skMap) {
    String id = swk.segment.getID();
    if (id != null) {
      skMap.put(id, swk);
    }
    int kidCount = swk.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = swk.kids.get(i);
      buildSKMapRecursive(kidSk, skMap);
    } 
    return;
  }
  
  
  /***************************************************************************
  **
  ** Get all real (non-drop) kids
  */
  
  private List<String> getRealKids(SegmentWithKids swk) {
    ArrayList<String> retval = new ArrayList<String>();
    getRealKidsRecursive(swk, retval);
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Build a map to swks from link segment ID
  */
  
  private void getRealKidsRecursive(SegmentWithKids swk, List<String> realKids) {
    String id = swk.segment.getID();
    if (id != null) {
      realKids.add(id);
    }
    int kidCount = swk.kids.size();
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = swk.kids.get(i);
      getRealKidsRecursive(kidSk, realKids);
    } 
    return;
  }  
  
  /***************************************************************************
  **
  ** Build a map for tracking progress
  */
  
  private Map<String, List<String>> buildProgressMap(SegmentWithKids swk) {
    HashMap<String, List<String>> retval = new HashMap<String, List<String>>();
    buildProgressMapRecursive(swk, retval);
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Build a map for tracking progress
  */
  
  private void buildProgressMapRecursive(SegmentWithKids swk, Map<String, List<String>> progMap) {
    String id = swk.segment.getID();
    int kidCount = swk.kids.size();
    ArrayList<String> kids = null;
    if (kidCount > 1) {
      if (id != null) {
        kids = new ArrayList<String>();
        progMap.put(id, kids); 
      }
    }
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSk = swk.kids.get(i);
      String kidID = kidSk.segment.getID();
      if ((kids != null) && (kidID != null)) {
        kids.add(kidID);
      }
      buildProgressMapRecursive(kidSk, progMap);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if the corner is useless (no direction change, only one child). 
  */
  
  private boolean startsUselessCorner(SegmentWithKids segWkids, SegmentWithKids parent) {

    //
    // More than one kid means it cannot be useless
    //

    if (segWkids.kids.size() != 1) {
      return (false);
    }
    
    //
    // Change in direction means it cannot be useless
    //
        
    SegmentDirections dir = getDirections(segWkids, parent);
    if ((dir == null) || (dir.forward == null)) {
      return (false);
    }
    
    return (true);
  }
    
  /***************************************************************************
  **
  ** Answer if the corner is useless (no direction change, only one child). 
  */
  
  private boolean startsUselessCorner(LinkSegmentID lsid, Set<LinkSegmentID> kids, InvertedLinkProps ilp) {

    //
    // More than one kid means it cannot be useless
    //
    
    if (kids.size() != 1) {
      return (false);
    }
    
    //
    // Change in direction means it cannot be useless
    //
        
    LSIDSegmentDirections dir = getDirections(lsid, kids, ilp);
    if ((dir == null) || (dir.forward == null)) {
      return (false);
    }
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if the segment is the parent of a jagged tee.  Null if not, else
  ** we get back data for processing.
  */
  
  private StaggeredRunResult startsStaggeredRun(SegmentWithKids skids, 
                                                SegmentWithKids parent, int bound, Set<Point2D> pinnedPoints) {
    //
    // Go through a number of forward segments, followed by a short jag, followed by
    // another forward segment.Hit a (left,right) corner while another segment continues straight
    // The straight segment must be short (1 - 2 grid blocks)
    // The next sideways branch goes in the opposite direction
    // One or both of the sideways branches must be free enought to move to eliminate
    // the straight segment.

    StaggeredRunResult retval = new StaggeredRunResult();
    retval.runStart = skids;

    retval.firstJunction = getDirections(skids, parent);
    if (retval.firstJunction == null) {
      return (null);
    }
    
    //
    // See if we have a single turning branch.  For now, if we have a tee, just choose
    // the left turn first.
    //
    
    int nextTurnDir;
    SegmentWithKids firstSegment; 
    if (retval.firstJunction.forward == null) {
      if (retval.firstJunction.left != null) {
        firstSegment = retval.firstJunction.left;
        nextTurnDir = LinkPlacementGrid.RIGHT;
        retval.firstDirection = LinkPlacementGrid.LEFT;
      } else if (retval.firstJunction.right != null) {
        firstSegment = retval.firstJunction.right;
        nextTurnDir = LinkPlacementGrid.LEFT;
        retval.firstDirection = LinkPlacementGrid.RIGHT;
      } else {
        return (null);
      }
    } else {
      return (null);
    }
    
    //
    // Get length of turn seg:
    //
    
    LinkSegment turnSeg = firstSegment.segment;
    double turnLength = turnSeg.getLength();
    int staggeredOffset = (int)(turnLength / 10.0);
    if (staggeredOffset > bound) {
      return (null);
    }
    
    //
    // Make sure next turn goes back to the original direction:
    //
    
    SegmentWithKids secondSegment;     
    retval.secondJunction = getDirections(firstSegment, null);
    if (retval.secondJunction == null) {
      return (null);
    }
    if (nextTurnDir == LinkPlacementGrid.LEFT) {
      if ((retval.secondJunction.left == null) || (retval.secondJunction.right != null)) {
        return (null);
      } else {
        secondSegment = retval.secondJunction.left;
      }
        
    } else if (nextTurnDir == LinkPlacementGrid.RIGHT) {
      if ((retval.secondJunction.left != null) || (retval.secondJunction.right == null)) {
        return (null);
      } else {
        secondSegment = retval.secondJunction.right;
      }
    } else {
      throw new IllegalStateException();
    }
        
    //
    // Make sure we have enough freedom of movement to eliminate the offset:
    //

    MinMax moves = freeLateralMovement(secondSegment, pinnedPoints);
    if (moves == null) {
      return (null);
    }
    
    if (retval.firstDirection == LinkPlacementGrid.RIGHT) {
      if ((moves.min == Integer.MAX_VALUE) || (moves.min <= -staggeredOffset)) {
        retval.slideAmount = -staggeredOffset;
      } else {
        return (null);
      }
    } else if (retval.firstDirection == LinkPlacementGrid.LEFT) {
      if ((moves.max == Integer.MIN_VALUE) || (moves.max > staggeredOffset)) {
        retval.slideAmount = staggeredOffset;        
      } else {
        return (null);
      }       
    } else {
      throw new IllegalStateException();
    }
    
    //
    // Fill in the third junction info and branch info:
    //
    
    retval.straightRun = getStraightRun(secondSegment);
    if (retval.straightRun == null) {
      return (null);
    }
    int numStr = retval.straightRun.size();
    int last = numStr - 1;
    retval.branchAways = new ArrayList<SegmentWithKids>();    
    retval.branchBacks = new ArrayList<SegmentWithKids>();        
    for (int i = 0; i < numStr; i++) {
      SegmentWithKids currSwk = retval.straightRun.get(i);
      SegmentDirections directions = getDirections(currSwk, null);
      if (directions == null) {
        return (null);
      }
      if (retval.firstDirection == LinkPlacementGrid.RIGHT) {
        if (directions.right != null) {
          retval.branchAways.add(directions.right);
        }
        if (directions.left != null) {
          retval.branchBacks.add(directions.left);
        }        
      } else if (retval.firstDirection == LinkPlacementGrid.LEFT) {
        if (directions.right != null) { 
          retval.branchBacks.add(directions.right);
        }
        if (directions.left != null) {
          retval.branchAways.add(directions.left);
        }        
      }
      if (i == last) {
        retval.thirdJunction = directions;
      }
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if the segment is the parent of an orthogonal run.  Null if not, else
  ** we get back data for processing.
  */
  
  private OrthoRunResult startsOrthogonalRun(SegmentWithKids skids, SegmentWithKids parent, Set<Point2D> pinnedPoints) {

    OrthoRunResult retval = new OrthoRunResult();
    retval.runStart = skids;

    //
    // Get the directions:
    //
    
    retval.firstJunction = getDirections(skids, parent);
    if (retval.firstJunction == null) {
      return (null);
    }
    
    //
    // If there is no left or right turn, we don't have a match:
    //
    
    if ((retval.firstJunction.left == null) && (retval.firstJunction.right == null)) {
      return (null);
    }

    //
    // Get the left and right straight runs
    //
    
    if (retval.firstJunction.left != null) {
      retval.leftRun = new OrthoSubrun();
      retval.leftRun.orthoStart = retval.firstJunction.left;
      retval.leftRun.subrunDirection = LinkPlacementGrid.LEFT;
      retval.leftRun.straightRun = getStraightRun(retval.leftRun.orthoStart);
      if (retval.leftRun.straightRun == null) {
        return (null);
      }
      analyzeOrthoSubrun(retval.leftRun, pinnedPoints);
    }
    
    if (retval.firstJunction.right != null) {
      retval.rightRun = new OrthoSubrun();
      retval.rightRun.orthoStart = retval.firstJunction.right;
      retval.rightRun.subrunDirection = LinkPlacementGrid.RIGHT;
      retval.rightRun.straightRun = getStraightRun(retval.rightRun.orthoStart);
      if (retval.rightRun.straightRun == null) {
        return (null);
      }
      analyzeOrthoSubrun(retval.rightRun, pinnedPoints);   
    }
    
    //
    // Null bounds from a non-null branch means we are pinned:
    //
    
    if (((retval.leftRun != null) && (retval.leftRun.bounds == null)) ||
        ((retval.rightRun != null) && (retval.rightRun.bounds == null))) {
      return (null);
    }

    //
    // Merge the travel bounds of the two branches
    //

    int maximumUnboundTravel = 20;
    SegmentWithKids forwardRun = retval.firstJunction.forward;
    int forwardBound = Integer.MAX_VALUE;
    if (forwardRun != null) {
      LinkSegment forSeg = forwardRun.segment;
      int pad = (forSeg.getID() == null) ? 1 : 0; // don't want drops to disappear
      forwardBound = (int)(forSeg.getLength() / 10.0) - pad;
    }
    if (forwardBound < 0) forwardBound = 0;
    LinkSegment parSeg = skids.segment;
    int pad = 0;
    if (parSeg.isDegenerate()) {
      parSeg = parent.segment; // a fake segment for the root drop
      pad = 1;  // don't want root drop to disappear
    }
    int backBound = -((int)(parSeg.getLength() / 10.0) - pad);
    if (backBound > 0) backBound = 0;
    retval.combinedBounds =  
      combineOrthoBounds((retval.leftRun == null) ? null : retval.leftRun.bounds,
                         (retval.rightRun == null) ? null : retval.rightRun.bounds,
                         maximumUnboundTravel, forwardBound, backBound);
    
    //
    // If we have no travel options, we are through:
    //
                         
    if (retval.combinedBounds == null) {
      return (null);
    }
                         
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Analyze the details of an orthogonal branch.  Returns false if we run
  ** into problems
  */
  
  private boolean analyzeOrthoSubrun(OrthoSubrun subrun, Set<Point2D> pinnedPoints) {    
    int numStr = subrun.straightRun.size();
    int last = numStr - 1;
    subrun.branchAways = new ArrayList<SegmentWithKids>();    
    subrun.branchBacks = new ArrayList<SegmentWithKids>();        
    for (int i = 0; i < numStr; i++) {
      SegmentWithKids currSwk = subrun.straightRun.get(i);
      SegmentDirections directions = getDirections(currSwk, null);
      if (directions == null) {
        return (false);
      }
      if (subrun.subrunDirection == LinkPlacementGrid.RIGHT) {
        if (directions.right != null) {
          subrun.branchBacks.add(directions.right);
        }
        if (directions.left != null) {
          subrun.branchAways.add(directions.left);
        }        
      } else if (subrun.subrunDirection == LinkPlacementGrid.LEFT) {
        if (directions.right != null) { 
          subrun.branchAways.add(directions.right);
        }
        if (directions.left != null) {
          subrun.branchBacks.add(directions.left);
        }        
      }
      if (i == last) {
        subrun.terminalJunction = directions;
        subrun.terminalPoint = (Point2D)currSwk.segment.getEnd().clone();
      }
    }
    subrun.bounds = freeLateralMovement(subrun.orthoStart, pinnedPoints);
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Combine left and right travel bounds into a single value.  We also modify
  ** the original inputs to impose the unbounded limit
  */
  
  private MinMax combineOrthoBounds(MinMax left, MinMax right, int unboundedLimit,
                                    int forwardBound, int backwardBound) {
    
    if ((left == null) && (right == null)) {
      return (null);
    }
    
    if (left != null) {
      // Don't set to unbounded limit unless we really have to:
      if ((left.max == Integer.MAX_VALUE) && (forwardBound == Integer.MAX_VALUE)) {
        left.max = unboundedLimit;
      }
      if ((left.min == Integer.MIN_VALUE) && (backwardBound == Integer.MIN_VALUE)) {
        left.min = -unboundedLimit;
      }
    }
    
    if (right != null) {
      if ((right.max == Integer.MAX_VALUE) && (backwardBound == Integer.MIN_VALUE)) {
        right.max = unboundedLimit;
      } 
      // Don't set to unbounded limit unless we really have to:      
      if ((right.min == Integer.MIN_VALUE) && (forwardBound == Integer.MAX_VALUE)) {
        right.min = -unboundedLimit;
      } 
    }    
    
    //
    // Negative means going backward, i.e. we use the sign convention
    // of the left orthogonal branch:
    //

    if (right == null) {
      MinMax retval = new MinMax(left);
      if (retval.min < backwardBound) {
        retval.min = backwardBound;
      }
      if (retval.max > forwardBound) {
        retval.max = forwardBound;
      }      
      return (retval);
    }

    MinMax retval = (right.min == Integer.MIN_VALUE) ? 
      new MinMax(-right.max, Integer.MAX_VALUE) : 
      new MinMax(-right.max, -right.min);
      
    if (retval.min < backwardBound) {
      retval.min = backwardBound;
    }
    if (retval.max > forwardBound) {
      retval.max = forwardBound;
    }     
    
    if (left == null) {
      return (retval);
    }

    if (left.max < retval.max) {
      retval.max = left.max;
    }
    
    if (left.min > retval.min) {
      retval.min = left.min;
    }
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Answer if the given segment starts a run of segments that is free to move 
  ** laterally.  If not, returns null.  Otherwise, returns a MinMax that indicates
  ** the movement restriction based on orthogonally branching segment lengths
  */
  
  private MinMax freeLateralMovementWorldCoord(SegmentWithKids skids, Set<Point2D> pinnedPoints) {

    //
    // No kids means we are pegged:
    //
    
    if (skids.kids.size() == 0) {
      return (null);
    }
    
    //
    // If one of our endpoints is in the pinned set, we are not moving anywhere:
    //
    
    LinkSegment mySeg = skids.segment;
    if (pinnedPoints != null) {
      if (pinnedPoints.contains(mySeg.getStart())) {
        return (null);
      }
      if (!mySeg.isDegenerate() && pinnedPoints.contains(mySeg.getEnd())) {
        return (null);
      }
    }
       
    //
    // Go through the kids.  If ortho, record the lengths.  If straight, call this
    // on it.
    //
      
    Vector2D myRun = mySeg.getRun();  
    MinMax retval = new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
    int kidCount = skids.kids.size();
    
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSwk = skids.kids.get(i);
      LinkSegment lseg = kidSwk.segment;
      Vector2D kidRun = lseg.getRun();
      if ((kidRun == null) || kidRun.isZero() || (!isOrtho(kidRun))) {
        return (null);  // FIX ME??? Relax this...
      }
      
      int kidLen = (int)lseg.getLength();
      if (kidSwk.segment.getID() == null) {  // drops cannot disappear...
        if (kidLen >= 10) {
          kidLen -= 10;
        }
      }
      if (kidRun.equals(myRun)) {
        MinMax kidmm = freeLateralMovementWorldCoord(kidSwk, pinnedPoints);
        if (kidmm == null) {
          return (null);
        }
        if (kidmm.max < retval.max) {
          retval.max = kidmm.max;
        }
        if (kidmm.min > retval.min) {
          retval.min = kidmm.min;
        }
      } else if (kidRun.equals(LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.LEFT))) {
        if (retval.min < -kidLen) {
          retval.min = -kidLen;
        }
      } else if (kidRun.equals(LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.RIGHT))) {
        if (retval.max > kidLen) {
          retval.max = kidLen;
        }
      } else { // reverse direction -> give up
        return (null);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if the given segment starts a run of segments that is free to move 
  ** laterally.  If not, returns null.  Otherwise, returns a MinMax that indicates
  ** the movement restriction based on orthogonally branching segment lengths
  */
  
  private MinMax freeLateralMovement(SegmentWithKids skids, Set<Point2D> pinnedPoints) {
    
    MinMax retval = freeLateralMovementWorldCoord(skids, pinnedPoints);
    if (retval == null) {
      return (null);
    }

    if (retval.min != Integer.MIN_VALUE) {
      retval.min /= 10;
    }
    if (retval.max != Integer.MAX_VALUE) {
      retval.max /= 10;
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Returns the segments heading out in different directions.  If we have
  ** weirdness (two in one direction, a backwards segment, a zero segment)
  ** it returns null
  */
  
  private SegmentDirections getDirections(SegmentWithKids skids, SegmentWithKids parent) {
    SegmentDirections retval = new SegmentDirections();
    
    //
    // More than three kids? Weirdness! So null...
    int kidCount = skids.kids.size();
    if (kidCount > 3) {
      return (null);
    }
    
    for (int i = 0; i < kidCount; i++) {
      SegmentWithKids kidSwk = skids.kids.get(i);
      
      // Get kid run, outta here if weird:
      LinkSegment lseg = kidSwk.segment;
      Vector2D kidRun = lseg.getRun();    
      if ((kidRun == null) || kidRun.isZero() || !isOrtho(kidRun)) {  // may want to relax this requirement?
        return (null);
      }
      
      // Get my run. If I am weird (or most likely the degenerate root), try using my parent run.
      Vector2D myRun = skids.segment.getRun();      
      if ((myRun == null) || myRun.isZero()) {
        if (parent != null) {
          myRun = parent.segment.getRun();
        }
      }
      
      // After parent replacement, if still weird, outta here: 
      if ((myRun == null) || myRun.isZero() || !isOrtho(myRun)) {
        return (null);
      }
      
      // If I am the first, add in my direction. If not, we have multiple kids, outta here! 
      Vector2D left = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.LEFT);
      if (left.equals(kidRun)) {
        if (retval.left == null) {
          retval.left = kidSwk;
        } else {       
          return (null);
        }
      }
      Vector2D right = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.RIGHT);
      if (right.equals(kidRun)) {
        if (retval.right == null) {
          retval.right = kidSwk;
        } else {    
          return (null);
        }
      }
      if (myRun.equals(kidRun)) {
        if (retval.forward == null) {
          retval.forward = kidSwk;
        } else {
          return (null);
        }
      }
    }
    // Wait... what about backwards run? We are not returning null... just (maybe) an empty directions.  FIX ME?? 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Returns the segments heading out in different directions.  If we have
  ** weirdness (two in one direction, a backwards segment, a zero segment)
  ** it returns null
  */
  
  private LSIDSegmentDirections getDirections(LinkSegmentID lsid, Set<LinkSegmentID> kids, InvertedLinkProps ilp) {
    LSIDSegmentDirections retval = new LSIDSegmentDirections();
    
    //
    // More than three kids? Weirdness! So null...
    //
    int kidCount = kids.size();
    if (kidCount > 3) {
      return (null);
    }
    
    // Get my run. If I am weird (or most likely the degenerate root), try using my parent run.
    Vector2D myRun = ilp.getGeometry(lsid).getRun();      
    if ((myRun == null) || myRun.isZero()) {
      LinkSegmentID parID = ilp.getParentSeg(lsid);
      if (parID != null) {
        myRun = ilp.getGeometry(parID).getRun();
      }
    }
    
    // After parent replacement, if still weird, outta here: 
    if ((myRun == null) || myRun.isZero() || !isOrtho(myRun)) {
      return (null);
    }

    Iterator<LinkSegmentID> kit = kids.iterator();
    while (kit.hasNext()) {
      LinkSegmentID kidLSID = kit.next();
      
      // Get kid run, outta here if weird:
      LinkSegment lseg = ilp.getGeometry(kidLSID);
      Vector2D kidRun = lseg.getRun();    
      if ((kidRun == null) || kidRun.isZero() || !isOrtho(kidRun)) {  // may want to relax this requirement?
        return (null);
      }
  
      // If I am the first, add in my direction. If not, we have multiple kids, outta here! 
      Vector2D left = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.LEFT);
      if (left.equals(kidRun)) {
        if (retval.left == null) {
          retval.left = kidLSID;
        } else {       
          return (null);
        }
      }
      Vector2D right = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.RIGHT);
      if (right.equals(kidRun)) {
        if (retval.right == null) {
          retval.right = kidLSID;
        } else {    
          return (null);
        }
      }
      if (myRun.equals(kidRun)) {
        if (retval.forward == null) {
          retval.forward = kidLSID;
        } else {
          return (null);
        }
      }
      if (myRun.equals(kidRun.scaled(-1.0))) {
        return (null);
      }   
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Returns the segments heading out in the direction of the first segment.
  ** List includes the first segment.
  */
  
  private List<SegmentWithKids> getStraightRun(SegmentWithKids skids) {
    ArrayList<SegmentWithKids> retval = new ArrayList<SegmentWithKids>();
    SegmentWithKids currSwk = skids;
    while (true) {
      retval.add(currSwk);
      SegmentDirections directions = getDirections(currSwk, null);
      if (directions == null) {
        return (null);
      }
      if (directions.forward == null) {
        return (retval);
      }
      currSwk = directions.forward;
    }
  }  
    
  /***************************************************************************
  **
  ** Answer if the corner is a simple turn with no branching.  Null if not,
  ** direction if it is.
  */
  
  private Integer endCornerIsSimple(SegmentWithKids skids) {
    if (skids.kids.size() != 1) {
      return (null);
    }
    Vector2D kidRun = skids.kids.get(0).segment.getRun();
    if ((kidRun == null) || kidRun.isZero() || !isOrtho(kidRun)) {  // may want to relax this requirement?
      return (null);
    }
    Vector2D myRun = skids.segment.getRun();
    if ((myRun == null) || myRun.isZero() || !isOrtho(myRun)) {  
      return (null);
    }
    Vector2D left = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.LEFT);
    if (left.equals(kidRun)) {
      return (new Integer(LinkPlacementGrid.LEFT));
    }
    Vector2D right = LinkPlacementGrid.getTurn(myRun, LinkPlacementGrid.RIGHT);
    if (right.equals(kidRun)) {
      return (new Integer(LinkPlacementGrid.RIGHT));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Answers if given vector is normalized orthogonal
  */
  
  private boolean isOrtho(Vector2D vector) {
    int vecX = (int)vector.getX();
    int vecY = (int)vector.getY();
    if (((vecX == -1) && (vecY == 0)) ||
        ((vecX == 0) && (vecY == 1)) ||
        ((vecX == 1) && (vecY == 0)) ||
        ((vecX == 0) && (vecY == -1))) {
      return (true);
    } else {
      return (false);
    }
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static class BridgeResult {    
    BusProperties.ClosestAnswer ca;
    Point2D segPoint;
    LinkSegment segment;
    boolean splitMe;
    boolean splitHim;    
    
    public String toString() {
      return ("closest = " + ca + " point = " + segPoint + " segment " + 
              segment + " splitMe = " + splitMe + " splitHim = " + splitHim);
    }  
  }  
  
  private static class SegmentDirections {    
    SegmentWithKids left;
    SegmentWithKids forward;
    SegmentWithKids right;
    
    public String toString() {
      return ("left = " + left + " right = " + right + " forward = " + forward);
    }  
  }  
  
  private static class LSIDSegmentDirections {    
    LinkSegmentID left;
    LinkSegmentID forward;
    LinkSegmentID right;
    
    public String toString() {
      return ("left = " + left + " right = " + right + " forward = " + forward);
    }
  }
  
  
  public static class StaggeredRunResult {    
    SegmentWithKids runStart;
    SegmentDirections firstJunction;
    SegmentDirections secondJunction;
    SegmentDirections thirdJunction;
    List<SegmentWithKids> straightRun;
    List<SegmentWithKids> branchAways;
    List<SegmentWithKids> branchBacks;    
    int slideAmount; // negative means to the left
    int firstDirection;
  }
  
  private static class OrthoRunResult {    
    SegmentWithKids runStart;
    SegmentDirections firstJunction;
    OrthoSubrun leftRun;
    OrthoSubrun rightRun;
    MinMax combinedBounds;
    
    public String toString() {
      return ("runStart = " + runStart + " firstJunction = " + firstJunction + 
              " leftRun = " + leftRun + " rightRun = " + rightRun +
              " combinedBounds = " + combinedBounds);
    }
  }
  
  private static class OrthoSubrun {
    SegmentWithKids orthoStart;
    int subrunDirection;
    SegmentDirections terminalJunction;
    Point2D terminalPoint;
    List<SegmentWithKids> straightRun;
    List<SegmentWithKids> branchAways;
    List<SegmentWithKids> branchBacks;
    MinMax bounds;
    
    public String toString() {
      return ("orthoStart = " + orthoStart + " subrunDirection = " + subrunDirection + 
              " terminalJunction = " + terminalJunction + " terminalPoint = " + terminalPoint +
              " straightRun = " + straightRun + " branchAways = " + branchAways +
              " branchBacks = " + branchBacks + " bounds = " + bounds);
    }
  }
  
  private static class DispResult {    
    LinkPlacementGrid.InkValues inkVals;
    int crossingCount;
    Vector2D dispVec;
    double dispLen;
    
    DispResult() {
      this.inkVals = new LinkPlacementGrid.InkValues();
    } 
    
    DispResult(DispResult other) {
      this();
      this.copy(other);
    } 
 
    DispResult(LinkPlacementGrid.InkValues inkVals, int crossingCount,
               Vector2D dispVec, double dispLen) {
      setValues(inkVals, crossingCount, dispVec, dispLen);
    }
    
    static DispResult merge(DispResult origFull, DispResult origSub, DispResult modSub) {
      DispResult retval = new DispResult(modSub);
      retval.inkVals = LinkPlacementGrid.InkValues.merge(origFull.inkVals, origSub.inkVals, modSub.inkVals);   
      retval.crossingCount = (origFull.crossingCount - origSub.crossingCount) + modSub.crossingCount;
      return (retval);
    }
    
    void setValues(LinkPlacementGrid.InkValues inkVals, int crossingCount,
                   Vector2D dispVec, double dispLen) {
      this.inkVals = new LinkPlacementGrid.InkValues(inkVals);
      this.crossingCount = crossingCount;
      this.dispVec = new Vector2D(dispVec);
      this.dispLen = dispLen;
    }    
    
    void copy(DispResult other) {
      this.inkVals.copy(other.inkVals);
      this.crossingCount = other.crossingCount;
      this.dispVec = new Vector2D(other.dispVec);
      this.dispLen = other.dispLen;
    }
  }
}
