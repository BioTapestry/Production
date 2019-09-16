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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Grid;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
**
** Gridding support
*/

public class TrackedGrid implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  //
  // There is an element of hackery in using this, but it does make the
  // layout look better when everything has some breathing room...
  
  
  private static final double FAN_GRID_PAD_Y_ = 40.0;
  private static final double FAN_GRID_PAD_X_ = 40.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Grid grid_;
  private double exactVerticalOffset_;
  private Dimension dims_;
  private HashMap<Integer, Double> widths_;
  private HashMap<Integer, Double> heights_;
  private HashMap<Integer, SortedMap<Integer, String>> colTracks_;
  private HashMap<Integer, SortedMap<Integer, String>> rowTracks_;
  private HashMap<String, Rectangle2D> cellBounds_;
  private Integer reverseCol_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

 /***************************************************************************
  ** 
  ** Constructor
  */
        
  public TrackedGrid(Grid grid, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                     DataAccessContext irx, String coreID, boolean textToo) {
    this(grid, nps, irx, coreID, textToo, false);
  }
  
   /***************************************************************************
  ** 
  ** Constructor
  */
        
  public TrackedGrid(Grid grid, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                     DataAccessContext irx, String coreID, boolean textToo, boolean revColZero) {
    grid_ = grid;
    widths_ = new HashMap<Integer, Double>();
    heights_= new HashMap<Integer, Double>();
    cellBounds_ = new HashMap<String, Rectangle2D>();
    dims_ = calcFanNodalDims(nps, irx, coreID, textToo, cellBounds_);
    exactVerticalOffset_ = Double.NEGATIVE_INFINITY;
    colTracks_ = new HashMap<Integer, SortedMap<Integer, String>>();
    rowTracks_ = new HashMap<Integer, SortedMap<Integer, String>>();
    reverseCol_ = (revColZero) ? Integer.valueOf(0) : null;
  }
  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public TrackedGrid clone() {
    try {
      TrackedGrid retval = (TrackedGrid)super.clone();
      
      retval.grid_ = (this.grid_ == null) ? null : (Grid)grid_.clone();
      
      if (this.colTracks_ != null) {
        retval.colTracks_ = new HashMap<Integer, SortedMap<Integer, String>>();
        for (Integer key : this.colTracks_.keySet()) {
          SortedMap<Integer, String> nextMap = this.colTracks_.get(key);
          SortedMap<Integer, String> retMap = new TreeMap<Integer, String>();
          retMap.putAll(nextMap);
          retval.colTracks_.put(key, retMap);
        }
      } 
      
      if (this.rowTracks_ != null) {
        retval.rowTracks_ = new HashMap<Integer, SortedMap<Integer, String>>();
        for (Integer key : this.rowTracks_.keySet()) {
          SortedMap<Integer, String> nextMap = this.rowTracks_.get(key);
          retval.rowTracks_.put(key, new TreeMap<Integer, String>(nextMap));
        }
      } 

      retval.dims_ = (this.dims_ == null) ? null : (Dimension)dims_.clone();
      retval.widths_ = (this.widths_ == null) ? null : new HashMap<Integer, Double>(this.widths_);
      retval.heights_ = (this.heights_ == null) ? null : new HashMap<Integer, Double>(this.heights_);
      
      if (this.cellBounds_ != null) {
        retval.cellBounds_ = new HashMap<String, Rectangle2D>();
        for (String key : this.cellBounds_.keySet()) {
          retval.cellBounds_.put(key, (Rectangle2D)this.cellBounds_.get(key).clone());
        }
      } 
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  ** 
  ** Get the contents, null if empty
  */
  
  public String getContents(int row, int col) {  
    return (grid_.getCellValue(row, col));
  }

  /***************************************************************************
  ** 
  ** Get the rightmost node for a given row:
  */
  
  public String rightmostForRow(int rowNum, String ignoreID) {  
    String retval = null;
    int colNum = grid_.getNumCols();
    for (int i = 0; i < colNum; i++) {
      String nodeID = grid_.getCellValue(rowNum, i);
      if ((nodeID == null) || nodeID.equals(ignoreID)) {
        continue;
      }
      retval = nodeID;
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Answer if we can route directly into the left side.  This is used for
  ** inbound links, where the source is not in the grid.
  */
  
  public boolean firstOnLeft(String trgID, String ignoreID) {
    // Must not be any occupants in intervening columns along
    // the target row.
    Grid.RowAndColumn trgRC = grid_.findPositionRandC(trgID);
    int colNum = grid_.getNumCols();
    for (int i = 0; i < colNum; i++) {
      String nodeID = grid_.getCellValue(trgRC.row, i);
      if (nodeID == null) {
       continue;
      }
      if (nodeID.equals(trgID)) {
        return (true);
      }
      if ((ignoreID != null) && nodeID.equals(ignoreID)) {
        continue;
      }
      return (false);
    }
    throw new IllegalArgumentException();
  }    
   
  /***************************************************************************
  ** 
  ** Answer if we can route directly into the left side
  */
  
  public boolean canEnterOnLeft(String srcID, String trgID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    // Must not be any occupants in intervening columns along
    // the target row.
    int startRow;
    int startCol;
    if (grid_.contains(srcID)) {
      Grid.RowAndColumn srcRC = grid_.findPositionRandC(srcID);
      startCol = srcRC.col + 1;
      startRow = srcRC.row;
    } else {
      startCol = 0;
      startRow = 0;  // maybe not?  Do we care?
    }
    
    if (!grid_.contains(trgID)) {
      return (false);
    }
    
    boolean maybeOK = false;
    Grid.RowAndColumn trgRC = grid_.findPositionRandC(trgID);
    int endRow = trgRC.row;
    int colNum = grid_.getNumCols();
    for (int i = startCol; i < colNum; i++) {
      String nodeID = grid_.getCellValue(trgRC.row, i);
      if ((nodeID != null) && nodeID.equals(trgID)) {
        if ((endRow == startRow) || (i == 0)) {
          return (true);
        } else {
          maybeOK = true;
          break;
        }
      }
      if (nodeID != null) {
        return (false);
      }
    }
    
    //
    // Additional constraint.  We cannot enter on left if the target is not in the
    // same row as the source, and the column preceeding the target holds a node 
    // with an outbound link.  In those cases, the two direct links can overlap,
    // since we are not trying to assign column tracks to avoid this problem.
    //
    
    if (maybeOK) {
      String prevID = grid_.getCellValue(trgRC.row, trgRC.col - 1);
      return ((prevID == null) || (nps.outboundLinkCount(prevID) == 0));
    }
    
    return (false);  // backwards feedback
  }  
 
  /***************************************************************************
  ** 
  ** Convert parameterized link tracks to absolute coordinates
  */

  public void convertToPoints(SpecialtyLayoutLinkData sin, Point2D upperLeft,
                              SpecialtyLayoutEngine.NodePlaceSupport nps, 
                              DataAccessContext irx, String coreID, double fixVal) {
    Iterator<String> vit = sin.getLinkList().iterator();
    while (vit.hasNext()) {
      String linkID = vit.next();
      SpecialtyLayoutLinkData.NoZeroList pointList = sin.getPositionList(linkID);      
      int numpts = pointList.size();
      for (int i = 0; i < numpts; i++) {
        TrackPosRC rct = (TrackPosRC)pointList.get(i);
        convertRCTrack(rct, upperLeft, nps, irx, coreID, fixVal);
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Used for core junpers, where two grids convert one list.  Find the last converted point.
  */

  public Point2D getLastConvertedPoint(SpecialtyLayoutLinkData sin) {
    Iterator<String> vit = sin.getLinkList().iterator();
    Point2D retval = null;
    while (vit.hasNext()) {
      String linkID = vit.next();
      SpecialtyLayoutLinkData.NoZeroList pointList = sin.getPositionList(linkID);      
      int numpts = pointList.size();
      for (int i = 0; i < numpts; i++) {
        TrackPosRC tprc = (TrackPosRC)pointList.get(i);
        if (tprc.needsConversion()) {
          return (retval);
        } else {
          retval = tprc.getPoint();
        }
      }
    }
    return (null);
  }  
  
  /***************************************************************************
  ** 
  ** Convert parameterized link tracks to absolute coordinates
  */

  public void convertPositionListToPoints(List<SpecialtyLayoutLinkData.TrackPos> pointList, Point2D upperLeft, 
                                          SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String coreID) {
    int numpts = pointList.size();
    for (int i = 0; i < numpts; i++) {
      SpecialtyLayoutLinkData.TrackPos rct = pointList.get(i);
      if (rct.needsConversion()) {
        convertRCTrack((TrackPosRC)rct, upperLeft, nps, irx, coreID, 0.0);
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Convert parameterized point to absolute coordinates
  */

  public Point2D convertPositionToPoint(TrackPosRC tprc, Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                        DataAccessContext irx, String coreID) {
    if (tprc.needsConversion()) {
      tprc = tprc.clone();
      convertRCTrack(tprc, upperLeft, nps, irx, coreID, 0.0);
    }
    return (tprc.getPoint());
  }  

  /***************************************************************************
  ** 
  ** Convert an RCTrack to a point
  */

  public void convertRCTrack(TrackPosRC tprc, Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                             DataAccessContext irx, String coreID, double fixVal) {
    if (tprc.needsConversion()) {
      RCTrack rct = tprc.rcTrack;
      if (!rct.isMyGrid(this)) {
        return;
      }
      tprc.fixLocation(rct.convert(upperLeft, nps, irx, coreID, fixVal));
    }
    return;
  }   
  
  /***************************************************************************
  ** 
  ** Get position
  */

  public Grid.RowAndColumn findPositionRandC(String nodeID) {
    return (grid_.findPositionRandC(nodeID)); 
  }
  
  /***************************************************************************
  ** 
  ** Answers if present
  */

  public boolean contains(String nodeID) {
    return (grid_.contains(nodeID)); 
  }  

  /***************************************************************************
  ** 
  ** inherit, supply new a pure track
  */

  public RCTrack buildRCTrack(RCTrack rct, TrackSpec spec, int whichSpec) {
    return (new RCTrack(rct, spec, whichSpec));
  } 
  
  /***************************************************************************
  ** 
  ** inherit pieces of two tracks
  */

  public RCTrack buildRCTrack(RCTrack rowRct, RCTrack colRct) {
    return (new RCTrack(rowRct, colRct));
  }  

  /***************************************************************************
  ** 
  ** Build a pure track
  */

  public RCTrack buildRCTrack(BTState appState, TrackSpec rowSpec, TrackSpec colSpec) {
    return (new RCTrack(appState, rowSpec, colSpec));
  }   

  /***************************************************************************
  ** 
  ** Build a mixed param/abs track
  */

  public RCTrack buildRCTrack(RCTrack rct, int whichFixed, double fixValue) {
    return (new RCTrack(rct, whichFixed, fixValue));
  } 
  
  /***************************************************************************
  ** 
  ** Build a mixed param/undef track
  */

  public RCTrack buildRCTrack(RCTrack rct, int whichFloats) {
    return (new RCTrack(rct, whichFloats));
  }   
 
  /***************************************************************************
  ** 
  ** Build a hybrid
  */

  public RCTrack buildRCTrackForRowMidline(BTState appState, TrackSpec colSpec,
                                           int padNum, String nodeID, String linkID, 
                                           boolean isLanding, int linkSign, int rowNum) {
    return (new RCTrack(appState, colSpec, RCTrack.ROW_MIDLINE, padNum, nodeID, linkID, isLanding, linkSign, rowNum));
  } 
  
  /***************************************************************************
  ** 
  ** Build a hybrid
  */

  public RCTrack buildRCTrackForColMidline(BTState appState, TrackSpec rowSpec, int padNum, 
                                           String nodeID, String linkID, boolean isLanding, int linkSign, int colNum) {
    return (new RCTrack(appState, rowSpec, RCTrack.COLUMN_MIDLINE, padNum, nodeID, linkID, isLanding, linkSign, colNum));
  }  
  
  /***************************************************************************
  ** 
  ** Build a hybrid
  */

  public RCTrack buildRCTrackWithPadChange(RCTrack current, int padNum, String linkID) {
    return (current.copyWithNewColPadNum(padNum, linkID));
  }   

  /***************************************************************************
  ** 
  ** Build a pure midline track
  */

  public RCTrack buildRCTrackForDualMidline(BTState appState, int colPadNum, String colNodeID, boolean colIsLanding, 
                                            int rowPadNum, String rowNodeID, boolean rowIsLanding,             
                                            int linkSign, String linkID, int colNum, int rowNum) {
    return (new RCTrack(appState, colPadNum, colNodeID, colIsLanding, rowPadNum, rowNodeID, rowIsLanding, linkSign, linkID, colNum, rowNum));
  }   

  /***************************************************************************
  ** 
  ** Build a hybrid
  */

  public RCTrack inheritRCTrackNewRowMidline(RCTrack rct,
                                             int padNum, String nodeID, String linkID, 
                                             boolean isLanding, int linkSign, int rowNum) {
    return (new RCTrack(rct, RCTrack.ROW_MIDLINE, padNum, nodeID, linkID, isLanding, linkSign, rowNum));
  } 
  
  /***************************************************************************
  ** 
  ** Build a hybrid
  */

  public RCTrack inheritRCTrackNewColMidline(RCTrack rct, int padNum, 
                                             String nodeID, String linkID, boolean isLanding, int linkSign, int colNum) {
    return (new RCTrack(rct, RCTrack.COLUMN_MIDLINE, padNum, nodeID, linkID, isLanding, linkSign, colNum));
  }    

  /***************************************************************************
  ** 
  ** Get the number of columns
  */

  public int getNumCols() {
    return (grid_.getNumCols());
  }  
  
  /***************************************************************************
  ** 
  ** Get the number of rows
  */

  public int getNumRows() {
    return (grid_.getNumRows());
  }    

  /***************************************************************************
  ** 
  ** Get a column track
  */

  public TrackSpec reserveColumnTrack(int col, String srcID) {
    boolean invert = ((this.reverseCol_ != null) && (this.reverseCol_.intValue() == col));
    Integer colKey = new Integer(col);
    SortedMap<Integer, String> trackMap = colTracks_.get(colKey);
    boolean isNew = false;
    if (trackMap == null) {
      trackMap = new TreeMap<Integer, String>();
      colTracks_.put(colKey, trackMap);
      isNew = true;
    }
    // If columns are assigned in reverse minor order for fan outs, 249 can be addressed   
    if (isNew) {
      trackMap.put(Integer.valueOf(0), srcID);
      return (new TrackSpec(col, 0));
    }
    
    //
    // Return preexisting reservation:
    //
    Iterator<Integer> tmkit = trackMap.keySet().iterator();
    while (tmkit.hasNext()) {
      Integer key = tmkit.next();
      String chkSrcID = trackMap.get(key);
      if (chkSrcID.equals(srcID)) {
        return (new TrackSpec(col, key.intValue()));
      }
    }
 
    //
    // Since we can skip, we now have to backfill reservations:
    //
   
    int inc = (invert) ? -1 : 1;
    int useKey = (invert) ? trackMap.firstKey() - 1 : trackMap.lastKey() + 1;
    Set<Integer> trackKeys = trackMap.keySet();
    Integer nextKey = null;
    for (int i = 0; i != useKey; i += inc) {
      Integer testInt = Integer.valueOf(i);
      if (!trackKeys.contains(testInt)) {
        nextKey = testInt;
        break;
      }
    }
    if (nextKey == null) {
      nextKey = Integer.valueOf(useKey);
    }

    trackMap.put(nextKey, srcID);
    return (new TrackSpec(col, nextKey.intValue()));
  }
  
  /***************************************************************************
  ** 
  ** Get a column track
  */

  public TrackSpec reserveSkippedColumnTrack(int col, String srcID, int srcSkip) {
    boolean invert = ((this.reverseCol_ != null) && (this.reverseCol_.intValue() == col));
    Integer colKey = new Integer(col);
    SortedMap<Integer, String> trackMap = colTracks_.get(colKey);
    boolean isNew = false;
    if (trackMap == null) {
      trackMap = new TreeMap<Integer, String>();
      colTracks_.put(colKey, trackMap);
      isNew = true;
    }
    
    //
    // Doing assignment reversal for fan-outs to handle issue #249. Lingering question is
    // whether the use of the original isNew flag is the correct thing to do here. Seems to
    // be right, but...?
    //
    
    if (isNew || (srcSkip != 0)) {
      int useSkip = (invert) ? -srcSkip : srcSkip;
      trackMap.put(Integer.valueOf(useSkip), srcID);
      return (new TrackSpec(col, useSkip));
    }
    
    //
    // Return preexisting reservation:
    //
    Iterator<Integer> tmkit = trackMap.keySet().iterator();
    while (tmkit.hasNext()) {
      Integer key = tmkit.next();
      String chkSrcID = trackMap.get(key);
      if (chkSrcID.equals(srcID)) {
        return (new TrackSpec(col, key.intValue()));
      }
    }    
 
    //
    // Since we can skip, we now have to backfill reservations:
    //
   
    int inc = (invert) ? -1 : 1;
    int useKey = (invert) ? trackMap.firstKey() - 1 : trackMap.lastKey() + 1;
    Set<Integer> trackKeys = trackMap.keySet();
    Integer nextKey = null;
    for (int i = 0; i != useKey; i += inc) {
      Integer testInt = Integer.valueOf(i);
      if (!trackKeys.contains(testInt)) {
        nextKey = testInt;
        break;
      }
    }
    if (nextKey == null) {
      nextKey = Integer.valueOf(useKey);
    }
    trackMap.put(nextKey, srcID);
    return (new TrackSpec(col, nextKey.intValue()));
  }  

  /***************************************************************************
  ** 
  ** A function
  */
  
  public TrackSpec reserveRowTrack(int row, String srcID) {
    Integer rowKey = new Integer(row);
    SortedMap<Integer, String> trackMap = rowTracks_.get(rowKey);
    boolean isNew = false;
    if (trackMap == null) {
      trackMap = new TreeMap<Integer, String>();
      rowTracks_.put(rowKey, trackMap);
      isNew = true;
    }
    
    if (isNew) {
      trackMap.put(new Integer(0), srcID);
      return (new TrackSpec(row, 0));
    }
    
    //
    // Return preexisting reservation:
    //
    Iterator<Integer> tmkit = trackMap.keySet().iterator();
    while (tmkit.hasNext()) {
      Integer key = tmkit.next();
      String chkSrcID = trackMap.get(key);
      if (chkSrcID.equals(srcID)) {
        return (new TrackSpec(row, key.intValue()));
      }
    }
    
    Integer lastKey = trackMap.lastKey();
    Integer nextKey = new Integer(lastKey.intValue() + 1);
    trackMap.put(nextKey, srcID);
    return (new TrackSpec(row, nextKey.intValue()));
  }  
 
  /***************************************************************************
  ** 
  ** get the width
  **
  ** Tendency to overestimate puts outbound fanout links _way_ to the right!
  ** For fan-ins, extra width is soaked up in the gap between fan-in right edge and core
  **
  ** Note that direct outputs reserve a column on their right for their jump off point,
  ** even if no trace will be using that column.  Also, non-direct departures are reserving
  ** a column at the very right of the grid, which also goes unused.
  ** >>Every source coming into the grid will get a column reserved in track 0!
  ** 
  */

  public double getWidth() {  
    double width = dims_.getWidth();
    Iterator<Integer> ctit = colTracks_.keySet().iterator();
    while (ctit.hasNext()) {
      Integer keyA = ctit.next();
      SortedMap<Integer, String> trackMap = colTracks_.get(keyA);
      int minKey = Integer.MAX_VALUE;
      int maxKey = Integer.MIN_VALUE;
      Iterator<Integer> tmkit = trackMap.keySet().iterator();
      while (tmkit.hasNext()) {
        Integer key = tmkit.next();
        int keyVal = key.intValue();
        if (keyVal < minKey) {
          minKey = keyVal;
        }
        if (keyVal > maxKey) {
          maxKey = keyVal;
        }
      }     
      int numTracks = (maxKey - minKey) + 1;
      width += (numTracks * UiUtil.GRID_SIZE);
    }
    return (width);
  }  
  
  /***************************************************************************
  ** 
  ** Get the reserved slots on top
   * 
  */

  public int getTopReservations() {
    if (rowTracks_.isEmpty()) {
      return (0);
    }
    SortedMap<Integer, String> topTrackMap = rowTracks_.get(Integer.valueOf(0));
    if (topTrackMap == null) {
      return (0);
    }
    int minKey = Integer.MAX_VALUE;
    int maxKey = Integer.MIN_VALUE;
    Iterator<Integer> tmkit = topTrackMap.keySet().iterator();
    while (tmkit.hasNext()) {
      Integer key = tmkit.next();
      int keyVal = key.intValue();
      if (keyVal < minKey) {
        minKey = keyVal;
      }
      if (keyVal > maxKey) {
        maxKey = keyVal;
      }
    }
    return (maxKey - minKey + 1);
  }
  
  /***************************************************************************
  ** 
  ** Get the reserved slots on the bottom
   * 
  */

  public List<String> getBottomReservations() {
    ArrayList<String> retval = new ArrayList<String>();
    if (rowTracks_.isEmpty()) {
      return (retval);
    }
        
    SortedSet<Integer> forBot = new TreeSet<Integer>(rowTracks_.keySet());
    Integer topBot = forBot.last();
    //
    // Issue #256. If there are no bottom tracks, just a single top track, we get a wildly inflated 
    // bottom reservation count, since the one top track is being mistaken as the bottom track as well.
    // This fixes it:
    //
    
    if (topBot.intValue() == 0) {
      return (retval);
    }

    SortedMap<Integer, String> botTrackMap = rowTracks_.get(topBot);
    if (botTrackMap == null) {
      return (retval);
    }
    retval.addAll(botTrackMap.values());
    return (retval);
  }

  /***************************************************************************
  ** 
  ** get the height
  */

  public double getHeight() {
    double height = dims_.getHeight();
    Iterator<SortedMap<Integer, String>> rtit = rowTracks_.values().iterator();
    while (rtit.hasNext()) {
      SortedMap<Integer, String> trackMap = rtit.next();
      int numTracks = trackMap.size();
      height += (numTracks * UiUtil.GRID_SIZE);
    }
    return (height);
  }
  
  /***************************************************************************
  ** 
  ** get the height
  */

  public double getExactVerticalOffset(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String coreID) {
    if (exactVerticalOffset_ == Double.NEGATIVE_INFINITY) {
      exactVerticalOffset_ = calcExactVerticalOffset(nps, irx, coreID);
    }
    return (exactVerticalOffset_);
  }  
  
  /***************************************************************************
  ** 
  ** Fill in node positions
  */
  
  public void placeGrid(Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps, String ignoreID) {
   
    int colNum = grid_.getNumCols();
    if (colNum == 0) {
      return;
    }
    int rowNum = grid_.getNumRows();
    
    //
    // Account for traces between the nodes. Each planned trace between the
    // row/cols get a grid slot.
    //
    
    HashMap<Integer, Double> rowAllotment = new HashMap<Integer, Double>();
    Iterator<Integer> rtit = rowTracks_.keySet().iterator();
    while (rtit.hasNext()) {
      Integer rowKey = rtit.next();
      // Top row not included in calculation.
      if (rowKey.intValue() == 0) {
        continue;
      }
      SortedMap<Integer, String> trackMap = rowTracks_.get(rowKey);
      int numTracks = trackMap.size();
      double height = numTracks * UiUtil.GRID_SIZE;
      rowAllotment.put(rowKey, new Double(height));
    }
    
    HashMap<Integer, Double> colAllotment = new HashMap<Integer, Double>();
    Iterator<Integer> ctit = colTracks_.keySet().iterator();
    while (ctit.hasNext()) {
      Integer colKey = ctit.next();
      SortedMap<Integer, String> trackMap = colTracks_.get(colKey);
      int numTracks = trackMap.size();
      double width = numTracks * UiUtil.GRID_SIZE;
      colAllotment.put(colKey, new Double(width));
    }
  
    //
    // Loop through and place the nodes W.r.t the upperLeft point. Note that the top overtraces are not
    // included in the placement calcualtion here.
    //
    
   
    double upperLeftX = upperLeft.getX();
    double totalY = upperLeft.getY();
    double centerY = totalY;
    for (int i = 0; i < rowNum; i++) {
      Integer rowKey = new Integer(i);
      // Calc the centerline of the row:
      double slotHeight = heights_.get(rowKey).doubleValue();
      Double rowHeightObj = rowAllotment.get(rowKey);
      double rowHeight = (rowHeightObj == null) ? 0.0 : rowHeightObj.doubleValue();
      centerY = totalY + rowHeight + (slotHeight / 2.0);
      totalY += (slotHeight + rowHeight);
      double totalX = upperLeftX;
      double centerX = totalX;
      for (int j = 0; j < colNum; j++) {
        Integer colKey = new Integer(j);
        // Calc the centerline of the column:
        double slotWidth = widths_.get(colKey).doubleValue();
        Double colWidthObj = colAllotment.get(colKey);
        double colWidth = (colWidthObj == null) ? 0.0 : colWidthObj.doubleValue();
        centerX = totalX + colWidth + (slotWidth / 2.0);
        totalX += (slotWidth + colWidth);
        String nodeID = grid_.getCellValue(i, j);
        if ((nodeID == null) || nodeID.equals(ignoreID)) {
          continue;
        }
        Rectangle2D forCell = cellBounds_.get(nodeID);
        double cellCenterX = forCell.getCenterX();
        NodeProperties np = nps.getNodeProperties(nodeID);
        INodeRenderer rend = np.getRenderer();
        // We use JUST the glyph height for height:
        double yOffset = rend.getStraightThroughOffset();
        Point2D loc = new Point2D.Double();      
        UiUtil.forceToGrid(centerX - cellCenterX, centerY - yOffset, loc, UiUtil.GRID_SIZE);
        // Place the node:
        nps.setPosition(nodeID, loc);
      }
    }
    
    return;
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  public static Vector2D launchPadToOffset(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, int padNum, String nodeID) {
    NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
    INodeRenderer rend = np.getRenderer();
    Node srcNode = irx.getGenome().getNode(nodeID);
    Vector2D futureOff = rend.getLaunchPadOffsetForLayout(srcNode, irx, NodeProperties.RIGHT, nps.inboundLinkCount(nodeID));    
    Vector2D currentOff = rend.getLaunchPadOffsetForLayout(srcNode, irx, NodeProperties.RIGHT, null);     
    Vector2D useOff = (futureOff.lengthSq() > currentOff.lengthSq()) ? futureOff : currentOff;
    return (useOff);    
  }
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  public static Rectangle2D layoutBounds(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String nodeID, boolean textToo) {
    NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
    INodeRenderer rend = np.getRenderer();
    Node node = irx.getGenome().getNode(nodeID);
    Rectangle2D rectFuture = rend.getBoundsForLayout(node, irx, NodeProperties.RIGHT, textToo, nps.inboundLinkCount(nodeID));
    Rectangle2D rectCurrent = rend.getBoundsForLayout(node, irx, NodeProperties.RIGHT, textToo, null);    
    Rectangle2D useRect = (rectFuture.getWidth() > rectCurrent.getWidth()) ? rectFuture : rectCurrent; 
    return (useRect);    
  }
  
  /***************************************************************************
  ** 
  ** Launch pad for grid nodes:
  */
    
  public static int launchForGrid(String nodeID, SpecialtyLayoutEngine.NodePlaceSupport nps) {
    
    Node node = nps.getNode(nodeID);
    int type = node.getNodeType();
    if ((type == Node.BOX) || (type == Node.BARE) || 
       (type == Node.BUBBLE) || (type == Node.DIAMOND)) {
      return (1);
    } else {
      return (0);
    }
  }    
  
  /***************************************************************************
  ** 
  ** A function
  */
  
  public static Vector2D landingPadToOffset(BTState appState, int padNum, SpecialtyLayoutEngine.NodePlaceSupport nps, 
                                            DataAccessContext irx, String nodeID, int sign) { 
    NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
    INodeRenderer rend = np.getRenderer();
    Node srcNode = irx.getGenome().getNode(nodeID);   
    int currOrient = np.getOrientation();
    if (currOrient == NodeProperties.RIGHT) {
      Vector2D offset = rend.getLandingPadOffset(padNum, srcNode, sign, irx);
      return (offset);      
    } 

    //
    // Kinda bogus:  Need to calculate based on final orientation:
    // FIX ME! Get this like the laucnch pad calculation, with specified props, etc.
    //
    Layout bogus = new Layout(appState, "bogus", nps.getGenomeID());
    NodeProperties newProp = np.clone();
    newProp.setOrientation(NodeProperties.RIGHT);
    bogus.setNodeProperties(nodeID, newProp);
    DataAccessContext bogorcx = new DataAccessContext(irx);
    bogorcx.setLayout(bogus);
    Vector2D offset = rend.getLandingPadOffset(padNum, srcNode, sign, bogorcx);
    return (offset);  
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Calculate the nodal dimensions
  */

  private Dimension calcFanNodalDims(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx,
                                     String ignoreID, boolean textToo, Map<String, Rectangle2D> holdBounds) {

    Dimension retval = new Dimension();
      
    int colNum = grid_.getNumCols();
    if (colNum == 0) {
      retval.setSize(0.0, 0.0);  // double precision!
      return (retval);
    }
    int rowNum = grid_.getNumRows();
       
    for (int i = 0; i < rowNum; i++) {
      heights_.put(new Integer(i), new Double(0.0)); 
    }  
    for (int j = 0; j < colNum; j++) {
      widths_.put(new Integer(j), new Double(0.0));      
    } 
     
    for (int i = 0; i < rowNum; i++) {
      double maxHeight = heights_.get(new Integer(i)).doubleValue(); 
      for (int j = 0; j < colNum; j++) { 
        String nodeID = grid_.getCellValue(i, j);
        if ((nodeID == null) || nodeID.equals(ignoreID)) {
          continue;
        }
        Node node = irx.getGenome().getNode(nodeID);
        NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
        INodeRenderer rend = np.getRenderer();  
        Rectangle2D useRect = layoutBounds(nps, irx, nodeID, textToo);
        holdBounds.put(node.getID(), useRect);
        double width = useRect.getWidth();
         //
        // Note that not adding the padding for cases where there is only one
        // row or column makes things less bloated.  BUT if there are links
        // passing nearby (e.g. right below the one row), we need the padding 
        // to keep the link at a decent distance!  So we go with bloated...
        //
        width += FAN_GRID_PAD_X_;
        // Use only the height of the glyph, not the name (included in the bounds, above)
        double height = rend.getGlyphHeightForLayout(node, irx);
        height += FAN_GRID_PAD_Y_;
        width = UiUtil.forceToGridValueMax(width, UiUtil.GRID_SIZE);
        height = UiUtil.forceToGridValueMax(height, UiUtil.GRID_SIZE);        
        double maxWidth = widths_.get(new Integer(j)).doubleValue();
        if (width > maxWidth) {
          widths_.put(new Integer(j), new Double(width));  
        }
        if (height > maxHeight) {
          maxHeight = height;
          heights_.put(new Integer(i), new Double(height));
        }        
      }
    }
    
    double totalHeight = 0.0;
    double totalWidth = 0.0;
    
    for (int i = 0; i < rowNum; i++) {
      totalHeight += heights_.get(new Integer(i)).doubleValue(); 
    }  
    for (int j = 0; j < colNum; j++) {
      totalWidth += widths_.get(new Integer(j)).doubleValue();
    }
    
    retval.setSize(totalWidth, totalHeight);  // double precision!
    return (retval);    
  }

  /***************************************************************************
  ** 
  ** Calculate the exact vertical offset for straight-in inputs. This is following the
  ** same recipe as the node placement algorithm (ignoring the X dimension). Note again that
  ** the top 0 row reservations are not included in the calculation. 
  */

  private double calcExactVerticalOffset(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String coreID) {
    
    if (!grid_.contains(coreID)) {
      return (0.0);
    }
    
    HashMap<Integer, Double> rowAllotment = new HashMap<Integer, Double>();
    Iterator<Integer> rtit = rowTracks_.keySet().iterator();
    while (rtit.hasNext()) {
      Integer rowKey = rtit.next();
      if (rowKey.intValue() == 0) {
        continue;
      }
      SortedMap<Integer, String> trackMap = rowTracks_.get(rowKey);
      int numTracks = trackMap.size();
      double height = numTracks * UiUtil.GRID_SIZE;
      rowAllotment.put(rowKey, new Double(height));
    }
  
    Grid.RowAndColumn coreRC = grid_.findPositionRandC(coreID); 
    double nodeTotal = 0.0;
    for (int i = 0; i < coreRC.row; i++) {
      double rowHeight = heights_.get(new Integer(i)).doubleValue();
      nodeTotal += rowHeight;
      Double rowHeightObj = rowAllotment.get(new Integer(i));
      double rowHeightTracks = (rowHeightObj == null) ? 0.0 : rowHeightObj.doubleValue();
      nodeTotal += rowHeightTracks;
    }
    
    double halfHeight = heights_.get(new Integer(coreRC.row)).doubleValue() / 2.0;      
    Double rowHeightObj = rowAllotment.get(new Integer(coreRC.row));
    double rowHeightTracks = (rowHeightObj == null) ? 0.0 : rowHeightObj.doubleValue();
    halfHeight += rowHeightTracks;
    halfHeight = UiUtil.forceToGridValue(halfHeight, UiUtil.GRID_SIZE);
    nodeTotal += halfHeight;
    
    INodeRenderer rend = irx.getLayout().getNodeProperties(coreID).getRenderer();
    Node srcNode = nps.getNode(coreID);
    int launch = launchForGrid(coreID, new SpecialtyLayoutEngine.NodePlaceSupport(irx.getGenome(), irx.getLayout()));
    Vector2D lpOffset = rend.getLaunchPadOffset(launch, srcNode, irx);   
    double stOffset = rend.getStraightThroughOffset();
    return (nodeTotal - lpOffset.getY() - stOffset);    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  ** 
  ** Track position
  */
   
  public static class TrackPosRC extends SpecialtyLayoutLinkData.TrackPos {

    private RCTrack rcTrack;

    public TrackPosRC(RCTrack rcTrack) {
      super(null);
      this.rcTrack = rcTrack.clone();
    }    
    
    public TrackPosRC clone() {
      TrackPosRC retval = (TrackPosRC)super.clone();
      retval.rcTrack = this.rcTrack.clone();
      return (retval);
    }
    
    public RCTrack getRCTrack() {
      return (rcTrack);
    }
    
    public void fixLocation(Point2D point) {
      this.point = (Point2D)point.clone();
      return;
    }
    
    public String toString() {
      return (super.toString() + " rcTrack = " + rcTrack);
    }  
  }  

  /***************************************************************************
  ** 
  ** Track specification
  */
  
  public static class TrackSpec implements Cloneable, Comparable<TrackSpec> {

    public int major;
    public int minor;
    
    public TrackSpec(int major, int minor) {
      this.major = major;
      this.minor = minor;
    }
    
    public String toString() {
      return ("TrackSpec: maj=" + major + " min=" + minor);
    }    
    
    public TrackSpec clone() {
      try {
        return ((TrackSpec)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public int compareTo(TrackSpec other) {

      //
      // Remember: Return neg if this is less than other
      //
            
      //
      // The one in a higher major is always
      // greater:
      //
      
      if (this.major > other.major) {
        return (1);
      } else if (this.major < other.major) {
        return (-1);
      }
      
      //
      // Ok, the major is the same.  Use the minor:
      //
      
      if (this.minor > other.minor) {
        return (1);
      } else if (this.minor < other.minor) {
        return (-1);
      }      
      
      return (0);
    }
  }

  /***************************************************************************
  ** 
  ** Track
  */
   
  public class RCTrack implements Cloneable {

    public final static int NO_TRACK = Integer.MIN_VALUE;    
    public final static int COLUMN_MIDLINE = 0;
    public final static int ROW_MIDLINE    = 1;

    public final static int NONE_FIXED = 0;    
    public final static int Y_FIXED    = 1;
    public final static int X_FIXED    = 2; 
    public final static int Y_FLOATS   = 3;
    public final static int X_FLOATS   = 4;
    
    public final static int COL_SPEC = 0;    
    public final static int ROW_SPEC = 1; 
    
    private final static int COPY_COL_ = 0;    
    private final static int COPY_ROW_ = 1; 
    private final static int COPY_BOTH_ = 2;     
           
    private TrackSpec row_;
    private TrackSpec col_;
    
    private int colPadNum_;
    private String colNodeID_;
    private String colLinkID_;
    private boolean colIsLanding_;
    private int rowNum_;

    private int rowPadNum_;
    private String rowNodeID_;
    private String rowLinkID_;
    private boolean rowIsLanding_;
    private int colNum_;    
    
    private int linkSign_;
    
    private double fixedX_;
    private double fixedY_;
    private int whichFixed_;
    private BTState appState_;

    //
    // Dual defined track:
    //    
    
    public RCTrack(RCTrack other, TrackSpec spec, int whichSpec) {
      copyGuts(other, COPY_BOTH_);
      if (whichSpec == COL_SPEC) {
         col_ = spec;
      } else if (whichSpec == ROW_SPEC) {
         row_ = spec;
      } else {
        throw new IllegalArgumentException();
      }
    }    
 
    //
    // Dual defined track:
    //    
    
    public RCTrack(BTState appState, TrackSpec row, TrackSpec col) {
      appState_ = appState;
      this.row_ = row;
      this.col_ = col;
      whichFixed_ = NONE_FIXED;
      rowNum_ = NO_TRACK;
      colNum_ = NO_TRACK;      
    }
    
    //
    // This sets a fixed value, inherting the other dim from the other:
    //
    
    public RCTrack(RCTrack other, int whichFixed, double fixValue) {
      copyGuts(other, COPY_BOTH_);
      if (whichFixed_ != NONE_FIXED) {
        throw new IllegalArgumentException();
      }
      whichFixed_ = whichFixed;
      if (whichFixed == X_FIXED) {
         col_ = null;
         fixedX_ = fixValue;
         fixedY_ = Double.NaN;
      } else if (whichFixed == Y_FIXED) {
         row_ = null;
         fixedX_ = Double.NaN;  
         fixedY_ = fixValue;
      } else {
        throw new IllegalArgumentException();
      }
      rowNum_ = NO_TRACK;
      colNum_ = NO_TRACK;      
    }
    
    //
    // This leaves a value floating, inherting the other dim from the other
    //
    
    public RCTrack(RCTrack other, int whichFixed) {
      copyGuts(other, COPY_BOTH_);
      if (whichFixed_ != NONE_FIXED) {
        throw new IllegalArgumentException();
      }
      whichFixed_ = whichFixed;
      if (whichFixed == X_FLOATS) {
         col_ = null;
         fixedX_ = Double.NEGATIVE_INFINITY;
         fixedY_ = Double.NaN;
      } else if (whichFixed == Y_FLOATS) {
         row_ = null;
         fixedX_ = Double.NaN;  
         fixedY_ = Double.NEGATIVE_INFINITY;         
      } else {
        throw new IllegalArgumentException();
      }
      rowNum_ = NO_TRACK;
      colNum_ = NO_TRACK;      
    }    

    public RCTrack(RCTrack rowOther, RCTrack colOther) {
      copyGuts(rowOther, COPY_ROW_);
      copyGuts(colOther, COPY_COL_);      
    }        
 
    public RCTrack copyWithNewColPadNum(int padNum, String linkID) {
      RCTrack retval = this.clone();
      retval.colPadNum_ = padNum;
      retval.colLinkID_ = linkID; 
      return (retval);
    }
      
    @Override
    public RCTrack clone() {
      try {
        RCTrack retval = (RCTrack)super.clone();
        retval.row_ = (this.row_ == null) ? null : (TrackSpec)this.row_.clone();
        retval.col_ = (this.col_ == null) ? null : (TrackSpec)this.col_.clone();        
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    //
    // This puts a track definition on one dimension, and a pad definition on the other:
    //
    
    public RCTrack(BTState appState, TrackSpec spec, int whichMidline, int padNum, 
                   String nodeID, String linkID, boolean isLanding, int linkSign, int trackNum) {
      appState_ = appState;
      whichFixed_ = NONE_FIXED;
      if (whichMidline == COLUMN_MIDLINE) {
        row_ = spec;
        col_ = null;
        colPadNum_ = padNum; 
        colNodeID_ = nodeID;
        colLinkID_ = linkID; 
        colIsLanding_ = isLanding;
        colNum_ = trackNum;
        rowNum_ = NO_TRACK;
      } else if (whichMidline == ROW_MIDLINE) {
        row_ = null;
        col_ = spec;
        rowPadNum_ = padNum; 
        rowLinkID_ = linkID; 
        rowNodeID_ = nodeID; 
        rowIsLanding_ = isLanding;
        rowNum_ = trackNum;
        colNum_ = NO_TRACK;     
      } else {
        throw new IllegalArgumentException();
      }
      linkSign_ = linkSign;
    }
    
    //
    // This puts a track definition on one dimension, and a pad definition on the other:
    //
    
    public RCTrack(RCTrack other, int whichMidline, int padNum, 
                   String nodeID, String linkID, boolean isLanding, int linkSign, int trackNum) {
      copyGuts(other, COPY_BOTH_);
      if (whichMidline == COLUMN_MIDLINE) {
        if ((whichFixed_ == X_FIXED) || (whichFixed_ == X_FLOATS)) {
          whichFixed_ = NONE_FIXED;
        }
        col_ = null;
        colPadNum_ = padNum; 
        colNodeID_ = nodeID;
        colLinkID_ = linkID; 
        colIsLanding_ = isLanding;
        colNum_ = trackNum;
        rowNum_ = NO_TRACK;
      } else if (whichMidline == ROW_MIDLINE) {
        if ((whichFixed_ == Y_FIXED) || (whichFixed_ == Y_FLOATS)) {
          whichFixed_ = NONE_FIXED;
        }
        row_ = null;
        rowPadNum_ = padNum; 
        rowLinkID_ = linkID; 
        rowNodeID_ = nodeID; 
        rowIsLanding_ = isLanding;
        rowNum_ = trackNum;
        colNum_ = NO_TRACK;     
      } else {
        throw new IllegalArgumentException();
      }
      linkSign_ = linkSign;
    }    

    //
    // Dual pad definition:
    //
    
    public RCTrack(BTState appState, int colPadNum, String colNodeID, boolean colIsLanding, 
                   int rowPadNum, String rowNodeID, boolean rowIsLanding,             
                   int linkSign, String linkID, int colNum, int rowNum) {
      appState_ = appState;
      whichFixed_ = NONE_FIXED;      
      colPadNum_ = colPadNum; 
      colNodeID_ = colNodeID; 
      colLinkID_ = linkID; 
      colIsLanding_ = colIsLanding;
      rowPadNum_ = rowPadNum; 
      rowNodeID_ = rowNodeID; 
      rowLinkID_ = linkID; 
      rowIsLanding_ = rowIsLanding;
      linkSign_ = linkSign;
      colNum_ = colNum;
      rowNum_ = rowNum;
    }

    private void copyGuts(RCTrack other, int which) {
      this.appState_ = other.appState_;
      this.whichFixed_ = other.whichFixed_;
      this.linkSign_ = other.linkSign_; 
      if ((which == COPY_BOTH_) || (which == COPY_ROW_)) {
        this.row_ = (other.row_ == null) ? null : (TrackSpec)other.row_.clone();
        this.fixedY_ = other.fixedY_;      
        this.rowPadNum_ = other.rowPadNum_; 
        this.rowNodeID_ = other.rowNodeID_; 
        this.rowLinkID_ = other.rowLinkID_; 
        this.rowIsLanding_ = other.rowIsLanding_;
        this.rowNum_ = other.rowNum_;      
      }
      
      if ((which == COPY_BOTH_) || (which == COPY_COL_)) {      
        this.col_ = (other.col_ == null) ? null : (TrackSpec)other.col_.clone();      
        this.fixedX_ = other.fixedX_;   
        this.colPadNum_ = other.colPadNum_; 
        this.colNodeID_ = other.colNodeID_; 
        this.colLinkID_ = other.colLinkID_; 
        this.colIsLanding_ = other.colIsLanding_;
        this.colNum_ = other.colNum_;    
      }
      return;
    }    
    
    public int rowCompare(RCTrack other) {
      if (this.row_ != null) {
        if (other.row_ != null) {
          return (this.row_.compareTo(other.row_));
        } else if (other.rowNum_ != NO_TRACK) {
          int diff = this.row_.major - other.rowNum_;
          if (diff != 0) {
            return (diff);
          } else {
            return (-1);
          }
        } else {
          throw new IllegalStateException();  // Can't compare to absolutes or unpinned pads
        }
      } else if (this.rowNum_ != NO_TRACK) {  // this row is null, but we can compare tracks
        if (other.row_ != null) {
          int diff = this.rowNum_ - other.row_.major;
          if (diff != 0) {
            return (diff);
          } else {
            return (1);
          }
        } else if (other.rowNum_ != NO_TRACK) {
          return (this.rowNum_ - other.rowNum_);
        } else {
          throw new IllegalStateException();  // Can't compare to absolutes or unpinned pads
        }        
      } else {
        throw new IllegalStateException(); 
      }
    }    

    public String toString() {
      return ("RCTrack: r=" + row_ + " c=" + col_ + " which= " + whichFixed_  + " colNum = " + colNum_  
              + " rowNum = " + rowNum_ + " colPadNum = " + colPadNum_   + " rowPadNum = " + rowPadNum_);
    } 
    
    public int whichFixed() {
      return (whichFixed_);
    } 
    
    public double getFixedValue(int whichFixed) {
      if (whichFixed != whichFixed_) {
        throw new IllegalArgumentException();
      }
      if (whichFixed == X_FIXED) {
        return (fixedX_);
      } else if (whichFixed == Y_FIXED) {
        return (fixedY_);
      } else {
        throw new IllegalArgumentException();
      }
    }    
 
    public TrackSpec getRow() {
      if (row_ == null) {
        throw new IllegalStateException();
      }
      return (row_);
    }    
    
    public TrackSpec getCol() {
      if (col_ == null) {
        throw new IllegalStateException();
      }
      return (col_);
    }   
    
    public int getColPadNum() {
      return (colPadNum_);
    } 
    
    public int getRowPadNum() {
      return (rowPadNum_);
    }     
    


    public boolean isMyGrid(TrackedGrid grid) {
      return (TrackedGrid.this == grid);
    }
    
    public Point2D convert(Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String coreID, double fixFloating) {    

      int colNum = grid_.getNumCols();
      if (colNum == 0) {
        return ((Point2D)upperLeft.clone());
      }
      int rowNum = grid_.getNumRows();
    
      if ((col_ != null) && (col_.major > colNum)) {
        throw new IllegalArgumentException();        
      }    
      if ((row_ != null) && (row_.major > rowNum)) {
        throw new IllegalArgumentException();        
      }

      Point2D trackRoot = getTrackRoot(upperLeft, nps, irx, coreID);
      double trackX = trackRoot.getX();
      double trackY = trackRoot.getY(); 


      if (row_ != null) {
        trackY += (row_.minor * UiUtil.GRID_SIZE);
      }
  
      // >>Every source coming into the grid will get a column reserved in track 0!
      
      if (col_ != null) {
        int useColMinor  = col_.minor;
        if ((reverseCol_ != null) && (reverseCol_.intValue() == col_.major)) {
          int fkiv = TrackedGrid.this.colTracks_.get(reverseCol_).firstKey().intValue();
          useColMinor -= fkiv;
        }
        trackX += (useColMinor * UiUtil.GRID_SIZE);
      }      

      Point2D retval = new Point2D.Double(trackX, trackY);
            
      if (row_ == null) {
        double yVal;
        if (whichFixed_ == Y_FIXED) {
          yVal = fixedY_;
        } else if (whichFixed_ == Y_FLOATS) {
          yVal = fixFloating;
        } else {
          Point2D padPt = padPoint(nps, irx, ROW_MIDLINE);
          yVal = padPt.getY();
        }
        retval.setLocation(retval.getX(), yVal);
      }  
      
      if (col_ == null) {
        double xVal;
        if (whichFixed_ == X_FIXED) {
          xVal = fixedX_;
        } else if (whichFixed_ == X_FLOATS) {
          if (colPadNum_ != 0) {
            throw new IllegalArgumentException();
          }
          xVal = fixFloating;
        } else {
          Point2D padPt = padPoint(nps, irx, COLUMN_MIDLINE);
          xVal = padPt.getX();
        }
        retval.setLocation(xVal, retval.getY());
      }          
      
      UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);
      return (retval);        
    }
    
    private Point2D padPoint(SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, int whichMidline) {
      
      String nodeID;
      String linkID;
      int padNum;
      boolean isLanding;
      
      if (whichMidline == ROW_MIDLINE) {
        nodeID = rowNodeID_;
        linkID = rowLinkID_;
        padNum = rowPadNum_;
        isLanding = rowIsLanding_;            
      } else if (whichMidline == COLUMN_MIDLINE) {
        nodeID = colNodeID_;
        linkID = colLinkID_;
        padNum = colPadNum_;
        isLanding = colIsLanding_;            
      } else {
        throw new IllegalArgumentException();
      }
      
      if (isLanding) {
        padNum = getCurrentLandingPad(linkID, padNum, nps);
      } else {
        padNum = getCurrentLaunchPad(linkID, padNum, nps);        
      }      
        
      Point2D nodeLoc = nps.getPosition(nodeID);
      Vector2D offset;
      if (isLanding) {
        offset = landingPadToOffset(appState_, padNum, nps, irx, nodeID, linkSign_); 
      } else {
        offset = launchPadToOffset(nps, irx, padNum, nodeID);
      }
      return (offset.add(nodeLoc));
    }
    

    private int getCurrentLandingPad(String linkID, int oldPad, SpecialtyLayoutEngine.NodePlaceSupport nps) {  
      PadCalculatorToo.PadResult pres = nps.getPadChanges().get(linkID);
      return ((pres == null) ? oldPad : pres.landing);
    }
  

    private int getCurrentLaunchPad(String linkID, int oldPad, SpecialtyLayoutEngine.NodePlaceSupport nps) {  
      PadCalculatorToo.PadResult pres = nps.getPadChanges().get(linkID);
      return ((pres == null) ? oldPad : pres.launch);
    }
    
    private Point2D getTrackRoot(Point2D upperLeft, SpecialtyLayoutEngine.NodePlaceSupport nps, DataAccessContext irx, String coreID) {

      double xVal = upperLeft.getX();  // back-up default value only...
      double yVal = upperLeft.getY();
      
      if (row_ != null) {
        if (row_.major == 0) {
          yVal = upperLeft.getY();
          Integer key = new Integer(0);
          SortedMap<Integer, String> trackMap = rowTracks_.get(key);
          int numTracks = trackMap.size();
          yVal -= (numTracks * UiUtil.GRID_SIZE); 
        } else {
          int prevRow = row_.major - 1;
          int maxCol = grid_.getNumCols();
          for (int j = 0; j < maxCol; j++) {
            String nodeID = grid_.getCellValue(prevRow, j);
            if ((nodeID == null) || nodeID.equals(coreID)) {
              continue;
            }
            Point2D pos = nps.getPosition(nodeID);
            NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
            INodeRenderer rend = np.getRenderer();
            double yOffset = rend.getStraightThroughOffset();            
            double centerY = pos.getY() - yOffset;
            double rowHeight = heights_.get(new Integer(prevRow)).doubleValue();
            rowHeight -= FAN_GRID_PAD_Y_;
            yVal = centerY + (rowHeight / 2.0) + UiUtil.GRID_SIZE;
            yVal = UiUtil.forceToGridValueMax(yVal, UiUtil.GRID_SIZE);
            break;
          }
        }
      }
      
      if (col_ != null) {
        if (col_.major == 0) {
          xVal = upperLeft.getX();
        } else {
          // Find the first non-null guy in the same column 
          // (note we repurposed the "0 column" for core, so prevCol refers to
          // our previous usage...).  Use his position to nail the xValue.  FAILS
          // for full name width on e.g. bubbles!
          int prevCol = col_.major - 1;
          int maxRow = grid_.getNumRows();
          for (int j = 0; j < maxRow; j++) {
            String nodeID = grid_.getCellValue(j, prevCol);
            if ((nodeID == null) || nodeID.equals(coreID)) {
              continue;
            }
            Point2D pos = nps.getPosition(nodeID);
            NodeProperties np = irx.getLayout().getNodeProperties(nodeID);
            Rectangle2D forCell = cellBounds_.get(nodeID);
           
            INodeRenderer rend = np.getRenderer();
            double xOffset = rend.getVerticalOffset();
            double centerX = pos.getX() - xOffset;
            double colWidth = widths_.get(new Integer(prevCol)).doubleValue();
            colWidth -= FAN_GRID_PAD_X_;
            xVal = centerX + forCell.getCenterX() + (colWidth / 2.0) + UiUtil.GRID_SIZE;  // GRID_SIZE is a magic number
            xVal = UiUtil.forceToGridValueMax(xVal, UiUtil.GRID_SIZE);
            break;
          }
        }
      }
      return (new Point2D.Double(xVal, yVal));
    }
  }
} 
