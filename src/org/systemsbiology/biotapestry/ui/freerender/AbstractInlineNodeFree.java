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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
import org.systemsbiology.biotapestry.ui.NodeInsertionDirective;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.modelobjectcache.CommonCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc.Type;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
 **
 ** This is base class for "inline" (slash, intercell) nodes
 */

public abstract class AbstractInlineNodeFree extends NodeRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final double SELECT_           = 12.0;
  private static final double PAD_SPACING_      = 10.0;
  private static final double EXTRA_BOX_HEIGHT_ = 6.0;
  private static final double SELECT_EXTRA_BOX_HEIGHT_ = 10.0;  
  private static final int STROKE_              = 3;
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
  
  public AbstractInlineNodeFree() {
    super();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Answer if we can be in a simple fan in
  */
 
  @Override
  public boolean simpleFanInOK() {
    return (true);
  }
 
  /***************************************************************************
  ** 
  ** Answer if assigning landing is consistent with direct landing:
  */
  
  @Override
  public boolean landOKForDirect(int landing) {
    return (landing == 0);
  }
  
  /***************************************************************************
  ** 
  ** Answer if node ignores force top directive
  */
  
  @Override
  public boolean ignoreForceTop() {
    return (true);
  }
  
  /***************************************************************************
  ** 
  ** Get the left pad number
  */
  
  @Override
  public int getLeftPad() {
    return (0);
  }

  /***************************************************************************
  **
  ** Render the node
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
    Point2D origin = np.getLocation();
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    int orient = np.getOrientation();
    AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());

    boolean isGhosted = rcx.isGhosted();
    boolean textGhosted = isGhosted;
    if (item instanceof NodeInstance) {
      int activityLevel = ((NodeInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == NodeInstance.VESTIGIAL) || (activityLevel == NodeInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != NodeInstance.VESTIGIAL);
    }
    Color col1Val = np.getColor();
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    Color vac = getVariableActivityColor(item, col1Val, false, dop);
    Color col = (isGhosted) ? dop.getInactiveGray() : vac;
    Color col2Val = np.getSecondColor();
    // Fix for BT-06-04-07:9
    col2Val = (col2Val == null) ? col1Val : col2Val;
    Color vac2 = getVariableActivityColor(item, col2Val, false, dop);
    Color col2 = (isGhosted) ? dop.getInactiveGray() : vac2;    
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, dop);            
    
    //ModelObjectCache.PushRotationWithOrigin pushTrans = getRotationTransform(orient, origin);
    
    CommonCacheGroup group = new CommonCacheGroup(item.getID(), item.getName(), "intercell");
    AffineTransform transOld = getRotationTransformOld(orient, origin);
    
    ModelObjectCache.PushTransformOperation pushTransOld = new ModelObjectCache.PushTransformOperation(transOld);
    group.addShape(pushTransOld, majorLayer, minorLayer);

    // TODO find a better way to do this
    if (rcx.forWeb) {
      group.addShapeSelected(pushTransOld, majorLayer, minorLayer);
    }

    roundSelectionSupport(group, selected, x, y, SELECT_, rcx.forWeb);    
    Rectangle2D extraRect = extraPadRectangle(item, rcx.getLayout());

    // TODO cleanup logic
    if ((selected != null || rcx.forWeb) && (extraRect != null)) {
      paintExtra(group, Color.orange, extraRect, SELECT_EXTRA_BOX_HEIGHT_, true);
    }
       
    BasicStroke pathStroke = new BasicStroke(STROKE_, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    
    ModelObjectCache.SegmentedPathShape pathShape = getPath1Shape(x, y, col, pathStroke);
    group.addShape(pathShape, majorLayer, minorLayer);
    
    pathShape = getPath2Shape(x, y, col2, pathStroke);
    group.addShape(pathShape, majorLayer, minorLayer);
    
    if (extraRect != null) {
      paintExtra(group, col, extraRect, EXTRA_BOX_HEIGHT_, false);
    }
    
    ModelObjectCache.PopTransformOperation popTrans = new ModelObjectCache.PopTransformOperation();
    group.addShape(popTrans, majorLayer, minorLayer);

    // TODO find a better way to do this
    if (rcx.forWeb) {
      group.addShapeSelected(popTrans, majorLayer, minorLayer);
    }
    
    if (rcx.showBubbles) {
      renderPads(group, (isGhosted) ? dop.getInactiveGray() : Color.BLACK, item, rcx);
    }
    
    double textPad = getTextPad();
    Color finalTextCol = (textGhosted) ? dop.getInactiveGray() : textCol;
    Point2D textEnd = renderText(group, (float)(x + textPad), (float)(y - textPad),
                                item.getName(), finalTextCol, np.getHideName(), amFont, np.getLineBreakDef(), rcx.getFrc(), rcx.fmgr, textFactory);
    
    Point2D pieCenter = new Point2D.Double(textEnd.getX() + 15.0, textEnd.getY());
    drawVariableActivityPie(group, item, col, pieCenter, dop);

    setGroupBounds(group, item, rcx);

    cache.addGroup(group);
  }
  
  /***************************************************************************
  **
  ** Get the shape to render one stroke
  */
  
  protected abstract ModelObjectCache.SegmentedPathShape getPath1Shape(float x, float y, Color col, BasicStroke pathStroke);
  
  /***************************************************************************
  **
  ** Get the shape to render the other stroke
  */
  
  protected abstract ModelObjectCache.SegmentedPathShape getPath2Shape(float x, float y, Color col, BasicStroke pathStroke);
  
  /***************************************************************************
   **
   ** Get the preferred launch direction from the given pad
   */
  
  @Override
  public Vector2D getDepartureDirection(int padNum, GenomeItem item, Layout layout) {
    NodeProperties np = layout.getNodeProperties(item.getID());
    int orient = np.getOrientation();
    AffineTransform rot = getRotationTransform(orient);
    Vector2D rightDepart = new Vector2D(1.0, 0.0);
    return (rightDepart.doTransform(rot));
  }
  
  /***************************************************************************
   **
   ** Get the preferred arrival direction
   */
  
  @Override
  public Vector2D getArrivalDirection(int padNum, GenomeItem item, Layout layout) {
    if (padNum >= 0) {
      return (getDepartureDirection(padNum, item, layout));
    }
    //
    // Extra pads:
    //
    Vector2D stdArrive = ((-padNum % 2) == 0) ? new Vector2D(0.0, -1.0) : new Vector2D(0.0, 1.0);
    NodeProperties np = layout.getNodeProperties(item.getID());
    int orient = np.getOrientation();
    AffineTransform rot = getRotationTransform(orient);
    return (stdArrive.doTransform(rot));
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
  ** Sets the bound shapes for the CacheGroup, to be exported in the
  ** web application.
  */
  
  protected void setGroupBounds(CommonCacheGroup group, GenomeItem item, DataAccessContext rcx) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, item, rcx);
    for (ModelObjectCache.ModalShape ms : bounds) {
      group.addBoundsShape(ms);
    }
  }
  
  /***************************************************************************
  **
  ** Fills an array with bound shapes that will be exported in the model map
  ** in the web application.
  */
  
  protected void fillBoundsArray(ArrayList<ModelObjectCache.ModalShape> targetArray, GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double cX = origin.getX();
    double cY = origin.getY();
    double textPad = getTextPad();
    
    ModelObjectCache.ModalShape bound = getFBAShape(cX, cY);
    targetArray.add(bound);
    
    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {
        fillBoundsArrayTextLabel(targetArray, rcx, (float)(cX + textPad), (float)(cY - textPad),
                name, np.getHideName(), FontManager.MEDIUM, np.getFontOverride(), np.getLineBreakDef(), rcx.fmgr);
    }    
  }
  
  /***************************************************************************
  **
  ** Get the shape to fill the bounds array
  */
  
  protected abstract ModelObjectCache.ModalShape getFBAShape(double cX, double cY);
    
  /***************************************************************************
   **
   ** Check for intersection:
   */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
    
    //
    // Simple distance check - > better to use rectangle.  FIX ME!!!
    //
    
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double x = origin.getX();
    double y = origin.getY();
    double dx = x - pt.getX();
    double dy = y - pt.getY();
    double distsq = (dx * dx) + (dy * dy);
    double textPad = getTextPad();
    
    String name = item.getName();
    if ((name != null) && (!name.trim().equals(""))) {
      if (isTextCandidate(rcx.getFrc(), FontManager.MEDIUM, np.getFontOverride(), name, textPad, dx, 
                          distsq, np.getLineBreakDef(), false, np.getHideName(), rcx.fmgr)) {
        if (intersectTextLabel(rcx.getFrc(), (float)(x + textPad), (float)(y - textPad),
                name, pt, np.getHideName(), FontManager.MEDIUM, np.getFontOverride(), np.getLineBreakDef(), rcx.fmgr)) {
          return (new Intersection(item.getID(), null, 0.0));
        }
      }
    }
    
    //
    // Relatively quick kill:
    //
    
    double intersect = getIntersect();
    double quickSq = intersect * intersect;
    Node theInter = (Node)item;
    int numPads = theInter.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theInter.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      double quickRad = 1.25 * (extraPadStartOffset() + ((extraPads + 1) / 2) * PAD_SPACING_);
      quickSq = quickRad * quickRad;
    }
    if (distsq > quickSq) {
      return (null);
    }
    
    //
    // This handles all the rotation junk:
    //
    
    Rectangle2D bounds = getBoundsWithExtraPads(item, rcx.getLayout());
    bounds = UiUtil.padTheRect(bounds, PAD_WIDTH_ / 2.0);
    if (Bounds.intersects(bounds.getX(), bounds.getY(),
                          bounds.getMaxX(), bounds.getMaxY(), pt.getX(), pt.getY())) {
      List<Intersection.PadVal> pads = calcPadIntersects(item, pt, rcx);
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
    double intersect = getIntersect();
    Rectangle2D myBounds = new Rectangle2D.Double(cX - intersect, cY - intersect,
                                                  2.0 * intersect, 2.0 * intersect);
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
    return (getLaunchPadOffsetGuts(orient));
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  private Vector2D getLaunchPadOffsetGuts(int orient) {
    AffineTransform rot = getRotationTransform(orient);
    Vector2D rightDepart = rightDepart();
    return (rightDepart.doTransform(rot));
  }
  
  /***************************************************************************
  **
  ** Get the right departure vector
  */
  
  protected abstract Vector2D rightDepart();
  
  /***************************************************************************
  **
  ** Get the left arrival vector
  */
  
  protected abstract Vector2D leftArrive();

  /***************************************************************************
  **
  ** How to draw the extra rectangle
  */
  
  protected abstract boolean extraRectPointed();
  
  /***************************************************************************
  **
  ** Offset to start extras
  */
  
  protected abstract double extraPadStartOffset();  
  
  /***************************************************************************
   **
   ** Get the landing pad offset
   */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign, 
                                      DataAccessContext icx) {
    Vector2D offVec;
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    int orient = np.getOrientation();
    AffineTransform rot = getRotationTransform(orient);
    Rectangle2D extra = extraPadRectangle(item, icx.getLayout());
    if (padNum >= 0) {
      if (extra == null) {
        return (leftArrive().doTransform(rot));
      } else {
        offVec = new Vector2D(-extra.getWidth(), 0.0);
      }
    } else {
      //
      // Extra pads:
      //
      float negPosTweak = 0.0F;
      if (sign == Linkage.NEGATIVE) {
        negPosTweak = NEG_OFFSET;
      } else if (sign == Linkage.POSITIVE) {
        negPosTweak = POS_OFFSET;
      }
      double baseOffset = (EXTRA_BOX_HEIGHT_  / 2.0) - 1.0;
      double finalOffset = baseOffset + negPosTweak;
      double yOff = ((-padNum % 2) == 0) ? finalOffset : -finalOffset;
      double xOff = -(extraPadStartOffset() + (((-padNum) - 1) / 2) * PAD_SPACING_);
      offVec = new Vector2D(xOff, yOff);
    }
    return (offVec.doTransform(rot));
  }
  
  /***************************************************************************
   **
   ** Get the landing pad width
   */
  
  public abstract double getLandingPadWidth(int padNum, GenomeItem item, Layout layout);
  
  
  /***************************************************************************
  **
  ** Get the intersect
  */
  
  protected abstract double getIntersect();
  
  /***************************************************************************
  **
  ** Get the text pad
  */
  
  protected abstract double getTextPad();  
  
  /***************************************************************************
   **
   ** Get the maximum number of launch pads
   */
  
  public int getFixedLaunchPadMax() {
    return (1);
  }
  
  /***************************************************************************
   **
   ** Get the maximum number of landing pads
   */
  
  public int getFixedLandingPadMax() {
    return (1);
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
   ** Answer if landing and launching pad namespaces are shared
   */
  
  public boolean sharedPadNamespaces(){
    return (false);
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
        return (new NodeInsertionDirective(0, 0, NodeProperties.UP));
      } else {
        return (new NodeInsertionDirective(0, 0, NodeProperties.DOWN));
      }
    } else if (travel.getY() == 0.0) {
      if (travel.getX() < 0.0) {
        return (new NodeInsertionDirective(0, 0, NodeProperties.LEFT));
      } else {
        return (new NodeInsertionDirective(0, 0, NodeProperties.RIGHT));
      }      
    } else if (Math.abs(travel.getX()) >= Math.abs(travel.getY())) {  // mostly horizontal
      if (travel.getX() > 0) {
        return (new NodeInsertionDirective(0, 0, NodeProperties.RIGHT));
      } else {
        return (new NodeInsertionDirective(0, 0, NodeProperties.LEFT));      
      }
    } else {  // mostly vertical
      if (travel.getY() > 0) {
        return (new NodeInsertionDirective(0, 0, NodeProperties.DOWN));
      } else {
        return (new NodeInsertionDirective(0, 0, NodeProperties.UP));      
      }     
    }
  }  
  
  /***************************************************************************
   **
   ** Return the Rectangle used by this node
   */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    
    Rectangle2D r2d = getBoundsWithExtraPads(item, rcx.getLayout());
    return (new Rectangle((int)r2d.getX(), (int)r2d.getY(), (int)r2d.getWidth(), (int)r2d.getHeight()));
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
     int totalPads = (topPadCount == null) ? theBox.getPadCount() : (topPadCount.intValue() * 2) + 1;
     Point2D origin = new Point2D.Double(0.0, 0.0);
     double textPad = getTextPad();
     
     Rectangle2D basic = getBoundsWithExtraPadsGuts(origin, totalPads, orientation, type);
     
     if (labelToo && !np.getHideName()) {
       AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.MEDIUM, np.getFontOverride());
       String breakDef = np.getLineBreakDef();
       Rectangle2D textRect = getTextBounds(rcx.getFrc(), (float)textPad, (float)textPad, 
                                            item.getName(), false, amFont, breakDef, rcx.fmgr);
       Rectangle fullRect = UiUtil.rectFromRect2D(textRect);
       Rectangle basicAsRect = UiUtil.rectFromRect2D(basic);
       return (fullRect.union(basicAsRect));
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
    return (getLaunchPadOffsetGuts(orientation));
  }
 
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public int topPadCount(int fullPadCount) {
    return ((fullPadCount - (fullPadCount % 2)) / 2);
  }
  
  /***************************************************************************
   **
   ** Return the height
   */
  
  public double getHeight(GenomeItem item, DataAccessContext rcx) {
    Rectangle2D extra = extraPadRectangle(item, rcx.getLayout());
    if (extra != null) {
      NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
      int orient = np.getOrientation();
      if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.LEFT)) {
        return (APPROX_DIAM_);
      } else {
        return ((APPROX_DIAM_ / 2.0) + extra.getWidth());
      }
    } else {
      return (APPROX_DIAM_);
    }
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
    Rectangle2D extra = extraPadRectangle(item, rcx.getLayout());
    if (extra != null) {
      NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
      int orient = np.getOrientation();
      if ((orient == NodeProperties.UP) || (orient == NodeProperties.DOWN)) {
        return (APPROX_DIAM_);
      } else {
        return ((APPROX_DIAM_ / 2.0) + extra.getWidth());
      }
    } else {
      return (APPROX_DIAM_);
    }
  }
  
  /***************************************************************************
  **
  ** Answer how much extra length will result from the given pad increase
  */
  
  public double getExtraLength(Node node, Genome genome, int newCount, boolean oneSideOnly) {
    int currPadCount = node.getPadCount();
    if (oneSideOnly) {
      currPadCount /= 2;
    }
    int padDiff = newCount - currPadCount;
    double extra = ((padDiff <= 0) ? 0.0 : PAD_SPACING_ * padDiff);
    
    //
    // Since we are adding extra width asymetrically, we need to report a double value:
    return (2.0 * extra); 
  } 
  
  /***************************************************************************
  **
  ** Hand out one bottom pad
  */  
  
  public int getBottomPad(GenomeItem item) {
    return (-2);
  }
   
  /***************************************************************************
  **
  ** Return a top pad, preferably unused.
  */

  public int getBestTopPad(GenomeItem item, Set<Integer> usedPads, int expandDir, 
                           int totalNeeded, boolean startFromLeft) {
    //
    // Top center is the first choice when building from center, else we build
    // from left:
    //
    
    if (startFromLeft) {
      int firstOnLeft = ((totalNeeded - 1) * -2) - 1;
      Integer firstChoice = new Integer(firstOnLeft);
      if (!usedPads.contains(firstChoice)) {
        usedPads.add(firstChoice);
        return (firstOnLeft);
      }            
    } else {  // build from center
      Integer firstChoice = new Integer(-1);
      if (!usedPads.contains(firstChoice)) {
        usedPads.add(firstChoice);
        return (-1);
      }
    }

    //
    // Start walking out from the center until we find a free pad:
    //    

    if (startFromLeft) {
      int firstOnLeft = ((totalNeeded - 1) * -2) - 1;
      int truePadNum = firstOnLeft;
      while (true) {
        Integer checkChoice = new Integer(truePadNum);
        if (!usedPads.contains(checkChoice)) {
          usedPads.add(checkChoice);
          return (truePadNum);
        }
        truePadNum += 2;
      }
    } else {
      int truePadNum = -1;
      while (true) {
        Integer checkChoice = new Integer(truePadNum);
        if (!usedPads.contains(checkChoice)) {
          usedPads.add(checkChoice);
          return (truePadNum);
        }
        truePadNum -= 2;
      }
    }
  }
  
  /***************************************************************************
  **
  ** Compare pads (negative if one is to the left of two).
  */  
  
  public int comparePads(int padOne, int padTwo) {
    if (padOne == padTwo) {
      return (0);
    }
    
    // Zero is always leftmost:
    
    if (padOne == 0) {
      return (-1);
    } else if (padTwo == 0) {
      return (1);
    }
    
    //
    // Extras just depend on the mod bin:
    //
    
    int xOne = ((-padOne) - 1) / 2;
    int xTwo = ((-padTwo) - 1) / 2; 
    
    // If x's are equal, we MUST return non-zero if pads are different.
    
    if (xOne == xTwo) {
      int yOne = ((-padOne % 2) == 0) ? 1 : -1;
      int yTwo = ((-padTwo % 2) == 0) ? 1 : -1;
      return (yOne - yTwo);
    }
    
    return (xTwo - xOne); // Note sign flip above, so we flip sign here...
  }
  
  /***************************************************************************
  **
  ** Get a list of pads that are nearby to the given one.  Only goes as far as the 
  ** total bank of pads on the side.  Gotta override the base class, since our
  ** numbering is non-standard:
  */

  public List<Integer> getNearbyPads(GenomeItem item, int startPad) {
     
    Node theNode = (Node)item;
    if (!theNode.haveExtraPads() || (startPad == 0)) {
      return (null);
    }
    
    MinMax range = getTargetPadRange(item);    
    int count = 0;
    HashMap<Integer, Integer> distances = new HashMap<Integer, Integer>();    
    
    int firstPad = ((-startPad % 2) == 0) ? range.min : (range.min + 1); 
    for (int i = firstPad; i < 0; i += 2) {
      Integer nextPad = new Integer(i);
      Integer nextDist = new Integer(count++);
      distances.put(nextPad, nextDist);
    }  
    
    return (alternationSupport(distances, startPad));    
  }    
 
  /***************************************************************************
   **
   ** Render the launch and landing pads
   */
  
  protected void renderPads(ModalShapeContainer group, Color col, GenomeItem item, DataAccessContext rcx) {
    
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    // g2.setPaint(col);
    // g2.setStroke(new BasicStroke(1));
    BasicStroke stroke = new BasicStroke(1);
    
    //
    // One launch pad:
    //
    
    Vector2D lpo = getLaunchPadOffset(0, item, rcx);
    double x = origin.getX() + lpo.getX();
    double y = origin.getY() + lpo.getY();
    double padRadius = (PAD_WIDTH_ / 2.0) - 1.0;
    // Ellipse2D circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
    //                                       2.0 * padRadius, 2.0 * padRadius);
    // g2.draw(circ);
    
    ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, stroke);
    group.addShape(circ, majorLayer, minorLayer);
    
    //
    // Always one landing pad:
    //
    
    lpo = getLandingPadOffset(0, item, Linkage.POSITIVE, rcx);
    x = origin.getX() + lpo.getX();
    y = origin.getY() + lpo.getY();
    // circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
    //                             2.0 * padRadius, 2.0 * padRadius);
    // g2.draw(circ);
    circ = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, stroke);
    group.addShape(circ, majorLayer, minorLayer);
    
    //
    // Extra pads:
    //
    
    Node theInter = (Node)item;
    int numPads = theInter.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theInter.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      for (int i = 1; i <= extraPads; i++) {
        int negPadNum = -i;
        lpo = getLandingPadOffset(negPadNum, item, Linkage.POSITIVE, rcx);
        x = origin.getX() + lpo.getX();
        y = origin.getY() + lpo.getY();
        // circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
        //                             2.0 * padRadius, 2.0 * padRadius);
        // g2.draw(circ);
        circ = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, stroke);
        group.addShape(circ, majorLayer, minorLayer);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Figure out which pad we intersect
  */
  
  public List<Intersection.PadVal> calcPadIntersects(GenomeItem item, Point2D pt, DataAccessContext rcx) {
    
    ArrayList<Intersection.PadVal> retval = new ArrayList<Intersection.PadVal>();
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
 
    double px = pt.getX();
    double py = pt.getY();
    //
    // Note that previous intersection test makes sure we have intersected the
    // node.  So relatively sloppy pad intersection is OK; we will later base
    // the best fit on the distance:
    //
    // Note that node origin is centered on landing pad, so split the difference!
    Vector2D lpo = getLaunchPadOffset(0, item, rcx);
    Point2D splitPt = lpo.scaled(0.5).add(origin);
    Vector2D clickVec = new Vector2D(splitPt, pt);
    
    double dot = lpo.dot(clickVec);
    if (dot >= 0.0) {    
      double x = origin.getX() + lpo.getX();
      double y = origin.getY() + lpo.getY(); 
      double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
      Intersection.PadVal retpad = new Intersection.PadVal();
      retpad.okEnd = false;
      retpad.okStart = true;
      retpad.padNum = 0;
      retpad.distance = Math.sqrt(distSq);
      retval.add(retpad);
      return (retval);  // Launch pad means we are done!
    }

    //
    // We are on the landing pad side of the node.  So fill in all the
    // options!
    //
    lpo = getLandingPadOffset(0, item, Linkage.POSITIVE, rcx);
    double x = origin.getX() + lpo.getX();
    double y = origin.getY() + lpo.getY();
    double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
    Intersection.PadVal retpad = new Intersection.PadVal();
    retpad.okEnd = true;
    retpad.okStart = false;
    retpad.padNum = 0;
    retpad.distance = Math.sqrt(distSq);
    retval.add(retpad);
      
    //
    // Extra pads:
    //
    Node theInter = (Node)item;
    int numPads = theInter.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theInter.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      for (int i = 1; i <= extraPads; i++) {
        int negPadNum = -i;
        lpo = getLandingPadOffset(negPadNum, item, Linkage.POSITIVE, rcx);
        x = origin.getX() + lpo.getX();
        y = origin.getY() + lpo.getY();      
        distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
        retpad = new Intersection.PadVal();
        retpad.okEnd = true;
        retpad.okStart = false;
        retpad.padNum = negPadNum;
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
    Vector2D lpo = getLaunchPadOffset(0, item, layout, frc);
    double x = origin.getX() + lpo.getX();
    double y = origin.getY() + lpo.getY();
    double padRadius = (PAD_WIDTH_ / 2.0) + 1.0;
    double prSq = padRadius * padRadius;
    double px = pt.getX();
    double py = pt.getY();
    double distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
    if (distSq <= prSq) {
      Intersection.PadVal retpad = new Intersection.PadVal();
      retpad.okEnd = false;
      retpad.okStart = true;
      retpad.padNum = 0;
      retpad.distance = Math.sqrt(distSq);
      retval.add(retpad);
    }

    lpo = getLandingPadOffset(0, item, Linkage.POSITIVE, layout, frc);
    x = origin.getX() + lpo.getX();
    y = origin.getY() + lpo.getY();
    distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
    if (distSq <= prSq) {
      Intersection.PadVal retpad = new Intersection.PadVal();
      retpad.okEnd = true;
      retpad.okStart = false;
      retpad.padNum = 0;
      retpad.distance = Math.sqrt(distSq);
      retval.add(retpad);
    }
      
    //
    // Extra pads:
    //
    Node theInter = (Node)item;
    int numPads = theInter.getPadCount();
    int defaultPads = DBNode.getDefaultPadCount(theInter.getNodeType());
    if (numPads > defaultPads) {
      int extraPads = numPads - defaultPads;
      for (int i = 1; i <= extraPads; i++) {
        int negPadNum = -i;
        lpo = getLandingPadOffset(negPadNum, item, Linkage.POSITIVE, layout, frc);
        x = origin.getX() + lpo.getX();
        y = origin.getY() + lpo.getY();      
        distSq = ((px - x) * (px - x)) + ((py - y) * (py - y));
        if (distSq <= prSq) {
          Intersection.PadVal retpad = new Intersection.PadVal();
          retpad.okEnd = true;
          retpad.okStart = false;
          retpad.padNum = negPadNum;
          retpad.distance = Math.sqrt(distSq);
          retval.add(retpad);
        }
      }
    }
    
    return ((retval.isEmpty()) ? null : retval);
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
    return (extraPadRectangleForPads(totalPads, origin, theBubble.getNodeType())); 
  }
  
  /***************************************************************************
  **
  ** If this node has extra pads, we return a rectangle that provides the
  ** extra sizing
  */
  
  private Rectangle2D extraPadRectangleForPads(int numPads, Point2D origin, int nodeType) {
    int defaultPads = DBNode.getDefaultPadCount(nodeType);
    if (numPads <= defaultPads) {
      return (null);
    }   
    int extraPerSide = (numPads - 1) / 2;
    double extraSize = extraPadStartOffset() + ((extraPerSide - 1) * PAD_SPACING_);
    double height = EXTRA_BOX_HEIGHT_;
    double minX = origin.getX() - extraSize;
    double minY = origin.getY() - height / 2.0;
    return (new Rectangle2D.Double(minX, minY, extraSize, height));
  }
  
  /***************************************************************************
   **
   ** Get the rotation transform we need
   */
  
  protected ModelObjectCache.PushRotationWithOrigin getRotationTransform(int orient, Point2D origin) {
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    // AffineTransform trans = new AffineTransform();
    double degrees = 0.0;
    if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.NONE)) {
      degrees = 0.0;
    } else if (orient == NodeProperties.UP) {
      degrees = -90.0;
    } else if (orient == NodeProperties.LEFT) {
      degrees = 180.0;
    } else if (orient == NodeProperties.DOWN) {
      degrees = 90.0;
    }
    ModelObjectCache.PushRotationWithOrigin trans = new ModelObjectCache.PushRotationWithOrigin(degrees, x, y);
    return (trans);
  }
  
  protected AffineTransform getRotationTransformOld(int orient, Point2D origin) {
    float x = (float)origin.getX();
    float y = (float)origin.getY();
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
    return (trans);
  }
  
  /***************************************************************************
   **
   ** Get the rotation transform we need
   */
  
  protected AffineTransform getRotationTransform(int orient) {
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
    trans.rotate(radians);
    return (trans);
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  private Rectangle2D getBoundsWithExtraPads(GenomeItem item, Layout layout) {
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int orient = np.getOrientation();
    Node theInter = (Node)item;
    int totalPads = theInter.getPadCount();
    return (getBoundsWithExtraPadsGuts(origin, totalPads, orient, theInter.getNodeType()));
  }
   
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  private Rectangle2D getBoundsWithExtraPadsGuts(Point2D origin, int totalPads, int orient, int nodeType) {
    double x = origin.getX();
    double y = origin.getY();
    double half = APPROX_DIAM_ / 2.0;
    Rectangle2D extra = extraPadRectangleForPads(totalPads, origin, nodeType);
    if (extra != null) {
      if ((orient == NodeProperties.RIGHT) || (orient == NodeProperties.NONE)) {
        return (new Rectangle2D.Double(x - extra.getWidth(), y - half, extra.getWidth() + half, APPROX_DIAM_));
      } else if (orient == NodeProperties.UP) {
        return (new Rectangle2D.Double(x - half, y - half, APPROX_DIAM_, extra.getWidth() + half));
      } else if (orient == NodeProperties.LEFT) {
        return (new Rectangle2D.Double(x - half, y - half, extra.getWidth() + half, APPROX_DIAM_));
      } else if (orient == NodeProperties.DOWN) {
        return (new Rectangle2D.Double(x - half, y - extra.getWidth(), APPROX_DIAM_, extra.getWidth() + half));
      } else {
        throw new IllegalStateException();
      }
    } else {
      return (new Rectangle2D.Double(x - half, y - half, APPROX_DIAM_, APPROX_DIAM_));
    }
  }

   /***************************************************************************
   **
   ** Paint support for extra node structure
   */
  
  protected void paintExtra(CommonCacheGroup group, Color fillColor, Rectangle2D extraRect, double diameter, boolean isSelected) {
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
  	GeneralPath path = new GeneralPath();
    float radius = (float)diameter / 2.0F;
    
    BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    path.moveTo((float)extraRect.getX(), (float)extraRect.getCenterY() - radius);
    if (!extraRectPointed()) {    
      path.lineTo((float)extraRect.getMaxX() - STROKE_ + 1, (float)extraRect.getCenterY() - radius);
      path.lineTo((float)extraRect.getMaxX() - STROKE_ - 1, (float)extraRect.getCenterY() + radius);
    } else {
      path.lineTo((float)extraRect.getMaxX() - STROKE_, (float)extraRect.getCenterY() - radius);
      path.lineTo((float)extraRect.getMaxX(), (float)extraRect.getCenterY());
      path.lineTo((float)extraRect.getMaxX() - STROKE_, (float)extraRect.getCenterY() + radius);
    }
    path.lineTo((float)extraRect.getX(), (float)extraRect.getCenterY() + radius);
    path.closePath();
    
    ModelObjectCache.SegmentedPathShape pathShape = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, fillColor, stroke, path);

    ModelObjectCache.Arc arc = new ModelObjectCache.Arc((float)extraRect.getX(),
                                  (float)extraRect.getCenterY(), radius, 90, 180, Type.CHORD,
                                  DrawMode.FILL, fillColor, stroke);  

    if (!isSelected) {
      group.addShape(pathShape, majorLayer, minorLayer);
      group.addShape(arc, majorLayer, minorLayer);
    }
    else {
      group.addShapeSelected(pathShape, majorLayer, minorLayer);
      group.addShapeSelected(arc, majorLayer, minorLayer);
    }
  }
}
