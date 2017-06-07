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

package org.systemsbiology.biotapestry.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;
import java.util.TreeMap;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.genome.Node;
 
 
/****************************************************************************
**
** A Grid
*/

public class Grid implements Cloneable {
  
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
  
  public static final int FIXED_RECTANGLE    = 0;
  public static final int VARIABLE_RECTANGLE = 1;  
  public static final int SINGLE_ROW         = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int numRows;
  private int numCols;
  private String[][] gridded;
  private double colSpace;
  private double rowSpace;
  private double[] colSpaceOverride;
  private double[] rowSpaceOverride;
  private String refID_;
  private Point refPt_;
  private Map<String, Integer> topoSort_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */
  
  public Grid(String[] positions, Dimension size, int layoutMode) {
      
    if (layoutMode == FIXED_RECTANGLE) {
      //
      // Try for a 3:4 aspect ratio:
      //
      int numPos = positions.length;
      double blocks = numPos / 12.0;
      double scale = Math.sqrt(blocks);
      numCols = (int)Math.round(scale * 4.0);
      numRows = (int)Math.ceil((double)numPos / (double)numCols);

      int count = 0;
      gridded = new String[numRows][];      
      for (int i = 0; i < numRows; i++) {
        int diff = numPos - count;
        int localCols = (diff >= numCols) ? numCols : diff;
        gridded[i] = new String[localCols];
        for (int j = 0; j < localCols; j++) {
          gridded[i][j] = positions[count++];
        }
      }

      colSpace = size.width / (double)numCols;
      colSpace = Math.round(colSpace / 10.0) * 10.0;     
      rowSpace = (size.height / (double)numRows) + 50;
      rowSpace = Math.round(rowSpace / 10.0) * 10.0;
    } else if (layoutMode == VARIABLE_RECTANGLE) {
      //
      // Try for a 3:4 aspect ratio:
      //
      int numPos = positions.length;
      double blocks = numPos / 12.0; // 12 = 3 * 4, i.e. has desired ratio.
      double scale = Math.sqrt(blocks);
      numCols = (int)Math.round(scale * 4.0);
      numRows = (int)Math.ceil((double)numPos / (double)numCols);

      int count = 0;
      gridded = new String[numRows][];      
      for (int i = 0; i < numRows; i++) {
        int diff = numPos - count;
        int localCols = (diff >= numCols) ? numCols : diff;
        gridded[i] = new String[localCols];
        for (int j = 0; j < localCols; j++) {
          gridded[i][j] = positions[count++];
        }
      }

      colSpace = 250.0;
      rowSpace = 300.0;    
    } else {  // single row
      int numPos = positions.length;        
      numCols = numPos;
      numRows = 1;

      gridded = new String[1][];      
      gridded[0] = new String[numPos];
      for (int j = 0; j < numPos; j++) {
        gridded[0][j] = positions[j];
      }

      colSpace = 250.0;
      rowSpace = 300.0;
    }
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public Grid clone() {
    try {
      Grid retval = (Grid)super.clone();
    
      if (this.colSpaceOverride != null) {
        System.arraycopy(this.colSpaceOverride, 0, retval.colSpaceOverride, 0, this.colSpaceOverride.length);
      }
      if (this.rowSpaceOverride != null) {
        System.arraycopy(this.rowSpaceOverride, 0, retval.rowSpaceOverride, 0, this.rowSpaceOverride.length);
      }
      
      retval.refPt_ = (this.refPt_ == null) ? null : (Point)refPt_.clone();
      retval.topoSort_ = (this.topoSort_ == null) ? null : new HashMap<String, Integer>(this.topoSort_);
      
      if (this.gridded != null) {
        retval.gridded = new String[this.gridded.length][];
        for (int i = 0; i < this.gridded.length; i++) {
          if (this.gridded[i] != null) {
            retval.gridded[i] = new String[this.gridded[i].length];
            System.arraycopy(this.gridded[i], 0, retval.gridded[i], 0, this.gridded[i].length);
          }
        }
      } 
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  
  
  
  /***************************************************************************
  **
  ** Constructor
  */ 

  public Grid(String[][] positions, String refID, Point refPt, Map<String, Integer> topoSort) {
    int maxRows = -1;
    for (int i = 0; i < positions.length; i++) {
      if (positions[i].length > maxRows) {
        maxRows = positions[i].length;
      }
    }
    if (maxRows == -1) {
      numRows = 0;
      numCols = 0;
    } else {
      numRows = maxRows;
      numCols = positions.length;
    }

    //
    // Have to reverse row/col order, and square it out with nulls:
    //

    gridded = new String[numRows][numCols]; 
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numCols; j++) {
        gridded[i][j] = (i > positions[j].length) ? null : positions[j][i];
      }
    }

    colSpace = 250.0;
    rowSpace = 300.0;
    
    refID_ = refID;
    refPt_ = refPt;
    topoSort_ = topoSort;
  }    
       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the original topological sort
  */
  
  public Map<String, Integer> getTopoSort() {
    return (topoSort_);
  } 
  
  /***************************************************************************
  **
  ** Get reference ID (may be null)
  */
  
  public String getReferenceID() {
    return (refID_);
  } 
  
  /***************************************************************************
  **
  ** Get reference point (may be null)
  */
  
  public Point getReferencePoint() {
    return (refPt_);
  }  
  
  /***************************************************************************
  **
  ** Get cell value
  */
  
  public String getCellValue(int row, int col) {
    if ((row >= gridded.length) || (col >= gridded[row].length)) {
      System.err.println("Bad access " + row + " " + col + " for " + refID_);
      throw new IllegalArgumentException();
    }
    return (gridded[row][col]);
  } 

  /***************************************************************************
  **
  ** Set cell value
  */
  
  public void setCellValue(int row, int col, String newVal) {
    gridded[row][col] = newVal;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get column count
  */
  
  public int getNumCols() {
    return (numCols);
  } 
  
  /***************************************************************************
  **
  ** Get row count
  */
  
  public int getNumRows() {
    return (numRows);
  } 
  
  /***************************************************************************
  **
  ** Get column size
  */
  
  public double getColSpace() {
    return (colSpace);
  } 
  
  /***************************************************************************
  **
  ** Get row size
  */
  
  public double getRowSpace() {
    return (rowSpace);
  } 
  
  /***************************************************************************
  **
  ** Set row size
  */
  
  public void setRowSpace(double rowSpace) {
    this.rowSpace = rowSpace;
    return;
  }   
 
  /***************************************************************************
  **
  ** Override column size for given column
  */
  
  public void overrideColSpace(int col, double space) {
    if (colSpaceOverride == null) {
      colSpaceOverride = new double[numCols];
      for (int i = 0; i < numCols; i++) {
        colSpaceOverride[i] = colSpace;
      }
    }
    space = Math.round(space / 10.0) * 10.0;
    colSpaceOverride[col] = space;
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if given column is empty
  */
  
  public boolean columnIsEmpty(int col) {
    for (int i = 0; i < numRows; i++) {
      if (gridded[i][col] != null) {
        return (false);
      }
    }
    return (true);
  }
    
  /***************************************************************************
  **
  ** Override row size for given row
  */
  
  public void overrideRowSpace(int row, double space) {
    if (rowSpaceOverride == null) {
      rowSpaceOverride = new double[numRows];
      for (int i = 0; i < numCols; i++) {
        rowSpaceOverride[i] = rowSpace;
      }
    }
    space = Math.round(space / 10.0) * 10.0;
    rowSpaceOverride[row] = space;
    return;
  }  
 
  /***************************************************************************
  **
  ** Answer if given row is empty
  */
  
  public boolean rowIsEmpty(int row) {
    for (int i = 0; i < numCols; i++) {
      if (gridded[row][i] != null) {
        return (false);
      }
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Drop a row
  */
  
  public void dropRow(int row) {
    String[][] replacement = new String[numRows - 1][];
    int count = 0;
    for (int i = 0; i < numRows; i++) {
      if (i != row) {
        replacement[count++] = gridded[i];
      }
    }
    numRows--;
    gridded = replacement;
    return;
  }    

  /***************************************************************************
  **
  ** Get size estimate  Deprecate!
  */
  
  public Dimension getSizeEstimate() {
    return (new Dimension((int)(numCols * colSpace), (int)(numRows * rowSpace)));
  } 
  
  /***************************************************************************
  **
  ** Is empty
  */
  
  public boolean isEmpty() {
    return ((numCols * numRows) == 0);
  }   
  
  
  /***************************************************************************
  **
  ** Get the ith row and column
  */ 
  
  public RowAndColumn getRowAndColumn(int i) {
    int col = i % numCols;
    int row = i / numCols;
    return (new RowAndColumn(row, col));
  }
  
  /***************************************************************************
  **
  ** Find position in set
  */ 
  
  public int findPosition(String srcID) {
    int myPos = -1;
    for (int i = 0; i < gridded.length; i++) {
      for (int j = 0; j < gridded[i].length; j++) {      
        if ((gridded[i][j] != null) && gridded[i][j].equals(srcID)) {
          myPos = i * gridded[i].length + j;
          break;
        }
      }
    }
    if (myPos == -1) {
      throw new IllegalStateException();
    }
    return (myPos);
  }

  /***************************************************************************
  **
  ** Answers if present
  */ 
  
  public boolean contains(String srcID) {
    for (int i = 0; i < gridded.length; i++) {
      for (int j = 0; j < gridded[i].length; j++) {      
        if ((gridded[i][j] != null) && gridded[i][j].equals(srcID)) {
          return (true);
        }
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Find position in set
  */ 
  
  public RowAndColumn findPositionRandC(String srcID) {
    for (int i = 0; i < gridded.length; i++) {
      for (int j = 0; j < gridded[i].length; j++) {      
        if ((gridded[i][j] != null) && gridded[i][j].equals(srcID)) {
          return (new RowAndColumn(i, j));
        }
      }
    }
    System.err.println("Failing to find " + srcID + " in the grid for " + refID_);
    return (new RowAndColumn(0, 0));
  //  throw new IllegalStateException();
  }  
 
  /***************************************************************************
  **
  ** Get the point for the specified slot.  Returns null if nothing in
  ** in the grid
  */
  
  public Point2D getGridLocation(int row, int col, Point2D corner) {
    if (getCellValue(row, col) == null) {
      return (null);
    }
    Point2D loc = new Point2D.Double();
    double x = corner.getX();
    double y = corner.getY();
    if ((colSpaceOverride == null) || (colSpaceOverride.length == 0)) {
      x += (getColSpace() * (col + 0.5));
    } else {
      int firstNonZero = Integer.MIN_VALUE;
      for (int i = 0; i < col; i++) {
        x += colSpaceOverride[i];
        if ((colSpaceOverride[i] != 0.0) && (firstNonZero == Integer.MIN_VALUE)) {
          firstNonZero = i;
        }
      }
      if (firstNonZero == Integer.MIN_VALUE) {
        firstNonZero = col;
      }
      x += (colSpaceOverride[firstNonZero] / 2.0);      
    }
    
    if ((rowSpaceOverride == null) || (rowSpaceOverride.length == 0)) {
      y += (getRowSpace() * (row + 0.5));
    } else {
      int firstNonZero = Integer.MIN_VALUE;
      for (int i = 0; i < row; i++) {
        y += rowSpaceOverride[i];
        if ((rowSpaceOverride[i] != 0.0) && (firstNonZero == Integer.MIN_VALUE)) {
          firstNonZero = i;
        }
      }
      if (firstNonZero == Integer.MIN_VALUE) {
        firstNonZero = row;
      }
      y += (rowSpaceOverride[firstNonZero] / 2.0);       
    }
    
    UiUtil.forceToGrid(x, y, loc, 10.0);
    return (loc);         
  }
  
  /***************************************************************************
  **
  ** Get the point for the specified id.  Returns null if nothing in
  ** in the grid
  */
  
  public Point2D getGridLocation(String srcID, Point2D corner) {
    for (int col = 0; col < numCols; col++) {
      for (int row = 0; row < numRows; row++) {
        String cellVal = getCellValue(row, col);
        if ((cellVal != null) && cellVal.equals(srcID)) {
          return (getGridLocation(row, col, corner));
        }
      }
    }
    return (null);
  }


  /***************************************************************************
  **
  ** Get the width
  */ 
  
  public double getWidth() {
    if (colSpaceOverride == null) {
      return (colSpace * numCols);
    }
    double w = 0.0;
    for (int i = 0; i < numCols; i++) {
      w += colSpaceOverride[i];
    }
    return (w);
  }
  
  /***************************************************************************
  **
  ** Get the height
  */ 
  
  public double getHeight() {
    if (rowSpaceOverride == null) {
      return (rowSpace * numRows);
    }
    double h = 0.0;
    for (int i = 0; i < numRows; i++) {
      h += rowSpaceOverride[i];
    }
    return (h);
  }  

  /***************************************************************************
  **
  ** Build a pattern
  */ 
  
  public Pattern buildPattern() {

    Pattern pat = new Pattern((int)Math.round(getWidth() / 10.0), 
                              (int)Math.round(getHeight() / 10.0));

    int currY = 0;
    for (int i = 0; i < numRows; i++) {
      double dHeight = (rowSpaceOverride == null) ? rowSpace : rowSpaceOverride[i];
      int height = (int)Math.round(dHeight / 10.0);
      int currX = 0;
      for (int j = 0; j < numCols; j++) {
        String gridVal = gridded[i][j];
        double dWidth = ((colSpaceOverride == null) ? colSpace : colSpaceOverride[j]);
        int width = (int)Math.round(dWidth / 10.0);
        if (gridVal != null) {
          for (int k = 0; k < width; k++) {
            for (int m = 0; m < height; m++) {
              pat.fill(currX + k, currY + m, gridVal);
            }
          }
        }
        currX += width;
      }
      currY += height;
    }
    return (pat);
  }
  
  /***************************************************************************
  **
  ** Build the grid bounds
  */
  
  @SuppressWarnings("unused")
  public GridBounds buildBounds(Genome genome, Layout layout, String srcID, int srcPos, Set<String> linkSet) {

    RowAndColumn rac = getRowAndColumn(srcPos);
    GridBounds retval = new GridBounds(rac);
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if ((linkSet != null) && !linkSet.contains(link.getID())) {
        continue;
      } 
      if (link.getSource().equals(srcID)) {
        String trgID = link.getTarget();
        for (int i = 0; i < numRows; i++) {
          int localCols = gridded[i].length;            
          for (int j = 0; j < localCols; j++) {
            if (trgID.equals(gridded[i][j])) {
              retval.addTarget(i, j, link.getID(), link.getLandingPad());
            }
          }
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Split up the maxiumum distance links so others may attach
  */     

  public String splitLinksUp(RowData targRD, int step, int srcRow, Map<Integer, Point2D> splitPoints,
                             StaticDataAccessContext rcx, int colNum, SlotTracker tracker,
                             GridBounds bounds) {

    //
    // Get a link on the minimum row:
    //

    String targLink = targRD.getLowestLink();
    String oppLink = targRD.getHighestLink();
    SortedMap<Integer, SortedMap<Integer, String>> targs;
    SortedSet<Integer> keys;
    SortedMap<Integer, SortedMap<Integer, String>> oppTargs;
    SortedSet<Integer> oppKeys;
    boolean lowerFirst;
    boolean oppLowerFirst;

    // Can't both be null:
    if (targLink == null) { // swap
      targLink = oppLink;
      oppLink = null;
      targs = targRD.higherTargets;
      keys = new TreeSet<Integer>(targs.keySet());
      oppTargs = null;
      oppKeys = null;
      lowerFirst = true;
      oppLowerFirst = false;
    } else if (oppLink == null) {  
      targs = targRD.lowerTargets;
      keys = new TreeSet<Integer>(Collections.reverseOrder());
      keys.addAll(targs.keySet());
      oppTargs = null;
      oppKeys = null;
      lowerFirst = false;
      oppLowerFirst = false;        
    } else {
      targs = targRD.lowerTargets;
      keys = new TreeSet<Integer>(Collections.reverseOrder());
      keys.addAll(targs.keySet());
      oppTargs = targRD.higherTargets;
      oppKeys = new TreeSet<Integer>(oppTargs.keySet());
      lowerFirst = false;
      oppLowerFirst = true;        
    }

    //
    // For each row we traverse, split the link into segments:
    //

    doRiserSplits(srcRow, targRD.rowNum, step, targLink, splitPoints, rowSpace, colNum, tracker, rcx, bounds);

    //
    // Split the runner out to the maximum into segments:
    //

    doRunnerSplits(targLink, rcx, targs, keys, targRD.rowNum, srcRow, rowSpace, tracker, lowerFirst);


    if (oppLink != null) {
      //
      // Relocate opposite segment to riser point:
      //
      relocToRiser(splitPoints, targRD.rowNum, rcx.getCurrentLayout(), oppLink);    
      //
      // Split the runner out to the opposite maximum into segments:
      //
      doRunnerSplits(oppLink, rcx, oppTargs, oppKeys, targRD.rowNum, srcRow, rowSpace, tracker, oppLowerFirst);
    }

    return (targLink);
  }

  /***************************************************************************
  **
  ** Do all split operations on a riser
  */    

  public void doRiserSplits(int startRow, int endRow, int step,
                            String targLink, Map<Integer, Point2D> splitPoints, 
                            double rowSpace, int colNum, SlotTracker tracker,
                            StaticDataAccessContext rcx, GridBounds bounds) {

    Linkage maxLink = rcx.getCurrentGenome().getLinkage(targLink);
    LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(targLink);
    BusProperties bp = (BusProperties)lp;
    NodeProperties srcProp = rcx.getCurrentLayout().getNodeProperties(maxLink.getSource());
    Point2D srcLoc = srcProp.getLocation();
    // Always install a riser above the source row if there is a target in it.
    int loopStart = (step == 1) ? startRow + 1 : startRow;
    int loopEnd = endRow + step;
    for (int i = loopStart; i != loopEnd; i += step) {
      if (bounds.getRowData(i) != null) {
        doRiserSplit(splitPoints, targLink, maxLink, i, startRow, rcx, rowSpace, bp, srcLoc, colNum, tracker);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Do a split operation on a riser
  */    

  public void doRiserSplit(Map<Integer, Point2D> splitPoints, String targLinkID, 
                           Linkage targLink, int row, int startRow,
                           StaticDataAccessContext rcx,
                           double rowSpace, 
                           BusProperties bp, Point2D srcLoc, 
                           int colNum, SlotTracker tracker) {      

    LinkSegmentID segID = (bp.isDirect()) ? LinkSegmentID.buildIDForDirect(targLinkID) 
                                          : LinkSegmentID.buildIDForDrop(targLinkID);      
  
    //
    // Figure out vertical position:

    double riserY = calcRiserY(targLink, row, startRow, rowSpace, tracker, rcx);

    //
    // Figure out horizontal position:
    //

    int lpad = targLink.getLaunchPad();
    Vector2D colDelt = new Vector2D(10.0, 0.0);
    String src = targLink.getSource();
    int slotNum = tracker.getColSlotForSource(colNum, src);
    colDelt.scale(slotNum + 2.0);            
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(src);
    Node node = rcx.getCurrentGenome().getNode(src);      
    Vector2D lpo = np.getRenderer().getLaunchPadOffset(lpad, node, rcx);      
    Point2D xCalc = lpo.add(colDelt.add(srcLoc));

    Point2D split = new Point2D.Double(xCalc.getX(), riserY);
    UiUtil.forceToGrid(split.getX(), split.getY(), split, 10.0); 
    rcx.getCurrentLayout().splitBusLink(segID, split, bp, null, rcx);
    splitPoints.put(new Integer(row), split);
    return;
  }      

  /***************************************************************************
  **
  ** Calc the Y coordinate of a riser
  */    

  public double calcRiserY(Linkage targLink, int row, int startRow, double rowSpace, 
                           SlotTracker tracker, StaticDataAccessContext rcx) {      

    Vector2D rowDelt = new Vector2D(0.0, rowSpace);
    rowDelt.scale(row - startRow);
    String src = targLink.getSource();
    Vector2D rowOffDelt = new Vector2D(0.0, -10.0); 
    int slotNum = tracker.getRowSlotForSource(row, src);      
    rowOffDelt.scale(slotNum + 2.0);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(src);
    Point2D srcLoc = np.getLocation();
    Node node = rcx.getCurrentGenome().getNode(src);
    int lPad = targLink.getLaunchPad();
    Vector2D lpo = np.getRenderer().getLaunchPadOffset(lPad, node, rcx);      
    Point2D split = lpo.add(rowOffDelt.add(rowDelt.add(srcLoc)));
    Point2D forced = new Point2D.Double();
    UiUtil.forceToGrid(split.getX(), split.getY(), forced, 10.0);
    return (forced.getY());
  }     

 /***************************************************************************
  **
  ** Do all split operations on a runner
  */    

  public void doRunnerSplits(String targLinkID, StaticDataAccessContext rcx,
                             SortedMap<Integer, SortedMap<Integer, String>> targs, 
                             SortedSet<Integer> keys,
                             int rowNum, int startRow, double rowSpace,
                             SlotTracker tracker, boolean lowerFirst) {

    LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(targLinkID);
    BusProperties bp = (BusProperties)lp;                          
    Iterator<Integer> kit = keys.iterator();
    while (kit.hasNext()) {
      SortedMap<Integer, String> linkIDs = targs.get(kit.next());
      TreeSet<Integer> padkeys = !lowerFirst ? new TreeSet<Integer>(Collections.reverseOrder())
                                             : new TreeSet<Integer>();
      padkeys.addAll(linkIDs.keySet());
      Iterator<Integer> lidit = padkeys.iterator();        
      while (lidit.hasNext()) {
        Integer padNum = lidit.next();
        String linkID = linkIDs.get(padNum);          
        doRunnerSplit(targLinkID, linkID, bp, rcx, rowNum, startRow, tracker, rowSpace);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Do a split operation on a runner
  */    

  public void doRunnerSplit(String targLinkID, String linkID,
                            BusProperties bp, StaticDataAccessContext rcx, int rowNum, 
                            int startRow, SlotTracker tracker, double rowSpace) {

    LinkSegmentID segID = LinkSegmentID.buildIDForDrop(targLinkID);
    Genome genome = rcx.getCurrentGenome();
    Linkage targLink = genome.getLinkage(targLinkID);      

    //
    // Figure out vertical position:
    //

    double riserY = calcRiserY(targLink, rowNum, startRow, rowSpace, tracker, rcx);      

    //
    // Figure out horizontal position:
    //

    Linkage link = genome.getLinkage(linkID);
    Node node = genome.getNode(link.getTarget());
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(link.getTarget());
    Point2D genePoint = np.getLocation();
    Vector2D lpo = np.getRenderer().getLandingPadOffset(link.getLandingPad(), node, link.getSign(), rcx);
    Vector2D lpox = new Vector2D(lpo.getX(), 0.0);
    Vector2D offset = new Vector2D(0.0, -10.0);
    int slotNum = tracker.getRowSlotForSource(rowNum, link.getSource());
    offset.scale(slotNum + 2.0);
    Point2D xCalc = offset.add(lpox.add(genePoint));
    Point2D split = new Point2D.Double(xCalc.getX(), riserY);
    UiUtil.forceToGrid(split.getX(), split.getY(), split, 10.0);      
    rcx.getCurrentLayout().splitBusLink(segID, split, bp, null, rcx);

    //
    // Do the relocation:
    //

    Iterator<LinkSegment> sit = bp.getSegments();
    LinkSegmentID dropID = LinkSegmentID.buildIDForDrop(linkID);
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      Point2D end = ls.getEnd();
      if (end != null) {
        if (end.equals(split)) {      
           LinkSegmentID relocSeg = LinkSegmentID.buildIDForSegment(ls.getID());
           relocSeg.tagIDWithEndpoint(LinkSegmentID.END);
           rcx.getCurrentLayout().relocateSegmentOnTree(bp, relocSeg, dropID, null, null);
           break;
        }
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Relocate to a riser point
  */    

  public void relocToRiser(Map<Integer, Point2D> splitPoints, int rowNum, 
                           Layout lo, String linkID) {

    BusProperties bp = lo.getLinkProperties(linkID);
    LinkSegmentID dropID = LinkSegmentID.buildIDForDrop(linkID);
    Point2D reloc = splitPoints.get(new Integer(rowNum));      
    Iterator<LinkSegment> sit = bp.getSegments();      
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      Point2D end = ls.getEnd();
      if (end != null) {
        if (end.equals(reloc)) {      
           LinkSegmentID relocSeg = LinkSegmentID.buildIDForSegment(ls.getID());
           relocSeg.tagIDWithEndpoint(LinkSegmentID.END);
           lo.relocateSegmentOnTree(bp, relocSeg, dropID, null, null);
           break;
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Debug string
  */    

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("numRows = " + numRows);
    buf.append(" numCols = " + numCols);
    buf.append("\n");
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numCols; j++) {
        buf.append("grid: " + i + " " + j + " = " + gridded[i][j]);
        buf.append("\n");
      }
    }
    buf.append("colSpace = " + colSpace);
    buf.append(" rowSpace = " + rowSpace);
    buf.append(" refID = " + refID_);
    buf.append(" refPt = " + refPt_); 
    return (buf.toString());
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
  
  public static class RowAndColumn {
    public int row;
    public int col;
    
    public RowAndColumn(int row, int col) {
      this.row = row;
      this.col = col;   
    }
    
    public String toString() {
      return ("RandC row = " + row + " col = " + col);
    }
  }  

  public static class GridBounds {
    
    TreeMap<Integer, RowData> rows;
    int srcCol;
    int srcRow;
    
    GridBounds(RowAndColumn rac) {
      srcCol = rac.col;
      srcRow = rac.row;
      rows = new TreeMap<Integer, RowData>();
    }
    
    RowData getRowData(int row) {
      return rows.get(new Integer(row));
    }
    
    void addRowData(RowData data) {
      rows.put(new Integer(data.rowNum), data);
      return;
    }

    boolean noTargets() {
      return (rows.isEmpty());
    }
    
    RowData getMinRowData() {
      return (rows.get(rows.firstKey()));
    }
    
    RowData getMaxRowData() {
      return (rows.get(rows.lastKey()));
    }    
            
    void addTarget(int row, int col, String linkID, int padNum) {
      RowData rd = rows.get(new Integer(row));
      if (rd == null) {
        rd = new RowData(row);
        addRowData(rd);
      }
      if (col < rd.minColumn) {
        rd.minColumn = col;
      }
      if (col > rd.maxColumn) {
        rd.maxColumn = col;
      }
      if (col <= srcCol) {
        Integer colObj = new Integer(col);
        SortedMap<Integer, String> targMap = rd.lowerTargets.get(colObj);
        if (targMap == null) {
          targMap = new TreeMap<Integer, String>();
          rd.lowerTargets.put(new Integer(col), targMap);
        }
        targMap.put(new Integer(padNum), linkID);
      }
      if (col > srcCol) {
        Integer colObj = new Integer(col);
        SortedMap<Integer, String> targMap = rd.higherTargets.get(colObj);
        if (targMap == null) {
          targMap = new TreeMap<Integer, String>();
          rd.higherTargets.put(new Integer(col), targMap);
        }
        targMap.put(new Integer(padNum), linkID);
      }
      return;
    }    
    
  }  

  public static class SlotTracker {
    
    HashMap<String, SlotData>[] rowData;
    HashMap<String, SlotData>[] colData;
    
    SlotTracker(Grid grid) {
      rowData = new HashMap[grid.numRows];
      colData = new HashMap[grid.numCols];
    }
    
    int getRowSlotForSource(int row, String source) {
      return (getTrackSlotForSource(rowData, row, source));
    }
    
    int getColSlotForSource(int col, String source) {
      return (getTrackSlotForSource(colData, col, source));
    }    
    
    private int getTrackSlotForSource(HashMap<String, SlotData>[] maps, int track, String source) {
      HashMap<String, SlotData> map = maps[track];
      if (map == null) {
        map = new HashMap<String, SlotData>();
        maps[track] = map;
      }
      SlotData sd = map.get(source);
      if (sd == null) {
        int nextSlot = -1;
        Iterator<SlotData> sdit = map.values().iterator();
        while (sdit.hasNext()) {
          SlotData csd = sdit.next();
          if (csd.slot > nextSlot) {
            nextSlot = csd.slot;
          }
        }
        sd = new SlotData(track, source, nextSlot + 1);
        map.put(source, sd);
      }
      return (sd.slot);
    }      
  }  
  
  public static class RowData {
    
    int rowNum;
    int minColumn;
    int maxColumn;
    TreeMap<Integer, SortedMap<Integer, String>> lowerTargets;
    TreeMap<Integer, SortedMap<Integer, String>> higherTargets;

    RowData(int rowNum) {
      this.rowNum = rowNum;
      lowerTargets = new TreeMap<Integer, SortedMap<Integer, String>>();
      higherTargets = new TreeMap<Integer, SortedMap<Integer, String>>();
      minColumn = Integer.MAX_VALUE;
      maxColumn = -1;
    }
    
    String getHighestLink() {
      if (higherTargets.size() == 0) {
        return (null);
      }
      SortedMap<Integer, String> targs = higherTargets.get(higherTargets.lastKey()); 
      return (targs.get(targs.lastKey()));
    }
    
    String getLowestLink() {
      if (lowerTargets.size() == 0) {
        return (null);
      }
      SortedMap<Integer, String> targs = lowerTargets.get(lowerTargets.firstKey()); 
      return (targs.get(targs.firstKey()));
    }    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  private static class SlotData {    
    int track;
    String src;
    int slot;

    SlotData(int track, String src, int slot) {
      this.track = track;
      this.src = src;
      this.slot = slot;      
    }

  }   
}
