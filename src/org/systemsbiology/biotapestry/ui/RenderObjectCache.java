/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.TreeMap;
import java.awt.geom.AffineTransform;

/****************************************************************************
**
** Handles rendering of cached drawing objects
*/

public class RenderObjectCache {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  private TreeMap cache_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public RenderObjectCache() {
    cache_ = new TreeMap();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Clear the object cache
  */
  
  public void clear() {
    cache_.clear();
    return;
  }  

  /***************************************************************************
  **
  ** Add to the object cache
  */
  
  public void addObject(ModalShape ms, Integer majorLayer, Integer minorLayer) {   
    TreeMap perMajor = (TreeMap)cache_.get(majorLayer);
    if (perMajor == null) {
      perMajor = new TreeMap();
      cache_.put(majorLayer, perMajor);
    }
    ArrayList perMinor = (ArrayList)perMajor.get(minorLayer);
    if (perMinor == null) {
      perMinor = new ArrayList();
      perMajor.put(minorLayer, perMinor);
    }
    perMinor.add(ms);   
    return;
  }  

  /***************************************************************************
  **
  ** Render the object cache
  */
  
  public void render(Graphics2D g2) {
    
    //
    // Rendering is done by layers
    //
    
    Iterator vit = cache_.values().iterator();
    while (vit.hasNext()) {
      TreeMap perMajor = (TreeMap)vit.next();
      Iterator mit = perMajor.values().iterator();
      while (mit.hasNext()) {
        ArrayList perMinor = (ArrayList)mit.next();
        int size = perMinor.size();
        for (int i = 0; i < size; i++) {
          ModalShape ms = (ModalShape)perMinor.get(i); 
          ms.render(g2);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Render a layer of the object cache
  */
  
  public void renderALayer(Graphics2D g2, Integer majorLayer) {
    
    //
    // Rendering is done by layers
    //
    int majVal = majorLayer.intValue();
    Iterator kit = cache_.keySet().iterator();
    while (kit.hasNext()) {
      Integer majKey = (Integer)kit.next();
      TreeMap perMajor = (TreeMap)cache_.get(majKey);
      if (majKey.intValue() == majVal) {
        Iterator kmit = perMajor.keySet().iterator();
        while (kmit.hasNext()) {
          Integer minKey = (Integer)kmit.next();
          ArrayList perMinor = (ArrayList)perMajor.get(minKey);     
          int size = perMinor.size();
          for (int i = 0; i < size; i++) {
            ModalShape ms = (ModalShape)perMinor.get(i); 
            ms.render(g2);
          }
        }
      }
    }
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to store shapes to draw
  */
  
  public static class ModalShape {
    
    public static final int DRAW = 0;
    public static final int FILL = 1;

    private boolean isText_;
    private int mode_;    
    private Color color_;
    private BasicStroke stroke_;    
    private Shape shape_;
    private Font font_;
    private String text_;    
    private AffineTransform trans_;
    private float txtX_;
    private float txtY_;
    
    public ModalShape(int mode, Color color, BasicStroke stroke, Shape shape) {
      isText_ = false;
      mode_ = mode;
      color_ = color;
      stroke_ = stroke;
      shape_ = shape;
    }
    
    public ModalShape(String text, Color color, Font font, AffineTransform trans, float txtX, float txtY) {
      isText_ = true;
      text_ = text;
      color_ = color;      
      font_ = font;
      trans_ = trans;
      txtX_ = txtX;
      txtY_ = txtY;
    }    

    public void render(Graphics2D g2) {
      g2.setPaint(color_);
      if (isText_) {
        renderText(g2);
      } else {
        renderShape(g2);
      }
      return;
    } 

    private void renderText(Graphics2D g2) {    
      g2.setFont(font_);
      AffineTransform saveTrans = g2.getTransform();
      g2.transform(trans_);
      g2.drawString(text_, txtX_, txtY_);
      g2.setTransform(saveTrans);
      return;
    }
    
    private void renderShape(Graphics2D g2) {
      g2.setStroke(stroke_);
      switch (mode_) {
        case DRAW:
          g2.draw(shape_);
          break;
        case FILL:
          g2.fill(shape_);
          break;
        default:
          throw new IllegalArgumentException();
      } 
      return;
    }    
  }  
}
