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
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
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
** Dialog box for adding node to models globally
*/

public class SuperAddTargetDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ArrayList<SuperAdd.SuperAddPair> saps_;
  private boolean haveResult_;
  private SuperAddTree sat_;
  private CheckBoxTree jtree_;
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
  
  public SuperAddTargetDialog(BTState appState, DataAccessContext dacx, String nodeID) {     
    super(appState.getTopFrame(), appState.getRMan().getString("satd.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    haveResult_ = false;
    saps_ = new ArrayList<SuperAdd.SuperAddPair>();
    
    ResourceManager rMan = appState.getRMan();    
    setSize(500, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    JLabel lab = new JLabel(rMan.getString("satd.label"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    cp.add(lab, gbc);
    
    sat_ = buildModelTree(nodeID);    
    jtree_ = new CheckBoxTree(sat_, appState_);
    JScrollPane jspt = new JScrollPane(jtree_);
    UiUtil.gbcSet(gbc, 0, 1, 1, 20, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(jspt, gbc);    
         
    //
    // Build the button panel:
    //
    
    FixedJButton buttonE = new FixedJButton(rMan.getString("satd.expand"));
    buttonE.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          expandFullTree();
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
            SuperAddTargetDialog.this.setVisible(false);
            SuperAddTargetDialog.this.dispose();
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
          SuperAddTargetDialog.this.setVisible(false);
          SuperAddTargetDialog.this.dispose();
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /****************************************************************************
  **
  ** The model for the model/region selection tree
  */

  public class SuperAddTree extends DefaultTreeModel {
    
    private HashMap<String, TreeNode> parentMap_;
    private static final long serialVersionUID = 1L;
    
    public SuperAddTree() {
      super(new DefaultMutableTreeNode());
      parentMap_ = new HashMap<String, TreeNode>();
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();
      String rootID = dacx_.getDBGenome().getID();
      SuperAddTreeNodeContents contents = 
        new SuperAddTreeNodeContents("allModels", Color.white, rootID, null, true, false, false, myRoot);
      myRoot.setUserObject(contents); 
      parentMap_.put(rootID, root);
    }    

    public void addNode(String name, String parentID, String modelID, String groupID, 
                        boolean isModel, boolean isChecked, boolean isLocked, Color color) {
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentMap_.get(parentID);
      int index = parentNode.getChildCount();  
      addNodeAtIndex(name, parentID, modelID, groupID, isModel, isChecked, isLocked, color, index); 
      return;
    }
 
    public void addNodeAtIndex(String name, String parentID, String modelID, String groupID, 
                               boolean isModel, boolean isChecked, boolean isLocked, Color color, int index) {
      
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
      SuperAddTreeNodeContents contents = 
        new SuperAddTreeNodeContents(name, color, modelID, groupID, isModel, isChecked, isLocked, newNode);      
      newNode.setUserObject(contents);
      
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentMap_.get(parentID);
      insertNodeInto(newNode, parentNode, index);
      if (isModel) {
        parentMap_.put(modelID, newNode);
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
  }
 
  /****************************************************************************
  **
  ** A node for the model/region selection tree
  */

  public class SuperAddTreeNodeContents implements CheckBoxTreeLeaf {    
    private boolean isModel_;
    private boolean isChecked_;
    private boolean isLocked_;    
    private String nodeName_;
    private Color nodeColor_;
    private String modelKey_;
    private String groupKey_;
    private DefaultMutableTreeNode myNode_;
    
    public SuperAddTreeNodeContents(String nodeName, Color color, String modelKey, String groupKey, 
                                    boolean isModel, boolean isChecked, boolean isLocked, 
                                    DefaultMutableTreeNode myNode) {
      isModel_ = isModel;
      isChecked_ = isChecked;
      nodeName_ = nodeName;
      nodeColor_ = color;
      modelKey_ = modelKey;
      isLocked_ = isLocked;
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
    
    public boolean isLocked() {
      return (isLocked_);
    }    
    
    public boolean isModel() {
      return (isModel_);
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
      if (isModel_) {
        return;
      }
      isChecked_ = isSelected;
      //
      // This is where we handle upward propagation to all parents. I am a group
      // node, so we want to jump up to my parent to propagate
      //
      TreeNode parent = myNode_.getParent();
      propagateSelectionUpTree(parent, Group.getBaseID(groupKey_));        
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
  
  private SuperAddTree buildModelTree(String nodeID) {  
  
    SuperAddTree retval = new SuperAddTree();
    
    String rootNodeID = GenomeItemInstance.getBaseID(nodeID);
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    String rootID = dacx_.getDBGenome().getID();
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      GenomeInstance gi = (GenomeInstance)dacx_.getGenomeSource().getGenome(gkey);
      if (gi instanceof DynamicGenomeInstance) {
        // FIX ME... Do something?
      } else {
        GenomeInstance parent = gi.getVfgParent();
        String parentID = (parent == null) ? rootID : parent.getID();
        GenomeInstance rootInstance = gi.getVfgParentRoot();
        if (rootInstance == null) {
          rootInstance = gi;
        }
        
        retval.addNode(gi.getName(), parentID, gkey, null, true, false, false, Color.white);
        Layout lo = dacx_.lSrc.getLayoutForGenomeKey(gkey);
        
        Iterator<Group> git = gi.getGroupIterator();
        while (git.hasNext()) {
          Group group = git.next();
          // FIX ME.  This only shows parent region name.  If model is showing a
          // subgroup of an inactive parent, this is confusing.
          if (group.isASubset(gi)) {
            continue;
          }
          String groupMsg = group.getInheritedDisplayName(gi);
          String groupID = group.getID();
          String baseGroupID = Group.getBaseID(groupID);
          GroupProperties gp = lo.getGroupProperties(baseGroupID);
          Color gCol = gp.getColor(true, dacx_.cRes);
         
          int iNum = rootInstance.getInstanceForNodeInGroup(rootNodeID, baseGroupID);
          boolean present = (iNum != -1);
          if (present) {
            String instanceKey = GenomeItemInstance.getCombinedID(rootNodeID, Integer.toString(iNum));
            Node node = gi.getNode(instanceKey);
            present = (node != null);
          }
          retval.addNode(groupMsg, gkey, gkey, groupID, false, present, present, gCol);
        }
      }
    }
    return (retval);
  }
  
  private void propagateSelectionUpTree(TreeNode myNode, String groupID) {    
    TreeNode parent = myNode.getParent();
    Enumeration e = parent.children();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      SuperAddTreeNodeContents contents = (SuperAddTreeNodeContents)next.getUserObject();
      if (contents.isModel()) {
        continue;
      }
      String kidGroupID = contents.getGroupID();
      if (Group.getBaseID(kidGroupID).equals(groupID)) {
        if (contents.isLocked()) { // we are done
          return;
        }
        if (contents.isSelected()) {  // also done
          return;
        }
        contents.setSelected(true); // this handles the recursion
        break;
      }
    }   
    return;
  }  

  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      saps_.clear();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)sat_.getRoot();    
      Enumeration e = root.preorderEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
        SuperAddTreeNodeContents contents = (SuperAddTreeNodeContents)next.getUserObject();
        if (contents.isModel()) {
          continue;
        }
        if (contents.isLocked()) {
          continue;
        }
        if (contents.isSelected()) {
          String modelID = contents.getModelID();
          String groupID = contents.getGroupID();
          SuperAdd.SuperAddPair sap = new SuperAdd.SuperAddPair(modelID, groupID);
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
    List<TreePath> nonleafPaths = sat_.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      jtree_.expandPath(tp);
    }
    return;
  }
  
}
