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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.PrintWriter;
import java.util.Map;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;  // FIX ME

/****************************************************************************
**
** This represents a genome in BioTapestry
*/

public interface Genome extends NetOverlayOwner {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Drop all nodes, genes, and links.  Keep everything else.
  */
  
  public Genome getStrippedGenomeCopy();  
  
  /***************************************************************************
  **
  ** Answer if it is empty
  */
  
  public boolean isEmpty();
   
  /***************************************************************************
  **
  ** Get the name
  **
  */
  
  public String getName();
  
  
  /***************************************************************************
  **
  ** Get the id
  **
  */
  
  public String getID();
  
  /***************************************************************************
  **
  ** Add a gene to the genome
  **
  */
  
  public GenomeChange addGene(Gene gene);
  
  /***************************************************************************
  **
  ** Get an Iterator over the genes:
  **
  */
  
  public Iterator<Gene> getGeneIterator();

  /***************************************************************************
  **
  ** Get a particular gene
  **
  */
  
  public Gene getGene(String key);
  
  /***************************************************************************
  **
  ** Get gene count:
  **
  */
  
  public int getGeneCount();
  
  /***************************************************************************
  **
  ** Get full (gene + other nodes) count:
  **
  */
  
  public int getFullNodeCount();
  
  /***************************************************************************
  **
  ** Get linkage count:
  **
  */
  
  public int getLinkageCount();
  
  /***************************************************************************
  **
  ** Add a miscellaneous node to the genome
  **
  */
  
  public GenomeChange addNode(Node node);
  
  /***************************************************************************
  **
  ** Replace a miscellaneous node in the genome
  **
  */
  
  public GenomeChange replaceNode(Node node);
  
  /***************************************************************************
  **
  ** Replace a miscellaneous node in the genome
  **
  */
  
  public GenomeChange replaceGene(Gene gene);
  
  /***************************************************************************
  **
  ** Change the node type
  **
  */
  
  public GenomeChange changeNodeType(String nodeID, int type);
  
  /***************************************************************************
  **
  ** Fix pad changes
  */
  
  public GenomeChange[] resolvePadChanges(String nodeID, NodeProperties.PadLimits padLimits);
  
  /***************************************************************************
  **
  ** Fix pad changes
  */
  
  public GenomeChange[] installPadChanges(Map<String, Integer> launchPads, Map<String, Integer> landingPads);  
  
  /***************************************************************************
  **
  ** Change the node name
  **
  */
  
  public GenomeChange changeNodeName(String nodeID, String name); 
  
 /***************************************************************************
  **
  ** Change the gene name
  **
  */
  
  public GenomeChange changeGeneName(String nodeID, String name);  
  
 /***************************************************************************
  **
  ** Change the gene evidence level
  **
  */
  
  public GenomeChange changeGeneEvidence(String nodeID, int evidence);  

  /***************************************************************************
  **
  ** Change the gene size
  **
  */
  
  public GenomeChange changeGeneSize(String geneID, int pads); 
  
  /***************************************************************************
  **
  ** Change the node size
  **
  */
  
  public GenomeChange changeNodeSize(String nodeID, int pads);   
  

  /***************************************************************************
  **
  ** Change the node URLs
  **
  */
  
  public GenomeChange changeNodeURLs(String nodeID, List<String> urls);     
  
  
  /***************************************************************************
  **
  ** Change the node description
  **
  */
  
  public GenomeChange changeNodeDescription(String nodeID, String desc);   
  
  /***************************************************************************
  **
  ** Change the gene regions
  **
  */
  
  public GenomeChange changeGeneRegions(String geneID, List<DBGeneRegion> newRegions);  
  
  /***************************************************************************
  **
  ** Get an Iterator over the miscellaneous nodes:
  **
  */
  
  public Iterator<Node> getNodeIterator();
  
  /***************************************************************************
  **
  ** Get an Iterator over the all the nodes (genes and other nodes):
  **
  */
  
  public Iterator<Node> getAllNodeIterator();  

  /***************************************************************************
  **
  ** Get a particular node, including genes:
  **
  */
  
  public Node getNode(String key);
  
  /***************************************************************************
  **
  ** Remove a particular node, including genes:
  **
  */
  
  public GenomeChange removeNode(String key);

  /***************************************************************************
  **
  ** Add a link to the genome
  **
  */
  
  public GenomeChange addLinkage(Linkage link);
  
  /***************************************************************************
  **
  ** Remove the link from the genome
  **
  */
  
  public GenomeChange removeLinkage(String key);
    
  /***************************************************************************
  **
  ** Replace a miscellaneous link in the genome
  **
  */
  
  public GenomeChange replaceLinkageProperties(String linkID, String name, 
                                               int sign, int targetLevel);  
   
  /***************************************************************************
  **
  ** Get an Iterator over the links:
  **
  */
  
  public Iterator<Linkage> getLinkageIterator();
  
  /***************************************************************************
  **
  ** Get a particular link
  **
  */
  
  public Linkage getLinkage(String key);
  
  /***************************************************************************
  **
  ** Change the linkage target
  **
  */
  
  public GenomeChange changeLinkageTarget(Linkage link, int targNum);
 
  /***************************************************************************
  **
  ** Change the linkage source
  **
  */
  
  public GenomeChange changeLinkageSource(Linkage link, int targNum);
  
  /***************************************************************************
  **
  ** Change the linkage source node
  **
  */
  
  public GenomeChange changeLinkageSourceNode(Linkage link, String srcID, int targNum);  

  /***************************************************************************
  **
  ** Change the linkage target node
  **
  */
 
  public GenomeChange changeLinkageTargetNode(Linkage link, String trgID, int targNum); 
  
  /***************************************************************************
  **
  ** Change the link URLs
  **
  */
  
  public GenomeChange changeLinkageURLs(String linkID, List<String> urls);     
  
  
  /***************************************************************************
  **
  ** Change the link description
  **
  */
  
  public GenomeChange changeLinkageDescription(String linkID, String desc);   
  
  
  /***************************************************************************
  **
  ** Set the genome image
  **
  */
  
  public ImageChange[] setGenomeImage(String imgKey);  
  
  /***************************************************************************
  **
  ** Drop the genome image
  **
  */
  
  public ImageChange dropGenomeImage();    

  /***************************************************************************
  **
  ** Get the genome image key
  **
  */
  
  public String getGenomeImage();      
  
  /***************************************************************************
  **
  ** Get the long display name
  **
  */
  
  public String getLongName();
  
  /***************************************************************************
  **
  ** Get the description
  **
  */
  
  public String getDescription();
  
  /***************************************************************************
  **
  ** Answers with all links having target pad collisions
  **
  */
  
  public Set<String> hasLinkTargetPadCollisions(); 
  
  /***************************************************************************
  **
  ** Answers with the minimum number of (launch, landing) pads needed by this
  ** genome for the given node.
  **
  */
  
  public PadCalculatorToo.PadResult getNodePadRequirements(Node whichNode, Layout lo);

 /***************************************************************************
  **
  ** Answer if a root display string is unique
  **
  */
  
  public boolean rootDisplayNameIsUnique(String nodeID);

 /***************************************************************************
  **
  ** Get node targets
  **
  */
  
  public Set<String> getNodeTargets(String nodeID);  
    
 /***************************************************************************
  **
  ** Get the IDs of the nodes that have links targeting the given node.
  **
  */
  
  public Set<String> getNodeSources(String nodeID);

  /***************************************************************************
  **
  ** Get the target count for the given node
  */
  
  public int getTargetCount(String nodeID);

  /***************************************************************************
  **
  ** Get the source count for the given node
  */
  
  public int getSourceCount(String nodeID); 
  
  /***************************************************************************
  ** 
  ** Get the number of inbound links to the node
  */

  public int getInboundLinkCount(String nodeID);
  
  /***************************************************************************
  ** 
  ** Get the number of outbound links from the node
  */

  public int getOutboundLinkCount(String nodeID);
  
  /***************************************************************************
  ** 
  ** Get the IDs of all outbound links from the node
  */

  public Set<String> getOutboundLinks(String nodeID);
  
  /***************************************************************************
  **
  ** Get the count of links between the source and target
  */  
 
  public int getLinkCount(String srcNodeID, String trgNodeID);
 
  /***************************************************************************
  **
  ** Get the single source pad for a node.  Null if the node is not a source.
  */  
 
  public Integer getSourcePad(String nodeID);
  
  /***************************************************************************
  **
  ** Write the genome using native XML format
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind); 
  
  /***************************************************************************
  **
  ** Write the genome as SIF:
  **
  */
  
  public void writeSIF(PrintWriter out);   
  
  /***************************************************************************
  **
  ** Answers if we can write SBML
  **
  */
  
  public boolean canWriteSBML();

  /***************************************************************************
  **
  ** Write out the genome using SBML
  **
  */  
 
  public void writeSBML(PrintWriter out, Indenter ind);
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GenomeChange undo);
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GenomeChange undo);
  
  
  /***************************************************************************
  **
  ** Undo an image change
  */
  
  public void imageChangeUndo(ImageChange undo);
  
  /***************************************************************************
  **
  ** Redo an image change
  */
  
  public void imageChangeRedo(ImageChange undo);  

  
  /***************************************************************************
  ** 
  ** Get the next note key, which must be unique across us and child VFNs
  */

  public String getNextNoteKey();
    
  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  public void addNote(Note note);
  
  /***************************************************************************
  **
  ** Add a note to the genome
  **
  */
  
  public GenomeChange addNoteWithExistingLabel(Note note);

  /***************************************************************************
  **
  ** Change the note in the genome
  **
  */
  
  public GenomeChange changeNote(Note note, String newLabel, String newText, boolean isInteractive);
  
  /***************************************************************************
  **
  ** Remove a particular note:
  **
  */
  
  public GenomeChange removeNote(String key);

  /***************************************************************************
  **
  ** Get an Iterator over the notes:
  **
  */
  
  public Iterator<Note> getNoteIterator();

  /***************************************************************************
  **
  ** Get a particular note
  **
  */
  
  public Note getNote(String key);
  
  /***************************************************************************
  **
  ** Get all the names up to the root
  **
  */
  
  public List<String> getNamesToRoot();
  
  /***************************************************************************
  **
  ** Gets text notes for the genome
  */
  
  public XPlatDisplayText getAllDisplayText();
   
}
