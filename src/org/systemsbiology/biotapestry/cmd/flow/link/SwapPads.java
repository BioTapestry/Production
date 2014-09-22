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
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Handle swapping pads
*/

public class SwapPads extends AbstractControlFlow {

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
  
  public SwapPads(BTState appState) {
    super(appState);
    name = "linkPopup.SwapPads";
    desc = "linkPopup.SwapPads";
    mnem = "linkPopup.SwapPadsMnem";
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
    return (LinkSupport.amIValidForTargetOrSource(inter, LinkSupport.IS_FOR_SWAP, false, rcx));
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    SwapPadState retval = new SwapPadState(appState_, dacx);
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
        SwapPadState ans = (SwapPadState)last.currStateX;
        if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();      
        } else if (ans.getNextStep().equals("stepSwapPads")) {   
          next = ans.stepSwapPads();  
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
    SwapPadState ans = (SwapPadState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    ans.rcxT_.pixDiam = pixDiam;
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = "stepSwapPads"; 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class SwapPadState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection intersect;
    private DataAccessContext rcxT_;
    private int x;
    private int y;
    private String nextStep_;   
    private BTState appState_;

    /***************************************************************************
    **
    ** Constructor
    */
     
    private SwapPadState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;
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
      return (true);
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
      if (!LinkSupport.amIValidForTargetOrSource(intersect, LinkSupport.IS_FOR_SWAP, false, rcxT_)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } 
      
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.SWAP_PADS;
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the relocation
    */
      
    private DialogAndInProcessCmd stepSwapPads() { 
      
      //
      // Old pre-install mouse handler code:
      //
      
      LinkSupport.SwapType swapMode = LinkSupport.SwapType.NO_SWAP;
      LinkSegmentID[] segIDs = intersect.segmentIDsFromIntersect();
      String oid = intersect.getObjectID(); // May not be for link we want...
            
      String linkID = rcxT_.getLayout().segmentSynonymousWithTargetDrop(oid, segIDs[0]);
      boolean startOK = rcxT_.getLayout().segmentSynonymousWithStartDrop(oid, segIDs[0]);
      
      if ((linkID != null) && startOK) {
        if (segIDs[0].isForEndDrop()) {
          swapMode = LinkSupport.SwapType.TARGET_SWAP;
        } else if (segIDs[0].isForStartDrop()) {
          swapMode = LinkSupport.SwapType.SOURCE_SWAP;
        } else { 
          swapMode = LinkSupport.SwapType.EITHER_SWAP;
        }
      } else if (linkID != null) {
        swapMode = LinkSupport.SwapType.TARGET_SWAP;
      } else if (startOK) {
        swapMode = LinkSupport.SwapType.SOURCE_SWAP;
      } else {
        throw new IllegalStateException();
      }
   
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

      //
      // Now that we have an intersection, we can decide ambiguous swap types and find the
      // true link ID:
      //
      
      String myLinkId = null;
      if (swapMode != LinkSupport.SwapType.SOURCE_SWAP) {
        BusProperties bp = rcxT_.getLayout().getLinkProperties(oid);
        Set<String> throughSeg = bp.resolveLinkagesThroughSegment(segIDs[0]);
        if (throughSeg.size() != 1) {
          throw new IllegalStateException();
        }
        myLinkId = throughSeg.iterator().next();
        if (swapMode == LinkSupport.SwapType.EITHER_SWAP) {
          Linkage link = rcxT_.getGenome().getLinkage(myLinkId);
          String src = link.getSource();
          String trg = link.getTarget();
          if (id.equals(src)) {
            swapMode = LinkSupport.SwapType.SOURCE_SWAP;
          } else if (id.equals(trg)) {
            swapMode = LinkSupport.SwapType.TARGET_SWAP;
          } else {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
        }
      }

      //
      // Pads gotta be OK, node must be a target or source:
      //

      List<Intersection.PadVal> pads = inter.getPadCand();
      int numCand = pads.size();
      Intersection.PadVal winner = null;
      
      if (swapMode == LinkSupport.SwapType.TARGET_SWAP) {
        for (int i = 0; i < numCand; i++) {
          Intersection.PadVal pad = pads.get(i);
          if (!pad.okEnd) {
            continue;
          }
          if ((winner == null) || (winner.distance > pad.distance)) {
            winner = pad;
          }
        }    
        if (winner == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        } 
        Linkage link = rcxT_.getGenome().getLinkage(myLinkId);
        if (!link.getTarget().equals(id)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
      } else if (swapMode == LinkSupport.SwapType.SOURCE_SWAP) {
        for (int i = 0; i < numCand; i++) {
          Intersection.PadVal pad = pads.get(i);
          if (!pad.okStart) {
            continue;
          }
          if ((winner == null) || (winner.distance > pad.distance)) {
            winner = pad;
          }
        }    
        if (winner == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        String lid = intersect.getObjectID();    
        Set<String> allLinks = rcxT_.getLayout().getSharedItems(lid);
        myLinkId = allLinks.iterator().next();
        Linkage firstLink = rcxT_.getGenome().getLinkage(myLinkId);
        if (!firstLink.getSource().equals(id)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
      } else {
        throw new IllegalArgumentException();
      }

      if (LinkSupport.swapLinkPads(appState_, intersect, inter, winner, id, rcxT_, swapMode, myLinkId)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
    }  
  }
}
