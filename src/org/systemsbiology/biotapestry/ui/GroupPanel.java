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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RGBImageFilter;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
** 
** This is the group panel wrapper.
*/

public class GroupPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private UIComponentSource uics_; 
  private CmdSource cSrc_;
  private StaticDataAccessContext dacx_;

  //
  // Null when headless:
  //
  
  private ModelImagePanel myPanel_;
  private BufferedImage myMap_;
  private BufferedImage myImg_;
  private Map<Color, NavTree.GroupNodeMapEntry> modelMap_;

  public enum ImageType {
    Image,
    Mask
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Constructor
  */
  
  public GroupPanel(UIComponentSource uics, CmdSource cSrc) {
    uics_ = uics;
    cSrc_ = cSrc;
    myPanel_ = new ModelImagePanel(uics);
    myPanel_.setImage(null);
   
    JPanel mymip = myPanel_.getPanel();
    if (mymip != null) {
      mymip.addMouseListener(new MouseHandler());
    }
    if (mymip != null) {
      mymip.addMouseMotionListener(new MouseMotionHandler());
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the DataAccessContext
  */
  
  public void setDataContext(StaticDataAccessContext dacx) {
    UiUtil.fixMePrintout("BOGUS DACX, but cannot be null");
    dacx_ = dacx;
    return;
  }

  /***************************************************************************
  **
  ** Set the image
  */
  
  public void setImage(BufferedImage bi) {
    JPanel mymip = myPanel_.getPanel();
    if (mymip != null) {
      myPanel_.setImage(bi);
    }
    myImg_ = bi;
    return;
  }

  /***************************************************************************
  **
  ** Set the map
  */

  public void setMap(BufferedImage bi, Map<Color, NavTree.GroupNodeMapEntry> modelMap) {
    myMap_ = bi;
    modelMap_ = (modelMap == null) ? null : new HashMap<Color, NavTree.GroupNodeMapEntry>(modelMap);
    return;
  }

  /***************************************************************************
   **
   ** Returns the map
   */
  public Map<Color, NavTree.GroupNodeMapEntry> getMap() {
    return modelMap_;
  }

  /***************************************************************************
   **
   ** Returns the image
   */
  public BufferedImage getImg() {
    return myImg_;
  }

  /***************************************************************************
   **
   ** Returns the map image
   */
  public BufferedImage getMapImg() {
    return myMap_;
  }

  /***************************************************************************
  **
  ** Set the map
  */
  
  public BufferedImage buildMaskedImage(BufferedImage baseImg, BufferedImage startImg, int r, int g, int b) {
    int h = startImg.getHeight();
    int w = startImg.getWidth();
    BufferedImage source = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Raster raster = startImg.getRaster().createChild(0, 0, w, h, 0, 0, new int[] {0, 1, 2});
    source.setData(raster);
 
    BufferedImage dstImage = null;
    LookupOp op = new LookupOp(new SingleColorLookupTable(r, g, b), null);
    dstImage = op.filter(source, null);   
    
    BufferedImage aimg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);   
    Graphics2D gd = aimg.createGraphics();  

    gd.drawImage(baseImg, 0, 0, w, h, 0, 0, w, h, null);
    gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));
    gd.drawImage(dstImage, 0, 0, w, h, 0, 0, w, h, null); 
    gd.dispose(); 
    return (aimg);
  }

  /***************************************************************************
  **
  ** Set the map
  */
  
  public BufferedImage buildMaskedImageToo(BufferedImage baseImg, BufferedImage startImg, int r, int g, int b) {
    int h = startImg.getHeight();
    int w = startImg.getWidth();
 
    OneColorFilter filter = new OneColorFilter(r, g, b);
    ImageProducer ip = new FilteredImageSource(startImg.getSource(), filter);
    Image ret = Toolkit.getDefaultToolkit().createImage(ip);
    BufferedImage aimg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
    Graphics2D g2 = aimg.createGraphics();
    g2.drawImage(baseImg, 0, 0, w, h, 0, 0, w, h, null);
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4F));    
    g2.drawImage(ret, 0, 0, null);
    g2.dispose();
    return (aimg);
  }

  /***************************************************************************
  **
  ** Model drawing
  */
   
  public void drawPanel(boolean revalidate) {
    JPanel mymip = myPanel_.getPanel();
    if (mymip != null) {
      if (revalidate) {
        mymip.revalidate();
      }
      mymip.repaint();
    }
    return;
  }  
   
  /***************************************************************************
  **
  ** Get panel. Null for headless
  */
   
  public JPanel getPanel() {
    return (myPanel_.getPanel());
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
  
  public class OneColorFilter extends RGBImageFilter {  
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
  ** Filter for turning one color black, all other colors white
  */  
  
  public class SingleColorLookupTable extends LookupTable {
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


  /***************************************************************************
   **
   ** ClickMapColorMinMaxBounder
   **
   **
   */
  public static class ClickMapColorMinMaxBounder {
    private BufferedImage myImg_;

    public class BoundSearchResult {
      public boolean found_;
      public Rectangle rectangle_;

      public BoundSearchResult(boolean found, Rectangle rectangle) {
        found_ = found;
        rectangle_ = rectangle;
      }
    }

    public BoundSearchResult getBoundingRectangleForColor(Color color) {
      int minX = Integer.MAX_VALUE;
      int maxX = -1;
      int minY = Integer.MAX_VALUE;
      int maxY = -1;

      boolean found = false;

      for (int y = 0; y < myImg_.getHeight(); y++) {
        for (int x = 0; x < myImg_.getHeight(); x++) {
          int color32 = myImg_.getRGB(x, y);
          int r = (color32 & 0x00ff0000) >> 16;
          int g = (color32 & 0x0000ff00) >> 8;
          int b = color32 & 0x000000ff;

          if (color.getRed() == r && color.getGreen() == g && color.getBlue() == b) {
            found = true;

            if (x < minX) {
              minX = x;
            }
            if (x > maxX) {
              maxX = x;
            }
            if (y < minY) {
              minY = y;
            }
            if (y > maxY) {
              maxY = y;
            }
          }
        }
      }

      return new BoundSearchResult(found, new Rectangle(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1));
    }

    public ClickMapColorMinMaxBounder(BufferedImage mapImage) {
      myImg_ = mapImage;
    }
  }

  /***************************************************************************
  **
  ** Handles click events
  */  
  
  public class MouseHandler extends MouseAdapter {

    private final static int CLICK_SLOP_  = 2;
    
    @Override
    public void mousePressed(MouseEvent me) {
      try {
        // IGNORE drags except with button 1 only
        int mods = me.getModifiers();
        int onmask = MouseEvent.BUTTON1_MASK;
        int offmask = MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK;
        boolean oneOnly = false;
        if ((mods & (onmask | offmask)) == onmask) {
          oneOnly = true;
        }
        if (!me.isPopupTrigger()) {
          //
        }
        if (me.isPopupTrigger()) {
       //
        } else if (oneOnly) {
//
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
      if (me.isPopupTrigger()) {
 
      }
      return;
    }    
    
    //
    // We want to catch the mouse position when overlaid dialog windows close
    // but the mouse is not moving:
    //
    
    @Override
    public void mouseEntered(MouseEvent me) {
      try {
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }        
      return;
    }       
    
    @Override
    public void mouseReleased(MouseEvent me) { 
      try {
        int currX = me.getX();
        int currY = me.getY();
        System.out.println("scr " + currX + " " + currY);
        if (myMap_ != null) {
          Point wpt = myPanel_.screenToWorld(new Point(currX, currY));
          System.out.println(currX + " " + currY);
          System.out.println("wo " + wpt);
          if ((wpt.x < 0) || (wpt.y < 0)) {
            System.out.println("OOB");
            return;
          }
          int width = myMap_.getWidth();
          int height = myMap_.getHeight();
          if ((wpt.x >= width) || (wpt.y >= height)) {
            System.out.println("OOB");
            return;
          }
          Color daCol = new Color(myMap_.getRGB(wpt.x, wpt.y));
          System.out.println(daCol.getRed() + " " + daCol.getGreen() + " " + daCol.getBlue());
          NavTree.GroupNodeMapEntry gnme = modelMap_.get(daCol);
          if (gnme == null) {
            UiUtil.fixMePrintout("Probably error cursor");
            return;
          }

          cSrc_.getGroupPanelCmds().processMouseClick(gnme.modelID, gnme.proxyID, gnme.proxyTime, dacx_);
        }
        
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }        
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Handles mouse motion events
  */  
      
  public class MouseMotionHandler extends MouseMotionAdapter {
    
    @Override
    public void mouseMoved(MouseEvent me) {
      try {
        int currX = me.getX();
        int currY = me.getY();
        System.out.println("scr " + currX + " " + currY);
        if (myMap_ != null) {
          Point wpt = myPanel_.screenToWorld(new Point(currX, currY));
          System.out.println(currX + " " + currY);
          System.out.println("wo " + wpt);
          if ((wpt.x < 0) || (wpt.y < 0)) {
            System.out.println("OOB");
            return;
          }
          int width = myMap_.getWidth();
          int height = myMap_.getHeight();
          if ((wpt.x >= width) || (wpt.y >= height)) {
            System.out.println("OOB");
            return;
          }
          Color daCol = new Color(myMap_.getRGB(wpt.x, wpt.y));
          System.out.println(daCol.getRed() + " " + daCol.getGreen() + " " + daCol.getBlue());
          NavTree.GroupNodeMapEntry gnme = modelMap_.get(daCol);
          if (gnme == null) {
            uics_.getTextBoxMgr().clearCurrentMouseOver();
            myPanel_.setImage(myImg_);
          } else {
            uics_.getTextBoxMgr().setCurrentMouseOver(gnme.modelID + " " + gnme.proxyID + " " + gnme.proxyTime);
            BufferedImage bim = buildMaskedImageToo(myImg_, myMap_, daCol.getRed(), daCol.getGreen(), daCol.getBlue());
            myPanel_.setImage(bim);
          }
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }      
  } 
}
