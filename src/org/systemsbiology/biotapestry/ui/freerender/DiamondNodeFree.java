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

package org.systemsbiology.biotapestry.ui.freerender;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;


/****************************************************************************
**
** This renders a diamond
*/

public class DiamondNodeFree extends AbstractTabletNodeFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double LINE_WIDTH_          = 3.0;
  private static final float  DIAMOND_HALF_HEIGHT_ = 20.0F;
  private static final float  DIAMOND_HALF_WIDTH_  = 15.0F; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public DiamondNodeFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Render
  */
  
	protected void renderGlyph(ModalShapeContainer group, GenomeItem item, Point2D origin,
			Rectangle2D extraPad, Color fillColor, NodeProperties np,
			DisplayOptions dopt, boolean isGhosted, Mode mode) {
		
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    GeneralPath path = new GeneralPath();    
    
    if (extraPad == null) {
      path.moveTo(x, y - DIAMOND_HALF_HEIGHT_);
      path.lineTo(x + DIAMOND_HALF_WIDTH_, y);
      path.lineTo(x, y + DIAMOND_HALF_HEIGHT_);
      path.lineTo(x - DIAMOND_HALF_WIDTH_, y);    
      path.closePath();    
    } else {
      int growthDir = np.getExtraGrowthDirection();
      float epX = (float)extraPad.getX();
      float epY = (float)extraPad.getY();
      float epMX = (float)extraPad.getMaxX();
      float epMY = (float)extraPad.getMaxY();     
      if (growthDir == NodeProperties.HORIZONTAL_GROWTH) {       
        path.moveTo(epX, epY);
        path.lineTo(epMX, epY);
        path.lineTo(epMX + DIAMOND_HALF_WIDTH_, y);
        path.lineTo(epMX, epMY);        
        path.lineTo(epX, epMY);
        path.lineTo(epX - DIAMOND_HALF_WIDTH_, y);    
        path.closePath();    
      } else {       
        path.moveTo(x, epY - DIAMOND_HALF_HEIGHT_);
        path.lineTo(epMX, epY);
        path.lineTo(epMX, epMY);
        path.lineTo(x, epMY + DIAMOND_HALF_HEIGHT_);        
        path.lineTo(epX, epMY);
        path.lineTo(epX, epY);    
        path.closePath();      
      }
    }
    
    // TODO is stroke correct?
    ModelObjectCache.SegmentedPathShape spath1 = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, fillColor, new BasicStroke(0), (GeneralPath)path.clone());
    
    BasicStroke drawStroke = new BasicStroke((float)LINE_WIDTH_, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    Color lineCol = getVariableActivityColor(item, Color.BLACK, false, dopt, mode);
    Color drawColor = (isGhosted) ? dopt.getInactiveGray() : lineCol;
    ModelObjectCache.SegmentedPathShape spath2 = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, drawColor, drawStroke, (GeneralPath)path.clone());
    
    group.addShape(spath1, majorLayer, minorLayer);
    group.addShape(spath2, majorLayer, minorLayer);
	}
    
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphHalfWidth() {
    return (DIAMOND_HALF_WIDTH_);
  }  
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphHalfHeight() {
    return (DIAMOND_HALF_HEIGHT_);
  }     
   
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphLineWidth() {
    return (LINE_WIDTH_);
  }      
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphPadWidth() {
    return (PAD_WIDTH_);
  }
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphPadTweak() {
    return (1.0);
  }
}
