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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.util.DataUtil;

public class ModelRegion implements Cloneable {
  private SortedSet<Integer> times_;
  private final String region_;
  private ArrayList<String> lineage_;
  
  public ModelRegion(SortedSet<Integer> times, String region, List<String> lineage) {
    this.times_ = DataUtil.fillOutHourly(times);
    this.region_ = region;
    this.lineage_ = new ArrayList<String>(lineage);
  }
       
  public List<String> getLineage() {
    return (new ArrayList<String>(lineage_));
  }
  
  public int getRegionStart() {
    return (times_.first().intValue());
  }
    
  public int getRegionEnd() {
    return (times_.last().intValue());
  }
  
  public String getRegionName() {
    return (region_);
  }
  
  public SortedSet<Integer> getTimes() {
    return (new TreeSet<Integer>(times_));
  }
      
  public String getLineageParent() {
    int numLim = lineage_.size();
    if (numLim < 2) {
      return (null);
    }
    return (lineage_.get(numLim - 2));
  }

  @Override
  public ModelRegion clone() {
    try {
      ModelRegion retval = (ModelRegion)super.clone();
      retval.times_ = new TreeSet<Integer>(this.times_);
      retval.lineage_ = new ArrayList<String>(this.lineage_);
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }    
}