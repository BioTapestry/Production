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

package org.systemsbiology.biotapestry.perturb;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** Handles mapping genes to possibly multiple underlying data sources
*/

public class NameMapper implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private HashMap<String, List<String>> mapToData_;
  private long serialNumber_;
  private String usage_;
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

  public NameMapper(BTState appState, String usage) {
    appState_ = appState;
    mapToData_ = new HashMap<String, List<String>>();
    serialNumber_ = 0L;
    usage_ = usage;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  public NameMapper clone() {
    try {
      NameMapper newVal = (NameMapper)super.clone();
      newVal.mapToData_ = new HashMap<String, List<String>>();
      Iterator<String> ksit = this.mapToData_.keySet().iterator();
      while(ksit.hasNext()) {
        String key = ksit.next();
        List<String> al = mapToData_.get(key);
        newVal.mapToData_.put(key, new ArrayList<String>(al));
      }
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }  
  }
 
  /***************************************************************************
  **
  ** Return the serialNumber
  */
  
  public long getSerialNumber() {
    return (serialNumber_);
  }
  
  /***************************************************************************
  **
  ** Set the serialNumber.  Only used in IO!
  */
  
  public void setSerialNumber(long serialNumber) {
    serialNumber_ = serialNumber;
    return;
  }

  /***************************************************************************
  **
  ** Get the usage
  */
  
  public String getUsage() {
    return (usage_);
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(PertDataChange undo) {
    if (undo.nameMapperListOrig != null) {
      mapToData_.put(undo.nameMapperKey, undo.nameMapperListOrig);
    } else {
      mapToData_.remove(undo.nameMapperKey);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(PertDataChange redo) {
    if (redo.nameMapperListNew != null) {
      mapToData_.put(redo.nameMapperKey, redo.nameMapperListNew);
    } else {
      mapToData_.remove(redo.nameMapperKey);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
  
  /***************************************************************************
  **
  ** Answers if we have mapping data
  **
  */
  
  public boolean haveData() {
    return (mapToData_.size() > 0);
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a list of nodes, used in legacy
  ** imports only!
  */
  
  public void importLegacyMapEntry(String key, List<String> entries, Map<String, String> nameToKeys) {
    int numE = entries.size();
    if (numE == 0) {
      return;  // clean up dangling legacy maps!
    }
    List<String> mappedEntries = new ArrayList<String>();
    for (int i = 0; i < numE; i++) {
      String oldName = entries.get(i);
      String newKey = nameToKeys.get(DataUtil.normKey(oldName));
      if ((newKey != null) && !mappedEntries.contains(newKey)) {
        mappedEntries.add(newKey);
      }
    }
    if (!mappedEntries.isEmpty()) {
      mapToData_.put(key, mappedEntries);
    }
    return;
  }
  

  /***************************************************************************
  **
  ** Answers if we have a mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    List<String> entryMapped = mapToData_.get(nodeID);
    if ((entryMapped == null) || (entryMapped.size() == 0)) {      
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if we have an empty, non-null mapping
  **
  */
  
  public boolean haveEmptyMapForNode(String nodeID) {
    List<String> entryMapped = mapToData_.get(nodeID);
    return ((entryMapped != null) && (entryMapped.size() == 0));   
  }
  
  /***************************************************************************
  **
  ** Duplicating a source/targ means that we give the option to duplicate
  ** the maps too!
  */
  
  public PertDataChange[] dupAMapTo(String existingID, String dupID) {
    ArrayList<PertDataChange> retvalList = new ArrayList<PertDataChange>();
    Iterator<String> mit = mapToData_.keySet().iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<String> targList = mapToData_.get(mapKey);
      if (targList.contains(existingID)) {
        PertDataChange pdc = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);
        pdc.nameMapperKey = mapKey;
        pdc.nameMapperMode = usage_;
        pdc.nameMapperListOrig = new ArrayList<String>(targList);
        targList.add(dupID);
        pdc.nameMapperListNew = new ArrayList<String>(targList);
        pdc.serialNumberNew = ++serialNumber_;
        retvalList.add(pdc);
        break;
      }
    }   
    return ((PertDataChange[])retvalList.toArray(new PertDataChange[retvalList.size()]));
  }  

  /***************************************************************************
  **
  ** When dropping a perturbation src/targ, we drop the associated maps that 
  ** are dangling.
  */
  
  public PertDataChange[] dropDanglingMapsFor(String deadID) {
    ArrayList<PertDataChange> retvalList = new ArrayList<PertDataChange>();
    // copy avoids concurrent modification:
    Iterator<String> mit = new HashSet<String>(mapToData_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = (String)mit.next();
      List<String> targList = mapToData_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = (String)targList.get(i);
        if (deadID.equals(targ)) {
          PertDataChange pdc = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);
          pdc.nameMapperKey = mapKey;
          pdc.nameMapperMode = usage_;
          pdc.nameMapperListOrig = new ArrayList<String>(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            pdc.nameMapperListNew = null;
            mapToData_.remove(mapKey);
          } else {
            pdc.nameMapperListNew = new ArrayList<String>(targList);
          }
          pdc.serialNumberNew = ++serialNumber_;
          retvalList.add(pdc);
          break;
        }
      }
    }   
    return ((PertDataChange[])retvalList.toArray(new PertDataChange[retvalList.size()]));
  }  
  
  /***************************************************************************
  **
  ** When merging several names to one, use this to preserve maps.  Everybody who
  ** points to a dropped ID needs to point to the keepID
  */
  
  public PertDataChange[] mergeMapsFor(String keepID, List<String> allIDs) {
    ArrayList<PertDataChange> retvalList = new ArrayList<PertDataChange>();
    Iterator<String> mit = mapToData_.keySet().iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<String> newList = new ArrayList<String>();
      List<String> targList = mapToData_.get(mapKey);
      int numTarg = targList.size();
      boolean changed = false;
      boolean gotKeepID = false;
      for (int i = 0; i < numTarg; i++) {
        String targ = (String)targList.get(i);
        if (!allIDs.contains(targ)) {
          newList.add(targ);
        } else if (keepID.equals(targ)) {
          newList.add(targ);
          gotKeepID = true;
        } else {
          changed = true;
        }
      }
      if (changed && !gotKeepID) {
        newList.add(keepID);
      }
      if (changed) {
        PertDataChange pdc = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);
        pdc.nameMapperKey = mapKey;
        pdc.nameMapperMode = usage_;
        pdc.nameMapperListOrig = new ArrayList<String>(targList);
        targList.clear();
        targList.addAll(newList);
        pdc.nameMapperListNew = new ArrayList<String>(targList);
        pdc.serialNumberNew = ++serialNumber_;
        retvalList.add(pdc);
      }
    }   
    return ((PertDataChange[])retvalList.toArray(new PertDataChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** During map manipulations, a map might turn into an identity map.  If we
  ** detect such a map, we will drop it.
  */
  
  public PertDataChange[] dropIdentityMaps(Map<String, String> nameResolver) {    
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();    
    ArrayList<PertDataChange> retvalList = new ArrayList<PertDataChange>();       
    // copy avoids concurrent modification:
    Iterator<String> mit = new HashSet<String>(mapToData_.keySet()).iterator();
    while (mit.hasNext()) {
      String nodeID = mit.next();
      List<String> newList = new ArrayList<String>();
      List<String> targList = mapToData_.get(nodeID);
      String name = genome.getNode(nodeID).getName();       
      int numTarg = targList.size();
      boolean changed = false;
      for (int i = 0; i < numTarg; i++) {
        String targ = targList.get(i);
        String targName = nameResolver.get(targ);
        if (!DataUtil.keysEqual(name, targName)) {
          newList.add(targ);
        } else {
          changed = true;
        }
      }
      if (changed) {
        PertDataChange pdc = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);
        pdc.nameMapperKey = nodeID;
        pdc.nameMapperMode = usage_;
        pdc.nameMapperListOrig = new ArrayList<String>(targList);
        targList.clear();
        targList.addAll(newList);      
        if (targList.isEmpty()) {
          mapToData_.remove(nodeID);
          pdc.nameMapperListNew = null;
        } else {
          pdc.nameMapperListNew = new ArrayList<String>(targList);
        }
        pdc.serialNumberNew = ++serialNumber_;
        retvalList.add(pdc);
      }
    }   
    return ((PertDataChange[])retvalList.toArray(new PertDataChange[retvalList.size()]));
  }

  /***************************************************************************
  **
  ** Set the map from a node to a list of targets
  */
  
  public PertDataChange setDataMap(String key, List<String> entries) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);    
    retval.nameMapperKey = key;
    retval.nameMapperMode = usage_;
    List<String> orig = mapToData_.get(key);
    retval.nameMapperListOrig = (orig == null) ? null : new ArrayList<String>(orig);   
    if ((entries != null) && (entries.size() > 0)) {
      ArrayList<String> uniqueEntries = new ArrayList<String>();
      int numE = entries.size();
      for (int i = 0; i < numE; i++) {
        String givenKey = entries.get(i);
        if (!uniqueEntries.contains(givenKey)) {
          uniqueEntries.add(givenKey);
        }
      }   
      mapToData_.put(key, uniqueEntries);
      retval.nameMapperListNew = new ArrayList<String>(uniqueEntries);
    } else {
      mapToData_.remove(key);
      retval.nameMapperListNew = null;
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Use for IO
  */
  
  public void initDataMapForIO(String key) {
    mapToData_.put(key, new ArrayList<String>());
    return;
  }
  
  /***************************************************************************
  **
  ** Use for IO
  */
  
  public void appendToDataMapForIO(String key, String newEntry) {
    List<String> forKey = mapToData_.get(key);
    forKey.add(newEntry);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of targets IDs for the gene ID.  May be empty.
  */
  
  public List<String> getDataKeysWithDefault(String nodeId, String defaultKey) {  
    List<String> retval = mapToData_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      if (defaultKey == null) {
        return (retval);
      }
      retval.add(defaultKey);
    }    
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Get the list of targets IDs for the gene ID.  May be null.
  */
  
  public List<String> getCustomDataKeys(String geneId) {
    List<String> keyList = mapToData_.get(geneId);    
    return ((keyList == null) ? null : new ArrayList<String>(keyList));
  }
 
  /***************************************************************************
  **
  ** Get the node IDs that target the given map entry
  */

  public Set<String> getDataKeyInverse(String key, Map<String, String> nameResolver) {
        
    HashSet<String> retval = new HashSet<String>();
    //
    // If there is anybody out there with the same name _and no custom map_, it
    // will map by default:
    //
    
   
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    String resolvesToName = nameResolver.get(key);
    Node node = genome.getGeneWithName(resolvesToName);
    if (node != null) {
      if (!haveCustomMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(resolvesToName);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = mapToData_.keySet().iterator();
    while (kit.hasNext()) {
      String mdkey = kit.next();
      List<String> targs = mapToData_.get(mdkey);
      if (targs.contains(key)) {  
        retval.add(mdkey);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get answer if anybody maps to the given entry
  */

  public boolean haveMapToEntry(String entryID) {
        
    Iterator<String> kit = mapToData_.keySet().iterator();
    while (kit.hasNext()) {
      String mdkey = kit.next();
      List<String> targs = mapToData_.get(mdkey);
      if (targs.contains(entryID)) {
        return (true);
      }
    }
    return (false);
  }
   
  /***************************************************************************
  **
  ** Answers if the only connection to the data is via default, and thus
  ** in danger from a name change
  */

  public boolean onlyInverseIsDefault(String key, Map<String, String> nameResolver) {

    //
    // If there is anybody out there with the same name _and no custom map_, we
    // have an issue:
    //
    
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    String resolvesToName = nameResolver.get(key);
    Node node = genome.getGeneWithName(resolvesToName);
    if (node != null) {
      if (!haveCustomMapForNode(node.getID())) {
        return (true);
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(resolvesToName);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomMapForNode(node.getID())) {
          return (true);
        }
      }
    }

    return (false);
  }
  
  
  /***************************************************************************
  **
  ** Get the Set of keys that map to targets.
  */
  
  public Set<String> getDataKeySet() {
    return (mapToData_.keySet());
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public PertDataChange dropDataKeys(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.NAME_MAPPER);    
    retval.nameMapperKey = key;
    retval.nameMapperMode = usage_;
    List<String> orig = mapToData_.get(key);
    retval.nameMapperListOrig = new ArrayList<String>(orig);
    mapToData_.remove(key);
    retval.nameMapperListNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Write the data map to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<dataMap usage=\"");
    out.print(usage_);
    out.print("\"");
    if (serialNumber_ != 0L) {
      out.print(" serialNum=\"");
      out.print(serialNumber_);
      out.print("\"");
    }
    out.println(">");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(mapToData_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<String> elist = mapToData_.get(key);
      ind.indent();
      out.print("<mapFrom");
      out.print(" key=\"");      
      out.print(key);
      out.println("\">");
      ind.up();
      Iterator<String> lit = elist.iterator();
      while (lit.hasNext()) {
        String mapTo = lit.next();      
        ind.indent();
        out.print("<mapTo nameKey=\"");          
        out.print(mapTo);
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</mapFrom>");
    }
    ind.down().indent(); 
    out.println("</dataMap>");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return (" NameMapper = " + mapToData_ + "  serial number = " + serialNumber_);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NameMapperWorker extends AbstractFactoryClient {
 
    private BTState appState_;
    
    public NameMapperWorker(BTState appState, FactoryWhiteboard whiteboard) {
      super(whiteboard);
      appState_ = appState;
      myKeys_.add("dataMap");
      installWorker(new MapFromWorker(whiteboard), new MyMapFromGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("dataMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currPertDataMap = buildFromXML(elemName, attrs);
        retval = board.currPertDataMap;
      }
      return (retval);     
    }  
    
    private NameMapper buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String serNum = AttributeExtractor.extractAttribute(elemName, attrs, "dataMap", "serialNum", false);
      String usage = AttributeExtractor.extractAttribute(elemName, attrs, "dataMap", "usage", true);
      NameMapper retval = new NameMapper(appState_, usage);
      if (serNum != null) { 
        try {
          long sNum = Long.parseLong(serNum);
          retval.setSerialNumber(sNum);
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      }
      return (retval);
    }
  }
  
  public static class MyMapFromGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NameMapper nmap = board.currPertDataMap;
      nmap.initDataMapForIO(board.currPertMapFrom);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class MapFromWorker extends AbstractFactoryClient {
 
    public MapFromWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("mapFrom");
      installWorker(new NameMapper.MapToWorker(whiteboard), new MyMapToGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("mapFrom")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currPertMapFrom = buildFromXML(elemName, attrs);
        retval = board.currPertMapFrom;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "mapFrom", "key", true);
      return (key);
    }
  }
  
  public static class MyMapToGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NameMapper nmap = board.currPertDataMap;
      nmap.appendToDataMapForIO(board.currPertMapFrom, board.currPertMapTo);
      return (null);
    }
  }
  
   /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class MapToWorker extends AbstractFactoryClient {
      
    public MapToWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("mapTo");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("mapTo")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currPertMapTo = buildFromXML(elemName, attrs);
        retval = board.currPertMapTo;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "mapTo", "nameKey", true);
      return (name);
    }
  }
}
