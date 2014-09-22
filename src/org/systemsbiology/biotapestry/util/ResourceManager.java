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

import java.util.ResourceBundle;
import java.util.MissingResourceException;

/****************************************************************************
**
** Resource Manager.
*/

public class ResourceManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ResourceBundle bundle_; 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null Constructor
  */

  public ResourceManager() {
    bundle_ = ResourceBundle.getBundle("org.systemsbiology.biotapestry.util.BioTapestry");
  }
  
  /***************************************************************************
  **
  ** Bundle-name Constructor
  */

  public ResourceManager(String bundleName) {
    bundle_ = ResourceBundle.getBundle(bundleName);
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get a resource String
  */

  public String getString(String key) {
    String retval;
    
    try {
      retval = bundle_.getString(key);
    } catch (MissingResourceException mre) {
      retval = key;
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get a resource Character
  */

  public char getChar(String key) {

    String str;
    try {
      str = bundle_.getString(key);
    } catch (MissingResourceException mre) {
      str = "!";
    }
    if (str.length() == 0) {
      str = "!";
    }
    return (str.charAt(0));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    ResourceManager rm = new ResourceManager();
    String test = rm.getString("testKey");
    System.out.println("Test key returns: " + test);
  }
}
