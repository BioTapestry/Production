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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.add.SuperAdd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.CheckBoxTree;
import org.systemsbiology.biotapestry.util.CheckBoxTreeLeaf;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for deciding which VfAs are going to get synched to the root layout
*/

public class SyncLayoutTargetDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private BTState appState_;
  private DataAccessContext dacx_;
  private ArrayList<SuperAdd.SuperAddPair> saps_;
  private boolean haveResult_;
  private SyncLayoutTree slt_;
  private CheckBoxTree jtree_;
  private boolean doExpand_;
 
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
  
  public SyncLayoutTargetDialog(BTState appState, DataAccessContext dacx) {     
    super(appState.getTopFrame(), appState.getRMan().getString("sltd.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    haveResult_ = false;
    saps_ = new ArrayList<SuperAdd.SuperAddPair>();
    
    ResourceManager rMan = appState.getRMan();    
    setSize(700, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    JLabel lab = new JLabel(rMan.getString("sltd.label"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    cp.add(lab, gbc);
    
    slt_ = buildModelTree();    
    jtree_ = new CheckBoxTree(slt_, appState_);
    expandFullTree();
    JScrollPane jspt = new JScrollPane(jtree_);
    UiUtil.gbcSet(gbc, 0, 1, 1, 20, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(jspt, gbc);    
         
    //
    // Build the button panel:
    //
    
    FixedJButton buttonE = new FixedJButton(rMan.getString("sltd.expand"));
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
    
    
    FixedJButton buttonX = new FixedJButton(rMan.getString("sltd.clearAll"));
    buttonX.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          clearAll();
          jtree_.repaint();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    }); 
    
    FixedJButton buttonS = new FixedJButton(rMan.getString("sltd.selectAll"));
    buttonS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          selectAll();
          jtree_.repaint();
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
            SyncLayoutTargetDialog.this.setVisible(false);
            SyncLayoutTargetDialog.this.dispose();
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
          SyncLayoutTargetDialog.this.setVisible(false);
          SyncLayoutTargetDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonE);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonX);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonS);
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 21, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
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
  
  public List<SuperAdd.SuperAddPair> getSuperAddPairs() {
    return (saps_);
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
  ** Clear out the tree
  ** 
  */
  
  void clearAll() {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)slt_.getRoot();    
    Enumeration e = root.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      SyncLayoutTreeNodeContents contents = (SyncLayoutTreeNodeContents)next.getUserObject();
      if (contents.isModel()) {
        continue;
      }
      if (contents.isLocked()) {
        continue;
      }
      if (contents.isSelected()) {
        contents.setSelected(false);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Select all out the tree
  ** 
  */
  
  void selectAll() {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)slt_.getRoot();    
    Enumeration e = root.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      SyncLayoutTreeNodeContents contents = (SyncLayoutTreeNodeContents)next.getUserObject();
      if (contents.isModel()) {
        continue;
      }
      if (contents.isLocked()) {
        continue;
      }
      if (!contents.isSelected()) {
        contents.setSelected(true);
      }
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
  ** The model for the model/region selection tree
  */

  public class SyncLayoutTree extends DefaultTreeModel {
    
    private HashMap<String, TreeNode> parentMap_;
    private static final long serialVersionUID = 1L;
    
    public SyncLayoutTree() {
      super(new DefaultMutableTreeNode());
      parentMap_ = new HashMap<String, TreeNode>();
      DefaultMutableTreeNode mroot = (DefaultMutableTreeNode)this.getRoot(); 
      String rootID = dacx_.getDBGenome().getID();
      SyncLayoutTreeNodeContents contents = 
        new SyncLayoutTreeNodeContents("allModels", Color.white, rootID, null, true, false, mroot);
      mroot.setUserObject(contents); 
      parentMap_.put(rootID, root);
    }    

    public void addNode(String name, String parentID, String modelID, String groupID, 
                        boolean isModel, boolean isChecked, Color color) {
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentMap_.get(parentID);
      int index = parentNode.getChildCount();  
      addNodeAtIndex(name, parentID, modelID, groupID, isModel, isChecked, color, index); 
      return;
    }
 
    public void addNodeAtIndex(String name, String parentID, String modelID, String groupID, 
                               boolean isModel, boolean isChecked, Color color, int index) {
      
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
      SyncLayoutTreeNodeContents contents = 
        new SyncLayoutTreeNodeContents(name, color, modelID, groupID, isModel, isChecked, newNode);      
      newNode.setUserObject(contents);
      
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentMap_.get(parentID);
      insertNodeInto(newNode, parentNode, index);
      if (isModel) {
        parentMap_.put(modelID, newNode);
      }
      DefaultMutableTreeNode mroot = (DefaultMutableTreeNode)this.getRoot();
      nodeStructureChanged(mroot);
      return;
    }
    
    public List<TreePath> getAllPathsToNonLeaves() {
      ArrayList<TreePath> retval = new ArrayList<TreePath>();
      DefaultMutableTreeNode mroot = (DefaultMutableTreeNode)this.getRoot();    
      Enumeration e = mroot.preorderEnumeration();
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
  ** A node for the model/region selection tree
  */

  public class SyncLayoutTreeNodeContents implements CheckBoxTreeLeaf {    
    private boolean isModel_;
    private boolean isChecked_;
    private String nodeName_;
    private Color nodeColor_;
    private String modelKey_;
    private String groupKey_;
    private DefaultMutableTreeNode myNode_;
    
    public SyncLayoutTreeNodeContents(String nodeName, Color color, String modelKey, String groupKey, 
                                    boolean isModel, boolean isChecked,
                                    DefaultMutableTreeNode myNode) {
      isModel_ = isModel;
      isChecked_ = isChecked;
      nodeName_ = nodeName;
      nodeColor_ = color;
      modelKey_ = modelKey;
      groupKey_ = groupKey;
      myNode_ = myNode;
    } 
    
    public String toString() {
      return (nodeName_);
    }

    public String getName() {
      return (nodeName_);
    }

    public boolean isDisplayOnly() {
      return (isModel_);
    }    
    
    public boolean isSelected() {
      return (isChecked_);
    }
    
    public boolean isModel() {
      return (isModel_);
    }  
    
    public boolean isLocked() {
      return (false);
    } 
    
    public String getModelID() {
      return (modelKey_);
    }  
    
    public String getGroupID() {
      if (isModel_) {
        throw new IllegalStateException();
      }
      return (groupKey_);
    }      
    
    public void setSelected(boolean isSelected) {
      isChecked_ = isSelected;
      return;
    }
    
    public Color getColor() {
      return (nodeColor_);
    }    
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private SyncLayoutTree buildModelTree() {  
  
    SyncLayoutTree retval = new SyncLayoutTree();
   
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    String rootID = dacx_.getDBGenome().getID();
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(gkey);
      if (!gi.isRootInstance()) {
        continue;
      }

      retval.addNode(gi.getName(), rootID, gkey, null, true, false, Color.white);
      Layout lo = dacx_.lSrc.getLayoutForGenomeKey(gkey);

      Iterator<Group> git = gi.getGroupIterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.isASubset(gi)) {
          continue;
        }
        String groupMsg = group.getInheritedDisplayName(gi);
        String groupID = group.getID();
        String baseGroupID = Group.getBaseID(groupID);
        GroupProperties gp = lo.getGroupProperties(baseGroupID);
        Color gCol = gp.getColor(true, dacx_.cRes);

        retval.addNode(groupMsg, gkey, gkey, groupID, false, true, gCol);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      saps_.clear();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)slt_.getRoot();    
      Enumeration e = root.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
        SyncLayoutTreeNodeContents contents = (SyncLayoutTreeNodeContents)next.getUserObject();
        if (contents.isModel()) {
          continue;
        }
        if (contents.isLocked()) {
          continue;
        }
        if (contents.isSelected()) {
          String modelID = contents.getModelID();
          String groupID = contents.getGroupID();
          SuperAdd.SuperAddPair sap = new SuperAdd.SuperAddPair(modelID, groupID, null, null);
          saps_.add(sap);
        }
      }
      haveResult_ = true;
    } else {
      saps_.clear();
      haveResult_ = false;      
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Full expansion
  */
  
  private void expandFullTree() {    
    List<TreePath> nonleafPaths = slt_.getAllPathsToNonLeaves();
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
    List<TreePath> nonleafPaths = slt_.getAllPathsToNonLeaves();
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
}
