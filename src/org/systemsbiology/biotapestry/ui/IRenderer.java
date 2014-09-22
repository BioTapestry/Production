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

import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;

/****************************************************************************
**
** Interface for renderers
*/

public interface IRenderer {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Render the item 
  */

	public void render(ModelObjectCache moc, GenomeItem item, Intersection selected, DataAccessContext rcx, Object miscInfo);

  /***************************************************************************
  **
  ** Check for intersection
  */

  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo);

  /***************************************************************************
  **
  ** Check for intersection of rectangle
  */

  public Intersection intersects(GenomeItem item, Rectangle rect, boolean countPartial, DataAccessContext rcx, Object miscInfo);  
   
  /***************************************************************************
  **
  ** Return the Rectangle used by this item
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo);
}