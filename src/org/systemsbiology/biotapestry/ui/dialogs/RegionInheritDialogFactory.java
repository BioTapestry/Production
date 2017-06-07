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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** Dialog box for specifying regions used in build instructions
*/

public class RegionInheritDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RegionInheritDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    BuildArgs dniba = (BuildArgs)ba;
 
    switch(platform.getPlatform()) {
      case DESKTOP:
        return (new DesktopDialog(cfh, dniba.parentRegions, dniba.workingRegions, false));  
      case WEB:
      default:
        throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
    }   
  }
  
  /***************************************************************************
  **
  ** Get the stashing version of the dialog
  */  
  
  public RegionInheritDialogFactory.DesktopDialog getStashDialog(DialogBuildArgs ba) {   
    BuildArgs dniba = (BuildArgs)ba;
    return (new DesktopDialog(cfh, dniba.parentRegions, dniba.workingRegions, true)); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    List<InstanceInstructionSet.RegionInfo> workingRegions; 
    List<InstanceInstructionSet.RegionInfo> parentRegions; 
          
    public BuildArgs(Genome genome, List<InstanceInstructionSet.RegionInfo> parentRegions, 
                     List<InstanceInstructionSet.RegionInfo> workingRegions) {
      super(genome);
      this.parentRegions = parentRegions; 
      this.workingRegions = workingRegions; 
    }
  }
   
  /****************************************************************************
  **
  ** Dialog box for specifying regions used in build instructions
  */
  
  public static class DesktopDialog extends BTTransmitResultsDialog {

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private EditableTable est_;
    private List<InstanceInstructionSet.RegionInfo> workingRegions_;
    private List<InstanceInstructionSet.RegionInfo> parentRegions_;
    
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
  
    public DesktopDialog(ServerControlFlowHarness cfh, 
                         List<InstanceInstructionSet.RegionInfo> parentRegions,
                         List<InstanceInstructionSet.RegionInfo> workingRegions, boolean forStash) {
      super(cfh, "reinherit.title", new Dimension(350, 300), 1, new RegionInheritRequest(), false, forStash);  
  
      workingRegions_ = workingRegions;
      parentRegions_ = parentRegions;
      
      est_ = new EditableTable(uics_, dacx_, new RegionInheritTableModel(uics_, dacx_), parent_);
      EditableTable.TableParams etp = new EditableTable.TableParams();
      etp.addAlwaysAtEnd = true;
      etp.singleSelectOnly = true;
      etp.buttons = EditableTable.NO_BUTTONS;
      addTable(est_.buildEditableTable(etp), 8);
      finishConstruction();
      displayProperties();
    } 
     
    public boolean dialogIsModal() {
      return (true);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
    
    @Override
    protected boolean stashForOK() {
      RegionInheritRequest crq = (RegionInheritRequest)request_;
      crq.regionResult = (((RegionInheritTableModel)est_.getModel()).applyValues());
      return (crq.regionResult != null);
    } 
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      RegionInheritRequest crq = (RegionInheritRequest)request_;
      crq.regionResult = (((RegionInheritTableModel)est_.getModel()).applyValues());
      if (crq.regionResult == null) {
        return (false);
      }
      crq.haveResult = true;
      return (true);
    }
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
     
    /***************************************************************************
    **
    ** Return results
    ** 
    */
    
    public List<InstanceInstructionSet.RegionInfo> getRegions() {
      RegionInheritRequest crq = (RegionInheritRequest)request_;
      return (crq.regionResult);
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
      List<AnnotIISRI> entries = tableEntries();
      est_.getModel().extractValues(entries);
      return;
    }
    
    /***************************************************************************
    **
    ** figure out the table entries
    ** 
    */
    
    private List<AnnotIISRI> tableEntries() {    
      ArrayList<AnnotIISRI> retval = new ArrayList<AnnotIISRI>();
      int pSize = parentRegions_.size();
      int wSize = workingRegions_.size();      
      for (int i = 0; i < pSize; i++) {
        AnnotIISRI val = new AnnotIISRI();
        retval.add(val);
        val.ri = parentRegions_.get(i);
        boolean match = false;
        for (int j = 0; j < wSize; j++) {
          InstanceInstructionSet.RegionInfo wRegion = workingRegions_.get(j);  
          if (DataUtil.keysEqual(val.ri.abbrev, wRegion.abbrev)) {
            val.checked = new Boolean(true);
            match = true;
            break;
          }
        }
        if (!match) {
          val.checked = new Boolean(false);
        }
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
    ** Used for transfer to data table
    */
    
    class AnnotIISRI {
      InstanceInstructionSet.RegionInfo ri;
      Boolean checked;
    }
      
    /***************************************************************************
    **
    ** Used for the data table
    */
    
    class RegionInheritTableModel extends EditableTable.TableModel {
      
      private final static int REGION_  = 0;
      private final static int INCLUDE_ = 1;
      private final static int NUM_COL_ = 2;
      
      private final static int HIDDEN_ABBREV_ = 0;
      private final static int NUM_HIDDEN_    = 1;
      
      private static final long serialVersionUID = 1L;
    
      RegionInheritTableModel(UIComponentSource uics, DataAccessContext dacx) {
        super(uics, dacx, NUM_COL_);
        colNames_ = new String[] {"reinherit.region",
                                  "reinherit.include"};
        colClasses_ = new Class[] {String.class,
                                   Boolean.class};
        canEdit_ = new boolean[] {false,
                                  true};    
        addHiddenColumns(NUM_HIDDEN_);
      }
   
      public List<AnnotIISRI> getValuesFromTable() {
        ArrayList<AnnotIISRI> retval = new ArrayList<AnnotIISRI>();
        for (int i = 0; i < rowCount_; i++) {
          String regName = (String)columns_[REGION_].get(i);
          Boolean inc = (Boolean)columns_[INCLUDE_].get(i);
          String abbrev = (String)hiddenColumns_[HIDDEN_ABBREV_].get(i);
          AnnotIISRI val = new AnnotIISRI();
          val.ri = new InstanceInstructionSet.RegionInfo(regName, abbrev);
          val.checked = inc; 
          retval.add(val);
        }      
        return (retval);
      }
      
     
      public void extractValues(List prsList) {
        super.extractValues(prsList);
        Iterator<AnnotIISRI> rit = prsList.iterator();
        while (rit.hasNext()) {
          AnnotIISRI val = rit.next(); 
          columns_[REGION_].add(val.ri.name);  
          columns_[INCLUDE_].add(val.checked);
          hiddenColumns_[HIDDEN_ABBREV_].add(val.ri.abbrev);
        }
        return;
      }
  
      List<InstanceInstructionSet.RegionInfo> applyValues() {
        List<AnnotIISRI> vals = getValuesFromTable();
        if (vals.isEmpty()) {
          return (null);
        }
        ArrayList<InstanceInstructionSet.RegionInfo> retval = new ArrayList<InstanceInstructionSet.RegionInfo>();
        Iterator<AnnotIISRI> rit = vals.iterator();
        while (rit.hasNext()) {
          AnnotIISRI val = rit.next(); 
          if (val.checked.booleanValue()) {
            retval.add(val.ri);
          }
        }
        return (retval);
      }
    }
  }

  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class RegionInheritRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public List<InstanceInstructionSet.RegionInfo> regionResult;
      
    public void clearHaveResults() {
      haveResult = false;
      return;
    }
	public void setHasResults() {
		this.haveResult = true;
		return;
	}  
    public boolean haveResults() {
      return (haveResult);
    }
    public boolean isForApply() {
      return (false);
    }
  }
}
