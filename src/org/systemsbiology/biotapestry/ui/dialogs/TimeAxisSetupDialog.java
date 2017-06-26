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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;

/****************************************************************************
**
** Dialog box for specifying regions used in build instructions
*/

public class TimeAxisSetupDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private EditableTable est_;
  private TimeAxisDefinition workingCopy_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  private UndoFactory uFac_;
  private JComboBox unitCombo_;
  private JTextField customUnitsField_;
  private JTextField customUnitsAbbrevField_;
  private JCheckBox isSuffixCheckBox_;  
  private JLabel customUnitsLabel_;
  private JLabel customUnitsAbbrevLabel_;  
  private JLabel stagesLabel_;
  private boolean canChange_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Wrapper on wrapper on constructor.  Have to define time axis before we can define
  ** topo structure.
  */ 
  
  public static boolean timeAxisSetupDialogWrapperWrapper(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac) {
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics, dacx, uFac);
      tasd.setVisible(true);
    }
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = dacx.getRMan();
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("tcsedit.noTimeDefinition"), 
                                    rMan.getString("tcsedit.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Wrapper on constructor.  Have to see if guts are editable first
  */ 
  
  public static TimeAxisSetupDialog timeAxisSetupDialogWrapper(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac) {
    
    //
    // Figure out if we can change the time units anymore:
    //
    
    boolean canChange = true;
    UiUtil.fixMePrintout("Gotta check all pert data sets");
    PerturbationData pd = dacx.getExpDataSrc().getPertData();    
    TimeCourseData tcdat = dacx.getExpDataSrc().getTimeCourseData();
    TemporalInputRangeData tirdat = dacx.getTemporalRangeSrc().getTemporalInputRangeData(); 
    if (pd.haveData() || pd.getPertDisplayOptions().hasColumns() || pd.getPertDisplayOptions().hasDefaultTimeSpan() ||
        ((tcdat != null) && tcdat.hasGeneTemplate()) || tirdat.haveData() || dacx.getGenomeSource().modelsHaveTimeBounds()) {
      canChange = false;
    }    
    
    if (!canChange) {
      ResourceManager rMan = dacx.getRMan();
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("timeAxisDialog.cannotChange"), 
                                    rMan.getString("timeAxisDialog.cannotChangeTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    
    TimeAxisSetupDialog qsd = new TimeAxisSetupDialog(uics, dacx, uFac, canChange);
    return (qsd);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 

  private TimeAxisSetupDialog(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, boolean canChange) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("timeAxisDialog.title"), true);
    canChange_ = canChange;
    dacx_ = dacx;
    uics_ = uics;
    uFac_ = uFac;
    ResourceManager rMan = dacx_.getRMan();
    setSize(600, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    workingCopy_ = dacx_.getExpDataSrc().getTimeAxisDefinition().clone();
    if (!workingCopy_.isInitialized()) {
      workingCopy_.setToDefault();
    }
    
    JLabel unitsLabel = new JLabel(rMan.getString("timeAxisDialog.units"));
    unitCombo_ = new JComboBox(TimeAxisDefinition.getUnitTypeChoices(uics_.getRMan()));
    unitCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (canChange_) {
            int units = ((ChoiceContent)unitCombo_.getSelectedItem()).val;
            enableCustomAndNamed(units);
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    unitCombo_.setEnabled(canChange_);
    unitsLabel.setEnabled(canChange_);
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(unitsLabel, gbc);
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(unitCombo_, gbc);
    
    customUnitsLabel_ = new JLabel(rMan.getString("timeAxisDialog.customUnits"));
    customUnitsField_ = new JTextField();
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(customUnitsLabel_, gbc);
    UiUtil.gbcSet(gbc, 1, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(customUnitsField_, gbc);

    customUnitsAbbrevLabel_ = new JLabel(rMan.getString("timeAxisDialog.customUnitAbbrev"));
    customUnitsAbbrevField_ = new JTextField();
    
    isSuffixCheckBox_ = new JCheckBox(rMan.getString("timeAxisDialog.isSuffix"));
    
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(customUnitsAbbrevLabel_, gbc);
    UiUtil.gbcSet(gbc, 1, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(customUnitsAbbrevField_, gbc);    
    UiUtil.gbcSet(gbc, 2, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(isSuffixCheckBox_, gbc);    
 
    stagesLabel_ = new JLabel(rMan.getString("timeAxisDialog.namedStages"));
    
    UiUtil.gbcSet(gbc, 0, 3, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(stagesLabel_, gbc);    
          
    //
    // Build the stages table.
    //
    
    est_ = new EditableTable(uics_, new TimeAxisSetupTableModel(uics_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);
    
    UiUtil.gbcSet(gbc, 0, 4, 3, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
    
    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    ds.buildAndInstallButtonBox(cp, 12, 3, false, false);
    
    displayProperties();
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (!canChange_ || applyProperties()) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    setVisible(false);
    dispose();
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Enable Named Stages and/or custom units
  ** 
  */
  
  private void enableCustomAndNamed(int units) {    
    boolean activateUnits = TimeAxisDefinition.wantsCustomUnits(units);
    boolean activateStages = TimeAxisDefinition.wantsNamedStages(units);
    enableCustomAndNamedCore(activateUnits, activateStages);
    return;
  }
  
  /***************************************************************************
  **
  ** Enable Named Stages and/or custom units
  ** 
  */
  
  private void enableCustomAndNamedCore(boolean activateUnits, boolean activateStages) {       
    customUnitsLabel_.setEnabled(activateUnits);
    customUnitsField_.setEnabled(activateUnits);
    customUnitsAbbrevLabel_.setEnabled(activateUnits);
    customUnitsAbbrevField_.setEnabled(activateUnits);
    isSuffixCheckBox_.setEnabled(activateUnits);
    stagesLabel_.setEnabled(activateStages);   
    est_.setEnabled(activateStages);
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** The table
  */

  class TimeAxisSetupTableModel extends EditableTable.TableModel {

    private final static int STAGE_   = 0;
    private final static int ABBREV_  = 1; 
    private final static int NUM_COL_ = 2;   
    
    TimeAxisSetupTableModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"timeAxisDialog.stage",
                                "timeAxisDialog.abbrev"};
      colClasses_ = new Class[] {String.class,
                                 String.class};
    }
    
    public List getValuesFromTable() {
      ArrayList<TimeAxisDefinition.NamedStage> retval = new ArrayList<TimeAxisDefinition.NamedStage>();
      for (int i = 0; i < this.rowCount_; i++) {
        TimeAxisDefinition.NamedStage ns  = 
          new TimeAxisDefinition.NamedStage((String)columns_[STAGE_].get(i), 
                                            (String)columns_[ABBREV_].get(i), i);
        retval.add(ns);
      }
      return (retval);
    }
     
    @Override
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        TimeAxisDefinition.NamedStage ns = (TimeAxisDefinition.NamedStage)rit.next();
        columns_[STAGE_].add(ns.name);
        columns_[ABBREV_].add(ns.abbrev);
      }
      return;
    }
    
    List<TimeAxisDefinition.NamedStage> applyValues() {
      List<TimeAxisDefinition.NamedStage> vals = getValuesFromTable();
      if (vals.isEmpty()) {
        return (null);
      }
      
      //
      // Make sure the stages are OK.  Stage names must be unique and non-blank.  Same with
      // abbreviations, which must be short (<= 3 chars)
      //
      
      ResourceManager rMan = uics_.getRMan();
      ArrayList<TimeAxisDefinition.NamedStage> seenStages = new ArrayList<TimeAxisDefinition.NamedStage>();
      ArrayList<String> seenNames = new ArrayList<String>();
      ArrayList<String> seenAbbrevs = new ArrayList<String>();      
      int size = rowCount_;
      if (size == 0) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.mustHaveStage"),
                                      rMan.getString("timeAxisDialog.badStageTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (null);
      }
      
      for (int i = 0; i < size; i++) {
        TimeAxisDefinition.NamedStage ns = vals.get(i);
        String name = ns.name;        
        if ((name == null) || (name.trim().equals(""))) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.badStage"),
                                        rMan.getString("timeAxisDialog.badStageTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (null);
        }
        
        name = name.trim();
        
        if (DataUtil.containsKey(seenNames, name)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.dupStage"),
                                        rMan.getString("timeAxisDialog.badStageTitle"),
                                        JOptionPane.ERROR_MESSAGE);           
            
          return (null);
        }
        
        seenNames.add(name);
                
        String abbrev = ns.abbrev;   
        if ((abbrev == null) || (abbrev.trim().equals(""))) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.badAbbrev"),
                                        rMan.getString("timeAxisDialog.badStageTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (null);
        }
        
        abbrev = abbrev.trim();
        
        if (abbrev.length() > 3) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.longAbbrev"),
                                        rMan.getString("timeAxisDialog.badStageTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (null);
        }        

        if (DataUtil.containsKey(seenAbbrevs, abbrev)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.dupAbbrev"),
                                        rMan.getString("timeAxisDialog.badStageTitle"),
                                        JOptionPane.ERROR_MESSAGE);           
            
          return (null);
        }
        
        seenAbbrevs.add(abbrev);
        seenStages.add(new TimeAxisDefinition.NamedStage(name, abbrev, i));
      }
      return (seenStages);
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {

    int units = workingCopy_.getUnits();
    unitCombo_.setSelectedItem(TimeAxisDefinition.unitTypeForCombo(uics_.getRMan(), workingCopy_.getUnits()));
    
    if (workingCopy_.haveCustomUnits()) {
      String customUnits = workingCopy_.getUserUnitName();
      customUnitsField_.setText(customUnits);
      String customUnitAbbrev = workingCopy_.getUserUnitAbbrev();
      customUnitsAbbrevField_.setText(customUnitAbbrev);
      isSuffixCheckBox_.setSelected(workingCopy_.unitsAreASuffix());
    } else {
      isSuffixCheckBox_.setSelected(true);  // disabled, but default if activated
    }
      
    List namedStages = workingCopy_.haveNamedStages() ? workingCopy_.getNamedStages() : new ArrayList();
    est_.getModel().extractValues(namedStages) ;  

    if (!canChange_) {
      enableCustomAndNamedCore(false, false);       
    } else {
      enableCustomAndNamed(units);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply our UI values to the time axis definition
  */
  
  private boolean applyProperties() {
    
    if (!canChange_) {
      return (true);
    }
    ResourceManager rMan = uics_.getRMan();
    
    //
    // Undo/Redo support
    //
   
    UndoSupport support = uFac_.provideUndoSupport("undo.timeAxisSpec", dacx_); 

    int units = ((ChoiceContent)unitCombo_.getSelectedItem()).val;
    
    String customUnits = null;
    String customUnitAbbrev = null;
    boolean isSuffix = true;
    if (TimeAxisDefinition.wantsCustomUnits(units)) {
      customUnits = customUnitsField_.getText();
      customUnitAbbrev = customUnitsAbbrevField_.getText();
      isSuffix = isSuffixCheckBox_.isSelected();
      if (customUnits.trim().equals("")) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisDialog.blankUnits"),
                                      rMan.getString("timeAxisDialog.blankUnitsTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      if (customUnitAbbrev.trim().equals("")) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("timeAxisResourceManager.getManager();Dialog.blankUnitAbbrev"),
                                      rMan.getString("timeAxisDialog.blankUnitsAbbrevTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }  
    }
 
    //
    // May bounce if the user specifies bad stage definitions
    //
    
    List<TimeAxisDefinition.NamedStage> stageResults = null;
    if (TimeAxisDefinition.wantsNamedStages(units)) {
      stageResults = ((TimeAxisSetupTableModel)est_.getModel()).applyValues();
      if (stageResults == null) {
        return (false);
      }
    }  
    
    //
    // Submit the changes:
    //
     
    TimeAxisDefinition tad = new TimeAxisDefinition(uics_.getRMan());
    tad.setDefinition(units, customUnits, customUnitAbbrev, isSuffix, stageResults);
    DatabaseChange dc = dacx_.getExpDataSrc().setTimeAxisDefinition(tad);
    if (dc != null) {
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(dacx_, dc);
      support.addEdit(dcc);
    }
    
    support.finish();
    return (true);
  } 
}
