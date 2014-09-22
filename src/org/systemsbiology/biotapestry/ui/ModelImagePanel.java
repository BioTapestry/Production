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

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.BTState;

/***************************************************************************
** 
** Panel to draw model images
*/

public class ModelImagePanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  

  
  private ModelImagePanelPanel mipp_;
  private ModelImagePresentation myImagePre_;
  private AffineTransform transform_;
  private BTState appState_;
  
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
  
  public ModelImagePanel(BTState appState) {
    appState_ = appState;
    myImagePre_ = new ModelImagePresentation();
    transform_ = new AffineTransform();
    if (!appState_.isHeadless()) {
      mipp_ = new ModelImagePanelPanel();
      mipp_.setBackground(Color.white);
    }   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Get the panel; null when headless
  */
  
  public JPanel getPanel() {
    return (mipp_);
  }
  
  /***************************************************************************
  **
  ** Set the image
  */
  
  public void setImage(BufferedImage bi) {
    myImagePre_.setImage(bi);
    if (!appState_.isHeadless()) {
      mipp_.repaint();
    }
    return;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
   private class ModelImagePanelPanel extends JPanel {
   
    private static final long serialVersionUID = 1L;
    
    /***************************************************************************
    **
    ** Drawing routine
    */
    
    public void paintComponent(Graphics g) {
      try {
        transform_.setToIdentity();
        Rectangle rect = myImagePre_.getRequiredSize();
        Rectangle2D finalRect = new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height);
        Dimension currentSize = this.getSize();
  
        double wZoom = (double)currentSize.width / finalRect.getWidth();
        double hZoom = (double)currentSize.height / finalRect.getHeight();    
        double zoom = ((wZoom > hZoom) ? hZoom : wZoom) / 1.5;  // give room to grow
        transform_.translate((double)currentSize.width / 2.0, (double)currentSize.height / 2.0);
        transform_.scale(zoom, zoom);
        double centerX = finalRect.getX() + (finalRect.getWidth() / 2.0);
        double centerY = finalRect.getY() + (finalRect.getHeight() / 2.0); 
        transform_.translate(-centerX, -centerY);
  
        super.paintComponent(g);
        drawingGuts(g, transform_);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayPaintException(ex);
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
      myImagePre_.presentImage(g2, useTrans);
      return;
    }
  }
}
