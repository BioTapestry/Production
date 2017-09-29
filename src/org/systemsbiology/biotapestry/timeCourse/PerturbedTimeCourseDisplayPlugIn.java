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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays perturbed time course data for a gene
*/

public class PerturbedTimeCourseDisplayPlugIn implements InternalNodeDataDisplayPlugIn {
  
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

  public PerturbedTimeCourseDisplayPlugIn() {
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
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String nodeID) {
    return (null);
  }
  
  /***************************************************************************
  **
  ** Show the Time course data
  */
  
  public String getDataAsHTML(String genomeIDX, String nodeID) {
  
    StringBuffer buf = new StringBuffer(); 
    Database db = appState_.getDB(); 
    TimeCourseData tcd = db.getTimeCourseData();
    if ((tcd == null) || !tcd.haveData()) {
      return ("");
    }
    
    // Always access from root model
    
    nodeID = GenomeItemInstance.getBaseID(nodeID);
    
    ResourceManager rMan = appState_.getRMan();
    buf.append("<center><h1>"); 
    buf.append(rMan.getString("dataWindow.perturbedExpressionProfiles")); 
    buf.append("</h1>\n");
    
    boolean gotData = false;
    List<TimeCourseData.TCMapping> dataKeys = tcd.getTimeCourseTCMDataKeysWithDefault(nodeID);
    if (dataKeys != null) {
      Iterator<TimeCourseData.TCMapping> dkit = dataKeys.iterator();
      int needKey = TimeCourseTableDrawer.NO_TABLE_KEY;
      while (dkit.hasNext()) {
        TimeCourseData.TCMapping tcm = dkit.next();
        TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(tcm.name);
        if (tcg == null) {  // If no table at all, still get back default name...
          continue;
        }
        Iterator<PertSources> pkit = tcg.getPertKeys();
        while (pkit.hasNext()) {
          PertSources pss = pkit.next();
          StringWriter sw = new StringWriter();
          PrintWriter out = new PrintWriter(sw);
          PerturbedTimeCourseGene pertGene = tcg.getPerturbedState(pss);
          int nextKey = pertGene.getExpressionTable(out, tcg, tcd);        
          needKey |= nextKey;
          String tab = sw.getBuffer().toString();
          if ((tab != null) && (!tab.trim().equals(""))) {
            gotData = true;
            buf.append("<p>");
            buf.append(tab);
            buf.append("</p>");          
          }
        }
      }
      buf.append(TimeCourseTableDrawer.buildKey(appState_, needKey, false, true));
    }
    
    if (!gotData) {
      buf.append(rMan.getString("dataWindow.noPerturbedExpressionProfile"));
    }
    buf.append("</center>");    
    return (buf.toString());
  }
}
