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

package org.systemsbiology.biotapestry.analysis;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/****************************************************************************
**
** Keeps track of paths generated during a graph search.  This is an ongoing
** refactoring to get Genome dependencies out of simple graph algorithms
*/

public class SimplePathTracker<T extends Link> {
  
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
 
  private HashSet<SimplePath<T>> paths_;
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

  public SimplePathTracker() {
    paths_ = new HashSet<SimplePath<T>>();
    pathLimit_ = Integer.MAX_VALUE;
  }  
  
   /***************************************************************************
  **
  ** Constructor
  */

  public SimplePathTracker(int pathLimit) {
    paths_ = new HashSet<SimplePath<T>>();
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
  
  public Iterator<SimplePath<T>> getPaths() {
    return (paths_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Add a new path; returns true if over the limit
  */
  
  public boolean addNewPath(SimplePath<T> path) {
    paths_.add(path);
    return (paths_.size() >= pathLimit_);
  }

  /***************************************************************************
  **
  ** Get the set of existing tails 
  */
  
  public Set<SimplePath<T>> getTails(String nodeID) {
    HashSet<SimplePath<T>> retval = new HashSet<SimplePath<T>>();
    Iterator<SimplePath<T>> pit = paths_.iterator();
    while (pit.hasNext()) {
      SimplePath<T> path = pit.next();
      SimplePath<T> tail = path.tail(nodeID);
      if ((tail != null) && (tail.getDepth() != 0)) {
        retval.add(tail);
      }
    }
    return (retval);
  }  
}
