/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.dialogs;


import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.HashSet;
import java.util.Set;
import java.text.MessageFormat;
import java.util.List;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.GeneTemplateEntry;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;


/****************************************************************************
**
** Dialog box for editing QPCR Footnotes
*/

public class TimeCourseSetupDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private EditableTable est_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private boolean namedStages_;
  private TimeAxisDefinition tad_;
  private TimeCourseData tcData_;
  private String unitHeading_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Wrapper on constructor.  Have to define time axis before we can define
  ** time course structure.
  */ 
  
  public static TimeCourseSetupDialog timeSourceSetupDialogWrapper(BTState appState, DataAccessContext dacx) {
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(appState, dacx);
      tasd.setVisible(true);
    }
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = appState.getRMan();
      JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                    rMan.getString("tcsedit.noTimeDefinition"), 
                                    rMan.getString("tcsedit.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }
    
    TimeCourseSetupDialog tcsd = new TimeCourseSetupDialog(appState, dacx);
    return (tcsd);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private TimeCourseSetupDialog(BTState appState, DataAccessContext dacx) {     
    super(appState.getTopFrame(), appState.getRMan().getString("tcsedit.title"), true);
    appState_ = appState;
    dacx_ = dacx;
  
    ResourceManager rMan = appState.getRMan();
    setSize(500, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    //
    // Figure out if we accept numbers or names for the stages:
    //
    
    tcData_ = dacx_.getExpDataSrc().getTimeCourseData();
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages();
    String displayUnits = tad_.unitDisplayString();
    unitHeading_ = MessageFormat.format(rMan.getString("tcsedit.timeUnitFormat"), new Object[] {displayUnits});      
   
    est_ = new EditableTable(appState_, new TimeCourseSetupTableModel(appState_), appState_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = false;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = null;
    JPanel tablePan = est_.buildEditableTable(etp);        
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
    
    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallButtonBox(cp, 10, 10, true, false);   
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    applyProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (applyProperties()) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    setVisible(false);
    dispose();
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class TimeCourseSetupTableModel extends EditableTable.TableModel {
    
    private final static int TIME_    = 0;
    private final static int REGION_  = 1;
    private final static int NUM_COL_ = 2;
    
    private final static int ORIG_INDEX_ = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private static final long serialVersionUID = 1L;
   
    class TableRow {
      Object time;
      String region;
      Integer origIndex;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        time = columns_[TIME_].get(i);
        region = (String)columns_[REGION_].get(i);
        origIndex = (Integer)hiddenColumns_[ORIG_INDEX_].get(i);
      }
      
      void toCols() {
        columns_[TIME_].add(time);  
        columns_[REGION_].add(region);
        hiddenColumns_[ORIG_INDEX_].add(origIndex);
        return;
      }
    }
  
    TimeCourseSetupTableModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {unitHeading_,
                                "tcsedit.region"};
      Class firstClass = (namedStages_) ? String.class : ProtoInteger.class;
      colClasses_ = new Class[] {firstClass,
                                 String.class};
      canEdit_ = new boolean[] {true,
                                true};
      addHiddenColumns(NUM_HIDDEN_);
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
  
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    List tableList = buildTableList();
    ((TimeCourseSetupTableModel)est_.getModel()).extractValues(tableList);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private List buildTableList() {
    ArrayList retval = new ArrayList();
    TimeCourseSetupTableModel tcs = (TimeCourseSetupTableModel)est_.getModel();  
 
    int count = 0;
    Iterator gtit = tcData_.getGeneTemplate();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = (GeneTemplateEntry)gtit.next();
      TimeCourseSetupTableModel.TableRow tr = tcs.new TableRow();    
      if (namedStages_) {
        TimeAxisDefinition.NamedStage stageName = tad_.getNamedStageForIndex(gte.time);
        tr.time = stageName.name;
      } else {          
        tr.time = new ProtoInteger(gte.time);                  
      }
      tr.region = gte.region;
      tr.origIndex = new Integer(count++);
      retval.add(tr);
    }
    return (retval); 
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the data
  ** 
  */
  
  private boolean applyProperties() {
 
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    
    //
    // Figure out the existing set of regions names:
    //
    
    HashSet origRegions = new HashSet();
    Iterator gtit = tcData_.getGeneTemplate();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = (GeneTemplateEntry)gtit.next();
      origRegions.add(gte.region);
    }
 
    //
    // Make sure the integers/stages are OK, and increasing:
    //

    ResourceManager rMan = appState_.getRMan();         

    int lastNum = 0;     
    TimeCourseSetupTableModel tcs = (TimeCourseSetupTableModel)est_.getModel(); 
    List vals = tcs.getValuesFromTable();
    int size = vals.size();

    for (int i = 0; i < size; i++) {
      TimeCourseSetupTableModel.TableRow tr = (TimeCourseSetupTableModel.TableRow)vals.get(i);    
      int thisNum;
      if (namedStages_) {
        String stageName = (String)tr.time;
        thisNum = tad_.getIndexForNamedStage(stageName);
        if (thisNum == TimeAxisDefinition.INVALID_STAGE_NAME) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcsedit.badStageName"),
                                        rMan.getString("tcsedit.badStageNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      } else {      
        ProtoInteger num = (ProtoInteger)tr.time;
        if ((num == null) || (!num.valid)) {
          IntegerEditor.triggerWarning(appState_, appState_.getTopFrame());
          return (false);
        }
        thisNum = num.value;  
      }

      if (thisNum < lastNum) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcsedit.notIncreasing"),
                                      rMan.getString("tcsedit.notIncreasingTitle"),
                                      JOptionPane.ERROR_MESSAGE);          
        return (false); 
      }
      lastNum = thisNum;       
    }

    //
    // Make sure the regions are OK:
    //

    for (int i = 0; i < size; i++) {
      TimeCourseSetupTableModel.TableRow tr = (TimeCourseSetupTableModel.TableRow)vals.get(i); 
      String region = tr.region;        
      if ((region == null) || (region.trim().equals(""))) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcsedit.badRegion"),
                                      rMan.getString("tcsedit.badRegionTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }

    //
    // Regions must be contiguous in time.  I.e. once it shows up, it needs
    // to stick around through all times until it disappears.  Create a set
    // of regions for each time:
    //

    SortedMap regionSets = new TreeMap();
    for (int i = 0; i < size; i++) {
      TimeCourseSetupTableModel.TableRow tr = (TimeCourseSetupTableModel.TableRow)vals.get(i); 
      String region = tr.region;
      Integer time;
      if (namedStages_) {
        String stageName = (String)tr.time;
        time = new Integer(tad_.getIndexForNamedStage(stageName));
      } else {      
        ProtoInteger num = (ProtoInteger)tr.time;
        time = new Integer(num.value);
      }
      Set setForTime = (Set)regionSets.get(time);
      if (setForTime == null) {
        setForTime = new HashSet();
        regionSets.put(time, setForTime);
      }
      if (DataUtil.containsKey(setForTime, region)) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcsedit.dupRegionForTime"),
                                      rMan.getString("tcsedit.dupRegionForTimeTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      setForTime.add(DataUtil.normKey(region));
    }

    //
    // Go through the sets.  If we see a new region in a set, we had better
    // not have seen it before; that means a non-contiguous region.
    //

    HashSet seen = new HashSet();
    Set lastSet = null;
    Iterator rskit = regionSets.keySet().iterator();
    while (rskit.hasNext()) {
      Integer time = (Integer)rskit.next();
      Set setForTime = (Set)regionSets.get(time);
      if (lastSet != null) {
        Set newRegions = new HashSet(setForTime);
        newRegions.removeAll(lastSet);  // result is new regions
        Iterator nrit = newRegions.iterator();
        while (nrit.hasNext()) {
          String region = (String)nrit.next();
          if (seen.contains(region)) {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcsedit.notContig"),
                                          rMan.getString("tcsedit.notContigTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
        }
      }
      seen.addAll(setForTime);        
      lastSet = setForTime;
    }

    //
    // Let user confirm that massive data deletions may occur:
    //

    if (!tcData_.isEmpty()) {
      int ok = 
        JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                      rMan.getString("tcsedit.ConfirmChange"), 
                                      rMan.getString("tcsedit.ConfirmChangeTitle"),
                                      JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }

    //
    // Build new template
    //

    ArrayList newGeneTemplate = new ArrayList();
    for (int i = 0; i < size; i++) {
      TimeCourseSetupTableModel.TableRow tr = (TimeCourseSetupTableModel.TableRow)vals.get(i); 
      int timeVal;
      if (namedStages_) {
        String stageName = (String)tr.time;
        timeVal = tad_.getIndexForNamedStage(stageName);
      } else {      
        ProtoInteger num = (ProtoInteger)tr.time;
        timeVal = num.value;
      }
      String region = tr.region;        
      GeneTemplateEntry newEntry = new GeneTemplateEntry(timeVal, region.trim());
      newGeneTemplate.add(newEntry);
    }

    //
    // Normalize the region names:
    //

    tcData_.normalizeTemplate(newGeneTemplate);
    
    
    //
    // Build set of new region names, and find deleted names:
    //
    
    HashSet newRegions = new HashSet();
    Iterator ngtit = newGeneTemplate.iterator();
    while (ngtit.hasNext()) {
      GeneTemplateEntry gte = (GeneTemplateEntry)ngtit.next();
      newRegions.add(gte.region);
    }
    HashSet droppedRegions = new HashSet(origRegions);
    droppedRegions.removeAll(newRegions);
    
    //
    // If region maps need to be chopped, let user confirm or abort:
    //

    Set dangling = tcData_.getDanglingRegionMaps(newGeneTemplate);
    if (!dangling.isEmpty()) {
      int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                             rMan.getString("tcsedit.danglingMaps"), 
                                             rMan.getString("tcsedit.danglingMapsTitle"),
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }

    //
    // If hierarchy need pruning, let user confirm or abort:
    //
 
    TimeCourseData.PrunedHierarchy ph = tcData_.havePrunedHierarchy(newGeneTemplate);
    if ((ph != null) && ph.droppedParents) {
      int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                             rMan.getString("tcsedit.prunedHierarchy"), 
                                             rMan.getString("tcsedit.prunedHierarchyTitle"),
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }
    
    //
    // If perturbation data needs pruning, let user confirm or abort:
    //

    boolean dropFromPert = pd.haveRegionNameInRegionRestrictions(droppedRegions);
    if (dropFromPert) {
      int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                             rMan.getString("tcsedit.pertRegRestDropped"), 
                                             rMan.getString("tcsedit.pertRegRestDroppedTitle"),
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }   
    
    //
    // If topo data needs pruning:
    //

    boolean dropFromTopo = tcData_.haveRegionNameInTopology(droppedRegions);
    if (dropFromTopo) {
      int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                             rMan.getString("tcsedit.regTopoRegDropped"), 
                                             rMan.getString("tcsedit.regTopoRegDroppedTitle"),
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }
    }   
    
    //
    // FIXME? Currently, temporal input data has its own independent region
    // namespace.  Perhaps they should be tied together, which would require
    // dropping TIRD regions at this point too...
    
    //
    // Undo/Redo support
    //

    UndoSupport support = new UndoSupport(appState_, "undo.tcsedit");      
    boolean doIssue = false;

    if (!dangling.isEmpty()) {
      TimeCourseChange[] tcca = 
        tcData_.repairDanglingRegionMaps(dangling, newGeneTemplate);
      for (int i = 0; i < tcca.length; i++) {
        support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcca[i]));
        doIssue = true;
      }
    }

    boolean doPerts = false;
    
    Iterator drit = droppedRegions.iterator();
    while (drit.hasNext()) {
      String dropRegion = (String)drit.next();
      
      TimeCourseChange drtopo = tcData_.dropRegionTopologyRegionName(dropRegion);
      if (drtopo != null) {
        support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, drtopo));
        doIssue = true;
      }

      PertDataChange[] pdc = pd.dropRegionNameForRegionRestrictions(dropRegion);
      if (pdc.length > 0) {
        support.addEdits(PertDataChangeCmd.wrapChanges(appState_, dacx_, pdc));
        doPerts = true;
      }
    
    }
    
    ArrayList origIndices = new ArrayList();
    for (int i = 0; i < size; i++) {
      TimeCourseSetupTableModel.TableRow tr = (TimeCourseSetupTableModel.TableRow)vals.get(i);
      origIndices.add(tr.origIndex);
    }   
    
    TimeCourseChange tcc = 
      tcData_.updateWithNewGeneTemplate(newGeneTemplate, origIndices);
    if (tcc != null) {
      support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));
      doIssue = true;
    }

    if (ph != null) {
      TimeCourseChange htcc = tcData_.setRegionHierarchy(ph.parents, ph.roots, true);
      if (htcc != null) {
        support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, htcc));
        doIssue = true;
      }
    }
  
    if (doPerts) {
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    }
    if (doIssue) {
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();
    }
    return (true);
  }
}
