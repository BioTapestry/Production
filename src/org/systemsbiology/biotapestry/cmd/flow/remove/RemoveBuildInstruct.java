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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.Iterator;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle deleting build instructions
*/

public class RemoveBuildInstruct extends AbstractControlFlow {

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
  
  public RemoveBuildInstruct() {
    name =  "command.DropAllInstructions";
    desc =  "command.DropAllInstructions";     
    mnem =  "command.DropAllInstructionsMnem";
  }
 
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.haveBuildInstructions());
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
  
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToRemove")) {
        next = ans.stepToRemove();
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
        
  public static class StepState extends AbstractStepState {
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepToRemove";
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
      
      if (!dacx_.getInstructSrc().haveBuildInstructions()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));      
      }
      
      String centered = UiUtil.convertMessageToHtml(dacx_.getRMan().getString("dropInstruct.dropQuestion")); 
      int result = JOptionPane.showConfirmDialog(uics_.getTopFrame(), centered,
                                                             dacx_.getRMan().getString("dropInstruct.messageTitle"),
                                                             JOptionPane.YES_NO_OPTION);
      if (result != 0) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      
      UndoSupport support = uFac_.provideUndoSupport("undo.dropBuildInstructions", dacx_);       
      dropAllInstructions(support);  
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));
      support.finish(); 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Handles dropping all build instructions.
    */ 
      
    private void dropAllInstructions(UndoSupport support) {
            
      DatabaseChange dc = dacx_.getInstructSrc().dropBuildInstructions();
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(dacx_, dc);
      support.addEdit(dcc);
    
      Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        String giid = gi.getID();
        if (dacx_.getInstructSrc().getInstanceInstructionSet(giid) != null) {
          DatabaseChange riis = dacx_.getInstructSrc().removeInstanceInstructionSet(giid);
          DatabaseChangeCmd riiscc = new DatabaseChangeCmd(dacx_, riis);
          support.addEdit(riiscc);
        }
      }      
      return;
    }  
  }
}
