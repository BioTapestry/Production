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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a net module
*/

public class NetModuleFree {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final double PAD_SIZE = UiUtil.GRID_SIZE;  
  public static final int PAD_SIZE_INT = UiUtil.GRID_SIZE_INT;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double INTERSECT_TOL = 5.0;
  

  private static final double PAD_RADIUS = PAD_SIZE / 2.0;
  private static final int    PAD_PAD = UiUtil.GRID_SIZE_INT * 2;
  private static final double BIG_RADIUS = PAD_RADIUS * 4.0;  
  
  private static final float  DOT_           = 2.0F;
  private static final float  DOT_SP_        = 6.0F;
  private static final float  DASH_          = 8.0F;
  private static final float  DASH_SP_       = 4.0F;
  private static final float  LONG_DASH_      = 12.0F;
  private static final float  LONG_DASH_SP_   = 4.0F;  
  
  private final float[] dotPatternArray = new float[] {DOT_, DOT_SP_};
  private final float[] dashPatternArray = new float[] {DASH_, DASH_SP_};
  private final float[] longDashPatternArray = new float[] {LONG_DASH_, LONG_DASH_SP_};  
  
  //
  // Pad force levels:
  //
  
  private static final int NORMAL_PADS_         = 0;
  private static final int ALL_BUT_CORNER_PADS_ = 1;
  private static final int ALL_PADS_            = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  //
  // Note that pad positions get frozen and preserved by the link ends.
  // At very wide zooms, we have seen tiny discrepancies using the current
  // zoomed-out FRC between what we say the pad should be and what it
  // actually is.  So, for pad calculations, use a consistent identity
  // FRC to generate these things.  NOTE! that platform-dependent font
  // rendering differences are probably going to hose use here!
  
  private FontRenderContext fixedFrc_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public NetModuleFree() { 
    super();
    fixedFrc_ = new FontRenderContext(null, true, true);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the module to a placement grid
  */
  
  public void renderToPlacementGrid(NetModule module, String ovrID,  LinkPlacementGrid grid, DataAccessContext rcx) {
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    Iterator<Rectangle2D> sit = nmp.getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D rect = sit.next();
      grid.addGroup(UiUtil.rectFromRect2D(rect), module.getID());
    }   
    return;
  }
 
  /***************************************************************************
  **
  ** Render the module.  For genome instances, provide the root instance to
  ** ensure we can determine non-member regions in a stable fashion.
  */
  
  public void render(ModalShapeContainer group, DynamicInstanceProxy dip, Set<String> useGroups, String ovrID, 
                     NetModule module, CurrentSettings settings, boolean showComponents, boolean showContents,
                     boolean drawEdges, boolean drawFills, boolean drawLabel, StaticDataAccessContext rcx) {
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.isForWeb()) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer minorLayer = 0;
  	Integer majorLayer = 0;
  
    //
    // Figure out the rectangle containing the members:
    //
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());

    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);
    
    //
    // OK, here is where we start to do pixel diameter based rendering optimizations.
    // If there are lots of pads per pixel, don't bother to render them even if asked to do so:
    //
    
    boolean tinyPads = !(rcx.getPixDiam() < (4.0 * PAD_RADIUS));
    
    ArrayList<LinkPad> padList = (rcx.getShowBubbles() && !tinyPads) ? new ArrayList<LinkPad>() : null;
    NetOverlayProperties.OvrType nopType = nop.getType();
    boolean isOpq = (nopType == NetOverlayProperties.OvrType.OPAQUE);
    
    float fillAlpha = (showContents && isOpq) ? 0.0F : (float) settings.regionFillAlpha;   
    
    List<TaggedShape> shapeList = getShape(dip, rcx, useGroups, module, nameBounds, 
                                           outerRect, nmp, showComponents, padList, null);

    //
    // For opaque types, we use Src compositing, which overlays the destination
    // regardless of src alpha.  EXCEPT when src alpha == 0!  This happens; if it
    // does, we do a two-pass clear step instead!
    //
    
    boolean twoPass = false;
    if (isOpq) {
      if (fillAlpha != 0.0) {
        // g2.setComposite(AlphaComposite.Src);
        ModelObjectCache.SetComposite sc = new ModelObjectCache.SetComposite(AlphaComposite.Src);
        group.addShape(sc, majorLayer, minorLayer);        
      } else {
        // g2.setComposite(AlphaComposite.Clear);
        ModelObjectCache.SetComposite sc = new ModelObjectCache.SetComposite(AlphaComposite.Clear);
        group.addShape(sc, majorLayer, minorLayer);        
        twoPass = true;
      }
    }
    
    float[] coVals = new float[4];
    int numShape = shapeList.size();
    if (twoPass && !rcx.isForWeb()) {
      for (int i = 0; i < numShape; i++) {
        TaggedShape nextShape = shapeList.get(i);
        if ((nextShape.shapeClass == TaggedShape.COMPONENT_SHAPE) || 
            (nextShape.shapeClass == TaggedShape.NON_MEMBER_SHAPE)) {
          continue;
        }
        Shape boundArea = nextShape.shape;
        
        // TODO check stroke
        // TODO check that this is equal to Graphics2D.fill(boundArea);
        ModelObjectCache.SegmentedPathShape path = new ModelObjectCache.SegmentedPathShape(
        		DrawMode.FILL, Color.black, new BasicStroke(0), new GeneralPath(boundArea));
        
        group.addShape(path, majorLayer, minorLayer);        
      } 
      
      ModelObjectCache.SetComposite sc = new ModelObjectCache.SetComposite(AlphaComposite.Src);
      group.addShape(sc, majorLayer, minorLayer);
    }
    
    //
    // Do all fills first.  This keeps edges from being obscured
    //
    
    if (drawFills && ((nopType == NetOverlayProperties.OvrType.OPAQUE) || (nopType == NetOverlayProperties.OvrType.UNDERLAY))) {
      for (int i = 0; i < numShape; i++) {
        TaggedShape nextShape = shapeList.get(i);
        if ((nextShape.shapeClass == TaggedShape.COMPONENT_SHAPE) || 
            (nextShape.shapeClass == TaggedShape.NON_MEMBER_SHAPE)) {
          continue;
        }
        Shape boundArea = nextShape.shape;
        if ((nopType == NetOverlayProperties.OvrType.OPAQUE) || (nopType == NetOverlayProperties.OvrType.UNDERLAY)) {
          Color fillCol = nmp.getFillColor(rcx.getColorResolver());
          fillCol.getComponents(coVals);
          Color aDrawCol = new Color(coVals[0], coVals[1], coVals[2], fillAlpha);
          
          // TODO check stroke
          // TODO check that this is equal to Graphics2D.fill(boundArea);
          ModelObjectCache.SegmentedPathShape path = new ModelObjectCache.SegmentedPathShape(
          		DrawMode.FILL, aDrawCol, new BasicStroke(0), new GeneralPath(boundArea));
          
          group.addShape(path, majorLayer, minorLayer);
        }
      }
    }
 
    if (drawEdges) {
	    for (int i = 0; i < numShape; i++) {
	      TaggedShape nextShape = shapeList.get(i);
	      Shape boundArea = nextShape.shape;
	      BasicStroke useStroke = calcStroke(nmp.getType(), nextShape.shapeClass, showComponents);
	      if (useStroke == null) {
	        continue;
	      }
	      Color borderCol = nmp.getColor(rcx.getColorResolver());
	      
	      borderCol.getComponents(coVals);            
	      Color bDrawCol = new Color(coVals[0], coVals[1], coVals[2], (float)settings.regionBoundaryAlpha);
	      
	      ModelObjectCache.SegmentedPathShape path = new ModelObjectCache.SegmentedPathShape(
	      		DrawMode.DRAW, bDrawCol, useStroke, new GeneralPath(boundArea));
	      
	      group.addShape(path, majorLayer, minorLayer);
	    }
    }
    
    ModelObjectCache.SetComposite sc = new ModelObjectCache.SetComposite(AlphaComposite.SrcOver);
    group.addShape(sc, majorLayer, minorLayer);
    
    //
    // Names are set to invisible if contents are being revealed _in overlay mode_ and with quick fade only!
    //
    boolean quickFade = (nmp.getNameFadeMode() == NetModuleProperties.FADE_QUICKLY);
    boolean nameHiding = (showContents && isOpq && quickFade);
    
    if ((nameBounds != null) && !nameHiding && drawLabel) {
      Color textCol = Color.black;
      textCol.getComponents(coVals);
      float labelAlpha = (quickFade) ? (float)settings.regionLabelAlpha : (float)settings.regionBoundaryAlpha;
      Color tDrawCol = new Color(coVals[0], coVals[1], coVals[2], labelAlpha);
      String name = module.getName();
      AnnotatedFont bFont = rcx.getFontManager().getOverrideFont(FontManager.NET_MODULE, nmp.getFontOverride());
      String breakDef = nmp.getLineBreakDef();
      if (breakDef == null) {
        // g2.setPaint(tDrawCol);       
        
        // g2.setFont(bFont);     
        Rectangle2D textBounds = bFont.getFont().getStringBounds(name, rcx.getFrc());
        double textX = nameBounds.getCenterX() - textBounds.getCenterX();
        double textY = nameBounds.getCenterY() - (textBounds.getCenterY() * 0.8);
        // g2.drawString(name, (float)textX, (float)textY);
        
        // TODO ConcreteGraphicsCache will translate the color to g2.setColor, not g2.setPaint
        ModalShape ts = textFactory.buildTextShape(name, bFont, tDrawCol, (float)textX, (float)textY, new AffineTransform());
        group.addShape(ts, majorLayer, minorLayer);
      } else {
        Point2D nameLoc = nmp.getNameLoc();
        List<String> frags = MultiLineRenderSupport.applyLineBreaksForFragments(name, breakDef);
        String[] toks = new String[frags.size()];
        frags.toArray(toks);
        MultiLineRenderSupport.multiLineRender(group, rcx.getFrc(), toks, bFont, nameLoc, false, MultiLineRenderSupport.CENTER_JUST, tDrawCol, textFactory);
      }
    }
    
    if (padList != null) {
      ArrayList<ModelObjectCache.Ellipse> bubbleList = new ArrayList<ModelObjectCache.Ellipse>();
      Color padCol = Color.black;
      padCol.getComponents(coVals);            
      Color bDrawCol = new Color(coVals[0], coVals[1], coVals[2], (float)settings.regionBoundaryAlpha);
      BasicStroke bubbleStroke = new BasicStroke(1);
      bubbleShapesForPads(padList, bubbleList, bDrawCol, bubbleStroke);

      int numBub = bubbleList.size();
      for (int i = 0; i < numBub; i++) {
      	ModelObjectCache.Ellipse nextPad = bubbleList.get(i);
      	group.addShape(nextPad, majorLayer, minorLayer);
      }
    }
    
    return;    
  }
  
  /***************************************************************************
   **
   ** Sets the bound shapes for the CacheGroup, to be exported in the
   ** web application.
  */
  public void setGroupBounds(NetModuleCacheGroup group, DynamicInstanceProxy dip, 
                          Set<String> useGroups, NetModule module,
                          String ovrID, StaticDataAccessContext irx) {
	
    NetOverlayProperties nop = irx.getCurrentLayout().getNetOverlayProperties(ovrID);
    String moduleID = module.getID(); 
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);      
    Rectangle2D nb = getNameBounds(nmp, module, irx);
  	ModelObjectCache.Rectangle nameBoundsRect = new ModelObjectCache.Rectangle(nb.getMinX(), nb.getMinY(), nb.getWidth(), nb.getHeight(), DrawMode.DRAW, Color.BLACK, new BasicStroke());
  	group.setNameBoundShape(nameBoundsRect);
  	
  	Rectangle outerRect = new Rectangle();
  	Rectangle2D superRect = new Rectangle();
  	List<TaggedShape> shapelist = getShape(dip, irx, useGroups, module, nb, outerRect, nmp, true, null, superRect);
  	
  	ModelObjectCache.Rectangle rectangle = null;
  	
  	// For MULTI_RECT type modules, the boundary containing each part is set in the outerRect parameter.  	
  	int moduleType = nmp.getType();
  	if (moduleType == NetModuleProperties.MULTI_RECT) {
  		rectangle = new ModelObjectCache.Rectangle(outerRect.getMinX(), outerRect.getMinY(), outerRect.getWidth(), outerRect.getHeight(), DrawMode.DRAW, Color.BLACK, new BasicStroke());
  	}
  	else {
		rectangle = new ModelObjectCache.Rectangle(superRect.getMinX(), superRect.getMinY(), superRect.getWidth(), superRect.getHeight(), DrawMode.DRAW, Color.BLACK, new BasicStroke());;
  	}
  	
  	group.setOuterBoundShape(rectangle);  	
  	group.setShape(shapelist);
  	
  	for (Iterator<NetModuleMember> memit = module.getMemberIterator(); memit.hasNext(); ) {
  		String memberID = memit.next().getID();
  		group.addMember(memberID);
  	}
  }  
  
  /***************************************************************************
  **
  ** Get the outer bounds of the module
  */
  
  public Rectangle bounds(DynamicInstanceProxy dip, 
                          Set<String> useGroups, NetModule module,
                          String ovrID, StaticDataAccessContext irx) {
  
    //
    // Figure out the rectangle containing the members:
    //  
    
    NetOverlayProperties nop = irx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, irx);    
    getShape(dip, irx, useGroups, module, nameBounds, outerRect, nmp, false, null, null);
    return (outerRect);
  }  
   
  /***************************************************************************
  **
  ** Answer if we intersect (are inside) the module
  */
  
  public boolean intersects(DynamicInstanceProxy dip, Set<String> useGroups, NetModule module,
                            String ovrID, Point2D pt, StaticDataAccessContext irx) {
  
    //
    // Figure out the rectangle containing the members:
    //  
    
    NetOverlayProperties nop = irx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, irx);    
    List<TaggedShape> shapeList = getShape(dip, irx, useGroups, module, 
                                           nameBounds, outerRect, nmp, false, null, null);
    if (!outerRect.contains(pt)) {
      return (false);
    }
    
    int numShape = shapeList.size();
    for (int i = 0; i < numShape; i++) {
      Shape boundArea = shapeList.get(i).shape;
      if (boundArea.contains(pt)) {
        return (true);
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answers if the module name is contained in the given contiguous region
  ** specified in a region intersection
  */
  
  public boolean nameInRegion(Intersection intersect, NetModule module, String ovrID, int mode, StaticDataAccessContext icx) {
  
    NetOverlayProperties nop = icx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    IntersectionExtraInfo ei = (IntersectionExtraInfo)intersect.getSubID();
    
    Set<Rectangle2D> shapes;
    if (mode == NetModuleProperties.ONE_MODULE_SHAPE) {
      shapes = nmp.rectsForDirect(ei);      
    } else if (mode == NetModuleProperties.CONTIG_MODULE_SHAPES) {
      shapes = ei.contigInfo.intersectedShape;
    } else {
      throw new IllegalArgumentException();
    }
    
    if ((shapes == null) || shapes.isEmpty()) {
      return (false);
    } 
    
    Rectangle2D nameBounds = getNameBounds(nmp, module, icx);
    if (nameBounds == null) {
       return (false);
    }  
        
    Iterator<Rectangle2D> sit = shapes.iterator();
    while (sit.hasNext()) {
      Rectangle2D chkShape = sit.next();
      Rectangle2D interRect = chkShape.createIntersection(nameBounds);
      //
      // Non-intersection is indicated by negative height and/or width:
      // Edge sharing by zero height OR width
      // Corner sharing by zero height AND width
      //
      if ((interRect.getHeight() >= 0.0) && (interRect.getWidth() >= 0.0)) {
        return (true);
      }   
    }  
  
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers which module members are contained in the given contiguous region
  ** specified in a region intersection
  */
  
  public Set<String> membersInRegion(Intersection intersect, NetModule module, Layout layout, String ovrID, int mode) {
  
    HashSet<String> retval = new HashSet<String>();   
    NetOverlayProperties nop = layout.getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    IntersectionExtraInfo ei = (IntersectionExtraInfo)intersect.getSubID();
    
    Set<Rectangle2D> shapes;
    if (mode == NetModuleProperties.ONE_MODULE_SHAPE) {
      shapes = nmp.rectsForDirect(ei);
      
    } else if (mode == NetModuleProperties.CONTIG_MODULE_SHAPES) {
      shapes = ei.contigInfo.intersectedShape;
      retval.addAll(ei.contigInfo.memberShapes.keySet());
    } else {
      throw new IllegalArgumentException();
    }
    
    if ((shapes == null) || shapes.isEmpty()) {
      return (retval);
    }
       
    //
    // Figure out the members in the shapes:
    //  
 
    Iterator<NetModuleMember> memit = module.getMemberIterator();
    while (memit.hasNext()) {
      NetModuleMember nmm = memit.next();
      String nodeID = nmm.getID();
      if (retval.contains(nodeID)) {
        continue;
      }
      NodeProperties np = layout.getNodeProperties(nodeID);
      Point2D pt = np.getLocation();
      Iterator<Rectangle2D> sit = shapes.iterator();
      while (sit.hasNext()) {
        Rectangle2D chkShape = sit.next();
        if (chkShape.contains(pt)) {
          retval.add(nodeID);
        }      
      }
    }  
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Gets back a list (with less than or equal to one member) of the shape specified by the given
  ** intersection with the given mode.  Outer rect is also filled in.
  */
  
  public List<Shape> shapesFromIntersection(Intersection intersect, String moduleID, Layout layout,
                                            String ovrID, int mode, Rectangle outerBounds) {
 
    NetOverlayProperties nop = layout.getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
    IntersectionExtraInfo ei = (IntersectionExtraInfo)intersect.getSubID();
    
    Set<Rectangle2D> shapes;
    if (mode == NetModuleProperties.ONE_MODULE_SHAPE) {
      shapes = nmp.rectsForDirect(ei);    
    } else if (mode == NetModuleProperties.CONTIG_MODULE_SHAPES) {
      shapes = ei.contigInfo.intersectedShape;
    } else {
      throw new IllegalArgumentException();
    }
    
    if (shapes == null) {
      outerBounds.setBounds(0, 0, 0, 0);
      return (new ArrayList<Shape>());
    }
      
    Area oneArea = null;       
    Rectangle extent = null;
    Iterator<Rectangle2D> sit = shapes.iterator();
    while (sit.hasNext()) {
      Rectangle2D nextRect = sit.next();
      Area rectArea = new Area(nextRect);
      if (oneArea == null) {
        oneArea = rectArea;
      } else {
        oneArea.add(rectArea);
      }     
      Rectangle eaRect = UiUtil.rectFromRect2D(nextRect);
      if (extent == null) {
        extent = eaRect;
      } else {
        Bounds.tweakBounds(extent, eaRect);
      }
    }
    
    if (extent == null) {
      outerBounds.setBounds(0, 0, 0, 0);
    } else {
      outerBounds.setBounds(extent);
    }
        
    ArrayList<Shape> retval = new ArrayList<Shape>();
    if (oneArea != null) {
      retval.add(oneArea);
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Get the shapes of the module.  Also fills in the outer rectangle.
  */
  
  public List<Shape> getModuleShapes(Genome ownerGenome,
                                     Set<String> useGroups, String ovrID, 
                                     String moduleID,
                                     Rectangle2D outerRectRet, StaticDataAccessContext rcx) {
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(ownerGenome.getID());
    DynamicInstanceProxy dip = null;
    if (ownerGenome instanceof DynamicGenomeInstance) {
      dip = rcx.getGenomeSource().getDynamicProxy(((DynamicGenomeInstance)ownerGenome).getProxyID());
    }
    NetworkOverlay novr = owner.getNetworkOverlay(ovrID);
    NetModule mod = novr.getModule(moduleID);
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, mod, rcx);
       
    List<TaggedShape> shapeList = getShape(dip, rcx, useGroups, mod, 
                                           nameBounds, outerRect, nmp, false, null, null);
    ArrayList<Shape> retval = new ArrayList<Shape>();
    int numShapes = shapeList.size();
    for (int i = 0; i < numShapes; i++) {
      retval.add(shapeList.get(i).shape);
    }
    outerRectRet.setRect(outerRect.getX(), outerRect.getY(), outerRect.getWidth(), outerRect.getHeight());
    return (retval);
  }  

  /***************************************************************************
  **
  ** Answer if we intersect the name
  */
  
  public Intersection intersectsName(NetModule module, String ovrID, 
                                     StaticDataAccessContext icx, Point2D pt) {
  
    //
    // Figure out name bound and see if we intersect it:
    //  
    
    NetOverlayProperties nop = icx.getCurrentLayout().getNetOverlayProperties(ovrID);
    String moduleID = module.getID(); 
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);      
    Rectangle2D nameBounds = getNameBounds(nmp, module, icx);
    if ((nameBounds != null) && nameBounds.contains(pt)) {
      return (new Intersection(moduleID, null, 0.0));
    }
    return (null);
  }

 /***************************************************************************
  **
  ** Answer if we intersect a pad
  */
  
  public Intersection intersectsLinkPad(DynamicInstanceProxy dip, 
                                        Set<String> useGroups, NetModule module,
                                        String ovrID, 
                                        StaticDataAccessContext rcx, 
                                        Point2D pt) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    
    String moduleID = module.getID();
    Rectangle outerRect = new Rectangle();
    rcx.pushFrc(fixedFrc_);
    
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    ArrayList<LinkPad> padList = new ArrayList<LinkPad>();
    getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, false, padList, null);
    rcx.popFrc();
     
    //
    // Use this as a testbed for handling large pixel diameters:
    //
    
    int usePad = ((2 * (int)rcx.getPixDiam()) > PAD_PAD) ? 2 * (int)rcx.getPixDiam() : PAD_PAD;
    
    Rectangle padded = new Rectangle(outerRect.x - usePad, outerRect.y - usePad, 
                                     outerRect.width + (2 * usePad), outerRect.height + (2 * usePad));
    if (!padded.contains(pt)) {
      return (null);
    } 
    
    LinkPad interPad = intersectsPad(padList, pt, rcx.getPixDiam());    
    if (interPad != null) {
      IntersectionExtraInfo ei = new IntersectionExtraInfo(interPad.point, interPad.getNormal());    
      Intersection intersect = new Intersection(moduleID, ei, 0.0);
      return (intersect);
    }
    return (null);   
  }
  

  /***************************************************************************
  **
  ** Answer if we intersect the boundary
  */
  
  public Intersection intersectsBoundary(DynamicInstanceProxy dip, Set<String> useGroups, NetModule module,
                                         String ovrID, 
                                         StaticDataAccessContext rcx, Point2D pt) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
    
    String moduleID = module.getID();
    double[] coords = new double[6];
    Point2D movePt = new Point2D.Double(0.0, 0.0);
    Point2D startPt = new Point2D.Double(0.0, 0.0);
    Point2D endPt = new Point2D.Double(0.0, 0.0);
    LinkSegment seg = new LinkSegment(startPt, endPt);
    Intersection result = null;
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    ArrayList<Point2D> intersectedShape = new ArrayList<Point2D>();
    List<TaggedShape> shapeList = getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, true, null, null);
    
    int numShape = shapeList.size();
    for (int i = 0; i < numShape; i++) {
      TaggedShape ts = shapeList.get(i);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        if ((ts.shapeClass != TaggedShape.AUTO_MEMBER_SHAPE) && 
            (ts.shapeClass != TaggedShape.TITLE_SHAPE)) {
          continue;
        }
      } else {           
        if (ts.shapeClass != TaggedShape.FULL_DRAW_SHAPE) {
          continue;
        }
      }
      Shape boundArea = ts.shape;
      PathIterator pit = boundArea.getPathIterator(null);
      boolean haveIntersect = false;
      while (!pit.isDone()) {
        int segType = pit.currentSegment(coords);
        switch (segType) {
          case PathIterator.SEG_CLOSE:
            startPt.setLocation(coords[0], coords[1]);
            intersectedShape.add((Point2D)startPt.clone());
            if (!haveIntersect) {
              seg.setBoth(startPt, movePt);
              result = checkSeg(seg, pt, moduleID, startPt, movePt, rcx.getPixDiam());
              if (result != null) {
                IntersectionExtraInfo ei = (IntersectionExtraInfo)result.getSubID();
                fillBoundaryLocation(outerRect, nmp, ei);
                haveIntersect = true;
              }
            }
            if (haveIntersect) {
              return (fillOutIntersection(result, module, ovrID, 
                                          pt, intersectedShape, shapeList, nmp, nameBounds, rcx));
            }   
            break; 
          case PathIterator.SEG_LINETO:
            endPt.setLocation(coords[0], coords[1]);
            intersectedShape.add((Point2D)endPt.clone());
            if (!haveIntersect) {
              seg.setBoth(startPt, endPt);
              result = checkSeg(seg, pt, moduleID, startPt, endPt, rcx.getPixDiam());
              if (result != null) {
                IntersectionExtraInfo ei = (IntersectionExtraInfo)result.getSubID();
                fillBoundaryLocation(outerRect, nmp, ei);
                haveIntersect = true;
              }
            }
            startPt.setLocation(coords[0], coords[1]);
            break;
          case PathIterator.SEG_MOVETO:
            movePt.setLocation(coords[0], coords[1]);
            startPt.setLocation(coords[0], coords[1]);
            if (haveIntersect) {
              return (fillOutIntersection(result, module, ovrID, 
                                          pt, intersectedShape, shapeList, nmp, nameBounds, rcx));
            }   
            intersectedShape.clear();
            intersectedShape.add((Point2D)startPt.clone());
            break;
          case PathIterator.SEG_QUADTO:
          case PathIterator.SEG_CUBICTO:
          default:
            throw new IllegalStateException();
        }
       // int winding = pit.getWindingRule();
        pit.next();
      }
      if (haveIntersect) {
        return (fillOutIntersection(result, module, ovrID, 
                                    pt, intersectedShape, shapeList, nmp, nameBounds, rcx));
      }    
    }
    return (null);
  } 
  
 /***************************************************************************
  **
  ** Finalize the intersection info
  */
  
  private Intersection fillOutIntersection(Intersection result,         
                                           NetModule module,
                                           String ovrID, 
                                           Point2D pt,
                                           List<Point2D> intersectedShape, List<TaggedShape> shapeList,
                                           NetModuleProperties nmp, 
                                           Rectangle2D nameBounds, StaticDataAccessContext icx) { 
    IntersectionExtraInfo ei = (IntersectionExtraInfo)result.getSubID();
    ei.contigInfo = nmp.rectsForContig(intersectedShape, shapeList);
    // This tells us what auto-gen member or non-member rects we intersect:
    ei.directShape = nmp.directRectIntersect(pt, shapeList);
    if (ei.directShape != null) {
      // These are alternatives from the underlying definition:
      ei.nonDirectAlternative = (intersectsDefinitionBoundary(module, ovrID, icx, pt) != null);
    }
    ei.gotNameBounds = ((nameBounds != null) && (NetModuleProperties.matchRect(nameBounds, pt, UiUtil.GRID_SIZE / 2.0) != NetModuleProperties.NONE));
    return (result);  
  }

  /***************************************************************************
  **
  ** Answer if we intersect an underlying definition boundary
  */
  
  public Intersection intersectsDefinitionBoundary(NetModule module,
                                                   String ovrID, 
                                                   StaticDataAccessContext icx, Point2D pt) {
  
    NetOverlayProperties nop = icx.getCurrentLayout().getNetOverlayProperties(ovrID);
    String moduleID = module.getID();
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
          
    Point2D startPt = new Point2D.Double(0.0, 0.0);
    Point2D endPt = new Point2D.Double(0.0, 0.0);
    LinkSegment seg = new LinkSegment(startPt, endPt);
    
    Iterator<Rectangle2D> sit = nmp.getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D rect = sit.next();
      Point2D topLeft = new Point2D.Double(rect.getX(), rect.getY());
      Point2D topRight = new Point2D.Double(rect.getMaxX(), rect.getY());
      Point2D bottomRight = new Point2D.Double(rect.getMaxX(), rect.getMaxY());
      Point2D bottomLeft = new Point2D.Double(rect.getX(), rect.getMaxY());
       
      seg.setBoth(topLeft, topRight);
      Intersection result = checkSeg(seg, pt, moduleID, topLeft, topRight, icx.getPixDiam());
      if (result != null) {
        return (result);
      }      
      seg.setBoth(topRight, bottomRight);
      result = checkSeg(seg, pt, moduleID, topRight, bottomRight, icx.getPixDiam());
      if (result != null) {
        return (result);
      }      
      seg.setBoth(bottomRight, bottomLeft);
      result = checkSeg(seg, pt, moduleID, bottomRight, bottomLeft, icx.getPixDiam());
      if (result != null) {
        return (result);
      }      
      seg.setBoth(bottomLeft, topLeft);
      result = checkSeg(seg, pt, moduleID, bottomLeft, topLeft, icx.getPixDiam());
      if (result != null) {
        return (result);
      }
    }
    return (null);
  }  
 
  /***************************************************************************
  **
  ** Answer if we intersect the interior.  This routine figures out what
  ** shapes go with the intersection Point
  */
  
  public Intersection intersectsInterior(DynamicInstanceProxy dip, 
                                         Set<String> useGroups, NetModule module,
                                         String ovrID, 
                                         StaticDataAccessContext rcx,
                                         Point2D pt) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    NetOverlayProperties nop = rcx.getCurrentLayout().getNetOverlayProperties(ovrID);
    NetModuleProperties nmp = nop.getNetModuleProperties(module.getID());
 
    
    //
    // Get all the shapes that make up this module:
    //
    
    String moduleID = module.getID();
    double[] coords = new double[6];
    Point2D nextPt = new Point2D.Double(0.0, 0.0);
    Rectangle outerRect = new Rectangle();
    ArrayList<Point2D> intersectedShape = new ArrayList<Point2D>();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    List<TaggedShape> shapeList = getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, true, null, null);
    if (!outerRect.contains(pt)) {
      return (null);
    }
    
    //
    // See if anybody in the shape contains us.  If so, track down which subshape:
    //
    
    GeneralPath subShape = new GeneralPath();    
    int numShape = shapeList.size();
    for (int i = 0; i < numShape; i++) {
      TaggedShape ts = shapeList.get(i);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        if (ts.shapeClass != TaggedShape.AUTO_MEMBER_SHAPE) {
          continue;
        }
      } else {           
        if (ts.shapeClass != TaggedShape.FULL_DRAW_SHAPE) {
          continue;
        }
      }
 
      //
      // If we do not intersect the interior, keep looking:
      //
      
      Shape boundArea = ts.shape;
      if (!boundArea.contains(pt)) {
        continue;
      }
      
      //
      // We are inside the current shape.  We need to figure out which
      // piece of the shape we are inside, and then what goes into building 
      // the shape.
      //
      
      subShape.reset();
      PathIterator pit = boundArea.getPathIterator(null);
      while (!pit.isDone()) {
        int segType = pit.currentSegment(coords);
        switch (segType) {
          case PathIterator.SEG_CLOSE:
            nextPt.setLocation(coords[0], coords[1]);
            intersectedShape.add((Point2D)nextPt.clone());
            subShape.closePath();
            if (subShape.contains(pt)) {
              IntersectionExtraInfo ei = new IntersectionExtraInfo((Point2D)pt.clone());
              Intersection result = new Intersection(moduleID, ei, 0.0);              
              ei.contigInfo = nmp.rectsForContig(intersectedShape, shapeList);
              return (result);
            }
            break; 
          case PathIterator.SEG_LINETO:
            nextPt.setLocation(coords[0], coords[1]);
            intersectedShape.add((Point2D)nextPt.clone());
            subShape.lineTo((float)coords[0], (float)coords[1]);
            break;
          case PathIterator.SEG_MOVETO:
            nextPt.setLocation(coords[0], coords[1]);
            subShape.reset();
            subShape.moveTo((float)coords[0], (float)coords[1]);
            intersectedShape.clear();
            intersectedShape.add((Point2D)nextPt.clone());
            break;
          case PathIterator.SEG_QUADTO:
          case PathIterator.SEG_CUBICTO:
          default:
            throw new IllegalStateException();
        }
       // int winding = pit.getWindingRule();
        pit.next();
      }
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Get the count of pads for the module
  */

  public int getPadCountForModule(DynamicInstanceProxy dip, Set<String> useGroups, NetModule module,
                                  NetModuleProperties nmp, StaticDataAccessContext rcx) {  

    rcx.pushFrc(fixedFrc_);
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    ArrayList<LinkPad> padList = new ArrayList<LinkPad>();
    getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, false, padList, null);
    rcx.popFrc();   
    return (padList.size());
  }
  
  
  /***************************************************************************
  **
  ** Get the pads for the module.  Given as pad set
  */

  public Set<LinkPad> padsForModule(DynamicInstanceProxy dip, Set<String> useGroups, NetModule module,
                                    NetModuleProperties nmp, StaticDataAccessContext rcx) {
    //
    // Figure out the shapes containing the members:
    //  
    
    rcx.pushFrc(fixedFrc_);
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    ArrayList<LinkPad> padList = new ArrayList<LinkPad>();
    getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, false, padList, null);
    rcx.popFrc();
    HashSet<LinkPad> retval = new HashSet<LinkPad>(padList);
    return (retval);   
  }   
 
  /***************************************************************************
  **
  ** Used for compression/expansion calculations
  ** We do not allow the rows and columns of the full draw shape outline or the component
  ** outline to be removed.  The latter prevents the overall outline from contracting
  ** such that member rectangles don't distort the outline.  The entire name bounds is also 
  ** being excluded, else it could start to bulge outside the bounds.  This is way too
  ** conservative!
  */

  public void needRowsAndCols(DynamicInstanceProxy dip, NetModule module,
                              NetModuleProperties nmp, StaticDataAccessContext rcx, 
                              Rectangle bounds, SortedSet<Integer> needRows, 
                              SortedSet<Integer> needCols, Set<Point2D> usedPads) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    List<TaggedShape> shapeList = getShape(dip, rcx, null, module, nameBounds, outerRect, nmp, true, null, null);      
    
    int slnum = shapeList.size();
    for (int i = 0; i < slnum; i++) {
      TaggedShape shape = shapeList.get(i);
      if ((shape.shapeClass == TaggedShape.FULL_DRAW_SHAPE) || 
          (shape.shapeClass == TaggedShape.COMPONENT_SHAPE)) {
        fillRowColNeeds((Area)shape.shape, bounds, needCols, needRows);
      } else if (shape.shapeClass == TaggedShape.AUTO_MEMBER_SHAPE) { 
        //
        // Need to do this, else space is sucked out between the boundary and the
        // member, but the shape is not changing, so stuff gets overlaid!
        //
        fillRectRowColNeeds(shape.rect, bounds, needCols, needRows, false);
      }
    }
    
    //
    // We want the names to still be bounded when we are done:
    //   
    
    if (nameBounds != null) {
      fillRectRowColNeeds(nameBounds, bounds, needCols, needRows, false);
    }
    
    //
    // Right now, we only spit back used pads.  No other information from the outline is needed:
    //
    Iterator<Point2D> upit = usedPads.iterator();
    while (upit.hasNext()) {
      Point2D padPt = upit.next();
      if ((bounds == null) || bounds.contains(padPt)) {
        needCols.add(new Integer((int)padPt.getX() / UiUtil.GRID_SIZE_INT));
        needRows.add(new Integer((int)padPt.getY() / UiUtil.GRID_SIZE_INT));
      }
    }
    
    return;   
  }  
  
  /***************************************************************************
  **
  ** Used to find out where we cannot expand
  */

  public void expansionExcludedRowsAndCols(DynamicInstanceProxy dip, NetModule module,
                                           NetModuleProperties nmp, StaticDataAccessContext rcx, 
                                           Rectangle bounds, SortedSet<Integer> excludeRows, 
                                           SortedSet<Integer> excludeCols, Set<Point2D> usedPads) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    Rectangle outerRect = new Rectangle();
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    List<TaggedShape> shapeList = getShape(dip, rcx, null, module, nameBounds, outerRect, nmp, true, null, null);      
    
    //
    // The only expansion we don't allow is between used pads and edges.  So for each used
    // pad, find the adjacent used row/column and block it.
    //
    
    TreeSet<Integer> usedRows = new TreeSet<Integer>();
    TreeSet<Integer> usedCols = new TreeSet<Integer>();
    
    //
    // With member-only modules, the module bounds are rigid wrt to
    // node origin, even after expansion.  So we cannot allow rows and
    // cols within the module to expand and keep pads consistent.
    //
    
    
    int slnum = shapeList.size();
    if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
      // Too conservative.  Only need to do this for module members that
      // have the links!
      if (!usedPads.isEmpty()) {
        for (int i = 0; i < slnum; i++) {   
          TaggedShape shape = shapeList.get(i);
          fillRectRowColNeeds(shape.rect, bounds, excludeCols, excludeRows, true);
        }
      }
      return;
    } else {
      for (int i = 0; i < slnum; i++) {
        TaggedShape shape = shapeList.get(i);
        if ((shape.shapeClass == TaggedShape.FULL_DRAW_SHAPE) || 
            (shape.shapeClass == TaggedShape.COMPONENT_SHAPE)) {
          fillRowColNeeds((Area)shape.shape, bounds, usedCols, usedRows);
          break;
        } else if (shape.shapeClass == TaggedShape.AUTO_MEMBER_SHAPE) {
          fillRectRowColNeeds(shape.rect, bounds, excludeCols, excludeRows, true);
        }
      }
    }

    Iterator<Point2D> upit = usedPads.iterator();
    while (upit.hasNext()) {
      Point2D padPt = upit.next();
      int padX = (int)padPt.getX() / UiUtil.GRID_SIZE_INT;
      int padY = (int)padPt.getY() / UiUtil.GRID_SIZE_INT;
      excludeCols.add(new Integer(padX));
      excludeRows.add(new Integer(padY));
      Integer xm = new Integer(padX - 1);
      Integer xp = new Integer(padX + 1);
      Integer ym = new Integer(padY - 1);
      Integer yp = new Integer(padY + 1);      
      if (usedCols.contains(xm)) {
        excludeCols.add(xm);
      }
      if (usedCols.contains(xp)) {
        excludeCols.add(xp);
      }
      if (usedRows.contains(ym)) {
        excludeRows.add(ym);
      }
      if (usedRows.contains(yp)) {
        excludeRows.add(yp);
      }
    }    
    return;   
  }  

  /***************************************************************************
  **
  ** Given preferred new pad locations, this tells us what we can actually
  ** get.
  */

  public Map<Layout.PointNoPoint, LinkPad> closestRemainingPads(DynamicInstanceProxy dip, Set<String> useGroups, NetModule module,
                                                                NetModuleProperties nmp, StaticDataAccessContext rcx, 
                                                                Map<Layout.PointNoPoint, Vector2D> rigidMoves, 
                                                                Map<Layout.PointNoPoint, LinkPad> wantPads, 
                                                                Map<Layout.PointNoPoint, LinkPad> currPads, boolean orphansOnly) {
  
    //
    // Figure out the shapes containing the members:
    //  
    
    Rectangle outerRect = new Rectangle();
    rcx.pushFrc(fixedFrc_);
    Rectangle2D nameBounds = getNameBounds(nmp, module, rcx);    
    ArrayList<LinkPad> padList = new ArrayList<LinkPad>();
    getShape(dip, rcx, useGroups, module, nameBounds, outerRect, nmp, false, padList, null);
    rcx.popFrc();
    
    HashMap<Layout.PointNoPoint, LinkPad> retval = new HashMap<Layout.PointNoPoint, LinkPad>();
    HashSet<LinkPad> padSet = new HashSet<LinkPad>(padList);  // faster search
    onePadPass(padSet, retval, wantPads, rigidMoves, true, currPads, orphansOnly);
    onePadPass(padSet, retval, wantPads, rigidMoves, false, currPads, orphansOnly);
    return (retval);   
  } 
  
  /***************************************************************************
  **
  ** Get the name bounds rectangle, null if not displaying name
  */  
  
  public Rectangle2D getNameBounds(NetModuleProperties nmp,  
                                   NetModule module,  
                                   StaticDataAccessContext irx) {  
  
    if (nmp.isHidingLabel()) {
      return (null);
    }    
    String name = module.getName();
    Point2D nameLoc = nmp.getNameLoc();
    if (nameLoc == null) {
      return (null);
    }    
    AnnotatedFont abFont = irx.getFontManager().getOverrideFont(FontManager.NET_MODULE, nmp.getFontOverride());
    String breakDef = nmp.getLineBreakDef();
    Rectangle2D nameBounds;
    if (breakDef == null) {  
      nameBounds = abFont.getFont().getStringBounds(name, irx.getFrc());
      double padWidth = nameBounds.getWidth() + (UiUtil.GRID_SIZE * 2.0);
      double padHeight = nameBounds.getHeight() + UiUtil.GRID_SIZE;
      nameBounds.setRect(nameLoc.getX() - (padWidth / 2.0), 
                         nameLoc.getY() - (padHeight / 2.0),  padWidth, padHeight);

    } else {
      List<String> frags = MultiLineRenderSupport.applyLineBreaksForFragments(name, breakDef);
      String[] toks = new String[frags.size()];
      frags.toArray(toks);
      nameBounds = MultiLineRenderSupport.multiLineGuts(irx.getFrc(), toks, abFont, nameLoc, null, null, null, null);
      nameBounds.setRect(nameBounds.getX() - UiUtil.GRID_SIZE, nameBounds.getY() - UiUtil.GRID_SIZE, 
                         nameBounds.getWidth() + (UiUtil.GRID_SIZE * 2.0),  nameBounds.getHeight() + (UiUtil.GRID_SIZE * 2.0)); 
    }
    UiUtil.force2DToGrid(nameBounds, UiUtil.GRID_SIZE);
    return (nameBounds);   
  }
  
  /***************************************************************************
  **
  ** Reorganize the module properties shapes to ~minimize (not optimal) the
  ** rectangle count.
  */  
    
  public List<Rectangle2D> newReassemble(NetModuleProperties nmp) {
    
    // Nothing to do if not multi-rect:
    if (nmp.getType() != NetModuleProperties.MULTI_RECT) {
      return (null);
    }
    
    //
    // Build up JUST the rects (no member/non-member etc.):
    //   
    
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;    
    Area oneArea = null;
    Iterator<Rectangle2D> sit = nmp.getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D rect = sit.next();
      // record upper left corners:
      double rectX = rect.getX();
      double rectY = rect.getY();
      if (rectX < minX) { 
        minX = rectX;
      }
      if (rectY < minY) { 
        minY = rectY;
      }      
      Area rectArea = new Area(rect);
      if (oneArea == null) {
        oneArea = (Area)rectArea.clone();
      } else {
        oneArea.add(rectArea);
      }
    }
    Point2D minULPt = new Point2D.Double(minX, minY);
    
    //
    // We only initiate at upper left corners:
    //
    
    ArrayList<Rectangle2D> retval = new ArrayList<Rectangle2D>();
    while (true) {
      Rectangle2D nextRect = ohGrowUp(oneArea, minULPt);
      if (nextRect == null) {
        // Failure? Then return original list!
        if (retval.isEmpty()) {
          Iterator<Rectangle2D> ebit = nmp.getShapeIterator();
          while (ebit.hasNext()) {
            Rectangle2D rect = ebit.next();
            retval.add(rect);
          }
        }        
        return (retval);
      }
      if (nextRect.getWidth() > 0.0) {
        retval.add(nextRect);
      }
      oneArea.subtract(new Area(nextRect));
    }    
  }  
  
  /***************************************************************************
  **
  ** Reorganize the module properties shapes to ~minimize (not optimal) the
  ** rectangle count.
  */  
    
  private Rectangle2D ohGrowUp(Area workingArea, Point2D minULPt) {
    
    //
    // We only initiate at upper left corners:
    //
    
    TreeMap<Double, Point2D> sortMap = new TreeMap<Double, Point2D>();
    List<CornerInfo> corners = buildCornerList(workingArea);
    
    Point2D testInPt = new Point2D.Double();
    int numCorner = corners.size();
    if (numCorner == 0) {
      return (null);
    }
    for (int i = 0; i < numCorner; i++) {
      CornerInfo ci = corners.get(i);
      int inDirCanon = ci.inDir.canonicalDir();
      int outDirCanon = ci.outDir.canonicalDir();
      if (((inDirCanon == Vector2D.NORTH) && (outDirCanon == Vector2D.EAST)) ||
          ((outDirCanon == Vector2D.SOUTH) && (inDirCanon == Vector2D.WEST))) {
        // gotta be an outside corner:
        testInPt.setLocation(ci.point.getX() + (UiUtil.GRID_SIZE / 2.0), ci.point.getY() + (UiUtil.GRID_SIZE / 2.0));
        if (workingArea.contains(testInPt)) {
          double distSq = minULPt.distanceSq(ci.point);
          sortMap.put(new Double(distSq), ci.point);
        }
      }
    }
    // Emergency bail!
    
    if (sortMap.isEmpty()) {
      return (null);
    }

    Point2D startPt = sortMap.get(sortMap.keySet().iterator().next());
    double stX = startPt.getX();
    double stY = startPt.getY();
    // First inc will make this square:
    // Seeing non-modular rects!  If this is true, we cannot assume we can work on GRID
    // and not have an infinite loop!  Of course, non-modular rects: WHY???
    Rectangle2D testRect = new Rectangle2D.Double(); 
    Rectangle2D lastRect = new Rectangle2D.Double(stX, stY, 0.0, 1.0 /*UiUtil.GRID_SIZE*/); 
    boolean doX = true;
    boolean doY = true;
    while (doX || doY) {
      if (doX) {
        testRect.setRect(lastRect.getX(), lastRect.getY(), lastRect.getWidth() + 1/*UiUtil.GRID_SIZE*/, lastRect.getHeight());
        if (!addAndCheck(workingArea, testRect, lastRect)) {
          doX = false;
        }
      }
      if (doY) {
        testRect.setRect(lastRect.getX(), lastRect.getY(), lastRect.getWidth(), lastRect.getHeight() + 1/*UiUtil.GRID_SIZE*/);
        if (!addAndCheck(workingArea, testRect, lastRect)) {
          doY = false;
        }
      }
    }
    return (lastRect);
  } 
  
  /***************************************************************************
  **
  ** Do a rectangle enclosure check.  If the testRect can be added into the working
  ** area without changing it, we keep going after updating the last rect.  If we
  ** have exceeded the bounds of the area, we stop.
  */  
    
  private boolean addAndCheck(Area workingArea, Rectangle2D testRect, Rectangle2D lastRect) {  
    Area checkingArea = (Area)workingArea.clone();
    checkingArea.add(new Area(testRect));
    if (!checkingArea.equals(workingArea)) {
      return (false);
    }
    lastRect.setRect(testRect);
    return (true);
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Check the segment for intersection
  */  
  
  private Intersection checkSeg(LinkSegment seg, Point2D pt, String moduleID, 
                                Point2D startPt, Point2D endPt, double pixDiam) {
    
    double useTol = (INTERSECT_TOL > pixDiam) ? INTERSECT_TOL : 2.0 * pixDiam;
    Double is = seg.intersectsStart(pt, useTol);
    if (is != null) {
      Point2D onSeg = (Point2D)startPt.clone();
      IntersectionExtraInfo ei = new IntersectionExtraInfo(startPt, null, onSeg);
      Intersection intersect = new Intersection(moduleID, ei, is.doubleValue());
      return (intersect); 
    }
    
    is = seg.intersectsEnd(pt, useTol);
    if (is != null) {
      Point2D onSeg = (Point2D)endPt.clone();
      IntersectionExtraInfo ei = new IntersectionExtraInfo(endPt, null, onSeg);
      Intersection intersect = new Intersection(moduleID, ei, is.doubleValue());
      return (intersect);               
    }
    
    is = seg.intersects(pt, useTol);
    if (is != null) {
      Point2D onSeg = seg.getClosestPoint(pt);
      IntersectionExtraInfo ei = new IntersectionExtraInfo(startPt, endPt, onSeg);
      Intersection intersect = new Intersection(moduleID, ei, is.doubleValue());
      return (intersect);
    }
    
    return (null);
  }

  /***************************************************************************
  **
  ** If we intersect the boundary of a contiguous region, it might not be part of
  ** the underlying defined rectangle for the region, but may be set by contained
  ** requirements.  Classify which boundary/corner is being intersected so we
  ** can interpret the result.
  */  
  
  private void fillBoundaryLocation(Rectangle2D outerRect,
                                    NetModuleProperties nmp, IntersectionExtraInfo iexi) {
    
    if (nmp.getType() == NetModuleProperties.CONTIG_RECT) {
      iexi.outerMatch = NetModuleProperties.matchRectExact(outerRect, iexi.intersectPt);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the shape of the module
  */
  
  private List<TaggedShape> getShape(DynamicInstanceProxy dip,
                                     StaticDataAccessContext rcx,
                                     Set<String> useGroups,  
                                     NetModule module, Rectangle2D textBounds,
                                     Rectangle outerRect,
                                     NetModuleProperties nmp, boolean showComponents, List<LinkPad> bubbleList,
                                     Rectangle2D superRect) {
    //
    // Figure out the areas containing the members:
    //
    switch (nmp.getType()) {
      case NetModuleProperties.CONTIG_RECT:
        return (getContigRectShape(rcx, dip, useGroups, module, 
                                   textBounds, outerRect, nmp, showComponents, bubbleList, superRect));
      case NetModuleProperties.MULTI_RECT:
        return (getMultiRectShape(rcx, dip, useGroups,  module, 
                                  textBounds, outerRect, nmp, showComponents, bubbleList, superRect));        
      case NetModuleProperties.MEMBERS_ONLY:
    	List<TaggedShape> retval = getMembersOnlyShape(rcx, module, textBounds, outerRect, nmp, bubbleList);
    	if (outerRect != null && superRect != null) {
    		superRect.setRect(outerRect);
    	}
    	return retval;
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the set of non members
  */
  
  private Set<String> getNonMembers(Genome genome, Set<String> useGroups, NetModule module, Set<String> nodes, DynamicInstanceProxy dip) {

    //
    // Build up a list for all non-members.  If the useGroups arg is null. we start wih all
    // nodes.  If not, we use the groups in the model to drive the set of all nodes!
    //
    
    HashSet<String> allNonMems = new HashSet<String>();
    if (useGroups == null) {
      Iterator<Node> git = genome.getAllNodeIterator();    
      while (git.hasNext()) {
        Node node = git.next();
        allNonMems.add(node.getID());
      }
    } else {
      GenomeInstance gi = (GenomeInstance)genome;
      Iterator<String> ugit = useGroups.iterator();
      while (ugit.hasNext()) {
        String grpID = ugit.next();
        Group grp = gi.getGroup(Group.getBaseID(grpID));
        Iterator<GroupMember> mit = grp.getMemberIterator();
        while (mit.hasNext()) {
          GroupMember gm = mit.next();
          allNonMems.add(gm.getID());
        }
      }
    }

    if ((dip != null) && dip.hasAddedNodes()) {
      Iterator<DynamicInstanceProxy.AddedNode> ait = dip.getAddedNodeIterator();    
      while (ait.hasNext()) {
        DynamicInstanceProxy.AddedNode an = ait.next();
        allNonMems.add(an.nodeName);
      } 
    }
    
    Iterator<NetModuleMember> mit = module.getMemberIterator();
    while (mit.hasNext()) {
      NetModuleMember nmm = mit.next();
      String nmID = nmm.getID();
      nodes.add(nmID);
      allNonMems.remove(nmID);
    }
 
    return (allNonMems);
  }
  
  
  
  /***************************************************************************
  **
  ** Get the shape of the module for the contiguous single rectangle case
  */
  
  private List<TaggedShape> getContigRectShape(StaticDataAccessContext rcx,
                                               DynamicInstanceProxy dip, Set<String> useGroups,
                                               NetModule module, Rectangle2D textBounds, 
                                               Rectangle outerRect,
                                               NetModuleProperties nmp,
                                               boolean showComponents, List<LinkPad> bubbleList,
                                               Rectangle2D superRect) {

    //
    // The contiguous rectangle shape is a single rectangle that bounds all the contents
    // of the module, with non-members internal to the rectangle excised from the region. 

    if (!nmp.hasOneShape()) {
      throw new IllegalStateException();
    }
    
    ArrayList<TaggedShape> retval = new ArrayList<TaggedShape>();
    
    //
    // Build up a list for all non-members.
    //
    
    HashSet<String> nodes = new HashSet<String>();
    Set<String> allNonMems = getNonMembers(rcx.getCurrentGenome(), useGroups, module, nodes, dip);
       
    Rectangle2D rect = nmp.getShapeIterator().next();
    
    HashMap<String, Rectangle> membRects = null;
    Rectangle intRect = UiUtil.rectFromRect2D(rect);
    
    if (showComponents || rcx.isForWeb()) {
    	membRects = new HashMap<String, Rectangle>();
    }
    
    if (showComponents) {
      Area rectArea = new Area(rect);
      //membRects = new HashMap<String, Rectangle>();
      retval.add(new TaggedShape(TaggedShape.COMPONENT_SHAPE, rectArea, rect, null));
      // Always show the text bounds in component mode:
      if (textBounds != null) {
        rectArea = new Area(textBounds);
        retval.add(new TaggedShape(TaggedShape.TITLE_SHAPE, rectArea, textBounds, null));
      }
    } 
      
    Rectangle labelRect = UiUtil.rectFromRect2D(textBounds);
    Rectangle finRect = NodeBounder.nodeBounds(nodes, rcx, intRect, labelRect, UiUtil.GRID_SIZE_INT, membRects);
    outerRect.setBounds(finRect);
    Area boundArea = new Area(finRect);
    

    
    if (showComponents) {
      Iterator<String> kit = membRects.keySet().iterator();
      while (kit.hasNext()) {
        String memberID = kit.next();
        Rectangle memberRect = membRects.get(memberID);
        retval.add(new TaggedShape(TaggedShape.AUTO_MEMBER_SHAPE, memberRect, memberRect.getBounds2D(), memberID));
      }
    }
    
    Area padArea = null;    
    if (bubbleList != null) {
      Rectangle padPath = padRect(finRect);
      padArea = new Area(padPath);
    }
    
    //
    // Figure out all the nodes that are inside that rect.  This may include some nodes not
    // part of the module.  Find out which those are, use them to figure the set of nodes 
    // inside but not part of the module:
    
    Map<String, Rectangle> skipRects = NodeBounder.nodeRects(allNonMems, rcx, UiUtil.GRID_SIZE_INT);
    Area origArea = (Area)boundArea.clone();

    //
    // Create the shape to draw:
    //
       
    Area subPadArea = (padArea != null) ? (Area)padArea.clone() : null;    
    // Use sorted keys so that omissions to prevent snuffing out the module are
    // consistent:
    TreeSet<String> sortedSRkeys = new TreeSet<String>(skipRects.keySet());
    Iterator<String> rit = sortedSRkeys.iterator();
    while (rit.hasNext()) {
      String skipID = rit.next();
      Rectangle eaRect = skipRects.get(skipID);
      if (!origArea.intersects(eaRect)) {
        continue;
      }
      Area rectArea = new Area(eaRect);
      if (showComponents) {
        retval.add(new TaggedShape(TaggedShape.NON_MEMBER_SHAPE, rectArea, eaRect.getBounds2D(), skipID));
      }
      Area nextArea = (Area)boundArea.clone();
      nextArea.subtract(rectArea);
      if (nextArea.isEmpty()) {
        continue;
      }
      boundArea = nextArea; 
      if (bubbleList != null) {
        Rectangle padPath = padRect(eaRect);
        Area eaPadArea = new Area(padPath);
        if (subPadArea == null) {
          subPadArea = (Area)eaPadArea.clone();
        } else {
          subPadArea.subtract(eaPadArea);
        }
      }
    }
    
    //
    // Figure out the super rectangle
    if (rcx.isForWeb() && superRect != null) {
    	superRect.setRect(outerRect.getBounds2D());
	    for (Iterator<String> kit = membRects.keySet().iterator(); kit.hasNext(); ) {
	    	String memberID = kit.next();
	        Rectangle memberRect = membRects.get(memberID);
	        
	        superRect.setRect(superRect.createUnion(memberRect.getBounds2D()));
	    }
    }
    
    //
    // Build bubbles:
    //
    
    bubbleMachine(padArea, subPadArea, boundArea, nmp, bubbleList, null, null);
  
    retval.add(new TaggedShape(TaggedShape.FULL_DRAW_SHAPE, boundArea, null, null));
    return (retval);
  }  
  
  
  /***************************************************************************
  **
  ** Get the shape of the module for the multi-rectangle type
  */
  
  private List<TaggedShape> getMultiRectShape(StaticDataAccessContext rcx,
                                              DynamicInstanceProxy dip, Set<String> useGroups, 
                                              NetModule module, Rectangle2D textBounds, Rectangle outerRect,
                                              NetModuleProperties nmp, boolean showComponents, List<LinkPad> bubbleList,
                                              Rectangle2D superRect) {

    //
    // Figure out the areas that are a combination of drawn rectangles:
    //
    
    
    ArrayList<TaggedShape> retval = new ArrayList<TaggedShape>();
    Rectangle extent = null;
    Area oneArea = null;
    Area onePadArea = null;
    Iterator<Rectangle2D> sit = nmp.getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D rect = sit.next();
      Area rectArea = new Area(rect);
      if (showComponents) {
        retval.add(new TaggedShape(TaggedShape.COMPONENT_SHAPE, rectArea, rect, null));
      } 
      if (oneArea == null) {
        oneArea = (Area)rectArea.clone();
      } else {
        oneArea.add(rectArea);
      }
      Rectangle extAdd = UiUtil.rectFromRect2D(rect);
      if (extent == null) {
        extent = extAdd;
      } else {
        Bounds.tweakBounds(extent, extAdd);
      }
      if (bubbleList != null) {
        Rectangle padPath = padRect(extAdd);
        Area padArea = new Area(padPath);
        if (onePadArea == null) {
          onePadArea = (Area)padArea.clone();
        } else {
          onePadArea.add(padArea);
        }
      }     
    }
    
    Area rectOnlyPadArea = null;
    if (bubbleList != null) {
      rectOnlyPadArea = (Area)onePadArea.clone();  
    }
    
    
    //
    // Build up a list for all non-members.
    //
    
    HashSet<String> nodes = new HashSet<String>();
    Set<String> allNonMems = getNonMembers(rcx.getCurrentGenome(), useGroups, module, nodes, dip);
    
    //
    // Now add rectangles for all members.  Keep a separate copy to use as a filter for
    // non-member deletions (NO! See below):
    //
    
    // Area memArea = null;
    HashMap<String, Area> memPadAreas = new HashMap<String, Area>();
    Map<String, Rectangle> eachRect = NodeBounder.nodeRects(nodes, rcx, UiUtil.GRID_SIZE_INT);
    Iterator<String> rit = eachRect.keySet().iterator();
    while (rit.hasNext()) {
      String memberID = rit.next();
      Rectangle eaRect = eachRect.get(memberID);
      Area rectArea = new Area(eaRect);
      if (showComponents) {
        retval.add(new TaggedShape(TaggedShape.AUTO_MEMBER_SHAPE, rectArea, eaRect.getBounds2D(), memberID));
      }
      if (oneArea == null) {
        oneArea = (Area)rectArea.clone();
      } else {
        oneArea.add(rectArea);
      }      
      //if (memArea == null) {
      //  memArea = (Area)rectArea.clone();
      //} else {
      //  memArea.add(rectArea);
      //}
      if (extent == null) {
        extent = eaRect;
      } else {
        Bounds.tweakBounds(extent, eaRect);
      }
      if (bubbleList != null) {
        Rectangle padPath = padRect(eaRect);
        Area padArea = new Area(padPath);
        memPadAreas.put(memberID, padArea);
        if (onePadArea == null) {
          onePadArea = (Area)padArea.clone();
        } else {
          onePadArea.add(padArea);
        }
      }
    }
   
    //
    // Add the members to the base area in one operation (NO! See below)
    //
    
    //if (memArea != null) {
    //  if (oneArea == null) {
    //    oneArea = (Area)memArea.clone();
    //  } else {
    //    oneArea.add(memArea);
    //  }
    //}
   
    if (textBounds != null) {
      Area rectArea = new Area(textBounds);
      if (showComponents) {
        retval.add(new TaggedShape(TaggedShape.TITLE_SHAPE, rectArea, textBounds, null));
      } 
      if (oneArea == null) {
        oneArea = (Area)rectArea.clone();
      } else {
        oneArea.add(rectArea);
      }
      Rectangle txtAdd = UiUtil.rectFromRect2D(textBounds);
      if (extent == null) {
        extent = txtAdd;
      } else {
        Bounds.tweakBounds(extent, txtAdd);
      }
      if (bubbleList != null) {
        Rectangle padPath = padRect(txtAdd);
        Area padArea = new Area(padPath);
        if (onePadArea == null) {
          onePadArea = (Area)padArea.clone();
        } else {
          onePadArea.add(padArea);
        }
      }
    }  
      
    //
    // Now subtract anybody not in the module.
    // NOTE:  Tried the operation of first filtering the non-member areas to subtract
    // by the member areas, such that the presence of a member prevented the erosion of
    // the module by the non-member.  But this made it easy for a non-member to "hide"
    // in the module.  Having eroded areas is much more noticeable that something is
    // amiss.  Downside is that a member could look outside the module when overlapped
    // by a non-member, but users should be tried to avoid a swiss-cheese module anyway...
    //
    // ALSO: Previous test only generated module bounds for nodes "inside" the extent,
    // but the problem here is that the traditional node intersection of a rectangle
    // misses all intersection where there is not at least one point inside the rect.
    // Need the CAG version of intersection.  Not _so_ bad, since both methods generate
    // node bounds for all nodes anyway!
   
    Area subPadArea = (onePadArea != null) ? (Area)onePadArea.clone() : null;
    Map<String, Rectangle> skipRects = NodeBounder.nodeRects(allNonMems, rcx, UiUtil.GRID_SIZE_INT);
    Area origArea = (Area)oneArea.clone();
    
    // Use sorted keys so that omissions to prevent snuffing out the module are
    // consistent:
    TreeSet<String> sortedSRkeys = new TreeSet<String>(skipRects.keySet());
    Iterator<String> srit = sortedSRkeys.iterator();
    while (srit.hasNext()) {
      String skipID = srit.next();
      Rectangle eaRect = skipRects.get(skipID);
      if (!origArea.intersects(eaRect)) {
        continue;
      }
      Area rectArea = new Area(eaRect);
      // Filter by the member area: (NO! see above)
      //  if (memArea != null) {
      //   rectArea.subtract(memArea);
      //  }
      retval.add(new TaggedShape(TaggedShape.NON_MEMBER_SHAPE, rectArea, eaRect.getBounds2D(), skipID));
      Area nextArea = (Area)oneArea.clone();
      nextArea.subtract(rectArea);
      //
      // We do not allow a non-member subtraction to completely snuff out the module:
      if (nextArea.isEmpty()) {
        continue;
      }
      oneArea = nextArea;
      if (bubbleList != null) {
        Rectangle padPath = padRect(eaRect);
        Area padArea = new Area(padPath);
        if (subPadArea == null) {
          subPadArea = (Area)padArea.clone();
        } else {
          subPadArea.subtract(padArea);
        }
      }
    }
      
    
    //
    // Figure out the super rectangle
    if (rcx.isForWeb() && superRect != null) {
    	superRect.setRect(outerRect.getBounds2D());
	    for (Iterator<String> kit = eachRect.keySet().iterator(); kit.hasNext(); ) {
	    	String memberID = kit.next();
	        Rectangle memberRect = eachRect.get(memberID);
	        
	        superRect.setRect(superRect.createUnion(memberRect.getBounds2D()));
	    }
    }
    
    //
    // Build bubbles:
    //
    
    bubbleMachine(onePadArea, subPadArea, oneArea, nmp, bubbleList, memPadAreas, rectOnlyPadArea);

       
    retval.add(new TaggedShape(TaggedShape.FULL_DRAW_SHAPE, oneArea, null, null));
    outerRect.setBounds(extent);
    return (retval);
  }  

  
  /***************************************************************************
  **
  ** Generate bubbles.  Make sure we have at least two!
  */
  
  private void bubbleMachine(Area onePadArea, Area subPadArea, Area trueArea,  
                             NetModuleProperties nmp, List<LinkPad> bubbleList, 
                             Map<String, Area> multiRectMemPadAreas, Area rectOnlyPadArea) {
    
    if (bubbleList == null) {
      return;
    }
      
    bubbleMachineCore(onePadArea, subPadArea, trueArea, bubbleList, multiRectMemPadAreas, rectOnlyPadArea, NORMAL_PADS_);
      
    if (bubbleList.size() < 4) {
      bubbleList.clear();
      bubbleMachineCore(onePadArea, subPadArea, trueArea, bubbleList, multiRectMemPadAreas, rectOnlyPadArea, ALL_BUT_CORNER_PADS_);
    }
    
    if (bubbleList.size() < 4) {
      bubbleList.clear();
      bubbleMachineCore(onePadArea, subPadArea, trueArea, bubbleList, multiRectMemPadAreas, rectOnlyPadArea, ALL_PADS_);
    } 
    
    //
    // Things should never get this desperate!
    //
    
    if (bubbleList.size() < 2) {
      bubbleList.clear();
      Rectangle2D rect = nmp.getShapeIterator().next();      
      double x = rect.getCenterX();
      double y = rect.getCenterY();
      x = UiUtil.forceToGridValue(x, UiUtil.GRID_SIZE);
      y = UiUtil.forceToGridValue(y, UiUtil.GRID_SIZE);  
      Point2D nextPoint = new Point2D.Double(x - UiUtil.GRID_SIZE, y);
      LinkPad lp = new LinkPad(new Vector2D(-1.0, 0.0), new Vector2D(0.0, 1.0), nextPoint, NetModuleProperties.NONE, null);
      bubbleList.add(lp);
      nextPoint = new Point2D.Double(x + UiUtil.GRID_SIZE, y);
      lp = new LinkPad(new Vector2D(1.0, 0.0), new Vector2D(0.0, -1.0), nextPoint, NetModuleProperties.NONE, null);
      bubbleList.add(lp);
    }
 
    return;
  } 
  
  /***************************************************************************
  **
  ** Only bubbles that appear even after non-members are excised survive:
  */
  
  private void bubbleMachineCore(Area onePadArea, Area subPadArea, Area trueArea,  
                                 List<LinkPad> bubbleList, Map<String, Area> multiRectMemPadAreas, 
                                 Area rectOnlyPadArea, int forceLevel) {
    
    boolean multiRect = (multiRectMemPadAreas != null);
    List<CornerInfo> corners = (multiRect) ? buildCornerList(trueArea) : null;
    
    //
    // All this is to allow us to tag pads in a multi-rect shape if the
    // pad is exclusive to a member-only block:
    //
    HashMap<Point2D, String> bubToMem = null;
    if (multiRect) {
      bubToMem = new HashMap<Point2D, String>();
      ArrayList<LinkPad> rectOnlyList = new ArrayList<LinkPad>();
      buildPadList(rectOnlyPadArea, rectOnlyList, null, ALL_PADS_, null);
      Set<Point2D> rectOnlySubSet = pointSetForPadList(rectOnlyList);
      Iterator<String> kit = multiRectMemPadAreas.keySet().iterator();
      while (kit.hasNext()) {
        String memID = kit.next();
        Area memPadArea = multiRectMemPadAreas.get(memID);
        ArrayList<LinkPad> memList = new ArrayList<LinkPad>();
        buildPadList(memPadArea, memList, null, ALL_PADS_, memID);        
        int numMem = memList.size();
        for (int i = 0; i < numMem; i++) {
          LinkPad forMem = memList.get(i);
          if (!rectOnlySubSet.contains(forMem.point)) {
            if (bubToMem.get(forMem.point) != null) {
              // FIX ME!  More than one node wants to claim this pad.  We should
              // keep a list of claimers, but then things get more complex.  I think
              // this means some times links may not stick with their original member
              // when shifting stuff around, which is not desirable...
              // FIX ME Shared TIEBREAKER
            }
            bubToMem.put(forMem.point, memID);
          }
        }
      }
    }
    
    ArrayList<LinkPad> addList = new ArrayList<LinkPad>();
    buildPadList(onePadArea, addList, corners, forceLevel, null);
    ArrayList<LinkPad> subList = new ArrayList<LinkPad>();
    buildPadList(subPadArea, subList, null, ALL_PADS_, null);
    Set<Point2D> subSet = pointSetForPadList(subList);
    int numAdd = addList.size();
    for (int i = 0; i < numAdd; i++) {
      LinkPad toAdd = addList.get(i);
      if (subSet.contains(toAdd.point)) {
        if (bubToMem != null) {
          String memberID = bubToMem.get(toAdd.point);
          if ((memberID != null) && (toAdd.nodeID == null)) { // Latter always true anyway, right??
            toAdd.nodeID = memberID;
          }
        }
        bubbleList.add(toAdd);
      }
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Get the shape of the module for the members only type:
  */
  
  private List<TaggedShape> getMembersOnlyShape(StaticDataAccessContext rcx,
                                                NetModule module, Rectangle2D textBounds,
                                                Rectangle outerRect,
                                                NetModuleProperties nmp, List<LinkPad> bubbleList) {
     
    if (nmp.hasShapes()) {
      throw new IllegalStateException();
    } 
    
    //
    // Get the member node IDs:
    //
 
    HashSet<String> nodes = new HashSet<String>();
    Iterator<NetModuleMember> mit = module.getMemberIterator();
    while (mit.hasNext()) {
      NetModuleMember nmm = mit.next();
      nodes.add(nmm.getID());
    }
    
    //
    // Create the shapes to draw.  Also create an outer bound.
    //
    
    ArrayList<TaggedShape> retval = new ArrayList<TaggedShape>();
    Rectangle extent = null;
    Map<String, Rectangle> eachRect = NodeBounder.nodeRects(nodes, rcx, UiUtil.GRID_SIZE_INT);
    Iterator<String> rit = eachRect.keySet().iterator();
    while (rit.hasNext()) {
      String memberID = rit.next();
      Rectangle eaRect = eachRect.get(memberID);    
      Area rectArea = new Area(eaRect);
      if (bubbleList != null) {
        Rectangle padPath = padRect(eaRect); 
        buildPadList(new Area(padPath), bubbleList, null, NORMAL_PADS_, memberID);
      }
      retval.add(new TaggedShape(TaggedShape.AUTO_MEMBER_SHAPE, rectArea, eaRect.getBounds2D(), memberID));
      if (extent == null) {
        extent = eaRect;
      } else {
        Bounds.tweakBounds(extent, eaRect);
      }
    }
    
    if (textBounds != null) {
      Area rectArea = new Area(textBounds);
      retval.add(new TaggedShape(TaggedShape.TITLE_SHAPE, rectArea, textBounds, null));
      Rectangle txtAdd = UiUtil.rectFromRect2D(textBounds);
      if (extent == null) {
        extent = txtAdd;
      } else {
        Bounds.tweakBounds(extent, txtAdd);
      }     
    }  

    outerRect.setBounds(extent);
    return (retval);
  }

  /***************************************************************************
  **
  ** Calculate the stroke
  */

  private BasicStroke calcStroke(int modType, int shapeClass, boolean showComponents) {
    
    if (shapeClass == TaggedShape.NON_MEMBER_SHAPE) {  // we never draw these guys...
      return (null);
    }
   
    if (!showComponents || 
        (shapeClass == TaggedShape.FULL_DRAW_SHAPE) || 
        (modType == NetModuleProperties.MEMBERS_ONLY)) {
      return (new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
    }
  
    int butt = BasicStroke.CAP_BUTT;
    int join = BasicStroke.JOIN_ROUND;
    float miter = 10.0F;    
 
    float[] usePattern;
    switch (shapeClass) {
      case TaggedShape.COMPONENT_SHAPE:
        usePattern = longDashPatternArray;
        break;
      case TaggedShape.AUTO_MEMBER_SHAPE:
        usePattern = dotPatternArray;
        break;
      case TaggedShape.TITLE_SHAPE:
        usePattern = dashPatternArray;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return (new BasicStroke(4, butt, join, miter, usePattern, 0F));
  }  
  
  /***************************************************************************
  **
  ** Calculate the centerline rectangle for pads
  */

  private Rectangle padRect(Rectangle shapeRect) {
    return (new Rectangle(shapeRect.x - PAD_SIZE_INT, shapeRect.y - PAD_SIZE_INT,
                          shapeRect.width + (2 * PAD_SIZE_INT), 
                          shapeRect.height + (2 * PAD_SIZE_INT)));
  }  

  
  /***************************************************************************
  **
  ** Get the point set for a pad list
  */

  private Set<Point2D> pointSetForPadList(List<LinkPad> padList) {  
  
    HashSet<Point2D> retval = new HashSet<Point2D>();
    int numPad = padList.size();
    for (int i = 0; i < numPad; i++) {
      LinkPad pad = padList.get(i);
      retval.add(pad.point);
    }
    return (retval); 
  }
  
  /***************************************************************************
  **
  ** Generate the list of pad centers to draw
  */

  private void buildPadList(Area padOutline, List<LinkPad> bubbleList, List<CornerInfo> cornerList, int forceLevel, String nodeID) {
    double[] coords = new double[6];
    Point2D firstPt = new Point2D.Double(0.0, 0.0);
    Point2D lastPt = new Point2D.Double(0.0, 0.0);    
    Point2D nextPt = new Point2D.Double(0.0, 0.0);
    Vector2D firstDir = null;
    Vector2D lastDir = null;
    Vector2D currDir = null;
    ArrayList<LinkPad> startPads = new ArrayList<LinkPad>();
    ArrayList<LinkPad> endPads = new ArrayList<LinkPad>();
    ArrayList<LinkPad> firstStartPads = null;
    ArrayList<LinkPad> lastEndPads = new ArrayList<LinkPad>();
    double lastLen = -1.0;
    
    PathIterator pit = padOutline.getPathIterator(null);
    while (!pit.isDone()) {
      int segType = pit.currentSegment(coords);
      switch (segType) {
        case PathIterator.SEG_CLOSE:
          NormAndLen nl = padsForSegment(bubbleList, lastPt, firstPt, NetModuleProperties.NONE, startPads, endPads, nodeID);
          if (nl != null) {
            currDir = nl.vec;
            // fill joint for the next-to-last closed corner:
            calcJointPads(bubbleList, currDir, lastDir, startPads, lastEndPads, lastPt, cornerList, lastLen, forceLevel);
            // fill joint for the now closed corner:
            calcJointPads(bubbleList, firstDir, currDir, firstStartPads, endPads, firstPt, cornerList, nl.len, forceLevel);
          }
          startPads.clear();
          endPads.clear();
          break; 
        case PathIterator.SEG_LINETO:
          nextPt.setLocation(coords[0], coords[1]);         
          nl = padsForSegment(bubbleList, lastPt, nextPt, NetModuleProperties.NONE, startPads, endPads, nodeID);
          if (nl != null) {
            currDir = nl.vec;
            if (firstStartPads == null) {
              firstStartPads = new ArrayList<LinkPad>(startPads);
            } else {
              calcJointPads(bubbleList, currDir, lastDir, startPads, lastEndPads, lastPt, cornerList, lastLen, forceLevel);
            }
          }
          lastEndPads.clear();
          lastEndPads.addAll(endPads);
          startPads.clear();
          endPads.clear();
          if (firstDir == null) {
            firstDir = currDir;
          }
          lastDir = currDir;
          lastLen = (nl == null) ? -1.0 : nl.len;
          lastPt.setLocation(nextPt);
          break;
        case PathIterator.SEG_MOVETO:
          firstPt.setLocation(coords[0], coords[1]);
          lastPt.setLocation(firstPt);
          lastDir = null;
          firstStartPads = null;
          lastEndPads.clear();
          firstDir = null;
          break;
        case PathIterator.SEG_QUADTO:
        case PathIterator.SEG_CUBICTO:
        default:
          throw new IllegalStateException();
      }
     // int winding = pit.getWindingRule();
      pit.next();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Fill in our requirements for rows and columns
  */

  private void fillRowColNeeds(Area rectOutline, Rectangle bounds, SortedSet<Integer> needCols, SortedSet<Integer> needRows) {
    double[] coords = new double[6];
    Point2D movePt = new Point2D.Double(0.0, 0.0);
    Point2D startPt = new Point2D.Double(0.0, 0.0);
    Point2D endPt = new Point2D.Double(0.0, 0.0);
    PathIterator pit = rectOutline.getPathIterator(null);
    while (!pit.isDone()) {
      int segType = pit.currentSegment(coords);
      switch (segType) {
        case PathIterator.SEG_CLOSE:
          startPt.setLocation(coords[0], coords[1]);
          isNeeded(bounds, startPt, movePt, needCols, needRows);
          break; 
        case PathIterator.SEG_LINETO:
          endPt.setLocation(coords[0], coords[1]);
          isNeeded(bounds, startPt, endPt, needCols, needRows);
          startPt.setLocation(coords[0], coords[1]);
          break;
        case PathIterator.SEG_MOVETO:
          movePt.setLocation(coords[0], coords[1]);
          startPt.setLocation(coords[0], coords[1]);
          break;
        case PathIterator.SEG_QUADTO:
        case PathIterator.SEG_CUBICTO:
        default:
          throw new IllegalStateException();
      }  
      pit.next();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Figure out if the line segment specifies a needed x or y value.
  */

  private void isNeeded(Rectangle bounds, Point2D startPt, Point2D endPt, SortedSet<Integer> needCols, SortedSet<Integer> needRows) {
    
    double startX = startPt.getX();
    double startY = startPt.getY();
    double endX = endPt.getX();
    double endY = endPt.getY();
       
    if (bounds == null) {
      needCols.add(new Integer((int)startX / UiUtil.GRID_SIZE_INT));
      needCols.add(new Integer((int)endX / UiUtil.GRID_SIZE_INT));      
      needRows.add(new Integer((int)startY / UiUtil.GRID_SIZE_INT));
      needRows.add(new Integer((int)endY / UiUtil.GRID_SIZE_INT));      
      return;
    }
      
    boolean oneIn = false;
    if (bounds.contains(startX, startY)) {
      needCols.add(new Integer((int)startX / UiUtil.GRID_SIZE_INT));
      needRows.add(new Integer((int)startY / UiUtil.GRID_SIZE_INT));
      oneIn = true;
    }
    
    if (bounds.contains(endX, endY)) {
      needCols.add(new Integer((int)endX / UiUtil.GRID_SIZE_INT));      
      needRows.add(new Integer((int)endY / UiUtil.GRID_SIZE_INT));
      oneIn = true;
    }    
    
    //
    // Although the segment may not have endpoints in the bounds,
    // it can pass through the bounds.  Only care about canonical
    // direction:
    //
 
    if (!oneIn) {
      if (bounds.intersectsLine(startX, startY, endX, endY)) {
        if (startX == endX) {
          needCols.add(new Integer((int)startX / UiUtil.GRID_SIZE_INT));
        }
        if (startY == endY) {
          needRows.add(new Integer((int)startY / UiUtil.GRID_SIZE_INT));
        }   
      }
    }
    return;
  }
  
  
  
  /***************************************************************************
  **
  ** Fill in our requirements for rows and columns with a rigid rectangle
  */

  private void fillRectRowColNeeds(Rectangle2D rect, Rectangle bounds, SortedSet<Integer> needCols, SortedSet<Integer> needRows, boolean padPads) {
    
    //
    // If we are restricted by bounds, then only the part within the bounds changes:
    //
    
    if (bounds != null) {
      Rectangle2D interRect = rect.createIntersection(bounds);
      if ((interRect.getHeight() > 0.0) && (interRect.getWidth() > 0.0)) {
        rect = interRect;
      } else {
        return;
      }
    }
    
    // Ones are to cover pads:
    int pad = (padPads) ? 1 : 0;
    
    int minX = ((int)rect.getMinX() / UiUtil.GRID_SIZE_INT) - pad;
    int minY = ((int)rect.getMinY() / UiUtil.GRID_SIZE_INT) - pad;  
    int maxX = ((int)rect.getMaxX() / UiUtil.GRID_SIZE_INT) + pad;
    int maxY = ((int)rect.getMaxY() / UiUtil.GRID_SIZE_INT) + pad;    
    
    for (int x = minX; x <= maxX; x++) {
      needCols.add(new Integer(x));
    }
    
    for (int y = minY; y <= maxY; y++) {
      needRows.add(new Integer(y));
    }    
    return;
  }  
  
  /***************************************************************************
  **
  ** Generate the list of corners
  */

  private List<CornerInfo> buildCornerList(Area coreOutline) {
    double[] coords = new double[6];
    Point2D firstPt = new Point2D.Double(0.0, 0.0);
    Point2D lastPt = new Point2D.Double(0.0, 0.0);    
    Point2D nextPt = new Point2D.Double(0.0, 0.0);
    Vector2D firstDir = null;
    Vector2D lastDir = null;
    Vector2D currDir = null;

    ArrayList<CornerInfo> retval = new ArrayList<CornerInfo>();
    PathIterator pit = coreOutline.getPathIterator(null);
    while (!pit.isDone()) {
      int segType = pit.currentSegment(coords);
      switch (segType) {
        case PathIterator.SEG_CLOSE:
          currDir = new Vector2D(lastPt, firstPt);
          if (!lastDir.equals(currDir)) {
            retval.add(new CornerInfo((Point2D)lastPt.clone(), lastDir.normalized(), currDir.normalized()));
          }
          if (!firstDir.equals(currDir)) {
            retval.add(new CornerInfo((Point2D)firstPt.clone(), currDir.normalized(), firstDir.normalized()));
          }
          break; 
        case PathIterator.SEG_LINETO:
          nextPt.setLocation(coords[0], coords[1]);
          currDir = new Vector2D(lastPt, nextPt);
          if (firstDir == null) {
            firstDir = currDir.clone();
          } else if (!lastDir.equals(currDir)) { // Ignore "straight" corners
            retval.add(new CornerInfo((Point2D)lastPt.clone(), lastDir.normalized(), currDir.normalized()));
          }
          lastDir = currDir;
          lastPt.setLocation(nextPt);
          break;
        case PathIterator.SEG_MOVETO:
          firstPt.setLocation(coords[0], coords[1]);
          lastPt.setLocation(firstPt);
          lastDir = null;
          firstDir = null;
          break;
        case PathIterator.SEG_QUADTO:
        case PathIterator.SEG_CUBICTO:
        default:
          throw new IllegalStateException();
      }
     // int winding = pit.getWindingRule();
      pit.next();
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Figure out pads to use at a joint
  */

  private void calcJointPads(List<LinkPad> padList, Vector2D currDir, Vector2D lastDir, List<LinkPad> startPads, 
                             List<LinkPad> endPads, Point2D corner, List<CornerInfo> cornerList, double lastLen, int forceLevel) { 
    //
    // Straight-thru joints get all pads
    //
   
    if ((forceLevel == ALL_PADS_) || ((currDir != null) && (lastDir != null) && currDir.equals(lastDir))) {
      if (startPads != null) {
        padList.addAll(startPads);
      }
      if (!endPads.isEmpty()) {
        padList.add(endPads.get(0));
      }
    //
    // Need to figure out inner versus outer corner
    //
    } else if ((cornerList == null) || isOuterCorner(corner, lastDir, currDir, cornerList)) {
      if (forceLevel == ALL_BUT_CORNER_PADS_) {
        if ((startPads != null) && (startPads.size() > 1)) {
          padList.add(startPads.get(1));
        }
        if (!endPads.isEmpty() && (lastLen >= (PAD_SIZE * 3.0))) {
          padList.add(endPads.get(0));
        } 
      } else {
        tossShortSidePad(padList, startPads, endPads, true);
      }
    } else {
      if ((startPads != null) && (startPads.size() > 1)) {
        padList.add(startPads.get(1));
      }
      if (!endPads.isEmpty() && (lastLen >= (PAD_SIZE * 3.0))) {
        padList.add(endPads.get(0));
      } 
      tossShortSidePad(padList, startPads, endPads, false); 
    }
  
    return;
  }
  
  /***************************************************************************
  **
  ** For really short edges leading into or out of an inside corner, we may have
  ** added pads from the previous corner that the current corner does not allow.
  ** Check for these cases
  */

  private void tossShortSidePad(List<LinkPad> padList, List<LinkPad> startPads, List<LinkPad> endPads, boolean isOuter) {
        
    HashSet<Point2D> badPads = new HashSet<Point2D>();
    if (isOuter) {     
      if ((endPads != null) && (endPads.size() >= 1)) badPads.add(endPads.get(0).point);
      if (startPads != null) {
        if (startPads.size() > 0) badPads.add(startPads.get(0).point);
        if (startPads.size() > 1) badPads.add(startPads.get(1).point);
      }
    
    } else {
      if ((startPads != null) && (startPads.size() > 0)) {
        badPads.add(startPads.get(0).point);
      }
    }
    
    if (badPads.isEmpty()) {
      return;
    }
          
    int count = 0;
    while (count < padList.size()) {
      LinkPad lp = padList.get(count);
      Point2D padPt = lp.point;
      if (badPads.contains(padPt)) {
        padList.remove(count);
      } else {
        count++;
      }
    }
    
    return;
  }
    
  /***************************************************************************
  **
  ** Answer if the point is an outer (vs. an inner) corner.
  */

  private boolean isOuterCorner(Point2D padCornerPt, Vector2D lastDir, Vector2D currDir, List<CornerInfo> cornerList) {
    //
    // find matching corner
    //
    
    double cpx = padCornerPt.getX();
    double cpy = padCornerPt.getY();
    
    CornerInfo useCorner = null;
    int numC = cornerList.size();
    for (int i = 0; i < numC; i++) {
      CornerInfo testCorner = cornerList.get(i);
      double tcx = testCorner.point.getX();
      double tcy = testCorner.point.getY();
      if ((Math.abs(cpx - tcx) <= PAD_SIZE) && (Math.abs(cpy - tcy) <= PAD_SIZE)) {
        if (testCorner.inDir.equals(lastDir) && testCorner.outDir.equals(currDir)) {
          if (useCorner != null) {
            // This is a case of clustered corners.  Like below, lets make this conservative
            // and call it an outer corner:
            return (true);
          } else {
            useCorner = testCorner;
          }
        }
      }
    }
    
    //
    // In geometries where two underlying boxes are still distinct, but the pad geometry region
    // has merged, there won't be a match.  In that case, be conservative:
    //
    
    if (useCorner == null) {
      return (true); // This gives fewer pads, which is conservative...
    }
   
    // Figure it out now.  As we approach the corner on the inbound vector, and
    // if the true corner point is passed by before we hit the pad corner, we have an
    // ouside corner.
    
    double testLength = PAD_SIZE * 5.0;
    Point2D beforePad = lastDir.scaled(-testLength).add(padCornerPt);
    Vector2D toUse = new Vector2D(beforePad, useCorner.point);
    double dot = lastDir.dot(toUse);
    boolean isOut = (dot < testLength);
    return (isOut);
  }  
  
  /***************************************************************************
  **
  ** Generate pads along a segment
  */

  private NormAndLen padsForSegment(List<LinkPad> padList, Point2D start, Point2D end, int side, 
                                    List<LinkPad> startPads, List<LinkPad> endPads, String nodeID) {
     
    Vector2D vec = new Vector2D(start, end);
    if (!vec.isCanonical()) {
      return (null);
    }   
    Vector2D normal = vec.normal();
    
    double len = vec.length();
    Vector2D normRun = vec.scaled(1.0 / len);
    int count = (int)(len / PAD_SIZE) + 1;
    int countm1 = count - 1;
    int countm2 = count - 2;
    Vector2D normVec = vec.normalized();
    for (int i = 0; i < count; i++) {
      double factor = i * PAD_SIZE_INT;
      Point2D nextPoint = normVec.scaled(factor).add(start);
      LinkPad lp = new LinkPad(normal, normRun, nextPoint, side, nodeID);
      boolean notTerminal = true;
      if ((i == 0) || (i == 1)) {
        if (startPads != null) startPads.add(lp);
        notTerminal = false;
      }
      if ((i == countm1) || (i == countm2)) { 
        if (endPads != null) endPads.add(lp);
        notTerminal = false;
      }
      if (notTerminal) {
        padList.add(lp);
      }
    }
    return (new NormAndLen(normVec, len));
  }  
  
  /***************************************************************************
  **
  ** Generate bubble shapes for a list of pads
   * @param drawColor TODO
   * @param stroke TODO
  */

  private void bubbleShapesForPads(List<LinkPad> padList, List<ModelObjectCache.Ellipse> bubbleList, Color drawColor, BasicStroke stroke) {
    int numPads = padList.size();
    for (int i = 0; i < numPads; i++) {
      Point2D nextPoint = padList.get(i).point;
      // Ellipse2D circ = new Ellipse2D.Double(nextPoint.getX() - PAD_RADIUS, 
      //                                       nextPoint.getY() - PAD_RADIUS,
      //                                       PAD_SIZE, PAD_SIZE); 
      
      // TODO fix PAD_RADIUS <-> PAD_SIZE
      ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(nextPoint.getX(), nextPoint.getY(), PAD_RADIUS,
      		DrawMode.DRAW, drawColor, stroke);
      
      bubbleList.add(circ); 
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Return an intersected link pad point
  */

  private LinkPad intersectsPad(List<LinkPad> padList, Point2D clickPt, double pixDiam) {
    int numPads = padList.size();
    double radiusSq = PAD_RADIUS * PAD_RADIUS;
    double bigRadiusSq = (BIG_RADIUS < 2.0 * pixDiam) ? 4.0 * (pixDiam * pixDiam) : (BIG_RADIUS * BIG_RADIUS); 
    double clkX = clickPt.getX();
    double clkY = clickPt.getY();
    ArrayList<LinkPad> cand = null;
    for (int i = 0; i < numPads; i++) {
      LinkPad nextPad = padList.get(i);
      Point2D nextPoint = nextPad.point;
      double nextX = nextPoint.getX();
      double nextY = nextPoint.getY();
      double distSq = ((nextX - clkX) * (nextX - clkX)) + ((nextY - clkY) * (nextY - clkY));
      // If the match is that precise, we are cool with using it directly:
      if (distSq <= radiusSq) {
        return (nextPad);
      }
      if (distSq <= bigRadiusSq) {
        if (cand == null) {
          cand = new ArrayList<LinkPad>();
        }
        cand.add(nextPad);
      }
    }
    //
    // No super precise matches, but we do have some near matches.  Take the
    // best fit.  Use the one with the smallest horizontal offset, as long as
    // the point is not too far inside the wall or too far away.
    //
    if (cand != null) {
      double radiusForMin = (PAD_RADIUS < 2.0 * pixDiam) ? 6.0 * pixDiam : 3.0 * PAD_RADIUS; 
      double minOff = Double.POSITIVE_INFINITY;
      LinkPad currBest = null;
      int numCand = cand.size();
      for (int i = 0; i < numCand; i++) {
        LinkPad chkPad = cand.get(i);
        Vector2D toClick = new Vector2D(chkPad.point, clickPt);
        double perp = Math.abs(chkPad.getRun().dot(toClick));
        if (perp < minOff) {
          double off = chkPad.getNormal().dot(toClick);
          if ((off > -radiusForMin) && (off < radiusForMin)) {
            minOff = perp;
            currBest = chkPad;
          }
        }
      }
      if (currBest != null) {
        return (currBest);
      }
    }

    return (null);
  }     
 
  /***************************************************************************
  **
  ** This returns a map of pad locations generated by the given rectangle
  ** (Point2D->LinkPad)
  */

  public Map<Point2D, LinkPad> padsForRectangle(Rectangle2D shapeRect, String nodeID) {   
    HashMap<Point2D, LinkPad> retval = new HashMap<Point2D, LinkPad>();
    Rectangle paddedRect = padRect(UiUtil.rectFromRect2D(shapeRect));
    ArrayList<LinkPad> padList = new ArrayList<LinkPad>();
    Point2D topLeft = new Point2D.Double(paddedRect.getX(), paddedRect.getY());
    Point2D topRight = new Point2D.Double(paddedRect.getMaxX(), paddedRect.getY()); 
       
    padsForSegment(padList, topLeft, topRight, NetModuleProperties.TOP, null, null, nodeID);
    int numPads = padList.size();
    for (int i = 0; i < numPads; i++) {
      LinkPad pad = padList.get(i);
      retval.put(pad.point, pad);
    }
    
    padList.clear();
    Point2D bottomRight = new Point2D.Double(paddedRect.getMaxX(), paddedRect.getMaxY());
    padsForSegment(padList, topRight, bottomRight, NetModuleProperties.RIGHT, null, null, nodeID);
    numPads = padList.size();
    for (int i = 0; i < numPads; i++) {
      LinkPad pad = padList.get(i);
      retval.put(pad.point, pad);
    }
    
    padList.clear();
    Point2D bottomLeft = new Point2D.Double(paddedRect.getX(), paddedRect.getMaxY());    
    padsForSegment(padList, bottomRight, bottomLeft, NetModuleProperties.BOTTOM, null, null, nodeID);
    numPads = padList.size();
    for (int i = 0; i < numPads; i++) {
      LinkPad pad = padList.get(i);
      retval.put(pad.point, pad);
    }
 
    padList.clear();
    padsForSegment(padList, bottomLeft, topLeft, NetModuleProperties.LEFT, null, null, nodeID);
    numPads = padList.size();
    for (int i = 0; i < numPads; i++) {
      LinkPad pad = padList.get(i);
      retval.put(pad.point, pad);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Do assignment in two passes.  First handles points that don't have an
  ** associated "no point".  Second pass handles those cases with restrictions.
  */

  private void onePadPass(HashSet<LinkPad> padSet, HashMap<Layout.PointNoPoint, LinkPad> results, 
                          Map<Layout.PointNoPoint, LinkPad> wantPads, Map<Layout.PointNoPoint, Vector2D> rigidMoves, 
                          boolean isFirst, Map<Layout.PointNoPoint, LinkPad> currPads, boolean orphansOnly) { 
    
    //
    // padSet is the renderer's take on the current state of the module pads given the new geometry.
    //
    // 03/05/12
    //
    // So when going after orphans only, the situation is that the pad needs have been modified 
    // based on correctly transformed link drop ends, and the only problem is with pads that might
    // disappear due to rendering shifts for the module.
    // 
    // Note the want pads keys (and as of 3/5/12, with correct fix, currPads) is based on
    // ORIGINAL GEOMETRY, and we get answers that are closest to the ORIGINAL locations with
    // orphansOnly == false.  Sometimes this is OK, but with e.g. layout expansions, the links
    // are trying to get close to their original, pre-exapnd locations.  This can mean lots of links
    // sliding to corners for no good reason.
    //
    // Ideally, we should use out BEST SHOT at where we think the link has been moved to, and find
    // the best pad from _that_ location!  FIXME: Not there yet!
    //
 
    Map<Layout.PointNoPoint, LinkPad> padSource = (orphansOnly && (currPads != null)) ? currPads : wantPads;
    Iterator<Layout.PointNoPoint> wpit = padSource.keySet().iterator();
    while (wpit.hasNext()) {
      Layout.PointNoPoint pnp = wpit.next();
      if ((isFirst && (pnp.noPoint != null)) || (!isFirst && (pnp.noPoint == null))) {
        continue;      
      }
           
      //
      // With multi-rects, links originally attached to pad with a distinct node
      // ID may no longer have that ID around!
      
      HashSet<String> haveNodeIDs = new HashSet<String>();
      Iterator<LinkPad> pstit = padSet.iterator();
      while (pstit.hasNext()) {
        LinkPad nextPad = pstit.next();
        if (nextPad.nodeID != null) {
          haveNodeIDs.add(nextPad.nodeID);
        }
      }
      
      //
      // If we have current state and are only trying to fix orphan pads, use the
      // current pad if it matches
      // currPads: PointNoPoint->LinkPad
      //
      // Pad set is the set of pads just generated for the current geometry!
      // Curr pads reflects what the actual link ends expect given the transformation.
      // Orphans result when module geometry shifts to eliminate a pad where we
      // expect one to be!
      //
      // So this asks does current geometry support the pad we want for PNP, where 
      // the pad source is currPads, which is the cleanly transformed set
      // of original pads that are now in the new configuration:
      //
      
      LinkPad wantPad = padSource.get(pnp);
      if (orphansOnly && (currPads != null)) {  // i.e. padSource is currPads (see above)
        if (padSet.contains(wantPad)) {
          results.put(pnp, wantPad);
          continue;
        }
      }
      
      //
      // If we have rigid move info attached to the point, then shift
      // the pad to the rigidly moved location!
      //
      
      LinkPad usePad = wantPad;
      if (rigidMoves != null) {
        Vector2D rigidDelta = rigidMoves.get(pnp);
        if (rigidDelta != null) {
          Point2D rigidMove = rigidDelta.add(wantPad.point);
          usePad = wantPad.clone();
          usePad.point = rigidMove;
        }
      }
      
      //
      // If we have it, then use it.  Else, start looking!
      //
      
      if (padSet.contains(usePad)) {
        results.put(pnp, usePad);
      } else {
        LinkPad withoutNodeID = gotAMatchAfterLostNodeID(padSet, usePad, haveNodeIDs);
        if (withoutNodeID != null) {
          results.put(pnp, usePad);
        } else {
          LinkPad havePad = closestPad(padSet, pnp, usePad, results, haveNodeIDs);
          if (havePad != null) {
            results.put(pnp, havePad);
          }
        }
      }
    }
    return;   
  }   

  /***************************************************************************
  **
  ** Get a match, ignoring a lost node ID:
  */

  private LinkPad gotAMatchAfterLostNodeID(HashSet<LinkPad> padSet, LinkPad wantPad, Set<String> haveNodeIDs) {     
    if ((wantPad.nodeID != null) && !haveNodeIDs.contains(wantPad.nodeID)) {         
      Iterator<LinkPad> psit = padSet.iterator();
      while (psit.hasNext()) {
        LinkPad lp = psit.next();
        if (lp.nodeID != null) {
          lp = lp.clone();
          lp.nodeID = null;
          if (lp.equals(wantPad)) {
            return (wantPad);
          }
        }
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Get the closest pad
  */

  private LinkPad closestPad(Set<LinkPad> havePads, Layout.PointNoPoint pnp, 
                             LinkPad wantPad, Map<Layout.PointNoPoint, LinkPad> assigned, Set<String> haveNodeIDs) { 
    
    if (wantPad == null) {
      return (null);
    }
    
    LinkPad cantUse = null;
    if (pnp.noPoint != null) {
      Iterator<Layout.PointNoPoint> akit = assigned.keySet().iterator();
      while (akit.hasNext()) {
        Layout.PointNoPoint akey = akit.next();
        if (akey.point.equals(pnp.noPoint)) {
          cantUse = assigned.get(akey);
          break;
        }    
      }    
    }
 
    //
    // Find points that are projections along the normal, if possible:
    // Note that with multi-region modules, there might end up being
    // several candidates that match the need.  Find the closest!  
    // Note also that if we are required to target a particular node
    // region, we don't look at other options.
    //
     
    double minCandDistSq = Double.POSITIVE_INFINITY;    
    LinkPad bestPad = null;    

    Vector2D wantPadNorm = wantPad.getNormal();
    Iterator<LinkPad> hpit = havePads.iterator();
    while (hpit.hasNext()) {
      LinkPad havePad = hpit.next();
      if ((cantUse != null) && havePad.point.equals(cantUse.point)) {
        continue;
      }
 
      //
      // Don't toss out an option if the nodeID we require isn't there anymore.
      // Else, don't look at mismatches. 
      if (wantPad.nodeID != null) {
        if (haveNodeIDs.contains(wantPad.nodeID) && !wantPad.nodeID.equals(havePad.nodeID)) {
          continue;
        }
      }
      if (havePad.getNormal().equals(wantPadNorm)) {
        if (wantPadNorm.getX() == 0.0) {
          if (wantPad.point.getX() == havePad.point.getX()) {
            double distSq = wantPad.point.distanceSq(havePad.point);
            if (distSq < minCandDistSq) {
              bestPad = havePad;
              minCandDistSq = distSq;
            }
          }
        } else if (wantPadNorm.getY() == 0.0) {
          if (wantPad.point.getY() == havePad.point.getY()) {
            double distSq = wantPad.point.distanceSq(havePad.point);
            if (distSq < minCandDistSq) {
              bestPad = havePad;
              minCandDistSq = distSq;
            }
          }          
        }
      }
    } 
    
    if (bestPad != null) {
      return (bestPad);
    }  

    //
    // No luck, find the closest point with a matching normal, if possible
    //
    
    LinkPad lastDitchRetval = null;
    LinkPad retvalForNorm = null;
    
    double minDistSq = Double.POSITIVE_INFINITY;
    double minDistSqForNorm = Double.POSITIVE_INFINITY;

    hpit = havePads.iterator();
    while (hpit.hasNext()) {
      LinkPad havePad = hpit.next();
      if ((cantUse != null) && havePad.point.equals(cantUse.point)) {
        continue;
      }
      if (wantPad.nodeID != null) {
        if (haveNodeIDs.contains(wantPad.nodeID) && !wantPad.nodeID.equals(havePad.nodeID)) {
          continue;
        }
      }
      double distSq = wantPad.point.distanceSq(havePad.point);
      if (havePad.getNormal().equals(wantPadNorm)) {        
        if (distSq < minDistSqForNorm) {
          retvalForNorm = havePad;
          minDistSqForNorm = distSq;
        }
      } else {      
        if (distSq < minDistSq) {
          lastDitchRetval = havePad;
          minDistSq = distSq;
        }
      }
    }
 
    return ((retvalForNorm == null) ? lastDitchRetval : retvalForNorm);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  
  static class NormAndLen {  
    Vector2D vec;
    double len;
    
    NormAndLen(Vector2D vec, double len) {
      this.vec = vec;
      this.len = len;
    } 
  }  
  
  
  static class CornerInfo {  
    Vector2D inDir;
    Point2D point;
    Vector2D outDir;
    
    CornerInfo(Point2D point, Vector2D inDir, Vector2D outDir) {
      this.inDir = inDir;
      this.point = point;
      this.outDir = outDir;
    }
    
    public String toString() {
      return ("CornerInfo " + inDir + " " + point + " " + outDir);
    }   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  public static class LinkPad implements Cloneable {
  
    private Vector2D normal_;  // Not really, but since we are doing Euclidean ops, don't need dual vector!
    private Vector2D run_;  // perpendicular
    public Point2D point;
    public int rectSide;
    public String nodeID;
    
    LinkPad(Vector2D normal, Vector2D normRun, Point2D point, int rectSide, String nodeID) {
      normal_ = normal;
      run_ = normRun;
      this.point = point;
      this.rectSide = rectSide;
      this.nodeID = nodeID;      
    } 
    
    public String toString() {
      return ("LinkPad " + normal_ + " " + run_ + " " + point + " " + rectSide + " " + nodeID);
    } 

    // Normal is shared, read-only, and not deep copied on clone!
    public Vector2D getNormal() {
      return (normal_);
    } 
    // Same...
    public Vector2D getRun() {
      return (run_);
    }     
       
    public int hashCode() {
      return (normal_.hashCode() + run_.hashCode() + point.hashCode() + ((nodeID == null) ? 0 : nodeID.hashCode()) + rectSide);
    }

    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof LinkPad)) {
        return (false);
      }
      LinkPad otherLP = (LinkPad)other;
      
      if (!this.point.equals(otherLP.point)) {
        return (false);
      }       
      
      if (this.rectSide != otherLP.rectSide) {
        return (false);
      }
      
      if (!this.normal_.equals(otherLP.normal_)) {
        return (false);
      } 
       
      if (!this.run_.equals(otherLP.run_)) {
        return (false);
      }     
      
      if (this.nodeID == null) {
        return (otherLP.nodeID == null);
      }
      
      return (this.nodeID.equals(otherLP.nodeID));
    }
    
    public LinkPad clone() {
      try {
        return ((LinkPad)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
  }  

  public static class TaggedShape {
  
    public final static int FULL_DRAW_SHAPE    = 0;
    public final static int COMPONENT_SHAPE    = 1;
    public final static int AUTO_MEMBER_SHAPE  = 2;
    public final static int NON_MEMBER_SHAPE   = 3;
    public final static int TITLE_SHAPE        = 4;    
    
    public int shapeClass;
    public Shape shape;
    public Rectangle2D rect;
    public String memberID;
    
    TaggedShape(int shapeClass, Shape shape, Rectangle2D rect, String memberID) {
      this.shapeClass = shapeClass;
      this.rect = rect;
      this.shape = shape;
      this.memberID = memberID;
    } 
    
    public String toString() {
      return ("TaggedShape " + shapeClass + " " + rect + " " + shape + " " + memberID);
    }
    
  }    
  
  public static class CurrentSettings {
  
    public static final double INTERSECTION_CUTOFF = 0.90;
  
    public final static int NOTHING_MASKED     = 0;
    public final static int NON_MEMBERS_MASKED = 1;
    public final static int ALL_MASKED         = 2;
     
    public double regionLabelAlpha;
    public double regionFillAlpha;
    public double regionBoundaryAlpha;    
    public double backgroundOverlayAlpha;
    public int intersectionMask;
    public boolean fastDecayLabelVisible;
    
    public CurrentSettings() {
      regionLabelAlpha = 1.0;
      regionFillAlpha = 1.0;
      regionBoundaryAlpha = 1.0;  
      backgroundOverlayAlpha = 1.0;
      intersectionMask = ALL_MASKED;
      fastDecayLabelVisible = true;
    }  
    
    public CurrentSettings(int maskType) {
      this();
      if (maskType == NON_MEMBERS_MASKED) {
        throw new IllegalArgumentException();
      }
      if (maskType == NOTHING_MASKED) {
        regionLabelAlpha = 0.0;
        regionFillAlpha = 0.0;
        regionBoundaryAlpha = 0.0;  
        backgroundOverlayAlpha = 0.0;
        intersectionMask = NOTHING_MASKED;
        fastDecayLabelVisible = false;
      }
    }      
  }
  
  //
  // For when we intersect a shape and/or an edge or
  // corner of the shape:
  //
  
  public static class IntersectionExtraInfo {
    
    public final static int IS_BOUNDARY = 0;
    public final static int IS_INTERNAL = 1;
    public final static int IS_PAD      = 2;
     
    public Point2D startPt;
    public Point2D endPt;
    public Point2D intersectPt;
    public int type;
    public int outerMatch;
    public ContigBreakdown contigInfo;
    public TaggedShape directShape;
    public boolean nonDirectAlternative;
    public Vector2D padNorm;
    public boolean gotNameBounds;
    
    public IntersectionExtraInfo(Point2D startPt, Point2D endPt, Point2D intersectPt) {
      this.type = IS_BOUNDARY;
      this.startPt = startPt;
      this.endPt = endPt; 
      this.intersectPt = intersectPt;
      this.outerMatch = NetModuleProperties.NONE;
    }  
    
   public IntersectionExtraInfo(Point2D intersectPt) {
      this.type = IS_INTERNAL;
      this.intersectPt = intersectPt;
      this.outerMatch = NetModuleProperties.NONE;
    }
   
   public IntersectionExtraInfo(Point2D intersectPt, int type) {
      this.type = type;
      this.intersectPt = intersectPt;
      this.outerMatch = NetModuleProperties.NONE;
    } 
   
    public IntersectionExtraInfo(Point2D intersectPt, Vector2D norm) {
      this.type = IS_PAD;
      this.padNorm = norm;
      this.intersectPt = intersectPt;
      this.outerMatch = NetModuleProperties.NONE;
    } 
  }
  
  public static class ContigBreakdown {
    public Set<Rectangle2D> intersectedShape;
    public Map<String, Rectangle2D> memberShapes;
    
    public ContigBreakdown(Set<Rectangle2D> intersectedShape, Map<String, Rectangle2D> memberShapes) {
      this.intersectedShape = intersectedShape;
      this.memberShapes = memberShapes;
    }  
  }  
}
