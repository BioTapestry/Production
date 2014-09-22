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

package org.systemsbiology.biotapestry.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/****************************************************************************
**
** A class to assist with finding intersections
*/

public class QuadTree {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////    
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private QuadNode root_;
  private Stash objCache_;
  private int binSize_;
  private int pointSize_;
  private int forcedCenterX_;
  private int forcedCenterY_;   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public QuadTree(int pointSize, int binSizeMult) {
    objCache_ = new Stash();
    root_ = objCache_.getQuad();
    pointSize_ = pointSize;
    binSize_ = pointSize * binSizeMult;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Initialize the tree
  */

  public void initialize(Rectangle rect) {
    objCache_.init();
    root_ = objCache_.getQuad();
    
    int origMinX = rect.x;
    int origMinY = rect.y;
    int origMaxX = rect.x + rect.width;
    int origMaxY = rect.y + rect.height; 
    forcedCenterX_ = (int)UiUtil.forceToGridValue(rect.getCenterX(), binSize_);
    forcedCenterY_ = (int)UiUtil.forceToGridValue(rect.getCenterY(), binSize_);   

    int halfSize = binSize_;
    while (true) {  
      int testMinX = forcedCenterX_ - halfSize;
      int testMinY = forcedCenterY_ - halfSize;
      int testMaxX = forcedCenterX_ + halfSize;
      int testMaxY = forcedCenterY_ + halfSize;
      if ((testMinX <= origMinX) && (testMinY <= origMinY) && (testMaxX >= origMaxX) && (testMaxY >= origMaxY)) {
        break;
      }
      halfSize *= 2;
    }

    int upperLX = forcedCenterX_ - halfSize;
    int upperLY = forcedCenterY_ - halfSize;    
    root_.init(upperLX, upperLY, halfSize * 2, false);
    return;
  }
  
  /***************************************************************************
  **
  ** Force to grid val. Argument modified.
  */

  public void binThePoint(Point coords) {
    coords.setLocation(forcedCenterX_ + (int)UiUtil.forceToGridValue(coords.getX() - forcedCenterX_, pointSize_),
                       forcedCenterY_ + (int)UiUtil.forceToGridValue(coords.getY() - forcedCenterY_, pointSize_));
    return;
  }
  
  /***************************************************************************
  **
  ** Add a set of points to the tree
  */

  public List<TaggedPoint> addPoints(double startX, double endX, double startY, double endY, Object data) {
    List<TaggedPoint> retval = objCache_.getTaggedList();
    if (startX == endX) {
      double gridX = UiUtil.forceToGridValueMin(startX, pointSize_);
      double currY = UiUtil.forceToGridValueMin(Math.min(startY, endY), pointSize_);
      double lastY = UiUtil.forceToGridValueMax(Math.max(startY, endY), pointSize_);
      while (currY <= lastY) {
        TaggedPoint tp = objCache_.getTagged();
        tp.init(gridX, currY, data);
        retval.add(tp);
        currY += pointSize_;
      }
    } else if (startY == endY) {
      double gridY = UiUtil.forceToGridValueMin(startY, pointSize_);
      double currX = UiUtil.forceToGridValueMin(Math.min(startX, endX), pointSize_);
      double lastX = UiUtil.forceToGridValueMax(Math.max(startX, endX), pointSize_);
      while (currX <= lastX) {
        TaggedPoint tp = objCache_.getTagged();
        tp.init(currX, gridY, data);
        retval.add(tp);
        currX += pointSize_;
      }      
    } else {
      System.err.println(startX + " " + endX + " " + startY + " " + endY);
      throw new IllegalArgumentException();
    }
    
    Iterator<TaggedPoint> tpit = retval.iterator();
    while (tpit.hasNext()) {
      TaggedPoint tp = tpit.next();
      root_.addPoint(tp, objCache_, binSize_);
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a list of colliding points
  */

  public List<TaggedPoint> getCollisions(List<TaggedPoint> tps) {
    List<TaggedPoint> retval = objCache_.getTaggedList();   
    return (root_.getPoints(tps, retval));
  }
 
  /***************************************************************************
  **
  ** Get a count of nodes
  */

  public int nodeCount() {
    return (root_.kidCount());
  }
  
  /***************************************************************************
  **
  ** Get a count of points
  */

  public int pointCount() {
    return (root_.pointCount());
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static class Stash {
    private ArrayList<QuadNode> allQuads_;
    private ArrayList<QuadNode[]> allQuadsArr_;
    private ArrayList<TaggedPoint> allTagged_;
    private ArrayList<ArrayList<TaggedPoint>> allTaggedLists_;
    
    private ArrayList<QuadNode> freeQuads_;
    private ArrayList<QuadNode[]> freeQuadsArr_;
    private ArrayList<TaggedPoint> freeTagged_;
    private ArrayList<ArrayList<TaggedPoint>> freeTaggedLists_;
    
    
    Stash() {
      allQuads_ = new ArrayList<QuadNode>();
      allQuadsArr_ = new ArrayList<QuadNode[]>();
      allTagged_ = new ArrayList<TaggedPoint>();
      allTaggedLists_ = new ArrayList<ArrayList<TaggedPoint>>();
  
      freeQuads_ = new ArrayList<QuadNode>();
      freeQuadsArr_ = new ArrayList<QuadNode[]>();
      freeTagged_ = new ArrayList<TaggedPoint>();
      freeTaggedLists_ = new ArrayList<ArrayList<TaggedPoint>>();
    }
    
    void init() {
      Iterator<QuadNode> it = allQuads_.iterator();
      while (it.hasNext()) {
        QuadNode qn = it.next();
        qn.clear();
      }
      
      Iterator<QuadNode[]> qait = allQuadsArr_.iterator();
      while (qait.hasNext()) {
        QuadNode[] qna = qait.next();
        for (int i = 0; i < 4; i++) {
          qna[i] = null;
        }
      } 
      
      Iterator<TaggedPoint> atit = allTagged_.iterator();
      while (atit.hasNext()) {
        TaggedPoint tp = atit.next();
        tp.clear();
      } 
      
      Iterator<ArrayList<TaggedPoint>> tlit = allTaggedLists_.iterator();
      while (tlit.hasNext()) {
        ArrayList<TaggedPoint> tpl = tlit.next();
        tpl.clear();
      } 
   
      freeQuads_.clear();
      freeQuads_.addAll(allQuads_);
      freeQuadsArr_.clear();
      freeQuadsArr_.addAll(allQuadsArr_);
      freeTagged_.clear();
      freeTagged_.addAll(allTagged_);
      freeTaggedLists_.clear();
      freeTaggedLists_.addAll(allTaggedLists_);

      return;
    }
 
    QuadNode getQuad() {
      if (freeQuads_.isEmpty()) {
        QuadNode nQuad = new QuadNode();
        allQuads_.add(nQuad);
        return (nQuad);
      } else {
        return (freeQuads_.remove(freeQuads_.size() - 1));
      }      
    }
    
    QuadNode[] getQuadArr() {
      if (freeQuadsArr_.isEmpty()) {
        QuadNode[] nQuadArr = new QuadNode[4];
        allQuadsArr_.add(nQuadArr);
        return (nQuadArr);
      } else {
        return (freeQuadsArr_.remove(freeQuadsArr_.size() - 1));
      }      
    }
    
    TaggedPoint getTagged() {
      if (freeTagged_.isEmpty()) {
        TaggedPoint tp = new TaggedPoint();
        allTagged_.add(tp);
        return (tp);
      } else {
        return (freeTagged_.remove(freeTagged_.size() - 1));
      }      
    }
    
    ArrayList<TaggedPoint> getTaggedList() {
      if (freeTaggedLists_.isEmpty()) {
        ArrayList<TaggedPoint> tpl = new ArrayList<TaggedPoint>();
        allTaggedLists_.add(tpl);
        return (tpl);
      } else {
        return (freeTaggedLists_.remove(freeTaggedLists_.size() - 1));
      }      
    }
  }

  private static class QuadNode {
    
    private static final int NUM_POINTS_ = 6;
    private static final int NW_ = 0;
    private static final int NE_ = 1;
    private static final int SW_ = 2;
    private static final int SE_ = 3;
    
    private Point upperLeft_;
    private int side_;
    private QuadNode[] kids_;
    private HashMap<Point, ArrayList<TaggedPoint>> points_;
    private int numPoints_;
    private boolean isMin_;
    
    QuadNode() {
      upperLeft_ = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
      points_ = new HashMap<Point, ArrayList<TaggedPoint>>();
    }
      
    void init(int ulx, int uly, int side, boolean isMin) {
      upperLeft_.setLocation(ulx, uly);
      side_ = side;
      isMin_ = isMin;
    }
    
    int kidCount() {
      int retval = 1;
      if (kids_ != null) {
        for (int i = 0; i < 4; i++) {
         retval += kids_[i].kidCount();
        }
      }
      return (retval);
    }
    
    int pointCount() {
      int retval = numPoints_;
      if (kids_ != null) {
        for (int i = 0; i < 4; i++) {
         retval += kids_[i].pointCount();
        }
      }
      return (retval);
    }
  
    void clear() {
      upperLeft_.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
      if (kids_ != null) {
        for (int i = 0; i < 4; i++) {
          kids_[i] = null;
        }
      }
      kids_ = null;
      side_ = 0;
      isMin_ = false;
      Iterator<Point> pit = points_.keySet().iterator();
      while (pit.hasNext()) {
        Point key = pit.next();
        ArrayList<TaggedPoint> altp = points_.get(key);
        altp.clear();
      }
      points_.clear();
      numPoints_ = 0;
      return;
    }

    boolean containsPoint(TaggedPoint tp) {
      double ulx = upperLeft_.getX();
      double uly = upperLeft_.getY();
      double lx = tp.getX();
      double ly = tp.getY();
      if (lx < ulx) {
        return (false);
      }
      if (lx >= ulx + side_) {
        return (false);
      }
      if (ly < uly) {
        return (false);
      }
      if (ly >= uly + side_) {
        return (false);
      }
      return (true);
    }
         
    boolean addPoint(TaggedPoint loc, Stash stash, int minSide) {
      if (!containsPoint(loc)) {
        return (false);
      }
      
      if ((numPoints_ < NUM_POINTS_) || isMin_) {
        ArrayList<TaggedPoint> forPt = points_.get(loc.point_);
        if (forPt == null) {
          forPt = stash.getTaggedList();
          points_.put(loc.point_, forPt);
          numPoints_++;
        }
        forPt.add(loc);
        return (true);
      }
      
      if (kids_ == null) {
        kids_ = stash.getQuadArr();
        for (int i = 0; i < 4; i++) {
          kids_[i] = stash.getQuad();
        }
        
        boolean nowMin = false;
        int halfSide = side_ / 2;
        if (halfSide < minSide) {
          throw new IllegalArgumentException();
        } else if (halfSide == minSide) {
          nowMin = true;
        }
        
        kids_[NW_].init(upperLeft_.x, upperLeft_.y, halfSide, nowMin);
        kids_[NE_].init(upperLeft_.x + halfSide, upperLeft_.y, halfSide, nowMin);
        kids_[SW_].init(upperLeft_.x, upperLeft_.y + halfSide, halfSide, nowMin);
        kids_[SE_].init(upperLeft_.x + halfSide, upperLeft_.y + halfSide, halfSide, nowMin);
      }
      
      for (int i = 0; i < 4; i++) {
        if (kids_[i].addPoint(loc, stash, minSide)) {
          return (true);
        }
      }     
      return (false);
    }
    
    List<TaggedPoint> getPoints(List<TaggedPoint> tps, List<TaggedPoint> retval) {
      int numLocs = tps.size();
      for (int i = 0; i < numLocs; i++) {
        TaggedPoint tp = tps.get(i);
        if (containsPoint(tp)) {
          ArrayList<TaggedPoint> forPt = points_.get(tp.getPoint());
          if (forPt != null) {           
            retval.addAll(forPt);
          }
        }
      }
      if (kids_ == null) {
        return (retval);
      }
      for (int i = 0; i < 4; i++) {
        kids_[i].getPoints(tps, retval);
      }
      return (retval);
    }
  }
  
  public static class TaggedPoint {  
    private Point point_;
    public Object data;
   
    TaggedPoint() {
      this.point_ = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
    } 
    
    void init(double px, double py, Object data) {
      point_.setLocation(px, py);
      this.data = data;
    } 
    
    boolean sameLocation(TaggedPoint other) {
      return ((this.point_.getX() == other.point_.getX()) && (this.point_.getY() == other.point_.getY()));
    } 
    
    Point getPoint() {
      return (point_);
    } 
    
    double getX() {
      return (point_.getX());
    } 
    
    double getY() {
      return (point_.getY());
    } 
    
    void clear() {
      point_.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
      this.data = null;
    } 

  }
}
