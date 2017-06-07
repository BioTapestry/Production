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

package org.systemsbiology.biotapestry.genome;

import org.systemsbiology.biotapestry.plugin.InternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;

/****************************************************************************
**
** Displays Free Text for a link
*/

public class FreeLinkTextDisplayPlugIn implements InternalLinkDataDisplayPlugIn {
  
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

  public FreeLinkTextDisplayPlugIn() {
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

    //
    // Have to work with the root instance:
    //
    
    TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(ddacx_, dbID);
    StaticDataAccessContext dacx = new StaticDataAccessContext(tpdacx).getContextForRoot(); 
    
    
    TopOfTheHeap tooth = new TopOfTheHeap(dacx, genomeID);
    Genome genome = tooth.getGenome();   
    boolean haveInstance = tooth.isInstance();
 
    //
    // If we have our own local free text, we display that.  If not, we display the root
    // text. 
    //
    
    if (!haveInstance) {
      return (false);
    }
    
    Linkage link = genome.getLinkage(itemID);
    String desc = link.getDescription();
    return (desc != null);
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
  
  public PluginCallbackWorker getCallbackWorker(String dbID, String genomeID, String linkId) {
    return (null);
  } 
  
  /***************************************************************************
  **
  ** Show the free text
  */
  
  public String getDataAsHTML(String dbID, String genomeID, String linkId) {
    StringBuffer buf = new StringBuffer();
    
    //
    // Have to work with the root instance:
    //
    
    TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(ddacx_, dbID);
    StaticDataAccessContext dacx = new StaticDataAccessContext(tpdacx).getContextForRoot();   
   
    TopOfTheHeap tooth = new TopOfTheHeap(dacx, genomeID);
    Genome genome = tooth.getGenome();   
    boolean haveInstance = tooth.isInstance();
    
    //
    // If we have our own local free text, we display that.  If not, we display the root
    // text. 
    //
       
    Linkage link = genome.getLinkage(linkId);
    String desc = link.getDescription();
    if ((desc == null) && haveInstance) {
      Genome useGenome = dacx.getDBGenome();
      String useID = GenomeItemInstance.getBaseID(linkId);
      link = useGenome.getLinkage(useID);
      desc = link.getDescription();
    }
    
    if (desc != null) {
      buf.append("<p>");
      buf.append(desc);
      buf.append("</p>");     
    }       
    return (buf.toString());
  }
}
