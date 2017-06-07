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

import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.GroupCreationSupport;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle duplicating multiple regions
*/

public class DupGroups extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean forPopup_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DupGroups(boolean forPopup) {
    forPopup_ = forPopup;  
    name = (forPopup_) ? "groupPopup.CopyRegion" : "command.CopyMultiGroup";
    desc = (forPopup_) ? "groupPopup.CopyRegion" :  "command.CopyMultiGroup";
    mnem = (forPopup_) ? "groupPopup.CopyRegionMnem" : "command.CopyMultiGroupMnem";
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
   
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.genomeIsRootInstance());
  }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!forPopup_) {
      throw new IllegalStateException();
    }
    return (rcx.getCurrentGenomeAsInstance().getVfgParent() == null);
  }             
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {  
    return (new StepState(forPopup_, dacx));
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
        StepState ans = new StepState(forPopup_, cfh);
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
    
    private boolean myForPop_;
    private String interID_;
  
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forPopup, StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepDoIt";
      myForPop_ = forPopup;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(boolean forPopup, ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
      myForPop_ = forPopup;
    }
 
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) {
      if (!myForPop_) {
        throw new IllegalStateException();
      }   
      interID_ = inter.getObjectID();
      return;
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */  
   
    private DialogAndInProcessCmd stepDoIt() {  
      
      if (dacx_.getInstructSrc().haveBuildInstructions()) {
        ResourceManager rMan = dacx_.getRMan(); 
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      rMan.getString("instructWarning.message"), 
                                      rMan.getString("instructWarning.title"),
                                      JOptionPane.WARNING_MESSAGE);
      } 
      if (myForPop_) { 
        HashSet<String> regionIDs = new HashSet<String>();
        regionIDs.add(interID_);
        duplicateRegions(regionIDs);              
      } else {
        HashSet<String> allGroups = new HashSet<String>();
        Iterator<Group> git = dacx_.getCurrentGenomeAsInstance().getGroupIterator();
        while (git.hasNext()) {
          Group group = git.next();
          if (!group.isASubset(dacx_.getCurrentGenomeAsInstance())) {
            allGroups.add(group.getID());
          }
        }
        duplicateRegions(allGroups); 
      }
      uics_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Duplicate several regions, either to the same or to another genome instance
    */  
   
    private boolean duplicateRegions(Set<String> regionIDs) {
   
      if (!dacx_.currentGenomeIsAnInstance()) {
        return (false);
      }
      if (!dacx_.currentGenomeIsRootInstance()) {
        return (false);
      }
      
      GenomeInstance srcGI = dacx_.getCurrentGenomeAsInstance();
      String genomeKey = srcGI.getID();
  
      // Pad fixup:
      
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();
      
      //
      // Undo/Redo support
      //    
      
      UndoSupport support = uFac_.provideUndoSupport("undo.duplicateRegion", dacx_);      
  
      GroupCreationSupport handler = new GroupCreationSupport(uics_, dacx_, regionIDs, uFac_);
   
      List<GroupCreationSupport.GroupDuplicationInfo> dupedGroups = handler.handleCopyCreation(support);
      if (dupedGroups == null) {
        return (false);
      }
      
      //
      // May have one or many regions:
      //
      
      HashSet<String> combinedLinks = new HashSet<String>();
      HashMap<String, String> createdGroupMap = new HashMap<String, String>();
      int numGroups = dupedGroups.size();
      StaticDataAccessContext rcxGD = null;
      
      //
      // Before we do the actual copy, if we are going to another model, we let 
      // the user know that if one of the copied groups has an attached module,
      // it is not coming along for the ride.  (To do so, user would have to
      // (in general) specify the mapping between overlays).  FIX ME!!! to
      // do this.  The mapping dialog would go right here instead of the
      // warning.
      //
        
      for (int i = 0; i < numGroups; i++) {   
        GroupCreationSupport.GroupDuplicationInfo cgd = dupedGroups.get(i);
        if (genomeKey.equals(cgd.targetGiID)) {
          break;
        }
        String srcGroupID = cgd.toCopy.getID();    
        Map<String, Set<String>> moduleMap = srcGI.findModulesOwnedByGroup(srcGroupID);
        if (!moduleMap.isEmpty()) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("duplicateRegion.cannotDupModules"), 
                                        rMan.getString("duplicateRegion.cannotDupModulesTitle"),
                                        JOptionPane.WARNING_MESSAGE);
          break;
        }
      }
         
      for (int i = 0; i < numGroups; i++) {   
        GroupCreationSupport.GroupDuplicationInfo cgd = dupedGroups.get(i);
        Group createdGroup = cgd.dupGroup;
        String createdGroupID = createdGroup.getID();
        rcxGD = new StaticDataAccessContext(dacx_, cgd.targetGiID); // Same for all models
  
        // Figure out the offset to the new region:
  
        Vector2D offset = cgd.bifg.myOffset;
  
        //
        // We are given a list of selected items that we are to propagate
        // down. Crank through the list and propagate.
        //
  
        HashMap<String, String> nodeIDMap = new HashMap<String, String>();
        duplicateNodes(rcxGD, createdGroup, cgd.genesToCopy, cgd.nodesToCopy, 
                       dacx_.getCurrentLayout(), offset, support, nodeIDMap);   
  
        //
        // If we have any net modules that are attached to the original group, we need to
        // duplicate those too (this can be done without user input since the model
        // is the same):
        //
  
        if (genomeKey.equals(cgd.targetGiID)) {
          handleModuleDuplication(rcxGD, nodeIDMap, cgd.toCopy.getID(), createdGroupID, offset, support);   
        }
  
        //
        // Finally handle links:
        //
        
        createdGroupMap.put(cgd.toCopy.getID(), createdGroupID);
  
        combineLinksToDuplicate(cgd.internalLinks, cgd.inboundLinks, cgd.outboundLinks, combinedLinks);
      }
      
      duplicateAllowedLinks(dacx_, rcxGD, createdGroupMap, combinedLinks, support);
  
      ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds, false, support);
      support.finish();
      return (true);
    }
    
    /***************************************************************************
    **
    ** Build a single set of link candidates
    */  
   
    private void combineLinksToDuplicate(ArrayList<String> internalLinks,           
                                         ArrayList<String> inboundLinks, 
                                         ArrayList<String>outboundLinks, Set<String> combinedLinks) {
      combinedLinks.addAll(internalLinks);
      combinedLinks.addAll(inboundLinks);
      combinedLinks.addAll(outboundLinks);
      return;
    }    
  
    /***************************************************************************
    **
    ** Handle link duplication
    */  
   
    private void duplicateAllowedLinks(StaticDataAccessContext rcxSrc,
                                       StaticDataAccessContext rcxTrg,
                                       Map<String, String> createdGroupMap,
                                       Set<String> linkCandidates, UndoSupport support) {
    
      //
      // If we are in the same model, we can do everybody.
      // Going into a new model, we can only handle those guys who start and end on the groups
      // we are duplicating!
      //
      
      boolean sameModel = rcxSrc.getCurrentGenomeID().equals(rcxTrg.getCurrentGenomeID());
      
      Iterator<String> lcit = linkCandidates.iterator();
      while (lcit.hasNext()) {
        String linkID = lcit.next();
        GenomeInstance.GroupTuple origTuple = rcxSrc.getCurrentGenomeAsInstance().getRegionTuple(linkID);
        String srcGrpID = origTuple.getSourceGroup();
        String mappedSrcGrpID = createdGroupMap.get(srcGrpID);
        if ((mappedSrcGrpID == null) && sameModel) {
          mappedSrcGrpID = srcGrpID;
        }
        String trgGrpID = origTuple.getTargetGroup();
        String mappedTrgGrpID = createdGroupMap.get(trgGrpID);
        if ((mappedTrgGrpID == null) && sameModel) {
          mappedTrgGrpID = trgGrpID;
        }
        if ((mappedSrcGrpID != null) && (mappedTrgGrpID != null)) {
          GenomeInstance.GroupTuple tuple = new GenomeInstance.GroupTuple(mappedSrcGrpID, mappedTrgGrpID);
          LinkageInstance link = (LinkageInstance)rcxSrc.getCurrentGenome().getLinkage(linkID);
          copyLinkageInstance(rcxTrg, link, tuple, rcxSrc, support);
        }
      }    
      
      return;
    }   
    
    /***************************************************************************
    **
    ** Copy the linkage instance 
    */  
   
    public void copyLinkageInstance(StaticDataAccessContext rcxTrg,
                                    LinkageInstance link, 
                                    GenomeInstance.GroupTuple groupTup,
                                    DataAccessContext rcxSrc, UndoSupport support) {
  
      LinkageInstance li = copyLinkageInstanceNoLayout(rcxTrg, link, groupTup, support);
  
      // This is the link properties for the original link.  It may get modified if
      // the source is the same between the old and new link copies:
      BusProperties lp = rcxSrc.getCurrentLayout().getLinkProperties(link.getID());
      lp = lp.clone();
      String sourceID = link.getSource();
      NodeProperties nps = rcxSrc.getCurrentLayout().getNodeProperties(sourceID);
      Point2D srcPt = nps.getLocation();
      PropagateSupport.downPropagateLinkProperties(rcxTrg, lp, li, srcPt, support, link.getID());
      return;
    }

    /***************************************************************************
    **
    ** Copy the linkage instance without layout support
    */  
   
    public static LinkageInstance copyLinkageInstanceNoLayout(DataAccessContext dacx, LinkageInstance link, 
                                                               GenomeInstance.GroupTuple groupTup,
                                                               UndoSupport support) {
         
      GenomeInstance gi = dacx.getCurrentGenomeAsInstance();
      int instanceCount = gi.getNextLinkInstanceNumber(GenomeItemInstance.getBaseID(link.getID()));
      
      //
      // We need to find the source and target instance numbers to use from
      // the group tuple.
      //
                                       
      String sourceID = link.getSource();
      String targID = link.getTarget();                                                                      
      int srcInstance = gi.getInstanceForNodeInGroup(GenomeItemInstance.getBaseID(sourceID), groupTup.getSourceGroup());
      int trgInstance = gi.getInstanceForNodeInGroup(GenomeItemInstance.getBaseID(targID), groupTup.getTargetGroup());    
         
      LinkageInstance newLink = new LinkageInstance(link, instanceCount, srcInstance, trgInstance);
                
      GenomeChange gc = gi.addLinkage(newLink);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx, gc);
        support.addEdit(gcc);
      }    
  
      //
      // Right now, outbound links from a copied group will hit the same landing pads
      //
      
      return (newLink);
    }  
 
    /***************************************************************************
    **
    ** If we have any net modules that are attached to the original group, we need to
    **  duplicate those too:
    */  
   
    private void handleModuleDuplication(DataAccessContext rcxTrg,
                                         Map<String, String> nodeIDMap,
                                         String regionID, String createdGroupID, 
                                         Vector2D offset, UndoSupport support) {   
      Map<String, Set<String>> moduleMap = dacx_.getCurrentGenomeAsInstance().findModulesOwnedByGroup(regionID);
      if (!moduleMap.isEmpty()) {
        DBGenome rootGenome = (DBGenome)rcxTrg.getGenomeSource().getRootDBGenome();
        HashMap<String, String> groupMap = new HashMap<String, String>();
        HashMap<String, String> modIDMap = new HashMap<String, String>();
        groupMap.put(regionID, createdGroupID);
        Iterator<String> mmkit = moduleMap.keySet().iterator();
        ResourceManager rMan = dacx_.getRMan();    
  
        while (mmkit.hasNext()) {
          String nokey = mmkit.next();
          NetworkOverlay novr = dacx_.getCurrentGenomeAsInstance().getNetworkOverlay(nokey);
          NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(nokey);        
          Set<String> modSet = moduleMap.get(nokey);
          Iterator<String> msit = modSet.iterator();
          while (msit.hasNext()) {
            String modID = msit.next();
            NetModule module = novr.getModule(modID);
            NetModuleProperties nmp = nop.getNetModuleProperties(modID);
            String newModID = rootGenome.getNextKey();
            modIDMap.put(modID, newModID);
            NetModule newMod = new NetModule(module, newModID, groupMap, nodeIDMap); 
            // Have to come up with a unique name for the duplicated region!
            String form = rMan.getString("duplicateRegion.nameFormat");
            String currentName = newMod.getName();
            int count = 1;
            while (true) {
              Integer copyNum = new Integer(count++);
              String copyName = MessageFormat.format(form, new Object[] {currentName, copyNum});    
              Iterator<NetModule> moit = novr.getModuleIterator();
              boolean nameExists = false;
              while (moit.hasNext()) {
                NetModule nm = moit.next();
                if (DataUtil.keysEqual(nm.getName(), copyName)) {
                  nameExists = true;
                  break;
                }
              }
              if (!nameExists) {
                newMod.setName(copyName);
                break;
              }
            }
            NetworkOverlayChange noc = rcxTrg.getCurrentGenomeAsInstance().addNetworkModule(nokey, newMod); 
            if (noc != null) {
              NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxTrg, noc);
              support.addEdit(gcc);
            }
            NetModuleProperties newNmp = new NetModuleProperties(nmp, newModID, offset);
            Layout.PropChange pc = rcxTrg.getCurrentLayout().setNetModuleProperties(newModID, newNmp, nokey);
            if (pc != null) {
              support.addEdit(new PropChangeCmd(rcxTrg, pc));
            }
          }
        }
      }
      return;
    }

    /***************************************************************************
    **
    ** Copy the node instance 
    */  
   
    private String copyNodeInstance(StaticDataAccessContext rcxi,
                                    NodeInstance node, boolean isGene, 
                                    Group trgGroup, 
                                    Layout srcLayout,
                                    Vector2D offset, UndoSupport support) {
  
      NodeInstance ni = copyNodeInstanceNoLayout(rcxi, (GenomeInstance)rcxi.getCurrentGenome(), node, isGene, trgGroup, support);   
      PropagateSupport.propagateNodeLayoutProps(rcxi, node, ni, srcLayout, offset, support);
      return (ni.getID());
    }   
  
    /***************************************************************************
    **
    ** Copy the node instance without layout support
    */  
   
    public static NodeInstance copyNodeInstanceNoLayout(DataAccessContext rcx, GenomeInstance targetGi,
                                                        NodeInstance node, boolean isGene, 
                                                        Group trgGroup, UndoSupport support) {
                                    
      int instanceCount = targetGi.getNextNodeInstanceNumber(GenomeItemInstance.getBaseID(node.getID()));
      NodeInstance newNode = (isGene) ? new GeneInstance((GeneInstance)node, instanceCount) 
                                      : new NodeInstance(node, instanceCount);                      
      GenomeChange gc = (isGene) ? targetGi.addGene((GeneInstance)newNode)
                                 : targetGi.addNode(newNode);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(rcx, gc);
        support.addEdit(gcc);
      }    
  
      GroupChange grc = trgGroup.addMember(new GroupMember(newNode.getID()), targetGi.getID());
      if (grc != null) {
        GroupChangeCmd grcc = new GroupChangeCmd(rcx, grc);
        support.addEdit(grcc);
      } 
      
      support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), targetGi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));       
      return (newNode);
    }    
    
      
      
    /***************************************************************************
    **
    ** Handle node duplication
    */  
   
    private void duplicateNodes(StaticDataAccessContext rcxi, Group createdGroup,
                                ArrayList<Node> genesToCopy, 
                                ArrayList<Node> nodesToCopy, 
                                Layout srcLayout, Vector2D offset, UndoSupport support, Map<String, String> nodeIDMap) {
      
      Iterator<Node> glit = genesToCopy.iterator();
      while (glit.hasNext()) {
        GeneInstance gene = (GeneInstance)glit.next();
        String newID = copyNodeInstance(rcxi, gene, true, createdGroup, srcLayout, offset, support);
        nodeIDMap.put(gene.getID(), newID);
      }
      Iterator<Node> nlit = nodesToCopy.iterator();
      while (nlit.hasNext()) {
        NodeInstance node = (NodeInstance)nlit.next();
        String newID = copyNodeInstance(rcxi, node, false, createdGroup, srcLayout, offset, support);
        nodeIDMap.put(node.getID(), newID);
      }
      return;
    }  
  }
}
