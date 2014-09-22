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
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding node to module
*/

public class AddNodeToModule extends AbstractControlFlow {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    

  private String modID_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNodeToModule(BTState appState, NamedModuleArgs nma) {
    super(appState);
    name = nma.getName();
    desc = nma.getName();
    modID_ = nma.getID();
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
    StepState retval = new StepState(appState_, modID_, dacx);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) { 
    return (rcx.oso.getCurrentOverlay() != null);
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
        if (ans.getNextStep().equals("stepToAdd")) {
          next = ans.stepToAdd();      
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
    
    private Intersection intersect_;
    private String myModID_;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext rcx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, String modID, DataAccessContext dacx) {
      appState_ = appState;
      myModID_ = modID;
      nextStep_ = "stepToAdd";
      rcx_ = dacx;
    }
    
    /***************************************************************************
    **
    ** Set the popup params
    */ 
        
    public void setIntersection(Intersection inter) {
      intersect_ = inter;
      return;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToAdd() {
      String ovrKey = rcx_.oso.getCurrentOverlay();

      //
      // Changing module geometry may need module link pad fixups:  
    
      Layout.PadNeedsForLayout padFixups = rcx_.getLayout().findAllNetModuleLinkPadRequirements(rcx_);   
  
      UndoSupport support = new UndoSupport(appState_, "undo.addNodeToNetworkModule");    
      NetOverlayOwner owner = rcx_.getCurrentOverlayOwner();
      NetworkOverlay nol = owner.getNetworkOverlay(ovrKey);
      NetModule nmod = nol.getModule(myModID_);
      NetModuleChange nmc = owner.addMemberToNetworkModule(ovrKey, nmod, intersect_.getObjectID());
      if (nmc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, rcx_, nmc);
        support.addEdit(gcc);
      } 
     
      //
      // Complete the fixups:
      //
      
      AddCommands.finishNetModPadFixups(appState_, null, null, rcx_, padFixups, support);    
      
      support.addEvent(new ModelChangeEvent(rcx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));   
      support.finish();    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }   
  }

  /***************************************************************************
  **
  ** Arguments
  */
  
  public static class NamedModuleArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    
    static {
      keys_ = new String[] {"name", "id"};  
    }
    
    public String getName() {
      return (getValue(0));
    }
     
    public String getID() {
      return (getValue(1));
    }
   
    public NamedModuleArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    public NamedModuleArgs(String name, String id) {
      super();
      setValue(0, name);
      setValue(1, id);
      bundle();
    }
  }
}
