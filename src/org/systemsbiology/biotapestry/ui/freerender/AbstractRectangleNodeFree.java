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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.BoundShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.SelectedModalShapeContainer;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.Vector2D;


/****************************************************************************
**
** This astract base class is for rectanglar text nodes
*/

public abstract class AbstractRectangleNodeFree extends NodeRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  protected static final double TEXT_PAD_ = 5.0;
  // Font bounds gives fixed height that is ~ 33% taller than upper case,
  // so hack it to get it centered.
  protected static final double HEIGHT_HACK_ = 0.75;
  protected static final double PAD_WIDTH_ = 15.0;
  protected static final double PAD_SPACING_ = 20.0;  
  protected static final int MAX_PADS_          = 4;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public AbstractRectangleNodeFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Sets the bound shapes for the CacheGroup, to be exported in the
  ** web application.
  */
  public void setGroupBounds(BoundShapeContainer group, GenomeItem item, DataAccessContext rcx) {
  	if (!rcx.forWeb) {
  		return;
  	}
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);

    // TODO getX was used here originally instead of getMinX
    double minX = bounds.getMinX();
    // TODO getY was used here originally instead of getMinY
    double minY = bounds.getMinY();
    double maxX = bounds.getMaxX();
    double maxY = bounds.getMaxY();

    ModelObjectCache.Rectangle boundsRect = ModalShapeFactory.buildBoundRectangleForGlyph(minX, minY, (maxX-minX), (maxY-minY));
    if (group != null) {
    	group.addBoundsShape(boundsRect);
    }
  }

  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
                              
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);
    double width = bounds.getWidth();
    double height = bounds.getHeight();
    double radius = (width > height) ? width : height;
    double dx = origin.getX() - pt.getX();
    double dy = origin.getY() - pt.getY();
    double distsq = (dx * dx) + (dy * dy);
    if (distsq <= (radius * radius)) {
      List<Intersection.PadVal> pads = calcPadIntersects(item, pt, rcx);
      if (pads != null) {
        return (new Intersection(item.getID(), pads));
      } else if (Bounds.intersects(bounds.getX(), bounds.getY(), 
                                   bounds.getMaxX(), bounds.getMaxY(), pt.getX(), pt.getY())) {
        return (new Intersection(item.getID(), null, 0.0));
      } else {
        return (null);
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
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);
    double width = bounds.getWidth();
    double height = bounds.getHeight();
    double minX = origin.getX() - width / 2.0 - (PAD_WIDTH_ / 2.0);  // PAD IS HACK: FIX ME
    double minY = origin.getY() - height / 2.0 - (PAD_WIDTH_ / 2.0);  // PAD IS HACK: FIX ME
       
    Rectangle2D myBounds = new Rectangle2D.Double(minX, minY, width, height);
    Rectangle2D inbounds = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
    if (countPartial) {
      Point2D chkPt = new Point2D.Double(minX, minY);
      boolean gotIt = inbounds.contains(chkPt);
      if (!gotIt) {
        chkPt.setLocation(minX + width, minY);
        gotIt = inbounds.contains(chkPt);
      }
      if (!gotIt) {
        chkPt.setLocation(minX + width, minY + height);
        gotIt = inbounds.contains(chkPt);
      }
      if (!gotIt) {
        chkPt.setLocation(minX, minY + height);
        gotIt = inbounds.contains(chkPt);
      }
      if (gotIt) {
        return (new Intersection(item.getID(), null, 0.0));
      }
    } else {
      if (inbounds.contains(myBounds)) { 
        return (new Intersection(item.getID(), null, 0.0));
      }      
    }
    return (null);                          
  }
  
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, 
                                      DataAccessContext icx) {
    
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    Node theBox = (Node)item;
    int totalPads = theBox.getPadCount();  
    return (getLandingPadOffsetGuts(padNum, item, sign, origin, totalPads, np, icx));
  }
  
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  private Vector2D getLandingPadOffsetGuts(int padNum, GenomeItem item, int sign,
                                           Point2D origin, int totalPads, NodeProperties np, DataAccessContext icx) {
      
    Rectangle2D bounds = boundsWithXtraPadsGuts(item, null, origin, totalPads, np, icx);        
    double width = bounds.getWidth();
    double height = bounds.getHeight();
    
    double padTweak = padTweak(sign);
    double vVal = (height / 2.0) + padTweak;
    double hVal = (width / 2.0) + padTweak;
    if (padNum < 0) {
      return (getExtraLandingPadOffset(padNum, vVal, true, PAD_SPACING_));
    }
    switch (padNum) {
      case 0:
        return (new Vector2D(0.0, -vVal));
      case 1:
        return (new Vector2D(hVal, 0.0));
      case 2:
        return (new Vector2D(0.0, vVal));
      case 3:
        return (new Vector2D(-hVal, 0.0));
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get tweaks for  pads
  */
  
  protected double padTweak(int sign) {
    return (0.0);
  }
  
  /***************************************************************************
  **
  ** Figure out which pad we intersect 
  */
  
  public List<Intersection.PadVal> calcPadIntersects(GenomeItem item, 
                                                        Point2D pt, DataAccessContext icx) {     
    return (calcSharedNamespacePadIntersectSupport(item, pt, MAX_PADS_, PAD_WIDTH_, icx));  
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  public Vector2D getLaunchPadOffset(int padNum, GenomeItem item, DataAccessContext icx) {
    // uses same algorithm:
    return (getLandingPadOffset(padNum, item, Linkage.NONE, icx));  
  }
  
  /***************************************************************************
  **  
  ** Get the landing pad width
  */
  
  public double getLandingPadWidth(int padNum, GenomeItem item, Layout layout) {
    return (PAD_WIDTH_);
  }
  
 /***************************************************************************
  **
  ** Answer if landing pads can overflow (e.g. be assigned negative values)
  */
  
  public boolean landingPadsCanOverflow() {
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Get the maximum number of launch pads
  */
  
  public int getFixedLaunchPadMax() {
    return (MAX_PADS_);
  }  
  
  /***************************************************************************
  **
  ** Get the maximum number of landing pads
  */
  
  public int getFixedLandingPadMax() {
    return (MAX_PADS_);
  }

  
  /***************************************************************************
  **
  ** Figure out if we have extra pad width
  */
  
  protected boolean haveExtraPadWidth(GenomeItem item, FontRenderContext frc, NodeProperties np, FontManager fmgr) {
    AnnotatedFont mFont = fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    String name = item.getName();
    Rectangle2D bounds = mFont.getFont().getStringBounds(name, frc);
    double width = bounds.getWidth() + (TEXT_PAD_ * 2.0);
    
    Node theBox = (Node)item;
    int numPads = theBox.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    if (numPads > defaultPads) {
      int extraPerSide = (numPads - MAX_PADS_) / 2;
      double minWidth = (double)(extraPerSide + 2) * PAD_SPACING_;
      if (minWidth > width) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Figure out if we have might have extra pad width
  */
  
  protected boolean mightHaveExtraPadWidth(GenomeItem item) {
    Node theBox = (Node)item;
    int numPads = theBox.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    return (numPads > defaultPads);
  }  
 
  /***************************************************************************
  **
  ** Figure out the bounds, including extra pad contribution
  */
  
  protected Rectangle2D boundsWithXtraPads(GenomeItem item, 
                                           Rectangle2D textBounds, NodeProperties np, DataAccessContext icx) {                       
    Point2D origin = np.getLocation();
    Node theBox = (Node)item;
    int totalPads = theBox.getPadCount();
    return (boundsWithXtraPadsGuts(item, textBounds, origin, totalPads, np, icx));
  } 
  
  /***************************************************************************
  **
  ** Figure out the bounds, including extra pad contribution
  */
  
  private Rectangle2D boundsWithXtraPadsGuts(GenomeItem item,
                                             Rectangle2D textBounds, Point2D origin,
                                             int totalPads, NodeProperties np, DataAccessContext icx) {

    AnnotatedFont mFont = icx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    String name = item.getName();
    Rectangle2D bounds = mFont.getFont().getStringBounds(name, icx.getFrc());
    if (textBounds != null) {
      textBounds.setRect(bounds);
    }
    double width = bounds.getWidth() + (TEXT_PAD_ * 2.0);
    
    Node theBox = (Node)item;
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    if (totalPads > defaultPads) {
      int extraPerSide = (totalPads - MAX_PADS_) / 2;
      double minWidth = (double)(extraPerSide + 2) * PAD_SPACING_;
      if (minWidth > width) {
        width = minWidth;
      }
    }  
    double height = (bounds.getHeight() * HEIGHT_HACK_) + (TEXT_PAD_ * 2.0);  
    double minX = origin.getX() - width / 2.0;
    double minY = origin.getY() - height / 2.0;  
       
    return (new Rectangle2D.Double(minX, minY, width, height));    
  } 

  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);
       
    return (new Rectangle((int)bounds.getX(), (int)bounds.getY(), 
                          (int)bounds.getWidth(), (int)bounds.getHeight()));
  }
  
  /***************************************************************************
  **
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal.
  ** The bounds are returned with the placement point at (0,0). 
  */
  
  public Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, 
                                        int orientation, boolean labelToo, Integer topPadCount) {
     NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
 
     Point2D origin = new Point2D.Double(0.0, 0.0);
     Node theBox = (Node)item;
     int totalPads = (topPadCount == null) ? theBox.getPadCount() : (topPadCount.intValue() * 2) + 2;
     return (boundsWithXtraPadsGuts(item, null, origin, totalPads, np, rcx));
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset for given layout case:
  */
  
  public Vector2D getLaunchPadOffsetForLayout(GenomeItem item, DataAccessContext rcx, 
                                              int orientation, Integer topPadCount) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = new Point2D.Double(0.0, 0.0);
    Node theBox = (Node)item;
    int totalPads = (topPadCount == null) ? theBox.getPadCount() : (topPadCount.intValue() * 2) + 2;
    return (getLandingPadOffsetGuts(1, item, Linkage.NONE, origin, totalPads, np, rcx));
  }
  
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public int topPadCount(int fullPadCount) {
    return (((fullPadCount + (fullPadCount % 2)) - 2) / 2);
  }
   
  /***************************************************************************
  **
  ** Return the height
  */
  
  public double getGlyphHeightForLayout(GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);
    return (bounds.getHeight());
  }  
  
  /***************************************************************************
  **
  ** Return the width
  */
  
  public double getWidth(GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, null, np, rcx);
    return (bounds.getWidth());
  }   
  
  /***************************************************************************
  **
  ** Return the Rectangle bounding the area where we do not want to allow network
  ** expansion.  May be null.
  */
  
  @Override
  public Rectangle getNonExpansionRegion(GenomeItem item, DataAccessContext rcx) {
    Node theBox = (Node)item;
    int numPads = theBox.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theBox.getNodeType());
    if (numPads > defaultPads) {
      return (getExtraPadNonExpansionRegion(item, rcx));
    } else {
      return (super.getNonExpansionRegion(item, rcx));
    }    
  }

  /***************************************************************************
  **
  ** Compare pads (negative if one is to the left of two).
  */  
  
  @Override
  public int comparePads(int padOne, int padTwo) {
    return (spiralComparePads(padOne, padTwo));
  }
  
  /***************************************************************************
  **
  ** Answer how much extra length will result from the given pad increase
  */

  @Override
  public double getExtraLength(Node node, Genome genome, int newCount, boolean oneSideOnly) {
    int currPadCount = node.getPadCount();
    if (oneSideOnly) {
      currPadCount /= 2;
    }
    int padDiff = newCount - currPadCount;
    //
    // To be conservative, some of the existing links could be on the side or bottom, and
    // will need to be shifted up to the top:
    //
    if (oneSideOnly) {
      padDiff += (MAX_PADS_ - 1);
    }       
    double extra = ((padDiff <= 0) ? 0.0 : PAD_SPACING_ * padDiff);
    return (extra);
  }  
  
  
  /***************************************************************************
  **
  ** Render support
  */
  
  protected Rectangle2D renderSupportA(SelectedModalShapeContainer group, GenomeItem item, 
                                       Intersection selected, Rectangle2D textBounds, DataAccessContext rcx) {
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, textBounds, np, rcx);

    selectionSupport(group, selected, (int)bounds.getX(), (int)bounds.getY(), 
                     (int)bounds.getWidth(), (int)bounds.getHeight(), rcx.forWeb);
    return (bounds);
  }
  
  /*
  protected Rectangle2D renderSupportAOld(Graphics2D g2, RenderObjectCache cache, GenomeItem item, 
                                       Layout layout, Intersection selected, 
                                       Rectangle2D textBounds) {

    FontRenderContext frc = g2.getFontRenderContext();
    NodeProperties np = layout.getNodeProperties(item.getID());
    Rectangle2D bounds = boundsWithXtraPads(item, layout, frc, textBounds, np);

    selectionSupport(g2, cache, selected, (int)bounds.getX(), (int)bounds.getY(), 
                     (int)bounds.getWidth(), (int)bounds.getHeight());
    return (bounds);
  }
  */ 
  
  /***************************************************************************
  **
  ** Render support
  */
  
  protected void renderSupportB(ModalShapeContainer group, GenomeItem item, boolean isGhosted,
      boolean textGhosted,
      AnnotatedFont mFont, Point2D origin,
      Rectangle2D textBounds, Color nodeColor, 
      Color textCol, ModalTextShapeFactory textFactory,
      DataAccessContext rcx) {
  	
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
  	
    if (rcx.showBubbles) {
      renderPads(group, (isGhosted) ? dop.getInactiveGray() : Color.BLACK, item, MAX_PADS_, PAD_WIDTH_, rcx);
    }
    
    String name = item.getName();
		Color finalTextColor = (textGhosted) ? dop.getInactiveGray() : textCol;
		
    float textX = (float)(origin.getX() - textBounds.getWidth() / 2.0);
    float textY = (float)(origin.getY() + ((textBounds.getHeight() * HEIGHT_HACK_) / 2.0));
    
    ModalShape ts = textFactory.buildTextShape(name, mFont, finalTextColor, textX, textY, new AffineTransform());
    group.addShape(ts, majorLayer, minorLayer);
    
    Vector2D lpo = getLaunchPadOffset(1, item, rcx);
    Point2D pieCenter = new Point2D.Double(origin.getX() + lpo.getX() + 15.0, origin.getY() + lpo.getY() - 15.0);
    drawVariableActivityPie(group, item, nodeColor, pieCenter, rcx.getDisplayOptsSource().getDisplayOptions());
  }
  
  /*
  protected void renderSupportBOld(Graphics2D g2, RenderObjectCache roc, GenomeItem item, Layout layout,
                                boolean isGhosted, boolean textGhosted, 
                                boolean showBubbles, Font mFont, 
                                Point2D origin, Rectangle2D textBounds, 
                                Color nodeColor, Color textCol, DisplayOptions dopt) {
    
    FontRenderContext frc = g2.getFontRenderContext();    
    if (showBubbles) {
      renderPads(null, (isGhosted) ?  dopt.getInactiveGray() : Color.BLACK, item, layout, MAX_PADS_, PAD_WIDTH_);
    }

    String name = item.getName();    
    g2.setPaint((textGhosted) ?  dopt.getInactiveGray() : textCol);    
    g2.setFont(mFont);
    float textX = (float)(origin.getX() - textBounds.getWidth() / 2.0);
    float textY = (float)(origin.getY() + ((textBounds.getHeight() * HEIGHT_HACK_) / 2.0));
    g2.drawString(name, textX, textY);
    
    //
    // Draw activity pie:
    //
    
    Vector2D lpo = getLaunchPadOffset(1, item, layout, frc);
    Point2D pieCenter = new Point2D.Double(origin.getX() + lpo.getX() + 15.0, origin.getY() + lpo.getY() - 15.0);
    drawVariableActivityPie(g2, roc, item, nodeColor, pieCenter, dopt);
        
    return;
  }
  */
}
