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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing setup of sources and targets
*/

public class PertInvestManagePanel extends AnimatedSplitManagePanel implements PertSimpleNameEditPanel.Client {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "invests";
  
  public final static String INVEST_KEY = "editInvest";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtdi_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private String pendingKey_;
  private PertSimpleNameEditPanel pip_;
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
  
  public PertInvestManagePanel(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw, 
                               PerturbationData pd, TimeCourseData tcd, TimeAxisDefinition tad, PendingEditTracker pet,  
                               PertFilterExpressionJumpTarget pfet) {
    super(uics, dacx.getFontManager(), tad, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    dacx_ = dacx;
    uFac_ = uFac;
    tcd_ = tcd;
    pmh_ = new PertManageHelper(uics_, pmw, pd, tcd, rMan_, gbc_, pet_);
    pfet_ = pfet;
    
    rtdi_ = new ReadOnlyTable(uics_, new PertManageHelper.NameWithHiddenIDAndRefCountModel(uics_, "pdim.invName"), new ReadOnlyTable.EmptySelector()); 
       
    //
    // Build the investigator  panel:
    //
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();     
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.NAME, 100, 150, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.REF_COUNT, 50, 50, 100));
   
    JPanel srcsPanel = commonTableBuild(rtdi_, "pertDefInvestManage.invests", new ButtonHand(INVEST_KEY), null, colWidths, INVEST_KEY);
    
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(srcsPanel, gbc_);  
    
    //
    // Edit panels:
    //
 
    pip_ = new PertSimpleNameEditPanel(uics_, parent_, this, "pdim.investName", this, INVEST_KEY);
    addEditPanel(pip_, INVEST_KEY);
    
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
  ** Need these guys to use the simple name edit panel:
  ** 
  */
   
   public String getNameForKey(String whoAmI, String key) {
     if (whoAmI.equals(INVEST_KEY)) {
       return (pd_.getInvestigator(key));     
     } else {
       throw new IllegalArgumentException();
     }
   }
    
   public boolean haveDuplication(String whoAmI, String key, String name) {
     String existingKey;
     if (whoAmI.equals(INVEST_KEY)) {
       existingKey = pd_.getInvestKeyFromName(name);     
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
     if (whoAmI.equals(INVEST_KEY)) {
       return (false);
     } else {
       throw new IllegalArgumentException();
     }
   }

  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  public void editIsComplete(String key, int what) {
    if (key.equals(INVEST_KEY)) {
      if (joinKeys_ == null) {
        editAnInvest(key, what, dacx_);
      } else {
        joinInvestigators(key, what, dacx_);
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
     rtdi_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    pip_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    pip_.closeAction();
    return;
  } 
    
  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(INVEST_KEY)) {
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
    if (whichTable.equals(INVEST_KEY)) {
      pmh_.selectTableRow(rtdi_, whichKey, this, INVEST_KEY);
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a cancelled edit.
  ** 

  
  public void editIsCancelled(String key) {
    super.editIsCancelled(key); 
    if (pendingKey_ != null) {
      setTableSelection(key, pendingKey_);
      pendingKey_ = null;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  @Override
  public void doAJoin(String key) {
    if (key.equals(INVEST_KEY)) {
      joinKeys_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKeys(rtdi_.selectedRows);
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      Map<String, Integer> refCounts = da.getAllInvestigatorReferenceCounts(true);
      pendingKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_); 
      TreeSet<String> nameOptions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      int numk = joinKeys_.size();
      for (int i = 0; i < numk; i++) {
        String nextJk = joinKeys_.get(i);
        nameOptions.add(pd_.getInvestigator(nextJk));
      }
      pip_.setMergeName(pendingKey_, nameOptions, new ArrayList<String>(joinKeys_));
      pip_.startEditing();
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
    if (key.equals(INVEST_KEY)) {
      String useKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKey(rtdi_.selectedRows);
      HashSet<String> allInvests = new HashSet<String>();
      Iterator<String> ikeys = pd_.getInvestigatorKeys();
      while (ikeys.hasNext()) {
        String iKey = ikeys.next();
        allInvests.add(pd_.getInvestigator(iKey));
      }      
      String copyName = pmh_.getNewUniqueCopyName(allInvests, pd_.getInvestigator(useKey));
      pendingKey_ = null;
      pip_.setDupName(copyName);
      pip_.startEditing();  
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
    if (key.equals(INVEST_KEY)) {
      String filterKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKey(rtdi_.selectedRows);   
      PertFilter invFilter = new PertFilter(PertFilter.Cat.INVEST, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(invFilter);
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
    rtdi_.setEnabled(enable);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) { 
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Integer noCount = new Integer(0);    
    List<String> selKeys = (rtdi_.selectedRows == null) ? null :
                     ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKeys(rtdi_.selectedRows);  
    rtdi_.rowElements.clear(); 
    
    Map<String, Integer> invRefs = da.getAllInvestigatorReferenceCounts(false);
    Iterator<String> iit = pd_.getInvestigatorKeys();
    while (iit.hasNext()) {
      String key = iit.next();
      String invest = pd_.getInvestigator(key);
      Integer count = invRefs.get(key);
      if (count == null) {
        count = noCount;
      }
      TrueObjChoiceContent tocc = new TrueObjChoiceContent(invest, key);
      PertManageHelper.ToccWithRefCount twrc = 
        new PertManageHelper.ToccWithRefCount(tocc, count.intValue());
      rtdi_.rowElements.add(twrc);
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtdi_, this, INVEST_KEY, selKeys);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(INVEST_KEY)) {
      pendingKey_ = null;
      rtdi_.clearSelections(false);
      pip_.setEditName(pendingKey_); 
      pip_.startEditing();
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
    if (key.equals(INVEST_KEY)) {     
      pendingKey_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKey(rtdi_.selectedRows);
      pip_.setEditName(pendingKey_);
      pip_.startEditing();
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
    if (key.equals(INVEST_KEY)) {
      String deleteKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtdi_.getModel()).getSelectedKey(rtdi_.selectedRows);
      if (deleteAnInvestigator(deleteKey, dacx_)) {       
        pet_.itemDeleted(INVEST_KEY);
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
  ** Handle delete operations 
  */ 
  
  private boolean deleteAnInvestigator(String key, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getInvestReferenceSet(key);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }       
    UndoSupport support = uFac_.provideUndoSupport("undo.deleteInvestigator", dacx);
    PertDataChange pdc2 = pd_.deleteInvestigator(key);
    support.addEdit(new PertDataChangeCmd(pdc2));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    return (true);
  }

  /***************************************************************************
  **
  ** Join investigators
  */ 
  
  private void joinInvestigators(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String name = pip_.getResult();      
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertTargets", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getInvestigatorMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeInvestigatorNames(joinKeys_, pendingKey_, name);
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
  ** Edit an investigator
  ** 
  */
  
  public void editAnInvest(String key, int what, DataAccessContext dacx) { 
    String name = pip_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createInvestigatorName" 
                                                                         : "undo.editInvestigatorName", dacx);      
    if (pendingKey_ == null) {
      pendingKey_ = pd_.getNextDataKey();
    } 
    PertDataChange pdc = pd_.setInvestigator(pendingKey_, name);
    support.addEdit(new PertDataChangeCmd(pdc));    
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    String resultKey = pendingKey_;
    pendingKey_ = null;
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
}
