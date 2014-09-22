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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.Dimension;
import java.util.Collection;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;

/***************************************************************************
**
** Represent blocks of clusters with identical inputs
*/

public class ClusterBattery {

  TreeMap<String, GeneAndSatelliteCluster> clustersByName;
  Set<String> inputs;

  ClusterBattery() {
    clustersByName = new TreeMap<String, GeneAndSatelliteCluster>(String.CASE_INSENSITIVE_ORDER);
    inputs = new HashSet<String>();
  }

  void setSources(Set<String> srcs) {
    inputs.clear();
    inputs.addAll(srcs);
    return;
  }

  int getSize() {
    return (clustersByName.size());
  }

  Dimension getLayout() {
    int size = getSize();
    double squareSize = Math.ceil(Math.sqrt(size));
    return (new Dimension((int)squareSize, (int)squareSize));
  }    

  void addCluster(GeneAndSatelliteCluster cluster, Genome genome) {
    if (!cluster.inputSetMatches(inputs)) {
      throw new IllegalArgumentException();
    }
    Node cTarg = genome.getNode(cluster.getCoreID());
    String targName = cTarg.getName();
    clustersByName.put(targName, cluster);
    return;
  } 

  static Map<Set<String>, ClusterBattery> findBatteries(Collection<GeneAndSatelliteCluster> clusters, Genome genome, boolean dropSingletons) {
    HashMap<Set<String>, ClusterBattery> retval = new HashMap<Set<String>, ClusterBattery>();
    Iterator<GeneAndSatelliteCluster> cit = clusters.iterator();
    while (cit.hasNext()) {
      GeneAndSatelliteCluster cluster = cit.next();
      Set<String> cinputs = cluster.getInputs();  // Hashing by the input sets
      ClusterBattery cbat = retval.get(cinputs);
      if (cbat == null) {
        cbat = new ClusterBattery();
        cbat.setSources(cinputs);
        retval.put(cinputs, cbat);
      }
      cbat.addCluster(cluster, genome);
    }

    if (dropSingletons) {
      Set<Set<String>> retKeys = new HashSet<Set<String>>(retval.keySet());
      Iterator<Set<String>> rkit = retKeys.iterator();
      while (rkit.hasNext()) {
        Set<String> key = rkit.next();
        ClusterBattery battery = retval.get(key);
        if (battery.getSize() < 2) {
          retval.remove(key);
        }
      }
    }
    return (retval);
  }     
}
