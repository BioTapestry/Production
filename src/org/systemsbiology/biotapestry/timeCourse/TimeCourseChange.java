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

package org.systemsbiology.biotapestry.timeCourse;
 
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/***************************************************************************
**
** Info needed to undo a time course change
**
*/
  
public class TimeCourseChange {
  
  public static final int BASE_SERIAL    = 0;
  public static final int LINEAGE_SERIAL = 1;
  public static final int TOPO_SERIAL    = 2;
  public static final int MAP_SERIAL     = 3;
  
  public boolean forMaps;
  public long baseSerialNumberOrig;
  public long baseSerialNumberNew;
  public long linSerialNumberOrig;
  public long linSerialNumberNew;
  public long topoSerialNumberOrig;
  public long topoSerialNumberNew;
  public long mapSerialNumberOrig;
  public long mapSerialNumberNew;
  public String mapKey;
  public List<TimeCourseDataMaps.TCMapping> mapListOrig;
  public List<TimeCourseDataMaps.TCMapping> mapListNew;
  public List<GroupUsage> groupMapListOrig;
  public List<GroupUsage> groupMapListNew;  
  public TimeCourseGene gOrig;
  public TimeCourseGene gNew;
  public int genePos;
  public List<TimeCourseGene> allGenesOrig;
  public List<TimeCourseGene> allGenesNew;
  public Map<String, String> groupParentsOrig;
  public Map<String, String> groupParentsNew;
  public Set<String> groupRootsOrig;  
  public Set<String> groupRootsNew;
  public SortedMap<TimeCourseData.TopoTimeRange, TimeCourseData.RegionTopology> regionTopologiesOrig;  
  public SortedMap<TimeCourseData.TopoTimeRange, TimeCourseData.RegionTopology> regionTopologiesNew;
  public TimeCourseData.TopoRegionLocator topoLocatorOrig;  
  public TimeCourseData.TopoRegionLocator topoLocatorNew;
  
  public TimeCourseChange(int whichSerial, long serNum) {
     
    baseSerialNumberOrig = -1L;
    baseSerialNumberNew = -1L;
    linSerialNumberOrig = -1L;
    linSerialNumberNew = -1L;
    topoSerialNumberOrig = -1L;
    topoSerialNumberNew = -1L;
    mapSerialNumberOrig = -1L;
    mapSerialNumberNew = -1L;    
    
    forMaps = false;
    switch (whichSerial) {
      case BASE_SERIAL:
        baseSerialNumberOrig = serNum;
        return;
      case LINEAGE_SERIAL:
        linSerialNumberOrig = serNum;  
        return;
      case TOPO_SERIAL:
        topoSerialNumberOrig = serNum;
        return;
      case MAP_SERIAL:
        mapSerialNumberOrig = serNum;
        forMaps = true;
        return;
      default:
        throw new IllegalArgumentException();
    }
  } 
}
