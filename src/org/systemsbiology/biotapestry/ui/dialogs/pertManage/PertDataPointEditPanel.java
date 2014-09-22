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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.LegacyPert;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureProps;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for editing or creating a perturbation data point
*/

public class PertDataPointEditPanel extends AnimatedSplitEditPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private EditableTable etudf_;
  private PertDataPoint pdpResult_;
  private List userValsResult_;
  private PerturbationData pd_;
  private String currKey_;
  private List currAnnots_;
  private PerturbationData.RegionRestrict currRegRes_;
  
  private JLabel idLabel_;
  private JTextField commentField_;
  private JTextField dateField_;
  private JTextField batchKeyField_;
  private JTextField pertValField_;
  private JTextField footField_;
  private JTextField regResField_;
  private JLabel legacyLabel_;
  private JComboBox sourceCombo_;
  private JComboBox targCombo_;
  private JComboBox forceCombo_;
  private JComboBox ctrlCombo_;
  private JComboBox measureTypeCombo_;
  private PertManageHelper pmh_;
  private List userCols_;
  
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
  
  public PertDataPointEditPanel(BTState appState, DataAccessContext dacx, JFrame parent, PerturbationData pd, 
                                PendingEditTracker pet, String myKey, int legacyModes) { 
    super(appState, dacx, parent, pet, myKey, 6);
    pd_ = pd;
    pmh_ = new PertManageHelper(appState, parent, pd, rMan_, gbc_, pet_);
  
    int colNum = 0;
    // -----------------------------------------------
   
    idLabel_ = new JLabel(""); 
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(idLabel_, gbc_);    
    
    rowNum_++;
    // -----------------------------------------------
    colNum = 0;
    
    colNum = 0;
    
    JLabel srcLabel = new JLabel(rMan_.getString("pertDataPointEdit.source"));
    sourceCombo_ = new JComboBox();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(srcLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(sourceCombo_, gbc_);
    colNum += 4;
     
    JButton addNewSource = new JButton(rMan_.getString("pertDataPointEdit.manageSources"), pmh_.getJumpIcon());
    addNewSource.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)sourceCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertExperimentManagePanel.MANAGER_KEY,
                                PertExperimentManagePanel.SOURCES_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    add(addNewSource, gbc_);
    
    rowNum_++;
    // -----------------------------------------------
    colNum = 0;
    
    JLabel targLabel = new JLabel(rMan_.getString("pertDataPointEdit.targ"));
    targCombo_ = new JComboBox();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(targLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(targCombo_, gbc_);
    colNum += 4;
    
    JButton addNewTarget = new JButton(rMan_.getString("pertDataPointEdit.manageTargets"), pmh_.getJumpIcon());
    addNewTarget.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)targCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertSrcsAndTargsManagePanel.MANAGER_KEY,
                                PertSrcsAndTargsManagePanel.TRG_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    colNum += 2;
    add(addNewTarget, gbc_);
 
    rowNum_++;
    // -----------------------------------------------
    colNum = 0;
    
    JLabel valueLabel = new JLabel(rMan_.getString("pertDataPointEdit.value"));
    pertValField_ = new JTextField();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(valueLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(pertValField_, gbc_);
   
    JLabel measureLabel = new JLabel(rMan_.getString("pertDataPointEdit.meaType"));
    measureTypeCombo_ = new JComboBox();
    JButton mtJump = new JButton(rMan_.getString("pdpe.jump"), pmh_.getJumpIcon());
    mtJump.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)measureTypeCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertMeasurementManagePanel.MANAGER_KEY,
                                PertMeasurementManagePanel.MEAS_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    JPanel mtCombo = pmh_.componentWithJumpButton(measureTypeCombo_, mtJump);
 
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(measureLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(mtCombo, gbc_);
       
    Vector sigOps = PertDataPoint.getSignificanceOptions(appState_);
    JLabel forceLabel = new JLabel(rMan_.getString("pertDataPointEdit.forceSig"));
    forceCombo_ = new JComboBox(sigOps);
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(forceLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(forceCombo_, gbc_);
    
    rowNum_++;
    
    // -----------------------------------------------
    
    if ((legacyModes & PerturbationData.HAVE_LEGACY_PERT) != 0x00) {
      JLabel dummyLabel = new JLabel("");
      UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      add(dummyLabel, gbc_);
      legacyLabel_ = new JLabel("");
      UiUtil.gbcSet(gbc_, 1, rowNum_++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      add(legacyLabel_, gbc_);
    }
        
    // -----------------------------------------------    
    colNum = 0;
    
    JLabel batchLabel = new JLabel(rMan_.getString("pertDataPointEdit.batch"));
    batchKeyField_ = new JTextField();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(batchLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(batchKeyField_, gbc_);     
    
    // -----------------------------------------------
    colNum = 0;
    
    JLabel dateLabel = new JLabel(rMan_.getString("pertDataPointEdit.date"));
    dateField_ = new JTextField();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(dateLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(dateField_, gbc_);  
     
    JLabel ctrlLabel = new JLabel(rMan_.getString("pertDataPointEdit.ctrl"));
    ctrlCombo_ = new JComboBox();
    JButton ctrlJump = new JButton(rMan_.getString("pdpe.jump"), pmh_.getJumpIcon());
    ctrlJump.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)ctrlCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertExpSetupManagePanel.MANAGER_KEY,
                                PertExpSetupManagePanel.CONTROL_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    JPanel combo = pmh_.componentWithJumpButton(ctrlCombo_, ctrlJump);
    
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(ctrlLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(combo, gbc_);
    
    JLabel commentLabel = new JLabel(rMan_.getString("pertDataPointEdit.comment"));
    commentField_ = new JTextField();
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(commentLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(commentField_, gbc_);
    
    JLabel footLabel = new JLabel(rMan_.getString("pertDataPointEdit.footnotes"));
    footField_ = new JTextField();
    footField_.setEditable(false);
    FixedJButton editFoots = new FixedJButton(rMan_.getString("pdpe.editAnnotations"));
    editFoots.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          pet_.editIsPushed(PertDataPointManagePanel.DATA_ANNOT_KEY);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
 
    colNum = 0;
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(footLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(footField_, gbc_);
    colNum += 4;
    UiUtil.gbcSet(gbc_, colNum, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    colNum += 2;
    add(editFoots, gbc_);        
    
    JLabel regLabel = new JLabel(rMan_.getString("pdpe.regRestrict"));
    regResField_ = new JTextField();
    regResField_.setEditable(false);
    FixedJButton editRegRes = new FixedJButton(rMan_.getString("pdpe.editRegRestrict"));
    editRegRes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          pet_.editIsPushed(PertDataPointManagePanel.REG_RESTRICT_KEY);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    colNum = 0;
    UiUtil.gbcSet(gbc_, colNum++, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(regLabel, gbc_);
    UiUtil.gbcSet(gbc_, colNum, rowNum_, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(regResField_, gbc_);
    colNum += 4;
    UiUtil.gbcSet(gbc_, colNum, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    colNum += 2;
    add(editRegRes, gbc_);    

    JLabel ufLabel = new JLabel(rMan_.getString("pertDataPointEdit.userFields"));
    //
    // Build the values table tabs.
    //

    userCols_ = buildUserColumns(pd);
    etudf_ = new EditableTable(appState_, new UserFieldsTableModel(appState_, userCols_, pd.getUserFieldCount() > 0), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.tableIsUnselectable = true;
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.noScroll = true;
    JPanel userFieldsTablePan = etudf_.buildEditableTable(etp);
    JPanel userFieldsWithButton = pmh_.addEditButton(userFieldsTablePan, "pdpe.UDFEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          pet_.jumpToRemoteEdit(PertMiscSetupManagePanel.MANAGER_KEY, PertMiscSetupManagePanel.UDF_KEY, null);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
       
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(ufLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, rowNum_, 5, 2, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum_ += 2;
    add(userFieldsWithButton, gbc_);

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
    etudf_.stopTheEditing(false);
    super.closeAction();
    return;
  }
    
  /***************************************************************************
  **
  ** Set the new data point
  ** 
  */
  
  public void setDataPoint(String pointKey) {
    mode_ = EDIT_MODE;
    currKey_ = pointKey;
    PertDataPoint pdp = (currKey_ == null) ? null : pd_.getDataPoint(currKey_); 
    currAnnots_ = (pdp == null) ? null : pd_.getDataPointNotes(currKey_);
    currRegRes_ = (pdp == null) ? null : pd_.getRegionRestrictionForDataPoint(currKey_);
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new data point for duplication
  ** 
  */
  
  public void setDataPointForDup(String origKey) {
    mode_ = DUP_MODE;
    currKey_ = origKey;
    PertDataPoint pdp = pd_.getDataPoint(currKey_); 
    currAnnots_ = (pdp == null) ? null : pd_.getDataPointNotes(currKey_);
    currRegRes_ = (pdp == null) ? null : pd_.getRegionRestrictionForDataPoint(currKey_);
    displayProperties();
    return;
  }
    
  /***************************************************************************
  **
  ** Update with edited annotation list
  ** 
  */
  
  public void updateAnnotations(List annotations) {
    currAnnots_ = (annotations == null) ? null : new ArrayList(annotations);
    //
    // Doing a full-blown displayProperties loses pending data!
    //
    footField_.setText((currAnnots_ == null) ? "" : pd_.getFootnoteListAsNVString(currAnnots_));
    footField_.setCaretPosition(0);
    return;
  }
  
  /***************************************************************************
  **
  ** Update with edited region restrictions
  ** 
  */
  
  public void updateRegionRestriction(PerturbationData.RegionRestrict regRes) {
    currRegRes_ = regRes;
    //
    // Doing a full-blown displayProperties loses pending data!
    //
    regResField_.setText((currRegRes_ == null) ? "" : currRegRes_.getDisplayValue());
    regResField_.setCaretPosition(0);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the new data point
  ** 
  */
  
  public PertDataPoint getResult() {
    return (pdpResult_);
  }
  
  /***************************************************************************
  **
  ** Get updated annotations
  ** 
  */
  
  public List getUpdatedAnnotResult() {
    return (currAnnots_);
  }
  
  /***************************************************************************
  **
  ** Get updated region restrictions
  ** 
  */
  
  public PerturbationData.RegionRestrict getUpdatedRegionRestrictionResult() {
    return (currRegRes_);
  }
  
  /***************************************************************************
  **
  ** Get the user values:
  ** 
  */
  
  public List getUserVals() {
    return (userValsResult_);
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
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    Vector srcVec = pd_.getExperimentOptions();
    UiUtil.replaceComboItems(sourceCombo_, srcVec);
    
    Vector targVec = pd_.getTargetOptions(true);
    UiUtil.replaceComboItems(targCombo_, targVec);
  
    Vector measureTypes = pd_.getMeasureDictionary().getMeasurementOptions();
    UiUtil.replaceComboItems(measureTypeCombo_, measureTypes);
       
    Vector sigOps = PertDataPoint.getSignificanceOptions(appState_);
    UiUtil.replaceComboItems(forceCombo_, sigOps);
    
    Vector ctrlOps = pd_.getConditionDictionary().getExprControlOptions();
    TrueObjChoiceContent tocc = new TrueObjChoiceContent(rMan_.getString("pdpe.notSpecified"), null);
    ctrlOps.add(0, tocc);   
    UiUtil.replaceComboItems(ctrlCombo_, ctrlOps);
    
    userCols_ = buildUserColumns(pd_);
    ((UserFieldsTableModel)etudf_.getModel()).resetColumns(userCols_, pd_.getUserFieldCount() > 0);
    return;
  }
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashResults() {
  
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)measureTypeCombo_.getSelectedItem();
    if (tocc == null) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.emptyMeasure"),
                                    rMan_.getString("pdpe.emptyMeasureTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }          
    String newMeasure = (String)tocc.val;
    
    tocc = (TrueObjChoiceContent)forceCombo_.getSelectedItem();
    Boolean newForce = (Boolean)tocc.val;    
    
    //
    // Check that the value works:
    //
    
    LegacyPert lpForDup = null;
    boolean legacyCasePasses = false;
    if (currKey_ != null) {
      PertDataPoint pdp = pd_.getDataPoint(currKey_);
      LegacyPert lp = pdp.getLegacyPert();
      if (lp != null) {
        String pvft = pertValField_.getText().trim();
        if (DataUtil.keysEqual(lp.oldValue, pvft)) {
          boolean illegalChange = false;
          Boolean currForce = pdp.getForcedSignificance();
          if (newForce == null) {
            if (currForce != null) {
              illegalChange = true;
            }
          } else if (!newForce.equals(currForce)) {
            illegalChange = true;
          }           
          if (!illegalChange && !newMeasure.equals(pdp.getMeasurementTypeKey())) {
            illegalChange = true;
          }
          if (illegalChange) {
            JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.illegalLegacyChange"),
                                          rMan_.getString("pdpe.illegalLegacyChangeTitle"), 
                                          JOptionPane.ERROR_MESSAGE);
            return (false);   
          }
          if (mode_ == DUP_MODE) {
            lpForDup = (LegacyPert)lp.clone();
          }
          legacyCasePasses = true;
        } else if (lp.unknownMultiCount) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.cannotModifyMultiCount"),
                                        rMan_.getString("pdpe.cannotModifyMultiCountTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);   
        }
      }
    }
    
    Double newVal = null;
    if (!legacyCasePasses) {
      newVal = getValue();
      if (newVal == null) {
        return (false);
      }
      MeasureDictionary mdict = pd_.getMeasureDictionary();
      MeasureProps mps = mdict.getMeasureProps(newMeasure);
      MeasureScale ms = mdict.getMeasureScale(mps.getScaleKey());
      BoundedDoubMinMax illegal = ms.getIllegalRange();
      if (illegal != null) {
        if (illegal.contained(newVal.doubleValue())) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.valueOutOfRangeForScale"),
                                        rMan_.getString("pdpe.valueOutOfRangeForScaleTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
           return (false);
        }
      }
    }
    
    //
    // Do we ALSO want to check for batch key overlaps??
    //
    
    String newBatch = batchKeyField_.getText().trim();
    if ((newBatch == null) || newBatch.equals("")) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.emptyBatchKey"),
                                    rMan_.getString("pdpe.emptyBatchKeyTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }     
  
    tocc = (TrueObjChoiceContent)sourceCombo_.getSelectedItem();
    if (tocc == null) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.emptySrc"),
                                    rMan_.getString("pdpe.emptySrcTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }     
    String newExprKey = (String)tocc.val;
  
    tocc = (TrueObjChoiceContent)targCombo_.getSelectedItem();
    if (tocc == null) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pdpe.emptyTarg"),
                                    rMan_.getString("pdpe.emptyTargTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }      
    String newTarg = (String)tocc.val;
       
    tocc = (TrueObjChoiceContent)ctrlCombo_.getSelectedItem();
    String newCtrlKey = (String)tocc.val;  // may be null! 
  
    String newComment = commentField_.getText().trim();
    if (newComment.equals("")) {
      newComment = null;
    }
  
    String newDate = dateField_.getText().trim();
    if (newDate.equals("")) {
      newDate = null;
    }

    PertDataPoint pdp;
    if ((currKey_ == null) || (mode_ == DUP_MODE)) {
      long stamp = System.currentTimeMillis();
      String newKey = pd_.getNextDataKey();
      pdpResult_ = new PertDataPoint(newKey, stamp, newExprKey, newTarg, newMeasure, 0.0);
      if (legacyCasePasses) {
        pdpResult_.setLegacyPert(lpForDup);
      } else {
        pdpResult_.setLegacyPert(null);
        pdpResult_.setValue(newVal.doubleValue());
      }
    } else {
      pdpResult_ = (PertDataPoint)(pd_.getDataPoint(currKey_).clone());
      pdpResult_.setExperimentKey(newExprKey);
      pdpResult_.setTargetKey(newTarg);
      pdpResult_.setMeasurementTypeKey(newMeasure);
      if (legacyCasePasses) {
        pdpResult_.setLegacyPert((LegacyPert)pdpResult_.getLegacyPert().clone());
      } else {
        pdpResult_.setLegacyPert(null);
        pdpResult_.setValue(newVal.doubleValue());
      }
    }
    pdpResult_.setIsSig(newForce);
    pdpResult_.setBatchKey(newBatch);
    pdpResult_.setDate(newDate);
    pdpResult_.setComment(newComment);
    pdpResult_.setControl(newCtrlKey);
    int fc = pd_.getUserFieldCount();
    if (fc == 0) {
       userValsResult_ = null;
    } else {
      userValsResult_ = new ArrayList();
      List vals = etudf_.getValuesFromTable();
      UserFieldsTableModel.TableRow row = (UserFieldsTableModel.TableRow)vals.get(0);
      for (int i = 0; i < fc; i++) {
        userValsResult_.add(row.values[i]);        
      }      
    }
    
    
    if (mode_ == DUP_MODE) {
      PertDataPoint pdpOrig = pd_.getDataPoint(currKey_);
      if (pdpResult_.willFallInSameBatchWithSameVal(pdpOrig)) {
        int ok = JOptionPane.showConfirmDialog(parent_, rMan_.getString("pdpe.identicalDup"),
                                               rMan_.getString("pdpe.identicalDupTitle"), 
                                               JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return (false);
        }
      }
    }
 
    etudf_.stopTheEditing(false);
    return (true);
  }    
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Columns for user fields
  */
  
  private ArrayList buildUserColumns(PerturbationData pd) {
    ArrayList retval = new ArrayList();
    Iterator ufit = pd.getUserFieldNames();
    while (ufit.hasNext()) {
      String field = (String)ufit.next();
      retval.add(field);
    }
    if (retval.isEmpty()) {
      retval.add(rMan_.getString("pdpep.noFieldsDefined"));
    }
    return (retval);
  }
      
  /***************************************************************************
  **
  ** Values for user fields
  */
  
  private ArrayList buildUserVals(PerturbationData pd, PertDataPoint pdp) {
    
    ArrayList loadVals = new ArrayList();
    int count = pd.getUserFieldCount();
    String pdpID = (pdp == null) ? null : pdp.getID();
    if (count > 0) {
      for (int i = 0; i < count; i++) {
        String val = (pdpID == null) ? "" : pd.getUserFieldValue(pdpID, i);
        loadVals.add((val == null) ? "" : val);
      }
    } else {
      loadVals.add("");
    }

    UserFieldsTableModel uftm = (UserFieldsTableModel)etudf_.getModel();   
    UserFieldsTableModel.TableRow tr = uftm.new TableRow();
    tr.load(loadVals);
    
    ArrayList retval = new ArrayList();
    retval.add(tr);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {

    updateOptions();
     
    if ((mode_ == EDIT_MODE) && (currKey_ == null)) {
      idLabel_.setText(rMan_.getString("pdpae.newDataPoint"));
      UiUtil.initCombo(sourceCombo_);
      UiUtil.initCombo(targCombo_);
      UiUtil.initCombo(measureTypeCombo_);
      pertValField_.setText("");
      commentField_.setText("");
      UiUtil.initCombo(forceCombo_);
      UiUtil.initCombo(ctrlCombo_);
      dateField_.setText("");
      batchKeyField_.setText(""); 
      footField_.setText("");
      footField_.setCaretPosition(0);
      if (legacyLabel_ != null) {
        legacyLabel_.setText("");
        legacyLabel_.invalidate(); 
      }
      regResField_.setText("");
      regResField_.setCaretPosition(0);
      measureTypeCombo_.setEnabled(true);
      forceCombo_.setEnabled(true);
      List loadVals = buildUserVals(pd_, null);
      etudf_.updateTable(true, loadVals);
      validate();
      return;
    }
        
    PertDataPoint pdp = pd_.getDataPoint(currKey_);
    
    String format  = rMan_.getString("pertDataPointEdit.numTimeFormat");
    String linkMsg = MessageFormat.format(format, new Object[] {pdp.getID(), 
                                                                DateFormat.getDateInstance().format(new Date(pdp.getTimeStamp()))});
    
    idLabel_.setText(linkMsg);

    TrueObjChoiceContent tocc = pdp.getExperiment(pd_).getChoiceContent(pd_);
    sourceCombo_.setSelectedItem(tocc);
  
    TrueObjChoiceContent tocct = pd_.getTargetChoiceContent(pdp.getTargetKey(), true);
    targCombo_.setSelectedItem(tocct);
 
    LegacyPert lp = pdp.getLegacyPert();
    if (lp != null) {
      pertValField_.setText(lp.oldValue);
      if (lp.unknownMultiCount) {
        legacyLabel_.setText(rMan_.getString("pdpe.legacyUnknownMultiCount"));
      } else {
        legacyLabel_.setText("");
      }
      legacyLabel_.invalidate();
      measureTypeCombo_.setEnabled(!lp.unknownMultiCount);
      forceCombo_.setEnabled(!lp.unknownMultiCount);
    } else {
      pertValField_.setText(Double.toString(pdp.getValue()));
      measureTypeCombo_.setEnabled(true);
      forceCombo_.setEnabled(true);
    }
 
    String comment = pdp.getComment();
    commentField_.setText((comment == null) ? "" : comment);
   
    TrueObjChoiceContent toccM = pd_.getMeasureDictionary().getMeasurementChoice(pdp.getMeasurementTypeKey());
    measureTypeCombo_.setSelectedItem(toccM);
      
    TrueObjChoiceContent toccf = pdp.getForcedSignificanceChoice(appState_);
    forceCombo_.setSelectedItem(toccf);
   
    String curr = pdp.getControl();
    if (curr == null) {
      UiUtil.initCombo(ctrlCombo_);
    } else {
      TrueObjChoiceContent toccCtrl = pd_.getConditionDictionary().getExprControlChoice(curr);
      ctrlCombo_.setSelectedItem(toccCtrl);
    }
  
    String date = pdp.getDate();
    dateField_.setText((date == null) ? "" : date);
 
    batchKeyField_.setText(pdp.getBatchKey()); 
    
    footField_.setText((currAnnots_ == null) ? "" : pd_.getFootnoteListAsNVString(currAnnots_));
    footField_.setCaretPosition(0);
    
    regResField_.setText((currRegRes_ == null) ? "" : currRegRes_.getDisplayValue());
    regResField_.setCaretPosition(0);
    
    List loadVals = buildUserVals(pd_, pdp);
    etudf_.updateTable(true, loadVals);
    validate();
    
    return;
  }
  
  /***************************************************************************
  **
  ** Used to parse out value
  */
  
  private Double getValue() {     
    String newLevelTxt = pertValField_.getText();
    boolean badNum = false;
    double newVal = 0.0;
    if ((newLevelTxt == null) || newLevelTxt.trim().equals("")) {
      badNum = true;
    } else {
      try {
        newVal = Double.parseDouble(newLevelTxt);
      } catch (NumberFormatException nfe) {
        badNum = true;
      }
    }
    if (badNum) {   
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(parent_, 
                                    rMan.getString("pdpe.badValueNumber"), 
                                    rMan.getString("pdpe.badValueNumberTitle"),
                                    JOptionPane.ERROR_MESSAGE);         
      return (null);            
    }
 
    return (new Double(newVal));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** User fields
  */

  static class UserFieldsTableModel extends EditableTable.TableModel {
    
    public class TableRow {
      public String[] values;

      TableRow() {
      }
      
      void load(List vals) {
        int numVal = vals.size();
        values = new String[numVal];
        for (int i = 0; i < numVal; i++) {
          values[i] = (String)vals.get(i);
        }
        return;
      }
      
      TableRow(int numCols) {
        values = new String[numCols];
        for (int i = 0; i < numCols; i++) {
          values[i] = (String)columns_[i].get(0);
        }
      }
      
      void toCols() {
        for (int i = 0; i < values.length; i++) {
          columns_[i].add(values[i]);
        }
        return;
      }
    }
 
    public UserFieldsTableModel(BTState appState, List columns, boolean haveFields) {
      super(appState, columns.size());
      int numCol = columns_.length;
      colNames_ = new String[numCol];
      colClasses_ = new Class[numCol];
      canEdit_ = new boolean[numCol];
      for (int i = 0; i < numCol; i++) {
        colNames_[i] = (String)columns.get(i);
        colClasses_[i] = String.class;
        canEdit_[i] = haveFields;
      }
    }
    
    public void resetColumns(List columns, boolean haveFields) {
      resetColumnCount(columns.size());
      int numCol = columns_.length;
      colNames_ = new String[numCol];
      colClasses_ = new Class[numCol];
      canEdit_ = new boolean[numCol];
      for (int i = 0; i < numCol; i++) {
        colNames_[i] = (String)columns.get(i);
        colClasses_[i] = String.class;
        canEdit_[i] = haveFields;
      }
      fireTableStructureChanged();
      return;
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      if (rowCount_ != 1) {
        throw new IllegalStateException();
      }
      TableRow ent = new TableRow(columns_.length);
      retval.add(ent);
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
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {
   
    private List frozenAnnots;
    private PerturbationData.RegionRestrict frozenRegRes;
    private String idLabelText;
    private String commentFieldText;
    private String dateFieldText;
    private String batchKeyFieldText;
    private String pertValFieldText;
    private String legacyLabelText;
    private TrueObjChoiceContent sourceComboTocc;
    private TrueObjChoiceContent targComboTocc;
    private TrueObjChoiceContent forceComboTocc;
    private TrueObjChoiceContent ctrlComboTocc;
    private TrueObjChoiceContent measureTypeComboTocc;   
    private List userValues;
    private List uvNames;
    
    
    MyFreezeDried() {    
      idLabelText = idLabel_.getText();
      commentFieldText = commentField_.getText();
      dateFieldText =  dateField_.getText();
      batchKeyFieldText = batchKeyField_.getText();
      pertValFieldText = pertValField_.getText();
      if (legacyLabel_ != null) {
        legacyLabelText = legacyLabel_.getText();
      }
      sourceComboTocc = (TrueObjChoiceContent)sourceCombo_.getSelectedItem();
      targComboTocc = (TrueObjChoiceContent)targCombo_.getSelectedItem();
      forceComboTocc = (TrueObjChoiceContent)forceCombo_.getSelectedItem();
      ctrlComboTocc = (TrueObjChoiceContent)ctrlCombo_.getSelectedItem();
      measureTypeComboTocc = (TrueObjChoiceContent)measureTypeCombo_.getSelectedItem();
      uvNames = new ArrayList(userCols_);
      int fc = uvNames.size();
      if (fc == 0) {
        userValues = null;
      } else {
        userValues = etudf_.getModel().getValuesFromTable();
      }
      frozenAnnots = (currAnnots_ == null) ? null : new ArrayList(currAnnots_);
      frozenRegRes = (currRegRes_ == null) ? null : (PerturbationData.RegionRestrict)currRegRes_.clone();
    }
    
    public boolean needToCancel() {
      if (currKey_ != null) {
        if (pd_.getDataPoint(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      
      idLabel_.setText(idLabelText);
      commentField_.setText(commentFieldText);
      dateField_.setText(dateFieldText);
      batchKeyField_.setText(batchKeyFieldText);
      pertValField_.setText(pertValFieldText);
      if (legacyLabel_ != null) {
        legacyLabel_.setText(legacyLabelText);
      }
      
      if (sourceComboTocc != null) {
        sourceCombo_.setSelectedItem(pd_.getExperimentChoice((String)sourceComboTocc.val));
      }
      if (targComboTocc != null) {
        targCombo_.setSelectedItem(pd_.getTargetChoiceContent((String)targComboTocc.val, true));
      }
      forceCombo_.setSelectedItem(forceComboTocc);
      if (measureTypeComboTocc != null) {
        TrueObjChoiceContent toccM = pd_.getMeasureDictionary().getMeasurementChoice((String)measureTypeComboTocc.val);
        measureTypeCombo_.setSelectedItem(toccM); 
      }   
      if ((ctrlComboTocc == null) || (ctrlComboTocc.val == null)) {
        UiUtil.initCombo(ctrlCombo_);
      } else {
        TrueObjChoiceContent toccCtrl = pd_.getConditionDictionary().getExprControlChoice((String)ctrlComboTocc.val);
        ctrlCombo_.setSelectedItem(toccCtrl);
      }
      
      recoverUserNameData();
      
      //
      // Gotta update stashed footnotes:
      //
      
      currAnnots_ = pmh_.fixupFrozenAnnot(frozenAnnots);
      footField_.setText((currAnnots_ == null) ? "" : pd_.getFootnoteListAsNVString(currAnnots_));
      footField_.setCaretPosition(0);
    
      //
      // Same with region restrictions:
      //
      
      currRegRes_ = pmh_.fixupFrozenRegRes(frozenRegRes);
      regResField_.setText((currRegRes_ == null) ? "" : currRegRes_.getDisplayValue());
      regResField_.setCaretPosition(0);
      return;
    }
      
    private void recoverUserNameData() {  
      int fc = pd_.getUserFieldCount();
      ArrayList loadVals = new ArrayList();
      if (fc == 0) {
        loadVals.add("");
      } else {
        String[] valList = ((UserFieldsTableModel.TableRow)userValues.get(0)).values;
        int savedNameCount = uvNames.size();
        boolean editAtMost = (fc == savedNameCount);
        SortedSet notHit = (new MinMax(0, fc - 1)).getAsSortedSet();
        int danglingDataIndex = -1;
        HashMap indexToVal = new HashMap();     
        for (int i = 0; i < savedNameCount; i++) {
          String oldName = (String)uvNames.get(i);
          Integer index = pd_.getUserFieldIndexFromNameAsInt(oldName);
          if (index == null) {
            if (editAtMost) {
              danglingDataIndex = i;
            }
          } else {
            indexToVal.put(index, valList[i]);
            notHit.remove(index);
          }
        }
  
        for (int i = 0; i < fc; i++) {          
          String val = (String)indexToVal.get(new Integer(i));
          if (indexToVal == null) {
            val = (String)uvNames.get(danglingDataIndex);
          }
          loadVals.add((val == null) ? "" : val);
        }
      }
      
      UserFieldsTableModel uftm = (UserFieldsTableModel)etudf_.getModel();   
      UserFieldsTableModel.TableRow tr = uftm.new TableRow();
      tr.load(loadVals);
      ArrayList invRows = new ArrayList();
      invRows.add(tr);
      etudf_.updateTable(true, invRows);
      return;
    }
  }
}
