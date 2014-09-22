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

package org.systemsbiology.biotapestry.genome;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;

/****************************************************************************
**
** Answers questions that require iterating over all models in the hierarchy
** (New class for 2007; start migrating stuff in here over time)
*/

public class FullGenomeHierarchyOracle {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private BTState appState_;
  private GenomeSource mySource_;
  private LayoutSource lSrc_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Constructor
  */

  public FullGenomeHierarchyOracle(BTState appState) {
    appState_ = appState; 
  }
  
  /***************************************************************************
  ** 
  ** Constructor
  */

  public FullGenomeHierarchyOracle(GenomeSource mySource, LayoutSource lSrc) {
    appState_ = null;
    mySource_ = mySource;
    lSrc_ = lSrc;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Helper
  */

  private GenomeSource getGenomeSource() {
    return ((mySource_ == null) ? appState_.getDB() : mySource_);
  }
  
  /***************************************************************************
  ** 
  ** Helper
  */

  private LayoutSource getLayoutSource() {
    return ((lSrc_ == null) ? appState_.getDB() : lSrc_);
  }
  
  /***************************************************************************
  **
  ** Find pad needs for _all_ net module links globally.  Gotta call this
  ** before making changes!
  */

  public Map<String, Layout.PadNeedsForLayout> getGlobalNetModuleLinkPadNeeds() {
    
    HashMap<String, Layout.PadNeedsForLayout> retval = new HashMap<String, Layout.PadNeedsForLayout>();
    
    LayoutSource lSrc = getLayoutSource();
    Map<String, Layout.OverlayKeySet> allKeys = fullModuleKeysPerLayout();
    
    Iterator<String> akit = allKeys.keySet().iterator();
    while (akit.hasNext()) {
      String layoutID = akit.next();
      Layout lo = lSrc.getLayout(layoutID);
      DataAccessContext rcx = new DataAccessContext(appState_, getGenomeSource().getGenome(lo.getTarget()), lo);
      rcx.fgho = this;
      Layout.PadNeedsForLayout padsForLo = lo.findAllNetModuleLinkPadRequirements(rcx, allKeys);
      retval.put(layoutID, padsForLo);
    } 
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Find pad needs for given genome.  Gotta call this before making changes!
  */

  public Layout.PadNeedsForLayout getLocalNetModuleLinkPadNeeds(String genomeKey) {  
    
    Map<String, Layout.OverlayKeySet> allKeys = fullModuleKeysPerLayout();
    LayoutSource lSrc = getLayoutSource();
    Layout lo = lSrc.getLayoutForGenomeKey(genomeKey);
    DataAccessContext rcx = new DataAccessContext(appState_, getGenomeSource().getGenome(genomeKey), lo);
    rcx.fgho = this;
    return (lo.findAllNetModuleLinkPadRequirements(rcx, allKeys));
  }
 
  /***************************************************************************
  **
  ** When doing instruction builds, member-only modules may lose all members,
  ** and need to be converted to survive.  Cache the info to begin
  */

  public Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> stockUpMemberOnlyModules() {  
    
    HashMap<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> retval = 
      new HashMap<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>>();
    
    LayoutSource lSrc = getLayoutSource();
    Map<String, Layout.OverlayKeySet> allKeys = fullModuleKeysPerLayout();
    
    Iterator<String> akit = allKeys.keySet().iterator();
    while (akit.hasNext()) {
      String layoutID = akit.next();
      Layout lo = lSrc.getLayout(layoutID);
      DataAccessContext rcx = new DataAccessContext(appState_, getGenomeSource().getGenome(lo.getTarget()), lo);
      rcx.fgho = this;
      Map<NetModule.FullModuleKey, Map<String, Rectangle>> geomForLo = lo.stashMemberOnlyGeometry(rcx, allKeys);
      retval.put(layoutID, geomForLo);
    } 
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** return a list of models in a top-down order
  */

  public List<String> orderedModels() {
   
    ArrayList<Link> linkList = new ArrayList<Link>();
    ArrayList<String> nodeList = new ArrayList<String>();
    GenomeSource gSrc = getGenomeSource();
 
    String rootModel = gSrc.getGenome().getID();
    nodeList.add(rootModel);
    
    Iterator<GenomeInstance> iit = gSrc.getInstanceIterator();  
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      String myID = gi.getID();
      nodeList.add(gi.getID());
      GenomeInstance pgi = gi.getVfgParent();
      String parID = (pgi == null) ? rootModel : pgi.getID();
      Link link = new Link(parID, myID);
      linkList.add(link);
    }
    
    GraphSearcher search = new GraphSearcher(nodeList, linkList);
    List<GraphSearcher.QueueEntry> depthFirst = search.depthSearch();
   
    ArrayList<String> retval = new ArrayList<String>();
    int num = depthFirst.size();
    for (int i = 0; i < num; i++) {
      retval.add(depthFirst.get(i).name);
    } 
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Return a list of models in a bottom up order from the given genome
  */

  public List<String> orderedModelsBottomUp(String genomeID) {
   
    ArrayList<String> nodeList = new ArrayList<String>();
 
    GenomeSource gSrc = getGenomeSource();
    Genome startModel = gSrc.getGenome(genomeID);
    nodeList.add(genomeID);
    if (startModel instanceof DBGenome) {
      return (nodeList);
    }
    GenomeInstance currGI = (GenomeInstance)startModel;
    currGI = currGI.getVfgParent();
    
    while (currGI != null) {
      nodeList.add(currGI.getID());
      currGI = currGI.getVfgParent();  
    }
    return (nodeList);
  } 
  
  /***************************************************************************
  **
  ** Returns a vector of top-level instance models (as ObjChoiceContent), sorted by name.
  **
  */
  
  public Vector<ObjChoiceContent> topLevelInstanceModels() {  
       
    GenomeSource gSrc = getGenomeSource(); 
    Iterator<GenomeInstance> iit = gSrc.getInstanceIterator();
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (gi.getVfgParent() == null) {
        ObjChoiceContent occ = new ObjChoiceContent(gi.getName(), gi.getID());
        retval.add(occ);
      }
    }  
    Collections.sort(retval);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answers if an hourly dynamic model exists
  **
  */
  
  public boolean hourlyDynamicModelExists() {
    GenomeSource gSrc = getGenomeSource();
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (!dip.isSingle()) {
        return (true);
      }
    }    
    return (false);
  }

  /***************************************************************************
  **
  ** Fill in maps needed to extract overlay properties for layout extraction
  ** for the given group.  Hand it the root instance; this method will dig
  ** out the info for all the child subset models
  */  
  
  public void fillMapsForGroupExtraction(GenomeInstance rootInstance, String grpID, 
                                         Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap) {
    
    if (!rootInstance.isRootInstance()) {
      throw new IllegalArgumentException();
    }
    rootInstance.fillMapsForGroupExtraction(grpID, keyMap);
    String baseID = Group.getBaseID(grpID);
    
    GenomeSource gSrc = getGenomeSource();
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (rootInstance == gi) {
        continue;
      }
      if (rootInstance.isAncestor(gi)) {
        int genCount = gi.getGeneration();        
        String inherit = Group.buildInheritedID(baseID, genCount);
        gi.fillMapsForGroupExtraction(inherit, keyMap);
      }
    }
 
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rootInstance)) {
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(baseID, kid.getGeneration());
        dip.fillMapsForGroupExtraction(inherit, keyMap);
      }
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Fill in maps needed to extract overlay properties for layout extraction
  ** for the given group.  Hand it the root instance; this method will dig
  ** out the info for all the child subset models
  */  
  
  public void getModulesAttachedToGroup(GenomeInstance rootInstance, String grpID, Set<NetModule.FullModuleKey> attached) {
    
    if (!rootInstance.isRootInstance()) {
      throw new IllegalArgumentException();
    }
    rootInstance.getModulesAttachedToGroup(grpID, attached);
    String baseID = Group.getBaseID(grpID);
    
    GenomeSource gSrc = getGenomeSource();
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (rootInstance == gi) {
        continue;
      }
      if (rootInstance.isAncestor(gi)) {
        int genCount = gi.getGeneration();        
        String inherit = Group.buildInheritedID(baseID, genCount);
        gi.getModulesAttachedToGroup(inherit, attached);
      }
    }
 
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.instanceIsAncestor(rootInstance)) {
        DynamicGenomeInstance kid = dip.getAnInstance();
        String inherit = Group.buildInheritedID(baseID, kid.getGeneration());
        dip.getModulesAttachedToGroup(inherit, attached);
      }
    }
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Returns all the Full Module Keys for all modules.
  **
  */
  
  public Layout.OverlayKeySet allFullModuleKeys(boolean addEmptyOverlayKeys) {
    
    Layout.OverlayKeySet retval = new Layout.OverlayKeySet();

    List<NetOverlayOwner> allOwners = getAllOverlayOwners();      
    int numOwn = allOwners.size();
    for (int i = 0; i < numOwn; i++) {
      NetOverlayOwner owner = allOwners.get(i); 
      int ownerMode = owner.overlayModeForOwner();
      String ownerID = owner.getID();
      Iterator<NetworkOverlay> noit = owner.getNetworkOverlayIterator();
      while (noit.hasNext()) {
        NetworkOverlay novr = noit.next();
        String ovrID = novr.getID();
        if (addEmptyOverlayKeys && (novr.getModuleCount() == 0)) {
          NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerMode, ownerID, ovrID, null);
          retval.addKey(fullKey);
        }
        Iterator<NetModule> nmit = novr.getModuleIterator();
        while (nmit.hasNext()) {
          NetModule nmod = nmit.next();
          String modID = nmod.getID();
          NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerMode, ownerID, ovrID, modID);
          retval.addKey(fullKey);
        }
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Returns all the Full Module Keys handled by each layout
  **
  */
  
  public Map<String, Layout.OverlayKeySet> fullModuleKeysPerLayout() {
    return (fullModuleKeysPerLayout(false));
  }    
  
  /***************************************************************************
  **
  ** Returns all the Full Module Keys handled by each layout, with an option
  ** to return single keys with null module IDs to placehold overlays with
  ** no module:
  **
  */
  
  public Map<String, Layout.OverlayKeySet> fullModuleKeysPerLayout(boolean addEmptyOverlayKeys) {
    HashMap<String, Layout.OverlayKeySet> retval = new HashMap<String, Layout.OverlayKeySet>();
    
    GenomeSource gSrc = getGenomeSource(); 
    LayoutSource lSrc = getLayoutSource();
    Layout.OverlayKeySet allFullKeys = allFullModuleKeys(addEmptyOverlayKeys);
    Iterator<NetModule.FullModuleKey> afkit = allFullKeys.iterator();
    while (afkit.hasNext()) {
      NetModule.FullModuleKey fullKey = afkit.next();
      String genKey;
      if (fullKey.ownerType == NetworkOverlay.DYNAMIC_PROXY) {
        DynamicInstanceProxy dip = gSrc.getDynamicProxy(fullKey.ownerKey);
        genKey = dip.getFirstProxiedKey();
      } else {
        genKey = fullKey.ownerKey;
      }
      String loKey = lSrc.mapGenomeKeyToLayoutKey(genKey);              
      Layout.OverlayKeySet keysForLayout = retval.get(loKey);
      if (keysForLayout == null) {
        keysForLayout = new Layout.OverlayKeySet();
        retval.put(loKey, keysForLayout);        
      }
      keysForLayout.addKey(fullKey);
    }
    return (retval);   
  }
    
  /***************************************************************************
  **
  ** Answers if an overlay exists
  **
  */
  
  public boolean overlayExists() {
    GenomeSource gSrc = getGenomeSource();
    
    Genome genome = gSrc.getGenome();
    if (genome.getNetworkOverlayCount() > 0) {
      return (true);
    }
    
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getNetworkOverlayCount() > 0) {
        return (true);
      }
    }
    
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.getNetworkOverlayCount() > 0) {
        return (true);
      }
    }
    
    return (false);
  }

  /***************************************************************************
  **
  ** Gets all the possible overlay owners
  **
  */
  
  public List<NetOverlayOwner> getAllOverlayOwners() {
    GenomeSource gSrc = getGenomeSource();
    ArrayList<NetOverlayOwner> allOwners = new ArrayList<NetOverlayOwner>();
    
    Genome genome = gSrc.getGenome();
    allOwners.add(genome);
    
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      allOwners.add(gi);
    }
    
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      allOwners.add(dip);
    }
    
    return (allOwners);
  }
  
  
  /***************************************************************************
  **
  ** Returns the full sets of all net modules tags and name/value pairs used 
  ** globally.  NvPairs map filled with all key-canonical names mapped to sets of values
  ** for that name.  If provided module is not null, it will be skipped.
  **
  */
  
  public void getGlobalTagsAndNVPairsForModules(Map<String, Set<String>> nvPairs, Set<String> allNames, 
                                                Set<String> allVals, Set<String> tags, NetModule skipMe) {
   
    List<NetOverlayOwner> allOwners = getAllOverlayOwners();
       
    int numOwn = allOwners.size();
    for (int i = 0; i < numOwn; i++) {
      NetOverlayOwner owner = allOwners.get(i);       
      Iterator<NetworkOverlay> noit = owner.getNetworkOverlayIterator();
      while (noit.hasNext()) {
        NetworkOverlay novr = noit.next();
        Iterator<NetModule> nmit = novr.getModuleIterator();
        while (nmit.hasNext()) {
          NetModule nmod = nmit.next();
          if ((skipMe != null) && (skipMe == nmod)) {
            continue;
          }
          NameValuePairList nvpl = nmod.getNVPairs();
          Iterator<NameValuePair> nvit = nvpl.getIterator();
          while (nvit.hasNext()) {
            NameValuePair nvp = nvit.next();
            String name = nvp.getName();
            String normName = DataUtil.normKey(name);
            Set<String> valsForName = nvPairs.get(normName);
            if (valsForName == null) {
              valsForName = new HashSet<String>();
              nvPairs.put(normName, valsForName);
            }
            allNames.add(name);
            String val = nvp.getValue();
            allVals.add(val);
            valsForName.add(val);
          }
          Iterator<String> ti = nmod.getTagIterator();
          while (ti.hasNext()) {
            String tag = ti.next();
            tags.add(tag);
          }
        }
      }
    }    
    return;
  }
    
  /***************************************************************************
  **
  ** Answers with the minimum number of (launch, landing) pads needed by 
  ** all genomes for the given base node.
  **
  */
  
  public PadCalculatorToo.PadResult getNodePadRequirements(String backingID) {
    GenomeSource gSrc = getGenomeSource();
    Genome genome = gSrc.getGenome();
    Node baseNode = genome.getNode(backingID);
    LayoutSource lSrc = getLayoutSource();
    Layout baseLayout = lSrc.getLayoutForGenomeKey(genome.getID());
    PadCalculatorToo.PadResult res = genome.getNodePadRequirements(baseNode, baseLayout);    
        
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      Set<String> instances = gi.getNodeInstances(backingID);
      Layout layout = lSrc.getLayoutForGenomeKey(gi.getID());
      Iterator<String> iit = instances.iterator();
      while (iit.hasNext()) {
        String nodeID = iit.next();
        Node node = gi.getNode(nodeID);
        PadCalculatorToo.PadResult giRes = gi.getNodePadRequirements(node, layout);
        if (giRes.launch > res.launch) {
          res.launch = giRes.launch;
        }
        if (giRes.landing > res.landing) {
          res.landing = giRes.landing;
        }        
      }
    }
    return (res);
  } 
  
  /***************************************************************************
  ** 
  ** Gene Names must be globally unique, including locally defined Names.  Answer if
  ** we have a match.  This version allows us to skip a node for the check.
  */

  public boolean matchesExistingGeneName(String name, Genome genome, String exceptionID) {
    name = name.replaceAll(" ", "");    
    //
    // Crank all the genomes. Look for matches with root gene Names, and look for
    // matches with local overrides.
    //
    String baseExceptionID = (exceptionID == null) ? null : GenomeItemInstance.getBaseID(exceptionID);
    Iterator<Gene> rit = getGenomeSource().getGenome().getGeneIterator();
    while (rit.hasNext()) {
      Gene gene = rit.next();
      if (baseExceptionID != null) {
        String baseID = GenomeItemInstance.getBaseID(gene.getID());
        if (baseID.equals(baseExceptionID)) {
          continue;
        }
      }
      if (DataUtil.keysEqual(gene.getName(), name)) {
        return (true);
      }
    }
    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Iterator<Gene> git = gi.getGeneIterator();
      while (git.hasNext()) {
        GeneInstance genei = (GeneInstance)git.next();
        if ((baseExceptionID != null) && genome.getID().equals(gi.getID())) { 
          String baseID = GenomeItemInstance.getBaseID(genei.getID());
          if (baseID.equals(baseExceptionID)) {
            continue;
          }
        }
        String oname = genei.getOverrideName();
        if ((oname != null) && DataUtil.keysEqual(oname, name)) {
          return (true);
        }
      }
    }
    return (false);
  }  

  /***************************************************************************
  ** 
  ** Gene Names must be globally unique, including locally defined Names.  Answer if
  ** we have a match.
  */

  public boolean matchesExistingGeneName(String name) {
    return (matchesExistingGeneName(name, null, null));
  }

  /***************************************************************************
  ** 
  ** Gene Names must be globally unique, including locally defined names.
  ** Answer if we have a match with any existing name
  */

  public boolean matchesExistingGeneOrNodeName(String name, Genome genome, String exceptionID) {
    if (matchesExistingGeneName(name, genome, exceptionID)) {
      return (true);
    }
    return (matchesExistingNodeName(name, genome, exceptionID));
  }  

  /***************************************************************************
  ** 
  ** 
  ** Answer if we have a match with any existing non-gene node name, including locally defined names.
  */

  public boolean matchesExistingNodeName(String name) {
    return (matchesExistingNodeName(name, null, null));
  }  
  
  /***************************************************************************
  ** 
  ** Answer if we have a match with any existing name, including locally defined names.
  */

  public boolean matchesExistingNodeName(String name, Genome genome, String exceptionID) {
    name = name.replaceAll(" ", "");
    String baseExceptionID = (exceptionID != null) ? GenomeItemInstance.getBaseID(exceptionID) : null;
    //
    // Crank all the genomes. Look for matches with root node Names, and look for
    // matches with local overrides.
    //
    Iterator<Node> rit = getGenomeSource().getGenome().getNodeIterator();
    while (rit.hasNext()) {
      Node node = rit.next();
      if ((exceptionID != null) && node.getID().equals(baseExceptionID)) {
        continue;
      }
      if (DataUtil.keysEqual(node.getName(), name)) {
        return (true);
      }
    }
    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Iterator<Node> nit = gi.getNodeIterator();
      while (nit.hasNext()) {
        NodeInstance nodei = (NodeInstance)nit.next();
        if ((exceptionID != null) && genome.getID().equals(gi.getID()) && nodei.getID().equals(exceptionID)) {
          continue;
        }
        String oname = nodei.getOverrideName();
        if ((oname != null) && DataUtil.keysEqual(oname, name)) {
          return (true);
        }
      }
    }
    return (false);
  }    

  /***************************************************************************
  ** 
  ** Gene Names must be globally unique, including locally defined names.
  ** Answer if we have a match with any existing name
  */

  public boolean matchesExistingGeneOrNodeName(String name) {
    return (matchesExistingGeneOrNodeName(name, null, null));
  } 

}
