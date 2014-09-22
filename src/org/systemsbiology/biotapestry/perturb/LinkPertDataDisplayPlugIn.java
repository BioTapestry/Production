/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.ui.DisplayOptionsManager;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays Perturbation Data for Links
*/

public class LinkPertDataDisplayPlugIn implements InternalLinkDataDisplayPlugIn {
  
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

  public LinkPertDataDisplayPlugIn() {
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
    return (true);
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
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String linkID) {
    return (null);
  }

  /***************************************************************************
  **
  ** Show the perturbation data for the link
  */
  
  public String getDataAsHTML(String genomeID, String linkID) {
    StringBuffer buf = new StringBuffer();
    Database db = appState_.getDB();
    Genome genome = db.getGenome(genomeID);
    Linkage link = genome.getLinkage(linkID);
    String targID = link.getTarget();
    String sourceID = link.getSource();
    PerturbationData pd = db.getPertData();
    String baseTargID = GenomeItemInstance.getBaseID(targID);
    String baseSourceID = GenomeItemInstance.getBaseID(sourceID);
    if (!pd.haveDataForNode(baseTargID, baseSourceID)) {
      return(" ");
    }
    buf.append("<center><h1>");
    String desc = appState_.getRMan().getString("dataWindow.pertDataForLink");
    buf.append(desc);
    buf.append("</h1></center>\n");
    boolean largeFont = appState_.getDisplayOptMgr().isForBigScreen();
    String table = pd.getHTML(baseTargID, baseSourceID, true, largeFont);
    if (table != null) {
      buf.append(table);
    } else {
      buf.append("<center>"); 
      buf.append(appState_.getRMan().getString("dataWindow.noPertDataForLink")); 
      buf.append("</center>");
    }
    return (buf.toString());
  } 
}
