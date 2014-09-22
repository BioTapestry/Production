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

package org.systemsbiology.biotapestry.ui.layouts;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;

/***************************************************************************
**
** Annotates the links for a super cluster
*/

public class LinkAnnotatedSuper {
    
  public SuperSourceCluster ssc;
  public HashSet<String> allInputs;
  public HashSet<String> allOutputs;  
  public HashSet<String> pastInputs;
  public HashSet<String> immediateInputs;  
  public HashSet<String> forwardOutputs;
  public HashSet<String> immediateOutputs;      
  public HashSet<String> backwardOutputs;
  public HashSet<String> backwardInputs;
  public HashSet<String> backwardSources;
  public HashSet<String> pureBackwardOutputSources;
  public Map<String, List<GeneAndSatelliteCluster.TagAnnotatedList<GeneAndSatelliteCluster.InboundCorners>>> deferredTerminalPaths;
 
  // using these fields for multi-series stacks.  FIX ME: Separate class?
  public int srcRow;
  public int srcCol;
  public HashSet<String> beforeRowSources;
  public HashSet<String> afterRowSources;
  public HashSet<String> beforeRowTargets;
  public HashSet<String> afterRowTargets;

  public LinkAnnotatedSuper(SuperSourceCluster ssc) {
    this.ssc = ssc;
    allInputs = new HashSet<String>();  // set to null these days...
    allOutputs = new HashSet<String>(); // set to null these days...
    pastInputs = new HashSet<String>();
    immediateInputs = new HashSet<String>();
    forwardOutputs = new HashSet<String> ();
    immediateOutputs = new HashSet<String> ();     
    backwardOutputs = new HashSet<String> ();
    backwardInputs = new HashSet<String>();
    backwardSources = new HashSet<String>();
    pureBackwardOutputSources = new HashSet<String>();
    
    beforeRowSources = new HashSet<String>();
    afterRowSources = new HashSet<String>();
    beforeRowTargets = new HashSet<String>();
    afterRowTargets = new HashSet<String>();
  }
  
  /***************************************************************************
  ** 
  ** get forward looking sources
  */
  
  public Set<String> forwardSources(SpecialtyLayoutEngine.NodePlaceSupport nps) {      
    HashSet<String> onlySrcs = new HashSet<String>();
    Iterator<String> foit = forwardOutputs.iterator();
    while (foit.hasNext()) {
      String linkID = foit.next();
      Linkage link = nps.getLinkage(linkID);
      onlySrcs.add(link.getSource());
    }
    Iterator<String> ioit = immediateOutputs.iterator();
    while (ioit.hasNext()) {
      String linkID = ioit.next();
      Linkage link = nps.getLinkage(linkID);
      onlySrcs.add(link.getSource());
    }
    return (onlySrcs);
  } 
  
  /***************************************************************************
  ** 
  ** get backward looking sources
  */
  
  public Set<String> backwardSources(SpecialtyLayoutEngine.NodePlaceSupport nps) {      
    HashSet<String> onlySrcs = new HashSet<String>();
    Iterator<String> foit = backwardOutputs.iterator();
    while (foit.hasNext()) {
      String linkID = foit.next();
      Linkage link = nps.getLinkage(linkID);
      onlySrcs.add(link.getSource());
    }
    return (onlySrcs);
  } 
} 
