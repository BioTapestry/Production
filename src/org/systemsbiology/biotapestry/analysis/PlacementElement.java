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

package org.systemsbiology.biotapestry.analysis;

import java.awt.Rectangle;

  /****************************************************************************
  **
  ** A class to hold elements to place
  */
  
public class PlacementElement implements Comparable<PlacementElement> {

  public Rectangle rect;
  public String id;
  private boolean widthFirst_;

  public PlacementElement(Rectangle rect, String id) {
    this.rect = rect;
    this.id = id;
    this.widthFirst_ = true;
  }
  
  public PlacementElement(Rectangle rect, String id, boolean widthFirst) {
    this.rect = rect;
    this.id = id;
    this.widthFirst_ = widthFirst;
  }  
 
  public int hashCode() {
    return (rect.hashCode());
  }

  public String toString() {
    return ("PlacementElement: " + rect + " " + id + " " + widthFirst_);
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof PlacementElement)) {
      return (false);
    }
    
    PlacementElement otherPe = (PlacementElement)other;
    
    if (this.widthFirst_ != otherPe.widthFirst_) {
      return (false);
    }    
    
    return (this.compareTo(otherPe) == 0);
  } 

  public int compareTo(PlacementElement other) {
    
    if (this.widthFirst_ != other.widthFirst_) {
      throw new IllegalArgumentException();
    }
    
    if (this.widthFirst_) {
      if (this.rect.width != other.rect.width) {
        return ((this.rect.width > other.rect.width) ? 1 : -1);
      }
    }

    if (this.rect.height != other.rect.height) {
      return ((this.rect.height > other.rect.height) ? 1 : -1);
    }

    if (!this.widthFirst_) {
      if (this.rect.width != other.rect.width) {
        return ((this.rect.width > other.rect.width) ? 1 : -1);
      }
    }
    
    return (this.id.compareTo(other.id));
  }
}