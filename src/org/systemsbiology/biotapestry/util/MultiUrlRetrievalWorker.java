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

import java.util.ArrayList;
import java.util.List;

/****************************************************************************
**
** Class to support getting http input on a background thread
*/

public class MultiUrlRetrievalWorker implements Runnable, UrlRetrievalWorkerClient {

  private String fakeKey_;
  private ArrayList myResults_;
  private UrlRetrievalWorkerClient client_;
  private ArrayList theUrls_;
  private ResourceManager rMan_;
 
  public MultiUrlRetrievalWorker(List theUrls, UrlRetrievalWorkerClient client, ResourceManager rMan) {
    client_ = client;
    theUrls_ = new ArrayList(theUrls);
    myResults_ = new ArrayList();
    rMan_ = rMan;
  }
  
  public void run() {
    try {
      StringBuffer buf = new StringBuffer();
      int numUrls = theUrls_.size();
      for (int i = 0; i < numUrls; i++) {
        myResults_.add("");
        buf.append((String)theUrls_.get(i));
      }
      fakeKey_ = buf.toString();
      for (int i = 0; i < numUrls; i++) {
        String url = (String)theUrls_.get(i);
        UrlRetrievalWorker worker = new UrlRetrievalWorker(url, this, rMan_);
        Thread runThread = new Thread(worker);
        runThread.start();
      }
      // To clear "pending" status:
      if (numUrls == 0) {
        client_.retrievedResult(fakeKey_, "");
      }
    } catch (Throwable ex) {
      System.err.println("Unexpected background thread exception in MultiUrlRetrievalWorker: " + ex.getMessage());
    } 
    return;
  }
  
  public void retrievedResult(String theUrl, String result) {
    final String thisResult;
    synchronized (this) {     
      int numUrls = theUrls_.size();    
      for (int i = 0; i < numUrls; i++) {
        String url = (String)theUrls_.get(i);
        if (url.equals(theUrl)) {
          myResults_.set(i, result);
          break;
        }
      }      
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < numUrls; i++) {
        String subresult = (String)myResults_.get(i);
        buf.append(subresult);
        if (i < (numUrls - 1)) {
          buf.append("<p></p><hr width=\"50%\" align=\"center\">");
        }      
      }
      thisResult = buf.toString();
    } 
    client_.retrievedResult(fakeKey_, thisResult);
  }  
} 
