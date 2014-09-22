/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.plugin.ExternalDataDisplayPlugIn;

/****************************************************************************
**
** A Class
*/

public class HelloWorldPlugIn implements ExternalDataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HelloWorldPlugIn() {
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
    key = ((key == null) || (key.trim().equals(""))) ? "\" \"" : key;
    buf.append("<p></p>");
    buf.append("<center>");
    buf.append("<center><h3>Example Display Plugin for ");
    buf.append(key);
    buf.append("</h3></center>\n");
    buf.append("<p></p>");
    buf.append("<b>Hello ");
    buf.append(key);    
    buf.append("!</b>");
    buf.append("<p></p>");
    return (buf.toString());
  }
}
