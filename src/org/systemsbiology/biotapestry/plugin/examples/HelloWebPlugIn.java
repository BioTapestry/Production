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

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.systemsbiology.biotapestry.plugin.ExternalDataDisplayPlugIn;

/****************************************************************************
**
** A Class
*/

public class HelloWebPlugIn implements ExternalDataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HelloWebPlugIn() {
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
    return (ExternalDataDisplayPlugIn.IS_INTERNAL_ANSWER);
  }
  
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or body tags, please!) to display
  ** as experimental data for the desired item.
  */
  
  public String getDataAsHTML(String key) {
    StringBuffer buf = new StringBuffer();
    buf.append("<p></p>");   
    buf.append("<center><h1>Example Web Data Display Plugin for ");
    buf.append(((key == null) || (key.trim().equals(""))) ? "\" \"" : key);
    buf.append("</h1></center>\n");
    buf.append("<p></p>");
    buf.append(getStringFromServer());
    return (buf.toString());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** A helper to make sure html and body tags are thrown out
  */ 
  
  private String stripper(String target) { 
    //
    // Not very robust.  Only good for plain tags.  Make this better!
    //
    Pattern hOpen = Pattern.compile("<\\s*[Hh][Tt][Mm][Ll]\\s*>");
    Pattern hClose = Pattern.compile("</\\s*[Hh][Tt][Mm][Ll]\\s*>");
    Pattern bOpen = Pattern.compile("<\\s*[Bb][Oo][Dd][Yy]\\s*>");
    Pattern bClose = Pattern.compile("</\\s*[Bb][Oo][Dd][Yy]\\s*>");
    Matcher m = hOpen.matcher(target);
    String retval = m.replaceAll("");
    m = hClose.matcher(retval);
    retval = m.replaceAll("");
    m = bOpen.matcher(retval);
    retval = m.replaceAll("");
    m = bClose.matcher(retval);
    retval = m.replaceAll("");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the string from a web server
  */
  
  private String getStringFromServer() {
    StringBuffer buf = new StringBuffer();
    try {
      String testUrlStr = "http://www.BioTapestry.org/developers/webPluginExample.html";      
      URL testUrl = new URL(testUrlStr);
      HttpURLConnection urlConnection = (HttpURLConnection)testUrl.openConnection();
      BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      String nextLine = null;
      while ((nextLine = br.readLine()) != null) {
        buf.append(nextLine);
      }
    } catch(IOException ioEx)  {
      System.err.println(ioEx);
    } 
    return (stripper(buf.toString()));
  }
} 
