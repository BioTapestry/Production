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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.ComboBoxEditor;
import org.systemsbiology.biotapestry.util.ComboBoxEditorTracker;
import org.systemsbiology.biotapestry.util.ComboFinishedTracker;
import org.systemsbiology.biotapestry.util.DoubleEditor;
import org.systemsbiology.biotapestry.util.EditableComboBoxEditor;
import org.systemsbiology.biotapestry.util.EditableComboBoxEditorTracker;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.util.NumberEditorTracker;
import org.systemsbiology.biotapestry.util.ProtoDouble;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TextEditor;
import org.systemsbiology.biotapestry.util.TextEditorTracker;
import org.systemsbiology.biotapestry.util.TextFinishedTracker;
import org.systemsbiology.biotapestry.util.TrackingUnit;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** This is an editable table.  Rows are not sortable
*/

public class EditableTable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  public final static int NO_BUTTONS    = 0x00;
  public final static int ADD_BUTTON    = 0x01;
  public final static int EDIT_BUTTON   = 0x02;
  public final static int DELETE_BUTTON = 0x04;
  public final static int RAISE_BUTTON  = 0x08; 
  public final static int LOWER_BUTTON  = 0x10;
  public final static int RAISE_AND_LOWER_BUTTONS = RAISE_BUTTON | LOWER_BUTTON;
  public final static int ALL_BUT_EDIT_BUTTONS = ADD_BUTTON | RAISE_AND_LOWER_BUTTONS | DELETE_BUTTON;
  public final static int ALL_BUTTONS = ALL_BUT_EDIT_BUTTONS | EDIT_BUTTON;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private TableModel estm_;
  private int[] selectedRows_;
  private boolean trueButtonDEnable_;
  private boolean trueButtonAEnable_;
  private boolean trueButtonREnable_;
  private boolean trueButtonLEnable_;
  private boolean trueButtonEEnable_;  
  private JButton tableButtonD_;
  private JButton tableButtonR_;
  private JButton tableButtonL_;  
  private JButton tableButtonA_;  
  private JButton tableButtonE_;    
  private TrackingJTable qt_;
  private SelectionTracker tracker_;
  private boolean addAlwaysAtEnd_;
  private TextEditor textEdit_;
  private Set usedClasses_;
  private HashMap<Integer, TableCellEditor> editors_;
  private HashMap<Integer, TableCellRenderer> renderers_;
  private HashSet<Integer> editableEnums_;
  private JFrame parent_;
  private boolean cancelEditOnDisable_;
  private EditButtonHandler ebh_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  
  public EditableTable(UIComponentSource uics, DataAccessContext dacx, TableModel atm, JFrame parent) {
    uics_ = uics;
    dacx_ = dacx;
    estm_ = atm;
    parent_ = parent;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  

  public JTable getTable() {
    return (qt_);
  }

  public TableModel getModel() {
    return (estm_);
  }
  
  /***************************************************************************
  **
  ** Get the selected rows
  */   

  public int[] getSelectedRows() {
    return (selectedRows_);
  } 

  /***************************************************************************
  **
  ** Install edit button handler
  */   

  public void setEditButtonHandler(EditButtonHandler ebh) {
    ebh_ = ebh;
    return;
  }
  
  /***************************************************************************
  **
  ** Enable or disable the table
  */   

  public void setEnabled(boolean enable) {
    qt_.setEnabled(enable);    
   
    //
    // Handle editing changes:
    //
    
    if (!enable) {
      stopTheEditing(cancelEditOnDisable_);
    }

    ((DefaultTableCellRenderer)qt_.getTableHeader().getDefaultRenderer()).setEnabled(enable);
    
    enableDefaultEditorsAndRenderers(enable);
    enableColumnEditorsAndRenderers(enable);
  
    //
    // Gathered up from TimeStage setup: use always?
    //
    
    qt_.getSelectionModel().clearSelection();
    
    //
    // Handle rendering changes:
    //
    
    if (!enable) {
      if (tableButtonA_ != null) tableButtonA_.setEnabled(false);
      if (tableButtonD_ != null) tableButtonD_.setEnabled(false);
      if (tableButtonR_ != null) tableButtonR_.setEnabled(false);
      if (tableButtonL_ != null) tableButtonL_.setEnabled(false);
      if (tableButtonE_ != null) tableButtonE_.setEnabled(false);
    } else {
      syncButtons();
    }
   
    qt_.getTableHeader().repaint();
    qt_.repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Sync buttons to true allowed state
  */   

  private void syncButtons() {
    if (tableButtonA_ != null) {
      tableButtonA_.setEnabled(trueButtonAEnable_);
      tableButtonA_.revalidate();
    }
    if (tableButtonD_ != null) {
      tableButtonD_.setEnabled(trueButtonDEnable_);
      tableButtonD_.revalidate();
    }
    if (tableButtonR_ != null) {
      tableButtonR_.setEnabled(trueButtonREnable_);
      tableButtonR_.revalidate();
    }
    if (tableButtonL_ != null) {
      tableButtonL_.setEnabled(trueButtonLEnable_);
      tableButtonL_.revalidate();  
    }
    if (tableButtonE_ != null) {
      tableButtonE_.setEnabled(trueButtonEEnable_);
      tableButtonE_.revalidate();  
    }
    return;
  }

  /***************************************************************************
  **
  ** Update the table
  */   

  public void updateTable(boolean fireChange, List elements) {
    estm_.extractValues(elements);
   // estm_.modifyMap(estm_.buildFullClickList());
    if (fireChange) {
      estm_.fireTableDataChanged();
    }
    if (estm_.getRowCount() > 0) {
      qt_.getSelectionModel().setSelectionInterval(0, 0);
    } else {
      selectedRows_ = new int[0];
      trueButtonAEnable_ = estm_.canAddRow();
      trueButtonDEnable_ = false;
      trueButtonREnable_ = false;
      trueButtonLEnable_ = false;
      trueButtonEEnable_ = false;      
      syncButtons();      
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get stuff out of the table
  */   

  public List getValuesFromTable() {
    return (estm_.getValuesFromTable());
  }

  /***************************************************************************
  **
  ** Clear current selections
  */   

  public void clearSelections() {    
    tracker_.setIgnore(true);
    qt_.getSelectionModel().clearSelection();
    selectedRows_ = null;
    tracker_.setIgnore(false);        
    return;
  }

  /***************************************************************************
  **
  ** Make selected row visible
  ** 
  */

  public void makeCurrentSelectionVisible() { 
    makeSelectionVisible(qt_.getSelectionModel().getMinSelectionIndex());
    return;
  }

  /***************************************************************************
  **
  ** Make specified row visible
  ** 
  */

  public void makeSelectionVisible(int selRow) { 
    JViewport viewport = (JViewport)qt_.getParent();
    Rectangle rect = qt_.getCellRect(selRow, 0, true);
    Point pt = viewport.getViewPosition();
    rect.setLocation(rect.x - pt.x, rect.y - pt.y);
    viewport.scrollRectToVisible(rect);
    viewport.validate();
    return;
  }

  /***************************************************************************
  **
  ** Used to build and install a JTable using the model
  */

  public JPanel buildEditableTable(TableParams etp) {
    
    usedClasses_ = estm_.usedClasses();   
    selectedRows_ = new int[0];
    addAlwaysAtEnd_ = etp.addAlwaysAtEnd;
    cancelEditOnDisable_ = etp.cancelEditOnDisable;
    qt_ = new TrackingJTable(estm_);
    estm_.setJTable(qt_);
    JTableHeader th = qt_.getTableHeader();
    // Not sorting these days:
    //th.addMouseListener(new ReadOnlyStringTableModel.SortTrigger(this));    
    th.setReorderingAllowed(false);

    tracker_ = new SelectionTracker();
    ListSelectionModel lsm = qt_.getSelectionModel();
    if (etp.singleSelectOnly) {
      lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    lsm.addListSelectionListener(tracker_);
    if (etp.tableIsUnselectable) {
      qt_.setRowSelectionAllowed(false);
    }
 
    //
    // Set specialty widths:
    //

    if (etp.colWidths != null) {
      int numC = etp.colWidths.size();
      TableColumnModel tcm = qt_.getColumnModel();
      for (int i = 0; i < numC; i++) {
        ColumnWidths cw = etp.colWidths.get(i); 
        int viewColumn = qt_.convertColumnIndexToView(cw.colNum);
        TableColumn tfCol = tcm.getColumn(viewColumn);
        tfCol.setMinWidth(cw.min);
        tfCol.setPreferredWidth(cw.pref);
        tfCol.setMaxWidth(cw.max);
      }
    }

    UiUtil.installDefaultCellRendererForPlatform(qt_, String.class, true, uics_.getHandlerAndManagerSource());
    UiUtil.installDefaultCellRendererForPlatform(qt_, ProtoInteger.class, true, uics_.getHandlerAndManagerSource());
    UiUtil.installDefaultCellRendererForPlatform(qt_, Integer.class, true, uics_.getHandlerAndManagerSource()); // For non-edit number columns!
    
    editors_ = new HashMap<Integer, TableCellEditor>();
    renderers_ = new HashMap<Integer, TableCellRenderer>();
    editableEnums_ = new HashSet<Integer>();
    setDefaultEditors();   
    if (etp.perColumnEnums != null) {  
      setColumnEditorsAndRenderers(etp.perColumnEnums, renderers_, editors_, editableEnums_);
      estm_.setEnums(renderers_, editors_, editableEnums_);
    }

    UiUtil.platformTableRowHeight(qt_, true);
    
    return ((etp.buttonsOnSide) ? addButtonsOnSide(etp) : addButtonsOnBottom(etp));
  }

  
  /***************************************************************************
  **
  ** Refresh editors and renderers
  */       
 
  public void refreshEditorsAndRenderers(Map<Integer, EnumCellInfo> perColumnEnums) { 
    editors_.clear();
    renderers_.clear();
    editableEnums_.clear();
    setDefaultEditors();   
    if (perColumnEnums != null) {  
      setColumnEditorsAndRenderers(perColumnEnums, renderers_, editors_, editableEnums_);
      estm_.setEnums(renderers_, editors_, editableEnums_);
    }
    estm_.refreshColumnEditorsAndRenderers();
    return;
  }
    
  /***************************************************************************
  **
  ** Actually make the buttons
  */       
 
  private void createButtons(TableParams etp) { 
    
    ResourceManager rMan = dacx_.getRMan(); 
    if (etp.buttons != NO_BUTTONS) {
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        String aTag = rMan.getString("dialogs.addEntry");
        tableButtonA_ = (etp.buttonsOnSide) ? new JButton(aTag) : new FixedJButton(aTag);
        tableButtonA_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing(false);
              int[] addAt = (addAlwaysAtEnd_) ? new int[0] : selectedRows_;
              if (estm_.addRow(addAt)) {
                qt_.revalidate();
              }
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
          }
        });
        //
        // FIX ME:  Should start off false, since table might not allow an add.  But too
        // many existing clients depend on this.  They just extract values, without calling
        // to update the table.  That needs to be fixed!
        trueButtonAEnable_ = true;
        tableButtonA_.setEnabled(true);
      }

      if ((etp.buttons & LOWER_BUTTON) != 0x00) {
        String lTag = rMan.getString("dialogs.lowerEntry");
        tableButtonL_ = (etp.buttonsOnSide) ? new JButton(lTag) : new FixedJButton(lTag);
        tableButtonL_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing(false);
              estm_.bumpRowDown(selectedRows_);
              selectedRows_[0]++;
              qt_.getSelectionModel().setSelectionInterval(selectedRows_[0], selectedRows_[0]);
              qt_.revalidate();
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
          }
        });
        tableButtonL_.setEnabled(false);
        trueButtonLEnable_ = false;
      }
      
      if ((etp.buttons & RAISE_BUTTON) != 0x00) {
        String rTag = rMan.getString("dialogs.raiseEntry");
        tableButtonR_ = (etp.buttonsOnSide) ? new JButton(rTag) : new FixedJButton(rTag);
        tableButtonR_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing(false);
              estm_.bumpRowUp(selectedRows_);
              selectedRows_[0]--;
              qt_.getSelectionModel().setSelectionInterval(selectedRows_[0], selectedRows_[0]);
              qt_.revalidate();
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
          }
        });
        tableButtonR_.setEnabled(false);
        trueButtonREnable_ = false;
      }

      if ((etp.buttons & DELETE_BUTTON) != 0x00) {
        String dTag = rMan.getString("dialogs.deleteEntry");
        tableButtonD_ = (etp.buttonsOnSide) ? new JButton(dTag) : new FixedJButton(dTag);
        tableButtonD_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing(true);
              estm_.deleteRows(selectedRows_);
              qt_.getSelectionModel().clearSelection();        
              qt_.revalidate();
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
          }
        });
        tableButtonD_.setEnabled(false);
        trueButtonDEnable_ = false;
      }
      
      if ((etp.buttons & EDIT_BUTTON) != 0x00) {
        String eTag = rMan.getString("dialogs.editEntry");
        tableButtonE_ = (etp.buttonsOnSide) ? new JButton(eTag) : new FixedJButton(eTag);
        tableButtonE_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing(false);
              ebh_.pressed();
              qt_.revalidate();
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
          }
        });
        tableButtonE_.setEnabled(false);
        trueButtonEEnable_ = false;
      }
    } 
    return;
  }  
  
 /***************************************************************************
  **
  ** Get buttons on the bottom
  */       
 
  private JPanel startPanel(TableParams etp) { 
    
    //
    // No scroll only implemented for NO_BUTTONS case!
    //
    
    if ((etp.buttons != NO_BUTTONS) && (etp.noScroll)) {
      throw new IllegalStateException();
    }
    
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();   
    
    if (etp.noScroll) {
      JTableHeader jth = qt_.getTableHeader();
      retval.setBorder(BorderFactory.createEtchedBorder());
      retval.setLayout(new BorderLayout()); 
      retval.add(jth, BorderLayout.NORTH); 
      retval.add(qt_, BorderLayout.CENTER);        
    } else {
      JScrollPane jsp = new JScrollPane(qt_);
      UiUtil.gbcSet(gbc, 0, 0, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
    }
    return (retval);
  }

  
  /***************************************************************************
  **
  ** Get buttons on the bottom
  */       
 
  private JPanel addButtonsOnBottom(TableParams etp) { 
    
    JPanel retval = startPanel(etp);
    
    if (etp.buttons != NO_BUTTONS) {
      GridBagConstraints gbc = new GridBagConstraints();
      createButtons(etp);
      int buttonCount = 0;
      
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        tableButtonPanel.add(tableButtonA_);
        buttonCount++;
      }
      if ((etp.buttons & LOWER_BUTTON) != 0x00) {
        if (buttonCount != 0) {
          tableButtonPanel.add(Box.createHorizontalStrut(10));
        }
        tableButtonPanel.add(tableButtonL_);
        buttonCount++;
      }
       if ((etp.buttons & RAISE_BUTTON) != 0x00) {
        if (buttonCount != 0) {
          tableButtonPanel.add(Box.createHorizontalStrut(10));
        }
        tableButtonPanel.add(tableButtonR_);
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
      tableButtonPanel.add(Box.createHorizontalGlue());
 
      UiUtil.gbcSet(gbc, 0, 10, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Put the buttons on the side
  */       
 
  private JPanel addButtonsOnSide(TableParams etp) { 
    
    JPanel retval = startPanel(etp);
    
    if (etp.buttons != NO_BUTTONS) {
      GridBagConstraints gbc = new GridBagConstraints();
      createButtons(etp);
      int buttonCount = 0;  
      JPanel tableButtonPanel = new JPanel();
      tableButtonPanel.setLayout(new GridBagLayout());
      if ((etp.buttons & ADD_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonA_, gbc);
      }
      if ((etp.buttons & LOWER_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonL_, gbc);
      }
       if ((etp.buttons & RAISE_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonR_, gbc);
      }
      if ((etp.buttons & DELETE_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonD_, gbc);
      }
      if ((etp.buttons & EDIT_BUTTON) != 0x00) {
        UiUtil.gbcSet(gbc, 0, buttonCount++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
        tableButtonPanel.add(tableButtonE_, gbc);
      }

      UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.VERT, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
      retval.add(tableButtonPanel, gbc);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Set the default editors
  ** 
  */

  private void setDefaultEditors() {
    Iterator cit = usedClasses_.iterator();
    while (cit.hasNext()) {
      Class used = (Class)cit.next();
      if (used.equals(ProtoDouble.class)) {
        DoubleEditor dEdit = new DoubleEditor(uics_.getHandlerAndManagerSource(), parent_);
        new NumberEditorTracker(dEdit, estm_, uics_.getHandlerAndManagerSource());
        qt_.setDefaultEditor(ProtoDouble.class, dEdit);
      } else if (used.equals(String.class)) {
        textEdit_ = new TextEditor(uics_.getHandlerAndManagerSource());
        new TextEditorTracker(textEdit_, estm_, estm_, uics_.getHandlerAndManagerSource());
        qt_.setDefaultEditor(String.class, textEdit_);
      } else if (used.equals(ProtoInteger.class)) {
        IntegerEditor intEdit = new IntegerEditor(uics_.getHandlerAndManagerSource(), parent_);
        new NumberEditorTracker(intEdit, estm_, uics_.getHandlerAndManagerSource());
        qt_.setDefaultEditor(ProtoInteger.class, intEdit);
      } else if (used.equals(Boolean.class)) {
        // Happy with the default!
      } else if (used.equals(Integer.class)) {
        // Happy with the default!  (actually gotta be non-editable!)
      } else if (used.equals(EnumChoiceContent.class)) {
        // Happy with default 
      } else if (!used.equals(EnumCell.class)) {
        throw new IllegalStateException();
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the editor/renderer enabled state
  ** 
  */

  private void enableDefaultEditorsAndRenderers(boolean enable) {
    Iterator cit = usedClasses_.iterator();
    while (cit.hasNext()) {
      Class used = (Class)cit.next();
      if (used.equals(ProtoDouble.class)) {
        DoubleEditor dt = (DoubleEditor)qt_.getDefaultEditor(ProtoDouble.class);
        dt.setEnabled(enable);
        dt.setEditable(enable);
        // Sometimes a TrackingDoubleRenderer, not a default renderer:
        JComponent dtcr = (JComponent)qt_.getDefaultRenderer(ProtoDouble.class);
        dtcr.setEnabled(enable);
      } else if (used.equals(String.class)) {
        TextEditor te = (TextEditor)qt_.getDefaultEditor(String.class);
        te.setEnabled(enable);
        te.setEditable(enable);
        DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer)qt_.getDefaultRenderer(String.class);
        dtcr.setEnabled(enable);
      } else if (used.equals(ProtoInteger.class)) {
        IntegerEditor ie = (IntegerEditor)qt_.getDefaultEditor(ProtoInteger.class);
        ie.setEnabled(enable);
        ie.setEditable(enable);
        DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer)qt_.getDefaultRenderer(ProtoInteger.class);
        dtcr.setEnabled(enable);
      } else if (used.equals(Boolean.class)) {
        // leave alone!
      } else if (used.equals(Integer.class)) {
        // leave alone!
      } else if (used.equals(EnumChoiceContent.class)) {
        // leave alone
      } else if (!used.equals(EnumCell.class)) {
        throw new IllegalStateException();
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Set the enabled/disabled state for the editors and renderers for specific columns
  ** 
  */

  private void enableColumnEditorsAndRenderers(boolean enable) {
    Iterator<Integer> rkit = renderers_.keySet().iterator();
    while (rkit.hasNext()) {
      Integer key = rkit.next();
      JComboBox ecbr = (JComboBox)renderers_.get(key);
      ecbr.setEnabled(enable);
    }
    Iterator<Integer> ekit = editors_.keySet().iterator();
    while (ekit.hasNext()) {
      Integer key = ekit.next();
      JComboBox eece = (JComboBox)editors_.get(key);
      eece.setEnabled(enable);
      if (editableEnums_.contains(key)) {
        eece.setEditable(enable);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Set the editors and renderers for specific columns
  ** 
  */

  private void setColumnEditorsAndRenderers(Map<Integer, EnumCellInfo> contents, Map<Integer, TableCellRenderer> renderers,
                                            Map<Integer, TableCellEditor> editors, Set<Integer> editableEnums) {  
    Iterator<Integer> cit = contents.keySet().iterator();
    while (cit.hasNext()) {
      Integer intObj = cit.next();
      EnumCellInfo eci = contents.get(intObj);
      TableColumn tc = qt_.getColumnModel().getColumn(intObj.intValue());
      Class colClass = eci.myClass;
      if (eci.editable) {
        TableCellRenderer ecbr;
        if (colClass.equals(EnumCell.class)) {
          ecbr = new EnumCell.EditableComboBoxRenderer(eci.values, uics_.getHandlerAndManagerSource());
        } else if (colClass.equals(EnumChoiceContent.class)) {
          ecbr = new EnumChoiceContent.EditableComboChoiceRenderer(eci.values, uics_.getHandlerAndManagerSource());
        } else {
          throw new IllegalStateException();
        }
        tc.setCellRenderer(ecbr);
        renderers.put(intObj, ecbr);
        
        EditableComboBoxEditor re;
        if (colClass.equals(EnumCell.class)) {
          re = new EnumCell.EditableEnumCellEditor(eci.values, uics_.getHandlerAndManagerSource());
        } else if (colClass.equals(EnumChoiceContent.class)) {
          re = new EnumChoiceContent.EditableComboChoiceEditor(eci.values, uics_.getHandlerAndManagerSource());
        } else {
          throw new IllegalStateException();
        }
        new EditableComboBoxEditorTracker(re, estm_, uics_.getHandlerAndManagerSource());
        tc.setCellEditor(re);
        editors.put(intObj, re);
        editableEnums.add(intObj);
      } else {
        TableCellRenderer rocbr;
        if (colClass.equals(EnumCell.class)) {
          if (eci.trackingUnits == null) {
            rocbr = new EnumCell.ReadOnlyComboBoxRenderer(eci.values, uics_.getHandlerAndManagerSource());
          } else {
            rocbr = new EnumCell.TrackingReadOnlyComboBoxRenderer(eci.values, eci.trackingUnits, uics_.getHandlerAndManagerSource());
          }
        } else if (colClass.equals(EnumChoiceContent.class)) {          
          if (eci.trackingUnits == null) {
            rocbr = new EnumChoiceContent.ReadOnlyComboChoiceRenderer(eci.values, uics_.getHandlerAndManagerSource());
          } else {
            rocbr = new EnumChoiceContent.TrackingReadOnlyComboChoiceRenderer(eci.values, eci.trackingUnits, uics_.getHandlerAndManagerSource());
          }
        } else {
          throw new IllegalStateException();
        }
        tc.setCellRenderer(rocbr);
        renderers.put(intObj, rocbr);
        ComboBoxEditor me;
        if (colClass.equals(EnumCell.class)) {       
          me = new EnumCell.ReadOnlyEnumCellEditor(eci.values, uics_.getHandlerAndManagerSource());
        } else if (colClass.equals(EnumChoiceContent.class)) {
          me = new EnumChoiceContent.ReadOnlyComboChoiceEditor(eci.values, uics_.getHandlerAndManagerSource());       
        } else {
          throw new IllegalStateException();
        }
        new ComboBoxEditorTracker(me, estm_, estm_, uics_.getHandlerAndManagerSource());     
        tc.setCellEditor(me);
        editors.put(intObj, me);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Stop all table editing.
  ** 
  */

  public void stopTheEditing(boolean doCancel) {
    Iterator cit = usedClasses_.iterator();
    while (cit.hasNext()) {
      Class used = (Class)cit.next();
      if (!used.equals(EnumCell.class) && !used.equals(EnumChoiceContent.class)) {
        // Ignore default editors:
        if (!used.equals(Integer.class) && !used.equals(Boolean.class)) {
          TableCellEditor tce = qt_.getDefaultEditor(used);
          if (doCancel) {
            tce.cancelCellEditing();
          } else {
            tce.stopCellEditing();
          }
        }
      }
    }
    Iterator ekit = editors_.keySet().iterator();
    while (ekit.hasNext()) {
      Integer key = (Integer)ekit.next();
      TableCellEditor tce = editors_.get(key);
      if (doCancel) {
        tce.cancelCellEditing();
      } else {
        tce.stopCellEditing();
      }     
    }
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for tracking button presses
  */

  public static interface EditButtonHandler {
    public void pressed();
  }  
 
  /***************************************************************************
  **
  ** Used for setup
  */
  
  public static class TableParams {
    public boolean addAlwaysAtEnd;
    public boolean singleSelectOnly;
    public boolean cancelEditOnDisable;
    public boolean tableIsUnselectable;
    public int buttons;
    public Map<Integer, EnumCellInfo> perColumnEnums;
    public List<ColumnWidths> colWidths;
    public boolean buttonsOnSide;
    public boolean noScroll;
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
  
  /***************************************************************************
  **
  ** Used for enum cell definitions
  */
  
  public static class EnumCellInfo {
    public boolean editable;
    public List values;
    public Map<Integer, TrackingUnit> trackingUnits;
    public Class myClass;
    
    public EnumCellInfo(boolean editable, List values, Class myClass) {
      this(editable, values, null, myClass);
    }
    
    public EnumCellInfo(boolean editable, List values, Map<Integer, TrackingUnit> trackingUnits, Class myClass) {
      this.editable = editable;
      this.values = values;
      this.trackingUnits = trackingUnits;
      if ((trackingUnits != null) && editable) {
        throw new IllegalArgumentException();
      }
      this.myClass = myClass;
    }
  }
  
  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  
  public static abstract class TableModel extends AbstractTableModel implements ComboFinishedTracker, TextFinishedTracker {

    protected JTable tmqt_;
    protected ArrayList[] columns_;
    protected ArrayList origIndex_;
    protected ArrayList[] hiddenColumns_;
    protected int rowCount_;
    protected String[] colNames_;
    protected Class[] colClasses_;
    protected boolean[] canEdit_;
    protected boolean[] colUseDirect_;
    protected Set myEditableEnums_;
    protected HashMap<Integer, TableCellRenderer> myRenderers_;
    protected HashMap<Integer, TableCellEditor> myEditors_;
    protected int collapseCount_;
    protected boolean isCollapsed_;
    protected UIComponentSource uics_;
    protected DataAccessContext dacx_;

    protected TableModel(UIComponentSource uics, DataAccessContext dacx, int colNum) {
      uics_ = uics;
      dacx_ = dacx;
      columns_ = new ArrayList[colNum];
      for (int i = 0; i < columns_.length; i++) {
        columns_[i] = new ArrayList();
      }
      origIndex_ = new ArrayList();
      hiddenColumns_ = new ArrayList[0];
      canEdit_ = null;
      colUseDirect_ = null;
      collapseCount_ = -1;
      isCollapsed_ = false;
      myRenderers_ = new HashMap<Integer, TableCellRenderer>();
      myEditors_ = new HashMap<Integer, TableCellEditor>();
    }
    
    
    //
    // Warning! Only useful in special circumstances!
    //
    
    protected void resetColumnCount(int colNum) {
      columns_ = new ArrayList[colNum];
      for (int i = 0; i < columns_.length; i++) {
        columns_[i] = new ArrayList();
      }
    } 
    
    protected void addHiddenColumns(int colNum) {
      hiddenColumns_ = new ArrayList[colNum];
      for (int i = 0; i < hiddenColumns_.length; i++) {
        hiddenColumns_[i] = new ArrayList();
      }
      return;
    }

    Set usedClasses() {
      HashSet retval = new HashSet();
      for (int i = 0; i < colClasses_.length; i++) {
        retval.add(colClasses_[i]);
      }
      return (retval);
    }
    
    
    void setJTable(JTable tab) {
      tmqt_ = tab; 
    }
    
    void setEnums(Map<Integer, TableCellRenderer> renderers, Map<Integer, TableCellEditor> editors, Set editableEnums) {
      myRenderers_.clear();
      myEditors_.clear();
      myRenderers_.putAll(renderers);
      myEditors_.putAll(editors);
      myEditableEnums_ = editableEnums;
      return;
    }

    public void deleteRows(int[] rows) {
      for (int i = 0; i < rows.length; i++) {
        for (int j = 0; j < columns_.length; j++) {
          columns_[j].remove(rows[i]);
        }
        for (int j = 0; j < hiddenColumns_.length; j++) {
          hiddenColumns_[j].remove(rows[i]);
        }
        origIndex_.remove(rows[i]);
        for (int j = i + 1; j < rows.length; j++) {
          if (rows[j] > rows[i]) {
            rows[j]--;
          }
        }
        rowCount_--;
      }
      return; 
    }

    public void bumpRowUp(int[] rows) {
      if ((rows.length != 1) || (rows[0] == 0)) {
        throw new IllegalStateException();
      }
      for (int j = 0; j < columns_.length; j++) {
        Object remObj = columns_[j].remove(rows[0] - 1);
        columns_[j].add(rows[0], remObj);
      }
      for (int j = 0; j < hiddenColumns_.length; j++) {
        Object remObj = hiddenColumns_[j].remove(rows[0] - 1);
        hiddenColumns_[j].add(rows[0], remObj);
      }
      Object remObj = origIndex_.remove(rows[0] - 1);
      origIndex_.add(rows[0], remObj);
      return;
    }

    public void bumpRowDown(int[] rows) {
      if ((rows.length != 1) || (rows[0] == (origIndex_.size() - 1))) {
        throw new IllegalStateException();
      }
      for (int j = 0; j < columns_.length; j++) {
        Object remObj = columns_[j].remove(rows[0]);
        columns_[j].add(rows[0] + 1, remObj);
      }
      for (int j = 0; j < hiddenColumns_.length; j++) {
        Object remObj = hiddenColumns_[j].remove(rows[0]);
        hiddenColumns_[j].add(rows[0] + 1, remObj);
      }
      Object remObj = origIndex_.remove(rows[0]);
      origIndex_.add(rows[0] + 1, remObj);
      return;
    }    

    public boolean addRow(int[] rows) {
      if (rows.length == 0) {
        origIndex_.add(null);
      } else if (rows.length == 1) {
        origIndex_.add(rows[0] + 1, null);
      } else {
        throw new IllegalStateException(); 
      }
      for (int j = 0; j < columns_.length; j++) {
        if (rows.length == 0) {
          columns_[j].add(null);
        } else {
          columns_[j].add(rows[0] + 1, null);
        }
      }
      for (int j = 0; j < hiddenColumns_.length; j++) {
        if (rows.length == 0) {
          hiddenColumns_[j].add(null);
        } else {
          hiddenColumns_[j].add(rows[0] + 1, null);
        }
      } 
      rowCount_++;
      return (true); 
    } 

    public int getRowCount() {
      return (rowCount_); 
    }  

    public int getColumnCount() {
      if (collapseCount_ == -1) {
        return (columns_.length);
      }
      return ((isCollapsed_) ? collapseCount_ : columns_.length);
    }

    protected List getListAt(int c) {
      try {
        if (c >= columns_.length) {
          throw new IllegalArgumentException();
        }
        return (columns_[c]);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    // NOT SORTABLE (YET!)

    protected int mapSelectionIndex(int r) {
      return (r);
    }

    public String getColumnName(int c) {
      try {
        if (c >= colNames_.length) {
          throw new IllegalArgumentException();
        }
        if ((colUseDirect_ == null) || !colUseDirect_[c]) {
          ResourceManager rMan = dacx_.getRMan();
          return (rMan.getString(colNames_[c]));
        } else {
          return (colNames_[c]);
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public Class<?> getColumnClass(int c) {
      try {
        if (c >= colClasses_.length) {
          throw new IllegalArgumentException();
        }
        return (colClasses_[c]);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public Object getValueAt(int r, int c) {
      try {
        List list = getListAt(c);
        if (list.isEmpty()) {
          return (null);
        }
        return (list.get(mapSelectionIndex(r)));
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }

    public void setValueAt(Object value, int r, int c) {
      try { 
        List list = getListAt(c);
        if (list.isEmpty()) {
          return;
        }
        Object currVal = list.get(r);
        Class colClass = getColumnClass(c);
        if (colClass.equals(ProtoInteger.class)) {
          ProtoInteger currNum = (ProtoInteger)currVal;
          if ((currNum != null) && currNum.textValue.equals(((ProtoInteger)value).textValue)) {
            return;
          }
        } else if (colClass.equals(String.class)) {
          String currStr = (String)currVal;
          if ((currStr != null) && currStr.equals(value)) {
            return;
          }    
        } else if (colClass.equals(ProtoDouble.class)) {
          ProtoDouble currNum = (ProtoDouble)currVal;
          if ((currNum != null) && currNum.textValue.equals(((ProtoDouble)value).textValue)) {
            return;
          }
        } else if (colClass.equals(Boolean.class)) {  
          Boolean currInclude = (Boolean)currVal;
          if ((currInclude != null) && (currInclude.equals(value))) {
            return;
          } 
        } else if (colClass.equals(EnumCell.class)) {
          EnumCell currEC = (EnumCell)currVal;
          if (myEditableEnums_.contains(Integer.valueOf(c))) {
            if ((currEC != null) && (currEC.internal != null) &&
                currEC.internal.equals(((EnumCell)value).internal)) {
              return;
            }
          } else {
            if ((currEC != null) && (currEC.value == ((EnumCell)value).value)) {
              return;
            }
          }
        } else if (colClass.equals(EnumChoiceContent.class)) {
          EnumChoiceContent currEC = (EnumChoiceContent)currVal;
          if (myEditableEnums_.contains(Integer.valueOf(c))) {
            if ((currEC != null) && (currEC.name != null) &&
                currEC.name.equals(((EnumChoiceContent)value).name)) {
              return;
            }
          } else {
            if ((currEC != null) && (currEC.val.equals(((EnumChoiceContent)value).val))) {
              return;
            }
          }  
        } else {
          throw new IllegalStateException();
        }
        list.set(r, value);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    } 

    public boolean isCellEditable(int r, int c) {
      try { 
        if (canEdit_ == null) {
          return (true);
        } else {
          return (canEdit_[c]);
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (false);
    }

    public void extractValues(List rowElements) {
      origIndex_.clear(); 
      for (int i = 0; i < columns_.length; i++) {
        columns_[i].clear();
      }
      for (int i = 0; i < hiddenColumns_.length; i++) {
        hiddenColumns_[i].clear();
      }
      rowCount_ = 0;
      Iterator iit = rowElements.iterator();
      while (iit.hasNext()) {
        Object dc = iit.next();
        origIndex_.add(new Integer(rowCount_++));
      }
      return;  
    }
   
    public abstract List getValuesFromTable();
    
    public void collapseView(boolean doCollapse) {
      if (collapseCount_ == -1) {
        throw new IllegalStateException();
      }
      isCollapsed_ = doCollapse;
      fireTableStructureChanged();
      refreshColumnEditorsAndRenderers();
      return;
    }
   
    //
    // In non-editable enums (e.g. OneEnumTableModel), an empty enum list means
    // you can't add rows to the table.  Provide this hook.
    //
    
    public boolean canAddRow() {
      return (true);
    }
        
    /***************************************************************************
    **
    ** If columns change, we gotta reinstall!
    ** 
    */

    private void refreshColumnEditorsAndRenderers() {  
      int currColumns = getColumnCount();
      TableColumnModel tcm = tmqt_.getColumnModel();
 
      Iterator<Integer> rkit = myRenderers_.keySet().iterator();
      while (rkit.hasNext()) {
        Integer key = rkit.next();
        if (key.intValue() >= currColumns) {
          continue;
        }
        TableColumn tc = tcm.getColumn(key.intValue());
        tc.setCellRenderer(myRenderers_.get(key));
      }
      
      Iterator<Integer> ekit = myEditors_.keySet().iterator();
      while (ekit.hasNext()) {
        Integer key = ekit.next();
        if (key.intValue() >= currColumns) {
          continue;
        }
        TableColumn tc = tcm.getColumn(key.intValue());
        tc.setCellEditor(myEditors_.get(key));
      }
      return;
    }

    //
    // These methods allow the table to revise its contents when a field is
    // finished editing!
    //

    public boolean textEditingDone(int col, int row, Object val) {
      return (true);
    }
    
    public boolean comboEditingDone(int col, int row, Object val) {
      return (true);
    }
      
  }
  
  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  class SelectionTracker implements ListSelectionListener {
    
    private boolean ignore_;
    
    SelectionTracker() {
      ignore_ = false;
    }
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (ignore_) {
          return;
        }
        if (lse.getValueIsAdjusting()) {
          return;
        }
        selectedRows_ = qt_.getSelectedRows();
        Object lastOrig = qt_.getLastOrig();
        int col = qt_.getLastColumn();
        if (lastOrig != null) {
          TableCellEditor fixit = editors_.get(new Integer(col));
          fixit.getTableCellEditorComponent(qt_, lastOrig, 
                                            true, selectedRows_[0], qt_.getLastColumn());
          qt_.clearLastOrig();
        }
        boolean haveARow = (selectedRows_.length > 0);
        trueButtonDEnable_ = haveARow;
        trueButtonEEnable_ = haveARow;
        boolean haveZeroOrOne = (selectedRows_.length <= 1);
        trueButtonAEnable_ = (addAlwaysAtEnd_ || haveZeroOrOne) && estm_.canAddRow();
        boolean canLower = ((selectedRows_.length == 1) && 
                            (selectedRows_[0] < estm_.getRowCount() - 1));        
        trueButtonLEnable_ = canLower;
        boolean canRaise = ((selectedRows_.length == 1) && (selectedRows_[0] != 0));        
        trueButtonREnable_ = canRaise;  
        syncButtons();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;             
    } 
    
    public void setIgnore(boolean ignore) {
      ignore_ = ignore;
      return;
    }
  } 
  
  /***************************************************************************
  **
  ** 4/19/10: NASTY Editable combo box bug in base Swing table implementation.
  ** If we edit a field, e.g. "AAA3" to "AAA34", then click to the _same_ column
  ** with an identical entry, "AAA3", it incorrectly pops to "AAA34".  Various
  ** attempts to circumvent this have failed.  So we track the editor selections
  ** in the table, and if the current request matches the previous editor column,
  ** and it is an editable combo box, we see if the original value of the previous
  ** editor matches our current value.  If so, we will replace it back to the original
  ** then the list selection event comes in; this appears to be consistently after the
  ** error has been introduced. 
  ** 
  ** Used to track editing bug
  */
  
  class TrackingJTable extends JTable {
    
    private int lastCol_ = -1;
    private Object lastOrig_;
    private static final long serialVersionUID = 1L;
           
    TrackingJTable(TableModel estm) {
      super(estm);
    }
    
    Object getLastOrig() {
      return (lastOrig_);
    }
    
    void clearLastOrig() {
      lastOrig_ = null;
      return;
    }
    
    int getLastColumn() {
      return (lastCol_);
    }
       
    public TableCellEditor getCellEditor(int row, int column) {
      lastOrig_ = null;
      if ((lastCol_ == column) && editableEnums_.contains(new Integer(column))) {
        TableCellEditor tce = editors_.get(new Integer(column));
        if (tce instanceof EnumCell.EditableEnumCellEditor) {
          EnumCell.EditableEnumCellEditor fixit = (EnumCell.EditableEnumCellEditor)editors_.get(new Integer(column));
          if (((EnumCell)estm_.getValueAt(row, column)).display.equals(((EnumCell)fixit.getOriginalValue()).display)) {
            lastOrig_ = estm_.getValueAt(row, column);
          }
        } else if (tce instanceof EnumChoiceContent.EditableComboChoiceEditor) {
          EnumChoiceContent.EditableComboChoiceEditor fixit = (EnumChoiceContent.EditableComboChoiceEditor)editors_.get(new Integer(column));
          if (((EnumChoiceContent)estm_.getValueAt(row, column)).name.equals(((EnumChoiceContent)fixit.getOriginalValue()).name)) {
            lastOrig_ = estm_.getValueAt(row, column);
          }
          
        }
      }
      lastCol_ = column;
      return (super.getCellEditor(row, column));
    }
  }
  
  /***************************************************************************
  **
  ** A Concrete instantiation of the TableModel that supports a single value.
  */
  
  public static class OneValueModel extends TableModel {
    
    private final static int VALUE   = 0;
    private final static int NUM_COL_ = 1;
    
    private static final long serialVersionUID = 1L;
    
    public class TableRow {
      public String value;
      
      public TableRow() {   
      }
     
      private TableRow(int i) {
        value = (String)columns_[VALUE].get(i);
      }
      
      private void toCols() {
        columns_[VALUE].add(value);  
        return;
      }
    }
 
    public OneValueModel(UIComponentSource uics, DataAccessContext dacx, String title, boolean edit) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {title};
      colClasses_ = new Class[] {String.class};
      canEdit_ = new boolean[] {edit};
    }
 
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
  
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Useful for single tagged values
  */

  public static class TaggedValueModel extends TableModel {
    
    private final static int VALUE    = 0;
    private final static int NUM_COL_ = 1;
    
    private final static int HIDDEN_     = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private static final long serialVersionUID = 1L;
    
    
    public class TableRow {
      public String hidden;
      public String value;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        hidden = (String)hiddenColumns_[HIDDEN_].get(i);
        value = (String)columns_[VALUE].get(i);
      }
      
      void toCols() {
        columns_[VALUE].add(value);
        hiddenColumns_[HIDDEN_].add(hidden);
        return;
      }
    }
 
    public TaggedValueModel(UIComponentSource uics, DataAccessContext dacx, String title, boolean editable) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {title};
      colClasses_ = new Class[] {String.class};
      canEdit_ = new boolean[] {editable};
      addHiddenColumns(NUM_HIDDEN_);
    }
 
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
  
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Used for single enum tables
  */
  
  public static class OneEnumTableModel extends TableModel {
    
    public final static int ENUM_COL_ = 0;
    private final static int NUM_COL_ = 1;
    
    private final static int ORIG_ORDER_ = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private ArrayList currEnums_;
    
    public class TableRow {
      public Integer origOrder;
      public EnumCell enumChoice;
      
      public TableRow() {   
      }
     
      TableRow(int i) {
        enumChoice = (EnumCell)columns_[ENUM_COL_].get(i);
        origOrder = (Integer)hiddenColumns_[ORIG_ORDER_].get(i);
      }
      
      void toCols() {
        columns_[ENUM_COL_].add(enumChoice);  
        hiddenColumns_[ORIG_ORDER_].add(origOrder);
        return;
      }
    }
  
    public OneEnumTableModel(UIComponentSource uics, DataAccessContext dacx, String title, List currEnums) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {title};
      colClasses_ = new Class[] {EnumCell.class};
      canEdit_ = new boolean[] {true};
      addHiddenColumns(NUM_HIDDEN_);
      currEnums_ = new ArrayList(currEnums);
    }
   
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    }
    
    //
    // This class does not allow editable enums, since we are
    // disabling the add button with an empty enum
    //
        
    void setEnums(Map renderers, Map editors, Set editableEnums) {
      if (!editableEnums.isEmpty()) {
        throw new IllegalArgumentException();
      }
      super.setEnums(renderers, editors, editableEnums);
      return;
    }

    public void setCurrentEnums(List prsList) {
      currEnums_.clear();
      currEnums_.addAll(prsList);
      return;
    }
    
    public boolean canAddRow() {
      return (!currEnums_.isEmpty());
    }   
       
    public boolean addRow(int[] rows) {
      super.addRow(rows);
      int lastIndex = rowCount_ - 1;
      int addIndex = (rows.length == 0) ? lastIndex : rows[0] + 1;
      if (currEnums_.isEmpty()) {
        throw new IllegalStateException();
      }
      EnumCell fillIn = new EnumCell((EnumCell)currEnums_.get(0));
      columns_[ENUM_COL_].set(addIndex, fillIn);
      hiddenColumns_[ORIG_ORDER_].set(addIndex, new Integer(lastIndex));      
      return (true);
    }    
  }
}
