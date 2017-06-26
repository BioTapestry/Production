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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.util.EnumCell;

/****************************************************************************
**
** Panel for editing or creating data region restrictions
*/

public class PertRegRestrictAddOrEditPanel extends AnimatedSplitEditPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private PerturbationData.RegionRestrict rrResult_;
  private PerturbationData pd_;
  private TimeCourseData tcd_;
  private ArrayList<EnumCell> regionList_;
  private String parentCurrKey_;

  private JTextField legacyNullRegion_;
  private EditableTable estRr_;
  private PerturbationData.RegionRestrict currRegRestrict_;
  private PertManageHelper pmh_;
  private JLabel legacyWarning_;
  private JLabel legacyLabel_;
  
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
  
  public PertRegRestrictAddOrEditPanel(UIComponentSource uics, JFrame myParent, 
                                       PerturbationData pd,
                                       TimeCourseData tcd, PendingEditTracker pet, String myKey, 
                                       int legacyModes) {
    super(uics, myParent, pet, myKey, 2);
    pd_ = pd;
    tcd_ = tcd;
    pmh_ = new PertManageHelper(uics, myParent, pd, tcd, rMan_, gbc_, pet_);
      
    JLabel descLabel = new JLabel(rMan_.getString("prraep.description"));
    UiUtil.gbcSet(gbc_, 0, rowNum_++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    add(descLabel, gbc_);

    //
    // Build the region table
    //

    regionList_ = new ArrayList<EnumCell>();
    estRr_ = new EditableTable(uics, new EditableTable.OneEnumTableModel(uics, "prraep.region", regionList_), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.tableIsUnselectable = false;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.buttonsOnSide = false;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, regionList_, EnumCell.class));  
    JPanel regTablePan = estRr_.buildEditableTable(etp);
    UiUtil.gbcSet(gbc_, 0, rowNum_, 2, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
    add(regTablePan, gbc_);
    rowNum_ += 2;
    
    if ((legacyModes & PerturbationData.HAVE_LEGACY_NULL_REGION) != 0x00) {
      legacyLabel_ = new JLabel(rMan_.getString("prraep.legacyNullTag"));
      legacyNullRegion_ = new JTextField();
      UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
      add(legacyLabel_, gbc_);
      UiUtil.gbcSet(gbc_, 1, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
      add(legacyNullRegion_, gbc_);
      legacyWarning_ = new JLabel("", JLabel.CENTER);
      UiUtil.gbcSet(gbc_, 0, rowNum_++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
      add(legacyWarning_, gbc_);   
    }
 
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Set the new psi
  ** 
  */
  
  public void setSources(String parentKey, PerturbationData.RegionRestrict currRegRestrict, int mode) {
    mode_ = mode;
    currRegRestrict_ = currRegRestrict;
    parentCurrKey_ = parentKey;
    displayProperties();
    return;
  }
 
  /***************************************************************************
  **
  ** Get the new region restriction
  ** 
  */
  
  public PerturbationData.RegionRestrict getResult() {
    return (rrResult_);
  }
  
  /***************************************************************************
  **
  ** Clear out the editor:
  */  
   
  public void closeAction() {
    estRr_.stopTheEditing(false);
    super.closeAction();
    return;
  } 
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  }  
   
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    regionList_ = buildRegionEnum(tcd_);
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, regionList_, EnumCell.class));      
    estRr_.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)estRr_.getModel()).setCurrentEnums(regionList_);
    return;
  }
  
  /***************************************************************************
  **
  ** Stash our UI values
  ** 
  */
  
  protected boolean stashResults() {
 
    String legNullVal = null;
    if (legacyNullRegion_ != null) {
      legNullVal = (legacyNullRegion_.isEnabled()) ? legacyNullRegion_.getText().trim() : null;
    }
    
    if ((legNullVal != null) && legNullVal.equals("")) {
      legNullVal = null;
    }
    
    if (legNullVal != null) {
      int yes = JOptionPane.showConfirmDialog(parent_,
                                              rMan_.getString("prraep.legacyNotRecommended"),
                                              rMan_.getString("prraep.legacyNotRecommendedTitle"),
                                              JOptionPane.YES_NO_OPTION);
      if (yes != JOptionPane.YES_OPTION) {  
        return (false);
      } 
    }
     
    Iterator sit = estRr_.getModel().getValuesFromTable().iterator();
    ArrayList regionList = (sit.hasNext()) ? new ArrayList() : null;
    while (sit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)sit.next();
      EnumCell ec = ent.enumChoice;
      if (regionList.contains(ec.internal)) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("prraep.dupRegion"),
                                      rMan_.getString("prraep.dupRegionTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }    
      regionList.add(ec.internal);
    }
    
    if ((legNullVal != null) && (regionList != null) && !regionList.isEmpty()) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("peaep.dualModeIllegal"),
                                    rMan_.getString("peaep.dualModeIllegalTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }    
    
   
    if (legNullVal != null) { 
      rrResult_ = new PerturbationData.RegionRestrict(legNullVal);
    } else {
      rrResult_ = (regionList == null) ? null : new PerturbationData.RegionRestrict(regionList);
    }
   
    estRr_.stopTheEditing(false);
    return (true);
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
    
    updateOptions();
                  
    List srcRows = buildRegionDisplayList();
    estRr_.updateTable(true, srcRows);
 
    if (currRegRestrict_ == null) {
      if (legacyNullRegion_ != null) {
        legacyNullRegion_.setText("");
        legacyNullRegion_.setEnabled(false);
      }
    } else {
      if (legacyNullRegion_ != null) {
        if (currRegRestrict_.isLegacyNullStyle()) {
          legacyLabel_.setEnabled(true);
          legacyNullRegion_.setText(currRegRestrict_.getLegacyValue());
          legacyNullRegion_.setEnabled(true);
          legacyWarning_.setText(rMan_.getString("prraep.legacyWarning"));
        } else {
          legacyLabel_.setEnabled(false);
          legacyNullRegion_.setText("");
          legacyNullRegion_.setEnabled(false);
          legacyWarning_.setText("");
        }
        legacyWarning_.revalidate();
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Build the Enum of regions
  ** 
  */
  
  private ArrayList<EnumCell> buildRegionEnum(TimeCourseData tcd) { 
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    TreeSet<String> toSort = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    toSort.addAll(tcd.getRegions());
    Iterator<String> tsit = toSort.iterator();
    int count = 0;
    while (tsit.hasNext()) {
      String regName = tsit.next();          
      retval.add(new EnumCell(regName, regName, count, count));
      count++;
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Get the region display list
  */
  
  private List buildRegionDisplayList() {
    ArrayList retval = new ArrayList();
    if ((currRegRestrict_ == null) || currRegRestrict_.isLegacyNullStyle()) {
      return (retval);
    }       
    Iterator<String> rit = currRegRestrict_.getRegions();
    return (buildRegionDisplayListCore(rit, false));  
  }

  /***************************************************************************
  **
  ** Get the region display list
  */
  
  private List<EditableTable.OneEnumTableModel.TableRow> buildRegionDisplayListCore(Iterator rit, boolean forHotUpdate) {
    ArrayList<EditableTable.OneEnumTableModel.TableRow> retval = new ArrayList<EditableTable.OneEnumTableModel.TableRow>();
    EditableTable.OneEnumTableModel rpt = (EditableTable.OneEnumTableModel)estRr_.getModel();  
    int count = 0;
    int useIndex = -1;
    int numReg = regionList_.size();
    while (rit.hasNext()) {
      String regionID;
      if (forHotUpdate) {
        regionID = ((EditableTable.OneEnumTableModel.TableRow)rit.next()).enumChoice.internal;
      } else {    
        regionID = (String)rit.next();
      }
      EditableTable.OneEnumTableModel.TableRow tr = rpt.new TableRow();
      tr.origOrder = new Integer(count++);
      for (int i = 0; i < numReg; i++) {
        EnumCell ecp = regionList_.get(i);
        if (regionID.equals(ecp.internal)) {
          useIndex = i;
          break;
        }
      }
      if (useIndex == -1) {
        if (forHotUpdate) {
          continue;
        } else {
          throw new IllegalStateException();
        }
      }
      tr.enumChoice = new EnumCell(regionList_.get(useIndex));
      retval.add(tr);
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {
  
    private String legacyNullRegionText;
    private List estRegValues;
    private PerturbationData.RegionRestrict frozenRegRestrict;
      
    MyFreezeDried() {     
      legacyNullRegionText = (legacyNullRegion_ == null) ? null : legacyNullRegion_.getText();    
      estRegValues = estRr_.getModel().getValuesFromTable();
      frozenRegRestrict = (currRegRestrict_ == null) ? null : (PerturbationData.RegionRestrict)currRegRestrict_.clone();
    }   
    
    public boolean needToCancel() {
      if ((parentCurrKey_ != null) && (mode_ == EDIT_MODE)) {
        if (pd_.getDataPoint(parentCurrKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      if (legacyNullRegion_ != null) {
        legacyNullRegion_.setText(legacyNullRegionText);
      }    
      currRegRestrict_ = pmh_.fixupFrozenRegRes(frozenRegRestrict);
      List regionRows = buildRegionDisplayListCore(estRegValues.iterator(), true);     
      estRr_.updateTable(true, regionRows);
      return;
    }
  }
}
