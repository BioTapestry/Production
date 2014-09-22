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

import java.io.PrintWriter;
import java.util.List;

import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** Common genome item code.  Move towards implementing into abstract base class; for
** now, we will just use composition
*/

public class CommonGenomeItemCode {   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static String FT_TAG_ROOT = "FreeText";
  public final static String URLS_TAG_ROOT = "URLs";
  public final static String URL_TAG_ROOT = "URL";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** support
  **
  */
  
  public void writeDescUrlToXML(PrintWriter out, Indenter ind, List<String> urls, String desc, String tagPrefix) {
    String ftTag = tagPrefix + FT_TAG_ROOT;
    String urlsTag = tagPrefix + URLS_TAG_ROOT;
    String urlTag = tagPrefix + URL_TAG_ROOT;
    int numURL = urls.size();
    if (desc != null) {
      ind.up().indent();  
      out.print("<");
      out.print(ftTag);
      out.print(">");
      out.print(CharacterEntityMapper.mapEntities(desc, false));
      out.print("</");
      out.print(ftTag);
      out.println(">");
      ind.down();   
    }
    if (numURL > 0) {
      ind.up().indent();  
      out.print("<");
      out.print(urlsTag);
      out.println(">");
      ind.up();
      for (int i = 0; i < numURL; i++) {
        String url = urls.get(i);
        ind.indent();  
        out.print("<");
        out.print(urlTag);
        out.print(">");
        out.print(CharacterEntityMapper.mapEntities(url, false));
        out.print("</");
        out.print(urlTag);
        out.println(">");
      }
      ind.down().indent();
      out.print("</");
      out.print(urlsTag);
      out.println(">");
      ind.down();   
    }          
    return;
  }
  
  /***************************************************************************
  **
  ** For sharing state
  */
  
  CommonGenomeItemCode() {
  }     
}