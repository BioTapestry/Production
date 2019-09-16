/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.util.HashSet;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Factory for dialog boxes that create Cis-Reg Modules
*/

public class CisModuleCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CisModuleCreationDialogFactory(ServerControlFlowHarness cfh) {
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
   
    CisModBuildArgs dniba = (CisModBuildArgs)ba;
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.regions, dniba.unused, dniba.canGiveLeftOnly, dniba.canGiveRightOnly));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
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
  
  public static class CisModBuildArgs extends DialogBuildArgs { 
       
    List<DBGeneRegion> regions;
    List<String> unused;
    boolean canGiveLeftOnly;
    boolean canGiveRightOnly;
          
    public CisModBuildArgs(List<DBGeneRegion> regions, boolean canGiveLeftOnly, boolean canGiveRightOnly) {
      super(null);
      this.canGiveLeftOnly = canGiveLeftOnly;
      this.canGiveRightOnly = canGiveRightOnly;
      
      unused = new ArrayList<String>();
      HashSet<String> usedNames = new HashSet<String>();
      for (DBGeneRegion reg : regions) {
        if (!reg.isHolder()) {
          usedNames.add(DataUtil.normKeyWithGreek(reg.getName()));
        }
      }
      String[] greek_ = DBGeneRegion.getGreek();
      
      for (int i = 0; i < greek_.length; i++) {
        if (!usedNames.contains(greek_[i])) {
          unused.add(greek_[i]);
        }
      } 
      this.regions = regions;
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JComboBox nameField_;   
    private List<DBGeneRegion> regions_;
    private JRadioButton chooseTwo_;
    private JRadioButton chooseLeft_;
    private JRadioButton chooseRight_;
         
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
     
    public DesktopDialog(ServerControlFlowHarness cfh, List<DBGeneRegion> regions, List<String> greek, 
                         boolean canGiveLeftOnly, boolean canGiveRightOnly) {
      super(cfh, "ciscreate.title", new Dimension(500, 300), 2, new CisModRequest(), false);
      regions_ = regions;
  
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan_.getString("ciscreate.name"));
      nameField_ = new JComboBox(greek.toArray());
      nameField_.setEditable(true);
      addLabeledWidget(label, nameField_, false, false);
      
      int leftPad = Integer.MAX_VALUE;
      int rightPad = Integer.MIN_VALUE;
      
      for (DBGeneRegion dbgr : regions_) {
        if (!dbgr.isHolder()) {
          if (dbgr.getStartPad() < leftPad) {
            leftPad = dbgr.getStartPad();
          }
          if (dbgr.getEndPad() > rightPad) {
            rightPad = dbgr.getEndPad();
          }
        }
      }
     
      //
      // Build the layer type selection
      //
      
      ButtonGroup group = new ButtonGroup();
     
      chooseTwo_ = new JRadioButton(rMan_.getString("ciscreate.both"), true);            
      group.add(chooseTwo_);
      addWidgetFullRow(chooseTwo_, true, true);    

      chooseLeft_ = new JRadioButton(rMan_.getString("ciscreate.left"), false);            
      group.add(chooseLeft_);
      addWidgetFullRow(chooseLeft_, true, true);
   
      chooseRight_ = new JRadioButton(rMan_.getString("ciscreate.right"), false);            
      group.add(chooseRight_);
      addWidgetFullRow(chooseRight_, true, true);
      
      chooseLeft_.setEnabled(canGiveLeftOnly);
      chooseRight_.setEnabled(canGiveRightOnly);
      finishConstruction();
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
    protected boolean bundleForExit(boolean forApply) { 
      CisModRequest crq = (CisModRequest)request_;     
      crq.nameResult = ((String)(nameField_.getSelectedItem())).trim();

      // Blank names not allowed starting in v 7.0.1:
      
      if (crq.nameResult.equals("")) {
        String msg = rMan_.getString("ciscreate.badEmptyName");
        String title = rMan_.getString("ciscreate.badEmptyNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }

      for (DBGeneRegion dbgr : regions_) {
        if (DataUtil.normKeyWithGreek(dbgr.getName()).equals(DataUtil.normKeyWithGreek(crq.nameResult))) {
          String msg = rMan_.getString("ciscreate.badName");
          String title = rMan_.getString("ciscreate.badNameTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
          cfh_.showSimpleUserFeedback(suf);
          return (false);
        }
      }      
      
      if (chooseTwo_.isSelected()) {
        crq.buildType = CisModRequest.BuildExtent.BOTH_PADS;      
      } else if (chooseLeft_.isSelected()) {
        crq.buildType = CisModRequest.BuildExtent.PAD_LEFT;
      } else if (chooseRight_.isSelected()) {
        crq.buildType = CisModRequest.BuildExtent.PAD_RIGHT;
      } else {
        throw new IllegalStateException();
      }
 
      crq.haveResult = true;
      return (true);
    } 
  }
  
  /***************************************************************************
   **
   ** Return Results
   ** 
   */
   
   public static class CisModRequest implements ServerControlFlowHarness.UserInputs {
     
     public enum BuildExtent {BOTH_PADS, PAD_LEFT, PAD_RIGHT};
     
     private boolean haveResult;
     public String nameResult;
     public BuildExtent buildType;
        
     public void clearHaveResults() {
       haveResult = false;
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
