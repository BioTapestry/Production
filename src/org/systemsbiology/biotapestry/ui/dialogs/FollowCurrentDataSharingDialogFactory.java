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
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;

/****************************************************************************
**
** Factory to create dialogs for specifying following existing data sharing policy
** for new model tabs
*/

public class FollowCurrentDataSharingDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public FollowCurrentDataSharingDialogFactory(ServerControlFlowHarness cfh) {
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
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, (BuildArgs)ba));
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
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    String message;
    
    public BuildArgs(String message) {
      super(null);
      this.message = message;
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
    // PUBLIC CONSTANTS
    //
    ////////////////////////////////////////////////////////////////////////////   
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JCheckBox shareDataBox_;  
   
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, BuildArgs ba) { 
      super(cfh, "dataSharingPolicy.title", new Dimension(700, 500), 4, new JoinSharingRequest(), false);
      
      //
      // We are adding a new tab, and data sharing is already going on. Give the user the opportunity to
      // join in, or to treat the new tab as standalone.
      //
      
      //
      // Title
      //

      JLabel topLabel = new JLabel(rMan_.getString("dataSharingPolicy.currentPolicy"));
      addWidgetFullRow(topLabel, false);
      
      //
      // Multi-line label to state existing policy:
      //

      JLabel tabLabel = new JLabel(ba.message);
      addWidgetFullRow(tabLabel, false);
           
      //
      // Check box to join in, or not:
      //
      
      shareDataBox_ = new JCheckBox(rMan_.getString("dataSharingPolicy.joinTheFun"), false);
      addWidgetFullRow(shareDataBox_, false);
      
      finishConstruction(); 
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    ////////////////////////////////////////////////////////////////////////////
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
      
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
    protected boolean bundleForExit(boolean forApply) {
      JoinSharingRequest crq = (JoinSharingRequest)request_;
      crq.setForApply(false);
      crq.joinSharing = shareDataBox_.isSelected();
      crq.haveResult = true;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class JoinSharingRequest implements ServerControlFlowHarness.UserInputs {

    private boolean haveResult;
    public boolean joinSharing;
    private boolean forApply_;
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    } 

  	public void setHasResults() {
  		this.haveResult = true;
  		return;
  	}   
    
    public void setForApply(boolean forApply) {
      forApply_ = forApply;
      return;
    }
   
    public boolean isForApply() {
      return (forApply_);
    }
  } 
}
