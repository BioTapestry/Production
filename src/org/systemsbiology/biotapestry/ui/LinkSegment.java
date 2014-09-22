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

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** A segment of a link tree
*/

public class LinkSegment implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static int NO_SHARED                   = 0;
  public final static int SHARED_STARTS               = 1;
  public final static int THIS_START_OTHER_END_SHARED = 2;
  public final static int THIS_END_OTHER_START_SHARED = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String parent_;  
  private Point2D start_;  
  private Point2D end_;
  private Vector2D normal_;  // If affine transforms are not euclidean, this
                             // really needs to be the dual vector.
  private Vector2D run_;
  private double length_;
  
  private SuggestedDrawStyle drawStyle_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor that converts a segment into a degenerate one.
  */

  public LinkSegment(LinkSegment current, boolean tossFirst) {      
    this.id_ = current.id_;
    this.parent_ = current.parent_;
    if (tossFirst) {
      this.start_ = (Point2D)current.end_.clone();
    } else {
      this.start_ = (Point2D)current.start_.clone();
    }      
    this.end_ = null;    
    if (current.drawStyle_ != null) {
      this.drawStyle_ = current.drawStyle_.clone();
    }   
    recalcVecs();
  }   

  /***************************************************************************
  **
  ** Constructor that moves a degenerate segment
  */

  public LinkSegment(LinkSegment current, Point2D newStart) {      
    this.id_ = current.id_;
    this.parent_ = current.parent_;
    this.start_ = (Point2D)newStart.clone();
    this.end_ = null;    
    if (current.drawStyle_ != null) {
      this.drawStyle_ = current.drawStyle_.clone();
    }      
    recalcVecs();
  }  

  /***************************************************************************
  **
  ** Constructor that merges two links to one, eliminating connection point.
  */

  public LinkSegment(LinkSegment first, LinkSegment second) {      
    this.id_ = first.id_;
    this.parent_ = first.parent_;
    this.start_ = (Point2D)first.start_.clone();
    this.end_ = (second.end_ == null) ? null : (Point2D)second.end_.clone();    
    
    if (first.drawStyle_ != null) {
      this.drawStyle_ = first.drawStyle_.clone();
    } else if (second.drawStyle_ != null) {
      this.drawStyle_ = second.drawStyle_.clone();
    } else {
      this.drawStyle_ = null;
    }
    recalcVecs();
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public LinkSegment(LinkSegment other) {
    this.id_ = other.id_;
    this.parent_ = other.parent_;
    this.start_ = (Point2D)other.start_.clone();
    this.end_ = (other.end_ == null) ? null : (Point2D)other.end_.clone();    
    if (other.drawStyle_ != null) {
      this.drawStyle_ = other.drawStyle_.clone();
    }      
    recalcVecs();
  }
  
  /***************************************************************************
  **
  ** UI-based Constructor
  */

  public LinkSegment(String id, String parent, Point2D start, Point2D end) {
      
    id_ = id;
    parent_ = parent;
    start_ = (Point2D)start.clone();
    end_ = (end == null) ? null : (Point2D)end.clone();
    drawStyle_ = null;
  //  if ((end_ != null) && (end_.equals(start_))) {
  //    throw new IllegalStateException();
  //  }
    recalcVecs();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public LinkSegment(String id, String parent, 
                     Point2D start, Point2D end, String specialLine) throws IOException {
      
    id_ = id;
    parent_ = parent;
    start_ = (Point2D)start.clone();
    end_ = (end == null) ? null : (Point2D)end.clone();

    //
    // Legacy only; this is now loaded in using a subsequent element...
    //
 
    try {
      if (specialLine != null) {      
        int style = LinkProperties.mapFromStyleTag(specialLine);
        if (style != LinkProperties.NONE) {
          drawStyle_ = SuggestedDrawStyle.buildFromLegacy(style);
        }   
      }
    } catch (IllegalArgumentException iaex) {
      throw new IOException();
    }  
    
   // if ((end_ != null) && (end_.equals(start_))) {
    //  throw new IllegalStateException();
   // }
    recalcVecs();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public LinkSegment(Point2D start, Point2D end) {      
    id_ = null;
    parent_ = null;
    start_ = (Point2D)start.clone();
    end_ = (end == null) ? null : (Point2D)end.clone();
    drawStyle_ = null;
    recalcVecs();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public LinkSegment(Point2D start, Point2D end, SuggestedDrawStyle drawStyle) {      
    id_ = null;
    parent_ = null;
    start_ = (Point2D)start.clone();
    end_ = (end == null) ? null : (Point2D)end.clone();
    if (drawStyle != null) {
      drawStyle_ = drawStyle.clone();    
    }
    recalcVecs();
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public LinkSegment clone() {
    try {
      LinkSegment retval = (LinkSegment)super.clone();
      retval.start_ = (Point2D)this.start_.clone();
      retval.end_ = (this.end_ == null) ? null : (Point2D)this.end_.clone();
      recalcVecs();  // Gets new vector copies
      if (this.drawStyle_ != null) {
        retval.drawStyle_ = this.drawStyle_.clone();
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  } 
 
  /***************************************************************************
  **
  ** Set the draw style
  */
  
  public void setDrawStyle(SuggestedDrawStyle drawStyle) {
    drawStyle_ = drawStyle;
    return;
  }
  
  /***************************************************************************
  **
  ** Gives the non-orthogonal area of the segment
  */
  
  public double getNonOrthogonalArea() {
    if (end_ == null) {
      return (0.0);
    }
    return (0.5 * Math.abs((end_.getX() - start_.getX()) * (end_.getY() - start_.getY())));
  }  
  
  /***************************************************************************
  **
  ** Gives the sum of endpoint distances
  */
  
  public double getEndpointDistanceSum(LinkSegment other) {
    double distance = this.start_.distance(other.start_);
    if ((this.end_ != null) && (other.end_ != null)) {
      distance += this.end_.distance(other.end_);
    }
    return (distance);
  }  
 
  /***************************************************************************
  **
  ** Answers if the given segment is just the given translation of this segment
  */
  
  public boolean isSimpleTranslation(LinkSegment other, Vector2D translation) {
    boolean iHaveEnd = (this.end_ != null);
    boolean otherHasEnd = (other.end_ != null);    
    if (iHaveEnd != otherHasEnd) {
      return (false);
    }
    Point2D target = translation.add(this.start_);
    if (!target.equals(other.start_)) {
      return (false);
    }
    if (!iHaveEnd) {
      return (true);
    }
    target = translation.add(this.end_);
    return (target.equals(other.end_));
  }  
  
  /***************************************************************************
  **
  ** Answers if the given segment is just some translation of this segment
  */
  
  public boolean isSimpleTranslation(LinkSegment other) {
    boolean iHaveEnd = (this.end_ != null);
    boolean otherHasEnd = (other.end_ != null);    
    if (iHaveEnd != otherHasEnd) {
      return (false);
    }
    if (!iHaveEnd) {
      return (true);
    }    
    return (this.run_.equals(other.run_) && (this.length_ == other.length_));
  }
  
  /***************************************************************************
  **
  ** Answers if the given segment is some translated scaling of this segment
  */
  
  public boolean isSimpleScaling(LinkSegment other) {
    boolean iHaveEnd = (this.end_ != null);
    boolean otherHasEnd = (other.end_ != null);    
    if (iHaveEnd != otherHasEnd) {
      return (false);
    }
    if (!iHaveEnd) {
      return (true);
    }        
    return (this.run_.equals(other.run_));
  }
  
  /***************************************************************************
  **
  ** Returns fraction of point along run
  */
  
  public double fractionOfRun(Point2D pt) {
    if (this.end_ == null) {
      return (0.0);
    }
    Vector2D toPt = new Vector2D(start_, pt);
    double normDot = toPt.dot(run_);
    return ((length_ == 0.0) ? 0.0 : normDot / length_);
  } 
  
  /***************************************************************************
  **
  ** Returns the split point at the fraction along run
  */
  
  public Point2D pointAtFraction(double fraction) {
    if (this.end_ == null) {
      return ((Point2D)this.start_.clone());
    }
    Vector2D fracRun = new Vector2D(run_);
    fracRun.scale(length_);
    fracRun.scale(fraction);
    Point2D retval = fracRun.add(start_);
    UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);
    return (retval);          
  }
  
  /***************************************************************************
  **
  ** Get the length
  */
  
  public double getLength() {
    return (length_);
  }
  
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Set the ID - use with caution!
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the start
  */
  
  public Point2D getStart() {
    return (start_);
  }
  
  /***************************************************************************
  **
  ** Get the end
  */
  
  public Point2D getEnd() {
    return (end_);
  }

  /***************************************************************************
  **
  ** Get the run
  */
  
  public Vector2D getRun() {
    return (run_);
  }
  
  /***************************************************************************
  **
  ** Get the normal
  */
  
  public Vector2D getNormal() {
    return (normal_);
  }  
  
  /***************************************************************************
  **
  ** Get the parent
  */
  
  public String getParent() {
    return (parent_);
  }

  /***************************************************************************
  **
  ** set the parent
  */
  
  public void setParent(String parent) {
    parent_ = parent;
    return;
  }  

  /***************************************************************************
  **
  ** Get the special style.  May be null
  */
  
  public SuggestedDrawStyle getSpecialDrawStyle() {
    return (drawStyle_);
  }   
  
  /***************************************************************************
  **
  ** Degenerate segment (just the point)
  */
  
  public boolean isDegenerate() {
    return (end_ == null);
  }  
  
  /***************************************************************************
  **
  ** Answers if segment is within the rectangle.
  */
  
  public boolean insideBox(Rectangle2D rect) {
    return (insideShape(rect));
  }
  
  /***************************************************************************
  **
  ** Answers if segment is within the arbitrary area 
  ** (Just checks endpoints.  For non-convex shape, this means the segment
  ** may leave and come back again.
  */
  
  public boolean insideShape(Shape shape) {
    if (!shape.contains(start_)) {
      return (false);
    }
    if (end_ != null) {
      return (shape.contains(end_));
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Answers if segment is orthogonal
  */
  
  public boolean isOrthogonal() {
    if (end_ == null) {
      return (true);
    }
    return ((end_.getX() == start_.getX()) || (end_.getY() == start_.getY()));
  }   
  
  /***************************************************************************
  **
  ** Answers if the point intersects the start point
  */
  
  public Double intersectsStart(Point2D pt, double tolerance) {
    return (intersectsEndpoint(start_, pt, tolerance));
  }

  /***************************************************************************
  **
  ** Answers if the point intersects the end point
  */
  
  public Double intersectsEnd(Point2D pt, double tolerance) {
    if (end_ == null) {
      return (null);
    }
    return (intersectsEndpoint(end_, pt, tolerance));
  }  
  
  /***************************************************************************
  **
  ** Answers if the point intersects this line segment
  */
  
  public Double intersects(Point2D pt, double tolerance) {
    if (end_ == null) {
      throw new IllegalStateException();
    }
    if (end_.equals(start_)) {
      return (intersectsStart(pt, tolerance));
    }
    //
    // Calculate the vector, take the dot product with the normal, and
    // look for a zero.  If it matches, see if the dot product with the
    // run vector is >= zero and <= length;
    //
    Vector2D toPt = new Vector2D(start_, pt);
    double dot = normal_.dot(toPt);
    // Gotta fix to abs of tolerance, or 0.0 == tolerance will never match!
    if (Math.abs(dot) > Math.abs(tolerance - Vector2D.TOLERANCE)) {
      return (null);
    }
    double runDot = run_.dot(toPt);
    if ((runDot < 0) || (runDot > (length_ + Vector2D.TOLERANCE))) {
      return (null);
    }
    return (new Double(dot));
  }
  
  /***************************************************************************
  **
  ** Answers if the point intersects this line segment someplace other than endpoint
  */
  
  public boolean intersectsNonEndpoint(Point2D pt, double tolerance) {
    if (end_ == null) {
      throw new IllegalStateException();
    }
    //
    // Calculate the vector, take the dot product with the normal, and
    // look for a zero.  If it matches, see if the dot product with the
    // run vector is >= zero and <= length;
    //
   
    Vector2D toPt = new Vector2D(start_, pt);
    double dot = normal_.dot(toPt); 
    // Gotta fix to abs of tolerance, or 0.0 == tolerance will never match!
    double abtolm = Math.abs(tolerance - Vector2D.TOLERANCE);
    if (Math.abs(dot) > abtolm) {
      return (false);
    }
    // Endpoint intersections do not count!
    double runDot = run_.dot(toPt);
    double abtolp = Math.abs(tolerance + Vector2D.TOLERANCE);
    if ((runDot < abtolm) || (runDot > (length_ - abtolp))) {
      return (false);
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Returns intersection point, null if no intersection.
  */
  
  private Point2D generalSegmentOverlap(LinkSegment other, boolean allowEndpoints, boolean ignoreNonCanonical) {
 
    double x1 = this.start_.getX();
    double y1 = this.start_.getY();
    double x2 = this.end_.getX();
    double y2 = this.end_.getY();
    
    double x3 = other.start_.getX();
    double y3 = other.start_.getY();
    double x4 = other.end_.getX();
    double y4 = other.end_.getY();
    
    //
    // Ignore non-ortho if directed:
    //
    
    if (ignoreNonCanonical) {
      if (!this.run_.isCanonical() || !other.run_.isCanonical()) {
        return (null);
      }
    }
  
    // Zero denom for parallel lines:
    double denom = ((y4 - y3) * (x2 - x1)) - ((x4 - x3) * (y2 - y1));
    if (Math.abs(denom) <= 1.0E-6) {
      return (null);
    }
    
    double uaNum = ((x4 - x3) * (y1 - y3)) - ((y4 - y3) * (x1 - x3));
    double ubNum = ((x2 - x1) * (y1 - y3)) - ((y2 - y1) * (x1 - x3));
    
    double ua = uaNum / denom;
    double ub = ubNum / denom;
 
    //
    // parametric values outside 0 <= u <= 1 means outside endpoints:
    //
    
    if ((ua < 0.0) || (ua > 1.0) || (ub < 0.0) || (ub > 1.0)) {
      return (null);
    }
    
    // If endpoints not allowed:
    if (!allowEndpoints) {
      if (Math.abs(ua) <= 1.0E-6) {
        return (null);
      }
      if (Math.abs(ub) <= 1.0E-6) {
        return (null);
      }
      if (Math.abs(1.0 - ua) <= 1.0E-6) {
        return (null);
      }
      if (Math.abs(1.0 - ub) <= 1.0E-6) {
        return (null);
      }
    }   
    double px = x1 + (ua * (x2 - x1));
    double py = y1 + (ua * (y2 - y1));
    
    Point2D retval = new Point2D.Double(px, py);
    UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);    
    return (retval);   
  }  
  
  /***************************************************************************
  **
  ** Check if endpoints overlap unless we are forgiven
  */
  
  private void nonSharedEndpointsOverlap(LinkSegment other, int okShared, OverlapResult result) {
    
    boolean startsMatch = (intersectsStart(other.start_, 0.0) != null);
    boolean endsMatch = (intersectsEnd(other.end_, 0.0) != null);
    boolean myStartOtherEndMatch = (intersectsStart(other.end_, 0.0) != null);
    boolean myEndOtherStartMatch = (intersectsEnd(other.start_, 0.0) != null);
    
    switch (okShared) {
      case NO_SHARED:
        if (startsMatch || myEndOtherStartMatch) {
          result.myOverlapByStart = (Point2D)other.start_.clone();
        }
        if (endsMatch || myStartOtherEndMatch) {
          result.myOverlapByEnd = (Point2D)other.end_.clone();
        }       
        return;
      case SHARED_STARTS:
        if (myEndOtherStartMatch) {
          result.myOverlapByStart = (Point2D)other.start_.clone();
        }
        if (endsMatch || myStartOtherEndMatch) {
          result.myOverlapByEnd = (Point2D)other.end_.clone();
        }       
        return;
      case THIS_START_OTHER_END_SHARED:
        if (startsMatch || myEndOtherStartMatch) {
          result.myOverlapByStart = (Point2D)other.start_.clone();
        }
        if (endsMatch) {
          result.myOverlapByEnd = (Point2D)other.end_.clone();
        }       
        return;
      case THIS_END_OTHER_START_SHARED:
        if (startsMatch) {
          result.myOverlapByStart = (Point2D)other.start_.clone();
        }
        if (endsMatch || myStartOtherEndMatch) {
          result.myOverlapByEnd = (Point2D)other.end_.clone();
        }       
        return;
      default:
        throw new IllegalArgumentException();
    }
  }
   
  /***************************************************************************
  **
  ** Check if must do degenenerate overlap
  */
  
  private boolean checkDegenerateOverlap(LinkSegment other) {
    return ((this.end_ == null) || (other.end_ == null));      
  }
  
  /***************************************************************************
  **
  ** Check if overlap cases are true with degenenerate segments
  */
  
  private OverlapResult degenerateOverlap(LinkSegment other, int okShared) { 
    OverlapResult retval = new OverlapResult();
    if (this.end_ == null) {
      if (other.end_ == null) { // both are degenerate.  Match if points match, unless exception allows        
        switch (okShared) {
          case NO_SHARED:            
            if (intersectsStart(other.start_, 0.0) != null) {
              retval.myOverlapByStart = (Point2D)other.start_.clone();
            }
            return (retval);
          case SHARED_STARTS:
            return (retval); // i.e. false...
          case THIS_START_OTHER_END_SHARED:
          case THIS_END_OTHER_START_SHARED:
          default:
            throw new IllegalArgumentException();
        }
      } else { // I am degenerate, other guy is not:
        switch (okShared) {
          case NO_SHARED:
            if (other.intersects(this.start_, 0.0) != null) {
              retval.otherOverlapByStart = (Point2D)this.start_.clone();      
            }
            return (retval);
          case SHARED_STARTS:
            if (other.intersectsNonEndpoint(this.start_, 0.0) ||
                (other.intersectsEnd(this.start_, 0.0) != null)) {
              retval.otherOverlapByStart = (Point2D)this.start_.clone();            
            }
            return (retval);
          case THIS_START_OTHER_END_SHARED:
            if (other.intersectsNonEndpoint(this.start_, 0.0) ||
                (other.intersectsStart(this.start_, 0.0) != null)) {
              retval.otherOverlapByStart = (Point2D)this.start_.clone();     
            }
            return (retval);
          case THIS_END_OTHER_START_SHARED:
          default:
            throw new IllegalArgumentException();
        }
      }     
    } else if (other.end_ == null) { // Other guy is degenerate, I am not:
      switch (okShared) {
        case NO_SHARED:
          if (this.intersects(other.start_, 0.0) != null) {
            retval.myOverlapByStart = (Point2D)other.start_.clone();         
          }
          return (retval);
        case SHARED_STARTS:
          if (this.intersectsNonEndpoint(other.start_, 0.0) ||
              (this.intersectsEnd(other.start_, 0.0) != null)) {
            retval.myOverlapByStart = (Point2D)other.start_.clone();
          }
          return (retval);
        case THIS_END_OTHER_START_SHARED:
          if (this.intersectsNonEndpoint(other.start_, 0.0) ||
              (this.intersectsStart(other.start_, 0.0) != null)) {
            retval.myOverlapByStart = (Point2D)other.start_.clone();
          }
          return (retval);
        case THIS_START_OTHER_END_SHARED:
        default:
          throw new IllegalArgumentException();
      }
    }
    throw new IllegalStateException();  // not degenerate, do not call!
  }
 
  /***************************************************************************
  **
  ** Answers if this link segment overlaps the other
  */
  
  public OverlapResult overlaps(LinkSegment other, int okShared) {
    
    if (checkDegenerateOverlap(other)) {
      OverlapResult retval = degenerateOverlap(other, okShared);
      return ((retval.haveEndpointOverlap()) ? retval : null);
    }
    
    //
    // We have two runs!
    //
    
    OverlapResult retval = new OverlapResult();
    
    if (this.intersectsNonEndpoint(other.start_, 0.0)) { 
      retval.myOverlapByStart = (Point2D)other.start_.clone();
    }    
    if (this.intersectsNonEndpoint(other.end_, 0.0)) {
      retval.myOverlapByEnd = (Point2D)other.end_.clone();
    }
        
    if (other.intersectsNonEndpoint(this.start_, 0.0)) {
      retval.otherOverlapByStart = (Point2D)this.start_.clone();
    }   
                  
    if (other.intersectsNonEndpoint(this.end_, 0.0)) {
      retval.otherOverlapByEnd = (Point2D)this.end_.clone();     
    }
 
    nonSharedEndpointsOverlap(other, okShared, retval);
      
    if (!retval.haveEndpointOverlap()) {
      Point2D ovp = generalSegmentOverlap(other, false, true);
      if (ovp != null) {
        retval.nonEndOverlap = ovp;
      }
    }
       
    return ((retval.haveAnyOverlap()) ? retval : null);
  }
 
  /***************************************************************************
  **
  ** Chop this segment into one or two segments at the given point.
  ** If the point matches an endpoint, we just return ourselves.
  */
  
  public LinkSegment[] split(Point2D pt) {
    if (end_ == null) {
      throw new IllegalStateException();
    } 
    Vector2D deltaS = new Vector2D(start_, pt);
    Vector2D deltaE = new Vector2D(end_, pt);    
    if (deltaS.isZero() || deltaE.isZero()) {
      LinkSegment[] retval = new LinkSegment[1];
      retval[0] = this;
      return (retval);
    } else {
      LinkSegment[] retval = new LinkSegment[2];
      retval[0] = new LinkSegment(start_, pt, drawStyle_);
      Point2D copy = (Point2D)pt.clone();
      retval[1] = new LinkSegment(copy, end_, drawStyle_);
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Change the start location
  */
  
  public void setStart(Point2D newStrt) {
    start_ = newStrt;
   // if ((end_ != null) && (end_.equals(start_))) {
    //  throw new IllegalStateException();
   // }
    recalcVecs();
    return;
  }
  
  /***************************************************************************
  **
  ** Change the end location
  */
  
  public void setEnd(Point2D newEnd) {
    end_ = newEnd;
  //  if ((end_ != null) && (end_.equals(start_))) {
  //    throw new IllegalStateException();
  //  }
    recalcVecs();    
    return;
  }
  
  /***************************************************************************
  **
  ** Change the start and end locations
  */
  
  public void setBoth(Point2D newStrt, Point2D newEnd) {
    start_ = newStrt;
    end_ = newEnd;
  //  if ((end_ != null) && (end_.equals(start_))) {
  //    throw new IllegalStateException();
  //  }
    recalcVecs();    
    return;
  }  

  /***************************************************************************
  **
  ** Shift the start location
  */
  
  public void shiftStart(double dx, double dy) {
    start_.setLocation(start_.getX() + dx, start_.getY() + dy);
 //   if ((end_ != null) && (end_.equals(start_))) {
   //   throw new IllegalStateException();
  //  }
    recalcVecs();
    return;
  }
  
  /***************************************************************************
  **
  ** Change the end location
  */
  
  public void shiftEnd(double dx, double dy) {
    end_.setLocation(end_.getX() + dx, end_.getY() + dy);
 //   if ((end_ != null) && (end_.equals(start_))) {
 //     throw new IllegalStateException();
 //   }
    recalcVecs();
    return;
  }  
  
  /***************************************************************************
  **
  ** Change both locations
  */
  
  public void shiftBoth(double dx, double dy) {
    start_.setLocation(start_.getX() + dx, start_.getY() + dy);
    if (end_ != null) {
      end_.setLocation(end_.getX() + dx, end_.getY() + dy);
    }
  //  if ((end_ != null) && (end_.equals(start_))) {  // Shouldn't happen
  //    throw new IllegalStateException();
  //  }
    recalcVecs();
    return;
  }
  
  /***************************************************************************
  **
  ** Get the closest point on the segment to the given point.   Null if it is beyond
  ** endpoints.
  */

  public Point2D getClosestPoint(Point2D pt) {
    if ((end_ == null) || run_.isZero()) {
      return ((Point2D)start_.clone());
    }    
    Vector2D toPt = new Vector2D(start_, pt);
    double runDot = run_.dot(toPt);
    if ((runDot < 0.0) || (runDot > length_)) {
      return (null);
    }
    Vector2D offset = run_.scaled(runDot);
    return (offset.add(start_));
  }
  
  /***************************************************************************
  **
  ** Get the distance from the given point to the segment.  If the point
  ** is beyond the endpoints, it returns the distance from the nearest endpoint.
  */

  public double getDistance(Point2D pt) {
    Point2D closest = getClosestPoint(pt);
    if (closest == null) {
      double toStart = (new Vector2D(start_, pt)).length();
      if (end_ == null) {
        return (toStart);
      }
      double toEnd = (new Vector2D(end_, pt)).length();
      return ((toEnd < toStart) ? toEnd : toStart);
    }
    return ((new Vector2D(closest, pt)).length());
  }  

  /***************************************************************************
  **
  ** To string
  */
  
  public String toString() {
    String dss = (drawStyle_ != null) ? drawStyle_.toString() : "(no Special Style)";
    return ("LinkSegment: start = " + start_ + " end = " + end_ + " normal = " +
            normal_ + " run = " + run_ + " length = " + length_ +
            " id = " + id_ + " parent = " + parent_ + " specSty = " + dss);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<linkSegment ");
    if (id_ != null) {
      out.print("id=\"");
      out.print(id_);    
      out.print("\" ");
    }
    if (parent_ != null) {
      out.print("parent=\"");
      out.print(parent_);    
      out.print("\" ");
    }
    out.print("strtX=\"");      
    out.print(start_.getX());
    out.print("\" strtY=\"");
    out.print(start_.getY());
    if (end_ != null) {
      out.print("\" endX=\"");
      out.print(end_.getX());
      out.print("\" endY=\"");
      out.print(end_.getY()); 
    }
    
    if (drawStyle_ != null) {
      out.println("\" >"); 
      ind.up();
      drawStyle_.writeXML(out, ind);
      ind.down().indent();
      out.println("</linkSegment>");
    } else {
      out.println("\" />");
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  
  /***************************************************************************
  **
  ** Useful to make this available without having a segment in hand
  **
  */
  
  public static Double intersectsEndpoint(Point2D endpoint, Point2D point, double tolerance) {
    double distance = endpoint.distance(point);
    return ((distance <= tolerance) ? new Double(distance) : null);
  }  
 
  /***************************************************************************
  **
  ** For returning overlap results
  */  
      
  public static class OverlapResult {   
    public Point2D myOverlapByStart;
    public Point2D myOverlapByEnd;
    public Point2D otherOverlapByStart;
    public Point2D otherOverlapByEnd;
    public Point2D nonEndOverlap;
    
    public boolean haveEndpointOverlap() {
      return ((myOverlapByStart != null) || (myOverlapByEnd != null) ||
              (otherOverlapByStart != null) || (otherOverlapByEnd != null));
    }  
    
    public boolean haveAnyOverlap() {
      return (haveEndpointOverlap() || (nonEndOverlap != null));
    }
    
    @Override
    public String toString() {
      return ("myOver St: " + myOverlapByStart + " myOver End:  " + myOverlapByEnd + " otherOver St:  " + otherOverlapByStart + " otherOver End:  " + otherOverlapByEnd  + " nonEndOverlap:  " + nonEndOverlap);
      
    }   
  }
  
  
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
//  public static String keywordOfInterest() {
//    return ("linkSegment");
 // }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class LinkSegmentWorker extends AbstractFactoryClient {
   
    public LinkSegmentWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("linkSegment");
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("linkSegment")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.linkSegment = buildFromXML(elemName, attrs);
        retval = board.linkSegment;
      }
      return (retval);     
    }
    
    private LinkSegment buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String strtX = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "strtX", true);
      String strtY = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "strtY", true);
      String endX = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "endX", false);
      String endY = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "endY", false);
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "id", false);
      String parent = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "parent", false);
      String specialLine = AttributeExtractor.extractAttribute(elemName, attrs, "linkSegment", "specialLine", false);
   
      if (((endX == null) && (endY != null)) || 
          ((endX != null) && (endY == null))) {
        throw new IOException();
      }          

      float sx, sy;
      float ex = 0.0F;
      float ey = 0.0F;
      try {
        sx = Float.parseFloat(strtX);
        sy = Float.parseFloat(strtY);
        if (endX != null) {
          ex = Float.parseFloat(endX);
          ey = Float.parseFloat(endY);
        }
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }

      Point2D start = new Point2D.Float(sx, sy);
      Point2D end = (endX != null) ? new Point2D.Float(ex, ey) : null;
      if ((id == null) && (parent == null) && (specialLine == null)) {
        return (new LinkSegment(start, end));
      } else {
        return (new LinkSegment(id, parent, start, end, specialLine));    
      }
    }
  }
  
   public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      LinkSegment segment = board.linkSegment;
      SuggestedDrawStyle suggSty = board.suggSty;
      segment.setDrawStyle(suggSty);
      return (null);
    }
  }     
  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Calculate vectors
  **
  */  
  
  private void recalcVecs() {
    if (end_ != null) {
      run_ = new Vector2D(start_, end_);    
      length_ = run_.length();   // may be zero
      run_ = run_.normalized();  // may be a zero vector
      normal_ = run_.normal();   // null if run is a zero vector
    } else {
      run_ = null;
      length_ = 0; 
      normal_ = null; 
    }
  }
}
