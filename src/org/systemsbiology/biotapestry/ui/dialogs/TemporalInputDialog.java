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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.InputTimeRange;
import org.systemsbiology.biotapestry.timeCourse.RegionAndRange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalRange;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing Temporal Input data
*/

public class TemporalInputDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<String, TabData> tabData_;
  private HashMap<Integer, TabData> tabMap_;
  private TabData tdatf_;
  private JTabbedPane tabPane_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  private UndoFactory uFac_;
  private List<EnumCell> regionList_;
  private List<EnumCell> inputList_;
  private List<EnumCell> signList_;
  private List<EnumCell> strategyList_;
  private boolean advanced_;
  private FixedJButton buttonAdv_;
  private boolean namedStages_;
  private TimeAxisDefinition tad_;
  private TemporalInputRangeData tirData_;
  
  private static final long serialVersionUID = 1L;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Wrapper on constructor.  Have to define time axis before we can define
  ** TI structure.
  */ 
  
  public static TemporalInputDialog temporalInputDialogWrapper(UIComponentSource uics, DataAccessContext dacx, List<String> mappedIDs, boolean doForTable, UndoFactory uFac) {
    
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
      return (null);
    }
    
    TemporalInputDialog qsd = new TemporalInputDialog(uics, dacx, mappedIDs, doForTable, uFac);
    return (qsd);
  }      

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private TemporalInputDialog(UIComponentSource uics, DataAccessContext dacx, List<String> mappedIDs, boolean doForTable, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("tientry.title"), true);
    dacx_ = dacx;
    uics_ = uics;
    uFac_ = uFac;
    advanced_ = false;    
        
    ResourceManager rMan = dacx_.getRMan();    
    setSize(900, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    //
    // Figure out if we accept numbers or names for the stages:
    //
    
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages();
    tirData_ = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    
    signList_ = buildSignList();
    inputList_ = buildInputList();
    regionList_ = buildRegionList();
    strategyList_ = buildStrategyList();
     
    //
    // Build the values table tabs
    //

    tabData_ = new HashMap<String, TabData>();
    tabMap_ = new HashMap<Integer, TabData>();
    
    if (!doForTable) {
      tabPane_ = new JTabbedPane();      
      Iterator<String> midit = mappedIDs.iterator();
      int index = 0;
      while (midit.hasNext()) {
        String mid = midit.next();
        TabData tdat = buildATab(mid, false);
        tabData_.put(mid, tdat);
        if (tdatf_ == null) {
          tdatf_ = tdat;
        }
        tabPane_.addTab(mid, tdat.panel);
        tabMap_.put(new Integer(index++), tdat);
      }    
      UiUtil.gbcSet(gbc, 0, 0, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tabPane_, gbc);
      
      tabPane_.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {
            stopTheEditing(false);
            tdatf_ = tabMap_.get(new Integer(tabPane_.getSelectedIndex()));
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
  
    } else {
      String mid = mappedIDs.get(0);
      TabData tdat = buildATab(mid, true);
      tabData_.put(mid, tdat);
      tdatf_ = tdat;
      UiUtil.gbcSet(gbc, 0, 0, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tdat.panel, gbc);
    }
    
    //
    // Build the expand button:
    //
    
    buttonAdv_ = new FixedJButton(rMan.getString("dialogs.expand"));
    buttonAdv_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          toggleAdvanced();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    

    DialogSupport ds = new DialogSupport(this, uics_, dacx_, gbc);
    ds.buildAndInstallButtonBoxWithExtra(cp, 8, 1, true, buttonAdv_, false); 
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
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
    stopTheEditing(false);
    applyProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    stopTheEditing(false);
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
    stopTheEditing(true);
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
  ** Build a tab for one species
  */ 
  
  private TabData buildATab(String mappedID, boolean doForTable) {       
    ResourceManager rMan = dacx_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    
    TabData tdat = new TabData();
  
    //
    // Build the values table.
    //
       
    tdat.panel = new JPanel();
    tdat.panel.setLayout(new GridBagLayout());
    
    JPanel tiRangeControls = buildRangeControls(tdat, mappedID, doForTable, rMan, gbc);
   
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    tdat.panel.add(tiRangeControls, gbc);
    
    //
    // Here is the table:
    //
    
    tdat.est = new EditableTable(uics_, dacx_, new TemporalInputTableModel(uics_, dacx_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.INPUT), new EditableTable.EnumCellInfo(true, inputList_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.SIGN), new EditableTable.EnumCellInfo(false, signList_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.REGION), new EditableTable.EnumCellInfo(true, regionList_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.START_STRAT), new EditableTable.EnumCellInfo(false, strategyList_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.END_STRAT), new EditableTable.EnumCellInfo(false, strategyList_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TemporalInputTableModel.RES_SOURCE), new EditableTable.EnumCellInfo(true, regionList_, EnumCell.class));
    JPanel tablePan = tdat.est.buildEditableTable(etp);
    // Can't do this too early:
    tdat.est.getModel().collapseView(!advanced_);
    UiUtil.gbcSet(gbc, 0, 1, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    tdat.panel.add(tablePan, gbc);
    
    return (tdat);
  }
    
  /***************************************************************************
  **
  ** Stock the range data controls
  */ 
  
  private JPanel buildRangeControls(TabData tdat, String mappedID, boolean doForTable, 
                                    ResourceManager rMan, GridBagConstraints gbc) {       
  
    JPanel tiRangeControls = new JPanel();
    tiRangeControls.setLayout(new GridBagLayout()); 

    tdat.range = dacx_.getTemporalRangeSrc().getTemporalInputRangeData().getRange(mappedID);
    if (tdat.range == null) {
      tdat.range = new TemporalRange(mappedID, null, false);
      tdat.isNew = true;
    } else {
      tdat.isNew = false;
    }      

    int col = 0;
    JLabel label = null;
    if (doForTable) {
      label = new JLabel(rMan.getString("tientry.name"));
      tdat.nameField = new JTextField();
      UiUtil.gbcSet(gbc, col++, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
      tiRangeControls.add(label, gbc);
      UiUtil.gbcSet(gbc, col, 0, 3, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      col += 3;
      tiRangeControls.add(tdat.nameField, gbc);
    }    

    label = new JLabel(rMan.getString("tientry.rangeNote"));  
    tdat.noteField = new JTextField();
    UiUtil.gbcSet(gbc, col++, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tiRangeControls.add(label, gbc);
    UiUtil.gbcSet(gbc, col, 0, 3, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    col += 3;
    tiRangeControls.add(tdat.noteField, gbc);    
   
    tdat.internalChoice = new JCheckBox(rMan.getString("tientry.internalOnly"));
    UiUtil.gbcSet(gbc, col++, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tiRangeControls.add(tdat.internalChoice, gbc); 
    return (tiRangeControls);  
  }
  
  /***************************************************************************
  **
  ** Stop all table editing.  Call when changing tabs or apply/OK!
  */ 
  
  private void stopTheEditing(boolean doCancel) {
    tdatf_.est.stopTheEditing(doCancel);
    return;
  }  
  
  /***************************************************************************
  **
  ** Stock the range data controls
  */ 
  
  private void extractRangeData(TabData tdat) {       
    tdat.noteField.setText(tdat.range.getNote());
    if (tdat.nameField != null) {
      tdat.nameField.setText(tdat.range.getName());
    }    
    tdat.internalChoice.setSelected(tdat.range.isInternalOnly());
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply the range data controls
  */ 
  
  private boolean applyRangeData(TemporalInputRangeData tird, TabData tdat, UndoSupport support) {
    if (tdat.nameField != null) {
      String currName = tdat.range.getName();
      String newName = tdat.nameField.getText().trim();
      if (!newName.equals(currName)) {
        if (newName.equals("")) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tientry.nameBlank"), 
                                        rMan.getString("tientry.nameBlankTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
        if (!tird.nameIsUnique(newName)) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tientry.nameNotUnique"), 
                                        rMan.getString("tientry.nameNotUniqueTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
        //
        // If name change disconnects data from network elements links by default
        // association, we let user choose how to handle it:
        //
        
        if (!DataUtil.keysEqual(newName, currName)) {
          Set nodes = tird.getTemporalInputEntryKeyInverse(currName);
          if (!nodes.isEmpty()) {
            //
            // We need to study the set of associated nodes.  Some may be default 
            // associated by name, others may be associated by custom map.
            //
            boolean haveDefault = false;
            Iterator nit = nodes.iterator();
            while (nit.hasNext()) {
              String nid = (String)nit.next();
              if (!tird.haveCustomEntryMapForNode(nid)) {
                haveDefault = true;
                break;
              }
            }
            if (haveDefault) {
              ResourceManager rMan = dacx_.getRMan();
              String desc = MessageFormat.format(rMan.getString("dataTables.disconnecting"), 
                                                 new Object[] {currName, newName});
              JOptionPane.showMessageDialog(uics_.getTopFrame(), desc, 
                                            rMan.getString("dataTables.disconnectingTitle"),
                                            JOptionPane.WARNING_MESSAGE);
            }
          }
        }
        tdat.range.setName(newName);
        TemporalInputChange[] tic = tird.changeDataMapsToName(currName, newName);
        for (int i = 0; i < tic.length; i++) {
          support.addEdit(new TemporalInputChangeCmd(dacx_, tic[i]));
        } 
      }
    }
    tdat.range.setNote(tdat.noteField.getText());
    tdat.range.setInternalOnly(tdat.internalChoice.isSelected());
    return (true);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for tracking tabs
  */

  private class TabData {
    EditableTable est;
    JPanel panel;
    JTextField noteField;
    JTextField nameField;
    JCheckBox internalChoice;
    TemporalRange range;
    boolean isNew;
  }
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class TemporalInputTableModel extends EditableTable.TableModel {
    
    final static int INPUT            = 0;
    final static int SIGN             = 1;
    final static int REGION           = 2;
    final static int MIN_TIME         = 3;
    final static int MAX_TIME         = 4;
    final static int NOTE             = 5;
    final static int START_STRAT      = 6;
    final static int END_STRAT        = 7;
    final static int RES_SOURCE       = 8;
    private final static int NUM_COL_ = 9; 
    
    private final static int ORIG_INPUT_ = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private static final long serialVersionUID = 1L;
     
    class TableRow {
      EnumCell input;
      EnumCell sign;
      EnumCell region;
      Object minTime;
      Object maxTime;
      String note;
      EnumCell startStrat;
      EnumCell endStrat;
      EnumCell restrictedSrc;
      String origInput;     
      
      TableRow() {   
      }
     
      TableRow(int i) {
        input = (EnumCell)columns_[INPUT].get(i);
        sign = (EnumCell)columns_[SIGN].get(i);
        region = (EnumCell)columns_[REGION].get(i);
        minTime = columns_[MIN_TIME].get(i);
        maxTime = columns_[MAX_TIME].get(i);
        note = (String)columns_[NOTE].get(i);
        startStrat = (EnumCell)columns_[START_STRAT].get(i);
        endStrat = (EnumCell)columns_[END_STRAT].get(i);
        restrictedSrc = (EnumCell)columns_[RES_SOURCE].get(i);                    
        origInput = (String)hiddenColumns_[ORIG_INPUT_].get(i);
      }
      
      void toCols() {
        columns_[INPUT].add(input);
        columns_[SIGN].add(sign);
        columns_[REGION].add(region);
        columns_[MIN_TIME].add(minTime);
        columns_[MAX_TIME].add(maxTime);
        columns_[NOTE].add(note);
        columns_[START_STRAT].add(startStrat);
        columns_[END_STRAT].add(endStrat);
        // Gotta crash doing a getValueAt(8) on code before refactoring: BT-11-25-09:1      
        columns_[RES_SOURCE].add(restrictedSrc);                    
        hiddenColumns_[ORIG_INPUT_].add(origInput);
        return;
      }
    }
   
    TemporalInputTableModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {"tientry.input",
                                "tientry.sign",
                                "tientry.region",
                                "tientry.minTime",
                                "tientry.maxTime",
                                "tientry.note",
                                "tientry.startStrat",
                                "tientry.endStrat",
                                "tientry.restrictedSource"};
      Class timeClass = (namedStages_) ? String.class : ProtoInteger.class;
      colClasses_ = new Class[] {EnumCell.class,
                                 EnumCell.class,
                                 EnumCell.class,
                                 timeClass,
                                 timeClass,
                                 String.class,
                                 EnumCell.class,
                                 EnumCell.class,
                                 EnumCell.class};
      addHiddenColumns(NUM_HIDDEN_);
      collapseCount_ = 6;  // Enables table collapse
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
  
    @Override
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    } 
  
    @Override
    public boolean addRow(int[] rows) {
      super.addRow(rows);
      int lastIndex = rowCount_ - 1;
      EnumCell regUse = (regionList_.size() > 0) ? (EnumCell)regionList_.get(0) : new EnumCell("", null, -1, -1);
      columns_[REGION].set(lastIndex, new EnumCell(regUse));  
      columns_[RES_SOURCE].set(lastIndex, new EnumCell(regUse));
     
      columns_[INPUT].set(lastIndex, new EnumCell("", null, -1, -1));
      hiddenColumns_[ORIG_INPUT_].set(lastIndex, "");  
      columns_[SIGN].set(lastIndex, new EnumCell((EnumCell)signList_.get(0)));
      if (namedStages_) {
        String firstName = tad_.getNamedStageForIndex(TimeAxisDefinition.FIRST_STAGE).name;
        columns_[MIN_TIME].set(lastIndex, firstName);
        columns_[MAX_TIME].set(lastIndex, firstName);        
      } else {
        columns_[MIN_TIME].set(lastIndex, new ProtoInteger(0));
        columns_[MAX_TIME].set(lastIndex, new ProtoInteger(0));
      }
      // columns_[NOTE] Stays null..
      columns_[START_STRAT].set(lastIndex, new EnumCell((EnumCell)strategyList_.get(0)));
      columns_[END_STRAT].set(lastIndex, new EnumCell((EnumCell)strategyList_.get(0)));
      return (true);
    } 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current temporal input data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    Iterator tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = (TabData)tdit.next();
      List entries = buildTableRows(tdat);
      tdat.est.getModel().extractValues(entries);
      extractRangeData(tdat);      
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Toggle the advanced display
  ** 
  */
  
  private void toggleAdvanced() {
    advanced_ = !advanced_;
    ResourceManager rMan = dacx_.getRMan();
    String text = rMan.getString((advanced_) ? "dialogs.collapse" : "dialogs.expand");
    buttonAdv_.setText(text);
    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      TemporalInputTableModel titm = (TemporalInputTableModel)tdat.est.getModel();
      titm.collapseView(!advanced_);
    }
    buttonAdv_.revalidate();
    return;
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the temporal input data
  ** 
  */
  
  private boolean applyProperties() {
    
    //
    // Check for errors before actually doing the work:
    //
    
    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next(); 
      List vals = tdat.est.getModel().getValuesFromTable();
      if (!checkValues(vals)) {
        return (false);
      }
    }
    
    tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      List vals = tdat.est.getModel().getValuesFromTable();
      //
      // Undo/Redo support
      //
   
      UndoSupport support = uFac_.provideUndoSupport("undo.tientry", dacx_);
      
      if (tdat.isNew) {
        support.addEdit(new TemporalInputChangeCmd(dacx_, tirData_.addEntry(tdat.range)));
      }
      TemporalInputChange tic = tirData_.startRangeUndoTransaction(tdat.range.getName());
      boolean ok = applyRangeData(tirData_, tdat, support);
      // Only time this is false is for single-shot panels, i.e. no partial
      // undo commits should occur.
      if (!ok) {
        if (tdit.hasNext()) {
          throw new IllegalStateException();
        }
        return (false);
      }
      
      applyValues(tdat, vals);
      tic = tirData_.finishRangeUndoTransaction(tic);
      support.addEdit(new TemporalInputChangeCmd(dacx_, tic));
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Build region list
  */
   
  private List<EnumCell> buildRegionList() {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    Set<String> regions = tirData_.getAllRegions();
    Iterator<String> rit = regions.iterator();
    ResourceManager rMan = dacx_.getRMan();
    String allRegions = rMan.getString("tientry.allRegions");
    retval.add(new EnumCell(allRegions, null, 0, 0));
    int count = 1;
    while (rit.hasNext()) {
      String regionName = rit.next();
      retval.add(new EnumCell(regionName, regionName, count, count));
      count++;
    }
    return (retval);      
  }

  /***************************************************************************
  **
  ** Build input list
  */
   
  private List<EnumCell> buildInputList() {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    ResourceManager rMan = dacx_.getRMan();
    String inputFormat = rMan.getString("tientry.inputFormat");    
    //
    // We provide all current mapping values.
    //
    Set ids = tirData_.getAllIdentifiers();
    Iterator sit = ids.iterator();
    Object[] inputName = new Object[1];
    int count = 1;
    while (sit.hasNext()) {
      inputName[0] = sit.next();
      String desc = MessageFormat.format(inputFormat, inputName);       
      retval.add(new EnumCell(desc, (String)inputName[0], count, count));
      count++;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build sign list
  */
   
  private List<EnumCell> buildSignList() {    
    ResourceManager rMan = dacx_.getRMan();     
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    String promote = rMan.getString("ticreate.promote");
    retval.add(new EnumCell(promote, Integer.toString(RegionAndRange.PROMOTER), RegionAndRange.PROMOTER, 0));
 
    String repress = rMan.getString("ticreate.repress");
    retval.add(new EnumCell(repress, Integer.toString(RegionAndRange.REPRESSOR), RegionAndRange.REPRESSOR, 1));
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Build Strategy list
  */
   
  private List<EnumCell> buildStrategyList() {   
    ResourceManager rMan = dacx_.getRMan(); 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    StringBuffer buf = new StringBuffer();
    int numItems = RegionAndRange.NUM_STRATEGIES;
    for (int i = 0; i < numItems; i++) {
      buf.setLength(0);
      buf.append("stratRendererTI.");
      String strat = RegionAndRange.mapStrategy(i);
      buf.append((strat == null) ? "noStrat" : strat);
      retval.add(new EnumCell(rMan.getString(buf.toString()), strat, i, i));
    }
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Build table rows
  */ 
  
  private List<TemporalInputTableModel.TableRow> buildTableRows(TabData tDat) {
    TemporalInputTableModel titm = (TemporalInputTableModel)tDat.est.getModel();
    ArrayList<TemporalInputTableModel.TableRow> retval = new ArrayList<TemporalInputTableModel.TableRow>();
 
    Iterator<InputTimeRange> pit = tDat.range.getTimeRanges();
    while (pit.hasNext()) {      
      InputTimeRange pert = pit.next();
      Iterator<RegionAndRange> rit = pert.getRanges();
      while (rit.hasNext()) {
        RegionAndRange rar = rit.next();
        TemporalInputTableModel.TableRow tr = titm.new TableRow();    
        //
        // Set input
        //
        String input = pert.getName();
        Iterator ilit = inputList_.iterator();
        while (ilit.hasNext()) {
          EnumCell eci = (EnumCell)ilit.next();
          if (eci.internal.equals(input)) {
            tr.input = new EnumCell(eci);
            tr.origInput = input;
            break;
          }
        }

        //
        // Set region
        //

        String region = rar.getRegion();
        if (region == null) {
          tr.region = new EnumCell((EnumCell)regionList_.get(0));
        } else {
          int rsize = regionList_.size();
          for (int i = 1; i < rsize; i++) {
            EnumCell ecr = (EnumCell)regionList_.get(i);
            if (ecr.internal.equals(region)) {
              tr.region = new EnumCell(ecr);
              break;
            }
          }
        }

        //
        // Set sign
        //

        int sign = rar.getSign();
        int useIndex = (sign == RegionAndRange.PROMOTER) ? 0 : 1;
        tr.sign = new EnumCell((EnumCell)signList_.get(useIndex));

        //
        // Set restricted source
        //

        String source = rar.getRestrictedSource();
        if (source == null) {
          tr.restrictedSrc = new EnumCell((EnumCell)regionList_.get(0));
        } else {
          int rsize = regionList_.size();
          for (int i = 1; i < rsize; i++) {
            EnumCell ecr = (EnumCell)regionList_.get(i);
            if (ecr.internal.equals(source)) {
              tr.restrictedSrc = new EnumCell(ecr);
              break;
            }
          }
        }

        //
        // Times as stages or not:
        //

        if (namedStages_) {
          TimeAxisDefinition.NamedStage stageName = tad_.getNamedStageForIndex(rar.getMin());
          tr.minTime = stageName.name;
          stageName = tad_.getNamedStageForIndex(rar.getMax());
          tr.maxTime = stageName.name;          
        } else {          
          tr.minTime = new ProtoInteger(rar.getMin());
          tr.maxTime = new ProtoInteger(rar.getMax());                  
        }

        //
        // Everybody else:
        //

        tr.note = rar.getNote();
        
        int startStrat = rar.getStartStrategy();
        int ssize = strategyList_.size();
        for (int i = 0; i < ssize; i++) {
          EnumCell ecr = (EnumCell)strategyList_.get(i);
          if (ecr.value == startStrat) {
            tr.startStrat = new EnumCell(ecr);
            break;
          }
        }
        int endStrat = rar.getEndStrategy();
        for (int i = 0; i < ssize; i++) {
          EnumCell ecr = (EnumCell)strategyList_.get(i);
          if (ecr.value == endStrat) {
            tr.endStrat = new EnumCell(ecr);
            break;
          }
        }      
        retval.add(tr);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Check table rows for correctness
  */ 
     
  private boolean checkValues(List vals) {
    if (vals.isEmpty()) {
      return (true);
    }

    ResourceManager rMan = dacx_.getRMan();
    int size = vals.size();
    for (int i = 0; i < size; i++) {
      TemporalInputTableModel.TableRow tr = (TemporalInputTableModel.TableRow)vals.get(i);
      int minTimeVal;
      int maxTimeVal;
      if (namedStages_) {
        String minStageName = (String)tr.minTime;
        String maxStageName = (String)tr.maxTime;
        minTimeVal = tad_.getIndexForNamedStage(minStageName);
        maxTimeVal = tad_.getIndexForNamedStage(maxStageName);
        if ((minTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME) ||
            (maxTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tientry.badStageName"),
                                        rMan.getString("tientry.badStageNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }   
      } else {      
        ProtoInteger min = (ProtoInteger)tr.minTime;
        ProtoInteger max = (ProtoInteger)tr.maxTime;
        if (((min == null) || (!min.valid)) ||
            ((max == null) || (!max.valid))) {
          IntegerEditor.triggerWarning(uics_.getHandlerAndManagerSource(), uics_.getTopFrame());
          return (false);
        }
        minTimeVal = min.value;
        maxTimeVal = max.value;
      }
      if ((minTimeVal < 0) || (maxTimeVal < 0) || (maxTimeVal < minTimeVal)) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tientry.badRange"), 
                                      rMan.getString("tientry.errorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }        

      EnumCell eci = tr.input;
      if ((eci.internal == null) || (eci.internal.trim().equals(""))) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tientry.cannotBeBlank"), 
                                      rMan.getString("tientry.errorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    return (true);
  }    
    
  /***************************************************************************
  **
  ** Apply table results
  */
    
    
  private void applyValues(TabData tDat, List vals) {
    int size = vals.size();
    for (int i = 0; i < size; i++) {
      TemporalInputTableModel.TableRow tr = (TemporalInputTableModel.TableRow)vals.get(i);
      EnumCell eci = tr.input;
      String pertName = eci.internal;
      String origPert = tr.origInput;
      if (!DataUtil.keysEqual(pertName, origPert)) {
        Set<String> nodes = tirData_.getTemporalInputSourceKeyInverse(origPert);
        if (!nodes.isEmpty()) {
          ResourceManager rMan = dacx_.getRMan();
          String desc = MessageFormat.format(rMan.getString("dataTables.disconnectingRow"), 
                                             new Object[] {origPert, pertName});
          JOptionPane.showMessageDialog(uics_.getTopFrame(), desc, 
                                        rMan.getString("dataTables.disconnectingTitle"),
                                        JOptionPane.WARNING_MESSAGE);
        }
      }
    }
    //
    // Apply the values by completely replacing the data:
    //

    tDat.range.dropTimeRanges();
    String currPertName = null;
    InputTimeRange currPert = null;

    for (int i = 0; i < size; i++) {
      TemporalInputTableModel.TableRow tr = (TemporalInputTableModel.TableRow)vals.get(i);
      EnumCell eci = tr.input;
      String pertName = eci.internal;
      if (!pertName.equals(currPertName)) {
        currPert = tDat.range.getTimeRange(pertName);
        if (currPert == null) {
          currPert = new InputTimeRange(pertName);
          tDat.range.addTimeRange(currPert);
        }
        currPertName = pertName;
      }

      String regionVal = tr.region.internal;
      int signVal = tr.sign.value;

      int minTimeVal;
      int maxTimeVal;
      if (namedStages_) {
        String minStageName = (String)tr.minTime;
        String maxStageName = (String)tr.maxTime;
        minTimeVal = tad_.getIndexForNamedStage(minStageName);
        maxTimeVal = tad_.getIndexForNamedStage(maxStageName);
        if ((minTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME) ||
            (maxTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME)) {
          throw new IllegalStateException();  // caught previously
        }   
      } else {      
        ProtoInteger min = (ProtoInteger)tr.minTime;
        ProtoInteger max = (ProtoInteger)tr.maxTime;
        minTimeVal = min.value;
        maxTimeVal = max.value;
      }        

      String note = tr.note;

      // Gotta crash here BT-11-25-09:1 (before 4/10 refactoring)
      EnumCell srcCell = tr.restrictedSrc;
      String src = srcCell.internal;        

      EnumCell strCellStart = tr.startStrat;
      EnumCell strCellEnd = tr.endStrat;

      RegionAndRange rAndR = 
        new RegionAndRange(regionVal, minTimeVal, maxTimeVal, signVal, 
                           note, src, strCellStart.value, strCellEnd.value);
      currPert.add(rAndR);
    }
    return;
  }
}
