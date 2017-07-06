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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

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

  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  private UndoFactory uFac_;
  private ArrayList<PerTab> ptDat_;
  boolean currentTabOnly_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Wrapper on wrapper on constructor.  Have to define time axis before we can do other
  ** time-based operations.
  */ 
  
  public static boolean timeAxisSetupDialogWrapperWrapper(UIComponentSource uics, DataAccessContext dacx, 
                                                          Metabase mb, TabSource tSrc, UndoFactory uFac,
                                                          boolean currentTabOnly) {
    
    Iterator<String> dbit;
    Iterator<String> dbit2;
    if (currentTabOnly) {
      HashSet<String> cto = new HashSet<String>();
      cto.add(tSrc.getCurrentTab());
      dbit = cto.iterator();
      dbit2 = cto.iterator();
    } else {
      dbit = mb.getDBIDs();
      dbit2 = mb.getDBIDs();
    }
    
    boolean needInit = false;
    while (dbit.hasNext()) {
      String id = dbit.next();
      TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(mb, id);
      TimeAxisDefinition tad = tpdacx.getExpDataSrc().getTimeAxisDefinition();
      if ((tad == null) || !tad.isInitialized()) {
        needInit = true;
        break;
      }
    }
    
    if (needInit) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics, dacx, mb, tSrc, uFac, currentTabOnly);
      tasd.setVisible(true);
    }
    
    //
    // After giving the user the chance to fix the problem, see if they did so:
    //

    needInit = false;
    while (dbit2.hasNext()) {
      String id = dbit2.next();
      TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(mb, id);
      TimeAxisDefinition tad = tpdacx.getExpDataSrc().getTimeAxisDefinition();
      if ((tad == null) || !tad.isInitialized()) {
        needInit = true;
        break;
      }
    }
    
    if (needInit) {
      ResourceManager rMan = uics.getRMan();
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
  
  public static TimeAxisSetupDialog timeAxisSetupDialogWrapper(UIComponentSource uics, DataAccessContext dacx, 
                                                               Metabase mb, TabSource tSrc, UndoFactory uFac, 
                                                               boolean currentTabOnly) {  
    //
    // Figure out if we can change the time units anymore:
    //
    
    Set<String> shar = mb.tabsSharingData();
    Iterator<String> dbit;
    if (currentTabOnly) {
      HashSet<String> cto = new HashSet<String>();
      String currTab = tSrc.getCurrentTab();
      if (shar.contains(currTab)) {
        cto.addAll(shar);
      } else {
        cto.add(currTab);
      }
      dbit = cto.iterator();
    } else {
      dbit = mb.getDBIDs();
    }
    
    Map<String, Boolean> canChange = new HashMap<String, Boolean>();

    boolean sharedFrozen = false;
    boolean somebodyFrozen = false;
    while (dbit.hasNext()) {
      String id = dbit.next();
      TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(mb, id);
      ExperimentalDataSource eds = tpdacx.getExpDataSrc();
      PerturbationData pd = eds.getPertData();    
      TimeCourseData tcdat = eds.getTimeCourseData();
      //
      // Note that this is *always* per-tab (not shared). So a shared data set can be frozen if ANY tab uses
      // the TAD:
      //
      TemporalInputRangeData tirdat = tpdacx.getTemporalRangeSrc().getTemporalInputRangeData();
      if (pd.haveData() || pd.getPertDisplayOptions().hasColumns() || pd.getPertDisplayOptions().hasDefaultTimeSpan() ||
          ((tcdat != null) && tcdat.hasGeneTemplate()) || tirdat.haveData() || dacx.getGenomeSource().modelsHaveTimeBounds()) {
        canChange.put(id, Boolean.FALSE);
        somebodyFrozen = true;
        if (shar.contains(id)) {
          sharedFrozen = true;
        }
      } else {
        canChange.put(id, Boolean.TRUE);
      }
    }
    if (sharedFrozen) {
      for (String id : shar) {
        canChange.put(id, Boolean.FALSE);
      }
    }
    
    if (somebodyFrozen) {
      ResourceManager rMan = dacx.getRMan();
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("timeAxisDialog.cannotChange"), 
                                    rMan.getString("timeAxisDialog.cannotChangeTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    
    TimeAxisSetupDialog qsd = new TimeAxisSetupDialog(uics, dacx, mb, tSrc, uFac, canChange, currentTabOnly);
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

  private TimeAxisSetupDialog(UIComponentSource uics, DataAccessContext dacx, Metabase mb, 
                              TabSource tSrc, UndoFactory uFac, 
                              Map<String, Boolean> canChange, boolean currentTabOnly) {     
    super(uics.getTopFrame(), uics.getRMan().getString("timeAxisDialog.title"), true);
 
    dacx_ = dacx;
    uics_ = uics;
    uFac_ = uFac;
    currentTabOnly_ = currentTabOnly;
    
    ptDat_ = new ArrayList<PerTab>();
    setSize(600, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
    DialogSupport ds = new DialogSupport(this, uics_, gbc);

    //
    // Each tab (though shared data tabs only get one tab) needs it's own pile of data
    //
      
    TreeMap<Integer, PerTab> ordered = new TreeMap<Integer, PerTab>();
    Set<String> shar = mb.tabsSharingData();
 
    Iterator<String> dbit;
    if (currentTabOnly_) {
      HashSet<String> cto = new HashSet<String>();
      cto.add(tSrc.getCurrentTab());
      dbit = cto.iterator();
    } else {
      dbit = mb.getDBIDs();
    }
    boolean haveShared = false;
    while (dbit.hasNext()) {
      String id = dbit.next();
      Database db = mb.getDB(id);
      int index = tSrc.getTabIndexFromId(id);
      TimeAxisDefinition tad = db.getTimeAxisDefinition();
      if (tad == null) {
        tad = new TimeAxisDefinition(uics_.getRMan());     
      }

      if (shar.contains(id)) {
        if (!haveShared) {
          haveShared = true;
          String sharedFmt = uics_.getRMan().getString("qsedit.sharedTabFmt");
          String otherNum = Integer.toString(shar.size() - 1);
          String suffix = (shar.size() == 2) ? "" : "s";
          String sharedTitle = MessageFormat.format(sharedFmt, new Object[] {db.getTabNameData().getTitle(), otherNum, suffix});
          PerTab pt = new PerTab(tad.clone(), sharedTitle, id, canChange.get(id).booleanValue());
          ordered.put(Integer.valueOf(index), pt);
        }
      } else {
        PerTab pt = new PerTab(tad.clone(), db.getTabNameData().getTitle(), id, canChange.get(id).booleanValue());
        ordered.put(Integer.valueOf(index), pt);
      }
    }
    
    //
    // Build the tabs.
    //
 
    JTabbedPane tabPane = new JTabbedPane();
    for (PerTab pt : ordered.values()) {
      ptDat_.add(pt);
      JPanel pan = perTabPanel(pt);
      pt.tabPanel_ = pan;
      tabPane.addTab(pt.tabName_, pt.tabPanel_);
    }
    
    int rowNum = 0;
    
    rowNum = ds.addTable(cp, tabPane, 12, rowNum, 3);
    ds.buildAndInstallButtonBox(cp, rowNum, 3, false, false);
    
    for (PerTab pt : ptDat_) {
      displayProperties(pt);
    }   
 
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
    if (applyProperties()) {
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
  ** Build a per-tab panel
  ** 
  */
  
  private JPanel perTabPanel(PerTab pt) {
    
    JPanel jp = new JPanel();
    jp.setBorder(new EmptyBorder(20, 20, 20, 20));
    jp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    DialogSupport ptds = new DialogSupport(uics_, gbc);
    ResourceManager rMan = uics_.getRMan();
    
    int rowNum = 0;
    
    if (!pt.workingCopy_.isInitialized()) {
      pt.workingCopy_.setToDefault();
    }

    final PerTab ftp = pt;
    JLabel unitsLabel = new JLabel(rMan.getString("timeAxisDialog.units"));
    pt.unitCombo_ = new JComboBox(TimeAxisDefinition.getUnitTypeChoices(uics_.getRMan()));
    if (pt.canChange_) {
      pt.unitCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            int units = ((ChoiceContent)ftp.unitCombo_.getSelectedItem()).val;
            enableCustomAndNamed(ftp, units);
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
    }
    pt.unitCombo_.setEnabled(pt.canChange_);
    unitsLabel.setEnabled(pt.canChange_);
    
    rowNum = ptds.addLabeledWidget(jp, unitsLabel, pt.unitCombo_, true, false, rowNum, 3);
    
    pt.customUnitsLabel_ = new JLabel(rMan.getString("timeAxisDialog.customUnits"));
    pt.customUnitsField_ = new JTextField();
    
    rowNum = ptds.addLabeledWidget(jp, pt.customUnitsLabel_, pt.customUnitsField_, true, false, rowNum, 3);
 
    pt.customUnitsAbbrevLabel_ = new JLabel(rMan.getString("timeAxisDialog.customUnitAbbrev"));
    pt.customUnitsAbbrevField_ = new JTextField();    
    pt.isSuffixCheckBox_ = new JCheckBox(rMan.getString("timeAxisDialog.isSuffix"));
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    jp.add(pt.customUnitsAbbrevLabel_, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    jp.add(pt.customUnitsAbbrevField_, gbc);    
    UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    jp.add(pt.isSuffixCheckBox_, gbc);    
  
    pt.stagesLabel_ = new JLabel(rMan.getString("timeAxisDialog.namedStages"));
    
    rowNum = ptds.addWidgetFullRow(jp, pt.stagesLabel_, true, false, rowNum, 3);
    
    //
    // Build the stages table.
    //
    
    pt.est_ = new EditableTable(uics_, new TimeAxisSetupTableModel(uics_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = pt.est_.buildEditableTable(etp);
    
    rowNum = ptds.addTable(jp, tablePan, 8, rowNum, 3);
    return (jp);
  }
 
  /***************************************************************************
  **
  ** Enable Named Stages and/or custom units
  ** 
  */
  
  private void enableCustomAndNamed(PerTab pt, int units) {    
    boolean activateUnits = TimeAxisDefinition.wantsCustomUnits(units);
    boolean activateStages = TimeAxisDefinition.wantsNamedStages(units);
    enableCustomAndNamedCore(pt, activateUnits, activateStages);
    return;
  }
  
  /***************************************************************************
  **
  ** Enable Named Stages and/or custom units
  ** 
  */
  
  private void enableCustomAndNamedCore(PerTab pt, boolean activateUnits, boolean activateStages) {       
    pt.customUnitsLabel_.setEnabled(activateUnits);
    pt.customUnitsField_.setEnabled(activateUnits);
    pt.customUnitsAbbrevLabel_.setEnabled(activateUnits);
    pt.customUnitsAbbrevField_.setEnabled(activateUnits);
    pt.isSuffixCheckBox_.setEnabled(activateUnits);
    pt.stagesLabel_.setEnabled(activateStages);   
    pt.est_.setEnabled(activateStages);
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Per-tab elements
  */
  
  private static class PerTab {
    String tabName_;
    String dbID_;
    EditableTable est_;
    TimeAxisDefinition workingCopy_;
    JComboBox unitCombo_;
    JTextField customUnitsField_;
    JTextField customUnitsAbbrevField_;
    JCheckBox isSuffixCheckBox_;  
    JLabel customUnitsLabel_;
    JLabel customUnitsAbbrevLabel_;  
    JLabel stagesLabel_;
    boolean canChange_;

    JPanel tabPanel_;
    
    PerTab(TimeAxisDefinition tad, String tabName, String dbID, boolean canChange) {
      workingCopy_ = tad.clone();
      tabName_ = tabName;
      dbID_ = dbID;
      canChange_ = canChange;
    }
  }

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
  
  private void displayProperties(PerTab pt) {

    int units = pt.workingCopy_.getUnits();
    pt.unitCombo_.setSelectedItem(TimeAxisDefinition.unitTypeForCombo(uics_.getRMan(), pt.workingCopy_.getUnits()));
    
    if (pt.workingCopy_.haveCustomUnits()) {
      String customUnits = pt.workingCopy_.getUserUnitName();
      pt.customUnitsField_.setText(customUnits);
      String customUnitAbbrev = pt.workingCopy_.getUserUnitAbbrev();
      pt.customUnitsAbbrevField_.setText(customUnitAbbrev);
      pt.isSuffixCheckBox_.setSelected(pt.workingCopy_.unitsAreASuffix());
    } else {
      pt.isSuffixCheckBox_.setSelected(true);  // disabled, but default if activated
    }
      
    List namedStages = pt.workingCopy_.haveNamedStages() ? pt.workingCopy_.getNamedStages() : new ArrayList();
    pt.est_.getModel().extractValues(namedStages) ;  

    if (!pt.canChange_) {
      enableCustomAndNamedCore(pt, false, false);       
    } else {
      enableCustomAndNamed(pt, units);
    }
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Apply our UI values to the time axis definition
  */
  
  private boolean applyProperties() {
  
    Map<String, List<TimeAxisDefinition.NamedStage>> stageResultsMap = 
      new HashMap<String, List<TimeAxisDefinition.NamedStage>>();
    for (PerTab pt : ptDat_) {
      if (!checkProperties(pt, stageResultsMap)) {
        return (false);
      } 
    }
    UndoSupport support = uFac_.provideUndoSupport("undo.timeAxisSpec", dacx_); 
    for (PerTab pt : ptDat_) {
      applyProperties(pt, stageResultsMap, support); 
    }
    support.finish();
    return (true);
  }

  
  /***************************************************************************
  **
  ** Check that our UI values for the time axis definition are OK
  */
  
  private boolean checkProperties(PerTab pt, Map<String, List<TimeAxisDefinition.NamedStage>> stageResultsMap) {
    
    if (!pt.canChange_) {
      return (true);
    }
    
    ResourceManager rMan = uics_.getRMan();
    int units = ((ChoiceContent)pt.unitCombo_.getSelectedItem()).val;
    
    String customUnits = null;
    String customUnitAbbrev = null;
    if (TimeAxisDefinition.wantsCustomUnits(units)) {
      customUnits = pt.customUnitsField_.getText().trim();
      customUnitAbbrev = pt.customUnitsAbbrevField_.getText().trim();
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
    
    if (TimeAxisDefinition.wantsNamedStages(units)) {
      List<TimeAxisDefinition.NamedStage> stageResults = ((TimeAxisSetupTableModel)pt.est_.getModel()).applyValues();
      if (stageResults == null) {
        return (false);
      }
      stageResultsMap.put(pt.dbID_, stageResults);
    }
 
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Apply our UI values to the time axis definition
  */
  
  private void applyProperties(PerTab pt, Map<String, List<TimeAxisDefinition.NamedStage>> stageResultsMap, UndoSupport support) {
    
    if (!pt.canChange_) {
      return;
    }
    
    int units = ((ChoiceContent)pt.unitCombo_.getSelectedItem()).val;
    boolean wantCustom = TimeAxisDefinition.wantsCustomUnits(units);
    
    String customUnits = (wantCustom) ? pt.customUnitsField_.getText().trim() : null;
    String customUnitAbbrev = (wantCustom) ? pt.customUnitsAbbrevField_.getText().trim() : null;
    boolean isSuffix = (wantCustom) ? pt.isSuffixCheckBox_.isSelected() : true;
    List<TimeAxisDefinition.NamedStage> stageResults = stageResultsMap.get(pt.dbID_);
  
    //
    // Submit the changes:
    //
   
    Metabase mb = dacx_.getMetabase();
    Database db = mb.getDB(pt.dbID_);
    TimeAxisDefinition tad = new TimeAxisDefinition(uics_.getRMan());
    tad.setDefinition(units, customUnits, customUnitAbbrev, isSuffix, stageResults);
    DatabaseChange dc = db.setTimeAxisDefinition(tad);
    if (dc != null) {
      TabPinnedDynamicDataAccessContext tpdacx = new TabPinnedDynamicDataAccessContext(mb, pt.dbID_);
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(tpdacx, dc);
      support.addEdit(dcc);
    }
    return;
  }  
}
