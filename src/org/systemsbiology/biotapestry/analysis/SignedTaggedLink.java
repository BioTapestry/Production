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
** Extend the Signed Link with a tag, e.g. a real link ID. This allows us to
** send lightweight link definitions over the wire.
*/

public class SignedTaggedLink extends SignedLink implements Cloneable, Comparable<Link> {
  protected String tag_;

  public SignedTaggedLink(String src, String trg, int sign, String tag) {
    super(src, trg, sign);
    if (tag == null) {
      throw new IllegalArgumentException();
    }
    tag_ = tag;
  }

  public SignedTaggedLink(Linkage link) {
    super(link.getSource(), link.getTarget(), link.getSign());
    this.tag_ = link.getID();
  }
  
  
  public SignedTaggedLink(SignedTaggedLink other) {
    super(other);
    this.tag_ = other.tag_;
  }
  
  public String getTag() {
    return (tag_);
  }  
  
  @Override
  public SignedTaggedLink clone() {
    return ((SignedTaggedLink)super.clone());
  }      
  
  @Override
  public int hashCode() {
    return (super.hashCode() + tag_.hashCode());
  }

  @Override
  public String toString() {
    return (super.toString() + " tag = " + tag_);
  }
  
  
  @Override
  public boolean equals(Object other) {
    if (!super.equals(other)) {
      return (false);
    }
    if (!(other instanceof SignedTaggedLink)) {
      return (false);
    }
    SignedTaggedLink otherLink = (SignedTaggedLink)other;    
        
    return (this.tag_.equals(otherLink.tag_));
  }
  
  public int compareTo(SignedTaggedLink otherLink) {   
    int supRes = super.compareTo(otherLink);
    if (supRes != 0) {
      return (supRes);
    }
    return (this.tag_.compareTo(otherLink.tag_));
  } 
}
