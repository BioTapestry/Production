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

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Dialog box for editing model tree group nodes
*/

public class GroupNodePropertiesDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GroupNodePropertiesDialogFactory(ServerControlFlowHarness cfh) {
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
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.currentName));
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
    
    String currentName;
        
    public BuildArgs(String currentName) {
      super(null);
      this.currentName = currentName; 
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
  
    private JTextField nameField_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, String currentName) { 
      super(cfh, "mgncreate.title", new Dimension(500, 200), 2, new CreateRequest(), false);      
    
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan_.getString("mgncreate.name"));
      nameField_ = new JTextField(currentName);
      addLabeledWidget(label, nameField_, false, true);
      finishConstruction();
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
    ** 
    */
    
    @Override   
    protected boolean bundleForExit(boolean forApply) {
      CreateRequest crq = (CreateRequest)request_;           
      crq.nameResult = nameField_.getText().trim();   
      //
      // 
      //            
      if (!queBombForName(crq.nameResult)) {
        return (false);
      }
      crq.haveResult = true;
      return (true);
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    //////////////////////////////////////////////////////////////////////////// 
    
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
     
    private boolean queBombForName(String nameResult) {
      RemoteRequest daBomb = new RemoteRequest("queBombCheckSiblingNameMatch");
      daBomb.setStringArg("nameResult", nameResult); 
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      SimpleUserFeedback suf = dbres.getSimpleUserFeedback();
      if (suf != null) {
        cfh_.showSimpleUserFeedback(suf);
      }
      if (dbres.keepGoing() == RemoteRequest.Progress.STOP) {
        return (false);
      } else {
        return (true);
      }
    }
  }
  
  /***************************************************************************
   **
   ** User input data
   ** 
   */

   public static class CreateRequest implements ServerControlFlowHarness.UserInputs {
     private boolean haveResult; 
     public String nameResult;
   
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
     
 	public void setHasResults() {
		this.haveResult = true;
		return;
	}  
   } 
}
