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


package org.systemsbiology.biotapestry.nav;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

import org.xml.sax.Attributes;

/****************************************************************************
**
** The model for the model navigation heirarchy tree
*/

public class NavTree extends DefaultTreeModel {
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public enum Skips {NO_FLAG, SKIP_EVENT, SKIP_FINISH};
  
  public enum Kids {
    HIDDEN_ROOT("hidden"),
    ROOT_MODEL("root"), 
    ROOT_INSTANCE("rootInstance"), 
    STATIC_CHILD_INSTANCE("staticInstance"), 
    DYNAMIC_SUM_INSTANCE("dynamicSum"), 
    DYNAMIC_SLIDER_INSTANCE("dynamicSlider"),
    GROUP_NODE("group"),
    ;
  
    private String tag_;
     
    Kids(String tag) {
      this.tag_ = tag;  
    }

    public String getTag() {
      return (tag_);      
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<ModelID, NodeID> modelMap_;
  private HashMap<NodeID, DefaultMutableTreeNode> nodeMap_;
  private boolean doingUndo_;
  private Skips skipFlag_;
  private UniqueLabeller labels_;
  private boolean needLegacyGlue_;
  private UIComponentSource uics_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build almost empty NavTree
  */ 
  
  public NavTree(UIComponentSource uics) {
    super(new DefaultMutableTreeNode());
    uics_ = uics;
    labels_ = new UniqueLabeller();
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    String hiddenID = labels_.getNextLabel(); 
    NavNodeContents contents = new NavNodeContents(Kids.HIDDEN_ROOT, "", null, null, hiddenID, null);
    dmtnRoot.setUserObject(contents);    
    modelMap_ = new HashMap<ModelID, NodeID>();
    nodeMap_ = new HashMap<NodeID, DefaultMutableTreeNode>();
    doingUndo_ = false;
    skipFlag_ = Skips.NO_FLAG;
    needLegacyGlue_ = true;
  }

  /***************************************************************************
  **
  ** Copy constructor
  */ 
  
  public NavTree(NavTree other) {
    super(other.deepCopyRoot());
    modelMap_ = new HashMap<ModelID, NodeID>();
    nodeMap_ = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps((DefaultMutableTreeNode)other.getRoot(), modelMap_, nodeMap_); 
    labels_ = other.labels_.clone();
    doingUndo_ = false;
    skipFlag_ = Skips.NO_FLAG;
    needLegacyGlue_ = other.needLegacyGlue_;
  }

  /***************************************************************************
  **
  ** Answers if input construction needs to be by models, or more recently we do it.  
  */ 
  
  public boolean needLegacyGlue() {
    return (needLegacyGlue_);
  }
  
  /***************************************************************************
  **
  ** Set if input construction needs to be by models, or more recently we do it.  
  */ 
  
  public void setLegacyGlue(boolean needIt) {
    needLegacyGlue_ = needIt;
    return;
  }
  
  /***************************************************************************
  **
  ** Empty out the NavTree
  */ 
  
  public void clearOut() {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    dmtnRoot.removeAllChildren();
    modelMap_ = new HashMap<ModelID, NodeID>();
    nodeMap_ = new HashMap<NodeID, DefaultMutableTreeNode>();
    if (!uics_.isHeadless()) {
      nodeStructureChangedWithoutFlow(dmtnRoot);
    } else {
      TreeNode[] tn = dmtnRoot.getPath();
      TreePath tp = new TreePath(tn);
      VirtualModelTree vmt = uics_.getTree();
      if (vmt != null) {
        vmt.setTreeSelectionPath(tp); 
      }
    }
    labels_ = new UniqueLabeller();
    NavNodeContents drNNC = (NavNodeContents)dmtnRoot.getUserObject();
    labels_.addExistingLabel(drNNC.nodeID);
    nodeMap_.put(new NodeID(drNNC.nodeID), dmtnRoot);
    needLegacyGlue_ = true;
    return;
  }
      
  /***************************************************************************
  **
  ** wrapper
  */ 
  
  public void nodeStructureChangedWithoutFlow(TreeNode dmtnRoot) {
    VirtualModelTree vmt = uics_.getTree();
    if (vmt != null) {
      vmt.setDoNotFlow(true);
    }
    nodeStructureChanged(dmtnRoot);
    if (vmt != null) {
      vmt.setDoNotFlow(false);
    }
    return;
  }    

  /***************************************************************************
  **
  ** Skip flag is used to avoid posting/sinking some programmatic change events
  */ 
  
  public void setSkipFlag(Skips flag) {
    skipFlag_ = flag;
    return;
  }
  
  /***************************************************************************
  **
  ** Skip flag is used to avoid posting/sinking some programmatic change events
  */ 
  
  public Skips getSkipFlag() {
    return (skipFlag_);
  }  

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NavTreeChange undo) {
    if (undo.oldNodeMap != null) {
      doingUndo_ = true;
      setRoot(undo.oldRoot);
      nodeMap_ = undo.oldNodeMap;
      modelMap_ = undo.oldModMap;
      labels_ = undo.oldLabels;
      if (!uics_.isHeadless()) {
        nodeStructureChangedWithoutFlow((TreeNode)this.getRoot());
      }
      doingUndo_ = false;
    } else {
      undoContentChange(undo);
    }
    uics_.getTree().refreshTree();
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NavTreeChange undo) {
    if (undo.newNodeMap != null) {
      doingUndo_ = true;
      setRoot(undo.newRoot);
      nodeMap_ = undo.newNodeMap;
      modelMap_ = undo.newModMap;
      labels_ = undo.newLabels;
      if (!uics_.isHeadless()) {
        nodeStructureChangedWithoutFlow((TreeNode)this.getRoot());
      }
      doingUndo_ = false;
    } else {
      redoContentChange(undo);      
    }
    uics_.getTree().refreshTree();
    return;
  }

  /***************************************************************************
  **
  ** Answer if undo is in progress
  */
  
  public boolean amDoingUndo() {
    return (doingUndo_);
  }
   
  /***************************************************************************
  **
  ** Add a node to the Nav Tree with the given information at the set index
  */ 
  
  public NodeAndChanges addNode(Kids type, String name, TreeNode parNode, ModelID modelID, String proxyID, Integer indexObj, DataAccessContext dacx) {
    int index;
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldModMap = new HashMap<ModelID, NodeID>();
    retval.oldNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.oldRoot, retval.oldModMap, retval.oldNodeMap); 
    retval.oldLabels = labels_.clone();
    String kidLabel = labels_.getNextLabel();
    NodeID kidNID = new NodeID(kidLabel);
    
    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parNode;
    switch (type) {
      case ROOT_MODEL:
        if (name != null) {
          throw new IllegalArgumentException();
        }
        if (proxyID != null) {
          throw new IllegalArgumentException();
        }
        if ((indexObj != null) && (indexObj.intValue() != 0)) {
          throw new IllegalArgumentException();
        }
        // parNode is null for ROOT (legacy usage):
        parentNode = (DefaultMutableTreeNode)this.getRoot();
        name = dacx.getRMan().getString("tree.FullGenome");
        index = 0;
        break;
      case ROOT_INSTANCE:
        if (proxyID != null) {
          throw new IllegalArgumentException();
        }
        index = (indexObj != null) ? indexObj.intValue() : parentNode.getChildCount();
        break;
      case STATIC_CHILD_INSTANCE:
      case DYNAMIC_SUM_INSTANCE:
        if (proxyID != null) {
          throw new IllegalArgumentException();
        }
        index = (indexObj != null) ? indexObj.intValue() : parentNode.getChildCount();
        break;
      case DYNAMIC_SLIDER_INSTANCE:
        if (proxyID == null) {
          throw new IllegalArgumentException();
        }
        if (modelID != null) {
          throw new IllegalArgumentException();
        }
        index = (indexObj != null) ? indexObj.intValue() : parentNode.getChildCount();
        break;
      case GROUP_NODE:
        if (name == null) {
          throw new IllegalArgumentException();
        }
        if (proxyID != null) {
          throw new IllegalArgumentException();
        }
        if (modelID != null) {
          throw new IllegalArgumentException();
        }     
        index = (indexObj != null) ? indexObj.intValue() : parentNode.getChildCount();
        break;
      default: 
        throw new IllegalArgumentException(); 
    }

    NavNodeContents parNNC = (NavNodeContents)parentNode.getUserObject();
    String mid = (modelID == null) ? null : modelID.modelID;
    NavNodeContents contents = new NavNodeContents(type, name, mid, proxyID, kidLabel, parNNC.nodeID);   
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(contents);
    this.insertNodeInto(newNode, parentNode, index);     
    nodeMap_.put(kidNID, newNode);
    if (modelID != null) {
      modelMap_.put(modelID, kidNID);
    }
    retval.newRoot = deepCopyRoot();
    retval.newModMap = new HashMap<ModelID, NodeID>();
    retval.newNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.newRoot, retval.newModMap, retval.newNodeMap); 
    retval.newLabels = labels_.clone();
    if (!uics_.isHeadless()) {
      nodeStructureChangedWithoutFlow(this.root);
    }
    return (new NodeAndChanges(newNode, kidNID, retval));
  }
  
  /***************************************************************************
  **
  ** Copy a group node. This includes duping the image references and the group map
  */ 
  
  public NodeAndChanges copyGroupNode(String name, TreeNode groupNodeToCopy, TreeNode parNode, Integer indexObj) {
   
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldModMap = new HashMap<ModelID, NodeID>();
    retval.oldNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.oldRoot, retval.oldModMap, retval.oldNodeMap); 
    retval.oldLabels = labels_.clone();
    String kidLabel = labels_.getNextLabel();
    NodeID kidNID = new NodeID(kidLabel);
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)groupNodeToCopy;    
    
    NavNodeContents oldNnc = (NavNodeContents)dmtn.getUserObject();
    if (oldNnc.type != NavTree.Kids.GROUP_NODE) {
      throw new IllegalArgumentException();
    }

    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parNode;
    if (name == null) {
      throw new IllegalArgumentException();
    }
    int index = (indexObj != null) ? indexObj.intValue() : parentNode.getChildCount();

    NavNodeContents parNNC = (NavNodeContents)parentNode.getUserObject();
    NavNodeContents contents = new NavNodeContents(NavTree.Kids.GROUP_NODE, name, null, null, kidLabel, parNNC.nodeID);
    
    //
    // Copy the map across, if it exists. But Image refs will be added below
    //
    
    GroupNodeEntry oldGne = oldNnc.getGroupEntry();
    if (oldGne != null) {
      GroupNodeEntry ngne = new GroupNodeEntry(null, null, null, null);
      contents.addGroupEntry(ngne);
      Map<Color, GroupNodeMapEntry> ogmm = oldGne.getModelMap();
      if (ogmm != null) {
         Iterator<Color> cit = ogmm.keySet().iterator();
         while (cit.hasNext()) {
           Color colKey = cit.next();
           GroupNodeMapEntry gnme = ogmm.get(colKey);
           ngne.addMapEntry(gnme.clone());  
         }   
      }
    }
   
   
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(contents);
    
    insertNodeInto(newNode, parentNode, index);     
    nodeMap_.put(kidNID, newNode);
    retval.newRoot = deepCopyRoot();
    retval.newModMap = new HashMap<ModelID, NodeID>();
    retval.newNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.newRoot, retval.newModMap, retval.newNodeMap); 
    retval.newLabels = labels_.clone();
    if (!uics_.isHeadless()) {
      nodeStructureChangedWithoutFlow(this.root);
    }
    
    NodeAndChanges superRetval = new NodeAndChanges(newNode, kidNID, retval);
  
    //
    // Bump image references:
    //
    
    String oldIKn = getImageKey(dmtn, false);
    if (oldIKn != null) {
      ImageChange[] ics = setGroupImage(superRetval.newID.nodeID, oldIKn, false);
      if (ics != null) {
        superRetval.icsl.addAll(Arrays.asList(ics));
      }
    }
    String oldIKm = getImageKey(dmtn, true);
    if (oldIKm != null) {
      ImageChange[] ics = setGroupImage(superRetval.newID.nodeID, oldIKm, true);
      if (ics != null) {
        superRetval.icsl.addAll(Arrays.asList(ics));
      }
    }
 
    return (superRetval);
  }

  /***************************************************************************
  **
  ** If we delete a model, we need to ditch any references to it in group node
  ** click maps. Same with regions, times, as well!
  */ 
  
  public List<NavTreeChange> dropGroupNodeModelReferences(String modelKey) {
    
    ArrayList<NavTreeChange> retval = new ArrayList<NavTreeChange>();
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      NavNodeContents nnc = (NavNodeContents)next.getUserObject();
      if (nnc.type == Kids.GROUP_NODE) {
        GroupNodeEntry oldGne = nnc.getGroupEntry();
        if (oldGne != null) {
          boolean needNew = false;
          GroupNodeEntry newGne = new GroupNodeEntry(oldGne.imageID,oldGne.mapImageID, oldGne.modelID, oldGne.proxyID);
          Map<Color, GroupNodeMapEntry> ogmm = oldGne.getModelMap();
          if (ogmm != null) {
            Iterator<Color> cit = ogmm.keySet().iterator();
            while (cit.hasNext()) {
              Color colKey = cit.next();
              GroupNodeMapEntry gnme = ogmm.get(colKey);
              GroupNodeMapEntry gnmeRepl = gnme.clone();
              if (modelKey.equals(gnme.modelID)) {
                gnmeRepl.modelID = null;
                needNew = true;
              }
              newGne.addMapEntry(gnmeRepl);
            }    
          }
          if (needNew) {
            NavTreeChange retvalElem = new NavTreeChange();
            retvalElem.oldContents = nnc.clone();
            nnc.replaceGroupEntry(newGne);
            retvalElem.newContents = nnc.clone();
            retval.add(retvalElem);
          }
        }
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Add a node to the Nav Tree for IO
  */ 
  
  void addNode(NavNodeContents nnc) {
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nnc);  
    labels_.addExistingLabel(nnc.nodeID);
    NodeID kidNID = new NodeID(nnc.nodeID);
    DefaultMutableTreeNode parentNode = nodeMap_.get(new NodeID(nnc.parNodeID));
    
    switch (nnc.type) {
      case ROOT_MODEL:
      case ROOT_INSTANCE:     
      case STATIC_CHILD_INSTANCE:
      case DYNAMIC_SUM_INSTANCE:
        modelMap_.put(new ModelID(nnc.modelID), kidNID);
        break;
      case DYNAMIC_SLIDER_INSTANCE:
      case GROUP_NODE:
        break;
      default:
        throw new IllegalStateException();
    }
    int index = parentNode.getChildCount();
    this.insertNodeInto(newNode, parentNode, index);
    //
    // If this call is not made, a VfG-only model tree will not show ANY nodes
    // on startup:
    //
    if (!uics_.isHeadless()) {
      nodeStructureChangedWithoutFlow(this.root);
    }
    nodeMap_.put(kidNID, newNode);
    return;
  }

  /***************************************************************************
  **
  ** Answer if we can shift node in tree
  */ 
  
  public boolean canShiftNode(DefaultMutableTreeNode movingNode, boolean doLower) {
    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)movingNode.getParent();
    int oldIndex = getIndexOfChild(parentNode, movingNode);
    int numKids = getChildCount(parentNode);
    int newIndex = oldIndex + ((doLower) ? 1 : -1);
    if ((newIndex >= numKids) || (newIndex < 0)) {
      return (false);
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Shift node in the tree
  */ 
  
  public NavTreeChange shiftNode(DefaultMutableTreeNode movingNode, boolean doLower) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldModMap = new HashMap<ModelID, NodeID>();
    retval.oldNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.oldRoot, retval.oldModMap, retval.oldNodeMap); 
    retval.oldLabels = labels_.clone();

    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)movingNode.getParent();
    int oldIndex = getIndexOfChild(parentNode, movingNode);
    int numKids = getChildCount(parentNode);
    int newIndex = oldIndex + ((doLower) ? 1 : -1);
    if ((newIndex >= numKids) || (newIndex < 0)) {
      return (null);
    }
    removeNodeFromParent(movingNode);
    insertNodeInto(movingNode, parentNode, newIndex);
    retval.newRoot = deepCopyRoot();
    retval.newModMap = new HashMap<ModelID, NodeID>();
    retval.newNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.newRoot, retval.newModMap, retval.newNodeMap); 
    retval.newLabels = labels_.clone();
    if (!uics_.isHeadless()) {
      nodeStructureChangedWithoutFlow(root);
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the set of existing sibling names, minus the given node
  */ 
  
  public Set<String> getSiblingNames(TreeNode checkNode) {
    HashSet<String> retval = new HashSet<String>();
    Enumeration cen = checkNode.getParent().children();
    while (cen.hasMoreElements()) {
      DefaultMutableTreeNode kid = (DefaultMutableTreeNode)cen.nextElement();
      if (kid != checkNode) {
        NavNodeContents nnc = (NavNodeContents)kid.getUserObject();
        retval.add(nnc.name);
      }
    }   
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the list of existing child names
  */ 
  
  public List<String> getChildNames(TreeNode checkNode) {
    ArrayList<String> retval = new ArrayList<String>();
    Enumeration cen = checkNode.children();
    while (cen.hasMoreElements()) {
      DefaultMutableTreeNode kid = (DefaultMutableTreeNode)cen.nextElement();
      NavNodeContents nnc = (NavNodeContents)kid.getUserObject();
      retval.add(nnc.name);
    }   
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the index for a new sibling node
  */ 
  
  public int getNewSiblingIndex(TreeNode checkNode) {
    TreeNode parentNode = checkNode.getParent();
    return (getIndexOfChild(parentNode, checkNode) + 1);
  }    

  /***************************************************************************
  **
  ** Change the given node to a proxy node
  */ 
  
  public NavTreeChange changeToProxy(DefaultMutableTreeNode node, String proxyID) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldContents = new NavNodeContents((NavNodeContents)node.getUserObject());
    retval.newContents = new NavNodeContents(Kids.DYNAMIC_SLIDER_INSTANCE, retval.oldContents.name, null, proxyID, retval.oldContents.nodeID, retval.oldContents.parNodeID);
    node.setUserObject(new NavNodeContents(retval.newContents));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the given node from a proxy node
  */ 
  
  public NavTreeChange changeFromProxy(DefaultMutableTreeNode node, String modelID) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldContents = new NavNodeContents((NavNodeContents)node.getUserObject());
    retval.newContents = new NavNodeContents(Kids.DYNAMIC_SUM_INSTANCE, retval.oldContents.name, modelID, null, retval.oldContents.nodeID, retval.oldContents.parNodeID);
    node.setUserObject(new NavNodeContents(retval.newContents)); 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** init
  */ 
  
  public ImageChange dropGroupImage(String groupNodeID, boolean forMap) {
    ImageManager mgr = uics_.getImageMgr();
    String oldID = null;    
    TreeNode tn = nodeForNodeID(groupNodeID);
    oldID = getImageKey(tn, forMap);
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tn;
    NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
    nnc.setImageID(null, forMap);
    if (oldID != null) {      
      ImageChange dropChange = mgr.dropImageUsage(oldID);
      dropChange.groupNodeKey = groupNodeID;
      dropChange.groupNodeForMap = forMap;
      return (dropChange);
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Set the group node image
  **
  */
  
  public ImageChange[] setGroupImage(String groupNodeID, String imgKey, boolean forMap) {  
    ImageManager mgr = uics_.getImageMgr();
    ArrayList<ImageChange> allChanges = new ArrayList<ImageChange>();
    String oldID = null;
    TreeNode tn = nodeForNodeID(groupNodeID);
    oldID = getImageKey(tn, forMap);

    if (oldID != null) {     
      ImageChange dropChange = mgr.dropImageUsage(oldID);
      dropChange.groupNodeKey = groupNodeID;
      dropChange.groupNodeForMap = forMap;
      allChanges.add(dropChange);
    } 
    if (imgKey != null) {
      ImageChange regChange = mgr.registerImageUsage(imgKey);
      regChange.groupNodeKey = groupNodeID;
      regChange.groupNodeForMap = forMap;
      allChanges.add(regChange);
    }
    int changeCount = allChanges.size();
    // If this is true, we are setting null to null and nothing changes....
    if (changeCount == 0) {
      return (null);
    }   
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tn;
    NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
    nnc.setImageID(imgKey, forMap);
    ImageChange[] retval = new ImageChange[changeCount];
    allChanges.toArray(retval);
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Answer if group node has image, map image
  */  
   
  public boolean nodeHasGroupImage(TreeNode node, boolean forMap) {
    String imKey = getImageKey(node, forMap);
    return (imKey != null);
  }

  /***************************************************************************
  **
  ** Get the image key
  */ 
  
  public String getImageKey(TreeNode node, boolean forMap) {
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)node;
    NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
    return (nnc.getImageID(forMap));
  }
  
  /***************************************************************************
  **
  ** Get the group model map
  */ 
  
  public Map<Color, GroupNodeMapEntry> getGroupModelMap(TreeNode node) {
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)node;
    NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
    if ((nnc.grpEntries_ != null) && !nnc.grpEntries_.isEmpty()) {
      // If we allow more than one, this needs to be fixed!
      return (nnc.grpEntries_.get(0).getModelMap());
    } else {
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Set the group model map
  */ 
  
  public NavTreeChange installGroupModelMap(TreeNode node, Map<Color, GroupNodeMapEntry> modMap) {
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)node;
    NavTreeChange retval = new NavTreeChange();
    NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
    if ((nnc.grpEntries_ == null) || nnc.grpEntries_.isEmpty()) {
      // If we allow more than one, this needs to be fixed!
      throw new IllegalStateException();
    }
    nnc.setModelMap(modMap);
    retval.oldContents = new NavNodeContents(nnc);
    nnc.setModelMap(modMap);
    retval.newContents = new NavNodeContents(nnc);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete node (or not) and all children of node from the Nav Tree, optional to keep node
  */ 
  
  public NavTreeChange deleteNodeAndChildren(TreeNode tNode, boolean nodeToo) {
    DefaultMutableTreeNode parentOfNodesToGo = (DefaultMutableTreeNode)tNode;
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldModMap = new HashMap<ModelID, NodeID>();
    retval.oldNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.oldRoot, retval.oldModMap, retval.oldNodeMap); 
    retval.oldLabels = labels_.clone();
    
    if (nodeToo) {
      removeNodeFromParent(parentOfNodesToGo);
    }

    HashSet<NodeID> cnids = new HashSet<NodeID>();
    HashSet<ModelID> cmids = new HashSet<ModelID>();
    getChildNodeAndModelIDs(parentOfNodesToGo, cnids, cmids);
    NavNodeContents nc = (NavNodeContents)parentOfNodesToGo.getUserObject();
    if (!nodeToo) {
      cnids.remove(new NodeID(nc.nodeID));
      if (nc.modelID != null) {
        cmids.remove(new ModelID(nc.modelID));
      }  
    }
    for (NodeID nid : cnids) {
      nodeMap_.remove(nid);
      labels_.removeLabel(nid.nodeID);
    }
    for (ModelID mid : cmids) {
      modelMap_.remove(mid);
    }

    ArrayList<DefaultMutableTreeNode> kids = new ArrayList<DefaultMutableTreeNode>();
    Enumeration cen = parentOfNodesToGo.children();
    while (cen.hasMoreElements()) {
      DefaultMutableTreeNode kid = (DefaultMutableTreeNode)cen.nextElement();
      kids.add(kid);
    }   
    Iterator<DefaultMutableTreeNode> kit = kids.iterator();
    while (kit.hasNext()) {
      DefaultMutableTreeNode kid = kit.next();
      removeNodeFromParent(kid);
    }
    retval.newRoot = deepCopyRoot();
    retval.newModMap = new HashMap<ModelID, NodeID>();
    retval.newNodeMap = new HashMap<NodeID, DefaultMutableTreeNode>();
    makeMaps(retval.newRoot, retval.newModMap, retval.newNodeMap); 
    retval.newLabels = labels_.clone();
    return (retval);
  }

  /***************************************************************************
  **
  ** Refresh the node name for the given id. Note these only handle the model nodes. Group nodes
  ** are handled directly by the tree. In fact, model names are also handled by the tree. This is
  ** a holdover that can probably be ditched.
  */ 
  
  public void nodeNameRefresh(String id, DataAccessContext dacx) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if ((contents != null) && (contents.modelID != null) && contents.modelID.equals(id)) {
        //
        // Workaround for bogosity. Note that a dynamic summary model traditionally uses the "modelID" instead
        // of the proxyID. But when a dynamic model name is updated, the proxy is changed, but the dynamic proxy 
        // cache is not invalidated until the operation is complete. So when we go to hit "the model" to get the name,
        // it is stale. Always go back to the proxy!
        //
        String name;
        if (DynamicInstanceProxy.isDynamicInstance(contents.modelID)) {  
           name = dacx.getGenomeSource().getDynamicProxy(DynamicInstanceProxy.extractProxyID(contents.modelID)).getName();
        } else {  
           name = dacx.getGenomeSource().getGenome(id).getName();
        }
        contents.name = name;
        nodeChanged(next);
        break;
      } else if ((contents != null) && (contents.proxyID != null) && contents.proxyID.equals(id)) {
        String name = dacx.getGenomeSource().getDynamicProxy(id).getName();
        contents.name = name;
        nodeChanged(next);
        break;
      }
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the IDs of the *immediate* non-group children, maybe us too. But if the child is a group
  ** node, we keep going down.
  */ 
  
  public Set<String> getImmediateChildModelIDs(TreeNode node, boolean meToo) {
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)node;    
    NavNodeContents nc = (NavNodeContents)dmtn.getUserObject();
    HashSet<String> cmids = new HashSet<String>();
    if (meToo && (nc.modelID != null)) {
      cmids.add(nc.modelID);
    }
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      NavNodeContents cnc = (NavNodeContents)child.getUserObject();
      if (cnc.modelID != null) {
        cmids.add(cnc.modelID);
      } else if (cnc.type == Kids.GROUP_NODE) {
        cmids.addAll(getImmediateChildModelIDs(child, false));
      }
    }
    return (cmids);
  }

  /***************************************************************************
  **
  ** Get the selected genomeID
  */ 
  
  public String getGenomeID(TreePath tp) {
    if (tp == null) {
      return (null);
    }
    Object[] path = tp.getPath();
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[path.length - 1];
    NavNodeContents contents = (NavNodeContents)last.getUserObject();
    return ((contents == null) ? null : contents.modelID);  // modelID is null for time slider proxies
  }  

  /***************************************************************************
  **
  ** Get the selected dynamic proxy ID
  */ 
  
  public String getDynamicProxyID(TreePath tp) {
    if (tp == null) {
      return (null);
    }
    Object[] path = tp.getPath();
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[path.length - 1];
    NavNodeContents contents = (NavNodeContents)last.getUserObject();
    return ((contents == null) ? null : contents.proxyID);  // proxy id is null for regular VFN
  }
  
  /***************************************************************************
  **
  ** Get the ancestor model ID
  */ 
  
  public String getGenomeModelAncestorID(TreePath tp) {
    if (tp == null) {
      return (null);
    }
    Object[] path = tp.getPath();
    for (int i = path.length - 1; i >=0; i--) {
      DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[i];
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      if ((contents == null) ||
          (contents.type == Kids.DYNAMIC_SLIDER_INSTANCE) ||
          (contents.type == Kids.HIDDEN_ROOT)) {
        return (null);
      }
      if (contents.type != Kids.GROUP_NODE) {
        return (contents.modelID);
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the ancestor model ID
  */ 
  
  public String getGenomeModelAncestorID(TreeNode tn) {
    if (tn == null) {
      return (null);
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn;
    String retval = null;
    while (retval == null) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      if ((contents.type == Kids.HIDDEN_ROOT) || (contents.type == Kids.DYNAMIC_SLIDER_INSTANCE)) {
        return (null);
      } else if (contents.type == Kids.GROUP_NODE) {
        NodeID parID = new NodeID(contents.parNodeID);
        last = this.nodeMap_.get(parID);
        if (last == null) {
          throw new IllegalStateException();
        }
      } else {
        retval = contents.modelID;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if our most recent ancestor is a dynamic sum instance
  */ 
  
  public boolean ancestorIsDynamicSum(TreeNode tn) {
    if (tn == null) {
      throw new IllegalArgumentException();
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn.getParent();
    if (last == null) {
      throw new IllegalArgumentException();
    }
    while (true) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      switch (contents.type) {
        case ROOT_MODEL:
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:
          return (false);
        case DYNAMIC_SUM_INSTANCE:       
          return (true);
        case GROUP_NODE:
          last = (DefaultMutableTreeNode)last.getParent();
          break;
        case HIDDEN_ROOT:
        case DYNAMIC_SLIDER_INSTANCE:
        default:
          throw new IllegalStateException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Answer if our most recent ancestor is a dynamic sum instance
  */ 
  
  public NavNode getDynamicSumAncestor(TreeNode tn) {
    if (tn == null) {
      throw new IllegalArgumentException();
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn.getParent();
    if (last == null) {
      throw new IllegalArgumentException();
    }
    while (true) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      switch (contents.type) {
        case DYNAMIC_SUM_INSTANCE:
          return (contentsToNavNode(contents));
        case GROUP_NODE:
          last = (DefaultMutableTreeNode)last.getParent();
          break;
        case ROOT_MODEL:
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:  
        case HIDDEN_ROOT:
        case DYNAMIC_SLIDER_INSTANCE:
        default:
          throw new IllegalStateException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Answer if our most recent ancestor is static
  */ 
  
  public boolean ancestorIsStatic(TreeNode tn) {
    if (tn == null) {
      throw new IllegalArgumentException();
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn.getParent();
    if (last == null) {
      throw new IllegalArgumentException();
    }
    while (true) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      switch (contents.type) {
        case ROOT_MODEL:
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:
          return (true);
        case DYNAMIC_SUM_INSTANCE:       
          return (false);
        case GROUP_NODE:
          last = (DefaultMutableTreeNode)last.getParent();
          break;
        case HIDDEN_ROOT:
        case DYNAMIC_SLIDER_INSTANCE:
        default:
          throw new IllegalStateException();
      }
    }    
  }
  
  /***************************************************************************
  **
  ** Get most recent static ancestor
  */ 
  
  public NavNode getStaticAncestor(TreeNode tn) {
    if (tn == null) {
      throw new IllegalArgumentException();
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn.getParent();
    if (last == null) {
      throw new IllegalArgumentException();
    }
    while (true) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      switch (contents.type) {
        case ROOT_MODEL:
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:
          return (contentsToNavNode(contents));
        case GROUP_NODE:
          last = (DefaultMutableTreeNode)last.getParent();
          break;
        case HIDDEN_ROOT:
        case DYNAMIC_SLIDER_INSTANCE:
        case DYNAMIC_SUM_INSTANCE:
        default:
          throw new IllegalStateException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Answer if our most recent ancestor is root model
  */ 
  
  public boolean ancestorIsRoot(TreeNode tn) {
    if (tn == null) {
      throw new IllegalArgumentException();
    }
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn.getParent();
    if (last == null) {
      throw new IllegalArgumentException();
    }
    while (true) {     
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      switch (contents.type) {
        case ROOT_MODEL:
          return (true);
        case ROOT_INSTANCE:
        case STATIC_CHILD_INSTANCE:
        case DYNAMIC_SUM_INSTANCE:       
          return (false);
        case GROUP_NODE:
          last = (DefaultMutableTreeNode)last.getParent();
          break;
        case HIDDEN_ROOT:
        case DYNAMIC_SLIDER_INSTANCE:
        default:
          throw new IllegalStateException();
      }
    }    
  }
 
  /***************************************************************************
  **
  ** Get the selected Group name
  */ 
  
  public String getGroupName(TreePath tp) {
    if (tp == null) {
      return (null);
    }
    Object[] path = tp.getPath();
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[path.length - 1];
    NavNodeContents contents = (NavNodeContents)last.getUserObject();
    if ((contents == null) || (contents.type != Kids.GROUP_NODE)) {
      return (null);
    }
    return (contents.name);
  }
  
  /***************************************************************************
  **
  ** Get the node name
  */ 
  
  public String getNodeName(TreeNode tn) {
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn;
    NavNodeContents contents = (NavNodeContents)last.getUserObject();
    if (contents == null) {
      return (null);
    }
    return (contents.name); 
  }

  /***************************************************************************
  **
  ** Set the node name
  */ 
  
  public NavTreeChange setNodeName(TreeNode tn, String name) {
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)tn;
    NavTreeChange retval = new NavTreeChange();
    retval.oldContents = new NavNodeContents((NavNodeContents)last.getUserObject());    
    retval.newContents = new NavNodeContents(retval.oldContents);
    retval.newContents.name = name;
    last.setUserObject(new NavNodeContents(retval.newContents)); 
    nodeChanged(tn);
    uics_.getTree().refreshTree();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the group node ID
  */ 
  
  public String getGroupNodeID(TreeNode node) {
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    if ((contents == null) || (contents.type != Kids.GROUP_NODE)) {
      return (null);
    }
    return (contents.nodeID);
  }
  
  /***************************************************************************
  **
  ** Get the selected Group node ID
  */ 
  
  public String getGroupNodeID(TreePath tp) {
    if (tp == null) {
      return (null);
    }
    Object[] path = tp.getPath();
    DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[path.length - 1];
    NavNodeContents contents = (NavNodeContents)last.getUserObject();
    if ((contents == null) || (contents.type != Kids.GROUP_NODE)) {
      return (null);
    }
    return (contents.nodeID);
  }

  /***************************************************************************
  **
  ** answer if a group node
  */ 
  
  public boolean isGroupNode(TreeNode node) {
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    if ((contents == null) || (contents.type != Kids.GROUP_NODE)) {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the selected genomeID
  */ 
  
  public String getGenomeID(TreeNode node) {
    if (node == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    return ((contents == null) ? null : contents.modelID);  // modelID is null for time slider proxies
  } 
  
  public NodeID getNodeIDObj(TreeNode node) {
    if (node == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    return ((contents == null) ? null : new NodeID(contents.nodeID));  // modelID is null for time slider proxies
  } 
  
  
  /********************
   * getNodeID
   *******************
   * 
   * 
   * 
   * @param node
   * @return
   * 
   */
  public String getNodeID(TreeNode node) {
    if (node == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    return ((contents == null) ? null : contents.nodeID);  // modelID is null for time slider proxies
  } 
  
  public String getNodeID(TreePath tp) {
    if (tp == null) {
        return (null);
      }
      Object[] path = tp.getPath();
      DefaultMutableTreeNode last = (DefaultMutableTreeNode)path[path.length - 1];
      NavNodeContents contents = (NavNodeContents)last.getUserObject();
      return ((contents == null) ? null : contents.nodeID);  // modelID is null for time slider proxies
  } 

  /***************************************************************************
  **
  ** Get the selected dynamic proxy ID
  */ 
  
  public String getDynamicProxyID(TreeNode node) {
    if (node == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    return ((contents == null) ? null : contents.proxyID);  // proxy id is null for regular VFN
  }  

  /***************************************************************************
  **
  ** Get the node for a model ID. Better not be a dynamic slider!
  */ 
  
  public TreeNode nodeForModel(String modelID) {
    NodeID nid = modelMap_.get(new ModelID(modelID));
    if (nid == null) {
      return (null);
    }
    return (nodeMap_.get(nid));
  }
  
  /***************************************************************************
  **
  ** Get the node for a nodeID.
  */ 
  
  public TreeNode nodeForNodeID(String nodeID) {
    if (nodeID == null) {
      return (null);
    }
    return (nodeMap_.get(new NodeID(nodeID)));
  }
  
  /***************************************************************************
  **
  ** Get the node for a nodeID.
  */ 
  
  public TreeNode nodeForNodeIDObj(NodeID nodeID) {
    if (nodeID == null) {
      return (null);
    }
    return (nodeMap_.get(nodeID));
  }
  
  /***************************************************************************
  **
  ** Get the NavNode for a TreeNode.
  */ 
  
  public NavNode navNodeForNode(TreeNode node) {
    DefaultMutableTreeNode mutNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)mutNode.getUserObject();  
    return (contentsToNavNode(contents));
  }

 /***************************************************************************
  **
  ** Get the node for a nodeID. Will return null for top level node, which has a null NodeID
  */ 
  
  public NavNode navNodeForNodeID(NodeID nodeID, DataAccessContext dacx) {
    if (nodeID == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtn = nodeMap_.get(nodeID);
    XPlatModelNode.NodeKey key = treeNodeToXPlatKey(dmtn, dacx);
    NavNode nn = new NavTree.NavNode(nodeID, key, getNodeName(dmtn), getNodeIDObj(dmtn.getParent()), dacx);
    return (nn);
  }

  /***************************************************************************
  **
  ** Get the VfG selection
  */ 
  
  public TreePath getVfgSelection() {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    if (dmtnRoot.getChildCount() == 0) {
      return (null);
    }
    DefaultMutableTreeNode full = (DefaultMutableTreeNode)dmtnRoot.getFirstChild();
    return (new TreePath(full.getPath()));
  }
 
  /***************************************************************************
  **
  ** Get the default selection (the last child at the VfA level);
  */ 
  
  public TreePath getDefaultSelection() {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    if (dmtnRoot.getChildCount() == 0) {
      return (null);
    }
    DefaultMutableTreeNode full = (DefaultMutableTreeNode)dmtnRoot.getFirstChild();
    if (full.getChildCount() == 0) {
      return (new TreePath(full.getPath()));
    }
    DefaultMutableTreeNode vfg = (DefaultMutableTreeNode)full.getLastChild(); 
    return (new TreePath(vfg.getPath()));
  } 
  
  /***************************************************************************
  **
  ** Get the startup selection
  */ 
  
  public TreePath getStartupSelection(String modelKey, DataAccessContext dacx) {
    if (modelKey == null) {
      return (getDefaultSelection());
    }
    
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      String genomeID = getGenomeID(next);
      String nodeID = getNodeID(next);
      if (((genomeID != null) && modelKey.equals(genomeID)) || (modelKey.equalsIgnoreCase(nodeID))) {
        return (new TreePath(next.getPath()));
      } 
      
      if (genomeID == null) {
        String dipID = getDynamicProxyID(next);
        if (dipID != null) {
          DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(dipID);
          Iterator<String> pkit = dip.getProxiedKeys().iterator();
          while (pkit.hasNext()) {
            String diKey = pkit.next();
            if (modelKey.equals(diKey)) {
              return (new TreePath(next.getPath()));
            }           
          }
        }
      }
    }
    return (getDefaultSelection());
  }  
  
  /***************************************************************************
  **
  ** Resolve NavNode to DefaultMutableTreeNode node
  */ 
  
  public DefaultMutableTreeNode resolveNavNode(NavNode nodeKey) {
    if (nodeKey == null) {
      return (null);
    }
    if (nodeKey.getNodeID() == null) {
      return ((DefaultMutableTreeNode)this.getRoot());
    } 
    return ((DefaultMutableTreeNode)nodeForNodeIDObj(nodeKey.getNodeID()));
  }
  
  /***************************************************************************
  **
  ** Resolve Node key to tree node
  */ 
  
  public TreeNode resolveNode(XPlatModelNode.NodeKey nodeKey) {
    if ((nodeKey == null) || (nodeKey.id == null)) {
      return (null);
    }
    
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      switch (nodeKey.modType) {
        case SUPER_ROOT:
          break;
        case GROUPING_ONLY:
          String nodeID = getGroupNodeID(next);         
          if ((nodeID != null) && nodeKey.id.equals(nodeID)) {
            return (next);
          }         
          break;
        case DB_GENOME:
        case GENOME_INSTANCE:
        case DYNAMIC_INSTANCE: // Yes this works as a getGenome() case
          String genomeID = getGenomeID(next);
          if ((genomeID != null) && nodeKey.id.equals(genomeID)) {
            return (next);
          } 
          break;
        case DYNAMIC_PROXY:
          String dipID = getDynamicProxyID(next);
          if (dipID != null) {
            if (dipID.equals(nodeKey.id)) {
              return (next);
            }
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Get a preorder listing of the tree, except for the root and (optionally) full genome
  */ 
  
  public List<String> getPreorderListing(boolean skipFull) {
    ArrayList<String> retval = new ArrayList<String>();
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    int doSkip = (skipFull) ? 2 : 1;
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if (doSkip == 0) {
        if (contents.modelID != null) {
          retval.add(contents.modelID);
        }
      } else {
        doSkip--;
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get a preorder listing of the full tree.
  ** This will include IDs for slider nodes as well (the first proxied key)!!!
  */ 
  
  public List<String> getFullTreePreorderListing(DataAccessContext dacx) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    return (getPreorderListing(dmtnRoot.getFirstChild(), dacx));
  }
  
  /***************************************************************************
  **
  ** Get a preorder listing of the tree below (and including) the given node.
  ** This will include IDs for slider nodes as well (the first proxied key)!!!
  */ 
  
  public List<String> getPreorderListing(TreeNode topNode, DataAccessContext dacx) {
    ArrayList<String> retval = new ArrayList<String>();
    DefaultMutableTreeNode topMutNode = (DefaultMutableTreeNode)topNode;
    Enumeration e = topMutNode.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if (contents.modelID != null) {
        retval.add(contents.modelID);
      } else if (contents.proxyID != null) {         
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(contents.proxyID);
        String giKey = dip.getFirstProxiedKey();
        retval.add(giKey);
      }
    }
    return (retval);
  }   
  
  /***************************************************************************
  **
  ** Get a preorder listing of the tree below (and including) the given node.
  ** This does not use model IDs, so works with group nodes as well.
  */ 
  
  public List<NavNode> getPreorderNodeListing(TreeNode topNode) {
    ArrayList<NavNode> retval = new ArrayList<NavNode>();
    DefaultMutableTreeNode topMutNode = (DefaultMutableTreeNode)topNode;
    Enumeration e = topMutNode.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      retval.add(contentsToNavNode(contents));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a preorder listing of the tree below (and including) the given node.
  ** This does not use model IDs, so works with group nodes as well.
  */ 
  
  private NavNode contentsToNavNode(NavNodeContents contents) {
    String mpnID = null;
    boolean match = false;
    if (contents.modelID != null) {
      if ((contents.type == Kids.ROOT_MODEL) || (contents.type == Kids.ROOT_INSTANCE) || (contents.type == Kids.STATIC_CHILD_INSTANCE)) {
        mpnID = contents.modelID;
        match = true;
      } else if (contents.type == Kids.DYNAMIC_SUM_INSTANCE) {
        mpnID = DynamicInstanceProxy.extractProxyID(contents.modelID); // NavNodes using proxyID for sum instance, not embedded in model ID.
        match = true;
      }
    } else if (contents.proxyID != null) {
      mpnID = contents.proxyID;
      match = (contents.type == Kids.DYNAMIC_SLIDER_INSTANCE);
    } else if (contents.nodeID != null) {
      mpnID = contents.nodeID;
      match = (contents.type == Kids.GROUP_NODE);
    } else {
      mpnID = null;
      match = (contents.type == Kids.HIDDEN_ROOT);
    }
    if (!match) {
      throw new IllegalStateException(); 
    }
    NodeID forNew = (contents.nodeID == null) ? null : new NodeID(contents.nodeID);
    return (new NavNode(forNew, contents.type, mpnID, contents.name, new NodeID(contents.parNodeID)));
  }
  
  /***************************************************************************
  **
  ** Get a preorder proxy listing of the tree, except for the root and full genome
  */ 
  
  public List<String> getProxyPreorderListing() {
    ArrayList<String> retval = new ArrayList<String>();
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    int doSkip = 2;
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if (doSkip == 0) {
        if (contents.proxyID != null) {
          retval.add(contents.proxyID);
        }
      } else {
        doSkip--;
      }
    }
    return (retval);
  }   

  /***************************************************************************
  **
  ** Get a list of all the tree paths to internal nodes
  */ 
  
  public List<TreePath> getAllPathsToNonLeaves() {
    ArrayList<TreePath> retval = new ArrayList<TreePath>();
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();
      if (next.getChildCount() != 0) {  
        TreePath path = new TreePath(next.getPath());
        retval.add(path);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Map the given old paths into an identical set of paths through the tree 
  ** held by the navigation change
  */
  
  public List<TreePath> mapAllPaths(List<TreePath> paths, NavTreeChange change, boolean useOld) {
    ArrayList<TreePath> retval = new ArrayList<TreePath>();
    DefaultMutableTreeNode useRoot = useOld ? change.oldRoot : change.newRoot;
    Iterator<TreePath> pit = paths.iterator();
    while (pit.hasNext()) {
      TreePath tp = pit.next();      
      retval.add(mapPath(tp, useRoot));
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Map the given old path into an identical path through the tree 
  ** held by the navigation change
  */
  
  public TreePath mapAPath(TreePath path, NavTreeChange change, boolean useOld) {
    if (path == null) {
      return (null);
    }
    DefaultMutableTreeNode useRoot = useOld ? change.oldRoot : change.newRoot;
    return (mapPath(path, useRoot));
  }  

  /***************************************************************************
  **
  ** Converter function
  */
   
  public List<DefaultMutableTreeNode> pathToList(TreePath path) {
    Object[] objs = path.getPath();
    ArrayList<DefaultMutableTreeNode> retval = new ArrayList<DefaultMutableTreeNode>();
    for (int i = 0; i < objs.length; i++) {
      retval.add((DefaultMutableTreeNode)objs[i]);
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Answer if the given path is present in the tree off the given root
  */
  
  public boolean isPathPresent(TreePath path) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot(); 
    List<DefaultMutableTreeNode> pathTail = pathToList(path);
    pathTail.remove(0);
    return (isPathPresentGuts(pathTail, dmtnRoot));
  }
  
  /***************************************************************************
  **
  ** Map the given old path into an identical path through the tree given 
  ** by the root
  */
  
  public TreePath mapPath(TreePath oldPath, DefaultMutableTreeNode dmtnRoot) {
    ArrayList<DefaultMutableTreeNode> newNodes = new ArrayList<DefaultMutableTreeNode>();
    List<DefaultMutableTreeNode> oldTail = pathToList(oldPath);
    
    oldTail.remove(0);
    newNodes.add(dmtnRoot);
    mapPathGuts(oldTail, dmtnRoot, newNodes);
    
    return (new TreePath(newNodes.toArray()));
  }  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Return value used for traversals.
  */
  
  public static class NavNode {
    private String name_;  
    private String mpnID_;
    private Kids type_;
    private NodeID parent_;
    private NodeID myNodeID_;
    
    NavNode(NodeID myID, Kids type, String mpnID, String name, NodeID parent) {  
      type_ = type;
      mpnID_ = mpnID;
      name_ = name;
      parent_ = parent;
      myNodeID_ = myID;
    }
    
    public NavNode(NodeID myID, XPlatModelNode.NodeKey nkey, String name, NodeID parent, DataAccessContext dacx) {      
      name_ = name;
      parent_ = parent;
      myNodeID_ = myID;
      switch (nkey.modType) {
        case SUPER_ROOT:
          type_ = Kids.HIDDEN_ROOT;
          mpnID_ = nkey.id;
          break;
        case DB_GENOME:
          type_ = Kids.ROOT_MODEL;
          mpnID_ = nkey.id;
          break;
        case GENOME_INSTANCE:
          GenomeSource gsrc = dacx.getGenomeSource();
          GenomeInstance gi = (GenomeInstance)gsrc.getGenome(nkey.id);
          type_ = (gi.getVfgParent() == null) ? Kids.ROOT_INSTANCE : Kids.STATIC_CHILD_INSTANCE;
          mpnID_ = nkey.id;
          break;
        case DYNAMIC_INSTANCE:
          type_ = Kids.DYNAMIC_SUM_INSTANCE;       
          if (!DynamicInstanceProxy.isDynamicInstance(nkey.id)) {
            throw new IllegalArgumentException();
          }
          mpnID_ = DynamicInstanceProxy.extractProxyID(nkey.id);
          break;
        case DYNAMIC_PROXY:
          type_ = Kids.DYNAMIC_SLIDER_INSTANCE;
          mpnID_ = nkey.id;
          break;
        case GROUPING_ONLY:
          type_ = Kids.GROUP_NODE;
          mpnID_ = nkey.id;
          break;
        default:
          throw new IllegalArgumentException();  
      }
      return;
    }
 
    public Kids getType() {
      return (type_);
    }
    
    public String getName() {
      return (name_);
    }
    
    public NodeID getNodeID() {
      return (myNodeID_);
    }
    
    public NodeID getParent() {
      return (parent_);
    }
    
    public String getModelID() {
      if ((type_ == Kids.ROOT_MODEL) ||  (type_ == Kids.ROOT_INSTANCE) || (type_ == Kids.STATIC_CHILD_INSTANCE)) {
         return (mpnID_); 
      }
      throw new IllegalStateException();
    }
    
    //
    // Departure from previous approach! DYNAMIC_SUM_INSTANCE is represented by proxyID, not the single instance ID.
    //
    
    public String getProxyID() {
      if ((type_ == Kids.DYNAMIC_SUM_INSTANCE) ||  (type_ == Kids.DYNAMIC_SLIDER_INSTANCE)) {
         return (mpnID_); 
      }
      throw new IllegalStateException();
    }
    
     public String getGroupID() {
      if (type_ == Kids.GROUP_NODE) {
         return (mpnID_); 
      }
      throw new IllegalStateException();
    }
     
    @Override
    public int hashCode() {
      return (mpnID_.hashCode() + type_.hashCode());
    }   
    
    @Override
    public boolean equals(Object other) {    
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof NavNode)) {
        return (false);
      }
      
      NavNode otherNN = (NavNode)other;
      if (this.type_ != otherNN.type_) {
        return (false);
      }
      if (this.mpnID_ == null) {
        return (otherNN.mpnID_ == null);
      }
      return (this.mpnID_.equals(otherNN.mpnID_));
    } 
  }

  /***************************************************************************
  **
  ** This is the user object in the tree
  */
  
  public static class NavNodeContents implements Cloneable {
    String name;  
    String modelID;
    String proxyID;
    Kids type;
    String nodeID;
    String parNodeID;
    private ArrayList<GroupNodeEntry> grpEntries_;
    
    public NavNodeContents(Kids type, String name, String id, String proxyID, String nodeID, String parNodeID) {
      this.type = type;
      this.name = name;
      this.modelID = id;
      this.proxyID = proxyID;
      this.nodeID = nodeID;
      this.parNodeID = parNodeID;
    }
    
    public NavNodeContents(NavNodeContents other) {
      this.type = other.type;
      this.name = other.name;
      this.modelID = other.modelID;
      this.proxyID = other.proxyID;
      this.nodeID = other.nodeID;
      this.parNodeID = other.parNodeID;
      if (other.grpEntries_ != null) {
        this.grpEntries_ = new ArrayList<GroupNodeEntry>();
        for (GroupNodeEntry gne : other.grpEntries_) {
          this.grpEntries_.add(gne.clone());
        }
      }
    }
    
    void setImageID(String imageID, boolean forMap) {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }      
      if (grpEntries_ == null) {
        grpEntries_ = new ArrayList<GroupNodeEntry>();
      }
      if (grpEntries_.isEmpty()) {
        grpEntries_.add(new GroupNodeEntry(null, null, null, null));
      }
      
      // If we allow more than one, this needs to be fixed!
      GroupNodeEntry gne = grpEntries_.get(0);
      gne.imageID = (forMap) ? gne.imageID : imageID;
      gne.mapImageID = (forMap) ? imageID : gne.mapImageID;
      return;
    }
    
    String getImageID(boolean forMap) {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if (grpEntries_ == null) {
        return (null);
      }
       // If we allow more than one, this needs to be fixed!
      GroupNodeEntry gne = grpEntries_.get(0);
      return ((forMap) ? gne.mapImageID : gne.imageID);
    }
    
    Map<Color, GroupNodeMapEntry> getModelMap() {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if (grpEntries_ == null) {
        return (null);
      }
      // If we allow more than one, this needs to be fixed!
      GroupNodeEntry gne = grpEntries_.get(0);
      return (gne.grpMapEntries);
    }
    
    void setModelMap(Map<Color, GroupNodeMapEntry> modMap) {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if ((grpEntries_ == null) || (grpEntries_.size() != 1)) {
        throw new IllegalStateException();
      }
      GroupNodeEntry gne = grpEntries_.get(0);
      if (gne.grpMapEntries == null) {
        gne.grpMapEntries = new HashMap<Color, GroupNodeMapEntry>();
      }
      gne.grpMapEntries.putAll(modMap);
      return;
    }

    void addGroupEntry(GroupNodeEntry ge) {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if (grpEntries_ == null) {
        grpEntries_ = new ArrayList<GroupNodeEntry>();
      }
      // If we allow more than one, this needs to be fixed!
      if (!grpEntries_.isEmpty()) {
        throw new IllegalStateException();
      }
      grpEntries_.add(ge);
      return;
    }
    
   void replaceGroupEntry(GroupNodeEntry ge) {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if (grpEntries_ == null) {
        throw new IllegalStateException();
      }
      // If we allow more than one, this needs to be fixed!
      if (grpEntries_.isEmpty()) {
        throw new IllegalStateException();
      }
      grpEntries_.clear();
      grpEntries_.add(ge);
      return;
    }
    
    GroupNodeEntry getGroupEntry() {
      if (type != Kids.GROUP_NODE) {
        throw new IllegalStateException();
      }
      if ((grpEntries_ == null) || grpEntries_.isEmpty()) {
        return (null);
      }
      // If we allow more than one, this needs to be fixed!
     
      return ( grpEntries_.get(0));
    }

    
    @Override
    public int hashCode() {
      return (nodeID.hashCode());
    }
     
    //
    // THIS RIGHT HERE is how the JTree knows how to draw the node name (DefaultMutableTreeNode asks user object to return toString())
    //
   
    @Override
    public String toString() {
      // FOR DEBUG return (super.toString() + " " + name + " " + id + " " + proxyID);
      return (name);
    }
    
    @Override
    public boolean equals(Object other) {
      
      if (!shallowEquals(other)) {
        return (false);
      }
      NavNodeContents otherNNC = (NavNodeContents)other;
      if (this.parNodeID == null) {
        if (otherNNC.parNodeID != null) {
          return (false);
        }
      } else if (!this.parNodeID.equals(otherNNC.parNodeID)) {
        return (false);
      }
      
      if ((this.grpEntries_ == null) || this.grpEntries_.isEmpty()) {
        if ((otherNNC.grpEntries_ != null) && !otherNNC.grpEntries_.isEmpty()) {
          return (false);
        }
      } else {
        if ((otherNNC.grpEntries_ == null) || otherNNC.grpEntries_.isEmpty()) {
          return (false);
        }
        // If we allow more than one, this needs to be fixed!
        if (!this.grpEntries_.get(0).equals(otherNNC.grpEntries_.get(0))) {
          return (false);
        }
      }
      return (this.nodeID.equals(otherNNC.nodeID));
    }

    public boolean shallowEquals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof NavNodeContents)) {
        return (false);
      }
      NavNodeContents otherNNC = (NavNodeContents)other;
      if (this.type != otherNNC.type) {
        return (false);
      }
      if (this.name == null) {
        if (otherNNC.name != null) {
          return (false);
        }
      } else if (!this.name.equals(otherNNC.name)) {
        return (false);
      }
      if (this.modelID == null) {
        if (otherNNC.modelID != null) {
          return (false);
        }
      } else if (!this.modelID.equals(otherNNC.modelID)) {
        return (false);
      }
      if (this.proxyID == null) {
        if (otherNNC.proxyID != null) {
          return (false);
        }
      } else if (!this.proxyID.equals(otherNNC.proxyID)) {
        return (false);
      }
      return (true);
    }

    public void writeXML(PrintWriter out, Indenter ind) {  
      ind.indent();     
      out.print("<navTreeNode type=\""); 
      out.print(type.getTag());
      if (name != null) {
        out.print("\" name=\"");
        out.print(CharacterEntityMapper.mapEntities(name, false));
      }
      if (modelID != null) {
        out.print("\" modelID=\"");
        out.print(modelID);
      }
      if (proxyID != null) {
        out.print("\" proxyID=\"");
        out.print(proxyID);
      }
      out.print("\" nodeID=\"");
      out.print(nodeID);
      
      if (parNodeID != null) {
        out.print("\" parNodeID=\"");
        out.print(parNodeID);
      }
      
      if (grpEntries_ != null) {
        out.println("\" >");
        ind.up();
        for (GroupNodeEntry gne : grpEntries_) {
          gne.writeXML(out, ind);
        }
        ind.down().indent();
        out.println("</navTreeNode>"); 
      } else {
        out.println("\" />");
      }
    }
    
    @Override
    public NavNodeContents clone() {
      try {
        NavNodeContents retval = (NavNodeContents)super.clone();
        if (this.grpEntries_ != null) {
          retval.grpEntries_ = new ArrayList<GroupNodeEntry>();
          for (GroupNodeEntry gne : this.grpEntries_) {
            retval.grpEntries_.add(gne.clone());
          }
        }
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }     
  }
  
  /***************************************************************************
  **
  ** This is a navigation link to a model from a group node
  */
  
  public static class GroupNodeEntry implements Cloneable {
    String imageID;
    String mapImageID;
    String modelID;
    String proxyID;
    HashMap<Color, GroupNodeMapEntry> grpMapEntries;
    
    public GroupNodeEntry(String imageID, String mapImageID, String modelID, String proxyID) {
      this.imageID = imageID;
      this.mapImageID = mapImageID;
      this.modelID = modelID;
      this.proxyID = proxyID;
    }
    
    public GroupNodeEntry(GroupNodeEntry other) {
      this.imageID = other.imageID;
      this.mapImageID = other.mapImageID;
      this.modelID = other.modelID;
      this.proxyID = other.proxyID;
      if (other.grpMapEntries != null) {
        this.grpMapEntries = new HashMap<Color, GroupNodeMapEntry>();
        for (Color col : other.grpMapEntries.keySet()) {
          this.grpMapEntries.put(col, other.grpMapEntries.get(col));
        }
      } 
    } 
    
    public Map<Color, GroupNodeMapEntry> getModelMap() {
      return (grpMapEntries);
    }
    
    void addMapEntry(GroupNodeMapEntry ge) {
      if (grpMapEntries == null) {
        grpMapEntries = new HashMap<Color, GroupNodeMapEntry>();
      }
      grpMapEntries.put(ge.color, ge);
      return;
    }
    
    @Override
    public int hashCode() {
      return (((imageID != null) ? imageID.hashCode() : 0) + 
              ((mapImageID != null) ? mapImageID.hashCode() : 0) + 
              ((modelID != null) ? modelID.hashCode() : 0) + 
              ((proxyID != null) ? proxyID.hashCode() : 0));
    }
      
    @Override
    public String toString() {
      // FOR DEBUG return (super.toString() + " " + name + " " + id + " " + proxyID);
      return (imageID);
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof GroupNodeEntry)) {
        return (false);
      }
      GroupNodeEntry otherNNC = (GroupNodeEntry)other;
      if (this.imageID == null) {
        if (otherNNC.imageID != null) {
          return (false);
        }
      } else if (!this.imageID.equals(otherNNC.imageID)) {
        return (false);
      }
      if (this.mapImageID == null) {
        if (otherNNC.mapImageID != null) {
          return (false);
        }
      } else if (!this.mapImageID.equals(otherNNC.mapImageID)) {
        return (false);
      }
      if (this.modelID == null) {
        if (otherNNC.modelID != null) {
          return (false);
        }
      } else if (!this.modelID.equals(otherNNC.modelID)) {
        return (false);
      }
      if (this.proxyID == null) {
        if (otherNNC.proxyID != null) {
          return (false);
        }
      } else if (!this.proxyID.equals(otherNNC.proxyID)) {
        return (false);
      }
      if ((this.grpMapEntries == null) || this.grpMapEntries.isEmpty()) {
        if ((otherNNC.grpMapEntries != null) && !otherNNC.grpMapEntries.isEmpty()) {
          return (false);
        }
      } else {
        if ((otherNNC.grpMapEntries == null) || otherNNC.grpMapEntries.isEmpty()) {
          return (false);
        }
        if (!this.grpMapEntries.equals(otherNNC.grpMapEntries)) {
          return (false);
        }
      }
      return (true);
    }
      
    public void writeXML(PrintWriter out, Indenter ind) {  
      ind.indent();     
      out.print("<grpNodeEntry"); 
      if (imageID != null) {
        out.print(" imageID=\"");
        out.print(imageID);
        out.print("\"");
      }   
      if (mapImageID != null) {
        out.print(" mapImageID=\"");
        out.print(mapImageID);
        out.print("\"");
      }   
      if (modelID != null) {
        out.print(" modelID=\"");
        out.print(modelID);
        out.print("\"");
      }
      if (proxyID != null) {
        out.print(" proxyID=\"");
        out.print(proxyID);
        out.print("\"");
      }
      
     if (grpMapEntries != null) {
        out.println(" >");
        ind.up();
        
        // Fix the order!
        int count = 0;
        TreeMap<NamedColor, GroupNodeMapEntry> sorted = new TreeMap<NamedColor, GroupNodeMapEntry>();
        for (Color col : grpMapEntries.keySet()) {
          GroupNodeMapEntry gnme = grpMapEntries.get(col);
          sorted.put(new NamedColor(Integer.toString(count++), col, gnme.idTag()), gnme);
        }

        for (GroupNodeMapEntry gnme : sorted.values()) {
          gnme.writeXML(out, ind);
        }
        ind.down().indent();
        out.println("</grpNodeEntry>"); 
      } else {
        out.println(" />");
      }
    }

    @Override
    public GroupNodeEntry clone() {
      try {
        GroupNodeEntry retval = (GroupNodeEntry)super.clone();
        if (this.grpMapEntries != null) {
          retval.grpMapEntries = new HashMap<Color, GroupNodeMapEntry>();
          for (Color key : this.grpMapEntries.keySet()) {
            retval.grpMapEntries.put(key, this.grpMapEntries.get(key).clone());
          }
        }
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }    
  }
  
  /***************************************************************************
  **
  ** This is a navigation link to a model from a group node
  */
  
  public static class GroupNodeMapEntry implements Cloneable {
    public Color color;
    public String tabID;
    public String modelID;
    public String proxyID;
    public Integer proxyTime;
    public String regionID;
    
    public GroupNodeMapEntry(Color col, String tabID, String modelID, String proxyID, Integer proxyTime, String regionID) {
      this.color = col;
      this.tabID = tabID;
      this.modelID = modelID;
      this.proxyID = proxyID;
      this.proxyTime = proxyTime;
      this.regionID = regionID;
    }
    
    public GroupNodeMapEntry(Color col) {
      this.color = col;
    }

    public GroupNodeMapEntry(GroupNodeMapEntry other) {
      this.color = other.color;
      this.tabID = other.tabID;
      this.modelID = other.modelID;
      this.proxyID = other.proxyID;
      this.proxyTime = other.proxyTime;
      this.regionID = other.regionID;
    }    
   
    @Override
    public int hashCode() {
      return (color.hashCode() + 
              ((proxyTime != null) ? proxyTime.hashCode() : 0) + 
              ((tabID != null) ? tabID.hashCode() : 0) + 
              ((modelID != null) ? modelID.hashCode() : 0) + 
              ((regionID != null) ? regionID.hashCode() : 0) + 
              ((proxyID != null) ? proxyID.hashCode() : 0));
    }
      
    @Override
    public String toString() {
      return (color.toString() + " " + modelID + " " + proxyID + " " + proxyTime  + " " + regionID + " " + tabID);
    }
    
    public String idTag() {
      return ((modelID != null) ? modelID : proxyID + "-" + proxyTime);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof GroupNodeMapEntry)) {
        return (false);
      }
      GroupNodeMapEntry otherNNC = (GroupNodeMapEntry)other;
      if (!this.color.equals(otherNNC.color)) {
         return (false);
      }
      
      if (this.modelID == null) {
        if (otherNNC.modelID != null) {
          return (false);
        }
      } else if (!this.modelID.equals(otherNNC.modelID)) {
        return (false);
      }
      if (this.proxyID == null) {
        if (otherNNC.proxyID != null) {
          return (false);
        }
      } else if (!this.proxyID.equals(otherNNC.proxyID)) {
        return (false);
      }
      if (this.proxyTime == null) {
        if (otherNNC.proxyTime != null) {
          return (false);
        }
      } else if (!this.proxyTime.equals(otherNNC.proxyTime)) {
        return (false);
      }
      if (this.regionID == null) {
        if (otherNNC.regionID != null) {
          return (false);
        }
      } else if (!this.regionID.equals(otherNNC.regionID)) {
        return (false);
      }
      if (this.tabID == null) {
        if (otherNNC.tabID != null) {
          return (false);
        }
      } else if (!this.tabID.equals(otherNNC.tabID)) {
        return (false);
      }
      return (true);
    }
      
    public void writeXML(PrintWriter out, Indenter ind) {  
      ind.indent();     
      out.print("<grpNodeMapEntry r=\"");
      out.print(color.getRed());
      out.print("\" g=\"");
      out.print(color.getGreen());
      out.print("\" b=\"");
      out.print(color.getBlue());
      out.print("\" ");
      if (tabID != null) {
        out.print(" tabID=\"");
        out.print(tabID);
        out.print("\"");
      }
      if (modelID != null) {
        out.print(" modelID=\"");
        out.print(modelID);
        out.print("\"");
      }
      if (proxyID != null) {
        out.print(" proxyID=\"");
        out.print(proxyID);
        out.print("\"");
      }
      if (proxyTime != null) {
        out.print(" proxyTime=\"");
        out.print(proxyTime);
        out.print("\"");
      }  
      if (regionID != null) {
        out.print(" regionID=\"");
        out.print(regionID);
        out.print("\"");
      }   
      out.println(" />"); 
    }

    @Override
    public GroupNodeMapEntry clone() {
      try {
        GroupNodeMapEntry retval = (GroupNodeMapEntry)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Make maps from the tree
  */ 
  
  private void makeMaps(DefaultMutableTreeNode node, Map<ModelID, NodeID> modelMap, Map<NodeID, DefaultMutableTreeNode> nodeMap) {
    NavNodeContents nnc = (NavNodeContents)node.getUserObject();
    NodeID nID = new NodeID(nnc.nodeID);
    nodeMap.put(nID, node);
    if (!(nnc.type == Kids.HIDDEN_ROOT) && !(nnc.type == Kids.DYNAMIC_SLIDER_INSTANCE) && !(nnc.type == Kids.GROUP_NODE)) {
      modelMap.put(new ModelID(nnc.modelID), nID);
    }
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      makeMaps(child, modelMap, nodeMap);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Deep copy the tree
  */ 
  
  private DefaultMutableTreeNode deepCopyRoot() {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)getRoot();
    return (deepCopyGuts(dmtnRoot));
  }
  
  /***************************************************************************
  **
  ** Recursive deep copy
  */
  
  private DefaultMutableTreeNode deepCopyGuts(DefaultMutableTreeNode node) {
    DefaultMutableTreeNode retval = new DefaultMutableTreeNode();
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      DefaultMutableTreeNode childCopy = deepCopyGuts(child);
      retval.add(childCopy);
    }
    NavNodeContents nc = new NavNodeContents((NavNodeContents)node.getUserObject());
    retval.setUserObject(nc);
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Recursive ID gatherer
  */
  
  private void getChildNodeAndModelIDs(DefaultMutableTreeNode node, Set<NodeID> nodeIDs, Set<ModelID> modelIDs) {
    NavNodeContents nc = (NavNodeContents)node.getUserObject();
    nodeIDs.add(new NodeID(nc.nodeID));
    if (nc.modelID != null) {
      modelIDs.add(new ModelID(nc.modelID));
    }
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      getChildNodeAndModelIDs(child, nodeIDs, modelIDs);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Recursive guts of path mapping
  */
  
  private void mapPathGuts(List<DefaultMutableTreeNode> oldTail, DefaultMutableTreeNode node, 
                           List<DefaultMutableTreeNode> newHead) {
    
    if (oldTail.size() == 0) {
      return;
    }
    DefaultMutableTreeNode oldNode = oldTail.get(0);
    NavNodeContents oldContents = (NavNodeContents)oldNode.getUserObject();
    int count = node.getChildCount();
    if (count == 0) {
      return;
    }
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      NavNodeContents childContents = (NavNodeContents)child.getUserObject(); 
      if (childContents.equals(oldContents)) {
        oldTail.remove(0);
        newHead.add(child);
        mapPathGuts(oldTail, child, newHead);
        return;
      }
    }
    System.err.println("No match with " + oldContents);
    throw new IllegalArgumentException();
  }
  
  
  /***************************************************************************
  **
  ** Recursive guts of path test
  */
  
  private boolean isPathPresentGuts(List<DefaultMutableTreeNode> pathTail, DefaultMutableTreeNode node) {
    int count = node.getChildCount();
    int ptSize = pathTail.size();
    if (count == 0) {
      return (ptSize == 0);
    } else if (ptSize == 0) {
      return (true);
    }
    
    DefaultMutableTreeNode oldNode = pathTail.get(0);
    NavNodeContents oldContents = (NavNodeContents)oldNode.getUserObject();

    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      NavNodeContents childContents = (NavNodeContents)child.getUserObject(); 
      if (childContents.equals(oldContents)) {
        pathTail.remove(0);
        return (isPathPresentGuts(pathTail, child));
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Generate a cross-platform tree description
  */ 
   
  public XPlatModelTree getXPlatTree(DataAccessContext dacx) {   
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)getRoot();
    XPlatModelNode root = deepXPlatGuts(dmtnRoot, dacx);
    Rectangle allBounds = uics_.getZoomCommandSupport().allModelsBounds();
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    return (new XPlatModelTree(root, dacx.getDisplayOptsSource().getDisplayOptions(), allBounds, 
                               ((tad == null || !tad.isInitialized()) ? null : new XPlatTimeAxisDefinition(tad, dacx))));
  }

  /***************************************************************************
  **
  ** Get an xplat key for a tree node 
  */
      
  public XPlatModelNode.NodeKey treeNodeToXPlatKey(DefaultMutableTreeNode node, DataAccessContext dacx) {
    NavNodeContents nc = (NavNodeContents)node.getUserObject();
    GenomeSource gSrc = dacx.getGenomeSource();
    
    if ((nc == null) || ((nc.modelID == null) && (nc.proxyID == null) && (nc.type != NavTree.Kids.GROUP_NODE))) {
      return (null);
    } else { 
      if (nc.type == NavTree.Kids.GROUP_NODE) {
        return (new XPlatModelNode.NodeKey(nc.nodeID, XPlatModelNode.ModelType.GROUPING_ONLY));
      } else if (nc.proxyID != null) {       
        return (new XPlatModelNode.NodeKey(nc.proxyID, XPlatModelNode.ModelType.DYNAMIC_PROXY));
      } else if (DynamicInstanceProxy.isDynamicInstance(nc.modelID)) {
        return (new XPlatModelNode.NodeKey(nc.modelID, XPlatModelNode.ModelType.DYNAMIC_INSTANCE));
      } else if (gSrc.getGenome(nc.modelID) instanceof DBGenome) {
        return (new XPlatModelNode.NodeKey(nc.modelID, XPlatModelNode.ModelType.DB_GENOME));
      } else {
        return (new XPlatModelNode.NodeKey(nc.modelID, XPlatModelNode.ModelType.GENOME_INSTANCE));
      }
    }
  }

  /***************************************************************************
  **
  ** Recursive deep generation of a cross-platform tree description
  */
      
  private XPlatModelNode deepXPlatGuts(DefaultMutableTreeNode node, DataAccessContext dacx) {
    NavNodeContents nc = (NavNodeContents)node.getUserObject();
    XPlatModelNode.ModelType mt = null;
    String useID = null;
    MinMax timeRange = null;

    boolean hasImages = false;
    NetOverlayOwner noo = null;
    GenomeSource gSrc = dacx.getGenomeSource();
    String genID = null;
    boolean isGroup = false;
    
    if ((nc == null) || (nc.type == Kids.HIDDEN_ROOT)) {
      mt = XPlatModelNode.ModelType.SUPER_ROOT;
    } else {  
      useID = nc.modelID;
      genID = useID;
      if (nc.type == NavTree.Kids.GROUP_NODE) {
        mt = XPlatModelNode.ModelType.GROUPING_ONLY;
        isGroup = true;
        useID = nc.nodeID;
      } else if (nc.proxyID != null) {       
        mt = XPlatModelNode.ModelType.DYNAMIC_PROXY;
        useID = nc.proxyID;
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(useID);
        timeRange = new MinMax(dip.getMinimumTime(), dip.getMaximumTime());
        noo = dip;
        hasImages = dip.hasGenomeImage();
        genID = dip.getFirstProxiedKey();
      } else if (DynamicInstanceProxy.isDynamicInstance(nc.modelID)) {
        mt = XPlatModelNode.ModelType.DYNAMIC_INSTANCE;
        Genome gen = gSrc.getGenome(useID);
        noo = gen;
        hasImages = (gen.getGenomeImage() != null);
      } else if (gSrc.getGenome(useID) instanceof DBGenome) {
        mt =  XPlatModelNode.ModelType.DB_GENOME;
        Genome gen = gSrc.getGenome(useID);
        noo = gen;
        hasImages = (gen.getGenomeImage() != null);
      } else {
        mt = XPlatModelNode.ModelType.GENOME_INSTANCE;
        Genome gen = gSrc.getGenome(useID);
        noo = gen;
        hasImages = (gen.getGenomeImage() != null);
      }
    }
    
    boolean hasOverlays = (noo == null) ? false : (noo.getNetworkOverlayCount() > 0);
       
    XPlatModelNode retval = (mt == XPlatModelNode.ModelType.DYNAMIC_PROXY) ? new XPlatModelNode(useID, nc.name, mt, timeRange, hasImages, hasOverlays) 
                                                                           : new XPlatModelNode(useID, nc.name, mt, hasImages, hasOverlays);   
   
    String anID = getGenomeModelAncestorID(node);
    
    DataAccessContext dacx4N = (genID == null) ? dacx : new StaticDataAccessContext(dacx, genID, dacx.getLayoutSource().getLayoutForGenomeKey(genID).getID());
    retval.fillOverlayInfoForNode(dacx4N);
        
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      XPlatModelNode childXPlat = deepXPlatGuts(child, dacx);
      retval.addChild(childXPlat);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Undo a group image change
  */ 
  
  public void undoImageChange(ImageChange undo) {
    ImageManager mgr = uics_.getImageMgr();
    String useKey = "";
    mgr.changeUndo(undo);
    if (undo.countOnlyKey != null) {
      useKey = (undo.newCount > undo.oldCount) ? null : undo.countOnlyKey;
    } else if (undo.newKey != null) {
      useKey = null;
    } else if (undo.oldKey != null) {
      useKey = undo.oldKey;
    }
    if (!useKey.equals("")) {
      TreeNode tn = nodeForNodeID(undo.groupNodeKey);
      DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tn;
      NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
      nnc.setImageID(useKey, undo.groupNodeForMap);
    }
    return;
  }

  /***************************************************************************
  **
  ** Undo a contents change
  */ 
  
  public void undoContentChange(NavTreeChange undo) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if ((contents != null) && (undo.newContents.equals(contents))) {
        next.setUserObject(undo.oldContents);
        nodeChanged(next);
        return;
      }
    }
    
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Undo a group image change
  */ 
  
  
  public void redoImageChange(ImageChange redo) {
    ImageManager mgr = uics_.getImageMgr();
    mgr.changeRedo(redo);
    String useKey = "";
    if (redo.countOnlyKey != null) {
      useKey = (redo.oldCount > redo.newCount) ? null : redo.countOnlyKey;
      return;
    } else if (redo.newKey != null) {
      useKey =  redo.newKey;
      return;
    } else if (redo.oldKey != null) {
      useKey = null;
      return;
    }
    if (!useKey.equals("")) {
      TreeNode tn = nodeForNodeID(redo.groupNodeKey);
      DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tn;
      NavNodeContents nnc = (NavNodeContents)dmtn.getUserObject();
      nnc.setImageID(useKey, redo.groupNodeForMap);
    }
  }
  

  /***************************************************************************
  **
  ** Redo a contents change
  */ 
  
  public void redoContentChange(NavTreeChange redo) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if ((contents != null) && (redo.oldContents.equals(contents))) {
        next.setUserObject(redo.newContents);
        nodeChanged(next);
        return;
      }
    }
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();     
    out.println("<navTree>");          
    ind.up();    
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if (contents.type != Kids.HIDDEN_ROOT) {
        contents.writeXML(out, ind);
      }
    }
    ind.down().indent();
    out.println("</navTree>");
    return;
  }
  
  /***************************************************************************
  **
  ** For type-safe map usage
  */  
      
  public static class NodeID  {
    public String nodeID;
    
    public NodeID(String nodeID) {
      if (nodeID == null) {
        throw new IllegalArgumentException();
      }
      this.nodeID = nodeID;
    }
    
    @Override
    public int hashCode() {
      return (nodeID.hashCode());
    }
  
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (!(other instanceof NodeID)) {
        return (false);
      }
      NodeID otherNID = (NodeID)other;
      return (this.nodeID.equals(otherNID.nodeID));
    } 
  }
  
  /***************************************************************************
  **
  ** For type-safe map usage
  */  
      
  public static class ModelID  {
    public String modelID;
    
    public ModelID(String modelID) {
      if (modelID == null) {
        throw new IllegalArgumentException();
      }
      this.modelID = modelID;
    }
    
    @Override
    public int hashCode() {
      return (modelID.hashCode());
    }
  
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (!(other instanceof ModelID)) {
        return (false);
      }
      ModelID otherNID = (ModelID)other;
      return (this.modelID.equals(otherNID.modelID));
    }    
  } 
  
  /***************************************************************************
  **
  ** For bundled returns
  */  
      
  public static class NodeAndChanges  {
    public TreeNode node;
    public NavTreeChange ntc;
    public NodeID newID;
    public List<ImageChange> icsl;
       
    public NodeAndChanges(TreeNode node, NodeID newID, NavTreeChange ntc) {
      this.node = node;
      this.ntc = ntc;
      this.newID = newID;
      icsl = new ArrayList<ImageChange>();
    }
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NavTreeWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    
    public NavTreeWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("navTree");
      installWorker(new TreeNodeWorker(whiteboard), new MyGlue());  
    }
    
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("navTree")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.navTree = (new StaticDataAccessContext(dacx_).getContextForRoot()).getGenomeSource().getModelHierarchy();
        board.navTree.setLegacyGlue(false);
        retval = board.navTree;
      }
      return (retval);     
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TreeNodeWorker extends AbstractFactoryClient {
    
    public TreeNodeWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("navTreeNode");
      installWorker(new GroupNodeEntryWorker(whiteboard), new MyGrpGlue());  
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("navTreeNode")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nnc = buildFromXML(elemName, attrs);
        retval = board.nnc;
      }
      return (retval);     
    }
    
    private NavNodeContents buildFromXML(String elemName, Attributes attrs) throws IOException {    
      String typeStr = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "type", true);
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "name", false);
      String modelID = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "modelID", false);
      String proxyID = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "proxyID", false);
      String nodeID = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "nodeID", true);
      String parNodeID = AttributeExtractor.extractAttribute(elemName, attrs, "navTreeNode", "parNodeID", false);
      Kids type = mapFromTypeTag(typeStr);
      return (new NavNodeContents(type, name, modelID, proxyID, nodeID, parNodeID));
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class GroupNodeEntryWorker extends AbstractFactoryClient {
    
    public GroupNodeEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("grpNodeEntry");
      installWorker(new GroupNodeMapEntryWorker(whiteboard), new MyGrpMapGlue()); 
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("grpNodeEntry")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.cge = buildFromXML(elemName, attrs);
        retval = board.cge;
      }
      return (retval);     
    }
    
    private GroupNodeEntry buildFromXML(String elemName, Attributes attrs) throws IOException {    
      String imageID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeEntry", "imageID", false);
      String mapImageID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeEntry", "mapImageID", false);
      String modelID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeEntry", "modelID", false);
      String proxyID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeEntry", "proxyID", false);
      return (new GroupNodeEntry(imageID, mapImageID, modelID, proxyID));
    }
  }
  

  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class GroupNodeMapEntryWorker extends AbstractFactoryClient {
    
    public GroupNodeMapEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("grpNodeMapEntry");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("grpNodeMapEntry")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.cgme = buildFromXML(elemName, attrs);
        retval = board.cge;
      }
      return (retval);     
    }
    
    private GroupNodeMapEntry buildFromXML(String elemName, Attributes attrs) throws IOException {    
      String rVal = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "r", true);
      String gVal = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "g", true);
      String bVal = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "b", true);
      String tabID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "tabID", false);
      String modelID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "modelID", false);
      String proxyID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "proxyID", false);
      String proxyTime = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "proxyTime", false);
      String regID = AttributeExtractor.extractAttribute(elemName, attrs, "grpNodeMapEntry", "regionID", false);
      
      int red = -1;
      int green = -1;
      int blue = -1;
      Integer proxyObj = null;
      try {
        red = Integer.parseInt(rVal);
        green = Integer.parseInt(gVal);    
        blue = Integer.parseInt(bVal);
        if (proxyTime != null) {
          proxyObj = Integer.valueOf(proxyTime);
        }
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
      if ((red < 0) || (green < 0) || (blue < 0) || (red > 255) || (green > 255) || (blue > 255)) {
        throw new IOException();
      }
      Color mapCol = new Color(red, green, blue);
      if ((modelID == null) && (proxyID == null)) {
        throw new IOException();
      }
      if ((proxyID == null) && (proxyObj != null)) {
        throw new IOException();
      }

      return (new GroupNodeMapEntry(mapCol, tabID, modelID, proxyID, proxyObj, regID));
    }
  }
  
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NavTree tree = board.navTree;
      NavNodeContents currNode = board.nnc;
      tree.addNode(currNode);
      return (null);
    }
  }
  
  public static class MyGrpGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NavNodeContents currNode = board.nnc;
      GroupNodeEntry currEntry = board.cge;
      try {
        currNode.addGroupEntry(currEntry);
      } catch (IllegalStateException isx) {
        throw new IOException();
      }
      return (null);
    }
  }

  public static class MyGrpMapGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      GroupNodeEntry currEntry = board.cge;
      GroupNodeMapEntry currMapEntry = board.cgme;
      try {
        currEntry.addMapEntry(currMapEntry);
      } catch (IllegalStateException isx) {
        throw new IOException();
      }
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Map types to type tags
  */

  public static String mapToTypeTag(Kids type) {
    return (type.getTag());
  }
  
  /***************************************************************************
  **
  ** Map type tags to types
  */

  public static Kids mapFromTypeTag(String tag) {
     for (Kids daType : Kids.values()) {
       if (daType.getTag().equals(tag)) {
         return (daType);
       }
    }
    throw new IllegalArgumentException();
  }
}
