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


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;

/****************************************************************************
**
** Panle holding a table for displaying and editing name/value paris
*/

public class NameValuePairTablePanel extends JPanel {

  private EditableTable est_;
  private JFrame parent_;
  private List namesList_; 
  private List valuesList_;
  private NameValuePairList nvpList_;
  private BTState appState_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////      

  public NameValuePairTablePanel(BTState appState, JFrame parent, NameValuePairList currList, Set allKeys, Set allValues, 
                                 Map allMappedValues, boolean buttonsOnSide) {
    appState_ = appState;
    parent_ = parent;
    nvpList_ = currList;
    
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    
    namesList_ = buildEnumList(allKeys);
    valuesList_ = buildEnumList(allValues);
    
    est_ = new EditableTable(appState_, new NameValuePairTable(appState_), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.perColumnEnums = new HashMap();
    etp.buttonsOnSide = buttonsOnSide;
    etp.perColumnEnums.put(new Integer(NameValuePairTable.NAME), new EditableTable.EnumCellInfo(true, namesList_));
    etp.perColumnEnums.put(new Integer(NameValuePairTable.VALUE), new EditableTable.EnumCellInfo(true, valuesList_));  
    JPanel tablePan = est_.buildEditableTable(etp);

    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(tablePan, gbc);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Get the table
  ** 
  */
  
  public EditableTable getTable() {
    return (est_);
  } 
    
  /***************************************************************************
  **
  ** reset the current list
  ** 
  */
  
  public void resetNVPList(NameValuePairList currList) {
    nvpList_ = currList;
    displayProperties();
    return;
  } 
 
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties() {
    List tableList = buildTableList();
    NameValuePairTable nvt = ((NameValuePairTable)est_.getModel());
    nvt.extractValues(tableList);
    nvt.fireTableDataChanged();
    return;
  } 
  
  /***************************************************************************
  **
  ** Extract our UI values
  ** 
  */
  
  public NameValuePairList extractData() {    
    //
    // Check for errors before actually doing the work.  Errors return null.
    //
    
    NameValuePairTable tcs = (NameValuePairTable)est_.getModel(); 
    List vals = tcs.getValuesFromTable();
    if (!checkValues(vals)) {
      return (null);
    }
    
    NameValuePairList retval = getResult(vals);
    return (retval);
  }  
  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Here's the table
  */  
  
 
  class NameValuePairTable extends EditableTable.TableModel {  
    
    final static int NAME             = 0;
    final static int VALUE            = 1;
    private final static int NUM_COL_ = 2;
    
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      EnumCell name;
      EnumCell value;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        name = (EnumCell)columns_[NAME].get(i);
        value = (EnumCell)columns_[VALUE].get(i);
      }
      
      void toCols() {
        columns_[NAME].add(name);  
        columns_[VALUE].add(value);
        return;
      }
    }
  
    NameValuePairTable(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"nvpEntry.name",    
                                "nvpEntry.value"};
      colClasses_ = new Class[] {EnumCell.class,
                                 EnumCell.class};
      canEdit_ = new boolean[] {true,
                                true};
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
    
    public boolean addRow(int[] rows) {
      super.addRow(rows);
      int lastIndex = rowCount_ - 1;
      columns_[NAME].set(lastIndex, new EnumCell("", null, -1, -1));
      columns_[VALUE].set(lastIndex, new EnumCell("", null, -1, -1));      
      return (true);
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build list of names
  */
   
  private List buildEnumList(Set allNames) {
    ArrayList retval = new ArrayList();
    TreeSet sortedNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    sortedNames.addAll(allNames);
    retval.add(new EnumCell("", null, 0, 0));
    Iterator sit = sortedNames.iterator();
    int count = 1;
    while (sit.hasNext()) {
      String desc = (String)sit.next();
      retval.add(new EnumCell(desc, desc, count, count));
      count++;
    }
    return (retval);      
  }

  /***************************************************************************
  **
  ** Build the table entries
  */
  
  private List buildTableList() {
    ArrayList retval = new ArrayList();
    if (nvpList_ == null) {
      return (retval);
    }
    
    NameValuePairTable nvpt = (NameValuePairTable)est_.getModel();  
 
    Iterator pit = nvpList_.getIterator();
    while (pit.hasNext()) {
      NameValuePair nvp = (NameValuePair)pit.next();
      NameValuePairTable.TableRow tr = nvpt.new TableRow(); 
      String name = nvp.getName();
      int rsize = namesList_.size();
      for (int i = 1; i < rsize; i++) {
        EnumCell ecn = (EnumCell)namesList_.get(i);
        if (ecn.internal.equals(name)) {
          tr.name = new EnumCell(ecn);
          break;
        }
      }
      String value = nvp.getValue();
      int vsize = valuesList_.size();
      for (int i = 1; i < vsize; i++) {
        EnumCell ecv = (EnumCell)valuesList_.get(i);
        if (ecv.internal.equals(value)) {
          tr.value = new EnumCell(ecv);
          break;
        }
      }
      retval.add(tr);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Check the table entries
  */
 
  private boolean checkValues(List vals) {
    //
    // There may not be any blank names.  There may be blank values.  There
    // may not be duplicate names.

    ResourceManager rMan = appState_.getRMan();
    HashSet otherNames = new HashSet();
    int numRow = vals.size();  
    for (int i = 0; i < numRow; i++) {
      NameValuePairTable.TableRow tr = (NameValuePairTable.TableRow)vals.get(i);     
      EnumCell ecn = tr.name;
      String checkName = ecn.display.trim();
      if (checkName.equals("")) {
        JOptionPane.showMessageDialog(parent_, 
                                      rMan.getString("nvpEntry.nameCannotBeBlank"), 
                                      rMan.getString("nvpEntry.errorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      String normName = DataUtil.normKey(checkName);

      if (otherNames.contains(normName)) {
        JOptionPane.showMessageDialog(parent_, 
                                      rMan.getString("nvpEntry.duplicateName"), 
                                      rMan.getString("nvpEntry.errorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      otherNames.add(normName);

    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Convert the table entries to the final result
  */ 
    
  private NameValuePairList getResult(List vals) {
    NameValuePairList retval = new NameValuePairList();
    int numRow = vals.size();  
    for (int i = 0; i < numRow; i++) {
      NameValuePairTable.TableRow tr = (NameValuePairTable.TableRow)vals.get(i);           
      NameValuePair nvp = new NameValuePair(tr.name.display.trim(), tr.value.display.trim());
      retval.addNameValuePair(nvp);
    }
    return (retval);
  }
}
