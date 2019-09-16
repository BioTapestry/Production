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

package org.systemsbiology.biotapestry.cmd.flow.link;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
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
  
  public ChangePad(BTState appState, boolean isForTarget, boolean isForModules, boolean isForGeneModSwap) {
    super(appState);
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    if (!isSingleSeg) {
      return (false);
    }
    if (isForGeneModSwap_) {    
      String lid = inter.getObjectID();  // Remember... this may not be the link we want!
      BusProperties bp = rcx.getLayout().getLinkProperties(lid);
      LinkSegmentID[] linkIDs = inter.segmentIDsFromIntersect();
      // Only allowed to perform this op if there is only one link through the segment! 
      Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);
      String myLinkId = throughSeg.iterator().next();
      Linkage link = rcx.getGenome().getLinkage(myLinkId);
      String target = link.getTarget();
      Node targNode = rcx.getGenome().getNode(target);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    ChangePadState retval = new ChangePadState(appState_, isForTarget_, isForModules_, dacx);
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
    ans.rcxT_.pixDiam = pixDiam;
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    
    if (isForGeneModSwap_) { 
      ans.nextStep_ = "stepSwitchGeneModule";
    } else {
      if (isForTarget_) {
        ans.nextStep_ = (ans.isForModule) ? "stepRelocateModuleTarget" : "stepRelocateTarget"; 
      } else {
        ans.nextStep_ = (ans.isForModule) ? "stepRelocateModuleSource" : "stepRelocateSource"; 
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class ChangePadState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection intersect;
    private DataAccessContext rcxT_;
    private boolean isForModule;
    private boolean isForTarget; 
    private int x;
    private int y;
    private String nextStep_;   
    private BTState appState_;

    /***************************************************************************
    **
    ** Constructor
    */
     
    private ChangePadState(BTState appState, boolean isForTarget, boolean isForModule, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;
      this.isForTarget = isForTarget;
      this.isForModule = isForModule;
      nextStep_ = "stepSetToMode";
    }
    
    /***************************************************************************
     **
     ** Next step...
     */ 
      
     public String getNextStep() {
       return (nextStep_);
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
                                                 isForModule, rcxT_)) {
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

      List<Intersection.AugmentedIntersection> augs = appState_.getGenomePresentation().intersectItem(x, y, rcxT_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxT_)).selectionRanker(augs);
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
      Node node = rcxT_.getGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
       
      List<Intersection.PadVal> pads = inter.getPadCand();
    
      if (LinkSupport.changeLinkTarget(appState_, intersect, inter, pads, id, rcxT_)) {
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
   //    Database db = appState_.getDB();
       String currentOverlay = appState_.getCurrentOverlay();
   
       //
       // Go and find if we intersect anything.
       //
  
       Intersection intersected = 
         appState_.getGenomePresentation().intersectANetModuleElement(x, y, rcxT_, GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
       //
       // If we don't intersect anything, or the correct thing, we need to bag it.
       //
  
       if (intersected == null) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
       }
       String id = intersected.getObjectID();
       NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
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
         rcxT_.getLayout().getNetOverlayProperties(currentOverlay).getNetModuleLinkagePropertiesFromTreeID(treeID);
       
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
  
       UndoSupport support = new UndoSupport(appState_, "undo.changeModuleLinkTarget");     
  
       //
       // We reject unless the target we are intersecting matches the
       // link we are trying to relocate.
       //
  
       Layout.PropChange lpc = rcxT_.getLayout().changeNetModuleTarget(currentOverlay, myLinkId, padPt, toSide, rcxT_);
       if (lpc != null) {
         PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, lpc);
         support.addEdit(mov);
         support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
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

     List<Intersection.AugmentedIntersection> augs = appState_.getGenomePresentation().intersectItem(x, y, rcxT_, true, false);
     Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxT_)).selectionRanker(augs);
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
      Node node = rcxT_.getGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      List<Intersection.PadVal> pads = inter.getPadCand();
   
      if (LinkSupport.changeLinkSource(appState_, intersect, inter, pads, id, rcxT_)) {
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
      String currentOverlay = rcxT_.oso.getCurrentOverlay();
  
      //
      // Go and find if we intersect anything.
      //
 
      Intersection intersected = 
        appState_.getGenomePresentation().intersectANetModuleElement(x, y, rcxT_, GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
      //
      // If we don't intersect anything, or the correct thing, we need to bag it.
      //

      if (intersected == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      String id = intersected.getObjectID();
      NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
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
        rcxT_.getLayout().getNetOverlayProperties(currentOverlay).getNetModuleLinkagePropertiesFromTreeID(treeID);
      
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

      UndoSupport support = new UndoSupport(appState_, "undo.changeModuleLinkSource");     

      //
      // We reject unless the target we are intersecting matches the
      // link we are trying to relocate.
      //

      Layout.PropChange lpc = rcxT_.getLayout().changeNetModuleSource(currentOverlay, treeID, padPt, toSide, rcxT_);
      if (lpc != null) {
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, lpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
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

      List<Intersection.AugmentedIntersection> augs = appState_.getGenomePresentation().intersectItem(x, y, rcxT_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxT_)).selectionRanker(augs);
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
      Node node = rcxT_.getGenome().getNode(id);
      if (node == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
       
      List<Intersection.PadVal> pads = inter.getPadCand();
      
      if (LinkSupport.changeLinkTargetGeneModule(appState_, intersect, pads, id, rcxT_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    } 
  }   
}
