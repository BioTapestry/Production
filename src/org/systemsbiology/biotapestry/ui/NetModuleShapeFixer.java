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

package org.systemsbiology.biotapestry.ui;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.freerender.NodeBounder;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.RectDefinedByRects;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handles the calculations of changing a net module shape in response
** to a layout change
*/

public class NetModuleShapeFixer {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetModuleShapeFixer() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  **
  ** Stash info that tells us how each module shape is positioned w.r.t. the
  ** module members it contains.
  ** 
  */  
  
  public ModuleRelocateInfo getModuleRelocInfo(NetModule.FullModuleKey fullKey, Point2D center, StaticDataAccessContext irx) {

    NetOverlayOwner noo = irx.getGenomeSource().getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
    NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
    NetModule nmod = no.getModule(fullKey.modKey);   
    NetOverlayProperties nop = irx.getCurrentLayout().getNetOverlayProperties(fullKey.ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
    
    Rectangle2D nameBounds = nmp.getRenderer().getNameBounds(nmp, nmod, irx);    
    ModuleRelocateInfo mri = new ModuleRelocateInfo(fullKey.modKey, center, nameBounds);
    
    //
    // Build up a module member set.  Save the positions if we need to:
    //

    HashSet<String> nodeSet = new HashSet<String>();
   
    Iterator<NetModuleMember> memit = nmod.getMemberIterator();
    while (memit.hasNext()) {
      NetModuleMember nmm = memit.next();
      String nodeID = nmm.getID();
      nodeSet.add(nodeID);
      NodeProperties np = irx.getCurrentLayout().getNodeProperties(nodeID);
      Point2D nodeLoc = np.getLocation();
      mri.memberLocs.put(nodeID, (Point2D)nodeLoc.clone());
    }
    
    //
    // For the nodes in the module, get the rectangle needed for each.  Note that member-only
    // modules don't usually need this info, but it might be needed if all the members go away
    // and we need to locate the name (currently not done...)
    //

    mri.memberRects = NodeBounder.nodeRects(nodeSet, irx, UiUtil.GRID_SIZE_INT);
    
    //
    // With member only modules, we are done...
    //
    
    int modType = nmp.getType();
    if (modType == NetModuleProperties.MEMBERS_ONLY) {
      return (mri);
    }
    
    //
    // Create a local stash of the old shapes. Then we are done for the moment:
    //
    
    Iterator<Rectangle2D> sit = nmp.getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D nextRect = sit.next();
      mri.oldShapes.add((Rectangle2D)nextRect.clone());
    }
    
    return (mri);
  }   
  
  /***************************************************************************
  **
  ** Get info that tells us how each unclaimed module shape is positioned w.r.t. 
  ** the pinned group-region shapes.
  */  
  
  public List<RectResolution> getModuleRelocInfoForRegions(Map<String, 
                                                           Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                                                           List<NetModuleProperties.TaggedShape> unclaimedSlices, 
                                                           NetModule.FullModuleKey fullKey, Point2D useCenter) {

    //
    // Now define each shape rectangle by how it is located w.r.t. the 
    // group-claimed rects.
    //
    
    ArrayList<RectResolution> retval = new ArrayList<RectResolution>();
    HashSet<RectDefinedByRects.TaggedRect> defined = new HashSet<RectDefinedByRects.TaggedRect>();
    ArrayList<Rectangle2D> definedRects = new ArrayList<Rectangle2D>();
    ArrayList<RectDefinedByRects.TaggedRect> remaining = new ArrayList<RectDefinedByRects.TaggedRect>();
    
    //
    // Gather up the slices attached to groups:
    //
    
    Iterator<String> oit = claimedSlices.keySet().iterator();
    while (oit.hasNext()) {
      String ownerGrpID = oit.next();    
      Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> perGroup = claimedSlices.get(ownerGrpID);
      Layout.DicedModuleInfo shapesPerKey = perGroup.get(fullKey);
      if (shapesPerKey == null) {
        continue;
      }
      int numShape = shapesPerKey.dicedShapes.size();
      for (int i = 0; i < numShape; i++) {
        NetModuleProperties.TaggedShape tShape = shapesPerKey.dicedShapes.get(i);
        GroupTRKey rrKey = new GroupTRKey(ownerGrpID, tShape.isName);
        RectDefinedByRects.TaggedRect tr = new RectDefinedByRects.TaggedRect(tShape.shape, rrKey);
        defined.add(tr);
        definedRects.add(tr.rect);
      }
    }
    
    //
    // Gather up unclaimed:
    //
   
    int numShape = unclaimedSlices.size();
    for (int i = 0; i < numShape; i++) {
      NetModuleProperties.TaggedShape tShape = unclaimedSlices.get(i);
      GroupTRKey rrKey = new GroupTRKey(i, tShape.isName);
      RectDefinedByRects.TaggedRect tr = new RectDefinedByRects.TaggedRect(tShape.shape, rrKey);
      remaining.add(tr);       
    }
    Collections.sort(remaining, new RectDefinedByRects.RectOrdering());   
    
    //
    // Now try to define remaining rects by how they overlap defined rects.
    // Keep going until we make no more progress.  If somebody is left, 
    // pin it down via centroid calculation and start over again.
    //
      
 
    int lastCount = remaining.size();
    if (lastCount > 0) {
      while (true) {
        int index = 0;
        // Make a pass that uses defined rects (if there are any):
        if (!defined.isEmpty()) {
          while (index < remaining.size()) {
            RectDefinedByRects.TaggedRect tr = remaining.get(index);
            RectDefinedByRects rdbr = new RectDefinedByRects(tr.rect, defined);
            if (rdbr.isDefined()) {
              retval.add(new RectResolution(RectResolution.BY_DEFINED_RECTS, false, tr.rect, rdbr, tr.tag));
              defined.add(tr);
              definedRects.add(tr.rect);
              remaining.remove(index);
            } else {
              index++;
            }
          }
        }
        // See if we made progress.  If we are done, we are gone.  If progress was made,
        // do it again.  If no progress, fall back to centroid definitions:
        int newCount = remaining.size();
        if (newCount == 0) {
          break;
        }
        if (newCount < lastCount) {
          lastCount = newCount;
          continue;
        }
        //
        // No progress. Define one in terms of a centroid and keep going:
        //
        Point2D usePoint;
        Point2D savePoint;
        if (!defined.isEmpty()) {
          usePoint = calcCentroidFromShapes(definedRects);
          savePoint = null;
        } else {
          usePoint = (Point2D)useCenter.clone();
          savePoint = usePoint;
        } 
        
        RectDefinedByRects.TaggedRect tr = remaining.get(0);
        Point2D rectCenter = new Point2D.Double(tr.rect.getCenterX(), tr.rect.getCenterY());
        UiUtil.forceToGrid(rectCenter, UiUtil.GRID_SIZE);
        Vector2D centroidDelta = new Vector2D(usePoint, rectCenter); 
        
        retval.add(new RectResolution(RectResolution.BY_CENTROID, false, tr.rect, savePoint, centroidDelta, tr.tag));
        defined.add(tr);
        definedRects.add(tr.rect);
        remaining.remove(0);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Shift module shapes around to accomodate moving member nodes
  ** 
  */  
  
  public NetModuleProperties shiftModuleShapesPerParams(NetModule.FullModuleKey fullKey, ModuleRelocateInfo mri, Point2D center,
                                                        StaticDataAccessContext irx) {

    //
    // Define the old shapes in terms of the surviving node members:
    //

    Set<String> nodeSet = parameterizeOldShapes(irx.getGenomeSource(), mri, irx.getCurrentLayout(), fullKey);
        
    NetOverlayProperties nop = irx.getCurrentLayout().getNetOverlayProperties(fullKey.ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);

    //
    // If we have a member-only module, this will do the trick, as long as some node survived:
    //

    int modType = nmp.getType();
    if (modType == NetModuleProperties.MEMBERS_ONLY) {
      if (mri.newNameLoc != null) {
        NetModuleProperties changedProps = nmp.clone();
        changedProps.setNameLocation(mri.newNameLoc);
        return (changedProps);
      }
      return (null);
    }
      
    //
    // For the nodes in the module, get the rectangle needed for each:
    //

    Map<String, Rectangle> newMemberRects = NodeBounder.nodeRects(nodeSet, irx, UiUtil.GRID_SIZE_INT); 
   
    //
    // Create the map from old member rects to new member rects:
    //  
    
    HashMap<RectDefinedByRects.TaggedRect, Rectangle2D> fixupMap = new HashMap<RectDefinedByRects.TaggedRect, Rectangle2D>();
    Iterator<RectDefinedByRects.TaggedRect> survit = mri.survivingRects.iterator();
    while (survit.hasNext()) {
      RectDefinedByRects.TaggedRect tr = survit.next();
      String nodeID = (String)tr.tag;
      Rectangle2D newMR = newMemberRects.get(nodeID);
      fixupMap.put(tr, newMR);
    } 
      
    
    //
    // Run resolution.  Since guys in the list have been defined by those
    // preceeding, we should be OK:
    //
    
    ArrayList<Rectangle2D> convertList = new ArrayList<Rectangle2D>();
    Point2D newNameLoc = null;
    int numRes = mri.resolutionList.size();
    for (int i = 0; i < numRes; i++) {
      RectResolution rectRes = mri.resolutionList.get(i); 
      boolean isName = ((mri.nameTag != null) && rectRes.tag.equals(mri.nameTag));  
      switch (rectRes.type) {
        case RectResolution.BY_MEMBERS:
        case RectResolution.BY_DEFINED_RECTS:
          Rectangle2D newDefined = rectRes.rdbr.generateNewRect(fixupMap); 
          if (isName) {
            newNameLoc = new Point2D.Double(newDefined.getCenterX(), newDefined.getCenterY());
          } else {
            convertList.add(newDefined);
          }
          fixupMap.put(new RectDefinedByRects.TaggedRect(rectRes.rect, rectRes.tag), newDefined);      
          break;
        case RectResolution.BY_CENTROID:
          Point2D usePoint = (rectRes.point != null) ? center : calcCentroidFromShapes(convertList);
          Point2D newCenter = rectRes.offset.add(usePoint);
          Point2D newCorner = new Point2D.Double(newCenter.getX() - (rectRes.rect.getWidth() / 2.0),
                                                 newCenter.getY() - (rectRes.rect.getHeight() / 2.0));
          UiUtil.forceToGrid(newCorner, UiUtil.GRID_SIZE);
          newDefined = new Rectangle2D.Double(newCorner.getX(), newCorner.getY(), 
                                              rectRes.rect.getWidth(), rectRes.rect.getHeight());
          if (isName) {
            newNameLoc = new Point2D.Double(newDefined.getCenterX(), newDefined.getCenterY());
          } else {
            convertList.add(newDefined);
          }
          fixupMap.put(new RectDefinedByRects.TaggedRect(rectRes.rect, rectRes.tag), newDefined);
          break;
        default:
          throw new IllegalStateException();
      }
    }
        
    NetModuleProperties changedProps = null;
    if (!convertList.isEmpty()) {
      changedProps = nmp.clone();
      changedProps.replaceTypeAndShapes(modType, convertList);
    }
    if (newNameLoc != null) {
      if (changedProps == null) {
        changedProps = nmp.clone();
      }
      changedProps.setNameLocation(newNameLoc);
    }    
    return (changedProps);
  }  

  
  /***************************************************************************
  **
  ** Build a fixup map:
  ** 
  */
  
  private Map<RectDefinedByRects.TaggedRect, Rectangle2D> buildFixupMapForKey(NetModule.FullModuleKey fullKey, 
                                                                              Map<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapeMap, 
                                                                              Map<Object, Vector2D> deltas, 
                                                                              Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                                                                              Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices) {
    
    // FIX ME!
    // Bug BT-07-22-09:1
    // Note how the unclaimed region is getting slightly changed, causing the derived
    // name location to shift.  This still needs to be fixed!
    //
    //old->new java.awt.geom.Rectangle2D$Double[x=6400.0,y=3810.0,w=200.0,h=400.0]
    //   -->java.awt.geom.Rectangle2D$Double[x=6420.0,y=3810.0,w=180.0,h=400.0] 
    //   new java.awt.geom.Rectangle2D$Double[x=5930.0,y=3810.0,w=180.0,h=400.0] Vector2D: x = 490.0 y = 0.0
    //old->new java.awt.geom.Rectangle2D$Float[x=6060.0,y=3910.0,w=190.0,h=50.0]
    //   -->java.awt.geom.Rectangle2D$Double[x=6420.0,y=3930.0,w=0.0,h=0.0] 
    //   new java.awt.geom.Rectangle2D$Double[x=5930.0,y=3930.0,w=0.0,h=0.0] Vector2D: x = 490.0 y = 0.0
    //old->new java.awt.geom.Rectangle2D$Double[x=5940.0,y=4210.0,w=460.0,h=230.0]
    //   -->java.awt.geom.Rectangle2D$Double[x=5940.0,y=4220.0,w=460.0,h=220.0] 
    //   new java.awt.geom.Rectangle2D$Double[x=5930.0,y=3810.0,w=460.0,h=220.0] Vector2D: x = 10.0 y = 410.0
 
    
    
    
    Rectangle2D oldName = getOldNameRect(claimedSlices, unclaimedSlices, fullKey);
    HashMap<RectDefinedByRects.TaggedRect, Rectangle2D> retval = new HashMap<RectDefinedByRects.TaggedRect, Rectangle2D>();
    Iterator<String> kit1 = oldToNewShapeMap.keySet().iterator();
    while (kit1.hasNext()) {
      String grpID = kit1.next();
      Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> oldToNewForGroup = oldToNewShapeMap.get(grpID);
      Vector2D delta = deltas.get(grpID);
      Iterator<String> kit2 = oldToNewForGroup.keySet().iterator();
      while (kit2.hasNext()) {
        String ovrID = kit2.next();
        if (!ovrID.equals(fullKey.ovrKey)) {
          continue;
        }
        Map<String, List<NetModuleProperties.TaggedShape>> oldToNewForOverlay = oldToNewForGroup.get(ovrID);
        Iterator<String> kit3 = oldToNewForOverlay.keySet().iterator();
        while (kit3.hasNext()) {
          String modID = kit3.next();
          if (!modID.equals(fullKey.modKey)) {
            continue;
          }
          List<NetModuleProperties.TaggedShape> oldAndNew = oldToNewForOverlay.get(modID);
          Iterator<NetModuleProperties.TaggedShape> kit4 = oldAndNew.iterator();
          while (kit4.hasNext()) {
            NetModuleProperties.TaggedShape ts = kit4.next(); 
            Rectangle2D newShape = ts.shape;
            Rectangle2D useShape = (Rectangle2D)newShape.clone();
            if (delta != null)  {
              useShape.setRect(newShape.getX() + delta.getX(), newShape.getY() + delta.getY(), newShape.getWidth(), newShape.getHeight());             
            }
            GroupTRKey gtrk = new GroupTRKey(grpID, ts.isName);
            Rectangle2D oldShape = (ts.isName) ? oldName : ts.oldShape;
            if (oldShape != null) {
              oldShape = (Rectangle2D)oldShape.clone();
              RectDefinedByRects.TaggedRect tr = new RectDefinedByRects.TaggedRect(oldShape, gtrk);
              retval.put(tr, useShape);
            }
          }    
        }  
      } 
    }
    return (retval);    
  }
  
  /***************************************************************************
  **
  ** bogus hack to find old name rectangle
  ** 
  */
  
  private Rectangle2D getOldNameRect(Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                                     Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices, NetModule.FullModuleKey fullKey) {  
  
    Layout.DicedModuleInfo dmi = unclaimedSlices.get(fullKey);
    int numRes = dmi.dicedRectByRects.size(); 
    for (int i = 0; i < numRes; i++) {
      RectResolution rectRes = dmi.dicedRectByRects.get(i);
      GroupTRKey gkey = (GroupTRKey)rectRes.tag;
      if (gkey.isName()) {
        return (rectRes.rect);
      }
    }
    
    Iterator<String> oit = claimedSlices.keySet().iterator();
    while (oit.hasNext()) {
      String ownerGrpID = oit.next();    
      Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> perGroup = claimedSlices.get(ownerGrpID);
      Layout.DicedModuleInfo shapesPerKey = perGroup.get(fullKey);
      if (shapesPerKey == null) {
        continue;
      }
      int numShape = shapesPerKey.dicedShapes.size();
      for (int i = 0; i < numShape; i++) {
        NetModuleProperties.TaggedShape tShape = shapesPerKey.dicedShapes.get(i);
        if (tShape.isName) {
          return (tShape.oldShape);
        }
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** The old to new shape map we are handed is only useful for groups that have
  ** actually been modified.  For everybody else, we have the same shape, just
  ** shifted
  ** 
  */
  
  private void fillInUndefined(Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices, 
                               Map<Object, Vector2D> deltas, 
                               Map<RectDefinedByRects.TaggedRect, Rectangle2D> fixupMap) {
    Iterator<NetModule.FullModuleKey> uskit = unclaimedSlices.keySet().iterator();
    while (uskit.hasNext()) {
      NetModule.FullModuleKey fullKey = uskit.next();
      Layout.DicedModuleInfo dmi = unclaimedSlices.get(fullKey);
      int numRes = dmi.dicedRectByRects.size();          
      for (int i = 0; i < numRes; i++) {
        RectResolution rectRes = dmi.dicedRectByRects.get(i); 
        if (rectRes.type == RectResolution.BY_DEFINED_RECTS) {  
          Set<RectDefinedByRects.TaggedRect> needKeys = rectRes.rdbr.definedByKeys();
          Iterator<RectDefinedByRects.TaggedRect> nkit = needKeys.iterator();
          while (nkit.hasNext()) {
            RectDefinedByRects.TaggedRect tr = nkit.next();
            if (fixupMap.get(tr) != null) {  // have the definition
              continue;
            }
            GroupTRKey gkey = (GroupTRKey)tr.tag;
            String grpID = gkey.getGroupID(); 
            if (grpID != null) {
              Vector2D delta = deltas.get(grpID);
              Rectangle2D useShape = (Rectangle2D)tr.rect.clone();
              if (delta != null)  {
                useShape.setRect(useShape.getX() + delta.getX(), useShape.getY() + delta.getY(), useShape.getWidth(), useShape.getHeight());             
              }
              RectDefinedByRects.TaggedRect tr1 = tr.clone();
              fixupMap.put(tr1, useShape);
            }
          }
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Shift module shapes around to accomodate moving the defining groups:
  ** 
  */
  
  public void pinModuleShapesForGroups(Map<String, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>>> oldToNewShapeMap, 
                                       Map<String, Map<NetModule.FullModuleKey, Layout.DicedModuleInfo>> claimedSlices, 
                                       Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> unclaimedSlices, Map<Object, Vector2D> deltas, Point2D center) {
    
    //
    // Run resolution.  Since guys in the list have been defined by those
    // preceeding, we should be OK:
    //
  
    Iterator<NetModule.FullModuleKey> uskit = unclaimedSlices.keySet().iterator();
    while (uskit.hasNext()) {
      NetModule.FullModuleKey fullKey = uskit.next();
      Map<RectDefinedByRects.TaggedRect, Rectangle2D> fixupMap = buildFixupMapForKey(fullKey, oldToNewShapeMap, deltas, claimedSlices, unclaimedSlices);
      fillInUndefined(unclaimedSlices, deltas, fixupMap);
      ArrayList<Rectangle2D> justShapes = new ArrayList<Rectangle2D>();
      //
      // Gotta get info for claimed slices into the defining set:
      //
      
      Iterator<String> oit = claimedSlices.keySet().iterator();
      while (oit.hasNext()) {
        String ownerGrpID = oit.next();    
        Map<NetModule.FullModuleKey, Layout.DicedModuleInfo> perGroup = claimedSlices.get(ownerGrpID);
        Layout.DicedModuleInfo shapesPerKey = perGroup.get(fullKey);
        if (shapesPerKey == null) {
          continue;
        }
        int numShape = shapesPerKey.dicedShapes.size();
        for (int i = 0; i < numShape; i++) {
          NetModuleProperties.TaggedShape tShape = shapesPerKey.dicedShapes.get(i);
          Rectangle2D useShape = (Rectangle2D)tShape.shape.clone();
          Vector2D delta = deltas.get(ownerGrpID);
          if (delta != null)  {
            useShape.setRect(useShape.getX() + delta.getX(), useShape.getY() + delta.getY(), useShape.getWidth(), useShape.getHeight());             
          }
          justShapes.add(useShape); 
        }
      }
         
      Layout.DicedModuleInfo dmi = unclaimedSlices.get(fullKey); 
      dmi.dicedShapes = new ArrayList<NetModuleProperties.TaggedShape>();
      int numRes = dmi.dicedRectByRects.size();
      for (int i = 0; i < numRes; i++) {
        RectResolution rectRes = dmi.dicedRectByRects.get(i);
        Rectangle2D oldShape = (Rectangle2D)rectRes.rect.clone();
        GroupTRKey gkey = (GroupTRKey)rectRes.tag;
        switch (rectRes.type) {          
          case RectResolution.BY_DEFINED_RECTS:            
            Rectangle2D newDefined = rectRes.rdbr.generateNewRect(fixupMap);
            UiUtil.force2DToGrid(newDefined, UiUtil.GRID_SIZE);
            
            NetModuleProperties.TaggedShape ts = new NetModuleProperties.TaggedShape(oldShape, newDefined, gkey.isName());
            justShapes.add(newDefined);
            dmi.dicedShapes.add(ts);           
            fixupMap.put(new RectDefinedByRects.TaggedRect(rectRes.rect, rectRes.tag), newDefined);      
            break;
          case RectResolution.BY_CENTROID:
            Point2D usePoint = (rectRes.point != null) ? center : calcCentroidFromShapes(justShapes);
            Point2D newCenter = rectRes.offset.add(usePoint);
            Point2D newCorner = new Point2D.Double(newCenter.getX() - (rectRes.rect.getWidth() / 2.0),
                                                   newCenter.getY() - (rectRes.rect.getHeight() / 2.0));
            UiUtil.forceToGrid(newCorner, UiUtil.GRID_SIZE);
            newDefined = new Rectangle2D.Double(newCorner.getX(), newCorner.getY(), 
                                                rectRes.rect.getWidth(), rectRes.rect.getHeight());
            ts = new NetModuleProperties.TaggedShape(oldShape, newDefined, gkey.isName());
            justShapes.add(newDefined);
            dmi.dicedShapes.add(ts);
            fixupMap.put(new RectDefinedByRects.TaggedRect(rectRes.rect, rectRes.tag), newDefined); 
            break;
          case RectResolution.BY_MEMBERS:
          default:
            throw new IllegalStateException();
        }
      }
    }
        
    return;
  }   
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   

  /***************************************************************************
  **
  ** Once we know what nodes have survived, we can define the rectangles in terms
  ** of the survivor's positions.
  ** 
  */  
  
  private Set<String> parameterizeOldShapes(GenomeSource gsrc, ModuleRelocateInfo mri, Layout layout, NetModule.FullModuleKey fullKey) {
    NetOverlayOwner noo = gsrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
    NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
    NetModule nmod = no.getModule(fullKey.modKey);   
    NetOverlayProperties nop = layout.getNetOverlayProperties(fullKey.ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
    int modType = nmp.getType();
    
    //
    // Get the member nodes (may be less than when we started if nodes have been deleted).
    // If we have member position info to use for name location, find those survivors too
    //
    
    HashSet<Point2D> oldPointSet = new HashSet<Point2D>();
    HashSet<Point2D> newPointSet = new HashSet<Point2D>();    
    HashSet<String> nodeSet = new HashSet<String>();
    Iterator<NetModuleMember> memit = nmod.getMemberIterator();
    while (memit.hasNext()) {
      NetModuleMember nmm = memit.next();
      String nodeID = nmm.getID();
      nodeSet.add(nodeID);
      if (modType != NetModuleProperties.MEMBERS_ONLY) {
        mri.survivingRects.add(new RectDefinedByRects.TaggedRect(mri.memberRects.get(nodeID), nodeID));
      } else {
        Point2D memberLoc = mri.memberLocs.get(nodeID);
        oldPointSet.add(memberLoc);
        NodeProperties np = layout.getNodeProperties(nodeID);
        Point2D nodeLoc = np.getLocation();
        newPointSet.add((Point2D)nodeLoc.clone());
      }     
    } 
    
    //
    // For member-only types, derive the old name location offset (if we need it) and be done:
    // Note:  In some cases, member-only modules lose all their members and get converted into
    // multi-rect modules.  That happens later!  So if we have that case, we need to fall back.
    //
      
    if (modType == NetModuleProperties.MEMBERS_ONLY) {
      if (mri.nameBounds == null) {
        mri.newNameLoc = null;
      } else {
        Point2D nameCenter = new Point2D.Double(mri.nameBounds.getCenterX(), mri.nameBounds.getCenterY());
        UiUtil.forceToGrid(nameCenter, UiUtil.GRID_SIZE);
        if (!oldPointSet.isEmpty() && !newPointSet.isEmpty()) {
          Point2D oldCentroid = AffineCombination.combination(oldPointSet, UiUtil.GRID_SIZE);          
          Vector2D nameOffset = new Vector2D(oldCentroid, nameCenter);
          Point2D newCentroid = AffineCombination.combination(newPointSet, UiUtil.GRID_SIZE);
          mri.newNameLoc = nameOffset.add(newCentroid);
        } else {
          // FIX ME: At the moment, we will just keep the old center.
          mri.newNameLoc = nameCenter;
        }
      } 

      return (nodeSet);
    }
  
    //
    // Now define each shape rectangle by how it is located w.r.t. the member
    // rects in contains.  Some rects may not qualify on the first pass:
    //
    
    HashSet<RectDefinedByRects.TaggedRect> defined = new HashSet<RectDefinedByRects.TaggedRect>();
    ArrayList<Rectangle2D> definedRects = new ArrayList<Rectangle2D>();
    ArrayList<RectDefinedByRects.TaggedRect> remaining = new ArrayList<RectDefinedByRects.TaggedRect>();
    mri.nameTag = null;
    
    // Glue the name bounds on as the last rectangle:
    int numOld = mri.oldShapes.size();
    int highIndex = (mri.nameBounds == null) ? numOld : numOld + 1;
    for (int i = 0; i < highIndex; i++) {
      boolean isName = (i == numOld);
      Rectangle2D nextRect = (!isName) ? (Rectangle2D)mri.oldShapes.get(i) : mri.nameBounds;
      RectDefinedByRects rdbr = new RectDefinedByRects(nextRect, mri.survivingRects);
      Integer rrKey = new Integer(i);
      if (isName) {
        mri.nameTag = rrKey;
      }
      RectDefinedByRects.TaggedRect tr = new RectDefinedByRects.TaggedRect(nextRect, rrKey);
      if (rdbr.isDefined()) {
        mri.resolutionList.add(new RectResolution(RectResolution.BY_MEMBERS, isName, nextRect, rdbr, rrKey));
        defined.add(tr);
        definedRects.add(nextRect);
      } else {
        remaining.add(tr);
      }
    }
        
    //
    // Now try to define remaining rects by how they overlap defined rects.
    // Keep going until we make no more progress.  If somebody is left, 
    // pin it down via centroid calculation and start over again.
    //
      
    int lastCount = remaining.size();
    if (lastCount > 0) {
      while (true) {
        int index = 0;
        // Make a pass that uses defined rects (if there are any):
        if (!defined.isEmpty()) {
          while (index < remaining.size()) {
            RectDefinedByRects.TaggedRect tr = remaining.get(index);
            RectDefinedByRects rdbr = new RectDefinedByRects(tr.rect, defined);
            boolean isName = ((mri.nameTag != null) && tr.tag.equals(mri.nameTag));  
            if (rdbr.isDefined()) {
              mri.resolutionList.add(new RectResolution(RectResolution.BY_DEFINED_RECTS, isName, tr.rect, rdbr, tr.tag));
              defined.add(tr);
              definedRects.add(tr.rect);
              remaining.remove(index);
            } else {
              index++;
            }
          }
        }
        // See if we made progress.  If we are done, we are gone.  If progress was made,
        // do it again.  If no progress, fall back to centroid definitions:
        int newCount = remaining.size();
        if (newCount == 0) {
          break;
        }
        if (newCount < lastCount) {
          lastCount = newCount;
          continue;
        }
        //
        // No progress. Define one in terms of a centroid and keep going:
        //
        Point2D usePoint;
        Point2D savePoint;
        if (!defined.isEmpty()) {
          usePoint = calcCentroidFromShapes(definedRects);
          savePoint = null;
        } else {
          usePoint = (Point2D)mri.center.clone();
          savePoint = usePoint;
        }         
        RectDefinedByRects.TaggedRect tr = remaining.get(0);
        Point2D rectCenter = new Point2D.Double(tr.rect.getCenterX(), tr.rect.getCenterY());
        UiUtil.forceToGrid(rectCenter, UiUtil.GRID_SIZE);
        Vector2D centroidDelta = new Vector2D(usePoint, rectCenter);
        boolean isName = ((mri.nameTag != null) && tr.tag.equals(mri.nameTag));  
        
        mri.resolutionList.add(new RectResolution(RectResolution.BY_CENTROID, isName, tr.rect, savePoint, centroidDelta, tr.tag));
        defined.add(tr);
        definedRects.add(tr.rect);
        remaining.remove(0);
      }
    }
    return (nodeSet);
  } 
   
  /***************************************************************************
  **
  ** Get the centroid of a list of rectangles.  Contribution of each
  ** center is weighed by area of rectangle.
  ** 
  */  
  
  private Point2D calcCentroidFromShapes(List<Rectangle2D> definedRects) {
 
    ArrayList<Point2D> pointList = new ArrayList<Point2D>();
    int numRes = definedRects.size();
    double[] weights = new double[numRes];
    for (int i = 0; i < numRes; i++) {
      Rectangle2D calcRect = definedRects.get(i);
      Point2D rectCenter = new Point2D.Double(calcRect.getCenterX(), calcRect.getCenterY());
      pointList.add(rectCenter);
      weights[i] = calcRect.getHeight() * calcRect.getWidth();
    }
    if (pointList.isEmpty()) {
      throw new IllegalArgumentException();
    }
    Point2D centroid = AffineCombination.combination(pointList, weights, UiUtil.GRID_SIZE);
    return (centroid);             
  }

  
   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Used to shift modules around following a relayout
  **
  */  
  
  public static class ModuleRelocateInfo {
    String modKey;
    Map<String, Rectangle> memberRects;
    ArrayList<Rectangle2D> oldShapes;
    HashSet<RectDefinedByRects.TaggedRect> survivingRects;
    ArrayList<RectResolution> resolutionList;
    Point2D center;
    Point2D newNameLoc;
    Rectangle2D nameBounds;
    HashMap<String, Point2D> memberLocs;
    Object nameTag;
    
    ModuleRelocateInfo(String modKey, Point2D center, Rectangle2D nameBounds) {
      this.modKey = modKey;
      this.center = center;
      this.nameBounds = nameBounds;
      oldShapes = new ArrayList<Rectangle2D>();
      resolutionList = new ArrayList<RectResolution>();
      survivingRects = new HashSet<RectDefinedByRects.TaggedRect>();
      memberLocs = new HashMap<String, Point2D>();
    }
  }   
   
  public static class RectResolution {
     
    public static final int BY_CENTROID      = 0;
    public static final int BY_MEMBERS       = 1;
    public static final int BY_DEFINED_RECTS = 2;
         
    public int type;
    public Object tag;
    public Rectangle2D rect;
    public RectDefinedByRects rdbr;
    public Point2D point;
    public Vector2D offset;
    public boolean isName;
    
    RectResolution(int type, boolean isName, Rectangle2D rect, RectDefinedByRects rdbr, Object tag) {
      this.type = type;
      this.tag = tag;
      this.rect = rect;
      this.rdbr = rdbr;
      this.isName = isName;
    }
    
    RectResolution(int type, boolean isName, Rectangle2D rect, Point2D point, Vector2D offset, Object tag) {
      this.type = type;
      this.tag = tag;
      this.rect = rect;
      this.point = point;
      this.offset = offset;
      this.isName = isName;
    }
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Special tag keys
  */
  
  public static class GroupTRKey implements Cloneable {
    
    private boolean isName;
    private String groupID;
    private int unclaimedIndex;
       
    public GroupTRKey(String groupID, boolean isName) {
      this.groupID = groupID;
      this.isName = isName;
    }
    
    public GroupTRKey(int unclaimedIndex, boolean isName) {
      this.unclaimedIndex = unclaimedIndex;
      this.isName = isName;
    }
    
    public boolean isName() {
      return (isName);
    }
    
    public String getGroupID() {
      return (groupID);
    }
    
    public Object clone() {
      try {
        GroupTRKey newVal = (GroupTRKey)super.clone();
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }    
    }

    public int hashCode() {
      int baseVal = (groupID != null) ? groupID.hashCode() : unclaimedIndex;
      int nameInc = (isName) ? 1 : 0;
      return (baseVal + nameInc);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof GroupTRKey)) {
        return (false);
      }
      GroupTRKey otherTR = (GroupTRKey)other;
      
      if (this.isName != otherTR.isName) {
        return (false); 
      }
      
      if (this.groupID == null) {
        if (otherTR.groupID != null) {
          return (false);
        }
        return (this.unclaimedIndex == otherTR.unclaimedIndex);
      } else {
        return (this.groupID.equals(otherTR.groupID));
      }
    }  
  }  
}
