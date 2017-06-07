/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
  
public class SimulatorPlugInDirective extends AbstractPlugInDirective {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final String SIMULATOR_TAG = "Simulator";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final String SIM_PLUGIN_TAG_ = "simulatorPlugIn";
  
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
  
  public SimulatorPlugInDirective(String type, String className, String order, File jarFile) {
    super(type, className, order, jarFile);
  }  

  /***************************************************************************
  **
  ** Constructor for XML input
  **
  */
  
  private SimulatorPlugInDirective() {
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
  
  @Override
  public String toString() { 
    return ("SimulatorPlugInDirective: " + mapToTypeTag(type_) + " " + className_ + " " + order_);
  }
  
  /***************************************************************************
  **
  ** Implement comparable interface:
  **
  */
  
  @Override
  public int compareTo(AbstractPlugInDirective other) {
    return (super.compareTo(other));
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
    retval.add(SIM_PLUGIN_TAG_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static SimulatorPlugInDirective buildFromXML(String elemName, 
                                             Attributes attrs) throws IOException {
                                                
    if (!elemName.equals(SIM_PLUGIN_TAG_)) {
      throw new IllegalArgumentException();
    }
    SimulatorPlugInDirective retval = new SimulatorPlugInDirective();
    retval.stockCoreFromXML(elemName, attrs);
    return (retval); 
  }

  /***************************************************************************
  **
  ** Map types
  */

  public String mapToTypeTag(DirType val) {   
    switch (val) {
      case SIMULATION:
        return (SIMULATOR_TAG); 
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Map types to values
  */

  public DirType mapFromTypeTag(String tag) {
    if (tag.equalsIgnoreCase(SIMULATOR_TAG)) {
      return (DirType.SIMULATION); 
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
