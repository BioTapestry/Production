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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.LayoutManager;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for adding node to models globally
*/

public class ChangeSourceOrTargetNodeReviewDialog extends JDialog implements TreeSelectionListener {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private HashMap<String, Map<String, String>> choices_;
  private boolean haveResult_;
  private LinkageReviewTree lrt_;
  private JTree jtree_;
  private JComboBox nodeCombo_;
  private String newNodeID_;
  private JPanel controlPanel_;
  private JPanel hidingPanel_;  
  private CardLayout myCard_;
  private DefaultMutableTreeNode currTargetNode_;
  private boolean editing_;
  private HashMap<String, Set<String>> modelPartition_;
  private JCheckBox deleteChk_;
  private JLabel deleteLabel_;
  private boolean doExpand_;
  private boolean changeSource_;
  private BTState appState_;
  private DataAccessContext dacx_; 
  
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
  
  public ChangeSourceOrTargetNodeReviewDialog(BTState appState, DataAccessContext dacx, String newNodeID, 
                                              Set<String> linkIDs, Map<String, Map<String, String>> quickKillMap, boolean changeSource) {     
    super(appState.getTopFrame(), appState.getRMan().getString((changeSource) ? "changeSrcRev.titleSrc" : "changeSrcRev.titleTrg"), true);
    appState_ = appState;
    dacx_ = dacx;
    haveResult_ = false;
    choices_ = new HashMap<String, Map<String, String>>();
    modelPartition_ = new HashMap<String, Set<String>>();
    newNodeID_ = newNodeID;
    currTargetNode_ = null;
    editing_ = false;
    changeSource_ = changeSource;
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(700, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    String which = (changeSource) ? "changeSrcRev.labelSrc" : "changeSrcRev.labelTrg";
    JLabel lab = new JLabel(rMan.getString(which));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    cp.add(lab, gbc);
    
    lrt_ = buildModelTree(newNodeID, linkIDs, quickKillMap, changeSource_);    
    jtree_ = new JTree(lrt_);
    jtree_.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    jtree_.setCellRenderer(new LinkInstanceRenderer());
    jtree_.addTreeSelectionListener(this);
    expandFullTree();
        
    JScrollPane jspt = new JScrollPane(jtree_);
    UiUtil.gbcSet(gbc, 0, 1, 1, 20, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
    cp.add(jspt, gbc);   
    
    //
    // Build the control panel:
    //
    
    hidingPanel_ = buildControlPanel();    
    UiUtil.gbcSet(gbc, 0, 21, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    cp.add(hidingPanel_, gbc);
    myCard_.show(hidingPanel_, "Hiding");
  
    //
    // Build the button panel:
    //
    
    FixedJButton buttonE = new FixedJButton(rMan.getString("changeSrcRev.expand"));
    buttonE.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (doExpand_) {
            expandFullTree();
          } else {
            collapseFullTree();
          }
          doExpand_ = !doExpand_;
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });        
        
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            ChangeSourceOrTargetNodeReviewDialog.this.setVisible(false);
            ChangeSourceOrTargetNodeReviewDialog.this.dispose();
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
          ChangeSourceOrTargetNodeReviewDialog.this.setVisible(false);
          ChangeSourceOrTargetNodeReviewDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonE);
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 26, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
    selectToFirst();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the result.
  ** 
  */
  
  public Map<String, Map<String, String>> getInstanceChoices() {
    return (choices_);
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
      String tag;
      currTargetNode_ = null;
      TreePath tp = jtree_.getSelectionPath();
      if (tp == null) {
        myCard_.show(hidingPanel_, "Hiding");
        jtree_.repaint();
        return;
      }
      DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)tp.getLastPathComponent();
      Object inNode = targetNode.getUserObject();
      if (inNode instanceof LinkageNodeContents) {
        editing_ = true;
        currTargetNode_ = targetNode;
        LinkageNodeContents lnc = (LinkageNodeContents)inNode;
        GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(lnc.getGenomeID());
        ArrayList<ObjChoiceContent> bc = buildCombo(gi, newNodeID_);
        int bcSize = bc.size();
        nodeCombo_.removeAllItems();
        String newNodeID = lnc.getNewNodeID();
        ObjChoiceContent currOcc = null;
        for (int i = 0; i < bcSize; i++) {
          ObjChoiceContent occ = bc.get(i);
          if ((newNodeID != null) && occ.val.equals(newNodeID)) {
            currOcc = occ;
          }
          nodeCombo_.addItem(occ);
        }
        //
        // If the node combo has no options, we must delete the
        // link, and we must lock it down.
        //
        
        String whichGotta = (changeSource_) ? "changeSrcRev.butIGottaDeleteSrc" : "changeSrcRev.butIGottaDeleteTrg";        
        String whichOptional = (changeSource_) ? "changeSrcRev.optionalDeletionSrc" : "changeSrcRev.optionalDeletionTrg";
 
        ResourceManager rMan = appState_.getRMan();
        if (bcSize == 0) { 
          nodeCombo_.setEnabled(false);
          deleteChk_.setSelected(true);
          deleteChk_.setEnabled(false);
          deleteLabel_.setText(rMan.getString(whichGotta));        
        } else if (newNodeID == null) {
          nodeCombo_.setSelectedIndex(0);
          nodeCombo_.setEnabled(false);
          deleteChk_.setSelected(true);
          deleteChk_.setEnabled(true);
          deleteLabel_.setText(rMan.getString(whichOptional));
        } else {
          nodeCombo_.setSelectedItem(currOcc);
          nodeCombo_.setEnabled(true);
          deleteChk_.setSelected(false);
          deleteChk_.setEnabled(true);
          deleteLabel_.setText("          ");
        }
        
        controlPanel_.invalidate();
        controlPanel_.validate();
        editing_ = false;
        tag = "Control";
        expandToShowMatches();
        jtree_.expandPath(tp);  // actual selection path 
      } else {
        tag = "Hiding";
      }
      jtree_.repaint();  // Because we are selecting other nodes at the same time
      myCard_.show(hidingPanel_, tag);
      
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  


  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /****************************************************************************
  **
  ** The model for the linkage
  */

  public class LinkageReviewTree extends DefaultTreeModel {
    
    private HashMap<String, DefaultMutableTreeNode> parentMap_;
    private HashMap<String, SortedMap<String, List<DefaultMutableTreeNode>>> tempHold_;
    private boolean changeSource_;
    private static final long serialVersionUID = 1L;
    
    public LinkageReviewTree(boolean changeSource) {
      super(new DefaultMutableTreeNode());
      changeSource_ = changeSource;
      parentMap_ = new HashMap<String, DefaultMutableTreeNode>();
      tempHold_ = new HashMap<String, SortedMap<String, List<DefaultMutableTreeNode>>>();
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();
      String rootID = dacx_.getDBGenome().getID();
      myRoot.setUserObject(appState_.getRMan().getString("tree.FullGenome")); 
      parentMap_.put(rootID, myRoot);      
    }    

    public void addNode(String parentID, boolean isModel, String modelID, String modelName, 
                        String newNodeID, String linkID, String genomeID, 
                        String layoutID) {
      DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
      int index = parentNode.getChildCount();  
      addNodeOrHold(parentID, isModel, modelID, modelName, newNodeID, linkID, genomeID, layoutID, index); 
      return;
    }
 
    private void addNodeOrHold(String parentID, boolean isModel, String modelID, String modelName, 
                               String newNodeID, String linkID, String genomeID, 
                               String layoutID, int index) {
      
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
      
      //
      // Models go right in.  Link nodes are held out for sorting:
      //
      
      if (isModel) {
        parentMap_.put(modelID, newNode);
        newNode.setUserObject(modelName);
        DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
        insertNodeInto(newNode, parentNode, index);
        nodeStructureChanged(root);
      } else {
        LinkageNodeContents lnc = new LinkageNodeContents(changeSource_, newNodeID, linkID, genomeID, layoutID);    
        newNode.setUserObject(lnc);
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
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();    
      Enumeration e = myRoot.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
        if (next.getChildCount() != 0) {  
          TreePath path = new TreePath(next.getPath());
          retval.add(path);
        }
      }
      return (retval);
    }      
    
    public List<TreePath> getAllPaths() {
      ArrayList<TreePath> retval = new ArrayList<TreePath>();
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();    
      Enumeration e = myRoot.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
        TreePath path = new TreePath(next.getPath());
        retval.add(path);
      }
      return (retval);
    }         

  }
  
  /****************************************************************************
  **
  ** A node for the linkage selection tree
  */

  public class LinkageNodeContents {
    private boolean changeSrc_;
    private String newNodeID_;
    private String linkID_;
    private String genomeID_;
    private String layoutID_;
    
    private String srcDisplay_;
    private Color srcColor_;
    private String trgDisplay_;
    private Color trgColor_;
    private String linkGlyph_;
    
    public LinkageNodeContents(boolean changeSrc, String newNodeID, String linkID, String genomeID, String layoutID) { 
      changeSrc_ = changeSrc;
      genomeID_ = genomeID;
      layoutID_ = layoutID;
      setLinkage(newNodeID, linkID);
    }
    
    public String toString() {
      return (srcDisplay_ + linkGlyph_ + trgDisplay_);
    }
    
    public String getSourceName() {
      return (srcDisplay_);
    }
    
    public Color getSourceColor() {
      return (srcColor_);
    }
    
    public String getTargetName() {
      return (trgDisplay_);
    }
    
    public Color getTargetColor() {
      return (trgColor_);
    }
    
    public String getLinkGlyph() {
      return (linkGlyph_);
    }    
    
    public String getLinkageID() {
      return (linkID_);
    }
    
    public String getNewNodeID() {
      return (newNodeID_);
    }
    
    public String getGenomeID() {
      return (genomeID_);
    }     
        
    public void setLinkage(String newNodeID, String linkID) {
      newNodeID_ = newNodeID;
      linkID_ = linkID;
      
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeID_);
      Layout lo = dacx_.lSrc.getLayout(layoutID_);
      
      String newDisplay;
      String staticDisplay;
      Color newColor;
      Color staticColor;  
      
      // New node stuff.  May not be in model...
      
      Node newNode = null;
      if (newNodeID_ != null) {
        newNode = gi.getNode(newNodeID_);
        if (newNode == null) {
          newNodeID_ = null;
        }
      }
        
      if (newNodeID_ != null) {  
        newDisplay = newNode.getDisplayString(gi, true);
        Group newGrp = gi.getGroupForNode(newNodeID_, GenomeInstance.ALWAYS_MAIN_GROUP);
        if (newGrp == null) { // should not happen
          newColor = Color.GRAY;
        } else {
          String groupID = newGrp.getID();
          String baseGroupID = Group.getBaseID(groupID);
          GroupProperties gp = lo.getGroupProperties(baseGroupID);
          newColor = gp.getColor(true, dacx_.cRes);
        }
      } else {
        ResourceManager rMan = appState_.getRMan();
        newDisplay = rMan.getString("changeSrcRev.noNode");
        newColor = Color.RED;
      }
      
      // Static node stuff
      
      Linkage link = gi.getLinkage(linkID_);
      String staticID = (changeSrc_) ? link.getTarget() : link.getSource();
      Node staticNode = gi.getNode(staticID);
      staticDisplay = staticNode.getDisplayString(gi, true);
      Group staticGrp = gi.getGroupForNode(staticID, GenomeInstance.ALWAYS_MAIN_GROUP);
      if (staticGrp == null) { // should not happen
        staticColor = Color.GRAY;
      } else {
        String groupID = staticGrp.getID();
        String baseGroupID = Group.getBaseID(groupID);
        GroupProperties gp = lo.getGroupProperties(baseGroupID);
        staticColor = gp.getColor(true, dacx_.cRes);      
      }
      
      //
      // Assign the changes to the actual fields:
      //
      
      if (changeSrc_) {      
        srcDisplay_ = newDisplay;
        trgDisplay_ = staticDisplay;
        srcColor_ = newColor;
        trgColor_ = staticColor;
      } else {
        trgDisplay_ = newDisplay; 
        srcDisplay_ = staticDisplay;
        trgColor_ = newColor;
        srcColor_ = staticColor;      
      }
      
      // Link glyph (A hack!)
      
      String format = DBLinkage.mapSignToDisplay(appState_, link.getSign());
      String linkMsg = MessageFormat.format(format, new Object[] {"",""});
      linkGlyph_ = linkMsg.trim(); // yuk!
      return;
    }
  }  
 
  /****************************************************************************
  **
  ** For rendering
  */

  public class LinkInstanceRenderer extends DefaultTreeCellRenderer {
    
    private LinkInstanceLeaf myLeaf_;
    private static final long serialVersionUID = 1L;
    
    public LinkInstanceRenderer() {
      super();
      myLeaf_ = new LinkInstanceLeaf();
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
                                                  boolean selected, boolean expanded, 
                                                  boolean leaf, int row, boolean hasFocus) {
      try {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        Object inNode = node.getUserObject();
        if (inNode instanceof LinkageNodeContents) {
          LinkageNodeContents lnc = (LinkageNodeContents)inNode;
          myLeaf_.setSrc(lnc.getSourceName(), lnc.getSourceColor());
          myLeaf_.setTarg(lnc.getTargetName(), lnc.getTargetColor());
          myLeaf_.setLink(lnc.getLinkGlyph());
          if (selected) {
            myLeaf_.setSelected(LinkInstanceLeaf.LeafSelectionOpt.FULL_SELECTION);
          } else if (currTargetNode_ == null) {
            myLeaf_.setSelected(LinkInstanceLeaf.LeafSelectionOpt.NOT_SELECTED);
          } else {
            Object currNode = currTargetNode_.getUserObject();
            LinkInstanceLeaf.LeafSelectionOpt selval = LinkInstanceLeaf.LeafSelectionOpt.NOT_SELECTED;
            if (currNode instanceof LinkageNodeContents) {
              LinkageNodeContents clnc = (LinkageNodeContents)currNode;
              String currGenome = clnc.getGenomeID();
              String currLinkage = clnc.getLinkageID();          
              String myGenome = lnc.getGenomeID();
              String myLinkage = lnc.getLinkageID();
              if ((currGenome == null) || (currLinkage == null) || 
                  (myGenome == null) || (myLinkage == null)) {
                selval = LinkInstanceLeaf.LeafSelectionOpt.NOT_SELECTED;
              } else {
                Set<String> models = modelPartition_.get(currGenome); 
                selval = (models.contains(myGenome) && 
                          currLinkage.equals(myLinkage)) ? LinkInstanceLeaf.LeafSelectionOpt.RELATIVE_SELECTED
                                                         : LinkInstanceLeaf.LeafSelectionOpt.NOT_SELECTED;
              }
            }
            myLeaf_.setSelected(selval);
          }
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
  
  public static class LinkInstanceLeaf extends JPanel {

    private JLabel srcLabel_;
    private JPanel srcPanel_;
    private JLabel trgLabel_;
    private JPanel trgPanel_;
    private JLabel linkLabel_;
    private JPanel linkPanel_;    
    private static final long serialVersionUID = 1L;
    
    public enum LeafSelectionOpt {      
      NOT_SELECTED(Color.white), 
      FULL_SELECTION(Color.blue),
      RELATIVE_SELECTED(Color.gray);
      
      private Color myCol_;
    
      private LeafSelectionOpt(Color color) {
        myCol_ = color;
      }
    
      public Color getBackColor() {
        return (myCol_); 
      }
    }
    
    public LinkInstanceLeaf() {
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
    
      srcLabel_ = new JLabel();
      srcPanel_ = new JPanel();
      srcPanel_.setLayout(new GridLayout(1, 1));
      srcPanel_.add(srcLabel_);
      UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0, 1, 1, 1, 0, UiUtil.CEN, 1.0, 0.0);       
      add(srcPanel_, gbc);
    
      linkLabel_ = new JLabel();
      linkPanel_ = new JPanel();
      linkPanel_.setLayout(new GridLayout(1, 1));
      linkPanel_.add(linkLabel_);      
      UiUtil.gbcSet(gbc, 5, 0, 1, 1, UiUtil.NONE, 0, 0, 1, 0, 1, 0, UiUtil.CEN, 0.0, 0.0);
      add(linkPanel_, gbc);
      
      trgLabel_ = new JLabel();
      trgPanel_ = new JPanel();
      trgPanel_.setLayout(new GridLayout(1, 1));
      trgPanel_.add(trgLabel_);      
      UiUtil.gbcSet(gbc, 6, 0, 5, 1, UiUtil.HOR, 0, 0, 1, 0, 1, 1, UiUtil.CEN, 1.0, 0.0);
      add(trgPanel_, gbc);
      
      setBackground(Color.white);
    }
    
    public void setSrc(String name, Color color) {
      srcLabel_.setText(name);
      srcPanel_.setBackground(color);
      return;
    }
    
    public void setTarg(String name, Color color) {
      trgLabel_.setText(name);
      trgPanel_.setBackground(color);
      return;
    }
    
    public void setLink(String linkTxt) {
      linkLabel_.setText(linkTxt);
      linkPanel_.setBackground(Color.white);
      return;      
    }
      
    public void setSelected(LeafSelectionOpt selected) {
      setBackground(selected.getBackColor());
      return;      
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build the tree model
  ** 
  */    
  
  private LinkageReviewTree buildModelTree(String newNodeID, Set<String> linkIDs, Map<String, Map<String, String>> quickKillMap, boolean changeSrc) {  
  
    LinkageReviewTree retval = new LinkageReviewTree(changeSrc);   


    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    String rootID = dacx_.getDBGenome().getID();
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(gkey);
      Map<String, String> quickKillForModel = (quickKillMap == null) ? null : quickKillMap.get(gkey);
      if (gi instanceof DynamicGenomeInstance) {
    
      } else {
        GenomeInstance parent = gi.getVfgParent();
        String parentID = (parent == null) ? rootID : parent.getID();
        // Keep track of namespaces for linkIDs:
        Set<String> models = (parent == null) ? new HashSet<String>() : modelPartition_.get(parentID);
        models.add(gkey);
        modelPartition_.put(gkey, models);
       
        retval.addNode(parentID, true, gkey, gi.getName(), null, null, null, null);
        String loKey = dacx_.lSrc.getLayoutForGenomeKey(gkey).getID();
        Iterator<Linkage> lit = gi.getLinkageIterator();
        while (lit.hasNext()) {
          Linkage link = lit.next();
          String linkID = link.getID();
          String newSrcID = (quickKillForModel == null) ? null : quickKillForModel.get(linkID);
          if (linkIDs.contains(GenomeItemInstance.getBaseID(linkID))) {
            retval.addNode(gkey, false, null, null, newSrcID, linkID, gkey, loKey);
          }
        }
      }
    }
    retval.finishTree();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get everybody in sync with the change
  ** 
  */
  
  private void propagateSettingThroughTree(String newNodeID, String linkID) {     
    if (currTargetNode_ == null) {
      return;
    }    
    TreeNode parent = currTargetNode_.getParent(); // parent will be a model node
    propagateSettingUp(parent, newNodeID, linkID);  
    return;
  }
  
  /***************************************************************************
  **
  ** Get everybody in sync with the change
  ** 
  */
  
  private void propagateSettingUp(TreeNode node, String newNodeID, String linkID) {
    // Keep crawling up model nodes until we hit a model below the root (VfA).  Then
    // turn around and head back down:
    TreeNode parent = node.getParent();    
    if (parent.getParent() == null) {
      propagateSettingDown(node, newNodeID, linkID);
    } else {   
      propagateSettingUp(parent, newNodeID, linkID);
    }
    return;
  }     

  /***************************************************************************
  **
  ** Get everybody in sync with the change
  ** 
  */
  
  private void propagateSettingDown(TreeNode node, String newNodeID, String linkID) {
    Enumeration e = node.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      Object contents = next.getUserObject();
      if (contents instanceof LinkageNodeContents) {
        LinkageNodeContents lnc = (LinkageNodeContents)contents;
        String currLink = lnc.getLinkageID();
        if (linkID.equals(currLink)) {
          lnc.setLinkage(newNodeID, currLink);
          lrt_.nodeChanged(next);
        }
      } else {
        propagateSettingDown(next, newNodeID, linkID);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Only sync everybody straight up
  ** 
  */
  
  private void propagateSettingStraightUp(TreeNode node, String newNodeID, String linkID) {
    TreeNode parent = node.getParent();
    if (parent == null) {
      return;
    }
    propagateOnlyToLinks(parent, newNodeID, linkID);
    propagateSettingStraightUp(parent, newNodeID, linkID);
    return;
  }
   
  /***************************************************************************
  **
  ** Apply only to links for model parent node
  ** 
  */
  
  private void propagateOnlyToLinks(TreeNode node, String newNodeID, String linkID) {
    Enumeration e = node.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      Object contents = next.getUserObject();
      if (contents instanceof LinkageNodeContents) {
        LinkageNodeContents lnc = (LinkageNodeContents)contents;
        String currLink = lnc.getLinkageID();
        if (linkID.equals(currLink)) {
          lnc.setLinkage(newNodeID, currLink);
          lrt_.nodeChanged(next);
        }
      }
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Find a non-null setting to use
  ** 
  */
  
  private String findSettingInTree(String linkID) {
    TreeNode parent = currTargetNode_.getParent(); // parent will be a model node
    return (findSettingUp(parent, linkID));  
  } 
  
  /***************************************************************************
  **
  ** Find a non-null setting to use
  ** 
  */
  
  private String findSettingUp(TreeNode node, String linkID) {
    // Keep crawling up model nodes until we hit a model below the root (VfA).  Then
    // turn around and head back down:
    TreeNode parent = node.getParent();    
    if (parent.getParent() == null) {
      return (findSettingDown(node, linkID));
    } else {   
      return (findSettingUp(parent, linkID));
    }
  }     

  /***************************************************************************
  **
  ** Find a non-null setting to use
  ** 
  */
  
  private String findSettingDown(TreeNode node, String linkID) {
    Enumeration e = node.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      Object contents = next.getUserObject();
      if (contents instanceof LinkageNodeContents) {
        LinkageNodeContents lnc = (LinkageNodeContents)contents;
        String currLink = lnc.getLinkageID();
        if (linkID.equals(currLink)) {
          String newNodeID = lnc.getNewNodeID();
          if (newNodeID != null) {
            return (newNodeID);
          }
        }
      } else {
        String fsd = findSettingDown(next, linkID);
        if (fsd != null) {
          return (fsd);
        }
      }
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Build the control panel
  ** 
  */  
  
  private JPanel buildControlPanel() {  
    JPanel retval = new JPanel();
    controlPanel_ = new JPanel();
    myCard_ = new CardLayout();
    retval.setLayout(myCard_);
    retval.add(controlPanel_, "Control");
    JPanel blankPanel = new JPanel();
    retval.add(blankPanel, "Hiding");
    
    controlPanel_.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    ResourceManager rMan = appState_.getRMan();
    
    String which = (changeSource_) ? "changeSrcRev.setSource" : "changeSrcRev.setTarget";
    JLabel nodeLabel = new JLabel(rMan.getString(which));
    nodeCombo_ = new JComboBox();

    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    controlPanel_.add(nodeLabel, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    controlPanel_.add(nodeCombo_, gbc);
    
    deleteChk_ = new JCheckBox(rMan.getString("changeSrcRev.deleteDontSwitch"));
    UiUtil.gbcSet(gbc, 0, 1, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    controlPanel_.add(deleteChk_, gbc);
 
    which = (changeSource_) ? "changeSrcRev.butIGottaDeleteSrc" : "changeSrcRev.butIGottaDeleteTrg";
    deleteLabel_ = new JLabel(rMan.getString(which));
    UiUtil.gbcSet(gbc, 0, 2, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    controlPanel_.add(deleteLabel_, gbc);    
   
    nodeCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          handleNodeChange();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    
    deleteChk_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          handleCheckBox();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Build the combo of available source nodes
  ** 
  */
  
  private ArrayList<ObjChoiceContent> buildCombo(GenomeInstance gi, String srcNodeBase) {
    TreeMap<String, ObjChoiceContent> orderMap = new TreeMap<String, ObjChoiceContent>();   
    Set<String> nodeInst = gi.getNodeInstances(srcNodeBase);
    Iterator<String> niit = nodeInst.iterator();
    while (niit.hasNext()) {
      String niid = niit.next();
      Node node = gi.getNode(niid);
      String display = node.getDisplayString(gi, false);
      orderMap.put(display, new ObjChoiceContent(display, niid));
    }
    return (new ArrayList<ObjChoiceContent>(orderMap.values()));
  }  
 
  /***************************************************************************
  **
  ** Track node selections
  ** 
  */

  void handleNodeChange() {
    try { 
      if (currTargetNode_ == null) {
        return;
      }
      if (editing_) {
        return;
      }
      String nodeID = ((ObjChoiceContent)nodeCombo_.getSelectedItem()).val;
      Object inNode = currTargetNode_.getUserObject();
      LinkageNodeContents lnc = (LinkageNodeContents)inNode;
      String currLink = lnc.getLinkageID();
      propagateSettingThroughTree(nodeID, currLink);      
      jtree_.repaint();
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }        
  
  /***************************************************************************
  **
  ** Track deletion requests
  ** 
  */
  
  void handleCheckBox() {
    if (currTargetNode_ == null) {
      return;
    }    
    if (editing_) {
      return;
    }
    boolean selected = deleteChk_.isSelected();
    String newNode;
    if (selected) {
      ResourceManager rMan = appState_.getRMan();
      nodeCombo_.setEnabled(false);
      newNode = null;
      String which = (changeSource_) ? "changeSrcRev.optionalDeletionSrc" : "changeSrcRev.optionalDeletionTrg";
      deleteLabel_.setText(rMan.getString(which));
    } else {
      nodeCombo_.setEnabled(true);
      newNode = ((ObjChoiceContent)nodeCombo_.getSelectedItem()).val;
      deleteLabel_.setText("          ");
    }
    //
    // If newly null, all kids below us must be null too.
    // If newly non-null, everybody straight above us must be non-null too; below stays null,
    // off to side stays null.  If we have some instance to tell us what to make the
    // source, keep that. Else use what we have.
    // 
    //
    
    Object inNode = currTargetNode_.getUserObject();
    LinkageNodeContents lnc = (LinkageNodeContents)inNode;
    String currLink = lnc.getLinkageID();    
    
    if (newNode == null) { 
      lnc.setLinkage(newNode, currLink);
      lrt_.nodeChanged(currTargetNode_);
      TreeNode parent = currTargetNode_.getParent();
      propagateSettingDown(parent, null, currLink);
    } else {
      String otherNewNode = findSettingInTree(currLink);  
      if (otherNewNode != null) {
        newNode = otherNewNode;
        editing_ = true;
        ComboBoxModel mod = nodeCombo_.getModel();
        int numNodeCom = mod.getSize();
        for (int i = 0; i < numNodeCom; i++) {
          ObjChoiceContent occ = (ObjChoiceContent)mod.getElementAt(i);
          if (occ.val.equals(newNode)) {
            nodeCombo_.setSelectedIndex(i);
            break;
          }
        }
        editing_ = false;
      }
      propagateSettingStraightUp(currTargetNode_, newNode, currLink);
    }
           
    jtree_.repaint();
    
    controlPanel_.invalidate();
    controlPanel_.validate();
    return;
  }    
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      choices_.clear();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)lrt_.getRoot();    
      Enumeration e = root.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
        Object inNode = next.getUserObject();
        if (inNode instanceof LinkageNodeContents) {
          LinkageNodeContents lnc = (LinkageNodeContents)inNode;
          String genomeID = lnc.getGenomeID();
          String linkID = lnc.getLinkageID();
          String newNodeID = lnc.getNewNodeID();
          Map<String, String> mapPerModel = choices_.get(genomeID);
          if (mapPerModel == null) {
            mapPerModel = new HashMap<String, String>();
            choices_.put(genomeID, mapPerModel);
          }
          mapPerModel.put(linkID, newNodeID);
        }
      }
      haveResult_ = true;
    } else {
      choices_.clear();
      haveResult_ = false;      
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Full expansion
  */
  
  private void expandFullTree() {
    List<TreePath> nonleafPaths = lrt_.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      jtree_.expandPath(tp);
    }
    return;
  }

  /***************************************************************************
  **
  ** Full collapse
  */  
  
  private void collapseFullTree() {
    List<TreePath> nonleafPaths = lrt_.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      while (tp.getPathCount() > 1) {
        jtree_.collapsePath(tp);
        tp = tp.getParentPath();
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** expansion to show link matches
  */
  
  private void expandToShowMatches() {
    List<TreePath> paths = lrt_.getAllPaths();
    Iterator<TreePath> pit = paths.iterator();
    while (pit.hasNext()) {
      TreePath tp = pit.next();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tp.getLastPathComponent();
      if (nodeMatchesCurrentTarget(node)) {
        jtree_.expandPath(tp.getParentPath());
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** expansion to show link matches
  */
  
  private void selectToFirst() {
    List<TreePath> paths = lrt_.getAllPaths();
    Iterator<TreePath> pit = paths.iterator();
    while (pit.hasNext()) {
      TreePath tp = pit.next();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tp.getLastPathComponent();
      if (node.isLeaf()) {
        Object inNode = node.getUserObject();
        if (inNode instanceof LinkageNodeContents) {
          jtree_.setSelectionPath(tp);
          break;
        }
      }
    }
    return;
  }    

  /***************************************************************************
  **
  ** Ask if we match current target
  */  
  
  private boolean nodeMatchesCurrentTarget(DefaultMutableTreeNode node) {  
    Object inNode = node.getUserObject();
    if (inNode instanceof LinkageNodeContents) {
      LinkageNodeContents lnc = (LinkageNodeContents)inNode;
      Object currNode = currTargetNode_.getUserObject();
      if (currNode instanceof LinkageNodeContents) {
        LinkageNodeContents clnc = (LinkageNodeContents)currNode;
        String currGenome = clnc.getGenomeID();
        String currLinkage = clnc.getLinkageID();          
        String myGenome = lnc.getGenomeID();
        String myLinkage = lnc.getLinkageID();
        if ((currGenome == null) || (currLinkage == null) || 
            (myGenome == null) || (myLinkage == null)) {
          return (false);
        } else {
          Set<String> models = modelPartition_.get(currGenome); 
          return (models.contains(myGenome) && currLinkage.equals(myLinkage));
        }  
      }
    }
    return (false);
  }
}
