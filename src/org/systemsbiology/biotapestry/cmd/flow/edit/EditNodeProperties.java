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
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.GenePropertiesDialog;
import org.systemsbiology.biotapestry.ui.dialogs.NodePropertiesDialog;

/****************************************************************************
**
** Handle editing node or gene properties
*/

public class EditNodeProperties extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean doGene_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditNodeProperties(boolean doGene) {         
    name = (doGene) ? "genePopup.Properties" : "nodePopup.Properties";
    desc = (doGene) ? "genePopup.Properties" : "nodePopup.Properties";
    mnem = (doGene) ? "genePopup.PropertiesMnem" : "nodePopup.PropertiesMnem";
    doGene_ = doGene;
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
    return (new StepState(doGene_, dacx));
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
        StepState ans = new StepState(doGene_, cfh);
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
    private boolean myDoGene_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doGene, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
      myDoGene_ = doGene;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean doGene, ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
      myDoGene_ = doGene;
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
      NodeProperties np = dacx_.getCurrentLayout().getNodeProperties(interID_);        
      if (myDoGene_) {
        GenePropertiesDialog gpd = new GenePropertiesDialog(uics_, dacx_, hBld_, np, uFac_);
        gpd.setVisible(true);              
      } else {
        NodePropertiesDialog npd = new NodePropertiesDialog(uics_, dacx_, hBld_, uFac_, np);
        npd.setVisible(true);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
}
