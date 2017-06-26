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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveLinkage;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of selections
*/

public class SetSelectedInactive extends AbstractControlFlow {

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
  
  public SetSelectedInactive() {
    name =  "command.SetSelectedInactive";
    desc = "command.SetSelectedInactive";
    mnem =  "command.SetSelectedInactiveMnem";
    accel = "command.SetSelectedInactiveAccel";
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
     return (cache.haveASelection() && !cache.genomeIsRoot() && cache.isNotDynamicInstance());
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
        ans = new StepState(cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToInactivate")) {
        next = ans.stepToInactivate();
      } else if (ans.getNextStep().equals("stepWillExit")) {
        next = ans.stepWillExit();
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
        
  public static class StepState extends AbstractStepState  {
    
    private HashMap<String, Intersection> linkInter_;
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepToInactivate";
    }

    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToInactivate() {  
        
      Map<String, Intersection> selmap = uics_.getGenomePresentation().getSelections();

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
        Linkage link = dacx_.getCurrentGenome().getLinkage(key);
        if (link != null) {
          if (inter.getSubID() == null) {
            inter = Intersection.fullIntersection(link, dacx_, true);
          }
          linkInter_.put(key, inter);
        }
        Node node = dacx_.getCurrentGenome().getNode(key);
        if (node != null) {
          deadSet.add(key);
        }
      }

      //
      // Nobody to change -> get lost
      //
    
      if (deadSet.isEmpty() && linkInter_.isEmpty()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
       
      Set<String> thru = RemoveLinkage.resolveLinksThruIntersections(linkInter_, dacx_);
           
      for (String nodeID : deadSet) {
        NodeInstance node = (NodeInstance)dacx_.getCurrentGenome().getNode(nodeID);
        NodeInstance.ActivityState nias = new NodeInstance.ActivityState(NodeInstance.INACTIVE, 0.0);
        GenomeItemInstance.ActivityTracking tracking = node.calcActivityBounds(dacx_.getCurrentGenomeAsInstance());
        ActivityLevelSupport.Results check = ActivityLevelSupport.checkActivityBounds(tracking, nias);
        if (check != ActivityLevelSupport.Results.VALID_ACTIVITY) {
          SimpleUserFeedback suf = ActivityLevelSupport.sufForNode(check, dacx_);
          nextStep_ = "stepWillExit";
          return (new DialogAndInProcessCmd(suf, this));  
        }
      }
      
      
      for (String linkID : thru) {
        LinkageInstance link = (LinkageInstance)dacx_.getCurrentGenome().getLinkage(linkID);
        if (link == null) { // Good old VfN non-links problem!
          continue;
        }
        GenomeItemInstance.ActivityTracking tracking = link.calcActivityBounds(dacx_.getCurrentGenomeAsInstance());      
        ActivityLevelSupport.Results check = ActivityLevelSupport.checkActivityBounds(tracking, LinkageInstance.INACTIVE, 0.0);
        if (check != ActivityLevelSupport.Results.VALID_ACTIVITY) {
          SimpleUserFeedback suf = ActivityLevelSupport.sufForNode(check, dacx_);
          nextStep_ = "stepWillExit";
          return (new DialogAndInProcessCmd(suf, this));  
        }
      }
    
          
      UndoSupport support = uFac_.provideUndoSupport("undo.SelectedToInactive", dacx_); 
     
      boolean needEvent = false;
      
      for (String nodeID : deadSet) {
        NodeInstance node = (NodeInstance)dacx_.getCurrentGenome().getNode(nodeID);
        int activeSetting = node.getActivity();
        if (activeSetting != NodeInstance.INACTIVE) {
          NodeInstance copyNode = node.clone();
          copyNode.setActivity(NodeInstance.INACTIVE);
          GenomeChange gc = (node.getNodeType() == Node.GENE) ? dacx_.getCurrentGenome().replaceGene((Gene)copyNode) : dacx_.getCurrentGenome().replaceNode(copyNode);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
            support.addEdit(gcc);
            needEvent = true;
          }          
        }
      }
        
      for (String linkID : thru) {
        LinkageInstance link = (LinkageInstance)dacx_.getCurrentGenome().getLinkage(linkID);
        if (link == null) { // Good old VfN non-links problem!
          continue;
        }
        int activeSetting = link.getActivitySetting();
        if (activeSetting != LinkageInstance.INACTIVE) {
          GenomeChange gc = dacx_.getCurrentGenomeAsInstance().replaceLinkageInstanceActivity(linkID, LinkageInstance.INACTIVE, 0.0);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
            support.addEdit(gcc);
            needEvent = true;
          }          
        }
      }  

  
      if (needEvent) {
       ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
       support.addEvent(mcev);
      }
   
      support.finish();  // no matter what to handle selection clearing
      uics_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
    
    /***************************************************************************
    **
    ** Done
    */
           
    private DialogAndInProcessCmd stepWillExit() { 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
 } 
}
