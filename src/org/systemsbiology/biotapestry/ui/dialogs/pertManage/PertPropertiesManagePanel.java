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
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;

/****************************************************************************
**
** Panel for managing setup of perturbation properties
*/

public class PertPropertiesManagePanel extends AnimatedSplitManagePanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "perProps";
  
  public final static String PERT_KEY = "editPert";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private ReadOnlyTable rtdp_;
  private PerturbationData pd_; 
  private PertPropsAddOrEditPanel ppaep_;
  private String pendingKey_;
  private List<String> joinKeys_;
  private PertManageHelper pmh_;
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
  
  public PertPropertiesManagePanel(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw,
                                   PerturbationData pd, PendingEditTracker pet, 
                                   PertFilterExpressionJumpTarget pfet) {
    super(uics, dacx, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    uFac_ = uFac;
    pfet_ = pfet;
    pmh_ = new PertManageHelper(uics_, dacx_, pmw, pd, rMan_, gbc_, pet_);
           
    rtdp_ = new ReadOnlyTable(uics_, dacx_, new PertPropsModel(uics_, dacx_), new ReadOnlyTable.EmptySelector());
  
    //
    // Build the perturbation properties panel:
    //
    
    JPanel ppPanel = buildPertPropertiesPanel(null);
    UiUtil.gbcSet(gbc_, 0, rowNum_++, 6, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(ppPanel, gbc_);      
      
    ppaep_ = new PertPropsAddOrEditPanel(uics_, dacx_, parent_, pd_, this, PERT_KEY);
    addEditPanel(ppaep_, PERT_KEY);
        
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
    if (key.equals(PERT_KEY)) {
      if (joinKeys_ == null) {
        editPertProp(key, what);
      } else {
        joinPertProps(key, what);
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
     rtdp_.makeCurrentSelectionVisible();
     return;
   } 
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    ppaep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    ppaep_.closeAction();
    return;
  }   
  
  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(PERT_KEY)) {
      PPropsEntry ppe = (PPropsEntry)obj;
      return (ppe.key.equals(key));
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
    if (whichTable.equals(PERT_KEY)) {
      pmh_.selectTableRow(rtdp_, whichKey, this, PERT_KEY); 
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
    if (key.equals(PERT_KEY)) {
      String useKey = ((PertPropsModel)rtdp_.getModel()).getSelectedKey(rtdp_.selectedRows); 
      pendingKey_ = null;
      PertDictionary pDict = pd_.getPertDictionary();
      HashSet<String> allTypes = new HashSet<String>();
      HashSet<String> allAbbrev = new HashSet<String>();
      HashSet<String> allLegAlt = new HashSet<String>();
      Iterator<String> kit = pDict.getKeys();
      while (kit.hasNext()) {
        String pkey = kit.next();
        PertProperties pp = pDict.getPerturbProps(pkey);
        String type = pp.getType();
        String abbrev = pp.getAbbrev();
        String legAlt = pp.getLegacyAlt();
        allTypes.add(type);
        if (abbrev != null) allAbbrev.add(abbrev);
        if (legAlt != null) allLegAlt.add(legAlt);
      } 
      PertProperties ppCurr = pDict.getPerturbProps(useKey);
      String copyType = pmh_.getNewUniqueCopyName(allTypes, ppCurr.getType());
      String currAbbrev = ppCurr.getAbbrev();
      String copyAbbrev = (currAbbrev == null) ? null : pmh_.getNewUniqueCopyName(allAbbrev, currAbbrev);
      String legAlt = ppCurr.getLegacyAlt();
      String copyLegAlt = (legAlt == null) ? null : pmh_.getNewUniqueCopyName(allLegAlt, legAlt);
      pendingKey_ = null;
      ppaep_.setPertPropsForDup(useKey, copyType, copyAbbrev, copyLegAlt);
      ppaep_.startEditing();
    } else {
      throw new IllegalArgumentException();
    }
    return;          
  }
  
  /***************************************************************************
  **
  ** Handle join operations 
  */ 
  
  public void doAJoin(String key) {
    if (key.equals(PERT_KEY)) { 
      joinKeys_ = ((PertPropsModel)rtdp_.getModel()).getSelectedKeys(rtdp_.selectedRows);    
      pendingKey_ = ppaep_.setPertPropsForMerge(joinKeys_);
      ppaep_.startEditing();
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
    if (key.equals(PERT_KEY)) {
      String filterKey = ((PertPropsModel)rtdp_.getModel()).getSelectedKey(rtdp_.selectedRows);    
      PertFilter pertFilter = new PertFilter(PertFilter.Cat.PERT, PertFilter.Match.STR_EQUALS, filterKey);
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
    rtdp_.setEnabled(enable);
    return;
  } 
   
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {  
    
    List<String> selKeys = (rtdp_.selectedRows == null) ? null :
                              ((PertPropsModel)rtdp_.getModel()).getSelectedKeys(rtdp_.selectedRows);
      
    rtdp_.rowElements.clear();
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map<String, Integer> pprefs = da.getAllPertPropReferenceCounts();   
    Integer noCount = new Integer(0);
       
    PertDictionary pDict = pd_.getPertDictionary();
    Iterator<String> etit = pDict.getKeys();
    while (etit.hasNext()) {
      String key = etit.next();
      PertProperties pp = pDict.getPerturbProps(key);
      Integer count = pprefs.get(key);
      if (count == null) {
        count = noCount;
      }   
      rtdp_.rowElements.add(new PPropsEntry(pp, count));
    }
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtdp_, this, PERT_KEY, selKeys);
    return;
  } 

  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(PERT_KEY)) {
      pendingKey_ = null;
      rtdp_.clearSelections(false);
      ppaep_.setPertProps(pendingKey_);
      ppaep_.startEditing();
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
    if (key.equals(PERT_KEY)) {
      pendingKey_ = ((PertPropsModel)rtdp_.getModel()).getSelectedKey(rtdp_.selectedRows);    
      ppaep_.setPertProps(pendingKey_);
      ppaep_.startEditing();
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
    if (key.equals(PERT_KEY)) {
      String selected = ((PertPropsModel)rtdp_.getModel()).getSelectedKey(rtdp_.selectedRows);
      if (deletePertProps(selected)) {
        pet_.itemDeleted(PERT_KEY);
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
  ** Edit a pert prop
  ** 
  */
  
  private void editPertProp(String key, int what) {
    PertProperties pp = ppaep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createPertProp" 
                                                                        : "undo.editPertProp", dacx_);
    PertDataChange pdc = pd_.setPerturbationProp(pp);
    String resultKey = pp.getID();
    pendingKey_ = null;
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
  ** Join pert props
  */ 
  
  private boolean joinPertProps(String key, int what) {  
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();  
    PertProperties pp = ppaep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport("undo.mergePertProps", dacx_);   
    DependencyAnalyzer.Dependencies refs = da.getPertPropMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }          
    da.mergeDependencies(refs, dacx_, support);
    PertDataChange[] pdc = pd_.mergePertProps(joinKeys_, pendingKey_, pp);
    support.addEdits(PertDataChangeCmd.wrapChanges(dacx_, pdc));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
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
      if (!pschk.getExpTypeKey().equals(resultKey)) {
        continue;
      }
      Iterator<String> sdkit2 = pd_.getSourceDefKeys();
      while (sdkit2.hasNext()) {
        String sdkey2 = sdkit2.next();
        if (sdkey.equals(sdkey2)) {
          continue;
        }   
        PertSource pschk2 = pd_.getSourceDef(sdkey2); 
        if (!pschk2.getExpTypeKey().equals(resultKey)) {
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
   
  /***************************************************************************
  **
  ** Build the perturbation properties panel 
  */ 
  
  private JPanel buildPertPropertiesPanel(List<ReadOnlyTable> allTabs) {
    
    ResourceManager rMan = dacx_.getRMan();
 
    //
    // Table:
    //
    
    JPanel display = new JPanel();
    display.setBorder(new EtchedBorder());
    display.setLayout(new GridBagLayout());
 
    rtdp_.setButtonHandler(new ButtonHand(PERT_KEY));
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    tp.multiTableSelectionSyncing = allTabs;
    tp.clearOthersOnSelect = true;
    tp.canMultiSelect = true;
    tp.tableTitle = rMan.getString("pertSetupManage.perturbationTypes");
    tp.titleFont = null;
    tp.colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertPropsModel.TYPE, 50, 100, 150));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertPropsModel.ABBREV, 50, 100, 150));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertPropsModel.SIGN, 50, 300, Integer.MAX_VALUE));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertPropsModel.NVPAIRS, 100, 200, Integer.MAX_VALUE));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PertPropsModel.COUNT, 50, 50, 100));
    addExtraButtons(tp, PERT_KEY);    
    JPanel tabPan = rtdp_.buildReadOnlyTable(tp);
    GridBagConstraints gbc = new GridBagConstraints();
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    display.add(tabPan, gbc);
    return (display);  
  }
  /***************************************************************************
  **
  ** 
  ** Pert prop deletion
  ** 
  */
  
  private boolean deletePertProps(String key) {
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    DependencyAnalyzer.Dependencies refs = da.getPertTypeReferenceSets(key);
    if (!pmh_.warnAndAsk(refs)) {
      return (false);
    }
       
    UndoSupport support = uFac_.provideUndoSupport("undo.deletePertProp", dacx_);  
    da.killOffDependencies(refs, dacx_, support);
    PertDataChange pdc2 = pd_.deletePerturbationProp(key);
    support.addEdit(new PertDataChangeCmd(dacx_, pdc2));
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
  ** Used for the perturbation properties table
  */

  private class PertPropsModel extends ReadOnlyTable.TableModel {

    final static int TYPE    = 0;
    final static int ABBREV  = 1; 
    final static int SIGN    = 2;
    final static int NVPAIRS = 3;
    final static int COUNT   = 4;
    private final static int NUM_COL_ = 5;
    
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    private static final long serialVersionUID = 1L;
    
    PertPropsModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {"pertProp.type",
                                "pertProp.abbrev",
                                "pertProp.sign",
                                "pertProp.nvPairs",
                                "pertProp.count"};
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
        PPropsEntry ppe = (PPropsEntry)iit.next();
        columns_[TYPE].add(ppe.type);
        columns_[ABBREV].add(ppe.expAbbrev);
        columns_[SIGN].add(ppe.sign);
        columns_[NVPAIRS].add(ppe.nvPairsStr);
        columns_[COUNT].add(ppe.count);
        hiddenColumns_[HIDDEN_NAME_ID_].add(ppe.key);
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

  private class PPropsEntry {

    String key;
    String type;
    String expAbbrev;
    String sign;
    String nvPairsStr;
    Integer count;
    
    PPropsEntry(PertProperties pp, Integer count) {
      key = pp.getID();
      type = pp.getType();
      if (type == null) type = "";
      expAbbrev = pp.getAbbrev();
      if (expAbbrev == null) expAbbrev = "";
      String legacyAltTag = pp.getLegacyAlt();
      if (legacyAltTag != null) {
        String format = rMan_.getString("ptmp.legacyAltFormat");
        expAbbrev = MessageFormat.format(format, new Object[] {expAbbrev, legacyAltTag}); 
      }
      sign = pp.getLinkSignRelationship().getDisplayTag(dacx_);
      
      Iterator<String> nvpkit = pp.getNvpKeys();
      if (nvpkit.hasNext()) {   
        StringBuffer buf = new StringBuffer();
        while (nvpkit.hasNext()) {
          String name = nvpkit.next();
          buf.append(name);
          buf.append("=");
          String value = pp.getValue(name);
          buf.append(value);
          if (nvpkit.hasNext()) {
            buf.append("; ");
          } 
        }
        nvPairsStr = buf.toString();
      } else {
        nvPairsStr = "";
      }
      this.count = count;
    }
  }
}
