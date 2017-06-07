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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.util.Iterator;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.TreeMap;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;

/****************************************************************************
**
** Dialog box for setting region topologies
*/

public class RegionTopologyDialog extends JDialog implements ChangeListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTabbedPane tabPane_;
  private HashMap<TimeCourseData.TopoTimeRange, TabData> tabData_;
  private TimeCourseData.TopoRegionLocator topoLocs_;
  private ArrayList<TimeCourseData.TopoTimeRange> tabIndex_;
  private List<Integer> currTimes_;
  private List<Integer> requiredTimes_;
  private TabData currTabData_;
  private FixedJButton deleteButton_;
  private boolean rebuilding_;
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Wrapper on constructor.  Have to define time axis before we can define
  ** topo structure.
  */ 
  
  public static RegionTopologyDialog regionTopoDialogWrapper(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac) {
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics, dacx, uFac);
      tasd.setVisible(true);
    }
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = dacx.getRMan();
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("tcsedit.noTimeDefinition"), 
                                    rMan.getString("tcsedit.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }
    
    RegionTopologyDialog rtd = new RegionTopologyDialog(uics, dacx, uFac);
    return (rtd);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private RegionTopologyDialog(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac) {    
    super(uics.getTopFrame(), dacx.getRMan().getString("regionTopo.title"), true);    
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    ResourceManager rMan = dacx_.getRMan();    
    setSize(640, 520);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new BorderLayout());
    tabPane_ = new JTabbedPane();
    rebuilding_ = false;

    tabData_ = new HashMap<TimeCourseData.TopoTimeRange, TabData>();
    cp.add(tabPane_, BorderLayout.CENTER);
    registerKeys(cp);
    tabIndex_ = new ArrayList<TimeCourseData.TopoTimeRange>();
    requiredTimes_ = findRequiredTabs();
    
    //
    // Build the position panel tabs:
    //

    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    Iterator<TimeCourseData.TopoTimeRange> timeit = tcd.getRegionTopologyTimes();    
    topoLocs_ = tcd.getRegionTopologyLocator().clone();
    currTimes_ = new ArrayList<Integer>();

    while (timeit.hasNext()) {
      TimeCourseData.TopoTimeRange ttr = timeit.next();
      TimeCourseData.RegionTopology regTopo = tcd.getRegionTopology(ttr);
      TabData ctd = new TabData();
      RegionTopologyPanel tabPanel = buildATab(ctd, regTopo, null, topoLocs_);
      tabPane_.addTab(ttr.toString(), tabPanel);
      currTimes_.add(Integer.valueOf(ttr.minTime));
      if (!timeit.hasNext()) {
        currTimes_.add(Integer.valueOf(ttr.maxTime));        
      }
      tabData_.put(ttr, ctd);
      tabIndex_.add(ttr);
    }
    tabPane_.addChangeListener(this);
    if (!tabIndex_.isEmpty()) {
      TimeCourseData.TopoTimeRange currReg = tabIndex_.get(0);
      currTabData_ = tabData_.get(currReg);
    }
    
    //
    // Build the button panel:
    //
    
    FixedJButton buttonT = new FixedJButton(rMan.getString("regionTopo.redoTabs"));
    buttonT.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try { 
          cancelAddMode();
          if (currTabData_ != null) {
            currTabData_.rtPanel.clearSelections();
          }
          RegionTopoTabSetupDialog rttsd = new RegionTopoTabSetupDialog(uics_, dacx_, currTimes_, requiredTimes_);
          rttsd.setVisible(true);
          if (rttsd.haveChanges()) {
            reformatTabs(rttsd.getNewTimes());
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }); 
    
    deleteButton_ = new FixedJButton(rMan.getString("dialogs.delete"));
    deleteButton_.setEnabled(false);
    deleteButton_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          deleteSelected();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });    
 
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (applyProperties()) {          
            RegionTopologyDialog.this.setVisible(false);
            RegionTopologyDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          RegionTopologyDialog.this.setVisible(false);
          RegionTopologyDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(buttonT);
    buttonPanel.add(Box.createHorizontalStrut(10));     
    buttonPanel.add(deleteButton_);
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    buttonPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.add(buttonPanel, BorderLayout.SOUTH); 
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  /***************************************************************************
  **
  ** Cancel current draw
  */ 
  
  private void cancelAddMode() {
    if (currTabData_ != null) {
      currTabData_.rtPanel.cancelAddMode();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Delete selected link
  */ 
  
  private void deleteSelected() {
    if (currTabData_ != null) {
      currTabData_.rtPanel.deleteSelected();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Track which tab is visible 
  */
  
  public void stateChanged(ChangeEvent e) {
    if (rebuilding_) {
      return;
    }
    cancelAddMode();
    if (currTabData_ != null) {
      currTabData_.rtPanel.clearSelections();
    }
    int currIndex = tabPane_.getSelectedIndex();
    Object currTab = tabIndex_.get(currIndex);
    currTabData_ = tabData_.get(currTab);
    return;
  }
  
  /***************************************************************************
  **
  ** Track if we have an active selection
  */
  
  public void activeSelection(boolean haveSelection) {
    deleteButton_.setEnabled(haveSelection);
    return;
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
 /***************************************************************************
  **
  ** Handle installation
  */ 
  
  private boolean applyProperties() {    
  
    UndoSupport support = uFac_.provideUndoSupport("undo.regTopoedit", dacx_);
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();

    TreeMap<TimeCourseData.TopoTimeRange, TimeCourseData.RegionTopology> newTopologies = 
      new TreeMap<TimeCourseData.TopoTimeRange, TimeCourseData.RegionTopology>(); 
    Iterator<TimeCourseData.TopoTimeRange> timeit = tabData_.keySet().iterator();
    while (timeit.hasNext()) {
      TimeCourseData.TopoTimeRange ttr = timeit.next();
      TabData tabDat = tabData_.get(ttr);
      newTopologies.put(ttr, tabDat.regTopo);
    }

    TimeCourseChange tcc = tcd.setRegionTopologiesInfo(newTopologies, topoLocs_);

    if (tcc != null) {
      support.addEdit(new TimeCourseChangeCmd(dacx_, tcc));
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();
    }

    return (true);
  }  
      
  /***************************************************************************
  **
  ** Handle keystroke setup
  */ 
  
  private void registerKeys(JComponent cp) {  
    cp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "BioTapTopoCancel");
    cp.getActionMap().put("BioTapTopoCancel", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(ActionEvent e) {
        try {
          cancelAddMode();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    ((JComponent)cp).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DELETE"), "BioTapTopoDelete");
    ((JComponent)cp).getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("BACK_SPACE"), "BioTapDelete");
     ((JComponent)cp).getActionMap().put("BioTapTopoDelete", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(ActionEvent e) {
        try {
          deleteSelected();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    return;
  }
  
  /***************************************************************************
  **
  ** Data per tab
  */ 
  
  private class TabData {
    TimeCourseData.RegionTopology regTopo;
    RegionTopologyPanel rtPanel;
       
    TabData() {
    }
  }
  
  /***************************************************************************
  **
  ** Build a tab for a region
  */ 
  
  private RegionTopologyPanel buildATab(TabData td, TimeCourseData.RegionTopology topo,
                                        TimeCourseData.TopoTimeRange newTtr,
                                        TimeCourseData.TopoRegionLocator locs) {
    td.regTopo = topo.clone();
    if (newTtr != null) {
      td.regTopo.times = newTtr;
    }
    td.rtPanel = new RegionTopologyPanel(uics_, dacx_, this, td.regTopo, locs);
    return (td.rtPanel);
  }  
  
  /***************************************************************************
  **
  ** Reorganize tabs
  */ 
  
  private void reformatTabs(List<Integer> tabBreaks) {
    HashMap<TimeCourseData.TopoTimeRange, TabData> oldData = tabData_;
    tabData_ = new HashMap<TimeCourseData.TopoTimeRange, TabData>();
    tabIndex_ = new ArrayList<TimeCourseData.TopoTimeRange>();
    rebuilding_ = true;
    tabPane_.removeAll();
    currTimes_.clear();
    TimeCourseData.TopoRegionLocator newLocs = new TimeCourseData.TopoRegionLocator();    
    
    //
    // Build the new time ranges:
    //
    
    ArrayList<TimeCourseData.TopoTimeRange> newRanges = new ArrayList<TimeCourseData.TopoTimeRange>();
    int num = tabBreaks.size();
    int lastTime = -1;
    for (int i = 0; i < num; i++) {
      Integer tabBreak = tabBreaks.get(i);
      int currTime = tabBreak.intValue();
      if (i != 0) {
        int maxTime = (i == (num - 1)) ? currTime : currTime - 1;
        TimeCourseData.TopoTimeRange ttr = new TimeCourseData.TopoTimeRange(dacx_, lastTime, maxTime);
        newRanges.add(ttr);
      }
      lastTime = currTime;   
    }
    
    //
    // For new slices, we copy the state of the existing topology at that slice.  Also transfer
    // layout info
    //

    int numNR = newRanges.size();
    for (int i = 0; i < numNR; i++) {
      TimeCourseData.TopoTimeRange ttr = newRanges.get(i);
      Iterator<TimeCourseData.TopoTimeRange> timeit = (new TreeSet<TimeCourseData.TopoTimeRange>(oldData.keySet())).iterator();
      while (timeit.hasNext()) {
        TimeCourseData.TopoTimeRange ttrCheck = timeit.next();
        if (ttrCheck.maxTime > ttr.minTime) {
          TabData oldTab = oldData.get(ttrCheck);
          TimeCourseData.RegionTopology regTopo = oldTab.regTopo;
          TabData ctd = new TabData();
          RegionTopologyPanel tabPanel = buildATab(ctd, regTopo, ttr, newLocs);
          tabPane_.addTab(ttr.toString(), tabPanel);
          currTimes_.add(new Integer(ttr.minTime));
          if (i == (numNR - 1)) {
            currTimes_.add(new Integer(ttr.maxTime));        
          }
          tabData_.put(ttr, ctd);
          tabIndex_.add(ttr);
          Iterator<TimeCourseData.TopoTimeRange> currLocs = topoLocs_.getRegionTopologyTimes();
          while (currLocs.hasNext()) {
            TimeCourseData.TopoTimeRange ttrCheckLoc = currLocs.next();
            if (ttrCheckLoc.maxTime > ttr.minTime) {
              newLocs.transferTopologyLocations(topoLocs_, ttrCheckLoc, ttr);
              break;
            }
          }
          break;
        }
      }
    }
    
    //
    // Finish
    //
    
    topoLocs_ = newLocs;
    if (!tabIndex_.isEmpty()) {
      TimeCourseData.TopoTimeRange currReg = tabIndex_.get(0);
      currTabData_ = tabData_.get(currReg);
    }
    rebuilding_ = false;
    tabPane_.invalidate();
    validate();
    return;
  }  
  
  /***************************************************************************
  **
  ** Find the required tabs
  */ 
  
  private List<Integer> findRequiredTabs() {    
    //
    // Times that reflect a change in the number and ID of the regions
    // cannot be removed.  Figure these out:
    //    
    ArrayList<Integer> retval = new ArrayList<Integer>();
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    Iterator<TimeCourseData.TopoTimeRange> timeit = tcd.getRegionTopologyTimes();    

    ArrayList<String> lastRegions = null;
    while (timeit.hasNext()) {
      TimeCourseData.TopoTimeRange ttr = timeit.next();
      TimeCourseData.RegionTopology regTopo = tcd.getRegionTopology(ttr);
      ArrayList<String> currRegions = new ArrayList<String>(regTopo.regions);
      Collections.sort(currRegions);
      if (lastRegions != null) {
        if (!lastRegions.equals(currRegions)) {
          retval.add(new Integer(ttr.minTime));
        }
      } else {
        retval.add(new Integer(ttr.minTime));
      }
      if (!timeit.hasNext()) {
        retval.add(new Integer(ttr.maxTime));          
      } 
      lastRegions = currRegions;
    }    

    return (retval);
  }  
  
}
