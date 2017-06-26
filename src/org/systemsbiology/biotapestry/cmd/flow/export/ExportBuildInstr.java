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
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;

/****************************************************************************
**
** Export perturb data to CSV file
*/

public class ExportBuildInstr extends AbstractSimpleExport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportBuildInstr(BTState appState) {
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
    name = "command.BuildInstrToCSV"; 
    desc = "command.BuildInstrToCSV"; 
    mnem = "command.BuildInstrToCSVMnem";
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
 
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.haveBuildInstructions());
  }
  
  /***************************************************************************
  **
  ** Fill in file specifics
  ** 
  */

  @Override
  protected void prepFileDialog(ExportState es) {
    es.filts.add(new FileExtensionFilters.SimpleFilter(es.getDACX().getRMan(), ".csv", "filterName.csv"));
    es.suffs.add("csv");
    es.direct = "BuildInstrToCSVDirectory";
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
    (new BuildInstructionProcessor(es.getUICS(), es.getDACX(), es.getTSrc(), es.getUFac())).exportInstructions(es.out, es.getDACX());        
    return (true);
  } 
}
