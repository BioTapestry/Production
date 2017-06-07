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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNodeSupport;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNode;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSupport;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleAlphaBuilder;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Editing Notes
*/

public class ChangeNodeGroup extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ChangeNodeGroup() {
    name = "nodePopup.ChangeNodeGroup";
    desc = "nodePopup.ChangeNodeGroup";
    mnem = "nodePopup.ChangeNodeGroupMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
 public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    return (rcx.currentGenomeIsAnInstance() && !rcx.currentGenomeIsADynamicInstance());
  }

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
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
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();
        } else if (ans.getNextStep().equals("stepDoNodeRegionChange")) {
          next = ans.stepDoNodeRegionChange();
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
    ans.getDACX().setPixDiam(pixDiam);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans); 
    ans.setNextStep("stepDoNodeRegionChange"); 
    return (retval);
  }

  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection inter_;
    private Intersection newReg_;
    private int x;
    private int y;
 
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepBiWarning";
    }

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepBiWarning";
    }
  
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
      
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (true);
    }
       
    public boolean mustBeDynamic() {
      return (false);
    }
    
    public boolean cannotBeDynamic() {
      return (true);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.NO_FLOATER);
    } 
    
    public void setFloaterPropsInLayout(Layout flay) {
      throw new IllegalStateException();
    }
  
    public Object getFloater(int x, int y) {
      throw new IllegalStateException();
    }
    
    public Color getFloaterColor() {
      throw new IllegalStateException();
    }

    /***************************************************************************
    **
    ** Preload init
    */ 
      
    public void setIntersection(Intersection nodeIntersect) {
      inter_ = nodeIntersect;
      return;
    } 
    
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.CHANGE_GROUP_MEMBERSHIP; 
      return (retval);
    }
       
    /***************************************************************************
    **
    ** Warn of build instructions and init some values
    */
       
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd daipc;
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
       } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      nextStep_ = "stepSetToMode";
      return (daipc);     
    }
  
    /***************************************************************************
    **
    ** Core ops for changing the node regiojn
    */  
   
    private DialogAndInProcessCmd stepDoNodeRegionChange() {
      GenomeInstance parent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
      if (parent != null) {    
        newReg_ = uics_.getGenomePresentation().selectGroupFromParent(x, y, dacx_);
      } else {
        newReg_ = uics_.getGenomePresentation().selectGroupForRootInstance(x, y, dacx_);
      }
      if (newReg_ == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
             
      String groupID = newReg_.getObjectID();
      String baseGrpID = Group.getBaseID(groupID);   
      int genCount = dacx_.getCurrentGenomeAsInstance().getGeneration();        
      String inherit = Group.buildInheritedID(baseGrpID, genCount);
      Group intersectGroup = dacx_.getCurrentGenomeAsInstance().getGroup(inherit);
      if (intersectGroup == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      //
      // Falling into a black hole.  Most of the time, this should not happen, as the
      // node will remain in whatever visible module it is in.  There is one exception:
      // if the node needs to exit a visible module because that visible module is attached
      // to the former region.  
      //
      
      boolean changeViz = false;
      if (dacx_.getCurrentGenomeAsInstance().inModuleAttachedToGroup(inter_.getObjectID())) {
        AddNodeSupport.BlackHoleResults bhr = AddNodeSupport.droppingIntoABlackHole(uics_, dacx_, x, y, false); 
        if (bhr.intoABlackHole) {
          ResourceManager rMan = dacx_.getRMan();
          String message = UiUtil.convertMessageToHtml(rMan.getString("intoBlackHole.resetVizMoved")); 
          int changeVizVal = 
            JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                          message,
                                          rMan.getString("intoBlackHole.resetVizTitle"),
                                          JOptionPane.YES_NO_CANCEL_OPTION);        
          if (changeVizVal == JOptionPane.CANCEL_OPTION) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.CANCELLED, this));
          }
          changeViz = (changeVizVal == JOptionPane.YES_OPTION);
        }    
      } 
      
      if (changeViz) {
        NetOverlayController noc = uics_.getNetOverlayController();
        noc.setSliderValue(NetModuleAlphaBuilder.MINIMAL_ALL_MEMBER_VIZ);
      }  
       
      return (changeNodeRegion(dacx_));    
    }
     
    /***************************************************************************
    **
    ** Change the region containing a node/gene
    */  
   
    private DialogAndInProcessCmd changeNodeRegion(StaticDataAccessContext rcx) {
          
      
      //
      // Note we are not allowing this from a Dynamic Genome Instance.
      //
      
      //
      // Work with the root instance:
      //
      
      GenomeInstance rootInstance = rcx.getRootInstanceFOrCurrentInstance();
      String rootID = rootInstance.getID();
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = rcx.getFGHO().getGlobalNetModuleLinkPadNeeds();
     
      //
      // Make sure the target group is free:
      //
      
      String nodeID = inter_.getObjectID();
      String targRegionID = newReg_.getObjectID();
      String baseID = GenomeItemInstance.getBaseID(nodeID); 
      String groupBase = Group.getBaseID(targRegionID);
      Group targetGroup = rootInstance.getGroup(groupBase);  
      if (targetGroup.instanceIsInGroup(baseID)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      //
      // Find out what group we are in:
      //
         
      Group currGrp = rootInstance.getGroupForNode(nodeID, GenomeInstance.ALWAYS_MAIN_GROUP);
      if (currGrp == null) { // should not happen
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      //
      // Crank thru the kids of the root instance and whichever ones do not have the 
      // target group (but do have the node) will need the node deleted.  Note that we just 
      // want to find the highest model with the problem; the lower ones will 
      // get knocked off automatically:
      //
       
      HashSet<String> killSet = new HashSet<String>();
      Iterator<GenomeInstance> giit = rcx.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gik = giit.next();
        if (gik == rootInstance) {
          continue;
        }
        if (!rootInstance.isAncestor(gik)) {
          continue;
        }
        if (gik.getNode(nodeID) == null) {
          continue;
        }
        String inherit = Group.buildInheritedID(groupBase, gik.getGeneration());
        if (gik.getGroup(inherit) == null) {
          killSet.add(gik.getID());
        }
      } 
      
      HashSet<String> trimmedKillSet = new HashSet<String>();
      Iterator<String> ksit = killSet.iterator();
      while (ksit.hasNext()) {
        String deadGuy = ksit.next();
        GenomeInstance dgi = (GenomeInstance)rcx.getGenomeSource().getGenome(deadGuy);
        if (dgi.isRootInstance()) {
          throw new IllegalStateException();  // Can't have a root guy in this list!
        }
        boolean haveParent = false;
        Iterator<String> ksit2 = killSet.iterator();
        while (ksit2.hasNext()) {
          String checkGuy = ksit2.next();
          if (checkGuy.equals(deadGuy)) {
            continue;
          }
          GenomeInstance cggi = (GenomeInstance)rcx.getGenomeSource().getGenome(checkGuy);
          if (cggi.isAncestor(dgi)) {
            haveParent = true;
            break;
          }
        }
        if (!haveParent) {
          trimmedKillSet.add(deadGuy);
        }
      }   
     
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.changeNodeGroup", rcx);     
      
      //
      // Move the node location:
      //
      
      Point2D oldLoc = rcx.getCurrentLayout().getNodeProperties(nodeID).getLocation();
      double dx = x - oldLoc.getX();
      double dy = y - oldLoc.getY();
      // Don't provide pad fixup yet.  Let that happen at the end when everything is done...
      StaticDataAccessContext rcxRI = new StaticDataAccessContext(rcx, rootInstance);
      Layout.PropChange lpc = rcxRI.getCurrentLayout().moveNode(nodeID, dx, dy, null, rcxRI);
      if (lpc != null) {
        PropChangeCmd pcc = new PropChangeCmd(rcxRI, lpc);
        support.addEdit(pcc);        
      }
     
      //
      // Get it out of modules attached to the old group:
      //
      
      HashSet<NetModule.FullModuleKey> attached = new HashSet<NetModule.FullModuleKey>();
      rcxRI.getFGHO().getModulesAttachedToGroup(rootInstance, currGrp.getID(), attached);
      Iterator<NetModule.FullModuleKey> ait = attached.iterator();
      while (ait.hasNext()) {
        NetModule.FullModuleKey fmk = ait.next();
        NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerWithOwnerKey(fmk.ownerKey);
        NetworkOverlay no = noo.getNetworkOverlay(fmk.ovrKey);
        NetModule nmod = no.getModule(fmk.modKey);
        if (nmod.isAMember(nodeID)) {
          String genomeM = (fmk.ownerType == NetworkOverlay.DYNAMIC_PROXY) ? 
                               rcx.getGenomeSource().getDynamicProxy(fmk.ownerKey).getFirstProxiedKey() : 
                               fmk.ownerKey;
          StaticDataAccessContext rcxO = new StaticDataAccessContext(rcx, genomeM);
          RemoveNode.supportNodeDeletionFromOverlays(uics_, noo, rcxO, support, nodeID, false);    
        }
      } 
   
      //
      // Drop it out of all the original groups (this catches subregions too!):
      //
      
      Iterator<Group> git = rootInstance.getGroupIterator();
      while (git.hasNext()) {
        Group gr = git.next();
        if (gr.isInGroup(nodeID, rootInstance)) {
          GroupChange grc = gr.removeMember(nodeID, rootID);
          if (grc != null) {
            GroupChangeCmd gcc = new GroupChangeCmd(rcxRI, grc);
            support.addEdit(gcc);        
          }
        }
      }
      
      //
      // Add it to the new group
      //
      
      GroupChange grc = targetGroup.addMember(new GroupMember(nodeID), rootID);
      if (grc != null) {
        GroupChangeCmd gcc = new GroupChangeCmd(rcxRI, grc);
        support.addEdit(gcc);        
      }     
     
      //
      // Dump it out of all the kids where it is not allowed:
      //
      
      HashSet<String> nodes = new HashSet<String>();
      nodes.add(nodeID);
      HashSet<String> links = new HashSet<String>();
     
      Iterator<String> tksit = trimmedKillSet.iterator();
      while (tksit.hasNext()) {
        String genomeKill = tksit.next();
        // Previously made provisions for checking if data table should be deleted, but that is DBGenome only! Go with the null map instead!
        StaticDataAccessContext rcxK = new StaticDataAccessContext(rcx, genomeKill);
        RemoveSupport.deleteNodesAndLinksFromModel(uics_, nodes, links, rcxK, support, null, false, uFac_); 
      }   
   
      
      //
      // If it is called out explicitly in layout metadata, its presence is
      // tied expliclitly to the old group.  Dump it:
      //
      
      Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
      while (layit.hasNext()) {
        Layout nl = layit.next();
        if (nl.haveNodeMetadataDependency(rcx.getCurrentGenomeID(), nodeID)) {
          lpc = nl.dropNodeMetadataDependency(rcx.getCurrentGenomeID(), nodeID);
          if (lpc != null) {
            PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
            support.addEdit(pcc);        
          }
        }
      }
     
      //
      // Remove from all dynamic proxies underneath the instance if it is
      // included there as an extra node, and we are transferring it to
      // a region that is included in the proxy
      //
      
      Iterator<DynamicInstanceProxy> pxit = rcx.getGenomeSource().getDynamicProxyIterator();        
      while (pxit.hasNext()) {
        DynamicInstanceProxy dip = pxit.next();
        if (dip.instanceIsAncestor(rcx.getCurrentGenomeAsInstance())) {
          if (dip.hasAddedNode(nodeID)) {
            Iterator<Group> dgit = dip.getGroupIterator();
            while (dgit.hasNext()) {
              Group dgrp = dgit.next();
              if (Group.getBaseID(dgrp.getID()).equals(groupBase)) {
                ProxyChange pc = dip.deleteExtraNode(nodeID);
                if (pc != null) {
                  ProxyChangeCmd pcc = new ProxyChangeCmd(rcx, pc);
                  support.addEdit(pcc);
                 // Don't want to enumerate all those dynamic instances for eventing.
                 // This is a stand in that forces update of all dynamic models:
                 //  needGeneral = true;
                 //   goodbyeExtras = true;
                }
              }    
            }
          }
        }        
      }       
     
      //
      // Fixup pads:
      //
      
      if (globalPadNeeds != null) {
        ModificationCommands.repairNetModuleLinkPadsGlobally(rcx, globalPadNeeds, false, support);
      }          
  
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), rootID, ModelChangeEvent.UNSPECIFIED_CHANGE));
      // clear out the dynamic proxies!!!!
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();     
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this)); 
    }         
  }  
}
