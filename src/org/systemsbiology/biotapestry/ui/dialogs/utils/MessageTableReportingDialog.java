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

import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for reporting a table of messages in a warning:
*/

public class MessageTableReportingDialog extends JDialog implements DialogSupport.DialogSupportClient {

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

  private ReadOnlyTable rot_;
  private List messages_;
  private boolean keepGoing_;
  private BTState appState_;
  
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
  
  public MessageTableReportingDialog(BTState appState, List messages, String title, 
                                     String tableLabel, String colLabel, Dimension dim, boolean showCancel, boolean modal) {
    super(appState.getTopFrame(), appState.getRMan().getString(title), modal);
    appState_ = appState;
    messages_ = messages;
    ResourceManager rMan = appState_.getRMan();
    setSize(dim.width, dim.height);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    int rowNum = 0;

    //
    // Build the tables:
    //
 
    rot_ = new ReadOnlyTable(appState_, new CheckoutTableModel(appState_, colLabel), null);   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = true;
    tp.tableIsUnselectable = true;
    tp.buttons = ReadOnlyTable.NO_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString(tableLabel);
    tp.titleFont = null;
    JPanel tabPan = rot_.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 8;
    cp.add(tabPan, gbc);
   
    if (showCancel) {
      DialogSupport ds = new DialogSupport(this, appState_, gbc);
      ds.buildAndInstallCenteredButtonBox(cp, rowNum, 1, false, true);
    } else {
      DialogSupport ds = new DialogSupport(appState_, gbc, this);
      ds.buildAndInstallCloseButtonBox(cp, rowNum, 1, null);
    }
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties(); 
  }
  
  /***************************************************************************
  **
  ** Button actions
  */
  
  public void applyAction() {
    throw new UnsupportedOperationException();
  }

  public void okAction() {
    keepGoing_ = true;
    setVisible(false);
    dispose();
    return;
  }

  public void closeAction() {
    keepGoing_ = false;
    setVisible(false);
    dispose();
    return;
  }
  
  /***************************************************************************
  **
  ** Cancel or continue
  */
  
  public boolean keepGoing() {
    return (keepGoing_);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for the message table
  */

  class CheckoutTableModel extends ReadOnlyTable.TableModel {
    
    final static int MESSAGE          = 0;
    private final static int NUM_COL_ = 1; 
    
    private static final long serialVersionUID = 1L;

    class TableRow {
      
      String message;
      
      TableRow() {   
      }
      
      TableRow(int i) {
        message = (String)columns_[MESSAGE].get(i);
      }
      
      void toCols() {
        columns_[MESSAGE].add(message);  
        return;
      }
    }
    
    CheckoutTableModel(BTState appState, String title) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {title};
    }
    
    List getValuesFromTable() {
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
   ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the data values to our property table UI component
  ** 
  */
  
  private void displayProperties() {
    rot_.rowElements.clear(); 
    rot_.rowElements.addAll(initTableRows());
    rot_.updateTable(true, -1);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List initTableRows() {
    ArrayList retval = new ArrayList();
    CheckoutTableModel ctm = (CheckoutTableModel)rot_.getModel();  
    int numMsg = messages_.size();
    for (int i = 0; i < numMsg; i++) {
      CheckoutTableModel.TableRow tr = ctm.new TableRow();
      tr.message = (String)messages_.get(i);
      retval.add(tr);
    }
    return (retval);
  }
}
