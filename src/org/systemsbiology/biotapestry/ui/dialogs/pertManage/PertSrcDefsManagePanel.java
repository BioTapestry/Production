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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
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
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing setup of sources and targets
*/

public class PertSrcDefsManagePanel extends AnimatedSplitManagePanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "srcDefs";
  
  public final static String PERT_DEF_KEY = "editPrtDef";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtdps_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private String pendingKey_;
  private PertSourceDefAddOrEditPanel pspp_;
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
  
  public PertSrcDefsManagePanel(UIComponentSource uics, FontManager fMgr, TimeAxisDefinition tad, DataAccessContext dacx, PerturbationsManagementWindow pmw, 
                                PerturbationData pd, TimeCourseData tcd, PendingEditTracker pet,  
                                PertFilterExpressionJumpTarget pfet, UndoFactory uFac) {
    super(uics, fMgr, tad, pmw, pet, MANAGER_KEY);
    uFac_ = uFac;
    pd_ = pd;
    tcd_ = tcd;
    dacx_ = dacx;
    pmh_ = new PertManageHelper(uics, pmw, pd_, tcd_, rMan_, gbc_, pet_);
    pfet_ = pfet;
    
    rtdps_ = new ReadOnlyTable(uics, new PertSourceModel(uics), new ReadOnlyTable.EmptySelector());
 
    //
    // Build the pert sources panel:
    //
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.SRC_NAME, 100, 200, 250));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.PERT_TYPE, 50, 100, 150));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.PROX_SIGN, 50, 200, 250));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.PROX_NAME, 50, 200, 250));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.ANNOTS, 50, 400, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertSourceModel.COUNT, 50, 50, 100));       
    JPanel pertSrcPanel = commonTableBuild(rtdps_, "psdm.pertDef", new ButtonHand(PERT_DEF_KEY), null, colWidths, PERT_DEF_KEY);
    
    UiUtil.gbcSet(gbc_, 0, rowNum_++, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(pertSrcPanel, gbc_);  
       
    //
    // Edit panels:
    //
    
    pspp_ = new PertSourceDefAddOrEditPanel(uics_, parent_, pd_, tcd_, this, PERT_DEF_KEY);
    addEditPanel(pspp_, PERT_DEF_KEY);

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
    if (key.equals(PERT_DEF_KEY)) {
      if (joinKeys_ == null) {
        editAPertDef(key, what, dacx_);
      } else {
        joinPertDefs(key, what, dacx_);
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
     rtdps_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    pspp_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    pspp_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(PERT_DEF_KEY)) {
      PertSourceEntry pse = (PertSourceEntry)obj;
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
    if (whichTable.equals(PERT_DEF_KEY)) {
      pmh_.selectTableRow(rtdps_, whichKey, this, PERT_DEF_KEY);
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
    if (key.equals(PERT_DEF_KEY)) {
      joinKeys_ = ((PertSourceModel)rtdps_.getModel()).getSelectedKeys(rtdps_.selectedRows);
      pendingKey_ = pspp_.setSourceDefsForMerge(joinKeys_);
      pspp_.startEditing();
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
    if (key.equals(PERT_DEF_KEY)) {
      String useKey = ((PertSourceModel)rtdps_.getModel()).getSelectedKey(rtdps_.selectedRows);
      pendingKey_ = null;
      pspp_.setDupSourceDef(useKey);
      pspp_.startEditing();
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
    if (key.equals(PERT_DEF_KEY)) {
      String filterKey = ((PertSourceModel)rtdps_.getModel()).getSelectedKey(rtdps_.selectedRows);
      PertFilter srcFilter = new PertFilter(PertFilter.Cat.SOURCE, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression srcExp = new PertFilterExpression(srcFilter);      
      /*
       * Can't do this, since two pert defs may have same source name & pert type!      
      PertSource ps = pd_.getSourceDef(filterKey);
      PertFilter srcFilter = new PertFilter(PertFilter.SOURCE_NAME, PertFilter.STR_EQUALS, ps.getSourceNameKey());
      PertFilterExpression pfes = new PertFilterExpression(srcFilter);
      PertFilter pertFilter = new PertFilter(PertFilter.PERT, PertFilter.STR_EQUALS, ps.getExpTypeKey());
      PertFilterExpression pfep = new PertFilterExpression(pertFilter);
      PertFilterExpression filtExp = new PertFilterExpression(PertFilterExpression.AND_OP, pfes, pfep); 
      */ 
      pfet_.jumpWithNewFilter(srcExp); 
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
    rtdps_.setEnabled(enable);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) { 
    
    List<String> selKeys = (rtdps_.selectedRows == null) ? null :
                               ((PertSourceModel)rtdps_.getModel()).getSelectedKeys(rtdps_.selectedRows);   
    rtdps_.rowElements.clear();
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> sdrefs = da.getAllSrcDefReferenceCounts(tcd_);   
    Integer noCount = new Integer(0);    
    
    Iterator<String> pskit = pd_.getSourceDefKeys();
    while (pskit.hasNext()) {
      String key = pskit.next();
      PertSource ps = pd_.getSourceDef(key);
      Integer count = sdrefs.get(key);
      if (count == null) {
        count = noCount;
      }        
      rtdps_.rowElements.add(new PertSourceEntry(ps, count));
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtdps_, this, PERT_DEF_KEY, selKeys);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(PERT_DEF_KEY)) {
      pendingKey_ = null;
      rtdps_.clearSelections(false);
      pspp_.setSource(pendingKey_);
      pspp_.startEditing();
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
    if (key.equals(PERT_DEF_KEY)) {
      pendingKey_ = ((PertSourceModel)rtdps_.getModel()).getSelectedKey(rtdps_.selectedRows);
      pspp_.setSource(pendingKey_);
      pspp_.startEditing();
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
    if (key.equals(PERT_DEF_KEY)) {
     String deleteKey = ((PertSourceModel)rtdps_.getModel()).getSelectedKey(rtdps_.selectedRows);   
     if (deleteSourceDef(deleteKey, dacx_)) {
       pet_.itemDeleted(PERT_DEF_KEY);
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
  ** Handle delete operations for source definitions
  */ 
  
  private boolean deleteSourceDef(String key, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getSourceDefReferenceSets(key, tcd_);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }       
    UndoSupport support = uFac_.provideUndoSupport("undo.deleteSourceDef", dacx);
    da.killOffDependencies(refs, tcd_,  support);
    PertDataChange pdc2 = pd_.deleteSourceDef(key);
    support.addEdit(new PertDataChangeCmd(pdc2));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  private boolean joinPertDefs(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();  
    PertSource psrc = pspp_.getResult();
    UndoSupport support = uFac_.provideUndoSupport("undo.mergeSourceDefs", dacx);  
    DependencyAnalyzer.Dependencies refs = da.getSourceDefMergeSet(new HashSet<String>(joinKeys_), pendingKey_, tcd_);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }
    Set<String> multiPertCollapse = da.getMultiSourceDefCollapseMergeSet(new HashSet<String>(joinKeys_));
    if (!multiPertCollapse.isEmpty()) {
      int doit = JOptionPane.showConfirmDialog(parent_, UiUtil.convertMessageToHtml(rMan_.getString("sdmp.multiPertCollapseOnJoin")),
                                               rMan_.getString("sdmp.multiPertCollapseOnJoinTitle"),
                                               JOptionPane.OK_CANCEL_OPTION);
      if (doit != JOptionPane.OK_OPTION) {
        return (false);
      }
    }
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergePertSourceDefs(joinKeys_, pendingKey_, psrc);
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
    return (true);          
  }   
  
  /***************************************************************************
  **
  ** Edit an perturbation def
  ** 
  */
  
  public void editAPertDef(String key, int what, DataAccessContext dacx) {
    PertSource psrc = pspp_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createPertSourceDef" 
                                                                         : "undo.editPertSourceDef", dacx);      
    if (pendingKey_ == null) { 
      pendingKey_ = psrc.getID();
    }
    
    PertDataChange pdc = pd_.setSourceDef(psrc);
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
  
  /***************************************************************************
  **
  ** Used for the perturb source table
  */

  private class PertSourceModel extends ReadOnlyTable.TableModel {

    final static int SRC_NAME   = 0;
    final static int PERT_TYPE  = 1; 
    final static int PROX_SIGN  = 2;
    final static int PROX_NAME  = 3;
    final static int ANNOTS     = 4;
    final static int COUNT      = 5;
    private final static int NUM_COL_ = 6;   
   
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    private static final long serialVersionUID = 1L;
   
    PertSourceModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"psdm.srcName",
                                "psdm.pertType",
                                "psdm.proxSign",
                                "psdm.proxName",
                                "psdm.annots",
                                "psdm.count"};
      colClasses_ = new Class[] {String.class,
                                 String.class,
                                 String.class,
                                 String.class,
                                 String.class,
                                 Integer.class};
      comparators_ = new Comparator[] {String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
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
        PertSourceEntry pse = (PertSourceEntry)iit.next();
        columns_[SRC_NAME].add(pse.srcName);
        columns_[PERT_TYPE].add(pse.pertType); 
        columns_[PROX_SIGN].add(pse.proxSignStr);
        columns_[PROX_NAME].add(pse.proxName);
        columns_[ANNOTS].add(pse.annots);
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
  
  /***************************************************************************
  ** 
  ** Used for the summary table
  */

  private class PertSourceEntry {

    String key;
    String srcName;
    String pertType;
    String proxSignStr;
    String proxName;
    String annots;
    Integer count;
    
    PertSourceEntry(PertSource ps, Integer count) {
      key = ps.getID();
      srcName = ps.getSourceName(pd_);    
      PertProperties currExp = ps.getExpType(pd_.getPertDictionary());
      pertType = currExp.getType();
      String proxSign = ps.getProxySign();
      boolean haveAProx = !proxSign.equals(PertSource.mapProxySignIndex(PertSource.NO_PROXY_INDEX));
      if (haveAProx) {
        ObjChoiceContent occP = PertSource.getProxySignValue(uics_, proxSign);
        proxSignStr = occP.name;
      } else {
        proxSignStr = "";
      } 
      proxName = (haveAProx) ? ps.getProxiedSpeciesName(pd_) : "";
      List<String> footKeys = ps.getAnnotationIDs();
      annots = pd_.getFootnoteListAsNVString(footKeys);
      this.count = count;
    }
  } 
}
