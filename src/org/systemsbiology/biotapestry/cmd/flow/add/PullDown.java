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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
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
  
  public PullDown(BTState appState) {
    super(appState);  
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
 
  public boolean canPush(int pushCondition) {
    return ((pushCondition & MainCommands.SKIP_PULLDOWN_PUSH) == 0x00);
  }
  
  /***************************************************************************
  **
  ** Just mess with the underlying toggle state; nothing else
  ** 
  */
  
  public void directCheck(boolean isActive) {
    appState_.setAmPulling(isActive);
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
        StepState ans = new StepState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepStart();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
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
    ans.nextStep_ = "stepContinuePullDown";
    ans.rcxX_.pixDiam = pixDiam;
    return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans));
  }
   
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.ButtonMaskerCmdState {

    private ServerControlFlowHarness cfh;
    private boolean rootInstance_;
    private DataAccessContext rcxX_;
    
    //--------------------
     
    private String nextStep_;
    private int x;
    private int y;  
    private BTState appState_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(BTState appState, DataAccessContext dacx) {
      rootInstance_ = false;
      appState_ = appState;
      rcxX_ = dacx;
      GenomeInstance parent = rcxX_.getGenomeAsInstance().getVfgParent();
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
      boolean nowPulling = !appState_.getAmPulling();
      appState_.setAmPulling(nowPulling);
      DialogAndInProcessCmd retval;
      if (nowPulling) { 
        if (appState_.getDB().haveBuildInstructions()) {
          ResourceManager rMan = appState_.getRMan();
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
        PullDownFromRootFrame pdfrd = new PullDownFromRootFrame(appState_, rcxX_.getDBGenome(), rcxX_.getGenomeAsInstance());
        appState_.getCommonView().registerPullFrame(pdfrd);
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
        appState_.getSUPanel().giveErrorFeedback();
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }        
      
      if (rcxX_.getGenome() instanceof GenomeInstance) {  
        GenomeInstance parent = rcxX_.getGenomeAsInstance().getVfgParent();
        if (parent != null) {            
          ServerControlFlowHarness.ClickResult result = ServerControlFlowHarness.ClickResult.UNSELECTED;
          try {
            if (rcxX_.getGenome() instanceof DynamicGenomeInstance) {
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
            appState_.getSUPanel().giveErrorFeedback();
          }
          appState_.getSUPanel().drawModel(false);            
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
      if (rcxX_.getGenome() instanceof GenomeInstance) {
        GenomeInstance parent = rcxX_.getGenomeAsInstance().getVfgParent();
        if (parent != null) {    
          Intersection inter = appState_.getGenomePresentation().selectGroupFromParent(x, y, rcxX_);
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
          genCount = rcxX_.getGenomeAsInstance().getGeneration();        
          inherit = Group.buildInheritedID(startID, genCount);
          Group checkGroup = rcxX_.getGenomeAsInstance().getGroup(inherit);
          if (checkGroup != null) { // already present
            return (ServerControlFlowHarness.ClickResult.PRESENT);
          }
          GroupCreationSupport handler = new GroupCreationSupport(appState_);
          handler.addNewGroupToSubsetInstance(rcxX_, subsetGroup, null);
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
      String currentOverlay = appState_.getCurrentOverlay();
      
      x = (int)((Math.round(x / 10.0)) * 10.0);
      y = (int)((Math.round(y / 10.0)) * 10.0); 
      if (rcxX_.getGenome() instanceof GenomeInstance) {
        GenomeInstance parent = rcxX_.getGenomeAsInstance().getVfgParent();
        if (parent != null) {    
          List<Intersection> itemList = appState_.getGenomePresentation().selectFromParent(x, y, rcxX_);
          Intersection inter = (new IntersectionChooser(true, rcxX_).intersectionRanker(itemList));
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
          int genCount = rcxX_.getGenomeAsInstance().getGeneration();        
          String inherit = Group.buildInheritedID(group.getID(), genCount);
          Group checkGroup = rcxX_.getGenomeAsInstance().getGroup(inherit);
          if (checkGroup == null) { // we don't have the group!
            return (ServerControlFlowHarness.ClickResult.UNSELECTED);
          }

          NodeInstance checkNode = (NodeInstance)rcxX_.getGenome().getNode(startID);
          if (checkNode != null) { // already present
            int activity = checkNode.getActivity();
            if (activity != NodeInstance.ACTIVE) {
              ResourceManager rMan = appState_.getRMan();
              boolean doGene = (checkNode.getNodeType() == Node.GENE);
              JOptionPane.showMessageDialog(appState_.getTopFrame(), 
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
          if (moduleOnDisplay(rcxX_.getGenome())) {
            NodeProperties np = rcxX_.getLayout().getNodeProperties(startID);
            Point2D nodeLoc = np.getLocation();
            AddNodeSupport.NetModuleIntersector nmi = new AddNodeSupport.NetModuleIntersector(appState_, rcxX_);
            modCandidates = nmi.netModuleIntersections((int)nodeLoc.getX(), (int)nodeLoc.getY(), 0.0);
          }

          UndoSupport support = new UndoSupport(appState_, (doGene) ? "undo.addGene" : "undo.addNode");
          PropagateSupport.addNewNodeToSubsetInstance(appState_, rcxX_, subsetNode, support);
          if (modCandidates != null) {
            Node inSub = rcxX_.getGenome().getNode(startID);
            AddCommands.drawIntoNetModuleSupport(appState_, inSub, modCandidates, rcxX_, currentOverlay, support);
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
    
    private boolean moduleOnDisplay(Genome genome) {
      String currentOverlay = appState_.getCurrentOverlay();
      TaggedSet currentNetMods = appState_.getCurrentNetModules();           
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
      if (rcxX_.getGenome() instanceof GenomeInstance) {
        GenomeInstance parent = rcxX_.getGenomeAsInstance().getVfgParent();
        //
        // Handles the subset inclusion case
        //
        if (parent != null) {    
          List<Intersection> itemList = appState_.getGenomePresentation().selectFromParent(x, y, rcxX_);
          Intersection inter = (new IntersectionChooser(true, rcxX_).intersectionRanker(itemList));
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
          BusProperties lp = rcxX_.getLayout().getLinkProperties(inter.getObjectID());
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
            LinkageInstance checkLink = (LinkageInstance)rcxX_.getGenome().getLinkage(linkageID);        
            if (checkLink != null) { // already present
              continue;
            }
            String srcKey = subsetLink.getSource();          
            String trgKey = subsetLink.getTarget();
            NodeInstance srcNode = (NodeInstance)rcxX_.getGenome().getNode(srcKey);
            NodeInstance trgNode = (NodeInstance)rcxX_.getGenome().getNode(trgKey);
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
              Group srcGrpInChild = rcxX_.getGenomeAsInstance().getGroup(Group.addGeneration(srcGrpInParent.getID()));
              if (srcGrpInChild == null) {
                continue;
              }
              Group trgGrpInParent = parent.getGroupForNode(trgKey, GenomeInstance.MAIN_GROUP_AS_FALLBACK);
              if (trgGrpInParent == null) {
                continue;
              }
              Group trgGrpInChild = rcxX_.getGenomeAsInstance().getGroup(Group.addGeneration(trgGrpInParent.getID()));
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
            ResourceManager rMan = appState_.getRMan();          
            int include = 
              JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                            rMan.getString("addLink.NeedSrcTarg"), 
                                            rMan.getString("addLink.NeedSrcTargTitle"),
                                            JOptionPane.YES_NO_CANCEL_OPTION);        
            if (include != JOptionPane.YES_OPTION) {
              return (ServerControlFlowHarness.ClickResult.CANCELLED);
            }
          }

          PropagateSupport.addNewLinkWithNodesToSubsetInstance(appState_, rcxX_, parent, pullSurvivors, nodesToAdd);      
          return (ServerControlFlowHarness.ClickResult.SELECTED);
        }
      }
      throw new IllegalArgumentException(); 
    }  
  }
}
