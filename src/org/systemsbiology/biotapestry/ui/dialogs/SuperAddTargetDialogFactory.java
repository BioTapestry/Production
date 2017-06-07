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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
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
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.CheckBoxTree;
import org.systemsbiology.biotapestry.util.CheckBoxTreeLeaf;
import org.systemsbiology.biotapestry.util.FixedJButton;

/****************************************************************************
**
** Factory for dialog boxes that define subgroups
*/

public class SuperAddTargetDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SuperAddTargetDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    SuperAddBuildArgs dniba = (SuperAddBuildArgs)ba;
    SuperAddTree sat = buildModelTree(dniba.nodeID, cfh.getDataAccessContext());
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, sat));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class SuperAddBuildArgs extends DialogBuildArgs { 
    
    String nodeID;
          
    public SuperAddBuildArgs(String nodeID) {
      super(null);
      this.nodeID = nodeID;
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
   
    private SuperAddTree sat_;
    private CheckBoxTree jtree_;
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
     
    public DesktopDialog(ServerControlFlowHarness cfh, SuperAddTree sat) {
      super(cfh, "satd.title", new Dimension(500, 700), 1, new SuperAddTargetRequest(), false);

      JLabel lab = new JLabel(rMan_.getString("satd.label")); 
      addWidgetFullRow(lab, false, false);
    
      sat_ = sat;  
      jtree_ = new CheckBoxTree(sat_, uics_.getHandlerAndManagerSource());
      JScrollPane jspt = new JScrollPane(jtree_);
      addTable(jspt, 20);

      FixedJButton buttonE = new FixedJButton(rMan_.getString("satd.expand"));
      buttonE.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            expandFullTree();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      }); 

      finishConstructionWithExtraLeftButton(buttonE);
    }
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
    **
    ** Gotta say
    */
    
    public boolean dialogIsModal() {
      return true;
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
 
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      SuperAddTargetRequest crq = (SuperAddTargetRequest)request_;     
      
      crq.sapResult = new ArrayList<SuperAdd.SuperAddPair>();
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
          SuperAdd.SuperAddPair sap = new SuperAdd.SuperAddPair(modelID, groupID, null, null);
          crq.sapResult.add(sap);
        }
      }
      crq.haveResult = true;
      return (true);
    }
  }

  /***************************************************************************
  **
  ** Return Results
  ** 
  */
   
  public static class SuperAddTargetRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public List<SuperAdd.SuperAddPair> sapResult;
     
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }
    
    public void setHasResults() {
      this.haveResult = true;
      return;
    }
    
    public boolean isForApply() {
      return (false);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPER: BUILD MODEL TREE
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private SuperAddTree buildModelTree(String nodeID, DataAccessContext dacxI) {  
  
    DataAccessContext dacx = new StaticDataAccessContext(dacxI).getContextForRoot();
    String rootID = dacx.getDBGenome().getID();
    SuperAddTree retval = new SuperAddTree(rootID);
    
    String rootNodeID = GenomeItemInstance.getBaseID(nodeID);
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
   
    List<String> navList = navTree.getPreorderListing(true);
    Iterator<String> nlit = navList.iterator();    
    while (nlit.hasNext()) {
      String gkey = nlit.next();
      GenomeInstance gi = (GenomeInstance)dacx.getGenomeSource().getGenome(gkey);
      if (gi instanceof DynamicGenomeInstance) {
        // FIX ME... Do something?
      } else {
        GenomeInstance parent = gi.getVfgParent();
        String parentID = (parent == null) ? rootID : parent.getID();
        GenomeInstance rootInstance = gi.getVfgParentRoot();
        if (rootInstance == null) {
          rootInstance = gi;
        }
        
        retval.addNode(gi.getName(), parentID, gkey, null, null, true, false, false, Color.white);
        Layout lo = dacx.getLayoutSource().getLayoutForGenomeKey(gkey);
        
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
          Color gCol = gp.getColor(true, dacx.getColorResolver());
        
          int iNum = rootInstance.getInstanceForNodeInGroup(rootNodeID, baseGroupID);
          boolean present = (iNum != -1);
          if (present) {
            String instanceKey = GenomeItemInstance.getCombinedID(rootNodeID, Integer.toString(iNum));
            Node node = gi.getNode(instanceKey);
            present = (node != null);
          }
          retval.addNode(groupMsg, gkey, gkey, groupID, Group.getBaseID(groupID), false, present, present, gCol);
        }
      }
    }
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES. MODEL TREE AND NODE CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /****************************************************************************
  **
  ** The model for the model/region selection tree. ***Uses SWING TREE classes***, but could be generalized
  ** for web dialog building.
  */

  public class SuperAddTree extends DefaultTreeModel {
    
    private HashMap<String, TreeNode> parentMap_;
    private static final long serialVersionUID = 1L;
    
    public SuperAddTree(String rootID) {
      super(new DefaultMutableTreeNode());
      parentMap_ = new HashMap<String, TreeNode>();
      DefaultMutableTreeNode myRoot = (DefaultMutableTreeNode)this.getRoot();
      SuperAddTreeNodeContents contents = 
        new SuperAddTreeNodeContents("allModels", Color.white, rootID, null, null, true, false, false, myRoot);
      myRoot.setUserObject(contents); 
      parentMap_.put(rootID, root);
    }    

    public void addNode(String name, String parentID, String modelID, String groupID, String baseID,
                        boolean isModel, boolean isChecked, boolean isLocked, Color color) {
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentMap_.get(parentID);
      int index = parentNode.getChildCount();  
      addNodeAtIndex(name, parentID, modelID, groupID, baseID, isModel, isChecked, isLocked, color, index); 
      return;
    }
 
    public void addNodeAtIndex(String name, String parentID, String modelID, String groupID, String baseID,
                               boolean isModel, boolean isChecked, boolean isLocked, Color color, int index) {
      
      DefaultMutableTreeNode newNode = new DefaultMutableTreeNode();
      SuperAddTreeNodeContents contents = 
        new SuperAddTreeNodeContents(name, color, modelID, groupID, baseID, isModel, isChecked, isLocked, newNode);      
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
    private String baseID_;
    private DefaultMutableTreeNode myNode_;
    
    public SuperAddTreeNodeContents(String nodeName, Color color, String modelKey, String groupKey, 
                                    String baseID, boolean isModel, boolean isChecked, boolean isLocked, 
                                    DefaultMutableTreeNode myNode) {
      isModel_ = isModel;
      isChecked_ = isChecked;
      nodeName_ = nodeName;
      nodeColor_ = color;
      modelKey_ = modelKey;
      isLocked_ = isLocked;
      groupKey_ = groupKey;
      baseID_ = baseID;
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
    
    public String getGroupBaseID() {
      if (isModel_) {
        throw new IllegalStateException();
      }
      return (baseID_);
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
      propagateSelectionUpTree(parent, baseID_);        
      return;
    }
    
    public Color getColor() {
      return (nodeColor_);
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
        String kidGroupBaseID = contents.getGroupBaseID();
        if (kidGroupBaseID.equals(groupID)) {
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
  }
}
