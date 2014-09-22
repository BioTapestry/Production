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

package org.systemsbiology.biotapestry.timeCourse;

/****************************************************************************
**
** Holds a region/hour tuple for time course tables.
*/

public class GeneTemplateEntry {

  public int time;
  public String region;  
  
  public GeneTemplateEntry(int time, String region) {
    this.time = time;
    this.region = region;
  }
  
  public GeneTemplateEntry(GeneTemplateEntry other) {
    this.time = other.time;
    this.region = other.region;
  }
  
  @Override
  public String toString() {
    return ("GeneTemplateEntry " + time + " " + region);
  }
  
  @Override
  public int hashCode() {
    return (time + region.hashCode());
  }
 
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    
    if (!(other instanceof GeneTemplateEntry)) {
      return (false);
    }
    
    GeneTemplateEntry otherGte = (GeneTemplateEntry)other;
    
    if (this.time != otherGte.time) {
      return (false);
    }
    
    if (this.region == null) {
      return (otherGte.region == null);
    }
    
    return (this.region.equals(otherGte.region)); 
  }
}
