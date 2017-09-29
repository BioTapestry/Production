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

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Utility for splicing together link bundles
*/

public class LinkBundleSplicer {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static int TOP       = 0;
  public final static int BOTTOM    = 1;
  public final static int LEFT      = 2;
  public final static int RIGHT     = 3;
  public final static int NUM_SIDES = 4;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  private final static int INTERNAL_INIT_HACK_ = 100000;
  private final static int GLUE_HACK_ = INTERNAL_INIT_HACK_ - 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Provide a solution to the splice problem
  */
  
  public SpliceSolution spliceBundles(SpliceProblem sp, boolean multiBorderSoln) {
    
    if (sp instanceof RightAngleProblem) {
      RightAngleProblem rap = (RightAngleProblem)sp;
      if (!multiBorderSoln && rap.canConvertToButtEndProblem()) {
        sp = new ButtEndProblem(rap);
      }
    }
    
    if (sp instanceof ButtEndProblem) {
      if (sp.directOrdering()) {
        return (spliceDirectOrderedEnds((ButtEndProblem)sp));
      } else {
        return (spliceDirectUnorderedEnds((ButtEndProblem)sp));
      }
    } else {   
      return (spliceRightAngle((RightAngleProblem)sp));
    }
  }
     

  /***************************************************************************
  **
  ** Answer if we should even bother with a downward search:
  */
  
  private boolean doNotBother(int track, SortedMap<Integer, List<TaggedMinMax>> trackUsage, MultiInstanceKey internalTag) {

    List<TaggedMinMax> trackUses = trackUsage.get(new Integer(track));
    if (trackUses == null) {
      throw new IllegalStateException(); 
    }
    
    //
    // If the track changes to another source on the external side,
    // don't bother with it:
    //
    
    Iterator<TaggedMinMax> minIt = trackUses.iterator();
    while (minIt.hasNext()) {
      TaggedMinMax tmm = minIt.next();
      if (!tmm.mik.tag.equals(internalTag.tag)) {
        return (true);
      }
    }
    return (false);
  }
   
  /***************************************************************************
  **
  ** Answer if we have clear tracks
  */
  
  private boolean trackRouteWorks(PerpTask task, TaggedMinMax needed, int perpTrack, boolean isRight) {

    List<TaggedMinMax> minUses = task.trackUsage.get(new Integer(needed.range.min));
    List<TaggedMinMax> maxUses = task.trackUsage.get(new Integer(needed.range.max));
    
    int internal = (task.internalIsMin) ? -GLUE_HACK_ : GLUE_HACK_; 
    // has to differ from init value by 2!  But we don't want any confusion with possible perpTracks, which can be lower than the interface coord
    MinMax gluePiece = (new MinMax()).init().update(perpTrack).update(task.start + internal);
 
    TaggedMinMax minMatch = null;
    TaggedMinMax maxMatch = null;
    
    boolean glueViaMin = true;
    if (minUses != null) {
      Iterator<TaggedMinMax> minIt = minUses.iterator();
      while (minIt.hasNext()) {
        TaggedMinMax tmm = minIt.next();
        if ((tmm.range.intersect(gluePiece) != null) && !tmm.mik.tag.equals(needed.mik.tag)) {
          if (task.antisense) {
            return (false);
          }
          glueViaMin = false;
          break;
        }
        if (tmm.mik.tag.equals(needed.mik.tag)) {
          minMatch = tmm;
        }
      } 
    }
    
    boolean glueViaMax = true;
    if (maxUses != null) {
      Iterator<TaggedMinMax> maxIt = maxUses.iterator();
      while (maxIt.hasNext()) {
        TaggedMinMax tmm = maxIt.next();
        if ((tmm.range.intersect(gluePiece) != null) && !tmm.mik.tag.equals(needed.mik.tag)) {
          if (task.antisense) {
            return (false);
          }
          glueViaMax = false;
          break;
        }
        if (tmm.mik.tag.equals(needed.mik.tag)) {
          maxMatch = tmm;
        }
      } 
    }
    
    //
    // Antisense failures have already exited.  So we are forward sense only now!
    //
   
    if (!glueViaMin && !glueViaMax) {
      return (false);
    }
    
    //
    // We have an OK solution!  But how we record that depends on our orientation
    //
    
    //
    // Boilerplate:
    //
    
    if (minUses == null) {
      minUses = new ArrayList<TaggedMinMax>();
      task.trackUsage.put(new Integer(needed.range.min), minUses);      
    }  
    if (maxUses == null) {
      maxUses = new ArrayList<TaggedMinMax>();
      task.trackUsage.put(new Integer(needed.range.max), maxUses);      
    }  

    //
    // Orientation dependent:
    //
    
    //
    // Fix the minimum track reservation:
    //
        
    if (minMatch == null) {
      if (task.leftTurnTurnsTowardsMin) {
        // handling internal arrival with no existing internal assignment
        TaggedMinMax addMin = new TaggedMinMax(new MultiInstanceKey(needed.mik.tag, needed.range.min), gluePiece.clone());
        minUses.add(addMin);
      } else {
        // handling external departure with no existing external assignment
        TaggedMinMax addMin = new TaggedMinMax(new MultiInstanceKey(needed.mik.tag, needed.range.min), new MinMax(perpTrack, Integer.MAX_VALUE));
        maxUses.add(addMin);
      }
    } else {
      if (task.leftTurnTurnsTowardsMin) {
        if (!isRight) { // handle internal arrival with existing internal assignment
          minMatch.range = minMatch.range.union(gluePiece);
        } else { // handling external departure with existing external assignment
          minMatch.range.update(perpTrack);
        }
      } else {
        if (!isRight) { // handling external departure with existing external assignment
          minMatch.range.update(perpTrack);
        } else {  // handle internal arrival with existing internal assignment 
          minMatch.range = minMatch.range.union(gluePiece);
        }        
      }
    }
    
    //
    // Fix the maximum track reservation:
    //
        
    if (maxMatch == null) {
      if (task.leftTurnTurnsTowardsMin) {
        // handling external departure with no existing external assignment
        TaggedMinMax addMax = new TaggedMinMax(new MultiInstanceKey(needed.mik.tag, needed.range.max), new MinMax(perpTrack, Integer.MAX_VALUE));
        maxUses.add(addMax);
      } else {  
        // handling internal arrival with no existing internal assignment
        TaggedMinMax addMax = new TaggedMinMax(new MultiInstanceKey(needed.mik.tag, needed.range.max), gluePiece.clone());
        maxUses.add(addMax);        
      }
    } else {
      if (task.leftTurnTurnsTowardsMin) {
        if (!isRight) { // handle external departure with existing external assignment
          maxMatch.range.update(perpTrack);
        } else { // handle internal arrival with existing internal assignment
          maxMatch.range = maxMatch.range.union(gluePiece);
        }
      } else {
        if (!isRight) { // handle internal arrival with existing internal assignment
          maxMatch.range = maxMatch.range.union(gluePiece);
        } else { // handling external departure with existing external assignment
          maxMatch.range.update(perpTrack);
        }        
      }
    }
        
    return (true);    
  }
  
  
  /***************************************************************************
  **
  ** Find and reserve a perpendicular route we can use
  */
  
  private int getPerpRoute(PerpTask task, TaggedMinMax needed, boolean isRight) {
    
    int firstKey = task.start;
    int lastKey = task.start;
    int nextKey = task.start;
    
    if (!task.externalPerpTrackUsage.isEmpty()) {
      firstKey = ((Integer)task.externalPerpTrackUsage.firstKey()).intValue();
      lastKey = ((Integer)task.externalPerpTrackUsage.lastKey()).intValue();
    }
    
    if (task.exclusive && !task.antisense) {
      if (!task.externalPerpTrackUsage.isEmpty()) {
        nextKey = lastKey + ((task.internalIsMin) ? 1 : -1);
      }
   
      if (!trackRouteWorks(task, needed, nextKey, isRight)) { 
        throw new IllegalStateException();
      }
      ArrayList<TaggedMinMax> uses = new ArrayList<TaggedMinMax>();
      task.externalPerpTrackUsage.put(new Integer(nextKey), uses);              
      uses.add(new TaggedMinMax(needed));
      return (nextKey);
    }
    
    
    int track;   
    if (isRight) {
      track = (task.leftTurnTurnsTowardsMin) ? needed.range.max : needed.range.min;
    } else {
      track = (task.leftTurnTurnsTowardsMin) ? needed.range.min : needed.range.max;
    }
    
    boolean justReverse = task.antisense && doNotBother(track, task.trackUsage, needed.mik);
        
    Iterator<Integer> useIt;
    if (justReverse) {
      TreeSet<Integer> backwardsSet = (task.internalIsMin) ? new TreeSet<Integer>(Collections.reverseOrder()) : new TreeSet<Integer>();
      backwardsSet.addAll(task.externalPerpTrackUsage.keySet());
      int extraKeyVal = (task.internalIsMin) ? firstKey - 1 : firstKey + 1;     
      backwardsSet.add(new Integer(extraKeyVal));
      useIt = backwardsSet.iterator();
    } else {
      TreeSet<Integer> forwardSet = (task.internalIsMin) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
      forwardSet.addAll(task.externalPerpTrackUsage.keySet());
      int extraKeyVal = (task.internalIsMin) ? lastKey + 1 : lastKey - 1;     
      forwardSet.add(new Integer(extraKeyVal));
      useIt = forwardSet.iterator();
    }

    while (useIt.hasNext()) {
      Integer perpTrack = useIt.next();
      List<TaggedMinMax> uses = task.externalPerpTrackUsage.get(perpTrack);
      boolean slotFound = true;
      if (uses != null) {
        Iterator<TaggedMinMax> uit = uses.iterator();          
        while (uit.hasNext()) {
          TaggedMinMax used = uit.next();
          if (used.range.intersect(needed.range) != null) {
            slotFound = false;
            break;
          }
        }
      }
      if (slotFound && trackRouteWorks(task, needed, perpTrack.intValue(), isRight)) {
        if (uses == null) {
          uses = new ArrayList<TaggedMinMax>();
          task.externalPerpTrackUsage.put(perpTrack, uses);              
        }
        uses.add(new TaggedMinMax(needed));
        return (perpTrack.intValue());
      }
      nextKey = perpTrack.intValue() + ((task.internalIsMin) ? 1 : -1);
    }
    System.err.println("Why did I get key " + nextKey);
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Splice two bundles headed straight towards each other and which share the
  ** same ordering. 
  */
  
  private SpliceSolution spliceDirectOrderedEnds(ButtEndProblem bep) {

    int solnSide = bep.getSide();
    SpliceSolution retval = new SpliceSolution(solnSide);
            
    //
    // Get task set up and oriented:
    //
    
    PerpTask task = new PerpTask(solnSide);
        
    //
    // Reserve the track routes for the internal and external bundles:
    //
    
    task.initTrackRoutes(bep);
           
    //
    // Figure out right, left, and no turns, as viewed from the external bundles arriving and
    // routing into the internal bundles.
    //
    
    task.assignTurnDirections(bep);
                
    int interfaceCoord = bep.getInterfaceCoord();
    task.setupUsage(interfaceCoord);
    
    
    task.buildTrackSets(bep);
 
    Iterator<Integer> ebit = task.backTracks.iterator();
    while (ebit.hasNext()) {
      Integer externTrackObj = ebit.next();
      MultiInstanceKey ebSrcK = bep.externalBundle_.get(externTrackObj);
      int trackNum = externTrackObj.intValue();      
      if (task.straight.contains(ebSrcK)) {
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path);
        // FIX ME?
        // Original early implementation comment here said to "reserve the track too!", but I think that is
        // just being fussy in the final implementation...that is not _really_ needed, just a case of 
        // being extremely precise.  But maybe I'm wrong....
        path.add((task.tracksAreYCoord) ? new Point(interfaceCoord, trackNum) : new Point(trackNum, interfaceCoord));
      } else if (task.rightTurns.contains(ebSrcK)) {
        task.routeDirect(bep, externTrackObj, retval, true);
      } else { // left turn
        task.leftTracks.add(externTrackObj);
      } 
    }
    
    // This clear gives us reuse of "exclusive" tracks as we move on to doing the left side:
    task.externalPerpTrackUsage.clear();    
    Iterator<Integer> ltit = task.leftTracks.iterator();
    while (ltit.hasNext()) {
      Integer externTrackObj = ltit.next();
      task.routeDirect(bep, externTrackObj, retval, false);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Calculate results for choosing one direction to be direct
  */
  
  private int checkDirectDirection(ButtEndProblem bep, HashSet<MultiInstanceKey> recoveryTurns, 
                                   HashSet<MultiInstanceKey> partialRecoveryTurnsInternal,
                                   HashSet<MultiInstanceKey> partialRecoveryTurnsExternal, 
                                   HashSet<MultiInstanceKey> directTurns, 
                                   PerpTask task, boolean recoveryIsRight) {
    
    if (!recoveryIsRight) {
      task.antisense = true;  // Go leftward instead of rightward!
    }
    
    task.buildTrackSets(bep);

    //
    // Recovery endruns are only REQUIRED when we have a dense set of bundles with overlapping
    // mismatches of internal and external traces.  NOTE that if we have a recovery situation
    // where there is an empty external trace opposite the desired target, we can go direct!  We can
    // also go direct when there is an empty internal trace opposite the incoming trace
    //
    
    recoveryTurns.addAll((recoveryIsRight) ? task.rightTurns : task.leftTurns);
    directTurns.addAll((recoveryIsRight) ? task.leftTurns : task.rightTurns); 
    HashSet<Integer> ibUses = bep.allInternalTrackVals();

    Iterator<MultiInstanceKey> rtit = recoveryTurns.iterator();
    while (rtit.hasNext()) {
      MultiInstanceKey srcK = rtit.next();
      // Look for unused track on the external side:
      Set<Integer> internalTracks = bep.internalBundle_.get(srcK.tag);
      if (internalTracks.size() != 1) {
        throw new IllegalStateException();
      }      
      Integer targTrack = internalTracks.iterator().next();

      MultiInstanceKey ebSrcK = bep.externalBundle_.get(targTrack);
      if (ebSrcK == null) {
        partialRecoveryTurnsExternal.add(new MultiInstanceKey(srcK));
        continue;
      }
      // Look for unused track on the internal side:
      Iterator<Integer> ebtit = bep.externalBundle_.keySet().iterator();
      while (ebtit.hasNext()) {
        Integer chkTrack = ebtit.next();
        MultiInstanceKey chkSrcK = bep.externalBundle_.get(chkTrack);
        if (chkSrcK.tag.equals(srcK.tag)) {
          if (!ibUses.contains(chkTrack)) {
            partialRecoveryTurnsInternal.add(new MultiInstanceKey(srcK));
          }
          break;
        }
      }
    }

    recoveryTurns.removeAll(partialRecoveryTurnsExternal);
    recoveryTurns.removeAll(partialRecoveryTurnsInternal);
    return (recoveryTurns.size());
  }
  
  /***************************************************************************
  **
  ** Figure out the guaranteed detour possibilities
  */
  
  private EndRunAnswer guaranteedDetours(ButtEndProblem bep, int indirectSize, boolean recoveryIsRight, PerpTask task) {
    
    //
    // Required guaranteed detours for left and right
    
    int leftDetour; 
    int rightDetour;
     
    leftDetour = ((Integer)task.backTracks.first()).intValue();
    TreeSet<Integer> forInternalLeftMost = (task.leftTurnTurnsTowardsMin) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
    forInternalLeftMost.addAll(bep.allInternalTrackVals());
    Integer internalLeftmostObj = (Integer)forInternalLeftMost.first();
    int internalLeftmost = internalLeftmostObj.intValue();
    if ((task.leftTurnTurnsTowardsMin && (internalLeftmost < leftDetour)) ||
        (!task.leftTurnTurnsTowardsMin && (internalLeftmost > leftDetour))) {
      leftDetour = internalLeftmost;
    }  

    // Note task.antisense is true in this case, so backTracks goes left. 
    rightDetour = ((Integer)task.backTracks.first()).intValue();
    TreeSet<Integer> forInternalRightmost = (task.leftTurnTurnsTowardsMin) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
    forInternalRightmost.addAll(bep.allInternalTrackVals());
    Integer internalRightmostObj = (Integer)forInternalRightmost.last();
    int internalRightmost = internalRightmostObj.intValue();
    if ((task.leftTurnTurnsTowardsMin && (internalRightmost > rightDetour)) ||
        (!task.leftTurnTurnsTowardsMin && (internalRightmost < rightDetour))) {
      rightDetour = internalRightmost;
    }

    int leftEndIncrement = (task.leftTurnTurnsTowardsMin) ? 1 : -1;
    int leftMax = leftDetour - (leftEndIncrement * indirectSize);    
       
    int rightEndIncrement = (task.leftTurnTurnsTowardsMin) ? -1 : 1;
    int rightMax = rightDetour - (rightEndIncrement * indirectSize);     
   
    int endRun;
    int endIncrement;       
    if (recoveryIsRight) { 
      endIncrement = leftEndIncrement;
      endRun = leftMax;            
    } else {
      endIncrement = rightEndIncrement;
      endRun = rightMax;     
    }
    
    HashSet<Integer> ibUses = new HashSet<Integer>(bep.allInternalTrackVals());  
    HashSet<Integer> ebKeys = new HashSet<Integer>(bep.externalBundle_.keySet());
    
    //
    // This is the set of tracks that are unused on both the internal
    // and external side, all the way out to the guaranteed detour maximums:
    // Detours tracks WILL come from this set:
    //
    
    SortedSet<Integer> availDetours = new TreeSet<Integer>();
    availDetours.add(new Integer(leftMax));
    availDetours.add(new Integer(rightMax));    
    availDetours = DataUtil.fillOutHourly(availDetours);
    availDetours.removeAll(ibUses); 
    availDetours.removeAll(ebKeys); 

    return (new EndRunAnswer(availDetours, endRun, endIncrement, leftDetour, leftEndIncrement, leftMax, rightDetour, rightEndIncrement, rightMax));
  }
  

  /***************************************************************************
  **
  ** Splice two bundles headed straight towards each other and which share the
  ** same ordering. 
  */
  
  private SpliceSolution spliceDirectUnorderedEnds(ButtEndProblem bep) {

    int solnSide = bep.getSide();
    SpliceSolution retval = new SpliceSolution(solnSide);
            
    //
    // Get task set up and oriented:
    //
    
    PerpTask preTask = new PerpTask(solnSide);
        
    //
    // Reserve the track routes for the internal and external bundles:
    //
    
    preTask.initTrackRoutes(bep);
           
    //
    // Figure out right, left, and no turns, as viewed from the external bundles arriving and
    // routing into the internal bundles.
    //
    
    preTask.assignTurnDirections(bep);
                
    int interfaceCoord = bep.getInterfaceCoord();
    preTask.setupUsage(interfaceCoord);
     
    
    //
    // Need to decide whether we will take left or right turns to be direct, versus
    // needing recovery.
    //
 
    HashSet<MultiInstanceKey> rightRecoveryTurns = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> rightPartialRecoveryTurnsInternal = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> rightPartialRecoveryTurnsExternal = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> rightDirectTurns = new HashSet<MultiInstanceKey>();
    PerpTask rightTask = (PerpTask)preTask.clone();    
    int rightRecoveryCount = checkDirectDirection(bep, rightRecoveryTurns, 
                                                  rightPartialRecoveryTurnsInternal, 
                                                  rightPartialRecoveryTurnsExternal, 
                                                  rightDirectTurns, rightTask, true);
    
    HashSet<MultiInstanceKey> leftRecoveryTurns = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> leftPartialRecoveryTurnsInternal = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> leftPartialRecoveryTurnsExternal = new HashSet<MultiInstanceKey>();
    HashSet<MultiInstanceKey> leftDirectTurns = new HashSet<MultiInstanceKey>();
    PerpTask leftTask = (PerpTask)preTask.clone();    
    int leftRecoveryCount = checkDirectDirection(bep, leftRecoveryTurns, 
                                                 leftPartialRecoveryTurnsInternal, 
                                                 leftPartialRecoveryTurnsExternal, 
                                                 leftDirectTurns, leftTask, false);
    
    
    HashSet<MultiInstanceKey> recoveryTurns;
    HashSet<MultiInstanceKey> partialRecoveryTurnsInternal;
    HashSet<MultiInstanceKey> partialRecoveryTurnsExternal;
    HashSet<MultiInstanceKey> directTurns;
    boolean recoveryIsRight;
    PerpTask task;
    if (leftRecoveryCount < rightRecoveryCount) {
      recoveryTurns = leftRecoveryTurns;
      partialRecoveryTurnsInternal = leftPartialRecoveryTurnsInternal;
      partialRecoveryTurnsExternal = leftPartialRecoveryTurnsExternal;
      directTurns = leftDirectTurns;
      recoveryIsRight = false;
      task = leftTask;    
    } else {
      recoveryTurns = rightRecoveryTurns;
      partialRecoveryTurnsInternal = rightPartialRecoveryTurnsInternal;
      partialRecoveryTurnsExternal = rightPartialRecoveryTurnsExternal;
      directTurns = rightDirectTurns;
      recoveryIsRight = true;
      task = rightTask;    
    }
    
    //
    // Guys going in the recovery direction are first assigned recovery tracks:
    // 
    
    HashMap<String, Integer> indirAssignS = new HashMap<String, Integer>();
    HashMap<String, Integer> indirRecoveryS = new HashMap<String, Integer>();    

    int preAssign = task.start;
    Iterator<Integer> ebit = task.backTracks.iterator();
    ArrayList<MultiInstanceKey> forMainRecovery = new ArrayList<MultiInstanceKey>();
    ArrayList<MultiInstanceKey> forMainDirects = new ArrayList<MultiInstanceKey>();
    while (ebit.hasNext()) {
      Integer externTrackObj = ebit.next();
      MultiInstanceKey ebSrcK = bep.externalBundle_.get(externTrackObj);
      if (recoveryTurns.contains(ebSrcK)) { 
        indirRecoveryS.put(ebSrcK.tag, new Integer(preAssign));
        preAssign += (task.internalIsMin) ? 1 : -1;
        forMainRecovery.add(0, ebSrcK);        
      } else if (partialRecoveryTurnsInternal.contains(ebSrcK)) {
        indirRecoveryS.put(ebSrcK.tag, new Integer(preAssign));
        preAssign += (task.internalIsMin) ? 1 : -1;
        forMainRecovery.add(0, ebSrcK);          
      } else if (partialRecoveryTurnsExternal.contains(ebSrcK)) {
        forMainRecovery.add(ebSrcK);          
      } else if (directTurns.contains(ebSrcK)) {
        forMainDirects.add(0, ebSrcK); 
      }
    }
    
    //
    // Guys going direct now get main tracks:
    //
 
    Iterator<MultiInstanceKey> fmlit = forMainDirects.iterator();
    while (fmlit.hasNext()) {
      MultiInstanceKey ebSrcK = fmlit.next();
      indirAssignS.put(ebSrcK.tag, new Integer(preAssign));
      preAssign += (task.internalIsMin) ? 1 : -1;
    }
    
    //
    // Guys going via recovery now get main tracks:
    //
 
    Iterator<MultiInstanceKey> fmrit = forMainRecovery.iterator();
    while (fmrit.hasNext()) {
      MultiInstanceKey ebSrcK = fmrit.next();
      if (partialRecoveryTurnsInternal.contains(ebSrcK)) {
        continue;
      }
      indirAssignS.put(ebSrcK.tag, new Integer(preAssign));
      preAssign += (task.internalIsMin) ? 1 : -1;
    }
 
    //
    // Figure out possible detours we can use:
    //
       
    EndRunAnswer era = guaranteedDetours(bep, recoveryTurns.size(), recoveryIsRight, task);    

    
    //
    // Assign detour tracks to the guys who need them.  Need to proceed backwards, so use the leftTracks mechanism:
    //
    
    HashMap<MultiInstanceKey, Integer> assignedDetours = new HashMap<MultiInstanceKey, Integer>();
    HashSet<Integer> detours = new HashSet<Integer>(era.availDetours);
    task.leftTracks.clear();
    task.leftTracks.addAll(task.backTracks);
    boolean tieBreakBelow = true; // No! More complex!    
    Iterator<Integer> dtit = task.leftTracks.iterator();
    while (dtit.hasNext()) {
      Integer externTrackObj = dtit.next();
      MultiInstanceKey ebSrcK = bep.externalBundle_.get(externTrackObj);
      int trackNum = externTrackObj.intValue(); 
      if (recoveryTurns.contains(ebSrcK)) { // full recovery turn goes to a detour track, then back to destination, using inner and outer recovery tracks:     
        int internalTrack = bep.getSingleInternalTrackVal(ebSrcK);
        Integer useTrackE = DataUtil.closestInt(detours, trackNum, tieBreakBelow);
        int uteVal = useTrackE.intValue();
        Integer useTrackI = DataUtil.closestInt(detours, internalTrack, tieBreakBelow);
        int utiVal = useTrackI.intValue();
        int sumE = Math.abs(uteVal - trackNum) + Math.abs(uteVal - internalTrack);
        int sumI = Math.abs(utiVal - trackNum) + Math.abs(utiVal - internalTrack);
        Integer use = (sumE < sumI) ? useTrackE : useTrackI;
        detours.remove(use);
        assignedDetours.put(ebSrcK, use);
      } 
    }
    task.leftTracks.clear();
    
    //
    // Now build the paths:
    //

    Iterator<Integer> btit = task.backTracks.iterator();
    while (btit.hasNext()) {
      Integer externTrackObj = btit.next();
      MultiInstanceKey ebSrcK = bep.externalBundle_.get(externTrackObj);
      int trackNum = externTrackObj.intValue();
      if (task.straight.contains(ebSrcK)) {
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path); 
        path.add((task.tracksAreYCoord) ? new Point(interfaceCoord, trackNum) : new Point(trackNum, interfaceCoord));
      } else if (directTurns.contains(ebSrcK)) {      
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path);               
        int internalTrack = bep.getSingleInternalTrackVal(ebSrcK);
        Integer crossTrackObj = indirAssignS.get(ebSrcK.tag); 
        int crossTrack = crossTrackObj.intValue();
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, internalTrack) : new Point(internalTrack, crossTrack));
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, trackNum) : new Point(trackNum, crossTrack));
      } else if (partialRecoveryTurnsInternal.contains(ebSrcK)) { // partial recovery turn goes via inner recovery track, else like direct:
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path);               
        int internalTrack = bep.getSingleInternalTrackVal(ebSrcK);
        Integer recoveryObj = indirRecoveryS.get(ebSrcK.tag); 
        int recoveryTrack = recoveryObj.intValue();
        path.add((task.tracksAreYCoord) ? new Point(recoveryTrack, internalTrack) : new Point(internalTrack, recoveryTrack));
        path.add((task.tracksAreYCoord) ? new Point(recoveryTrack, trackNum) : new Point(trackNum, recoveryTrack));
      } else if (partialRecoveryTurnsExternal.contains(ebSrcK)) { // partial recovery turn goes via outer recovery track, else like direct:
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path);               
        int internalTrack = bep.getSingleInternalTrackVal(ebSrcK);
        Integer crossTrackObj = indirAssignS.get(ebSrcK.tag); 
        int crossTrack = crossTrackObj.intValue();
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, internalTrack) : new Point(internalTrack, crossTrack));
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, trackNum) : new Point(trackNum, crossTrack));        
        
      } else { // full recovery turn goes to a detour track, then back to destination, using inner and outer recovery tracks:
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ebSrcK, path);               
        int internalTrack = bep.getSingleInternalTrackVal(ebSrcK);
        Integer crossTrackObj = indirAssignS.get(ebSrcK.tag); 
        int crossTrack = crossTrackObj.intValue();
        Integer recoveryObj = indirRecoveryS.get(ebSrcK.tag); 
        int recoveryTrack = recoveryObj.intValue();
        Integer use = (Integer)assignedDetours.get(ebSrcK);
        int useVal = use.intValue();        
        path.add((task.tracksAreYCoord) ? new Point(recoveryTrack, internalTrack) : new Point(internalTrack, recoveryTrack));
        path.add((task.tracksAreYCoord) ? new Point(recoveryTrack, useVal) : new Point(useVal, recoveryTrack));
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, useVal) : new Point(useVal, crossTrack));
        path.add((task.tracksAreYCoord) ? new Point(crossTrack, trackNum) : new Point(trackNum, crossTrack));
      } 
    }    
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Splice two bundles, one is at right angles to the other.  The external traces
  ** need to be connected into the (already existing) right angle internal traces,
  ** but those right-angle traces already have one (OR MORE) internal traces attached
  ** to it.
  */
  
  private SpliceSolution spliceRightAngle(RightAngleProblem rap) {
 
    //
    // Do preprocessing:
    //
    
    rap.internalContraints_ = new HashMap<Integer, Integer>();
    Iterator<String> ulflit = rap.internalBundle_.keySet().iterator();
    while (ulflit.hasNext()) {
      String urID = ulflit.next();
      Set<Integer> tracks = rap.internalBundle_.get(urID);
      Iterator<Integer> trit = tracks.iterator();
      while (trit.hasNext()) {
        Integer track = trit.next();
        Integer targCol = rap.rightAngleTargTraces_.get(urID);
        rap.internalContraints_.put(track, targCol);
      }    
    }
    
    //
    // Solve:
    //
    
    int solnSide = rap.getSide();
    SpliceSolution retval = new SpliceSolution(solnSide);    
    PerpTask task = new PerpTask(solnSide);     
  
    //
    // Figure out the first external right-angle track that we can self-assign
    // to move across the tracks to form detours:
    //
    
    TreeSet<Integer> useForCurrCalc = new TreeSet<Integer>(rap.rightAngleTargTraces_.values());   
    int currBlockTrack;
    if (!task.internalIsMin) {
      currBlockTrack = (useForCurrCalc.first()).intValue() - 1;
    } else {
      currBlockTrack = (useForCurrCalc.last()).intValue() + 1;
    }

    //
    // Do the assignments.  Note for left-hand borders, this moves
    // top to bottom:
    //
     
    HashSet<Integer> tracksUsed = new HashSet<Integer>();
    TreeSet<Integer> gottaDetour = new TreeSet<Integer>();
       
    Iterator<Integer> trackKit = rap.externalBundle_.keySet().iterator();
    while (trackKit.hasNext()) {
      Integer track = trackKit.next();
      int trackNum = track.intValue();
      MultiInstanceKey ridK = rap.externalBundle_.get(track);
      // Where we want to go:
      Integer targetObj = rap.rightAngleTargTraces_.get(ridK.tag);
      int target = targetObj.intValue();
      if (trackIsAvailable(ridK.tag, track, rap, task, target)) {
        ArrayList<Point> path = new ArrayList<Point>();
        retval.addPath(ridK, path);
        path.add((task.tracksAreYCoord) ? new Point(target, trackNum) : new Point(trackNum, target));
        tracksUsed.add(track);
        continue; 
      }
      gottaDetour.add(track);
    }
    
    //
    // Once directs are placed, we go back and handle the detours:
    //
      
    Iterator<Integer> gdit = gottaDetour.iterator();
    while (gdit.hasNext()) {
      Integer track = gdit.next();
      int trackNum = track.intValue();
      MultiInstanceKey ridK = rap.externalBundle_.get(track);
      Integer targetObj = rap.rightAngleTargTraces_.get(ridK.tag);
      int target = targetObj.intValue();
      //
      // We cannot go direct.  Find the closest tracks on either side that
      // can support a detour:
      //
      // Nov. 2012 fix:  The trackIsAvailable test is looking inward to see if the way is clear.
      // But if we have been detouring, some of the track on the external side may have been
      // occupied already by a detour.  Now, the best test would consider our detour in the
      // context of the specific tracks blocked by detours, but V6 is on the way out the door.
      // To avoid all tracks used for detours, we should be conservative!
      //
      int availMinTrack = trackNum - 1;
      while (true) {
        Integer amtObj = new Integer(availMinTrack);
        // Now completely rejecting detour tracks:
        if (trackIsAvailable(ridK.tag, amtObj, rap, task, target) && !gottaDetour.contains(amtObj)) {
          if (!tracksUsed.contains(amtObj)) {
            break;
          }
        }
        availMinTrack--;
      }
      int availMaxTrack = trackNum + 1;
      while (true) {
        Integer amtObj = new Integer(availMaxTrack);
        // Now completely rejecting detour tracks:
        if (trackIsAvailable(ridK.tag, amtObj, rap, task, target) && !gottaDetour.contains(amtObj)) {
          if (!tracksUsed.contains(amtObj)) {
            break;
          }
        }
        availMaxTrack++;
      }

      //
      // Route is constructed to support an internal->external exit.  Must be
      // reversed to build an inbound path!
      //
      
      TreeSet<Integer> targTracks = new TreeSet<Integer>(rap.internalBundle_.get(ridK.tag));
      Integer minTrg = (Integer)targTracks.first();
      Integer maxTrg = (Integer)targTracks.last();    
      int lenViaMax = Math.abs(availMaxTrack - trackNum) + Math.abs(availMaxTrack - maxTrg.intValue());
      int lenViaMin = Math.abs(availMinTrack - trackNum) + Math.abs(availMinTrack - minTrg.intValue());
      int sign = (task.internalIsMin) ? 1 : -1;
      
      ArrayList<Point> path = new ArrayList<Point>();
      retval.addPath(ridK, path);
      if (lenViaMin < lenViaMax) {
        path.add((task.tracksAreYCoord) ? new Point(target, availMinTrack) : new Point(availMinTrack, target));
        path.add((task.tracksAreYCoord) ? new Point(currBlockTrack, availMinTrack) : new Point(availMinTrack, currBlockTrack));
        path.add((task.tracksAreYCoord) ? new Point(currBlockTrack, trackNum) : new Point(trackNum, currBlockTrack));
        currBlockTrack += sign;
        tracksUsed.add(new Integer(availMinTrack));
      } else {
        path.add((task.tracksAreYCoord) ? new Point(target, availMaxTrack) : new Point(availMaxTrack, target));
        path.add((task.tracksAreYCoord) ? new Point(currBlockTrack, availMaxTrack) : new Point(availMaxTrack, currBlockTrack));   
        path.add((task.tracksAreYCoord) ? new Point(currBlockTrack, trackNum) : new Point(trackNum, currBlockTrack));
        currBlockTrack += sign;
        tracksUsed.add(new Integer(availMaxTrack));
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answers if the track can access the given target
  */
  
  private boolean trackIsAvailable(String ebSrc, Integer checkTrack, RightAngleProblem rap, PerpTask task, int target) {
    // Can we get there?
    Integer backBlock = (Integer)rap.internalContraints_.get(checkTrack);
     // No block, we can go direct.
    if (backBlock == null) {
      return (true);
    }
    // If block is further away than our target, we can go direct:
    int bbVal = backBlock.intValue();
    if ((!task.internalIsMin && (bbVal > target)) || (task.internalIsMin && (bbVal < target))) {
      return (true);
    }
    // If block is the same as our target, we can still
    // go direct IF the block has the same source as us!
    if (bbVal == target) {
      Set<Integer> tracks = rap.internalBundle_.get(ebSrc);
      if ((tracks != null) && tracks.contains(checkTrack)) {
        return (true);
      }
    }
    return (false);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  ** 
  ** Base class of various splice problems
  */
  
  public static abstract class SpliceProblem {
    
    // internalBundle_: String src -> Integer trace list
    // externalBundle_: Sorted Integer externalTrack -> MultiInstanceKey src
    
    protected SortedMap<Integer, MultiInstanceKey> externalBundle_;
    protected int side_;
    protected Map<String, Set<Integer>> internalBundle_; 
    protected int interfaceCoord_;
       
    // Shallow only!  Used for type conversion....
    
    protected SpliceProblem(SpliceProblem other) {
      this.side_ = other.side_;
      this.externalBundle_ = other.externalBundle_;
      this.internalBundle_ = other.internalBundle_;
      this.interfaceCoord_ = other.interfaceCoord_;
    }
    
    protected SpliceProblem(int side) {
      side_ = side;
      externalBundle_ = new TreeMap<Integer, MultiInstanceKey>();
      internalBundle_ = new HashMap<String, Set<Integer>>();
      interfaceCoord_ = ((side == BOTTOM) || (side == RIGHT)) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }
    
    public boolean wantY() {    
      return ((side_ == LEFT) || (side_ == RIGHT));
    }
    
    public int getSide() {
      return (side_);
    }
    
    public void putExternalBundle(int coord, String src) {
      externalBundle_.put(new Integer(coord), new MultiInstanceKey(src, coord));
      return;
    }
    
    public void setInternalTrack(String src, int coord) {
      Set<Integer> forSrc = internalBundle_.get(src);
      if (forSrc == null) {
        forSrc = new HashSet<Integer>();
        internalBundle_.put(src, forSrc);
      }
      forSrc.add(new Integer(coord));
      return;
    }
    
    public void setAdditionalInternalTracks(String src, List<Integer> coords) {
      Set<Integer> forSrc = internalBundle_.get(src);
      if (forSrc == null) {
        forSrc = new HashSet<Integer>();
        internalBundle_.put(src, forSrc);
      }
      forSrc.addAll(coords);
      return;
    }
    
    HashSet<Integer> allInternalTrackVals() {
      HashSet<Integer> ibUses = new HashSet<Integer>();
      Iterator<Set<Integer>> ibit = internalBundle_.values().iterator();
      while (ibit.hasNext()) {
        Set<Integer> internalTracks = ibit.next();
        if (internalTracks.size() != 1) {
          throw new IllegalStateException();
        }      
        ibUses.addAll(internalTracks);
      }
      return (ibUses);
    }
    
    int getSingleInternalTrackVal(MultiInstanceKey srcIDK) {
      Set<Integer> internalTracks = internalBundle_.get(srcIDK.tag);
      if (internalTracks.size() != 1) {
        throw new IllegalStateException();
      }      
      return (internalTracks.iterator().next().intValue());
    }   
 
    // Answer if the internal and external bundling orders match:
    protected boolean directOrdering() { 
      int lastIbtr = Integer.MIN_VALUE;
      HashSet<String> seenMIK = new HashSet<String>();
      Iterator<MultiInstanceKey> ebit = externalBundle_.values().iterator();
      while (ebit.hasNext()) {
        MultiInstanceKey ebsrck = ebit.next();
        Set<Integer> forSrc = internalBundle_.get(ebsrck.tag);
        if (forSrc.size() != 1) {
          return (false);
        }
        if (seenMIK.contains(ebsrck.tag)) {
          return (false);
        }
        seenMIK.add(ebsrck.tag);
        Integer ibtr = forSrc.iterator().next();
        int currIbtr = ibtr.intValue();
        if (currIbtr <= lastIbtr) {
          return (false);
        }
        lastIbtr = currIbtr;
      }
      return (true);
    }
     
    public void updateInterfaceCoord(Rectangle2D rect) {
      if (side_ == TOP) {
        int altIC = (int)rect.getMinY();
        //
        // Treating Issue #212. Previously, added or subtracted UiUtil.GRID_SIZE_INT, i.e. 10. But these units are in grid
        // counts, so 10 should be one. Furthermore, the offset needs to be based on half the size of the bundle. Thus, 
        // these fixes. What is strange is that 10 was "too big", but ultimately lead to the overlap. Unclear why...
        //
        if (interfaceCoord_ > altIC) {
          interfaceCoord_ = altIC - ((internalBundle_.size() / 2) + 1); //NO! UiUtil.GRID_SIZE_INT;
        }
      } else if (side_ == BOTTOM) {
        int altIC = (int)rect.getMaxY();
        if (interfaceCoord_ < altIC) {
          interfaceCoord_ = altIC + (internalBundle_.size() / 2) + 1; // NO! UiUtil.GRID_SIZE_INT;
        }
      }
      return;
    }

    public void putTargetTrace(String src, int coord) {
      if ((side_ == BOTTOM) || (side_ == RIGHT)) {
        if (coord < interfaceCoord_) {
          interfaceCoord_ = coord;
        }
      } else {
        if (coord > interfaceCoord_) {
          interfaceCoord_ = coord;
        }
      }
      return;
    }
    
    public int getInterfaceCoord() {
      return (interfaceCoord_);
    }   
  }

  /***************************************************************************
  ** 
  **  Problem class I
  */
  
  public static class ButtEndProblem extends SpliceProblem {
    public ButtEndProblem(int side) {
      super(side);  
    }
    
    public ButtEndProblem(RightAngleProblem rap) {
      super(rap);
    }    
  }

  /***************************************************************************
  ** 
  **  Problem class II
  */
  
  public static class RightAngleProblem extends SpliceProblem {
    
    Map<String, Integer> rightAngleTargTraces_;
    HashMap<Integer, Integer> internalContraints_;
     
    //
    // rightAngleTargTraces_: String src -> Integer trace 
    //
        
    public RightAngleProblem(int side) {
      super(side);
      rightAngleTargTraces_ = new HashMap<String, Integer>();
      internalContraints_ = new HashMap<Integer, Integer>();
    }     
        
    public boolean canConvertToButtEndProblem() {
      Iterator<Set<Integer>> ibit = internalBundle_.values().iterator();
      while (ibit.hasNext()) {
        Set<Integer> internalTracks = ibit.next();
        if (internalTracks.size() != 1) {
          return (false);
        }      
      }
      return (true);
    }
 
    public void putTargetTrace(String src, int coord) {
      super.putTargetTrace(src, coord);
      rightAngleTargTraces_.put(src, new Integer(coord));
      return;
    }
               
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("*********************LCO" + "\n");
      TreeSet<Integer> order = new TreeSet<Integer>(internalContraints_.keySet());
      Iterator<Integer> odit = order.iterator();
      while (odit.hasNext()) {
        Integer r = odit.next();
        Integer blo = internalContraints_.get(r);
        buf.append("***" + r + " -> " + blo + "\n");
      }
      buf.append("*********************URFL" + "\n");
      TreeSet<String> order1 = new TreeSet<String>(internalBundle_.keySet());
      Iterator<String> odit1 = order1.iterator();
      while (odit1.hasNext()) {
        String r = odit1.next();
        Set<Integer> blo = internalBundle_.get(r);
        buf.append("*** s" + r + " -> " + blo + "\n");
      }
      buf.append("*********************HRB" + "\n");
      TreeSet<Integer> order2 = new TreeSet<Integer>(externalBundle_.keySet());
      Iterator<Integer> odit2 = order2.iterator();
      while (odit2.hasNext()) {
        Integer r = odit2.next();
        MultiInstanceKey blo = externalBundle_.get(r);
        buf.append("***" + r + " -> s" + blo + "\n");
      }
      return (buf.toString());
    }  
  }
   
  /***************************************************************************
  ** 
  ** Solution
  */
  
  public static class SpliceSolution {
    
    private int side_;
    private HashMap<String, Map<Integer, List<Point>>> pathBySrc_;
    
    public SpliceSolution(int side) {
      side_ = side;
      pathBySrc_ = new HashMap<String, Map<Integer, List<Point>>>();
    }
    
    public int getSide() {
      return (side_);
    }
       
    void addPath(MultiInstanceKey mik, List<Point> path) {
      Map<Integer, List<Point>> perTrace = pathBySrc_.get(mik.tag);
      if (perTrace == null) {
        perTrace = new HashMap<Integer, List<Point>>();
        pathBySrc_.put(mik.tag, perTrace);
      }
      perTrace.put(new Integer(mik.instance), path);
      return;
    }
    
    public Rectangle2D getBounds() {
      Rectangle2D retval = null;
      Iterator<String> pbsit = pathBySrc_.keySet().iterator();
      while (pbsit.hasNext()) {
        String tag = pbsit.next();
        Map<Integer, List<Point>> pbs = pathBySrc_.get(tag);
        Iterator<Integer> pbskit = pbs.keySet().iterator();
        while (pbskit.hasNext()) {
          Integer trk = pbskit.next();
          List<Point> path = pbs.get(trk);
          int pLen = path.size();
          for (int i = 0; i < pLen; i++) {
            Point nextPt = path.get(i);
            if (retval == null) {
              retval = Bounds.initBoundsWithPoint(nextPt);
            } else {
              Bounds.tweakBoundsWithPoint(retval, nextPt);
            }
          }
        }
      }
      return (retval);
    }
       
    public Map<Integer, List<Point>> getPaths(String tag) {
      return (pathBySrc_.get(tag));
    }
      
    public String toString() {
      StringBuffer buf = new StringBuffer();
      Iterator<String> pbsit = pathBySrc_.keySet().iterator();
      while (pbsit.hasNext()) {
        String tag = pbsit.next();
        Map<Integer, List<Point>> pbs = pathBySrc_.get(tag);
        Iterator<Integer> pbskit = pbs.keySet().iterator();
        while (pbskit.hasNext()) {
          Integer trk = pbskit.next();
          List<Point> path = pbs.get(trk);
          buf.append(tag);
          buf.append(": ");
          buf.append(trk);
          buf.append(": ");
          buf.append(path.toString());
          buf.append("\n");
        }
      }
      return (buf.toString());      
    }    
  }
  
  /***************************************************************************
  ** 
  ** A task to perform
  */
  
  private class PerpTask implements Cloneable {
    
    TreeMap<Integer, List<TaggedMinMax>> externalPerpTrackUsage;
    TreeMap<Integer, List<TaggedMinMax>> trackUsage;
    int start; 
    boolean exclusive; 
    boolean antisense; 
    boolean internalIsMin;
    boolean leftTurnTurnsTowardsMin;
    boolean tracksAreYCoord;
    HashSet<MultiInstanceKey> rightTurns;
    HashSet<MultiInstanceKey> leftTurns;
    HashSet<MultiInstanceKey> straight;
    TreeSet<Integer> backTracks;
    TreeSet<Integer> leftTracks;
            
    PerpTask(int solnSide) {      
      leftTurnTurnsTowardsMin = (solnSide == LEFT) || (solnSide == BOTTOM);
      internalIsMin = (solnSide == RIGHT) || (solnSide == BOTTOM);
      tracksAreYCoord = (solnSide == LEFT) || (solnSide == RIGHT);
      antisense = false;
      exclusive = true;  // ignored with antisense true!
      rightTurns = new HashSet<MultiInstanceKey>();
      leftTurns = new HashSet<MultiInstanceKey>();
      straight = new HashSet<MultiInstanceKey>();    
    } 
    
    
    /***************************************************************************
    **
    ** Gotta be able to clone
    */

    public PerpTask clone() {
      try {       
        PerpTask retval = (PerpTask)super.clone();

        retval.externalPerpTrackUsage = new TreeMap<Integer, List<TaggedMinMax>>();
        Iterator<Integer> epit = this.externalPerpTrackUsage.keySet().iterator();
        while (epit.hasNext()) {
          Integer perpTrack = epit.next();
          List<TaggedMinMax> uses = this.externalPerpTrackUsage.get(perpTrack);
          ArrayList<TaggedMinMax> retUses = new ArrayList<TaggedMinMax>();
          retval.externalPerpTrackUsage.put(perpTrack, retUses);
          Iterator<TaggedMinMax> uit = uses.iterator();          
          while (uit.hasNext()) {
            TaggedMinMax used = uit.next();
            TaggedMinMax retTmm = new TaggedMinMax(used);
            retUses.add(retTmm);
          }          
        }
 
        retval.trackUsage = new TreeMap<Integer, List<TaggedMinMax>>();     
        Iterator<Integer> tuit = this.trackUsage.keySet().iterator();
        while (tuit.hasNext()) {
          Integer ebTrackObj = tuit.next();
          List<TaggedMinMax> uses = this.trackUsage.get(ebTrackObj);
          ArrayList<TaggedMinMax> retUses = new ArrayList<TaggedMinMax>();
          retval.trackUsage.put(ebTrackObj, retUses);
          Iterator<TaggedMinMax> uit = uses.iterator();
          while (uit.hasNext()) {
            TaggedMinMax tmm = uit.next();
            TaggedMinMax retTmm = new TaggedMinMax(tmm);
            retUses.add(retTmm);
          }
        }
        
        // Immutable contents, shallow is OK:
        
        retval.rightTurns = (this.rightTurns == null) ? null : new HashSet<MultiInstanceKey>(this.rightTurns);
        retval.leftTurns = (this.leftTurns == null) ? null : new HashSet<MultiInstanceKey>(this.leftTurns);
        retval.straight = (this.straight == null) ? null : new HashSet<MultiInstanceKey>(this.straight);
        retval.backTracks = (this.backTracks == null) ? null : new TreeSet<Integer>(this.backTracks);
        retval.leftTracks = (this.leftTracks == null) ? null : new TreeSet<Integer>(this.leftTracks);   
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    
    /***************************************************************************
    **
    ** Setup the usage
    */     
    
    void setupUsage(int interfaceCoord) {
      externalPerpTrackUsage = (internalIsMin) ? new TreeMap<Integer, List<TaggedMinMax>>() : new TreeMap<Integer, List<TaggedMinMax>>(Collections.reverseOrder());
      start = interfaceCoord + ((internalIsMin) ? -1 : 1);
      return;
    } 
    
    /***************************************************************************
    **
    ** Initialize track routes
    */

    void initTrackRoutes(ButtEndProblem bep) {

      trackUsage = new TreeMap<Integer, List<TaggedMinMax>>(); 
      Iterator<String> ibit = bep.internalBundle_.keySet().iterator();
      while (ibit.hasNext()) {
        String internSrc = ibit.next();
        Set<Integer> ibTrackObj = bep.internalBundle_.get(internSrc);
        Iterator<Integer> ibtoit = ibTrackObj.iterator();
        while (ibtoit.hasNext()) {
          Integer trackObj = ibtoit.next();
          List<TaggedMinMax> uses = new ArrayList<TaggedMinMax>();
          trackUsage.put(trackObj, uses);
          MinMax ismm = (internalIsMin)
            ? new MinMax(Integer.MIN_VALUE, bep.interfaceCoord_ - INTERNAL_INIT_HACK_)
            : new MinMax(bep.interfaceCoord_ + INTERNAL_INIT_HACK_, Integer.MAX_VALUE);
          uses.add(new TaggedMinMax(new MultiInstanceKey(internSrc, trackObj.intValue()), ismm));          
        }
      }

      Iterator<Integer> ebit = bep.externalBundle_.keySet().iterator();
      while (ebit.hasNext()) {
        Integer ebTrackObj = ebit.next();
        MultiInstanceKey extSrcK = bep.externalBundle_.get(ebTrackObj);
        List<TaggedMinMax> uses = trackUsage.get(ebTrackObj);
        if (uses == null) {
          uses = new ArrayList<TaggedMinMax>();
          trackUsage.put(ebTrackObj, uses);
        }
        MinMax esmm = (internalIsMin)
          ? new MinMax(Integer.MAX_VALUE, Integer.MAX_VALUE)
          : new MinMax(Integer.MIN_VALUE, Integer.MIN_VALUE);

        uses.add(new TaggedMinMax(extSrcK, esmm));  
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Figure out right, left, and no turns, as viewed from the external bundles arriving and
    ** routing into the internal bundles.
    */

    void assignTurnDirections(ButtEndProblem bep) {

      // internalBundlex_: String src -> List of Integer traces
      // externalBundle_: Sorted Integer externalTrack -> MultiInstanceKey src

      Iterator<Integer> ebit = bep.externalBundle_.keySet().iterator();
      while (ebit.hasNext()) {
        Integer externTrackObj = ebit.next();
        MultiInstanceKey ebSrcK = bep.externalBundle_.get(externTrackObj);
        int extTrack = externTrackObj.intValue();
        int intTrack = bep.getSingleInternalTrackVal(ebSrcK);
        if (extTrack == intTrack) {
          straight.add(ebSrcK);
        } else if (((leftTurnTurnsTowardsMin) && (extTrack > intTrack)) ||
                   ((!leftTurnTurnsTowardsMin) && (extTrack < intTrack))) {
          leftTurns.add(ebSrcK);
        } else {
          rightTurns.add(ebSrcK);
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Handle direct routing case
    */

    void routeDirect(ButtEndProblem bep, Integer externTrackObj, SpliceSolution retval, boolean isRight) {
      MultiInstanceKey ebSrcK = (MultiInstanceKey)bep.externalBundle_.get(externTrackObj);
      int trackNum = externTrackObj.intValue();
      ArrayList<Point> path = new ArrayList<Point>();
      retval.addPath(ebSrcK, path);
      int intTrack = bep.getSingleInternalTrackVal(ebSrcK);
      MinMax needed = (new MinMax()).init().update(intTrack).update(trackNum);
      TaggedMinMax tNeed = new TaggedMinMax(ebSrcK, needed);
      int avail = getPerpRoute(this, tNeed, isRight);
      path.add((tracksAreYCoord) ? new Point(avail, intTrack) : new Point(intTrack, avail));
      path.add((tracksAreYCoord) ? new Point(avail, trackNum) : new Point(trackNum, avail));
      return;
    } 
    
    /***************************************************************************
    **
    ** Build the track sets.  The backTracks collection is filled with external tracks and 
    ** iterates to the right for sense, left for antisense.  The leftTracks collection is
    ** empty, but will iterate in the reverse direction.
    */

    void buildTrackSets(ButtEndProblem bep) {   
      if (leftTurnTurnsTowardsMin) {
        backTracks = (antisense) ? new TreeSet<Integer>(Collections.reverseOrder()) : new TreeSet<Integer>();
        leftTracks = (antisense) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
      } else {
        backTracks = (antisense) ? new TreeSet<Integer>() : new TreeSet<Integer>(Collections.reverseOrder());
        leftTracks = (antisense) ? new TreeSet<Integer>(Collections.reverseOrder()) : new TreeSet<Integer>();
      }
      backTracks.addAll(bep.externalBundle_.keySet());
      return;
    }   
  }
  
  /***************************************************************************
  ** 
  ** End run answer
  */
  
  private static class EndRunAnswer {
    
    public SortedSet<Integer> availDetours;
    //int endRun;
    //int endIncrement;
   // int leftEndIncrement;
    //int leftMax;           
    //int rightEndIncrement;
    //int rightMax; 
    //int leftDetour;    
   //int rightDetour;    

    EndRunAnswer(SortedSet<Integer> availDetours, int endRun, int endIncrement, int leftDetour, int leftEndIncrement, int leftMax, int rightDetour, int rightEndIncrement, int rightMax) {
      this.availDetours = availDetours;
      //this.endRun = endRun;
      //this.endIncrement = endIncrement;      
      //this.leftEndIncrement = leftEndIncrement;      
      //this.leftMax = leftMax;      
      //this.rightEndIncrement = rightEndIncrement;
      //this.rightMax = rightMax;
      //this.leftDetour = leftDetour;    
      //this.rightDetour = rightDetour;
    }
  }
  
  /***************************************************************************
  ** 
  ** Tag a range
  */
  
  private static class TaggedMinMax {
    
    MultiInstanceKey mik;
    MinMax range;
    
    TaggedMinMax(MultiInstanceKey mik, MinMax range) {
      this.mik = new MultiInstanceKey(mik);
      this.range = (MinMax)range.clone();
    }
    
    TaggedMinMax(TaggedMinMax other) {
      this.mik = new MultiInstanceKey(other.mik);
      this.range = (MinMax)other.range.clone();      
    }    
  }
  
  /***************************************************************************
  ** 
  ** Uses for external bundle members where source shows up multiple times 
  ** (e.g. heading out to multiple target modules)
  */
  
  private static class MultiInstanceKey {
    
    String tag;
    int instance;
    
    MultiInstanceKey(String tag, int instance) {
      this.tag = tag;
      this.instance = instance;      
    }
    
    MultiInstanceKey(MultiInstanceKey other) {
      this.tag = other.tag;
      this.instance = other.instance;      
    } 
    
    public String toString() {
      return ("MultiInstanceKey " + tag + " : " + instance);  
    }
    
    public int hashCode() {
      return (tag.hashCode() + instance);
    }
    
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof MultiInstanceKey)) {
        return (false);
      }
      MultiInstanceKey omik = (MultiInstanceKey)other;
      if (!this.tag.equals(omik.tag)) {
        return (false);
      }
      return (this.instance == omik.instance);
    }
  }
}
