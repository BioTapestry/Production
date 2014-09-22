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

package org.systemsbiology.biotapestry.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

/****************************************************************************
**
** Class to support getting http input on a background thread
*/

public class UrlRetrievalWorker implements Runnable {

  private String myResult_;
  private UrlRetrievalWorkerClient client_;
  private String theUrl_;
  private ResourceManager rMan_;
 
  public UrlRetrievalWorker(String theUrl, UrlRetrievalWorkerClient client, ResourceManager rMan) {
    myResult_ = "";
    client_ = client;
    theUrl_ = theUrl;
    rMan_ = rMan;
  }
 
  public void run() {
    try {
      myResult_ = getStringFromServer(theUrl_);
    } catch (IOException ex) {
      String format = rMan_.getString("urlPlug.ioError");
      myResult_ = MessageFormat.format(format, new Object[] {theUrl_});       
    } catch (Throwable oom) {
      String format = rMan_.getString("urlPlug.otherError");
      myResult_ = MessageFormat.format(format, new Object[] {theUrl_, oom.getMessage()}); 
    }
    client_.retrievedResult(theUrl_, myResult_);
    return;
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
    return (UiUtil.stripper(buf.toString()));
  }
} 
