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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleBusDrop;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Common code for getting link rendered into a LinkPlacementGrid
*/

public class PlacementGridSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public PlacementGridSupport() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Render the link tree to a pattern grid
  */
  
	public void renderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, Set<String> skipLinks, 
                                    String overID, DataAccessContext irx) {
    Genome genome = irx.getCurrentGenome();
		LinkPathInfo paths = getBusPath(lp, irx, null, null, skipLinks);
		if (paths == null) {
			return;
		}
		Set<String> allForSource = genome.getOutboundLinks(lp.getSourceTag());
		if (skipLinks != null) {
			allForSource.removeAll(skipLinks);
		}
		grid.drawLink(lp.getSourceTag(), paths.path, lp, allForSource);
		return;
	}
  
  /***************************************************************************
  **
  ** Render the link to a pattern grid
  */

  public void multiRenderToPlacementGrid(NetModuleLinkageProperties lp, StaticDataAccessContext irx, LinkPlacementGrid grid, Map<Point2D, Map<String, Point>> multiLinkData) {
    
    MultiRenderLinkPathInfo paths = getMultiBusPaths(lp, irx, multiLinkData);
    if (paths == null) {
      return;
    }
 
    Iterator<String> mldit = paths.srcToPaths.keySet().iterator();
    while (mldit.hasNext()) {
      String mid = mldit.next();
      LinkPlacementGrid.DecoratedPath dp = paths.srcToPaths.get(mid);
      grid.drawLinkWithUserData(mid, dp, lp, new HashSet<String>(lp.getLinkageList())); 
    }
    return;
  }
 
 /***************************************************************************
  **
  ** Answer if we can render the link to a pattern grid
  */
  
  public boolean canRenderToPlacementGrid(LinkProperties lp, LinkPlacementGrid grid, 
                                          Set<LinkPlacementGrid.PointPair> dropSet, 
                                          String overID, DataAccessContext irx) { 
    Genome genome = irx.getCurrentGenome();
    HashMap<String, String> targMap = new HashMap<String, String>();
    HashMap<String, GenomeInstance.GroupTuple> tupMap = 
      ((overID == null) && (genome instanceof GenomeInstance)) ? new HashMap<String, GenomeInstance.GroupTuple>() : null;    
    Map<LinkPlacementGrid.PointPair, List<String>> allSegsMap = fillMaps(lp, irx, targMap, tupMap, dropSet);
    return (grid.canDrawLink(lp.getSourceTag(), lp, allSegsMap, targMap, tupMap));
  }

  /***************************************************************************
  **
  ** Get the map of point pairs to links
  */
  
  public Map<LinkPlacementGrid.PointPair, List<String>> getPointPairMap(LinkProperties lp,
                                                                        String overID, 
                                                                        DataAccessContext irx) {
    Genome genome = irx.getCurrentGenome();
    HashMap<String, String> dummy = new HashMap<String, String>();
    HashMap<String, GenomeInstance.GroupTuple> tupMap = 
      ((overID == null) && (genome instanceof GenomeInstance)) ? new HashMap<String, GenomeInstance.GroupTuple>() : null;    
    return (fillMaps(lp, irx, dummy, tupMap, null));
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
 /***************************************************************************
  **
  ** Get the map of point pairs to links for the given link, and fill target map
  */
  
  private Map<LinkPlacementGrid.PointPair, List<String>> fillMaps(LinkProperties lp,
                                                                  DataAccessContext icx,
                                                                  Map<String, String> targMap, 
                                                                  Map<String, GenomeInstance.GroupTuple> tupMap, 
                                                                  Set<LinkPlacementGrid.PointPair> dropSet) {
    
    HashMap<LinkPlacementGrid.PointPair, String> linkMap = new HashMap<LinkPlacementGrid.PointPair, String>();
    //
    // FIX ME?  targ map maps seg points to link IDs.  Coincident
    // target points for overlapping node landing pads (rare)
    // are thus not accounted for correctly.
    //
    LinkPlacementGrid.PointPair rootPair = null;
    rootPair = new LinkPlacementGrid.PointPair();
    getBusPath(lp, icx, linkMap, rootPair, null);
     
    Map<LinkPlacementGrid.PointPair, List<String>> retval = lp.buildSegmentToLinksMap();
    mergeLinkMaps(lp, icx, retval, linkMap, targMap, tupMap, rootPair, dropSet);
    return (retval);
  }

 /***************************************************************************
  **
  ** Merge links maps
  */
  
  private void mergeLinkMaps(LinkProperties lp,
                             DataAccessContext icx,
                             Map<LinkPlacementGrid.PointPair, List<String>> allSegsMap, 
                             Map<LinkPlacementGrid.PointPair, String> linkMap, 
                             Map<String, String> targMap, 
                             Map<String, GenomeInstance.GroupTuple> tupMap,
                             LinkPlacementGrid.PointPair rootPair, Set<LinkPlacementGrid.PointPair> dropSet) {
    
    if ((rootPair != null) && ((dropSet == null) || (!dropSet.contains(rootPair)))) {
      Iterator<String> vit = linkMap.values().iterator();
      List<String> rootList = new ArrayList<String>();
      while (vit.hasNext()) {
        String lid = vit.next();
        rootList.add(lid);
      }
      allSegsMap.put(rootPair, rootList);
    }
    
    Genome genome = icx.getCurrentGenome();
    
    Iterator<LinkPlacementGrid.PointPair> lmkit = linkMap.keySet().iterator();
    while (lmkit.hasNext()) {
      LinkPlacementGrid.PointPair pair = lmkit.next();
      if ((dropSet != null) && (dropSet.contains(pair))) {
        continue;
      }
      List<String> asList = allSegsMap.get(pair);
      if (asList == null) {
        asList = new ArrayList<String>();
        allSegsMap.put(pair, asList);
      }
      String linkID = linkMap.get(pair);
      asList.add(linkID);
      
      targMap.put(linkID, lp.getLinkTarget(genome, linkID));
      
      if (tupMap != null) {
        tupMap.put(linkID, ((GenomeInstance)genome).getRegionTuple(linkID));
      }      
    }
    return;
  }

  /***************************************************************************
  **
  ** Figure out a path for the bus linkage
  */
  
  private LinkPathInfo getBusPath(LinkProperties bp,
                                  DataAccessContext icx, 
                                  Map<LinkPlacementGrid.PointPair, String> linkMap, 
                                  LinkPlacementGrid.PointPair rootPair,
                                  Set<String> skipLinks) {
    //
    // Get the drops first, which will tell us what segments 
    // we need to draw back to the root
    //
      
    GeneralPath usePath = new GeneralPath();

    LinkPathInfo retval = new LinkPathInfo();
    retval.path = usePath;
    retval.segSet = new HashSet<LinkSegment>();

    Genome genome = icx.getCurrentGenome();
    GenomeSource gSrc = icx.getGenomeSource();
    //
    // Handle the direct, no segment case:
    //
    
    if (bp.isDirect()) {
      LinkBusDrop endDrop = bp.getTargetDrop();
      String busID = endDrop.getTargetRef();
      if (!bp.linkIsInModel(gSrc, genome, icx.getOSO(), busID)) {
        return (null);
      }
      if ((skipLinks == null) || !skipLinks.contains(busID)) {
        LinkSegment fakeDirect = bp.getDirectLinkPath(icx);
        processDropSegmentIntoPaths(fakeDirect, endDrop, retval, bp, linkMap, rootPair, busID, true);
      }
    } else {
      //
      // Process the drops into the stroke paths.  Note we only draw the root drop if there is
      // at least one link going someplace!
      //
      boolean haveOneLink = false;
      LinkSegment rootSeg = null;
      LinkBusDrop rootDrop = null;
      Iterator<LinkBusDrop> dit = bp.getDrops();   
      while (dit.hasNext()) {
        LinkBusDrop drop = dit.next();
        String busID = drop.getTargetRef();  // may be null...
        int dropType = drop.getDropType();
        if (dropType != LinkBusDrop.START_DROP) {
          if (!bp.linkIsInModel(gSrc, genome, icx.getOSO(), busID)) {
            continue;
          }
          if ((skipLinks != null) && skipLinks.contains(busID)) {
            continue;
          }
          haveOneLink = true;
        } else {
          rootDrop = drop;
        }
        LinkSegmentID segID = LinkSegmentID.buildIDForDrop(busID);
        LinkSegment fakeSeg = bp.getSegmentGeometryForID(segID, icx, true);
        if (dropType == LinkBusDrop.START_DROP) {
          rootSeg = fakeSeg;
        } else {
          processDropSegmentIntoPaths(fakeSeg, drop, retval, bp, linkMap, rootPair, busID, false);
        }
      }
      if (haveOneLink) {
        processDropSegmentIntoPaths(rootSeg, rootDrop, retval, bp, linkMap, rootPair, null, false);
      }
    }

    //
    // Process the needed segments into stroke paths. 
    //

    Iterator<LinkSegment> sit = retval.segSet.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      Point2D strt = seg.getStart();
      Point2D end = seg.getEnd();
      if (end != null) {
        usePath.moveTo((float)strt.getX(), (float)strt.getY());
        usePath.lineTo((float)end.getX(), (float)end.getY());
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Make a combined ID
  */
  
  private String multiBusID(NetModuleLinkageProperties bp, String src) {
    return (src + "@" + bp.getID());
  }

  /***************************************************************************
  **
  ** Figure out a set of paths for links routed by a single path.  Used for
  ** module links only.  NOTE!  We use lots of null arguments for geometry
  ** (genome, layout, frc) which we can get away with because module drops
  ** already know their endpoints!  Otherwise, this is death!
  */
  
  private MultiRenderLinkPathInfo getMultiBusPaths(NetModuleLinkageProperties bp, StaticDataAccessContext icx, 
                                                   Map<Point2D, Map<String, Point>> multiLinkData) {
    //
    // Get the drops first, which will tell us what segments 
    // we need to draw back to the root
    //
     
    MultiRenderLinkPathInfo retval = new MultiRenderLinkPathInfo();
    retval.segSet = new HashSet<LinkSegment>();
    retval.srcToPaths = new HashMap<String, LinkPlacementGrid.DecoratedPath>();
    Iterator<Point2D> mldit = multiLinkData.keySet().iterator();
    while (mldit.hasNext()) {
      Point2D pt = mldit.next();
      Map<String, Point> forPt = multiLinkData.get(pt);
      Iterator<String> fpit = forPt.keySet().iterator();
      while (fpit.hasNext()) {
        String src = fpit.next();
        String mid = multiBusID(bp, src);
        retval.srcToPaths.put(mid, new LinkPlacementGrid.DecoratedPath());
      }
    }
    
    //
    // Handle the direct, no segment case:
    //
    
    if (bp.isDirect()) {
      NetModuleBusDrop endDrop = (NetModuleBusDrop)bp.getTargetDrop();
      NetModuleBusDrop startDrop = (NetModuleBusDrop)bp.getRootDrop();
      LinkSegment fakeDirect = bp.getDirectLinkPath(icx);
      Point2D dLoc = startDrop.getEnd(0.0);
      fakeDirect.setStart(dLoc);
      dLoc = endDrop.getEnd(0.0);
      fakeDirect.setEnd(dLoc);
      processDropSegmentIntoMultiPaths(fakeDirect, LinkSegmentID.buildIDForDirect(bp.getSingleLinkage()), endDrop, retval, bp, multiLinkData);
    } else {
      //
      // Process the drops into the stroke paths.  Note we only draw the root drop if there is
      // at least one link going someplace!
      //
      boolean haveOneLink = false;
      LinkSegment rootSeg = null;
      NetModuleBusDrop rootDrop = null;
      Iterator<LinkBusDrop> dit = bp.getDrops();   
      while (dit.hasNext()) {
        NetModuleBusDrop drop = (NetModuleBusDrop)dit.next();
        String busID = drop.getTargetRef();  // may be null...
        int dropType = drop.getDropType();
        if (dropType != LinkBusDrop.START_DROP) {
          haveOneLink = true;
        } else {
          rootDrop = drop;
        }
        LinkSegmentID segID = LinkSegmentID.buildIDForDrop(busID);
        // Note the segment geometry for the drops has a tweaked end, and we need
        // the actual end!
        LinkSegment fakeSeg = bp.getSegmentGeometryForID(segID, icx, true);
        Point2D dLoc = drop.getEnd(0.0);
        if (dropType == LinkBusDrop.START_DROP) {
          rootSeg = fakeSeg;          
          rootSeg.setStart(dLoc);
        } else {
          fakeSeg.setEnd(dLoc);
          processDropSegmentIntoMultiPaths(fakeSeg, segID, drop, retval, bp, multiLinkData);
        }
      }
      if (haveOneLink) {
        processDropSegmentIntoMultiPaths(rootSeg, LinkSegmentID.buildIDForStartDrop(), rootDrop, retval, bp, multiLinkData);
      }
    }

    //
    // Process the needed segments into stroke paths. 
    //

    Iterator<LinkSegment> sit = retval.segSet.iterator();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      segToMultiSeg(bp, seg, LinkSegmentID.buildIDForSegment(seg.getID()), retval, multiLinkData);
    }
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Process a drop segment to a path
  */
  
  private void processDropSegmentIntoPaths(LinkSegment seg,
                                           LinkBusDrop drop,
                                           LinkPathInfo retval,                                        
                                           LinkProperties bp,
                                           Map<LinkPlacementGrid.PointPair, String> linkMap, 
                                           LinkPlacementGrid.PointPair rootPair,
                                           String busID, boolean isDirect) {
 
    if (seg == null) {
      return;
    }
    
    Point2D strt = seg.getStart();
    retval.path.moveTo((float)strt.getX(), (float)strt.getY());
    Point2D end = seg.getEnd();
    retval.path.lineTo((float)end.getX(), (float)end.getY());
    if (linkMap != null) {
      if ((busID != null) && !isDirect) {
        linkMap.put(new LinkPlacementGrid.PointPair(strt, end), busID);
      } else {
        rootPair.start = strt;
        rootPair.end = end;
      }
    }
    retval.segSet.addAll(bp.getSegmentsToRoot(drop));   
    return;
  }   
  
  
  /***************************************************************************
  **
  ** Process a drop segment to a path
  */
  
  private void processDropSegmentIntoMultiPaths(LinkSegment seg,
                                                LinkSegmentID truID,
                                                LinkBusDrop drop,
                                                MultiRenderLinkPathInfo retval,                                        
                                                NetModuleLinkageProperties bp,
                                                Map<Point2D, Map<String, Point>> multiLinkData) {
 
    if (seg == null) {
      return;
    }
    segToMultiSeg(bp, seg, truID, retval, multiLinkData);
    retval.segSet.addAll(bp.getSegmentsToRoot(drop));   
    return;
  }
  
  
  /***************************************************************************
  **
  ** Process a segment into multiple per-source paths
  */
  
  private void segToMultiSeg(NetModuleLinkageProperties bp, LinkSegment seg, LinkSegmentID truID,
                             MultiRenderLinkPathInfo retval, Map<Point2D, Map<String, Point>> multiLinkData) {
 
    if (seg == null) {
      return;
    }    
    Point2D strt = seg.getStart();
    Point2D end = seg.getEnd();
    if (end == null) {
      return;
    }
    // Endpoints are typically off grid:
    UiUtil.forceToGrid(strt, UiUtil.GRID_SIZE);
    UiUtil.forceToGrid(end, UiUtil.GRID_SIZE);
    
    Map<String, Point> forStartPt = multiLinkData.get(strt);
    Map<String, Point> forEndPt = multiLinkData.get(end);
    if ((forStartPt == null) || (forEndPt == null)) {
      System.err.println("Unexpected lack of point map: " + strt + " and/or " + end + " keys " + multiLinkData.keySet());
      return;
    }
     
    //
    // Figure out the range of possible trace offsets for this segment:
    //
    int myNormCanon = seg.getNormal().canonicalDir();
    boolean xVaries = (myNormCanon == Vector2D.EAST) || (myNormCanon == Vector2D.WEST);
    MinMax traceRange = new MinMax();
    traceRange.init();
    Iterator<String> fpit = forStartPt.keySet().iterator();
    while (fpit.hasNext()) {
      String src = fpit.next();  
      Point offsetForSrc = forStartPt.get(src);
      int useVal = (xVaries) ? offsetForSrc.x : offsetForSrc.y;
      traceRange.update(useVal);
    } 
    
    fpit = forStartPt.keySet().iterator();
    while (fpit.hasNext()) {
      String src = fpit.next();  
      String mid = multiBusID(bp, src);
      LinkPlacementGrid.DecoratedPath decoPath = retval.srcToPaths.get(mid);
      GeneralPath pathForSrc = decoPath.getPath();
      Point offsetForSrc = forStartPt.get(src);
      Point offsetForEnd = forEndPt.get(src);
      if ((offsetForSrc != null) && (offsetForEnd != null)) {
        double useX = strt.getX() + (offsetForSrc.getX() * UiUtil.GRID_SIZE);
        double useY = strt.getY() + (offsetForSrc.getY() * UiUtil.GRID_SIZE);
        pathForSrc.moveTo((float)useX, (float)useY);
        LinkPlacementGrid.DecoInfo di = new LinkPlacementGrid.DecoInfo(bp.getID(), truID, src, strt, myNormCanon, offsetForSrc, traceRange);        
        decoPath.decorate(new Point2D.Double(useX, useY), di);
        useX = end.getX() + (offsetForEnd.getX() * UiUtil.GRID_SIZE);
        useY = end.getY() + (offsetForEnd.getY() * UiUtil.GRID_SIZE);        
        pathForSrc.lineTo((float)useX, (float)useY);        
      }
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to return bus paths
  */
  
  private class LinkPathInfo {
    GeneralPath path;
    HashSet<LinkSegment> segSet;
  }
  
  /***************************************************************************
  **
  ** Used to return multiple bus links
  */
  
  private class MultiRenderLinkPathInfo {
    Map<String, LinkPlacementGrid.DecoratedPath> srcToPaths;
    HashSet<LinkSegment> segSet;
  }
}
