/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.embedded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

/****************************************************************************
**
** Parts list for users of a BioTapestry embedded viewer panel
*/

public class EmbeddedViewerInventory {
                                                                                                                       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<String, ExternalModelInfo> modelInfo_;
  private ArrayList<String> orderedModels_;
  private HashMap<String, List<ExternalInventoryNode>> modelNodes_;
  private HashMap<String, List<ExternalInventoryLink>> modelLinks_;
  private BTState appState_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VIZ CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  EmbeddedViewerInventory(BTState appState)  {
   
    appState_ = appState;
    modelNodes_ = new HashMap<String, List<ExternalInventoryNode>>();
    modelLinks_ = new HashMap<String, List<ExternalInventoryLink>>();
    modelInfo_ = new HashMap<String, ExternalModelInfo>();
    orderedModels_ = new ArrayList<String>();
    
    Database db = appState_.getDB();
    NavTree navTree = db.getModelHierarchy();
    TimeCourseData tcd = db.getTimeCourseData();
    
    List<String> pol = navTree.getFullTreePreorderListing(new DataAccessContext(appState_));
    Iterator<String> it = pol.iterator();
    while (it.hasNext()) {
      String id = it.next();
      Genome gen = db.getGenome(id);
      orderedModels_.add(id);
      List<String> names = gen.getNamesToRoot();
      boolean canSelect = true;
      if (gen instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)gen;
        DynamicInstanceProxy dprox = db.getDynamicProxy(dgi.getProxyID());
        if (!dprox.isSingle()) {
          names.set(names.size() - 1, dprox.getName());
        }
        canSelect = false;
      }
      String[] modelNameChain = (String[])names.toArray(new String[names.size()]);
      modelInfo_.put(id, new ExternalModelInfo(id, modelNameChain, canSelect));
      ArrayList<ExternalInventoryNode> nodes = new ArrayList<ExternalInventoryNode>();
      ArrayList<ExternalInventoryLink> links = new ArrayList<ExternalInventoryLink>();
      modelLinks_.put(id, links);
      modelNodes_.put(id, nodes);
      if (!(gen instanceof DynamicGenomeInstance)) {
        inventoryForModel(gen, nodes, links, tcd);
      }
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get orderedModelIDs
  */ 
  
  public Iterator<String> getOrderedModelIDs()  { 
    return (orderedModels_.iterator());  
  }
  
  /***************************************************************************
  **
  ** Get model info
  */ 
  
  public ExternalModelInfo getModelInfo(String modelID)  { 
    return (modelInfo_.get(modelID));  
  }
   
  /***************************************************************************
  **
  ** Get the link inventory for a model
  */ 
  
  public List<ExternalInventoryLink> getLinks(String modelID)  { 
    return (new ArrayList<ExternalInventoryLink>(modelLinks_.get(modelID)));  
  }
    
  /***************************************************************************
  **
  ** Get the node inventory for a model
  */ 
  
  public List<ExternalInventoryNode> getNodes(String modelID)  { 
   return (new ArrayList<ExternalInventoryNode>(modelNodes_.get(modelID)));  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build model inventory
  */ 
  
  private void inventoryForModel(Genome gen, List<ExternalInventoryNode> nodes, List<ExternalInventoryLink> links, TimeCourseData tcd)  {
    String genomeKey = gen.getID();
    

    //
    // All nodes:
    //
    
    Iterator<Node> git = gen.getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String nodeID = node.getID();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      List<TimeCourseData.TCMapping> customs = tcd.getCustomTCMTimeCourseDataKeys(baseID);
      boolean internalOnly = tcd.isAllInternalForNode(baseID);
      ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, genomeKey, nodeID, false);
      ExternalInventoryItem.ArgsForExternalNode afen = new ExternalInventoryItem.ArgsForExternalNode(ba);
      ExternalInventoryNode ens = new ExternalInventoryNode(ba, afen, customs, internalOnly);
      nodes.add(ens);
    }

    //
    // Links too:
    //
      
    Iterator<Linkage> lit = gen.getLinkageIterator();    
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      int sign = link.getSign();
           
      ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(appState_, genomeKey, linkID, true);
      ExternalInventoryItem.ArgsForExternalLink afel = new ExternalInventoryItem.ArgsForExternalLink(ba);
      ExternalInventoryLink els = new ExternalInventoryLink(ba, afel, sign);
      links.add(els);    
    }
    return;
  }
}
