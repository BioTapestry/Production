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

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;

/****************************************************************************
**
** Base class for plugins that display experimental data using internal link IDs.
*/

public interface InternalDataDisplayPlugInV2 extends DataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final boolean IS_INTERNAL_ANSWER = true;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Internal plugins need to have access to internal state
  */
  
  public void setDataAccessContext(DynamicDataAccessContext dacx, UIComponentSource uics);
  
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String dbID, String genomeID, String itemID);
    
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or head or body tags, please!) to display
  ** as experimental data for the desired link in the desired model.  Used 
  ** for internal plug-ins with more required arguments.
  */
  
  public String getDataAsHTML(String dbID, String genomeID, String itemID);
   
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML routine above should only provide an "initial" data view.
  */
  
  public boolean haveCallbackWorker();
  
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or head or body tags, please!) to display
  ** as experimental data for the desired link in the desired model.  Used 
  ** for internal plug-ins with more required arguments.
  */
  
  public PluginCallbackWorker getCallbackWorker(String dbID, String genomeID, String itemID);
  
}
