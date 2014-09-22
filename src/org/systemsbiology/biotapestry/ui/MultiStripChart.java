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

import java.awt.Color;
import java.awt.Point;
import java.awt.Paint;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.event.MouseEvent;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.DoubMinMax;
import org.systemsbiology.biotapestry.util.PatternPaint;

/****************************************************************************
**
** The multi strip chart
*/

public abstract class MultiStripChart extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int REGULAR      = 0;
  public static final int DISQUALIFIED = 1;
  public static final int CALLED_OUT   = 2; 
  
  public static final int SOLID_PATTERN   = 0;
  public static final int HATCHED_PATTERN = 1;
  public static final int OUTLINE_PATTERN = 2;
  public static final int DOT_PATTERN     = 3;
  
  public static final int COVERAGE_ALL     = 0;
  public static final int COVERAGE_LIMITED = 1;
  //private static final int NUM_COVERAGE_    = 2; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  protected static final int STRIP_SIZE_      = 16;
  protected static final int STRIPE_SIZE_     = STRIP_SIZE_ / 4;
  protected static final int STRIPE_OFFSET_   = (STRIP_SIZE_ - STRIPE_SIZE_) / 2;
  protected static final int BUFFER_SIZE_     = 8; 
  protected static final int ROW_DELTA_       = STRIP_SIZE_ + BUFFER_SIZE_; 
  protected static final int VERTICAL_MARGIN_ = 20;
  // Font bounds gives fixed height that is ~ 33% taller than upper case,
  // so hack it to get it centered.
  protected static final double HEIGHT_HACK_ = 0.75;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  protected List<Strip> allStrips_;
  protected List<MultiStripChart.Strip> prunedStrips_;
  protected Color vlg_;
  protected FontRenderContext frc_;
  protected Dimension dimForCache_;
  protected ChartTransform transform_;
  protected int coverage_;
  protected BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Null constructor
  */
  
  public MultiStripChart(BTState appState) {
    appState_ = appState;
    allStrips_ = new ArrayList<MultiStripChart.Strip>();
    prunedStrips_ = new ArrayList<MultiStripChart.Strip>();
    coverage_ = COVERAGE_ALL;
    setBackground(Color.white);
    vlg_ = new Color(242, 221, 170);
    frc_ = new FontRenderContext(new AffineTransform(), true, true);  // for consistency with tooltips
    transform_ = null;
    dimForCache_ = null;
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the tool tip
  */
  
  public String getToolTipText(MouseEvent event) {
    Point tipPoint = event.getPoint();
    int size = useStrips().size();
    int minY = VERTICAL_MARGIN_;
    int ptY = tipPoint.y;
    int ptX = tipPoint.x;
        
    Dimension dim = getSize();
    if ((dimForCache_ == null) || !dimForCache_.equals(dim)) {
      refreshSizeBasedCache(dim);
    }
    double worldX = convertFromScreenX(ptX, transform_);
    if (childRejectTip(worldX)) {
      return (null);
    }
    
    for (int i = 0; i < size; i++) {
      int maxY = minY + STRIP_SIZE_;
      if ((ptY >= minY) && (ptY <= maxY)) {
        Strip intersect = useStrips().get(i);
        List<StripElement> elem = intersect.elements;
        int elemSize = elem.size();
        for (int j = 0; j < elemSize; j++) {   
          StripElement se = elem.get(j);
          if ((worldX >= se.min) && (worldX <= se.max)) {
            return (se.tipText);
          }
        }
        return (null);
      }
      minY = maxY + BUFFER_SIZE_;
    }    
    return (null);    
  }    
  
  /***************************************************************************
  **
  ** Set coverage
  */
  
  public void setCoverage(int coverage) {
    coverage_ = coverage;
    dimForCache_ = null;
    invalidate();
    repaint();
    return;
  }
   
  /***************************************************************************
  **
  ** Drawing routine
  */
  
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g; 
    drawingGuts(g2);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the strips to display
  */
  
  public void setStrips(List<Strip> strips, List<Strip> prunedStrips) {
    dimForCache_ = null;
    allStrips_.clear();
    allStrips_.addAll(strips);
    prunedStrips_.clear();
    prunedStrips_.addAll(prunedStrips);
    invalidate();
    return;
  }
  
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMinimumSize() {
    return (getPreferredSize());
  }  
  
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMaximumSize() {
    return (getPreferredSize());
  }    
 
  /***************************************************************************
  **
  ** Get the preferred size
  */
  
  public Dimension getPreferredSize() {
    //
    // We always display a fixed-size working space in world coords.  This 
    // gives us the necessary component size we need to accommodate that world
    // at the given zoom level.
    //
    int width = 400;  // FIX ME 
    int stripSize = useStrips().size();
    stripSize = (stripSize == 0) ? 1 : stripSize;
    int height = (ROW_DELTA_ * stripSize) + (2 * VERTICAL_MARGIN_);
    return (new Dimension(width, height));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Defines a display strip
  */  
      
  public static class Strip {
    
    public List<StripElement> elements;
    public String name;
  
    public Strip(String name, List<StripElement> elements) {
      this.name = name;
      this.elements = elements;
    }
  }
  
  /***************************************************************************
  **
  ** Defines a display strip element
  */
  
  public static class StripElement {
    
    public double min;
    public double max;
    public Color color;
    public Color stripeColor;
    public int pattern;
    public String tipText;
    public int decoration;

    public StripElement(double min, double max, Color color, int pattern, 
                        String tipText, int decoration) {
      this.min = min;
      this.max = max;
      this.color = color;
      this.stripeColor = null;
      this.pattern = pattern;
      this.tipText = tipText;
      this.decoration = decoration;
    }
    
    public StripElement(double min, double max, Color color, Color stripeColor, 
                        String tipText, int decoration) {
      this.min = min;
      this.max = max;
      this.color = color;
      this.pattern = MultiStripChart.SOLID_PATTERN;
      this.stripeColor = stripeColor;
      this.tipText = tipText;
      this.decoration = decoration;      
    }    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** get the current strip list
  */
  
  protected List<Strip> useStrips() {
    return ((coverage_ == COVERAGE_ALL) ? allStrips_ : prunedStrips_);
  }
  
  /***************************************************************************
  **
  ** Kids can reject non-strip related intersections:
  */
  
  protected abstract boolean childRejectTip(double worldX);

  /***************************************************************************
  **
  ** Handle resize
  */
  
  protected abstract ChartTransform childRefresh(Font mFont, Dimension dim);   

  /***************************************************************************
  **
  ** Handle resize
  */
  
  protected void refreshSizeBasedCache(Dimension dim) {     
    Font mFont = appState_.getFontMgr().getFixedFont(FontManager.STRIP_CHART_AXIS);
    transform_ = childRefresh(mFont, dim);
    dimForCache_ = dim;
    return;
  }  
  
  /***************************************************************************
  **
  ** Drawing core
  */
  
  protected void drawingGuts(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    Dimension dim = getSize();
    if ((dimForCache_ == null) || !dimForCache_.equals(dim)) {
      refreshSizeBasedCache(dim);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drawing core for just the strips
  */
  
  protected void drawAllStrips(Graphics2D g2, Dimension dim, DoubMinMax range) {
    BasicStroke ghostStroke = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
    int numRows = useStrips().size();
    int yDisp = numRows - 1;
    for (int i = 0; i < numRows; i++) {
      Strip strip = useStrips().get(yDisp);
      drawStrip(g2, yDisp--, dim.height, strip, range, transform_, ghostStroke);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Draw a strip
  */
  
  protected void drawStrip(Graphics2D g2, int row, int height, Strip strip, DoubMinMax displayRange,
                         ChartTransform transform, BasicStroke ghostStroke) {
    int numElem = strip.elements.size();
    int y = VERTICAL_MARGIN_ + (row * ROW_DELTA_);
    int stripMinX = Integer.MAX_VALUE;
    int stripMaxX = Integer.MIN_VALUE;
    int stripMinY = y;
    int stripMaxY = y + STRIP_SIZE_;
    int drawnElem = 0;
    for (int i = 0; i < numElem; i++) {
      StripElement se = (StripElement)strip.elements.get(i);
      Color drawColor = se.color;
      boolean doFill;
      Paint usePaint = null;
      switch (se.pattern) {
        case SOLID_PATTERN:
          usePaint = drawColor;
          doFill = true;
          break;
        case HATCHED_PATTERN:
          usePaint = new PatternPaint(PatternPaint.BACKWARD_DENSE_DIAGONAL_LINES, drawColor);
          doFill = true;
          break;
        case DOT_PATTERN:
          usePaint = new PatternPaint(PatternPaint.DOTS, drawColor);
          doFill = true;
          break;           
        case OUTLINE_PATTERN:
          usePaint = drawColor;
          doFill = false;
          break;
        default:
          throw new IllegalArgumentException();
      } 
      g2.setPaint(usePaint);
      double showMin = (se.min < displayRange.min) ? displayRange.min : se.min;
      double showMax = (se.max > displayRange.max) ? displayRange.max : se.max;
      int minX = convertToScreenX(showMin, transform);
      int maxX = convertToScreenX(showMax, transform);

      int width = maxX - minX;
      if (width > 0) {
        drawnElem++;
        if (minX < stripMinX) {
          stripMinX = minX;
        }
        if (maxX > stripMaxX) {
          stripMaxX = maxX;
        }      
        if (doFill) {
          g2.fillRect(minX, y, width, STRIP_SIZE_);
        } else {
          g2.setStroke(ghostStroke);
          g2.drawRect(minX, y, width, STRIP_SIZE_);        
        }
        if (se.stripeColor != null) {
          g2.setPaint(se.stripeColor);
          g2.fillRect(minX, y + STRIPE_OFFSET_, width, STRIPE_SIZE_);
        }
        if (se.decoration == DISQUALIFIED) {
          g2.setPaint(Color.black);
          g2.drawLine(minX, y, maxX, y + STRIP_SIZE_);
          g2.drawLine(minX, y + STRIP_SIZE_, maxX, y);          
        } else if (se.decoration == CALLED_OUT) {
          g2.setPaint(Color.black);
          g2.drawRect(minX, y, width - 1, STRIP_SIZE_);
        }
      }
    }
    
    //
    // Place strip label:
    //
    
    if (drawnElem > 0) {
      g2.setPaint(Color.black);
      Font mFont = appState_.getFontMgr().getFixedFont(FontManager.STRIP_CHART);
      Rectangle2D bounds = mFont.getStringBounds(strip.name, frc_);
      double tWidth = bounds.getWidth();
      double tHeight = bounds.getHeight() * HEIGHT_HACK_;
      double x = ((stripMaxX + stripMinX) / 2.0) - (tWidth / 2.0);
      double by = ((stripMaxY + stripMinY) / 2.0) + (tHeight / 2.0);
      g2.setFont(mFont);
      g2.drawString(strip.name, (float)x, (float)by);
    }
    return;
  }

  /***************************************************************************
  **
  ** X coordinate transform
  */
  
  protected int convertToScreenX(double xWorld, ChartTransform transform) {
    double xScr = ((xWorld - transform.worldMin) * transform.slope) + transform.screenMin;
    return (Math.round((float)xScr));
  }
  
  /***************************************************************************
  **
  ** X coordinate transform
  */
  
  protected double convertFromScreenX(int xScreen, ChartTransform transform) {
    double xWorld = (((double)xScreen - transform.screenMin) / transform.slope) + transform.worldMin;
    return (xWorld);
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Define transform
  */  
      
  protected class ChartTransform {
    
    protected double screenMin;
    protected double worldMin;
    protected double slope;
  
    protected ChartTransform(double slope, int screenMin, double worldMin) {
      this.screenMin = (double)screenMin;
      this.worldMin = worldMin;
      this.slope = slope;
    }
  }
}
