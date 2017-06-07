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
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.io.PrintWriter;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalRange;
import org.systemsbiology.biotapestry.timeCourse.InputTimeRange;
import org.systemsbiology.biotapestry.timeCourse.RegionAndRange;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** This represents a dynamically generated genome
*/

public class DynamicGenomeInstance extends GenomeInstance {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private boolean initialized_;
  private HashSet<Integer> times_;
  private String proxyID_;
  private String simKey_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DynamicGenomeInstance(DataAccessContext dacx, String name, String id, 
                               GenomeInstance vfgParent, String proxyID, String imgKey, String simKey) {
    super(dacx, name, id, (vfgParent == null) ? null : vfgParent.getID());
    initialized_ = false;
    times_ = new HashSet<Integer>();
    longName_ = name_;    
    proxyID_ = proxyID;
    imgKey_ = imgKey;
    simKey_ = simKey;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set a simulation comparison key. Can be null to stop a simulation comparison.
  */
  
  public void setSimKey(String key) {
    simKey_ = key;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the ProxyID
  */
  
  public String getProxyID() {
    return (proxyID_);
  }
  
  /***************************************************************************
  ** 
  ** Get the next note key
  */

  @Override
  public String getNextNoteKey() {   
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    String next = dip.getNextKey();
    return (next);
  }
  
  /***************************************************************************
  **
  ** Undo a note change
  */
  
  @Override
  protected void noteChangeUndo(GenomeChange undo) {
    super.noteChangeUndo(undo);
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);    
    dip.undoNoteChange(undo);
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a link change
  */
  
  @Override
  protected void noteChangeRedo(GenomeChange undo) {
    super.noteChangeRedo(undo);
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.redoNoteChange(undo);
    return;
  }

  /***************************************************************************
  **
  ** Undo a group change
  */
  
  @Override
  protected void groupChangeUndo(GenomeChange undo) {
    super.groupChangeUndo(undo);   
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);    
    dip.undoGroupChange(undo);
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a link change
  */
  
  @Override
  protected void groupChangeRedo(GenomeChange undo) {
    super.groupChangeRedo(undo);  
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.redoGroupChange(undo);
    return;
  }  

  /***************************************************************************
  **
  ** Set the time
  */
  
  public void setTime(Set<Integer> times) {
    times_.clear();
    times_.addAll(times);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the time (single time only)
  */
  
  public Integer getTime() {
    if (times_.size() != 1) {
      throw new IllegalStateException();
    }
    return (times_.iterator().next());
  }    

  /***************************************************************************
  **
  ** Set the genome image
  **
  */
  
  @Override
  public ImageChange[] setGenomeImage(ImageManager mgr, String imgKey) {
    initialize();
    ImageChange[] retval = super.setGenomeImage(mgr, imgKey);   
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.setGenomeImage(imgKey, getID());
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Drop the genome image
  **
  */
  
  @Override
  public ImageChange dropGenomeImage(ImageManager mgr) {
    initialize();
    ImageChange retval = super.dropGenomeImage(mgr);  
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    retval.timeKey = dip.dropGenomeImage(getID());
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Undo an image change
  */
  
  @Override
  public void imageChangeUndo(ImageManager mgr, ImageChange undo) {
    initialize();
    super.imageChangeUndo(mgr, undo);  
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.imageChangeUndo(mgr, undo);
    return;
  }  
    
  
  /***************************************************************************
  **
  ** Redo an image change
  */
  
  @Override
  public void imageChangeRedo(ImageManager mgr, ImageChange redo) {   
    initialize();
    super.imageChangeRedo(mgr, redo);    
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.imageChangeRedo(mgr, redo);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a gene to the genome
  **
  */
  
  @Override
  public GenomeChange addGene(Gene gene) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Don't know if we are empty until we init!
  **
  */
  
  @Override
  public boolean isEmpty() {
    initialize();
    return (super.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Get an Iterator over the genes:
  **
  */
  
  @Override
  public Iterator<Gene> getGeneIterator() {
    initialize();
    return (super.getGeneIterator());
  }
  
  /***************************************************************************
  **
  ** Get a sorted iterator over the genes:
  **
  */
  
  @Override
  public Iterator<Gene> getSortedGeneIterator() {
    initialize();
    return (super.getSortedGeneIterator());
  }

  /***************************************************************************
  **
  ** Get gene count:
  **
  */
  
  @Override
  public int getGeneCount() {
    initialize();
    return (super.getGeneCount());
  }
  
  /***************************************************************************
  **
  ** Get a particular gene
  **
  */
  
  @Override
  public Gene getGene(String key) {
    initialize();
    return (super.getGene(key));
  }
  
  /***************************************************************************
  **
  ** Get the next instance number to use for a particular node
  **
  */
  
  @Override
  public int getNextNodeInstanceNumber(String nodeKey) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Get the next instance number to use for a particular linkage
  **
  */
  
  @Override
  public int getNextLinkInstanceNumber(String linkKey) {
    throw new UnsupportedOperationException();
  }  
    
  /***************************************************************************
  **
  ** Add a miscellaneous node to the genome
  **
  */
  
  @Override
  public GenomeChange addNode(Node node) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the miscellaneous nodes:
  **
  */
  
  @Override
  public Iterator<Node> getNodeIterator() {
    initialize();
    return (super.getNodeIterator());
  }

  /***************************************************************************
  **
  ** Get an Iterator over the all the nodes (genes and other nodes):
  **
  */
  
  @Override
  public Iterator<Node> getAllNodeIterator() {
    initialize();
    return (super.getAllNodeIterator());
  }  

  /***************************************************************************
  **
  ** Get a particular node, including genes:
  **
  */
  
  @Override
  public Node getNode(String key) {
    initialize();
    return (super.getNode(key));
  }

  /***************************************************************************
  **
  ** Remove a particular node, including genes:
  **
  */
  
  @Override
  public GenomeChange removeNode(String key) {
    throw new UnsupportedOperationException();
  }  
    
  /***************************************************************************
  **
  ** Add a link to the genome
  **
  */
  
  @Override
  public GenomeChange addLinkage(Linkage link) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the links:
  **
  */
  
  @Override
  public Iterator<Linkage> getLinkageIterator() {
    initialize();
    return (super.getLinkageIterator());
  }
  
  /***************************************************************************
  **
  ** Get a particular link
  **
  */
  
  @Override
  public Linkage getLinkage(String key) {
    initialize();
    return (super.getLinkage(key));
  }
  
  /***************************************************************************
  **
  ** Remove the link from the genome
  **
  */
  
  @Override
  public GenomeChange removeLinkage(String key) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  **
  ** Answers if there exists an instance of the given backing link that bridges
  ** the provided source and target node instances.
  **
  */
  
  @Override
  public boolean haveBridgingInstance(String backingID, String testSrcID, String testTrgID) {
    initialize();
    return (super.haveBridgingInstance(backingID, testSrcID, testTrgID));
  }
  
  /***************************************************************************
  **
  ** Get the Group that contains the given node id.  May be null, but not if
  ** the node is actually in the genome.  NOT TRUE - FIX ME
  */
  
  // FIX ME: Some nodes (intracell) are currently not in a group!
  // 5-20-04: Might be null if the node is included in a dynamic instance by way
  // of an dynamic proxy extra node!  Right??  
  
  @Override
  public Group getGroupForNode(String nodeID, int fallbackMode) {
    initialize();
    return (super.getGroupForNode(nodeID, fallbackMode));
  }
  
  /***************************************************************************
  **
  ** Given a backing node ID and a group, return the instance number of the
  ** instance in the group.  Will return -1 if it is not there.
  */
  
  @Override
  public int getInstanceForNodeInGroup(String backingNodeID, String groupID) {
    initialize();
    return (super.getInstanceForNodeInGroup(backingNodeID, groupID));
  }
  
  /***************************************************************************
  **
  ** Given the backing ID for a link, return a set of 2-tuples of groups that
  ** could possibly be newly connected via that link.  The set may be empty if 
  ** all groups are connected, or if there is no possibility of connection.
  **
  */
 
  @Override
  public Set<GroupTuple> getNewConnectionTuples(String backingKey) {
    initialize();
    return (super.getNewConnectionTuples(backingKey));
  }
  
  /***************************************************************************
  **
  ** Add a group to the genome
  **
  */
  
  @Override
  public GenomeChange addGroup(Group group) {    
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);    
    dip.addGroup(group);
    return (super.addGroup(group));
  }
    
  /***************************************************************************
  **
  ** Add a group to the genome
  **
  */
  
  @Override
  public GenomeChange addGroupWithExistingLabel(Group group) { 
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);    
    dip.addGroup(group);
    return (super.addGroupWithExistingLabel(group));
  }

  /***************************************************************************
  **
  ** Remove a particular group.  Group must be empty first, or inherited.
  **
  */
  
  @Override
  public GenomeChange[] removeEmptyGroup(String key) {   
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    GenomeChange[] retval = super.removeEmptyGroup(key);
    dip.removeGroup(key);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Remove a particular subgroup.
  **
  */
  
  @Override
  public GenomeChange[] removeSubGroup(String key) {   
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    GenomeChange[] retval = super.removeSubGroup(key);
    dip.removeSubGroup(key);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Activate a particular subgroup.
  **
  */
  
  @Override
  public GenomeChange[] activateSubGroup(String parentKey, String subKey) { 
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    GenomeChange[] retval = super.activateSubGroup(parentKey, subKey);  // Handles ADDING the group
    // This is EVIL!!!!  FIX ME!!!:
    int parentIndex = (retval[1].grOrig == null) ? 0 : 1;
    Group sub = retval[(parentIndex == 0) ? 1 : 0].grNew;
    Group parent = retval[parentIndex].grNew;    
    dip.activateSubGroup(parent, sub);
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get the next key
  */

  @Override
  public String getNextKey() {
    initialize();
    return (super.getNextKey());
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the groups:
  **
  */
  
  @Override
  public Iterator<Group> getGroupIterator() {
    initialize();
    return (super.getGroupIterator());
  }
 
  /***************************************************************************
  **
  ** Needed by NetOverlayOwner interface
  **
  */
  
  @Override
  public Set<String> getGroupsForOverlayRendering() {
    initialize();
    return (super.getGroupsForOverlayRendering());
  }     
  
  /***************************************************************************
  **
  ** Get a particular group
  **
  */
  
  @Override
  public Group getGroup(String key) {
    initialize();
    return (super.getGroup(key));
  }
  
  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  @Override
  public void addNote(Note note) {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  @Override
  public GenomeChange addNoteWithExistingLabel(Note note) {
    initialize();    
    GenomeChange retval = super.addNoteWithExistingLabel(note);  
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.addNoteWithExistingLabel(note);
    return (retval);
  }

  /***************************************************************************
  **
  ** Change the note in the genome
  **
  */
  
  @Override
  public GenomeChange changeNote(Note note, String newLabel, String newText, boolean isInteractive) {
    initialize();
    GenomeChange retval = super.changeNote(note, newLabel, newText, isInteractive);    
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.changeNote(retval.ntOrig, retval.ntNew);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Remove a particular note:
  **
  */
  
  @Override
  public GenomeChange removeNote(String key) {
    initialize();
    GenomeChange retval = super.removeNote(key);  
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.removeNote(key);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the notes:
  **
  */
  
  @Override
  public Iterator<Note> getNoteIterator() {
    initialize();
    return (super.getNoteIterator());
  }
  
  /***************************************************************************
  **
  ** Get a particular note
  **
  */
  
  @Override
  public Note getNote(String key) {
    initialize();
    return (super.getNote(key));
  }
  
  /***************************************************************************
  ** 
  ** Answer if the group name is already in use
  */

  @Override
  public boolean groupNameInUse(String name) {
    initialize();
    return (super.groupNameInUse(name));
  }
  
  /***************************************************************************
  ** 
  ** Get the next group name
  */

  @Override
  public String getUniqueGroupName() {
    initialize();
    return (super.getUniqueGroupName());
  }
  
  /***************************************************************************
  ** 
  ** Get the generation count. 
  */

  @Override
  public int getGeneration() {
    return (super.getGeneration());
  }

  /***************************************************************************
  **
  ** Write the genome instance to XML
  **
  */
  
  @Override
  public void writeXML(PrintWriter out, Indenter ind) {
    // We are dynamic; nothing to write
    return;
  }
  
  /***************************************************************************
  **
  ** Add a network module view to the genome
  **
  */
  
  @Override
  public NetworkOverlayOwnerChange addNetworkOverlay(NetworkOverlay nmView) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.addNetworkOverlay(nmView));
  }

  /***************************************************************************
  **
  ** Add a network module view to the genome.  Use for IO
  **
  */
  
  @Override
  public void addNetworkOverlayAndKey(NetworkOverlay nmView) throws IOException {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.addNetworkOverlayAndKey(nmView);
    return;
  } 

  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome.  Use for IO
  **
  */
  
  @Override
  public void addNetworkModuleAndKey(String overlayKey, NetModule module) throws IOException {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.addNetworkModuleAndKey(overlayKey, module);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.  Use for IO
  **
  */
  
  @Override
  public void addNetworkModuleLinkageAndKey(String overlayKey, NetModuleLinkage linkage) throws IOException {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.addNetworkModuleLinkageAndKey(overlayKey, linkage);    
    return;
  }    
  
  /***************************************************************************
  **
  ** Remove the network module view from the genome
  **
  */
  
  @Override
  public NetworkOverlayOwnerChange removeNetworkOverlay(String key) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.removeNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get a network overlay from the genome
  **
  */
  
  @Override
  public NetworkOverlay getNetworkOverlay(String key) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.getNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get an iterator over network overlays
  **
  */
  
  @Override
  public Iterator<NetworkOverlay> getNetworkOverlayIterator() {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.getNetworkOverlayIterator());
  }  
  
  /***************************************************************************
  **
  ** Get the count of network overlays
  **
  */
 
  @Override
  public int getNetworkOverlayCount() {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.getNetworkOverlayCount());
  }
  
  /***************************************************************************
  **
  ** Get the count of network modules
  **
  */
  
  @Override
  public int getNetworkModuleCount() {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.getNetworkModuleCount());
  }  
  
  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome
  **
  */
  
  @Override
  public NetworkOverlayChange addNetworkModule(String overlayKey, NetModule module) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.addNetworkModule(overlayKey, module));
  } 
  
  /***************************************************************************
  **
  ** Remove a network module from an overlay in this genome
  **
  */
  
  @Override
  public NetworkOverlayChange[] removeNetworkModule(String overlayKey, String moduleKey) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.removeNetworkModule(overlayKey, moduleKey));    
  }
    
  /***************************************************************************
  **
  ** Add a new member to a network module of an overlay in this genome
  **
  */
  
  @Override
  public NetModuleChange addMemberToNetworkModule(String overlayKey, NetModule module, String nodeID) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.addMemberToNetworkModule(overlayKey, module, nodeID));
  } 
  
  /***************************************************************************
  **
  ** Remove a member from a network module of an overlay in this genome
  **
  */
  
  @Override
  public NetModuleChange deleteMemberFromNetworkModule(String overlayKey, NetModule module, String nodeID) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.deleteMemberFromNetworkModule(overlayKey, module, nodeID));
  } 

  /***************************************************************************
  **
  ** Return matching Network Modules (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
  
  @Override
  public Map<String, Set<String>> findMatchingNetworkModules(int searchMode, String key, NameValuePair nvPair) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (dip.findMatchingNetworkModules(searchMode, key, nvPair));
  }
  
  /***************************************************************************
  **
  ** Return modules that are attached to the given group ID.  Overridden here 
  ** to get overlays from the proxy!
  **
  */
  
  @Override
  public Map<String, Set<String>> findModulesOwnedByGroup(String groupID) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    return (ovrops_.findModulesOwnedByGroupGuts(groupID, dip.getNetworkOverlayMap()));
  }  
 
  /***************************************************************************
  **
  ** Get the overlay mode
  **
  */
  
  @Override
  public int overlayModeForOwner() {
    return (NetworkOverlay.DYNAMIC_PROXY);
  }  
 
  /***************************************************************************
  **
  ** Undo a change
  */
  
  @Override
  public void overlayChangeUndo(NetworkOverlayOwnerChange undo) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.overlayChangeUndo(undo);
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  @Override
  public void overlayChangeRedo(NetworkOverlayOwnerChange redo) {
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    dip.overlayChangeRedo(redo);
    return;  
  }      

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Lazy initialization
  */

  private void initialize() {
    if (initialized_) {
      return;
    }
    initialized_ = true;
    
    //
    // Do groups first, so they are available during calls below
    //

    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    Iterator<Group> grit = dip.getGroupIterator();
    while (grit.hasNext()) {
      Group grp = grit.next();
      Group myGroup = grp.copyForProxy();      
      groups_.put(myGroup.getID(), myGroup);
    } 
    //
    // Do notes
    //
    
    Iterator<Note> nit = dip.getNoteIterator();
    while (nit.hasNext()) {
      Note note = nit.next();
      Note myNote = new Note(note);
      notes_.put(myNote.getID(), myNote);
    }    
  
    //
    // Go to the root instance.  For each node, ask if it is expressed at our
    // time, and add if it is.  Do the same for each link.
    //
 
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    if (tcd == null) {
      return;
    }
    GenomeInstance parent = getVfgParent();

    HashSet<String> caught = new HashSet<String>();
    initCore(tcd, parent, caught, simKey_);

    TemporalInputRangeData trd = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    if (trd == null) {
      return;
    }
    lazyLinkInit(parent, trd);
    if (simKey_ != null) {
      simLinkInit(parent);
    }

    //
    // FIX ME!  This misses the lack of Lim in v2 endoderm at 30 hours, because
    // it is expressing in veg1!!
    //
    //
    // This is REALLY slow! Don't do it if we aren't using the result!
    //
    
    /*
    grit = getGroupIterator();
    HashSet neededExpression = new HashSet();
    while (grit.hasNext()) {
      Group group = (Group)grit.next();
      List groupKeys = 
        tcd.getTimeCourseGroupKeysWithDefault(Group.getBaseID(group.getID()), 
                                              group.getInheritedTrueName(this));

      if (groupKeys == null) continue;
      Iterator gkit = groupKeys.iterator();
      while (gkit.hasNext()) {
        GroupUsage groupUse = (GroupUsage)gkit.next();
        if (groupUse == null) continue;
        if ((groupUse.usage != null) && (!groupUse.usage.equals(proxyID_))) {
          continue;
        }
        Iterator hit = times_.iterator();
        while (hit.hasNext()) {
          int time = ((Integer)hit.next()).intValue();
          tcd.getExpressedGenes(groupUse.mappedGroup, time, neededExpression);          
        }
      }
    }
    neededExpression.removeAll(caught);
    Iterator neit = neededExpression.iterator();
    while (neit.hasNext()) {
      String next = (String)neit.next();
      // FIX ME Do something here like a dialog...
    }
     */
    return;
  }
  
  /***************************************************************************
  **
  ** Lazy initialization
  */

  private void initCore(TimeCourseData tcd, GenomeInstance parent, Set<String> caught, String simKey) {
        
    TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    double weakLevel = dacx_.getDisplayOptsSource().getDisplayOptions().getWeakExpressionLevel();
    
    Iterator<Node> it = parent.getAllNodeIterator();
    while (it.hasNext()) {
      NodeInstance node = (NodeInstance)it.next();
      Group group = getGroupForNode(node.getID(), LEGACY_MODE);
      String trueName = null;
      //
      // If the node is not in one of our groups, we may still show it if it is 
      // designated as an extra node to our proxy:
      //
      String gid = null;      
      if (group == null) {
        Iterator<DynamicInstanceProxy.AddedNode> anit = dip.getAddedNodeIterator();
        while (anit.hasNext()) {
          DynamicInstanceProxy.AddedNode added = anit.next();
          if (added.nodeName.equals(node.getID())) {
            gid = added.groupToUse;
            trueName = getVfgParentRoot().getGroup(Group.getBaseID(gid)).getName();
            break;
          }
        }
        if (gid == null) {
          continue;
          
        }
      } else {
        gid = group.getID();
        trueName = group.getInheritedTrueName(this);
      }
      List<GroupUsage> groupKeys = 
        tcdm.getTimeCourseGroupKeysWithDefault(Group.getBaseID(gid), trueName); 
      if (groupKeys == null) {
        continue;
      }
      Iterator<GroupUsage> gkit = groupKeys.iterator();
      boolean keepLooking = true;
      while (gkit.hasNext()) {
        GroupUsage groupUse = gkit.next();
        if (groupUse == null) continue;
        if ((groupUse.usage != null) && (!groupUse.usage.equals(proxyID_))) {          
          continue;
        }
        String baseID = GenomeItemInstance.getBaseID(node.getID());        
        List<TimeCourseDataMaps.TCMapping> dataKeys = tcdm.getTimeCourseTCMDataKeysWithDefault(baseID, dacx_.getGenomeSource());
        // If no data is available, the keys list is null (6/17/04 not anymore!)
        // if (dataKeys == null) continue;
        Iterator<TimeCourseDataMaps.TCMapping> dkit = dataKeys.iterator();
        while (dkit.hasNext() && keepLooking) {
          TimeCourseDataMaps.TCMapping tcm = dkit.next();
          TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(tcm.name);
          if (tcg == null) {
            // FIX ME?? Used to throw illegal state exception: now not valid with default keys
            continue;
          }
          Iterator<Integer> hit = times_.iterator();
          boolean addIt = false;
          double simValMaxDiff = 0.0;             
          boolean partialLevel = true;
          double partialMax = 0.0;
          while (hit.hasNext()) {
            int time = hit.next().intValue();
            Double simVal = null;
            if (simKey != null) {
              UiUtil.fixMePrintout("Still knotty issues");
              int simExp = tcg.getExpressionLevelForSimulation(simKey, groupUse.mappedGroup, time, varLev);
              simVal = ExpressionEntry.discreteToVar(simExp, (simExp == ExpressionEntry.VARIABLE) ? Double.valueOf(varLev.level) : null, weakLevel);
            }
            int expression = tcg.getExpressionLevelForSource(groupUse.mappedGroup, time, tcm.channel, varLev);
            if (expression == ExpressionEntry.EXPRESSED) {
              addIt = true;
              partialLevel = false;
              if (simKey == null) {
                break;
              }
            } else if (expression == ExpressionEntry.WEAK_EXPRESSION) {
              addIt = true;
              partialMax = Math.max(partialMax, weakLevel);
            } else if (expression == ExpressionEntry.VARIABLE) {
              double level = varLev.level;
              // If it is variable, add it even if the current level is zero!
              //if (level > 0.0) {
              addIt = true;
              partialMax = Math.max(partialMax, level);
            }
            // Sim key comparison
            if (simKey != null) {
              Double oVal = ExpressionEntry.discreteToVar(expression, (expression == ExpressionEntry.VARIABLE) ? Double.valueOf(varLev.level) : null, weakLevel);
              if ((simVal != null) && (oVal != null)) {
                double diff = Math.abs(simVal.doubleValue() - oVal.doubleValue());
                if (diff > simValMaxDiff) {
                  simValMaxDiff = diff;
                  addIt = true;
                }
              }
            }
          }
          if (addIt) {
            NodeInstance newNode = node.clone();          
            //
            // Here is where we now make weak nodes show up as partially active:
            //
            if (partialLevel) {
              int parentActivity = node.getActivity();
              if (parentActivity == NodeInstance.ACTIVE) {
                newNode.setActivity(NodeInstance.VARIABLE);              
                newNode.setActivityLevel(partialMax);
              } else if (parentActivity == NodeInstance.VARIABLE) {
                newNode.setActivityLevel(Math.min(partialMax, node.getActivityLevel()));
              }
              if (simKey != null) {
                UiUtil.fixMePrintout("Note this reflects true diff not parent-clamped diff! OK?");
                newNode.setSimulationLevel(simValMaxDiff);
              }
            } else if (simKey != null) {
              newNode.setSimulationLevel(simValMaxDiff);
            }
            if (node.getNodeType() == Node.GENE) {
              this.genes_.put(newNode.getID(), (GeneInstance)newNode);
            } else {
              this.nodes_.put(newNode.getID(), newNode);
            }
            caught.add(tcg.getName());
            keepLooking = false;
          }
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Lazy initialization
  */

  private void lazyLinkInit(GenomeInstance parent, TemporalInputRangeData trd) {
    //
    // Crank through all the links in the root.  Go to the target
    // of each link.  Get the mappings for that target.  For each
    // mapping, see if we come up with temporal input data.  If
    // we do, get the mappings for the source, and see if any
    // source shows up.  If yes, take the resulting data and
    // find out if it is expressed at the time.

    DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxyID_);    
    Iterator<Linkage> lit = parent.getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance link = (LinkageInstance)lit.next();
      String target = link.getTarget();
      String baseID = GenomeItemInstance.getBaseID(target);
      List<String> rangeKeys = trd.getTemporalInputRangeEntryKeysWithDefault(baseID, dacx_.getGenomeSource());
      if (rangeKeys == null) continue;
      String source = link.getSource();
      String srcBaseID = GenomeItemInstance.getBaseID(source);
      List<String> srcKeys = trd.getTemporalInputRangeSourceKeysWithDefault(srcBaseID, dacx_.getGenomeSource());
      if (srcKeys == null) continue;
      // Find out the groups we are in
      // Build this into an acceptable list of target groups
      Group group = getGroupForNode(target, LEGACY_MODE);
      //
      // If the node is not in one of our groups, we may still show it if it is 
      // designated as an extra node to our proxy:
      //
      boolean force = false;
      String gid = null; 
      String trueName = null;      
      if (group == null) {
        Iterator<DynamicInstanceProxy.AddedNode> anit = dip.getAddedNodeIterator();
        while (anit.hasNext()) {
          DynamicInstanceProxy.AddedNode added = anit.next();
          if (added.nodeName.equals(target)) {
            gid = added.groupToUse;
            trueName = getVfgParentRoot().getGroup(Group.getBaseID(gid)).getName();
            force = true;
            break;
          }
        }
        if (gid == null) {
          continue;
        }
      } else {
        gid = group.getID();
        trueName = group.getInheritedTrueName(this);        
      }
      
      //
      // Do the same for extra sources:
      //
      
      Group srcGroup = getGroupForNode(source, LEGACY_MODE);
      boolean forceSrc = false;
      String srcGid = null; 
      String srcTrueName = null;      
      if (srcGroup == null) {
        Iterator<DynamicInstanceProxy.AddedNode> anit = dip.getAddedNodeIterator();
        while (anit.hasNext()) {
          DynamicInstanceProxy.AddedNode added = anit.next();
          if (added.nodeName.equals(source)) {
            srcGid = added.groupToUse;
            srcTrueName = getVfgParentRoot().getGroup(Group.getBaseID(srcGid)).getName();
            forceSrc = true;
            break;
          }
        }
        if (srcGid == null) {
          continue;
        }
      } else {
        srcGid = srcGroup.getID();
        srcTrueName = srcGroup.getInheritedTrueName(this);                 
      }

      //
      // 10/26/06: Start checking signs
      //
      int linkSign = link.getSign();
      
      Set<String> groupTargs = resolveGroupTargets(trd, gid, trueName);
      Set<String> srcGroupTargs = resolveGroupTargets(trd, srcGid, srcTrueName);

      Iterator<String> rkit = rangeKeys.iterator();
      while (rkit.hasNext()) {
        String rKey = rkit.next();
        TemporalRange tr = trd.getRange(rKey);
        if (tr == null) continue;
        Iterator<InputTimeRange> perts = tr.getTimeRanges();
        while (perts.hasNext()) {
          InputTimeRange pert = perts.next();
          Iterator<String> skit = srcKeys.iterator();
          while (skit.hasNext()) {
            String srcKey = skit.next();
            if (DataUtil.keysEqual(srcKey, pert.getName())) {
              Iterator<RegionAndRange> rit = pert.getRanges();
              while (rit.hasNext()) {
                RegionAndRange range = rit.next();                
                String reg = range.getRegion();
                if ((reg == null) || DataUtil.containsKey(groupTargs, reg)) {
                  //
                  // Need to ask here if the region and range is restricted to
                  // sources from another region, and eliminate accordingly.
                  //                  
                  String restrict = range.getRestrictedSource();
                  if ((restrict != null) && (!DataUtil.containsKey(srcGroupTargs, restrict))) {
                    continue;
                  }
                  //
                  // 10/26/06: Start checking signs
                  //
                  
                  if (!range.signMatch(linkSign)) {
                    continue;
                  }
                  
                  Iterator<Integer> hit = times_.iterator();
                  boolean addIt = false;
                  while (hit.hasNext()) {
                    int time = hit.next().intValue();
                    if (range.isActive(time)) {
                      addIt = true;
                      break;
                    }
                  }
                  if (addIt) {
                    if (!installNeededNode(parent, source, forceSrc)) {
                      //System.err.println("REPORT: could not install node for " + source + " : " + source + "->" + target);
                      break;
                    }
                    if (!installNeededNode(parent, target, force)) {
                      //System.err.println("REPORT: could not install node for " + target + " : " + source + "->" + target);
                      break;
                    }                    
                    // FIX ME? : Compare signs
                    LinkageInstance newLink= new LinkageInstance(link);
                    //
                    // Here is where we now make links from weak nodes show up as partially active:
                    //
                    NodeInstance sourceNode = (NodeInstance)getNode(source);
                    int sourceActivity = sourceNode.getActivity();
                    int linkActivity = newLink.getActivitySetting();
                    double currActivity = 1.0;
                    double dynLevel = 1.0;
                    // This is the inherited upper bound from the parent (1.0, or less if set)
                    if ((linkActivity == LinkageInstance.VARIABLE) || 
                        ((linkActivity == LinkageInstance.USE_SOURCE) && (sourceActivity == NodeInstance.VARIABLE))) {
                      currActivity = newLink.getActivityLevel(this);
                    }
                    // I.E. link is full-on AND source is variable (i.e. (this includes weak))-> make it variable instead.
                    if ((linkActivity == LinkageInstance.ACTIVE) && (sourceActivity == NodeInstance.VARIABLE)) {
                      newLink.setActivity(LinkageInstance.VARIABLE);
                      dynLevel = sourceNode.getActivityLevel();
                    }
                    // get the setting AGAIN
                    linkActivity = newLink.getActivitySetting();
                    if (linkActivity == LinkageInstance.VARIABLE) {
                      newLink.setActivityLevel(Math.min(dynLevel, currActivity));
                    }
                    links_.put(newLink.getID(), newLink);
                    break;
                  }
                }
              }
              break;
            }
          }
        }
      }
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Link init for simulation comparison.
  */

  private void simLinkInit(GenomeInstance parent) {
    //
    // When doing simulation comparison, we need to make sure that links with active 
    // sources get included, regardless of the results of lazyLinkInit and whether
    // the TemporalInputRangeData has anything to say about the link. So this serves
    // as a second pass.
    //

    Iterator<Linkage> lit = parent.getLinkageIterator();
    while (lit.hasNext()) {
      LinkageInstance parLink = (LinkageInstance)lit.next();
      String source = parLink.getSource();
      // Get MY source node; may not be there...
      NodeInstance srcNode = (NodeInstance)getNode(source);
      if (srcNode == null) {
        continue;
      }
      String targ = parLink.getTarget();
      // Get MY targ node; may not be there...
      NodeInstance trgNode = (NodeInstance)getNode(targ);
      if (trgNode == null) {
        continue;
      }
      LinkageInstance myLink = (LinkageInstance)this.getLinkage(parLink.getID());
      if (myLink == null) {
        myLink = new LinkageInstance(parLink); 
        links_.put(myLink.getID(), myLink);
      }
      Double simLev = srcNode.getSimulationLevel();
      myLink.setSimulationLevel(simLev);
    }  
    return;
  }

  /***************************************************************************
  **
  ** Lazy initialization
  */

  private Set<String> resolveGroupTargets(TemporalInputRangeData trd, String gid, String trueName) {   
    HashSet<String> groupTargs = new HashSet<String>();
    List<GroupUsage> grpKeys = 
      trd.getTemporalRangeGroupKeysWithDefault(Group.getBaseID(gid), trueName);
    if (grpKeys != null) {
      Iterator<GroupUsage> gkit = grpKeys.iterator();
      while (gkit.hasNext()) {
        GroupUsage groupUse = gkit.next();
        if (groupUse == null) continue;
        if ((groupUse.usage == null) || (groupUse.usage.equals(proxyID_))) {
          groupTargs.add(DataUtil.normKey(groupUse.mappedGroup));      
        }
      }
    }
    return (groupTargs);
  }  
  
  /***************************************************************************
  **
  ** Lazy initialization
  */

  private boolean installNeededNode(GenomeInstance parent, String nodeID, boolean force) {  
    Node srcNode = this.getNode(nodeID);
    if (srcNode == null) {
      Node parentNode = parent.getNode(nodeID);
      if (parentNode == null) {
        return (false);
      }
      int nodeType = parentNode.getNodeType();
/*      if (nodeType == Node.INTERCELL) {  // Doesn't belong to a group
        NodeInstance newNode = new NodeInstance((NodeInstance)parentNode);
        newNode.setActivity(NodeInstance.ACTIVE);
        nodes_.put(parentNode.getID(), newNode);
        return (true);
      } */
      Group group = getGroupForNode(nodeID, LEGACY_MODE);
      if ((group == null) && !force) {
        return (false);
      }
      if (nodeType == Node.GENE) {
        GeneInstance newNode = new GeneInstance((GeneInstance)parentNode);
        newNode.setActivity(NodeInstance.INACTIVE);
        genes_.put(parentNode.getID(), newNode);
      } else {
        NodeInstance newNode = new NodeInstance((NodeInstance)parentNode);
        newNode.setActivity(NodeInstance.INACTIVE);
        nodes_.put(parentNode.getID(), newNode);
      }
    }  
    return (true);
  }
}
