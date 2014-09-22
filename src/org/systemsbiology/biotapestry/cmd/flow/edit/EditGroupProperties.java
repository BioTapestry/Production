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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.GroupPropertiesDialog;

/****************************************************************************
**
** Handle editing group properties
*/

public class EditGroupProperties extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private String groupID_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditGroupProperties(BTState appState, GroupArg arg) {
    super(appState);   
    name = "groupPopup.GroupProperties";
    desc = "groupPopup.GroupProperties";
    mnem =  "groupPopup.GroupPropertiesMnem";
    groupID_ = (arg.getForDeferred()) ? null : arg.getGroupID();
    
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {  
    return (new StepState(appState_, groupID_, dacx));
  }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(appState_, groupID_, cfh.getDataAccessContext());
        next = ans.stepDoIt();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.getNextStep().equals("stepDoIt")) {
          next = ans.stepDoIt();      
        } else {
          throw new IllegalStateException();
        }
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private String nextStep_;    
    private BTState appState_;
    private String myGroupID_;
    private String interID_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, String groupID, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepDoIt";
      myGroupID_ = groupID;
      dacx_ = dacx;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      if (myGroupID_ == null) {
        interID_ = inter.getObjectID();
      }
      return;
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {  
      GroupProperties gp = null;
      if (myGroupID_ == null) {
        gp = dacx_.getLayout().getGroupProperties(Group.getBaseID(interID_));
      } else {
        gp = dacx_.getLayout().getGroupProperties(Group.getBaseID(myGroupID_));
      }
      if (gp != null) {
        GroupPropertiesDialog gpd = new GroupPropertiesDialog(appState_, dacx_, gp, (myGroupID_ != null));
        gpd.setVisible(true);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
 
  /***************************************************************************
  **
  ** Control Flow Argument
  */  
  
  public static class GroupArg extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"groupID", "forDeferred"};
      classes_ = new Class<?>[] {String.class, Boolean.class};  
    }
    
    public String getGroupID() {
      return (getValue(0));
    }
    
    public boolean getForDeferred() {
      return (Boolean.parseBoolean(getValue(1)));
    }
    
    public GroupArg(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public GroupArg(String groupID, boolean forDeferred) {
      super();
      if (forDeferred) {
        if (groupID != null) {
          throw new IllegalArgumentException();
        }
        groupID = "DONT_CARE";
      }
      setValue(0, groupID);
      setValue(1, Boolean.toString(forDeferred));
      bundle();
    }
  } 
}
