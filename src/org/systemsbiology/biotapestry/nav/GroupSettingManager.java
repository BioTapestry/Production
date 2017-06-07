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

package org.systemsbiology.biotapestry.nav;

import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.io.PrintStream;

import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;

/****************************************************************************
**
** Handles the current group expansion/collapse state. 
*/

public class GroupSettingManager implements GroupSettingSource {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, GroupSettings> settings_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public GroupSettingManager() {
    settings_ = new HashMap<String, GroupSettings>();
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Drop the current settings
  */

  public void drop() {
    settings_.clear();
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Set the active toggle state for a view and group
  */

  public GroupSettingChange setGroupVisibility(String genomeName, String groupName, GroupSettings.Setting setting) {
    GroupSettingChange retval = new GroupSettingChange();
    retval.genomeKey = genomeName;
    retval.groupKey = groupName;
    GroupSettings gs = settings_.get(genomeName);
    retval.oldSetting = gs;
    // FIX ME?? Kinda inefficient to use clones for a single setting change!
    if (gs == null) {
      retval.oldSetting = null;
      gs = new GroupSettings();
      settings_.put(genomeName, gs);
    } else {
      retval.oldSetting = new GroupSettings(gs);
    }
    gs.setVisibility(groupName, setting);
    retval.newSetting = new GroupSettings(gs);
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Set the active toggle state for all groups of a view
  */

  public void setGroupVisibilities(GenomeInstance gi, GroupSettings.Setting setting) {
    String giName = gi.getID();
    GroupSettings gs = settings_.get(giName);
    if (gs == null) {
      gs = new GroupSettings();
      settings_.put(giName, gs);
    }    
     
    HashSet<String> groupNames = new HashSet<String>();
    Iterator<Group> grit = gi.getGroupIterator();
    while (grit.hasNext()) {
      Group group = grit.next();
      String groupID = group.getID();
      groupNames.add(groupID);
    }
    
    gs.setVisibility(groupNames, setting);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Drop the visibility information for a genome instance
  */

  public GroupSettingChange dropGroupVisibilities(String giName) {
    GroupSettingChange retval = new GroupSettingChange();
    retval.genomeKey = giName;
    retval.oldSetting = settings_.remove(giName);
    return ((retval.oldSetting != null) ? retval : null);
  }  

  /***************************************************************************
  ** 
  ** Drop the visibility information for a genome instance
  */

  public GroupSettingChange dropGroupVisibility(String giName, String groupName) {
    GroupSettingChange retval = new GroupSettingChange();
    retval.genomeKey = giName;
    GroupSettings gs = settings_.get(giName);
    if (gs == null) {
      return (null);
    }
    retval.oldSetting = new GroupSettings(gs);
    gs.dropVisibility(groupName);
    retval.newSetting = new GroupSettings(gs);
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Get the active toggle state for a view and group
  */

  public GroupSettings.Setting getGroupVisibility(String viewName, String groupName, DataAccessContext dacx) {
    GroupSettings gs = settings_.get(viewName);    
    // Lazy init for dynamic instances:
    if (gs == null) {
      Genome genome = dacx.getGenomeSource().getGenome(viewName);
      setGroupVisibilities((GenomeInstance)genome, GroupSettings.Setting.ACTIVE);
      gs = settings_.get(viewName);
    }
    return (gs.getVisibility(groupName));
  }

  /***************************************************************************
  ** 
  ** Get all the group settings for a view
  */

  public GroupSettings getGroupVisibilities(String viewName, DataAccessContext dacx) {
    GroupSettings retval = settings_.get(viewName);
    // Lazy init for dynamic instances:
    if (retval == null) {
      Genome genome = dacx.getGenomeSource().getGenome(viewName);
      setGroupVisibilities((GenomeInstance)genome, GroupSettings.Setting.ACTIVE);
      retval = settings_.get(viewName);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GroupSettingChange undo) {
    if (undo.oldSetting == null) {
      settings_.remove(undo.genomeKey);
    } else {
      settings_.put(undo.genomeKey, undo.oldSetting);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GroupSettingChange undo) {
    settings_.put(undo.genomeKey, undo.newSetting);       
    return;
  }
  
  /***************************************************************************
  ** 
  ** For debug: output all group visibilities
  */

  public void dumpGroupVisibilities(PrintStream out) {
    Iterator<String> kit = settings_.keySet().iterator();
    while (kit.hasNext()) {
      String giid = kit.next();
      out.print(giid);
      out.println(":");
      GroupSettings gs = settings_.get(giid);
      gs.dumpVisibilities(out);
    }
    return;
  } 

  /***************************************************************************
  ** 
  ** Drop all orphaned group visibilities for a genome instance
  */

  public boolean dropOrphanedVisibilities(String genomeID, DataAccessContext dacx, Set<String> liveGroups, UndoSupport support) {
    boolean retval = false;
    if (liveGroups.isEmpty()) {
      GroupSettingChange gsc = dropGroupVisibilities(genomeID);
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(dacx, gsc);
        support.addEdit(gscc);
        retval = true;
      }
      return (retval);
    }
    
    GroupSettings gs = settings_.get(genomeID);
    if (gs == null) {
      return (false);
    }
    Iterator<String> kit = gs.getKeyIterator();
    HashSet<String> deadGuys = new HashSet<String>();
    while (kit.hasNext()) {
      String key = kit.next();
      if (!liveGroups.contains(key)) {
        deadGuys.add(key);
      }
    }
    if (deadGuys.isEmpty()) {
      return (false);
    }
    
    GroupSettingChange gsc = new GroupSettingChange();
    gsc.genomeKey = genomeID;
    gsc.oldSetting = new GroupSettings(gs);

    Iterator<String> dgit = deadGuys.iterator();
    while (dgit.hasNext()) {
      String key = dgit.next();
      gs.dropVisibility(key);
    }
      
    gsc.newSetting = new GroupSettings(gs);      
    GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(dacx, gsc);
    support.addEdit(gscc);
    return (true);
  }  

  /***************************************************************************
  ** 
  ** Drop all orphaned group visibilities for a dynamic proxy
  */

  public boolean dropOrphanedVisibilitiesForProxy(String proxyID, DataAccessContext dacx, Set<String> liveGroups, UndoSupport support) {
    
    HashSet<String> deadGuys = new HashSet<String>();
    Iterator<String> kit = settings_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      if (!DynamicInstanceProxy.isDynamicInstance(key)) {
        continue;
      }
      String proxID = DynamicInstanceProxy.extractProxyID(key);
      if (proxyID.equals(proxID)) {
        deadGuys.add(key);
      }
    }
  
    if (deadGuys.isEmpty()) {
      return (false);
    }
  
    boolean retval = false;
    Iterator<String> dgit = deadGuys.iterator();
    while (dgit.hasNext()) {
      String key = dgit.next();
      boolean didIt = dropOrphanedVisibilities(key, dacx, liveGroups, support);
      retval |= didIt;
    }
    
    return (retval);
  }
}
