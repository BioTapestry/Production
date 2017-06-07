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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ModelRegionTopologyForTime implements Cloneable {

  private final int minTime_;
  private final int maxTime_;
  private ArrayList<String> regions_;
  private ArrayList<TopoLink> links_;

  public ModelRegionTopologyForTime(int minTime, int maxTime) {
    if ((minTime < 0) || (minTime > maxTime)) {
      throw new IllegalArgumentException();
    }
    this.minTime_ = minTime;
    this.maxTime_ = maxTime;
    this.regions_ = new ArrayList<String>();
    this.links_ = new ArrayList<TopoLink>();
  }    
   
  public ModelRegionTopologyForTime(int minTime, int maxTime, List<String> regions, List<TopoLink> links) {
    this(minTime, maxTime);
    this.regions_.addAll(regions);
    this.links_.addAll(links);
  }
  
  public Iterator<TopoLink> getLinks() {
    return (links_.iterator());
  }    
  
  public Iterator<String> getRegions() {
    return (regions_.iterator());
  }  
  
  public int getMinTime() {
    return (minTime_);
  }
  
  public int getMaxTime() {
    return (maxTime_);
  }  

  @Override
  public ModelRegionTopologyForTime clone() {
    try {
      ModelRegionTopologyForTime retval = (ModelRegionTopologyForTime)super.clone();
      retval.regions_ = new ArrayList<String>(this.regions_);
      retval.links_ = new ArrayList<TopoLink>();
      int numL = this.links_.size();
      for (int i = 0; i < numL; i++) {
        TopoLink tl = this.links_.get(i);
        retval.links_.add(tl.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Used to define topology links
  **
  */
  
  public static class TopoLink implements Cloneable {
    private final String region1_;
    private final String region2_;

    public TopoLink(String region1, String region2) {
      this.region1_ = region1;
      this.region2_ = region2;      
    }
    
    public String getRegion1() {
      return (region1_);
    }
  
    public String getRegion2() {
      return (region2_);
    } 

    @Override
    public TopoLink clone() {
      try {
        TopoLink retval = (TopoLink)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    @Override
    public int hashCode() {
      return (region1_.hashCode() + region2_.hashCode());
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TopoLink)) {
        return (false);
      }
      TopoLink otherTL = (TopoLink)other;
      
      return (this.region1_.equals(otherTL.region1_) && this.region2_.equals(otherTL.region2_));
    }
  }  
}