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

package org.systemsbiology.biotapestry.cmd.flow.link;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle changing target or source pads
*/

public class ChangePad extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean isForTarget_;
  private boolean isForModules_;
  private boolean isForGeneModSwap_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ChangePad(boolean isForTarget, boolean isForModules, boolean isForGeneModSwap) {
    isForTarget_ = isForTarget;
    isForModules_ = isForModules;
    isForGeneModSwap_ = isForGeneModSwap;
    if (isForGeneModSwap_) {
      if (!isForTarget_ || isForModules_) {
        throw new IllegalArgumentException();
      }
      name = "linkPopup.ModSwap";
      desc = "linkPopup.ModSwap";
      mnem = "linkPopup.ModSwapMnem";
    } else if (isForTarget) {
      name =  (isForModules) ? "linkPopup.TargReloc" : "linkPopup.TargReloc";
      desc = (isForModules) ? "linkPopup.TargReloc" : "linkPopup.TargReloc";
      mnem =  (isForModules) ? "linkPopup.TargRelocMnem" : "linkPopup.TargRelocMnem";
    } else {
      name =  (isForModules) ? "linkPopup.SrcReloc" : "linkPopup.SrcReloc";
      desc = (isForModules) ? "linkPopup.SrcReloc" : "linkPopup.SrcReloc";
      mnem =  (isForModules) ? "linkPopup.SrcRelocMnem" : "linkPopup.SrcRelocMnem";      
    }
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
    if (!isSingleSeg) {
      return (false);
    }
    if (isForGeneModSwap_) {    
      String lid = inter.getObjectID();  // Remember... this may not be the link we want!
      BusProperties bp = rcx.getCurrentLayout().getLinkProperties(lid);
      LinkSegmentID[] linkIDs = inter.segmentIDsFromIntersect();
      // Only allowed to perform this op if there is only one link through the segment! 
      Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);
      String myLinkId = throughSeg.iterator().next();
      Linkage link = rcx.getCurrentGenome().getLinkage(myLinkId);
      String target = link.getTarget();
      Node targNode = rcx.getCurrentGenome().getNode(target);
      if (targNode.getNodeType() != Node.GENE) {
        return (false);
      }
      Gene targGene = (Gene)targNode;
      if (targGene.getNumRegions() == 0) {
        return (false);
      }
    }

    return (LinkSupport.amIValidForTargetOrSource(inter, 
                                                 (isForTarget_) ? LinkSupport.IS_FOR_TARGET : LinkSupport.IS_FOR_SOURCE, 
                                                  isForModules_, rcx));
  }
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    ChangePadState retval = new ChangePadState(isForTarget_, isForModules_, dacx);
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
        ChangePadState ans = (ChangePadState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();      
        } else if (ans.getNextStep().equals("stepRelocateTarget")) {   
          next = ans.stepRelocateTarget();  
        } else if (ans.getNextStep().equals("stepRelocateModuleTarget")) {   
          next = ans.stepRelocateModuleTarget();
        } else if (ans.getNextStep().equals("stepRelocateSource")) {   
          next = ans.stepRelocateSource(); 
        } else if (ans.getNextStep().equals("stepRelocateModuleSource")) {   
          next = ans.stepRelocateModuleSource();   
        } else if (ans.getNextStep().equals("stepSwitchGeneModule")) {   
          next = ans.stepSwitchGeneModule();   
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
    ChangePadState ans = (ChangePadState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    ans.getDACX().setPixDiam(pixDiam);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    
    if (isForGeneModSwap_) { 
      ans.setNextStep("stepSwitchGeneModule");
    } else {
      if (isForTarget_) {
        ans.setNextStep((ans.isForModule) ? "stepRelocateModuleTarget" : "stepRelocateTarget"); 
      } else {
        ans.setNextStep((ans.isForModule) ? "stepRelocateModuleSource" : "stepRelocateSource"); 
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class ChangePadState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection intersect;
    private boolean isForModule;
    private boolean isForTarget; 
    private int x;
    private int y;  

    /***************************************************************************
    **
    ** Constructor
    */
     
    private ChangePadState(boolean isForTarget, boolean isForModule, StaticDataAccessContext dacx) {
      super(dacx);
      this.isForTarget = isForTarget;
      this.isForModule = isForModule;
      nextStep_ = "stepSetToMode";
    }
    
    /***************************************************************************
    **
    ** Constructor
    */
     
    private ChangePadState(boolean isForTarget, boolean isForModule, ServerControlFlowHarness cfh) {
      super(cfh);
      this.isForTarget = isForTarget;
      this.isForModule = isForModule;
      nextStep_ = "stepSetToMode";
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
      return (!isForModule);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (true);
    }
    
    public boolean noRootModel() {
      return (false);
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      return (!isForModule);
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
    ** for preload
    */ 
       
    public void setIntersection(Intersection intersect) {
      this.intersect = intersect;
      return;
    }
 
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {  
      
      // Legacy belt-and-supenders code:
      if (!LinkSupport.amIValidForTargetOrSource(intersect, 
                                                 (isForTarget) ? LinkSupport.IS_FOR_TARGET : LinkSupport.IS_FOR_SOURCE, 
                                                 isForModule, dacx_)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } 
      
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      if (isForTarget) {
        retval.suPanelMode = (isForModule) ? PanelCommands.Mode.RELOCATE_NET_MOD_TARGET : PanelCommands.Mode.RELOCATE_TARGET;
      } else {
        retval.suPanelMode = (isForModule) ? PanelCommands.Mode.RELOCATE_NET_MOD_SOURCE : PanelCommands.Mode.RELOCATE_SOURCE;
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the relocation
    */
      
    private DialogAndInProcessCmd stepRelocateTarget() {        
      //
      // Go and find if we intersect anything.
      //

      List<Intersection.AugmentedIntersection> augs = uics_.getGenomePresentation().intersectItem(x, y, dacx_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      //
      // If we don't intersect anything, we need to bag it.
      //

      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      String id = inter.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
       
      List<Intersection.PadVal> pads = inter.getPadCand();
    
      if (LinkSupport.changeLinkTarget(intersect, pads, id, dacx_, uFac_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    }
  
    /***************************************************************************
    **
    ** Do the relocation
    */
       
    private DialogAndInProcessCmd stepRelocateModuleTarget() {

       String currentOverlay = dacx_.getOSO().getCurrentOverlay();
   
       //
       // Go and find if we intersect anything.
       //
  
       Intersection intersected = 
         uics_.getGenomePresentation().intersectANetModuleElement(x, y, dacx_, GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
       //
       // If we don't intersect anything, or the correct thing, we need to bag it.
       //
  
       if (intersected == null) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
       String id = intersected.getObjectID();
       NetOverlayOwner owner = dacx_.getGenomeSource().getOverlayOwnerFromGenomeKey(dacx_.getCurrentGenomeID());
       NetworkOverlay novr = owner.getNetworkOverlay(currentOverlay);
       NetModule nm = novr.getModule(id);
       if (nm == null) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
             
       NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersected.getSubID();
       if (ei == null) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
       if (ei.type != NetModuleFree.IntersectionExtraInfo.IS_PAD) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
       
       // New point:
       
       Point2D padPt = (Point2D)ei.intersectPt.clone();
       UiUtil.forceToGrid(padPt, UiUtil.GRID_SIZE);
       Vector2D toSide = ei.padNorm.scaled(-1.0);
       
       String treeID = intersect.getObjectID();
       NetModuleLinkageProperties nmlp = 
         dacx_.getCurrentLayout().getNetOverlayProperties(currentOverlay).getNetModuleLinkagePropertiesFromTreeID(treeID);
       
       LinkSegmentID[] linkIDs = intersect.segmentIDsFromIntersect();
       Set<String> throughSeg = nmlp.resolveLinkagesThroughSegment(linkIDs[0]);
       if (throughSeg.size() != 1) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }     
       String myLinkId = throughSeg.iterator().next();
       NetModuleLinkage myLink = novr.getLinkage(myLinkId);
       if (!myLink.getTarget().equals(id)) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }     
       
       //
       // Pretty lax about links in and out of modules.  Only restriction is that
       // link cannot end on the same pad as it starts (avoids zero-length links!)
       //
       
       if (novr.linkIsFeedback(myLinkId)) {      
         Point2D startPoint = nmlp.getSourceStart(0.0);    
         if (padPt.equals(startPoint)) {
           return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
         }
       }
  
       //
       // Undo/Redo support
       //
  
       UndoSupport support = uFac_.provideUndoSupport("undo.changeModuleLinkTarget", dacx_);     
  
       //
       // We reject unless the target we are intersecting matches the
       // link we are trying to relocate.
       //
  
       Layout.PropChange lpc = dacx_.getCurrentLayout().changeNetModuleTarget(currentOverlay, myLinkId, padPt, toSide, dacx_);
       if (lpc != null) {
         PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
         support.addEdit(mov);
         support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
         support.finish();
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
       } else {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
     }
    
    /***************************************************************************
    **
    ** Do the relocation
    */
       
    private DialogAndInProcessCmd stepRelocateSource() {
 
      //
      // Go and find if we intersect anything.
      //

     List<Intersection.AugmentedIntersection> augs = uics_.getGenomePresentation().intersectItem(x, y, dacx_, true, false);
     Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
     Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      //
      // If we don't intersect anything, we need to bag it.
      //

      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      String id = inter.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      List<Intersection.PadVal> pads = inter.getPadCand();
   
      if (LinkSupport.changeLinkSource(uics_, intersect, pads, id, dacx_, uFac_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    }
    
    /***************************************************************************
    **
    ** Do the relocation
    */
        
    private DialogAndInProcessCmd stepRelocateModuleSource() {
      String currentOverlay = dacx_.getOSO().getCurrentOverlay();
  
      //
      // Go and find if we intersect anything.
      //
 
      Intersection intersected = 
        uics_.getGenomePresentation().intersectANetModuleElement(x, y, dacx_, GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
      //
      // If we don't intersect anything, or the correct thing, we need to bag it.
      //

      if (intersected == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      String id = intersected.getObjectID();
      NetOverlayOwner owner = dacx_.getGenomeSource().getOverlayOwnerFromGenomeKey(dacx_.getCurrentGenomeID());
      NetworkOverlay novr = owner.getNetworkOverlay(currentOverlay);
      NetModule nm = novr.getModule(id);
      if (nm == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
            
      NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersected.getSubID();
      if (ei == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      if (ei.type != NetModuleFree.IntersectionExtraInfo.IS_PAD) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      // New point:
      
      Point2D padPt = (Point2D)ei.intersectPt.clone();
      UiUtil.forceToGrid(padPt, UiUtil.GRID_SIZE);
      Vector2D toSide = ei.padNorm.scaled(-1.0);
           
      String treeID = intersect.getObjectID();
      NetModuleLinkageProperties nmlp = 
        dacx_.getCurrentLayout().getNetOverlayProperties(currentOverlay).getNetModuleLinkagePropertiesFromTreeID(treeID);
      
      LinkSegmentID[] linkIDs = intersect.segmentIDsFromIntersect();
      Set<String> throughSeg = nmlp.resolveLinkagesThroughSegment(linkIDs[0]);
      int totalLinks = nmlp.getLinkageList().size();
      if (throughSeg.size() != totalLinks) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }     
      String myLinkId = throughSeg.iterator().next();
      NetModuleLinkage myLink = novr.getLinkage(myLinkId);
      if (!myLink.getSource().equals(id)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }     
      
      //
      // Pretty lax about links in and out of modules.  Only restriction is that
      // link cannot end on the same pad as it starts (avoids zero-length links!)
      // 
      
      Iterator<String> tsit = throughSeg.iterator();
      while (tsit.hasNext()) {
        String linkID = tsit.next();
        if (!novr.linkIsFeedback(linkID)) {
          continue;
        }
        Point2D targPt = nmlp.getTargetEnd(linkID, 0.0);
        if (padPt.equals(targPt)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }       
      }

      //
      // Undo/Redo support
      //

      UndoSupport support = uFac_.provideUndoSupport("undo.changeModuleLinkSource", dacx_);     

      //
      // We reject unless the target we are intersecting matches the
      // link we are trying to relocate.
      //

      Layout.PropChange lpc = dacx_.getCurrentLayout().changeNetModuleSource(currentOverlay, treeID, padPt, toSide, dacx_);
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
        support.finish();
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    }
 
    /***************************************************************************
    **
    ** Do the relocation
    */
      
    private DialogAndInProcessCmd stepSwitchGeneModule() {        
      //
      // Go and find if we intersect anything.
      //

      List<Intersection.AugmentedIntersection> augs = uics_.getGenomePresentation().intersectItem(x, y, dacx_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      //
      // If we don't intersect anything, we need to bag it.
      //

      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      String id = inter.getObjectID();
      Node node = dacx_.getCurrentGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
       
      List<Intersection.PadVal> pads = inter.getPadCand();
      
      if (LinkSupport.changeLinkTargetGeneModule(intersect, pads, id, dacx_, uFac_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    } 
  }   
}
