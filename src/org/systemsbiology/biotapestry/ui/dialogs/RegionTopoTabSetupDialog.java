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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.text.MessageFormat;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;

/****************************************************************************
**
** Dialog box for setting up region topology table
*/

public class RegionTopoTabSetupDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private EditableTable est_;
  private boolean haveChanges_;
  private ArrayList<Integer> times_;
  private List<Integer> origTimes_;
  private List<Integer> requiredTimes_;
  private boolean namedStages_;
  private TimeAxisDefinition tad_;
  private UIComponentSource uics_;
  
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
  
  public RegionTopoTabSetupDialog(UIComponentSource uics, TimeAxisDefinition tad, List<Integer> currTimes, List<Integer> requiredTimes) {     
    super(uics.getTopFrame(), uics.getRMan().getString("topoTab.title"), true);
    uics_ = uics;
    tad_ = tad;
    setSize(500, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    haveChanges_ = false;
    origTimes_ = new ArrayList<Integer>(currTimes);
    requiredTimes_ = new ArrayList<Integer>(requiredTimes);    
 
    //
    // Figure out if we accept numbers or names for the stages:
    //
    

    namedStages_ = tad_.haveNamedStages();    
    
    est_ = new EditableTable(uics_, new ColumnTableModel(uics_, tad_, namedStages_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);                                      
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
    
    DialogSupport ds = new DialogSupport(this, uics_, gbc);
    ds.buildAndInstallButtonBox(cp, 10, 10, false, true);
    setLocationRelativeTo(uics_.getTopFrame());
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
    throw new UnsupportedOperationException();
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
  
  /***************************************************************************
  **
  ** Answer if changes have been made
  ** 
  */
  
  public boolean haveChanges() {
    return (haveChanges_);
  }
  
  /***************************************************************************
  **
  ** Get added info
  ** 
  */
  
  public List<Integer> getNewTimes() {
    return (new ArrayList<Integer>(times_));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class ColumnTableModel extends EditableTable.TableModel {
    
    private final static int TIME_    = 0;
    private final static int NUM_COL_ = 1;

    private static final long serialVersionUID = 1L;
    
    ColumnTableModel(UIComponentSource uics, TimeAxisDefinition tad, boolean namedStages) {
      super(uics, NUM_COL_);
      String displayUnits = tad.unitDisplayString();
      ResourceManager rMan = uics_.getRMan();
      String unitHeading = MessageFormat.format(rMan.getString("topoTab.timeUnitFormat"), new Object[] {displayUnits});
      colNames_ = new String[] {unitHeading};
      colClasses_ = (namedStages) ? new Class[] {String.class} : new Class[] {ProtoInteger.class};
    }
    
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        retval.add(columns_[TIME_].get(i));
      }
      return (retval);
    }
     
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        Integer next = (Integer)rit.next();
        if (colClasses_[0].equals(String.class)) {
          TimeAxisDefinition.NamedStage stageName = tad_.getNamedStageForIndex(next.intValue());
          columns_[TIME_].add(stageName.name);
        } else {          
          columns_[TIME_].add(new ProtoInteger(next.toString()));                  
        }
      }
    }
  
    boolean applyValues() {
      List vals = getValuesFromTable();
      if (vals.isEmpty()) {
        return (false);
      }
      
      ResourceManager rMan = uics_.getRMan();
      //
      // Make sure the integers are OK, non-overlapping, etc:
      //
      
      ArrayList<Integer> newTimes = new ArrayList<Integer>();   
      int size = vals.size();
      int lastVal = -1;
      for (int i = 0; i < size; i++) {
        int timeVal;
        if (namedStages_) {
          String stageName = (String)vals.get(i);
          timeVal = (stageName == null) ? TimeAxisDefinition.INVALID_STAGE_NAME 
                                        : tad_.getIndexForNamedStage(stageName);
          if (timeVal == TimeAxisDefinition.INVALID_STAGE_NAME) {
            JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("topoTab.badStageName"),
                                          rMan.getString("topoTab.badStageNameTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }   
        } else {      
          ProtoInteger val = (ProtoInteger)vals.get(i);
          if ((val == null) || (!val.valid)) {
            IntegerEditor.triggerWarning(uics_.getHandlerAndManagerSource(), uics_.getTopFrame());
            return (false);
          }
          timeVal = val.value;
        }
        if (timeVal <= lastVal) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("topoTab.badVals"),
                                                 rMan.getString("topoTab.badValsTitle"),
                                                 JOptionPane.ERROR_MESSAGE);          
          return (false); 
        }
        newTimes.add(Integer.valueOf(timeVal));
        lastVal = timeVal;       
      }           
      
      //
      // Apply the values by completely replacing the data:
      //
      
      times_ = newTimes;
 
      int numReq = requiredTimes_.size();   
      for (int i = 0; i < numReq; i++) {
        Integer val = requiredTimes_.get(i);
        if (!times_.contains(val)) {
          String missingTime = (namedStages_) ? tad_.getNamedStageForIndex(val.intValue()).name : val.toString();
          String form = rMan.getString("topoTab.missingTime");
          String msg = MessageFormat.format(form, new Object[] {missingTime, tad_.unitDisplayString()});
          JOptionPane.showMessageDialog(uics_.getTopFrame(), msg,
                                                 rMan.getString("topoTab.missingTimeTitle"),
                                                 JOptionPane.ERROR_MESSAGE);          
          return (false); 
        }  
      }      

      if (!times_.equals(origTimes_)) {
        haveChanges_ = true;
      }
  
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
    est_.getModel().extractValues((origTimes_ == null) ? new ArrayList<Integer>() : origTimes_);
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the data
  ** 
  */
  
  private boolean applyProperties() {
    return (((ColumnTableModel)est_.getModel()).applyValues());
  }
}
