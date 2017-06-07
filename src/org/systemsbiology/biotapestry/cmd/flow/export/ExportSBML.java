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

import java.util.Date;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** Export perturb data to CSV file
*/

public class ExportSBML extends AbstractSimpleExport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportSBML(BTState appState) {
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
    name = "command.SBMLWrite"; 
    desc = "command.SBMLWrite"; 
    icon = "Export24.gif";
    mnem = "command.SBMLWriteMnem";
    accel = null;
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.genomeCanWriteSBML());
  }
  
  /***************************************************************************
  **
  ** Fill in file specifics
  ** 
  */
   
  @Override
  protected void prepFileDialog(ExportState es) {
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    es.filts.add(new FileExtensionFilters.SimpleFilter(dacx.getRMan(), ".xml", "filterName.xml"));
    es.filts.add(new FileExtensionFilters.DoubleExtensionFilter(dacx.getRMan(), ".sbm", ".sbml", "filterName.sbm"));  
    es.suffs.add("xml");
    es.suffs.add("sbm");
    es.suffs.add("sbml");
    es.direct = "SBMLWriterDirectory";
    es.pref = "sbm";
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
    Indenter ind = new Indenter(es.out, Indenter.DEFAULT_INDENT);
    DataAccessContext dacx = new StaticDataAccessContext(appState_);
    Genome genome = dacx.getCurrentGenome();
    es.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    es.out.println("<!-- Created by BioTapestry " + new Date() + " -->");
    ind.indent();
    es.out.println("<sbml xmlns=\"http://www.sbml.org/sbml/level1\" level=\"1\" version=\"2\">");
    ind.up();
    genome.writeSBML(es.out, ind);
    ind.down().indent();
    es.out.println("</sbml>");
    return (true);  
  } 
}
