/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeInsertionDirective;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.RenderObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a slash
*/

public class SlashFree extends NodeRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final float SLASH_HALF_HEIGHT_ = 8.0F;
  private static final float SLASH_HALF_LENGTH_ = 3.0F;
  private static final float SLASH_HALF_OFFSET_ = 4.0F;  
  private static final double INTERSECT_SQ_     = 144.0;
  private static final double INTERSECT_        = 12.0;
  private static final double TEXT_PAD_         = 10.0;
  private static final float PAD_WIDTH_         = 10.0F;
  private static final double APPROX_DIAM_      = 20.0; 
  
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
  ** Get the preferred launch direction from the given pad
  */
  
  public Vector2D getDepartureDirection(int padNum, GenomeItem item, Layout layout) {
    NodeProperties np = layout.getNodeProperties(item.getID());
    int orient = np.getOrientation();    
    if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.NONE)) {
      return ((padNum == 0) ? new Vector2D(1.0, 0.0) : new Vector2D(-1.0, 0.0));
    } else if (orient == NodeProperties.UP) {
      return ((padNum == 0) ? new Vector2D(0.0, -1.0) : new Vector2D(0.0, 1.0));
    } else if (orient == NodeProperties.LEFT) {
      return ((padNum == 0) ? new Vector2D(-1.0, 0.0) : new Vector2D(1.0, 0.0));
    } else if (orient == NodeProperties.DOWN) {
      return ((padNum == 0) ? new Vector2D(0.0, 1.0) : new Vector2D(0.0, -1.0));
    } else {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Get the preferred arrival direction
  */
  
  public Vector2D getArrivalDirection(int padNum, GenomeItem item, Layout layout) {
    Vector2D depDir = getDepartureDirection(padNum, item, layout);
    depDir.scale(-1.0);
    return (depDir);
  }
  
  /***************************************************************************
  **
  ** Render the slash at the designated location
  */
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, DataAccessContext rcx, Object miscInfo) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.forWeb) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
    
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    Point2D origin = np.getLocation();
    float x = (float)origin.getX();
    float y = (float)origin.getY();    

    int orient = np.getOrientation();    
    AffineTransform trans = new AffineTransform();
    
    double radians = 0.0;
    if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.NONE)) {
      radians = 0.0;
    } else if (orient == NodeProperties.UP) {
      radians = -Math.PI / 2.0;
    } else if (orient == NodeProperties.LEFT) {
      radians = Math.PI;   
    } else if (orient == NodeProperties.DOWN) {
      radians = Math.PI / 2.0;
    }
    trans.rotate(radians, x, y);

    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "slash", trans);

    ModelObjectCache.PushTransformOperation pushTrans = new ModelObjectCache.PushTransformOperation(trans);
    group.addShape(pushTrans, majorLayer, minorLayer);
   

    boolean isGhosted = rcx.isGhosted();
    boolean textGhosted = isGhosted;
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != NodeInstance.VESTIGIAL);
    }
    Color vac = getVariableActivityColor(item, np.getColor(), false, rcx.getDisplayOptsSource().getDisplayOptions());
    Color col = (isGhosted) ? Color.LIGHT_GRAY : vac;
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, rcx.getDisplayOptsSource().getDisplayOptions());           
    BasicStroke stroke = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    
    roundSelectionSupport(group, selected, origin.getX(), origin.getY(), INTERSECT_, rcx.forWeb);
    
    // gpath1 and gpath2 used to be one, such that the path was reset after the first lineTo-segment.
    GeneralPath gpath1 = new GeneralPath();
    gpath1.moveTo(x - SLASH_HALF_LENGTH_ - SLASH_HALF_OFFSET_, y + SLASH_HALF_HEIGHT_);
    gpath1.lineTo(x + SLASH_HALF_LENGTH_ - SLASH_HALF_OFFSET_, y - SLASH_HALF_HEIGHT_);
    
    ModelObjectCache.SegmentedPathShape path1 = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, stroke, gpath1);
    
    GeneralPath gpath2 = new GeneralPath();
    gpath2.moveTo(x + SLASH_HALF_LENGTH_ + SLASH_HALF_OFFSET_, y - SLASH_HALF_HEIGHT_);
    gpath2.lineTo(x - SLASH_HALF_LENGTH_ + SLASH_HALF_OFFSET_, y + SLASH_HALF_HEIGHT_);
    
    ModelObjectCache.SegmentedPathShape path2 = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, stroke, gpath2);
  
    group.addShape(path1, majorLayer, minorLayer);
    group.addShape(path2, majorLayer, minorLayer);

    ModelObjectCache.PopTransformOperation popTrans = new ModelObjectCache.PopTransformOperation();
    group.addShape(popTrans, majorLayer, minorLayer);
    
    if (rcx.showBubbles) {
      renderPads(group, (isGhosted) ? Color.LIGHT_GRAY : Color.BLACK, item, rcx);
    }

    String name = item.getName();         
    Color actualTextCol = (textGhosted) ? Color.LIGHT_GRAY : textCol;
    Point2D textEnd = renderText(group, (float)(x + TEXT_PAD_), (float)(y - TEXT_PAD_), 
                                 name, actualTextCol, np.getHideName(), amFont, np.getLineBreakDef(), rcx.getFrc(), rcx.fmgr, textFactory);
  
    Point2D pieCenter = new Point2D.Double(textEnd.getX() + 15.0, textEnd.getY());
    drawVariableActivityPie(group, item, col, pieCenter, rcx.getDisplayOptsSource().getDisplayOptions());
    
    setGroupBounds(group, rcx.getGenome(), item, rcx);
    
    cache.addGroup(group);
  }
  
  
  /***************************************************************************
   **
   ** Sets the bound shapes for the CacheGroup, to be exported in the
   ** web application.
  */
  public void setGroupBounds(CommonCacheGroup group, Genome genome, GenomeItem item, DataAccessContext rcx) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, genome, item, rcx);
    for (ModelObjectCache.ModalShape ms : bounds) {
      group.addBoundsShape(ms);
    }
  }
  
  /***************************************************************************
   **
   ** Fills an array with bound shapes that will be exported in the model map
   ** in the web application.
  */
  void fillBoundsArray(ArrayList<ModelObjectCache.ModalShape> targetArray, Genome genome, GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double cX = origin.getX();
    double cY = origin.getY();

    ModelObjectCache.Ellipse bound = ModalShapeFactory.buildBoundCircle(cX, cY, INTERSECT_);
    targetArray.add(bound);
    
    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {
        fillBoundsArrayTextLabel(targetArray, rcx, (float)(cX + TEXT_PAD_), (float)(cY - TEXT_PAD_),
                name, np.getHideName(), FontManager.MEDIUM, np.getFontOverride(), np.getLineBreakDef(), rcx.fmgr);
    }    
  }
  
  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
                              
    //
    // Simple distance check - > better to use rectangle
    //
    // FIX ME!!                              
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double x = origin.getX();
    double y = origin.getY();
    double dx = x - pt.getX();
    double dy = y - pt.getY();
    double distsq = (dx * dx) + (dy * dy);    
    
    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {         
      if (isTextCandidate(rcx.getFrc(), FontManager.MEDIUM, np.getFontOverride(), name, TEXT_PAD_, 
                          dx, distsq, np.getLineBreakDef(), false, np.getHideName(), rcx.fmgr)) {
        if (intersectTextLabel(rcx.getFrc(), (float)(x + TEXT_PAD_), (float)(y - TEXT_PAD_), 
                               name, pt, np.getHideName(), FontManager.MEDIUM, np.getFontOverride(), np.getLineBreakDef(), rcx.fmgr)) {
          return (new Intersection(item.getID(), null, 0.0));
        }
      }
    }      

    if (distsq <= INTERSECT_SQ_) {
      List<Intersection.PadVal> pads = calcPadIntersects(item, rcx, pt);
      if (pads != null) {
        return (new Intersection(item.getID(), pads));
      } else {
        return (new Intersection(item.getID(), null, Math.sqrt(distsq)));
      }
    } else {
      return (null);
    }   
  }
  
  /***************************************************************************
  **
  ** Check for intersection of rectangle
  */

  public Intersection intersects(GenomeItem item, Rectangle rect, boolean countPartial, DataAccessContext rcx, Object miscInfo) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double cX = origin.getX();
    double cY = origin.getY();
    Rectangle2D myBounds = new Rectangle2D.Double(cX - INTERSECT_, cY - INTERSECT_,
                                                  2.0 * INTERSECT_, 2.0 * INTERSECT_);
    Rectangle2D inbounds = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
    if (inbounds.contains(myBounds)) {
      return (new Intersection(item.getID(), null, 0.0));
    } else {
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  public Vector2D getLaunchPadOffset(int padNum, GenomeItem item, DataAccessContext icx) {
    
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    int orient = np.getOrientation();
    return (getLaunchPadOffsetGuts(padNum, orient));
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  private Vector2D getLaunchPadOffsetGuts(int padNum, int orient) {    
    if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.NONE)) {
      return ((padNum == 0) ? new Vector2D(SLASH_HALF_OFFSET_, 0.0)
                            : new Vector2D(-SLASH_HALF_OFFSET_, 0.0));
    } else if (orient == NodeProperties.UP) {
      return ((padNum == 0) ? new Vector2D(0.0, -SLASH_HALF_OFFSET_)
                            : new Vector2D(0.0, SLASH_HALF_OFFSET_));
    } else if (orient == NodeProperties.LEFT) {
      return ((padNum == 0) ? new Vector2D(-SLASH_HALF_OFFSET_, 0.0)
                            : new Vector2D(SLASH_HALF_OFFSET_, 0.0));
    } else if (orient == NodeProperties.DOWN) {
      return ((padNum == 0) ? new Vector2D(0.0, SLASH_HALF_OFFSET_)
                            : new Vector2D(0.0, -SLASH_HALF_OFFSET_));
    } else {
      throw new IllegalStateException();
    }
  }
 
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, DataAccessContext icx) {
    return (getLaunchPadOffset(padNum, item, icx));
  }

  /***************************************************************************
  **  
  ** Get the landing pad width
  */
  
  public double getLandingPadWidth(int padNum, GenomeItem item, Layout layout) {
    return (SLASH_HALF_HEIGHT_);
  }
  
  /***************************************************************************
  **
  ** Get the maximum number of launch pads
  */
  
  public int getFixedLaunchPadMax() {
    return (2);
  }
  
  
  /***************************************************************************
  **
  ** Get the maximum number of landing pads
  */
  
  public int getFixedLandingPadMax() {
    return (2);
  }
  
   
  /***************************************************************************
  **
  ** Get the recommended node insertion info
  */
  
  public NodeInsertionDirective getInsertionDirective(Vector2D travel, Point2D insertion) {
    //
    // Different approach than others: pads stay the same, but
    // we change the orientation!
    //

    if (travel.getX() == 0.0) {
      if (travel.getY() < 0.0) {
        return (new NodeInsertionDirective(1, 0, NodeProperties.UP));
      } else {
        return (new NodeInsertionDirective(1, 0, NodeProperties.DOWN));
      }
    } else if (travel.getY() == 0.0) {
      if (travel.getX() < 0.0) {
        return (new NodeInsertionDirective(1, 0, NodeProperties.LEFT));
      } else {
        return (new NodeInsertionDirective(1, 0, NodeProperties.RIGHT));
      }      
    } else if (Math.abs(travel.getX()) >= Math.abs(travel.getY())) {  // mostly horizontal
      if (travel.getX() > 0) {
        return (new NodeInsertionDirective(1, 0, NodeProperties.RIGHT));
      } else {
        return (new NodeInsertionDirective(1, 0, NodeProperties.LEFT));      
      }
    } else {  // mostly vertical
      if (travel.getY() > 0) {
        return (new NodeInsertionDirective(1, 0, NodeProperties.DOWN));
      } else {
        return (new NodeInsertionDirective(1, 0, NodeProperties.UP));      
      }     
    }
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int x = (int)origin.getX();
    int y = (int)origin.getY();
    // Really need to do this correctly!!
    int half = (int)APPROX_DIAM_ / 2;
    return (new Rectangle(x - half, y - half, (int)APPROX_DIAM_, (int)APPROX_DIAM_));  
  }
 
  /***************************************************************************
  **
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal.
  ** The bounds are returned with the placement point at (0,0).
  */
  
  public Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, 
                                        int orientation, boolean labelToo, Integer topPadCount) {    
     if ((topPadCount != null) && (topPadCount.intValue() > 1)) {
       throw new IllegalArgumentException();
     }
     NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());  
     int half = (int)APPROX_DIAM_ / 2;
     Rectangle basic = new Rectangle(-half, -half, (int)APPROX_DIAM_, (int)APPROX_DIAM_);  
        
     if (labelToo && !np.getHideName()) {
       AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
       String breakDef = np.getLineBreakDef();
       Rectangle2D textRect = getTextBounds(rcx.getFrc(), (float)TEXT_PAD_, (float)TEXT_PAD_, 
                                            item.getName(), false, amFont, breakDef, rcx.fmgr);
       Rectangle fullRect = UiUtil.rectFromRect2D(textRect);
       return (fullRect.union(basic));
     } else {
       return (basic);
     }    
  }  
 
  /***************************************************************************
  **
  ** Get the launch pad offset for given layout case:
  */
  
  public Vector2D getLaunchPadOffsetForLayout(GenomeItem item, DataAccessContext rcx, 
                                              int orientation, Integer topPadCount) {
    return (getLaunchPadOffsetGuts(0, orientation));
  }
  
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public int topPadCount(int fullPadCount) {
     return (1);  
  }
  
  /***************************************************************************
  **
  ** Return the height
  */
  
  public double getGlyphHeightForLayout(GenomeItem item, DataAccessContext rcx) {
    return (APPROX_DIAM_); 
  }  
  
  /***************************************************************************
  **
  ** Return the width
  */
  
  public double getWidth(GenomeItem item, DataAccessContext rcx) {  
    return (APPROX_DIAM_); 
  }        
    
  /***************************************************************************
  **
  ** Render the launch and landing pads
  */
  
	private void renderPads(ModalShapeContainer group, Color col, GenomeItem item, DataAccessContext rcx) {
		
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
		NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
		Point2D origin = np.getLocation();
		//g2.setPaint(col);
		//g2.setStroke(new BasicStroke(1));
		Vector2D lpo = getLaunchPadOffset(0, item, rcx);
		double x = origin.getX() + lpo.getX();
		double y = origin.getY() + lpo.getY();
		double padRadius = (PAD_WIDTH_ / 2.0) - 1.0;
		//Ellipse2D circ = new Ellipse2D.Double(x - padRadius, y - padRadius, 2.0 * padRadius, 2.0 * padRadius);
		//g2.draw(circ);
		
		ModelObjectCache.Ellipse circ1 = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, new BasicStroke(1));
		group.addShape(circ1, majorLayer, minorLayer);
		
		lpo = getLaunchPadOffset(1, item, rcx);
		x = origin.getX() + lpo.getX();
		y = origin.getY() + lpo.getY();
		// circ = new Ellipse2D.Double(x - padRadius, y - padRadius, 2.0 * padRadius, 2.0 * padRadius);
		// g2.draw(circ);
		
		ModelObjectCache.Ellipse circ2 = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, new BasicStroke(1));
		group.addShape(circ2, majorLayer, minorLayer);
		
		return;
	}
  
  // TODO remove old impl
  private void renderPadsOld(Graphics2D g2, RenderObjectCache cache, Color col, 
                          GenomeItem item, DataAccessContext rcx) {
                       
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    g2.setPaint(col); 
    g2.setStroke(new BasicStroke(1));
    Vector2D lpo = getLaunchPadOffset(0, item, rcx);
    double x = origin.getX() + lpo.getX();
    double y = origin.getY() + lpo.getY();
    double padRadius = (PAD_WIDTH_ / 2.0) - 1.0;
    Ellipse2D circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
                                          2.0 * padRadius, 2.0 * padRadius);
    g2.draw(circ);    
    
    lpo = getLaunchPadOffset(1, item, rcx);
    x = origin.getX() + lpo.getX();
    y = origin.getY() + lpo.getY();
    circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
                                2.0 * padRadius, 2.0 * padRadius);
    g2.draw(circ);            
    return;
  }

  /***************************************************************************
  **
  ** Figure out which pad we intersect
  */
  
  private List<Intersection.PadVal> calcPadIntersects(GenomeItem item, DataAccessContext rcx, Point2D pt) {

    ArrayList<Intersection.PadVal> retval = new ArrayList<Intersection.PadVal>();
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    Vector2D clickVec = new Vector2D(origin, pt);
    for (int i = 0; i < 2; i++) {
      Vector2D lpo = getLaunchPadOffset(i, item, rcx);
      double dot = lpo.dot(clickVec);
      if (dot >= 0.0) {
        double x = origin.getX() + lpo.getX();
        double y = origin.getY() + lpo.getY();
        double px = pt.getX();
        double py = pt.getY();
        double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
        Intersection.PadVal retpad = new Intersection.PadVal();
        retpad.okEnd = true;
        retpad.okStart = true;
        retpad.padNum = i;
        retpad.distance = Math.sqrt(distSq);
        retval.add(retpad);
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  } 

  /***************************************************************************
  **
  ** Figure out which pad we intersect
  
  
  private List calcPadIntersects(GenomeItem item, 
                                 Layout layout,
                                 FontRenderContext frc, Point2D pt) {

    ArrayList retval = new ArrayList();
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    for (int i = 0; i < 2; i++) {
      Vector2D lpo = getLaunchPadOffset(i, item, layout, frc);
      double x = origin.getX() + lpo.getX();
      double y = origin.getY() + lpo.getY();
      double padRadius = (PAD_WIDTH_ / 2.0) - 1.0;
      double prSq = padRadius * padRadius;
      double px = pt.getX();
      double py = pt.getY();
      double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
      if (distSq <= prSq) {
        Intersection.PadVal retpad = new Intersection.PadVal();
        retpad.okEnd = true;
        retpad.okStart = true;
        retpad.padNum = i;
        retpad.distance = Math.sqrt(distSq);
        retval.add(retpad);
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  }
  */ 
}
