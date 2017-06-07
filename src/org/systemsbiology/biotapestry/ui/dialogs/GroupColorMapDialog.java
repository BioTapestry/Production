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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.TreeNodeChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.ui.ModelImagePanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.DataUtil;
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
  private DataAccessContext dacx_;
  private UndoFactory uFac_;
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
  
  
  private ModelImagePanel myPanel_;
 // private BufferedImage myMap_;
// private BufferedImage myImg_;

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
    nt_ = nt;
    treeNode_ = treeNode;
    currMap_ = new HashMap<Color, NavTree.GroupNodeMapEntry>();
    fixMap_ = new HashMap<String, NavTree.GroupNodeMapEntry>();
    if ((currMap == null) || currMap.isEmpty()) {
      Set<Color> cols = ImageHighlighter.collectColors(mapImg);
      for (Color col : cols) {
        currMap_.put(col, new NavTree.GroupNodeMapEntry(col));     
      }     
    } else {
      for (Color key : currMap.keySet()) {
        NavTree.GroupNodeMapEntry gnme = currMap.get(key);
        NavTree.GroupNodeMapEntry gnmeCl = gnme.clone();
        currMap_.put(key, gnmeCl);     
      }
    }
    
    setSize(850, 750);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    
    DialogSupport ds = new DialogSupport(this, uics_, dacx_, gbc);
    
    modTocc_ = buildModelChoices();
    regTocc_ = buildRegionChoices(null, null, null);
    timTocc_ = buildTimeChoices(null);
     
    JPanel tpan = buildATable(dacx_.getRMan());
    int row = ds.addTable(cp, tpan, 6, 0, 10);  
    
    pathLabel_ = new JLabel("Full model path here--------------------------------------------");  
    row = ds.addWidgetFullRow(cp, pathLabel_, true, row, 10);
 
    JPanel epan = buildEntryPanel(dacx_.getRMan(), gbc);
    row = ds.addTable(cp, epan, 4, row, 10);
    
//   row = ds.addTallWidgetFullRow(cp, epan, false, true, 4, row, 10);
    
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
//    stopTheEditing(false);
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
//    stopTheEditing(true);
    setVisible(false);
    dispose();
    return;
  } 

  /***************************************************************************
  **
  ** Build the table
  */ 
  
  private JPanel buildATable(ResourceManager rMan) {   

    //
    // Build the tables:
    //
 
    est_ = new ReadOnlyTable(uics_, dacx_, new ColorMapTableModel(uics_, dacx_), new Selector());   
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.NO_BUTTONS;
    tp.multiTableSelectionSyncing = null;
    tp.tableTitle = rMan.getString("Hello Worls");
    tp.titleFont = null;
    
    ArrayList<ReadOnlyTable.ColumnWidths> colWidths = new ArrayList<ReadOnlyTable.ColumnWidths>();   
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.COLOR, 100, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.TARGET_MODEL, 100, 150, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.REGION, 200, 200, Integer.MAX_VALUE));
    colWidths.add(new ReadOnlyTable.ColumnWidths(ColorMapTableModel.TIME, 200, 200, Integer.MAX_VALUE));     
    tp.colWidths = colWidths;
    tp.canMultiSelect = false;
    JPanel tabPan = est_.buildReadOnlyTable(tp); 
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
          updateTimeAndRegions();
          setTheModel();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
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

    class TableRow {
      String color;
      String targetModel;
      String region;
      String time;
      String hiddenCandID;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        color = (String)columns_[COLOR].get(i); 
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
  
    ColorMapTableModel(UIComponentSource uics, DataAccessContext dacx) {
      super(uics, dacx, NUM_COL_);
      String displayUnits = dacx_.getExpDataSrc().getTimeAxisDefinition().unitDisplayString();
      ResourceManager rMan = dacx_.getRMan();
      String timeColumnHeading = MessageFormat.format(rMan.getString("colMapEntry.timeColFormat"), new Object[] {displayUnits});

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
  ** Update Time And Regions for JCombos
  */
  
  private void updateTimeAndRegions() {  
 
    installing_ = true;
    ModelOrProxy choice = (ModelOrProxy)((TrueObjChoiceContent)modelChoices_.getSelectedItem()).val; 
    String pathToShow;
    GenomeSource gs = dacx_.getGenomeSource();
    if (choice == null) {
      pathToShow = "";
      timTocc_ = buildTimeChoices(null);
      regTocc_ = buildRegionChoices(null, null, null);
    } else if (choice.proxyID != null) {       
      DynamicInstanceProxy dip = gs.getDynamicProxy(choice.proxyID);
      timeChoices_.setEnabled(!dip.isSingle());
      Iterator<Group> grit = dip.getGroupIterator();
      regTocc_ = buildRegionChoices(grit, dip.getAnInstance(), dip);
      regionChoices_.setEnabled(true);
      timTocc_ = buildTimeChoices(dip);  
      pathToShow = choice.displayPath;
    } else {
      timeChoices_.setEnabled(false);
      Genome model = gs.getGenome(choice.modelID);
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
      pathToShow = choice.displayPath;
    }
    pathLabel_.setText(pathToShow);
    pathLabel_.invalidate();
    pathLabel_.revalidate();

    UiUtil.replaceComboItems(regionChoices_, regTocc_);
    UiUtil.replaceComboItems(timeChoices_, timTocc_);
    installing_ = false;
    return;
  }

  /***************************************************************************
  **
  ** Set the time
  */
  
  private void setTheTime() {  
  
    if (installing_) {
      return;
    }
    installing_ = true;
    Integer choice = (Integer)((TrueObjChoiceContent)timeChoices_.getSelectedItem()).val;
    ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
    selectedEntry_.time = choice.toString();
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
  ** Set the region
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
    selectedEntry_.region = getRegionNameForEntry(gnme, dacx_.getGenomeSource());  
    selectedEntry_.replaceCols(selectedRow_.intValue());
    ecdtm.fireTableRowsUpdated(selectedRow_.intValue(), selectedRow_.intValue());
    installing_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the Model. Note that setting the model will null out the region and
  ** time guys as well.
  */
  
  private void setTheModel() {  
  
    if (installing_) {
      return;
    }
    installing_ = true;
    GenomeSource gs = dacx_.getGenomeSource();
    NavTree.GroupNodeMapEntry gnme = fixMap_.get(selectedEntry_.hiddenCandID);
    ColorMapTableModel ecdtm = (ColorMapTableModel)est_.getModel();
    ModelOrProxy choice = (ModelOrProxy)((TrueObjChoiceContent)modelChoices_.getSelectedItem()).val;
    
    if (choice == null) {
      if ((gnme.modelID == null) && (gnme.proxyID == null)) {
        installing_ = false;
        return;
      } 
      selectedEntry_.targetModel = "None";
      // Model changed? Toss everything else...
      selectedEntry_.region = "N/A";
      selectedEntry_.time = "N/A";
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
      DynamicInstanceProxy dip = gs.getDynamicProxy(choice.proxyID);
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
      selectedEntry_.region = "N/A";
      selectedEntry_.time = "N/A";
      gnme.proxyID = choice.proxyID;
      gnme.regionID = null;
      gnme.proxyTime = null;
    } else if (choice.modelID != null) {
      if (choice.modelID.equals(gnme.modelID)) {
        installing_ = false;
        return;
      } 
      Genome gen = gs.getGenome(choice.modelID);
      selectedEntry_.targetModel = gen.getName();
      // Model changed? Toss everything else...
      selectedEntry_.region = "N/A";
      selectedEntry_.time = "N/A";
      gnme.modelID = choice.modelID;
      gnme.regionID = null;
      gnme.proxyTime = null; 
    }
    selectedEntry_.replaceCols(selectedRow_.intValue());
    ecdtm.fireTableRowsUpdated(selectedRow_.intValue(), selectedRow_.intValue());
    installing_ = false;
    return;
  } 

  /***************************************************************************
  **
  ** Pass the selection to the search panel
  */
  
  private void goAndSelect() {
    if (selectedEntry_ == null) {
      System.out.println("Empty selections!");
      return;
    }
    
    NavTree.GroupNodeMapEntry gnme = fixMap_.get(selectedEntry_.hiddenCandID);
 
    BufferedImage bim = ImageHighlighter.buildMaskedImageBandW(mapImg_, mapImg_, gnme.color.getRed(), gnme.color.getGreen(), gnme.color.getBlue(), 0.6F);
    myPanel_.setImage(bim);

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
    List<ColorMapTableModel.TableRow> entries = buildTableRows(currMap_, fixMap_);
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
  
  private List<ColorMapTableModel.TableRow> buildTableRows(Map<Color, NavTree.GroupNodeMapEntry> modMap, Map<String, NavTree.GroupNodeMapEntry> fixMap) {
    ColorMapTableModel tctm = (ColorMapTableModel)est_.getModel();
    ArrayList<ColorMapTableModel.TableRow> retval = new ArrayList<ColorMapTableModel.TableRow>();
    
    int count = 0;
    Iterator<Color> eeit = modMap.keySet().iterator();
    while (eeit.hasNext()) {
      Color key = eeit.next();
      NavTree.GroupNodeMapEntry entry = modMap.get(key); 
      ColorMapTableModel.TableRow tr = tctm.new TableRow();  
      
      tr.color = key.toString();
      tr.region = "";
      tr.time = "";
      tr.targetModel = "";
      GenomeSource gs = dacx_.getGenomeSource();

      if (entry.proxyID != null) {  
              UiUtil.fixMePrintout("Null ptr here trying to set image map for MTGNNNONOtesMoreSubPreDupPoDupOK.btp");
        DynamicInstanceProxy dip = gs.getDynamicProxy(entry.proxyID);
        if (dip.isSingle()) {
          List<String> newNodes = dip.getProxiedKeys();
          if (newNodes.size() != 1) {
            throw new IllegalStateException();
          }
          String key1 = newNodes.iterator().next();
          tr.targetModel = dip.getProxiedInstanceName(key1);
          UiUtil.fixMePrintout("Nah use rMan for this");
          tr.time = "ALL";
        } else {
          tr.targetModel = dip.getName();
          tr.time = entry.proxyTime.toString();
        }
      } else if (entry.modelID != null) {
        Genome gen = gs.getGenome(entry.modelID);
        tr.targetModel = gen.getName();
      }
      UiUtil.fixMePrintout("Nah use rMan for this");
      tr.region = getRegionNameForEntry(entry, gs);
      tr.hiddenCandID = Integer.toString(count++);
      fixMap.put(tr.hiddenCandID, entry);     
      retval.add(tr);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Apply our UI values to the time course data
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
    UiUtil.fixMePrintout("Nah use rMan for this!");
    String retval;
    if (gi == null) {
      retval = "N/A"; 
    } else if (entry.regionID == null) {
      retval = "All regions"; 
    } else {
      Group grp = gi.getGroup(entry.regionID);
      System.out.println(entry.regionID + " " + grp);
      retval = (grp == null) ? "NO group for " + entry.regionID : grp.getInheritedDisplayName(gi);
    }
    return (retval); 
  }
  
  
  /***************************************************************************
  **
  ** Apply our UI values to the time course data
  ** 
  */
  
  private boolean applyProperties() {
    
    UndoSupport support = uFac_.provideUndoSupport("undo.setGroupNavMap", dacx_);
    
    Map<Color, NavTree.GroupNodeMapEntry> newMap = new HashMap<Color, NavTree.GroupNodeMapEntry>();
    Iterator<NavTree.GroupNodeMapEntry> trit = fixMap_.values().iterator();
    while (trit.hasNext()) {        
      NavTree.GroupNodeMapEntry tr = trit.next();
      newMap.put(tr.color, tr);
    }
    
    NavTreeChange ntc = nt_.installGroupModelMap(treeNode_, newMap);
    support.addEdit(new NavTreeChangeCmd(dacx_, ntc));
    NavTree.NodeID nodeKey = new NavTree.NodeID(nt_.getGroupNodeID(treeNode_));
    support.addEvent(new TreeNodeChangeEvent(nodeKey, TreeNodeChangeEvent.Change.GROUP_NODE_CHANGE));
    support.finish();
    return (true);
  }
  

  /***************************************************************************
  **
  ** Apply table values
  */
   
  private void applyTableValues() {        
  

    return;
  }
  

  
  /***************************************************************************
  **
  ** Build the Model Choices
  ** 
  */
  
  private Vector<TrueObjChoiceContent> buildModelChoices() { 
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>(); 
    retval.add(new TrueObjChoiceContent(dacx_.getRMan().getString("grpColMatEdit.noDest"), null));
    GenomeSource gs = dacx_.getGenomeSource();
    NavTree navTree = gs.getModelHierarchy();   
    List<String> pol = navTree.getFullTreePreorderListing(new StaticDataAccessContext(dacx_).getContextForRoot());
    Iterator<String> it = pol.iterator();
    int count = 0;
    while (it.hasNext()) {
      String id = it.next();
      Genome gen = gs.getGenome(id);
      String modID = null;
      String proxID = null;
      List<String> names = gen.getNamesToRoot();
      if (gen instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)gen;
        proxID = dgi.getProxyID();        
        DynamicInstanceProxy dprox = gs.getDynamicProxy(proxID);
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
      retval.add(new TrueObjChoiceContent(dacx_.getRMan().getString("grpColMatEdit.noRegions"), null));
    } else {
      retval.add(new TrueObjChoiceContent(dacx_.getRMan().getString("grpColMatEdit.noRegionSelected"), null));
      while (grit.hasNext()) {
        Group grp = grit.next();
        if (((dip == null) && grp.isASubset(gi)) || ((dip != null) && grp.isASubset(dip))) {
          continue;
        }
        System.out.println(grp.getInheritedDisplayName(gi));
        retval.add(new TrueObjChoiceContent(grp.getInheritedDisplayName(gi), grp.getID()));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build Times
  */
  
  private Vector<TrueObjChoiceContent> buildTimeChoices(DynamicInstanceProxy dip) { 
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>(); 
    if ((dip == null) || dip.isSingle()) {
      retval.add(new TrueObjChoiceContent(dacx_.getRMan().getString("grpColMatEdit.noTimes"), null));
      
    } else {
      UiUtil.fixMePrintout("USe this: format for named stages");
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      boolean namedStages = tad.haveNamedStages();
      String displayUnits = tad.unitDisplayString();       
      String formatKey = (tad.unitsAreASuffix()) ? "grpColMatEdit.nameFormat" : "grpColMatEdit.nameFormatPrefix";
      String format = dacx_.getRMan().getString(formatKey);
      int min = dip.getMinimumTime();
      int max = dip.getMaximumTime();
      for (int i = min; i <= max; i++) {
        String stageName = (namedStages) ? tad.getNamedStageForIndex(i).name : Integer.toString(i);
        String dispName = MessageFormat.format(format, new Object[] {stageName, displayUnits});
        retval.add(new TrueObjChoiceContent(dispName, Integer.valueOf(i)));
      }
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
    
    public Dimension getMinimumSize() {
      return (new Dimension(200, 200));
    }
    
    /***************************************************************************
    **
    ** Fixed minimum
    */
    
    public Dimension getPreferredSize() {
      return (new Dimension(600, 400));
    }
    
  }
}
