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


package org.systemsbiology.biotapestry.nav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.systemsbiology.biotapestry.app.NavigationChange;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.undo.NavigationChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Controller for user-defined tree path navigation
*/

public class UserTreePathController {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final String INSERT_START         = "insertStart";
  public static final String INSERT_END           = "insertEnd";  
  public static final String INSERT_AFTER_CURRENT = "insertAfterCurrent";     

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  private static final int NO_STOP_CHANGE_ = 0;
  private static final int STOP_MOD_       = 1;  
  private static final int STOP_DROP_      = 2;      
  
  private static final int MODEL_CHECK_    = 0;
  private static final int OVERLAY_CHECK_  = 1;  
  private static final int MODULE_CHECK_   = 2;        
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private String currPathKey_;
  private int currStopIndex_;

  private String lastPathKey_;  
  private HashMap<String, Integer> lastStop_;
  
  private PathNavigationInfo pendingNavInfo_;
  private boolean pendingNavProcessed_;
  private UIComponentSource uics_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public UserTreePathController(UIComponentSource uics) {
    uics_ = uics;
    pendingNavInfo_ = null;
    pendingNavProcessed_ = false;
    lastStop_ = new HashMap<String, Integer>();
    clearControllerState();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   

  /***************************************************************************
  **
  ** Clear current controller state
  ** 
  */
  
  public void clearControllerState() {
    currPathKey_ = null;
    currStopIndex_ = -1;   
    lastPathKey_ = null;  
    lastStop_.clear();
    return;
  }  

  /***************************************************************************
  **
  ** Answer if the current navigation state matches my state
  ** 
  */
  
  public boolean currentModelMatchesState(DataAccessContext dacx) {
    if (currPathKey_ == null) {
      return (true);
    }
    if (currStopIndex_ == -1) {
      return (false);
    }
    
    UserTreePathManager utpm = uics_.getPathMgr();
    UserTreePath currPath = utpm.getPath(currPathKey_);
    UserTreePathStop stop = currPath.getStop(currStopIndex_);
    return (currentModelMatchesStop(stop, dacx));
  }
  
  /***************************************************************************
  **
  ** Answer if the current navigation state matches my state
  ** 
  */
  
  public boolean currentModelMatchesStop(UserTreePathStop stop, DataAccessContext dacx) {
    String genomeKey = dacx.getCurrentGenomeID();
    if (!stop.getGenomeID().equals(genomeKey)) {
      return (false);
    }
    
    String currOverlay = dacx.getOSO().getCurrentOverlay();
    String stopOverlay = stop.getOverlay();
    if (stopOverlay == null) {
      return (currOverlay == null);
    } else if (!stopOverlay.equals(currOverlay)) {
      return (false);
    }

    TaggedSet currModules = dacx.getOSO().getCurrentNetModules();
    TaggedSet stopModules = stop.getModules();
    if (!currModules.equals(stopModules)) {
      return (false);
    }
    
    TaggedSet currReveal = NetOverlayController.normalizedRevealedModules(dacx, genomeKey, currOverlay, currModules, dacx.getOSO().getRevealedModules());
    TaggedSet stopReveal = stop.getRevealed();
    return (currReveal.equals(stopReveal));
  }  
 
  /***************************************************************************
  **
  ** Answer if the current model shows up as a stop on the current path.
  ** If it does, it finds the nearest matching stop.  Else returns Integer.MIN_VALUE.
  ** 
  */
  
  public int currentModelOnCurrentPath(DataAccessContext dacx) {
    if (currPathKey_ == null) {
      return (-1);
    }
    UserTreePathManager utpm = uics_.getPathMgr();
    UserTreePath currPath = utpm.getPath(currPathKey_);
    int maxBelow = Integer.MIN_VALUE;
    int minAtOrAbove = Integer.MAX_VALUE;
    boolean haveAMatch = false;
    int stopCount = getStopCount();
    for (int i = 0; i < stopCount; i++) {
      UserTreePathStop stop = currPath.getStop(i);
      if (currentModelMatchesStop(stop, dacx)) {
        if (i < currStopIndex_) {
          maxBelow = i;
          haveAMatch = true;
        } else {
          minAtOrAbove = i;
          haveAMatch = true;
          // Yes the break belongs here and only here:
          break;
        }
      }
    }
    if (!haveAMatch) {
      return (Integer.MIN_VALUE);
    }
    // Some value will be sane:
    if (maxBelow == Integer.MIN_VALUE) {
      return (minAtOrAbove);
    } else if (minAtOrAbove == Integer.MAX_VALUE) {
      return (maxBelow);
    } else {  
      int diffBelow = currStopIndex_ - maxBelow;
      int diffAbove = minAtOrAbove - currStopIndex_;
      return ((diffBelow < diffAbove) ? maxBelow : minAtOrAbove);
    }
  }
  
  /***************************************************************************
  **
  ** Get last stop for given path
  */
  
  public Integer getLastStop(String pathKey) {
    if (pathKey.equals(currPathKey_)) {
      return (new Integer(currStopIndex_));
    } else {
      return (lastStop_.get(pathKey));  
    }
  }
  
  /***************************************************************************
  **
  ** Called when nav selection is not triggered by us, we need to get
  ** our state in sync.
  ** 
  */
  
  public boolean syncPathControls(NavigationChange nc, DataAccessContext dacx) {
    if (currentModelMatchesState(dacx)) {
      return (false);
    }
    nc.oldUserPathKey = currPathKey_;
    nc.oldUserPathStop = currStopIndex_;
    
    UserTreePathManager utpm = uics_.getPathMgr();
    UserTreePath currPath;
    UserTreePathStop stop;
    nc.odc = new OverlayDisplayChange();
    if ((currPathKey_ != null) && (currStopIndex_ != -1)) {
      currPath = utpm.getPath(currPathKey_);
      stop = currPath.getStop(currStopIndex_);
      nc.odc.oldGenomeID = stop.getGenomeID();
      nc.odc.oldOvrKey = stop.getOverlay();
      nc.odc.oldModKeys = stop.getModules().clone();
      nc.odc.oldRevealedKeys = stop.getRevealed().clone();
    } else {
      nc.odc.oldGenomeID = dacx.getCurrentGenomeID();
      nc.odc.oldOvrKey = null;
      nc.odc.oldModKeys = new TaggedSet();
      nc.odc.oldRevealedKeys = new TaggedSet();
    }    

    int newStop = currentModelOnCurrentPath(dacx);
    
    // Set to NO PATH
    if ((newStop == -1) || (newStop == Integer.MIN_VALUE)) {
      lastStop_.put(currPathKey_, new Integer(currStopIndex_));
      lastPathKey_ = currPathKey_;
      currPathKey_ = null;
      currStopIndex_ = -1;
    // Set to new stop
    } else {
      currStopIndex_ = newStop;      
    } 
    nc.userPathSelection = false;
    nc.userPathSync = true;
    nc.newUserPathKey = currPathKey_;
    nc.newUserPathStop = currStopIndex_;
    
    if ((currPathKey_ != null) && (currStopIndex_ != -1)) {
      currPath = utpm.getPath(currPathKey_);
      stop = currPath.getStop(currStopIndex_);  
      nc.odc.newGenomeID = stop.getGenomeID();
      nc.odc.newOvrKey = stop.getOverlay();
      nc.odc.newModKeys = stop.getModules().clone();
      nc.odc.newRevealedKeys = stop.getRevealed().clone();      
    } else {
      nc.odc.newGenomeID = dacx.getCurrentGenomeID();
      nc.odc.newOvrKey = null;
      nc.odc.newModKeys = new TaggedSet();
      nc.odc.newRevealedKeys = new TaggedSet();
    }    
    int currPathIndex = (currPathKey_ == null) ? -1 : utpm.getPathIndex(currPathKey_);
    // Handles button syncing too:
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return (true);
  }  
  

  /***************************************************************************
  **
  ** Set the current path
  ** 
  */
  
  public void setCurrentPath(String pathKey, DataAccessContext dacx, UndoFactory uFac) {
    if (currPathKey_ != null) {
      lastStop_.put(currPathKey_, new Integer(currStopIndex_));
    }
    setCurrentPath(pathKey, null, null, null, null, dacx, uFac);
    UserTreePathManager mgr = uics_.getPathMgr();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the current path index
  ** 
  */
  
  public int getCurrentPathIndex() {
    UserTreePathManager mgr = uics_.getPathMgr();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    return (currPathIndex + 1);
  }
  
  /***************************************************************************
  **
  ** Go backward in the path
  ** 
  */
  
  public void pathBackward(DataAccessContext dacx, UndoFactory uFac) {
    int stopCount = getStopCount();
    if ((stopCount > 0) && (currStopIndex_ > 0)) {
      PathNavigationInfo pni = new PathNavigationInfo();
      pni.support = uFac.provideUndoSupport("undo.treePathBackward", dacx);
      pni.nav = new NavigationChange();
      pni.nav.userPathSelection = true;
      pni.nav.userPathSync = false;
      
      UserTreePathManager mgr = uics_.getPathMgr();
      UserTreePath currPath = mgr.getPath(currPathKey_);
      pni.nav.oldUserPathKey = currPathKey_;
      pni.nav.newUserPathKey = currPathKey_;
      
      pni.nav.odc = new OverlayDisplayChange();
      UserTreePathStop stop = currPath.getStop(currStopIndex_);
      pni.nav.odc.oldGenomeID = stop.getGenomeID();
      pni.nav.odc.oldOvrKey = stop.getOverlay();
      pni.nav.odc.oldModKeys = stop.getModules().clone();
      pni.nav.odc.oldRevealedKeys = stop.getRevealed().clone();      
 
      pni.nav.oldUserPathStop = currStopIndex_--;
      pni.nav.newUserPathStop = currStopIndex_;
      
      stop = currPath.getStop(currStopIndex_);
      pni.nav.odc.newGenomeID = stop.getGenomeID();
      pni.nav.odc.newOvrKey = stop.getOverlay();
      pni.nav.odc.newModKeys = stop.getModules().clone();
      pni.nav.odc.newRevealedKeys = stop.getRevealed().clone();
          
      selectTreePath(stop.getGenomeID(), stop.getOverlay(), stop.getModules(), stop.getRevealed(), pni, dacx);      
    }   
    return;
  }  
  
  /***************************************************************************
  **
  ** Go forward in the path
  ** 
  */
  
  public void pathForward(DataAccessContext dacx, UndoFactory uFac) {
    int stopCount = getStopCount();
    if ((stopCount > 0) && (currStopIndex_ < (stopCount - 1))) {
      PathNavigationInfo pni = new PathNavigationInfo();
      pni.support = uFac.provideUndoSupport("undo.treePathForward", dacx);
      pni.nav = new NavigationChange();
      pni.nav.userPathSelection = true;
      pni.nav.userPathSync = false;
      
      UserTreePathManager mgr = uics_.getPathMgr();
      UserTreePath currPath = mgr.getPath(currPathKey_);
      pni.nav.oldUserPathKey = currPathKey_;
      pni.nav.newUserPathKey = currPathKey_;
      
      pni.nav.odc = new OverlayDisplayChange();
      UserTreePathStop stop = currPath.getStop(currStopIndex_);
      pni.nav.odc.oldGenomeID = stop.getGenomeID();
      pni.nav.odc.oldOvrKey = stop.getOverlay();
      pni.nav.odc.oldModKeys = stop.getModules().clone();
      pni.nav.odc.oldRevealedKeys = stop.getRevealed().clone();      
      
      pni.nav.oldUserPathStop = currStopIndex_++;
      pni.nav.newUserPathStop = currStopIndex_;
 
      stop = currPath.getStop(currStopIndex_);
      pni.nav.odc.newGenomeID = stop.getGenomeID();
      pni.nav.odc.newOvrKey = stop.getOverlay();
      pni.nav.odc.newModKeys = stop.getModules().clone();
      pni.nav.odc.newRevealedKeys = stop.getRevealed().clone();      
      selectTreePath(stop.getGenomeID(), stop.getOverlay(), stop.getModules(), stop.getRevealed(), pni, dacx);
    }   
    return;
  } 

  /***************************************************************************
  **
  ** Answer do we have a path
  ** 
  */
  
  public boolean hasAPath() {
    UserTreePathManager mgr = uics_.getPathMgr();
    return (mgr.getPathCount() > 0);
  }    
  
  /***************************************************************************
  **
  ** Answer can we go forward
  ** 
  */
  
  public boolean canGoForward() {
    int stopCount = getStopCount();
    return (pathIsSelected() && (stopCount > 0) && (currStopIndex_ < (stopCount - 1)));
  }  
  
  
  /***************************************************************************
  **
  ** Answer can we go backward
  ** 
  */
  
  public boolean canGoBackward() {
    int stopCount = getStopCount();
    return (pathIsSelected() && (stopCount > 0) && (currStopIndex_ > 0));
  }    
  
  /***************************************************************************
  **
  ** Answer if a path is selected
  ** 
  */
  
  public boolean pathIsSelected() {
    return (currPathKey_ != null);
  } 
  
  /***************************************************************************
  **
  ** Answer if path has a stop
  */
  
  public boolean pathHasAStop() {
    int stopCount = getStopCount();
    return (pathIsSelected() && (stopCount > 0));
  }
  
  /***************************************************************************
  **
  ** Answer if current path has only one stop
  */
  
  public boolean pathHasOnlyOneStop() {
    int stopCount = getStopCount();
    return (pathIsSelected() && (stopCount == 1));
  }  
  
  /***************************************************************************
  **
  ** Get the current (or last) path
  */
  
  public String getLastPath() {
    return ((currPathKey_ != null) ? currPathKey_ : lastPathKey_);
  }
  
  /***************************************************************************
  **
  ** Add a stop to the current path.  May be a change to navigation.
  */
  
  public void addAStop(String addToPathKey, String insertMode, DataAccessContext dacx, UndoFactory uFac) {
    String key = dacx.getCurrentGenomeID();
    String ovrKey = dacx.getOSO().getCurrentOverlay();
    TaggedSet mods = dacx.getOSO().getCurrentNetModules();
    TaggedSet revs = dacx.getOSO().getRevealedModules();    
    
    ArrayList<UserTreePathChangeCmd> postNavs = new ArrayList<UserTreePathChangeCmd>();
    UserTreePathManager mgr = uics_.getPathMgr();
    UserTreePath targetPath = mgr.getPath(addToPathKey);
    int newIndexVal;
    if (insertMode.equals(INSERT_START)) {
      newIndexVal = 0;
    } else if (insertMode.equals(INSERT_END)) {
      newIndexVal = targetPath.getStopCount();
    } else if (insertMode.equals(INSERT_AFTER_CURRENT)) {
      // This routine correctly places the stop after the current one if we are
      // currently on the path (i.e. duplicating the stop...)
      Integer lastStopVal = getLastStop(addToPathKey);      
      newIndexVal = (lastStopVal == null) ? targetPath.getStopCount() : lastStopVal.intValue() + 1;
    } else {
      throw new IllegalArgumentException();
    } 

    UserTreePathChange change = mgr.addPathStop(addToPathKey, key, newIndexVal, ovrKey, mods, revs);
    UndoSupport support = uFac.provideUndoSupport("undo.treePathAddStop", dacx);
    postNavs.add(new UserTreePathChangeCmd(dacx, change));
    setCurrentPath(addToPathKey, new Integer(newIndexVal), support, postNavs, null, dacx, uFac);

    uics_.getPathControls().updateUserPathActions();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a new path.  Adds a stop at the same time.  No change to navigation.
  */
  
  public void addAPath(String newName, DataAccessContext dacx, UndoFactory uFac) {
    String key = dacx.getCurrentGenomeID();
    String ovrKey = dacx.getOSO().getCurrentOverlay();
    TaggedSet mods = dacx.getOSO().getCurrentNetModules();
    TaggedSet revs = dacx.getOSO().getRevealedModules();
       
    UserTreePathManager mgr = uics_.getPathMgr();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    UserTreePathChange change = mgr.addPath(newName, ++currPathIndex);
    UndoSupport support = uFac.provideUndoSupport("undo.treePathCreate", dacx);
    UserTreePathChangeCmd utpcc = new UserTreePathChangeCmd(dacx, change);
    support.addEdit(utpcc);
    lastPathKey_ = currPathKey_;
    currPathKey_ = mgr.getPathKey(currPathIndex);
    currStopIndex_ = -1;
    change = mgr.addPathStop(currPathKey_, key, ++currStopIndex_, ovrKey, mods, revs);
    utpcc = new UserTreePathChangeCmd(dacx, change);
    support.addEdit(utpcc);
    support.finish();
    
    uics_.getPathControls().updateUserPathActions();
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  }  
 
  /***************************************************************************
  **
  ** Delete the current path. May be a navigation change
  */
  
  public void deleteCurrentPath(DataAccessContext dacx, UndoFactory uFac) {
    OverlayDisplayChange odc = stashForDeletion(dacx);
    ArrayList<UserTreePathChangeCmd> postNavs = new ArrayList<UserTreePathChangeCmd>();
    UserTreePathManager mgr = uics_.getPathMgr();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    UserTreePathChange change = mgr.deletePath(currPathKey_);
    lastStop_.remove(currPathKey_);
    currPathIndex = (currPathIndex == -1) ? -1 : (currPathIndex - 1);
    UndoSupport support = uFac.provideUndoSupport("undo.treePathDelete", dacx);    
    postNavs.add(new UserTreePathChangeCmd(dacx, change));
    String newKey = (currPathIndex == -1) ? null : mgr.getPathKey(currPathIndex);
    setCurrentPath(newKey, null, support, postNavs, odc, dacx, uFac);

    uics_.getPathControls().updateUserPathActions();
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  }    
  
  /***************************************************************************
  **
  ** When doing a deletion, we need to remember some undo state for overlays
  ** before doing the deletion.
  */
  
  private OverlayDisplayChange stashForDeletion(DataAccessContext dacx) { 
   
    OverlayDisplayChange retval = new OverlayDisplayChange();

    UserTreePathManager mgr = uics_.getPathMgr();
    UserTreePath currPath;
    UserTreePathStop stop; 
    if ((currPathKey_ != null) && (currStopIndex_ != -1) && (mgr.getPath(currPathKey_) != null)) {
      currPath = mgr.getPath(currPathKey_);
      stop = currPath.getStop(currStopIndex_);
      retval.oldGenomeID = stop.getGenomeID();
      retval.oldOvrKey = stop.getOverlay();
      retval.oldModKeys = stop.getModules().clone();
      retval.oldRevealedKeys = stop.getRevealed().clone();
    } else {
      retval.oldGenomeID = dacx.getCurrentGenomeID();
      retval.oldOvrKey = null;
      retval.oldModKeys = new TaggedSet();
      retval.oldRevealedKeys = new TaggedSet();
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Delete the current stop.  May be a nav change.
  */
  
  public void deleteCurrentStop(DataAccessContext dacx, UndoFactory uFac) {
    OverlayDisplayChange odc = stashForDeletion(dacx);
    UserTreePathManager mgr = uics_.getPathMgr();
    int stopCount = getStopCount();
    boolean deletePathToo = (stopCount == 1);
    ArrayList<UserTreePathChangeCmd> postNavs = new ArrayList<UserTreePathChangeCmd>();
    UserTreePathChange change = mgr.deletePathStop(currPathKey_, currStopIndex_);
    
    UndoSupport support = uFac.provideUndoSupport("undo.treePathDeleteStop", dacx);   
    postNavs.add(new UserTreePathChangeCmd(dacx, change));
    int newStopIndexVal = ((currStopIndex_ == 0) && (stopCount > 1)) ? 0 : (currStopIndex_ - 1);
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    String pathKey = ((currPathIndex == -1) || (currStopIndex_ == -1)) ? null : currPathKey_;
    if (deletePathToo) {
      UserTreePathChange change2 = mgr.deletePath(currPathKey_);
      postNavs.add(new UserTreePathChangeCmd(dacx, change2));
      lastStop_.remove(currPathKey_);
      currPathIndex = (currPathIndex == -1) ? -1 : (currPathIndex - 1);
      newStopIndexVal = -1;
      pathKey = null;
    }
    Integer newStopIndex = new Integer(newStopIndexVal);
    setCurrentPath(pathKey, newStopIndex, support, postNavs, odc, dacx, uFac);

    uics_.getPathControls().updateUserPathActions();
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);    
    return;
  }

  /***************************************************************************
  **
  ** Handle undo recording for arbitrary stop drop operations
  ** 
  */
  
  public UserTreePathControllerChange recordStateForUndo(boolean doUndo) {
    UserTreePathControllerChange retval = new UserTreePathControllerChange();
    retval.forUndo = doUndo;
    retval.currPathKey = currPathKey_;
    retval.lastPathKey = lastPathKey_;
    retval.currStopIndex = currStopIndex_;
    retval.lastStop = new HashMap<String, Integer>(lastStop_);
    return (retval);
  }

  /***************************************************************************
  **
  ** Drop all the path stops on the given model
  ** 
  */
  
  public UserTreePathChange[] dropStopsOnModel(String modelKey) {
    return (stopDropperCore(modelKey, null, null, MODEL_CHECK_));
  }
  
  /***************************************************************************
  **
  ** Drop all the path stops on the given overlay
  ** 
  */
  
  public UserTreePathChange[] dropStopsOnOverlay(String modelKey, String ovrKey) {
    return (stopDropperCore(modelKey, ovrKey, null, OVERLAY_CHECK_));
  } 
  
  /***************************************************************************
  **
  ** If we drop a network module, we need to remove it from all user path stops,
  ** possibly deleting some stops as a result.
  ** 
  */
  
  public UserTreePathChange[] dropOrChangeStopsOnModules(String modelKey, String ovrKey, String modKey) {
    return (stopDropperCore(modelKey, ovrKey, modKey, MODULE_CHECK_));
  }   

  /***************************************************************************
  **
  ** Test for stop drop (or modify):
  ** 
  */
  
  private int stopDropperTest(UserTreePathStop currStop, String modelKey, String ovrKey, String modKey, int forWhat) {
    if (!currStop.getGenomeID().equals(modelKey)) {
      return (NO_STOP_CHANGE_);
    }
    if (forWhat == MODEL_CHECK_) {
      return (STOP_DROP_);
    }
    String currStopOverlay = currStop.getOverlay();
    if ((currStopOverlay == null) || !currStopOverlay.equals(ovrKey)) {
      return (NO_STOP_CHANGE_);
    }
    if (forWhat == OVERLAY_CHECK_) {
      return (STOP_DROP_);
    } 
    
    if (forWhat != MODULE_CHECK_) {
      throw new IllegalArgumentException();
    }
    // Note that since revealed modules are a subset of stop modules, we don't need to check it separately.
    TaggedSet tset = currStop.getModules();
    boolean contains = tset.set.contains(modKey);
    if (!contains) {
      return (NO_STOP_CHANGE_);
    } else {
      return ((tset.set.size() == 1) ? STOP_DROP_ : STOP_MOD_);
    }
  }
  
  /***************************************************************************
  **
  ** Drop path stops.  Common routine.
  ** 
  */
  
  private UserTreePathChange[] stopDropperCore(String modelKey, String ovrKey, String modKey, int forWhat) {
    UserTreePathManager mgr = uics_.getPathMgr();
    ArrayList<UserTreePathChange> postNavs = new ArrayList<UserTreePathChange>();
    int pathNum = mgr.getPathCount();
    int pathIndex = 0;
    while (pathIndex < pathNum) {
      String pathKey = mgr.getPathKey(pathIndex);
      UserTreePath currPath = mgr.getPath(pathKey);
      int stopNum = currPath.getStopCount();
      int stopIndex = 0;
      while (stopIndex < stopNum) {
        UserTreePathStop currStop = currPath.getStop(stopIndex);
        int doWhat = stopDropperTest(currStop, modelKey, ovrKey, modKey, forWhat);
        if (doWhat == STOP_DROP_) {
          postNavs.add(mgr.deletePathStop(pathKey, stopIndex));
          fixupLastStops(pathKey, stopIndex);
          if (pathKey.equals(currPathKey_)) {
            if (stopIndex == currStopIndex_) { // dropping current view
              currPathKey_ = null;
              lastPathKey_ = null;
              currStopIndex_ = -1;
            } else if (stopIndex < currStopIndex_) {
              currStopIndex_--;
            }
          }
          stopNum--;
        } else if (doWhat == STOP_MOD_) {
          postNavs.add(mgr.dropPathStopModule(pathKey, stopIndex, modKey));
          stopIndex++;
        } else {
          stopIndex++;
        }
      }
      if (stopNum == 0) {
        postNavs.add(mgr.deletePath(pathKey));
        lastStop_.remove(pathKey);
        if (pathKey.equals(currPathKey_)) {
          currPathKey_ = null;
          lastPathKey_ = null;
          currStopIndex_ = -1;
        }
        pathNum--;
      } else {
        pathIndex++;
      }
    }
    
    uics_.getPathControls().updateUserPathActions();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    
    return (postNavs.toArray(new UserTreePathChange[postNavs.size()]));
  }  
  
  /***************************************************************************
  **
  ** Keep last stops consistent with index changes
  ** 
  */
  
  private void fixupLastStops(String pathKey, int deletedStop) {
    if (pathKey == null) {
      return;
    }
    Integer stopNum = lastStop_.get(pathKey);
    if (stopNum == null) {
      return;
    }
    int stopNumVal = stopNum.intValue();
    if (stopNumVal == deletedStop) {
      lastStop_.remove(pathKey);
    } else if (stopNumVal > deletedStop) {
      lastStop_.put(pathKey, new Integer(stopNumVal - 1));
    }
    return;
  }

  /***************************************************************************
  **
  ** Replace model key for all the path stops on the given model
  ** 
  */
  
  public UserTreePathChange[] replaceStopsOnModel(String oldModelKey, String newModelKey) {
    UserTreePathManager mgr = uics_.getPathMgr();
    ArrayList<UserTreePathChange> postNavs = new ArrayList<UserTreePathChange>();
    int pathNum = mgr.getPathCount();
    for (int i = 0; i < pathNum; i++) {
      String pathKey = mgr.getPathKey(i);
      UserTreePath currPath = mgr.getPath(pathKey);
      int stopNum = currPath.getStopCount();
      for (int j = 0; j < stopNum; j++) {
        UserTreePathStop currStop = currPath.getStop(j);
        if (currStop.getGenomeID().equals(oldModelKey)) {
          postNavs.add(mgr.replacePathStop(pathKey, j, newModelKey));
        }
      }
    }    
    return (postNavs.toArray(new UserTreePathChange[postNavs.size()]));
  }  

  /***************************************************************************
  **
  ** Get a pending nav change (will be null if we are not triggering a nav change)
  */
  
  public PathNavigationInfo getPendingNavUndo() {
    pendingNavProcessed_ = true;
    return (pendingNavInfo_);
  }
  
  /***************************************************************************
  **
  ** Undo a navigation change
  */
  
  public void changeUndo(NavigationChange undo) {
    if (!undo.userPathSelection && !undo.userPathSync) {
      return;
    }
    changeGuts(undo.oldUserPathKey, undo.oldUserPathStop);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NavigationChange redo) {
    if (!redo.userPathSelection && !redo.userPathSync) {
      return;
    }
    changeGuts(redo.newUserPathKey, redo.newUserPathStop);
    return; 
  }  

  /***************************************************************************
  **
  ** get the stop count
  */
  
  private int getStopCount() {
    if (currPathKey_ == null) {
      return (0);
    }
    UserTreePathManager utpm = uics_.getPathMgr();
    UserTreePath currPath = utpm.getPath(currPathKey_);
    return (currPath.getStopCount());         
  }
  
  /***************************************************************************
  **
  ** Undo/Redo a change
  */
  
  private void changeGuts(String userPathKey, int userPathStop) {
    lastPathKey_ = null;
    currPathKey_ = userPathKey;
    currStopIndex_ = userPathStop;
    uics_.getPathControls().updateUserPathActions();
    UserTreePathManager mgr = uics_.getPathMgr();      
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return; 
  }    
  
  /***************************************************************************
  **
  ** Undo a path change
  */
  
  public void pathChangeUndo(UserTreePathChange undo) {
    UserTreePathManager mgr = uics_.getPathMgr();
    if (mgr.changeUndo(undo)) {
      currPathKey_ = null;
    }
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);
    uics_.getPathControls().updateUserPathActions();
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void pathChangeRedo(UserTreePathChange redo) {   
    UserTreePathManager mgr = uics_.getPathMgr();
    if (mgr.changeRedo(redo)) {
      currPathKey_ = null;
    }
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_);    
    uics_.getPathControls().updateUserPathActions(); 
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);
    return;
  } 
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void controllerChangeUndo(UserTreePathControllerChange undo) {
    if (undo.forUndo) {
      restoreStateForUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void controllerChangeRedo(UserTreePathControllerChange undo) {
    if (!undo.forUndo) {
      restoreStateForUndo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle undo restoration for arbitrary stop drop operations
  ** 
  */
  
  private void restoreStateForUndo(UserTreePathControllerChange undo) {
    currPathKey_ = undo.currPathKey;
    lastPathKey_ = undo.lastPathKey;
    currStopIndex_ = undo.currStopIndex;
    lastStop_ = new HashMap<String, Integer>(undo.lastStop);
    UserTreePathManager mgr = uics_.getPathMgr();
    int currPathIndex = (currPathKey_ == null) ? -1 : mgr.getPathIndex(currPathKey_); 
    uics_.getPathControls().updateUserPathActions();
    uics_.getPathControls().setCurrentUserPath(currPathIndex + 1);    
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to transmit undo info for path-driven navigation
  */
  
  public static class PathNavigationInfo {
    public UndoSupport support;
    public NavigationChange nav;
    private List<UserTreePathChangeCmd> postNavs; //UserTreePathChangeCmd
    
    public void finish() {
      if (postNavs != null) {
        Iterator<UserTreePathChangeCmd> pnit = postNavs.iterator();
        while (pnit.hasNext()) {
          UserTreePathChangeCmd cc = pnit.next();
          if (cc != null) {
            support.addEdit(cc);
          }
        }
      }
      support.finish();
      return;
    } 
    
    public void addPostNav(UserTreePathChangeCmd cc) {
      if (postNavs == null) {
        postNavs = new ArrayList<UserTreePathChangeCmd>();
      }
      postNavs.add(cc);
      return;
    } 
    
    public void setAllPostNavs(List<UserTreePathChangeCmd> postNavs) {
      this.postNavs = postNavs;
      return;
    }     
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For insertion mode combo boxes
  */

  public static Vector<ObjChoiceContent> insertionOptions(DataAccessContext dacx) {
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    ResourceManager rMan = dacx.getRMan();
    retval.add(new ObjChoiceContent(rMan.getString("pathInsertOptions." + INSERT_AFTER_CURRENT), INSERT_AFTER_CURRENT));   
    retval.add(new ObjChoiceContent(rMan.getString("pathInsertOptions." + INSERT_END), INSERT_END));
    retval.add(new ObjChoiceContent(rMan.getString("pathInsertOptions." + INSERT_START), INSERT_START));
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ObjChoiceContent choiceForOption(DataAccessContext dacx, String option) {
    return (new ObjChoiceContent(dacx.getRMan().getString("pathInsertOptions." + option), option));
  }    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the current path
  ** 
  */
  
  private void setCurrentPath(String pathKey, Integer newStopIndex, 
                              UndoSupport support, List<UserTreePathChangeCmd> postNavs, 
                              OverlayDisplayChange precalcOdc, DataAccessContext dacx, UndoFactory uFac) {
    PathNavigationInfo pni = new PathNavigationInfo();
    pni.support = (support == null) ? uFac.provideUndoSupport("undo.chooseTreePath", dacx) : support;
    if (postNavs != null) {
      pni.setAllPostNavs(postNavs);
    }
    pni.nav = new NavigationChange();
    pni.nav.userPathSelection = true;
    pni.nav.userPathSync = false;
    pni.nav.oldUserPathKey = currPathKey_;
    pni.nav.oldUserPathStop = currStopIndex_;
    
    UserTreePathManager utpm = uics_.getPathMgr();
    
    //
    // Note that to set up undo for overlays, we interrogate stops and paths.  When
    // doing a deletion, they don't exist anymore.  So for deletions, we figure out\
    // the overlay undo beforehand!
    //
    
    if (precalcOdc != null) {
      pni.nav.odc = precalcOdc;
    } else {
      pni.nav.odc = new OverlayDisplayChange();
      if ((currPathKey_ != null) && (currStopIndex_ != -1)) {
        UserTreePath currPath = utpm.getPath(currPathKey_);
        UserTreePathStop stop = currPath.getStop(currStopIndex_);
        pni.nav.odc.oldGenomeID = stop.getGenomeID();
        pni.nav.odc.oldOvrKey = stop.getOverlay();
        pni.nav.odc.oldModKeys = stop.getModules().clone();
        pni.nav.odc.oldRevealedKeys = stop.getRevealed().clone();
      } else {
        pni.nav.odc.oldGenomeID = dacx.getCurrentGenomeID();
        pni.nav.odc.oldOvrKey = null;
        pni.nav.odc.oldModKeys = new TaggedSet();
        pni.nav.odc.oldRevealedKeys = new TaggedSet();
      }
    }
 
    lastPathKey_ = pathKey;
    
    if (pathKey == null) {
      currPathKey_ = null;
      currStopIndex_ = -1;
      pni.nav.newUserPathKey = currPathKey_;
      pni.nav.newUserPathStop = currStopIndex_;
      pni.nav.odc.newGenomeID = dacx.getCurrentGenomeID();
      pni.nav.odc.newOvrKey = null;
      pni.nav.odc.newModKeys = new TaggedSet();
      pni.nav.odc.newRevealedKeys = new TaggedSet();      
      NavigationChangeCmd ncc = new NavigationChangeCmd(dacx, pni.nav);
      pni.support.addEdit(ncc);
      pni.finish();
      return;
    }
    
    currPathKey_ = pathKey;
    UserTreePath currPath = utpm.getPath(currPathKey_);
    UiUtil.fixMePrintout("See null ptr here if we choose path stop while on tab without path defined (Jchooser not changing)");
    int currStopCount = currPath.getStopCount();
    pni.nav.newUserPathKey = currPathKey_;
    
    if (currStopCount > 0) {
      if (newStopIndex == null) {
        Integer lastStopForPath = lastStop_.get(currPathKey_);
        currStopIndex_ = (lastStopForPath == null) ? 0 : lastStopForPath.intValue();
      } else {
        currStopIndex_ = newStopIndex.intValue();
      }
      pni.nav.newUserPathStop = currStopIndex_;
      UserTreePathStop stop = currPath.getStop(currStopIndex_);
      pni.nav.odc.newGenomeID = stop.getGenomeID();
      pni.nav.odc.newOvrKey = stop.getOverlay();
      pni.nav.odc.newModKeys = stop.getModules().clone();
      pni.nav.odc.newRevealedKeys = stop.getRevealed().clone();      
      
      selectTreePath(stop.getGenomeID(), stop.getOverlay(), stop.getModules(), stop.getRevealed(), pni, dacx);
    } else {
      currStopIndex_ = -1;
      pni.nav.newUserPathStop = currStopIndex_;
      pni.nav.odc.newGenomeID = dacx.getCurrentGenomeID();
      pni.nav.odc.newOvrKey = null;
      pni.nav.odc.newModKeys = new TaggedSet();
      pni.nav.odc.newRevealedKeys = new TaggedSet();
      NavigationChangeCmd ncc = new NavigationChangeCmd(dacx, pni.nav);
      pni.support.addEdit(ncc);
      pni.finish();
    }
    return;
  }   
   
  /***************************************************************************
  **
  ** Transmit to nav system
  ** 
  */
  
  private void selectTreePath(String modelKey, String ovrKey, TaggedSet modules, 
                              TaggedSet revealed, PathNavigationInfo pni, DataAccessContext dacx) {
    pendingNavInfo_ = pni;
    pendingNavProcessed_ = false;
    StartupView suv = new StartupView(modelKey, ovrKey, modules, revealed, null);  
    SetCurrentModel.StepState.installStartupView(uics_, dacx, false, suv);
    // 
    // If the path we sent didn't actually change the model, then we need to
    // make sure that the path selection change gets registered.
    //
    if (!pendingNavProcessed_) {
      NavigationChangeCmd ncc = new NavigationChangeCmd(dacx, pni.nav);
      pni.support.addEdit(ncc);
      pni.finish();
    }
    pendingNavInfo_ = null;
    pendingNavProcessed_ = false;  
    return;
  }   
}
