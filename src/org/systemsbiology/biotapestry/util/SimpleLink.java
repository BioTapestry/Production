/*
**    Copyright (C) 2003-2004 Institute for Systems Biology 
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
** A class for modeling links in networks
*/

public class SimpleLink {

  private String src_;
  private String trg_;

  public SimpleLink(String src, String trg) {
    if ((src == null) || (trg == null)) {
      throw new IllegalArgumentException();
    }
    this.src_ = src;
    this.trg_ = trg;
  }

  public SimpleLink(SimpleLink other) {
    this.src_ = other.src_;
    this.trg_ = other.trg_;
  }    

  public String getTrg() {
    return (trg_);
  }

  public String getSrc() {
    return (src_);
  }    

  public int hashCode() {
    return (src_.hashCode() + trg_.hashCode());
  }

  public String toString() {
    return ("src = " + src_ + " trg = " + trg_);
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof SimpleLink)) {
      return (false);
    }
    SimpleLink otherLink = (SimpleLink)other;
    return (this.src_.equals(otherLink.src_) && this.trg_.equals(otherLink.trg_));
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
