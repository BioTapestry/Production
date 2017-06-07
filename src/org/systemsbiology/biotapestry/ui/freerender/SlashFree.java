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

import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a slash
*/

public class SlashFree extends AbstractInlineNodeFree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final float SLASH_HALF_HEIGHT_ = 8.0F;
  private static final float SLASH_HALF_LENGTH_ = 3.0F;
  private static final float SLASH_HALF_OFFSET_ = 4.0F;
  private static final double INTERSECT_        = 12.0;
  private static final double TEXT_PAD_         = 10.0;
  private static final double EXTRA_PAD_START_OFFSET_ = 10.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public SlashFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the shape to render one stroke
  */
  
  protected ModelObjectCache.SegmentedPathShape getPath1Shape(float x, float y, Color col, BasicStroke pathStroke) {
    GeneralPath path = new GeneralPath();
    path.moveTo(x - SLASH_HALF_LENGTH_ - SLASH_HALF_OFFSET_, y + SLASH_HALF_HEIGHT_);
    path.lineTo(x + SLASH_HALF_LENGTH_ - SLASH_HALF_OFFSET_, y - SLASH_HALF_HEIGHT_);
    ModelObjectCache.SegmentedPathShape pathShape = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, pathStroke, path);
    return (pathShape);
  }
  
  /***************************************************************************
  **
  ** Get the shape to render the other stroke
  */
  
  protected ModelObjectCache.SegmentedPathShape getPath2Shape(float x, float y, Color col, BasicStroke pathStroke) {
    GeneralPath path = new GeneralPath();
    path.moveTo(x + SLASH_HALF_LENGTH_ + SLASH_HALF_OFFSET_, y - SLASH_HALF_HEIGHT_);
    path.lineTo(x - SLASH_HALF_LENGTH_ + SLASH_HALF_OFFSET_, y + SLASH_HALF_HEIGHT_);
    ModelObjectCache.SegmentedPathShape pathShape = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, pathStroke, path);
    return (pathShape); 
  }
 
  /***************************************************************************
  **
  ** Fills an array with bound shapes that will be exported in the model map
  ** in the web application.
  */
  
  protected ModelObjectCache.ModalShape getFBAShape(double cX, double cY) { 
    return (ModalShapeFactory.buildBoundCircle(cX, cY, INTERSECT_));
  }
 
  /***************************************************************************
  **
  ** Get the intersect
  */
  
  @Override
  protected double getIntersect() {
    return (INTERSECT_);
  }
     
  /***************************************************************************
  **
  ** Get the text pad
  */
  
  @Override
  protected double getTextPad() {
    return (TEXT_PAD_);
  }

  /***************************************************************************
   **
   ** Get the landing pad width
   */
  
  @Override
  public double getLandingPadWidth(int padNum, GenomeItem item, Layout layout) {
    return (SLASH_HALF_HEIGHT_);
  }
  
  /***************************************************************************
  **
  ** Get the right departure vector
  */

  @Override
  protected Vector2D rightDepart() {
    return (new Vector2D(SLASH_HALF_OFFSET_, 0.0));
  }
  
  /***************************************************************************
  **
  ** Get the left arrival vector
  */
  
  @Override
  protected Vector2D leftArrive() {
    return (new Vector2D(-SLASH_HALF_OFFSET_, 0.0));
  }
  
  /***************************************************************************
  **
  ** How to draw the extra rectangle
  */
  
  protected boolean extraRectPointed() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** Offset to start extras
  */
  
  protected double extraPadStartOffset() {
    return (EXTRA_PAD_START_OFFSET_);
  }
}
