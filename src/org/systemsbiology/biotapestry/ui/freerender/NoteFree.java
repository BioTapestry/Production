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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.ItemRenderBase;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.modelobjectcache.BoundShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;

/****************************************************************************
**
** This renders a group
*/

public class NoteFree extends ItemRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double TEXT_PAD_ = 10.0;
  // Font bounds gives fixed height that is ~ 33% taller than upper case,
  // so hack it to get it centered.
  private static final double HEIGHT_HACK_ = 0.75;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public NoteFree() {
    super();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the bare node using the provided layout
  */
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, DataAccessContext rcx, Mode mode, Object miscInfo) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.isForWeb()) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    NoteProperties np = rcx.getCurrentLayout().getNoteProperties(item.getID());
    Point2D origin = np.getLocation();
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    Color col = (rcx.isGhosted()) ? dop.getInactiveGray() : np.getColor();
    AnnotatedFont bFont = rcx.getFontManager().getOverrideFont(FontManager.NOTES, np.getFontOverride());

    String name = item.getName();
    
    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "note");
    
    if (name.indexOf('\n') == -1) {
      Rectangle2D bounds = bFont.getFont().getStringBounds(name, rcx.getFrc());
      double width = bounds.getWidth();
      double height = bounds.getHeight() * HEIGHT_HACK_;
      double x = origin.getX() - width / 2.0;
      double by = origin.getY() + height / 2.0;
      double ty = origin.getY() - height / 2.0;
      if (selected != null) {
        // TODO fix stroke
        ModelObjectCache.Rectangle rect = new ModelObjectCache.Rectangle((float)x, (float)ty, 
                                                 (float)width, (float)height,
                                                 DrawMode.FILL, Color.orange, new BasicStroke());
        group.addShape(rect, majorLayer, minorLayer);
      }

      // TODO check transform
      ModalShape ts = textFactory.buildTextShape(name, bFont, col, (float)x, (float)by, new AffineTransform());
      group.addShape(ts, majorLayer, minorLayer);    
    } else {
      multiLineRender(group, name, bFont, origin, (selected != null), np.getJustification(), col, rcx.getFrc(), textFactory);
    }
    
    setGroupBounds(group, rcx.getCurrentGenome(), item, rcx, miscInfo);

    cache.addGroup(group);
    
    return;    
  }
  
  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
                              
    Rectangle bounds = getBounds(item, rcx, miscInfo);
    if (Bounds.intersects(bounds, (int)pt.getX(), (int)pt.getY())) {
      return (new Intersection(item.getID(), null, 0.0));
    } else {
      return (null);
    }    
  }
  
  /***************************************************************************
  **
  ** Check for intersection of rectangle
  */

  public Intersection intersects(GenomeItem item, Rectangle rect, boolean countPartial, DataAccessContext rcx, Object miscInfo) {
    return (null);                                
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this note
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    
    NoteProperties np = rcx.getCurrentLayout().getNoteProperties(item.getID());
    Point2D origin = np.getLocation();
    String name = item.getName();
    AnnotatedFont abFont = rcx.getFontManager().getOverrideFont(FontManager.NOTES, np.getFontOverride());
    
    if (name.indexOf('\n') == -1) {
      Rectangle2D bounds = abFont.getFont().getStringBounds(name, rcx.getFrc());
      double width = bounds.getWidth() + (TEXT_PAD_ * 2.0);
      double height = (bounds.getHeight() * HEIGHT_HACK_) + (TEXT_PAD_ * 2.0);
      double minX = origin.getX() - width / 2.0;
      double minY = origin.getY() - height / 2.0;
      return (new Rectangle((int)minX, (int)minY, (int)width, (int)height));
    } else {
      String[] toks = name.split("\n");
      Rectangle2D rect2D = MultiLineRenderSupport.multiLineGuts(rcx.getFrc(), toks, abFont, origin, null, null, null, null);
      return (new Rectangle((int)rect2D.getX(), (int)rect2D.getY(), (int)rect2D.getWidth(), (int)rect2D.getHeight()));
    }
  }

  /***************************************************************************
   **
   ** Sets the bound shapes for the CacheGroup, to be exported in the
   ** web application.
  */
  public void setGroupBounds(BoundShapeContainer group, Genome genome, GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, genome, item, rcx, miscInfo);
    for (ModelObjectCache.ModalShape ms : bounds) {
      group.addBoundsShape(ms);
    }
  }

  /***************************************************************************
   **
   ** Fills an array with bound shapes that will be exported in the model map
   ** in the web application.
  */  
  void fillBoundsArray(ArrayList<ModelObjectCache.ModalShape> targetArray, Genome genome, GenomeItem item, 
                       DataAccessContext rcx, Object miscInfo) {

	Rectangle bounds = getBounds(item, rcx, miscInfo);
    targetArray.add(ModalShapeFactory.buildBoundRectangleForGlyph(bounds));
  }
  
  /***************************************************************************
  **
  ** Render this note for multiple-line cases
  */
  
  private Rectangle2D multiLineRender(ModalShapeContainer group, String name, AnnotatedFont amFont, Point2D origin,
      boolean selected, int justification, Color col, FontRenderContext frc, ModalTextShapeFactory textFactory) {
  	
    //
    // Build up the lines to display first:
    //

    String[] toks = name.split("\n");
    if (toks.length == 0) {
      return (null);
    }
    
    return (MultiLineRenderSupport.multiLineRender(group, frc, toks, amFont, origin, selected, justification, col, textFactory));
  }
}
