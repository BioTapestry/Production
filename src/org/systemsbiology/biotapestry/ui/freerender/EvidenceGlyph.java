/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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
import java.awt.geom.GeneralPath;
import java.awt.BasicStroke;

import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;

/****************************************************************************
**
** This renders evidence glyphs
*/

public class EvidenceGlyph {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int EVIDENCE_GLYPH_HEIGHT = 6;
  public static final int EVIDENCE_GLYPH_HALF_WIDTH = 5;      

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  

  /***************************************************************************
  **
  ** Add glyph to path
  */

  public static void addGlyphToPath(GeneralPath usePath, float glyphBaseX, float glyphBaseY) {    
    usePath.moveTo(glyphBaseX, glyphBaseY);
    usePath.lineTo(glyphBaseX - EVIDENCE_GLYPH_HALF_WIDTH, glyphBaseY + EVIDENCE_GLYPH_HEIGHT);
    usePath.lineTo(glyphBaseX, glyphBaseY + (2.0f * EVIDENCE_GLYPH_HEIGHT));    
    usePath.lineTo(glyphBaseX + EVIDENCE_GLYPH_HALF_WIDTH, glyphBaseY + EVIDENCE_GLYPH_HEIGHT);
    usePath.closePath();      
    return;
  }

  /***************************************************************************
  **
  ** Render glyph
  */
                                                                           
  public static void renderEvidenceGlyph(ModalShapeContainer group, Color col, float glyphBaseX, float glyphBaseY) {  
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
  	GeneralPath path = new GeneralPath(); 
    path.moveTo(glyphBaseX, glyphBaseY);
    path.lineTo(glyphBaseX - EVIDENCE_GLYPH_HALF_WIDTH, glyphBaseY + EVIDENCE_GLYPH_HEIGHT);
    path.lineTo(glyphBaseX, glyphBaseY + (2.0f * EVIDENCE_GLYPH_HEIGHT));    
    path.lineTo(glyphBaseX + EVIDENCE_GLYPH_HALF_WIDTH, glyphBaseY + EVIDENCE_GLYPH_HEIGHT);
    path.closePath();
        
    BasicStroke stroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    ModelObjectCache.SegmentedPathShape path1 = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, col, stroke, path);
    group.addShape(path1, majorLayer, minorLayer);
    
    return;
  }
}
