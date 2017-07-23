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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.ConditionDictionary;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.ExperimentConditions;
import org.systemsbiology.biotapestry.perturb.ExperimentControl;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing setup of perturbation environment
*/

public class PertExpSetupManagePanel extends AnimatedSplitManagePanel implements PertSimpleNameEditPanel.Client {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "expSetup"; 
  
  public final static String EXC_KEY = "editExC";
  public final static String CONTROL_KEY = "editControl";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtexco_;
  private ReadOnlyTable rtctrl_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private PertSimpleNameEditPanel excep_;
  private PertSimpleNameEditPanel ctrlep_;    
  private String pendingKey_;
  private PertManageHelper pmh_;
  private List<String> joinKeys_;
  private PertFilterExpressionJumpTarget pfet_;
  private UndoFactory uFac_;
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
  
  public PertExpSetupManagePanel(UIComponentSource uics, FontManager fMgr, TimeAxisDefinition tad, 
                                 DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw,
                                 PerturbationData pd, TimeCourseData tcd, PendingEditTracker pet, 
                                 PertFilterExpressionJumpTarget pfet) {
    super(uics, fMgr, tad, pmw, pet, MANAGER_KEY);
    uFac_ = uFac;
    dacx_ = dacx;
    pd_ = pd;
    pfet_ = pfet;
    tcd_ = tcd;
    pmh_ = new PertManageHelper(uics_, pmw, pd, tcd, rMan_, gbc_, pet_);
 
    
    ArrayList<ReadOnlyTable> allTabs = new ArrayList<ReadOnlyTable>();

    rtexco_ = new ReadOnlyTable(uics_, new PertManageHelper.NameWithHiddenIDAndRefCountModel(uics_, "pmsm.condition"), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtexco_);
     
    rtctrl_ = new ReadOnlyTable(uics_, new PertManageHelper.NameWithHiddenIDAndRefCountModel(uics_, "pmsm.control"), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtctrl_);
    
    //
    // Build the experimental conditions panel:
    //
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.NAME, 100, 150, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.REF_COUNT, 50, 50, 100));
        
    JPanel excPanel = commonTableBuild(rtexco_, "pmsmp.expConditions", new ButtonHand(EXC_KEY), allTabs, colWidths, EXC_KEY);
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(excPanel, gbc_);  
    
    //
    // Build the experimental control panel:
    //
    
    JPanel ctrlPanel = commonTableBuild(rtctrl_, "pmsmp.expControls", new ButtonHand(CONTROL_KEY), allTabs, colWidths, CONTROL_KEY);
    UiUtil.gbcSet(gbc_, 1, rowNum_++, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(ctrlPanel, gbc_);  
       
    excep_ = new PertSimpleNameEditPanel(uics_, parent_, this, "pmsmp.conditionName", this, EXC_KEY);
    addEditPanel(excep_, EXC_KEY);
     
    ctrlep_ = new PertSimpleNameEditPanel(uics_, parent_, this, "pmsmp.controlName", this, CONTROL_KEY);
    addEditPanel(ctrlep_, CONTROL_KEY);
 
    finishConstruction();
    displayProperties(true);   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Need these guys to use the simple name edit panel:
  ** 
  */
   
   public String getNameForKey(String whoAmI, String key) {
     if (whoAmI.equals(EXC_KEY)) {
       return (pd_.getConditionDictionary().getExprConditions(key).getDescription());
     } else if (whoAmI.equals(CONTROL_KEY)) {
       return (pd_.getConditionDictionary().getExprControl(key).getDescription());
     } else {
       throw new IllegalArgumentException();
     }
   }
    
   public boolean haveDuplication(String whoAmI, String key, String name) {
     String existingKey;
     if (whoAmI.equals(EXC_KEY)) {
       existingKey = pd_.getConditionDictionary().getConditionKeyFromName(name);
     } else if (whoAmI.equals(CONTROL_KEY)) {
       existingKey = pd_.getConditionDictionary().getControlKeyFromName(name);    
     } else {
       throw new IllegalArgumentException();
     }     
     if (existingKey == null) {
       return (false);
     }
     if (joinKeys_ == null) {
       return (!((key != null) && existingKey.equals(key)));
     } else {
       return (!joinKeys_.contains(existingKey));
     }
   }
   
   public boolean haveDisconnect(String whoAmI, String key, String name) {
     return (false);
   }

  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  @Override
  public void editIsComplete(String key, int what) {
    if (key.equals(EXC_KEY)) {
      if (joinKeys_ == null) {
        editCondition(key, what, dacx_);
      } else {
        joinConditions(key, what, dacx_);
      }
    } else if (key.equals(CONTROL_KEY)) {
      if (joinKeys_ == null) {
        editControl(key, what, dacx_);
      } else {
        joinControls(key, what, dacx_);
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
    
   public void finished() {
     rtexco_.makeCurrentSelectionVisible();
     rtctrl_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    excep_.hotUpdate(mustDie);
    ctrlep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    excep_.closeAction();
    ctrlep_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(EXC_KEY) || tableID.equals(CONTROL_KEY)) {    
      PertManageHelper.ToccWithRefCount twrc = (PertManageHelper.ToccWithRefCount)obj;
      return (twrc.tocc.val.equals(key));
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
    if (whichTable.equals(EXC_KEY)) {
      pmh_.selectTableRow(rtexco_, whichKey, this, EXC_KEY);
    } else if (whichTable.equals(CONTROL_KEY)) {
      pmh_.selectTableRow(rtctrl_, whichKey, this, CONTROL_KEY);
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
    if (key.equals(EXC_KEY)) {
      String useKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKey(rtexco_.selectedRows);
      ConditionDictionary cd = pd_.getConditionDictionary();
      String copyName = pmh_.getUnique(cd.getExprConditionsOptions(), cd.getExprConditions(useKey).getDescription());
      pendingKey_ = null;  
      excep_.setDupName(copyName);
      excep_.startEditing();
    } else if (key.equals(CONTROL_KEY)) {
      String useKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKey(rtctrl_.selectedRows);
      ConditionDictionary cd = pd_.getConditionDictionary();
      String copyName = pmh_.getUnique(cd.getExprControlOptions(), cd.getExprControl(useKey).getDescription());
      pendingKey_ = null;
      ctrlep_.setDupName(copyName);
      ctrlep_.startEditing();  
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
    if (key.equals(EXC_KEY)) {
      joinKeys_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKeys(rtexco_.selectedRows);
      ConditionDictionary cd = pd_.getConditionDictionary();
      String stdco = cd.getStandardConditionKey();
      if (joinKeys_.contains(stdco)) {
        pendingKey_ = stdco;
      } else {
        DependencyAnalyzer da = pd_.getDependencyAnalyzer();
        Map<String, Integer>refCounts = da.getAllExprConditionReferenceCounts();
        pendingKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_); 
      }
      TreeSet nameOptions = new TreeSet(String.CASE_INSENSITIVE_ORDER);
      int numk = joinKeys_.size();     
      for (int i = 0; i < numk; i++) {
        String nextJk = (String)joinKeys_.get(i);
        nameOptions.add(cd.getExprConditions(nextJk).getDescription());
      }
      excep_.setMergeName(pendingKey_, nameOptions, new ArrayList(joinKeys_));
      excep_.startEditing();
    } else if (key.equals(CONTROL_KEY)) {
      joinKeys_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKeys(rtctrl_.selectedRows);
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      Map refCounts = da.getAllExprControlReferenceCounts();
      pendingKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_); 
      TreeSet nameOptions = new TreeSet(String.CASE_INSENSITIVE_ORDER);
      int numk = joinKeys_.size();
      ConditionDictionary cd = pd_.getConditionDictionary();
      for (int i = 0; i < numk; i++) {
        String nextJk = joinKeys_.get(i);
        nameOptions.add(cd.getExprControl(nextJk).getDescription());
      }
      ctrlep_.setMergeName(pendingKey_, nameOptions, new ArrayList(joinKeys_));
      ctrlep_.startEditing();  
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
    if (key.equals(EXC_KEY)) {
      String filterKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKey(rtexco_.selectedRows);
      PertFilter filter = new PertFilter(PertFilter.Cat.EXP_CONDITION, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(filter);
      pfet_.jumpWithNewFilter(pfe); 
    } else if (key.equals(CONTROL_KEY)) {
      String filterKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKey(rtctrl_.selectedRows);
      PertFilter filter = new PertFilter(PertFilter.Cat.EXP_CONTROL, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(filter);
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
    rtexco_.setEnabled(enable);
    rtctrl_.setEnabled(enable);
    return;
  } 
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {
    
    //
    // Experiment conditions
    //
    List<String> selKeys = (rtexco_.selectedRows == null) ? null :
                                     ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKeys(rtexco_.selectedRows);
      
    rtexco_.rowElements.clear();
    Vector<TrueObjChoiceContent> eco = pd_.getConditionDictionary().getExprConditionsOptions();
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> sdrefs = da.getAllExprConditionReferenceCounts();
    Integer noCount = new Integer(0);
    Iterator<TrueObjChoiceContent> ecit = eco.iterator();  
    while (ecit.hasNext()) {
      TrueObjChoiceContent tocc = ecit.next();
      Integer count = sdrefs.get((String)tocc.val);
      if (count == null) {
        count = noCount;
      }
      PertManageHelper.ToccWithRefCount twrc = 
        new PertManageHelper.ToccWithRefCount(tocc, count.intValue());
      rtexco_.rowElements.add(twrc);
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtexco_, this, EXC_KEY, selKeys);
 
    //
    // Experiment controls
    //
    
    selKeys = (rtctrl_.selectedRows == null) ? null :
                ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKeys(rtctrl_.selectedRows);   
    rtctrl_.rowElements.clear();
    Vector<TrueObjChoiceContent> ectrl = pd_.getConditionDictionary().getExprControlOptions();
    Map<String, Integer> ctrlRefs = da.getAllExprControlReferenceCounts();
    Iterator<TrueObjChoiceContent> ecrlit = ectrl.iterator();  
    while (ecrlit.hasNext()) {
      TrueObjChoiceContent tocc = ecrlit.next();
      Integer count = ctrlRefs.get((String)tocc.val);
      if (count == null) {
        count = noCount;
      }
      PertManageHelper.ToccWithRefCount twrc = 
        new PertManageHelper.ToccWithRefCount(tocc, count.intValue());
      rtctrl_.rowElements.add(twrc);
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtctrl_, this, CONTROL_KEY, selKeys);
    return;
  }  

  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(EXC_KEY)) {  
      pendingKey_ = null;
      rtexco_.clearSelections(false);
      rtctrl_.clearSelections(false);
      excep_.setEditName(pendingKey_); 
      excep_.startEditing();
    } else if (key.equals(CONTROL_KEY)) {  
      pendingKey_ = null;
      rtexco_.clearSelections(false);
      rtctrl_.clearSelections(false);
      ctrlep_.setEditName(pendingKey_); 
      ctrlep_.startEditing();
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
    if (key.equals(EXC_KEY)) {     
      String selKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKey(rtexco_.selectedRows);
      String std = pd_.getConditionDictionary().getStandardConditionKey();
      if (selKey.equals(std)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotEditDefault"),
                                      rMan_.getString("pmsmp.cannotEditDefaultTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
      pendingKey_ = selKey;
      excep_.setEditName(pendingKey_);
      excep_.startEditing();
    } else if (key.equals(CONTROL_KEY)) {  
      pendingKey_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKey(rtctrl_.selectedRows);
      ctrlep_.setEditName(pendingKey_); 
      ctrlep_.startEditing();    
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
    if (key.equals(EXC_KEY)) {
      String selKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtexco_.getModel()).getSelectedKey(rtexco_.selectedRows);
      String std = pd_.getConditionDictionary().getStandardConditionKey();
      if (selKey.equals(std)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotDeleteDefault"),
                                      rMan_.getString("pmsmp.cannotDeleteDefaultTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      DependencyAnalyzer.Dependencies refs = da.getExprConditionReferenceSet(selKey);
      if (!pmh_.warnAndAsk(refs)) {
        return;
      }
       
      UndoSupport support = uFac_.provideUndoSupport("undo.deleteExprCondition", dacx_);
      da.killOffDependencies(refs, tcd_, support);
      PertDataChange pdc = pd_.deleteExperimentConditions(selKey);
      support.addEdit(new PertDataChangeCmd(pdc));
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
      pet_.editSubmissionBegins();
      support.finish();
      pet_.editSubmissionEnds();
      pet_.itemDeleted(EXC_KEY);    
    } else if (key.equals(CONTROL_KEY)) {
      String selKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtctrl_.getModel()).getSelectedKey(rtctrl_.selectedRows);
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      DependencyAnalyzer.Dependencies refs = da.getExprControlReferenceSet(selKey);
      if (!pmh_.warnAndAsk(refs)) {
        return;
      }      
      UndoSupport support = uFac_.provideUndoSupport("undo.deleteExprControl", dacx_);
      da.killOffDependencies(refs, tcd_, support);
      PertDataChange pdc = pd_.deleteExperimentControl(selKey);
      support.addEdit(new PertDataChangeCmd(pdc));
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
      pet_.editSubmissionBegins();
      support.finish();
      pet_.editSubmissionEnds();
      pet_.itemDeleted(CONTROL_KEY);      
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
  ** Join controls
  */ 
  
  private void joinControls(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String name = ctrlep_.getResult();
    
    ConditionDictionary ecd = pd_.getConditionDictionary(); 
    ExperimentControl revisedECtrl = ecd.getExprControl(pendingKey_).clone();
    revisedECtrl.setDescription(name);   
    UndoSupport support = uFac_.provideUndoSupport("undo.mergeExpControls", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getExprControlMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeExprControls(joinKeys_, pendingKey_, revisedECtrl);
    for (int i = 0; i < pdc.length; i++) {
      support.addEdit(new PertDataChangeCmd(pdc[i]));
    }
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    String resultKey = pendingKey_;
    pendingKey_ = null;
    joinKeys_ = null;
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;          
  }
  
  /***************************************************************************
  **
  ** Join conditions
  */ 
  
  private void joinConditions(String key, int what, DataAccessContext dacx) { 
    ConditionDictionary ecd = pd_.getConditionDictionary(); 
    String std = ecd.getStandardConditionKey();
    boolean mustMatchStd = joinKeys_.contains(std);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();  
    String name = excep_.getResult();
    ExperimentConditions revisedECond = ecd.getExprConditions(pendingKey_).clone();
    revisedECond.setDescription(name);
    if (mustMatchStd) { 
      ExperimentConditions stdCond = ecd.getExprConditions(std);
      if (!revisedECond.equals(stdCond)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotChangeStd"),
                                      rMan_.getString("pmsmp.cannotChangeStdTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }      
    }
    
    UndoSupport support = uFac_.provideUndoSupport("undo.mergeExprConditions", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getExprConditionMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeExprConditions(joinKeys_, pendingKey_, revisedECond);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
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
  ** Edit condition
  ** 
  */
  
  private void editCondition(String key, int what, DataAccessContext dacx) {
    String name = excep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createConditionName" 
                                                                         : "undo.editConditionName", dacx); 
    ConditionDictionary ecd = pd_.getConditionDictionary(); 
    ExperimentConditions revisedEC;
    if (pendingKey_ == null) {
      pendingKey_ = ecd.getNextDataKey();
      revisedEC = new ExperimentConditions(pendingKey_, name);
    } else {
      revisedEC = ecd.getExprConditions(pendingKey_).clone();
      revisedEC.setDescription(name);
    }   
    PertDataChange pdc = pd_.setExperimentConditions(revisedEC);
    support.addEdit(new PertDataChangeCmd(pdc));
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    String resultKey = pendingKey_;
    pendingKey_ = null;

    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;
  }
  
  /***************************************************************************
  **
  ** Edit a control
  ** 
  */
  
  private void editControl(String key, int what, DataAccessContext dacx) {
    String name = ctrlep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createExpControl" 
                                                                         : "undo.editExpControl", dacx); 
    ConditionDictionary ecd = pd_.getConditionDictionary(); 
    ExperimentControl revisedECtrl;
    if (pendingKey_ == null) {
      pendingKey_ = ecd.getNextDataKey();
      revisedECtrl = new ExperimentControl(pendingKey_, name);
    } else {
      revisedECtrl = ecd.getExprControl(pendingKey_).clone();
      revisedECtrl.setDescription(name);
    }   
    PertDataChange pdc = pd_.setExperimentControl(revisedECtrl);
    support.addEdit(new PertDataChangeCmd(pdc));
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    String resultKey = pendingKey_;
    pendingKey_ = null;    
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;
  }
}
