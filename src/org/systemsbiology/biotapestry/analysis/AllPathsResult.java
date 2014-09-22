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

package org.systemsbiology.biotapestry.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/****************************************************************************
**
** All paths between two nodes
*/

public class AllPathsResult {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String srcID_;
  private String targID_;
  private TreeMap<String, Integer> nodes_;
  private HashSet<String> links_;
  private PathTracker tracker_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public AllPathsResult(String srcID, String targID) {
    srcID_ = srcID;
    targID_ = targID;
    nodes_ = new TreeMap<String, Integer>();
    links_ = new HashSet<String>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  

  /***************************************************************************
  **
  ** Add a node
  */
    
  public void addNode(String nodeID) {
    nodes_.put(nodeID, new Integer(0));
    return;
  }

  /***************************************************************************
  **
  ** Set a depth
  */
    
  public void setDepth(String nodeID, int depth) {
    nodes_.put(nodeID, new Integer(depth));
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a link
  */
    
  public void addLink(String linkID) {
    links_.add(linkID);
  }
    
  /***************************************************************************
  **
  ** Get the depth of a node
  */
  
  public Integer getDepth(String nodeID) {
    return (nodes_.get(nodeID));
  }     

  /***************************************************************************
  **
  ** Get the links
  */
  
  public Iterator<String> getLinks() {
    return (links_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Set<String> getNodeSet() {
    return (Collections.unmodifiableSet(nodes_.keySet()));
  }  

  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<String> getNodes() {
    return (nodes_.keySet().iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the links
  */
  
  public Set<String> getLinkSet() {
    return (Collections.unmodifiableSet(links_));
  }  
  
  
  
  /***************************************************************************
  **
  ** Get the source of the paths
  */
  
  public String getSource() {
    return (srcID_);
  } 
  
  /***************************************************************************
  **
  ** Get the target of the paths
  */
  
  public String getTarget() {
    return (targID_);
  }   

  /***************************************************************************
  **
  ** Get a list of path descriptions
  */
  
  public List<Path> getPaths() {
    ArrayList<Path> retval = new ArrayList<Path>();
    Iterator<Path> paths = tracker_.getPaths();
    while (paths.hasNext()) {
      Path path = paths.next();
      retval.add(new Path(path));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Set the tracker results
  */
  
  public void setTracker(PathTracker tracker) {
    tracker_ = tracker;
    Iterator<Path> paths = tracker.getPaths();
    while (paths.hasNext()) {
      Path path = paths.next();
      Iterator<SignedTaggedLink> lit = path.pathIterator();
      while (lit.hasNext()) {
        SignedTaggedLink link = lit.next();
        addLink(link.getTag());
        addNode(link.getSrc());
      }
    }
    addNode(targID_);
    return;
  }  
  
  /***************************************************************************
  **
  ** Standard toString
  */
  
  public String toString() {
    return ("AllPathsResult source = " + srcID_ +
                          " target = " + targID_ +
                          " links = " + links_ +
                          " nodes = " + nodes_);
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
