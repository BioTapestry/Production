/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.analysis.SignedTaggedLink;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This represents a root genome in BioTapestry
*/

public class DBGenome extends AbstractGenome implements Genome, Cloneable {
  
  private int uniqueNameSuffix_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic constructor
  */

  public DBGenome(DataAccessContext dacx, String name, String id) {
    super(dacx, name, id, NetworkOverlay.DB_GENOME, true);
    uniqueNameSuffix_ = 1;
  }

  /***************************************************************************
  **
  ** Copy constructor
  */

  public DBGenome(DBGenome other) {
    super(other, true);
  } 
  
  /***************************************************************************
  **
  ** Minimum copy constructor
  */

  private DBGenome(DBGenome other, boolean arg) {
    super(other, arg);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Drop everything BUT nodes, genes, and links.
  */
  
  public DBGenome getBasicGenomeCopy() {
    return (new DBGenome(this, false));
  }
  
  /***************************************************************************
  ** 
  ** Add a Key
  */

  public void addKey(String key) {
    if (!labels_.addExistingLabel(key)) {
      System.err.println("key is " + key);
      System.err.println("labels are " + labels_);
      throw new IllegalStateException();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a gene to the genome
  **
  */
  
  @Override
  public GenomeChange addGene(Gene gene) {
    String id = gene.getID();
    if (genes_.get(id) != null) {
      throw new IllegalArgumentException();
    }
    if (!labels_.addExistingLabel(id)) {
      throw new IllegalArgumentException();
    }
    return (super.addGene(gene));
  }
  
  /***************************************************************************
  **
  ** Add a miscellaneous gene to the genome
  **
  */
  
  public GenomeChange addGeneWithExistingLabel(Gene gene) {
    String id = gene.getID();
    if (genes_.get(id) != null) {
      System.err.println("got an existing gene");
      throw new IllegalArgumentException();
    }
    return (super.addGene(gene));
  }
  
  /***************************************************************************
  **
  ** Add a link to the genome
  **
  */
  
  @Override
  public GenomeChange addLinkage(Linkage link) {
    String id = link.getID();
    if (links_.get(id) != null) {
      throw new IllegalArgumentException();
    }
    if (!labels_.addExistingLabel(id)) {
      System.err.println("Don't like " + id);
      throw new IllegalArgumentException();
    }
    return (super.addLinkage(link));
  }
  
  /***************************************************************************
  **
  ** Add a miscellaneous link to the genome
  **
  */
  
  public GenomeChange addLinkWithExistingLabel(Linkage link) {
    String id = link.getID();
    if (links_.get(id) != null) {
      System.err.println("got an existing link");
      throw new IllegalArgumentException();
    }
    return (super.addLinkage(link));
  }
 
  /***************************************************************************
  **
  ** Add a miscellaneous node to the genome
  **
  */
  
  @Override
  public GenomeChange addNode(Node node) {
    String id = node.getID();
    if (nodes_.get(id) != null) {
      System.err.println("got an existing node");
      throw new IllegalArgumentException();
    }
    if (!labels_.addExistingLabel(id)) {
      System.err.println("got an existing label " + id);
      throw new IllegalArgumentException();
    }
    return (super.addNode(node));
  }
  
  /***************************************************************************
  **
  ** Add a miscellaneous node to the genome
  **
  */
  
  public GenomeChange addNodeWithExistingLabel(Node node) {
    String id = node.getID();
    if (nodes_.get(id) != null) {
      System.err.println("got an existing node");
      throw new IllegalArgumentException();
    }
    return (super.addNode(node));
  }
  
  /***************************************************************************
  **
  ** Answers if we can write SBML
  **
  */
  
  @Override
  public boolean canWriteSBML() {
    //
    // Currently, can't write SBML if we have bubbles, diamonds, 
    // slashes, or intercells.  Also can't write if text nodes have
    // inbounds links, or if there are multiple paths between two
    // nodes
    //

      // SR:  it appears that all non-gene nodes must be either
      // of type "box" or type "bare"
    Iterator<Node> nit = getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      //int type = node.getNodeType();

// SR:  treat all non-gene nodes the same, to separate biological semantics from
// the particular glyphs used in a diagram.
//      if ((type != Node.BARE) && (type != Node.BOX)) {
//	  System.out.println("canWriteSBML() returning false:  disallowed node type");
//        return (false);
//      }


      // count the number of input links for the node
      Iterator<Linkage> lit = getLinkageIterator();

      int inputCount = 0;
      while(lit.hasNext())
      {
          Linkage link = lit.next();
          String linkTarget = link.getTarget();
          Node node2 = getNode(linkTarget);
          if(node2.getID().equals(node.getID()))
          {
              ++inputCount;
          }
      }
      
      if(inputCount > 0)
      {
          // non-gene nodes with a function type of XOR and 1 or more inputs, cannot be converted to SBML
          if(((DBNode) node).getInternalLogic().getFunctionType() == DBInternalLogic.XOR_FUNCTION)
          {
              //System.out.println("canWriteSBML() returning false:  non-gene node with XOR function not implemented");
              return(false);
              }
          
      }
    }


    HashSet<LinkEnds> allLinkEnds = new HashSet<LinkEnds>();
    Iterator<Linkage> lit = getLinkageIterator();

    // iterate through all linkages in the diagram
    while (lit.hasNext()) {

	// get a specific link in the diagram
      Linkage link = lit.next();

      String trg = link.getTarget();
      Node node = getNode(trg);
      // link target must be a gene
      int targetNodeType = node.getNodeType();

      if(targetNodeType != Node.GENE)
      {
          int linkageType = link.getSign();
          if(linkageType == Linkage.NONE)
          {
              // any link to a non-gene node, must have a well-defined link sign
              // (positive or negative)
              //System.out.println("canWriteSBML() returning false:  undefined link sign terminating on a non-gene node");
              return(false);
          }
      }


      LinkEnds le = new LinkEnds(link.getSource(), trg);
      if (allLinkEnds.contains(le)) {
        return (false);
      }
      allLinkEnds.add(le);
    }    
    return (true);
  }   
  
  /***************************************************************************
  **
  ** Change the gene evidence level
  **
  */
  
  @Override
  public GenomeChange changeGeneEvidence(String geneID, int evidence) {
    GenomeChange retval = new GenomeChange();
    retval.gOrig = genes_.get(geneID).clone();
    retval.gNew = retval.gOrig.clone();
    ((DBGene)retval.gNew).setEvidenceLevel(evidence);
    genes_.put(geneID, retval.gNew);    
    retval.genomeKey = getID();    
    return (retval);      

  }  
 
  /***************************************************************************
  **
  ** Change the gene Name
  **
  */
  
  @Override
  public GenomeChange changeGeneName(String geneID, String name) { 
    GenomeChange retval = new GenomeChange();
    retval.gOrig = genes_.get(geneID).clone();
    retval.gNew = retval.gOrig.clone();
    retval.gNew.setName(name);
    genes_.put(geneID, retval.gNew);    
    retval.genomeKey = getID();    
    return (retval);  
  }

  /***************************************************************************
  **
  ** Change the gene regions
  **
  */
  
  @Override
  public GenomeChange changeGeneRegions(String geneID, List<DBGeneRegion> newRegions) {
    GenomeChange retval = new GenomeChange();
    retval.gOrig = genes_.get(geneID).clone();
    retval.gNew = retval.gOrig.clone();
    ((DBGene)retval.gNew).setRegions(newRegions);
    genes_.put(geneID, retval.gNew);
    retval.genomeKey = getID();    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Change the gene size
  **
  */
  
  @Override
  public GenomeChange changeGeneSize(String geneID, int pads) {
    GenomeChange retval = new GenomeChange();
    retval.gOrig = genes_.get(geneID).clone();
    retval.gNew = retval.gOrig.clone();
    ((DBGene)retval.gNew).setPadCount(pads);
    genes_.put(geneID, retval.gNew);    
    retval.genomeKey = getID();    
    return (retval);      
  }

  /***************************************************************************
  **
  ** Change the node Name
  **
  */
  
  @Override
  public GenomeChange changeNodeName(String nodeID, String name) { 
    GenomeChange retval = new GenomeChange();
    retval.nOrig = nodes_.get(nodeID).clone();
    retval.nNew = retval.nOrig.clone();
    retval.nNew.setName(name);
    nodes_.put(nodeID, retval.nNew);    
    retval.genomeKey = getID();    
    return (retval);  
  }
  
 /***************************************************************************
  **
  ** Change the node size
  **
  */
  
  @Override
  public GenomeChange changeNodeSize(String nodeID, int pads) {
    GenomeChange retval = new GenomeChange();
    retval.nOrig = nodes_.get(nodeID).clone();
    retval.nNew = retval.nOrig.clone();
    
    int padInc = DBNode.getPadIncrement(retval.nOrig.getNodeType());
    if (padInc != 0) {     
      int currVal = DBNode.getDefaultPadCount(retval.nOrig.getNodeType());
      while (currVal < pads) {
        currVal += padInc;
      }
      pads = currVal;
    }  
    
    ((DBNode)retval.nNew).setPadCount(pads);
    nodes_.put(nodeID, retval.nNew);    
    retval.genomeKey = getID();    
    return (retval);      
  }
  
  /***************************************************************************
  **
  ** Change the node type
  **
  */

  @Override
  public GenomeChange changeNodeType(String nodeID, int type) {
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();
    
    //
    // For nodes that support extra pads, we do not limit the pad count (though the
    // UI has a limited selection, auto layout can do what it needs).  So we
    // only toss out extra pads if the node does not support them (e.g. slash nodes)
    //
  
    Node existing = getNode(nodeID);
    int oldType = existing.getNodeType();
    int newPads = DBNode.mapPadCount(type, existing.getPadCount());    
      
    if (type == Node.GENE) {
      if (oldType == Node.GENE) { // gene->gene A NO-OP, but implement anyway
        retval.gOrig = new DBGene((DBGene)existing);
        retval.gNew = new DBGene((DBGene)existing);
        ((DBNode)retval.gNew).setPadCount(newPads);
        genes_.put(nodeID, new DBGene((DBGene)retval.gNew));          
      } else { // node - > gene
        retval.nOrig = new DBNode((DBNode)existing);
        retval.gNew = new DBGene((DBNode)existing);
        ((DBNode)retval.gNew).setPadCount(newPads);
        nodes_.remove(nodeID);
        genes_.put(nodeID, new DBGene((DBGene)retval.gNew));        
      }
    } else {
      if (oldType == Node.GENE) {  // gene -> node
        retval.gOrig = new DBGene((DBGene)existing);
        retval.nNew = new DBNode((DBNode)existing); // Note we lose gene subregion data with this constructor!
        retval.nNew.setNodeType(type);
        ((DBNode)retval.nNew).setPadCount(newPads);
        genes_.remove(nodeID);
        nodes_.put(nodeID, new DBNode((DBNode)retval.nNew));        
      } else {  // node -> node (probably different type)
        retval.nOrig = new DBNode((DBNode)existing);
        retval.nNew = new DBNode((DBNode)existing);
        retval.nNew.setNodeType(type);
        ((DBNode)retval.nNew).setPadCount(newPads);
        nodes_.put(nodeID, new DBNode((DBNode)retval.nNew));        
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Redo a change
  */

  @Override
  public void changeRedo(GenomeChange undo) {
    if (changeRedoSupport(undo)) {
      return;
    } 
    if ((undo.descOld != null) || (undo.descNew != null) ||
               (undo.nameOld != null) || (undo.nameNew != null) ||
               (undo.longNameOld != null) || (undo.longNameNew != null)) {
      propChangeRedo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  @Override  
  public void changeUndo(GenomeChange undo) {
    if (changeUndoSupport(undo)) {
      return;
    } 
    if ((undo.descOld != null) || (undo.descNew != null) ||
               (undo.nameOld != null) || (undo.nameNew != null) ||
               (undo.longNameOld != null) || (undo.longNameNew != null)) {
      propChangeUndo(undo);
    }
    return;
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public DBGenome clone() {
    DBGenome retval = (DBGenome)super.clone();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public GenomeChange finishNodeUndoTransaction(String nodeName, GenomeChange change) {
    Node node = getNode(nodeName);
    if (node.getNodeType() == Node.GENE) {
      change.gNew = (Gene)node.clone();
    } else {
      change.nNew = node.clone();     
    }
    return (change);
  }  

  /***************************************************************************
  **
  ** Come up with unique names.  If everybody is already unique, we return
  ** null.
  */
  
  public Map<String, String> genUnique() {    
    Set<String> dups = nameDups();
    if (dups.isEmpty()) {
      return (null);
    }
    String suffix = uniqueSuffix();
    HashMap<String, String> unique = new HashMap<String, String>();
    StringBuffer buf = new StringBuffer();
    
    Iterator<Node> git = getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String normName = DataUtil.normKey(node.getName());
      String genName;
      if (!dups.contains(normName)) {
        genName = node.getName();
      } else {
        buf.setLength(0);
        buf.append(node.getName());
        buf.append(suffix);
        buf.append(node.getID());
        buf.append("}");
        genName = buf.toString();
      }
      unique.put(node.getID(), genName);
    }
    return (unique);
  }
  
  /***************************************************************************
  **
  ** Get the gene with given (case-insensitive), space insensitive, display name
  **
  */
  
  public DBGene getGeneWithName(String geneName) {
    if ((geneName == null) || geneName.trim().equals("")) {
      return (null);
    }
    Iterator<Gene> git = getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String testName = gene.getName();
      if ((testName == null) || testName.trim().equals("")) {
        throw new IllegalStateException();
      }
      if (DataUtil.keysEqual(testName, geneName)) {
        return ((DBGene)gene);
      }
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Needed by NetOverlayOwner interface
  **
  */
  
  public Set<String> getGroupsForOverlayRendering() {
    return (null);
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
  ** Get the nodes with the given (case-insensitive) display name
  **
  */
  
  public Set<Node> getNodesWithName(String nodeName) {
    HashSet<Node> retval = new HashSet<Node>();    
    Iterator<Node> nit = getNodeIterator();
    while (nit.hasNext()) {
      DBNode node = (DBNode)nit.next();
      String testName = node.getName();
      if (testName == null) {
        if (nodeName == null) {
          retval.add(node);
        }
      } else {
        if ((nodeName != null) && DataUtil.keysEqual(testName, nodeName)) {
          retval.add(node);
        }
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get node targets
  **
  */
  
  public List<String> getNodeTargetNames(String nodeID, boolean toUpper) {
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<Linkage> lit = links_.values().iterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getSource().equals(nodeID)) {
        String targ = lnk.getTarget();
        Node node = getNode(targ);
        String name = node.getName();
        retval.add((toUpper) ? name.toUpperCase() : name);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop all nodes, genes, and links.  Keep everything else.
  */
  
  @Override
  public Genome getStrippedGenomeCopy() {
    DBGenome retval = new DBGenome(this.dacx_, this.name_, this.id_);
    retval.notes_ = this.notes_;  // Note this is shared, not copied!
    retval.longName_ = this.longName_;
    retval.description_ = this.description_;
    retval.labels_ = new UniqueLabeller(this.labels_);
    retval.uniqueNameSuffix_ = this.uniqueNameSuffix_;
    retval.imgKey_ = this.imgKey_;
    //
    // We need to retain all net overlays and modules and links, but we need to
    // ditch members until they have been (maybe) rebuilt.
    //
    retval.ovrops_ = ovrops_.getStrippedOverlayCopy();
    return (retval);
  }
  

  
  /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getUniqueGeneName() {
    String format = dacx_.getRMan().getString("gene.defaultNameFormat");
    while (true) {
      Integer suffix = new Integer(uniqueNameSuffix_++);
      String tryName = MessageFormat.format(format, new Object[] {suffix});
      if (format.equals(tryName)) { // Avoid infinite loops with punk resource file
        throw new IllegalStateException();
      }
      if (!nameInUse(tryName)) {
        return (tryName);
      }
    }
  }

  /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getUniqueNodeName() {
    String format = dacx_.getRMan().getString("node.defaultNameFormat");
    while (true) {
      Integer suffix = new Integer(uniqueNameSuffix_++);
      String tryName = MessageFormat.format(format, new Object[] {suffix});
      if (format.equals(tryName)) { // Avoid infinite loops with punk resource file
        throw new IllegalStateException();
      }
      if (!nameInUse(tryName)) {
        return (tryName);
      }
    }    
  }
  
  /***************************************************************************
  ** 
  ** Answer if the gene name / node name is already in use
  */

  public boolean nameInUse(String name) {
    if (name == null) {
      return (true);  // Don't allow null names
    }
    if (name.trim().equals("")) {
      return (true);
    }
    Iterator<Gene> genes = getGeneIterator();
    while (genes.hasNext()) {
      DBGene g = (DBGene)genes.next();
      if (g.getName().equals(name)) {
        return (true);
      }
    }
    Iterator<Node> nodes = getNodeIterator();
    while (nodes.hasNext()) {
      DBNode n = (DBNode)nodes.next();
      if (n.getName().equals(name)) {
        return (true);
      }
    }
    return (false);
  }    
    
  /***************************************************************************
  **
  ** Answer if a node has any inputs
  **
  */
  
  public boolean nodeHasInputs(String nodeID) {
    Iterator<Linkage> lit = links_.values().iterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getTarget().equals(nodeID)) {
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Recover network overlay module members
  */  
  
  public void recoverMappedModuleMembers(Genome oldGenome, Map<String, String> oldNodeToNew) {
    ovrops_.recoverMappedModuleMembers(oldGenome, oldNodeToNew, null);
    return;
   }  
  
  /***************************************************************************
  ** 
  ** Remove a key
  */

  public void removeKey(String key) {
    labels_.removeLabel(key);
    return;
  }
 
  /***************************************************************************
  **
  ** Remove the link from the genome
  **
  */
  
  @Override
  public GenomeChange removeLinkage(String key) {
    GenomeChange retval = new GenomeChange();
    retval.lOrig = links_.get(key);
    links_.remove(key);
    retval.lNew = null;
    labels_.removeLabel(key);
    retval.genomeKey = getID();        
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace linkage properties in the genome
  **
  */
  
  public GenomeChange replaceLinkageProperties(String linkID, String name, 
                                               int sign, int targetLevel) {
    GenomeChange retval = new GenomeChange();
    Linkage link = links_.get(linkID);
    retval.lOrig = link.clone();
    retval.lNew = link.clone();
    retval.lNew.setName(((name == null) || name.trim().equals("")) ? null : name.trim());
    retval.lNew.setSign(sign);
    retval.lNew.setTargetLevel(targetLevel);
    links_.put(linkID, retval.lNew.clone());
    retval.genomeKey = getID();
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get required parameters
  */
  
  public Set<String> requiredGeneParameters(Gene gene) {  
    SbmlSupport sbmls = new SbmlSupport(dacx_);
    return (sbmls.requiredGeneParameters(gene, this));
  } 
  
  /***************************************************************************
  **
  ** Get required parameters
  */
  
  public Set<String> requiredNonGeneParameters(Node node) {  
    SbmlSupport sbmls = new SbmlSupport(dacx_);
    return (sbmls.requiredNonGeneParameters(node, this));
  }
  
  /***************************************************************************
  **
  ** Answer if a display string is unique across both nodes and genes
  **
  */
  
  @Override
  public boolean rootDisplayNameIsUnique(String nodeID) {
    nodeID = GenomeItemInstance.getBaseID(nodeID);
    Node node = getGene(nodeID);
    if (node == null) {
      node = getNode(nodeID);
    }
    String display = node.getName();
    if ((display == null) || (display.trim().equals(""))) {
      return (false);
    }
    return (uniqueCheck(getAllNodeIterator(), node, display));
  }
  
  /***************************************************************************
  **
  ** Set the id.  DANGER! Use this only in very special cases.
  **
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  }

  /***************************************************************************
  **
  ** Change the model properties
  **
  */
  
  public GenomeChange setProperties(String name, String longName, String desc) {
    GenomeChange retval = new GenomeChange();
    retval.genomeKey = getID();
    
    retval.descOld = description_;
    retval.longNameOld = longName_;
    retval.nameOld = name_;
        
    longName_ = longName;
    name_ = name;
    description_ = desc;
    
    retval.descNew = description_;
    retval.longNameNew = longName_;
    retval.nameNew = name_;    
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public GenomeChange startNodeUndoTransaction(String nodeName) {
    GenomeChange retval = new GenomeChange();
    Node node = getNode(nodeName);
    if (node.getNodeType() == Node.GENE) {
      retval.gOrig = (Gene)node.clone();
    } else {
      retval.nOrig = node.clone();     
    }
    retval.genomeKey = getID();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Write the genome using SBML
  **
  */
  
  public void writeSBML(PrintWriter out, Indenter ind) {
    SbmlSupport sbmls = new SbmlSupport(dacx_);
    StringBuffer buf = new StringBuffer();
    Indenter bufIndent = new Indenter(buf, ind.getIndent());
    bufIndent.setCurrLevel(ind.getCurrLevel());
    sbmls.writeSBML(buf, bufIndent, this);
    ind.setCurrLevel(bufIndent.getCurrLevel());
    out.print(buf.toString());
    return;
  }  
  
  /***************************************************************************
  **
  ** Write the genome using SBML
  */
  
  public void writeSBML(StringBuffer buf, Indenter ind) {
    SbmlSupport sbmls = new SbmlSupport(dacx_);
    sbmls.writeSBML(buf, ind, this);
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
  ** Write the genome to XML
  **
  */
  
  @Override
  public void writeXML(PrintWriter out, Indenter ind) {
    nodeCollMap_ = buildNodeCollectionMap();
    ind.indent();
    out.print("<genome ");
    out.print("name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    // Added as part of 7.0.1 (Issue #240): change from id -> gid. This will 
    // cause versions older than 7.0.1 to refuse to read 7.0.1+ files.
    out.print("\" gid=\"");
    out.print(id_);
    if (imgKey_ != null) {
      out.print("\" image=\"");
      out.print(imgKey_); 
    }
    out.println("\" >");
    ind.up().indent();
    if (longName_ != null) {
      out.print("<rootLongName>");
      out.print(CharacterEntityMapper.mapEntities(longName_, false));
      out.println("</rootLongName>");
    } else {
      out.println("<rootLongName />");      
    }
    ind.indent();
    if (description_ != null) {
      out.print("<rootDescription>");
      out.print(CharacterEntityMapper.mapEntities(description_, false));
      out.println("</rootDescription>");
    } else {
      out.println("<rootDescription />");      
    }        
    ind.indent();
    out.println("<nodes>");
    writeNodes(out, ind);
    writeGenes(out, ind);
    ind.down().indent();
    out.println("</nodes>");
    writeLinks(out, ind, "links");
    ovrops_.writeOverlaysToXML(out, ind);
    writeNotes(out, ind);    
    ind.down().indent();
    out.println("</genome>");
    return;
  }  

  /***************************************************************************
  **
  ** Analyzes DBGenome for duplicated links between same source and target, same sign, same evidence:
  **
  */
  
  public Set<String> mergeableLinks(String linkID) {
    HashSet<String> retval = new HashSet<String>();
    Linkage link = getLinkage(linkID);
    String targID = link.getTarget();
    Node targNode = getNode(targID);
    
    //
    // Links into genes with defined regions can only be merged if
    // they are in the same region, or both in holders.
    //
    
    Gene gene = null;
    DBGeneRegion targRegLink = null;
    if (targNode.getNodeType() == Node.GENE) {
      gene = (Gene)targNode;
      if (gene.getNumRegions() > 0) {
        targRegLink = gene.getRegionForPad(link.getLandingPad());
      }
    }
    
    //
    // We want EVIDENCE LEVELS to be kept distinct, so we use the tagged version:
    //
    
    SignedTaggedLink key = new SignedTaggedLink(link.getSource(), targID, link.getSign(), Integer.toString(link.getTargetLevel()));
    Iterator<Linkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      Linkage nlink = lit.next();
      String nID = nlink.getID();
      if (linkID.equals(nID)) {
        continue;
      }
      SignedTaggedLink nkey = new SignedTaggedLink(nlink.getSource(), nlink.getTarget(), nlink.getSign(), Integer.toString(nlink.getTargetLevel()));
      if (!nkey.equals(key)) {
        continue;
      }
      if (targRegLink != null) {
        DBGeneRegion targRegCheck = gene.getRegionForPad(nlink.getLandingPad());
        if (targRegLink.isHolder() && targRegCheck.isHolder()) {
          retval.add(nID);
        } else if (targRegLink.getKey().equals(targRegCheck.getKey())) {
          retval.add(nID);
        }
      } else {
        retval.add(nID);
      }
    }
    return (retval);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Map node types to element names
  **
  */
  
  private HashMap<Integer, String> buildNodeCollectionMap() {
    HashMap<Integer, String> retval = new HashMap<Integer, String>();
    retval.put(new Integer(Node.BARE), "bares");
    retval.put(new Integer(Node.BOX), "boxes");   
    retval.put(new Integer(Node.BUBBLE), "bubbles");   
    retval.put(new Integer(Node.GENE), "genes");   
    retval.put(new Integer(Node.INTERCELL), "intercels");   
    retval.put(new Integer(Node.SLASH), "slashes");
    retval.put(new Integer(Node.DIAMOND), "diamonds");
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Come up with duplicated names
  */
  
  private Set<String> nameDups() {
    HashSet<String> seen = new HashSet<String>();
    HashSet<String> dups = new HashSet<String>();
    
    Iterator<Node> git = getAllNodeIterator();    
    while (git.hasNext()) {
      Node node = git.next();
      String normName = DataUtil.normKey(node.getName());
      if (seen.contains(normName)) {
        dups.add(normName); 
      }
      seen.add(normName);
    }    
    return (dups);
  }
  
  /***************************************************************************
  **
  ** Helper
  **
  */
  
  private boolean uniqueCheck(Iterator<Node> nodes, Node node, String display) {
    display = display.toUpperCase().replaceAll(" ", "");
    while (nodes.hasNext()) {
      Node testNode = nodes.next();    
      if (testNode == node) {
        continue;
      }
      String testName = testNode.getName();
      if ((testName == null) || (testName.trim().equals(""))) {
        continue;
      }      
      if (testName.toUpperCase().replaceAll(" ", "").equals(display)) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Come up with unique ID suffix
  */
  
  private String uniqueSuffix() {
    StringBuffer buf = new StringBuffer();
    buf.append("-{BIO_TAP_ID=");
    while (true) {
      String retval = buf.toString();
      Iterator<Node> git = getAllNodeIterator();
      boolean noMatch = true;
      while (git.hasNext()) {
        Node node = git.next();
        String normName = DataUtil.normKey(node.getName());
        if (normName.indexOf(retval) != -1) {
          buf.append("=");
          noMatch = false;
          break;
        }
      }
      if (noMatch) {
        return (retval);
      }
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Build from XML
  */
  
  public static DBGenome buildFromXML(DataAccessContext dacx, String elemName, Attributes attrs) 
                                      throws IOException {
    if (!elemName.equals("genome")) {
      return (null);
    }
    String name = null;
    String id = null;
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
          // Added as part of 7.0.1 (Issue #240): change from id -> gid. This will 
          // cause versions older than 7.0.1 to refuse to read 7.0.1+ files.
        } else if (key.equals("gid") || key.equals("id")) {
          id = val;
        } else if (key.equals("image")) {
          imgKey = val;          
        }
      }
    }
    if ((name == null) || (id == null)) {
      throw new IOException();
    }
    
    DBGenome retval = new DBGenome(dacx, name, id);
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
    return ("rootDescription");
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("genome");
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Return the long name keyword
  **
  */
  
  public static String longNameKeyword() {
    return ("rootLongName");
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to detect duplicate paths
  */
  
  class LinkEnds {
  
    public String src;
    public String trg;
    
    public LinkEnds(String src, String trg) {
      this.src = src;
      this.trg = trg;
    }  

    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof LinkEnds)) {
        return (false);
      }

      LinkEnds otherLE = (LinkEnds)other;
      if (!this.src.equals(otherLE.src)) {
        return (false);
      }
      return (this.trg.equals(otherLE.trg));
    } 
    
    public int hashCode() {
      return (src.hashCode() + trg.hashCode());
    } 

    public String toString() {
      return ("LinkEnds: " + src + " -> " + trg);
    }
  } 
}
