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


package org.systemsbiology.biotapestry.nav;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.MinMax;

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

  public static final int NO_FLAG     = 0;   
  public static final int SKIP_EVENT  = 1;    
  public static final int SKIP_FINISH = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<String, DefaultMutableTreeNode> parentMap_;
  private boolean doingUndo_;
  private int skipFlag_;
  private BTState appState_;
  
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
  
  public NavTree(BTState appState) {
    super(new DefaultMutableTreeNode());
    appState_ = appState;
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    NavNodeContents contents = new NavNodeContents("", null, null);
    dmtnRoot.setUserObject(contents);    
    parentMap_ = new HashMap<String, DefaultMutableTreeNode>();
    doingUndo_ = false;
    skipFlag_ = NO_FLAG;
  }

  /***************************************************************************
  **
  ** Empty out the NavTree
  */ 
  
  public void clearOut() {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
    dmtnRoot.removeAllChildren();
    parentMap_ = new HashMap<String, DefaultMutableTreeNode>();
    if (!appState_.isHeadless()) {
      nodeStructureChangedWithoutFlow(dmtnRoot);
    } else {
      TreeNode[] tn = ((DefaultMutableTreeNode)dmtnRoot).getPath();
      TreePath tp = new TreePath(tn);
      VirtualModelTree vmt = appState_.getTree();
      if (vmt != null) {
        vmt.setTreeSelectionPath(tp); 
      }
    }
    return;
  }
      
  /***************************************************************************
  **
  ** wrapper
  */ 
  
  public void nodeStructureChangedWithoutFlow(TreeNode dmtnRoot) {
    VirtualModelTree vmt = appState_.getTree();
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
  
  public void setSkipFlag(int flag) {
    skipFlag_ = flag;
    return;
  }
  
  /***************************************************************************
  **
  ** Skip flag is used to avoid posting/sinking some programmatic change events
  */ 
  
  public int getSkipFlag() {
    return (skipFlag_);
  }  

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NavTreeChange undo) {
    if (undo.oldMap != null) {
      doingUndo_ = true;
      this.setRoot(undo.oldRoot);
      parentMap_ = undo.oldMap;
      if (!appState_.isHeadless()) {
        nodeStructureChangedWithoutFlow((TreeNode)this.getRoot());
      }
      doingUndo_ = false;
    } else {
      undoContentChange(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NavTreeChange undo) {
    if (undo.newMap != null) {
      doingUndo_ = true;
      this.setRoot(undo.newRoot);
      parentMap_ = undo.newMap;
      if (!appState_.isHeadless()) {
        nodeStructureChangedWithoutFlow((TreeNode)this.getRoot());
      }
      doingUndo_ = false;
    } else {
      redoContentChange(undo);      
    }
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
  ** Add a node to the Nav Tree with the given information
  */ 
  
  public NavTreeChange addNode(String name, String parentID, String id) {
    int index;
    if (name == null) {  // This is the full genome case
      index = 0;
    } else if (parentID == null) {  // This is a VFG case            
      DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)dmtnRoot.getFirstChild();
      index = parentNode.getChildCount();
    } else {
      DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
      index = parentNode.getChildCount();
    }       
    return (addNodeAtIndex(name, parentID, id, index));  
  }
  
  /***************************************************************************
  **
  ** Add a node to the Nav Tree with the given information at the set index
  */ 
  
  public NavTreeChange addNodeAtIndex(String name, String parentID, String id, int index) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldMap = makeParentMap(retval.oldRoot);    
    DefaultMutableTreeNode newNode;
    if (name == null) {  // This is the full genome case
      DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
      String fullGenLabel = appState_.getRMan().getString("tree.FullGenome");
      NavNodeContents contents = new NavNodeContents(fullGenLabel, id, null);
      newNode = new DefaultMutableTreeNode(contents);      
      this.insertNodeInto(newNode, dmtnRoot, index);
    } else if (parentID == null) {  // This is a VFG case
      NavNodeContents contents = new NavNodeContents(name, id, null);
      newNode = new DefaultMutableTreeNode(contents);
      DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)dmtnRoot.getFirstChild();
      this.insertNodeInto(newNode, parentNode, index);
      parentMap_.put(id, newNode);
    } else {
      NavNodeContents contents = new NavNodeContents(name, id, null);  
      newNode = new DefaultMutableTreeNode(contents);     
      DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
      this.insertNodeInto(newNode, parentNode, index);
      parentMap_.put(id, newNode);
    }
    retval.newRoot = deepCopyRoot();
    retval.newMap = makeParentMap(retval.newRoot);
    if (!appState_.isHeadless()) {
      nodeStructureChangedWithoutFlow(this.root);
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Add a dynamic proxy node to the Nav Tree with the given information
  */ 
  
  public NavTreeChange addProxyNode(String name, String parentID, String proxyID) {
    DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
    int index = parentNode.getChildCount();
    return (addProxyNodeAtIndex(name, parentID, proxyID, index));
  }
  
  /***************************************************************************
  **
  ** Add a dynamic proxy node to the Nav Tree with the given information at the given index
  */ 
  
  public NavTreeChange addProxyNodeAtIndex(String name, String parentID, String proxyID, int index) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldMap = makeParentMap(retval.oldRoot);        
    NavNodeContents contents = new NavNodeContents(name, null, proxyID);
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(contents);     
    DefaultMutableTreeNode parentNode = parentMap_.get(parentID);
    this.insertNodeInto(newNode, parentNode, index);
    //parentMap_.put(id, newNode);  Dynamic Proxy nodes cannot be parents.
    retval.newRoot = deepCopyRoot();
    retval.newMap = makeParentMap(retval.newRoot);
    if (!appState_.isHeadless()) {
      nodeStructureChangedWithoutFlow(this.root);
    }
    return (retval);
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
    retval.oldMap = makeParentMap(retval.oldRoot);

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
    retval.newMap = makeParentMap(retval.newRoot);
    if (!appState_.isHeadless()) {
      nodeStructureChangedWithoutFlow(root);
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
    retval.oldContents = (NavNodeContents)node.getUserObject();
    retval.newContents = new NavNodeContents(retval.oldContents.name, null, proxyID);
    node.setUserObject(new NavNodeContents(retval.oldContents.name, null, proxyID));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the given node from a proxy node
  */ 
  
  public NavTreeChange changeFromProxy(DefaultMutableTreeNode node, String id) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldContents = (NavNodeContents)node.getUserObject();
    retval.newContents = new NavNodeContents(retval.oldContents.name, id, null);
    node.setUserObject(new NavNodeContents(retval.oldContents.name, id, null)); 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a node from the Nav Tree, taking all children with it.
  */ 
  
  public NavTreeChange deleteNode(DefaultMutableTreeNode nodeToGo, Set<String> deadIDs) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldMap = makeParentMap(retval.oldRoot);
        
    removeNodeFromParent(nodeToGo);
    Iterator<String> diit = deadIDs.iterator();
    while (diit.hasNext()) {
      String did = diit.next();
      parentMap_.remove(did);
    }

    retval.newRoot = deepCopyRoot();
    retval.newMap = makeParentMap(retval.newRoot);    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Delete all children of node from the Nav Tree, keep the node
  */ 
  
  public NavTreeChange deleteNodeChildren(DefaultMutableTreeNode parentOfNodesToGo, Set<String> deadIDs) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldMap = makeParentMap(retval.oldRoot);
        
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
  
    Iterator<String> diit = deadIDs.iterator();
    while (diit.hasNext()) {
      String did = diit.next();
      parentMap_.remove(did);
    }
    retval.newRoot = deepCopyRoot();
    retval.newMap = makeParentMap(retval.newRoot);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a node from the Nav Tree, taking all children with it.
  */ 
  
  public NavTreeChange deleteNode(String nodeID, Set<String> deadIDs) {
    NavTreeChange retval = new NavTreeChange();
    retval.oldRoot = deepCopyRoot();
    retval.oldMap = makeParentMap(retval.oldRoot);
 
    DefaultMutableTreeNode nodeToGo = parentMap_.get(nodeID);
    removeNodeFromParent(nodeToGo);
    Iterator<String> diit = deadIDs.iterator();
    while (diit.hasNext()) {
      String did = diit.next();
      parentMap_.remove(did);
    }

    retval.newRoot = deepCopyRoot();
    retval.newMap = makeParentMap(retval.newRoot);    
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Refresh the node name for the given id
  */ 
  
  public void nodeNameRefresh(String id) {
    DefaultMutableTreeNode dmtnRoot = (DefaultMutableTreeNode)this.getRoot();    
    Enumeration e = dmtnRoot.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode next = (DefaultMutableTreeNode)e.nextElement();        
      NavNodeContents contents = (NavNodeContents)next.getUserObject();
      if ((contents != null) && 
          (contents.id != null) &&
          contents.id.equals(id)) {
        String name = appState_.getDB().getGenome(id).getName();
        contents.name = name;
        break;
      } else if ((contents != null) && 
                 (contents.proxyID != null) &&
                  contents.proxyID.equals(id)) {
        String name = appState_.getDB().getDynamicProxy(id).getName();
        contents.name = name;
        break;
      }
    }
    return;
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
    return ((contents == null) ? null : contents.id);  // id is null for time slider proxies
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
  ** Get the selected genomeID
  */ 
  
  public String getGenomeID(TreeNode node) {
    if (node == null) {
      return (null);
    }
    DefaultMutableTreeNode dmtNode = (DefaultMutableTreeNode)node;
    NavNodeContents contents = (NavNodeContents)dmtNode.getUserObject();
    return ((contents == null) ? null : contents.id);  // id is null for time slider proxies
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
      if ((genomeID != null) && modelKey.equals(genomeID)) {
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
  ** Resolve Node key to tree node
  */ 
  
  public TreeNode resolveNode(XPlatModelNode.NodeKey nodeKey, DataAccessContext dacx) {
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
        if (contents.id != null) {
          retval.add(contents.id);
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
      if (contents.id != null) {
        retval.add(contents.id);
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
  ** This is the user object in the tree
  */
  
  public class NavNodeContents {
    String name;  
    String id;
    String proxyID;
    
    public NavNodeContents(String name, String id, String proxyID) {
      this.name = name;
      this.id = id;
      this.proxyID = proxyID;
    }
    
    public NavNodeContents(NavNodeContents other) {
      this.name = other.name;
      this.id = other.id;
      this.proxyID = other.proxyID;
    }    
    
    public String toString() {
      // FOR DEBUG return (super.toString() + " " + name + " " + id + " " + proxyID);
      return (name);
    }
    
    public boolean equals(Object other) {
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
      if (this.name == null) {
        if (otherNNC.name != null) {
          return (false);
        }
      } else if (!this.name.equals(otherNNC.name)) {
        return (false);
      }
      if (this.id == null) {
        if (otherNNC.id != null) {
          return (false);
        }
      } else if (!this.id.equals(otherNNC.id)) {
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
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Make a map from the tree
  */ 
  
  private HashMap<String, DefaultMutableTreeNode> makeParentMap(DefaultMutableTreeNode dmtnRoot) {
    HashMap<String, DefaultMutableTreeNode> retval = new HashMap<String, DefaultMutableTreeNode>();
    if (dmtnRoot.getChildCount() > 0) {
      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)dmtnRoot.getFirstChild();
      makeParentMapGuts(parentNode, retval, true);
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Deep copy the tree
  */ 
  
  private void makeParentMapGuts(DefaultMutableTreeNode node, HashMap<String, DefaultMutableTreeNode> map, boolean isTop) {
   int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      makeParentMapGuts(child, map, false);
    }
    if (!isTop) {
      NavNodeContents nc = new NavNodeContents((NavNodeContents)node.getUserObject());
      if (nc.id != null) {
        map.put(nc.id, node);
      }
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
  ** Recursive guts of path mapping
  */
  
  private void mapPathGuts(List<DefaultMutableTreeNode> oldTail, DefaultMutableTreeNode node, 
                           List<DefaultMutableTreeNode> newHead) {
    
    if (oldTail.size() == 0) {
      return;
    }
    DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode)oldTail.get(0);
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
    XPlatModelNode root = deepXPlatGuts(dmtnRoot, true, dacx);
    Rectangle allBounds = appState_.getZoomCommandSupport().allModelsBounds();
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    return (new XPlatModelTree(
		root, appState_.getDisplayOptMgr().getDisplayOptions(), allBounds, (tad == null ? null : new XPlatTimeAxisDefinition(tad,appState_)))
	);
  }

  /***************************************************************************
  **
  ** Get an xplat key for a tree node 
  */
      
  public XPlatModelNode.NodeKey treeNodeToXPlatKey(DefaultMutableTreeNode node, DataAccessContext dacx) {
    NavNodeContents nc = (NavNodeContents)node.getUserObject();
    GenomeSource gSrc = dacx.getGenomeSource();
    
    if ((nc == null) || ((nc.id == null) && (nc.proxyID == null))) {
      return (null);
    } else {  
      if (nc.proxyID != null) {       
        return (new XPlatModelNode.NodeKey(nc.proxyID, XPlatModelNode.ModelType.DYNAMIC_PROXY));
      } else if (DynamicInstanceProxy.isDynamicInstance(nc.id)) {
        return (new XPlatModelNode.NodeKey(nc.id, XPlatModelNode.ModelType.DYNAMIC_INSTANCE));
      } else if (gSrc.getGenome(nc.id) instanceof DBGenome) {
        return (new XPlatModelNode.NodeKey(nc.id, XPlatModelNode.ModelType.DB_GENOME));
      } else {
        return (new XPlatModelNode.NodeKey(nc.id, XPlatModelNode.ModelType.GENOME_INSTANCE));
      }
    }
  }

  /***************************************************************************
  **
  ** Recursive deep generation of a cross-platform tree description
  */
      
  private XPlatModelNode deepXPlatGuts(DefaultMutableTreeNode node, boolean isRoot, DataAccessContext dacx) {
    NavNodeContents nc = (NavNodeContents)node.getUserObject();
    XPlatModelNode.ModelType mt = null;
    String useID = null;
    MinMax timeRange = null;

    boolean hasImages = false;
    NetOverlayOwner noo = null;
    GenomeSource gSrc = dacx.getGenomeSource();
    String genID = null;
    
    if ((nc == null) || ((nc.id == null) && (nc.proxyID == null))) {
      mt = XPlatModelNode.ModelType.SUPER_ROOT;
    } else {  
      useID = nc.id;
      genID = useID;
      if (nc.proxyID != null) {       
        mt = XPlatModelNode.ModelType.DYNAMIC_PROXY;
        useID = nc.proxyID;
        DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(useID);
        timeRange = new MinMax(dip.getMinimumTime(), dip.getMaximumTime());
        noo = dip;
        hasImages = dip.hasGenomeImage();
        genID = dip.getFirstProxiedKey();
      } else if (DynamicInstanceProxy.isDynamicInstance(nc.id)) {
        mt = XPlatModelNode.ModelType.DYNAMIC_INSTANCE;
        Genome gen = gSrc.getGenome(useID);
        noo = gen;
        hasImages = (gen.getGenomeImage() != null);
      } else if (isRoot) {
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
    
    DataAccessContext dacx4N = (genID == null) ? dacx : new DataAccessContext(dacx, genID, dacx.lSrc.getLayoutForGenomeKey(genID).getID());
    retval.fillOverlayInfoForNode(dacx4N);
        
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
      XPlatModelNode childXPlat = deepXPlatGuts(child, false, dacx);
      retval.addChild(childXPlat);
    }
    return (retval);
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
        return;
      }
    }
    throw new IllegalStateException();
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
        return;
      }
    }
    throw new IllegalStateException();
  }  
}
