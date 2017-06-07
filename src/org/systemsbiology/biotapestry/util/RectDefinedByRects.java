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

package org.systemsbiology.biotapestry.util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/****************************************************************************
**
** Defines a Rectangle in terms of other rectangles
*/

public class RectDefinedByRects {
  
  private Rectangle2D myOrig_;
  private HashMap<TaggedRect, Rectangle2D> myDef_;
  private double deltaMinX_;
  private double deltaMinY_;
  private double deltaMaxX_;
  private double deltaMaxY_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Define the given rectangle in terms of the other rectangle
  */
  
  public RectDefinedByRects(Rectangle2D myRect, Set<TaggedRect> otherRects) {
    //
    // For each rectangle, find the intersection.  If there is one, store that 
    // intersection in a map.   Then find the bounding rect defined by those
    // intersections and determine our additional dimensions outside that
    // bound.
    //
    
    ArrayList<TaggedRect> fixedOrder = new ArrayList<TaggedRect>(otherRects);
    Collections.sort(fixedOrder, new RectOrdering());
    
    myDef_ = new HashMap<TaggedRect, Rectangle2D>();
    myOrig_ = (Rectangle2D)myRect.clone();
    Iterator<TaggedRect> orit = fixedOrder.iterator();
    Rectangle2D interUnion = null;
    while (orit.hasNext()) {
      TaggedRect otherRect = orit.next();
      Rectangle2D interRect = myRect.createIntersection(otherRect.rect);
      //
      // Non-intersection is indicated by negative height and/or width:
      // Edge sharing by zero height OR width
      // Corner sharing by zero height AND width
      //
      if ((interRect.getHeight() >= 0.0) && (interRect.getWidth() >= 0.0)) {
        myDef_.put(otherRect.clone(), interRect);
        if (interUnion == null) {
          interUnion = interRect;
        } else {
          interUnion = interUnion.createUnion(interRect);
        }
      }
    }
    
    if (interUnion != null) {
      deltaMinX_ = interUnion.getMinX() - myOrig_.getMinX();
      deltaMinY_ = interUnion.getMinY() - myOrig_.getMinY();
      deltaMaxX_ = myOrig_.getMaxX() - interUnion.getMaxX();
      deltaMaxY_ = myOrig_.getMaxY() - interUnion.getMaxY();
    }
  }

 /***************************************************************************
  **
  ** Answer if we got a definition....
  */
  
  public boolean isDefined() {
    return (!myDef_.isEmpty());
  }  
  
 /***************************************************************************
  **
  ** Find out the keys we need to get defined
  */
  
  public Set<TaggedRect> definedByKeys() {
    return (myDef_.keySet());
  }    
  
  /***************************************************************************
  **
  ** This rectangle is defined in terms of other rectangles.  Given a map of
  ** how those rectangles are now positioned, return the corresponding definition
  ** of this rectangle.
  */
  
  public Rectangle2D generateNewRect(Map<TaggedRect, Rectangle2D> oldToNew) {
    //
    // For each defining rectangle, convert the overlap based on the new geometry.
    // Take the union, then increase it based on the deltas.
    //
    
    Point2D interPt = new Point2D.Double();
    Rectangle2D interUnion = null;
    
    ArrayList<TaggedRect> fixedOrder = new ArrayList<TaggedRect>(myDef_.keySet());
    Collections.sort(fixedOrder, new RectOrdering());
    
    Iterator<TaggedRect> mdkit = fixedOrder.iterator();
    while (mdkit.hasNext()) {
      TaggedRect defTagRect = mdkit.next();
      Rectangle2D defRect = defTagRect.rect;
      Rectangle2D interRect = myDef_.get(defTagRect);
      Rectangle2D transDefRect = oldToNew.get(defTagRect);
      
      Point2D ulp = new Point2D.Double(interRect.getX(), interRect.getY());
      convertPtToRectFrame(defRect, ulp, interPt);
      Point2D transUlp = new Point2D.Double();
      convertNormPtToPt(transDefRect, interPt, transUlp);
      
      Point2D lrp = new Point2D.Double(interRect.getMaxX(), interRect.getMaxY());
      convertPtToRectFrame(defRect, lrp, interPt);
      Point2D transLrp = new Point2D.Double();
      convertNormPtToPt(transDefRect, interPt, transLrp);
  
      Rectangle2D transInterRect = new Rectangle2D.Double(transUlp.getX(), transUlp.getY(), transLrp.getX() - transUlp.getX(), transLrp.getY() - transUlp.getY());
      if (interUnion == null) {
        interUnion = transInterRect;
      } else {
        interUnion = interUnion.createUnion(transInterRect);
      }
    }
    
    double retX = interUnion.getMinX() - deltaMinX_;
    double retY = interUnion.getMinY() - deltaMinY_;
    double retMaxX = deltaMaxX_ + interUnion.getMaxX();
    double retMaxY =  deltaMaxY_ + interUnion.getMaxY();
    return (new Rectangle2D.Double(retX, retY, retMaxX - retX, retMaxY - retY));
 
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Given a point in a rectangle, fill in the result with the point in rectangle
  ** frame coordinates (normalized)
  */
  
  private void convertPtToRectFrame(Rectangle2D rect, Point2D pt, Point2D result) {
    double delX = pt.getX() - rect.getX();
    double delY = pt.getY() - rect.getY();
    double rW = rect.getWidth();
    double rH = rect.getHeight();
    double rcx = (rW == 0.0) ? 0.0 : delX / rW;
    double rcy = (rH == 0.0) ? 0.0 : delY / rH;
    result.setLocation(rcx, rcy);
    return;
  } 
  
  /***************************************************************************
  **
  ** Inverse of above
  */
  
  private void convertNormPtToPt(Rectangle2D rect, Point2D normPt, Point2D result) {
    //
    // For each defining rectangle, convert the overlap based on the new geometry.
    // Take the union, then increase it based on the deltas.
    //
    
    double delX = normPt.getX() * rect.getWidth();
    double delY = normPt.getY() * rect.getHeight();
    
    double rcx = rect.getX() + delX;
    double rcy = rect.getY() + delY;
    result.setLocation(rcx, rcy);
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Used to wrap the defining rectangle (provides an ID tag)
  */
  
  public static class TaggedRect implements Cloneable {
    
    public Rectangle2D rect;
    public Object tag;
    
    // Tag should be immutable!!
    
    public TaggedRect(Rectangle2D rect, Object tag) {
      this.rect = rect;
      this.tag = tag;
    }
    
    @Override
    public TaggedRect clone() {
      try {
        TaggedRect newVal = (TaggedRect)super.clone();
        newVal.rect = (Rectangle2D)this.rect.clone();
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }    
    }
    
    @Override
    public int hashCode() {
      return (rect.hashCode() + tag.hashCode());
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TaggedRect)) {
        return (false);
      }
      TaggedRect otherTR = (TaggedRect)other;
      
      if (!this.tag.equals(otherTR.tag)) {
        return (false);
      }
      
      //
      // Tag should uniquely identify the object!
      //
      
      if (!this.rect.equals(otherTR.rect)) {
        throw new IllegalStateException();
      }
      return (true);
    }  
  } 
  
  /***************************************************************************
  **
  ** Want to fix ordering.  Order should not affect results, but it
  ** makes it easier to track what is going on:
  */
  
  public static class RectOrdering implements Comparator<RectDefinedByRects.TaggedRect> {
    
    public int compare(RectDefinedByRects.TaggedRect first, RectDefinedByRects.TaggedRect second) {
      int xDiff = (int)(first.rect.getX() - second.rect.getX());
      int yDiff = (int)(first.rect.getY() - second.rect.getY());
      int xMaxDiff = (int)(first.rect.getMaxX() - second.rect.getMaxX());
      int yMaxDiff = (int)(first.rect.getMaxY() - second.rect.getMaxY());      
      
      if (xDiff != 0) {
        return (xDiff);
      }
      if (yDiff != 0) {
        return (yDiff);
      }
      if (xMaxDiff != 0) {
        return (xMaxDiff);
      }
      return (yMaxDiff);
    }    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // TEST METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    try {
      Rectangle2D testRect = new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0);
      Rectangle2D baseRect0 = new Rectangle2D.Double(-10.0, -10.0, 5.0, 5.0);
      Rectangle2D baseRect1 = new Rectangle2D.Double(-10.0, -10.0, 20.0, 20.0);
      Rectangle2D baseRect2 = new Rectangle2D.Double(-10.0, 90.0, 20.0, 20.0);
      TaggedRect tr0 = new TaggedRect(baseRect0, "0");
      TaggedRect tr1 = new TaggedRect(baseRect1, "1");
      TaggedRect tr2 = new TaggedRect(baseRect2, "2");
      HashSet<TaggedRect> baseSet = new HashSet<TaggedRect>();
      baseSet.add(tr0);
      baseSet.add(tr1);
      baseSet.add(tr2);
      RectDefinedByRects rdr = new RectDefinedByRects(testRect, baseSet);
      Rectangle2D transRect0 = new Rectangle2D.Double(-10.0, -10.0, 5.0, 5.0);
      Rectangle2D transRect1 = new Rectangle2D.Double(-10.0, -50.0, 20.0, 40.0);
      Rectangle2D transRect2 = new Rectangle2D.Double(-10.0, 200.0, 30.0, 10.0);
      HashMap<TaggedRect, Rectangle2D> o2n = new HashMap<TaggedRect, Rectangle2D>();
      o2n.put(tr0, transRect0);
      o2n.put(tr1, transRect1);
      o2n.put(tr2, transRect2);
      Rectangle2D transRdr = rdr.generateNewRect(o2n);
      System.out.println(transRdr);
    } catch (Exception ioex) {
      System.err.println(ioex);
      ioex.printStackTrace();
    }
    return;
  }  
}  

