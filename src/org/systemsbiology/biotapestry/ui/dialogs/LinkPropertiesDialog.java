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


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.DoubleEditor;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.ProtoDouble;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrackingDoubleRenderer;
import org.systemsbiology.biotapestry.util.TrackingUnit;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing link properties
*/

public class LinkPropertiesDialog extends JDialog implements ColorDeletionListener, 
                                                             DialogSupport.DialogSupportClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private EditableTable est_;
  private ReadOnlyTable rot_;
  private String propKey_;
  private LinkProperties props_;
  private LinkProperties changedProps_;
  private List<ColorDeletionListener> deletionListeners_;
  private DataAccessContext dacx_;
  
  private BTState appState_;
  private JComboBox dirCombo_;
  private JCheckBox doSourceBox_;
  private SuggestedDrawStylePanel sdsPan_;
  private DisplayOptions dOpt_;
  
  private ArrayList<EnumCell> activities_;
  private ArrayList<EnumCell> evidence_;
  private ArrayList<EnumCell> signs_;
  
  private Set<String> myLinks_;
  private boolean forModules_;
  private boolean isForInstance_;
  private NodeAndLinkPropertiesSupport nps_;
  
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
  
  public LinkPropertiesDialog(BTState appState, DataAccessContext dacx, 
                              String propKey, boolean forModules, String ovrRef) {     
    super(appState.getTopFrame(), appState.getRMan().getString("lprop.title"), true);
    appState_ = appState;
    propKey_ = propKey;
    dacx_ = dacx;
    forModules_ = forModules;
    dOpt_ = appState_.getDisplayOptMgr().getDisplayOptions();
    nps_ = new NodeAndLinkPropertiesSupport(appState_, dacx_);  
     
    Layout layout = dacx_.getLayout();
    props_ = (forModules) ? (LinkProperties)layout.getNetModuleLinkagePropertiesFromTreeID(propKey_, ovrRef)
                          : (LinkProperties)layout.getLinkProperties(propKey_);
       
    Genome genome = dacx_.getGenome(); 
    isForInstance_ = !forModules_ && (genome instanceof GenomeInstance);
    if (forModules_) {
      myLinks_ = new HashSet<String>(props_.getLinkageList());
    } else {
      myLinks_ = layout.getSharedItems(propKey_);
    }
    boolean forRoot = (genome instanceof DBGenome);
    boolean topTwoLevels = forRoot;
    if (genome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      topTwoLevels = gi.isRootInstance();
    }
    
    // Need a local copy to hold onto special per-link property changes!
    changedProps_ = props_.clone();
    deletionListeners_ = new ArrayList<ColorDeletionListener>();
    deletionListeners_.add(this);
      
    activities_ = buildActivityEnum();
    evidence_ = buildEvidenceEnum();
    signs_ = buildSignEnum(forModules_);
     
    ResourceManager rMan = appState_.getRMan();    
    setSize(975, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    //
    // Build the tabs.
    //

    JTabbedPane tabPane = new JTabbedPane();
    if ((!(genome instanceof DynamicGenomeInstance)) || forModules) {   // include for modules
      tabPane.addTab(rMan.getString("propDialogs.modelProp"), buildModelTab());
    }      
    tabPane.addTab(rMan.getString("propDialogs.layoutProp"), buildLayoutTab());
    if (topTwoLevels && !forModules) {  // Do not include for modules
      tabPane.addTab(rMan.getString("propDialogs.freeText"), nps_.buildTextTab(myLinks_, !isForInstance_));
      tabPane.addTab(rMan.getString("propDialogs.URLTab"), nps_.buildUrlTab(myLinks_, !isForInstance_));
    }
  
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tabPane, gbc);    
    
    tabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        try {
          stopTheEditing(false);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallButtonBox(cp, 9, 10, true, false); 
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
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
  ** Used to track color deletions
  ** 
  */
  
  public void colorReplacement(Map<String, String> oldToNew) {
    Iterator<String> otnit = oldToNew.keySet().iterator();
    while (otnit.hasNext()) {
      String oldColor = otnit.next();
      String newColor = oldToNew.get(oldColor);
      changedProps_.replaceColor(oldColor, newColor);
    }
    PerLinkSpecialDrawTableModel dtm = (PerLinkSpecialDrawTableModel)rot_.getModel();
    displayPropTableProperties();
    dtm.fireTableDataChanged();
    return;
  }  

  /***************************************************************************
  **
  ** Get a linkage that we actually own from the possiblities (some may be null for VfN)
  ** Used to fix BT-10-24-07:1 (same as BT-08-14-07:5)
  ** 
  */
  
  private Linkage getALinkageWeOwn(Set<String> linkCandidates, Genome genome) {        
    Iterator<String> lit = linkCandidates.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = genome.getLinkage(linkID);
      if (link != null) {
        return (link);
      }   
    }
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Build a tab for layout properties
  ** 
  */
  
  private JPanel buildLayoutTab() {    
    
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();

    sdsPan_ = new SuggestedDrawStylePanel(appState_, dacx_, false, deletionListeners_);          
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum++, 8, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    retval.add(sdsPan_, gbc);
    rowNum += 8;
    
    //
    // Build the do source too box
    //

    doSourceBox_ = new JCheckBox(rMan.getString("lprop.doSource"));
    // WJRL 4/30/09: Having the check box set has bad side effects: unexpected 
    // color changes when setting other tabs.  Drop it.
    doSourceBox_.setSelected(false);
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 8, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(doSourceBox_, gbc);

    //
    // Build the text tag direction:
    //
      
    if (!forModules_) {
      Vector<ChoiceContent> dchoices = new Vector<ChoiceContent>();
      Set<String> dirs = BusProperties.labelDirections();
      Iterator<String> dit = dirs.iterator();
      while (dit.hasNext()) {
        String val = dit.next();
        dchoices.add(new ChoiceContent(rMan.getString("lprop." + val), BusProperties.mapFromDirectionTag(val)));
      }      
      dirCombo_ = new JComboBox(dchoices);    

      JLabel label = new JLabel(rMan.getString("lprop.tagDir"));
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      retval.add(label, gbc);

      UiUtil.gbcSet(gbc, 1, rowNum++, 7, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(dirCombo_, gbc);
    }
    
    //
    // Per-link property overrides:
    //
    
    rot_ = new ReadOnlyTable(appState_, new PerLinkSpecialDrawTableModel(appState_), new Selector());   
    rot_.setButtonHandler(new ButtonHand());
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = true;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.EDIT_BUTTON;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = null;
    tp.titleFont = null;
    tp.colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PerLinkSpecialDrawTableModel.SRC, 50, 100, 200));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PerLinkSpecialDrawTableModel.TARG, 50, 200, 500));
    tp.colWidths.add(new ReadOnlyTable.ColumnWidths(PerLinkSpecialDrawTableModel.DRAW_DESC, 200, 500, Integer.MAX_VALUE));  
    tp.clientHandlesButtonEnable = true;
    JPanel tabPan = rot_.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc, 0, rowNum, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(tabPan, gbc);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a tab for model properties
  ** 
  */
  
  private JPanel buildModelTab() {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
       
    est_ = new EditableTable(appState_, new LinkTableModel(appState_), appState_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = true;
    etp.tableIsUnselectable = false;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    if (forModules_) {
      etp.perColumnEnums.put(new Integer(LinkTableModel.MOD_SIGN_), new EditableTable.EnumCellInfo(false, signs_));
    } else {
      etp.perColumnEnums.put(new Integer(LinkTableModel.SIGN_), new EditableTable.EnumCellInfo(false, signs_));
      etp.perColumnEnums.put(new Integer(LinkTableModel.EVIDENCE_), new EditableTable.EnumCellInfo(false, evidence_));
      if (isForInstance_) {
        etp.perColumnEnums.put(new Integer(LinkTableModel.ACTIVITY_), new EditableTable.EnumCellInfo(false, activities_));
      }
    }
    JPanel tablePan = est_.buildEditableTable(etp);
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(tablePan, gbc);

    if ((!forModules_) && isForInstance_) {
      List activList = ((LinkTableModel)est_.getModel()).getActivityColumn();
      TrackingUnit tu = new TrackingUnit.ListTrackingUnit(activList, LinkageInstance.VARIABLE);
      HashMap<Integer, TrackingUnit> tdrMap = new HashMap<Integer, TrackingUnit>();
      tdrMap.put(new Integer(LinkTableModel.LEVEL_), tu);   
      est_.getTable().setDefaultRenderer(ProtoDouble.class, new TrackingDoubleRenderer(tdrMap, appState_));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Stop all table editing.  Call when changing tabs or apply/OK!
  */ 
  
  private void stopTheEditing(boolean doCancel) {
    if ((est_ == null) || forModules_) {
      return;
    }
    est_.stopTheEditing(doCancel);
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Answer if there is link custom evidence
  */ 
  
  
  private boolean linkCustomEvidence(String linkID) {
    if (forModules_) {
      return (false);
    }
    PerLinkDrawStyle plfe = null;
    Genome genome = dacx_.getGenome();
    Linkage link = genome.getLinkage(linkID);
    int evidence = link.getTargetLevel(); 
    plfe = (dOpt_ == null) ? null : dOpt_.getEvidenceDrawChange(evidence);
    return (plfe != null);
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
   
  class LinkTableModel extends EditableTable.TableModel {
    
    private final static int SOURCE_  = 0;
    private final static int TARG_    = 1;
    
    //
    // MODULE COLUMNS:
    //
 
    final static int MOD_SIGN_    = 2;
    private final static int NUM_MOD_COL_ = 3;
    
    //
    // ROOT COLUMNS:
    //
 
    private final static int LABEL_        = 2;
    final static int SIGN_         = 3;
    final static int EVIDENCE_     = 4;
    private final static int NUM_ROOT_COL_ = 5;
    
    //
    // INSTANCE COLUMNS:
    //
 
    final static int ACTIVITY_     = 5;
    private final static int LEVEL_        = 6;
    private final static int NUM_INST_COL_ = 7;
      
    private final static int LINK_ID_    = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private static final long serialVersionUID = 1L;
  
    LinkTableModel(BTState appState) {
      super(appState, (isForInstance_) ? NUM_INST_COL_ : ((forModules_) ? NUM_MOD_COL_ : NUM_ROOT_COL_));
      if (isForInstance_) {
        colNames_ = new String[] {"lptable.src",
                                  "lptable.targ",
                                  "lptable.label",
                                  "lptable.sign",
                                  "lptable.evidence",
                                  "lptable.activity",
                                  "lptable.activeLevel"};
        colClasses_ = new Class[] {String.class,
                                   String.class, 
                                   String.class,
                                   EnumCell.class,
                                   EnumCell.class,
                                   EnumCell.class,     
                                   ProtoDouble.class};
        canEdit_ = new boolean[] {false,
                                  false,
                                  true,
                                  true,
                                  true,
                                  true,
                                  true}; // sometimes! 
      } else if (forModules_) {
        colNames_ = new String[] {"lptable.src",
                                  "lptable.targ",
                                  "lptable.sign"};
        colClasses_ = new Class[] {String.class,
                                   String.class, 
                                   EnumCell.class};
        canEdit_ = new boolean[] {false,
                                  false,
                                  true};
      } else {
        colNames_ = new String[] {"lptable.src",
                                  "lptable.targ",
                                  "lptable.label",
                                  "lptable.sign",
                                  "lptable.evidence"};
        colClasses_ = new Class[] {String.class,
                                   String.class, 
                                   String.class,
                                   EnumCell.class,
                                   EnumCell.class};
        canEdit_ = new boolean[] {false,
                                  false,
                                  true,
                                  true,
                                  true};
      }
      addHiddenColumns(NUM_HIDDEN_);
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        if (forModules_) {
          ModuleLinkModelTableEntry ent = new ModuleLinkModelTableEntry();
          ent.linkID = (String)hiddenColumns_[LINK_ID_].get(i);
          ent.src = (String)columns_[SOURCE_].get(i);
          ent.targ = (String)columns_[TARG_].get(i);
          ent.sign = (EnumCell)columns_[MOD_SIGN_].get(i);
          retval.add(ent);
        } else {
          ModelLinkModelTableEntry ent = new ModelLinkModelTableEntry();
          ent.linkID = (String) hiddenColumns_[LINK_ID_].get(i);
          ent.src = (String)columns_[SOURCE_].get(i);
          ent.targ = (String)columns_[TARG_].get(i);
          ent.label = (String)columns_[LABEL_].get(i);
          ent.sign = (EnumCell)columns_[SIGN_].get(i);
          ent.evidence = (EnumCell)columns_[EVIDENCE_].get(i);
          if (isForInstance_) {
            ent.activity = (EnumCell)columns_[ACTIVITY_].get(i);
            ent.level = (ProtoDouble)columns_[LEVEL_].get(i);
          }
          retval.add(ent);
        }
      }
      return (retval); 
    }
    
    List getActivityColumn() {
      return (columns_[ACTIVITY_]); 
    }      

    public void setValueAt(Object value, int r, int c) {
      try {
        Object oldVal = getValueAt(r, c);
        super.setValueAt(value, r, c);
        if (c == ACTIVITY_) {           
          EnumCell oldValEC = (EnumCell)oldVal;
          if (oldValEC.value != ((EnumCell)value).value) {
            // Have to get the column next door to repaint as active/inactive:
            tmqt_.invalidate();
            tmqt_.validate();
            tmqt_.repaint();
          }        
        }
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    } 
    
    public boolean isCellEditable(int r, int c) {
      try {
        if (c != LEVEL_) {
          return (super.isCellEditable(r, c));
        }
        EnumCell currVala = (EnumCell)columns_[ACTIVITY_].get(r);
        return (currVala.value == LinkageInstance.VARIABLE);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return (false);
    }
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        if (forModules_) {
          ModuleLinkModelTableEntry ent = (ModuleLinkModelTableEntry)rit.next();
          hiddenColumns_[LINK_ID_].add(ent.linkID);
          columns_[SOURCE_].add(ent.src);  
          columns_[TARG_].add(ent.targ);
          columns_[MOD_SIGN_].add(ent.sign);  
        } else {
          ModelLinkModelTableEntry ent = (ModelLinkModelTableEntry)rit.next();
          hiddenColumns_[LINK_ID_].add(ent.linkID);
          columns_[SOURCE_].add(ent.src);  
          columns_[TARG_].add(ent.targ);
          columns_[LABEL_].add(ent.label); 
          columns_[SIGN_].add(ent.sign); 
          columns_[EVIDENCE_].add(ent.evidence);
          if (isForInstance_) {
            columns_[ACTIVITY_].add(ent.activity); 
            columns_[LEVEL_].add(ent.level);
          }
        }
      }
      return;
    }

    //
    // If evidence/sign of one instance changes, all instances change.
    //
    
    public boolean comboEditingDone(int col, int row, Object val) {
      if (!isForInstance_ || forModules_) {
        return (true);
      }
      if (col == SIGN_) {
        return (signComboEditingDone(row, val));
      } else if (col == EVIDENCE_) {
        return (evidenceComboEditingDone(row, val));
      } else {
        return (true);
      }
    }    
    
    //
    // If sign of one instance changes, all instances change.
    //
    
    private boolean signComboEditingDone(int row, Object val) {
      EnumCell newSign = (EnumCell)val;   
      EnumCell currSign = (EnumCell)columns_[SIGN_].get(row);
      if (currSign.value == newSign.value) {
        return (true);
      }
      
      boolean changed = false;
      String instanceID = (String)hiddenColumns_[LINK_ID_].get(row);
      String baseID = GenomeItemInstance.getBaseID(instanceID);
      int size = hiddenColumns_[LINK_ID_].size();
      for (int i = 0; i < size; i++) {        
        String nextId = (String)hiddenColumns_[LINK_ID_].get(i);
        if (nextId.equals(instanceID)) {
          continue;
        }
        String nextBaseId = GenomeItemInstance.getBaseID(nextId);
        if (nextBaseId.equals(baseID)) {
          changed = true;
          columns_[SIGN_].set(i, getEnumForVal(newSign.value, signs_));
        }
      }
      
      if (changed) {
        tmqt_.revalidate();
        tmqt_.repaint();
      }
      return (true);
    }
    
    //
    // If evidence of one instance changes, all instances change.
    //
    
    private boolean evidenceComboEditingDone(int row, Object val) {
      EnumCell newEvidence = (EnumCell)val;   
      EnumCell currEvi = (EnumCell)columns_[EVIDENCE_].get(row);
      if (currEvi.value == newEvidence.value) {
        return (true);
      }      
      boolean changed = false;
      String instanceID = (String)hiddenColumns_[LINK_ID_].get(row);
      String baseID = GenomeItemInstance.getBaseID(instanceID);
      int size = hiddenColumns_[LINK_ID_].size();
      for (int i = 0; i < size; i++) {        
        String nextId = (String)hiddenColumns_[LINK_ID_].get(i);
        if (nextId.equals(instanceID)) {
          continue;
        }
        String nextBaseId = GenomeItemInstance.getBaseID(nextId);
        if (nextBaseId.equals(baseID)) {
          changed = true;
          columns_[EVIDENCE_].set(i, getEnumForVal(newEvidence.value, evidence_));
        }
      }     
      if (changed) {
        tmqt_.revalidate();
        tmqt_.repaint();
      }
      return (true);
    }
    
    //
    // If label of one instance changes, all instances change.
    //  
    
    public boolean textEditingDone(int col, int row, Object val) {
      if (!isForInstance_ || forModules_) {
        return (true);
      }
      if ((val == null) || ((String)val).trim().equals("")) {
        val = "";
      }
      String newName = (String)val;
      boolean changed = false;
      String instanceID = (String)hiddenColumns_[LINK_ID_].get(row);
      String baseID = GenomeItemInstance.getBaseID(instanceID);
      int size = hiddenColumns_[LINK_ID_].size();
      for (int i = 0; i < size; i++) {        
        String nextId = (String)hiddenColumns_[LINK_ID_].get(i);
        if (nextId.equals(instanceID)) {
          continue;
        }
        String nextBaseId = GenomeItemInstance.getBaseID(nextId);
        if (nextBaseId.equals(baseID)) {
          changed = true;
          columns_[LABEL_].set(i, newName);
        }
      }
      
      if (changed) {
        tmqt_.revalidate();
        tmqt_.repaint();
      }
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  private class PerLinkSpecialDrawTableModel extends ReadOnlyTable.TableModel {
    
    final static int SRC       = 0;
    final static int TARG      = 1;
    final static int DRAW_DESC = 2; 
    private final static int NUM_COL_   = 3;   

    private final static int HIDDEN_LINKID_ = 0;
    private final static int NUM_HIDDEN_    = 1;
   
    PerLinkSpecialDrawTableModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"lptable.src",
                                "lptable.targ",
                                "lptable.drawDesc"};
      addHiddenColumns(NUM_HIDDEN_);
    }
          
     String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_LINKID_].get(mapSelectionIndex(selected[0])));
    }  

    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_LINKID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        LayoutTableEntry lte = (LayoutTableEntry)iit.next();
        columns_[SRC].add(lte.source);
        columns_[TARG].add(lte.targ);
        columns_[DRAW_DESC].add(lte.desc);
        hiddenColumns_[HIDDEN_LINKID_].add(lte.linkID);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Used for the selector
  */
 
  private class Selector implements ReadOnlyTable.SelectionHandler {
 
    public void selected(Object obj, int whichTab, int[] whichIndex) {
      if (rot_.selectedRows.length > 1) {
        throw new IllegalArgumentException();
      }
      boolean haveARow = (rot_.selectedRows.length == 1); 
      if (haveARow) {
        PerLinkSpecialDrawTableModel pls = (PerLinkSpecialDrawTableModel)rot_.getModel();
        String linkID = pls.getSelectedKey(rot_.selectedRows);
        boolean gotCustom = linkCustomEvidence(linkID);
        if (gotCustom) {
          haveARow = false;
        }
      }
      rot_.setButtonEnabledState(ReadOnlyTable.EDIT_BUTTON, haveARow);
      rot_.syncButtons();
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Used for tracking button presses
  */

  private class ButtonHand implements ReadOnlyTable.ButtonHandler {    
    public void pressed(int whichButton) {
      switch (whichButton) {
        case ReadOnlyTable.EDIT_BUTTON:
          if (rot_.selectedRows.length > 1) {
            throw new IllegalArgumentException();
          }
          PerLinkSpecialDrawTableModel pls = (PerLinkSpecialDrawTableModel)rot_.getModel();
          String linkID = pls.getSelectedKey(rot_.selectedRows);
          LinkSpecialPropsDialog lpsd = new LinkSpecialPropsDialog(appState_, dacx_, changedProps_, linkID, deletionListeners_);       
          lpsd.setVisible(true);      
          if (lpsd.haveResult()) {
            changedProps_.setDrawStyleForLinkage(linkID, lpsd.getProps());
            displayPropTableProperties();
            pls.fireTableDataChanged();
          }
          break;
        case ReadOnlyTable.ADD_BUTTON:
        case ReadOnlyTable.DELETE_BUTTON:
        default:
          throw new IllegalArgumentException();
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
  ** Apply the data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    Genome genome = dacx_.getGenome();
    boolean haveDynInstance = (genome instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (genome instanceof GenomeInstance) && !haveDynInstance;
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      topTwoLevels = gi.isRootInstance();
    }
    
    if ((!(dacx_.getGenome() instanceof DynamicGenomeInstance)) || forModules_) {
      List linkList = buildModelLinkList();
      ((LinkTableModel)est_.getModel()).extractValues(linkList);
    }
    
    if (topTwoLevels && !forModules_) {
      nps_.displayLinkFreeText();
      nps_.displayLinkURLs();     
    }
    
    SuggestedDrawStyle sds = props_.getDrawStyle();
    sdsPan_.displayProperties(sds.clone(), sds.clone());
    
    if (!forModules_) {
      int dir = props_.getTextDirection();
      ResourceManager rMan = appState_.getRMan();    
      dirCombo_.setSelectedItem(
        new ChoiceContent(rMan.getString("lprop." + BusProperties.mapDirectionToTag(dir)), dir));
    }
    
    displayPropTableProperties();
    return;
  }

  /***************************************************************************
  **
  ** Apply the data values to our property table UI component
  ** 
  */
  
  private void displayPropTableProperties() {
    List layoutLinkList = buildLayoutLinkList();
    rot_.rowElements = layoutLinkList;
    ((PerLinkSpecialDrawTableModel)rot_.getModel()).extractValues(layoutLinkList);
    return;
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the linkage properties
  */
  
  private boolean applyProperties() {

    Genome genome = dacx_.getGenome();
    boolean haveDynInstance = (genome instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (genome instanceof GenomeInstance) && !haveDynInstance;   
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      topTwoLevels = gi.isRootInstance();
    }
    
    
    
    //
    // Check for correctness before continuing:
    //
    
    List tableResults = null;
    if (!haveDynInstance || forModules_) { 
      tableResults = est_.getModel().getValuesFromTable();
      if (!checkTableValues(tableResults)) {
        return (false);
      }
    }

    //
    // Undo/Redo support
    //

    UndoSupport support = new UndoSupport(appState_, "undo.lprop"); 
    
    if (!haveDynInstance || forModules_) {    
      applyTableValues(tableResults, support);
    }
    
    //
    // New text description and URLs
    //
      
    if (topTwoLevels && !forModules_) {
      nps_.installLinkFreeText(support);   
      nps_.installLinkURLs(support);
    }
    
    Layout layout = dacx_.getLayout();

    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    SuggestedDrawStylePanel.QualifiedSDS qsds = sdsPan_.getChosenStyle();
    if (!qsds.isOK) {
      return (false);
    }
    
    // We now clone this at the start, to hold onto per-link changes:
    // LinkProperties changedProps = (LinkProperties)props_.clone(); 
    

    changedProps_.setDrawStyle(qsds.sds);
   
    if (!forModules_) {
      int newDir = ((ChoiceContent)dirCombo_.getSelectedItem()).val;
      changedProps_.setTextDirection(newDir); 
    }
    
    Layout.PropChange[] lpc = new Layout.PropChange[1];    
    
    if (forModules_) {
      NetModuleLinkageProperties nmlp = (NetModuleLinkageProperties)changedProps_;
      lpc[0] = layout.replaceNetModuleLinkageProperties(propKey_, nmlp, nmlp.getOverlayID(), dacx_);
    } else {
      lpc[0] = layout.replaceLinkProperties((BusProperties)props_, (BusProperties)changedProps_);
    }
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);
      props_ = changedProps_;
      changedProps_ = props_.clone();
      LayoutChangeEvent ev = new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev);
      if (doSourceBox_.isSelected()) {
        if (forModules_) {
          applyColorToSourceForModules(support, qsds.sds.getColorName(), dacx_);
        } else {
          applyColorToSource(support, qsds.sds.getColorName());
        }
      }
    }
        
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Apply color value to source node
  */
  
  private void applyColorToSource(UndoSupport support, String colName) {
            
    Set<String> links = dacx_.getLayout().getSharedItems(propKey_);
    Linkage link = getALinkageWeOwn(links, dacx_.getGenome());
    String srcID = link.getSource();
    NodeProperties props = dacx_.getLayout().getNodeProperties(srcID);
    Node node = dacx_.getGenome().getNode(srcID);
 
    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    NodeProperties changedProps = props.clone();
     
    if (node.getNodeType() != Node.INTERCELL) {
      changedProps.setColor(colName);
    } else {
      if (!changedProps.getColorName().equals(colName)) {
        changedProps.setSecondColor(colName);
      }
    }
   
    Layout.PropChange[] lpc = new Layout.PropChange[1];    
    lpc[0] = dacx_.getLayout().replaceNodeProperties(props, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);    
    } 

    return;
  }
  
  /***************************************************************************
  **
  ** Apply color value to source node
  */
  
  private void applyColorToSourceForModules(UndoSupport support, String colName, DataAccessContext rcx) {
    NetModuleLinkageProperties nmlp = (NetModuleLinkageProperties)props_;        
    String ovrKey = nmlp.getOverlayID();
    String modKey = nmlp.getSourceTag();
     
    Layout.PropChange[] lpcs = dacx_.getLayout().applySameColorToModuleAndLinks(ovrKey, modKey, colName, true, propKey_, rcx);
    if (lpcs.length > 0) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpcs);
      support.addEdit(mov);
      support.addEvent(new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
    }
    return;
  }  

  /***************************************************************************
  **
  ** Build the Enum of evidence values
  ** 
  */
  
  private ArrayList<EnumCell> buildEvidenceEnum() {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    StringBuffer buf = new StringBuffer();
    ResourceManager rMan = appState_.getRMan();
    List<String> evi = DBLinkage.linkEvidence();
    Iterator<String> eit = evi.iterator();
    int count = 0;
    while (eit.hasNext()) {
      String val = eit.next();
      buf.setLength(0);
      buf.append("lprop.");
      buf.append(val);
      String disp = rMan.getString(buf.toString());
      retval.add(new EnumCell(disp, val, DBLinkage.mapFromEvidenceTag(val), count++));
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Build the Enum of activity values
  ** 
  */
  
  private ArrayList<EnumCell> buildActivityEnum() { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    Vector<ChoiceContent> choices = LinkageInstance.getActivityChoices(appState_);
    int numChoices = choices.size();
    for (int i = 0; i < numChoices; i++) {
      ChoiceContent cc = choices.get(i);
      retval.add(new EnumCell(cc.name, cc.name, cc.val, i));
    }
    return (retval);
  }
  
    /***************************************************************************
  **
  ** Sign combos depend on model
  ** 
  */
  
  private ArrayList<EnumCell> buildSignEnum(boolean forModules) {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();   
    if (forModules) {
      // Wrap new method into old out-of-date method:
      Vector<ChoiceContent> signs = NetModuleLinkage.getLinkSigns(appState_);
      int numSigns = signs.size();
      for (int i = 0; i < numSigns; i++) {
        ChoiceContent cc = signs.get(i);
        retval.add(new EnumCell(cc.name, cc.name, cc.val, i));
      }
    } else {
      StringBuffer buf = new StringBuffer();
      ResourceManager rMan = appState_.getRMan();
      Set<String> signs = DBLinkage.linkSigns();
      Iterator<String> sit = signs.iterator();
      int count = 0;
      while (sit.hasNext()) {
        String val = sit.next();
        buf.setLength(0);
        buf.append("lprop.");
        buf.append(val);
        String disp = rMan.getString(buf.toString());
        retval.add(new EnumCell(disp, val, DBLinkage.mapFromSignTag(val), count++));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the combo for the desired level
  */
 
  private EnumCell getEnumForVal(int val, List<EnumCell> cells) {  
    Iterator<EnumCell> cit = cells.iterator();
    while (cit.hasNext()) {
      EnumCell elev = cit.next();
      if (elev.value == val) {
        return (new EnumCell(elev));
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the layout table entries
  */
 
  private List buildLayoutLinkList() {  
    if (forModules_) {
      return (fillModuleRowsForLayout());
    } else {
      return (fillRowsForLayout());
    }
  }
  
  /***************************************************************************
  **
  ** Get the model table entries
  */
 
  private List buildModelLinkList() {  
    if (forModules_) {
      return (fillModuleRows());
    } else {
      return (fillRows());
    }
  }

  /***************************************************************************
  **
  ** Fill rows for a model
  */  
  
  private List<LinkModelTableEntry> fillRows() {
    ArrayList<LinkModelTableEntry> retval = new ArrayList<LinkModelTableEntry>();
    ResourceManager rMan = appState_.getRMan();
    HashMap<String, Integer> seenLinks = new HashMap<String, Integer>();

    Iterator<String> lit = myLinks_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = dacx_.getGenome().getLinkage(linkID);
      // Fix for BT-10-05-06:1: Don't show model rows not in a VFN.  Better to
      // ghost those entries?
      if (link == null) {
        continue;
      }
      if (link instanceof LinkageInstance) {
        LinkageInstance li = (LinkageInstance)link;
        String bid = li.getBacking().getID();
        Integer prevRow = seenLinks.get(bid);
        if (prevRow == null) {
          seenLinks.put(bid, new Integer(retval.size())); 
          addLinkageRow(dacx_.getGenome(), link, rMan, -1, retval);
        } else {
          int prow = prevRow.intValue();
          addLinkageRow(dacx_.getGenome(), link, rMan, prow, retval);
          Iterator<String> slit = new HashSet<String>(seenLinks.keySet()).iterator();
          while (slit.hasNext()) {
            String nextID = slit.next();
            Integer rownum = seenLinks.get(nextID);
            int rowVal = rownum.intValue();
            if (rowVal > prow) {
              seenLinks.put(nextID, new Integer(rowVal + 1));
            }
          }
        }
      } else {
        addLinkageRow(dacx_.getGenome(), link, rMan, -1, retval);
      }
    }
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Fill rows for a module
  */  
   
  private List<LinkModelTableEntry> fillModuleRows() {
    ArrayList<LinkModelTableEntry> retval = new ArrayList<LinkModelTableEntry>();
    String ovrKey = ((NetModuleLinkageProperties)props_).getOverlayID();
    NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();   
    NetworkOverlay nov = owner.getNetworkOverlay(ovrKey);
    Iterator<String> lit = myLinks_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      NetModuleLinkage link = nov.getLinkage(linkID);
      addModuleLinkageRow(link, nov, retval);
    }
    return(retval);
  } 
    
  /***************************************************************************
  **
  ** Fill a row for a module
  */
    
  private void addModuleLinkageRow(NetModuleLinkage link, NetworkOverlay nov, List<LinkModelTableEntry> entries) {
    ModuleLinkModelTableEntry ent = new ModuleLinkModelTableEntry();  
    ent.linkID = link.getID();
    String srcID = link.getSource();
    NetModule smod = nov.getModule(srcID);
    ent.src  = smod.getName();

    String targID = link.getTarget();
    NetModule tmod = nov.getModule(targID);
    ent.targ = tmod.getName();
  
    ent.sign = getEnumForVal(link.getSign(), signs_);
    entries.add(ent);
    return;
  }         

  /***************************************************************************
  **
  ** Fill a row for a model
  */
    
  private void addLinkageRow(Genome genome, Linkage link, ResourceManager rMan, int rowNum, List<LinkModelTableEntry> entries) {
    ModelLinkModelTableEntry ent = new ModelLinkModelTableEntry(); 
    if (rowNum == -1) {
      rowNum = entries.size();
    }
    ent.linkID = link.getID();
    String srcID = link.getSource();
    Node src = genome.getNode(srcID);
    ent.src = src.getName();
    if ((ent.src == null) || (ent.src.trim().equals(""))) {
      ent.src = rMan.getString("lptable.unnamed");    
    }  
    String targID = link.getTarget();
    Node targ = genome.getNode(targID);
    String name = targ.getName();
    boolean showType = ((name == null) || (name.trim().equals("")));
    ent.targ = targ.getDisplayString(genome, showType);
    if ((ent.targ == null) || (ent.targ.trim().equals(""))) {
      ent.targ = rMan.getString("lptable.unnamed");    
    }      
    ent.label = link.getName();
    ent.sign = getEnumForVal(link.getSign(), signs_);
    ent.evidence = getEnumForVal(link.getTargetLevel(), evidence_);
    if (isForInstance_) {
      LinkageInstance li = (LinkageInstance)link;
      int activity = li.getActivitySetting();
      ent.activity = getEnumForVal(activity, activities_);
      ent.level = (activity == LinkageInstance.VARIABLE) ? new ProtoDouble(li.getActivityLevel(null))
                                                         : new ProtoDouble("");    
    }
    entries.add(rowNum, ent);
    return;
  }
  
  /***************************************************************************
  **
  ** Check table activity
  */
    
  private boolean tableActivityCheck(List<LinkModelTableEntry> results) {
    if (!isForInstance_) {
      return (true);
    }
    GenomeInstance gi = dacx_.getGenomeAsInstance();
    int count = results.size();
    for (int i = 0; i < count; i++) {
      ModelLinkModelTableEntry ent = (ModelLinkModelTableEntry)results.get(i);
      String linkID = ent.linkID;
      LinkageInstance li = (LinkageInstance)gi.getLinkage(linkID);
      int newActivity = ent.activity.value;
      int oldActivity = li.getActivitySetting();
      double newLevel = ent.level.value;
      double oldLevel = (oldActivity == LinkageInstance.VARIABLE) ? li.getActivityLevel(null) : 0.0;
      if ((newActivity != oldActivity) || 
          ((oldActivity == LinkageInstance.VARIABLE) && (newLevel != oldLevel))) {
        GenomeItemInstance.ActivityTracking act = li.calcActivityBounds(gi);
        if (!nps_.checkActivityBounds(act, newActivity, newLevel)) {
          return (false);
        }
      }
    }
    return (true);
  }    
    
  /***************************************************************************
  **
  ** Check table values
  */
  
  private boolean checkTableValues(List<LinkModelTableEntry> results) {
    if ((!isForInstance_) || forModules_) {
      return (true);
    }
    if (!tableDoubleCheck(results)) {
      return (false);
    }
    return (tableActivityCheck(results));
  }
  
  /***************************************************************************
  **
  ** Check the double values
  */
  
  private boolean tableDoubleCheck(List<LinkModelTableEntry> results) {
    if (!isForInstance_) {
      return (true);
    }     
    int count = results.size();
    for (int i = 0; i < count; i++) {
      ModelLinkModelTableEntry ent = (ModelLinkModelTableEntry)results.get(i);      
      int newActivity = ent.activity.value;
      if (newActivity != LinkageInstance.VARIABLE) {
        continue;
      }
      ProtoDouble num = ent.level;
      if (!num.valid) {
        DoubleEditor.triggerWarning(appState_, appState_.getTopFrame());
        return (false);
      }
      if ((num.value < 0.0) || (num.value > 1.0)) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("lprop.badActivityValue"), 
                                      rMan.getString("lprop.badActivityValueTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Apply table values
  */
  
  private void applyTableValues(List<LinkModelTableEntry> results, UndoSupport support) {
    if (myLinks_ == null) {
      return;
    }
    int count = results.size();
    boolean doEvent = false;
    for (int i = 0; i < count; i++) {
      if (forModules_) {
        ModuleLinkModelTableEntry ent = (ModuleLinkModelTableEntry)results.get(i);
        doEvent |= transferModuleModelValues(support, ent);
      } else {
        ModelLinkModelTableEntry ent = (ModelLinkModelTableEntry)results.get(i);
        doEvent |= transferModelValues(support, ent, dacx_.getGenome());
      }
    }

    if (doEvent) {
      // FIX ME: Actually, all models may change. The backing changes for sure.)
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Apply table values for models
  */
 
  private boolean transferModelValues(UndoSupport support, ModelLinkModelTableEntry ent, Genome genome) {

    boolean retval = false;
    String linkID = ent.linkID;
    Linkage link = genome.getLinkage(linkID);
    String oldName = link.getName();
    int oldEvidence = link.getTargetLevel();
    int oldSign = link.getSign();
    String newName = ent.label;
    newName = (newName == null) ? "" : newName;
    int newEvidence = ent.evidence.value;
    int newSign = ent.sign.value;
    if (!(newName.equals(oldName)) || (oldEvidence != newEvidence) || (oldSign != newSign)) {
      GenomeChange gc = genome.replaceLinkageProperties(linkID, newName, newSign, newEvidence);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
        retval = true;
      }
    }
    if (isForInstance_) {
      LinkageInstance li = (LinkageInstance)link;
      int newActivity = ent.activity.value;
      int oldActivity = li.getActivitySetting();
      double newLevel = ent.level.value;
      double oldLevel = (oldActivity == LinkageInstance.VARIABLE) ? li.getActivityLevel(null) : 0.0;
      if ((newActivity != oldActivity) || 
          ((oldActivity == LinkageInstance.VARIABLE) && (newLevel != oldLevel))) {
        GenomeInstance gi = (GenomeInstance)genome;
        GenomeChange gc = gi.replaceLinkageInstanceActivity(linkID, newActivity, newLevel);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
          retval = true;
        }          
      }
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply table values for modules
  */

  private boolean transferModuleModelValues(UndoSupport support, ModuleLinkModelTableEntry ent) {
    boolean retval = false;      
    String ovrKey = ((NetModuleLinkageProperties)props_).getOverlayID();
    NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();
    NetworkOverlay nov = owner.getNetworkOverlay(ovrKey);
    String linkID = ent.linkID;
    NetModuleLinkage link = nov.getLinkage(linkID);
    int oldSign = link.getSign();
    int newSign = ent.sign.value;
    if (oldSign != newSign) {
      NetworkOverlayChange noc = owner.modifyNetModuleLinkage(ovrKey, linkID, newSign);
      if (noc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, dacx_, noc);
        support.addEdit(gcc);
        retval = true;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get layout table entries
  */
  
  private List fillRowsForLayout() {
    ArrayList retval = new ArrayList();
    ResourceManager rMan = appState_.getRMan();
    HashMap<String, Integer> seenLinks = new HashMap<String, Integer>();

    Iterator<String> lit = myLinks_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = dacx_.getGenome().getLinkage(linkID);
      // Fix for BT-10-05-06:1: Don't show model rows not in a VFN.  Better to
      // ghost those entries?
      if (link == null) {
        continue;
      }
      if (link instanceof LinkageInstance) {
        LinkageInstance li = (LinkageInstance)link;
        String bid = li.getBacking().getID();
        Integer prevRow = seenLinks.get(bid);
        if (prevRow == null) {
          seenLinks.put(bid, new Integer(retval.size()));            
          addLinkageRowForLayout(dacx_.getGenome(), link, rMan, -1, retval);
        } else {
          int prow = prevRow.intValue();
          addLinkageRowForLayout(dacx_.getGenome(), link, rMan, prow, retval);
          Iterator<String> slit = new HashSet<String>(seenLinks.keySet()).iterator();
          while (slit.hasNext()) {
            String nextID = slit.next();
            Integer rownum = seenLinks.get(nextID);
            int rowVal = rownum.intValue();
            if (rowVal > prow) {
              seenLinks.put(nextID, new Integer(rowVal + 1));
            }
          }        
        }
      } else {
        addLinkageRowForLayout(dacx_.getGenome(), link, rMan, -1, retval);
      }
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Get layout table row
  */
    
  private void addLinkageRowForLayout(Genome genome, Linkage link, ResourceManager rMan, int rowNum, List entries) {
    LayoutTableEntry lte = new LayoutTableEntry();  
    if (rowNum == -1) {
      rowNum = entries.size();
    }
    lte.linkID = link.getID();
    String srcID = link.getSource();
    Node src = genome.getNode(srcID);
    lte.source = src.getName();
    if ((lte.source == null) || (lte.source.trim().equals(""))) {
      lte.source = rMan.getString("lptable.unnamed");    
    }  
    String targID = link.getTarget();
    Node targ = genome.getNode(targID);
    String name = targ.getName();
    boolean showType = ((name == null) || (name.trim().equals("")));
    lte.targ = targ.getDisplayString(genome, showType);
    if ((lte.targ == null) || (lte.targ.trim().equals(""))) {
      lte.targ = rMan.getString("lptable.unnamed");    
    }  
    lte.desc = generateDisplayString(lte.linkID, rMan);
    entries.add(rowNum, lte);
    return;
  } 
    
  /***************************************************************************
  **
  ** Get layout table entries
  */
    
  private List<LayoutTableEntry> fillModuleRowsForLayout() {
    ArrayList<LayoutTableEntry> retval = new ArrayList<LayoutTableEntry>();
    String ovrKey = ((NetModuleLinkageProperties)props_).getOverlayID();
    NetOverlayOwner owner = dacx_.getCurrentOverlayOwner();
    NetworkOverlay nov = owner.getNetworkOverlay(ovrKey);
    Iterator<String> lit = myLinks_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      NetModuleLinkage link = nov.getLinkage(linkID);
      addModuleLinkageRowForLayout(link, nov, dacx_.rMan, retval);
    }
    return (retval);
  } 
    
  /***************************************************************************
  **
  ** Get layout table row
  */ 
    
  private void addModuleLinkageRowForLayout(NetModuleLinkage link, NetworkOverlay nov, ResourceManager rMan, List<LayoutTableEntry> entries) {
    LayoutTableEntry lte = new LayoutTableEntry();  
    lte.linkID = link.getID();
    String srcID = link.getSource();
    NetModule smod = nov.getModule(srcID);
    lte.source = smod.getName();
    String targID = link.getTarget();
    NetModule tmod = nov.getModule(targID);
    lte.targ = tmod.getName();
    lte.desc = generateDisplayString(link.getID(), rMan);   
    entries.add(lte);
    return;
  }
   
  /***************************************************************************
  **
  ** Generate the display description string
  */
 
  private String generateDisplayString(String linkID, ResourceManager rMan) {
    boolean gotCustom = linkCustomEvidence(linkID);
    PerLinkDrawStyle plds = changedProps_.getDrawStyleForLinkage(linkID);
    if (gotCustom) {
      return (rMan.getString("lptable.evidenceOverride")); 
    } else if (plds == null) {
      return (rMan.getString("lptable.noSpecLinkSty")); 
    } else {
      return (plds.getDisplayString(appState_.getRMan(), dacx_.cRes));
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used for the layout props table
  */

  private class LayoutTableEntry {  
    String linkID;
    String source;
    String targ;
    String desc;
  }

  interface LinkModelTableEntry {
    
  }
  
  /***************************************************************************
  **
  ** Used for transfer to data table
  */
  
  class ModuleLinkModelTableEntry implements LinkModelTableEntry {
    String linkID;
    String src;
    String targ;
    EnumCell sign;
  }
  
  /***************************************************************************
  **
  ** Used for transfer to data table
  */
  
  class ModelLinkModelTableEntry implements LinkModelTableEntry {
    String linkID;
    String src;
    String targ;
    String label;
    EnumCell sign;
    EnumCell evidence;
    EnumCell activity;
    ProtoDouble level;
  }
}
