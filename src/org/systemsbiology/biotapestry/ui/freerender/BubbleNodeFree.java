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

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.BasicStroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.RenderObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Ellipse;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Rectangle;


/****************************************************************************
**
** This renders a bubble
*/

public class BubbleNodeFree extends AbstractTabletNodeFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  private static final double BUBBLE_RADIUS_ = 8.0;
  private static final double BUBBLE_DIAM_ = 2 * BUBBLE_RADIUS_;
  private static final double LINE_WIDTH_    = 2.0;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public BubbleNodeFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Render
  */
  
  protected void renderGlyph(ModalShapeContainer group, GenomeItem item, Point2D origin, 
      Rectangle2D extraPad, Color fillColor, NodeProperties np, 
      DisplayOptions dopt, boolean isGhosted) {
    double x;
    double y;
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
  	BasicStroke line_stroke = new BasicStroke((int)LINE_WIDTH_);
  	Color vac = getVariableActivityColor(item, Color.BLACK, false, dopt);
  	Color draw_col = (isGhosted) ? dopt.getInactiveGray() : vac;
  	
  	if (extraPad == null) {
      x = origin.getX();
      y = origin.getY();
      
      /*
  		Ellipse circ1 = new ModelObjectCache.Ellipse(x - BUBBLE_RADIUS_,  y - BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_,
  				DrawMode.FILL, fillColor, new BasicStroke(0));
  		Ellipse circ2 = new ModelObjectCache.Ellipse(x - BUBBLE_RADIUS_,  y - BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_,
  				DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
  		*/
      
  		Ellipse circ1 = new ModelObjectCache.Ellipse(x,  y, BUBBLE_RADIUS_,
  				DrawMode.FILL, fillColor, new BasicStroke(0));
  		Ellipse circ2 = new ModelObjectCache.Ellipse(x,  y, BUBBLE_RADIUS_,
  				DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
  		
  		// TODO check that default BasicStroke is sufficient for the filled circle
  		group.addShape(circ1, majorLayer, minorLayer);
  	  group.addShape(circ2, majorLayer, minorLayer);
  		
  	  return;
  	}

  	int growthDir = np.getExtraGrowthDirection();
  	
  	x = extraPad.getX();
  	y = extraPad.getY();

  	double arc1_x;
  	double arc1_y;
  	double arc_start;
  	double arc2_x;
  	double arc2_y;
  	
  	double line_x1;
  	double line_x2;
  	double line_y1;
  	double line_y2;
  	
  	Line line1;
  	Line line2;
  	
  	if (growthDir == NodeProperties.HORIZONTAL_GROWTH) {
  		arc1_x = x;
  		arc1_y = y + BUBBLE_RADIUS_;
  		arc_start = 90.0;
  		
  		arc2_x = x + extraPad.getWidth();
  		arc2_y = y + BUBBLE_RADIUS_;
  		
  		line_x1 = x;
  		line_x2 = x + extraPad.getWidth();
  		line_y1 = y;
  		line_y2 = y + extraPad.getHeight();
  		
  		line1 = new Line(line_x1, line_y1, line_x2, line_y1, DrawMode.DRAW, draw_col, line_stroke);
  		line2 = new Line(line_x2, line_y2, line_x1, line_y2, DrawMode.DRAW, draw_col, line_stroke);
  	} else {
  		arc1_x = x + BUBBLE_RADIUS_;
  		arc1_y = y;
  		arc_start = 0.0;
  		
  		arc2_x = x + BUBBLE_RADIUS_;
  		arc2_y = y + extraPad.getHeight();

  		line_x1 = x;
  		line_x2 = x + extraPad.getWidth();
  		line_y1 = y;
  		line_y2 = y + extraPad.getHeight();
  		
  		line1 = new Line(line_x1, line_y1, line_x1, line_y2, DrawMode.DRAW, draw_col, line_stroke);
  		line2 = new Line(line_x2, line_y2, line_x2, line_y1, DrawMode.DRAW, draw_col, line_stroke);
  	}
  	/*
		Arc arc1_fill = new Arc(arc1_x, arc1_y, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, arc_start, 180.0,
				Arc.Type.OPEN, DrawMode.FILL, fillColor, new BasicStroke());
		
		Arc arc1_draw = new Arc(arc1_x, arc1_y, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, arc_start, 180.0,
				Arc.Type.OPEN, DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
		
		Arc arc2_fill = new Arc(arc2_x, arc2_y, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, arc_start + 180.0, 180.0,
				Arc.Type.OPEN, DrawMode.FILL, fillColor, new BasicStroke());
		
		Arc arc2_draw = new Arc(arc2_x, arc2_y, 2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, arc_start + 180.0, 180.0,
				Arc.Type.OPEN, DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
		*/
		Arc arc1_fill = new Arc(arc1_x, arc1_y, BUBBLE_RADIUS_, arc_start, 180.0,
				Arc.Type.OPEN, DrawMode.FILL, fillColor, new BasicStroke());
		
		Arc arc1_draw = new Arc(arc1_x, arc1_y, BUBBLE_RADIUS_, arc_start, 180.0,
				Arc.Type.OPEN, DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
		
		Arc arc2_fill = new Arc(arc2_x, arc2_y, BUBBLE_RADIUS_, arc_start + 180.0, 180.0,
				Arc.Type.OPEN, DrawMode.FILL, fillColor, new BasicStroke());
		
		Arc arc2_draw = new Arc(arc2_x, arc2_y, BUBBLE_RADIUS_, arc_start + 180.0, 180.0,
				Arc.Type.OPEN, DrawMode.DRAW, draw_col, new BasicStroke((int)LINE_WIDTH_));
		
		Rectangle extrapad_rect = new Rectangle(extraPad.getX(), extraPad.getY(), extraPad.getWidth(), extraPad.getHeight(),
				DrawMode.FILL, fillColor, new BasicStroke());
		
		group.addShape(arc1_fill, majorLayer, minorLayer);
		group.addShape(arc2_fill, majorLayer, minorLayer);
		group.addShape(extrapad_rect, majorLayer, minorLayer);
		group.addShape(arc1_draw, majorLayer, minorLayer);
		group.addShape(line1, majorLayer, minorLayer);
		group.addShape(arc2_draw, majorLayer, minorLayer);		
		group.addShape(line2, majorLayer, minorLayer);
  }

  // TODO remove old implementation
  protected void renderGlyphOld(Graphics2D g2, RenderObjectCache cache, GenomeItem item, Point2D origin, 
                             Rectangle2D extraPad, NodeProperties np, 
                             DisplayOptions dopt, boolean isGhosted, ModelObjectCache moc) {
    double x = origin.getX();
    double y = origin.getY();
    
    if (extraPad == null) {
      Ellipse2D circ = new Ellipse2D.Double(x - BUBBLE_RADIUS_, 
                                            y - BUBBLE_RADIUS_,
                                            2.0 * BUBBLE_RADIUS_,
                                            2.0 * BUBBLE_RADIUS_);
      //
      // 11-6-03  Starting today, after installing the slider control, I am seeing
      // race conditions on Linux going into this call for the _chi_ (!) i.e. the first bubble when
      // rapidly changing tree selections.  The call never returns, with 100% CPU
      // usage.  A known Swing problem with sliders - Fixed by having separate sliders 
      // for each model!  (YUK)
      g2.fill(circ); 
      g2.setStroke(new BasicStroke((int)LINE_WIDTH_));
      Color vac = getVariableActivityColor(item, Color.BLACK, false, dopt);
      g2.setPaint((isGhosted) ? dopt.getInactiveGray() : vac);
      g2.draw(circ);
      return;
    }
    
    int growthDir = np.getExtraGrowthDirection();
    Arc2D arc1;     
    Arc2D arc2;
    Line2D line1;
    Line2D line2;
    if (growthDir == NodeProperties.HORIZONTAL_GROWTH) {       
      x = extraPad.getX();
      y = extraPad.getY();
      arc1 = new Arc2D.Double(x - BUBBLE_RADIUS_, y,
                              2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 90, 180, Arc2D.OPEN);      
      x += extraPad.getWidth();
      arc2 = new Arc2D.Double(x - BUBBLE_RADIUS_, y,
                              2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 270, 180, Arc2D.OPEN);

      double x1 = extraPad.getX();
      double x2 = x1 + extraPad.getWidth();    
      line1 = new Line2D.Double(x1, y, x2, y);
      y += extraPad.getHeight();
      line2 = new Line2D.Double(x2, y, x1, y);
    } else {
      x = extraPad.getX();
      
      y = extraPad.getY();
      arc1 = new Arc2D.Double(x, y - BUBBLE_RADIUS_,
                              2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 0, 180, Arc2D.OPEN);      
      y += extraPad.getHeight();
      arc2 = new Arc2D.Double(x, y - BUBBLE_RADIUS_,
                              2.0 * BUBBLE_RADIUS_, 2.0 * BUBBLE_RADIUS_, 180, 180, Arc2D.OPEN);

      double y1 = extraPad.getY();
      double y2 = y1 + extraPad.getHeight();    
      line1 = new Line2D.Double(x, y1, x, y2);
      x += extraPad.getWidth();
      line2 = new Line2D.Double(x, y2, x, y1);            
    }      
    g2.fill(arc1); 
    g2.fill(arc2); 
    g2.fill(extraPad);
    g2.setStroke(new BasicStroke((int)LINE_WIDTH_));
    Color vac = getVariableActivityColor(item, Color.BLACK, false, dopt);
    g2.setPaint((isGhosted) ? dopt.getInactiveGray() : vac);    
    g2.draw(arc1);
    g2.draw(line1);    
    g2.draw(arc2); 
    g2.draw(line2);       
    
    return;
  }
	
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphHalfWidth() {
    return (BUBBLE_RADIUS_);
  }  
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphHalfHeight() {
    return (BUBBLE_RADIUS_);
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
    return (BUBBLE_DIAM_ + 2.0);
  }
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected double glyphPadTweak() {
    return (1.0);
  }
}
