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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.systemsbiology.biotapestry.cmd.flow.move.RunningMove;
import org.systemsbiology.biotapestry.db.DataAccessContext;


/****************************************************************************
**
** Choose the best candidate for an intersection
*/

public class IntersectionChooser {
 
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

  private boolean legacyMode_;
  private Layout layout_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */
  
  public IntersectionChooser(boolean legacyMode, DataAccessContext rcx) {
    legacyMode_ = legacyMode;
    layout_ = rcx.getLayout();
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Handle intersection choosing
  */
  
  public Intersection intersectionRanker(List<Intersection> newSelections) {
    if (legacyMode_) {
      return (legacyIntersectionRanker(newSelections));
    } else {
      throw new IllegalStateException();
    }
  }
   
  /***************************************************************************
  **
  ** Handle augmented intersection choosing
  */
  
  public Intersection.AugmentedIntersection selectionRanker(List<Intersection.AugmentedIntersection> newSelections) {
    if (legacyMode_) {
      return (legacySelectionRanker(newSelections));
    } else {
      return (smarterSelectionRanker(newSelections, false));
    }
  }
  
  /***************************************************************************
  **
  ** Handle running move
  */
  
  public RunningMove runningMoveRanker(List<RunningMove> newMoves) {
    if (legacyMode_) {
      return (legacyMoveRanker(newMoves));
    } else {
      return (smarterMoveRanker(newMoves));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Handle selection semantics
  */
  
  private Intersection.AugmentedIntersection legacySelectionRanker(List<Intersection.AugmentedIntersection> newSelections) {
    if ((newSelections == null) || newSelections.isEmpty()) {
      return (null);
    }
    return (newSelections.get(0));
  }  
  
  /***************************************************************************
  **
  ** Handle selection semantics
  */
  
  private Intersection.AugmentedIntersection smarterSelectionRanker(List<Intersection.AugmentedIntersection> newSelections, boolean usingForPads) {
    if ((newSelections == null) || newSelections.isEmpty()) {
      return (null);
    }
    
    //
    // The current nasty problem is where a tiny direct segment cannot be split with a
    // corner point if it is over a node intersection.  If this is the case, return the
    // segment first!
    //    

    Intersection.AugmentedIntersection firstAug = newSelections.get(0);
    
    int numCand = newSelections.size();
    if (numCand == 1) {
      return (firstAug);
    }
    
    Intersection.AugmentedIntersection secondAug = newSelections.get(1);
    if (((firstAug.type == Intersection.IS_GENE) || 
         (firstAug.type == Intersection.IS_NODE)) &&
        (secondAug.type == Intersection.IS_LINK)) {
      // We do not override pad candidates:
      if (usingForPads) {
        if (firstAug.intersect.getPadCand() != null) {
          return (firstAug);
        }
      }
      // Only deal with links from the node:
      Intersection selection = secondAug.intersect;
      LinkProperties lp = layout_.getLinkProperties(selection.getObjectID());
      String src = lp.getSourceTag();
      if (!firstAug.intersect.getObjectID().equals(src)) {
        return (firstAug);
      }
      LinkSegmentID[] segIDs = selection.segmentIDsFromIntersect();
      if ((segIDs != null) && (segIDs.length == 1)) {          
        LinkSegmentID nextID = segIDs[0];
        if (nextID.isDirect()) {
          return (secondAug);
        }
      }            
    }     
    return (firstAug);
  }  

  /***************************************************************************
  **
  ** Handle selection semantics
  */
  
  private Intersection legacyIntersectionRanker(List<Intersection> newSelections) {
    if ((newSelections == null) || newSelections.isEmpty()) {
      return (null);
    }
    return (newSelections.get(0));
  }  

  /***************************************************************************
  **
  ** Handle selection semantics
  */
  
  private RunningMove legacyMoveRanker(List<RunningMove> newMoves) {
    if ((newMoves == null) || newMoves.isEmpty()) {
      return (null);
    }
    return (newMoves.get(0));
  }  
  
  /***************************************************************************
  **
  ** Handle move rankings
  */
  
  private RunningMove smarterMoveRanker(List<RunningMove> newMoves) {
    if ((newMoves == null) || newMoves.isEmpty()) {
      return (null);
    }
    RunningMove firstMove = newMoves.get(0);
    
    int numCand = newMoves.size();
    if (numCand == 1) {
      return (firstMove);
    }
    
    if ((firstMove.notPermitted) || (firstMove.type != RunningMove.MoveType.INTERSECTIONS)) {
      return (firstMove);
    }
       
    //
    // If the first move is for multiple guys (i.e. dragging a selection set), just choose it:
    //
    
    if (firstMove.toMove.length > 1) {
      return (firstMove);
    }
   
    //
    // If the first option is not a link, just return it:
    //
    
    if (firstMove.augInterType != Intersection.IS_LINK) {
      return (firstMove);         
    } 
    
    //
    // PAY ATTENTION!  Now that we don't take just the first link intersection, BRANCH POINTS GENERATE
    // MULTIPLE INTERSECTIONS!!!!
    //

    //
    // For the moment, just do something fancy with the multiple link candidates.  Bin them
    // up by identical link tree:
    //
    
    HashMap<String, List<RunningMove>> candBySource = new HashMap<String, List<RunningMove>>();
    HashSet<String> taggedBySource = new HashSet<String>();
    
    for (int i = 0; i < numCand; i++) {
      RunningMove nextMove = newMoves.get(i);
      if (nextMove.augInterType != Intersection.IS_LINK) {
        continue;
      }
      Intersection selection = nextMove.toMove[0];
      LinkProperties lp = layout_.getLinkProperties(selection.getObjectID());
      String src = lp.getSourceTag();
      LinkSegmentID[] segIDs = selection.segmentIDsFromIntersect();
      if (segIDs != null) {          
        for (int j = 0; j < segIDs.length; j++) {
          LinkSegmentID nextID = segIDs[j];
          if (nextID.isTaggedWithEndpoint()) {
            taggedBySource.add(src);
            break;
          }
        }
      }      
      List<RunningMove> bySrc = candBySource.get(src);
      if (bySrc == null) {
        bySrc = new ArrayList<RunningMove>();
        candBySource.put(src, bySrc);
      }
      bySrc.add(nextMove);
    }
     
    //
    // If we have more than one tree, and fewer have a tagged endpoint, return one. 
    //

    int cbs = candBySource.size();
    int tbs = taggedBySource.size();
    if ((cbs > 1) && (tbs > 0) && (tbs < cbs)) {
      String taggedSrc = taggedBySource.iterator().next();
      List<RunningMove> bySrc = candBySource.get(taggedSrc);
      int numMov = bySrc.size();
      for (int i = 0; i < numMov; i++) {  
        RunningMove nextMove = bySrc.get(i);     
        Intersection selection = nextMove.toMove[0];
        LinkSegmentID[] segIDs = selection.segmentIDsFromIntersect();     
        for (int j = 0; j < segIDs.length; j++) {
          LinkSegmentID nextID = segIDs[j];
          if (nextID.isTaggedWithEndpoint()) {
            return (nextMove);
          }
        }
      }
    }
    
    //
    // If we have just one tree, and one of the endpoints is tagged, return a tagged:
    //
    
    if ((cbs == 1) && (tbs == 1)) {
      String taggedSrc = taggedBySource.iterator().next();
      List<RunningMove> bySrc = candBySource.get(taggedSrc);
      int numMov = bySrc.size();
      for (int i = 0; i < numMov; i++) {  
        RunningMove nextMove = bySrc.get(i);     
        Intersection selection = nextMove.toMove[0];
        LinkSegmentID[] segIDs = selection.segmentIDsFromIntersect();
        // Fix for BT-10-24-12:2
        if (segIDs != null) {
          for (int j = 0; j < segIDs.length; j++) {
            LinkSegmentID nextID = segIDs[j];
            if (nextID.isTaggedWithEndpoint()) {
              return (nextMove);
            }
          }
        }
      }
    }
    
    //
    // Future improvements would show a preference for shorter segments over longer.  Also
    // show a pref for smaller nodes over larger, and for short segments instead of nodes....
    //
     
    //
    // Fixme: Module link or corner on top of net module should win! 
    //
    
    //
    // Two links, take the one with the smallest intersection distance (need to make sure intersections
    // are being tagged with distance now!
    //
    
    //
    // Regular link corner point should beat out a module link segment
    //
    
    return (firstMove);
    
  }  
}
