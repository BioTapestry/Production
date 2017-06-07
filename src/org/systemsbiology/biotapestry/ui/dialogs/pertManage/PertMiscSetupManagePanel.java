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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Panel for managing setup of perturbation environment
*/

public class PertMiscSetupManagePanel extends AnimatedSplitManagePanel implements PertSimpleNameEditPanel.Client {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public final static String MANAGER_KEY = "setup"; 
  
  public final static String UDF_KEY = "editUDF";
  public final static String MEAS_SCALE_KEY = "editMeasScale";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable rtudf_;
  private ReadOnlyTable rtmt_;
  private PerturbationData pd_;
  private PertSimpleNameEditPanel udfep_;
  private PertMeasureScaleAddOrEditPanel mtep_;
  private String pendingKey_;
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
  
  public PertMiscSetupManagePanel(UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, PerturbationsManagementWindow pmw, 
                                  PerturbationData pd, PendingEditTracker pet, 
                                  PertFilterExpressionJumpTarget pfet) {
    super(uics, dacx, pmw, pet, MANAGER_KEY);
    pd_ = pd;
    uFac_ = uFac;
    pfet_ = pfet;
    pmh_ = new PertManageHelper(uics_, dacx_, pmw, pd, rMan_, gbc_, pet_);
 
    
    ArrayList<ReadOnlyTable> allTabs = new ArrayList<ReadOnlyTable>();
    rtudf_ = new ReadOnlyTable(uics_, dacx_, new ReadOnlyTable.NameWithHiddenIDModel(uics_, dacx_), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtudf_);
        
    rtmt_ = new ReadOnlyTable(uics_, dacx_, new MeasureScaleModel(uics_, dacx_), new ReadOnlyTable.EmptySelector());
    allTabs.add(rtmt_);
    
    //
    // Build the user-defined data fields panel:
    //
        
    JPanel ufPanel = commonTableBuild(rtudf_, "pmsmp.userFields", new ButtonHand(UDF_KEY), allTabs, null, null);
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(ufPanel, gbc_);  
    
    //
    // Build the measurement scales
    //
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();     
    colWidths.add(new ReadOnlyTable.ColumnWidths(MeasureScaleModel.TYPE, 100, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(MeasureScaleModel.UNCHANGED, 50, 50, 100));
    colWidths.add(new ReadOnlyTable.ColumnWidths(MeasureScaleModel.CONVERT_TYPE, 100, 250, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(MeasureScaleModel.ILLEGAL_RANGE, 50, 200, 300));
    colWidths.add(new ReadOnlyTable.ColumnWidths(MeasureScaleModel.COUNT, 50, 50, 100));
   
    JPanel mtPanel = commonTableBuild(rtmt_, "pmsmp.measure", new ButtonHand(MEAS_SCALE_KEY), allTabs, colWidths, MEAS_SCALE_KEY);
    UiUtil.gbcSet(gbc_, 1, rowNum_++, 1, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);    
    topPanel_.add(mtPanel, gbc_);  
    
 
    udfep_ = new PertSimpleNameEditPanel(uics_, dacx_, parent_, this, "pmsmp.userField", this, UDF_KEY);
    addEditPanel(udfep_, UDF_KEY);
       
    mtep_ = new PertMeasureScaleAddOrEditPanel(uics_, dacx_, parent_, pd_, this, MEAS_SCALE_KEY);
    addEditPanel(mtep_, MEAS_SCALE_KEY);
 
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
     if (whoAmI.equals(UDF_KEY)) {  
       return (pd_.getUserFieldName(Integer.parseInt(key)));
     } else {
       throw new IllegalArgumentException();
     }
   }
    
   public boolean haveDuplication(String whoAmI, String key, String name) {
     String existingKey;
     if (whoAmI.equals(UDF_KEY)) {
       existingKey = pd_.getUserFieldIndexFromName(name);     
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
    if (key.equals(UDF_KEY)) {
      if (joinKeys_ == null) {
        editUserField(key, what);
      } else {
        throw new IllegalStateException();
      }
    } else if (key.equals(MEAS_SCALE_KEY)) {
      if (joinKeys_ == null) {
        editMeasureScale(key, what);
      } else {
        joinMeasureScales(key, what);
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
     rtudf_.makeCurrentSelectionVisible();
     rtmt_.makeCurrentSelectionVisible();
     return;
   }
   
  /***************************************************************************
  **
  ** Handle a change
  */ 
  
  public void haveAChange(boolean mustDie) { 
    displayProperties(true);
    udfep_.hotUpdate(mustDie);
    mtep_.hotUpdate(mustDie);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop pending edits
  */ 
  
  public void dropPendingEdits() { 
    udfep_.closeAction();
    mtep_.closeAction();
    return;
  }   

  /***************************************************************************
  **
  ** For table row support
  */   
  
  public boolean tableRowMatches(String key, Object obj, String tableID) {
    if (tableID.equals(UDF_KEY)) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)obj;
      return (tocc.val.equals(key));
    } else if (tableID.equals(MEAS_SCALE_KEY)) {    
      MSMEntry twrc = (MSMEntry)obj;
      return (twrc.key.equals(key));
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
    if (whichTable.equals(UDF_KEY)) {
      pmh_.selectTableRow(rtudf_, whichKey, this, UDF_KEY);
    } else if (whichTable.equals(MEAS_SCALE_KEY)) {
      pmh_.selectTableRow(rtmt_, whichKey, this, MEAS_SCALE_KEY);
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
    if (key.equals(MEAS_SCALE_KEY)) {
      String useKey = ((MeasureScaleModel)rtmt_.getModel()).getSelectedKey(rtmt_.selectedRows); 
      pendingKey_ = null;
      mtep_.setMeasureScaleForDup(useKey); 
      mtep_.startEditing();    
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
    if (key.equals(MEAS_SCALE_KEY)) {
      joinKeys_ = ((MeasureScaleModel)rtmt_.getModel()).getSelectedKeys(rtmt_.selectedRows);
      MeasureDictionary md = pd_.getMeasureDictionary();
      Iterator<String> jkit = joinKeys_.iterator();
      MeasureScale.Conversion conv = null;
      boolean isFirst = true;
      boolean mismatch = false;
      while (jkit.hasNext()) {
        String ckey = jkit.next();
        MeasureScale chkScale = md.getMeasureScale(ckey);
        if (isFirst) {
          conv = chkScale.getConvToFold();
          isFirst = false;
        } else {
          if (conv == null) {
            if (chkScale.getConvToFold() != null) {
              mismatch = true;
              break;
            }
          } else if (!conv.equals(chkScale.getConvToFold())) {
            mismatch = true;
            break;
          }
        }       
      }
      if (mismatch) {
         JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.dangerousFoldMismatchMerge"),
                                       rMan_.getString("pmsmp.dangerousMergeTitle"), 
                                       JOptionPane.WARNING_MESSAGE);
      }
      pendingKey_ = mtep_.setMeasureScaleForMerge(joinKeys_);
      if (pendingKey_ == null) {
        joinKeys_ = null;
        return;
      }
      mtep_.startEditing();    
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
    if (key.equals(MEAS_SCALE_KEY)) {
      String filterKey = ((MeasureScaleModel)rtmt_.getModel()).getSelectedKey(rtmt_.selectedRows);
      PertFilter filter = new PertFilter(PertFilter.Cat.MEASURE_SCALE, PertFilter.Match.STR_EQUALS, filterKey);
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
    rtudf_.setEnabled(enable);
    rtmt_.setEnabled(enable);
    return;
  } 
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected void displayProperties(boolean fireChange) {
    
    List<String> selKeys = (rtudf_.selectedRows == null) ? null :
                              ((ReadOnlyTable.NameWithHiddenIDModel)rtudf_.getModel()).getSelectedKeys(rtudf_.selectedRows);
    
    
    List<TrueObjChoiceContent> ufnames = buildUserFieldList();
    rtudf_.rowElements.clear();
    rtudf_.rowElements.addAll(ufnames);
    
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtudf_, this, UDF_KEY, selKeys);
   
    //
    // Measurement scales
    //
    
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    rtmt_.rowElements.clear();
    Vector<TrueObjChoiceContent> mopts = pd_.getMeasureDictionary().getScaleOptions();
    Map<String, Integer> scaleRefs = da.getAllMeasureScaleReferenceCounts(false);
    Integer noCount = Integer.valueOf(0);
    Iterator<TrueObjChoiceContent> mopit = mopts.iterator();  
    while (mopit.hasNext()) {
      TrueObjChoiceContent tocc = mopit.next();
      Integer count = scaleRefs.get((String)tocc.val);
      if (count == null) {
        count = noCount;
      }
      MSMEntry msme = new MSMEntry((String)tocc.val, count);
      rtmt_.rowElements.add(msme);
    }
    
    selKeys = (rtmt_.selectedRows == null) ? null :
                ((MeasureScaleModel)rtmt_.getModel()).getSelectedKeys(rtmt_.selectedRows);
    //
    // Cannot specify the selection (except no selection or row 0) until the 
    // data is installed and the sort map is re-intialized
    
    pmh_.updateAndReselect(fireChange, rtmt_, this, MEAS_SCALE_KEY, selKeys);
    return;
  }  

  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected void doAnAdd(String key) {
    if (key.equals(UDF_KEY)) {
      pendingKey_ = null;
      rtudf_.clearSelections(false);
      rtmt_.clearSelections(false);
      udfep_.setEditName(pendingKey_); 
      udfep_.startEditing();
    } else if (key.equals(MEAS_SCALE_KEY)) {  
      pendingKey_ = null;
      rtudf_.clearSelections(false);
      rtmt_.clearSelections(false);
      mtep_.setEditMeasureScale(pendingKey_); 
      mtep_.startEditing();    
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
    if (key.equals(UDF_KEY)) {
      pendingKey_ = ((ReadOnlyTable.NameWithHiddenIDModel)rtudf_.getModel()).getSelectedKey(rtudf_.selectedRows);    
      udfep_.setEditName(pendingKey_);
      udfep_.startEditing();
    } else if (key.equals(MEAS_SCALE_KEY)) {  
      String selKey = ((MeasureScaleModel)rtmt_.getModel()).getSelectedKey(rtmt_.selectedRows);
      String[] stds = pd_.getMeasureDictionary().getStandardScaleKeys();
      for (int i = 0; i < stds.length; i++) {
        if (selKey.equals(stds[i])) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotEditDefaultScale"),
                                        rMan_.getString("pmsmp.cannotEditDefaultScaleTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      pendingKey_ = selKey;
      mtep_.setEditMeasureScale(pendingKey_); 
      mtep_.startEditing();    
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
    if (key.equals(UDF_KEY)) {
      String selKey = ((ReadOnlyTable.NameWithHiddenIDModel)rtudf_.getModel()).getSelectedKey(rtudf_.selectedRows);
      String delWarn = rMan_.getString("pertDelete.losingUserDataInDataPoints");   
      int doit = JOptionPane.showConfirmDialog(parent_, delWarn,
                                             rMan_.getString("pertDelete.losingUserDataInDataPointsTitle"),
                                             JOptionPane.OK_CANCEL_OPTION);
      if (doit != JOptionPane.OK_OPTION) {
        return;
      }
      UndoSupport support = uFac_.provideUndoSupport("undo.deleteUserDataField", dacx_);
      PertDataChange pdc = pd_.deleteUserFieldName(selKey);
      support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
      pet_.editSubmissionBegins();
      support.finish();
      pet_.editSubmissionEnds();
      pet_.itemDeleted(UDF_KEY);             
    } else if (key.equals(MEAS_SCALE_KEY)) {
      String selKey = ((MeasureScaleModel)rtmt_.getModel()).getSelectedKey(rtmt_.selectedRows);
      String[] stds = pd_.getMeasureDictionary().getStandardScaleKeys();
      for (int i = 0; i < stds.length; i++) {
        if (selKey.equals(stds[i])) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotDeleteDefaultScale"),
                                        rMan_.getString("pmsmp.cannotDeleteDefaultScaleTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      DependencyAnalyzer.Dependencies refs = da.getMeasScaleReferenceSet(selKey);
      if (!pmh_.warnAndAsk(refs)) {
        return;
      }      
      UndoSupport support = uFac_.provideUndoSupport("undo.deleteMeasScale", dacx_);
      da.killOffDependencies(refs, dacx_, support);
      PertDataChange pdc = pd_.deleteMeasureScale(selKey);
      support.addEdit(new PertDataChangeCmd(dacx_, pdc));
      dacx_.getDisplayOptsSource().modifyForPertDataChange(support, dacx_);
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
      pet_.editSubmissionBegins();
      support.finish();
      pet_.editSubmissionEnds();
      pet_.itemDeleted(MEAS_SCALE_KEY);             
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
  ** Join measurement scales
  */ 
  
  private void joinMeasureScales(String key, int what) {
    MeasureDictionary md = pd_.getMeasureDictionary();
    String[] stds = md.getStandardScaleKeys();
    String chkStd = null;
    for (int i = 0; i < stds.length; i++) {
      if (joinKeys_.contains(stds[i])) {
        if (chkStd != null) {
          throw new IllegalStateException();
        }
        chkStd = stds[i];
      }
    }
  
    MeasureScale revisedScale = mtep_.getResult();
    if (chkStd != null) {
      MeasureScale stdScale = md.getMeasureScale(chkStd);
      if (!revisedScale.equals(stdScale)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsmp.cannotChangeStd"),
                                      rMan_.getString("pmsmp.cannotChangeStdTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
 
    MeasureScale origScale = md.getMeasureScale(pendingKey_);
    MeasureScale.Conversion revisedFold = revisedScale.getConvToFold();
    MeasureScale.Conversion origFold = origScale.getConvToFold();
    
    boolean danger = false;
    if (revisedFold == null) {
      danger = (origFold != null);
    } else {
      danger = !revisedFold.equals(origFold);
    }  
    if (danger) {
      int ok = 
        JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.dangerousMergeForFold"),
                                      rMan_.getString("pmsmp.dangerousMergeForFoldTitle"), 
                                      JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return;
      }
    }
    
    BoundedDoubMinMax revisedIllegal = revisedScale.getIllegalRange();
    BoundedDoubMinMax origIllegal = origScale.getIllegalRange();
    
    boolean dangerIlleg = false;
    if (revisedIllegal != null) {
      dangerIlleg = !revisedIllegal.equals(origIllegal);
    }  
    if (dangerIlleg) {
      int ok = 
        JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.dangerousMergeForRange"),
                                      rMan_.getString("pmsmp.dangerousMergeForRangeTitle"), 
                                      JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return;
      }
    }
 
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    UndoSupport support = uFac_.provideUndoSupport("undo.mergeMeasureScales", dacx_);   
    DependencyAnalyzer.Dependencies refs = da.getMeasureScaleMergeSet(new HashSet<String>(joinKeys_), pendingKey_);
    da.mergeDependencies(refs, dacx_, support);
    PertDataChange[] pdc = pd_.mergeMeasureScales(joinKeys_, pendingKey_, revisedScale);
    support.addEdits(PertDataChangeCmd.wrapChanges(dacx_, pdc));
    dacx_.getDisplayOptsSource().modifyForPertDataChange(support, dacx_);
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
  ** Edit user field
  ** 
  */
  
  private void editUserField(String key, int what) {
    String name = udfep_.getResult();
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createUserFieldName" 
                                                                         : "undo.editUserFieldName", dacx_);      
    if (pendingKey_ == null) {
      pendingKey_ = Integer.toString(pd_.getUserFieldCount());
    } 
    PertDataChange pdc = pd_.setUserFieldName(pendingKey_, name);
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));    
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
  
  /***************************************************************************
  **
  ** Handle measure scale edit
  ** 
  */
  
  private void editMeasureScale(String key, int what) {
    MeasureScale revisedScale = mtep_.getResult();
    if (pendingKey_ != null) {
      MeasureScale origScale = pd_.getMeasureDictionary().getMeasureScale(pendingKey_);
      MeasureScale.Conversion revisedFold = revisedScale.getConvToFold();
      MeasureScale.Conversion origFold = origScale.getConvToFold();
    
      boolean danger = false;
      if (revisedFold == null) {
        danger = (origFold != null);
      } else {
        danger = !revisedFold.equals(origFold);
      }  
      if (danger) {
        int ok = 
          JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.dangerousFoldChange"),
                                        rMan_.getString("pmsmp.dangerousFoldChangeTitle"), 
                                        JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }
      BoundedDoubMinMax revisedIllegal = revisedScale.getIllegalRange();
      BoundedDoubMinMax origIllegal = origScale.getIllegalRange();

      boolean dangerIlleg = false;
  
      if (revisedIllegal != null) {
        dangerIlleg = !revisedIllegal.equals(origIllegal);
      }  
      if (dangerIlleg) {
        int ok = 
          JOptionPane.showConfirmDialog(parent_, rMan_.getString("pmsmp.dangerousRangeChange"),
                                        rMan_.getString("pmsmp.dangerousRangeChangeTitle"), 
                                        JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return;
        }
      }    
    }
 
    UndoSupport support = uFac_.provideUndoSupport((pendingKey_ == null) ? "undo.createMeasureScale" 
                                                                         : "undo.editMeasureScale", dacx_);    
    PertDataChange pdc = pd_.setMeasureScale(revisedScale);
    support.addEdit(new PertDataChangeCmd(dacx_, pdc));
    dacx_.getDisplayOptsSource().modifyForPertDataChange(support, dacx_);
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

  /***************************************************************************
  **
  ** Build the user field list
  ** 
  */
  
  private List<TrueObjChoiceContent> buildUserFieldList() {
    ArrayList<TrueObjChoiceContent> ufnames = new ArrayList<TrueObjChoiceContent>();
    Iterator ufit = pd_.getUserFieldNames();
    int count = 0;
    while (ufit.hasNext()) {
      TrueObjChoiceContent tocc = new TrueObjChoiceContent((String)ufit.next(), Integer.toString(count++));
      ufnames.add(tocc);    
    }
    return (ufnames);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the scale rows
  */

  private class MeasureScaleModel extends ReadOnlyTable.TableModel {

    final static int TYPE           = 0;
    final static int UNCHANGED      = 1; 
    final static int CONVERT_TYPE   = 2; 
    final static int ILLEGAL_RANGE  = 3; 
    final static int COUNT          = 4;
    private final static int NUM_COL_ = 5;   
   
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1; 
    
    private static final long serialVersionUID = 1L;
   
    MeasureScaleModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      colNames_ = new String[] {"pertMeaSc.type",
                                "pertMeaSc.unchanged",
                                "pertMeaSc.convertType",
                                "pertMeaSc.illegalRange",
                                "pertMeaSc.refCount"};
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
        MSMEntry msm = (MSMEntry)iit.next();
        columns_[TYPE].add(msm.type);
        columns_[UNCHANGED].add(msm.unchanged);
        columns_[CONVERT_TYPE].add(msm.convertType); 
        columns_[ILLEGAL_RANGE].add(msm.illegalRange); 
        columns_[COUNT].add(msm.count);
        hiddenColumns_[HIDDEN_NAME_ID_].add(msm.key);
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

  private class MSMEntry {

    String key;
    String type;
    String unchanged;
    String convertType;
    String illegalRange;
    Integer count;
    
    MSMEntry(String key, Integer count) {
      this.key = key;
      this.count = count;
      MeasureScale ms = pd_.getMeasureDictionary().getMeasureScale(key);
      type = ms.getDisplayString();
      Double unch = ms.getUnchanged();
      unchanged = (unch == null) ? "" : unch.toString();  
      MeasureScale.Conversion conv = ms.getConvToFold();
      convertType = (conv == null) ? "" : conv.getDisplayString(dacx_);
      BoundedDoubMinMax illegal = ms.getIllegalRange();
      illegalRange = (illegal == null) ? "" : illegal.toString();
    }
  }  
}
