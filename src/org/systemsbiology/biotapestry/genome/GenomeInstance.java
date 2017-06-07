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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This represents a genome instance in BioTapestry
*/

public class GenomeInstance extends AbstractGenome implements Genome, Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE/PROTECTED VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final String KEY_PREF_ = "-ZZ-0";
  
  protected int maxTime_;
  protected int minTime_; 
  protected boolean timed_;   
  protected String vfgParentID_; 
  protected TreeMap<String, Group> groups_;   
  private int uniqueGroupSuffix_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Constructor
  */

  public GenomeInstance(DataAccessContext dacx, String name, String id, String vfgParentID) {
    this(dacx, name, id, vfgParentID, false, -1, -1);    
  }
 
  /***************************************************************************
  **
  ** Constructor
  */

  public GenomeInstance(DataAccessContext dacx, String name, String id, String vfgParentID, boolean timed, int minTime, int maxTime) {
    super(dacx, name, id, NetworkOverlay.GENOME_INSTANCE, false);
    vfgParentID_ = vfgParentID;
    groups_ = new TreeMap<String, Group>();
    uniqueGroupSuffix_ = 1;
    timed_ = timed;
    minTime_ = minTime;
    maxTime_ = maxTime;
    labels_.setFixedPrefix(id_ + KEY_PREF_);
  }

  /***************************************************************************
  **
  ** Copy constructor that is used to create a sibling.  Group IDs are modified
  ** and recorded in the provided map, as for other maps too
  */

  public GenomeInstance(GenomeInstance other, String newName, String newParent, 
                        String newID, Map<String, String> groupIDMap, Map<String, String> noteIDMap, 
                        Map<String, String> ovrIDMap, Map<String, String> modIDMap, 
                        Map<String, String> modLinkIDMap, List<ImageChange> imageChanges, ImageManager imgr) {
    super(other, false);
    this.name_ = newName;
    this.id_ = newID;
    // Part of Issue 195 Fix
    this.ovrops_.resetOwner(this.id_);
    this.vfgParentID_ = newParent;   
    if (this.imgKey_ != null) {
      ImageChange ichange = imgr.registerImageUsage(this.imgKey_);
      if (ichange != null) {
        ichange.genomeKey = newID;
        imageChanges.add(ichange);
      }
    }
 
    //
    // Groups require wierdo, two-pass handling:
    //

    GenomeSource gSrc = (this.mySource_ == null) ? dacx_.getGenomeSource() : this.mySource_;
    DBGenome rootGenome = (DBGenome)gSrc.getRootDBGenome();
    twoPGroupCopy(rootGenome, other, groupIDMap);
    
    //
    // Overlays and modules have globally unique IDs, so generate new IDs and track the mapping.
    // We gotta know group mappings, so we do this after groups above.
    //
        
    ovrops_.copyWithMap(other.ovrops_, rootGenome, groupIDMap, ovrIDMap, modIDMap, modLinkIDMap);
    
    //
    // Note that we need to change note IDs to remain globally unique for all
    // children of root instances (else the layout is messed up)
    //

    this.labels_ = other.labels_.mappedPrefixCopy(other.id_ + KEY_PREF_, this.id_ + KEY_PREF_);    
    notes_ = new HashMap<String, Note>();
    Iterator<String> ntit = other.notes_.keySet().iterator();
    while (ntit.hasNext()) {
      String ntID = ntit.next();
      String myNtID = UniqueLabeller.mapKeyPrefix(ntID, other.id_ + KEY_PREF_, this.id_ + KEY_PREF_, null);
      Note oldNote = other.notes_.get(ntID);
      noteIDMap.put(ntID, myNtID);
      notes_.put(myNtID, new Note(oldNote, myNtID));
    }    
 
    this.uniqueGroupSuffix_ = other.uniqueGroupSuffix_;
  } 
  
  /***************************************************************************
  **
  ** Constructor
  */

  private GenomeInstance(GenomeInstance other, boolean arg) {
    super(other, arg);
    this.vfgParentID_ = other.vfgParentID_;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Drop everything BUT nodes, genes, and links (and groups!)
  */
  
  public GenomeInstance getBasicGenomeCopy(Map<String, String> groupIDMap) {    
    GenomeInstance retval = new GenomeInstance(this, false);
    GenomeSource gSrc = (this.mySource_ == null) ? dacx_.getGenomeSource() : this.mySource_;
    DBGenome rootGenome = (DBGenome)gSrc.getRootDBGenome();
    retval.twoPGroupCopy(rootGenome, this, groupIDMap);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop everything BUT nodes, genes, and ONLY SOME links (and groups!)
  */
  
  public GenomeInstance getBasicGenomeCopy(Map<String, String> groupIDMap, Set<String> keepLinks) {    
    GenomeInstance retval = getBasicGenomeCopy(groupIDMap);
    Iterator<Linkage> lit = retval.getLinkageIterator();
    HashSet<String> deadList = new HashSet<String>();
    while (lit.hasNext()) {
      Linkage daLink = lit.next();
      String daLinkID = daLink.getID();
      if (!keepLinks.contains(daLinkID)) {
        deadList.add(daLinkID);
      }
    }
    Iterator <String> dlit = deadList.iterator();
    while (dlit.hasNext()) {
      String linkID = dlit.next();
      retval.removeLinkage(linkID);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Set optional alternate genome source
  **
  */
  
  @Override
  public void setGenomeSource(GenomeSource gSrc) {
    super.setGenomeSource(gSrc);
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance daLink = (LinkageInstance)lit.next();
      daLink.setGenomeSource(gSrc);
    }
    Iterator<Node> nit = getAllNodeIterator();
    while (nit.hasNext()) {
      NodeInstance daNode = (NodeInstance)nit.next();
      daNode.setGenomeSource(gSrc);
    } 
    return;
  }
  
  
  /***************************************************************************
  **
  ** Activate a particular subgroup. 
  */
  
  public GenomeChange[] activateSubGroup(String parentKey, String subKey) {
    
    //
    // Check that the input args are kosher:
    //
    
    GenomeInstance topInstance = getVfgParentRoot();
    if (topInstance == null) {
      throw new IllegalStateException();
    }
    Group topParent = topInstance.getGroup(Group.getBaseID(parentKey));
    Group topSub = topInstance.getGroup(Group.getBaseID(subKey));
    if ((topSub == null) || (topParent == null)) {
      throw new IllegalArgumentException();
    }
    Set<String> subsets = topParent.getSubsets(topInstance);
    if (!subsets.contains(topSub.getID())) {
      throw new IllegalArgumentException();
    }

    //
    // Build a new group with the necessary generational ID:
    //
    
    GenomeInstance parentInstance = getVfgParent();
    int genCount = parentInstance.getGeneration();        
    String startID = Group.buildInheritedID(subKey, genCount);
    Group newGroup = new Group(dacx_.getRMan(), startID, true, null);
    Group parentGroup = groups_.get(parentKey);
    
    //
    // Add it to the list of groups, and change the parent
    // group to have an active subset:
    //
    
    GenomeChange[] retval = new GenomeChange[2];
    retval[0] = addGroupWithExistingLabel(newGroup);
 
    GenomeChange chng = new GenomeChange();
    retval[1] = chng; 
    chng.grOrig = new Group(parentGroup);
    parentGroup.setActiveSubset(newGroup.getID());
    chng.grNew = new Group(parentGroup);
    chng.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a group to the genome
  **
  */
  
  public GenomeChange addGroup(Group group) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    String id = group.getID();
    if (groups_.get(id) != null) {
      System.err.println("Already have a group: " + id);
      throw new IllegalArgumentException();
    }
    if (vfgParentID_ == null) { 
      ((DBGenome)gSrc.getRootDBGenome()).addKey(id);
    }
    GenomeChange retval = new GenomeChange();
    groups_.put(id, group);
    retval.grOrig = null;
    retval.grNew = group;
    retval.genomeKey = getID();
    return (retval);
  }   
   
  /***************************************************************************
  **
  ** Add a group to the genome
  **
  */
  
  public GenomeChange addGroupWithExistingLabel(Group group) {
    String id = group.getID();
    if (groups_.get(id) != null) {
      throw new IllegalArgumentException();
    }
    GenomeChange retval = new GenomeChange();
    groups_.put(id, group);
    retval.grOrig = null;
    retval.grNew = group;
    retval.genomeKey = getID();
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Answers if we can write SBML
  **
  */
  
  public boolean canWriteSBML() {
    // NOPE
    return (false);
  }   

  /***************************************************************************
  **
  ** Change the gene evidence level
  **
  */
  
  @Override
  public GenomeChange changeGeneEvidence(String geneID, int evidence) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    return (((DBGenome)gSrc.getRootDBGenome()).changeGeneEvidence(GenomeItemInstance.getBaseID(geneID), evidence));  
  } 
  
  /***************************************************************************
  **
  ** Change the gene Name
  **
  */

  @Override
  public GenomeChange changeGeneName(String geneID, String name) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // We are actually messing with the backing item, so the change
    // is in the root model:
    // 
    return (((DBGenome)gSrc.getRootDBGenome()).changeGeneName(GenomeItemInstance.getBaseID(geneID), name));
  }
  
  /***************************************************************************
  **
  ** Change the gene regions
  **
  */

  @Override
  public GenomeChange changeGeneRegions(String geneID, List<DBGeneRegion> newRegions) { 
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // We are actually messing with the backing item, so the change
    // is in the root model:
    // 
    return (((DBGenome)gSrc.getRootDBGenome()).changeGeneRegions(GenomeItemInstance.getBaseID(geneID), newRegions));
  }

  /***************************************************************************
  **
  ** Change the gene size
  **
  */
  
  @Override
  public GenomeChange changeGeneSize(String geneID, int pads) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    return (((DBGenome)gSrc.getRootDBGenome()).changeGeneSize(GenomeItemInstance.getBaseID(geneID), pads));  
  }

  /***************************************************************************
  **
  ** Change the link description
  **
  */
  
  @Override
  public GenomeChange changeLinkageDescription(String linkID, String desc) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }   
    return (super.changeLinkageDescription(linkID, desc)); 
  }  
  
  /***************************************************************************
  **
  ** Change the linkage URLs
  **
  */
 
  @Override
  public GenomeChange changeLinkageURLs(String linkID, List<String> urls) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }   
    return (super.changeLinkageURLs(linkID, urls)); 
  }
  
  /***************************************************************************
  **
  ** Change the node description
  **
  */
  
  @Override
  public GenomeChange changeNodeDescription(String nodeID, String desc) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }   
    return (super.changeNodeDescription(nodeID, desc)); 
  }
  
  /***************************************************************************
  **
  ** Change the node Name
  **
  */
  
  @Override
  public GenomeChange changeNodeName(String nodeID, String name) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // We are actually messing with the backing item, so the change
    // is in the root model:
    // 
    return (((DBGenome)gSrc.getRootDBGenome()).changeNodeName(GenomeItemInstance.getBaseID(nodeID), name));
  } 
  
  /***************************************************************************
  **
  ** Change the node size
  **
  */
  
  @Override
  public GenomeChange changeNodeSize(String nodeID, int pads) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    return (((DBGenome)gSrc.getRootDBGenome()).changeNodeSize(GenomeItemInstance.getBaseID(nodeID), pads));  
  }
  
  /***************************************************************************
  **
  ** Change the node type.  THIS CAN ONLY BE CALLED FOLLOWING THE CONVERSION
  ** IN THE ROOT MODEL
  **
  */
  
  @Override
  public GenomeChange changeNodeType(String nodeID, int type) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();

    if (((DBGenome)gSrc.getRootDBGenome()).getNode(GenomeItemInstance.getBaseID(nodeID)).getNodeType() != type) {
      throw new IllegalArgumentException();
    }
    
    Node existing = getNode(nodeID);
    if (type == Node.GENE) {
      if (existing.getNodeType() == Node.GENE) { // gene->gene A NO-OP, but implement anyway
        retval.gOrig = new GeneInstance((GeneInstance)existing);
        retval.gNew = new GeneInstance((GeneInstance)existing);
        genes_.put(nodeID, new GeneInstance((GeneInstance)retval.gNew));          
      } else { // node - > gene
        retval.nOrig = new NodeInstance((NodeInstance)existing);
        retval.gNew = new GeneInstance((NodeInstance)existing);
        nodes_.remove(nodeID);
        genes_.put(nodeID, new GeneInstance((GeneInstance)retval.gNew));        
      }
    } else {
      if (existing.getNodeType() == Node.GENE) {  // gene -> node
        retval.gOrig = new GeneInstance((GeneInstance)existing);
        retval.nNew = new NodeInstance((NodeInstance)existing); // Note we lose gene subregion data with this constructor!
        retval.nNew.setNodeType(type);     
        genes_.remove(nodeID);
        nodes_.put(nodeID, new NodeInstance((NodeInstance)retval.nNew));
      } else {  // node -> node (probably different type)
        retval.nOrig = new NodeInstance((NodeInstance)existing);
        retval.nNew = new NodeInstance((NodeInstance)existing);
        retval.nNew.setNodeType(type);
        nodes_.put(nodeID, new NodeInstance((NodeInstance)retval.nNew));        
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Change the node URLs
  **
  */
  
  @Override
  public GenomeChange changeNodeURLs(String nodeID, List<String> urls) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }       
    return (super.changeNodeURLs(nodeID, urls)); 
  }
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GenomeChange undo) {
    if (super.changeRedoSupport(undo)) {
      return;
    }
    if ((undo.grOrig != null) || (undo.grNew != null)) {
      groupChangeRedo(undo);
    } else if ((undo.descOld != null) || (undo.descNew != null) ||
        (undo.nameOld != null) || (undo.nameNew != null) ||
        (undo.longNameOld != null) || (undo.longNameNew != null)) {
      propChangeRedo(undo);
      if (undo.timeChanged) { // may occur at the same time
        timeChangeRedo(undo); 
      }
    } else if (undo.timeChanged) {
      timeChangeRedo(undo);
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GenomeChange undo) {
    if (super.changeUndoSupport(undo)) {
      return;
    }
    if ((undo.grOrig != null) || (undo.grNew != null)) {
      groupChangeUndo(undo);
    } else if ((undo.descOld != null) || (undo.descNew != null) ||
               (undo.nameOld != null) || (undo.nameNew != null) ||
               (undo.longNameOld != null) || (undo.longNameNew != null)) {
      propChangeUndo(undo);
      if (undo.timeChanged) { // may occur at the same time
        timeChangeUndo(undo); 
      }
    } else if (undo.timeChanged) {
      timeChangeUndo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** For intercell nodes, figure out what source is hiding behind the node.
  ** We do not backtrack through P/P bubbles: this is one hop only. FIX ME!!
  */

  public String checkOneHopSource(String nodeID) {
    //
    // To find out the incident source on the node, we need to crank through
    // all the links and find out what links we are the target for.  If we
    // have more than one, we punt, unless it is from the same source.
    //

    String retval = null;
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String target = link.getTarget();
      if (target.equals(nodeID)) {
        String source = link.getSource();
        if ((retval != null) && (!retval.equals(source))) {
          return (null);
        } else {
          retval = source;
        } 
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public GenomeInstance clone() {
    GenomeInstance retval = (GenomeInstance)super.clone();
      
    retval.groups_ = new TreeMap<String, Group>();
    Iterator<String> grit = this.groups_.keySet().iterator();
    while (grit.hasNext()) {
      String grID = grit.next();
      retval.groups_.put(grID, this.groups_.get(grID).clone());
    }      

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the model times: drop it
  **
  */
  
  public GenomeChange dropTimes() {
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();
    retval.timedOld = timed_;
    retval.minTimeOld = minTime_;
    retval.maxTimeOld = maxTime_;
    
    timed_ = false;
    minTime_ = -1;
    maxTime_ = -1;
    
    retval.timedNew = timed_;
    retval.minTimeNew = minTime_;
    retval.maxTimeNew = maxTime_;
    
    retval.timeChanged = ((retval.timedOld != retval.timedNew) || 
                          (retval.timedNew && ((retval.minTimeOld != retval.minTimeNew) ||
                                               (retval.maxTimeOld != retval.maxTimeNew))));
    return (retval);
  }  
     
  /***************************************************************************
  **
  ** Fill in map needed to extract overlay properties for layout extraction
  ** for the given group
  */  
  
  public void fillMapsForGroupExtraction(String grpID, Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap) {
    ovrops_.fillMapsForGroupExtraction(grpID, keyMap);
    return;
  }  

  /***************************************************************************
  **
  ** Return modules that are attached to the given group ID.  Overridden in 
  ** dynamic g.i. to get overlays from the proxy!
  **
  */
  
  public Map<String, Set<String>> findModulesOwnedByGroup(String groupID) {
    return (ovrops_.findModulesOwnedByGroupGuts(groupID, ovrops_.getNetworkOverlayMap()));
  }

  /***************************************************************************
  **
  ** Need fixup on Legacy IO to apply hour bounds to this model if we have bounded parents.
  ** 
  */
  
  public void fixupLegacyIOHourBoundsFromParents(List<String> changeList) {    
    GenomeInstance rootParent = getVfgParentRoot();
    if (rootParent != null) {
      if (rootParent.hasTimeBounds()) {
        timed_ = true;
        minTime_ = rootParent.getMinTime();
        maxTime_ = rootParent.getMaxTime();
        changeList.add(getName());
      }
    }    
    return;
  }  

  /***************************************************************************
  **
  ** Need fixup on Legacy IO to apply hour bounds to this model if we have dynamic
  ** children.
  */
  
  public void fixupLegacyIOHourBoundsFromProxies(int minInit, int maxInit, List<String> changeList) {
    
    ArrayList<DynamicInstanceProxy> kidProxies = new ArrayList<DynamicInstanceProxy>();    
    
    // dynamic children:
    
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Iterator<DynamicInstanceProxy> pit = gSrc.getDynamicProxyIterator();
    while (pit.hasNext()) {
      DynamicInstanceProxy dip = pit.next();
      if (dip.instanceIsAncestor(this)) {
        kidProxies.add(dip);
      }
    }
    
    if (kidProxies.isEmpty()) {
      return;
    }
    
    int minChildTime = minInit;
    int maxChildTime = maxInit;    
    
    int size = kidProxies.size();
    for (int i = 0; i < size; i++) {
      DynamicInstanceProxy dip = kidProxies.get(i);
      int min = dip.getMinimumTime();
      if (min < minChildTime) {
        minChildTime = min;
      }
      int max = dip.getMaximumTime();
      if (max > maxChildTime) {
        maxChildTime = max;
      }      
    }
    
    timed_ = true;
    minTime_ = minChildTime;
    maxTime_ = maxChildTime;
    changeList.add(getName());
    return;
  }

  /***************************************************************************
  **
  ** Fixup legacy node activity inconsistencies
  */
  
  public void fixupLegacyIONodeActivities(List<String> changeList) {
    Iterator<Node> anit = getAllNodeIterator();
    while (anit.hasNext()) {
      NodeInstance ni = (NodeInstance)anit.next();
      if (ni.fixupLegacyIOActivityBounds(this)) {
        changeList.add(ni.getName());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Return all the group IDs that are subgroups
  */
   
  public Set<String> getAllSubsets() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        retval.add(group.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the tuple for the link
  */
  
  public GroupTuple getRegionTuple(String linkID) {
  
    Linkage lnk = getLinkage(linkID);
    String src = lnk.getSource();
    String trg = lnk.getTarget();
    
    Iterator<Group> git = getGroupIterator();
    String srcGrp = null;
    String trgGrp = null;
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        continue;
      }
      if (group.isInGroup(src, this)) {
        srcGrp = group.getID();
      }
      if (group.isInGroup(trg, this)) {
        trgGrp = group.getID();
      }
      if ((srcGrp != null) && (trgGrp != null)) {
        break;
      }
    }
    return (new GroupTuple(srcGrp, trgGrp));
  }
  
  
  /***************************************************************************
  **
  ** Get all region tuples:
  */
  
  public Map<String, GroupTuple> getAllRegionTuples() {
         
    HashSet<String> allIDs = new HashSet<String>();
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      allIDs.add(linkID);
    }
    
    return (getRegionTuples(allIDs));
  }
  
  /***************************************************************************
  **
  ** Get a map to tuples for given links
  */
  
  public Map<String, GroupTuple> getRegionTuples(Set<String> linkIDs) {
  
    HashMap<String, String> id2grp = new HashMap<String, String>();   
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        continue;
      }
      Set<String> forGroup = group.areInGroup(this);
      String grpID = group.getID();
      Iterator<String> fgit = forGroup.iterator();
      while (fgit.hasNext()) {
        String nodeID = fgit.next();
        id2grp.put(nodeID, grpID);
      }
    }   
    
    HashMap<String, GroupTuple> retval = new HashMap<String, GroupTuple>();
  
    Iterator<String> lit = linkIDs.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = getLinkage(linkID);
      String src = lnk.getSource();
      String trg = lnk.getTarget();
      retval.put(linkID, new GroupTuple(id2grp.get(src), id2grp.get(trg)));
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Get the generation count.
  */
  
  public int getGeneration() {
    if (vfgParentID_ == null) {
      return (0);
    }
    return (getVfgParent().getGeneration() + 1);
  }  

  /***************************************************************************
  **
  ** Get a particular group
  **
  */
  
  public Group getGroup(String key) {
    return (groups_.get(key));
  }  
    
  /***************************************************************************
  **
  ** Get the group that contains the given node id.  May be null.
  */
  
  // 5-20-04: Might be null if the node is included in a dynamic instance by way
  // of an dynamic proxy extra node!  Right??  Might also be null if an unhidden
  // node has been included (outside of rectangle, inactive sibling subgroup
  // of activated subgroup).  Note these cases do not apply to top-level instance.

  // 3-25-08: Subgroup inclusions are now being checked for group membership, so orphan
  // nodes should be the result of old models or new bugs.  Extra nodes in dynamic models
  // may still return null.  In legacy mode, it is also true that nodes in a main group that
  // are not part of an active subgroup will still return null.  The new mode settings are
  // designed to handle the case of dealing with cases with active subregions.
  
  public static final int LEGACY_MODE = 0;
  public static final int ALWAYS_MAIN_GROUP = 1;
  public static final int MAIN_GROUP_AS_FALLBACK = 2;
   
  public Group getGroupForNode(String nodeID, int mainFallbackMode) {   
    if (mainFallbackMode == LEGACY_MODE) {
      return (getGroupForNodeLegacy(nodeID));
    } else if (mainFallbackMode == ALWAYS_MAIN_GROUP) {    
      Iterator<Group> git = getGroupIterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.isASubset(this)) {
          continue;
        }
        if (group.isInGroup(nodeID, this)) {
          return (group);
        }
      }
      return (null);
    } else { // MAIN_GROUP_AS_FALLBACK
      Group theGroup = getGroupForNodeLegacy(nodeID);
      if (theGroup == null) {
        theGroup = getGroupForNode(nodeID, ALWAYS_MAIN_GROUP);
      }
      return (theGroup);
    }    
  }  
  
  /***************************************************************************
  **
  ** Get an Iterator over the groups:
  **
  */
  
  public Iterator<Group> getGroupIterator() {
    return (groups_.values().iterator());
  }
 
  /***************************************************************************
  **
  ** Given an ordered list of group IDs from a root instance, get an Iterator 
  ** over the actual groups:
  **
  */
  
  public Iterator<Group> getGroupIteratorFromList(List<String> groupOrder) {
    int numG = groupOrder.size();
    Group[] realGroups = new Group[numG];
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group grp = git.next();
      String base = Group.getBaseID(grp.getID()); 
      for (int i = 0; i < numG; i++) {
        String gKey = groupOrder.get(i);
        if (gKey.equals(base)) {
          realGroups[i] = grp;
          break;
        }
      }
    }
    ArrayList<Group> retvalBase = new ArrayList<Group>();
    for (int i = 0; i < numG; i++) {
      if (realGroups[i] != null) {
        retvalBase.add(realGroups[i]);
      }
    }
    
    return (retvalBase.iterator());
  }
 
  /***************************************************************************
  **
  ** Get a reverse iterator over the groups:
  **
  */
  
  public Iterator<Group> getGroupReverseIterator() {
    //
    // There has got to be a better way than this!
    //
    TreeMap<String, Group> tempCopy = new TreeMap<String, Group>(groups_);
    ArrayList<Group> valList = new ArrayList<Group>();
    while (!tempCopy.isEmpty()) {
      Object key = tempCopy.lastKey();
      valList.add(tempCopy.get(key));
      tempCopy.remove(key);
    }
    return (valList.iterator());
  }
 
  /***************************************************************************
  **
  ** Needed by NetOverlayOwner interface
  **
  */
  
  public Set<String> getGroupsForOverlayRendering() {
    return ((vfgParentID_ == null) ? null : new HashSet<String>(groups_.keySet()));
  }

  /***************************************************************************
  **
  ** Return the name of the inherited group
  */
  
  public String getInheritedGroupName(String groupID) {
    Group group = getGroup(groupID);
    String retval = group.getName();
    if (retval != null) {
      return (retval);
    }
    
    Group rootGroup = getVfgParentRoot().getGroup(Group.getBaseID(groupID));
    return (rootGroup.getName());
  }
  
  /***************************************************************************
  **
  ** Given a backing node ID and a group, return the instance number of the
  ** instance in the group.  Will return -1 if it is not there.  Only use
  ** on root instances?
  */
  
  public int getInstanceForNodeInGroup(String backingNodeID, String groupID) {
    Group group = getGroup(groupID);
    Iterator<GroupMember> mit = group.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      if (GenomeItemInstance.getBaseID(nodeID).equals(backingNodeID)) {
        return (GenomeItemInstance.getInstanceID(nodeID));
      }
    }
    return (-1);
  }  
  
  /***************************************************************************
  **
  ** Get Map for node instance in group. Only use
  ** on root instances?
  */
  
  public Map<String, Map<String, Integer>> getInstanceForNodeInGroupMap() {
    HashMap<String, Map<String, Integer>> retval = new HashMap<String, Map<String, Integer>>();
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        continue;
      }
      String groupID = group.getID();
      HashMap<String, Integer> forGp = new HashMap<String, Integer>();
      retval.put(groupID, forGp);
      Iterator<GroupMember> mit = group.getMemberIterator();
      while (mit.hasNext()) {
        GroupMember mem = mit.next();
        String nodeID = mem.getID();
        forGp.put(GenomeItemInstance.getBaseID(nodeID), new Integer(GenomeItemInstance.getInstanceID(nodeID)));
      }
    }
    return (retval);
  }  
  

  /***************************************************************************
  **
  ** Get group membership
  */

  public GroupMembership getLinkGroupMembership(LinkageInstance link) {
    NodeInstance src = (NodeInstance)getNode(link.getSource());
    GroupMembership retval = getNodeGroupMembership(src);
    NodeInstance trg = (NodeInstance)getNode(link.getTarget());
    GroupMembership trgMemb = getNodeGroupMembership(trg);    
    retval.mainGroups.addAll(trgMemb.mainGroups);
    retval.subGroups.addAll(trgMemb.subGroups);
    //int mainSize = retval.mainGroups.size();
    //
    // Turns out there are cases where nodes do not need to belong to groups.
    // (See getNodeGroupMembership())
    // So this test is messed up.!  Better fix those cases!
    //
    //if ((mainSize != 1) && (mainSize != 2)) {
    //  System.err.println("Link " + getID() + " in " + gi.getName() + ": " + retval);      
    //  throw new IllegalStateException();
    // }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the maximum time.  Only valid if we have time bounds.
  **
  */
  
  public int getMaxTime() {
    if (!timed_) {
      throw new IllegalStateException();
    }    
    return (maxTime_);
  }

  /***************************************************************************
  **
  ** Get the minimum time.  Only valid if we have time bounds.
  **
  */
  
  public int getMinTime() {
    if (!timed_) {
      throw new IllegalStateException();
    }
    return (minTime_);
  }
  
  /***************************************************************************
  **
  ** Get the FullKeys for all the modules attached to the given group
  ** for the given group
  */  
  
  public void getModulesAttachedToGroup(String grpID, Set<NetModule.FullModuleKey> attached) {
    ovrops_.getModulesAttachedToGroup(grpID, attached);
    return;
  }
  
  /***************************************************************************
  **
  ** Get all the names up to the root
  **
  */
  
  @Override
  public List<String> getNamesToRoot() {
    ArrayList<String> retval = new ArrayList<String>(); 
    retval.add(name_);
 
    GenomeInstance parent = getVfgParent();
    while (parent != null) {   
      retval.add(parent.getName());
      parent = parent.getVfgParent();
    }

    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Genome top = gSrc.getRootDBGenome();
    retval.add(top.getName());   
    Collections.reverse(retval);
    return (retval); 
  } 

  /***************************************************************************
  **
  ** Given the backing ID for a link, return a set of 2-tuples of groups that
  ** could possibly be newly connected via that link.  The set may be empty if 
  ** all groups are connected, or if there is no possibility of connection.
  **
  */
  
  public Set<GroupTuple> getNewConnectionTuples(String backingKey) {
    
    HashSet<GroupTuple> retval = new HashSet<GroupTuple>();
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    
    DBLinkage link = (DBLinkage)((DBGenome)gSrc.getRootDBGenome()).getLinkage(backingKey);
    String backingID = link.getID();
    String sourceID = link.getSource();
    String targID = link.getTarget();
  
    int srcMax = getMaxNodeInstanceNumber(sourceID);       
    int trgMax = getMaxNodeInstanceNumber(targID);                  
            
    for (int i = 0; i < srcMax; i++) {
      String testSrcID = GenomeItemInstance.getCombinedID(sourceID, Integer.toString(i));
      if (getNode(testSrcID) == null) {
        continue;
      }
      for (int j = 0; j < trgMax; j++) {
        String testTrgID = GenomeItemInstance.getCombinedID(targID, Integer.toString(j));
        if (getNode(testTrgID) == null) {
          continue;
        }
        if (!haveBridgingInstance(backingID, testSrcID, testTrgID)) {
          Group srcGroup = getGroupForNode(testSrcID, LEGACY_MODE);
          Group trgGroup = getGroupForNode(testTrgID, LEGACY_MODE);
          if ((srcGroup == null) || (trgGroup == null)) {
            throw new IllegalStateException();  // should not happen in root instance
          }
          GroupTuple tuple = new GroupTuple(srcGroup.getID(), trgGroup.getID());
          retval.add(tuple);
        }
      }  
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getNextKey() {
    return (labels_.getNextLabel());
  } 
  
  /***************************************************************************
  **
  ** Get the next instance number to use for a particular linkage
  **
  */
  
  public int getNextLinkInstanceNumber(String linkKey) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }
    return (getNextInstanceNumberCore(linkKey, links_, null));
  }
  
  /***************************************************************************
  **
  ** Get the top instance numbers in use for all linkages
  **
  */
  
  public Map<String, Integer> getTopLinkInstanceNumberMap() {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }
    return (buildFastMapTopInstanceNumberCore(links_));
  }

  /***************************************************************************
  **
  ** Get the next instance number to use for a particular linkage
  **
  */
  
  public int getNextLinkInstanceNumberWithExclusion(String linkKey, Set<Integer> exclude) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }
    return (getNextInstanceNumberCore(linkKey, links_, exclude));
  }  
  
  /***************************************************************************
  **
  ** Get the next instance number to use for a particular node
  **
  */
  
  public int getNextNodeInstanceNumber(String nodeKey) {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }
    int nodeCount = getNextInstanceNumberCore(nodeKey, nodes_, null);
    int geneCount = getNextInstanceNumberCore(nodeKey, genes_, null);
    return ((nodeCount > geneCount) ? nodeCount : geneCount);
  }
  
   /***************************************************************************
  **
  ** Get the top instance numbers in use for all nodes
  **
  */
  
  public Map<String, Integer> getTopNodeInstanceNumberMap() {
    if (vfgParentID_ != null) {
      throw new IllegalStateException();
    }
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    retval.putAll(buildFastMapTopInstanceNumberCore(nodes_));
    retval.putAll(buildFastMapTopInstanceNumberCore(genes_));
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Get group membership
  */

  public GroupMembership getNodeGroupMembership(Node node) {
    GroupMembership retval = new GroupMembership();
    Iterator<Group> grit = getGroupIterator();
    while (grit.hasNext()) {
      Group group = grit.next();
      if (group.isInGroup(node.getID(), this)) {
        if (group.isASubset(this)) {
          retval.subGroups.add(group.getID());
        } else {
          retval.mainGroups.add(group.getID());
        } 
      }
    }
    //
    // In general, a node must belong to a group.  But in dynamic instances or subset
    // instances, nodes can get included (via extra nodes or clicking on nodes not hidden
    // by grayed out group with negative boundary padding) even if they are not in a group.
    // If having a main group is required, caller needs to check this out.
    //
    
    //if (retval.mainGroups.size() != 1) {
    //  System.err.println("Node " + getID() + " name: " + getName() + " in " + gi.getName() + ": " + retval);
    //  throw new IllegalStateException();
    //}
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of instances that match the backing ID
  **
  */
  
  public Set<String> getNodeInstances(String backingID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Node> git = getAllNodeIterator();
    while (git.hasNext()) {
      Node node = git.next();
      if (GenomeItemInstance.getBaseID(node.getID()).equals(backingID)) {
        retval.add(node.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop all nodes, genes, groups, and links.  Keep everything else.
  */
  
  @Override
  public Genome getStrippedGenomeCopy() {
    GenomeInstance retval = new GenomeInstance(this.dacx_, this.name_, this.id_, this.vfgParentID_);
    retval.notes_ = this.notes_;
    retval.longName_ = this.longName_;
    retval.description_ = this.description_;
    retval.labels_ = new UniqueLabeller(this.labels_);
    retval.uniqueGroupSuffix_ = this.uniqueGroupSuffix_;
    retval.timed_ = this.timed_;
    retval.minTime_ = this.minTime_;
    retval.maxTime_ = this.maxTime_;
    //
    // We need to retain all net overlays and modules and links, but we need to
    // ditch members until they have been (maybe) rebuilt.
    //
    retval.ovrops_ = ovrops_.getStrippedOverlayCopy();
    return (retval);
  }

  /***************************************************************************
  **
  ** Return those groups that are a subset of the given group.
  */
  
  public List<String> getSubgroupsForGroup(String groupID) {
    //
    // Unfortunately, we need to go up to the top group definitions
    // to get this information.
    //
    ArrayList<String> retval = new ArrayList<String>();
    if (getVfgParent() == null) {
      Iterator<Group> git = getGroupIterator();
      while (git.hasNext()) {
        Group group = git.next();
        String pID = group.getParentID();
        if ((pID != null) && (pID.equals(groupID))) {
          retval.add(group.getID());
        }
      }
    } else {
      // get child info from root, then reduce this by what is actually present
      List<String> potentials = getVfgParentRoot().getSubgroupsForGroup(Group.getBaseID(groupID));
      Iterator<Group> git = getGroupIterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (potentials.contains(Group.getBaseID(group.getID()))) {
          retval.add(group.getID());
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get the next group name
  */

  public String getUniqueGroupName() {
    String format = dacx_.getRMan().getString("group.defaultNameFormat");
    while (true) {
      Integer suffix = new Integer(uniqueGroupSuffix_++);
      String tryName = MessageFormat.format(format, new Object[] {suffix});
      if (format.equals(tryName)) { // Avoid infinite loops with punk resource file
        throw new IllegalStateException();
      }
      if (!groupNameInUse(tryName)) {
        return (tryName);
      }
    }
  }  

  /***************************************************************************
  **
  ** Get the Vfg parent (may be null)
  **
  */
  
  public GenomeInstance getVfgParent() {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    return ((vfgParentID_ == null) ? null : (GenomeInstance)gSrc.getGenome(vfgParentID_));
  }
 
  /***************************************************************************
  **
  ** Get the Vfg parent root (may be null if we are the root)
  **
  */
  
  public GenomeInstance getVfgParentRoot() {
    if (vfgParentID_ == null) {
      return (null);
    }
    GenomeInstance lastParent = getVfgParent();
    while (true) {
      GenomeInstance parentvfg = lastParent.getVfgParent();
      if (parentvfg == null) {
        return (lastParent);
      }
      lastParent = parentvfg;
    }
  }
  
  /***************************************************************************
  **
  ** Answers if we have a group
  **
  */
  
  public int groupCount() {
    return (groups_.size());
  }
  
  /***************************************************************************
  ** 
  ** Answer if the group name is already in use
  */

  public boolean groupNameInUse(String name) {
    return (groupNameInUse(name, null));
  }

  /***************************************************************************
  ** 
  ** Answer if the group name is already in use
  */

  public boolean groupNameInUse(String name, String exceptionID) {
    if (name == null) {
      return (true);  // Don't allow null names
    }
    Iterator<Group> groups = getGroupIterator();
    while (groups.hasNext()) {
      Group g = groups.next();
      if ((exceptionID != null) && g.getID().equals(exceptionID)) {
        continue;
      }
      String gn = g.getName();
      if (gn == null) { // legacy support
        // Fix for BT-10-27-09:9, a backdoor way to get multiple groups with blank names!
        if (DataUtil.keysEqual("", name)) {
          return (true);
        }
      } else {
        if (DataUtil.keysEqual(gn, name)) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Provide used group names.  Caller must catch null names!
  */

  public Set<String> usedGroupNames() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Group> groups = getGroupIterator();
    while (groups.hasNext()) {
      Group g = groups.next();
      String gn = g.getName();
      if (gn == null) { // legacy support
        retval.add("");
      } else {
        retval.add(gn);
      } 
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Answers if we have a group
  **
  */
  
  public boolean hasAGroup() {
    return (groups_.size() > 0);
  }
  
  /***************************************************************************
  **
  ** Answer if instance is time bounded
  **
  */
  
  public boolean hasTimeBounds() {
    return (timed_);
  }
  
  /***************************************************************************
  **
  ** Answers if the genome has at most one copy of each node from the root.
  **
  */
  
  public boolean hasZeroOrOneInstancePerNode() {
    return (hasZeroOrOneInstancePerNodeAfterAdditions(null, null));
  }  
  
  /***************************************************************************
  **
  ** Answers if the genome
  **
  */
  
  public boolean hasZeroOrOneInstancePerNodeAfterAdditions(List<Node> newGenes, List<Node> newNodes) {
    //
    // Go through each gene & node.  Get the base ID and 
    // add to a set; complain if it is already there.
    //    
    HashSet<String> seen = new HashSet<String>();
    Iterator<Gene> git = getGeneIterator();
    while (git.hasNext()) {
      Node node = git.next();
      String baseID = GenomeItemInstance.getBaseID(node.getID());
      if (seen.contains(baseID)) {
        return (false);
      }
      seen.add(baseID);
    }
    Iterator<Node> nit = getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String baseID = GenomeItemInstance.getBaseID(node.getID());
      if (seen.contains(baseID)) {
        return (false);
      }
      seen.add(baseID);
    }
    if (newGenes != null) {
      Iterator<Node> ngit = newGenes.iterator();
      while (ngit.hasNext()) {
        Node node = ngit.next();
        String baseID = GenomeItemInstance.getBaseID(node.getID());
        if (seen.contains(baseID)) {
          return (false);
        }
        seen.add(baseID);
      }
    }
    if (newNodes != null) {
      nit = newNodes.iterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        String baseID = GenomeItemInstance.getBaseID(node.getID());
        if (seen.contains(baseID)) {
          return (false);
        }
        seen.add(baseID);
      }        
    }
    
    return (true);
  }  

  /***************************************************************************
  **
  ** Answers if there exists an instance of the given backing link that bridges
  ** the provided source and target node instances.
  **
  */
  
  public boolean haveBridgingInstance(String backingID, String testSrcID, String testTrgID) {
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance lnk = (LinkageInstance)lit.next();
      String backing = lnk.getBacking().getID();
      if (backing.equals(backingID)) {
        String srcChk = lnk.getSource();
        String trgChk = lnk.getTarget();
        if (srcChk.equals(testSrcID) && trgChk.equals(testTrgID)) {           
          return (true);
        }
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Answer if we have a dynamic proxy decendant
  */
  
  public boolean haveProxyDecendant() {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Iterator<DynamicInstanceProxy> pit = gSrc.getDynamicProxyIterator();
    while (pit.hasNext()) {
      DynamicInstanceProxy dip = pit.next();
      if (dip.instanceIsAncestor(this)) {
        return (true);
      }
    }
    return (false);
  }
    
  /***************************************************************************
  **
  ** Answers if this instance is an ancestor (e.g grandparent) of the given instance.
  ** We are an ancestor of ourselves (EXCEPT FOR dynamic instances?  FIX ME??)
  **
  */
  
  public boolean isAncestor(GenomeInstance child) {
    GenomeInstance lastGI = child;
    while (true) {
      if (lastGI == null) {
        return (false);
      } else if (lastGI == this) {
        return (true);
      }
      lastGI = lastGI.getVfgParent();
    }
  }
 
  /***************************************************************************
  **
  ** Get all links that are cross region. Fill in optional tupMap (may be null)
  */
  
  public Set<String> getCrossRegionLinks(Map<String, GroupTuple> tupMap) {
    
    HashSet<String> retval = new HashSet<String>();
    Map<String, GroupTuple> regTups = getAllRegionTuples();
    Iterator<String> kit = regTups.keySet().iterator();
    while (kit.hasNext()) {
      String linkID = kit.next();
      GroupTuple gt = regTups.get(linkID);
      String srcGrp = gt.getSourceGroup(); // Should not be null, but...
      if ((srcGrp != null) && !srcGrp.equals(gt.getTargetGroup())) {
        retval.add(linkID);
        if (tupMap != null) {
          tupMap.put(linkID, gt);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if the linkage goes between two different regions. Currently not used (see above)
  */
  
  @SuppressWarnings("unused")
  private boolean isCrossRegionLink(String linkID) {

    Linkage lnk = getLinkage(linkID);
    String src = lnk.getSource();
    String trg = lnk.getTarget();
    
    Iterator<Group> git = getGroupIterator();
    String srcGrp = null;
    String trgGrp = null;
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        continue;
      }
      if (group.isInGroup(src, this)) {
        srcGrp = group.getID();
      }
      if (group.isInGroup(trg, this)) {
        trgGrp = group.getID();
      }
      if ((srcGrp != null) && (trgGrp != null)) {
        break;
      }
    }
    return (!srcGrp.equals(trgGrp));
  }
  
  /***************************************************************************
  **
  ** Answer if it is empty
  */
  
  @Override
  public boolean isEmpty() {
    return (super.isEmpty() && (groups_.size() == 0));
  }  

  /***************************************************************************
  **
  ** Convenience function
  **
  */
  
  public boolean isRootInstance() {
    return (vfgParentID_ == null);
  }  

  /***************************************************************************
  **
  ** Answer if there is no place for a root element to go in the given root instance
  ** with given source and target regions (second region can be null)
  */  
 
  public boolean linkInstanceExists(String baseID, String groupKey1, String groupKey2) {
   
    //
    // Get groups resolved:
    //
    
    if (groupKey2 == null) {
      groupKey2 = groupKey1;
    }

    Set<String> ids = returnLinkInstanceIDsForBacking(baseID);
    if (ids.isEmpty()) {
      return (false);
    }
    
    Iterator<String> sit = ids.iterator();
    while (sit.hasNext()) {
      String instanceID = sit.next();
      GenomeInstance.GroupTuple tup = getRegionTuple(instanceID);
      if (tup.getSourceGroup().equals(groupKey1) && tup.getTargetGroup().equals(groupKey2)) {
        return (true);
      } 
    }
    return (false);
  }  
   
  /***************************************************************************
  **
  ** Recover network overlay module members and module group attachments
  */  
  
  public void recoverMappedModuleMembers(Genome oldGenome, Map<String, String> oldNodeToNew, Map<String, String> oldGroupToNew) {
    ovrops_.recoverMappedModuleMembers(oldGenome, oldNodeToNew, oldGroupToNew);
    return;
  }
  
  /***************************************************************************
  **
  ** Remove a particular group.  Group must be empty first, or inherited.
  **
  */
  
  public GenomeChange[] removeEmptyGroup(String key) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Group group = groups_.get(key);
    if (!group.isInherited() && (group.getMemberCount() != 0)) {
      throw new IllegalStateException();
    }
    Set<String> subs = group.getSubsets(this);
    int size = subs.size() + 1;
    GenomeChange[] retval = new GenomeChange[size];
    Iterator<String> subit = subs.iterator();
    int count = 0;
    while (subit.hasNext()) {
      String subID = subit.next();
      GenomeChange chng = new GenomeChange();
      retval[count] = chng;
      chng.grOrig = groups_.get(subID);
      groups_.remove(subID);
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).removeKey(subID);
      }
      chng.grNew = null;
      chng.genomeKey = getID();
      count++;
    }
    GenomeChange chng = new GenomeChange();
    retval[count] = chng;    
    groups_.remove(key);
    if (vfgParentID_ == null) {
      ((DBGenome)gSrc.getRootDBGenome()).removeKey(key);  
    }
    chng.grOrig = group;
    chng.grNew = null;
    chng.genomeKey = getID();
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Remove a particular group without checks (used during fixups following
  ** an instruction-based build).
  **
  */
 
  public GenomeChange removeGroupNoChecks(String key) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    GenomeChange chng = new GenomeChange();
    Group group = groups_.get(key);
    groups_.remove(key);
    if (vfgParentID_ == null) {
      ((DBGenome)gSrc.getRootDBGenome()).removeKey(key);  
    }
    chng.grOrig = group;
    chng.grNew = null;
    chng.genomeKey = getID();
    return (chng);
  }
  
  /***************************************************************************
  **
  ** Remove a particular group.  Group must be empty first, or inherited.
  **
  */  
  public GenomeChange[] removeStrippedGroup(String key) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Group group = groups_.get(key);
    if (!group.isInherited() && (group.getMemberCount() != 0)) {
      throw new IllegalStateException();
    }
    Set<String> subs = group.getSubsets(this);
    int size = subs.size() + 1;
    GenomeChange[] retval = new GenomeChange[size];
    Iterator<String> subit = subs.iterator();
    int count = 0;
    while (subit.hasNext()) {
      String subID = subit.next();
      GenomeChange chng = new GenomeChange();
      retval[count] = chng;
      chng.grOrig = groups_.get(subID);
      groups_.remove(subID);
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).removeKey(subID);
      }
      chng.grNew = null;
      chng.genomeKey = getID();
      count++;
    }
    GenomeChange chng = new GenomeChange();
    retval[count] = chng;    
    groups_.remove(key);
    if (vfgParentID_ == null) {
      ((DBGenome)gSrc.getRootDBGenome()).removeKey(key);  
    }
    chng.grOrig = group;
    chng.grNew = null;
    chng.genomeKey = getID();
    return (retval);
  }  
  
 
  /***************************************************************************
  **
  ** Remove a particular subgroup. 
  */
  
  public GenomeChange[] removeSubGroup(String key) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Group group = groups_.get(key);
    if (!group.isASubset(this)) {
      throw new IllegalArgumentException();
    }
    //
    // If there is a parent group that has us as an active
    // subset, it needs to drop that reference.
    //
    Iterator<Group> grit = groups_.values().iterator();
    GenomeChange[] retval = null;
    while (grit.hasNext()) {
      Group grpChk = grit.next();
      String active = grpChk.getActiveSubset();
      if ((active != null) && active.equals(key)) {
        retval = new GenomeChange[2];
        GenomeChange chng = new GenomeChange();
        retval[0] = chng;
        chng.grOrig = new Group(grpChk);
        grpChk.setActiveSubset(null);
        chng.grNew = new Group(grpChk);
        chng.genomeKey = getID();

        break;
      }
    }
    if (retval == null) {
      retval = new GenomeChange[1];
    }
    GenomeChange chng = new GenomeChange();
    retval[retval.length - 1] = chng;    
    groups_.remove(key);
    if (vfgParentID_ == null) {
      ((DBGenome)gSrc.getRootDBGenome()).removeKey(key);
    }
    chng.grOrig = group;
    chng.grNew = null;
    chng.genomeKey = getID();
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Replace linkage properties in the genome
  **
  */
  
  public GenomeChange replaceLinkageInstanceActivity(String linkID, int activity, double level) {
    GenomeChange retval = new GenomeChange();
    retval.lOrig = links_.get(linkID);
    retval.lNew = retval.lOrig.clone();
    ((LinkageInstance)retval.lNew).setActivity(activity);
    if (activity == LinkageInstance.VARIABLE) {
      ((LinkageInstance)retval.lNew).setActivityLevel(level);      
    }
    links_.put(linkID, retval.lNew);
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace linkage properties in the genome
  **
  */
  
  public GenomeChange replaceLinkageProperties(String linkID, String name, int sign, int targetLevel) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // We are actually messing with the backing item, so the change
    // is in the root model:
    // 
    return (((DBGenome)gSrc.getRootDBGenome()).replaceLinkageProperties(GenomeItemInstance.getBaseID(linkID), 
                                                                  name, sign, targetLevel));
  }
  

  /***************************************************************************
  **
  ** Returns the set of link IDs that match the backing instance
  **
  */
  
  public Set<String> returnLinkInstanceIDsForBacking(String backingID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance lnk = (LinkageInstance)lit.next();
      String backing = lnk.getBacking().getID();
      if (backing.equals(backingID)) {   
        retval.add(lnk.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Revert pad configuration to root configuration
  **
  */
  
  public GenomeChange[] revertPadsToRoot(String nodeID) {

    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    ArrayList<GenomeChange> retList = new ArrayList<GenomeChange>();
    DBGenome genome = (DBGenome)gSrc.getRootDBGenome();   
    
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      if (src.equals(nodeID) || trg.equals(nodeID)) {
        Integer newLaunch = null;
        Integer newLand = null;
        String baseID = GenomeItemInstance.getBaseID(link.getID());
        Linkage baseLink = genome.getLinkage(baseID);
        if (src.equals(nodeID)) {
          int basePad = baseLink.getLaunchPad();
          if (link.getLaunchPad() != basePad) {
            newLaunch = new Integer(basePad);
          }
        }
        if (trg.equals(nodeID)) {
          int basePad = baseLink.getLandingPad();
          if (link.getLandingPad() != basePad) {
            newLand = new Integer(basePad);
          }
        }  
        if ((newLaunch != null) || (newLand != null)) {
          GenomeChange retval = new GenomeChange();
          retval.genomeKey = getID();  
          retval.lOrig = new LinkageInstance((LinkageInstance)link);
          if (newLaunch != null) {
            link.setLaunchPad(newLaunch.intValue());
          }
          if (newLand != null) {
            link.setLandingPad(newLand.intValue());
          }          
          retval.lNew = new LinkageInstance((LinkageInstance)link);
          retList.add(retval);
        }
      }      
    }        

    return (retList.toArray(new GenomeChange[retList.size()]));
  } 

  /***************************************************************************
  **
  ** Merge complementary links in the genome. This assumes we have confirmed that
  ** all links are mergeable.
  */
  
  public GenomeChange[] mergeComplementaryLinks(String masterID, Set<String> dupIDs, Map<String, String> oldToNew) {
    ArrayList<GenomeChange> retList = new ArrayList<GenomeChange>();
    Iterator<String> diit = dupIDs.iterator();
    while (diit.hasNext()) {
      String dit = diit.next();
      Set<String> liSet = returnLinkInstanceIDsForBacking(dit);
      Iterator<String> liit = liSet.iterator();
      while (liit.hasNext()) {
        String lit = liit.next();
        LinkageInstance li = (LinkageInstance)getLinkage(lit);
        LinkageInstance lin;
        if (isRootInstance()) {
          int instanceCount = getNextLinkInstanceNumber(masterID);
          lin = new LinkageInstance(li, masterID, instanceCount);
        } else {
          String newID = oldToNew.get(li.getID());
          int instanceCount = GenomeItemInstance.getInstanceID(newID);
          lin = new LinkageInstance(li, masterID, instanceCount);
        }
        GenomeChange gc = addLinkage(lin);
        retList.add(gc);
        gc = removeLinkage(li.getID());
        retList.add(gc);
        oldToNew.put(li.getID(), lin.getID());
      }     
    }
    return (retList.toArray(new GenomeChange[retList.size()]));
  }

  /***************************************************************************
  **
  ** Answer if a display string is unique
  **
  */
  
  @Override
  public boolean rootDisplayNameIsUnique(String nodeID) {
    //
    // We need to be unique across the root model namespace,
    // since matches in other top genome instances need to be caught,
    // and matches in our instance among multiple instances
    // need to be ignored. We also need to ignore name overrides!
    //
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    return (((DBGenome)gSrc.getRootDBGenome()).rootDisplayNameIsUnique(nodeID));
  }  
  
  /***************************************************************************
  **
  ** Change the model properties
  **
  */
  
  public GenomeChange setProperties(String name, String longName, String desc, 
                                    boolean isTimeBounded, int minTime, int maxTime) {
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();
    
    retval.descOld = description_;
    retval.longNameOld = longName_;
    retval.nameOld = name_;
    retval.timedOld = timed_;
    retval.minTimeOld = minTime_;
    retval.maxTimeOld = maxTime_;    

    longName_ = longName;
    name_ = name;
    description_ = desc;
    timed_ = isTimeBounded;
    minTime_ = minTime;
    maxTime_ = maxTime;
    
    retval.descNew = description_;
    retval.longNameNew = longName_;
    retval.nameNew = name_;    
    retval.timedNew = timed_;
    retval.minTimeNew = minTime_;
    retval.maxTimeNew = maxTime_;
    
    retval.timeChanged = ((retval.timedOld != retval.timedNew) || 
                          (retval.timedNew && ((retval.minTimeOld != retval.minTimeNew) ||
                                               (retval.maxTimeOld != retval.maxTimeNew))));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the model times
  **
  */
  
  public GenomeChange setTimes(int minTime, int maxTime) {
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();
    retval.timedOld = timed_;
    retval.minTimeOld = minTime_;
    retval.maxTimeOld = maxTime_;
    
    timed_ = true;
    minTime_ = minTime;
    maxTime_ = maxTime;
    
    retval.timedNew = timed_;
    retval.minTimeNew = minTime_;
    retval.maxTimeNew = maxTime_;
    
    retval.timeChanged = ((retval.timedOld != retval.timedNew) || 
                          (retval.timedNew && ((retval.minTimeOld != retval.minTimeNew) ||
                                               (retval.maxTimeOld != retval.maxTimeNew))));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Sync all linkage pads to the root model
  **
  */
  
  public GenomeChange[] syncAllLinkagePads(Set<String> groupsToSync) {
    Map<String, GroupTuple> allTup = null;
    if (groupsToSync != null) {
      allTup = getAllRegionTuples();
    }   
    ArrayList<GenomeChange> changes = new ArrayList<GenomeChange>();
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String id = link.getID();
      boolean doSource = true;
      boolean doTarget = true;
      if (groupsToSync != null) {
        GroupTuple tup = allTup.get(id);
        doSource = groupsToSync.contains(tup.getSourceGroup());
        doTarget = groupsToSync.contains(tup.getTargetGroup()); 
      }
      if (!doSource && !doTarget) {
        continue;
      }
      GenomeChange retval = new GenomeChange();
      retval.lOrig = link.clone();   
      LinkageInstance newLink = (LinkageInstance)link.clone();   
      retval.lNew = newLink;
      newLink.syncLaunchPads(doSource, doTarget);
      links_.put(id, retval.lNew.clone());
      retval.genomeKey = getID();
      changes.add(retval);
    }
    return (changes.toArray(new GenomeChange[changes.size()]));
  }  
  
  /***************************************************************************
  **
  ** Write the genome using SBML
  **
  */
  
  @Override
  public void writeSBML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<model ");
    out.print("name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.println("\" >");
    ind.up().indent();
    out.println("<listOfCompartments>");
    ind.up().indent();    
    out.println("<compartment name=\"FG\" />");
    ind.down().indent();
    out.println("</listOfCompartments>");
    //
    // We now list all species.  Each species corresponds to an occupied pad
    // on a node.  For output pads, we have two species, since the reaction will
    // take place between those two species.
    //
    ind.down().indent();      
    out.println("</model>");
    return;
  } 
  
  /***************************************************************************
   **
   ** Write the genome as SIF:
   **
   */
   
  @Override
   public void writeSIF(PrintWriter out) {
     SifSupport ssup = new SifSupport(this);
     ssup.writeSIF(out);
     return;
   }

  /***************************************************************************
  **
  ** Write the genome instance to XML
  **
  */
  
  @Override
  public void writeXML(PrintWriter out, Indenter ind) {
    nodeCollMap_ = buildNodeCollectionMap();
    ind.indent();    
    out.print("<genomeInstance ");
    out.print("name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" id=\"");
    out.print(id_);
    if (vfgParentID_ != null) {
      out.print("\" vfgParent=\"");
      out.print(vfgParentID_);
    }  
    if (timed_) {
      out.print("\" minTime=\"");
      out.print(minTime_);
      out.print("\" maxTime=\"");
      out.print(maxTime_);      
    }   
    if (imgKey_ != null) {
      out.print("\" image=\"");
      out.print(imgKey_); 
    }       
    out.println("\" >");
    ind.up().indent();
    if (longName_ != null) {
      out.print("<longName>");
      out.print(CharacterEntityMapper.mapEntities(longName_, false));
      out.println("</longName>");
    } else {
      out.println("<longName />");      
    }
    ind.indent();
    if (description_ != null) {
      out.print("<description>");
      out.print(CharacterEntityMapper.mapEntities(description_, false));
      out.println("</description>");
    } else {
      out.println("<description />");      
    }    
    ind.indent();    
    out.println("<nodeInstances>");
    writeNodes(out, ind);
    writeGenes(out, ind);
    ind.down().indent();
    out.println("</nodeInstances>");    
    writeLinks(out, ind, "linkInstances");
    writeGroups(out, ind);
    ovrops_.writeOverlaysToXML(out, ind);    
    writeNotes(out, ind);    
    ind.down().indent();       
    out.println("</genomeInstance>");
    return;
  }

  /***************************************************************************
  **
  ** Answer if we are in a module attached to our group IN THE CURRENT MODEL
  */  
 
  public boolean inModuleAttachedToGroup(String nodeID) {
    
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    Group currGrp = getGroupForNode(nodeID, GenomeInstance.ALWAYS_MAIN_GROUP);
    if (currGrp == null) { // should not happen
      return (false);
    }  
    HashSet<NetModule.FullModuleKey> attached = new HashSet<NetModule.FullModuleKey>();
    getModulesAttachedToGroup(currGrp.getID(), attached);
    Iterator<NetModule.FullModuleKey> ait = attached.iterator();
    while (ait.hasNext()) {
      NetModule.FullModuleKey fmk = ait.next();
      NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fmk.ownerKey);
      NetworkOverlay no = noo.getNetworkOverlay(fmk.ovrKey);
      NetModule nmod = no.getModule(fmk.modKey);
      if (nmod.isAMember(nodeID)) {
        return (true);
      }
    } 
    return (false);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Map node types to element names
   **
   */
   
   protected HashMap<Integer, String> buildNodeCollectionMap() {
     HashMap<Integer, String> retval = new HashMap<Integer, String>();
     retval.put(new Integer(Node.BARE), "bareInstances");
     retval.put(new Integer(Node.BOX), "boxInstances");   
     retval.put(new Integer(Node.BUBBLE), "bubbleInstances");   
     retval.put(new Integer(Node.GENE), "geneInstances");   
     retval.put(new Integer(Node.INTERCELL), "intercelInstances");   
     retval.put(new Integer(Node.SLASH), "slashInstances");
     retval.put(new Integer(Node.DIAMOND), "diamondInstances");
     return (retval);
   }

  /***************************************************************************
  **
  ** Redo a group change
  */
  
  protected void groupChangeRedo(GenomeChange undo) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((undo.grOrig != null) && (undo.grNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      String id = undo.grNew.getID();
      groups_.put(id, undo.grNew);
    } else if (undo.grOrig == null) {
      String id = undo.grNew.getID();
      groups_.put(id, undo.grNew);
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).addKey(id);
      }
    } else {
      groups_.remove(undo.grOrig.getID());
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).removeKey(undo.grOrig.getID());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a group change
  */
  
  protected void groupChangeUndo(GenomeChange undo) {
    GenomeSource gSrc = (mySource_ == null) ? dacx_.getGenomeSource() : mySource_;
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.grOrig != null) && (undo.grNew != null)) {  // DOES THIS EVER HAPPEN?  YES
      String id = undo.grOrig.getID();
      groups_.put(id, undo.grOrig);
    } else if (undo.grOrig == null) {
      groups_.remove(undo.grNew.getID());
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).removeKey(undo.grNew.getID());
      }
    } else {
      String id = undo.grOrig.getID();
      groups_.put(id, undo.grOrig);
      if (vfgParentID_ == null) {
        ((DBGenome)gSrc.getRootDBGenome()).addKey(id);
      }
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Legacy case: Get the group that contains the given node id.  May be null.
  */    

  private Group getGroupForNodeLegacy(String nodeID) {   
    // 
    //------------------------------
    // Comments associated with legacy case:
    //
    // Subgroups:  If both a group and subgroup are present, and a node is in
    // both, we return the parent group, unless the parent group is only there
    // as an inactive parent.  If only the subgroup is present, we return that.
    // We assume that if a node is in two subgroups, and the parent group is
    // inactive, both are not in the genome at the same time!
    //
    // Upshot for dynamic models:  included subregions are illustrative only, and
    // do not drive display (parent group does that).  If subregion is active, it
    // is what drives inclusion.
    // ----------------------------
    
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      //
      // If the group is an inactive parent, skip it
      //
      if (group.getActiveSubset() != null) {
        continue;
      }
      
      //
      // If I am a subset: If I am active, return me if node is present.  If not active, ignore me.
      //
      
      if (group.isASubset(this)) {
        String parentID = group.getParentGroup(this);
        Group parent = getGroup(parentID);
        String subset = parent.getActiveSubset();
        if ((subset != null) && subset.equals(group.getID()) && (group.isInGroup(nodeID, this))) {
          return (group);
        } else {
          continue;
        }
      }
      //
      // Other groups are fair game.
      //
      
      if (group.isInGroup(nodeID, this)) {
        return (group);
      }
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Get the max instance number in use for a particular node
  **
  */
  
  private int getMaxNodeInstanceNumber(String nodeKey) {
    int nodeCount = getNextInstanceNumberCore(nodeKey, nodes_, null);
    int geneCount = getNextInstanceNumberCore(nodeKey, genes_, null);
    return ((nodeCount > geneCount) ? nodeCount : geneCount);
  }

  /***************************************************************************
  **
  ** Get the next instance number to use for a particular node or link
  **
  */
  
  private int getNextInstanceNumberCore(String key, Map<String, ? extends GenomeItem> map, Set<Integer> exclude) {
    //
    // Crank through the items, chop off instance numbers and sort them.
    // THIS IS DEADLY FOR BATCH LOADS (CSV LOADS!) Use the FastMap option below instead!
    //

    TreeSet<Integer> sorted = new TreeSet<Integer>();
    if (exclude != null) {
      sorted.addAll(exclude);
    }
    String testPrefix = key.concat(":");
    Iterator<String> keys = map.keySet().iterator();
    while (keys.hasNext()) {
      String nextKey = keys.next();   
      if (nextKey.startsWith(testPrefix)) {
        String instance = nextKey.substring(testPrefix.length());
        try {
          Integer val = Integer.valueOf(instance);
          sorted.add(val);
        } catch (NumberFormatException ex) {
          throw new IllegalStateException();
        }
      }
    }
    int retval = (sorted.size() == 0) ? 0 : sorted.last().intValue() + 1;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a fast map for next instance numbers
  **
  */
  
  private Map<String, Integer> buildFastMapTopInstanceNumberCore(Map<String, ? extends GenomeItem> map) {
    //
    // Crank through the items, chop off instance numbers and sort them.
    //
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    
    Iterator<String> keys = map.keySet().iterator();
    while (keys.hasNext()) {
      String nextKey = keys.next();
      int instanceNum = GenomeItemInstance.getInstanceID(nextKey);
      String base = GenomeItemInstance.getBaseID(nextKey);
      Integer topDog = retval.get(base);
      if ((topDog == null) || (instanceNum > topDog.intValue())) {
        retval.put(base, new Integer(instanceNum));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Redo a time change
  */
  
  private void timeChangeRedo(GenomeChange undo) {
    if (undo.timeChanged) {
      timed_ = undo.timedNew;
      minTime_ = undo.minTimeNew;
      maxTime_ = undo.maxTimeNew;
    }    
    return;
  }
    
  /***************************************************************************
  **
  ** Undo a time change
  */
  
  private void timeChangeUndo(GenomeChange undo) {
    if (undo.timeChanged) {
      timed_ = undo.timedOld;
      minTime_ = undo.minTimeOld;
      maxTime_ = undo.maxTimeOld;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Write the groups to XML
  **
  */
  
  private void writeGroups(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<groups>");
    Iterator<Group> groups = getGroupIterator();

    ind.up();
    while (groups.hasNext()) {
      Group g = groups.next();
      g.writeXML(out, ind, false);
    }
    ind.down().indent(); 
    out.println("</groups>");
    return;
  }
  
  /***************************************************************************
  **
  ** Groups require wierdo, two-pass handling:
  **
  */
  
  private void twoPGroupCopy(DBGenome rootGenome, GenomeInstance other, Map<String, String> groupIDMap) {
    groups_ = new TreeMap<String, Group>();
    Iterator<String> grit = other.groups_.keySet().iterator();
    while (grit.hasNext()) {
      String grID = grit.next();
      Group otherGroup = other.groups_.get(grID);
      if (otherGroup.isASubset(other)) {
        continue;
      }
      Group groupCopy = otherGroup.getMappedCopy(rootGenome, other, groupIDMap);
      groups_.put(groupCopy.getID(), groupCopy);
    }
    //
    // Subgroups get done on a second pass:
    //
    grit = other.groups_.keySet().iterator();
    while (grit.hasNext()) {
      String grID = grit.next();
      Group otherGroup = other.groups_.get(grID);
      if (!otherGroup.isASubset(other)) {
        continue;
      }
      Group groupCopy = otherGroup.getMappedCopy(rootGenome, other, groupIDMap);
      groups_.put(groupCopy.getID(), groupCopy);
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static GenomeInstance buildFromXML(DataAccessContext dacx, String elemName, 
                                            Attributes attrs) throws IOException {
    if (!elemName.equals("genomeInstance")) {
      return (null);
    }
    String name = null;
    String id = null;
    String parentVfg = null;
    String minTimeStr = null;
    String maxTimeStr = null;    
    String imgKey = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("id")) {
          id = val;
        } else if (key.equals("vfgParent")) {
          parentVfg = val;          
        } else if (key.equals("minTime")) {
          minTimeStr = val;          
        } else if (key.equals("maxTime")) {
          maxTimeStr = val;          
        } else if (key.equals("image")) {
          imgKey = val;          
        }
      }
    }
    
    if ((name == null) || (id == null)) {
      throw new IOException();
    }
    
    boolean isTimed = false;
    int minTime = -1;
    int maxTime = -1;
    if ((minTimeStr != null) || (maxTimeStr != null)) {
      if ((minTimeStr == null) || (maxTimeStr == null)) {
        throw new IOException();
      }
      try {
        minTime = Integer.parseInt(minTimeStr);
        maxTime = Integer.parseInt(maxTimeStr);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      if ((minTime > maxTime) || (minTime < 0) || (maxTime < 0)) {
        throw new IOException();       
      }
      isTimed = true;
    }

    GenomeInstance retval = new GenomeInstance(dacx, name, id, parentVfg, isTimed, minTime, maxTime);
    if (imgKey != null) {
      retval.imgKey_ = imgKey;
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Return the description keyword
  **
  */
  
  public static String descriptionKeyword() {
    return ("description");
  }

   /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("genomeInstance");
    return (retval);
  }
  
 
  /***************************************************************************
  **
  ** Return the long name keyword
  **
  */
  
  public static String longNameKeyword() {
    return ("longName");
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Used to return group tuples
  */
  
  public static class GroupTuple implements Comparable<GroupTuple>, Cloneable {
    private String srcGroup_;
    private String trgGroup_;
    
    public GroupTuple(String srcGroup, String trgGroup) {
      if ((srcGroup == null) || (trgGroup == null)) {
        throw new IllegalArgumentException();
      }
      srcGroup_ = srcGroup;
      trgGroup_ = trgGroup;
    }
    
    @Override
    public GroupTuple clone() {
      try {
        GroupTuple retval = (GroupTuple)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }

    //
    // Tuples with matching source and target always beat out
    // mismatches.  Else first group, last group.
    // NOTE: THIS IS BASED ON INTERNAL ID!  SO ordering is
    // actually dependent on creation order, not name order!
    // FIX ME???
    //
    
    public int compareTo(GroupTuple other) {
      boolean iMatch = this.srcGroup_.equalsIgnoreCase(this.trgGroup_);      
      boolean heMatches = other.srcGroup_.equalsIgnoreCase(other.trgGroup_);      
      if (iMatch && !heMatches) {
        return (-1);
      } else if (!iMatch && heMatches) {
        return (1);
      } else if (iMatch && heMatches) {
        return (this.srcGroup_.compareToIgnoreCase(other.srcGroup_));
      }
      
      // Neither matches.
      
      int srcResult = this.srcGroup_.compareToIgnoreCase(other.srcGroup_);
      if (srcResult != 0) {
        return (srcResult);
      }
      return (this.trgGroup_.compareToIgnoreCase(other.trgGroup_));
    }
   
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof GroupTuple)) {
        return (false);
      }
      GroupTuple otherGT = (GroupTuple)other;
      if (!this.srcGroup_.equals(otherGT.srcGroup_)) {
        return (false);
      }
      return (this.trgGroup_.equals(otherGT.trgGroup_));
    }
    
    public String getSourceGroup() {
      return (srcGroup_);
    }
    
    public String getTargetGroup() {
      return (trgGroup_);
    } 
    
    public int hashCode() {
      return ((srcGroup_.hashCode() * 3) + trgGroup_.hashCode());
    }
    
    public String toString() {
      return ("GroupTuple : " + srcGroup_ + " -> " + trgGroup_);
    }
  }   
}
