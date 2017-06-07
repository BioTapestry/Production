/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.Pattern;

import org.systemsbiology.biotapestry.analysis.Link;

/***************************************************************************
**
** Gene/Link construct for dialog-built networks
*/
  
public class DialogBuiltSignalMotif extends DialogBuiltMotif {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  protected final static int SOURCE_       = 0;
  protected final static int SIGNAL_       = 1;
  protected final static int BUBBLE_       = 2;
  protected final static int TRANSFAC_     = 3;
  protected final static int TARGET_       = 4;

  protected final static int NUM_COL_TAGS_ = 5;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  protected String signalId_;
  protected String toSignalLinkId_;
  protected String bubbleId_;
  protected String toBubbleLinkId_; 
  protected String transFacId_;
  protected String fromTransFacLinkId_;
  protected String toTargetPosLinkId_;
  protected String toTargetNegLinkId_;
  protected int signalMode_;   // PROMOTE_SIGNAL, REPRESS_SIGNAL, or SWITCH_SIGNAL   
 
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltSignalMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltSignalMotif(String sourceId, String targetId, String signalId,
                                String toSignalLinkId, String bubbleId,
                                String toBubbleLinkId, String transFacId,
                                String fromTransFacLinkId, String toTargetPosLinkId,
                                String toTargetNegLinkId, int signalMode) {
    super(sourceId, targetId);
    toSignalLinkId_ = toSignalLinkId;
    bubbleId_ = bubbleId;
    toBubbleLinkId_ = toBubbleLinkId; 
    transFacId_ = transFacId;
    fromTransFacLinkId_ = fromTransFacLinkId;
    toTargetPosLinkId_ = toTargetPosLinkId;
    toTargetNegLinkId_ = toTargetNegLinkId;
    signalMode_ = signalMode;
    signalId_ = signalId;
  }
  
  /***************************************************************************
  **
  ** Get the signal ID
  */
  
  public String getSignalId() {
    return (signalId_);
  }
    
  /***************************************************************************
  **
  ** Set the signal ID
  */
  
  public void setSignalId(String signalId) {
    signalId_ = signalId;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the signal link ID
  */
  
  public String getSignalLinkId() {
    return (toSignalLinkId_);
  }
    
  /***************************************************************************
  **
  ** Set the signal link ID
  */
  
  public void setSignalLinkId(String toSignalLinkId) {
    toSignalLinkId_ = toSignalLinkId;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the BubbleId
  */
  
  public String getBubbleId() {
    return (bubbleId_);
  }
    
  /***************************************************************************
  **
  ** Set the BubbleId
  */
  
  public void setBubbleId(String bubbleId) {
    bubbleId_ = bubbleId;
    return;
  }  

  /***************************************************************************
  **
  ** Get the BubbleLinkId
  */
  
  public String getBubbleLinkId() {
    return (toBubbleLinkId_);
  }
    
  /***************************************************************************
  **
  ** Set the BubbleLinkId
  */
  
  public void setBubbleLinkId(String toBubbleLinkId) {
    toBubbleLinkId_ = toBubbleLinkId;
    return;
  }  

  /***************************************************************************
  **
  ** Get the TransFacId
  */
  
  public String getTransFacId() {
    return (transFacId_);
  }
    
  /***************************************************************************
  **
  ** Set the TransFacId
  */
  
  public void setTransFacId(String transFacId) {
    transFacId_ = transFacId;
    return;
  }  

  /***************************************************************************
  **
  ** Get the TransFacLinkId
  */
  
  public String getTransFacLinkId() {
    return (fromTransFacLinkId_);
  }
    
  /***************************************************************************
  **
  ** Set the sTransFacLinkId
  */
  
  public void setTransFacLinkId(String fromTransFacLinkId) {
    fromTransFacLinkId_ = fromTransFacLinkId;
    return;
  }

  /***************************************************************************
  **
  ** Get the TargetPosLinkId
  */
  
  public String getTargetPosLinkId() {
    return (toTargetPosLinkId_);
  }
    
  /***************************************************************************
  **
  ** Set the TargetPosLinkId
  */
  
  public void setTargetPosLinkId(String toTargetPosLinkId) {
    toTargetPosLinkId_ = toTargetPosLinkId;
    return;
  }  

  /***************************************************************************
  **
  ** Get the TargetNegLinkId
  */
  
  public String getTargetNegLinkId() {
    return (toTargetNegLinkId_);
  }
    
  /***************************************************************************
  **
  ** Set the TargetNegLinkId
  */
  
  public void setTargetNegLinkId(String toTargetNegLinkId) {
    toTargetNegLinkId_ = toTargetNegLinkId;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the signal mode: PROMOTE_SIGNAL, REPRESS_SIGNAL, or SWITCH_SIGNAL  
  */
  
  public int getSignalMode() {
    return (signalMode_);
  }
    
  /***************************************************************************
  **
  ** Set the signal mode
  */
  
  public void setSignalMode(int signalMode) {
    signalMode_ = signalMode;
    return;
  }
  
  /***************************************************************************
  **
  ** Get display ordering
  */
  
  public String[] ordering() {
    return (new String[] {sourceId_, signalId_, transFacId_, bubbleId_, targetId_});
  }
  
  /***************************************************************************
  **
  ** Get empty copy
  */
  

  public DialogBuiltMotif emptyCopy() {
    return (new DialogBuiltSignalMotif());
  }
  
  /***************************************************************************
  **
  ** Find out what nodes and links exist in the motif
  */

  public void getExistingNodesAndLinks(Set<String> nodes, Set<String> links) {
    super.getExistingNodesAndLinks(nodes, links);
    if (signalId_ != null) {
      nodes.add(signalId_);
    }
    if (toSignalLinkId_ != null) {
      links.add(toSignalLinkId_);
    }
    if (bubbleId_ != null) {
      nodes.add(bubbleId_);
    }
    if (toBubbleLinkId_ != null) {
      links.add(toBubbleLinkId_);
    }
    if (transFacId_ != null) {
      nodes.add(transFacId_);
    }
    if (fromTransFacLinkId_ != null) {
      links.add(fromTransFacLinkId_);
    }
    if (toTargetPosLinkId_ != null) {
      links.add(toTargetPosLinkId_);
    }
    if (toTargetNegLinkId_ != null) {
      links.add(toTargetNegLinkId_);
    }
    return;
  }

  /***************************************************************************
  **
  ** Assign the nodes to source and target regions
  */

  public void getRegionAssignments(Set<String> srcRegionNodes, Set<String> targRegionNodes, Map<String, Integer> linkTupleMap) {
    super.getRegionAssignments(srcRegionNodes, targRegionNodes, linkTupleMap);
    if (signalId_ != null) {
      targRegionNodes.add(signalId_);
    }
    if (bubbleId_ != null) {
      targRegionNodes.add(bubbleId_);
    }
    if (transFacId_ != null) {
      targRegionNodes.add(transFacId_);
    }
    
    if (toSignalLinkId_ != null) {
      linkTupleMap.put(toSignalLinkId_, new Integer(SOURCE_TARGET));
    }
    if (toBubbleLinkId_ != null) {
      linkTupleMap.put(toBubbleLinkId_, new Integer(TARGET_TARGET));
    }
    if (fromTransFacLinkId_ != null) {
      linkTupleMap.put(fromTransFacLinkId_, new Integer(TARGET_TARGET));
    }
    if (toTargetPosLinkId_ != null) {
      linkTupleMap.put(toTargetPosLinkId_, new Integer(TARGET_TARGET));
    }
    if (toTargetNegLinkId_ != null) {
      linkTupleMap.put(toTargetNegLinkId_, new Integer(TARGET_TARGET));
    }    

    return;
  }  

  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltSignalMotif: (" + super.toString() + ")"  
            + " signalId_ = " + signalId_
            + " toSignalLinkId_ = " + toSignalLinkId_
            + " bubbleId_ = " + bubbleId_
            + " toBubbleLinkId_ = " + toBubbleLinkId_
            + " transFacId_ = " + transFacId_
            + " fromTransFacLinkId_ = " + fromTransFacLinkId_
            + " toTargetPosLinkId_ = " + toTargetPosLinkId_
            + " toTargetNegLinkId_ = " + toTargetNegLinkId_
            + " signalMode_ = " + signalMode_);
  }
  
  /***************************************************************************
  **
  ** Get link count
  */
  
  public int getLinkCount() {
    return ((signalMode_ == SignalBuildInstruction.SWITCH_SIGNAL) ? 5 : 4);
  }
  
 /***************************************************************************
  **
  ** Get given link
  */
  
  public Link getLink(int index) {
    switch (index) {
      case 0:
        return (new Link(sourceId_, signalId_));  
      case 1:
        return (new Link(signalId_, bubbleId_));      
      case 2:
        return (new Link(transFacId_, bubbleId_));     
      case 3:
        return (new Link(bubbleId_, targetId_));    
      case 4:
        if (signalMode_ != SignalBuildInstruction.SWITCH_SIGNAL) {
          throw new IllegalArgumentException();
        }
        // Note 3, 4 are identical (no sign included in a Link)  FIX ME??
        return (new Link(bubbleId_, targetId_));
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get column info; null for already placed columns
  */
  
  protected Integer[] getColumns(Map<String, Integer> columnInfo, Set<String> placed, Set<String> needToPlace) {
    
    Integer[] retval = new Integer[5];

    if (needToPlace.contains(sourceId_) && !placed.contains(sourceId_)) {
      retval[SOURCE_] = columnInfo.get(sourceId_);
    }
    
    if (needToPlace.contains(signalId_) && !placed.contains(signalId_)) {
      retval[SIGNAL_] = columnInfo.get(signalId_);
    }
    
    if (needToPlace.contains(bubbleId_) && !placed.contains(bubbleId_)) {
      retval[BUBBLE_] = columnInfo.get(bubbleId_);
    }    

    if (needToPlace.contains(targetId_) && 
        !placed.contains(targetId_) &&
        !targetId_.equals(sourceId_)) {
      retval[TARGET_] = columnInfo.get(targetId_);
    }    
    
    if (needToPlace.contains(transFacId_) && 
        !placed.contains(transFacId_) &&
        !transFacId_.equals(sourceId_) &&
        !transFacId_.equals(targetId_)) {
      retval[TRANSFAC_] = columnInfo.get(transFacId_);
    }
    
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Generate pattern. No pattern if everybody is placed
  */
  
  protected Pattern genPattern(Integer[] columns) {
    
    //
    // Get minimum, maximum columns:
    //
    
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    int size = columns.length;
    for (int i = 0; i < size; i++) {
      if (columns[i] == null) {
        continue;
      }
      int value = columns[i].intValue();
      if (value < min) {
        min = value;
      }
      if (value > max) {
        max = value;
      }
    }
    
    if ((min == Integer.MAX_VALUE) || (max == Integer.MIN_VALUE)) {
      return (null);
    }
     
    //
    // Figure out the rows we need:
    //

    HashMap<Integer, Integer> numPerCol = new HashMap<Integer, Integer>();
    for (int i = 0; i < NUM_COL_TAGS_; i++) {
      Integer column = columns[i];
      if (column != null) {
        Integer count = numPerCol.get(column);
        if (count == null) {
          numPerCol.put(column, new Integer(1));
        } else {
          numPerCol.put(column, new Integer(count.intValue() + 1));
        }
      }
    }
    int numRow = 1;
    Iterator<Integer> vit = numPerCol.values().iterator();
    while (vit.hasNext()) {
      Integer val = vit.next();
      if (val.intValue() > numRow) {
        numRow = val.intValue();
      }
    }
    
    //
    // Build the pattern
    //
    
    Pattern retval = new Pattern(max - min + 1, numRow);
    if (columns[SOURCE_] != null) {
      Integer count = numPerCol.get(columns[SOURCE_]);
      int row = count.intValue();
      numPerCol.put(columns[SOURCE_], new Integer(row - 1));
      retval.fill(columns[SOURCE_].intValue() - min, numRow - row, sourceId_);
    }
    if (columns[TRANSFAC_] != null) {
      Integer count = numPerCol.get(columns[TRANSFAC_]);
      int row = count.intValue();
      numPerCol.put(columns[TRANSFAC_], new Integer(row - 1));
      retval.fill(columns[TRANSFAC_].intValue() - min, numRow - row, transFacId_);
    }
    if (columns[TARGET_] != null) {
      Integer count = numPerCol.get(columns[TARGET_]);
      int row = count.intValue();
      numPerCol.put(columns[TARGET_], new Integer(row - 1));
      retval.fill(columns[TARGET_].intValue() - min, numRow - row, targetId_);
    }          
    if (columns[SIGNAL_] != null) {
      Integer count = numPerCol.get(columns[SIGNAL_]);
      int row = count.intValue();
      numPerCol.put(columns[SIGNAL_], new Integer(row - 1));
      retval.fill(columns[SIGNAL_].intValue() - min, numRow - row, signalId_);
    }
    if (columns[BUBBLE_] != null) {
      Integer count = numPerCol.get(columns[BUBBLE_]);
      int row = count.intValue();
      numPerCol.put(columns[BUBBLE_], new Integer(row - 1));
      retval.fill(columns[BUBBLE_].intValue() - min, numRow - row, bubbleId_);
    }  
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get row info; null for unplaced nodes
  */
  
  protected Integer[] getRows(PatternGrid grid, Set<String> placed, Set<String> needToPlace) {
    
    Integer[] retval = new Integer[5];

    if (placed.contains(sourceId_)) {
      retval[SOURCE_] = new Integer(grid.getLocation(sourceId_).y);
    }
    
    if (placed.contains(signalId_)) {
      retval[SIGNAL_] = new Integer(grid.getLocation(signalId_).y);
    }
    
    if (placed.contains(bubbleId_)) {
      retval[BUBBLE_] = new Integer(grid.getLocation(bubbleId_).y);
    }    
    
    if (placed.contains(transFacId_)) {
      retval[TRANSFAC_] = new Integer(grid.getLocation(transFacId_).y);
    }
    
    if (placed.contains(targetId_)) {
      retval[TARGET_] = new Integer(grid.getLocation(targetId_).y);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the suggested row.  May be null if nothing suggested.
  */
  
  public Integer getSuggestedRow(Integer[] rowData) {
    if (rowData != null) {
      if (rowData[SOURCE_] != null) {
        return (new Integer(rowData[SOURCE_].intValue() - 1));
      } else if (rowData[TRANSFAC_] != null) {
        return (rowData[TRANSFAC_]);
      } else if (rowData[TARGET_] != null) {
        return (new Integer(rowData[TARGET_].intValue() - 1));
      } else if (rowData[SIGNAL_] != null) {
        return (new Integer(rowData[SIGNAL_].intValue() - 1));
      } else if (rowData[BUBBLE_] != null) {
        return (new Integer(rowData[BUBBLE_].intValue() - 1));
      }
    }
    return (null);
  }    
}
