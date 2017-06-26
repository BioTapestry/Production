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


package org.systemsbiology.biotapestry.cmd.flow.export;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;

/****************************************************************************
**
** Export perturb data to CSV file
*/

public class ExportPerturbCSV extends AbstractSimpleExport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportPerturbCSV(BTState appState) {
    super(appState);
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Kids gotta know this stuff
  ** 
  */
   
  @Override
  protected void fillResources() {    
    name = "command.PerturbToCSV"; 
    desc = "command.PerturbToCSV"; 
    icon = "FIXME24.gif";
    mnem = "command.PerturbToCSVMnem";
    accel = null;
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  public boolean isEnabled(CheckGutsCache cache) {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    PerturbationData pd = dacx.getExpDataSrc().getPertData();
    return ((pd != null) && pd.haveData());
  }
  
  /***************************************************************************
  **
  ** Fill in file specifics
  ** 
  */
  
  @Override
  protected void prepFileDialog(ExportState es) {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    es.filts.add(new FileExtensionFilters.SimpleFilter(dacx.getRMan(), ".csv", "filterName.csv"));
    es.suffs.add("csv");
    es.direct = "PerturbToCSVDirectory";
    es.pref = "csv";
    return;
  }
 
  /***************************************************************************
  **
  ** Do the operations
  ** 
  */

  @Override
  protected boolean runTheExport(ExportState es, TabSource tSrc) {
    es.fileErrMsg = "PerturbToCSV.IOError";
    es.fileErrTitle = "PerturbToCSV.IOErrorTitle";
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    PerturbationData pd = dacx.getExpDataSrc().getPertData();  
    return (pd.publishAsCSV(es.out, tad));  
  }
  
}
