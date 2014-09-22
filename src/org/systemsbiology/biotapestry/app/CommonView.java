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
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DesktopControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.OverlayDisplayChange;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.ModelImagePanel;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
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
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** The guts of the top-level BioTapestry window
*/

public class CommonView implements ModelChangeListener, BackgroundWorkerControlManager { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private ModelImagePanel mip_;
  private boolean mipVisible_;  
  private JPanel ovrPanel_;
  private boolean ovrVisible_; 
  private CardLayout myCard_;
  private JPanel hidingPanel_;
  private JPanel sliderPanel_;
  private JScrollPane jsp_;
  private JDialog pullFrame_;

  private MainCommands.ChecksForEnabled undoAction_;
  private MainCommands.ChecksForEnabled redoAction_;
  private SyncToggleWidgets pulldownSync_;
  
  private Map<FlowMeister.FlowKey, MainCommands.ChecksForEnabled> actionMap_;
  private JToolBar toolBar_;
  private BTToggleButton tb_;
  private AbstractAction pullwi_;
  
  private PerturbationsManagementWindow pmw_;
  
  
  private boolean accept_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CommonView(BTState appState) {
    appState_ = appState.setCommonView(this);  
    accept_ = true;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Do window title
  */ 

  public void manageWindowTitle(String fileName) {
    if (appState_.isHeadless() || !appState_.getIsEditor()) {
      return;
    }
    if (appState_.getTopFrame() == null) {
      return;  // Don't mess with viewer title (huh??)
    }
    ResourceManager rMan = appState_.getRMan();
    String title;
    if (fileName == null) {
      title = rMan.getString("window.editorTitle");  
    } else {
      String titleFormat = rMan.getString("window.editorTitleWithName");
      title = MessageFormat.format(titleFormat, new Object[] {fileName});
    }
    appState_.getTopFrame().setTitle(title);
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
    if (appState_.getTree().getTreeSelectionPath() == null) {
      DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
      appState_.getTree().setTreeSelectionPath(dacx.getGenomeSource().getModelHierarchy().getVfgSelection());
    }
    enableControls(true);
    return;
  }  
  
  /****************************************************************************
  **
  ** also redraw....
  */  
  
  public void redraw() {
    appState_.getSUPanel().drawModel(false);
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
    
    if (appState_.isHeadless()) {
      throw new IllegalStateException();
    }
    
    JComponent jcp = appState_.getContentPane();
    MenuSource mSrc = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
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
            ControlFlow cf = appState_.getFloM().getControlFlow(xpka.getKey(), xpka.getActionArg());
            DesktopControlFlowHarness dcf = new DesktopControlFlowHarness(appState_, new DesktopDialogPlatform(appState_.getTopFrame()));
            dcf.initFlow(cf, new DataAccessContext(appState_, appState_.getGenome()));
            dcf.runFlow();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
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
    if (appState_.getIsEditor()) {
      buildEditorView();
    } else {
      buildViewOnlyView();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Build an editor view
  */ 
  
  private void buildEditorView() {
    if (!appState_.isHeadless()) {
      bindKeys();
      appState_.getContentPane().setLayout(new BorderLayout());
    }
    
    //
    // Even the "Headless" commonView has lots of virtual controls that stand in for the Swing versions:
    //
    
    new SUPanel(appState_);            
    new MainCommands(appState_);
    new VirtualRecentMenu(appState_);
    new VirtualPathControls(appState_, new DynamicDataAccessContext(appState_));
    new VirtualGaggleControls(appState_);  
    NavTree navTree = appState_.getDB().getModelHierarchy();   
    JTree jTree = (appState_.isHeadless()) ? null : new JTree(navTree);
    new VirtualModelTree(appState_, jTree, navTree);
       
    //
    // Build up the main menu bar.  Ask for the cross-platform editor menubar definition,
    // supply prebuilt menus, build it, then extract tagged items and use then to finish
    // the job:
    //
    
    if (!appState_.isHeadless()) {
      buildEditorUI();
    }
   
    buildCore(toolBar_, jTree);  
    TreePath dstp = navTree.getDefaultSelection();
    navTree.setSkipFlag(NavTree.SKIP_EVENT);
    appState_.getTree().setTreeSelectionPath(dstp);
    navTree.setSkipFlag(NavTree.NO_FLAG);
    appState_.getZoomTarget().fixCenterPoint(true, null, false);
    return;
  }

  /***************************************************************************
  **
  ** Build editor UI components
  */ 
  
  private void buildEditorUI() {
    boolean doGaggle = appState_.getDoGaggle();  
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    MenuSource mSrc = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
    XPlatMenuBar xpmbar = mSrc.defineEditorMenuBar(dacx);
    
    DesktopMenuFactory dmFac = new DesktopMenuFactory(appState_);
    MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
        
    bifo.collected.put("SELECTED", appState_.getSUPanel().getSelectedMenu());
    bifo.collected.put("CURRENT_MODEL", appState_.getTree().getCurrentModelMenu());
    bifo.collected.put("RECENT", appState_.getRecentMenu().getJMenu());
    bifo.collected.put("USER_PATH_CHOOSE", appState_.getPathControls().getJMenu());    
    if (doGaggle) {
      bifo.collected.put("GOOSE_CHOOSE", appState_.getGaggleControls().getJMenu());
    }   
    bifo.conditions.put("DO_GAGGLE", Boolean.valueOf(doGaggle));

    JMenuBar menuBar = dmFac.buildDesktopMenuBar(xpmbar, new DesktopMenuFactory.ActionSource(appState_), bifo);
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
          UndoManager undom = appState_.getUndoManager();
          boolean canUndo = undom.canUndo();
          boolean canRedo = undom.canRedo();
          ResourceManager rMan = appState_.getRMan();
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
          appState_.getSUPanel().stockSelectedMenu(new DataAccessContext(appState_, appState_.getGenome()));
          appState_.getTree().stockCurrentModelMenu();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
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
    appState_.getTopFrame().setJMenuBar(menuBar);
    return;
  }
  
  /***************************************************************************
  **
  ** Stock the tool bar
  */ 
  
  private void stockEditorToolBar(boolean showPathControls, boolean showOverlayControls) {
    
    if (appState_.isHeadless()) {
      throw new IllegalStateException();
    }
    
    boolean doGaggle = appState_.getDoGaggle();
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    MenuSource mSrc = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
    DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
    XPlatToolBar toob = mSrc.defineEditorToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();
    
    bifot.conditions.put("DO_GAGGLE", Boolean.valueOf(doGaggle));
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(showPathControls));
    bifot.conditions.put("SHOW_OVERLAY", Boolean.valueOf(showOverlayControls));  
    if (showPathControls) {
      bifot.components.put("PATH_COMBO", appState_.getPathControls().getJCombo());
    }
     
    appState_.getGaggleControls().stockToolBarPre(bifot);
    
    dmf.stockToolBar(toolBar_, toob, actionMap_, bifot);
    
    if (tb_ == null) {
      pullwi_ = bifot.taggedActions.get("PULLDOWN");
      tb_ = (BTToggleButton)bifot.components.get("PULLDOWN");
    }
    
    appState_.getGaggleControls().stockToolBarPost(bifot);
  
    return;
  }

  /***************************************************************************
  **
  ** Calculate how to disable the main controls
  */ 
  
  public XPlatMaskingStatus calcDisableControls(int pushFlags, boolean displayToo) {
    XPlatMaskingStatus ms = new XPlatMaskingStatus();
    appState_.getMainCmds().calcPushDisabled(pushFlags, ms); 
    ms.setRecentMenuOn(false); 
    ms.setPathControlsOn(false);
    // any push disables the overlay control, though visibility may still be allowed:    
    boolean allowOverlayViz = ((pushFlags & MainCommands.SKIP_OVERLAY_VIZ_PUSH) != 0x00);
    ms.setOverlayLevelOn(allowOverlayViz);
    ms.setMainOverlayControlsOn(false);
       
    if (displayToo) {
      if (myCard_ != null) {
        ms.setModelDisplayOn(false);
      }
      ms.setKeystrokesOn(false);
    } else {
      ms.setModelDisplayOn(true);
      ms.setKeystrokesOn(true);
    }
    ms.setModelTreeOn(false);
    ms.setSliderOn(false);
    VirtualGaggleControls vgc = appState_.getGaggleControls();
    if (vgc != null) {
      ms.setGeeseOn(false);
    }
    return (ms);
  }  
  
  /***************************************************************************
  **
  ** Disable the main controls based on masking information
  */ 
  
  public void disableControls(XPlatMaskingStatus ms) {
    if (!ms.isMaskingActive()) {
      return;
    }
    appState_.getMainCmds().pushDisabled(ms);     
    appState_.getRecentMenu().pushDisabled(ms.isRecentMenuOn().booleanValue());
    appState_.getPathControls().pushDisabled(ms.arePathControlsOn().booleanValue());
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    appState_.getNetOverlayController().pushDisabled(ms, ddacx);
     
    if ((myCard_ != null) && !ms.isModelDisplayOn()) {
      myCard_.show(hidingPanel_, "Hiding");
    }

    acceptKeystrokes(ms.areKeystrokesOn().booleanValue());

    appState_.getTree().setTreeEnabled(ms.isModelTreeOn().booleanValue());

    appState_.getVTSlider().setSliderEnabled(ms.isSliderOn().booleanValue());

    appState_.getPathControls().setPathsEnabled(ms.arePathControlsOn().booleanValue()); // Doesn't this duplicate part of the PathControl push above??
    VirtualGaggleControls vgc = appState_.getGaggleControls();
    if (vgc != null) {
      vgc.setGooseEnabled(ms.areGeeseOn().booleanValue());
    }
    
    if (!appState_.isHeadless()) {  // not headless...
      appState_.getContentPane().validate();
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
    appState_.getMainCmds().popDisabled();
    if (myCard_ != null) {
      myCard_.show(hidingPanel_, "SUPanel");
    }
    acceptKeystrokes(true);
    appState_.getRecentMenu().popDisabled();
    appState_.getPathControls().popDisabled();
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    appState_.getNetOverlayController().popDisabled(ddacx);
    appState_.getTree().setTreeEnabled(true);
    appState_.getVTSlider().setSliderEnabled(true);
    appState_.getPathControls().setPathsEnabled(true);
    VirtualGaggleControls vgc = appState_.getGaggleControls();
    if (vgc != null) {
      vgc.setGooseEnabled(true);
    }
    
    if (!appState_.isHeadless()) {  // not headless...
      appState_.getContentPane().validate();    
    }
    
    //
    // Following background thread operations, sometimes we need to
    // get keyboard focus back to the network panel:
    //
    // We make this conditional to keep it from being called in normal operation as 
    // the genome is changed, which causes the time slider to lose focus EVERY 
    // TIME IT IS MOVED!!!!!!!
    
    if (withFocus) {
      appState_.getSUPanel().requestFocusForModel();
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
  ** Stock the viewer tool bar
  */ 
  
  private void stockViewerToolBar(boolean showPathControls) {
    
    if (appState_.isHeadless()) {
      throw new IllegalStateException();
    }
       
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    MenuSource mSrc = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
    DesktopMenuFactory dmf = new DesktopMenuFactory(appState_);
    XPlatToolBar toob = mSrc.defineViewerToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();
 
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(showPathControls));
 
    if (showPathControls) {
      bifot.components.put("PATH_COMBO", appState_.getPathControls().getJCombo());
    }
      
    dmf.stockToolBar(toolBar_, toob, actionMap_, bifot);   
    return;
  }

  /***************************************************************************
  **
  ** Build a viewer view for an application
  */ 
  
  private void buildViewOnlyView() {
    if (!appState_.isHeadless()) {
      appState_.getContentPane().setLayout(new BorderLayout());
    }
    new SUPanel(appState_);
    new MainCommands(appState_);
    new VirtualRecentMenu(appState_);
    new VirtualPathControls(appState_, new DynamicDataAccessContext(appState_));
   
    //
    // Build the tool bar, using actions with icons
    //

    if (!appState_.isHeadless()) {
      buildViewOnlyUI();
    }
    NavTree navTree = appState_.getDB().getModelHierarchy();   
    JTree jTree = (appState_.isHeadless()) ? null : new JTree(navTree);
    new VirtualModelTree(appState_, jTree, navTree);
    buildCore(toolBar_, jTree);
    return;
  }  

  /***************************************************************************
  **
  ** Build a viewer view for an application
  */ 
  
  private void buildViewOnlyUI() {
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    MenuSource mSrc = new MenuSource(appState_.getFloM(), !appState_.getIsEditor(), appState_.getDoGaggle());
    XPlatToolBar toob = mSrc.defineViewerToolBar(dacx);   
    MenuSource.BuildInfo bifot = new MenuSource.BuildInfo();       
    bifot.conditions.put("SHOW_PATH", Boolean.valueOf(true));
    DesktopMenuFactory dmf = new DesktopMenuFactory(appState_); 
    actionMap_ = dmf.stockActionMap(toob, bifot);
    
    toolBar_ = new JToolBar();
    stockViewerToolBar(false);
    toolBar_.setFloatable(false);
    return;
  }

  /***************************************************************************
  **
  ** init
  */ 
  
  private void buildCore(JToolBar toolBar, JTree jtree) {
    
    new VirtualTimeSlider(appState_);
    
    if (!appState_.isHeadless()) {
      buildUICore1(toolBar);
    }
    
    // Kinda bogus: chicken and egg!
    appState_.getZoomCommandSupport().registerScrollPaneAndZoomTarget(jsp_, appState_.getZoomTarget());
    
    //
    // Build the nav tree panel stuff:
    //
    
    appState_.getTree().setupTree();
    mip_ = new ModelImagePanel(appState_); 
    mip_.setImage(null);
    mipVisible_ = false;   
    ovrVisible_ = false;
 
    if (!appState_.isHeadless()) {
      buildUICore2(jtree);
    }
    
    appState_.getDisplayOptMgr().setForBigScreen(appState_.doBig());    
    
    EventManager mgr = appState_.getEventMgr();
    mgr.addModelChangeListener(this);    

    SelectionChangeEvent ev = new SelectionChangeEvent(null, null, SelectionChangeEvent.SELECTED_MODEL);
    mgr.sendSelectionChangeEvent(ev);
    return;
  }

  /***************************************************************************
  **
  ** init
  */ 
  
  private void buildUICore1(JToolBar toolBar) {
 
    appState_.getContentPane().add(toolBar, BorderLayout.NORTH);
    jsp_ = appState_.getSUPanel().getPanelInPane();
    hidingPanel_ = new JPanel();
    myCard_ = new CardLayout();
    hidingPanel_.setLayout(myCard_);
    hidingPanel_.add(jsp_, "SUPanel");
    JPanel blankPanel = new JPanel();
    blankPanel.setBackground(Color.white);
    hidingPanel_.add(blankPanel, "Hiding");
    jsp_.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    jsp_.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    return;
  }
  
  /***************************************************************************
  **
  ** init
  */ 
  
  private void buildUICore2(JTree jtree) {

    JScrollPane tjsp = new JScrollPane(jtree);
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    UiUtil.gbcSet(gbc, 0, 0, 1, 2, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.N, 1.0, 1.0);
    controlPanel.add(tjsp, gbc);
 
    JPanel mipPan = mip_.getPanel();
    mipPan.setBorder(new LineBorder(Color.black, 1));
    mipPan.setMinimumSize(new Dimension(250,0));
    mipPan.setPreferredSize(new Dimension(250,0)); 
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    ovrPanel_ = appState_.getNetOverlayController().getNetOverlayNavigator(ddacx);
    ovrPanel_.setBorder(new LineBorder(Color.black, 1));      
    ovrPanel_.setMinimumSize(new Dimension(250,0));
    ovrPanel_.setPreferredSize(new Dimension(250,0)); 
     
    sliderPanel_ = appState_.getVTSlider().getSliderPanel();
          
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(sliderPanel_, gbc);
    
    UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(ovrPanel_, gbc);
    
    UiUtil.gbcSet(gbc, 0, 4, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
    controlPanel.add(mipPan, gbc);  

    JTextPane jtp = appState_.getTextBoxMgr().getTextPane();
    if (appState_.doBig()) {
      Font tpFont = jtp.getFont();
      String fontName = tpFont.getName();
      int fontType = tpFont.getStyle();
      jtp.setFont(new Font(fontName, fontType, 20));
    }
    jtp.setMinimumSize(new Dimension(640,(appState_.doBig()) ? 75 : 50));    
    jtp.setPreferredSize(new Dimension(640, (appState_.doBig()) ? 75 : 50));
    jtp.setEditable(false);
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new GridLayout());

    JScrollPane txtsp = new JScrollPane(jtp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    textPanel.add(txtsp);

    JPanel topPanel = new JPanel();
    jsp_.setMinimumSize(new Dimension(150, 150));
    tjsp.setMinimumSize(new Dimension(250, 250));    
    jtree.setMinimumSize(new Dimension(250,250));
    controlPanel.setMinimumSize(new Dimension(250, 250));    
      //myTree_.setPreferredSize(new Dimension(250, 250));  NO! Scroll bars don't work!
    //JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lsp, jsp);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, hidingPanel_);
    topPanel.setLayout(new GridLayout(1,1));
    topPanel.add(sp);
    JComponent cp = appState_.getContentPane();
    cp.add(topPanel, BorderLayout.CENTER);
    cp.add(textPanel, BorderLayout.SOUTH);
    return;
  }

  /***************************************************************************
  **
  ** Handle display update
  */
  
  public void updateDisplayForGenome(Genome currGenome) {
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    String mi = currGenome.getGenomeImage();
    BufferedImage bi = appState_.getImageMgr().getImage(mi);
    makeImageVisible(appState_.getImageMgr().hasAnImage());
    mip_.setImage(bi);
    makeOverlayControlsVisible(appState_.getNetOverlayController().aModelHasOverlays(ddacx));
    makeSliderVisible((new FullGenomeHierarchyOracle(appState_)).hourlyDynamicModelExists());    
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
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    makeImageVisible(appState_.getImageMgr().hasAnImage());
    makePathControlsVisible(appState_.getPathMgr().getPathCount() > 0);
    makeOverlayControlsVisible(appState_.getNetOverlayController().aModelHasOverlays(ddacx));
    makeSliderVisible((new FullGenomeHierarchyOracle(appState_)).hourlyDynamicModelExists());
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set the visibility of the image panel
  */
  
  private void makeImageVisible(boolean show) {
    if (mipVisible_ == show) {
      return;
    }
    boolean isHeadless = appState_.isHeadless();
    if (!isHeadless) {
      int height = (show) ? 200 : 0;
      JPanel mipPan = mip_.getPanel();
      mipPan.setMinimumSize(new Dimension(250, height));
      mipPan.setPreferredSize(new Dimension(250, height));
      mipPan.invalidate();
      appState_.getContentPane().validate();
    }
    mipVisible_ = show;
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set the visibility of the overlay controls
  */
  
  private void makeOverlayControlsVisible(boolean show) {
    if (!appState_.isHeadless()) {
      JFrame topFrame = appState_.getTopFrame();
      if (ovrVisible_ == show) {
        if ((toolBar_ != null) && (topFrame != null)) {
          toolBar_.invalidate();
          appState_.getContentPane().validate();
          toolBar_.repaint();
        }
        return;
      }
      
      if (appState_.getIsEditor()) {
        stockEditorToolBar(appState_.getPathControls().areControlsVisible(), show);
      } 
      
      int height = (show) ? 250 : 0;
      ovrPanel_.setMinimumSize(new Dimension(250, height));
      ovrPanel_.setPreferredSize(new Dimension(250, height));
      ovrPanel_.invalidate();
      if (toolBar_ != null) toolBar_.invalidate();
      if (topFrame != null) appState_.getContentPane().validate();
      if (toolBar_ != null) toolBar_.repaint();
    }
    ovrVisible_ = show;
    return;
  }        
  
  /***************************************************************************
  **
  ** Used to set the visibility of the slider controls
  */
  
  private void makeSliderVisible(boolean show) {
    appState_.getVTSlider().makeSliderVisible(show);
    return;
  }    

  /***************************************************************************
  **
  ** Used to set the visibility of the path controls
  */
  
  public void makePathControlsVisible(boolean show) {
    VirtualPathControls vpc = appState_.getPathControls();
    if (!appState_.isHeadless()) {
      JFrame topFrame = appState_.getTopFrame();  
      if (vpc.areControlsVisible() == show) {
        if ((toolBar_ != null) && (topFrame != null)) {
          toolBar_.invalidate();
          appState_.getContentPane().validate();
          toolBar_.repaint();
        }
        return;
      }
      if (appState_.getIsEditor()) {
        stockEditorToolBar(show, ovrVisible_);
      } else {
        stockViewerToolBar(show);
      }
      if ((toolBar_ != null) && (topFrame != null)) {
        toolBar_.invalidate();
        appState_.getContentPane().validate();
        toolBar_.repaint();
      }
    }
    vpc.setControlsVisible(show);
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
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(appState_);
    if (mcev.getChangeType() == ModelChangeEvent.MODEL_DROPPED) {
      appState_.getVTSlider().dropTheSlider();
      makeImageVisible(appState_.getImageMgr().hasAnImage());
      makeOverlayControlsVisible(appState_.getNetOverlayController().aModelHasOverlays(ddacx));
      makeSliderVisible((new FullGenomeHierarchyOracle(appState_)).hourlyDynamicModelExists());
      mip_.setImage(null);
      return;
    }
    if (mcev.getChangeType() == ModelChangeEvent.DYNAMIC_MODEL_ADDED) {
      makeSliderVisible((new FullGenomeHierarchyOracle(appState_)).hourlyDynamicModelExists());
    }    
    makeOverlayControlsVisible(appState_.getNetOverlayController().aModelHasOverlays(ddacx));    
    Database db = appState_.getDB();
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    if (mcev.getChangeType() == ModelChangeEvent.PROPERTY_CHANGE) {     
      if (!appState_.getTree().isCurrentlyDisplayed(mcev)) {
        appState_.getVTSlider().ditchSliderIfStale(mcev);
      } else {  // Displayed
        if (appState_.getTree().isDynamic(dacx)) {   
          String id = appState_.getVTSlider().handleSliderModelChange(mcev);
          //
          // We are handling an event notification. This means we are in the middle of another control flow execution, and 
          // we have no business launching another control flow.  So just use the guts of the switching operation from the
          // SetCurrentModel.StepState flow state:
          //
          SetCurrentModel.StepState agis = new SetCurrentModel.StepState(appState_, SetCurrentModel.SettingAction.FOR_EVENT, dacx);
          agis.coreSwitchingSupport(id, null);
        } else {
          appState_.getTextBoxMgr().checkForChanges(mcev);
          String id = mcev.getGenomeKey();
          Genome currGenome = db.getGenome(id);
          String mi = currGenome.getGenomeImage();
          BufferedImage bi = appState_.getImageMgr().getImage(mi);
          makeImageVisible(appState_.getImageMgr().hasAnImage());
          mip_.setImage(bi);
          appState_.getSUPanel().drawModel(true);
        }
      }
      appState_.getTree().updateNavTreeNames(mcev, dacx);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NavigationChange undo) {
    if (undo.userPathSelection || undo.userPathSync) {
      appState_.getPathController().changeUndo(undo);
    }
    
    if (undo.odc != null) {
      OverlayDisplayChange odc = undo.odc;
      NetOverlayController noc = appState_.getNetOverlayController();
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
          DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
          noc.setFullOverlayState(odc.oldOvrKey, odc.oldModKeys, odc.oldRevealedKeys, null, dacx);
        }
      }
    }
      
    if (undo.oldPath != null) {
      NavTree navTree = appState_.getDB().getModelHierarchy();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)navTree.getRoot();
      appState_.getTree().setTreeSelectionPathForUndo(navTree.mapPath(undo.oldPath, root));
    } else if (undo.sliderID != null) {
      appState_.getVTSlider().handleSliderUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NavigationChange undo) {
    if (undo.userPathSelection || undo.userPathSync) {
      appState_.getPathController().changeRedo(undo);
    }
    
    if (undo.odc != null) {
      OverlayDisplayChange odc = undo.odc;
      NetOverlayController noc = appState_.getNetOverlayController();
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
          DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
          noc.setFullOverlayState(odc.newOvrKey, odc.newModKeys, odc.newRevealedKeys, null, dacx);
        }
      }
    }    
    
    if (undo.newPath != null) {
      NavTree navTree = appState_.getDB().getModelHierarchy();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)navTree.getRoot();
      appState_.getTree().setTreeSelectionPathForUndo(navTree.mapPath(undo.newPath, root));
    } else if (undo.sliderID != null) {
      appState_.getVTSlider().handleSliderRedo(undo);
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Do init steps for embedded panel
  */  
  
  public void embeddedPanelInit() {

    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    Database db = appState_.getDB();
    dacx.drop();
    db.setWorkspaceNeedsCenter();
    db.installLegacyTimeAxisDefinition();
    appState_.getGroupMgr().drop(); 
    appState_.getLSSupport().newModelOperations(dacx);
    appState_.getSUPanel().drawModel(true);
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
  
   public void launchPerturbationsManagementWindow(PertFilterExpression pfe, DataAccessContext dacx) {
     if (pmw_ != null) {   
       pmw_.setExtendedState(JFrame.NORMAL);
       pmw_.toFront();
       pmw_.jumpWithNewFilter(pfe);
       return;
     }
              
     if (!appState_.getLSSupport().prepTimeAxisForDataImport(dacx)) {
       return;
     }    
     PerturbationData pd = dacx.getExpDataSrc().getPertData();  
     pmw_ = new PerturbationsManagementWindow(appState_, dacx, pd, pfe);
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
    return;
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
        appState_.getExceptionHandler().displayException(ex);
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
