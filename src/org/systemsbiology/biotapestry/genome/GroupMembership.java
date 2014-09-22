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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


/***************************************************************************
**
** Gives group membership info for a node or link
**
*/
  
public class GroupMembership {
  public Set<String> mainGroups;  // One member for nodes, one or two for links
  public Set<String> subGroups;
  
  public GroupMembership() {
    mainGroups = new HashSet<String>();    
    subGroups = new HashSet<String>();
  }
  
  public boolean contains(String groupKey) {
    Iterator<String> mit = mainGroups.iterator();
    while (mit.hasNext()) {
      String gid = mit.next();
      if (gid.equals(groupKey)) {
        return (true);
      }
    }
    Iterator<String> sit = subGroups.iterator();
    while (sit.hasNext()) {
      String gid = sit.next();
      if (gid.equals(groupKey)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a group match with one of the groups in the provided sets
  ** 
  */
  
  public String gotAGroupMatch(Set<String> mainGroupsToShow, Set<String> activeSubGroupToShow) {
    Iterator<String> mit = mainGroups.iterator();
    while (mit.hasNext()) {
      String grpID = mit.next();
      String baseGroupID = Group.getBaseID(grpID);
      if (mainGroupsToShow.contains(baseGroupID)) {
        return (baseGroupID);
      }
    }   
    Iterator<String> sit = subGroups.iterator();
    while (sit.hasNext()) {
      String grpID = sit.next();
      String baseGroupID = Group.getBaseID(grpID);
      if (activeSubGroupToShow.contains(baseGroupID)) {
        return (baseGroupID);
      }
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Standard toString...
  ** 
  */  
  
  public String toString() {
    return ("GroupMembership: main = " + mainGroups + " sub = " + subGroups);
  }
}
