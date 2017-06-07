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

package org.systemsbiology.biotapestry.plugin;

import java.util.ResourceBundle;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** API for Model Builder Plugin
*/

public interface ModelBuilderPlugIn extends BioTapestryPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Internal plugins need to have access to internal state
  */
  
  public void setDataAccessContext(DynamicDataAccessContext ddac);
  
  /***************************************************************************
  **
  ** get the Model Builder Database source
  */
  
  public ModelBuilder.Database getModelBuilderDatabase();
 
  /***************************************************************************
  **
  ** Returns name to be displayed in the builder plug-in menu
  */
  
  public String getMenuName();
  
  /***************************************************************************
  **
  ** Returns dashboard
  */
  
  public ModelBuilder.Dashboard getDashboard();
  
  /***************************************************************************
  **
  ** Returns dashboard
  */
  
  public ModelBuilder.LinkDrawingTracker getTracker();
  
  /***************************************************************************
  **
  ** Returns IDMapper
  */
  
  public ModelBuilder.IDMapper getIDMapper();
 
  /***************************************************************************
  **
  ** Returns Resolver
  */
  
  public ModelBuilder.Resolver getResolver();
  
  /***************************************************************************
  **
  ** Returns Data Loader
  */
  
  public ModelBuilder.DataLoader getDataLoader();
 
  /***************************************************************************
  **
  ** Tell of region name change
  */
  
  public void regionNameChange(DataAccessContext dacx, String oldName, String newName, UndoSupport support);        
   
  /***************************************************************************
  **
  ** Tell of new drawn link
  */
  
  public void linkJustDrawn(DataAccessContext dacx, String builtID);     
  
  /***************************************************************************
  **
  ** Tell of deleted node
  */

  public void handleNodeDeletion(DataAccessContext dacx, String deadID);
 
  /***************************************************************************
  **
  ** Tell of region deletion change
  */
  
  public void dropRegionName(DataAccessContext dacx, String dropRegion, UndoSupport support);
  
  /***************************************************************************
  **
  ** Get plugin resource bundle
  */
  
  public ResourceBundle getPluginResources();

}
