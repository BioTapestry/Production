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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This holds TemporalInputRangeData
*/

public class TemporalInputRangeData {
  
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

  private ArrayList<TemporalRange> entries_;
  private HashMap<String, List<String>> trEntryMap_;
  private HashMap<String, List<String>> trSourceMap_;  
  private HashMap<String, List<GroupUsage>> groupMap_;
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

  public TemporalInputRangeData(BTState appState) {
    appState_ = appState;
    entries_ = new ArrayList<TemporalRange>();
    trEntryMap_ = new HashMap<String, List<String>>();
    trSourceMap_ = new HashMap<String, List<String>>();    
    groupMap_ = new HashMap<String, List<GroupUsage>>();
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
    if (!entries_.isEmpty()) {
      return (true);
    }
    if (!trEntryMap_.isEmpty()) {
      return (true);
    }
    if (!trSourceMap_.isEmpty()) {
      return (true);
    }    
    if (!groupMap_.isEmpty()) {
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have a tir mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    return (haveCustomSourceMapForNode(nodeID) || haveCustomEntryMapForNode(nodeID));
  } 
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input region mapping
  **
  */
  
  public boolean haveCustomMapForRegion(String groupId) {
    if (!haveData()) {
      return (false);
    }
    List<GroupUsage> mapped = groupMap_.get(groupId);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input mapping
  **
  */
  
  public boolean haveCustomEntryMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = trEntryMap_.get(nodeID);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if we have a temporal input mapping
  **
  */
  
  public boolean haveCustomSourceMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = trSourceMap_.get(nodeID);
    if ((mapped == null) || (mapped.size() == 0))  {
      return (false);
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getTemporalInputRangeEntryKeysWithDefault(nodeID);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getRange(name) != null) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNodeOrName(String nodeID, String nodeName) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = getTemporalInputRangeEntryKeysWithDefaultGivenName(nodeID, nodeName);
    if (mapped == null) {
      return (false);
    }
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String name = mit.next();
      if (getRange(name) != null) {
        return (true);
      }
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Get the node IDs that target the given table entry
  */

  public Set<String> getTemporalInputEntryKeyInverse(String name) {
    name = DataUtil.normKey(name);
    HashSet<String> retval = new HashSet<String>();
    //
    // If there is anybody out there with the same name _and no custom map_, it
    // will map by default:
    //
    
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    Node node = genome.getGeneWithName(name);
    if (node != null) {
      if (!haveCustomEntryMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(name);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomEntryMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = trEntryMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> targs = trEntryMap_.get(key);
      Iterator<String> trit = targs.iterator();
      while (trit.hasNext()) {
        String testName = trit.next();
        if (DataUtil.keysEqual(testName, name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation source name
  */

  public Set<String> getTemporalInputSourceKeyInverse(String name) {
    name = DataUtil.normKey(name);
    HashSet<String> retval = new HashSet<String>();
    //
    // Blank names cannot be inverted, and blank names are handed to us when
    // new entries are added to the tables. (Bug BT-08-18-04:1)
    //
    if (name.equals("")) {
      return (retval);
    }
    //
    // If there is anybody out there with the same name _and no custom map_, it
    // will map by default:
    //
    
    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    Node node = genome.getGeneWithName(name);
    if (node != null) {
      if (!haveCustomSourceMapForNode(node.getID())) {
        retval.add(node.getID());
      }
    }
    
    Set<Node> nodes = genome.getNodesWithName(name);
    if (!nodes.isEmpty()) {
      Iterator<Node> sit = nodes.iterator();
      while (sit.hasNext()) {
        node = sit.next();
        if (!haveCustomSourceMapForNode(node.getID())) {
          retval.add(node.getID());
        }
      }
    }

    Iterator<String> kit = trSourceMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> targs = trSourceMap_.get(key);
      Iterator<String> trit = targs.iterator();
      while (trit.hasNext()) {
        String testName = trit.next();
        if (DataUtil.keysEqual(testName, name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Gets input sources that do not appear as targets
  */
  
  public Set<String> getNonTargetSources() {
    HashSet<String> retval = new HashSet<String>();
    HashSet<String> targetNames = new HashSet<String>();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      targetNames.add(DataUtil.normKey(trg.getName()));
    }
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (!DataUtil.containsKey(targetNames, itrName)) {
          retval.add(itrName);
        }
      }
    }
    
    return (retval);
  }

  /***************************************************************************
  **
  ** Gets all input sources
  */
  
  public Set<String> getAllSources() {
    HashSet<String> retval = new HashSet<String>();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (!DataUtil.containsKey(retval, itrName)) {
          retval.add(itrName);
        }
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answer if the given name shows up as a perturbation source:
  */
  
  public boolean isPerturbationSourceName(String name) {
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      Iterator<InputTimeRange> itrs = trg.getTimeRanges();
      while (itrs.hasNext()) {
        InputTimeRange itr = itrs.next();
        String itrName = itr.getName();
        if (DataUtil.keysEqual(name, itrName)) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) || (undo.entryMapListNew != null)) {
      entryMapChangeUndo(undo);
    } else if ((undo.sourceMapListOrig != null) || (undo.sourceMapListNew != null)) {
      sourceMapChangeUndo(undo);      
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeUndo(undo);      
    } else if ((undo.eOrig != null) || (undo.eNew != null)) {
      entryChangeUndo(undo);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) || (undo.entryMapListNew != null)) {
      entryMapChangeRedo(undo);
    } else if ((undo.sourceMapListOrig != null) || (undo.sourceMapListNew != null)) {
      sourceMapChangeRedo(undo);
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeRedo(undo);
    } else if ((undo.eOrig != null) || (undo.eNew != null)) {
      entryChangeRedo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public TemporalInputChange startRangeUndoTransaction(String targetName) {
    TemporalInputChange retval = new TemporalInputChange();
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange trg = entries_.get(i);
      String name = trg.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        retval.eOrig = new TemporalRange(trg);
        retval.entryPos = i;
        return (retval);
      }
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public TemporalInputChange finishRangeUndoTransaction(TemporalInputChange change) {
    TemporalRange trg = entries_.get(change.entryPos);
    change.eNew = new TemporalRange(trg);
    return (change);
  }
  
  /***************************************************************************
  **
  ** Answer if the data is empty
  */
  
  public boolean isEmpty() {
    return (entries_.isEmpty());
  } 
  
  /***************************************************************************
  **
  ** Add an entry
  */
  
  public TemporalInputChange addEntry(TemporalRange range) {
    TemporalInputChange retval = new TemporalInputChange();
    retval.entryPos = entries_.size();      
    entries_.add(range);
    retval.eNew = new TemporalRange(range);
    retval.eOrig = null;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the entries
  */
  
  public Iterator<TemporalRange> getEntries() {
    return (entries_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get nth entry
  */
  
  public TemporalRange getEntry(int n) {
    return (entries_.get(n));
  }
  /***************************************************************************
  **
  ** Drop maps that resolve to the given input range entry
  */
  
  public TemporalInputChange[] dropMapsTo(String entryID) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> mit = new HashSet<String>(trEntryMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<String> targList = trEntryMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = targList.get(i);
        if (DataUtil.keysEqual(targ, entryID)) {
          TemporalInputChange tic = new TemporalInputChange();
          tic.mapKey = mapKey;
          tic.entryMapListOrig = new ArrayList<String>(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tic.entryMapListNew = null;
            trEntryMap_.remove(mapKey);
          } else {
            tic.entryMapListNew = new ArrayList<String>(targList);
          }
          retvalList.add(tic);
          break;
        }
      }
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
 
  /***************************************************************************
  **
  ** Fixup all maps to lose dangling references
  */

  /*
  public TemporalInputChange[] mapFixups() {

    ArrayList retvalList = new ArrayList();
 
    //
    // Make a set of all entry names and row names, and a set of all region 
    // names.  Go through the maps and drop any mapping to a name not in those
    // sets.
    //
    

    Iterator mit = new HashSet(trMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = (String)mit.next();
      List targList = (List)trMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        String targ = (String)targList.get(i);
        if (targ.equals(entryID)) {
          TemporalInputChange tic = new TemporalInputChange();
          tic.mapKey = mapKey;
          tic.mapListOrig = new ArrayList(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tic.mapListNew = null;
            trMap_.remove(mapKey);
          } else {
            tic.mapListNew = new ArrayList(targList);
          }
          retvalList.add(tic);
          break;
        }
      }
    }
    return ((TemporalInputChange[])retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** When changing an entry name, we need to change one or more maps to keep
  ** them consistent.  Do this operation. Return empty array if no changes occurred.
  */
  
  public TemporalInputChange[] changeDataMapsToName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> tmit = trEntryMap_.keySet().iterator();
    while (tmit.hasNext()) {
      String mkey = tmit.next();
      List<String> keys = trEntryMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.entryMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);        
        keys.add(newName);
        retval.entryMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }

  /***************************************************************************
  **
  ** We are changing the name of a node or gene, and have been told to modify the
  ** data as needed.  We need to change the entry (if any) to the given name, the
  ** source rows, and any custom maps to that name.
  */
  
  public TemporalInputChange[] changeName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    
    if (!DataUtil.keysEqual(oldName, newName)) {
      if ((getRange(newName) != null) || isPerturbationSourceName(newName)) {
        throw new IllegalArgumentException();
      }
    }
     
    //
    // Do all the rows in other entries:
    //
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        String pertName = itr.getName();
        if (DataUtil.keysEqual(oldName, pertName)) {
          TemporalInputChange tic = startRangeUndoTransaction(tr.getName());
          itr.setName(newName);
          tic = finishRangeUndoTransaction(tic);
          retvalList.add(tic);
        }
      }
    }
    
    //
    // Do the entry:
    //
    
    TemporalRange range = getRange(oldName); 
    if (range != null) {
      TemporalInputChange tic = startRangeUndoTransaction(oldName);
      range.setName(newName);
      tic = finishRangeUndoTransaction(tic);
      retvalList.add(tic);
    }    
    
    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = trEntryMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<String> keys = trEntryMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.entryMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.entryMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }
    
    //
    // Fix any custom source maps
    // 
    
    dmit = trSourceMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<String> keys = trSourceMap_.get(mkey);
      if (DataUtil.containsKey(keys, oldName)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.mapKey = mkey;
        retval.sourceMapListOrig = new ArrayList<String>(keys);
        keys.remove(oldName);
        keys.add(newName);
        retval.sourceMapListNew = new ArrayList<String>(keys);
        retvalList.add(retval);
      }
    }

    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** We are changing the name of a region, and have been told to modify the
  ** data as needed.  We need to change the regions.  
  */
  
  public TemporalInputChange[] changeRegionName(String oldName, String newName) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    
    //
    // Do all the rows in the entries:
    //
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        Iterator<RegionAndRange> rit = itr.getRanges();
        while (rit.hasNext()) {
          RegionAndRange rar = rit.next();
          boolean regionMatch = (rar.getRegion() != null) && DataUtil.keysEqual(rar.getRegion(), oldName);
          boolean sourceMatch = (rar.getRestrictedSource() != null) && DataUtil.keysEqual(rar.getRestrictedSource(), oldName);
          if (regionMatch || sourceMatch) {
            TemporalInputChange tic = startRangeUndoTransaction(tr.getName());
            if (regionMatch) {
              rar.setRegion(newName);
            }
            if (sourceMatch) {
              rar.setRestrictedSource(newName);
            }
            tic = finishRangeUndoTransaction(tic);
            retvalList.add(tic);
          }
        }
      }
    }

    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = groupMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<GroupUsage> currentMap = groupMap_.get(mkey);
      List<GroupUsage> newMap = new ArrayList<GroupUsage>();
      boolean haveChange = false;
      int csize = currentMap.size();
      for (int i = 0; i < csize; i++) {
        GroupUsage gu = currentMap.get(i);
        GroupUsage newgu  = new GroupUsage(gu);
        if (DataUtil.keysEqual(gu.mappedGroup, oldName)) {
          haveChange = true;
          newgu.mappedGroup = newName;
        }
        newMap.add(newgu);
      }
      if (haveChange) {
        TemporalInputChange tic = new TemporalInputChange();
        retvalList.add(tic);
        tic.mapKey = mkey;
        tic.groupMapListOrig = deepCopyGroupMap(currentMap);
        groupMap_.put(mkey, newMap);
        tic.groupMapListNew = deepCopyGroupMap(newMap);      
      }  
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
 
  /***************************************************************************
  **
  ** Return a list of the regions 
  */
  
  public Set<String> getRegions() {
    HashSet<String> retval = new HashSet<String>();
    
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange tr = entries_.get(i);
      Iterator<InputTimeRange> perts = tr.getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange itr = perts.next();
        Iterator<RegionAndRange> rit = itr.getRanges();
        while (rit.hasNext()) {
          RegionAndRange rar = rit.next();
          String region = rar.getRegion();
          if (region != null) {
            retval.add(region);
          }
        }
      }
    }    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Drop nth entry 
  */
  
  public TemporalInputChange dropEntry(int n) {
    TemporalInputChange retval = new TemporalInputChange();
    TemporalRange entry = entries_.get(n);
    retval.eOrig = new TemporalRange(entry);
    retval.eNew = null;
    retval.entryPos = n;
    entries_.remove(n);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the entry
  */
  
  public TemporalInputChange dropEntry(String entryID) {
    int size = entries_.size();
    for (int i = 0; i < size; i++) {
      TemporalRange entry = entries_.get(i);
      if (entry.getName().equals(entryID)) {
        TemporalInputChange retval = new TemporalInputChange();
        retval.eOrig = new TemporalRange(entry);
        retval.eNew = null;
        retval.entryPos = i;
        entries_.remove(i);
        return (retval);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Undo an entry change
  */
  
  private void entryChangeUndo(TemporalInputChange undo) {
    if ((undo.eOrig != null) && (undo.eNew != null)) {
      entries_.set(undo.entryPos, undo.eOrig);
    } else if (undo.eOrig == null) {
      entries_.remove(undo.entryPos);
    } else {
      entries_.add(undo.entryPos, undo.eOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an entry change
  */
  
  private void entryChangeRedo(TemporalInputChange undo) {
    if ((undo.eOrig != null) && (undo.eNew != null)) {
      entries_.set(undo.entryPos, undo.eNew);
    } else if (undo.eNew == null) {
      entries_.remove(undo.entryPos);
    } else {
      entries_.add(undo.entryPos, undo.eNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the temporal range for the name.
  */
  
  public TemporalRange getRange(String targetName) {
    targetName = targetName.replaceAll(" ", "");
    Iterator<TemporalRange> trgit = entries_.iterator();  // FIX ME: use a hash map
    while (trgit.hasNext()) {
      TemporalRange trg = trgit.next();
      String name = trg.getName().replaceAll(" ", "");
      if (name.equalsIgnoreCase(targetName)) {
        return (trg);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the set of "interesting times" (Integers)
  **
  */
  
  public Set<Integer> getInterestingTimes() {
    HashSet<Integer> retval = new HashSet<Integer>();
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      tr.getInterestingTimes(retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add source and target maps
  */

  public TemporalInputChange[] addTemporalInputRangeMaps(String key, List<String> entries, List<String> sources) {
    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    TemporalInputChange retval;
    if ((entries != null) && (entries.size() > 0)) {
      retval = new TemporalInputChange();
      retval.mapKey = key;
      retval.entryMapListNew = new ArrayList<String>(entries);
      List<String> orig = trEntryMap_.get(key);
      retval.entryMapListOrig = (orig == null) ? null : new ArrayList<String>(orig);
      trEntryMap_.put(key, entries);
      retvalList.add(retval);
    }
    if ((sources != null) && (sources.size() > 0)) {    
      retval = new TemporalInputChange(); 
      retval.mapKey = key;    
      retval.sourceMapListNew = new ArrayList<String>(sources);
      List<String> orig = trSourceMap_.get(key);
      retval.sourceMapListOrig = (orig == null) ? null : new ArrayList<String>(orig);
      trSourceMap_.put(key, sources);
      retvalList.add(retval);
    }
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));    
  }  

  /***************************************************************************
  **
  ** Add combined source and target maps
  */
  
  public void addCombinedTemporalInputRangeMaps(String key, List<TirMapResult> mapSets) {
    ArrayList<String> sourceList = new ArrayList<String>();
    ArrayList<String> entryList = new ArrayList<String>();
    Iterator<TirMapResult> mit = mapSets.iterator();
    while (mit.hasNext()) {
      TirMapResult res = mit.next();
      if (res.type == TirMapResult.ENTRY_MAP) {
        entryList.add(res.name);
      } else {
        sourceList.add(res.name);
      }
    }
    if (entryList.size() > 0) {
      trEntryMap_.put(key, entryList);
    }
    if (sourceList.size() > 0) {
      trSourceMap_.put(key, sourceList);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get the list of entry names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeEntryKeysWithDefault(String nodeId) {
    List<String> retval = trEntryMap_.get(nodeId);
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
  ** Get the list of entry names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeEntryKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<String> retval = trEntryMap_.get(nodeId);
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
  ** Get the list of entry names for the node ID.  May be null.
  */
  
  public List<String> getCustomTemporalInputRangeEntryKeys(String nodeId) {
    return (trEntryMap_.get(nodeId));
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeSourceKeysWithDefault(String nodeId) {
    List<String> retval = trSourceMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      String defMap = getTemporalInputRangeDefaultMap(nodeId);
      if (defMap == null) {
        return (retval);
      }
      retval.add(defMap);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the default target name for the node ID.  May be null.
  */
  
  public String getTemporalInputRangeDefaultMap(String nodeId) {
    Node node = appState_.getDB().getGenome().getNode(nodeId);      
    if (node == null) { // for when node has been already deleted...
      throw new IllegalStateException();
    }      
    String nodeName = node.getRootName();
    if ((nodeName == null) || (nodeName.trim().equals(""))) {
      return (null);
    } 
    return (nodeName);
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the node ID.  May be empty.
  */
  
  public List<String> getTemporalInputRangeSourceKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<String> retval = trSourceMap_.get(nodeId);
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
  ** Get the list of source names for the node ID.  May be null.
  */
  
  public List<String> getCustomTemporalInputRangeSourceKeys(String nodeId) {
    return (trSourceMap_.get(nodeId));
  }  

  /***************************************************************************
  **
  ** Delete the key mapping
  */

  public TemporalInputChange dropDataEntryKeys(String geneId) {
    if (trEntryMap_.get(geneId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = geneId;
    retval.entryMapListNew = null;
    // FIX ME!!!! Clone
    retval.entryMapListOrig = trEntryMap_.remove(geneId);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */

  public TemporalInputChange dropDataSourceKeys(String geneId) {
    if (trSourceMap_.get(geneId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = geneId;
    retval.sourceMapListNew = null;
    // FIX ME!!!! Clone
    retval.sourceMapListOrig = trSourceMap_.remove(geneId);
    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void sourceMapChangeUndo(TemporalInputChange undo) {
    if ((undo.sourceMapListOrig != null) && (undo.sourceMapListNew != null)) {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListOrig);
    } else if (undo.sourceMapListOrig == null) {
      trSourceMap_.remove(undo.mapKey);
    } else {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void sourceMapChangeRedo(TemporalInputChange undo) {
    if ((undo.sourceMapListOrig != null) && (undo.sourceMapListNew != null)) {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListNew);
    } else if (undo.sourceMapListNew == null) {
      trSourceMap_.remove(undo.mapKey);
    } else {
      trSourceMap_.put(undo.mapKey, undo.sourceMapListNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void entryMapChangeUndo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) && (undo.entryMapListNew != null)) {
      trEntryMap_.put(undo.mapKey, undo.entryMapListOrig);
    } else if (undo.entryMapListOrig == null) {
      trEntryMap_.remove(undo.mapKey);
    } else {
      trEntryMap_.put(undo.mapKey, undo.entryMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void entryMapChangeRedo(TemporalInputChange undo) {
    if ((undo.entryMapListOrig != null) && (undo.entryMapListNew != null)) {
      trEntryMap_.put(undo.mapKey, undo.entryMapListNew);
    } else if (undo.entryMapListNew == null) {
      trEntryMap_.remove(undo.mapKey);
    } else {
      trEntryMap_.put(undo.mapKey, undo.entryMapListNew);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Add a map from a group to a list of target groups
  */
  
  public TemporalInputChange setTemporalRangeGroupMap(String key, List<GroupUsage> mapSets) {
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = key;
    retval.groupMapListNew = deepCopyGroupMap(mapSets);
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.get(key));
    groupMap_.put(key, mapSets);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Duplicate a map for genome duplications
  */
  
  public TemporalInputChange copyTemporalRangeGroupMapForDuplicateGroup(String oldKey, String newKey, Map<String, String> modelMap) {
    List<GroupUsage> oldMapList = groupMap_.get(oldKey);
    if (oldMapList == null) {
      return (null);
    }
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = newKey;
    retval.groupMapListNew = deepCopyGroupMapMappingUsage(oldMapList, modelMap, oldKey.equals(newKey));
    retval.groupMapListOrig = null;
    groupMap_.put(newKey, deepCopyGroupMap(retval.groupMapListNew));
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the list of target names for the group.  May be empty for a group
  ** with no name.
  */
  
  public List<GroupUsage> getTemporalRangeGroupKeysWithDefault(String groupId, String groupName) {
    List<GroupUsage> retval = groupMap_.get(groupId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<GroupUsage>();
      if ((groupName == null) || (groupName.trim().equals(""))) {
        return (retval);
      }
      retval.add(new GroupUsage(groupName.toUpperCase().replaceAll(" ", ""), null));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be null.
  */
  
  public List<GroupUsage> getCustomTemporalRangeGroupKeys(String groupId) {
    return (groupMap_.get(groupId));
  }
  
  /***************************************************************************
  **
  ** Delete the group mapping
  */

  public TemporalInputChange dropGroupMap(String groupId) {
    if (groupMap_.get(groupId) == null) {
      return (null);
    }    
    TemporalInputChange retval = new TemporalInputChange();
    retval.mapKey = groupId;
    retval.groupMapListNew = null;
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.remove(groupId));
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get an iterator over all the group map keys
  */

  public Iterator<String> getGroupMapKeys() {
    return (groupMap_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** Delete the group mappings referencing the given proxy
  */

  public TemporalInputChange[] dropGroupMapsForProxy(String proxyId) {

    ArrayList<TemporalInputChange> retvalList = new ArrayList<TemporalInputChange>();
    Iterator<String> gmit = new HashSet<String>(groupMap_.keySet()).iterator();
    while (gmit.hasNext()) {
      String key = gmit.next();
      List<GroupUsage> currentMap = groupMap_.get(key);
      int mSize = currentMap.size();
      ArrayList<GroupUsage> newMap = new ArrayList<GroupUsage>();
      for (int i = 0; i < mSize; i++) {
        GroupUsage gu = currentMap.get(i);
        if ((gu.usage == null) || (!gu.usage.equals(proxyId))) {
          newMap.add(gu);
        }
      }
      if (newMap.size() < mSize) {
        TemporalInputChange tic = new TemporalInputChange();      
        retvalList.add(tic);
        tic.mapKey = key;
        tic.groupMapListOrig = deepCopyGroupMap(currentMap);
        if (newMap.size() > 0) {
          groupMap_.put(key, newMap);
          tic.groupMapListNew = deepCopyGroupMap(newMap);        
        } else {
          groupMap_.remove(key);
          tic.groupMapListNew = null;
        }
      }
    }
    
    return (retvalList.toArray(new TemporalInputChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** Deep copy a group mapping
  */
  
  private List<GroupUsage> deepCopyGroupMap(List<GroupUsage> oldMap) {
    if (oldMap == null) {
      return (null);
    }
    ArrayList<GroupUsage> retval = new ArrayList<GroupUsage>();
    int size = oldMap.size();
    for (int i = 0; i < size; i++) {
      retval.add(oldMap.get(i).clone());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Deep copy a group mapping, mapping model usage as we go
  */
  
  private List<GroupUsage> deepCopyGroupMapMappingUsage(List<GroupUsage> oldMap, Map<String, String> modelMaps, boolean append) {
    if (oldMap == null) {
      return (null);
    }
    ArrayList<GroupUsage> retval = new ArrayList<GroupUsage>();
    int size = oldMap.size();
    for (int i = 0; i < size; i++) {
      GroupUsage copied = oldMap.get(i).clone();
      String modelID = modelMaps.get(copied.usage);
      if (modelID != null) {
        if (append) {
          retval.add(copied);
          copied = copied.clone();
        }
        copied.usage = modelID;
      }
      retval.add(copied);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void groupMapChangeUndo(TemporalInputChange undo) {
    if ((undo.groupMapListOrig != null) && (undo.groupMapListNew != null)) {
      groupMap_.put(undo.mapKey, undo.groupMapListOrig);
    } else if (undo.groupMapListOrig == null) {
      groupMap_.remove(undo.mapKey);
    } else {
      groupMap_.put(undo.mapKey, undo.groupMapListOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void groupMapChangeRedo(TemporalInputChange undo) {
    if ((undo.groupMapListOrig != null) && (undo.groupMapListNew != null)) {
      groupMap_.put(undo.mapKey, undo.groupMapListNew);
    } else if (undo.groupMapListNew == null) {
      groupMap_.remove(undo.mapKey);
    } else {
      groupMap_.put(undo.mapKey, undo.groupMapListNew);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of all regions used in the range data.
  */
  
  public Set<String> getAllRegions() {
    TreeSet<String> retval = new TreeSet<String>();
    //
    // Get regions present in entries, but maybe not mapped:
    //
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      tr.getAllRegions(retval);
    }
    //
    // Get regions in maps, but maybe not in entries:
    
    Iterator<List<GroupUsage>> gmit = groupMap_.values().iterator();
    while (gmit.hasNext()) {
      List<GroupUsage> groupsUsed = gmit.next();
      Iterator<GroupUsage> guit = groupsUsed.iterator();
      while (guit.hasNext()) {
        GroupUsage groupUse = guit.next();
        retval.add(groupUse.mappedGroup);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the set of all current map targets
  */
  
  public Set<String> getAllIdentifiers() {
    TreeSet<String> retval = new TreeSet<String>();
    //
    // Get regions present in entries, but maybe not mapped:
    //
    Iterator<TemporalRange> eit = getEntries();
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      retval.add(tr.getName());
      Iterator<InputTimeRange> pit = tr.getTimeRanges();
      while (pit.hasNext()) {
        InputTimeRange itr = pit.next();
        retval.add(itr.getName());
      }
    }
    //
    // Get targets in maps:
    //
    Iterator<List<String>> tmit = trEntryMap_.values().iterator();
    while (tmit.hasNext()) {
      List<String> idsUsed = tmit.next();
      retval.addAll(idsUsed);
    }
    tmit = trSourceMap_.values().iterator();
    while (tmit.hasNext()) {
      List<String> idsUsed = tmit.next();
      retval.addAll(idsUsed);
    }    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answers if a range name is unique
  */
  
  public boolean nameIsUnique(String targName) {
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        return (false);
      }
    // FIX ME ???
    }  
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if a range name shows up NO MORE THAN ONCE
  */
  
  public boolean nameAppearsZeroOrOne(String targName) {
    int hitCount = 0;
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        if (++hitCount > 1) {
          return (false);
        }
      }
    }  
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if a range name shows up ONCE
  */
  
  public boolean nameAppearsOnce(String targName) {
    int hitCount = 0;
    Iterator<TemporalRange> eit = getEntries();  
    while (eit.hasNext()) {
      TemporalRange tr = eit.next();
      if (DataUtil.keysEqual(tr.getName(), targName)) {
        if (++hitCount > 1) {
          return (false);
        }
      }
    }  
    return (hitCount != 0);
  }
  
  /***************************************************************************
  **
  ** Write the Temporal Input Range Data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<TemporalInputRangeData>");
    if (entries_.size() > 0) {
      Iterator<TemporalRange> eit = getEntries();
      ind.up();    
      while (eit.hasNext()) {
        TemporalRange tr = eit.next();
        tr.writeXML(out, ind);
      }
      ind.down(); 
    }
    if ((trEntryMap_.size() > 0) || (trSourceMap_.size() > 0)) {
      writeTrMap(out, ind);
    } 
    if (groupMap_.size() > 0) {
      writeGroupMap(out, ind);
    }    
    ind.indent();
    out.println("</TemporalInputRangeData>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get an HTML inputs table suitable for display.
  */
  
  public String getInputsTable(String targetName, Set<String> srcNames) {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);    
    Iterator<TemporalRange> trit = entries_.iterator();  // FIX ME: use a hash map
    while (trit.hasNext()) {
      TemporalRange tr = trit.next();
      String name = tr.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        if (!tr.isInternalOnly()) {
          tr.getInputsTable(appState_, out, srcNames);
        }
      }
    }
    return (sw.toString());
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("TemporalInputRangeData: " + " entries = " + entries_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to return QPCR map results
  **
  */
  
  public static class TirMapResult {
    public String name;
    public int type;
    
    public static final int ENTRY_MAP  = 0;
    public static final int SOURCE_MAP = 1;

    public TirMapResult(String name, int type) {
      this.name = name;
      this.type = type;
    }
  }  

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
    retval.add("TemporalInputRangeData");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  @SuppressWarnings("unused")
  public static TemporalInputRangeData buildFromXML(BTState appState, String elemName, 
                                                    Attributes attrs) throws IOException {
    if (!elemName.equals("TemporalInputRangeData")) {
      return (null);
    }
    
    return (new TemporalInputRangeData(appState));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractTrMapKey(String elemName, 
                                     Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "trMap", "key", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static TirMapResult extractUseTr(String elemName, 
                                          Attributes attrs) throws IOException {
    String name = AttributeExtractor.extractAttribute(
                         elemName, attrs, "useTr", "name", true);
    String type = AttributeExtractor.extractAttribute(
                   elemName, attrs, "useTr", "type", true);
    int mapType = (type.equals("entry")) ? TirMapResult.ENTRY_MAP : TirMapResult.SOURCE_MAP;
    return (new TirMapResult(name, mapType));   
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractGroupMapKey(String elemName, 
                                     Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "trGroupMap", "key", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static GroupUsage extractUseGroup(String elemName, 
                                           Attributes attrs) throws IOException {
    String mappedGroup = AttributeExtractor.extractAttribute(
                         elemName, attrs, "trUseGroup", "name", true);
    String usage = AttributeExtractor.extractAttribute(
                   elemName, attrs, "trUseGroup", "useFor", false);    
    return (new GroupUsage(mappedGroup, usage));
  }  
  
  /***************************************************************************
  **
  ** Return the trMap keyword
  **
  */
  
  public static String trMapKeyword() {
    return ("trMap");
  }
  
  /***************************************************************************
  **
  ** Return the usetr keyword
  **
  */
  
  public static String useTrKeyword() {
    return ("useTr");
  }

  /***************************************************************************
  **
  ** Return the groupmap keyword
  **
  */
  
  public static String groupMapKeyword() {
    return ("trGroupMap");
  }
  
  /***************************************************************************
  **
  ** Return the useGroup keyword
  **
  */
  
  public static String useGroupKeyword() {
    return ("trUseGroup");
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the trmap to XML
  **
  */
  
  private void writeTrMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<trMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(trEntryMap_.keySet());
    sorted.addAll(trSourceMap_.keySet());    
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<String> elist = trEntryMap_.get(key);
      List<String> slist = trSourceMap_.get(key);
      ind.indent();
      out.print("<trMap key=\"");
      out.print(key);
      out.println("\">");
      ind.up();
      if (elist != null) {
        Iterator<String> lit = elist.iterator();
        while (lit.hasNext()) {
          String useTr = lit.next();      
          ind.indent();
          out.print("<useTr type=\"entry\" name=\"");
          out.print(useTr);
          out.println("\"/>");
        }
      }
      if (slist != null) {
        Iterator<String> lit = slist.iterator(); 
        while (lit.hasNext()) {
          String useTr = lit.next();      
          ind.indent();
          out.print("<useTr type=\"source\" name=\"");
          out.print(useTr);
          out.println("\"/>");
        }
      }
      ind.down().indent(); 
      out.println("</trMap>");
    }
    ind.down().indent(); 
    out.println("</trMaps>");
    return;
  }  

  /***************************************************************************
  **
  ** Write the group map to XML
  **
  */
  
  private void writeGroupMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<trGroupMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(groupMap_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<GroupUsage> list = groupMap_.get(key);
      ind.indent();
      out.print("<trGroupMap key=\"");
      out.print(key);
      out.println("\">");
      Iterator<GroupUsage> lit = list.iterator();
      ind.up();
      while (lit.hasNext()) {
        GroupUsage usegr = lit.next();      
        ind.indent();
        out.print("<trUseGroup name=\"");
        out.print(usegr.mappedGroup);
        if (usegr.usage != null) {
          out.print("\" useFor=\""); 
          out.print(usegr.usage);          
        }
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</trGroupMap>");
    }
    ind.down().indent(); 
    out.println("</trGroupMaps>");
    return;
  }
}
