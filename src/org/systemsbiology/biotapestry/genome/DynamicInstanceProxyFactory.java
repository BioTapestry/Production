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

package org.systemsbiology.biotapestry.genome;

import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;

import javax.swing.tree.TreeNode;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This builds Dynamic Instance Proxies in BioTapestry
*/

public class DynamicInstanceProxyFactory extends AbstractFactoryClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private static final int NONE_             = 0;  
  private static final int NOTE_             = 1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set<String> proxyKeys_;
  private Set<String> groupKeys_;
  private Set<String> noteKeys_;

  private String addedNodeKey_;
  private String imageKey_;  
  private DynamicInstanceProxy currDip_;
  private Note currNote_;
  private int charTarget_;
  private DataAccessContext dacx_;
  private NetworkOverlay.NetOverlayWorker now_;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for a Dynamic Instance Proxy Factory 
  */

  public DynamicInstanceProxyFactory() {
    super(new FactoryWhiteboard());
    dacx_ = null;
    proxyKeys_ = DynamicInstanceProxy.keywordsOfInterest();
    groupKeys_ = Group.keywordsOfInterest(true);
    noteKeys_ = Note.keywordsOfInterest(true);    
    addedNodeKey_ = DynamicInstanceProxy.addedNodeKeyword();
    imageKey_ = DynamicInstanceProxy.imageKeyword();
    
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    whiteboard.genomeType = NetworkOverlay.DB_GENOME;
    now_ = new NetworkOverlay.NetOverlayWorker(whiteboard);
    installWorker(now_, new MyGlue());
    myKeys_.addAll(proxyKeys_);    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the current context
  */
  
  public void setContext(DataAccessContext dacx) {
    dacx_ = dacx;
    now_.installContext(dacx);
    return;
  }

  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  @Override
  protected void localFinishElement(String elemName) {
    if (currNote_ != null) {
      currNote_ = null;
    }
    charTarget_ = NONE_;    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  @Override
  protected void localProcessCharacters(char[] chars, int start, int length) {
    String nextString = new String(chars, start, length);
    switch (charTarget_) {
      case NONE_:
        break;
      case NOTE_:
        if (currNote_ != null) {
          currNote_.appendToNote(nextString);
        }
        break;
      default:
        throw new IllegalStateException();        
    }    
    return;
  }
 
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {

    if (proxyKeys_.contains(elemName)) {
      DynamicInstanceProxy dip = DynamicInstanceProxy.buildFromXML(dacx_, elemName, attrs);
      if (dip != null) {
        dacx_.getGenomeSource().addDynamicProxy(dip.getID(), dip);
        NavTree tree = dacx_.getGenomeSource().getModelHierarchy();
        if (tree.needLegacyGlue()) {
          GenomeInstance parent = dip.getVfgParent();
          String parentID = parent.getID();
          // Summed dynamic proxies are being represented by the single
          // instance as a regular tree node, not as a proxy node.  YUK.
          // FIX ME???
          TreeNode parNode = tree.nodeForModel(parentID);
          if (dip.isSingle()) {
            List<String> newNodes = dip.getProxiedKeys();
            Iterator<String> nnit = newNodes.iterator();
            while (nnit.hasNext()) {
              String key = nnit.next();
              tree.addNode(NavTree.Kids.DYNAMIC_SUM_INSTANCE, dip.getProxiedInstanceName(key), parNode, new NavTree.ModelID(key), null, null, dacx_); 
            }
          } else {
            tree.addNode(NavTree.Kids.DYNAMIC_SLIDER_INSTANCE, dip.getName(), parNode, null, dip.getID(), null, dacx_);
          }
        }
        currDip_ = dip;
        ((FactoryWhiteboard)sharedWhiteboard_).prox = dip;
        return (dip);
      }
    } else if (groupKeys_.contains(elemName)) {
      Group newGroup = Group.buildFromXML(dacx_.getRMan(), elemName, attrs);
      if (newGroup != null) {        
        currDip_.addGroup(newGroup);
      }
    } else if (noteKeys_.contains(elemName)) {
      Note newNote = Note.buildFromXML(elemName, attrs);
      if (newNote != null) {
        charTarget_ = NOTE_;
        currNote_ = newNote;
        currDip_.addNote(currNote_);
      }
    } else if (addedNodeKey_.equals(elemName)) {
      currDip_.addExtraNode(DynamicInstanceProxy.extractAddedNode(elemName, attrs));
    } else if (imageKey_.equals(elemName)) {
      currDip_.addImage(elemName, attrs);
    }
    return (null);
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      DynamicInstanceProxy prox = board.prox;
      NetworkOverlay netOvr = board.netOvr;
      if (netOvr != null) {
        prox.addNetworkOverlayAndKey(netOvr);
      }
      return (null);
    }
  } 
}
