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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.BasicStroke;
import java.awt.geom.GeneralPath;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.awt.Font;
import java.awt.font.FontRenderContext;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.DoubMinMax;
import org.systemsbiology.biotapestry.util.PatternPaint;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** The multi strip chart
*/

public class MultiStripTimeAxisChart extends MultiStripChart {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int AXIS_SIZE_       = 10;
  private static final int AXIS_MARGIN_     = 30;
  private static final int TIC_PADDING_     = 40;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private int[] ticCandidates_;
  private TimeBounds dataTimeRange_;
  private TimeBounds focusTimeRange_;
  private Axis axis_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Null constructor
  */
  
  public MultiStripTimeAxisChart(DataAccessContext dacx) {
    super(dacx);
    ticCandidates_ = new int[] {1,2,3,4,5,6,8,10,12,15,20,25,30,40,50,100,150,200,250,500,1000};
    dataTimeRange_ = new TimeBounds(0, 1, "");
    focusTimeRange_ = new TimeBounds(0, 1, ""); 
    axis_ = null;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  /***************************************************************************
  **
  ** Set the data range to display
  */
  
  public void setDataTimeRange(TimeBounds tr) {
    dimForCache_ = null;
    dataTimeRange_ = tr;
    return;
  }  

  /***************************************************************************
  **
  ** Set the data range to display
  */
  
  public void setFocusTimeRange(TimeBounds tr) {
    focusTimeRange_ = tr;
    return;
  }   
    
  /***************************************************************************
  **
  ** Get the preferred size
  */
  
  public Dimension getPreferredSize() {
    Dimension dim = super.getPreferredSize();
    dim.height += AXIS_SIZE_;
    return (dim);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Defines time bounds
  */
  
  public static class TimeBounds implements Cloneable {
    
    private int min_;
    private int max_;
    private String units_;

    public TimeBounds(int min, int max, String units) {
      this.min_ = min;
      this.max_ = max;
      this.units_ = units;
    }
    
    public Object clone() {
      try {
        TimeBounds retval = (TimeBounds)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }  
     
    public int getMin() {
      return (min_);
    }
    public int getMax() {
      return (max_);
    }
    
    public double getMinDouble() {
      return ((double)min_);
    }
    public double getMaxDouble() {
      return ((double)max_);
    }
    
    public DoubMinMax getDoubleBounds() {
      return (new DoubMinMax((double)min_, (double)max_));
    }
 
    public String getUnits() {
      return (units_);
    }         
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Kids can reject non-strip related intersections:
  */
  
  protected boolean childRejectTip(double worldX) {
    DoubMinMax dmm = axis_.bounds.getDoubleBounds();
    return (worldX < dmm.min) || (worldX > dmm.max);
  }
  
  /***************************************************************************
  **
  ** Handle resize
  */
  
  protected ChartTransform childRefresh(Font mFont, Dimension dim) {
    axis_ = axisCalc(mFont, frc_, dataTimeRange_, dim);
    ChartTransform transform = buildTransform(axis_.bounds.getDoubleBounds(), dim);
    return (transform);
  }  

  /***************************************************************************
  **
  ** Drawing core
  */
  
  protected void drawingGuts(Graphics2D g2) {
    super.drawingGuts(g2);
    BasicStroke selectedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    Dimension dim = getSize();
    drawDataFocus(g2, transform_, axis_.bounds, dim, selectedStroke, focusTimeRange_);
    drawAllStrips(g2, dim, axis_.bounds.getDoubleBounds());
    drawAxis(g2, transform_, axis_, dim, selectedStroke);    
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Draw the horizontal axis and graph frame
  */
  
  private void drawAxis(Graphics2D g2, ChartTransform transform, Axis axis, 
                        Dimension dim, BasicStroke selectedStroke) {
    Font mFont = dacx_.getFontManager().getFixedFont(FontManager.STRIP_CHART_AXIS);
    GeneralPath path = new GeneralPath();
    
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    boolean isSuffix = tad.unitsAreASuffix();
    
    int x1 = AXIS_MARGIN_;
    int x2 = dim.width - AXIS_MARGIN_;
    int y1 = dim.height - VERTICAL_MARGIN_ - AXIS_SIZE_;
    int y2 = VERTICAL_MARGIN_ - BUFFER_SIZE_;    
    path.moveTo(x1, y1);
    path.lineTo(x2, y1);
    path.lineTo(x2, y2);
    path.lineTo(x1, y2);    
    path.closePath();
    g2.setStroke(selectedStroke);
    g2.setPaint(Color.black);
    g2.setFont(mFont);
    g2.draw(path);
        
    StringBuffer ticBuf = new StringBuffer();
    double labY = Double.NEGATIVE_INFINITY;
    // tics
    double currTic = axis.bounds.getMinDouble();
    int ticLen = AXIS_SIZE_ / 2;
    while (currTic <= axis.bounds.getMaxDouble()) {
      int ticX = convertToScreenX(currTic, transform);
      path.moveTo(ticX, y1);
      path.lineTo(ticX, y1 + ticLen);
      g2.draw(path);
      
      double floor = Math.floor(currTic);
      String ticString = null;
      if (tad.haveNamedStages()) {
        if (floor == currTic) {  // tic is an integer....
          int ticIndex = (int)floor;
          if (tad.haveNamedStageForIndex(ticIndex)) {
            ticString = tad.getNamedStageForIndex(ticIndex).name;
          }
        } // else we cannot show a label.
      } else {
        ticString = (floor == currTic) ? Long.toString((long)floor) : Double.toString(currTic);
      }
 
      if (ticString != null) {
        ticBuf.setLength(0);
        if (!isSuffix) {
          ticBuf.append(axis.bounds.getUnits());
          ticBuf.append(" ");
        }
        ticBuf.append(ticString);
        if (isSuffix) {
          ticBuf.append(" ");
          ticBuf.append(axis.bounds.getUnits());
        }
        ticString = ticBuf.toString();

        //
        // Place tic label:
        //
        Rectangle2D bounds = mFont.getStringBounds(ticString, frc_);
        double tWidth = bounds.getWidth();
        double labX = (double)ticX - (tWidth / 2.0);
        if (labY == Double.NEGATIVE_INFINITY) {
          double tHeight = bounds.getHeight() * HEIGHT_HACK_;
          labY = y1 + AXIS_SIZE_ + tHeight;
        }
        g2.drawString(ticString, (float)labX, (float)labY);
      }
      currTic += axis.tic;
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Draw the data focus frame
  */
  
  private void drawDataFocus(Graphics2D g2, ChartTransform transform, TimeBounds displayRange, 
                             Dimension dim, BasicStroke selectedStroke, TimeBounds focusRange) {   
    GeneralPath path = new GeneralPath();
    
    int x1 = AXIS_MARGIN_;
    int x2 = dim.width - AXIS_MARGIN_;
    int y1 = dim.height - VERTICAL_MARGIN_ - AXIS_SIZE_;
    int y2 = VERTICAL_MARGIN_ - BUFFER_SIZE_;    

    g2.setStroke(selectedStroke);
    g2.setPaint(new PatternPaint(PatternPaint.FORWARD_DIAGONAL_LINES, vlg_));

    if (focusRange.getMinDouble() > displayRange.getMinDouble()) {
      int minX = convertToScreenX(focusRange.getMinDouble(), transform);      
      path.moveTo(x1, y1);
      path.lineTo(minX, y1);
      path.lineTo(minX, y2);
      path.lineTo(x1, y2);    
      path.closePath();
      g2.fill(path);
    }
    
    if (focusRange.getMaxDouble() < displayRange.getMaxDouble()) {
      int maxX = convertToScreenX(focusRange.getMaxDouble(), transform);
      path.reset();
      path.moveTo(maxX, y1);
      path.lineTo(x2, y1);
      path.lineTo(x2, y2);
      path.lineTo(maxX, y2);    
      path.closePath();
      g2.fill(path);
    } 
    return;
  }

  /***************************************************************************
  **
  ** Calculate axis
  */
  
  private Axis axisCalc(Font mFont, FontRenderContext frc, TimeBounds dataBounds, Dimension dim) {
  
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();    
    
    String biggestLabel = "";    
    if (tad.haveNamedStages()) {
      // Assume longest string is biggest (maybe not, depends on font rendering)
      int maxLen = Integer.MIN_VALUE;
      for (int i = dataBounds.getMin(); i <= dataBounds.getMax(); i++) {
        if (tad.haveNamedStageForIndex(i)) {
          String nextStage = tad.getNamedStageForIndex(i).name;
          int nsLen = nextStage.length();
          if (nsLen > maxLen) {
            maxLen = nsLen;
            biggestLabel = nextStage;
          }
        }
      }
    } else {
      // Figure out the biggest tic label size (assume the maximum, i.e. no negative numbers)
      double floor = Math.floor(dataBounds.getMaxDouble());
      biggestLabel = (floor == dataBounds.getMaxDouble()) ? Long.toString((long)floor) 
                                                          : Double.toString(dataBounds.getMaxDouble());
    }
    
    boolean isSuffix = tad.unitsAreASuffix();
    StringBuffer ticBuf = new StringBuffer();
    ticBuf.setLength(0);
    if (!isSuffix) {
      ticBuf.append(dataBounds.getUnits());
      ticBuf.append(" ");
    }
    ticBuf.append(biggestLabel);
    if (isSuffix) {
      ticBuf.append(" ");
      ticBuf.append(dataBounds.getUnits());
    }
    biggestLabel = ticBuf.toString();
    Rectangle2D bounds = mFont.getStringBounds(biggestLabel, frc);
    double tWidth = bounds.getWidth();
    tWidth += TIC_PADDING_; 
    
    // Figure out the width we are working with, and thus the maximum number of tics:
    // Subtract tWidth to account for tics at start and end:
    
    double workingWidth = (double)(dim.width - (2 * AXIS_MARGIN_)) - tWidth;
    int maxTics = (int)Math.floor(workingWidth / tWidth);
    
    //
    // Figure out the tic candidates we can work with.  Record the difference from 
    // the optimum delta. Take the smallest tic that provides the smallest remainder.
    //

    double axisDelta = dataBounds.getMaxDouble() - dataBounds.getMinDouble();
    TreeMap<Double, SortedSet<Integer>> ticsPerRemainder = new TreeMap<Double, SortedSet<Integer>>();
    int numCandidates = ticCandidates_.length;
    for (int i = 0; i < numCandidates; i++) {
      int ticCand = ticCandidates_[i];
      double floorTic = UiUtil.forceToGridValueMin(dataBounds.getMinDouble(), (double)ticCand);
      double ceilTic = UiUtil.forceToGridValueMax(dataBounds.getMaxDouble(), (double)ticCand);
      double axisFullDelta = ceilTic - floorTic;
      int numTics = (int)axisFullDelta / ticCand;
      if (numTics > maxTics) {
        continue;
      }
      double deltaDiff = axisFullDelta - axisDelta;
      Double deltaKey = new Double(deltaDiff);
      SortedSet<Integer> set = ticsPerRemainder.get(deltaKey);
      if (set == null) {
        set = new TreeSet<Integer>();
        ticsPerRemainder.put(deltaKey, set);
      }
      set.add(new Integer(ticCand));
    }
    
    int ticVal;
    Double lokey = ticsPerRemainder.firstKey();
    if (lokey == null) {
      ticVal = ticCandidates_[ticCandidates_.length - 1];
    } else {
      SortedSet<Integer> set = ticsPerRemainder.get(lokey);
      Integer firstTic = set.first();
      ticVal = firstTic.intValue();
    }
    
    double axisFloor = UiUtil.forceToGridValueMin(dataBounds.getMinDouble(), (double)ticVal);
    double axisCeil = UiUtil.forceToGridValueMax(dataBounds.getMaxDouble(), (double)ticVal);    
    TimeBounds axisBounds = new TimeBounds((int)axisFloor, (int)axisCeil, dataBounds.getUnits());
    Axis retval = new Axis(axisBounds, ticVal);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Build X coordinate transform
  */
  
  private ChartTransform buildTransform(DoubMinMax bounds, Dimension dim) {
    double slope = ((double)(dim.width - (2 * AXIS_MARGIN_))) / (bounds.max - bounds.min);
    return (new ChartTransform(slope, AXIS_MARGIN_, bounds.min));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Defines axis
  */
  
  private class Axis {
    
    TimeBounds bounds;
    int tic;

    Axis(TimeBounds bounds, int tic) {
      this.bounds = bounds;
      this.tic = tic;
    }
  }   
}
