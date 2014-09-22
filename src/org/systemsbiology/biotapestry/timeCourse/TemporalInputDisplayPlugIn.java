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

import java.util.List;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays Temporal Input Data
*/

public class TemporalInputDisplayPlugIn implements InternalNodeDataDisplayPlugIn {

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

  public TemporalInputDisplayPlugIn() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Internal plugins need to have access to internal state
  */
  
  public void setAppState(BTState appState) {
    appState_ = appState;
    return;
  }

  /***************************************************************************
  **
  ** Determine data requirements
  */
  
  public boolean isInternal() {
    return (InternalDataDisplayPlugInV2.IS_INTERNAL_ANSWER);
  }
  
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String genomeID, String itemID) {
    return (false);
  }
   
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML() routine should only provide the initial data view.
  */
  
  public boolean haveCallbackWorker() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get the worker that will gather up background data and call us back
  */
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String nodeId) {
    return (null);
  }
 
  /***************************************************************************
  **
  ** Show the Temporal Input Data
  */
  
  public String getDataAsHTML(String genomeIDX, String nodeID) {
    StringBuffer buf = new StringBuffer(); 
    Database db = appState_.getDB();
  
    TemporalInputRangeData trd = db.getTemporalInputRangeData();
    if ((trd == null) || !trd.haveData()) {
      return ("");
    }
    
    // Always access from root model
    
    nodeID = GenomeItemInstance.getBaseID(nodeID);
    
    ResourceManager rMan = appState_.getRMan();
    String desc = rMan.getString("dataWindow.temporalInputs");
    buf.append("<center><h1>"); 
    buf.append(desc); 
    buf.append("</h1>\n");

    List<String> dataKeys = trd.getTemporalInputRangeEntryKeysWithDefault(nodeID);
    boolean gotData = false;
    if (dataKeys != null) {
      Iterator<String> dkit = dataKeys.iterator();
      while (dkit.hasNext()) {
        String key = dkit.next();
        String tab = trd.getInputsTable(key, null);
        if ((tab != null) && (!tab.trim().equals(""))) {
          gotData = true;
          buf.append("<p>");
          buf.append(tab);
          buf.append("</p>");          
        }
      }
    }
    if (!gotData) {
      buf.append(rMan.getString("dataWindow.noTemporalInputs"));
    }
    buf.append("</center>");
    
    return (buf.toString());
  }
}
