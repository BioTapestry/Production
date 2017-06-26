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

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/***************************************************************************
** 
** Supports operations used in zooming
*/

public class BasicZoomTargetSupport implements ZoomTarget {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  protected ZoomPresentation myGenomePre_;
  protected double zoom_ = .38;
  protected AffineTransform transform_;
  protected JPanel paintTarget_;
  protected DataAccessContext rcx_;
  protected Rectangle2D clipRect_;
  
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
  
  public BasicZoomTargetSupport(ZoomPresentation genomePre, JPanel paintTarget, DataAccessContext rcx) {    
    transform_ = new AffineTransform();
    transform_.scale(zoom_, zoom_);
    myGenomePre_ = genomePre;
    paintTarget_ = paintTarget;
    rcx_ = rcx;
    clipRect_ = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Let us know what the current clip rect is
  */

  public void setCurrClipRect(Rectangle2D clipRect) { 
    clipRect_.setRect(clipRect);
    return;
  }

  /***************************************************************************
  ** 
  ** Bump to next selection
  */

  public void incrementToNextSelection() { 
    myGenomePre_.bumpNextSelection(rcx_);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Bump to previous selection
  */

  public void decrementToPreviousSelection() { 
    myGenomePre_.bumpPreviousSelection(rcx_);
    return;
  } 

  /***************************************************************************
  ** 
  ** repaint
  */
  
  public void repaint() {
    if (paintTarget_ != null) {
      paintTarget_.repaint();
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Set the transform
  */
  
  public void setTransform(AffineTransform transform) {
    transform_ = transform;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the transform
  */
  
  public AffineTransform getTransform() {    
    return (transform_);
  }  

  /***************************************************************************
  **
  ** Get the preferred size X
  */
  
  public Dimension getPreferredSize() {
    //
    // We always display a fixed-size working space in world coords.  This 
    // gives us the necessary component size we need to accommodate that world
    // at the given zoom level.
    //
    UiUtil.fixMePrintout("seeing NPE here after append tab, undo, redo, undo:");
    if (rcx_ == null) {
      System.out.println("hi");
    }
    if (rcx_.getWorkspaceSource() == null) {
      System.out.println("hi2");
    }
    if (rcx_.getWorkspaceSource().getWorkspace() == null) {
      System.out.println("hi3");
    }
    Rectangle rect = rcx_.getWorkspaceSource().getWorkspace().getWorkspace();
    int width = (int)(rect.width * zoom_); 
    int height = (int)(rect.height * zoom_);
    return (new Dimension(width, height));
  }  
 
  /***************************************************************************
  **
  ** Get workspace bounds X
  */
  
  public Rectangle getWorkspaceBounds() {
    return ((Rectangle)rcx_.getWorkspaceSource().getWorkspace().getWorkspace().clone());
  }  
  
  /***************************************************************************
  **
  ** Gets center point in viewport coordinates X
  */
  
  public Point getCenterPoint() {
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Point2D center = ws.getCenter();
    Point2D centerPoint = (Point2D)center.clone();
    transform_.transform(centerPoint, centerPoint);
    int x = (int)(Math.round(centerPoint.getX()));
    int y = (int)(Math.round(centerPoint.getY()));
    Point pt = new Point(x, y);
    return (pt);
  }

  /***************************************************************************
  **
  ** Gets center point in world coordinates X
  */
  
  public Point2D getRawCenterPoint() {
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Point2D center = ws.getCenter();    
    return ((Point2D)center.clone());
  } 
  
  /***************************************************************************
  **
  ** Fixes the center point X.  Some users do not care; override if wanted
  */
  
  public void fixCenterPoint(boolean doComplete, UndoSupport support, boolean closeIt) {
    return;
  }
    
  /***************************************************************************
  **
  ** Get the basic (unzoomed) image size: X Override for complex behavior
  */
  
  public Dimension getBasicSize(boolean doComplete, boolean doBuffer, int moduleHandling) {
    Rectangle origRect = myGenomePre_.getRequiredSize(rcx_, doComplete, 
                                                      doBuffer, false, false, 
                                                      null, null, null);
    return (new Dimension(origRect.width, origRect.height));
  }
  
  /***************************************************************************
  **
  ** Get the basic model bounds X.  Override for complex behavior
  */
  
  public Rectangle getCurrentBasicBounds(boolean doComplete, boolean doBuffer, int moduleHandling) {
    return (myGenomePre_.getRequiredSize(rcx_, doComplete, doBuffer, false, false, 
                                         null, null, null));  
  }  
 
  /***************************************************************************
  **
  ** Get the bounds of the selected parts.  May be null. X
  */
  
  public Rectangle getSelectedBounds() {  
    if (rcx_.getCurrentGenome() == null) {
      return (null);
    }
    return (myGenomePre_.getSelectionSize(rcx_));  
  }
 
  /***************************************************************************
  **
  ** Answers if we have a current selection (i.e. are cycling through the selections) 
  */
  
  public boolean haveCurrentSelectionForBounds() {  
    if (rcx_.getCurrentGenome() == null) {
      return (false);
    }
    return (myGenomePre_.haveCurrentSelection());
  }

  /***************************************************************************
  **
  ** Answers if we have a current selection (i.e. are cycling through the selections) 
  */
  
  public boolean haveMultipleSelectionsForBounds() {  
    if (rcx_.getCurrentGenome() == null) {
      return (false);
    }
    return (myGenomePre_.haveMultipleSelections());
  }

  /***************************************************************************
  **
  ** Get the bounds of the selected parts.  May be null. X
  */
  
  public Rectangle getCurrentSelectedBounds() {  
    if (rcx_.getCurrentGenome() == null) {
      return (null);
    }
    return (myGenomePre_.getCurrentSelectionSize(rcx_));  
  }
  
  /***************************************************************************
  **
  ** Get the bounds of all models.  Override for more complex behavior.
  */ 
       
  public Rectangle getAllModelBounds() {
    return (null);
  } 
 
  /***************************************************************************
  **
  ** Gets given point in viewport coordinates X
  */
  
  public Point pointToViewport(Point worldPoint) {
    Point2D newPoint = new Point2D.Double(worldPoint.getX(), worldPoint.getY());
    transform_.transform(newPoint, newPoint);
    int x = (int)(Math.round(newPoint.getX()));
    int y = (int)(Math.round(newPoint.getY()));
    Point pt = new Point(x, y);
    return (pt);
  } 
  
  /***************************************************************************
  **
  ** Gets given viewpoint in world coordinates: Computes inverse; use sparingly! X
  */
  
  public Point2D viewToWorld(Point viewPoint) {
    Point2D ptSrc = new Point2D.Double(viewPoint.getX(), viewPoint.getY());
    try {
      Point2D ptDest = new Point2D.Double(0.0, 0.0);
      transform_.inverseTransform(ptSrc, ptDest);
      return (ptDest);
    } catch (NoninvertibleTransformException ex) {
      System.err.println("cannot invert: " + transform_);
      throw new IllegalStateException();
    }
  }  
  
  /***************************************************************************
  **
  ** Set the zoom
  */
 
  public void setZoomFactor(double zoom) {   
    zoom_ = zoom;
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Rectangle rect = ws.getWorkspace();
    transform_ = new AffineTransform();    
    transform_.translate((rect.getWidth() / 2.0) * zoom, (rect.getHeight() / 2.0) * zoom);
    transform_.scale(zoom, zoom);
    Point2D center = ws.getCenter();
    transform_.translate(-center.getX(), -center.getY()); 
    myGenomePre_.setPresentationZoomFactor(zoom, rcx_); 
    return;
  }
   
  /***************************************************************************
  **
  ** Set the zoom X
  */
 
  public void setWideZoomFactor(double zoom, Dimension viewportDim) {   
    //
    // If the viewport is larger than the preferred size, we center using that:
    //
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Rectangle rect = ws.getWorkspace();
   
    int rectWidth = (int)(rect.getWidth() * zoom); 
    int rectHeight = (int)(rect.getHeight() * zoom);
    
    int useWidth = (rectWidth < viewportDim.width) ? viewportDim.width : rectWidth;
    int useHeight = (rectHeight < viewportDim.height) ? viewportDim.height : rectHeight;
    Point2D center = ws.getCenter(); 
    
    zoom_ = zoom;
    transform_ = new AffineTransform();    
    transform_.translate((useWidth / 2.0), (useHeight / 2.0));
    transform_.scale(zoom, zoom);
    transform_.translate(-center.getX(), -center.getY()); 
    myGenomePre_.setPresentationZoomFactor(zoom, rcx_); 
    return;
  }
  
  /***************************************************************************
  **
  ** Set the zoom X
  */
 
  public void adjustWideZoomForSize(Dimension viewportDim) {   
    //
    // If the viewport is larger than the preferred size, we center using that:
    //
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Rectangle rect = ws.getWorkspace();
   
    int rectWidth = (int)(rect.getWidth() * zoom_); 
    int rectHeight = (int)(rect.getHeight() * zoom_);
    
    
    int useWidth = (rectWidth < viewportDim.width) ? viewportDim.width : rectWidth;
    int useHeight = (rectHeight < viewportDim.height) ? viewportDim.height : rectHeight;
    Point2D center = ws.getCenter();
    
    transform_ = new AffineTransform();    
    transform_.translate((useWidth / 2.0), (useHeight / 2.0));
    transform_.scale(zoom_, zoom_);
    transform_.translate(-center.getX(), -center.getY());   

    return;
  }  
  
  /***************************************************************************
  **
  ** Get the zoom level needed to show the workspace X
  */
 
  public double getWorkspaceZoom(Dimension viewportDim, double percent) { 
    Workspace ws = rcx_.getWorkspaceSource().getWorkspace();
    Rectangle rect = ws.getWorkspace();

    double wZoom = viewportDim.width / rect.getWidth();
    double hZoom = viewportDim.height / rect.getHeight();        
    return (((wZoom > hZoom) ? hZoom : wZoom) * percent);
  }    

  /***************************************************************************
  **
  ** Get the zoom
  */
  
  public double getZoomFactor() {
    return (zoom_);
  } 
  
  /***************************************************************************
  **
  ** Install our transform, unless overridden
  ** PAY ATTENTION!  This is COMPOSITING the given transform with the
  ** existing g2 transform, NOT SETTING it!
  */  
  
  public void installTransform(Graphics2D g2, AffineTransform useTrans) { 
    if (useTrans == null) {
      useTrans = transform_;
    }
    g2.transform(useTrans);
    return;
  }  
  
  /***************************************************************************
  **
  ** Transforms screen point to model coordinates
  */  
  
  public void transformPoint(Point pt) { 
    try {
      transform_.inverseTransform(pt, pt);
    } catch (NoninvertibleTransformException nitex) {
      throw new IllegalStateException();
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Transforms screen point to model coordinates
  */  
  
  public void transformClick(int x, int y, Point pt) {
    Point2D clickPoint = new Point2D.Double(x, y);      
    try {
      transform_.inverseTransform(clickPoint, clickPoint);
    } catch (NoninvertibleTransformException nitex) {
      throw new IllegalStateException();
    }
    pt.x = (int)(Math.round(clickPoint.getX()));
    pt.y = (int)(Math.round(clickPoint.getY()));     
  }
  
  /***************************************************************************
  **
  ** Return the current pixel diameter
  */  
  
  public double currentPixelDiameter() {
    Point2D clickPoint0 = new Point2D.Double(0.0, 0.0); 
    Point2D clickPoint1 = new Point2D.Double(1.0, 0.0);
    try {
      transform_.inverseTransform(clickPoint0, clickPoint0);
      transform_.inverseTransform(clickPoint1, clickPoint1);
    } catch (NoninvertibleTransformException nitex) {
      throw new IllegalStateException();
    }
    return (Math.abs(clickPoint1.getX() - clickPoint0.getX()));
  }
  
  /***************************************************************************
  **
  ** Return the current clip rect (in world coordinates)
  */  
  
  public Rectangle2D getCurrentWorldClipRect() {
    Point2D clipUL = new Point2D.Double(clipRect_.getMinX(),  clipRect_.getMinY()); 
    Point2D clipLR = new Point2D.Double(clipRect_.getMaxX(),  clipRect_.getMaxY());
    try {
      transform_.inverseTransform(clipUL, clipUL);
      transform_.inverseTransform(clipLR, clipLR);
    } catch (NoninvertibleTransformException nitex) {
      throw new IllegalStateException();
    }
    return (new Rectangle2D.Double(clipUL.getX(), clipUL.getY(), clipLR.getX() - clipUL.getX(), clipLR.getY() - clipUL.getY()));
  }
  
  /***************************************************************************
  **
  ** Transforms screen point to model coordinates
  */  
  
  public void transformClick(double x, double y, Point pt) {
    Point2D clickPoint = new Point2D.Double(x, y);      
    try {
      transform_.inverseTransform(clickPoint, clickPoint);
    } catch (NoninvertibleTransformException nitex) {
      throw new IllegalStateException();
    }
    pt.x = (int)(Math.round(clickPoint.getX()));
    pt.y = (int)(Math.round(clickPoint.getY()));     
  }  
}
