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


package org.systemsbiology.biotapestry.nav;

import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;

/****************************************************************************
**
** Maps the given genome to the currently selected layout
*/

public class LayoutManager {

  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build a layout manager
  */ 
  
  public LayoutManager(BTState appState) {
    appState_ = appState;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Find out which layout to use for the given genome.  ANCIENT, BOGUS code
  */ 
  
  public String getLayout(String genomeID) {
    if (genomeID == null) {
      return (null);
    } else {
      Database db = appState_.getDB();
      Genome gen = db.getGenome(genomeID);
      // For subset instances, look for the layout to the source
      if (gen instanceof GenomeInstance) {  // FIX ME
        GenomeInstance parent = ((GenomeInstance)gen).getVfgParentRoot();
        if (parent != null) {
          genomeID = parent.getID();
        }
      } 
      Iterator<Layout> lit = db.getLayoutIterator();
      while (lit.hasNext()) {
        Layout lo = lit.next();
        String targ = lo.getTarget();
        if (targ.equals(genomeID)) {
          return (lo.getID());
        }
      }
      return (null);
    }
  }
}
