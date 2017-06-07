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
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;

/****************************************************************************
**
** A stack of multi-strip charts
*/

public class ChartStack extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<StackElement> charts_;
  private JPanel childPanel_;
  private ChartStackLayoutManager lom_;
  private JPanel spacer_;
  private JTextField feedback_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Null constructor
  */
  
  public ChartStack(UIComponentSource uics, DataAccessContext dacx, JTextField feedback) {
    uics_ = uics;
    dacx_ = dacx;
    feedback_ = feedback;
    charts_ = new ArrayList<StackElement>();
    childPanel_ = new JPanel();
    childPanel_.setBorder(BorderFactory.createLineBorder(Color.black, 2));
    lom_ = new ChartStackLayoutManager();
    childPanel_.setLayout(lom_);
    JScrollPane jsp = new JScrollPane(childPanel_);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    setLayout(new GridLayout(1,1));
    add(jsp);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the stack disabled
  */
  
  public void disableStack() {
    int currCount = charts_.size();
    for (int i = 0; i < currCount; i++) {
      StackElement element = charts_.get(i);
      element.disableElement();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the stack enabled
  */
  
  public void enableStack() {
    int currCount = charts_.size();
    for (int i = 0; i < currCount; i++) {
      StackElement element = charts_.get(i);
      element.enableElement();
    }
    return;
  }
  

  /***************************************************************************
  **
  ** Get components unregistered
  */
  
  public void cleanCharts() {
    int currCount = charts_.size();
    for (int i = 0; i < currCount; i++) {
      StackElement element = charts_.get(i);
      ToolTipManager.sharedInstance().unregisterComponent(element.getTooltipComponent());
      MultiStripChart msc = element.msc.getChart();
      if (element.handler != null) {
        msc.removeMouseListener(element.handler);
      }
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Get the current chart visibility
  */
  
  public Map<String, Boolean> getCurrentViz() {
    HashMap<String, Boolean> retval = new HashMap<String, Boolean>();
    int currCount = charts_.size();
    for (int i = 0; i < currCount; i++) {
      StackElement element = charts_.get(i);
      retval.put(element.titleBar.getKey(), new Boolean(element.titleBar.isOpen()));
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Set the charts to display
  */
  
  public void setCharts(Collected dataCharts, 
                        MultiStripTimeAxisChart.TimeBounds focusTimes, 
                        MultiStripTimeAxisChart.TimeBounds dataTimes,
                        int coverage,
                        Map<String, Boolean> currentViz) {
    
    int nsinc = (dataCharts.neighborSrc == null) ? 0 : 1;
    
    int numDC = dataCharts.stripCharts.size();
    setChartCount(numDC, nsinc);
    for (int i = 0; i < numDC; i++) {
      StackElement element = charts_.get(i);
      DataSet data = dataCharts.stripCharts.get(i);
      element.titleBar.setTitle(data.title);
      element.titleBar.setKey(data.uniqueKey);
      MultiStripChart msc = element.msc.getChart();
      boolean needTime = data.hasTimeAxis; 
      if (needTime) {
        if (!((msc != null) && (msc instanceof MultiStripTimeAxisChart))) {
          if (msc != null) {
            ToolTipManager.sharedInstance().unregisterComponent(msc);       
            if (element.handler != null) msc.removeMouseListener(element.handler);
          }
          msc = new MultiStripTimeAxisChart(dacx_);
          element.msc.setChart(msc);
          ToolTipManager.sharedInstance().registerComponent(msc);
          if (element.handler != null) msc.addMouseListener(element.handler);
        }
      } else {
        if (!((msc != null) && (msc instanceof MultiStripGenericChart))) {
          if (msc != null) {
            ToolTipManager.sharedInstance().unregisterComponent(msc);
            if (element.handler != null) msc.removeMouseListener(element.handler);
          }
          msc = new MultiStripGenericChart(dacx_);
          element.msc.setChart(msc);
          ToolTipManager.sharedInstance().registerComponent(msc);
          if (element.handler != null) msc.addMouseListener(element.handler);
        }    
      }
      msc.setStrips(data.strips, data.prunedStrips);
      if (msc instanceof MultiStripTimeAxisChart) {
        MultiStripTimeAxisChart tac = (MultiStripTimeAxisChart)msc;
        tac.setDataTimeRange(dataTimes);
        tac.setFocusTimeRange(focusTimes);  
      }
      msc.setCoverage(coverage);
    }
    //
    // The neighbor network display
    //
    if (nsinc == 1) {
      StackElement element = charts_.get(charts_.size() - 1);
      element.titleBar.setTitle(dataCharts.batchDataTitle);
      element.titleBar.setKey(dataCharts.batchDataKey);
      element.csmv.setSources(dataCharts.neighborSrc, 
                              new LocalLayoutSource(dataCharts.neighborLo, dataCharts.neighborSrc), 
                              dataCharts.batchData);
    }
    
    //
    // Reset visibility:
    //
    
    if (currentViz != null) {    
      int currCount = charts_.size();
      for (int i = 0; i < currCount; i++) {
        StackElement element = charts_.get(i);
        String barKey = element.titleBar.getKey();
        Boolean prevViz = currentViz.get(barKey);
        if (prevViz != null) {
          element.titleBar.setOpen(prevViz.booleanValue());
        }
      }
    }      
    invalidate();
    validate();
    repaint();
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Defines a DataSet
  */  
      
  public static class DataSet {
    
    public String title;
    public List<MultiStripChart.Strip> strips;
    public List<MultiStripChart.Strip> prunedStrips;
    public boolean hasTimeAxis;
    public String uniqueKey;
    
    public DataSet(String title, List<MultiStripChart.Strip> strips, List<MultiStripChart.Strip> prunedStrips, String uniqueKey, boolean hasTimeAxis) {
      this.title = title;
      this.strips = strips;
      this.prunedStrips = prunedStrips;
      this.hasTimeAxis = hasTimeAxis;
      this.uniqueKey = uniqueKey;
    }
  }
  
  /***************************************************************************
  **
  ** Used to gather up data 
  */
  
  public static class Collected {
    public List<DataSet> stripCharts;
    public String batchDataTitle;
    public String batchDataKey;
    // ***Still trying to track down the Object types!
    public Map<Link, Map<String, List<Object>>> batchData;
    public GenomeSource neighborSrc; 
    public Layout neighborLo;
    
    public Collected(List<DataSet> stripCharts) {
      this.stripCharts = stripCharts;
      this.batchDataTitle = null;
      this.batchDataKey = null;
      this.batchData = null;
      this.neighborSrc = null;
      this.neighborLo = null;
    }
        
    public Collected(List<DataSet> stripCharts, Map<Link, Map<String, List<Object>>> batchData, String batchDataTitle, String batchDataKey, 
                     GenomeSource neighborSrc, Layout neighborLo) {
      this.stripCharts = stripCharts;
      this.batchDataTitle = batchDataTitle;
      this.batchDataKey = batchDataKey;
      this.batchData = batchData;
      this.neighborSrc = neighborSrc;
      this.neighborLo = neighborLo;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the correct number of charts installed.  The last chart is the
  ** neighbor net display.
  */
  
  private void setChartCount(int count, int modelCount) {
    if (spacer_ != null) {
      childPanel_.remove(spacer_);
    }
 
    int currCount = charts_.size();
    int mscCount = 0;
    int csmvCount = 0;
    for (int i = 0; i < currCount; i++) {
      StackElement element = charts_.get(i);
      if (element.msc != null) {
        mscCount++;
      } else {
        csmvCount++;
      }// index set later      
    }
     
    if (mscCount < count) {
      addMsc(count - mscCount);
    } else if (mscCount > count) {
      dropMsc(mscCount - count);
    }
    
    if (csmvCount < modelCount) {
      addCsmv(modelCount - csmvCount);
    } else if (csmvCount > modelCount) {
      dropCsmv(csmvCount - modelCount);
    }
    
    //
    // Get the spacer in:
    //
    
    int num = childPanel_.getComponentCount();
    if (spacer_ == null) {
      spacer_ = new JPanel();
      spacer_.setMinimumSize(new Dimension(2, 2));
      spacer_.setBorder(new ChartStackTitleBar.ChartBorder(2));
    }
    childPanel_.add(spacer_, new ChartStackLayoutManager.StackRole(true, num));
      
    //
    // Need to get layout manager informed
    //
    Component[] foo = childPanel_.getComponents(); 
    for (int i = 0; i < foo.length; i++) {
      Component comp = foo[i];
      ChartStackLayoutManager.StackRole role = lom_.getConstraint(comp);
      role.position = i / 2;
    }
   
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the correct number of charts installed.  The last chart is the
  ** neighbor net display.
  */
  
  private void addMsc(int addCount) {
    for (int i = 0; i < addCount; i++) {
      MultiStripChartWrapper msc = new MultiStripChartWrapper();
      ChartStackTitleBar bar = new ChartStackTitleBar("", this, msc, null, uics_.getHandlerAndManagerSource());
      StackElement element = new StackElement(bar, msc, new MouseHandler());
      //ToolTipManager.sharedInstance().registerComponent(msc.getChart());        
      charts_.add(0, element);
      // Doing it backwards, since inserting at zero:
      ChartStackLayoutManager.StackRole role = 
        new ChartStackLayoutManager.StackRole(false, 0, null); // index set later      
      childPanel_.add(msc, role, 0);
      role = new ChartStackLayoutManager.StackRole(true, 0, msc); // index set later      
      childPanel_.add(element.titleBar, role, 0);
    }
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the correct number of CSMV panels added
  */
  
  private void addCsmv(int addCount) {
    for (int i = 0; i < addCount; i++) {
      ChartStackableModelView csmv = new ChartStackableModelView(uics_, dacx_);
      ChartStackTitleBar bar = new ChartStackTitleBar("", this, csmv, null, uics_.getHandlerAndManagerSource());
      StackElement element = new StackElement(bar, csmv);   
      ToolTipManager.sharedInstance().registerComponent(csmv.getToolTipTarget());        
      charts_.add(element);
      ChartStackLayoutManager.StackRole role = 
        new ChartStackLayoutManager.StackRole(true, 0, csmv); // index set later      
      childPanel_.add(element.titleBar, role);
      role = new ChartStackLayoutManager.StackRole(false, 0, null);  // index set later         
      childPanel_.add(csmv, role);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Drop the given number of MSC elements
  */
  
  private void dropMsc(int dropCount) {
    for (int i = 0; i < dropCount; i++) {
      StackElement element = charts_.remove(0);
      childPanel_.remove(element.titleBar);
      if (element.msc == null) {
        throw new IllegalStateException();
      }
      childPanel_.remove(element.msc);
      ToolTipManager.sharedInstance().unregisterComponent(element.msc.getChart());
      MultiStripChart chart = element.msc.getChart();
      if (element.handler != null) {
        chart.removeMouseListener(element.handler);
      }
    } 
    return;
  }       
  
  /***************************************************************************
  **
  ** Drop the given number of CSMV elements
  */
  
  private void dropCsmv(int dropCount) {
    for (int i = 0; i < dropCount; i++) {
      StackElement element = charts_.remove(charts_.size() - 1);
      childPanel_.remove(element.titleBar);
      if (element.csmv == null) {
        throw new IllegalStateException();
      }
      childPanel_.remove(element.csmv);
      ToolTipManager.sharedInstance().unregisterComponent(element.csmv);    
    } 
    return;
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Defines a chart element
  */  
      
  private static class StackElement {
    
    public ChartStackTitleBar titleBar;
    public MultiStripChartWrapper msc;
    public ChartStackableModelView csmv;
    public MouseHandler handler;
  
    public StackElement(ChartStackTitleBar titleBar, MultiStripChartWrapper msc, MouseHandler handler) {
      this.titleBar = titleBar;
      this.msc = msc;
      this.csmv = null;
      this.handler = handler;
    }
    
    public StackElement(ChartStackTitleBar titleBar, ChartStackableModelView csmv) {
      this.titleBar = titleBar;
      this.msc = null;
      this.csmv = csmv;
    }
    
    public void disableElement() {
      titleBar.disableBar();
      return;
    }
     
    public void enableElement() {
      titleBar.enableBar();
      return;
    }
    
    public JComponent getTooltipComponent() {
      return ((msc == null) ? csmv.getToolTipTarget() : msc.getChart());
    }
      
  }
  
  /***************************************************************************
  **
  ** Handles click events
  */  
  
  public class MouseHandler extends MouseAdapter {
    public void mouseReleased(MouseEvent me) {
      try {
        String tttext = ((JComponent)me.getSource()).getToolTipText(me);
        feedback_.setText(tttext);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
    }
  }
}
