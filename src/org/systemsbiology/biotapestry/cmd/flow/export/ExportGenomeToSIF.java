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


package org.systemsbiology.biotapestry.cmd.flow.export;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;

/****************************************************************************
**
** Export perturb data to CSV file
*/

public class ExportGenomeToSIF extends AbstractSimpleExport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportGenomeToSIF(BTState appState) {
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
    name = "command.GenomeToSIF"; 
    desc = "command.GenomeToSIF"; 
    icon = "Export24.gif";
    mnem = "command.GenomeToSIFMnem";
    accel = null;
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.genomeNotEmpty());
  }
  
  /***************************************************************************
  **
  ** Fill in file specifics
  ** 
  */

  @Override 
  protected void prepFileDialog(ExportState es) {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    es.filts.add(new FileExtensionFilters.SimpleFilter(dacx.getRMan(), ".sif", "filterName.sif"));
    es.suffs.add("sif");
    es.direct = "GenomeToSIFDirectory";
    es.pref = "sif";
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
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    String key = dacx.getCurrentGenomeID();
    Genome genome = dacx.getGenomeSource().getGenome(key);            
    genome.writeSIF(es.out);     
    return (true);  
  } 
}
