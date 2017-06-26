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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.GroupPanelCommands;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TabNameData;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.TreeNodeChangeEvent;
import org.systemsbiology.biotapestry.event.TreeNodeChangeListener;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.OverlayDisplayChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.GroupPanel;
import org.systemsbiology.biotapestry.ui.ModelImagePanel;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.PerturbationsManagementWindow;
import org.systemsbiology.biotapestry.ui.menu.DesktopMenuFactory;
import org.systemsbiology.biotapestry.ui.menu.XPlatKeyBindings;
import org.systemsbiology.biotapestry.ui.menu.XPlatKeypressAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenuBar;
import org.systemsbiology.biotapestry.ui.menu.XPlatToolBar;
import org.systemsbiology.biotapestry.util.BTToggleButton;
import org.systemsbiology.biotapestry.util.BackgroundWorkerControlManager;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.JTabbedPaneWithPopup;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** The guts of the top-level BioTapestry window
*/

public class CommonView implements ModelChangeListener, TreeNodeChangeListener, BackgroundWorkerControlManager, ChangeListener { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private UIComponentSource uics_;
  private CmdSource cSrc_;
  private TabSource tSrc_;
  private HarnessBuilder hBld_;
  private UndoFactory uFac_;
  private PathAndFileSource pafs_;
  
  private HashMap<Integer, PerTab> pert_;
  private boolean ignoreTabs_;
  private JDialog pullFrame_;
  private JTabbedPane tabPanel_;

  private MainCommands.ChecksForEnabled undoAction_;
  private MainCommands.ChecksForEnabled redoAction_;
  private SyncToggleWidgets pulldownSync_;
  
  private Map<FlowMeister.FlowKey, MainCommands.ChecksForEnabled> actionMap_;
  private JToolBar toolBar_;
  private BTToggleButton tb_;
  private AbstractAction pullwi_;
  
  private PerturbationsManagementWindow pmw_;
  
  private ModelBuilder.Dashboard iw_;
  private ModelBuilder.LinkDrawingTracker wldt_;
  
  private boolean accept_;
  
  private DynamicDataAccessContext ddacx_;
  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CommonView(BTState appState, UIComponentSource uics, CmdSource cSrc, TabSource tSrc) {
    appState_ = appState;
    uics_ = uics;
    cSrc_ = cSrc;
    tSrc_ = tSrc;
    accept_ = true;
    ignoreTabs_ = false;
    pert_ = new HashMap<Integer, PerTab>();
    uFac_ = new UndoFactory(tSrc_, cSrc_, uics_);
    pafs_ = appState_.getPathAndFileSource();
    hBld_ = new HarnessBuilder(appState_, uics_, cSrc_, tSrc_, 
                               appState_.getRememberSource(), pafs_, uFac_);
    ddacx_ = new DynamicDataAccessContext(appState_);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Do setting of tab names and tips
  */   
  
  public TabChange setTabTitleData(int index, TabNameData tnd) {
	  TabChange tc = null;
	  if (!uics_.isHeadless()) {
		  PerTab curr = pert_.get(Integer.valueOf(index));
		  tc = new TabChange(true);
		  tc.oldPerTab = curr.clone();
		  curr.tabNameData = tnd.clone();
		  tc.newPerTab = curr.clone();
		  if (tabPanel_.getTabCount() > 0) {
		    tabPanel_.setTitleAt(index, tnd.getTitle());
		    tabPanel_.setToolTipTextAt(index, tnd.getDesc());
		  }
	  }
	  return (tc);
  }

  /***************************************************************************
  **
  ** Do adding of tab UI
  */ 

  public TabChange addTabUI(int index, int indexOldCurrent, TabNameData tnd) {
    if (uics_.isHeadless()) {
      PerTab newPerTab = new PerTab(index);
      pert_.put(Integer.valueOf(index), newPerTab);
      ignoreTabs_ = true;
      buildTab(); // may be null if headless...
      ignoreTabs_ = false;
      return (null);
    }
    PerTab cpt = null;
    if (tabPanel_.getTabCount() == 0) {
      cpt = pert_.get(pert_.keySet().iterator().next());
    }
    
    TabChange retval = new TabChange(true);
    retval.didTabChange = false;
    retval.newPerTab = new PerTab(index);
    // The PENDING stuff is for Tabs loaded from IO; that will be filled in when we get it:
    retval.newPerTab.tabNameData = (tnd == null) ? new TabNameData("PENDING", "PENDING", "PENDING") : tnd.clone();
    retval.newCurrIndexPre = indexOldCurrent;
    retval.newChangeIndex = index;
    pert_.put(Integer.valueOf(index), retval.newPerTab);
    
    ignoreTabs_ = true;
    retval.newTabUI = buildTab(); // may be null if headless...
    retval.newPerTab.tabContents = retval.newTabUI;
    if (tabPanel_.getTabCount() == 0) {
      uics_.getContentPane().remove(cpt.tabContents);
      uics_.getContentPane().add(tabPanel_, BorderLayout.CENTER);
      tabPanel_.addTab(cpt.tabNameData.getTitle(), null, cpt.tabContents, cpt.tabNameData.getDesc());
    }
    tabPanel_.addTab(retval.newPerTab.tabNameData.getTitle(), null, retval.newTabUI, retval.newPerTab.tabNameData.getDesc());
    uics_.getContentPane().revalidate();
   
    ignoreTabs_ = false;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Do removal of tab UI
  */ 

  public TabChange removeTabUI(int indexRem, int indexOldCurrent, int indexNewCurrent) {
    if (uics_.isHeadless()) {
      return (null);
    }

    TabChange retval = new TabChange(true);
    retval.didTabChange = false;
    retval.oldCurrIndexPre = indexOldCurrent;
    retval.oldPerTab = pert_.remove(Integer.valueOf(indexRem));

    // Fix the indices of the higher tabs:
    HashMap<Integer, PerTab> moddie = new HashMap<Integer, PerTab>();
    Iterator<Integer> kit = pert_.keySet().iterator();
    while (kit.hasNext()) {
      Integer testKey = kit.next();
      PerTab pt = pert_.get(testKey);
      if (pt.index > indexRem) {
        pt.index--;
      }
      moddie.put(Integer.valueOf(pt.index), pt);
    }
    pert_ = moddie;
       
    retval.oldChangeIndex = indexRem;
    ignoreTabs_ = true;
    // Note we do NOT use getTabComponentAt(), which returns null:
    retval.oldTabUI = (JPanel)tabPanel_.getComponentAt(indexRem);
    tabPanel_.remove(indexRem);
    retval.oldCurrIndexPost = indexNewCurrent;
    tabPanel_.setSelectedIndex(indexNewCurrent);
    if (tabPanel_.getTabCount() == 1) {
      PerTab cpt = pert_.get(pert_.keySet().iterator().next());
      tabPanel_.remove(0);
      uics_.getContentPane().remove(tabPanel_);
      uics_.getContentPane().add(cpt.tabContents, BorderLayout.CENTER);
    }
    uics_.getContentPane().revalidate();
    ignoreTabs_ = false;
    return (retval);
  }

  
  /***************************************************************************
  **
  ** Set current tab
  */ 

  public TabChange setVisibleTab(int index, int prevIndex) {
    if (uics_.isHeadless()) {
      return (null);
    }
    TabChange retval = new TabChange(true);
    retval.didTabChange = true;
    retval.changeTabPreIndex = prevIndex;
    retval.changeTabPostIndex = index;
    ignoreTabs_ = true;
    if (tabPanel_.getTabCount() > 0) { 
      tabPanel_.setSelectedIndex(index);
    }
    ignoreTabs_ = false;
    return (retval);
  }

  /***************************************************************************
  **
  ** Set current tab
  */ 

  public void setInternalVisibleTab(int oldIndex, int index) {
    if (uics_.isHeadless()) {
      return;
    }
    ignoreTabs_ = true;
    if (tabPanel_.getTabCount() > 0) { 
      tabPanel_.setSelectedIndex(index);
    } 
    ignoreTabs_ = false;
    return;
  }

  /***************************************************************************
  **
  ** Do window title
  */ 

  public void manageWindowTitle(String fileName) {
    if (uics_.isHeadless() || !uics_.getIsEditor()) {
      return;
    }
    if (uics_.getTopFrame() == null) {
      return;  // Don't mess with viewer title (huh??)
    }
    ResourceManager rMan = uics_.getRMan();
    String title;
    if (fileName == null) {
      title = rMan.getString("window.editorTitle");  
    } else {
      String titleFormat = rMan.getString("window.editorTitleWithName");
      title = MessageFormat.format(titleFormat, new Object[] {fileName});
    }
    uics_.getTopFrame().setTitle(title);
    return;
  }  
  
  /***************************************************************************
  **
  ** Set to accept keystrokes or not
  */
  
  public void acceptKeystrokes(boolean accept) {
    accept_ = accept;
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Answer if we accept keystrokes
  */

  public boolean canAccept() { 
    return (accept_);
  }
   
  /****************************************************************************
  **
  ** disable
  */  
  
  public void disableControls() {
    XPlatMaskingStatus xpms = calcDisableControls(MainCommands.GENERAL_PUSH, true);
    disableControls(xpms);
    return;
  }

  /****************************************************************************
  **
  ** enable
  */  
  
  public void reenableControls() {
    // if we don't have something selected, better fix that:
    if (uics_.getTree().getTreeSelectionPath() == null) {
      DataAccessContext dacx = new StaticDataAccessContext(appState_);
      uics_.getTree().setTreeSelectionPath(dacx.getGenomeSource().getModelHierarchy().getVfgSelection());
    }
    enableControls(true);
    return;
  }  
  
  /****************************************************************************
  **
  ** also redraw....
  */  
  
  public void redraw() {
    uics_.getSUPanel().drawModel(false);
    return;
  }

  /***************************************************************************
  **
  ** Get rid of displayed pull frame
  */
   
  public void clearPullFrame() {
    if (pullFrame_ != null) {
      // to break callback loop:
      JDialog holdFrame = pullFrame_;
      pullFrame_ = null;
      holdFrame.setVisible(false);
      holdFrame.dispose();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Tell us who our pull source is:
  */
  
  public void registerPullFrame(JDialog pullFrame) {
    pullFrame_ = pullFrame;
    return;
  } 
  
  /***************************************************************************
  **
  ** Tell us who our pull source is:
  */
  
  public JDialog getPullFrame() {
    return (pullFrame_);
  }
  
  /***************************************************************************
  **
  ** do key bindings
  */ 
  
  private void bindKeys() { 
    
    if (uics_.isHeadless()) {
      throw new IllegalStateException();
    }
    
    JComponent jcp = uics_.getContentPane();
    MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
    XPlatKeyBindings xpkb;
    try {
      xpkb = mSrc.getXPlatKeyBindings();
    } catch (MenuSource.InvalidMenuRequestException imrex) {
      throw new IllegalStateException();
    }
    Iterator<XPlatKeypressAction> xpit = xpkb.getKeypresses();
    while (xpit.hasNext()) {
      final XPlatKeypressAction xpka = xpit.next();
      if (xpka.hasStrokeTags()) {
        Set<String> st = xpka.getStrokeTags();
        Iterator<String> stit = st.iterator();
        while (stit.hasNext()) {
          String stktag = stit.next();
          jcp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(stktag), xpka.getActionMapTag());
        }
      } else {
        jcp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(xpka.getEvent(), xpka.getMask()), xpka.getActionMapTag()); 
      }
      jcp.getActionMap().put(xpka.getActionMapTag(), new AbstractAction() {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
          try {
            if (xpka.isBlockable() && !canAccept()) {
              return;
            }
            ControlFlow cf = cSrc_.getFloM().getControlFlow(xpka.getKey(), xpka.getActionArg());
            hBld_.buildAndRunHarness(cf, new DynamicDataAccessContext(appState_), uics_);
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Build the correct view
  */ 
  
  public void buildTheView() {
    if (uics_.getIsEditor()) {
      buildEditorView();
    } else {
      buildViewOnlyView();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Build editor UI components
  */ 
  
  private void buildEditorUI() {
    boolean doGaggle = uics_.getDoGaggle();
    
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    
    MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
    XPlatMenuBar xpmbar = mSrc.defineEditorMenuBar(dacx);
    
    DesktopMenuFactory dmFac = new DesktopMenuFactory(cSrc_, dacx.getRMan(), uics_, hBld_);
    MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
        
    bifo.collected.put("SELECTED", uics_.getSUPanel().getSelectedMenu());
    bifo.collected.put("CURRENT_MODEL", uics_.getTree().getCurrentModelMenu());
    bifo.collected.put("RECENT", uics_.getRecentMenu().getJMenu());
    bifo.collected.put("USER_PATH_CHOOSE", uics_.getPathControls().getJMenu());    
    if (doGaggle) {
      bifo.collected.put("GOOSE_CHOOSE", uics_.getGaggleControls().getJMenu());
    }   
    bifo.conditions.put("DO_GAGGLE", Boolean.valueOf(doGaggle));

    JMenuBar menuBar = dmFac.buildDesktopMenuBar(xpmbar, new DesktopMenuFactory.ActionSource(cSrc_), bifo);
    JMenu eMenu = bifo.collected.get("EMENU");
    undoAction_ = (MainCommands.ChecksForEnabled)bifo.taggedActions.get("UNDO");
    redoAction_ = (MainCommands.ChecksForEnabled)bifo.taggedActions.get("REDO");
    MainCommands.ChecksForEnabled pda = (MainCommands.ChecksForEnabled)bifo.taggedActions.get("PULLDOWN");
    JCheckBoxMenuItem jcb = (JCheckBoxMenuItem)bifo.components.get("PULLDOWN");
    
    eMenu.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent ev) {}
      public void menuDeselected(MenuEvent ev) {}
      public void menuSelected(MenuEvent ev) {
        try {
          UndoManager undom = cSrc_.getUndoManager();
          boolean canUndo = undom.canUndo();
          boolean canRedo = undom.canRedo();
          ResourceManager rMan = uics_.getRMan();
          String desc;
          if (canUndo) {
            desc = undom.getUndoPresentationName();
          } else {
            desc = rMan.getString("command.Undo");
          }
          undoAction_.putValue(AbstractAction.NAME, desc);
          undoAction_.setEnabled(undoAction_.isPushed() ? false : canUndo);
          if (canRedo) {
            desc = undom.getRedoPresentationName();
          } else {
            desc = rMan.getString("command.Redo");
          }
          redoAction_.putValue(AbstractAction.NAME, desc);
          redoAction_.setEnabled(redoAction_.isPushed() ? false : canRedo);
          uics_.getSUPanel().stockSelectedMenu(new StaticDataAccessContext(appState_));
          uics_.getTree().stockCurrentModelMenu();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;      
      }      
    });
    
    //
    // Build the tool bar, using actions with icons
    //   

    XPlatToolBar toob = mSrc.defineEditorToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();
        
    bifot.conditions.put("DO_GAGGLE", Boolean.valueOf(doGaggle));
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(true));
    bifot.conditions.put("SHOW_OVERLAY", Boolean.valueOf(true));
  
    actionMap_ = dmFac.stockActionMap(toob, bifot);

    toolBar_ = new JToolBar();
    stockEditorToolBar(false, false);   
    //
    // Keep the toggle mode widgets in sync with each other
    //
    
    pulldownSync_ = new SyncToggleWidgets(jcb, tb_, (MainCommands.ToggleAction)pda, (MainCommands.ToggleAction)pullwi_); 

    //
    // Wrap it up:
    //      
    uics_.getTopFrame().setJMenuBar(menuBar);
    uics_.getContentPane().add(toolBar_, BorderLayout.NORTH);
    return;
  }
  
  /***************************************************************************
  **
  ** Stock the tool bar
  */ 
  
  private void stockEditorToolBar(boolean showPathControls, boolean showOverlayControls) {
    
    if (uics_.isHeadless()) {
      throw new IllegalStateException();
    }
    
    boolean doGaggle = uics_.getDoGaggle();
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    
    MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
    DesktopMenuFactory dmf = new DesktopMenuFactory(cSrc_, dacx.getRMan(), uics_, hBld_);
    XPlatToolBar toob = mSrc.defineEditorToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();
    
    bifot.conditions.put("DO_GAGGLE", Boolean.valueOf(doGaggle));
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(showPathControls));
    bifot.conditions.put("SHOW_OVERLAY", Boolean.valueOf(showOverlayControls));  
    if (showPathControls) {
      bifot.components.put("PATH_COMBO", uics_.getPathControls().getJCombo());
    }
     
    uics_.getGaggleControls().stockToolBarPre(bifot);
    
    dmf.stockToolBar(toolBar_, toob, actionMap_, bifot);
    
    if (tb_ == null) {
      pullwi_ = bifot.taggedActions.get("PULLDOWN");
      tb_ = (BTToggleButton)bifot.components.get("PULLDOWN");
    }
    
    uics_.getGaggleControls().stockToolBarPost(bifot);
  
    return;
  }

  /***************************************************************************
  **
  ** Calculate how to disable the main controls
  */ 
  
  public XPlatMaskingStatus calcDisableControls(int pushFlags, boolean displayToo) {
    XPlatMaskingStatus ms = new XPlatMaskingStatus();
    cSrc_.getMainCmds().calcPushDisabled(pushFlags, ms); 
    ms.setRecentMenuOn(false); 
    ms.setPathControlsOn(false);
    // any push disables the overlay control, though visibility may still be allowed:    
    boolean allowOverlayViz = ((pushFlags & MainCommands.SKIP_OVERLAY_VIZ_PUSH) != 0x00);
    ms.setOverlayLevelOn(allowOverlayViz);
    ms.setMainOverlayControlsOn(false);
       
    if (displayToo) {
      int currTab = tSrc_.getCurrentTabIndex();
      if (pert_.get(currTab).myCard_ != null) {
        ms.setModelDisplayOn(false);
      }
      ms.setKeystrokesOn(false);
    } else {
      ms.setModelDisplayOn(true);
      ms.setKeystrokesOn(true);
    }
    ms.setModelTreeOn(false);
    ms.setSliderOn(false);
    VirtualGaggleControls vgc = uics_.getGaggleControls();
    if (vgc != null) {
      ms.setGeeseOn(false);
    }
    return (ms);
  }  
 
  /***************************************************************************
  **
  ** Show the group node panel or not
  */ 
  
  public void showGroupNode(boolean showIt) {
    int currTab = tSrc_.getCurrentTabIndex();
    pert_.get(currTab).showGroup = showIt;
    VirtualModelTree vmt = uics_.getTree();
    TreePath tp = vmt.getTreeSelectionPath();
    NavTree nt = ddacx_.getGenomeSource().getModelHierarchy();
    String groupNodeID = nt.getGroupNodeID(tp);
    installGroupNodeState(nt, groupNodeID, pert_.get(currTab).grp_);
    if (pert_.get(currTab).myCard_ != null) {
      String tag = (showIt) ? "GroupPanel" : "SUPanel";
      pert_.get(currTab).myCard_.show(pert_.get(currTab).hidingPanel_, tag);   
      uics_.getContentPane().validate();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Install group node stuff
  */ 
  
  public void installGroupNodeState(NavTree nt, String groupNodeID, GroupPanel grp) {
    String imgID = null;
    String mapID = null;
    Map<Color, NavTree.GroupNodeMapEntry> modMap = null;
    if (groupNodeID != null) {
      TreeNode tn = nt.nodeForNodeID(groupNodeID);
      imgID = nt.getImageKey(tn, false);
      mapID = nt.getImageKey(tn, true);
      modMap = nt.getGroupModelMap(tn);
    }
 
    grp.setDataContext(new StaticDataAccessContext(appState_).getContextForRoot());
    BufferedImage bi = (imgID != null) ? uics_.getImageMgr().getImage(imgID) : null;
    grp.setImage(bi);
    bi = (mapID != null) ? uics_.getImageMgr().getImage(mapID) : null;
    grp.setMap(bi, modMap);
    return;
  }
  
  /***************************************************************************
  **
  ** Disable the main controls based on masking information
  */ 
  
  public void disableControls(XPlatMaskingStatus ms) {
    if (!ms.isMaskingActive()) {
      return;
    }
    cSrc_.getMainCmds().pushDisabled(ms);     
    uics_.getRecentMenu().pushDisabled(ms.isRecentMenuOn().booleanValue());
    uics_.getPathControls().pushDisabled(ms.arePathControlsOn().booleanValue());
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    uics_.getNetOverlayController().pushDisabled(ms, dacx);
    
    int currTab = tSrc_.getCurrentTabIndex();
    if ((pert_.get(currTab).myCard_ != null) && !ms.isModelDisplayOn()) {
      pert_.get(currTab).myCard_.show(pert_.get(currTab).hidingPanel_, "Hiding");
    }
    
    //
    // Gotta disable all the other tab choices:
    // 
    int numTab = tSrc_.getNumTab();
    for (int i = 0; i < numTab; i++) {
      if (i != currTab) {
        tabPanel_.setEnabledAt(i, false);
      }
    }
    
    acceptKeystrokes(ms.areKeystrokesOn().booleanValue());

    uics_.getTree().setTreeEnabled(ms.isModelTreeOn().booleanValue());

    uics_.getVTSlider().setSliderEnabled(ms.isSliderOn().booleanValue());

    uics_.getPathControls().setPathsEnabled(ms.arePathControlsOn().booleanValue()); // Doesn't this duplicate part of the PathControl push above??
    VirtualGaggleControls vgc = uics_.getGaggleControls();
    if (vgc != null) {
      vgc.setGooseEnabled(ms.areGeeseOn().booleanValue());
    }
    
    if (!uics_.isHeadless()) {  // not headless...
      uics_.getContentPane().validate();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Reenable the controls
  */ 
  
  public void enableControls() {
    enableControls(false);
    return;
  }
  
  /***************************************************************************
  **
  ** Reenable the controls
  */ 
  
  public void enableControls(boolean withFocus) {
    cSrc_.getMainCmds().popDisabled();
    int currTab = tSrc_.getCurrentTabIndex();
    if (pert_.get(currTab).myCard_ != null) {
      String tag = (pert_.get(currTab).showGroup) ? "GroupPanel" : "SUPanel";      
      pert_.get(currTab).myCard_.show(pert_.get(currTab).hidingPanel_, tag);
    }
    
    //
    // Gotta re-enable all the other tab choices:
    //
    int numTab = tSrc_.getNumTab();
    for (int i = 0; i < numTab; i++) {
      if (i != currTab) {
        tabPanel_.setEnabledAt(i, true);
      }
    }

    acceptKeystrokes(true);
    uics_.getRecentMenu().popDisabled();
    uics_.getPathControls().popDisabled();
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    uics_.getNetOverlayController().popDisabled(dacx);
    uics_.getTree().setTreeEnabled(true);
    uics_.getVTSlider().setSliderEnabled(true);
    uics_.getPathControls().setPathsEnabled(true);
    VirtualGaggleControls vgc = uics_.getGaggleControls();
    if (vgc != null) {
      vgc.setGooseEnabled(true);
    }
    
    if (!uics_.isHeadless()) {  // not headless...
      uics_.getContentPane().validate();    
    }
    
    //
    // Following background thread operations, sometimes we need to
    // get keyboard focus back to the network panel:
    //
    // We make this conditional to keep it from being called in normal operation as 
    // the genome is changed, which causes the time slider to lose focus EVERY 
    // TIME IT IS MOVED!!!!!!!
    
    if (withFocus) {
      uics_.getSUPanel().requestFocusForModel();
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Cancel modal toggle controls
  */ 
  
  public void cancelModals(int which) {
    if (((which & PanelCommands.CANCEL_ADDS_SKIP_PULLDOWNS) == 0x00) && (pulldownSync_ != null)) {
      pulldownSync_.cancelled();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Programatically enable modal toggle controls
  */ 
  
  public void enableModals(int which) {
    if (((which & PanelCommands.ENABLE_PULLDOWNS) != 0x00) && (pulldownSync_ != null)) {
      pulldownSync_.activated();
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** init
  */ 
  
  private JPanel buildTab() {
   
    //
    // Even the "Headless" commonView has lots of virtual controls that stand in for the Swing versions:
    //

    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    
    new SUPanel(appState_, uics_, cSrc_, tSrc_, hBld_, ddacx);
    appState_.setGroupPanel(new GroupPanel(uics_, cSrc_));
    cSrc_.setGroupPanelCmds(new GroupPanelCommands(hBld_));

    VirtualZoomControls vzc = new VirtualZoomControls(uics_, ddacx_, cSrc_);
    uics_.setVirtualZoom(vzc);
    uics_.setZoomCommandSupport(new ZoomCommandSupport(vzc, uics_, tSrc_, -1));
 
    uics_.setPathController(new UserTreePathController(uics_));

    uics_.setNetOverlayController(new NetOverlayController(ddacx, uics_, cSrc_, uFac_, hBld_)); 
    uics_.getNetOverlayController().resetControllerState(ddacx);
    NavTree navTree = ddacx_.getGenomeSource().getModelHierarchy();   
    JTree jTree = (uics_.isHeadless()) ? null : new JTree(navTree);  
    uics_.setVmTree(new VirtualModelTree(jTree, navTree, cSrc_, uics_, ddacx, hBld_, tSrc_, uFac_));
 
    if (!uics_.isHeadless()) {
      buildUICore1();
    }

    uics_.setVTSlider(new VirtualTimeSlider(ddacx, uics_, hBld_, tSrc_, uFac_));
    
    // Kinda bogus: chicken and egg!
    int currTab = tSrc_.getCurrentTabIndex();
    uics_.getZoomCommandSupport().registerScrollPaneAndZoomTarget(pert_.get(currTab).jsp_, uics_.getZoomTarget());
    uics_.getZoomCommandSupport().setTabNum(currTab);
    
    //
    // Build the nav tree panel stuff:
    //
    
    uics_.getTree().setupTree();
    pert_.get(currTab).mip_ = new ModelImagePanel(uics_); 
    pert_.get(currTab).mip_.setImage(null);
    pert_.get(currTab).mipVisible_ = false;   
    pert_.get(currTab).ovrVisible_ = false;

    pert_.get(currTab).grp_ = uics_.getGroupPanel();

    if (!uics_.isHeadless()) {
      pert_.get(currTab).hidingPanel_.add(pert_.get(currTab).grp_.getPanel(), "GroupPanel");
      return (buildUICore2(jTree));
    }
   
    return (null);
  }

  /***************************************************************************
  **
  ** Stock the viewer tool bar
  */ 
  
  private void stockViewerToolBar(boolean showPathControls) {
    
    if (uics_.isHeadless()) {
      throw new IllegalStateException();
    }
       
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    
    MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
    DesktopMenuFactory dmf = new DesktopMenuFactory(cSrc_, dacx.getRMan(), uics_, hBld_);
    XPlatToolBar toob = mSrc.defineViewerToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();
 
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(showPathControls));
 
    if (showPathControls) {
      bifot.components.put("PATH_COMBO", uics_.getPathControls().getJCombo());
    }
      
    dmf.stockToolBar(toolBar_, toob, actionMap_, bifot);   
    return;
  }

  /***************************************************************************
  **
  ** Build an editor view
  */ 
  
  private void buildEditorView() {
    if (!uics_.isHeadless()) {
      bindKeys();
      JComponent cp = uics_.getContentPane();
      cp.setLayout(new BorderLayout());
      tabPanel_ = new JTabbedPaneWithPopup(uics_);   
      tabPanel_.addChangeListener(this);   
    }
    
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);

  
    MainCommands mc = new MainCommands(uics_, ddacx, cSrc_, hBld_); 
    cSrc_.setMainCmds(mc);  
    appState_.setLSSupport(new LoadSaveSupport(uics_, ddacx, tSrc_, uFac_, cSrc_, pafs_, mc.getClass()));
  
    appState_.setRecentMenu(new VirtualRecentMenu(uics_, cSrc_, pafs_, ddacx.getRMan()));    
    appState_.setGaggleControls(new VirtualGaggleControls(uics_, cSrc_, ddacx.getRMan()));
    
  
    List<JTabbedPaneWithPopup.InformedAction> aacts = new ArrayList<JTabbedPaneWithPopup.InformedAction>();
    aacts.add(new MainCommands.TabPopupAction(ddacx, cSrc_.getMainCmds().getCachedFlow(FlowMeister.MainFlow.DROP_THIS_TAB), uics_, hBld_));
    aacts.add(new MainCommands.TabPopupAction(ddacx, cSrc_.getMainCmds().getCachedFlow(FlowMeister.MainFlow.DROP_ALL_BUT_THIS_TAB), uics_, hBld_));
    aacts.add(new MainCommands.TabPopupAction(ddacx, cSrc_.getMainCmds().getCachedFlow(FlowMeister.MainFlow.RETITLE_TAB), uics_, hBld_));
    if (!uics_.isHeadless()) {
      ((JTabbedPaneWithPopup)tabPanel_).setActions(aacts);
    }
    
    PerTab npt = new PerTab(0);
    pert_.put(Integer.valueOf(0), npt); // Gotta be before we build the tab...
    npt.tabNameData = ddacx.getGenomeSource().getTabNameData();
    npt.tabContents = buildTab();
     
    VirtualPathControls vpc = new VirtualPathControls(uics_, ddacx, uFac_, cSrc_);
    appState_.setPathControls(vpc);
    
    
    ignoreTabs_ = true;
    if (!uics_.isHeadless()) {
      uics_.getContentPane().add(npt.tabContents, BorderLayout.CENTER);
    }
    
    ignoreTabs_ = false;

    if (!uics_.isHeadless()) {
      buildEditorUI();
    }
    
    EventManager mgr = uics_.getEventMgr();
    mgr.addModelChangeListener(this);
    mgr.addTreeNodeChangeListener(this);

    SelectionChangeEvent ev = new SelectionChangeEvent(null, null, SelectionChangeEvent.SELECTED_MODEL);
    mgr.sendSelectionChangeEvent(ev);
    
    NavTree navTree = ddacx.getGenomeSource().getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();
    navTree.setSkipFlag(NavTree.Skips.SKIP_EVENT);
    uics_.getTree().setTreeSelectionPath(dstp);
    navTree.setSkipFlag(NavTree.Skips.NO_FLAG);
    uics_.getZoomTarget().fixCenterPoint(true, null, false);
    return;
  }
   
  /***************************************************************************
  **
  ** Build a viewer view for an application
  */ 
  
  private JPanel buildTextPane() {
  
    JTextPane jtp = uics_.getTextBoxMgr().getTextPane();
    if (uics_.doBig()) {
      Font tpFont = jtp.getFont();
      String fontName = tpFont.getName();
      int fontType = tpFont.getStyle();
      jtp.setFont(new Font(fontName, fontType, 20));
    }
    jtp.setMinimumSize(new Dimension(640,(uics_.doBig()) ? 75 : 50));    
    jtp.setPreferredSize(new Dimension(640, (uics_.doBig()) ? 75 : 50));
    jtp.setEditable(false);
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new GridLayout());

    JScrollPane txtsp = new JScrollPane(jtp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    textPanel.add(txtsp);
    return (textPanel);
  } 
  
  /***************************************************************************
  **
  ** Build a viewer view for an application
  */ 
  
  private void buildViewOnlyView() {
    if (!uics_.isHeadless()) {
      JComponent cp = uics_.getContentPane();
      cp.setLayout(new BorderLayout());
      tabPanel_ = new JTabbedPane();
      tabPanel_.addChangeListener(this);
    }

    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);

    MainCommands mc = new MainCommands(uics_, ddacx, cSrc_, hBld_); 
    cSrc_.setMainCmds(mc);  
    appState_.setLSSupport(new LoadSaveSupport(uics_, ddacx, tSrc_, uFac_, cSrc_, pafs_, mc.getClass()));  
    appState_.setRecentMenu(new VirtualRecentMenu(uics_, cSrc_, pafs_, ddacx.getRMan()));
       
    PerTab npt = new PerTab(0);  
    pert_.put(Integer.valueOf(0), npt);
    npt.tabNameData = ddacx.getGenomeSource().getTabNameData();
    npt.tabContents = buildTab();
       
    VirtualPathControls vpc = new VirtualPathControls(uics_, ddacx, uFac_, cSrc_);
    appState_.setPathControls(vpc);
     
    ignoreTabs_ = true;
    if (!uics_.isHeadless()) {
      uics_.getContentPane().add(npt.tabContents, BorderLayout.CENTER);
    }
    ignoreTabs_ = false;
  
    //
    // Build the tool bar, using actions with icons
    //

    if (!uics_.isHeadless()) {
      buildViewOnlyUI();
    }
    
    EventManager mgr = uics_.getEventMgr();
    mgr.addModelChangeListener(this);  
    SelectionChangeEvent ev = new SelectionChangeEvent(null, null, SelectionChangeEvent.SELECTED_MODEL);
    mgr.sendSelectionChangeEvent(ev);
    
    return;
  }  

  /***************************************************************************
  **
  ** Build a viewer view for an application
  */ 
  
  private void buildViewOnlyUI() {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
    XPlatToolBar toob = mSrc.defineViewerToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();       
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(true));
    
    DesktopMenuFactory dmf = new DesktopMenuFactory(cSrc_, dacx.getRMan(), uics_,  hBld_); 
    actionMap_ = dmf.stockActionMap(toob, bifot);
    
    toolBar_ = new JToolBar();
    stockViewerToolBar(false);
    toolBar_.setFloatable(false);
    uics_.getContentPane().add(toolBar_, BorderLayout.NORTH);
    return;
  }

  /***************************************************************************
  **
  ** init
  */ 
  
  private void buildUICore1() { 
    int currTab = tSrc_.getCurrentTabIndex();
    pert_.get(currTab).jsp_ = uics_.getSUPanel().getPanelInPane();
    pert_.get(currTab).showGroup = false;
    pert_.get(currTab).hidingPanel_ = new JPanel();
    pert_.get(currTab).myCard_ = new CardLayout();
    pert_.get(currTab).hidingPanel_.setLayout(pert_.get(currTab).myCard_);
    pert_.get(currTab).hidingPanel_.add(pert_.get(currTab).jsp_, "SUPanel");
    JPanel blankPanel = new JPanel();
    blankPanel.setBackground(Color.white);
    pert_.get(currTab).hidingPanel_.add(blankPanel, "Hiding");
    pert_.get(currTab).jsp_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    pert_.get(currTab).jsp_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    return;
  }
 

  
  /***************************************************************************
  **
  ** init
  */ 
  
  private JPanel buildUICore2(JTree jtree) {

    JScrollPane tjsp = new JScrollPane(jtree);
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    UiUtil.gbcSet(gbc, 0, 0, 1, 2, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.N, 1.0, 1.0);
    controlPanel.add(tjsp, gbc);
    int currTab = tSrc_.getCurrentTabIndex();
    
    JPanel mipPan = pert_.get(currTab).mip_.getPanel();
    mipPan.setBorder(new LineBorder(Color.black, 1));
    mipPan.setMinimumSize(new Dimension(250,0));
    mipPan.setPreferredSize(new Dimension(250,0)); 
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    pert_.get(currTab).ovrPanel_ = uics_.getNetOverlayController().getNetOverlayNavigator(ddacx);
    pert_.get(currTab).ovrPanel_.setBorder(new LineBorder(Color.black, 1));      
    pert_.get(currTab).ovrPanel_.setMinimumSize(new Dimension(250,0));
    pert_.get(currTab).ovrPanel_.setPreferredSize(new Dimension(250,0)); 
     
    pert_.get(currTab).sliderPanel_ = uics_.getVTSlider().getSliderPanel();
          
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(pert_.get(currTab).sliderPanel_, gbc);
    
    UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(pert_.get(currTab).ovrPanel_, gbc);
    
    UiUtil.gbcSet(gbc, 0, 4, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(mipPan, gbc);  

    pert_.get(currTab).jsp_.setMinimumSize(new Dimension(150, 150));
    tjsp.setMinimumSize(new Dimension(250, 250));    
    jtree.setMinimumSize(new Dimension(250,250));
    controlPanel.setMinimumSize(new Dimension(250, 250));    
    //myTree_.setPreferredSize(new Dimension(250, 250));  NO! Scroll bars don't work!
    //JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lsp, jsp);
    JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, pert_.get(currTab).hidingPanel_);
    //  topPanel.setLayout(new GridLayout(1,1));
    
    JPanel returnPanel = new JPanel();
    returnPanel.setLayout(new BorderLayout());
    returnPanel.add(jsp, BorderLayout.CENTER);
    returnPanel.add(buildTextPane(), BorderLayout.SOUTH);
    return (returnPanel);
  }

  /***************************************************************************
  **
  ** Handle display update
  */
  
  public void updateDisplayForGenome(Genome currGenome) {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    String mi = currGenome.getGenomeImage();
    BufferedImage bi = uics_.getImageMgr().getImage(mi);
    makeImageVisible(dacx.getFGHO().modelImageExists());
    int currTab = tSrc_.getCurrentTabIndex();
    pert_.get(currTab).mip_.setImage(bi);
    makeOverlayControlsVisible(uics_.getNetOverlayController().aModelHasOverlays(dacx));
    makeSliderVisible(dacx.getFGHO().hourlyDynamicModelExists());    
    return;
  }
  
  /***************************************************************************
  **
  ** Set visibility after a new model
  */
  
  public void newModelVisibility() { 
    makeImageVisible(false);
    makeOverlayControlsVisible(false);
    makeSliderVisible(false);
    makePathControlsVisible(false);
    return;
  }
  
  /***************************************************************************
  **
  ** Set visibility after a load
  */
  
  public void postLoadVisibility() {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    makeImageVisible(dacx.getFGHO().modelImageExists());
    makePathControlsVisible(uics_.getPathMgr().getPathCount() > 0);
    makeOverlayControlsVisible(uics_.getNetOverlayController().aModelHasOverlays(dacx));
    makeSliderVisible(dacx.getFGHO().hourlyDynamicModelExists());
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set the visibility of the image panel
  */
  
  private void makeImageVisible(boolean show) {
    int currTab = tSrc_.getCurrentTabIndex();
    if (pert_.get(currTab).mipVisible_ == show) {
      return;
    }
    boolean isHeadless = uics_.isHeadless();
    if (!isHeadless) {
      int height = (show) ? 200 : 0;
      JPanel mipPan = pert_.get(currTab).mip_.getPanel();
      mipPan.setMinimumSize(new Dimension(250, height));
      mipPan.setPreferredSize(new Dimension(250, height));
      mipPan.invalidate();
      uics_.getContentPane().validate();
    }
    pert_.get(currTab).mipVisible_ = show;
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set the visibility of the overlay controls
  */
  
  private void makeOverlayControlsVisible(boolean show) {
    int currTab = tSrc_.getCurrentTabIndex();
    if (!uics_.isHeadless()) {
      JFrame topFrame = uics_.getTopFrame();
      if (pert_.get(currTab).ovrVisible_ == show) {
        if ((toolBar_ != null) && (topFrame != null)) {
          toolBar_.invalidate();
          uics_.getContentPane().validate();
          toolBar_.repaint();
        }
        return;
      }
      
      if (uics_.getIsEditor()) {
        stockEditorToolBar(uics_.getPathControls().areControlsVisible(), show);
      } 
      
      int height = (show) ? 250 : 0;
      pert_.get(currTab).ovrPanel_.setMinimumSize(new Dimension(250, height));
      pert_.get(currTab).ovrPanel_.setPreferredSize(new Dimension(250, height));
      pert_.get(currTab).ovrPanel_.invalidate();
      if (toolBar_ != null) toolBar_.invalidate();
      if (topFrame != null) uics_.getContentPane().validate();
      if (toolBar_ != null) toolBar_.repaint();
    }
    pert_.get(currTab).ovrVisible_ = show;
    return;
  }        
  
  /***************************************************************************
  **
  ** Used to set the visibility of the slider controls
  */
  
  private void makeSliderVisible(boolean show) {
    uics_.getVTSlider().makeSliderVisible(show);
    return;
  }    

  /***************************************************************************
  **
  ** Used to set the visibility of the path controls
  */
  
  public void makePathControlsVisible(boolean show) {
    VirtualPathControls vpc = uics_.getPathControls();
    if ((vpc == null) && (toolBar_ == null)) {
      return; // Startup!
    } 
    if (!uics_.isHeadless()) {
      JFrame topFrame = uics_.getTopFrame();  
      // Null at startup...
      if ((vpc != null) && (vpc.areControlsVisible() == show)) {
        if ((toolBar_ != null) && (topFrame != null)) {
          toolBar_.invalidate();
          uics_.getContentPane().validate();
          toolBar_.repaint();
        }
        return;
      }
      
      if (toolBar_ != null) {
        if (uics_.getIsEditor()) {
          int currTab = tSrc_.getCurrentTabIndex();
          stockEditorToolBar(show, pert_.get(currTab).ovrVisible_);
        } else {
          stockViewerToolBar(show);
        }
        if (topFrame != null) {
          toolBar_.invalidate();
          uics_.getContentPane().validate();
          toolBar_.repaint();
        }
      }
    }
    vpc.setControlsVisible(show);
    return;
  }  

  /***************************************************************************
  **
  ** Notify listener of Node Tree change
  */ 
  
  public void treeNodeHasChanged(TreeNodeChangeEvent tncev) {
  
    if (tncev.getChangeType() == TreeNodeChangeEvent.Change.NO_CHANGE) {
      return;
    } else if (tncev.getChangeType() == TreeNodeChangeEvent.Change.GROUP_NODE_CHANGE) {
      int currTab = tSrc_.getCurrentTabIndex();
      NavTree nt = ddacx_.getGenomeSource().getModelHierarchy();
      VirtualModelTree vmt = uics_.getTree();
      TreePath tp = vmt.getTreeSelectionPath();
      String groupNodeID = nt.getGroupNodeID(tp);
      if (groupNodeID == null) {
        return;
      } else if (groupNodeID.equals(tncev.getNodeID().nodeID)) {
        installGroupNodeState(nt, groupNodeID, pert_.get(currTab).grp_);
      }
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event, int remaining) {
    modelHasChanged(event);
    return;
  }      
 
  /***************************************************************************
  **
  ** Notify listener of model change
  */ 
  
  public void modelHasChanged(ModelChangeEvent mcev) {
    StaticDataAccessContext dacx = new StaticDataAccessContext(appState_);
    if (mcev.getChangeType() == ModelChangeEvent.MODEL_DROPPED) {
      uics_.getVTSlider().dropTheSlider();
      makeImageVisible(dacx.getFGHO().modelImageExists());
      makeOverlayControlsVisible(uics_.getNetOverlayController().aModelHasOverlays(dacx));
      makeSliderVisible(dacx.getFGHO().hourlyDynamicModelExists());
      int currTab = tSrc_.getCurrentTabIndex();
      pert_.get(currTab).mip_.setImage(null);
      return;
    }
    if (mcev.getChangeType() == ModelChangeEvent.DYNAMIC_MODEL_ADDED) {
      makeSliderVisible(dacx.getFGHO().hourlyDynamicModelExists());
    }    
    makeOverlayControlsVisible(uics_.getNetOverlayController().aModelHasOverlays(dacx));
    if (mcev.getChangeType() == ModelChangeEvent.PROPERTY_CHANGE) {     
      if (!uics_.getTree().isCurrentlyDisplayed(mcev, dacx)) {
        uics_.getVTSlider().ditchSliderIfStale(mcev);
      } else {  // Displayed
        if (uics_.getTree().isDynamic(dacx)) {   
          String id = uics_.getVTSlider().handleSliderModelChange(mcev);
          //
          // We are handling an event notification. This means we are in the middle of another control flow execution, and 
          // we have no business launching another control flow.  So just use the guts of the switching operation from the
          // SetCurrentModel.StepState flow state:
          //
          SetCurrentModel.StepState agis = new SetCurrentModel.StepState(SetCurrentModel.SettingAction.FOR_EVENT, dacx);
          agis.setAppState(appState_, new DynamicDataAccessContext(appState_));
          agis.coreSwitchingSupport(id, null);
        } else {
          uics_.getTextBoxMgr().checkForChanges(mcev);
          String id = mcev.getGenomeKey();
          Genome currGenome = dacx.getGenomeSource().getGenome(id);
          String mi = currGenome.getGenomeImage();
          BufferedImage bi = uics_.getImageMgr().getImage(mi);
          makeImageVisible(uics_.getImageMgr().hasAnImage());
          int currTab = tSrc_.getCurrentTabIndex();
          pert_.get(currTab).mip_.setImage(bi);
          uics_.getSUPanel().drawModel(true);
        }
      }
      // NavTree tracks name changes through THIS one call:
      uics_.getTree().updateNavTreeNames(mcev, dacx);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Undo a tab edit change
  */  
  
  public void editTabUndo(PerTab undo) {
		setTabTitleData(undo.index, undo.tabNameData);
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a tab edit change
  */
  
  public void editTabRedo(PerTab redo) {
    setTabTitleData(redo.index, redo.tabNameData);
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a tab change
  */
  
  public void changeTabUndo(TabChange undo) {
    // We only handle UI undo operations
    if (!undo.forUI) {
      return;
    }
    // If headless, we are not involved in tab management
    if (uics_.isHeadless()) {
      return;
    }
    ignoreTabs_ = true;  
    
    // First case: switch of visible tab:
    if (undo.didTabChange) {
      tabPanel_.setSelectedIndex(undo.changeTabPreIndex);
    // Next case: removal of tab:
    } else if (undo.oldPerTab != null) {
      if (tabPanel_.getTabCount() == 0) {
        PerTab cpt = pert_.get(pert_.keySet().iterator().next());
        uics_.getContentPane().remove(cpt.tabContents);
        uics_.getContentPane().add(tabPanel_, BorderLayout.CENTER);
        tabPanel_.addTab(cpt.tabNameData.getTitle(), null, cpt.tabContents, cpt.tabNameData.getDesc());
      }
      tabPanel_.insertTab(undo.oldPerTab.tabNameData.getTitle(), null, undo.oldTabUI, 
                          undo.oldPerTab.tabNameData.getDesc(), undo.oldChangeIndex);        
      // Fix the indices of the higher tabs:
      HashMap<Integer, PerTab> moddie = new HashMap<Integer, PerTab>();
      Iterator<Integer> kit = pert_.keySet().iterator();
      while (kit.hasNext()) {
        Integer testKey = kit.next();
        PerTab pt = pert_.get(testKey);
        if (pt.index >= undo.oldChangeIndex) {
          pt.index++;
        }
        moddie.put(Integer.valueOf(pt.index), pt);
      }      
      pert_ = moddie;
      // All fixed; insert the deleted info back, reset to old current tab.
      pert_.put(Integer.valueOf(undo.oldChangeIndex), undo.oldPerTab);
      tabPanel_.setSelectedIndex(undo.oldCurrIndexPre);
      uics_.getContentPane().revalidate();
    // Next case: addition of tab:
    } else if (undo.newPerTab != null) {
      pert_.remove(Integer.valueOf(undo.newChangeIndex));     
      tabPanel_.remove(undo.newChangeIndex);
      tabPanel_.setSelectedIndex(undo.newCurrIndexPre);
      if (tabPanel_.getTabCount() == 1) {
        PerTab cpt = pert_.get(pert_.keySet().iterator().next());
        tabPanel_.remove(0);
        uics_.getContentPane().remove(tabPanel_);
        uics_.getContentPane().add(cpt.tabContents, BorderLayout.CENTER);
      }  
      uics_.getContentPane().revalidate();
    }
    ignoreTabs_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a tab change
  */
  
  public void changeTabRedo(TabChange redo) {
    // We only handle UI undo operations
    if (!redo.forUI) {
      return;
    }
    // If headless, we are not involved in tab management
    if (uics_.isHeadless()) {
      return;
    }
    ignoreTabs_ = true;  
    
    // First case: switch of visible tab:
    if (redo.didTabChange) {
      tabPanel_.setSelectedIndex(redo.changeTabPostIndex);
    // Next case: removal of tab, so we need to re-remove  
    } else if (redo.oldPerTab != null) {
      pert_.remove(Integer.valueOf(redo.oldChangeIndex));
      // Fix the indices of the higher tabs:
      HashMap<Integer, PerTab> moddie = new HashMap<Integer, PerTab>();
      Iterator<Integer> kit = pert_.keySet().iterator();
      while (kit.hasNext()) {
        Integer testKey = kit.next();
        PerTab pt = pert_.get(testKey);
        if (pt.index > redo.oldChangeIndex) {
          pt.index--;
        }
        moddie.put(Integer.valueOf(pt.index), pt);
      }
      pert_ = moddie;
      // Note we do NOT use getTabComponentAt(), which returns null:
      tabPanel_.remove(redo.oldChangeIndex);      
      tabPanel_.setSelectedIndex(redo.oldCurrIndexPost);
      if (tabPanel_.getTabCount() == 1) {
        PerTab cpt = pert_.get(pert_.keySet().iterator().next());
        tabPanel_.remove(0);
        uics_.getContentPane().remove(tabPanel_);
        uics_.getContentPane().add(cpt.tabContents, BorderLayout.CENTER);
      }
      uics_.getContentPane().revalidate();
    // Next case: addition of tab, so we need to re-add  
    } else if (redo.newPerTab != null) {
      pert_.put(Integer.valueOf(redo.newChangeIndex), redo.newPerTab);
      if (tabPanel_.getTabCount() == 0) {
        PerTab cpt = pert_.get(pert_.keySet().iterator().next());
        uics_.getContentPane().remove(cpt.tabContents);
        uics_.getContentPane().add(tabPanel_, BorderLayout.CENTER);
        tabPanel_.addTab(cpt.tabNameData.getTitle(), null, cpt.tabContents, cpt.tabNameData.getDesc());
      }
      tabPanel_.addTab(redo.newPerTab.tabNameData.getTitle(), null, redo.newTabUI, redo.newPerTab.tabNameData.getDesc());
      uics_.getContentPane().revalidate();
    }
    ignoreTabs_ = false;
    return;
  }

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NavigationChange undo, StaticDataAccessContext dacx) {
    if (undo.userPathSelection || undo.userPathSync) {
      uics_.getPathController().changeUndo(undo);
    }
    
    if (undo.odc != null) {
      OverlayDisplayChange odc = undo.odc;
      NetOverlayController noc = uics_.getNetOverlayController();
      if ((odc.oldOvrKey != null) || (odc.newOvrKey != null)) {
        boolean differentModel;
        if (odc.oldGenomeID == null) {
          differentModel = (odc.newGenomeID != null);
        } else {
          differentModel = !odc.oldGenomeID.equals(odc.newGenomeID);
        }
        if (differentModel) {
          noc.preloadForNextGenome(odc.oldGenomeID, odc.oldOvrKey, odc.oldModKeys, odc.oldRevealedKeys);     
        } else {
          noc.setFullOverlayState(odc.oldOvrKey, odc.oldModKeys, odc.oldRevealedKeys, null, dacx);
        }
      }
    }
      
    if (undo.oldPath != null) {
      NavTree navTree = ddacx_.getGenomeSource().getModelHierarchy();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)navTree.getRoot();
      uics_.getTree().setTreeSelectionPathForUndo(navTree.mapPath(undo.oldPath, root), dacx);
    } else if (undo.sliderID != null) {
      uics_.getVTSlider().handleSliderUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NavigationChange undo, StaticDataAccessContext dacx) {
    if (undo.userPathSelection || undo.userPathSync) {
      uics_.getPathController().changeRedo(undo);
    }
    
    if (undo.odc != null) {
      OverlayDisplayChange odc = undo.odc;
      NetOverlayController noc = uics_.getNetOverlayController();
      if ((odc.oldOvrKey != null) || (odc.newOvrKey != null)) {
        boolean differentModel;
        if (odc.newGenomeID == null) {
          differentModel = (odc.oldGenomeID != null);
        } else {
          differentModel = !odc.newGenomeID.equals(odc.oldGenomeID);
        }
        if (differentModel) {
          noc.preloadForNextGenome(odc.newGenomeID, odc.newOvrKey, odc.newModKeys, odc.newRevealedKeys);     
        } else {
          noc.setFullOverlayState(odc.newOvrKey, odc.newModKeys, odc.newRevealedKeys, null, dacx);
        }
      }
    }    
    
    if (undo.newPath != null) {
      NavTree navTree = ddacx_.getGenomeSource().getModelHierarchy();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)navTree.getRoot();
      uics_.getTree().setTreeSelectionPathForUndo(navTree.mapPath(undo.newPath, root), dacx);
    } else if (undo.sliderID != null) {
      uics_.getVTSlider().handleSliderRedo(undo);
    }
    return;
  }
 

  /***************************************************************************
  **
  ** UI inputs or programmatic changes of current tab trigger this. Ignore it if
  ** we are currently messing around, else we use this to trigger the control flow 
  ** to switch the current tab. 
  */ 
  
  public void stateChanged(ChangeEvent e) {
    if (ignoreTabs_) {
      return;
    }
    TabChange tc = new TabChange(true);
    tc.didTabChange = true;
    tc.changeTabPreIndex = tSrc_.getCurrentTabIndex();
    tc.changeTabPostIndex = tabPanel_.getSelectedIndex();
    tabChange(tabPanel_.getSelectedIndex(), true, tc);
    return;
  }
  
  /***************************************************************************
  **
  ** Record tab changes
  */ 
  
  private void tabChange(int newTab, boolean viaUI, TabChange tc) {
    if (ignoreTabs_) {
      return;
    }
    if (viaUI) {
      ControlFlow myFlow = cSrc_.getMainCmds().getCachedFlow(FlowMeister.MainFlow.CHANGE_TAB);
      HarnessBuilder.PreHarness pH = hBld_.buildHarness(myFlow);
      DialogAndInProcessCmd.TabPopupCmdState agis = (DialogAndInProcessCmd.TabPopupCmdState)pH.getCmdState();
      agis.setTab(newTab, viaUI, tc);
      hBld_.runHarness(pH);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Do init steps for embedded panel
  */  
  
  public void embeddedPanelInit() {

    StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_);
    dacx.drop();
    dacx.getWorkspaceSource().setWorkspaceNeedsCenter();
    dacx.getExpDataSrc().installLegacyTimeAxisDefinition(dacx);
    dacx.getGSM().drop();
    uics_.getLSSupport().newModelOperations(dacx);
    uics_.getSUPanel().drawModel(true);
    return;  
  }
 
    /***************************************************************************
  **
  ** Answer if we have edits in progress
  */
  
   public boolean havePerturbationEditsInProgress() {
     if (pmw_ == null) {
       return (false);
     }
     return (pmw_.havePendingEdit());
   }
            
  /***************************************************************************
  **
  ** Drop all edits in progress
  */
  
   public void dropAllPendingPerturbationEdits() {
     if (pmw_ == null) {
       throw new IllegalStateException();
     }
     pmw_.dropAllPendingEdits();
   }
 
  /***************************************************************************
  **
  ** Launch the Perturbations Window
  */
  
   public void launchPerturbationsManagementWindow(PertFilterExpression pfe, DataAccessContext dacx, UIComponentSource uics, UndoFactory uFac) {
     if (pmw_ != null) {   
       pmw_.setExtendedState(JFrame.NORMAL);
       pmw_.toFront();
       pmw_.jumpWithNewFilter(pfe);
       return;
     }
              
     if (!uics_.getLSSupport().prepTimeAxisForDataImport(dacx)) {
       return;
     }    
     PerturbationData pd = dacx.getExpDataSrc().getPertData();  
     pmw_ = new PerturbationsManagementWindow(uics, dacx, pd, pfe, uFac);
     pmw_.setVisible(true);
     return;
  }
 
  /***************************************************************************
  **
  ** Clear the Perturbations Window
  */ 
  
  public void clearPerturbationsManagementWindow() {
    pmw_ = null;
    return;
  }
 
  /***************************************************************************
  **
  ** Tell the Perturbations Window we have a change
  */ 
  
  public void perturbationsManagementWindowHasChanged() {
    if (pmw_ != null) {
      // So perturbation data presentation keeps track of undo/redo
      pmw_.modelChanged();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle model drop
  */ 
  
  public void handleModelDrop() {
    if (pmw_ != null) {
      pmw_.setVisible(false);
      pmw_.dispose();
      pmw_ = null;
    } 
    if (iw_ != null) {
      iw_.dropDashboard();
      iw_ = null;
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Clear the builder dashboard
  */ 
  
  public void clearBuilderDashboard() {
    iw_ = null;
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if builder dashboard is there
  */ 
  
  public boolean builderDashboardIsPresent() {
    return (iw_ != null);
  }
  
  /***************************************************************************
  **
  ** Get builder dashboard
  */ 
  
  public ModelBuilder.Dashboard getBuilderDashboard() {
    return (iw_);
  }

  /***************************************************************************
  **
  ** Pop the builder dashboard to the front
  */ 
  
  public void bringBuilderDashboardToFront() {
    if (iw_ != null) {
      iw_.pullToFront();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Install the builder dashboard
  */ 
  
  public void installBuilderDashboard(ModelBuilder.Dashboard dashboard) {
    if (iw_ != null) {
      throw new IllegalStateException();         
    }
    iw_ = dashboard;
    return;
  }
   
  /***************************************************************************
  **
  ** Clear the link drawing tracker
  */ 
  
  public void clearWorksheetLinkDrawingTracker() {
    wldt_ = null;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the link drawing tracker (null if not active)
  */ 
  
  public ModelBuilder.LinkDrawingTracker getWorksheetLinkDrawingTracker() {
    return (wldt_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have the link drawing tracker 
  */ 
  
  public boolean haveWorksheetLinkDrawingTracker() {
    return (wldt_ != null);
  }
  
  /***************************************************************************
  **
  ** Create the link drawing tracker 
  */ 
  
  public void installLinkDrawingTracker(ModelBuilder.LinkDrawingTracker ldt) {
    if (wldt_ != null) {
      throw new IllegalStateException();
    }     
    wldt_ = ldt;
    return;
  }   
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Handles per-tab state
  */ 
    
  public static class PerTab implements Cloneable {
    int index; // Per tab
    ModelImagePanel mip_; // Per tab
    boolean mipVisible_; // Per tab 
    JPanel ovrPanel_; // Per tab
    boolean ovrVisible_; // Per tab
    CardLayout myCard_; // Per tab
    JPanel hidingPanel_; // Per tab
    JPanel sliderPanel_; // Per tab
    JScrollPane jsp_; // Per tab
    GroupPanel grp_; // Per tab
    boolean showGroup;
    TabNameData tabNameData;
    JPanel tabContents;
  
    public PerTab(int index) {
      this.index = index;
    }    
    
    @Override
    public PerTab clone() {
      try {
        PerTab newVal = (PerTab)super.clone();
        return (newVal);            
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Handles syncing modal widgets for toggle-type actions
  */ 
    
  private class SyncToggleWidgets implements ActionListener {
    private JCheckBoxMenuItem jcb_;
    private BTToggleButton tb_;
    private MainCommands.ToggleAction noIcons_; 
    private MainCommands.ToggleAction withIcons_;
    private boolean ignore_;                   
                       
    SyncToggleWidgets(JCheckBoxMenuItem jcb, BTToggleButton tb, 
                      MainCommands.ToggleAction noIcons, 
                      MainCommands.ToggleAction withIcons) {
      jcb_ = jcb;
      tb_ = tb;
      noIcons_ = noIcons; 
      withIcons_ = withIcons;
      
      ignore_ = false;
      
      if (jcb_ != null) {
        jcb_.addActionListener(this);
      }
      if (tb_ != null) {
        tb_.addActionListener(this);
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        if (ignore_) {
          return;
        }
        ignore_ = true;
        if ((jcb_ != null) && (e.getSource() == jcb_)) {
          // Don't want action to trigger as we sync:
          if (tb_ != null) {
            withIcons_.setToIgnore(true);
            tb_.setSelected(jcb_.isSelected());
            withIcons_.setToIgnore(false);
          }
        } else if ((tb_ != null) && (e.getSource() == tb_)) {
          if (jcb_ != null) {
            noIcons_.setToIgnore(true);
            jcb_.setSelected(tb_.isSelected());
            noIcons_.setToIgnore(false);
          }
        }
        ignore_ = false;
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }
    
    public void cancelled() {
      MainCommands.ToggleAction useMe = (withIcons_ == null) ? noIcons_ : withIcons_;
      if (useMe == null) {
        return;
      }
      useMe.setAsActive(false);  // Only need to do to one (shared state!)
      ignore_ = true;
      if (noIcons_ != null) noIcons_.setToIgnore(true);
      if (withIcons_ != null) withIcons_.setToIgnore(true);

      if (jcb_ != null) jcb_.setSelected(false);         
      if (tb_ != null) tb_.setSelected(false);

      if (noIcons_ != null) noIcons_.setToIgnore(false);
      if (withIcons_ != null) withIcons_.setToIgnore(false);
      ignore_ = false;
      return;
    }
    
    public void activated() {
      MainCommands.ToggleAction useMe = (withIcons_ == null) ? noIcons_ : withIcons_;
      if (useMe == null) {
        return;
      }
      useMe.setAsActive(true);  // Only need to do to one (shared state!)
      ignore_ = true;
      if (noIcons_ != null) noIcons_.setToIgnore(true);
      if (withIcons_ != null) withIcons_.setToIgnore(true);

      if (jcb_ != null) jcb_.setSelected(true);         
      if (tb_ != null) tb_.setSelected(true);

      if (noIcons_ != null) noIcons_.setToIgnore(false);
      if (withIcons_ != null) withIcons_.setToIgnore(false);
      ignore_ = false;
      return;
    }    
  }
}
