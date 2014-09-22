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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.perturb.BatchCollision;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport.DialogSupportClient;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;

/****************************************************************************
**
** Dialog box reporting duplicated batch IDs
*/

public class BatchDupReportDialog extends JDialog implements DialogSupportClient {

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
  private Map batchDups_;
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
  
  public BatchDupReportDialog(BTState appState, JFrame parent, Map batchDups, String windowTitle, String tableTitle) {
    super(parent, appState.getRMan().getString(windowTitle), true);
    appState_ = appState;
    batchDups_ = batchDups;
    keepGoing_ = false;
    ResourceManager rMan = appState_.getRMan();
    setSize(1000, 650);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    int rowNum = 0;

    //
    // Build the tables:
    //
 
    rot_ = new ReadOnlyTable(appState_, new BatchDupReportModel(appState_), null);   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = true;
    tp.buttons = ReadOnlyTable.NO_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString(tableTitle);
    tp.titleFont = null;
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(BatchDupReportModel.EXPERIMENT_DESC, 100, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(BatchDupReportModel.TARGET_NAME, 100, 150, 200));
    colWidths.add(new ReadOnlyTable.ColumnWidths(BatchDupReportModel.BATCH_NAME, 200, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(BatchDupReportModel.DATA_VALS, 200, 200, Integer.MAX_VALUE));     
    tp.colWidths = colWidths;
    tp.canMultiSelect = false;
    JPanel tabPan = rot_.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 8;
    cp.add(tabPan, gbc);
    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallCenteredButtonBox(cp, rowNum, 1, false, true); 
    setLocationRelativeTo(parent);
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
  ** Used for the batch duplication report
  */

  private class BatchDupReportModel extends ReadOnlyTable.TableModel {

    final static int EXPERIMENT_DESC  = 0;
    final static int TARGET_NAME      = 1;
    final static int BATCH_NAME       = 2; 
    final static int DATA_VALS        = 3;
    private final static int NUM_COL_ = 4;   
    
    class TableRow {
      
      String expDesc;
      String targetName;
      String batchName;
      String dataVals;
      
      TableRow() {   
      }
      
      TableRow(int i) {
        expDesc = (String)columns_[EXPERIMENT_DESC].get(i);
        targetName = (String)columns_[TARGET_NAME].get(i);
        batchName = (String)columns_[BATCH_NAME].get(i);
        dataVals = (String)columns_[DATA_VALS].get(i);
      }
      
      void toCols() {
        columns_[EXPERIMENT_DESC].add(expDesc);
        columns_[TARGET_NAME].add(targetName);
        columns_[BATCH_NAME].add(batchName);
        columns_[DATA_VALS].add(dataVals);
        return;
      }
    }

    BatchDupReportModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"batchDupReport.expDesc",
                                "batchDupReport.target",
                                "batchDupReport.batchID",
                                "batchDupReport.dataVals"};
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
    List tableRows = initTableRows();
    // NO! Supposed to update table, not call extractValues directly, correct?? Fix this as shown?
    // rot_.updateTable(true, -1);
    ((BatchDupReportModel)rot_.getModel()).extractValues(tableRows);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List initTableRows() {
    StringBuffer buf = new StringBuffer();
    ArrayList retval = new ArrayList();
    BatchDupReportModel ctm = (BatchDupReportModel)rot_.getModel();
    Iterator nbit = batchDups_.keySet().iterator();
    while (nbit.hasNext()) {
      String expDesc = (String)nbit.next();
      Map dups = (Map)batchDups_.get(expDesc);
      Iterator tidit = dups.keySet().iterator();
      while (tidit.hasNext()) {
        String targetID = (String)tidit.next();
        Map batches = (Map)dups.get(targetID);
        Iterator bidit = batches.keySet().iterator();
        while (bidit.hasNext()) {
          String batchID = (String)bidit.next();
          BatchCollision bc = (BatchCollision)batches.get(batchID);        
          BatchDupReportModel.TableRow tr = ctm.new TableRow();
          tr.expDesc = bc.expDesc;
          tr.targetName = bc.targetName;
          tr.batchName = bc.batchID;
          buf.setLength(0);
          int numVal = bc.vals.size();
          for (int i = 0; i < numVal; i++) {
            buf.append((String)bc.vals.get(i));
            if (i < (numVal - 1)) {
              buf.append(", ");
            }
          }
          tr.dataVals = buf.toString();
          retval.add(tr);
        }
      }
    }
    return (retval);
  }
}
