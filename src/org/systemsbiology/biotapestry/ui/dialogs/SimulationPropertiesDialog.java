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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.SimParamSource;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBInternalLogic;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.SbmlSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ProtoDouble;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing simulation properties
*/

public class SimulationPropertiesDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private String nodeID_;
  private JComboBox combo_;
  private HashMap<Integer, ComboBoxEntry> comboChoices_;
  private JPanel customPlot_;
  private EditableTable est_;
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private SimParamSource sps_;
  
  private DBNode node_;
  private DBGenome dbGenome_;
  
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
  
  public SimulationPropertiesDialog(UIComponentSource uics, DataAccessContext dacx, String nodeID, UndoFactory uFac) {     
    super(uics.getTopFrame(), "", true);
    uics_ = uics;
    uFac_ = uFac;
    dacx_ = dacx;
    if (!dacx.currentGenomeIsRootDBGenome()) {
      throw new IllegalArgumentException();
    }
    dbGenome_ = dacx_.getCurrentGenomeAsDBGenome();
    node_ = (DBNode)dbGenome_.getNode(nodeID);
    sps_ = dacx_.getSimParamSource();
    ResourceManager rMan = uics_.getRMan();
    String format = rMan.getString("simProp.title");
    String desc = MessageFormat.format(format, new Object[] {node_.getName()});    
    setTitle(desc);    
    
    nodeID_ = nodeID;
    
    setSize(700, 600);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(5, 5, 20, 5));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    //
    // Build two tabs; one for logic, the other for parameters
    //

    JTabbedPane jtp = new JTabbedPane();
    jtp.addTab(rMan.getString("simProp.logic"), buildLogicPanel());
    jtp.addTab(rMan.getString("simProp.parameters"), buildParameterPanel());

    UiUtil.gbcSet(gbc, 0, 0, 1, 6, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(jtp, gbc);
    
    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    ds.buildAndInstallButtonBox(cp, 6, 1, true, false);   
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
   
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
  ** Used for the parameter table
  */
  
  class ParameterTableModel extends EditableTable.TableModel {
    
    private final static int PARAM_   = 0;
    private final static int VALUE_   = 1;
    private final static int REQUIRED = 2;
    private final static int NUM_COL_ = 3;
    
    private final static int ORIG_VAL_   = 0;
    private final static int NUM_HIDDEN_ = 1;
    
    private static final long serialVersionUID = 1L;
   
    class TableRow {
      String param;
      ProtoDouble value;
      String required;
      Double origVal;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        param = (String)columns_[PARAM_].get(i);
        value = (ProtoDouble)columns_[VALUE_].get(i);
        required = (String)columns_[REQUIRED].get(i);
        origVal = (Double)hiddenColumns_[ORIG_VAL_].get(i);
      }
      
      void toCols() {
        columns_[PARAM_].add(param);  
        columns_[VALUE_].add(value);
        columns_[REQUIRED].add(required);
        hiddenColumns_[ORIG_VAL_].add(origVal);
        return;
      }
    }
  
    ParameterTableModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"simProp.param",
                                "simProp.value",
                                "simProp.required"};
      colClasses_ = new Class[] {String.class,
                                 ProtoDouble.class,
                                 String.class};
      canEdit_ = new boolean[] {false,
                                true,
                                false};
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
  
  /***************************************************************************
  **
  ** We stick these into combo boxes
  ** 
  */
  
  public class ComboBoxEntry {
    int actionKey;
    String display;
    
    ComboBoxEntry(int action, String display) {
      this.actionKey = action;
      this.display = display;
    }
    
    public String toString() {
      return (display);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current simulation property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    DBInternalLogic dbil = node_.getInternalLogic();
    int ft = dbil.getFunctionType();
    Object choose = comboChoices_.get(new Integer(ft)); 
    combo_.setSelectedItem(choose);

    List tableList = buildTableList();
    ((ParameterTableModel)est_.getModel()).extractValues(tableList);
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the simulation properties
  ** 
  */
  
  private boolean applyProperties() {
  
    UndoSupport support = uFac_.provideUndoSupport("undo.spd", dacx_);    
    GenomeChange change = dbGenome_.startNodeUndoTransaction(nodeID_);
       
    DBInternalLogic dbil = node_.getInternalLogic();
    ComboBoxEntry cbe = (ComboBoxEntry)combo_.getSelectedItem();
    dbil.setFunctionType(cbe.actionKey);

    ParameterTableModel ptm = (ParameterTableModel)est_.getModel(); 
    List vals = ptm.getValuesFromTable();
    applyParameters(vals);
    
    change = dbGenome_.finishNodeUndoTransaction(nodeID_, change);
    support.addEdit(new GenomeChangeCmd(change));
    
    support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
    support.finish();
    return (true);
  }
  
  /***************************************************************************
  **
  ** Build the logic tab
  ** 
  */
  
  private JPanel buildLogicPanel() {
    JPanel logicPanel = new JPanel();
    ResourceManager rMan = uics_.getRMan();
    comboChoices_ = new HashMap<Integer, ComboBoxEntry>();
    
    logicPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JLabel label = new JLabel(rMan.getString("simProp.logicFunc"));
    Map<Integer, String> vals = DBInternalLogic.getLogicValues();
    Iterator<Integer> vit = vals.keySet().iterator();
    Vector<ComboBoxEntry> lvChoices = new Vector<ComboBoxEntry>();
    while (vit.hasNext()) {
      Integer valKey = vit.next();
      String val = vals.get(valKey);
      String display = rMan.getString("simProp." + val);
      ComboBoxEntry cbe = new ComboBoxEntry(valKey.intValue(), display);
      lvChoices.add(cbe);
      comboChoices_.put(valKey, cbe); 
    }
    combo_ = new JComboBox(lvChoices);
    combo_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          ComboBoxEntry sel = (ComboBoxEntry)combo_.getSelectedItem();
          Color col = (sel.actionKey == DBInternalLogic.CUSTOM_FUNCTION) ? Color.white : Color.lightGray;
          customPlot_.setBackground(col);
          customPlot_.repaint();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        // FIX ME!! Does not change parameter list to reflect new function type
      }
    });
    
    Box comboPanel = Box.createHorizontalBox();
    comboPanel.add(Box.createHorizontalStrut(10)); 
    comboPanel.add(label);
    comboPanel.add(Box.createHorizontalStrut(10));    
    comboPanel.add(combo_);
    comboPanel.add(Box.createHorizontalGlue());    
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    logicPanel.add(comboPanel, gbc);
    
    customPlot_ = new JPanel();
    customPlot_.setBackground(Color.lightGray);
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    logicPanel.add(customPlot_, gbc);    
   
    return (logicPanel);
  }  

  /***************************************************************************
  **
  ** Build the parameter tab
  ** 
  */
  
  private JPanel buildParameterPanel() {
    JPanel paramPanel = new JPanel();
    paramPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    est_ = new EditableTable(uics_, new ParameterTableModel(uics_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);        
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    paramPanel.add(tablePan, gbc);
    return (paramPanel);
  }
  
  /***************************************************************************
  **
  ** Build the parameter tab
  ** 
  */  
  
  private List<ParameterTableModel.TableRow> buildTableList() {
    ArrayList<ParameterTableModel.TableRow> retval = new ArrayList<ParameterTableModel.TableRow>();
    if (node_ == null) {
      return (retval);
    }
    ParameterTableModel ptm = (ParameterTableModel)est_.getModel();  
  
    Set<String> needed;
    if (node_.getNodeType() == Node.GENE) {
      needed = dbGenome_.requiredGeneParameters((DBGene)node_);
    } else {
      needed = dbGenome_.requiredNonGeneParameters(node_);
    }
    
    Set<String> baseNeeded = new HashSet<String>();
    DBInternalLogic logic = node_.getInternalLogic();
    Iterator<String> pkit = logic.getParameterKeys();
    ArrayList<String> keys = new ArrayList<String>();
    while (pkit.hasNext()) {
      keys.add(pkit.next());
    }
    Iterator<String> nit = needed.iterator();
    while (nit.hasNext()) {
      String need = nit.next();
      need = SbmlSupport.extractBaseIdFromParam(need);
      if (!keys.contains(need)) {
        keys.add(need);
      }
      baseNeeded.add(need);
    }
    Collections.sort(keys);

    Iterator<String> kit = keys.iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      ParameterTableModel.TableRow tr = ptm.new TableRow();    
      tr.param = key;
      tr.origVal = new Double(logic.getSimulationParam(sps_, key, node_.getNodeType()));
      tr.value = new ProtoDouble(tr.origVal.doubleValue());
      tr.required = Boolean.toString(baseNeeded.contains(key));
      retval.add(tr);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Apply parameters
  ** 
  */      
  
  private void applyParameters(List<ParameterTableModel.TableRow> vals) {
    if (node_ == null) {
      return;
    }
    DBInternalLogic logic = node_.getInternalLogic();
    int size = vals.size();
    for (int i = 0; i < size; i++) {
      ParameterTableModel.TableRow tr = vals.get(i);
      if (tr.value.value != tr.origVal.doubleValue()) {
        logic.setSimulationParam(tr.param, tr.value.textValue);
      }
    }
    return;
  }
}
