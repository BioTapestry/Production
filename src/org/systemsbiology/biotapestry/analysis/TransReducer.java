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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.systemsbiology.biotapestry.util.SimpleLink;

/****************************************************************************
**
** A class for building a transitive reduction of a topologically sorted DAG
*/

public class TransReducer {
  
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TransReducer() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Return a set of reduced links
  */

  public Set<SimpleLink> reduceGraph(Set<SimpleLink> links) {
    
    //
    // Go through each link.  If we can omit it and still reach the
    // target from the source, drop it from the list.
    //
    
    HashSet<SimpleLink> retval = new HashSet<SimpleLink>(links);
    ArrayList<SimpleLink> linkList = new ArrayList<SimpleLink>(links);
    int currLink = 0;
    while (currLink < linkList.size()) {
      SimpleLink omitLink = linkList.get(currLink);
      if (reachable(omitLink.getSrc(), omitLink.getTrg(), retval, omitLink)) {
        retval.remove(omitLink);
        linkList.remove(currLink);
      } else {
        currLink++;
      }
    }
    return (retval);
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

  /***************************************************************************
  ** 
  ** Answer if the second node is reachable from the first while omitting the
  ** given link
  */

  private boolean reachable(String from, String to, Set<SimpleLink> links, SimpleLink omit) {
    HashSet<String> visited = new HashSet<String>();
    return (reachableGuts(from, to, visited, links, omit));
  }

  /***************************************************************************
  ** 
  ** Reachable recursive guts
  */

  private boolean reachableGuts(String from, String to, HashSet<String> visited, Set<SimpleLink> links, SimpleLink omit) {

    if (visited.contains(from)) {
      return (false);
    }
    visited.add(from);
    
    if (from.equals(to)) {
      return (true);
    }
    
    Iterator<SimpleLink> lit = links.iterator();
    while (lit.hasNext()) {
      SimpleLink slink = lit.next();
      if (!slink.getSrc().equals(from)) {
        continue;
      }
      if ((omit != null) && omit.equals(slink)) {
        continue;
      }
      if (reachableGuts(slink.getTrg(), to, visited, links, omit)) {
        return (true);
      }
    }
    return (false);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST FRAMEWORK
  //
  ////////////////////////////////////////////////////////////////////////////

}
