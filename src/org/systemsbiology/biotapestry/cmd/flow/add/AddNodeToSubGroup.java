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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding node to subgroup
*/

public class AddNodeToSubGroup extends AbstractControlFlow {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    

  private String niID_;
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
  
  public AddNodeToSubGroup(BTState appState, NodeInGroupArgs nia) {
    super(appState);
    name = nia.getName();
    desc = nia.getName();
    niID_ = nia.getNiID();
    groupID_ = nia.getGroupID();
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
    StepState retval = new StepState(appState_, niID_, groupID_, dacx);
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
        if (ans.getNextStep().equals("stepToProcess")) {
          next = ans.stepToProcess();      
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
    
    private String myNiID_;
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
    
    public StepState(BTState appState, String niID, String groupID, DataAccessContext dacx) {
      appState_ = appState;
      myNiID_ = niID;
      myGroupID_ = groupID;
      nextStep_ = "stepToProcess";
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
       
    private DialogAndInProcessCmd stepToProcess() {
      GenomeInstance gi = dacx_.getGenomeAsInstance();
      addNodeToSubGroup(appState_, dacx_, gi, myGroupID_, myNiID_, null);
    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }
  
  /***************************************************************************
  **
  ** Add a node to a subgroup
  */  
 
  public static boolean addNodeToSubGroup(BTState appState, DataAccessContext dacx, GenomeInstance gi, String groupID, String niID, UndoSupport support) {
      
    //
    // Undo/Redo support
    //
    
    boolean gottaFinish = false;
    if (support == null) {
      support = new UndoSupport(appState, "undo.addNewSubGroup");
      gottaFinish = true;
    }
    Group group = gi.getGroup(groupID);
    GroupChange grc = group.addMember(new GroupMember(niID), gi.getID());
    if (grc != null) {
      GroupChangeCmd grcc = new GroupChangeCmd(appState, dacx, grc);
      support.addEdit(grcc);
      ModelChangeEvent mcev = new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);      
    }
    if (gottaFinish) {
      support.finish(); 
    }
    return (true); 
  } 
 
  /***************************************************************************
  **
  ** Arguments
  */
  
  public static class NodeInGroupArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    
    static {
      keys_ = new String[] {"niID", "groupID", "name"};  
    }
    
    public String getNiID() {
      return (getValue(0));
    }
     
    public String getGroupID() {
      return (getValue(1));
    }
   
    public String getName() {
      return (getValue(2));
    }
    
    public NodeInGroupArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    public NodeInGroupArgs(String niID, String groupID, String name) {
      super();
      setValue(0, niID);
      setValue(1, groupID);
      setValue(2, name);
      bundle();
    }
  } 
}
