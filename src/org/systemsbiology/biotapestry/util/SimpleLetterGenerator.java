/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

/****************************************************************************
**
** A class for creating a series of unique letters
*/

public class SimpleLetterGenerator {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private TreeSet generated_;
  
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

  public SimpleLetterGenerator() {
    generated_ = new TreeSet();
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public SimpleLetterGenerator(Set existingTags) {
    generated_ = new TreeSet(existingTags);
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get the next free tag
  */

  public String getNextTag() {
    
    String nextAlpha = null;
    if (generated_.isEmpty()) {
      nextAlpha = UniqueLabeller.genNextAlpha(null);
    } else {
      Iterator git = generated_.iterator();
      while (git.hasNext()) {
        String lastOne = (String)git.next();
        nextAlpha = UniqueLabeller.genNextAlpha(lastOne);
        if (!generated_.contains(nextAlpha)) {
          break;
        }
      }
    }
    if (nextAlpha == null) {
      throw new IllegalStateException();
    }
    generated_.add(nextAlpha);
    return (nextAlpha);
  } 
}
