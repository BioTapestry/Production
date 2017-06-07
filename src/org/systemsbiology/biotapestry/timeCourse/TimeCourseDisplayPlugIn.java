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

package org.systemsbiology.biotapestry.timeCourse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.InternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Displays Time course data
*/

public class TimeCourseDisplayPlugIn implements InternalNodeDataDisplayPlugIn {
  
  private DynamicDataAccessContext ddacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseDisplayPlugIn() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Internal plugins need to have access to data state
  */
  
  public void setDataAccessContext(DynamicDataAccessContext ddacx, UIComponentSource uics) {
    ddacx_ = ddacx;
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
  
  public boolean requiresPerInstanceDisplay(String dbID, String genomeID, String itemID) {
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
  
  public PluginCallbackWorker getCallbackWorker(String dbID, String genomeID, String nodeID) {
    return (null);
  }
  
  /***************************************************************************
  **
  ** Show the Time course data
  */
  
  public String getDataAsHTML(String dbID, String genomeIDX, String nodeID) {
  
    TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(ddacx_, dbID);
    StaticDataAccessContext dacx = new StaticDataAccessContext(tpdacx).getContextForRoot();
    StringBuffer buf = new StringBuffer(); 
    TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
    TimeCourseDataMaps tcdm = dacx.getDataMapSrc().getTimeCourseDataMaps();
    if ((tcd == null) || !tcd.haveDataEntries()) {
      return ("");
    }
    
    // Always access from root model
    
    nodeID = GenomeItemInstance.getBaseID(nodeID);
    
    ResourceManager rMan = dacx.getRMan();
    buf.append("<center><h1>"); 
    buf.append(rMan.getString("dataWindow.expressionProfile")); 
    buf.append("</h1>\n");
    
    
    boolean haveHierarchy = tcd.hierarchyIsSet();
    DisplayOptions dOpt = dacx.getDisplayOptsSource().getDisplayOptions();
    boolean showTree = (haveHierarchy) ? dOpt.showExpressionTableTree() : false;
  
    List<TimeCourseDataMaps.TCMapping> dataKeys = tcdm.getTimeCourseTCMDataKeysWithDefault(nodeID, dacx.getGenomeSource());
    boolean gotData = false;
    if (dataKeys != null) {
      Iterator<TimeCourseDataMaps.TCMapping> dkit = dataKeys.iterator();
      int needKey = TimeCourseTableDrawer.NO_TABLE_KEY;
      while (dkit.hasNext()) {
        TimeCourseDataMaps.TCMapping tcm = dkit.next();
        TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(tcm.name);
        if (tcg == null) {  // If no table at all, still get back default name...
          continue;
        }
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int nextKey = tcg.getExpressionTable(out, dacx, tcd, showTree);
        needKey |= nextKey;
        String tab = sw.getBuffer().toString();
        if ((tab != null) && (!tab.trim().equals(""))) {
          gotData = true;
          buf.append("<p>");
          buf.append(tab);
          buf.append("</p>");          
        }
      }
      buf.append(TimeCourseTableDrawer.buildKey(dacx, needKey, showTree, false));
    }
    if (!gotData) {
      buf.append(rMan.getString("dataWindow.noExpressionProfile"));
    }
    buf.append("</center>");    
    return (buf.toString());
  }
}
