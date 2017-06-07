/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.simulation;

/****************************************************************************
**
** A BioTapestry Link
** 
*/

public class ModelLink implements Cloneable {
  
  public enum Sign {POSITIVE, NEGATIVE, NONE};
    
  private final Sign sign_;
  private final String srcID_;
  private final String trgID_;
  private final String id_;
  
  public ModelLink(String id, String src, String trg, Sign sign) {
    if ((src == null) || (trg == null) || (id == null) || (sign == null)) {
      throw new IllegalArgumentException();
    }
    this.id_ = id;
    this.srcID_ = src;
    this.trgID_ = trg;
    this.sign_ = sign;
  }

  public ModelLink(ModelLink other) {
    this.srcID_ = other.srcID_;
    this.trgID_ = other.trgID_;
    this.sign_ = other.sign_;
    this.id_ = other.id_;
  }
  
  @Override
  public ModelLink clone() {
    try {
      return ((ModelLink)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
  
  public String getTrg() {
    return (trgID_);
  }

  public String getSrc() {
    return (srcID_);
  }
  
  public Sign getSign() {
    return (sign_);
  }
  
  public String getUniqueInternalID() {
    return (id_);
  } 

  @Override
  public int hashCode() {
    return (srcID_.hashCode() + trgID_.hashCode() + sign_.hashCode() + id_.hashCode());
  }

  @Override
  public String toString() {
    return ("src = " + srcID_ + " trg = " + trgID_ + " sign = " + sign_ + " id = " + id_);
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof ModelLink)) {
      return (false);
    }
    ModelLink otherLink = (ModelLink)other;
    if (!this.srcID_.equals(otherLink.srcID_)) {
      return (false);
    }
      
    if (!this.trgID_.equals(otherLink.trgID_)) {
      return (false);
    }

    if (!this.sign_.equals(otherLink.sign_)) {
      return (false);
    }
    
    return (this.id_.equals(otherLink.id_));   
  }
}
