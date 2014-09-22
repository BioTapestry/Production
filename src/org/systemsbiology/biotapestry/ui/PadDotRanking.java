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

package org.systemsbiology.biotapestry.ui;

/****************************************************************************
**
** This ranks landing pads by dot product
*/

public class PadDotRanking implements Comparable<PadDotRanking> {
  
  public int padNum;
  public double dot;

  public PadDotRanking(int padNum, double dot) {
    this.padNum = padNum;
    this.dot = dot;
  }
  
  public int hashCode() {
    return ((int)(dot * 10000.0));
  }

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof PadDotRanking)) {
      return (false);
    }
    return (this.compareTo((PadDotRanking)other) == 0);
  }   

  public int compareTo(PadDotRanking other) {
    double diff = this.dot - other.dot;
    if (diff < 0.0) {
      return (-1);
    } else if (diff > 0.0) {
      return (1);
    } else {
      return ((new Integer(padNum)).compareTo(new Integer(other.padNum)));
    }
  }  
  
  public String toString() {
    return ("PadDotRanking: pad = " + padNum + " dot = " + dot);
  }
  
  
}  
