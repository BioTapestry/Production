/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JPanel;
import java.awt.event.MouseEvent;

/****************************************************************************
**
** The multi strip chart wrapper
*/

public class MultiStripChartWrapper extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private MultiStripChart chart_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Null constructor
  */
  
  public MultiStripChartWrapper() {
    setLayout(new GridLayout(1, 1));
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

 /***************************************************************************
  **
  ** Get the chart
  */
  
  public MultiStripChart getChart() {
    return (chart_);    
  }   
  
  /***************************************************************************
  **
  ** Set the chart
  */
  
  public void setChart(MultiStripChart msc) {
    removeAll();
    add(msc);
    chart_ = msc;
    return;    
  }    
   
  /***************************************************************************
  **
  ** Get the tool tip
  */
  
  public String getToolTipText(MouseEvent event) {
    chart_.getToolTipText();
    return (null);    
  }    
   
  /***************************************************************************
  **
  ** Set the strips to display
  */
  
  public void setStrips(List strips, List prunedStrips) {
    chart_.setStrips(strips, prunedStrips);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMinimumSize() {
    return (chart_.getPreferredSize());
  }  
  
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMaximumSize() {
    return (chart_.getPreferredSize());
  }    
 
  /***************************************************************************
  **
  ** Get the preferred size
  */
  
  public Dimension getPreferredSize() {
    return (chart_.getPreferredSize());
  }
}
