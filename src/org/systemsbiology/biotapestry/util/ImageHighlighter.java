/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RGBImageFilter;
import java.awt.image.Raster;
import java.util.HashSet;
import java.util.Set;

/***************************************************************************
** 
** This class handles image highlighting, useful for e.g. mouseover intersections
*/

public class ImageHighlighter {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor is Private. Just use the static methods.
  */
  
  private ImageHighlighter() {
 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build a masked image where the base image is highlighted where the mask image matches the given color.
  ** Using the snazzy java.awt.image classes, but this seems unable to do the map to transparency I need, so
  ** the baseImg is just alpha'd with a black and white mask.
  */
  
  public static BufferedImage buildMaskedImageBandW(BufferedImage baseImg, BufferedImage maskImg, int r, int g, int b, float alpha) {
    int h = baseImg.getHeight();
    int w = baseImg.getWidth();
    // Had REAL problems unless we created an RGB-only (i.e. no alpha channel) image.
    BufferedImage source = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Raster raster = maskImg.getRaster().createChild(0, 0, w, h, 0, 0, new int[] {0, 1, 2});
    source.setData(raster);
 
    //
    // Converting the target color to black, and everything else to white. Note I was not able
    // to get alpha stuff set using this technique, so the maskImg ends up black and white. Suboptimal!
    //
    BufferedImage dstImage = null;
    LookupOp op = new LookupOp(new SingleColorLookupTable(r, g, b), null);
    dstImage = op.filter(source, null);   
    
    BufferedImage aimg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);   
    Graphics2D gd = aimg.createGraphics();  

    gd.drawImage(baseImg, 0, 0, w, h, 0, 0, w, h, null);
    gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    gd.drawImage(dstImage, 0, 0, w, h, 0, 0, w, h, null); 
    gd.dispose(); 
    return (aimg);
  }

  /***************************************************************************
  **
  ** Build a masked image with white and transparency
  */
  
  public static BufferedImage buildMaskedImage(BufferedImage baseImg, BufferedImage maskImg, int r, int g, int b, float alpha) {
    int h = baseImg.getHeight();
    int w = baseImg.getWidth();
 
    OneColorFilter filter = new OneColorFilter(r, g, b);
    ImageProducer ip = new FilteredImageSource(maskImg.getSource(), filter);
    Image ret = Toolkit.getDefaultToolkit().createImage(ip);
    BufferedImage aimg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
    Graphics2D g2 = aimg.createGraphics();
    g2.drawImage(baseImg, 0, 0, w, h, 0, 0, w, h, null);
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));    
    g2.drawImage(ret, 0, 0, null);
    g2.dispose();
    return (aimg);
  }
 
  /***************************************************************************
  **
  ** Collect up colors in an image
  */
  
  public static Set<Color> collectColors(BufferedImage img) { 
    
    if (img == null) {
      return (new HashSet<Color>());
    }

    int h = img.getHeight();
    int w = img.getWidth();
    
    CollectColorFilter filter = new CollectColorFilter();
    ImageProducer ip = new FilteredImageSource(img.getSource(), filter);
    Image ret = Toolkit.getDefaultToolkit().createImage(ip);
    // Thought just the above code would do the trick, but the filtering is apparently lazy.
    // Need to actually draw the image to get it to run the filter. Thus, we do. Oh well....
    BufferedImage aimg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
    Graphics2D g2 = aimg.createGraphics();
    g2.drawImage(ret, 0, 0, null);
    g2.dispose();
     
    return (filter.getResults());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Filter for turning one color to opaque white, all other colors to transparent black
  */  
  
  public static class OneColorFilter extends RGBImageFilter {  
    private int r_;
    private int g_;
    private int b_;
     
    public OneColorFilter(int r, int g, int b) {
      r_ = r;
      g_ = g;
      b_ = b;  
    } 
    
    public int filterRGB(int x, int y, int rgb) {
      int r = (rgb & 0xFF0000) >> 16;
      int g = (rgb & 0xFF00) >> 8;
      int b = rgb & 0xFF;
      if ((r == r_) && (g == g_) && (b == b_)) {
          // Set fully transparent but keep color
        return (0xFFFFFFFF);
      } else {
        return (0x00000000);
      }
    }
  };
  
  /***************************************************************************
  **
  ** On the assumption that the filter will access the image faster than a pixel-by-pixel walk
  */  
  
  public static class CollectColorFilter extends RGBImageFilter {  
    private HashSet<Integer> myColors_;
     
    public CollectColorFilter() {
      myColors_ = new HashSet<Integer>();
    } 
    
    public Set<Color> getResults() {
      HashSet<Color> retval = new HashSet<Color>();
      for (Integer col : myColors_) {
        retval.add(new Color(col.intValue()));
      } 
      return (retval);
    }
    
    public int filterRGB(int x, int y, int rgb) {
      myColors_.add(Integer.valueOf(rgb));
      return (rgb);
    }
  };
 
 
  /***************************************************************************
  **
  ** Filter for turning one color black, all other colors white
  */  
  
  public static class SingleColorLookupTable extends LookupTable {
    private int r_;
    private int g_;
    private int b_;
 
    public SingleColorLookupTable(int r, int g, int b) {
      super(0, 3);
      r_ = r;
      g_ = g;
      b_ = b;  
    }   
    
    public int[] lookupPixel(int[] src, int[] dest)  {
      if (dest == null) {
        dest = new int[3];
      }
      if ((src[0] == r_) && (src[1] == g_) && (src[2] == b_)) {
        dest[0] = 0x00;
        dest[1] = 0x00;
        dest[2] = 0x00;
      } else {
        dest[0] = 0xFF;
        dest[1] = 0xFF;
        dest[2] = 0xFF;
      }
      return (dest);
    }
  }
}
