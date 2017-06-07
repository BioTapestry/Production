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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;

import org.xml.sax.Attributes;

/****************************************************************************
**
** This holds TimeCourseDataMaps (per database)
*/

public class TimeCourseDataMaps implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int NO_SLICE         = -1;
  public static final int SLICE_BY_TIMES   = 0;
  public static final int SLICE_BY_REGIONS = 1;
  
  public static final String SLICE_BY_TIMES_TAG   = "byTimes";
  public static final String SLICE_BY_REGIONS_TAG = "byRegions";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, List<TCMapping>> tcMap_;
  private HashMap<String, List<GroupUsage>> groupMap_;
  private long mapSerialNumber_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseDataMaps(DataAccessContext dacx) {
    dacx_ = dacx;
    tcMap_ = new HashMap<String, List<TCMapping>>();
    groupMap_ = new HashMap<String, List<GroupUsage>>();
    mapSerialNumber_ = 0L;
  }
  
  /***************************************************************************
  **
  ** Constructor for legacy loads
  */

  public TimeCourseDataMaps(DataAccessContext dacx, long serialNum) {
    dacx_ = dacx;
    tcMap_ = new HashMap<String, List<TCMapping>>();
    groupMap_ = new HashMap<String, List<GroupUsage>>();
    mapSerialNumber_ = serialNum;
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

  @Override
  public TimeCourseDataMaps clone() {
    try {
      TimeCourseDataMaps retval = (TimeCourseDataMaps)super.clone();  
      retval.tcMap_ = getTCMapCopyGuts();     
      retval.groupMap_ = getGroupMapCopyGuts();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  **  Get a copy of the TC map
  */

  public Map<String, List<TCMapping>> getTCMapCopy() {
    return (getTCMapCopyGuts());
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  private HashMap<String, List<TCMapping>> getTCMapCopyGuts() {
   
    HashMap<String, List<TCMapping>> retval = new HashMap<String, List<TCMapping>>();
    Iterator<String> tcmit = this.tcMap_.keySet().iterator();
    while (tcmit.hasNext()) {
      String mapKey = tcmit.next();
      List<TCMapping> targList = tcMap_.get(mapKey);
      ArrayList<TCMapping> retTargList = new ArrayList<TCMapping>();       
      retval.put(mapKey, retTargList);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        TCMapping targ = targList.get(i);
        retTargList.add(targ.clone());        
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  **  Get a copy of the group map
  */

  public Map<String, List<GroupUsage>> getGroupMapCopy() {
    return (getGroupMapCopyGuts());
  }

  /***************************************************************************
  **
  **  Get a copy of the group map
  */

  private HashMap<String, List<GroupUsage>> getGroupMapCopyGuts() {
    HashMap<String, List<GroupUsage>> retval = new HashMap<String, List<GroupUsage>>();
    Iterator<String> gmkit = this.groupMap_.keySet().iterator();
    while (gmkit.hasNext()) {
      String key = gmkit.next();
      List<GroupUsage> mapList = groupMap_.get(key);
      ArrayList<GroupUsage> retGroupList = new ArrayList<GroupUsage>();       
      retval.put(key, retGroupList);
      int mlsize = mapList.size();
      for (int i = 0; i < mlsize; i++) {
        GroupUsage gu = mapList.get(i);
        retGroupList.add(gu.clone());     
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Return the serialNumber
  */
  
  public long getSerialNumber() {
    return (mapSerialNumber_);
  } 
  
  /***************************************************************************
  **
  ** Answer if we have data
  */
  
  public boolean haveData() {
    if (!tcMap_.isEmpty()) {
      return (true);
    }
    if (!groupMap_.isEmpty()) {
      return (true);
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answers if we have a time course region mapping
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
  ** Answers if we have a time course mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    List<TCMapping> mapped = tcMap_.get(nodeID);
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
  
  public boolean haveDataForNode(TimeCourseData tcd, String nodeID, GenomeSource gs) {
    if (!haveData()) {
      return (false);
    }
    List<TCMapping> mapped = getTimeCourseTCMDataKeysWithDefault(nodeID, gs);
    if (mapped == null) {
      return (false);
    }
    Iterator<TCMapping> mit = mapped.iterator();
    while (mit.hasNext()) {
      TCMapping tcm = mit.next();
      if (tcd.getTimeCourseData(tcm.name) != null) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if all the time course data we have is internal only
  **
  */
  
  public boolean isAllInternalForNode(TimeCourseData tcd, String nodeID, GenomeSource gs) {
    if (!haveData()) {
      return (false);
    }
    List<TCMapping> mapped = getTimeCourseTCMDataKeysWithDefault(nodeID, gs);
    if (mapped == null) {
      return (false);
    }
    Iterator<TCMapping> mit = mapped.iterator();
    boolean retval = false;
    while (mit.hasNext()) {
      retval = true;
      TCMapping tcm = mit.next();
      TimeCourseGene tcg = tcd.getTimeCourseData(tcm.name);
      if (tcg == null) {
        continue;
      }
      if (!tcg.isInternalOnly()) {
        return (false);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNodeOrName(TimeCourseData tcd, String nodeID, String deadName) {
    if (tcd == null) {
      return (false);
    }
    if (!haveData()) {
      return (false);
    }
    List<TCMapping> mapped = getTimeCourseTCMDataKeysWithDefaultGivenName(nodeID, deadName);
    if (mapped == null) {
      return (false);
    }
    Iterator<TCMapping> mit = mapped.iterator();
    while (mit.hasNext()) {
      TCMapping tcm = mit.next();
      if (tcd.getTimeCourseData(tcm.name) != null) {
        return (true);
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(TimeCourseChange undo) {
    if ((undo.mapListOrig != null) || (undo.mapListNew != null)) {
      mapChangeUndo(undo);
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeUndo(undo);
    }
    if (undo.mapSerialNumberOrig != -1L) {
      mapSerialNumber_ = undo.mapSerialNumberOrig;
    } else {
      throw new IllegalStateException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(TimeCourseChange undo) {
    if ((undo.mapListOrig != null) || (undo.mapListNew != null)) {
      mapChangeRedo(undo);
    } else if ((undo.groupMapListOrig != null) || (undo.groupMapListNew != null)) {
      groupMapChangeRedo(undo);
    }
    if (undo.mapSerialNumberNew != -1L) {
      mapSerialNumber_ = undo.mapSerialNumberNew;
    } else {
      throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drop maps that resolve to the given time course entry SOURCE CHANNEL
  */
  
  public TimeCourseChange[] dropMapsToEntrySourceChannel(String nodeID, ExpressionEntry.Source channel) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    Iterator<String> mit = new HashSet<String>(tcMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<TCMapping> targList = tcMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        TCMapping targ = targList.get(i);
        if (DataUtil.keysEqual(targ.name, nodeID) && (targ.channel == channel)) {
          TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
          tcc.mapKey = mapKey;
          tcc.mapListOrig = TCMapping.cloneAList(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tcc.mapListNew = null;
            tcMap_.remove(mapKey);
          } else {
            tcc.mapListNew = TCMapping.cloneAList(targList);
          }
          tcc.mapSerialNumberNew = ++mapSerialNumber_;
          retvalList.add(tcc);
          break;
        }
      }
    }
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }    
 
  /***************************************************************************
  **
  ** Answer if we have a map to the given source channel on a node:
  */
  
  public boolean haveMapsToEntrySourceChannel(String nodeID, ExpressionEntry.Source channel) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    Iterator<String> mit = tcMap_.keySet().iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<TCMapping> targList = tcMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        TCMapping targ = targList.get(i);
        if (DataUtil.keysEqual(targ.name, nodeID) && (targ.channel == channel)) {
          return (true);
        }
      }
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Drop maps that resolve to the given time course entry.
  */
  
  public TimeCourseChange[] dropMapsTo(String nodeID) {
    //
    // Crank through the maps and look for entries that target the 
    // given entry.  Delete it.
    //
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    Iterator<String> mit = new HashSet<String>(tcMap_.keySet()).iterator();
    while (mit.hasNext()) {
      String mapKey = mit.next();
      List<TCMapping> targList = tcMap_.get(mapKey);
      int tlSize = targList.size();
      for (int i = 0; i < tlSize; i++) {
        TCMapping targ = targList.get(i);
        if (DataUtil.keysEqual(targ.name, nodeID)) {
          TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
          tcc.mapKey = mapKey;
          tcc.mapListOrig = TCMapping.cloneAList(targList);
          targList.remove(i);
          if (targList.isEmpty()) {
            tcc.mapListNew = null;
            tcMap_.remove(mapKey);
          } else {
            tcc.mapListNew = TCMapping.cloneAList(targList);
          }
          tcc.mapSerialNumberNew = ++mapSerialNumber_;
          retvalList.add(tcc);
          break;
        }
      }
    }
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }    
 
  /***************************************************************************
  **
  ** Delete the entire mapping set
  */
  
  public TimeCourseChange[] clearAllDataKeys(boolean bumpSerial) {
    ArrayList<TimeCourseChange> retval = new ArrayList<TimeCourseChange>();
    Iterator<String> ksit = new HashSet<String>(tcMap_.keySet()).iterator();  
    while (ksit.hasNext()) {
      String id = ksit.next(); 
      TimeCourseChange tcc = coreDropDataKeys(id, bumpSerial);
      retval.add(tcc);
    }  
    return (retval.toArray(new TimeCourseChange[retval.size()])); 
    
  }

  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public TimeCourseChange dropDataKeys(String geneId) {
    return (coreDropDataKeys(geneId, true));
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  private TimeCourseChange coreDropDataKeys(String geneId, boolean bumpSerial) {
    if (tcMap_.get(geneId) == null) {
      return (null);
    }    
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
    retval.mapKey = geneId;
    retval.mapListOrig = TCMapping.cloneAList(tcMap_.get(geneId));
    tcMap_.remove(geneId);
    retval.mapListNew = null;
    if (bumpSerial) {
      ++mapSerialNumber_;
    }
    retval.mapSerialNumberNew = mapSerialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void mapChangeUndo(TimeCourseChange undo) {
    if ((undo.mapListOrig != null) && (undo.mapListNew != null)) {
      tcMap_.put(undo.mapKey, TCMapping.cloneAList(undo.mapListOrig));
    } else if (undo.mapListOrig == null) {
      tcMap_.remove(undo.mapKey);
    } else {
      tcMap_.put(undo.mapKey, TCMapping.cloneAList(undo.mapListOrig));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a map change
  */
  
  private void mapChangeRedo(TimeCourseChange undo) {
    if ((undo.mapListOrig != null) && (undo.mapListNew != null)) {
      tcMap_.put(undo.mapKey, TCMapping.cloneAList(undo.mapListNew));
    } else if (undo.mapListNew == null) {
      tcMap_.remove(undo.mapKey);
    } else {
      tcMap_.put(undo.mapKey, TCMapping.cloneAList(undo.mapListNew));
    }
    return;
  }  


  /***************************************************************************
  **
  ** As the table template is changed, we need to make sure we don't have
  ** dangling maps.
  */
  
  public Set<String> getDanglingRegionMaps(ArrayList<GeneTemplateEntry> newTemplate) {
    HashSet<String> haveRegions = new HashSet<String>();
    int size = newTemplate.size();
    for (int i = 0; i < size; i++) {
      GeneTemplateEntry entry = newTemplate.get(i);
      haveRegions.add(entry.region);
    }
    
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> gmkit = groupMap_.keySet().iterator();
    while (gmkit.hasNext()) {
      String key = gmkit.next();
      List<GroupUsage> mapList = groupMap_.get(key);
      int mlsize = mapList.size();
      for (int i = 0; i < mlsize; i++) {
        GroupUsage gu = mapList.get(i);
        if (!haveRegions.contains(gu.mappedGroup)) {
          retval.add(key);
        }
      }
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** As the table template is changed, we need to make sure we don't have
  ** dangling maps.  This repairs dangling maps.
  */
  
  public TimeCourseChange[] repairDanglingRegionMaps(Set<String> badMapKeys, ArrayList<GeneTemplateEntry> newTemplate) {

    //
    // Figure out what we have
    //
                                                       
    HashSet<String> haveRegions = new HashSet<String>();
    int size = newTemplate.size();
    for (int i = 0; i < size; i++) {
      GeneTemplateEntry entry = newTemplate.get(i);
      haveRegions.add(entry.region);
    }                                                   
                                                                                                          
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    Iterator<String> bmkit = badMapKeys.iterator();
    while (bmkit.hasNext()) {
      String key = bmkit.next();
      TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
      retvalList.add(tcc);
      tcc.mapKey = key;
      List<GroupUsage> currentMap = groupMap_.get(key);
      tcc.groupMapListOrig = deepCopyGroupMap(currentMap);
      int mSize = currentMap.size();
      ArrayList<GroupUsage> newMap = new ArrayList<GroupUsage>();
      for (int i = 0; i < mSize; i++) {
        GroupUsage gu = currentMap.get(i);
        if (haveRegions.contains(gu.mappedGroup)) {
          newMap.add(gu);
        }
      }
      if (newMap.size() > 0) {
        groupMap_.put(key, newMap);
        tcc.groupMapListNew = deepCopyGroupMap(newMap);        
      } else {
        groupMap_.remove(key);
        tcc.groupMapListNew = null;
      }
      tcc.mapSerialNumberNew = ++mapSerialNumber_;
    }
    
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }  
 
    
  /***************************************************************************
  **
  ** Add a map from a node to a List of target nodes
  */
  
  public TimeCourseChange addTimeCourseTCMMap(String key, List<TCMapping> mapSets, boolean bumpSerial) {
    TimeCourseChange retval = null;
    if ((mapSets != null) && (mapSets.size() > 0)) {
      retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
      retval.mapKey = key;
      retval.mapListNew = TCMapping.cloneAList(mapSets);
      retval.mapListOrig = null;
      retval.mapSerialNumberNew = (bumpSerial) ? ++mapSerialNumber_ : mapSerialNumber_;
      tcMap_.put(key, mapSets);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** When changing an entry name, we need to change one or more maps to keep
  ** them consistent.  Do this operation. Return empty array if no changes occurred.
  */
  
  public TimeCourseChange[] changeTimeCourseMapsToName(String oldName, String newName) {  
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    Iterator<String> tmit = tcMap_.keySet().iterator();
    while (tmit.hasNext()) {
      String mkey = tmit.next();
      List<TCMapping> keys = tcMap_.get(mkey);
      int numKeys = keys.size();
      for (int i = 0; i < numKeys; i++) {
        TCMapping tcm = keys.get(i);
        if (DataUtil.keysEqual(tcm.name, oldName)) {
          TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
          retval.mapKey = mkey;
          retval.mapListOrig = TCMapping.cloneAList(keys);
          tcm.name = newName;
          retval.mapListNew = TCMapping.cloneAList(keys);
          retval.mapSerialNumberNew = ++mapSerialNumber_;
          retvalList.add(retval);
        }
      }
    }
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** We are changing the name of a region, and have been told to modify the
  ** data as needed.  We need to change the regions.  
  */
  
  public TimeCourseChange[] changeRegionNameForMaps(String oldName, String newName) {
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    
    //
    // Fix any custom entry maps
    //
    
    Iterator<String> dmit = groupMap_.keySet().iterator();
    while (dmit.hasNext()) {
      String mkey = dmit.next();
      List<GroupUsage> currentMap = groupMap_.get(mkey);
      List<GroupUsage> newMap = new ArrayList<GroupUsage>();
      boolean haveChange = false;
      int size = currentMap.size();
      for (int i = 0; i < size; i++) {
        GroupUsage gu = currentMap.get(i);
        GroupUsage newgu  = new GroupUsage(gu);
        if (DataUtil.keysEqual(gu.mappedGroup, oldName)) {
          haveChange = true;
          newgu.mappedGroup = newName;
        }
        newMap.add(newgu);
      }
      if (haveChange) {
        TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
        retvalList.add(tcc);
        tcc.mapKey = mkey;
        tcc.groupMapListOrig = deepCopyGroupMap(currentMap);
        groupMap_.put(mkey, newMap);
        tcc.groupMapListNew = deepCopyGroupMap(newMap); 
        tcc.mapSerialNumberNew = ++mapSerialNumber_;
      }  
    }
   
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  } 
  
  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be empty.
  */
  
  public List<TCMapping> getTimeCourseTCMDataKeysWithDefault(String nodeId, GenomeSource db) {
    List<TCMapping> retval = tcMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<TCMapping>();
      TCMapping defMap = getTimeCourseDefaultMap(nodeId, db);
      if (defMap != null) {
        retval.add(defMap);
      }
    }    
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the default target name for the node ID.  May be null.
  */
  
  public TCMapping getTimeCourseDefaultMap(String nodeId, GenomeSource db) {
    Node node = db.getRootDBGenome().getNode(nodeId);      
    if (node == null) { // for when node has been already deleted...
      throw new IllegalStateException();
    }      
    String nodeName = node.getRootName();
    if ((nodeName == null) || (nodeName.trim().equals(""))) {
      return (null);
    } 
    return (new TCMapping(nodeName));
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be empty.
  */
  
  public List<TCMapping> getTimeCourseTCMDataKeysWithDefaultGivenName(String nodeId, String nodeName) {
    List<TCMapping> retval = tcMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<TCMapping>();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(new TCMapping(nodeName));
    }    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the list of targets names for the node ID.  May be null
  */
  
  public List<TCMapping> getCustomTCMTimeCourseDataKeys(String nodeId) {
    return (tcMap_.get(nodeId));
  }
  
  /***************************************************************************
  **
  ** Get the set of nodeIDs that target the given name. May be empty, not null.
  */
  
  public Set<String> getTimeCourseDataKeyInverses(String name) {
    name = DataUtil.normKey(name);
    HashSet<String> retval = new HashSet<String>();
    //
    // If there is anybody out there with the same name and no custom map, it
    // will map by default:
    //
    
    DBGenome genome = dacx_.getDBGenome();
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

    Iterator<String> kit = tcMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<TCMapping> targs = tcMap_.get(key);
      if (TCMapping.nameInList(name, targs)) {
        retval.add(key);
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Add a map from a group to a target group
  */
  
  public TimeCourseChange setTimeCourseGroupMap(String key, List<GroupUsage> mapSets, boolean bumpSerial) {
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
    retval.mapKey = key;
    retval.groupMapListNew = deepCopyGroupMap(mapSets);
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.get(key));
    groupMap_.put(key, mapSets);
    retval.mapSerialNumberNew = (bumpSerial) ? ++mapSerialNumber_ : mapSerialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Duplicate a map for genome duplications
  */
  
  public TimeCourseChange copyTimeCourseGroupMapForDuplicateGroup(String oldKey, String newKey, Map<String, String> modelMap) {
    List<GroupUsage> oldMapList = groupMap_.get(oldKey);
    if (oldMapList == null) {
      return (null);
    }
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
    retval.mapKey = newKey;
    retval.groupMapListNew = deepCopyGroupMapMappingUsage(oldMapList, modelMap, oldKey.equals(newKey));
    retval.groupMapListOrig = null;
    groupMap_.put(newKey, deepCopyGroupMap(retval.groupMapListNew));
    retval.mapSerialNumberNew = ++mapSerialNumber_;
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
  ** Get the list of target names for the group.  May be empty for a group
  ** with no name.
  */
  
  public List<GroupUsage> getTimeCourseGroupKeysWithDefault(String groupId, String groupName) {
    List<GroupUsage> retval = groupMap_.get(groupId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<GroupUsage>();
      if ((groupName == null) || (groupName.trim().equals(""))) {        
        return (retval);
      }
      retval.add(new GroupUsage(DataUtil.normKey(groupName), null));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the list of target names for the group.  May be empty for a group
  ** with no name.
  */
  
  public List<GroupUsage> getCustomTimeCourseGroupKeys(String groupId) {
    return (groupMap_.get(groupId));
  }  
  
  /***************************************************************************
  **
  ** Get the set of groupIDs that target the given group. May be empty, not null.
  */
  
  public Set<String> getTimeCourseGroupKeyInverses(String name) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> kit = groupMap_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<GroupUsage> targs = groupMap_.get(key);
      Iterator<GroupUsage> trit = targs.iterator();
      while (trit.hasNext()) {
        GroupUsage usage = trit.next();
        if (usage.mappedGroup.equals(name)) {
          retval.add(key);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete the group mappings referencing the given proxy
  */

  public TimeCourseChange[] dropGroupMapsForProxy(String proxyId) {

    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
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
        TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);      
        retvalList.add(tcc);
        tcc.mapKey = key;
        tcc.groupMapListOrig = deepCopyGroupMap(currentMap);
        if (newMap.size() > 0) {
          groupMap_.put(key, newMap);
          tcc.groupMapListNew = deepCopyGroupMap(newMap);        
        } else {
          groupMap_.remove(key);
          tcc.groupMapListNew = null;
        }
        tcc.mapSerialNumberNew = ++mapSerialNumber_;
      }
    }
    
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }  
 
  /***************************************************************************
  **
  ** Delete the group mapping
  */

  public TimeCourseChange dropGroupMap(String groupId) {
    if (groupMap_.get(groupId) == null) {
      return (null);
    }    
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.MAP_SERIAL, mapSerialNumber_);
    retval.mapKey = groupId;
    retval.groupMapListNew = null;
    retval.groupMapListOrig = deepCopyGroupMap(groupMap_.remove(groupId));
    retval.mapSerialNumberNew = ++mapSerialNumber_;
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
  ** For debug: output all group maps
  */

  public void dumpGroupMaps(PrintStream out) {
    Iterator<String> kit = groupMap_.keySet().iterator();
    while (kit.hasNext()) {
      String giid = kit.next();
      out.print(giid);
      out.print(":");
      List<GroupUsage> gm = groupMap_.get(giid);
      if (gm != null) {
        out.println(gm);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Undo a map change
  */
  
  private void groupMapChangeUndo(TimeCourseChange undo) {
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
  
  private void groupMapChangeRedo(TimeCourseChange undo) {
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
  ** Write the Time Course Data Mapping to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<TimeCourseDataMaps");
    if (mapSerialNumber_ != 0L) {
      out.print(" mapSerialNum=\"");
      out.print(mapSerialNumber_);
      out.print("\"");
    }
    out.println(">");
   
    if (tcMap_.size() > 0) {
      writeTcMap(out, ind);
    } 
    if (groupMap_.size() > 0) {
      writeGroupMap(out, ind);
    } 
    ind.indent();
    out.println("</TimeCourseDataMaps>");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("TimeCourseDataMaps: " + " tcMap_ = " + tcMap_  + " groupMap_ = " + groupMap_);
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
  ** For XML I/O
  */  
      
  public static class TimeCourseDataMapsWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    private TimeCourseMapWorker tcmw_;
    private TimeCourseGroupMapWorker tcgmw_;
    private boolean serialNumberIsIllegal_;
    private long origSerialNumber_;
    
    public TimeCourseDataMapsWorker(FactoryWhiteboard whiteboard, boolean serialNumberIsIllegal) {
      super(whiteboard);
      serialNumberIsIllegal_ = serialNumberIsIllegal;
      myKeys_.add("TimeCourseDataMaps");
      tcmw_ = new TimeCourseMapWorker(whiteboard, false);  
      installWorker(tcmw_, null);
      tcgmw_ = new TimeCourseGroupMapWorker(whiteboard, false);
      installWorker(tcgmw_, null);
    }
    
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("TimeCourseDataMaps")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        if ((board.tcdm != null)  && (board.tcdm.getSerialNumber() != origSerialNumber_)) {
          throw new IOException();
        }
        board.tcdm = buildFromXML(elemName, attrs);
        retval = board.tcdm;
        if (retval != null) {
          dacx_.getDataMapSrc().setTimeCourseDataMaps(board.tcdm);
          origSerialNumber_ = board.tcdm.getSerialNumber();
        }
      }
      return (retval);     
    }  
    
    private TimeCourseDataMaps buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String serialString = AttributeExtractor.extractAttribute(elemName, attrs, "TimeCourseDataMaps", "mapSerialNum", false);
      if (serialNumberIsIllegal_ && (serialString != null)) {
        throw new IOException();
      }    
      TimeCourseDataMaps retval = new TimeCourseDataMaps(dacx_);
      try {
        if (serialString != null) {
          long serialNumber = Long.parseLong(serialString);
          if (serialNumber < 0) {
            throw new IOException();
          }
          retval.mapSerialNumber_ = serialNumber;
        }  
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
      return (retval);
    }
  }
  

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TimeCourseMapWorker extends AbstractFactoryClient {
    
    boolean mapsAreIllegal_;
    
    public TimeCourseMapWorker(FactoryWhiteboard whiteboard, boolean mapsAreIllegal) {
      super(whiteboard);
      myKeys_.add("tcMap");
      mapsAreIllegal_ = mapsAreIllegal;
      installWorker(new TimeCourseMapEntryWorker(whiteboard), new MyMapEntryGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      if (elemName.equals("tcMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdmKey = buildFromXML(elemName, attrs);
        board.tcdmList = new ArrayList<TimeCourseDataMaps.TCMapping>();
        retval = board.tcdmList;
      }
      return (retval);     
    }
    //
    // This list addition can only happen when the list is complete! No glue stick! 
    //
    @Override
    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("tcMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdm.addTimeCourseTCMMap(board.tcdmKey, board.tcdmList, false);
        board.tcdmKey = null;
        board.tcdmList = null;
      } 
      return;
    }
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "tcMap", "key", true);
      return (key);
    }
  } 
  
  public static class MyMapEntryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.tcdmList.add(board.tcdmap);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TimeCourseMapEntryWorker extends AbstractFactoryClient {
 
    public TimeCourseMapEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("useTc");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("useTc")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdmap = buildFromXML(elemName, attrs);
        retval = board.tcdmap;
      }
      return (retval);     
    } 
    
    private TCMapping buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "useTc", "name", true);
      String srcString = AttributeExtractor.extractAttribute(elemName, attrs, "useTc", "src", false);
      ExpressionEntry.Source source;
      if (srcString == null) {
        source = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      } else {
        srcString = srcString.trim();
        try {
          source = ExpressionEntry.mapFromSourceTag(srcString);
        } catch (IllegalArgumentException iaex) {
          throw new IOException();
        }
      }
      return (new TCMapping(name, source));
    }
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TimeCourseGroupMapWorker extends AbstractFactoryClient {
    
    boolean mapsAreIllegal_;
    
    public TimeCourseGroupMapWorker(FactoryWhiteboard whiteboard, boolean mapsAreIllegal) {
      super(whiteboard);
      myKeys_.add("tcGroupMap");
      mapsAreIllegal_ = mapsAreIllegal;
      installWorker(new TimeGroupMapEntryWorker(whiteboard), new MyGroupMapEntryGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (mapsAreIllegal_) {
        throw new IOException();
      }
      if (elemName.equals("tcGroupMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdguKey = buildFromXML(elemName, attrs);
        board.tcdguList = new ArrayList<GroupUsage>();
        retval = board.tcdguList;
      }
      return (retval);     
    } 
    
    //
    // This list addition can only happen when the list is complete! No glue stick! 
    //
    @Override
    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("tcGroupMap")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdm.setTimeCourseGroupMap(board.tcdguKey, board.tcdguList, false);
        board.tcdguKey = null;
        board.tcdguList = null;
      }
      return;
    }

    private String buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "tcGroupMap", "key", true);
      return (key);
    }
  } 
  
  public static class MyGroupMapEntryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.tcdguList.add(board.tcdgu);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TimeGroupMapEntryWorker extends AbstractFactoryClient {
 
    public TimeGroupMapEntryWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("useGroup");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("useGroup")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tcdgu = buildFromXML(elemName, attrs);
        retval = board.tcdgu;
      }
      return (retval);     
    } 
    
    private GroupUsage buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String mappedGroup = AttributeExtractor.extractAttribute(elemName, attrs, "useGroup", "name", true);
      String usage = AttributeExtractor.extractAttribute(elemName, attrs, "useGroup", "useFor", false);    
      return (new GroupUsage(mappedGroup, usage));   
    }
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("TimeCourseDataMaps");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractTcMapKey(String elemName, 
                                     Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "tcMap", "key", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static TCMapping extractTCMapping(String elemName, 
                                           Attributes  attrs) throws IOException {
    String name = AttributeExtractor.extractAttribute(elemName, attrs, "useTc", "name", true);
    String srcString = AttributeExtractor.extractAttribute(elemName, attrs, "useTc", "src", false);
    ExpressionEntry.Source source;
    if (srcString == null) {
      source = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
    } else {
      srcString = srcString.trim();
      try {
        source = ExpressionEntry.mapFromSourceTag(srcString);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
    return (new TCMapping(name, source));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractGroupMapKey(String elemName, 
                                     Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "tcGroupMap", "key", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static GroupUsage extractUseGroup(String elemName, 
                                           Attributes attrs) throws IOException {
    String mappedGroup = AttributeExtractor.extractAttribute(
                         elemName, attrs, "useGroup", "name", true);
    String usage = AttributeExtractor.extractAttribute(
                   elemName, attrs, "useGroup", "useFor", false);    
    return (new GroupUsage(mappedGroup, usage));
  }
  
  /***************************************************************************
  **
  ** Return the tcMap keyword
  **
  */
  
  public static String tcMapKeyword() {
    return ("tcMap");
  }
  
  /***************************************************************************
  **
  ** Return the usetc keyword
  **
  */
  
  public static String useTcKeyword() {
    return ("useTc");
  }

  /***************************************************************************
  **
  ** Return the groupmap keyword
  **
  */
  
  public static String groupMapKeyword() {
    return ("tcGroupMap");
  }
  
  /***************************************************************************
  **
  ** Return the useGroup keyword
  **
  */
  
  public static String useGroupKeyword() {
    return ("useGroup");
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the tcmap to XML
  **
  */
  
  private void writeTcMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<tcMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(tcMap_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<TCMapping> list = tcMap_.get(key);
      ind.indent();
      out.print("<tcMap key=\"");
      out.print(key);
      out.println("\">");
      Iterator<TCMapping> lit = list.iterator();
      ind.up();    
      while (lit.hasNext()) {
        TCMapping usetc = lit.next();      
        ind.indent();
        out.print("<useTc name=\"");
        out.print(usetc.name);
        if (usetc.channel != ExpressionEntry.Source.NO_SOURCE_SPECIFIED) {       
          out.print("\" src=\"");
          out.print(ExpressionEntry.mapToSourceTag(usetc.channel));
        }
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</tcMap>");
    }
    ind.down().indent(); 
    out.println("</tcMaps>");
    ind.down();
    return;
  }  

  /***************************************************************************
  **
  ** Write the group map to XML
  **
  */

  private void writeGroupMap(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<tcGroupMaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(groupMap_.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<GroupUsage> list = groupMap_.get(key);
      ind.indent();
      out.print("<tcGroupMap key=\"");
      out.print(key);
      out.println("\">");
      Iterator<GroupUsage> lit = list.iterator();
      ind.up();    
      while (lit.hasNext()) {
        GroupUsage usegr = lit.next();      
        ind.indent();
        out.print("<useGroup name=\"");
        out.print(usegr.mappedGroup);
        if (usegr.usage != null) {
          out.print("\" useFor=\""); 
          out.print(usegr.usage);          
        }
        out.println("\"/>");
      }
      ind.down().indent(); 
      out.println("</tcGroupMap>");
    }
    ind.down().indent(); 
    out.println("</tcGroupMaps>");
    ind.down();
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Specifies a mapping from node to time course gene AND OPTIONAL SOURCE CHANNEL
  **
  */
  
  public static class TCMapping implements Cloneable, Comparable<TCMapping> {
    public String name;
    public ExpressionEntry.Source channel;
    
    public TCMapping(String name) {
      this.name = name;
      this.channel = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
    }
    
    public TCMapping(String name, ExpressionEntry.Source channel) {
      this.name = name;
      this.channel = channel;
    }
    
    @Override
    public TCMapping clone() {
      try {
        TCMapping retval = (TCMapping)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
 
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TCMapping)) {
        return (false);
      }
      TCMapping otherTCM = (TCMapping)other;

      if (!DataUtil.keysEqual(this.name, otherTCM.name)) {
        return (false);
      }
      return (this.channel == otherTCM.channel);
    }
    
    @Override
    public String toString() {
      return (name + ((channel != ExpressionEntry.Source.NO_SOURCE_SPECIFIED) ? (": " + ExpressionEntry.mapToSourceTag(channel)) : ""));
    }    

    @Override
    public int hashCode() {
      return (DataUtil.normKey(name).hashCode() + channel.hashCode());
    }  

    public static ArrayList<TCMapping> cloneAList(List<TCMapping> toClone) {
      ArrayList<TCMapping> retval = new ArrayList<TCMapping>();
      int num = toClone.size();
      for (int i = 0; i < num; i++) {
        TCMapping tcm = toClone.get(i);
        retval.add(tcm.clone());
      }
      return (retval);      
    }
    
    public int compareTo(TCMapping other) {
      if (this == other) {
        return (0);
      }
      int retval = this.name.compareTo(other.name);
      if (retval != 0) {
        return (retval);
      }
      return (this.channel.compareTo(other.channel));
    }  
     
    public static boolean nameInList(String name, List<TCMapping> toCheck) {
      int num = toCheck.size();
      for (int i = 0; i < num; i++) {
        TCMapping tcm = toCheck.get(i);
        if (DataUtil.keysEqual(name, tcm.name)) {
          return (true);
        }
      }
      return (false);
    }
  }
 
}
