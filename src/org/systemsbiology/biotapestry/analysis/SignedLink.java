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

package org.systemsbiology.biotapestry.analysis;

import org.systemsbiology.biotapestry.genome.Linkage;

/****************************************************************************
**
** A Class
** For signs use case Linkage.NEGATIVE, Linkage.POSITIVE, Linkage.NONE
 * 
*/

public class SignedLink extends Link implements Cloneable, Comparable<Link> {
  protected int sign_;

  public SignedLink(String src, String trg, int sign) {
    super(src, trg);
    sign_ = sign;
  }

  public SignedLink(SignedLink other) {
    super(other);
    this.sign_ = other.sign_;
  }
  
  public int getSign() {
    return (sign_);
  }  
  
  @Override
  public SignedLink clone() {
    return ((SignedLink)super.clone());
  }      
  
  @Override
  public int hashCode() {
    return (super.hashCode() + sign_);
  }

  @Override
  public String toString() {
    return (super.toString() + " sign = " + sign_);
  }
  
  public String signSymbol() {
    switch (sign_) {
      case Linkage.NEGATIVE:
        return ("-|");
      case Linkage.POSITIVE:
        return ("->");
      case Linkage.NONE:
        return ("--");
      default:
        throw new IllegalArgumentException();
    }  
  }
  
  @Override
  public boolean equals(Object other) {
    if (!super.equals(other)) {
      return (false);
    }
    if (!(other instanceof SignedLink)) {
      return (false);
    }
    SignedLink otherLink = (SignedLink)other;    
        
    return (this.sign_ == otherLink.sign_);
  }
  
  public int compareTo(SignedLink otherLink) {   
    int supRes = super.compareTo(otherLink);
    if (supRes != 0) {
      return (supRes);
    }
    return (this.sign_ - otherLink.sign_);
  } 
}
