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

package org.systemsbiology.biotapestry.ui;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;

/****************************************************************************
**
** A Class
*/

public class InstanceEngine {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final double GRID_X_ = 250.0;
  private static final double GRID_Y_ = 150.0;
  private static final int MAX_ROW_ = 10;  
  
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
  
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public InstanceEngine(BTState appState) {
    appState_ = appState;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Layout the given nodes by groups
  */
  
  public Map<String, Point2D> layout(Map<String, Set<String>> regions, String genomeKey) {
    Database db = appState_.getDB();
    GenomeInstance gi = (GenomeInstance)db.getGenome(genomeKey);
    HashMap<String, Point2D> retval = new HashMap<String, Point2D>();
   
    int row = 0;
    int col = 0;
    Iterator<String> reit = regions.keySet().iterator();
    while (reit.hasNext()) {
      String grKey = reit.next();
      Set<String> nodes = regions.get(grKey);
      row = 0;
      col += 2;
      Iterator<String> nit = nodes.iterator();
      while (nit.hasNext()) {
        if (row > MAX_ROW_) {
          row = 0;
          col++;
        }
        String nodeID = nit.next();
        Point2D loc = new Point2D.Double(col * GRID_Y_, row * GRID_X_);
        int instance = gi.getInstanceForNodeInGroup(nodeID, grKey);        
        retval.put(GenomeItemInstance.getCombinedID(nodeID, Integer.toString(instance)), loc);
        row++;
      }
    }
    
    return (retval);
  }
}
