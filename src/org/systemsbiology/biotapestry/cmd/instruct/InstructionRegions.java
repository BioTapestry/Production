/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/****************************************************************************
**
** A class holding info on regions for a build instruction
*/

public class InstructionRegions implements Cloneable {
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private ArrayList<RegionTuple> tuples_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public InstructionRegions() {
    tuples_ = new ArrayList<RegionTuple>();
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public InstructionRegions(InstructionRegions other) {
    this.tuples_ = new ArrayList<RegionTuple>();
    int size = other.tuples_.size();
    for (int i = 0; i < size; i++) {
      this.tuples_.add(new RegionTuple(other.tuples_.get(i)));
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public InstructionRegions clone() {
    try {
      InstructionRegions retval = (InstructionRegions)super.clone();
      retval.tuples_ = new ArrayList<RegionTuple>();
      int size = this.tuples_.size();
      for (int i = 0; i < size; i++) {
        RegionTuple tup = this.tuples_.get(i);
        retval.tuples_.add(tup.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
   
  /***************************************************************************
  **
  ** Get tuple count
  */
  
  public int getNumTuples() {
    return (tuples_.size());
  }  
  
  /***************************************************************************
  **
  ** Get regions
  */
  
  public Iterator<RegionTuple> getRegionTuples() {
    return (tuples_.iterator());
  }

  /***************************************************************************
  **
  ** Set regions
  */
  
  public void setRegionTuples(List<RegionTuple> newTuples) {
    tuples_.clear();
    tuples_.addAll(newTuples);
    return;
  }
  
  /***************************************************************************
  **
  ** Merge with another IR
  */
  
  public void merge(InstructionRegions other) {
    int size = other.tuples_.size();
    for (int i = 0; i < size; i++) {
      this.tuples_.add(new RegionTuple(other.tuples_.get(i)));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Merge with another IR.  If a duplicate would be produced, it is instead
  ** inserted (nonuniquely) into the overflow.
  */
  
  public void mergeUniquely(InstructionRegions other, InstructionRegions overflow) {
    int size = other.tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple currTup = new RegionTuple(other.tuples_.get(i));
      if (!this.hasTuple(currTup)) {
        this.tuples_.add(currTup);
      } else {
        overflow.addRegionTuple(currTup);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Distribute the regions: Unused....
  */
  
  @SuppressWarnings("unused")
  public void distributeUniquely(InstructionRegions other, List<?> overflows) {

    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if we have duplicate tuple
  */
  
  public boolean hasDuplicates() {
    int size = tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple rt1 = tuples_.get(i);
      for (int j = 0; j < size; j++) {
        if (i == j) {
          continue;
        }
        RegionTuple rt2 = tuples_.get(j);
        if (rt2.equals(rt1)) {
          return (true);
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have the given tuple
  */
  
  public boolean hasTuple(RegionTuple rt) {
    int size = tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple rt1 = tuples_.get(i);
      if (rt.equals(rt1)) {
        return (true);
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Set region tuple
  */
  
  public void addRegionTuple(RegionTuple newTuple) {
    tuples_.add(newTuple);
    return;
  }  
  
  /***************************************************************************
  **
  ** Standard toString
  */
  
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("InstructionRegion: ["); 
    int size = tuples_.size();
    for (int i = 0; i < size; i++) {
      buf.append(tuples_.get(i).toString());
      if (i != (size - 1)) buf.append(" : " );
    }
    buf.append("]");
    return (buf.toString());
  }

  /***************************************************************************
  **
  ** Standard hashCode:
  **
  */
  
  @Override
  public int hashCode() {
    int retval = 0;
    int size = tuples_.size();
    for (int i = 0; i < size; i++) {
      retval += tuples_.get(i).hashCode();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Standard equals
  */  
  
  @Override  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof InstructionRegions)) {
      return (false);
    }
    InstructionRegions otherIR = (InstructionRegions)other;

    if (this.tuples_.size() != otherIR.tuples_.size()) {
      return (false);
    }
    
    //
    // May have duplicates, so we have to go both ways:
    //
    
    int size = this.tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple rt = this.tuples_.get(i);
      if (!otherIR.hasTuple(rt)) {
        return (false);
      }
    }
    
    size = otherIR.tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple rt = otherIR.tuples_.get(i);
      if (!this.hasTuple(rt)) {
        return (false);
      }
    }
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if the argument loses on of our tuples (it may also ADD tuples,
  ** but we don't care).
  */  
  
  public boolean losesRegion(InstructionRegions otherIR) {
    if (this == otherIR) {
      return (false);
    }

    //
    // Every tuple we have must be present in the argument.
    // We don't care about the reverse:
    //
    
    int size = this.tuples_.size();
    for (int i = 0; i < size; i++) {
      RegionTuple rt = this.tuples_.get(i);
      if (!otherIR.hasTuple(rt)) {
        return (true);
      }
    }
    
    return (false);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Used for region info
  */
  
  public static class RegionTuple implements Cloneable {
    
    public String sourceRegion;
    public String targetRegion;

    public RegionTuple(String sourceRegion, String targetRegion) {
      if (sourceRegion == null) {
        throw new IllegalArgumentException();
      }
      this.sourceRegion = sourceRegion;
      this.targetRegion = targetRegion;
    }
    
    public RegionTuple(RegionTuple other) {
      this.sourceRegion = other.sourceRegion;
      this.targetRegion = other.targetRegion;
    }
    
    public RegionTuple clone() {
      try {
        RegionTuple retval = (RegionTuple)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }    
      
    public String toString() {
      return (sourceRegion + " " + targetRegion);
    }
    
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof RegionTuple)) {
        return (false);
      }
      RegionTuple otherRT = (RegionTuple)other;
            
      if (!this.sourceRegion.equals(otherRT.sourceRegion)) {
        return (false);
      }
      
      if (this.targetRegion == null) {
        return (otherRT.targetRegion == null);
      }
      
      return (this.targetRegion.equals(otherRT.targetRegion));      
    }
    
    public int hashCode() {
      return (sourceRegion.hashCode() + ((targetRegion == null) ? 0 : targetRegion.hashCode()));
    }
  }
}
