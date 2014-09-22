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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import javax.swing.JPanel;
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
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.TimeAxisHelper;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

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

  private EditableTable est_;
  private EditableTable mcet_;
  private boolean namedStages_;
  private ArrayList origCols_;
  private ArrayList resultCols_;
  private ArrayList colorList_;
  
  private TreeMap origColorMap_;
  private TimeAxisDefinition tad_;
  private JComboBox scaleOptions_;
  private JCheckBox investMode_;
  
  private boolean resultInvestMode_;
  private MinMax resultDefaultSpan_;
  private String resultCurrentScale_;
  private Map resultCurrColors_;
  private TimeAxisHelper tah_;
  private DataAccessContext dacx_;
  
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
  
  public static QpcrSetupDialog qpcrSetupDialogWrapper(BTState appState,
                                                       DataAccessContext dacx,
                                                       List currColumns, Map currColors, 
                                                       String currentScale, boolean currentInvestMode,
                                                       MinMax currentNullDefaultSpan) {
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(appState, dacx);
      tasd.setVisible(true);
    }
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = appState.getRMan();
      JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                    rMan.getString("tcsedit.noTimeDefinition"), 
                                    rMan.getString("tcsedit.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }
    
    QpcrSetupDialog qsd = new QpcrSetupDialog(appState, dacx, currColumns, currColors, currentScale, 
                                              currentInvestMode, currentNullDefaultSpan);
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
  
  private QpcrSetupDialog(BTState appState, DataAccessContext dacx, List currColumns, Map currColors, 
                          String currentScale, boolean currentInvestMode, 
                          MinMax currentNullDefaultSpan) {
    super(appState, "qsedit.title", new Dimension(900, 500), 10);
    dacx_ = dacx;
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages(); 
       
    JLabel t1Lab = new JLabel(rMan_.getString("qsedit.setDisplayColumns"), JLabel.LEFT);
    addWidgetFullRow(t1Lab, true);
    
    est_ = new EditableTable(appState_, new ColumnTableModel(appState_, tad_, namedStages_), appState_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);
    addTable(tablePan, 8);
    
    scaleOptions_ = new JComboBox();
    JLabel sLab = new JLabel(rMan_.getString("qsedit.chooseScale"));
    addLabeledWidget(sLab, scaleOptions_, true, false);
     
    investMode_ = new JCheckBox(rMan_.getString("qsedit.breakUpInvest"));
    addWidgetFullRow(investMode_, true);
     
    //
    // First time thru, we might needt to grab this from the TAD:
    //
 
    if (currentNullDefaultSpan == null) {
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      currentNullDefaultSpan = tad.getDefaultTimeSpan();
    }
    
    tah_ = new TimeAxisHelper(appState, dacx_, this, currentNullDefaultSpan.min, currentNullDefaultSpan.max);
    JPanel helper = tah_.buildHelperPanel();
    JLabel dLab = new JLabel(rMan_.getString("qsedit.defaultSpan"));
    addLabeledWidget(dLab, helper, true, false);
  
    //
    // measurement->color table:
    //
  
    JLabel t2Lab = new JLabel(rMan_.getString("qsedit.setColors"), JLabel.LEFT);
    addWidgetFullRow(t2Lab, true);
    
    colorList_ = buildColors();    
    mcet_ = new EditableTable(appState_, new MeasureColorTableModel(appState_), appState_.getTopFrame());
    etp = new EditableTable.TableParams();
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.tableIsUnselectable = true;
    etp.perColumnEnums = new HashMap();
    etp.perColumnEnums.put(new Integer(MeasureColorTableModel.COLOR), new EditableTable.EnumCellInfo(false, colorList_));
    tablePan = mcet_.buildEditableTable(etp);
    addTable(tablePan, 4);
       
    finishConstruction(); 
    displayProperties(currColumns, currColors, currentScale, currentInvestMode, currentNullDefaultSpan);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  /***************************************************************************
  **
  ** Get the column results
  ** 
  */
  
  public List getColumnResults() {
    return (resultCols_);
  }
  
  /***************************************************************************
  **
  ** Get the investigator display mode
  ** 
  */
  
  public boolean getInvestModeResults() {
    return (resultInvestMode_);
  }
  
  /***************************************************************************
  **
  ** Get the default span result
  ** 
  */
  
  public MinMax getSpanResult() {
    return (resultDefaultSpan_);
  }
  
  /***************************************************************************
  **
  ** Get the display scale result
  ** 
  */
  
  public String getScaleResult() {
    return (resultCurrentScale_);
  }
  
  /***************************************************************************
  **
  ** Get the color result
  ** 
  */
  
  public Map getColorResults() {
    return (resultCurrColors_);
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
   
    ColumnTableModel(BTState appState, TimeAxisDefinition tad, boolean namedStages) {
      super(appState, NUM_COL_);
      String displayUnits = tad.unitDisplayString();
      ResourceManager rMan = appState_.getRMan();
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
      Iterator cit = prsList.iterator();
      while (cit.hasNext()) {
        MinMax next = (MinMax)cit.next();
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

    boolean applyValues() {
    
      ResourceManager rMan = appState_.getRMan();
      if (appState_.getDB().getPertData().columnDefinitionsLocked()) { 
        resultCols_ = new ArrayList(origCols_); 
        return (true);
      }
      
      List vals = getValuesFromTable();
      if (vals.isEmpty()) {
        return (false);
      }
     
      //
      // Make sure the integers are OK, non-overlapping, etc:
      //
      
      resultCols_ = new ArrayList();
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
            JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("qsedit.badStageName"),
                                          rMan.getString("qsedit.badStageNameTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            resultCols_ = null;
            return (false);
          }   
        } else {      
          ProtoInteger min = (ProtoInteger)val[0];
          ProtoInteger max = (ProtoInteger)val[1];   
          if (((min == null) || (!min.valid)) ||
              ((max == null) || (!max.valid))) {
            IntegerEditor.triggerWarning(appState_, appState_.getTopFrame());
            resultCols_ = null;
            return (false);
          }
          minTimeVal = min.value;
          maxTimeVal = max.value;
        }

        if ((minTimeVal <= lastMax) || (minTimeVal > maxTimeVal)) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("qsedit.badBounds"),
                                                 rMan.getString("qsedit.badBoundsTitle"),
                                                 JOptionPane.ERROR_MESSAGE);
          resultCols_ = null;
          return (false); 
        }
        lastMax = maxTimeVal;
        resultCols_.add(new MinMax(minTimeVal, maxTimeVal));
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
  
    MeasureColorTableModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"qpcrSetup.measureType",
                                "qpcrSetup.color"};
      colClasses_ = new Class[] {String.class,
                                 EnumCell.class};
      canEdit_ = new boolean[] {false,
                                true};
      addHiddenColumns(NUM_HIDDEN_);
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
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties(List currColumns, Map currColors, String currentScale, 
                                 boolean currentInvestMode, MinMax currentNullDefaultSpan) {
    
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    MeasureDictionary md = pd.getMeasureDictionary();
    
    Vector scaleTypes = md.getScaleOptions();
    UiUtil.replaceComboItems(scaleOptions_, scaleTypes);   
    
    TrueObjChoiceContent toccScale = md.getScaleChoice(currentScale);
    scaleOptions_.setSelectedItem(toccScale);
      
    investMode_.setSelected(currentInvestMode);
    
    if (currentNullDefaultSpan == null) {
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      currentNullDefaultSpan = tad.getDefaultTimeSpan();
    }
    
    // Default span had to be loaded in constructor!
 

    origCols_ = new ArrayList();
    Iterator colIt = currColumns.iterator();
    while (colIt.hasNext()) {
      MinMax nextCol = (MinMax)colIt.next();
      origCols_.add(nextCol.clone());   
    }
    est_.getModel().extractValues(origCols_);
    
    //
    // FIX ME: legacy spanned data points prevent changes to column definitions
    // once they are in use.
    //     
     
    if (pd.columnDefinitionsLocked()) {
      est_.setEnabled(false);   
    }
      
    MeasureColorTableModel mctm = (MeasureColorTableModel)mcet_.getModel(); 
    origColorMap_ = new TreeMap();
    Iterator dcit = currColors.keySet().iterator();
    while (dcit.hasNext()) {
      MeasureColorTableModel.TableRow tr = mctm.new TableRow();
      tr.measureKey = (String)dcit.next();
      String colString = (String)currColors.get(tr.measureKey);
      int clsize = colorList_.size();
      for (int i = 0; i < clsize; i++) {
        EnumCell cCell = (EnumCell)colorList_.get(i);
        if (cCell.internal.equals(colString)) {
          tr.color = new EnumCell(cCell);
          break;
        }   
      }
      tr.measure = md.getMeasureProps(tr.measureKey).getName();
      origColorMap_.put(tr.measure, tr);
    }
    
    mcet_.getModel().extractValues(new ArrayList(origColorMap_.values()));
    return;
  }

  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() { 
    
    resultDefaultSpan_ = tah_.getSpanResult();
    if (resultDefaultSpan_ == null) {
      return (false);
    }
      
    ((ColumnTableModel)est_.getModel()).applyValues();
    
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)scaleOptions_.getSelectedItem();
    resultCurrentScale_ = (String)tocc.val;
  
    resultInvestMode_ = investMode_.isSelected();
    resultCurrColors_ = new HashMap();
    List vals = ((MeasureColorTableModel)mcet_.getModel()).getValuesFromTable();
    int num = vals.size();
    for (int i = 0; i < num; i++) {
      MeasureColorTableModel.TableRow tr = (MeasureColorTableModel.TableRow)vals.get(i);
      resultCurrColors_.put(tr.measureKey, tr.color.internal);
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
