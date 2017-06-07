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

package org.systemsbiology.biotapestry.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;

/****************************************************************************
**
** Handles source/target selection activities
*/

public class SourceAndTargetSelector {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Search Types
  */ 
  
  public enum Searches {TARGET_SELECT, SOURCE_SELECT, DIRECT_SELECT}
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private StaticDataAccessContext rcx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SourceAndTargetSelector(StaticDataAccessContext rcx) {
    rcx_ = rcx;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  **
  ** Do the selection based on the provided criteria judge
  ** 
  */
  
  public SearchResult doCriteriaSelection(String nodeID, Searches selectType, 
                                          boolean includeLinks, GraphSearcher.CriteriaJudge judge) {
    SearchResult retval = new SearchResult(includeLinks);
    HashSet<String> startNodes = new HashSet<String>();
    startNodes.add(nodeID);
    switch (selectType) {
      case DIRECT_SELECT:
        retval.found.addAll(startNodes);
        break;
      case TARGET_SELECT:
        retval.found.addAll(findCriteriaSelections(startNodes, false, judge));
        break;
      case SOURCE_SELECT:
        retval.found.addAll(findCriteriaSelections(startNodes, true, judge));
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Do the selection
  ** 
  */
  
  public SearchResult doSelection(Set<String> nodeIDs, Searches selectType, boolean includeItem, boolean includeLinks) {
    SearchResult retval = new SearchResult(includeLinks);
    HashSet<String> linksFromSources = (includeLinks) ? new HashSet<String>() : null;
    switch (selectType) {
      case DIRECT_SELECT:
        retval.found.addAll(nodeIDs);
        break;
      case TARGET_SELECT:
        Iterator<Linkage> lit = rcx_.getCurrentGenome().getLinkageIterator();
        // Using a map of link intersections PER SOURCE fixes BT-02-04-08:1
        HashMap<String, Intersection> linkInters = new HashMap<String, Intersection>();
        while (lit.hasNext()) {
          Linkage link = lit.next();
          String src = link.getSource();
          if (nodeIDs.contains(src)) {           
            retval.found.add(link.getTarget());
            if (includeItem) {
              retval.found.add(src);
            }
            //
            // Any linkID at all serves the needs of the intersection:
            //
            if (includeLinks && (linkInters.get(src) == null)) {
              Intersection aLinkInter = new Intersection(link.getID(), null, 0.0, true);
              linkInters.put(src, aLinkInter);
              retval.linkIntersections.add(aLinkInter);
            }
          }
        }
        break;
      case SOURCE_SELECT:
        Iterator<Linkage> slit = rcx_.getCurrentGenome().getLinkageIterator();
        while (slit.hasNext()) {
          Linkage link = slit.next();
          String targ = link.getTarget();
          if (nodeIDs.contains(targ)) {
            retval.found.add(link.getSource());
            if (includeItem) {
              retval.found.add(targ);
            }
            if (includeLinks) {
              linksFromSources.add(link.getID());
            }
          }
        }
        if (includeLinks) {
          retval.linkIntersections = rcx_.getCurrentLayout().getIntersectionsForLinks(rcx_, linksFromSources, false);
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Do the selection
  ** 
  */
  
  public SearchResult doLinkSelection(Set<String> linkIDs) {

    SearchResult retval = new SearchResult(false);
    HashSet<String> linksFromSources = new HashSet<String>();

    Iterator<Linkage> slit = rcx_.getCurrentGenome().getLinkageIterator();
    while (slit.hasNext()) {
      Linkage link = slit.next();
      String linkID = link.getID();
      if (linkIDs.contains(linkID)) {
        retval.found.add(link.getSource());
        retval.found.add(link.getTarget());
        linksFromSources.add(linkID);
      }
    }

    retval.linkIntersections = rcx_.getCurrentLayout().getIntersectionsForLinks(rcx_, linksFromSources, false); 
    return (retval);
  }
  
 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
 
  /***************************************************************************
  **
  ** Select based on a criteria
  ** 
  */
    
  private Set<String> findCriteriaSelections(Set<String> startNodes, boolean invert, GraphSearcher.CriteriaJudge judge) {   
    GraphSearcher gs = new GraphSearcher(new SpecialtyLayoutEngine.NodePlaceSupport(rcx_.getCurrentGenome()), invert);    
    List<GraphSearcher.QueueEntry> results  = gs.breadthSearchUntilStopped(startNodes, judge);
    HashSet<String> retval = new HashSet<String>();
    Iterator<GraphSearcher.QueueEntry> rit = results.iterator();
    while (rit.hasNext()) {
      GraphSearcher.QueueEntry qe = rit.next();
      retval.add(qe.name);
    }
    return (retval);
  }     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
    
  /***************************************************************************
  **
  ** Tell us when to stop looking
  */  
      
  public static interface CriteriaJudge {
    public boolean stopHere(String nodeID);  
  } 
  
  /***************************************************************************
  **
  ** What we return
  */ 
  
  public static class SearchResult {    
    public List<Intersection> linkIntersections;
    public Set<String> found;
    
    public SearchResult(boolean includeLinks) {
      linkIntersections = (includeLinks) ? new ArrayList<Intersection>() : null;
      found = new HashSet<String>();    
    }
    
    public void mapToResults(Map<String, Object> results) {
 
      for( String node : found) {
        results.put(node, new Boolean(true)); 
      }       
          
      if (linkIntersections != null) {
        for (Intersection i : linkIntersections) {
          if (i.segmentIDsFromIntersect() != null) {
            Set<LinkSegmentID> linksFound = new HashSet<LinkSegmentID>();            
            linksFound.addAll(Arrays.asList(i.segmentIDsFromIntersect()));
            if (results.get(i.getObjectID()) != null) {
              Set<LinkSegmentID> currFound = (HashSet<LinkSegmentID>)results.get(i.getObjectID());
              linksFound.addAll(currFound);
            }
            results.put(i.getObjectID(), linksFound);
          } else {
            results.put(i.getObjectID(), new Boolean(true));
          }
        }
      }
      return;
    }
  } 
}
