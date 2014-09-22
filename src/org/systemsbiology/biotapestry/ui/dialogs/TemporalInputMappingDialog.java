/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalRange;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for choosing Temporal Input mapping
*/

public class TemporalInputMappingDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ListWidget lw_;
  private ListWidget lwnt_;  
  private String nodeID_;
  private boolean cancelled_;
  private TemporalInputRangeData tird_;
  private ArrayList newEntries_;
  private ArrayList newSources_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private ArrayList<String> srcs_;
  private ArrayList ntsrcs_; 
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TemporalInputMappingDialog(BTState appState, DataAccessContext dacx, String nodeName, String nodeID) {     
    super(appState.getTopFrame(), "", true);
    appState_ = appState;
    dacx_ = dacx;
    ResourceManager rMan = appState_.getRMan();
    String format = rMan.getString("timd.title");
    String desc = MessageFormat.format(format, new Object[] {nodeName});    
    this.setTitle(desc);    
    
    nodeID_ = nodeID;
    cancelled_ = false;
    newEntries_ = new ArrayList();
    newSources_ = new ArrayList();
     
    setSize(500, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    tird_ = appState_.getDB().getTemporalInputRangeData();
    List<String> mappedEnt = tird_.getCustomTemporalInputRangeEntryKeys(nodeID);
    if (mappedEnt == null) {
      mappedEnt = new ArrayList<String>();
    }
    mappedEnt = DataUtil.normalizeList(mappedEnt);
    ArrayList<Integer> tSelections = new ArrayList<Integer>();     
        
    //
    // Create a list of the target genes available:
    //
    
    Iterator<TemporalRange> entries = tird_.getEntries();
    srcs_ = new ArrayList<String>();
    int count = 0;
    while (entries.hasNext()) {
      TemporalRange range = entries.next();
      String name = range.getName();      
      srcs_.add(name);
      name = DataUtil.normKey(name);
      if (mappedEnt.contains(name)) {
        tSelections.add(new Integer(count));
      }
      count++;
    }

    String msg = rMan.getString("timd.sources");
    msg = MessageFormat.format(msg, new Object[] {nodeName});    
    JLabel lab = new JLabel(msg);
    UiUtil.gbcSet(gbc, 0, 0, 6, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    cp.add(lab, gbc);

    lw_ = new ListWidget(appState_, srcs_, new ListWidgetWrapper(true), ListWidget.ADD_ONLY_HOLD_SELECT);    
    UiUtil.gbcSet(gbc, 0, 1, 6, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(lw_, gbc);

    List mappedSrc = tird_.getCustomTemporalInputRangeSourceKeys(nodeID);
    if (mappedSrc == null) {
      mappedSrc = new ArrayList();
    }
    mappedSrc = DataUtil.normalizeList(mappedSrc);
    ArrayList sSelections = new ArrayList();    
    
    //
    // Create the list of all sources
    //  
    
    Set nonTarg = tird_.getAllSources();
    ntsrcs_ = new ArrayList();
    Iterator ntit = nonTarg.iterator();
    count = 0;
    while (ntit.hasNext()) {
      String src = (String)ntit.next();
      ntsrcs_.add(src);
      String name = DataUtil.normKey(src);
      if (mappedSrc.contains(name)) {
        sSelections.add(new Integer(count));
        mappedSrc.remove(name);
      }
      count++;
    }

    //
    // Add mapped strings not showing up elsewhere here
    //
    
    Iterator mit = mappedSrc.iterator();
    while (mit.hasNext()) {
      String name = (String)mit.next();
      ntsrcs_.add(name);
      sSelections.add(new Integer(count));
      count++;
    }    
    
    //
    // Add the list widgets:
    //    

    msg = rMan.getString("timd.nonTarg");
    msg = MessageFormat.format(msg, new Object[] {nodeName});    
    lab = new JLabel(msg);
    UiUtil.gbcSet(gbc, 0, 6, 6, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    cp.add(lab, gbc);

    lwnt_ = new ListWidget(appState_, ntsrcs_, new ListWidgetWrapper(false), ListWidget.ADD_ONLY_HOLD_SELECT);    
    UiUtil.gbcSet(gbc, 0, 7, 6, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(lwnt_, gbc);      

    //
    // Set the selections:
    //
   
    int[] select = new int[tSelections.size()];
    for (int i = 0; i < select.length; i++) {
      select[i] = ((Integer)(tSelections.get(i))).intValue();
    }
    lw_.setSelectedIndices(select);    

    select = new int[sSelections.size()];
    for (int i = 0; i < select.length; i++) {
      select[i] = ((Integer)(sSelections.get(i))).intValue();
    }
    lwnt_.setSelectedIndices(select);    
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
          TemporalInputMappingDialog.this.cancelled_ = false;
          TemporalInputMappingDialog.this.setVisible(false);
          TemporalInputMappingDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TemporalInputMappingDialog.this.cancelled_ = true;
          TemporalInputMappingDialog.this.setVisible(false);
          TemporalInputMappingDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 13, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public boolean wasCancelled() {
    return (cancelled_);
  }

  /***************************************************************************
  **
  ** Add a row to the list
  */
  
  public List addTargetRow(ListWidget widget) {
    ResourceManager rMan = appState_.getRMan();
    String newGene = 
      (String)JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                          rMan.getString("timd.newGene"), 
                                          rMan.getString("timd.newGeneTitle"),     
                                          JOptionPane.QUESTION_MESSAGE, null, 
                                          null, null);
    if (newGene == null) {
      return (null);
    }
      
    newGene = newGene.trim();
      
    if (newGene.equals("")) {
      Toolkit.getDefaultToolkit().beep();
      return (null);
    }

    if (DataUtil.containsKey(srcs_, newGene)) {
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("timd.nameNotUnique"), 
                                    rMan.getString("timd.nameNotUniqueTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }
    
    String ngCrush = newGene.toUpperCase().replaceAll(" ", "");
    if (ngCrush.equals("UBQ") || 
        ngCrush.equals("UBIQ") || 
        ngCrush.equals("UBIQUITOUS")) {
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("timd.nameReserved"), 
                                    rMan.getString("timd.nameReservedTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }    
    
    newEntries_.add(newGene);
    srcs_.add(newGene);
    
    return (srcs_);
  }
  
  /***************************************************************************
  **
  ** Add a source row to the list
  */
  
  public List addSourceRow(ListWidget widget) {
    ResourceManager rMan = appState_.getRMan(); 
    String newSource = 
      (String)JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                          rMan.getString("timd.newSource"), 
                                          rMan.getString("timd.newSourceTitle"),
                                          JOptionPane.QUESTION_MESSAGE, null, 
                                          null, null);
    if (newSource == null) {
      return (null);
    }
      
    if (newSource.trim().equals("")) {
      Toolkit.getDefaultToolkit().beep();
      return (null);
    }

    if (DataUtil.containsKey(ntsrcs_, newSource)) {
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("timd.nameNotUnique"), 
                                    rMan.getString("timd.nameNotUniqueTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }    
    
    newSources_.add(newSource);
    ntsrcs_.add(newSource);
    
    return (ntsrcs_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  
  private class ListWidgetWrapper implements ListWidgetClient {
    private boolean forTarget_;
    
    /***************************************************************************
    **
    ** Constructor 
    */ 
  
    public ListWidgetWrapper(boolean forTarget) {
      forTarget_ = forTarget;
    }
    
    /***************************************************************************
    **
    ** Add a row to the list
    */
  
    public List addRow(ListWidget widget) {
      if (forTarget_) {
        return (TemporalInputMappingDialog.this.addTargetRow(widget));
      } else {
        return (TemporalInputMappingDialog.this.addSourceRow(widget));
      }
    }
  
    /***************************************************************************
    **
    ** Delete rows from the ORDERED list
    */
  
    public List deleteRows(ListWidget widget, int[] rows) {
      // Not used
      return (null);
    }

    /***************************************************************************
    **
    ** Edit a row from the list
    */
  
    public List editRow(ListWidget widget, int[] selectedRows) {
      // NOT USED
      return (null);
    }
  
    /***************************************************************************
    **
    ** Merge rows from the list
    */
  
    public List combineRows(ListWidget widget, int[] selectedRows) {
      // not used
      return (null);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply our UI values to the temporal input map
  ** 
  */

  private void applyProperties() {
    
    UndoSupport support = new UndoSupport(appState_, "undo.timd");

    //
    // Add all selected objects to new map list:
    //

    Object[] values = lw_.getSelectedObjects();
    ArrayList elist = new ArrayList();
    ArrayList slist = new ArrayList();
    for (int i = 0; i < values.length; i++) {
      elist.add(values[i]);
    }  
   
    //
    // Create new ranges for new objects:
    //
    
    Iterator newit = newEntries_.iterator();
    while (newit.hasNext()) {
      String newEnt = (String)newit.next();
      if (DataUtil.containsKey(elist, newEnt)) {      
        TemporalRange tr = new TemporalRange(newEnt, null, false);
        TemporalInputChange tic = tird_.addEntry(tr);
        support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic));          
      }
    }

    //
    // Add new non-targets to the map:
    //
    
    values = lwnt_.getSelectedObjects();
    for (int i = 0; i < values.length; i++) {
      slist.add(values[i]);
    }
        
    //
    // Submit the actual map change:
    //
    
    if ((elist.size() > 0) || (slist.size() > 0)) {
      TemporalInputChange[] tic = tird_.addTemporalInputRangeMaps(nodeID_, elist, slist);
      for (int i = 0; i < tic.length; i++) {
        support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic[i])); 
      }
    }
    
    if (elist.size() == 0) {
      TemporalInputChange tic = tird_.dropDataEntryKeys(nodeID_);
      support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic, false));
    }
    
    if (slist.size() == 0) {
      TemporalInputChange tic = tird_.dropDataSourceKeys(nodeID_);
      support.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic, false));
    }    

    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();    
    return;
  }  
}
