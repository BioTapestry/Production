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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JOptionPane;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.PertDisplayOptions;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.TimeAxisHelper;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Dialog box for setting up QPCR Table Structure
*/

public class QpcrSetupDialog extends BTStashResultsDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private TimeAxisDefinition tad_;
  private TimeAxisHelper tah_;
  private PerturbationData pd_;
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private ArrayList<PerTab> ptDat_;
  private boolean namedStages_;
  
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Wrapper on constructor.  Have to define time axis before we can define
  ** qpcr structure.
  */ 
  
  public static QpcrSetupDialog qpcrSetupDialogWrapper(UIComponentSource uics,
                                                       DataAccessContext dacx,
                                                       List<PertDisplayOptions> currPdos, UndoFactory uFac) {
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
    
    QpcrSetupDialog qsd = new QpcrSetupDialog(uics, dacx, currPdos, uFac);
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
  *List<MinMax> currColumns, Map<String, String> currColors, 
                          String currentScale, boolean currentInvestMode, 
                          MinMax currentNullDefaultSpan
  */ 
  
  private QpcrSetupDialog(UIComponentSource uics, DataAccessContext dacx, List<PertDisplayOptions> currPdos, UndoFactory uFac) {
    super(uics, "qsedit.title", new Dimension(900, 500), 10);
    uics_ = uics;
    dacx_ = dacx;
    pd_ = dacx.getExpDataSrc().getPertData();
    uFac_ = uFac;
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages();
    ptDat_ = new ArrayList<PerTab>();
    
    //
    // Build the tabs.
    //

    JTabbedPane tabPane = new JTabbedPane();
    
    for (PertDisplayOptions pdo : currPdos) {
      PerTab pt = new PerTab(pdo);
      ptDat_.add(pt);
      JPanel pan = perTabPanel(pt, tad_);
      pt.tabPanel_ = pan;
      tabPane.addTab(rMan_.getString("propDialogs.layoutProp"), pt.tabPanel_);
    }
    addTable(tabPane, 4);
       
    finishConstruction();
    
    for (PerTab pt : ptDat_) {
      displayProperties(pt, tad_);
    }   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Get the results
  ** 
  */
  
  public PertDisplayOptions getResults(int tabNum) {
    PerTab pt = ptDat_.get(tabNum);
    PertDisplayOptions pdo = pt.currPdo_.clone();
    pdo.setColumns(pt.resultCols_);         
    pdo.setBreakOutInvestigatorMode(pt.resultInvestMode_);
    pdo.setNullDefaultSpan(pt.resultDefaultSpan_);  
    pdo.setPerturbDataDisplayScaleKey(pt.resultCurrentScale_); 
    pdo.setMeasurementDisplayColors(pt.resultCurrColors_);     
    return (pdo);
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
  
  class ColumnTableModel extends EditableTable.TableModel {
    
    private final static int MIN_TIME_ = 0;
    private final static int MAX_TIME_ = 1;
    private final static int NUM_COL_  = 2;  
    private static final long serialVersionUID = 1L;
   
    ColumnTableModel(UIComponentSource uics, TimeAxisDefinition tad, boolean namedStages) {
      super(uics, NUM_COL_);
      String displayUnits = tad.unitDisplayString();
      ResourceManager rMan = uics_.getRMan();
      String minHeading = MessageFormat.format(rMan.getString("qsedit.minTimeUnitFormat"), new Object[] {displayUnits});
      String maxHeading = MessageFormat.format(rMan.getString("qsedit.maxTimeUnitFormat"), new Object[] {displayUnits});
 
      colNames_ = new String[] {minHeading,
                                maxHeading};
      
      Class theClass = (namedStages) ? String.class : ProtoInteger.class;
      colClasses_ = new Class[] {theClass,
                                 theClass};
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        Object[] vals = new Object[2];
        vals[0] = columns_[MIN_TIME_].get(i);
        vals[1] = columns_[MAX_TIME_].get(i);
        retval.add(vals);
      }
      return (retval);
    }
    

    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator<MinMax> cit = prsList.iterator();
      while (cit.hasNext()) {
        MinMax next = cit.next();
        if (colClasses_[0].equals(String.class)) {
          TimeAxisDefinition.NamedStage stageName = tad_.getNamedStageForIndex(next.min);
          columns_[MIN_TIME_].add(stageName.name);
          stageName = tad_.getNamedStageForIndex(next.max);
          columns_[MAX_TIME_].add(stageName.name);
        } else {
          columns_[MIN_TIME_].add(new ProtoInteger(next.min));
          columns_[MAX_TIME_].add(new ProtoInteger(next.max));                  
        }
      }
      return;
    }

    boolean applyValues(PerTab pt) {
    
      ResourceManager rMan = uics_.getRMan();
      if (pd_.columnDefinitionsLocked()) { 
        pt.resultCols_ = new ArrayList<MinMax>(pt.origCols_); 
        return (true);
      }
      
      List vals = getValuesFromTable();
      if (vals.isEmpty()) {
        return (false);
      }
     
      //
      // Make sure the integers are OK, non-overlapping, etc:
      //
      
      pt.resultCols_ = new ArrayList<MinMax>();
      int size = vals.size();
      int lastMax = -1;
      for (int i = 0; i < size; i++) {     
        Object[] val = (Object[])vals.get(i);
        int minTimeVal;
        int maxTimeVal;
        if (namedStages_) {
          String minStageName = (String)val[0];
          String maxStageName = (String)val[1];
          minTimeVal = tad_.getIndexForNamedStage(minStageName);
          maxTimeVal = tad_.getIndexForNamedStage(maxStageName);
          if ((minTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME) ||
              (maxTimeVal == TimeAxisDefinition.INVALID_STAGE_NAME)) {
            JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("qsedit.badStageName"),
                                          rMan.getString("qsedit.badStageNameTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            pt.resultCols_ = null;
            return (false);
          }   
        } else {      
          ProtoInteger min = (ProtoInteger)val[0];
          ProtoInteger max = (ProtoInteger)val[1];   
          if (((min == null) || (!min.valid)) ||
              ((max == null) || (!max.valid))) {
            IntegerEditor.triggerWarning(uics_.getHandlerAndManagerSource(), uics_.getTopFrame());
            pt.resultCols_ = null;
            return (false);
          }
          minTimeVal = min.value;
          maxTimeVal = max.value;
        }

        if ((minTimeVal <= lastMax) || (minTimeVal > maxTimeVal)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("qsedit.badBounds"),
                                                 rMan.getString("qsedit.badBoundsTitle"),
                                                 JOptionPane.ERROR_MESSAGE);
          pt.resultCols_ = null;
          return (false); 
        }
        lastMax = maxTimeVal;
        pt.resultCols_.add(new MinMax(minTimeVal, maxTimeVal));
      }
      return (true);
    }
  }
  
 /***************************************************************************
  **
  ** Used for the data table
  */
  
  class MeasureColorTableModel extends EditableTable.TableModel {
    
    final static int MEASURE  = 0;
    final static int COLOR    = 1;
    private final static int NUM_COL_ = 2; 
    
    private final static int MEASURE_ID_ = 0;
    private final static int NUM_HIDDEN_ = 1;
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      String measureKey;
      String measure;
      EnumCell color;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        measure = (String)columns_[MEASURE].get(i);
        color = (EnumCell)columns_[COLOR].get(i);
        measureKey = (String)hiddenColumns_[MEASURE_ID_].get(i);
      }
      
      void toCols() {
        columns_[MEASURE].add(measure);  
        columns_[COLOR].add(color);
        hiddenColumns_[MEASURE_ID_].add(measureKey);
        return;
      }
    }
  
    MeasureColorTableModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"qpcrSetup.measureType",
                                "qpcrSetup.color"};
      colClasses_ = new Class[] {String.class,
                                 EnumCell.class};
      canEdit_ = new boolean[] {false,
                                true};
      addHiddenColumns(NUM_HIDDEN_);
    }
   
    public List getValuesFromTable() {
      ArrayList<TableRow> retval = new ArrayList<TableRow>();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator<TableRow> rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = rit.next();
        ent.toCols();
      }
      return;
    }
  }

  private static class PerTab {
    EditableTable est_;
    EditableTable mcet_;
    ArrayList<MinMax> origCols_;
    ArrayList<MinMax> resultCols_;
    ArrayList<EnumCell> colorList_;
    
    TreeMap<String, MeasureColorTableModel.TableRow> origColorMap_;
    JComboBox scaleOptions_;
    JCheckBox investMode_;
    
    boolean resultInvestMode_;
    MinMax resultDefaultSpan_;
    String resultCurrentScale_;
    Map<String, String> resultCurrColors_;
    
    PertDisplayOptions currPdo_;
    
    
    JPanel tabPanel_;
    
    PerTab(PertDisplayOptions pdo) {
      currPdo_ = pdo.clone();     
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the column results
  ** 
  */
  
  private JPanel perTabPanel(PerTab pt, TimeAxisDefinition tad) {
  
    JPanel jp = new JPanel();
    jp.setBorder(new EmptyBorder(20, 20, 20, 20));
    jp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    DialogSupport ptds = new DialogSupport(uics_, gbc);
    int rowNum = 0;
    
    JLabel t1Lab = new JLabel(rMan_.getString("qsedit.setDisplayColumns"), JLabel.LEFT);
   
   
    rowNum = ptds.addWidgetFullRow(jp, t1Lab, true, false, rowNum, 10);
    
    pt.est_ = new EditableTable(uics_, new ColumnTableModel(uics_, tad_, namedStages_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    JPanel tablePan = pt.est_.buildEditableTable(etp);
    rowNum = ptds.addTable(jp, tablePan, 8, rowNum, 10);
    
    pt.scaleOptions_ = new JComboBox();
    JLabel sLab = new JLabel(rMan_.getString("qsedit.chooseScale"));
    rowNum = ptds.addLabeledWidget(jp, sLab, pt.scaleOptions_, true, false, rowNum, 10);
     
    pt.investMode_ = new JCheckBox(rMan_.getString("qsedit.breakUpInvest"));
    rowNum = ptds.addWidgetFullRow(jp, pt.investMode_, true, false, rowNum, 10);
     
    //
    // First time thru, we might need to grab this from the TAD:
    //
  
    MinMax mm = pt.currPdo_.getNullPertDefaultSpan(tad);
    if (mm == null) {
      mm = tad.getDefaultTimeSpan().clone();
      pt.currPdo_.setNullDefaultSpan(mm);
    }
    
    tah_ = new TimeAxisHelper(uics_, dacx_, this, mm.min, mm.max, uFac_);
    JPanel helper = tah_.buildHelperPanel();
    JLabel dLab = new JLabel(rMan_.getString("qsedit.defaultSpan"));
    rowNum = ptds.addLabeledWidget(jp, dLab, helper, true, false, rowNum, 10);

  
    //
    // measurement->color table:
    //
  
    JLabel t2Lab = new JLabel(rMan_.getString("qsedit.setColors"), JLabel.LEFT);
    rowNum = ptds.addWidgetFullRow(jp, t2Lab, true, false, rowNum, 10);
    
    pt.colorList_ = buildColors();    
    pt.mcet_ = new EditableTable(uics_, new MeasureColorTableModel(uics_), uics_.getTopFrame());
    etp = new EditableTable.TableParams();
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.tableIsUnselectable = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(MeasureColorTableModel.COLOR), new EditableTable.EnumCellInfo(false, pt.colorList_, EnumCell.class));
    tablePan = pt.mcet_.buildEditableTable(etp);
    rowNum = ptds.addTable(jp, tablePan, 4, rowNum, 10);
    return (jp);
  }
  
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties(PerTab pt, TimeAxisDefinition tad) {
   
    MeasureDictionary md = pd_.getMeasureDictionary();
    
    Vector<TrueObjChoiceContent> scaleTypes = md.getScaleOptions();
    UiUtil.replaceComboItems(pt.scaleOptions_, scaleTypes);   
    
    TrueObjChoiceContent toccScale = md.getScaleChoice(pt.currPdo_.getPerturbDataDisplayScaleKey());
    pt.scaleOptions_.setSelectedItem(toccScale);
      
    pt.investMode_.setSelected(pt.currPdo_.breakOutInvestigators());
    
    MinMax mm = pt.currPdo_.getNullPertDefaultSpan(tad);
     
    if (mm == null) {
      pt.currPdo_.setNullDefaultSpan(tad_.getDefaultTimeSpan());
    }
    
    // Default span had to be loaded in constructor!
 
    pt.origCols_ = new ArrayList<MinMax>();
    Iterator<MinMax> colIt = pt.currPdo_.getColumnIterator();
    while (colIt.hasNext()) {
      MinMax nextCol = colIt.next();
      pt.origCols_.add(nextCol.clone());   
    }
    pt.est_.getModel().extractValues(pt.origCols_);
    
    //
    // FIX ME: legacy spanned data points prevent changes to column definitions
    // once they are in use.
    //     
     
    if (pd_.columnDefinitionsLocked()) {
      pt.est_.setEnabled(false);   
    }
      
    
    MeasureColorTableModel mctm = (MeasureColorTableModel)pt.mcet_.getModel(); 
    pt.origColorMap_ = new TreeMap<String, MeasureColorTableModel.TableRow>();
    Map<String, String> currColors = pt.currPdo_.getMeasurementDisplayColors();
    Iterator<String> dcit = currColors.keySet().iterator();
    while (dcit.hasNext()) {
      MeasureColorTableModel.TableRow tr = mctm.new TableRow();
      tr.measureKey = dcit.next();
      String colString = currColors.get(tr.measureKey);
      int clsize = pt.colorList_.size();
      for (int i = 0; i < clsize; i++) {
        EnumCell cCell = pt.colorList_.get(i);
        if (cCell.internal.equals(colString)) {
          tr.color = new EnumCell(cCell);
          break;
        }   
      }
      tr.measure = md.getMeasureProps(tr.measureKey).getName();
      pt.origColorMap_.put(tr.measure, tr);
    }
    
    pt.mcet_.getModel().extractValues(new ArrayList<MeasureColorTableModel.TableRow>(pt.origColorMap_.values()));
    return;
  }

  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() {    
    for (PerTab pt : ptDat_) {
      boolean ok = stashPerTab(pt);
      if (!ok) {
        return (false);
      }
    }                 
    return (true);
  }
   
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashPerTab(PerTab pt) { 
      
    pt.resultDefaultSpan_ = tah_.getSpanResult();
    if (pt.resultDefaultSpan_ == null) {
      return (false);
    }
      
    ((ColumnTableModel)pt.est_.getModel()).applyValues(pt);
    
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)pt.scaleOptions_.getSelectedItem();
    pt.resultCurrentScale_ = (String)tocc.val;
  
    pt.resultInvestMode_ = pt.investMode_.isSelected();
    pt.resultCurrColors_ = new HashMap<String, String>();
    List vals = ((MeasureColorTableModel)pt.mcet_.getModel()).getValuesFromTable();
    int num = vals.size();
    for (int i = 0; i < num; i++) {
      MeasureColorTableModel.TableRow tr = (MeasureColorTableModel.TableRow)vals.get(i);
      pt.resultCurrColors_.put(tr.measureKey, tr.color.internal);
    }
                  
    return (true);
  }

  /***************************************************************************
  **
  ** Build color list
  */
   
  private ArrayList<EnumCell> buildColors() {    
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    retval.add(new EnumCell("Black", "black", 0, 0));
    retval.add(new EnumCell("Aqua", "aqua", 1, 1));
    retval.add(new EnumCell("Blue", "blue", 2, 2));
    retval.add(new EnumCell("Fuchsia", "fuchsia", 3, 3));
    retval.add(new EnumCell("Gray", "gray", 4, 4));
    retval.add(new EnumCell("Green", "green", 5, 5));
    retval.add(new EnumCell("Lime", "lime", 6, 6));
    retval.add(new EnumCell("Maroon", "maroon", 7, 7));
    retval.add(new EnumCell("Navy", "navy", 8, 8));
    retval.add(new EnumCell("Olive", "olive", 9, 9));
    retval.add(new EnumCell("Purple", "purple", 10, 10));
    retval.add(new EnumCell("Red", "red", 11, 11));
    retval.add(new EnumCell("Silver", "silver", 12, 12));
    retval.add(new EnumCell("Teal", "teal", 13, 13));
    retval.add(new EnumCell("Yellow", "yellow", 14, 14));
    return (retval);
  }
  

}
