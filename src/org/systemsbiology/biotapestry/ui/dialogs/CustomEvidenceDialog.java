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


import java.awt.Dimension;
import javax.swing.JPanel;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.CustomEvidenceDrawStyle;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for editing custom evidence
*/

public class CustomEvidenceDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private EditableTable est_;
  private SortedMap<Integer, CustomEvidenceDrawStyle> returnMap_;
  private HarnessBuilder hBld_;
  private ColorResolver cRes_;
  
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
  
  public CustomEvidenceDialog(UIComponentSource uics, ColorResolver cRes, HarnessBuilder hBld, SortedMap<Integer, CustomEvidenceDrawStyle> evidenceMap) {
    super(uics, "custEvi.title", new Dimension(800, 400), 1);
    hBld_ = hBld;
    cRes_ = cRes;
    returnMap_ = new TreeMap<Integer, CustomEvidenceDrawStyle>();
    Iterator<Integer> ceit = evidenceMap.keySet().iterator();
    while (ceit.hasNext()) {
      Integer level = ceit.next();
      CustomEvidenceDrawStyle ceds = evidenceMap.get(level);
      returnMap_.put(level, ceds.clone()); 
    }   
 
    est_ = new EditableTable(uics_, new EvidenceCustomDrawTableModel(uics_), uics_.getTopFrame());
    est_.setEditButtonHandler(new ButtonHand());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = true;
    etp.buttons = EditableTable.EDIT_BUTTON;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = null;
    etp.colWidths = new ArrayList<EditableTable.ColumnWidths>();  
    etp.colWidths.add(new EditableTable.ColumnWidths(EvidenceCustomDrawTableModel.LEVEL, 50, 100, 200));   
    etp.colWidths.add(new EditableTable.ColumnWidths(EvidenceCustomDrawTableModel.DIAMOND, 50, 100, 200));
    etp.colWidths.add(new EditableTable.ColumnWidths(EvidenceCustomDrawTableModel.DRAW_DESC, 200, 500, 2000));
   
    JPanel tablePan = est_.buildEditableTable(etp);
    addTable(tablePan, 8);
     
    finishConstruction();
    displayProperties();
  }
  /***************************************************************************
  **
  ** Get the result
  ** 
  */
  
  public SortedMap<Integer, CustomEvidenceDrawStyle> getNewEvidenceMap() {
    return (returnMap_);
  }   
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class EvidenceCustomDrawTableModel extends EditableTable.TableModel {
    
    final static int LEVEL            = 0;
    final static int DIAMOND          = 1;
    final static int DRAW_DESC        = 2;
    private final static int NUM_COL_ = 3;
    
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      Integer level;
      Boolean diamond;
      String desc;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        level = (Integer)columns_[LEVEL].get(i);
        diamond = (Boolean)columns_[DIAMOND].get(i);
        desc = (String)columns_[DRAW_DESC].get(i);
      }
      
      void toCols() {
        columns_[LEVEL].add(level);  
        columns_[DIAMOND].add(diamond);
        columns_[DRAW_DESC].add(desc);
        return;
      }
      
      void replaceCols(int row) {
        columns_[LEVEL].set(row, level);  
        columns_[DIAMOND].set(row, diamond);
        columns_[DRAW_DESC].set(row, desc);
        return;
      }  
    }
 
    EvidenceCustomDrawTableModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"cetable.level",
                                "cetable.diamond",
                                "cetable.drawDesc"};
      colClasses_ = new Class[] {Integer.class,
                                 Boolean.class, 
                                 String.class};
      canEdit_ = new boolean[] {false,
                                true,
                                false};
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
  ** Used for tracking button presses
  */

  private class ButtonHand implements EditableTable.EditButtonHandler {    
    public void pressed() {
      int[] sel = est_.getSelectedRows();
      if (sel.length != 1) {
        throw new IllegalArgumentException();
      }
      EvidenceCustomDrawTableModel ecdtm = (EvidenceCustomDrawTableModel)est_.getModel();
      List vals = ecdtm.getValuesFromTable();
      EvidenceCustomDrawTableModel.TableRow tr = (EvidenceCustomDrawTableModel.TableRow)vals.get(sel[0]);
      
      CustomEvidenceDrawStyle ceds = returnMap_.get(tr.level);
      PerLinkDrawStyle oldPlds = (ceds == null) ? null : ceds.getDrawStyle();
      LinkSpecialPropsDialog lspd = new LinkSpecialPropsDialog(uics_, cRes_, hBld_, oldPlds);
      lspd.setVisible(true);      
      if (lspd.haveResult()) {        
        PerLinkDrawStyle plds = lspd.getProps();
        if (ceds == null) {
          boolean doDiamond = tr.diamond.booleanValue();
          ceds = new CustomEvidenceDrawStyle(tr.level.intValue(), doDiamond, plds);
          returnMap_.put(tr.level, ceds);
        }
        ceds.setDrawStyle(plds);
        ResourceManager rMan = uics_.getRMan();
        String noSpec = rMan.getString("lptable.noSpecLinkSty");
        if (plds == null) {
          tr.desc = noSpec;        
        } else {
          tr.desc = plds.getDisplayString(uics_.getRMan(), cRes_); 
        }
        tr.replaceCols(sel[0]);
        ecdtm.fireTableDataChanged();
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
    List<EvidenceCustomDrawTableModel.TableRow> tableRows = initTableRows();
    ((EvidenceCustomDrawTableModel)est_.getModel()).extractValues(tableRows);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List<EvidenceCustomDrawTableModel.TableRow> initTableRows() {
    ArrayList<EvidenceCustomDrawTableModel.TableRow> retval = new ArrayList<EvidenceCustomDrawTableModel.TableRow>();
    EvidenceCustomDrawTableModel ecdtm = (EvidenceCustomDrawTableModel)est_.getModel();  
    String noSpec = uics_.getRMan().getString("lptable.noSpecLinkSty");
         
    for (int i = Linkage.LEVEL_1; i <= Linkage.MAX_LEVEL; i++) {
      EvidenceCustomDrawTableModel.TableRow tr = ecdtm.new TableRow();
      tr.level = new Integer(i);
      CustomEvidenceDrawStyle ceds = returnMap_.get(tr.level);
      if (ceds == null) {
        tr.diamond = new Boolean(true);
        tr.desc = noSpec;        
      } else {
        tr.diamond = new Boolean(ceds.showDiamonds());
        PerLinkDrawStyle plds = ceds.getDrawStyle();
        if (plds == null) {
          tr.desc = noSpec;        
        } else {
          tr.desc = plds.getDisplayString(uics_.getRMan(), cRes_);
        }          
      }
      retval.add(tr);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Stash our results for later interrogation.
  ** 
  */
  
  protected boolean stashForOK() {
    EvidenceCustomDrawTableModel ecdtm = (EvidenceCustomDrawTableModel)est_.getModel(); 
    List vals = ecdtm.getValuesFromTable();
    int numVals = vals.size();

    // We already maintain the per-link draw style.  Just collect up diamond booleans.
    for (int i = 0; i < numVals; i++) {
      EvidenceCustomDrawTableModel.TableRow tr = (EvidenceCustomDrawTableModel.TableRow)vals.get(i);
      boolean doDiamond = tr.diamond.booleanValue();
      Integer level = tr.level;
      CustomEvidenceDrawStyle ceds = returnMap_.get(level);
      //
      // If no custom evidence, and doDiamond is true, there is nothing special about
      // the level, and we add nothing to the table.  If false, we add the custom evidence.
      // If there is, and a special link property, we add the diamond info.  If there
      // is, with no special property, we drop if diamond is true, and add diamond is false.
      //
      if (ceds == null) {
        if (!doDiamond) {
          ceds = new CustomEvidenceDrawStyle(level.intValue(), doDiamond, null);
          returnMap_.put(level, ceds);
        } // else do nothing...
      } else {
        PerLinkDrawStyle plds = ceds.getDrawStyle();
        if (plds == null) {
          if (doDiamond) {
            returnMap_.remove(level);
          } else {
            ceds.setShowDiamonds(false);
          }
        } else {
          ceds.setShowDiamonds(doDiamond);
        }
      }
    }
    return (true);
  }  
}
