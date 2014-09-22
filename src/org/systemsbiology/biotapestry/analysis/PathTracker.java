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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/****************************************************************************
**
** Keeps track of paths generated during a graph search.
*/

public class PathTracker {
  
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
 
  private HashSet<Path> paths_;
  private int pathLimit_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PathTracker() {
    paths_ = new HashSet<Path>();
    pathLimit_ = Integer.MAX_VALUE;
  }  
  
   /***************************************************************************
  **
  ** Constructor
  */

  public PathTracker(int pathLimit) {
    paths_ = new HashSet<Path>();
    pathLimit_ = pathLimit;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the paths
  */
  
  public Iterator<Path> getPaths() {
    return (paths_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Add a new path; returns true if over the limit
  */
  
  public boolean addNewPath(Path path) {
    paths_.add(path);
    return (paths_.size() >= pathLimit_);
  }

  /***************************************************************************
  **
  ** Get the set of existing tails 
  */
  
  public Set<Path> getTails(String nodeID) {
    HashSet<Path> retval = new HashSet<Path>();
    Iterator<Path> pit = paths_.iterator();
    while (pit.hasNext()) {
      Path path = pit.next();
      Path tail = path.tail(nodeID);
      if ((tail != null) && (tail.getDepth() != 0)) {
        retval.add(tail);
      }
    }
    return (retval);
  }  
}
