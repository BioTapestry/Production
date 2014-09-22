/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.perturb;

/***************************************************************************
**
** Info needed to build a preliminary link from perturbation data
**
*/
  
public class LinkCandidate implements Cloneable {
  public String srcID;
  public String targID;
  public int sign;
  
  public LinkCandidate(String srcID, String targID, int sign) {
    this.srcID = srcID;
    this.targID = targID;
    this.sign = sign;
  }
  
  public LinkCandidate(LinkCandidate other) {
    this.srcID = other.srcID;
    this.targID = other.targID;
    this.sign = other.sign;
  }
  
  public LinkCandidate(LinkCandidate other, int newSign) {
    this.srcID = other.srcID;
    this.targID = other.targID;
    this.sign = newSign;
  }
  
   public Object clone() {
    try {
      LinkCandidate retval = (LinkCandidate)super.clone();
      return (retval);            
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }   
  
  public int hashCode() {
    return (srcID.hashCode() + targID.hashCode() + sign);
  }
  
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof LinkCandidate)) {
      return (false);
    }
    LinkCandidate otherLC = (LinkCandidate)other;
    
    if (!this.srcID.equals(otherLC.srcID)) {
      return (false);
    }
    if (!this.targID.equals(otherLC.targID)) {
      return (false);
    }

    return (this.sign == otherLC.sign);
  }  

  //
  // Ignore the sign difference:
  //
  public boolean sameEndpoints(LinkCandidate other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!this.srcID.equals(other.srcID)) {
      return (false);
    }
    
    if (!this.targID.equals(other.targID)) {
      return (false);
    }
    return (true);
  }
}
