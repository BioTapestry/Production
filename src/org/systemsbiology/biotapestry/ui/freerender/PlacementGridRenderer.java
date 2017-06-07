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

package org.systemsbiology.biotapestry.ui.freerender;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;

/****************************************************************************
**
** Interface getting link rendered into a LinkPlacementGrid
*/

public interface PlacementGridRenderer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the link to a pattern grid
  */
  
  public void renderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, Set<String> skipLinks, 
                                    String overID, DataAccessContext icx);
  
  
 /***************************************************************************
  **
  ** Answer if we can render the link to a pattern grid
  ** 
  */
  
  public boolean canRenderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, 
                                          Set<LinkPlacementGrid.PointPair> dropSet, 
                                          String overID, DataAccessContext icx); 

  /***************************************************************************
  **
  ** Get the map of point pairs to links
  */
  
  public Map<LinkPlacementGrid.PointPair, List<String>> getPointPairMap(LinkProperties lp,
                                                                        String overID, 
                                                                        DataAccessContext icx);
}
