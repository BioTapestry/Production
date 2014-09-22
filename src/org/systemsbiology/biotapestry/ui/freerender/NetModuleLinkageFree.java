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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleBusDrop;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.ResolvedDrawStyle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkageCacheGroup;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkageExportForWeb;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** This renders a net module linkage
*/

public class NetModuleLinkageFree implements DrawTreeModelDataSource, PlacementGridRenderer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double INTERSECT_TOL = 10.0;
 
  private static final double PLUS_ARROW_DEPTH_MOD_ = 20.0;
  private static final double NEG_FOOT_MOD_ = 17.0;
  private static final double POSITIVE_DROP_OFFSET_MOD_ = PLUS_ARROW_DEPTH_MOD_ - 1.0;  
  private static final double PLUS_ARROW_HALF_WIDTH_MOD_ = 7.0;
  private static final double TIP_FUDGE_MOD_ = 0.0;
  private static final int NEG_THICK_MOD_ = 4;
  private static final double NEGATIVE_DROP_OFFSET_MOD_ = 4.0;
 
  private static final double LEVEL_FUDGE_ = 0.0;
  private static final double LEVEL_FUDGE_NEG_ = 0.0;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic constructor
  */

  public NetModuleLinkageFree() {
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
                                    String overID, DataAccessContext icx) {
    
    
    (new PlacementGridSupport()).renderToPlacementGrid(lp, grid, skipLinks, overID, icx);
    return;
  }
  
 /***************************************************************************
  **
  ** Answer if we can render the link to a pattern grid
  */
  
  public boolean canRenderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, 
                                          Set<LinkPlacementGrid.PointPair> dropSet, 
                                          String overID, DataAccessContext icx) {
    
    return ((new PlacementGridSupport()).canRenderToPlacementGrid(lp, grid, dropSet, overID, icx));
  }

 /***************************************************************************
  **
  ** Get the map of point pairs to links
  */
  
  public Map<LinkPlacementGrid.PointPair, List<String>> getPointPairMap(LinkProperties lp,
                                                                        String overID, 
                                                                        DataAccessContext icx) {
    
     return ((new PlacementGridSupport()).getPointPairMap(lp, overID, icx));
  }
  
  /***************************************************************************
  **
  ** Render the module linkage TREE!
  */
  
  public void render(ModalShapeContainer group, String ovrID, String treeID, DataAccessContext rcx) {

    NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(ovrID);
    NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);   
    DrawTree dTree = new DrawTree(nmlp, this,  null, rcx);
    rcx.pushGhosted(false);
    dTree.renderToCache(group, this, nmlp, null, rcx);
    rcx.popGhosted();
    return;  	
  }
  
  public void renderForWeb(NetModuleLinkageCacheGroup group, String ovrID, String treeID, NetModuleLinkageExportForWeb lefw, DataAccessContext rcx) {
    NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(ovrID);
    NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);   
    DrawTree dTree = new DrawTree(nmlp, this,  null, rcx);
    rcx.pushGhosted(false);
    dTree.renderToCache(group, this, nmlp, null, rcx);
    dTree.exportNetModuleLinkages(lefw, rcx, ovrID, nmlp);
    rcx.popGhosted();
    return;  	
  }  
  
  /***************************************************************************
  **
  ** Answer if we intersect the link TREE.
  */
  
  public Intersection intersects(String treeID, String ovrID, DataAccessContext itx, Point2D pt) {
    NetOverlayProperties nop = itx.getLayout().getNetOverlayProperties(ovrID);
    NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
    
    //
    // Use mod links as a testbed for scale-dependent intersection testing:
    //
    
    double useTol = (INTERSECT_TOL > itx.pixDiam) ? INTERSECT_TOL : 2.0 * itx.pixDiam;
     
    LinkProperties.DistancedLinkSegID dslid = nmlp.intersectBusSegment(itx, pt, null, useTol);
    if (dslid != null) {
      // For really short links, we need to be able to add a corner point to
      // be able to deal with it.  These normally report an endpoint intersection,
      // so clear that flag:
      if (nmlp.isDirect()) {
        dslid.segID.clearTaggedEndpoint();
      }
      return (new Intersection(treeID, new MultiSubID(dslid.segID), dslid.distance, true));
    } else {
      return (null);
    }   
  }
  
  /***************************************************************************
  **
  ** Return the Rectangle used by the single path ot the tree
  */
  
  public Rectangle getBoundsForSinglePath(NetModuleLinkageProperties nmlp, String linkID) {
                                                               
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;    

    Iterator<LinkBusDrop> dit = nmlp.getDrops();
    NetModuleBusDrop itemDrop = null;
    while (dit.hasNext()) {
      NetModuleBusDrop drop = (NetModuleBusDrop)dit.next();
      String ref = drop.getTargetRef();
      if (ref == null) {
        continue;
      }
      if (ref.equals(linkID)) {
        itemDrop = drop;
        break;
      }
    }
    Iterator<LinkSegment> sit = null;
    if (itemDrop != null) {
      sit = nmlp.getSegmentsToRoot(itemDrop).iterator();
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
    
    if (minX == Double.POSITIVE_INFINITY) {
      return (null);
    }
 
    return (new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY)));
  }  
  
 /***************************************************************************
  **
  ** Get information on potential line style modulation for a model
  */
  
  public DrawTreeModelDataSource.ModelLineStyleModulation 
    getModelLineStyleModulation(DataAccessContext icx) {
    
    DrawTreeModelDataSource.ModelLineStyleModulation retval = 
      new DrawTreeModelDataSource.ModelLineStyleModulation();
    
    retval.linkModulation = DisplayOptions.NO_LINK_ACTIVITY_DISPLAY;
    retval.checkForActive = false;
    retval.branchRenderMode = DisplayOptions.NO_BUS_BRANCHES;
    retval.forModules = true;
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Get information on line style modulation for a link
  */
  
  public DrawTreeModelDataSource.LinkLineStyleModulation 
    getLinkLineStyleModulation(DataAccessContext icx, String linkID, LinkProperties lp, 
                               ModelLineStyleModulation modulationInfo) {
           
     
    GenomeSource gSrc = icx.getGenomeSource();
    Genome genome = icx.getGenome();
    
    if (!lp.linkIsInModel(gSrc, genome, icx.oso, linkID)) {
      return (null);
    }
 
    DrawTreeModelDataSource.LinkLineStyleModulation retval = new DrawTreeModelDataSource.LinkLineStyleModulation();

    NetModuleLinkageProperties nmlp = (NetModuleLinkageProperties)lp;
    NetworkOverlay no = gSrc.getOverlayOwnerFromGenomeKey(genome.getID()).getNetworkOverlay(nmlp.getOverlayID());
    NetModuleLinkage link = no.getLinkage(linkID);
    retval.sign = link.getSign();
    retval.perLinkActivity = null;
    retval.perLinkForEvidence = null;
    retval.isActive = true;
    retval.targetOffset = 0.0;
    if (retval.sign == NetModuleLinkage.POSITIVE) {
      retval.targetOffset = POSITIVE_DROP_OFFSET_MOD_;
    } else if (retval.sign == NetModuleLinkage.NEGATIVE) {
      retval.targetOffset = NEGATIVE_DROP_OFFSET_MOD_;
    }
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Handle Model data 
  */
  
  public DrawTreeModelDataSource.ModelDataForTip 
    getModelDataForTip(DataAccessContext icx, String linkID, LinkProperties lp) {    
    
    DrawTreeModelDataSource.ModelDataForTip retval = new DrawTreeModelDataSource.ModelDataForTip();
    GenomeSource gSrc = icx.getGenomeSource();
    Genome genome = icx.getGenome();
      
    NetModuleLinkageProperties nmlp = (NetModuleLinkageProperties)lp;
    NetworkOverlay no = gSrc.getOverlayOwnerFromGenomeKey(genome.getID()).getNetworkOverlay(nmlp.getOverlayID());
    NetModuleLinkage link = no.getLinkage(linkID);
    retval.sign = link.getSign();
    retval.isActive = true;
    retval.padWidth = NetModuleFree.PAD_SIZE;
     
    LinkSegmentID segID = nmlp.isDirect() ? LinkSegmentID.buildIDForDirect(linkID) : LinkSegmentID.buildIDForEndDrop(linkID);
    LinkSegment geom = nmlp.getSegmentGeometryForID(segID, icx, true);
    retval.arrival = new Vector2D(1.0, 0.0);  // Worst-case: should a fall-through ever happen?
    double segLen = geom.getLength();
    if (segLen > 0.0) {      
      retval.arrival = geom.getRun();
    } else if (!nmlp.isDirect()) {// for zero-length end drops (oddball case)  
      List<LinkSegmentID> segs = nmlp.getSegmentIDsToRootForEndDrop(nmlp.getDrop(segID));
      int numSegs = segs.size();
      for (int i = 0; i < numSegs; i++) {
        LinkSegmentID nextID = segs.get(i);
        geom = nmlp.getSegmentGeometryForID(nextID, icx, true);
        if (geom.getLength() > 0.0) {
          retval.arrival = geom.getRun();
          break;
        }
      }
    }

    retval.lanLoc = (retval.sign == NetModuleLinkage.NEGATIVE) ? retval.arrival.scaled(-NEGATIVE_DROP_OFFSET_MOD_).add(geom.getEnd()) : (Point2D)geom.getEnd().clone();
    retval.negLength = NEG_FOOT_MOD_;
    retval.negThick = NEG_THICK_MOD_;
    
    retval.plusArrowDepth = PLUS_ARROW_DEPTH_MOD_;
    retval.positiveDropOffset = POSITIVE_DROP_OFFSET_MOD_;
    retval.plusArrowHalfWidth = PLUS_ARROW_HALF_WIDTH_MOD_; 
    retval.thickThick = ResolvedDrawStyle.THICK_THICK_MOD; 
    retval.tipFudge = TIP_FUDGE_MOD_;
    retval.levelFudge = (retval.sign == NetModuleLinkage.NEGATIVE) ? LEVEL_FUDGE_NEG_ : LEVEL_FUDGE_;
    retval.hasDiamond = false;

    return (retval);   
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

}
