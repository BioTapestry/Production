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
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.BoundShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc.Type;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.SelectedModalShapeContainer;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Abstract base class for bubbles and diamonds 
*/

public abstract class AbstractTabletNodeFree extends NodeRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  protected static final double TEXT_PAD_             = 12.0;
  protected static final float PAD_WIDTH_             = 15.0F; 
  protected static final double SELECTION_RADIUS_PAD_ = 7.0;   
  protected static final double PAD_SPACING_          = 20.0;
  protected static final int MAX_PADS_                = 4;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public AbstractTabletNodeFree() {
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
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, DataAccessContext rcx, Object miscInfo) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.forWeb) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
    Point2D origin = np.getLocation();
    String name = item.getName();
    double x = origin.getX();
    double y = origin.getY();
    
    Rectangle2D extraPad = extraPadRectangle(item, rcx.getLayout());

    //
    // Modify the ghosted state to include inactive nodes:
    //
    
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
    
    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "tablet");
    
    tabletSelectionSupport(group, selected, extraPad, origin.getX(), origin.getY(), np, rcx.forWeb);
  
    if (rcx.showBubbles) {
    	renderPads(group, (isGhosted) ? Color.LIGHT_GRAY : Color.BLACK, item, MAX_PADS_, PAD_WIDTH_, rcx);
    }
    
    renderGlyph(group, item, origin, extraPad, col, np, rcx.getDisplayOptsSource().getDisplayOptions(), isGhosted);    
    
    double textX = x + TEXT_PAD_;
    double textY = y - TEXT_PAD_;
    
    if (extraPad != null) {
      if (np.getExtraGrowthDirection() == NodeProperties.HORIZONTAL_GROWTH) {
        textX = extraPad.getMaxX() + TEXT_PAD_;
      } else {
        textY = extraPad.getMinY() - TEXT_PAD_;
      }
    }
    
    textCol = (textGhosted) ? Color.LIGHT_GRAY : textCol;
    Point2D textEnd = renderText(group, (float)textX, (float)textY, name, textCol, np.getHideName(), amFont, np.getLineBreakDef(), rcx.getFrc(), rcx.fmgr, textFactory);
    
    // alpha beta chi "\u03b1 \u03b2 \u03c7"
    
    //
    // Draw activity pie:
    //
    
    Point2D pieCenter = new Point2D.Double(textEnd.getX() + 15.0, textEnd.getY());
    
    drawVariableActivityPie(group, item, (np.getColor().equals(Color.WHITE)) ? Color.BLACK : col, pieCenter, rcx.getDisplayOptsSource().getDisplayOptions());

    setGroupBounds(group, item, rcx);

    cache.addGroup(group);
  }

  /***************************************************************************
  **
  ** Fills an array with bound shapes that will be exported in the model map
  ** in the web application.
  */
  void fillBoundsArray(ArrayList<ModelObjectCache.ModalShape> targetArray, GenomeItem item, 
                       DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();

  //  double halfWidth = glyphHalfWidth();
  //  double halfHeight = glyphHalfHeight();
   // double maxRadius = Math.max(halfWidth, halfHeight);
   // double intersectSq = maxRadius * maxRadius;

    TextStats stats = genTextStats(extraPadRectangle(item, rcx.getLayout()),
            origin, np.getExtraGrowthDirection());

    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {
        MultiLineRenderSupport.fillBoundsArrayTextLabel(targetArray, (float)stats.textX, (float)stats.textY, name,
                np.getHideName(), FontManager.MEDIUM, np.getLineBreakDef(), rcx.getFrc(), rcx.fmgr);
    }

    //double cX = origin.getX();
   // double cY = origin.getY();


    // The 'miscinfo' parameter is not used in getBounds, so send in null
    Rectangle trueRect = getBounds(item, rcx, null);

    ModelObjectCache.Rectangle trueRectBounds = ModalShapeFactory.buildBoundRectangleForGlyph(trueRect);
    targetArray.add(trueRectBounds);

    /*
    Rectangle2D myBounds = new Rectangle2D.Double(cX - maxRadius, cY - maxRadius,
            2.0 * maxRadius, 2.0 * maxRadius);


    ModelObjectCache.Rectangle myBoundsRect = ModalShapeFactory.buildBoundRectangleForGlyph(myBounds);
    targetArray.add(myBoundsRect);
    */

    // TODO implement
    /*
    List<Intersection.PadVal> pads = calcPadIntersects(item, layout, frc, pt);
    if (pads != null) {
      return (new Intersection(item.getID(), pads));
    } else if (stats.extra == null) {
      if (distsq <= intersectSq) {
        return (new Intersection(item.getID(), null, Math.sqrt(distsq)));
      }
    } else {  // have an oblong node
      double ptX = pt.getX();
      double ptY = pt.getY();
      if (Bounds.intersects(stats.extra.getX(), stats.extra.getY(),
              stats.extra.getMaxX(), stats.extra.getMaxY(), ptX, ptY)) {
        return (new Intersection(item.getID(), null, 0.0));
      } else {  // look at the rounded ends
        double rdistsq;
        double ldistsq;
        if (stats.extraDir == NodeProperties.HORIZONTAL_GROWTH) {
          double rdx = stats.extra.getMaxX() - ptX;
          double rdy = y - ptY;
          rdistsq = (rdx * rdx) + (rdy * rdy);
          double ldx = stats.extra.getX() - ptX;
          ldistsq = (ldx * ldx) + (rdy * rdy);
        } else { // vertical
          double tdx = x - ptX;
          double tdy = stats.extra.getY() - ptY;
          rdistsq = (tdx * tdx) + (tdy * tdy);
          double bdy = stats.extra.getMaxY() - ptX;
          ldistsq = (tdx * tdx) + (bdy * bdy);
        }
        if ((rdistsq <= intersectSq) || (ldistsq <= intersectSq)) {
          return (new Intersection(item.getID(), null, 0.0));
        }
      }
    }
    */
  }

  /***************************************************************************
  **
  ** Sets the bound shapes for the CacheGroup, to be exported in the
  ** web application.
  */
  public void setGroupBounds(BoundShapeContainer group, GenomeItem item, DataAccessContext rcx) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, item, rcx);
    for (ModelObjectCache.ModalShape ms : bounds) {
      group.addBoundsShape(ms);
    }
  }

  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract void renderGlyph(ModalShapeContainer group, GenomeItem item, Point2D origin, 
                                      Rectangle2D extraPad, Color fillColor, NodeProperties np, 
                                      DisplayOptions dopt, boolean isGhosted);

  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract double glyphHalfWidth();  
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract double glyphHalfHeight();    
   
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract double glyphLineWidth();  
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract double glyphPadWidth();
  
  /***************************************************************************
  **
  ** Implement in concrete classes
  */
  
  protected abstract double glyphPadTweak();  
  
  /***************************************************************************
  **
  ** Useful helper:
  */
  
  private TextStats genTextStats(Rectangle2D extraRect, Point2D origin, int extraDir) {

    double x = origin.getX();
    double y = origin.getY();
    double halfWidth = glyphHalfWidth();
    double halfHeight = glyphHalfHeight();    
    double maxRadius = Math.max(halfWidth, halfHeight);
    double bigIntersect = maxRadius + PAD_WIDTH_; 
    double bigIntersectSq  = bigIntersect * bigIntersect;    
    TextStats retval = new TextStats();
    
    // Quick kill:
    retval.extra = extraRect; 
    retval.extraDir = extraDir;
    boolean defaultLength = (retval.extra == null);
    
    if (defaultLength) {
      retval.sqRad = bigIntersectSq;
      retval.textPad = TEXT_PAD_;
      retval.textX = x + TEXT_PAD_;
      retval.textY = y - TEXT_PAD_;      
    } else if (retval.extraDir == NodeProperties.HORIZONTAL_GROWTH) {
      double width = retval.extra.getWidth() / 2.0;
      double rad = (width + halfWidth) * 1.25F;     
      retval.sqRad = rad * rad;
      retval.textPad = TEXT_PAD_ + width;
      retval.textX = retval.extra.getMaxX() + TEXT_PAD_;
      retval.textY = y - TEXT_PAD_;      
    } else {  // Vertical growth
      double height = retval.extra.getHeight() / 2.0;
      double rad = (height + halfHeight) * 1.25F;     
      retval.sqRad = rad * rad;
      retval.textPad = TEXT_PAD_;
      retval.textX = x + TEXT_PAD_;
      retval.textY = retval.extra.getY() - TEXT_PAD_;     
    }   
    return (retval);  
  }
   
  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {

    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double x = origin.getX();
    double y = origin.getY();
    double dx = x - pt.getX();
    double dy = y - pt.getY();
    double distsq = (dx * dx) + (dy * dy);

    double halfWidth = glyphHalfWidth();
    double halfHeight = glyphHalfHeight();    
    double maxRadius = Math.max(halfWidth, halfHeight);
    double intersectSq = maxRadius * maxRadius;

    TextStats stats = genTextStats(extraPadRectangle(item, rcx.getLayout()), 
                                   origin, np.getExtraGrowthDirection());
       
    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {         
      if (isTextCandidate(rcx.getFrc(), FontManager.MEDIUM, np.getFontOverride(), name, stats.textPad, 
                          dx, distsq, np.getLineBreakDef(), false, np.getHideName(), rcx.fmgr)) {
        if (intersectTextLabel(rcx.getFrc(), (float)stats.textX, (float)stats.textY, 
                               name, pt, np.getHideName(), FontManager.MEDIUM, np.getFontOverride(), np.getLineBreakDef(), rcx.fmgr)) {
          return (new Intersection(item.getID(), null, 0.0));
        }
      }
    }
        
    if (distsq > stats.sqRad) {
      return (null);
    }
    
    List<Intersection.PadVal> pads = calcPadIntersects(item, pt, rcx);
    if (pads != null) {
      return (new Intersection(item.getID(), pads));
    } else if (stats.extra == null) {
      if (distsq <= intersectSq) {
        return (new Intersection(item.getID(), null, Math.sqrt(distsq)));
      }
    } else {  // have an oblong node
      double ptX = pt.getX(); 
      double ptY = pt.getY();
      if (Bounds.intersects(stats.extra.getX(), stats.extra.getY(), 
                            stats.extra.getMaxX(), stats.extra.getMaxY(), ptX, ptY)) {
        return (new Intersection(item.getID(), null, 0.0));       
      } else {  // look at the rounded ends
        double rdistsq;         
        double ldistsq;
        if (stats.extraDir == NodeProperties.HORIZONTAL_GROWTH) {
          double rdx = stats.extra.getMaxX() - ptX;
          double rdy = y - ptY;
          rdistsq = (rdx * rdx) + (rdy * rdy);         
          double ldx = stats.extra.getX() - ptX;
          ldistsq = (ldx * ldx) + (rdy * rdy);
        } else { // vertical
          double tdx = x - ptX;
          double tdy = stats.extra.getY() - ptY;
          rdistsq = (tdx * tdx) + (tdy * tdy);         
          double bdy = stats.extra.getMaxY() - ptX;
          ldistsq = (tdx * tdx) + (bdy * bdy);          
        }
        if ((rdistsq <= intersectSq) || (ldistsq <= intersectSq)) {
          return (new Intersection(item.getID(), null, 0.0));
        }                  
      }
    }
    return (null);
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
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal.
  ** The bounds are returned with the placement point at (0,0). 
  */
  
  public Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, 
                                        int orientation, boolean labelToo, Integer topPadCount) {
     NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
     Node theBox = (Node)item;
     int type = theBox.getNodeType();
     Point2D origin = new Point2D.Double(0.0, 0.0);
     int totalPads = (topPadCount == null) ? theBox.getPadCount() : (topPadCount.intValue() * 2) + 2;
     
     Rectangle basic = getBoundsGuts(NodeProperties.HORIZONTAL_GROWTH, totalPads, origin, type);     
 
     if (labelToo && !np.getHideName()) {
       Rectangle2D extraRect = extraPadRectangleForPads(totalPads, NodeProperties.HORIZONTAL_GROWTH, 
                                                        origin, theBox.getNodeType());
       TextStats stats = genTextStats(extraRect, origin, NodeProperties.HORIZONTAL_GROWTH);

       AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
       String breakDef = np.getLineBreakDef();    
       Rectangle2D textRect = getTextBounds(rcx.getFrc(),(float)stats.textX, (float)stats.textY, 
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
    Point2D origin = new Point2D.Double(0.0, 0.0);
    Node theBox = (Node)item;
    int totalPads = (topPadCount == null) ? theBox.getPadCount() : (topPadCount.intValue() * 2) + 2; 
    return (getLandingPadOffsetGuts(1, item, origin, Linkage.NONE, NodeProperties.HORIZONTAL_GROWTH, totalPads));
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
    double halfWidth = glyphHalfWidth();
    double halfHeight = glyphHalfHeight();    
    double maxRadius = Math.max(halfWidth, halfHeight); 
    Rectangle2D myBounds = new Rectangle2D.Double(cX - maxRadius, cY - maxRadius,
                                                  2.0 * maxRadius, 2.0 * maxRadius);
    Rectangle2D inbounds = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
    if (countPartial) {
      // 1/28/09 Note the above code is bogus when dealing with expanded nodes!  Glyph
      // stuff is fixed.  Most rectangle intersections are kinda inexact anyway, but we
      // need to know stuff correctly for partial intersections
      Rectangle trueRect = getBounds(item, rcx, miscInfo);
      double minX = trueRect.x;
      double minY = trueRect.y;
      double maxX = minX + trueRect.width;
      double maxY = minY + trueRect.height;      
      Point2D chkPt = new Point2D.Double(minX, minY);
      boolean gotIt = inbounds.contains(chkPt);
      if (!gotIt) {
        chkPt.setLocation(maxX, minY);
        gotIt = inbounds.contains(chkPt);
      }
      if (!gotIt) {
        chkPt.setLocation(maxX, maxY);
        gotIt = inbounds.contains(chkPt);
      }
      if (!gotIt) {
        chkPt.setLocation(minX, maxY);
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
  ** Get the landing pad offset
  */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, 
                                      DataAccessContext icx) {    
    Node theBubble = (Node)item;
    int totalPads = theBubble.getPadCount();
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int growthDir = np.getExtraGrowthDirection();    
    return (getLandingPadOffsetGuts(padNum, item, origin, sign, growthDir, totalPads));
  }
  
  /***************************************************************************
  **
  ** Get the landing pad offset
  */
  
  private Vector2D getLandingPadOffsetGuts(int padNum, GenomeItem item, Point2D origin, int sign,
                                           int growthDir, int totalPads) {
    float negPosTweak = 0.0F;
    if (sign == Linkage.NEGATIVE) {
      negPosTweak = NEG_OFFSET;
    } else if (sign == Linkage.POSITIVE) {
      negPosTweak = POS_OFFSET;
    }
    double baseOffsetH = glyphHalfWidth() + glyphLineWidth() - glyphPadTweak();    
    double baseOffsetV = glyphHalfHeight() + glyphLineWidth() - glyphPadTweak();
    double finalOffsetH = baseOffsetH + negPosTweak;
    double finalOffsetV = baseOffsetV + negPosTweak;    
    boolean horiz = (growthDir == NodeProperties.HORIZONTAL_GROWTH);
    
    double finalOffset = (horiz) ? finalOffsetV : finalOffsetH;
    if (padNum < 0) {
      return (getExtraLandingPadOffset(padNum, finalOffset, horiz, PAD_SPACING_));
    }
    Rectangle2D extra = extraPadRectangleForPads(totalPads, growthDir, origin, ((Node)item).getNodeType()); 
    double topExtra = 0.0;
    double sideExtra = 0.0;
    if (extra != null) {
      if (horiz) {
        sideExtra = (extra.getWidth() / 2.0);
      } else {
        topExtra = (extra.getHeight() / 2.0);
      }
      sideExtra = UiUtil.forceToGridValueMax(sideExtra, UiUtil.GRID_SIZE);
      topExtra = UiUtil.forceToGridValueMax(topExtra, UiUtil.GRID_SIZE);      
    }
        
    switch (padNum) {
      case 0:
        return (new Vector2D(0.0, -finalOffsetV - topExtra));
      case 1:
        return (new Vector2D(finalOffsetH + sideExtra, 0.0));
      case 2:
        return (new Vector2D(0.0, finalOffsetV + topExtra));
      case 3:
        return (new Vector2D(-finalOffsetH - sideExtra, 0.0));
      default:
        throw new IllegalArgumentException();
    }
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
    return (glyphPadWidth());
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
  ** Answer if landing pads can overflow (e.g. be assigned negative values)
  */
  
  public boolean landingPadsCanOverflow() {
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    Node theBubble = (Node)item;
    int totalPads = theBubble.getPadCount();
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int growthDir = np.getExtraGrowthDirection();   
    return (getBoundsGuts(growthDir, totalPads, origin, theBubble.getNodeType()));
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  private Rectangle getBoundsGuts(int growthDirection, int totalPads, Point2D origin, int nodeType) {
    int halfWidth = (int)glyphHalfWidth();
    int halfHeight = (int)glyphHalfHeight();        
    Rectangle2D extra = extraPadRectangleForPads(totalPads, growthDirection, origin, nodeType);
    if (extra != null) {
      if (growthDirection == NodeProperties.HORIZONTAL_GROWTH) {
        return (new Rectangle((int)extra.getX() - halfWidth, (int)extra.getY(), 
                              (int)extra.getWidth() + (2 * halfWidth), 
                              (int)extra.getHeight())); 
      } else {
        return (new Rectangle((int)extra.getX(), (int)extra.getY() - halfHeight, 
                              (int)extra.getWidth(), 
                              (int)extra.getHeight() + (2 * halfHeight)));         
      }
    } else { 
      int x = (int)origin.getX();
      int y = (int)origin.getY();
      return (new Rectangle(x - halfWidth, y - halfHeight, 2 * halfWidth, 2 * halfHeight)); 
    }
  }

  /***************************************************************************
  **
  ** Return the height
  */
  
  public double getHeight(GenomeItem item, DataAccessContext rcx) {
    double retval = glyphHalfHeight() * 2.0;        
    Rectangle2D extra = extraPadRectangle(item, rcx.getLayout());
    if (extra != null) {
      NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
      if (np.getExtraGrowthDirection() == NodeProperties.HORIZONTAL_GROWTH) {
        return (retval);
      } else {
        return (retval + extra.getHeight());
      }
    } else {
      return (retval);
    }
  }  
  
  /***************************************************************************
  **
  ** Return the height
  */
  
  public double getGlyphHeightForLayout(GenomeItem item, DataAccessContext rcx) {
    return (glyphHalfHeight() * 2.0);        
  }  

  /***************************************************************************
  **
  ** Return the width
  */
  
  public double getWidth(GenomeItem item, DataAccessContext rcx) {
    double retval = glyphHalfWidth() * 2.0;        
    Rectangle2D extra = extraPadRectangle(item, rcx.getLayout());
    if (extra != null) {
      NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
      if (np.getExtraGrowthDirection() == NodeProperties.HORIZONTAL_GROWTH) {
        return (retval + extra.getWidth());
      } else {
        return (retval);
      }
    } else {
      return (retval);
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
  ** Figure out which pad we intersect 
  */
  
  protected List<Intersection.PadVal> calcPadIntersects(GenomeItem item, Point2D pt, DataAccessContext rcx) {     
    return (calcSharedNamespacePadIntersectSupport(item, pt, MAX_PADS_, PAD_WIDTH_, rcx));  
  }  
  
  /***************************************************************************
  **
  ** Handle selection highlighting
  */
  
  protected void tabletSelectionSupport(SelectedModalShapeContainer group, Intersection selected, Rectangle2D extraPad, 
                                       double x, double y, NodeProperties np, boolean isWeb) {
    if (!isWeb && selected == null) {
      return;
    }

  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    double halfWidth = glyphHalfWidth();
    double halfHeight = glyphHalfHeight();    
    double maxRadius = Math.max(halfWidth, halfHeight);    
    double bigRadius = maxRadius + SELECTION_RADIUS_PAD_;
    if (extraPad == null) {  
      ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, bigRadius,
                                            DrawMode.FILL, Color.orange, new BasicStroke());
      
      group.addShapeSelected(circ, majorLayer, minorLayer);

      return;
    }    
    
    int growthDir = np.getExtraGrowthDirection();
    if (growthDir == NodeProperties.HORIZONTAL_GROWTH) {
      x = extraPad.getX();
      y = extraPad.getY() - SELECTION_RADIUS_PAD_ + bigRadius;
      
      /*
			ModelObjectCache.Arc arc11 = new ModelObjectCache.Arc(x - bigRadius, y - SELECTION_RADIUS_PAD_,
					2.0 * bigRadius, 2.0 * bigRadius, 90, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());
			
			x += extraPad.getWidth();
			ModelObjectCache.Arc arc12 = new ModelObjectCache.Arc(x - bigRadius, y - SELECTION_RADIUS_PAD_,
					2.0 * bigRadius, 2.0 * bigRadius, 270, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());
      */
      
			ModelObjectCache.Arc arc11 = new ModelObjectCache.Arc(x, y, bigRadius, 90, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());
			
			x += extraPad.getWidth();
			ModelObjectCache.Arc arc12 = new ModelObjectCache.Arc(x, y, bigRadius, 270, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());
            
      ModelObjectCache.Rectangle selectedBounds = new ModelObjectCache.Rectangle(extraPad.getX(), extraPad.getY() - SELECTION_RADIUS_PAD_,
          extraPad.getWidth(), extraPad.getHeight() + (2.0 * SELECTION_RADIUS_PAD_),
          DrawMode.FILL, Color.orange, new BasicStroke());
      
      group.addShapeSelected(arc11, majorLayer, minorLayer);
      group.addShapeSelected(arc12, majorLayer, minorLayer);
      group.addShapeSelected(selectedBounds, majorLayer, minorLayer);
      
    } else {
      x = extraPad.getX() - SELECTION_RADIUS_PAD_ + bigRadius;
      y = extraPad.getY();
      
      /*
   		ModelObjectCache.Arc arc11 = new ModelObjectCache.Arc(x - SELECTION_RADIUS_PAD_, y - bigRadius,
      2.0 * bigRadius, 2.0 * bigRadius, 0, 180,
			Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());

      y += extraPad.getHeight();
			ModelObjectCache.Arc arc12 = new ModelObjectCache.Arc(x - SELECTION_RADIUS_PAD_, y - bigRadius,
      2.0 * bigRadius, 2.0 * bigRadius, 180, 180,
			Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());
       */
      
			ModelObjectCache.Arc arc11 = new ModelObjectCache.Arc(x, y, bigRadius, 0, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());

      y += extraPad.getHeight();
			ModelObjectCache.Arc arc12 = new ModelObjectCache.Arc(x, y, bigRadius, 180, 180,
					Type.OPEN, DrawMode.FILL, Color.orange, new BasicStroke());    
      
      ModelObjectCache.Rectangle selectedBounds = new ModelObjectCache.Rectangle(extraPad.getX() - SELECTION_RADIUS_PAD_, extraPad.getY(),
          extraPad.getWidth()+ (2.0 * SELECTION_RADIUS_PAD_), 
          extraPad.getHeight(),
          DrawMode.FILL, Color.orange, new BasicStroke());
      
      group.addShapeSelected(arc11, majorLayer, minorLayer);
      group.addShapeSelected(arc12, majorLayer, minorLayer);
      group.addShapeSelected(selectedBounds, majorLayer, minorLayer);
    }
  }

  /***************************************************************************
  **
  ** If this node has extra pads, we return a rectangle that provides the
  ** extra sizing
  */
  
  protected Rectangle2D extraPadRectangle(GenomeItem item, Layout layout) {
    Node theBubble = (Node)item;
    int totalPads = theBubble.getPadCount();
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int growthDir = np.getExtraGrowthDirection();
    return (extraPadRectangleForPads(totalPads, growthDir, origin, theBubble.getNodeType())); 
  }
  
  /***************************************************************************
  **
  ** If this node has extra pads, we return a rectangle that provides the
  ** extra sizing
  */
  
  private Rectangle2D extraPadRectangleForPads(int numPads, int growthDir, Point2D origin, int nodeType) {
    int defaultPads = DBNode.getDefaultPadCount(nodeType);
    if (numPads <= defaultPads) {
      return (null);
    }
    int extraPerSide = (numPads - MAX_PADS_) / 2;
    double extraSize = (double)(extraPerSide) * PAD_SPACING_;
    if (growthDir == NodeProperties.HORIZONTAL_GROWTH) {
      double height = 2.0 * glyphHalfHeight();
      double minX = origin.getX() - extraSize / 2.0;
      double minY = origin.getY() - glyphHalfHeight();       
      return (new Rectangle2D.Double(minX, minY, extraSize, height));
    } else {
      double width = 2.0 * glyphHalfWidth();
      double minX = origin.getX() - glyphHalfWidth();      
      double minY = origin.getY() - extraSize / 2.0;
      return (new Rectangle2D.Double(minX, minY, width, extraSize));      
    }
  }
 
  /***************************************************************************
  **
  ** Internal use
  */
  
  private static class TextStats {
    double sqRad;
    double textPad;
    double textX;
    double textY;
    Rectangle2D extra;
    int extraDir;
  }
 
}
