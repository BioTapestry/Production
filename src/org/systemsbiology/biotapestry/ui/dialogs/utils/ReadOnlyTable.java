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


package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Used for read-only string tables
*/

public class ReadOnlyTable {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  public final static int UPDATE_NO_SELECT    = -1;
  public final static int UPDATE_SELECT_FIRST =  0;
  
  
  public final static int NO_BUTTONS    = 0x00;
  public final static int ADD_BUTTON    = 0x01;
  public final static int EDIT_BUTTON   = 0x02;
  public final static int DELETE_BUTTON = 0x04;
  public final static int ALL_BUTTONS = ADD_BUTTON | EDIT_BUTTON | DELETE_BUTTON;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // (MOSTLY) PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  private JTable jt_;
  private JLabel tableLabel_;
  private JScrollPane jsp_;
  private TableModel atm_;
  public int[] selectedRows;  // FIX ME!!
  private SelectionTracker tracker_;
  public List rowElements;  // FIX ME!
  private SelectionHandler userSh_;
  private ButtonHandler bh_;
  private JButton tableButtonA_;   
  private JButton tableButtonD_;   
  private JButton tableButtonE_;
  private boolean trueButtonDEnable_;
  private boolean trueButtonAEnable_;
  private boolean trueButtonEEnable_;
  private boolean identitySort_;
  private List<ExtraButton> extraButtonProps_;
  private boolean tableSuppressed_;
  private BTState appState_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Use if model is built first
  */
  
  public ReadOnlyTable(BTState appState, TableModel atm, SelectionHandler sh) {
    appState_ = appState;
    rowElements = new ArrayList();
    this.atm_ = atm;
    userSh_ = sh;
    tableSuppressed_ = false;
  }
  
  /***************************************************************************
  **
  ** Use when the Table needs to be built before the model
  */   

  public ReadOnlyTable(BTState appState) {
    appState_ = appState;
    rowElements = new ArrayList();
    tableSuppressed_ = false;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Use when the Table needs to be built before the model
  */   
  
  public void lateBinding(TableModel atm, SelectionHandler sh) {
    this.atm_ = atm;
    userSh_ = sh;
    return;
  }   

  /***************************************************************************
  **
  ** Set the button handler
  */   
  
  public void setButtonHandler(ButtonHandler bh) {
    this.bh_ = bh;
    return;
  }       

  /***************************************************************************
  **
  ** Get Swing table 
  */ 
  
  public JTable getTable() {
    return (jt_);
  }

  /***************************************************************************
  **
  ** Get scroll pane holding table
  */ 
  
  public JScrollPane getScrollPane() {
    return (jsp_);
  }
  
  /***************************************************************************
  **
  ** Get underlying model
  */ 
  
  public TableModel getModel() {
    return (atm_);
  }

  /***************************************************************************
  **
  ** Get selection tracker
  */ 
  
  public SelectionTracker getTracker() {
    return (tracker_);
  }

  /***************************************************************************
  **
  ** Enable or disable the table
  */   

  public void setEnabled(boolean enable) {
    tableSuppressed_ = !enable;
    if (tableLabel_ != null) {
      tableLabel_.setEnabled(enable);
    }
    jt_.setEnabled(enable);
    ((DefaultTableCellRenderer)jt_.getTableHeader().getDefaultRenderer()).setEnabled(enable);
    ((DefaultTableCellRenderer)jt_.getDefaultRenderer(String.class)).setEnabled(enable);
    ((DefaultTableCellRenderer)jt_.getDefaultRenderer(Integer.class)).setEnabled(enable);
    
    if (!enable) {
      if (tableButtonA_ != null) tableButtonA_.setEnabled(false);
      if (tableButtonD_ != null) tableButtonD_.setEnabled(false);
      if (tableButtonE_ != null) tableButtonE_.setEnabled(false);
      if (extraButtonProps_ != null) {
        int numEB = extraButtonProps_.size();
        for (int i = 0; i < numEB; i++) {
          ExtraButton eb = (ExtraButton)extraButtonProps_.get(i);
          eb.extraButton.setEnabled(false);
        } 
      }  
    } else {
      syncButtons();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Set "true" underlying button state
  */   

  public void setButtonEnabledState(int whichButton, boolean enabled) {
    switch (whichButton) {
      case ReadOnlyTable.EDIT_BUTTON:
        trueButtonEEnable_ = enabled;
        break;
      case ReadOnlyTable.ADD_BUTTON:
        trueButtonAEnable_ = enabled;
        break;
      case ReadOnlyTable.DELETE_BUTTON:
        trueButtonDEnable_ = enabled;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Sync buttons to true allowed state
  */   

  public void syncButtons() {
    //
    // If table is currently suppressed, this does nothing!
    //
    if (tableSuppressed_) {
      return;
    }
    if (tableButtonA_ != null) {
      tableButtonA_.setEnabled(trueButtonAEnable_);
      tableButtonA_.revalidate();
    }
    if (tableButtonD_ != null) {
      tableButtonD_.setEnabled(trueButtonDEnable_);
      tableButtonD_.revalidate();
    }
    if (tableButtonE_ != null) {
      tableButtonE_.setEnabled(trueButtonEEnable_);
      tableButtonE_.revalidate();
    }
    if (extraButtonProps_ != null) {
      int numEB = extraButtonProps_.size();
      for (int i = 0; i < numEB; i++) {
        ExtraButton eb = (ExtraButton)extraButtonProps_.get(i);
        eb.extraButton.setEnabled(eb.shadowEnabled);
        eb.extraButton.revalidate();
      } 
    }  
    return;
  }  

  /***************************************************************************
  **
  ** Update the table
  */   

  public void updateTable(boolean fireChange) {
    updateTable(fireChange, UPDATE_NO_SELECT);
    return;
  }
  
  /***************************************************************************
  **
  ** Update the table.  IMPORTANT!  If you are sending in a row to select,
  ** it had better be in sync with the NEW ordering of the rowElements!
  */   

  public void updateTable(boolean fireChange, int selRow) {
    atm_.extractValues(rowElements);
    if (!identitySort_) {
      atm_.modifyMap(atm_.buildFullClickList());
    }
    if (fireChange) {
      atm_.fireTableDataChanged();
    }
    if ((atm_.getRowCount() > 0) && (selRow >= 0)) {
      int size = rowElements.size();
      if (selRow >= size) {
        selRow = size - 1;
      }   
      jt_.getSelectionModel().setSelectionInterval(selRow, selRow);
    } else {
      trueButtonAEnable_ = true;
      trueButtonDEnable_ = false;
      trueButtonEEnable_ = false;
      if (extraButtonProps_ != null) {
        int numEB = extraButtonProps_.size();
        for (int i = 0; i < numEB; i++) {
          ExtraButton eb = (ExtraButton)extraButtonProps_.get(i);
          eb.shadowEnabled = eb.behavior.alwaysOn;
        } 
      }  
      syncButtons();   
      clearSelections(true);  // IS this necessary? (found in some usages before refactoring)
    }
    return;
  }

  /***************************************************************************
  **
  ** Clear current selections.  Note that the tracker (e.g. button tracker) is not tracking this!
  */   

  public void clearSelections(boolean trackerIgnore) {    
    if ((tracker_ != null) && trackerIgnore) tracker_.setIgnore(true);
    jt_.getSelectionModel().clearSelection();
    selectedRows = null;
    if ((tracker_ != null) && trackerIgnore) tracker_.setIgnore(false);        
    return;
  }

 /***************************************************************************
  **
  ** Make selected row visible
  ** 
  */

  public void makeCurrentSelectionVisible() { 
    makeSelectionVisible(jt_.getSelectionModel().getMinSelectionIndex());
    return;
  }

  /***************************************************************************
  **
  ** Make selected row visible
  ** 
  */

  public void makeSelectionVisible(int selRow) { 
    JViewport viewport = (JViewport)jt_.getParent();
    Rectangle rect = jt_.getCellRect(selRow, 0, true);
    int numElem = this.rowElements.size();
    Rectangle vRect = viewport.getViewRect();
    //
    // If the selection cell is within the half the
    // height of the viewport, just scroll to the 
    // first cell:
    // 
    if ((rect.y + rect.height) < (vRect.height / 2)) {
      rect.setLocation(rect.x - vRect.x, 0);
      viewport.scrollRectToVisible(rect);
      viewport.validate();
    } else if (((numElem - 1 - selRow) * rect.height) < (vRect.height / 2)) {
      //
      // Appears to be a bug, at least in 1.4.  Cannot seem to get the pane
      // to scroll to the maximum value, which means we cannot see the
      // last selected row.  This fixes the problem by scheduling it for
      // later...
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          JScrollBar jsb = jsp_.getVerticalScrollBar();
          int max = jsb.getMaximum();
          int inc = jsb.getUnitIncrement(1);
          jsp_.getVerticalScrollBar().setValue(max - inc);
        }
      });
    } else {    
      double vHeight = (double)vRect.height;
      double halfHeightInRows = (vHeight/(double)rect.height) / 2.0;
      int padY = (int)(halfHeightInRows * rect.height);
      int useRectY = rect.y - vRect.y - padY;
      rect.setBounds(rect.x - vRect.x, useRectY, rect.width, vRect.height);
      viewport.scrollRectToVisible(rect);
      viewport.validate();
    }     
    return;
  }
  
  /***************************************************************************
  **
  ** Build table 
  ** 
  */
 
  public JPanel buildReadOnlyTable(TableParams etp) {

    jt_ = new JTable(atm_);
    JTableHeader th = jt_.getTableHeader();
    if (!etp.disableColumnSort) {
      th.addMouseListener(new SortTrigger());
      identitySort_ = false;
    } else {
      identitySort_ = true;
    }
    th.setReorderingAllowed(false);
 
    //
    // Get row shading on the Mac:
    //
    
    UiUtil.installDefaultCellRendererForPlatform(jt_, String.class, false, appState_);
    UiUtil.installDefaultCellRendererForPlatform(jt_, Integer.class, false, appState_);
    
    //
    // Center integers by default:
    //
    ((DefaultTableCellRenderer)jt_.getDefaultRenderer(Integer.class)).setHorizontalAlignment(JLabel.CENTER);
    selectedRows = new int[0];

    if (etp.tableIsUnselectable) {
      jt_.setRowSelectionAllowed(false);
      if (userSh_ != null) {
        throw new IllegalStateException();
      }
    } else {
      ArrayList allTables = new ArrayList();
      if (etp.multiTableSelectionSyncing == null) {
        allTables.add(this);
      } else {
        allTables.addAll(etp.multiTableSelectionSyncing);
      }
      
      if (etp.buttons != NO_BUTTONS) {
        if (!etp.clientHandlesButtonEnable) {
          userSh_ = new SelectorForButtons(userSh_);
        }
      }   
      tracker_ = new SelectionTracker(allTables, etp.clearOthersOnSelect, userSh_);
      ListSelectionModel lsm = jt_.getSelectionModel();
      lsm.setSelectionMode((etp.canMultiSelect) ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION 
                                                : ListSelectionModel.SINGLE_SELECTION);    
      lsm.addListSelectionListener(tracker_);
    }

    //
    // Set specialty widths:
    //

    UiUtil.platformTableRowHeight(jt_, false);
    if (etp.colWidths != null) {
      int numC = etp.colWidths.size();
      TableColumnModel tcm = jt_.getColumnModel();
      for (int i = 0; i < numC; i++) {
        ColumnWidths cw = (ColumnWidths)etp.colWidths.get(i); 
        int viewColumn = jt_.convertColumnIndexToView(cw.colNum);
        TableColumn tfCol = tcm.getColumn(viewColumn);
        tfCol.setMinWidth(cw.min);
        tfCol.setPreferredWidth(cw.pref);
        tfCol.setMaxWidth(cw.max);
      }
    }
    
    return ((etp.buttonsOnSide) ? buttonsOnSideLayout(etp) : buttonsOnBottomLayout(etp));
  }
  
  /***************************************************************************
  **
  ** Stick the buttons on the bottom
  ** 
  */
 
  private JPanel buttonsOnBottomLayout(TableParams etp) {
    
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();   

    //
    // Add the table title, if specified:
    //

    int rowNum = 0;
    if (etp.tableTitle != null) {
      tableLabel_ = new JLabel(etp.tableTitle, JLabel.LEFT);
      if (etp.titleFont != null) {
        tableLabel_.setFont(etp.titleFont);
      }
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableLabel_, gbc);
      rowNum++;
    } else if (etp.tableJLabel != null) {
      tableLabel_ = etp.tableJLabel;
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(etp.tableJLabel, gbc);
      rowNum++;
    }
    
    int height = (etp.tableHeight == 0) ? 5 : etp.tableHeight;
    jsp_ = new JScrollPane(jt_);

    UiUtil.gbcSet(gbc, 0, rowNum, 1, height, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(jsp_, gbc);
    rowNum += height;
    
    if ((etp.buttons != NO_BUTTONS) || (etp.userAddedButtons != null)) {
      createButtons(etp);
      int buttonCount = 0;
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue());
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        tableButtonPanel.add(tableButtonA_);
        buttonCount++;
      }
      if ((etp.buttons & DELETE_BUTTON) != 0x00) {
        if (buttonCount != 0) {
          tableButtonPanel.add(Box.createHorizontalStrut(10));
        }
        tableButtonPanel.add(tableButtonD_);
        buttonCount++;
      }
      if ((etp.buttons & EDIT_BUTTON) != 0x00) {
        if (buttonCount != 0) {
          tableButtonPanel.add(Box.createHorizontalStrut(10));
        }
        tableButtonPanel.add(tableButtonE_);
        buttonCount++;
      }
      if (etp.userAddedButtons != null) {
        int numUab = etp.userAddedButtons.size();
        for (int i = 0; i < numUab; i++) {
          if (buttonCount != 0) {
            tableButtonPanel.add(Box.createHorizontalStrut(10));
          }
          ExtraButton eb = etp.userAddedButtons.get(i);
          tableButtonPanel.add(eb.extraButton);
          buttonCount++;
        }
      }
      
      tableButtonPanel.add(Box.createHorizontalGlue());    
      //
      // Add the button panel
      //

      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Common button creation
  ** 
  */
 
  private void createButtons(TableParams etp) {
  
    ResourceManager rMan = appState_.getRMan();
    if (etp.buttons != NO_BUTTONS) {
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        String aTag = rMan.getString("dialogs.addEntry");
        tableButtonA_ = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, aTag) : new FixedJButton(aTag);
        tableButtonA_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {   
              bh_.pressed(ADD_BUTTON);
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
          }
        });
        trueButtonAEnable_ = true;
      }

      if ((etp.buttons & DELETE_BUTTON) != 0x00) {
        String dTag = rMan.getString("dialogs.deleteEntry");
        tableButtonD_ = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, dTag) : new FixedJButton(dTag);
        tableButtonD_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              bh_.pressed(DELETE_BUTTON);
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
          }
        });
        trueButtonDEnable_ = false;
        tableButtonD_.setEnabled(false);
      }
      
      if ((etp.buttons & EDIT_BUTTON) != 0x00) {
        String eTag = rMan.getString("dialogs.editEntry");
        tableButtonE_ = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, eTag) : new FixedJButton(eTag);
        tableButtonE_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              bh_.pressed(EDIT_BUTTON);
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
          }
        });
        trueButtonEEnable_ = false;
        tableButtonE_.setEnabled(false);
      }
      
      if (etp.userAddedButtons != null) {
        extraButtonProps_ = new ArrayList<ExtraButton>(etp.userAddedButtons);
        int numEB = extraButtonProps_.size();
        for (int i = 0; i < numEB; i++) {
          ExtraButton eb = extraButtonProps_.get(i);
          eb.shadowEnabled = eb.behavior.alwaysOn;
          eb.extraButton.setEnabled(eb.shadowEnabled);
        } 
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Stick the buttons on the side
  ** 
  */
 
  public JPanel buttonsOnSideLayout(TableParams etp) {
    
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();   
    boolean gottaButton = (etp.buttons != NO_BUTTONS) || (etp.userAddedButtons != null);
    //
    // Add the table title, if specified:
    //

    int rowNum = 0;
    if (etp.tableTitle != null) {
      tableLabel_ = new JLabel(etp.tableTitle, JLabel.LEFT);
      if (etp.titleFont != null) {
        tableLabel_.setFont(etp.titleFont);
      }
      UiUtil.gbcSet(gbc, 0, rowNum, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableLabel_, gbc);
      rowNum++;
    } else if (etp.tableJLabel != null) {
      tableLabel_ = etp.tableJLabel;
      UiUtil.gbcSet(gbc, 0, rowNum, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(etp.tableJLabel, gbc);
      rowNum++;
    }
    
    int height = (etp.tableHeight == 0) ? 5 : etp.tableHeight;
    int width = gottaButton ? 2 : 3;
    jsp_ = new JScrollPane(jt_);
    UiUtil.gbcSet(gbc, 0, rowNum, width, height, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(jsp_, gbc);
      
    if (gottaButton) {
      createButtons(etp);

      int buttonCount = 0;
      JPanel tableButtonPanel = new JPanel();
      tableButtonPanel.setLayout(new GridBagLayout());
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonA_, gbc);
      }
      if ((etp.buttons & DELETE_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonD_, gbc);
      }
      if ((etp.buttons & EDIT_BUTTON) != 0x00) {   
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonE_, gbc);
      }
      if (etp.userAddedButtons != null) {
        int numUab = etp.userAddedButtons.size();
        for (int i = 0; i < numUab; i++) {
          UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
          ExtraButton eb = etp.userAddedButtons.get(i);
          tableButtonPanel.add(eb.extraButton, gbc);
          buttonCount++;
        }
      }
 
      UiUtil.gbcSet(gbc, width, rowNum, 1, height, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 1.0);
      retval.add(tableButtonPanel, gbc);
    }
    
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for setup
  */
  
  public static class TableParams {     
    public boolean disableColumnSort;
    public boolean tableIsUnselectable;
    public int buttons;
    public List multiTableSelectionSyncing;
    public boolean clearOthersOnSelect;
    public String tableTitle;
    public JLabel tableJLabel;
    public Font titleFont;
    public List<ColumnWidths> colWidths;
    public int tableHeight;
    public boolean clientHandlesButtonEnable;
    public boolean buttonsOnSide;
    public List<ExtraButton> userAddedButtons;
    public boolean canMultiSelect;
  }
  
  /***************************************************************************
  **
  ** Used for setup
  */
  
  public static class ColumnWidths {
    public int colNum;
    public int min;
    public int pref;
    public int max;
    
    public ColumnWidths() {   
    }
    
    public ColumnWidths(int colNum, int min, int pref, int max) {
      this.colNum = colNum;
      this.min = min;
      this.pref = pref;
      this.max = max;
    } 
  }
  
  /****************************************************************************
  **
  ** Base class for read-only string tables
  */

  public static abstract class TableModel extends AbstractTableModel {

    protected ArrayList[] columns_;
    protected ArrayList[] hiddenColumns_;
    protected Class[] colClasses_;
    protected Comparator[] comparators_;
    protected List sortMap_;
    protected int rowCount_;
    protected String[] colNames_;
    protected BTState tabAppState_;
    
    private static final long serialVersionUID = 1L;
    
    protected TableModel(BTState appState, int colNum) {
      tabAppState_ = appState;
      columns_ = new ArrayList[colNum];
      for (int i = 0; i < colNum; i++) {
        columns_[i] = new ArrayList();
      }
      sortMap_ = new ArrayList();
      hiddenColumns_ = new ArrayList[0];
    }
    
    protected void addHiddenColumns(int colNum) {
      hiddenColumns_ = new ArrayList[colNum];
      for (int i = 0; i < hiddenColumns_.length; i++) {
        hiddenColumns_[i] = new ArrayList();
      }
      return;
    }
    
    public void modifyMap(List columns) {
      int num = columns.size();
      ArrayList args = new ArrayList();
      for (int i = 0; i < num; i++) {
        Integer colNum = (Integer)columns.get(i);
        int colNumVal = colNum.intValue();
        boolean positive = (colNumVal >= 0);
        int trueCol = (positive) ? colNumVal : ((colNumVal == Integer.MIN_VALUE) ? 0 : -colNumVal);
        List elems = getListAt(trueCol);
        Comparator compare = getColumnComparator(trueCol);
        MapBuilderColumnInfo colInfo = new MapBuilderColumnInfo(positive, compare); 
        MapBuilderArg mba = new MapBuilderArg(elems, colInfo);
        args.add(mba);
      }
      sortMap_ = MapBuilder.mapFromSort(args);
      return;
    }

    protected List getListAt(int c) {
      try {
        if (c >= columns_.length) {
          throw new IllegalArgumentException();
        }
        return (columns_[c]);
      } catch (Exception ex) {
        tabAppState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public String getColumnName(int c) {
      try {
        ResourceManager rMan = tabAppState_.getRMan();
        if (c >= colNames_.length) {
          throw new IllegalArgumentException();
        }
        return (rMan.getString(colNames_[c]));
      } catch (Exception ex) {
        tabAppState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public int getColumnCount() {
      return (columns_.length);
    }

    public void extractValues(List rowElements) {
      sortMap_.clear(); 
      for (int i = 0; i < columns_.length; i++) {
        columns_[i].clear();
      }
      rowCount_ = 0;
      Iterator iit = rowElements.iterator();
      while (iit.hasNext()) {
        Object dc = iit.next();
        sortMap_.add(new Integer(rowCount_));
        rowCount_++;
      }
      return;  
    }

    public List<Integer> buildFullClickList() {
      ArrayList<Integer> retval = new ArrayList<Integer>();
      int numCol = getColumnCount();
      for (int i = 0; i < numCol; i++) {
        retval.add(new Integer(i));
      }
      return (retval);
    }

    public int mapSelectionIndex(int r) {
      return (((Integer)sortMap_.get(r)).intValue());
    }

    public int mapToSelectionIndex(Object obj, List base) {
      int size = base.size();
      for (int i = 0; i < size; i++) {
        if (obj == base.get(i)) { // note ACTUAL EQUALS HERE!
          int sSize = sortMap_.size();
          for (int j = 0; j < sSize; j++) {
            Integer sVal = (Integer)sortMap_.get(j);
            if (sVal.intValue() == i) {
              return (j);
            }
          }
        }
      }
      throw new IllegalArgumentException();
    } 
    
    public int mapToSelectionIndexEquals(Object obj, List base) {
      int size = base.size();
      for (int i = 0; i < size; i++) {
        if (obj.equals(base.get(i))) { 
          int sSize = sortMap_.size();
          for (int j = 0; j < sSize; j++) {
            Integer sVal = (Integer)sortMap_.get(j);
            if (sVal.intValue() == i) {
              return (j);
            }
          }
        }
      }
      throw new IllegalArgumentException();
    }

    public Object getValueAt(int r, int c) {
      try {
        List list = getListAt(c);
        return (list.get(mapSelectionIndex(r)));
      } catch (Exception ex) {
        tabAppState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public int getRowCount() {
      return (rowCount_); 
    }  

    public Class<?> getColumnClass(int c) {
      try {
        if (colClasses_ == null) {
          return (String.class);
        }
        if (c >= colClasses_.length) {
          throw new IllegalArgumentException();
        }
        return (colClasses_[c]);
      } catch (Exception ex) {
        tabAppState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }
    
    public Comparator getColumnComparator(int c) {
      try {
        if (comparators_ == null) {
          return (String.CASE_INSENSITIVE_ORDER);
        }
        if (c >= comparators_.length) {
          throw new IllegalArgumentException();
        }
        return (comparators_[c]);
      } catch (Exception ex) {
        tabAppState_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public void setValueAt(Object value, int r, int c) {
      throw new IllegalStateException();
    }

    public boolean isCellEditable(int r, int c) {
      return (false);
    }
  }

  /***************************************************************************
  **
  ** Used for tracking tables
  */

  public static interface SelectionHandler {
    public void selected(Object obj, int whichTab, int[] whichIndex);
  }
  
  /***************************************************************************
  **
  ** Handle selections; do nothing
  */ 
  
  public static class EmptySelector implements SelectionHandler {
    public void selected(Object obj, int whichTab, int[] whichIndex) {
      return;
    }
  }

  /***************************************************************************
  **
  ** Used for tracking button presses
  ** 
  ** Note that e.g. pressing a delete button _just_ calls this handler.  The
  ** selected item is NOT automatically removed from the table.  Instead, the
  ** handler is responsible for removing table elements and calling a
  ** _updateTable_!!!!! If a parallel non-table data structure is being kept
  ** in sync, do that too.  IT IS NOT ENOUGH to just remove the element and
  ** tell the table to extractValues: e.g. the delete button will not be
  ** correctly disabled!
  **   String key = ((AnnotTableModel)rtd_.getModel()).getSelectedKey(selected);
  **   resultSource_.deleteNote(key);
  **   rtd_.rowElements.remove(rtd_.getModel().mapSelectionIndex(rtd_.selectedRows[0]));
  **   rtd_.updateTable(true, rtd_.selectedRows[0]);
  */

  public static interface ButtonHandler {
    public void pressed(int whichButton);
  }  
  
  /***************************************************************************
  **
  ** Used to build maps
  */
  
  public static class MapBuilderColumnInfo {
    public boolean positive;
    public Comparator compare;
    
    public MapBuilderColumnInfo(boolean positive, Comparator compare) {
      this.positive = positive;
      this.compare = compare;
    }  
  }
  
  /***************************************************************************
  **
  ** Used to build maps
  */
  
  public static class MapBuilderArg {
    public List elements;
    public boolean positive;
    public MapBuilderColumnInfo colInfo;
    
    public MapBuilderArg(List elements, MapBuilderColumnInfo colInfo) {
      this.elements = elements;
      this.colInfo = colInfo;
    }  
  } 
  
  /***************************************************************************
  **
  ** Used to build maps
  */
  
  public static class MapBuilder implements Comparable {

    private List elements;
    private Integer index;
    private List columnProps;

    public MapBuilder(List elements, int index, List columnProps) {
      this.elements = elements;
      this.index = new Integer(index);
      this.columnProps = columnProps;
    }

    public int compareTo(Object o) {
      MapBuilder other = (MapBuilder)o;
      int numElem = this.elements.size();
      int numElemO = other.elements.size();
      if (numElem != numElemO) {
        throw new IllegalArgumentException();
      }
      for (int i = 0; i < numElem; i++) {
        Comparable tComp = (Comparable)this.elements.get(i);
        Comparable oComp = (Comparable)other.elements.get(i);        
        int compVal = compareOneElem(tComp, oComp, (MapBuilderColumnInfo)columnProps.get(i));
        if (compVal != 0) {
          return (compVal);
        }
      }
      return (0);
    }
    
    private int compareOneElem(Comparable thiso, Comparable othero, MapBuilderColumnInfo mbci) {
      int retval = mbci.compare.compare(thiso, othero);
      if (!mbci.positive) {
        retval = -retval;
      }
      return (retval);
    }    
    
    public static List mapFromSort(List toSort) { // list of lists
      ArrayList builders = new ArrayList();
      int numLists = toSort.size();
      int colSize = 0;
      ArrayList columnProps = new ArrayList();
      for (int j = 0; j < numLists; j++) {
        MapBuilderArg mba = (MapBuilderArg)toSort.get(j);
        columnProps.add(mba.colInfo);
        if (j == 0) {
          colSize = mba.elements.size();    
        }
      }
      for (int i = 0; i < colSize; i++) {
        ArrayList rowData = new ArrayList();
        for (int j = 0; j < numLists; j++) {       
          MapBuilderArg mba = (MapBuilderArg)toSort.get(j);
          rowData.add(mba.elements.get(i));
        }
        builders.add(new MapBuilder(rowData, i, columnProps));
      }
      Collections.sort(builders);
      ArrayList retval = new ArrayList();
      for (int i = 0; i < colSize; i++) {
        retval.add(((MapBuilder)builders.get(i)).index);
      }
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Used to track selections
  */
  
  public class SelectionTracker implements ListSelectionListener {
    
    private List allTabs_;
    private boolean ignore_;
    private boolean clearOthers_;
    private SelectionHandler handler_;
 
    SelectionTracker(List allTabs, boolean clearOthers, SelectionHandler handler) {
      allTabs_ = allTabs;
      ignore_ = false;
      clearOthers_ = clearOthers;
      handler_ = handler;
    }
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (ignore_) {
          return;
        }
        if (lse.getValueIsAdjusting()) {
          return;
        }
        handleSelection();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
      }
      return;             
    }
    
    public void setIgnore(boolean ignore) {
      ignore_ = ignore;
      return;
    }
        
    private void handleSelection() {
      
      int matchTab = 0;
      int numTabs = allTabs_.size();
      for (int i = 0; i < numTabs; i++) {
        ReadOnlyTable td = (ReadOnlyTable)allTabs_.get(i);
        if (td == ReadOnlyTable.this) {
          matchTab = i;
          continue;
        }
        if (clearOthers_) {
          td.tracker_.setIgnore(true);
          td.jt_.getSelectionModel().clearSelection();
          td.selectedRows = null;
          td.trueButtonDEnable_ = false;
          td.trueButtonAEnable_ = true; 
          td.trueButtonEEnable_ = false;
          if (td.extraButtonProps_ != null) {
            int numEB = td.extraButtonProps_.size();
            for (int j = 0; j < numEB; j++) {
              ExtraButton eb = (ExtraButton)td.extraButtonProps_.get(j);
              eb.shadowEnabled = eb.behavior.alwaysOn;
            } 
          }  
          td.syncButtons(); 
          td.tracker_.setIgnore(false);
        }
      }

      selectedRows = jt_.getSelectedRows(); 
      if (selectedRows.length > 0) {
        Object obj;
        if (selectedRows.length == 1) {
          int selRow = atm_.mapSelectionIndex(selectedRows[0]);
          obj = rowElements.get(selRow);
        } else {
          obj = new ArrayList();
          for (int i = 0; i < selectedRows.length; i++) {
            int selRow = atm_.mapSelectionIndex(selectedRows[i]);
            ((ArrayList)obj).add(rowElements.get(selRow));
          }
        }
        handler_.selected(obj, matchTab, selectedRows); 
      } else {
        handler_.selected(null, matchTab, selectedRows); 
      }
      return;             
    }    
  }
  
  /***************************************************************************
  **
  ** Used to trigger sorts
  */
  
  public class SortTrigger extends MouseAdapter {
    private ArrayList clickList_;
 
    SortTrigger() {
      clickList_ = new ArrayList();
    }

    public void mouseClicked(MouseEvent evt) {
      try {
        JTable table = ((JTableHeader)evt.getSource()).getTable();
        TableColumnModel colModel = table.getColumnModel();
        int vColIndex = colModel.getColumnIndexAtX(evt.getX());
        if (vColIndex == -1) {
          return;
        }
        
        //
        // FIX ME!  After ctrl-click, single non-ctrl click on same column does not clear
        // the multi-column sort!
        //
        
        boolean isCtrl = evt.isControlDown();
        Integer nextElem = new Integer(vColIndex);
        Integer oppoElem = (vColIndex == 0) ? new Integer(Integer.MIN_VALUE) : new Integer(-vColIndex);
        int neind = clickList_.indexOf(nextElem);
        int oeind = clickList_.indexOf(oppoElem);
        if (!isCtrl) {
          clickList_.clear();
        }       
        if (neind != -1) {
          if (!isCtrl) {
            clickList_.add(oppoElem);
          } else {
            clickList_.set(neind, oppoElem);
          }
        } else if (oeind != -1) {
          if (!isCtrl) {
            clickList_.add(nextElem);
          } else {
            clickList_.set(oeind, nextElem);
          }
        } else {              
          clickList_.add(nextElem);
        }
        
        Object selCand = null;
        if ((selectedRows != null) && (selectedRows.length > 0)) {
          int selRow = atm_.mapSelectionIndex(selectedRows[0]);
          selCand = rowElements.get(selRow);          
          selectedRows = null;
          tracker_.setIgnore(true);
          jt_.getSelectionModel().clearSelection();
          tracker_.setIgnore(false);
        }
        
        atm_.modifyMap(clickList_);
        atm_.fireTableDataChanged();
        if (selCand != null) {
          int selRow = atm_.mapToSelectionIndex(selCand, rowElements);
          tracker_.setIgnore(true);
          jt_.getSelectionModel().setSelectionInterval(selRow, selRow);
          selectedRows = new int[] {selRow};
          tracker_.setIgnore(false);
          //
          // Make the selection visible:
          //
          JViewport viewport = (JViewport)table.getParent();
          Rectangle rect = table.getCellRect(selRow, 0, true);
          Point pt = viewport.getViewPosition();
          rect.setLocation(rect.x - pt.x, rect.y - pt.y);
          viewport.scrollRectToVisible(rect);
        }
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Useful concrete instantiation of the Table Model that shows one column,
  ** and traffics in TrueObjChoiceContent list elements.
  */

  public static class NameWithHiddenIDModel extends TableModel {
    public final static int  NAME    = 0;
    private final static int NUM_COL_ = 1; 
    
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    private static final long serialVersionUID = 1L;
 
    public NameWithHiddenIDModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"nameTable.name"};
      addHiddenColumns(NUM_HIDDEN_);
    }    
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_NAME_ID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        TrueObjChoiceContent tocc = (TrueObjChoiceContent)iit.next();
        columns_[NAME].add(tocc.name); 
        hiddenColumns_[HIDDEN_NAME_ID_].add(tocc.val);
      }
      return;
    }
    
    public String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[0])));
    } 
    
    public List getSelectedKeys(int[] selected) {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < selected.length; i++) {
        retval.add((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[i])));
      }
      return (retval);
    } 
  }

  /***************************************************************************
  **
  ** Handle selections
  */ 
    
  public class SelectorForButtons implements SelectionHandler {
    
    private SelectionHandler mySelector_;
    
    SelectorForButtons(SelectionHandler mySelector) {
      mySelector_ = mySelector;
    }
 
    public void selected(Object obj, int whichTab, int[] whichIndex) {    
      trueButtonDEnable_ = (whichIndex.length == 1);
      trueButtonAEnable_ = true; 
      trueButtonEEnable_ = (whichIndex.length == 1);
      if (extraButtonProps_ != null) {
        int numEB = extraButtonProps_.size();
        for (int j = 0; j < numEB; j++) {
          ExtraButton eb = (ExtraButton)extraButtonProps_.get(j);
          if (eb.behavior.alwaysOn) {
            eb.shadowEnabled = true;
          } else {
            if (whichIndex.length == 0) {
              eb.shadowEnabled = false;
            } else if (whichIndex.length == 1) {
              eb.shadowEnabled = eb.behavior.singleSelectionOn;
            } else if (whichIndex.length > 1) {
              eb.shadowEnabled = eb.behavior.multiSelectionOn;
            }
          }
        } 
      }  
      syncButtons();
      if (mySelector_ != null) {
        mySelector_.selected(obj, whichTab, whichIndex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Specify Extra Button Behavior
  */ 
    
  public static class ExtraButtonBehavior {
    
    public boolean alwaysOn;
    public boolean singleSelectionOn;
    public boolean multiSelectionOn;
    
    public ExtraButtonBehavior(boolean alwaysOn, boolean singleSelectionOn, boolean multiSelectionOn) {
      this.alwaysOn = alwaysOn;
      this.singleSelectionOn = singleSelectionOn;
      this.multiSelectionOn = multiSelectionOn;
    }
  }
  
  /***************************************************************************
  **
  ** Specify Extra Button
  */ 
    
  public static class ExtraButton {
    
    public ExtraButtonBehavior behavior;
    public boolean shadowEnabled;
    public JButton extraButton;
    
    public ExtraButton(boolean alwaysOn, boolean singleSelectionOn, boolean multiSelectionOn, JButton extraButton) {
      behavior = new ExtraButtonBehavior(alwaysOn, singleSelectionOn, multiSelectionOn);
      this.shadowEnabled = false;
      this.extraButton = extraButton;
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // USEFUL COMPARATORS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Integers
  **
  */
  
  public static class IntegerComparator implements Comparator {
 
    public int compare(Object o1, Object o2) {
      Integer int1 = (Integer)o1;
      Integer int2 = (Integer)o2;
      return (int1.compareTo(int2));
    }
  } 
  
  /***************************************************************************
  **
  ** Doubles
  **
  */
  
  public static class DoubleComparator implements Comparator<Double> {
 
    public int compare(Double db1, Double db2) {
      return (db1.compareTo(db2));
    }
  } 
    
  /***************************************************************************
  **
  ** Puts INTEGER numbers after strings, with numbers sorted numerically, not lexicographically
  **
  */
  
  public static class NumStrComparator implements Comparator<String> {
    private Pattern pattern_;
    private Matcher matcher_;
        
    public NumStrComparator() {
      pattern_ = Pattern.compile("[0-9][0-9]*");
      matcher_ = pattern_.matcher("");
    }
      
    public int compare(String str1, String str2) {
      str1 = str1.trim();
      str2 = str2.trim();
      matcher_.reset(str1);
      boolean num1 = matcher_.matches();
      matcher_.reset(str2);
      boolean num2 = matcher_.matches();
      if (num1 && num2) {
        int val1 = new Integer(str1).intValue();
        int val2 = new Integer(str2).intValue();
        return (val1 - val2);
      } else if (num1) {
        return (-1);
      } else if (num2) {
        return (1);
      } else {
        return (str1.compareTo(str2));
      }
    }
  }
}