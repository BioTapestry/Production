/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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


import org.systemsbiology.biotapestry.plugin.ExternalNodeDataDisplayPlugIn;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;

/****************************************************************************
**
** Example plugin that uses a worker to retrieve info on a background thread
** which then calls back when done.
*/

public class HelloWebPluginWithWorker implements ExternalNodeDataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HelloWebPluginWithWorker() {
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
    return (ExternalNodeDataDisplayPlugIn.IS_INTERNAL_ANSWER);
  }
    
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String dbID, String[] modelNameChain, String nodeName, String regionName) {
    return (false);
  }
 
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML routine above should only provide an "initial" data view.
  */
  
  public boolean haveCallbackWorker() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the callback worker 
  */
  
  public PluginCallbackWorker getCallbackWorker(String dbID, String[] modelNameChain, String nodeName, String regionName) {
    //
    // If this HelloWebPluginWithWorker class is loaded in from a jar file in the plugins directory,
    // the HelloWebWorker will be found by the class loader if it too comes in from the same jar file.
    // No magic jar extraction should be needed
    //
    HelloWebWorker hww = new HelloWebWorker();  
    hww.setArgs(modelNameChain, nodeName, regionName);
    return (hww);
  }
  
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html head or body tags, please!) to display
  ** as experimental data for the desired node.  The specified region may 
  ** be null for the top-level VfG.  The model name chain
  ** provides the names of all the models down from the root ("Full Genome") 
  ** Since this is plugin is worker-based, we will just return an initial string:
  */
  
  public String getDataAsHTML(String dbID, String[] modelNameChain, String nodeName, String regionName) {
    return ("<p>Waiting for response....</p>");   
  }
} 
