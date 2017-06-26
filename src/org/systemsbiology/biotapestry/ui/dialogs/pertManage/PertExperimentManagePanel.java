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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.BatchCollision;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;

/****************************************************************************
**
** Window used to manage perturbation sources
*/

public class PertExperimentManagePanel extends AnimatedSplitManagePanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "sources";
  
  public final static String SOURCES_KEY = "editSrcs";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ReadOnlyTable rtd_;
  private PerturbationData pd_;
  private String pendingKey_;
  private PertExperimentAddOrEditPanel pssaep_;
  private PertManageHelper pmh_;
  private List<String> joinKeys_;
  private PertFilterExpressionJumpTarget pfet_;
  private UndoFactory uFac_;
  private TimeCourseData tcd_;
  private DataAccessContext dacx_;
  
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
  
  public PertExperimentManagePanel(UIComponentSource uics, DataAccessContext dacx, FontManager fMgr, UndoFactory uFac, PerturbationsManagementWindow pmw,
                                   PerturbationData pd, TimeCourseData tcd, TimeAxisDefinition tad, PendingEditTracker pet, 
                                   PertFilterExpressionJumpTarget pfet, int legacyModes) {
    super(uics, fMgr, tad, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    dacx_ = dacx;
    tcd_ = tcd;
    uFac_ = uFac;
    pfet_ = pfet;
    pmh_ = new PertManageHelper(uics_, pmw, pd, tcd, rMan_, gbc_, pet_);

    //
    // Build the tables:
    //
    
    rtd_ = new ReadOnlyTable(uics_);   
    rtd_.lateBinding(new PertSourcesTableModel(uics_, rtd_.rowElements), new ReadOnlyTable.EmptySelector());
    rtd_.setButtonHandler(new ButtonHand(SOURCES_KEY));
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan_.getString("pertSourcesManage.sourceList");
    tp.titleFont = null;
    tp.colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourcesTableModel.PERT, 100, 150, 200));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourcesTableModel.TIME, 100, 100, 200));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourcesTableModel.CONDS, 100, 100, 200));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourcesTableModel.INVEST, 100, 200, Integer.MAX_VALUE));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourcesTableModel.COUNT, 50, 50, 100));
    tp.canMultiSelect = true;
    addExtraButtons(tp, SOURCES_KEY);   
    JPanel tabPan = rtd_.buildReadOnlyTable(tp);
    tabPan.setMinimumSize(new Dimension(400, 100));
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    topPanel_.add(tabPan, gbc_); 
    
    //
    // Now do the edit panel:
    //
 
    pssaep_ = new PertExperimentAddOrEditPanel(uics_, parent_, pd_, tcd_, this, SOURCES_KEY, legacyModes); 
    addEditPanel(pssaep_, SOURCES_KEY);
  
    finishConstruction();    
    displayProperties(false);  
  }
  
  /***************************************************************************
  **
  ** Let us know if we are done sliding
  ** 
  */  
   
  @Override
   public void finished() {
     rtd_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  @Override
  public void editIsComplete(String key, int what) {
    if (key.equals(SOURCES_KEY)) {
      if (joinKeys_ == null) {
        editAnExperiment(key, what, dacx_);
      } else {
        joinExperiments(key, what, dacx_);
      }
    } else {
      throw new IllegalArgumentException();
    }    
    return;
  }
    
  /***************************************************************************
  **
  ** Edit a source name
  ** 
  */
  
  private void editAnExperiment(String key, int what, DataAccessContext dacx) {
    String resultKey = pendingKey_;
    Experiment psi = pssaep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createExperiment" 
                                                                         : "undo.editExperiment", dacx);
    PertDataChange pdc;
    if (pendingKey_ == null) {
      resultKey = psi.getID();
    }
    pdc = pd_.setExperiment(psi); 
    support.addEdit(new PertDataChangeCmd(pdc));  
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds(); 
    super.editIsComplete(key, what);  
    setTableSelection(SOURCES_KEY, resultKey);
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  private void joinExperiments(String key, int what, DataAccessContext dacx) {
    Experiment psi = pssaep_.getResult();
    Map<String, Map<String, List<String>>> collisions = pd_.mergeExperimentBatchCollisions(new HashSet<String>(joinKeys_));
    SortedMap<String, SortedMap<String, SortedMap<String, BatchCollision>>> batchDups = pd_.prepBatchCollisions(collisions, psi.getDisplayString(pd_, tad_));
    if (batchDups != null) {
      BatchDupReportDialog bdrd = new BatchDupReportDialog(uics_, parent_, batchDups, 
                                                           "batchDup.MergeDialog", "batchDup.MergeTable");
      bdrd.setVisible(true);
      if (!bdrd.keepGoing()) {
        return;
      }
    }
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    
    UndoSupport support = uFac_.provideUndoSupport("undo.mergeExperiments", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getExperimentMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);  
    PertDataChange[] pdc = pd_.mergeExperiments(joinKeys_, pendingKey_, psi);
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
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(SOURCES_KEY)) {
      PsEntry pse = (PsEntry)obj;
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
    if (whichTable.equals(SOURCES_KEY)) {
      pmh_.selectTableRow(rtd_, whichKey, this, SOURCES_KEY);
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    pssaep_.hotUpdate(mustDie);
    return;
  } 
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    pssaep_.closeAction();
    return;
  } 

  /***************************************************************************
  **
  ** Handle duplication operations 
  */ 
  
  @Override
  public void doADuplication(String key) {
    String useKey = ((PertSourcesTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
    pendingKey_ = null;
    pssaep_.setSourcesForDup(useKey);
    pssaep_.startEditing();
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  public void doAJoin(String key) {
    if (key.equals(SOURCES_KEY)) {
      joinKeys_ = ((PertSourcesTableModel)rtd_.getModel()).getSelectedKeys(rtd_.selectedRows);
      Iterator<String> jkit = joinKeys_.iterator();
      Experiment exp = null;
      boolean mismatch = false;
      while (jkit.hasNext()) {
        String ckey = jkit.next();
        Experiment chkExp = pd_.getExperiment(ckey);
        if (exp == null) {
          exp = chkExp;
        } else {
          if (exp.getTime() != chkExp.getTime()) {
            mismatch = true;
            break;
          }
          if (exp.getLegacyMaxTime() != chkExp.getLegacyMaxTime()) {
            mismatch = true;
            break;
          }
          if (!exp.getSources().equals(chkExp.getSources())) {
            mismatch = true;
            break;
          }
          if (!exp.getConditionKey().equals(chkExp.getConditionKey())) {
            mismatch = true;
            break;
          }
        } 
      }
      if (mismatch) {
        int ok = 
          JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.experimentMergeMismatch"),
                                        rMan_.getString("pmsmp.experimentMergeMismatchTitle"), 
                                        JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }
      pendingKey_ = pssaep_.setExperimentsForMerge(joinKeys_);
      pssaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle filter jumps
  */ 
  
  public void doAFilterJump(String key) {
    if (key.equals(SOURCES_KEY)) {
      String filterKey = ((PertSourcesTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
      PertFilter srcFilter = new PertFilter(PertFilter.Cat.EXPERIMENT, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(srcFilter);
      pfet_.jumpWithNewFilter(pfe); 
    } else  {
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
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    pendingKey_ = null;
    rtd_.clearSelections(false);
    pssaep_.setSources(pendingKey_);
    pssaep_.startEditing();
    return;
  }
  
  /***************************************************************************
  **
  ** Handle edit operations 
  */ 
  
  protected void doAnEdit(String key) {
    pendingKey_ = ((PertSourcesTableModel)rtd_.getModel()).getSelectedKey(rtd_.selectedRows);
    pssaep_.setSources(pendingKey_);
    pssaep_.startEditing();
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  protected void doADelete(String key) {
    if (deleteExpr(rtd_.selectedRows, tcd_, dacx_)) {
      pet_.itemDeleted(SOURCES_KEY);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle top pane enabling
  */ 
  
  protected void enableTopPane(boolean enable) {  
    rtd_.setEnabled(enable);
    return;
  } 
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {
    
    List<String> selKeys = (rtd_.selectedRows == null) ? null :
                             ((PertSourcesTableModel)rtd_.getModel()).getSelectedKeys(rtd_.selectedRows);
    rtd_.rowElements.clear();
    
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> expRefs = da.getAllExperimentReferenceCounts();
    Integer noCount = Integer.valueOf(0);
    
    Iterator<String> pskit = pd_.getExperimentKeys();
    while (pskit.hasNext()) {
      String key = pskit.next();
      Integer count = expRefs.get(key);
      if (count == null) {
        count = noCount;
      }     
      Experiment psi = pd_.getExperiment(key);
      rtd_.rowElements.add(new PsEntry(psi, count, pd_, tad_));
    }
   
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtd_, this, SOURCES_KEY, selKeys);
    return;
  } 
 
  /***************************************************************************
  **
  ** Delete an experiment
  ** 
  */
  
  private boolean deleteExpr(int[] selectedRows, TimeCourseData tcd, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String key = ((PertSourcesTableModel)rtd_.getModel()).getSelectedKey(selectedRows);
    DependencyAnalyzer.Dependencies refs = da.getExperimentReferenceSet(key);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }

    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertSourceInfo", dacx);
    da.killOffDependencies(refs, tcd, support);  
    PertDataChange pdc = pd_.deleteExperiment(key);
    support.addEdit(new PertDataChangeCmd(pdc));  
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
  ** Used for the perturbations table
  */

  class PertSourcesTableModel extends ReadOnlyTable.TableModel {

    final static int PERT    = 0;
    final static int TIME    = 1;
    final static int CONDS   = 2; 
    final static int INVEST  = 3; 
    final static int COUNT   = 4;
    private final static int NUM_COL_ = 5;
    
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
 
 
    PertSourcesTableModel(UIComponentSource uics, List prsList) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"pertSource.pert",
                                "pertSource.time",
                                "pertSource.conditions",
                                "pertSource.invest",
                                "pertSource.refCount"};
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
        PsEntry pse = (PsEntry)iit.next();
        columns_[PERT].add(pse.perts);
        columns_[TIME].add(pse.time);
        columns_[CONDS].add(pse.conds);
        columns_[INVEST].add(pse.invest);
        columns_[COUNT].add(pse.count);
        hiddenColumns_[HIDDEN_NAME_ID_].add(pse.key);
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

  private class PsEntry {

    String perts;
    String time;
    String conds;
    String invest;
    String key;
    Integer count;
    
    PsEntry(Experiment psi, Integer count, PerturbationData pd, TimeAxisDefinition tad) {
      key = psi.getID();
      invest = psi.getInvestigatorDisplayString(pd);
      if (invest == null) invest = "";
      perts = psi.getPertDisplayString(pd, PertSources.BRACKET_FOOTS);
      if (perts == null) perts = "";
      conds = psi.getCondsDisplayString(pd);
      if (conds == null) conds = "";
      time = psi.getTimeDisplayString(tad, true, true);
      if (time == null) time = "";
      this.count = count;
    }
  }
}
