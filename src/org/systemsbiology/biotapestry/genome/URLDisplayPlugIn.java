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

package org.systemsbiology.biotapestry.genome;

import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.plugin.InternalDataDisplayPlugInV2;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorkerClient;
import org.systemsbiology.biotapestry.util.MultiUrlRetrievalWorker;
import org.systemsbiology.biotapestry.util.UrlRetrievalWorkerClient;

/****************************************************************************
**
** Displays URLS for a gene or node
*/

public abstract class URLDisplayPlugIn {
  
  protected BTState appState_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.  Gotta be null!
  */

  public URLDisplayPlugIn() {
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
    return (isUrlListPerInstance(genomeID, itemID));
  }
  
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML() routine should only provide the initial data view.
  */
  
  public boolean haveCallbackWorker() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the worker that will gather up background data and call us back
  */
  
  public PluginCallbackWorker getCallbackWorker(String genomeID, String itemID) {
    List<String> urls = getUrlListFromArgs(genomeID, itemID);
    return (new URLDisplayWorker(urls, appState_));
  }
 
  /***************************************************************************
  **
  ** Get a string of valid HTML to display as experimental data for the desired item.
  */
  
  public String getDataAsHTML(String genomeID, String itemID) {
    List<String> urls = getUrlListFromArgs(genomeID, itemID);
    if (urls.isEmpty()) {
      return (" ");
    }
    return (appState_.getRMan().getString("dataWindow.waitingForResponse"));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get URL list
  */
  
  protected abstract List<String> getUrlListFromArgs(String genomeID, String itemId);
  
  /***************************************************************************
  **
  ** Answer if we are per-instance:
  */
  
  protected abstract boolean isUrlListPerInstance(String genomeID, String itemId); 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static class URLDisplayWorker implements PluginCallbackWorker, UrlRetrievalWorkerClient {
    
    private MultiUrlRetrievalWorker murw_;
    private PluginCallbackWorkerClient myClient_;
    private String myID_;
    
    public URLDisplayWorker(List<String> urls, BTState appState) {
      murw_ = new MultiUrlRetrievalWorker(urls, this, appState.getRMan());
    }
  
    public void run() {
      murw_.run();
      return;
    }
   
    public void setIDAndClient(String idKey, PluginCallbackWorkerClient client) {
      myID_ = idKey;
      myClient_ = client;
      return;
    }
      
    public void retrievedResult(String theUrl, String result) {
      myClient_.retrievedResult(myID_, result);
    }
  }
}
