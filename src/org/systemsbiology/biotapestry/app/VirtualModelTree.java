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

import java.awt.Event;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.TreeSupport;
import org.systemsbiology.biotapestry.cmd.undo.ExpansionChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathControllerChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.nav.XPlatModelTree;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.menu.DesktopMenuFactory;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.MenuItemTarget;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** A JTree Wrapper that has Popups and Tooltips
*/

public class VirtualModelTree implements TreeSelectionListener, TreeExpansionListener {
                                                          
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private JPopupMenu leafPopup_;
  private ModelMenu popMenuContents_;
  private ModelMenu mainMenuContents_;
  private JTree myTree_;
  
  private Genome popupModel_;
  private Genome popupModelAncestor_;
  private TreeNode popupNode_;
  private String selectionTargetID_;
  private TreeNode selectionNode_;

  private boolean doingUndo_;
  private StaticDataAccessContext doingUndoDacx_;
  private boolean doSelectionIgnore_;
  private boolean doNotFlow_;
  private JMenu currModMenu_;
  private TreePath lastPath_;
  
  private UIComponentSource uics_;
  private CmdSource cSrc_;
  private HarnessBuilder hBld_;
  
  private TreePath virtualSelection_;
  private DefaultTreeModel virtualModel_;
  private DynamicDataAccessContext ddacx_;
  private TabSource tSrc_;
  private UndoFactory uFac_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualModelTree(JTree myTree, DefaultTreeModel model, CmdSource cSrc, UIComponentSource uics, 
                          DynamicDataAccessContext ddacx, HarnessBuilder hBld, TabSource tSrc, UndoFactory uFac) {
   
    cSrc_ = cSrc;
    uics_ = uics;
    ddacx_ = ddacx;
    hBld_ = hBld;
    tSrc_ = tSrc;
    uFac_ = uFac;
  
    myTree_ = myTree;
    virtualModel_ = model;
    
    if (myTree_ != null) {
      myTree_.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      myTree_.addTreeSelectionListener(this);
      myTree_.addTreeExpansionListener(this);
      ToolTipManager.sharedInstance().registerComponent(myTree_);
      myTree_.addMouseListener(new MouseHandler());

      myTree_.setCellRenderer(new PopupCellRenderer(uics_, ddacx_));
    }
    
    doSelectionIgnore_ = false;
    selectionTargetID_ = null;
    selectionNode_ = null;
    doNotFlow_ = false;
    
    boolean readOnly = !uics_.getIsEditor();
    if (!readOnly) {   
      popMenuContents_ = new ModelMenu();
      if (myTree_ != null) {
        leafPopup_ = new JPopupMenu();
        leafPopup_.addPopupMenuListener(new PopupHandler());
        popMenuContents_.fillMenu(new MenuItemTarget(leafPopup_));
      }
      mainMenuContents_ = new ModelMenu();
    }
    
    // Done for headless mode:
    
    if (myTree_ == null) {
      return;
    } 
    
    if (uics_.doBig()) {
      PopupCellRenderer renderer = (PopupCellRenderer)myTree_.getCellRenderer();
      Font drFont = myTree_.getFont();
      String fontName = (drFont == null) ? "arial" : drFont.getName();
      int fontType = (drFont == null) ? Font.PLAIN : drFont.getStyle();
      renderer.setFont(new Font(fontName, fontType, 20));
    }

    ResourceManager rMan = ddacx_.getRMan();
    currModMenu_ = new JMenu(rMan.getString("command.currentModelMenu"));
    currModMenu_.setMnemonic(rMan.getChar("command.currentModelMenuMnem"));
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /** Called whenever an item in the tree has been collapsed.
  *
  */
  public void treeCollapsed(TreeExpansionEvent event) {
  }  

  /** Called whenever an item in the tree has been expanded.
  *
  */
  public void treeExpanded(TreeExpansionEvent event) { 
  }
   
  /***************************************************************************
  **
  ** Called when the tree is selected
  */
  
  public void valueChanged(TreeSelectionEvent e) {
    try {
      //
      // Previous handler installed on JTree
      //
      TreePath tp = (myTree_ != null) ? myTree_.getSelectionPath() : virtualSelection_;
      // THIS IS OLD STATE: NOTHING HAS BEEN CHANGED YET!
      StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_);
      PathResolution res = resolvePath(tp, dacx);
      if (res == null) {
        selectionTargetID_ = null;
        selectionNode_ = null;
      } else if (res.pathTarget != null) {
        selectionTargetID_ = res.pathTarget.getID();
        selectionNode_ = res.pathNode;
      } else {
        selectionTargetID_ = null;
        selectionNode_ = res.pathNode;
      }
      
      if (doNotFlow_) {
        //
        // This means we are in the middle of another control flow execution, and we have no business launching another control 
        // flow.  So just use the guts of the switching operation from the SetCurrentModel.StepState flow state:
        //
        
        DataAccessContext useDacx = (doingUndo_) ? doingUndoDacx_ : ddacx_;
        SetCurrentModel.StepState agis = (SetCurrentModel.StepState)hBld_.getNewCmdState(FlowMeister.OtherFlowKey.MODEL_SELECTION_FROM_TREE, useDacx);
        agis.setPreloadForTreeClick(tp, lastPath_, doSelectionIgnore_, doingUndo_, uics_, tSrc_, uFac_);
        agis.stepToProcessTree();
      } else {   
        HarnessBuilder.PreHarness preH = hBld_.buildHarness(FlowMeister.OtherFlowKey.MODEL_SELECTION_FROM_TREE);
        SetCurrentModel.StepState agis = (SetCurrentModel.StepState)preH.getCmdState();
        agis.setPreloadForTreeClick(tp, lastPath_, doSelectionIgnore_, doingUndo_, uics_, tSrc_, uFac_);
        agis.setAppState(ddacx_.getMetabase().getAppState(), ddacx_); // UGH! FIXME
        hBld_.runHarness(preH);
      }
      lastPath_ = tp;
    } catch (Exception ex) {
      ExceptionHandler exh = uics_.getExceptionHandler();
      if (exh != null) {
        exh.displayException(ex);
      } else {
        Thread.dumpStack();
        System.err.println(ex);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Scroll to show path
  */
     
  public void scrollTreePathToVisible(TreePath tp) {
    if (myTree_ != null) {
      myTree_.scrollPathToVisible(tp);
    }
    return;
  }

  /***************************************************************************
  **
  ** Request focus
  */
     
  public void requestTreeFocus() {
    if (myTree_ != null) {
      myTree_.requestFocus();
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Answer if expanded
  */
     
  public boolean isTreeExpanded(TreePath tp) {   
    return ((myTree_ == null) ? true : myTree_.isExpanded(tp));
  }
 
  /***************************************************************************
  **
  ** setup the tree
  */
    
   public void setupTree() {
     if (myTree_ != null) {
       myTree_.setRootVisible(false);
       myTree_.setFont(ddacx_.getFontManager().getFixedFont(FontManager.TREE));
       myTree_.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
     }
     return;
   }
  
  /***************************************************************************
  **
  ** Get the selection path
  */
   
  public TreePath getTreeSelectionPath() {
    return ((myTree_ != null) ? myTree_.getSelectionPath() : virtualSelection_);
  } 
  
  /***************************************************************************
  **
  ** Set the selection path
  */
   
  public void setTreeSelectionPathForUndo(TreePath tp, StaticDataAccessContext dacx) {
    doNotFlow_ = true;
    doingUndo_ = true;
    doingUndoDacx_ = dacx;
    if (myTree_ != null) {
      myTree_.setSelectionPath(tp);
    } else {
      virtualSelection_ = tp;
      valueChanged(null);
    }
    doingUndo_ = false;
    doNotFlow_ = false;
    return;
  } 
   
  /***************************************************************************
  **
  ** Set the selection path
  */
   
  public void setTreeSelectionPath(TreePath tp) {
    doNotFlow_ = true;
    if (myTree_ != null) {
      myTree_.setSelectionPath(tp);
    } else {
      virtualSelection_ = tp;
      valueChanged(null);
    }
    doNotFlow_ = false;
    return;
  } 
 
  /***************************************************************************
  **
  ** Enabled/disable the tree
  */
   
  public void setTreeEnabled(boolean enabled) {
    if (myTree_ != null) {
      myTree_.setEnabled(enabled);
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Expand to show the path
  */
   
  public void expandTreePath(TreePath tp) {
    if (myTree_ != null) {
      myTree_.expandPath(tp);
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the model
  */
   
  public DefaultTreeModel getTreeModel() {
    return ((myTree_ != null) ? (DefaultTreeModel)myTree_.getModel() : virtualModel_);
  }  
  
  /***************************************************************************
  **
  ** Set the model
  */
   
  public void setTreeModel(TreeModel model) {
    if (myTree_ != null) {
      myTree_.setModel(model);
    } else {
      virtualModel_ = (DefaultTreeModel)model;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get popup model. May be null if group node
  */
  
  public Genome getPopupModel() {
    return (popupModel_);
  }
  
  /***************************************************************************
  **
  ** Get popup model ancestor. Only null if nothing is selected.
  */
  
  public Genome getPopupModelAncestor() {
    return (popupModelAncestor_);
  }

  /***************************************************************************
  **
  ** Get popup node
  */
  
  public TreeNode getPopupNode() {
    return (popupNode_);
  }
  
  /***************************************************************************
  **
  ** Get SelectionTargetID
  */
  
  public String getSelectionTargetID() {
    return (selectionTargetID_);
  }
  
  /***************************************************************************
  **
  ** Get Selection Node
  */
  
  public TreeNode getSelectionNode() {
    return (selectionNode_);
  }
  
  /***************************************************************************
  **
  ** Get menu for current model
  */
  
  public JMenu getCurrentModelMenu() {
    return (currModMenu_);
  }
  
  /***************************************************************************
  **
  ** Get xplatmenu for current model
  */
  
  public XPlatMenu getCurrentModelXPlatMenu() {
    if (mainMenuContents_ == null) {
      return (null);
    }
    return (mainMenuContents_.xpm_);
  }
  
  /***************************************************************************
  **
  ** Stock the current model menu
  */
  
  public void stockCurrentModelMenu() {
    
    boolean readOnly = !uics_.getIsEditor();
    
    if ((selectionNode_ == null) || readOnly) {
      currModMenu_.removeAll();
      currModMenu_.setEnabled(false);
    } else { 
      StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_, selectionTargetID_);
      popupModel_ = dacx.getCurrentGenome();
      popupNode_ = selectionNode_;      
      currModMenu_.removeAll();
      mainMenuContents_.fillMenu(new MenuItemTarget(currModMenu_));      
      mainMenuContents_.manageActionEnables(popupNode_, dacx);
      currModMenu_.setEnabled(true);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Map of enabled flows
  */
  
  public Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> getTreeEnables(DataAccessContext dacx) {
    if (mainMenuContents_ == null) { // Viewer mode
      return (null);
    }
    return (mainMenuContents_.getTreeFlowsEnabled(dacx));  
  }
  
  /***************************************************************************
  **
  ** Answers if model notification is for currently displayed model.
  */
  
  public boolean isCurrentlyDisplayed(ModelChangeEvent mcev, DataAccessContext dacx) {
    String currID = dacx.getCurrentGenomeID();
    if (currID == null) {
      return (false);
    }
    String proxyID = null;
    if (DynamicInstanceProxy.isDynamicInstance(currID)) {
      proxyID = DynamicInstanceProxy.extractProxyID(currID);
    }
    String id = mcev.getOldKey();
    if (id != null) {
      if (mcev.oldKeyIsProxy()) {
        return ((proxyID != null) && (id.equals(proxyID)));
      } else {
        return (id.equals(currID));
      }
    } else { // no old key->key in current event is used
      if (mcev.isProxyKey()) {
        return ((proxyID != null) && (mcev.getProxyKey().equals(proxyID)));
      } else {
        return (mcev.getGenomeKey().equals(currID));
      }        
    }
  }
 
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(ExpansionChange undo) {
    if (undo.isForUndo) {
      doingUndo_ = true;
      Iterator<TreePath> plit = undo.expanded.iterator();
      while (plit.hasNext()) {
        TreePath tp = plit.next();
        expandTreePath(tp);
      }
      if (undo.selected != null) {
        setTreeSelectionPath(undo.selected);
      }
      doingUndo_ = false;
      if (myTree_ != null) {
        myTree_.revalidate();
      }
    }
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(ExpansionChange undo) {
    if (!undo.isForUndo) {
      doingUndo_ = true;
      Iterator<TreePath> plit = undo.expanded.iterator();
      while (plit.hasNext()) {
        TreePath tp = plit.next();
        expandTreePath(tp);
      }
      if (undo.selected != null) {
        setTreeSelectionPath(undo.selected);
      }
      doingUndo_ = false;
      if (myTree_ != null) {
        myTree_.revalidate(); // do any good?
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Set if we should ignore selection...
  */
  
  public void setIgnoreSelection(boolean val) {
    doSelectionIgnore_ = val;
    return;
  }  
    
  /***************************************************************************
  **
  ** Set if we should not launch our own control flow in our event handler
  */
  
  public void setDoNotFlow(boolean val) {
    doNotFlow_ = val;
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle an expansion change
  */
  
  public ExpansionChange doUndoExpansionChange(UndoSupport support, DataAccessContext dacx, boolean recordPathControllerState) {  
    //
    // Record the pre-change tree expansion state, and hold onto a copy of the expansion
    // info, since it will need to be mapped based on the nav tree undo info, and we
    // are going to use it to restore our tree after we are done.
    //
    NavTree nt = dacx.getGenomeSource().getModelHierarchy();  
    ExpansionChange ec = (new TreeSupport(uics_)).buildExpansionChange(true, nt);
    support.addEdit(new ExpansionChangeCmd(ec));
    if (recordPathControllerState) {
      UserTreePathController utpc = uics_.getPathController();
      support.addEdit(new UserTreePathControllerChangeCmd(dacx, utpc.recordStateForUndo(true)));
    }
    
    return (ec);
  }
  
  /***************************************************************************
  **
  ** Handle an expansion change
  */
  
  public void doRedoExpansionChange(UndoSupport support, DataAccessContext dacx, NavTreeChange ntc, boolean recordPathControllerState) {      

    NavTree nt = dacx.getGenomeSource().getModelHierarchy();
    ExpansionChange ec = (new TreeSupport(uics_)).buildExpansionChange(false, nt);
    ec.expanded = nt.mapAllPaths(ec.expanded, ntc, false);
    ec.selected = nt.mapAPath(ec.selected, ntc, false);
    if (recordPathControllerState) {
      UserTreePathController utpc = uics_.getPathController();
      support.addEdit(new UserTreePathControllerChangeCmd(dacx, utpc.recordStateForUndo(false)));
    }
    support.addEdit(new ExpansionChangeCmd(ec));      

    return;     
  }  
  
  /***************************************************************************
  **
  ** Get Action wrapped around a newly-created flow.  No additional arguments, so these actions can be cached by FlowMeister key.
  */ 
  
  public static ModelTreeAction getAction(ResourceManager rMan, UIComponentSource uics, CmdSource cSrc, HarnessBuilder hBld, FlowMeister.TreeFlow key) {
    return (new ModelTreeAction(rMan, uics, cSrc, hBld, false, cSrc.getFloM().getControlFlow(key, null)));  
  }
  
  /***************************************************************************
  **
  ** Get Action wrapped around an existing flow. 
  */ 
  
  public static ModelTreeAction getAction(ResourceManager rMan, UIComponentSource uics, CmdSource cSrc, HarnessBuilder hBld, ControlFlow flow) {
    return (new ModelTreeAction(rMan, uics, cSrc, hBld, false, flow));  
  }
  
  /***************************************************************************
  **
  ** Answer if group node has image, map image
  */  
   
  public boolean currNodeHasGroupImage(boolean forMap) {
    NavTree nt = ddacx_.getGenomeSource().getModelHierarchy();
    TreePath tp = getTreeSelectionPath();  
    String groupNodeID = nt.getGroupNodeID(tp);
    if (groupNodeID == null) {
      return (false);
    }
    TreeNode tn = nt.nodeForNodeID(groupNodeID);
    String imKey = nt.getImageKey(tn, forMap);
    return (imKey != null);
  }
   
  /***************************************************************************
  **
  ** Answer if curr node is a group node
  */  
   
  public boolean currNodeIsGroupNode() {
    NavTree nt = ddacx_.getGenomeSource().getModelHierarchy();
    TreePath tp = getTreeSelectionPath();  
    String groupNodeID = nt.getGroupNodeID(tp);
    return (groupNodeID != null);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Resolve the path into a target and a node
  */    
  
  private PathResolution resolvePath(TreePath tp, StaticDataAccessContext dacx) {
    NavTree nt = dacx.getGenomeSource().getModelHierarchy();
    String genomeID = nt.getGenomeID(tp);
    String proxyID = nt.getDynamicProxyID(tp);
    String groupName = nt.getGroupName(tp);
    String genomeAncestorID = nt.getGenomeModelAncestorID(tp);
    Genome pathTarget = null;
    Genome pathModelAncestor = null;
    TreeNode pathNode = null;
    boolean groupNode = false;
    if (genomeID != null) {
      pathTarget = dacx.getGenomeSource().getGenome(genomeID);
      pathNode = (TreeNode)tp.getLastPathComponent();
      pathModelAncestor = pathTarget;
    } else if (proxyID != null) {
      DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(proxyID);  
      genomeID = uics_.getVTSlider().getCurrentSliderKey(dip);
      pathTarget = dacx.getGenomeSource().getGenome(genomeID);
      pathNode = (TreeNode)tp.getLastPathComponent();
      pathModelAncestor = pathTarget;
    } else if (groupName != null) {
      pathNode = (TreeNode)tp.getLastPathComponent();
      pathModelAncestor = dacx.getGenomeSource().getGenome(genomeAncestorID);
      groupNode = true;
    }

    if (pathNode != null) {
      return (new PathResolution(pathTarget, pathModelAncestor, pathNode, groupNode));
    }
    return (null);
  }

  /***************************************************************************
  **
  ** refresh tree
  */
  
  public void refreshTree() { 
    if (myTree_ != null) {
      myTree_.revalidate();
      myTree_.repaint();
    }
    return;
  }

  /***************************************************************************
  **
  ** Update nav tree names
  */
  
  public void updateNavTreeNames(ModelChangeEvent mcev, DataAccessContext dacx) { 
    String id = mcev.isProxyKey() ? mcev.getProxyKey() : mcev.getGenomeKey();
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    // NOTE: This is where nav tree names are changed to match model name change.
    navTree.nodeNameRefresh(id, dacx);
    if (myTree_ != null) {
      myTree_.revalidate();
      myTree_.repaint();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answers if we are currently displaying a dynamic model
  */
  
  public boolean isDynamic(DataAccessContext dacx) {
    TreePath tp = getTreeSelectionPath();
    String genomeID = dacx.getGenomeSource().getModelHierarchy().getGenomeID(tp);
    String proxyID = dacx.getGenomeSource().getModelHierarchy().getDynamicProxyID(tp);    
    return ((proxyID != null) || (DynamicInstanceProxy.isDynamicInstance(genomeID)));
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

 /***************************************************************************
  **
  ** Answer for path resolution
  */  
  
  private static class PathResolution {
    Genome pathTarget;
    Genome pathAncestorModel;
    TreeNode pathNode;
    boolean groupNode;
    
    PathResolution(Genome pathTarget, Genome pathAncestorModel, TreeNode pathNode, boolean groupNode) {
      this.pathTarget = pathTarget;
      this.pathAncestorModel = pathAncestorModel;
      this.pathNode = pathNode;
      this.groupNode = groupNode;
    }
  }  
   
  /***************************************************************************
  **
  ** Handles click events
  */  
  
  public class MouseHandler extends MouseAdapter {
    
    @Override
    public void mousePressed(MouseEvent me) {
      if (me.isPopupTrigger()) {
        triggerPopup(me.getX(), me.getY());
      }
      return;
    }
    
    @Override
    public void mouseClicked(MouseEvent me) {
      if (me.isPopupTrigger()) {
        triggerPopup(me.getX(), me.getY());
      }
      return;
    }    

    @Override
    public void mouseReleased(MouseEvent me) {
      if (me.isPopupTrigger()) {
        triggerPopup(me.getX(), me.getY());
      }
      return;
    }
    
    private void triggerPopup(int x, int y) {
      try {
        boolean readOnly = !uics_.getIsEditor();
        if (!readOnly) {
          TreePath tp = myTree_.getPathForLocation(x, y);
          StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_);
          PathResolution res = resolvePath(tp, dacx);
          if (res == null) {
            return ;
          }
          popupModel_ = res.pathTarget;
          popupModelAncestor_ = res.pathAncestorModel;
          popupNode_ = res.pathNode;
          
          StaticDataAccessContext dacx2 = new StaticDataAccessContext(dacx, popupModel_);
          popMenuContents_.manageActionEnables(popupNode_, dacx2);
          leafPopup_.show(myTree_, x, y);
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }     
    }
  }

  /***************************************************************************
  **
  ** Listens for popup events
  */  
  
  public class PopupHandler implements PopupMenuListener {
    
    public void popupMenuCanceled(PopupMenuEvent e) {
    }
    
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }
    
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }
  }
 
  /***************************************************************************
  **
  ** Command for model tree popups
  */
  
  public static class ModelTreeAction extends AbstractAction {     
    private static final long serialVersionUID = 1L;     
    // Used so we can build cross-platform menus and toolbars:
    private CmdSource cSrc_;
    private UIComponentSource uics_;
    private HarnessBuilder hBld_;
    private String name_;
    private String desc_;      
    private String icon_;     
    private Character mNem_;
    private Character accel_; 
    protected ControlFlow myControlFlow;

    public ModelTreeAction(ResourceManager rMan, UIComponentSource uics, CmdSource cSrc, HarnessBuilder hBld, boolean doIcon, ControlFlow theFlow) {
      cSrc_ = cSrc;
      hBld_ = hBld;
      uics_ = uics;
      myControlFlow = theFlow;
      installName(rMan, doIcon, myControlFlow.getName(), myControlFlow.getDesc(), 
                  myControlFlow.getIcon(), myControlFlow.getMnem(), myControlFlow.getAccel());
    }
    
    private void installName(ResourceManager rMan, boolean doIcon, String name, String sDesc, String icon, String mnem, String accel) {
      name_ = rMan.getString(name);
      putValue(Action.NAME, name_);
      if (doIcon) {
        desc_ = rMan.getString(sDesc);
        putValue(Action.SHORT_DESCRIPTION, desc_);
        if (icon != null) {
          URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/" + icon);  
          putValue(Action.SMALL_ICON, new ImageIcon(ugif));
          icon_ = icon;
        }
      } else {
        char mnemC = rMan.getChar(mnem);
        mNem_ = new Character(mnemC);
        putValue(Action.MNEMONIC_KEY, new Integer(mnemC)); 
      }
      if (accel != null) {
        char accelC = rMan.getChar(accel);
        accel_ = new Character(accelC);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelC, Event.CTRL_MASK, false));
      }
    }

    public String getName() {
      return (name_);
    }    
    public String getDesc() {
      return (desc_);
    }     
    public String getIcon() {
      return (icon_);       
    }
    public Character getMnem() {
      return (mNem_);        
    }
    public Character getAccel() {
      return (accel_);      
    }
    
    public void actionPerformed(ActionEvent e) {
      try {       
        HarnessBuilder.PreHarness preH = hBld_.buildHarness(myControlFlow);
        VirtualModelTree vmTree = uics_.getTree();
        DialogAndInProcessCmd.ModelTreeCmdState agis = (DialogAndInProcessCmd.ModelTreeCmdState)preH.getCmdState();
        agis.setPreload(vmTree.getPopupModel(), vmTree.getPopupModelAncestor(), vmTree.getPopupNode());
        hBld_.runHarness(preH);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }
  
    public boolean manageActionEnables(TreeNode modelNode, StaticDataAccessContext dacx) {
      XPlatModelNode.NodeKey key = dacx.getGenomeSource().getModelHierarchy().treeNodeToXPlatKey((DefaultMutableTreeNode)modelNode, dacx);
      return (myControlFlow.isTreeEnabled(key, dacx, uics_));
    }  
  }

  /***************************************************************************
  **
  ** Holds model menu contents
  */
  
  class ModelMenu {
      
    private XPlatMenu xpm_;
    private MenuSource.BuildInfo actionBifo_;
    private Map<FlowMeister.FlowKey, ControlFlow> flows_;
    
    /***************************************************************************
    **
    ** Constructor
    */

    ModelMenu() {
      if (!uics_.getIsEditor()) {
        throw new IllegalStateException();
      }
      StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_);
      MenuSource mSrc = new MenuSource(uics_, tSrc_, cSrc_);
      xpm_ = mSrc.defineEditorTreePopup(dacx);
      MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
      flows_ = mSrc.fillFlowMap(xpm_, bifo);
    }
     
    /***************************************************************************
    **
    ** Handle enabling the actions
    */

    void manageActionEnables(TreeNode modelNode, StaticDataAccessContext dacx) {
      boolean readOnly = !uics_.getIsEditor();
      if (!readOnly) {
        Iterator<AbstractAction> cavit = actionBifo_.allActions.iterator();
        while (cavit.hasNext()) {
          ModelTreeAction mta = (ModelTreeAction)cavit.next();
          mta.setEnabled(mta.manageActionEnables(modelNode, dacx));
        }
      }
      return;
    }
 
    /***************************************************************************
    **
    ** Map of enabled state for tree flows
    */

    Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> getTreeFlowsEnabled(DataAccessContext dacx) {     
      XPlatModelTree tree = dacx.getGenomeSource().getModelHierarchy().getXPlatTree(dacx);
      
      Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> retval = 
        new HashMap<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>>();
      
      XPlatModelNode xpRoot = tree.getRoot();
      getEnabledForNodeAndKids(xpRoot, dacx, retval); 
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Map of enabled state for tree flows
    */

    private void getEnabledForNode(XPlatModelNode xpNode, DataAccessContext dacx, 
                                   Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> fillMeIn) { 
      
      boolean readOnly = !uics_.getIsEditor();
      if (readOnly) {
        throw new IllegalStateException();
      }
      
      XPlatModelNode.NodeKey nKey = xpNode.getNodeKey();
      HashMap<FlowMeister.FlowKey, Boolean> retval = new HashMap<FlowMeister.FlowKey, Boolean>();
      Iterator<FlowMeister.FlowKey> akit = flows_.keySet().iterator();
      while (akit.hasNext()) {
        FlowMeister.FlowKey fk = akit.next();
        ControlFlow cf = flows_.get(fk);
        boolean enabled = cf.isTreeEnabled(nKey, dacx, uics_);
        retval.put(fk, Boolean.valueOf(enabled));        
      }
      
      fillMeIn.put(nKey, retval);
      return;
    } 
      
    /***************************************************************************
    **
    ** Map of enabled state for tree flows
    */

    private void getEnabledForNodeAndKids(XPlatModelNode xpNode, DataAccessContext dacx, 
                                          Map<XPlatModelNode.NodeKey, Map<FlowMeister.FlowKey, Boolean>> fillMeIn) {     
   
        
      getEnabledForNode(xpNode, dacx, fillMeIn);
      Iterator<XPlatModelNode> xprit = xpNode.getChildren();
      while (xprit.hasNext()) {
        XPlatModelNode kidNode = xprit.next();
        getEnabledForNodeAndKids(kidNode, dacx, fillMeIn);
      }
      return;
    } 

    /***************************************************************************
    **
    ** Fill in the given menu
    */

    void fillMenu(MenuItemTarget menuTarg) {
      boolean readOnly = !uics_.getIsEditor();
      if (!readOnly) {
        DesktopMenuFactory dmf = new DesktopMenuFactory(cSrc_, ddacx_.getRMan(), uics_, hBld_);
        actionBifo_ = new MenuSource.BuildInfo();
        dmf.buildMenuGeneral(xpm_, new DesktopMenuFactory.ActionSource(cSrc_, flows_), actionBifo_, menuTarg);
      }
      return;
    }
  }
}
