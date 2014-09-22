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


package org.systemsbiology.biotapestry.nav;

import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.io.PrintStream;

/****************************************************************************
**
** Describes the current group settings for a genome view
*/

public class GroupSettings {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Possible Group settings we can perform
  */ 
   
  public enum Setting {NONE, INACTIVE, ACTIVE};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, Setting> groupSettings_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Null constructor
  */
  
  public GroupSettings() {
    groupSettings_ = new HashMap<String, Setting>();
    return;
  }    
 
  /***************************************************************************
  ** 
  ** Copy constructor
  */
  
  public GroupSettings(GroupSettings other) {
    this.groupSettings_ = new HashMap<String, Setting>(other.groupSettings_);
    return;
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Initialize all the group settings to the given value, for the given groups.
  */

  public void setVisibility(Set<String> groupNames, Setting newSetting) {
    Iterator<String> gnit = groupNames.iterator();
    while (gnit.hasNext()) {  
      String name = gnit.next();
      groupSettings_.put(name, newSetting);
    } 
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Set the visibility setting for a particular group
  */

  public void setVisibility(String groupName, Setting newSetting) {
    groupSettings_.put(groupName, newSetting); 
    return;
  }
  
  /***************************************************************************
  ** 
  ** Drop the visibility setting for a particular group
  */

  public void dropVisibility(String groupName) {
    groupSettings_.remove(groupName); 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the visibility setting for a particular group
  */

  public Setting getVisibility(String groupName) {
    Setting setting = groupSettings_.get(groupName);
    // May need lazy init for dynamic proxy groups
    if (setting == null) {
      groupSettings_.put(groupName, Setting.ACTIVE);
      return (Setting.ACTIVE);
    }
    return (setting);
  }

  /***************************************************************************
  ** 
  ** Iterate over keys
  */

  public Iterator<String> getKeyIterator() {
    return (groupSettings_.keySet().iterator());
  }
  
  /***************************************************************************
  ** 
  ** For debug: output all group visibilities
  */

  public void dumpVisibilities(PrintStream out) {
    Iterator<String> kit = groupSettings_.keySet().iterator();
    while (kit.hasNext()) {
      String id = kit.next();
      out.print(id);
      Setting setting = groupSettings_.get(id);    
      switch (setting) {
        case NONE:
          out.println(": NONE");
          break;
        case INACTIVE:
          out.println(": INACTIVE");
          break;          
        case ACTIVE:
          out.println(": ACTIVE");
          break;          
        default:
          throw new IllegalStateException();
      }
    }
    return;
  }
}
