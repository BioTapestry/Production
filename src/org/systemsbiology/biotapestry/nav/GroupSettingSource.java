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

import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Interface for group setting sources
*/

public interface GroupSettingSource {

  /***************************************************************************
  ** 
  ** Get the active toggle state for a view and group
  */

  public GroupSettings.Setting getGroupVisibility(String viewName, String groupName, DataAccessContext dacx);
  
  
  /***************************************************************************
  ** 
  ** Set the active toggle state for all groups of a view
  */

  public void setGroupVisibilities(GenomeInstance gi, GroupSettings.Setting setting);
  
  
  /***************************************************************************
  ** 
  ** Drop state for a genome
  */
  
  public GroupSettingChange dropGroupVisibilities(String giName);
  
  /***************************************************************************
  ** 
  ** Drop state for a view and group
  */

  public GroupSettingChange dropGroupVisibility(String viewName, String groupName);  
  
  
  /***************************************************************************
  ** 
  ** Set the active toggle state for a view and group
  */

  public GroupSettingChange setGroupVisibility(String genomeName, String groupName, GroupSettings.Setting setting);
  
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GroupSettingChange undo);
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GroupSettingChange undo);
  
  
  /***************************************************************************
  **
  ** drop Orphaned Visibilities
  */
  
  public boolean dropOrphanedVisibilities(String genomeID, DataAccessContext dacx, Set<String> liveGroups, UndoSupport support);
  
  /***************************************************************************
  **
  ** drop Orphaned Visibilities For Proxy
  */
  
  public boolean dropOrphanedVisibilitiesForProxy(String proxyID, DataAccessContext dacx, Set<String> liveGroups, UndoSupport support);
       
  /***************************************************************************
  **
  ** Drop settings
  */
  
  public void drop();
  


}
