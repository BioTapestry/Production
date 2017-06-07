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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.Vector2D;

/***************************************************************************
** 
** Panel to draw region topology relationships
*/

public class RegionTopologyPanel extends JPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private RegionTopologyPresentation myTopoPre_;
  private AffineTransform transform_;
  private Point2D clickPointStart_;
  private String dragReg_;
  private Vector2D dragOffset_;  
  private TimeCourseData.RegionTopology topo_;
  private TimeCourseData.TopoRegionLocator topoLocs_;
  private TimeCourseData.TopoLink selectedLink_;
  private RegionTopologyDialog dialog_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public RegionTopologyPanel(UIComponentSource uics, DataAccessContext dacx, RegionTopologyDialog dialog, 
                             TimeCourseData.RegionTopology topo, 
                             TimeCourseData.TopoRegionLocator topoLocs) {
    uics_ = uics;
    dacx_ = dacx;
    myTopoPre_ = new RegionTopologyPresentation(dacx_, topo, topoLocs);
    addMouseListener(new MouseHandler());
    addMouseMotionListener(new MouseMotionHandler());
    setBackground(Color.white);
    transform_ = new AffineTransform();
    clickPointStart_ = null;
    dragReg_ = null;
    topo_ = topo; // already cloned; results are available to dialog
    topoLocs_ = topoLocs; // already cloned; shared by many panels
    dialog_ = dialog;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  /***************************************************************************
  **
  ** Drawing routine
  */
  
  public void paintComponent(Graphics g) {
    try {
      //
      // Chicken and egg: font sizing needs transform to tell us how big it is
      // so we can set the transform.  For now, just provide an identity:
      //
      transform_ = new AffineTransform();
      Rectangle rect = myTopoPre_.getRequiredSize(transform_);
      Rectangle2D finalRect = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
      Dimension currentSize = this.getSize();

      double wZoom = currentSize.width / finalRect.getWidth();
      double hZoom = currentSize.height / finalRect.getHeight();    
      double zoom = ((wZoom > hZoom) ? hZoom : wZoom) / 1.5;  // give room to grow
      transform_.translate(currentSize.width / 2.0, currentSize.height / 2.0);
      transform_.scale(zoom, zoom);
      double centerX = finalRect.getX() + (finalRect.getWidth() / 2.0);
      double centerY = finalRect.getY() + (finalRect.getHeight() / 2.0); 
      transform_.translate(-centerX, -centerY);

      super.paintComponent(g);
      drawingGuts(g, transform_);
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayPaintException(ex);
    }
    return;
  }    

  /***************************************************************************
  **
  ** This is what needs to be overridden to handle window resize operations
  */
  
  public void setBounds(int x, int y, int width, int height) {
    myTopoPre_.setSize(width, height);
    super.setBounds(x, y, width, height);
    return;
  }  

  /***************************************************************************
  **
  ** Cancel current draw
  */ 
  
  public void cancelAddMode() {
    clickPointStart_ = null;
    myTopoPre_.clearFloater();
    repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Delete selected link
  */ 
  
  public void deleteSelected() {
    if (selectedLink_ != null) {
      myTopoPre_.setIntersectedLink(null);
      topo_.removeLink(selectedLink_);
      selectedLink_ = null;
      dialog_.activeSelection(false);
      repaint();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Clear selections
  */ 
  
  public void clearSelections() {
    if (selectedLink_ != null) {
      myTopoPre_.setIntersectedLink(null);
      dialog_.activeSelection(false);
      selectedLink_ = null;
      repaint();
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
  ** Drawing guts
  */
  
  private void drawingGuts(Graphics g, AffineTransform useTrans) {
    Graphics2D g2 = (Graphics2D)g;   
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    AffineTransform saveTrans = g2.getTransform();
    g2.transform(useTrans);    
    myTopoPre_.presentTopology(g2);
    g2.setTransform(saveTrans);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Handles click events
  */  
  
  public class MouseHandler extends MouseAdapter {

    private final static int CLICK_SLOP_  = 2;
    private final static int DRAG_SLOP_   = 2;
    
    private Point lastPress_ = null;

    @Override
    public void mousePressed(MouseEvent me) {
      try {
        lastPress_ = new Point(me.getX(), me.getY());
        Point2D clickPoint = new Point2D.Double();
        try {
          transform_.inverseTransform(lastPress_, clickPoint);
        } catch (NoninvertibleTransformException nitex) {
          throw new IllegalStateException();
        }
        if (clickPointStart_ == null) {
          String regID = myTopoPre_.intersectsRegion(clickPoint);
          if (regID != null) {
            dragReg_ = regID;
            dragOffset_ = myTopoPre_.getCenterOffset(regID, clickPoint);
          }
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
      return;
    }    

    @Override    
    public void mouseReleased(MouseEvent me) {
      try {
        int currX = me.getX();
        int currY = me.getY();
        if (lastPress_ == null) {
          return;
        }
        int lastX = lastPress_.x;
        int lastY = lastPress_.y;
        int diffX = Math.abs(currX - lastX);
        int diffY = Math.abs(currY - lastY);      
        if ((diffX <= CLICK_SLOP_) && (diffY <= CLICK_SLOP_)) {
          lastPress_ = null;
          dragReg_ = null;
          clickResult(lastX, lastY);
        } else if ((diffX >= DRAG_SLOP_) || (diffY >= DRAG_SLOP_)) {
          lastPress_ = null;
          dragResult(currX, currY);
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    private void clickResult(int x, int y) {
      Point2D clickPoint = new Point2D.Double(x, y);      
      try {
        transform_.inverseTransform(clickPoint, clickPoint);
      } catch (NoninvertibleTransformException nitex) {
        throw new IllegalStateException();
      }
      processNewPosition(clickPoint);
      return;
    }
        
    private void processNewPosition(Point2D clickPoint) {
      String regID = myTopoPre_.intersectsRegion(clickPoint);
      if (regID == null) {
        TimeCourseData.TopoLink intLink = myTopoPre_.intersectsLink(clickPoint);
        if (intLink == null) {
          myTopoPre_.setIntersectedLink(null);
          dialog_.activeSelection(false);
          selectedLink_ = null;
        } else {
          myTopoPre_.setIntersectedLink(intLink);
          dialog_.activeSelection(true);
          selectedLink_ = intLink;
        }
        RegionTopologyPanel.this.repaint();
        return;
      }
      
      //
      // Have an intersection; either start a link or finish one
      //
      if (clickPointStart_ == null) {
        clickPointStart_ = myTopoPre_.normalizeClickPoint(regID, clickPoint);
        myTopoPre_.setFloater(clickPointStart_, clickPointStart_);
      } else {
        String startRegID = myTopoPre_.intersectsRegion(clickPointStart_);
        if (startRegID.equals(regID)) {  // No self loops
          return;
        }
        TimeCourseData.TopoLink newLink = new TimeCourseData.TopoLink(startRegID, regID);
        TimeCourseData.TopoLink revLink = new TimeCourseData.TopoLink(regID, startRegID);        
        if (!topo_.hasLink(newLink) && !topo_.hasLink(revLink)) {
          topo_.addLink(newLink);
          clickPointStart_ = null;
          myTopoPre_.clearFloater();
          RegionTopologyPanel.this.repaint();
        }
      }
      return;
    }
    
    private void dragResult(int ptx, int pty) {
      if (ptx < 0) {
        ptx = 0;
      }
      if (pty < 0) {
        pty = 0;
      }
      Dimension dim = RegionTopologyPanel.this.getSize();
      if (ptx > dim.width) {
        ptx = dim.width;
      }
      if (pty > dim.height) {
        pty = dim.height;
      }        
      Point2D clickPoint = new Point2D.Double(ptx, pty);
      try {
        transform_.inverseTransform(clickPoint, clickPoint);
      } catch (NoninvertibleTransformException nitex) {
        throw new IllegalStateException();
      }
      processNewDraggedPosition(clickPoint);
      return;
    }
    
    private void processNewDraggedPosition(Point2D clickPoint) {
      if (dragReg_ == null) {
        return;
      }
      clickPoint = dragOffset_.add(clickPoint);
      topoLocs_.setRegionTopologyLocation(topo_.times, dragReg_, clickPoint);
      dragReg_ = null;
      dragOffset_ = null;
      myTopoPre_.clearRegionFloater();
      RegionTopologyPanel.this.repaint();
      return;
    }    
  }
    
  /***************************************************************************
  **
  ** Handles mouse motion events
  */  
      
  public class MouseMotionHandler extends MouseMotionAdapter {
    public void mouseDragged(MouseEvent me) {
      if (dragReg_ != null) {
        int ptx = me.getX();
        if (ptx < 0) {
          ptx = 0;
        }
        int pty = me.getY();
        if (pty < 0) {
          pty = 0;
        }
        Dimension dim = RegionTopologyPanel.this.getSize();
        if (ptx > dim.width) {
          ptx = dim.width;
        }
        if (pty > dim.height) {
          pty = dim.height;
        }        
        Point2D dragPoint = new Point2D.Double(ptx, pty);      
        try {
          transform_.inverseTransform(dragPoint, dragPoint);
        } catch (NoninvertibleTransformException nitex) {
          throw new IllegalStateException();
        }
        dragPoint = dragOffset_.add(dragPoint);
        myTopoPre_.setRegionFloater(dragReg_, dragPoint);
        RegionTopologyPanel.this.repaint();
      }
      return;
    }
    
    public void mouseMoved(MouseEvent me) {
      try {
        if (clickPointStart_ != null) {      
          Point2D movePoint = new Point2D.Double(me.getX(), me.getY());      
          try {
            transform_.inverseTransform(movePoint, movePoint);
          } catch (NoninvertibleTransformException nitex) {
            throw new IllegalStateException();
          }
          myTopoPre_.setFloater(clickPointStart_, movePoint);
          RegionTopologyPanel.this.repaint();
        } 
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }        
  }
}
