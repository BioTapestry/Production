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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.perturb.ConditionDictionary;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for editing or creating an Experiment
*/

public class PertExperimentAddOrEditPanel extends AnimatedSplitEditPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Experiment expResult_;
  private PerturbationData pd_;
  private ArrayList<EnumCell> pertSrcList_;
  private ArrayList<EnumCell> investList_;
  
  private JTextField timeFieldForEdit_;
  private JTextField legacyMaxTimeFieldForEdit_;
  private JComboBox condsComboForEdit_;
  
  private JComboBox timeCombo_;
  private JComboBox legacyMaxTimeCombo_; 
  private JComboBox condsComboForMerge_;
  
  private EditableTable estInvForEdit_;
  private EditableTable estSrcForEdit_;
  private EditableTable estInvForMerge_;
  private EditableTable estSrcForMerge_;
  
  private String currKey_;
  private HashSet<String> allMerge_;
  private PertManageHelper pmh_;
  private TreeSet<String> timeOptions_;
  private TreeSet<String> legMaxTimeOptions_;
  
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
  
  public PertExperimentAddOrEditPanel(UIComponentSource uics, JFrame parent, PerturbationData pd,
                                      TimeCourseData tcd, PendingEditTracker pet, String myKey, 
                                      int legacyModes) {
    super(uics, parent, pet, myKey, 2);
    pd_ = pd;
    pmh_ = new PertManageHelper(uics, parent, pd, tcd, rMan_, gbc_, pet_);   
    pertSrcList_ = new ArrayList<EnumCell>();
    investList_ = new ArrayList<EnumCell>();
    
    //
    // Edit version:
    //
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout()); 
    int editRow = 0;
    JPanel[] tabPans = buildTables(false);       
    JPanel tAndCPan = buildTimeAndCond(false, legacyModes);
  
    //
    // Add the three subpanels:
    //
    
    UiUtil.gbcSet(gbc_, 0, editRow, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.5);       
    editPanel.add(tabPans[0], gbc_);
  
    UiUtil.gbcSet(gbc_, 1, editRow, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.5);       
    editPanel.add(tabPans[1], gbc_);
    editRow += 2;
    
    UiUtil.gbcSet(gbc_, 0, editRow++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    editPanel.add(tAndCPan, gbc_);
    
    //
    // Merge version:
    //
       
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout()); 
    int mergeRow = 0;
    tabPans = buildTables(true);       
    tAndCPan = buildTimeAndCond(true, legacyModes);
  
    //
    // Add the three subpanels:
    //
    
    UiUtil.gbcSet(gbc_, 0, mergeRow, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.5);       
    mergePanel.add(tabPans[0], gbc_);
  
    UiUtil.gbcSet(gbc_, 1, mergeRow, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.5);       
    mergePanel.add(tabPans[1], gbc_);
    mergeRow += 2;
    
    UiUtil.gbcSet(gbc_, 0, mergeRow++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    mergePanel.add(tAndCPan, gbc_);

    makeMultiMode(editPanel, mergePanel, 3);    
 
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Set the new experiment
  ** 
  */
  
  public void setSources(String currKey) {
    mode_ = EDIT_MODE;
    currKey_ = currKey;
    allMerge_ = null;
    displayProperties();
    return;
  }
 
  /***************************************************************************
  **
  ** Set the new experiment
  ** 
  */
  
  public void setSourcesForDup(String origKey) {
    mode_ = DUP_MODE;
    currKey_ = origKey;
    allMerge_ = null;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new experiment
  ** 
  */
  
  public String setExperimentsForMerge(List<String> joinKeys) {
    mode_ = MERGE_MODE;
    allMerge_ = new HashSet<String>(joinKeys);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> refCounts = da.getAllExperimentReferenceCounts();
    currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys);
    timeOptions_ = new TreeSet<String>();
    legMaxTimeOptions_ = new TreeSet<String>();
    int numk = joinKeys.size();
    for (int i = 0; i < numk; i++) {
      String nextJk = joinKeys.get(i);
      Experiment exp = pd_.getExperiment(nextJk);
      timeOptions_.add(Integer.toString(exp.getTime()));
      int legMax = exp.getLegacyMaxTime();
      String legString = (legMax == Experiment.NO_TIME) ? "" : Integer.toString(legMax);
      legMaxTimeOptions_.add(legString);
    }     
    displayProperties();
    return (currKey_);
  }

  /***************************************************************************
  **
  ** Get the new experiment result
  ** 
  */
  
  public Experiment getResult() {
    return (expResult_);
  }
  
  /***************************************************************************
  **
  ** Clear out the editor:
  */  
   
  @Override
  public void closeAction() {
    if (mode_ == MERGE_MODE) {
      estSrcForMerge_.stopTheEditing(false);
      estInvForMerge_.stopTheEditing(false);
    } else {
      estSrcForEdit_.stopTheEditing(false);
      estInvForEdit_.stopTheEditing(false);
    }
    super.closeAction();
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
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    pertSrcList_ = buildPertSourceEnum();
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, pertSrcList_, EnumCell.class));      
    EditableTable useTable = (mode_ == MERGE_MODE) ? estSrcForMerge_ : estSrcForEdit_;
    useTable.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)useTable.getModel()).setCurrentEnums(pertSrcList_);
       
    investList_ = buildInvestEnum();
    perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, investList_, EnumCell.class));      
    EditableTable useInvTable = (mode_ == MERGE_MODE) ? estInvForMerge_ : estInvForEdit_;
    useInvTable.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)useInvTable.getModel()).setCurrentEnums(investList_);      
            
    ConditionDictionary cd = pd_.getConditionDictionary();
    Vector<TrueObjChoiceContent> condTypes = cd.getExprConditionsOptions();
    JComboBox useCombo = (mode_ == MERGE_MODE) ? condsComboForMerge_ : condsComboForEdit_;
    UiUtil.replaceComboItems(useCombo, condTypes);
       
    if (mode_ == MERGE_MODE) {
      UiUtil.replaceComboItems(timeCombo_, new Vector<String>(timeOptions_));
      if (legacyMaxTimeCombo_ != null) {
        UiUtil.replaceComboItems(legacyMaxTimeCombo_, new Vector<String>(legMaxTimeOptions_));
      }
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Stash our UI values
  ** 
  */
  
  protected boolean stashResults() {
 
    boolean badNum = false;
    int timeVal = Experiment.NO_TIME;
    int maxLegVal = Experiment.NO_TIME;
    
    String timeText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(timeCombo_.getEditor().getEditorComponent());
      timeText = jtc.getText().trim();
    } else {
      timeText = timeFieldForEdit_.getText().trim();
    }
    
    String legacyMaxText = null;
    if (mode_ == MERGE_MODE) {
      if ((legacyMaxTimeCombo_ != null) && legacyMaxTimeCombo_.isEnabled()) {
        JTextComponent jtc = (JTextComponent)(legacyMaxTimeCombo_.getEditor().getEditorComponent());
        legacyMaxText = jtc.getText().trim();
      }       
    } else {
      if ((legacyMaxTimeFieldForEdit_ != null) && legacyMaxTimeFieldForEdit_.isEnabled()) {  
        legacyMaxText = legacyMaxTimeFieldForEdit_.getText().trim();
      }          
    }
    if ((legacyMaxText != null) && legacyMaxText.equals("")) {
      legacyMaxText = null;
    }
 
    try {
      timeVal = Integer.parseInt(timeText);
      if (timeVal < 0) {
        badNum = true;
      }
      if (legacyMaxText != null) {
        maxLegVal = Integer.parseInt(legacyMaxText);
      }
      if ((maxLegVal != Experiment.NO_TIME) && (timeVal > maxLegVal)) {
        badNum = true;
      }
    } catch (NumberFormatException nfex) {
      badNum = true;
    }
    
    if (badNum) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("peaep.badNumber"),
                                    rMan_.getString("peaep.badNumberTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    EditableTable useTable = (mode_ == MERGE_MODE) ? estSrcForMerge_ : estSrcForEdit_;
    Iterator sit = useTable.getModel().getValuesFromTable().iterator();
    if (!sit.hasNext()) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("peaep.emptySrcList"),
                                    rMan_.getString("peaep.emptySrcListTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    ArrayList<String> srcsResult = (sit.hasNext()) ? new ArrayList<String>() : null;
    while (sit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)sit.next();
      EnumCell ec = ent.enumChoice;
      if (srcsResult.contains(ec.internal)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("peaep.dupSource"),
                                      rMan_.getString("peaep.dupSourceTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }    
      srcsResult.add(ec.internal);
    }
    PertSources srcs = new PertSources(srcsResult);
    
    EditableTable useInvTable = (mode_ == MERGE_MODE) ? estInvForMerge_ : estInvForEdit_;    
    Iterator ivit = useInvTable.getModel().getValuesFromTable().iterator();
    ArrayList<String> invResult = (ivit.hasNext()) ? new ArrayList<String>() : null;
    while (ivit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)ivit.next();
      EnumCell ec = ent.enumChoice;
      if (invResult.contains(ec.internal)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("peaep.dupInvest"),
                                      rMan_.getString("peaep.dupInvestTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }    
      invResult.add(ec.internal);
    }
    
    JComboBox useCombo = (mode_ == MERGE_MODE) ? condsComboForMerge_ : condsComboForEdit_;
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)useCombo.getSelectedItem();
    String expCoKey = (String)tocc.val;
        
    if ((currKey_ == null) || (mode_ == DUP_MODE)) {  // true for dup mode too
      String nextKey = pd_.getNextDataKey();
      expResult_ = new Experiment(nextKey, srcs, timeVal, invResult, expCoKey);
      if ((mode_ == DUP_MODE) && (maxLegVal != Experiment.NO_TIME)) {
        expResult_.setLegacyMaxTime(maxLegVal);
      }      
    } else {
      expResult_ = pd_.getExperiment(currKey_).clone();
      expResult_.setTime(timeVal);
      expResult_.setInvestigators(invResult);
      expResult_.setLegacyMaxTime(maxLegVal);
      expResult_.setSources(srcs);
      expResult_.setConditionKey(expCoKey);
    }
       
    useTable.stopTheEditing(false);
    useInvTable.stopTheEditing(false);
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
  
  private JPanel[] buildTables(boolean forMerge) { 
    
    final EditableTable useSrcTable;
    if (!forMerge) {
      estSrcForEdit_ = new EditableTable(uics_, new EditableTable.OneEnumTableModel(uics_, "peaep.perturb", pertSrcList_), parent_);
      useSrcTable = estSrcForEdit_;
    } else {
      estSrcForMerge_ = new EditableTable(uics_, new EditableTable.OneEnumTableModel(uics_, "peaep.perturb", pertSrcList_), parent_);
      useSrcTable = estSrcForMerge_;
    }

    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.tableIsUnselectable = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    etp.buttonsOnSide = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, pertSrcList_, EnumCell.class));  
    JPanel srcTablePan = useSrcTable.buildEditableTable(etp);
    JPanel srcTableWithButton = pmh_.addEditButton(srcTablePan, "peaep.srcEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          String who = pmh_.getSelectedEnumVal(useSrcTable);
          pet_.jumpToRemoteEdit(PertSrcDefsManagePanel.MANAGER_KEY, PertSrcDefsManagePanel.PERT_DEF_KEY, who);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
       
    final EditableTable useInvTable;
    if (!forMerge) {
      estInvForEdit_ = new EditableTable(uics_,new EditableTable.OneEnumTableModel(uics_, "peaep.invest", investList_), parent_);
      useInvTable = estInvForEdit_;
    } else {
      estInvForMerge_ = new EditableTable(uics_, new EditableTable.OneEnumTableModel(uics_, "peaep.invest", investList_), parent_);
      useInvTable = estInvForMerge_;
    }   
    etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, investList_, EnumCell.class));   
    etp.colWidths = null;
    etp.buttonsOnSide = true;
    JPanel invTablePan = useInvTable.buildEditableTable(etp);
    JPanel invTableWithButton = pmh_.addEditButton(invTablePan, "peaep.investEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          String who = pmh_.getSelectedEnumVal(useInvTable);        
          pet_.jumpToRemoteEdit(PertInvestManagePanel.MANAGER_KEY, PertInvestManagePanel.INVEST_KEY, who);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    JPanel[] retval = new JPanel[2];
    retval[0] = srcTableWithButton;
    retval[1] = invTableWithButton;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build time and conditions for edit or merge:
  ** 
  */
  
  private JPanel buildTimeAndCond(boolean forMerge, int legacyModes) { 
  
    //
    // Set the time:
    //
    
    JPanel timeAndCondPanel = new JPanel();
    timeAndCondPanel.setBorder(BorderFactory.createEtchedBorder());
    timeAndCondPanel.setLayout(new GridBagLayout());
      
    int tacpRowNum = 0;
    JLabel timeLabel = new JLabel(rMan_.getString("peaep.setTime"));
    JComponent useComp;
    if (!forMerge) {
      timeFieldForEdit_ = new JTextField();
      useComp = timeFieldForEdit_;
    } else {
      timeCombo_ = new JComboBox();
      timeCombo_.setEditable(true);
      useComp = timeCombo_;
    }
    UiUtil.gbcSet(gbc_, 0, tacpRowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    timeAndCondPanel.add(timeLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, tacpRowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    timeAndCondPanel.add(useComp, gbc_);
     
    //
    // Prefer this to be hidden when not needed:
    //
    
    if ((legacyModes & PerturbationData.HAVE_LEGACY_TIME_SPAN) != 0x00) {
      JLabel legTimeLabel = new JLabel(rMan_.getString("peaep.setLegacyTime"));
      if (!forMerge) {
        legacyMaxTimeFieldForEdit_ = new JTextField();
        useComp = legacyMaxTimeFieldForEdit_;
      } else {
        legacyMaxTimeCombo_ = new JComboBox();
        legacyMaxTimeCombo_.setEditable(true);
        useComp = legacyMaxTimeCombo_;
      }
      UiUtil.gbcSet(gbc_, 0, tacpRowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
      timeAndCondPanel.add(legTimeLabel, gbc_);
      UiUtil.gbcSet(gbc_, 1, tacpRowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
      timeAndCondPanel.add(useComp, gbc_);
    } else {
      if (!forMerge) {
        legacyMaxTimeFieldForEdit_ = null;
      } else {
        legacyMaxTimeCombo_ = null;
      }
    }
      
    //
    // Set the experimental condition:
    //
    
    JLabel condLabel = new JLabel(rMan_.getString("peaep.setCondition"));
    Vector<TrueObjChoiceContent> condTypes = pd_.getConditionDictionary().getExprConditionsOptions();
    final JComboBox useCombo;
    if (!forMerge) {
      condsComboForEdit_ = new JComboBox(condTypes);
      useCombo = condsComboForEdit_;
    } else {
      condsComboForMerge_ = new JComboBox(condTypes);
      useCombo = condsComboForMerge_;
    }
    JButton jumpToCond = new JButton(rMan_.getString("peaep.jumpExpCond"), pmh_.getJumpIcon());
    jumpToCond.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)useCombo.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertExpSetupManagePanel.MANAGER_KEY,
                                PertExpSetupManagePanel.EXC_KEY, whichRow);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });   
    
    JPanel panWB = pmh_.componentWithJumpButton(useCombo, jumpToCond);   
        
    UiUtil.gbcSet(gbc_, 0, tacpRowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    timeAndCondPanel.add(condLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, tacpRowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    timeAndCondPanel.add(panWB, gbc_);
    return (timeAndCondPanel);
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    updateOptions();
    
    EditableTable useSrcTable = (mode_ == MERGE_MODE) ? estSrcForMerge_ : estSrcForEdit_;   
    EditableTable useInvTable = (mode_ == MERGE_MODE) ? estInvForMerge_ : estInvForEdit_;   
     
    List<EditableTable.OneEnumTableModel.TableRow> srcRows = buildSourceDisplayList();
    useSrcTable.updateTable(true, srcRows);        
    List<EditableTable.OneEnumTableModel.TableRow> invRows = buildInvestDisplayList();
    useInvTable.updateTable(true, invRows);
    
    if (currKey_ == null) {  // Gotta be edit mode...
      timeFieldForEdit_.setText("");
      UiUtil.initCombo(condsComboForEdit_);
      if (legacyMaxTimeFieldForEdit_ != null) {
        legacyMaxTimeFieldForEdit_.setEnabled(false);
      }
      cardLayout_.show(myCard_, EDIT_CARD);
      return;
    }
    Experiment exp = pd_.getExperiment(currKey_);
    ConditionDictionary cd = pd_.getConditionDictionary();
    
    switch (mode_) {
      case EDIT_MODE: 
      case DUP_MODE:       
        condsComboForEdit_.setSelectedItem(cd.getExprConditionsChoice(exp.getConditionKey()));              
        timeFieldForEdit_.setText(Integer.toString(exp.getTime()));
        if (legacyMaxTimeFieldForEdit_ != null) {
          int legMax = exp.getLegacyMaxTime();
          if (legMax != Experiment.NO_TIME) {
            legacyMaxTimeFieldForEdit_.setEnabled(true);
            legacyMaxTimeFieldForEdit_.setText(Integer.toString(legMax));    
          } else {
            legacyMaxTimeFieldForEdit_.setEnabled(false);
          }
        }
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        condsComboForMerge_.setSelectedItem(cd.getExprConditionsChoice(exp.getConditionKey()));
        timeCombo_.setSelectedItem(Integer.toString(exp.getTime()));
        if (legacyMaxTimeCombo_ != null) {
          int legMax = exp.getLegacyMaxTime();
          String legMaxStr = (legMax != Experiment.NO_TIME) ? Integer.toString(legMax) : "";
          legacyMaxTimeCombo_.setSelectedItem(legMaxStr);
          legacyMaxTimeCombo_.setEnabled((legMaxTimeOptions_.size() > 1) || (legMax != Experiment.NO_TIME));
        }
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Build the Enum of perturb source defs
  ** 
  */
  
  private ArrayList<EnumCell> buildPertSourceEnum() {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    Iterator<String> sdkit = pd_.getSourceDefKeys();
    int count = 0;
    while (sdkit.hasNext()) {
      String key = sdkit.next();
      PertSource ps = pd_.getSourceDef(key);
      String display = ps.getDisplayValueWithFootnotes(pd_, false);
      retval.add(new EnumCell(display, key, count, count));
      count++;
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Build the Enum of investigators
  ** 
  */
  
  private ArrayList<EnumCell> buildInvestEnum() {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    Iterator<String> ikit = pd_.getInvestigatorKeys();
    int count = 0;
    while (ikit.hasNext()) {
      String key = ikit.next();
      String invest = pd_.getInvestigator(key);
      retval.add(new EnumCell(invest, key, count, count));
      count++;
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get the source display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildSourceDisplayList() {
    if (currKey_ == null) {
      return (new ArrayList<EditableTable.OneEnumTableModel.TableRow>());
    }    
    if (allMerge_ == null) {
      Experiment exp = pd_.getExperiment(currKey_);
      PertSources pss = exp.getSources(); 
      Iterator<String> psit = pss.getSources();
      return (buildSourceDisplayListCore(psit, false));
    } else {
      HashSet<String> merged = new HashSet<String>();
      Iterator<String> amit = allMerge_.iterator();
      while (amit.hasNext()) {
        String nextKey = amit.next();
        Experiment exp = pd_.getExperiment(nextKey);
        PertSources pss = exp.getSources(); 
        Iterator<String> psit = pss.getSources();
        while (psit.hasNext()) {
          String psID = psit.next();
          merged.add(psID);
        }
      }
      return (buildSourceDisplayListCore(merged.iterator(), false));     
    }
  }
  
  /***************************************************************************
  **
  ** Get the source display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildSourceDisplayListCore(Iterator psit, boolean forHotUpdate) {
    ArrayList<EditableTable.OneEnumTableModel.TableRow> retval = new ArrayList<EditableTable.OneEnumTableModel.TableRow>();
    EditableTable useTable = (mode_ == MERGE_MODE) ? estSrcForMerge_ : estSrcForEdit_;   
    EditableTable.OneEnumTableModel rpt = (EditableTable.OneEnumTableModel)useTable.getModel();  
    int count = 0;
    int useIndex = -1;
    int numSrc = pertSrcList_.size();
    while (psit.hasNext()) {
      String psID;
      if (forHotUpdate) {
        psID = ((EditableTable.OneEnumTableModel.TableRow)psit.next()).enumChoice.internal;
      } else {    
        psID = (String)psit.next();
      }
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
        if (forHotUpdate) {
          continue;
        } else {
          throw new IllegalStateException();
        }
      }
      tr.enumChoice = new EnumCell(pertSrcList_.get(useIndex));
      retval.add(tr);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the investigator display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildInvestDisplayList() {
    if (currKey_ == null) {
      return (new ArrayList<EditableTable.OneEnumTableModel.TableRow>());
    }    

    if (allMerge_ == null) {
      Experiment exp = pd_.getExperiment(currKey_);
      List<String> invests = exp.getInvestigators();
      Iterator<String> iit = invests.iterator();
      return (buildInvestDisplayListCore(iit, false));
    } else {
      HashSet<String> merged = new HashSet<String>();
      Iterator<String> amit = allMerge_.iterator();
      while (amit.hasNext()) {
        String nextKey = amit.next();
        Experiment exp = pd_.getExperiment(nextKey);
        List<String> invests = exp.getInvestigators();
        Iterator<String> iit = invests.iterator();
        while (iit.hasNext()) {
          String invID = iit.next();
          merged.add(invID);
        }
      }
      return (buildInvestDisplayListCore(merged.iterator(), false));     
    }
  }
  
  /***************************************************************************
  **
  ** Get the investigator display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildInvestDisplayListCore(Iterator iit, boolean forHotUpdate) {
    ArrayList<EditableTable.OneEnumTableModel.TableRow> retval = new ArrayList<EditableTable.OneEnumTableModel.TableRow>();
    EditableTable useTable = (mode_ == MERGE_MODE) ? estInvForMerge_ : estInvForEdit_;   
    EditableTable.OneEnumTableModel rpt = (EditableTable.OneEnumTableModel)useTable.getModel(); 

    int numInv = investList_.size();
    int count = 0;
    int useIndex = -1;
    
    while (iit.hasNext()) {
      String invID;
      if (forHotUpdate) {
        invID = ((EditableTable.OneEnumTableModel.TableRow)iit.next()).enumChoice.internal;
      } else {    
        invID = (String)iit.next();
      }
      EditableTable.OneEnumTableModel.TableRow tr = rpt.new TableRow();
      tr.origOrder = new Integer(count++);
      for (int i = 0; i < numInv; i++) {
        EnumCell ecp = (EnumCell)investList_.get(i);
        if (invID.equals(ecp.internal)) {
          useIndex = i;
          break;
        }
      }
      if (useIndex == -1) {
        if (forHotUpdate) {
          continue;
        } else {
          throw new IllegalStateException();
        }
      }
      tr.enumChoice = new EnumCell((EnumCell)investList_.get(useIndex));
      retval.add(tr);
    }
    return (retval);
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

  public class MyFreezeDried implements FreezeDried {
    private String timeFieldText;
    private String legacyMaxTimeFieldText;
    private List estSrcValues;
    private List estInvValues;
    private TrueObjChoiceContent condsComboTocc;
    
    MyFreezeDried() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(timeCombo_.getEditor().getEditorComponent());
        timeFieldText = jtc.getText().trim();
        if (legacyMaxTimeCombo_ != null) {
          jtc = (JTextComponent)(legacyMaxTimeCombo_.getEditor().getEditorComponent());
          legacyMaxTimeFieldText = jtc.getText().trim();
        }
        condsComboTocc = (TrueObjChoiceContent)condsComboForMerge_.getSelectedItem();
        estSrcValues = estSrcForMerge_.getModel().getValuesFromTable();
        estInvValues = estInvForMerge_.getModel().getValuesFromTable();
      } else {
        timeFieldText = timeFieldForEdit_.getText().trim();
        if (legacyMaxTimeFieldForEdit_ != null) {
          legacyMaxTimeFieldText = legacyMaxTimeFieldForEdit_.getText();
        } 
        condsComboTocc = (TrueObjChoiceContent)condsComboForEdit_.getSelectedItem();
        estSrcValues = estSrcForEdit_.getModel().getValuesFromTable();
        estInvValues = estInvForEdit_.getModel().getValuesFromTable();
      }
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        if (pd_.getExperiment(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      List srcRows = buildSourceDisplayListCore(estSrcValues.iterator(), true);
      List invRows = buildInvestDisplayListCore(estInvValues.iterator(), true);
      
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(timeCombo_.getEditor().getEditorComponent());
        jtc.setText(timeFieldText);
        if (legacyMaxTimeCombo_ != null) {
          jtc = (JTextComponent)(legacyMaxTimeCombo_.getEditor().getEditorComponent());
          jtc.setText(legacyMaxTimeFieldText);
        }
        if (condsComboTocc != null) {
          condsComboForMerge_.setSelectedItem(pd_.getConditionDictionary().getExprConditionsChoice((String)condsComboTocc.val));
        }
        estSrcForMerge_.updateTable(true, srcRows);
        estInvForMerge_.updateTable(true, invRows);
      } else {
        timeFieldForEdit_.setText(timeFieldText);
        if (legacyMaxTimeFieldForEdit_ != null) {
          legacyMaxTimeFieldForEdit_.setText(legacyMaxTimeFieldText);
        }
        if (condsComboTocc != null) {
          condsComboForEdit_.setSelectedItem(pd_.getConditionDictionary().getExprConditionsChoice((String)condsComboTocc.val));
        }
        estSrcForEdit_.updateTable(true, srcRows);
        estInvForEdit_.updateTable(true, invRows);
      }
      return;
    }
  }
}
