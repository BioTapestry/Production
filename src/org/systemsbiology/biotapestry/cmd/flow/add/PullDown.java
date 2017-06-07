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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.PullDownFromRootFrame;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle pulling down parent network elements into current network
*/

public class PullDown extends AbstractControlFlow implements ControlFlow.FlowForMainToggle {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
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
  
  public PullDown() {
    name =  "command.Pulldown" ;
    desc = "command.Pulldown";
    icon =  "Inherit24.gif";
    mnem = "command.PulldownMnem"; 
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
    return (cache.canPullDown());
  }
  
  /***************************************************************************
  **
  ** Answer if we can be pushed
  ** 
  */
 
  @Override
  public boolean canPush(int pushCondition) {
    return ((pushCondition & MainCommands.SKIP_PULLDOWN_PUSH) == 0x00);
  }
  
  /***************************************************************************
  **
  ** Just mess with the underlying toggle state; nothing else
  ** 
  */
  
  public void directCheck(boolean isActive, CmdSource cSrc) {
    cSrc.setAmPulling(isActive);
    return;
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
        StepState ans = new StepState(cfh); 
        next = ans.stepStart();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepStart")) {
          next = ans.stepStart();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();
        } else if (ans.getNextStep().equals("stepContinuePullDown")) {
          next = ans.stepContinuePullDown();
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
  ** Called as new X, Y info comes in to place the node.
  */
  
  @Override    
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    StepState ans = (StepState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    ans.setNextStep("stepContinuePullDown");
    ans.getDACX().setPixDiam(pixDiam);
    return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans));
  }
   
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.ButtonMaskerCmdState {

    private boolean rootInstance_;
    private int x;
    private int y;  
 
    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      rootInstance_ = false;
      GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
      rootInstance_ = (parent == null);    
      nextStep_ = "stepStart";  
    }
 
    /***************************************************************************
    **
    ** Button masking semantics
    */
        
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH | MainCommands.SKIP_PULLDOWN_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    /***************************************************************************
    **
    ** Kick things off
    */
      
    private DialogAndInProcessCmd stepStart() {
      //
      // First thing to do is to toggle the pulldown state.
      //     
      boolean nowPulling = !cmdSrc_.getAmPulling();
      cmdSrc_.setAmPulling(nowPulling);
      DialogAndInProcessCmd retval;
      if (nowPulling) { 
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          ResourceManager rMan = dacx_.getRMan();
          String message = rMan.getString("instructWarning.PullDownMessage");
          String title = rMan.getString("instructWarning.PullDownTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
          retval = new DialogAndInProcessCmd(suf, this);
        } else {
          retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        }           
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
        retval.suPanelMode = PanelCommands.Mode.NO_MODE; // Or something more?
        //appState_.getPanelCmds().cancelAddMode(PanelCommands.CANCEL_ADDS_SKIP_PULLDOWNS);   
      }
      nextStep_ = "stepSetToMode";
      return (retval); 
    }

    /***************************************************************************
    **
    ** Install a mouse handler
    */
    
    private DialogAndInProcessCmd stepSetToMode() {
      if (rootInstance_) {
        PullDownFromRootFrame pdfrd = new PullDownFromRootFrame(uics_, dacx_, uFac_, cmdSrc_);
        uics_.getCommonView().registerPullFrame(pdfrd);
        pdfrd.show(); // NEED TO USE THIS...OVERRIDDEN IN pdrfd class!
      }
      
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (!rootInstance_) ? PanelCommands.Mode.PULL_DOWN :  PanelCommands.Mode.PULL_DOWN_ROOT_INSTANCE;
      return (retval);
    } 
      
    /***************************************************************************
    **
    ** Handle the pull down clicks
    */  
   
    private DialogAndInProcessCmd stepContinuePullDown() {
      if (rootInstance_) {
        uics_.getSUPanel().giveErrorFeedback();
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }        
      
      if (dacx_.currentGenomeIsAnInstance()) {  
        GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        if (parent != null) {            
          ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.UNSELECTED;
          try {
            if (dacx_.currentGenomeIsADynamicInstance()) {
              result = doGroupAdd(x, y);
            } else {
              //
              // First check if group needs to be added, to keep from 
              // selecting items hidden by an unadded group.
              //
              result = doGroupAdd(x, y);
              if ((result == ServerControlFlowHarness.ClickResult.UNSELECTED)  || (result == ServerControlFlowHarness.ClickResult.PRESENT)) {
                result = doLinkPulldown(x, y);
              }
              if (result == ServerControlFlowHarness.ClickResult.UNSELECTED) {
                result = doNodePulldown(x, y);           
              }
            }
          } finally {
           // isPullDownMode_ = false;
          //  appState_.getCommonView().enableControls();              
          //  appState_.getCursorMgr().showDefaultCursor();
          }
          if ((result == ServerControlFlowHarness.ClickResult.ERROR) || (result == ServerControlFlowHarness.ClickResult.UNSELECTED)) {
            uics_.getSUPanel().giveErrorFeedback();
          }
          uics_.getSUPanel().drawModel(false);            
        }
      }
     return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this)); 
    }
    
    /***************************************************************************
    **
    ** Handle group adds
    */  
      
    private ServerControlFlowHarness.ClickResult doGroupAdd(int x, int y) {
    
      x = (int)((Math.round(x / 10.0)) * 10.0);
      y = (int)((Math.round(y / 10.0)) * 10.0); 
      if (dacx_.currentGenomeIsAnInstance()) {
        GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        if (parent != null) {    
          Intersection inter = uics_.getGenomePresentation().selectGroupFromParent(x, y, dacx_);
          if (inter == null) {
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          String startID = inter.getObjectID();
          int genCount = parent.getGeneration();        
          String inherit = Group.buildInheritedID(startID, genCount);
          Group subsetGroup = parent.getGroup(inherit);
          if (subsetGroup == null) {
            return (ServerControlFlowHarness.ClickResult.ERROR);
          }
          genCount = dacx_.getCurrentGenomeAsInstance().getGeneration();        
          inherit = Group.buildInheritedID(startID, genCount);
          Group checkGroup = dacx_.getCurrentGenomeAsInstance().getGroup(inherit);
          if (checkGroup != null) { // already present
            return (ServerControlFlowHarness.ClickResult.PRESENT);
          }
          GroupCreationSupport handler = new GroupCreationSupport(uics_, uFac_);
          handler.addNewGroupToSubsetInstance(dacx_, subsetGroup, null);
          return (ServerControlFlowHarness.ClickResult.SELECTED);
        }
      }
      throw new IllegalArgumentException();
    }
   
    /***************************************************************************
    **
    ** Handle node pulls
    */  
    
    private ServerControlFlowHarness.ClickResult doNodePulldown(int x, int y) {
      String currentOverlay = dacx_.getOSO().getCurrentOverlay();
      
      x = (int)((Math.round(x / 10.0)) * 10.0);
      y = (int)((Math.round(y / 10.0)) * 10.0); 
      if (dacx_.currentGenomeIsAnInstance()) {
        GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        if (parent != null) {    
          List<Intersection> itemList = uics_.getGenomePresentation().selectFromParent(x, y, dacx_);
          Intersection inter = (new IntersectionChooser(true, dacx_).intersectionRanker(itemList));
          if (inter == null) {
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          String startID = inter.getObjectID();
          NodeInstance subsetNode = (NodeInstance)parent.getNode(startID);
          if (subsetNode == null) {
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          //
          // Make sure the region is here: FIXES BT-02-15-08:1
          //
          Group group = parent.getGroupForNode(startID, GenomeInstance.MAIN_GROUP_AS_FALLBACK);
          if (group == null) {  // should not happen...
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          int genCount = dacx_.getCurrentGenomeAsInstance().getGeneration();        
          String inherit = Group.buildInheritedID(group.getID(), genCount);
          Group checkGroup = dacx_.getCurrentGenomeAsInstance().getGroup(inherit);
          if (checkGroup == null) { // we don't have the group!
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }

          NodeInstance checkNode = (NodeInstance)dacx_.getCurrentGenome().getNode(startID);
          if (checkNode != null) { // already present
            int activity = checkNode.getActivity();
            if (activity != NodeInstance.ACTIVE) {
              ResourceManager rMan = dacx_.getRMan();
              boolean doGene = (checkNode.getNodeType() == Node.GENE);
              JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                            rMan.getString((doGene) ? "addGene.PresentButInactive" 
                                                                    : "addNode.PresentButInactive"), 
                                            rMan.getString((doGene) ? "addGene.MakeActiveTitle" 
                                                                    : "addNode.MakeActiveTitle"),
                                            JOptionPane.ERROR_MESSAGE);            
              return (ServerControlFlowHarness.ClickResult.CANCELLED);
            } else {
              return (ServerControlFlowHarness.ClickResult.ERROR);
            }
          }

          boolean doGene = (subsetNode.getNodeType() == Node.GENE);

          // Gotta check for intersection before adding, since adding node will change module
          // bounds to not include the newly added node:
          //
          List<String> modCandidates = null;
          if (moduleOnDisplay()) {
            NodeProperties np = dacx_.getCurrentLayout().getNodeProperties(startID);
            Point2D nodeLoc = np.getLocation();
            AddNodeSupport.NetModuleIntersector nmi = new AddNodeSupport.NetModuleIntersector(uics_, dacx_);
            modCandidates = nmi.netModuleIntersections((int)nodeLoc.getX(), (int)nodeLoc.getY(), 0.0);
          }

          UndoSupport support = uFac_.provideUndoSupport((doGene) ? "undo.addGene" : "undo.addNode", dacx_);
          PropagateSupport.addNewNodeToSubsetInstance(dacx_, subsetNode, support);
          if (modCandidates != null) {
            Node inSub = dacx_.getCurrentGenome().getNode(startID);
            AddCommands.drawIntoNetModuleSupport(inSub, modCandidates, dacx_, currentOverlay, support);
          }
          support.finish();

          return (ServerControlFlowHarness.ClickResult.SELECTED);
        }
      }
      throw new IllegalArgumentException(); 
    }
   
    /***************************************************************************
    **
    ** Helper
    */  
    
    private boolean moduleOnDisplay() {
      String currentOverlay = dacx_.getOSO().getCurrentOverlay();
      TaggedSet currentNetMods = dacx_.getOSO().getCurrentNetModules();           
      if (currentOverlay == null) {
        return (false);
      }
      return (!currentNetMods.set.isEmpty());
    }
 
    /***************************************************************************
    **
    ** Handle link pulls
    */ 
    
    private ServerControlFlowHarness.ClickResult doLinkPulldown(int x, int y) {
  
      x = (int)((Math.round(x / 10.0)) * 10.0);
      y = (int)((Math.round(y / 10.0)) * 10.0); 
      if (dacx_.currentGenomeIsAnInstance()) {
        GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        //
        // Handles the subset inclusion case
        //
        if (parent != null) {    
          List<Intersection> itemList = uics_.getGenomePresentation().selectFromParent(x, y, dacx_);
          Intersection inter = (new IntersectionChooser(true, dacx_).intersectionRanker(itemList));
          if (inter == null) {
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          Linkage lnk = parent.getLinkage(inter.getObjectID());
          if (lnk == null) {
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }
          //
          // The intersection returns some linkage ID, but it may not be the one we want.
          // We want to find all linkages that pass through the intersected LinkSegment,
          // and pull in those.
          //
          HashSet<String> linkagesToPull = new HashSet<String>();
          BusProperties lp = dacx_.getCurrentLayout().getLinkProperties(inter.getObjectID());
          LinkSegmentID segID = inter.segmentIDFromIntersect();       
          Set<String> resolved = lp.resolveLinkagesThroughSegment(segID);
          linkagesToPull.addAll(resolved);

          //
          // Cull out anybody who is not in the parent (can this happen?) or is already
          // in the child.  If nobody is left, we exit with false.  We also need to find any 
          // sources and targets that are not present.  If there are any, we need to let the 
          // user add them.  
          //

          HashSet<LinkageInstance> pullSurvivors = new HashSet<LinkageInstance>();
          HashSet<String> nodesToAdd = new HashSet<String>();
          Iterator<String> pit = linkagesToPull.iterator();
          while (pit.hasNext()) {
            String linkageID = pit.next();
            LinkageInstance subsetLink = (LinkageInstance)parent.getLinkage(linkageID);
            if (subsetLink == null) {
              continue;
            }        
            LinkageInstance checkLink = (LinkageInstance)dacx_.getCurrentGenome().getLinkage(linkageID);        
            if (checkLink != null) { // already present
              continue;
            }
            String srcKey = subsetLink.getSource();          
            String trgKey = subsetLink.getTarget();
            NodeInstance srcNode = (NodeInstance)dacx_.getCurrentGenome().getNode(srcKey);
            NodeInstance trgNode = (NodeInstance)dacx_.getCurrentGenome().getNode(trgKey);
            // Don't let it go in if groups aren't there
            // This check addresses BT-06-17-05:4:
            // FIX TO FIX:  Allow it to go if src AND target is already present in the model.
            // (Handles intercells that are outside region block)
            // 3/25/08: Added code to handle null group for node
            if ((srcNode == null) || (trgNode == null)) {
              Group srcGrpInParent = parent.getGroupForNode(srcKey, GenomeInstance.MAIN_GROUP_AS_FALLBACK);
              if (srcGrpInParent == null) {
                continue;
              }
              Group srcGrpInChild = dacx_.getCurrentGenomeAsInstance().getGroup(Group.addGeneration(srcGrpInParent.getID()));
              if (srcGrpInChild == null) {
                continue;
              }
              Group trgGrpInParent = parent.getGroupForNode(trgKey, GenomeInstance.MAIN_GROUP_AS_FALLBACK);
              if (trgGrpInParent == null) {
                continue;
              }
              Group trgGrpInChild = dacx_.getCurrentGenomeAsInstance().getGroup(Group.addGeneration(trgGrpInParent.getID()));
              if (trgGrpInChild == null) {
                continue;
              }
            }

            pullSurvivors.add(subsetLink);
            if (srcNode == null) {
              nodesToAdd.add(srcKey);
            }

            if (trgNode == null) {
              nodesToAdd.add(trgKey);
            }
          }

          if (pullSurvivors.size() == 0) {
            return (ServerControlFlowHarness.ClickResult.ERROR);
          }

          if (nodesToAdd.size() > 0) {
            ResourceManager rMan = dacx_.getRMan();          
            int include = 
              JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                            rMan.getString("addLink.NeedSrcTarg"), 
                                            rMan.getString("addLink.NeedSrcTargTitle"),
                                            JOptionPane.YES_NO_CANCEL_OPTION);        
            if (include != JOptionPane.YES_OPTION) {
              return (ServerControlFlowHarness.ClickResult.CANCELLED);
            }
          }

          PropagateSupport.addNewLinkWithNodesToSubsetInstance(dacx_, parent, pullSurvivors, nodesToAdd, uFac_);      
          return (ServerControlFlowHarness.ClickResult.SELECTED);
        }
      }
      throw new IllegalArgumentException(); 
    }  
  }
}
