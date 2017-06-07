/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Linkage;

/***************************************************************************
**
** Build dedicated fan-ins for target clusters
*/

public class FanBuilder {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public FanBuilder() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Fill out target clusters.  Keep sucking in bubbles that ultimately only
  ** hit the target, even if it is by way of other bubbles.
  */
  
  public Set<String> buildFanIn(SpecialtyLayoutEngine.NodePlaceSupport nps, String targetID, Map<String, Set<String>> sources, Set<Integer> okTypes) {
    
    //
    // Start with the target.  Find all sources, cull out nodes that
    // are not candidates (e.g. not bubbles).  Of candidates, find 
    // their targets.  If the target list contains nodes that are not
    // possible candidates, pitch the candidate 
    //
    //
    
    HashSet<String> candidates = new HashSet<String>();
    HashMap<String, Set<String>> targets = new HashMap<String, Set<String>>();
     
    //
    // List all the cluster candidates:
    //
    
    candidates.add(targetID);
    Iterator<Node> anit = nps.getAllNodeIterator();
    while (anit.hasNext()) {
      Node node = anit.next();
      Integer typeObj = new Integer(node.getNodeType());
      if (okTypes.contains(typeObj)) {
        candidates.add(node.getID());
      }
    }
    
    //
    // Get source and target lists:
    //
    
    Iterator<Linkage> lit = nps.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      Set<String> targsForSrc = targets.get(src);
      if (targsForSrc == null) {
        targsForSrc = new HashSet<String>();
        targets.put(src, targsForSrc);
      }
      targsForSrc.add(trg);
      
      Set<String> srcsForTarg = sources.get(trg);
      if (srcsForTarg == null) {
        srcsForTarg = new HashSet<String>();
        sources.put(trg, srcsForTarg);
      }
      srcsForTarg.add(src);
    }
    
    //
    // For each candidate in the set, throw it out if it has a target
    // that is known to not be a candidate.  Keep doing this until
    // we can't throw anybody else out.
    //
    
    int lastSize = candidates.size();
    int currSize = 0;
 
    while (lastSize != currSize) {    
      HashSet<String> newCands = new HashSet<String>();
      HashSet<String> testSet = new HashSet<String>();
      Iterator<String> cit = candidates.iterator();
      while (cit.hasNext()) {
        String cand = cit.next();
        if (cand.equals(targetID)) {
          newCands.add(cand);
          continue;
        }
        Set<String> candTargs = targets.get(cand);
        if ((candTargs == null) || candTargs.isEmpty()) {  // Ditch candidates with no targets...
          continue;
        }
        testSet.clear();
        testSet.addAll(candTargs);
        testSet.removeAll(candidates);
        if (!testSet.isEmpty()) {
          continue;
        }
        newCands.add(cand);
      }
      lastSize = candidates.size();
      candidates = newCands;
      currSize = candidates.size();
    }

    //
    // What about closed loops?  They will not rule each other out.
    // Build backwards from the target, using sources that are
    // surviving candidates.
    //

    ArrayList<String> queue = new ArrayList<String>();
    HashSet<String> emptySet = new HashSet<String>();
    HashSet<String> testSet = new HashSet<String>();
    HashSet<String> testSet2 = new HashSet<String>();    
    HashSet<String> survivors = new HashSet<String>();
    queue.add(targetID);
    survivors.add(targetID);
    while (!queue.isEmpty()) {
      String cand = queue.remove(0);
      Set<String> candSrcs = sources.get(cand);
      testSet.clear();
      testSet.addAll((candSrcs == null) ? emptySet : candSrcs);      
      testSet.retainAll(candidates);  // The intersection
      testSet2.clear();
      testSet2.addAll(testSet);
      testSet2.removeAll(survivors);
      queue.addAll(testSet2);  // only add if not seen already
      survivors.addAll(testSet);
    }
    
    return (survivors);
  }  
} 
