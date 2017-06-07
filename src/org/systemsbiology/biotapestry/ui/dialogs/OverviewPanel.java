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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/***************************************************************************
** 
** Actually handles the drawing
*/

public class OverviewPanel extends JPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private OverviewPresentation myGenomePre_;
  private StaticDataAccessContext rcx_;
  private UIComponentSource uics_;
  private AffineTransform transform_;
  private Point2D clickPoint_;
  private Rectangle2D floater_;
  private Rectangle2D directRect_;
  private ActionListener listener_;
  
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
  
  public OverviewPanel(UIComponentSource uics, Rectangle rect, boolean allowDirect, ActionListener al) {
    uics_ = uics;
    myGenomePre_ = new OverviewPresentation();
    addMouseListener(new MouseHandler());
    addMouseMotionListener(new MouseMotionHandler());
    setBackground(Color.white);
    transform_ = new AffineTransform();
    floater_ = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
    directRect_ = (allowDirect) ? (Rectangle2D)floater_.clone() : null;    
    listener_ = al;
    clickPoint_ = null;
    rcx_ = null;
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
    
      if (rcx_ != null) {
        FontRenderContext frc = new FontRenderContext(transform_, true, true);
        rcx_.setFrc(frc);
      }
      Rectangle rect = myGenomePre_.getRequiredSize(rcx_);
      Rectangle2D finalRect = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
      if (directRect_ != null) {
        finalRect = directRect_.createUnion(finalRect);
        // Can union give negative width, height?  (Intersection does)
        double frW = finalRect.getWidth();
        double frH = finalRect.getHeight();
        double frX = finalRect.getX();
        double frY = finalRect.getY();
        if (frW < 0.0) {
          finalRect.setRect(frX + frW, frY, -frW, frH);
        }
        if (frH < 0.0) {
          finalRect.setRect(frX, frY + frH, frW, -frH);
        }        
      }
      
      Dimension currentSize = this.getSize();

      double wZoom = currentSize.width / finalRect.getWidth();
      double hZoom = currentSize.height / finalRect.getHeight();    
      double zoom = ((wZoom > hZoom) ? hZoom : wZoom) / 3.0;  // give lots of room to grow
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
  ** Set the genome
  */
  
  public void setRenderingContext(StaticDataAccessContext rcx) {
    rcx_ = rcx;
    return;
  }
  
  /***************************************************************************
  **
  ** This is what needs to be overridden to handle window resize operations
  */
  
  public void setBounds(int x, int y, int width, int height) {
    myGenomePre_.setSize(width, height);
    super.setBounds(x, y, width, height);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the layout key
 
  
  public String getLayoutKey() {
    return (layout_);
  }     

  /***************************************************************************
  **
  ** Get the click point
  */
  
  public Point2D getChosenPoint() {
    return (clickPoint_);
  }
  
  /***************************************************************************
  **
  ** Set the click point
  */
  
  public void setChosenPoint(Point2D newPt) {
    clickPoint_ = (Point2D)newPt.clone();
    processNewPosition();    
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
    if (rcx_ != null) {
      Graphics2D g2 = (Graphics2D)g;   
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      AffineTransform saveTrans = g2.getTransform();
      g2.transform(useTrans);
      myGenomePre_.presentOverview(g2, rcx_);
      g2.setTransform(saveTrans);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle new position input
  */
  
  private void processNewPosition() {
    double width = floater_.getWidth();
    double height = floater_.getHeight();      
    floater_.setRect(clickPoint_.getX() - (width / 2.0), 
                     clickPoint_.getY() - (height / 2.0), width, height);
    myGenomePre_.setFloater(floater_);
    repaint();
    listener_.actionPerformed(new ActionEvent(this, 0, null));
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
    
    public void mousePressed(MouseEvent me) {
      try {
        lastPress_ = new Point(me.getX(), me.getY());
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    public void mouseClicked(MouseEvent me) {
      return;
    }    

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
          clickResult(lastX, lastY);
        } else if ((diffX >= DRAG_SLOP_) || (diffY >= DRAG_SLOP_)) {
          lastPress_ = null;
          dragResult(lastX, lastY);
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    private void clickResult(int x, int y) {
      clickPoint_ = new Point2D.Double(x, y);
      try {
        transform_.inverseTransform(clickPoint_, clickPoint_);
      } catch (NoninvertibleTransformException nitex) {
        throw new IllegalStateException();
      }
      processNewPosition();
    }
    
    private void dragResult(int sx, int sy) {
    }
  }
    
  /***************************************************************************
  **
  ** Handles mouse motion events
  */  
      
  public class MouseMotionHandler extends MouseMotionAdapter {
    public void mouseDragged(MouseEvent me) {
    }
    public void mouseMoved(MouseEvent me) {
      try {
        if (clickPoint_ == null) {      
          Point2D movePoint = new Point2D.Double(me.getX(), me.getY());      
          try {
            transform_.inverseTransform(movePoint, movePoint);
          } catch (NoninvertibleTransformException nitex) {
            throw new IllegalStateException();
          }
          double width = floater_.getWidth();
          double height = floater_.getHeight();      
          floater_.setRect(movePoint.getX() - (width / 2.0), 
                           movePoint.getY() - (height / 2.0), width, height);
          myGenomePre_.setFloater(floater_);
          OverviewPanel.this.repaint();
        } 
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }        
  }
}
