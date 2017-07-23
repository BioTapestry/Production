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
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing setup of sources and targets
*/

public class PertSrcsAndTargsManagePanel extends AnimatedSplitManagePanel implements PertSimpleNameEditPanel.Client {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "srcsAndTargs";
  
  public final static String SRC_KEY = "editSrc";
  public final static String TRG_KEY = "editTrg"; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtds_;
  private ReadOnlyTable rtdt_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private String pendingKey_;
  private String dupKey_;
  private PertSimpleNameEditPanel psp_;
  private PertTargetAddOrEditPanel ptp_;
  private PertManageHelper pmh_;
  private List<String> joinKeys_;
  private PertFilterExpressionJumpTarget pfet_;
  private UndoFactory uFac_;
  private PerturbationDataMaps pdms_;
  private DBGenome dbGenome_;
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
  
  public PertSrcsAndTargsManagePanel(UIComponentSource uics, FontManager fMgr, TimeAxisDefinition tad, DataAccessContext dacx, PerturbationsManagementWindow pmw, 
                                     PerturbationData pd, TimeCourseData tcd, PerturbationDataMaps pdms, DBGenome dbGenome,
                                     PendingEditTracker pet, PertFilterExpressionJumpTarget pfet, UndoFactory uFac) {
    super(uics, fMgr, tad, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    pfet_ = pfet;
    uFac_ = uFac;
    tcd_ = tcd;
    pdms_ = pdms;
    dacx_ = dacx;
    dbGenome_ = dbGenome;
    pmh_ = new PertManageHelper(uics_, pmw, pd_, tcd_, rMan_, gbc_, pet_);

    ArrayList<ReadOnlyTable> allTabs = new ArrayList<ReadOnlyTable>();
    rtds_ = new ReadOnlyTable(uics_, new PertManageHelper.NameWithHiddenIDAndRefCountModel(uics_, "pstm.sourceName"), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtds_);
    rtdt_ = new ReadOnlyTable(uics_,new AnnotatedTargModel(uics_), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtdt_);
    
    //
    // Build the sources panel:
    //
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.NAME, 100, 150, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(PertManageHelper.NameWithHiddenIDAndRefCountModel.REF_COUNT, 50, 50, 100));

    JPanel srcsPanel = commonTableBuild(rtds_, "pertSrcTargManage.sources", new ButtonHand(SRC_KEY), allTabs, colWidths, SRC_KEY);
    
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(srcsPanel, gbc_);  
     
    //
    // Build the target names panel:
    //
    
    colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotatedTargModel.TRG_NAME, 100, 100, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotatedTargModel.ANNOTS, 50, 100, 100));
    colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotatedTargModel.COUNT, 50, 50, 100));

    JPanel targsPanel = commonTableBuild(rtdt_, "pertSrcTargManage.targets", 
                                         new ButtonHand(TRG_KEY), allTabs, colWidths, TRG_KEY);
    
    UiUtil.gbcSet(gbc_, 1, rowNum_, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(targsPanel, gbc_);  
 
    //
    // Edit panels:
    //
         
    psp_ = new PertSimpleNameEditPanel(uics_, parent_, this, "pertSrcTargEdit.sourceName", this, SRC_KEY);
    addEditPanel(psp_, SRC_KEY);
    
    ptp_ = new PertTargetAddOrEditPanel(uics_, parent_, pd_, tcd_, this, this, TRG_KEY);
    addEditPanel(ptp_, TRG_KEY);
 
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
     if (whoAmI.equals(SRC_KEY)) {
       return (pd_.getSourceName(key));     
     } else if (whoAmI.equals(TRG_KEY)) {
       return (pd_.getTarget(key));     
     } else {
       throw new IllegalArgumentException();
     }
   }
    
   public boolean haveDuplication(String whoAmI, String key, String name) {
     String existingKey;
     if (whoAmI.equals(SRC_KEY)) {
       existingKey = pd_.getSourceKeyFromName(name);
     } else if (whoAmI.equals(TRG_KEY)) {
       existingKey = pd_.getTargKeyFromName(name);     
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
     if (key == null) {
       return (false);
     }
     ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
     pdmsl.add(pdms_);
     
     
     if (whoAmI.equals(SRC_KEY)) {
       String currName = pd_.getSourceName(key);
       if (!DataUtil.keysEqual(currName, name)) {
         return (pdms_.dataSourceOnlyInverseIsDefault(dbGenome_, key, pd_.getSourceNames()));
       }        
     } else if (whoAmI.equals(TRG_KEY)) {
       String currName = pd_.getTarget(key);     
       if (!DataUtil.keysEqual(currName, name)) {
         return (pdms_.dataEntryOnlyInverseIsDefault(dbGenome_, key, pd_.getEntryNames()));
       }
     } else {
       throw new IllegalArgumentException();
     }     
     return (false);
   }

  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  @Override  
  public void editIsComplete(String key, int what) {
    if (key.equals(SRC_KEY)) {
      if (joinKeys_ == null) {
        editASource(key, what, dacx_);
      } else {
        joinSources(key, what, dacx_);
      }
    } else if (key.equals(TRG_KEY)) {
      if (joinKeys_ == null) {
        editATarget(key, what, dacx_);
      } else {
        joinTargets(key, what, dacx_);
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
     rtds_.makeCurrentSelectionVisible();
     rtdt_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    psp_.hotUpdate(mustDie);
    ptp_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    psp_.closeAction();
    ptp_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(SRC_KEY)) {
      PertManageHelper.ToccWithRefCount twrc = (PertManageHelper.ToccWithRefCount)obj;
      return (twrc.tocc.val.equals(key));
    } else if (tableID.equals(TRG_KEY)) {
      ATMEntry atme = (ATMEntry)obj;
      return (atme.key.equals(key));
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
    if (whichTable.equals(SRC_KEY)) {
      pmh_.selectTableRow(rtds_, whichKey, this, SRC_KEY); 
    } else if (whichTable.equals(TRG_KEY)) {
      pmh_.selectTableRow(rtdt_, whichKey, this, TRG_KEY); 
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
    if (key.equals(SRC_KEY)) {
      String useKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKey(rtds_.selectedRows);
      String copyName = pmh_.getUnique(pd_.getSourceNameOptions(), pd_.getSourceName(useKey));
      dupKey_ = useKey;
      pendingKey_ = null;
      psp_.setDupName(copyName);
      psp_.startEditing();
    } else if (key.equals(TRG_KEY)) {
      String useKey = ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKey(rtdt_.selectedRows);
      String copyName = pmh_.getUnique(pd_.getTargetOptions(false), pd_.getTarget(useKey));
      dupKey_ = useKey;
      pendingKey_ = null;
      ptp_.setDupTarget(useKey, copyName);
      ptp_.startEditing();
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
    if (key.equals(SRC_KEY)) {
      joinKeys_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKeys(rtds_.selectedRows);
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      Map<String, Integer> refCounts = da.getAllSourceNameReferenceCounts(true, tcd_);
      pendingKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_); 
      TreeSet<String> nameOptions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      int numk = joinKeys_.size();
      for (int i = 0; i < numk; i++) {
        String nextJk = joinKeys_.get(i);
        nameOptions.add(pd_.getSourceName(nextJk));
      }
      psp_.setMergeName(pendingKey_, nameOptions, new ArrayList<String>(joinKeys_));
      psp_.startEditing();
    } else if (key.equals(TRG_KEY)) {
      joinKeys_ = ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKeys(rtdt_.selectedRows);
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      Map<String, Integer> refCounts = da.getAllTargetReferenceCounts();
      pendingKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_); 
      TreeSet<String> nameOptions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      int numk = joinKeys_.size();
      for (int i = 0; i < numk; i++) {
        String nextJk = joinKeys_.get(i);
        nameOptions.add(pd_.getTarget(nextJk));
      }     
      ptp_.setMergeTarget(pendingKey_, new ArrayList<String>(joinKeys_), nameOptions);
      ptp_.startEditing();
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
    if (key.equals(SRC_KEY)) {
      String filterKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKey(rtds_.selectedRows);   
      PertFilter srcFilter = new PertFilter(PertFilter.Cat.SOURCE_NAME, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(srcFilter);
      pfet_.jumpWithNewFilter(pfe); 
    } else if (key.equals(TRG_KEY)) {
      String filterKey = ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKey(rtdt_.selectedRows);
      PertFilter trgFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, filterKey);
      PertFilterExpression pfe = new PertFilterExpression(trgFilter);
      pfet_.jumpWithNewFilter(pfe); 
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
    rtds_.setEnabled(enable);
    rtdt_.setEnabled(enable);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) { 
    
    List<String> selKeys = (rtds_.selectedRows == null) ? null :
                              ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKeys(rtds_.selectedRows);   
    rtds_.rowElements.clear();
    Vector<TrueObjChoiceContent> sno = pd_.getSourceNameOptions();
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> sdrefs = da.getAllSourceNameReferenceCounts(false, tcd_);
    Integer noCount = new Integer(0);
    Iterator<TrueObjChoiceContent> snit = sno.iterator();  
    while (snit.hasNext()) {
      TrueObjChoiceContent tocc = snit.next();
      Integer count = sdrefs.get(tocc.val);
      if (count == null) {
        count = noCount;
      }
      PertManageHelper.ToccWithRefCount twrc = 
        new PertManageHelper.ToccWithRefCount(tocc, count.intValue());
      rtds_.rowElements.add(twrc);
    }
       //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtds_, this, SRC_KEY, selKeys);
    
    selKeys = (rtdt_.selectedRows == null) ? null :
                ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKeys(rtdt_.selectedRows); 
    rtdt_.rowElements.clear();
    Vector<TrueObjChoiceContent> ato = pd_.getTargetOptions(false);
    Map<String, Integer> trefs = da.getAllTargetReferenceCounts();
    Iterator<TrueObjChoiceContent> atoit = ato.iterator();
    while (atoit.hasNext()) {
      TrueObjChoiceContent tocc = atoit.next();
      Integer count = trefs.get((String)tocc.val);
      if (count == null) {
        count = noCount;
      }
      rtdt_.rowElements.add(new ATMEntry((String)tocc.val, tocc.name, count));
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtdt_, this, TRG_KEY, selKeys);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(SRC_KEY)) {
      pendingKey_ = null;
      rtds_.clearSelections(false);
      rtdt_.clearSelections(false);
      psp_.setEditName(pendingKey_); 
      psp_.startEditing();
    } else if (key.equals(TRG_KEY)) {
      pendingKey_ = null;
      rtds_.clearSelections(false);
      rtdt_.clearSelections(false);
      ptp_.setTarget(pendingKey_); 
      ptp_.startEditing();
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
    if (key.equals(SRC_KEY)) {
      pendingKey_ = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKey(rtds_.selectedRows);    
      psp_.setEditName(pendingKey_);
      psp_.startEditing();
    } else if (key.equals(TRG_KEY)) {     
      pendingKey_ = ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKey(rtdt_.selectedRows);
      ptp_.setTarget(pendingKey_);
      ptp_.startEditing();
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
    if (key.equals(SRC_KEY)) {
      String deleteKey = ((PertManageHelper.NameWithHiddenIDAndRefCountModel)rtds_.getModel()).getSelectedKey(rtds_.selectedRows);
      if (deleteASourceName(deleteKey, dacx_)) {       
        pet_.itemDeleted(SRC_KEY);
      }
    } else if (key.equals(TRG_KEY)) {
      String deleteKey = ((AnnotatedTargModel)rtdt_.getModel()).getSelectedKey(rtdt_.selectedRows);
      if (deleteATarget(deleteKey, dacx_)) {       
        pet_.itemDeleted(TRG_KEY);
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
  
  private boolean deleteATarget(String key, DataAccessContext dacx) {    
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getTargetReferenceSet(key);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }
       
    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertTarget", dacx);  
    da.killOffDependencies(refs, tcd_, support);
    ArrayList<PerturbationDataMaps> pdatMaps = new ArrayList<PerturbationDataMaps>();
    pdatMaps.add(pdms_);
    PertDataChange[] pdc = pd_.deleteTargetName(key, pdatMaps);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  private boolean deleteASourceName(String key, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getSourceNameReferenceSets(key, tcd_);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }
      
    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertSourceName", dacx);
    da.killOffDependencies(refs, tcd_, support);
    ArrayList<PerturbationDataMaps> pdms = new ArrayList<PerturbationDataMaps>();
    pdms.add(pdms_);
    PertDataChange[] pdc = pd_.deleteSourceName(key, pdms);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
    support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Edit a target
  ** 
  */
  
  private void editATarget(String key, int what, DataAccessContext dacx) {
    String name = ptp_.getTargetName();
    int mode = ptp_.getMode();
    String action;
    boolean dupMap = false;
    ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
    pdmsl.add(pdms_);
    
    if (mode == PertTargetAddOrEditPanel.DUP_MODE) {
      if (pdms_.haveTargetNameMapTo(dupKey_)) {
        int doit = JOptionPane.showConfirmDialog(parent_, rMan_.getString("pstmp.wantToDupTrgMap"),
                                                 rMan_.getString("pstmp.wantToDupTrgMapTitle"),
                                                 JOptionPane.YES_NO_OPTION);
        dupMap = (doit == JOptionPane.YES_OPTION);
      }
      pendingKey_ = pd_.getNextDataKey(); 
      action = "undo.dupPertTargetName";
    } else {
      action = (pendingKey_ == null) ? "undo.createPertTargetName" : "undo.editPertTargetName";
      if (pendingKey_ == null) {
        pendingKey_ = pd_.getNextDataKey();
      }
    }
    
    UndoSupport support = uFac_.provideUndoSupport(action, dacx);      
  
    List annots = ptp_.getTargetAnnots();
    PertDataChange[] pdc = pd_.setTargetName(pendingKey_, name, annots, pdmsl, dbGenome_);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
    
    if (dupMap) {
      PertDataChange[] pdctnm = pdms_.duplicateTargetNameMap(dupKey_, pendingKey_);
      support.addEdits(PertDataChangeCmd.wrapChanges(pdctnm));
      dupKey_ = null;
    }
 
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
  ** Edit a source name
  ** 
  */
  
  private void editASource(String key, int what, DataAccessContext dacx) {
    String name = psp_.getResult();
    int mode = psp_.getMode();
    String action;
    boolean dupMap = false;
    ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
    pdmsl.add(pdms_);
    
    
    if (mode == PertSimpleNameEditPanel.DUP_MODE) {
      if (pdms_.haveSourceNameMapTo(dupKey_)) {
        int doit = JOptionPane.showConfirmDialog(parent_, rMan_.getString("pstmp.wantToDupSrcMap"),
                                                 rMan_.getString("pstmp.wantToDupSrcMapTitle"),
                                                 JOptionPane.YES_NO_OPTION);
        dupMap = (doit == JOptionPane.YES_OPTION);
      }      
      pendingKey_ = pd_.getNextDataKey(); 
      action = "undo.dupPertSourceName";
    } else if (mode == PertSimpleNameEditPanel.EDIT_MODE) {
      action = (pendingKey_ == null) ? "undo.createPertSourceName" : "undo.editPertSourceName";  
      if (pendingKey_ == null) {
        pendingKey_ = pd_.getNextDataKey();
      } 
    } else {
      throw new IllegalStateException();
    }
    
    UndoSupport support = uFac_.provideUndoSupport(action, dacx);
    PertDataChange[] pdc = pd_.setSourceName(pendingKey_, name, pdmsl, dbGenome_);
    support.addEdits(PertDataChangeCmd.wrapChanges(pdc));
   
    if (dupMap) {
      PertDataChange[] pdcsnm = pdms_.duplicateSourceNameMap(dupKey_, pendingKey_);
      support.addEdits(PertDataChangeCmd.wrapChanges(pdcsnm));
      dupKey_ = null;
    }
          
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
  ** Join targets
  */ 
  
  private void joinTargets(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String name = ptp_.getTargetName();
    List annots = ptp_.getTargetAnnots();
    ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
    pdmsl.add(pdms_);
       
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertTargets", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getTargetMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeTargetNames(joinKeys_, pendingKey_, name, annots, pdmsl, dbGenome_);
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
  ** Handle join operations 
  */ 
  
  private boolean joinSources(String key, int what, DataAccessContext dacx) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String name = psp_.getResult();
    ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
   
    pdmsl.add(pdms_);
    
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertSourceNames", dacx);   
    DependencyAnalyzer.Dependencies refs = da.getSourceNameMergeSet(new HashSet<String>(joinKeys_), pendingKey_, tcd_);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }     
    da.mergeDependencies(refs, tcd_, support);
    PertDataChange[] pdc = pd_.mergeSourceNames(joinKeys_, pendingKey_, name, pdmsl, dbGenome_);
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
    Iterator<String> sdkit = pd_.getSourceDefKeys();
    while (sdkit.hasNext()) {
      String sdkey = sdkit.next();
      PertSource pschk =  pd_.getSourceDef(sdkey);
      if (!pschk.getSourceNameKey().equals(resultKey)) {
        continue;
      }
      Iterator<String> sdkit2 = pd_.getSourceDefKeys();
      while (sdkit2.hasNext()) {
        String sdkey2 = sdkit2.next();
        if (sdkey.equals(sdkey2)) {
          continue;
        }   
        PertSource pschk2 = pd_.getSourceDef(sdkey2); 
        if (!pschk2.getSourceNameKey().equals(resultKey)) {
          continue;
        }
        if (pschk.compareSrcAndType(pschk2) == 0) {  // just compares source/pert type
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pstmp.mergedSourceDefs"),
                                        rMan_.getString("pstmp.mergedSourceDefsTitle"), 
                                        JOptionPane.WARNING_MESSAGE);
          return (true);
        }
      }
    } 
    return (true);          
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the annotated targets
  */

  private class AnnotatedTargModel extends ReadOnlyTable.TableModel {

    final static int TRG_NAME = 0;
    final static int ANNOTS   = 1; 
    final static int COUNT    = 2;
    private final static int NUM_COL_ = 3;   
   
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    private static final long serialVersionUID = 1L;
   
    AnnotatedTargModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"pertSrc.targName",
                                "pertSrc.annots",
                                "pertSrc.refCounts"};
      colClasses_ = new Class[] {String.class,
                                 String.class,
                                 Integer.class};
      comparators_ = new Comparator[] {String.CASE_INSENSITIVE_ORDER,
                                       String.CASE_INSENSITIVE_ORDER,
                                       new ReadOnlyTable.IntegerComparator()};
      
      addHiddenColumns(NUM_HIDDEN_);
    }    
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_NAME_ID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        ATMEntry atm = (ATMEntry)iit.next();
        columns_[TRG_NAME].add(atm.trgName);
        columns_[ANNOTS].add(atm.annots); 
        columns_[COUNT].add(atm.count);
        hiddenColumns_[HIDDEN_NAME_ID_].add(atm.key);
      }
      return;
    }
    
    public String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[0])));
    }
    
    public List<String> getSelectedKeys(int[] selected) {
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

  private class ATMEntry {

    String key;
    String trgName;
    String annots;
    Integer count;
    
    ATMEntry(String key, String name, Integer count) {
      this.key = key;
      trgName = name;
      this.count = count;
      List anList = pd_.getFootnotesForTarget(key);
      annots = ((anList == null) || anList.isEmpty()) ? "" : pd_.getFootnoteListAsString(anList);
    }
  } 
}
