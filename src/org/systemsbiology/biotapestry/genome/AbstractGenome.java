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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** Genome abstract base class
*/

public abstract class AbstractGenome implements Genome, Cloneable {   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE/PROTECTED VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  protected BTState appState_;
  protected String description_;
  protected HashMap<String, Gene> genes_;
  protected String id_;
  protected String imgKey_;
  protected UniqueLabeller labels_;
  protected HashMap<String, Linkage> links_;
  protected String longName_;
  protected String name_;
  protected OverlayOpsSupport ovrops_;
  protected HashMap<Integer, String> nodeCollMap_;
  protected HashMap<String, Node> nodes_;
  protected HashMap<String, Note> notes_;  
  protected int ownerMode_;
  protected GenomeSource mySource_;
  private boolean labelManager_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public AbstractGenome(BTState appState, String name, String id, int overlayOwnerMode, boolean labelMgr) {
    ownerMode_ = overlayOwnerMode;
    appState_ = appState;
    name_ = name;
    id_ = id;
    genes_ = new HashMap<String, Gene>();
    nodes_ = new HashMap<String, Node>();
    links_ = new HashMap<String, Linkage>();
    notes_ = new HashMap<String, Note>();
    labels_ = new UniqueLabeller();
    imgKey_ = null;
    mySource_ = null;
    ovrops_ = new OverlayOpsSupport(appState_, overlayOwnerMode, id_);
    labelManager_ = labelMgr;  // Handles labels for nodes, genes, links, i.e. ROOT GENOME ONLY!!
  }  

  /***************************************************************************
  **
  ** Copy constructor that is used to create a sibling.
  */

  public AbstractGenome(AbstractGenome other, boolean overAndNotes) {
    this.appState_ = other.appState_;
    this.name_ = other.name_;
    this.id_ = other.id_;
    this.mySource_ = other.mySource_;
    this.imgKey_ = other.imgKey_;
    if (other.longName_ != null) {
      this.longName_ = other.longName_;
    }
    if (other.description_ != null) {
      this.description_ = other.description_;
    }
    ownerMode_ = other.ownerMode_;
    labels_ = other.labels_.clone();
 
    this.genes_ = new HashMap<String, Gene>();
    Iterator<String> git = other.genes_.keySet().iterator();
    while (git.hasNext()) {
      String gID = git.next();
      this.genes_.put(gID, other.genes_.get(gID).clone());
    }

    this.nodes_ = new HashMap<String, Node>();
    Iterator<String> nit = other.nodes_.keySet().iterator();
    while (nit.hasNext()) {
      String nID = nit.next();
      this.nodes_.put(nID, other.nodes_.get(nID).clone());
    }    

    this.links_ = new HashMap<String, Linkage>();
    Iterator<String> lit = other.links_.keySet().iterator();
    while (lit.hasNext()) {
      String lID = lit.next();
      this.links_.put(lID, other.links_.get(lID).clone());
    }    

    if (overAndNotes) {
      this.ovrops_ = other.ovrops_.clone();
  
      this.notes_ = new HashMap<String, Note>();
      Iterator<String> noit = other.notes_.keySet().iterator();
      while (noit.hasNext()) {
        String noID = noit.next();
        this.notes_.put(noID, other.notes_.get(noID).clone());
      }            
    } else {
      this.ovrops_ = new OverlayOpsSupport(appState_, ownerMode_, id_);
      this.notes_ = new HashMap<String, Note>();
    }
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set optional alternate genome source
  **
  */
  
  public void setGenomeSource(GenomeSource gSrc) {
    mySource_ = gSrc;
    return;
  }
  
  /***************************************************************************
  **
  ** Add a network module view to the genome
  **
  */
  
  public NetworkOverlayOwnerChange addNetworkOverlay(NetworkOverlay nmView) {
    return (ovrops_.addNetworkOverlay(nmView));
  }
  
  /***************************************************************************
  **
  ** Add a network module view to the genome.  Use for IO
  **
  */
  
  public void addNetworkOverlayAndKey(NetworkOverlay nmView) throws IOException {
    ovrops_.addNetworkOverlayAndKey(nmView);
    return;
  } 

  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome.  Use for IO
  **
  */
  
  public void addNetworkModuleAndKey(String overlayKey, NetModule module) throws IOException {
    ovrops_.addNetworkModuleAndKey(overlayKey, module);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.  Use for IO
  **
  */
  
  public void addNetworkModuleLinkageAndKey(String overlayKey, NetModuleLinkage linkage) throws IOException {
    ovrops_.addNetworkModuleLinkageAndKey(overlayKey, linkage);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.
  **
  */
  
  public NetworkOverlayChange addNetworkModuleLinkage(String overlayKey, NetModuleLinkage linkage) {
    return (ovrops_.addNetworkModuleLinkage(overlayKey, linkage));
  }
  
  /***************************************************************************
  **
  ** Remove a network module linkage from an overlay in this owner.
  **
  */
  
  public NetworkOverlayChange removeNetworkModuleLinkage(String overlayKey, String linkageKey) {  
    return (ovrops_.removeNetworkModuleLinkage(overlayKey, linkageKey));
  }  
 
  /***************************************************************************
  **
  ** Modify a network module linkage in an overlay in this genome.
  **
  */
 
  public NetworkOverlayChange modifyNetModuleLinkage(String overlayKey, String linkKey, int newSign) {  
    return (ovrops_.modifyNetModuleLinkage(overlayKey, linkKey, newSign));
  }    
  
  /***************************************************************************
  **
  ** Remove the network module view from the genome
  **
  */
  
  public NetworkOverlayOwnerChange removeNetworkOverlay(String key) {
    return (ovrops_.removeNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get a network overlay from the genome
  **
  */
  
  public NetworkOverlay getNetworkOverlay(String key) {
    return (ovrops_.getNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get an iterator over network overlays
  **
  */
  
  public Iterator<NetworkOverlay> getNetworkOverlayIterator() {
    return (ovrops_.getNetworkOverlayIterator());
  }  
  
  /***************************************************************************
  **
  ** Get the count of network overlays
  **
  */
  
  public int getNetworkOverlayCount() {
    return (ovrops_.getNetworkOverlayCount());
  }
  
  /***************************************************************************
  **
  ** Get the count of network modules
  **
  */
  
  public int getNetworkModuleCount() {
    return (ovrops_.getNetworkModuleCount());
  }      
   
  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome
  **
  */
  
  public NetworkOverlayChange addNetworkModule(String overlayKey, NetModule module) {
    return (ovrops_.addNetworkModule(overlayKey, module));
  }
  
  /***************************************************************************
  **
  ** Remove a network module from an overlay in this genome
  **
  */
  
  public NetworkOverlayChange[] removeNetworkModule(String overlayKey, String moduleKey) {  
    return (ovrops_.removeNetworkModule(overlayKey, moduleKey));    
  }
 
  /***************************************************************************
  **
  ** Add a new member to a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange addMemberToNetworkModule(String overlayKey, NetModule module, String nodeID) {
    return (ovrops_.addMemberToNetworkModule(overlayKey, module, nodeID));
  } 
  
  /***************************************************************************
  **
  ** Remove a member from a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange deleteMemberFromNetworkModule(String overlayKey, NetModule module, String nodeID){
    return (ovrops_.deleteMemberFromNetworkModule(overlayKey, module, nodeID));
  } 

  /***************************************************************************
  **
  ** Return matching Network Modules (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
  
  public Map<String, Set<String>> findMatchingNetworkModules(int searchMode, String key, NameValuePair nvPair) {
    return (ovrops_.findMatchingNetworkModules(searchMode, key, nvPair));
  }
  
  /***************************************************************************
  **
  ** Get the firstView preference
  **
  */   
  
  public String getFirstViewPreference(TaggedSet modChoice, TaggedSet revChoice) {
    return (ovrops_.getFirstViewPreference(modChoice, revChoice));
  }      
  
  /***************************************************************************
  **
  ** Add a gene to the genome
  **
  */
  
  public GenomeChange addGene(Gene gene) {
    GenomeChange retval = new GenomeChange();
    genes_.put(gene.getID(), gene);
    retval.gOrig = null;
    retval.gNew = gene;
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a link to the genome
  **
  */
  
  public GenomeChange addLinkage(Linkage link) {
    GenomeChange retval = new GenomeChange();
    links_.put(link.getID(), link);
    retval.lOrig = null;
    retval.lNew = link;
    retval.genomeKey = getID();
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Add a miscellaneous node to the genome
  **
  */
  
  public GenomeChange addNode(Node node) {
    GenomeChange retval = new GenomeChange();
    nodes_.put(node.getID(), node);
    retval.nOrig = null;
    retval.nNew = node;
    retval.genomeKey = getID();
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  public void addNote(Note note) {
    String id = note.getID();
    notes_.put(id, note);
    if (!labels_.addExistingLabel(id)) {
      System.err.println("Don't like " + id);
      throw new IllegalArgumentException();
    }    
    return;
  }  

  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  public GenomeChange addNoteWithExistingLabel(Note note) {
    String id = note.getID();
    if (notes_.get(id) != null) {
      throw new IllegalArgumentException();
    }
    GenomeChange retval = new GenomeChange();
    notes_.put(id, note);
    retval.ntOrig = null;
    retval.ntNew = note;
    retval.genomeKey = id_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Append to the description
  **
  */
  
  public void appendDescription(String description) {
    if (description_ == null) {
      description_ = description;
    } else {
      description_ = description_.concat(description);
    }
    description_ = CharacterEntityMapper.unmapEntities(description_, false);
    return;
  }
   
  /***************************************************************************
  **
  ** Append to the long display name
  **
  */
  
  public void appendLongName(String longName) {
    if (longName_ == null) {
      longName_ = longName;
    } else {
      longName_ = longName_.concat(longName);
    }
    longName_ = CharacterEntityMapper.unmapEntities(longName_, false);    
    return;
  }   
  
  /***************************************************************************
  **
  ** Can we write to SBML
  **
  */
  
  public abstract boolean canWriteSBML();  
  
  /***************************************************************************
  **
  ** Change the gene evidence level
  **
  */
    
  public abstract GenomeChange changeGeneEvidence(String geneID, int evidence);  
  
  /***************************************************************************
  **
  ** Change the gene Name
  **
  */
  
  public abstract GenomeChange changeGeneName(String geneID, String name); 
  
  /***************************************************************************
  **
  ** Change the gene regions
  **
  */

  public abstract GenomeChange changeGeneRegions(String geneID, List<DBGeneRegion> newRegions);
  
  /***************************************************************************
  **
  ** Change the gene size
  **
  */
    
  public abstract GenomeChange changeGeneSize(String geneID, int pads);
  
  /***************************************************************************
  **
  ** Change the link description
  **
  */
  
  public GenomeChange changeLinkageDescription(String linkID, String desc) {
    GenomeChange retval = new GenomeChange();
    Linkage theLink = getLinkage(linkID);
    retval.lOrig = theLink.clone();
    retval.lNew = retval.lOrig.clone();
    retval.lNew.setDescription(desc);
    links_.put(linkID, retval.lNew.clone());   
    retval.genomeKey = id_;    
    return (retval);
  }

  /***************************************************************************
  **
  ** Change the linkage source
  **
  */
  
  public GenomeChange changeLinkageSource(Linkage link, int targNum) {
    String id = link.getID();    
    GenomeChange retval = new GenomeChange();
    if (links_.get(id) != link) {
      throw new IllegalArgumentException();
    }
    retval.lOrig = link.clone();
    retval.lNew = link.clone();
    retval.lNew.setLaunchPad(targNum);
    links_.put(id, retval.lNew.clone());
    retval.genomeKey = getID();
    return (retval);
  }

  /***************************************************************************
  **
  ** Change the linkage source node
  **
  */
  
  public GenomeChange changeLinkageSourceNode(Linkage link, String srcID, int targNum) {
    String id = link.getID();    
    GenomeChange retval = new GenomeChange();
    if (links_.get(id) != link) {
      throw new IllegalArgumentException();
    }
    retval.lOrig = link.clone();
    retval.lNew = link.clone();
    retval.lNew.setSource(srcID);
    retval.lNew.setLaunchPad(targNum);
    links_.put(id, retval.lNew.clone());
    retval.genomeKey = getID();
    return (retval);    
  }

  /***************************************************************************
  **
  ** Change the linkage target
  **
  */
  
  public GenomeChange changeLinkageTarget(Linkage link, int targNum) {
    String id = link.getID();
    GenomeChange retval = new GenomeChange();
    if (links_.get(id) != link) {
      throw new IllegalArgumentException();
    }
    retval.lOrig = link.clone();
    retval.lNew = link.clone();
    retval.lNew.setLandingPad(targNum);
    links_.put(id, retval.lNew.clone());
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the linkage target node
  **
  */
  
  public GenomeChange changeLinkageTargetNode(Linkage link, String trgID, int targNum) {
    String id = link.getID();    
    GenomeChange retval = new GenomeChange();
    if (links_.get(id) != link) {
      throw new IllegalArgumentException();
    }
    retval.lOrig = link.clone();
    retval.lNew = link.clone();
    retval.lNew.setTarget(trgID);
    retval.lNew.setLandingPad(targNum);
    links_.put(id, retval.lNew.clone());
    retval.genomeKey = getID();
    return (retval);    
  } 
  
  /***************************************************************************
  **
  ** Change the linkage URLs
  **
  */
  
  public GenomeChange changeLinkageURLs(String linkID, List<String> urls) {
    GenomeChange retval = new GenomeChange();
    Linkage theLink = getLinkage(linkID);
    retval.lOrig = theLink.clone();
    retval.lNew = retval.lOrig.clone();
    retval.lNew.setAllUrls(urls);
    links_.put(linkID, retval.lNew.clone());    
    retval.genomeKey = id_;    
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Change the node description
  **
  */
  
  public GenomeChange changeNodeDescription(String nodeID, String desc) {
    GenomeChange retval = new GenomeChange();
    Node theNode = getNode(nodeID);
    if (theNode.getNodeType() == DBNode.GENE) {
      retval.gOrig = (Gene)theNode.clone();
      retval.gNew = retval.gOrig.clone();
      retval.gNew.setDescription(desc);
      genes_.put(nodeID, retval.gNew.clone());    
    } else {
      retval.nOrig = theNode.clone();
      retval.nNew = retval.nOrig.clone();
      retval.nNew.setDescription(desc);
      nodes_.put(nodeID, retval.nNew.clone());                 
    }
    retval.genomeKey = id_;    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the node Name
  **
  */
  
  public abstract GenomeChange changeNodeName(String nodeID, String name);
  
  /***************************************************************************
  **
  ** Change the node size
  **
  */
  
  public abstract GenomeChange changeNodeSize(String nodeID, int pads);
  
  /***************************************************************************
  **
  ** Very implementation dependent
  **
  */
  
  public abstract GenomeChange changeNodeType(String nodeID, int type);
  
  /***************************************************************************
  **
  ** Change the node URLs
  **
  */
  
  public GenomeChange changeNodeURLs(String nodeID, List<String> urls) {
    GenomeChange retval = new GenomeChange();
    Node theNode = getNode(nodeID);
    if (theNode.getNodeType() == DBNode.GENE) {
      retval.gOrig = (Gene)theNode.clone();
      retval.gNew = retval.gOrig.clone();
      retval.gNew.setAllUrls(urls);
      genes_.put(nodeID, retval.gNew.clone());    
    } else {
      retval.nOrig = theNode.clone();
      retval.nNew = retval.nOrig.clone();
      retval.nNew.setAllUrls(urls);
      nodes_.put(nodeID, retval.nNew.clone());                 
    }
    retval.genomeKey = id_;    
    return (retval);    
  } 
  
  /***************************************************************************
  **
  ** Change the note in the genome
  **
  */
  
  public GenomeChange changeNote(Note note, String newLabel, String newText, boolean isInteractive) {
    Note newNote = new Note(note);
    newNote.setName(newLabel);
    newNote.setText(newText);
    newNote.setInteractive(isInteractive);
    notes_.put(newNote.getID(), newNote);
    GenomeChange retval = new GenomeChange();
    retval.ntOrig = note;
    retval.ntNew = newNote;
    retval.genomeKey = id_;
    return (retval);
  } 
  
  
  /***************************************************************************
  **
  ** Redo a change
  */

  public abstract void changeRedo(GenomeChange undo);
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  protected boolean changeRedoSupport(GenomeChange undo) {
    if (((undo.gOrig != null) && (undo.nNew != null)) ||
        ((undo.nOrig != null) && (undo.gNew != null))) {
      typeChangeRedo(undo);
      return (true);
    } else if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeRedo(undo);
      return (true);
    } else if ((undo.nOrig != null) || (undo.nNew != null)) {
      nodeChangeRedo(undo);
      return (true);
    } else if ((undo.lOrig != null) || (undo.lNew != null)) {
      linkChangeRedo(undo);
      return (true);
    } else if ((undo.ntOrig != null) || (undo.ntNew != null)) {
      noteChangeRedo(undo);
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */

  public abstract void changeUndo(GenomeChange undo);

  /***************************************************************************
  **
  ** Undo a change
  */
  
  protected boolean changeUndoSupport(GenomeChange undo) {
    if (((undo.gOrig != null) && (undo.nNew != null)) ||
        ((undo.nOrig != null) && (undo.gNew != null))) {
      typeChangeUndo(undo);
      return (true);
    } else if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeUndo(undo);
      return (true);
    } else if ((undo.nOrig != null) || (undo.nNew != null)) {
      nodeChangeUndo(undo);
      return (true);
    } else if ((undo.lOrig != null) || (undo.lNew != null)) {
      linkChangeUndo(undo);
      return (true);
    } else if ((undo.ntOrig != null) || (undo.ntNew != null)) {
      noteChangeUndo(undo);
      return (true);
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Clone
  */

  public AbstractGenome clone() {
    try {
      AbstractGenome retval = (AbstractGenome)super.clone();
  
      retval.genes_ = new HashMap<String, Gene>();
      Iterator<String> git = this.genes_.keySet().iterator();
      while (git.hasNext()) {
        String gID = git.next();
        retval.genes_.put(gID, this.genes_.get(gID).clone());
      }

      retval.nodes_ = new HashMap<String, Node>();
      Iterator<String> nit = this.nodes_.keySet().iterator();
      while (nit.hasNext()) {
        String nID = nit.next();
         retval.nodes_.put(nID, this.nodes_.get(nID).clone());
      }    

      retval.links_ = new HashMap<String, Linkage>();
      Iterator<String> lit = this.links_.keySet().iterator();
      while (lit.hasNext()) {
        String lID = lit.next();
         retval.links_.put(lID, this.links_.get(lID).clone());
      }    

      retval.ovrops_ = this.ovrops_.clone();
   
      retval.notes_ = new HashMap<String, Note>();
      Iterator<String> noit = this.notes_.keySet().iterator();
      while (noit.hasNext()) {
        String noID = noit.next();
        retval.notes_.put(noID, this.notes_.get(noID).clone());
      }            
      
      retval.labels_ = this.labels_.clone();
      retval.mySource_ = this.mySource_;
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
 
  /***************************************************************************
  **
  ** Drop the genome image
  **
  */
  
  public ImageChange dropGenomeImage() {
    ImageManager mgr = appState_.getImageMgr();
    if (imgKey_ != null) {
      ImageChange dropChange = mgr.dropImageUsage(imgKey_);
      dropChange.genomeKey = id_;
      imgKey_ = null;
      return (dropChange);
    }
    imgKey_ = null;
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get an Iterator over the all the nodes (genes and other nodes):
  **
  */
  
  public Iterator<Node> getAllNodeIterator() {
    HashSet<Node> allSet = new HashSet<Node>(nodes_.values());
    allSet.addAll(genes_.values());
    return (allSet.iterator());
  }  
  
  /***************************************************************************
  **
  ** Get the description
  **
  */
  
  public String getDescription() {
    return (description_);
  }
  


  /***************************************************************************
  **
  ** Get full (gene + other nodes) count:
  **
  */
  
  public int getFullNodeCount() {
    return (genes_.size() + nodes_.size());
  }

  /***************************************************************************
  **
  ** Get a particular gene
  **
  */
  
  public Gene getGene(String key) {
    return (genes_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get gene count:
  **
  */
  
  public int getGeneCount() {
    return (genes_.size());
  }

  /***************************************************************************
  **
  ** Get an Iterator over the genes:
  **
  */
  
  public Iterator<Gene> getGeneIterator() {
    return (genes_.values().iterator());
  }
  
  /***************************************************************************
  **
  ** Get the genome image key
  **
  */
  
  public String getGenomeImage() {
    return (imgKey_);
  } 
  
  /***************************************************************************
  **
  ** Get the id
  **
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  ** 
  ** Get the number of inbound links to the node
  */

  public int getInboundLinkCount(String nodeID) {

    int count = 0;
    Iterator<? extends Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String trgID = link.getTarget();
      if (nodeID.equals(trgID)) {
        count++;
      }
    }
    return (count);
  }
  
  /***************************************************************************
  **
  ** Get a particular link
  **
  */
  
  public Linkage getLinkage(String key) {
    return (links_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get linkage count:
  **
  */
  
  public int getLinkageCount() {
    return (links_.size());
  } 
  
  /***************************************************************************
  **
  ** Get an Iterator over the links:
  **
  */
  
  public Iterator<Linkage> getLinkageIterator() {
    return (links_.values().iterator());
  }
 
  /***************************************************************************
  **
  ** Get the count of links between the source and target
  */  
 
  public int getLinkCount(String srcNodeID, String trgNodeID) {
    int count = 0;
    Iterator<? extends Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(srcNodeID) && link.getTarget().equals(trgNodeID)) {
        count++;
      }
    }
    return (count);
  }
 
  /***************************************************************************
  **
  ** Get the long display name
  **
  */
  
  public String getLongName() {
    return (longName_);
  }
  
  /***************************************************************************
  **
  ** Get the name
  **
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Get all the names up to the root
  **
  */
   
  public abstract List<String> getNamesToRoot();

  /***************************************************************************
  ** 
  ** Get the next note key
  */

  public String getNextNoteKey() {
    return (labels_.getNextLabel());
  }
  
  /***************************************************************************
  **
  ** Get a particular node, including genes:
  **
  */
  
  public Node getNode(String key) {
    Node retval = nodes_.get(key);
    if (retval != null) {
      return (retval);
    } else {
      return (genes_.get(key));
    }
  }  
  
  /***************************************************************************
  **
  ** Get an Iterator over the miscellaneous nodes:
  **
  */
  
  public Iterator<Node> getNodeIterator() {
    return (nodes_.values().iterator());
  }
  
  /***************************************************************************
  **
  ** Answers with the minimum number of (launch, landing) pads needed by this
  ** genome for the given node.
  **
  */
  
  public PadCalculatorToo.PadResult getNodePadRequirements(Node whichNode, Layout lo) {
    String nodeID = whichNode.getID();
    int minLandPad = Integer.MAX_VALUE;
    int maxLandPad = Integer.MIN_VALUE;
    int minLaunchPad = Integer.MAX_VALUE;
    int maxLaunchPad = Integer.MIN_VALUE;    
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String targ = link.getTarget();
      if (targ.equals(nodeID)) {
        int linkPad = link.getLandingPad();
        if (linkPad > maxLandPad) {
          maxLandPad = linkPad;
        }
        if (linkPad < minLandPad) {
          minLandPad = linkPad;
        }       
      }  
      String src = link.getSource();     
      if (src.equals(nodeID)) {
        int linkPad = link.getLaunchPad();
        if (linkPad > maxLaunchPad) {
          maxLaunchPad = linkPad;
        }
        if (linkPad < minLaunchPad) {
          minLaunchPad = linkPad;
        }       
      }    
    }
    
    if (lo.getNodeProperties(nodeID) == null) {
      System.err.println("Buggy recovered layout: " + nodeID);
      throw new IllegalArgumentException();
    }
       
    INodeRenderer nodeRenderer = lo.getNodeProperties(nodeID).getRenderer();
    
    int trueLandMax = (minLandPad < 0) ? nodeRenderer.getFixedLandingPadMax() - minLandPad : maxLandPad;
    int trueLaunchMax = (minLaunchPad < 0) ? nodeRenderer.getFixedLaunchPadMax() - minLaunchPad : maxLaunchPad;    

    if (nodeRenderer.sharedPadNamespaces()) {
      int fullMax = (trueLandMax > trueLaunchMax) ? trueLandMax : trueLaunchMax;
      trueLandMax = fullMax;
      trueLaunchMax = fullMax;
    }    
    
    PadCalculatorToo.PadResult retval = new PadCalculatorToo.PadResult(trueLaunchMax, trueLandMax);
 
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Get the IDs of the nodes that have links targeting the given node.
  **
  */
    
  public Set<String> getNodeSources(String nodeID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> lit = links_.values().iterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getTarget().equals(nodeID)) {
        retval.add(lnk.getSource());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get node targets
  **
  */
  
  public Set<String> getNodeTargets(String nodeID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> lit = links_.values().iterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getSource().equals(nodeID)) {
        retval.add(lnk.getTarget());
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get a particular note
  **
  */
  
  public Note getNote(String key) {
    return (notes_.get(key));
  }
   
  /***************************************************************************
  **
  ** Get an Iterator over the notes:
  **
  */
  
  public Iterator<Note> getNoteIterator() {
    return (notes_.values().iterator());
  } 
  
  /***************************************************************************
  ** 
  ** Get the number of outbound links from the node
  */

  public int getOutboundLinkCount(String nodeID) {
    int count = 0;
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      if (nodeID.equals(srcID)) {
        count++;
      }
    }
    return (count);
  }  

  /***************************************************************************
  ** 
  ** Get the IDs of all outbound links from the node
  */

  public Set<String> getOutboundLinks(String nodeID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      if (nodeID.equals(srcID)) {
        retval.add(link.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a sorted iterator over the genes:
  **
  */
  
  public Iterator<Gene> getSortedGeneIterator() {
    TreeSet<Gene> sorted = new TreeSet<Gene>(new AbstractGenome.NodeComparator());
    sorted.addAll(genes_.values());
    return (sorted.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the source count for the given node
  */
  
  public int getSourceCount(String nodeID) {
    Set<String> srcs = getNodeSources(nodeID);
    return (srcs.size());
  } 
  
  /***************************************************************************
  **
  ** Get the single source pad for a node.  null if the node is not a source.
  */  
 
  public Integer getSourcePad(String nodeID) {
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(nodeID)) {
        return (new Integer(link.getLaunchPad()));
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get map of node to used source AND target pads. Use this for large batch operations
  ** to avoid O(l^2) behavior.
  */  
 
  public PadCalculatorToo.PadCache getFullPadMap() {
    PadCalculatorToo.PadCache retval = new PadCalculatorToo.PadCache();
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      int launch = link.getLaunchPad();
      retval.addSource(src, launch);
 
      String trg = link.getTarget();
      int landing = link.getLandingPad();
      retval.addTarget(trg, landing);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop all nodes, genes, and links.  Keep everything else.
  */
  
  public abstract Genome getStrippedGenomeCopy();
  
  /***************************************************************************
  **
  ** Get the target count for the given node
  */
  
  public int getTargetCount(String nodeID) {
    Set<String> targs = getNodeTargets(nodeID);
    return (targs.size());
  } 
  
  /***************************************************************************
  **
  ** Answers with all links having target pad collisions
  **
  */
  
  public Set<String> hasLinkTargetPadCollisions() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Linkage> lit1 = this.getLinkageIterator();
    while (lit1.hasNext()) {
      Linkage link1 = lit1.next();
      String link1Targ = link1.getTarget();
      int link1Pad = link1.getLandingPad();
      Iterator<Linkage> lit2 = this.getLinkageIterator();
      while (lit2.hasNext()) {
        Linkage link2 = lit2.next();
        if (link1 == link2) {
          continue;
        }
        String link2Targ = link2.getTarget();
        if (!link1Targ.equals(link2Targ)) {
          continue;
        }
        int link2Pad = link2.getLandingPad();        
        if (link1Pad == link2Pad) {
          retval.add(link1.getID());
          retval.add(link2.getID());
        }
      }
    }
    return (retval);       
  }    
  
  /***************************************************************************
  **
  ** Redo an image change
  */
  
  public void imageChangeRedo(ImageChange redo) { 
    ImageManager mgr = appState_.getImageMgr();
    mgr.changeRedo(redo);
    if (redo.countOnlyKey != null) {
      // FIX FOR BT-10-25-07:1 ??
      imgKey_ = (redo.oldCount > redo.newCount) ? null : redo.countOnlyKey;
      //return (redo.countOnlyKey);
      return;
    } else if (redo.newKey != null) {
      imgKey_ =  redo.newKey;
      return;
    } else if (redo.oldKey != null) {
      imgKey_ = null;
      return;
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** Undo an image change
  */
  
  public void imageChangeUndo(ImageChange undo) {
    ImageManager mgr = appState_.getImageMgr();
    mgr.changeUndo(undo);
    if (undo.countOnlyKey != null) {
      // FIX FOR BT-10-25-07:1 ??
      imgKey_ = (undo.newCount > undo.oldCount) ? null : undo.countOnlyKey;
      return;
    } else if (undo.newKey != null) {
      imgKey_ = null;
      return;
    } else if (undo.oldKey != null) {
      imgKey_ = undo.oldKey;
      return;
    }
    throw new IllegalStateException();        
  }
  
  /***************************************************************************
  **
  ** Install pad changes (calculated previously)
  **
  */
  
  public GenomeChange[] installPadChanges(Map<String, Integer> launchPads, Map<String, Integer> landPads) {

    ArrayList<GenomeChange> retList = new ArrayList<GenomeChange>();
    
    // 
    // Change pads
    //
    
    Iterator<String> doit = launchPads.keySet().iterator();
    while (doit.hasNext()) {
      String key = doit.next();
      Linkage link = getLinkage(key);
      GenomeChange retval = new GenomeChange();
      retval.genomeKey = this.getID();  
      retval.lOrig = link.clone();
      link.setLaunchPad(launchPads.get(key).intValue());
      retval.lNew = link.clone();
      retList.add(retval);   
    }

    Iterator<String> diit = landPads.keySet().iterator();
    while (diit.hasNext()) {
      String key = diit.next();
      Linkage link = getLinkage(key);
      GenomeChange retval = new GenomeChange();
      retval.genomeKey = this.getID();  
      retval.lOrig = link.clone();
      link.setLandingPad(landPads.get(key).intValue());
      retval.lNew = link.clone();
      retList.add(retval);   
    }

    return (retList.toArray(new GenomeChange[retList.size()]));
  }  
  
  /***************************************************************************
  **
  ** Answer if it is empty
  */
  
  public boolean isEmpty() {
    return ((genes_.size() == 0) && (nodes_.size() == 0) && (links_.size() == 0) && (notes_.size() == 0));
  } 
  
  /***************************************************************************
  **
  ** Redo an overlay change
  **
  */
  
  public void overlayChangeRedo(NetworkOverlayOwnerChange redo) {
    GenomeSource gSrc = (mySource_ == null) ? appState_.getDB() : mySource_;
    if ((redo.nmvOrig != null) && (redo.nmvNew != null)) {
      throw new IllegalArgumentException();
    } else if ((redo.nmvOrig == null) && (redo.nmvNew != null)) {
      ((DBGenome)gSrc.getGenome()).addKey(redo.nmvNew.getID());
      ovrops_.getNetworkOverlayMap().put(redo.nmvNew.getID(), redo.nmvNew.clone());
    } else if ((redo.nmvOrig != null) && (redo.nmvNew == null)) {
      ((DBGenome)gSrc.getGenome()).removeKey(redo.nmvOrig.getID());
      ovrops_.getNetworkOverlayMap().remove(redo.nmvOrig.getID());
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo an overlay change
  **
  */
  
  public void overlayChangeUndo(NetworkOverlayOwnerChange undo) {
    GenomeSource gSrc = (mySource_ == null) ? appState_.getDB() : mySource_;
    if ((undo.nmvOrig != null) && (undo.nmvNew != null)) {
      throw new IllegalArgumentException();
    } else if ((undo.nmvOrig == null) && (undo.nmvNew != null)) {     
      ((DBGenome)gSrc.getGenome()).removeKey(undo.nmvNew.getID());
      ovrops_.getNetworkOverlayMap().remove(undo.nmvNew.getID());
    } else if ((undo.nmvOrig != null) && (undo.nmvNew == null)) {
      ((DBGenome)gSrc.getGenome()).addKey(undo.nmvOrig.getID());
      ovrops_.getNetworkOverlayMap().put(undo.nmvOrig.getID(), undo.nmvOrig.clone());
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the overlay mode
  **
  */
  
  public int overlayModeForOwner() {
    return (ownerMode_);
  }
  
  /***************************************************************************
  **
  ** Remove the link from the genome
  **
  */
  
  public GenomeChange removeLinkage(String key) {
    GenomeChange retval = new GenomeChange();
    retval.lOrig = links_.get(key);
    links_.remove(key);
    retval.lNew = null;
    retval.genomeKey = getID();    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Remove a particular node, including genes:
  **
  */
  
  public GenomeChange removeNode(String key) {
    GenomeChange retval = new GenomeChange();
    Node rem = nodes_.get(key);
    if (rem != null) {
      nodes_.remove(key);
      retval.nOrig = rem;
      retval.nNew = null;
    } else {
      retval.gOrig = genes_.get(key);      
      genes_.remove(key);
      retval.gNew = null;      
    }
    retval.genomeKey = getID();
    return (retval);
  }

  /***************************************************************************
  **
  ** Remove a particular note:
  **
  */
  
  public GenomeChange removeNote(String key) {
    GenomeChange retval = new GenomeChange();
    retval.ntOrig = notes_.remove(key);
    retval.ntNew = null;
    retval.genomeKey = id_;
    labels_.removeLabel(key);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Replace a miscellaneous gene in the genome
  **
  */
  
  public GenomeChange replaceGene(Gene gene) {
    GenomeChange retval = new GenomeChange();
    retval.gOrig = genes_.get(gene.getID());
    retval.gNew = gene;
    genes_.put(gene.getID(), gene);
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace a miscellaneous node in the genome
  **
  */
  
  public GenomeChange replaceNode(Node node) {
    GenomeChange retval = new GenomeChange();
    retval.nOrig = nodes_.get(node.getID());
    retval.nNew = node;
    nodes_.put(node.getID(), node);
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Fixup pad changes
  **
  */
  
  public GenomeChange[] resolvePadChanges(String nodeID, NodeProperties.PadLimits padLimits) {
   
    HashMap<String, Integer> launchPads = new HashMap<String, Integer>();
    HashMap<String, Integer> landPads = new HashMap<String, Integer>();
    (new PadCalculatorToo()).decidePadChanges(this, nodeID, padLimits, launchPads, landPads);   
    return (installPadChanges(launchPads, landPads));   
  }  
  
  /***************************************************************************
  **
  ** Answer if a display string is unique
  **
  */
  
  public abstract boolean rootDisplayNameIsUnique(String nodeID);
    
  /***************************************************************************
  **
  ** Set the genome image
  **
  */
  
  public ImageChange[] setGenomeImage(String imgKey) {   
    ImageManager mgr = appState_.getImageMgr();
    ArrayList<ImageChange> allChanges = new ArrayList<ImageChange>();
    if (imgKey_ != null) {
      ImageChange dropChange = mgr.dropImageUsage(imgKey_);
      dropChange.genomeKey = id_;
      allChanges.add(dropChange);
    } 

    if (imgKey != null) {
      ImageChange regChange = mgr.registerImageUsage(imgKey);
      regChange.genomeKey = id_;
      allChanges.add(regChange);
    }
    int changeCount = allChanges.size();
    if (changeCount == 0) {
      imgKey_ = imgKey;
      return (null);
    }
    ImageChange[] retval = new ImageChange[changeCount];
    allChanges.toArray(retval);
    imgKey_ = imgKey;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Gets text notes for the genome
  */
  
  public XPlatDisplayText getAllDisplayText() {
    XPlatDisplayText retval = new XPlatDisplayText();
    retval.setModelText(description_);
    
    ovrops_.getAllDisplayText(retval);
    
    Iterator<String> noit = notes_.keySet().iterator();
    while (noit.hasNext()) {
      String noID = noit.next();
      Note note = notes_.get(noID);
      String nDesc = note.getTextWithBreaksReplaced();     
      if ((nDesc != null) && !nDesc.trim().equals("")) { 
        retval.setNoteText(noID, nDesc);
      }    
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Write the genome using SBML
  **
  */
  
  public abstract void writeSBML(PrintWriter out, Indenter ind);
 
  /***************************************************************************
  **
  ** Write the genome as SIF:
  **
  */
  
  public abstract void writeSIF(PrintWriter out);
 
  /***************************************************************************
  **
  ** Write the genome to XML
  **
  */
  
  public abstract void writeXML(PrintWriter out, Indenter ind);
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
   
  /***************************************************************************
  **
  ** Write the genes to XML
  **
  */
  
  protected void writeGenes(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<" + nodeCollMap_.get(Node.GENE) + ">");
    TreeSet<Node> sorted = new TreeSet<Node>(new NodeComparator());
    sorted.addAll(genes_.values());
    Iterator<Node> genes = sorted.iterator();    

    ind.up();
    while (genes.hasNext()) {
      Node g = genes.next();
      g.writeXML(out, ind);
    }
    ind.down().indent(); 
    out.println("</" + nodeCollMap_.get(Node.GENE) + ">");
    return;
  } 
   
  /***************************************************************************
  **
  ** Write the nodes to XML
  **
  */
  
  protected void writeNodes(PrintWriter out, Indenter ind) {
    TreeSet<Node> sorted = new TreeSet<Node>(new NodeComparator());
    sorted.addAll(nodes_.values());
    Iterator<Node> nodes = sorted.iterator();
    ind.up();
    int currNodeType = -1;
    while (nodes.hasNext()) {
      Node n = nodes.next();
      int nodeType = n.getNodeType();
      if (nodeType != currNodeType) {
        if (currNodeType != -1) {
          ind.down().indent();
          out.println("</" + nodeCollMap_.get(Integer.valueOf(currNodeType)) + ">");
        }
        ind.indent();
        out.println("<" + nodeCollMap_.get(Integer.valueOf(nodeType)) + ">");
        currNodeType = nodeType;
        ind.up();        
      }
      n.writeXML(out, ind);
    }
    if (currNodeType != -1) {
      ind.down().indent();
      out.println("</" + nodeCollMap_.get(Integer.valueOf(currNodeType)) + ">");
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Write the notes to XML
  **
  */
  
  protected void writeNotes(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<notes>");
    Iterator<Note> notes = getNoteIterator();

    ind.up();
    while (notes.hasNext()) {
      Note n = notes.next();
      n.writeXML(out, ind, false);
    }
    ind.down().indent(); 
    out.println("</notes>");
    return;
  }   

  /***************************************************************************
  **
  ** Drop the genome image
  **
  */
  
  protected ImageChange dropGenomeImageSupport(String currImgKey) {
    ImageManager mgr = appState_.getImageMgr();
    if (currImgKey != null) {
      ImageChange dropChange = mgr.dropImageUsage(currImgKey);
      dropChange.genomeKey = id_;
      return (dropChange);
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Redo a note change
  */
  
  protected void noteChangeRedo(GenomeChange undo) {
    if ((undo.ntOrig != null) && (undo.ntNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      String id = undo.ntNew.getID();
      notes_.put(id, undo.ntNew);
    } else if (undo.ntOrig == null) {
      String id = undo.ntNew.getID();
      notes_.put(id, undo.ntNew);
      if (!labels_.addExistingLabel(id)) {
        System.err.println("Don't like " + id);
        throw new IllegalArgumentException();
      }
    } else {
      notes_.remove(undo.ntOrig.getID());
      labels_.removeLabel(undo.ntOrig.getID());
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Undo a note change
  */
  
  protected void noteChangeUndo(GenomeChange undo) {
    if ((undo.ntOrig != null) && (undo.ntNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      String id = undo.ntOrig.getID();
      notes_.put(id, undo.ntOrig);
    } else if (undo.ntOrig == null) {
      notes_.remove(undo.ntNew.getID());
      labels_.removeLabel(undo.ntNew.getID());
    } else {
      String id = undo.ntOrig.getID();
      notes_.put(id, undo.ntOrig);
      if (!labels_.addExistingLabel(id)) {
        System.err.println("Don't like " + id);
        throw new IllegalArgumentException();
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a properties change
  */
  
  protected void propChangeRedo(GenomeChange undo) {
    longName_ = undo.longNameNew;
    name_ = undo.nameNew;
    description_ = undo.descNew;
    return;
  }
    
  /***************************************************************************
  **
  ** Undo a properties change
  */
  
  protected void propChangeUndo(GenomeChange undo) {
    longName_ = undo.longNameOld;
    name_ = undo.nameOld;
    description_ = undo.descOld;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a type change
  */
  
  protected void typeChangeRedo(GenomeChange undo) {
    if ((undo.nOrig != null) && (undo.gNew != null)) {
      genes_.put(undo.gNew.getID(), undo.gNew);
      nodes_.remove(undo.nOrig.getID());      
    } else if ((undo.gOrig != null) && (undo.nNew != null)) {
      nodes_.put(undo.nNew.getID(), undo.nNew);
      genes_.remove(undo.gOrig.getID());
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a type change
  */
  
  protected void typeChangeUndo(GenomeChange undo) {
    if ((undo.nOrig != null) && (undo.gNew != null)) {
      nodes_.put(undo.nOrig.getID(), undo.nOrig);
      genes_.remove(undo.gNew.getID());
    } else if ((undo.gOrig != null) && (undo.nNew != null)) {
      genes_.put(undo.gOrig.getID(), undo.gOrig);
      nodes_.remove(undo.nNew.getID());      
    }
    return;
  }

  /***************************************************************************
  **
  ** Write the links to XML
  **
  */
  
  protected void writeLinks(PrintWriter out, Indenter ind, String elemTag) {
    ind.indent();
    out.println("<" + elemTag + ">");
    TreeSet<Linkage> sorted = new TreeSet<Linkage>(new LinkComparator());
    sorted.addAll(links_.values());
    Iterator<Linkage> links = sorted.iterator();

    ind.up();
    while (links.hasNext()) {
      Linkage lnk = links.next();
      lnk.writeXML(out, ind);
    }
    ind.down().indent(); 
    out.println("</" + elemTag + ">");
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Redo a gene change
  */
  
  private void geneChangeRedo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.put(undo.gNew.getID(), undo.gNew);
    } else if (undo.gOrig == null) {
      genes_.put(undo.gNew.getID(), undo.gNew);
      if (labelManager_) {
        labels_.addExistingLabel(undo.gNew.getID());
      }
    } else {
      genes_.remove(undo.gOrig.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.gOrig.getID());
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Undo a gene change
  */
  
  private void geneChangeUndo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.put(undo.gOrig.getID(), undo.gOrig);
    } else if (undo.gOrig == null) {
      genes_.remove(undo.gNew.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.gNew.getID());
      }
    } else {
      genes_.put(undo.gOrig.getID(), undo.gOrig);
      if (labelManager_) {
        labels_.addExistingLabel(undo.gOrig.getID());
      }
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Redo a link change
  */
  
  private void linkChangeRedo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((undo.lOrig != null) && (undo.lNew != null)) {
      links_.put(undo.lNew.getID(), undo.lNew);
    } else if (undo.lOrig == null) {
      links_.put(undo.lNew.getID(), undo.lNew);
      if (labelManager_) {
        labels_.addExistingLabel(undo.lNew.getID());
      }
    } else {
      links_.remove(undo.lOrig.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.lOrig.getID());
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Undo a link change
  */
  
  private void linkChangeUndo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.lOrig != null) && (undo.lNew != null)) {
      links_.put(undo.lOrig.getID(), undo.lOrig);
    } else if (undo.lOrig == null) {
      links_.remove(undo.lNew.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.lNew.getID());
      }
    } else {
      links_.put(undo.lOrig.getID(), undo.lOrig);
      if (labelManager_) {
        labels_.addExistingLabel(undo.lOrig.getID());
      }
    }
    return;
  }
  

  
  /***************************************************************************
  **
  ** Redo a node change
  */
  
  private void nodeChangeRedo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((undo.nOrig != null) && (undo.nNew != null)) {
      nodes_.put(undo.nNew.getID(), undo.nNew);
    } else if (undo.nOrig == null) {
      nodes_.put(undo.nNew.getID(), undo.nNew);
      if (labelManager_) {
        labels_.addExistingLabel(undo.nNew.getID());
      }
    } else {
      nodes_.remove(undo.nOrig.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.nOrig.getID());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a node change
  */
  
  private void nodeChangeUndo(GenomeChange undo) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.nOrig != null) && (undo.nNew != null)) {
      nodes_.put(undo.nOrig.getID(), undo.nOrig);
    } else if (undo.nOrig == null) {
      nodes_.remove(undo.nNew.getID());
      if (labelManager_) {
        labels_.removeLabel(undo.nNew.getID());
      }
    } else {
      nodes_.put(undo.nOrig.getID(), undo.nOrig);
      if (labelManager_) {
        labels_.addExistingLabel(undo.nOrig.getID());
      }
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Sorts links for output
  **
  */
   
  public static class LinkComparator implements Comparator<Linkage> {
    public int compare(Linkage link1, Linkage link2) {
      int srcCompare = (link1.getSource().compareTo(link2.getSource()));
      int trgCompare = (link1.getTarget().compareTo(link2.getTarget()));      
      if (srcCompare == 0) {
        if (trgCompare == 0) {
          return (link1.getID().compareTo(link2.getID()));
        } else {
          return (trgCompare);
        }
      } else {
        return (srcCompare); 
      }
    }
  }
   
  /***************************************************************************
  **
  ** Sorts nodes for output
  **
  */
   
  public static class NodeComparator implements Comparator<Node> {
    public int compare(Node node1, Node node2) {
      int type1 = node1.getNodeType();
      int type2 = node2.getNodeType();
      if (type1 == type2) {
        return (node1.getID().compareTo(node2.getID()));
      } else {
        return ((type1 > type2) ? 1 : -1); 
      }
    }
  } 
}