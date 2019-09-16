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

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.AnnotatedFont;
import org.systemsbiology.biotapestry.ui.BusDrop;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.ItemRenderBase;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.ResolvedDrawStyle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.LinkageCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.LinkageExportForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactory;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForDesktop;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalTextShapeFactoryForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a linkage
*/

public class LinkageFree extends ItemRenderBase implements DrawTreeModelDataSource, PlacementGridRenderer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double INTERSECT_TOL = 5.0;   
  
  private static final int NEG_THICK_ = 2;
  
  private static final double PLUS_ARROW_DEPTH_ = 8.0;
  private static final double POSITIVE_DROP_OFFSET_ = PLUS_ARROW_DEPTH_ - 1.0;  
  private static final double PLUS_ARROW_HALF_WIDTH_ = 5.0;
  private static final double TIP_FUDGE_ = INodeRenderer.POS_OFFSET + 1;
  
  private static final double LEVEL_FUDGE_ = POSITIVE_DROP_OFFSET_ + GeneFree.LINE_THICK + 5.0;  // FIX_ME  
  private static final double LEVEL_FUDGE_NEG_ = GeneFree.LINE_THICK + 4.0;  // FIX_ME  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public LinkageFree() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the link to a pattern grid
  */
  
  public void renderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, Set<String> skipLinks, 
                                    String overID, DataAccessContext irx) {
  	
    (new PlacementGridSupport()).renderToPlacementGrid(lp, grid, skipLinks, overID, irx);
    return;  	
  }
  
 /***************************************************************************
  **
  ** Answer if we can render the link to a pattern grid
  */
  
  public boolean canRenderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, 
                                          Set<LinkPlacementGrid.PointPair> dropSet, 
                                          String overID, DataAccessContext irx) {
    
    return ((new PlacementGridSupport()).canRenderToPlacementGrid(lp, grid, dropSet, overID, irx));
  }

 /***************************************************************************
  **
  ** Get the map of point pairs to links
  */
  
  public Map<LinkPlacementGrid.PointPair, List<String>> getPointPairMap(LinkProperties lp,
                                                                        String overID, 
                                                                        DataAccessContext irx) {
    
     return ((new PlacementGridSupport()).getPointPairMap(lp, overID, irx));
  }  
 
  /***************************************************************************
  **
  ** Render the link at the designated location
  */
  
  public void render(ModelObjectCache cache, GenomeItem item, Intersection selected, 
                     DataAccessContext rcx, Object miscInfo) {
  	
  	ModalTextShapeFactory textFactory = null;
  	
  	if (rcx.forWeb) {
  		textFactory = new ModalTextShapeFactoryForWeb(rcx.getFrc());
  	}
  	else {
  		textFactory = new ModalTextShapeFactoryForDesktop();
  	}
  	
  	Integer majorLayer = DrawTree.ACTIVE_PATH_LAYER;
  	Integer minorLayer = DrawTree.MINOR_TEXT_LAYER;
    BusProperties bp = rcx.getLayout().getLinkProperties(item.getID());
   
    DrawTree dTree = new DrawTree(bp, this, selected, rcx);
    
    LinkageExportForWeb lefw = new LinkageExportForWeb();
    
    LinkageCacheGroup group = new LinkageCacheGroup(item, lefw, bp.getSourceTag(), rcx.getLayout().getSharedItems(item.getID()));
    
    DataAccessContext rcxNO = new DataAccessContext(rcx);
    rcxNO.oso = null;
    dTree.renderToCache(group, this, bp, ((AugmentedDisplayOptions)miscInfo).skipDrops, rcxNO);
    
    // Call exportLinkages after renderToCache, so that the resolved draw styles are set for all DrawTreeSegments
    dTree.exportLinkages(lefw, rcxNO, bp);
    
    Font mFont = rcx.fmgr.getFont(FontManager.LINK_LABEL);
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    String label = bp.getLabel(rcx.getGenome());       
    if ((label != null) && (!label.trim().equals(""))) {
      Point2D txtLoc = bp.getTextPosition();
      float txtX = (txtLoc == null) ? 0.0F : (float)txtLoc.getX();
      float txtY = (txtLoc == null) ? 0.0F : (float)txtLoc.getY();      
      int txtDir = bp.getTextDirection();      
      double radians = 0.0;
      if (txtDir == LinkProperties.LEFT) {
        radians = 0.0; 
      } else if (txtDir == LinkProperties.UP) {
        radians = -Math.PI / 2.0;
      } else if (txtDir == LinkProperties.DOWN) {
        radians = Math.PI / 2.0;
      } else if (txtDir == LinkProperties.RIGHT) {
        radians = Math.PI;
      }
      AffineTransform trans = new AffineTransform();
      trans.rotate(radians, txtX, txtY);
      Color textCol = (rcx.isGhosted()) ? dop.getInactiveGray() : bp.getColor(rcx.cRes);

      // In the web application, push the same transformation to the shape stream,
      // because the transform will NOT be serialized from the TextShape in ModalShapeJSONizer..
      if (rcx.forWeb) {
    	  ModelObjectCache.PushTransformOperation pushTrans = new ModelObjectCache.PushTransformOperation(trans);    	  
    	  group.addShape(pushTrans, majorLayer, minorLayer);
      }
      

      ModalShape ts = textFactory.buildTextShape(label, new AnnotatedFont(mFont, true), textCol, txtX, txtY, trans);
      group.addShape(ts, DrawTree.ACTIVE_PATH_LAYER, DrawTree.MINOR_TEXT_LAYER);      

      // In the web application, push a pop transformation to the shape stream,
      // so that the above transform for the TextShape will be popped in the web client.
      if (rcx.forWeb) {
    	  ModelObjectCache.PopTransformOperation popTrans = new ModelObjectCache.PopTransformOperation();    	  
    	  group.addShape(popTrans, majorLayer, minorLayer);
      }
    }
    
    cache.addGroup(group);
    
    return;    
  }

  /***************************************************************************
  **
  ** Check for intersection:
  */
  
  public Intersection intersects(GenomeItem item, Point2D pt, DataAccessContext rcx, Object miscInfo) {
    return (intersectBusForSelectedDrops(item, rcx, pt, null));
  }

  /***************************************************************************
  **
  ** Check for intersection, only allowing a subset of drops to be checked
  */
  
  public Intersection intersectBusForSelectedDrops(GenomeItem item, DataAccessContext rcx,                                              
                                                   Point2D pt, Set<String> omittedDrops) {

    BusProperties bp = rcx.getLayout().getLinkProperties(item.getID());
    double useDiam = ((2.0 * rcx.pixDiam) > INTERSECT_TOL) ? 2.0 * rcx.pixDiam : INTERSECT_TOL;
    LinkProperties.DistancedLinkSegID dslsid = bp.intersectBusSegment(rcx, pt, omittedDrops, useDiam);
    return ((dslsid == null) ? null : new Intersection(item.getID(), new MultiSubID(dslsid.segID), dslsid.distance, true));
  }
  

  /***************************************************************************
  **
  ** Check for intersection of label:
  */
  
  public boolean intersectsLabel(GenomeItem item, LinkProperties lp, DataAccessContext rcx, Point2D pt) {

    Rectangle2D bounds = labelBounds(rcx.getGenome(), item, lp, rcx.getFrc(), rcx.fmgr);    
    if (bounds == null) {
      return (false);
    }
    return (Bounds.intersects(bounds.getX(), bounds.getY(), 
                              bounds.getMaxX(), bounds.getMaxY(), pt.getX(), pt.getY()));
  }
  
 /***************************************************************************
  **
  ** Check for intersection of label:
  */
  
  public boolean intersectsLabel(GenomeItem item, LinkProperties lp, DataAccessContext rcx, Rectangle rect) {

    Rectangle2D bounds = labelBounds(rcx.getGenome(), item, lp, rcx.getFrc(), rcx.fmgr);    
    if (bounds == null) {
      return (false);
    }
    Rectangle2D testRect = 
      new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height); 
    
    return (testRect.contains(bounds));
  }
  
  /***************************************************************************
  **
  ** Get bounds for label
  */
  
  private Rectangle2D labelBounds(Genome genome, GenomeItem item, 
                                  LinkProperties lp, 
                                  FontRenderContext frc, FontManager fmgr) {

    String label = ((BusProperties)lp).getLabel(genome);
    if ((label == null) || (label.trim().equals(""))) {
      return (null);
    }
    
    Font mFont = fmgr.getFont(FontManager.LINK_LABEL);    
    Rectangle2D bounds = mFont.getStringBounds(label, frc);
    double width = bounds.getWidth();
    double height = bounds.getHeight();    
    Point2D txtLoc = lp.getTextPosition();
    float txtX = (txtLoc == null) ? 0.0F : (float)txtLoc.getX();
    float txtY = (txtLoc == null) ? 0.0F : (float)txtLoc.getY();      
    int txtDir = lp.getTextDirection();      
    double minX;
    double maxX;
    double maxY;
    double minY;
    if (txtDir == LinkProperties.LEFT) {
      minX = txtX;
      maxX = txtX + width;
      maxY = txtY;
      minY = txtY - height;
    } else if (txtDir == LinkProperties.UP) {
      minX = txtX - height;
      maxX = txtX;
      maxY = txtY;
      minY = txtY - width;      
    } else if (txtDir == LinkProperties.DOWN) {
      minX = txtX;
      maxX = txtX + height;
      maxY = txtY + width;
      minY = txtY;      
    } else if (txtDir == LinkProperties.RIGHT) {
      minX = txtX - width;
      maxX = txtX;
      maxY = txtY + height;
      minY = txtY;      
    } else {
      throw new IllegalStateException();
    }
    return (new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
  }  

  /***************************************************************************
  **
  ** Check for intersection of rectangle
  */

  public Intersection intersects(GenomeItem item, Rectangle rect, boolean countPartial, 
                                 DataAccessContext rcx, Object miscInfo) {
                                   
    Rectangle2D testRect = 
      new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);         
  
    MultiSubID retSub = null;
    
    String itemID = item.getID();
    BusProperties bp = rcx.getLayout().getLinkProperties(itemID);    
    List<LinkSegmentID> segs = bp.intersectBusSegmentsWithRect(rcx, null, testRect, false);
    Iterator<LinkSegmentID> sit = segs.iterator();
    while (sit.hasNext()) {
      LinkSegmentID lsid = sit.next();
      MultiSubID si = new MultiSubID(lsid);
      if (retSub == null) {
        retSub = si;
      } else {
        retSub.merge(si);
      }                                
    }
    return ((retSub == null) ? null : new Intersection(item.getID(), retSub, 0.0, true));
  }  
  
  /***************************************************************************
  **
  ** Return the Rectangle used by certain segments of the item
  */
  
  public Rectangle getBoundsOfSegments(GenomeItem item, DataAccessContext irx,  MultiSubID subIDs, boolean doLinkLabels) {
                                                                        
    BusProperties bp = irx.getLayout().getLinkProperties(item.getID());
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY; 
    
    ArrayList<LinkSegment> segsToCheck = new ArrayList<LinkSegment>();
    
    LinkSegmentID[] segsFromMulti = null;
    if (subIDs != null) {
      segsFromMulti = Intersection.segmentIDsFromMultiSubID(subIDs);
      for (int i = 0; i < segsFromMulti.length; i++) {
        LinkSegment fakeSeg = bp.getSegmentGeometryForID(segsFromMulti[i], irx, true);
        segsToCheck.add(fakeSeg);
      }
    //
    // FIX ME!! This is the legacy method: note no calcs for drops or directs.  Fast,
    // but incomplete:
    //
    } else {
      Iterator<LinkSegment> sit = bp.getSegments();
      while (sit.hasNext()) {
        LinkSegment seg = sit.next();
        segsToCheck.add(seg);
      }
    }    

    int numToCheck = segsToCheck.size();
    for (int i = 0; i < numToCheck; i++) {
      LinkSegment seg = segsToCheck.get(i);
      Point2D pt = seg.getStart();
      double x = pt.getX();
      double y = pt.getY();
      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x > maxX) maxX = x;
      if (y > maxY) maxY = y;
      pt = seg.getEnd();
      if (pt == null) {
        continue;
      }
      x = pt.getX();
      y = pt.getY();
      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x > maxX) maxX = x;
      if (y > maxY) maxY = y;      
    }
    //
    // Label bounds too: (though long labels will be inomplete: need to
    // rotate and bound the text):
    //
    if ((subIDs == null) && doLinkLabels) {
      String label = bp.getLabel(irx.getGenome());       
      if ((label != null) && (!label.trim().equals(""))) {
        Point2D txtLoc = bp.getTextPosition();
        double txtX = (txtLoc == null) ? 0.0 : (double)txtLoc.getX();
        double txtY = (txtLoc == null) ? 0.0 : (double)txtLoc.getY();     
        if (txtX < minX) minX = txtX;
        if (txtY < minY) minY = txtY;
        if (txtX > maxX) maxX = txtX;
        if (txtY > maxY) maxY = txtY; 
      }
    }

    if (minX == Double.POSITIVE_INFINITY) {
      return (null);
    }
 
    return (new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY)));
  }  
  
  /***************************************************************************
  **
  ** Return the Rectangle used by this item
  */
  
  public Rectangle getBounds(GenomeItem item, DataAccessContext irx,  Object miscInfo) {                                                 
    return (getBoundsOfSegments(item, irx, null, true));  
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by the single path of this item
  */
  
  public Rectangle getBoundsForSinglePath(GenomeItem item, DataAccessContext irx) {
                                                                        
    LinkProperties lp = irx.getLayout().getLinkProperties(item.getID());
    if (lp == null) {
      System.err.println("No props for " + item.getID());
    }
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;    

    BusProperties bp = (BusProperties)lp;
    Iterator<LinkBusDrop> dit = bp.getDrops();
    BusDrop itemDrop = null;
    String itemID = item.getID();
    while (dit.hasNext()) {
      BusDrop drop = (BusDrop)dit.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        continue;
      }
      if (ref.equals(itemID)) {
        itemDrop = drop;
        break;
      }
    }
    Iterator<LinkSegment> sit = null;
    if (itemDrop != null) {
      sit = bp.getSegmentsToRoot(itemDrop).iterator();
    }
    
    if (sit == null) {
      return (null);
    }
    
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      Point2D pt = seg.getStart();
      double x = pt.getX();
      double y = pt.getY();
      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x > maxX) maxX = x;
      if (y > maxY) maxY = y;
      pt = seg.getEnd();
      if (pt == null) {
        continue;
      }
      x = pt.getX();
      y = pt.getY();
      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x > maxX) maxX = x;
      if (y > maxY) maxY = y;      
    }
    
    // FIX ME: include label bounds too!
    
    if (minX == Double.POSITIVE_INFINITY) {
      return (null);
    }
 
    return (new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY)));
  }
  
  /***************************************************************************
  **
  ** Get information on potential line style modulation for a model
  */
  
  public DrawTreeModelDataSource.ModelLineStyleModulation getModelLineStyleModulation(DataAccessContext icx) {
    
    DrawTreeModelDataSource.ModelLineStyleModulation retval = 
      new DrawTreeModelDataSource.ModelLineStyleModulation();
    DisplayOptions dop = icx.getDisplayOptsSource().getDisplayOptions();
    retval.linkModulation = (dop == null) ? DisplayOptions.NO_LINK_ACTIVITY_DISPLAY : dop.getLinkActivity();
    retval.checkForActive = (icx.getGenome() instanceof GenomeInstance);
    retval.branchRenderMode = (dop == null) ? DisplayOptions.NO_BUS_BRANCHES : dop.getBranchMode();
    retval.forModules = false;

    return (retval);
  }
    
  /***************************************************************************
  **
  ** Get information on line style modulation for a link
  */
  
  public DrawTreeModelDataSource.LinkLineStyleModulation 
    getLinkLineStyleModulation(DataAccessContext icx, String linkID, LinkProperties lp, 
                               ModelLineStyleModulation modulationInfo) {
    
    Genome genome = icx.getGenome();
    
    if (!lp.linkIsInModel(icx.getGenomeSource(), genome, icx.oso, linkID)) {
      return (null);
    }
    
    Linkage link = genome.getLinkage(linkID);
    DrawTreeModelDataSource.LinkLineStyleModulation retval = new DrawTreeModelDataSource.LinkLineStyleModulation();
    int evidence = link.getTargetLevel();
    DisplayOptions dop = icx.getDisplayOptsSource().getDisplayOptions();
    retval.perLinkForEvidence = (dop == null) ? null : dop.getEvidenceDrawChange(evidence); 
    retval.sign = link.getSign();
    retval.perLinkActivity = null;
    retval.isActive = true;
    if (modulationInfo.checkForActive) {
      GenomeInstance gi = (GenomeInstance)genome;
      LinkageInstance li = (LinkageInstance)link;
      int linkageActivity = li.getActivity(gi);
      retval.isActive = (linkageActivity != LinkageInstance.INACTIVE);
      if ((modulationInfo.linkModulation != DisplayOptions.NO_LINK_ACTIVITY_DISPLAY) && 
          (linkageActivity == LinkageInstance.VARIABLE)) {
        retval.perLinkActivity = new Double(li.getActivityLevel(gi));
      }
    }
    retval.targetOffset = (retval.sign == Linkage.POSITIVE) ? POSITIVE_DROP_OFFSET_ : 0.0;  
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Handle Model data 
  */
  
  public DrawTreeModelDataSource.ModelDataForTip getModelDataForTip(DataAccessContext icx, String linkID, LinkProperties lp) {    
   
    Genome genome = icx.getGenome();
    DrawTreeModelDataSource.ModelDataForTip retval = new DrawTreeModelDataSource.ModelDataForTip();
    Linkage link = genome.getLinkage(linkID);
    retval.sign = link.getSign();
    Node node = genome.getNode(link.getTarget());
    NodeProperties nProp = icx.getLayout().getNodeProperties(node.getID());
    INodeRenderer render = nProp.getRenderer();
    int land = link.getLandingPad();
    retval.padWidth = render.getLandingPadWidth(land, node, icx.getLayout());
    retval.arrival = render.getArrivalDirection(land, node, icx.getLayout());
    Point2D trgLoc = nProp.getLocation();
    Vector2D lanPO = render.getLandingPadOffset(land, node, retval.sign, icx);
    retval.lanLoc = lanPO.add(trgLoc);
    DisplayOptions dop = icx.getDisplayOptsSource().getDisplayOptions();
    
    retval.negLength = retval.padWidth + dop.getExtraFootSize();
    retval.negThick = Math.round((float)(NEG_THICK_ * (retval.negLength / retval.padWidth)));
    
    boolean checkForActive = (genome instanceof GenomeInstance);
    retval.isActive = (checkForActive)
                        ? (((LinkageInstance)link).getActivity((GenomeInstance)genome) == LinkageInstance.ACTIVE)
                        : true;
    
    retval.plusArrowDepth = PLUS_ARROW_DEPTH_;
    retval.positiveDropOffset = POSITIVE_DROP_OFFSET_;
    retval.plusArrowHalfWidth = PLUS_ARROW_HALF_WIDTH_; 
    retval.thickThick = ResolvedDrawStyle.THICK_THICK; 
    retval.tipFudge = TIP_FUDGE_; 
    retval.levelFudge = (retval.sign == Linkage.NEGATIVE) ? LEVEL_FUDGE_NEG_ : LEVEL_FUDGE_;
    
    int level = link.getTargetLevel();    
    retval.hasDiamond = (dop == null) ? false : dop.drawEvidenceGlyphs(level);
 
    return (retval);   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to send more info in via misc. option
  */
  
  public static class AugmentedDisplayOptions {
    public DisplayOptions opts;
    public Set<String> skipDrops;
    
    public AugmentedDisplayOptions(DisplayOptions opts, Set<String> skipDrops) {
      this.opts = opts;
      this.skipDrops = skipDrops;     
    }
  }  
}
