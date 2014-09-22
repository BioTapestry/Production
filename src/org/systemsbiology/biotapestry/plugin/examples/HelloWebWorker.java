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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorker;
import org.systemsbiology.biotapestry.plugin.PluginCallbackWorkerClient;

/****************************************************************************
**
** Worker class for background retrieval
*/

public class HelloWebWorker implements PluginCallbackWorker {

  private String idKey_;
  private String theUrl_;
  private String desc_;
  private PluginCallbackWorkerClient client_;
 
  /***************************************************************************
  **
  ** Allows for dynamic instantiation
  */ 
    
  public HelloWebWorker() {
  }
 
  /***************************************************************************
  **
  ** Store args before run call
  */ 
  
  public void setArgs(String[] modelNameChain, String nodeName, String regionName) {
    StringBuffer buf = new StringBuffer();
    buf.append("<p></p>");
    buf.append("<center><h3>Example for External Node Data Display Plugin With Worker</h3></center>\n");
    
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
    buf.append("Node: <b>");
    buf.append(nodeName);
    buf.append("</b> in region <b>");  
    buf.append(regionName);
    buf.append("</b></p>\n");
    desc_ = buf.toString();    
    theUrl_ = "http://www.BioTapestry.org/developers/webPluginExample.html"; 
    return;
  }
 
  /***************************************************************************
  **
  ** Will be called by BioTapestry experimental data window framework
  */ 
   
  public void setIDAndClient(String idKey, PluginCallbackWorkerClient client) {
    idKey_ = idKey;
    client_ = client;
    return;
  }

  /***************************************************************************
  **
  ** Run on background thread
  */ 
  
  public void run() {
    String result = null;
    try {
      result = getStringFromServer(theUrl_);
    } catch (IOException ex) {   
      result = "IOException: " + theUrl_ + " " + ex.getMessage();       
    } catch (Throwable th) {
      result = "Other Exception: " + theUrl_ + " " + th.getMessage();      
    }
    // Note that the Client will transfer the data and install it via the
    // AWT thread!
    client_.retrievedResult(idKey_, result);
    return;
  }
  
  /***************************************************************************
  **
  ** Crude helper to make sure html, head, and body tags are thrown out
  */ 
  
  private String stripper(String target) { 
    
    //
    // Not very robust:
    //
    Pattern hOpen = Pattern.compile("<\\s*[Hh][Tt][Mm][Ll]\\s*>");
    Pattern hClose = Pattern.compile("</\\s*[Hh][Tt][Mm][Ll]\\s*>");
    Pattern bOpen = Pattern.compile("<\\s*[Bb][Oo][Dd][Yy]\\s*>");
    Pattern bClose = Pattern.compile("</\\s*[Bb][Oo][Dd][Yy]\\s*>");
    Pattern head = Pattern.compile("<\\s*[Hh][Ee][Aa][Dd]\\s*>\\s*</\\s*[Hh][Ee][Aa][Dd]\\s*>");
    
    Matcher m = hOpen.matcher(target);
    String retval = m.replaceAll("");
    m = head.matcher(retval);
    retval = m.replaceAll("");
    m = hClose.matcher(retval);
    retval = m.replaceAll("");
    m = bOpen.matcher(retval);
    retval = m.replaceAll("");
    m = bClose.matcher(retval);
    retval = m.replaceAll("");
    m = head.matcher(retval);
    retval = m.replaceAll("");
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the string from a web server
  */
  
  private String getStringFromServer(String urlString) throws IOException {
    StringBuffer buf = new StringBuffer();
    URL theUrl = new URL(urlString);
    HttpURLConnection urlConnection = (HttpURLConnection)theUrl.openConnection();
    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    String nextLine = null;
    while ((nextLine = br.readLine()) != null) {
      buf.append(nextLine);
    } 
    String fromServ = stripper(buf.toString());
    buf.setLength(0);
    buf.append(desc_);
    buf.append(fromServ);
    return (buf.toString());
  }
} 
