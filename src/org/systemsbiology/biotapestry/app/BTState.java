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


package org.systemsbiology.biotapestry.app;

import java.awt.font.FontRenderContext;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.GroupPanelCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.WebServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateDown;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.SimParamSource;
import org.systemsbiology.biotapestry.db.TabNameData;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.gaggle.GooseManager;
import org.systemsbiology.biotapestry.nav.DataPopupManager;
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.OverlayDisplayChange;
import org.systemsbiology.biotapestry.nav.RecentFilesManager;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.UserTreePathManager;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.simulation.ModelSource;
import org.systemsbiology.biotapestry.ui.CursorManager;
import org.systemsbiology.biotapestry.ui.DisplayOptionsManager;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.GroupPanel;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.OverlayStateWriter;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.TextBoxManager;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Essential current application state
*/

public class BTState { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // For server use:
  //
  
  private WebServerControlFlowHarness currCfh_;
  private SerializableDialogPlatform.Dialog lastWebDialog_;
  private SimpleUserFeedback lastSuf_;
  private WebServerControlFlowHarness.PendingMouseClick pendingClick_;

  //
  // General use:
  //
  
  private SimParamSource sps_;
  private FlowMeister fm_;
  private File currentFile_;
  private LoadSaveSupport lsSup_;
  private String sid_;
  private ResourceManager rMan_;

  private CommonView cView_;
  
  private Metabase mBase_;
  private HashMap<String, PerTab> perTab_;
  private ArrayList<String> tabOrder_;
  private String currTab_;
    
  //
  // Controls that used to be implemented purely as Swing
  // now can operate in both Swing and Headless mode:
  //
  
  //
  // These are shared by all tabs. Some e.g. Path control, are
  // restocked with each tab switch.
  //
  
  private VirtualGaggleControls vgc_;
  private VirtualRecentMenu vrm_; 
  private VirtualPathControls vpc_;
 
  private UndoManager undo_;
  private JFrame topFrame_;

  private boolean doGaggle_;
  private String gaggleSpecies_;
  private boolean isEditor_;
  private Map<String, Object> args_;
  private JComponent topContent_;
  private FontRenderContext frc_;

  private boolean isHeadless_;
  private boolean isWebApplication_;
  private JFrame tempTop_;
  private boolean bigScreen_;
  private EventManager eventMgr_;
  private DataPopupManager dpm_;
  private DisplayOptionsManager dom_;
  private PlugInManager plum_;
  
  private PrintWriter out_;
  private ImageManager imageMgr_;
  private RecentFilesManager rfm_;
  private FontManager fom_;
  private GooseManager goomr_;
  private LayoutOptionsManager lopm_;
  private ExceptionHandler exh_;
  
  private int undoChangeCount_;
  private HashMap<String, Boolean> status_;  // Remember e.g. yes no shutup state
  private CmdSource.SearchModifiers searchMod_; 
  
  private boolean amPulling_;  // Is pulldown mode in effect
 
  private ModelSource mSrc_;
  
  private UIComponentSource uics_;
  private TabSource tSrc_;
  private CmdSource cSrc_;
  private RememberSource rSrc_;
  private PathAndFileSource pafs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor for disaster only
  */ 
   
  public BTState() {
    rMan_ = new ResourceManager();
    uics_ = new UIComponentSource(this);
  }
   
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BTState(String session, Map<String, Object> args, boolean isHeadless, boolean isWebApplication) {
    sid_ = session;
    isHeadless_ = isHeadless;
    isWebApplication_ = isWebApplication;

    UiUtil.fixMePrintout("All tabs getting notified of a change in one tab??");
    eventMgr_ = new EventManager();
    imageMgr_ = new ImageManager();
    goomr_ = new GooseManager();
    
    uics_ = new UIComponentSource(this);
    tSrc_ = new TabSource(this);
    mBase_ = new Metabase(this, uics_, tSrc_);
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(this, mBase_, isWebApplication);
    mBase_.setDDacx(ddacx);
    
    fom_ = new FontManager();
    rfm_ = new RecentFilesManager(uics_);
    lopm_ = new LayoutOptionsManager(ddacx);
    rMan_ = new ResourceManager();
   
    //
    // Plugin manager needs to start before database:
    //
    
    plum_ = new PlugInManager(ddacx, uics_);
    
    // DB gotta get going before DOM:
    perTab_ = new HashMap<String, PerTab>();
    tabOrder_ = new ArrayList<String>();

    currTab_ = mBase_.getNextDbID();
    perTab_.put(currTab_, new PerTab(currTab_));
    perTab_.get(currTab_).initRcx(this);
    mBase_.addDB(0, currTab_);
    
    tabOrder_.add(currTab_);
    
    dom_ = new DisplayOptionsManager();
    cSrc_ = new CmdSource(this, uics_);
    rSrc_ = new RememberSource(this);
    pafs_ = new PathAndFileSource(this);
    
    args_ = args;

    gaggleSpecies_ = ((String)args_.get(ArgParser.GAGGLE));
    doGaggle_ = (gaggleSpecies_ != null);
    Boolean doBig = ((Boolean)args_.get(ArgParser.BIG_SCREEN));
    if (doBig == null) {
      doBig = new Boolean(false);
    }
    bigScreen_ = doBig.booleanValue(); 
    dpm_ = new DataPopupManager(ddacx, uics_, tSrc_);
    undoChangeCount_ = 0;
    status_ = new HashMap<String, Boolean>();
    searchMod_ = new CmdSource.SearchModifiers();
    amPulling_ = false;
    sps_ = new SimParamSource();
   
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  ** 
  ** Get tab-pinned data access contexts
  */  
  
  Map<String, TabPinnedDynamicDataAccessContext> getTabContexts() { 
    HashMap<String, TabPinnedDynamicDataAccessContext> retval = new HashMap<String, TabPinnedDynamicDataAccessContext>();     
    for (String tabKey : perTab_.keySet()) {
      retval.put(tabKey, perTab_.get(tabKey).rcx_);
    }
    return (retval);   
  }
  
  /***************************************************************************
  ** 
  ** Get single tab-pinned data access context
  */  
  
  TabPinnedDynamicDataAccessContext getTabContext(String tabKey) {
    return (perTab_.get(tabKey).rcx_);
  }

  /***************************************************************************
  ** 
  ** Get the various application sources
  */   
  
  public AppSources getAppSources() {
    return (new AppSources(this));
  }
  
  /***************************************************************************
  ** 
  ** Certain saved state is handled by RememberSource
  */   
  
  PathAndFileSource getPathAndFileSource() {
    return (pafs_);
  }
  
  /***************************************************************************
  ** 
  ** Certain saved state is handled by RememberSource
  */   
  
  RememberSource getRememberSource() {
    return (rSrc_);
  }

  /***************************************************************************
  ** 
  ** Command access is handled by CmdSource:
  */   
  
  CmdSource getCmdSource() {
    return (cSrc_);
  }

  /***************************************************************************
  ** 
  ** Tabbing state is handled by the TabSource:
  */   
  
  TabSource getTabSource() {
    return (tSrc_);
  }

  /***************************************************************************
  ** 
  ** UI Components are handled by the UIComponentSource:
  */   
  
  UIComponentSource getUIComponentSource() {
    return (uics_);
  }
 
  /***************************************************************************
  ** 
  ** get if group node is selected
  */   
  
  public boolean groupNodeSelected() {
    return (perTab_.get(currTab_).groupNodeSelected_);
  }
 
  /***************************************************************************
  ** 
  ** set if group node is selected
  */
 
  public void setGroupNodeSelected(boolean selected) {
    perTab_.get(currTab_).groupNodeSelected_ = selected;
    return;
  }

  /***************************************************************************
  ** 
  ** get the Metabase
  */
  
  Metabase getMetabase() { 
    return (mBase_);  
  } 
  
  /***************************************************************************
  ** 
  ** reset data for our one tab on load
  */
  
  void resetTabForLoadX(String dbID, int tabNum) throws IOException {

    if ((perTab_.size() != 1) || (tabOrder_.size() != 1) || (tabNum != 0)) {
      throw new IOException(); 
    }   
    mBase_.resetZeroDB(tabNum, dbID);
    PerTab pt = perTab_.get(currTab_);
    currTab_ = dbID;
    perTab_.put(currTab_, pt);
    tabOrder_.clear();
    tabOrder_.add(tabNum, currTab_); 
    return;  
  } 
  
  /***************************************************************************
  ** 
  ** Add a new tab. If dbID != null, we are specifying database from IO load. Else
  ** this is what creates the database for the new tab.
  */
  
  TabChange addATabX(boolean forLoad, String dbID, int tabNum) {
	  
    TabChange retval = new TabChange(false);
    retval.didTabChange = false;
    retval.newCurrIndexPre = getCurrentTabIndexX();
    retval.newChangeIndex = (!forLoad) ? tabOrder_.size() : tabNum; 
    
    if (dbID == null) {
      currTab_ = mBase_.getNextDbID();
      retval.newBTPerTab = new PerTab(currTab_);
      perTab_.put(currTab_, retval.newBTPerTab);
      retval.newBTPerTab.initRcx(this);
      mBase_.addDB(retval.newChangeIndex, currTab_);
    } else {
      currTab_ = dbID;
      retval.newBTPerTab = new PerTab(currTab_);
      perTab_.put(currTab_, retval.newBTPerTab);
      retval.newBTPerTab.initRcx(this);
      mBase_.loadDB(tabNum, dbID);
    }
    retval.newDB = mBase_.getDB(currTab_);
    retval.newDbId = retval.newDB.getID();

   
    tabOrder_.add(retval.newChangeIndex, currTab_);
    
    retval.newCurrIndexPost = getCurrentTabIndexX();
    return (retval);  
  } 
  
  /***************************************************************************
  ** 
  ** Remove a tab
  */
  
  TabChange removeATabX(int tabIndex, int currentCurrent, int newCurrent) {
    if (perTab_.size() == 1) {
      return (null);
    }
    TabChange retval = new TabChange(false);
    retval.didTabChange = false;
    retval.oldCurrIndexPre = currentCurrent;
    retval.oldChangeIndex = tabIndex;
    retval.oldDbId = tabOrder_.remove(tabIndex);    
    retval.oldDB = mBase_.removeDB(retval.oldDbId);
    retval.oldBTPerTab = perTab_.remove(retval.oldDbId);
    if (tabIndex == retval.oldCurrIndexPre) {
      if (perTab_.size() == tabIndex) {
        currTab_ = tabOrder_.get(retval.oldCurrIndexPre - 1);
      } else {
        currTab_ = tabOrder_.get(retval.oldCurrIndexPre);
      }
    } else if (tabIndex < retval.oldCurrIndexPre) {
      currTab_ = tabOrder_.get(retval.oldCurrIndexPre - 1);
    }
    
    //
    // Have to reset the indices of all the databases above the deletion:
    //
    Iterator<String> idIt = mBase_.getDBIDs();
    while (idIt.hasNext()) {
      String dbid = idIt.next();
      Database dbNow = mBase_.getDB(dbid);
      int oldIndex = dbNow.getIndex();
      if (oldIndex > tabIndex) {
        dbNow.setIndex(oldIndex - 1);
        Integer[] onindx = new Integer[2];
        onindx[0] = Integer.valueOf(oldIndex);
        onindx[1] = Integer.valueOf(oldIndex - 1);
        retval.reindex.put(dbid, onindx);
      }
    }
    
    retval.oldCurrIndexPost = newCurrent;
    return (retval);   
  }
  
 
  /***************************************************************************
  **
  ** Undo a tab change
  */
  
  void changeTabUndoX(TabChange undo) {
    if (undo.didTabChange) {
      setCurrentTabIndexX(undo.changeTabPreIndex);
    } else if (undo.oldDB != null) {
      perTab_.put(undo.oldDbId, undo.oldBTPerTab);
      mBase_.restoreDB(undo.oldDB);
      tabOrder_.add(undo.oldChangeIndex, undo.oldDbId);
      currTab_ = tabOrder_.get(undo.oldCurrIndexPre);
      //
      // Have to reset the indices of all the databases above the deletion:
      //
      Iterator<String> idIt = undo.reindex.keySet().iterator();
      while (idIt.hasNext()) {
        String dbid = idIt.next();
        Integer[] onindx = undo.reindex.get(dbid);
        Database dbNow = mBase_.getDB(dbid);
        dbNow.setIndex(onindx[0].intValue());
      }
    } else if (undo.newDB != null) {
      tabOrder_.remove(undo.newDbId);
      mBase_.removeDB(undo.newDbId);
      perTab_.remove(undo.newBTPerTab);
      currTab_ = tabOrder_.get(undo.newCurrIndexPre);   
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a tab change
  */
  
  void changeTabRedoX(TabChange redo) {
    if (redo.didTabChange) {
      setCurrentTabIndexX(redo.changeTabPostIndex);
    } else if (redo.oldDB != null) {
      tabOrder_.remove(redo.oldDbId);
      mBase_.removeDB(redo.oldDbId);
      perTab_.remove(redo.oldBTPerTab);
      currTab_ = tabOrder_.get(redo.oldCurrIndexPost);
      //
      // Have to reset the indices of all the databases above the deletion:
      //
      Iterator<String> idIt = redo.reindex.keySet().iterator();
      while (idIt.hasNext()) {
        String dbid = idIt.next();
        Integer[] onindx = redo.reindex.get(dbid);
        Database dbNow = mBase_.getDB(dbid);
        dbNow.setIndex(onindx[1].intValue());
      }
    } else if (redo.newDB != null) {
      perTab_.put(redo.newDbId, redo.newBTPerTab);
      mBase_.restoreDB(redo.newDB);
      tabOrder_.add(redo.newChangeIndex, redo.newDbId);
      currTab_ = tabOrder_.get(redo.newCurrIndexPost);   
    }
    return;
  }

   /*************
   * getTabs
   ************* 
   * 
   * Returns the tab name data needed to generate an 
   * Array of the current set of tabs based on tabOrder_
   * which contains XPlatTab objects, which house information needed
   * by the WebClient
   * 
   * 
   */
  
  List<TabSource.AnnotatedTabData> getTabsX() {  
    ArrayList<TabSource.AnnotatedTabData> tabs = new ArrayList<TabSource.AnnotatedTabData>();
    for(String tab : this.tabOrder_) {
      Database tabDB = mBase_.getDB(tab);
      tabs.add(new TabSource.AnnotatedTabData(tab, tabDB.getTabNameData()));
    }
   return tabs; 
  }
 
  /***************************************************************************
  ** 
  ** Not efficient, but tiny, and this way we don't have to maintain a 
  ** separate data structure:
  */  
  
  int getTabIndexFromIdX(String id) {
    for (int i = 0; i < tabOrder_.size(); i++) {
      if (tabOrder_.get(i).equals(id)) {
        return (i);
      }
    }
	  throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  ** 
  ** Need to be able to rename a tab that is not current; need info to fill in
  */
  
  TabNameData getTabNameDataForIndexX(int index) {  
    Database tabDB = mBase_.getDB(tabOrder_.get(index));
    return (tabDB.getTabNameData()); 
  }
  
  /***************************************************************************
  ** 
  ** Need to get dbid for given index
  */
  
  String getDbIdForIndexX(int index) {  
    return (tabOrder_.get(index));
  }

  /***************************************************************************
  ** 
  ** Get current tab
  */
  
  String getCurrentTabX() {
    return (currTab_);  
  }  
  
  /***************************************************************************
  ** 
  ** Get current tab
  */
  
  int getCurrentTabIndexX() {
    int num = getNumTabX();
    for (int i = 0; i < num; i ++) {
      String oid = tabOrder_.get(i);
      if (currTab_.equals(oid)) {
        return (i);
      }
    }
    throw new IllegalStateException();
  } 

  /***************************************************************************
  ** 
  ** Get number of tabs
  */
  
   int getNumTabX() {
    return (perTab_.size());  
  } 
  
  /***************************************************************************
  ** 
  ** Set current tab
  */

  TabChange setCurrentTabIndexX(String tabId) {
	  return (this.setCurrentTabIndexX(getTabIndexFromIdX(tabId)));
  }
  
  /***************************************************************************
  ** 
  ** Set current tab
  */

  TabChange setCurrentTabIndexX(int cTab) {
    if ((cTab < 0) || cTab >= perTab_.size()) {
      throw new IllegalArgumentException();
    }
    TabChange retval = new TabChange(false);
    retval.didTabChange = true;
    retval.changeTabPreIndex = getCurrentTabIndexX();
    currTab_ = tabOrder_.get(cTab);
    retval.changeTabPostIndex = cTab;
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** On reset, we need to get ourselves back in sync with the Metabase state.
  */
  
  void clearCurrentTabIDX(String id) {
    currTab_ = id;
    UiUtil.fixMePrintout("Illegal state here when reading multi-tab file with bad XML");
    if ((perTab_.size() != 1) || (tabOrder_.size() != 1)) {
      throw new IllegalStateException();
    }
    
    String oneKey = perTab_.keySet().iterator().next();
    PerTab onePT = perTab_.get(oneKey);
    perTab_.clear();
    perTab_.put(id, onePT);
    
    tabOrder_.set(0, id);
    return;
  } 

  /***************************************************************************
  ** 
  ** Set current tab
  */
  
  void setCurrentTabIDX(String id) {
    currTab_ = id;
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get simulator parameter source
  */
  
  SimParamSource getSimParamSource() {
    return (sps_);  
  }  
  
  /***************************************************************************
  ** 
  ** Get simulator model source
  */
  
  public ModelSource getModelSource() {
    return (mSrc_);  
  }
  
  /***************************************************************************
  ** 
  ** Set simulator model source
  */
  
  public void setModelSource(ModelSource mSrc) {
    mSrc_ = mSrc; 
    return;
  }   
  
  /***************************************************************************
  ** 
  ** For setup only! This context is used to drive the zoomer state only
  */
  
  public DataAccessContext getRenderingContextForZTS() {
    return (perTab_.get(currTab_).rcx_);  
  }     

  /***************************************************************************
  ** 
  ** Get the VirtualZoomControls
  */
  
  VirtualZoomControls getVirtualZoomX() {
    return (perTab_.get(currTab_).vzc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualZoomControls
  */
  
  void setVirtualZoomX(VirtualZoomControls vzc) {
    perTab_.get(currTab_).vzc_ = vzc; 
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get the VirtualRecentMenu
  */
  
  VirtualRecentMenu getRecentMenu() {
    return (vrm_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualRecentMenu
  */
  
  public void setRecentMenu(VirtualRecentMenu vrm) {
    vrm_ = vrm; 
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the VirtualGaggleControls
  */
  
  VirtualGaggleControls getGaggleControls() {
    return (vgc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualGaggleControls
  */
  
  public void setGaggleControls(VirtualGaggleControls vgc) {
    vgc_ = vgc; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the VirtualPathControls
  */
  
  VirtualPathControls getPathControls() {
    return (/*perTab_.get(currTab_).*/vpc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualPathControls
  */
  
  public void setPathControls(VirtualPathControls vpc) {
    /*perTab_.get(currTab_).*/ vpc_ = vpc; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the Web Server Control Flow Harness
  */
  
  public WebServerControlFlowHarness getHarness() {
    return (currCfh_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the Web Server Control Flow Harness
  */
  
  public void setHarness(WebServerControlFlowHarness cfh) {
    currCfh_ = cfh; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the SimpleBrowserDialogPlatform.Dialog
  */
  
  SerializableDialogPlatform.Dialog getDialog() {
    return (lastWebDialog_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the SimpleBrowserDialogPlatform.Dialog
  */
  
  void setDialog(SerializableDialogPlatform.Dialog lastWebDialog) {
    lastWebDialog_ = lastWebDialog; 
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get the SimpleUserFeedback
  */
  
  SimpleUserFeedback getSUF() {
    return (lastSuf_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set theSimpleUserFeedback
  */
  
  void setSUF(SimpleUserFeedback lastSuf) {
    lastSuf_ = lastSuf; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the PendingMouseClick
  */
  
  WebServerControlFlowHarness.PendingMouseClick getPendingClick() {
    return (pendingClick_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the PendingMouseClick
  */
  
  void setPendingClick(WebServerControlFlowHarness.PendingMouseClick pendingClick) {
    pendingClick_ = pendingClick; 
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the Flow meister
  */
  
  FlowMeister getFloMX() {
    return (fm_);  
  }   
  
  /***************************************************************************
  **
  ** Return last down prop target
  */
  
  PropagateDown.DownPropState getDownProp() {
    return (perTab_.get(currTab_).downProp_);  
  }   

  /***************************************************************************
  **
  ** Are we doing pulldown?
  */
  
  boolean getAmPulling() {
    UiUtil.fixMePrintout("Should this be per tab??");
    UiUtil.fixMePrintout("DANGER! Note that with pulldown dialog up, can still launch another pulldown window!");
    return (amPulling_);  
  }   

  /***************************************************************************
  **
  ** Set if we doing pulldown
  */
  
  void setAmPulling(boolean amPulling) {
    amPulling_ = amPulling;
    return;
  }   
  
  /***************************************************************************
  **
  ** Bump up undo count
  */
  
  void bumpUndoCount() {
    undoChangeCount_++;  
    return;
  }   

  /***************************************************************************
  **
  ** Clear the change tracking
  */
  
  void clearUndoTracking() {
    undoChangeCount_ = 0;  
    return;
  }  
 
  /***************************************************************************
  **
  ** Answer if a change has occurred since last clear.  FIX ME: We don't
  ** account for undone/redone changes.
  */
  
  boolean hasAnUndoChange() {
    return (undoChangeCount_ > 0);
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
       
   LayoutOptionsManager getLayoutOptMgr() {
     return (lopm_);
   } 
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  UserTreePathManager getPathMgr() {
    return (perTab_.get(currTab_).utpm_);
  } 

  /***************************************************************************
  **
  ** Command
  */ 

  boolean hasPaths() {
	  boolean hasPaths = false;
	  Set<String> tabs = perTab_.keySet();
	  
	  Iterator<String> tabIt = tabs.iterator();
	  
	  while(tabIt.hasNext() && !hasPaths) {
		  String thisTab = tabIt.next();
		  hasPaths = (perTab_.get(thisTab).utpm_.getPathCount() > 0);
	  }
	  
	  return hasPaths;
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   GooseManager getGooseMgr() {
     return (goomr_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   FontManager getFontMgr() {
     return (fom_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   RecentFilesManager getRecentFilesMgr() {
     return (rfm_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   public GroupSettingManager getGroupMgrX(String tab) {
     return (perTab_.get(tab).gsm_);
   }  
   
  /***************************************************************************
  **
  ** Command
  */ 
      
   GroupSettingManager getGroupMgrX() {
     return (getGroupMgrX(currTab_));
   }  

  /***************************************************************************
  **
  ** Command
  */ 
     
  void setCurrentPrintWriterX(PrintWriter out) {
    out_ = out;
    return;
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
     
  PrintWriter getCurrentPrintWriterX() {
    return (out_);
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
     
  PlugInManager getPlugInMgrX() {
    return (plum_);
  }   
   
  /***************************************************************************
  **
  ** Command
  */ 
     
  DisplayOptionsManager getDisplayOptMgrX() {
    return (dom_);
  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  DataPopupManager getDataPopupMgrX() {
    return (dpm_);
  } 
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  EventManager getEventMgrX() {
    return (eventMgr_);
  } 
   
  /***************************************************************************
  **
  ** Command
  */ 
    
  ImageManager getImageMgrX() {
    return (imageMgr_);
  } 
 
  /****************************************************************************
  **
  ** Push down a temp top frame
  */  
   
  public void pushTopFrameX(JFrame newTop) {
    tempTop_ = newTop;
    return;
  }  
  
  /****************************************************************************
   **
   ** Pop a temp top frame
   */  
    
   public void popTopFrameX() {
     tempTop_ = null;
     return;
   }  

  /****************************************************************************
  **
  ** Get OverlayStateOracle
  */  
   
  public OverlayStateOracle getOSOX() {
    return (getOSOX(currTab_));
  }
  
  /****************************************************************************
  **
  ** Get OverlayStateOracle
  */  
   
  OverlayStateOracle getOSOX(String tab) {
    return (perTab_.get(tab).ptoso_);
  }
  
  /****************************************************************************
  **
  ** Get OverlayState Writer
  */  
   
  public OverlayStateWriter getOverlayWriterX(String tab) {
    return (perTab_.get(tab).ptoso_);
  }
  
  /****************************************************************************
  **
  ** Get OverlayState Writer
  */  
   
  public OverlayStateWriter getOverlayWriter() {
    return (getOverlayWriterX(currTab_));
  }
  
  /****************************************************************************
  **
  ** Get Database
  */  
   
  Database getDBX() {
    return (getDBX(currTab_));
  }
  
  /****************************************************************************
  **
  ** Get Database
  */  
   
  Database getDBX(String tab) {
    return (mBase_.getDB(tab));
  }
  
  /****************************************************************************
  **
  ** Get Resource Manager
  */  
   
  public ResourceManager getRManX() {
    return (rMan_);
  }
  
  /****************************************************************************
  **
  ** Get Args
  */  
   
  public Map<String, Object> getArgs() {
    return (args_);
  }
  
  /****************************************************************************
  **
  ** Set TopFrame
  */  
  
  public BTState setTopFrameX(JFrame topFrame, JComponent contentPane) {
    topFrame_ = topFrame;
    topContent_ = contentPane;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Set ExceptionHandler
  */  
   
  void setExceptionHandlerX(ExceptionHandler exh) {
    synchronized (this) {
      exh_ = exh;
    }
    return;
  }
  
  /****************************************************************************
  **
  ** Get ExceptionHandler
  */  
   
  public ExceptionHandler getExceptionHandlerX() {
    synchronized (this) {
      return (exh_);
    }
  }
  
  /****************************************************************************
  **
  ** Set status value
  */  
   
  void setStatus(String key, boolean val) {
    status_.put(key, Boolean.valueOf(val));
    return;
  }
  
  /****************************************************************************
  **
  ** Get status value
  */  
   
  Boolean getStatus(String key) {
    return (status_.get(key));
  }  
   
  /****************************************************************************
  **
  ** Answer if headless
  */  
   
  boolean isHeadless() {
    return (isHeadless_);
  }
 
  /****************************************************************************
  **
  ** Answer if web application
  */  
   
  public boolean isWebApplication() {
    return (isWebApplication_);
  }
  
  /****************************************************************************
  **
  ** Get TopFrame
  */  
   
  JFrame getTopFrame() {
    if (tempTop_ != null) {
      return (tempTop_);
    }
    return (topFrame_);
  }
 
  /****************************************************************************
  **
  ** Get top container
  */  
   
  public JComponent getContentPane() {
    // Hack for headless:
    return ((topContent_ == null) ? new JPanel() : topContent_);
  }
   
  /****************************************************************************
  **
  ** Set current file
  */  
  
  void setCurrentFile(File currentFile) {
    currentFile_ = currentFile;
    return;
  }
  
  /****************************************************************************
  **
  ** Get current file
  */  
   
  File getCurrentFile() {
    return (currentFile_);
  }
  
  /****************************************************************************
  **
  ** Set load save support
  */  
  
  public void setLSSupport(LoadSaveSupport lsSup) {
    lsSup_ = lsSup;
    return;
  }
  
  /****************************************************************************
  **
  ** Get load save support
  */  
   
  LoadSaveSupport getLSSupport() {
    return (lsSup_);
  }
  
  /****************************************************************************
   **
   ** Set CommonView
   */  
   
   BTState setCommonView(CommonView cView) {
     cView_ = cView;
     return (this);
   }
   
   /****************************************************************************
   **
   ** Get CommonView
   */  
    
   CommonView getCommonView() {
     return (cView_);
   }
  
  /****************************************************************************
  **
  ** Set Cursor Manager
  */  
  
  BTState setCursorManager(CursorManager cursorMgr) {
    perTab_.get(currTab_).cursorMgr_ = cursorMgr;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Cursor Manager
  */  
   
  CursorManager getCursorMgr() {
    return (perTab_.get(currTab_).cursorMgr_);
  }
  
  /****************************************************************************
  **
  ** Set Path Controller
  */  
  
  BTState setPathControllerX(UserTreePathController utpControl) {
    perTab_.get(currTab_).utpControl_ = utpControl;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Path Controller
  */  
   
  UserTreePathController getPathControllerX() {
    return (perTab_.get(currTab_).utpControl_);
  }

  /****************************************************************************
  **
  ** Set TextBoxManager
  */  
  
  public BTState setTextBoxManagerX(TextBoxManager textMgr) {
    perTab_.get(currTab_).textMgr_ = textMgr;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get TextBoxManager
  */  
   
  TextBoxManager getTextBoxMgrX() {
    return (perTab_.get(currTab_).textMgr_);
  }

  /****************************************************************************
  **
  ** Get Font render context
  */  
   
  FontRenderContext getFontRenderContext() {
    return (frc_);
  }
  
 /****************************************************************************
  **
  ** Set Font render context
  */  
   
  public void setFontRenderContext(FontRenderContext frc) {
    frc_ = frc;
    return;
  }
  
  /****************************************************************************
  **
  ** Set if is editor
  */  
  
  BTState setIsEditor(boolean isEdit) {
    isEditor_ = isEdit;
    // needs to be initialized even for viewer since tree navigation creates undos that are tossed away...
    undo_ = new UndoManager();
    undo_.setLimit(50); 
    return (this);
  }
 
  /****************************************************************************
  **
  ** Get if is editor
  */  
  
  boolean getIsEditorX() {
    return (isEditor_);
  }
  
  /****************************************************************************
  **
  ** Get if gaggle-enabled
  */  
  
  boolean getDoGaggleX() {
    return (doGaggle_);
  }
  
  /****************************************************************************
  **
  ** Get if using large fonts
  */  
  
  boolean doBigX() {
    return (bigScreen_);
  }
   
  /****************************************************************************
  **
  ** Get gaggle species
  */  
  
  public String getGaggleSpecies() {
    return (gaggleSpecies_);
  }
   
  /****************************************************************************
  **
  ** set Pop Commands
  */  
  
  BTState setPopCmdsX(PopCommands popCmds) {
    perTab_.get(currTab_).popCmds_ = popCmds;
    return (this);
  }
  
  /****************************************************************************
   **
   ** Get Pop Commands
   */  
   
   PopCommands getPopCmdsX() {
     return (perTab_.get(currTab_).popCmds_);
   } 
  
  /****************************************************************************
  **
  ** Get Panel Commands
  */  
  
  PanelCommands getPanelCmdsX() {  
    return (perTab_.get(currTab_).pCmds_);
  }
  
  /****************************************************************************
  **
  ** set Group Panel Commands
  */  
  
  BTState setGroupPanelCmdsX(GroupPanelCommands gpCmds) {
    perTab_.get(currTab_).gpCmds_ = gpCmds;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Group Panel Commands
  */  
  
  GroupPanelCommands getGroupPanelCmdsX() {
    return (perTab_.get(currTab_).gpCmds_);
  }
  
  /****************************************************************************
  **
  ** set Panel Commands
  */  
  
  BTState setPanelCmdsX(PanelCommands pCmds) {
    perTab_.get(currTab_).pCmds_ = pCmds;
    return (this);
  }
  

  /****************************************************************************
  **
  ** set ZoomCommandSupport
  */  
   
  BTState setZoomCommandSupportX(ZoomCommandSupport zcs) {
    perTab_.get(currTab_).zcs_ = zcs;
    return (this);
  }
   
   /***************************************************************************
   **
   ** Used to get the ZoomCommandSupport
   */
   
   ZoomCommandSupport getZoomCommandSupportX() {
     return (perTab_.get(currTab_).zcs_);
   }
  
   /***************************************************************************
   **
   ** Used to get the ZoomCommandSupport
   */
   
   ZoomCommandSupport getZoomCommandSupportForTabX(int index) {
     return (perTab_.get(tabOrder_.get(index)).zcs_);
   }
   
  /****************************************************************************
  **
  ** set time slider
  */  
  
  BTState setVTSliderX(VirtualTimeSlider vtSlider) {
    perTab_.get(currTab_).vtSlider_ = vtSlider;
    return (this);
  }
  
  /***************************************************************************
  **
  ** Used to get the time slider
  */
  
   VirtualTimeSlider getVTSliderX() {
    return (perTab_.get(currTab_).vtSlider_);
  }
  
  /***************************************************************************
  **
  ** Used to get the display panel
  */
  
  SUPanel getSUPanelX() {
    return (perTab_.get(currTab_).sup_);
  }
 
  /****************************************************************************
  **
  ** set the display panel
  */  
   
  BTState setSUPanelX(SUPanel sup) {
    perTab_.get(currTab_).sup_ = sup;
    return (this);
  }
  
  /***************************************************************************
  **
  ** Used to get the group panel
  */
  
  GroupPanel getGroupPanel() {
    return (perTab_.get(currTab_).gup_);
  }
 
  /****************************************************************************
  **
  ** set the group panel
  */  
   
  public BTState setGroupPanel(GroupPanel gup) {
    perTab_.get(currTab_).gup_ = gup;
    return (this);
  }

  /****************************************************************************
  **
  ** set the tree
  */  
  
  BTState setVmTreeX(VirtualModelTree vmTree) {
    perTab_.get(currTab_).vmTree_ = vmTree;
    return (this);
  }
 
  /***************************************************************************
  **
  ** Used to get the tree
  */
  
  VirtualModelTree getTreeX() {
    return (perTab_.get(currTab_).vmTree_);
  }
  
  /****************************************************************************
  **
  ** set the Zoom target
  */  
  
  BTState setZoomTargetX(ZoomTargetSupport zoomer) {
    perTab_.get(currTab_).zoomer_ = zoomer;
    return (this);
  }
 
  /***************************************************************************
  **
  ** Used to get the Zoom target
  */
  
  ZoomTargetSupport getZoomTargetX() {
    return (getZoomTargetX(currTab_));
  }
    
  /***************************************************************************
  **
  ** Used to get the Zoom target
  */
  
  ZoomTargetSupport getZoomTargetX(String currTab) {
    return (perTab_.get(currTab).zoomer_);
  }

  /***************************************************************************
  **
  ** Used to get the undo manager
  */
  
  UndoManager getUndoManager() {
    return (undo_);
  }  

  /****************************************************************************
  **
  ** set the renderer
  */  
    
  BTState setGenomePresentationX(GenomePresentation myGenomePre) {
    perTab_.get(currTab_).myGenomePre_ = myGenomePre;
    return (this);
  }
  
  /****************************************************************************
  **
  ** get the Net Overlay Controller
  */  
    
  NetOverlayController getNetOverlayControllerX() {
    return (perTab_.get(currTab_).noc_);
  }
  
  /****************************************************************************
  **
  ** Set the NetOverlayController
  */  
    
  BTState setNetOverlayControllerX(NetOverlayController noc) {
    perTab_.get(currTab_).noc_ = noc;
    return (this);
  }
  
  /****************************************************************************
  **
  ** get the renderer
  */  
    
  GenomePresentation getGenomePresentationX() {
    return (perTab_.get(currTab_).myGenomePre_);
  }
  
  /***************************************************************************
  **
  ** Set the genome without a selection change; used during UNDO/ REDO ONLY!
  */
  
  public void setGenomeForUndo(String key, DataAccessContext dacx) {
    perTab_.get(currTab_).genomeKey_ = key;
    //
    // If dacx is static, gotta keep it in sync!
    //
    if (dacx instanceof StaticDataAccessContext) {
      ((StaticDataAccessContext)dacx).setGenome((key == null) ? null : dacx.getGenomeSource().getGenome(key));
      ((StaticDataAccessContext)dacx).setLayout((key == null) ? null : dacx.getLayoutSource().getLayoutForGenomeKey(key));
    }
    perTab_.get(currTab_).textMgr_.setMessageSource(perTab_.get(currTab_).genomeKey_, TextBoxManager.MODEL_MESSAGE, true);
    
    NetOverlayController noc = getNetOverlayControllerX();
   // if ((key != null) && noc.hasPreloadForNextGenome(key)) {
      noc.setForNewGenome(perTab_.get(currTab_).genomeKey_, null, null, dacx);
  //  }
      
    // This makes no sense in the current set-up? A dynamic DACX tracks current genome automatically, from genomeKey_ = key. You CANNOT set it! 
    perTab_.get(currTab_).rcx_.setGenome((perTab_.get(currTab_).genomeKey_ == null) ? null : perTab_.get(currTab_).rcx_.getGenomeSource().getGenome(key));
    return;
  }  
   
  /***************************************************************************
  **
  ** Used to set the genome
  */
  
  public void setGenome(String key, UndoSupport support, DataAccessContext dacx) {	
    cSrc_.getPanelCmds().cancelAddMode(PanelCommands.CANCEL_ADDS_ALL_MODES);
    boolean updateModules = false;
   
    // Note this omits case of (genomeKey == null) && (key != null), which
    // OK in the context of clearing selections (key == null means nothing is
    // selected and nothing needs clearing:
    if ((key == null) || ((perTab_.get(currTab_).genomeKey_ != null) && !perTab_.get(currTab_).genomeKey_.equals(key))) {
      // FIX ME:  Better yet, cache selections and reinstall later?
      StaticDataAccessContext rcx2 = new StaticDataAccessContext(perTab_.get(currTab_).rcx_,
                                                                 perTab_.get(currTab_).genomeKey_, 
                                                                 perTab_.get(currTab_).layout_);
      perTab_.get(currTab_).myGenomePre_.clearSelections(new UIComponentSource(this), rcx2, support);
      updateModules = true;
    }
     
     //
     // For module updates, we need to catch the extra case, plus when we are waiting for
     // a user path change:
     OverlayDisplayChange odc = null;
     NetOverlayController noc = getNetOverlayControllerX();
     
     if (updateModules || ((perTab_.get(currTab_).genomeKey_ == null) && (key != null)) || noc.hasPreloadForNextGenome(key)) {
       updateModules = true;
       // Gotta send all the old info to the overlay controller so it can generate
       // consistent undo packages:
       odc = new OverlayDisplayChange();
       odc.oldGenomeID = perTab_.get(currTab_).genomeKey_;
       odc.oldOvrKey = perTab_.get(currTab_).currentOverlay_;
       odc.oldModKeys = perTab_.get(currTab_).currentNetMods_.clone();
       odc.oldRevealedKeys = perTab_.get(currTab_).revealed_.clone();
     }
     perTab_.get(currTab_).genomeKey_ = key;
     //
     // If dacx is static, gotta keep it in sync!
     //
     if (dacx instanceof StaticDataAccessContext) {
       ((StaticDataAccessContext)dacx).setGenome((key == null) ? null : dacx.getGenomeSource().getGenome(key));
       ((StaticDataAccessContext)dacx).setLayout((key == null) ? null : dacx.getLayoutSource().getLayoutForGenomeKey(key));
     }
    
     perTab_.get(currTab_).textMgr_.setMessageSource(perTab_.get(currTab_).genomeKey_, TextBoxManager.MODEL_MESSAGE, true);
     if (updateModules) {
       noc.setForNewGenome(perTab_.get(currTab_).genomeKey_, support, odc, dacx);
     }
     perTab_.get(currTab_).rcx_.setGenome((perTab_.get(currTab_).genomeKey_ == null) ? null : perTab_.get(currTab_).rcx_.getGenomeSource().getGenome(key));

     return; 
  } 
  
  /***************************************************************************
  **
  ** Used to get the genome
  */
  
  String getGenomeX() {
    return (perTab_.get(currTab_).genomeKey_);
  }
  
  /***************************************************************************
  **
  ** Used to get the layout
  */
   
  String getLayoutKeyX() {
    return (perTab_.get(currTab_).layout_);
  } 
  
  /***************************************************************************
  **
  ** Set the layout
  */
  
  public void setGraphLayout(String key) {
    perTab_.get(currTab_).layout_ = key;
    perTab_.get(currTab_).rcx_.setLayout(((perTab_.get(currTab_).layout_ == null) ? null : perTab_.get(currTab_).rcx_.getLayoutSource().getLayout(perTab_.get(currTab_).layout_)));
    return;
  }
  
  /***************************************************************************
  **
  ** Set the state to show module components.
  */
  
  void toggleModuleComponentsX() {
    perTab_.get(currTab_).showingNetModuleComponents_ = !perTab_.get(currTab_).showingNetModuleComponents_;
 //   if (isShowBubbleMode(currentMode_)) {
  //    pushedShowBubbles_ = showBubbles_;
  //  }
    return;
  }
  
  /***************************************************************************
  **
  ** Search modifiers
  */
  
  CmdSource.SearchModifiers getSearchModifiersX() {
    return (searchMod_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Things that are different per tab:
  */ 
    
  public static class AppSources {
 
    public CmdSource cSrc;
    public TabSource tSrc;
    public HarnessBuilder hBld;
    public PathAndFileSource pafs;
    public UIComponentSource uics;
    public UndoFactory uFac;
    public RememberSource rSrc;
    
    AppSources(BTState appState) {
      this.pafs = appState.getPathAndFileSource();
      this.rSrc = appState.getRememberSource();
      this.cSrc = appState.getCmdSource(); 
      this.tSrc = appState.getTabSource();
      this.uics = appState.getUIComponentSource();
      this.uFac = new UndoFactory(tSrc, cSrc, uics);
      this.hBld = new HarnessBuilder(appState, uics, cSrc, tSrc, rSrc, pafs, uFac);
    }
  }
  
  /***************************************************************************
  **
  ** Things that are different per tab:
  */ 
    
  public class PerTab {
 
    private SUPanel sup_; // Per Tab
    private GroupPanel gup_;

    private boolean groupNodeSelected_;
    
    private String myKey_;
    //
    // Controls that used to be implemented purely as Swing
    // now can operate in both Swing and Headless mode:
    //
    
    private VirtualModelTree vmTree_; // Per Tab
    private VirtualTimeSlider vtSlider_; // Per Tab
    private VirtualZoomControls vzc_; // Per Tab
   //   private VirtualPathControls vpc_;

    
    private GenomePresentation myGenomePre_; // Per Tab
    private UserTreePathController utpControl_; // Per Tab
    private NetOverlayController noc_; // Per Tab
    private ZoomTargetSupport zoomer_; // Per Tab?
    private ZoomCommandSupport zcs_; // Per Tab?
    private UserTreePathManager utpm_; // Per Tab
    private TextBoxManager textMgr_; // Per Tab
    
    private String layout_; // Per Tab
    private String genomeKey_; // Per Tab
    private String currentOverlay_; // Per Tab
    private NetModuleFree.CurrentSettings currOvrSettings_; // Per Tab
    private TaggedSet currentNetMods_; // Per Tab
    private TaggedSet revealed_; // Per Tab
    private boolean showingNetModuleComponents_; // Per Tab
   
    private PanelCommands pCmds_;
    private GroupPanelCommands gpCmds_;
    private PopCommands popCmds_;
    private CursorManager cursorMgr_;

    private GroupSettingManager gsm_; // Per Tab
    private PropagateDown.DownPropState downProp_; // remember user's last down propagation Genome // Per Tab?
    
    private TabPinnedDynamicDataAccessContext rcx_; // Needed to keep zoom target in sync with current genome. // Per Tab
    private PerTabOSO ptoso_;
 
    PerTab(String myKey) {
      myKey_ = myKey;
      layout_ = null;
      genomeKey_ = null;
      utpm_ = new UserTreePathManager();
      gsm_ = new GroupSettingManager();
      currentNetMods_ = new TaggedSet();
      revealed_ = new TaggedSet();
      showingNetModuleComponents_ = false;     
      downProp_ = new PropagateDown.DownPropState();
      groupNodeSelected_ = false;
      ptoso_ = new PerTabOSO(this);
    }
    
    private void initRcx(BTState parent) {
      rcx_ = new TabPinnedDynamicDataAccessContext(parent, myKey_);
    }
  }
  
  /****************************************************************************
  **
  ** The implementation of overlay state oracle
  */
  
  public class PerTabOSO implements OverlayStateWriter { 
                                                               
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private PerTab myTabInfo_;
     
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    public PerTabOSO(PerTab myTabInfo) {
      myTabInfo_ = myTabInfo;
    }
      
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
    
    /***************************************************************************
    **
    ** Get the current network overlay key
    */
    
    public String getCurrentOverlay() {
      return (myTabInfo_.currentOverlay_);
    } 
    
    /***************************************************************************
    **
    ** Set the current network module
    */
    
    public void setCurrentNetModules(TaggedSet keys, boolean forUndo) {
      myTabInfo_.currentNetMods_ = new TaggedSet(keys);
      String currKey = (myTabInfo_.currentNetMods_.set.size() == 1) ? (String)myTabInfo_.currentNetMods_.set.iterator().next() : null;
      myTabInfo_.textMgr_.setMessageSource(currKey, TextBoxManager.NETMOD_MESSAGE, forUndo);
      myTabInfo_.zoomer_.setCurrentNetModules(keys);
      return;
    }
    
    /***************************************************************************
    **
    ** Set the currently revealed modules
    */
    
    public void setRevealedModules(TaggedSet keys, boolean forUndo) {
      myTabInfo_.revealed_ = new TaggedSet(keys);
      return;
    }  
   
    /***************************************************************************
    **
    ** Get the current network module keys
    */
    
    public TaggedSet getCurrentNetModules() {
      return (new TaggedSet(myTabInfo_.currentNetMods_));
    } 
  
    /***************************************************************************
    **
    ** Get the current overlay settings
    */
    
    public NetModuleFree.CurrentSettings getCurrentOverlaySettings() {
      return (myTabInfo_.currOvrSettings_);
    } 
  
    /***************************************************************************
    **
    ** Get the set of modules that are showing their contents
    */  
    
    public TaggedSet getRevealedModules() {
      return (myTabInfo_.revealed_);
    }  
    
    /***************************************************************************
    **
    ** Answer if module components are being displayed
    */  
    
    public boolean showingModuleComponents() {
      return (myTabInfo_.showingNetModuleComponents_);
    } 
    
    /***************************************************************************
    **
    ** Set the current network overlay key
    */
    
    public void installCurrentSettings(NetModuleFree.CurrentSettings settings) { 
      myTabInfo_.currOvrSettings_ = settings;
      return;
    }
    
    /***************************************************************************
    **
    ** Set the current network overlay key
    */
    
    public SelectionChangeCmd.Bundle setCurrentOverlay(String key, boolean forUndo) { 
      
      //
      // Note that during set genome ops, by the time we get here we don't have the
      // old genome ID, and the layout is stale.  So it is involved to figure out if
      // we were showing links before....
      
      myTabInfo_.currentOverlay_ = key;
      
      Layout lo = myTabInfo_.rcx_.getLayoutSource().getLayout(myTabInfo_.layout_);     
      SelectionChangeCmd.Bundle bundle = null;
      if ((myTabInfo_.currentOverlay_ != null) && !forUndo) {
        NetOverlayProperties nop = lo.getNetOverlayProperties(myTabInfo_.currentOverlay_);
        if (nop.hideLinks()) {
          StaticDataAccessContext rcx2 = new StaticDataAccessContext(myTabInfo_.rcx_, myTabInfo_.genomeKey_, myTabInfo_.layout_);
          bundle = myTabInfo_.sup_.dropLinkSelections(new UIComponentSource(BTState.this), true, null, rcx2);
        }
      }
      myTabInfo_.textMgr_.setMessageSource(myTabInfo_.currentOverlay_, TextBoxManager.OVERLAY_MESSAGE, forUndo);
      myTabInfo_.zoomer_.setOverlay(key);
      return (bundle);
    }
  }
}
