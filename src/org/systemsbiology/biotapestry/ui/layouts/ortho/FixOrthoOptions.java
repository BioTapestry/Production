/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Vector2D;


/****************************************************************************
**
** Strategies for fixing non-ortho segments
*/

public class FixOrthoOptions  {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int STOP_RECURSION_   = -1;
  private static final int NO_RECURSION_   = 0;
  private static final int IS_RECURSION_   = 1;
  private static final int UP_RECURSION_   = 2;
  private static final int DOWN_RECURSION_ = 3;
  
  private static final int STRATEGY_LIMIT_ = 200;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<TreeStrategy> strats_;
  private FixOrthoTreeInfo foti_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */
  
  public FixOrthoOptions(LinkSegmentID segID, FixOrthoTreeInfo foti) {
      
    strats_ = new ArrayList<TreeStrategy>();
    foti_ = foti;
    
    PointDOFsPerSeg pdps = (PointDOFsPerSeg)foti.ptDofs.get(segID);
    if ((pdps == null) || (pdps.p0d == null)) {
      return;
    }
    
    PointDegreesOfFreedom p0d = pdps.p0d;
    PointDegreesOfFreedom p1d = pdps.p1d;
      
    //
    // If we have required departure or arrival directions, the
    // simple orthogonalization approaches we can apply will 
    // NEVER work unless the directions are consistent with the
    // X or Y components of the overall travel vector!
    //
   
    if (p0d.requiredDir != null) {
      Vector2D overall = new Vector2D(p0d.point, p1d.point);
      double dot = overall.dot(p0d.requiredDir);
      if (dot < 0.0) {
        return;
      }
    }
      
    if (p1d.requiredDir != null) {
      Vector2D overall = new Vector2D(p0d.point, p1d.point);
      double dot = overall.dot(p1d.requiredDir);
      if (dot < 0.0) {
        return;
      }
    }
    
    //
    // We want to make sure that the recursive strategies don't
    // crowd out the non-recursive ones, given that recursive ones
    // undergo combinatorial explosion.  But the follwing is somewhat
    // wasteful, in that the non-recursive ones get generated twice:
    //
    
    buildRecursiveStrategy(segID, strats_, STOP_RECURSION_);   
    buildRecursiveStrategy(segID, strats_, NO_RECURSION_);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the available strategies:
  */
  
  public Iterator<TreeStrategy> getStrategies() {
    return (strats_.iterator());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Build a strategy using recursion.
  **
  ** FIXME! Make the granularity of the orthogonality variable!  For large scale
  ** inter-module links, want to have e.g. links fall every 100 units, not 10.   
  **
  */
  
  private boolean buildRecursiveStrategy(LinkSegmentID segID, List<TreeStrategy> strats, int recursionType) {
 
    PointDOFsPerSeg pdps = (PointDOFsPerSeg)foti_.ptDofs.get(segID);
    if ((pdps == null) || (pdps.p0d == null) || (pdps.p1d == null)) {
      return (false);
    }
    
    //
    // Put a cap on the number of strategies that can be generated.  This is not perfect,
    // in that it will allow some overflow, and also terminate the generation of a perfectly
    // valid strategy, but more precise control is probably not worth the effort:
    //
    
    if (strats.size() > STRATEGY_LIMIT_) {
      return (false);
    }
    
      
    PointDegreesOfFreedom p0d = pdps.p0d;
    PointDegreesOfFreedom p1d = pdps.p1d;
    
    //
    // Non-null treeStrat means we are in recursion.  If we are not working on the
    // original segment, we restrict the strategies available to us.
    //
    
    boolean inRecursion = (recursionType != NO_RECURSION_) && (recursionType != STOP_RECURSION_);
      
    //
    // Possible strategies for getting a diagonal segment to ortho:  moving one or both
    // endpoints, or creating one or more split points, or splitting and moving old points.
    // THIS LIST IS NOT EXHAUSTIVE!  e.g. could create two split points and move end points,
    // but trying to keep this under control at the moment:
    //
    
    boolean retval = false;
    
    //
    // Moving one of the existing points:
    //
     
    boolean canDo = generateSingleMoveStrategies(segID, p0d, p1d, strats, recursionType);
    retval = retval || canDo;
  
   
    //
    // We will allow double moves only on the first level, not in recursion.  Otherwise, the attempt
    // to move backward to move p1 makes no sense while moving upward; p0 has already been adjusted!
    //
    
    if (!inRecursion) {
     //
     // Moving both existing points:
     //
      canDo = generateDoubleMoveStrategies(segID, p0d, p1d, strats, recursionType);
      retval = retval || canDo;
    } 

    //
    // Create a single split point:
    //
    
    canDo = generateSingleSplitStrategies(segID, p0d, p1d, strats);
    retval = retval || canDo;
   
    //
    // Create a single split point, then move one existing point
    //
    
    canDo = generateSingleSplitAndMoveStrategies(segID, p0d, p1d, strats, recursionType);
    retval = retval || canDo;
    
    //
    // Create two split points:
    //
      
    canDo = generateDoubleSplitStrategies(segID, p0d, p1d, strats, recursionType);
    retval = retval || canDo;
    return (retval);
  }
 
  /***************************************************************************
  **
  **
  ** If the non-moving point has a required in/out direction (e.g. launch/land pads) then we can
  **  only generate a strategy that conforms.  X_AXIS-> checking NORTH & SOUTH!
  **
  */
  
  private boolean nonMoveSatisfied(Vector2D nonMoveRequiredVec, int axis) {   
    boolean nonMoveSatisfiedAlongY = true;
    boolean nonMoveSatisfiedAlongX = true;
    if (nonMoveRequiredVec != null) {
      int canon = nonMoveRequiredVec.canonicalDir();
      nonMoveSatisfiedAlongY = ((canon == Vector2D.NORTH) || (canon == Vector2D.SOUTH));
      nonMoveSatisfiedAlongX = ((canon == Vector2D.EAST) || (canon == Vector2D.WEST));
    }   
    // This will not work if required landing direction (if any) is not satisfied:   
    return ((axis == DegreeOfFreedom.X_AXIS) ? nonMoveSatisfiedAlongY : nonMoveSatisfiedAlongX);  
  }
  
  /***************************************************************************
  **
  ** Strategies for single moves
  */
  
  private boolean buildAMove(LinkSegmentID segID, PointDegreesOfFreedom p0d, PointDegreesOfFreedom p1d, 
                             Vector2D nonMoveRequiredVec, DegreeOfFreedom moveDOF,
                             LinkSegStrategy.Goal reqArg1, LinkSegStrategy.Goal reqArg2, int stratMove, List<TreeStrategy> strats, int recursionType) {
 
    // This will not work if required landing direction (if any) is not satisfied:

    if ((moveDOF.getType() == DegreeOfFreedom.FIXED) || !nonMoveSatisfied(nonMoveRequiredVec, moveDOF.getAxis())) {
      return (false);
    }
  
    ArrayList<TreeStrategy> allMyStrats = new ArrayList<TreeStrategy>();
    TreeStrategy useStrat = new TreeStrategy();
  
    // Handles unconditional and first step of recursive conditional:
    
    ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
    if ((recursionType != STOP_RECURSION_) || (moveDOF.getType() != DegreeOfFreedom.CONDITIONAL))  {
      reqs.add(new LinkSegStrategy.Requirement(reqArg1, LinkSegStrategy.Requirement.ReqType.EQUALS, reqArg2));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, stratMove, reqs, p0d, p1d, false));
      allMyStrats.add(useStrat);
    }
    if ((recursionType != STOP_RECURSION_) && (moveDOF.getType() == DegreeOfFreedom.CONDITIONAL)) {
      //
      // Point can only be moved if the conditional DOF allows it!
      //
      LinkSegmentID plisd = (LinkSegmentID)foti_.parentIDs.get(segID);
      Iterator<LinkSegmentID> dit = moveDOF.getDependencies();    
      while (dit.hasNext()) {
        LinkSegmentID depend = dit.next();
        ArrayList<TreeStrategy> recursionStrats = new ArrayList<TreeStrategy>();
        int recursion = ((plisd == null) || (plisd.equals(depend))) ? UP_RECURSION_ : DOWN_RECURSION_;           
        boolean canDo = buildRecursiveStrategy(depend, recursionStrats, recursion);
        if (!canDo) {
          return (false);
        }
        if (!mergeConditionals(allMyStrats, recursionStrats)) {
          return (false);
        }
      }      
    }
    
    if ((strats.size() + allMyStrats.size()) > STRATEGY_LIMIT_) {
      return (false);
    }
    
    strats.addAll(allMyStrats);
    return (true);   
  }
    
  /***************************************************************************
  **
  ** Used to merge recursive strategy options
  */
  
  private boolean mergeConditionals(List<TreeStrategy> conditionalStrats, List<TreeStrategy> recursionStrats) {
    ArrayList<TreeStrategy> nextCStrats = new ArrayList<TreeStrategy>();
    int numCS = conditionalStrats.size();
    int numRS = recursionStrats.size();
    if ((numCS * numRS) > STRATEGY_LIMIT_) {
      return (false);
    }
    for (int i = 0; i < numCS; i++) {
      TreeStrategy cStrat = conditionalStrats.get(i);    
      for (int j = 0; j < numRS; j++) {
        TreeStrategy rStrat = recursionStrats.get(j);
        TreeStrategy augCStrat = cStrat.clone();
        augCStrat.mergeSegStrategies(rStrat);
        nextCStrats.add(augCStrat);
      }
    }
    conditionalStrats.clear();
    conditionalStrats.addAll(nextCStrats);
    return (true);
  }
  
  /***************************************************************************
  **
  ** Strategies for single moves
  */
  
  private boolean generateSingleMoveStrategies(LinkSegmentID segID, PointDegreesOfFreedom p0d, 
                                               PointDegreesOfFreedom p1d, List<TreeStrategy> strats, 
                                               int recursionType) {
 
 
    if (recursionType == IS_RECURSION_) {
      throw new IllegalArgumentException();
    }
    
    boolean retval = false;
    boolean canDo;
    
    if ((recursionType == STOP_RECURSION_) || (recursionType == NO_RECURSION_) || (recursionType == UP_RECURSION_)) {
      // Move POx to current P1x
      canDo = buildAMove(segID, p0d, p1d, p1d.requiredDir, p0d.xDOF,
                         LinkSegStrategy.Goal.P_0_X_NEW, LinkSegStrategy.Goal.P_1_X, 
                         LinkSegStrategy.POINT_0_MOVE_X, strats, recursionType);
      retval = retval || canDo;
     // Move POy to current P1y
      canDo = buildAMove(segID, p0d, p1d, p1d.requiredDir, p0d.yDOF,
                         LinkSegStrategy.Goal.P_0_Y_NEW, LinkSegStrategy.Goal.P_1_Y,
                         LinkSegStrategy.POINT_0_MOVE_Y, strats, recursionType);
      retval = retval || canDo;
    }
    
    if ((recursionType == STOP_RECURSION_) || (recursionType == NO_RECURSION_) || (recursionType == DOWN_RECURSION_)) {
      // Move P1x to current P0x
      canDo = buildAMove(segID, p0d, p1d, p0d.requiredDir, p1d.xDOF,
                         LinkSegStrategy.Goal.P_1_X_NEW, LinkSegStrategy.Goal.P_0_X, 
                         LinkSegStrategy.POINT_1_MOVE_X, strats, recursionType);
      retval = retval || canDo;   
      // Move P1y to current P0y
      canDo = buildAMove(segID, p0d, p1d, p0d.requiredDir, p1d.yDOF,
                         LinkSegStrategy.Goal.P_1_Y_NEW, LinkSegStrategy.Goal.P_0_Y, 
                         LinkSegStrategy.POINT_1_MOVE_Y, strats, recursionType);
      retval = retval || canDo;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Strategies for double moves
  */
 
  
  
  private boolean buildDoubleMove(LinkSegmentID segID, PointDegreesOfFreedom p0d, PointDegreesOfFreedom p1d, 
                                  DegreeOfFreedom move0DOF, DegreeOfFreedom move1DOF,
                                  LinkSegStrategy.Goal reqArg1, LinkSegStrategy.Goal reqArg2, 
                                  int stratMove, List<TreeStrategy> strats, int recursionType) { 
    //
    // If either end is fixed, we cannot do it:
    //
    if ((move0DOF.getType() == DegreeOfFreedom.FIXED) || (move1DOF.getType() == DegreeOfFreedom.FIXED)) {
      return (false);
    }
     
    ArrayList<TreeStrategy> allMyStrats = new ArrayList<TreeStrategy>();
    TreeStrategy useStrat = new TreeStrategy();
    
    ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
    // Handles unconditional and first step of recursive conditional:
    if ((recursionType != STOP_RECURSION_) || 
         ((move0DOF.getType() != DegreeOfFreedom.CONDITIONAL) && 
          (move1DOF.getType() != DegreeOfFreedom.CONDITIONAL)))  {
      reqs.add(new LinkSegStrategy.Requirement(reqArg1, LinkSegStrategy.Requirement.ReqType.EQUALS, reqArg2));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, stratMove, reqs, p0d, p1d, false)); 
      allMyStrats.add(useStrat);
    }
    
    if (recursionType != STOP_RECURSION_) {
      if (move0DOF.getType() == DegreeOfFreedom.CONDITIONAL) {
        LinkSegmentID parentID = (LinkSegmentID)foti_.parentIDs.get(segID);
        Iterator<LinkSegmentID> dit = move0DOF.getDependencies();
        while (dit.hasNext()) {
          LinkSegmentID depend = dit.next();
          ArrayList<TreeStrategy> recursionStrats = new ArrayList<TreeStrategy>();
          // Siblings need down recursion, not up recursion!
          int useRec = (depend.equals(parentID)) ? UP_RECURSION_ : DOWN_RECURSION_;
          boolean canDo = buildRecursiveStrategy(depend, recursionStrats, useRec);
          if (!canDo) {
            return (false);
          }
          mergeConditionals(allMyStrats, recursionStrats);
        }
      } 
      if (move1DOF.getType() == DegreeOfFreedom.CONDITIONAL) {
        Iterator<LinkSegmentID> dit = move1DOF.getDependencies();

        while (dit.hasNext()) {
          LinkSegmentID depend = dit.next();
          ArrayList<TreeStrategy> recursionStrats = new ArrayList<TreeStrategy>();
          boolean canDo = buildRecursiveStrategy(depend, recursionStrats, DOWN_RECURSION_);
          if (!canDo) {
            return (false);
          }
          mergeConditionals(allMyStrats, recursionStrats);
        }
      }
      if ((strats.size() + allMyStrats.size()) > STRATEGY_LIMIT_) {
        return (false);
      }
    }

    strats.addAll(allMyStrats);
    return (true);   
  }
   
  /***************************************************************************
  **
  ** Strategies for double moves
  */
  
  private boolean generateDoubleMoveStrategies(LinkSegmentID segID, PointDegreesOfFreedom p0d, 
                                               PointDegreesOfFreedom p1d, List<TreeStrategy> strats, int recursionType) { 
    boolean retval = false;    
    boolean canDo = buildDoubleMove(segID, p0d, p1d, 
                                   p0d.xDOF, p1d.xDOF,
                                   LinkSegStrategy.Goal.P_0_X_NEW, LinkSegStrategy.Goal.P_1_X_NEW, 
                                   (LinkSegStrategy.POINT_0_MOVE_X | LinkSegStrategy.POINT_1_MOVE_X), 
                                   strats, recursionType);            
    retval = retval || canDo;
    
    canDo = buildDoubleMove(segID, p0d, p1d, 
                            p0d.yDOF, p1d.yDOF,
                            LinkSegStrategy.Goal.P_0_Y_NEW, LinkSegStrategy.Goal.P_1_Y_NEW, 
                            (LinkSegStrategy.POINT_0_MOVE_Y | LinkSegStrategy.POINT_1_MOVE_Y), 
                            strats, recursionType); 
    retval = retval || canDo;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Strategies for single splits
  */
  
  private boolean generateSingleSplitStrategies(LinkSegmentID segID, PointDegreesOfFreedom p0d, 
                                                PointDegreesOfFreedom p1d, List<TreeStrategy> strats) { 
      
    boolean retval = false;
     ArrayList<TreeStrategy> allMyStrats = new ArrayList<TreeStrategy>();
    
    //
    // Single split strategies do not require any concessions from the upstream or downstream segments, just
    // that required directions are OK with the split type!
    //

    // This split point goes y dir, then x dir.  Only OK if required dirs are OK with it:  
    if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.X_AXIS) &&
        nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.Y_AXIS)) {
      TreeStrategy useStrat = new TreeStrategy();     
      ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_0_X));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_1_Y));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, (LinkSegStrategy.CREATE_SPLIT_0), reqs, p0d, p1d, false));
      retval = true;
      allMyStrats.add(useStrat);
    }
   
    // This split point goes x dir, then y dir.  Only OK if required dirs are OK with it:  
    
    if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.Y_AXIS) &&     
        nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.X_AXIS)) {
      TreeStrategy useStrat = new TreeStrategy();
      ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_1_X));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_0_Y));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, (LinkSegStrategy.CREATE_SPLIT_0), reqs, p0d, p1d, false));
      retval = true;
      allMyStrats.add(useStrat);
    }
    if ((strats.size() + allMyStrats.size()) > STRATEGY_LIMIT_) {
      return (false);
    }
    strats.addAll(allMyStrats);
    return (retval); 
  }
  
  /***************************************************************************
  **
  ** Low-level bundle ops
  */
  
  private void bundleSplitAndMove(TreeStrategy treeStrat, LinkSegmentID segID, 
                                  PointDegreesOfFreedom p0d, PointDegreesOfFreedom p1d,
                                  LinkSegStrategy.Goal req1A, LinkSegStrategy.Goal req1B, LinkSegStrategy.Goal req2A, LinkSegStrategy.Goal req2B, int strat) { 
    ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
    reqs.add(new LinkSegStrategy.Requirement(req1A, LinkSegStrategy.Requirement.ReqType.EQUALS, req1B));
    reqs.add(new LinkSegStrategy.Requirement(req2A, LinkSegStrategy.Requirement.ReqType.EQUALS, req2B));
    treeStrat.addSegStrategy(new LinkSegStrategy(segID, strat, reqs, p0d, p1d, false));
    return;
  }
  
  /***************************************************************************
  **
  ** Strategies for single splits with moving one of the points
  */
  
  private boolean generateSingleSplitAndMoveStrategies(LinkSegmentID segID, PointDegreesOfFreedom p0d,
                                                       PointDegreesOfFreedom p1d, List<TreeStrategy> strats, int recursionType) { 
     
    
    boolean inRecursion = (recursionType != NO_RECURSION_) && (recursionType != STOP_RECURSION_);
    if (inRecursion) {
      // FIX ME! FIX ME!  This strategy is not yet implemented for recursion!  Fix this!
      return (false);
    }
    
    boolean retval = false;    
    ArrayList<TreeStrategy> allMyStrats = new ArrayList<TreeStrategy>();
  
    // This requires that pt1 can move in Y, and that pt0 allows a vertical departure:  
    if ((p1d.yDOF.getType() != DegreeOfFreedom.FIXED) && (p1d.yDOF.getType() != DegreeOfFreedom.CONDITIONAL)) {
      if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.X_AXIS)) {
        retval = true;
        TreeStrategy useStrat = new TreeStrategy();
        bundleSplitAndMove(useStrat, segID, p0d, p1d,
                           LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Goal.P_0_X, 
                           LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Goal.P_1_Y_NEW, 
                           (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.POINT_1_MOVE_Y));
        allMyStrats.add(useStrat);
      }
    }
    
    // This requires that pt0 can move in Y, and that pt1 allows a vertical arrival:  
    if ((p0d.yDOF.getType() != DegreeOfFreedom.FIXED) && (p0d.yDOF.getType() != DegreeOfFreedom.CONDITIONAL)) {
      if (nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.X_AXIS)) {   
        retval = true;
        TreeStrategy useStrat = new TreeStrategy();
        bundleSplitAndMove(useStrat, segID, p0d, p1d,
                           LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Goal.P_1_X, 
                           LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Goal.P_0_Y_NEW, 
                           (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.POINT_0_MOVE_Y));
        allMyStrats.add(useStrat);
      }
    }

    // This requires that pt1 can move in X, and that pt0 allows a horizontal departure:  
    if ((p1d.xDOF.getType() != DegreeOfFreedom.FIXED) && (p1d.xDOF.getType() != DegreeOfFreedom.CONDITIONAL)){
      if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.Y_AXIS)) {       
        retval = true;
        TreeStrategy useStrat = new TreeStrategy();
        bundleSplitAndMove(useStrat, segID, p0d, p1d,
                           LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Goal.P_1_X_NEW, 
                           LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Goal.P_0_Y, 
                           (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.POINT_1_MOVE_X));
        allMyStrats.add(useStrat);        
      }
    }
    
    // This requires that pt0 can move in X, and that pt1 allows a horizontal arrival:  
    if ((p0d.xDOF.getType() != DegreeOfFreedom.FIXED) && (p0d.xDOF.getType() != DegreeOfFreedom.CONDITIONAL)) {
      if (nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.Y_AXIS)) {      
        retval = true;
        TreeStrategy useStrat = new TreeStrategy();
        bundleSplitAndMove(useStrat, segID, p0d, p1d,
                           LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Goal.P_0_X_NEW, 
                           LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Goal.P_1_Y, 
                           (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.POINT_0_MOVE_X));
        allMyStrats.add(useStrat);
      }
    }
    if ((strats.size() + allMyStrats.size()) > STRATEGY_LIMIT_) {
      return (false);
    }
    strats.addAll(allMyStrats);
    return (retval);
  }

 /***************************************************************************
  **
  ** Strategies for double splits
  */
  
  private boolean generateDoubleSplitStrategies(LinkSegmentID segID, PointDegreesOfFreedom p0d, 
                                                PointDegreesOfFreedom p1d, List<TreeStrategy> strats, int recursionType) { 
  
    boolean retval = false;
    ArrayList<TreeStrategy> allMyStrats = new ArrayList<TreeStrategy>();
    boolean varyMoves = (recursionType == STOP_RECURSION_);
    
    if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.X_AXIS) &&
        nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.X_AXIS)) {
      TreeStrategy useStrat = new TreeStrategy();
      // This double split goes vertical, horizontal, vertical:
      ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_0_X));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.S_1_Y));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_1_X, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_1_X));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.CREATE_SPLIT_1), reqs, p0d, p1d, varyMoves));
      retval = true;
      allMyStrats.add(useStrat);
    }
    
    if (nonMoveSatisfied(p0d.requiredDir, DegreeOfFreedom.Y_AXIS) &&
        nonMoveSatisfied(p1d.requiredDir, DegreeOfFreedom.Y_AXIS)) {
      TreeStrategy useStrat = new TreeStrategy();
      // This double split goes horizontal, vertical, horizontal:
      ArrayList<LinkSegStrategy.Requirement> reqs = new ArrayList<LinkSegStrategy.Requirement>();
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_Y, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_0_Y));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_0_X, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.S_1_X));
      reqs.add(new LinkSegStrategy.Requirement(LinkSegStrategy.Goal.S_1_Y, LinkSegStrategy.Requirement.ReqType.EQUALS, LinkSegStrategy.Goal.P_1_Y));
      useStrat.addSegStrategy(new LinkSegStrategy(segID, (LinkSegStrategy.CREATE_SPLIT_0 | LinkSegStrategy.CREATE_SPLIT_1), reqs, p0d, p1d, varyMoves));
      retval = true;
      allMyStrats.add(useStrat);
    }
    if ((strats.size() + allMyStrats.size()) > STRATEGY_LIMIT_) {
      return (false);
    }
    strats.addAll(allMyStrats);
    return (retval);
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Degrees of freedom for a seg
  */  
      
  public static class PointDOFsPerSeg {    
    public PointDegreesOfFreedom p0d;
    public PointDegreesOfFreedom p1d;   
  }
   
  /***************************************************************************
  **
  ** Info on direct/drop required directions and current compliance
  */  

  public static class ForceDirsAndMatches {
    
    public static final int NONE       = -1;
    public static final int DIRECT     = 0;
    public static final int START_DROP = 1;
    public static final int END_DROP   = 2;
    
    public Vector2D[] forceDirs;
    public boolean[] matches;
    public int type;
    public LinkSegmentID segID;

    public ForceDirsAndMatches(int type, LinkSegmentID segID) {
      this.type = type;
      this.segID = segID;
      forceDirs = new Vector2D[2];
      matches = new boolean[2];
    }
  }
  
   /***************************************************************************
  **
  ** Convert CornerDOFs into relevant axes and canonical info
  */  

  static class AxesAndCanonicals {
     
    boolean isForZero;
    int runAxis;
    int normAxis;      
    int inboundAxis;
    int inboundNorm;
    boolean inboundCanonical;
    boolean inboundCanonicalDrop;
    boolean outboundCanonicalDrop;
    boolean outboundCanonical;
    LinkSegmentID outboundCanonicalPoint;
    LinkSegmentID lsid0;
    LinkSegmentID lsid1;
    
    AxesAndCanonicals(LinkProperties.CornerDoF cdof, LinkProperties.CornerDoF p1dof, boolean isForZero, ForceDirsAndMatches fdam) { 
      this.isForZero = isForZero;
      inboundAxis = DegreeOfFreedom.UNDEF_AXIS;
      inboundNorm = DegreeOfFreedom.UNDEF_AXIS;
      inboundCanonical = false;
      lsid0 = cdof.myID;
      lsid1 = (p1dof == null) ? fdam.segID : p1dof.myID;
      
      //
      // For zero point, inbound is determined by cdof, and this seg run determined
      // by p1dof.  For p1, inbound doesn't matter
      //
           
      if (isForZero) {
        int inboundDir = cdof.runVector.canonicalDir();  // Note that run vectors are always forced to nearest canonical
        inboundCanonical = cdof.inboundIsCanonical;
        inboundCanonicalDrop = cdof.backDrop;       
        if ((inboundDir == Vector2D.EAST) || (inboundDir == Vector2D.WEST)) {
          inboundAxis = DegreeOfFreedom.X_AXIS;
          inboundNorm = DegreeOfFreedom.Y_AXIS;
        } else {
          inboundAxis = DegreeOfFreedom.Y_AXIS;
          inboundNorm = DegreeOfFreedom.X_AXIS;
        }
        int runDir = p1dof.runVector.canonicalDir();
        if ((runDir == Vector2D.EAST) || (runDir == Vector2D.WEST)) {
          runAxis = DegreeOfFreedom.X_AXIS;
          normAxis = DegreeOfFreedom.Y_AXIS;
        } else {
          runAxis = DegreeOfFreedom.Y_AXIS;
          normAxis = DegreeOfFreedom.X_AXIS;
        } 
      } else {        
        outboundCanonicalDrop = cdof.runDrop;
        // Second case handles crooked in and ortho out:
        outboundCanonical = false;
        if (cdof.runPoint != null) {
          outboundCanonical = true;
          outboundCanonicalPoint = cdof.runPoint;
        } else if (!cdof.inboundIsCanonical && cdof.backupPoint != null) {
          outboundCanonical = true;
          outboundCanonicalPoint = cdof.backupPoint;          
        }
        int runDir = cdof.runVector.canonicalDir();
        if ((runDir == Vector2D.EAST) || (runDir == Vector2D.WEST)) {
          runAxis = DegreeOfFreedom.X_AXIS;
          normAxis = DegreeOfFreedom.Y_AXIS;
        } else {
          runAxis = DegreeOfFreedom.Y_AXIS;
          normAxis = DegreeOfFreedom.X_AXIS;
        }      
      }
    }
    
    //
    // This is to handle the case where we are dealing with an end drop.  isForZero == true,
    // and we have no p1dof to work with!
    
    
    AxesAndCanonicals(LinkProperties.CornerDoF cdof, ForceDirsAndMatches fdam) { 
      this.isForZero = true;
      this.lsid0 = cdof.myID;
      this.lsid1 = fdam.segID;
      
      //
      // For zero point, inbound is determined by cdof, and this seg run determined
      // by pad requirement!
      //
          
      int inboundDir = cdof.runVector.canonicalDir();  // Note that run vectors are always forced to nearest canonical
      inboundCanonical = cdof.inboundIsCanonical;
      inboundCanonicalDrop = cdof.backDrop;       
      if ((inboundDir == Vector2D.EAST) || (inboundDir == Vector2D.WEST)) {
        inboundAxis = DegreeOfFreedom.X_AXIS;
        inboundNorm = DegreeOfFreedom.Y_AXIS;
      } else {
        inboundAxis = DegreeOfFreedom.Y_AXIS;
        inboundNorm = DegreeOfFreedom.X_AXIS;
      }
      int runDir = fdam.forceDirs[1].canonicalDir();
      if ((runDir == Vector2D.EAST) || (runDir == Vector2D.WEST)) {
        runAxis = DegreeOfFreedom.X_AXIS;
        normAxis = DegreeOfFreedom.Y_AXIS;
      } else {
        runAxis = DegreeOfFreedom.Y_AXIS;
        normAxis = DegreeOfFreedom.X_AXIS;
      } 
    }
    
    //
    // This is to handle the p1 case of a start drop
    //
    
    AxesAndCanonicals(ForceDirsAndMatches fdam) { 
      isForZero = false;
      int runDir = fdam.forceDirs[0].canonicalDir();
      if ((runDir == Vector2D.EAST) || (runDir == Vector2D.WEST)) {
        runAxis = DegreeOfFreedom.X_AXIS;
        normAxis = DegreeOfFreedom.Y_AXIS;
      } else {
        runAxis = DegreeOfFreedom.Y_AXIS;
        normAxis = DegreeOfFreedom.X_AXIS;
      } 
    }
  }
  
  /***************************************************************************
  **
  ** Required setup info
  */  
      
  public static class FixOrthoTreeInfo {
    public HashSet<LinkSegmentID> allSegIDs;
    public HashMap<LinkSegmentID, LinkProperties.CornerDoF> cornerDofs; // segID -> LinkProperties.CornerDOF
    public HashMap<LinkSegmentID, LinkSegmentID> parentIDs;  // segID -> parent segID
    public HashMap<LinkSegmentID, ForceDirsAndMatches> forceDirs;  // segID -> ForceDirsAndMatches
    public HashMap<LinkSegmentID, LinkSegment> segGeoms;   // segID -> fake seg with geom
    public HashMap<LinkSegmentID, PointDOFsPerSeg> ptDofs;
        
    public FixOrthoTreeInfo() {
      allSegIDs = new HashSet<LinkSegmentID>();
      cornerDofs = new HashMap<LinkSegmentID, LinkProperties.CornerDoF>();
      parentIDs = new HashMap<LinkSegmentID, LinkSegmentID>();
      forceDirs = new HashMap<LinkSegmentID, ForceDirsAndMatches>();
      segGeoms = new HashMap<LinkSegmentID, LinkSegment>();  
      ptDofs = new HashMap<LinkSegmentID, PointDOFsPerSeg>();  
    }
        
    /***************************************************************************
    **
    ** process raw data to what we need
    */
        
    public void prepare(LinkProperties bp) {
      Iterator<LinkSegmentID> aidit = allSegIDs.iterator();
      while (aidit.hasNext()) {
        LinkSegmentID lsid = aidit.next();
        // Note this will be null for end drops, but still need to generate for end drop segment:
        LinkProperties.CornerDoF p1Dof = cornerDofs.get(lsid);
        // DOF for start point actually uses the parent corner DOF to do calculations.
        // Note the start drop has no parent
        // Standard link -> standard parent
        // End drop -> parent segment
        // Root seg -> start drop is parent
        LinkSegmentID psid = parentIDs.get(lsid);
        // So start drop gets null DOF, start drop fdam
        // Root segment gets null for everything
        // Root's children skip the root to use the start drop
        // Everybody else gets dof of parent and fdam for the link itself
        LinkProperties.CornerDoF p0Dof;
        ForceDirsAndMatches fdam;
        
        
        if (psid == null) {        
          if (lsid.isDirect()) {
            p0Dof = null; // Don't care...
            fdam = forceDirs.get(lsid);
          } else {
            p0Dof = null;
            fdam = forceDirs.get(LinkSegmentID.buildIDForStartDrop());
          }
        } else if (psid.isForSegment() && bp.getSegment(psid).isDegenerate()) { // i.e. psid is root segment
          p0Dof = cornerDofs.get(LinkSegmentID.buildIDForStartDrop());
          fdam = forceDirs.get(lsid);
        } else if (lsid.isForSegment() && bp.getSegment(lsid).isDegenerate()) { // i.e. lsid is root segment 
          p0Dof = null;
          fdam = null;
        } else {
          p0Dof = cornerDofs.get(psid);
          fdam = forceDirs.get(lsid);
        }
        LinkSegment noSeg = segGeoms.get(lsid);
        PointDOFsPerSeg pdps = new PointDOFsPerSeg();
        pdps.p0d = dofAnalyze(noSeg.getStart(), p0Dof, p1Dof, bp, true, fdam);
        pdps.p1d = dofAnalyze(noSeg.getEnd(), p1Dof, null, bp, false, fdam);
        ptDofs.put(lsid, pdps);       
      }
      return;
    }    
  
    /****************************************************************************
    **
    ** Analyze the degrees of freedom
    */

    private PointDegreesOfFreedom dofAnalyze(Point2D point, LinkProperties.CornerDoF cdof, 
                                             LinkProperties.CornerDoF p1dof, LinkProperties bp,  
                                             boolean isForZero, ForceDirsAndMatches fdam) {

      //
      // Case for drops, or direct.  There is no flexibility!
      //
      

      DegreeOfFreedom fixY = new DegreeOfFreedom(DegreeOfFreedom.Y_AXIS, DegreeOfFreedom.FIXED, null, null);
      DegreeOfFreedom fixX = new DegreeOfFreedom(DegreeOfFreedom.X_AXIS, DegreeOfFreedom.FIXED, null, null);
      // For degenerate root, return crap:
      if (fdam == null) {
        return (new PointDegreesOfFreedom(point, fixX, fixY, null));       
      }
      
      Vector2D forceDir = (isForZero) ? fdam.forceDirs[0] : fdam.forceDirs[1];
          
      if (fdam.type == ForceDirsAndMatches.DIRECT) {
        return (new PointDegreesOfFreedom(point, fixX, fixY, forceDir));
      } else if (fdam.type == ForceDirsAndMatches.START_DROP) {
        // We have a problem, right?  For start drops, we don't know what the constraints are
        // on the p1 point.  We have no info due to the zero-length root segment? 
        if (isForZero) {
          return (new PointDegreesOfFreedom(point, fixX, fixY, fdam.forceDirs[0])); 
        } else {
         // if (cdof != null) {
      //      throw new IllegalStateException();
       //   }
          AxesAndCanonicals aac = new AxesAndCanonicals(fdam);
          return (genP1DoFForStartDrop(bp, point, fdam.forceDirs[0], aac.runAxis, aac.normAxis));
        }
      } else if (fdam.type == ForceDirsAndMatches.END_DROP) {
        if (!isForZero) {
          return (new PointDegreesOfFreedom(point, fixX, fixY, fdam.forceDirs[1]));
        }
      }          

      AxesAndCanonicals aac;
      if (isForZero && (p1dof == null)) { // End drops handled
        if ((cdof == null) || (fdam.forceDirs[1] == null))  { // Belt and suspenders.  Should not happen if no zero-length segments
          return (new PointDegreesOfFreedom(point, fixX, fixY, null));       
        }
        aac = new AxesAndCanonicals(cdof, fdam);
      } else {
        if (cdof == null) { // Belt and suspenders.  Should not happen if no zero-length segments
          return (new PointDegreesOfFreedom(point, fixX, fixY, null));       
        }
        aac = new AxesAndCanonicals(cdof, p1dof, isForZero, fdam);
      }
      
      if (aac.isForZero) {
        if (aac.inboundCanonicalDrop) {  // constrained by ortho inbound drop
          return (genP0DoFCanonDropIn(cdof, point, forceDir, aac));
        } else if (aac.inboundCanonicalDrop || aac.inboundCanonical) { 
          return (genP0DoFCanonIn(cdof, point, forceDir, aac.inboundAxis, aac.inboundNorm, aac.lsid0, aac.lsid1));
        } else { // unconstrained by non-ortho inbound segment
          return (genP0DoF(cdof, point, forceDir, aac.runAxis, aac.normAxis, DegreeOfFreedom.UNCONDITIONAL, null));
        }
      } else {     
        if (aac.outboundCanonicalDrop) {  // constrained by ortho outbound drop
          return (genP1DoF(cdof, point, forceDir, aac.runAxis, aac.normAxis, DegreeOfFreedom.FIXED, null));
        } else if (aac.outboundCanonical) {  // constrained by ortho outbound segment
          return (genP1DoF(cdof, point, forceDir, aac.runAxis, aac.normAxis, DegreeOfFreedom.CONDITIONAL, aac.outboundCanonicalPoint));
        } else { // unconstrained by ortho outbound segment
          return (genP1DoF(cdof, point, forceDir, aac.runAxis, aac.normAxis, DegreeOfFreedom.UNCONDITIONAL, null));
        }
      }
    }
    
    /***************************************************************************
    **
    ** Generate a P0 point degree of freedom when the inbound run is canonical
    */

    private PointDegreesOfFreedom genP0DoFCanonIn(LinkProperties.CornerDoF cdof, Point2D point, Vector2D forceDir, 
                                                  int inboundAxis, int inboundNorm, 
                                                  LinkSegmentID normCond, LinkSegmentID mySeg) {
      
      
      //
      // We get called if the inbound run is canonical, so ONE of our degrees of freedom is fixed by that.
      // Which (x or y) depends on what our run axis is and if we agree with the inbound run axis.
      //

      ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
      depend.add(normCond.clone());
      if ((cdof.runPoint != null) && !cdof.runPoint.equals(mySeg)) {
        depend.add(cdof.runPoint.clone());            
      }
      DegreeOfFreedom dofInboundNorm = new DegreeOfFreedom(inboundNorm, DegreeOfFreedom.CONDITIONAL, null, depend);
 
      DegreeOfFreedom dofInboundRun;
      if (cdof.antiDrop || cdof.normDrop) {
        dofInboundRun = new DegreeOfFreedom(inboundAxis, DegreeOfFreedom.FIXED, null, null);      
      } else {
        depend = new ArrayList<LinkSegmentID>();
        // If we are turning, one of these is us, and we must ignore!
        if ((cdof.antiNormPoint != null) && !cdof.antiNormPoint.equals(mySeg)) {
          depend.add(cdof.antiNormPoint.clone());
        }
        if ((cdof.normPoint != null) && !cdof.normPoint.equals(mySeg)) {
          depend.add(cdof.normPoint.clone());            
        }
        if (!depend.isEmpty()) {
          // dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null); AVOIDS CONDITIONAL DOFS
          dofInboundRun = new DegreeOfFreedom(inboundAxis, DegreeOfFreedom.CONDITIONAL, null, depend);        
        } else {
          dofInboundRun = new DegreeOfFreedom(inboundAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
        }        
      }
      
      DegreeOfFreedom dofx = (inboundAxis == DegreeOfFreedom.X_AXIS) ? dofInboundRun : dofInboundNorm;
      DegreeOfFreedom dofy = (inboundAxis == DegreeOfFreedom.X_AXIS) ? dofInboundNorm : dofInboundRun; 
      return (new PointDegreesOfFreedom(point, dofx, dofy, forceDir));
    } 
  
    /***************************************************************************
    **
    ** Generate a P0 point degree of freedom when the inbound start drop is canonical
    */

    private PointDegreesOfFreedom genP0DoFCanonDropIn(LinkProperties.CornerDoF cdof, Point2D point, Vector2D forceDir, 
                                                      AxesAndCanonicals aac) {
           
      //
      //  One of our axes is fixed (since drop is canonical).  One is either unconstrained of conditional on another root child
      //

      DegreeOfFreedom dofNorm;
      if (aac.inboundAxis == aac.runAxis) {
        dofNorm = new DegreeOfFreedom(aac.normAxis, DegreeOfFreedom.FIXED, null, null);
      } else {
        ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
        if ((cdof.antiNormPoint != null) && !cdof.antiNormPoint.equals(aac.lsid1)) {
          depend.add(cdof.antiNormPoint.clone());
        }
        if ((cdof.normPoint != null) && !cdof.normPoint.equals(aac.lsid1)) {
          depend.add(cdof.normPoint.clone());            
        }
        if (depend.isEmpty()) {
          dofNorm = new DegreeOfFreedom(aac.normAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
        } else {
          dofNorm = new DegreeOfFreedom(aac.normAxis, DegreeOfFreedom.CONDITIONAL, null, depend);
        }
      }
        
      DegreeOfFreedom dofRun;
      if (aac.inboundAxis != aac.runAxis) {
        dofRun = new DegreeOfFreedom(aac.runAxis, DegreeOfFreedom.FIXED, null, null);
      } else {
        ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
        if ((cdof.antiNormPoint != null) && !cdof.antiNormPoint.equals(aac.lsid1)) {
          depend.add(cdof.antiNormPoint.clone());
        }
        if ((cdof.normPoint != null) && !cdof.normPoint.equals(aac.lsid1)) {
          depend.add(cdof.normPoint.clone());            
        }
        if (depend.isEmpty()) {
          dofRun = new DegreeOfFreedom(aac.runAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
        } else {
          dofRun = new DegreeOfFreedom(aac.runAxis, DegreeOfFreedom.CONDITIONAL, null, depend);
        }
      }  
 
      DegreeOfFreedom dofx = (aac.runAxis == DegreeOfFreedom.X_AXIS) ? dofRun : dofNorm;
      DegreeOfFreedom dofy = (aac.runAxis == DegreeOfFreedom.X_AXIS) ? dofNorm : dofRun; 
      return (new PointDegreesOfFreedom(point, dofx, dofy, forceDir));
    } 
    
    /***************************************************************************
    **
    ** Generate a point degree of freedom
    */

    private PointDegreesOfFreedom genP0DoF(LinkProperties.CornerDoF cdof, Point2D point, Vector2D forceDir, 
                                           int runAxis, int normAxis, int normDOF, LinkSegmentID normCond) {
      
      
      //
      // We get called if the inbound run is canonical, so ONE of our degrees of freedom is fixed by that.
      // Which (x or y) depends on what our run axis is and if we agree with the inbound run axis.
      //
      
      DegreeOfFreedom dofN;
      switch (normDOF) {
        case DegreeOfFreedom.FIXED:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.FIXED, null, null);
          break;
        case DegreeOfFreedom.CONDITIONAL:
          ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
          depend.add(normCond.clone());
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.CONDITIONAL, null, depend);
          break;
        case DegreeOfFreedom.UNCONDITIONAL:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
          break;
        default:
          throw new IllegalArgumentException();
      }

      DegreeOfFreedom dofR;
      if (cdof.antiDrop || cdof.normDrop) {
        dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null);      
      } else {
         // NO NO NO SEE ABOVE IN CANON CASE
        ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();             
        if (cdof.antiNormPoint != null) {
          depend.add(cdof.antiNormPoint.clone());
        }
        if (cdof.normPoint != null) {
          depend.add(cdof.normPoint.clone());            
        }
        if (!depend.isEmpty()) {
          // dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null); AVOIDS CONDITIONAL DOFS
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.CONDITIONAL, null, depend);        
        } else {
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
        }        
      }
      DegreeOfFreedom dofx = (runAxis == DegreeOfFreedom.X_AXIS) ? dofR : dofN;
      DegreeOfFreedom dofy = (runAxis == DegreeOfFreedom.X_AXIS) ? dofN : dofR; 
      return (new PointDegreesOfFreedom(point, dofx, dofy, forceDir));
    } 
  
    /***************************************************************************
    **
    ** Generate a point degree of freedom
    */

    private PointDegreesOfFreedom genP1DoF(LinkProperties.CornerDoF cdof, Point2D point, 
                                           Vector2D forceDir, int runAxis, int normAxis, int normDOF, 
                                           LinkSegmentID normCond) {
      DegreeOfFreedom dofN;
      switch (normDOF) {
        case DegreeOfFreedom.FIXED:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.FIXED, null, null);
          break;
        case DegreeOfFreedom.CONDITIONAL:
          ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
          depend.add(normCond.clone());
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.CONDITIONAL, null, depend);
          break;
        case DegreeOfFreedom.UNCONDITIONAL:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
          break;
        default:
          throw new IllegalArgumentException();
      }

      DegreeOfFreedom dofR;
      if (cdof.antiDrop || cdof.normDrop) {
        dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null);      
      } else {
        ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();             
        if (cdof.antiNormPoint != null) {
          depend.add(cdof.antiNormPoint.clone());
        }
        if (cdof.normPoint != null) {
          depend.add(cdof.normPoint.clone());            
        }
        if (!depend.isEmpty()) {
          // dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null); AVOIDS CONDITIONAL DOFS
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.CONDITIONAL, null, depend);        
        } else {
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
        }        
      }
      DegreeOfFreedom dofx = (runAxis == DegreeOfFreedom.X_AXIS) ? dofR : dofN;
      DegreeOfFreedom dofy = (runAxis == DegreeOfFreedom.X_AXIS) ? dofN : dofR; 
      return (new PointDegreesOfFreedom(point, dofx, dofy, forceDir));
    }   
 
  
   /***************************************************************************
   **
   ** Special case: Generate a point degree of freedom for the start drop, where null
   ** CornerDOF from root seg screws us up!
   */

    private PointDegreesOfFreedom genP1DoFForStartDrop(LinkProperties lp, Point2D point, 
                                                       Vector2D forceDir, int runAxis, int normAxis) {
      
      LinkSegment startGeom = (LinkSegment)segGeoms.get(LinkSegmentID.buildIDForStartDrop());
      List<LinkSegmentID> csegs = lp.getChildSegs(lp.getRootSegmentID());
      int normDOF = (startGeom.isOrthogonal()) ? DegreeOfFreedom.FIXED : DegreeOfFreedom.UNCONDITIONAL;
      int runDOF = DegreeOfFreedom.UNCONDITIONAL;
      LinkSegmentID normCond = null;
      ArrayList<LinkSegmentID> runConds = new ArrayList<LinkSegmentID>();
      Vector2D negForceDir = forceDir.scaled(-1.0);
      
      Iterator<LinkSegmentID> csit = csegs.iterator();
      while (csit.hasNext()) {
        LinkSegmentID lsid = csit.next();
        LinkSegment geom = segGeoms.get(lsid);
        if (geom.isOrthogonal()) {
          Vector2D run = geom.getRun();
          Vector2D norm = geom.getNormal();
          if ((normDOF != DegreeOfFreedom.FIXED) && run.equals(forceDir)) {
            normCond = lsid;
            normDOF = DegreeOfFreedom.CONDITIONAL;
          } else if (norm.equals(forceDir) || norm.equals(negForceDir)) {
            runConds.add(lsid.clone());
            runDOF = DegreeOfFreedom.CONDITIONAL;
          }
        }
      }
      
      DegreeOfFreedom dofN;
      switch (normDOF) {
        case DegreeOfFreedom.FIXED:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.FIXED, null, null);
          break;
        case DegreeOfFreedom.CONDITIONAL:
          ArrayList<LinkSegmentID> depend = new ArrayList<LinkSegmentID>();
          depend.add(normCond.clone());
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.CONDITIONAL, null, depend);
          break;
        case DegreeOfFreedom.UNCONDITIONAL:
          dofN = new DegreeOfFreedom(normAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
          break;
        default:
          throw new IllegalArgumentException();
      }

      DegreeOfFreedom dofR;
      switch (runDOF) {
        case DegreeOfFreedom.FIXED:
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.FIXED, null, null);
          break;
        case DegreeOfFreedom.CONDITIONAL:
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.CONDITIONAL, null, runConds);
          break;
        case DegreeOfFreedom.UNCONDITIONAL:
          dofR = new DegreeOfFreedom(runAxis, DegreeOfFreedom.UNCONDITIONAL, new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE), null);
          break;
        default:
          throw new IllegalArgumentException();
      }
      
      DegreeOfFreedom dofx = (runAxis == DegreeOfFreedom.X_AXIS) ? dofR : dofN;
      DegreeOfFreedom dofy = (runAxis == DegreeOfFreedom.X_AXIS) ? dofN : dofR; 
      return (new PointDegreesOfFreedom(point, dofx, dofy, forceDir));
    }   
  }
}
