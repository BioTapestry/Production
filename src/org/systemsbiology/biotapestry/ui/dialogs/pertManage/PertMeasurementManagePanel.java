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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureProps;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;

/****************************************************************************
**
** Panel for managing setup of perturbation environment
*/

public class PertMeasurementManagePanel extends AnimatedSplitManagePanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "pertMeasure";
  
  public final static String MEAS_KEY = "editMeas";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ReadOnlyTable rtdm_;
  private PerturbationData pd_;
  private PertMeasureAddOrEditPanel pmaep_;
  private String pendingKey_;
  private List<String> joinKeys_;
  private PertManageHelper pmh_;
  private PertFilterExpressionJumpTarget pfet_;
  private UndoFactory uFac_;
  private DataAccessContext dacx_;
  private TimeCourseData tcd_;
  
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
  
  public PertMeasurementManagePanel(UIComponentSource uics, FontManager fMgr, TimeAxisDefinition tad, 
                                    DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw, 
                                    PerturbationData pd, TimeCourseData tcd, PendingEditTracker pet, 
                                    PertFilterExpressionJumpTarget pfet) {
    super(uics, fMgr, tad, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    uFac_ = uFac;
    pfet_ = pfet;
    dacx_ = dacx;
    tcd_ = tcd;
    pmh_ = new PertManageHelper(uics_, pmw, pd, tcd, rMan_, gbc_, pet_);
         

    rtdm_ = new ReadOnlyTable(uics_, new MeasurementModel(uics_), new ReadOnlyTable.EmptySelector());      
       
    //
    // Build the measurement properties panel:
    //
    
    JPanel mPanel = buildMeasurementPanel(null);
    UiUtil.gbcSet(gbc_, 0, rowNum_++, 6, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(mPanel, gbc_);  
 
    pmaep_ = new PertMeasureAddOrEditPanel(uics_, fMgr, tcd_, parent_, pd_, this, MEAS_KEY);  
    addEditPanel(pmaep_, MEAS_KEY);
    
    finishConstruction();
    displayProperties(true);   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  @Override
  public void editIsComplete(String key, int what) {
    if (key.equals(MEAS_KEY)) {
      if (joinKeys_ == null) {
        editMeasure(key, what, dacx_);
      } else {
        joinMeasures(key, what, dacx_);
      }
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
 
  
  /***************************************************************************
  **
  ** Let us know if we are done sliding
  ** 
  */  
   @Override 
   public void finished() {
     rtdm_.makeCurrentSelectionVisible();
     return;
   } 
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    pmaep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    pmaep_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(MEAS_KEY)) {
      MeasEntry me = (MeasEntry)obj;
      return (me.key.equals(key));
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
    if (whichTable.equals(MEAS_KEY)) {
      pmh_.selectTableRow(rtdm_, whichKey, this, MEAS_KEY); 
    } else {
      throw new IllegalArgumentException();
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Handle duplication operations 
  */ 
  @Override
  public void doADuplication(String key) {
    if (key.equals(MEAS_KEY)) { 
      String useKey = ((MeasurementModel)rtdm_.getModel()).getSelectedKey(rtdm_.selectedRows);
      pendingKey_ = null;
      pmaep_.setMeasurePropsForDup(useKey);
      pmaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  @Override
  public void doAJoin(String key) {
    if (key.equals(MEAS_KEY)) { 
      joinKeys_ = ((MeasurementModel)rtdm_.getModel()).getSelectedKeys(rtdm_.selectedRows);
      MeasureDictionary md = pd_.getMeasureDictionary();
      Iterator<String> jkit = joinKeys_.iterator();
      String scaleKey = null;
      boolean mismatch = false;
      while (jkit.hasNext()) {
        String ckey = jkit.next();
        MeasureProps chkProps = md.getMeasureProps(ckey);
        String chkKey = chkProps.getScaleKey(); 
        if (scaleKey == null) {
          scaleKey = chkKey;
        } else if (!scaleKey.equals(chkKey)) {
          mismatch = true;
          break;
        } 
      }
      if (mismatch) {
        int ok = 
          JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.dangerousMergeScaleMismatch"),
                                        rMan_.getString("pmsmp.dangerousMergeScaleMismatchTitle"), 
                                        JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }
      pendingKey_ = pmaep_.setMeasurePropsForMerge(joinKeys_);
      pmaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle filter jumps
  */ 
  @Override
  public void doAFilterJump(String key) {
    if (key.equals(MEAS_KEY)) {
      String filterKey = ((MeasurementModel)rtdm_.getModel()).getSelectedKey(rtdm_.selectedRows);    
      PertFilter pertFilter = new PertFilter(PertFilter.Cat.MEASURE_TECH, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(pertFilter);
      pfet_.jumpWithNewFilter(pfe); 
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Handle top pane enabling
  */ 
  
  protected void enableTopPane(boolean enable) {  
    rtdm_.setEnabled(enable);
    return;
  } 
   
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {    
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> mprefs = da.getAllMeasurePropReferenceCounts();
    Integer noCount = new Integer(0);
    List<String> selKeys = (rtdm_.selectedRows == null) ? null :
                             ((MeasurementModel)rtdm_.getModel()).getSelectedKeys(rtdm_.selectedRows); 
    rtdm_.rowElements.clear(); 
    MeasureDictionary mDict = pd_.getMeasureDictionary();
    Iterator<String> mtit = mDict.getKeys();
    while (mtit.hasNext()) {
      String key = mtit.next();
      MeasureProps mp = mDict.getMeasureProps(key);
      Integer count = mprefs.get(key);
      if (count == null) {
        count = noCount;
      }        
      rtdm_.rowElements.add(new MeasEntry(mp, count));
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtdm_, this, MEAS_KEY, selKeys);
    return;
  } 

  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(MEAS_KEY)) {
      pendingKey_ = null;
      rtdm_.clearSelections(false);
      pmaep_.setEditMeasureProps(pendingKey_);
      pmaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle edit operations 
  */ 
  
  protected void doAnEdit(String key) {
    if (key.equals(MEAS_KEY)) { 
      pendingKey_ = ((MeasurementModel)rtdm_.getModel()).getSelectedKey(rtdm_.selectedRows);
      pmaep_.setEditMeasureProps(pendingKey_);
      pmaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  protected void doADelete(String key) {
    if (key.equals(MEAS_KEY)) {        
      String selected = ((MeasurementModel)rtdm_.getModel()).getSelectedKey(rtdm_.selectedRows);
      if (deleteMeasureProps(selected, dacx_)) {
        pet_.itemDeleted(MEAS_KEY);
      }
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Join measurement props
  */ 
  
  private void joinMeasures(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();  
    MeasureProps mp = pmaep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertMeasure", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getMeasurePropMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeMeasureProps(joinKeys_, pendingKey_, mp);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    joinKeys_ = null;
    String resultKey = pendingKey_;
    pendingKey_ = null;
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);   
    return;          
  }   
  
  /***************************************************************************
  **
  ** Edit measurement props
  ** 
  */
  
  private void editMeasure(String key, int what, DataAccessContext dacx) {
    MeasureProps mp = pmaep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createPertMeasure" 
                                                                         : "undo.editPertMeasure", dacx);
    if (pendingKey_ == null) {
      pendingKey_ = mp.getID();
    }      
    PertDataChange pdc = pd_.setMeasureProp(mp);
    String resultKey = pendingKey_;
    pendingKey_ = null;
    support.addEdit(new PertDataChangeCmd(pdc));
    Metabase mb = dacx.getMetabase();
    String tab = mb.getTabForPD(pd_);
    pd_.modifyForPertDataChange(support, new TabPinnedDynamicDataAccessContext(mb, tab));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;
  }
    
  /***************************************************************************
  **
  ** Build the perturbation measurement panel 
  */ 
  
  private JPanel buildMeasurementPanel(List<ReadOnlyTable> allTabs) {
   
    ResourceManager rMan = uics_.getRMan();
    //
    // Table:
    //
    
    JPanel display = new JPanel();
    display.setBorder(new EtchedBorder());
    display.setLayout(new GridBagLayout());
    rtdm_.setButtonHandler(new ButtonHand(MEAS_KEY));
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    tp.multiTableSelectionSyncing = allTabs;
    tp.clearOthersOnSelect = true;
    tp.tableTitle = rMan.getString("pertSetupManage.measurementTypes");
    tp.titleFont = null;
    tp.colWidths = new ArrayList();   
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(MeasurementModel.NAME, 50, 100, Integer.MAX_VALUE));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(MeasurementModel.SCALE, 50, 150, 250));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(MeasurementModel.POST, 50, 150, 250));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(MeasurementModel.NEGT, 50, 150, 250));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(MeasurementModel.COUNT, 50, 50, 100));
    tp.canMultiSelect = true;
    addExtraButtons(tp, MEAS_KEY);   
    JPanel tabPan = rtdm_.buildReadOnlyTable(tp);
    GridBagConstraints gbc = new GridBagConstraints();
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    display.add(tabPan, gbc); 
    return (display);  
  }
  
  /***************************************************************************
  **
  ** Pert measure deletion
  ** 
  */
  
  private boolean deleteMeasureProps(String key, DataAccessContext dacx) {  
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getMeasureReferenceSet(key);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }
       
    UndoSupport support = uFac_.provideUndoSupport("undo.deleteMeasureProp", dacx);  
    da.killOffDependencies(refs, tcd_, support);
    PertDataChange pdc2 = pd_.deleteMeasureProp(key);
    support.addEdit(new PertDataChangeCmd(pdc2));
    Metabase mb = dacx.getMetabase();
    String tab = mb.getTabForPD(pd_);
    pd_.modifyForPertDataChange(support, new TabPinnedDynamicDataAccessContext(mb, tab));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    return (true);
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for the measurement properties table
  */

  private class MeasurementModel extends ReadOnlyTable.TableModel {
    final static int NAME    = 0;
    final static int SCALE   = 1; 
    final static int POST    = 2;
    final static int NEGT    = 3;
    final static int COUNT   = 4;
    private final static int NUM_COL_ = 5;
    
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    MeasurementModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"pertMeas.name",
                                "pertMeas.scale",
                                "pertMeas.posThresh",
                                "pertMeas.negThresh",
                                "pertMeas.count"};
      colClasses_ = new Class[] {String.class,
                                 String.class,
                                 String.class,
                                 String.class,
                                 Integer.class};
      comparators_ = new Comparator[] {String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       new ReadOnlyTable.IntegerComparator()};   
      addHiddenColumns(NUM_HIDDEN_);      
    }    
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_NAME_ID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        MeasEntry me = (MeasEntry)iit.next();
        columns_[NAME].add(me.name);
        columns_[SCALE].add(me.scale); 
        columns_[POST].add(me.posThresh);
        columns_[NEGT].add(me.negThresh);
        columns_[COUNT].add(me.count);
        hiddenColumns_[HIDDEN_NAME_ID_].add(me.key);
      }
      return;
    }
    
    String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[0])));
    }   
    
    List<String> getSelectedKeys(int[] selected) {
      ArrayList<String> retval = new ArrayList<String>();
      for (int i = 0; i < selected.length; i++) {
        retval.add((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[i])));
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

  private class MeasEntry {

    String key;
    String name;
    String scale;
    String posThresh;
    String negThresh;
    Integer count;
    
    MeasEntry(MeasureProps mp, Integer count) {
      key = mp.getID();
      name = mp.getName();
      if (name == null) name = "";
      String scaleKey = mp.getScaleKey();
      scale = pd_.getMeasureDictionary().getMeasureScale(scaleKey).getDisplayString();
      Double posThreshVal = mp.getPosThresh();
      posThresh = (posThreshVal == null) ? "" : posThreshVal.toString();
      Double negThreshVal = mp.getNegThresh();
      negThresh = (negThreshVal == null) ? "" : negThreshVal.toString();
      this.count = count;
    }
  } 
}
