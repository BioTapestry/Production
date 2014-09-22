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


package org.systemsbiology.biotapestry.cmd.flow.userPath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.SimpleRestrictedNameDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Handle create/delete of user tree path
*/

public class PathManage extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private boolean doCreate_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathManage(BTState appState, boolean doCreate) {
    super(appState);
    name =  (doCreate) ? "command.TreePathCreate" : "command.TreePathDelete";
    desc = (doCreate) ? "command.TreePathCreate" : "command.TreePathDelete";
    icon = (doCreate) ? "NewTreePath24.gif" : "DeleteTreePath24.gif";
    mnem =  (doCreate) ? "command.TreePathCreateMnem" : "command.TreePathDeleteMnem";
    doCreate_ = doCreate;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  public boolean externallyEnabled() {
    return ((doCreate_) ? false : true); // Second case actually managed manually!
  }

  /***************************************************************************
    **
   ** Answer if we are enabled
   ** 
   */
   
   @Override  
   public boolean isEnabled(CheckGutsCache cache) {
     return ((doCreate_) ? cache.haveSubmodelsOrOverlay() : true); // Second case actually managed manually!
   }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      ManageState ans;
      if (last == null) {
        ans = new ManageState(appState_, doCreate_, cfh, cfh.getDataAccessContext());      
      } else {
        ans = (ManageState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepOneForCreate")) {
        next = ans.stepOneForCreate();      
      } else if (ans.getNextStep().equals("stepTwoForCreate")) {
        next = ans.stepTwoForCreate(last);   
      } else if (ans.getNextStep().equals("stepThreeForCreate")) {
        next = ans.stepThreeForCreate();        
      } else if (ans.getNextStep().equals("stepForDelete")) {
        next = ans.stepForDelete();
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class ManageState implements DialogAndInProcessCmd.CmdState {
    
    private String nextStep_;    
    private BTState appState_;
    private String createName_;
    private ServerControlFlowHarness cfh;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public ManageState(BTState appState, boolean doCreate, ServerControlFlowHarness cfh, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = (doCreate) ? "stepOneForCreate" : "stepForDelete";
      this.cfh = cfh;
      dacx_ = dacx;
    }
      
    /***************************************************************************
    **
    ** Do the creation
    */ 
       
    private DialogAndInProcessCmd stepOneForCreate() {     
      String messageKey = "addPath.ChooseName";
      String titleKey = "addPath.ChooseTitle";
      String defaultName = appState_.getRMan().getString("addPath.defaultName");
      
      SimpleRestrictedNameDialogFactory.RestrictedNameBuildArgs ba = 
        new SimpleRestrictedNameDialogFactory.RestrictedNameBuildArgs(titleKey, messageKey, defaultName, null, false);
      SimpleRestrictedNameDialogFactory dgcdf = new SimpleRestrictedNameDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepTwoForCreate";
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Extract name
    */ 
        
    private DialogAndInProcessCmd stepTwoForCreate(DialogAndInProcessCmd cmd) {
      SimpleRestrictedNameDialogFactory.SimpleNameRequest crq = (SimpleRestrictedNameDialogFactory.SimpleNameRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }    
      createName_ = crq.nameResult;
      ResourceManager rMan = appState_.getRMan(); 
      String message = rMan.getString("addPath.addsFirstStop");
      String title = rMan.getString("addPath.addsFirstStopTitle");
      SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
      nextStep_ = "stepThreeForCreate";
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Done
    */ 
         
    private DialogAndInProcessCmd stepThreeForCreate() {
      appState_.getPathController().addAPath(createName_, dacx_);
      appState_.getPathControls().handlePathButtons();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  
    /***************************************************************************
    **
    ** Do the deletion
    */ 
        
    private DialogAndInProcessCmd stepForDelete() {
      appState_.getPathController().deleteCurrentPath(dacx_); 
      appState_.getPathControls().handlePathButtons();      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }  
}
