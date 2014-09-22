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

package org.systemsbiology.biotapestry.nav;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.NavigationChange;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DesktopControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.view.Zoom;
import org.systemsbiology.biotapestry.cmd.undo.NavigationChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayDisplayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.event.OverlayDisplayChangeEvent;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NetOverlayControlPanel;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleAlphaBuilder;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.FixedJComboBox;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.TaggedString;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Controller for net overlay/module installation
*/

public class NetOverlayController {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int NO_HIDING       = 0x00;
  public static final int LINKS_NOT_SHOWN = 0x01;
  public static final int OPAQUE_OVERLAY  = 0x02;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private BTState appState_;
  private DynamicDataAccessContext dacxDyn_;
  
  private JComboBox netOverlaysCombo_;
  private CheckBoxList netModuleChoices_;
  private FixedJButton allButtonA_;
  private FixedJButton allButtonS_;  
  private FixedJButton revAllButton_;
  private FixedJButton hideAllButton_;  
  private FixedJButton noneButtonA_;  
  private FixedJButton noneButtonS_;  
  private FixedJButton zoomButtonA_;  
  private FixedJButton zoomButtonS_;  
  private CardLayout myCard_;
  private JPanel buttonPanel_;
  private JLabel ovrLabel_;
  private JLabel modLabel_;
  private JLabel levelLabel_;  
  private JPanel ovrPanel_;

  private boolean ovrEnabled_;
  private boolean downStrEnabled_;
  private boolean pushedAllowLevel_;  
  private boolean pushed_;
  private boolean zoomCanShow_;
  private boolean showingRevControls_;
  private boolean activeRevControls_;
  
  private boolean managingOverlayControls_;
  private NetOverlayControlPanel nocp_;
  
  private HashMap<String, TaggedString> lastSeenOverlayPerOwner_;
  private HashMap<String, TaggedSet> lastSeenModulePerOverlay_;
  private HashMap<String, TaggedSet> lastSeenRevealedPerOverlay_;  
  
  private String pendingGenomeID_;
  private String pendingOvrKey_;
  private TaggedSet pendingModKeys_;  
  private TaggedSet pendingRevealedKeys_; 
  private HashMap<FlowMeister.FlowKey, Boolean> buttonStat_;
  private NetModuleAlphaBuilder alphaCalc_;
  
  // PRIVATE CONSTANT 
  
  private final static int INITIALIZED_ = 1;  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetOverlayController(BTState appState, DynamicDataAccessContext dacx) {
    appState_ = appState.setNetOverlayController(this);
    dacxDyn_ = dacx;  
    ovrEnabled_ = false;
    downStrEnabled_ = false;
    pushed_ = false;
    zoomCanShow_ = false;
    showingRevControls_ = false;
    activeRevControls_ = false;
    //
    // These guys used to be internal to the UI component. Now we keep them
    // and let the UI component use them, if there IS a UI component.
    //
    alphaCalc_ = new NetModuleAlphaBuilder();
    appState_.installCurrentSettings(new NetModuleFree.CurrentSettings());
    
    lastSeenOverlayPerOwner_ = new HashMap<String, TaggedString>();
    lastSeenModulePerOverlay_ = new HashMap<String, TaggedSet>(); 
    lastSeenRevealedPerOverlay_ = new HashMap<String, TaggedSet>(); 
    buttonStat_ = new HashMap<FlowMeister.FlowKey, Boolean>();
    if (!appState_.isHeadless()) {
      // nocp_ is gonna be null in headless mode!
      init(dacx);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   

  /***************************************************************************
  **
  ** Get button enabled status
  */ 
 
  public Map<FlowMeister.FlowKey, Boolean> getButtonEnables() {
    return (buttonStat_);
  }
  
  /***************************************************************************
  **
  ** Set the intensity slider value (0 - 100) programmatically.  Note that,
  ** like zooming, this is not an operation that is tracked via undo!
  ** 
  */  
  
  public void setSliderValue(int slideVal) {
    if (nocp_ != null) {
      nocp_.setSliderValue(slideVal);
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Answer if anybody exists who needs to see us!
  ** 
  */
  
  public boolean aModelHasOverlays(DataAccessContext dacx) {
    return (dacx.fgho.overlayExists());
  }
  
  /***************************************************************************
  **
  ** Answer if we are masking contents
  ** 
  */
  
  public boolean maskingIsActive() {
    return (showingRevControls_ && activeRevControls_);
  }  
 
  /***************************************************************************
  **
  ** Handle model state changes
  ** 
  */
  
  public void checkForChanges(DataAccessContext dacx) {
    syncControlState(true, dacx.oso.getCurrentOverlay(), 
                     true, new TaggedSet(dacx.oso.getCurrentNetModules()),
                           new TaggedSet(dacx.oso.getRevealedModules()), null, dacx);
    return;
  }
  
  /***************************************************************************
  **
  ** Slider position can disable reveal/hide buttons when opaque intensity
  ** is too low.
  ** 
  */
  
  public void checkMaskState(boolean maybeMasking, DataAccessContext dacx) {
    activeRevControls_ = maybeMasking;
    Boolean pushedLevel = (pushed_) ? new Boolean(pushedAllowLevel_) : null;
    downstreamSetEnabled(downStrEnabled_ && !pushed_, pushedLevel, dacx);
    ovrPanel_.invalidate();
    ovrPanel_.validate();
    netModuleChoices_.repaint(); // get those circles drawn in grey ASAP!
    return;
  }
  
  /***************************************************************************
  **
  ** Clear current controller state.  Only acceptable during non-undo/redo cases!
  ** 
  */
  
  public void resetControllerState(DataAccessContext dacx) {
    TaggedSet ts = new TaggedSet();
    TaggedSet ts2 = new TaggedSet();
    pushDownState(null, ts, ts2, dacx);
    lastSeenOverlayPerOwner_.clear();
    lastSeenModulePerOverlay_.clear();
    lastSeenRevealedPerOverlay_.clear();
    return;
  }

  /***************************************************************************
  **
  ** Preload settings for next genome to show up (used for paths/display init):
  */ 
    
  public void preloadForNextGenome(String genomeID, String ovrKey, TaggedSet modKeys, TaggedSet revealedKeys) {
    pendingGenomeID_ = genomeID;
    pendingOvrKey_ = ovrKey;
    pendingModKeys_ = modKeys.clone();
    pendingRevealedKeys_ = revealedKeys.clone();
    return;
  }    
    
  /***************************************************************************
  **
  ** Answers if we have a pending preload
  */ 
    
  public boolean hasPreloadForNextGenome(String genomeID) {
    return ((pendingGenomeID_ != null) && pendingGenomeID_.equals(genomeID));
  }  

  /***************************************************************************
  **
  ** Set for a new genome
  */ 
    
  public void setForNewGenome(String genomeID, UndoSupport support, OverlayDisplayChange odc, DataAccessContext dacx) {
    String useOvrKey; 
    TaggedSet modsForOvr;
    TaggedSet revsForOvr;
    if (hasPreloadForNextGenome(genomeID)) {
      useOvrKey = pendingOvrKey_; 
      modsForOvr = pendingModKeys_;
      revsForOvr = pendingRevealedKeys_;
      pendingOvrKey_ = null;
      pendingModKeys_ = null;
      pendingRevealedKeys_ = null;
      pendingGenomeID_ = null;      
    } else {
      if (pendingGenomeID_ != null) {
        pendingOvrKey_ = null;
        pendingModKeys_ = null;
        pendingRevealedKeys_ = null;
        pendingGenomeID_ = null;      
      }
      StartupView sv = getCachedStateForGenome(genomeID, dacx);
      useOvrKey = sv.getOverlay();
      modsForOvr = sv.getModules();
      revsForOvr = sv.getRevealedModules();
    }
    OdcAndSccBundle oasb = setFullStateGuts(useOvrKey, modsForOvr, revsForOvr, odc, dacx); 
    if ((oasb != null) && (support != null) && (oasb.odc != null)) {
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, oasb.odc);
      support.addEdit(nodcc);
      if ((oasb.bundle != null) && (oasb.bundle.cmd != null)) {
        support.addEdit(oasb.bundle.cmd);
        support.addEvent(oasb.bundle.event);
      }    
    }    
    return;
  }

  /***************************************************************************
  **
  ** return the cached overlay state for the given genome
  */ 
    
  public StartupView getCachedStateForGenome(String genomeID, DataAccessContext dacx) {  
    NetOverlayOwner owner = (genomeID == null) ? null: dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeID);
    TaggedString taggedOvr = (owner == null) ? null: (TaggedString)lastSeenOverlayPerOwner_.get(owner.getID());     
    TaggedSet modsForOvr = new TaggedSet();
    TaggedSet revsForOvr = new TaggedSet();
    String useOvrKey;
      // First time seen; use that preference:
    if ((owner != null) && (taggedOvr == null)) {
      useOvrKey = owner.getFirstViewPreference(modsForOvr, revsForOvr);
      // Nothing done at the moment with revsForOvr;
    } else if ((genomeID != null) && (taggedOvr != null) && (taggedOvr.value != null)) {
      useOvrKey = taggedOvr.value;
      modsForOvr = lastSeenModulePerOverlay_.get(useOvrKey);
      if (modsForOvr == null) {
        modsForOvr = getAllModuleChoiceSet(getModuleChoices(genomeID, useOvrKey, dacx));     
      }
      revsForOvr = lastSeenRevealedPerOverlay_.get(useOvrKey);
      if (revsForOvr == null) {
        revsForOvr = new TaggedSet();
      }
    } else {
      useOvrKey = null;
    } 
    return (new StartupView(genomeID, useOvrKey, modsForOvr, revsForOvr)); 
  } 
  
  /***************************************************************************
  **
  ** When a pile of modules are deleted (e.g. region deletion), we need to drop 
  ** references to them in our last seen map, as well as clear the current settings
  ** if they are affected.
  ** 
  */
  
  public void cleanUpDeletedModules(Map<String, Set<String>> lostModules, UndoSupport support, DataAccessContext dacx) {
    //
    // See if we need to make a change.
    //   
    Iterator<String> lmksit = lostModules.keySet().iterator();
    while (lmksit.hasNext()) {
      String ovrKey = lmksit.next();
      boolean changed = false;
      TaggedSet mods = lastSeenModulePerOverlay_.get(ovrKey);
      TaggedSet newMods = (mods == null) ? null : mods.clone();    
      if (mods != null) {
        Set<String> lostModForOvr = lostModules.get(ovrKey);
        HashSet<String> intersect = new HashSet<String>(lostModForOvr);
        intersect.retainAll(mods.set);
        // If not empty, we are deleting one or more modules that are referenced
        // in the last seen modules for the overlay.  We will need to delete them,
        // and register the change for undo!
        if (!intersect.isEmpty()) {    
          newMods.set.removeAll(intersect);
          lastSeenModulePerOverlay_.put(ovrKey, newMods);
          changed = true;
        }
      }
      
      TaggedSet revs = lastSeenRevealedPerOverlay_.get(ovrKey);
      TaggedSet newRevs = (revs == null) ? null : revs.clone();    
      if (revs != null) {
        Set<String> lostRevsForOvr = lostModules.get(ovrKey);
        HashSet<String> intersect = new HashSet<String>(lostRevsForOvr);
        intersect.retainAll(revs.set);
        // If not empty, we are deleting one or more modules that are referenced
        // in the last seen reveals for the overlay.  We will need to delete them,
        // and register the change for undo!
        if (!intersect.isEmpty()) {
          newRevs.set.removeAll(intersect);
          lastSeenRevealedPerOverlay_.put(ovrKey, newRevs);
          changed = true;
        }
      }
 
      if (changed) {
        OverlayDisplayChange odc = getDisplayChangeForLastSeen(ovrKey, mods, newMods, revs, newRevs); 
        NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
        support.addEdit(nodcc);
      }
    }
    
    //
    // If the deleted modules are currently displayed, we need to clean up 
    // the current display:
    //
    
    String currOvr = dacx.oso.getCurrentOverlay();
    Set<String> lostModForOvr = lostModules.get(currOvr);
    if (lostModForOvr != null) {
      TaggedSet currMods = dacx.oso.getCurrentNetModules();
      HashSet<String> intersect = new HashSet<String>(lostModForOvr);
      intersect.retainAll(currMods.set);
      TaggedSet currRevs = dacx.oso.getRevealedModules();
      HashSet<String> intersectRev = new HashSet<String>(lostModForOvr);
      intersectRev.retainAll(currRevs.set);
           
      // Showing a deleted module? clear it.  
      if (!intersect.isEmpty()) {
        currMods = currMods.clone();
        currMods.set.removeAll(lostModForOvr);
        currRevs = currRevs.clone();
        currRevs.set.removeAll(lostModForOvr);
        setCurrentModules(currMods, currRevs, support, dacx);
      // Revealing (but not showing - can this happen?) a deleted module? clear it.  
      } else if (!intersectRev.isEmpty()) {
        currRevs = currRevs.clone();
        currRevs.set.removeAll(lostModForOvr);
        setCurrentRevealed(currRevs, support, dacx);
      // Not showing? get menu in sync.  
      } else {
        syncControlState(false, currOvr, true, currMods, currRevs, null, dacx);
      }
    }

    return;
  } 
  
  /***************************************************************************
  **
  ** Record display cache changes that are occurring
  ** 
  */     
  
  
  private OverlayDisplayChange getDisplayChangeForLastSeen(String ovrKey, TaggedSet oldMods, TaggedSet newMods, 
                                                                          TaggedSet oldRevs, TaggedSet newRevs) { 
    OverlayDisplayChange retval = new OverlayDisplayChange();
    retval.useForModuleCache = true;
    retval.lastSeenModuleKey = ovrKey;
    retval.oldlastSeenModuleValue = (oldMods == null) ? null : oldMods.clone();
    retval.oldlastSeenRevealedValue = (oldRevs == null) ? null : oldRevs.clone();
    retval.newlastSeenModuleValue = (newMods == null) ? null : newMods.clone();
    retval.newlastSeenRevealedValue = (newRevs == null) ? null : newRevs.clone();    
    return (retval);
  }
  
 
  /***************************************************************************
  **
  ** Handle interactions with path controller
  */   
  
  private void syncPathController(UndoSupport support, DataAccessContext dacx) {
    //
    // When we change an overlay or module setting, if we are on a path, we need to
    // (possibly) move off the path. 
    //
    
    UserTreePathController utpc = appState_.getPathController();
    NavigationChange nc = new NavigationChange();
    nc.commonView = appState_.getCommonView();
    if (utpc.syncPathControls(nc, dacx)) { 
      support.addEdit(new NavigationChangeCmd(appState_, dacx, nc));
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Set the full state
  */ 
    
  public void setFullOverlayState(String ovrKey, TaggedSet modKeys, TaggedSet revealedKeys, UndoSupport support, DataAccessContext dacx) {    
    OdcAndSccBundle oasb = setFullStateGuts(ovrKey, modKeys, revealedKeys, null, dacx);
    if ((oasb != null) && (oasb.odc != null) && (support != null)) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, oasb.odc);
      support.addEdit(nodcc);
      if ((oasb.bundle != null) && (oasb.bundle.cmd != null)) {
        support.addEdit(oasb.bundle.cmd);
        support.addEvent(oasb.bundle.event);
      }
    }    
    return;
  }   
 
  /***************************************************************************
  **
  ** Set the overlay
  */ 
    
  public void setCurrentOverlay(String ovrKey, UndoSupport support, DataAccessContext dacx) {    
    OdcAndSccBundle oasb = setCurrentOverlayGuts(ovrKey, dacx);
    if (oasb != null) {
      if (oasb.odc != null) {
        syncPathController(support, dacx);
        NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, oasb.odc);
        support.addEdit(nodcc);
        support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE));
        if ((oasb.bundle != null) && (oasb.bundle.cmd != null)) {
          support.addEdit(oasb.bundle.cmd);
          support.addEvent(oasb.bundle.event);
        }
      } 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Clear overlay
  */ 
    
  public void clearCurrentOverlay(UndoSupport support, DataAccessContext dacx) {
    OdcAndSccBundle oasb = setCurrentOverlayGuts(null, dacx);
    if (oasb != null) {
      if (oasb.odc != null) {
        syncPathController(support, dacx);
        NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, oasb.odc);
        support.addEdit(nodcc);
        support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE));
        if ((oasb.bundle != null) && (oasb.bundle.cmd != null)) {
          support.addEdit(oasb.bundle.cmd);
          support.addEvent(oasb.bundle.event);
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a module to the current display
  */ 
    
  public void addToCurrentModules(String modKey, UndoSupport support, DataAccessContext dacx) {
    //
    // Adding a module to the current display has no effect on current visibility set
    //
    TaggedSet currMods = dacx.oso.getCurrentNetModules();
    currMods = currMods.clone();
    currMods.set.add(modKey);
    TaggedSet currRev = dacx.oso.getRevealedModules();
    OverlayDisplayChange odc = setCurrentModuleGuts(currMods, currRev, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
      support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Drop a module from the current display
  */ 
    
  public void dropACurrentModule(String modKey, UndoSupport support, DataAccessContext dacx) {
    TaggedSet currMods = dacx.oso.getCurrentNetModules();
    currMods = currMods.clone();
    currMods.set.remove(modKey);
    TaggedSet currRev = dacx.oso.getRevealedModules();
    currRev = currRev.clone();
    currRev.set.remove(modKey);
    OverlayDisplayChange odc = setCurrentModuleGuts(currMods, currRev, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
      support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
    }    
    return;
  }

  /***************************************************************************
  **
  ** Make a single module current
  */ 
    
  public void setToSingleCurrentModule(String modKey, UndoSupport support, DataAccessContext dacx) {
    //
    // Adding a module to the current display has no effect on current visibility set
    //
    TaggedSet currMods = new TaggedSet();
    currMods.set.add(modKey);
    TaggedSet currRev = dacx.oso.getRevealedModules();
    OverlayDisplayChange odc = setCurrentModuleGuts(currMods, currRev, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
      support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Update module options (e.g.following a module deletion, where the
  ** revealed module key needs to be dropped...)
  */
    
  public void updateModuleOptions(UndoSupport support, DataAccessContext dacx) {
    TaggedSet currMods = dacx.oso.getCurrentNetModules();
    currMods = currMods.clone();
    TaggedSet currRev = dacx.oso.getRevealedModules();
    currRev = currRev.clone();
    OverlayDisplayChange odc = setCurrentModuleGuts(currMods, currRev, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
      support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
    }    
    return;
  } 

  /***************************************************************************
  **
  ** Toggle a module to show contents
  */  
  
  public void toggleModContentDisplay(String modID, UndoSupport support, DataAccessContext dacx) {
    TaggedSet revealed = dacx.oso.getRevealedModules();
    TaggedSet updated = revealed.clone();
    if (revealed.set.contains(modID)) {
      updated.set.remove(modID);
    } else {
      updated.set.add(modID);
    }
    setCurrentRevealed(updated, support, dacx); 
    return;
  }    

  /***************************************************************************
  **
  ** Set just the currently revealed modules
  */ 
    
  public void setCurrentRevealed(TaggedSet revealedKeys, UndoSupport support, DataAccessContext dacx) {
    OverlayDisplayChange odc = setCurrentRevealedGuts(revealedKeys, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
    }    
    return;
  }    
  
  /***************************************************************************
  **
  ** Push the controls to disabled
  */
  
  public void pushDisabled(XPlatMaskingStatus ms, DataAccessContext dacx) {
    if (!ms.areMainOverlayControlsOn().booleanValue()) {
      pushed_ = true;
      upstreamSetEnabled(false);
      pushedAllowLevel_ = ms.isOverlayLevelOn().booleanValue();
      downstreamSetEnabled(false, new Boolean(pushedAllowLevel_), dacx);    
    }
    return;
  }  

  /***************************************************************************
  **
  ** Pop disabled state
  */
 
  public void popDisabled(DataAccessContext dacx) {
    if (pushed_) {
      upstreamSetEnabled(ovrEnabled_);
      downstreamSetEnabled(downStrEnabled_, null, dacx);
      pushed_ = false;
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the net module selector
  ** 
  */
  
  public CheckBoxList getNetModuleChoices() {
    return (netModuleChoices_);
  }
    
  /***************************************************************************
  **
  ** Get the full blown navigation panel
  ** 
  */
  
  public JPanel getNetOverlayNavigator(DataAccessContext dacx) {
    if (ovrPanel_ != null) {
      return (ovrPanel_);
    }
 
    ovrPanel_ = new JPanel();
    ovrPanel_.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = dacx.rMan;
        
    ovrLabel_ = new JLabel(rMan.getString("netOverlayController.chooseOverlay"));
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    ovrPanel_.add(ovrLabel_, gbc);    
    UiUtil.gbcSet(gbc, 1, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    ovrPanel_.add(netOverlaysCombo_, gbc);
    
    modLabel_ = new JLabel(rMan.getString("netOverlayController.chooseModule"));    
    UiUtil.gbcSet(gbc, 0, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 0, 5, UiUtil.W, 1.0, 0.0);
    ovrPanel_.add(modLabel_, gbc);      
    
    UiUtil.gbcSet(gbc, 0, 2, 4, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);   
    JScrollPane jsp = new JScrollPane(netModuleChoices_);
    ovrPanel_.add(jsp, gbc);

    Box buttonPanelAll = Box.createHorizontalBox();
    buttonPanelAll.add(zoomButtonA_);
    buttonPanelAll.add(Box.createHorizontalGlue()); 
    buttonPanelAll.add(allButtonA_);
    buttonPanelAll.add(Box.createHorizontalStrut(3));    
    buttonPanelAll.add(revAllButton_);
    buttonPanelAll.add(Box.createHorizontalStrut(3));    
    buttonPanelAll.add(hideAllButton_);    
    buttonPanelAll.add(Box.createHorizontalStrut(3));        
    buttonPanelAll.add(noneButtonA_);
    
    Box buttonPanelSome = Box.createHorizontalBox();
    buttonPanelSome.add(zoomButtonS_);
    buttonPanelSome.add(Box.createHorizontalGlue()); 
    buttonPanelSome.add(allButtonS_);
    buttonPanelSome.add(Box.createHorizontalStrut(3));        
    buttonPanelSome.add(noneButtonS_);
  
    buttonPanel_ = new JPanel();
    myCard_ = new CardLayout();
    buttonPanel_.setLayout(myCard_);
    buttonPanel_.add(buttonPanelAll, "All");
    buttonPanel_.add(buttonPanelSome, "Some");
    
    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 6, 4, 1, UiUtil.HOR, 0, 0, 0, 5, 0, 5, UiUtil.SE, 1.0, 0.0);
    ovrPanel_.add(buttonPanel_, gbc);
 
    levelLabel_ = new JLabel(rMan.getString("netOverlayController.level"));
    UiUtil.gbcSet(gbc, 0, 7, 1, 1, UiUtil.NONE, 0, 0, 10, 5, 5, 5, UiUtil.N, 0.0, 0.0);
    ovrPanel_.add(levelLabel_, gbc);        
    UiUtil.gbcSet(gbc, 1, 7, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    ovrPanel_.add(nocp_, gbc);

    return (ovrPanel_);
  
  }
  
  /***************************************************************************
  **
  ** Undo a module selection change
  */
  
  public void changeUndo(OverlayDisplayChange undo, DataAccessContext dacx) {
    
    if (undo.useForModuleCache) {
      if (undo.oldlastSeenModuleValue == null) {
        lastSeenModulePerOverlay_.remove(undo.lastSeenModuleKey);
      } else {
        TaggedSet oldSet = undo.oldlastSeenModuleValue.clone();
        lastSeenModulePerOverlay_.put(undo.lastSeenModuleKey, oldSet);
      }
      if (undo.oldlastSeenRevealedValue == null) {
        lastSeenRevealedPerOverlay_.remove(undo.lastSeenModuleKey);
      } else {
        TaggedSet oldRev = undo.oldlastSeenRevealedValue.clone();
        lastSeenRevealedPerOverlay_.put(undo.lastSeenModuleKey, oldRev);
      }
      return;
    }
    
    syncControlState(true, undo.oldOvrKey, true, undo.oldModKeys, undo.oldRevealedKeys, undo.oldGenomeID, dacx);
    pushCore(true, undo.oldOvrKey, true, undo.oldModKeys, undo.oldRevealedKeys, true, dacx);    
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a module selection change
  */
  
  public void changeRedo(OverlayDisplayChange redo, DataAccessContext dacx) {
    
    if (redo.useForModuleCache) {
      if (redo.newlastSeenModuleValue == null) {
        lastSeenModulePerOverlay_.remove(redo.lastSeenModuleKey);
      } else {
        TaggedSet newSet = redo.newlastSeenModuleValue.clone();
        lastSeenModulePerOverlay_.put(redo.lastSeenModuleKey, newSet);
      }
      if (redo.newlastSeenRevealedValue == null) {
        lastSeenRevealedPerOverlay_.remove(redo.lastSeenModuleKey);
      } else {
        TaggedSet newRev = redo.newlastSeenRevealedValue.clone();
        lastSeenRevealedPerOverlay_.put(redo.lastSeenModuleKey, newRev);
      }
      return;
    }
       
    syncControlState(true, redo.newOvrKey, true, redo.newModKeys, redo.newRevealedKeys, redo.newGenomeID, dacx);
    pushCore(true, redo.newOvrKey, true, redo.newModKeys, redo.newRevealedKeys, true, dacx);    
    return; 
  }
  
  /***************************************************************************
  **
  ** Set the current display state as the first viewed for the model.
  */ 
    
  public void setCurrentAsFirst(DataAccessContext dacx) {
    UndoSupport support = new UndoSupport(appState_, "undo.setCurrOverlayFirst");
    String genomeID = dacx.getGenomeID();
    NetOverlayOwner owner = dacx.getCurrentOverlayOwner();
    String ownID = owner.getID();
    int ownMode = owner.overlayModeForOwner();
    String ovrKey = dacx.oso.getCurrentOverlay();
    TaggedSet currMods = new TaggedSet(dacx.oso.getCurrentNetModules());
    TaggedSet currRevs = new TaggedSet(dacx.oso.getRevealedModules());
    currRevs = normalizedRevealedModules(dacx, genomeID, ovrKey, currMods, currRevs);    
    Iterator<NetworkOverlay> noit = owner.getNetworkOverlayIterator();
    boolean didIt = false;
    while (noit.hasNext()) {
      NetworkOverlay no = noit.next();
      String noID = no.getID();
      TaggedSet fvs = new TaggedSet();
      TaggedSet fvr = new TaggedSet();
      boolean isFirst = no.getFirstViewState(fvs, fvr);
      NetworkOverlayChange noc = null; 
      boolean setIt = false;
      boolean clearIt = false;
      if (isFirst) { // may need clearing or changing
        clearIt = (ovrKey == null) || !ovrKey.equals(noID);
        setIt = (ovrKey != null) && ovrKey.equals(noID);         
      } else { // current is null; doesn't need clearing
        clearIt = false;
        setIt = (ovrKey != null) && (ovrKey.equals(noID));
      }        
      if (setIt) {
        noc = no.setAsFirstView(currMods, currRevs, ownID, ownMode);
      } else if (clearIt) {
        noc = no.clearAsFirstView(ownID, ownMode);  
      }  
      if (noc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, dacx, noc);
        support.addEdit(gcc);
        didIt = true;
      }
    }
    if (didIt) {
      support.finish();
    }
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Create a normalized revealed module set (i.e. subset of currMods that only
  ** is stocked if the overlay mode is opaque:
  */ 
    
  public static TaggedSet normalizedRevealedModules(DataAccessContext dacx, String genomeID, String ovrKey, 
                                                    TaggedSet currMods, TaggedSet revealed) {

    Layout lo = dacx.lSrc.getLayoutForGenomeKey(genomeID);
    if (ovrKey == null) {
      return (new TaggedSet());
    }
    NetOverlayProperties nop = lo.getNetOverlayProperties(ovrKey);
    if (nop.getType() != NetOverlayProperties.OvrType.OPAQUE) {
      return (new TaggedSet());
    }
    TaggedSet retval = revealed.clone(); 
    retval.set.retainAll(currMods.set);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Answer if the given view state will obscure links or possibly nodes:
  */ 
     
  public static int hasObscuredElements(DataAccessContext dacx, StartupView sv) {
    int retval = NO_HIDING;
    Layout lo = dacx.lSrc.getLayoutForGenomeKey(sv.getModel());
    String ovrKey = sv.getOverlay();
    if (ovrKey == null) {
      return (retval);
    }
    NetOverlayProperties nop = lo.getNetOverlayProperties(ovrKey);
    if (nop.getType() == NetOverlayProperties.OvrType.OPAQUE) {
      retval |= OPAQUE_OVERLAY;
    }  
    if (nop.hideLinks()) {
      retval |= LINKS_NOT_SHOWN;
    }
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VIZ METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  /***************************************************************************
  **
  ** Set the full state
  ** 
  */
  
  OdcAndSccBundle setFullStateGuts(String ovrKey, TaggedSet modKeys, TaggedSet revealedKeys, OverlayDisplayChange odc, DataAccessContext dacx) {

    if (odc == null) {
      odc = new OverlayDisplayChange();
      odc.oldGenomeID = dacx.getGenomeID();
      odc.oldOvrKey = dacx.oso.getCurrentOverlay();
      odc.oldModKeys = new TaggedSet(dacx.oso.getCurrentNetModules());
      odc.oldRevealedKeys = new TaggedSet(dacx.oso.getRevealedModules());
    }
    odc.newGenomeID = dacx.getGenomeID();
    odc.newOvrKey = ovrKey;
    odc.newModKeys = new TaggedSet(modKeys);
    odc.newRevealedKeys = new TaggedSet(revealedKeys);

    OdcAndSccBundle retval = new OdcAndSccBundle();
    retval.odc = odc;
    syncControlState(true, ovrKey, true, modKeys, revealedKeys, null, dacx);
    retval.bundle = pushCore(true, ovrKey, true, modKeys, revealedKeys, false, dacx);
    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Set the current overlay from my menu/controls
  ** 
  */
  
  OdcAndSccBundle setCurrentOverlayGuts(String ovrKey, DataAccessContext dacx) {
    
    OdcAndSccBundle retval = new OdcAndSccBundle();
    retval.odc = new OverlayDisplayChange();
    
    String genomeID = dacx.getGenomeID();
    String currOvrKey = dacx.oso.getCurrentOverlay();
    if (currOvrKey == null) {
      if (ovrKey == null) {
        return (null);
      }
    } else {
      if (currOvrKey.equals(ovrKey)) {
        return (null);
      }
    }

    TaggedSet modsForOvr = null;
    TaggedSet revsForOvr = null;
    if ((genomeID != null) && (ovrKey != null)) {
      modsForOvr = lastSeenModulePerOverlay_.get(ovrKey);
      revsForOvr = lastSeenRevealedPerOverlay_.get(ovrKey);
    }
    if (modsForOvr == null) {
      modsForOvr = getAllModuleChoiceSet(getModuleChoices(genomeID, ovrKey, dacx));     
    }
    if (revsForOvr == null) {
      revsForOvr = new TaggedSet();
    }    
        
    TaggedSet currModKeys = dacx.oso.getCurrentNetModules();
    TaggedSet currRevKeys = dacx.oso.getRevealedModules();
    retval.odc.oldGenomeID = genomeID;
    retval.odc.newGenomeID = genomeID;
    retval.odc.oldOvrKey = currOvrKey;
    retval.odc.newOvrKey = ovrKey;
    retval.odc.oldModKeys = new TaggedSet(currModKeys);
    retval.odc.newModKeys = new TaggedSet(modsForOvr);
    retval.odc.oldRevealedKeys = new TaggedSet(currRevKeys);
    retval.odc.newRevealedKeys = new TaggedSet(revsForOvr);    

    syncControlState(true, ovrKey, true, modsForOvr, revsForOvr, null, dacx);
    retval.bundle = pushCore(true, ovrKey, true, modsForOvr, revsForOvr, false, dacx);
    return (retval);
  }   
   
  /***************************************************************************
  **
  ** Set the current modules and revealed set
  ** 
  */
  
  OverlayDisplayChange setCurrentModuleGuts(TaggedSet modKeys, TaggedSet revKeys, DataAccessContext dacx) {
    OverlayDisplayChange retval = new OverlayDisplayChange();
    
    TaggedSet currModKeys = dacx.oso.getCurrentNetModules();
    TaggedSet currRevKeys = dacx.oso.getRevealedModules();

    String currOvrKey = dacx.oso.getCurrentOverlay();
    retval.oldGenomeID = dacx.getGenomeID();
    retval.newGenomeID = retval.oldGenomeID;
    retval.oldOvrKey = currOvrKey;
    retval.newOvrKey = currOvrKey;
    retval.oldModKeys = new TaggedSet(currModKeys);
    retval.newModKeys = new TaggedSet(modKeys);
    retval.oldRevealedKeys = new TaggedSet(currRevKeys);
    retval.newRevealedKeys = new TaggedSet(revKeys);

    TaggedSet revealedKeys = new TaggedSet(revKeys);
    syncControlState(false, currOvrKey, true, modKeys, revealedKeys, null, dacx);
    pushCore(false, currOvrKey, true, modKeys, revealedKeys, false, dacx);
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Set the currently revealed modules
  ** 
  */
  
  OverlayDisplayChange setCurrentRevealedGuts(TaggedSet revealed, DataAccessContext dacx) {
    OverlayDisplayChange retval = new OverlayDisplayChange();
    
    TaggedSet currModKeys = dacx.oso.getCurrentNetModules();
    TaggedSet currRevKeys = dacx.oso.getRevealedModules();
    
    if (currRevKeys.set.equals(revealed.set) && (currRevKeys.tag == revealed.tag)) {
      return (null);
    }
    
    String currOvrKey = dacx.oso.getCurrentOverlay();
    retval.oldGenomeID = dacx.getGenomeID();
    retval.newGenomeID = retval.oldGenomeID;
    retval.oldOvrKey = currOvrKey;
    retval.newOvrKey = currOvrKey;
    retval.oldModKeys = new TaggedSet(currModKeys);
    retval.newModKeys = new TaggedSet(currModKeys);
    retval.oldRevealedKeys = new TaggedSet(currRevKeys);
    retval.newRevealedKeys = new TaggedSet(revealed);

    syncControlState(false, currOvrKey, true, currModKeys, revealed, null, dacx);
    pushCore(false, currOvrKey, true, currModKeys, revealed, false, dacx);
    return (retval);
  }
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Set just the modules (with optional normalization)
  */
  
  private void setCurrentModules(TaggedSet modKeys, TaggedSet revKeys, UndoSupport support, DataAccessContext dacx) {    
    OverlayDisplayChange odc = setCurrentModuleGuts(modKeys, revKeys, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
    }    
    return;
  }      
  
  /***************************************************************************
  **
  ** Handle the pieces that control overlay setting
  */ 
  
   private void upstreamSetEnabled(boolean enabled) {
    
    if (netOverlaysCombo_ != null) {
      netOverlaysCombo_.setEnabled(enabled);
    }
    if (ovrLabel_ != null) {
      ovrLabel_.setEnabled(enabled);
    }
    return;
  }    

  /***************************************************************************
  **
  ** Handle controls that depend on overlay setting
  */ 
  
  private void downstreamSetEnabled(boolean enabled, Boolean allowLevelSetting, DataAccessContext dacx) {
   
    activeRevControls_ = alphaCalc_.modContentsMasked(dacx.oso.getCurrentOverlaySettings());
    
    if (netModuleChoices_ != null) {
      netModuleChoices_.setEnabled(enabled);
      netModuleChoices_.showAndEnableSubSelection(showingRevControls_, (activeRevControls_) ? enabled : false);
    }
    
    if (modLabel_ != null) {
      modLabel_.setEnabled(enabled);
    } 
    
    if (allButtonA_ != null) {
      allButtonA_.setEnabled(enabled);
      allButtonS_.setEnabled(enabled);
    } 
    
    if (noneButtonA_ != null) {
      noneButtonA_.setEnabled(enabled);
      noneButtonS_.setEnabled(enabled);
    }     
    
    if (hideAllButton_ != null) {
      hideAllButton_.setEnabled(activeRevControls_ ? enabled : false);
    } 
    
    if (revAllButton_ != null) {
      revAllButton_.setEnabled(activeRevControls_ ? enabled : false);
    }     
 
    if (zoomButtonA_ != null) {
      zoomButtonA_.setEnabled(zoomCanShow_ ? enabled : false);
      zoomButtonS_.setEnabled(zoomCanShow_ ? enabled : false);
    }     
    
    boolean levelEnabled = enabled;
    if (!enabled && (allowLevelSetting != null) && allowLevelSetting.booleanValue()) {
      levelEnabled = true;
    }   
    
    if (nocp_ != null) {
      nocp_.setEnabled(levelEnabled);
    }
    if (levelLabel_ != null) {
      levelLabel_.setEnabled(levelEnabled);
    } 

    return;
  }   
  
  /***************************************************************************
  **
  ** Handle syncing tasks
  ** 
  */
  
  private SelectionChangeCmd.Bundle pushDownState(String ovrKey, TaggedSet modKeys, TaggedSet revealedKeys, DataAccessContext dacx) {    
    syncControlState(true, ovrKey, true, modKeys, revealedKeys, null, dacx);
    SelectionChangeCmd.Bundle bundle = pushCore(true, ovrKey, true, modKeys, revealedKeys, false, dacx);
    return (bundle);
  }    
  
  /***************************************************************************
  **
  ** Pass stuff off to the SUPanel
  ** 
  */
  
  private SelectionChangeCmd.Bundle pushCore(boolean doOvr, String ovrKey, boolean doModAndRev, 
                                             TaggedSet modKeys, TaggedSet revealed, boolean forUndo, DataAccessContext dacx) {
    SelectionChangeCmd.Bundle bundle = null;
    if (doOvr) {      
      bundle = appState_.setCurrentOverlay(ovrKey, forUndo);
    }
    if (doModAndRev) {
      appState_.setCurrentNetModules(modKeys, forUndo);
      appState_.setRevealedModules(revealed, forUndo);
    }

    appState_.getSUPanel().drawModel(false);
    return (bundle);
  }  
   
  /***************************************************************************
  **
  ** Handle syncing tasks
  ** 
  */
  
  private void syncControlState(boolean doOvr, String ovrKey, 
                                boolean doModAndRev, TaggedSet modKeys, TaggedSet revealedKeys,
                                String undoGenomeID, DataAccessContext dacx) {
    String genomeID = (undoGenomeID == null) ? dacx.getGenomeID() : undoGenomeID;
    if (doOvr) updateNetOverlayOptions(genomeID, dacx);
    if (doModAndRev) updateNetModuleOptions(genomeID, ovrKey, modKeys, dacx);
    
    if (doOvr) selectCurrentOverlayInUI(ovrKey);
    if (doModAndRev) selectCurrentNetModulesInUI(modKeys, revealedKeys);
      
    NetOverlayOwner owner = (genomeID == null) ? null: dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeID);
    
    if (owner != null) {
      if (ovrKey != null) {
        lastSeenOverlayPerOwner_.put(owner.getID(), new TaggedString(INITIALIZED_, ovrKey));
        lastSeenModulePerOverlay_.put(ovrKey, modKeys);  
        lastSeenRevealedPerOverlay_.put(ovrKey, revealedKeys);  
      } else {
        // Note we keep the module keys, so that the next time we return to this
        // overlay, we are looking at those modules again:
        lastSeenOverlayPerOwner_.put(owner.getID(), new TaggedString(INITIALIZED_, null));
      }
    }
    
    int numOvr = -1;
    if (netOverlaysCombo_ != null) {
      numOvr = netOverlaysCombo_.getItemCount();
    }
   
    ovrEnabled_ = (numOvr > 1);
    upstreamSetEnabled(ovrEnabled_ && !pushed_);
    downStrEnabled_ = (ovrKey != null);
    zoomCanShow_ = !modKeys.set.isEmpty();
    Boolean pushedLevel = (pushed_) ? new Boolean(pushedAllowLevel_) : null;
    downstreamSetEnabled(downStrEnabled_ && !pushed_, pushedLevel, dacx);

    updateOverlayActionStatus(ovrKey, modKeys.set);
    return;
  }     
  
  /***************************************************************************
  **
  ** Sync UI controls for overlay
  */ 
    
  private void selectCurrentOverlayInUI(String key) {
    managingOverlayControls_ = true;
   
    if (netOverlaysCombo_ != null) {
      int numUpm = netOverlaysCombo_.getItemCount();
      for (int i = 0; i < numUpm; i++) {
        ObjChoiceContent occ = (ObjChoiceContent)netOverlaysCombo_.getItemAt(i);
        String ovKey = occ.val;
        boolean selected = false;
        if (key == null) {
          selected = (ovKey == null);
        } else {
          selected = (key.equals(ovKey));
        }
        if (selected) {
          netOverlaysCombo_.setSelectedIndex(i);
          netOverlaysCombo_.invalidate();
          netOverlaysCombo_.validate();
          break;
        }
      }
    }
       
    managingOverlayControls_ = false;
    return;    
  } 
  
  /***************************************************************************
  **
  ** Sync UI controls for modules
  */ 
    
  private void selectCurrentNetModulesInUI(TaggedSet currKeys, TaggedSet revKeys) {
    managingOverlayControls_ = true;
  
    if (netModuleChoices_ != null) {
      ListModel myModel = netModuleChoices_.getModel();
      int numElem = myModel.getSize();
      for (int i = 0; i < numElem; i++) {
        CheckBoxList.ListChoice choice = (CheckBoxList.ListChoice)myModel.getElementAt(i);
        choice.isSelected = (currKeys.set.contains(choice.getObject()));
        choice.isSubSelected = (revKeys.set.contains(choice.getObject()));
      }
      netModuleChoices_.repaint();
    }    

    managingOverlayControls_ = false;
    return;    
  }   
  
  /***************************************************************************
  **
  ** Return possible overlay choices
  */
  
  private Vector<ObjChoiceContent> getOverlayChoices(String genomeID, DataAccessContext dacx) {
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    if (genomeID == null) {
      return (retval);
    }
    NetOverlayOwner owner = dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeID);
    // 11/06/13 Now seeing a null return from above. Happening when a model is being loaded 
    // and "bioTapA" returns null while debugging; the problem seems to have come from out of
    // the blue. Does this key into the 4-year old problem below?
    if (owner == null) {
      return (retval);
    }
    // FIX ME?? Saw core dump here when loading file (SPEndoOverlay2 from EmptyArea3)
    // As of 6/2/09, could not seem to reproduce...
    Iterator<NetworkOverlay> noit = owner.getNetworkOverlayIterator();    
   
    while (noit.hasNext()) {
      NetworkOverlay ovr = noit.next();
      retval.add(new ObjChoiceContent(ovr.getName(), ovr.getID()));
    }    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Return possible module choices
  */
  
  private Vector<CheckBoxList.ListChoice> getModuleChoices(String genomeID, String ovrKey, DataAccessContext dacx) {
    Vector<CheckBoxList.ListChoice> retval = new Vector<CheckBoxList.ListChoice>();
    if ((genomeID == null) || (ovrKey == null)) {
      return (retval);
    }
    NetOverlayOwner owner = dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeID);
    NetworkOverlay ovr = owner.getNetworkOverlay(ovrKey);
    Iterator<NetModule> nmit = ovr.getModuleIterator();    
   
    while (nmit.hasNext()) {
      NetModule mod = nmit.next();
      CheckBoxList.ListChoice lc = new CheckBoxList.ListChoice(mod.getID(), mod.getName(), Color.white, false);
      retval.add(lc);
    }    
    return (retval);
  }   
  
  /***************************************************************************
  **
  ** Return the entry for all module choices
  */
  
  private TaggedSet getAllModuleChoiceSet(Vector<CheckBoxList.ListChoice> modChoices) {
    TaggedSet retval = new TaggedSet();
    int size = modChoices.size();    
    for (int i = 0; i < size; i++) {
      CheckBoxList.ListChoice lc = modChoices.get(i);
      retval.set.add(lc.getObjectAsString());
    }
    return (retval);
  }   
 
  /***************************************************************************
  **
  ** Update the controls for network overlays
  */ 
    
  private void updateNetOverlayOptions(String genomeID, DataAccessContext dacx) {
    managingOverlayControls_ = true;
    Vector<ObjChoiceContent> overlayChoices = getOverlayChoices(genomeID, dacx);
    int numOV = overlayChoices.size();
    String noOverlay =  dacx.rMan.getString("netOverlayController.noOverlay");     
        
    if (netOverlaysCombo_ != null) {
      netOverlaysCombo_.removeAllItems();
      netOverlaysCombo_.addItem(new ObjChoiceContent(noOverlay, null));   
      for (int i = 0; i < numOV; i++) {
        ObjChoiceContent occ = overlayChoices.get(i); 
        netOverlaysCombo_.addItem(occ);    
      }      
      netOverlaysCombo_.invalidate();
      netOverlaysCombo_.validate(); 
    }

    managingOverlayControls_ = false;
    return;
  }  
  
  /***************************************************************************
  **
  ** Update the controls for network modules
  */ 
    
  private void updateNetModuleOptions(String genomeID, String ovrKey, TaggedSet currModules, DataAccessContext dacx) {
    managingOverlayControls_ = true;
    
    showingRevControls_ = false;
    if ((genomeID != null) && (ovrKey != null)) {
      Layout lo = dacx.lSrc.getLayoutForGenomeKey(genomeID);
      NetOverlayProperties nop = lo.getNetOverlayProperties(ovrKey);
      showingRevControls_ = (nop.getType() == NetOverlayProperties.OvrType.OPAQUE);
    }
    Vector<CheckBoxList.ListChoice> moduleChoices = getModuleChoices(genomeID, ovrKey, dacx);
    if (netModuleChoices_ != null) {
      netModuleChoices_.showAndEnableSubSelection(showingRevControls_, activeRevControls_);
      netModuleChoices_.setListData(moduleChoices);
      netModuleChoices_.invalidate();
      netModuleChoices_.validate(); 
    } 
    
    if (buttonPanel_ != null) {
      myCard_.show(buttonPanel_, (showingRevControls_) ? "All" : "Some");
      buttonPanel_.invalidate();
      buttonPanel_.validate();
    }

    managingOverlayControls_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Wrap undo around operations
  */ 
    
  private void unDoableSetOverlay(String ovKey, DataAccessContext dacx) {         
    UndoSupport support = new UndoSupport(appState_, "undo.setCurrentNetOverlay");
    OdcAndSccBundle oasb = setCurrentOverlayGuts(ovKey, dacx);
    if (oasb != null) {
      if (oasb.odc != null) {
        syncPathController(support, dacx);
        NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, oasb.odc);
        support.addEdit(nodcc);
        support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
        if ((oasb.bundle != null) && (oasb.bundle.cmd != null)) {
          support.addEdit(oasb.bundle.cmd);
          support.addEvent(oasb.bundle.event);
        }
        support.finish();
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Wrap undo around operations
  */ 
    
  private void unDoableSetModules(TaggedSet modKeys, TaggedSet revKeys, DataAccessContext dacx) {    
    UndoSupport support = new UndoSupport(appState_, "undo.setCurrentNetModule");
    // This is called during checkbox click or "all" or "clear" button clicks.
    // Should the argument be false (no normalization)?  I believe so; we
    // want the revealed state to carry across these interactions:
    OverlayDisplayChange odc = setCurrentModuleGuts(modKeys, revKeys, dacx);
    if (odc != null) {
      syncPathController(support, dacx);
      NetOverlayDisplayChangeCmd nodcc = new NetOverlayDisplayChangeCmd(appState_, dacx, odc);
      support.addEdit(nodcc);
      support.addEvent(new OverlayDisplayChangeEvent(OverlayDisplayChangeEvent.MODULE_SELECTION_CHANGE)); 
      support.finish();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Init the controller
  */ 
    
  private void init(DataAccessContext dacx) {  

    ResourceManager rMan = dacx.rMan;
    nocp_ = new NetOverlayControlPanel(appState_, alphaCalc_);
    netOverlaysCombo_ = new FixedJComboBox(250);
    
    //
    // Controls for network overlays and modules
    //

    netOverlaysCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (managingOverlayControls_) {
            return;
          }     
          ObjChoiceContent occ = (ObjChoiceContent)netOverlaysCombo_.getSelectedItem();
          String ovKey = (occ == null) ? null : occ.val;
          unDoableSetOverlay(ovKey, dacxDyn_);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    
    netModuleChoices_ = new CheckBoxList(new CheckBoxList.CheckBoxListListener() {
      public void checkIsClicked() {
        try {
          if (managingOverlayControls_) {  // Actually only called on mouse click...should not matter...
            return;
          }
          boolean somebodyChecked = false;
          ListModel myModel = netModuleChoices_.getModel();
          TaggedSet modKeys = new TaggedSet();
          TaggedSet revKeys = new TaggedSet();
          int numElem = myModel.getSize();
          for (int i = 0; i < numElem; i++) {
            CheckBoxList.ListChoice choice = (CheckBoxList.ListChoice)myModel.getElementAt(i);
            if (choice.isSelected) {
              modKeys.set.add(choice.getObjectAsString());
              somebodyChecked = true;
            }
            if (choice.isSubSelected) {
              revKeys.set.add(choice.getObjectAsString()); 
            }          
          }
          zoomCanShow_ = somebodyChecked;
          unDoableSetModules(modKeys, revKeys, dacxDyn_);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    }, true, true, appState_);
    netModuleChoices_.setToolTipText(rMan.getString("netOverlayController.mainTip"), 
                                     rMan.getString("netOverlayController.subTip"));
    
    allButtonA_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.allModules"), 12);
    allButtonA_.addActionListener(new AllActor());
    allButtonA_.setToolTipText(rMan.getString("netOverlayController.allModulesToolTip"));
    allButtonS_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.allModules"), 12);
    allButtonS_.addActionListener(new AllActor());
    allButtonS_.setToolTipText(rMan.getString("netOverlayController.allModulesToolTip"));    
    
    revAllButton_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.revAllModules"), 12);
    revAllButton_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TaggedSet useChoices = dacxDyn_.oso.getCurrentNetModules();
          TaggedSet currRevs = dacxDyn_.oso.getRevealedModules();
          TaggedSet useRevs = useChoices.clone();
          useRevs.set.addAll(currRevs.set);
          unDoableSetModules(useChoices, useRevs, dacxDyn_);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    revAllButton_.setToolTipText(rMan.getString("netOverlayController.revAllModulesToolTip"));
    
    hideAllButton_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.hideAllModules"), 12);
    hideAllButton_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TaggedSet useChoices = dacxDyn_.oso.getCurrentNetModules();
          TaggedSet currRevs = dacxDyn_.oso.getRevealedModules();
          TaggedSet useRevs = currRevs.clone();
          useRevs.set.removeAll(useChoices.set);
          unDoableSetModules(useChoices, useRevs, dacxDyn_);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    hideAllButton_.setToolTipText(rMan.getString("netOverlayController.hideAllModulesToolTip"));    
    
    noneButtonA_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.noModules"), 12);
    noneButtonA_.addActionListener(new NoneActor());
    noneButtonA_.setToolTipText(rMan.getString("netOverlayController.noModulesToolTip"));
    noneButtonS_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.noModules"), 12);
    noneButtonS_.addActionListener(new NoneActor());
    noneButtonS_.setToolTipText(rMan.getString("netOverlayController.noModulesToolTip"));    
    
    zoomButtonA_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.zoom"), 12);
    zoomButtonA_.addActionListener(new ZoomActor());
    zoomButtonA_.setToolTipText(rMan.getString("netOverlayController.zoomToolTip"));
    zoomButtonS_ = FixedJButton.miniFactory(rMan.getString("netOverlayController.zoom"), 12);
    zoomButtonS_.addActionListener(new ZoomActor());
    zoomButtonS_.setToolTipText(rMan.getString("netOverlayController.zoomToolTip"));   
  }  
  
   
  /***************************************************************************
  **
  ** Do overlay enabling and disabling.
  */ 

  private void updateOverlayActionStatus(String ovrKey, Set<String> modKeys) {  
    //
    // Enable/disable overlay actions:
    //
    boolean headless = appState_.isHeadless();
    MainCommands mcmd = appState_.getMainCmds();
    
    //
    // Enable/disable path actions based on path limits:
    //
    
    if (!headless) {
      // Used to be this lived in MainCommmands, and the cache was checked directly. Now need to have a call that does not
      // return anything if it does not already exist!
   
      MainCommands.ChecksForEnabled rnoWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.REMOVE_CURR_NETWORK_OVERLAY, true);
      MainCommands.ChecksForEnabled rnoNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.REMOVE_CURR_NETWORK_OVERLAY, false);
    
      MainCommands.ChecksForEnabled dnWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.DRAW_NETWORK_MODULE, true);
      MainCommands.ChecksForEnabled dnNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.DRAW_NETWORK_MODULE, false);
      
      MainCommands.ChecksForEnabled enoWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.EDIT_CURR_NETWORK_OVERLAY, true);
      MainCommands.ChecksForEnabled enoNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.EDIT_CURR_NETWORK_OVERLAY, false);
     
      MainCommands.ChecksForEnabled ztcWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_TO_CURRENT_NETWORK_MODULE, true);
      MainCommands.ChecksForEnabled ztcNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_TO_CURRENT_NETWORK_MODULE, false);
      
      MainCommands.ChecksForEnabled dnmlWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK, true);
      MainCommands.ChecksForEnabled dnmlNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK, false);    
  
      MainCommands.ChecksForEnabled tmcdWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY, true);
      MainCommands.ChecksForEnabled tmcdNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY, false);   
      
      boolean ovrSelected = (ovrKey != null);
      if (rnoWI != null) rnoWI.setConditionalEnabled(ovrSelected);
      if (rnoNI != null) rnoNI.setConditionalEnabled(ovrSelected);
      
      if (enoWI != null) enoWI.setConditionalEnabled(ovrSelected);
      if (enoNI != null) enoNI.setConditionalEnabled(ovrSelected);    
      
      if (dnWI != null) dnWI.setConditionalEnabled(ovrSelected);
      if (dnNI != null) dnNI.setConditionalEnabled(ovrSelected);
        
      boolean modsSelected = (modKeys.size() >= 1);
      if (dnmlWI != null) dnmlWI.setConditionalEnabled(modsSelected);
      if (dnmlNI != null) dnmlNI.setConditionalEnabled(modsSelected);     
        
      if (ztcWI != null) ztcWI.setConditionalEnabled(modsSelected);
      if (ztcNI != null) ztcNI.setConditionalEnabled(modsSelected); 
      
      if (tmcdWI != null) tmcdWI.setConditionalEnabled(modsSelected);
      if (tmcdNI != null) tmcdNI.setConditionalEnabled(modsSelected);  
    } else {
      buttonStat_.clear();
      boolean ovrSelected = (ovrKey != null);
      boolean modsSelected = (modKeys.size() >= 1);
      //
      // Nice to do checking, but it requires that the flow classes be available, which is undesirable
      // in the web viewer. Make the checks editor only:
      //
      boolean doChecks = appState_.getIsEditor();
      
      if (doChecks) { 
        ControlFlow rnoFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.REMOVE_CURR_NETWORK_OVERLAY);
        if (!rnoFlow.externallyEnabled()) {
          throw new IllegalStateException();
        }
      }
      buttonStat_.put(FlowMeister.MainFlow.REMOVE_CURR_NETWORK_OVERLAY, new Boolean(ovrSelected));

      if (doChecks) { 
        ControlFlow dnFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.DRAW_NETWORK_MODULE);
        if (!dnFlow.externallyEnabled()) {
          throw new IllegalStateException();
        }
      }
      buttonStat_.put(FlowMeister.MainFlow.DRAW_NETWORK_MODULE, new Boolean(ovrSelected));       
       
      if (doChecks) { 
        ControlFlow enoFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.EDIT_CURR_NETWORK_OVERLAY);
        if (!enoFlow.externallyEnabled()) {
          throw new IllegalStateException();
        }
      }
      buttonStat_.put(FlowMeister.MainFlow.EDIT_CURR_NETWORK_OVERLAY, new Boolean(ovrSelected));
       
      ControlFlow ztcFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.ZOOM_TO_CURRENT_NETWORK_MODULE);
      if (!ztcFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      buttonStat_.put(FlowMeister.MainFlow.ZOOM_TO_CURRENT_NETWORK_MODULE, new Boolean(modsSelected));
       
      if (doChecks) { 
        ControlFlow dnmlFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK);
        if (!dnmlFlow.externallyEnabled()) {
          throw new IllegalStateException();
        }
      }
      buttonStat_.put(FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK, new Boolean(modsSelected));   
        
      ControlFlow tmcdFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY);
      if (!tmcdFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      buttonStat_.put(FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY, new Boolean(modsSelected));   
    }  
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES 
  //
  ////////////////////////////////////////////////////////////////////////////

  private static class OdcAndSccBundle {
    OverlayDisplayChange odc;
    SelectionChangeCmd.Bundle bundle;
  }
   
  private class ZoomActor implements ActionListener {    
    public void actionPerformed(ActionEvent e) {
      try {  
        DesktopControlFlowHarness dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame())); 
        ControlFlow cf = new Zoom(appState_, Zoom.ZoomAction.ZOOM_CURRENT_MOD);
        dcf.initFlow(cf, dacxDyn_);
        dcf.runFlow();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }
  }  
  
  private class AllActor implements ActionListener {
    public void actionPerformed(ActionEvent ev) {
      try {
        String currOvrKey = dacxDyn_.oso.getCurrentOverlay();
        TaggedSet useChoices = getAllModuleChoiceSet(getModuleChoices(dacxDyn_.getGenomeID(), currOvrKey, dacxDyn_));
        TaggedSet useRevs = dacxDyn_.oso.getRevealedModules();
        unDoableSetModules(useChoices, useRevs, dacxDyn_);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
  }
  
  
  private class NoneActor implements ActionListener {
    public void actionPerformed(ActionEvent ev) {
      try {
        TaggedSet useChoices = new TaggedSet();
        TaggedSet useRevs = new TaggedSet();
        unDoableSetModules(useChoices, useRevs, dacxDyn_);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
  }  
}
