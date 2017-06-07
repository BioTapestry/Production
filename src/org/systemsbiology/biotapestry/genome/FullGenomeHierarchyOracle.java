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
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.MinMax;
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
  
  private DataAccessContext dacx_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Constructor
  */

  public FullGenomeHierarchyOracle(DataAccessContext dacx) {
    dacx_ = dacx; 
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
    return (dacx_.getGenomeSource());
  }
  
  /***************************************************************************
  ** 
  ** Helper
  */

  private LayoutSource getLayoutSource() {
    return (dacx_.getLayoutSource());
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
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, getGenomeSource().getGenome(lo.getTarget()), lo);
      rcx.setFGHO(this);
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
    StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, getGenomeSource().getGenome(genomeKey), lo);
    rcx.setFGHO(this);
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
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, getGenomeSource().getGenome(lo.getTarget()), lo);
      rcx.setFGHO(this);
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
 
    String rootModel = gSrc.getRootDBGenome().getID();
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
  ** For finding links only used in the root genome
  */

  public Set<String> findRootOnlyLinks() {    
  
    DBGenome genome = (DBGenome)getGenomeSource().getRootDBGenome();
    HashSet<String> usageSet = new HashSet<String>();
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      usageSet.add(linkID);
    }

    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Iterator<Linkage> liit = gi.getLinkageIterator();
      while (liit.hasNext()) {
        Linkage link = liit.next();
        String linkID = link.getID();
        String baseID = GenomeItemInstance.getBaseID(linkID);
        usageSet.remove(baseID);
      }
    }
    return (usageSet);
  }
  
  /***************************************************************************
  **
  ** For finding nodes only used in the root genome
  */

  public Set<String> findRootOnlyNodes() {    
  
    DBGenome genome = (DBGenome)getGenomeSource().getRootDBGenome();
    HashSet<String> usageSet = new HashSet<String>();
    
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      usageSet.add(nodeID);
    }

    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Iterator<Node> niit = gi.getAllNodeIterator();
      while (niit.hasNext()) {
        Node node = niit.next();
        String nodeID = node.getID();
        String baseID = GenomeItemInstance.getBaseID(nodeID);
        usageSet.remove(baseID);
      }
    }
    return (usageSet);
  } 
  
  /***************************************************************************
  **
  ** Answers if the given master link and associated dups have any overlaps in
  ** genome instances.
  **
  */
  
  public boolean noInstanceOverlap(String master, Set<String> dups) {
    
    HashSet<String> allLinks = new HashSet<String>(dups);
    allLinks.add(master);
    GenomeSource gSrc = getGenomeSource(); 
    Iterator<GenomeInstance> iit = gSrc.getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      HashSet<GenomeInstance.GroupTuple> gtset = new HashSet<GenomeInstance.GroupTuple>();
      if (gi.getVfgParent() == null) {
        Iterator<String> lit = allLinks.iterator();
        while (lit.hasNext()) {
          String lid = lit.next();
          Set<String> liSet = gi.returnLinkInstanceIDsForBacking(lid);
          Iterator<String> liit = liSet.iterator();
          while (liit.hasNext()) {
            String liid = liit.next();
            GenomeInstance.GroupTuple lgt = gi.getRegionTuple(liid);
            if (gtset.contains(lgt)) {
              return (false);
            }
            gtset.add(lgt);
          }
        }
      }
    }  
    return (true);
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
    
    Genome genome = gSrc.getRootDBGenome();
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
  ** Answers if a model image exists
  **
  */
  
  public boolean modelImageExists() {
    GenomeSource gSrc = getGenomeSource();
    
    Genome genome = gSrc.getRootDBGenome();

    if (genome.getGenomeImage() != null) {
      return (true);
    }
    
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getGenomeImage() != null) {
        return (true);
      }
    }
    
    Iterator<DynamicInstanceProxy> dpit = gSrc.getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      if (dip.hasGenomeImage()) {
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
    
    Genome genome = gSrc.getRootDBGenome();
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
    Genome genome = gSrc.getRootDBGenome();
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
  ** Get cross-links
  **
  */
  
  public Map<String, Set<TemporalInputRangeData.CrossRegionTuple>> getAllCrossLinks(TemporalInputRangeData tird) {
    GenomeSource gSrc = getGenomeSource();
         
    HashMap<String, Set<TemporalInputRangeData.CrossRegionTuple>> retval = new HashMap<String, Set<TemporalInputRangeData.CrossRegionTuple>>();
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.isRootInstance()) {
        Map<String, GenomeInstance.GroupTuple> art = gi.getAllRegionTuples();
        for (String artk : art.keySet()) {
          GenomeInstance.GroupTuple gtup = art.get(artk);
          String baseLinkID = GenomeItemInstance.getBaseID(artk);
          String srcKey = gtup.getSourceGroup();
          String trgKey = gtup.getTargetGroup();
          if (trgKey.equals(srcKey)) {
            continue;
          }
          Group srcGrp = gi.getGroup(srcKey);
          String srcTrueName = srcGrp.getInheritedTrueName(gi);
          Group trgGrp = gi.getGroup(trgKey);
          String trgTrueName = trgGrp.getInheritedTrueName(gi);
          List<GroupUsage> srcGrpKeys = tird.getTemporalRangeGroupKeysWithDefault(Group.getBaseID(srcKey), srcTrueName);
          List<GroupUsage> trgGrpKeys = tird.getTemporalRangeGroupKeysWithDefault(Group.getBaseID(trgKey), trgTrueName);
          for (GroupUsage sGroupUse : srcGrpKeys) {
            for (GroupUsage tGroupUse : trgGrpKeys) {
              TemporalInputRangeData.CrossRegionTuple crt = new TemporalInputRangeData.CrossRegionTuple(sGroupUse.mappedGroup, tGroupUse.mappedGroup);
              Set<TemporalInputRangeData.CrossRegionTuple> forLink = retval.get(baseLinkID);
              if (forLink == null) {
                forLink = new HashSet<TemporalInputRangeData.CrossRegionTuple>();
                retval.put(baseLinkID, forLink);
              }
              forLink.add(crt);
            }
          }
        }
      }
    }
    return (retval);
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
    Iterator<Gene> rit = getGenomeSource().getRootDBGenome().getGeneIterator();
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
    Iterator<Node> rit = getGenomeSource().getRootDBGenome().getNodeIterator();
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
  
  /***************************************************************************
  **
  ** Global Analysis of how child models are respecting link module boundaries:
  */
       
  public static boolean hasModuleProblems(Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla) {    
  
    boolean retval = false;
    for (String genomeID : gla.keySet()) {
      Map<String, DBGeneRegion.LinkAnalysis> lsMap = gla.get(genomeID);
      for (String geneID : lsMap.keySet()) {
        DBGeneRegion.LinkAnalysis ls = lsMap.get(geneID);
        for (String linkID : ls.status.keySet()) {
          DBGeneRegion.LinkModStatus stat = ls.status.get(linkID);
          if ((stat == DBGeneRegion.LinkModStatus.ORPHANED) || (stat == DBGeneRegion.LinkModStatus.TRESSPASS)) {
            return (true);
          }
        }
      } 
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** For a gene, find holders with links into them
  */
       
  public static Set<DBGeneRegion.DBRegKey> holdersWithLinks(String baseGeneID, Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla) {    
  
    HashSet<DBGeneRegion.DBRegKey> retval = new HashSet<DBGeneRegion.DBRegKey>();
    for (String genomeID : gla.keySet()) {
      Map<String, DBGeneRegion.LinkAnalysis> lsMap = gla.get(genomeID);
      for (String geneID : lsMap.keySet()) {
        if (GenomeItemInstance.getBaseID(geneID).equals(baseGeneID)) {
          DBGeneRegion.LinkAnalysis ls = lsMap.get(geneID);
          for (String linkID : ls.status.keySet()) {
            DBGeneRegion.LinkModStatus stat = ls.status.get(linkID);
            if (stat == DBGeneRegion.LinkModStatus.NON_MODULE) {
              DBGeneRegion.PadOffset po = ls.offsets.get(linkID);
              retval.add(po.regKey);
            }
          }
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Global Analysis of how child models are respecting link module boundaries:
  */
       
  public Map<String, Map<String, DBGeneRegion.LinkAnalysis>> fullyAnalyzeLinksIntoModules() {    
    return (analyzeLinksIntoModules(null));
  }

  /***************************************************************************
  **
  ** What genes need fixing?
  */
       
  public static Set<String> geneModsNeedFixing(Map<String, Map<String, DBGeneRegion.LinkAnalysis>> analysis) {
    HashSet<String> retval = new HashSet<String>();
    for (String genKey : analysis.keySet()) {
      Map<String, DBGeneRegion.LinkAnalysis> forGen = analysis.get(genKey);
      for (String geneID : forGen.keySet()) {
        retval.add(GenomeItemInstance.getBaseID(geneID));  
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Global Analysis of how child models are respecting link module boundaries:
  */
       
  public Map<String, Map<String, DBGeneRegion.LinkAnalysis>> analyzeLinksIntoModules(String baseGeneID) {    
  
    Map<String, Map<String, DBGeneRegion.LinkAnalysis>> retval = new HashMap<String, Map<String, DBGeneRegion.LinkAnalysis>>();

    GenomeSource gSrc = getGenomeSource(); 
    DBGenome genome = (DBGenome)gSrc.getRootDBGenome();
    Map<String, DBGeneRegion.LinkAnalysis> canonical = canonicalGeneRegions(genome, baseGeneID);
    retval.put(genome.getID(), canonical);
      
    Iterator<GenomeInstance> iit = gSrc.getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      retval.put(gi.getID(), analyzeLinksIntoModulesInInstance(gi, canonical));
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Provide the canonical definition of which links go to which gene modules, from the root definition:
  */
       
  private Map<String, DBGeneRegion.LinkAnalysis> canonicalGeneRegions(DBGenome genome, String baseGeneID) {    
  

    Map<String, DBGeneRegion.LinkAnalysis> result = new HashMap<String, DBGeneRegion.LinkAnalysis>();
    
    //
    // Find the genes that have module definitions, and map from pad number to region key (for all pads in regions);
    //
    
    ArrayList<Gene> miniList = null;
    if (baseGeneID != null) {
      miniList = new ArrayList<Gene>();
      miniList.add(genome.getGene(baseGeneID));  
    }
    Iterator<Gene> git = (miniList == null) ? genome.getGeneIterator() : miniList.iterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      if (gene.getNumRegions() > 0) {
        DBGeneRegion.LinkAnalysis la = new DBGeneRegion.LinkAnalysis(gene.getID());
        result.put(gene.getID(), la);
        Iterator<DBGeneRegion> rit = gene.regionIterator();
        while (rit.hasNext()) {
          DBGeneRegion reg = rit.next();
          DBGeneRegion.DBRegKey key = reg.getKey();
          la.keyToReg.put(key, reg);
          int startPad = reg.getStartPad();
          int endPad = reg.getEndPad();
          for (int i = startPad; i <= endPad; i++) {
            la.padToReg.put(Integer.valueOf(i), key);
          }
        }   
      }
    }
   
    //
    // Find the links landing on genes of interest:
    //
    
    Set<String> genesWithRegs = result.keySet();
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      String targ = link.getTarget();
      if (!genesWithRegs.contains(targ)) {
        continue;
      }
      int tpad = link.getLandingPad();
      Integer tPabObj = Integer.valueOf(tpad);
      DBGeneRegion.LinkAnalysis la = result.get(targ);
      DBGeneRegion.DBRegKey key = la.padToReg.get(tPabObj);
      if (key == null) {
       
      } else {
        DBGeneRegion reg = la.keyToReg.get(key);
        DBGeneRegion.LinkModStatus lms = (reg.isHolder()) ? DBGeneRegion.LinkModStatus.NON_MODULE : DBGeneRegion.LinkModStatus.CONSISTENT;
        la.offsets.put(linkID, new DBGeneRegion.PadOffset(key, tpad - reg.getStartPad()));
        la.status.put(linkID, lms);
      }
    }
    return (result);
  }
  
  /***************************************************************************
  **
  ** Provide the canonical definition of which links go to which gene modules, from the root definition:
  */
       
  public Map<String, DBGeneRegion.LinkAnalysis> analyzeLinksIntoModulesInInstance(GenomeInstance gi,
                                                                                  Map<String, DBGeneRegion.LinkAnalysis> canonical) {
 
    Map<String, DBGeneRegion.LinkAnalysis> result = new HashMap<String, DBGeneRegion.LinkAnalysis>();
    
    //
    // Go to all instances of all the genes in the canonical description, and see how they match that
    // requirement.
    //
    
    for (String geneID : canonical.keySet()) {
      DBGeneRegion.LinkAnalysis lac = canonical.get(geneID);
      for (String lid : lac.status.keySet()) {
        DBGeneRegion.LinkModStatus canonStatus = lac.status.get(lid);
        DBGeneRegion.PadOffset forPadCanon = lac.offsets.get(lid); // null if not in region.
        for (String liid : gi.returnLinkInstanceIDsForBacking(lid)) {
          Linkage iLink = gi.getLinkage(liid);
          int tPad = iLink.getLandingPad();
          String lTarg = iLink.getTarget();
          DBGeneRegion.LinkAnalysis lai = result.get(lTarg);
          if (lai == null) {
            lai = new DBGeneRegion.LinkAnalysis(lTarg, lac.keyToReg, lac.padToReg);
            result.put(lTarg, lai);
          }       
          DBGeneRegion.DBRegKey forPadMine = lai.padToReg.get(Integer.valueOf(tPad));
          DBGeneRegion.LinkModStatus myStatus;
          DBGeneRegion reg = lai.keyToReg.get(forPadMine);
          if (reg.isHolder()) { // I am outside any region         
            if (canonStatus == DBGeneRegion.LinkModStatus.CONSISTENT) {
              myStatus = DBGeneRegion.LinkModStatus.ORPHANED;  
            } else if (canonStatus == DBGeneRegion.LinkModStatus.NON_MODULE) {
              myStatus = DBGeneRegion.LinkModStatus.NON_MODULE;
            } else {
              throw new IllegalStateException();
            }
          } else if (canonStatus == DBGeneRegion.LinkModStatus.NON_MODULE) { // Canonical outside any region
            myStatus = DBGeneRegion.LinkModStatus.TRESSPASS; // We must be in somebody's territory....
          } else if (canonStatus == DBGeneRegion.LinkModStatus.CONSISTENT) { // Canonical in a module
            if (forPadCanon.regKey.equals(forPadMine)) { // forPadCanon must != null
              myStatus = DBGeneRegion.LinkModStatus.CONSISTENT;
            } else {
              myStatus = DBGeneRegion.LinkModStatus.TRESSPASS;
            }   
          } else {
            throw new IllegalStateException();
          }
          lai.status.put(liid, myStatus);
          lai.offsets.put(liid, new DBGeneRegion.PadOffset(forPadMine, tPad - reg.getStartPad()));
        }
      }
    }
    return (result);
  }   

  /***************************************************************************
  ** 
  ** Get all the instances of a DBNode
  */

  public List<NodeUsage> getAllNodeInstances(String baseID) {
    ArrayList<NodeUsage> usageList = new ArrayList<NodeUsage>();
    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      String giID = gi.getID();
      Set<String> niSet = gi.getNodeInstances(baseID);
      Iterator<String> nisit = niSet.iterator();
      while (nisit.hasNext()) {
        String nodeInstID = nisit.next();
        Group grp = gi.getGroupForNode(nodeInstID, GenomeInstance.ALWAYS_MAIN_GROUP);
        String grpID = (grp == null) ? null : grp.getID();
        Map<String, Set<String>> modMem = gi.getModuleMembership(nodeInstID);
        GroupMembership grm = gi.getNodeGroupMembership(gi.getNode(nodeInstID));
        NodeUsage le1 = new NodeUsage(baseID, giID, nodeInstID, grpID, modMem, grm.subGroups);
        usageList.add(le1);
      }
    }
    return (usageList);
  }
  
  /***************************************************************************
  ** 
  ** Get all the instances of a Linkage
  */
  
  public List<LinkUsage> getAllLinkInstances(String linkID) {
    ArrayList<LinkUsage> usageList = new ArrayList<LinkUsage>();
    Iterator<GenomeInstance> iit = getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      String giID = gi.getID();
      Set<String> liSet = gi.returnLinkInstanceIDsForBacking(linkID);
      Iterator<String> lisit = liSet.iterator();
      while (lisit.hasNext()) {
        String linkInstID = lisit.next();
        GenomeInstance.GroupTuple groupTup = gi.getRegionTuple(linkInstID);
        usageList.add(new LinkUsage(linkID, giID, linkInstID, groupTup));                      
      }
    }
    return (usageList);
  }
 
  
  /***************************************************************************
  ** 
  ** We need to fix gene lengths from before 7.0.1. Genes can be shorter than their biggest used pad, and we need
  ** to check all usages. At the same time, use this as a chance to collect up inbound pads for links into genes
  ** with cis-reg modules:
  */

  private void minMaxForGenome(Map<String, MinMax> geneExtents, Genome genome, 
                               Map<String, Integer> geneLens, Map<String, Map<String, Set<Integer>>> padsForModGenes) {
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String target = link.getTarget();

      Node node = genome.getNode(target);
      if (node.getNodeType() != node.GENE) {
        continue;
      }
      int tPad = link.getLandingPad();
      String baseID = GenomeItemInstance.getBaseID(target);
      geneLens.put(baseID, node.getPadCount());
      
      MinMax mm = geneExtents.get(baseID);
      if (mm == null) {
        mm = new MinMax(tPad, tPad);
        geneExtents.put(baseID, mm);
      } else {
        mm.update(tPad);
      }
      
      Gene gene = (Gene)node;
      if (gene.getNumRegions() > 0) {
        
        Map<String, Set<Integer>> forMods = padsForModGenes.get(baseID);
        if (forMods == null) {
          forMods = new HashMap<String, Set<Integer>>();
          padsForModGenes.put(baseID, forMods);
        }
        String baseLink = GenomeItemInstance.getBaseID(link.getID());
        Set<Integer> allPads = forMods.get(baseLink);
        if (allPads == null) {
          allPads = new HashSet<Integer>();
          forMods.put(baseLink, allPads);
        }        
        allPads.add(Integer.valueOf(tPad));
      }  
    }
    return;  
  }

  
  /***************************************************************************
  ** 
  ** We need to fix gene lengths from before 7.0.1. Genes can be shorter than their biggest used pad, and we need
  ** to check all usages
  */

  public void legacyIOGeneLengthFixup(Map<String, Integer> retval, 
                                      Map<String, Map<String, Set<Integer>>> padsForModGenes) {
    //
    // Globally track the min and max pad usage into each (base) geneID.
    //
    
    Map<String, MinMax> geneExtents = new HashMap<String, MinMax>();
    Map<String, Integer> geneLens = new HashMap<String, Integer>();

    GenomeSource gSrc = getGenomeSource(); 
    DBGenome genome = (DBGenome)gSrc.getRootDBGenome();

    minMaxForGenome(geneExtents, genome, geneLens, padsForModGenes);    
    Iterator<GenomeInstance> iit = gSrc.getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      minMaxForGenome(geneExtents, gi, geneLens, padsForModGenes);
    }
    
    for (String geneID : geneLens.keySet()) {
      int length = geneLens.get(geneID).intValue(); 
      int firstPad = DBGene.DEFAULT_PAD_COUNT - length;
      MinMax minMax = geneExtents.get(geneID);
      if (minMax.min < firstPad) {
        int newLength = length + (firstPad - minMax.min);
        retval.put(geneID, Integer.valueOf(newLength));    
      }
    }

    return;  
  }

  /***************************************************************************
  **
  ** Used to return node usage results
  */
  
  public static class NodeUsage implements Cloneable {

    public String baseID;
    public String nodeInstID;
    public String modelID;
    public String groupID;
    public Map<String, Set<String>> modMem;
    public Set<String> subGroups;
    
    public NodeUsage(String baseID, String modelID, String nodeInstID, String groupID, Map<String, Set<String>> modMem, Set<String> subGroups) {
      this.baseID = baseID;
      this.nodeInstID = nodeInstID;
      this.modelID = modelID;
      this.groupID = groupID;
      this.modMem = modMem;
      this.subGroups = subGroups;
    }
    
    @Override
    public String toString() {
      return ("NodeUsage baseID = " + baseID + " modelID = " + modelID + " nodeInstID = " + nodeInstID + " groupID = " + groupID + " modMem = " + modMem + " subGroups = " + subGroups);
    }
   
    @Override
    public NodeUsage clone() {
      try {
        NodeUsage retval = (NodeUsage)super.clone();
        retval.modMem = new HashMap<String, Set<String>>();
        for (String ovrID : this.modMem.keySet()) {
          Set<String> forOvr = this.modMem.get(ovrID);
          retval.modMem.put(ovrID, new HashSet<String>(forOvr));
        }
        retval.subGroups = (this.subGroups == null) ? null : new HashSet<String>(this.subGroups);
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Used to return link usage results
  */
  
  public static class LinkUsage implements Cloneable {

    public String baseID;
    public String linkInstID;
    public String modelID;
    public GenomeInstance.GroupTuple groupTup;
    
    public LinkUsage(String baseID, String modelID, String linkInstID, GenomeInstance.GroupTuple groupTup) {
      this.baseID = baseID;
      this.linkInstID = linkInstID;
      this.modelID = modelID;
      this.groupTup = groupTup;
    }
    
    @Override
    public String toString() {
      return ("NodeUsage baseID = " + baseID + " modelID = " + modelID + " linkInstID = " + linkInstID + " groupTup = " + groupTup);
    }
   
    @Override
    public LinkUsage clone() {
      try {
        LinkUsage retval = (LinkUsage)super.clone();
        retval.groupTup = this.groupTup.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
  }
}
