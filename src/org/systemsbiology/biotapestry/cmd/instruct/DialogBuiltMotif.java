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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.util.Pattern;

/***************************************************************************
**
** Node/Link construct for dialog-built networks
*/
  
public abstract class DialogBuiltMotif {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static int SOURCE_SOURCE = 0;
  public final static int SOURCE_TARGET = 1;
  public final static int TARGET_TARGET = 2;
  public final static int TARGET_SOURCE = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  protected String sourceId_;
  protected String targetId_;  
 
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltMotif(String sourceId, String targetId) {
    sourceId_ = sourceId;
    targetId_ = targetId;    
  }

  /***************************************************************************
  **
  ** Get the source ID
  */
  
  public String getSourceId() {
    return (sourceId_);
  }
     
  /***************************************************************************
  **
  ** Set the source ID
  */
  
  public void setSourceId(String sourceId) {
    sourceId_ = sourceId;
    return;
  }

  /***************************************************************************
  **
  ** Answer if we have a target
  */
  
  public boolean hasTarget() {
    return (targetId_ != null);
  }  
  
  /***************************************************************************
  **
  ** Get the target ID
  */
  
  public String getTargetId() {
    return (targetId_);
  }
  
  /***************************************************************************
  **
  ** Set the target ID
  */
  
  public void setTargetId(String targetId) {
    targetId_ = targetId;
    return;
  }

  /***************************************************************************
  **
  ** Get display ordering
  */
  
  public abstract String[] ordering();
  
  /***************************************************************************
  **
  ** Get link count
  */
  
  public abstract int getLinkCount(); 
  
  /***************************************************************************
  **
  ** Get given link
  */
  
  public abstract Link getLink(int index);   
  
  /***************************************************************************
  **
  ** Get empty copy
  */
  
  public abstract DialogBuiltMotif emptyCopy();  

  
  /***************************************************************************
  **
  ** Get placement info.  Null if nothing needs to be done.
  */
  
  public Placement getPlacementInfo(Map<String, Integer> columnInfo, Set<String> placed, Set<String> needToPlace, PatternGrid grid) {

    //
    // Generate a pattern of nodes that need placement, and a preferred row if somebody has been placed.
    //

    Integer[] colData = getColumns(columnInfo, placed, needToPlace);
    Pattern pat = genPattern(colData);
    if (pat == null) {
      return (null);
    }
    
    //
    // Get left column:
    //
    
    int leftColumn = Integer.MAX_VALUE;
    int size = colData.length;
    for (int i = 0; i < size; i++) {
      if (colData[i] == null) {
        continue;
      }
      int value = colData[i].intValue();
      if (value < leftColumn) {
        leftColumn = value;
      }
    }
    
    //
    // Return a suggested row
    //
    
    Integer[] rowData = null;
    if (nodeIsPlaced(colData)) {
      rowData = getRows(grid, placed, needToPlace);
    }    
    Integer suggestedRow = getSuggestedRow(rowData);
    
    return (new Placement(pat, suggestedRow, new Integer(leftColumn)));
  }
  
  /***************************************************************************
  **
  ** Get the suggested row.  May be null if nothing suggested.  Overridden for
  ** more complex motifs (e.g. signals)
  */
  
  public Integer getSuggestedRow(Integer[] rowData) {
    //
    // For now, just use first row encountered.
    //
    Integer suggestedRow = null;
    if (rowData != null) {
      int size = rowData.length;
      for (int i = 0; i < size; i++) {
        if (rowData[i] == null) {
          continue;
        }
        suggestedRow = rowData[i];
        break;
      }
    }
    
    return (suggestedRow);
  }  

  /***************************************************************************
  **
  ** Answer if a node is placed
  */
  
  protected boolean nodeIsPlaced(Integer[] columns) {
    for (int i = 0; i < columns.length; i++) {
      if (columns[i] == null) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get column info; null for already placed columns
  */
  
  protected Integer[] getColumns(Map<String, Integer> columnInfo, Set<String> placed, Set<String> needToPlace) {
    
    Integer[] retval = new Integer[2];

    if (needToPlace.contains(sourceId_) && !placed.contains(sourceId_)) {
      retval[0] = columnInfo.get(sourceId_);
    }
    
    if (needToPlace.contains(targetId_) && !placed.contains(targetId_)) {
      retval[1] = columnInfo.get(targetId_);
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
    // Build the pattern
    //
    
    Pattern retval = new Pattern(max - min + 1, 1);
    if (columns[0] != null) {
      retval.fill(columns[0].intValue() - min, 0, sourceId_);
    }
    if (columns[1] != null) {
      retval.fill(columns[1].intValue() - min, 0, targetId_);
    }    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get row info; null for unplaced nodes
  */
  
  protected Integer[] getRows(PatternGrid grid, Set<String> placed, Set<String> needToPlace) {
    
    Integer[] retval = new Integer[2];

    if (placed.contains(sourceId_)) {
      retval[0] = new Integer(grid.getLocation(sourceId_).y);
    }
    
    if (placed.contains(targetId_)) {
      retval[1] = new Integer(grid.getLocation(targetId_).y);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find out what nodes and links exist in the motif
  */

  public void getExistingNodesAndLinks(Set<String> nodes, Set<String> links) {
    if (sourceId_ != null) {
      nodes.add(sourceId_);
    }
    if (targetId_ != null) {
      nodes.add(targetId_);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Assign the nodes to source and target regions
  */

  public void getRegionAssignments(Set<String> srcRegionNodes, Set<String> targRegionNodes, Map<String, Integer> linkTupleMap) {
    if (sourceId_ != null) {
      srcRegionNodes.add(sourceId_);
    }
    if (targetId_ != null) {
      targRegionNodes.add(targetId_);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltMotif:"
            + "sourceId = " + sourceId_
            + "targetId = " + targetId_);            
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class Placement {
    public Pattern pattern;
    public Integer suggestedRow;
    public Integer leftColumn;
 
    public Placement(Pattern pattern, Integer suggestedRow, Integer leftColumn) {
      this.pattern = pattern;
      this.suggestedRow = suggestedRow;
      this.leftColumn = leftColumn;
    }
  }  
  
  /***************************************************************************
  **
  ** Just compares the basics.  Child class may be equal even though they
  ** are different.
  */
  
  
  public static class DBMComparator implements Comparator<DialogBuiltMotif> {
    public Pattern pattern;
    public Integer suggestedRow;
    public Integer leftColumn;
 
    public int compare(DialogBuiltMotif firstDbm, DialogBuiltMotif secondDbm) {
      int fComp = firstDbm.getSourceId().compareTo(secondDbm.getSourceId());
      if (fComp != 0) {
        return (fComp);
      }
      boolean fht = firstDbm.hasTarget();
      boolean sht = secondDbm.hasTarget();
      if (fht == sht) {
        if (!fht) {
          return (0);
        } else {
          return (firstDbm.getTargetId().compareTo(secondDbm.getTargetId()));
        }
      } else {
        return (fht) ? -1 : 1;
      }
    }
  }   
}
