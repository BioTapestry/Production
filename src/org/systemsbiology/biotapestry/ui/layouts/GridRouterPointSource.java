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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.Grid;

/***************************************************************************
**
** Route grid links
*/

public class GridRouterPointSource {

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
  
  private TrackedGrid grid_;
  private TrackedGrid.TrackPosRC rootPos_;
  private TreeMap<Double, SortedMap<Integer, TrackedGrid.TrackPosRC>> posPerRow_;
  private HashMap<Double, TrackedGrid.TrackPosRC> rightDeparturePerRow_;  
  private String srcID_;
  private int rootCol_;
  private int rootRow_;
  private BTState appState_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  public static class PointAndPath {
    public TrackedGrid.TrackPosRC pos;
    public ArrayList<TrackedGrid.TrackPosRC> path;
    
    PointAndPath() {
      path = new ArrayList<TrackedGrid.TrackPosRC>();
    }
    
    public String toString() {
      return ("PointAndPath: pos = " + pos + " path = " + path);
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
        
  public GridRouterPointSource(BTState appState, TrackedGrid grid, TrackedGrid.TrackPosRC rootPos, 
                               Grid.RowAndColumn srcRC, String srcID) {
    appState_ = appState;
    grid_ = grid;
    srcID_ = srcID;
    rootPos_ = rootPos.clone();
    rootCol_ = srcRC.col;
    rootRow_ = srcRC.row;    
    posPerRow_ = new TreeMap<Double, SortedMap<Integer, TrackedGrid.TrackPosRC>>();
    
    TrackedGrid.RCTrack colTrack = rootPos_.getRCTrack();
    TreeMap<Integer, TrackedGrid.TrackPosRC> rootPerRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
    double offset = (grid_.contains(srcID)) ? 0.5 : 0.0;
    posPerRow_.put(new Double(rootRow_ + offset), rootPerRow);
    TrackedGrid.RCTrack rootTrack = rootPos_.getRCTrack();   
    TrackedGrid.RCTrack rootRowLoc = grid_.buildRCTrack(rootTrack, colTrack); 
    TrackedGrid.TrackPosRC posForRootRow = new TrackedGrid.TrackPosRC(rootRowLoc);
    rootPerRow.put(new Integer(rootCol_), posForRootRow);        

    rightDeparturePerRow_ = new HashMap<Double, TrackedGrid.TrackPosRC>();      
  }  
  
 /***************************************************************************
  ** 
  ** Constructor for inbound tracks
  */
        
  public GridRouterPointSource(BTState appState, TrackedGrid grid, TrackedGrid.TrackPosRC rootPos, String srcID) {
    appState_ = appState;
    grid_ = grid;
    srcID_ = srcID;
    rootPos_ = rootPos.clone();
   // rootCol_ = srcRC.col;
   // rootRow_ = srcRC.row;    
    posPerRow_ = new TreeMap<Double, SortedMap<Integer, TrackedGrid.TrackPosRC>>();
    rightDeparturePerRow_ = new HashMap<Double, TrackedGrid.TrackPosRC>();      
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  /***************************************************************************
  ** 
  ** Get the root pos
  */

  public TrackedGrid.TrackPosRC getRootPos() {
    return (rootPos_);
  }

  /***************************************************************************
  ** 
  ** Get the outmost existing position for the given row.  If we need to have a
  ** path generated to get it, then that will come back too.
  */

  public PointAndPath getLastPoint(boolean direct, int rowNum, int colNum, int padNum, String nodeID, String linkID, int linkSign) {
    //
    // Get the outermost point for a row.  If that doesn't exist, we get
    // the central point.  If that doesn't exist, we build a path from 
    // the root.
    //
    
    PointAndPath retval = new PointAndPath();
    double fractionalRowNum = (direct) ? rowNum + 0.5 : rowNum;
    double fractionalRoot = rootRow_ + 0.5;    
    
    Double rowNumObj = new Double(fractionalRowNum);
    SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(rowNumObj);  // any points on row already?
    TrackedGrid.TrackPosRC posForRow;
    TrackedGrid.TrackPosRC posForRowCol = null;
    TrackedGrid.RCTrack rowLoc = null;
    if (perRow == null) { // no, so build one:
      boolean wasNoRows = posPerRow_.isEmpty();
      double lastRow = Double.NEGATIVE_INFINITY;
      
      if (!wasNoRows) {
        Double outKey = (fractionalRowNum <= fractionalRoot) ? posPerRow_.firstKey() : posPerRow_.lastKey();
        double maybeLast = outKey.doubleValue();
        if ((fractionalRowNum <= fractionalRoot) && (maybeLast <= fractionalRoot)) {
          lastRow = maybeLast;
        } else if ((fractionalRowNum > fractionalRoot) && (maybeLast >= fractionalRoot)) {
          lastRow = maybeLast;
        }
      }
      perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      posPerRow_.put(rowNumObj, perRow);
      TrackedGrid.RCTrack colTrack = rootPos_.getRCTrack();      
      TrackedGrid.TrackSpec colSpec = colTrack.getCol();
      if (!direct) {
        TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(rowNum, srcID_);
        rowLoc = grid_.buildRCTrack(appState_, rowSpec, colSpec);
      } else {
        rowLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, padNum, nodeID, linkID,
                                                 true, linkSign, rowNum);      
      }
      posForRow = new TrackedGrid.TrackPosRC(rowLoc);
      perRow.put(new Integer(rootCol_), posForRow.clone());
      if (wasNoRows) { // no rows even, so start from root
        retval.path.add(rootPos_.clone());
        /*
        if (direct) {
          TreeMap rootPerRow = new TreeMap();
          posPerRow_.put(new Double(rootRow_ + 0.5), rootPerRow);
          TrackedGrid.RCTrack rootTrack = (TrackedGrid.RCTrack)rootPos_.getRCTrack();   
          TrackedGrid.RCTrack rootRowLoc = grid_.buildRCTrack(rootTrack, colTrack); 
          TrackedGrid.TrackPosRC posForRootRow = new TrackedGrid.TrackPosRC(rootRowLoc);
          rootPerRow.put(new Integer(rootCol_), posForRootRow);        
        }
         */
      } else if (lastRow != Double.NEGATIVE_INFINITY) { // had a row to work from
        SortedMap<Integer, TrackedGrid.TrackPosRC> perLastRow = posPerRow_.get(new Double(lastRow));
        Integer outKey = new Integer(rootCol_);        
        TrackedGrid.TrackPosRC posForLastRow = perLastRow.get(outKey);
        retval.path.add(posForLastRow.clone());        
      }      
    } else {  // got points on the row
      Integer outKey = (colNum <= rootCol_) ? perRow.firstKey() : perRow.lastKey();
      posForRow = perRow.get(outKey);
      // If column matches, we just differ by a pad number (or maybe not, if no multipads...)
      if (colNum == outKey.intValue()) {
        posForRowCol = posForRow;
      }      
    }
    retval.path.add(posForRow.clone());
    
    //
    // If we already have a point for the column, we return it if the pads match.  Otherwise,
    // we just treat it as normal:
    //
    
    if (posForRowCol != null) {
      if (posForRowCol.getRCTrack().getColPadNum() == padNum) {
        retval.pos = (TrackedGrid.TrackPosRC)posForRowCol.clone();
        return (retval);
      }
    }
    

    TrackedGrid.RCTrack lastLoc;
    if (!direct) {
      TrackedGrid.TrackSpec rowSpec = posForRow.getRCTrack().getRow();
      lastLoc = grid_.buildRCTrackForColMidline(appState_, rowSpec, padNum, nodeID, linkID,
                                                true, linkSign, TrackedGrid.RCTrack.NO_TRACK);
    } else {
      lastLoc = rowLoc.clone();
    }
    
    
    TrackedGrid.TrackPosRC lastPos = new TrackedGrid.TrackPosRC(lastLoc);    
    perRow.put(new Integer(colNum), lastPos);
    retval.pos = lastPos.clone();    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** For the special case of a bottom-entering elbow, return the one point to
  ** handle it:
  */

  public TrackedGrid.TrackPosRC getBottomElbowPoint(int rowNum, int colNum, int sPadNum, int tPadNum, String nodeID, String linkID, int linkSign) {
    //
    // Get the outermost point for a row.  If that doesn't exist, we get
    // the central point.  If that doesn't exist, we build a path from 
    // the root.
    //
    
 //   PointAndPath retval = new PointAndPath();
    double fractionalRowNum = rowNum + 0.5;
    double fractionalRoot = rootRow_ + 0.5;    
    
    Double rowNumObj = new Double(fractionalRowNum);
    SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(rowNumObj);  // any points on row already?
    TrackedGrid.TrackPosRC posForRow;
    TrackedGrid.TrackPosRC posForRowCol = null;
    TrackedGrid.RCTrack rowLoc = null;
    
    if (perRow == null) { // no, so build one:
      boolean wasNoRows = posPerRow_.isEmpty();
      double lastRow = Double.NEGATIVE_INFINITY;
      
      if (!wasNoRows) {
        Double outKey = (Double)((fractionalRowNum <= fractionalRoot) ? posPerRow_.firstKey() : posPerRow_.lastKey());
        double maybeLast = outKey.doubleValue();
        if ((fractionalRowNum <= fractionalRoot) && (maybeLast <= fractionalRoot)) {
          lastRow = maybeLast;
        } else if ((fractionalRowNum > fractionalRoot) && (maybeLast >= fractionalRoot)) {
          lastRow = maybeLast;
        }
      }
      perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      posPerRow_.put(rowNumObj, perRow);
      TrackedGrid.RCTrack colTrack = rootPos_.getRCTrack();      
      TrackedGrid.TrackSpec colSpec = colTrack.getCol();
   
      rowLoc = grid_.buildRCTrackForRowMidline(appState_, colSpec, tPadNum, nodeID, linkID, true, linkSign, rowNum);      
      posForRow = new TrackedGrid.TrackPosRC(rowLoc);
      perRow.put(new Integer(rootCol_), posForRow.clone());
      if (wasNoRows) { // no rows even, so start from root
     //   retval.path.add(rootPos_.clone());
      } else if (lastRow != Double.NEGATIVE_INFINITY) { // had a row to work from
        throw new IllegalStateException(); 
      }      
    } else {  // got points on the row
      Integer outKey = (colNum <= rootCol_) ? perRow.firstKey() : perRow.lastKey();
      posForRow = perRow.get(outKey);
      // If column matches, we just differ by a pad number (or maybe not, if no multipads...)
      if (colNum == outKey.intValue()) {
        posForRowCol = posForRow;
      }      
    }
    //retval.path.add(posForRow.clone());
    
    //
    // If we already have a point for the column, we return it if the pads match.  Otherwise,
    // we just treat it as normal:
    //
    
    if (posForRowCol != null) {
      throw new IllegalStateException(); 
    }
    
    TrackedGrid.RCTrack lastLoc;
    
    if (rowLoc == null) {
      lastLoc = grid_.buildRCTrackForDualMidline(appState_, tPadNum, nodeID, true, sPadNum, 
                                                 srcID_, false, linkSign, linkID, 
                                                 TrackedGrid.RCTrack.NO_TRACK, rowNum); 

      
      
      
      
 //     TrackedGrid.TrackSpec colSpec = posForRow.getRCTrack().getCol();
   //   lastLoc = grid_.buildRCTrackForRowMidline(colSpec, padNum, nodeID, linkID,
                                                //true, linkSign, TrackedGrid.RCTrack.NO_TRACK);
    } else {
      lastLoc = (TrackedGrid.RCTrack)rowLoc.clone();
    }
    
    TrackedGrid.TrackPosRC lastPos = new TrackedGrid.TrackPosRC(lastLoc);    
    perRow.put(new Integer(colNum), lastPos);
    return ((TrackedGrid.TrackPosRC)lastPos.clone());
  }
  
  /***************************************************************************
  ** 
  ** Need to get a start point for feedback onto the core
  */

  public PointAndPath getSimpleFeedbackPoint() {

    PointAndPath retval = new PointAndPath();
    setupFeedbackPoint(retval);
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Need to get a start point for feedback onto the core
  */

  public PointAndPath getPointForInboundDirectToCore(boolean comingFromTop) {
    PointAndPath retval = new PointAndPath();
    inboundDirectToCore(retval, comingFromTop);
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get the last point needed for an inbound link.  If we need to have a
  ** path generated to get it, then that will come back too.
  */

  public PointAndPath getLastPointForInbound(boolean direct, int rowNum, int colNum, 
                                             int padNum, String nodeID, String linkID, 
                                             int linkSign, boolean comingFromTop) {
    //
    // Get the point.  If that doesn't exist, we build a path from 
    // the inbound corner.
    //
    
    PointAndPath retval = new PointAndPath();
    Integer farLeftKey = new Integer(-1);
    
    double fractionalRowNum = (direct) ? rowNum + 0.5 : rowNum;
        
    Double rowNumObj = new Double(fractionalRowNum);
    SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(rowNumObj);  // any points on row already?
    TrackedGrid.TrackPosRC posForRow;
    TrackedGrid.TrackPosRC posForRowCol = null;
    TrackedGrid.RCTrack rowLoc = null; 
    
    if (perRow == null) { // no, so build one:
      boolean wasNoRows = posPerRow_.isEmpty();
      double lastRow = (wasNoRows) ? Double.NEGATIVE_INFINITY 
                                   : ((comingFromTop) ? posPerRow_.lastKey() : posPerRow_.firstKey()).doubleValue();
      perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      posPerRow_.put(rowNumObj, perRow);
      TrackedGrid.RCTrack colTrack = rootPos_.getRCTrack();
      if (!direct) {
        TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(rowNum, srcID_);
        rowLoc = grid_.buildRCTrack(colTrack, rowSpec, TrackedGrid.RCTrack.ROW_SPEC);
      } else {        
        rowLoc = grid_.inheritRCTrackNewRowMidline(colTrack, padNum, nodeID, linkID,
                                                   true, linkSign, rowNum);
      }
      posForRow = new TrackedGrid.TrackPosRC(rowLoc);
      perRow.put(farLeftKey, posForRow.clone());
      if (wasNoRows) { // no rows even, so start from root. Need to add a 0 row point if point was direct
        if (!direct || (direct && comingFromTop)) {
          retval.path.add(rootPos_.clone());
        }
        /*
        if (direct) {
          TreeMap rootPerRow = new TreeMap();
          posPerRow_.put(new Double(0.0), rootPerRow);
          TrackedGrid.RCTrack rootTrack = (TrackedGrid.RCTrack)rootPos_.getRCTrack();   
          TrackedGrid.RCTrack rootRowLoc = grid_.buildRCTrack(rootTrack, colTrack); 
          TrackedGrid.TrackPosRC posForRootRow = new TrackedGrid.TrackPosRC(rootRowLoc);
          rootPerRow.put(new Integer(rootCol_), posForRootRow);        
        }
         */
      } else if (lastRow != Double.NEGATIVE_INFINITY) { // had a row to work from
        SortedMap<Integer, TrackedGrid.TrackPosRC> perLastRow = posPerRow_.get(new Double(lastRow));  
        TrackedGrid.TrackPosRC posForLastRow = perLastRow.get(farLeftKey);
        retval.path.add(posForLastRow.clone());
      }      
    } else {  // got points on the row
      Integer outKey = perRow.lastKey();
      posForRow = perRow.get(outKey);
      // If column matches, we just differ by a pad number (or maybe not, if no multipads...)
      if (colNum == outKey.intValue()) {
        posForRowCol = posForRow;
      }  
    }
    retval.path.add(posForRow.clone());
 
    
    //
    // If we already have a point for the column, we return it if the pads match.  Otherwise,
    // we just treat it as normal:
    //
    
    if (posForRowCol != null) {
      if (!direct) {
        if (posForRowCol.getRCTrack().getColPadNum() == padNum) {
          retval.pos = posForRowCol.clone();
          return (retval);
        }
      } else {
        if (posForRowCol.getRCTrack().getRowPadNum() == padNum) {
          retval.pos = posForRowCol.clone();
          return (retval);
        }                
      }
    }    
       
    //
    // generate it, store it, and return it:
    //
    
    TrackedGrid.RCTrack lastLoc;
    if (!direct) {
      TrackedGrid.TrackSpec rowSpec = posForRow.getRCTrack().getRow();
      lastLoc = grid_.buildRCTrackForColMidline(appState_, rowSpec, padNum, nodeID, linkID,
                                                true, linkSign, TrackedGrid.RCTrack.NO_TRACK);
    } else {
      lastLoc = rowLoc.clone();
    }    

    TrackedGrid.TrackPosRC lastPos = new TrackedGrid.TrackPosRC(lastLoc);    
    perRow.put(new Integer(colNum), lastPos);
    retval.pos = lastPos.clone();
     
    return (retval);    
  }  

  /***************************************************************************
  ** 
  ** Hang a jumper off an existing link directly to the core:
  */

  public PointAndPath hangJumperOffDirectToCore() {
           
    PointAndPath retval = new PointAndPath();
    DepartureInfo di = setupDepartingPoint(retval);
    TrackedGrid.TrackPosRC lastPos = rightDeparturePerRow_.get(di.useRowNumObj);    
    retval.pos = lastPos.clone();
    // Just want the point, not the path up to it:
    retval.path.clear();
    return (retval);
  }  
 
  /***************************************************************************
  ** 
  ** Tweak departure for jumper:
  */

  public void updateLastDepartingPointToCore(TrackedGrid.TrackPosRC newLastPos) {           
    PointAndPath retval = new PointAndPath();
    DepartureInfo di = setupDepartingPoint(retval);
    rightDeparturePerRow_.put(di.useRowNumObj, newLastPos.clone());
    return;
  }    
  
  /***************************************************************************
  ** 
  ** Get the "departing" point for the grid, i.e. the best place to have a
  ** link departing from the grid to go to the target.
  */

  public PointAndPath getDepartingPointToCore(int padNum, String nodeID, String linkID, int linkSign) {
    
    //
    // Look for the highest numbered column in the highest numbered row that we can find.
    // If nothing shows up, build one.
    //
        
    PointAndPath retval = new PointAndPath();
    DepartureInfo di = setupDepartingPoint(retval);

    TrackedGrid.RCTrack lastLoc = 
      grid_.inheritRCTrackNewColMidline(di.rowTrack, padNum, nodeID, linkID,
                                        true, linkSign, TrackedGrid.RCTrack.NO_TRACK);
    TrackedGrid.TrackPosRC lastPos = new TrackedGrid.TrackPosRC(lastLoc); 
    rightDeparturePerRow_.put(di.useRowNumObj, lastPos);    
    retval.pos = (TrackedGrid.TrackPosRC)lastPos.clone();
    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get the "departing" point for the grid, i.e. the best place to have a
  ** link departing from the grid to go to the target.
  */

  public PointAndPath getDepartingPointToOutbound() {
    
    PointAndPath retval = new PointAndPath();
    DepartureInfo di = setupDepartingPoint(retval);

    int numCol = grid_.getNumCols();
    TrackedGrid.TrackSpec colSpec = grid_.reserveColumnTrack(numCol, srcID_);
    TrackedGrid.RCTrack lastLoc = grid_.buildRCTrack(di.rowTrack, colSpec, TrackedGrid.RCTrack.COL_SPEC);
    TrackedGrid.TrackPosRC lastPos = new TrackedGrid.TrackPosRC(lastLoc); 
    rightDeparturePerRow_.put(di.useRowNumObj, lastPos);    
    retval.pos = (TrackedGrid.TrackPosRC)lastPos.clone(); 
    
    return (retval);
  }  
 

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Setup for departing point
  */

  private DepartureInfo setupDepartingPoint(PointAndPath pap) {
    
    //
    // Look for the highest numbered column in the highest numbered row that we can find.
    // If nothing shows up, build one.
    //
        
    TrackedGrid.TrackPosRC usePosForRow = null;
    Double useRowNumObj = null;
    TrackedGrid.TrackPosRC establishedPosForRow = null;    
    Double establishedRow = null;
    TrackedGrid.TrackPosRC bottomPosForRow = null;    
    Double bottomRow = null;    
    
      
    Iterator<Double> pprkit = posPerRow_.keySet().iterator();
    while (pprkit.hasNext()) {
      Double rowNumObj = pprkit.next();
      // Departing points can't use midrows (keys are n.5, not n.0)
      boolean keyEven = (Math.floor(rowNumObj.doubleValue()) == rowNumObj.doubleValue());
      SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(rowNumObj);  // any points on row already? 
      Integer outKey = perRow.lastKey();
      if (keyEven) {
        int maybeLast = outKey.intValue();
        if (maybeLast >= rootCol_) { 
          usePosForRow = perRow.get(outKey);
          useRowNumObj = rowNumObj;
        }
        establishedPosForRow = perRow.get(outKey);
        establishedRow = rowNumObj;
      }
      bottomPosForRow = perRow.get(outKey);
      bottomRow = rowNumObj;
    }

    boolean haveARow = ((useRowNumObj != null) || (establishedRow != null));
    TrackedGrid.RCTrack rowTrack;
    if (haveARow) {
      if (useRowNumObj == null) {  // Not optimal, but still usable 
         usePosForRow = establishedPosForRow;
         useRowNumObj = establishedRow;
      }
      pap.path.add(usePosForRow.clone());
      rowTrack = usePosForRow.getRCTrack();      
    } else if (bottomRow != null) { // only have midrow points
      pap.path.add(bottomPosForRow.clone());
      SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      useRowNumObj = new Double(bottomRow.doubleValue() + 0.5);
      posPerRow_.put(useRowNumObj, perRow);
      TrackedGrid.TrackSpec colSpec = rootPos_.getRCTrack().getCol();
      TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(useRowNumObj.intValue(), srcID_);
      rowTrack = grid_.buildRCTrack(appState_, rowSpec, colSpec);
      TrackedGrid.TrackPosRC posForRow = new TrackedGrid.TrackPosRC(rowTrack);
      perRow.put(new Integer(rootCol_), posForRow.clone());
      pap.path.add(posForRow.clone());          
    } else { // nothing there, so build a minimal starting point:
      SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      useRowNumObj = new Double(rootRow_);
      posPerRow_.put(useRowNumObj, perRow);
      TrackedGrid.TrackSpec colSpec = rootPos_.getRCTrack().getCol();
      TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(rootRow_, srcID_);
      rowTrack = grid_.buildRCTrack(appState_, rowSpec, colSpec);
      TrackedGrid.TrackPosRC posForRow = new TrackedGrid.TrackPosRC(rowTrack);
      perRow.put(new Integer(rootCol_), posForRow.clone());
      pap.path.add(rootPos_.clone()); 
      pap.path.add(posForRow.clone());
    }
    
    return (new DepartureInfo(rowTrack, useRowNumObj));
  }  
  
  /***************************************************************************
  ** 
  ** Setup for simple feedback
  */

  private DepartureInfo setupFeedbackPoint(PointAndPath pap) {
    
    TrackedGrid.TrackPosRC usePosForRow = null;    
    Double useRowNumObj = null;    
    
    //
    // Get the lowest row above the root:
    //
    
    Iterator<Double> pprkit = posPerRow_.keySet().iterator();
    while (pprkit.hasNext()) {
      Double rowNumObj = pprkit.next();
      if (rowNumObj.doubleValue() > rootRow_) {
        break;
      }        
      SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(rowNumObj);
      Integer feedKey = perRow.firstKey();
      usePosForRow = perRow.get(feedKey);
      useRowNumObj = rowNumObj;
    }
    
    //
    // Get the points per row:
    //
         
    TrackedGrid.RCTrack rowTrack;
    if (useRowNumObj != null) {
      pap.pos = usePosForRow.clone();
      rowTrack = usePosForRow.getRCTrack();
    } else { // nothing there, so build a minimal starting point:
      SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      useRowNumObj = new Double(rootRow_);
      posPerRow_.put(useRowNumObj, perRow);
      TrackedGrid.TrackSpec colSpec = rootPos_.getRCTrack().getCol();
      TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(rootRow_, srcID_);
      rowTrack = grid_.buildRCTrack(appState_, rowSpec, colSpec);
      TrackedGrid.TrackPosRC posForRow = new TrackedGrid.TrackPosRC(rowTrack);
      perRow.put(new Integer(rootCol_), posForRow.clone());
      pap.path.add(rootPos_.clone()); 
      pap.pos = posForRow.clone();
    }
    
    return (new DepartureInfo(rowTrack, useRowNumObj));
  } 
  
  /***************************************************************************
  ** 
  ** Handle going direct to the core:
  */

  private DepartureInfo inboundDirectToCore(PointAndPath pap, boolean comingFromTop) {
    
    TrackedGrid.TrackPosRC usePosForRow = null;    
    Double useRowNumObj = null;    
    
    //
    // Get the lowest row we can:
    //
    
    Double rootKey = new Double(0.0);
    Double getKey = (posPerRow_.isEmpty()) ? rootKey : (Double)posPerRow_.lastKey();
    if (Math.floor(getKey.doubleValue()) != getKey.doubleValue()) {  // If fractional, go to root
      getKey = rootKey;
    }
    SortedMap<Integer, TrackedGrid.TrackPosRC> perRow = posPerRow_.get(getKey);
    if (perRow != null) {
      Integer coreKey = perRow.lastKey();
      usePosForRow = perRow.get(coreKey);
      useRowNumObj = getKey;
    }
    
    //
    // Get the points per row:
    //
         
    TrackedGrid.RCTrack rowTrack;
    if (useRowNumObj != null) {
      pap.pos = usePosForRow.clone();
      rowTrack = usePosForRow.getRCTrack();
    } else {
      //
      // Nothing there, so build a minimal starting point.  Coming from the top,
      // we just use the root.  Coming from the bottom, use the last previous
      // point.  If none, use the root.
      // 

      if (!comingFromTop && !posPerRow_.isEmpty()) {
        getKey = posPerRow_.firstKey();
        perRow = posPerRow_.get(getKey);
        Integer coreKey = perRow.firstKey();
        usePosForRow = perRow.get(coreKey);
        pap.path.add(usePosForRow.clone());
      }

      perRow = new TreeMap<Integer, TrackedGrid.TrackPosRC>();
      useRowNumObj = new Double(rootRow_);
      posPerRow_.put(useRowNumObj, perRow);
      TrackedGrid.RCTrack rootRC = rootPos_.getRCTrack();
      TrackedGrid.TrackSpec rowSpec = grid_.reserveRowTrack(rootRow_, srcID_);
      rowTrack = grid_.buildRCTrack(rootRC, rowSpec, TrackedGrid.RCTrack.ROW_SPEC);
      TrackedGrid.TrackPosRC posForRow = new TrackedGrid.TrackPosRC(rowTrack);
      perRow.put(new Integer(rootCol_), posForRow.clone());
       
      pap.path.add(rootPos_.clone()); 
      pap.pos = posForRow.clone();
    }
  
    return (new DepartureInfo(rowTrack, useRowNumObj));
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static class DepartureInfo {
    TrackedGrid.RCTrack rowTrack;
    Double useRowNumObj;
    
    DepartureInfo(TrackedGrid.RCTrack rowTrack, Double useRowNumObj) {
      this.rowTrack = rowTrack;
      this.useRowNumObj = useRowNumObj;
    }
  
  }
} 
