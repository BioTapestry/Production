/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of selections
*/

public class RemoveSelections extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

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
  
  public RemoveSelections(BTState appState) {
    super(appState);
    name =  "command.DeleteSelections";
    desc = "command.DeleteSelections";
    mnem =  "command.DeleteSelectionsMnem"; 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override
   public boolean isEnabled(CheckGutsCache cache) {
     return (cache.haveASelection());
   }

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
        ans = new StepState(appState_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepToWarn")) {
        next = ans.stepToWarn();      
      } else if (ans.getNextStep().equals("stepToCheckDataDelete")) {
        next = ans.stepToCheckDataDelete();   
      } else if (ans.getNextStep().equals("registerCheckDataDelete")) {
        next = ans.registerCheckDataDelete(last);       
      } else if (ans.getNextStep().equals("stepToRemove")) {
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {
    
    private DataAccessContext rcxT_;
    private String nextStep_;    
    private BTState appState_;
    private HashMap<String, Intersection> linkInter_;
    private RemoveSupport.DataDeleteQueryState ddqs_;
   
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepToWarn";
      rcxT_ = dacx;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToWarn() {
     
      // RE: Issue 163. Cut this off right at the start
      if (rcxT_.getGenome() instanceof DynamicGenomeInstance) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));       
      }
      
      Map<String, Intersection> selmap = appState_.getGenomePresentation().getSelections();

      //
      // Crank through the selections.  For selections that are linkages, 
      // blow them apart into segments if they are full intersections.
      // Then hand the list off to the delete command to figure out all
      // the linkages passing through the all the segments.
      //
      
      TreeSet<String> deadSet = new TreeSet<String>();
      linkInter_ = new HashMap<String, Intersection>();
      
      Iterator<String> selKeys = selmap.keySet().iterator();
      while (selKeys.hasNext()) {
        String key = selKeys.next();
        Intersection inter = selmap.get(key);
        Linkage link = rcxT_.getGenome().getLinkage(key);
        if (link != null) {
          if (inter.getSubID() == null) {
            inter = Intersection.fullIntersection(link, rcxT_, true);
          }
          linkInter_.put(key, inter);
        }
        Node node = rcxT_.getGenome().getNode(key);
        if (node != null) {
          deadSet.add(key);
        }
      }

      //
      // Nobody to delete -> get lost
      //
    
      if (deadSet.isEmpty() && linkInter_.isEmpty()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }

      ArrayList<String> deadList = new ArrayList<String>(deadSet);
      DialogAndInProcessCmd daipc;
      SimpleUserFeedback suf = RemoveSupport.deleteWarningHelperNew(appState_, rcxT_);
      if (suf != null) {
        daipc = new DialogAndInProcessCmd(suf, this);     
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      
      if (!rcxT_.genomeIsRootGenome() || (deadList.size() == 0)) {
        ddqs_ = new RemoveSupport.DataDeleteQueryState(deadList, daipc, rcxT_.genomeIsRootGenome());
        nextStep_ = mapNextStepToFunction(ddqs_.nextStep);
      } else {
        ddqs_ = new RemoveSupport.DataDeleteQueryState(deadList, daipc);
        nextStep_ = mapNextStepToFunction(ddqs_.nextStep);
      }
      return (daipc); 
    }
    
    /***************************************************************************
    **
    ** Mapping function
    */ 
       
    private String mapNextStepToFunction(RemoveSupport.DataDeleteQueryState.NextStep next) {
      switch (next) {
        case GO_DELETE:
          return ("stepToRemove");
        case CHECK_DATA_DELETE:
          return ("stepToCheckDataDelete");
        case REGISTER_DATA_DELETE:
          return ("registerCheckDataDelete");
        default:
          throw new IllegalArgumentException();
      }
    } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToCheckDataDelete() {    
      RemoveSupport.stepToCheckDataDelete(rcxT_, ddqs_, this);
      nextStep_ = mapNextStepToFunction(ddqs_.nextStep);
      return (ddqs_.retval);
    } 
  
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd registerCheckDataDelete(DialogAndInProcessCmd daipc) {
      RemoveSupport.registerCheckDataDelete(daipc, ddqs_, this);
      nextStep_ = mapNextStepToFunction(ddqs_.nextStep);
      return (ddqs_.retval);
    } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
      
   
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = (new FullGenomeHierarchyOracle(appState_)).getGlobalNetModuleLinkPadNeeds();
    
      UndoSupport support = new UndoSupport(appState_, "undo.deleteSelected");        
      appState_.getGenomePresentation().clearSelections(rcxT_, support);
      boolean didDelete = false;
   
      if (RemoveLinkage.deleteLinksFromModel(appState_, linkInter_, rcxT_, support)) {
        didDelete = true;
      }
  
      Iterator<String> dsit = ddqs_.deadList.iterator();
      while (dsit.hasNext()) {
        String deadID = dsit.next();
        boolean nodeRemoved = RemoveNode.deleteNodeFromModelCore(appState_, deadID, rcxT_, support, ddqs_.dataDelete, true);
        didDelete |= nodeRemoved;
      }
      
      if (globalPadNeeds != null) {
        ModificationCommands.repairNetModuleLinkPadsGlobally(appState_, rcxT_, globalPadNeeds,false, support);
      }    
   
      if (didDelete) {
        support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }
  
      support.finish();  // no matter what to handle selection clearing
      appState_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
  }
}
