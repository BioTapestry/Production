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


package org.systemsbiology.biotapestry.embedded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;

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
  private DynamicDataAccessContext ddacx_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VIZ CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  EmbeddedViewerInventory(DynamicDataAccessContext dacx)  {
   
    ddacx_ = dacx;
    modelNodes_ = new HashMap<String, List<ExternalInventoryNode>>();
    modelLinks_ = new HashMap<String, List<ExternalInventoryLink>>();
    modelInfo_ = new HashMap<String, ExternalModelInfo>();
    orderedModels_ = new ArrayList<String>();
    
    
    
    Map<String, TabPinnedDynamicDataAccessContext> tabCons = ddacx_.getTabContexts();
    for (String tabKey : tabCons.keySet()) {
      TabPinnedDynamicDataAccessContext tdacx = tabCons.get(tabKey);
      GenomeSource gs = tdacx.getGenomeSource();
      NavTree navTree = gs.getModelHierarchy();
      TimeCourseData tcd = tdacx.getExpDataSrc().getTimeCourseData();
      
      List<String> pol = navTree.getFullTreePreorderListing(new StaticDataAccessContext(tdacx).getContextForRoot());
      Iterator<String> it = pol.iterator();
      while (it.hasNext()) {
        String id = it.next();
        Genome gen = gs.getGenome(id);
        orderedModels_.add(id);
        List<String> names = gen.getNamesToRoot();
        boolean canSelect = true;
        if (gen instanceof DynamicGenomeInstance) {
          DynamicGenomeInstance dgi = (DynamicGenomeInstance)gen;
          DynamicInstanceProxy dprox = gs.getDynamicProxy(dgi.getProxyID());
          if (!dprox.isSingle()) {
            names.set(names.size() - 1, dprox.getName());
          }
          canSelect = false;
        }
        String[] modelNameChain = names.toArray(new String[names.size()]);
        modelInfo_.put(id, new ExternalModelInfo(id, modelNameChain, canSelect));
        ArrayList<ExternalInventoryNode> nodes = new ArrayList<ExternalInventoryNode>();
        ArrayList<ExternalInventoryLink> links = new ArrayList<ExternalInventoryLink>();
        modelLinks_.put(id, links);
        modelNodes_.put(id, nodes);
        if (!(gen instanceof DynamicGenomeInstance)) {
          inventoryForModel(tabKey, gen, nodes, links, tdacx.getDataMapSrc().getTimeCourseDataMaps(), tcd, tdacx);
        }
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
  
  private void inventoryForModel(String tabID, Genome gen, List<ExternalInventoryNode> nodes, List<ExternalInventoryLink> links, 
                                 TimeCourseDataMaps tcdm, TimeCourseData tcd, TabPinnedDynamicDataAccessContext dacx)  {
    String genomeKey = gen.getID();
    
    //
    // All nodes:
    //
    
    Iterator<Node> git = gen.getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String nodeID = node.getID();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      List<TimeCourseDataMaps.TCMapping> customs = tcdm.getCustomTCMTimeCourseDataKeys(baseID);
      boolean internalOnly = tcdm.isAllInternalForNode(tcd, baseID, dacx.getGenomeSource());
      ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(dacx, tabID, genomeKey, nodeID, false);
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
           
      ExternalInventoryItem.BuilderArgs ba = new ExternalInventoryItem.BuilderArgs(dacx, tabID, genomeKey, linkID, true);
      ExternalInventoryItem.ArgsForExternalLink afel = new ExternalInventoryItem.ArgsForExternalLink(ba);
      ExternalInventoryLink els = new ExternalInventoryLink(ba, afel, sign);
      links.add(els);    
    }
    return;
  }
}
