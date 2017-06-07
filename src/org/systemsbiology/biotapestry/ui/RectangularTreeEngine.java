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

package org.systemsbiology.biotapestry.ui;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.util.Collections;

import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.analysis.CrossingReducer;
import org.systemsbiology.biotapestry.analysis.LayerAssignment;
import org.systemsbiology.biotapestry.analysis.TransReducer;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.PatternPlacerMinimizeHeight;
import org.systemsbiology.biotapestry.util.PatternPlacerVerticalFit;
import org.systemsbiology.biotapestry.util.SimpleLink;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltMotif;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** A Class
*/

public class RectangularTreeEngine {
  
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public RectangularTreeEngine() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Layout the given nodes in a rectangular pattern
  */
  
  public String[] layout(Set<String> nodes, Genome genome, boolean targsFirst) {
    
    //
    // Sort the nodes by the number of targets they have
    //    

    TreeSet<TargetRank> ranking = new TreeSet<TargetRank>();
    Iterator<String> nit = nodes.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      int count = 0;
      ArrayList<String> list = new ArrayList<String>();
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        if (link.getSource().equals(nodeID)) {
          String trgID = link.getTarget();
          if (nodes.contains(trgID)) {
            list.add(trgID);
            count++;
          }
        }
      }
      TargetRank tr = new TargetRank(nodeID, count, list);
      ranking.add(tr);
    }
    
    //
    // Order the targets
    //
    
    HashSet<String> placed = new HashSet<String>();
    int numNodes = nodes.size();
    String[] positions = new String[numNodes];
    int currPos = 0; 

    //
    // The first approach places active sources first, but places
    // targets between sources.  The second approach puts all sources
    // first
    //
   
    if (targsFirst) {
      ArrayList<TargetRank> rankedList = new ArrayList<TargetRank>(ranking);
      int rankSize = rankedList.size();
      for (int i = rankSize - 1; i >= 0; i--) {
        TargetRank trank = rankedList.get(i);
        if ((trank.targets.size() > 0) && (!placed.contains(trank.nodeID))) {
          positions[currPos++] = trank.nodeID;
          placed.add(trank.nodeID);
          Iterator<String> trgit = trank.targets.iterator();
          while (trgit.hasNext()) {
            String targID = trgit.next();
            if (!placed.contains(targID)) {
              positions[currPos++] = targID;
              placed.add(targID);
            }
          }
        }
      }
      for (int i = rankSize - 1; i >= 0; i--) {
        TargetRank trank = rankedList.get(i);
        if (!placed.contains(trank.nodeID)) {
          positions[currPos++] = trank.nodeID;
          placed.add(trank.nodeID);
        }     
      } 
    } else {
      ArrayList<TargetRank> rankedList = new ArrayList<TargetRank>(ranking);
      int rankSize = rankedList.size();
      for (int i = rankSize - 1; i >= 0; i--) {
        TargetRank trank = rankedList.get(i);
        if ((trank.targets.size() > 0) && (!placed.contains(trank.nodeID))) {
          positions[currPos++] = trank.nodeID;
          placed.add(trank.nodeID);
        }
      }
      for (int i = rankSize - 1; i >= 0; i--) {
        TargetRank trank = rankedList.get(i);
        Iterator<String> trgit = trank.targets.iterator();
        while (trgit.hasNext()) {
          String targID = trgit.next();
          if (!placed.contains(targID)) {
            positions[currPos++] = targID;
            placed.add(targID);
          }
        }
      }
      for (int i = rankSize - 1; i >= 0; i--) {
        TargetRank trank = rankedList.get(i);
        if (!placed.contains(trank.nodeID)) {
          positions[currPos++] = trank.nodeID;
          placed.add(trank.nodeID);
        }     
      }
    }
    return (positions);
  }

  /***************************************************************************
  **
  ** Layout the given motifs in a hierarchical pattern.  
  */
  
  public Grid layoutMotifsHier(List<DialogBuiltMotif> motifList, Set<String> nodesToPlace, 
                               DataAccessContext rcx, LayoutOptions options, 
                               boolean incremental, String forceCore, boolean forceToLeft) {

    //
    // Work with all nodes, not just those to place:
    //
    
    HashSet<String> allNodes = new HashSet<String>();
    Iterator<Node> nit = rcx.getCurrentGenome().getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      allNodes.add(node.getID());
    }
    
    Set<Link> linksWithFeedback = new HashSet<Link>();
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      Link slink = new Link(src, trg);
      linksWithFeedback.add(slink);
    }
 
    return (layoutMotifsHierCore(allNodes, linksWithFeedback, motifList, nodesToPlace, options, incremental, forceCore, forceToLeft));    
    
  }
  
  /***************************************************************************
  **
  ** Layout the given motifs in a hierarchical pattern.  
  */
  
  private Grid layoutMotifsHierCore(Set<String> allNodes, Set<Link> allLinks, List<DialogBuiltMotif> motifList, Set<String> nodesToPlace, 
                                    LayoutOptions options, boolean incremental, 
                                    String forceCore, boolean forceToLeft) {
    
    //
    // Get a topo sort of the nodes.  Gotta force reproducible order for VfA to Vfg stacked layout consistency!
    // Thus the tree sets and the preprocess loop and the comparator...
    // 

    TreeSet<String> nodes = new TreeSet<String>(allNodes);
    ArrayList<Link> preLinks = new ArrayList<Link>();
    ArrayList<DialogBuiltMotif> sortedMList = new ArrayList<DialogBuiltMotif>();
    HashSet<String> targetOnly = new HashSet<String>(allNodes);
    
    Iterator<DialogBuiltMotif> mit = motifList.iterator();
    while (mit.hasNext()) {
      DialogBuiltMotif dbm = mit.next();
      int lcount = dbm.getLinkCount();
      for (int i = 0; i < lcount; i++) {
        Link cfl = dbm.getLink(i);
        preLinks.add(cfl);
        targetOnly.remove(cfl.getSrc());
      }
      sortedMList.add(dbm);
    }
    Collections.sort(sortedMList, new DialogBuiltMotif.DBMComparator());
    
    TreeSet<Link> links = new TreeSet<Link>();
    Iterator<Link> prit = preLinks.iterator();
    while (prit.hasNext()) {
      Link cfl = prit.next();
      links.add(cfl);
      if (targetOnly.contains(cfl.getTrg())) {
       continue;
      }     
      CycleFinder cf = new CycleFinder(nodes, links);
      if (cf.hasACycle()) {
        links.remove(cfl);
      }
    }

    GraphSearcher searcher = new GraphSearcher(nodes, links);    
    Map<String, Integer> topoSort = searcher.topoSort(options.topoCompress);
    if (forceCore != null) {
      topoSort = GraphSearcher.topoSortReposition(topoSort, forceCore, forceToLeft);  
    }  
         
    PatternGrid grid = new PatternGrid();
    
    // Required bogus conversion (FIX ME)
    Set<SimpleLink> simpleLinks = new HashSet<SimpleLink>();
    Iterator<Link> lit = allLinks.iterator();
    while (lit.hasNext()) {
      Link slink = lit.next();
      String src = slink.getSrc();
      String trg = slink.getTrg();
      simpleLinks.add(new SimpleLink(src, trg));      
    }

    //
    // Squash tall columns into several short ones:
    //
    
    if (options.layeringMethod == LayoutOptions.AD_HOC) {
      topoSort = squashSort(topoSort, options.maxPerLayer);
    } else {
      topoSort = layerAssignment(simpleLinks, topoSort, options.maxPerLayer, options.layeringMethod);      
    }
    //
    // Choose method:
    //
    
    if (options.firstPass == LayoutOptions.DO_SEQUENTIAL) {
      HashSet<String> placed = new HashSet<String>();
      int mSize = motifList.size();    
      for (int i = 0; i < mSize; i++) {
        DialogBuiltMotif dbm = sortedMList.get(i);
        placeMotifInPattern(dbm, grid, placed, allNodes, topoSort);
      }
    } else {
      if (options.firstPass != LayoutOptions.DO_RECURSIVE) {
        throw new IllegalStateException();
      }
      recursivePlaceMotifInPattern(sortedMList, grid, allNodes, topoSort);
    }

    Grid retGrid = patternToGrid(grid, nodesToPlace, simpleLinks, options, topoSort);
    if (options.normalizeRows  || incremental) {
      retGrid = normalizeGrid(retGrid);
    }
    return (retGrid);
  }  

  /***************************************************************************
  **
  ** Use the same general approach as above for fan-ins and fan-outs of GeneAndSatelliteClusters
  */
  
  public Grid layoutFanInOutHier(Set<String> allNodes, Set<Link> allLinks, Set<String> nodesToPlace, 
                                 List<DialogBuiltMotif> motifList, LayoutOptions options, String forceCore, boolean forceToLeft) {
    return (layoutMotifsHierCore(allNodes, allLinks, motifList, nodesToPlace, options, false, forceCore, forceToLeft));
  }  

  /***************************************************************************
  **
  ** Convert pattern to grid
  */
  
  private Grid patternToGrid(PatternGrid grid, Set<String> nodesToPlace, Set<SimpleLink> allLinks,
                             LayoutOptions options, Map<String, Integer> topoSort) {
    //
    // Transfer the pattern to a grid.  This is where existing nodes that were included
    // in the grid are thrown away.
    //
    
    MinMax xRange = grid.getMinMaxXForRange(null);
    MinMax yRange = grid.getMinMaxYForRange(null);

    if ((xRange == null) || (yRange == null)) {
      return (new Grid(new String[0][0], null, null, topoSort));
    }

    String[][] retval = new String[xRange.max - xRange.min + 1][yRange.max - yRange.min + 1];    
    String refID = null;
    Point refPt = null;
    
    for (int x = xRange.min; x <= xRange.max; x++) {
      for (int y = yRange.min; y <= yRange.max; y++) {
        String nodeID = grid.getValue(x, y);
        if (nodesToPlace.contains(nodeID)) {
          retval[x - xRange.min][y - yRange.min] = nodeID;
        } else if (refID == null) {
          refID = nodeID;
          refPt = new Point(x - xRange.min, y - yRange.min);
        }
      }
    }
    Grid retvalGrid = new Grid(retval, refID, refPt, topoSort);
    if (options.doCrossingReduction) {
      minimizeCrossingsInGrid(retvalGrid, allLinks);
    }
    return (retvalGrid);
  }
  
  /***************************************************************************
  **
  ** Place the nodes in the motif
  */
  
  private Set<String> placeMotifInPattern(DialogBuiltMotif dbm, PatternGrid grid, 
                                  Set<String> placed, Set<String> toPlace, Map<String, Integer> columnsForNodes) {
                                     
    DialogBuiltMotif.Placement pInfo = dbm.getPlacementInfo(columnsForNodes, placed, toPlace, grid);
    if (pInfo == null) {
      return (new HashSet<String>());
    }
    
    //
    // Placement scheme depends on whether nodes have been placed:
    //
    
    if (pInfo.suggestedRow == null) {
      PatternPlacerMinimizeHeight pp = 
        new PatternPlacerMinimizeHeight(grid, pInfo.pattern, pInfo.leftColumn.intValue());
      pp.placePattern();
    } else {
      PatternPlacerVerticalFit pp = 
        new PatternPlacerVerticalFit(grid, pInfo.pattern, 
                                     pInfo.suggestedRow.intValue(), pInfo.leftColumn.intValue());
      pp.placePattern();
    }
    
    Set<String> newlyPlaced = pInfo.pattern.getValues();
    Iterator<String> npit = newlyPlaced.iterator();
    while (npit.hasNext()) {
      String nodeID = npit.next();
      placed.add(nodeID);
    }
    return (newlyPlaced);
  }

  /***************************************************************************
  **
  ** Place the nodes in the motif
  */
  
  private void recursivePlaceMotifInPattern(List<DialogBuiltMotif> motifList, PatternGrid grid, 
                                            Set<String> toPlace, Map<String, Integer> columnsForNodes) {

    HashSet<String> placed = new HashSet<String>();
    int mSize = motifList.size();
    if (mSize == 0) {
      return;
    }
    ArrayList<DialogBuiltMotif> workingList = new ArrayList<DialogBuiltMotif>(motifList);
    while (!workingList.isEmpty()) {
      DialogBuiltMotif dbm = workingList.remove(0);
      recursivePlaceMotifInPatternCore(dbm, placed, workingList, grid, toPlace, columnsForNodes);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Place the nodes in the motif
  */
  
  private void recursivePlaceMotifInPatternCore(DialogBuiltMotif dbm, Set<String> placed,
                                                List<DialogBuiltMotif> motifList, PatternGrid grid, 
                                                Set<String> toPlace, Map<String, Integer> columnsForNodes) {

    //
    // Place the given motif, then place all other motifs that are targeted by
    // the motif
    //
    
    // 11-10-06 Can't do this!  Last guy can get missed!  Why has this worked for 2 years?
    //if (motifList.size() == 0) {
    //  return;
    //}
    Set<String> newlyPlaced = placeMotifInPattern(dbm, grid, placed, toPlace, columnsForNodes);
    Iterator<String> npit = newlyPlaced.iterator();
    while (npit.hasNext()) {
      String nodeID = npit.next();
      int index = 0;
      while (index < motifList.size()) {
        dbm = motifList.get(index);
        if (dbm.getSourceId().equals(nodeID)) {
          motifList.remove(index);
          recursivePlaceMotifInPatternCore(dbm, placed, motifList, grid, toPlace, columnsForNodes);
        } else {
          index++;
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Vertically squash the given column assignment
  */
  
  private Map<String, Integer> squashSort(Map<String, Integer> columnsForNodes, int maxRows) {
       
    //
    // Invert the map, to give a sorted list of nodes per column:
    //
    
    Integer maxColumn = new Integer(Integer.MIN_VALUE);
    HashMap<Integer, SortedSet<String>> invert = new HashMap<Integer, SortedSet<String>>();
    Iterator<String> cfnit = columnsForNodes.keySet().iterator();
    while (cfnit.hasNext()) {
      String nodeID = cfnit.next();
      Integer col = columnsForNodes.get(nodeID);
      SortedSet<String> nodes  = invert.get(col);
      if (nodes == null) {
        nodes = new TreeSet<String>();
        invert.put(col, nodes);
      }
      nodes.add(nodeID);
      if (col.compareTo(maxColumn) > 0) {
        maxColumn = col;
      }
    }
    
    //
    // Rebuild the node to column map, splitting extra-long
    // columns as we go:
    //
    
    int max = maxColumn.intValue();    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    int newCol = 0;
    for (int i = 0; i <= max; i++) {
      Integer currCol = new Integer(i);
      SortedSet<String> nodes  = invert.get(currCol);
      if (nodes == null) {
        newCol++;
        continue;
      }
      int size = nodes.size();
      int numCols = (int)Math.ceil((double)size /(double)maxRows);
      int numPerCol = (int)Math.round((double)size / (double)numCols);
      int currNodeIndex = 0;
      Iterator<String> nit = nodes.iterator();
      while (nit.hasNext()) {
        String node = nit.next();
        retval.put(node, new Integer(newCol));
        currNodeIndex++;
        if ((currNodeIndex % numPerCol) == 0) {
          newCol++;
        }
      }
    }
    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Assign links to layer
  */
  
  private Map<String, Integer> layerAssignment(Set<SimpleLink> linksWithFeedback, Map<String, Integer> topoSort, int max, int method) {
       
    Set<SimpleLink> links = new HashSet<SimpleLink>();
    Iterator<SimpleLink> lit = linksWithFeedback.iterator();
    while (lit.hasNext()) {
      SimpleLink slink = lit.next();
      //
      // Skip feedback links
      //
      String src = slink.getSrc();
      String trg = slink.getTrg();
      Integer srcLayer = topoSort.get(src); 
      Integer trgLayer = topoSort.get(trg);
      if (srcLayer.intValue() >= trgLayer.intValue()) {
        continue;
      }
      links.add(new SimpleLink(src, trg));
    }
    
    TransReducer tr = new TransReducer();
    links = tr.reduceGraph(links);
        
    LayerAssignment la = new LayerAssignment();
    return (la.assignLayers(topoSort, links, max, (method == LayoutOptions.COFFMAN_GRAHAM)));    
  }
  
 /***************************************************************************
  **
  ** Place a point
  */
  
  public Point2D placePoint(int i, String[] positions, Point2D center, 
                            Dimension dims, int layoutMode) {
    
    Grid grid = new Grid(positions, dims, layoutMode);    
    return (placePoint(i, grid, center, dims));         
  }
  
 /***************************************************************************
  **
  ** Place a point
  */
  
  public Point2D placePoint(int i, Grid grid, Point2D center, Dimension dims) { 
    
    Grid.RowAndColumn cAndR = grid.getRowAndColumn(i);
    int col = cAndR.col;
    int row = cAndR.row;
   
    Point2D loc = new Point2D.Double();
    double x = center.getX() - (dims.height / 2.0) + (grid.getColSpace() * col);
    double y = center.getY() - (dims.width / 2.0) + (grid.getRowSpace() * (row + 2));
    UiUtil.forceToGrid(x, y, loc, 10.0);
    return (loc);         
  }  

  /***************************************************************************
  **
  ** Decide the location of new blocks (to the right or below).
  */
  
  @SuppressWarnings("unused")
  private boolean newBlockToRight(Dimension oldBlock, Dimension newBlock) {
    
    //
    // Calculate the bounds placing the new rectangle to the right or below.
    // Choose the one that gives 4:3 aspect ratio (width/height).
    //
    
    int rightHeight = (oldBlock.height > newBlock.height) ? oldBlock.height : newBlock.height;
    int rightWidth = oldBlock.width + newBlock.width;
    
    int bottomHeight = oldBlock.height + newBlock.height;
    int bottomWidth = (oldBlock.width > newBlock.width) ? oldBlock.width : newBlock.width;

    double aspectRight = (double)rightWidth / (double)rightHeight;
    double aspectBot = (double)bottomWidth / (double)bottomHeight;    
    
    double botBottom = (1.0 / aspectBot) * (4.0 / 3.0);
    double botDiff = Math.abs(botBottom - 1.0);
    
    double rightRight = aspectRight * (4.0 / 3.0);
    double rightDiff = Math.abs(rightRight - (4.0 / 3.0));

    return (rightDiff < botDiff);
  } 
  /***************************************************************************
  **
  ** Permute the grid to minimize crossings
  */
  
  private void minimizeCrossingsInGrid(Grid grid, Set<SimpleLink> links) {
    
    int cols = grid.getNumCols();
    int rows = grid.getNumRows();    
    
    if (cols <= 1) {
      return;
    }
    Map<String, Integer> lastColumn = buildAugmentedPseudoColumn(grid, 0);
    
    CrossingReducer cr = new CrossingReducer();    

    for (int i = 1; i < cols; i++) {
      HashMap<String, Integer> currColumn = new HashMap<String, Integer>();
      for (int j = 0; j < rows; j++) {
        String node = grid.getCellValue(j, i);
        if (node != null) {
          currColumn.put(node, new Integer(j));
        }
      }    
      Map<String, Integer> permuted = cr.reduceCrossings(lastColumn, currColumn, links);
      Iterator<String> pit = permuted.keySet().iterator();
      while (pit.hasNext()) {
        String node = pit.next();
        Integer row = permuted.get(node);
        grid.setCellValue(row.intValue(), i, node);
      }
      lastColumn = buildAugmentedPseudoColumn(grid, i);
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Normalize the grid to eliminate holes and offsets
  */
  
  private Grid normalizeGrid(Grid grid) {
        
    int cols = grid.getNumCols();
    int rows = grid.getNumRows();
    HashMap<Integer, Integer> numsPerCol = new HashMap<Integer, Integer>();
    
    int maxPerCol = 0;
    for (int i = 0; i < cols; i++) {
      int numPerCol = 0;
      for (int j = 0; j < rows; j++) {
        String node = grid.getCellValue(j, i);
        if (node != null) {
          numPerCol++;
        }
      }
      numsPerCol.put(new Integer(i), new Integer(numPerCol));
      if (numPerCol > maxPerCol) {
        maxPerCol = numPerCol;
      }
    }  
      
    String[][] vals = new String[cols][maxPerCol];    
    String refID = grid.getReferenceID();
    Point refPt = grid.getReferencePoint();
    
    for (int i = 0; i < cols; i++) {
      int numPerCol = numsPerCol.get(new Integer(i)).intValue();
      int currRow = (maxPerCol - numPerCol) / 2;
      for (int j = 0; j < rows; j++) {
        String nodeID = grid.getCellValue(j, i);
        if (nodeID != null) {
          vals[i][currRow] = nodeID;
          if (nodeID.equals(refID)) {
            refPt = new Point(refPt.x, refPt.y + (currRow - j));
          }
          currRow++;
        }
      }
    }
    return (new Grid(vals, refID, refPt, grid.getTopoSort()));
  }  
  
 
  /***************************************************************************
  **
  ** Build a pseudo column that contains all nodes from all previous columns
  */
  
  private Map<String, Integer> buildAugmentedPseudoColumn(Grid grid, int column) {
    
    int rows = grid.getNumRows();    

    Map<String, Integer> retval = new HashMap<String, Integer>();
    int count = 0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j <= column; j++) {
        String node = grid.getCellValue(i, j);
        if (node != null) {
          retval.put(node, new Integer(count++));
        }
      }
    }   
    return (retval);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private class TargetRank implements Comparable<TargetRank> {
    
    String nodeID;
    int targetCount;
    List<String> targets;
        
    TargetRank(String nodeID, int targetCount, List<String> targets) {
      this.nodeID = nodeID;
      this.targetCount = targetCount;
      this.targets = targets;
    }
    
    public int compareTo(TargetRank other) {
      int diff = this.targetCount - other.targetCount;
      if (diff != 0) {
        return (diff);
      }
      return (this.nodeID.compareTo(other.nodeID));
    }    
  }
}
