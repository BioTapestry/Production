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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.GeneTemplateEntry;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.DoubleEditor;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ProtoDouble;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrackingDoubleRenderer;
import org.systemsbiology.biotapestry.util.TrackingUnit;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing perturbed expression state
*/

public class TimeCourseEntryDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<String, TabData> tabData_;
  private DataAccessContext dacx_;
  private TimeCourseData tcData_;
  private TimeCourseDataMaps tcdm_; 
  private TimeAxisDefinition tad_; 
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private boolean advanced_;
  private FixedJButton buttonAdv_;
  private JTabbedPane tabPane_;
  private HashMap<Integer, TabData> tabMap_;
  private TabData tdatf_;
  private ArrayList<EnumCell> confidenceEnum_;
  private ArrayList<EnumCell> expressEnum_;
  private ArrayList<EnumChoiceContent<ExpressionEntry.Source>> sourceEnum_;
  private ArrayList<EnumCell> stratEnum_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor. Needs casting work! List of mappedIDs is either <TimeCourseDataMaps.TCMapping> OR <String>
  */ 
  
  public TimeCourseEntryDialog(UIComponentSource uics, DataAccessContext dacx, 
                               List mappedIDs, boolean doForTable, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("tcentry.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    advanced_ = false;
        
    ResourceManager rMan = uics_.getRMan();    
    setSize(850, 750);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();  
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();   
    String display = tad_.unitDisplayString();   
    tcData_ = dacx_.getExpDataSrc().getTimeCourseData();
    tcData_ = dacx_.getExpDataSrc().getTimeCourseData();
    tcdm_ = dacx_.getDataMapSrc().getTimeCourseDataMaps();
    
       
    confidenceEnum_ = buildConfidenceEnum(); 
    expressEnum_ = buildExpressionEnum(); 
    sourceEnum_ = buildSourceEnum(); 
    stratEnum_ = buildStrategyEnum();   
    
    //
    // Build the values table tabs.
    //

    tabData_ = new HashMap<String, TabData>();
    tabMap_ = new HashMap<Integer, TabData>();
    if (!doForTable) {
      tabPane_ = new JTabbedPane();      
      Iterator<TimeCourseDataMaps.TCMapping> midit = (Iterator<TimeCourseDataMaps.TCMapping>)mappedIDs.iterator();
      int index = 0;
      while (midit.hasNext()) {
        TimeCourseDataMaps.TCMapping tcm = midit.next();
        TabData tdat = buildATab(tcm.name, false, display);
        tabData_.put(tcm.name, tdat);
        if (tdatf_ == null) {
          tdatf_ = tdat;
        }
        tabPane_.addTab(tcm.name, tdat.panel);
        tabMap_.put(new Integer(index++), tdat);
      }
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tabPane_, gbc);
      tabPane_.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {     
            stopTheEditing(false);
            tdatf_ = tabMap_.get(Integer.valueOf(tabPane_.getSelectedIndex()));
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
    } else {
      String mid = (String)mappedIDs.get(0);
      TabData tdat = buildATab(mid, true, display);
      tabData_.put(mid, tdat);
      tdatf_ = tdat;
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
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
   
    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    ds.buildAndInstallButtonBoxWithExtra(cp, 10, 1, true, buttonAdv_, false); 
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
  
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

  /***************************************************************************
  **
  ** Build a tab for one species
  */ 
  
  private TabData buildATab(String mappedID, boolean doForTable, String display) {       
    ResourceManager rMan = uics_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();

    TabData tdat = new TabData();
    
    //
    // Build the values table.
    //
       
    tdat.panel = new JPanel();
    tdat.panel.setLayout(new GridBagLayout());
    
    
    JPanel tcGeneControls = buildEntryControls(tdat, mappedID, doForTable, rMan, gbc, tcData_);   
   
    UiUtil.gbcSet(gbc, 0, 0, 10, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    tdat.panel.add(tcGeneControls, gbc);  
        
   
    tdat.est = new EditableTable(uics_, new TimeCourseTableModel(uics_, display, tdat), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.singleSelectOnly = true;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.VALUE), new EditableTable.EnumCellInfo(false, expressEnum_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.CONFIDENCE), new EditableTable.EnumCellInfo(false, confidenceEnum_, EnumCell.class));
    List valList = ((TimeCourseTableModel)tdat.est.getModel()).getValueColumn();
    HashMap<Integer, TrackingUnit> tdrxMap = new HashMap<Integer, TrackingUnit>();
    int[] enableSource = new int[] {ExpressionEntry.EXPRESSED, ExpressionEntry.WEAK_EXPRESSION, ExpressionEntry.VARIABLE};
    TrackingUnit tu = new TrackingUnit.ListTrackingMultiValUnit(valList, enableSource);
    tdrxMap.put(new Integer(TimeCourseTableModel.SOURCE), tu);   
    StratSourceTracker sst = ((TimeCourseTableModel)tdat.est.getModel()).getStratSourceTracker();    
    tdrxMap.put(new Integer(TimeCourseTableModel.STRAT_SOURCE), sst);
    int[] enableStrat = new int[] {ExpressionEntry.EXPRESSED, ExpressionEntry.WEAK_EXPRESSION};
    TrackingUnit tus = new TrackingUnit.ListTrackingMultiValUnit(valList, enableStrat);
    tdrxMap.put(new Integer(TimeCourseTableModel.START_STRAT), tus);
    tdrxMap.put(new Integer(TimeCourseTableModel.END_STRAT), tus);
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.SOURCE), new EditableTable.EnumCellInfo(false, sourceEnum_, tdrxMap, EnumChoiceContent.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.STRAT_SOURCE), new EditableTable.EnumCellInfo(false, sourceEnum_, tdrxMap, EnumChoiceContent.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.START_STRAT), new EditableTable.EnumCellInfo(false, stratEnum_, tdrxMap, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.END_STRAT), new EditableTable.EnumCellInfo(false, stratEnum_, tdrxMap, EnumCell.class));   
    JPanel tablePan = tdat.est.buildEditableTable(etp);     
    tu = new TrackingUnit.ListTrackingUnit(valList, ExpressionEntry.VARIABLE);
    HashMap tdrMap = new HashMap();
    tdrMap.put(new Integer(TimeCourseTableModel.ACTIVITY), tu);   
    tdat.est.getTable().setDefaultRenderer(ProtoDouble.class, new TrackingDoubleRenderer(tdrMap, uics_.getHandlerAndManagerSource()));
    // FIX ME, MAKE AVAILABLE FROM SUPERCLASS   
    ((DefaultTableCellRenderer)tdat.est.getTable().getDefaultRenderer(String.class)).setHorizontalAlignment(JLabel.CENTER);
  
   // Can't do this too early:
    tdat.est.getModel().collapseView(!advanced_);     
    UiUtil.gbcSet(gbc, 0, 1, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    tdat.panel.add(tablePan, gbc);
    return (tdat);
  }
  
  
  /***************************************************************************
  **
  ** Stock the entry controls
  */ 
  
  private JPanel buildEntryControls(TabData tdat, String mappedID, boolean doForTable, 
                                    ResourceManager rMan, GridBagConstraints gbc, TimeCourseData tcd) {       
 
    JPanel tcGeneControls = new JPanel();
    tcGeneControls.setLayout(new GridBagLayout()); 

   
    tdat.gene = tcd.getTimeCourseData(mappedID);
    
    if (tdat.gene == null) {
      tdat.isNew = true;
      Iterator<GeneTemplateEntry> template = tcd.getGeneTemplate();
      tdat.gene = new TimeCourseGene(mappedID, template);
    } else {
      tdat.isNew = false;
    }

    tdat.confBox = new JComboBox(buildConfidenceChoices());
    final TabData forButton = tdat; 
    tdat.confBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          EnumCell.ReadOnlyEnumCellEditor ece = ((TimeCourseTableModel)forButton.est.getModel()).reviseConfidenceDisplay();
          ece.revalidate();
          forButton.est.getModel().fireTableDataChanged();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });

    int row = 0;
    JLabel label = null;
    if (doForTable) {
      label = new JLabel(rMan.getString("tcentry.name"));
      tdat.nameField = new JTextField();
      UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
      tcGeneControls.add(label, gbc);
      UiUtil.gbcSet(gbc, 1, row, 10, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
      tcGeneControls.add(tdat.nameField, gbc);
      row++;
    }
        
    label = new JLabel(rMan.getString("tcentry.defaultConfidence"));          
    UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, row, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.confBox, gbc);    
 
    label = new JLabel(rMan.getString("tcentry.note"));  
    tdat.noteField = new JTextField();
    UiUtil.gbcSet(gbc, 3, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(label, gbc);
    UiUtil.gbcSet(gbc, 4, row, 3, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    tcGeneControls.add(tdat.noteField, gbc);    
   
    tdat.internalChoice = new JCheckBox(rMan.getString("tcentry.internalOnly"));
    UiUtil.gbcSet(gbc, 7, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.internalChoice, gbc);
    
    return (tcGeneControls);  
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
  ** Stock the gene data controls
  */ 
  
  private void extractGeneData(TabData tdat) {       
    tdat.confBox.setSelectedIndex(tdat.gene.getConfidence());
    tdat.noteField.setText(tdat.gene.getTimeCourseNote());
    if (tdat.nameField != null) {
      tdat.nameField.setText(tdat.gene.getName());
    }
    tdat.internalChoice.setSelected(tdat.gene.isInternalOnly());
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply the data to the model
  */ 
  
  private boolean applyGeneData(TimeCourseData tcd, TimeCourseDataMaps tcdm, TabData tdat, UndoSupport support) {
    if (tdat.nameField != null) {
      String currName = tdat.gene.getName();
      String newName = tdat.nameField.getText().trim();
      if (!newName.equals(currName)) {
        if (newName.equals("")) {
          ResourceManager rMan = uics_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tcentry.nameBlank"), 
                                        rMan.getString("tcentry.nameBlankTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }        
        if (!tcd.nameIsUnique(newName)) {
          ResourceManager rMan = uics_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("tcentry.nameNotUnique"), 
                                        rMan.getString("tcentry.nameNotUniqueTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
        
        //
        // If name change disconnects data from network elements links by default
        // association, we let user choose how to handle it:
        //
        if (!DataUtil.keysEqual(newName, currName)) {        
          Set<String> nodes = tcdm.getTimeCourseDataKeyInverses(currName);
          if (!nodes.isEmpty()) {
            //
            // We need to study the set of associated nodes.  Some may be default 
            // associated by name, others may be associated by custom map.
            //
            boolean haveDefault = false;
            Iterator<String> nit = nodes.iterator();
            while (nit.hasNext()) {
              String nid = nit.next();
              if (!tcdm.haveCustomMapForNode(nid)) {
                haveDefault = true;
                break;
              }
            }
            if (haveDefault) {
              ResourceManager rMan = uics_.getRMan();
              String desc = MessageFormat.format(rMan.getString("dataTables.disconnecting"), 
                                                 new Object[] {currName, newName});
              JOptionPane.showMessageDialog(uics_.getTopFrame(), desc, 
                                            rMan.getString("dataTables.disconnectingTitle"),
                                            JOptionPane.WARNING_MESSAGE);
            }
          }
        }
        tdat.gene.setName(newName);        
        TimeCourseChange[] tcc = tcdm.changeTimeCourseMapsToName(currName, newName);
        for (int i = 0; i < tcc.length; i++) {
          support.addEdit(new TimeCourseChangeCmd(tcc[i]));
        }
      }
    }
    tdat.gene.setConfidence(tdat.confBox.getSelectedIndex());
    tdat.gene.setTimeCourseNote(tdat.noteField.getText().trim());
    tdat.gene.setInternalOnly(tdat.internalChoice.isSelected());
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Enabling the strategy source choice is pretty complex!
  */
 
  public static class StratSourceTracker extends TrackingUnit {
  
    private List<EnumCell> exprCol_;
    private List<EnumChoiceContent<ExpressionEntry.Source>> srcCol_;
    private List<EnumCell> startStratCol_;
    private List<EnumCell> endStratCol_;

    public StratSourceTracker(List<EnumCell> exprCol, List<EnumChoiceContent<ExpressionEntry.Source>> srcCol, List<EnumCell> startStratCol, List<EnumCell> endStratCol) {
      exprCol_ = exprCol;
      srcCol_ = srcCol;
      startStratCol_ = startStratCol;
      endStratCol_ = endStratCol;     
    }
    
    public boolean isEnabled(int row) {      
      int exprVal = (exprCol_.get(row)).value;
      ExpressionEntry.Source srcVal = srcCol_.get(row).val;
      int ssVal = (startStratCol_.get(row)).value;
      int esVal = (endStratCol_.get(row)).value;
      
      if ((exprVal != ExpressionEntry.EXPRESSED) &&  (exprVal != ExpressionEntry.WEAK_EXPRESSION) && (exprVal != ExpressionEntry.VARIABLE)) {
        return (false);
      }
      if (srcVal == ExpressionEntry.Source.NO_SOURCE_SPECIFIED) {
        return (false);
      }
      if ((ssVal == ExpressionEntry.NO_STRATEGY_SPECIFIED) && (esVal == ExpressionEntry.NO_STRATEGY_SPECIFIED)) {
        return (false);
      }
      return (true);    
    }
  }
  
  /***************************************************************************
  **
  ** Used for tracking tabs
  */

  class TabData {
    TimeCourseGene gene;
    boolean isNew;
    EditableTable est;
    JPanel panel;
    JTextField nameField;
    JComboBox confBox;
    JTextField noteField;
    JCheckBox internalChoice;
    HashSet<ExpressionEntry.Source> goodbyeChannels_;
    
    TabData() {
      goodbyeChannels_ = new HashSet<ExpressionEntry.Source>();     
    }
  }
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class TimeCourseTableModel extends EditableTable.TableModel {
    
    final static int TIME         = 0;
    final static int REGION       = 1;
    final static int VALUE        = 2;
    final static int ACTIVITY     = 3;   
    final static int CONFIDENCE   = 4;
    final static int SOURCE       = 5;
    final static int STRAT_SOURCE = 6;
    final static int START_STRAT  = 7;
    final static int END_STRAT    = 8;
    private final static int NUM_COL_ = 9; 
    
    private final static int HIDDEN_CONFIDENCE_ = 0;
    private final static int NUM_HIDDEN_        = 1;
    
    private TabData tdat_;
    private StratSourceTracker sst_;
    
    class TableRow {
      String time;
      String region;
      EnumCell value;
      ProtoDouble activity;   
      EnumCell confidence;
      EnumChoiceContent<ExpressionEntry.Source> source;
      EnumChoiceContent<ExpressionEntry.Source> stratSource;
      EnumCell startStrat;
      EnumCell endStrat;
      Integer hiddenConfidence;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        time = (String)columns_[TIME].get(i);
        region = (String)columns_[REGION].get(i);       
        value = (EnumCell)columns_[VALUE].get(i);
        activity = (ProtoDouble)columns_[ACTIVITY].get(i);
        confidence = (EnumCell)columns_[CONFIDENCE].get(i);
        source = (EnumChoiceContent<ExpressionEntry.Source>)columns_[SOURCE].get(i);
        stratSource = (EnumChoiceContent<ExpressionEntry.Source>)columns_[STRAT_SOURCE].get(i);
        startStrat = (EnumCell)columns_[START_STRAT].get(i);
        endStrat = (EnumCell)columns_[END_STRAT].get(i);       
        hiddenConfidence = (Integer)hiddenColumns_[HIDDEN_CONFIDENCE_].get(i);
      }
      
      void toCols() {
        columns_[TIME].add(time); 
        columns_[REGION].add(region);
        columns_[VALUE].add(value);
        columns_[ACTIVITY].add(activity);      
        columns_[CONFIDENCE].add(confidence);
        columns_[SOURCE].add(source);
        columns_[STRAT_SOURCE].add(stratSource);
        columns_[START_STRAT].add(startStrat);
        columns_[END_STRAT].add(endStrat);
    
        hiddenColumns_[HIDDEN_CONFIDENCE_].add(hiddenConfidence);
        return;
      }
    }
  
    TimeCourseTableModel(UIComponentSource uics, String displayUnits, TabData td) {
      super(uics, NUM_COL_);
      tdat_ = td;
      ResourceManager rMan = uics.getRMan();
      String timeColumnHeading = MessageFormat.format(rMan.getString("tcentry.timeColFormat"), new Object[] {displayUnits});
      sst_ = new StratSourceTracker(columns_[VALUE], columns_[SOURCE], columns_[START_STRAT], columns_[END_STRAT]);

      colNames_ = new String[] {timeColumnHeading,
                                "tcentry.region",
                                "tcentry.value",
                                "tcentry.activity",                                
                                "tcentry.confidence",
                                "tcentry.source", 
                                "tcentry.stratSource", 
                                "tcentry.startStrat",
                                "tcentry.endStrat"};
      colUseDirect_ = new boolean[] {true,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false};
      
      colClasses_ = new Class[] {String.class,      
                                 String.class,
                                 EnumCell.class,
                                 ProtoDouble.class,
                                 EnumCell.class,
                                 EnumChoiceContent.class,
                                 EnumChoiceContent.class,
                                 EnumCell.class,          
                                 EnumCell.class};
      canEdit_ = new boolean[] {false,
                                false,
                                true,
                                true, //changes....
                                true,
                                true, // changes...
                                true, // changes...
                                true,
                                true};
      addHiddenColumns(NUM_HIDDEN_);
      collapseCount_ = 5;  // Enables table collapse
    }
    
    List getValueColumn() {
      return (columns_[VALUE]); 
    }
    
    StratSourceTracker getStratSourceTracker() {
      return (sst_); 
    }
  
    public boolean isCellEditable(int r, int c) {
      try {
        if (c == ACTIVITY) {
          EnumCell currVala = (EnumCell)columns_[VALUE].get(r);
          return (currVala.value == ExpressionEntry.VARIABLE);          
        } else if (c == SOURCE) {
          EnumCell currVala = (EnumCell)columns_[VALUE].get(r);
          return ((currVala.value == ExpressionEntry.VARIABLE) || 
                  (currVala.value == ExpressionEntry.EXPRESSED) || 
                  (currVala.value == ExpressionEntry.WEAK_EXPRESSION));          
        } else if (c == STRAT_SOURCE) {
          return (sst_.isEnabled(r));
        } else if ((c == END_STRAT) || (c == START_STRAT)) {
          EnumCell currVala = (EnumCell)columns_[VALUE].get(r);
          return ((currVala.value == ExpressionEntry.EXPRESSED) || 
                  (currVala.value == ExpressionEntry.WEAK_EXPRESSION));         
        } else {
          return (super.isCellEditable(r, c));
        }

      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (false);
    }
    
    //
    // If evidence/sign of one instance changes, all instances change.
    //
    
    public boolean comboEditingDone(int col, int row, Object val) {
      if (col == VALUE) {
        return (activityEditingDone(row, val));
      } else if (col == CONFIDENCE) {
        return (confidenceEditingDone(row, val));
      } else if (col == SOURCE) {
        return (sourceEditingDone(col, row, val));
      } else if ((col == SOURCE) || (col == START_STRAT) || (col == END_STRAT)) {
        return (generalEditingDone(col, row, val));
      } else {
        return (true);
      }
    }    
    
    public boolean activityEditingDone(int row, Object val) {
      EnumCell newAct = (EnumCell)val;   
      EnumCell currAct = (EnumCell)columns_[VALUE].get(row);
      if (currAct.value == newAct.value) {
        return (true);
      }
      // Have to get the column next door to repaint as active/inactive:
      tmqt_.invalidate();
      tmqt_.validate();
      tmqt_.repaint();
      return (true);  
    }    
    
    public boolean generalEditingDone(int col, int row, Object val) {
      EnumCell newAct = (EnumCell)val;   
      EnumCell currAct = (EnumCell)columns_[col].get(row);
      if (currAct.value == newAct.value) {
        return (true);
      }
      // Have to get the column next door to repaint as active/inactive:
      tmqt_.invalidate();
      tmqt_.validate();
      tmqt_.repaint();
      return (true);  
    }
    
    public boolean sourceEditingDone(int col, int row, Object val) {
      EnumChoiceContent<ExpressionEntry.Source> newAct = (EnumChoiceContent<ExpressionEntry.Source>)val;   
      EnumChoiceContent<ExpressionEntry.Source> currAct = (EnumChoiceContent<ExpressionEntry.Source>)columns_[col].get(row);
      if (currAct.val == newAct.val) {
        return (true);
      }
      // Have to get the column next door to repaint as active/inactive:
      tmqt_.invalidate();
      tmqt_.validate();
      tmqt_.repaint();
      return (true);  
    }
  
    public boolean confidenceEditingDone(int row, Object val) {
      EnumCell newConf = (EnumCell)val;
      EnumCell currConf = (EnumCell)columns_[CONFIDENCE].get(row);
      if (currConf.value == newConf.value) {
        return (true);
      }
      int displayConfidence = newConf.value;
      
      int baseConfidence = tdat_.confBox.getSelectedIndex();
      int entryConfidence = 
        TimeCourseGene.reverseMapEntryConfidence(baseConfidence, displayConfidence);
      hiddenColumns_[HIDDEN_CONFIDENCE_].set(row, new Integer(entryConfidence));
      return (true);
    }
    
    EnumCell.ReadOnlyEnumCellEditor reviseConfidenceDisplay() {
      columns_[CONFIDENCE].clear();
      Iterator ccit = hiddenColumns_[HIDDEN_CONFIDENCE_].iterator();
      TableColumn tc = tdat_.est.getTable().getColumnModel().getColumn(CONFIDENCE);   
      EnumCell.ReadOnlyEnumCellEditor ece = (EnumCell.ReadOnlyEnumCellEditor)tc.getCellEditor();
      int editRow = ece.getCurrRow();
      ChoiceContent selConf = (ChoiceContent)tdat_.confBox.getSelectedItem();
      int numEnum = confidenceEnum_.size();
      int rowNum = 0;
      while (ccit.hasNext()) {
        Integer hidConf = (Integer)ccit.next();
        int entryConfidence = hidConf.intValue();
        int baseConfidence = selConf.val;
        int displayConfidence = TimeCourseGene.mapEntryConfidence(baseConfidence, entryConfidence);
        EnumCell displayCell = null; 
        for (int i = 0; i < numEnum; i++) {
          EnumCell ecr = (EnumCell)confidenceEnum_.get(i);
          if (ecr.value == displayConfidence) {
            displayCell = new EnumCell(ecr);
            columns_[CONFIDENCE].add(displayCell);
            break;
          }
        }
        if (editRow == rowNum++) {
          ece.reviseValue(displayCell);
        }
      }      
      return (ece);  
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
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current Time course data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    Iterator tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = (TabData)tdit.next();
      List entries = buildTableRows(tdat);
      tdat.est.getModel().extractValues(entries);
      extractGeneData(tdat);
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
    ResourceManager rMan = uics_.getRMan();
    String text = rMan.getString((advanced_) ? "dialogs.collapse" : "dialogs.expand");
    buttonAdv_.setText(text);
    Iterator tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = (TabData)tdit.next();
      TimeCourseTableModel tctd = (TimeCourseTableModel)tdat.est.getModel();
      tctd.collapseView(!advanced_);
    }
    buttonAdv_.revalidate();
    return;
  } 
  
  /***************************************************************************
  **
  ** Build table rows
  */ 
  
  private List<TimeCourseTableModel.TableRow> buildTableRows(TabData tDat) {
    TimeCourseTableModel tctm = (TimeCourseTableModel)tDat.est.getModel();
    ArrayList<TimeCourseTableModel.TableRow> retval = new ArrayList<TimeCourseTableModel.TableRow>();
    
    Iterator<ExpressionEntry> eeit = tDat.gene.getExpressions();
    while (eeit.hasNext()) {
      ExpressionEntry entry = eeit.next();
      TimeCourseTableModel.TableRow tr = tctm.new TableRow();    
      Integer timeIndex = new Integer(entry.getTime());
      tr.time = TimeAxisDefinition.getTimeDisplay(tad_, timeIndex, false, false);        
      tr.region = entry.getRegion();
      
      int expr = entry.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      int esize = expressEnum_.size();
      for (int i = 0; i < esize; i++) {
        EnumCell ecr = expressEnum_.get(i);
        if (ecr.value == expr) {
          tr.value = new EnumCell(ecr);
          break;
        }
      }
      
      tr.activity = (expr == ExpressionEntry.VARIABLE) 
        ? new ProtoDouble(entry.getVariableLevelForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED)) 
        : new ProtoDouble("");
 
      ExpressionEntry.Source source = entry.getSource();
      int rsize = sourceEnum_.size();
      for (int i = 0; i < rsize; i++) {
        EnumChoiceContent<ExpressionEntry.Source> ecr = sourceEnum_.get(i);
        if (ecr.val == source) {
          tr.source = new EnumChoiceContent<ExpressionEntry.Source>(ecr.name, ecr.val, i);
          break;
        }
      }
      
      int entryConfidence = entry.getConfidence();  
      tr.hiddenConfidence = new Integer(entryConfidence);
      int showConf = tDat.gene.mapEntryConfidence(entryConfidence);      
      int csize = confidenceEnum_.size();
      for (int i = 0; i < csize; i++) {
        EnumCell ecr = confidenceEnum_.get(i);
        if (ecr.value == showConf) {
          tr.confidence = new EnumCell(ecr);
          break;
        }
      }
      
      ExpressionEntry.Source stratSource = entry.getStrategySource();
      rsize = sourceEnum_.size();
      for (int i = 0; i < rsize; i++) {
        EnumChoiceContent<ExpressionEntry.Source> ecr = sourceEnum_.get(i);
        if (ecr.val == stratSource) {
          tr.stratSource = new EnumChoiceContent<ExpressionEntry.Source>(ecr.name, ecr.val, i);
          break;
        }
      }
           
      int startStrat = entry.getRawStartStrategy();
      int ssize = stratEnum_.size();
      for (int i = 0; i < ssize; i++) {
        EnumCell ecr = stratEnum_.get(i);
        if (ecr.value == startStrat) {
          tr.startStrat = new EnumCell(ecr);
          break;
        }
      }
      int endStrat = entry.getRawEndStrategy();
      for (int i = 0; i < ssize; i++) {
        EnumCell ecr = stratEnum_.get(i);
        if (ecr.value == endStrat) {
          tr.endStrat = new EnumCell(ecr);
          break;
        }
      }      
      retval.add(tr);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the time course data
  ** 
  */
  
  private boolean applyProperties() {

    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      if (!checkValues(tdat)) {
        return (false);
      }
    }    

    tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      //
      // Undo/Redo support
      //
   
      UndoSupport support = uFac_.provideUndoSupport("undo.tcentry", dacx_);
      if (tdat.isNew) {
        support.addEdit(new TimeCourseChangeCmd(tcData_.addTimeCourseGene(tdat.gene)));
      }
      TimeCourseChange tcc = tcData_.startGeneUndoTransaction(tdat.gene.getName());
      
      boolean ok = applyGeneData(tcData_, tcdm_, tdat, support);
      // Only time this is false is for single-shot panels, i.e. no partial
      // undo commits should occur.
      if (!ok) {
        if (tdit.hasNext()) {
          throw new IllegalStateException();
        }
        return (false);
      }
      
      applyTableValues(tdat);
      tcc = tcData_.finishGeneUndoTransaction(tdat.gene.getName(), tcc);
      support.addEdit(new TimeCourseChangeCmd(tcc));
     
      //
      // If maps to channels are going to be left dangling by the loss of a source
      // channel, handle that now!
      //
      
      if (!tdat.goodbyeChannels_.isEmpty()) {
        Iterator<ExpressionEntry.Source> gcit = tdat.goodbyeChannels_.iterator();
        while (gcit.hasNext()) {
          ExpressionEntry.Source channelObj = gcit.next();
          TimeCourseChange[] tccm = tcdm_.dropMapsToEntrySourceChannel(tdat.gene.getName(), channelObj);
          for (int i = 0; i < tccm.length; i++) {
            support.addEdit(new TimeCourseChangeCmd(tccm[i]));
          }
          UiUtil.fixMePrintout("WORKSHEET do something here!!!");
          // public ModelBuilder.Undo handleSourceChannelDeletion(String tcNodeName, int channel);         
        }   
      }
   
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      support.finish();
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Check table rows for correctness
  */ 
     
  private boolean checkValues(TabData tdat) {
        
    if (tdat.gene == null) {
      return (true);
    }
     
    List vals = tdat.est.getModel().getValuesFromTable();   
    if (vals.isEmpty()) {
      return (true);
    }

    int size = vals.size();
    for (int i = 0; i < size; i++) {
      TimeCourseTableModel.TableRow tr = (TimeCourseTableModel.TableRow)vals.get(i); 
      if (tr.value.value != ExpressionEntry.VARIABLE) {
        continue;
      }
      if (!tr.activity.valid) {
        DoubleEditor.triggerWarning(uics_.getHandlerAndManagerSource(), uics_.getTopFrame());
        return (false);
      }
      if ((tr.activity.value < 0.0) || (tr.activity.value > 1.0)) {
        ResourceManager rMan = uics_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("tcentry.badActivityValue"), 
                                      rMan.getString("tcentry.badActivityValueTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
      
    //
    // Make sure that the edge strategies are consistent.  No strategy should
    // exist for non-edges, and a pulse cannot be on and off at the same time.
    //

    Iterator<ExpressionEntry> eeit = tdat.gene.getExpressions();
    ArrayList<ExpressionEntry> newExp = new ArrayList<ExpressionEntry>();
    boolean haveFixed = false;
    boolean haveVar = false;
    int count = 0;
    while (eeit.hasNext()) {        
      ExpressionEntry entry = eeit.next();
      TimeCourseTableModel.TableRow tr = (TimeCourseTableModel.TableRow)vals.get(count++); 
      ExpressionEntry ee = resolveToNewEntry(entry, tr);
      int eeVal = ee.getRawExpression();
      if (eeVal == ExpressionEntry.VARIABLE) {
        haveVar = true;
      } else if ((eeVal == ExpressionEntry.EXPRESSED) ||
                 (eeVal == ExpressionEntry.WEAK_EXPRESSION) ||
                 (eeVal == ExpressionEntry.NOT_EXPRESSED)) {               
        haveFixed = true; 
      }
      newExp.add(ee);
    }

    if (haveVar && haveFixed) {
      ResourceManager rMan = uics_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tcentry.mixed"),
                                    rMan.getString("tcentry.mixedTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }

    if (!checkStrategySourceMatch(newExp)) {
      return (false);
    }
      
    //
    // In the presence of multiple source states, we run the strategy checker on all
    // of the possible states:
    //
    
    Set<ExpressionEntry.Source> sources = TimeCourseGene.getSourceOptions(newExp);
    if (ExpressionEntry.hasMaternalChannel(sources)) {
      if (!checkStrategyPerChannel(newExp, ExpressionEntry.Source.MATERNAL_SOURCE)) {
        return (false);
      }
    }
  
    if (ExpressionEntry.hasZygoticChannel(sources)) { 
      if (!checkStrategyPerChannel(newExp, ExpressionEntry.Source.ZYGOTIC_SOURCE)) {
        return (false);
      }
    }
    
    if (ExpressionEntry.hasNoSourceChannel(sources)) { 
      if (!checkStrategyPerChannel(newExp, ExpressionEntry.Source.NO_SOURCE_SPECIFIED)) {
        return (false);
      }
    }
   
    //
    // If somebody is mapping to one of our source option channels, losing both of them
    // will require that the mapping get dumped!
    // Used to be that losing one would cause the dump of just that one, but we want e.g. a 
    // gene to be tracking the zygotic channel even in the absence of any zygotic data!
    //
     
    tdat.goodbyeChannels_.clear();
    Set<ExpressionEntry.Source> sourceOptions = tdat.gene.getSourceOptions();
    boolean maternalExists = ExpressionEntry.hasMaternalChannel(sourceOptions);
    boolean zygoticExists = ExpressionEntry.hasZygoticChannel(sourceOptions);
    Set<ExpressionEntry.Source> newsOpts = TimeCourseGene.getSourceOptions(newExp);
    boolean maternalExistsNew = ExpressionEntry.hasMaternalChannel(newsOpts);
    boolean zygoticExistsNew = ExpressionEntry.hasZygoticChannel(newsOpts);
    if ((maternalExists || zygoticExists) && !(maternalExistsNew || zygoticExistsNew)) {
      if (tcdm_.haveMapsToEntrySourceChannel(tdat.gene.getName(), ExpressionEntry.Source.MATERNAL_SOURCE)) {
        tdat.goodbyeChannels_.add(ExpressionEntry.Source.MATERNAL_SOURCE);
      }
      if (tcdm_.haveMapsToEntrySourceChannel(tdat.gene.getName(), ExpressionEntry.Source.ZYGOTIC_SOURCE)) {
        tdat.goodbyeChannels_.add(ExpressionEntry.Source.ZYGOTIC_SOURCE);
      }      
    }
    
    if (!tdat.goodbyeChannels_.isEmpty()) {
      ResourceManager rMan = uics_.getRMan();
      int ok = 
        JOptionPane.showConfirmDialog(uics_.getTopFrame(), rMan.getString("tcentry.channelMapLoss"),
                                      rMan.getString("tcentry.channelMapLossTitle"),
                                      JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Check table rows for correctness
  */ 
     
  private ExpressionEntry resolveToNewEntry(ExpressionEntry entry, TimeCourseTableModel.TableRow tr) {

    int startStrat;
    int endStrat;
    ExpressionEntry.Source exprSource;
    ExpressionEntry.Source stratSource;
    double varLev;
    
    if (tr.value.value == ExpressionEntry.VARIABLE) {
      varLev = tr.activity.value;
      exprSource = tr.source.val;
      stratSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      startStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;   
      endStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;   
    } else if (tr.value.value == ExpressionEntry.NO_DATA) {
      varLev = 0.0;
      exprSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      stratSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      startStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;   
      endStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;   
    } else if ((tr.value.value == ExpressionEntry.EXPRESSED) ||
               (tr.value.value == ExpressionEntry.WEAK_EXPRESSION)) {
      varLev = 0.0;
      exprSource = tr.source.val;
      startStrat = tr.startStrat.value;
      endStrat = tr.endStrat.value;        
      if ((startStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED) || (endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED)) {
        stratSource = tr.stratSource.val;
      } else {
        stratSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      }
    } else if (tr.value.value == ExpressionEntry.NOT_EXPRESSED) {
      varLev = 0.0;
      exprSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      stratSource = ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
      startStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;   
      endStrat = ExpressionEntry.NO_STRATEGY_SPECIFIED;         
    } else {
      throw new IllegalStateException();
    }
    return(new ExpressionEntry(entry.getRegion(), entry.getTime(),
                               tr.value.value, exprSource, 
                               tr.hiddenConfidence.intValue(), 
                               stratSource, startStrat, endStrat, varLev));
  }
  
  /***************************************************************************
  **
  ** Check strategy source consistency:
  */ 
  
  private boolean checkStrategySourceMatch(ArrayList<ExpressionEntry> newExp) { 
    Iterator<ExpressionEntry> eeit = newExp.iterator();
    boolean badNews = false;
    while (eeit.hasNext()) {
      ExpressionEntry entry = eeit.next();
      ExpressionEntry.Source src = entry.getSource();
      ExpressionEntry.Source stratSrc = entry.getStrategySource();
      int rawStart = entry.getRawStartStrategy();
      int rawEnd = entry.getRawEndStrategy();
      boolean gotStrat = ((rawStart != ExpressionEntry.NO_STRATEGY_SPECIFIED) || (rawEnd != ExpressionEntry.NO_STRATEGY_SPECIFIED));
      if (!gotStrat) {
        if (stratSrc != ExpressionEntry.Source.NO_SOURCE_SPECIFIED) {
          badNews = true;
          break;
        } else {
          continue;
        }
      }
      if (src == ExpressionEntry.Source.NO_SOURCE_SPECIFIED) {
        if (stratSrc != ExpressionEntry.Source.NO_SOURCE_SPECIFIED) {
          badNews = true;
          break;
        }
      } else if (src == ExpressionEntry.Source.MATERNAL_SOURCE) {
        if (stratSrc != ExpressionEntry.Source.MATERNAL_SOURCE) {
          badNews = true;
          break;
        }       
      } else if (src == ExpressionEntry.Source.ZYGOTIC_SOURCE) {
        if (stratSrc != ExpressionEntry.Source.ZYGOTIC_SOURCE) {
          badNews = true;
          break;
        }              
      } else if (src == ExpressionEntry.Source.MATERNAL_AND_ZYGOTIC) {
        if ((stratSrc != ExpressionEntry.Source.ZYGOTIC_SOURCE) && (stratSrc != ExpressionEntry.Source.MATERNAL_SOURCE)) {
          badNews = true;
          break;
        }
      } else {
        throw new IllegalStateException();
      }
    }
  
    if (badNews) {
      ResourceManager rMan = uics_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tcentry.badStrategySource"),
                                    rMan.getString("tcentry.badStrategySourceTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Check edge strategy on a per-channel basis:
  */ 
  
  private boolean checkStrategyPerChannel(ArrayList<ExpressionEntry> newExp, ExpressionEntry.Source exprSource) {
    HashMap<String, Integer> lastEntryStates = new HashMap<String, Integer>();   
    Iterator<ExpressionEntry> eeit = newExp.iterator();
    while (eeit.hasNext()) {
      ExpressionEntry entry = eeit.next();
      if (TimeCourseGene.strategyProblems(entry, exprSource, newExp.iterator(), lastEntryStates)) {
        ResourceManager rMan = uics_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tcentry.badStrategy"),
                                      rMan.getString("tcentry.badStrategyTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Apply table values
  */ 
   
  private void applyTableValues(TabData tdat) {        
    if (tdat.gene == null) {
      return;
    }
     
    List vals = tdat.est.getModel().getValuesFromTable();   
    if (vals.isEmpty()) {
      return;
    }
    
    int count = 0;
    Iterator<ExpressionEntry> eeit = tdat.gene.getExpressions();
    while (eeit.hasNext()) {        
      ExpressionEntry entry = eeit.next();
      TimeCourseTableModel.TableRow tr = (TimeCourseTableModel.TableRow)vals.get(count++); 
      ExpressionEntry ee = resolveToNewEntry(entry, tr);
      entry.copyInto(ee);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Build the Confidences
  ** 
  */
  
  private ArrayList<EnumCell> buildConfidenceEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    int numItems = TimeCourseGene.NUM_CONFIDENCE;
    for (int i = 0; i < numItems; i++) {
      String conf = TimeCourseGene.mapConfidence(i);
      retval.add(new EnumCell(conf, conf, i, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the Confidences
  ** 
  */
  
  private Vector<ChoiceContent> buildConfidenceChoices() { 
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    int numItems = TimeCourseGene.NUM_CONFIDENCE;
    for (int i = 0; i < numItems; i++) {
      String conf = TimeCourseGene.mapConfidence(i);
      retval.add(new ChoiceContent(conf, i));
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Build Expressions
  */
  
  private ArrayList<EnumCell> buildExpressionEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    int numItems = ExpressionEntry.NUM_EXPRESSIONS;
    for (int i = 0; i < numItems; i++) {
      String expr = ExpressionEntry.mapExpression(i);
      retval.add(new EnumCell(expr, expr, i, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build Sources
  */
  
  private ArrayList<EnumChoiceContent<ExpressionEntry.Source>> buildSourceEnum() { 
    ArrayList<EnumChoiceContent<ExpressionEntry.Source>> retval = new ArrayList<EnumChoiceContent<ExpressionEntry.Source>>();
    ResourceManager rMan = uics_.getRMan(); 
    StringBuffer buf = new StringBuffer();
    ExpressionEntry.Source[] vals = ExpressionEntry.Source.values();
    for (int i = 0; i < vals.length; i++) {    
      buf.setLength(0);
      buf.append("tcSourceOpt.");
      String srcTag = ExpressionEntry.mapToSourceTag(vals[i]);
      buf.append(srcTag);
      String fullTag = rMan.getString(buf.toString());
      retval.add(new EnumChoiceContent<ExpressionEntry.Source>(fullTag, vals[i], i));
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Build Strategies
  */
  
  private ArrayList<EnumCell> buildStrategyEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    ResourceManager rMan = uics_.getRMan(); 
    StringBuffer buf = new StringBuffer();
    int numItems = ExpressionEntry.NUM_STRATEGIES;
    for (int i = 0; i < numItems; i++) {
      buf.setLength(0);
      buf.append("stratRenderer.");
      String strat = ExpressionEntry.mapStrategy(i);
      buf.append((strat == null) ? "noStrat" : strat);
      String fullStrat = rMan.getString(buf.toString());
      retval.add(new EnumCell(fullStrat, strat, i, i));
    }
    return (retval);
  }
}
