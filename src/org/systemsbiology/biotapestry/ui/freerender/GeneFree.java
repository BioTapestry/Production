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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeInsertionDirective;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.NodeRenderBase;
import org.systemsbiology.biotapestry.ui.RenderObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.BoundShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.GeneCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a gene
*/

public class GeneFree extends NodeRenderBase {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final float LINE_THICK          = 5.0F; //= 3.0F;
  public static final double EXTRA_WIDTH_PER_PAD = 10.0; 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final float LINE_THIN_         = 3.0F;
  private static final float DIV_LINE_LEN_      = 2.0F * EvidenceGlyph.EVIDENCE_GLYPH_HEIGHT;  
  private static final float GENE_HEIGHT_       = 40.0F;
  private static final float GENE_WIDTH_        = 90.0F;
  private static final float GENE_DIAG_SQ_      = (GENE_HEIGHT_ * GENE_HEIGHT_) + (GENE_WIDTH_ * GENE_WIDTH_); 
  private static final float GENE_DIAG_         = (float)Math.sqrt(GENE_DIAG_SQ_);
  private static final float ARROW_HALF_HEIGHT_ = 10.0F;
  private static final float ARROW_LENGTH_      = 10.0F;  
  private static final float LINE_LENGTH_       = GENE_WIDTH_ - ARROW_LENGTH_;
  //private static final float BRANCH_HEIGHT_     = GENE_HEIGHT_ - ARROW_HALF_HEIGHT_;
  private static final float BRANCH_OFFSET_     = 5.0F;
  private static final float BRANCH_THICK_      = LINE_THICK - 1;
  private static final float PAD_WIDTH_         = 10.0F;
  private static final float PAD_HEIGHT_        = 10.0F;    
  private static final float SQUARED_TOSS_RADIUS_ = 35000.0F;
  private static final float BIG_TOSS_RADIUS_   = DBGene.MAX_PAD_COUNT * PAD_WIDTH_ * 1.25F;
  private static final float SQUARED_BIG_TOSS_RADIUS_ = BIG_TOSS_RADIUS_ * BIG_TOSS_RADIUS_;
  private static final float TEXT_PAD_          = 5.0F;
  private static final float REGION_HACK_       = 50.0F;   // FIX ME  
  private static final int MAX_PADS_            = DBGene.DEFAULT_PAD_COUNT;
  private static final int INBOUND_REGION_HEIGHT_LEGACY_ = 3;
  private static final int INBOUND_REGION_HEIGHT_ORTHO_ = 4; // Was 3; 4 allows ortho fixing to work
  private static final int INBOUND_REGION_WIDTH_ = 9;
  private static final float GLYPH_HACK_         = 1.0F;   
  
  
  // Needed to figure out what extra width increment is:
  
  public static final int SHORTIE_CHAR         = 3;
  public static final float DEFAULT_GENE_WIDTH = GENE_WIDTH_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //layout
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public GeneFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the Y offset needed to line up nodes with a through link in a straight line
  */
  
  @Override  
  public double getStraightThroughOffset() {
    return (-10.0);
  }
  
  /***************************************************************************
  **
  ** Get the X offset needed to line up nodes with centers aligned
  */
  
  @Override  
  public double getVerticalOffset() {
    return (-50.0);
  }
  
  /***************************************************************************
  **
  ** Get the preferred launch direction from the given pad
  */
  
  @Override  
  public Vector2D getDepartureDirection(int padNum, GenomeItem item, Layout layout) {
    NodeProperties np = layout.getNodeProperties(item.getID());
    int orient = np.getOrientation();
    if (orient == NodeProperties.RIGHT) {
      return (new Vector2D(1.0, 0.0));
    } else {
      return (new Vector2D(-1.0, 0.0));
    }
  }

  /***************************************************************************
  **
  ** Get the preferred arrival direction
  */
  
  @Override  
  public Vector2D getArrivalDirection(int padNum, GenomeItem item, Layout layout) {
    return (new Vector2D(0.0, 1.0));
  }
  
 /***************************************************************************
  **
  ** Answer if landing pads can overflow (e.g. be assigned negative values)
  */
  
  @Override  
  public boolean landingPadsCanOverflow() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if landing and launching pad namespaces are shared
  */

  @Override
  public boolean sharedPadNamespaces(){
    return (false);
  }

  /***************************************************************************
  **
  ** Return the Rectangle bounding the area where we do not want to allow network
  ** expansion.  May be null.
  */
  
  @Override
  public Rectangle getNonExpansionRegion(GenomeItem item, DataAccessContext rcx) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double minX = origin.getX();
    double minY = origin.getY();
    double maxX = minX;
    double maxY = minY;
    
    //
    // Disallow any expansion between occupied launch and landing pads
    //
    
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    String myId = item.getID();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      // This introduces diagonals on recompression following link additions.
      if (rcx.getLayout().getLinkProperties(link.getID()) == null) {  // can use on incomplete layouts...
        continue;
      }
      String src = link.getSource();
      if (src.equals(myId)) {
        Vector2D lpo = getLaunchPadOffset(link.getLaunchPad(), item, rcx);
        Point2D lPad = lpo.add(origin);
        double padX = lPad.getX();
        double padY = lPad.getY();
        if (padX > maxX) maxX = padX;
        if (padY > maxY) maxY = padY;
        if (padX < minX) minX = padX;
        if (padY < minY) minY = padY;        
      }
      String trg = link.getTarget();
      if (trg.equals(myId)) {
        Vector2D lpo = getLandingPadOffset(link.getLandingPad(), item, link.getSign(), rcx);
        Point2D lPad = lpo.add(origin);
        double padX = lPad.getX();
        double padY = lPad.getY();
        if (padX > maxX) maxX = padX;
        if (padY > maxY) maxY = padY;
        if (padX < minX) minX = padX;
        if (padY < minY) minY = padY;        
      }
    }
    if ((maxX == minX) && (maxY == minY)) {
      return (null);
    }
    return (new Rectangle((int)minX, (int)minY, (int)maxX - (int)minX, (int)maxY - (int)minY));
  }  

  /***************************************************************************
  **
  ** Render the gene at the designated location
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
    AnnotatedFont mFontAnn = rcx.fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride());
        
    //
    // Figure out the length of the gene line:
    //
    
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = extraPads * PAD_WIDTH_;    
   
    //
    // Modify the ghosted state to include inactive genes:
    //

    boolean isGhosted = rcx.isGhosted();
    boolean textGhosted = isGhosted;
    if (item instanceof GeneInstance) {
      int activityLevel = ((GeneInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == GeneInstance.VESTIGIAL) || (activityLevel == GeneInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != GeneInstance.VESTIGIAL);
    }
    Color vac = getVariableActivityColor(item, np.getColor(), false, rcx.getDisplayOptsSource().getDisplayOptions());
    Color col = (isGhosted) ? Color.LIGHT_GRAY : vac;
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, rcx.getDisplayOptsSource().getDisplayOptions());    
    
    int orient = np.getOrientation();   
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    
    AffineTransform trans = new AffineTransform();
    //AffineTransform saveTrans = g2.getTransform();
    if (orient == NodeProperties.LEFT) {
      trans.translate(x + GENE_WIDTH_, 0.0);
      trans.scale(-1.0, 1.0);
      trans.translate(-x, 0.0);
    }
    
    GeneCacheGroup group = new GeneCacheGroup(item.getID(), item.getName(), "gene", getLaunchPadOffset(0, item, rcx));

    ModelObjectCache.PushTransformOperation pushTrans = new ModelObjectCache.PushTransformOperation(trans);
    group.addShape(pushTrans, majorLayer, minorLayer);
    
    // In the web application, push the same transformation to the selection shape stream,
    // as the selection shapes are rendered separately (in a different call) in the web renderer.
    if (rcx.forWeb) {
        group.addShapeSelected(pushTrans, majorLayer, minorLayer);
    }
    
    selectionSupport(group, selected, (int)(x - extraLen), (int)y, (int)(GENE_WIDTH_ + extraLen), (int)GENE_HEIGHT_, rcx.forWeb);
    
    //
    // Draw the branch
    //
    
    GeneralPath branchPath = new GeneralPath(); 
    branchPath.moveTo(x + LINE_LENGTH_ - BRANCH_OFFSET_, y + GENE_HEIGHT_);
    branchPath.lineTo(x + LINE_LENGTH_ - BRANCH_OFFSET_, y + ARROW_HALF_HEIGHT_);
    branchPath.lineTo(x + GENE_WIDTH_ - ARROW_LENGTH_ + 2, y + ARROW_HALF_HEIGHT_);    
    
    BasicStroke branchDrawStroke = new BasicStroke(BRANCH_THICK_, BasicStroke.CAP_BUTT, 
        BasicStroke.JOIN_ROUND);
    ModelObjectCache.SegmentedPathShape branchPathShape = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, branchDrawStroke, branchPath);
    group.addShape(branchPathShape, majorLayer, minorLayer);
    
    //
    // Draw the baseline:
    //
    
    BasicStroke baselineStroke = new BasicStroke(LINE_THICK, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    
    ModelObjectCache.Line line1 = new Line(x - extraLen, y + GENE_HEIGHT_, 
        x + LINE_LENGTH_, y + GENE_HEIGHT_,
        DrawMode.DRAW, col, baselineStroke);
    
    group.addShape(line1, majorLayer, minorLayer);
    
    //
    // Draw the arrow:
    //
    
    GeneralPath arrowPath = new GeneralPath();
    arrowPath.moveTo(x + GENE_WIDTH_ - ARROW_LENGTH_, y);    
    arrowPath.lineTo(x + GENE_WIDTH_, y + ARROW_HALF_HEIGHT_);
    arrowPath.lineTo(x + GENE_WIDTH_ - ARROW_LENGTH_, y + (2.0F * ARROW_HALF_HEIGHT_));
    arrowPath.closePath();
    
    // TODO correct stroke
    ModelObjectCache.SegmentedPathShape arrowPathShape = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, col, new BasicStroke(0), arrowPath);
    group.addShape(arrowPathShape, majorLayer, minorLayer);
    
    //
    // Draw the evidence glyph:
    //
    
    renderEvidenceGlyphForGene(group, (Gene)item, origin, isGhosted);  
    
    ModelObjectCache.PopTransformOperation popTrans = new ModelObjectCache.PopTransformOperation();
    group.addShape(popTrans, majorLayer, minorLayer);
 
    // In the web application, push the same pop transformation operation to the selection shape stream,
    // as the selection shapes are rendered separately (in a different call) in the web renderer.
    if (rcx.forWeb) {
        group.addShapeSelected(popTrans, majorLayer, minorLayer);
    }
    
    if (rcx.showBubbles || rcx.forWeb) {
      renderPads(group, (isGhosted) ? Color.LIGHT_GRAY : Color.BLACK, item, rcx);
    }    
         
    textCol = (textGhosted) ? Color.LIGHT_GRAY : textCol;

    //
    // Draw region labels:
    // 
    
    Iterator<DBGeneRegion> rit = ((Gene)item).regionIterator();
    boolean hasARegion = rit.hasNext();
    if (hasARegion) {
      Font rFont = rcx.fmgr.getFont(FontManager.MODULE_NAME);
      // g2.setFont(rFont);
      BasicStroke divStroke = new BasicStroke(LINE_THIN_, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
      // g2.setStroke(divStroke);
      DBGeneRegion lastRegion = null;
      while (rit.hasNext()) {
        DBGeneRegion region = rit.next();
        String label = region.getName();
        int startPad = region.getStartPad();
        int endPad = region.getEndPad();
        int midPad = (startPad + endPad) / 2;
        Vector2D off = getLandingPadOffset(midPad, item, Linkage.NONE, rcx);      
        float xLabel = x + (float)off.getX();
        float yLabel = y + (float)off.getY();
        Rectangle2D bounds = rFont.getStringBounds(label, rcx.getFrc());
        float width = (float)bounds.getWidth() / 2.0F;
        
        ModalShape ts = textFactory.buildTextShape(label, mFontAnn, textCol, xLabel - width, (float)(yLabel + DIV_LINE_LEN_ + bounds.getHeight()), new AffineTransform());
        
        group.addShape(ts, majorLayer, minorLayer);        
        
        //
        // Draw the bars separating regions
        //
        if (lastRegion != null) {
          //BAR ON RIGHT SIDE IF REGION IS SHORT - CENTERING ISSUES?  FIX ME
          int lrEndPad = lastRegion.getEndPad();
          Vector2D divOffL = getLandingPadOffset(lrEndPad, item, Linkage.NONE, rcx);
          Vector2D divOffR = getLandingPadOffset(startPad, item, Linkage.NONE, rcx);
          float divY = y + GENE_HEIGHT_ + (float)(LINE_THICK / 2.0);
          float divX = x + (float)((divOffL.getX() + divOffR.getX()) / 2.0);
          
          // Line2D divLine = new Line2D.Float(divX, divY, divX, divY + DIV_LINE_LEN_);
          // g2.setPaint(textCol);// col); 
          // g2.draw(divLine);
          
          ModelObjectCache.Line divLine = new Line(divX, divY, divX, divY + DIV_LINE_LEN_,
              DrawMode.DRAW, textCol, divStroke);
          
          group.addShape(divLine, majorLayer, minorLayer);
        }

        renderEvidenceGlyphForRegion(group, (Gene)item, region, origin, isGhosted, rcx);
        lastRegion = region;
        // g2.setPaint(textCol);        
      }
    }
    
    // Good for debug:
    //Rectangle2D rect4Reg = regionBounds(item, origin, layout, frc);
    //if (rect4Reg != null) {
    //  g2.draw(rect4Reg);
    //}
    
    //
    // Draw the gene name:
    //   
    
    String name = item.getName();
    Point2D nStart = getNameStart(item, name, mFontAnn.getFont(), rcx.getFrc(), orient, hasARegion, origin, np.getHideName(), np.getLineBreakDef(), rcx.fmgr);
    // Good for debug:g2.draw(new Rectangle2D.Double(nStart.getX() - 5.0, nStart.getY() - 5.0, 10.0, 10.0));
    renderText(group, (float)nStart.getX(), (float)nStart.getY(), name, textCol, np.getHideName(), mFontAnn, np.getLineBreakDef(), rcx.getFrc(), rcx.fmgr, textFactory);
    
    
    // Good for debug:g2.draw(getTextBounds(frc, (float)nStart.getX(), (float)nStart.getY(), name, np.getHideName(), mFont, np.getLineBreakDef()));
    
    //
    // Draw activity pie:
    //
    
    Vector2D pieOff = getLaunchPadOffset(0, item, rcx); 
    double xOff = (orient == NodeProperties.LEFT) ? -5.0 : 5.0;
    Point2D pieCenter = new Point2D.Double(x + pieOff.getX() + xOff, y + pieOff.getY() + 20.0);
    drawVariableActivityPie(group, item, col, pieCenter, rcx.getDisplayOptsSource().getDisplayOptions());    
    
    setGroupBounds(group, rcx.getGenome(), item, rcx.getLayout(), rcx.getFrc(), rcx.fmgr);
    
    cache.addGroup(group);
  }
  
  // TODO remove old implementation
  public void renderOld(Graphics2D g2, RenderObjectCache cache, GenomeItem item, 
                    Intersection selected, 
                    DataAccessContext rcx,                   
                     Object miscInfo, ModelObjectCache moc) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    Font mFont = rcx.fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride()).getFont();
        
    //
    // Figure out the length of the gene line:
    //
    
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = (float)extraPads * PAD_WIDTH_;    
   
    //
    // Modify the ghosted state to include inactive genes:
    //
    
    boolean isGhosted = rcx.isGhosted();
    boolean textGhosted = isGhosted;
    if (item instanceof GeneInstance) {
      int activityLevel = ((GeneInstance)item).getActivity();
      isGhosted = isGhosted || (activityLevel == GeneInstance.VESTIGIAL) || (activityLevel == GeneInstance.INACTIVE);
      textGhosted = isGhosted && (activityLevel != GeneInstance.VESTIGIAL);
    }
    Color vac = getVariableActivityColor(item, np.getColor(), false, rcx.getDisplayOptsSource().getDisplayOptions());
    Color col = (isGhosted) ? Color.LIGHT_GRAY : vac;
    Color textCol = getVariableActivityColor(item, Color.BLACK, true, rcx.getDisplayOptsSource().getDisplayOptions());        

    int orient = np.getOrientation();   
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    
    AffineTransform trans = new AffineTransform();
    AffineTransform saveTrans = g2.getTransform();
    if (orient == NodeProperties.LEFT) {
      trans.translate(x + GENE_WIDTH_, 0.0);
      trans.scale(-1.0, 1.0);
      trans.translate(-x, 0.0);
    }
    
    g2.transform(trans);
    
    // selectionSupport(g2, cache, selected, (int)(x - extraLen), (int)y, (int)(GENE_WIDTH_ + extraLen), (int)GENE_HEIGHT_);
       
    g2.setPaint(col);        
    g2.setStroke(new BasicStroke(BRANCH_THICK_, BasicStroke.CAP_BUTT, 
                                 BasicStroke.JOIN_ROUND));
   
    //
    // Draw the branch
    //
    
    GeneralPath path = new GeneralPath(); 
    path.moveTo(x + LINE_LENGTH_ - BRANCH_OFFSET_, y + GENE_HEIGHT_);
    path.lineTo(x + LINE_LENGTH_ - BRANCH_OFFSET_, y + ARROW_HALF_HEIGHT_);
    path.lineTo(x + GENE_WIDTH_ - ARROW_LENGTH_ + 2, y + ARROW_HALF_HEIGHT_);    
    g2.draw(path);
    
    //
    // Draw the baseline:
    //
    
    g2.setStroke(new BasicStroke(LINE_THICK, BasicStroke.CAP_BUTT, 
                                 BasicStroke.JOIN_ROUND));
       
    Line2D line = new Line2D.Float(x - extraLen, y + GENE_HEIGHT_, 
                                   x + LINE_LENGTH_, y + GENE_HEIGHT_);
    g2.draw(line);
    
    //
    // Draw the arrow:
    //
    
    path.reset();
    path.moveTo(x + GENE_WIDTH_ - ARROW_LENGTH_, y);    
    path.lineTo(x + GENE_WIDTH_, y + ARROW_HALF_HEIGHT_);
    path.lineTo(x + GENE_WIDTH_ - ARROW_LENGTH_, y + (2.0F * ARROW_HALF_HEIGHT_));
    path.closePath();
    g2.fill(path);
    
    //
    // Draw the evidence glyph:
    //
    
    // renderEvidenceGlyphForGene(g2, (Gene)item, origin, isGhosted);  
    
   
    g2.setTransform(saveTrans);
    
    if (rcx.showBubbles) {
      // renderPads(group, (isGhosted) ? Color.LIGHT_GRAY : Color.BLACK, item, layout, frc);
    }    
         
    textCol = (textGhosted) ? Color.LIGHT_GRAY : textCol;    
    g2.setPaint(textCol);    
    
    //
    // Draw region labels:
    // 
    
    Iterator<DBGeneRegion> rit = ((Gene)item).regionIterator();
    boolean hasARegion = rit.hasNext();
    if (hasARegion) {
      Font rFont = rcx.fmgr.getFont(FontManager.MODULE_NAME);
      g2.setFont(rFont);
      BasicStroke divStroke = new BasicStroke(LINE_THIN_, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
      g2.setStroke(divStroke);
      DBGeneRegion lastRegion = null;
      while (rit.hasNext()) {
        DBGeneRegion region = rit.next();
        String label = region.getName();
        int startPad = region.getStartPad();
        int endPad = region.getEndPad();
        int midPad = (startPad + endPad) / 2;
        Vector2D off = getLandingPadOffset(midPad, item, Linkage.NONE, rcx);      
        float xLabel = x + (float)off.getX();
        float yLabel = y + (float)off.getY();
        Rectangle2D bounds = rFont.getStringBounds(label, rcx.getFrc());
        float width = (float)bounds.getWidth() / 2.0F;      
        g2.drawString(label, xLabel - width, (float)(yLabel + DIV_LINE_LEN_ + bounds.getHeight()));
        //
        // Draw the bars separating regions
        //
        if (lastRegion != null) {
          //BAR ON RIGHT SIDE IF REGION IS SHORT - CENTERING ISSUES?  FIX ME
          int lrEndPad = lastRegion.getEndPad();
          Vector2D divOffL = getLandingPadOffset(lrEndPad, item, Linkage.NONE, rcx);
          Vector2D divOffR = getLandingPadOffset(startPad, item, Linkage.NONE, rcx);
          float divY = y + GENE_HEIGHT_ + (float)(LINE_THICK / 2.0);
          float divX = x + (float)((divOffL.getX() + divOffR.getX()) / 2.0);
          Line2D divLine = new Line2D.Float(divX, divY, divX, divY + DIV_LINE_LEN_);
          g2.setPaint(textCol);// col); 
          g2.draw(divLine);
        }
        // renderEvidenceGlyphForRegion(g2, (Gene)item, layout, frc, region, origin, isGhosted);
        lastRegion = region;
        g2.setPaint(textCol);        
      }
    }
    
    // Good for debug:
    //Rectangle2D rect4Reg = regionBounds(item, origin, layout, frc);
    //if (rect4Reg != null) {
    //  g2.draw(rect4Reg);
    //}
    
    //
    // Draw the gene name:
    //   
    
    String name = item.getName();
    Point2D nStart = getNameStart(item, name, mFont, rcx.getFrc(), orient, hasARegion, origin, np.getHideName(), np.getLineBreakDef(), rcx.fmgr);
    // Good for debug:g2.draw(new Rectangle2D.Double(nStart.getX() - 5.0, nStart.getY() - 5.0, 10.0, 10.0));
    //renderText(g2, cache, (float)nStart.getX(), (float)nStart.getY(), name, np.getHideName(), mFont, np.getLineBreakDef());
    // Good for debug:g2.draw(getTextBounds(frc, (float)nStart.getX(), (float)nStart.getY(), name, np.getHideName(), mFont, np.getLineBreakDef()));
    
    //
    // Draw activity pie:
    //
    
    Vector2D pieOff = getLaunchPadOffset(0, item, rcx); 
    double xOff = (orient == NodeProperties.LEFT) ? -5.0 : 5.0;
    Point2D pieCenter = new Point2D.Double(x + pieOff.getX() + xOff, y + pieOff.getY() + 20.0);
    drawVariableActivityPie(null, item, col, pieCenter, rcx.getDisplayOptsSource().getDisplayOptions());    
      
    return;
  }

 /***************************************************************************
  **
  ** Figure out the name string start
  */ 
  
  private Point2D getNameStart(GenomeItem item, String name, Font mFont, FontRenderContext frc,
                               int orient, boolean hasARegion, 
                               Point2D origin, boolean hideName, String lineBreakDef, FontManager fmgr) {  

    double orientPad = (orient == NodeProperties.RIGHT) ? 0.0 : (double)ARROW_LENGTH_;        
    Dimension flDim = firstLineBounds(frc, name, hideName, mFont, lineBreakDef, fmgr);
    double xoff = origin.getX() + LINE_LENGTH_ - flDim.getWidth() + orientPad;
   
    //
    // If we have regions, and a short name, just push the name over to the right.
    // With a long name, we need to move the name down below the region tags:
    //
    
    int regionExtraHeight = 0;
    float textPad = TEXT_PAD_;
    boolean isTiny = (name.length() <= SHORTIE_CHAR) && isSingleLineText(name, hideName, lineBreakDef);   
    if (hasARegion) {
      if (isTiny) {
        xoff += REGION_HACK_;
      } else {
        Rectangle2D rect4Reg = regionBounds(item, origin, orient, frc, fmgr);
        regionExtraHeight = (int)rect4Reg.getHeight();
        textPad = 0;
      }
    }
    double yoff = (double)(origin.getY() + GENE_HEIGHT_ + flDim.height + regionExtraHeight + textPad);
    return (new Point2D.Double(xoff, yoff));
  }
  
  /***************************************************************************
  **
  ** Render the node to a placement grid
  */
  
  @Override
  public void renderToPlacementGrid(GenomeItem item, 
                                    DataAccessContext rcx, 
                                    LinkPlacementGrid grid, Map<String, Integer> targetCounts, 
                                    Map<String, Integer> minPads, int strictness) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    int orient = np.getOrientation();
    
    //
    // Gotta know if we are having link overflow, even if the gene has been expanded:
    //

    boolean notATarget = true;
    int padCount = ((Gene)item).getPadCount();
    int trueCount = padCount;
    if (minPads != null) {
      Integer min = minPads.get(item.getID());
      if (min != null) {
        notATarget = false;
        int minPad = min.intValue();
        if ((minPad < 0) && (minPad < (DBGene.DEFAULT_PAD_COUNT - padCount))) {
          trueCount = DBGene.DEFAULT_PAD_COUNT - minPad;
        }
      }
    }
    int overflow = trueCount - padCount;
    
    //
    // Figure out rendered length:
    //
    
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = (float)extraPads * PAD_WIDTH_;         
 
    //
    // Bounds is kinds bogus for width (eg includes long gene names). 
    // So make a custom width calc that includes overflow pads.
    //
    
    Rectangle rect = getBounds(item, rcx, null);
    rect.width = (int)(GENE_WIDTH_ + extraLen) + (overflow * 10);
    int shift = (orient == NodeProperties.RIGHT) ? (int)(extraLen + (overflow * PAD_WIDTH_)) : 0;
    rect.x = (int)np.getLocation().getX() - shift;
    
    Point2D forcedUL = new Point2D.Double();
    UiUtil.forceToGrid(rect.x, rect.y, forcedUL, 10.0);
    Point2D forcedLR = new Point2D.Double();
    UiUtil.forceToGrid(rect.x + rect.width, rect.y + rect.height, forcedLR, 10.0);
    // Off centering requires us to pad more on the left and bottom
    // WJRL 5/1/08 Why 30??? Seems to be a magic number...
    // Maybe for keeping links some distance away from gene name??
    int patW = (((int)(forcedLR.getX() - forcedUL.getX())) + 30) / 10;
    int patH = (((int)(forcedLR.getY() - forcedUL.getY())) + 30) / 10;
    
    if (strictness == MODULE_PADDED) {
      patW += 2;
      patH += 2;
    }
    
 
    Pattern pat = new Pattern(patW, patH);
    String itemID = item.getID();
    if ((strictness == STRICT) || (strictness == MODULE_PADDED)) {
      pat.fillAll(itemID);
    } else {
      // If lax, trim around the edges...
      for (int i = 1; i < patW - 1; i++) {
        for (int j = 1; j < patH - 1; j++) {
          pat.fill(i, j, itemID);
        }
      }
    }

    //
    // Note how the inbound pattern is a direct overlay of overall gene (right?)
    // If we are strict, nobody is allowed into our inbound region.
    //
    
    Pattern inbound = new Pattern(patW, patH);
    int trueWidth = INBOUND_REGION_WIDTH_ + (padCount - MAX_PADS_) + overflow;
    
    if ((strictness == STRICT) || (strictness == MODULE_PADDED)) {
      int startInbound = (orient == NodeProperties.RIGHT) ? 0 : patW - trueWidth;   
      int endInbound = (orient == NodeProperties.RIGHT) ? trueWidth : patW;    
      for (int i = startInbound; i < endInbound; i++) {
        for (int j = 0; j < INBOUND_REGION_HEIGHT_LEGACY_; j++) {
          inbound.fill(i, j, item.getID());
        }
      } 
    
    } else {
    //
    // If we are lax, we only reserve the area we actually use
    //
      int startInbound = (orient == NodeProperties.RIGHT) ? 1 : patW - trueWidth - 1; 
      // 6/10/08 This was originally trueWidth - 1, but didn't correctly cover the
      // rightmost pad right next to the gene arrow.  The above value was unchanged;
      // left-facing genes appear to work OK:
      int endInbound = (orient == NodeProperties.RIGHT) ? trueWidth : patW - 1;    
      int minPad = Integer.MAX_VALUE;    
      if (!notATarget) {
        String myID = item.getID();
        Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
        while (lit.hasNext()) {
          Linkage link = lit.next();
          String targ = link.getTarget();
          if (!targ.equals(myID)) {
            continue;
          }
          int pad = link.getLandingPad();
          if (pad < minPad) {
            minPad = pad;
          }
        }
      }
      
      
      int transition = startInbound;
      if (!notATarget) {
         // there is a pad of 3 on the right (is that the magic number 30 up above?) 
        transition = (orient == NodeProperties.RIGHT) ? minPad + 3 + (padCount - MAX_PADS_) + overflow 
                                                      : endInbound - minPad - 2 - (padCount - MAX_PADS_) - overflow;
      }       
      int useHeight;
      //
      // Turns out the legacy lax height causes ortho fixes to fail.
      // Use an increased height; investigate if the legacy value
      // is crucial at the lower value at a future date:
      if (strictness == LAX) {
        useHeight = INBOUND_REGION_HEIGHT_LEGACY_;
      } else if (strictness == LAX_FOR_ORTHO) {
        useHeight = INBOUND_REGION_HEIGHT_ORTHO_;
      } else {
        throw new IllegalArgumentException();
      }
      for (int i = startInbound; i < endInbound; i++) {
        for (int j = 0; j < useHeight; j++) {
          if (notATarget) {
            pat.fill(i, j, null);
          } else if (i < transition) {
            if (orient == NodeProperties.RIGHT) {
              pat.fill(i, j, null);
            } else {
              inbound.fill(i, j, item.getID());
            }
          } else {
            if (orient == NodeProperties.RIGHT) {             
              inbound.fill(i, j, item.getID());            
            } else {
              pat.fill(i, j, null);
            }
          }
        }
      } 
    }
 
    int px = ((int)forcedUL.getX() - 10) / 10;
    int py = ((int)forcedUL.getY() - 10) / 10;
    if (strictness == MODULE_PADDED) {
      px--;
      py--;
    }
    
    grid.addNode(pat, inbound, item.getID(), px, py, strictness);
    return;
  }

  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double orx = origin.getX();
    double ory = origin.getY();
    double x = pt.getX();
    double y = pt.getY();
    double dx = orx - x;
    double dy = ory - y;
    double distsq = (dx * dx) + (dy * dy);
    // Quick kill:
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    float sqRad;
    if (defaultLength) {
      sqRad = SQUARED_TOSS_RADIUS_;
    } else if (padCount <= DBGene.MAX_PAD_COUNT) {
      sqRad = SQUARED_BIG_TOSS_RADIUS_;
    } else {
      float rad = padCount * PAD_WIDTH_ * 1.25F;     
      sqRad = rad * rad;
    }
    
    String name = item.getName();
    String breakDef = np.getLineBreakDef();
    if ((distsq > sqRad) && !isTextCandidate(rcx.getFrc(), FontManager.GENE_NAME, np.getFontOverride(), 
                                             name, GENE_DIAG_, dx, distsq, breakDef, true, false, rcx.fmgr)) {
      return (null);
    }
    
    int orient = np.getOrientation();      

    //
    // Figure out the length of the gene line:
    //

    int extraPads = (defaultLength) ? 0 : padCount - DBGene.DEFAULT_PAD_COUNT;     
    float extraLen = (float)extraPads * PAD_WIDTH_;
    float lineLen = (defaultLength) ? LINE_LENGTH_ : LINE_LENGTH_ + extraLen; 
    
    //
    // Mask the upper left / right  corner of the gene:
    //
   
    double maskX = (orient == NodeProperties.RIGHT) ? 
                     orx - 2 - extraLen: 
                     (orx - 2) + ARROW_LENGTH_ + (3.0 * BRANCH_OFFSET_);
    double maskY = ory - 2;
    double maskW = (orient == NodeProperties.RIGHT) ? 
                     lineLen - (2.0 * BRANCH_OFFSET_) + 2 :
                     lineLen - (3.0 * BRANCH_OFFSET_) + 2;
    double maskH = GENE_HEIGHT_ - (PAD_WIDTH_ + PAD_HEIGHT_); 
    if (Bounds.intersects(maskX, maskY, maskX + maskW, maskY + maskH, x, y)) {
      return (null);
    }
    
    double minX = (orient == NodeProperties.RIGHT) ? orx - extraLen : orx - PAD_WIDTH_;
    double minY = ory - PAD_WIDTH_;
    double maxX = (orient == NodeProperties.RIGHT) ? 
                    orx + GENE_WIDTH_ + PAD_WIDTH_ :
                    orx + GENE_WIDTH_ + extraLen; // minX + GENE_WIDTH_ + PAD_WIDTH_;
    double maxY = minY + GENE_HEIGHT_ + PAD_WIDTH_ + (LINE_THICK / 2.0F);

    if ((x >= minX) && (x <= maxX) && (y >= minY) && (y <= maxY)) {
      List<Intersection.PadVal> pads = calcPadIntersects(item, pt, rcx);
      if (pads != null) {
        return (new Intersection(item.getID(), pads));
      } else { // did not hit pad, need to narrow down hit range, since pads extend out
        minY += PAD_WIDTH_;
        minX += (orient == NodeProperties.RIGHT) ? 0 : PAD_WIDTH_;
        maxX -= (orient == NodeProperties.RIGHT) ? PAD_WIDTH_ : 0;
        if ((x >= minX) && (x <= maxX) && (y >= minY) && (y <= maxY)) {
          return (new Intersection(item.getID(), null, 0.0));
        }
      }
    }
    
    //
    // No hit on gene glyph.  If we have regions, try to see if we hit those:
    //
       
    Rectangle2D regBounds = regionBounds(item, origin, orient, rcx.getFrc(), rcx.fmgr);
    boolean hasARegion = false;
    if (regBounds != null) {
      hasARegion = true;
      if (regBounds.contains(x, y)) {
        return (new Intersection(item.getID(), null, 0.0));
      }
    }
    
    //
    // Still no hit.  Try to intersect the gene label
    //
    
    AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride());
   
    Point2D nStart = getNameStart(item, name, amFont.getFont(), rcx.getFrc(), orient, hasARegion, origin, np.getHideName(), breakDef, rcx.fmgr);
    Rectangle2D textRect = getTextBounds(rcx.getFrc(), (float)nStart.getX(), (float)nStart.getY(), name, false, amFont, breakDef, rcx.fmgr);
    minX = (int)textRect.getX();
    minY = (int)textRect.getY();
    maxX = minX + (int)textRect.getWidth();
    maxY = minY + (int)textRect.getHeight();
    if (textRect.contains(x, y)) {
      return (new Intersection(item.getID(), null, 0.0));
    } else {
      return (null);
    }
  }
  
 /***************************************************************************
  **
  ** Get the rectangle bounding the cis-reg region display
  */

  private Rectangle2D regionBounds(GenomeItem item, Point2D origin, int orient, FontRenderContext frc, FontManager fmgr) {  
    //
    // No hit on gene glyph.  If we have regions, try to see if we hit those:
    //
    double orx = origin.getX();
    double ory = origin.getY();    
    Font mFont = fmgr.getFont(FontManager.MODULE_NAME);
    Iterator<DBGeneRegion> rit = ((Gene)item).regionIterator();
    boolean hasARegion = rit.hasNext();
    if (hasARegion) {
      double minMinX = Double.POSITIVE_INFINITY;
      double maxMaxX = Double.NEGATIVE_INFINITY;
      double minMinY = Double.POSITIVE_INFINITY;
      double maxMaxY = Double.NEGATIVE_INFINITY;            
      while (rit.hasNext()) {
        DBGeneRegion region = rit.next();
        String label = region.getName();
        int startPad = region.getStartPad();
        int endPad = region.getEndPad();
        int midPad = (startPad + endPad) / 2;
        Vector2D off = getLandingPadOffsetByOrientAndSign(midPad, Linkage.NONE, orient);      
        double xLabel = orx + off.getX();
        double yLabel = ory + off.getY();
        Rectangle2D bounds = mFont.getStringBounds(label, frc);
        float width = (float)bounds.getWidth() / 2.0F;
        double minX = xLabel - width;
        double maxX = xLabel + width;
        double minY = yLabel + (LINE_THICK / 2.0);
        double maxY = minY + DIV_LINE_LEN_ + bounds.getHeight();
        if (minX < minMinX) minMinX = minX;
        if (maxX > maxMaxX) maxMaxX = maxX;
        if (minY < minMinY) minMinY = minY;
        if (maxY > maxMaxY) maxMaxY = maxY;
      }
      return (new Rectangle2D.Double(minMinX, minMinY, maxMaxX - minMinX, maxMaxY - minMinY));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Check for intersection of rectangle
  */

  public Intersection intersects(GenomeItem item, Rectangle rect, boolean countPartial, 
                                 DataAccessContext rcx, Object miscInfo) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double minX = origin.getX();
    double minY = origin.getY();
    
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = (float)extraPads * PAD_WIDTH_;    
    int orient = np.getOrientation();   
    minX = (orient == NodeProperties.LEFT) ? minX : minX - extraLen;
    float width = GENE_WIDTH_ + extraLen;
 
    Rectangle2D myBounds = new Rectangle2D.Double(minX, minY, width, GENE_HEIGHT_);
    Rectangle2D inbounds = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
    if (countPartial) {
      // 1/28/09 Note the above code is approximate; name is ignored.
      // Most rectangle intersections are kinda inexact anyway, but we
      // need to know stuff correctly for partial intersections
      // 5/19/09 And of course this DOES NOT check for overlap intersections,
      // where all 4 points are out, but the two still intersect!
      Rectangle trueRect = getBounds(item, rcx, miscInfo);
      minX = trueRect.x;
      minY = trueRect.y;
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
  ** Get the launch pad offset
  */
  
  public Vector2D getLaunchPadOffset(int padNum, GenomeItem item, DataAccessContext icx) {
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());
    int orient = np.getOrientation();
    return (getLaunchPadOffsetByOrient(orient));
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset
  */
  
  private Vector2D getLaunchPadOffsetByOrient(int orient) {
    Vector2D retval = null;
    if (orient == NodeProperties.RIGHT) {
      retval = (new Vector2D(GENE_WIDTH_, ARROW_HALF_HEIGHT_));
    } else {
      retval = (new Vector2D(0, ARROW_HALF_HEIGHT_));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the landing pad offset, for the given pad
  */
  
  public Vector2D getLandingPadOffset(int padNum, GenomeItem item, int sign,
                                      DataAccessContext icx) {
    NodeProperties np = icx.getLayout().getNodeProperties(item.getID());    
    int orient = np.getOrientation();
    return (getLandingPadOffsetByOrientAndSign(padNum, orient, sign));
  }
  
  /***************************************************************************
  **
  ** Get the landing pad offset, for the given pad
  */
  
  private Vector2D getLandingPadOffsetByOrientAndSign(int padNum, int orient, int sign) {
    float negPosTweak = 0.0F;
    if (sign == Linkage.NEGATIVE) {
      negPosTweak = NEG_OFFSET;
    } else if (sign == Linkage.POSITIVE) {
      negPosTweak = POS_OFFSET;
    }
    float vertical = GENE_HEIGHT_ - (LINE_THICK / 2.0F) - negPosTweak;
    Vector2D retval = null;
    if (orient == NodeProperties.RIGHT) {
      retval = (new Vector2D((PAD_WIDTH_ * (float)(padNum + 1)), vertical));
    } else {
      retval = (new Vector2D(GENE_WIDTH_-(PAD_WIDTH_ * (float)(padNum + 1)), vertical));
    }
    return (retval);
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
    //
    // Actually, even though "big" genes can have many pads, any pads over
    // the default are assigned negative pad numbers (a legacy...).  So
    // this is actually independent of the gene size.
    //
    return (MAX_PADS_);
  }
   
  /***************************************************************************
  **
  ** Get the bounds for autolayout, where we provide the needed orientation, padcount,
  ** and additional label text if desired.  It is assumed that the growth is horizontal.
  ** The bounds are returned with the placement point at (0,0). 
  */
  
  public Rectangle2D getBoundsForLayout(GenomeItem item, DataAccessContext rcx, int orientation, 
                                        boolean labelToo, Integer topPadCount) {    
  
     Point2D origin = new Point2D.Double(0.0, 0.0);
     NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
     String breakDef = np.getLineBreakDef();
     boolean hideName = np.getHideName();
     Gene theGene = (Gene)item;
     int padCount = (topPadCount == null) ? theGene.getPadCount() : topPadCount.intValue();
     AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride());
     Rectangle basic = getBoundsGuts(item, rcx.getFrc(), origin, padCount, NodeProperties.RIGHT, 
                                     breakDef, hideName, amFont, rcx.fmgr);
     return (basic);
  }
  
  /***************************************************************************
  **
  ** Get the launch pad offset for given layout case:
  */
  
  public Vector2D getLaunchPadOffsetForLayout(GenomeItem item, DataAccessContext rcx, 
                                              int orient, Integer topPadCount) {
    return (getLaunchPadOffsetByOrient(orient));
  }
   
  /***************************************************************************
  **
  ** Return the number of top pads on the node associated with the provided
  ** full pad count:
  */
  
  public int topPadCount(int fullPadCount) {
    return (fullPadCount);
  }

  /***************************************************************************
   **
   ** Sets the bound shapes for the CacheGroup, to be exported in the
   ** web application.
  */
  public void setGroupBounds(BoundShapeContainer group, Genome genome, GenomeItem item, Layout layout, FontRenderContext frc, FontManager fmgr) {
    ArrayList<ModelObjectCache.ModalShape> bounds = new ArrayList<ModelObjectCache.ModalShape>();

    fillBoundsArray(bounds, genome, item, layout, frc, fmgr);
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
                       Layout layout, FontRenderContext frc, FontManager fmgr) {
    NodeProperties np = layout.getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    double orx = origin.getX();
    double ory = origin.getY();

    // Quick kill:
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    
    String name = item.getName();
    String breakDef = np.getLineBreakDef();

    int orient = np.getOrientation();

    //
    // Figure out the length of the gene line:
    //

    int extraPads = (defaultLength) ? 0 : padCount - DBGene.DEFAULT_PAD_COUNT;
    float extraLen = (float)extraPads * PAD_WIDTH_;
    float lineLen = (defaultLength) ? LINE_LENGTH_ : LINE_LENGTH_ + extraLen;

    //
    // Mask the upper left / right  corner of the gene:
    //

    // TODO fix masking in web app
    double maskX = (orient == NodeProperties.RIGHT) ?
            orx - 2 - extraLen:
            (orx - 2) + ARROW_LENGTH_ + (3.0 * BRANCH_OFFSET_);
    double maskY = ory - 2;
    double maskW = (orient == NodeProperties.RIGHT) ?
            lineLen - (2.0 * BRANCH_OFFSET_) + 2 :
            lineLen - (3.0 * BRANCH_OFFSET_) + 2;
    double maskH = GENE_HEIGHT_ - (PAD_WIDTH_ + PAD_HEIGHT_);

    double minX = (orient == NodeProperties.RIGHT) ? orx - extraLen : orx - PAD_WIDTH_;
    double minY = ory - PAD_WIDTH_;
    double maxX = (orient == NodeProperties.RIGHT) ?
            orx + GENE_WIDTH_ + PAD_WIDTH_ :
            orx + GENE_WIDTH_ + extraLen; // minX + GENE_WIDTH_ + PAD_WIDTH_;
    double maxY = minY + GENE_HEIGHT_ + PAD_WIDTH_ + (LINE_THICK / 2.0F);

    ModelObjectCache.Rectangle rect1 = ModalShapeFactory.buildBoundRectangleForGlyph(minX, minY, (maxX - minX), (maxY - minY));
    targetArray.add(rect1);

    //
    // Export label bounds
    //

    Rectangle2D regBounds = regionBounds(item, origin, orient, frc, fmgr);
    boolean hasARegion = false;
    if (regBounds != null) {
      hasARegion = true;
      // TODO ModalShape regBoundsRect

    }

    AnnotatedFont amFont = fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride());

    Point2D nStart = getNameStart(item, name, amFont.getFont(), frc, orient, hasARegion, origin, np.getHideName(), breakDef, fmgr);
    Rectangle2D textRect = getTextBounds(frc, (float)nStart.getX(), (float)nStart.getY(), name, false, amFont, breakDef, fmgr);
    double labelMinX = (int)textRect.getX();
    double labelMinY = (int)textRect.getY();
    double labelMaxX = labelMinX + (int)textRect.getWidth();
    double labelMaxY = labelMinY + (int)textRect.getHeight();

    ModelObjectCache.Rectangle textBoundsRect = ModalShapeFactory.buildBoundRectangleForLabel(labelMinX, labelMinY,
            (labelMaxX - labelMinX), (labelMaxY - labelMinY));
    targetArray.add(textBoundsRect);
  }

  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext rcx, Object miscInfo) {
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    int padCount = ((Gene)item).getPadCount();
    int orient = np.getOrientation();
    String breakDef = np.getLineBreakDef();
    boolean hideName = np.getHideName();
    AnnotatedFont amFont = rcx.fmgr.getOverrideFont(FontManager.GENE_NAME, np.getFontOverride());
    return (getBoundsGuts(item, rcx.getFrc(), origin, padCount, orient, breakDef, hideName, amFont, rcx.fmgr));
  }
   
  /***************************************************************************
  **
  ** Return the Rectangle used by this node
  */
  
  private Rectangle getBoundsGuts(GenomeItem item, FontRenderContext frc, Point2D origin, 
                                  int padCount, int orient, String breakDef, boolean hideName, 
                                  AnnotatedFont amFont, FontManager fmgr) {
 
    double minXd = origin.getX();
    int minY = (int)origin.getY();
    
    //
    // Handle big genes:
    //
    
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = (float)extraPads * PAD_WIDTH_;      
    minXd = (orient == NodeProperties.LEFT) ? minXd : minXd - extraLen;
    float geneWidth = GENE_WIDTH_ + extraLen;
    int minX = (int)minXd;
    
    //
    // Check for regions:
    //
    
    boolean hasARegion = ((Gene)item).getNumRegions() > 0;
    
    //
    // If name is longer than gene, and is single-line, pump it up even more:
    //
    
    int boundWidth = (int)geneWidth;
    String name = item.getName();
    float height = 0.0F;
    boolean isSingle = isSingleLineText(name, false, breakDef);
    if (isSingle) {    
      Rectangle2D bounds = amFont.getFont().getStringBounds(name, frc);
      height = (float)bounds.getHeight();
      float width = (float)bounds.getWidth(); 
      float delta = width - (geneWidth - ARROW_LENGTH_);
      if (delta > 0.0F) {
        minX = minX - (int)delta;
        boundWidth = (int)(geneWidth + delta);
      }
      // This pads genes with "tiny" gene names
      if (hasARegion && (name.length() <= SHORTIE_CHAR)) {
        boundWidth += (REGION_HACK_ - 5);  // FIX ME!
      }
    }
    
    Rectangle retval = new Rectangle(minX, minY, boundWidth, (int)(GENE_HEIGHT_ + height + TEXT_PAD_));
    if (!hasARegion && isSingle) {
      return (retval);
    }
    
    //
    // Heavy-duty stuff (multi-line, or with regions)
    //
    
    Point2D nStart = getNameStart(item, name, amFont.getFont(), frc, orient, hasARegion, origin, hideName, breakDef, fmgr);
    
    if (isSingle) {
      retval.height = (int)nStart.getY() - retval.y;
      return (retval);
    }    
    
    Rectangle2D textRect = getTextBounds(frc, (float)nStart.getX(), (float)nStart.getY(), name, false, amFont, breakDef, fmgr);
    // Need to know height of first line of text:
    List<String> frags = MultiLineRenderSupport.applyLineBreaksForFragments(name, breakDef); 
    String frag = frags.get(0);
    Rectangle2D bounds = amFont.getFont().getStringBounds(frag, frc);
    
    retval.height = (int)nStart.getY() - (int)(bounds.getHeight() * 0.8) + (int)textRect.getHeight() - minY;
    retval.x = ((int)nStart.getX() < minX) ? (int)nStart.getX() : minX;
    int maxX = retval.x + retval.width;
    int maxXForText = (int)(textRect.getX() + textRect.getWidth());
    
    retval.width = (maxXForText >  maxX) ? maxXForText - retval.x : retval.width;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Return the height
  */
  
  public double getGlyphHeightForLayout(GenomeItem item, DataAccessContext rcx) {
    return (GENE_HEIGHT_ + 50.0); // FIX ME to handle text correctly
  }  
  
  /***************************************************************************
  **
  ** Return the width
  */
  
  public double getWidth(GenomeItem item, DataAccessContext rcx) {
    int padCount = ((Gene)item).getPadCount();
    boolean defaultLength = (padCount == DBGene.DEFAULT_PAD_COUNT);
    int extraPads = (defaultLength) ? 0 : padCount - MAX_PADS_;    
    float extraLen = (float)extraPads * PAD_WIDTH_;    
    float geneWidth = GENE_WIDTH_ + extraLen;
    return (geneWidth);
  }

  /***************************************************************************
  **
  ** Get the recommended node insertion info
  */
  
  @Override
  public NodeInsertionDirective getInsertionDirective(Vector2D travel, Point2D insertion) {
    //
    // Different approach than others: pads stay the same, but
    // we change the orientation!
    //
    
    int orient = (travel.getX() >= 0) ? NodeProperties.RIGHT : NodeProperties.LEFT;
    Vector2D lau = getLaunchPadOffsetByOrient(orient);
    Vector2D lnd = getLandingPadOffsetByOrientAndSign(0, orient, Linkage.NONE);
    Vector2D posTweak = new Vector2D(-10.0, -10.0);
    ArrayList<Point2D> landingCorners = new ArrayList<Point2D>();
    ArrayList<Point2D> launchCorners = new ArrayList<Point2D>();
    Point2D tweaked = posTweak.add(insertion);
    
    if (travel.getX() == 0.0) {  // Pure vertical
      if (travel.getY() < 0.0) { // Coming from below
        Point2D pt0 = new Point2D.Double(tweaked.getX() + lnd.getX(), insertion.getY() + 80.0);
        landingCorners.add(pt0);       
        Point2D pt1 = new Point2D.Double(pt0.getX() - 50.0, pt0.getY());
        landingCorners.add(pt1);        
        Point2D pt2 = new Point2D.Double(pt1.getX(), insertion.getY());
        landingCorners.add(pt2);    
        Point2D pt3 = new Point2D.Double(pt0.getX(), pt2.getY());
        landingCorners.add(pt3);
        
        Point2D lpt0 = new Point2D.Double(tweaked.getX() + lau.getX() + 20.0, insertion.getY());        
        Point2D lpt1 = new Point2D.Double(lpt0.getX(), insertion.getY() - 20.0);
        Point2D lpt2 = new Point2D.Double(tweaked.getX() + lnd.getX(), lpt1.getY());
        // added backwards!
        launchCorners.add(lpt2);
        launchCorners.add(lpt1);
        launchCorners.add(lpt0);        
      } else { // Coming from above (no landing points needed)
        Point2D lpt0 = new Point2D.Double(tweaked.getX() + lau.getX() + 20.0, insertion.getY());        
        Point2D lpt1 = new Point2D.Double(lpt0.getX(), insertion.getY() + 50.0);
        Point2D lpt2 = new Point2D.Double(tweaked.getX() + lnd.getX(), lpt1.getY());
        // added backwards!
        launchCorners.add(lpt2);
        launchCorners.add(lpt1);
        launchCorners.add(lpt0);
      }
    } else if (travel.getY() == 0.0) {  // Pure horizontal
      Point2D lastLanding = posTweak.add(insertion);
      lastLanding.setLocation(lastLanding.getX() + lnd.getX(), insertion.getY());
      landingCorners.add(lastLanding);          
    }
    
    if (landingCorners.isEmpty()) {
      landingCorners = null;
    }
    
    if (launchCorners.isEmpty()) {
      launchCorners = null;
    }
    
    return (new NodeInsertionDirective(0, 0, orient, posTweak, landingCorners, launchCorners));
  }
  
  /***************************************************************************
  **
  ** Get a list of pads that are nearby to the given one.  Gotta override the base class, since our
  ** numbering is not spiral:
  */

  @Override
  public List<Integer> getNearbyPads(GenomeItem item, int startPad, Layout layout) {

    MinMax range = getTargetPadRange(item);    
    int count = 0;
    HashMap<Integer, Integer> distances = new HashMap<Integer, Integer>();    
    
    for (int i = range.min; i <= range.max; i++) {
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
  
  private void renderPads(GeneCacheGroup group, Color col, GenomeItem item, DataAccessContext rcx) {
                       
  	Integer majorLayer = NodeRenderBase.NODE_MAJOR_LAYER;
  	Integer minorLayer = NodeRenderBase.NODE_MINOR_LAYER;
  	
    NodeProperties np = rcx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    // g2.setPaint(col); 
    // g2.setStroke(new BasicStroke(1));
    Vector2D lpo = getLaunchPadOffset(0, item, rcx);
    double x = origin.getX() + lpo.getX();
    double y = origin.getY() + lpo.getY();
    double padRadius = (PAD_WIDTH_ / 2.0) - 1.0;
    // Ellipse2D circ = new Ellipse2D.Double(x - padRadius, y - padRadius,
    //                                       2.0 * padRadius, 2.0 * padRadius);
    // g2.draw(circ);    
    
    ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, padRadius, DrawMode.DRAW, col, new BasicStroke(1));
    
    if (!rcx.forWeb) {
    	group.addShape(circ, majorLayer, minorLayer);
    }
    else {
    	group.addPadShape(circ, majorLayer, minorLayer);
    	group.addPad(x, y);
    }
    
    //
    // Draw tablets for landing pads, not circles.
    //
    // Pad 0 is at the end of a standard length gene.  For extra-length genes, the
    // gene grows in the negative direction, thus the introduction of MAX_PADS:
    //
    
    int padCount = ((Gene)item).getPadCount();
    int minPad = MAX_PADS_ - padCount;
    for (int i = minPad; i < MAX_PADS_; i++) {
      lpo = getLandingPadOffset(i, item, Linkage.POSITIVE, rcx);
      x = origin.getX() + lpo.getX();
      y = origin.getY() + lpo.getY();
      // Arc2D arc = new Arc2D.Double(x - padRadius, y - padRadius,
      //                              2.0 * padRadius, 2.0 * padRadius, 180, 180, Arc2D.OPEN);
      // g2.draw(arc);
      
  		Arc arc1 = new Arc(x, y, padRadius, 180.0, 180.0,
  				Arc.Type.OPEN, DrawMode.DRAW, col, new BasicStroke(1));
  		
      
      double xt = x;
      double yt = y - PAD_HEIGHT_;
      // arc = new Arc2D.Double(xt - padRadius, yt - padRadius,
      //                        2.0 * padRadius, 2.0 * padRadius, 0, 180, Arc2D.OPEN);
      // g2.draw(arc);      
      
  		Arc arc2 = new Arc(xt, yt, padRadius, 0.0, 180.0,
  				Arc.Type.OPEN, DrawMode.DRAW, col, new BasicStroke(1));
  		
      

      // Line2D line = new Line2D.Double(x - padRadius, y, xt - padRadius, yt);
      // g2.draw(line);      

      Line line1 = new Line(x - padRadius, y, xt - padRadius, yt, DrawMode.DRAW, col, new BasicStroke(1));
  		
  		
      // line = new Line2D.Double(x + padRadius, y, xt + padRadius, yt);
      // g2.draw(line);      

      Line line2 = new Line(x + padRadius, y, xt + padRadius, yt, DrawMode.DRAW, col, new BasicStroke(1));
  		
  		
  		if (!rcx.forWeb) {
  			group.addShape(arc1, majorLayer, minorLayer);
  			group.addShape(arc2, majorLayer, minorLayer);
  			group.addShape(line1, majorLayer, minorLayer);
  			group.addShape(line2, majorLayer, minorLayer);
  		}
  		else {
  			group.addPadShape(arc1, majorLayer, minorLayer);
  			group.addPadShape(arc2, majorLayer, minorLayer);
  			group.addPadShape(line1, majorLayer, minorLayer);
  			group.addPadShape(line2, majorLayer, minorLayer);
  			
  			group.addPad(x, y);
  		}
  		
    }
    return;
  }

  /***************************************************************************
  **
  ** Figure out which pads we intersect
  */
  
  private List<Intersection.PadVal> calcPadIntersects(GenomeItem item,  Point2D pt, DataAccessContext irx) {

    ArrayList<Intersection.PadVal> retval = new ArrayList<Intersection.PadVal>();
 
    NodeProperties np = irx.getLayout().getNodeProperties(item.getID());
    Point2D origin = np.getLocation();
    Vector2D lpo = getLaunchPadOffset(0, item, irx);
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
    
    int padCount = ((Gene)item).getPadCount();
    int minPad = MAX_PADS_ - padCount;
    for (int i = minPad; i < MAX_PADS_; i++) {
      lpo = getLandingPadOffset(i, item, Linkage.POSITIVE, irx);
      x = origin.getX() + lpo.getX();
      y = origin.getY() + lpo.getY();
      double yt = y - PAD_HEIGHT_;
      double botBubSq = ((px - x) * (px - x)) + ((py - y) * (py - y)); // bottom bubble
      double topBubSq = ((px - x) * (px - x)) + ((py - yt) * (py - yt));  // top bubble
      boolean inMiddle = ((Math.abs(px - x) < padRadius) && (py < y) && (py > yt)); // middle band 
      
      if ((botBubSq <= prSq) ||  // bottom bubble
          (topBubSq <= prSq) ||  // top bubble
          (inMiddle)) {          // middle band 
        Intersection.PadVal retpad = new Intersection.PadVal();
        retpad.okEnd = true;
        retpad.okStart = false;
        retpad.padNum = i;
        retpad.distance = 0.0;
        retval.add(retpad);
      }
    }
    return ((retval.isEmpty()) ? null : retval);
  }

  /***************************************************************************
  **
  ** Render the glyph used to indicate experimental confidence
  */
  
  private void renderEvidenceGlyphForGene(ModalShapeContainer group, Gene gene,
                                          Point2D origin, boolean isGhosted) {  

    int evidence = gene.getEvidenceLevel();
    if (evidence == Gene.LEVEL_NONE) {
      return;
    }
    
    double x = origin.getX();
    double y = origin.getY();
    
    float glyphBaseX = (float)(x + LINE_LENGTH_ - BRANCH_OFFSET_);
    float glyphBaseY = (float)(y + GENE_HEIGHT_ + (LINE_THICK / 2.0) + 1.0);
    Color col = (isGhosted) ? Color.LIGHT_GRAY : Color.red;
    EvidenceGlyph.renderEvidenceGlyph(group, col, glyphBaseX, glyphBaseY);
    return;
  }
  
  /***************************************************************************
  **
  ** Render the glyph used to indicate experimental confidence
  */
  
  private void renderEvidenceGlyphForRegion(ModalShapeContainer group, Gene item, DBGeneRegion region,
                                            Point2D origin, boolean isGhosted, DataAccessContext rcx) {  

    int evidence = region.getEvidenceLevel();
    if (evidence == Gene.LEVEL_NONE) {
      return;
    }
    float x = (float)origin.getX();
    float y = (float)origin.getY();
    Color col = (isGhosted) ? Color.LIGHT_GRAY : Color.red;    
    int endPad = region.getEndPad();
    Vector2D off = getLandingPadOffset(endPad, item, Linkage.NONE, rcx);      
    float glyphBaseY = y + GENE_HEIGHT_ + (float)(LINE_THICK / 2.0) + GLYPH_HACK_;
    float glyphBaseX = x + (float)(off.getX());
    EvidenceGlyph.renderEvidenceGlyph(group, col, glyphBaseX, glyphBaseY);
    return;
  }  
  
}
