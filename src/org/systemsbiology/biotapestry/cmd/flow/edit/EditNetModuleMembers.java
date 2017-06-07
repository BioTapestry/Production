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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.EditModuleMemberDialog;

/****************************************************************************
**
** Handle adding net module members via dialog
*/

public class EditNetModuleMembers extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditNetModuleMembers() {
    name = "modulePopup.editModuleMemberDisplay";
    desc = "modulePopup.editModuleMemberDisplay";
    mnem =  "modulePopup.editModuleMemberDisplayMnem";
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {  
    return (new StepState(dacx));
  }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    // Can only edit members via dialog if we are dynamic:
    return (rcx.currentGenomeIsADynamicInstance());
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
        StepState ans = new StepState(cfh);
        next = ans.stepDoIt();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {

    private String interID_;
       
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      interID_ = inter.getObjectID();
      return;
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {  
      String ovrKey = dacx_.getOSO().getCurrentOverlay(); 
      if (!dacx_.currentGenomeIsADynamicInstance()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }  
      DynamicGenomeInstance dgi = dacx_.getCurrentGenomeAsDynamicInstance();
      String genomeKey = dacx_.getCurrentGenomeID();
      String proxID = dgi.getProxyID();
      DynamicInstanceProxy dip = dacx_.getGenomeSource().getDynamicProxy(proxID);
      GenomeInstance gi = dip.getStaticVfgParent();
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_);     
      EditModuleMemberDialog emmd = new EditModuleMemberDialog(uics_, rcx, genomeKey, gi.getID(), ovrKey, interID_, uFac_);
      emmd.setVisible(true);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
