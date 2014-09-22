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
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle changing segment parent
*/

public class RelocateSeg extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean isForModules_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RelocateSeg(BTState appState, boolean isForModules) {
    super(appState);
    isForModules_ = isForModules;
    name = (isForModules) ? "linkPopup.SegReloc" : "linkPopup.SegReloc";
    desc = (isForModules) ? "linkPopup.SegReloc" : "linkPopup.SegReloc";
    mnem = (isForModules) ? "linkPopup.SegRelocMnem" : "linkPopup.SegRelocMnem";      
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
    LinkSegmentID[] ids = inter.segmentIDsFromIntersect();
    if (ids.length != 1) {
      return (false);
    }
    String oid = inter.getObjectID();
 
    LinkProperties lp;
    if (isForModules_) {
      String ovrKey = rcx.oso.getCurrentOverlay();
      NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(ovrKey);
      lp = nop.getNetModuleLinkagePropertiesFromTreeID(oid);
      Set<String> throughSeg = lp.resolveLinkagesThroughSegment(ids[0]);
      if (!LinkSupport.haveModLinkDOFs(rcx, throughSeg, ovrKey)) {
        return (false);
      }
      //
      // Don't allow root drop reparent if it is the only tree coming out of the module.
      //
      int treeCount = nop.getOutboundTreeCountForModule(lp.getSourceTag());
      if ((ids[0].isDirect() || ids[0].isForStartDrop()) && (treeCount == 1)) {
        return (false);
      }
    } else {
      if (rcx.getGenome() instanceof GenomeInstance) {
        if (rcx.getGenomeAsInstance().getVfgParent() != null) {
          return (false);
        }
      }        
      lp = rcx.getLayout().getLinkProperties(oid);
    }  
    return (rcx.getLayout().canRelocateSegmentOnTree(lp, ids[0]));
  }
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, isForModules_, dacx);
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
        if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();      
        } else if (ans.getNextStep().equals("stepRelocate")) {   
          next = ans.stepRelocate();
        } else if (ans.getNextStep().equals("stepRelocateContinue")) {   
          next = ans.stepRelocateContinue();
        } else if (ans.getNextStep().equals("stepRelocateModule")) {   
          next = ans.stepRelocateModule();
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
    ans.rcxT_.pixDiam = pixDiam;
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = (ans.isForModule) ? "stepRelocateModule" : "stepRelocate"; 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection intersect;
    private DataAccessContext rcxT_;
    private boolean isForModule;
    private Intersection parentInter_;
    private BusProperties parentBp_;
    private int x;
    private int y;
    private String nextStep_;   
    private BTState appState_;

    /***************************************************************************
    **
    ** Constructor
    */
     
    private StepState(BTState appState, boolean isForModule, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;
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
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (isForModule) ? PanelCommands.Mode.RELOCATE_NET_MOD_LINK : PanelCommands.Mode.RELOCATE;
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the relocation of the given link to another segment of a link tree
    */
       
    private DialogAndInProcessCmd stepRelocate() {
 
      //
      // Go and find if we intersect anything.
      //
      List<Intersection.AugmentedIntersection> augs = appState_.getGenomePresentation().intersectItem(x, y, rcxT_, false, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxT_)).selectionRanker(augs);
      parentInter_ = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      //
      // If we don't intersect anything, we need to bag it.
      //

      if (parentInter_ == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      Object sub = parentInter_.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      SimpleUserFeedback suf = null;
      boolean keepGoing = true;
      String id = parentInter_.getObjectID();
      if (rcxT_.getGenome().getLinkage(id) == null) {
        Node node = rcxT_.getGenome().getNode(id);
        if (node != null) {
          LinkProperties lp = rcxT_.getLayout().getLinkProperties(intersect.getObjectID());
          String src = ((BusProperties)lp).getSourceTag();      
          if (!id.equals(src)) {
            String message = rcxT_.rMan.getString("linkSrcChange.useNodeSwitch");
            String title = rcxT_.rMan.getString("linkSrcChange.useNodeSwitchTitle");
            suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
          }
        }
        keepGoing = false;
      }

      //
      // We reject unless the link we are intersecting matches the
      // link we are trying to relocate.
      //

      parentBp_ = rcxT_.getLayout().getLinkProperties(id);
      if (parentBp_ != rcxT_.getLayout().getLinkProperties(intersect.getObjectID())) {
        String message = rcxT_.rMan.getString("linkSrcChange.useNodeSwitch");
        String title = rcxT_.rMan.getString("linkSrcChange.useNodeSwitchTitle");
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        keepGoing = false;
      }
      
      
      DialogAndInProcessCmd daipc;
      if (!keepGoing) {
        if (suf != null) {
          daipc = new DialogAndInProcessCmd(suf, ServerControlFlowHarness.ClickResult.REJECT, this);
        } else {
          daipc = new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this);
        }
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepRelocateContinue";
      }    
      return (daipc);               
    }
        
    /***************************************************************************
    **
    ** Do the relocation of the given link to another segment of a link tree
    */
       
    private DialogAndInProcessCmd stepRelocateContinue() {     
      
      LinkSegmentID[] segIDs = parentInter_.segmentIDsFromIntersect();
      // We reject unless the user selects a segment end point
      if (!segIDs[0].isTaggedWithEndpoint()) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      
      LinkSegmentID[] moveIDs = intersect.segmentIDsFromIntersect();
      Layout.PropChange lpc = rcxT_.getLayout().relocateSegmentOnTree(parentBp_, segIDs[0], moveIDs[0], null, null);
      if (lpc != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.linkrelocate");
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, new Layout.PropChange[] {lpc});
        support.addEdit(mov);
        support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
        appState_.getGenomePresentation().clearSelections(rcxT_, support);
        support.finish();
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      }
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
    }
      
    /***************************************************************************
    **
    ** Relocates the given link to another segment of a link tree from the same source, or
    ** a pad of the same source.
    */
        
    private DialogAndInProcessCmd stepRelocateModule() {
 
      String currentOverlay = rcxT_.oso.getCurrentOverlay();

      //
      // Go and find if we intersect anything.
      //
          
      boolean gotAModule = true;
      Intersection intersected = appState_.getGenomePresentation().intersectANetModuleElement(x, y, rcxT_,
                                                                                              GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
      if (intersected == null) {
        gotAModule = false;
        intersected = appState_.getGenomePresentation().intersectNetModuleLinks(x, y, rcxT_);
      }
 
      // Still nothing, we are done:
      
      if (intersected == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }    
      
      NetOverlayProperties nop = rcxT_.getLayout().getNetOverlayProperties(currentOverlay); 
      
      //
      // Link segment to move has to have same source as wherever we are going.
      //
      
      String movingTreeID = intersect.getObjectID();
      LinkProperties movLp = nop.getNetModuleLinkagePropertiesFromTreeID(movingTreeID);
      String modSource = movLp.getSourceTag();
      LinkSegmentID[] moveIDs = intersect.segmentIDsFromIntersect();
      Set<String> resolved = movLp.resolveLinkagesThroughSegment(moveIDs[0]);
      
      String targetID = intersected.getObjectID();
      Point2D padPt = null;
      Vector2D toSide = null;
      LinkSegmentID segID = null;
      boolean jumpingTrees = false;
      Point2D sourcePt;
           
      if (gotAModule) {
        if (!targetID.equals(modSource)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersected.getSubID();
        if (ei == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        if (ei.type != NetModuleFree.IntersectionExtraInfo.IS_PAD) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        //
        // If we are relocating the root drop, we are not allowed to use this as a change source pad
        //
        
        if (moveIDs[0].isDirect() || moveIDs[0].isForStartDrop()) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
       
        // New point:
        padPt = (Point2D)ei.intersectPt.clone();
        UiUtil.forceToGrid(padPt, UiUtil.GRID_SIZE);
        sourcePt = padPt;
        toSide = ei.padNorm.scaled(-1.0);
        
      } else {
        NetModuleLinkageProperties targlp = nop.getNetModuleLinkagePropertiesFromTreeID(targetID);
        String targSource = targlp.getSourceTag();  
        if (!targSource.equals(modSource)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        if (!targetID.equals(movingTreeID)) {
          jumpingTrees = true;
        }
        
        LinkSegmentID[] segIDs = intersected.segmentIDsFromIntersect();
        segID = segIDs[0];
        // We reject unless the user selects a segment end point
        if (!segID.isTaggedWithEndpoint()) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        } 
        sourcePt = targlp.getSourceStart(0.0);
      }
      
      //
      // Pretty lax about links in and out of modules.  Only restriction is that
      // link cannot end on the same pad as it starts (avoids zero-length links!)
      //
        
      NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
      NetworkOverlay novr = owner.getNetworkOverlay(currentOverlay); 
      NetModuleLinkageProperties nmlp = (NetModuleLinkageProperties)movLp;
      Iterator<String> tsit = resolved.iterator();
      while (tsit.hasNext()) {
        String linkID = tsit.next();
        if (!novr.linkIsFeedback(linkID)) {
          continue;
        }
        Point2D targPt = nmlp.getTargetEnd(linkID, 0.0);
        if (sourcePt.equals(targPt)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }       
      }

      //
      // Do the operation:
      //
      
      Layout.PropChange[] lpcm = null;
      if (gotAModule) {        
        lpcm = rcxT_.getLayout().supportModuleLinkTreeSwitch(moveIDs[0], resolved, currentOverlay,
                                                        movingTreeID, null, null, padPt, toSide, rcxT_);       
      } else if (jumpingTrees) {       
        lpcm = rcxT_.getLayout().supportModuleLinkTreeSwitch(moveIDs[0], resolved, currentOverlay,
                                                        movingTreeID, targetID, segID, null, null, rcxT_);        
      } else {       
        Layout.PropChange lpc = rcxT_.getLayout().relocateSegmentOnTree(movLp, segID, moveIDs[0], currentOverlay, rcxT_);
        if (lpc != null) {
          lpcm = new Layout.PropChange[1];
          lpcm[0] = lpc;
        }       
      }
      
      if ((lpcm != null) && (lpcm.length != 0)) {
        UndoSupport support = new UndoSupport(appState_, "undo.linkrelocate");
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, lpcm);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));    
        support.finish();
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      }
 
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
    } 
  }
}
