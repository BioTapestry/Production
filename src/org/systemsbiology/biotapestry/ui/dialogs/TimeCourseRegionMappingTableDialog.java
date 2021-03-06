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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for choosing Time Course mapping
*/

public class TimeCourseRegionMappingTableDialog extends JDialog implements DialogSupport.DialogSupportClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private EditableTable est_;
  private String groupID_;
  private TimeCourseData tcd_; 
  private ArrayList regions_;
  private ArrayList models_;
  private BTState appState_;
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
  
  public TimeCourseRegionMappingTableDialog(BTState appState, DataAccessContext dacx, String groupID, String groupName) {     
    super(appState.getTopFrame(), appState.getRMan().getString("tcrmd.title"), true);
    groupID_ = groupID;
    appState_ = appState;
    dacx_ = dacx;
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(500, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    tcd_ = dacx_.getExpDataSrc().getTimeCourseData();
    regions_ = buildRegionEnum(tcd_);
    models_ = buildModelEnum();
    
    //
    // Build the values table.
    //
      
    String labelFormat = rMan.getString("tcrmd.labelFormat");
    if ((groupName == null) || (groupName.trim().equals(""))) {
      groupName = rMan.getString("tcrmd.noName");
    }
    Object[] regionName = new Object[] {groupName};
    String message = MessageFormat.format(labelFormat, regionName);     
    JLabel lab = new JLabel(message);
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(lab, gbc);
    
    est_ = new EditableTable(appState_, new TimeCourseRegionMappingTableModel(appState_), appState_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.perColumnEnums = new HashMap();
    etp.perColumnEnums.put(new Integer(TimeCourseRegionMappingTableModel.REGION_), new EditableTable.EnumCellInfo(true, regions_));
    etp.perColumnEnums.put(new Integer(TimeCourseRegionMappingTableModel.MODEL_), new EditableTable.EnumCellInfo(false, models_));  
    JPanel tablePan = est_.buildEditableTable(etp);
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 6, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(tablePan, gbc);
 
    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallButtonBox(cp, 7, 1, true, false);
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
  ** Used for transfer to data table
  */
  
  class RegUseEnumCellPair {
    EnumCell regEC;
    EnumCell modEC;
  }
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class TimeCourseRegionMappingTableModel extends EditableTable.TableModel {
    
    final static int REGION_  = 0;
    final static int MODEL_   = 1;
    private final static int NUM_COL_ = 2;
    
    private static final long serialVersionUID = 1L;
     
    TimeCourseRegionMappingTableModel(BTState appState) {
      super(appState, NUM_COL_);
      colNames_ = new String[] {"tcrmap.region",
                                "tcrmap.model"};
      colClasses_ = new Class[] {EnumCell.class,
                                 EnumCell.class};
    }

    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        EnumCell rec = (EnumCell)columns_[REGION_].get(i);
        EnumCell mec = (EnumCell)columns_[MODEL_].get(i);
        GroupUsage gu = new GroupUsage(rec.internal, (mec.value == 0) ? null : mec.internal);
        retval.add(gu);
      }
      return (retval);
    }
    
    public boolean addRow(int[] rows) {
      super.addRow(rows);
      int lastIndex = rowCount_ - 1;
      columns_[REGION_].set(lastIndex, new EnumCell((EnumCell)regions_.get(0)));
      columns_[MODEL_].set(lastIndex, new EnumCell((EnumCell)models_.get(0))); 
      return (true);
    }
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        RegUseEnumCellPair ruecp = (RegUseEnumCellPair)rit.next(); 
        columns_[REGION_].add(ruecp.regEC);  
        columns_[MODEL_].add(ruecp.modEC);
      }
      return;
    }

    boolean applyValues() {
      List vals = getValuesFromTable();
      if (vals.isEmpty()) {
        return (true);
      }

      //
      // Blank region names don't cut it:
      //

      for (int i = 0; i < rowCount_; i++) {
        GroupUsage gu = (GroupUsage)vals.get(i);
        if ((gu.mappedGroup == null) || (gu.mappedGroup.trim().equals(""))) {
          ResourceManager rMan = appState_.getRMan();
          JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("tcrmap.blankGroup"),
                                        rMan.getString("tcrmap.blankGroupTitle"),
                                        JOptionPane.ERROR_MESSAGE);          
          return (false);
        }
      }
       
      //
      // Undo/Redo support
      //
    
      UndoSupport support = new UndoSupport(appState_, "undo.tcrmd");
      TimeCourseChange tcc = tcd_.setTimeCourseGroupMap(groupID_, vals, true);
      support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));  
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();      
      return (true);
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
    List entries = tableEntries();
    est_.getModel().extractValues(entries);
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the data
  ** 
  */
  
  private boolean applyProperties() {
    return (((TimeCourseRegionMappingTableModel)est_.getModel()).applyValues());
  } 
  
  /***************************************************************************
  **
  ** figure out the table entries
  ** 
  */
  
  private List tableEntries() {    
    ArrayList retval = new ArrayList();
    List keys = tcd_.getCustomTimeCourseGroupKeys(groupID_);
    if (keys == null) {
      return (retval);
    }
   
    Iterator kit = keys.iterator();
    while (kit.hasNext()) {
      GroupUsage gu = (GroupUsage)kit.next();
      RegUseEnumCellPair ruecp = new RegUseEnumCellPair();
      retval.add(ruecp);
      Iterator rit = regions_.iterator();
      while (rit.hasNext()) {
        EnumCell cell = (EnumCell)rit.next();
        if (cell.internal.equals(gu.mappedGroup)) {
          ruecp.regEC = new EnumCell(cell);
          break;
        }
      }
      if (gu.usage == null) {
        ruecp.modEC = new EnumCell((EnumCell)models_.get(0));
      } else {
        Iterator mit = models_.iterator();
        while (mit.hasNext()) {
          EnumCell cell = (EnumCell)mit.next();
          if (cell.internal == null) {
            continue;
          }
          if (cell.internal.equals(gu.usage)) {
            ruecp.modEC = new EnumCell(cell);
            break;
          }
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Build the Enum of regions
  ** 
  */
  
  private ArrayList buildRegionEnum(TimeCourseData tcd) {  
    ArrayList retval = new ArrayList();
    Iterator rit = tcd.getRegions().iterator();
    int count = 0;
    while (rit.hasNext()) {
      String regionName = (String)rit.next();
      retval.add(new EnumCell(regionName, regionName, count, count));
      count++;
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Build the Enum of models
  ** 
  */
  
  private ArrayList buildModelEnum() {
    ArrayList retval = new ArrayList();
    ResourceManager rMan = appState_.getRMan();
    String modelFormat = rMan.getString("tcrmd.modelFormat");
    String allModels = rMan.getString("tcrmd.allModels");
    Object[] modelName = new Object[1];
    Iterator dpit = dacx_.getGenomeSource().getDynamicProxyIterator();
    retval.add(new EnumCell(allModels, null, 0, 0));
    int count = 1;
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = (DynamicInstanceProxy)dpit.next();
      modelName[0] = dip.getName();
      String modelId = dip.getID();
      String desc = MessageFormat.format(modelFormat, modelName);          
      retval.add(new EnumCell(desc, modelId, count, count));
      count++;
    }
    return (retval);
  }    
}
