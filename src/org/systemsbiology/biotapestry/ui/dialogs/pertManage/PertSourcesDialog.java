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

import java.awt.Dimension;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Panel for editing or creating an Experiment
*/

public class PertSourcesDialog extends BTStashResultsDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private PerturbationData pd_;
  private HashSet<PertSources> existingSrcs_;
  private PertSources currSources_;
  private ArrayList<EnumCell> pertSrcList_;  
  private EditableTable estSrcForEdit_;
  private PertSources result_;
  
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
  
  public PertSourcesDialog(UIComponentSource uics, DataAccessContext dacx, PertSources currSources, Iterator<PertSources> existing) {
    super(uics, dacx, "setPertSources.title", new Dimension(500, 300), 1);
    currSources_ = currSources;
    existingSrcs_ = new HashSet<PertSources>();
    while (existing.hasNext()) {
      PertSources eps = existing.next();
      if ((currSources != null) && eps.equals(currSources)) {
        continue;
      }
      existingSrcs_.add(eps);     
    }
    pd_ = dacx.getExpDataSrc().getPertData();
    pertSrcList_ = buildPertSourceEnum();
    JPanel tabPan = buildTable();
    addTable(tabPan, 5);
    finishConstruction();
    displayProperties();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the new result
  ** 
  */
  
  public PertSources getResult() {
    return (result_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    pertSrcList_ = buildPertSourceEnum();
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, pertSrcList_, EnumCell.class));      
    estSrcForEdit_.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)estSrcForEdit_.getModel()).setCurrentEnums(pertSrcList_);
    return;
  }

  /***************************************************************************
  **
  ** Do the stashing 
  ** 
  */
  
  protected boolean stashForOK() {
  
    estSrcForEdit_.stopTheEditing(false);
    Iterator sit = estSrcForEdit_.getModel().getValuesFromTable().iterator();
    if (!sit.hasNext()) {
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan_.getString("setPertSources.emptySrcList"),
                                    rMan_.getString("setPertSources.emptySrcListTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    ArrayList<String> srcsResult = (sit.hasNext()) ? new ArrayList<String>() : null;
    while (sit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)sit.next();
      EnumCell ec = ent.enumChoice;
      if (srcsResult.contains(ec.internal)) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan_.getString("setPertSources.dupSource"),
                                      rMan_.getString("setPertSources.dupSourceTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }    
      srcsResult.add(ec.internal);
    }
    result_ = new PertSources(srcsResult);
    
    if (existingSrcs_.contains(result_)) {
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan_.getString("setPertSources.dupOfExistingSource"),
                                    rMan_.getString("setPertSources.dupOfExistingSourceTitle"), 
                                    JOptionPane.ERROR_MESSAGE); 
      result_ = null;
      return (false);
    } 

    return (true);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build tables for edit or merge
  ** 
  */
  
  private JPanel buildTable() {    
    estSrcForEdit_ = new EditableTable(uics_, dacx_, new EditableTable.OneEnumTableModel(uics_, dacx_, "peaep.perturb", pertSrcList_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.tableIsUnselectable = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    etp.buttonsOnSide = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, pertSrcList_, EnumCell.class));  
    JPanel srcTablePan = estSrcForEdit_.buildEditableTable(etp);
    return (srcTablePan);
  }
   
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    updateOptions(); 
    List srcRows = buildSourceDisplayList();
    estSrcForEdit_.updateTable(true, srcRows);
    return;
  }
  
  /***************************************************************************
  **
  ** Build the Enum of perturb source defs
  ** 
  */
  
  private ArrayList<EnumCell> buildPertSourceEnum() {
    TreeMap<String, List<String>> sorter = new TreeMap<String, List<String>>();
    Iterator<String> sdkit = pd_.getSourceDefKeys();
    while (sdkit.hasNext()) {
      String key = sdkit.next();
      PertSource ps = pd_.getSourceDef(key);
      String display = ps.getDisplayValueWithFootnotes(pd_, false);
      List<String> perName = sorter.get(display);
      if (perName == null) {
        perName = new ArrayList<String>();
        sorter.put(display, perName);
      }
      perName.add(key);
    }
      
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    int count = 0;
    Iterator<String> vit = sorter.keySet().iterator();
    while (vit.hasNext()) {
      String display = vit.next();
      List<String> perName = sorter.get(display);
      int pns = perName.size();
      for (int i = 0; i < pns; i++) {
        String key = perName.get(i);
        retval.add(new EnumCell(display, key, count, count));
      }
      count++;
    }
     
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the source display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildSourceDisplayList() {
    ArrayList<EditableTable.OneEnumTableModel.TableRow> retval = new ArrayList<EditableTable.OneEnumTableModel.TableRow>();
    if (currSources_ == null) {
      return (retval);
    }
    Iterator<String> psit = currSources_.getSources(); 
    EditableTable.OneEnumTableModel rpt = (EditableTable.OneEnumTableModel)estSrcForEdit_.getModel();  
    int count = 0;
    int useIndex = -1;
    int numSrc = pertSrcList_.size();
    while (psit.hasNext()) {
      String psID = psit.next();
      EditableTable.OneEnumTableModel.TableRow tr = rpt.new TableRow();
      tr.origOrder = new Integer(count++);
      for (int i = 0; i < numSrc; i++) {
        EnumCell ecp = (EnumCell)pertSrcList_.get(i);
        if (psID.equals(ecp.internal)) {
          useIndex = i;
          break;
        }
      }
      if (useIndex == -1) {
        throw new IllegalStateException();
      }
      tr.enumChoice = new EnumCell((EnumCell)pertSrcList_.get(useIndex));
      retval.add(tr);
    }
    return (retval);
  } 
}
