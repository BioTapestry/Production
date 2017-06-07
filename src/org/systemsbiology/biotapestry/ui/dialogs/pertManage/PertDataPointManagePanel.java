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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing perturbation data points summarizing available perturbation result sets
*/

public class PertDataPointManagePanel extends AnimatedSplitManagePanel 
                                      implements PertFilterPanel.Client, PertFilterExpressionJumpTarget {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "dataPts";
  public final static String DATA_ANNOT_KEY = "editDataAnnot";
  public final static String REG_RESTRICT_KEY = "editRegRestrict";
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private final static String DATA_KEY_ = "editData";  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ReadOnlyTable rtd_;
  private PerturbationData pd_;
  private PertDataPointEditPanel pdpep_;
  private PertDataAnnotAddOrEditPanel pdaep_;
  private PertRegRestrictAddOrEditPanel prraep_;
  private PertFilterPanel filtPanel_;
  private String currKey_;
  private String dupKey_;
  private PertFilterExpression currPfe_;
  private String[] stack_;
  private PertManageHelper pmh_;
  private JLabel tableLabel_;
  private String conversionKey_;
  private JComboBox displayScaleCombo_;
  private HashMap currentScaleState_;
  private boolean ignoreScaleChange_;
  private JLabel dispLab_;
  private UndoFactory uFac_;
  
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
  
  public PertDataPointManagePanel(UIComponentSource uics, DataAccessContext dacx, PerturbationsManagementWindow pmw,
                                  PerturbationData pd, 
                                  PendingEditTracker pet, int legacyModes, UndoFactory uFac) {
    super(uics, dacx, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    uFac_ = uFac;
    pmh_ = new PertManageHelper(uics, dacx, pmw, pd, rMan_, gbc_, pet_);
  
    //
    // Build the filter panel:
    //
    
    filtPanel_ = new PertFilterPanel(uics, dacx, pd, this);
    UiUtil.gbcSet(gbc_, 0, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);    
    topPanel_.add(filtPanel_, gbc_);
    
    //
    // Panel for holding table:
    //
    
    JPanel display = new JPanel();
    display.setBorder(new EtchedBorder());
    display.setLayout(new GridBagLayout());    
    
    //
    // User sets display scale.  Actaully, this is going to be displayed in the data
    // panel, but managed here
    //
    
    dispLab_ = new JLabel(rMan_.getString("pertManage.chooseScale"));
    displayScaleCombo_ = new JComboBox();
    displayScaleCombo_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          if (ignoreScaleChange_) {
            return;
          }
          TrueObjChoiceContent useTocc = (TrueObjChoiceContent)displayScaleCombo_.getSelectedItem();
          setDisplayScale((String)useTocc.val);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    initScalingState();
  
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);    
    display.add(dispLab_, gbc_);    
    UiUtil.gbcSet(gbc_, 1, 0, 9, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);    
    display.add(displayScaleCombo_, gbc_);
    
    //
    // Build the table:
    //  
    
    rtd_ = new ReadOnlyTable(uics_, dacx_);
    rtd_.rowElements = new ArrayList();
    rtd_.lateBinding(new PertDataTableModel(uics_, dacx_, rtd_.rowElements), new ReadOnlyTable.EmptySelector());
    rtd_.setButtonHandler(new ButtonHand(DATA_KEY_));   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    tp.multiTableSelectionSyncing = null;          
    tableLabel_ = new JLabel(rMan_.getString("pertManage.dataPointList"), JLabel.LEFT);
    tp.tableJLabel = tableLabel_;
    tp.titleFont = null;
    tp.colWidths = null;
    tp.userAddedButtons = new ArrayList();
    FixedJButton myButton = new FixedJButton(rMan_.getString("asmp.dupSelected"));
    myButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {   
          doADuplication(DATA_KEY_);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    ReadOnlyTable.ExtraButton eb = new ReadOnlyTable.ExtraButton(false, true, false, myButton);
    tp.userAddedButtons.add(eb); 
    JPanel tabPan = rtd_.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc_, 0, 1, 10, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    display.add(tabPan, gbc_);
       
    //
    // Finish the panel:
    //
    
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum_ += 10;
    topPanel_.add(display, gbc_);
    topPanel_.countOnlyMe(display);
       
    pdpep_ = new PertDataPointEditPanel(uics_, dacx_, parent_, pd, this, DATA_KEY_, legacyModes);
    addEditPanel(pdpep_, DATA_KEY_);
    
    pdaep_ = new PertDataAnnotAddOrEditPanel(uics_, dacx_, parent_, pd, this, DATA_ANNOT_KEY);
    addEditPanel(pdaep_, DATA_ANNOT_KEY);
    
    prraep_ = new PertRegRestrictAddOrEditPanel(uics_, dacx_, parent_, pd, this, REG_RESTRICT_KEY, legacyModes);
    addEditPanel(prraep_, REG_RESTRICT_KEY);
            
    stack_ = new String[2];
    stack_[0] = DATA_KEY_;
 
    finishConstruction();
    
    filtPanel_.stockFilterPanel();
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Find out (and do it) if we need to update scaling
  ** 
  */
  
  public boolean doScalingUpdate() {
    MeasureDictionary md = pd_.getMeasureDictionary();
    Vector scaleTypes = md.getConvertibleScaleOptions();
    int numScaleTypes = scaleTypes.size();
    HashSet newOptions = new HashSet();
    for (int i = 0; i < numScaleTypes; i++) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)scaleTypes.get(i);
      newOptions.add(tocc.val);
    }   
    boolean unchanged = true;
    Set currKeys = currentScaleState_.keySet();
    if (newOptions.equals(currKeys)) {
      Iterator scit = currKeys.iterator();
      while (scit.hasNext()) {
        String key = (String)scit.next();
        MeasureScale.Conversion conv = (MeasureScale.Conversion)currentScaleState_.get(key);
        MeasureScale ms = md.getMeasureScale(key);
        if (!conv.equals(ms.getConvToFold())) {
          unchanged = false;
          break;
        }
      }
    } else {
      unchanged = false;
    }
    
    if (unchanged) {
      return (false);
    }
    
    currentScaleState_ = generateScalingState(scaleTypes);     
    ResourceManager rMan = dacx_.getRMan();
    TrueObjChoiceContent nativeTocc = new TrueObjChoiceContent(rMan.getString("pertManage.nativeScaling"), null);
    scaleTypes.add(0, nativeTocc);
    TrueObjChoiceContent saveScaleComboTocc = (TrueObjChoiceContent)displayScaleCombo_.getSelectedItem();
    String selectedKey = (String)saveScaleComboTocc.val;
    ignoreScaleChange_ = true;
    UiUtil.replaceComboItems(displayScaleCombo_, scaleTypes);
    TrueObjChoiceContent toccScale;
    // May be gone if deleted; check this!
    if ((selectedKey == null) || (scaleTypes.size() == 1) || (md.getMeasureScale(selectedKey) == null)) {
      toccScale = nativeTocc;    
    } else {
      toccScale = md.getScaleChoice(selectedKey);
    }
    displayScaleCombo_.setSelectedItem(toccScale);
    ignoreScaleChange_ = false;
    setDisplayScale((String)toccScale.val);
    return (true);
  }  
    
  /***************************************************************************
  **
  ** Set the scale for conversion
  */  
  
  public void setDisplayScale(String key) {
    boolean change;
    if (key == null) {
      change = (conversionKey_ != null);
    } else {
      change = !key.equals(conversionKey_);
    }
    conversionKey_ = key;
    if (change) {
      displayProperties(true);
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Handle filtering
  ** 
  */  
  
  public void jumpWithNewFilter(PertFilterExpression pfe) {
    filtPanel_.setCurrentSettings(pfe);
    installNewFilter(pfe);
    return;
  }
    
  /***************************************************************************
  **
  ** Handle filtering
  ** 
  */  
  
  public void installNewFilter(PertFilterExpression pfe) {
    currPfe_ = pfe;
    displayProperties(true);
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  @Override
  public void editIsComplete(String key, int what) {
    String resultKey = currKey_;
    if (key.equals(DATA_KEY_)) {
      PertDataPoint pdp = pdpep_.getResult();
      List annotResult = pdpep_.getUpdatedAnnotResult();
      PerturbationData.RegionRestrict rrResult = pdpep_.getUpdatedRegionRestrictionResult();
      List userV = pdpep_.getUserVals();
      UndoSupport support = uFac_.provideUndoSupport((currKey_ == null) ? "undo.createDataPoint" 
                                                                        : "undo.editDataPoint", dacx_); 
      if (currKey_ == null) {
        resultKey = pdp.getID();
      }
      PertDataChange pdc = pd_.setDataPoint(pdp);
      support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      boolean allEmpty = true;
      int numV = (userV == null) ? 0 : userV.size();
      for (int i = 0; i < numV; i++) {
        String val = (String)userV.get(i);
        if (!val.trim().equals("")) {
          allEmpty = false;
        }
      }
      userV = (allEmpty) ? null : new ArrayList(userV);
      pdc = pd_.setUserFieldValues(resultKey, userV);
      if (pdc != null) {
        support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      }
      
      pdc = pd_.setFootnotesForDataPoint(resultKey, annotResult);
      if (pdc != null) {
        support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      }
      
      pdc = pd_.setRegionRestrictionForDataPoint(resultKey, rrResult);
      if (pdc != null) {
        support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      }
      
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
      pet_.editSubmissionBegins();
      support.finish();
      pet_.editSubmissionEnds();
    } else if (key.equals(DATA_ANNOT_KEY)) {
      pdpep_.updateAnnotations(pdaep_.getResult());      
    } else if (key.equals(REG_RESTRICT_KEY)) {
      pdpep_.updateRegionRestriction(prraep_.getResult());      
    } else {
      throw new IllegalArgumentException();
    }
    super.editIsComplete(key, what); 
    setTableSelection(key, resultKey);
    return;
  }
  
  /***************************************************************************
  **
  ** Push down for annot edit:
  ** 
  */
  
  public void editIsPushed(String key) {
    int pushMode = pdpep_.getMode();
    String pushKey = (pushMode == AnimatedSplitEditPanel.EDIT_MODE) ? currKey_ : dupKey_;
    if (key.equals(DATA_ANNOT_KEY)) {
      stack_[1] = DATA_ANNOT_KEY;
      setEditStack(stack_);
      pdaep_.setAnnots(pushKey, pdpep_.getUpdatedAnnotResult(), pushMode);
      pdaep_.startAPush();
    } else if (key.equals(REG_RESTRICT_KEY)) {
      stack_[1] = REG_RESTRICT_KEY;
      setEditStack(stack_);
      prraep_.setSources(pushKey, pdpep_.getUpdatedRegionRestrictionResult(), pushMode);
      prraep_.startAPush();
    } else {
      throw new IllegalArgumentException();
    }    
    super.editIsPushed(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we are done sliding
  ** 
  */  
    
  public void finished() {
    rtd_.makeCurrentSelectionVisible();
    return;
  }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) {
    filtPanel_.stockFilterPanel();
    PertFilterExpression pfe = filtPanel_.buildPertFilterExpr();
    installNewFilter(pfe);
    pdaep_.hotUpdate(mustDie);
    prraep_.hotUpdate(mustDie);
    pdpep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    pdaep_.closeAction();
    prraep_.closeAction();
    pdpep_.closeAction();
    return;
  } 

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(DATA_KEY_)) {
      PdEntry pse = (PdEntry)obj;
      return (pse.key.equals(key));
    } else {
      throw new IllegalArgumentException();
    }
  }
     
  /***************************************************************************
  **
  ** make a table selection
  */ 
  
  public void setTableSelection(String whichTable, String whichKey) {
    if (whichTable == null) {
      return;
    }
    if (whichTable.equals(DATA_KEY_)) {
      pmh_.selectTableRow(rtd_, whichKey, this, DATA_KEY_); 
    } else if (whichTable.equals(DATA_ANNOT_KEY)) {
      // Nothing...
    } else if (whichTable.equals(REG_RESTRICT_KEY)) {
      // Nothing...
    } else {
      throw new IllegalArgumentException();
    } 
    return;
  }
  
   /***************************************************************************
  **
  ** Handle duplication operations 
  */ 
  
  public void doADuplication(String key) {
    String useKey = ((PertDataTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
    currKey_ = null;
    dupKey_ = useKey;
    pdpep_.setDataPointForDup(dupKey_);
    pdpep_.startEditing();
    return;          
  } 
  
  /***************************************************************************
  **
  ** Handle top pane enabling
  */ 
  
  protected void enableTopPane(boolean enable) {
    dispLab_.setEnabled(enable);
    displayScaleCombo_.setEnabled(enable);
    rtd_.setEnabled(enable);
    filtPanel_.enableFilters(enable);
    return;
  }   
   
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {
    String yesStr = rMan_.getString("pertManage.yes");
    String noStr = rMan_.getString("pertManage.no");
    List pertData = pd_.getPerturbations(currPfe_);
    
    List selKeys = (rtd_.selectedRows == null) ? null :
                     ((PertDataTableModel)rtd_.getModel()).getSelectedKeys(rtd_.selectedRows);
    
    rtd_.rowElements.clear();
    int numPert = pertData.size();
    for (int i = 0; i < numPert; i++) {
      PertDataPoint pdp = (PertDataPoint)pertData.get(i);
      rtd_.rowElements.add(new PdEntry(pdp, pd_, yesStr, noStr));
    }
    filtPanel_.updateFilterRenderers(pertData);
    
     //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtd_, this, DATA_KEY_, selKeys);  
    String format = rMan_.getString("pertManage.dataPointListFormat");
    String desc = MessageFormat.format(format, new Object[] {new Integer(numPert)});  
    tableLabel_.setText(desc);
    tableLabel_.invalidate();
    tableLabel_.validate();
    return;
  }

  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    currKey_ = null;
    rtd_.clearSelections(false);
    pdpep_.setDataPoint(currKey_);
    pdpep_.startEditing();
    return;
  }
  
  /***************************************************************************
  **
  ** Handle edit operations 
  */ 
  
  protected void doAnEdit(String key) {
    currKey_ = ((PertDataTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
    pdpep_.setDataPoint(currKey_);
    pdpep_.startEditing();
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  protected void doADelete(String key) {
    String delKey = ((PertDataTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertDataPoint", dacx_);
    PertDataChange pdc = pd_.deleteDataPoint(delKey);
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));  
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();    
    pet_.itemDeleted(DATA_KEY_);   
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Initialize the scaling state
  ** 
  */
  
  private void initScalingState() {
    MeasureDictionary md = pd_.getMeasureDictionary();
    Vector scaleTypes = md.getConvertibleScaleOptions();
    currentScaleState_ = generateScalingState(scaleTypes);
    ignoreScaleChange_ = true;
    ResourceManager rMan = dacx_.getRMan();
    TrueObjChoiceContent nativeTocc = new TrueObjChoiceContent(rMan.getString("pertManage.nativeScaling"), null);
    scaleTypes.add(0, nativeTocc);
    UiUtil.replaceComboItems(displayScaleCombo_, scaleTypes);
    ignoreScaleChange_ = false;
    return;
  }  
   
   /***************************************************************************
  **
  ** Initialize the scaling state
  ** 
  */
  
  private HashMap generateScalingState(Vector scaleTypes) {
    HashMap retval = new HashMap();
    MeasureDictionary md = pd_.getMeasureDictionary();
    int numScaleTypes = scaleTypes.size();  
    for (int i = 0; i < numScaleTypes; i++) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)scaleTypes.get(i);
      MeasureScale ms = md.getMeasureScale((String)tocc.val);
      retval.put(tocc.val, ms.getConvToFold());
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
  ** Used for the perturbations table
  */

  class PertDataTableModel extends ReadOnlyTable.TableModel {

    private final static int PERT_    = 0;
    private final static int TARGET_  = 1; 
    private final static int TIME_    = 2;
    private final static int VALUE_   = 3;
    private final static int FORCED_  = 4;
    private final static int CTRL_    = 5;
    private final static int TECH_    = 6;
    private final static int INVEST_  = 7;
    private final static int BATCH_   = 8;
    private final static int DATE_    = 9;
    private final static int FOOTS_   = 10;
    private final static int COMMENT_ = 11;
    private final static int REG_RESTRICT_ = 12;
    private final static int NUM_COL_ = 13;   
    
    private ArrayList hidden_;
    
    PertDataTableModel(UIComponentSource uics, DataAccessContext dacx, List prsList) {
      super(uics, dacx, NUM_COL_);
      hidden_ = new ArrayList();
      colNames_ = new String[] {"pertData.pert",
                                "pertData.target",
                                "pertData.time",
                                "pertData.value",
                                "pertData.forced",
                                "pertData.control",
                                "pertData.technology",
                                "pertData.invest",
                                "pertData.batch",
                                "pertData.date",
                                "pertData.foots",
                                "pertData.comment",
                                "pertData.regRes"};
      comparators_ = new Comparator[] {String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       new PertManageHelper.TimeComparator(dacx_),
                                       new PertManageHelper.DoubleStrComparator(),
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER, // FIX ME FOR FOOTNOTES!
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER};
      extractValues(prsList); 
    }    
   
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hidden_.clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        PdEntry pde = (PdEntry)iit.next();
        columns_[PERT_].add(pde.perts);
        columns_[TARGET_].add(pde.target); 
        columns_[TIME_].add(pde.time);
        columns_[VALUE_].add(pde.value);
        columns_[FORCED_].add(pde.forced);
        columns_[CTRL_].add(pde.control);
        columns_[TECH_].add(pde.tech);
        columns_[INVEST_].add(pde.invest);
        columns_[BATCH_].add(pde.batch);
        columns_[DATE_].add(pde.date);
        columns_[FOOTS_].add(pde.footnote);
        columns_[COMMENT_].add(pde.comment);
        columns_[REG_RESTRICT_].add(pde.regRes);
        hidden_.add(pde.key);
      }
      return;
    }
    
    String getSelectedKey(int[] selected) {
      return ((String)hidden_.get(mapSelectionIndex(selected[0])));
    }
    
    public List getSelectedKeys(int[] selected) {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < selected.length; i++) {
        retval.add((String)hidden_.get(mapSelectionIndex(selected[i])));
      }
      return (retval);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the summary table
  */

  private class PdEntry {

    String perts;
    String time;
    String invest;
    String target;
    String value;
    String forced;
    String control;
    String tech;
    String comment;
    String batch;
    String date;
    String footnote;
    String regRes;
    String key;
    
    PdEntry(PertDataPoint pdp, PerturbationData pd, String yesStr, String noStr) {
      key = pdp.getID();
      invest = pdp.getInvestigatorDisplayString(pd);
      if (invest == null) invest = "";
      perts = pdp.getPertDisplayString(pd, PertSources.BRACKET_FOOTS);
      if (perts == null) perts = "";
      time = pdp.getTimeDisplayString(pd, false, false);
      if (time == null) time = "";
      target = pd.getAnnotatedTargetDisplay(pdp.getTargetKey());
      if (target == null) target = "";   
      value = pdp.getScaledDisplayValue(conversionKey_, pd_, true);
      if (value == null) value = "";
      Boolean forcedObj = pdp.getForcedSignificance();
      if (forcedObj == null) {
        forced = "";
      } else if (forcedObj.booleanValue()) {
        forced = yesStr;
      } else {
        forced = noStr;
      }
      String controlKey = pdp.getControl();
      if (controlKey == null) {
        control = "";
      } else {
        control = pd_.getConditionDictionary().getExprControl(controlKey).getDisplayString();
      }
      tech = pdp.getMeasurementDisplayString(pd);
      if (tech == null) tech = "";
      comment = pdp.getComment();
      if (comment == null) comment = "";
      batch = pdp.getBatchKey();
      if (batch == null) batch = "";
      date = pdp.getDate();
      if (date == null) date = "";
      List footList = pd.getDataPointNotes(key);
      footnote = (footList == null) ? "" : pd.getFootnoteListAsString(footList);
      PerturbationData.RegionRestrict rr = pdp.getRegionRestriction(pd);
      if (rr != null) {
        regRes = rr.getDisplayValue();
      } else {
        regRes = "";
      }
    }
  }
}
