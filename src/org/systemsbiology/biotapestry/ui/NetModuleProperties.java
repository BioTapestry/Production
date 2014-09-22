/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.freerender.NodeBounder;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.SliceAndDicer;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Contains the information needed to layout a net module
*/

public class NetModuleProperties implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static final int ONE_MODULE_SHAPE     = 0;
  public static final int CONTIG_MODULE_SHAPES = 1;
  public static final int ALL_MODULE_SHAPES    = 2;
 
  public static final int CONTIG_RECT  = 0;
  public static final int MULTI_RECT   = 1;
  public static final int MEMBERS_ONLY = 2;
  private static final int NUM_TYPES_  = 3;

  public static final int INTERIOR    = -1;
  public static final int NONE        = 0;
  public static final int UPPER_LEFT  = 1;
  public static final int TOP         = 2;
  public static final int UPPER_RIGHT = 3;
  public static final int RIGHT       = 4;
  public static final int LOWER_RIGHT = 5;
  public static final int BOTTOM      = 6;
  public static final int LOWER_LEFT  = 7;     
  public static final int LEFT        = 8;  
  
  public static final int FADE_QUICKLY = 0;
  public static final int FADE_SLOWLY  = 1;  
  private static final int NUM_FADES_  = 2;    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  private static final String CONTIG_STR_  = "contig";
  private static final String MULTI_STR_   = "multi";
  private static final String MEMBERS_STR_ = "members"; 
  
  private static final int START_  = 1;
  private static final int END_    = 2;
  private static final int MIDDLE_ = 3;
  
  private static final String FADE_QUICKLY_STR_ = "quick";
  private static final String FADE_SLOWLY_STR_  = "slow"; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final double INTERSECT_TOL_ = 1.0E-3;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String moduleID_;
  private ArrayList<Rectangle2D> rectList_;
  private int type_;
  private Point2D nameLoc_;
  private boolean hideName_;
  private int nameFadeMode_;
  private String colorTag_;
  private String fillColorTag_;
  private FontManager.FontOverride localFont_;
  private String lineBreakDef_;
  private NetModuleFree freeRender_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Simple constructor
  */

  public NetModuleProperties(String moduleID, int type, List<Rectangle2D> shapes, Point2D labelLoc) {
    moduleID_ = moduleID;
    hideName_ = false;
    rectList_ = new ArrayList<Rectangle2D>();
    if (type != MEMBERS_ONLY) {
      rectList_.addAll(shapes);
      Rectangle2D firstRect = shapes.get(0);
      double labelX = firstRect.getCenterX();
      double labelY = firstRect.getCenterY();
      nameLoc_ = new Point2D.Double(labelX, labelY);
      UiUtil.forceToGrid(nameLoc_, UiUtil.GRID_SIZE);
    } else {
      if (labelLoc != null) {
        nameLoc_ = (Point2D)labelLoc.clone();
        UiUtil.forceToGrid(nameLoc_, UiUtil.GRID_SIZE);
      } else {
        hideName_ = true;
      }
    }
    type_ = type;
    colorTag_ = "black";
    fillColorTag_ = "white";
    localFont_ = null;
    lineBreakDef_ = null;
    nameFadeMode_ = FADE_SLOWLY;
    freeRender_ = new NetModuleFree();
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NetModuleProperties(NetModuleProperties other) {
    this.freeRender_ = other.freeRender_;
    this.moduleID_ = other.moduleID_;
    this.type_ = other.type_;
    this.rectList_ = new ArrayList<Rectangle2D>();
    int numRect = other.rectList_.size();
    for (int i = 0; i < numRect; i++) {
      Rectangle2D orect = other.rectList_.get(i);
      this.rectList_.add((Rectangle2D)orect.clone());
    }
    this.colorTag_ = other.colorTag_;
    this.fillColorTag_ = other.fillColorTag_;
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.nameLoc_ = (other.nameLoc_ == null) ? null : (Point2D)other.nameLoc_.clone();
    this.hideName_ = other.hideName_;
    this.lineBreakDef_ = other.lineBreakDef_;
    this.nameFadeMode_ = other.nameFadeMode_;
  }

  /***************************************************************************
  **
  ** Copy Constructor with ID change and optional offset
  */

  public NetModuleProperties(NetModuleProperties other, String newID, Vector2D offset) {
    this.freeRender_ = other.freeRender_;
    this.moduleID_ = newID;
    this.type_ = other.type_;
    this.rectList_ = new ArrayList<Rectangle2D>();
    int numRect = other.rectList_.size();
    for (int i = 0; i < numRect; i++) {
      Rectangle2D orect = other.rectList_.get(i);
      this.rectList_.add((Rectangle2D)orect.clone());
    }   
    this.colorTag_ = other.colorTag_;
    this.fillColorTag_ = other.fillColorTag_; 
    this.localFont_ = (other.localFont_ == null) ? null : (FontManager.FontOverride)other.localFont_.clone();
    this.nameLoc_ = (other.nameLoc_ == null) ? null : (Point2D)other.nameLoc_.clone();
    this.hideName_ = other.hideName_;
    this.lineBreakDef_ = other.lineBreakDef_;
    this.nameFadeMode_ = other.nameFadeMode_;
    if (offset != null) {
      this.moveAllShapes(offset.getX(), offset.getY());
      this.moveName(offset.getX(), offset.getY());
    }
  }  
 
  /***************************************************************************
  **
  ** Constructor for I/O
  */

  public NetModuleProperties(String ref, String typStr, String cStr, String fcStr, String lineBreaks, 
                             boolean hideLabel, String fadeStr, Point2D nameLoc) throws IOException {
    
    freeRender_ = new NetModuleFree();
    moduleID_ = ref;
    colorTag_ = cStr;
    fillColorTag_ = fcStr;
    rectList_ = new ArrayList<Rectangle2D>();  
    localFont_ = null;
    
    try {
      type_ = mapFromTypeTag(typStr);
    } catch (IllegalArgumentException iax) {
      throw new IOException();
    }
    nameLoc_ = nameLoc;
    hideName_ = hideLabel;
    lineBreakDef_ = lineBreaks;
    
    if (fadeStr == null) { // Actually only needed for prerelease files...
      nameFadeMode_ = FADE_QUICKLY;
    } else {
      try {
        nameFadeMode_ = mapFromFadeTag(fadeStr);
      } catch (IllegalArgumentException iax) {
        throw new IOException();
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  public NetModuleProperties clone() { 
    try {
      NetModuleProperties retval = (NetModuleProperties)super.clone();
      retval.rectList_ = new ArrayList<Rectangle2D>();
      int numRect = this.rectList_.size();
      for (int i = 0; i < numRect; i++) {
        Rectangle2D orect = this.rectList_.get(i);
        retval.rectList_.add((Rectangle2D)orect.clone());
      }
      retval.localFont_ = (this.localFont_ == null) ? null : (FontManager.FontOverride)this.localFont_.clone();
      retval.nameLoc_ = (this.nameLoc_ == null) ? null : (Point2D)this.nameLoc_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    

  /***************************************************************************
  **
  ** Get the renderer
  ** 
  */
  
  public NetModuleFree getRenderer() {
    return (freeRender_);
  }  
  
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (moduleID_);
  }  
  
  /***************************************************************************
  **
  ** Get the type
  */
  
  public int getType() {
    return (type_);
  }  
  
  /***************************************************************************
  **
  ** Get the name location
  */
  
  public Point2D getNameLoc() {
    return (nameLoc_);
  }  
  
  /***************************************************************************
  **
  ** Move the name location
  */
  
  public void moveName(double dx, double dy) {
    if (nameLoc_ == null) {
      return;
    }
    Point2D newLoc = new Point2D.Double(nameLoc_.getX() + dx, nameLoc_.getY() + dy);
    UiUtil.forceToGrid(newLoc, UiUtil.GRID_SIZE);
    nameLoc_ = newLoc;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the name location
  */
  
  public void setNameLocation(Point2D newLoc) {
    UiUtil.forceToGrid(newLoc, UiUtil.GRID_SIZE);
    nameLoc_ = newLoc;
    return;
  }  

  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    if (colorTag_.equals(oldID)) {
      colorTag_ = newID;
      retval = true;
    }
    if (fillColorTag_.equals(oldID)) {
      fillColorTag_ = newID;
      retval = true;
    }    
    return (retval);
  }    
 
  /***************************************************************************
  **
  ** Ask if we are hiding the label
  */
  
  public boolean isHidingLabel() {
    return (hideName_);
  }  
  
  /***************************************************************************
  **
  ** Set if we are hiding the label
  */
  
  public void setHidingLabel(boolean hideName) {
    hideName_ = hideName;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the name fade mode
  */
  
  public int getNameFadeMode() {
    return (nameFadeMode_);
  }  
  
  /***************************************************************************
  **
  ** Set the name fade mode
  */
  
  public void setNameFadeMode(int nameFadeMode) {
    nameFadeMode_ = nameFadeMode;
    return;
  }    
  
  /***************************************************************************
  **
  ** Get the line break definition
  */
  
  public String getLineBreakDef() {
    return (lineBreakDef_);
  }
  
 /***************************************************************************
  **
  ** Set the line break definition
  */
  
  public void setLineBreakDef(String lineBreakDef) {
    lineBreakDef_ = lineBreakDef;
    return;
  }
 
  /***************************************************************************
  **
  ** Trim and set the line break definition.  Make result available without
  ** actually setting it!
  */
  
  public static String trimOps(String lineBreakDef, String untrimmed) {
    String trimmedBreakDef = LineBreaker.fixBreaksForTrim(lineBreakDef, untrimmed);
    return (LineBreaker.massageLineBreaks(trimmedBreakDef, untrimmed.trim()));
  }

  /***************************************************************************
  **
  ** Trim and set the line break definition
  */
  
  public void trimAndSetLineBreakDef(String lineBreakDef, String untrimmed) {
    lineBreakDef_ = trimOps(lineBreakDef, untrimmed);
    return;
  }

  /***************************************************************************
  **
  ** Modify the line break definition.  Used for global propagation from a cumulative
  ** editing session.
  */
  
  public void applyLineBreakMod(LineBreaker.LineBreakChangeSteps steps, String untrimmed) {
    String resolvedBreakDef = LineBreaker.resolveNameChange(lineBreakDef_, steps);
    trimAndSetLineBreakDef(resolvedBreakDef, untrimmed);
    return;
  }    
  
  /***************************************************************************
  **
  ** Answer if we have shapes 
  */
  
  public boolean hasShapes() {
    return (!rectList_.isEmpty());
  }
  
  /***************************************************************************
  **
  ** Answer if we have only one shape
  */
  
  public boolean hasOneShape() {
    return (rectList_.size() == 1);
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over shapes
  */
  
  public Iterator<Rectangle2D> getShapeIterator() {
    return (rectList_.iterator());
  }    
     
  /***************************************************************************
  **
  ** Add a shape.
  */
  
  public void addShape(Rectangle2D rect) {
    rectList_.add(rect);
    return;
  }   

  /***************************************************************************
  **
  ** Compress the module by deleting extra rows and columns
  */
  
  public void compress(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, Rectangle bounds, List<TaggedShape> oldAndNew) { 
    
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      Rectangle2D newShape = UiUtil.compressRect(oldShape, emptyRows, emptyCols, bounds);
      if (oldAndNew != null) {
        oldAndNew.add(new TaggedShape(oldShape, newShape, false));
      }
      oldShape.setRect(newShape);
    } 
    
    if (nameLoc_ != null) {
      Point2D newLoc = UiUtil.compressPoint(nameLoc_, emptyRows, emptyCols, bounds);
      if (newLoc != null) {
        if (oldAndNew != null) {
          Rectangle2D fakeOld = new Rectangle2D.Double(nameLoc_.getX(), nameLoc_.getY(), 0.0, 0.0);
          Rectangle2D fakeNew = new Rectangle2D.Double(newLoc.getX(), newLoc.getY(), 0.0, 0.0);
          oldAndNew.add(new TaggedShape(fakeOld, fakeNew, true));
        }
        nameLoc_ = newLoc;
      }
    }
    return;
  }    
 
  /***************************************************************************
  **
  ** Expand the module by inserting extra rows and columns
  */
  
  public void expand(SortedSet<Integer> insertRows, SortedSet<Integer> insertCols, int mult, List<TaggedShape> oldAndNew) { 
    
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      Rectangle2D newShape = UiUtil.expandRect(oldShape, insertRows, insertCols, mult);
      if (oldAndNew != null) {
        oldAndNew.add(new TaggedShape(oldShape, newShape, false));
      }      
      oldShape.setRect(newShape);
    } 
    
    if (nameLoc_ != null) {
      Point2D newLoc = UiUtil.expandPoint(nameLoc_, insertRows, insertCols, mult);
      if (newLoc != null) {
        if (oldAndNew != null) {
          Rectangle2D fakeOld = new Rectangle2D.Double(nameLoc_.getX(), nameLoc_.getY(), 0.0, 0.0);
          Rectangle2D fakeNew = new Rectangle2D.Double(newLoc.getX(), newLoc.getY(), 0.0, 0.0);
          oldAndNew.add(new TaggedShape(fakeOld, fakeNew, true));
        }
        nameLoc_ = newLoc;
      }      
    }
    return;
  } 

  
 /***************************************************************************
  **
  ** If module shapes that are enclosed inside a group are expanded, they
  ** can grow beyond the region bounds because they can have some extra expand
  ** rows and cols beyond the genes.  This is OK when working in the root layout,
  ** but is deadly when merging group layouts into a new root instance layout.
  ** Use this to force clipping:
  */
  
  public void clipShapes(Rectangle2D clipBounds, List<TaggedShape> oldAndNew) {   
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();         
      Rectangle2D interRect = clipBounds.createIntersection(oldShape);
      if (!interRect.equals(oldShape)) {
        if (oldAndNew != null) {
          int numOaN = oldAndNew.size();
          for (int i = 0; i < numOaN; i++) {
            TaggedShape ts = oldAndNew.get(i);
            if (ts.shape.equals(oldShape)) {
              ts.shape = (Rectangle2D)interRect.clone();
              break;
            }
          }
        }              
        oldShape.setRect(interRect);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Emergency conversion of a drained member-only module to 
  ** legacy "ghost" rectangles
  */
  
  public void convertEmptyMemberOnly(Map<String, Rectangle> eachRect) {
    if (!rectList_.isEmpty()) {
      throw new IllegalStateException();
    }
    type_ = MULTI_RECT;
    Iterator<Rectangle> rit = eachRect.values().iterator();
    while (rit.hasNext()) {
      Rectangle eaRect = rit.next();
      Rectangle2D rect2D = new Rectangle2D.Double(eaRect.getX(), eaRect.getY(), 
                                                  eaRect.getWidth(), eaRect.getHeight());
      rectList_.add(rect2D);
    }
    return;
  }    
 
  /***************************************************************************
  **
  ** Convert between shape types
  */
  
  public List<Rectangle2D> convertShapes(int newType, Set<String> nodes, DataAccessContext rcx) {
    ArrayList<Rectangle2D> retval = new ArrayList<Rectangle2D>();
    switch (type_) {
      case CONTIG_RECT:
        if (newType == MULTI_RECT) {
          retval.addAll(rectList_);
        } else if (newType == MEMBERS_ONLY) {
          // Do nothing: retval is empty!
        }
        break;      
      case MULTI_RECT:
        if (newType == CONTIG_RECT) {
          int numRect = rectList_.size();
          Rectangle2D newRect = rectList_.get(0);
          for (int i = 1; i < numRect; i++) {
            Rectangle2D rect = rectList_.get(i);
            newRect = newRect.createUnion(rect);
          }
          retval.add(newRect);
        } else if (newType == MEMBERS_ONLY) {
          // Do nothing: retval is empty!
        }        
        break;        
      case MEMBERS_ONLY:
        Map<String, Rectangle> eachRect = null;
        if (newType != MEMBERS_ONLY) {
         eachRect = NodeBounder.nodeRects(nodes, rcx, UiUtil.GRID_SIZE_INT);          
        }
        if ((newType == MULTI_RECT) || (newType == CONTIG_RECT)) {
       //   retval.addAll(eachRect.values());
     //   } else if (newType == CONTIG_RECT) {
          Rectangle extent = null;
          Iterator<Rectangle> rit = eachRect.values().iterator();
          while (rit.hasNext()) {
            Rectangle eaRect = rit.next();
            if (extent == null) {
              extent = eaRect;
            } else {
              Bounds.tweakBounds(extent, eaRect);
            }         
          }
          Rectangle2D result = new Rectangle2D.Double(extent.getX(), extent.getY(), 
                                                      extent.getWidth(), extent.getHeight());
          retval.add(result);
        }        
        break;
      default:
        throw new IllegalStateException();
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Remove one intersected shape
  */
  
  public boolean removeBiggestShape(NetModuleFree.IntersectionExtraInfo iexi) {
    List<LocatedIntersection> extract = extractRectangles(iexi);
    if (extract == null) {
      return (false);
    }
    
    if (rectList_.size() <= 1) {
      return (false);
    } 
    
    //
    // We only allow one intersected shape to disappear at a time.  If we
    // intersect more than one, pick the one with the largest area to
    // go.  Never let the rect count drop to zero!
    //
    
    Rectangle2D deadRect = null;
    double deadArea = 0.0;
    
    Iterator<LocatedIntersection> eit = extract.iterator();
    while (eit.hasNext()) {
      LocatedIntersection li = eit.next();
      double chkArea = li.rect.getHeight() * li.rect.getWidth();
      if ((deadRect == null) || (chkArea > deadArea)) {
        deadRect = li.rect;
        deadArea = chkArea;
      }
    }
    
    if (deadRect != null) {
      rectList_.remove(deadRect);
      return (true);
    }
    
    return (false);
  }

  /***************************************************************************
  **
  ** Start the slice process by filling in the slice list 
  */
  
  public List<Rectangle2D> initSliceList() {
    ArrayList<Rectangle2D> retval = new ArrayList<Rectangle2D>();
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      retval.add((Rectangle2D)oldShape.clone());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** After slicing a our shapes up, try to reassemble them back into
  ** larget rectangles:
  */
  
  public void reassembleSlices() {
    
    rectList_ = new ArrayList<Rectangle2D>((new NetModuleFree()).newReassemble(this));
    return;
  }
  

  /***************************************************************************
  **
  ** Return a modified shape list that slices the module up by the bounds
  */
  
  public SliceResult sliceByAllBounds(Map<String, Rectangle> boundsForGroups, Map<String, Integer> groupZOrder, Rectangle2D nameBounds) {

    SliceResult retval = new SliceResult();
    SliceAndDicer sad = new SliceAndDicer();    
 
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      List<SliceAndDicer.OneSliceResult> osrList = sad.sliceARectByAllBounds(boundsForGroups, groupZOrder, oldShape, false);
      retval.merge(osrList);
    }
    if (nameBounds != null) {
      List<SliceAndDicer.OneSliceResult> osrList = sad.sliceARectByAllBounds(boundsForGroups, groupZOrder, nameBounds, true);
      retval.merge(osrList);  
    }
 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Move the shape.  Returns info on rigid pad moves
  */
  
  public Map<Layout.PointNoPoint, Vector2D> moveShape(NetModuleFree.IntersectionExtraInfo iexi, double dx, double dy, Set<Layout.PointNoPoint> usedPads) {
  
    HashMap<Layout.PointNoPoint, Vector2D> retval = new HashMap<Layout.PointNoPoint, Vector2D>();
 
    //
    // If we are given no boundary info, we just move the whole shape:
    //      
    
    if (iexi == null) {
      moveAllShapes(dx, dy);
      Vector2D moveVec = new Vector2D(dx, dy);
      Iterator<Layout.PointNoPoint> upit = usedPads.iterator();
      while (upit.hasNext()) {
        Layout.PointNoPoint pt = upit.next();
        retval.put(pt, moveVec);
      }  
      return (retval);
    }
    
    //
    // get the shapes to work with
    //   
    
    if ((iexi.contigInfo != null) && 
        (iexi.contigInfo.intersectedShape != null) && 
        (!iexi.contigInfo.intersectedShape.isEmpty())) {
      return (moveMatchingShapes(iexi.contigInfo.intersectedShape, dx, dy, usedPads));
    }
    
    List<LocatedIntersection> extract = extractRectangles(iexi);
    if (extract == null) {
      return (retval);
    }
    
    Iterator<LocatedIntersection> eit = extract.iterator();
    while (eit.hasNext()) {
      LocatedIntersection li = eit.next();
      Rectangle2D oldShape = li.rect;      
      double orx = oldShape.getX();
      double ory = oldShape.getY();
      double orw = oldShape.getWidth();
      double orh = oldShape.getHeight();
      if (li.extraDelta != null) {
        dx += li.extraDelta.getX();
        dy += li.extraDelta.getY();
      }
      
      //
      // If only one point is given, we just move that point.  If two points, we move
      // that segment.  If no points, we move the whole shape:
      //

      if ((iexi.endPt == null) && (iexi.startPt == null)) {  // Move whole shape
        double nrx = UiUtil.forceToGridValue(orx + dx, UiUtil.GRID_SIZE);
        double nry = UiUtil.forceToGridValue(ory + dy, UiUtil.GRID_SIZE); 
        assembleRigidPadVecForRect(oldShape, dx, dy, usedPads, retval, NONE);
        oldShape.setRect(nrx, nry, orw, orh);
      } else if ((li.where == UPPER_LEFT) || 
                 (li.where == UPPER_RIGHT) ||    
                 (li.where == LOWER_RIGHT) ||   
                 (li.where == LOWER_LEFT)) { // Move corner
        assembleRigidPadVecForRect(oldShape, dx, dy, usedPads, retval, li.where);
        moveCorner(li.where, orx, ory, orw, orh, dx, dy, oldShape);     
      } else if ((li.where == TOP) || 
                 (li.where == RIGHT) ||    
                 (li.where == BOTTOM) ||   
                 (li.where == LEFT)) { // Move edge
        assembleRigidPadVecForRect(oldShape, dx, dy, usedPads, retval, li.where);
        moveEdge(li.where, orx, ory, orw, orh, dx, dy, oldShape);        
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Move the whole shape
  */
  
  private void moveAllShapes(double dx, double dy) {
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      double orx = oldShape.getX();
      double ory = oldShape.getY();
      double orw = oldShape.getWidth();
      double orh = oldShape.getHeight();
      double nrx = UiUtil.forceToGridValue(orx + dx, UiUtil.GRID_SIZE);
      double nry = UiUtil.forceToGridValue(ory + dy, UiUtil.GRID_SIZE);    
      oldShape.setRect(nrx, nry, orw, orh);
    } 
    return;
  }  

  /***************************************************************************
  **
  ** Figure out the rigid move map for the rect
  */
  
  private void assembleRigidPadVecForRect(Rectangle2D oldShape, double dx, double dy, Set<Layout.PointNoPoint> usedPads, 
                                          Map<Layout.PointNoPoint, Vector2D> moveMap, int cornerOrSide) {
    Vector2D moveVec = new Vector2D(dx, dy);
    Map<Point2D, NetModuleFree.LinkPad> pfr = freeRender_.padsForRectangle(oldShape, null);
    Iterator<Layout.PointNoPoint> upit = usedPads.iterator();
    while (upit.hasNext()) {
      Layout.PointNoPoint pt = upit.next();
      NetModuleFree.LinkPad pad = pfr.get(pt.point);
      if (pad != null) { 
        if ((cornerOrSide == NONE) || (cornerOrSide == pad.rectSide) || cornerMatchesSide(cornerOrSide, pad.rectSide)) {
          Vector2D useVec;
          if (cornerOrSide != NONE) {
            Vector2D padNorm = pad.getNormal();
            if (padNorm.getX() == 0.0) {
              useVec = new Vector2D(0.0, dy);
            } else if (padNorm.getY() == 0.0) {
              useVec = new Vector2D(dx, 0.0);
            } else {
              useVec = moveVec;
            }
          } else {
            useVec = moveVec;
          }
          moveMap.put(pt, useVec);
        }
      }
    } 
    return; 
  }  

  /***************************************************************************
  **
  ** Move Matching shapes
  */
  
  private Map<Layout.PointNoPoint, Vector2D> moveMatchingShapes(Set<Rectangle2D> toMatch, double dx, double dy, Set<Layout.PointNoPoint> usedPads) {
    HashMap<Layout.PointNoPoint, Vector2D> retval = new HashMap<Layout.PointNoPoint, Vector2D>();
    Iterator<Rectangle2D> sit = getShapeIterator();
    while (sit.hasNext()) {
      Rectangle2D oldShape = sit.next();
      if (toMatch.contains(oldShape)) {
        assembleRigidPadVecForRect(oldShape, dx, dy, usedPads, retval, NONE);
        double orx = oldShape.getX();
        double ory = oldShape.getY();
        double orw = oldShape.getWidth();
        double orh = oldShape.getHeight();
        double nrx = UiUtil.forceToGridValue(orx + dx, UiUtil.GRID_SIZE);
        double nry = UiUtil.forceToGridValue(ory + dy, UiUtil.GRID_SIZE); 
        oldShape.setRect(nrx, nry, orw, orh);
      }
    } 
    return (retval); 
  }  
  
  /***************************************************************************
  **
  ** Figure out what underlying module rectangles are part of the given shape
  ** defined by the list of points.
  */

  
  public NetModuleFree.ContigBreakdown rectsForContig(List<Point2D> shapePoints, List<NetModuleFree.TaggedShape> shapeList) {

    //
    // Defined shapes:
    //
    
    HashSet<Rectangle2D> shapes = new HashSet<Rectangle2D>();
    Iterator<Rectangle2D> sit = getShapeIterator();  
    int numPts = shapePoints.size();
    while (sit.hasNext()) {
      Rectangle2D myshape = sit.next();
      Point2D lastPt = (numPts != 0) ? shapePoints.get(numPts - 1) : null;
      for (int i = 0; i < numPts; i++) {
        Point2D nextPt = shapePoints.get(i);
        //
        // Note that if a e.g. a rectangle contributes to an edge of a contiguous
        // region, but does not contribute a CORNER, this misses it:
        //
        int chkMatch = matchRectExact(myshape, nextPt);
        if (chkMatch != NONE) {
          shapes.add((Rectangle2D)myshape.clone());
          break;
        }
        //
        // So this checks the sides:
        //
        if (matchRectSide(myshape, nextPt, lastPt)) {
          shapes.add((Rectangle2D)myshape.clone());
          break;
        }
        lastPt = nextPt;
      }
    }
    
    //
    // Just-in-time member shapes:
    //
 
    Map<String, Rectangle2D> mr4c = otherRectsForContig(shapePoints, shapeList, NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE);
      
    NetModuleFree.ContigBreakdown retval = new NetModuleFree.ContigBreakdown(shapes, mr4c);
    return (retval);
  }

  /***************************************************************************
  **
  ** Figure out what auto-generated member-bounding rectangle or non-member rect are intersected
  ** by the given point.  Does auto members first, then non-members:
  */
  
  public NetModuleFree.TaggedShape directRectIntersect(Point2D pt, List<NetModuleFree.TaggedShape> shapeList) {
    int numSL = shapeList.size();
    for (int j = 0; j < 2; j++) {
      for (int i = 0; i < numSL; i++) {
        NetModuleFree.TaggedShape ts = shapeList.get(i);
        // First pass:
        if ((j == 0) && (ts.shapeClass != NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE)) {
          continue;
        }
        // Second pass:
        if ((j == 1) && (ts.shapeClass != NetModuleFree.TaggedShape.NON_MEMBER_SHAPE)) {
          continue;
        }       
        int chkMatch = matchRect(ts.rect, pt, UiUtil.GRID_SIZE / 2.0);
        if (chkMatch != NONE) {
          return (ts);
        }
      }
    }
    return (null);
  }    
 
  /***************************************************************************
  **
  ** Figure out what rectangles  are part of the contiguous shape defined by the list of points.
  */
  
  private Map<String, Rectangle2D> otherRectsForContig(List<Point2D> shapePoints, List<NetModuleFree.TaggedShape> shapeList, int type) {
    HashMap<String, Rectangle2D> retval = new HashMap<String, Rectangle2D>();
    int numPts = shapePoints.size();
    int numSL = shapeList.size();
    for (int i = 0; i < numSL; i++) {
      NetModuleFree.TaggedShape ts = shapeList.get(i);
      if (ts.shapeClass != type) {
        continue;
      }
      Point2D lastPt = (numPts != 0) ? shapePoints.get(numPts - 1) : null;
      for (int j = 0; j < numPts; j++) {
        Point2D nextPt = shapePoints.get(j);
        int chkMatch = matchRectExact(ts.rect, nextPt);
        if (chkMatch != NONE) {
          retval.put(ts.memberID, (Rectangle2D)ts.rect.clone());
          break;
        }
        if (matchRectSide(ts.rect, nextPt, lastPt)) {
          retval.put(ts.memberID, (Rectangle2D)ts.rect.clone());
          break;
        }
        lastPt = nextPt;        
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Figure out what underlying module rectangles are being intersected by
  ** the click.  Just guys with intersected edges (maybe more than one),
  ** not all contiguous shapes (see above)
  */

  public Set<Rectangle2D> rectsForDirect(NetModuleFree.IntersectionExtraInfo iexi) {
    HashSet<Rectangle2D> retval = new HashSet<Rectangle2D>();
    List<LocatedIntersection> exr = extractRectangles(iexi);
    if (exr == null) {
      return (retval);
    }
    int numEx = exr.size();
    for (int i = 0; i < numEx; i++) {
      LocatedIntersection li = exr.get(i);
      retval.add((Rectangle2D)li.rect.clone());
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Figure out what underlying module rectangles are being intersected by
  ** the click.  can toss certain types
  */

  public Set<Rectangle2D> specificRectsForDirect(NetModuleFree.IntersectionExtraInfo iexi, boolean noInterior, boolean noExtraDelta) {
    HashSet<Rectangle2D> retval = new HashSet<Rectangle2D>();
    List<LocatedIntersection> exr = extractRectangles(iexi);
    if (exr == null) {
      return (retval);
    }
    int numEx = exr.size();
    for (int i = 0; i < numEx; i++) {
      LocatedIntersection li = exr.get(i);
      if ((!noExtraDelta || (li.extraDelta == null)) && 
          (!noInterior || li.where != INTERIOR)) {
        retval.add((Rectangle2D)li.rect.clone());
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answer if direct intersection contains an interior point
  */

  public boolean haveInteriorRect(NetModuleFree.IntersectionExtraInfo iexi) {
    List<LocatedIntersection> exr = extractRectangles(iexi);
    if (exr == null) {
      return (false);
    }
    int numEx = exr.size();
    for (int i = 0; i < numEx; i++) {
      LocatedIntersection li = exr.get(i);
      if (li.where == INTERIOR) {
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Answer if direct intersection contains an extra delta point
  */

  public boolean haveExtraDeltaRect(NetModuleFree.IntersectionExtraInfo iexi) {
    List<LocatedIntersection> exr = extractRectangles(iexi);
    if (exr == null) {
      return (false);
    }
    int numEx = exr.size();
    for (int i = 0; i < numEx; i++) {
      LocatedIntersection li = exr.get(i);
      if (li.extraDelta != null) {
        return (true);
      }
    }
    return (false);
  } 
 
  /***************************************************************************
  **
  ** Answer if the segment formed by the two points is coincident with the
  ** an edge of the test rectangle:
  */
  
  public static boolean matchRectSide(Rectangle2D testRect, Point2D pt, Point2D pt2) {   
    LinkSegment testSeg = new LinkSegment(pt, pt2);
    Point2D cornerPt = new Point2D.Double(0.0, 0.0);
    double tol = INTERSECT_TOL_;
    //
    // Top left
    //
    cornerPt.setLocation(testRect.getX(), testRect.getY());
    if (checkSeg(testSeg, cornerPt, tol) != NONE) {
      return (true);
    }
    //
    // Top right
    //
    cornerPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY());
    if (checkSeg(testSeg, cornerPt, tol) != NONE) {
      return (true);
    }
    //
    // Bottom right
    //
    cornerPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight());
    if (checkSeg(testSeg, cornerPt, tol) != NONE) {
      return (true);
    }
    //
    // Bottom left
    //
    cornerPt.setLocation(testRect.getX(), testRect.getY() + testRect.getHeight());
    if (checkSeg(testSeg, cornerPt, tol) != NONE) {
      return (true);
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Answer if the intersection matches the rectangle, and where
  */
  
  public static int matchRectExact(Rectangle2D testRect, Point2D pt) {   
    return (matchRect(testRect, pt, INTERSECT_TOL_));   
  }
  
  /***************************************************************************
  **
  ** Answer if the intersection matches the rectangle, and where
  */
  
  public static int matchRect(Rectangle2D testRect, Point2D pt, double tol) {   
    Point2D firstPt = new Point2D.Double(0.0, 0.0);
    Point2D secondPt = new Point2D.Double(0.0, 0.0);
    LinkSegment testSeg = new LinkSegment(firstPt, secondPt);
    //
    // Top
    //
    firstPt.setLocation(testRect.getX(), testRect.getY());
    secondPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY());
    testSeg.setBoth(firstPt, secondPt);
    int which = checkSeg(testSeg, pt, tol);
    switch (which) {
      case START_:
        return (UPPER_LEFT);
      case END_:
        return (UPPER_RIGHT);
      case MIDDLE_:
        return (TOP);
      case NONE:
        break;
      default:
        throw new IllegalStateException();
    }
    //
    // Right
    //
    firstPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY());
    secondPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight());
    testSeg.setBoth(firstPt, secondPt);
    which = checkSeg(testSeg, pt, tol);
    switch (which) {
      case START_:
        return (UPPER_RIGHT);
      case END_:
        return (LOWER_RIGHT);
      case MIDDLE_:
        return (RIGHT);
      case NONE:
        break;
      default:
        throw new IllegalStateException();
    }
    //
    // Bottom
    //
    firstPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight());
    secondPt.setLocation(testRect.getX(), testRect.getY() + testRect.getHeight());
    testSeg.setBoth(firstPt, secondPt);
    which = checkSeg(testSeg, pt, tol);
    switch (which) {
      case START_:
        return (LOWER_RIGHT);
      case END_:
        return (LOWER_LEFT);
      case MIDDLE_:
        return (BOTTOM);
      case NONE:
        break;
      default:
        throw new IllegalStateException();
    }
    //
    // Left
    //
    firstPt.setLocation(testRect.getX(), testRect.getY() + testRect.getHeight());
    secondPt.setLocation(testRect.getX(), testRect.getY());
    testSeg.setBoth(firstPt, secondPt);
    which = checkSeg(testSeg, pt, tol);
    switch (which) {
      case START_:
        return (LOWER_LEFT);
      case END_:
        return (UPPER_LEFT);
      case MIDDLE_:
        return (LEFT);
      case NONE:
        break;
      default:
        throw new IllegalStateException();
    }
    return (NONE);
  } 

 
  /***************************************************************************
  **
  ** Replace the shape
  */
  
  public void replaceShape(Rectangle2D oldRect, Rectangle2D newRect) {
    int index = rectList_.indexOf(oldRect);
    rectList_.set(index, newRect);
    return;
  }       
 
  /***************************************************************************
  **
  ** Get the color tag
  */
  
  public String getColorTag() {
    return (colorTag_);
  }   
  
  /***************************************************************************
  **
  ** Set the color tag
  */
  
  public void setColorTag(String cTag) {
    colorTag_ = cTag;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor(ColorResolver cRes) {
    return (cRes.getColor(colorTag_));
  }  
  
  /***************************************************************************
  **
  ** Get the fill color tag
  */
  
  public String getFillColorTag() {
    return (fillColorTag_);
  }   
  
  /***************************************************************************
  **
  ** Set the fill color tag
  */
  
  public void setFillColorTag(String cTag) {
    fillColorTag_ = cTag;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the fill color
  */
  
  public Color getFillColor(ColorResolver cRes) {
    return (cRes.getColor(fillColorTag_));
  }
  
  /***************************************************************************
  **
  ** Get the local font (may be null)
  */
  
  public FontManager.FontOverride getFontOverride() {
    return (localFont_);
  }
  
  /***************************************************************************
  **
  ** Set the local font
  */
  
  public void setFontOverride(FontManager.FontOverride fontover) {
    localFont_ = fontover;
    return;
  }      
  
  /***************************************************************************
  **
  ** Shift entire module
  */

  public void shift(double dx, double dy) {
    int numRect = rectList_.size();
    for (int i = 0; i < numRect; i++) {
      Rectangle2D rect = rectList_.get(i);
      rect.setRect(rect.getX() + dx, rect.getY() + dy, rect.getWidth(), rect.getHeight());
    }
    if (nameLoc_ != null) {
      nameLoc_.setLocation(nameLoc_.getX() + dx, nameLoc_.getY() + dy);
    }
    return;
  } 

  /***************************************************************************
  **
  ** Change the type.  This means shapes need to be modified to suit.
  */

  public void replaceTypeAndShapes(int type, List<Rectangle2D> shapes) {
    type_ = type;
    rectList_ = new ArrayList<Rectangle2D>(shapes);
    return;
  } 
  
  
  /***************************************************************************
  **
  ** Returns the bounds of just the defined shapes.  Faster than full-blown
  ** rendering bounds, and useful when members are being taken into
  ** account separately (e.g. finding group bounds);
  */
  
  public Rectangle shapeOnlyBounds() {   
 
    Rectangle extent = null;   
    Iterator<Rectangle2D> rlit = rectList_.iterator();
    while (rlit.hasNext()) {
      Rectangle2D rec2 = rlit.next();
      Rectangle rect = UiUtil.rectFromRect2D(rec2);  
      if (extent == null) {
        extent = rect;
      } else {
        Bounds.tweakBounds(extent, rect);
      }
    }
    return (extent);
  }
  
  /***************************************************************************
  **
  ** Write the module property to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();     
    out.print("<nModProp id=\""); 
    out.print(moduleID_);
    out.print("\" color=\"");
    out.print(colorTag_);
    out.print("\" fillColor=\"");
    out.print(fillColorTag_);    
    out.print("\" type=\"");
    out.print(mapToTypeTag(type_));
    if (hideName_) {
      out.print("\" hideLabel=\"true");
    }
    out.print("\" nameFade=\"");
    out.print(mapToFadeTag(nameFadeMode_));  
    
    if (localFont_ != null) {
      out.print("\" fsize=\"");
      out.print(localFont_.size);
      out.print("\" fbold=\"");
      out.print(localFont_.makeBold);    
      out.print("\" fital=\"");
      out.print(localFont_.makeItalic);    
      out.print("\" fsans=\"");
      out.print(localFont_.makeSansSerif);
    }
    
    if (nameLoc_ != null) {
      out.print("\" labelX=\"");
      out.print(nameLoc_.getX());
      out.print("\" labelY=\"");
      out.print(nameLoc_.getY());
    }
    
    if (lineBreakDef_ != null) {
      out.print("\" breakDef=\"");
      out.print(lineBreakDef_);
    }       
    
    if (rectList_.size() == 0) {
      out.println("\" />");          
      return;
    } 
    out.println("\" >");    
    ind.up().indent();
    out.println("<nModPropShapes>");
    Iterator<Rectangle2D> mi = rectList_.iterator();
    ind.up();
    while (mi.hasNext()) {
      Rectangle2D rec = mi.next();
      writeRectXML(out, ind, rec);
    }
    ind.down().indent();
    out.println("</nModPropShapes>");
    ind.down().indent();
    out.println("</nModProp>");
    return;
  }
       
  /***************************************************************************
  **
  ** Write a shape to XML
  **
  */
  
  public void writeRectXML(PrintWriter out, Indenter ind, Rectangle2D rect) {
    ind.indent();     
    out.print("<nModPropShape x=\"");
    out.print(rect.getX());
    out.print("\" y=\"");
    out.print(rect.getY());
    out.print("\" width=\"");
    out.print(rect.getWidth());
    out.print("\" height=\"");
    out.print(rect.getHeight());    
    out.println("\" />");
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  


  /***************************************************************************
  **
  ** return closest point to segment, where points beyond endpoints 
  ** are assigned to endpoints
  */
  
  private Point2D getClosestPointHandleEndpoints(Point2D firstPt, Point2D secondPt, Point2D pt) {
    LinkSegment testSeg = new LinkSegment(firstPt, secondPt);
    Point2D closest = testSeg.getClosestPoint(pt);
    if (closest == null) {
      double dist1 = pt.distanceSq(firstPt);
      double dist2 = pt.distanceSq(secondPt);
      closest = (dist1 < dist2) ? firstPt : secondPt;
    }
    return (closest);
  }
   
  /***************************************************************************
  **
  ** Get the closest point on the given rectangle
  */
  
  private Point2D getClosestPoint(int which, Rectangle2D testRect, Point2D pt) {

    Point2D firstPt = new Point2D.Double(0.0, 0.0);
    Point2D secondPt = new Point2D.Double(0.0, 0.0);

    switch (which) {
      case NONE:
        return (null);
      case UPPER_LEFT:
        return (new Point2D.Double(testRect.getX(), testRect.getY()));
      case TOP:
        firstPt.setLocation(testRect.getX(), testRect.getY());
        secondPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY());
        return (getClosestPointHandleEndpoints(firstPt, secondPt, pt));
      case UPPER_RIGHT:
        return (new Point2D.Double(testRect.getX() + testRect.getWidth(), testRect.getY()));
      case RIGHT:
        firstPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY());
        secondPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight());
        return (getClosestPointHandleEndpoints(firstPt, secondPt, pt));
      case LOWER_RIGHT:
        return (new Point2D.Double(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight()));
      case BOTTOM:
        firstPt.setLocation(testRect.getX() + testRect.getWidth(), testRect.getY() + testRect.getHeight());
        secondPt.setLocation(testRect.getX(), testRect.getY() + testRect.getHeight());
        return (getClosestPointHandleEndpoints(firstPt, secondPt, pt));
      case LOWER_LEFT:
        return (new Point2D.Double(testRect.getX(), testRect.getY() + testRect.getHeight()));
      case LEFT:
        firstPt.setLocation(testRect.getX(), testRect.getY() + testRect.getHeight());
        secondPt.setLocation(testRect.getX(), testRect.getY());
        return (getClosestPointHandleEndpoints(firstPt, secondPt, pt));
      default:
        throw new IllegalStateException();
    }
  }  
  
  /***************************************************************************
  **
  ** Answer if corner is an alias for a side (yuk : shoulda used bitwise OR!)
  */
  
  private boolean cornerMatchesSide(int corner, int side) {

    switch (corner) {
      case NONE:
        return (false);
      case UPPER_LEFT:
        return ((side == TOP) || (side == LEFT));
      case UPPER_RIGHT:
        return ((side == TOP) || (side == RIGHT));
      case LOWER_LEFT:
        return ((side == BOTTOM) || (side == LEFT));
      case LOWER_RIGHT:
        return ((side == BOTTOM) || (side == RIGHT));
      case TOP:
      case RIGHT:
      case BOTTOM:
      case LEFT:
        return (false);
      default:
        throw new IllegalArgumentException();
    }
  }  
  
  /***************************************************************************
  **
  ** Move given corner
  */

  private void moveCorner(int whichCorner, double orx, double ory, double orw, double orh, 
                          double dx, double dy, Rectangle2D rect) {    
    double nrx;
    double nry;
    double nrw;
    double nrh;
    dx = UiUtil.forceToGridValue(dx, UiUtil.GRID_SIZE);
    dy = UiUtil.forceToGridValue(dy, UiUtil.GRID_SIZE);  
    switch (whichCorner) {
      case UPPER_LEFT:
        nrx = orx + dx;
        nry = ory + dy;
        nrw = orw - dx;
        nrh = orh - dy;
        break;
      case UPPER_RIGHT:
        nrx = orx;
        nry = ory + dy;
        nrw = orw + dx;
        nrh = orh - dy;
        break;
      case LOWER_LEFT:
        nrx = orx + dx;
        nry = ory;
        nrw = orw - dx;
        nrh = orh + dy;
        break;
      case LOWER_RIGHT:
        nrx = orx;
        nry = ory;
        nrw = orw + dx;
        nrh = orh + dy;
        break;
      default:
        throw new IllegalArgumentException();
    }
    if (nrw == 0.0) {
      nrw = UiUtil.GRID_SIZE;
    } else if (nrw < 0.0) {
      nrx = nrx + nrw;
      nrw = -nrw;
    }
    
    if (nrh == 0.0) {
      nrh = UiUtil.GRID_SIZE;
    } else if (nrh < 0.0) {
      nry = nry + nrh;
      nrh = -nrh;
    }
    
    rect.setRect(nrx, nry, nrw, nrh);                 
    return;
  }
 
  /***************************************************************************
  **
  ** Move given edge
  */

  private void moveEdge(int whichEdge, double orx, double ory, double orw, double orh, 
                        double dx, double dy, Rectangle2D rect) {    
    double nrx;
    double nry;
    double nrw;
    double nrh;
    dx = UiUtil.forceToGridValue(dx, UiUtil.GRID_SIZE);
    dy = UiUtil.forceToGridValue(dy, UiUtil.GRID_SIZE);
    switch (whichEdge) {
      case TOP:
        nrx = orx;
        nry = ory + dy;
        nrw = orw;
        nrh = orh - dy;
        break;
      case RIGHT:
        nrx = orx;
        nry = ory;
        nrw = orw + dx;
        nrh = orh;
        break;
      case LEFT:
        nrx = orx + dx;
        nry = ory;
        nrw = orw - dx;
        nrh = orh;
        break;
      case BOTTOM:
        nrx = orx;
        nry = ory;
        nrw = orw;
        nrh = orh + dy;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    //
    // Fixes rectangles turned inside out:
    //
    
    if (nrw == 0.0) {
      nrw = UiUtil.GRID_SIZE;
    } else if (nrw < 0.0) {
      nrx = nrx + nrw;
      nrw = -nrw;
    }
    
    if (nrh == 0.0) {
      nrh = UiUtil.GRID_SIZE;
    } else if (nrh < 0.0) {
      nry = nry + nrh;
      nrh = -nrh;
    }
    
    rect.setRect(nrx, nry, nrw, nrh);                 
    return;
  } 
  
  /***************************************************************************
  **
  ** Extract matching rectangles for intersection
  */
  
  private List<LocatedIntersection> extractRectangles(NetModuleFree.IntersectionExtraInfo iexi) {   
    //
    // Depends on the type:
    //
    
    Point2D pt = iexi.intersectPt;
    
    ArrayList<LocatedIntersection> retval = new ArrayList<LocatedIntersection>();
    switch (getType()) {
      case NetModuleProperties.CONTIG_RECT:
        if (!hasOneShape()) {
          throw new IllegalStateException();
        } 
        Rectangle2D rect = getShapeIterator().next();
        if (iexi.type == NetModuleFree.IntersectionExtraInfo.IS_INTERNAL) {
          retval.add(new LocatedIntersection(INTERIOR, rect, null));
          return (retval);
        } else {
          int where = matchRectExact(rect, pt);  // MAY BE NOWHERE!
          Vector2D extraDelta = null;
          if (where == NONE) {
            where = iexi.outerMatch;
            Point2D closestPoint = getClosestPoint(where, rect, pt);
            extraDelta = (closestPoint == null) ? null : new Vector2D(closestPoint, pt);
          }
          retval.add(new LocatedIntersection(where, rect, extraDelta));
          return (retval);
        }
      case NetModuleProperties.MULTI_RECT:
        Iterator<Rectangle2D> sit = getShapeIterator();
        while (sit.hasNext()) {
          Rectangle2D nextRect = sit.next();
          if (iexi.type == NetModuleFree.IntersectionExtraInfo.IS_INTERNAL) {
            if (nextRect.contains(pt)) {
              retval.add(new LocatedIntersection(INTERIOR, nextRect, null));
            }
          } else {
            int where2 = matchRectExact(nextRect, pt);
            if (where2 != NONE) {
              retval.add(new LocatedIntersection(where2, nextRect, null));
            }
          }
        }
        return ((retval.isEmpty()) ? null : retval);
      case NetModuleProperties.MEMBERS_ONLY:
        return (null);
      default:
        throw new IllegalArgumentException();
    }    
  }
   
  /***************************************************************************
  **
  ** Check the segment for intersection
  */  
  
  private static int checkSeg(LinkSegment seg, Point2D pt, double tol) {
    if (seg.intersectsStart(pt, tol) != null) {
      return (START_); 
    } else if (seg.intersectsEnd(pt, tol) != null) {
      return (END_);               
    } else if (seg.intersects(pt, tol) != null) {
      return (MIDDLE_);
    } else {
      return (NONE);
    }
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetModulePropertiesWorker extends AbstractFactoryClient {
  
    public NetModulePropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nModProp");
      installWorker(new NMPShapeWorker(whiteboard), new MyGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nModProp")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netModProps = buildFromXML(elemName, attrs);
        retval = board.netModProps;
      }
      return (retval);     
    }
    
    private NetModuleProperties buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "id", true);
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "color", true);
      String fillColor = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "fillColor", true);
      // False arg and default only to support development files.  Should not be used in production code!
      //String type = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "type", false);
      //if (type == null) {
      //  type = CONTIG_STR_;
      //}
      String type = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "type", true);
      /*
      // Here only to support development files.  Should not be needed in production code...
      String xStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "x", false);
      String yStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "y", false);
      String wStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "width", false);
      String hStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "height", false);
      if (xStr != null) {
        try {
          double x = Double.parseDouble(xStr);
          double y = Double.parseDouble(yStr);
          double w = Double.parseDouble(wStr);
          double h = Double.parseDouble(hStr);
          Rectangle2D devOnly = new Rectangle2D.Double(x, y, w, h);
          ArrayList shapeList = new ArrayList();
          shapeList.add(devOnly);
          int typeVal;
          try {
            typeVal = mapFromTypeTag(type);
          } catch (IllegalArgumentException iax) {
            throw new IOException();
          }
          
          NetModuleProperties retval = new NetModuleProperties(id, typeVal, shapeList);
          retval.setColorTag(color);
          retval.setFillColorTag(fillColor);
          return (retval);   
        } catch (NumberFormatException nfe) {
          throw new IOException();
        }
      }
       */
      // ------------------
      
      
      // Label stuff
      String hideLabelStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "hideLabel", false);
      String labelXStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "labelX", false);
      String labelYStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "labelY", false);
      String fadeModeStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "nameFade", false);
      
      boolean hideLabel = (hideLabelStr == null) ? false : Boolean.valueOf(hideLabelStr).booleanValue();
      
      Point2D labelLoc = null;
      if ((labelXStr != null) && (labelYStr != null)) {
        try {
          double labelX = Double.parseDouble(labelXStr);
          double labelY = Double.parseDouble(labelYStr);
          labelLoc = new Point2D.Double(labelX, labelY);
        } catch (NumberFormatException nfe) {
          throw new IOException();
        }
      }
      
      String breakDef = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "breakDef", false);

      NetModuleProperties retval = new NetModuleProperties(id, type, color, fillColor, breakDef, 
                                                           hideLabel, fadeModeStr, labelLoc);
      
      // Font override stuff:
      String fsize = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "fsize", false);
      String fbold = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "fbold", false);
      String fital = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "fital", false);
      String fsans = AttributeExtractor.extractAttribute(elemName, attrs, "nModProp", "fsans", false);

      
      if (fsize != null) {      
        if ((fbold == null) || (fital == null) || (fsans == null)) {
          throw new IOException();
        }
        boolean makeBold = Boolean.valueOf(fbold).booleanValue();
        boolean makeItalic = Boolean.valueOf(fital).booleanValue();
        boolean makeSansSerif = Boolean.valueOf(fsans).booleanValue();

        int size;
        try {
          size = Integer.parseInt(fsize); 
        } catch (NumberFormatException nfe) {
          throw new IOException();
        }    
        if ((size < FontManager.MIN_SIZE) || (size > FontManager.MAX_SIZE)) {
          throw new IOException();
        }
        FontManager.FontOverride fo = new FontManager.FontOverride(size, makeBold, makeItalic, makeSansSerif);
        retval.setFontOverride(fo);
      }
      
      return (retval);
    }
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModuleProperties nmp = board.netModProps;
      Rectangle2D shape = board.nmpShape;
      nmp.addShape(shape);
      return (null);
    }
  } 
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NMPShapeWorker extends AbstractFactoryClient {
 
    public NMPShapeWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nModPropShape");
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nModPropShape")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nmpShape = buildFromXML(elemName, attrs);
        retval = board.nmpShape;
      }
      return (retval);     
    }
    
    private Rectangle2D buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String xStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModPropShape", "x", true);
      String yStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModPropShape", "y", true);
      String wStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModPropShape", "width", true);
      String hStr = AttributeExtractor.extractAttribute(elemName, attrs, "nModPropShape", "height", true);
      try {
        double x = Double.parseDouble(xStr);
        double y = Double.parseDouble(yStr);
        double w = Double.parseDouble(wStr);
        double h = Double.parseDouble(hStr);
        return (new Rectangle2D.Double(x, y, w, h));
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return possible type choices values
  */
  
  public static Vector<ChoiceContent> getDisplayTypes(BTState appState, boolean includeMemberOnly) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_TYPES_; i++) {
      if ((i == MEMBERS_ONLY) && !includeMemberOnly) {
        continue;
      }
      retval.add(typeForCombo(appState, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent typeForCombo(BTState appState, int type) {
    return (new ChoiceContent(mapTypeToDisplay(appState, type), type));
  }  

  /***************************************************************************
  **
  ** Map display types
  */

  public static String mapTypeToDisplay(BTState appState, int type) {
    String typeTag = mapToTypeTag(type);
    return (appState.getRMan().getString("nModProp." + typeTag));
  }  
  
  /***************************************************************************
  **
  ** Map display types to instructions
  */

  public static String mapTypeToInstruction(BTState appState, int type) {
    String typeTag = mapToTypeTag(type);
    return (appState.getRMan().getString("nModProp.instruction_" + typeTag));
  }    
  
  /***************************************************************************
  **
  ** Map types to type tags
  */

  public static String mapToTypeTag(int val) {
    switch (val) {
      case CONTIG_RECT:
        return (CONTIG_STR_);
      case MULTI_RECT:
        return (MULTI_STR_);
      case MEMBERS_ONLY:
        return (MEMBERS_STR_);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map type tags to types
  */

  public static int mapFromTypeTag(String tag) {
    if (tag.equals(CONTIG_STR_)) {
      return (CONTIG_RECT);
    } else if (tag.equals(MULTI_STR_)) {
      return (MULTI_RECT);
    } else if (tag.equals(MEMBERS_STR_)) {
      return (MEMBERS_ONLY);
    } else {
      throw new IllegalArgumentException();
    }
  }    

  /***************************************************************************
  **
  ** Return possible name fade choices values
  */
  
  public static Vector<ChoiceContent> getNameFades(BTState appState) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_FADES_; i++) {
      retval.add(fadeForCombo(appState, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent fadeForCombo(BTState appState, int fade) {
    return (new ChoiceContent(mapFadeToDisplay(appState, fade), fade));
  }  

  /***************************************************************************
  **
  ** Map fade types
  */

  public static String mapFadeToDisplay(BTState appState, int fade) {
    String fadeTag = mapToFadeTag(fade);
    return (appState.getRMan().getString("nModProp." + fadeTag));
  }
  
  /***************************************************************************
  **
  ** Map fades to fade tags
  */

  public static String mapToFadeTag(int val) {
    switch (val) {
      case FADE_QUICKLY:
        return (FADE_QUICKLY_STR_);
      case FADE_SLOWLY:
        return (FADE_SLOWLY_STR_);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map fade tags to fades
  */

  public static int mapFromFadeTag(String tag) {
    if (tag.equals(FADE_QUICKLY_STR_)) {
      return (FADE_QUICKLY);
    } else if (tag.equals(FADE_SLOWLY_STR_)) {
      return (FADE_SLOWLY);
    } else {
      throw new IllegalArgumentException();
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class SliceResult {
    public HashMap<String, List<TaggedShape>> ownedByGroup;
    public HashSet<TaggedShape> unclaimed;
    public ArrayList<MultiClaim> multiClaimed;
    
    SliceResult() {
      ownedByGroup = new HashMap<String, List<TaggedShape>>();
      unclaimed = new HashSet<TaggedShape>();
      multiClaimed = new ArrayList<MultiClaim>();
    }
    
    void merge(List<SliceAndDicer.OneSliceResult> osrList) {
      int numOsr = osrList.size();
      for (int i = 0; i < numOsr; i++) {      
        SliceAndDicer.OneSliceResult osr = osrList.get(i);
        if (osr.status == SliceAndDicer.Claim.UNCLAIMED) {
          unclaimed.add(new TaggedShape(null, osr.shape, osr.isName));
        } else if (osr.status == SliceAndDicer.Claim.MULTI_CLAIMED) {
          MultiClaim mc = new MultiClaim(osr.groups, osr.shape, osr.isName);
          multiClaimed.add(mc);
        } else if (osr.status == SliceAndDicer.Claim.OWNED_BY_GROUP) {
          String grpID = osr.groups.iterator().next();
          List<TaggedShape> obg = ownedByGroup.get(grpID);
          if (obg == null) {
            obg = new ArrayList<TaggedShape>();
            ownedByGroup.put(grpID, obg);
          }
          obg.add(new TaggedShape(null, osr.shape, osr.isName));
        }
      }
      return;
    }
  }
  
  public static class MultiClaim {
    public HashSet<String> groups;
    public Rectangle2D shape;
    public boolean isName;
    
    MultiClaim(Set<String> allGroups, Rectangle2D shape, boolean isName) {
      groups = new HashSet<String>(allGroups);
      this.shape = (Rectangle2D)shape.clone();
      this.isName = isName;
    }
  } 
  
  public static class TaggedShape {
    public boolean isName;
    public Rectangle2D oldShape;
    public Rectangle2D shape;
    
    public TaggedShape(Rectangle2D oldShape, Rectangle2D shape, boolean isName) {
      this.isName = isName;
      this.oldShape = (oldShape == null) ? null : (Rectangle2D)oldShape.clone();
      this.shape = (Rectangle2D)shape.clone();
    }
  }   

  /***************************************************************************
  **
  ** For intersections
  */  
      
  class LocatedIntersection {
    int where;
    Rectangle2D rect;
    Vector2D extraDelta;
     
    LocatedIntersection(int where, Rectangle2D rect, Vector2D extraDelta) {
     this.where = where;
     this.rect = rect;
     this.extraDelta = extraDelta;
    } 
  }

}
