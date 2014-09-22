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

package org.systemsbiology.biotapestry.util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.analysis.GraphColorer;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.CornerOracle;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;

/****************************************************************************
**
** A class to assist with routing links
*/

public class LinkPlacementGrid {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int STOP     = 0;    
  private static final int OK       = 1;
  private static final int CONTINUE = 2;

  private static final int NO_CORNER          = 0;    
  private static final int STRAIGHT_CORNER    = 1;   
  private static final int SINGLE_BEND_CORNER = 2; 
  private static final int COMPLEX_CORNER     = 3;
  private static final int MULTI_CORNER       = 4;  
  
  private static final int NOT_RUN  = 0;    
  private static final int VERT_RUN = 1;   
  private static final int HORZ_RUN = 2;
  private static final int BAD_RUN  = 3;
  
  private static final int NO_CLEANUP_           = 0;    
  private static final int DID_CLEANUP_          = 1;

  private static final int RECOV_STEP_NO_CODE_              = -1;
  private static final int RECOV_STEP_CORE_FAILED_          = 0;
  private static final int RECOV_STEP_CORE_NO_TURN_         = 1;
  private static final int RECOV_STEP_CORE_NO_TURN_NO_JUMP_ = 2;  
  private static final int RECOV_STEP_EMBEDDED_JUMP_FAILED_ = 3; 
  private static final int RECOV_STEP_CORE_OK_              = 4;   
  
  ////////////////////////////////////////////////////////////////////////////    
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int NONE  = 0x00;    
  public static final int NORTH = 0x01;
  public static final int EAST  = 0x02; 
  public static final int SOUTH = 0x04;
  public static final int WEST  = 0x08;
  public static final int DIAGONAL = 0x10;
  
  public static final int IS_EMPTY                  = 0;
  public static final int IS_NODE                   = 1;
  public static final int IS_NODE_INBOUND_OK        = 2;
  public static final int IS_CORNER                 = 3;
  public static final int IS_PAD                    = 4;
  public static final int IS_RUN                    = 5;
  public static final int IS_DEGENERATE             = 6;
  public static final int IS_RESERVED_DEPARTURE_RUN = 7;  
  public static final int IS_RESERVED_ARRIVAL_RUN   = 8;
  public static final int IS_GROUP                  = 9;  // Hacktastic: use for modules as well
  public static final int IS_RESERVED_MULTI_LINK_ARRIVAL_RUN   = 10;
  
  public static final int LEFT     = 0;    
  public static final int RIGHT    = 1; 
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashMap<String, Rectangle> groups_;
  private HashMap<Point, GridContents> patternx_;
  private HashMap<Point, Map<String, DecoInfo>> userData_;
  private HashMap<Double, Double> gauss_;
  private Rectangle bounds_;
  private double sqrt2o2_;
  private HashSet<String> drawnLinks_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LinkPlacementGrid() {
    groups_ = new HashMap<String, Rectangle>();
    patternx_ = new HashMap<Point, GridContents>();
    userData_ = new HashMap<Point, Map<String, DecoInfo>>();
    gauss_ = new HashMap<Double, Double>();
    sqrt2o2_ = Math.sqrt(2.0) / 2.0;
    bounds_ = null;
    drawnLinks_ = new HashSet<String>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Answers if the given rectangle is empty of content
  */

  public boolean isEmpty(Rectangle rect) {    
    //
    // This is backwards.  Only iterate over the points in the rect!
    //
    Rectangle cRect = rectConversion(rect);
    Iterator<Point> it = new PatternIterator(false, false);
    while (it.hasNext()) {
      Point pt = it.next();
      if (cRect.contains(pt)) {
        GridContents gc = this.getGridContents(pt, false);
        if (gc != null) {
          return (false);
        }
      }
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Get back a subset of the full grid.  Note that drawn links semantics
  ** are flawed; contains all links from original.
  */

  public LinkPlacementGrid subset(Rectangle rect) {
     
    LinkPlacementGrid retval = new LinkPlacementGrid();
    retval.bounds_ = (Rectangle)rect.clone();
    Iterator<Point> it = new PatternIterator(false, false);

    while (it.hasNext()) {
      Point pt = it.next();
      if (retval.bounds_.contains(pt)) {
        GridContents gc = this.getGridContents(pt, false);
        retval.patternx_.put((Point)pt.clone(), gc.clone()); 
      }
    }
    
    Iterator<String> git = this.groups_.keySet().iterator();
    while (git.hasNext()) {
      String gid = git.next();
      Rectangle gRect = this.groups_.get(gid);
      Rectangle2D retPart = gRect.createIntersection(retval.bounds_);
      if ((retPart.getHeight() > 0.0) && (retPart.getWidth() > 0.0)) {
        retval.groups_.put(gid, UiUtil.rectFromRect2D(retPart));
      }
    }
    
    // This will be a superset of all the links in the subset.
    retval.drawnLinks_ = new HashSet<String>(this.drawnLinks_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop all links for given source
  */
  
  public void dropLink(String srcID, Genome genome, String overlayID) {
    //
    // We do this in a multi-pass fashion to allow a read-only PatternIterator:
    //
    Iterator<Point> acit = new PatternIterator(false, false);
    ArrayList<Point> deadList = new ArrayList<Point>();
    HashMap<Point, GridContents> holdMods = new HashMap<Point, GridContents>();
    while (acit.hasNext()) {
      Point cellPt = acit.next();
      GridContents gc = getGridContents(cellPt, false);
      if (!lookForSource(srcID, gc)) {
        continue;
      }
      if (!dropLinkReference(gc, srcID, cellPt, deadList)) {
        holdMods.put(cellPt, gc);
      }
    }
    
    Iterator<Point> mit = holdMods.keySet().iterator();
    while (mit.hasNext()) {
      Point cellPt = mit.next();
      GridContents gc = holdMods.get(cellPt);
      writeBackModifiedGridContents(cellPt, gc);
    }
    
    int dlnum = deadList.size();
    for (int i = 0; i < dlnum; i++) {
      Point dead = deadList.get(i);
      dropFromPattern(dead);
    }
      
    drawnLinkAccounting(genome, srcID, overlayID);
   
    return;
  }
 
  /***************************************************************************
  **
  ** Debug output for a pattern grid
  */
  
  public void debugPatternGrid(boolean skipGroups) {
    PatternGrid retval = new PatternGrid();
    Iterator<Point> kit = new PatternIterator(!skipGroups, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if (skipGroups && cellIsGroupOnly(gval)) {
        continue;
      }
      retval.fill(pt.x, pt.y, "X");
    }
    System.out.println(retval);
    return;
  }

  /***************************************************************************
  **
  ** Extract a pattern grid
  */
  
  public PatternGrid extractPatternGrid(boolean skipGroups) {
    PatternGrid retval = new PatternGrid();
    Iterator<Point> kit = new PatternIterator(!skipGroups, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if (skipGroups && cellIsGroupOnly(gval)) {
        continue;
      }
      retval.fill(pt.x, pt.y, "");
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find problems with module-directed link placement:
  */

  public Map<Point, Map<String, DecoInfo>> findBadModuleCells(boolean ignoreBacktracks) {
   
    HashMap<Point, Map<String, DecoInfo>> badRuns = new HashMap<Point, Map<String, DecoInfo>>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      //
      // Here's the rub.  Bad cells include ones where a single link backtracks on
      // itself before turning around again to continue in the same direction.  It's
      // trut this is bad, but we actually want to ignore it in many cases, since this
      // is not something module expansion will fix!  Instead it gets fixed in link
      // geometry repair!
      //
          
      Set<String> badCell = hasBadCellContents(gval);   
      if (badCell != null) {
        if (!ignoreBacktracks || (badCell.size() > 1)) {
          addForBadCell(badCell, pt, badRuns, false);
        }
      }
      Set<String> moduleOverlap = hasBadModuleCollisions(gval);
      if (moduleOverlap != null) {
        addForBadCell(moduleOverlap, pt, badRuns, true);
      }
    }
    return (badRuns.isEmpty() ? null : badRuns);
  }
 
  /***************************************************************************
  **
  ** Handle bad cell data
  */

  private void addForBadCell(Set<String> badCell, Point pt, Map<Point, Map<String, DecoInfo>> badRuns, boolean isForMod) {
  
    Map<String, DecoInfo> forPt = badRuns.get(pt);
    if (forPt == null) {
      forPt = new HashMap<String, DecoInfo>();
      badRuns.put(pt, forPt);
    }
    Iterator<String> bcit = badCell.iterator();
    while (bcit.hasNext()) {
      String key = bcit.next();
      DecoInfo di = getUserData(pt, key);
      if (di != null) {
        forPt.put(key, di);
      }
    }
    if (isForMod) {
      DecoInfo di4mod = new DecoInfo(true);
      forPt.put(DecoInfo.MODULE_KEY, di4mod);
    }
    return;  
  }
  
  /***************************************************************************
  **
  ** Find links that overlap incorrectly
  */

  public Set<String> findBadRuns() {
   
    HashSet<String> badRuns = new HashSet<String>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      Set<String> badCell = hasBadCellContents(gval);
      if (badCell != null) {
        badRuns.addAll(badCell);
      }
    }
    return (badRuns.isEmpty() ? null : badRuns);
  }
  
  /***************************************************************************
  **
  ** Find incorrect overlaps with nodes
  */

  public Set<Point> getNodeOverlaps() {
   
    HashSet<Point> badPoints = new HashSet<Point>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if (hasNodeOverlap(gval)) {
        badPoints.add(pt);
      }
    }
    return (badPoints);
  }  

  /***************************************************************************
  **
  ** Determine if this cell is well-formed or not.  If not, returns list
  ** of sources, else return null.
  */
  
  private Set<String> hasBadCellContents(GridContents gc) {
    
    HashSet<String> retval = new HashSet<String>();
    if (gc == null) {
      return (null);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.cornerType() == MULTI_CORNER) {
        retval.add(gc.entry.id);
        return (retval);
      } else {
        return (null);
      }
    }

    //
    // Note that here we add ALL ids in the cell.   This can 
    // result in over-conservative expansions if not interpreted 
    // correctly!
    //
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_CORNER) || 
          (currEntry.type == IS_PAD) ||
          (currEntry.type == IS_RUN)) {
        retval.add(currEntry.id);
      }
    }    
  
    boolean gottaCorner = false;
    boolean gottaVertRun = false;
    boolean gottaHorzRun = false;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      int corType = currEntry.cornerType();
      if (corType == MULTI_CORNER) {
        return (retval);
      } else if (corType != NO_CORNER) {
        if (gottaCorner || gottaVertRun || gottaHorzRun) {
          return (retval);
        }
        gottaCorner = true;
      } else {
        int runType = currEntry.runDirection();
        if (runType == NOT_RUN) {
          continue;
        } else if (runType == BAD_RUN) {
          return (retval);
        } else if (runType == VERT_RUN) {
          if (gottaCorner || gottaVertRun) {
            return (retval);
          }
          gottaVertRun = true;
        } else if (runType == HORZ_RUN) {
          if (gottaCorner || gottaHorzRun) {
            return (retval);
          }
          gottaHorzRun = true;
        }
      }
    }

    return (null);
  } 
  
  /***************************************************************************
  **
  ** Determine if this cell is works out for module link runs
  ** of sources, else return null.
  */
  
  private Set<String> hasBadModuleCollisions(GridContents gc) {
    
    HashSet<String> retval = new HashSet<String>();
    if (gc == null) {
      return (null);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (null);
    }

    boolean gottaModule = false;
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_CORNER) || (currEntry.type == IS_RUN)) {
        retval.add(currEntry.id);
      }
      if (currEntry.type == IS_GROUP) {
        gottaModule = true;
      }
    }
    
    if (gottaModule && !retval.isEmpty()) {
      return (retval);
    }
    
    return (null);
  } 
 
  /***************************************************************************
  **
  ** Used to determine if reversing a grid expansion results in node collisions
  */
  
  private boolean hasNodeOverlap(GridContents gc) {
    
    if (gc == null) {
      return (false);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (false);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry1 = gc.extra.get(i);
      for (int j = 0; j < size; j++) {
        if (i == j) {
          continue;
        }
        GridEntry currEntry2 = gc.extra.get(j);
        if (currEntry1.hasNodeCollision(currEntry2)) {
          return (true);
        }
      }
    }
    
    return (false);
  }

  /***************************************************************************
  **
  ** Get the closest point NOT IN THE GIVEN SET and return the type.
  */
  
  public int getClosestPoint(String srcID, Point target, Point result, 
                             Set<Point> ignore, Set<String> okGroups, Set<String> linksFromSource) throws NonConvergenceException {
    int retval = IS_EMPTY;
    //int minDist = Integer.MAX_VALUE;
    double minDist = Double.POSITIVE_INFINITY;
    Point currResult = null;

    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      if (ignore.contains(pt)) {
        continue;
      }
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      int cellType = lookForLaunchCell(srcID, gval, okGroups, linksFromSource);
      if (cellType != IS_EMPTY) {
        Point2D ptr = new Point2D.Double(pt.x, pt.y);
        if (notOverMyGenePad(srcID, null, ptr)) {
          continue;
        }
        double distance = new Vector2D(ptr, target).length();        
        // Use Manhattan distance; more realistic of orthogonal routing
        //int distance = Math.abs(pt.x - target.x) + Math.abs(pt.y - target.y);
        if (distance < minDist) {
          currResult = pt;
          minDist = distance;
          retval = cellType;
        }
      }
    }
    if (currResult != null) {
      result.setLocation(currResult);
    } else {
      throw new NonConvergenceException();
    }

    return (retval);
  } 

  /***************************************************************************
  **
  ** If a possible "closest point" is over a gene pad that the source is
  ** not targeting, we can't use it:
  */
  
  public boolean notOverMyGenePad(String srcID, String trgID, Point2D candidate) {
    // FIX ME: Implement this!
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get a set of run alternatives
  */
  
  public Set<PointAlternative> getRunAlternatives(String srcID, Point original, int origType, Set<Point> ignore) {
    //
    // Take the point and head out in the run directions to create a set
    // of alternatives.  Stop when we hit corners; include the corners.
    //
    
    HashSet<PointAlternative> retval = new HashSet<PointAlternative>();
    retval.add(new PointAlternative(original, origType, NONE));
    GridContents gc = getGridContents(original);
    if (gc.extra != null) {  // Don't mess with multi-entry slots
      return (retval);
    }
    
    
    HashSet<Integer> checkVec = new HashSet<Integer>();
    GridEntry entry = gc.entry;
    
    if (entry.entryCount > 1) {  // Don't mess with multi-corner points
      return (retval);
    }
    if (((entry.exits & NORTH) != 0x00) || ((entry.entries & NORTH) != 0x00)) {
      checkVec.add(new Integer(NORTH));
    }
    if (((entry.exits & SOUTH) != 0x00) || ((entry.entries & SOUTH) != 0x00)) {
      checkVec.add(new Integer(SOUTH));
    }
    if (((entry.exits & EAST) != 0x00) || ((entry.entries & EAST) != 0x00)) {
      checkVec.add(new Integer(EAST));
    }
    if (((entry.exits & WEST) != 0x00) || ((entry.entries & WEST) != 0x00)) {
      checkVec.add(new Integer(WEST));
    }
    
    Iterator<Integer> cvit = checkVec.iterator();
    while (cvit.hasNext()) {
      Integer dir = cvit.next();
      Vector2D vec = getVector(dir.intValue());
      Point2D newPt = (Point2D)original.clone();
      while (true) {
        newPt = vec.add(newPt);
        Point intPt = new Point((int)newPt.getX(), (int)newPt.getY());
        GridContents currGc = getGridContents(intPt);
        if (currGc == null) {  //FIX ME: SHOULD NEVER HAPPEN, BUT IT DOES!
          break;
        }
        if (this.isSingleRunForSource(currGc, srcID)) {
          if ((currGc.extra == null) && (!ignore.contains(intPt))) {
            retval.add(new PointAlternative((Point)intPt.clone(), IS_RUN, NONE));
          }
          continue;
        } else if (currGc.extra != null) {  // Don't mess with multi-entry slots
          break;
        }
        GridEntry currEntry = currGc.entry;
        if (currEntry.id.equals(entry.id)) { 
          if (currEntry.type == IS_CORNER) {
            if (!ignore.contains(intPt)) {
              retval.add(new PointAlternative((Point)intPt.clone(), IS_CORNER, NONE));
            }
            break;
          } else {
            break;
          }
        } else {
          break;
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the best departure directions
  */

  private List<Integer> getBestDirections(Point start, Point2D scaleStart, Point2D end) {

    Vector2D direct = new Vector2D(scaleStart, end);
    //
    // Find the valid exit directions for the point:
    //
    List<Vector2D> exits = getPossibleExits(start);
    if (exits == null) {
      return (null);
    }
      
    //
    // Return the available direction that has the best dot product, 
    // followed by the next best, etc.
    //
    
    TreeMap<Double, List<Integer>> bestest = new TreeMap<Double, List<Integer>>(Collections.reverseOrder());
    Iterator<Vector2D> eit = exits.iterator();
    while (eit.hasNext()) {
      Vector2D pExit = eit.next();
      double dot = direct.dot(pExit);
      Double dotObj = new Double(dot);
      List<Integer> dirs = bestest.get(dotObj);
      if (dirs == null) {
        dirs = new ArrayList<Integer>();
        bestest.put(dotObj, dirs);
      }
      dirs.add(new Integer(getDirection(pExit)));
    }
    
    ArrayList<Integer> retval = new ArrayList<Integer>();
    Iterator<List<Integer>> bvit = bestest.values().iterator();
    while (bvit.hasNext()) {
      List<Integer> nextList = bvit.next();
      retval.addAll(nextList);
    }
    return ((retval.isEmpty()) ? null : retval);
  }
  
  /***************************************************************************
  **
  ** Choose the best alternative *closest* to the original point
  */
  
  public PointAlternative chooseBestAlternative(String src, String trg, String linkID, Point end, 
                                                Set<PointAlternative> nearby, Point orig, int origType,
                                                Set<String> okGroups, boolean isStart) {
    
    Point2D ptEnd = new Point2D.Double(end.getX(), end.getY());
    double bestDist = 0.0;
    HashMap<PointAlternative, TravelResult> bestCorners = new HashMap<PointAlternative, TravelResult>();
    HashMap<PointAlternative, TravelResult> bestRuns = new HashMap<PointAlternative, TravelResult>();
    HashMap<PointAlternative, TravelResult> farCorners = new HashMap<PointAlternative, TravelResult>();
    HashMap<PointAlternative, TravelResult> farRuns = new HashMap<PointAlternative, TravelResult>();    
    HashSet<PointAlternative> orthoCorners = new HashSet<PointAlternative>();
    HashSet<PointAlternative> orthoRuns = new HashSet<PointAlternative>();    
    
    //
    // Best: A corner near the original, with best direction dot > 0
    // Next: Run near original, with best dot > 0
    // Next: A corner away from original, with best direction dot > 0
    // Next: Run away from original, with best dot > 0   
    // Next: Corner with dot == 0
    // Next: Run with dot == 0
    // Corners with only slots in opposite direction or no slots are discarded.
    //

    HashSet<String> groupBlockers = (okGroups != null) ? new HashSet<String>() : null;
    
    
    //
    // Crank through each start alternative:
    //
    
    Iterator<PointAlternative> nit = nearby.iterator();
    while (nit.hasNext()) {
      PointAlternative noDirAlt = nit.next();
      //
      // Find out the best departure direction for the candidate.  If none, we skip
      //
      Point2D ptOnRun = new Point2D.Double(noDirAlt.point.getX(), noDirAlt.point.getY());          
      List<Integer> bestDirs = getBestDirections(noDirAlt.point, ptOnRun, end);
      if (bestDirs == null) {
        continue;
      }
      //
      // Skip directions heading the wrong way.  If orthogonal, hold onto it as an ortho
      // type and move on:
      //
      int numDirs = bestDirs.size();
      for (int i = 0; i < numDirs; i++) {
        Integer dir = bestDirs.get(i);
        int bestDir = dir.intValue();
        PointAlternative dirAlt = noDirAlt.clone();
        dirAlt.dir = bestDir;
        Vector2D bestVec = getVector(bestDir);
        Vector2D travel = new Vector2D(ptOnRun, ptEnd);
        double fullDist = travel.dot(bestVec);
        if (fullDist < 0.0) {
          continue;
        } else if (fullDist == 0.0) {
          if (noDirAlt.type == IS_CORNER) {     
            orthoCorners.add(dirAlt);
          } else {
            orthoRuns.add(dirAlt);
          }
          continue;        
        }

        //
        // Figure out how far we get with the candidate, as a fraction of how far we want to
        // go:
        //
        Point2D ptStart = bestVec.add(ptOnRun);
        TravelResult res = analyzeTravel(src, ptStart, bestVec, trg, ptEnd, linkID, okGroups, groupBlockers, isStart);
        double offset = (new Vector2D(orig, ptOnRun)).length();
      //  if (res.fraction > bestDist) {
      //    bestDist = res.fraction;
     //   }
        if (res.travelDistance > bestDist) {
          bestDist = res.travelDistance;
        }       
        
        if (offset < fullDist) {
          if (dirAlt.type == IS_CORNER) {
            bestCorners.put(dirAlt, res);
          } else if (dirAlt.type == IS_RUN) {
            bestRuns.put(dirAlt, res);
          }
        } else {
          if (dirAlt.type == IS_CORNER) {
            farCorners.put(dirAlt, res);
          } else if (dirAlt.type == IS_RUN) {
            farRuns.put(dirAlt, res);
          }        
        }
      }
    }
    
    //
    // Crank through all the possibilities, return the best:
    //
    
    PointAlternative retval = bestAlternative(bestCorners, orig, bestDist);
    if (retval != null) {
      return (retval);
    }
    retval = bestAlternative(bestRuns, orig, bestDist);
    if (retval != null) {
      return (retval);
    }            
    retval = bestAlternative(farCorners, orig, bestDist);
    if (retval != null) {
      return (retval);
    }
    retval = bestAlternative(farRuns, orig, bestDist);
    if (retval != null) {
      return (retval);
    }
           
    if (!orthoCorners.isEmpty()) {
      return (bestOrtho(orthoCorners, orig));
    } else if (!orthoRuns.isEmpty()) {
      return (bestOrtho(orthoRuns, orig));      
    } else {
      return (null);
    }
  }  

  /***************************************************************************
  **
  ** Choose the best alternative closest to the target
  */
  
  public PointAlternative chooseBestAlternativeCloseToTarg(String src, String trg, String linkID, Point end, 
                                                           Set<PointAlternative> nearby, Point orig, int origType,
                                                           Set<String> okGroups, boolean isStart) {
    
    Point2D ptEnd = new Point2D.Double(end.getX(), end.getY());
    double bestDist = 0.0;
    HashMap<PointAlternative, TravelResult> bestCorners = new HashMap<PointAlternative, TravelResult>();
    HashMap<PointAlternative, TravelResult> bestRuns = new HashMap<PointAlternative, TravelResult>();
    HashSet<PointAlternative> orthoCorners = new HashSet<PointAlternative>();
    HashSet<PointAlternative> orthoRuns = new HashSet<PointAlternative>();
    HashMap<PointAlternative, Double> runFracs = new HashMap<PointAlternative, Double>();
    HashMap<Integer, Double> bestFracForDir = new HashMap<Integer, Double>();
    
    HashSet<String> groupBlockers = (okGroups != null) ? new HashSet<String>() : null;
      
    //
    // Crank through each start alternative:
    //
    
    Iterator<PointAlternative> nit = nearby.iterator();
    while (nit.hasNext()) {
      PointAlternative noDirAlt = nit.next();
      //
      // Find out the best departure direction for the candidate.  If none, we skip
      //
      Point2D ptOnRun = new Point2D.Double(noDirAlt.point.getX(), noDirAlt.point.getY());          
      List<Integer> bestDirs = getBestDirections(noDirAlt.point, ptOnRun, end);
      if (bestDirs == null) {
        continue;
      }
    //  double offset = (new Vector2D(orig, ptOnRun)).length();
            
      //
      // Skip directions heading the wrong way.  If orthogonal, hold onto it as an ortho
      // type and move on:
      //
      int numDirs = bestDirs.size();
      for (int i = 0; i < numDirs; i++) {
        Integer dir = bestDirs.get(i);
        int bestDir = dir.intValue();
        PointAlternative dirAlt = noDirAlt.clone();
        dirAlt.dir = bestDir;
        Vector2D bestVec = getVector(bestDir);
        Point2D ptStart = bestVec.add(ptOnRun);
        Vector2D travel = new Vector2D(ptStart, ptEnd);       
        double fullDist = travel.dot(bestVec);
        if (fullDist < 0.0) {
          continue;
        } else if (fullDist == 0.0) {
          if (noDirAlt.type == IS_CORNER) {     
            orthoCorners.add(dirAlt);
          } else {
            orthoRuns.add(dirAlt);
          }
          continue;        
        }

        //
        // Figure out how far we get with the candidate:
        //
       
        TravelResult res = analyzeTravel(src, ptStart, bestVec, trg, ptEnd, linkID, okGroups, groupBlockers, isStart);
        if (res.travelDistance > bestDist) {
          bestDist = res.travelDistance;
        }
        double currFrac = res.travelDistance / fullDist;
        Double currFracObj = new Double(currFrac);
        Integer bestDirObj = new Integer(bestDir);
        Double bestFrac = bestFracForDir.get(bestDirObj);
        if ((bestFrac == null) || (currFrac > bestFrac.doubleValue())) {
          bestFracForDir.put(bestDirObj, currFracObj);       
        }
        runFracs.put(dirAlt, currFracObj);
        
        if (dirAlt.type == IS_CORNER) {
          bestCorners.put(dirAlt, res);
        } else if (dirAlt.type == IS_RUN) {
          bestRuns.put(dirAlt, res);
        }        
      }
    }

    //
    // Crank through all the possibilities, return the best.  If the only alternative is zero
    // travel, just return null:
    //
    
    PointAlternative retval = bestAlternative(bestRuns, end, runFracs, bestFracForDir);
    if ((retval != null) && (bestRuns.get(retval).travelDistance == 0)) {
      retval = null;
    }
    PointAlternative retvalCor = bestAlternative(bestCorners, end, runFracs, bestFracForDir);
    if ((retval != null) && (bestRuns.get(retval).travelDistance == 0)) {
      retval = null;
    }    
    if (retvalCor != null) {
      if (retval == null) {
        return (retvalCor);
      } else {
        //
        // Choose a corner instead of a run if they are right next to each other:
        return ((retval.point.distance(retvalCor.point) <= 2.0) ? retvalCor : retval);
      }
    }
    
    if (retval != null) {
      return (retval);
    }
           
    if (!orthoCorners.isEmpty()) {
      return (bestOrtho(orthoCorners, end));
    } else if (!orthoRuns.isEmpty()) {
      return (bestOrtho(orthoRuns, end));      
    } else {
      return (null);
    }
  }
  
  
  /***************************************************************************
  **
  ** Choose the best alternative
  */
  
  public PointAlternative bestAlternative(Map<PointAlternative, TravelResult> alts, Point orig, 
                                          Map<PointAlternative, Double> runFracs, 
                                          Map<Integer, Double> bestFracPerDir) {
 
    //
    // For a given travel direction, choose the points with the best fraction to the
    // target.  Of points with equal fraction, we take the ones closest to the target.
    // If there is a tie, take the point with the fewest crossings.
    //
    // Of the different direction possibilities, take the one that is closest.  If
    // that is a tie, take the one with that goes the farthest absolute distance to
    // the target.
    //
    
    
    HashMap<Integer, PointAlternative> bestPAPerDir = new HashMap<Integer, PointAlternative>();
    HashMap<Integer, Integer> bestCrossingPerDir = new HashMap<Integer, Integer>();
    HashMap<Integer, Double> bestOffsetPerDir = new HashMap<Integer, Double>();
    HashMap<Integer, Double> bestAbsPerDir = new HashMap<Integer, Double>();    
    double bestOffsetOfAll = Double.POSITIVE_INFINITY;
     
    Iterator<PointAlternative> ait = alts.keySet().iterator();
    while (ait.hasNext()) {
      PointAlternative alt = ait.next();
      //
      // Filter on fraction:
      //
      double runFrac = runFracs.get(alt).doubleValue();
      Integer dirObj = new Integer(alt.dir);
      double bestFrac = bestFracPerDir.get(dirObj).doubleValue();
      if (runFrac != bestFrac) {
        continue;
      }
      //
      // Filter on offset:
      //
      Point2D ptOnRun = new Point2D.Double(alt.point.getX(), alt.point.getY()); 
      double offset = (new Vector2D(orig, ptOnRun)).length();
      Double bestOffset = bestOffsetPerDir.get(dirObj);
      if ((bestOffset == null) || (offset <= bestOffset.doubleValue())) {
        if ((bestOffset == null) || (offset < bestOffset.doubleValue())) {
          bestOffsetPerDir.put(dirObj, new Double(offset));
          bestCrossingPerDir.remove(dirObj); // Crossing counts are stale...
          if (offset < bestOffsetOfAll) {
            bestOffsetOfAll = offset;
          }
        }
      } else {
        continue;
      }
      //
      // Filter on crossings:
      //
      TravelResult res = alts.get(alt);
      Integer bestCrossing = bestCrossingPerDir.get(dirObj);
      if ((bestCrossing == null) || (res.crossings <= bestCrossing.intValue())) {
        bestCrossingPerDir.put(dirObj, new Integer(res.crossings));
      } else {
        continue;
      }
      bestPAPerDir.put(dirObj, alt);
      bestAbsPerDir.put(dirObj, new Double(res.travelDistance));
    }

    //
    // Ditch alternatives that are not the best:
    //
    
    Iterator<Integer> bopdit = bestOffsetPerDir.keySet().iterator();
    while (bopdit.hasNext()) {
      Integer dirObj = bopdit.next();
      double offset = bestOffsetPerDir.get(dirObj).doubleValue();
      if (offset != bestOffsetOfAll) {
        bestPAPerDir.remove(dirObj);
      }      
    }    
    
    PointAlternative retval = null;
    double bestAbs = Double.NEGATIVE_INFINITY;
    Iterator<Integer> bpadit = bestPAPerDir.keySet().iterator();
    while (bpadit.hasNext()) {
      Integer dirObj = bpadit.next();
      double abs = bestAbsPerDir.get(dirObj).doubleValue();
      if (abs > bestAbs) {
        retval = bestPAPerDir.get(dirObj);
        bestAbs = abs;
      }
    } 
    return (retval);
  }   
  
  /***************************************************************************
  **
  ** Choose the best alternative
  */
  
  public PointAlternative bestAlternative(Map<PointAlternative, TravelResult> alts, Point orig, double bestVal) {
    
    PointAlternative retval = null;
    int bestCrossing = Integer.MAX_VALUE;
    double bestOffset = Double.POSITIVE_INFINITY;

    //
    // Only looking at the alternatives that give us the best fraction to the
    // target, we choose the one with the best offset, i.e. distance from the
    // original point alternative.  If two offsets are equal, we choose the one 
    // with the fewest crossings.
    //
    
    Iterator<PointAlternative> ait = alts.keySet().iterator();
    while (ait.hasNext()) {
      PointAlternative alt = ait.next();
      TravelResult res = alts.get(alt);
      Point2D ptOnRun = new Point2D.Double(alt.point.getX(), alt.point.getY()); 
      
      double offset = (new Vector2D(orig, ptOnRun)).length();
      if (res.travelDistance != bestVal) {        
        continue;
      }
      if (offset < bestOffset) {
        retval = alt;
        bestCrossing = res.crossings;
        bestOffset = offset;
      } else if (offset == bestOffset) {
        if (res.crossings < bestCrossing) {
          retval = alt;
          bestCrossing = res.crossings;
        }
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Choose the best alternative among ortho options, which is measured as the
  ** closest to the original point.
  */
  
  public PointAlternative bestOrtho(Set<PointAlternative> alts, Point orig) {
    
    PointAlternative retval = null;
    double bestOffset = Double.POSITIVE_INFINITY;

    Iterator<PointAlternative> ait = alts.iterator();
    while (ait.hasNext()) {
      PointAlternative alt = ait.next();
      Point2D ptOnRun = new Point2D.Double(alt.point.getX(), alt.point.getY());      
      double offset = (new Vector2D(orig, ptOnRun)).length();
      if (offset < bestOffset) {
        retval = alt;
        bestOffset = offset;
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get possible exit directions
  */
  
  public List<Vector2D> getPossibleExits(Point exitPt) {
     
    GridContents gc = getGridContents(exitPt);
    if (gc == null) {
      return (null);
    }

    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.availableExits());
    }
    
    int size = gc.extra.size();
    List<Vector2D> retval = null;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_GROUP) {
        continue;
      } else if ((currEntry.type == IS_RUN) || (currEntry.type == IS_CORNER)) {
        retval = currEntry.availableExits();
        return (retval);
      } 
    }
   
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the entry direction for the source
  */
  
  private int getEntryDirectionForSrc(Point pt, String srcID) {
    GridContents gc = getGridContents(pt);
    if (gc == null) {
      return (NONE);
    }
    List<GridEntry> entries = linkEntriesForSource(srcID, gc);
    int numEntries = entries.size();
    for (int i = 0; i < numEntries; /*i++*/) {
      GridEntry currEntry = entries.get(i);
      return (currEntry.entries); // Just return the first...
    }
    
    return (NONE);
  }  
  
  /***************************************************************************
  **
  ** Return the entries for links from the given source
  */
  
  private List<GridEntry> linkEntriesForSource(String srcID, GridContents gc) {
    ArrayList<GridEntry> retval = new ArrayList<GridEntry>();
    if (gc == null) {
      return (retval);
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.id.equals(srcID) && 
          ((gc.entry.type != IS_NODE) && (gc.entry.type != IS_NODE_INBOUND_OK))) {
        retval.add(gc.entry);
      }
      return (retval);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.id.equals(srcID) && 
          ((currEntry.type != IS_NODE) && (currEntry.type != IS_NODE_INBOUND_OK))) {
        retval.add(currEntry);
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the list of exemptions
  */
  
  public Map<String, Set<String>> buildExemptions() {
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      lookForExemption(gval, retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Generate exemptions for this cell
  */
  
  private void lookForExemption(GridContents gc, Map<String, Set<String>> exemptions) {
    if (gc == null) {
      return;
    }
    //
    // Single entry cells cannot generate an exemption:
    //
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return;
    }
    
    //
    // Multi entry cells.  If node(s) are present and links are present,
    // add to exemption list:
    //
    
    HashSet<String> nodes = new HashSet<String>();
    HashSet<String> links = new HashSet<String>();
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_NODE) || (currEntry.type == IS_NODE_INBOUND_OK)) {
        nodes.add(currEntry.id);
      }
      if ((currEntry.type == IS_CORNER) || (currEntry.type == IS_RUN)) {
        links.add(currEntry.id);
      }
    }
    
    Iterator<String> nit = nodes.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Iterator<String> lit = links.iterator();
      while (lit.hasNext()) {
        String srcID = lit.next();
        Set<String> exempt = exemptions.get(srcID);
        if (exempt == null) {
          exempt = new HashSet<String>();
          exemptions.put(srcID, exempt);
        }
        exempt.add(nodeID);
      }
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Answer if the given source has a link already in the grid.
  */
  
  public boolean linkForSourceIsPresent(String srcID, Genome genome, String overlayID) {   
    //
    // New code that SHOULD be consistent with old method:
    //
    HashSet<String> drawnForSource = drawnLinksForID(genome, srcID, overlayID);
     
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if (lookForSource(srcID, gval)) {
        if (drawnForSource.isEmpty()) {
          System.err.println("Incorrect drawnLinks status:" + drawnLinks_);
        }
        return (true);
      }
    }
    if (!drawnForSource.isEmpty()) {
      System.err.println("Incorrect drawnForSource status:" + drawnForSource);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Return the set of links that have been drawn for the source
  */
  
  public Set<String> allLinksDrawnForSource(String srcID, Genome genome, String overlayID) {   
    return (drawnLinksForID(genome, srcID, overlayID));
  }
     
  /***************************************************************************
  **
  ** Answer if the link is already in the grid.
  */
  
  public boolean linkIsDrawn(String linkID, String overlayID) {
    if (overlayID != null) {
      throw new IllegalArgumentException();
    }   
    return (this.drawnLinks_.contains(linkID));
  }
 
  /***************************************************************************
  **
  ** Answer if the given source has a link already at the given point
  */
  
  public boolean linkIsPresentAtPoint(String srcID, Point pt) {
    GridContents gval = getGridContents(pt);
    if (gval == null) {
      return (false);
    }
    return (lookForSource(srcID, gval));
  }  

  /***************************************************************************
  **
  ** Answer if a link reroute is allowed.
  */
  
  public boolean rerouteAllowed(String src, Point2D start, Point2D oldEnd,
                                Point2D newEnd, String trg, Set<String> okGroups,
                                List<Point2D> awayBranchPts, List<Point2D> backBranchPts, boolean isStart) {
                                  
    //
    // The line between the start and the newEnd must be OK for a run, ignoring
    // points already owned by back branches.
    // The perpendicular segments have the following criteria:
    // 1) If we have a branch in (in the direction of the shift), it must be
    // that we own all points betwen the old and new ends (this should already
    // have been verified before this call).  Checking now would be a useful assertion.
    // 2) If we have ONLY a branch out, then all points between old and new
    // ends should accept a run in the branch out direction.
    //

    //
    // Calculate points we ignore along the way (they will already be occupied by
    // branch backs:
    //

    Point startPt = new Point(((int)start.getX() / 10), ((int)start.getY() / 10)); 
    Point newEndPt = new Point(((int)newEnd.getX() / 10), ((int)newEnd.getY() / 10));
   // Point oldEndPt = new Point(((int)oldEnd.getX() / 10), ((int)oldEnd.getY() / 10));
    //Point2D oldEndPtD = new Point2D.Double(oldEndPt.x, oldEndPt.y);
    Vector2D branchVec = new Vector2D(oldEnd, newEnd);
    
    ArrayList<Point> ignorePts = new ArrayList<Point>();
    int backNum = backBranchPts.size();
    for (int i = 0; i < backNum; i++) {
      Point2D ignoreBack = backBranchPts.get(i);
      Point2D ignorePt = branchVec.add(ignoreBack);
      ignorePts.add(new Point(((int)ignorePt.getX() / 10), ((int)ignorePt.getY() / 10)));    
    }
    
    Point2D startPtD = new Point2D.Double(startPt.x, startPt.y);
    Point2D newEndPtD = new Point2D.Double(newEndPt.x, newEndPt.y);
    Point chkPt = new Point();
    
    Vector2D runVec = new Vector2D(start, newEnd);
    runVec = runVec.normalized();
    WalkValues walk = getWalkValues(runVec, startPtD, newEndPtD, 1);    
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        chkPt.setLocation(x, y);
        if (chkPt.equals(startPt)) {
          continue;
        }
        if (ignorePts.contains(chkPt)) {
          continue;
        }
        GridContents gc = getGridContents(x, y);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
        if (currentCrossings < 0) {
          return (false);
        }
      }
    }

    //
    // Figure out the points that begin solitary away branches.
    // Paths must be clear for these newly extended solitary away branches:
    //
    //
    
    int awayNum = awayBranchPts.size();
    for (int i = 0; i < awayNum; i++) {
      Point2D away = awayBranchPts.get(i);
      if (backBranchPts.contains(away)) {
        continue;
      }
      Point2D newAwayStart = branchVec.add(away);
      Point loneAwayPt = new Point(((int)away.getX() / 10), ((int)away.getY() / 10));
      Point newAwayStartPt = new Point(((int)newAwayStart.getX() / 10), ((int)newAwayStart.getY() / 10));
      Point2D loneAwayPtD = new Point2D.Double(loneAwayPt.x, loneAwayPt.y);
      Point2D newAwayStartPtD = new Point2D.Double(newAwayStartPt.x, newAwayStartPt.y);
      Vector2D awayRunVec = new Vector2D(newAwayStart, away);
      awayRunVec = awayRunVec.normalized();
      walk = getWalkValues(awayRunVec, newAwayStartPtD, loneAwayPtD, 1);    
      for (int x = walk.startX; x != walk.endX; x += walk.incX) {
        for (int y = walk.startY; y != walk.endY; y += walk.incY) {
          chkPt.setLocation(x, y);
          if (chkPt.equals(newAwayStartPt) || chkPt.equals(loneAwayPt)) {
            continue;
          }
          GridContents gc = getGridContents(x, y);
          int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
          if (currentCrossings < 0) {
            return (false);
          }
        }
      }
    } 
    return (true);
  }

  /***************************************************************************
  **
  ** Handle rectangle conversion
  */
  
  public static Rectangle rectConversion(Rectangle orig) { 
    return (new Rectangle(orig.x / 10, orig.y / 10, orig.width / 10, orig.height / 10));
  }
   
  /***************************************************************************
  **
  ** Handle point conversion
  */
  
  public static void pointConversion(Point2D orig, Point2D newPt2D, Point newPt) { 
    newPt.setLocation((int)orig.getX() / 10, (int)orig.getY() / 10);
    if (newPt2D != null) {
      newPt2D.setLocation(newPt.x, newPt.y);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Convert a list of points
  */
  
  private void pointListConversion(List<Point2D> orig, List<Point2D> newPts2D, List<Point> newPts) {
    int size = orig.size();
    for (int i = 0; i < size; i++) {
      Point2D origPt = orig.get(i);
      Point newPt = new Point(((int)origPt.getX() / 10), ((int)origPt.getY() / 10));
      newPts.add(newPt);
      newPts2D.add(new Point2D.Double(newPt.x, newPt.y));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if given plan for orthogonalization is allowed
  */
  
  public boolean orthoPlanAllowed(GenomeSource gSrc, String src, LinkSegment segGeom,
                                  String trg, Set<String> okGroups, 
                                  boolean isStart, boolean isEnd) {
      
    
    //
    // Convert points to grid coords, and create double and integer versions
    //
      
    ArrayList<Point2D> segCorners = new ArrayList<Point2D>();
    segCorners.add(segGeom.getStart());
    segCorners.add(segGeom.getEnd());
    ArrayList<Point> orthoPathCornerPts = new ArrayList<Point>();
    ArrayList<Point2D> orthoPathCornersPtDs = new ArrayList<Point2D>();
    pointListConversion(segCorners, orthoPathCornersPtDs, orthoPathCornerPts);

    //
    // Check that the displaced run is free of conflicts:
    //   
 
    Point startPt = orthoPathCornerPts.get(0);
    Point endPt = orthoPathCornerPts.get(1);
    if (startPt.equals(endPt)) {
      return (true);
    }
    Point2D startPtD = orthoPathCornersPtDs.get(0);
    Point2D endPtD = orthoPathCornersPtDs.get(1);
    Point chkPt = new Point();    
    Vector2D runVec = new Vector2D(startPtD, endPtD);
    runVec = runVec.normalized();
    if (!runVec.isCanonical()) {  // Not straight?  That's OK!
      return (true);
    }
    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);    
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {    
        chkPt.setLocation(x, y);
        GridContents gc = getGridContents(x, y);     
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
        //
        // Standard test is for < 0, but -2 case (approaching a target landing but encountering
        // a target grid entry) crops up when testing segments into a signal chevron.  Since ortho
        // currently should not encounter this because the inbound direction must match the 
        // landing pad approach vector, we should be able to accept the -2 case as OK. [WRONG! See
        // below.  Need to catch that case separately.] Otherwise, we fail on orthogonalizing drops 
        // into signal node targets.
        if (currentCrossings == -1) {
          return (false);
        }
        //
        // The above statement is wrong!  Allowing -2 permits links to e.g. pass vertically upward through
        // a gene landing pad, then turning around 180 degrees and landing.  This is a kinda bogus last-minute
        // fix for V6 release.  FIX ME!
        //
        if (currentCrossings == -2) {
          Node node = gSrc.getGenome().getNode(GenomeItemInstance.getBaseID(trg));
          if (node.getNodeType() != Node.INTERCELL) {
            return (false);
          }
        }
      }
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Answer if the point is in a node (where inbound is NOT OK)
  */
  
  public boolean pointInsideNode(Point pt, String nodeID) {
    
    GridContents gc = getGridContents(pt);
    if (gc == null) {
      return (false);
    }
  
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.type == IS_NODE) { 
        return (true);
      } else {
        return (false);
      }
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_NODE) {
        return (true);
      }
    }
    return (false);  
  }
 
  /***************************************************************************
  **
  ** Return the bounds of exploration for the given test.
  */
  
  public Rectangle displacementRange(Point2D start, Point2D end,
                                     Vector2D displacement, List<Point2D> shorterStarts, 
                                     List<Point2D> longerLoneStarts) {
    //
    // Convert points to grid coords, and create double and integer versions
    //
                                       
    Point startPt = new Point();
    Point2D startPtD = new Point2D.Double();
    pointConversion(start, startPtD, startPt);

    Point endPt = new Point();
    Point2D endPtD = new Point2D.Double();
    pointConversion(end, endPtD, endPt);
    
    ArrayList<Point> shorterStartPts = new ArrayList<Point>();
    ArrayList<Point2D> shorterStartPtDs = new ArrayList<Point2D>();
    pointListConversion(shorterStarts, shorterStartPtDs, shorterStartPts);

    ArrayList<Point> longerLoneStartPts = new ArrayList<Point>();
    ArrayList<Point2D> longerLoneStartPtDs = new ArrayList<Point2D>();
    pointListConversion(longerLoneStarts, longerLoneStartPtDs, longerLoneStartPts);
  
    MinMax xRange = new MinMax();
    MinMax yRange = new MinMax();
    xRange.init();
    yRange.init();
       
    Vector2D runVec = new Vector2D(startPtD, endPtD);
    runVec = runVec.normalized();
    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);
    xRange.update(walk.startX);
    xRange.update(walk.endX);
    yRange.update(walk.startY);
    yRange.update(walk.endY);
    
    Vector2D scaledDisplacement = displacement.scaled(.10);
    Vector2D normDisplacement = displacement.normalized();
    int longerNum = longerLoneStartPtDs.size();
    for (int i = 0; i < longerNum; i++) {
      Point2D loneStartD = longerLoneStartPtDs.get(i);
      Point2D loneEndD = scaledDisplacement.add(loneStartD);
      walk = getWalkValues(normDisplacement, loneStartD, loneEndD, 1);
      xRange.update(walk.startX);
      xRange.update(walk.endX);
      yRange.update(walk.startY);
      yRange.update(walk.endY);
    }
    Rectangle retval = new Rectangle(xRange.min, yRange.min, 
                                     xRange.max - xRange.min, 
                                     yRange.max - yRange.min);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if given link lateral displacement is allowed.
  */
  
  public boolean displacementAllowed(String src, Point2D start, Point2D end,
                                     Vector2D displacement, String trg,
                                     Set<String> okGroups,
                                     List<Point2D> shorterStarts,
                                     List<Point2D> longerLoneStarts, boolean isStart) {
                                  
    //
    // The line between the start and the newEnd must be OK for a run, ignoring
    // given points already being occupied by branches being shortened.
    // The lines belonging to branches being lengthened need to be OK for a run
    // also; these only need checking if they do not have a corresponding shorter
    // run on the opposite side
    //

    //
    // Convert points to grid coords, and create double and integer versions
    //
                                       
    Point startPt = new Point();
    Point2D startPtD = new Point2D.Double();
    pointConversion(start, startPtD, startPt);

    Point endPt = new Point();
    Point2D endPtD = new Point2D.Double();
    pointConversion(end, endPtD, endPt);
    
    ArrayList<Point> shorterStartPts = new ArrayList<Point>();
    ArrayList<Point2D> shorterStartPtDs = new ArrayList<Point2D>();
    pointListConversion(shorterStarts, shorterStartPtDs, shorterStartPts);

    ArrayList<Point> longerLoneStartPts = new ArrayList<Point>();
    ArrayList<Point2D> longerLoneStartPtDs = new ArrayList<Point2D>();
    pointListConversion(longerLoneStarts, longerLoneStartPtDs, longerLoneStartPts);
     
    //
    // Check that the displaced run is free of conflicts:
    //
    
    Point chkPt = new Point();    
    Vector2D runVec = new Vector2D(startPtD, endPtD);
    runVec = runVec.normalized();
    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);    
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        chkPt.setLocation(x, y);
        if (chkPt.equals(startPt) || chkPt.equals(endPt)) {
          continue;
        }
        if (shorterStartPts.contains(chkPt)) {
          continue;
        }
        GridContents gc = getGridContents(x, y);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
        if (currentCrossings < 0) {
          return (false);
        }
      }
    }
   
    //
    // Make sure that newly extended branch point paths are clear too:
    //

    Vector2D scaledDisplacement = displacement.scaled(.10);
    Vector2D normDisplacement = displacement.normalized();
    int longerNum = longerLoneStartPtDs.size();
    Point endChk = new Point();
    for (int i = 0; i < longerNum; i++) {
      Point2D loneStartD = longerLoneStartPtDs.get(i);
      Point2D loneEndD = scaledDisplacement.add(loneStartD);
      Point loneStart = longerLoneStartPts.get(i);
      endChk.setLocation(loneEndD.getX(), loneEndD.getY());
      walk = getWalkValues(normDisplacement, loneStartD, loneEndD, 1);    
      for (int x = walk.startX; x != walk.endX; x += walk.incX) {
        for (int y = walk.startY; y != walk.endY; y += walk.incY) {
          chkPt.setLocation(x, y);
          // Skipping the end results in single-grid point link overlap bugs:
          if (chkPt.equals(loneStart)) { //  || chkPt.equals(endChk)) {
            continue;
          }
          GridContents gc = getGridContents(x, y);
          int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
          if (currentCrossings < 0) {
            return (false);
          }
        }
      }
    } 
    return (true);
  }  

  /***************************************************************************
  **
  ** Answer if given link lateral displacement is allowed.
  */
  
  public boolean routeAllowed(String src, String trg, Point2D start, 
                              Point2D end, Set<String> okGroups, boolean isStart) {
                                  
    //
    // The line between the start and the newEnd must be OK for a run, ignoring
    // given points already being occupied by branches being shortened.
    // The lines belonging to branches being lengthened need to be OK for a run
    // also; these only need checking if they do not have a corresponding shorter
    // run on the opposite side
    //

    //
    // Convert points to grid coords, and create double and integer versions
    //
                                       
    Point startPt = new Point();
    Point2D startPtD = new Point2D.Double();
    pointConversion(start, startPtD, startPt);

    Point endPt = new Point();
    Point2D endPtD = new Point2D.Double();
    pointConversion(end, endPtD, endPt);
    
     
    //
    // Check that the displaced run is free of conflicts:
    //
    
    Point chkPt = new Point();    
    Vector2D runVec = new Vector2D(startPtD, endPtD);
    runVec = runVec.normalized();
    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);    
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        chkPt.setLocation(x, y);
        if (chkPt.equals(startPt) || chkPt.equals(endPt)) {
          continue;
        }
        GridContents gc = getGridContents(x, y);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, false);
        if (currentCrossings < 0) {
          return (false);
        }
      }
    }

    return (true);
  }  

  
  /***************************************************************************
  **
  **  Get a measure of the link crossings for the source.  Not as efficient as
  ** the following version, but this works if you dop not have a starting point.
  */
  
  public int getLinkCrossingsForSource(String srcID) {
    Iterator<Point> kit = new PatternIterator(false, false);
    int retval = 0;
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if (lookForSource(srcID, gval)) {
        retval += otherLinkCount(srcID, gval);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get a measure of the link crossings for the source
  */

  public int getLinkCrossingsForSource(String src, Point2D pt) {
    Point start = new Point(((int)pt.getX() / 10), ((int)pt.getY() / 10));    
    int retval = 0;
    Set<Point> allCells = getAllLinkCells(src, start);
    Iterator<Point> cellit = allCells.iterator();
    while (cellit.hasNext()) {
      Point cpt = cellit.next();
      GridContents gc = getGridContents(cpt);
      retval += otherLinkCount(src, gc);    
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get all adjacent points for links
  */
  
  private void getNextPointsForEntry(GridEntry entry, Point currPt, Set<Point> points) {
    if ((entry.exits & NORTH) != 0x00) {
      points.add(new Point(currPt.x, currPt.y - 1));
    }
    if ((entry.exits & SOUTH) != 0x00) {
      points.add(new Point(currPt.x, currPt.y + 1));
    }
    if ((entry.exits & EAST) != 0x00) {
      points.add(new Point(currPt.x + 1, currPt.y));
    }
    if ((entry.exits & WEST) != 0x00) {
      points.add(new Point(currPt.x - 1, currPt.y));
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Get all link cells in the run
  */
  
  public void getNextPointsForContents(String srcID, GridContents gc, Point currPt, Set<Point> points) {
    if (gc == null) {
      return;
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.id.equals(srcID) && 
          ((gc.entry.type != IS_NODE) && (gc.entry.type != IS_NODE_INBOUND_OK))) {
        getNextPointsForEntry(gc.entry, currPt, points);
      }
      return;
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.id.equals(srcID) && 
          ((currEntry.type != IS_NODE) && (currEntry.type != IS_NODE_INBOUND_OK))) {
        getNextPointsForEntry(currEntry, currPt, points);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get all link cells in the grid
  */
  
  public Set<Point> getAllLinkCells(String srcID, Point start) {
    HashSet<Point> retval = new HashSet<Point>();
    getAllLinkCellsRecursive(srcID, start, retval);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** recursive cell gathering for link
  */
  
  private void getAllLinkCellsRecursive(String srcID, Point start, Set<Point> retval) {
    GridContents gc = getGridContents(start);
    if (!lookForSource(srcID, gc)) {
      // FIX ME: Unexpected non-link cell (BT-05-05-05:16)
      // Maybe solved (in some cases) by fixing BT-06-29-05:2?
      return;
    }
    retval.add(start);
    HashSet<Point> nextPoints = new HashSet<Point>();
    getNextPointsForContents(srcID, gc, start, nextPoints);
    Iterator<Point> npit = nextPoints.iterator();
    while (npit.hasNext()) {
      Point pt = npit.next();
      if (retval.contains(pt)) {
        continue;
      }
      getAllLinkCellsRecursive(srcID, pt, retval);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Draw the link path to the grid, from points
  */
  
  private void drawLinkFromPoints(String linkSrc, List<Point2D> pointList, CornerOracle corc, Set<String> linkIDs) { 
    GeneralPath myPath = new GeneralPath();
    Iterator<Point2D> sit = pointList.iterator();
    boolean isFirst = true;
    Point2D last = null;
    while (sit.hasNext()) {
      Point2D pt = sit.next();
      if (!isFirst) {  
        myPath.moveTo((float)last.getX(), (float)last.getY());
        myPath.lineTo((float)pt.getX(), (float)pt.getY());
      }
      isFirst = false;
      last = pt;
    }
    drawLink(linkSrc, myPath, corc, linkIDs);
    return;
  }
 
  /***************************************************************************
  **
  ** Draw the link path to the grid
  */
  
  public void drawLinkWithUserData(String linkSrc, DecoratedPath path, CornerOracle corc, Set<String> linkIDs) {
    drawLinkGuts(linkSrc, path.getPath(), corc, path.getData(), linkIDs);
    return;
  }

  /***************************************************************************
  **
  ** Draw the link path to the grid
  */
  
  public void drawLink(String linkSrc, GeneralPath path, CornerOracle corc, Set<String> linkIDs) {
    drawLinkGuts(linkSrc, path, corc, null, linkIDs);
    return;    
  }
  
  /***************************************************************************
  **
  ** Draw the link path to the grid
  */
  
  private void drawLinkGuts(String linkSrc, GeneralPath path, CornerOracle corc, Map<Point2D, Object> dataMap, Set<String> linkIDs) {
    double[] coords = new double[6];
    double[] origCurr = new double[2];
    double[] origNew = new double[2];
    double[] forcedCurr = new double[] {Double.MAX_VALUE, Double.MAX_VALUE};
    double[] forcedNew = new double[2]; 
    DecoInfo startData = null;
    Point2D dataKey = new Point2D.Double(0.0, 0.0);
    PathIterator pit = path.getPathIterator(null);
    while (!pit.isDone()) {
      int segType = pit.currentSegment(coords);
      switch (segType) {
        case PathIterator.SEG_MOVETO:
          origCurr[0] = coords[0];
          origCurr[1] = coords[1];
          forcedCurr[0] = UiUtil.forceToGridValue(coords[0], UiUtil.GRID_SIZE);
          forcedCurr[1] = UiUtil.forceToGridValue(coords[1], UiUtil.GRID_SIZE);
          if (dataMap != null) {
            dataKey.setLocation(origCurr[0], origCurr[1]);
            startData = (DecoInfo)dataMap.get(dataKey);
          }
          break;
        case PathIterator.SEG_LINETO:
          origNew[0] = coords[0];
          origNew[1] = coords[1];
          forcedNew[0] = UiUtil.forceToGridValue(coords[0], UiUtil.GRID_SIZE);
          forcedNew[1] = UiUtil.forceToGridValue(coords[1], UiUtil.GRID_SIZE);
          padTweaks(origCurr, origNew, forcedCurr, forcedNew);
          fillForLink(linkSrc, forcedCurr[0], forcedCurr[1], forcedNew[0], forcedNew[1], corc, startData);
          origCurr[0] = origNew[0];
          origCurr[1] = origNew[1];          
          forcedCurr[0] = forcedNew[0];
          forcedCurr[1] = forcedNew[1];
          break;
        default:
          throw new IllegalArgumentException();
      }
      pit.next();
    }
    drawnLinks_.addAll(linkIDs);
    return;    
  }
  
  /***************************************************************************
  **
  ** Tweak ends for pads.  If pads are not on grid, we want to shorten the link
  ** instead of having it too long and collide with other pads on the node.
  ** FIX ME!!! No adjustment is occuring if link is coming in at a weird angle.
  ** Need to know approach direction for each pad to do this correctly
  */
  
  public void padTweaks(double[] origCurr, double[] origNew, 
                        double[] forcedCurr, double[] forcedNew) {
    
    int direction = getDirectionFromCoords(forcedCurr[0], forcedCurr[1], forcedNew[0], forcedNew[1]);
    switch (direction) {
      case NONE:
        return;
      case WEST:
        if (forcedCurr[0] > origCurr[0]) {
          forcedCurr[0] -= UiUtil.GRID_SIZE;
        }
        if (forcedNew[0] < origNew[0]) {
          forcedNew[0] += UiUtil.GRID_SIZE;
        }
        break;
      case SOUTH:
        if (forcedCurr[1] < origCurr[1]) {
          forcedCurr[1] += UiUtil.GRID_SIZE;
        }
        if (forcedNew[1] > origNew[1]) {
          forcedNew[1] -= UiUtil.GRID_SIZE;
        }
        break;
      case EAST:
        if (forcedCurr[0] < origCurr[0]) {
          forcedCurr[0] += UiUtil.GRID_SIZE;
        }
        if (forcedNew[0] > origNew[0]) {
          forcedNew[0] -= UiUtil.GRID_SIZE;
        }
        break;        
      case NORTH:
        if (forcedCurr[1] > origCurr[1]) {
          forcedCurr[1] -= UiUtil.GRID_SIZE;
        }
        if (forcedNew[1] < origNew[1]) {
          forcedNew[1] += UiUtil.GRID_SIZE;
        }
        break;
      case DIAGONAL:
        return;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Check that the link path is allowed
  */
  
  public boolean canDrawLink(String linkSrc, LinkProperties props, 
                             Map<PointPair, List<String>> linkMap, Map<String, String> targMap, Map<String, GenomeInstance.GroupTuple> tupMap) {
    Iterator<PointPair> pit = linkMap.keySet().iterator();
    while (pit.hasNext()) {
      PointPair pair = pit.next();
      if (pair.end == null) {
        continue;
      }
      if (!canFillForLink(linkSrc, pair.start.getX(), pair.start.getY(), 
                          pair.end.getX(), pair.end.getY(), props, 
                          linkMap, targMap, tupMap)) {
        return (false);
      }
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Add a node to the grid.  Second pattern is the allowed inbound zone)
  */
  
  public void addNode(Pattern pat, Pattern inbound, String id, int px, int py, int strictness) {
    int maxX = pat.getWidth();
    int maxY = pat.getHeight();

    for (int x = 0; x < maxX; x++) {
      for (int y = 0; y < maxY; y++) {
        if (pat.getValue(x, y) != null) {
          GridEntry entry;
          if ((inbound != null) && (inbound.getValue(x, y) != null)) {
            entry = new GridEntry(id, IS_NODE_INBOUND_OK, NONE, NONE, false);
          } else {
            entry = new GridEntry(id, IS_NODE, NONE, NONE, false);
          }
          int xval = px + x;
          int yval = py + y;
          // If we have to, we will overwrite the existing contents to force
          // a link placement
          //  if (strictness == INodeRenderer.LAX) ... FIX ME: Lax add Node!         
          installModifiedGridContents(xval, yval, entry);
        }
      }
    }    
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a group to the grid.  Hacktastic: USED FOR MODULES AS WELL!!!!!
  */
  
  public void addGroup(Rectangle rect, String groupID) {
    Rectangle use = null;
    if (bounds_ != null) {  
      Rectangle2D choppedPart = rect.createIntersection(bounds_);
      if ((choppedPart.getHeight() > 0.0) && (choppedPart.getWidth() > 0.0)) {
        use = UiUtil.rectFromRect2D(choppedPart);
      }      
    } else {
      use = (Rectangle)rect.clone();
    }

    if (use != null) {
      groups_.put(groupID, use);
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Reserve a terminal region
  */
  
  public void reserveTerminalRegion(TerminalRegion termReg, String tag, boolean multiArrive) {

    int tagVal;
    if (termReg.type == TerminalRegion.DEPARTURE) {
      tagVal = IS_RESERVED_DEPARTURE_RUN;
    } else {
      tagVal = (multiArrive) ? IS_RESERVED_MULTI_LINK_ARRIVAL_RUN : IS_RESERVED_ARRIVAL_RUN; 
    }
        
    int numPt = termReg.getNumTravelPoints();
    if (numPt == 0) {
      return;
    }
        
    for (int i = 0; i < numPt; i++) {
      Point pt = termReg.getTravelPoint(i);
      GridEntry entry = new GridEntry(tag, tagVal, getReverse(termReg.direction), 
                                      termReg.direction, false);
      installModifiedGridContents(pt, entry);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Answer if we can reserve a terminal region
  */
  
  public boolean terminalRegionCanBeReserved(TerminalRegion termReg) {

    int numPt = termReg.getNumTravelPoints();
    if (numPt == 0) {
      return (true);
    }
        
    for (int i = 0; i < numPt; i++) {
      Point pt = termReg.getTravelPoint(i);
      GridContents gc = getGridContents(pt);
      if (gc == null) {
        continue;
      } else if (!isOKForTerminal(gc)) {
        return (false);
      }
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Get a valid departure region
  */
  
  public TerminalRegion getDepartureRegion(String id, Vector2D vec, 
                                           Set<String> okGroups, // null, or preloaded with source group
                                           Point start, int max) {
    //
    // Starting at the pad, move out in the given direction and record valid first
    // corner points, up to the given maximum (if possible)
    //
    
    //
    // We ignore run matches for our source, as they are spurious.  We can cross over
    // over other runs orthogonally, and valid corner points must be empty.  Once we hit
    // another non-source node cell, we cannot go any further.
    //
    
 
    WalkValues walk = getWalkValues(vec, start, null, 1);
    
     //
    // This is the way we gather up, and allow, other groups that our target happens to be
    // sitting on top of:
    //
    
    if (okGroups != null) {
      int grSlot = 0;
      int genResult = OK;
      for (int x = walk.startX; (x != walk.endX && (grSlot < max)); x += walk.incX) {
        for (int y = walk.startY; (y != walk.endY && (grSlot < max)); y += walk.incY) {
          GridContents gc = getGridContents(x, y);
          okGroups.addAll(getCellGroupContents(gc));
          // Need this to prevent infinite looping, but generated info is useless:
          genResult = generateDeparture(gc, id, null, walk);
          if (genResult == STOP) {
            grSlot = max;
          } else {
            grSlot++;
          }
        }
      }
    }
    
    boolean[] slots = new boolean[max];
    int currSlot = 0;
       
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        GridContents gc = getGridContents(x, y);
        int genResult = generateDeparture(gc, id, okGroups, walk);
        if (genResult == STOP) {
          return (new TerminalRegion(id, start, slots, walk.exitDirection, TerminalRegion.DEPARTURE)); 
        } else if (genResult == OK) {
          slots[currSlot++] = true;
        } else if (genResult == CONTINUE) {
          currSlot++;
        } else {
          throw new IllegalStateException();
        }
        if (currSlot == max) {
          return (new TerminalRegion(id, start, slots, walk.exitDirection, TerminalRegion.DEPARTURE));
        }
      }
    }

    return (new TerminalRegion(id, start, slots, walk.exitDirection, TerminalRegion.DEPARTURE));
  } 

  /***************************************************************************
  **
  ** Get a valid arrival region
  */
  
  public TerminalRegion getArrivalRegion(String srcID, String targID, String linkID,
                                         Set<String> okGroups, // null, or preloaded with the link tuple!
                                         Vector2D vec, Point end, int max) {
    //
    // Starting at the pad, move out in the given direction and record valid first
    // corner points, up to the given maximum (if possible)
    //
    
    //
    // We ignore run matches for our source, as they are spurious.  We can cross over
    // over other runs orthogonally, and valid corner points must be empty.  Once we hit
    // another non-source node cell, we cannot go any further.
    //
    
   
    WalkValues walk = getWalkValues(vec, end, null, -1);
    
    //
    // This is the way we gather up, and allow, other groups that our target happens to be
    // sitting on top of:
    //
    
    if (okGroups != null) {
      int grSlot = 0;
      int genResult = OK;
      for (int x = walk.startX; (x != walk.endX && (grSlot < max)); x += walk.incX) {
        for (int y = walk.startY; (y != walk.endY && (grSlot < max)); y += walk.incY) {
          GridContents gc = getGridContents(x, y);
          okGroups.addAll(getCellGroupContents(gc));
          // Need this to prevent infinite looping, but generated info is useless:
          genResult = generateArrival(gc, srcID, targID, linkID, null, walk);
          if (genResult == STOP) {
            grSlot = max;
          } else {
            grSlot++;
          }
        }
      }
    }
  
    boolean[] slots = new boolean[max];
    int currSlot = 0;
    
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        GridContents gc = getGridContents(x, y);
        int genResult = generateArrival(gc, srcID, targID, linkID, okGroups, walk);
        if (genResult == STOP) {
          return (new TerminalRegion(srcID, end, slots, walk.entryDirection, TerminalRegion.ARRIVAL)); 
        } else if (genResult == OK) {
          slots[currSlot++] = true;
        } else if (genResult == CONTINUE) {
          currSlot++;
        } else {
          throw new IllegalStateException();
        }
        if (currSlot == max) {
          return (new TerminalRegion(srcID, end, slots, walk.entryDirection, TerminalRegion.ARRIVAL));
        }
      }
    }
    return (new TerminalRegion(srcID, end, slots, walk.entryDirection, TerminalRegion.ARRIVAL));
  }
  
  /***************************************************************************
  **
  ** Get a valid arrival region for "emergency" conditions, e.g. the best we can do
  ** is a nearby point.
  */
  
  public TerminalRegion getEmergencyArrivalRegion(String srcID, String targID, String linkID,
                                                  Set<String> okGroups, Vector2D vec, Point end) {
    //
    // Starting at the pad, move out in the given direction and record valid first
    // corner points, up to the given maximum (if possible)
    //
    
    //
    // We ignore run matches for our source, as they are spurious.  We can cross over
    // over other runs orthogonally, and valid corner points must be empty.  Once we hit
    // another non-source node cell, we cannot go any further.
    //
    
    boolean[] slots = new boolean[1];
    // Just use to generate the direction:
    WalkValues walk = getWalkValues(vec, end, null, -1);
    GridContents gc = getGridContents(end.x, end.y);
    
    if (okGroups != null) {
      okGroups.addAll(getCellGroupContents(gc));
    }
              
    if (canGenerateEmergencyArrival(gc, srcID, targID, linkID, okGroups, walk.entryDirection)) {
      slots[0] = true;
      return (new TerminalRegion(srcID, end, slots, walk.entryDirection, TerminalRegion.ARRIVAL)); 
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Get a valid track.  If no track can be found, returns an empty list.
  */

  public List<Point2D> recoverValidFirstTrack(String src, String trg, String linkID, 
                                              TerminalRegion departure, TerminalRegion arrival,
                                              LinkPlacementGrid.GoodnessParams goodness, 
                                              Set<String> okGroups, boolean force, 
                                              int entryPref, RecoveryDataForLink recoverForLink, 
                                              Map<String, RecoveryDataForSource> recoveryDataMap,
                                              Map<String, TerminalRegion> departures, Map<String, TerminalRegion> arrivals, 
                                              Set<String> unseenLinks, boolean strictOKGroups) {
       
    Set<String> exemptions = recoverForLink.getEnclosingSource().getExemptions();
    
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    gauss_.clear();
    goodness.differenceNormalize = 1.0 / gaussian(0.0, goodness.differenceSigma);
    goodness.normalize = 1.0;
    goodness.normalize = 1.0 / goodness(goodness, 1.0, 0, 0); 
    IterationChecker checker = new IterationChecker(100);
    
    Point start = departure.start; 
    if (arrival.getNumPoints() == 0) {  // when does this happen?
      return (retval);
    }
    Point end = arrival.getPoint(0);
       
    List<Point2D> threePts = buildRecoverPointList(start, end, recoverForLink, 0);
    if (threePts == null) {
      return (null);
    }
       
    List<PlanOrPoint> tempMasks = temporaryMaskForRecovery(src, recoveryDataMap, departures, arrivals, unseenLinks);
      
    Point2D startPt = threePts.get(0);
    Point2D endPt = threePts.get(1);  
    Vector2D departDir = (new Vector2D(startPt, endPt)).normalized();
    Vector2D arriveDir = arrival.getTerminalVector();
 
    try {
      // Want init recovery turn to reflect non-orthogonal situation, but the start direction
      // sent down needs to be orthogonal:
      TravelTurn initTurn = buildInitRecoveryTurn(departDir, threePts);
      Vector2D canStart = (departDir.isCanonical()) ? departDir : departDir.canonical().normalized();
      int maxDepth = recoverForLink.getPointCount() + 15;
      int rsval = recoverStep(src, trg, linkID, new Point2D.Double(end.x, end.y), canStart, arriveDir,            
                              initTurn, retval, maxDepth, 0, goodness, checker, true,
                              force, okGroups, entryPref, recoverForLink, exemptions, null, strictOKGroups);
      if (rsval == RECOV_STEP_CORE_OK_) {
        clearRecoveryMasks(tempMasks);
        //  checkForTemps();
        convertTerminalRegion(departure);
        convertTerminalRegion(arrival);                        
        commitTemps(linkID);
        recoverForLink.commitAllRevisedPoints();  
        return (retval);
      } else if (rsval == RECOV_STEP_EMBEDDED_JUMP_FAILED_) {
        clearRecoveryMasks(tempMasks);
        recoverForLink.popAllRevisedPoints();
        return (retval);
      }
    } catch (NonConvergenceException ncex) {
      if (checker.limitExceeded()) {
        // do nothing special at the moment
      }
    }
    clearRecoveryMasks(tempMasks);
    recoverForLink.popAllRevisedPoints();

    return (retval);
  }
 
  /***************************************************************************
  **
  ** Clear temp entries for all links not yet recovered
  */

  private void clearRecoveryMasks(List<PlanOrPoint> masks) {    
    int numSkips = masks.size();
    for (int i = numSkips - 1; i >= 0; i--) {
      PlanOrPoint pop = masks.get(i);
      if (pop.plan != null) {
        removeTempForPlan(pop.plan);
      } else {
        removeTemp(pop.point, true);
      }
    }
    return;
  } 

  /***************************************************************************
  **
  ** Install temp entries for all links not yet recovered
  */

  private List<PlanOrPoint> temporaryMaskForRecovery(String src,
                                                     Map<String, RecoveryDataForSource> recoveryDataMap,
                                                     Map<String, TerminalRegion> departures, Map<String, TerminalRegion> arrivals, 
                                                     Set<String> unseenLinks) { 
    
    ArrayList<PlanOrPoint> tempPlans = new ArrayList<PlanOrPoint>();  
    Iterator<String> rdmkit = recoveryDataMap.keySet().iterator();
    while (rdmkit.hasNext()) { 
      String srcKey = rdmkit.next();
      if (srcKey.equals(src)) {
        continue;
      }
 
      HashSet<PointPair> seenPairs = new HashSet<PointPair>();
      RecoveryDataForSource rdfs = recoveryDataMap.get(srcKey);
      Iterator<String> linkit = rdfs.segListPerLink.keySet().iterator();
      while (linkit.hasNext()) { 
        String linkID = linkit.next();
        if (!unseenLinks.contains(linkID)) {
          continue;
        }
        RecoveryDataForLink rdfl = new LinkPlacementGrid.RecoveryDataForLink(rdfs, linkID);
        TerminalRegion arrive = arrivals.get(linkID);
        Point end = new Point(10000, 16234); // Bogus...
        if (arrive == null) {
          // FIX ME!  This should NOT happen!
        } else {        
          end = arrive.start; 
        }  
        int currDepth = 0;
        Point start = null;
        TerminalRegion depart = departures.get(linkID);
        if (depart == null) {
          currDepth = 1;
        } else {        
          start = depart.start; 
        }        
        List<Point2D> tempoPts = buildRecoverPointList(start, end, rdfl, currDepth++);
        while ((tempoPts != null) && (tempoPts.size() >= 2)) {
          TravelPlan tempPlan = null;
          Point2D tmp0 = tempoPts.get(0);
          Point2D tmp1 = tempoPts.get(1);
          PointPair chkKey = new PointPair(tmp0, tmp1);
          if (!seenPairs.contains(chkKey)) {
            if (recoverPointListStartsOrtho(tempoPts)) {
              Vector2D turnVector = null;
              if (recoverPointListIsOrtho(tempoPts) && (tempoPts.size() == 3)) {
                Point2D tmp2 = tempoPts.get(2);
                if (!tmp1.equals(tmp2)) {
                  turnVector = (new Vector2D(tmp1, tmp2)).normalized();
                }
              }
              tempPlan = new TravelPlan(tmp0, tmp1, null, new Vector2D(tmp0, tmp1).normalized(), turnVector);           
              addTempForPlan(tempPlan, srcKey, false, null);
              tempPlans.add(new PlanOrPoint(tempPlan, null));
            } else {
              addTempEntryForDiagonalCorner(tmp1, srcKey);
              tempPlans.add(new PlanOrPoint(null, tmp1));
            }
          }
          seenPairs.add(chkKey);
          tempoPts = buildRecoverPointList(start, end, rdfl, currDepth++);
        }
      }
    }
    return (tempPlans);
  }
  
  /***************************************************************************
  **
  ** Get a valid track.  If no track can be found, returns an empty list.
  */

  public NewCorner recoverValidAddOnTrack(DataAccessContext icx, String src, String trg, String linkID,
                                          TerminalRegion arrival, List<Point2D> points, 
                                          LinkPlacementGrid.GoodnessParams goodness,
                                          Set<String> okGroups, 
                                          boolean force, int entryPref, 
                                          RecoveryDataForLink recoverForLink, 
                                          Map<String, RecoveryDataForSource> recoveryDataMap,
                                          Map<String, TerminalRegion> departures, Map<String, TerminalRegion> arrivals, 
                                          Set<String> unseenLinks,
                                          Set<String> linksFromSource, 
                                          boolean strictOKGroups) {

 
    
    if ((arrival == null) || (arrival.getNumPoints() == 0)) {
      return (null);
    }
    Point end = arrival.getPoint(0);
    Point2D endPt = new Point2D.Double(end.getX(), end.getY());
    Point start = null;
    
    gauss_.clear();
    goodness.differenceNormalize = 1.0 / gaussian(0.0, goodness.differenceSigma);
    goodness.normalize = 1.0;
    goodness.normalize = 1.0 / goodness(goodness, 1.0, 0, 0); 

    List<PlanOrPoint> tempMasks = temporaryMaskForRecovery(src, recoveryDataMap, departures, arrivals, unseenLinks);    
    RecoveryDataForSource rds = recoveryDataMap.get(src);
    
    Set<String> exemptions = recoverForLink.getEnclosingSource().getExemptions();
     
    int numExist = recoverForLink.getPointCount();
    Point testPt = new Point();
    Point2D testPt2D = new Point2D.Double();
    HashSet<LinkSegmentID> noRecovery = new HashSet<LinkSegmentID>();
    for (int i = numExist - 1; i >= 0; i--) {
      LinkSegmentID lsid = recoverForLink.getPointKey(i);
      RecoveryPointData rpd = recoverForLink.getEnclosingSource().getRpd(lsid);
      if (rpd == null) {
        continue;  // FIX ME! WHY?
      }
      pointConversion(rpd.point, testPt2D, testPt);
      if (!isReachable(testPt2D, src, trg, linkID, null, exemptions, false)) {  // Ignore tup for recovery...
        noRecovery.add(lsid);
      }
    }
    Iterator<LinkSegmentID> nrit = noRecovery.iterator();
    while (nrit.hasNext()) {
      LinkSegmentID lsid = nrit.next();
      RecoveryPointData rpd = recoverForLink.getEnclosingSource().getRpd(lsid);
      rpd.tagAsUnreachable();
    }  
    
    //
    // Walk up our existing point list until we find a launch cell.  In cases where that is not going to work (large scale
    // layout changes) we fall back on finding the closest point we can use:
    //
    
    HashSet<Point> ignore = new HashSet<Point>();
    LaunchAnalysis la = null;
    PointAlternativeForRecovery lastDitch = null;
    PointAlternative nextTry = null;
    List<PointAlternativeForRecovery> pafrList = getPointAlternativesForRecovery(src, arrival, rds, recoverForLink, linksFromSource);
    
    int index = -1;
    if (pafrList == null) {
      LinkSegmentID srcBound = recoverForLink.getSourceBoundPoint();
      
      index = recoverForLink.getSourceBoundIndex();
      if (index != -1) {
        Point2D targPt = recoverForLink.getPoint(srcBound);
        Point dummyPt = new Point();
        Point2D targPt2D = new Point2D.Double();
        pointConversion(targPt, targPt2D, dummyPt);
        la = generateLaunchAlternatives(icx, targPt2D, src, linkID, okGroups, unseenLinks, linksFromSource);
        nextTry = la.extractNextAlternative();
      }
      if (nextTry == null) {
        lastDitch = lastDitchForRecovery(src, linkID, icx.getLayout(), arrival, okGroups, recoverForLink, linksFromSource, 0);
      }
    }
    
    //
    // If we have ntohing, we are hosed:
    //
   
    if ((pafrList == null) && (nextTry == null) && (lastDitch == null)) {
      clearRecoveryMasks(tempMasks);
      return (null);
    }

    int pafrIndex = 0;
    int maxIndex = (pafrList == null) ? 0 : pafrList.size();
    Vector2D arriveDir = arrival.getTerminalVector();
       
    while (true) {
      IterationChecker checker = new IterationChecker(100);
      List<Point2D> threePts = null;
      RecoveryDataForLink truncatedRFL = null;      
      int type = IS_EMPTY;
      int bestDir = NONE;
      boolean letsTry = false;
      
      if ((pafrList != null) || (lastDitch != null)) { 
        PointAlternativeForRecovery pafr = (pafrList != null) ? pafrList.get(pafrIndex) : lastDitch;
        type = pafr.pa.type;
        start = pafr.pa.point;
        bestDir = pafr.pa.dir;
        end = pafr.end;
 
        threePts = pafr.threePts;
        truncatedRFL = pafr.truncatedRFL;
        letsTry = true;
        
      } else if (nextTry != null) {
        type = nextTry.type;
        start = nextTry.point; 
        //
        // Build up a set of nearby alternatives and choose the best one.  Note that if no alternatives
        // appear, we add all the possibilities to the ignore list.  If something appears, we will
        // probably be back again to check out most of these alternatives after the one failed candidate
        // has been added to the ignore list.
        //

        Set<PointAlternative> nearby = getRunAlternatives(src, start, type, ignore);
        PointAlternative choose = chooseBestAlternative(src, trg, linkID, end, nearby, start, type, okGroups, false);
        if (choose == null) {
          Iterator<PointAlternative> nit = nearby.iterator();
          while (nit.hasNext()) {
            PointAlternative pit = nit.next();
            ignore.add((Point)pit.point.clone());
          }        
        } else {
          type = choose.type;
          start = choose.point;
          bestDir = choose.dir;
          Point2D shifted = new Point2D.Double(choose.point.getX(), choose.point.getY());
          truncatedRFL = new RecoveryDataForLink(recoverForLink, index);  
          threePts = buildRecoverPointList(shifted, endPt, truncatedRFL, 0);
          if (threePts != null)  {
            letsTry = true;
          }
        }
      } 
      //
      // We have our next point to work with.  Go for it:
      //
      
      if (letsTry) {      
        Vector2D departDir = getVector(bestDir);
        Point2D scaleStart = new Point2D.Double(start.getX(), start.getY());
        scaleStart = departDir.add(scaleStart);   

        try {
          // Note init recovery turn reflects non-orthogonal situation     
          TravelTurn initTurn = buildInitRecoveryTurn(departDir, threePts);
          int maxDepth = truncatedRFL.getPointCount() + 15;
          // We do not draw the start corner except on first link from source due to isStart == false
          int rsval = recoverStep(src, trg, linkID, new Point2D.Double(end.x, end.y), departDir, arriveDir, 
                                  initTurn, points, maxDepth, 0, goodness, checker, false,
                                  force, okGroups, entryPref, truncatedRFL, exemptions, null, strictOKGroups);
          if (rsval == RECOV_STEP_CORE_OK_) {
            clearRecoveryMasks(tempMasks);
            //  checkForTemps();
            convertTerminalRegion(arrival);
            addTempEntryForRun(scaleStart, src, bestDir);
            commitTemps(linkID);
            recoverForLink.commitAllRevisedPoints();
            return (new NewCorner(type, new Point2D.Double(start.x * 10.0, start.y * 10.0), bestDir));
          } else if (rsval == RECOV_STEP_EMBEDDED_JUMP_FAILED_) {
            // recoverForLink.popAllRevisedPoints();
          }
        } catch (NonConvergenceException ncex) {
          if (checker.limitExceeded()) {
             // Nothing special...
          }
        }
        recoverForLink.popAllRevisedPoints();
      }
      
      //
      // Increment the search:
      //
      
      if (pafrList != null) {
        if (++pafrIndex >= maxIndex) {
          break;
        }
      } else if (lastDitch != null) {
        break;       
      } else {
        nextTry = la.extractNextAlternative();
        if (nextTry == null) {
          break;
        }
      }
    }
        
    clearRecoveryMasks(tempMasks);
   // checkForTemps();
 
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get a list of point alternatives for recovery
  */

  private List<PointAlternativeForRecovery> getPointAlternativesForRecovery(String src,
                                                                            TerminalRegion arrival,
                                                                            RecoveryDataForSource rds,
                                                                            RecoveryDataForLink recoverForLink, 
                                                                            Set<String> linksFromSource) { 

    //
    // Walk up our existing point list until we find a launch cell:
    //
      
    ArrayList<PointAlternativeForRecovery> retval = new ArrayList<PointAlternativeForRecovery>();
    if ((arrival == null) || (arrival.getNumPoints() == 0)) {
      return (null);
    }
    
    Point end = arrival.getPoint(0);
    Point2D endPt = new Point2D.Double(end.getX(), end.getY());

    int numExist = recoverForLink.getPointCount();
    Point testPt = new Point();
    Point2D testPt2D = new Point2D.Double();   
    Vector2D lastRun = null;
    Point2D lastPoint = endPt;
    
    for (int i = numExist - 1; i >= 0; i--) {
      pointConversion(recoverForLink.getPoint(i), testPt2D, testPt);
      GridContents gval = getGridContents(testPt);
      if (gval != null) {
        int cellType = lookForLaunchCell(src, gval, null, linksFromSource);  // tup is ignored for recovery...
        if (cellType != IS_EMPTY) {
          Vector2D departDir;
          if (lastRun == null) {
            departDir = (new Vector2D(testPt2D, endPt)).normalized();
          } else {  
            departDir = lastRun;
          }
          if (departDir.isZero()) {
            lastRun = recoverForLink.getInboundRun(i);
            lastPoint = (Point2D)testPt2D.clone();   
            continue;
          }
          Vector2D canonDepart = (isOrthogonal(departDir)) ? departDir : getClosestCanonical(departDir);
          PointAlternative pa = new PointAlternative(testPt, cellType, getDirection(canonDepart));
          int entryForCell = getEntryDirectionForSrc(testPt, src);
          //
          // If we have found a place to launch from, we need to make sure
          // that the new link is not overlaying a jump or DOF extension of
          // the original geometry.
          // 
          List<PointAlternative> launchOptions = modifiedRecoveryLaunch(pa, entryForCell, departDir, src, rds, linksFromSource, lastPoint);
          if ((launchOptions == null) || launchOptions.isEmpty()) {
            return (null);
          }
          int numOptions = launchOptions.size();
          for (int j = 0; j < numOptions; j++) {
            PointAlternative npa = launchOptions.get(j);
            RecoveryDataForLink truncatedRFL;
            Point2D shifted = null;
            if (npa.point.equals(testPt)) {
              shifted = testPt2D;
              truncatedRFL = new RecoveryDataForLink(recoverForLink, i + 1);  
            } else {
              shifted = new Point2D.Double(npa.point.getX(), npa.point.getY());
              truncatedRFL = new RecoveryDataForLink(recoverForLink, i + 1);  
            }
            List<Point2D> threePts = buildRecoverPointList(shifted, endPt, truncatedRFL, 0);
            if (threePts != null)  {
              retval.add(new PointAlternativeForRecovery(npa, threePts, end, truncatedRFL));
            }
          }
          return ((retval.isEmpty()) ? null : retval);
        } else if (linkIsPresentAtPoint(src, testPt)) {  // No!  best to chop to make a point?
          return (null);
        }
      }
      lastRun = recoverForLink.getInboundRun(i);
      lastPoint = (Point2D)testPt2D.clone();
    }
    return (null);       
  }
  
  /***************************************************************************
  **
  ** Complete failure looking for an attachment for recovery; build an alternative
  ** that starts at the root
  */
  
  private PointAlternativeForRecovery lastDitchForRecovery(String src, String linkID, Layout lo,
                                                           TerminalRegion arrival, Set<String> okGroups, 
                                                           RecoveryDataForLink recoverForLink, Set<String> linksFromSource, int chopIndex) {  
        
    BusProperties bp = lo.getLinkProperties(linkID);
    if (bp.isDirect()) {
      return (null);
    }
    
    LinkSegment rseg = bp.getRootSegment();
    PointAlternative pa = degeneratePoint(rseg, src, okGroups, linksFromSource); 
    if (pa == null) {
      return (null);
    }
    List<Vector2D> exits = getPossibleExits(pa.point);
    if (exits.isEmpty()) {
      return (null);
    }
    Vector2D useVec = exits.get(0);  // FIX ME get the best, not the first...
    pa.dir = getDirection(useVec);
    Point end = arrival.getPoint(0); 
    Point2D endPt = new Point2D.Double(end.getX(), end.getY());  
    RecoveryDataForLink truncatedRFL = new RecoveryDataForLink(recoverForLink, chopIndex + 1);  
    Point2D strtPt = new Point2D.Double(pa.point.getX(), pa.point.getY());
    List<Point2D> threePts = buildRecoverPointList(strtPt, endPt, truncatedRFL, 0);
    if (threePts == null)  {
      return (null);
    }     
    return (new PointAlternativeForRecovery(pa, threePts, end, truncatedRFL));
  }  

  /***************************************************************************
  **
  ** Get a valid track.  If no track can be found, returns an empty list.
  */

  public List<Point2D> getValidFirstTrack(String src, String trg, String linkID, 
                                          TerminalRegion departure, TerminalRegion arrival,
                                          LinkPlacementGrid.GoodnessParams goodness, 
                                          Set<String> okGroups, boolean force, int entryPref, Set<String> linksFromSource) {
    //
    // Pick the first departure and arrival points.  Get the vector arrival - departure,
    // and take the dot product.  Choose the direction with the most positive dot product.
    // If product is zero, we only go the minimum amount before we can turn in a positive
    // direction.  If running in a positive direction, go as far as possible in that direction
    // until that dimension difference is swallowed.  If we then can only approach in the
    // invalid direction, back up until we can approach. We can never approach in the wrong
    // direction through target node space.  Then turn and approach.
    //
    
    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    int numDep = departure.getNumPoints();
    int numArr = arrival.getNumPoints();
    if ((numDep == 0) || (numArr == 0)) {     
      return (retval);
    }
    gauss_.clear();
    goodness.differenceNormalize = 1.0 / gaussian(0.0, goodness.differenceSigma);
    goodness.normalize = 1.0;
    goodness.normalize = 1.0 / goodness(goodness, 1.0, 0, 0); 
    IterationChecker checker = new IterationChecker(100);
    
    List<StartIndex> bestStarts = pickStartIndex(departure, arrival);
    int numStart = bestStarts.size();
    for (int i = 0; i < numStart; i++) {
      StartIndex sp = bestStarts.get(i);
      Point start = departure.getPoint(sp.index);
      Point end = arrival.getPoint(0);
      Vector2D departDir = sp.direction;
      Vector2D arriveDir = arrival.getTerminalVector();

      try {
        TravelTurn initTurn = buildInitTurn(src, start, departDir, trg, end, linkID, okGroups);
        if (jumpStep(src, trg, linkID, new Point2D.Double(end.x, end.y), getVector(departure.direction), arriveDir, 
                     initTurn, retval, 15, 0, goodness, checker, true, force, okGroups, entryPref, null)) {
          // Get the first corner added (end of departure run) (FIXME: maybe only do this if we turn here?)
          retval.add(0, new Point2D.Double(start.getX() * 10.0, start.getY() * 10.0));
          int cleanVal = cleanupRoughDraft(retval, 
                                           new Point2D.Double(departure.start.getX() * 10.0, departure.start.getY() * 10.0),
                                           true, src, trg, okGroups, departure, arrival, null, linksFromSource, linkID);
          if (cleanVal == NO_CLEANUP_) {
            convertTerminalRegion(departure);
            convertTerminalRegion(arrival);             
            commitTemps(linkID);      
          }
          return (retval);
        }
      } catch (NonConvergenceException ncex) {
        if (checker.limitExceeded()) {
          break;
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** What we get out of the link layout is a rough draft based on a greedy algorithm
  ** that does not backtrack if there is no failure.  Do some cleanup on the result:
  */

  private int cleanupRoughDraft(List<Point2D> retval, Point2D start, boolean isFromPad,
                                String src, String trg, Set<String> okGroups, 
                                TerminalRegion depart, TerminalRegion arrive, 
                                NewCorner changedStart, Set<String> linksFromSource, String linkID) {
   
    ArrayList<Point2D> chopped = new ArrayList<Point2D>();
    int retType = chopLoops(retval, start, chopped);
    if (retType != NO_CLEANUP_) {
      retval.clear();
      retval.addAll(chopped);
    }
    List<Point2D> shorter = retval;
    while (shorter != null) {
      shorter = chopToShorterBackward(retval, start, src, trg, okGroups);
      if (shorter != null) {
        retType = (retType == NO_CLEANUP_) ? DID_CLEANUP_ : retType;
        retval.clear();
        retval.addAll(shorter);
      }
    }
    List<Point2D> shorterF = retval;
    while (shorterF != null) {    
      shorterF = chopToShorterForward(retval, start, src, trg, okGroups);
      if (shorterF != null) {
        retType = (retType == NO_CLEANUP_) ? DID_CLEANUP_ : retType;
        retval.clear();
        retval.addAll(shorterF);
      }
    }
    
    if (!isFromPad) {
      List<Point2D> shorterS = lookForAlternateLaunch(retval, start, src, trg, okGroups, changedStart, linksFromSource);
      if (shorterS != null) {
        retType = (retType == NO_CLEANUP_) ? DID_CLEANUP_ : retType;
        retval.clear();
        retval.addAll(shorterS);
 
      }
    }

    if (retType != NO_CLEANUP_) {
      clearTemps();
      if (depart != null) {
        dropTerminalRegion(depart);
      }
      dropTerminalRegion(arrive);
      Point2D strt = (!isFromPad) ? start : null;
      Point2D end = new Point2D.Double(arrive.start.getX() * 10.0, arrive.start.getY() * 10.0);
      MyCornerOracle mco = new MyCornerOracle(strt, end);
      ArrayList<Point2D> drawList = new ArrayList<Point2D>(retval);
      drawList.add(0, start);
      drawList.add(end);
      HashSet<String> linkIDs = new HashSet<String>();
      linkIDs.add(linkID);
      drawLinkFromPoints(src, drawList, mco, linkIDs);
    }
    return (retType);
  }  
  
  /***************************************************************************
  **
  ** Get a measure of the total length of links for a source.
  */

  public InkValues getLinkInk(String src) {
    int inkCount = 0;
    int simpleBendCount = 0;
    int complexCount = 0;
    Iterator<GridContents> vit = new PatternValueIterator(false);
    while (vit.hasNext()) {
      GridContents gc = vit.next();
      if (lookForSource(src, gc)) {    
        inkCount++;
        int ctype = cornerType(gc);
        if (ctype == SINGLE_BEND_CORNER) {
          simpleBendCount++;
        } else if (ctype == COMPLEX_CORNER) {
          complexCount++;
        }
      }
    }
    return (new InkValues(inkCount, simpleBendCount, complexCount));
  }
   
  /***************************************************************************
  **
  ** Get a valid track.  If no track can be found, returns an empty list.
  */

  public NewCorner getValidAddOnTrack(DataAccessContext icx, String src, String trg, String linkID, 
                                      TerminalRegion arrival, List<Point2D> points, 
                                      LinkPlacementGrid.GoodnessParams goodness,
                                      Set<String> okGroups, 
                                      boolean force, int entryPref,
                                      Set<String> omitted, Set<String> linksFromSource) {
    
    HashSet<Point> ignore = new HashSet<Point>();
    Point start = new Point();
    int numArr = (arrival == null) ? 0 : arrival.getNumPoints();
    if (numArr == 0) {     
      return (null);
    }
    gauss_.clear();
    goodness.differenceNormalize = 1.0 / gaussian(0.0, goodness.differenceSigma);
    goodness.normalize = 1.0;
    goodness.normalize = 1.0 / goodness(goodness, 1.0, 0, 0);         
    Point end = arrival.getPoint(0);
    Point2D targPt = new Point2D.Double(end.x, end.y);
    
    LaunchAnalysis la = generateLaunchAlternatives(icx, targPt, src, linkID, okGroups, omitted, linksFromSource);
    
    while (true) {    
      PointAlternative pa = la.extractNextAlternative();
      if (pa == null) {
        return (null);
      }
      int type = pa.type;
      start = pa.point;
      IterationChecker checker = new IterationChecker(25);
 
      //
      // Build up a set of nearby alternatives and choose the best one.  Note that if no alternatives
      // appear, we add all the possibilities to the ignore list.  If something appears, we will
      // probably be back again to check out most of these alternatives after the one failed candidate
      // has been added to the ignore list.
      //
      
      Set<PointAlternative> nearby = getRunAlternatives(src, start, type, ignore);
      PointAlternative choose = chooseBestAlternativeCloseToTarg(src, trg, linkID, end, nearby, start, type, okGroups, false);
      if (choose == null) {
        Iterator<PointAlternative> nit = nearby.iterator();
        while (nit.hasNext()) {
          PointAlternative pit = nit.next();
          ignore.add((Point)pit.point.clone());
        }        
        continue;
      }
      
      BusProperties bp = icx.getLayout().getLinkProperties(linkID);
      Point2D chooseConv = new Point2D.Double(choose.point.getX() * 10.0, choose.point.getY() * 10.0);
      LinkProperties.DistancedLinkSegID dlsegID = bp.intersectBusSegment(icx, chooseConv, omitted, 5.0);    
      if (dlsegID == null) {  // May happen if we have serious mismatch of grid and reality!
        // FIX ME: Why would this happen?
      }
        
      start = choose.point;
      type = choose.type;
      
      //
      // Figure out the direction we are heading: to start
      //
      
      Point2D scaleStart = new Point2D.Double(start.getX(), start.getY());
      int bestDir = choose.dir;
      Vector2D arriveDir = arrival.getTerminalVector();
      Vector2D departDir = getVector(bestDir);
      scaleStart = departDir.add(scaleStart);
      
      try {
        TravelTurn initTurn = buildInitTurn(src, scaleStart, departDir, trg, end, linkID, okGroups);
        if (jumpStep(src, trg, linkID, new Point2D.Double(end.x, end.y), departDir, arriveDir, 
                      initTurn, points, 15, 0, goodness, checker, false, force, okGroups, entryPref, null)) {
          NewCorner changedStart = new NewCorner(type, null, bestDir);
          if (points.size() == 0) {  // First guy was the target
            points.add(0, new Point2D.Double(scaleStart.getX() * 10.0, scaleStart.getY() * 10.0));
          }
          int cleanVal = cleanupRoughDraft(points, new Point2D.Double(start.x * 10.0, start.y * 10.0), false,
                                           src, trg, okGroups, null, arrival, changedStart, linksFromSource, linkID);
          if (cleanVal == NO_CLEANUP_) {
            convertTerminalRegion(arrival);
            // FIX ME!  If added before the jumpStep call, we get deflected immediately!
            addTempEntryForRun(scaleStart, src, bestDir);
            commitTemps(linkID);
          }
          if (changedStart.point == null) {
            return (new NewCorner(type, new Point2D.Double(start.x * 10.0, start.y * 10.0), bestDir));
          } else {
            changedStart.point = new Point2D.Double(changedStart.point.getX() * 10.0, changedStart.point.getY() * 10.0);
            return (changedStart);
          }
        }
      } catch (NonConvergenceException ncex) {
        if (checker.limitExceeded()) {
          // Nothing special...
        }
      }
      //removeTemp(scaleStart);     
      // NOTE that alternate directions out of the same point are being ignored!
      ignore.add((Point)start.clone());
      //
      // Magic number.  15 worked OK in region-free cases, but with regions,
      // we need to move big distances to get around large region blockades.
      // WJRL 5/1/08: Even 40 produces problems where the closest segments
      // are buried in very dense layout.  We exhaust our tries trying to 
      // get out of the dense region, instead of jumping to a new region.
      //
      if (ignore.size() > 80) {
        return (null);
      }
    }
  }
  
  /***************************************************************************
  **
  ** Generate launch alternatives. 
  */

  private LaunchAnalysis generateLaunchAlternatives(DataAccessContext icx, Point2D targPt, String src, String linkID, Set<String> okGroups,                                                
                                                    Set<String> omitted, Set<String> linksFromSource) {
    //
    // Find the link segments from the source that we are happy with (placed, orthogonal).
    // Rank by distance, within directional bins (N/E/S/W).  For each segment, we can 
    // find the outbound link count.  If 3+, it is not an alternative.  We can also
    // find the "keep going" direction that maintains a single straight run; that is the
    // much preferred direction. For the segment, the target is either mid span or off 
    // an end.
    //

    BusProperties bp = icx.getLayout().getLinkProperties(linkID);
    
    Set<String> candidateStarts = icx.getGenome().getOutboundLinks(src);
    candidateStarts.removeAll(omitted);
    
    Set<LinkSegmentID> candSegs = new HashSet<LinkSegmentID>();
    LinkSegmentID degen = null;
    Iterator<String> csit = candidateStarts.iterator();
    while (csit.hasNext()) {
      String candID = csit.next();
      List<LinkSegmentID> segsForLink = bp.getBusLinkSegmentsForOneLink(icx, candID);
      // Should always be a segment, since we are working with links following crude auto-add!
      int numsfl = segsForLink.size();
      for (int i = 0; i < numsfl; i++) {
        LinkSegmentID segID = segsForLink.get(i);
        if (!segID.isForSegment()) {
          continue;
        }
        LinkSegment seg = bp.getSegment(segID);
        if (seg.isDegenerate()) {
          degen = segID;
        } else if (seg.isOrthogonal()) {
          candSegs.add(segID);
        }
      }
    }
    if (candSegs.isEmpty()) {
      candSegs.add(degen);
    }  
    LaunchAnalysis la = analyzeSegs(targPt, candSegs, bp, src, okGroups, linksFromSource);   
    return (la);
  }  
 
  /***************************************************************************
  **
  ** Analyze and bin the segments
  */
  
  private LaunchAnalysis analyzeSegs(Point2D targPt, Set<LinkSegmentID> candSegs, BusProperties bp, 
                                     String srcID, Set<String> okGroups, Set<String> linksFromSource) { 
 
    //
    // Best: closest corner with a (free) travel vector from source that is the best dot
    // product to us.  If we are "close" to the midpoint of a segment, then an orthogonal
    // vector from that closest point is best:
    // 
    
    Integer noKey = new Integer(NONE);
    LaunchAnalysis retval = new LaunchAnalysis();
    
    Iterator<LinkSegmentID> csit = candSegs.iterator();
    while (csit.hasNext()) {
      LinkSegmentID segID = csit.next();
      LinkSegment seg = bp.getSegment(segID);
      PointAlternative pa;
      if (seg.isDegenerate()) {
        pa = degeneratePoint(seg, srcID, okGroups, linksFromSource);
      } else {
        pa = closestPoint(targPt, seg, srcID, okGroups, linksFromSource);
      }
      if (pa == null) {
        continue;
      }
      Point2D usePt = new Point2D.Double(pa.point.x, pa.point.y);
      Vector2D trav = new Vector2D(usePt, targPt);
      Double sidt = new Double(trav.length());
      Integer binKey = getVectorBin(trav);
      if (binKey.equals(noKey)) {
        continue;
      }
      SortedMap<Double, List<PointAlternative>> distForBin = retval.binnedResults.get(binKey);
      if (distForBin == null) {
        distForBin = new TreeMap<Double, List<PointAlternative>>();
        retval.binnedResults.put(binKey, distForBin);
      }
      List<PointAlternative> ptsForDist = distForBin.get(sidt);
      if (ptsForDist == null) {
        ptsForDist = new ArrayList<PointAlternative>();
        distForBin.put(sidt, ptsForDist);
      }      
      ptsForDist.add(pa);
    } 
    
    TreeMap<Double, Integer> buildOrder = new TreeMap<Double, Integer>();
    Iterator<Integer> brkit = retval.binnedResults.keySet().iterator();
    while (brkit.hasNext()) {
      Integer binKey = brkit.next();
      SortedMap<Double, List<PointAlternative>> distForBin = retval.binnedResults.get(binKey);
      Double minDist = distForBin.firstKey();
      buildOrder.put(minDist, binKey);
    }
    
    retval.bestOrder.addAll(buildOrder.values());
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Get the point for a degenerate.  Has to be launchable.
  */
  
  private PointAlternative degeneratePoint(LinkSegment seg, String srcID, Set<String> okGroups, Set<String> linksFromSource) {   
    Point2D startPt = seg.getStart();  
    Point griddedStart = new Point();
    Point2D griddedStart2D = new Point2D.Double();
    pointConversion(startPt, griddedStart2D, griddedStart);
    GridContents gval = getGridContents(griddedStart);
    int cellType = lookForLaunchCell(srcID, gval, okGroups, linksFromSource);
    if ((cellType != IS_EMPTY) && (!notOverMyGenePad(srcID, null, griddedStart2D))) {
      return (new PointAlternative(griddedStart, cellType, NONE));
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Get the closest point on the segment.  Has to be launchable.
  */
  
  private PointAlternative closestPoint(Point2D targPt, LinkSegment seg, String srcID, 
                                        Set<String> okGroups, Set<String> linksFromSource) {   
    Point2D startPt = seg.getStart();  
    Point2D endPt = seg.getEnd();
    Point griddedStart = new Point();
    Point griddedEnd = new Point();    
    Point2D griddedStart2D = new Point2D.Double();
    Point2D griddedEnd2D = new Point2D.Double();        
    pointConversion(startPt, griddedStart2D, griddedStart);
    pointConversion(endPt, griddedEnd2D, griddedEnd);
    Vector2D trav = seg.getRun();
    if (trav.isZero()) {
      return (null);
    }
    WalkValues walk = getWalkValues(trav, griddedStart2D, griddedEnd2D, 1);
    
    double minDist = Double.POSITIVE_INFINITY;
    PointAlternative minPt = null;
    Point2D testPt = new Point2D.Double();
    Point keyPt = new Point();
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        testPt.setLocation(x, y);
        keyPt.setLocation(x, y);
        GridContents gval = getGridContents(keyPt);
        if (gval == null) {
          continue;  // Should not happen
        }
        int cellType = lookForLaunchCell(srcID, gval, okGroups, linksFromSource);
        if (cellType != IS_EMPTY) {
          if (notOverMyGenePad(srcID, null, testPt)) {
            continue;
          }
          Vector2D checkVec = new Vector2D(testPt, targPt);
          double checkDist = checkVec.length();
          if (checkDist < minDist) {
            minDist = checkDist;
            if (minPt == null) {
              minPt = new PointAlternative((Point)keyPt.clone(), cellType, NONE);
            } else {
              minPt.point.setLocation(testPt);
              minPt.type = cellType;
            }
          }
        }
      }
    }
    return (minPt);
  }
 
  /***************************************************************************
  **
  ** Modify a recovery launch if needed to account for jump/DOF adjustments.
  */
  
  private List<PointAlternative> modifiedRecoveryLaunch(PointAlternative orig, int entryForCell, 
                                                        Vector2D outbound, String srcID, 
                                                        RecoveryDataForSource rds, Set<String> linksFromSource,
                                                        Point2D nextPoint) {
    
    
    //
    // If we have encountered an original point, head out in our original direction.
    // If we do not, move along the segment to the end, then see if our original
    // direction can be accomodated.  If not, extend the new segment instead.
    
    ArrayList<PointAlternative> retval = new ArrayList<PointAlternative>();
    if (entryForCell == NONE) {
      retval.add(orig);
      return (retval);
    }
    
    if (!outbound.isCanonical()) {
      outbound = outbound.canonical();
    }
    outbound = outbound.normalized();  
    
    //
    // If the alternative is a run point, we need to follow the run to the end and return it.
  
    
    Point2D startPt = orig.point;
    if (orig.type == IS_RUN) {
      int exitForCell = GridEntry.getCardinalOppositeDirection(entryForCell);
      Vector2D currentRun = getVector(exitForCell);
      List<PointAlternative> runPAs = walkForModifiedRecoveryLaunchOption(orig, currentRun, startPt, 
                                                                          exitForCell, outbound, srcID, 
                                                                          linksFromSource, nextPoint);
      if ((runPAs != null) && !runPAs.isEmpty()) {
        retval.addAll(runPAs);
      }
      return (retval);
    }
    
    //
    // FIX ME!!!!!!!!  Ordering of which points get used should be based on a measure
    // of goodness!
    //
    
    
    //
    // If our corner turns out to have jump-in or jump out alternatives, try those as points
    // too:
    //     
    
    Point2D jumped = rds.getJumpIntoPoint(orig.point);
    if (jumped != null) {
      PointAlternative jiPA = modifiedRecoveryLaunchOption(jumped, outbound, srcID, linksFromSource);
      if (jiPA != null) {
        retval.add(jiPA);
      }   
    }
   
    List<Point2D> jumpedFrom = rds.getJumpFromPoints(orig.point);
    if (jumpedFrom != null) {
      int numJf = jumpedFrom.size();
      for (int i = 0; i < numJf; i++) {
        Point2D jumpedPoint = jumpedFrom.get(i);
        PointAlternative joPA = modifiedRecoveryLaunchOption(jumpedPoint, outbound, srcID, linksFromSource);
        if (joPA != null) {
          retval.add(joPA);
        }
      }
    }
    
    //
    // We are a corner.  Check that we can exit in the preferable direction.  If not,
    // try another
    //
        
    Point chkPt = new Point((int)startPt.getX(), (int)startPt.getY());
    List<Vector2D> exits = getPossibleExits(chkPt);
    boolean canGoOutbound = exits.contains(outbound);
    if (!canGoOutbound) {
      Vector2D tryMe = new Vector2D(startPt, nextPoint);
      Vector2D tryMeBest = getClosestCanonical(tryMe);
      if (exits.contains(tryMeBest)) {
        retval.add(new PointAlternative((Point)chkPt.clone(), orig.type, getDirection(tryMeBest)));
      }
      Vector2D tryMeAlso = getSecondClosestCanonical(tryMe);     
      if (exits.contains(tryMeAlso)) {
        retval.add(new PointAlternative((Point)chkPt.clone(), orig.type, getDirection(tryMeAlso)));
      }
    } else {
      retval.add(orig);
    }    

    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Modify a recovery launch if needed to account for jump/DOF adjustments.
  */
  
  private List<PointAlternative> walkForModifiedRecoveryLaunchOption(PointAlternative orig, Vector2D currentRun, 
                                                                     Point2D startPt, int exitForCell, 
                                                                     Vector2D outbound, String srcID, 
                                                                     Set<String> linksFromSource, Point2D nextPoint) {
    
    WalkValues walk = getWalkValues(currentRun, startPt, null, 1);

    //
    // Go in the direction of the existing link until you fall off the end (or
    // hit the previous test point):
    //
    
    ArrayList<PointAlternative> retval = new ArrayList<PointAlternative>();
    
    PointAlternative retvalOne = orig;
    PointAlternative retvalZero = null;
    Point keyPt = new Point();
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        keyPt.setLocation(x, y);
        if ((x == walk.startX) && (y == walk.startY)) {
          // If the point we have is a straight shot, add it to the list, else skip it...
          List<Vector2D> exits = getPossibleExits(keyPt);
          boolean canGoOutbound = exits.contains(outbound);
          if (canGoOutbound) {          
            Vector2D toNext = (new Vector2D(keyPt, nextPoint)).normalized();
            if (toNext.equals(outbound)) {
              retvalZero = new PointAlternative((Point)keyPt.clone(), IS_RUN, getDirection(outbound));            
            }
          }
          continue;
        }      
        GridContents gval = getGridContents(keyPt);
        if (gval == null) {  // fall off the end
          if (retvalZero != null) retval.add(retvalZero);
          if (retvalOne != null) retval.add(retvalOne);
          return (retval);
        }
        int cellType = lookForLaunchCell(srcID, gval, null, linksFromSource);  // recovery doesn't care about tup status
        if (cellType != IS_EMPTY) {
          List<Vector2D> exits = getPossibleExits(keyPt);
          boolean canGoOutbound = exits.contains(outbound);
          if (canGoOutbound) {          
            Vector2D toNext = (new Vector2D(keyPt, nextPoint)).normalized();
            if (toNext.equals(outbound)) {
              retvalZero = new PointAlternative((Point)keyPt.clone(), cellType, getDirection(outbound));            
            }
          }
          int exitDir = (canGoOutbound) ? getDirection(outbound) : exitForCell;
          retvalOne = new PointAlternative((Point)keyPt.clone(), cellType, exitDir);
        } else if (!linkIsPresentAtPoint(srcID, keyPt)) {
          if (retvalZero != null) retval.add(retvalZero);
          if (retvalOne != null) retval.add(retvalOne);
          return (retval);
        } else {
          retvalOne = null;
        }
      }
    }
    if (retvalZero != null) retval.add(retvalZero);
    if (retvalOne != null) retval.add(retvalOne);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Modify a recovery launch if needed to account for a jump alternative:
  */
  
  private PointAlternative modifiedRecoveryLaunchOption(Point2D startPt, Vector2D outbound, String srcID, Set<String> linksFromSource) {
       
    Point keyPt = new Point((int)startPt.getX(), (int)startPt.getY());
    GridContents gval = getGridContents(keyPt);
    if (gval == null) {  // should not happen...
      return (null);
    }
    int cellType = lookForLaunchCell(srcID, gval, null, linksFromSource);  // recovery doesn't care about tup status
    if (cellType != IS_EMPTY) {
      List<Vector2D> exits = getPossibleExits(keyPt);
      int entryForCell = getEntryDirectionForSrc(keyPt, srcID);
      int straightExit = GridEntry.getCardinalOppositeDirection(entryForCell);
      int exitDir;
      if (exits.contains(outbound)) {
        exitDir = getDirection(outbound);
      } else if (exits.contains(getVector(straightExit))) { 
        exitDir = straightExit;
      } else if (exits.size() > 0) {
        exitDir = getDirection(exits.get(0));
      } else {
        return (null);
      }
      PointAlternative retval = new PointAlternative(keyPt, cellType, exitDir);
      return (retval);
    }
    return (null);
  }
 
  /***************************************************************************
  **
  ** Convert a run grid entry to a corner
  */

  public void convertRunToCorner(Point2D split, String src, int dir) { 
    Point address = new Point(((int)split.getX() / 10), ((int)split.getY() / 10));
    GridContents gc = getGridContents(address);
    GridEntry entry = extractTargetedEntry(src, gc, IS_RUN);
    entry.type = IS_CORNER;
    entry.exits |= dir;
    writeBackModifiedGridContents(address, gc);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a new direction entry for an existing grid corner
  */

  public void addCornerDirection(Point2D split, String src, int dir) {  
    Point address = new Point(((int)split.getX() / 10), ((int)split.getY() / 10));
    GridContents gc = getGridContents(address);
    GridEntry entry = extractTargetedEntry(src, gc, IS_CORNER);   
    entry.exits |= dir;
    writeBackModifiedGridContents(address, gc);
    return;
  }  

  /***************************************************************************
  **
  ** Extract the xxx entry from a possible xxx/yyy mixture
  */

  private GridEntry extractTargetedEntry(String src, GridContents gc, int type) {  
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (!gc.entry.id.equals(src) || (gc.entry.type != type)) {
        throw new IllegalArgumentException();
      }
      return (gc.entry);
    } 
       
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_GROUP) {
        continue;
      } 
      if (currEntry.id.equals(src) && (currEntry.type == type)) {
        return (currEntry);
      } 
    }
    
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the grid contents
  */
  
  private GridContents getGridContents(Point cellPt) {
    return (getGridContents(cellPt, true));
  }
    
  /***************************************************************************
  **
  ** Get the grid contents
  */
  
  private GridContents getGridContents(int x, int y) {   
    return (getGridContents(new Point(x, y)));
  }
  
  /***************************************************************************
  **
  ** Get the grid contents, can skip groups if desired
  */
  
  private GridContents getGridContents(Point cellPt, boolean needGroups) {
    GridContents gc = patternx_.get(cellPt);
    if (gc != null) {
      gc = gc.clone();
    }
    if (needGroups) {
      List<GridEntry> groupEntries = generateGroups(cellPt);
      gc = foldInGroups(groupEntries, gc);
    }
    return (gc);
  }  
  
  /***************************************************************************
  **
  ** Drop cell from grid
  */
  
  private void dropFromPattern(Point cellPt) { 
    patternx_.remove(cellPt);
    return;  
  }
  
  /***************************************************************************
  **
  ** Generate group grid entries for a particular point
  */
  
  private List<GridEntry> generateGroups(Point pt) {
    ArrayList<GridEntry> retval = new ArrayList<GridEntry>();   
    Iterator<String> git = groups_.keySet().iterator();
    while (git.hasNext()) {
      String groupID = git.next();
      Rectangle rect = groups_.get(groupID);
      int grpX = rect.x / 10;
      int grpY = rect.y / 10;
      int grpW = rect.width / 10;
      int grpH = rect.height / 10;
      if ((pt.x >= grpX) && (pt.y >= grpY) && (pt.x < (grpX + grpW)) && (pt.y < (grpY + grpH))) {
        GridEntry entry = new GridEntry(groupID, IS_GROUP, NONE, NONE, false);
        retval.add(entry);
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Fold in group entries
  */
  
  private GridContents foldInGroups(List<GridEntry> groupEntries, GridContents gc) {
    int numGrp = groupEntries.size();
    for (int i = 0; i < numGrp; i++) {
      GridEntry entry = groupEntries.get(i);  
      if (gc == null) {
        gc = new GridContents(entry);
      } else {
        gc.mergeContents(entry);
      }
    }    
    return (gc);
  } 

  /***************************************************************************
  **
  ** Fold in group entries
  */  
   
  private boolean dropGroupEntries(GridContents gc) {
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.type == IS_GROUP) { 
        return (true);
      }
      return (false);
    }
    
    int size = gc.extra.size();
    for (int i = size - 1; i >= 0; i--) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_GROUP) { 
        gc.extra.remove(i);
      }
    }
    
    size = gc.extra.size();
    if (size == 0) {
      return (true);
    } else if (size == 1) {
      gc.entry = gc.extra.get(0);
      gc.extra = null;
    }
    return (false);
  }    
 
  /***************************************************************************
  **
  ** Write a modified grid contents
  */
  
  private void installModifiedGridContents(Point cellPt, GridEntry entry) {
    if (entry == null) {
      return;
    }
    if ((bounds_ != null) && !bounds_.contains(cellPt)) {
      return;
    }    
    GridContents gc = patternx_.get(cellPt);
    if (gc == null) {
      gc = new GridContents(entry);
      patternx_.put(cellPt, gc);
    } else {
      gc.mergeContents(entry);
    }
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Write a modified grid contents
  */
  
  private void installModifiedGridContents(int xval, int yval, GridEntry entry) { 
    Point cellPt = new Point(xval, yval);
    installModifiedGridContents(cellPt, entry);
    return;
  }
  
 /***************************************************************************
  **
  ** Write a modified 
  */
  
  private void writeBackModifiedGridContents(Point pt, GridContents gc) {
    if ((bounds_ != null) && !bounds_.contains(pt)) {
      return;
    }     
    if ((gc == null) || dropGroupEntries(gc)) {
      patternx_.remove(pt);
    } else {
      patternx_.put(pt, gc);         
    }
    return;
  }      
  
  /***************************************************************************
  **
  ** Write a modified 
  */
  
  private void writeBackModifiedGridContents(int xval, int yval, GridContents gc) {
    Point cellPt = new Point(xval, yval);
    writeBackModifiedGridContents(cellPt, gc);
    return;
  }    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the vector from the direction
  */
  
  public static Vector2D getVector(int direction) {
    switch (direction) {
      case NORTH:
        return (new Vector2D(0.0, -1.0));
      case EAST:
        return (new Vector2D(1.0, 0.0));        
      case SOUTH:
        return (new Vector2D(0.0, 1.0));          
      case WEST:
        return (new Vector2D(-1.0, 0.0));
      default:
        throw new IllegalStateException();
    }
  }
  /***************************************************************************
  **
  ** Get the reverse direction
  */
  
  public static int getReverse(int forward) {
    switch (forward) {
      case NORTH:
        return (SOUTH);
      case SOUTH:          
        return (NORTH);
      case EAST:
        return (WEST);
      case WEST:
        return (EAST);
      default:
        throw new IllegalArgumentException();
    }
  }  
  
  /***************************************************************************
  **
  ** Get the direction from the vector
  */
  
  public static int getDirection(Vector2D vector) {
    int vecX = (int)vector.getX();
    int vecY = (int)vector.getY();
    if ((vecX == -1) && (vecY == 0)) {
      return (WEST); 
    } else if ((vecX == 0) && (vecY == 1)) {
     return (SOUTH); 
    } else if ((vecX == 1) && (vecY == 0)) {
     return (EAST); 
    } else if ((vecX == 0) && (vecY == -1)) {
     return (NORTH); 
    } else {
      System.err.println("Not canonical: " + vector);
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get run direction from points
  */
  
  public static int getDirectionFromPoints(Point start, Point end) {
    int vecX = end.x - start.x;
    int vecY = end.y - start.y;
    if ((vecX == -1) && (vecY == 0)) {
      return (WEST); 
    } else if ((vecX == 0) && (vecY == 1)) {
     return (SOUTH); 
    } else if ((vecX == 1) && (vecY == 0)) {
     return (EAST); 
    } else if ((vecX == 0) && (vecY == -1)) {
     return (NORTH); 
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Answer if we are orthogonal
  */
  
  public static boolean isOrthogonal(Point2D start, Point2D end) {
    double vecX = end.getX() - start.getX();
    double vecY = end.getY() - start.getY();
    if ((vecX == 0.0) && (vecY == 0.0)) {  // zero-length not orthogonal
      return (false); 
    }
    return ((vecX == 0.0) || (vecY == 0.0)); 
  }  
  
 /***************************************************************************
  **
  ** Answer if we are orthogonal
  */
  
  public static boolean isOrthogonal(Vector2D vec) {
    double vecX = vec.getX();
    double vecY = vec.getY();
    if ((vecX == 0.0) && (vecY == 0.0)) {  // zero-length not orthogonal
      return (false); 
    }
    return ((vecX == 0.0) || (vecY == 0.0)); 
  }    
  
  
  /***************************************************************************
  **
  ** Get run direction from points
  */
  
  public static int getDirectionFromCoords(double startX, double startY, double endX, double endY) {
    double vecX = endX - startX;
    double vecY = endY - startY;
    if ((vecX == 0.0) && (vecY == 0.0)) {
      return (NONE);
    } else if ((vecX < 0.0) && (vecY == 0.0)) {
      return (WEST); 
    } else if ((vecX == 0.0) && (vecY > 0.0)) {
      return (SOUTH); 
    } else if ((vecX > 0.0) && (vecY == 0.0)) {
      return (EAST); 
    } else if ((vecX == 0.0) && (vecY < 0.0)) {
      return (NORTH); 
    } else {
      return (DIAGONAL);
    }
  }  

  /***************************************************************************
  **
  ** Get opposite turn 
  */
  
  public static int oppositeTurn(int leftRight) {
    switch (leftRight) {
      case RIGHT:
        return (LEFT);
      case LEFT:
        return (RIGHT);
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the desired turn
  */
  
  public static Vector2D getTurn(Vector2D straight, int leftRight) {
    if ((leftRight != RIGHT) && (leftRight != LEFT)) {
      throw new IllegalArgumentException();
    }

    int vecX = (int)straight.getX();
    int vecY = (int)straight.getY();
    if ((vecX == -1) && (vecY == 0)) { // WEST : NORTH, SOUTH
      return ((leftRight == RIGHT) ? new Vector2D(0.0, -1.0) : new Vector2D(0.0, 1.0)); 
    } else if ((vecX == 0) && (vecY == 1)) { // SOUTH: WEST, EAST
     return ((leftRight == RIGHT) ? new Vector2D(-1.0, 0.0) : new Vector2D(1.0, 0.0)); 
    } else if ((vecX == 1) && (vecY == 0)) { // EAST: SOUTH, NORTH
     return ((leftRight == RIGHT) ? new Vector2D(0.0, 1.0) : new Vector2D(0.0, -1.0)); 
    } else if ((vecX == 0) && (vecY == -1)) { // NORTH: EAST, WEST
     return ((leftRight == RIGHT) ? new Vector2D(1.0, 0.0) : new Vector2D(-1.0, 0.0)); 
    } else {
      System.err.println(straight);
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** get a 4-quad binning of a vector
  */
  
  private Integer getVectorBin(Vector2D vector) {
    if (vector.isZero()) {
      return (new Integer(NONE));
    }
    Vector2D normVec = vector.normalized();
   
    int retval;
    double vecX = normVec.getX();
    double vecY = normVec.getY();
    if (vecX < 0.0) {
      if (vecY < -sqrt2o2_) {
        retval = NORTH;
      } else if (vecY > sqrt2o2_) {
        retval = SOUTH;
      } else {
        retval = WEST; 
      }
    } else {
      if (vecY < -sqrt2o2_) {
        retval = NORTH;
      } else if (vecY > sqrt2o2_) {
        retval = SOUTH;
      } else {
        retval = EAST; 
      }
    }
    return (new Integer(retval));
  }

  /***************************************************************************
  **
  ** Canonicalize a vector: second best choice.  If ortho, we return the
  ** same as the first choice
  */
  
  private Vector2D getSecondClosestCanonical(Vector2D vector) {
    if (vector.isZero()) {
      return (null);
    }
    Vector2D normVec = vector.normalized();
    
    int retval = NONE;
    double vecX = normVec.getX();
    double vecY = normVec.getY();
    if (vecX < 0.0) {
      if ((vecY < -sqrt2o2_) || (vecY > sqrt2o2_)) { 
        retval = WEST;
      } else if (vecY < 0.0) {
        retval = NORTH; 
      } else if (vecY > 0.0) {
        retval = SOUTH; 
      } else {
        retval = NONE;
      }
    } else if (vecX > 0.0) {
      if ((vecY < -sqrt2o2_) || (vecY > sqrt2o2_)) {
        retval = EAST;
      } else if (vecY < 0.0) {
        retval = NORTH;
      } else if (vecY > 0.0) {
        retval = SOUTH; 
      } else {
        retval = NONE;
      }
    }
    if (retval == NONE) {
      return (getClosestCanonical(vector));
    }
    return (getVector(retval));
  }  
  
  /***************************************************************************
  **
  ** Canonicalize a vector
  */
  
  private Vector2D getClosestCanonical(Vector2D vector) {
    
    Integer bin = getVectorBin(vector);
    if (bin.equals(new Integer(NONE))) {
      return (null);
    }
    return (getVector(bin.intValue()));
  }  
  
  /***************************************************************************
  **
  ** Compare this grid to another grid.  Return the list of rows where new
  ** corners have been added (to us compared with the other grid)
  */
  
  public SortedSet<Integer> getAddedCornerRows(LinkPlacementGrid otherGrid, BTProgressMonitor monitor) throws AsynchExitRequestException {
    MinMax myRowRange = this.getMinMaxYForRange(null, monitor);
    MinMax otherRowRange = otherGrid.getMinMaxYForRange(null, monitor);
    MinMax combinedRowRange = myRowRange.union(otherRowRange);
    
    MinMax myColRange = this.getMinMaxXForRange(null, monitor);
    MinMax otherColRange = otherGrid.getMinMaxXForRange(null, monitor);    
    MinMax combinedColRange = myColRange.union(otherColRange);
    
    TreeSet<Integer> retval = new TreeSet<Integer>();
    
    for (int i = combinedRowRange.min; i <= combinedRowRange.max; i++) {
      if (cornerAddedToRow(i, otherGrid, combinedColRange)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Compare this grid to another grid.  Return the list of cols where new
  ** corners have been added (to us compared with the other grid)
  */
  
  public SortedSet<Integer> getAddedCornerCols(LinkPlacementGrid otherGrid, BTProgressMonitor monitor) throws AsynchExitRequestException {
    MinMax myRowRange = this.getMinMaxYForRange(null, monitor);
    MinMax otherRowRange = otherGrid.getMinMaxYForRange(null, monitor);
    MinMax combinedRowRange = myRowRange.union(otherRowRange);
    
    MinMax myColRange = this.getMinMaxXForRange(null, monitor);
    MinMax otherColRange = otherGrid.getMinMaxXForRange(null, monitor);    
    MinMax combinedColRange = myColRange.union(otherColRange);
    
    TreeSet<Integer> retval = new TreeSet<Integer>();
    
    for (int i = combinedColRange.min; i <= combinedColRange.max; i++) {
      if (cornerAddedToCol(i, otherGrid, combinedRowRange)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }   
 
 /***************************************************************************
  **
  ** Return empty rows
  */
  
  public SortedSet<Integer> getEmptyRows(MinMax xBounds, boolean makeRegionsOpaque, 
                                         BTProgressMonitor monitor) throws AsynchExitRequestException {
    return (getEmptyRows(xBounds, makeRegionsOpaque, monitor, null, null));
  }   

  /***************************************************************************
  **
  ** Return empty rows
  */
  
  public SortedSet<Integer> getEmptyRowsWithYBounds(MinMax xBounds, MinMax rowRange, boolean makeRegionsOpaque, 
                                                    BTProgressMonitor monitor) throws AsynchExitRequestException {
    int minGrid = xBounds.min / 10;
    int maxGrid = xBounds.max / 10;
    xBounds = new MinMax(minGrid, maxGrid);
    
    minGrid = rowRange.min / 10;
    maxGrid = rowRange.max / 10;
    rowRange = new MinMax(minGrid, maxGrid);    
    
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    for (int i = rowRange.min; i <= rowRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (rowIsEmpty(i, xBounds, makeRegionsOpaque, null)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return empty rows
  */
  
  public SortedSet<Integer> getEmptyRows(MinMax xBounds, boolean makeRegionsOpaque, 
                                         BTProgressMonitor monitor, Set<String> ignoreNodes, MinMax testedRange) throws AsynchExitRequestException {
    if (xBounds != null) {
      int minGrid = xBounds.min / 10;
      int maxGrid = xBounds.max / 10;
      xBounds = new MinMax(minGrid, maxGrid);
    }
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    MinMax rowRange = getMinMaxYForRange(xBounds, monitor);
    if (rowRange == null) {
      return (retval);
    }
    if (testedRange != null) {
      testedRange.min = rowRange.min;
      testedRange.max = rowRange.max;
    }
    for (int i = rowRange.min; i <= rowRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (rowIsEmpty(i, xBounds, makeRegionsOpaque, ignoreNodes)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return rows that can be expanded
  */
  
  public SortedSet<Integer> getExpandableRows(MinMax xBounds, boolean reversable, BTProgressMonitor monitor) throws AsynchExitRequestException  {
    if (xBounds != null) {
      int minGrid = xBounds.min / 10;
      int maxGrid = xBounds.max / 10;
      xBounds = new MinMax(minGrid, maxGrid);
    }
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    MinMax rowRange = getMinMaxYForRange(xBounds, monitor);
    MinMax colRange = getMinMaxXForRange(null, monitor); // not calculated in rowCanExpand...   
    if ((rowRange == null) || (colRange == null)) {
      return (retval);
    }
    for (int i = rowRange.min; i <= rowRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (rowCanExpand(i, reversable, colRange)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return empty columns
  */
  
  public SortedSet<Integer> getEmptyColumns(MinMax yBounds, boolean makeRegionsOpaque,
                                            BTProgressMonitor monitor) throws AsynchExitRequestException {
    return (getEmptyColumns(yBounds, makeRegionsOpaque, monitor, null, null));
  }  
  
  /***************************************************************************
  **
  ** Return empty columns
  */
  
  public SortedSet<Integer> getEmptyColumnsWithXBounds(MinMax yBounds, MinMax colRange, boolean makeRegionsOpaque,
                                                       BTProgressMonitor monitor) throws AsynchExitRequestException {
  
    int minGrid = yBounds.min / 10;
    int maxGrid = yBounds.max / 10;
    yBounds = new MinMax(minGrid, maxGrid);
    
    minGrid = colRange.min / 10;
    maxGrid = colRange.max / 10;
    colRange = new MinMax(minGrid, maxGrid);    
 
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    for (int i = colRange.min; i <= colRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (columnIsEmpty(i, yBounds, makeRegionsOpaque)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Return empty columns
  */
  
  public SortedSet<Integer> getEmptyColumns(MinMax yBounds, boolean makeRegionsOpaque,
                                            BTProgressMonitor monitor, Set<String> ignoreNodes, MinMax testedRange) throws AsynchExitRequestException {
    if (yBounds != null) {
      int minGrid = yBounds.min / 10;
      int maxGrid = yBounds.max / 10;
      yBounds = new MinMax(minGrid, maxGrid);
    }
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    MinMax colRange = getMinMaxXForRange(yBounds, monitor);
    if (colRange == null) {
      return (retval);
    }
    if (testedRange != null) {
      testedRange.min = colRange.min;
      testedRange.max = colRange.max;
    } 
    for (int i = colRange.min; i <= colRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (columnIsEmpty(i, yBounds, makeRegionsOpaque)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return expandable columns
  */
  
  public SortedSet<Integer> getExpandableColumns(MinMax yBounds, boolean reversable, BTProgressMonitor monitor) throws AsynchExitRequestException  {
    if (yBounds != null) {
      int minGrid = yBounds.min / 10;
      int maxGrid = yBounds.max / 10;
      yBounds = new MinMax(minGrid, maxGrid);
    }    
    TreeSet<Integer> retval = new TreeSet<Integer>(Collections.reverseOrder());
    MinMax rowRange = getMinMaxYForRange(null, monitor);
    MinMax colRange = getMinMaxXForRange(yBounds, monitor);    
    if ((rowRange == null) || (colRange == null)) {
      return (retval);
    }
    for (int i = colRange.min; i <= colRange.max; i++) {
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (columnCanExpand(i, reversable, rowRange)) {
        retval.add(new Integer(i));
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the minimum and maximum bounds of the given range (inclusive).  If none,
  ** (i.e. unoccupied in that range) return null.  If argument is null, returns
  ** for entire grid
  */
  
  public MinMax getMinMaxYForRange(MinMax xRange, BTProgressMonitor monitor) throws AsynchExitRequestException {
    //
    // Go through the keys for the given range and return the
    // minimum.
    //
    
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }
    Iterator<Point> kit = new PatternIterator(true, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if ((xRange == null) || ((pt.x >= xRange.min) && (pt.x <= xRange.max))) {
        if (pt.y < minValue) {
          minValue = pt.y;
        }
        if (pt.y > maxValue) {
          maxValue = pt.y;
        }
      }
    }    
    
    if ((minValue == Integer.MAX_VALUE) || (maxValue == Integer.MIN_VALUE)) {
      return (null);
    } else {
      return (new MinMax(minValue, maxValue));
    }
  } 
  
  /***************************************************************************
  **
  ** Get the minimum and maximum bounds of the given range (inclusive).  If none,
  ** (i.e. unoccupied in that range) return null.  If argument is null, returns
  ** for entire grid
  */
  
  public MinMax getMinMaxXForRange(MinMax yRange, BTProgressMonitor monitor) throws AsynchExitRequestException {
    //
    // Go through the keys for the given range and return the
    // minimum.
    //
    
    int minValue = Integer.MAX_VALUE;
    int maxValue = Integer.MIN_VALUE;
    
    Iterator<Point> kit = new PatternIterator(true, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      if ((yRange == null) || ((pt.y >= yRange.min) && (pt.y <= yRange.max))) {
        if (pt.x < minValue) {
          minValue = pt.x;
        }
        if (pt.x > maxValue) {
          maxValue = pt.x;
        }
      }
    }    
    
    if ((minValue == Integer.MAX_VALUE) || (maxValue == Integer.MIN_VALUE)) {
      return (null);
    } else {
      return (new MinMax(minValue, maxValue));
    }
  }  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class TerminalRegion {
  
    public static final int ARRIVAL   = 0;    
    public static final int DEPARTURE = 1;
    
    String id;
    Point start;
    boolean[] slots;
    int direction;
    int type;
    
    TerminalRegion(String id, Point start, boolean[] slots, int direction, int type) {
      this.id = id;
      this.start = start;
      this.slots = slots;
      this.direction = direction;
      this.type = type;
    } 

    public int hashCode() {
      int slotCode = 0;
      int factor = 1;
      for (int i = 0; i < slots.length; i++) {
        int slotVal = (slots[i]) ? 1 : 0;
        slotCode += (factor * slotVal);
        factor *= 2;      
      }
      return (id.hashCode() + start.hashCode() + direction + type + slotCode);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TerminalRegion)) {
        return (false);
      }
      TerminalRegion otherTR = (TerminalRegion)other;
      
      if (!this.id.equals(otherTR.id)) {
        return (false);
      }
      if (!this.start.equals(otherTR.start)) {
        return (false);
      }
      if (this.direction != otherTR.direction) {
        return (false);
      }
      if (this.type != otherTR.type) {
        return (false);
      }
      if (this.slots.length != otherTR.slots.length) {
        return (false);
      }     
       
      for (int i = 0; i < this.slots.length; i++) {
        if (this.slots[i] != otherTR.slots[i]) {
          return (false);
        }
      }
      return (true);
    }
 
    public String toString() {
      StringBuffer slotBuf = new StringBuffer();
      for (int i = 0; i < slots.length; i++) {
        slotBuf.append(slots[i]);
        slotBuf.append(' ');
      }
      return ("Terminal Region: id = " + id + " start = " + start + " slots = " +
              slotBuf + " direction = " + direction + " type = " + type);  
    }    
    
    int getNumPoints() {
      int retval = 0;
      for (int i = 0; i < slots.length; i++) { 
        if (slots[i]) retval++;
      }
      return (retval);
    }    
    
    
    Point getPoint(int n) {
      int count = 0;
      for (int i = 0; i < slots.length; i++) { 
        if (slots[i]) {
          if (count == n) {
            Vector2D addon = getTerminalVector();
            addon.scale((type == ARRIVAL) ? -i : i);
            Point2D retval = addon.add(start);
            return (new Point((int)retval.getX(), (int)retval.getY()));
          }   
          count++;
        }
      }
      throw new IllegalArgumentException();
    }
    
    int getNumTravelPoints() {
      return (slots.length);
    }

    Point getTravelPoint(int n) {
      Vector2D addon = getTerminalVector();
      addon.scale((type == ARRIVAL) ? -n : n);
      Point2D retval = addon.add(start);
      return (new Point((int)retval.getX(), (int)retval.getY()));
    }    
 
    Vector2D getTerminalVector() {
      return (getVector(direction));
    }
  }
  
  public static class NewCorner {
    
    public int type;
    public Point2D point;
    public int dir;    
    
    NewCorner(int type, Point2D point, int dir) {
      this.type = type;
      this.point = point;
      this.dir = dir;
    } 
    
    public String toString() {
      return ("NewCorner: type = " + type + " point = " + point + " dir = " + dir);  
    }   
   
  }
  
  public static class GoodnessParams {  
    public double differenceSigma;
    public double differenceCoeff;
    double differenceNormalize;
    public double crossingMultiplier;
    public double crossingCoeff;
    double normalize;
    double terminal;
  
    public GoodnessParams() {
      //
      // WJRL 5-20-08: Ditch the crossing parameter.  This keeps things
      // from running out forever to get around other links.
      crossingCoeff = 0.0;
      crossingMultiplier = 0.0;
      //crossingCoeff = 0.25;
      //crossingMultiplier = 0.10;
      differenceCoeff = .50;
      differenceSigma = 15;
      terminal = 10.0;
    }
    
    public GoodnessParams(GoodnessParams other) {
      this.crossingCoeff = other.crossingCoeff;
      this.crossingMultiplier = other.crossingMultiplier;
      this.differenceCoeff = other.differenceCoeff;
      this.differenceSigma = other.differenceSigma;
      this.terminal = other.terminal;
      this.differenceNormalize = other.differenceNormalize;
      this.normalize = other.normalize;
    }
  }
  
  public static class InkValues {  
    public int ink;
    public int simpleCorners;
    public int complexCorners;

    public InkValues(int ink, int simpleCorners, int complexCorners) {
      this.ink = ink;
      this.simpleCorners = simpleCorners;
      this.complexCorners = complexCorners;
    }
    
    public InkValues() {
    }     

    public InkValues(InkValues other) {
      this.copy(other);
    }  
      
    public static InkValues merge(InkValues origFull, InkValues origSub, InkValues modSub) {
      InkValues retval = new InkValues();
      retval.ink = (origFull.ink - origSub.ink) + modSub.ink;
      retval.simpleCorners = (origFull.simpleCorners - origSub.simpleCorners) + modSub.simpleCorners;
      retval.complexCorners = (origFull.complexCorners - origSub.complexCorners) + modSub.complexCorners;
      return (retval);
    }
 
    public void copy(InkValues other) {
      this.ink = other.ink;
      this.simpleCorners = other.simpleCorners;
      this.complexCorners = other.complexCorners;
    }
    
    public String toString() {
      return ("InkValues: ink = " + ink + 
              " simpleCorners: " + simpleCorners +  
              " complexCorners: " + complexCorners);        
    }        
  }
  
  public static class PointPair {

    public Point2D start;
    public Point2D end;

    public PointPair() {
    }    
    
    public PointPair(Point2D start, Point2D end) {
      this.start = (Point2D)start.clone();
      this.end = (end == null) ? null : (Point2D)end.clone();
    }

    public void shift(Vector2D shift) {
      start = shift.add(start);
      end = (end == null) ? null : shift.add(end);
      return;
    }

    public int hashCode() {
      return (start.hashCode() + ((end == null) ? 0 : end.hashCode()));
    }

    public String toString() {
      return ("start = " + start + " end = " + end);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof PointPair)) {
        return (false);
      }
      PointPair otherPair = (PointPair)other;
      
      if (!this.start.equals(otherPair.start)) {
        return (false);
      }
      
      if (this.end == null) {
        return (otherPair.end == null);
      }
      
      return (this.end.equals(otherPair.end));
    }
  }

  // Partial closure of RecoveryDataForSource:
  
  public static class RecoveryDataForLink {    
    private RecoveryDataForSource rdfs_;
    private String linkID_;
    private int offset_;
    private Point2D shiftedStart_;
       
    public RecoveryDataForLink(RecoveryDataForSource rdfs, String linkID) {   
      rdfs_ = rdfs;
      linkID_ = linkID;
      shiftedStart_ = null;
      offset_ = 0;
    }
          
    public RecoveryDataForLink(RecoveryDataForLink other, int offset) {
      this.rdfs_ = other.rdfs_;
      this.linkID_ = other.linkID_;
      if (offset < 0) {
        throw new IllegalArgumentException();
      }
      this.offset_ = offset;
      this.shiftedStart_ = null;
    } 
    
    public RecoveryDataForLink(RecoveryDataForLink other, int offset, Point2D shiftedStart) {
      this.rdfs_ = other.rdfs_;
      this.linkID_ = other.linkID_;
      if (offset < 0) {
        throw new IllegalArgumentException();
      }
      this.offset_ = offset;
      this.shiftedStart_ = shiftedStart;
    }     
    
    public String dumpPath() {
      StringBuffer buf = new StringBuffer();
      int count = getPointCount();
      for (int i = 0; i < count; i++) {
        buf.append("["); 
        buf.append(getPoint(i).toString());
        buf.append(" @ ");  
        buf.append(getInboundRun(i).toString());
        buf.append("] "); 
      }
      return (buf.toString());
    }
    
    public RecoveryDataForSource getEnclosingSource() {
      return (rdfs_);
    }  
    
    public LinkSegmentID getSourceBoundPoint() {
      return (rdfs_.getBoundPointPerLink(linkID_));
    }  
    
    public int getSourceBoundIndex() {
      LinkSegmentID id = rdfs_.getBoundPointPerLink(linkID_);
      int ptCount = getPointCount();
      for (int i = 0; i < ptCount; i++) {
        LinkSegmentID pkfi = getPointKey(i);
        if (pkfi.equals(id)) {
          return (i);
        }
      }
      return (-1);
    }      
   
    public RecoveryPointData getRpd(int index) {
      return (rdfs_.getRpd(linkID_, index, offset_));
    }

    public int getPointCount() {
      return (rdfs_.getPointCount(linkID_, offset_));
    }      
    
    public Point2D getPoint(int index) {
      if ((index == 0) && (shiftedStart_ != null)) {
        return (shiftedStart_);
      }
      return (rdfs_.getPoint(linkID_, index, offset_));
    }
    
    public Vector2D getInboundRun(int index) {
      return (rdfs_.getInboundRun(linkID_, index, offset_));
    }    
    
    public Point2D getPoint(LinkSegmentID lsid) {
      return (rdfs_.getPoint(lsid));
    }
   
    public boolean getInboundOrtho(int index) {
      return (rdfs_.getInboundOrtho(linkID_, index, offset_));
    }
    
    public BusProperties.CornerDoF getCornerDoF(int index) {
      return (rdfs_.getCornerDoF(linkID_, index, offset_));
    } 
    
    public boolean revisedPointCanBePushed(LinkSegmentID lsid) {
      return (rdfs_.revisedPointCanBePushed(lsid));
    }        
 
    public void pushRevisedPoint(LinkSegmentID lsid, Point2D point) {
      rdfs_.pushRevisedPoint(lsid, point);
      return;
    }
    
    public void popRevisedPoint(LinkSegmentID lsid) {
      rdfs_.popRevisedPoint(lsid);
      return;
    }    
    
    public void commitAllRevisedPoints() {
      rdfs_.commitAllRevisedPoints();
      return;
    }
    
    public void popAllRevisedPoints() {
      rdfs_.popAllRevisedPoints();
      return;
    }         
    
    public LinkSegmentID getPointKey(int index) {
      return (rdfs_.getPointKey(linkID_, index, offset_));
    }      

  }  
 
 ////////////////// Data per source
  
  public static class RecoveryDataForSource {    
    private HashMap<String, List<LinkSegmentID>> segListPerLink;
    private HashMap<LinkSegmentID, RecoveryPointData> pointDataPerSegID;
    private HashMap<LinkSegmentID, RecoveryPointData> pushedPointDataPerSegID;
    private HashMap<Point2D, List<Point2D>> jumpsFromPoints;
    private HashMap<Point2D, Point2D> jumpsIntoPoints;
    private HashMap<String, LinkSegmentID> boundPointPerLink;
    private Map<LinkSegmentID, SortedSet<LinkSegmentID>> segmentToKidsMap;
    private LinkSegmentID rootSeg;
    private String directLinkID;
    private Set<String> exemptions;
    private HashMap<String, LayoutRubberStamper.EdgeMove> arrivalRegionChops;
    private HashMap<String, LayoutRubberStamper.EdgeMove> departureRegionChops;
    
    public RecoveryDataForSource() {
      segListPerLink = new HashMap<String, List<LinkSegmentID>>();
      pointDataPerSegID = new HashMap<LinkSegmentID, RecoveryPointData>();
      pushedPointDataPerSegID = new HashMap<LinkSegmentID, RecoveryPointData>();
      jumpsFromPoints = new HashMap<Point2D, List<Point2D>>();
      jumpsIntoPoints = new HashMap<Point2D, Point2D>();
      boundPointPerLink = new HashMap<String, LinkSegmentID>();
      arrivalRegionChops = new HashMap<String, LayoutRubberStamper.EdgeMove>();
      departureRegionChops = new HashMap<String, LayoutRubberStamper.EdgeMove>();
    }
    
    public void setArrivalRegionChops(Map<String, LayoutRubberStamper.EdgeMove> arrivalRegionChops) {
      this.arrivalRegionChops = (arrivalRegionChops == null) ? null : new HashMap<String, LayoutRubberStamper.EdgeMove>(arrivalRegionChops);
      return;
    }     
   
    public void setDepartureRegionChops(Map<String, LayoutRubberStamper.EdgeMove> departureRegionChops) {
     this.departureRegionChops = (departureRegionChops == null) ? null : new HashMap<String, LayoutRubberStamper.EdgeMove>(departureRegionChops);
     return;
    } 
    
    public Map<String, LayoutRubberStamper.EdgeMove> getArrivalRegionChops() {
      return (arrivalRegionChops);
    }     
   
    public Map<String, LayoutRubberStamper.EdgeMove> getDepartureRegionChops() {
     return (departureRegionChops);
    }    
  
    public void setTreeTopology(Map<LinkSegmentID, SortedSet<LinkSegmentID>> segmentToKidsMap, LinkSegmentID rootSeg, String directLinkID) {
      this.segmentToKidsMap = segmentToKidsMap;
      this.rootSeg = rootSeg;
      this.directLinkID = directLinkID;
      return;
    }
    
    public void setExemptions(Set<String> exemptions) {
      this.exemptions = exemptions;
      return;
    }     
   
    public Set<String> getExemptions() {
      return (exemptions);
    }    
       
    public String getDirectLinkID() {
      return (directLinkID);
    }
    
    public LinkSegmentID getRootSeg() {
      return (rootSeg);
    }
    
    public Map<LinkSegmentID, SortedSet<LinkSegmentID>> getSegmentToKidsMap() {
      return (segmentToKidsMap);
    }    
 
    public Set<String> getLinks() {
      return (segListPerLink.keySet());
    } 
    
    public void setSegListPerLink(String linkID, List<LinkSegmentID> segList) {
      segListPerLink.put(linkID, new ArrayList<LinkSegmentID>(segList));
      return;
    }
    
    public void setBoundPointPerLink(String linkID, LinkSegmentID bpSeg) {
      boundPointPerLink.put(linkID, bpSeg);
      return;
    }
    
    public LinkSegmentID getBoundPointPerLink(String linkID) {
      return (boundPointPerLink.get(linkID));
    }    
    
    public boolean revisedPointCanBePushed(LinkSegmentID lsid) {
      RecoveryPointData rpd = pointDataPerSegID.get(lsid);
      if (rpd.isCommitted()) {
        return (false);
      }
      if (rpd.isUnreachable()) {
        return (false);
      }
      RecoveryPointData pushedRpd = pushedPointDataPerSegID.get(lsid);
      return (pushedRpd == null);
    }    

    public void pushRevisedPoint(LinkSegmentID lsid, Point2D point) {
      RecoveryPointData rpd = pointDataPerSegID.get(lsid);
      if (rpd.isCommitted()) {
        throw new IllegalStateException();
      }
      if (rpd.isUnreachable()) {
        throw new IllegalStateException();
      }      
      RecoveryPointData pushedRpd = pushedPointDataPerSegID.get(lsid);
      if (pushedRpd != null) {
        throw new IllegalStateException();
      }
      RecoveryPointData copy = rpd.clone();
      copy.point = (Point2D)point.clone();
      pushedPointDataPerSegID.put(lsid, copy);
      return;
    }
    
    public void popAllRevisedPoints() {
      pushedPointDataPerSegID.clear();
      return;
    } 
    
    public void popRevisedPoint(LinkSegmentID lsid) {
      pushedPointDataPerSegID.remove(lsid);
      return;
    } 
    
    public void addJumpFromPoint(Point2D oldPoint, Point2D jumped) {
      Point dummyPt = new Point();
      Point2D jumped2D = new Point2D.Double();   
      pointConversion(jumped, jumped2D, dummyPt);    
      List<Point2D> ptList = jumpsFromPoints.get(oldPoint);
      if (ptList == null) {
        ptList = new ArrayList<Point2D>();
        jumpsFromPoints.put(oldPoint, ptList);
      }
      ptList.add(jumped2D);
      return;
    }    
    
    public void addJumpIntoPoint(Point2D oldPoint, Point2D jumped) {
      Point dummyPt = new Point();
      Point2D jumped2D = new Point2D.Double();   
      pointConversion(jumped, jumped2D, dummyPt);    
      jumpsIntoPoints.put(oldPoint, jumped2D);
      return;
    }     
       
    public List<Point2D> getJumpFromPoints(Point2D oldPoint) {
      return (jumpsFromPoints.get(oldPoint));
    }   
    
    public Point2D getJumpIntoPoint(Point2D oldPoint) {
      return (jumpsIntoPoints.get(oldPoint));
    }     
    
    public void commitAllRevisedPoints() {
      Iterator<LinkSegmentID> pkit = pushedPointDataPerSegID.keySet().iterator();
      while (pkit.hasNext()) {
        LinkSegmentID lsid = pkit.next();
        RecoveryPointData pushedRpd = pushedPointDataPerSegID.get(lsid);
        pointDataPerSegID.put(lsid, pushedRpd);
        pushedRpd.commit();
      }
      pushedPointDataPerSegID.clear();
      return;
    } 
    
    public RecoveryPointData getRpd(LinkSegmentID lsid) { 
      RecoveryPointData rpd = pushedPointDataPerSegID.get(lsid);
      if (rpd == null) {
        rpd = pointDataPerSegID.get(lsid);
      }
      if ((rpd == null) || rpd.isUnreachable()) {
        return (null);
      }
      return (rpd);
    }       
   
    public Point2D getPoint(LinkSegmentID lsid) { 
      RecoveryPointData rpd = pushedPointDataPerSegID.get(lsid);
      if (rpd == null) {
        rpd = pointDataPerSegID.get(lsid);
      }
      if ((rpd == null) || rpd.isUnreachable()) {
        return (null);
      }
      
      return (rpd.point);
    }  
    
    public boolean getInboundOrtho(LinkSegmentID lsid) { 
      RecoveryPointData rpd = pushedPointDataPerSegID.get(lsid);
      if (rpd == null) {
        rpd = pointDataPerSegID.get(lsid);
      }
      if ((rpd == null) || rpd.isUnreachable()) {
        return (false);
      }      
      return (rpd.inboundOrtho);
    }     
     
    public void putPointData(LinkSegmentID lsid, RecoveryPointData rpd) {
      pointDataPerSegID.put(lsid, rpd);
      return;
    }     
       
    // Get rid of deadly zero-length segments 
    public void cleanup() {
      Iterator<String> slit = segListPerLink.keySet().iterator(); 
      while (slit.hasNext()) {
        String linkID = slit.next();
        List<LinkSegmentID> linkList = segListPerLink.get(linkID);
        int numpt = linkList.size();
        ArrayList<LinkSegmentID> cleanedPtsForLink = new ArrayList<LinkSegmentID>();
        LinkPlacementGrid.RecoveryPointData lastPt = null;
        for (int i = 0; i < numpt; i++) {
          LinkSegmentID segID = linkList.get(i);
          LinkPlacementGrid.RecoveryPointData nextPt = pointDataPerSegID.get(segID);
          if ((nextPt == null) || (nextPt.point == null)) {
            // If we get here, we messed up.  Ignore for now...
            continue;
          }
          if ((lastPt == null) || !nextPt.point.equals(lastPt.point)) {
            cleanedPtsForLink.add(segID);
            lastPt = nextPt;
          }
        }
        linkList.clear();
        linkList.addAll(cleanedPtsForLink);
      }
      return;
    }
    
    public LinkSegmentID getPointKey(String linkID, int index, int offset) {
      List<LinkSegmentID> segList = segListPerLink.get(linkID); 
      int reachablePreOff = 0;
      int reachablePostOff = 0;      
      int segSize = segList.size();
      for (int i = 0; i < segSize; i++) {
        LinkSegmentID segID = segList.get(i);
        RecoveryPointData rpd = pushedPointDataPerSegID.get(segID);
        if (rpd == null) {
          rpd = pointDataPerSegID.get(segID);
        }
        if (rpd.isUnreachable()) {
          continue;
        }
        if ((reachablePreOff == offset) && (index == reachablePostOff)) {
          return (segID);          
        }       
        // Offset takes unreachable into account already, so we need to count differently!        
        if (reachablePreOff < offset) {
          reachablePreOff++;
        } else if (reachablePreOff == offset) {
          reachablePostOff++;
        } else {
          throw new IllegalStateException();
        }
      }
      return (null);
    }
    
    public RecoveryPointData getRpd(String linkID, int index, int offset) {
      List<LinkSegmentID> segList = segListPerLink.get(linkID); 
      int reachablePreOff = 0;
      int reachablePostOff = 0;      
      int segSize = segList.size();
      for (int i = 0; i < segSize; i++) {
        LinkSegmentID segID = segList.get(i);
        RecoveryPointData rpd = pushedPointDataPerSegID.get(segID);
        if (rpd == null) {
          rpd = pointDataPerSegID.get(segID);
        }
        if (rpd.isUnreachable()) {
          continue;
        }
        if ((reachablePreOff == offset) && (index == reachablePostOff)) {
          return (rpd);          
        }       
        // Offset takes unreachable into account already, so we need to count differently!        
        if (reachablePreOff < offset) {
          reachablePreOff++;
        } else if (reachablePreOff == offset) {
          reachablePostOff++;
        } else {
          throw new IllegalStateException();
        }
      }
      return (null);
    }

    public int getPointCount(String linkID, int offset) {
      List<LinkSegmentID> segList = segListPerLink.get(linkID);
      int segSize = (segList == null) ? 0 : segList.size();
      int reachablePreOff = 0;
      int reachablePostOff = 0;      
      for (int i = 0; i < segSize; i++) {
        LinkSegmentID segID = segList.get(i);
        RecoveryPointData rpd = pushedPointDataPerSegID.get(segID);
        if (rpd == null) {
          rpd = pointDataPerSegID.get(segID);
        }
        if (rpd.isUnreachable()) {
          continue;
        }  
        // Offset takes unreachable into account already, so we need to count differently!        
        if (reachablePreOff < offset) {
          reachablePreOff++;
        } else if (reachablePreOff == offset) {
          reachablePostOff++;
        } else {
          throw new IllegalStateException();
        }
      }
      return (reachablePostOff);
    }    
      
    public Point2D getPoint(String linkID, int index, int offset) {
      RecoveryPointData rpd = getRpd(linkID, index, offset);
      return (rpd.point);
    }
    
    public Vector2D getInboundRun(String linkID, int index, int offset) {
      RecoveryPointData rpd = getRpd(linkID, index, offset);
      return (rpd.inboundRun);
    }    
    
    public boolean getInboundOrtho(String linkID, int index, int offset) {
      RecoveryPointData rpd = getRpd(linkID, index, offset);
      return (rpd.inboundOrtho);
    }
    
    public BusProperties.CornerDoF getCornerDoF(String linkID, int index, int offset) {
      RecoveryPointData rpd = getRpd(linkID, index, offset);
      return (rpd.dof);
    } 

    public boolean isOriginalPoint(Point2D pt) {
      Point2D testPt = new Point2D.Double(pt.getX() * 10.0, pt.getY() * 10.0);
      Iterator<RecoveryPointData> pvdit = pointDataPerSegID.values().iterator();
      while (pvdit.hasNext()) { 
        RecoveryPointData rpd = pvdit.next();
        if (rpd.point == null) {
          // FIX ME: Why am I null?
          continue;
        }
        if (rpd.point.equals(testPt)) {
          return (true);
        }
      }  
      return (false);
    }
  }  
   
  ////////////////// Data per point
  
  
  public static class RecoveryPointData implements Cloneable {    
    public Point2D point;
    public boolean inboundOrtho;
    public Vector2D inboundRun;
    public BusProperties.CornerDoF dof;
    private boolean committed_;
    private boolean unreachable_;
    
    public RecoveryPointData(Point2D point, boolean inboundOrtho, Vector2D inboundRun, BusProperties.CornerDoF cornerDoF) {
      this.point = point;
      this.inboundOrtho = inboundOrtho;
      this.inboundRun = inboundRun;
      this.dof = cornerDoF;
      this.committed_ = false;
      this.unreachable_ = false;
    } 
    
    public RecoveryPointData clone() {
      try {
        RecoveryPointData retval = (RecoveryPointData)super.clone();
        retval.point = (this.point == null) ? null : (Point2D)this.point.clone();
        retval.inboundRun = (this.inboundRun == null) ? null : (Vector2D)this.inboundRun.clone();
        retval.dof = (this.dof == null) ? null : (BusProperties.CornerDoF)this.dof.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    public void commit() {
      committed_ = true;
      return;
    }
    
    public boolean isCommitted() {
      return (committed_);
    }   
    
    public void tagAsUnreachable() {
      unreachable_ = true;
      return;
    }
    
    public boolean isUnreachable() {
      return (unreachable_);
    }       
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  /***************************************************************************
  **
  ** Answer if a corner has been added to our row compared with the other grid.
  */
  
  private boolean cornerAddedToRow(int y, LinkPlacementGrid otherGrid, MinMax colRange) {
    Point pt = new Point(colRange.min, y);
    for (int i = colRange.min; i <= colRange.max; i++) {
      pt.x = i;
      GridContents myGval = this.getGridContents(pt);
      GridContents otherGval = otherGrid.getGridContents(pt);      
      if (cornerAddedToContents(myGval, otherGval)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if a corner has been added to our column compared with the other grid.
  */
  
  private boolean cornerAddedToCol(int x, LinkPlacementGrid otherGrid, MinMax rowRange) {
    Point pt = new Point(x, rowRange.min);
    for (int i = rowRange.min; i <= rowRange.max; i++) {
      pt.y = i;
      GridContents myGval = this.getGridContents(pt);
      GridContents otherGval = otherGrid.getGridContents(pt);
      if (cornerAddedToContents(myGval, otherGval)) {
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Answer if a corner has been added to our contents compared with the other grid.
  */
  
  private boolean cornerAddedToContents(GridContents myGval, GridContents otherGval) {
    if (myGval == null) {  // Nothing left here, can't have a new corner...
      return (false);
    }
    int myType = cornerType(myGval);
    if (otherGval == null) {
      if (myType != NO_CORNER) {
        return (true);
      }
    } else {
      int otherType = cornerType(otherGval);
      if ((otherType == NO_CORNER) && (myType != NO_CORNER)) {
        return (true);
      }
    }
    return (false);
  }   
  
  /***************************************************************************
  **
  ** Drop data for given link from given point.  Returns true if we add to dead list.
  */
  
  private boolean dropLinkReference(GridContents gc, String srcID, Point pt, List<Point> deadList) {
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.id.equals(srcID) && 
          ((gc.entry.type != IS_NODE) && (gc.entry.type != IS_NODE_INBOUND_OK))) { 
        deadList.add(pt);
        return (true);
      }
      return (false);
    }
    
    int size = gc.extra.size();
    for (int i = size - 1; i >= 0; i--) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.id.equals(srcID) && 
          ((currEntry.type != IS_NODE) && (currEntry.type != IS_NODE_INBOUND_OK))) { 
        gc.extra.remove(i);
      }
    }
    
    size = gc.extra.size();
    if (size == 0) {
      deadList.add(pt);
      return (true);
    } else if (size == 1) {
      gc.entry = gc.extra.get(0);
      gc.extra = null;
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Dump all temporary entries
  */
  
  private void clearTemps() {
    List<Point> myTemps = findTemps();
    int numTemps = myTemps.size();
    for (int i = 0; i < numTemps; i++) {
      Point pt = myTemps.get(i);
      removeTemp(pt, false);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Check for temps
  */
  
  public void checkForTemps() {
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      checkForTemp(pt);
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Find points with temps
  */
  
  private List<Point> findTemps() {
    ArrayList<Point> list = new ArrayList<Point>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      if (hasATemp(pt)) {
        list.add(pt);
      }
    }
    return (list);
  }      
  
  /***************************************************************************
  **
  ** Following a successful link path, we sink the temporary entries
  */
  
  private void commitTemps(String linkID) {
    List<Point> myTemps = findTemps();
    int numTemps = myTemps.size();
    for (int i = 0; i < numTemps; i++) {
      Point pt = myTemps.get(i);
      GridContents gc = getGridContents(pt);
      commitForContents(gc);
      writeBackModifiedGridContents(pt, gc);
    }
    drawnLinks_.add(linkID);
    return;
  }
    
  /***************************************************************************
  **
  ** Commit an entry
  */
  
  private void commitForContents(GridContents gc) {
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      gc.entry.commitTemp();
      return;
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i); 
      currEntry.commitTemp();
    }
    return;
  }

  /***************************************************************************
  **
  ** Used to find temps
  */
  
  private boolean hasATemp(Point2D pt) {
    int x = (int)pt.getX();
    int y = (int)pt.getY();
    GridContents gc = getGridContents(x, y);
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.isTemp());
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.isTemp()) {
        return (true);
      }
    }
 
    return (false);
  }    

  /***************************************************************************
  **
  ** Used to debug state temps
  */
  
  private void checkForTemp(Point2D pt) {
    int x = (int)pt.getX();
    int y = (int)pt.getY();
    GridContents gc = getGridContents(x, y);
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.isTemp()) {
        System.out.println("Temp at " + pt);
      }
      return;
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.isTemp()) {
        System.out.println("Temp at " + pt);
      }
    }
 
    return;
  }  
  
  /***************************************************************************
  **
  ** Used to debug state temps
  */
  
  private boolean checkForTempFlag(Point2D pt) {
    int x = (int)pt.getX();
    int y = (int)pt.getY();
    GridContents gc = getGridContents(x, y);
    if (gc == null) {
      return (false);
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.isTemp()) {
        return (true);
      }
      return (false);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.isTemp()) {
        return (true);
      }
    }
 
    return (false);
  }    
  /***************************************************************************
  **
  ** Following a failed link path, we remove the temporary entries
  */
  
  private void removeTemp(Point2D pt, boolean checkCorrect) {
    int x = (int)pt.getX();
    int y = (int)pt.getY();
    GridContents gc = getGridContents(x, y);
    if (checkCorrect && (gc == null)) {
      //System.err.println("no entry at " + pt);
      return;
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.isTemp()) {
        if (gc.entry.needsPostTempRestore()) {
          gc.entry.clearTemp();
        } else {
          gc = null;
        }
      } else if (checkCorrect) {
        //System.err.println("not temp at " + pt);
        //throw new IllegalStateException();
      }
      writeBackModifiedGridContents(x, y, gc);
      return;
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.isTemp()) {
        if (currEntry.needsPostTempRestore()) {
          currEntry.clearTemp();
        } else {
          gc.extra.remove(i);
        }
        break;
      }
    }
    
    size = gc.extra.size();
    if (size == 0) {
      throw new IllegalStateException();  
    } else if (size == 1) {
      gc.entry = gc.extra.get(0);
      gc.extra = null;
    }
    writeBackModifiedGridContents(x, y, gc);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a temporary entry for a run
  */  

  private void addTempEntryForRun(Point2D pt, String src, int direction) {
    if (direction == DIAGONAL) {
      return;
    }
    int ptx = (int)pt.getX();
    int pty = (int)pt.getY();
    
    GridEntry entry = new GridEntry(src, IS_RUN, getReverse(direction), direction, true);
   // if (this.checkForTempFlag(pt)) {
   //   System.err.println("bad point is " + pt);
    //  throw new IllegalArgumentException();
  //  }
    installModifiedGridContents(ptx, pty, entry);
    return;
  }

  /***************************************************************************
  **
  ** Add a temporary entry for a corner
  */  

  private void addTempEntryForCorner(Point2D pt, String src, int entryDir, int exit) {
    GridEntry entry = new GridEntry(src, IS_CORNER, getReverse(entryDir), exit, true);
    int ptx = (int)pt.getX();
    int pty = (int)pt.getY();
    installModifiedGridContents(ptx, pty, entry);
  } 
  
 /***************************************************************************
  **
  ** Add a temporary corner entry for a diagonal:
  */  

  private void addTempEntryForDiagonalCorner(Point2D pt, String src) {
    GridEntry entry = new GridEntry(src, IS_CORNER, NONE, DIAGONAL, true);
    int ptx = (int)pt.getX();
    int pty = (int)pt.getY();
    installModifiedGridContents(ptx, pty, entry);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a temporary entry for a final corner.  There should already be a corner there.
  */  

  private void addTempEntryForFinalCorner(Point2D pt, String src, int exit) throws NonConvergenceException {
    int ptx = (int)pt.getX();
    int pty = (int)pt.getY();
    GridContents gc = getGridContents(ptx, pty);
    if (gc == null) {
      System.err.println("no grid contents at " + pt + " for src " + src);
      // Used to throw Illegal State, but saw this during a trySimpleRecovery just before
      // V6 release.  Get the hell out of here, but be graceful...
      throw new NonConvergenceException();
    } else {
      gc.mergeContentsForFinalCorner(src, exit);
      writeBackModifiedGridContents(ptx, pty, gc);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Answer if the given source can travel from start to end.  Points must
  ** have pure vertical or horizontal displacement
  */
  
  @SuppressWarnings("unused")
  private boolean canTravel(String src, String trg, String linkID, Set<String> okGroups,
                            Point2D start, Point2D end, boolean directDepart) {
    
    Vector2D travel = new Vector2D(start, end);
    // will throw an exception if vector is not horizontal or vertical.
    WalkValues walk = getWalkValues(travel.normalized(), start, end, 1);
        
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        GridContents gc = getGridContents(x, y);
        if (!checkForTravel(gc, src, trg, linkID, okGroups, walk, null, null, directDepart)) {
          return (false);
        }
      }
    }
    return (true);
  }
 

  /***************************************************************************
  **
  ** Determine if this is a valid departure slot
  */
  
  private int generateDeparture(GridContents gc, String id, Set<String> okGroups, WalkValues walk) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we are in our source node space, or if we cross another link run at right
    // angles.  We are only valid if nothing is present.  Otherwise, we have to stop.
    //
    
    if (gc == null) {
      return (OK);
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (resolveDepartureCell(gc.entry, id, okGroups, walk));
    }
    
    int size = gc.extra.size();
     /*
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = (GridEntry)gc.extra.get(i);
      if (resolveDepartureCell(currEntry, id, okGroups, walk) == STOP) {
        return (STOP);
      }
    }
    return (CONTINUE);
    */
    boolean allOK = true;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      int racVal = resolveDepartureCell(currEntry, id, okGroups, walk);
      if (racVal == STOP) {
        return (STOP);
      } else if (racVal != OK) {
        allOK = false;
      }
    }
    return ((allOK) ? OK : CONTINUE);
  } 
  
  /***************************************************************************
  **
  ** Determine if this is a valid arrival slot
  */
  
  private int generateArrival(GridContents gc, String srcID, String targID, String linkID,
                              Set<String> okGroups, WalkValues walk) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we are in our source node space, or if we cross another link run at right
    // angles.  We are only valid if nothing is present.  Otherwise, we have to stop.
    //
    
    if (gc == null) {
      return (OK);
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (resolveArrivalCell(gc.entry, srcID, targID, linkID, okGroups, walk.entryDirection));
    }
    
    int size = gc.extra.size();
    boolean allOK = true;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      int racVal = resolveArrivalCell(currEntry, srcID, targID, linkID, okGroups, walk.entryDirection);
      if (racVal == STOP) {
        return (STOP);
      } else if (racVal != OK) {
        allOK = false;
      }
    }
    return ((allOK) ? OK : CONTINUE);
  } 
  
  /***************************************************************************
  **
  ** Determine if this is a valid arrival slot for an emergency collision resolution
  ** (e.g. the best we can do is a free point in the rough vicinity of the target)
  */
  
  private boolean canGenerateEmergencyArrival(GridContents gc, String srcID, String targID, String linkID,
                                              Set<String> okGroups, int entryDirection) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we are in our source node space, or if we cross another link run at right
    // angles.  We are only valid if nothing is present.  Otherwise, we have to stop.
    //
    
    if (gc == null) {
      return (true);
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (resolveArrivalCell(gc.entry, srcID, targID, linkID, okGroups, entryDirection) == OK);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      int racVal = resolveArrivalCell(currEntry, srcID, targID, linkID, okGroups, entryDirection);
      if (racVal != OK) {
        return (false);
      }
    }
    return (true);
  }   
   
  /***************************************************************************
  **
  ** Resolve departure cell calculations
  */
  
  private int resolveDepartureCell(GridEntry entry, String id, Set<String> okGroups, WalkValues walk) {

    //
    // Check the grid contents.  If we are still inside the source node space, just
    // continue.  If we are crossing another link at right angles, keep going.  If
    // we are overlaid on another link (except ourselves) stop.  If we hit another
    // node space, stop.
    //
    
    //
    // FIX ME!! Implement IS_NODE_OUTBOUND_OK!!
    
    
    if (entry.type == IS_NODE) {
      if (entry.id.equals(id)) {
        //return (CONTINUE);  We want to allow early turns... 5/13/08
        return (OK);
      } else {
        return (STOP);
      }
    }
    
    if (!entry.canPassThrough(id, null, null, okGroups, walk.exitDirection, null, null, true, false)) {
      return (STOP);
    } else if (entry.type == IS_GROUP) {
      return (OK);
    } else {
      return (CONTINUE);
    } 
  }   
 
  /***************************************************************************
  **
  ** Resolve arrival cell calculations
  */
  
  private int resolveArrivalCell(GridEntry entry, String srcID, String targID, String linkID, 
                                 Set<String> okGroups, int entryDirection) {

    //
    // Check the grid contents.  If we are still inside the source node space, just
    // continue.  If we are crossing another link at right angles, keep going.  If
    // we are overlaid on another link (except ourselves) stop.  If we hit another
    // node space, stop.
    //
    
    if (entry.type == IS_NODE) {
      if (entry.id.equals(targID)) {
        return (CONTINUE);
      } else {
        return (STOP);
      }
    }
    if (!entry.canPassThrough(srcID, targID, linkID, okGroups, entryDirection, null, null, false, false)) {
      return (STOP);
    } else if (entry.type == IS_NODE_INBOUND_OK) {
      return (OK);
    } else if (entry.type == IS_GROUP) {
      return (OK);
    } else {
      return (CONTINUE);
    }
  }
  
   /***************************************************************************
  **
  ** Determine if this is a valid travel slot
  */
  
  private boolean checkForTravel(GridContents gc, String id, 
                                 String trg, String linkID, 
                                 Set<String> okGroups,
                                 WalkValues walk, Set<String> groupBlockers, 
                                 Set<String> recoveryExemptions,
                                 boolean directDepart) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we cross another link run at right angles.  For the last cell, nothing
    // must be present.
    //
    
    if (gc == null) {
      return (true);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (resolveTravelCell(gc.entry, id, trg, linkID, okGroups, walk, groupBlockers, recoveryExemptions, directDepart, false));
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (!resolveTravelCell(currEntry, id, trg, linkID, okGroups, walk, groupBlockers, recoveryExemptions, directDepart, false)) {
        return (false);
      }
    }
    return (true);
  } 
  
   /***************************************************************************
  **
  ** Determine if this is a valid travel slot.  -1 means not valid, -2 means hit
  ** the target and stopped else returns crossing count
  */
  
  private int checkForTravelCountCrossings(GridContents gc, String id, 
                                           String trg, String linkID,
                                           Set<String> okGroups, 
                                           WalkValues walk, 
                                           Set<String> groupBlockers, Set<String> recoveryExemptions,
                                           boolean directDepart, boolean ignoreMyTemps) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we cross another link run at right angles. 
    //
    
    if (gc == null) {
      return (0);
    }

    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (resolveTravelCellCountCrossings(gc.entry, id, trg, linkID, okGroups, walk, 
                                              groupBlockers, recoveryExemptions, 
                                              directDepart, ignoreMyTemps));
    }
    
    int size = gc.extra.size();
    int crossingCount = 0;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      int nextCrossing = resolveTravelCellCountCrossings(currEntry, id, trg, linkID, 
                                                         okGroups, walk, groupBlockers, recoveryExemptions, 
                                                         directDepart, ignoreMyTemps);
      if (nextCrossing < 0) {
        return (nextCrossing);
      }
      crossingCount += nextCrossing;
    }
    return (crossingCount);
  } 
  
   /***************************************************************************
  **
  ** Determine if this cell can support a turn.
  */
  
  private boolean okForTurns(GridContents gc, String src, String trg, 
                             String linkID, Set<String> okGroups) {

    //
    // Check the grid contents. If anything is present, we can only continue if
    // we cross another link run at right angles.  For the last cell, nothing
    // must be present unless it is an allowed inbound node cell.
    //
    
    if (gc == null) {
      return (true);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.entryOKForCorner(src, trg, linkID, okGroups));
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (!currEntry.entryOKForCorner(src, trg, linkID, okGroups)) {
        return (false);
      }
    }   
    return (true);
  }
  
  /***************************************************************************
  **
  ** Determine if this cell has one and only one run for the source
  */
  
  private boolean isSingleRunForSource(GridContents gc, String src) {
    
    if (gc == null) {
      return (false);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return ((gc.entry.type == IS_RUN) && (gc.entry.id.equals(src)));
    }
    
    boolean gottaRun = false;
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_RUN) && (currEntry.id.equals(src))) {
        if (gottaRun) {
          return (false);
        }
        gottaRun = true;
      }
    }   
    return (gottaRun);
  }  
  
  /***************************************************************************
  **
  ** Get the groups in the cell
  */
  
  private Set<String> getCellGroupContents(GridContents gc) {
    
    HashSet<String> retval = new HashSet<String>();
    if (gc == null) {
      return (retval);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.type == IS_GROUP) {
        retval.add(gc.entry.id);
      }
      return (retval);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_GROUP) {
        retval.add(currEntry.id);
      }
    }   
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Resolve travel cell calculations
  */
  
  private boolean resolveTravelCell(GridEntry entry, String id, String trg, 
                                    String linkID, Set<String> okGroups, 
                                    WalkValues walk, Set<String> groupBlockers, Set<String> recoveryExemptions,
                                    boolean directDepart, boolean ignoreMyTemps) {

    //
    // Check the grid contents for traveling
    //
    
    if (entry.type == IS_NODE) {
      if ((entry.id.equals(id) && directDepart) || 
          ((recoveryExemptions != null) && recoveryExemptions.contains(entry.id))) {
        // We want to allow direct starts to traverse the node space
        return (true);
      } else {
        return (false);
      }
    }
    
    return (entry.canPassThrough(id, trg, linkID, okGroups, walk.exitDirection, groupBlockers, 
                                 recoveryExemptions, directDepart, ignoreMyTemps));
  }
  
  /***************************************************************************
  **
  ** Resolve travel cell calculations.  -1 means not allowed, else gives crossing count
  */
  
  private int resolveTravelCellCountCrossings(GridEntry entry, String id, String trg, String linkID, 
                                              Set<String> okGroups, 
                                              WalkValues walk, 
                                              Set<String> groupBlockers, Set<String> recoveryExemptions,
                                              boolean directDepart, boolean ignoreMyTemps) {

    //
    // Check the grid contents for traveling
    //
    if (entry.type == IS_NODE) {
      if ((recoveryExemptions != null) && recoveryExemptions.contains(entry.id)) {
        return (0);
      }
      if (!(entry.id.equals(id) && directDepart)) {
        if ((trg != null) && (entry.id.equals(trg))) {
          // used to check if approach is blocked by target node
          return (-2);
        }
        // We want to allow direct starts to traverse the node space
        return (-1);
      }
 
    }  
    return (entry.canPassThroughCountCrossings(id, trg, linkID, okGroups, walk.exitDirection, 
                                               groupBlockers, recoveryExemptions, directDepart, ignoreMyTemps));
  }  
    
  /***************************************************************************
  **
  ** Draw the path segment
  */
  
  private void fillForLink(String linkSrc, double x1, double y1, 
                           double x2, double y2, CornerOracle corc, DecoInfo pointData) {

    FillData fd = buildFillData(linkSrc, x1, y1, x2, y2, corc);
    for (int x = fd.startX; x != fd.endX; x += fd.incX) {
      for (int y = fd.startY; y != fd.endY; y += fd.incY) {
        GridEntry entry;
        if ((x == fd.startX) && (y == fd.startY)) {
          entry = fd.startEntry;
        } else if ((x == fd.endX - fd.incX) && (y == fd.endY - fd.incY)) {
          entry = fd.endEntry;
        } else {
          entry = fd.runEntry;
        }
        if (entry != null) { // skip diagonal runs
          Point cellPt = new Point(x, y);
          installModifiedGridContents(cellPt, new GridEntry(entry));
          if (pointData != null) {
            addUserData(cellPt, pointData, linkSrc);            
          }
        }
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Add user data.  User data allows us to hold information for a link
  ** concerning e.g. the associated net module link used to place the link.
  */
  
  private void addUserData(Point cellPt, DecoInfo pointData, String dataID) {
    if (pointData == null) {
      return;
    }
    Map<String, DecoInfo> udm = userData_.get(cellPt);
    if (udm == null) {
      udm = new HashMap<String, DecoInfo>();
      userData_.put(cellPt, udm);      
    }
    udm.put(dataID, pointData);
    return;
  }
  
  /***************************************************************************
  **
  ** Get user data
  */
  
  private DecoInfo getUserData(Point cellPt, String dataID) {
    Map<String, DecoInfo> udm = userData_.get(cellPt);
    if (udm == null) {
      return (null);
    }
    return (udm.get(dataID));
  }

  /***************************************************************************
  **
  ** Draw the path segment
  */
  
  private FillData buildFillData(String linkSrc, double x1, double y1, 
                                 double x2, double y2, CornerOracle corc) {
    //
    // Ditch if not initialized properly:
    //
    if ((x1 == Double.MAX_VALUE) || (y1 == Double.MAX_VALUE)) {
      throw new IllegalArgumentException();
    }
    
    FillData fd = new FillData();
    
    double deltaX = x2 - x1;
    double deltaY = y2 - y1;
    
    fd.startX = (int)x1 / 10;
    fd.incX = 1; // so we always have one pass
    fd.endX = (int)x2 / 10;
    fd.startY = (int)y1 / 10;
    fd.incY = 1; // so we always have one pass
    fd.endY = (int)y2 / 10;
    fd.startEntry = null;
    fd.runEntry = null;
    fd.endEntry = null;
        
    boolean startIsPad = !corc.isCornerPoint(new Point2D.Double(x1, y1));
    boolean endIsPad = !corc.isCornerPoint(new Point2D.Double(x2, y2));
    
    int startType = (startIsPad) ? IS_PAD : IS_CORNER;
    int endType = (endIsPad) ? IS_PAD : IS_CORNER;
    
    //
    // Enumerate each combination separately in a flat listing for clarity, not efficiency:
    //
    
    if ((deltaX < 0) && (deltaY < 0)) {   // DIAGONAL; JUST DO ENDPOINTS
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, DIAGONAL, false);
      fd.endEntry = new GridEntry(linkSrc, endType, DIAGONAL, NONE, false);
      fd.incX = -1;
      fd.incY = -1;
    } else if ((deltaX < 0) && (deltaY == 0)) { // EAST-WEST
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, WEST, false);
      fd.runEntry = new GridEntry(linkSrc, IS_RUN, EAST, WEST, false);      
      fd.endEntry = new GridEntry(linkSrc, endType, EAST, NONE, false);
      fd.incX = -1;
    } else if ((deltaX < 0) && (deltaY > 0)) {   // DIAGONAL; JUST DO ENDPOINTS
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, DIAGONAL, false);
      fd.endEntry = new GridEntry(linkSrc, endType, DIAGONAL, NONE, false);
      fd.incX = -1;
      fd.incY = 1;
    } else if ((deltaX == 0) && (deltaY < 0)) {  // SOUTH-NORTH 
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, NORTH, false);
      fd.runEntry = new GridEntry(linkSrc, IS_RUN, SOUTH, NORTH, false);      
      fd.endEntry = new GridEntry(linkSrc, endType, SOUTH, NONE, false);
      fd.incY = -1;
    } else if ((deltaX == 0) && (deltaY == 0)) { // DEGENERATE
      fd.startEntry = new GridEntry(linkSrc, IS_DEGENERATE, NONE, NONE, false);      
    } else if ((deltaX == 0) && (deltaY > 0)) { // NORTH-SOUTH
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, SOUTH, false);
      fd.runEntry = new GridEntry(linkSrc, IS_RUN, NORTH, SOUTH, false);      
      fd.endEntry = new GridEntry(linkSrc, endType, NORTH, NONE, false);
      fd.incY = 1;
    } else if ((deltaX > 0) && (deltaY < 0)) {   // DIAGONAL; JUST DO ENDPOINTS
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, DIAGONAL, false);
      fd.endEntry = new GridEntry(linkSrc, endType, DIAGONAL, NONE, false);
      fd.incX = 1;
      fd.incY = -1;
    } else if ((deltaX > 0) && (deltaY == 0)) { // WEST-EAST
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, EAST, false);
      fd.runEntry = new GridEntry(linkSrc, IS_RUN, WEST, EAST, false);      
      fd.endEntry = new GridEntry(linkSrc, endType, WEST, NONE, false);
      fd.incX = 1;
    } else if ((deltaX > 0) && (deltaY > 0)) {   // DIAGONAL; JUST DO ENDPOINTS
      fd.startEntry = new GridEntry(linkSrc, startType, NONE, DIAGONAL, false);
      fd.endEntry = new GridEntry(linkSrc, endType, DIAGONAL, NONE, false);
      fd.incX = 1;
      fd.incY = 1;
    }
    fd.endX += fd.incX;
    fd.endY += fd.incY;
    
    return (fd);
  }
  

  /***************************************************************************
  **
  ** Answer if we can draw the path segment
  */
  
  private boolean canFillForLink(String linkSrc, double x1, double y1, 
                                 double x2, double y2, LinkProperties props, 
                                 Map<PointPair, List<String>> linkMap, 
                                 Map<String, String> targMap, Map<String, GenomeInstance.GroupTuple> tupMap) {

    FillData fd = buildFillData(linkSrc, x1, y1, x2, y2, props);
    PointPair pair = new PointPair(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
    List<String> links = linkMap.get(pair);
    ArrayList<String> targs = new ArrayList<String>();
    ArrayList<GenomeInstance.GroupTuple> tups = new ArrayList<GenomeInstance.GroupTuple>();    
    int lnum = links.size();
    for (int i = 0; i < lnum; i++) {
      String linkID = links.get(i);
      targs.add(targMap.get(linkID));
      GenomeInstance.GroupTuple tup = tupMap.get(linkID);
      if ((tup != null) && !tups.contains(tup)) {
        tups.add(tup);
      }
    }
    for (int x = fd.startX; x != fd.endX; x += fd.incX) {
      for (int y = fd.startY; y != fd.endY; y += fd.incY) {
        GridEntry entry;
        if ((x == fd.startX) && (y == fd.startY)) {
          entry = fd.startEntry;
        } else if ((x == fd.endX - fd.incX) && (y == fd.endY - fd.incY)) {
          entry = fd.endEntry;
        } else {
          entry = fd.runEntry;
        }
        if (entry != null) { // skip diagonal runs
          GridContents gc = getGridContents(x, y);
          if (!canMergeContents(gc, entry, links, targs, tups)) {
            return (false);
          }
        }
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Determine if it is legal to merge the given entry into the contents
  */
  
  private boolean canMergeContents(GridContents gc, GridEntry entry, 
                                   List<String> links, List<String> targs, List<GenomeInstance.GroupTuple> tups) {
    if (gc == null) {
      return (true);
    }    
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.canMergeEntries(entry, links, targs, tups));
    }
       
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (!currEntry.canMergeEntries(entry, links, targs, tups)) {
        return (false);
      }
    }
    return (true);
  }   

  /***************************************************************************
  **
  ** Answer if the given row is empty.  It is MUCH better to hand it
  ** precalculated bounds for huge grids.
  */
  
  @SuppressWarnings("unused")
  private boolean rowIsEmpty(int y, MinMax xBounds, boolean makeRegionsOpaque) {
    return (rowIsEmpty(y, xBounds, makeRegionsOpaque, null));
  }  

  /***************************************************************************
  **
  ** Answer if the given row is empty.  It is MUCH better to hand it
  ** precalculated bounds for huge grids.
  */
  
  private boolean rowIsEmpty(int y, MinMax xBounds, boolean makeRegionsOpaque, Set<String> ignoreNodes) {
    if (xBounds != null) {
      Point pt = new Point(xBounds.min, y);
      for (int i = xBounds.min; i <= xBounds.max; i++) {
        pt.x = i;
        // grid contents need group entries if we are making regions opaque:
        GridContents gval = getGridContents(pt, makeRegionsOpaque);        
        if (gval == null) {
          continue;
        }      
        if (!cellNotRequired(gval, makeRegionsOpaque, ignoreNodes)) {
          return (false);
        }
      }
      return (true);
    } else {    
      Iterator<Point> kit = new PatternIterator(makeRegionsOpaque, false);
      while (kit.hasNext()) {
        Point pt = kit.next();
        if (pt.y != y) {
          continue;
        }
        if ((xBounds != null) && 
            ((pt.x < xBounds.min) || (pt.x > xBounds.max))) { 
          continue;
        }
        GridContents gval = getGridContents(pt, makeRegionsOpaque);
        if (gval == null) {
          continue;
        }
        if (!cellNotRequired(gval, makeRegionsOpaque, ignoreNodes)) {
          return (false);
        }
      }
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if the given row can expand
  */
  
  private boolean rowCanExpand(int y, boolean reversable, MinMax xBounds) {
    if (!reversable) {
      return (true);
    }
    ArrayList<String> nodeList = new ArrayList<String>();
    ArrayList<String> neighborList = new ArrayList<String>();
    Point pt = new Point(xBounds.min, y);
    Point pt2 = new Point(xBounds.min, y - 1);      
    for (int i = xBounds.min; i <= xBounds.max; i++) {
      pt.x = i;
      pt2.x = i;
      GridContents gval = getGridContents(pt, false);
      GridContents gval2 = getGridContents(pt2, false);
      nodeList.clear();
      neighborList.clear();
      boolean iAmNode = (gval != null) && cellHasNode(gval, nodeList);
      boolean neighborIsNode = (gval2 != null) && cellHasNode(gval2, neighborList);
      if (!iAmNode) {
        continue;
      }
      //
      // Special case: we allow expansion at top boundary of a node region.
      //
      if (iAmNode) {
        if (!neighborIsNode) {
          continue;
        } else {  // both are nodes, but must be disjoint!
          nodeList.retainAll(neighborList);
          if (nodeList.isEmpty()) {
            continue;
          }
        }
      }
      return (false);
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Answer if the given column is empty.   It is MUCH better to hand it
  ** precalculated bounds for huge grids.
  */
  
  private boolean columnIsEmpty(int x, MinMax yBounds, boolean makeRegionsOpaque) {
    return (columnIsEmpty(x, yBounds, makeRegionsOpaque, null));
  }  
 
  /***************************************************************************
  **
  ** Answer if the given column is empty.   It is MUCH better to hand it
  ** precalculated bounds for huge grids.
  */
  
  private boolean columnIsEmpty(int x, MinMax yBounds, boolean makeRegionsOpaque, Set<String> ignoreNodes) {
    if (yBounds != null) {
      Point pt = new Point(x, yBounds.min);
      for (int i = yBounds.min; i <= yBounds.max; i++) {
        pt.y = i;
        GridContents gval = getGridContents(pt, makeRegionsOpaque);
        if (gval == null) {
          continue;
        }      
        if (!cellNotRequired(gval, makeRegionsOpaque, ignoreNodes)) {
          return (false);
        }
      }
      return (true);
    } else {
      Iterator<Point> kit = new PatternIterator(makeRegionsOpaque, false);
      while (kit.hasNext()) {
        Point pt = kit.next();
        if (pt.x != x) {
          continue;
        }
        if ((yBounds != null) && 
            ((pt.y < yBounds.min) || (pt.y > yBounds.max))) {
          continue;
        }
        GridContents gval = getGridContents(pt, makeRegionsOpaque);
        if (gval == null) {
          continue;
        }
        if (!cellNotRequired(gval, makeRegionsOpaque, ignoreNodes)) {
          return (false);
        }
      }
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if the given column can expand
  */
  
  private boolean columnCanExpand(int x, boolean reversable, MinMax yBounds) {
    if (!reversable) {
      return (true);
    }
    ArrayList<String> nodeList = new ArrayList<String>();
    ArrayList<String> neighborList = new ArrayList<String>();
    Point pt = new Point(x, yBounds.min);
    Point pt2 = new Point(x - 1, yBounds.min);      
    for (int i = yBounds.min; i <= yBounds.max; i++) {
      pt.y = i;
      pt2.y = i;
      GridContents gval = getGridContents(pt, false);
      GridContents gval2 = getGridContents(pt2, false);
      nodeList.clear();
      neighborList.clear();
      boolean iAmNode = (gval != null) && cellHasNode(gval, nodeList);
      boolean neighborIsNode = (gval2 != null) && cellHasNode(gval2, neighborList);
      if (!iAmNode) {
        continue;
      } else {
      //
      // Special case: we allow expansion at top boundary of a node region.
      //
        if (!neighborIsNode) {
          continue;
        } else {  // both are nodes, but must be disjoint!
          nodeList.retainAll(neighborList);
          if (nodeList.isEmpty()) {
            continue;
          }
        }
      }
      return (false);
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Answer if the cell is unessential to the layout
  */
 
  @SuppressWarnings("unused")
  private boolean cellNotRequired(GridContents gc, boolean makeRegionsOpaque) {
    return (cellNotRequired(gc, makeRegionsOpaque, null));
  }
  
  /***************************************************************************
  **
  ** Answer if the cell is unessential to the layout.  We can provide a list of
  ** nodes to ignore.
  */
  
  private boolean cellNotRequired(GridContents gc, boolean makeRegionsOpaque, Set<String> ignoreNodes) {

    //
    // Figure out if the node entries we encounter can be ignored:
    //
    
    boolean canIgnoreNodes = false;
    if (ignoreNodes != null) {
      ArrayList<String> nodeList = new ArrayList<String>();
      if (cellHasNode(gc, nodeList)) {
        HashSet<String> hasSet = new HashSet<String>(nodeList);
        hasSet.removeAll(ignoreNodes);
        canIgnoreNodes = hasSet.isEmpty();
      }
    }       
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (canIgnoreNodes && ((gc.entry.type == IS_NODE) || (gc.entry.type == IS_NODE_INBOUND_OK))) {
        return (true);
      }
      //
      // If there is ONLY a run here at this block, it is never required (return true).
      // If there is only a group here, it is not required if we are not making regions opaque (return true),
      // but it is required if we are making regions opaque.  If there is a non-group/non-run here,
      // it is required no matter what.
      //
      return ((gc.entry.type == IS_RUN) || 
              (!makeRegionsOpaque && (gc.entry.type == IS_GROUP)));
    }

    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (canIgnoreNodes && ((currEntry.type == IS_NODE) || (currEntry.type == IS_NODE_INBOUND_OK))) {
        continue;
      } 
      //
      // As we iterate through entries here, this block is required if we have a non-run while making
      // regions opaque, or a non-group/non-run while not making regions opaque.
      //
      if ((currEntry.type != IS_RUN) && 
          (makeRegionsOpaque || (currEntry.type != IS_GROUP))) {
        return (false);
      }
    }
    return (true);  
  }  
  
  /***************************************************************************
  **
  ** Answer if the cell is unessential to the layout
  */
  
  private boolean cellIsGroupOnly(GridContents gc) {
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.type == IS_GROUP);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type != IS_GROUP) {
        return (false);
      }
    }
    return (true);  
  }  
  
  /***************************************************************************
  **
  ** Answer if the cell is a node.  Fill in IDs if it is
  */
  
  private boolean cellHasNode(GridContents gc, List<String> ids) {
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if ((gc.entry.type == IS_NODE) || (gc.entry.type == IS_NODE_INBOUND_OK)) {
        ids.add(gc.entry.id);  
        return (true);
      } else {
        return (false);
      }
    }
    
    boolean hasNode = false;
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_NODE) || (currEntry.type == IS_NODE_INBOUND_OK)) {
        ids.add(currEntry.id);  
        hasNode = true;
      }
    }
    return (hasNode);  
  }
  
  /***************************************************************************
  **
  ** Answer if the cell is a pad
  */
  
  @SuppressWarnings("unused")
  private boolean cellIsPad(GridContents gc) {
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (gc.entry.type == IS_PAD);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_PAD) {
        return (true);
      }
    }
    return (false);  
  }  

  /***************************************************************************
  **
  ** Answer the type if this cell is OK for launching a new branch
  */
  
  private int lookForLaunchCell(String srcID, GridContents gc, Set<String> okGroups, Set<String> linksFromSource) {
    
    //
    // To be valid, it either must have a single valid launch entry,
    // or a set with the others being group entries, one of which matches the 
    // tuple source or target.
    //
   
    if (gc == null) {
      return (IS_EMPTY);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return (lookForLaunchEntry(srcID, gc.entry));
    }
    
    int size = gc.extra.size();
    int retval = IS_EMPTY;
    boolean needEntry = true;
    boolean gotGroup = false;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_GROUP) {
        if ((okGroups != null) && okGroups.contains(currEntry.id)) {
          gotGroup = true;
        }
      } else if (currEntry.type == IS_NODE_INBOUND_OK) { // Not disqualifying
        continue;  
      } else if (!currEntry.id.equals(srcID) &&
                 (currEntry.type == IS_RESERVED_ARRIVAL_RUN) &&   
                 linksFromSource.contains(currEntry.id)) { // OK if compatable
        continue;
      } else if (currEntry.id.equals(srcID) &&
                 (currEntry.type == IS_RESERVED_MULTI_LINK_ARRIVAL_RUN)) { // Also compatable
        continue;
      } else if (currEntry.id.equals(srcID) && (currEntry.type == IS_NODE)) { // Not disqualifying, as long as there is a corner there...
        continue;  
      } else if (needEntry) {
        retval = lookForLaunchEntry(srcID, currEntry);
        needEntry = false;
      }
    }
    //
    // With multiple overlapping groups, we allow as long as source
    // group is one of the overlaps:
    //
    if ((okGroups != null) && !gotGroup) {
      return (IS_EMPTY);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer the type if this cell is OK for launching a new branch
  */
  
  private int lookForLaunchEntry(String srcID, GridEntry entry) {
    
    if (entry == null) {
      return (IS_EMPTY);
    }
    
    if (!entry.id.equals(srcID)) {
      return (IS_EMPTY);
    }
    
    // This is an issue if we are trying to relocate while the link is being laid out:
    if (entry.isTemp()) {
      return (IS_EMPTY);
    }
    
    if (entry.type == IS_RUN) {
      return (IS_RUN);
    }
    
    if (entry.type != IS_CORNER) {
      return (IS_EMPTY);
    }
    
    if (entry.entryCount != 1) {
      return (IS_EMPTY);
    }
 
    // FIX ME: Restrict to < 3 outbounds, with available directions facing us. 
    return (IS_CORNER);
  }  
  
  /***************************************************************************
  **
  ** Answer if this cell registers a link from a source
  */
  
  private boolean lookForSource(String srcID, GridContents gc) {
    if (gc == null) {
      return (false);
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.id.equals(srcID) && 
          ((gc.entry.type != IS_NODE) && (gc.entry.type != IS_NODE_INBOUND_OK))) {
        return (true);
      }
      return (false);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.id.equals(srcID) && 
          ((currEntry.type != IS_NODE) && (currEntry.type != IS_NODE_INBOUND_OK))) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Returns count of other links in this cell
  */
  
  private int otherLinkCount(String srcID, GridContents gc) {
    if (gc == null) {
      return (0);
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (!gc.entry.id.equals(srcID) && 
          ((gc.entry.type != IS_NODE) && 
           (gc.entry.type != IS_NODE_INBOUND_OK) &&
           (gc.entry.type != IS_GROUP))) {
        return (1);
      }
      return (0);
    }
    
    int size = gc.extra.size();
    int count = 0;
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (!currEntry.id.equals(srcID) && 
          ((currEntry.type != IS_NODE) && 
           (currEntry.type != IS_NODE_INBOUND_OK) &&
           (currEntry.type != IS_GROUP))) {
        count++;
      } 
    }
    return (count);
  }
  
  /***************************************************************************
  **
  ** Answers if we are cool with a reserved arrival
  */
  
  private boolean isOKForTerminal(GridContents gc) {
    if (gc == null) {
      return (true);
    }
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if ((gc.entry.type != IS_NODE) && 
          (gc.entry.type != IS_NODE_INBOUND_OK) &&
          (gc.entry.type != IS_GROUP)) {
        return (false);
      }
      return (true);
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type != IS_NODE) && 
          (currEntry.type != IS_NODE_INBOUND_OK) &&
          (currEntry.type != IS_GROUP)) {
        return (false);
      } 
    }
    return (true);
  }  
   

 /***************************************************************************
  **
  ** Make sure these values are not insane......
  */
  
  private boolean checkWalkValues(WalkValues walk) {
    //
    // 3/19/10: One version of insane is diffX or diffY of
    // zero, which was throwing division by zero exceptions.
    // Catch it first: 
    int diffX = (walk.endX - walk.startX);
    if ((diffX == 0) || ((diffX / Math.abs(diffX)) != walk.incX)) {
      return (false);
    }
    int diffY = (walk.endY - walk.startY);
    if ((diffY == 0) || ((diffY / Math.abs(diffY)) != walk.incY)) {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Figure out how to move orthogonally in single steps in the given direction
  */
  
  private WalkValues getWalkValues(Vector2D vec, Point2D start, Point2D end, int sign) {
    if ((sign != 1) && (sign != -1)) {
      throw new IllegalArgumentException();
    }
    WalkValues retval = new WalkValues();
    int vecX = (int)vec.getX();
    int vecY = (int)vec.getY();
    retval.startX = (int)start.getX();
    retval.startY = (int)start.getY();
    int minVal = (sign > 0) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int maxVal = (sign > 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    if ((vecX == -1) && (vecY == 0)) {
      retval.incY = 1;
      retval.incX = -1 * sign;      
      retval.endX = (end == null) ? minVal : (int)end.getX() + retval.incX;
      retval.endY = retval.startY + retval.incY;
      retval.entryDirection = (sign > 0) ? EAST : WEST;
      retval.exitDirection = (sign > 0) ? WEST : EAST;
    } else if ((vecX == 0) && (vecY == 1)) {
      retval.incY = 1 * sign;
      retval.incX = 1;
      retval.endX = retval.startX + retval.incX;
      retval.endY = (end == null) ? maxVal : (int)end.getY() + retval.incY;
      retval.entryDirection = (sign > 0) ? NORTH : SOUTH;
      retval.exitDirection = (sign > 0) ? SOUTH : NORTH;      
    } else if ((vecX == 1) && (vecY == 0)) {
      retval.incY = 1;
      retval.incX = 1 * sign;
      retval.endX = (end == null) ? maxVal : (int)end.getX() + retval.incX;
      retval.endY = retval.startY + retval.incY;
      retval.entryDirection = (sign > 0) ? WEST : EAST;
      retval.exitDirection = (sign > 0) ? EAST : WEST;      
    } else if ((vecX == 0) && (vecY == -1)) {
      retval.incY = -1 * sign;
      retval.incX = 1;
      retval.endX = retval.startX + retval.incX;
      retval.endY = (end == null) ? minVal : (int)end.getY() + retval.incY;
      retval.entryDirection = (sign > 0) ? SOUTH : NORTH;
      retval.exitDirection = (sign > 0) ? NORTH : SOUTH;      
    } else {
      System.err.println("bad vector: " + vec);
      throw new IllegalArgumentException();
    }
     
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find out how far we can get to our destination, starting at the given point,
  ** and only travelling in the given direction.
  */
  
  private TravelResult analyzeTravel(String src, Point2D start, Vector2D direction,
                                     String trg, Point2D end, String linkID, 
                                     Set<String> okGroups, Set<String> groupBlockers, boolean isStart) {
    //
    // If the direction we are handed is sending us in the wrong direction, get
    // outta here:
    //
    
    HashSet<String> myGroupBlocker = (okGroups != null) ? new HashSet<String>() : null;
    Vector2D travel = new Vector2D(start, end);
    double fullDist = travel.dot(direction);
    if (fullDist < 0.0) { 
      System.err.println(start + " " + end + " " + travel + " " + direction + " "+ fullDist);
      throw new IllegalArgumentException();
      //return (null);
    } else if (fullDist == 0.0) {
      return (new TravelResult(false, (Point2D)end.clone(), 1.0, 0, 0.0, 0.0, null, false));
    }
    
    //
    // Try stepping to the end, going until we hit the end or a block, and summing up the 
    // number of crossings we make:
    //
    
    WalkValues walk = getWalkValues(direction, start, end, 1);    
    int crossings = 0;
    double fraction = 0.0;
    double currDist = 0.0;
    Point2D farthestPt = (Point2D)start.clone();
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        GridContents gc = getGridContents(x, y);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walk, myGroupBlocker, null, isStart, false);
        if (currentCrossings < 0) {
          if ((groupBlockers != null) && (myGroupBlocker != null)) {
            groupBlockers.addAll(myGroupBlocker);
          }
          return (new TravelResult(true, farthestPt, fraction, crossings, 0.0, currDist, myGroupBlocker, (currentCrossings == -2)));
        }
        crossings += currentCrossings;
        farthestPt = new Point2D.Double(x, y);
        Vector2D travelVec = new Vector2D(start, farthestPt);
        currDist = travelVec.length();
        fraction = currDist / fullDist;
      }
    }
    return (new TravelResult(true, farthestPt, fraction, crossings, 0.0, currDist, myGroupBlocker, false));
  }
  
  /***************************************************************************
  **
  ** Handle detour calculations
  */
  
  private TravelResult analyzeDetourTravel(String src, Point2D start, 
                                           Vector2D detourDirection,
                                           Vector2D desiredDirection,
                                           String trg, Point2D end, 
                                           String linkID, Set<String> okGroups,
                                           Set<String> groupBlockers, boolean isStart) {
    //
    // Heading in a direction orthogonal to how we want to go, find out how far off course
    // we need to go before we can begin to make forward progress again.  Will return null
    // if we are blocked.
    //
    WalkValues walk = getWalkValues(detourDirection, start, null, 1);
    
 //   int crossings = 0;
 //   Point2D farthestPt = (Point2D)start.clone();
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        GridContents gc = getGridContents(x, y);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walk, groupBlockers, null, isStart, false);
        if (currentCrossings < 0) {
          return (null);
        }
//        crossings += currentCrossings;
        Point2D detourTurn = new Point2D.Double(x, y);
        TravelResult result = analyzeTravel(src, detourTurn, desiredDirection, trg, end, linkID, okGroups, groupBlockers, isStart);
        // farthest point represents turn point, fraction says how close to target.
        if (result.fraction > 0.0) {
          Vector2D detour = new Vector2D(start, detourTurn);
          result.detourDistance = detour.length();
          return (result);
        }
      }
    }
    throw new IllegalStateException();
  }
  
  
  
  /***************************************************************************
  **
  ** We want to start with an immediate turn if that's where the target is.
  */
  
  private List<StartIndex> pickStartIndex(TerminalRegion departure, TerminalRegion arrival) {  
    ArrayList<StartIndex> retval = new ArrayList<StartIndex>();
  
    int numDep = departure.getNumPoints();
    Point end = arrival.getPoint(0);
    Vector2D departDir = departure.getTerminalVector();
    int leftDir = getDirection(getTurn(departDir, LEFT));
    int rightDir = getDirection(getTurn(departDir, RIGHT));
    
    //
    // Try to find an immediate turn:
    //
    
    int foundMatch = -1;
    for (int i = 0; i < numDep; i++) {  
      Point start = departure.getPoint(i); 
      int direct = getDirectionFromCoords(start.getX(), start.getY(), end.getX(), end.getY());
      if ((direct != DIAGONAL) && (direct != NONE)) {
        Vector2D travel = getVector(direct);
        if ((direct == leftDir) || (direct == rightDir)) {
          retval.add(new StartIndex(i, travel));
          foundMatch = i;
          break; 
        }
      }
    }
 
    //
    // The best of the rest:
    //

    int bestStart = (numDep < 3) ? numDep - 1 : 2;
    for (int i = bestStart; i >= 0; i--) {
      if (i != foundMatch) {
        retval.add(new StartIndex(i, departDir));
      }
    }
    for (int i = numDep - 1; i > bestStart; i--) {
      if (i != foundMatch) {
        retval.add(new StartIndex(i, departDir));
      }
    }    
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Build a starting turn specification to boot the link search.
  */
  
  private TravelTurn buildInitTurn(String src, Point2D start, Vector2D direction,
                                   String trg, Point2D end, String linkID,
                                   Set<String> okGroups) {

    HashSet<String> groupBlockers = (okGroups != null) ? new HashSet<String>() : null;                                 
    Vector2D travel = new Vector2D(start, end);
    double fullDist = travel.dot(direction);
    TravelTurn retval = new TravelTurn();
    retval.start = (Point2D)start.clone();
    retval.runDirection = new Vector2D(direction);    
    if (fullDist < 0.0) { // Headed the wrong way
      retval.runTerminus = null;  // Turn ASAP
    } else if (fullDist == 0) { // Headed at right angle
      retval.runTerminus = null;  // Turn ASAP     
    } else {  // headed toward
      TravelResult res = analyzeTravel(src, start, direction, trg, end, linkID, okGroups, groupBlockers, true);
      retval.runTerminus = (Point2D)res.farthestPt.clone();
    }     
    fillOrthogonalVectors(retval, start, end);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a starting turn specification to boot the link search.  Used for recovery
  */
  
  TravelTurn buildInitRecoveryTurn(Vector2D direction, List<Point2D> threePts) {
 
    Point2D startPt = threePts.get(0);      
    TravelTurn retval = new TravelTurn();   
    retval.start = (Point2D)startPt.clone();
    
    if (threePts.size() == 1) {
      return (retval);
    }
    Point2D endPt = threePts.get(1);
    Point2D turnTarget = threePts.get(threePts.size() - 1);    
    
    // If recovery path is not starting ortho, we can't create a full turn.
    // Should drop down to jump mode before we need these fields:
    if (isOrthogonal(direction)) {
      retval.runDirection = new Vector2D(direction);
      retval.runTerminus = (Point2D)endPt.clone();
      fillOrthogonalVectors(retval, startPt, turnTarget);
    }
    return (retval);
  }
  
 /***************************************************************************
  **
  ** Build a turn specification used when transitioning from recovery mode
  ** to jump mode
  */
  
  @SuppressWarnings("unused")
  private TravelTurn buildTryTurn(List<Point2D> threePts, boolean force, Point2D turnTarget) {

    Point2D startPt = threePts.get(0);
    Point2D endPt = threePts.get(1);
    turnTarget.setLocation(threePts.get(threePts.size() - 1));
       
    TravelTurn retval = new TravelTurn();   
    retval.start = (Point2D)startPt.clone();
    // May not be canonical!
    retval.runDirection = new Vector2D(startPt, endPt).normalized();
    if (force) {
      retval.runDirection.scale(-1.0);
    }
    if (!isOrthogonal(retval.runDirection)) {
      retval.runDirection = getClosestCanonical(retval.runDirection);
      Vector2D toPt = new Vector2D(startPt, endPt);
      double runDot = retval.runDirection.dot(toPt);
      Vector2D delta = retval.runDirection.scaled(runDot);
      endPt = delta.add(startPt);
    }
    
    retval.runTerminus = (force) ? null : (Point2D)endPt.clone();
    fillOrthogonalVectors(retval, startPt, turnTarget);
    return (retval);
  }  
    
 /***************************************************************************
  **
  ** Build a turn specification used when transitioning from recovery mode
  ** to jump mode
  */
  
  private TravelTurn buildTryTurnToo(List<Point2D> threePts, boolean force, Point2D turnTarget, List<Point2D> prevJumpDetour) {
    boolean noDetour = ((prevJumpDetour == null) || prevJumpDetour.isEmpty());
    Point2D prePt;
    if (noDetour) {
      prePt = threePts.get(0);
    } else {
      Point2D detPt = prevJumpDetour.get(prevJumpDetour.size() - 2);
      Point dummyPt = new Point();
      prePt = new Point2D.Double();   
      pointConversion(detPt, prePt, dummyPt);   
    }
       
    Point2D startPt = threePts.get(1);
    Point2D endPt = threePts.get(2);
    turnTarget.setLocation(endPt);
       
    TravelTurn retval = new TravelTurn();   
    retval.start = (Point2D)startPt.clone();
    
    Vector2D preRunDirection = new Vector2D(prePt, startPt).normalized();
    Vector2D toPt = new Vector2D(startPt, endPt);
    Vector2D toPtNorm = toPt.normalized();
    
    // May not be canonical...Probably isn't!
    //
    // This method means we will take the "less direct" direction
    // if it means that we keep going the same direction as we arrived
    // in, as long as it is heading the right way.
    //
   
    Vector2D primary = null;
    boolean wentSecondBest = false;
    if (!isOrthogonal(toPtNorm)) {
      primary = getClosestCanonical(toPtNorm);
      Vector2D secondBest = getSecondClosestCanonical(toPtNorm);
      if (primary.equals(secondBest)) {  // shouldn't happen...
        retval.runDirection = primary;
      } else if (secondBest.equals(preRunDirection)) {      
        retval.runDirection = secondBest;
        wentSecondBest = true;
      } else {
        retval.runDirection = primary;
      }
    } else {
      retval.runDirection = toPtNorm;
    }
   
    //
    // Get the point for an ortho departure:
    //
    
    double runDot = retval.runDirection.dot(toPt);
    Vector2D delta = retval.runDirection.scaled(runDot);
    Point2D closestOrtho = delta.add(startPt);
    
    //
    // Is we are leaving the same way as we came in...don't!  Not relevant if
    // we were using the second best approach:
    //    
    
    if (!wentSecondBest && retval.runDirection.scaled(-1.0).equals(preRunDirection)) {
      retval.runDirection = (new Vector2D(closestOrtho, endPt)).normalized();
      if (retval.runDirection.isZero()) {
        return (null);
      }
      runDot = retval.runDirection.dot(toPt);
      delta = retval.runDirection.scaled(runDot);
      closestOrtho = delta.add(startPt); 
    }
    
    //
    // If we are forcing, try the reverse direction, unless we went second best.  If we did,
    // try primary:
    // BETTER YET:  two force levels to handle each of three directions!
    //
    
    boolean nullTerm = false;
    if (force) {
      if (!wentSecondBest) {
        retval.runDirection = getVector(getReverse(getDirection(retval.runDirection)));
        nullTerm = true;
      } else { // did go second best, so primary cannot be in preRunDirection!
        retval.runDirection = primary;
        runDot = retval.runDirection.dot(toPt);
        delta = retval.runDirection.scaled(runDot);
        closestOrtho = delta.add(startPt);
        nullTerm = false;
      }  // third case would be to reverse the primary even if we went second best...
    } 
    
    retval.runTerminus = (nullTerm) ? null : (Point2D)closestOrtho.clone();
    fillOrthogonalVectors(retval, startPt, turnTarget);
    return (retval);
  }   
 
  /***************************************************************************
  **
  ** Fill in the orthogonal vectors
  */
  
  private void fillOrthogonalVectors(TravelTurn turn, Point2D start, Point2D end) {
    Vector2D travel = new Vector2D(start, end);        
    Vector2D leftTurn = getTurn(turn.runDirection, LEFT);
    Vector2D rightTurn = getTurn(turn.runDirection, RIGHT);  
    double leftDot = leftTurn.dot(travel);
    double rightDot = rightTurn.dot(travel); 
    if (leftDot > 0.0) {
      turn.primaryTurnDirection = leftTurn;
      turn.secondaryTurnDirection = rightTurn;
      turn.isDirect = false;
    } else if (rightDot > 0.0) {
      turn.primaryTurnDirection = rightTurn;
      turn.secondaryTurnDirection = leftTurn;
      turn.isDirect = false;      
    } else if ((leftDot == 0) && (rightDot == 0)) {
      turn.primaryTurnDirection = leftTurn;
      turn.secondaryTurnDirection = rightTurn;
      // FIX ME???? Is moving directly away handled by offset case.
      if (travel.length() != 0.0) {
        turn.isDirect = getReverse(getDirection(turn.runDirection)) != getDirection(travel.normalized());
      } else {
        turn.isDirect = true;
      }
    } else {
      throw new IllegalStateException();
    }
    return;
  }

  /***************************************************************************
  **
  ** Return an ordered list of travel results for each step of travel
  ** to the given terminus.  We keep going until we hit a stop, or the first
  ** 100% zero-crossing travel result past the run terminus.    
  */
  
  private TravelOptions analyzeTravelsForRun(TravelTurn turn, String src, String trg, 
                                             String linkID, Set<String> okGroups, 
                                             Point2D end, boolean isStart) { 
    if (turn.isDirect) {
      return (analyzeTravelsForDirectRun(turn, src, trg, linkID, okGroups, end, isStart));
    } else {
      return (analyzeTravelsForOffsetRun(turn, src, trg, linkID, okGroups, end, isStart));
    }    
  }
  
  /***************************************************************************
  **
  ** Return an ordered list of travel results for each step of travel
  ** to the given terminus.  We keep going until we hit a stop, or the first
  ** 100% zero-crossing travel result past the run terminus.    
  */
  
  private TravelOptions analyzeTravelsForOffsetRun(TravelTurn turn, String src, String trg, String linkID, 
                                                   Set<String> okGroups, Point2D end, boolean directDepart) {
        
    TravelOptions retval = new TravelOptions();
    retval.travels = new ArrayList<TravelResultPair>();                            
    retval.turn = turn;
    retval.groupBlockers = (okGroups != null) ? new HashSet<String>() : null;

    WalkValues walk = getWalkValues(retval.turn.runDirection, retval.turn.start, null, 1);
    //
    // If heading in the wrong direction, turn as quickly as possible:
    //
    double runDist = 0.0;
    if (retval.turn.runTerminus != null) {
      Vector2D runVector = new Vector2D(retval.turn.start, retval.turn.runTerminus);
      runDist = runVector.length();
    }
    
    Vector2D postDetour = new Vector2D(retval.turn.runDirection);

    //
    // If we cannot turn at the point (e.g. fails last point), we enter a
    // null TravelResult in the point list
    //
         
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        if ((x == walk.startX) && (y == walk.startY)) {
          retval.travels.add(new TravelResultPair(null, null));
          continue;
        }
        GridContents gc = getGridContents(x, y);
        if (!checkForTravel(gc, src, trg, linkID, okGroups, walk, null, null, directDepart)) {
          return (retval);
        }
        if (!okForTurns(gc, src, trg, linkID, okGroups)) {
          retval.travels.add(new TravelResultPair(null, null));
          continue;
        }            
        Point2D currPt = new Point2D.Double(x, y);
        TravelResult result = analyzeTravel(src, currPt, retval.turn.primaryTurnDirection, 
                                           trg, end, linkID, okGroups, retval.groupBlockers, directDepart);
        // WJRL 5/16/08: If all else fails, we may need to have some detour options to work with:
        Point2D detEnd = postDetour.scaled(100.0).add(currPt);
        TravelResult resultS = analyzeDetourTravel(src, currPt, retval.turn.secondaryTurnDirection,
                                                   postDetour, trg, detEnd, linkID, okGroups, retval.groupBlockers, directDepart);        
        retval.travels.add(new TravelResultPair(result, resultS));
        Vector2D haveCovered = new Vector2D(retval.turn.start, currPt);
        if ((haveCovered.length() > runDist) &&
            ((!result.needATurn ||
              ((result.fraction == 1.0) &&
               (result.crossings == 0))))) {
          return (retval);
        }
      }
    }
    throw new IllegalStateException();
  } 
  
  /***************************************************************************
  **
  ** Return an ordered list of travel results for each step of travel
  ** to the given terminus.  We keep going until we hit a stop, or the first
  ** 100% zero-crossing travel result past the run terminus.    
  */
  
  private TravelOptions analyzeTravelsForDirectRun(TravelTurn turn, String src, 
                                                   String trg, String linkID, 
                                                   Set<String> okGroups,
                                                   Point2D end, boolean directDepart) {
    TravelOptions retval = new TravelOptions();
    retval.travels = new ArrayList<TravelResultPair>();                            
    retval.turn = turn;

    
    WalkValues walk = getWalkValues(retval.turn.runDirection, retval.turn.start, null, 1);
    //
    // If heading in the wrong direction, turn as quickly as possible, and detour is backwards:
    //    
    double runDist = 0.0;
    Vector2D postDetour = new Vector2D(retval.turn.runDirection);
    postDetour.scale(-1.0);
    if (retval.turn.runTerminus != null) {
      Vector2D runVector = new Vector2D(retval.turn.start, retval.turn.runTerminus);
      runDist = runVector.length();
      postDetour = retval.turn.runDirection;
    }

    HashSet<String> groupBlockers = (okGroups != null) ? new HashSet<String>() : null; 
    
    //
    // If we cannot turn at the point (e.g. fails last point), we enter a
    // null TravelResult in the point list
    //
         
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        Point2D currPt = new Point2D.Double(x, y);
        Vector2D haveCovered = new Vector2D(retval.turn.start, currPt);
        if ((x == walk.startX) && (y == walk.startY)) {
          retval.travels.add(new TravelResultPair(null, null));
          continue;
        }
       
        GridContents gc = getGridContents(x, y);
        if (!checkForTravel(gc, src, trg, linkID, okGroups, walk, groupBlockers, null, directDepart)) {
          return (retval);
        }
        if (!okForTurns(gc, src, trg, linkID, okGroups)) {
          retval.travels.add(new TravelResultPair(null, null));
          if ((runDist > 0.0) && (haveCovered.length() >= runDist)) {
            return (retval);
          }
          continue;
        }            
        
        TravelResult resultP = analyzeDetourTravel(src, currPt, retval.turn.primaryTurnDirection,
                                                   postDetour, trg, end, linkID, okGroups, groupBlockers, directDepart);
        TravelResult resultS = analyzeDetourTravel(src, currPt, retval.turn.secondaryTurnDirection,
                                                   postDetour, trg, end, linkID, okGroups, groupBlockers, directDepart);
        retval.travels.add(new TravelResultPair(resultP, resultS));        
        if (((runDist > 0.0) && (haveCovered.length() >= runDist)) ||
            ((runDist == 0.0) && ((resultP != null) || (resultS != null)))) {
          return (retval);
        }
      }
    }
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Generate a TravelOptions for a recovery run
  */
  
  private TravelOptions analyzeTravelsForRecoverRun(TravelTurn turn, String src, String trg, String linkID, 
                                                    Set<String> okGroups, List<Point2D> threePoints, 
                                                    Set<String> exemptions, boolean directDepart) {
        
    TravelOptions retval = new TravelOptions();
    retval.travels = new ArrayList<TravelResultPair>();                            
    retval.turn = turn;
    Point2D endPt = threePoints.get(1);
    Point2D turnTarget = (threePoints.size() == 3) ? threePoints.get(2) : null;
    retval.groupBlockers = (okGroups != null) ? new HashSet<String>() : null; 

    WalkValues walk = getWalkValues(turn.runDirection, turn.start, turn.runTerminus, 1);
    if (!checkWalkValues(walk)) {  // keep crazy calculations from blowing the stack
      return (retval);
    }
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        Point2D currPt = new Point2D.Double(x, y);
        if ((x == walk.startX) && (y == walk.startY)) {
          retval.travels.add(new TravelResultPair(null, null));
          continue;
        }
        GridContents gc = getGridContents(x, y);
        if (!checkForTravel(gc, src, trg, linkID, okGroups, walk, null, exemptions, directDepart)) {
          return (retval);
        }
        if (currPt.equals(endPt)) {
          TravelResult result;
          if (turnTarget == null) {
            result = new TravelResult(false, (Point2D)endPt.clone(), 1.0, 0, 0.0, 0.0, null, false);
          } else {
            Vector2D turnVec = new Vector2D(endPt, turnTarget);
            result = new TravelResult(true, (Point2D)turnTarget.clone(), 1.0, 0, 0.0, turnVec.length(), null, false);
          }
          retval.travels.add(new TravelResultPair(result, null));
          return (retval);
        } else {
          retval.travels.add(new TravelResultPair(null, null));
        }
      }
    }
    return (retval);
  }   
  
 
  /***************************************************************************
  **
  ** Chop loops out
  */

  private int chopLoops(List<Point2D> points, Point2D start, List<Point2D> retList) { 
    int numPoints = points.size();
    if (numPoints < 3) {
      return (NO_CLEANUP_);
    }
    int chopi = -1;
    int chopj = -1;
    Point2D chopPoint = null;
    Point2D lastOuterPoint = start;
    for (int i = 0; i < numPoints - 2; i++) {
      Point2D nextOuterPoint = points.get(i);
      Point2D lastInnerPoint = points.get(i + 1);       
      for (int j = i + 2; j < numPoints; j++) {
        Point2D nextInnerPoint = points.get(j);
        Point2D inter = lineIntersection(lastOuterPoint, nextOuterPoint, lastInnerPoint, nextInnerPoint);
        if (inter != null) {      
          chopi = i;
          chopj = j;
          chopPoint = inter;
          break;
        }
        lastInnerPoint = nextInnerPoint;
      }
      lastOuterPoint = nextOuterPoint;
    }
    
    if (chopPoint != null) {
      retList.clear();
      for (int i = 0; i < chopi; i++) {
        retList.add(points.get(i));
      }
      retList.add(chopPoint);
      for (int j = chopj; j < numPoints; j++) {
        retList.add(points.get(j));    
      }
      return (DID_CLEANUP_);
    }
   
    return (NO_CLEANUP_);
  }  
  
 /***************************************************************************
  **
  ** Chop wandering paths out
  */

  private List<Point2D> chopToShorterBackward(List<Point2D> points, Point2D start, String src,
                                     String trg, Set<String> okGroups) { 
    int numPoints = points.size();
    if (numPoints < 3) {
      return (null);
    }
    
    int chopi = -1;
    int chopj = -1;   
    Point2D chopPoint = null;
    Point2D lastOuterPoint = points.get(numPoints - 1);
    for (int i = numPoints - 2; (i > 0) && (chopPoint == null); i--) {
      Point2D nextOuterPoint = points.get(i);
      if (nextOuterPoint.equals(lastOuterPoint) || !isOrthogonal(nextOuterPoint, lastOuterPoint)) {
        lastOuterPoint = nextOuterPoint;
        continue;
      }
      Vector2D segt = new Vector2D(lastOuterPoint, nextOuterPoint);
      double segtLen = segt.length();
      Vector2D segta = segt.normalized();
      segta.scale(segtLen + 300.0);
      Point2D nextScaledOuterPoint = segta.add(lastOuterPoint);
         
      Point2D lastInnerPoint = start;     
      for (int j = 0; (j < i) && (chopPoint == null); j++) {
        Point2D nextInnerPoint = points.get(j);
        if (nextInnerPoint.equals(lastInnerPoint) || !isOrthogonal(nextInnerPoint, lastInnerPoint)) {
          lastInnerPoint = nextInnerPoint;
          continue;
        }            
        Point2D inter = lineIntersection(lastOuterPoint, nextScaledOuterPoint, lastInnerPoint, nextInnerPoint);
        if (inter != null) {
          if (haveChopWillTravel(nextOuterPoint, lastOuterPoint, inter, src, trg, okGroups, i == 0)) {
            chopi = i;
            chopj = j;
            chopPoint = inter;
            break;
          }
        }
        lastInnerPoint = nextInnerPoint;
      }
      lastOuterPoint = nextOuterPoint;
    }
    
    if (chopPoint != null) {
      List<Point2D> retval = new ArrayList<Point2D>();
      for (int j = 0; j < chopj; j++) {
        retval.add(points.get(j));    
      }
      // can get dups if the chop point equals an existing corner:
      if ((retval.size() == 0) || (!chopPoint.equals(retval.get(retval.size() - 1)))) {
        retval.add(chopPoint);
      }
      for (int i = chopi; i < numPoints; i++) {
        retval.add(points.get(i));
      }
      retval = dropCollinearPoints(retval);
      if (retval.equals(points)) {
        return (null);
      }
      return (retval);
    }
   
    return (null);
  }  
  
 /***************************************************************************
  **
  ** Chop wandering paths out
  */

  private List<Point2D> chopToShorterForward(List<Point2D> points, Point2D start, String src,
                                             String trg, Set<String> okGroups) { 
    int numPoints = points.size();
    if (numPoints < 3) {
      return (null);
    }
    int chopi = -1;
    int chopj = -1;   
    Point2D chopPoint = null;
    Point2D lastOuterPoint = start;
    int starti = (collinearStart(points, start)) ? 1 : 0;    
    
    for (int i = starti; (i < numPoints - 2) && (chopPoint == null); i++) {
      Point2D nextOuterPoint = points.get(i);
      if (nextOuterPoint.equals(lastOuterPoint) || !isOrthogonal(nextOuterPoint, lastOuterPoint)) {
        lastOuterPoint = nextOuterPoint;
        continue;
      }
      Vector2D segt = new Vector2D(lastOuterPoint, nextOuterPoint);
      double segtLen = segt.length();
      Vector2D segta = segt.normalized();
      segta.scale(segtLen + 300.0);
      Point2D nextScaledOuterPoint = segta.add(lastOuterPoint);
      
      Point2D lastInnerPoint = points.get(numPoints - 1);     
      for (int j = numPoints - 2; (j > i) && (chopPoint == null); j--) {
        Point2D nextInnerPoint = points.get(j);
        if (nextInnerPoint.equals(lastInnerPoint) || !isOrthogonal(nextInnerPoint, lastInnerPoint)) {
          lastInnerPoint = nextInnerPoint;
          continue;
        }
        Point2D inter = lineIntersection(lastOuterPoint, nextScaledOuterPoint, lastInnerPoint, nextInnerPoint);
        if (inter != null) {
          if (haveChopWillTravel(nextOuterPoint, lastOuterPoint, inter, src, trg, okGroups, i == 0)) { 
            chopi = i;
            chopj = j;
            chopPoint = inter;
            break;
          }
        }
        lastInnerPoint = nextInnerPoint;
      }
      lastOuterPoint = nextOuterPoint;
    }
    
    if (chopPoint != null) {
      List<Point2D> retval = new ArrayList<Point2D>();
      for (int i = 0; i <= chopi; i++) {
        retval.add(points.get(i));    
      }
      // can get dups if the chop point equals an existing corner:
      if (!chopPoint.equals(retval.get(retval.size() - 1))) {
        retval.add(chopPoint);
      }
      for (int j = chopj + 1; j < numPoints; j++) {
        retval.add(points.get(j));
      }
      retval = dropCollinearPoints(retval);
      if (retval.equals(points)) {
        return (null);
      }
      return (retval);
    }
   
    return (null);
  } 

  /***************************************************************************
  **
  ** Gotta ignore collinear starts
  */
  
  private boolean collinearStart(List<Point2D> points, Point2D start) {
    if (points.size() < 2) {
      return (false);
    }
    Point2D pt1 = points.get(0);
    Point2D pt2 = points.get(1);
    return (collinearPoints(start, pt1, pt2));
  }
  
  /***************************************************************************
  **
  ** Gotta find colinear triples
  */
  
  private boolean collinearPoints(Point2D start, Point2D pt1, Point2D pt2) {
    double x0 = start.getX();
    double x1 = pt1.getX(); 
    double x2 = pt2.getX();
    double y0 = start.getY();
    double y1 = pt1.getY();
    double y2 = pt2.getY();
    return (((x0 == x1) && (x0 == x2)) || ((y0 == y1) && (y0 == y2))); 
  }  
 
 /***************************************************************************
  **
  ** Gotta drop the middle point of collinear triples!
  */
  
  private List<Point2D> dropCollinearPoints(List<Point2D> points) {

    int numPoints = points.size();
    if (numPoints < 3) {
      return (points);
    }
    
    ArrayList<Point2D> retval = new ArrayList<Point2D>();    
    retval.add(points.get(0));
    
    for (int i = 0; i < (numPoints - 2); i++) {
      Point2D pt0 = points.get(i);
      Point2D pt1 = points.get(i + 1);
      Point2D pt2 = points.get(i + 2);
      if (!collinearPoints(pt0, pt1, pt2)) {
        retval.add(pt1);
      }
    }
    retval.add(points.get(numPoints - 1));
    return (retval);
  }  
  

 /***************************************************************************
  **
  ** Sometimes the algorithm launches from the "nearest point" to the target,
  ** but ends up running almost next to existing links for a way before 
  ** branching off.  See if we have this case, and chop accordingly.
  */

  private List<Point2D> lookForAlternateLaunch(List<Point2D> points, Point2D start, String src, String trg,
                                               Set<String> okGroups, NewCorner changedStart, Set<String> linksFromSource) {

    int numPoints = points.size();
    if (numPoints < 3) {
      return (null);
    }    
    
    ArrayList<Point2D> allPoints = new ArrayList<Point2D>(points);
    allPoints.add(0, start);
    numPoints++;
    
    int chopi = -1;
    Point2D chopPoint = null;
    Point2D checkPt = new Point2D.Double();
    Point2D lastOuterPoint = allPoints.get(numPoints - 1);
    for (int i = numPoints - 2; (i >= 0) && (chopPoint == null); i--) {
      Point2D nextOuterPoint = allPoints.get(i);
      double lopX = lastOuterPoint.getX();
      double lopY = lastOuterPoint.getY();
      double nopX = nextOuterPoint.getX();
      double nopY = nextOuterPoint.getY();            
      int direct = getDirectionFromCoords(lopX, lopY, nopX, nopY);
      if ((direct == NONE) || (direct == DIAGONAL)) {
        lastOuterPoint = nextOuterPoint;
        continue;
      }
      Vector2D runVec = (new Vector2D(lastOuterPoint, nextOuterPoint)).normalized();
      Vector2D goRight = getTurn(runVec, RIGHT);
      Vector2D goLeft = getTurn(runVec, LEFT);
      
      Point startPt = new Point();
      Point2D startPtD = new Point2D.Double();
      pointConversion(lastOuterPoint, startPtD, startPt);
      
      Point endPt = new Point();
      Point2D endPtD = new Point2D.Double();
      pointConversion(nextOuterPoint, endPtD, endPt);
      
      WalkValues walk = getWalkValues(runVec, startPt, endPt, 1);
      for (int xc = walk.startX; (xc != walk.endX) && (chopPoint == null); xc += walk.incX) {
        for (int yc = walk.startY; (yc != walk.endY) && (chopPoint == null); yc += walk.incY) {
          checkPt.setLocation(xc, yc);
          if (checkPt.equals(startPt) || checkPt.equals(endPt)) {
            continue;
          }
          Point2D ugCheckPoint = new Point2D.Double(checkPt.getX() * 10.0, checkPt.getY() * 10.0);
          for (int j = 1; j < 4; j++) { 
            Vector2D scaledGoLeft = goLeft.scaled((double)j);
            Point2D leftPoint = scaledGoLeft.add(checkPt);
            GridContents gcl = getGridContents((int)leftPoint.getX(), (int)leftPoint.getY());
            int cellType = lookForLaunchCell(src, gcl, okGroups, linksFromSource);
            if (cellType != IS_EMPTY) {
              Point2D ugLeftPoint = new Point2D.Double(leftPoint.getX() * 10.0, leftPoint.getY() * 10.0);                 
              if (haveChopWillTravel(ugLeftPoint, ugCheckPoint, ugLeftPoint, src, trg, okGroups, false)) { 
                Vector2D newDistVec = new Vector2D(ugLeftPoint, ugCheckPoint);
                double newDist = newDistVec.length();
                if (newDist < traceLength(start, ugCheckPoint, points, i)) {               
                  changedStart.type = cellType;
                  changedStart.point = leftPoint;
                  changedStart.dir = getReverse(getDirection(goLeft));
                  chopPoint = (Point2D)ugCheckPoint.clone();
                  chopi = i;
                  break;
                }
              }
            }
            Vector2D scaledGoRight = goRight.scaled((double)j);
            Point2D rightPoint = scaledGoRight.add(checkPt);
            GridContents gcr = getGridContents((int)rightPoint.getX(), (int)rightPoint.getY());
            cellType = lookForLaunchCell(src, gcr, okGroups, linksFromSource);
            if (cellType != IS_EMPTY) {
              Point2D ugRightPoint = new Point2D.Double(leftPoint.getX() * 10.0, leftPoint.getY() * 10.0);
              if (haveChopWillTravel(ugRightPoint, ugCheckPoint, ugRightPoint, src, trg, okGroups, false)) {
                Vector2D newDistVec = new Vector2D(ugRightPoint, ugCheckPoint);
                double newDist = newDistVec.length();
                if (newDist < traceLength(start, ugCheckPoint, points, i)) {               
                  changedStart.type = cellType;
                  changedStart.point = rightPoint;
                  changedStart.dir = getReverse(getDirection(goLeft));
                  chopPoint = (Point2D)ugCheckPoint.clone();
                  chopi = i;
                  break;
                }
              }
            }
          }
        }
      }
      lastOuterPoint = nextOuterPoint;
    }    
    
    numPoints = points.size();
    if (chopPoint != null) {
      List<Point2D> retval = new ArrayList<Point2D>();
      retval.add(chopPoint);
      for (int j = chopi; j < numPoints; j++) {
        retval.add(points.get(j));
      }
      if (retval.equals(points)) {
        return (null);
      }    
      return (retval);
    }
   
    return (null);
  }
  
 /***************************************************************************
  **
  ** Get length along path to a chop point
  */

  private double traceLength(Point2D start, Point2D chop, List<Point2D> pointList, int lastIndex) {
    int numPoints = pointList.size();
    if ((lastIndex < 0) || (numPoints == 0)) {
      return ((new Vector2D(start, chop)).length()); 
    }
    Point2D lastPoint = pointList.get(0);
    double retval = (new Vector2D(start, lastPoint)).length();  
    for (int i = 1; i <= lastIndex; i++) {
      Point2D nextPoint = pointList.get(i);
      retval +=  (new Vector2D(lastPoint, nextPoint)).length();
      lastPoint = nextPoint;
    }
    retval +=  (new Vector2D(lastPoint, chop)).length();
    return (retval);
  }
 
  
 /***************************************************************************
  **
  ** Get intersecting point
  */

  private Point2D lineIntersection(Point2D lastOuterPoint, Point2D nextScaledOuterPoint,
                                   Point2D lastInnerPoint, Point2D nextInnerPoint) {
    
     return (UiUtil.lineIntersection(lastOuterPoint, nextScaledOuterPoint,
                                     lastInnerPoint, nextInnerPoint));
  }    
  
 /***************************************************************************
  **
  ** Can a chop path cross the grid without hitting something?
  */

  private boolean haveChopWillTravel(Point2D runPt, Point2D firstPt, Point2D lastPt, 
                                     String src, String trg, Set<String> okGroups, boolean isStart) { 
    Vector2D seg1 = new Vector2D(firstPt, runPt);
    Vector2D runVec = (new Vector2D(seg1)).normalized();
    Point startPt = new Point();
    Point2D startPtD = new Point2D.Double();
    pointConversion(firstPt, startPtD, startPt);

    Point endPt = new Point();
    Point2D endPtD = new Point2D.Double();
    pointConversion(lastPt, endPtD, endPt);
    Point chkPt = new Point();

    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);
    for (int xc = walk.startX; xc != walk.endX; xc += walk.incX) {
      for (int yc = walk.startY; yc != walk.endY; yc += walk.incY) {
        chkPt.setLocation(xc, yc);
        if (chkPt.equals(startPt) || chkPt.equals(endPt)) {
          continue;
        }
        GridContents gc = getGridContents(xc, yc);
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, null, okGroups, walk, null, null, isStart, true);
        if (currentCrossings < 0) {
          return (false);
        }
      }
    }
    return (true);
  } 

 /***************************************************************************
  **
  ** Answer if the next segment needs to be made orthogonal:
  */

  private boolean needOrthoRecovery(RecoveryDataForLink recoverForLink, int index) {
    int pc = recoverForLink.getPointCount(); 
    // Fix for BT-03-16-10:3 Path with two incoming diagonal link segments before the
    // node was insisting it needed final ortho link.  Wrong!  Return the final
    // inbound ortho, if there is one.  If not, just say no to avoid the artifical
    // introduction of an nonexistent ortho constraint.
    if (index >= pc) {
      if (pc == 0) {
        return (false);
      } else {
        return (recoverForLink.getInboundOrtho(pc - 1));
      }
      //return (true);  // FIX ME? is this appropriate?  NO, it is bogus!  See fix above...
    }
    return (recoverForLink.getInboundOrtho(index));
  }
  
  /***************************************************************************
  **
  ** Answer if the next segment has some wiggle room:
  */

  BusProperties.CornerDoF getCornerDoF(RecoveryDataForLink recoverForLink, int index) {
    if (index >= recoverForLink.getPointCount()) {
      return (null);
    }
    return (recoverForLink.getCornerDoF(index));
  }

  /***************************************************************************
  **
  ** Build the list of relevant points for the next step.
  ** Size < 3 if it is direct.
  */

  List<Point2D> buildRecoverPointList(Point2D start, Point2D end, RecoveryDataForLink recoverForLink, int index) {
     
    Point2D firstPoint;
    Point2D secondPoint = null;
    Point2D thirdPoint = null;
    Point dummyPt = new Point();
    if (index < 0) {
      throw new IllegalArgumentException();
    }
    int epSize = recoverForLink.getPointCount();
    if (epSize == 0) { // No points is a direct special case:
      if (index > 0) { // We are starting with the end point:
        firstPoint = new Point2D.Double(end.getX(), end.getY());
      } else {
        firstPoint = new Point2D.Double(start.getX(), start.getY());
        secondPoint = new Point2D.Double(end.getX(), end.getY());
      }
    } else if (index == 0) {  // First time out requires start point in list:
      firstPoint = new Point2D.Double(start.getX(), start.getY());
      secondPoint = new Point2D.Double();
      pointConversion(recoverForLink.getPoint(0), secondPoint, dummyPt);
      if (epSize == 1) {
        thirdPoint = new Point2D.Double(end.getX(), end.getY());
      } else {
        thirdPoint = new Point2D.Double();
        pointConversion(recoverForLink.getPoint(1), thirdPoint, dummyPt);
      }  
    } else if (index == epSize) {  // Turning for home, we only have two points:
      firstPoint = new Point2D.Double();
      pointConversion(recoverForLink.getPoint(index - 1), firstPoint, dummyPt);
      secondPoint = end;
    } else if (index > epSize) {
      return (null);
    } else { // General case
      firstPoint = new Point2D.Double();
      pointConversion(recoverForLink.getPoint(index - 1), firstPoint, dummyPt);
      secondPoint = new Point2D.Double();
      pointConversion(recoverForLink.getPoint(index), secondPoint, dummyPt);
      if (index == epSize - 1) {
        thirdPoint = new Point2D.Double(end.getX(), end.getY());
      } else {
        thirdPoint = new Point2D.Double();
        pointConversion(recoverForLink.getPoint(index + 1), thirdPoint, dummyPt);
      }  
    }

    ArrayList<Point2D> retval = new ArrayList<Point2D>();
    retval.add((Point2D)firstPoint.clone());
    if (secondPoint != null) {
      retval.add((Point2D)secondPoint.clone());
    }
    if (thirdPoint != null) {
      retval.add((Point2D)thirdPoint.clone());
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the list of relevant point keys for the next step.  KEEP IN SYNC
  ** with above approach!
  */

  @SuppressWarnings("unused")
  private List<LinkSegmentID> buildRecoverKeyList(RecoveryDataForLink recoverForLink, int index) {
     
    LinkSegmentID firstPoint;
    LinkSegmentID secondPoint;
    LinkSegmentID thirdPoint = null;
 
    int epSize = recoverForLink.getPointCount();
    if (epSize == 0) { // No points is a direct special case:
      if (index > 0) {
        throw new IllegalArgumentException();
      }
      firstPoint = null;
      secondPoint = null;
    } else if (index == 0) {  // First time out requires start point in list:
      firstPoint = null;
      secondPoint = recoverForLink.getPointKey(0);
      if (epSize == 1) {
        thirdPoint = null;
      } else {
        thirdPoint =  recoverForLink.getPointKey(1);
      }  
    } else if (index == epSize) {  // Turning for home, we only have two points:
      firstPoint = recoverForLink.getPointKey(index - 1);
      secondPoint = null;
    } else if (index > epSize) {
      return (null);
    } else { // General case
      firstPoint = recoverForLink.getPointKey(index - 1);
      secondPoint = recoverForLink.getPointKey(index);
      if (index == epSize - 1) {
        thirdPoint = null;
      } else {
        thirdPoint = recoverForLink.getPointKey(index + 1);
      }  
    }
 
    ArrayList<LinkSegmentID> retval = new ArrayList<LinkSegmentID>();
    retval.add(firstPoint);
    retval.add(secondPoint);
    retval.add(thirdPoint);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answer if the point list is ortho
  */

  boolean recoverPointListIsOrtho(List<Point2D> threePoints) {
     
    Point2D firstPoint = threePoints.get(0);
    Point2D secondPoint = threePoints.get(1);
    Point2D thirdPoint = (threePoints.size() == 3) ? threePoints.get(2) : null;
    
    if (!isOrthogonal(firstPoint, secondPoint)) {
      return (false);
    }   
    
    if (thirdPoint != null) {
      if (!secondPoint.equals(thirdPoint) && !isOrthogonal(secondPoint, thirdPoint)) {
        return (false);
      }
    }
    return (true);
    
  } 
  
  /***************************************************************************
  **
  ** Answer if the point list starts ortho
  */

  boolean recoverPointListStartsOrtho(List<Point2D> threePoints) {
     
    Point2D firstPoint = threePoints.get(0);
    Point2D secondPoint = threePoints.get(1);
    
    return (isOrthogonal(firstPoint, secondPoint));
  }   
  
  /***************************************************************************
  **
  ** Take a path step.
  */

  private int recoverStep(String src, String trg, String linkID, Point2D targ, Vector2D startDir, Vector2D finalDir, 
                          TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                          GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                          Set<String> okGroups, int entryPref, 
                          RecoveryDataForLink recoverForLink, 
                          Set<String> recoveryExemptions, 
                          List<Point2D> prevJumpDetour, boolean strictOKGroups) 
    throws NonConvergenceException {
    
    //
    // Are we there yet?  If yes, we are done:
    //
   
    if (turn.start.equals(targ)) {
      if (currDepth == 0) {  // I.e. the first
        addTempEntryForCorner(turn.start, src, getDirection(startDir), getDirection(finalDir));
      } else {
        addTempEntryForFinalCorner(turn.start, src, getDirection(finalDir));
      }
      return (RECOV_STEP_CORE_OK_);
    }
   
    if (currDepth == maxDepth) {
      throw new NonConvergenceException();
    }
    checker.bump();
    
    //
    // We are not starting anymore once the depth goes up...
    
    isStart = isStart && (currDepth == 0);
    
    //
    // Ways to go:
    // 1) If the next segment is orthogonal, we continue with straightforward recovery recursion.  
    //    If that works, we are golden.  If it fails, we need to jump to the next recovery point
    //    before continuing to do recursive recovery.
    // 2) If the next segment is supposed to be orthogonal, and the points we have in hand are
    //    not, just go to jump and then continue recursion.
    // 3) If the next segment is not orthogonal, and the original was not either, we just step
    //    forward in a non-orthogonal fashion before continuing recursion.
    //
      
    boolean gottaBeOrtho = needOrthoRecovery(recoverForLink, currDepth);
    boolean cornerGottaBeOrtho = needOrthoRecovery(recoverForLink, currDepth + 1);    
    
    RecoveryDOFAnalyzer dofa = new RecoveryDOFAnalyzer(this, gottaBeOrtho, cornerGottaBeOrtho, 
                                                       recoverForLink, currDepth, targ, turn, src, trg, linkID,
                                                       okGroups, recoveryExemptions, isStart);    
    List<RecoveryDOFAnalyzer.DOFOptionPair> dofOptions = dofa.generateDOFOptions();
    //
    // If there are no DOF options, we just proceed with what we have.  If there are DOF options, we
    // try them out, taking the first that works.  If none work, we fall back to the original plan
    //
    
    int numOptions = dofOptions.size();
    int lastRetval = RECOV_STEP_NO_CODE_;
        
    for (int i = 0; i <= numOptions; i++) {  // that's right; we include i == numOptions (the no DOF case)
      RecoveryDOFAnalyzer.DOFOptionPair optionPair = (i != numOptions) ? dofOptions.get(i) : null;
      DOFState state = installDOFOption(optionPair, recoverForLink, currDepth, targ, turn);
      boolean haveOrthoPoints = state.haveOrthoPoints;
      List<Point2D>  threePts = state.threePts;
      turn = state.turn;

      boolean trySimpleRecovery;
      boolean fallbackToJump;
      boolean goDirectDiagonal;

      if (gottaBeOrtho || haveOrthoPoints) { 
        trySimpleRecovery = haveOrthoPoints;
        fallbackToJump = true;
        goDirectDiagonal = false;
      } else {
        trySimpleRecovery = false;
        fallbackToJump = false;
        goDirectDiagonal = true;
      }

      try {
        lastRetval = recoverStepPhaseTwo(threePts, trySimpleRecovery, fallbackToJump, goDirectDiagonal,
                                         src, trg, linkID, targ, startDir, finalDir, 
                                         turn, points, maxDepth, currDepth,
                                         params, checker, isStart, force, okGroups, entryPref, recoverForLink, 
                                         recoveryExemptions, prevJumpDetour, strictOKGroups);
        
        if (lastRetval == RECOV_STEP_CORE_OK_) {
          return (RECOV_STEP_CORE_OK_);  
        }

      } catch (NonConvergenceException ncex) {
        // pop the proposed DOF changes, if any
        dropDOFOption(optionPair, recoverForLink);
        throw ncex;
      }
      dropDOFOption(optionPair, recoverForLink);
    }
    
    return (lastRetval);    
  }
  
  /***************************************************************************
  **
  ** Take a path step.
  */

  private int recoverStepPhaseTwo(List<Point2D> threePts, boolean trySimpleRecovery, boolean fallbackToJump, boolean goDirectDiagonal,
                                  String src, String trg, String linkID, Point2D targ, Vector2D startDir, Vector2D finalDir, 
                                  TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                                  GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                                  Set<String> okGroups, int entryPref, 
                                  RecoveryDataForLink recoverForLink, Set<String> exemptions, 
                                  List<Point2D> prevJumpDetour, boolean strictOKGroups) 
                                    throws NonConvergenceException {
    
    //
    // If we can get to the next point via recovery, then do it.  If we cannot, then use the
    // search approach to get there, then pick up from there.
    //
    SimpleRecoveryResult simpleResult = null;
    if (trySimpleRecovery) {
      simpleResult = trySimpleRecovery(src, trg, linkID, targ, startDir, finalDir, 
                                       turn, points, maxDepth, currDepth,
                                       params, checker, isStart, force,
                                       okGroups, entryPref, recoverForLink, threePts, strictOKGroups);
      if (simpleResult.exitWithCode) {
        return (simpleResult.exitCode);
      }
      if (simpleResult.recovResult == RECOV_STEP_CORE_NO_TURN_NO_JUMP_) {
        currDepth++;
        fallbackToJump = false;
      }
    }
 
    RecoveryJumpResult jumpResult = null;
    if (fallbackToJump) { 
      //
      // At this point, we either didn't bother to try simple recovery, or we failed
      // (either fully or just being able to then turn the corner).  So go to a jump 
      // Process the simple recovery info and get ready for the jump:
      //
      JumpPreparations jumpPrep = prepareForJump(simpleResult, turn, targ, recoverForLink, 
                                                 threePts, currDepth, src, prevJumpDetour, 
                                                 isStart, startDir, okGroups, linkID, trg);
      if (jumpPrep.exitWithCode) {
        return (jumpPrep.exitCode);
      }
      currDepth = jumpPrep.currDepth;

      //
      // Try to get to the next recovery point via a jump:
      //
    
      try {
        jumpResult = handleRecoveryJump(src, trg, linkID, targ, jumpPrep.jumpTarg, 
                                        startDir, finalDir, turn, jumpPrep.tryTurn,
                                        maxDepth, currDepth, params, checker, 
                                        (simpleResult == null) ? RECOV_STEP_NO_CODE_ : simpleResult.recovResult, 
                                        threePts, isStart, force, okGroups, entryPref, 
                                        recoverForLink, prevJumpDetour, strictOKGroups);
      } catch (NonConvergenceException ncex) {
        if ((simpleResult != null) && (simpleResult.noTurnPlan != null)) {
          removeTempForPlan(simpleResult.noTurnPlan);
        }
        throw ncex;
      }    
      if (jumpResult.exitWithCode) {
        if (jumpResult.exitCode == RECOV_STEP_CORE_OK_) {
          installSuccessfulPaths(src, jumpResult, simpleResult, null, recoverForLink, points);
        } else {
          backOutTempPlans(jumpResult, simpleResult, null);
        }        
        return (jumpResult.exitCode);
      }
      currDepth = jumpResult.currDepth;
      threePts = jumpResult.threePts;
    }
    List<Point2D> jumpDetour = (jumpResult == null) ? null : jumpResult.jumpDetour;
    
    //
    // Just go diagonal to the next recovery point:
    //
    
    DiagonalResult diagResult = null;
    if (goDirectDiagonal) {
      diagResult = handleDiagonalStepJump(src, targ, turn, currDepth, points, threePts, recoverForLink); 
      if (diagResult.exitWithCode) {
        return (diagResult.exitCode);
      }
      currDepth = diagResult.currDepth;
      threePts = diagResult.threePts;
    } 
    
    //
    // Having gone through the needed detour to get to the next recovery point, now
    // continue recovery recursion:
    //
    
    // If we are about to hit the end, there is only one point in the list:
    Point2D startPt = threePts.get(0);
    Vector2D departDir = null;
    if (threePts.size() > 1) {
      Point2D endPt = threePts.get(1);
      departDir = (new Vector2D(startPt, endPt)).normalized();
    }
    TravelTurn pickupTurn = buildInitRecoveryTurn(departDir, threePts); 
    int retval;
    try {
      retval = recoverStep(src, trg, linkID, targ, startDir, finalDir, pickupTurn, points, maxDepth, currDepth,
                           params, checker, isStart, force, okGroups, entryPref, 
                           recoverForLink, exemptions, jumpDetour, strictOKGroups);
      if (retval == RECOV_STEP_CORE_OK_) {
        installSuccessfulPaths(src, jumpResult, simpleResult, diagResult, recoverForLink, points);
        return (RECOV_STEP_CORE_OK_);  
      }
    } catch (NonConvergenceException ncex) {
      backOutTempPlans(jumpResult, simpleResult, diagResult);
      throw ncex;
    } 
    
    backOutTempPlans(jumpResult, simpleResult, diagResult);
    return (retval);
  } 
  


  /***************************************************************************
  **
  ** Drop a DOF option
  */

  private void dropDOFOption(RecoveryDOFAnalyzer.DOFOptionPair optionPair, RecoveryDataForLink recoverForLink) { 
    if (optionPair == null) {
      return;
    }
    if (optionPair.firstPoint != null) {
      recoverForLink.popRevisedPoint(optionPair.firstPoint.lsid);
    }   
    if (optionPair.cornerPoint != null) {
      recoverForLink.popRevisedPoint(optionPair.cornerPoint.lsid);    
    }   
    return;  
  }  
  

  /***************************************************************************
  **
  ** Can a DOF path cross the grid without hitting something?
  */

  boolean haveDOFWillTravel(List<Point2D> threePts, boolean checkCorner, String src, String trg, String linkID,
                            Set<String> okGroups, Set<String> recoveryExemptions, boolean isStart) { 

    if (checkCorner && (threePts.size() != 3)) {
      return (false);
    }
    
    // FIX ME!! CHECK EXTENSION OF EXISTING RUN TOO!
    
    Point2D startPtD = (checkCorner) ? threePts.get(1) : threePts.get(0);
    Point2D endPtD = (checkCorner) ? threePts.get(2) : threePts.get(1);    
    Point2D chkPtD = new Point2D.Double();
    Vector2D runVec = (new Vector2D(startPtD, endPtD)).normalized();
    if (runVec.isZero()) {
      return (false);
    }

    WalkValues walk = getWalkValues(runVec, startPtD, endPtD, 1);
    for (int xc = walk.startX; xc != walk.endX; xc += walk.incX) {
      for (int yc = walk.startY; yc != walk.endY; yc += walk.incY) {
        chkPtD.setLocation(xc, yc);
        
        if (chkPtD.equals(startPtD) || chkPtD.equals(endPtD)) {
          continue;
        }
        GridContents gc = getGridContents(xc, yc);
        //
        // We do not allow recovery exemptions for paths that are changed from the original path:
        int currentCrossings = checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walk, null, null, isStart, false);
        if (currentCrossings < 0) {
          return (false);
        }
      }
    }
    return (true);
  }  
  
  
  /***************************************************************************
  **
  ** Is a given point reachable (in the immediate vicinity)
  */

  private boolean isReachable(Point2D chkPtD, String src, String trg, String linkID,
                              Set<String> okGroups, Set<String> recoveryExemptions, boolean isStart) { 


    if (linkIsPresentAtPoint(src, new Point((int)chkPtD.getX(), (int)chkPtD.getY()))) {  // If link is there, skip the test...
      return (true);
    }
    
    Vector2D downVec = getVector(SOUTH);
    Vector2D upVec = getVector(NORTH);
    Vector2D leftVec = getVector(WEST);
    Vector2D rightVec = getVector(EAST);
    
    Point2D startAbove = upVec.add(chkPtD);
    Point2D startBelow = downVec.add(chkPtD);
    Point2D startLeft = leftVec.add(chkPtD);
    Point2D startRight = rightVec.add(chkPtD);
       
    WalkValues walkDown = getWalkValues(downVec, startAbove, chkPtD, 1);
    WalkValues walkUp = getWalkValues(upVec, startBelow, chkPtD, 1);    
    WalkValues walkLeft = getWalkValues(leftVec, startRight, chkPtD, 1);
    WalkValues walkRight = getWalkValues(rightVec, startLeft, chkPtD, 1);    
    
    int xc = (int)chkPtD.getX();
    int yc = (int)chkPtD.getY();
    
    // Upper Left
    GridContents gc = getGridContents(xc - 1, yc - 1);  
  //  boolean uld = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkDown, null, recoveryExemptions, isStart, false) >= 0);
  //  boolean ulr = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkRight, null, recoveryExemptions, isStart, false) >= 0);
    // above
    gc = getGridContents(xc, yc - 1);  
    boolean ad = (checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walkDown, null, recoveryExemptions, isStart, false) >= 0);
    // upper right
    gc = getGridContents(xc + 1, yc - 1);  
 //   boolean urd = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkDown, null, recoveryExemptions, isStart, false) >= 0);
 //   boolean url = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkLeft, null, recoveryExemptions, isStart, false) >= 0);
    // left
    gc = getGridContents(xc - 1, yc); 
    boolean lr = (checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walkRight, null, recoveryExemptions, isStart, false) >= 0);
    // right
    gc = getGridContents(xc + 1, yc);  
    boolean rl = (checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walkLeft, null, recoveryExemptions, isStart, false) >= 0);
    // lower left
    gc = getGridContents(xc - 1, yc + 1);  
 //   boolean llu = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkUp, null, recoveryExemptions, isStart, false) >= 0);
 //   boolean llr = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkRight, null, recoveryExemptions, isStart, false) >= 0);
    // below
    gc = getGridContents(xc, yc + 1);  
    boolean bu = (checkForTravelCountCrossings(gc, src, trg, linkID, okGroups, walkUp, null, recoveryExemptions, isStart, false) >= 0);
    // lower right
    gc = getGridContents(xc + 1, yc + 1);  
  //  boolean lru = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkUp, null, recoveryExemptions, isStart, false) >= 0);
  //  boolean lrl = (checkForTravelCountCrossings(gc, src, trg, linkID, tup, walkLeft, null, recoveryExemptions, isStart, false) >= 0);    
    
    // Need _two_ paths for the point to be reachable: in and out!
    
    
    // FIX ME!!!! We miscount an arrival zone as not allowing an exit: WRONG!
    int count = (ad) ? 1 : 0;
    count = (lr) ? count + 1 : count;
    count = (rl) ? count + 1 : count;
    count = (bu) ? count + 1 : count;
    return (count >= 2); 
  }    
   
  /***************************************************************************
  **
  ** Install a DOF option
  */

  private DOFState installDOFOption(RecoveryDOFAnalyzer.DOFOptionPair optionPair, RecoveryDataForLink recoverForLink, 
                                    int currDepth, Point2D targ, TravelTurn turn) {

    DOFState retval = new DOFState();    
    
    retval.threePts = buildRecoverPointList(turn.start, targ, recoverForLink, currDepth);
    retval.haveOrthoPoints = (retval.threePts == null) ? false : recoverPointListStartsOrtho(retval.threePts);
    retval.haveOrthoCorner = (retval.haveOrthoPoints && recoverPointListIsOrtho(retval.threePts));
    retval.turn = turn;
   
    if ((optionPair != null) && (optionPair.firstPoint != null)) {
      recoverForLink.pushRevisedPoint(optionPair.firstPoint.lsid, optionPair.firstPoint.newPt);      
      retval.threePts = buildRecoverPointList(turn.start, targ, recoverForLink, currDepth); 
      Point2D startPt = retval.threePts.get(0);
      Point2D endPt = retval.threePts.get(1);
      Vector2D departDir = (new Vector2D(startPt, endPt)).normalized();
      retval.turn = buildInitRecoveryTurn(departDir, retval.threePts);
      retval.haveOrthoPoints = (retval.threePts == null) ? false : recoverPointListStartsOrtho(retval.threePts);
      retval.haveOrthoCorner = (retval.haveOrthoPoints && recoverPointListIsOrtho(retval.threePts));      
    }
    
    if ((optionPair != null) && (optionPair.cornerPoint != null)) {
      recoverForLink.pushRevisedPoint(optionPair.cornerPoint.lsid, optionPair.cornerPoint.newPt);      
      retval.threePts = buildRecoverPointList(turn.start, targ, recoverForLink, currDepth);
      retval.haveOrthoCorner = (retval.haveOrthoPoints && recoverPointListIsOrtho(retval.threePts));      
    }
    
    return (retval);  
  }   
  
  /***************************************************************************
  **
  ** Install successful routing plans
  */

  private void installSuccessfulPaths(String src, RecoveryJumpResult jumpResult, 
                                      SimpleRecoveryResult simpleResult, 
                                      DiagonalResult diagResult, 
                                      RecoveryDataForLink recoverForLink, List<Point2D> points) {    
    if (jumpResult != null) {
      int numDetour = jumpResult.jumpDetour.size();
      for (int i = numDetour - 1; i >= 0; i--) {
        Point2D nextPoint = jumpResult.jumpDetour.get(i);
        if (points.isEmpty() || !nextPoint.equals(points.get(0))) {
          points.add(0, nextPoint);
        }
      }
      RecoveryDataForSource rds = recoverForLink.getEnclosingSource();
      if (rds.isOriginalPoint(jumpResult.detourStart)) {
        rds.addJumpFromPoint(jumpResult.detourStart, jumpResult.jumpDetour.get(0));      
      }
      if (rds.isOriginalPoint(jumpResult.detourEnd) && (numDetour > 1)) {
        rds.addJumpIntoPoint(jumpResult.detourEnd, jumpResult.jumpDetour.get(numDetour - 2));      
      }      
      
    }
    if (diagResult != null) {
      Point2D nextPoint = new Point2D.Double(diagResult.endPt.getX() * 10.0, diagResult.endPt.getY() * 10.0);
      if (points.isEmpty() || !nextPoint.equals(points.get(0))) {
        points.add(0, nextPoint);
      }
    }
    
    if ((simpleResult != null) && (simpleResult.noTurnPlan != null)) {
      Point2D nextPoint = new Point2D.Double(simpleResult.noTurnPlan.corner.getX() * 10.0, simpleResult.noTurnPlan.corner.getY() * 10.0);
      if (points.isEmpty() || !nextPoint.equals(points.get(0))) {
        points.add(0, nextPoint);
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Back out temporary plans
  */

  private void backOutTempPlans(RecoveryJumpResult jumpResult, SimpleRecoveryResult simpleResult, DiagonalResult diagResult) {
    if (jumpResult != null) {
      for (int i = jumpResult.tempedPlans.size() - 1; i >= 0; i--) {
        removeTempForPlan((TravelPlan)jumpResult.tempedPlans.get(i));
      }
    }
    if (diagResult != null) {
      removeTemp(diagResult.startPt, true);
    }           

    if ((simpleResult != null) && (simpleResult.noTurnPlan != null)) {
      removeTempForPlan(simpleResult.noTurnPlan);
    }
    return;
  }

  /***************************************************************************
  **
  ** Take a path step.
  */

  private SimpleRecoveryResult trySimpleRecovery(String src, String trg, String linkID, Point2D targ, 
                                                 Vector2D startDir, Vector2D finalDir, 
                                                 TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                                                 GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                                                 Set<String> okGroups, int entryPref, RecoveryDataForLink recoverForLink, 
                                                 List<Point2D> threePts, boolean strictOKGroups) 
    throws NonConvergenceException {
   
    SimpleRecoveryResult retval = new SimpleRecoveryResult();
    retval.noTurnPlan = null;
    retval.recovResult = RECOV_STEP_CORE_FAILED_;  // need to do the whole turn... 
      
    // Simple increment of depth if we do not have to jump right away:
    Set<String> modOkGroups = (strictOKGroups) ? okGroups : null;
    TaggedPlan rscResult = recoverStepCore(src, trg, linkID, targ, startDir, finalDir, 
                                           turn, points, maxDepth, currDepth + 1,
                                           params, checker, isStart, force, modOkGroups,
                                           entryPref, recoverForLink, threePts, strictOKGroups);
    retval.recovResult = rscResult.returnCode;
    retval.noTurnPlan = rscResult.partialPlan;
    if (retval.recovResult == RECOV_STEP_CORE_OK_) {
      retval.exitCode = RECOV_STEP_CORE_OK_;
      retval.exitWithCode = true;
      return (retval);
      // This case handles when we hit the target, though it skips the NO_TURN case when
      // the last segment needs jump handling...
    } else if (retval.recovResult == RECOV_STEP_CORE_NO_TURN_) {
      int numPts = threePts.size();
      Point2D secondPt = threePts.get(1);
      Point2D lastPt = threePts.get(numPts - 1);
      if ((numPts == 3) && secondPt.equals(lastPt) && lastPt.equals(targ)) {
        addTempForPlan(retval.noTurnPlan, src, isStart, startDir);
        addTempEntryForFinalCorner(turn.start, src, getDirection(finalDir));
        points.add(0, new Point2D.Double(retval.noTurnPlan.corner.getX() * 10.0, retval.noTurnPlan.corner.getY() * 10.0));
        retval.exitWithCode = true;
        retval.exitCode = RECOV_STEP_CORE_OK_;
        return (retval);
      }
      if (!needOrthoRecovery(recoverForLink, currDepth + 1)) {
        retval.recovResult = RECOV_STEP_CORE_NO_TURN_NO_JUMP_;
        addTempForPlan(retval.noTurnPlan, src, isStart, startDir);
        return (retval);       
      } 
    }
    
    retval.exitWithCode = false;
    retval.exitCode = RECOV_STEP_NO_CODE_;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get everything set up for a jump detour
  */

  private JumpPreparations prepareForJump(SimpleRecoveryResult simpleResult, TravelTurn turn, Point2D targ,  
                                          RecoveryDataForLink recoverForLink, 
                                          List<Point2D> threePts, int currDepth, String src, List<Point2D> prevJumpDetour, 
                                          boolean isStart, Vector2D startDir, Set<String> okGroups, String linkID, String trg) { 
    JumpPreparations retval = new JumpPreparations();
    retval.currDepth = currDepth;
    retval.jumpTarg = new Point2D.Double();
    retval.exitWithCode = false;
    retval.exitCode = RECOV_STEP_NO_CODE_;    
    
    if ((simpleResult == null) || (simpleResult.recovResult == RECOV_STEP_CORE_FAILED_)) {
      // Jumping needs we need to look backwards for points if everything failed:
      // Prev point may come from jump path, and NOT from existing points list!!!!
      if (retval.currDepth > 0) {
        List<Point2D> turnPts = buildRecoverPointList(turn.start, targ, recoverForLink, retval.currDepth - 1);
        retval.tryTurn = buildTryTurnToo(turnPts, false, retval.jumpTarg, prevJumpDetour);
        if (retval.tryTurn == null) {
          retval.exitWithCode = true;
          retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;                
        }
      } else {
        // Bug fix BT-03-17-10:1
        // Saw a case where the startDir was the zero vector, causing illegal argument exception
        // in getTurn().  Could not reproduce after other bugs were addressed; but this is belt
        // and suspenders:
        //  
        if (startDir.isZero()) {
          retval.exitWithCode = true;
          retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;                
        } else {
          retval.jumpTarg.setLocation(threePts.get(1));    
          Vector2D canStart = (startDir.isCanonical()) ? startDir : startDir.canonical().normalized();
          retval.tryTurn = buildInitTurn(src, turn.start, canStart, trg, threePts.get(1), linkID, okGroups);
        }
      }
    } else if (simpleResult.recovResult == RECOV_STEP_CORE_NO_TURN_) {       
      addTempForPlan(simpleResult.noTurnPlan, src, isStart, startDir);
      // Build off current depth to choose points, then inc because recovery sorta worked:
      List<Point2D> turnPts = buildRecoverPointList(turn.start, targ, recoverForLink, retval.currDepth); 
      retval.tryTurn = buildTryTurnToo(turnPts, false, retval.jumpTarg, null);
      if (retval.tryTurn == null) {
        retval.exitWithCode = true;
        retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;                
      } else {
        retval.currDepth++;
      }
      // if the failure occurred several jumps down the line, it is pointless to try doing it here:
    } else if (simpleResult.recovResult == RECOV_STEP_EMBEDDED_JUMP_FAILED_) {
      retval.exitWithCode = true;
      retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;
      return (retval);
    } else {
      throw new IllegalStateException();
    }
 
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Take a recovery jump step
  */

  private RecoveryJumpResult handleRecoveryJump(String src, String trg, String linkID, 
                                                Point2D finalTarg, Point2D jumpTarg, 
                                                Vector2D startDir, Vector2D finalDir, 
                                                TravelTurn turn, TravelTurn tryTurn,
                                                int maxDepth, int currDepth,
                                                GoodnessParams params, IterationChecker checker,
                                                int prevRecoveryState, List<Point2D> threePts, 
                                                boolean isStart, boolean force,
                                                Set<String> okGroups, int entryPref, 
                                                RecoveryDataForLink recoverForLink, 
                                                List<Point2D> prevJumpDetour, boolean strictOkGroups)
                                                  throws NonConvergenceException {
    
    //
    // Usual recursive approach add/removes temp plans from the grid via the call stack.
    // But here we may have jump-added temp plans that may need to be removed after a
    // successful route but an unsuccessful recover later on:
    //
   
    RecoveryJumpResult retval = new RecoveryJumpResult();
    retval.tempedPlans = new ArrayList<TravelPlan>();
    retval.jumpDetour = new ArrayList<Point2D>();
    retval.threePts = threePts;
    retval.detourStart = (Point2D)tryTurn.start.clone();
    retval.detourEnd = (Point2D)jumpTarg.clone();    
    
    //
    // Need to build temporary downstream links to keep from crossing them:
    //
    
    int currSkip = currDepth;
    List<Point2D> skipPts = buildRecoverPointList(turn.start, finalTarg, recoverForLink, currSkip++);
    ArrayList<TravelPlan> skipPlans = new ArrayList<TravelPlan>();
    while ((skipPts != null) && (skipPts.size() >= 2)) {
      TravelPlan skipPlan = null;
      if ((skipPts != null) && (skipPts.size() >= 2) && recoverPointListStartsOrtho(skipPts)) {
        Vector2D turnVector = null;
        Point2D skip0 = skipPts.get(0);
        Point2D skip1 = skipPts.get(1);
        if (recoverPointListIsOrtho(skipPts) && (skipPts.size() == 3)) {
          Point2D skip2 = skipPts.get(2);
          if (!skip1.equals(skip2)) {
            turnVector = (new Vector2D(skip1, skip2)).normalized();
          }
        }
        skipPlan = new TravelPlan(skip0, skip1, null, new Vector2D(skip0, skip1).normalized(), turnVector);
        addTempForPlan(skipPlan, src, false, null);
        skipPlans.add(skipPlan);
      }
      skipPts = buildRecoverPointList(turn.start, finalTarg, recoverForLink, currSkip++);
    }
    
    //
    // okGroups disposition 4/19/12
    //
    // We need to be ABSOLUTELY SURE that link repairs stay out of a region that is being
    // laid out, as we are doing this step before the link internals are actually created (since
    // we need to get the group shifting finished so we can use those results before we go off
    // to finish up link internals.
    // As of this date, a previous fix had been to **NULL out** the okGroups argument into jumpStepCore.
    // IIRC, this was done to allow lots of flexibility when trying to get something, anything to work
    // while recovering a difficult path.  Actually, this is too lax, since I am seeing paths passing
    // through unrelated regions when there are still a boatload of non-intrusive alternatives available.
    // So that actually deserves a FIXME.  In the meantime, we CANNOT allow links to try to route 
    // through a re-layout region.  So if we are doing a relayout, we retain the previous strict okGroups
    // case.
    
    
    //
    // Do the jump.  We handle force cases by trying to do a non-force for this segment
    // before trying to do a force:
    //
    
    boolean jscOK;
    try {
      Vector2D canStart = (startDir.isCanonical()) ? startDir : startDir.canonical().normalized();
      Set<String> modOkGroups = (strictOkGroups) ? okGroups : null;
      jscOK = jumpStepCore(src, trg, linkID, jumpTarg, canStart, finalDir, 
                           tryTurn, retval.jumpDetour, maxDepth, currDepth,
                           params, checker, isStart, false, modOkGroups, entryPref, retval.tempedPlans);
      if (!jscOK && force) {       
        if ((prevRecoveryState == RECOV_STEP_CORE_FAILED_) || (prevRecoveryState == RECOV_STEP_NO_CODE_)) {
          if (retval.currDepth > 0) {
            List<Point2D> turnPts = buildRecoverPointList(turn.start, finalTarg, recoverForLink, currDepth - 1);
            tryTurn = buildTryTurnToo(turnPts, force, jumpTarg, prevJumpDetour);
            if (tryTurn == null) {
              retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;
              retval.exitWithCode = true;
              retval.currDepth = currDepth;
              return (retval);
            }
          } else {
            jumpTarg.setLocation(threePts.get(1));
            tryTurn = buildInitTurn(src, turn.start, canStart, trg, threePts.get(1), linkID, okGroups);     
          }
        } else {
          // Note that currDepth was incremented in preparation stage, so we need to look back:
          List<Point2D> turnPts = buildRecoverPointList(turn.start, finalTarg, recoverForLink, currDepth - 1); 
          tryTurn = buildTryTurnToo(turnPts, force, jumpTarg, null);
          if (tryTurn == null) {
            retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;
            retval.exitWithCode = true;
            retval.currDepth = currDepth;
            return (retval);
          }
        }
        jscOK = jumpStepCore(src, trg, linkID, jumpTarg, canStart, finalDir, 
                             tryTurn, retval.jumpDetour, maxDepth, currDepth,
                             params, checker, isStart, false, modOkGroups, entryPref, retval.tempedPlans);
      }    
    } catch (NonConvergenceException ncex) {
      int numSkips = skipPlans.size();
      for (int i = numSkips - 1; i >= 0; i--) {
        removeTempForPlan(skipPlans.get(i));
      }
      throw ncex;
    }  
    
    int numSkips = skipPlans.size();
    for (int i = numSkips - 1; i >= 0; i--) {
      removeTempForPlan(skipPlans.get(i));
    }
    
    if (!jscOK) {
      retval.exitCode = RECOV_STEP_EMBEDDED_JUMP_FAILED_;
      retval.exitWithCode = true;
      retval.currDepth = currDepth;
      return (retval);
    }
    
    //
    // Success in jump sends us one more down the path:
    //
    
    currDepth++;

    //
    // Handle termination case:
    //
    retval.threePts = buildRecoverPointList(turn.start, finalTarg, recoverForLink, currDepth);
    if (retval.threePts == null) {
      addTempEntryForFinalCorner(turn.start, src, getDirection(finalDir));
      retval.exitCode = RECOV_STEP_CORE_OK_;
      retval.exitWithCode = true;
      retval.currDepth = currDepth;   
      return (retval);  
    }
    
    retval.exitCode = RECOV_STEP_NO_CODE_;
    retval.exitWithCode = false;
    retval.currDepth = currDepth;   
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Take a recovery diagonal step
  */  
  
  
 private DiagonalResult handleDiagonalStepJump(String src, Point2D targ,
                                               TravelTurn turn, int currDepth,
                                               List<Point2D> points, List<Point2D> threePts, 
                                               RecoveryDataForLink recoverForLink) throws NonConvergenceException {
    //
    // Just go diagonal to the next recovery point:
    //
   
    DiagonalResult retval = new DiagonalResult();
    
    if (threePts == null) { // FIX ME!!! What do we do here?  Will this happen??
      retval.exitCode = RECOV_STEP_CORE_OK_;
      retval.exitWithCode = false;
      return (retval);  
    }
    retval.threePts = threePts;
    retval.startPt = threePts.get(0);
    retval.endPt = threePts.get(1);
    addTempEntryForDiagonalCorner(retval.startPt, src);
 
    currDepth++;
    
    retval.threePts = buildRecoverPointList(turn.start, targ, recoverForLink, currDepth);  
    if (retval.threePts == null) {
      addTempEntryForFinalCorner(retval.endPt, src, NONE);
      Point2D nextPoint = new Point2D.Double(retval.endPt.getX() * 10.0, retval.endPt.getY() * 10.0);
      if (points.isEmpty() || !nextPoint.equals(points.get(0))) {
        points.add(0, nextPoint);
      }
      retval.exitCode = RECOV_STEP_CORE_OK_;
      retval.exitWithCode = true;
      retval.currDepth = currDepth;   
      return (retval);  
    }
    
    retval.exitCode = RECOV_STEP_NO_CODE_;
    retval.exitWithCode = false;
    retval.currDepth = currDepth;
    return (retval);
  } 
       
  /***************************************************************************
  **
  ** Take a path step.
  */
  
  

  private TaggedPlan recoverStepCore(String src, String trg, String linkID, Point2D targ, Vector2D startDir, Vector2D finalDir, 
                                     TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                                     GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                                     Set<String> okGroups, int entryPref, RecoveryDataForLink recoverForLink, List<Point2D> threePts, boolean strictOKGroups) 
                                       throws NonConvergenceException {
    
    Set<String> exemptions = recoverForLink.getEnclosingSource().getExemptions();
    TravelOptions options = analyzeTravelsForRecoverRun(turn, src, trg, linkID, okGroups, threePts, exemptions, isStart);
    
    while (true) {
      TravelPlan newPlan = chooseRecoveryPlan(options);
      if (newPlan == null) {
        return (new TaggedPlan(RECOV_STEP_CORE_FAILED_, null));
      }
      //
      // If we get back a recovery plan, then we made it to our target for that
      // segment, so that point needs to be added to the point list.  But if the
      // next point around the corner is not ortho, we need to bag recovery for
      // the time being and start jumping.
      // If the new target is not ortho, we abort.  Note that the run direction
      // from the travel options IS orthogonal, but might not be headed directly
      // at the next point around the corner!
      //
      if (threePts.size() == 3) {
        Point2D startPt = threePts.get(1);
        Point2D endPt = threePts.get(2);
        if (!isOrthogonal(startPt, endPt)) {
          return (new TaggedPlan(RECOV_STEP_CORE_NO_TURN_, newPlan));
        }
      }

      // We do not draw the start corner except on first link from source:
      addTempForPlan(newPlan, src, isStart, startDir);
      threePts = buildRecoverPointList(turn.start, targ, recoverForLink, currDepth);  // Note currDepth was incremented...  
      TravelTurn newTurn = planToNewRecoverTurn(newPlan, threePts);
      int retval;
      try {
        Set<String> modOkGroups = (strictOKGroups) ? okGroups : null;
        retval = recoverStep(src, trg, linkID, targ, startDir, finalDir, newTurn, points, maxDepth, 
                             currDepth, params, checker, false, force, modOkGroups, entryPref, 
                             recoverForLink, exemptions, null, strictOKGroups);
        if (retval == RECOV_STEP_CORE_OK_) {
          points.add(0, new Point2D.Double(newPlan.corner.getX() * 10.0, newPlan.corner.getY() * 10.0));
          return (new TaggedPlan(RECOV_STEP_CORE_OK_, null));
        } else if (retval == RECOV_STEP_EMBEDDED_JUMP_FAILED_) {
          removeTempForPlan(newPlan);
          return (new TaggedPlan(RECOV_STEP_EMBEDDED_JUMP_FAILED_, null));
        }
      } catch (NonConvergenceException ncex) {
        removeTempForPlan(newPlan);
        throw ncex;
      }
      removeTempForPlan(newPlan);
    }   
  }    
  
  
  /***************************************************************************
  **
  ** Take a path step.
  */

  private boolean jumpStep(String src, String trg, String linkID, Point2D targ, Vector2D startDir, Vector2D finalDir, 
                           TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                           GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                           Set<String> okGroups, int entryPref, List<TravelPlan> tempedPlans) 
    throws NonConvergenceException {

    currDepth++;
    if (currDepth == maxDepth) {
      throw new NonConvergenceException();
    }
    checker.bump();
    
    //
    // Are we there yet?  If yes, we are done:
    //
 
    if (turn.start.equals(targ)) {
      if (currDepth == 1) {  // I.e. the first
        addTempEntryForCorner(turn.start, src, getDirection(startDir), getDirection(finalDir));
      } else {
        addTempEntryForFinalCorner(turn.start, src, getDirection(finalDir));
      }
      return (true);
    }

    //
    // Figure out what to do, and try options until we are exhausted:
    //
    
    
    return (jumpStepCore(src, trg, linkID, targ, startDir, finalDir, 
                         turn, points, maxDepth, currDepth,
                         params, checker, isStart, force, okGroups, entryPref, tempedPlans)); 
   
  }
  
  /***************************************************************************
  **
  ** Take a path step.
  */

  private boolean jumpStepCore(String src, String trg, String linkID, Point2D targ, Vector2D startDir, Vector2D finalDir, 
                               TravelTurn turn, List<Point2D> points, int maxDepth, int currDepth,
                               GoodnessParams params, IterationChecker checker, boolean isStart, boolean force,
                               Set<String> okGroups, int entryPref, List<TravelPlan> tempedPlans) 
                               throws NonConvergenceException {

    
    TravelOptions options = analyzeTravelsForRun(turn, src, trg, linkID, okGroups, targ, isStart);
    while (true) {
      TravelPlan newPlan = chooseAPlan(options, params, targ, isStart || force, entryPref);
      if (newPlan == null) {
        return (false);
      }
      addTempForPlan(newPlan, src, isStart, startDir);
      if (tempedPlans != null) tempedPlans.add(newPlan);
      TravelTurn newTurn = planToNewTurn(newPlan, targ);
      try {
        if (jumpStep(src, trg, linkID, targ, startDir, finalDir, newTurn, points, maxDepth, 
                     currDepth, params, checker, false, force, okGroups, entryPref, tempedPlans)) {
          points.add(0, new Point2D.Double(newPlan.corner.getX() * 10.0, newPlan.corner.getY() * 10.0));
          return (true);
        }
      } catch (NonConvergenceException ncex) {
        removeTempForPlan(newPlan);
        if (tempedPlans != null) tempedPlans.remove(tempedPlans.size() - 1);
        throw ncex;
      }
      removeTempForPlan(newPlan);
      if (tempedPlans != null) tempedPlans.remove(tempedPlans.size() - 1);
    }   
  }  
  
  /***************************************************************************
  **
  ** Add temporary grid entries for the plan
  */

  private void addTempForPlan(TravelPlan plan, String src, boolean doStart, Vector2D turnEntry) {
    if (plan.corner.equals(new Point2D.Double(-13.0, 147.0)) && src.equals("wnt9zzzr:0")) {
      plan.turnDirection = new Vector2D(0.0, 1.0);
    }
    WalkValues walk = getWalkValues(plan.runDirection, plan.start, plan.corner, 1);
    int dirVal = getDirection(plan.runDirection);
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        if ((x == walk.startX) && (y == walk.startY)) {   
          if (doStart) {
            int sDirVal = getDirection(turnEntry);
            addTempEntryForCorner(new Point2D.Double(x, y), src, sDirVal, getDirection(plan.runDirection));
          }
        } else if ((x == walk.endX - walk.incX) && (y == walk.endY - walk.incY)) {
          int turnDir = (plan.turnDirection != null) ? getDirection(plan.turnDirection) : DIAGONAL;
          addTempEntryForCorner(new Point2D.Double(x, y), src, dirVal, turnDir);
        } else {
          addTempEntryForRun(new Point2D.Double(x, y), src, dirVal);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drop temp entries for a plan
  */

  private void removeTempForPlan(TravelPlan plan) {
    WalkValues walk = getWalkValues(plan.runDirection, plan.start, plan.corner, 1);   
    for (int x = walk.startX; x != walk.endX; x += walk.incX) {
      for (int y = walk.startY; y != walk.endY; y += walk.incY) {
        if ((x == walk.startX) && (y == walk.startY)) {
          continue;
        }
        Point2D pointForPlan = new Point2D.Double(x, y);
        removeTemp(pointForPlan, true);
        if (checkForTempFlag(pointForPlan)) {
          removeTemp(pointForPlan, true); // FIX ME!!!! Multi temp setting for pt  
        }
      }
    }
    return;
  }  
  
  
  /***************************************************************************
  **
  ** From a list of travel options, select out a plan (options are modified to
  ** eliminate chosen plan)
  */

  private TravelPlan chooseAPlan(TravelOptions options, GoodnessParams params, Point2D targ, boolean force, int entryPref) { 
    
    if (options.turn.isDirect) {
      return (chooseDirectPlan(options, targ));
    } else {
      return (chooseOffsetPlan(options, params, targ, force, entryPref));
    }
  }
  
  /***************************************************************************
  **
  ** From a list of travel options, select out a plan (options are modified to
  ** eliminate chosen plan)
  */

  private TravelPlan chooseOffsetPlan(TravelOptions options, GoodnessParams params, Point2D targ, boolean force, int entryPref) { 
    
    //
    // FIX ME!! If we are heading down to a gene pad, do not approach directly
    // towards another pad and veer off at the last opportunity!  This will block
    // the next link from the pad!
    //
    
    if ((options.groupBlockers != null) && !options.groupBlockers.isEmpty()) {
      double blockFraction = Double.NEGATIVE_INFINITY;
      int osize = options.travels.size();
      for (int i = 0; i < osize; i++) {
        TravelResultPair pair = options.travels.get(i);
        TravelResult prim = pair.primary;
        if (prim == null) {
          continue;
        }
        
        if ((prim.groupBlocker != null) && 
            (!prim.groupBlocker.isEmpty())) {
          if (blockFraction == Double.NEGATIVE_INFINITY) {
            blockFraction = prim.fraction;
            continue;
          } else {
            pair.primary = null;
          }
        }
      }
      // Don't do this again...
      options.groupBlockers = null;
    }

    
    //
    // This is where we determine the "best place" to turn.
    //
    int preferredPos;
    if (options.turn.runTerminus == null) {
      preferredPos = 1;
    } else {
      Vector2D run = new Vector2D(options.turn.start, options.turn.runTerminus);
      double distance = run.length();
      preferredPos = (int)distance;
    }
    if (options.travels.size() <= preferredPos) {
      preferredPos = options.travels.size() - 1;
    }
    int bestResult = findBestDirectionViaGoodness(params, options, preferredPos, targ, entryPref);

    //
    // Generally, detours are undesirable, if we can back off and find a route
    // that gets us closer.  But sometimes the detour is essential:
    boolean goToBackup = false;
    if (bestResult == -1) {
      if (force) {
        bestResult = findALastDitchDetour(options);
        if (bestResult == -1) {
          return (null);
        }
        goToBackup = true;
      } else {
        return (null);
      }
    }
    TravelResultPair pair = options.travels.get(bestResult);
    TravelResult result;
    Vector2D turnDir;
    if (goToBackup) {
      result = pair.secondary;
      pair.secondary = null;
      turnDir = options.turn.secondaryTurnDirection;
    } else {
      result = pair.primary;
      pair.primary = null;
      turnDir = options.turn.primaryTurnDirection;
    }
    
    Vector2D toCorner = new Vector2D(options.turn.runDirection);
    toCorner.scale((double)bestResult);
    Point2D corner = toCorner.add(options.turn.start);
    return (new TravelPlan(options.turn.start, corner, result.farthestPt, 
                           options.turn.runDirection, turnDir));
  }
  
  
  /***************************************************************************
  **
  ** From a list of travel options, select out a plan (options are modified to
  ** eliminate chosen plan)
  */

  private TravelPlan chooseRecoveryPlan(TravelOptions options) { 

    TravelResult prim = null;
    int osize = options.travels.size();
    int matchIndex = -1;
    for (int i = 0; i < osize; i++) {
      TravelResultPair pair = options.travels.get(i);
      TravelResult chk = pair.primary;
      if (chk != null) { 
        prim = chk;
        pair.primary = null;
        matchIndex = i;
        break;
      }
    }
    if (prim == null) {
      return (null);
    }

    Vector2D turnDir = (options.turn.isDirect) ? options.turn.runDirection : options.turn.primaryTurnDirection;
    Vector2D toCorner = new Vector2D(options.turn.runDirection);
    toCorner.scale((double)matchIndex);
    Point2D corner = toCorner.add(options.turn.start);
    return (new TravelPlan(options.turn.start, corner, prim.farthestPt, 
                           options.turn.runDirection, turnDir));
  }  
 
  /***************************************************************************
  **
  ** From a list of travel options, select out a plan (options are modified to
  ** eliminate chosen plan)
  */

  private TravelPlan chooseDirectPlan(TravelOptions options, Point2D targ) { 

    int preferredPos;
    if (options.turn.runTerminus == null) {
      preferredPos = 1;
    } else {
      Vector2D run = new Vector2D(options.turn.start, options.turn.runTerminus);
      double distance = run.length();
      preferredPos = (int)distance;
    }
    
    if (options.travels.size() <= preferredPos) {
      preferredPos = options.travels.size() - 1;
    }   
   
    BestDetour bestResult = findBestDetour(options, preferredPos);
    if ((bestResult == null) || (bestResult.index == -1)) {
      return (null);
    }
    TravelResultPair pair = options.travels.get(bestResult.index);
    TravelResult result;
    Vector2D turnDir;
    if (bestResult.usePrimary) {
      result = pair.primary;
      turnDir = options.turn.primaryTurnDirection;
      pair.primary = null;
    } else {
      result = pair.secondary;
      pair.secondary = null;
      turnDir = options.turn.secondaryTurnDirection;
    }
    Vector2D toCorner = new Vector2D(options.turn.runDirection);
    toCorner.scale((double)bestResult.index);
    Point2D corner = toCorner.add(options.turn.start);
    return (new TravelPlan(options.turn.start, corner, result.farthestPt, 
                           options.turn.runDirection, turnDir));
  }  

  /***************************************************************************
  **
  ** Incrementing using the given step from the given preferred point, find
  ** the first travel plan with the maximum travel and least amount of crossings;
  ** return the index
  */

  @SuppressWarnings("unused")
  private int findNearestMax(TravelOptions options, int preferredIndex, int step) {   
   
    if ((step != 1) && (step != -1)) {
      throw new IllegalArgumentException();
    }
    int end = (step == 1) ? options.travels.size() : 0;  // look at last, but not start corner
    double currentMaxTravel = Double.NEGATIVE_INFINITY;
    int currentMaxIndex = -1;
    int currentCrossing = Integer.MAX_VALUE;
    
    for (int i = preferredIndex; i != end; i += step) {
      TravelResultPair pair = options.travels.get(i);
      if (pair.primary == null) {
        continue;
      }
      if ((pair.primary.fraction  >  currentMaxTravel) ||
          ((pair.primary.fraction == currentMaxTravel) && (pair.primary.crossings < currentCrossing))) {
        currentMaxTravel = pair.primary.fraction;
        currentCrossing = pair.primary.crossings;
        currentMaxIndex = i;
      }
    }
    return (currentMaxIndex);
  }
  
  /***************************************************************************
  **
  ** Incrementing using the given step from the given preferred point, find
  ** the first travel plan with the maximum travel and least amount of crossings;
  ** return the index
  */

  private int findBestDirectionViaGoodness(GoodnessParams params, TravelOptions options, 
                                           int preferredPos, Point2D targ, int entryPref) {
    
    
    TravelResultPair prefPair = options.travels.get(preferredPos);
    boolean toTarg = false;
    if ((prefPair != null) && (prefPair.primary != null) && prefPair.primary.stoppedByTarget) {
      toTarg = true;
    }       
    
    int bestBelow = findBestGoodness(params, options, preferredPos, -1, targ);
    double bestBelowGoodness;
    if (bestBelow == -1) {
      bestBelowGoodness = 0.0;
    } else {
      TravelResultPair pair = options.travels.get(bestBelow);
      bestBelowGoodness = goodness(params, pair.primary.fraction, preferredPos - bestBelow, pair.primary.crossings);   
    }
    
    int bestAbove = findBestGoodness(params, options, preferredPos, 1, targ);
    double bestAboveGoodness;
    if (bestAbove == -1) {
      bestAboveGoodness = 0.0;
    } else {
      TravelResultPair pair = options.travels.get(bestAbove);
      bestAboveGoodness = goodness(params, pair.primary.fraction, preferredPos - bestAbove, pair.primary.crossings);
    }
    
    if ((bestBelowGoodness == 0.0) && (bestAboveGoodness == 0.0)) {
      return (-1);
    }
    
    //
    // Pay heed to target preferences:
    //
    
    if (toTarg && (entryPref != NONE)) {
      boolean useBelow;
      int runDir = getDirection(options.turn.runDirection);
      switch (runDir) {
        case EAST:
          useBelow = ((entryPref & WEST) != 0x00);
          break;
        case WEST:
           useBelow = ((entryPref & EAST) != 0x00);
          break;
        case NORTH:
           useBelow = ((entryPref & SOUTH) != 0x00);
          break;
        case SOUTH:
           useBelow = ((entryPref & NORTH) != 0x00);
          break;    
        default:
          throw new IllegalArgumentException();
      }
      if (useBelow) {
        return ((bestBelowGoodness != 0.0) ? bestBelow : bestAbove);
      } else {
        return ((bestAboveGoodness != 0.0) ? bestAbove : bestBelow);
      }
    }
      
    if (bestAboveGoodness > bestBelowGoodness) {
      return (bestAbove);
    } else {
      return (bestBelow);
    }
  }
  
  /***************************************************************************
  **
  ** Incrementing using the given step from the given preferred point, find
  ** the first travel plan with the maximum travel and least amount of crossings;
  ** return the index
  */

  private int findBestGoodness(GoodnessParams params, TravelOptions options, 
                               int preferredIndex, int step, Point2D targ) {   
   
    if ((step != 1) && (step != -1)) {
      throw new IllegalArgumentException();
    }
    int end = (step == 1) ? options.travels.size() : 0;  // look at last, but not start corner    
    double bestGoodness = 0.0;
    int currentMaxIndex = -1;
             
    for (int i = preferredIndex; i != end; i += step) {
      TravelResultPair pair = options.travels.get(i);
      if (pair.primary == null) {
        continue;
      }
      //
      // If we make no progress towards the goal, skip it!
      //
      if (pair.primary.fraction == 0.0) {
        continue;
      }
      //
      // Ditch the crossings check in the terminal region, where basic one-hop lookahead
      // crossing assumptions are poor.
      //
      Vector2D trav = new Vector2D(options.turn.runDirection);
      trav.scale((double)i);
      Point2D pos = trav.add(options.turn.start);
      Vector2D remains = new Vector2D(pos, targ);
      boolean isTerminal = (remains.length() < params.terminal);            
 
      int useCrossings = (isTerminal) ? 0 : pair.primary.crossings;
      double currGoodness = goodness(params, pair.primary.fraction, preferredIndex - i, useCrossings);
      if (currGoodness > bestGoodness) {
        bestGoodness = currGoodness;
        currentMaxIndex = i;
      }
    }
    return (currentMaxIndex);
  }
  
  /***************************************************************************
  **
  ** If we must go this route, find a detour!  Plus kill off all the
  ** useless primary options.
  */

  private int findALastDitchDetour(TravelOptions options) {
    int retval = -1;
    int end = options.travels.size();    
    for (int i = 0; i < end; i++) {
      TravelResultPair pair = options.travels.get(i);
      if ((pair.secondary != null) && (retval == -1)) {
        retval = i;
      }
      pair.primary = null;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Goodness function.
  */

  private double goodness(GoodnessParams params, double targetPercent, int travelDiff, int crossings) {
    //
    // Goodness of a travel choice is in 3-D space:
    //  1) Percentage to target (0.0 - 1.0)
    //  2) Difference from preferred position N(0, s)
    //  3) Crossing number (1 / x + 1)
    //

    double term1 = targetPercent * params.normalize;
    double term2 = params.differenceCoeff * gaussian((double)travelDiff, params.differenceSigma);
    term2 *= params.differenceNormalize * params.normalize;
    double term3 = params.crossingCoeff * (1.0 / ((params.crossingMultiplier * (double)crossings) + 1.0));
    term3 *= params.normalize;
    return (term1 + term2 + term3);
  }

  /***************************************************************************
  **
  ** Gaussian
  */

  private double gaussian(double x, double sigma) {
    Double lookupArg = new Double(x);
    Double result = gauss_.get(lookupArg);
    if (result == null) {
      double coeff = 1.0 / (Math.sqrt(2.0 * Math.PI) * sigma);
      double exp = - ((x * x) / (2 * sigma * sigma));
      double retval = (coeff * Math.exp(exp));
      gauss_.put(lookupArg, new Double(retval));
      return (retval);
    }
    return (result.doubleValue());
  }  

  /***************************************************************************
  **
  ** Find the best detour option
  */
  
  private BestDetour findBestDetour(TravelOptions options, int preferredPos) {
    double currentMinDetour = Double.POSITIVE_INFINITY;
    int currentBestIndex = -1;
    boolean usePrimary = false;
    
    //
    // Go backwards and find the shortest detour 
    //
    for (int i = preferredPos; i != 0; i--) {
      TravelResultPair pair = options.travels.get(i);
      if ((pair.primary == null) && (pair.secondary == null)) {
        continue;
      }
      if (pair.primary != null) {
        if (pair.primary.detourDistance < currentMinDetour) {
          currentMinDetour = pair.primary.detourDistance;
          usePrimary = true;
          currentBestIndex = i;
        }
      }
      if (pair.secondary != null) {
        if (pair.secondary.detourDistance < currentMinDetour) {
          currentMinDetour = pair.secondary.detourDistance;
          usePrimary = false;
          currentBestIndex = i;
        }
      }
    }
    return (new BestDetour(currentBestIndex, usePrimary));
  }  

  /***************************************************************************
  **
  ** Advance chosen plan to turn for next level of recursion.  
  */

  private TravelTurn planToNewTurn(TravelPlan oldPlan, Point2D finalPt) {
  
    TravelTurn retval = new TravelTurn();
    retval.runDirection = oldPlan.turnDirection;
    retval.runTerminus = oldPlan.farthestPt;
    retval.start = (Point2D)oldPlan.corner.clone();
    fillOrthogonalVectors(retval, oldPlan.corner, finalPt);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Advance chosen plan to turn for next level of recursion.  
  */

  private TravelTurn planToNewRecoverTurn(TravelPlan oldPlan, List<Point2D> threePoints) {
    
    TravelTurn retval = new TravelTurn();
    retval.runDirection = oldPlan.turnDirection;
    retval.runTerminus = oldPlan.farthestPt;
    retval.start = (Point2D)oldPlan.corner.clone();
   
    if ((threePoints != null) /*&& recoverPointListIsOrtho(threePoints)*/ && (threePoints.size() == 3)) {
      Point2D turnTarget = threePoints.get(2);
      fillOrthogonalVectors(retval, oldPlan.corner, turnTarget);
    } else {
      retval.primaryTurnDirection = getTurn(retval.runDirection, LEFT);
      retval.secondaryTurnDirection = getTurn(retval.runDirection, RIGHT);  
      retval.isDirect = true;
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Convert the terminal region from reserved state to occupied.
  */

  private void convertTerminalRegion(TerminalRegion region) { 
    
    //
    // Go into the grid and drop the IS_RESERVED_DEPARTURE_RUN or 
    // IS_RESERVED_ARRIVAL_RUN status, and install actual run cells for
    // the cells that got used.
    //
    
    boolean isRun = false;
    int numPt = region.getNumTravelPoints();
    for (int i = 0; i < numPt; i++) {
      Point pt = region.getTravelPoint(i);
      GridContents gc = getGridContents(pt);
      if (!dropReservation(gc, pt, region.type)) {
        writeBackModifiedGridContents(pt, gc);
      } else {
        dropFromPattern(pt);
      }    
      if (i == 0) {
        GridEntry entry = convertTerminalPoint(pt, region, IS_PAD);
        if (entry != null) {
          installModifiedGridContents(pt, entry);
        }
        isRun = true;
      } else if (cornerType(gc) != NO_CORNER) {
        isRun = false;
      } else if (isRun) {
        GridEntry entry = convertTerminalPoint(pt, region, IS_RUN);
        if (entry != null) {
          installModifiedGridContents(pt, entry);
        }
      }
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Convert the terminal region to unreserved
  */

  private void dropTerminalRegion(TerminalRegion region) { 
    
    //
    // Go into the grid and drop the IS_RESERVED_DEPARTURE_RUN or 
    // IS_RESERVED_ARRIVAL_RUN status
    //
    
    int numPt = region.getNumTravelPoints();
    for (int i = 0; i < numPt; i++) {
      Point pt = region.getTravelPoint(i);
      GridContents gc = getGridContents(pt);
      if (!dropReservation(gc, pt, region.type)) {
        writeBackModifiedGridContents(pt, gc);
      } else {
        dropFromPattern(pt);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Drop terminal zone reservation.
  */
  
  private boolean dropReservation(GridContents gc, Point pt, int type) {

    if (gc == null) {
      //  Should FIX ME:  This should not be empty
      return (false);
    }
    
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if ((gc.entry.type == IS_RESERVED_ARRIVAL_RUN) && 
          (type == TerminalRegion.ARRIVAL)) {
        return (true);
      } else if ((gc.entry.type == IS_RESERVED_MULTI_LINK_ARRIVAL_RUN) && 
          (type == TerminalRegion.ARRIVAL)) {
        return (true);  
      } else if ((gc.entry.type == IS_RESERVED_DEPARTURE_RUN) && 
                 (type == TerminalRegion.DEPARTURE)) { 
        return (true);
      }
      return (false);
    }
    
    //
    // FIX FOR BT-05-23-05:1  Don't remove arrival reservation IF
    // we are removing a departure reservation!
    //

    int size = gc.extra.size();
    for (int i = size - 1; i >= 0; i--) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_RESERVED_ARRIVAL_RUN) && 
          (type == TerminalRegion.ARRIVAL)) {
        gc.extra.remove(i);
      } else if ((currEntry.type == IS_RESERVED_MULTI_LINK_ARRIVAL_RUN) && 
                 (type == TerminalRegion.ARRIVAL)) { 
        gc.extra.remove(i);     
      } else if ((currEntry.type == IS_RESERVED_DEPARTURE_RUN) && 
                 (type == TerminalRegion.DEPARTURE)) { 
        gc.extra.remove(i);
      }      
    }
    
    size = gc.extra.size();
    if (size == 0) {
      return (true);
    } else if (size == 1) {
      gc.entry = gc.extra.get(0);
      gc.extra = null;
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Determine if this is a corner slot for the source
  */
  
  private int cornerType(GridContents gc) {
    
    if (gc == null) {
      return (NO_CORNER);
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.type == IS_CORNER) {
        return (gc.entry.cornerType());
      } else {
        return (NO_CORNER);
      }
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_CORNER) {
        return (currEntry.cornerType());
      }
    }
    return (NO_CORNER);
  }
  
  /***************************************************************************
  **
  ** Convert the terminal point to occupied.
  */

  private GridEntry convertTerminalPoint(Point pt, TerminalRegion region, int type) {   

    GridEntry retval = null;
    int direction = getDirection(region.getTerminalVector());    
    if (type == IS_RUN) {
      addTempEntryForRun(pt, region.id, direction);
    } else if (type == IS_PAD) {
      retval = new GridEntry(region.id, IS_PAD, getReverse(direction), NONE, true);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Return the set of link crossings
  */

  public Set<Link> getAllCrossings() {
    HashSet<Link> crossings = new HashSet<Link>();
    Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      getCrossings(gval, crossings);
    }
    return (crossings);
  }  
  
  /***************************************************************************
  **
  ** Build a link crossing graph
  */

  public GraphColorer buildCrossingGraph() {
    HashSet<String> nodes = new HashSet<String>();
    HashSet<Link> links = new HashSet<Link>();
    
   Iterator<Point> kit = new PatternIterator(false, false);
    while (kit.hasNext()) {
      Point pt = kit.next();
      GridContents gval = getGridContents(pt);
      if (gval == null) {
        continue;
      }
      getNodeIDs(gval, nodes);  
      getCrossings(gval, links);
    }
    return (new GraphColorer(nodes, links));
  }
   
  /***************************************************************************
  **
  ** Return node IDs found.  May be more than one.
  */
  
  private void getNodeIDs(GridContents gc, Set<String> nodeIDs) {
    
    if (gc == null) {
      return;
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      if (gc.entry.type == IS_NODE) {
        nodeIDs.add(gc.entry.id);
      }
      return;
    }
    
    int size = gc.extra.size();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if (currEntry.type == IS_NODE) {
        nodeIDs.add(currEntry.id);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Return link crossings found.  May be more than one.
  */
  
  private void getCrossings(GridContents gc, Set<Link> crossings) {
    
    if (gc == null) {
      return;
    }
 
    if (gc.entry != null) {
      if (gc.extra != null) {
        throw new IllegalStateException();
      }
      return;
    }
    
    int size = gc.extra.size();
    HashSet<String> runsPresent = new HashSet<String>();
    for (int i = 0; i < size; i++) {
      GridEntry currEntry = gc.extra.get(i);
      if ((currEntry.type == IS_CORNER) || 
          (currEntry.type == IS_PAD) ||
          (currEntry.type == IS_RUN)) {
        if (!runsPresent.contains(currEntry.id)) {
          Iterator<String> rpit = runsPresent.iterator();
          while (rpit.hasNext()) {
            String present = rpit.next();
            crossings.add(new Link(present, currEntry.id));
            crossings.add(new Link(currEntry.id, present));
          }
          runsPresent.add(currEntry.id);
        }
      }
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Clear out all the links associated with the given key (sourceID for regular
  ** links, link tree ID for net module links:
  */
  
  private void drawnLinkAccounting(Genome genome, String linkKey, String overlayKey) {
    if (overlayKey != null) {
      throw new IllegalArgumentException();  // Functionality not yet supported
    } 
    
    Set<String> allLinks = genome.getOutboundLinks(linkKey);
    drawnLinks_.removeAll(allLinks); 
    
  }
  
  /***************************************************************************
  ** 
  ** Find out all the links associated with the given key (sourceID for regular
  ** links, link tree ID for net module links:
  */
  
  private HashSet<String> drawnLinksForID(Genome genome, String linkKey, String overlayKey) {
    if (overlayKey != null) {
      throw new IllegalArgumentException();  // Functionality not yet supported
    } 
    Set<String> allLinks = genome.getOutboundLinks(linkKey);
    HashSet<String> drawnForSource = new HashSet<String>();
    DataUtil.intersection(allLinks, drawnLinks_, drawnForSource); 
    return (drawnForSource);   
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Used to iterate over a pattern, including grid entries for groups if requested
  */
  
  private class PatternIterator implements Iterator<Point> {
    
    private Set<Point> keySet_;
    private Iterator<Point> gridIterator_;
    private Iterator<String> groupIterator_;
    private ArrayList<Point> currentGroupList_;
     
    PatternIterator(boolean doGroups, boolean allowDeletes) {
      // copy so we can delete as we go:
      if (allowDeletes) {
        keySet_ = new HashSet<Point>(patternx_.keySet());
      } else {
        keySet_ = patternx_.keySet();
      }
      gridIterator_ = keySet_.iterator();      
      if (doGroups) {
        groupIterator_ = groups_.keySet().iterator();
      }
      currentGroupList_ = new ArrayList<Point>();
    }
    
    public boolean hasNext() {
      if (gridIterator_.hasNext()) {
        return (true);
      }
      if (groupIterator_ == null) {
        return (false);
      }
      
      if (currentGroupList_.isEmpty()) {
        stockGroupList();
      }
      
      return (currentGroupList_.size() > 0);
    }
    
    public Point next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      if (gridIterator_.hasNext()) {
        return (gridIterator_.next());
      }
      if (currentGroupList_.isEmpty()) {
        String nextGroup = groupIterator_.next();
        Rectangle rect = groups_.get(nextGroup);
        int grpX = rect.x / 10;
        int grpY = rect.y / 10;
        int grpW = rect.width / 10;
        int grpH = rect.height / 10;
        for (int x = 0; x < grpW; x++) {
          for (int y = 0; y < grpH; y++) {
            int xval = grpX + x;
            int yval = grpY + y;
            Point nextPt = new Point(xval, yval); 
            currentGroupList_.add(nextPt);
          }
        }
      }
      int cslSize = currentGroupList_.size();
      return (currentGroupList_.remove(cslSize - 1));
    }
    
    private void stockGroupList() {
      while (groupIterator_.hasNext()) {
        String nextGroup = groupIterator_.next();
        Rectangle rect = groups_.get(nextGroup);
        int grpX = rect.x / 10;
        int grpY = rect.y / 10;
        int grpW = rect.width / 10;
        int grpH = rect.height / 10;
        for (int x = 0; x < grpW; x++) {
          for (int y = 0; y < grpH; y++) {
            int xval = grpX + x;
            int yval = grpY + y;
            Point nextPt = new Point(xval, yval);
            if (!keySet_.contains(nextPt)) {
              currentGroupList_.add(nextPt);
            }
          }
        }
        if (!currentGroupList_.isEmpty()) {
          return;
        }
      }
      return;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    } 
  }  
  
  /***************************************************************************
  **
  ** Used to iterate over a pattern, including grid entries for groups
  */
  
  private class PatternValueIterator implements Iterator<GridContents> {
    
    private PatternIterator iterator_;
    private boolean doGroups_;
 
    PatternValueIterator(boolean doGroups) {
      doGroups_ = doGroups;
      iterator_ = new PatternIterator(doGroups, false);
    }
    
    public boolean hasNext() {
       return (iterator_.hasNext());
    }
    
    public GridContents next() {
      Point pt = iterator_.next();
      return (getGridContents(pt, doGroups_));
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }     
  }    

  /***************************************************************************
  **
  ** Holds data for drawing/checking links
  */
  
  private static class FillData { 
    int startX;
    int incX;
    int endX;
    int startY;
    int incY; 
    int endY;
    GridEntry startEntry;
    GridEntry runEntry;
    GridEntry endEntry;
  }
  
  /***************************************************************************
  **
  ** Holds data for a grid entry
  */
  
  private static class GridEntry implements Cloneable {
    
    String id;
    int type;
    int entries;
    int entryCount;
    int exits;
    private boolean isTemp_;
    // Overlaid/saved temp entry actually stuff not needed...add-on corners not merged until no longer temp...
    GridEntry preTemp;

    GridEntry(GridEntry other) {
      this.id = other.id;
      this.type = other.type;
      this.entries = other.entries;
      this.entryCount = other.entryCount;
      this.exits = other.exits;
      this.isTemp_ = other.isTemp_;
      if (other.preTemp != null) {
        this.preTemp = new GridEntry(other.preTemp);
      }
    }
    
    GridEntry(String id, int type, int entries, int exits, boolean isTemp) {
      this.id = id;
      this.type = type;
      this.entries = entries;
      this.entryCount = (entries != NONE) ? 1 : 0;
      this.exits = exits;
      this.isTemp_ = isTemp;
      this.preTemp = null;
    }
    
    public GridEntry clone() {
      try {
        GridEntry retval = (GridEntry)super.clone();
        if (this.preTemp != null) {
          retval.preTemp = (GridEntry)this.preTemp.clone();
        }
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
   public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof GridEntry)) {
        return (false);
      }
      GridEntry otherEnt = (GridEntry)other;
      if (!(this.id.equals(otherEnt.id) &&
            (this.type == otherEnt.type) &&
            (this.entries == otherEnt.entries) &&
            (this.entryCount == otherEnt.entryCount) &&
            (this.exits == otherEnt.exits) &&
            (this.isTemp_ == otherEnt.isTemp_))) {
        return (false);
      }
      
      if (this.preTemp == null) {
        return (otherEnt.preTemp == null);
      }
     
      return (this.preTemp.equals(otherEnt.preTemp));
    }    
    
    
    
    void commitTemp() {
      isTemp_ = false;
      preTemp = null;
      return;
    }
    
    void clearTemp() {
      if (preTemp != null) {
        this.id = preTemp.id;
        this.type = preTemp.type;
        this.entries = preTemp.entries;
        this.entryCount = preTemp.entryCount;
        this.exits = preTemp.exits;
      }      
      isTemp_ = false;
      preTemp = null;
      return;
    }
    
    void setToTemp() {
      GridEntry myTemp = new GridEntry(this);
      preTemp = myTemp;
      isTemp_ = true;
      return;
    }  
    
    boolean needsPostTempRestore() {
      return (preTemp != null);
    }      
    
    boolean isTemp() {
      return (isTemp_);
    }      

    public String toString() {
      return (id + " typ=" + type + " ent=" + entries + " entCount=" + 
              entryCount + " exits=" + exits + " tmp=" + isTemp_ + " pretemp=" + preTemp);     
    } 
    
    boolean canPassThrough(String id, String trg, String linkID, 
                           Set<String> okGroups, 
                           int direction, Set<String> groupBlockers, Set<String> exemptions,
                           boolean isDirectDepart, boolean ignoreMyTemps) {
      return (canPassThroughCountCrossings(id, trg, linkID, okGroups, direction, 
                                           groupBlockers, exemptions, isDirectDepart, ignoreMyTemps) >= 0);
    }
      
    // Return -1 for no passage, else return the crossing count  
    int canPassThroughCountCrossings(String id, String trg, String linkID, 
                                     Set<String> okGroups, int direction, 
                                     Set<String> groupBlockers, Set<String> exemptions, 
                                     boolean isDirectDepart, boolean ignoreMyTemps) {
      if (this.type == IS_GROUP) {
        if (okGroups != null) {
          if (okGroups.contains(this.id)) {
            return (0);
          } else {
            if (groupBlockers != null) {
              groupBlockers.add(this.id);
            }
            return (-1);
          }
        } else {
          return (0);
        }
      }
                                            
      if ((trg != null) && (this.id.equals(trg)) && (this.type == IS_NODE_INBOUND_OK)) {
        return (0);
      }      
      if ((id != null) && this.id.equals(id)) {
        if ((this.type == IS_RESERVED_DEPARTURE_RUN) || isDirectDepart) {
          return (0);
        }
      }
      if ((id != null) && this.id.equals(id) && (this.type == IS_RESERVED_MULTI_LINK_ARRIVAL_RUN)) {
        return (0);
      }
      
      if ((linkID != null) && this.id.equals(linkID) &&(this.type == IS_RESERVED_ARRIVAL_RUN)) {
        return (0);
      }
      
      if ((exemptions != null) && (this.type == IS_NODE_INBOUND_OK) && exemptions.contains(this.id)) {
        return (0);
      } 

      if (id.equals(this.id)) {
        if (this.isTemp_ && ignoreMyTemps) {
          return (0); //(this.type != IS_RUN) ? -1 : 0);
        } else {
          return (-1);
        }
      }
        
      if (this.type != IS_RUN) {
        return (-1);
      }
      
      boolean canCross = false;
      switch (direction) {
        case NORTH:
        case SOUTH:
          canCross = (((entries & NORTH) == 0x00) && ((exits & SOUTH) == 0x00) &&
                      ((entries & SOUTH) == 0x00) && ((exits & NORTH) == 0x00));
          break;
        case EAST:     
        case WEST:
          canCross = (((entries & EAST) == 0x00) && ((exits & WEST) == 0x00) &&
                      ((entries & WEST) == 0x00) && ((exits & EAST) == 0x00));
          break;
        default:
          throw new IllegalArgumentException();
      }
      return (canCross ? 1 : -1);
    }

    
    boolean canMergeEntries(GridEntry other, List<String> links, List<String> targs, List<GenomeInstance.GroupTuple> tups) {
      if (targs.contains(this.id) && 
          (this.type == IS_NODE_INBOUND_OK) &&
          ((other.type == IS_RUN) || (other.type == IS_CORNER))) {
        return (true);
      }
         
      if (targs.contains(other.id) && 
          (other.type == IS_NODE_INBOUND_OK) &&
          ((this.type == IS_RUN) || (this.type == IS_CORNER))) {
        return (true);
      } 
      
      //
      // FIX ME!!! These are weak cases!  But if a run is continuing on
      // horizonally to another gene, it no longer knows about the target
      // of the upstream link segment
      //

      if ((tups != null) && !tups.isEmpty()) {
        if (this.type == IS_GROUP) {
          int numTup = tups.size();
          for (int i = 0; i < numTup; i++) {
            GenomeInstance.GroupTuple tup = tups.get(i);
            if (this.id.equals(tup.getSourceGroup()) || this.id.equals(tup.getTargetGroup())) {
              return (true);
            }
          }
          return (false);
        }
      }
      
      // FIX ME??? Is this a kludge?
      if (targs.contains(this.id) && (this.type == IS_NODE)) {
        return (true);
      } 
      if (targs.contains(other.id) && (other.type == IS_NODE)) {
        return (true);
      }      
      if ((this.id.equals(other.id)) && (this.type == IS_RESERVED_DEPARTURE_RUN)) {
        return (true);
      }
      if ((this.id.equals(other.id)) && (this.type == IS_RESERVED_MULTI_LINK_ARRIVAL_RUN)) {
        return (true);
      }     
      // FIX ME??? Is this a kludge?  Is this for departures?
      if (this.id.equals(other.id) && 
          (this.type == IS_NODE) && 
          ((other.type == IS_RUN) || (other.type == IS_PAD) || (other.type == IS_CORNER))) {
        return (true);
      } 
      if (this.id.equals(other.id) && 
          (other.type == IS_NODE) && 
          ((this.type == IS_RUN) || (this.type == IS_PAD) || (this.type == IS_CORNER))) {
        return (true);
      }     
      if (links.contains(this.id) && (this.type == IS_RESERVED_ARRIVAL_RUN)) {
        return (true);
      }
      if (mergableRuns(other)) {
        return (true);
      }
      if (mergableCorners(other)) {
        return (true);
      }
      if (mergableRunToCorner(other)) {
        return (true);
      }
      return (false);
    }       
   
    //
    // Answers if the two grid entries represent an illegal node overlap
    //
    
    boolean hasNodeCollision(GridEntry other) {
  
      //
      // If at least one entry is a group, we are happy:
      //
      
      if ((this.type == IS_GROUP) || (other.type == IS_GROUP)) {
        return (false);  
      }
      
      //
      // If neither type is a node type, we are happy:
      //
      
      if (((this.type != IS_NODE) && (this.type != IS_NODE_INBOUND_OK)) &&
          ((other.type != IS_NODE) && (other.type != IS_NODE_INBOUND_OK))) {
        return (false);
      }
      
      //
      // If somebody is a straight node, no overlaps are permitted:
      //
      
      if ((this.type == IS_NODE) || (other.type == IS_NODE)) {
        return (true);
      }
      
      //
      // All we are left with is allowed inbound overlap regions and links.
      // Should check for target/inbound match (FIX ME).  For now, just
      // say its OK.
      //
      
      return (false);
    }    
    

    boolean mergableRuns(GridEntry other) { 
      if ((other.id.equals(this.id)) || (this.type != IS_RUN) || (other.type != IS_RUN)) {
        return (false);
      }
      boolean thisIsEW = (((this.entries & NORTH) == 0x00) && ((this.exits & SOUTH) == 0x00) &&
                          ((this.entries & SOUTH) == 0x00) && ((this.exits & NORTH) == 0x00));
      boolean thisIsNS = (((this.entries & EAST) == 0x00) && ((this.exits & WEST) == 0x00) &&
                          ((this.entries & WEST) == 0x00) && ((this.exits & EAST) == 0x00));      
      boolean otherIsEW = (((other.entries & NORTH) == 0x00) && ((other.exits & SOUTH) == 0x00) &&
                           ((other.entries & SOUTH) == 0x00) && ((other.exits & NORTH) == 0x00));
      boolean otherIsNS = (((other.entries & EAST) == 0x00) && ((other.exits & WEST) == 0x00) &&
                           ((other.entries & WEST) == 0x00) && ((other.exits & EAST) == 0x00));
      
      return ((thisIsEW && otherIsNS) || (thisIsNS && otherIsEW));
    }
    
    boolean mergableCorners(GridEntry other) { 
      if ((!other.id.equals(this.id)) || (this.type != IS_CORNER) || (other.type != IS_CORNER)) {
        return (false);
      }
      return ((this.entries & other.entries & this.exits & other.exits) == 0x00);
    }    
    
    boolean mergableRunToCorner(GridEntry other) { 
      if ((!other.id.equals(this.id)) || (this.type != IS_RUN) || (other.type != IS_CORNER)) {
        return (false);
      }
      return ((this.entries & other.entries & this.exits & other.exits) == 0x00);
    }    
        
    static int getCardinalOppositeDirection(int direction) {
      switch (direction) {
        case NORTH:
          return (SOUTH);
        case SOUTH:
          return (NORTH);
        case WEST:
          return (EAST);
        case EAST:
          return (WEST);
        default:
          return (NONE);
      }
    }
    
    int cornerType() {
      if (type != IS_CORNER) {
        return (NO_CORNER);
      }
      if (entryCount > 1) {
        return (MULTI_CORNER);
      }
      
      int opposite = getCardinalOppositeDirection(exits);
      if (opposite == NONE) {  // diagonal or multi exits
        return (COMPLEX_CORNER);
      }
      
      return ((entries == opposite) ? STRAIGHT_CORNER : SINGLE_BEND_CORNER);      
    }
    
    int runDirection() {
      if (type != IS_RUN) {
        return (NOT_RUN);
      }
      if (((entries & NORTH) != 0x00) && ((exits & SOUTH) != 0x00) ||
          ((entries & SOUTH) != 0x00) && ((exits & NORTH) != 0x00)) {
        return (VERT_RUN);
      }
      if (((entries & WEST) != 0x00) && ((exits & EAST) != 0x00) ||
          ((entries & EAST) != 0x00) && ((exits & WEST) != 0x00)) {
        return (HORZ_RUN);
      }
      return (BAD_RUN);
    }    

    // answer if we can turn
    boolean entryOKForCorner(String src, String trg, String linkID, Set<String> okGroups) {
      switch (type) {
        case IS_NODE_INBOUND_OK:
          return ((trg != null) && id.equals(trg));
        case IS_RESERVED_ARRIVAL_RUN: 
         return ((linkID != null) && id.equals(linkID));
        case IS_RESERVED_MULTI_LINK_ARRIVAL_RUN:
          return ((src != null) && id.equals(src)); 
        case IS_RESERVED_DEPARTURE_RUN:
          return ((src != null) && id.equals(src));
        case IS_GROUP:
          return ((okGroups == null) || okGroups.contains(id));
        default:
          return (false);
      }
    }
    
    List<Vector2D> availableExits() {
      ArrayList<Vector2D> retval = new ArrayList<Vector2D>();
      if (((entries & NORTH) == 0x00) && ((exits & NORTH) == 0x00)) {
        retval.add(getVector(NORTH));
      }
      if (((entries & SOUTH) == 0x00) && ((exits & SOUTH) == 0x00)) {
        retval.add(getVector(SOUTH));
      }
      if (((entries & EAST) == 0x00) && ((exits & EAST) == 0x00)) {
        retval.add(getVector(EAST));
      }
      if (((entries & WEST) == 0x00) && ((exits & WEST) == 0x00)) {
        retval.add(getVector(WEST));
      }      
      return (retval);
    }
  }
  
  //
  // Class fro grid contents
  //
  
  public static class GridContents implements Cloneable {
    
    GridEntry entry; // First entry
    ArrayList<GridEntry> extra; // added entries (if any)
    
    GridContents(GridEntry entry) {
      this.entry = entry;
    } 
    
    public String toString() {
      return ("GridContents: " + entry + " " + extra);      
    }
    
    public GridContents clone() {
      try {
        GridContents retval = (GridContents)super.clone();
        if (this.entry != null) {
          retval.entry = this.entry.clone();
        }
        if (this.extra != null) {
          retval.extra = new ArrayList<GridEntry>();
          int numExt = this.extra.size();
          for (int i = 0; i < numExt; i++) {
            GridEntry nextra = this.extra.get(i);
            retval.extra.add(nextra.clone());
          }
        }
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    void mergeContents(GridEntry newEntry) {
      //
      // If the sources are different, we make different entries.  Separate
      // runs from the same source get separate entries, as do runs and
      // corners or pads.  If we have two corners from the same source,
      // we combine them.
      //

      if (entry != null) {
        if (extra != null) {
          throw new IllegalStateException();
        }
        if (entry.id.equals(newEntry.id)) {
          if (resolveSameSource(entry, newEntry)) {
            return;
          } 
        }
        extra = new ArrayList<GridEntry>();
        extra.add(entry);
        entry = null;
        extra.add(newEntry);
        return;
      }

      int size = extra.size();
      for (int i = 0; i < size; i++) {
        GridEntry currEntry = extra.get(i);
        if (currEntry.id.equals(newEntry.id)) {
          if (resolveSameSource(currEntry, newEntry)) {
            return;
          }
        }
      }
      extra.add(newEntry);
      return;
    }
  
    void mergeContentsForFinalCorner(String src, int exitDir) {      
      if (entry != null) {
        if (extra != null) {
          throw new IllegalStateException();
        }
        if (entry.id.equals(src)) {
          if (resolveSameSourceForFinalCorner(entry, src, exitDir)) {
            return;
          } 
        }
        //System.out.println("bad final contents " + gc.entry);
        return;
        //throw new IllegalStateException();
      }


      int size = extra.size();
      for (int i = 0; i < size; i++) {
        GridEntry currEntry = extra.get(i);
        if (currEntry.id.equals(src)) {
          if (resolveSameSourceForFinalCorner(currEntry, src, exitDir)) {
            return;
          }
        }
      }
      //for (int i = 0; i < size; i++) {
       // GridEntry currEntry = (GridEntry)gc.extra.get(i);
        //System.out.println("bad final contents" + currEntry);
     // }
     // throw new IllegalStateException();
      return;
    }    
  
    private boolean resolveSameSource(GridEntry existing, GridEntry newEntry) {
      //
      // If the sources are different, we make different entries.  Separate
      // runs from the same source get separate entries, as do runs and
      // corners or pads.  If we have two corners from the same source,
      // we combine
      //

      if (!existing.id.equals(newEntry.id)) {
        throw new IllegalArgumentException();
      }

      // Note:  Sometimes we see identical merges, from converting terminal
      // regions that contain a run.  We don't care!
      //if (existing.equals(newEntry)) {
        // Don't care
      //}

      if ((existing.type != IS_CORNER) || (newEntry.type != IS_CORNER)) {
        return (false);
      }


      //
      // Both of these entries are corners from the same source.  Fold them together.
      //

      // FIXME!!!!!
      if (existing.isTemp() && newEntry.isTemp()) {
        return (true);
      }

      if (newEntry.isTemp()) {
        existing.setToTemp();
      }    
      existing.entries |= newEntry.entries;
      existing.exits |= newEntry.exits;
      existing.entryCount += newEntry.entryCount;

      return (true);
    }
  
    private boolean resolveSameSourceForFinalCorner(GridEntry existing, String src, int exitDir) {
      //
      // If the sources are different, we make different entries.  Separate
      // runs from the same source get separate entries, as do runs and
      // corners or pads.  If we have two corners from the same source,
      // we combine
      //

      if (!existing.id.equals(src)) {
        throw new IllegalArgumentException();
      }

      if (existing.type != IS_CORNER) {
        return (false);
      }

      //
      // Both of these entries are corners from the same source.  Fold them together by setting
      // throws final exit direction.
      //

      existing.exits = exitDir;
      return (true);
    }  
  }
  
  private static class WalkValues {  
    int startX;
    int incX;
    int endX;
    int startY;
    int incY;
    int endY;
    int entryDirection;
    int exitDirection;
    
    public String toString() {
      return (startX + " " + incX + " " + endX + " " + 
              startY + " " + incY + " " + endY + " " + entryDirection + " " + exitDirection);     
    }   
  }
  
  private static class TravelResult {
    boolean needATurn;
    Point2D farthestPt;
    double fraction;
    double travelDistance;
    int crossings;
    double detourDistance;
    boolean stoppedByTarget;
    Set<String> groupBlocker;
    
    TravelResult(boolean needATurn, Point2D farthestPt, 
                 double fraction, int crossings, 
                 double detourDistance, double travelDistance, 
                 Set<String> groupBlocker, boolean stoppedByTarget) {
      this.needATurn = needATurn;   
      this.farthestPt = farthestPt;
      this.fraction = fraction;
      this.travelDistance = travelDistance;
      this.crossings = crossings;
      this.detourDistance = detourDistance;
      this.groupBlocker = groupBlocker;
      this.stoppedByTarget = stoppedByTarget;
    } 
    
    public String toString() {        
      return ("TravelResult: needATurn" + needATurn +   
              " farthestPt="+ farthestPt +
              " fraction="+ fraction +
              " crossings="+ crossings +
              " detourDistance="+ detourDistance +
              " groupBlocker="+ groupBlocker); 
    }   
  }

  // Make this package viz for DOF analysis
  static class TravelTurn {  
    Point2D start;
    Point2D runTerminus;
    Vector2D runDirection;
    Vector2D primaryTurnDirection;
    Vector2D secondaryTurnDirection;    
    boolean isDirect;
    
    TravelTurn() {
    }
    
    public String toString() {        
      return ("TravelTurn: st=" + start + " rt=" + runTerminus + " rd=" + runDirection + 
                         " ptd=" + primaryTurnDirection + " std=" + secondaryTurnDirection +
                         " id=" + isDirect);  
    
    }   
  }   

  private static class TravelResultPair {  
    TravelResult primary;
    TravelResult secondary;

    TravelResultPair(TravelResult primary, TravelResult secondary) {
      this.primary = primary;
      this.secondary = secondary;
    }
    
    public String toString() {        
      return ("TravelResultPair: prim=" + primary + " sec=" + secondary);      
    }   

  }  
  
  private static class TravelOptions {  
    List<TravelResultPair> travels;
    TravelTurn turn;
    Set<String> groupBlockers;

    TravelOptions() {
    }
  }
  
  private static class IterationChecker {
    int max;
    int count;

    IterationChecker(int max) {
      this.max = max;
      this.count = 0;
    }
    
    void bump() throws NonConvergenceException {
      count++;     
      if (count >= max) {
        throw new NonConvergenceException();
      }
      return;
    }
    
    boolean limitExceeded() {
      return (count >= max);
    }
  }
  
  private static class StartIndex {  
    int index;
    Vector2D direction;

    StartIndex(int index, Vector2D direction) {
      this.index = index;
      this.direction = direction;
    }
    
    public String toString() {
      return ("StartIndex: " + index + " " + direction);  
    }  
  }    
  
  private static class BestDetour {  
    int index;
    boolean usePrimary;

    BestDetour(int index, boolean usePrimary) {
      this.index = index;
      this.usePrimary = usePrimary;
    }
    
    public String toString() {
      return ("BestDetour: " + index + " " + usePrimary);  
    }  
  }  

  private static class TravelPlan {  
    Point2D start;
    Point2D corner;
    Point2D farthestPt;    
    Vector2D runDirection;
    Vector2D turnDirection;
    
    TravelPlan(Point2D start, Point2D corner, Point2D farthestPt, 
               Vector2D runDirection, Vector2D turnDirection) {

      this.start = start;
      this.corner = corner;
      this.farthestPt = farthestPt;      
      this.runDirection = runDirection;
      this.turnDirection = turnDirection;
    }
    
    public String toString() {
      return ("TravelPlan: st=" + start + " co=" + corner + " fp=" + farthestPt + 
                          " rd=" + runDirection + " td=" + turnDirection);  
    }  
  }
  
  private static class MyCornerOracle implements CornerOracle {  
    private Point2D start_;
    private Point2D end_;
    
    MyCornerOracle(Point2D start, Point2D end) {
      start_ = start;
      end_ = end;
    }
    
    public boolean isCornerPoint(Point2D pt) {
      if ((start_ != null) && (start_.equals(pt))) {
        return (false);
      }
      if ((end_ != null) && (end_.equals(pt))) {
        return (false);
      }
      return (true);
    }
    
  }
  
  public static class PointAlternative implements Cloneable {  
    Point point;
    int type;
    int dir;
    
    PointAlternative(Point point, int type, int dir) {
      this.point = point;
      this.type = type;
      this.dir = dir;
    }

    public PointAlternative clone() {
      try {
        PointAlternative retval = (PointAlternative)super.clone();
        retval.point = (Point)this.point.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }  

    public String toString() {
      return ("PointAlternative: " + point + " " + type + " " + dir);  
    }
    
    public int hashCode() {
      return (point.hashCode() + type + dir);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof PointAlternative)) {
        return (false);
      }
      PointAlternative otherAlt = (PointAlternative)other;
      return (this.point.equals(otherAlt.point) && (this.type == otherAlt.type) && (this.dir == otherAlt.dir));
    }
  }
  
  private static class LaunchAnalysis {  
    HashMap<Integer, SortedMap<Double, List<PointAlternative>>> binnedResults;
    ArrayList<Integer> bestOrder;
    private int currBinIndex;
   
    LaunchAnalysis() {
      binnedResults = new HashMap<Integer, SortedMap<Double, List<PointAlternative>>>();
      bestOrder = new ArrayList<Integer>();
      currBinIndex = 0;
    }
    
    @SuppressWarnings("unused")
    void addAlternative(PointAlternative pa, Integer binKey, Double dist) {     
      SortedMap<Double, List<PointAlternative>> distForBin = binnedResults.get(binKey);
      if (distForBin == null) {
        distForBin = new TreeMap<Double, List<PointAlternative>>();
        binnedResults.put(binKey, distForBin);
      }
      List<PointAlternative> ptsForDist = distForBin.get(dist);
      if (ptsForDist == null) {
        ptsForDist = new ArrayList<PointAlternative>();
        distForBin.put(dist, ptsForDist);
      }      
      ptsForDist.add(pa);
      return;
    }

    @SuppressWarnings("unused")
    void rankAlternatives() {
      TreeMap<Double, Integer> buildOrder = new TreeMap<Double, Integer>();
      Iterator<Integer> brkit = binnedResults.keySet().iterator();
      while (brkit.hasNext()) {
        Integer binKey = brkit.next();
        SortedMap<Double, List<PointAlternative>> distForBin = binnedResults.get(binKey);
        Double minDist = distForBin.firstKey();
        buildOrder.put(minDist, binKey);
      }
      bestOrder.addAll(buildOrder.values());
      return;
    }

    PointAlternative extractNextAlternative() {
      if (bestOrder.size() == 0) {
        return (null);
      }
      int startIndex = currBinIndex;
      boolean wrapped = false;
      while (true) {
        if ((currBinIndex == startIndex) && wrapped) {
          return (null);
        }
        Integer nextBestBin = bestOrder.get(currBinIndex++);
        if (currBinIndex >= bestOrder.size()) {
          currBinIndex = 0;
          wrapped = true;
        }
        SortedMap<Double, List<PointAlternative>> distForBin = binnedResults.get(nextBestBin);
        if (distForBin.isEmpty()) {
          continue;
        }
        Double firstKey = distForBin.firstKey();
        List<PointAlternative> ptsForDist = distForBin.get(firstKey);
        PointAlternative retval = ptsForDist.remove(0);
        if (ptsForDist.isEmpty()) {
          distForBin.remove(firstKey);
        }
        return (retval);
      }
    }
  }
  
  private static class TaggedPlan {  
    TravelPlan partialPlan;
    int returnCode;
   
    TaggedPlan(int returnCode, TravelPlan partialPlan) {
      this.returnCode = returnCode; 
      this.partialPlan = partialPlan;
    } 
  }
  
  private static class RecoveryJumpResult {
    ArrayList<Point2D> jumpDetour;
    ArrayList<TravelPlan> tempedPlans;
    List<Point2D> threePts;
    Point2D detourStart;
    Point2D detourEnd;    
    boolean exitWithCode;
    int currDepth;
    int exitCode;
  } 
  
  private static class SimpleRecoveryResult {
    int recovResult;
    TravelPlan noTurnPlan;
    boolean exitWithCode;
    int exitCode;
  }     
  
  private static class JumpPreparations {
    TravelTurn tryTurn;
    Point2D jumpTarg;
    boolean exitWithCode;
    int exitCode;
    int currDepth;
  }
  
  private static class DiagonalResult { 
    Point2D startPt;
    Point2D endPt;
    List<Point2D> threePts;
    boolean exitWithCode;
    int currDepth;
    int exitCode;    
  }
  
  private static class PlanOrPoint { 
    TravelPlan plan;
    Point2D point;
    
    PlanOrPoint(TravelPlan plan, Point2D point) {
      this.plan = plan; 
      this.point = point;
    } 
  
  } 
  
  private static class PointAlternativeForRecovery { 
    PointAlternative pa;
    List<Point2D> threePts;
    Point end;
    RecoveryDataForLink truncatedRFL;
    
    PointAlternativeForRecovery(PointAlternative pa, List<Point2D> threePts, Point end, RecoveryDataForLink truncatedRFL) {
      this. pa = pa;
      this.threePts = threePts;
      this.end = end;
      this.truncatedRFL = truncatedRFL;   
    }
  }
 
  private static class DOFState {
    boolean haveOrthoPoints;
    @SuppressWarnings("unused")
    boolean haveOrthoCorner;
    List<Point2D> threePts;
    TravelTurn turn; 
  }
  
  
  public static class DecoratedPath {  
    private GeneralPath thePath_;
    private HashMap<Point2D, Object> theData_;
    
    public DecoratedPath() {
      thePath_ = new GeneralPath();
      theData_ = new HashMap<Point2D, Object>();
    }
  
    public GeneralPath getPath() {
      return (thePath_);
    }  
  
    public Map<Point2D, Object> getData() {
      return (theData_);
    }
      
    public void decorate(Point2D theStart, Object theData) {
      theData_.put(theStart, theData);
      return;
    }
  }

  /***************************************************************************
  **
  ** Used for hashable keys
  */
  
  public static class DecoInfoKey {
    public String treeID; 
    public LinkSegmentID segID; 
    
    public DecoInfoKey(String treeID, LinkSegmentID segID) {
      this.treeID = treeID;
      this.segID = segID;
    }
    
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof DecoInfoKey)) {
        return (false);
      }
      
      DecoInfoKey otherDIK = (DecoInfoKey)other; 

      if (!this.treeID.equals(otherDIK.treeID)) {
        return (false); 
      } 
        
      return (this.segID.equals(otherDIK.segID));
    }
    
    public int hashCode() {
      return (treeID.hashCode() + segID.hashCode());      
    }
  }
  
  /***************************************************************************
  **
  ** Used for multi-link grid decoration
  */
  
  public static class DecoInfo {
    
    public static final String MODULE_KEY = "__WJRL_DECO_BOGUS_KEY__";
    
    public String treeID; 
    public String srcID; 
    public LinkSegmentID segID;
    public Point2D firstPt; 
    public int normCanon; 
    public Point offsetForSrc;
    public MinMax traceRange;
    public boolean isModule; 
    
    public DecoInfo(String treeID, LinkSegmentID segID, String srcID, Point2D firstPt, int normCanon, Point offsetForSrc, MinMax traceRange) {
      this.treeID = treeID;
      this.segID = segID;
      this.srcID = srcID;
      this.firstPt = firstPt;
      this.normCanon = normCanon; 
      this.offsetForSrc = offsetForSrc;
      this.traceRange = traceRange;
      this.isModule = false;
    } 
    
    public DecoInfo(boolean isModule) {
      this.treeID = null;
      this.segID = null;
      this.srcID = null;
      this.firstPt = null;
      this.normCanon = Vector2D.NOT_CANONICAL; 
      this.offsetForSrc = null;
      this.traceRange = null;
      this.isModule = isModule;
    } 
    
    public DecoInfoKey getDIK() {
      return (new DecoInfoKey(treeID, segID));
    } 
    
    public String toString() {
      return ("DecoInfo: " + treeID + " " + segID + " " + srcID + " " +  firstPt + " " +  normCanon + " " + offsetForSrc + " " +  traceRange + " " +  isModule);  
    }

  }    
}
