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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This holds Copies Per Embryo Data
*/

public class CopiesPerEmbryoData {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<CopiesPerEmbryoGene> genes_;
  private HashMap<String, List<String>> gpeMap_;
  private TreeSet<Integer> defaultTimes_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public CopiesPerEmbryoData(BTState appState) {
    appState_ = appState;
    genes_ = new ArrayList<CopiesPerEmbryoGene>();
    gpeMap_ = new HashMap<String, List<String>>();
    defaultTimes_ = new TreeSet<Integer>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answer if we have data
  */
  
  public boolean haveData() {
    if (!genes_.isEmpty()) {
      return (true);
    }
    if (!gpeMap_.isEmpty()) {
      return (true);
    }
    if (!defaultTimes_.isEmpty()) {
      return (true);
    }    
    return (false);
  }

  /***************************************************************************
  **
  ** Answers if we have a CPE mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = gpeMap_.get(nodeID);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if we have copy per embryo data for the given node
  **
  */
  
  public boolean haveDataForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getPerEmbryoCountDataKeysWithDefault(nodeID);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getCopyPerEmbryroData(name) != null) {
        return (true);
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answers if we have per embryo data for the given node
  **
  */
  
  public boolean haveDataForNodeOrName(String nodeID, String deadName) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getPerEmbryoCountDataKeysWithDefaultGivenName(nodeID, deadName);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getCopyPerEmbryroData(name) != null) {
        return (true);
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(CopiesPerEmbryoChange undo) {
    if ((undo.mapListOrig != null) || (undo.mapListNew != null)) {
      mapChangeUndo(undo);
    } else if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeUndo(undo);
    } 
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(CopiesPerEmbryoChange undo) {
    if ((undo.mapListOrig != null) || (undo.mapListNew != null)) {
      mapChangeRedo(undo);
    } else if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeRedo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if the data is empty
  */
  
  public boolean isEmpty() {
    return (genes_.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Add a gene
  */
  
  public CopiesPerEmbryoChange addGene(CopiesPerEmbryoGene gene) {
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    retval.genePos = genes_.size();      
    genes_.add(gene);
    retval.gNew = gene.clone();
    retval.gOrig = null;
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get an iterator over the genes
  */
  
  public Iterator<CopiesPerEmbryoGene> getGenes() {
    return (genes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get nth gene 
  */
  
  public CopiesPerEmbryoGene getGene(int n) {
    return (genes_.get(n));
  }

  /***************************************************************************
  **
  ** Drop maps that resolve to the given time course entry.
  */
  
  public CopiesPerEmbryoChange[] dropMapsTo(String nodeID) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    ArrayList<CopiesPerEmbryoChange> retvalList = new ArrayList<CopiesPerEmbryoChange>();
    Iterator<String> mit = new HashSet<String>(gpeMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<String> targList = gpeMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = targList.get(i);
        if (targ.equals(nodeID)) {
          CopiesPerEmbryoChange tcc = new CopiesPerEmbryoChange();
          tcc.mapKey = mapKey;
          tcc.mapListOrig = new ArrayList<String>(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tcc.mapListNew = null;
            gpeMap_.remove(mapKey);
          } else {
            tcc.mapListNew = new ArrayList<String>(targList);
          }
          retvalList.add(tcc);
          break;
        }
      }
    }
    return (retvalList.toArray(new CopiesPerEmbryoChange[retvalList.size()]));
  }    
 
  /***************************************************************************
  **
  ** Drop nth gene 
  */
  
  public CopiesPerEmbryoChange dropGene(int n) {
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    CopiesPerEmbryoGene gene = genes_.get(n);
    retval.gOrig = gene.clone();
    retval.gNew = null;
    retval.genePos = n;
    genes_.remove(n);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the gene 
  */
  
  public CopiesPerEmbryoChange dropGene(String nodeID) {
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      CopiesPerEmbryoGene gene = genes_.get(i);
      if (gene.getName().equals(nodeID)) {
        CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
        retval.gOrig = gene;
        retval.gNew = null;
        retval.genePos = i;
        genes_.remove(i);
        return (retval);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public CopiesPerEmbryoChange startGeneUndoTransaction(String geneName) {
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    Iterator<CopiesPerEmbryoGene> git = getGenes();
    int count = 0;
    while (git.hasNext()) {
      CopiesPerEmbryoGene tg = git.next();
      if (DataUtil.keysEqual(tg.getName(), geneName)) {
        retval.genePos = count;
        retval.gOrig = tg.clone();
        return (retval);
      }
      count++;
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  @SuppressWarnings("unused")
  public CopiesPerEmbryoChange finishGeneUndoTransaction(String geneName, CopiesPerEmbryoChange change) {
    CopiesPerEmbryoGene tg = genes_.get(change.genePos);
    change.gNew = tg.clone();
    return (change);
  }
  
  /***************************************************************************
  **
  ** Undo a gene change
  */
  
  private void geneChangeUndo(CopiesPerEmbryoChange undo) {
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.set(undo.genePos, undo.gOrig);
    } else if (undo.gOrig == null) {
      genes_.remove(undo.genePos);
    } else {
      genes_.add(undo.genePos, undo.gOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a gene change
  */
  
  private void geneChangeRedo(CopiesPerEmbryoChange undo) {
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.set(undo.genePos, undo.gNew);
    } else if (undo.gNew == null) {
      genes_.remove(undo.genePos);
    } else {
      genes_.add(undo.genePos, undo.gNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public CopiesPerEmbryoChange dropDataKeys(String geneId) {
    if (gpeMap_.get(geneId) == null) {
      return (null);
    }    
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    retval.mapKey = geneId;
    retval.mapListNew = null;
    retval.mapListOrig = gpeMap_.remove(geneId);
    return (retval);
  }

  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void mapChangeUndo(CopiesPerEmbryoChange undo) {
    if ((undo.mapListOrig != null) && (undo.mapListNew != null)) {
      gpeMap_.put(undo.mapKey, undo.mapListOrig);
    } else if (undo.mapListOrig == null) {
      gpeMap_.remove(undo.mapKey);
    } else {
      gpeMap_.put(undo.mapKey, undo.mapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void mapChangeRedo(CopiesPerEmbryoChange undo) {
    if ((undo.mapListOrig != null) && (undo.mapListNew != null)) {
      gpeMap_.put(undo.mapKey, undo.mapListNew);
    } else if (undo.mapListNew == null) {
      gpeMap_.remove(undo.mapKey);
    } else {
      gpeMap_.put(undo.mapKey, undo.mapListNew);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Return an iterator over the default times
  */
  
  public Iterator<Integer> getDefaultTimes() {
    return (defaultTimes_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Answer if we have default times
  */
  
  public boolean hasDefaultTimes() {
    return (!defaultTimes_.isEmpty());
  }   

  
  /***************************************************************************
  **
  ** Drop the default times
  */
  
  public CopiesPerEmbryoChange dropDefaultTimes() {
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    retval.oldDefaultTimes = new TreeSet<Integer>(defaultTimes_);
    defaultTimes_.clear();
    retval.newDefaultTimes = new TreeSet<Integer>(defaultTimes_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a default time
  */
  
  public CopiesPerEmbryoChange addDefaultTime(int time) {
    CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
    retval.oldDefaultTimes = new TreeSet<Integer>(defaultTimes_);
    defaultTimes_.add(new Integer(time));   
    retval.newDefaultTimes = new TreeSet<Integer>(defaultTimes_);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the copy per embryo data
  */
  
  public CopiesPerEmbryoGene getCopyPerEmbryroData(String targetName) {
    Iterator<CopiesPerEmbryoGene> trgit = genes_.iterator();
    while (trgit.hasNext()) {
      CopiesPerEmbryoGene trg = trgit.next();
      if (DataUtil.keysEqual(targetName, trg.getName())) {
        return (trg);
      }
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  public String getCountTable(String targetName, BTState appState) {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    
    Iterator<CopiesPerEmbryoGene> trgit = genes_.iterator();  // FIX ME: use a hash map
    while (trgit.hasNext()) {
      CopiesPerEmbryoGene trg = trgit.next();
      String name = trg.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        trg.getCountTable(out, appState);
      }
    }
    return (sw.toString());
  }
    
  /***************************************************************************
  **
  ** Add a map from a node to a List of target nodes
  */
  
  public CopiesPerEmbryoChange addCpeMap(String key, List<String> mapSets) {
    CopiesPerEmbryoChange retval = null;
    if ((mapSets != null) && (mapSets.size() > 0)) {
      retval = new CopiesPerEmbryoChange();
      retval.mapKey = key;
      retval.mapListNew = mapSets;
      retval.mapListOrig = null;
      gpeMap_.put(key, mapSets);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** When changing an entry name, we need to change one or more maps to keep
  ** them consistent.  Do this operation. Return null if no changes occurred.
  */
  
  public CopiesPerEmbryoChange[] changeCpeMapToName(String oldName, String newName) {  
    ArrayList<CopiesPerEmbryoChange> retvalList = new ArrayList<CopiesPerEmbryoChange>();
    Iterator<String> tmit = gpeMap_.keySet().iterator();
    while (tmit.hasNext()) {
      String mkey = tmit.next();
      List<String> keys = gpeMap_.get(mkey);
      if (keys.contains(oldName)) {
        CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
        retval.mapKey = mkey;
        retval.mapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.mapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }
    return (retvalList.toArray(new CopiesPerEmbryoChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** We are changing the name of a node or gene, and have been told to modify the
  ** data as needed.  We need to change the entry (if any) to the given name
  ** and any custom maps to that name.
  */
  
  public CopiesPerEmbryoChange[] changeName(String oldName, String newName) {
    ArrayList<CopiesPerEmbryoChange> retvalList = new ArrayList<CopiesPerEmbryoChange>();

    if (getCopyPerEmbryroData(newName) != null) {
      throw new IllegalArgumentException();
    }
    
    //
    // Do the entry:
    //
    
    CopiesPerEmbryoGene gene = getCopyPerEmbryroData(oldName); 
    if (gene != null) {
      CopiesPerEmbryoChange tcc = startGeneUndoTransaction(oldName);
      gene.setName(newName);
      tcc = finishGeneUndoTransaction(newName, tcc);
      retvalList.add(tcc);
    }
    
    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = gpeMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<String> keys = gpeMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        CopiesPerEmbryoChange retval = new CopiesPerEmbryoChange();
        retval.mapKey = mkey;
        retval.mapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.mapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }

    return (retvalList.toArray(new CopiesPerEmbryoChange[retvalList.size()]));
  }

  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be empty.
  */
  
  public List<String> getPerEmbryoCountDataKeysWithDefault(String nodeId) {
    List<String> retval = gpeMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      Node node = appState_.getDB().getGenome().getNode(nodeId);      
      if (node == null) { // for when node has been already deleted...
        throw new IllegalStateException();
      }      
      String nodeName = node.getRootName();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be empty.
  */
  
  public List<String> getPerEmbryoCountDataKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<String> retval = gpeMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be null
  */
  
  public List<String> getCustomPerEmbryoCountDataKeys(String nodeId) {
    return (gpeMap_.get(nodeId));
  }
  
  /***************************************************************************
  **
  ** Get the set of nodeIDs that target the given name. May be empty, not null.
  */
  
  public Set<String> getPerEmbryoCountDataKeyInverses(String name) {
    name = name.toUpperCase().replaceAll(" ", "");
    HashSet<String> retval = new HashSet<String>();
    //
    // If there is anybody out there with the same name and no custom map, it
    // will map by default:
    //
    
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    Node node = genome.getGeneWithName(name);
    if (node != null) {
      if (!haveCustomMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(name);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = gpeMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> targs = gpeMap_.get(key);
      Iterator<String> trit = targs.iterator();
      while (trit.hasNext()) {
        String testName = trit.next();
        if (testName.replaceAll(" ", "").equalsIgnoreCase(name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }  
  
  /**
   * 
   * Write the CopiesPerEmbryoData to XML
   */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<CopiesPerEmbryoData>");
    if (genes_.size() > 0) {
      Iterator<CopiesPerEmbryoGene> git = getGenes();
      ind.up();    
      while (git.hasNext()) {
        CopiesPerEmbryoGene tg = git.next();
        tg.writeXML(out, ind);
      }
      ind.down();
    }
    if (defaultTimes_.size() > 0) {
      writeDefaultTimes(out, ind);
    } 
    if (gpeMap_.size() > 0) {
      writeCpeMap(out, ind);
    } 
    ind.indent();
    out.println("</CopiesPerEmbryoData>");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("CopiesPerEmbryoData: " + " genes = " + genes_);
  }
  
  /***************************************************************************
  **
  ** Answers if a target gene name is unique
  */
  
  public boolean nameIsUnique(String targName) {
    Iterator<CopiesPerEmbryoGene> git = getGenes();  
    while (git.hasNext()) {
      CopiesPerEmbryoGene tg = git.next();
      if (DataUtil.keysEqual(tg.getName(), targName)) {
        return (false);
      }
    }  
    return (true);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("CopiesPerEmbryoData");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */

  @SuppressWarnings("unused")
  public static CopiesPerEmbryoData buildFromXML(BTState appState, String elemName, 
                                                 Attributes attrs) throws IOException {
    if (!elemName.equals("CopiesPerEmbryoData")) {
      return (null);
    }
    
    return (new CopiesPerEmbryoData(appState));
  }

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractCpeMapKey(String elemName, 
                                        Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "cpeMap", "key", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractUseCpe(String elemName, 
                                    Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "useCpe", "name", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static int extractTime(String elemName, 
                                Attributes attrs) throws IOException {
    String timeVal = AttributeExtractor.extractAttribute(elemName, attrs, "cpeTime", "time", true);
    try {
      return (Integer.parseInt(timeVal));
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
  }  
  
  /***************************************************************************
  **
  ** Return the cpeMap keyword
  **
  */
  
  public static String cpeMapKeyword() {
    return ("cpeMap");
  }
  
  /***************************************************************************
  **
  ** Return the useCpe keyword
  **
  */
  
  public static String useCpeKeyword() {
    return ("useCpe");
  }
  
  /***************************************************************************
  **
  ** Return the cpeTime keyword
  **
  */
  
  public static String cpeTimeKeyword() {
    return ("cpeTime");
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the cpe map to XML
  **
  */
  
  private void writeCpeMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<cpeMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(gpeMap_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<String> list = gpeMap_.get(key);
      ind.indent();
      out.print("<cpeMap key=\"");
      out.print(key);
      out.println("\">");
      Iterator<String> lit = list.iterator();
      ind.up();    
      while (lit.hasNext()) {
        String usetc = lit.next();      
        ind.indent();
        out.print("<useCpe name=\"");
        out.print(usetc);
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</cpeMap>");
    }
    ind.down().indent(); 
    out.println("</cpeMaps>");
    ind.down();
    return;
  }  
  
  /***************************************************************************
  **
  ** Write the default times to XML
  **
  */
  
  private void writeDefaultTimes(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<cpeDefaultTimes>");
    Iterator<Integer> timeit = defaultTimes_.iterator();
    ind.up();    
    while (timeit.hasNext()) {
      Integer time = timeit.next();     
      ind.indent();
      out.print("<cpeTime time=\"");
      out.print(time);
      out.println("\">");
    }
    ind.down().indent(); 
    out.println("</cpeDefaultTimes>");
    ind.down();
    return;
  }  
}
