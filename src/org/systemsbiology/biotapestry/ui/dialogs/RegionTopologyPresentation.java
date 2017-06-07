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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.util.List;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.BasicStroke;
import java.awt.Font;

import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
 
/****************************************************************************
**
** This draws an overview of region topology
*/

public class RegionTopologyPresentation {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final double REGION_RADIUS_    = 100.0;
  private static final double REGION_RADIUS_SQ_ = REGION_RADIUS_ * REGION_RADIUS_;
  private static final double INTERSECT_TOL_ = 10.0;   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Rectangle cachedBounds_;
  private TimeCourseData.RegionTopology topo_;
  private int width_;
  private int height_;
  private Point2D floatStart_;
  private Point2D floatEnd_;
  private String regionFloaterID_;
  private Point2D regionFloater_;
  private TimeCourseData.TopoRegionLocator topoLocs_;
  private TimeCourseData.TopoLink intersectedLink_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */

  public RegionTopologyPresentation(DataAccessContext dacx, TimeCourseData.RegionTopology topo, 
                                    TimeCourseData.TopoRegionLocator topoLocs) {
    dacx_ = dacx;
    topo_ = topo;
    topoLocs_ = topoLocs;
    cachedBounds_ = null;
    floatStart_ = null;
    floatEnd_ = null;
    regionFloaterID_ = null;
    regionFloater_ = null;
    intersectedLink_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Present the topology info
  */
  
  public void presentTopology(Graphics2D g2) {
    renderLinks(g2);
    renderFloater(g2);
    renderRegions(g2);
    return;
  }
    
  /***************************************************************************
  **
  ** Set the intersected link
  */
  
  public void setIntersectedLink(TimeCourseData.TopoLink intersectedLink) {
    intersectedLink_ = intersectedLink;
    return;
  }
  
  /***************************************************************************
  **
  ** clear the intersected link
  */
  
  public void clearIntersectedLink() {
    intersectedLink_ = null;
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the floater to display
  */
  
  public void setFloater(Point2D startPt, Point2D endPt) {
    floatStart_ = startPt;
    floatEnd_ = endPt;
    return;
  }  
  
  /***************************************************************************
  **
  ** Clear the floater to display
  */
  
  public void clearFloater() {
    floatStart_ = null;
    floatEnd_ = null;
    return;
  }    
  
  /***************************************************************************
  **
  ** Set the floater to display
  */
  
  public void setRegionFloater(String regionID, Point2D location) {
    regionFloaterID_ = regionID;
    regionFloater_ = location;
    return;
  }  
  
  /***************************************************************************
  **
  ** Clear the floater to display
  */
  
  public void clearRegionFloater() {
    regionFloaterID_ = null;
    regionFloater_ = null;  
    floatStart_ = null;
    floatEnd_ = null;
    return;
  }    

  /***************************************************************************
  **
  ** Normalize the click point
  */
  
  public Point2D normalizeClickPoint(String regID, Point2D clickPoint) {
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    Point2D loc = topoLocs_.getRegionTopologyLocation(tRange, regID);
    return ((Point2D)loc.clone());
  }      
  
  /***************************************************************************
  **
  ** Answer if we intersect
  */
  
  public String intersectsRegion(Point2D point) {
    return (selectionGuts(point.getX(), point.getY()));
  }
  
  /***************************************************************************
  **
  ** Get delta to center
  */
  
  public Vector2D getCenterOffset(String regionID, Point2D point) {
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    Point2D loc = topoLocs_.getRegionTopologyLocation(tRange, regionID);
    return (new Vector2D(point, loc));
  }  
 
  /***************************************************************************
  **
  ** Answer if we intersect link
  */
  
  public TimeCourseData.TopoLink intersectsLink(Point2D point) {
    return (linkSelectionGuts(point, INTERSECT_TOL_));
  }  

  /***************************************************************************
  **
  ** Return the required size of the layout
  */
  
  public Rectangle getRequiredSize(AffineTransform trans) {
                                                                    
    if (cachedBounds_ != null) {
      return (cachedBounds_);
    }
  
    Rectangle retval = null;
    
    List<String> regions = topo_.regions;
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    int numReg = regions.size();
    for (int i = 0; i < numReg; i++) {
      String regionID = regions.get(i);
      Point2D loc = topoLocs_.getRegionTopologyLocation(tRange, regionID);
      int x = (int)loc.getX();
      int y = (int)loc.getY();
      int rad = (int)REGION_RADIUS_;
      Rectangle bounds = new Rectangle(x - rad, y - rad, 2 * rad, 2 * rad);
      if (retval == null) {
        retval = bounds;
      } else {
        Bounds.tweakBounds(retval, bounds);
      }
    }

    if (retval == null) {
      retval = new Rectangle(0, 0, 0, 0);
    }
    
    //
    // Pad the outside
    
    Bounds.padBounds(retval, 10, 10);
    cachedBounds_ = retval;
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Set the size
  */
  
  public void setSize(int width, int height) {
    if ((width_ != width) || (height_ != height)) {
      cachedBounds_ = null;
    }
    width_ = width;
    height_ = height;    
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the zoom
  */
  
  public void setTransform(AffineTransform transform) {
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Guts of presentation
  */
  
  private void renderRegions(Graphics2D g2) {
    g2.setStroke(new BasicStroke(5));
    Font mFont = dacx_.getFontManager().getFixedFont(FontManager.TOPO_BUBBLES); 
    FontRenderContext frc = g2.getFontRenderContext();
    
    List<String> regions = topo_.regions;
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    int numReg = regions.size();
    for (int i = 0; i < numReg; i++) {
      String regionID = regions.get(i);
      Point2D loc = (regionID.equals(regionFloaterID_)) ? regionFloater_
                                                        : topoLocs_.getRegionTopologyLocation(tRange, regionID);
      Ellipse2D circ = new Ellipse2D.Double(loc.getX() - REGION_RADIUS_, 
                                            loc.getY() - REGION_RADIUS_,
                                            2.0 * REGION_RADIUS_, 2.0 * REGION_RADIUS_);
      g2.setPaint(Color.white);
      g2.fill(circ);
      g2.setPaint(Color.blue);      
      g2.draw(circ);      
      
      Rectangle2D bounds = mFont.getStringBounds(regionID, frc);
      g2.setFont(mFont);
      g2.drawString(regionID, (float)(loc.getX() - (bounds.getWidth() / 2.0)),
                              (float)(loc.getY() + (bounds.getHeight() / 2.0)));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Guts of presentation
  */
  
  private void renderLinks(Graphics2D g2) {    
    List<TimeCourseData.TopoLink> links = topo_.links;
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      TimeCourseData.TopoLink link = links.get(i);
      Point2D loc1 = (link.region1.equals(regionFloaterID_)) ? regionFloater_
                                                             : topoLocs_.getRegionTopologyLocation(tRange, link.region1);
      Point2D loc2 = (link.region2.equals(regionFloaterID_)) ? regionFloater_
                                                             : topoLocs_.getRegionTopologyLocation(tRange, link.region2);
      GeneralPath linkPath = new GeneralPath();   
      linkPath.moveTo((float)loc1.getX(), (float)loc1.getY());
      linkPath.lineTo((float)loc2.getX(), (float)loc2.getY());
      if (link.equals(intersectedLink_)) {
        g2.setPaint(Color.green);
        g2.setStroke(new BasicStroke(7));
        g2.draw(linkPath);              
      }
      g2.setPaint(Color.red);
      g2.setStroke(new BasicStroke(5));
      g2.draw(linkPath);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Guts of presentation
  */
  
  private void renderFloater(Graphics2D g2) {
    if (floatStart_ == null) {
      return;
    }
    
    g2.setPaint(Color.black); 
    g2.setStroke(new BasicStroke(5));
    GeneralPath linkPath = new GeneralPath();   
    linkPath.moveTo((float)floatStart_.getX(), (float)floatStart_.getY());
    linkPath.lineTo((float)floatEnd_.getX(), (float)floatEnd_.getY());
    g2.draw(linkPath);
    return;
  }  
  
  /***************************************************************************
  **
  ** Guts of selection
  */
  
  private String selectionGuts(double x, double y) {

    List<String> regions = topo_.regions;
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    int numReg = regions.size();
    for (int i = 0; i < numReg; i++) {
      String regionID = regions.get(i);
      Point2D loc = topoLocs_.getRegionTopologyLocation(tRange, regionID);
      double dx = x - loc.getX();
      double dy = y - loc.getY();
      double distsq = (dx * dx) + (dy * dy);
      if (distsq < REGION_RADIUS_SQ_) {
        return (regionID);
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Guts of selection
  */
  
  private TimeCourseData.TopoLink linkSelectionGuts(Point2D point, double tolerance) {  
    List<TimeCourseData.TopoLink> links = topo_.links;
    TimeCourseData.TopoTimeRange tRange = topo_.times;
    int numLinks = links.size();
    for (int i = 0; i < numLinks; i++) {
      TimeCourseData.TopoLink link = links.get(i);
      Point2D loc1 = topoLocs_.getRegionTopologyLocation(tRange, link.region1);
      Point2D loc2 = topoLocs_.getRegionTopologyLocation(tRange, link.region2);
      Vector2D run = new Vector2D(loc1, loc2);    
      double length = run.length();   // may be zero
      run = run.normalized();  // may be a zero vector
      Vector2D normal = run.normal();   // null if run is a zero vector
      Vector2D toPt = new Vector2D(loc1, point);
      double dot = normal.dot(toPt);
      if (Math.abs(dot) > tolerance - Vector2D.TOLERANCE) {
        continue;
      }
      double runDot = run.dot(toPt);
      if ((runDot < 0.0) || (runDot > (length + Vector2D.TOLERANCE))) {
        continue;
      }
      return (link);
    }  
    return (null);
  } 
}
