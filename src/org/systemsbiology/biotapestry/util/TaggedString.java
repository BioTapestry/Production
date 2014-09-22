/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

/****************************************************************************
**
** Use where a "Null" String still needs a non-null placeholder
*/

public class TaggedString implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  public static final int UNTAGGED = -1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  public int tag;
  public String value;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedString() {
    this.tag = UNTAGGED;
    this.value = null;
  }       
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedString(int tag, String value) {
    this.tag = tag;
    this.value = value;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedString(TaggedString other) {
    this.tag = other.tag;
    this.value = other.value;
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Standard equals
  */

  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TaggedString)) {
      return (false);
    }
    TaggedString otherTS = (TaggedString)other;

    if (this.tag != otherTS.tag) {
      return (false);
    }
    
    if (this.value == null) {
      return (otherTS.value == null);
    }
    
    return (this.value.equals(otherTS.value));
  }
  
  /***************************************************************************
  **
  ** Standard hashcode
  */

  public int hashCode() {
    return (((value == null) ? 0 : value.hashCode()) + tag);
  }  
 
  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      TaggedString retval = (TaggedString)super.clone();
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();
    }
  }      

  /***************************************************************************
  **
  ** Standard toString
  */

  public String toString() {
    return ("TaggedString: tag = " + tag + " value = " + value);
  }
}
