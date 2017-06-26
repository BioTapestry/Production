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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.TreeNodeChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.ui.ModelImagePanel;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.ImageHighlighter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing the targets of a group color map
*/

public class GroupColorMapDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ReadOnlyTable est_;
  private UIComponentSource uics_;
  private TimeAxisDefinition tad_;
  private GenomeSource gs_;
  private DataAccessContext dacx_;
  private StaticDataAccessContext rcxR_;
  private UndoFactory uFac_;
  private List<Color> colOrder_;
  private Map<Color, NavTree.GroupNodeMapEntry> currMap_;
  private HashMap<String, NavTree.GroupNodeMapEntry> fixMap_;
  private ColorMapTableModel.TableRow selectedEntry_;
  private Integer selectedRow_; // may be null
  private JComboBox modelChoices_;
  private JComboBox regionChoices_;
  private JComboBox timeChoices_;
  private Vector<TrueObjChoiceContent> modTocc_;
  private Vector<TrueObjChoiceContent> regTocc_;
  private Vector<TrueObjChoiceContent> timTocc_;
  private JLabel pathLabel_;
  private BufferedImage mapImg_;
  private boolean installing_;
  private TreeNode treeNode_;
  private NavTree nt_;
  private String pathFormat_;
  private String pathNone_; 
  private ModelImagePanel myPanel_;

  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor. Needs casting work! List of mappedIDs is either <TimeCourseDataMaps.TCMapping> OR <String>
  */ 
  
  public GroupColorMapDialog(UIComponentSource uics, DataAccessContext dacx, 
                             Map<Color, NavTree.GroupNodeMapEntry> currMap, 
                             BufferedImage mapImg, NavTree nt, TreeNode treeNode, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("grpColMapEdit.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    mapImg_ = mapImg;
    rcxR_ = new StaticDataAccessContext(dacx).getContextForRoot();
    tad_ = dacx.getExpDataSrc().getTimeAxisDefinition();
    String displayUnits = tad_.unitDisplayString();
    gs_ = dacx.getGenomeSource();
    nt_ = nt;
    treeNode_ = treeNode;
    currMap_ = new HashMap<Color, NavTree.GroupNodeMapEntry>();
    fixMap_ = new HashMap<String, NavTree.GroupNodeMapEntry>();
    if ((currMap == null) || currMap.isEmpty()) {
      Set<Color> cols = ImageHighlighter.collectColors(mapImg);
      cols.remove(Color.WHITE);
      colOrder_ = colorSorter(cols);
      for (Color col : cols) {
        currMap_.put(col, new NavTree.GroupNodeMapEntry(col));     
      }     
    } else {
      for (Color key : currMap.keySet()) {
        NavTree.GroupNodeMapEntry gnme = currMap.get(key);
        NavTree.GroupNodeMapEntry gnmeCl = gnme.clone();
        currMap_.put(key, gnmeCl);     
      }
      colOrder_ = colorSorter(currMap_.keySet());
    }
    
    setSize(850, 750);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    
    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    
    modTocc_ = buildModelChoices();
    regTocc_ = buildRegionChoices(null, null, null);
    timTocc_ = buildTimeChoices(null);
     
    JPanel tpan = buildATable(uics_.getRMan(), displayUnits);
    int row = ds.addTable(cp, tpan, 6, 0, 10);  
       
    pathFormat_ = uics_.getRMan().getString("grpColMapEdit.selPath");
    String none = uics_.getRMan().getString("grpColMapEdit.selPathNone");
    pathNone_ = MessageFormat.format(pathFormat_, new Object[] {none});  
     
    pathLabel_ = new JLabel(pathNone_);  
    row = ds.addWidgetFullRow(cp, pathLabel_, true, row, 10);
 
    JPanel epan = buildEntryPanel(uics_.getRMan(), gbc);
    row = ds.addTable(cp, epan, 4, row, 10);
    
    ds.buildAndInstallButtonBox(cp, row, 10, false, true); 
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (applyProperties(dacx_)) {
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

  /***************************************************************************
  **
  ** Build the table
  */ 
  
  private JPanel buildATable(ResourceManager rMan, String displayUnits) {   

    //
    // Build the tables:
    //

    est_ = new ReadOnlyTable(uics_, new ColorMapTableModel(uics_, displayUnits), new Selector());   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.NO_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString("grpColMapEdit.assignColorToTarget");
    tp.titleFont = null;
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.COLOR, 100, 150, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.TARGET_MODEL, 100, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.REGION, 100, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.TIME, 50, 50, Integer.MAX_VALUE));     
    tp.colWidths = colWidths;
    tp.canMultiSelect = false;
    JPanel tabPan = est_.buildReadOnlyTable(tp);
    DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer)est_.getTable().getDefaultRenderer(DisplayColor.class);
    est_.getTable().setDefaultRenderer(DisplayColor.class, new ColorBlockRenderer(dtcr, uics_));

    return (tabPan);
  }
  
  /***************************************************************************
  **
  ** Stock the entry controls
  */ 
  
  private JPanel buildEntryPanel(ResourceManager rMan, GridBagConstraints gbc) {       
 
    modelChoices_ = new JComboBox(modTocc_);
    modelChoices_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          setTheModel();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    modelChoices_.setEnabled(false);
    
    regionChoices_ = new JComboBox(regTocc_);
    regionChoices_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          setTheRegion();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    regionChoices_.setEnabled(false);
    
    timeChoices_ = new JComboBox(timTocc_);
    timeChoices_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          setTheTime();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    timeChoices_.setEnabled(false);

    JPanel ctrlPanel = new JPanel();
    ctrlPanel.setLayout(new GridBagLayout()); 
    int row = 0;
    JLabel label = null;

    label = new JLabel(rMan.getString("grpColMapEdit.model"));          
    UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    ctrlPanel.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, row++, 2, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    ctrlPanel.add(modelChoices_, gbc);    

    label = new JLabel(rMan.getString("grpColMapEdit.region"));
    UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    ctrlPanel.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, row++, 2, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    ctrlPanel.add(regionChoices_, gbc);    
    
    label = new JLabel(rMan.getString("grpColMapEdit.time"));
    UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    ctrlPanel.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, row++, 2, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    ctrlPanel.add(timeChoices_, gbc);   
        
    JPanel retPanel = new JPanel();
    retPanel.setLayout(new GridBagLayout()); 
       
    myPanel_ = new ModelImagePanel(uics_);
    myPanel_.setImage(null);
     
    FixedMinPanel fmp = new FixedMinPanel();
    fmp.setLayout(new GridLayout(1, 1));
    fmp.add(myPanel_.getPanel());
 
    fmp.setBackground(Color.red);
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retPanel.add(fmp, gbc);
    ctrlPanel.setBackground(Color.green);
    UiUtil.gbcSet(gbc, 1, 0, 3, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    retPanel.add(ctrlPanel, gbc);  
    retPanel.setBackground(Color.blue);
    return (retPanel);  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Handle selections
  */ 
    
  private class Selector implements ReadOnlyTable.SelectionHandler {
 
    public void selected(Object obj, int whichTab, int[] whichIndex) {
      if (installing_) {
        return;
      }
      installing_ = true;
      selectedEntry_ = (ColorMapTableModel.TableRow)obj;
      ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
      selectedRow_ = (whichIndex.length > 0) ? Integer.valueOf(ecdtm.mapSelectionIndex(whichIndex[0])) : null;
      goAndSelect();
      installing_ = false;
      return;
    }
  } 
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class ColorMapTableModel extends ReadOnlyTable.TableModel {
    
    final static int COLOR        = 0;
    final static int TARGET_MODEL = 1;
    final static int REGION       = 2;
    final static int TIME         = 3;   
    private final static int NUM_COL_ = 4; 
    
    private final static int HIDDEN_CAND_ID = 0;
    private final static int NUM_HIDDEN_    = 1;
    
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      DisplayColor color;
      String targetModel;
      String region;
      String time;
      String hiddenCandID;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        color = (DisplayColor)columns_[COLOR].get(i); 
        targetModel = (String)columns_[TARGET_MODEL].get(i);
        region = (String)columns_[REGION].get(i);
        time = (String)columns_[TIME].get(i);
        hiddenCandID = (String)hiddenColumns_[HIDDEN_CAND_ID].get(i);
      }
      
      void toCols() {
        columns_[COLOR].add(color);
        columns_[TARGET_MODEL].add(targetModel);
        columns_[REGION].add(region);
        columns_[TIME].add(time);
        hiddenColumns_[HIDDEN_CAND_ID].add(hiddenCandID);      
        return;
      }
      
      void replaceCols(int row) {
        columns_[COLOR].set(row, color);
        columns_[TARGET_MODEL].set(row, targetModel);
        columns_[REGION].set(row, region);
        columns_[TIME].set(row, time);
        hiddenColumns_[HIDDEN_CAND_ID].set(row, hiddenCandID);      
        return;
      }   
    }
  
    ColorMapTableModel(UIComponentSource uics, String displayUnits) {
      super(uics, NUM_COL_);

      ;
      ResourceManager rMan = uics_.getRMan();
      String timeColumnHeading = MessageFormat.format(rMan.getString("colMapEntry.timeColFormat"), new Object[] {displayUnits});

      colClasses_ = new Class[] {
          DisplayColor.class,
          String.class,
          String.class,
          String.class};
      
      colNames_ = new String[] {"colMapEntry.color",
                                "colMapEntry.model",
                                "colMapEntry.region", 
      		                      timeColumnHeading};
      addHiddenColumns(NUM_HIDDEN_);
    }
    
    List<TableRow> getValuesFromTable() {
      ArrayList<TableRow> retval = new ArrayList<TableRow>();
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
  ** Update the path display
  */
  
  private void updatePathDisplay() {
    ModelOrProxy choice = (ModelOrProxy)((TrueObjChoiceContent)modelChoices_.getSelectedItem()).val;
    String pathToShow = (choice == null) ? pathNone_ :MessageFormat.format(pathFormat_, new Object[] {choice.displayPath});
    pathLabel_.setText(pathToShow);
    pathLabel_.invalidate();
    pathLabel_.revalidate();
    return;
  }
 
  /***************************************************************************
  **
  ** Update Time And Regions for JCombos based on current model selection. Assumes
  ** installing_ is set to true. This is not touching the table entries.
  */
  
  private void updateTimeAndRegionsCombos() {
    if (!installing_) {
      throw new IllegalStateException();
    }
 
    ModelOrProxy choice = (ModelOrProxy)((TrueObjChoiceContent)modelChoices_.getSelectedItem()).val; 
  
    if (choice == null) {
      timTocc_ = buildTimeChoices(null);
      regTocc_ = buildRegionChoices(null, null, null);
    } else if (choice.proxyID != null) {       
      DynamicInstanceProxy dip = gs_.getDynamicProxy(choice.proxyID);
      timeChoices_.setEnabled(!dip.isSingle());
      Iterator<Group> grit = dip.getGroupIterator();
      regTocc_ = buildRegionChoices(grit, dip.getAnInstance(), dip);
      regionChoices_.setEnabled(true);
      timTocc_ = buildTimeChoices(dip);
    } else {
      timeChoices_.setEnabled(false);
      Genome model = gs_.getGenome(choice.modelID);
      if (model instanceof GenomeInstance) {
        regionChoices_.setEnabled(true);
        GenomeInstance gi = (GenomeInstance)model;
        Iterator<Group> grit = gi.getGroupIterator();
        regTocc_ = buildRegionChoices(grit, gi, null);
        regionChoices_.setEnabled(true);
      } else { // Root model
        regionChoices_.setEnabled(false);
        regTocc_ = buildRegionChoices(null, null, null);
      }
      timTocc_ = buildTimeChoices(null);
    }

    UiUtil.replaceComboItems(regionChoices_, regTocc_);
    UiUtil.replaceComboItems(timeChoices_, timTocc_);
    return;
  }

  /***************************************************************************
  **
  ** Set the time from the JCombo setting
  */
  
  private void setTheTime() {  
    if (installing_) {
      return;
    }
    installing_ = true;
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)timeChoices_.getSelectedItem();
    Integer choice = (Integer)tocc.val;
    ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
    selectedEntry_.time = (tocc.val == null) ? "" : tocc.name;
    // This is the same object as in the currMap_:
    NavTree.GroupNodeMapEntry gnme = fixMap_.get(selectedEntry_.hiddenCandID);
    gnme.proxyTime = choice;
    selectedEntry_.replaceCols(selectedRow_.intValue());
    ecdtm.fireTableRowsUpdated(selectedRow_.intValue(), selectedRow_.intValue());
    installing_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the region from the JCombo setting
  */
  
  private void setTheRegion() {   
    if (installing_) {
      return;
    }
    installing_ = true;
    String groupID = (String)((TrueObjChoiceContent)regionChoices_.getSelectedItem()).val;
    ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
    // This is the same object as in the currMap_:
    NavTree.GroupNodeMapEntry gnme = fixMap_.get(selectedEntry_.hiddenCandID);
    gnme.regionID = groupID;
    selectedEntry_.region = getRegionNameForEntry(gnme, gs_);  
    selectedEntry_.replaceCols(selectedRow_.intValue());
    ecdtm.fireTableRowsUpdated(selectedRow_.intValue(), selectedRow_.intValue());
    installing_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the model, from the JCombo. Note that setting the model will null out the region and
  ** time guys as well, and update the JCombos.
  */
  
  private void setTheModel() {  
    if (installing_) {
      return;
    }
    installing_ = true;
    updateTimeAndRegionsCombos();
    updatePathDisplay();
    
    NavTree.GroupNodeMapEntry gnme = fixMap_.get(selectedEntry_.hiddenCandID);
    ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
    ModelOrProxy choice = (ModelOrProxy)((TrueObjChoiceContent)modelChoices_.getSelectedItem()).val;
    
    if (choice == null) {
      if ((gnme.modelID == null) && (gnme.proxyID == null)) {
        installing_ = false;
        return;
      } 
      selectedEntry_.targetModel = "";
      // Model changed? Toss everything else...
      selectedEntry_.region = "";
      selectedEntry_.time = "";
      gnme.modelID = null;
      gnme.proxyID = null;
      gnme.regionID = null;
      gnme.proxyTime = null;
    } else if (choice.proxyID != null) {
      // NO CHANGE? Do nothing.....
      if (choice.proxyID.equals(gnme.proxyID)) {
        installing_ = false;
        return;
      }      
      DynamicInstanceProxy dip = gs_.getDynamicProxy(choice.proxyID);
      if (dip.isSingle()) {
        List<String> newNodes = dip.getProxiedKeys();
        if (newNodes.size() != 1) {
          throw new IllegalStateException();
        }
        String key1 = newNodes.iterator().next();
        selectedEntry_.targetModel = dip.getProxiedInstanceName(key1);
      } else {
        selectedEntry_.targetModel = dip.getName();
      }
      // Model changed? Toss everything else...
      gnme.modelID = null;
      gnme.proxyID = choice.proxyID;
      gnme.regionID = null;
      gnme.proxyTime = null;
      selectedEntry_.region = getRegionNameForEntry(gnme, gs_);
      selectedEntry_.time = (timTocc_.get(0).val == null) ? "" : timTocc_.get(0).name;
    } else if (choice.modelID != null) {
      if (choice.modelID.equals(gnme.modelID)) {
        installing_ = false;
        return;
      } 
      Genome gen = gs_.getGenome(choice.modelID);
      selectedEntry_.targetModel = gen.getName();
      // Model changed? Toss everything else...
      selectedEntry_.time = "";
      gnme.proxyID = null;
      gnme.modelID = choice.modelID;
      gnme.regionID = null;
      gnme.proxyTime = null;
      selectedEntry_.region = getRegionNameForEntry(gnme, gs_);
    }
    selectedEntry_.replaceCols(selectedRow_.intValue());
    ecdtm.fireTableRowsUpdated(selectedRow_.intValue(), selectedRow_.intValue());
    installing_ = false;
    return;
  } 

  /***************************************************************************
  **
  ** Pass the table selection to the search panel
  */
  
  private void goAndSelect() {
  
    //
    // If we have no selection, we need to provide a basic disabled state:
    //
    
    NavTree.GroupNodeMapEntry gnme = (selectedEntry_ == null) ? null : fixMap_.get(selectedEntry_.hiddenCandID);
 
    if (selectedEntry_ == null) {
      myPanel_.setImage(mapImg_);
    } else {
      BufferedImage bim = ImageHighlighter.buildMaskedImageBandW(mapImg_, mapImg_, gnme.color.getRed(), gnme.color.getGreen(), gnme.color.getBlue(), 0.6F);
      myPanel_.setImage(bim);
    }

    installing_ = true;
    
    //
    // Get the model installed:
    //
    
    installModelFromEntry(gnme);

    //
    // With model installed, we can update the path display and change the contents of the 
    // Time and region combos:
    //
    
    updatePathDisplay();
    updateTimeAndRegionsCombos();

    //
    // With time and region changed to match model, we can now set them to the correct vals:
    //
    
    installRegionFromEntry(gnme);
    installTimeFromEntry(gnme);
    
    //
    // Finally, set the enabled/disabled state:
    //
    
    setComboEnables(gnme);
    
    installing_ = false;
    
    return;
  }
  
  /***************************************************************************
  **
  ** Set enabed/disabled state of combo boxes
  ** 
  */
  
  private void setComboEnables(NavTree.GroupNodeMapEntry gnme) {
    
    //
    // If nothing in the table is selected, everybody is disabled:
    //
    
    if (selectedEntry_ == null) {
      modelChoices_.setEnabled(false);
      regionChoices_.setEnabled(false);
      timeChoices_.setEnabled(false);
      return;
    }
    
    //
    // If something is selected, model is always available:
    //
       
    modelChoices_.setEnabled(true);
 
    //
    // If no model (or proxy) is chosen, other combos are disabled:
    //
    
    if ((gnme.modelID == null) && (gnme.proxyID == null)) {    
      regionChoices_.setEnabled(false);
      timeChoices_.setEnabled(false);
      return;
    }
    
    //
    // If we have a full genome, region and time are both as well
    //
    
    if (gnme.modelID != null) {
      Genome gen = gs_.getGenome(gnme.modelID);
      if (gen instanceof DBGenome) {
        regionChoices_.setEnabled(false);
        timeChoices_.setEnabled(false);
        return;
      }
    }
    
    //
    // No DBGenome, we have regions. If we have an hourly proxy, we enable time
    //
    
    regionChoices_.setEnabled(true);
 
    if (gnme.proxyID != null) {
      DynamicInstanceProxy dip = gs_.getDynamicProxy(gnme.proxyID);
      timeChoices_.setEnabled(!dip.isSingle());
    } else {
      timeChoices_.setEnabled(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Set model choice based on MapEntry. Assumes installing_ is true.
  ** 
  */
  
  private void installModelFromEntry(NavTree.GroupNodeMapEntry gnme) {
    
    if (gnme == null) {
      modelChoices_.setSelectedIndex(0);
      return;
    }

    for (TrueObjChoiceContent tocc : modTocc_) {
      ModelOrProxy mop = (ModelOrProxy)tocc.val;
      if (mop == null) { 
        if ((gnme.modelID == null) && (gnme.proxyID == null)) {
          modelChoices_.setSelectedItem(tocc);
          break;
        }
      } else if (((gnme.modelID != null) && gnme.modelID.equals(mop.modelID)) ||
                 ((gnme.proxyID != null) && gnme.proxyID.equals(mop.proxyID))) {
        modelChoices_.setSelectedItem(tocc);
        break;
      }
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Set region choice based on MapEntry. Assumes installing_ is true.
  ** 
  */
  
  private void installRegionFromEntry(NavTree.GroupNodeMapEntry gnme) {
    
    if (gnme == null) {
      regionChoices_.setSelectedIndex(0);
      return;
    }   
    
    for (TrueObjChoiceContent tocc : regTocc_) {
      if (tocc.val == null) { 
        if (gnme.regionID == null) {
          regionChoices_.setSelectedItem(tocc);
          break;
        }
      } else if (tocc.val.equals(gnme.regionID)) {
        regionChoices_.setSelectedItem(tocc);
        break;
      }
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Set model choice based on MapEntry. Assumes installing_ is true.
  ** 
  */
  
  private void installTimeFromEntry(NavTree.GroupNodeMapEntry gnme) {
    
    if (gnme == null) {
      timeChoices_.setSelectedIndex(0);
      return;
    }  
      
    for (TrueObjChoiceContent tocc : timTocc_) {
      if (tocc.val == null) { 
        if (gnme.proxyTime == null) {
          timeChoices_.setSelectedItem(tocc);
          break;
        }
      } else if (tocc.val.equals(gnme.proxyTime)) {
        timeChoices_.setSelectedItem(tocc);
        break;
      }
    }
    return;
  }
  

  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    installing_ = true;
    List<ColorMapTableModel.TableRow> entries = buildTableRows(colOrder_, currMap_, fixMap_);
    est_.rowElements = entries;   
    est_.getModel().extractValues(entries);
    myPanel_.setImage(mapImg_);
    installing_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Build table rows
  */ 
  
  private List<ColorMapTableModel.TableRow> buildTableRows(List<Color> colOrder, Map<Color, NavTree.GroupNodeMapEntry> modMap, Map<String, NavTree.GroupNodeMapEntry> fixMap) {
    ColorMapTableModel tctm = (ColorMapTableModel)est_.getModel();
    ArrayList<ColorMapTableModel.TableRow> retval = new ArrayList<ColorMapTableModel.TableRow>();
    
    int count = 0;
    for (Color key: colOrder) {
      NavTree.GroupNodeMapEntry entry = modMap.get(key); 
      ColorMapTableModel.TableRow tr = tctm.new TableRow();  
      
      tr.color = new DisplayColor(key, uics_.getRMan());
      tr.region = "";
      tr.time = "";
      tr.targetModel = "";

      if (entry.proxyID != null) {  
        DynamicInstanceProxy dip = gs_.getDynamicProxy(entry.proxyID);
        if (dip.isSingle()) {
          List<String> newNodes = dip.getProxiedKeys();
          if (newNodes.size() != 1) {
            throw new IllegalStateException();
          }
          String key1 = newNodes.iterator().next();
          tr.targetModel = dip.getProxiedInstanceName(key1);
          tr.time = "";
        } else {
          tr.targetModel = dip.getName();
          tr.time = timeValToString(tad_, entry.proxyTime.intValue(), uics_.getRMan());
        }
      } else if (entry.modelID != null) {
        Genome gen = gs_.getGenome(entry.modelID);
        tr.targetModel = gen.getName();
      }
      tr.region = getRegionNameForEntry(entry, gs_);
      tr.hiddenCandID = Integer.toString(count++);
      fixMap.put(tr.hiddenCandID, entry);     
      retval.add(tr);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the region name for the map entry to use in the table
  ** 
  */
  
  private String getRegionNameForEntry(NavTree.GroupNodeMapEntry entry, GenomeSource gs) {
    GenomeInstance gi = null;
    if (entry.proxyID != null) {       
      DynamicInstanceProxy dip = gs.getDynamicProxy(entry.proxyID);
      gi = dip.getAnInstance();
    } else if (entry.modelID != null) {
      Genome gen = gs.getGenome(entry.modelID);
      if (gen instanceof GenomeInstance) {
        gi = (GenomeInstance)gen;
      }
    }
    ResourceManager rMan = uics_.getRMan();
    String retval;
    if (gi == null) {
      retval = ""; 
    } else if (entry.regionID == null) {
      retval = rMan.getString("colMapEntry.allRegions");
    } else {
      Group grp = gi.getGroup(entry.regionID);
      retval = grp.getInheritedDisplayName(gi);
    }
    return (retval); 
  }
  
  
  /***************************************************************************
  **
  ** Apply our UI values to the map 
  ** 
  */
  
  private boolean applyProperties(DataAccessContext dacx) {
    
    UndoSupport support = uFac_.provideUndoSupport("undo.setGroupNavMap", dacx);
    
    Map<Color, NavTree.GroupNodeMapEntry> newMap = new HashMap<Color, NavTree.GroupNodeMapEntry>();
    Iterator<NavTree.GroupNodeMapEntry> trit = fixMap_.values().iterator();
    while (trit.hasNext()) {        
      NavTree.GroupNodeMapEntry tr = trit.next();
      newMap.put(tr.color, tr);
    }
    
    NavTreeChange ntc = nt_.installGroupModelMap(treeNode_, newMap);
    support.addEdit(new NavTreeChangeCmd(dacx, ntc));
    NavTree.NodeID nodeKey = new NavTree.NodeID(nt_.getGroupNodeID(treeNode_));
    support.addEvent(new TreeNodeChangeEvent(nodeKey, TreeNodeChangeEvent.Change.GROUP_NODE_CHANGE));
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Build the Model Choices
  ** 
  */
  
  private Vector<TrueObjChoiceContent> buildModelChoices() { 
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>(); 
    retval.add(new TrueObjChoiceContent(uics_.getRMan().getString("grpColMatEdit.noDest"), null));
    NavTree navTree = gs_.getModelHierarchy();   
    List<String> pol = navTree.getFullTreePreorderListing(rcxR_);
    Iterator<String> it = pol.iterator();
    int count = 0;
    while (it.hasNext()) {
      String id = it.next();
      Genome gen = gs_.getGenome(id);
      String modID = null;
      String proxID = null;
      List<String> names = gen.getNamesToRoot();
      if (gen instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)gen;
        proxID = dgi.getProxyID();        
        DynamicInstanceProxy dprox = gs_.getDynamicProxy(proxID);
        if (!dprox.isSingle()) {
          names.set(names.size() - 1, dprox.getName());
        }
      } else {
        modID = id;
      }
      String display = DataUtil.pathToString(names);
      retval.add(new TrueObjChoiceContent(names.get(names.size() - 1), new ModelOrProxy(count, modID, proxID, display)));
      count++;
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Build regions
  */
  
  private Vector<TrueObjChoiceContent> buildRegionChoices(Iterator<Group> grit, GenomeInstance gi, DynamicInstanceProxy dip) { 
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
    if (grit == null) {
      retval.add(new TrueObjChoiceContent(uics_.getRMan().getString("grpColMatEdit.noRegions"), null));
    } else {
      retval.add(new TrueObjChoiceContent(uics_.getRMan().getString("grpColMatEdit.allRegions"), null));
      while (grit.hasNext()) {
        Group grp = grit.next();
        if (((dip == null) && grp.isASubset(gi)) || ((dip != null) && grp.isASubset(dip))) {
          continue;
        }
        retval.add(new TrueObjChoiceContent(grp.getInheritedDisplayName(gi), grp.getID()));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
   **
   ** Build time string
   */
   
   public static String timeValToString(TimeAxisDefinition tad, int val, ResourceManager rMan) { 

     boolean namedStages = tad.haveNamedStages();
     String displayUnits = tad.unitDisplayString();       
     String formatKey = (tad.unitsAreASuffix()) ? "grpColMatEdit.nameFormat" : "grpColMatEdit.nameFormatPrefix";
     String format = rMan.getString(formatKey);
     String stageName = (namedStages) ? tad.getNamedStageForIndex(val).name : Integer.toString(val);
     String dispName = MessageFormat.format(format, new Object[] {stageName, displayUnits});
     return (dispName);
   }  

  /***************************************************************************
  **
  ** Build Times
  */
  
  private Vector<TrueObjChoiceContent> buildTimeChoices(DynamicInstanceProxy dip) { 
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>(); 
    if ((dip == null) || dip.isSingle()) {
      retval.add(new TrueObjChoiceContent(uics_.getRMan().getString("grpColMatEdit.noTimes"), null));    
    } else {
      int min = dip.getMinimumTime();
      int max = dip.getMaximumTime();
      for (int i = min; i <= max; i++) {
        String dispName = timeValToString(tad_, i, uics_.getRMan());
        retval.add(new TrueObjChoiceContent(dispName, Integer.valueOf(i)));
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Provide a sorted list of the colors
  */
  
  private List<Color> colorSorter(Set<Color> cols) {
    int count = 0;
    ArrayList<NamedColor> preRetval = new ArrayList<NamedColor>();
    for (Color col : cols) {
      String nameAndKey = Integer.toString(count++);
      NamedColor nc = new NamedColor(nameAndKey, col, nameAndKey);
      preRetval.add(nc); 
    }
    Collections.sort(preRetval);
    ArrayList<Color> retval = new ArrayList<Color>();
    for (NamedColor ncol : preRetval) {
      retval.add(ncol.getColor());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Used for TOCC object
  */  
  
  public static class ModelOrProxy implements Comparable<ModelOrProxy>, Cloneable {
    int order;
    String modelID;
    String proxyID;
    String displayPath;
    
    public ModelOrProxy(int order, String modelID, String proxyID, String displayPath) {
      this.order = order;
      this.modelID = modelID;
      this.proxyID = proxyID;
      this.displayPath = displayPath;
    }
  
    public int compareTo(ModelOrProxy other) { 
      if (this == other) {
        return (0);
      }   
      return (this.order - other.order);   
    }
    
    @Override
    public ModelOrProxy clone() {
      try {
        ModelOrProxy newVal = (ModelOrProxy)super.clone();
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }    
    }
  
    @Override
    public int hashCode() {
      return (order);
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof ModelOrProxy)) {
        return (false);
      }
      ModelOrProxy otherMP = (ModelOrProxy)other;     
      if (this.order != otherMP.order) {
        return (false);
      }
      if (!this.displayPath.equals(otherMP.displayPath)) {
        return (false);
      }
      
      if (this.modelID != null) {
        return (this.modelID.equals(otherMP.modelID));
      }
      if (this.proxyID != null) {
        return (this.proxyID.equals(otherMP.proxyID));
      }
      throw new IllegalStateException();
    }    
  }
 
  /***************************************************************************
  **
  ** Used for a min wrapper
  */  
  
  public static class FixedMinPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
  
    /***************************************************************************
    **
    ** Constructor
    */
  
    public FixedMinPanel() {
      super();
    }     
 
    /***************************************************************************
    **
    ** Fixed minimum
    */
    
    @Override
    public Dimension getMinimumSize() {
      return (new Dimension(200, 200));
    }
    
    /***************************************************************************
    **
    ** Fixed minimum
    */

    @Override
    public Dimension getPreferredSize() {
      return (new Dimension(600, 400));
    } 
  }
  
  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  public static class DisplayColor extends Color {
    
    private static final long serialVersionUID = 1L;
    private String format_;
    
    DisplayColor(Color col, ResourceManager rMan) {
      super(col.getRed(), col.getGreen(), col.getBlue());
      format_ = rMan.getString("colMapEntry.colorDisplayFormat");
    }
  
    @Override
    public String toString() {
      return (MessageFormat.format(format_, new Object[] {Integer.toString(getRed()),
                                                          Integer.toString(getGreen()),
                                                          Integer.toString(getBlue())}));
    }
  }
  
  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  public static class ColorBlockRenderer extends DefaultTableCellRenderer {
  
    private DefaultTableCellRenderer defaultRenderer_;
    private HandlerAndManagerSource hams_;
    private static final long serialVersionUID = 1L;
           
    public ColorBlockRenderer(DefaultTableCellRenderer defaultRenderer, HandlerAndManagerSource hams) {
      defaultRenderer_ = defaultRenderer;
      hams_ = hams;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
      try {
        Component aComp = defaultRenderer_.getTableCellRendererComponent(table, value, isSelected, 
                                                                         hasFocus, row, column);
        
        aComp.setBackground((DisplayColor)value);
        return (aComp);
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }      
      return (null);    
    }
  }
}
