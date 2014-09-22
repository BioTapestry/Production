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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Font;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.DoubMinMax;

/****************************************************************************
**
** The multi strip chart
*/

public class MultiStripGenericChart extends MultiStripChart {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final int MARGIN_ = 30;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  protected DoubMinMax bounds_;
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
  
  public MultiStripGenericChart(BTState appState) {
    super(appState);
    bounds_ = new DoubMinMax(0.0, 100.0);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set coverage
  */
  
  public void setCoverage(int coverage) {
    coverage_ = coverage;
    boundsCalc();
    super.setCoverage(coverage);
    return;
  }
    
  /***************************************************************************
  **
  ** Set the strips to display
  */
  
  public void setStrips(List<MultiStripChart.Strip> strips, List<MultiStripChart.Strip> prunedStrips) {
    super.setStrips(strips, prunedStrips);
    boundsCalc();
    return;
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
    return (false);
  }

  /***************************************************************************
  **
  ** Handle resize
  */
  
  protected ChartTransform childRefresh(Font mFont, Dimension dim) {
    return (buildTransform(bounds_, dim));
  } 
  
  /***************************************************************************
  **
  ** Drawing core
  */
  
  protected void drawingGuts(Graphics2D g2) {
    super.drawingGuts(g2);
    Dimension dim = getSize();
    drawAllStrips(g2, dim, bounds_); 
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Do bounds calcs
  */
  
  private void boundsCalc() {
    int size = useStrips().size();
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    
    for (int i = 0; i < size; i++) {
      Strip intersect = (Strip)useStrips().get(i);
      List<StripElement> elem = intersect.elements;
      int elemSize = elem.size();
      for (int j = 0; j < elemSize; j++) {   
        StripElement se = elem.get(j);
        if (se.min < minX) {
          minX = se.min;
        }
        if (se.max > maxX) {
          maxX = se.max;
        }
      }
    }
    bounds_ = (size == 0) ? new DoubMinMax(0.0, 100.0) :  new DoubMinMax(minX, maxX);  
    return;
  } 
  
  /***************************************************************************
  **
  ** Build X coordinate transform
  */
 
  private ChartTransform buildTransform(DoubMinMax bounds, Dimension dim) {
    double slope = ((double)(dim.width - (2 * MARGIN_))) / (bounds.max - bounds.min);
    return (new ChartTransform(slope, MARGIN_, bounds.min));
  }
}
