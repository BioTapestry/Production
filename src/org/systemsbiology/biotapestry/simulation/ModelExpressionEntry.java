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
** A BioTapestry expression table entry
** 
*/

public class ModelExpressionEntry implements Cloneable {

  public enum Level {NO_REGION, NO_DATA, NOT_EXPRESSED, WEAK_EXPRESSION, EXPRESSED, VARIABLE};
  public enum Source {NO_SOURCE_SPECIFIED, MATERNAL_SOURCE, ZYGOTIC_SOURCE, MATERNAL_AND_ZYGOTIC};
  
  private final Level lev_;
  private final Source src_;
  private final String region_;
  private final int time_;
  public final Double varVal_;
  
  public ModelExpressionEntry(String region, int time, Level lev, Source src, Double varVal) {
    this.region_ = region;
    this.time_ = time;
    this.lev_ = lev;
    this.src_ = src;
    this.varVal_ = varVal;
  }

  public ModelExpressionEntry(ModelExpressionEntry other) {
    this.region_ = other.region_;
    this.time_ = other.time_;
    this.lev_ = other.lev_;
    this.src_ = other.src_;
    this.varVal_ = other.varVal_;
  }
  
  @Override
  public ModelExpressionEntry clone() {
    try {
      return ((ModelExpressionEntry)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
  
  public Level getLevel() {
    return (lev_);
  }

  public String getRegion() {
    return (region_);
  }
  
  public int getTime() {
    return (time_);
  }
  
  public Source getSource() {
    return (src_);
  }
    
  public Double getVariable() {
    if (lev_ != Level.VARIABLE) {
      throw new IllegalStateException();
    }
    return (varVal_);
  }
  
  @Override
  public int hashCode() {
    return (region_.hashCode() + time_  + lev_.hashCode());
  }

  @Override
  public String toString() {
    return ("reg = " + region_ + " time = " + time_ + " level = " + lev_ + " src = " + src_ + " varVal = " + varVal_);
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof ModelExpressionEntry)) {
      return (false);
    }
    ModelExpressionEntry otherEntry = (ModelExpressionEntry)other;
    if (!this.region_.equals(otherEntry.region_)) {
      return (false);
    }
      
    if (!this.lev_.equals(otherEntry.lev_)) {
      return (false);
    }
    
    if (!this.src_.equals(otherEntry.src_)) {
      return (false);
    }
    
    if (this.time_ != otherEntry.time_) {
      return (false);
    }
    
    if (this.varVal_ == null) {
      return (otherEntry.varVal_ == null);
    }
    
    return (this.varVal_.equals(otherEntry.varVal_));   
  }
}
