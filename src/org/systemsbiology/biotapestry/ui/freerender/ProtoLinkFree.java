/*
**    Copyright (C) 2003-2004 Institute for Systems Biology 
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

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.BasicStroke;
import java.util.Iterator;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;

/****************************************************************************
**
** This renders a protolink in Biotapestry
*/

public class ProtoLinkFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int THIN_THICK = 1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public ProtoLinkFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the link at the designated location
  */
  
  public void render(ModelObjectCache moc, ArrayList points) {
    int butt = BasicStroke.CAP_BUTT;
    int join = BasicStroke.JOIN_ROUND;
    BasicStroke stroke = new BasicStroke(THIN_THICK, butt, join);
    GeneralPath path = new GeneralPath();
    Iterator pit = points.iterator();
    boolean first = true;
    while (pit.hasNext()) {
      Point pt = (Point)pit.next();
      if (first) {
        path.moveTo((float)pt.x, (float)pt.y);
        first = false;
      } else{
        path.lineTo((float)pt.x, (float)pt.y);
      }
    }
    Color col = Color.BLACK;
    
    // TODO fix CacheGroup ID
    CommonCacheGroup group = new CommonCacheGroup("_float", "protolink", "protolink");
    ModelObjectCache.SegmentedPathShape ps = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, stroke, path);
    group.addShape(ps, new Integer(0), new Integer(0));
    moc.addGroup(group);
    
    return;
  }
}
