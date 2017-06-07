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
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Dialog box for specifying regions used in build instructions
*/

public class RegionSetupDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RegionSetupDialogFactory(ServerControlFlowHarness cfh) {
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
        return (new DesktopDialog(cfh, dniba.workingRegions, false));  
      case WEB:
      default:
        throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
    }   
  }
  
  /***************************************************************************
  **
  ** Get the stashing version of the dialog
  */  
  
  public RegionSetupDialogFactory.DesktopDialog getStashDialog(DialogBuildArgs ba) {   
    BuildArgs dniba = (BuildArgs)ba;
    return (new DesktopDialog(cfh, dniba.workingRegions, true)); 
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
          
    public BuildArgs(Genome genome, List<InstanceInstructionSet.RegionInfo> workingRegions) {
      super(genome);
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
    
    private static final long serialVersionUID = 1L;
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor. Note this dialog can either be used remotely, or locally
    ** as a stashing dialog.
    */ 
  
    public DesktopDialog(ServerControlFlowHarness cfh, List<InstanceInstructionSet.RegionInfo> workingRegions, boolean forStash) { 
      super(cfh, "rsedit.title", new Dimension(650, 300), 1, new RegionSetupRequest(), false, forStash);         
      est_ = new EditableTable(uics_, dacx_, new RegionSetupTableModel(uics_, dacx_), parent_);
      EditableTable.TableParams etp = new EditableTable.TableParams();
      etp.addAlwaysAtEnd = false;
      etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
      etp.singleSelectOnly = true;
      addTable(est_.buildEditableTable(etp), 8);
      finishConstruction();
      est_.getModel().extractValues((workingRegions == null) ? new ArrayList<InstanceInstructionSet.RegionInfo>() : workingRegions) ;  
    }
    
    /***************************************************************************
    **
    ** Return results
    ** 
    */
    
    public List<InstanceInstructionSet.RegionInfo> getRegions() {
      RegionSetupRequest crq = (RegionSetupRequest)request_;
      return (crq.regionResult);
    }  
  
    public boolean dialogIsModal() {
      return (true);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Stash our results for later interrogation.
    ** 
    */
    
    @Override
    protected boolean stashForOK() {
      RegionSetupRequest crq = (RegionSetupRequest)request_;
      crq.regionResult = ((RegionSetupTableModel)est_.getModel()).applyValues();
      return (crq.regionResult != null);
    } 
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      RegionSetupRequest crq = (RegionSetupRequest)request_;
      crq.regionResult = ((RegionSetupTableModel)est_.getModel()).applyValues();
      if (crq.regionResult == null) {
        return (false);
      }
      crq.haveResult = true;
      return (true);
    }
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /***************************************************************************
    **
    ** The table
    */
  
    class RegionSetupTableModel extends EditableTable.TableModel {
  
      private final static int REGION_  = 0;
      private final static int ABBREV_  = 1; 
      private final static int NUM_COL_ = 2;
      
      private static final long serialVersionUID = 1L;
      
      RegionSetupTableModel(UIComponentSource uics, DataAccessContext dacx) {
        super(uics, dacx, NUM_COL_);
        colNames_ = new String[] {"rsdedit.region",
                                  "rsdedit.abbrev"};
        colClasses_ = new Class[] {String.class,
                                   String.class};
      }    
     
      public List<InstanceInstructionSet.RegionInfo> getValuesFromTable() {
        ArrayList<InstanceInstructionSet.RegionInfo> retval = new ArrayList<InstanceInstructionSet.RegionInfo>();
        for (int i = 0; i < this.rowCount_; i++) {
          InstanceInstructionSet.RegionInfo ri = 
            new InstanceInstructionSet.RegionInfo((String)columns_[REGION_].get(i), 
                                                  (String)columns_[ABBREV_].get(i));
          retval.add(ri);
        }
        return (retval);
      }
       
      public void extractValues(List prsList) {
        super.extractValues(prsList);
        Iterator<InstanceInstructionSet.RegionInfo> rit = prsList.iterator();
        while (rit.hasNext()) {
          InstanceInstructionSet.RegionInfo region = rit.next();
          columns_[REGION_].add(region.name);
          columns_[ABBREV_].add(region.abbrev);
        }
        return;
      }
      
      List<InstanceInstructionSet.RegionInfo> applyValues() {
        List<InstanceInstructionSet.RegionInfo> vals = getValuesFromTable();
        if (vals.isEmpty()) {
          return (null);
        }
        
        //
        // Make sure the regions are OK.  Region names must be unique and non-blank. Same with
        // abbreviations, which must be short (<= 3 chars)
        //
        
        ResourceManager rMan = dacx_.getRMan();
        ArrayList<InstanceInstructionSet.RegionInfo> seenRegions = new ArrayList<InstanceInstructionSet.RegionInfo>();
        ArrayList<String> seenNames = new ArrayList<String>();
        ArrayList<String> seenAbbrevs = new ArrayList<String>();      
        int size = vals.size();
        if (size == 0) {
          String message = rMan.getString("rsedit.mustHaveRegion");
          String title = rMan.getString("rsedit.badRegionTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
          cfh_.showSimpleUserFeedback(suf);
          return (null);
        }
        
        for (int i = 0; i < size; i++) {
          InstanceInstructionSet.RegionInfo ri = vals.get(i);
          String name = ri.name; 
          if ((name == null) || (name.trim().equals(""))) {
            String message = rMan.getString("rsedit.badRegion");
            String title = rMan.getString("rsedit.badRegionTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (null);
          }
          
          name = name.trim();
          
          if (DataUtil.containsKey(seenNames, name)) {
            String message = rMan.getString("rsedit.dupRegion");
            String title = rMan.getString("rsedit.badRegionTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (null);
          }
          
          seenNames.add(name);
                  
          String abbrev = ri.abbrev;  
          if ((abbrev == null) || (abbrev.trim().equals(""))) {
            String message = rMan.getString("rsedit.badAbbrev");
            String title = rMan.getString("rsedit.badRegionTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (null);
          }
          
          abbrev = abbrev.trim();
          
          if (abbrev.length() > 3) {
            String message = rMan.getString("rsedit.longAbbrev");
            String title = rMan.getString("rsedit.badRegionTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (null);
          }        
  
          if (DataUtil.containsKey(seenAbbrevs, abbrev)) {
            String message = rMan.getString("rsedit.dupAbbrev");
            String title = rMan.getString("rsedit.badRegionTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (null);
          }
          
          seenAbbrevs.add(abbrev);
          seenRegions.add(new InstanceInstructionSet.RegionInfo(name, abbrev));
        }
        return (seenRegions);
      }
    }
  }
    
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class RegionSetupRequest implements ServerControlFlowHarness.UserInputs {
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
