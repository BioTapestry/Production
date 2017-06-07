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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport.DialogSupportClient;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;

/****************************************************************************
**
** Dialog box reporting new entities
*/

public class NewbieReportingDialog extends JDialog implements DialogSupportClient {

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
  private Map<String, Set<String>> newbies_;
  private Map<String, Map<String, String>> closest_;
  private boolean keepGoing_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  
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
  
  public NewbieReportingDialog(UIComponentSource uics, DataAccessContext dacx, JFrame parent, Map<String, Set<String>> newbies, Map<String, Map<String, String>> newbieClosest) {
    super(parent, dacx.getRMan().getString("newbieReport.dialogTitle"), true);
    uics_ = uics;
    dacx_ = dacx;
    newbies_ = newbies;
    closest_ = newbieClosest;
    keepGoing_ = false;
    ResourceManager rMan = dacx_.getRMan();
    setSize(1000, 650);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    int rowNum = 0;

    //
    // Build the tables:
    //
 
    rot_ = new ReadOnlyTable(uics_, dacx_, new NewbieReportModel(uics_, dacx_), null);   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = true;
    tp.buttons = ReadOnlyTable.NO_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString("newbieReport.tableTitle");
    tp.titleFont = null;
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(NewbieReportModel.NEWBIE_CLASS, 100, 150, 200));
    colWidths.add(new ReadOnlyTable.ColumnWidths(NewbieReportModel.NEWBIE_NAME, 200, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(NewbieReportModel.NEWBIE_ALT, 200, 200, Integer.MAX_VALUE));     
    tp.colWidths = colWidths;
    tp.canMultiSelect = false;
    JPanel tabPan = rot_.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 8;
    cp.add(tabPan, gbc);
    DialogSupport ds = new DialogSupport(this, uics_, dacx_, gbc);
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
  ** Used for the newbie issues
  */

  private class NewbieReportModel extends ReadOnlyTable.TableModel {

    final static int NEWBIE_CLASS     = 0;
    final static int NEWBIE_NAME      = 1; 
    final static int NEWBIE_ALT       = 2;
    private final static int NUM_COL_ = 3;
    
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      
      String newbieClass;
      String newbieName;
      String newbieAlt;
      
      TableRow() {   
      }
      
      TableRow(int i) {
        newbieClass = (String)columns_[NEWBIE_CLASS].get(i);
        newbieName = (String)columns_[NEWBIE_NAME].get(i);
        newbieAlt = (String)columns_[NEWBIE_ALT].get(i);
      }
      
      void toCols() {
        columns_[NEWBIE_CLASS].add(newbieClass);  
        columns_[NEWBIE_NAME].add(newbieName);
        columns_[NEWBIE_ALT].add(newbieAlt);
        return;
      }
    }

    NewbieReportModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {"newbieReport.class",
                                "newbieReport.name",
                                "newbieReport.alternative"};
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
    ((NewbieReportModel)rot_.getModel()).extractValues(tableRows);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List<NewbieReportModel.TableRow> initTableRows() {
    ArrayList<NewbieReportModel.TableRow> retval = new ArrayList<NewbieReportModel.TableRow>();
    ResourceManager rMan = dacx_.getRMan();
    NewbieReportModel ctm = (NewbieReportModel)rot_.getModel();
    Iterator<String> nbit = newbies_.keySet().iterator();
    while (nbit.hasNext()) {
      String newbieClass = nbit.next();
      Set<String> newbiesForClass = newbies_.get(newbieClass);
      Map<String, String> closestForClass = closest_.get(newbieClass);
      Iterator<String> nfcit = newbiesForClass.iterator();
      while (nfcit.hasNext()) {
        String newbie = nfcit.next();
        String suggest = null;
        if (closestForClass != null) {
          suggest = closestForClass.get(newbie);
        }
        NewbieReportModel.TableRow tr = ctm.new TableRow();
        tr.newbieClass = rMan.getString("newbieClass." + newbieClass);
        tr.newbieName = newbie;
        tr.newbieAlt = (suggest != null) ? suggest : "";
        retval.add(tr);
      }
    }
    return (retval);
  }
}
