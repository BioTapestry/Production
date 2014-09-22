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

package org.systemsbiology.biotapestry.plugin;

import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;

/***************************************************************************
**
** Used to specify plugins that handle links
*/
  
public class LinkPlugInDirective extends AbstractPlugInDirective {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final String INTERNAL_LINK_DATA_DISPLAY_TAG = "InternalLinkDataDisplay";
  public static final String EXTERNAL_LINK_DATA_DISPLAY_TAG = "ExternalLinkDataDisplay";   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String LINK_PLUGIN_TAG_ = "linkPlugIn";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor for internal use
  **
  */
  
  public LinkPlugInDirective(String type, String className, String order, File jarFile) {
    super(type, className, order, jarFile);
  }  

  /***************************************************************************
  **
  ** Constructor for XML input
  **
  */
  
  private LinkPlugInDirective() {
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Standard toString:
  **
  */
  
  public String toString() { 
    return ("LinkPlugInDirective: " + mapToTypeTag(type_) + " " + className_ + " " + order_);
  }
  
  /***************************************************************************
  **
  ** Implement comparable interface:
  **
  */
    
  public int compareTo(Object o) {
    LinkPlugInDirective other = (LinkPlugInDirective)o;
    return (super.compareTo(o));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(LINK_PLUGIN_TAG_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static LinkPlugInDirective buildFromXML(String elemName, 
                                             Attributes attrs) throws IOException {
                                                
    if (!elemName.equals(LINK_PLUGIN_TAG_)) {
      throw new IllegalArgumentException();
    }
    LinkPlugInDirective retval = new LinkPlugInDirective();
    retval.stockCoreFromXML(elemName, attrs);
    return (retval); 
  }

  /***************************************************************************
  **
  ** Map types
  */

  public String mapToTypeTag(int val) {   
    switch (val) {
      case INTERNAL_DATA_DISPLAY:
        return (INTERNAL_LINK_DATA_DISPLAY_TAG); 
      case EXTERNAL_DATA_DISPLAY:
        return (EXTERNAL_LINK_DATA_DISPLAY_TAG); 
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Map types to values
  */

  public int mapFromTypeTag(String tag) {
    if (tag.equalsIgnoreCase(INTERNAL_LINK_DATA_DISPLAY_TAG)) {
      return (INTERNAL_DATA_DISPLAY); 
    } else if (tag.equalsIgnoreCase(EXTERNAL_LINK_DATA_DISPLAY_TAG)) {
      return (EXTERNAL_DATA_DISPLAY); 
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
}
