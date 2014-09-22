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

/****************************************************************************
**
** If sorting by integer, have a backup ID to break ties.
*/

public class TaggedComparableInteger implements Comparable<TaggedComparableInteger> {
  
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
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Integer value_;
  private String uniqueTieBreaker_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedComparableInteger(Integer value, String uniqueTieBreaker) {
    value_ = value;
    uniqueTieBreaker_ = uniqueTieBreaker;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  public int hashCode() {
    return (value_.hashCode() + uniqueTieBreaker_.hashCode());
  }

  public String toString() {
    return (value_ + ": " + uniqueTieBreaker_);
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof TaggedComparableInteger)) {
      return (false);
    }
    return (this.compareTo((TaggedComparableInteger)other) == 0);
  } 

  public int compareTo(TaggedComparableInteger other) {
    if (!this.value_.equals(other.value_)) {
      return (value_.compareTo(other.value_));
    }
    return (this.uniqueTieBreaker_.compareTo(other.uniqueTieBreaker_));
  } 
}
