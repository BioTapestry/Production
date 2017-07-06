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
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.TimeCourseCSVExportOptionsDialogFactory;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;

/****************************************************************************
**
** Export expression data to CSV file
*/

public class ExportExpression extends AbstractSimpleExport {

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportExpression(BTState appState) {
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
    name = "command.ExpressToCSV"; 
    desc = "command.ExpressToCSV"; 
    icon = "FIXME24.gif";
    mnem = "command.ExpressToCSVMnem";
    accel = null;
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.haveTimeCourseData());
  }
  
  /***************************************************************************
  **
  ** Handle pre-file dialogs
  ** 
  */
  
  @Override
  protected ServerControlFlowHarness.Dialog preFileDialog(ServerControlFlowHarness cfh) {
    TimeCourseCSVExportOptionsDialogFactory df = new TimeCourseCSVExportOptionsDialogFactory(cfh);
    return (df.getDialog(null));
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
    es.direct = "ExpressToCSVDirectory";
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
    es.fileErrMsg = "none";
    es.fileErrTitle = "none";
    TimeCourseExportSettings tces = (TimeCourseExportSettings)es.preFileSet;
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
    tcd.exportCSV(es.out, !tces.ordByReg, tces.embedConf, !tces.skipInt, dacx.getRMan());             
    return (true);  
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** User input data
  ** 
  */

  public static class TimeCourseExportSettings implements ServerControlFlowHarness.UserInputs {
    public boolean skipInt;
    public boolean ordByReg;
    public boolean embedConf;
    private boolean haveResult;
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }     
  	public void setHasResults() {
  		this.haveResult = true;
  		return;
  	}  
    public boolean isForApply() {
      return (false);
    } 
  }    
}
