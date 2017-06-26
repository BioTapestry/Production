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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.PerturbedTimeCourseGene;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.PertSourcesDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DoubleEditor;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ProtoDouble;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrackingDoubleRenderer;
import org.systemsbiology.biotapestry.util.TrackingUnit;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing perturbed expression state
*/

public class PerturbExpressionEntryDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private HashMap<String, TabData> tabData_;
  private UIComponentSource uics_;
  private UndoFactory uFac_; 
  private JTabbedPane tabPane_;
  private HashMap<Integer, TabData> tabMap_;
  private TabData tdatf_;
  private ArrayList<EnumCell> confidenceEnum_;
  private ArrayList<EnumCell> expressEnum_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private TimeAxisDefinition tad_;
  
  private static final long serialVersionUID = 1L;
 
  /***************************************************************************
  **
  ** Wrapper on constructor.  Give user a chance to manage perturbation
  ** sources first
  */ 
  
  public static PerturbExpressionEntryDialog perturbExpressionEntryDialogWrapper(UIComponentSource uics, PerturbationData pd,
                                                                                 TabPinnedDynamicDataAccessContext tapdx,
                                                                                 UndoFactory uFac) {
    
    boolean haveDefs = pd.getSourceDefKeys().hasNext();
    
    TimeCourseData tcd = tapdx.getExpDataSrc().getTimeCourseData();
    TimeAxisDefinition tad = tapdx.getExpDataSrc().getTimeAxisDefinition();
    UiUtil.fixMePrintout("WRONG! THis is always per tab, even with shared pert data");
    ArrayList<TimeCourseDataMaps.TCMapping> mappedIDs = new ArrayList<TimeCourseDataMaps.TCMapping>();
    mappedIDs.add(tapdx.getDataMapSrc().getTimeCourseDataMaps());
    
 
    if (!haveDefs) {
      ResourceManager rMan = uics.getRMan();
      int ok = JOptionPane.showConfirmDialog(uics.getTopFrame(), rMan.getString("tcentryp.needSourcesDefined"),
                                             rMan.getString("tcentryp.needSourcesDefinedTitle"), 
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (null);
      }
      uics.getCommonView().launchPerturbationsManagementWindow(new PertFilterExpression(PertFilterExpression.Op.ALWAYS_OP), uics, pd, tcd, tad, uFac);
      return (null);
    }
    PerturbExpressionEntryDialog peed = new PerturbExpressionEntryDialog(uics, pd, tcd, tad, mappedIDs, null, uFac);
    return (peed);
  }  
  
  /***************************************************************************
  **
  ** Wrapper on constructor.  Give user a chance to manage perturbation
  ** sources first
  */
  
  public static PerturbExpressionEntryDialog launchIfPerturbSourcesExist(UIComponentSource uics, 
                                                                         PerturbationData pd, 
                                                                         TimeCourseData tcd, 
                                                                         TimeAxisDefinition tad, String mid, UndoFactory uFac) {
    boolean haveDefs = pd.getSourceDefKeys().hasNext();
    if (!haveDefs) {
      ResourceManager rMan = uics.getRMan();
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("tcentryp.cannotEditWithoutSources"), 
                                    rMan.getString("tcentryp.cannotEditWithoutSourcesTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }
    PerturbExpressionEntryDialog peed = new PerturbExpressionEntryDialog(uics, pd, tcd, tad, null, mid, uFac);
    return (peed);
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
  
  private PerturbExpressionEntryDialog(UIComponentSource uics, PerturbationData pd, 
                                       TimeCourseData tcd, TimeAxisDefinition tad, 
                                       List<TimeCourseDataMaps.TCMapping> mappedIDs, 
                                       String mid, UndoFactory uFac) {     
    super(uics.getTopFrame(), uics.getRMan().getString("tcentryp.title"), true);
    uics_ = uics;
    uFac_ = uFac;
    pd_ = pd;
    tad_ = tad;
    tcd_ = tcd;
    ResourceManager rMan = uics.getRMan();
    boolean doForTable = (mid != null);
    if (doForTable) {
      String format = rMan.getString("tcentryp.forTableTitle");
      String desc = MessageFormat.format(format, new Object[]{mid});
      setTitle(desc);
    }
          
    setSize(1000, 750);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
       
    confidenceEnum_ = buildConfidenceEnum(); 
    expressEnum_ = buildExpressionEnum(); 
    
    //
    // Build the values table tabs.
    //

    tabData_ = new HashMap<String, TabData>();
    tabMap_ = new HashMap<Integer, TabData>();
    if (!doForTable) {
      tabPane_ = new JTabbedPane();      
      Iterator<TimeCourseDataMaps.TCMapping> midit = mappedIDs.iterator();
      int index = 0;
      while (midit.hasNext()) {
        TimeCourseDataMaps.TCMapping tcm = midit.next();
        if (tcd.getTimeCourseDataCaseInsensitive(tcm.name) == null) {
          continue;
        } 
        TabData tdat = buildATab(tcm.name, pd_);
        tabData_.put(tcm.name, tdat);
        if (tdatf_ == null) {
          tdatf_ = tdat;
        }
        tabPane_.addTab(tcm.name, tdat.panel);
        tabMap_.put(new Integer(index++), tdat);
      }
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tabPane_, gbc);
      tabPane_.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {     
            stopTheEditing(false);
            tdatf_ = tabMap_.get(new Integer(tabPane_.getSelectedIndex()));
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
    } else {    
      TabData tdat = buildATab(mid, pd_);
      tabData_.put(mid, tdat);
      tdatf_ = tdat;
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tdat.panel, gbc);
    } 
    
    //
    // Load the preexisting information into the cache:
    //
    
    initializeCache();
    
    //
    // Finish building and display:
    //    

    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    ds.buildAndInstallButtonBox(cp, 9, 10, true, false); 
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    stopTheEditing(false);
    applyProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    stopTheEditing(false);
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
    stopTheEditing(true);
    setVisible(false);
    dispose();
    return;
  } 

  /***************************************************************************
  **
  ** Build up perturbation options
  */ 
  
  private Vector<TrueObjChoiceContent> buildPertOptions(Iterator<PertSources> pKeys, PerturbationData pd) {
    Vector<TrueObjChoiceContent> pertOptions = new Vector<TrueObjChoiceContent>();
    if (!pKeys.hasNext()) {
      TrueObjChoiceContent tocc = new TrueObjChoiceContent(uics_.getRMan().getString("tcentryp.noSourceSpecified"), null);
      pertOptions.add(tocc);
    } else {
      while (pKeys.hasNext()) {
        PertSources pss = pKeys.next();
        TrueObjChoiceContent tocc = new TrueObjChoiceContent(pss.getDisplayString(pd, PertSources.BRACKET_FOOTS), pss);
        pertOptions.add(tocc);      
      }
    }
    return (pertOptions);
  }
  
  /***************************************************************************
  **
  ** Build a tab for one species
  */ 
  
  private TabData buildATab(String mappedID, PerturbationData pd) {       
    ResourceManager rMan = uics_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();

    final TabData tdat = new TabData();
    
    // This gets the gene field stocked, so do early:
    
    JPanel tcGeneControls = buildEntryControls(tdat, mappedID, rMan, gbc);   
    
    //
    // Build the values table.
    //
       
    tdat.panel = new JPanel();
    tdat.panel.setLayout(new GridBagLayout());
    int pRow = 0;
    JLabel label = new JLabel(rMan.getString("tcentryp.perSource"));
    tdat.currSources = buildPertOptions(tdat.gene.getPertKeys(), pd);
    // Gotta clone; the combo is using the Vector as-is for its backing store!!!!
    tdat.perturbSources = new JComboBox(new Vector(tdat.currSources));
    tdat.perturbSources.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {  
          TrueObjChoiceContent item = (TrueObjChoiceContent)ev.getItem();
          int state = ev.getStateChange();
          updateForSource(tdat, item, (state == ItemEvent.DESELECTED));
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc, 0, pRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tdat.panel.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, pRow, 4, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    tdat.panel.add(tdat.perturbSources, gbc);
 
    tdat.addSources = new FixedJButton(rMan.getString("tcentryp.addSources"));
    tdat.addSources.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          PertSourcesDialog psd = new PertSourcesDialog(uics_, pd_, null, tdat.dataCache.keySet().iterator());
          psd.setVisible(true);
          if (!psd.haveResult()) {
            return;
          }
          PertSources pss = psd.getResult();
          TrueObjChoiceContent tocc = new TrueObjChoiceContent(pss.getDisplayString(pd_, PertSources.BRACKET_FOOTS), pss);
          tdat.currSources.add(tocc);
          // Ditch placeholder if it was there:
          TrueObjChoiceContent firstSrc = tdat.currSources.get(0);
          if (firstSrc.val == null) {
            tdat.currSources.remove(0);
          }
          UiUtil.replaceComboItems(tdat.perturbSources, tdat.currSources);
          tdat.perturbSources.setSelectedItem(tocc);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    tdat.deleteSources = new FixedJButton(rMan.getString("tcentryp.deleteSources"));
    tdat.deleteSources.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)tdat.perturbSources.getSelectedItem();
          PertSources pss = (PertSources)tocc.val;
          tdat.dataCache.remove(pss);
          tdat.currSources.remove(tocc);
          if (tdat.currSources.isEmpty()) {
            tocc = new TrueObjChoiceContent(uics_.getRMan().getString("tcentryp.noSourceSpecified"), null);
            tdat.currSources.add(tocc);
          }        
          UiUtil.replaceComboItems(tdat.perturbSources, tdat.currSources);
          UiUtil.initCombo(tdat.perturbSources);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    tdat.deleteSources.setEnabled(false);
         
    tdat.editSources = new FixedJButton(rMan.getString("tcentryp.editSources"));
    tdat.editSources.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)tdat.perturbSources.getSelectedItem();
          int editIndex = tdat.perturbSources.getSelectedIndex();
          PertSources editOrig = ((PertSources)tocc.val).clone();
          PertSourcesDialog psd = new PertSourcesDialog(uics_, pd_, (PertSources)tocc.val, tdat.dataCache.keySet().iterator());
          psd.setVisible(true);
          if (!psd.haveResult()) {
            return;
          }
          PertSources pss = psd.getResult();
          // Gotta shift cache key on edit:
          if (!pss.equals(editOrig)) {
            CachedValues cv = tdat.dataCache.remove(editOrig);
            if (cv == null) {
              throw new IllegalStateException();
            }
            tdat.dataCache.put(pss, cv);
          }
          tocc = new TrueObjChoiceContent(pss.getDisplayString(pd_, PertSources.BRACKET_FOOTS), pss);
          tdat.currSources.set(editIndex, tocc);
          UiUtil.replaceComboItems(tdat.perturbSources, tdat.currSources);
          tdat.perturbSources.setSelectedItem(tocc);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    tdat.editSources.setEnabled(false);
   
    
    UiUtil.gbcSet(gbc, 5, pRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tdat.panel.add(tdat.addSources, gbc);
    UiUtil.gbcSet(gbc, 6, pRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tdat.panel.add(tdat.deleteSources, gbc);
    UiUtil.gbcSet(gbc, 7, pRow++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tdat.panel.add(tdat.editSources, gbc);
  
    //
    // Sub panel controlled by source combo choice:
    //
        
    JPanel subPanel = new JPanel();
    subPanel.setLayout(new GridBagLayout());
    String srcFormat = rMan.getString("tcentryp.setForSource");
    String pert = rMan.getString("tcentryp.notSpecified");
    String borderHeading = MessageFormat.format(srcFormat, new Object[] {pert}); 
    tdat.tBord = new TitledBorder(borderHeading);
    subPanel.setBorder(tdat.tBord);
    int spRow = 0;
       
    UiUtil.gbcSet(gbc, 0, spRow++, 10, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    subPanel.add(tcGeneControls, gbc);
              
    tdat.est = new EditableTable(uics_, new TimeCourseTableModel(uics_, tad_, tdat), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.singleSelectOnly = true;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    HashMap<Integer, TrackingUnit> tdrxMap = new HashMap<Integer, TrackingUnit>();
    tdat.tujv = new TrackingUnit.JustAValue(false);
    tdrxMap.put(Integer.valueOf(TimeCourseTableModel.CTRL_VALUE), tdat.tujv);
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.CTRL_VALUE), new EditableTable.EnumCellInfo(false, expressEnum_, tdrxMap, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.PERT_VALUE), new EditableTable.EnumCellInfo(false, expressEnum_, EnumCell.class));
    etp.perColumnEnums.put(new Integer(TimeCourseTableModel.CONFIDENCE), new EditableTable.EnumCellInfo(false, confidenceEnum_, EnumCell.class));
    JPanel tablePan = tdat.est.buildEditableTable(etp);
    HashMap<Integer, TrackingUnit> tdrMap = new HashMap<Integer, TrackingUnit>();
    List valList = ((TimeCourseTableModel)tdat.est.getModel()).getValueColumn();
    TrackingUnit tu = new TrackingUnit.ListTrackingUnit(valList, ExpressionEntry.VARIABLE);
    tdrMap.put(Integer.valueOf(TimeCourseTableModel.ACTIVITY), tu);
    List val2List = ((TimeCourseTableModel)tdat.est.getModel()).getValueColumnToo();
    tdat.tuo = new TrackingUnit.ListTrackingUnitWithOverride(val2List, ExpressionEntry.VARIABLE, false);
    tdrMap.put(Integer.valueOf(TimeCourseTableModel.CTRL_ACTIV), tdat.tuo);
    tdat.est.getTable().setDefaultRenderer(ProtoDouble.class, new TrackingDoubleRenderer(tdrMap, uics_.getHandlerAndManagerSource()));
    // FIX ME, MAKE AVAILABLE FROM SUPERCLASS   
    ((DefaultTableCellRenderer)tdat.est.getTable().getDefaultRenderer(String.class)).setHorizontalAlignment(JLabel.CENTER);
    tdat.est.setEnabled(false);
       
    UiUtil.gbcSet(gbc, 0, spRow, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    subPanel.add(tablePan, gbc);
     
    UiUtil.gbcSet(gbc, 0, pRow, 8, 8, UiUtil.BO, 0, 0, 15, 15, 15, 15, UiUtil.CEN, 1.0, 1.0);
    tdat.panel.add(subPanel, gbc);
        
    return (tdat);
  }
  
  /***************************************************************************
  **
  ** Get the cache loaded from the gene we are editing
  */ 
  
  private void initializeCache() { 
    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      TimeCourseGene gene = tdat.gene;
      Vector srcs = tdat.currSources;
      int numS = srcs.size();
      for (int i = 0; i < numS; i++) {
        TrueObjChoiceContent tocc = (TrueObjChoiceContent)srcs.get(i);
        if (tocc.val == null) {
          continue;
        }
        PertSources pss = (PertSources)tocc.val;
        PerturbedTimeCourseGene ptcg = gene.getPerturbedState(pss);
        List vals = buildTableRows(tdat, pss);
        CachedValues cv = new CachedValues(ptcg.isInternalOnly(), 
                                           ptcg.getTimeCourseNote(), 
                                           ptcg.getConfidence(), 
                                           ptcg.usingDistinctControlExpr(), vals);
        tdat.dataCache.put(pss.clone(), cv);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Extract vals from UI into a cached pert gene
  */ 
  
  private void extractUiToCache(TabData tdat, TrueObjChoiceContent item) {
    if (item.val == null) {
      return;
    }
    CachedValues cv = tdat.dataCache.get(item.val);
    if (cv == null) {  // Deleted entry is gone...
      return;
    }
    
    EditableTable.TableModel tm = tdat.est.getModel();
    ChoiceContent cc = (ChoiceContent)tdat.confBox.getSelectedItem();
    String newText = tdat.noteField.getText();
    if (newText.trim().equals("")) {
      newText = null;
    }
    boolean isInternal = tdat.internalChoice.isSelected();
    List vals = tm.getValuesFromTable();
        
    cv.confidence = cc.val;
    cv.isInternal = isInternal;
    cv.note = newText;
    cv.tableVals = vals;
    cv.setCtrlExp = tdat.setCtrlExpressionBox.isSelected();
    return;
  }

  /***************************************************************************
  **
  ** Update the controls for the new pert source
  */ 
  
  private void updateForSource(TabData tdat, TrueObjChoiceContent item, boolean leaving) {
    if (leaving) {
      stopTheEditing(false);
      extractUiToCache(tdat, item);
    } else {
      installToUI(tdat, item);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Update the controls for the new pert source
  */ 
  
  private void installToUI(TabData tdat, TrueObjChoiceContent item) {  
    EditableTable.TableModel tm = tdat.est.getModel();
    CachedValues cv = null;
    if (item.val != null) {
      cv = tdat.dataCache.get(item.val);
    }
    if (cv == null) {
      tdat.settingConf = true;
      UiUtil.initCombo(tdat.confBox);
      tdat.settingConf = false;
      tdat.noteField.setText("");  
      tdat.internalChoice.setSelected(false);
      tdat.settingCtrl = true;
      tdat.setCtrlExpressionBox.setSelected(false);
      tdat.settingCtrl = false;
      tdat.setCtrlExpression = false;
      tdat.tuo.setOverride(false);
      tdat.tujv.setOverride(false);
      List vals = buildTableRows(tdat, (PertSources)item.val);
      tm.extractValues(vals);
      tm.fireTableDataChanged();
      boolean newGuy = (item.val != null);
      if (newGuy) {
        ChoiceContent cc = (ChoiceContent)tdat.confChoices.get(0);
        cv = new CachedValues(false, null, cc.val, false, vals);
        tdat.dataCache.put((PertSources)item.val, cv);
      }
  
      manageControls(tdat, newGuy, item);
      return;
    }
 
    //
    // Have stashed items:
    //
    
    tm.extractValues(cv.tableVals);
    tm.fireTableDataChanged();
    
    int numItems = tdat.confChoices.size();
    for (int i = 0; i < numItems; i++) {
      ChoiceContent cc = (ChoiceContent)tdat.confChoices.get(i);
      if (cc.val == cv.confidence) {
        tdat.settingConf = true;
        tdat.confBox.setSelectedItem(cc);
        tdat.settingConf = false;
        break;
      }
    }
 
    tdat.noteField.setText((cv.note == null) ? "" : cv.note);  
    tdat.internalChoice.setSelected(cv.isInternal);
    tdat.settingCtrl = true;
    tdat.setCtrlExpressionBox.setSelected(cv.setCtrlExp);   
    tdat.settingCtrl = false;
    tdat.setCtrlExpression = cv.setCtrlExp;
    tdat.tuo.setOverride(cv.setCtrlExp);
    tdat.tujv.setOverride(cv.setCtrlExp);

    manageControls(tdat, true, item);
    return;
  }
 
  /***************************************************************************
  **
  ** Stock the entry controls
  */ 
  
  private void manageControls(TabData tdat, boolean enabled, TrueObjChoiceContent item) {   
    tdat.deleteSources.setEnabled(enabled);
    tdat.editSources.setEnabled(enabled);
    tdat.est.setEnabled(enabled);
    tdat.confBox.setEnabled(enabled);
    tdat.noteField.setEnabled(enabled);
    tdat.internalChoice.setEnabled(enabled);
    tdat.confLab.setEnabled(enabled);
    tdat.noteLab.setEnabled(enabled);
    tdat.setCtrlExpressionBox.setEnabled(enabled);
    
    ResourceManager rMan = uics_.getRMan();
    String srcFormat = rMan.getString("tcentryp.setForSource");
    String pertName = (item.val == null) ? rMan.getString("tcentryp.notSpecified") : item.name;
    String borderHeading = MessageFormat.format(srcFormat, new Object[] {pertName}); 
    tdat.tBord.setTitle(borderHeading);
    tdat.panel.repaint(); // makes title change show up!
    return;
  }
  
  /***************************************************************************
  **
  ** Stock the entry controls
  */ 
  
  private JPanel buildEntryControls(TabData tdat, String mappedID,
                                    ResourceManager rMan, GridBagConstraints gbc) {       
 
    JPanel tcGeneControls = new JPanel();
    tcGeneControls.setLayout(new GridBagLayout()); 

    tdat.gene = tcd_.getTimeCourseData(mappedID); 
    tdat.confChoices = buildConfidenceChoices();
    tdat.confBox = new JComboBox(new Vector(tdat.confChoices));
    final TabData forButton = tdat; 
    tdat.confBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          if (forButton.settingConf) {
            return;
          }
          EnumCell.ReadOnlyEnumCellEditor ece = ((TimeCourseTableModel)forButton.est.getModel()).reviseConfidenceDisplay();
          ece.revalidate();
          forButton.est.getModel().fireTableDataChanged();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });

    int row = 0;      
    tdat.confLab = new JLabel(rMan.getString("tcentryp.defaultConfidence"));          
    UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.confLab, gbc);
    UiUtil.gbcSet(gbc, 1, row, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.confBox, gbc);    
 
    tdat.noteLab = new JLabel(rMan.getString("tcentryp.note"));  
    tdat.noteField = new JTextField();
    UiUtil.gbcSet(gbc, 3, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.noteLab, gbc);
    UiUtil.gbcSet(gbc, 4, row, 3, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    tcGeneControls.add(tdat.noteField, gbc);    
   
    tdat.internalChoice = new JCheckBox(rMan.getString("tcentryp.internalOnly"));
    UiUtil.gbcSet(gbc, 7, row++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.internalChoice, gbc);
    
    tdat.setCtrlExpressionBox = new JCheckBox(rMan.getString("tcentryp.setControls"));
    UiUtil.gbcSet(gbc, 0, row, 8, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    tcGeneControls.add(tdat.setCtrlExpressionBox, gbc);
    tdat.setCtrlExpressionBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          if (forButton.settingCtrl) {
            return;
          }
          ((TimeCourseTableModel)forButton.est.getModel()).reviseControlExpressionDisplay();
          forButton.est.getModel().fireTableDataChanged();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    return (tcGeneControls);  
  }
  
  /***************************************************************************
  **
  ** Stop all table editing.  Call when changing tabs or apply/OK!
  */ 
  
  private void stopTheEditing(boolean doCancel) {
    tdatf_.est.stopTheEditing(doCancel);
    return;
  }  
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used for tracking tabs
  */

  class TabData {
    TimeCourseGene gene;
    EditableTable est;
    JPanel panel;
    JComboBox perturbSources;
    FixedJButton editSources;
    FixedJButton addSources;
    FixedJButton deleteSources;
    boolean settingConf;
    JComboBox confBox;
    JTextField noteField;
    JCheckBox internalChoice;
    HashMap<PertSources, CachedValues> dataCache;
    Vector<TrueObjChoiceContent> currSources;
    Vector confChoices;
    JLabel confLab;          
    JLabel noteLab;
    TitledBorder tBord;
    boolean setCtrlExpression;
    boolean settingCtrl;
    JCheckBox setCtrlExpressionBox;
    TrackingUnit.ListTrackingUnitWithOverride tuo;
    TrackingUnit.JustAValue tujv;
    
    TabData() {
      dataCache = new HashMap<PertSources, CachedValues>();
    }
  }
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class TimeCourseTableModel extends EditableTable.TableModel {
    
    final static int TIME        = 0;
    final static int REGION      = 1;
    final static int CTRL_VALUE  = 2;
    final static int CTRL_ACTIV  = 3;
    final static int PERT_VALUE  = 4;
    final static int ACTIVITY    = 5;
    final static int CONFIDENCE  = 6;
    private final static int NUM_COL_ = 7; 
    
    private final static int HIDDEN_CONFIDENCE_     = 0;
    private final static int HIDDEN_TIME_           = 1;
    private final static int HIDDEN_REG_VALUE_      = 2;
    private final static int HIDDEN_REG_ACTIV_      = 3;
    private final static int HIDDEN_LOC_CTRL_VALUE_ = 4;
    private final static int HIDDEN_LOC_CTRL_ACTIV_ = 5;
    private final static int NUM_HIDDEN_            = 6;
    
    private TabData tdat_;
    
    class TableRow {
      String time;
      String region;
      EnumCell ctrlValue;
      ProtoDouble ctrlActivity;
      EnumCell pertValue;
      ProtoDouble activity;
      EnumCell confidence;
      Integer hiddenConfidence;
      Integer hiddenTime;
      Integer hiddenRegValue;
      ProtoDouble hiddenRegActiv;
      Integer hiddenLocCtrlValue;
      ProtoDouble hiddenLocCtrlActivity;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        time = (String)columns_[TIME].get(i);
        region = (String)columns_[REGION].get(i);       
        ctrlValue = (EnumCell)columns_[CTRL_VALUE].get(i);
        ctrlActivity = (ProtoDouble)columns_[CTRL_ACTIV].get(i);
        pertValue = (EnumCell)columns_[PERT_VALUE].get(i);
        activity = (ProtoDouble)columns_[ACTIVITY].get(i);
        confidence = (EnumCell)columns_[CONFIDENCE].get(i);
        hiddenConfidence = (Integer)hiddenColumns_[HIDDEN_CONFIDENCE_].get(i);
        hiddenTime = (Integer)hiddenColumns_[HIDDEN_TIME_].get(i);
        hiddenRegValue = (Integer)hiddenColumns_[HIDDEN_REG_VALUE_].get(i);
        hiddenRegActiv = (ProtoDouble)hiddenColumns_[HIDDEN_REG_ACTIV_].get(i);
        hiddenLocCtrlValue = (Integer)hiddenColumns_[HIDDEN_LOC_CTRL_VALUE_].get(i);
        hiddenLocCtrlActivity = (ProtoDouble)hiddenColumns_[HIDDEN_LOC_CTRL_ACTIV_].get(i);
      }
      
      void toCols() {
        columns_[TIME].add(time); 
        columns_[REGION].add(region);
        columns_[CTRL_VALUE].add(ctrlValue);
        columns_[CTRL_ACTIV].add(ctrlActivity);
        columns_[PERT_VALUE].add(pertValue);
        columns_[ACTIVITY].add(activity);
        columns_[CONFIDENCE].add(confidence);
        hiddenColumns_[HIDDEN_CONFIDENCE_].add(hiddenConfidence);
        hiddenColumns_[HIDDEN_TIME_].add(hiddenTime);
        hiddenColumns_[HIDDEN_REG_VALUE_].add(hiddenRegValue);
        hiddenColumns_[HIDDEN_REG_ACTIV_].add(hiddenRegActiv); 
        hiddenColumns_[HIDDEN_LOC_CTRL_VALUE_].add(hiddenLocCtrlValue); 
        hiddenColumns_[HIDDEN_LOC_CTRL_ACTIV_].add(hiddenLocCtrlActivity); 
        return;
      }
    }
  
    TimeCourseTableModel(UIComponentSource uics, TimeAxisDefinition tad, TabData td) {
      super(uics,NUM_COL_);
      tdat_ = td;
      String displayUnits = tad.unitDisplayString();
      ResourceManager rMan = uics.getRMan();
      String timeColumnHeading = MessageFormat.format(rMan.getString("tcentryp.timeColFormat"), new Object[] {displayUnits});

      colNames_ = new String[] {timeColumnHeading,
                                "tcentryp.region",
                                "tcentryp.ctrlValue",
                                "tcentryp.ctrlActivity",
                                "tcentryp.pertValue",
                                "tcentryp.activity",          
                                "tcentryp.confidence"};
      colUseDirect_ = new boolean[] {true,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false,
                                     false};      
      colClasses_ = new Class[] {String.class,      
                                 String.class,
                                 EnumCell.class,
                                 ProtoDouble.class,
                                 EnumCell.class,
                                 ProtoDouble.class,     
                                 EnumCell.class};
      canEdit_ = new boolean[] {false,
                                false,
                                false, //changes....
                                false, //changes....
                                true, 
                                true,//changes....
                                true};
      addHiddenColumns(NUM_HIDDEN_);
    }
    
    List getValueColumn() {
      return (columns_[PERT_VALUE]); 
    }  
    
    List getValueColumnToo() {
      return (columns_[CTRL_VALUE]); 
    }  
       
    public boolean isCellEditable(int r, int c) {
      try {
        if (c == ACTIVITY) {
          EnumCell currVala = (EnumCell)columns_[PERT_VALUE].get(r);
          return (currVala.value == ExpressionEntry.VARIABLE);
        } else if (c == CTRL_VALUE) {
          return (tdat_.setCtrlExpressionBox.isSelected());
        } else if (c == CTRL_ACTIV) {
          if (!tdat_.setCtrlExpressionBox.isSelected()) {
            return (false);
          }
          EnumCell currVala = (EnumCell)columns_[CTRL_VALUE].get(r);
          return (currVala.value == ExpressionEntry.VARIABLE);
        } else {
          return (super.isCellEditable(r, c));
        }   
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (false);
    }
    
    //
    // Handle change of combo setting
    //
    
    public boolean comboEditingDone(int col, int row, Object val) {
      if (col == PERT_VALUE) {
        return (activityEditingDone(row, val));
      } else if (col == CONFIDENCE) {
        return (confidenceEditingDone(row, val));
      } else {
        return (true);
      }
    }    
    
    public boolean activityEditingDone(int row, Object val) {
      EnumCell newAct = (EnumCell)val;   
      EnumCell currAct = (EnumCell)columns_[PERT_VALUE].get(row);
      if (currAct.value == newAct.value) {
        return (true);
      }
      // Have to get the column next door to repaint as active/inactive:
      tmqt_.invalidate();
      tmqt_.validate();
      tmqt_.repaint();
      return (true);  
    }       

    public boolean confidenceEditingDone(int row, Object val) {
      EnumCell newConf = (EnumCell)val;
      EnumCell currConf = (EnumCell)columns_[CONFIDENCE].get(row);
      if (currConf.value == newConf.value) {
        return (true);
      }
      int displayConfidence = newConf.value;
      
      int baseConfidence = tdat_.confBox.getSelectedIndex();
      int entryConfidence = 
        TimeCourseGene.reverseMapEntryConfidence(baseConfidence, displayConfidence);
      hiddenColumns_[HIDDEN_CONFIDENCE_].set(row, new Integer(entryConfidence));
      return (true);
    }
    
    EnumCell.ReadOnlyEnumCellEditor reviseConfidenceDisplay() {
      columns_[CONFIDENCE].clear();
      Iterator ccit = hiddenColumns_[HIDDEN_CONFIDENCE_].iterator();
      TableColumn tc = tdat_.est.getTable().getColumnModel().getColumn(CONFIDENCE);   
      EnumCell.ReadOnlyEnumCellEditor ece = (EnumCell.ReadOnlyEnumCellEditor)tc.getCellEditor();
      int editRow = ece.getCurrRow();
      ChoiceContent selConf = (ChoiceContent)tdat_.confBox.getSelectedItem();
      int numEnum = confidenceEnum_.size();
      int rowNum = 0;
      while (ccit.hasNext()) {
        Integer hidConf = (Integer)ccit.next();
        int entryConfidence = hidConf.intValue();
        int baseConfidence = selConf.val;
        int displayConfidence = TimeCourseGene.mapEntryConfidence(baseConfidence, entryConfidence);
        EnumCell displayCell = null; 
        for (int i = 0; i < numEnum; i++) {
          EnumCell ecr = (EnumCell)confidenceEnum_.get(i);
          if (ecr.value == displayConfidence) {
            displayCell = new EnumCell(ecr);
            columns_[CONFIDENCE].add(displayCell);
            break;
          }
        }
        if (editRow == rowNum++) {
          ece.reviseValue(displayCell);
        }
      }      
      return (ece);  
    }
    
    void reviseControlExpressionDisplay() {
      //
      // Install displayed control expression from correct source
      //
      boolean localControl = tdat_.setCtrlExpressionBox.isSelected();
      
      //
      // If the display was live, extract out current UI values before
      // restocking:
      //
        
      if (tdat_.setCtrlExpression) {
        stopTheEditing(false);
        hiddenColumns_[HIDDEN_LOC_CTRL_VALUE_].clear();
        hiddenColumns_[HIDDEN_LOC_CTRL_ACTIV_].clear();
        Iterator cvit = columns_[CTRL_VALUE].iterator();
        Iterator cait = columns_[CTRL_ACTIV].iterator();
        while (cvit.hasNext()) {
          EnumCell dispVal = (EnumCell)cvit.next();
          ProtoDouble dispActiv = (ProtoDouble)cait.next();
          hiddenColumns_[HIDDEN_LOC_CTRL_VALUE_].add(new Integer(dispVal.value));
          hiddenColumns_[HIDDEN_LOC_CTRL_ACTIV_].add(dispActiv.clone());        
        }       
      }
      tdat_.setCtrlExpression = localControl;
      tdat_.tuo.setOverride(localControl);
      tdat_.tujv.setOverride(localControl);
      
   
      //
      // Now restock the display columns with the stashed hidden values:
      //
      
      int whichValCol = (localControl) ? HIDDEN_LOC_CTRL_VALUE_ : HIDDEN_REG_VALUE_;
      int whichActivCol = (localControl) ? HIDDEN_LOC_CTRL_ACTIV_ : HIDDEN_REG_ACTIV_;
    
      columns_[CTRL_VALUE].clear();
      columns_[CTRL_ACTIV].clear();
       
      Iterator cvit = hiddenColumns_[whichValCol].iterator();
      Iterator cait = hiddenColumns_[whichActivCol].iterator();
             
      int numEnum = expressEnum_.size(); 
      while (cvit.hasNext()) {
        Integer hidVal = (Integer)cvit.next();
        ProtoDouble hidActiv = (ProtoDouble)cait.next();
        int showVal = hidVal.intValue();
        EnumCell displayCell = null; 
        for (int i = 0; i < numEnum; i++) {
          EnumCell ecr = expressEnum_.get(i);
          if (ecr.value == showVal) {
            displayCell = new EnumCell(ecr);
            columns_[CTRL_VALUE].add(displayCell);
            break;
          }
        }
        columns_[CTRL_ACTIV].add((hidActiv == null) ? new ProtoDouble("") : hidActiv.clone());      
      }      
      return;  
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
  ** Initialize the display
  ** 
  */
  
  private void displayProperties() {
    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();  
      TrueObjChoiceContent tocc = tdat.currSources.get(0);
      installToUI(tdat, tocc);
    }
    return;
  }

  /***************************************************************************
  **
  ** Build table rows
  */ 
  
  private List<TimeCourseTableModel.TableRow> buildTableRows(TabData tDat, PertSources pss) {
    TimeCourseTableModel tctm = (TimeCourseTableModel)tDat.est.getModel();
    ArrayList<TimeCourseTableModel.TableRow> retval = new ArrayList<TimeCourseTableModel.TableRow>(); 

    Iterator<ExpressionEntry> bit = tDat.gene.getExpressions();
    
    Iterator<ExpressionEntry> eeit = null;
    PerturbedTimeCourseGene ptcg = null;
    Iterator<ExpressionEntry> lceeit = null;
    if (pss != null) {
      ptcg = tDat.gene.getPerturbedState(pss);
      eeit = (ptcg != null) ? ptcg.getExpressions() : null;
      lceeit = ((ptcg != null) && ptcg.usingDistinctControlExpr()) ? ptcg.getControlExpressions() : null;
    }

    while (bit.hasNext()) {
      ExpressionEntry entry = bit.next();
      ExpressionEntry pertEntry = (eeit == null) ? null : eeit.next();
      ExpressionEntry crtlEntry = (lceeit == null) ? null : lceeit.next();
      TimeCourseTableModel.TableRow tr = tctm.new TableRow();    
      Integer timeIndex = new Integer(entry.getTime());
      tr.time = TimeAxisDefinition.getTimeDisplay(tad_, timeIndex, false, false);
      tr.hiddenTime = timeIndex;
      tr.region = entry.getRegion();
      
      //
      // record base expression and activity:
      //
    
      int normExpr = entry.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      tr.hiddenRegValue = new Integer(normExpr);   
      if (normExpr == ExpressionEntry.VARIABLE) {
        double normAct = entry.getVariableLevelForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
        tr.hiddenRegActiv = new ProtoDouble(normAct);
      } else {
        tr.hiddenRegActiv = null;
      }
      
      //
      // record local control expression:
      //
      
      int ctrlExpr = (crtlEntry == null) ? ExpressionEntry.NO_DATA : crtlEntry.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      tr.hiddenLocCtrlValue = new Integer(ctrlExpr);
      tr.hiddenLocCtrlActivity = ((crtlEntry != null) && (ctrlExpr == ExpressionEntry.VARIABLE)) 
        ? new ProtoDouble(crtlEntry.getVariableLevelForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED)) : null;

      //
      // Install displayed:
      //
      
      int useExp = ((crtlEntry == null) ? tr.hiddenRegValue : tr.hiddenLocCtrlValue).intValue();
      ProtoDouble useActiv = (crtlEntry == null) ? tr.hiddenRegActiv : tr.hiddenLocCtrlActivity;
      
      int ssize = expressEnum_.size();
      for (int i = 0; i < ssize; i++) {
        EnumCell ecr = expressEnum_.get(i);
        if (ecr.value == useExp) {
          tr.ctrlValue = new EnumCell(ecr);
          break;
        }
      }
      tr.ctrlActivity = (useActiv != null) ? (ProtoDouble)useActiv.clone() : new ProtoDouble("");

  
      //
      // Perturbed expression value:
      //
      
      int pertExpr = (pertEntry == null) ? ExpressionEntry.NO_DATA 
                                         : pertEntry.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      for (int i = 0; i < ssize; i++) {
        EnumCell ecr = expressEnum_.get(i);
        if (ecr.value == pertExpr) {
          tr.pertValue = new EnumCell(ecr);
          break;
        }
      }
      tr.activity = ((pertEntry != null) && (pertExpr == ExpressionEntry.VARIABLE)) 
        ? new ProtoDouble(pertEntry.getVariableLevelForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED)) 
        : new ProtoDouble("");

      //
      // Confidence:
      //
      
      int entryConfidence = (pertEntry != null) ? pertEntry.getConfidence() : TimeCourseGene.USE_BASE_CONFIDENCE;
      tr.hiddenConfidence = new Integer(entryConfidence);
      int myConf = (ptcg != null) ? ptcg.getConfidence() : TimeCourseGene.NORMAL_CONFIDENCE;
      int showConf = TimeCourseGene.mapEntryConfidence(myConf, entryConfidence);
      int csize = confidenceEnum_.size();
      for (int i = 0; i < csize; i++) {
        EnumCell ecr = confidenceEnum_.get(i);
        if (ecr.value == showConf) {
          tr.confidence = new EnumCell(ecr);
          break;
        }
      }
      retval.add(tr);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the time course data
  ** 
  */
  
  private boolean applyProperties() {

    TrueObjChoiceContent tocc = (TrueObjChoiceContent)tdatf_.perturbSources.getSelectedItem();
    extractUiToCache(tdatf_, tocc);
       
    Iterator<TabData> tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      if (!checkValues(tdat)) {
        return (false);
      }
    }    

    UndoSupport support = uFac_.provideUndoSupport("undo.tcpertentry", dacx_);
    tdit = tabData_.values().iterator();
    while (tdit.hasNext()) {
      TabData tdat = tdit.next();
      String gName = tdat.gene.getName();
      TimeCourseChange tcc = tcd_.startGeneUndoTransaction(gName);
      tdat.gene.clearPerturbedStates();
      applyCacheData(tdat);   
      tcc = tcd_.finishGeneUndoTransaction(gName, tcc);
      support.addEdit(new TimeCourseChangeCmd(dacx_, tcc));
    }
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Apply the data to the model
  */ 
  
  private void applyCacheData(TabData tdat) {
    
    Iterator<PertSources> ckit = tdat.dataCache.keySet().iterator();
    while (ckit.hasNext()) {
      PertSources pss = ckit.next();
      CachedValues cv = tdat.dataCache.get(pss);
      PerturbedTimeCourseGene ptcg = new PerturbedTimeCourseGene(pd_, pss.clone(), cv.confidence, cv.isInternal);
      if (tdat.setCtrlExpressionBox.isSelected()) {
        ptcg.setForDistinctControlExpr();
      }   
      ptcg.setTimeCourseNote(cv.note);
      int size = cv.tableVals.size();
      for (int i = 0; i < size; i++) {
        TimeCourseTableModel.TableRow tr = (TimeCourseTableModel.TableRow)cv.tableVals.get(i);
        ExpressionEntry entry = new ExpressionEntry(tr.region, tr.hiddenTime.intValue(), tr.pertValue.value, tr.hiddenConfidence.intValue());
        double varLev = 0.0;
        if (tr.pertValue.value == ExpressionEntry.VARIABLE) {
          varLev = tr.activity.value;
        }
        entry.setVariableLevel(varLev);
        ExpressionEntry ctrlEntry = null;
        if (tdat.setCtrlExpressionBox.isSelected()) {
          ctrlEntry = new ExpressionEntry(tr.region, tr.hiddenTime.intValue(), tr.ctrlValue.value, TimeCourseGene.USE_BASE_CONFIDENCE);
          double ctrlVarLev = 0.0;
          if (tr.ctrlValue.value == ExpressionEntry.VARIABLE) {
            ctrlVarLev = tr.ctrlActivity.value;
          }
          ctrlEntry.setVariableLevel(ctrlVarLev);
        }
        ptcg.addExpression(entry, ctrlEntry);
      }
      tdat.gene.setPerturbedState(pss.clone(), ptcg);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we have a value not compatible with variable expression
  */ 
     
  private boolean varIncompatable(int expr) { 
    return ((expr == ExpressionEntry.EXPRESSED) ||
            (expr == ExpressionEntry.NOT_EXPRESSED) || 
            (expr == ExpressionEntry.WEAK_EXPRESSION));
  }
  
  /***************************************************************************
  **
  ** Check table rows for correctness
  */ 
     
  private boolean checkValues(TabData tdat) {
        
    if (tdat.gene == null) {
      return (true);
    }

    ResourceManager rMan = uics_.getRMan();
    Iterator<PertSources> ckit = tdat.dataCache.keySet().iterator();
    while (ckit.hasNext()) {
      PertSources pss = ckit.next();
      CachedValues cv = tdat.dataCache.get(pss);
   
      int size = cv.tableVals.size();
      boolean haveVar = false;
      boolean haveFixed = false;
      boolean mixedRow = false;
      for (int i = 0; i < size; i++) {
        TimeCourseTableModel.TableRow tr = (TimeCourseTableModel.TableRow)cv.tableVals.get(i); 
        int ctrlVal = (cv.setCtrlExp) ? tr.ctrlValue.value : tr.hiddenRegValue.intValue();
        if (tr.pertValue.value == ExpressionEntry.VARIABLE) {
          haveVar = true;
          if (varIncompatable(ctrlVal)) {
            mixedRow = true;
          }
        } else if (varIncompatable(tr.pertValue.value)) {
          haveFixed = true;
          if (ctrlVal == ExpressionEntry.VARIABLE) {
            mixedRow = true;
          }    
        }
   
        //
        // Check both double values for validity:
        //

        ArrayList<ProtoDouble> actToCheck = new ArrayList<ProtoDouble>();
        if (tr.pertValue.value == ExpressionEntry.VARIABLE) {
          actToCheck.add(tr.activity);
        }
        if (cv.setCtrlExp && (ctrlVal == ExpressionEntry.VARIABLE)) {
          actToCheck.add(tr.ctrlActivity);
        }        

        Iterator<ProtoDouble> atcit = actToCheck.iterator();
        while (atcit.hasNext()) {
          ProtoDouble prod = atcit.next();
          if (!prod.valid) {
            String format = rMan.getString("tcentryf.badActivityValueFormat"); 
            String desc = MessageFormat.format(format, new Object[] {tdat.gene.getName(), pss.getDisplayString(pd_, PertSources.BRACKET_FOOTS)}); 
            DoubleEditor.triggerWarningWithTag(uics_.getHandlerAndManagerSource(), uics_.getTopFrame(), desc, prod.textValue);
            return (false);
          }
          if ((prod.value < 0.0) || (prod.value > 1.0)) {
            JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                          rMan.getString("tcentry.badActivityValue"), 
                                          rMan.getString("tcentry.badActivityValueTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
        }
      }
     
      if (haveVar && haveFixed) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tcentry.mixed"),
                                      rMan.getString("tcentry.mixedTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (mixedRow) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("tcentryf.mixedRow"),
                                      rMan.getString("tcentryf.mixedTitleRow"),
                                      JOptionPane.WARNING_MESSAGE);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Build the Confidences
  ** 
  */
  
  private ArrayList<EnumCell> buildConfidenceEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    int numItems = TimeCourseGene.NUM_CONFIDENCE;
    for (int i = 0; i < numItems; i++) {
      String conf = TimeCourseGene.mapConfidence(i);
      retval.add(new EnumCell(conf, conf, i, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the Confidences
  ** 
  */
  
  private Vector<ChoiceContent> buildConfidenceChoices() { 
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    int numItems = TimeCourseGene.NUM_CONFIDENCE;
    for (int i = 0; i < numItems; i++) {
      String conf = TimeCourseGene.mapConfidence(i);
      retval.add(new ChoiceContent(conf, i));
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Build Expressions
  */
  
  private ArrayList<EnumCell> buildExpressionEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    int numItems = ExpressionEntry.NUM_EXPRESSIONS;
    for (int i = 0; i < numItems; i++) {
      String expr = ExpressionEntry.mapExpression(i);
      retval.add(new EnumCell(expr, expr, i, i));
    }
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Temporary holding class
  */
  
  public static class CachedValues {   
    boolean isInternal;
    String note;
    int confidence;
    List tableVals;
    boolean setCtrlExp;

    CachedValues(boolean isInternal, String note, int confidence, boolean setCtrlExp, List tableVals) {
      this.isInternal = isInternal;
      this.note = note;
      this.confidence = confidence;
      this.tableVals = tableVals;
      this.setCtrlExp = setCtrlExp;
    }    
  } 
}
