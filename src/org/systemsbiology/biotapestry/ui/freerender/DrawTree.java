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

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.BasicStroke;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.IRenderer;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.ui.ResolvedDrawStyle;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.LinkSegmentExport;
import org.systemsbiology.biotapestry.ui.modelobjectcache.LinkageExportForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModalShapeContainer;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkSegmentExport;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.DrawMode;
import org.systemsbiology.biotapestry.ui.modelobjectcache.NetModuleLinkageExportForWeb;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** This helps to render a linkage
*/

public class DrawTree {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  // PRE-RESIZE:
  private static final int REG_THICK        = 3;
  private static final int SELECTED_THICK   = 5;
  private static final int SELECTED_DELTA   = SELECTED_THICK - REG_THICK;

  private static final float PAD_RADIUS_ = 5.0F;
  private static final float BB_RADIUS_ = PAD_RADIUS_;

  //
  // Major layers:
  //

  public static final Integer MAJOR_SELECTED_LAYER_ = new Integer(1);
  public static final Integer INACTIVE_PATH_LAYER = new Integer(2);
  public static final Integer ACTIVE_PATH_LAYER = new Integer(3);

  //
  // Minor layers:
  //

  private static final Integer MINOR_SELECTED_LAYER_ = new Integer(0);
  private static final Integer MINOR_NORMAL_LAYER_ = new Integer(1);
  private static final Integer MINOR_BRANCH_LAYER_ = new Integer(2);
  private static final Integer MINOR_BUBBLE_LAYER_ = new Integer(3);
  private static final Integer MINOR_PAD_LAYER_ = new Integer(4);
  private static final Integer MINOR_TARGET_LABEL_LAYER_ = new Integer(5);
  static final Integer MINOR_TEXT_LAYER = new Integer(6);

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<LinkSegmentID, DrawTreeSegment> segments_;
  private HashMap<String, LinkSegmentID> startForLink_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DrawTree(LinkProperties bp,
                  DrawTreeModelDataSource mds,
                  Intersection selected, DataAccessContext rcx) {
    segments_ = new HashMap<LinkSegmentID, DrawTreeSegment>();
    startForLink_ = new HashMap<String, LinkSegmentID>();
    DrawTreeModelDataSource.ModelLineStyleModulation lsMod =
      mds.getModelLineStyleModulation(rcx);

    //
    // Crank through drops.  Get segmentID for each end drop, get a path
    // back up to root.
    //

    Iterator<LinkBusDrop> dit = bp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        String linkID = drop.getTargetRef();
        PerLinkDrawStyle perLink = drop.getDrawStyleForLink();
        DrawTreeModelDataSource.LinkLineStyleModulation llsm =
          mds.getLinkLineStyleModulation(rcx, linkID, bp, lsMod);
        // VFN may not include some drops!
        if (llsm == null) {
          continue;
        }
        DrawTreeSegment lastDts = null;
        List<LinkSegmentID> toRoot = bp.getSegmentIDsToRootForEndDrop(drop);
        int numToRoot = toRoot.size();
        for (int i = 0; i < numToRoot; i++) {
          LinkSegmentID segID = toRoot.get(i);
          DrawTreeSegment dts = segments_.get(segID);
          if (dts == null) {
            LinkSegment geomSeg = bp.getSegmentGeometryForID(segID, rcx, false);
            // Gotta tweak the target drop end so it butts up to the back of the arrowhead;
            if (segID.isDirectOrEndDrop()) {
              tweakTargetDropPath(geomSeg, llsm);
            }
            SuggestedDrawStyle drawStyle = bp.getDrawStyleForID(segID);
            dts = new DrawTreeSegment(segID, geomSeg, drawStyle);
            tagIfSelected(dts, segID, selected);
            segments_.put(segID, dts);
          }
          if (lastDts != null) {
            if (lastDts.getParent() == null) {
              lastDts.setParent(segID);
              dts.addChild();
            }
          } else {
            startForLink_.put(linkID, segID);
          }
          dts.incrementPathCount();
          if (llsm.isActive) {
            dts.setActive();
          }
          if ((perLink != null) || (llsm.perLinkForEvidence != null) || (llsm.perLinkActivity != null) || (llsm.simDiff != null)) {
            dts.addPerLinkProps(perLink, linkID, llsm.perLinkForEvidence, llsm.perLinkActivity, llsm.simDiff);
          }
          lastDts = dts;
        }
      }
    }
    registerActiveKids();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    


  /***************************************************************************
  **
  ** Render to the drawing cache
  */

  public void renderToCache(ModalShapeContainer group, 
                            DrawTreeModelDataSource mds,                          
                            LinkProperties lp, Set<String> skipDrops,
                            DataAccessContext rcx, IRenderer.Mode mode) {

    if (startForLink_.isEmpty()) {  // Nothing to draw...
      return;
    }
    
    boolean isGhosted = rcx.isGhosted();
    BasicStroke branchStroke = new BasicStroke(1);
    BasicStroke padStroke = new BasicStroke(1);
    DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();
    Color basePadCol = (isGhosted) ? dop.getInactiveGray() : Color.BLACK;
    Color padCol = applyAlphaToColor(basePadCol, rcx.getOSO());
    GeneralPath currPath = new GeneralPath();
    DrawTreeSegment lastDts = null;
    FlipStatus flipStat = null;
    boolean haveSelection = false;
    HashMap<Integer, GeneralPath> selectPaths = new HashMap<Integer, GeneralPath>();
    HashMap<Integer, BasicStroke> selectStrokes = new HashMap<Integer, BasicStroke>();

    DrawTreeModelDataSource.ModelLineStyleModulation lsMod = mds.getModelLineStyleModulation(rcx);

    //
    // Go through and resolve the draw style for each segment. That is used to order
    // the link rendering.  Build a top down tree too:
    //

    HashMap<LinkSegmentID, List<LinkSegmentID>> kiddies = new HashMap<LinkSegmentID, List<LinkSegmentID>>();
    DisplayOptions dopt = rcx.getDisplayOptsSource().getDisplayOptions();
    LinkSegmentID rootSegID = resolveDrawStyles(lp, isGhosted, kiddies, lsMod.linkModulation, lsMod.forModules, rcx.getColorResolver(), mode, dopt);
    clearTags();

    //
    // We want to render links in a particular order, trying to maintain style continuity
    // when possible to give smooth corners.  All active links go before any inactive
    // links.
    //

    DrawTreeSegment rootDts = segments_.get(rootSegID);
    ArrayList<String> activeDrawOrder = new ArrayList<String>();
    ArrayList<String> inactiveDrawOrder = new ArrayList<String>();
    orderForDrawing(segments_, kiddies, rootSegID, activeDrawOrder, inactiveDrawOrder, rootDts.isActive());

    ArrayList<String> combined = new ArrayList<String>(inactiveDrawOrder);
    combined.addAll(activeDrawOrder);

    int numDo = combined.size();

    for (int i = numDo - 1; i >= 0; i--) {  // Backwards!
      String linkID = combined.get(i);
      LinkSegmentID lsid = startForLink_.get(linkID);

      DrawTreeModelDataSource.LinkLineStyleModulation llsm =
        mds.getLinkLineStyleModulation(rcx, linkID, lp, lsMod);

      boolean isDrop = true;
      boolean skipDrop = ((skipDrops != null) && skipDrops.contains(linkID));

      lastDts = null;
      while (lsid != null) {
        DrawTreeSegment currDts = segments_.get(lsid);
        // Stop when we hit stuff already drawn:
        if (!currDts.getWhoDraws().equals(linkID)) {
        //if (currDts.isDrawn()) {
          break;
        } else {
          lsid = currDts.getParent();
        }
        //
        // Figure out what path we are using:
        //

        flipStat = pathFlipper(group, isGhosted, currDts, lastDts, flipStat, rcx);
        Point2D strt = currDts.getStart();
        Point2D end = currDts.getEnd();
        currPath = flipStat.currPath;

        switch (flipStat.flipStatus) {
          case FlipStatus.NEW_PATH:
            currPath.moveTo((float)end.getX(), (float)end.getY());
            if (skipDrop && isDrop) {
              currPath.moveTo((float)strt.getX(), (float)strt.getY());
            } else {
              currPath.lineTo((float)strt.getX(), (float)strt.getY());
            }
            break;
          case FlipStatus.SAME_PATH_JUMP:
            currPath.moveTo((float)end.getX(), (float)end.getY());
            if (skipDrop && isDrop) {
              currPath.moveTo((float)strt.getX(), (float)strt.getY());
            } else {
              currPath.lineTo((float)strt.getX(), (float)strt.getY());
            }
            break;
          case FlipStatus.SAME_PATH_CONTINUE:
            if (skipDrop && isDrop) {
              currPath.moveTo((float)strt.getX(), (float)strt.getY());
            } else {
              currPath.lineTo((float)strt.getX(), (float)strt.getY());
            }
            break;
          default:
            throw new IllegalStateException();
        }


        if (currDts.isSelected()) {
          int thick = flipStat.styleUsed.getThickness();
          Integer thickObj = new Integer(thick);
          GeneralPath selectionPath = selectPaths.get(thickObj);
          if (selectionPath == null) {
            selectionPath = new GeneralPath();
            selectPaths.put(thickObj, selectionPath);
            int selThick = thick + SELECTED_DELTA;
            if (selThick < SELECTED_THICK) {
              selThick = SELECTED_THICK;
            }
            BasicStroke selectedStroke = new BasicStroke(selThick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
            selectStrokes.put(thickObj, selectedStroke);
          }
          haveSelection = true;
          selectionPath.moveTo((float)strt.getX(), (float)strt.getY());
          selectionPath.lineTo((float)end.getX(), (float)end.getY());
        }

        //
        // Draw these at the foot of the segment, if appropriate:
        //
        if (isDrop) {
          if (!skipDrop) {
          	renderTipToCache(group, rcx, mds, currDts, linkID, lp, isGhosted);
          }
          isDrop = false;
        } else {
          if ((lsMod.branchRenderMode != DisplayOptions.NO_BUS_BRANCHES) && currDts.needsBranchPoint()) {
            Color baseCol = flipStat.styleUsed.getColor();
            Color useCol = applyAlphaToColor(baseCol, rcx.getOSO());
            drawABusBranch(group, end, useCol, padCol, lsMod.branchRenderMode,
                           branchStroke, currDts.isActive());
          }
          if (rcx.getShowBubbles() && !isDrop) {
            drawAPad(group, end, padCol, padStroke, currDts.isActive(), rcx.getPixDiam());
          }
        }
        currDts.setTag();
        lastDts = currDts;
      }
    }
    if (haveSelection) {
      Iterator<Integer> spkit = selectPaths.keySet().iterator();
      while (spkit.hasNext()) {
        Integer thickObj = spkit.next();
        GeneralPath selectionPath = selectPaths.get(thickObj);
        BasicStroke selectedStroke = selectStrokes.get(thickObj);

        ModelObjectCache.SegmentedPathShape ms = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, Color.red, selectedStroke, selectionPath);
        group.addShape(ms, MAJOR_SELECTED_LAYER_, MINOR_NORMAL_LAYER_);
      }
    }
    if (flipStat != null) {
      flushPath(group, flipStat, rcx);
    }

    return;
  }

  public void exportLinkages(LinkageExportForWeb lefw, StaticDataAccessContext rcx, LinkProperties lp) {
	  Genome genome = rcx.getCurrentGenome();
	  Iterator<LinkSegmentID> iter = segments_.keySet().iterator();

	  while (iter.hasNext()) {
		  
		  LinkSegmentID lsid = iter.next();
		  LinkSegment ls = lp.getSegmentGeometryForID(lsid, rcx, false);
		  ResolvedDrawStyle rds = segments_.get(lsid).getResolvedStyle();
		  
		  // Selection thickness has to be exported
		  int thick = rds.getThickness();
		  Color color = rds.getColor();

		  int selThick = thick + SELECTED_DELTA;
		  if (selThick < SELECTED_THICK) {
			  selThick = SELECTED_THICK;
		  }
		  
		  LinkSegmentExport lse = new LinkSegmentExport(lsid, ls, thick, selThick, color, rds.getLineStyle());
		  lefw.addSegmentExport(lse);
		  
		  String linkID = null;
		  
		  if (lsid.isForEndDrop()) {
			  linkID = lsid.getEndDropLinkRef();
		  }
		  else if (lsid.isDirect()) {
			  linkID = lsid.getDirectLinkRef();
		  }
		  
		  if (linkID != null) {
			  Linkage linkage = genome.getLinkage(linkID);
			  if (linkage != null) {
			  
				  lefw.addLinkage(linkID, linkage);
			  }
		  }
	  }

	  return;
  }
  
  public void exportNetModuleLinkages(NetModuleLinkageExportForWeb lefw, DataAccessContext rcx, String ovrID, NetModuleLinkageProperties lp) {
	  NetworkOverlay overlay = rcx.getCurrentGenome().getNetworkOverlay(ovrID);
	  Iterator<LinkSegmentID> iter = segments_.keySet().iterator();

	  while (iter.hasNext()) {
		  LinkSegmentID lsid = iter.next();
		  LinkSegment ls = lp.getSegmentGeometryForID(lsid, rcx, false);
		  ResolvedDrawStyle rds = segments_.get(lsid).getResolvedStyle();
		  
		  // Selection thickness has to be exported
		  int thick = rds.getThickness();
		  Color color = rds.getColor();

		  int selThick = thick + SELECTED_DELTA;
		  if (selThick < SELECTED_THICK) {
			  selThick = SELECTED_THICK;
		  }
		  
		  String linkID = null;
		  
		  if (lsid.isForEndDrop()) {
			  linkID = lsid.getEndDropLinkRef();
		  }
		  else if (lsid.isDirect()) {
			  linkID = lsid.getDirectLinkRef();
		  }
		  
		  NetModuleLinkSegmentExport lse = new NetModuleLinkSegmentExport(lsid, ls, thick, selThick, color, rds.getLineStyle());
		  
		  if (linkID != null) {
			  NetModuleLinkage linkage = overlay.getLinkage(linkID);
			  if (linkage != null) {
				  lefw.addLinkage(linkID, linkage);
				  lse.addLinkId(linkID);
			  }
		  }
		  else {
			  Set<String> linkages = lp.resolveLinkagesThroughSegment(lsid);
			  for (Iterator<String> link_iter = linkages.iterator(); link_iter.hasNext(); ) {
				  String segment_link_id = link_iter.next();
				  lse.addLinkId(segment_link_id);
			  }
		  }
		  
		  lefw.addSegmentExport(lse);		  
	  }

	  return;
  }  
  
  public HashMap<String, Object> exportLinkInfoForAll() {
	  HashMap<String, Object> retval = new HashMap<String, Object>();
	  
	  
	  
	  return retval;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clear breadcrumbs
  */

  private void clearTags() {
    Iterator<DrawTreeSegment> vit = segments_.values().iterator();
    while (vit.hasNext()) {
      DrawTreeSegment dts = vit.next();
      dts.clearTag();
    }
    return;
  }

  /***************************************************************************
  **
  ** Count up active kids
  */

  private void registerActiveKids() {
    Iterator<DrawTreeSegment> vit = segments_.values().iterator();
    while (vit.hasNext()) {
      DrawTreeSegment dts = vit.next();
      if (!dts.isActive()) {
        continue;
      }
      LinkSegmentID segID = dts.getParent();
      if (segID != null) {
        DrawTreeSegment pdts = segments_.get(segID);
        pdts.addActiveChild();
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Figure out the styles to use for drawing
  */

  private LinkSegmentID resolveDrawStyles(LinkProperties lp, boolean isGhosted,
                                          Map<LinkSegmentID, List<LinkSegmentID>> kiddies, 
                                          int activityDrawChange, boolean forModules, 
                                          ColorResolver cRes, IRenderer.Mode mode, DisplayOptions dopt) {
    LinkSegmentID retval = null;
    clearTags();
    Iterator<String> kit = startForLink_.keySet().iterator();
    while (kit.hasNext()) {  // For each link...
      String linkID = kit.next();
      LinkSegmentID lsid = startForLink_.get(linkID);
      DrawTreeSegment lastDts = null;
      while (lsid != null) {
        DrawTreeSegment currDts = segments_.get(lsid);
        LinkSegmentID currID = currDts.getID();
        if (lastDts != null) {
          // Build a top-down tree for determining drawing order:
          List<LinkSegmentID> kids = kiddies.get(currID);
          if (kids == null) {
            kids = new ArrayList<LinkSegmentID>();
            kiddies.put(currID, kids);
          }
          kids.add(lastDts.getID());
        }
        // Stop when we hit stuff already tagged:
        if (currDts.isDrawn()) {
          break;
        } else {
          lsid = currDts.getParent();
        }
        currDts.setResolvedStyle(currDts.resolveDrawStyle(lp, isGhosted, activityDrawChange, forModules, cRes, mode, dopt));
        currDts.setTag();
        lastDts = currDts;
        if (lsid == null) {
          retval = currID;
        }
      }
    }
    if (retval == null) {
      throw new IllegalStateException();
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Order the links for drawing
  */

  private String orderForDrawing(Map<LinkSegmentID, DrawTreeSegment> segments, 
                                 Map<LinkSegmentID, List<LinkSegmentID>> kiddies,
                                 LinkSegmentID currSegID,
                                 List<String> activeDrawList, List<String> inactiveDrawList,
                                 boolean doingActives) {

    //
    // Start at the root.  Scan the children to find the thickest.  Follow it down to
    // the leaf.  Do this in a depth-first fashion to build up the order.
    //

    int matchKey = Integer.MIN_VALUE;
    int colorMatchKey = matchKey + 1;

    DrawTreeSegment dts = segments.get(currSegID);
    ResolvedDrawStyle currRds = dts.getResolvedStyle();
    Color currColor = currRds.getColor();
    List<LinkSegmentID> kids = kiddies.get(currSegID);
    if (kids == null) {
      String linkID = (currSegID.isDirect()) ? currSegID.getDirectLinkRef() : currSegID.getEndDropLinkRef();
      List<String> useList = (doingActives) ? activeDrawList : inactiveDrawList;
      useList.add(linkID);
      dts.setWhoDraws(linkID);
      return (linkID);
    }
    int numKids = kids.size();
    TreeMap<Integer, List<LinkSegmentID>> drawOrder = new TreeMap<Integer, List<LinkSegmentID>>();
    for (int i = 0; i < numKids; i++) {
      LinkSegmentID doSegID = kids.get(i);
      DrawTreeSegment kidDts = segments.get(doSegID);
      ResolvedDrawStyle rds = kidDts.getResolvedStyle();
      // This is the core decision process of who draws first (actually, last...)
      // First is exact style match, then any color matches, finally in order of thickness...
      int keyVal;
      if (currRds.equals(rds)) {
        keyVal = matchKey;
      } else if (currColor.equals(rds.getColor())) {
        keyVal = colorMatchKey;
      } else {
        keyVal = -1 * rds.getThickness();
      }
      Integer key = new Integer(keyVal);
      List<LinkSegmentID> kidsForKey = drawOrder.get(key);
      if (kidsForKey == null) {
        kidsForKey = new ArrayList<LinkSegmentID>();
        drawOrder.put(key, kidsForKey);
      }
      kidsForKey.add(doSegID);
    }

    String retval = null;
    Iterator<List<LinkSegmentID>> doit = drawOrder.values().iterator();
    while (doit.hasNext()) {
      List<LinkSegmentID> toDo = doit.next();
      int numtoDo = toDo.size();
      for (int i = 0; i < numtoDo; i++) {
        LinkSegmentID doSegID = toDo.get(i);
        DrawTreeSegment kidDts = segments.get(doSegID);
        boolean kidIsActive = kidDts.isActive();
        String firstLink = orderForDrawing(segments, kiddies, doSegID,
                                           activeDrawList, inactiveDrawList, kidIsActive);
        if (dts.getWhoDraws() == null) {
          dts.setWhoDraws(firstLink);
          retval = firstLink;
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Flip and flush
  */

	private FlipStatus pathFlipper(ModalShapeContainer group, boolean isGhosted,
			DrawTreeSegment currDts, DrawTreeSegment lastDts, FlipStatus lastStat, DataAccessContext rcx) {
		//
		// Figure out current style:
		//

		ResolvedDrawStyle currStyle = currDts.getResolvedStyle();
		Integer pathLayer = (currDts.isActive()) ? ACTIVE_PATH_LAYER : INACTIVE_PATH_LAYER;

		//
		// Flip the path if the drawing style or activity changes
		//

		if (lastStat == null) {
			return (new FlipStatus(FlipStatus.SAME_PATH_JUMP, new GeneralPath(),
					currStyle, pathLayer));
		} else if (!currStyle.equals(lastStat.styleUsed)
				|| !pathLayer.equals(lastStat.currentLayer)) {
			flushPath(group, lastStat, rcx);
			return (new FlipStatus(FlipStatus.NEW_PATH, new GeneralPath(), currStyle,
					pathLayer));
		} else if (lastDts == null) { // New drop comes along...
			return (new FlipStatus(FlipStatus.SAME_PATH_JUMP, lastStat.currPath,
					currStyle, pathLayer));
		} else {
			return (new FlipStatus(FlipStatus.SAME_PATH_CONTINUE, lastStat.currPath,
					currStyle, pathLayer));
		}
	}

  /***************************************************************************
  **
  ** Flush
  */

  private void flushPath(ModalShapeContainer group, FlipStatus flipStat, DataAccessContext rcx) {
    Integer pathLayer = flipStat.currentLayer;
    Color baseCol = flipStat.styleUsed.getColor();
    Color col = applyAlphaToColor(baseCol, rcx.getOSO());
    BasicStroke stroke = flipStat.styleUsed.calcStroke();

    ModelObjectCache.SegmentedPathShape sps = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, stroke, flipStat.currPath);
    group.addShape(sps, pathLayer, MINOR_NORMAL_LAYER_);

    return;
  }

  /***************************************************************************
  **
  ** Set the segment as selected if it is
  */

  private void tagIfSelected(DrawTreeSegment dts, LinkSegmentID segID,
                             Intersection selected) {
    if (selected != null) {
      MultiSubID sub = (MultiSubID)selected.getSubID();
      if ((sub == null) || sub.getParts().contains(segID)) {
        dts.setSelected();
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Render the link tip to the cache
  */

	private void renderTipToCache(ModalShapeContainer group, DataAccessContext icx,
			DrawTreeModelDataSource mds, 
			DrawTreeSegment dts, String linkID, LinkProperties lp, boolean isGhosted) {

		DrawTreeModelDataSource.ModelDataForTip tipData = mds.getModelDataForTip(icx, linkID, lp);

		Integer pathLayer = (tipData.isActive) ? ACTIVE_PATH_LAYER : INACTIVE_PATH_LAYER;
		ResolvedDrawStyle rds = dts.getResolvedStyle();

		Color baseCol = rds.getColor();
		Color col = applyAlphaToColor(baseCol, icx.getOSO());

		if (tipData.sign == Linkage.NEGATIVE) {
			BasicStroke negStroke = new BasicStroke(tipData.negThick,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
			GeneralPath gp = negativeTipPath(dts, tipData);

			ModelObjectCache.SegmentedPathShape sps = new ModelObjectCache.SegmentedPathShape(DrawMode.DRAW, col, negStroke, gp);
			group.addShape(sps, pathLayer, MINOR_NORMAL_LAYER_);

		} else if (tipData.sign == Linkage.POSITIVE) {
			BasicStroke posStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND);
			GeneralPath gp = positiveTipPath(dts, tipData);

			ModelObjectCache.SegmentedPathShape sps = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, col, posStroke, gp);
			group.addShape(sps, pathLayer, MINOR_NORMAL_LAYER_);
		}

		// Fixes BT-10-27-09:2; previously required llsm.perLinkForEvidence to be
		// non-null,
		// which isn't true for diamond-only. But dropping that req'd us to check
		// forModules!
		// 11/25/09: Now the info is embedded in the tip data!
		if (tipData.hasDiamond) {
			renderEvidenceGlyphToCache(group, icx.getCurrentGenome(), dts, linkID, isGhosted, tipData, icx);
		}

		return;
	}

  /***************************************************************************
  **
  ** Colors for net module links need to be modified by alpha values
  */

  private Color applyAlphaToColor(Color col, OverlayStateOracle oso) {
    if (oso == null) {
      return (col);
    }
    double alpha = oso.getCurrentOverlaySettings().regionBoundaryAlpha;
    if (alpha == 1.0) {
      return (col);
    }
    float[] coVals = new float[4];
    col.getComponents(coVals);
    Color drawCol = new Color(coVals[0], coVals[1], coVals[2], (float)alpha);
    return (drawCol);
  }

  /***************************************************************************
  **
  ** Gotta tweak the target drop end so it butts up to the back of the arrowhead
  */

  private void tweakTargetDropPath(LinkSegment ls, DrawTreeModelDataSource.LinkLineStyleModulation llsm) {

    Vector2D run = ls.getRun();
    Point2D end = ls.getEnd();

    double offx = end.getX() - (run.getX() * llsm.targetOffset);
    double offy = end.getY() - (run.getY() * llsm.targetOffset);
    ls.setEnd(new Point2D.Double(offx, offy));
    return;
  }

  /***************************************************************************
  **
  ** Get the path for a positive tip
  */

  private GeneralPath positiveTipPath(DrawTreeSegment dts, DrawTreeModelDataSource.ModelDataForTip mdt) {


    Vector2D arrival = mdt.arrival;
    Point2D lanLoc = mdt.lanLoc;

    Vector2D run = dts.getRun();
    Vector2D norm = dts.getNormal();
    Point2D end = dts.getEnd();
    Point2D start = dts.getStart();

    //
    // Things get really weird close into odd-width nodes like boxes and text
    // If the last segment is a fragment very, very close to the landing point, we ignore
    // its direction. We also fall back on the landing location as the end if last point
    // is closer to the end than the pad point.
    //
    Vector2D finalRun = new Vector2D(start, lanLoc);
    if (finalRun.length() <= mdt.plusArrowDepth) {
      run = arrival;
      Vector2D fromEnd = new Vector2D(lanLoc, end);
      if (fromEnd.dot(arrival) > 0.0) {
        end = lanLoc;
      }
      norm = run.normal();
    }
    GeneralPath tipPath = new GeneralPath();

    //
    // Up until the "thick" line width, we keep arrow size constant.  Above that,
    // we maintain the size delta we had at the thick width.
    //

    double halfWidth;
    int dropWidth = dts.getResolvedStyle().getThickness();
    if (dropWidth > mdt.thickThick) {
      halfWidth = mdt.plusArrowHalfWidth + ((dropWidth - mdt.thickThick) / 2.0);
    } else {
      halfWidth = mdt.plusArrowHalfWidth;
    }

    double offnegx = end.getX() - (run.getX() * (mdt.plusArrowDepth - mdt.positiveDropOffset))
                                - (norm.getX() * halfWidth);
    double offnegy = end.getY() - (run.getY() * (mdt.plusArrowDepth - mdt.positiveDropOffset))
                                - (norm.getY() * halfWidth);
    double offposx = end.getX() - (run.getX() * (mdt.plusArrowDepth - mdt.positiveDropOffset))
                                + (norm.getX() * halfWidth);
    double offposy = end.getY() - (run.getY() * (mdt.plusArrowDepth - mdt.positiveDropOffset))
                                + (norm.getY() * halfWidth);
    double tipx = end.getX() + (run.getX() * mdt.positiveDropOffset) + (run.getX() * mdt.tipFudge);
    double tipy = end.getY() + (run.getY() * mdt.positiveDropOffset) + (run.getY() * mdt.tipFudge);
    tipPath.moveTo((float)offnegx, (float)offnegy);
    tipPath.lineTo((float)offposx, (float)offposy);
    tipPath.lineTo((float)tipx, (float)tipy);
    tipPath.closePath();

    return (tipPath);
  }

  /***************************************************************************
  **
  ** Get the path for a negative tip
  */

  private GeneralPath negativeTipPath(DrawTreeSegment dts, DrawTreeModelDataSource.ModelDataForTip mdt) {

    double padWidth = mdt.negLength;
    Vector2D arrival = mdt.arrival;
    Point2D lanLoc = mdt.lanLoc;

    Vector2D run = dts.getRun();
    Vector2D norm = dts.getNormal();
    Point2D end = dts.getEnd();
    Point2D start = dts.getStart();
    GeneralPath tipPath = new GeneralPath();
    double halfPad = padWidth / 2.0;

    //
    // Things get really weird close into odd-width nodes like boxes and text
    // If the last segment is a fragment very, very close to the landing point, we ignore
    // its direction. We also fall back on the landing location as the end if last point
    // is closer to the end than the pad point.
    //

    Vector2D finalRun = new Vector2D(start, lanLoc);
    if (finalRun.length() <= mdt.plusArrowDepth) {
      run = arrival;
      Vector2D fromEnd = new Vector2D(lanLoc, end);
      if (fromEnd.dot(arrival) > 0.0) {
        end = lanLoc;
      }
      norm = run.normal();
    }

    //
    // Up until the "thick" line width, we keep arrow size constant.  Above that,
    // we maintain the size delta we had at the thick width.
    //

    double halfWidth;
    int dropWidth = dts.getResolvedStyle().getThickness();
    if (dropWidth > mdt.thickThick) {
      halfWidth = halfPad + ((dropWidth - mdt.thickThick) / 2.0);
    } else {
      halfWidth = halfPad;
    }

    double offnegx = end.getX() - (norm.getX() * halfWidth);
    double offnegy = end.getY() - (norm.getY() * halfWidth);
    double offposx = end.getX() + (norm.getX() * halfWidth);
    double offposy = end.getY() + (norm.getY() * halfWidth);
    tipPath.moveTo((float)offnegx, (float)offnegy);
    tipPath.lineTo((float)offposx, (float)offposy);

    return (tipPath);
  }

  /***************************************************************************
  **
  ** Render an evidence glyph
  */

	private void renderEvidenceGlyphToCache(ModalShapeContainer group, Genome genome,
			DrawTreeSegment dts, String linkID, boolean isGhosted,
			DrawTreeModelDataSource.ModelDataForTip mdt, DataAccessContext rcx) {

		Vector2D arrival = mdt.arrival;
		Point2D lanLoc = mdt.lanLoc;

		Linkage link = genome.getLinkage(linkID);
		int level = link.getTargetLevel();
		if (level == Linkage.LEVEL_NONE) {
			return;
		}

		boolean checkForActive = (genome instanceof GenomeInstance);
		boolean isActive = (checkForActive) ? (((LinkageInstance) link)
				.getActivity((GenomeInstance) genome) == LinkageInstance.ACTIVE) : true;

		Vector2D run = dts.getRun();
		Point2D end = dts.getEnd();
		Point2D start = dts.getStart();

		//
		// Things get really weird close into odd-width nodes like boxes and text,
		// particularly
		// since fonts aren't being scaled exactly for speed.
		// If the last segment is a fragment very, very close to the landing point,
		// we ignore
		// its direction. We also fall back on the landing location as the end if
		// last point
		// is closer to the end than the pad point.
		//
		Vector2D finalRun = new Vector2D(start, lanLoc);
		if (finalRun.length() <= mdt.plusArrowDepth) {
			run = arrival;
			Vector2D fromEnd = new Vector2D(lanLoc, end);
			if (fromEnd.dot(arrival) > 0.0) {
				end = lanLoc;
			}
		}

		double lf = mdt.levelFudge;
		float glyphBaseX = (float) (end.getX() + (run.getX() * lf));
		float glyphBaseY = (float) (end.getY() + (run.getY() * lf));
		GeneralPath glyphPath = new GeneralPath();
		EvidenceGlyph.addGlyphToPath(glyphPath, glyphBaseX, glyphBaseY);
		DisplayOptions dop = rcx.getDisplayOptsSource().getDisplayOptions();

		Integer pathLayer = (isActive) ? ACTIVE_PATH_LAYER : INACTIVE_PATH_LAYER;
		Color col = (isGhosted || !isActive) ? dop.getInactiveGray()
				: evidenceToGlyphColor(level, dop);
		BasicStroke tagStroke = new BasicStroke(REG_THICK, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND);

		ModelObjectCache.SegmentedPathShape sps = new ModelObjectCache.SegmentedPathShape(DrawMode.FILL, col, tagStroke, glyphPath);
		group.addShape(sps, pathLayer, MINOR_TARGET_LABEL_LAYER_);

		return;
	}

  /***************************************************************************
  **
  ** Render the pads
  */

  private void drawAPad(ModalShapeContainer group, Point2D pt, Color col, BasicStroke padStroke, boolean isActive, double pixDiam) {
    double x = pt.getX();
    double y = pt.getY();
    //
    // Start doing scale-dependent rendering.  NOTE that this is only godd at the moment since pads are
    // shown in editing, and cache is used for view only.  Obviously, cache would need to be scale-aware
    // if this is used with a real cache situation!
    //
    double useRad = (pixDiam < PAD_RADIUS_) ? PAD_RADIUS_ : UiUtil.forceToGridValue(2.0 * pixDiam, UiUtil.GRID_SIZE);

    ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, useRad, DrawMode.DRAW, col, padStroke);

    Integer pathLayer = (isActive) ? ACTIVE_PATH_LAYER : INACTIVE_PATH_LAYER;
    group.addShape(circ, pathLayer, MINOR_PAD_LAYER_);
  }

  /***************************************************************************
  **
  ** Render a bus branch
  */

	private void drawABusBranch(ModalShapeContainer group, Point2D pt, Color col1,
			Color col2, int branchRender, BasicStroke branchStroke, boolean isActive) {
		double x = pt.getX();
		double y = pt.getY();
		ModelObjectCache.Ellipse circ = new ModelObjectCache.Ellipse(x, y, BB_RADIUS_, DrawMode.FILL, col1, branchStroke);

		Integer pathLayer = (isActive) ? ACTIVE_PATH_LAYER : INACTIVE_PATH_LAYER;
		group.addShape(circ, pathLayer, MINOR_BRANCH_LAYER_);

		if (branchRender == DisplayOptions.OUTLINED_BUS_BRANCHES) {
			circ = new ModelObjectCache.Ellipse(x, y, BB_RADIUS_, DrawMode.DRAW, col2, branchStroke);
			group.addShape(circ, pathLayer, MINOR_BRANCH_LAYER_);
		}

		return;
	}

  /***************************************************************************
  **
  ** Map to evidence glyph color
  */

  private Color evidenceToGlyphColor(int level, DisplayOptions dop) {
    switch (level) {
      case Linkage.LEVEL_1:
        return (Color.blue);
      case Linkage.LEVEL_2:
        return (Color.orange);
      case Linkage.LEVEL_3:
        return (Color.green);
      case Linkage.LEVEL_4:
        return (Color.getHSBColor(0.534F, 1.0F, 0.5F));  // dark steel blue
      case Linkage.LEVEL_5:
        return (Color.magenta);
      case Linkage.LEVEL_6:
        return (Color.getHSBColor(0.033F, 0.4F, 0.9F)); // dark salmon
      case Linkage.LEVEL_7:
        return (Color.getHSBColor(0.1F, 1.0F, 1.0F));  // tangerine
      case Linkage.LEVEL_8:
        return (Color.getHSBColor(0.567F, 1.0F, 1.0F));  // sky blue
      case Linkage.LEVEL_9:
        return (Color.getHSBColor(0.0F, 0.6F, 0.55F));  // deep ochre
      case Linkage.LEVEL_10:
        return (Color.getHSBColor(0.283F, 0.5F, 0.8F)); // pale green       
      default:
        return (dop.getInactiveGray());
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** get the key for the selection layer
  */

  public static Integer getSelectionLayerKey() {
    return (MINOR_SELECTED_LAYER_);
  }

  /***************************************************************************
  **
  ** Figure out the thickness of a segment
  */

  public static int getSegThick(DataAccessContext icx, DrawTreeModelDataSource mds, LinkSegmentID getID, LinkProperties lp) {

    DrawTreeModelDataSource.ModelLineStyleModulation lsMod = mds.getModelLineStyleModulation(icx);

    HashMap<LinkSegmentID, DrawTreeSegment> segments = new HashMap<LinkSegmentID, DrawTreeSegment>();
    DisplayOptions dopt = icx.getDisplayOptsSource().getDisplayOptions(); 

    //
    // Crank through drops.  Get segmentID for each end drop, get a path
    // back up to root.
    //

    Iterator<LinkBusDrop> dit = lp.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        String linkID = drop.getTargetRef();
        PerLinkDrawStyle perLink = drop.getDrawStyleForLink();
        DrawTreeModelDataSource.LinkLineStyleModulation llsm =
          mds.getLinkLineStyleModulation(icx, linkID, lp, lsMod);
        // VFN may not include some drops!
        if (llsm == null) {
          continue;
        }

        List<LinkSegmentID> toRoot = lp.getSegmentIDsToRootForEndDrop(drop);
        int numToRoot = toRoot.size();
        for (int i = 0; i < numToRoot; i++) {
          LinkSegmentID segID = toRoot.get(i);
          DrawTreeSegment dts = segments.get(segID);
          if (dts == null) {
            SuggestedDrawStyle drawStyle = lp.getDrawStyleForID(segID);
            dts = new DrawTreeSegment(segID, null, drawStyle);
            segments.put(segID, dts);
          }
          dts.incrementPathCount();
          if ((perLink != null) || (llsm.perLinkForEvidence != null) || (llsm.perLinkActivity != null)) {
            dts.addPerLinkProps(perLink, linkID, llsm.perLinkForEvidence, llsm.perLinkActivity, llsm.simDiff);
          }
        }
      }
    }

    DrawTreeSegment answerDts = segments.get(getID);
    return (answerDts.resolveDrawStyle(lp, true, lsMod.linkModulation, lsMod.forModules, icx.getColorResolver(), IRenderer.Mode.NORMAL, dopt).getThickness());
  }

  /***************************************************************************
  **
  ** Status of a path flip
  */

  private static class FlipStatus {

    static final int NEW_PATH           = 0;
    static final int SAME_PATH_JUMP     = 1;
    static final int SAME_PATH_CONTINUE = 2;

    int flipStatus;
    GeneralPath currPath;
    ResolvedDrawStyle styleUsed;
    Integer currentLayer;

    FlipStatus(int flipStatus, GeneralPath currPath, ResolvedDrawStyle styleUsed, Integer currentLayer) {
      this.flipStatus = flipStatus;
      this.currPath = currPath;
      this.styleUsed = styleUsed;
      this.currentLayer = currentLayer;
    }
  }
}
