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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for creating or editing a perturbation target
*/

public class PertTargetAddOrEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JTextField nameFieldForEdit_;
  private JComboBox nameCombo_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private String currKey_;
  private PertManageHelper pmh_;
  private String nameResult_;
  private List annotResult_;
  private EditableTable estAnnotForEdit_;
  private EditableTable estAnnotForMerge_;
  private ArrayList<EnumCell> annotList_;
  private PertSimpleNameEditPanel.Client client_;
  private List allMerge_;
  private String copyName_;
  private SortedSet nameOptions_;
  
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
  
  public PertTargetAddOrEditPanel(UIComponentSource uics,
                                  JFrame parent, PerturbationData pd, TimeCourseData tcd, PendingEditTracker pet, 
                                  PertSimpleNameEditPanel.Client client, String myKey) { 
    super(uics, parent, pet, myKey, 4);
    pd_ = pd;
    tcd_ = tcd;
    pmh_ = new PertManageHelper(uics_, parent, pd_, tcd_, rMan_, gbc_, pet_);
    client_ = client;
    annotList_ = new ArrayList<EnumCell>();
    
    //
    // Edit version:
    //
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout()); 
    JLabel label = new JLabel(rMan_.getString("ptae.targName"));
    int editRow = 0;
    UiUtil.gbcSet(gbc_, 0, editRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    editPanel.add(label, gbc_);   
    nameFieldForEdit_ = new JTextField();    
    UiUtil.gbcSet(gbc_, 3, editRow++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    editPanel.add(nameFieldForEdit_, gbc_);

    JPanel annotTableWithButton = buildAnnotTable(false); 
    UiUtil.gbcSet(gbc_, 0, editRow, 4, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    editRow += 2;
    editPanel.add(annotTableWithButton, gbc_);
       
    //
    // Merge version:
    //
       
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout()); 
    JLabel labelToo = new JLabel(rMan_.getString("ptae.targName"));
    int mergeRow = 0;
    UiUtil.gbcSet(gbc_, 0, mergeRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    mergePanel.add(labelToo, gbc_);   
    nameCombo_ = new JComboBox();
    nameCombo_.setEditable(true);
    UiUtil.gbcSet(gbc_, 3, mergeRow++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    mergePanel.add(nameCombo_, gbc_);

    JPanel annotTableWithButtonForMerge = buildAnnotTable(true);
    UiUtil.gbcSet(gbc_, 0, mergeRow, 4, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    mergeRow += 2;
    mergePanel.add(annotTableWithButtonForMerge, gbc_);
         
    makeMultiMode(editPanel, mergePanel, 4);    
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
  /***************************************************************************
  **
  ** Clear out the editor:
  */  
   
  public void closeAction() {
    if (mode_ == MERGE_MODE) {
      estAnnotForMerge_.stopTheEditing(false);
    } else {
      estAnnotForEdit_.stopTheEditing(false);
    }
    super.closeAction();
    return;
  }  
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public String getTargetName() {
    return (nameResult_);
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public List getTargetAnnots() {
    return (annotResult_);
  }  
  
  /***************************************************************************
  **
  ** Set the new target
  ** 
  */
  
  public void setTarget(String currKey) {
    mode_ = EDIT_MODE;
    currKey_ = currKey;
    displayProperties();
    return;
  }
 
  /***************************************************************************
  **
  ** Set the new target for dup
  ** 
  */
  
  public void setDupTarget(String origKey, String copyName) {
    mode_ = DUP_MODE;
    currKey_ = origKey;
    copyName_ = copyName;
    displayProperties();
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the new target for merge
  ** 
  */
  
  public void setMergeTarget(String origKey, List allMerge, SortedSet nameOptions) {
    mode_ = MERGE_MODE;
    currKey_ = origKey;
    allMerge_ = allMerge;
    nameOptions_ = nameOptions;    
    displayProperties();
    return;
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  } 
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  protected boolean stashResults() {
 
    String newText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
      newText = jtc.getText().trim();
    } else {
      newText = nameFieldForEdit_.getText().trim();
    }   
 
    if ((newText == null) || newText.equals("")) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("snep.emptyName"),
                                    rMan_.getString("snep.emptyNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }     
    
    if (client_.haveDuplication(myKey_, currKey_, newText)) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("snep.duplicateName"),
                                    rMan_.getString("snep.duplicateNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    if (mode_ == MERGE_MODE) {
      int numDisconnect = 0;
      String exampOldName = null;
      int numM = allMerge_.size();
      for (int i = 0; i < numM; i++) {
        String nextKey = (String)allMerge_.get(i);
        if (client_.haveDisconnect(myKey_, nextKey, newText)) {
          numDisconnect++;
          if (exampOldName == null) {
            exampOldName = client_.getNameForKey(myKey_, nextKey);
          }
        }
      }      
      if (numDisconnect > 0) {
        String desc = UiUtil.convertMessageToHtml(MessageFormat.format(rMan_.getString("snep.disconnectingMultiRow"), 
                                                  new Object[] {new Integer(numDisconnect), exampOldName, newText}));
        int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                                 rMan_.getString("snep.disconnectingTitle"),
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (doit != JOptionPane.OK_OPTION) {
          return (false);
        }
      }
    } else {   
      if (client_.haveDisconnect(myKey_, currKey_, newText)) {
        String oldName = client_.getNameForKey(myKey_, currKey_);
        String desc = UiUtil.convertMessageToHtml(MessageFormat.format(rMan_.getString("snep.disconnectingRow"), 
                                                  new Object[] {oldName, newText}));
        int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                                 rMan_.getString("snep.disconnectingTitle"),
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (doit != JOptionPane.OK_OPTION) {
          return (false);
        }
      }
    }
    nameResult_ = newText;
 
    //
    // Footnotes:
    //

    EditableTable useTable = (mode_ == MERGE_MODE) ? estAnnotForMerge_ : estAnnotForEdit_;
    Iterator tdit = useTable.getModel().getValuesFromTable().iterator();
    annotResult_ = (tdit.hasNext()) ? new ArrayList() : null;
    while (tdit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)tdit.next();
      EnumCell ec = ent.enumChoice;
      annotResult_.add(ec.internal);
    }
    useTable.stopTheEditing(false);
    
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Update current options
  ** 
  */
  
  protected void updateOptions() {
    annotList_ = pmh_.buildAnnotEnum();
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, annotList_, EnumCell.class));      
    EditableTable useTable = (mode_ == MERGE_MODE) ? estAnnotForMerge_ : estAnnotForEdit_;
    useTable.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)useTable.getModel()).setCurrentEnums(annotList_);         
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build an annotation table.
  */ 
  
  private JPanel buildAnnotTable(boolean forMerge) {   
    final EditableTable useTable;
    if (!forMerge) {
      estAnnotForEdit_ = new EditableTable(uics_, new EditableTable.OneEnumTableModel(uics_, "ptae.annot", annotList_), parent_);
      useTable = estAnnotForEdit_;
    } else {
      estAnnotForMerge_ = new EditableTable(uics_, new EditableTable.OneEnumTableModel(uics_, "ptae.annot", annotList_), parent_);
      useTable = estAnnotForMerge_;
    }
    EditableTable.TableParams etp = pmh_.tableParamsForAnnot(annotList_);
    JPanel annotTablePan = useTable.buildEditableTable(etp);
    JPanel annotTableWithButton = pmh_.addEditButton(annotTablePan, "ptae.annotEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          String who = pmh_.getSelectedEnumVal(useTable);        
          pet_.jumpToRemoteEdit(PertAnnotManagePanel.MANAGER_KEY, PertAnnotManagePanel.ANNOT_KEY, who);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    return (annotTableWithButton);    
  }
  
  /***************************************************************************
  **
  ** Apply the current property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    
    updateOptions();
    
    switch (mode_) {
      case EDIT_MODE: 
        if (currKey_ == null) {
          nameFieldForEdit_.setText("");
          List annotRows = pmh_.buildAnnotDisplayList(new ArrayList(), estAnnotForEdit_, annotList_, false);
          estAnnotForEdit_.updateTable(true, annotRows);
          return;
        }
        nameFieldForEdit_.setText(pd_.getTarget(currKey_));    
        List anids = pd_.getFootnotesForTarget(currKey_);
        List annotRows = pmh_.buildAnnotDisplayList(anids, estAnnotForEdit_, annotList_, false);
        estAnnotForEdit_.updateTable(true, annotRows);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case DUP_MODE:
        nameFieldForEdit_.setText(copyName_);
        // Use the footnotes from the original!
        anids = pd_.getFootnotesForTarget(currKey_);
        annotRows = pmh_.buildAnnotDisplayList(anids, estAnnotForEdit_, annotList_, false);
        estAnnotForEdit_.updateTable(true, annotRows);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        UiUtil.replaceComboItems(nameCombo_, new Vector(nameOptions_));
        nameCombo_.setSelectedItem(pd_.getTarget(currKey_));
        int numMerge = allMerge_.size();    
        TreeSet combined = new TreeSet();
        for (int i = 0; i < numMerge; i++) {
          String tkey = (String)allMerge_.get(i);
          anids = pd_.getFootnotesForTarget(tkey);
          if (anids != null) {
            combined.addAll(anids);
          }
        }  
        annotRows = pmh_.buildAnnotDisplayList(new ArrayList(combined), estAnnotForMerge_, annotList_, false);
        estAnnotForMerge_.updateTable(true, annotRows);
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  private class MyFreezeDried implements FreezeDried {
    
    private String nameFieldText;
    private List estAnnotValues;
    
    MyFreezeDried() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
        nameFieldText = jtc.getText().trim();
        estAnnotValues = estAnnotForMerge_.getModel().getValuesFromTable();
      } else {
        nameFieldText = nameFieldForEdit_.getText().trim();
        estAnnotValues = estAnnotForEdit_.getModel().getValuesFromTable();
      } 
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        if (client_.getNameForKey(myKey_, currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    } 
 
    public void reInstall() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
        jtc.setText(nameFieldText);
        List annotRows = pmh_.buildAnnotDisplayList(estAnnotValues, estAnnotForMerge_, annotList_, true);
        estAnnotForMerge_.updateTable(true, annotRows);    
      } else {
        nameFieldForEdit_.setText(nameFieldText);
        List annotRows = pmh_.buildAnnotDisplayList(estAnnotValues, estAnnotForEdit_, annotList_, true);
        estAnnotForEdit_.updateTable(true, annotRows);      
      }
      return;
    }
  }
}
