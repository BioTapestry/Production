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

package org.systemsbiology.biotapestry.plugin.examples;

import org.systemsbiology.biotapestry.plugin.ExternalLinkDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;

/****************************************************************************
**
** A Class
*/

public class LinkArgsPlugIn implements ExternalLinkDataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LinkArgsPlugIn() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Determine data requirements
  */
  
  public boolean isInternal() {
    return (ExternalLinkDataDisplayPlugIn.IS_INTERNAL_ANSWER);
  }
 
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String[] modelNameChain, 
                                            String srcNodeName, String srcRegionName, 
                                            String trgNodeName, String trgRegionName) {
    return (true);
  } 
    
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML routine above should only provide an "initial" data view.
  */
  
  public boolean haveCallbackWorker() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get the callback worker (we do not have one)
  */
  
  public PluginCallbackWorker getCallbackWorker(String[] modelNameChain, 
                                                String srcNodeName, String srcRegionName, 
                                                String trgNodeName, String trgRegionName) {
    return (null);
  }
 
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or body tags, please!) to display
  ** as experimental data for the desired item.
  */
  
  public String getDataAsHTML(String[] modelNameChain, 
                              String srcNodeName, String srcRegionName, 
                              String trgNodeName, String trgRegionName) {
    StringBuffer buf = new StringBuffer();
    buf.append("<p></p>");
    buf.append("<center><h3>Arguments for External Link Data Display Plugin</h3></center>\n");
    
    buf.append("<p>");
    buf.append("Model chain: ");
    for (int i = 0; i < modelNameChain.length; i++) {
      buf.append("<b>");
      buf.append(modelNameChain[i]);
      buf.append("</b>");
      if (i != modelNameChain.length - 1) {
        buf.append("::");
      }
    }
    buf.append("</p>\n");
    
    buf.append("<p>");
    buf.append("Source: <b>");
    buf.append(srcNodeName);
    buf.append("</b> in region <b>");  
    buf.append(srcRegionName);
    buf.append("</b></p>\n");
    
    buf.append("<p>");
    buf.append("Target: <b>");
    buf.append(trgNodeName);
    buf.append("</b> in region <b>");  
    buf.append(trgRegionName);
    buf.append("</b></p>\n");
    return (buf.toString());
  }
}
