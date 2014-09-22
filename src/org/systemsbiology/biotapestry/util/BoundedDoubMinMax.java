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

package org.systemsbiology.biotapestry.util;

public class BoundedDoubMinMax implements Cloneable {
  
  public double min;
  public boolean includeMin;
  public double max;
  public boolean includeMax;
  
  public BoundedDoubMinMax() {
    this(0.0);
  }

  public BoundedDoubMinMax(double val) {
    this(val, val);
  }  
  
  public BoundedDoubMinMax(double min, double max) {
    this(min, max, true, true);
  }
  
  public BoundedDoubMinMax(double min, double max, boolean includeMin, boolean includeMax) {
    this.min = min;
    this.max = max;
    this.includeMin = includeMin;
    this.includeMax = includeMax;   
  }
    
  public BoundedDoubMinMax(BoundedDoubMinMax other) {
    this.min = other.min;
    this.max = other.max;
    this.includeMin = other.includeMin;
    this.includeMax = other.includeMax;   
  }
  
  public boolean contained(double val) {
    boolean gtMin = (includeMin) ? (val >= min) : (val > min);
    boolean ltMax = (includeMax) ? (val <= max) : (val < max);
    return (gtMin && ltMax);
  } 
  
  public Object clone() {
    try {
      BoundedDoubMinMax newVal = (BoundedDoubMinMax)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }    
  }
  
  public int hashCode() {
    return ((int)Math.round(min + max) + ((includeMin) ? 0 : 1) + ((includeMax) ? 0 : 1));
  }
  
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof BoundedDoubMinMax)) {
      return (false);
    }
    BoundedDoubMinMax otherMM = (BoundedDoubMinMax)other;
    return ((this.min == otherMM.min) && (this.max == otherMM.max) && 
            (this.includeMin == otherMM.includeMin) && (this.includeMax == otherMM.includeMax));
  }  
 
  public String toString() {
    String open = (includeMin) ? "[" : "(";
    String close = (includeMax) ? "]" : ")";
    return (open + min + ", " + max + close);
  }
}
