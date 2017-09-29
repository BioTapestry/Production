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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.cmd.flow.layout.UpwardSync;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutDataSource;
import org.systemsbiology.biotapestry.ui.LayoutDerivation;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.BackgroundWorkerControlManager;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.FixedJLabel;
import org.systemsbiology.biotapestry.util.FixedJTextField;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** Dialog box for planning layout propagation
*/

public class LayoutPropagationDialog extends JDialog implements TreeSelectionListener, 
                                                                BackgroundWorkerControlManager {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private LocalGenomeSource fgs_;
  private LocalLayoutSource lls_;
  private DataAccessContext rcxR_;
  private DataAccessContext dacx_;
  private GenomeSource origGs_;
  private Layout origLO_;
  private LayoutSourceTree lst_;
  private LayoutDerivation layoutDerive_;
  private ArrayList<TrueObjChoiceContent> layoutDeriveDisplay_;
  private ArrayList<ObjChoiceContent> elementsNeedingLayout_;
  private ArrayList<ObjChoiceContent> linksNeedingLayout_;  
  private JList sourceList_;
  private JList needList_;
  private JList needLinkList_;
  private JCheckBox dropFGOnlyBox_;
  private boolean dropFGOnly_;
  private JTree jtree_;
  private boolean haveResult_;
  private DefaultMutableTreeNode currTargetNode_;
  private FixedJButton buttonA_;
  private FixedJButton buttonANode_;  
  private FixedJButton buttonDD_;
  private FixedJButton buttonED_;  
  private FixedJButton buttonUp_;
  private FixedJButton buttonDown_;  
  private FixedJButton buttonPrev_;  
  private JButton buttonIn_;  
  private JButton buttonOut_; 
  private ModelViewPanel msp_;
  private int selectedLayoutDirectiveIndex_;
  private boolean calculating_;
  private boolean sliderProc_;  

  private JLabel xSliderLabel_; 
  private JLabel ySliderLabel_;    
  private JSlider xSlider_;
  private JSlider ySlider_;
  private JTextField xField_;
  private JTextField yField_; 
  
  private JLabel xScaleLabel_; 
  private JLabel yScaleLabel_;    
  private JSlider xScaleSlider_;
  private JSlider yScaleSlider_;
  private JTextField xScaleField_;
  private JTextField yScaleField_;
  
  private MySelectListener specialSelectListener_;
  private JPanel hidingPanel_;
  private CardLayout myCard_;
  
  private JCheckBox swapPadsBox_;
  private JCheckBox noPadOverlayBox_;
  
  private JComboBox overlayOptionCombo_;
  
  private BTState appState_;
  private static final long serialVersionUID = 1L;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LayoutPropagationDialog(BTState appState, DataAccessContext dacx, boolean showOverlayOptions) {     
    super(appState.getTopFrame(), appState.getRMan().getString("lpd.title"), true);
    appState_ = appState;
    haveResult_ = false;
    calculating_ = false;
    sliderProc_ = false;
    dacx_ = dacx;
        
    ResourceManager rMan = appState.getRMan();    
    setSize(1000, 800);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new BorderLayout());
    
    fgs_ = new LocalGenomeSource(new DBGenome(appState_, "foo", "bar"), new DBGenome(appState_, "foo", "bar"));
    origGs_ = dacx_.getGenomeSource();
    lls_ = new LocalLayoutSource(null, fgs_);

    GridBagConstraints gbc = new GridBagConstraints();
    rcxR_ = new DataAccessContext(null, null, false, false, 0.0,
                                 appState_.getFontMgr(), appState_.getDisplayOptMgr(), 
                                 appState_.getFontRenderContext(), fgs_, dacx_.cRes, false, 
                                 new FullGenomeHierarchyOracle(fgs_, lls_), appState_.getRMan(), 
                                 new LocalGroupSettingSource(), dacx_.wSrc, lls_,
                                 new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), 
                                 dacx_.getExpDataSrc(), dacx_.getInstructSrc()                               
      );   
    origLO_ = dacx_.lSrc.getLayoutForGenomeKey(dacx_.getDBGenome().getID());

    //
    // Get existing layout directives, if any:
    //

    LayoutDerivation existingLD = origLO_.getDerivation();
    if (existingLD == null) {
      layoutDerive_ = new LayoutDerivation();
    } else {
      layoutDerive_ = existingLD.clone();
    }
 
    JPanel leftSide = new JPanel();
    leftSide.setLayout(new GridBagLayout());
    JPanel rightSide = new JPanel();
    rightSide.setLayout(new GridBagLayout());    
        
    //
    // Selection tree:
    //
      
    JPanel treePanel = buildSelectionTreePanel();

    UiUtil.gbcSet(gbc, 0, 0, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.6);       
    leftSide.add(treePanel, gbc);
    
    //
    // List of elements needing layout:
    //   

    JPanel unresPanel = buildUnresolvedPanel();
    UiUtil.gbcSet(gbc, 0, 10, 1, 20, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    leftSide.add(unresPanel, gbc); 
    
    //
    // Layout directives list:
    //
    
    Rectangle rect = dacx_.wSrc.getWorkspace().getWorkspace();    
    JPanel directivesPanel = buildLayoutDirectivesPanel(rect);
    UiUtil.gbcSet(gbc, 0, 0, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.6);              
    rightSide.add(directivesPanel, gbc);    
        
    //
    // Layout the pad panel:
    //
    
    JPanel padPanel = buildPadPanel(showOverlayOptions);
    UiUtil.gbcSet(gbc, 0, 10, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);              
    rightSide.add(padPanel, gbc);           
    
    //
    // Build the preview panel:
    //
    
    JPanel previewPanel = buildPreviewPanel();
    UiUtil.gbcSet(gbc, 0, 11, 1, 20, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
    rightSide.add(previewPanel, gbc);

    leftSide.setMinimumSize(new Dimension(300, 700));
    leftSide.setPreferredSize(new Dimension(300, 700));    
    rightSide.setMinimumSize(new Dimension(300, 700));
    rightSide.setPreferredSize(new Dimension(700, 700));    
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSide, rightSide); 
    cp.add(sp, BorderLayout.CENTER);    
    
    //
    // Button Panel
    //
        
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            LayoutPropagationDialog.this.setVisible(false);
            LayoutPropagationDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);        
          LayoutPropagationDialog.this.setVisible(false);
          LayoutPropagationDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);    

    //
    // Build the dialog:
    //
    buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    cp.add(buttonPanel, BorderLayout.SOUTH);   
    setLocationRelativeTo(appState_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /****************************************************************************
  **
  ** disable
  */  
  
  public void disableControls() {
    myCard_.show(hidingPanel_, "Hiding");
    controlsEnabled(false);
    getContentPane().validate();
    getContentPane().repaint();
    return;
  }
  
  /****************************************************************************
  **
  ** enable
  */  
  
  public void reenableControls() {
    myCard_.show(hidingPanel_, "Showing");
    msp_.showFullModel();  // kinda bogus to go here...
    controlsEnabled(true);
    getContentPane().validate();
    return;
  }  
  
  /****************************************************************************
  **
  ** also redraw....
  */  
  
  public void redraw() {
    msp_.repaint();
    return;
  }  
   
  /***************************************************************************
  **
  ** Get the result.
  ** 
  */
  
  public LayoutDerivation getLayoutDerivation() {
    return (layoutDerive_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Called when the tree is selected
  */
  
  public void valueChanged(TreeSelectionEvent e) {
    try {
      currTargetNode_ = null;
      TreePath tp = jtree_.getSelectionPath();
      LayoutSourceNodeContents lsnc = null;
      if (tp != null) {
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)tp.getLastPathComponent();
        Object inNode = targetNode.getUserObject();
        if (inNode instanceof LayoutSourceNodeContents) {
          lsnc = (LayoutSourceNodeContents)inNode;
          currTargetNode_ = targetNode;
        }
      }
      updateAddButton();
      buttonANode_.setEnabled(currTargetNode_ != null);
      specialSelectListener_.specialClearSelection();
      calcDefinedBy();
      getContentPane().validate();
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** See if the add button is enabled
  */
  
  private void updateAddButton() {
    LayoutSourceNodeContents lsnc = null;
    if (currTargetNode_ != null) {
      Object inNode = currTargetNode_.getUserObject();
      if (inNode instanceof LayoutSourceNodeContents) {
        lsnc = (LayoutSourceNodeContents)inNode;
        LayoutDataSource check = new LayoutDataSource(lsnc.getModelID(), lsnc.getGroupID());
        buttonA_.setEnabled(!layoutDerive_.containsDirectiveModuloTransforms(check));
        getContentPane().validate();
        return;
      }
    }
    buttonA_.setEnabled(false);
    getContentPane().validate();
    return;
  }  

  /***************************************************************************
  **
  ** Handle the defined-by node selections.  Assumes we have previously cleared.
  */
  
  private void calcDefinedBy() {
    if (currTargetNode_ != null) {
      LayoutSourceNodeContents lsnc = (LayoutSourceNodeContents)currTargetNode_.getUserObject();
      LayoutDataSource lds = new LayoutDataSource(lsnc.getModelID(), lsnc.getGroupID());
      LayoutRubberStamper lrs = new LayoutRubberStamper(appState_);
      
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(lds.getModelID());
      Layout lo = dacx_.lSrc.getLayoutForGenomeKey(lds.getModelID());
      DataAccessContext rcxT = new DataAccessContext(dacx_, gi, lo);
      Set<String> defs = lrs.definedByLayout(lds, rcxT);
      int numEnl = elementsNeedingLayout_.size();
      for (int i = 0; i < numEnl; i++) {
        ObjChoiceContent occ = elementsNeedingLayout_.get(i);
        String nodeID = occ.val;
        if (defs.contains(nodeID)) {
          specialSelectListener_.specialAddSelectionInterval(i, i);
        }        
      }
    }
    return;
  }
  
  /****************************************************************************
  **
  ** The model for the linkage
  */

  public class LayoutSourceTree extends DefaultTreeModel {
    
    private HashMap<String, DefaultMutableTreeNode> parentMap_;
    private HashMap<String, SortedMap<String, List<DefaultMutableTreeNode>>> tempHold_;
    
    private static final long serialVersionUID = 1L;
    
    public LayoutSourceTree() {
      super(new DefaultMutableTreeNode());
      parentMap_ = new HashMap<String, DefaultMutableTreeNode>();
      tempHold_ = new HashMap<String, SortedMap<String, List<DefaultMutableTreeNode>>>();
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();
      String rootID = dacx_.getDBGenome().getID();
      myRoot.setUserObject(dacx_.rMan.getString("tree.FullGenome")); 
      parentMap_.put(rootID, myRoot);      
    }    

    public void addNode(String parentID, boolean isModel, String modelID, String modelName, 
                        String groupID) {
      DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
      int index = parentNode.getChildCount();  
      addNodeOrHold(parentID, isModel, modelID, modelName, groupID, index); 
      return;
    }
 
    private void addNodeOrHold(String parentID, boolean isModel, String modelID, String modelName, 
                               String groupID, int index) {
      
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
      
      //
      // Models go right in.  Group nodes are held out for sorting:
      //
      
      if (isModel) {
        parentMap_.put(modelID, newNode);
        newNode.setUserObject(modelName);
        DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
        insertNodeInto(newNode, parentNode, index);
        nodeStructureChanged(root);
      } else {
        LayoutSourceNodeContents lsnc = new LayoutSourceNodeContents(modelID, groupID);    
        newNode.setUserObject(lsnc);
        SortedMap<String, List<DefaultMutableTreeNode>> nodesToAdd = tempHold_.get(parentID);
        if (nodesToAdd == null) {
          nodesToAdd = new TreeMap<String, List<DefaultMutableTreeNode>>();
          tempHold_.put(parentID, nodesToAdd);
        }
        String ntaKey = newNode.toString();
        List<DefaultMutableTreeNode> nodesForKey = nodesToAdd.get(ntaKey);
        if (nodesForKey == null) {
          nodesForKey = new ArrayList<DefaultMutableTreeNode>();
          nodesToAdd.put(ntaKey, nodesForKey);
        }
        nodesForKey.add(newNode);
      }

      return;
    }

    public void finishTree() {
      Iterator<String> thkit = tempHold_.keySet().iterator();
      while (thkit.hasNext()) {
        String key = thkit.next();
        DefaultMutableTreeNode parentNode = parentMap_.get(key);
        SortedMap<String, List<DefaultMutableTreeNode>> nodesToAdd = tempHold_.get(key);
        Iterator<List<DefaultMutableTreeNode>> kit = nodesToAdd.values().iterator();
        int index = 0;//parentNode.getChildCount();
        while (kit.hasNext()) {
          List<DefaultMutableTreeNode> nodesForKey = kit.next();
          int numNfk = nodesForKey.size();
          for (int i = 0; i < numNfk; i++) {
            DefaultMutableTreeNode newNode = nodesForKey.get(i);
            insertNodeInto(newNode, parentNode, index++);
          }
        }
      }  
      nodeStructureChanged(root);
      return;
    }
    
    public List<TreePath> getAllPathsToNonLeaves() {
      ArrayList<TreePath> retval = new ArrayList<TreePath>();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)this.getRoot();    
      Enumeration e = root.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
        if (next.getChildCount() != 0) {  
          TreePath path = new TreePath(next.getPath());
          retval.add(path);
        }
      }
      return (retval);
    }             
  }
  
  /****************************************************************************
  **
  ** A node for the layout source tree
  */

  public class LayoutSourceNodeContents {
    private String modelID_;
    private String groupID_;    
    private String groupDisplay_;
    private String groupModelDisplay_;
    private Color groupColor_;
    
    public LayoutSourceNodeContents(String modelID, String groupID) { 
      modelID_ = modelID;
      groupID_ = groupID;
      setData();
    }
    
    public String toString() {
      return (groupDisplay_);
    }
    
    public String getGroupModelDisplay() {
      return (groupModelDisplay_);
    }

    public Color getGroupColor() {
      return (groupColor_);
    }
        
    public String getModelID() {
      return (modelID_);
    }
    
    public String getGroupID() {
      return (groupID_);
    }
    
    public void setData() {
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(modelID_);
      Layout lo = dacx_.lSrc.getLayoutForGenomeKey(modelID_);
      Group newGrp = gi.getGroup(groupID_);
      groupDisplay_ = newGrp.getInheritedDisplayName(gi);
      groupModelDisplay_ = gi.getName() + ": " + groupDisplay_;
      String baseGroupID = Group.getBaseID(groupID_);
      GroupProperties gp = lo.getGroupProperties(baseGroupID);
      groupColor_ = gp.getColor(true, dacx_.cRes);
      return;
    }
  }  
 
  /****************************************************************************
  **
  ** For rendering
  */

  public class LayoutSourceNodeRenderer extends DefaultTreeCellRenderer {
    
    private LayoutSourceLeaf myLeaf_;
    private static final long serialVersionUID = 1L;
    
    
    public LayoutSourceNodeRenderer() {
      super();
      myLeaf_ = new LayoutSourceLeaf();
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
                                                  boolean selected, boolean expanded, 
                                                  boolean leaf, int row, boolean hasFocus) {
      try {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        Object inNode = node.getUserObject();
        if (inNode instanceof LayoutSourceNodeContents) {
          LayoutSourceNodeContents lsnc = (LayoutSourceNodeContents)inNode;
          myLeaf_.setGroup(lsnc.toString(), lsnc.getGroupColor());
          myLeaf_.setSelected(selected);
          return (myLeaf_); 
        } else {
          return (super.getTreeCellRendererComponent(tree, value, selected, expanded, 
                                                     leaf, row, hasFocus));
        }
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }  
  }

  /****************************************************************************
  **
  ** Component returned for a leaf
  */  
  
  public class LayoutSourceLeaf extends JPanel {

    private JLabel leafLabel_;
    private JPanel leafPanel_;
    private static final long serialVersionUID = 1L;
   
    public LayoutSourceLeaf() {
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
    
      leafLabel_ = new JLabel();
      leafPanel_ = new JPanel();
      leafPanel_.setLayout(new GridLayout(1, 1));
      leafPanel_.add(leafLabel_);
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 1, 1, 1, 1, UiUtil.CEN, 1.0, 1.0);       
      add(leafPanel_, gbc);
      
      setBackground(Color.white);
    }
    
    public void setGroup(String name, Color color) {
      leafLabel_.setText(name);
      leafPanel_.setBackground(color);
      return;
    }
    
    public void setSelected(boolean selected) {
      setBackground((selected) ? Color.blue : Color.white);
      return;      
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /****************************************************************************
  **
  ** Signal need for preview refresh
  */  
  
  private void makePreviewStale() {
    buttonPrev_.setForeground(Color.red);
    getContentPane().validate();
    getContentPane().repaint();
    return;
  }  
   
  /***************************************************************************
  **
  ** Build the tree model
  ** 
  */    
  
  private LayoutSourceTree buildModelTree() {  
  
    LayoutSourceTree retval = new LayoutSourceTree();   
    
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    String rootID = dacx_.getDBGenome().getID();
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      GenomeInstance gi = (GenomeInstance)origGs_.getGenome(gkey);
      if (gi instanceof DynamicGenomeInstance) {
        continue;
      } else {
        GenomeInstance parent = gi.getVfgParent();
        if (parent != null) {
          continue;
        }
        retval.addNode(rootID, true, gkey, gi.getName(), null);

        Iterator<Group> git = gi.getGroupIterator();
        while (git.hasNext()) {
          Group group = git.next();
          if (group.isASubset(gi)) {
            continue;
          }
          String groupID = group.getID();
          retval.addNode(gkey, false, gkey, null, groupID);
        }
      }
    }
    retval.finishTree();
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      // layoutDerive_ has already been built, but clean up force unique:
      if (!layoutDerive_.getSwitchPads()) {
        layoutDerive_.setForceUnique(false);
      }
      haveResult_ = true;
    } else {
      layoutDerive_ = null;
      haveResult_ = false;      
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Full expansion
  */
  
  private void expandFullTree() {
    List<TreePath> nonleafPaths = lst_.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      jtree_.expandPath(tp);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Stock the list of nodes requiring definition
  */
  
  private void stockUndefinedLists(List<ObjChoiceContent> nodeList, List<ObjChoiceContent> linkList, 
                                   boolean dropFGOnly) {
    nodeList.clear();
    linkList.clear();
    
    LayoutRubberStamper lrs = new LayoutRubberStamper(appState_);

    HashSet<String> undefNodes = new HashSet<String>();
    HashSet<String> undefLinks = new HashSet<String>();
    
    lrs.undefinedLayout(dacx_, layoutDerive_, undefNodes, undefLinks, dropFGOnly);  

    // Issue #187 has to work at all levels of model hierarchy
    Genome rootG = dacx_.getGenomeSource().getGenome();
    Iterator<String> undit = undefNodes.iterator();
    while (undit.hasNext()) {
      String nodeID = undit.next();
      Node node = rootG.getNode(nodeID);
      String display = node.getDisplayString(rootG, false);
      nodeList.add(new ObjChoiceContent(display, nodeID)); 
    }
    Collections.sort(nodeList);
    
    Iterator<String> undlit = undefLinks.iterator();
    while (undlit.hasNext()) {
      String linkID = undlit.next();
      Linkage link = rootG.getLinkage(linkID);
      String display = link.getDisplayString(rootG, false);
      linkList.add(new ObjChoiceContent(display, linkID)); 
    }
    Collections.sort(linkList);
    
    return;
  }
  
  /***************************************************************************
  **
  ** Stock the layout definition display list
  */
  
  private ArrayList<TrueObjChoiceContent> stockLDSDisplayList() {
    ArrayList<TrueObjChoiceContent> retval = new ArrayList<TrueObjChoiceContent>();
    int numLds = layoutDerive_.numDirectives();
    for (int i = 0; i < numLds; i++) {
      LayoutDataSource lds = layoutDerive_.getDirective(i);
      String display = lds.getDisplayString(origGs_);
      retval.add(new TrueObjChoiceContent(display, lds)); 
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Update the link display
  ** 
  */

  private void updatePreviewDisplay() { 
    
    DBGenome dbg = dacx_.getDBGenome();
    buttonPrev_.setForeground(Color.black);
    
    fgs_.dropAll();
    DBGenome dbgCopy = dbg.clone();
    fgs_.setGenome(dbgCopy);
    fgs_.setGenome(dbg.getID(), dbgCopy);
    
    Layout lorCopy = new Layout(origLO_, origLO_.getID(), dbg.getID(), null);
    rcxR_.setGenome(dbg);
    lls_.dropAll();
    lls_.setRootLayout(lorCopy);
    rcxR_.setLayout(lorCopy);
    Set<String> modDep = layoutDerive_.getModelDependencies();
    Iterator<String> mdit = modDep.iterator();
    while (mdit.hasNext()) {
      String mKey = mdit.next();
      Layout log = dacx_.lSrc.getLayoutForGenomeKey(mKey);
      lls_.addInstanceLayout(log);
      fgs_.setGenome(mKey, dacx_.getGenomeSource().getGenome(mKey));
    }
             
    UpwardSync.UpwardLayoutSynch uls = new UpwardSync.UpwardLayoutSynch();   
    uls.synchronizeRootLayoutFromChildrenAsynch(appState_, rcxR_, null, layoutDerive_, msp_.getRawCenterPoint(), this);
    if (uls.wasCancelled()) {
      lls_.setRootLayout(new Layout(origLO_));
    }
    
    msp_.repaint();
    return;
  }

  
  /***************************************************************************
  **
  ** Build the selection tree
  ** 
  */

  private JPanel buildSelectionTreePanel() {
    ResourceManager rMan = dacx_.rMan;
    GridBagConstraints gbc = new GridBagConstraints();
  
    //
    // Selection tree:
    //
      
    JPanel treePanel = new JPanel();
    treePanel.setBorder(new EtchedBorder());
    treePanel.setLayout(new GridBagLayout());    
    
    JLabel selectLab = new JLabel(rMan.getString("lpd.select"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    treePanel.add(selectLab, gbc);
    
    lst_ = buildModelTree();    
    jtree_ = new JTree(lst_);    
    JScrollPane jspt = new JScrollPane(jtree_);
    jtree_.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    jtree_.setCellRenderer(new LayoutSourceNodeRenderer());
    jtree_.addTreeSelectionListener(this);
    expandFullTree();    
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);       
    treePanel.add(jspt, gbc);    
  
    buttonA_ = new FixedJButton(rMan.getString("dialogs.add"));
    buttonA_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          addLayoutElement(); 
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonA_.setEnabled(false);
    
    buttonANode_ = new FixedJButton(rMan.getString("lpd.addANode"));
    buttonANode_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          addOneNodeLayoutElement(); 
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonANode_.setEnabled(false);
    
    Box abuttonPanel = Box.createHorizontalBox();
    abuttonPanel.add(Box.createHorizontalGlue()); 
    abuttonPanel.add(buttonA_);
    abuttonPanel.add(Box.createHorizontalStrut(10)); 
    abuttonPanel.add(buttonANode_);
    abuttonPanel.add(Box.createHorizontalGlue());       
    
    UiUtil.gbcSet(gbc, 0, 11, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);       
    treePanel.add(abuttonPanel, gbc); 
    return (treePanel);
  }
  
  /***************************************************************************
  **
  ** Build the layout directives
  ** 
  */

  private JPanel buildLayoutDirectivesPanel(Rectangle workspace) {
    ResourceManager rMan = dacx_.rMan;   
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel directivesPanel = new JPanel();
    directivesPanel.setBorder(new EtchedBorder());
    directivesPanel.setLayout(new GridLayout(1, 2)); 
    
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new GridBagLayout());    
        
    JLabel lab2 = new JLabel(rMan.getString("lpd.appliedInOrder"));
    UiUtil.gbcSet(gbc, 0, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    listPanel.add(lab2, gbc);

    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_ = new JList(layoutDeriveDisplay_.toArray());
    JScrollPane jsp2 = new JScrollPane(sourceList_);
    UiUtil.gbcSet(gbc, 0, 1, 2, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);       
    listPanel.add(jsp2, gbc);
    sourceList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    sourceList_.addListSelectionListener(new SelectionTracker());
    
    buttonDD_ = new FixedJButton(rMan.getString("dialogs.delete"));
    buttonDD_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          deleteLayoutElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonDD_.setEnabled(false);
    
    buttonED_ = new FixedJButton(rMan.getString("dialogs.edit"));
    buttonED_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          editLayoutElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonED_.setEnabled(false);
    
    buttonUp_ = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
    buttonUp_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bumpUpLayoutElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonUp_.setEnabled(false);
    
    buttonDown_ = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
    buttonDown_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bumpDownLayoutElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonDown_.setEnabled(false);
        
    Box dbuttonPanel = Box.createHorizontalBox();
    dbuttonPanel.add(Box.createHorizontalGlue()); 
    dbuttonPanel.add(buttonDD_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonED_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonUp_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonDown_);
    dbuttonPanel.add(Box.createHorizontalGlue());       
    
    UiUtil.gbcSet(gbc, 0, 11, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);       
    listPanel.add(dbuttonPanel, gbc);
    
    directivesPanel.add(listPanel);
     
    JPanel sliderPanel = new JPanel();
    sliderPanel.setLayout(new GridBagLayout());
    
    JLabel lab3 = new JLabel(rMan.getString("lpd.sliders"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    sliderPanel.add(lab3, gbc);

    Map labels = buildSliderPanelLabels();
    JPanel sl1 = buildSliderPanel(true, false, workspace, (JLabel)labels.get("lpd.slideX"));
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 0, 5, UiUtil.CEN, 1.0, 0.0);       
    sliderPanel.add(sl1, gbc);     
    
    JPanel sl2 = buildSliderPanel(false, false, workspace, (JLabel)labels.get("lpd.slideY"));
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 0, 5, UiUtil.CEN, 1.0, 0.0);       
    sliderPanel.add(sl2, gbc);
    
    JPanel sl3 = buildSliderPanel(true, true, workspace, (JLabel)labels.get("lpd.slideScaleX"));
    UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 0, 5, UiUtil.CEN, 1.0, 0.0);       
    sliderPanel.add(sl3, gbc);     
    
    JPanel sl4 = buildSliderPanel(false, true, workspace, (JLabel)labels.get("lpd.slideScaleY"));
    UiUtil.gbcSet(gbc, 0, 4, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    sliderPanel.add(sl4, gbc);  
    
    JPanel sliderMetaPanel = new JPanel();
    sliderMetaPanel.setLayout(new BorderLayout());   
    sliderMetaPanel.add(sliderPanel, BorderLayout.NORTH);    
    
    directivesPanel.add(sliderMetaPanel);
   
    selectedLayoutDirectiveIndex_ = -1;
   
    return (directivesPanel);
  }
  
  /***************************************************************************
  **
  ** Build the layout pad panel
  ** 
  */

  private JPanel buildPadPanel(boolean showOverlayOptions) {
    ResourceManager rMan = dacx_.rMan; 
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel padPanel = new JPanel();
    padPanel.setBorder(new EtchedBorder());
    padPanel.setLayout(new GridBagLayout());
        
    swapPadsBox_ = new JCheckBox(rMan.getString("lpd.swapPads"));
    swapPadsBox_.setSelected(layoutDerive_.getSwitchPads());
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    padPanel.add(swapPadsBox_, gbc);
    swapPadsBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          layoutDerive_.setSwitchPads(swapPadsBox_.isSelected());
          syncUniqBox();
          makePreviewStale();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });   
    
    noPadOverlayBox_ = new JCheckBox(rMan.getString("lpd.uniquePads"));
    noPadOverlayBox_.setSelected(layoutDerive_.getForceUnique());
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    padPanel.add(noPadOverlayBox_, gbc);         
    noPadOverlayBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          layoutDerive_.setForceUnique(noPadOverlayBox_.isSelected());
          makePreviewStale();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    if (showOverlayOptions) {
      JLabel overlayLabel = new JLabel(rMan.getString("layoutParam.overlayOptions"));
      Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(appState_);
      overlayOptionCombo_ = new JComboBox(relayoutChoices);
      overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(appState_, layoutDerive_.getOverlayOption()));
      overlayOptionCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            layoutDerive_.setOverlayOption(((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val);
            makePreviewStale();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });

      UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 25, UiUtil.E, 1.0, 0.0);    
      padPanel.add(overlayLabel, gbc);
      UiUtil.gbcSet(gbc, 1, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 25, UiUtil.CEN, 1.0, 0.0);    
      padPanel.add(overlayOptionCombo_, gbc);    
    }
   
    selectedLayoutDirectiveIndex_ = -1;
   
    return (padPanel);
  }
  
  /***************************************************************************
  **
  ** Keep uniq box in sync
  ** 
  */

  private void syncUniqBox() {
    noPadOverlayBox_.setEnabled(layoutDerive_.getSwitchPads());
    getContentPane().validate();
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Build the unresolved panel
  ** 
  */

  private JPanel buildUnresolvedPanel() {
    ResourceManager rMan = dacx_.rMan;
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel unresPanel = new JPanel();
    unresPanel.setBorder(new EtchedBorder());
    unresPanel.setLayout(new GridBagLayout());    
    
    JLabel lab = new JLabel(rMan.getString("lpd.need"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    unresPanel.add(lab, gbc);
    
    dropFGOnly_ = true;
    elementsNeedingLayout_ = new ArrayList<ObjChoiceContent>(); 
    linksNeedingLayout_ = new ArrayList<ObjChoiceContent>();
    stockUndefinedLists(elementsNeedingLayout_, linksNeedingLayout_, dropFGOnly_);
    
    needList_ = new JList(elementsNeedingLayout_.toArray());
    specialSelectListener_ = new MySelectListener();
    needList_.setSelectionModel(specialSelectListener_);
    needList_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);    
    JScrollPane jsp = new JScrollPane(needList_);
    UiUtil.gbcSet(gbc, 0, 1, 1, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    unresPanel.add(jsp, gbc);
    
    JLabel lab2 = new JLabel(rMan.getString("lpd.needLinks"));
    UiUtil.gbcSet(gbc, 0, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    unresPanel.add(lab2, gbc);
    
    needLinkList_ = new JList(linksNeedingLayout_.toArray());
    needLinkList_.setSelectionModel(new MySelectListener());
    needLinkList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);    
    JScrollPane jsp2 = new JScrollPane(needLinkList_);
    UiUtil.gbcSet(gbc, 0, 7, 1, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    unresPanel.add(jsp2, gbc);    
       
    
    dropFGOnlyBox_ = new JCheckBox(rMan.getString("lpd.dropFGOnly"));
    dropFGOnlyBox_.setSelected(dropFGOnly_);
    UiUtil.gbcSet(gbc, 0, 12, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 0.0, 0.0);       
    unresPanel.add(dropFGOnlyBox_, gbc); 
    
    dropFGOnlyBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          dropFGOnly_ = dropFGOnlyBox_.isSelected();
          maintainUndefinedLists();
          getContentPane().validate();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     

    return (unresPanel);
  }
  
  /***************************************************************************
  **
  ** Build the preview panel
  ** 
  */

  private JPanel buildPreviewPanel() {
    ResourceManager rMan = dacx_.rMan;
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel previewPanel = new JPanel();
    previewPanel.setBorder(new EtchedBorder());
    previewPanel.setLayout(new GridBagLayout());

    JLabel prevLab = new JLabel(rMan.getString("lpd.preview"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    previewPanel.add(prevLab, gbc);
       
    msp_ = new ModelViewPanel(appState_, rcxR_);
 
    msp_.setMinimumSize(new Dimension(200, 300));
    msp_.setPreferredSize(new Dimension(200, 300));
    JScrollPane jspm = new JScrollPane(msp_);
    jspm.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jspm.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    msp_.registerScrollPane(jspm);
    
    hidingPanel_ = new JPanel();
    myCard_ = new CardLayout();
    hidingPanel_.setLayout(myCard_);
    hidingPanel_.add(jspm, "Showing");
    JPanel blankPanel = new JPanel();
    blankPanel.setBackground(Color.white);
    hidingPanel_.add(blankPanel, "Hiding");    
      
    UiUtil.gbcSet(gbc, 0, 1, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    previewPanel.add(hidingPanel_, gbc);
    
    buttonOut_ = new FixedJButton(rMan.getString("lpd.zoomOut"));
    buttonOut_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          updateZoom('-');
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    buttonIn_ = new FixedJButton(rMan.getString("lpd.zoomIn"));
    buttonIn_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          updateZoom('+');
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
 
    buttonPrev_ = new FixedJButton(rMan.getString("lpd.refreshPreview"));
    buttonPrev_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          updatePreviewDisplay();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    
    Box dbuttonPanel = Box.createHorizontalBox();
    dbuttonPanel.add(Box.createHorizontalStrut(10)); 
    dbuttonPanel.add(buttonOut_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonIn_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));
    dbuttonPanel.add(buttonPrev_);
    dbuttonPanel.add(Box.createHorizontalStrut(10));       
    
    UiUtil.gbcSet(gbc, 0, 11, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);       
    previewPanel.add(dbuttonPanel, gbc); 
    
    return (previewPanel);
  }
  
  /***************************************************************************
  **
  ** Add a new element to the list
  */
  
  void addLayoutElement() {
    if (currTargetNode_ == null) {
      throw new IllegalStateException();
    }
    Object inNode = currTargetNode_.getUserObject();
    LayoutSourceNodeContents lsnc = (LayoutSourceNodeContents)inNode;
    layoutDerive_.addDirective(new LayoutDataSource(lsnc.getModelID(), lsnc.getGroupID()));
    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    sourceList_.getSelectionModel().setSelectionInterval(layoutDeriveDisplay_.size() - 1, 
                                                         layoutDeriveDisplay_.size() - 1);
    updateAddButton();
    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a new element to the list
  */
  
  void addOneNodeLayoutElement() {
    if (currTargetNode_ == null) {
      throw new IllegalStateException();
    }
    Object inNode = currTargetNode_.getUserObject();
    LayoutSourceNodeContents lsnc = (LayoutSourceNodeContents)inNode;
    LayoutDataSource master = new LayoutDataSource(lsnc.getModelID(), lsnc.getGroupID());
    
    PickNodesForLayoutDialog pnfl = new PickNodesForLayoutDialog(appState_, master, new DataAccessContext(appState_));
    pnfl.setVisible(true);
    
    if (!pnfl.haveResult()) {
      return;
    }
    
    LayoutDataSource noded = pnfl.getLDSWithNodes();
    layoutDerive_.addDirective(noded);
    layoutDeriveDisplay_ = stockLDSDisplayList();
        
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    sourceList_.getSelectionModel().setSelectionInterval(layoutDeriveDisplay_.size() - 1, 
                                                         layoutDeriveDisplay_.size() - 1);
    
    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();
    return;
  } 
  
  /***************************************************************************
  **
  ** Move up an element in the layout directives list
  */
  
  void bumpUpLayoutElement() {
    if (selectedLayoutDirectiveIndex_ <= 0) {
      throw new IllegalStateException();
    }
    calculating_ = true;
    layoutDerive_.bumpDirectiveUp(selectedLayoutDirectiveIndex_);
    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    selectedLayoutDirectiveIndex_--;
    calculating_ = false;
    sourceList_.getSelectionModel().setSelectionInterval(selectedLayoutDirectiveIndex_, 
                                                         selectedLayoutDirectiveIndex_);

    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();    
    return;
  }  
  
  /***************************************************************************
  **
  ** Move down an element in the layout directives list
  */
  
  void bumpDownLayoutElement() {
    int maxRow = sourceList_.getModel().getSize() - 1;   
    if (selectedLayoutDirectiveIndex_ >= maxRow) {
      throw new IllegalStateException();
    }
    calculating_ = true;
    layoutDerive_.bumpDirectiveDown(selectedLayoutDirectiveIndex_);
    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    selectedLayoutDirectiveIndex_++;
    calculating_ = false;
    sourceList_.getSelectionModel().setSelectionInterval(selectedLayoutDirectiveIndex_, 
                                                         selectedLayoutDirectiveIndex_);
    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();    
    return;
  }
  
  /***************************************************************************
  **
  ** Delete the selected element in the layout directives list
  */
  
  void deleteLayoutElement() {
    if (selectedLayoutDirectiveIndex_ == -1) {
      throw new IllegalStateException();
    }
    calculating_ = true;
    layoutDerive_.removeDirective(selectedLayoutDirectiveIndex_);
    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    calculating_ = false;
    int maxRow = sourceList_.getModel().getSize() - 1;
    if (maxRow == -1) {
      selectedLayoutDirectiveIndex_ = -1;
      buttonDD_.setEnabled(false);
      buttonED_.setEnabled(false);      
      clearOutSliders();
    } else {
      if (maxRow == 0) {
        selectedLayoutDirectiveIndex_ = 0;
      } else if (selectedLayoutDirectiveIndex_ >= maxRow) {
        selectedLayoutDirectiveIndex_ = maxRow;
      }
      sourceList_.getSelectionModel().setSelectionInterval(selectedLayoutDirectiveIndex_, 
                                                           selectedLayoutDirectiveIndex_);
    }
    updateAddButton();
    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();
   
    return;
  } 
  
  /***************************************************************************
  **
  ** Edit an element in the list
  */
  
  void editLayoutElement() {
    if (selectedLayoutDirectiveIndex_ == -1) {
      throw new IllegalStateException();
    }
    
    LayoutDataSource lds = layoutDerive_.getDirective(selectedLayoutDirectiveIndex_);
    LayoutDataSource ldsCopy = lds.clone();
    PickNodesForLayoutDialog pnfl = new PickNodesForLayoutDialog(appState_, ldsCopy, new DataAccessContext(appState_));
    pnfl.setVisible(true);   
    if (!pnfl.haveResult()) {
      return;
    }   
    LayoutDataSource noded = pnfl.getLDSWithNodes();    
 
    calculating_ = true;
    layoutDerive_.replaceDirective(selectedLayoutDirectiveIndex_, noded);    
    layoutDeriveDisplay_ = stockLDSDisplayList();
    sourceList_.setListData(layoutDeriveDisplay_.toArray());
    calculating_ = false;
    sourceList_.getSelectionModel().setSelectionInterval(selectedLayoutDirectiveIndex_, 
                                                         selectedLayoutDirectiveIndex_);    
    updateAddButton();
    maintainUndefinedLists();
    makePreviewStale();
    getContentPane().validate();
    return;
  }   

  /***************************************************************************
  **
  ** Handle the undefined lists
  */
  
  void maintainUndefinedLists() {
    specialSelectListener_.specialClearSelection();    
    stockUndefinedLists(elementsNeedingLayout_, linksNeedingLayout_, dropFGOnly_);    
    needList_.setListData(elementsNeedingLayout_.toArray());
    needLinkList_.setListData(linksNeedingLayout_.toArray()); 
    calcDefinedBy();
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle slider clearing
  */
  
  void clearOutSliders() {
    xSliderLabel_.setEnabled(false); 
    ySliderLabel_.setEnabled(false);    
    xScaleLabel_.setEnabled(false); 
    yScaleLabel_.setEnabled(false);                    
    xField_.setEnabled(false);
    xSlider_.setEnabled(false);
    yField_.setEnabled(false);
    ySlider_.setEnabled(false);
    xScaleField_.setEnabled(false);
    xScaleSlider_.setEnabled(false);
    yScaleField_.setEnabled(false);
    yScaleSlider_.setEnabled(false);

    sliderProc_ = true;
    xField_.setText("");
    xSlider_.setValue(0);
    yField_.setText("");
    ySlider_.setValue(0);
    xScaleField_.setText("");
    xScaleSlider_.setValue(0);
    yScaleField_.setText("");
    yScaleSlider_.setValue(0);                           
    sliderProc_ = false;       
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle slider stocking
  */
  
  void stockSliders(LayoutDataSource lds) {
    xSliderLabel_.setEnabled(true); 
    ySliderLabel_.setEnabled(true);    
    xScaleLabel_.setEnabled(true); 
    yScaleLabel_.setEnabled(true);                    
    xField_.setEnabled(true);
    xSlider_.setEnabled(true);
    yField_.setEnabled(true);
    ySlider_.setEnabled(true);
    xScaleField_.setEnabled(true);
    xScaleSlider_.setEnabled(true);
    yScaleField_.setEnabled(true);
    yScaleSlider_.setEnabled(true);

    sliderProc_ = true;
    xField_.setText(Integer.toString(lds.getXOffset()));
    xSlider_.setValue(lds.getXOffset());
    yField_.setText(Integer.toString(lds.getYOffset()));
    ySlider_.setValue(lds.getYOffset());
    xScaleField_.setText(Integer.toString(lds.getXScale()));
    xScaleSlider_.setValue(lds.getXScale());
    yScaleField_.setText(Integer.toString(lds.getYScale()));
    yScaleSlider_.setValue(lds.getYScale());
    sliderProc_ = false;    
    return;
  }
  
  /***************************************************************************
  **
  ** Update zoom state
  */ 
  
    
  void updateZoom(char sign) {
    try {
      msp_.getZoomController().bumpZoomWrapper(sign);
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }

 /***************************************************************************
  **
  ** Called when the slider moves
  */
 
  void updateSlider(JSlider slider) {
    try {
      if (sliderProc_) {
        return;
      }      
      if (selectedLayoutDirectiveIndex_ == -1) {
        return;
      }
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)layoutDeriveDisplay_.get(selectedLayoutDirectiveIndex_);
      // This is a ref to the copy held inside the Layout derivation:
      LayoutDataSource lds = (LayoutDataSource)tocc.val;
      sliderProc_ = true;
      int value = slider.getValue();            
      if (slider == xSlider_) {
        lds.setXOffset(value);    
        xField_.setText(Integer.toString(value));
      } else if (slider == ySlider_) {
        lds.setYOffset(value);
        yField_.setText(Integer.toString(value));
      } else if (slider == xScaleSlider_) {
        lds.setXScale(value);
        this.xScaleField_.setText(Integer.toString(value));            
      } else if (slider == yScaleSlider_) {
        lds.setYScale(value);
        this.yScaleField_.setText(Integer.toString(value));        
      } else {
        throw new IllegalArgumentException();
      }
      maintainUndefinedLists();
      makePreviewStale();
    } finally {
      sliderProc_ = false;
    }
    return;
  }

  /***************************************************************************
  **
  ** Build a slider
  */
  
  private JSlider buildSlider(int min, int max, int minorTic, int start) {
    JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, start);
    slider.setMinorTickSpacing(minorTic);  
    slider.setPaintTicks(false);
    slider.setPaintLabels(false);
    slider.setSnapToTicks(true);
    slider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        try {
          JSlider slider = (JSlider)ev.getSource();
          updateSlider(slider);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    return (slider);
  } 

  /***************************************************************************
  **
  ** Build a slider panel
  */
  
  private JPanel buildSliderPanel(boolean forX, boolean forScale, Rectangle workspace, JLabel myLabel) {  
    JPanel sliderPanel = new JPanel();
    sliderPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    String label;
    int min;
    int max; 
    int minorTic; 
    int start;

    if (forScale) {
      label = (forX) ? "lpd.slideScaleX" : "lpd.slideScaleY";
      min = 0;
      max = 100; 
      minorTic = 1; 
      start = 0;
    } else {
      double workWidth = workspace.getWidth() / 2.0;
      int workWidthInt = (int)UiUtil.forceToGridValue(workWidth, UiUtil.GRID_SIZE);
      double workHeight = workspace.getHeight() / 2.0;
      int workHeightInt = (int)UiUtil.forceToGridValue(workHeight, UiUtil.GRID_SIZE);      
      if (forX) {
        label = "lpd.slideX";    
        min = -workWidthInt;
        max = workWidthInt; 
        minorTic = UiUtil.GRID_SIZE_INT; 
        start = 0;                
      } else {
        label = "lpd.slideY";
        min = -workHeightInt;
        max = workHeightInt; 
        minorTic = UiUtil.GRID_SIZE_INT; 
        start = 0;                
      }
    }
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 0, 5, 0, 5, UiUtil.E, 0.0, 0.0);       
    sliderPanel.add(myLabel, gbc);    
    
    JSlider mySlider = buildSlider(min, max, minorTic, start);
    FixedJTextField myField = new FixedJTextField();    
    myField.setPreferredSize(new Dimension(60, myField.getPreferredSize().height));

    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 0, 5, 0, 5, UiUtil.CEN, 1.0, 0.0);       
    sliderPanel.add(mySlider, gbc);    
        
    UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.NONE, 0, 0, 0, 5, 0, 5, UiUtil.W, 0.0, 0.0);       
    sliderPanel.add(myField, gbc);
    
    mySlider.setEnabled(false); 
    myField.setEnabled(false);    
    myLabel.setEnabled(false);
    
    FieldListener pl2 = new FieldListener(mySlider, myField, min, max, minorTic, start);
    myField.addActionListener(pl2);
    myField.addFocusListener(pl2);

    if (forScale) {       
      if (forX) {
        xScaleLabel_ = myLabel;
        xScaleField_ = myField;
        xScaleSlider_ = mySlider;
      } else {
        yScaleLabel_ = myLabel;
        yScaleField_ = myField;
        yScaleSlider_ = mySlider;
      }
    } else {
      if (forX) {
        xSliderLabel_ = myLabel;
        xField_ = myField;
        xSlider_ = mySlider;
      } else {
        ySliderLabel_ = myLabel;
        yField_ = myField;
        ySlider_ = mySlider;
      }  
    }
      
    return (sliderPanel);
  }
  
  /***************************************************************************
  **
  ** Build a slider panel labels
  */
  
  private Map<String, FixedJLabel> buildSliderPanelLabels() {  
    ResourceManager rMan = appState_.getRMan();
        
    HashMap<String, FixedJLabel> retval = new HashMap<String, FixedJLabel>();    
    int maxWidth = Integer.MIN_VALUE;
    String[] keys = new String[] {"lpd.slideScaleX", "lpd.slideScaleY", "lpd.slideX", "lpd.slideY"};
     
    for (int i = 0; i < keys.length; i++) {
      String key = keys[i];
      FixedJLabel label = new FixedJLabel(rMan.getString(key), JLabel.RIGHT);
      Dimension ps = label.getPreferredSize();
      if (ps.width > maxWidth) {
        maxWidth = ps.width;
      }
      retval.put(key, label);
    }
    
    for (int i = 0; i < keys.length; i++) {
      String key = keys[i];
      FixedJLabel label = retval.get(key);
      label.setPreferredSize(new Dimension(maxWidth, label.getPreferredSize().height));
    }    
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Process a new size value from a text field
  */
  
  private void roundVal(JSlider mySlider, JTextField myField, int min, int max, int minorTic, int start) {
   
    if (sliderProc_) {
      return;
    }
    sliderProc_ = true;    
    try {

      String newValStr = myField.getText().trim();
      int parsedVal;
      if (!newValStr.equals("")) {
        try {
          parsedVal = Integer.parseInt(newValStr);
          if (parsedVal > max) {
            parsedVal = max;
          } else if (parsedVal < min) {
            parsedVal = min;
          } else {
            parsedVal = (int)UiUtil.forceToGridValue(parsedVal, minorTic);
          }
        } catch (NumberFormatException nfe) {
          parsedVal = mySlider.getValue();
        }
      } else {
        parsedVal = start;
      }
      myField.setText(Integer.toString(parsedVal));
      mySlider.setValue(parsedVal);
      if (selectedLayoutDirectiveIndex_ == -1) {
        return;
      }
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)layoutDeriveDisplay_.get(selectedLayoutDirectiveIndex_);
      // This is a ref to the copy held inside the Layout derivation:
      LayoutDataSource lds = (LayoutDataSource)tocc.val;
      if (mySlider == xSlider_) {
        lds.setXOffset(parsedVal);    
      } else if (mySlider == ySlider_) {
        lds.setYOffset(parsedVal);
      } else if (mySlider == xScaleSlider_) {
        lds.setXScale(parsedVal);
      } else if (mySlider == yScaleSlider_) {
        lds.setYScale(parsedVal);
      } else {
        throw new IllegalArgumentException();
      }
      maintainUndefinedLists();
      makePreviewStale();
    } finally {
      sliderProc_ = false;
    }
    return;
  } 
  
  /****************************************************************************
  **
  ** Handle controls
  */  
  
  private void controlsEnabled(boolean enabled) { 
    
    //
    // These always track:
    //
    
    sourceList_.setEnabled(enabled);
    needList_.setEnabled(enabled);
    jtree_.setEnabled(enabled);
    buttonPrev_.setEnabled(enabled);
    buttonIn_.setEnabled(enabled);  
    buttonOut_.setEnabled(enabled);
    needLinkList_.setEnabled(enabled);  
    dropFGOnlyBox_.setEnabled(enabled);
    swapPadsBox_.setEnabled(enabled);


    //
    // These depend on having a selected row:
    //
    
    boolean qualifiedEnabled;
    if (!enabled) {
      qualifiedEnabled = false;
    } else {
      boolean haveARow = (selectedLayoutDirectiveIndex_ != -1);      
      qualifiedEnabled = haveARow;
    }
    
    xSliderLabel_.setEnabled(qualifiedEnabled); 
    ySliderLabel_.setEnabled(qualifiedEnabled);    
    xSlider_.setEnabled(qualifiedEnabled);
    ySlider_.setEnabled(qualifiedEnabled);
    xField_.setEnabled(qualifiedEnabled);
    yField_.setEnabled(qualifiedEnabled);   
    xScaleLabel_.setEnabled(qualifiedEnabled); 
    yScaleLabel_.setEnabled(qualifiedEnabled);    
    xScaleSlider_.setEnabled(qualifiedEnabled);
    yScaleSlider_.setEnabled(qualifiedEnabled);
    xScaleField_.setEnabled(qualifiedEnabled);
    yScaleField_.setEnabled(qualifiedEnabled);
    buttonDD_.setEnabled(qualifiedEnabled);

    
    //
    // These depend on which row is selected:
    //    
    
    int maxRow = sourceList_.getModel().getSize() - 1;
    boolean canLower = (qualifiedEnabled && (selectedLayoutDirectiveIndex_ < maxRow));        
    buttonDown_.setEnabled(canLower);
    boolean canRaise = (qualifiedEnabled && (selectedLayoutDirectiveIndex_ > 0));        
    buttonUp_.setEnabled(canRaise);
    
    //
    // Depend on which tree element is selected:
    //
    
    buttonA_.setEnabled(enabled && (currTargetNode_ != null));
    buttonANode_.setEnabled(enabled && (currTargetNode_ != null));
    
    //
    // Must be node specific:
    //
    
    if (qualifiedEnabled) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)layoutDeriveDisplay_.get(selectedLayoutDirectiveIndex_);
      LayoutDataSource lds = (LayoutDataSource)tocc.val;
      buttonED_.setEnabled(lds.isNodeSpecific()); 
    } else {
      buttonED_.setEnabled(false); 
    }
    
    //
    // Uniq box depends on swap box:
    //
    
    if (enabled) {
      syncUniqBox();
    } else {
      noPadOverlayBox_.setEnabled(false);
    }
   
    return;
  }      
  
  
  
  /***************************************************************************
  **
  ** Used to track selected layout directives
  */
  
  private class SelectionTracker implements ListSelectionListener {
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (lse.getValueIsAdjusting()) {
          return;
        }
        if (calculating_) {
          return;
        }
        selectedLayoutDirectiveIndex_ = sourceList_.getSelectedIndex();
        boolean haveARow = (selectedLayoutDirectiveIndex_ != -1);
        buttonDD_.setEnabled(haveARow);
        int maxRow = sourceList_.getModel().getSize() - 1;
        boolean canLower = (haveARow && (selectedLayoutDirectiveIndex_ < maxRow));        
        buttonDown_.setEnabled(canLower);
        boolean canRaise = (haveARow && (selectedLayoutDirectiveIndex_ > 0));        
        buttonUp_.setEnabled(canRaise);
        
        boolean canEdit = false;
        if (haveARow) {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)layoutDeriveDisplay_.get(selectedLayoutDirectiveIndex_);
          // This is a ref to the copy held inside the Layout derivation:
          LayoutDataSource lds = (LayoutDataSource)tocc.val;
          canEdit = lds.isNodeSpecific();
          stockSliders(lds);
        } else {
          clearOutSliders();             
        }
        buttonED_.setEnabled(canEdit); 
        getContentPane().validate();        
 
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;             
    }  
  }
  
  /***************************************************************************
  **
  ** For handling textField changes
  ** 
  */
  
  private class FieldListener implements ActionListener, FocusListener {
    
    private JSlider mySlider_;
    private JTextField myField_;
    private int min_;
    private int max_;
    private int minorTic_;
    private int start_;
    
    FieldListener(JSlider mySlider, JTextField myField, int min, int max, int minorTic, int start) {
      mySlider_ = mySlider;
      myField_ = myField; 
      min_ = min; 
      max_ = max; 
      minorTic_ = minorTic; 
      start_ = start;
    }
    
    public void actionPerformed(ActionEvent evt) {
      try {
        roundVal(mySlider_, myField_, min_, max_, minorTic_, start_);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
    public void focusGained(FocusEvent evt) {
    }    
    public void focusLost(FocusEvent evt) {
      try {
        roundVal(mySlider_, myField_, min_, max_, minorTic_, start_);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }        
  }
  
  /***************************************************************************
  **
  ** Ignore user selection operations....
  ** 
  */
  
  private class MySelectListener extends DefaultListSelectionModel  {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public void clearSelection() {
      return;
    }       
    
    @Override
    public void setSelectionInterval(int s ,int e) {
      return;
    }
    
    @Override
    public void addSelectionInterval(int s, int e) {
      return;
    }
    
    public void specialClearSelection() {
      super.clearSelection();
      return;
    }       
        
    public void specialAddSelectionInterval(int s, int e) { 
      super.addSelectionInterval(s, e);
      return;
    }    
  }  
}
