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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removing subgroup
*/

public class RemoveSubGroup extends AbstractControlFlow {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean forDeactivation_;
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
  
  public RemoveSubGroup(BTState appState, SubGroupArgs sga) {  
    super(appState);
    forDeactivation_ = sga.getForDeactivation();
    name = (forDeactivation_) ? "groupPopup.Deactivate" : "groupPopup.DeleteSubGroup";
    desc = (forDeactivation_) ? "groupPopup.Deactivate" : "groupPopup.DeleteSubGroup";
    mnem = (forDeactivation_) ? "groupPopup.DeactivateMnem" : "groupPopup.DeleteSubGroupMnem";
    groupID_ = sga.getGroupID();
    
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
    StepState retval = new StepState(appState_, forDeactivation_, groupID_, dacx);
    return (retval);
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
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.getNextStep().equals("stepToRemove")) {
          next = ans.stepToRemove();      
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
    
    private boolean myDeactive_;
    private String myGroupID_;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, boolean deactive, String groupID, DataAccessContext dacx) {
      appState_ = appState;
      myDeactive_ = deactive;
      myGroupID_ = groupID;
      nextStep_ = "stepToRemove";
      dacx_ = dacx;
    }
    
    /***************************************************************************
    **
    ** Set the popup params. All info previously installed!
    */ 
        
    public void setIntersection(Intersection inter) {
      return;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
      GenomeInstance parentGi = null;     
      Group parentGroup = null;
      if (myDeactive_) {
        parentGi = dacx_.getGenomeAsInstance().getVfgParent();
        Group parentModelGroup = parentGi.getGroup(Group.removeGeneration(myGroupID_));
        if (parentModelGroup.isUsingParent()) {
          Group group = dacx_.getGenomeAsInstance().getGroup(myGroupID_);
          String parentGroupID = group.getParentGroup(dacx_.getGenomeAsInstance());
          parentGroup = dacx_.getGenomeAsInstance().getGroup(parentGroupID);
        }
      }

      UndoSupport support = new UndoSupport(appState_, "undo.deleteSubgroup");

      //
      // If parent model has activated subgroup, then deactivating this
      // subgroup must also remove the parent group from this model.
      //
      if (!RemoveGroupSupport.deleteSubGroupFromModel(appState_, myGroupID_, dacx_, support)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      if (parentGroup != null) {
        RemoveGroupSupport.deleteGroupFromModel(appState_, parentGroup.getID(), dacx_, support, false);        
      }
      support.finish();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }

  /***************************************************************************
  **
  ** Arguments
  */
  
    public static class SubGroupArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"groupID", "forDeactivation"};
      classes_ = new Class<?>[] {String.class, Boolean.class};  
    }

    public String getGroupID() {
      return (getValue(0));
    }
     
    public boolean getForDeactivation() {
      return (Boolean.parseBoolean(getValue(1)));
    }
 
    public SubGroupArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
     
    public SubGroupArgs(String groupID, boolean forDeactivation) {
      super();
      setValue(0, groupID);
      setValue(1, Boolean.toString(forDeactivation));
      bundle();
    }
  } 
}
