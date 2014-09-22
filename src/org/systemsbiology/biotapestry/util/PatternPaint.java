/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform; 
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.Transparency;

/****************************************************************************
**
** A Paint that uses a pattern
*/

public class PatternPaint implements Paint {
  
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
  
  public static final int FORWARD_DIAGONAL_LINES        = 0;
  public static final int BACKWARD_DENSE_DIAGONAL_LINES = 1;
  public static final int DOTS                          = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private int patternType_;
  private Color patternColor_;
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PatternPaint(int patternType, Color patternColor) {
    patternType_ = patternType;
    patternColor_ = patternColor;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** A function
  */
 
  public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, 
                                    Rectangle2D userBounds, AffineTransform xform, 
                                    RenderingHints hints) {
    return (new PatternPaintContext(patternType_, patternColor_));                      
  }

  
  /***************************************************************************
  **
  ** Return transparency type
  */
  
  public int getTransparency() {
    return (Transparency.OPAQUE);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Context for pattern painting
  */
  
  public static class PatternPaintContext implements PaintContext {
    
    private int patternType_;
    private Color patternColor_;
       
    public PatternPaintContext(int patternType, Color patternColor) {
      patternType_ = patternType;
      patternColor_ = patternColor;
    }  
 
    public void dispose() {
      // Nothing to do...
      return;
    }

    public ColorModel getColorModel() {
      return (new DirectColorModel(32, 0x00ff0000,0x0000ff00,0x000000ff, 0xff000000));       
    }
    
    public Raster getRaster(int x, int y, int w, int h) {
      int pcRed = patternColor_.getRed();
      int pcGreen = patternColor_.getGreen();
      int pcBlue = patternColor_.getBlue();
      
      Color white = Color.white;
      int bgRed = white.getRed();
      int bgGreen = white.getGreen();
      int bgBlue = white.getBlue();      
      
      
      WritableRaster wr = getColorModel().createCompatibleWritableRaster(w, h);
      int[] data = new int[4];
      switch (patternType_) {
        case FORWARD_DIAGONAL_LINES:
          for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
              int patPos = (x + y + i + j) % 8;            
              boolean isPat = (patPos >= 0) && (patPos <= 1);
              int base = 0;           
              data[base]     = (isPat) ? pcRed : bgRed;
              data[base + 1] = (isPat) ? pcGreen : bgGreen;
              data[base + 2] = (isPat) ? pcBlue : bgBlue;
              data[base + 3] = 255;
              wr.setPixel(i, j, data);
            }
          }
          break;
        case BACKWARD_DENSE_DIAGONAL_LINES:
          for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
              int patPos = ((3 - ((x + i) % 4)) + y + j) % 4;            
              boolean isPat = (patPos >= 2) && (patPos <= 3);
              int base = 0;           
              data[base]     = (isPat) ? pcRed : bgRed;
              data[base + 1] = (isPat) ? pcGreen : bgGreen;
              data[base + 2] = (isPat) ? pcBlue : bgBlue;
              data[base + 3] = 255;
              wr.setPixel(i, j, data);
            }
          }
          break;
       case DOTS:
          for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
              int patPos = ((3 - ((x + i) % 4)) + y + j) % 4;
              int patLine = (y + j) % 2;
              boolean isPat = (patPos >= 2) && (patPos <= 3) && (patLine == 0);
              int base = 0;           
              data[base]     = (isPat) ? pcRed : bgRed;
              data[base + 1] = (isPat) ? pcGreen : bgGreen;
              data[base + 2] = (isPat) ? pcBlue : bgBlue;
              data[base + 3] = 255;
              wr.setPixel(i, j, data);
            }
          }
          break;
        default:
          throw new IllegalStateException();
      }
      return (wr);
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
