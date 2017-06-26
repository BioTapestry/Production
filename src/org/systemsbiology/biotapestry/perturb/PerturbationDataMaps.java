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

package org.systemsbiology.biotapestry.perturb;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.qpcr.QpcrLegacyPublicExposed;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** Mapping of genes to perturbation entries
*/

public class PerturbationDataMaps implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  public static enum MapType {SOURCE, ENTRIES}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  //
  // The chunk of original PerturbationData devoted to mapping nodes to data
  // entries. Split out so each database can use one underlying data source.
  //
  
  private NameMapper entryMap_;
  private NameMapper sourceMap_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbationDataMaps() {
    entryMap_ = new NameMapper("targets");
    sourceMap_ = new NameMapper("sources"); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Duplicate a source name map (if it exists)
  */
  
  public PertDataChange[] duplicateSourceNameMap(String existingID, String dupID) {
    return (sourceMap_.dupAMapTo(existingID, dupID));
  }

  /***************************************************************************
  **
  ** Duplicate a target name map (if it exists)
  */
  
  public PertDataChange[] duplicateTargetNameMap(String existingID, String dupID) {
    return (entryMap_.dupAMapTo(existingID, dupID));
  }
  
  /***************************************************************************
  **
  ** Answer if we have a map to the source
  */
  
  public boolean haveSourceNameMapTo(String chkID) {
    return (sourceMap_.haveMapToEntry(chkID));
  }

  /***************************************************************************
  **
  ** Answer if we have a map to the target
  */
  
  public boolean haveTargetNameMapTo(String chkID) {
    return (entryMap_.haveMapToEntry(chkID));
  }

  /***************************************************************************
  **
  ** Return the serialNumbers
  */
  
  public long getSerialNumber(MapType which) {
    return ((which == MapType.ENTRIES) ? entryMap_.getSerialNumber() : sourceMap_.getSerialNumber());
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(PertDataChange undo) {
    switch (undo.mode) {
      case NAME_MAPPER:
        if (undo.nameMapperMode.equals("targets")) {
          entryMap_.changeUndo(undo);
        } else if (undo.nameMapperMode.equals("sources")) {
          sourceMap_.changeUndo(undo);
        } else {
          throw new IllegalArgumentException();
        }
        return;  
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(PertDataChange redo) {
    switch (redo.mode) {
      case NAME_MAPPER:
        if (redo.nameMapperMode.equals("targets")) {
          entryMap_.changeRedo(redo);
        } else if (redo.nameMapperMode.equals("sources")) {
          sourceMap_.changeRedo(redo);
        } else {
          throw new IllegalArgumentException();
        }
        return;
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Answers if we have perturbation data
  **
  */
  
  public boolean haveData() {    
    if (entryMap_.haveData()) {
      return (true);
    }      
    if (sourceMap_.haveData()) {
      return (true);
    }   
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomEntryMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    return (entryMap_.haveCustomMapForNode(nodeID));
  }
  
  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomSourceMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    return (sourceMap_.haveCustomMapForNode(nodeID));
  }
  
  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    return (haveCustomSourceMapForNode(nodeID) || haveCustomEntryMapForNode(nodeID));
  }
  
  /***************************************************************************
  **
  ** Drop the maps to an entry that is about to disappear
  */
  
  public PertDataChange[] dropDanglingMapsForEntry(String entryID) {
    return (entryMap_.dropDanglingMapsFor(entryID));
  }  
  
  /***************************************************************************
  **
  ** Drop the maps to a source map that is about to disappear
  */
  
  public PertDataChange[] dropDanglingMapsForSource(String entryID) {
    return (sourceMap_.dropDanglingMapsFor(entryID));
  } 
  
  /***************************************************************************
  **
  ** Merge Maps
  */
  
  public PertDataChange[] mergeMapsForEntries(String keepID, List<String> allIDs) {
    return (entryMap_.mergeMapsFor(keepID, allIDs));
  }  
  
  /***************************************************************************
  **
  ** Merge Maps
  */
  
  public PertDataChange[] mergeMapsForSources(String keepID, List<String> allIDs) {
    return (sourceMap_.mergeMapsFor(keepID, allIDs));
  }
  
  /***************************************************************************
  **
  ** Drop identity maps
  */
  
  public PertDataChange[] dropIdentityMapsForEntries(DBGenome genome, Map<String, String> targets) {
    return (entryMap_.dropIdentityMaps(genome, targets));
  }  
  
  /***************************************************************************
  **
  ** Drop identity maps
  */
  
  public PertDataChange[] dropIdentityMapsForSources(DBGenome genome, Map<String, String> targets) {
    return (sourceMap_.dropIdentityMaps(genome, targets));
  }  
  
  
  /***************************************************************************
  **
  ** Use for IO
  */
  
  public void installNameMap(NameMapper nMap) {
    String usage = nMap.getUsage();
    if (usage.equals("targets")) {
      entryMap_ = nMap;
    } else if (usage.equals("sources")) {
      sourceMap_ = nMap;
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of target nodes
  */
  
  public PertDataChange setEntryMap(String key, List<String> entries) {
    return (entryMap_.setDataMap(key, entries));
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of targets, used in legacy
  ** imports only!
  */
  
  public void importLegacyEntryMapEntry(String key, List<String> entries, Map<String, String> invertTargNameCache) {
    entryMap_.importLegacyMapEntry(key, entries, invertTargNameCache);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of sources, used in legacy
  ** imports only!
  */
  
  public void importLegacySourceMapEntry(String key, List<String> entries, Map<String, String> invertSrcNameCache) {
    sourceMap_.importLegacyMapEntry(key, entries, invertSrcNameCache);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a map from a gene to a List of source genes
  */
  
  public PertDataChange setSourceMap(String key, List<String> entries) {
    return (sourceMap_.setDataMap(key, entries));
  }
  
  /***************************************************************************
  **
  ** Add both entry and source maps
  */
  
  public PertDataChange[] addDataMaps(String key, List<String> entries, List<String> sources) { 
    ArrayList<PertDataChange> retvalList = new ArrayList<PertDataChange>();
    PertDataChange retval;
    if ((entries != null) && (entries.size() > 0)) {
      retval = setEntryMap(key, entries);
      retvalList.add(retval);
    }
    if ((sources != null) && (sources.size() > 0)) {
      retval = setSourceMap(key, sources);
      retvalList.add(retval);
    }
    return (retvalList.toArray(new PertDataChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the gene ID.  May be empty.
  */
  
  public List<String> getDataEntryKeysWithDefault(DBGenome genome, String nodeId, PerturbationData pd) {
    String name = genome.getNode(nodeId).getName();
    return (entryMap_.getDataKeysWithDefault(nodeId, pd.getTargKeyFromName(name)));
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the gene ID.  May be empty.
  */
  
  public List<String> getDataSourceKeysWithDefault(DBGenome genome, String nodeId, PerturbationData pd) {
    String name = genome.getNode(nodeId).getName();
    return (sourceMap_.getDataKeysWithDefault(nodeId, pd.getSourceKeyFromName(name)));
  }

  /***************************************************************************
  **
  ** Get the list of targets names for the gene ID.  May be empty.
  */ 
  
  public List<String> getDataEntryKeysWithDefaultGivenName(String nodeId, String nodeName, PerturbationData pd) {  
    return (entryMap_.getDataKeysWithDefault(nodeId, pd.getTargKeyFromName(nodeName)));
  }
  
  /***************************************************************************
  **
  ** Get the list of entry names for the gene ID.  May be null.
  */
  
  public List<String> getCustomDataEntryKeys(String geneId) {
    return (entryMap_.getCustomDataKeys(geneId));    
  }
  
  /***************************************************************************
  **
  ** Get the list of pert source keys for the gene ID.  May be null.
  */
  
  public List<String> getCustomDataSourceKeys(String geneId) {
    return (sourceMap_.getCustomDataKeys(geneId));    
  }  

  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation target key
  */

  public Set<String> getDataEntryKeyInverse(DBGenome genome, String key, Map<String, String> targets) {
    return (entryMap_.getDataKeyInverse(genome, key, targets));
  }
  
  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation source key
  */

  public Set<String> getDataSourceKeyInverse(DBGenome genome, String key, Map<String, String> sourceNames) {
    return (sourceMap_.getDataKeyInverse(genome, key, sourceNames));
  }
  
  /***************************************************************************
  **
  ** Answer if we are vulnerable to a disconnect:
  */

  public boolean dataEntryOnlyInverseIsDefault(DBGenome genome, String key, Map<String, String> targets) {
    return (entryMap_.onlyInverseIsDefault(genome, key, targets));
  }
  
  /***************************************************************************
  **
  ** Answer if we are vulnerable to a disconnect:
  */

  public boolean dataSourceOnlyInverseIsDefault(DBGenome genome, String key, Map<String, String> sourceNames) {
    return (sourceMap_.onlyInverseIsDefault(genome, key, sourceNames));
  }

  /***************************************************************************
  **
  ** Get the Set of keys that map to targets.
  */
  
  public Set<String> getDataEntryKeySet() {
    return (entryMap_.getDataKeySet());
  }
  
  /***************************************************************************
  **
  ** Get the Set of keys that map to sources.
  */
  
  public Set<String> getDataSourceKeySet() {
    return (sourceMap_.getDataKeySet());
  }  
 
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public PertDataChange dropDataEntryKeys(String geneId) {
    if (!entryMap_.haveCustomMapForNode(geneId) && !entryMap_.haveEmptyMapForNode(geneId)) {
      return (null);
    }
    return (entryMap_.dropDataKeys(geneId));
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public PertDataChange dropDataSourceKeys(String geneId) {
    if (!sourceMap_.haveCustomMapForNode(geneId) && !sourceMap_.haveEmptyMapForNode(geneId)) {
      return (null);
    }
    return (sourceMap_.dropDataKeys(geneId));
  }
  
  /***************************************************************************
  **
  ** Drop all entry key mappings.  Big serial number bumping!
  */
  
  public PertDataChange[] dropAllDataEntryKeys() {   
    ArrayList<PertDataChange> retval = new ArrayList<PertDataChange>();
    Iterator<String> ksit = new HashSet<String>(getDataEntryKeySet()).iterator();
    while (ksit.hasNext()) {
      String id = ksit.next(); 
      PertDataChange pdc = dropDataEntryKeys(id);
      retval.add(pdc);
    }  
    return (retval.toArray(new PertDataChange[retval.size()]));    
  }
  
  /***************************************************************************
  **
  ** Drop all source key mappings.  Big serial number bumping!
  */
  
  public PertDataChange[] dropAllDataSourceKeys() {   
    ArrayList<PertDataChange> retval = new ArrayList<PertDataChange>();
    Iterator<String> ksit = new HashSet<String>(getDataSourceKeySet()).iterator();
    while (ksit.hasNext()) {
      String id = ksit.next(); 
      PertDataChange pdc = dropDataSourceKeys(id);
      retval.add(pdc);
    }  
    return (retval.toArray(new PertDataChange[retval.size()]));    
  }
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PerturbationDataMaps clone() {
    try {
      PerturbationDataMaps newVal = (PerturbationDataMaps)super.clone();      
      newVal.entryMap_ = this.entryMap_.clone();
      newVal.sourceMap_ = this.sourceMap_.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Write the Perturbation Maps data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<pertDataMaps>");
    ind.up();
     
    boolean doEntry = entryMap_.haveData();
    boolean doSrc = sourceMap_.haveData();    
    
    if (doEntry || doSrc) {
      if (doEntry) {
        entryMap_.writeXML(out, ind);
      }
      if (doSrc) {
        sourceMap_.writeXML(out, ind);
      }
    }
    ind.down().indent();    
    out.println("</pertDataMaps>");
    return;
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class DataMapsWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    private NameMapper.NameMapperWorker nmw_;
    
    public DataMapsWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertDataMaps");    
      nmw_ = new NameMapper.NameMapperWorker(whiteboard);
      installWorker(nmw_, new MyNameMapperGlue());
    }
   
   /***************************************************************************
   **
   ** Set the current context
   */
  
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
 
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertDataMaps")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pdms = buildFromXML(elemName, attrs);
        retval = board.pdms;
        if (retval != null) {
          dacx_.getDataMapSrc().setPerturbationDataMaps(board.pdms);
        }
      }
      return (retval);     
    }  
    
    private PerturbationDataMaps buildFromXML(String elemName, Attributes attrs) throws IOException {  
      return (new PerturbationDataMaps());
    }
  }
  
  public static class MyNameMapperGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pdms.installNameMap(board.currPertDataMap);
      return (null);
    }
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
}
