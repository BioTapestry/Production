/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.ui.Layout;

/***************************************************************************
**
** Data used for specialty layout
*/

public class SpecialtyLayoutData {
    
  public GenomeSubset subset;
  public Genome genome;
  public Layout lo;
  public SpecialtyLayoutEngineParams param;
  public SpecialtyInstructions results;
  public List<GeneAndSatelliteCluster> gASCs;

  public SpecialtyLayoutEngine.NodePlaceSupport nps;
  public SortedMap<Integer, String> existingOrder;
  public Set<String> pureTargets;
  public Set<String> nodeSet;
  public StaticDataAccessContext rcx;

  public SpecialtyLayoutData(GenomeSubset subset, StaticDataAccessContext rcx,
                             SpecialtyLayoutEngineParams param, SpecialtyLayoutEngine.GlobalSLEState gss, 
                             SortedMap<Integer, String> customOrder, 
                             Set<String> pureTargets, Set<String> nodeSet) {
    this.subset = subset;
    genome = subset.getBaseGenome();
    this.lo = rcx.getCurrentLayout();
    this.param = param;
    existingOrder = customOrder;
    results = new SpecialtyInstructions(gss.getGlobalPadChanges());
    gASCs = new ArrayList<GeneAndSatelliteCluster>();
    this.pureTargets = pureTargets;
    this.nodeSet = nodeSet;
    nps = new SpecialtyLayoutEngine.NodePlaceSupport(genome, lo, results.nodeLocations, gss.getGlobalPadChanges(), gss.getInvertedSrcTrg());
    this.rcx = rcx;
  }
  
  public SpecialtyLayoutData( GenomeSubset subset, StaticDataAccessContext rcx, SpecialtyLayoutEngineParams param, InvertedSrcTrg ist) {
    this.subset = subset;
    genome = subset.getBaseGenome();
    this.lo = rcx.getCurrentLayout();
    this.param = param;
    existingOrder = null;
    results = null;
    gASCs = new ArrayList<GeneAndSatelliteCluster>();
    this.pureTargets = null;
    this.nodeSet = new HashSet<String>();
    nps = new SpecialtyLayoutEngine.NodePlaceSupport(genome, lo, null, null, ist);
    this.rcx = rcx;
  }
} 
