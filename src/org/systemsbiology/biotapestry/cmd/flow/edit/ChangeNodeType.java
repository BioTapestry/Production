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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.ChangeNodeTypeDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Editing Notes
*/

public class ChangeNodeType extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ChangeNodeType() {
    name = "nodePopup.ChangeType";
    desc = "nodePopup.ChangeType";
    mnem = "nodePopup.ChangeTypeMnem";  
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
    ChangeState retval = new ChangeState(dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalStateException();
      } else {
        ChangeState ans = (ChangeState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepChangeDialog")) {
          next = ans.stepChangeDialog();
        } else if (ans.getNextStep().equals("stepExtractAndChange")) {
          next = ans.stepExtractAndChange(last);
        } else if (ans.getNextStep().equals("stepWillExit")) {
          next = ans.stepWillExit();
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
        
  public static class ChangeState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
     
    private String nodeID_;    
    private Integer newType_;
    private Node changeNode_;
    private int existingType_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public ChangeState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepBiWarning";
    }
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public ChangeState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepBiWarning";
    }

    /***************************************************************************
    **
    ** Preload init
    */ 
      
    public void setIntersection(Intersection nodeIntersect) {
      nodeID_ = nodeIntersect.getObjectID();
      return;
    } 
    
    /***************************************************************************
    **
    ** Warn of build instructions and init some values
    */
       
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        String message = dacx_.getRMan().getString("instructWarning.message");
        String title = dacx_.getRMan().getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
       } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      nextStep_ = "stepChangeDialog";
      return (daipc);     
    }
 
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepChangeDialog() { 
       
      changeNode_ = dacx_.getCurrentGenome().getNode(nodeID_);
      existingType_ = changeNode_.getNodeType();
          
      ChangeNodeTypeDialogFactory.ChangeNodeBuildArgs ba = new ChangeNodeTypeDialogFactory.ChangeNodeBuildArgs(existingType_);
      ChangeNodeTypeDialogFactory cntd = new ChangeNodeTypeDialogFactory(cfh_);  
      ServerControlFlowHarness.Dialog cfhd = cntd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractAndChange";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Just exit
    */
           
    private DialogAndInProcessCmd stepWillExit() { 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
 
    /***************************************************************************
    **
    ** Extract and install note properties
    */
       
    private DialogAndInProcessCmd stepExtractAndChange(DialogAndInProcessCmd cmd) {
         
      ChangeNodeTypeDialogFactory.NodeTypeChangeRequest crq = (ChangeNodeTypeDialogFactory.NodeTypeChangeRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
      newType_ = crq.typeResult;
      
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();

    //
    // While nodes can share names, they cannot share if they are genes
    // 
    
      if (newType_.intValue() == Node.GENE) { 
        boolean collision = false;
        boolean empty = false;
        if (changeNode_ instanceof NodeInstance) {
          String local = ((NodeInstance)changeNode_).getOverrideName();      
          String root = ((NodeInstance)changeNode_).getRootName();
          empty = ((local != null) && local.trim().equals("")) ||
                  ((root == null) || root.trim().equals(""));        
          collision = !empty && (((local != null) && dacx_.getFGHO().matchesExistingGeneOrNodeName(local, dacx_.getCurrentGenome(), nodeID_)) 
                                  || dacx_.getFGHO().matchesExistingGeneOrNodeName(root, dacx_.getCurrentGenome(), nodeID_));               
        } else {
          String root = changeNode_.getName();
          empty = (root == null) || root.trim().equals("");        
          collision = !empty && dacx_.getFGHO().matchesExistingGeneOrNodeName(root, dacx_.getCurrentGenome(), nodeID_);      
        }
        if (empty) {
          ResourceManager rMan = dacx_.getRMan();
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("changeType.EmptyName"), rMan.getString("changeType.ErrorTitle"));
          nextStep_ = "stepWillExit";
          return (new DialogAndInProcessCmd(suf, this));
        } else if (collision) {
          ResourceManager rMan = dacx_.getRMan();
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("changeType.BadName"), rMan.getString("changeType.ErrorTitle"));
          nextStep_ = "stepWillExit";
          return (new DialogAndInProcessCmd(suf, this));
        }
      }
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.changeNodeType", dacx_);     
  
      //
      // Characterize node pads:
      //
      
      Map<Integer, NodeProperties.PadLimits> limMap = NodeProperties.getFixedPadLimits();
      NodeProperties.PadLimits lim = limMap.get(newType_);    
      
      //
      // Get the root genome:
      //
          
      DBGenome rootGenome = dacx_.getDBGenome();
    
      
      HashMap<String, Set<String>> idMap = new HashMap<String, Set<String>>();                                                   
      
      //
      // Change the node type in the root model
      //
                                                         
      changeNodeTypeCore(dacx_, GenomeItemInstance.getBaseID(nodeID_), newType_.intValue(), 
                         lim, rootGenome, support, idMap);  
   
      //
      // Make the changes in all child models and layouts
      //
      
      Iterator<GenomeInstance> git = dacx_.getGenomeSource().getInstanceIterator();
      while (git.hasNext()) {
        GenomeInstance gi = git.next();
        changeNodeTypeCore(dacx_, GenomeItemInstance.getBaseID(nodeID_), newType_.intValue(), lim, gi, 
                           support, idMap);
      }
      Set<String> changed = idMap.keySet();
      
      //
      // Fix up the layouts to handle the change.
      //
      
      Iterator<Layout> lit = dacx_.getLayoutSource().getLayoutIterator();
      while (lit.hasNext()) {
        Layout lo = lit.next();
        if (changed.contains(lo.getTarget())) {
          Set<String> instSet = idMap.get(lo.getTarget());
          Iterator<String> isit = instSet.iterator();
          while (isit.hasNext()) {
            String nodeID = isit.next();
            Layout.PropChange lpc = lo.changeNodePropertiesType(nodeID, existingType_, newType_.intValue());
            support.addEdit(new PropChangeCmd(dacx_, new Layout.PropChange[] {lpc}));
            //
            // Code for Issue #241. If node is white, and the target type is not happy with white, change it
            // to black so it does not disappear:
            //
            NodeProperties np = lo.getNodeProperties(nodeID);
            String name = np.getColorName();
            if (name.equals("white")) {
              INodeRenderer ir = NodeProperties.buildRenderer(newType_.intValue());
              if (!ir.getTargetColor().equals("white")) {
                NodeProperties npc = np.clone();
                npc.setColor("black");
                lpc = lo.setNodeProperties(nodeID, npc);
                support.addEdit(new PropChangeCmd(dacx_, new Layout.PropChange[] {lpc}));
              }
            } 
          }
          LayoutChangeEvent lcev = new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
          support.addEvent(lcev);
        }
      }
      
      ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds, false, support);
         
      //
      // Flush dynamic cache
      //
      
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      
      //
      // Finish up the undo
      //
      
      support.finish();   
            
      //
      // DONE!
      //
        
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Core ops for changing the node type
    */  
   
    private static boolean changeNodeTypeCore(StaticDataAccessContext dacx, String backingID, int newType, NodeProperties.PadLimits lim,
                                              Genome genome, UndoSupport support, Map<String, Set<String>> idMap) {    
      //
      // Node not in genome -> skip it
      //
            
      
      Set<String> instances = null;
      if (genome instanceof GenomeInstance) {
        instances = ((GenomeInstance)genome).getNodeInstances(backingID);
        if (instances.size() == 0) {
          return (false);
        }
      } else {
        if (genome.getNode(backingID) == null) {
          throw new IllegalStateException();
        }
        instances = new HashSet<String>();
        instances.add(backingID);
      }
      idMap.put(genome.getID(), instances);
       
      ModelChangeEvent mcev = new ModelChangeEvent(dacx.getGenomeSource().getID(), genome.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);       
      // 
      // Change the node type, for each instance 
      //
      
      Iterator<String> iit = instances.iterator();
      while (iit.hasNext()) {
        String id = iit.next();      
        GenomeChange gc = genome.changeNodeType(id, newType);
        support.addEdit(new GenomeChangeCmd(dacx, gc));
  
        //
        // All linkages inbound and outbound may need landing and
        // launch pad tweaks
        //
      
        GenomeChange[] gca = genome.resolvePadChanges(id, lim);
        for (int i = 0; i < gca.length; i++) {
          support.addEdit(new GenomeChangeCmd(dacx, gca[i]));
        }
      }
      return (true);
    }
  }  
}
