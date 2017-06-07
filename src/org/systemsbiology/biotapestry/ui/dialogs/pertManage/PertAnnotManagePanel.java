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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.perturb.PertAnnotations;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing annotations
*/

public class PertAnnotManagePanel extends AnimatedSplitManagePanel {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "annotManager";
  
  public final static String ANNOT_KEY = "editAnnot";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtda_;
  private PerturbationData pd_;
  private PertAnnotEditPanel paep_;
  private String currKey_;
  private PertManageHelper pmh_;
  private List<String> joinKeys_;
  private PertFilterExpressionJumpTarget pfet_;
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
  
  public PertAnnotManagePanel(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw, 
                              PerturbationData pd, PendingEditTracker pet, 
                              PertFilterExpressionJumpTarget pfet) {
    super(uics, dacx, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    pfet_ = pfet;
    uFac_ = uFac;
    pmh_ = new PertManageHelper(uics, dacx, pmw, pd, rMan_, gbc_, pet_);

    //
    // Build the Annotation panel
    //
    
    JPanel annotPanel = buildAnnotPanel();
    UiUtil.gbcSet(gbc_, 2, rowNum_++, 4, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(annotPanel, gbc_);  
 
    paep_ = new PertAnnotEditPanel(uics, dacx, parent_, pd_, this, ANNOT_KEY);
    addEditPanel(paep_, ANNOT_KEY);
  
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
  
  public void editIsComplete(String key, int what) {
    if (key.equals(ANNOT_KEY)) {
      if (joinKeys_ == null) {
        editAnnot(key, what);
      } else {
        joinAnnots(key, what);
      }
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
    paep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    paep_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** Let us know if we are done sliding
  ** 
  */  
    
   public void finished() {
     rtda_.makeCurrentSelectionVisible();
     return;
   }
  
  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(ANNOT_KEY)) {
      AnnotEntry pse = (AnnotEntry)obj;
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
    if (whichTable.equals(ANNOT_KEY)) {
      pmh_.selectTableRow(rtda_, whichKey, this, ANNOT_KEY);
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
    rtda_.setEnabled(enable);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {
    List<String> selKeys = (rtda_.selectedRows == null) ? null :
                             ((AnnotModel)rtda_.getModel()).getSelectedKeys(rtda_.selectedRows);
    
    rtda_.rowElements.clear(); 
    PertAnnotations pa = pd_.getPertAnnotations();
    Map<String, Integer> refCounts = pd_.getDependencyAnalyzer().getAllAnnotReferenceCounts();
    Integer noCount = new Integer(0);
    SortedMap<String, String> n2k = pa.getFootTagToKeyMap();
    Iterator<String> n2kit = n2k.keySet().iterator();
    while (n2kit.hasNext()) {
      String tag = n2kit.next();
      String key = n2k.get(tag);
      String message = pa.getMessage(key);
      Integer refCount = refCounts.get(key);
      if (refCount == null) {
        refCount = noCount;
      }
      rtda_.rowElements.add(new AnnotEntry(key, tag, message, refCount));
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtda_, this, ANNOT_KEY, selKeys);
    return;
  } 
  
  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    currKey_ = null;
    rtda_.clearSelections(false);
    paep_.setEditAnnotation(currKey_);
    paep_.startEditing();
    return;
  }
  
  /***************************************************************************
  **
  ** Handle edit operations 
  */ 
  
  protected void doAnEdit(String key) {
    currKey_ = ((AnnotModel)rtda_.getModel()).getSelectedKey(rtda_.selectedRows);
    paep_.setEditAnnotation(currKey_);
    paep_.startEditing();
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  protected void doADelete(String key) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    String annotKey = ((AnnotModel)rtda_.getModel()).getSelectedKey(rtda_.selectedRows);
    DependencyAnalyzer.Dependencies refs = da.getAnnotReferenceSet(annotKey);
    if (!pmh_.warnAndAsk(refs)) {
      return;
    }
 
    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertAnnot", dacx_);
    da.killOffDependencies(refs, dacx_, support);    
    PertDataChange pdc = pd_.deleteAnnotation(annotKey);
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));    
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    pet_.itemDeleted(ANNOT_KEY);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle duplication operations 
  */ 
  
  public void doADuplication(String key) {
    currKey_ = null;
    String dupKey = ((AnnotModel)rtda_.getModel()).getSelectedKey(rtda_.selectedRows);
    TrueObjChoiceContent tocc = pd_.getPertAnnotations().getAvailableTagChoices().get(0);
    String dupText = pd_.getPertAnnotations().getMessage(dupKey);
    paep_.setDupAnnotation(tocc.name, dupText);
    paep_.startEditing();
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  @Override
  public void doAJoin(String key) {
    joinKeys_ = ((AnnotModel)rtda_.getModel()).getSelectedKeys(rtda_.selectedRows);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> refCounts = da.getAllAnnotReferenceCounts();
    currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys_);
    
    TreeSet<String> nameOptions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    TreeSet<String> tagOptions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    PertAnnotations pa = pd_.getPertAnnotations();
    int numk = joinKeys_.size();
    for (int i = 0; i < numk; i++) {
      String nextJk = joinKeys_.get(i);
      nameOptions.add(pa.getMessage(nextJk));
      tagOptions.add(pa.getTag(nextJk));
    }
    paep_.setMergeName(joinKeys_, currKey_, nameOptions, tagOptions);
    paep_.startEditing();
    return;
  }
  
  /***************************************************************************
  **
  ** Handle filter jumps
  */ 
  
  public void doAFilterJump(String key) {
    String filterKey = ((AnnotModel)rtda_.getModel()).getSelectedKey(rtda_.selectedRows);
    PertFilter filter = new PertFilter(PertFilter.Cat.ANNOTATION, PertFilter.Match.STR_EQUALS, filterKey);
    PertFilterExpression pfe = new PertFilterExpression(filter);
    pfet_.jumpWithNewFilter(pfe); 
    return;
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Join annotations
  */ 
  
  private void joinAnnots(String key, int what) {
    String msg = paep_.getMessageResult();
    String tag = paep_.getTagResult();
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertAnnot", dacx_);   
  
    DependencyAnalyzer.Dependencies refs = da.getAnnotMergeSet(new HashSet<String>(joinKeys_), currKey_);
    da.mergeDependencies(refs, dacx_, support);
    PertDataChange pdc = pd_.mergeAnnotations(joinKeys_, currKey_, tag, msg);
   
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    pet_.editSubmissionBegins();
    support.finish();
    pet_.editSubmissionEnds();
    String resultKey = currKey_;
    currKey_ = null;
    joinKeys_ = null;
    super.editIsComplete(key, what);
    setTableSelection(key, resultKey);
    return;          
  }
  
  /***************************************************************************
  **
  ** Edit the annotation
  ** 
  */
  
  public void editAnnot(String key, int what) {
    String resultKey;
    String msg = paep_.getMessageResult();
    String tag = paep_.getTagResult();

    UndoSupport support;
    PertDataChange pdc;

    if (currKey_ == null) {
      support = uFac_.provideUndoSupport("undo.createPertAnnot", dacx_);
      pdc = pd_.addAnnotation(tag, msg);
      resultKey = pdc.annotKey; // Kinda bogus...
    } else {
      support = uFac_.provideUndoSupport("undo.editPertAnnot", dacx_);
      pdc = pd_.editAnnotation(currKey_, tag, msg);
      resultKey = currKey_;
    }
    currKey_ = null;
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));    
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
  ** Build the annotations panel 
  */ 
  
  private JPanel buildAnnotPanel() {
    
    ResourceManager rMan = dacx_.getRMan();
 
    //
    // Table:
    //
    
    JPanel display = new JPanel();
    display.setBorder(new EtchedBorder());
    display.setLayout(new GridBagLayout());
    
    //
    // Build the tables:
    //
    
    rtda_ = new ReadOnlyTable(uics_, dacx_, new AnnotModel(uics_, dacx_), new ReadOnlyTable.EmptySelector());   
    rtda_.setButtonHandler(new ButtonHand(ANNOT_KEY));
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString("pertSetupManage.annotations");
    tp.canMultiSelect = true;
    tp.titleFont = null;
    tp.colWidths = new ArrayList();   
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotModel.TAG, 50, 100, 200));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotModel.MESSAGE, 100, 400, Integer.MAX_VALUE));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(AnnotModel.REFCOUNT, 50, 50, 100));
    addExtraButtons(tp, ANNOT_KEY);    
    JPanel tabPan = rtda_.buildReadOnlyTable(tp);
    GridBagConstraints gbc = new GridBagConstraints();
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    display.add(tabPan, gbc);
    tabPan.setMinimumSize(new Dimension(400, 100));
    return (display);  
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

  class AnnotModel extends ReadOnlyTable.TableModel {
    final static int TAG     = 0;
    final static int MESSAGE = 1;
    final static int REFCOUNT = 2;
    private final static int NUM_COL_ = 3;   
    
    private final static int HIDDEN_ID_   = 0;
    private final static int NUM_HIDDEN_  = 1;
    
    private static final long serialVersionUID = 1L;
 
    AnnotModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {"annotTable.tag",
                                "annotTable.message",
                                "annotTable.refCount"};
      colClasses_ = new Class[] {String.class,
                                 String.class,
                                 Integer.class};
      comparators_ = new Comparator[] {new ReadOnlyTable.NumStrComparator(),
                                       String.CASE_INSENSITIVE_ORDER,
                                       new ReadOnlyTable.IntegerComparator()};
      addHiddenColumns(NUM_HIDDEN_);
    }    
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_ID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        AnnotEntry ae = (AnnotEntry)iit.next();
        columns_[TAG].add(ae.tag);
        columns_[MESSAGE].add(ae.message); 
        columns_[REFCOUNT].add(ae.refCount); 
        hiddenColumns_[HIDDEN_ID_].add(ae.key);
      }
      return;
    }
    
    String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_ID_].get(mapSelectionIndex(selected[0])));
    }   
    
    public List<String> getSelectedKeys(int[] selected) {
      ArrayList<String> retval = new ArrayList<String>();
      for (int i = 0; i < selected.length; i++) {
        retval.add((String)hiddenColumns_[HIDDEN_ID_].get(mapSelectionIndex(selected[i])));
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

  private class AnnotEntry {

    String key;
    String tag;
    String message; 
    Integer refCount; 

    AnnotEntry(String key, String tag, String message, Integer refCount) {
      this.key = key;
      this.tag = tag;
      this.message = message;
      this.refCount = refCount;
    }
  }
}
