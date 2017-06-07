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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.OldPadMapper;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.DrawLinkInstanceCreationDialog;
import org.systemsbiology.biotapestry.ui.dialogs.DrawLinkInstanceExistingOptionsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.LinkCreationDialog;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle Adding Links
*/

public class AddLink extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean forModules_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddLink(boolean forModules) {
    forModules_ = forModules;
    name = (forModules_) ? "command.DrawNetworkModuleLink" : "command.AddLink";
    desc = (forModules_) ? "command.DrawNetworkModuleLink" : "command.AddLink";
    icon = (forModules_) ? "CreateNewModuleLink24.gif" : "NewLink24.gif";
    mnem = (forModules_) ? "command.DrawNetworkModuleLinkMnem" : "command.AddLinkMnem"; 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
 
  @Override  
  public boolean externallyEnabled() {
    return (forModules_);
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    if (forModules_) {
      throw new IllegalStateException();
    }  
    return (cache.canAddLink());
  }
  
  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    StepState ans = (StepState)cms;
    if (qbom.getLabel().equals("queBombDummy")) {
      return (ans.queBombDummy(qbom));
    } else {
      throw new IllegalArgumentException();
    }
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
        StepState ans = new StepState(cfh, forModules_);
        next = (forModules_) ? ans.stepStartModule() : ans.stepBiWarning();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepStart")) {
          next = ans.stepStart();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();
        } else if (ans.getNextStep().equals("stepGrowOrFinishLink")) {
          next = ans.stepGrowOrFinishLink();
        } else if (ans.getNextStep().equals("stepGrowOrFinishNetModuleLink")) {
          next = ans.stepGrowOrFinishNetModuleLink();
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
    ans.origX = theClick.x;
    ans.origY = theClick.y;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);  
    // Handles shift key forcing to orthogonal directions:
    boolean adding = (forModules_) ? (ans.currentModuleLinkCandidate_.buildState == NetModuleLinkCandidate.ADDING) :
                                     (ans.currentLinkCandidate_.buildState == LinkCandidate.ADDING);
    if (adding && isShifted) { 
      Point submit = new Point(ans.x, ans.y);
      ArrayList<Point> ptList = (forModules_) ? ans.currentModuleLinkCandidate_.points : ans.currentLinkCandidate_.points;
      Point2D lastPoint = ptList.get(ptList.size() - 1);
      Vector2D vec = (new Vector2D(lastPoint, submit)).canonical();
      Point2D canonicalPt = vec.add(lastPoint);
      ans.x = (int)canonicalPt.getX();
      ans.y = (int)canonicalPt.getY();
      ans.origX = ans.x;
      ans.origY = ans.y;
    }
    ans.setNextStep((forModules_) ? "stepGrowOrFinishNetModuleLink" : "stepGrowOrFinishLink"); 
    return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans));
  }
   
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.MouseClickCmdState {

    private boolean myForModules_;
    public NetModuleLinkCandidate currentModuleLinkCandidate_;
    public LinkCandidate currentLinkCandidate_;
    private String currentOverlay;
    private StaticDataAccessContext rcxR_;
     
    private int x;
    private int y;
    private int origX;
    private int origY;
     
    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(ServerControlFlowHarness cfh, boolean forModules) {
      super(cfh);
      myForModules_ = forModules;
      rcxR_ = dacx_.getContextForRoot();
      currentOverlay = rcxR_.getOSO().getCurrentOverlay();    
    }
     
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      if (myForModules_) {
        return (MainCommands.ALLOW_NAV_PUSH | MainCommands.SKIP_OVERLAY_VIZ_PUSH);
      } else {
        return (MainCommands.ALLOW_NAV_PUSH);
      }
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
      return (true);
    }
    
    public boolean noRootModel() {
      return (false);
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      return (!myForModules_);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (true);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.LINES_FLOATER);
    }
    
    public Object getFloater(int x, int y) {
      if (myForModules_) {
        if (currentModuleLinkCandidate_.buildState == AddLink.NetModuleLinkCandidate.ADDING) { 
          return (currentModuleLinkCandidate_.points);
        }
      } else {
        if (currentLinkCandidate_.buildState == AddLink.LinkCandidate.ADDING) { 
          return (currentLinkCandidate_.points);
        }    
      }
      return (null);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      return;
    }
    
    public Color getFloaterColor() {
      return (Color.BLACK);
    }

    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    private RemoteRequest.Result queBombDummy(RemoteRequest qbom) {
      return (null);
    }
   
    /***************************************************************************
    **
    ** Warn of build instructions
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
      nextStep_ = "stepStart";
      return (daipc);     
    }
     
    /***************************************************************************
    **
    ** Kick things off
    */
      
    private DialogAndInProcessCmd stepStartModule() {   
      currentModuleLinkCandidate_ = beginDrawNetworkModuleLink(currentOverlay);
      if (currentModuleLinkCandidate_ == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }  
      nextStep_ = "stepSetToMode";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this)); 
    }
      
    /***************************************************************************
    **
    ** Kick things off
    */
      
    private DialogAndInProcessCmd stepStart() { 
 
      currentLinkCandidate_ = addNewLinkStart();
      if (currentLinkCandidate_ == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
      boolean gotRegions = dacx_.currentGenomeIsAnInstance();
      if (gotRegions) {
        Map<String, Set<String>> okstm = currentLinkCandidate_.okSrcTrgMap;
        if (okstm != null) {
          Set<String> okTargs = okstm.keySet();
          if (okTargs == null) {
            throw new IllegalStateException();
          }
          Set<String> okTargLinks = new HashSet<String>();
          Iterator<String> oktit = okTargs.iterator();
          while (oktit.hasNext()) {
            String nodeID = oktit.next();
            okTargLinks.addAll(dacx_.getCurrentGenome().getOutboundLinks(nodeID));
          }
          HashSet<String> allOk = new HashSet<String>(okTargs);
          allOk.addAll(okTargLinks);
          uics_.getGenomePresentation().setTargets(allOk);
        }
        Genome rootParent = dacx_.getCurrentGenomeAsInstance().getVfgParentRoot();
        if (rootParent != null) {  // drawing at the subset layer...
          HashSet<String> overlays = new HashSet<String>();
          Set<String> oversrcs;
          if (okstm != null) {
            oversrcs = okstm.keySet();
          } else {
            oversrcs = new HashSet<String>();
            Iterator<Node> anit = dacx_.getCurrentGenome().getAllNodeIterator();
            while (anit.hasNext()) {
              Node node = anit.next();
              oversrcs.add(node.getID());
            }
          }
          Iterator<String> anit = oversrcs.iterator();
          while (anit.hasNext()) {
            String nodeID = anit.next();
            overlays.addAll(rootParent.getOutboundLinks(nodeID));
          }  
          uics_.getGenomePresentation().setRootOverlays(overlays);
        }
      }
      nextStep_ = "stepSetToMode";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this)); 
    }

    /***************************************************************************
    **
    ** Install a mouse handler
    */
    
    private DialogAndInProcessCmd stepSetToMode() {
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (myForModules_) ? PanelCommands.Mode.DRAW_NET_MODULE_LINK : PanelCommands.Mode.ADD_LINK;
      return (retval);
    } 
      
    /***************************************************************************
    **
    ** Keep going after each mouse click
    */   
       
    /*
    **
    ** Take the given X,Y location and either add it to a growing link candidate,
    ** or finish the link candidate off if a target is selected.
    */  

    private DialogAndInProcessCmd stepGrowOrFinishLink() {
      boolean gotRegions = !dacx_.currentGenomeIsRootDBGenome();
      // Catch state weirdness:
      if (currentLinkCandidate_.buildState == LinkCandidate.DONE) {
        throw new IllegalStateException();
      } 

      //
      // Reject if point overlaps another
      //

      int checkX = (int)((Math.round(x / 10.0)) * 10.0);
      int checkY = (int)((Math.round(y / 10.0)) * 10.0);
      Point checkPt = new Point(checkX, checkY);
      Iterator<Point> pit = currentLinkCandidate_.points.iterator();
      while (pit.hasNext()) {
        Point pt = pit.next();
        if (pt.equals(checkPt)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
      }

      //
      // Go and find if we intersect anything.
      //
 
      List<Intersection.AugmentedIntersection> augs = 
        uics_.getGenomePresentation().intersectItem(x, y, dacx_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, dacx_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;
      if (gotRegions && (inter != null) && (dacx_.getCurrentGenomeAsInstance().getGroup(inter.getObjectID()) != null)) {
        inter = null;
      }    

      //
      // When adding to subset models, we need to make it possible to draw
      // links after the first only from link segments.  So we need to be able to
      // intersect any link from the root parent and check for pad problems at
      // the root layer
      //

      boolean atSubsetLayer = false;
      Genome rootParent = null;
      if (gotRegions) {
        rootParent = dacx_.getCurrentGenomeAsInstance().getVfgParent();
        atSubsetLayer = (rootParent != null);
      }
      boolean linkFromRoot = false;
      if (inter == null) {
        if (currentLinkCandidate_.buildState == LinkCandidate.EMPTY) {
          if ((gotRegions) && (dacx_.getCurrentGenomeAsInstance().getVfgParent() != null)) {
            HashSet<String> okLinks = new HashSet<String>();
            Set<String> okTargs;
            Map<String, Set<String>> okstm = currentLinkCandidate_.okSrcTrgMap;
            if (okstm != null) {
              okTargs = okstm.keySet();
            } else {
              okTargs = new HashSet<String>();
              Iterator<Node> sit = dacx_.getCurrentGenome().getAllNodeIterator();
              while (sit.hasNext()) {
                Node node = sit.next();
                okTargs.add(node.getID());
              }  
            }
            Iterator<String> idit = okTargs.iterator();
            while (idit.hasNext()) {
              String nodeID = idit.next();
              okLinks.addAll(rootParent.getOutboundLinks(nodeID));
            }

           //   if (!sourcePadIsClear(genome, id, pad.padNum, render.sharedPadNamespaces())) {
         //       return (REJECT_);
          //    }
            List<Intersection> itemList = uics_.getGenomePresentation().selectLinkFromRootParent(x, y, dacx_, okLinks);
            inter = (new IntersectionChooser(true, dacx_).intersectionRanker(itemList));
            if (inter != null) {
              linkFromRoot = true;
            }
          }
        }
      }

      //
      // If we are empty and don't intersect anything, we need to bag it.  The
      // same thing applies if we intersect something that can't be a link source.
      //

      if (currentLinkCandidate_.buildState == LinkCandidate.EMPTY) {
        if (inter == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        } else {
          Object sub = inter.getSubID();
          if (sub == null) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
          String id = inter.getObjectID();
          Linkage link = dacx_.getCurrentGenome().getLinkage(id);
          if ((link == null) && linkFromRoot) {
            link = rootParent.getLinkage(id);
          }
          boolean recenter = true;
          if (link != null) {
            LinkSegmentID[] segIDs = inter.segmentIDsFromIntersect();
            // We reject unless the user selects a segment end point
            if (!segIDs[0].isTaggedWithEndpoint()) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
            currentLinkCandidate_.srcID = link.getSource();
            // we have a node intersection
          } else {
            if (!(sub instanceof Intersection.PadVal)) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
            NodeProperties nprop = dacx_.getCurrentLayout().getNodeProperties(id);
            INodeRenderer render = nprop.getRenderer();

            //
            // Issue #209: Working with gridded points (x,y) to choose pad is a mess when pads are off-grid (slashes, intercell). For the final
            // winner, we need to use the actual click point:
            //
            Node theNode = dacx_.getCurrentGenome().getNode(id);
            List<Intersection.PadVal> pads = render.calcPadIntersects(theNode, new Point2D.Double(origX, origY), dacx_);
            int numCand = pads.size();
            Intersection.PadVal winner = null;
            boolean flipIt = false;
            for (int i = 0; i < numCand; i++) {
              Intersection.PadVal pad = pads.get(i);
              if (!pad.okStart) {
                if ((theNode.getNodeType() == Node.SLASH) && !theNode.haveExtraPads())  {
                  flipIt = true;
                  pad.okStart = true;
                  pad.okEnd = false;
                } else {
                  continue;
                }
              }
              if (!LinkSupport.sourcePadIsClear(uics_, dacx_, dacx_.getCurrentGenome(), id, pad.padNum, render.sharedPadNamespaces())) {
                continue;
              }
              if (atSubsetLayer) {
                if (!LinkSupport.sourcePadIsClear(uics_, dacx_, rootParent, id, pad.padNum, render.sharedPadNamespaces())) {
                  continue;
                }          
              }
              if ((winner == null) || (winner.distance > pad.distance)) {
                winner = pad;
              }
            }    
            if (winner == null) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
            // Want to hide the flip from the user: use the landing offset up until we flip the node orientation:
            Vector2D loff = (flipIt) ? render.getLandingPadOffset(winner.padNum, dacx_.getCurrentGenome().getNode(id), Linkage.NONE, dacx_)
                                     : render.getLaunchPadOffset(winner.padNum, dacx_.getCurrentGenome().getNode(id), dacx_);
            Point2D cent = loff.add(nprop.getLocation());
            x = (int)cent.getX();
            y = (int)cent.getY();
            recenter = false;
            currentLinkCandidate_.srcID = id;
            currentLinkCandidate_.slashFlipSrc = flipIt;
          }
          if (gotRegions) {
            Map<String, Set<String>> okstm = currentLinkCandidate_.okSrcTrgMap;
            if (okstm != null) {
              Set<String> okTargs = okstm.get(currentLinkCandidate_.srcID);
              if (okTargs == null) {
                return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
              }
              uics_.getGenomePresentation().setTargets(okTargs);
            }
            uics_.getGenomePresentation().clearRootOverlays();
          }        
          currentLinkCandidate_.source = inter;
          currentLinkCandidate_.buildState = LinkCandidate.ADDING;
          if (recenter) {
            x = (int)((Math.round(x / 10.0)) * 10.0);
            y = (int)((Math.round(y / 10.0)) * 10.0);
          }
          currentLinkCandidate_.points.add(new Point(x, y));
          uics_.getGenomePresentation().setFloater(currentLinkCandidate_.points);
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
        }
      }

      //
      // If we are adding, we will be done if we intersect something that we are 
      // allowed to target; else we ignore this point and return a balk code.   
      //

      if (currentLinkCandidate_.buildState == LinkCandidate.ADDING) {
        if (inter == null) {
          x = (int)((Math.round(x / 10.0)) * 10.0);
          y = (int)((Math.round(y / 10.0)) * 10.0);        
          currentLinkCandidate_.points.add(new Point(x, y));
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
        } else {
          Object sub = inter.getSubID();
          if ((sub == null) || !(sub instanceof Intersection.PadVal)) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
          String id = inter.getObjectID();
          INodeRenderer render = dacx_.getCurrentLayout().getNodeProperties(id).getRenderer();
          //
          // Issue #209: Working with gridded points (x,y) to choose pad is a mess when pads are off-grid (slashes, intercell). For the final
          // winner, we need to use the actual click point:
          //
          Node theNode = dacx_.getCurrentGenome().getNode(id);
          List<Intersection.PadVal> pads = render.calcPadIntersects(theNode, new Point2D.Double(origX, origY), dacx_);
          // Fixing ISSUE #247: calcPadIntersects can return null!
          int numCand = (pads == null) ? 0 : pads.size();
          Intersection.PadVal winner = null;
          boolean flipIt = false;
          for (int i = 0; i < numCand; i++) {
            Intersection.PadVal pad = pads.get(i);
            if (!pad.okEnd) {
              if ((theNode.getNodeType() == Node.SLASH) && !theNode.haveExtraPads())  {
                flipIt = true;
                pad.okStart = false;
                pad.okEnd = true;
              } else {
                continue;
              }
            }
            if (!LinkSupport.targPadIsClear(dacx_.getCurrentGenome(), id, pad.padNum, render.sharedPadNamespaces())) {
              continue;
            }
            if (atSubsetLayer) {
              if (!LinkSupport.targPadIsClear(rootParent, id, pad.padNum, render.sharedPadNamespaces())) {
                continue;
              }          
            }       
            if ((winner == null) || (winner.distance > pad.distance)) {
              winner = pad;
            }
          }
          if (winner == null) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          } 
          // Don't allow tiny links that start and end on the same object
          if ((currentLinkCandidate_.points.size() == 1) && 
              (currentLinkCandidate_.source.getObjectID().equals(inter.getObjectID()))) {
            Point firstPt = currentLinkCandidate_.points.get(0);
            if ((Math.abs(checkPt.x - firstPt.x) <= 20) &&
                (Math.abs(checkPt.y - firstPt.y) <= 20)) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
          }
          
          //
          // If we are targeting a gene with a region, gotta match the region!
          //
          if (theNode.getNodeType() == Node.GENE) {
            Gene theGene = (Gene)theNode;
            DBGeneRegion reg = (theGene.getNumRegions() > 0) ? theGene.getRegionForPad(winner.padNum) : null; // May be null
            if ((reg != null) && reg.isHolder()) {
              reg = null;
            }
            if (currentLinkCandidate_.existingLinkID != null) {
              DBGenome dbg = dacx_.getDBGenome();
              Linkage dbLink = dbg.getLinkage(currentLinkCandidate_.existingLinkID);
              Gene dbGene = dbg.getGene(GenomeItemInstance.getBaseID(theGene.getID()));
              DBGeneRegion regR = dbGene.getRegionForPad(dbLink.getLandingPad()); // may be null
              if (regR != null) { // Parent link in module, we do not match it
                if ((reg == null) || !reg.equals(regR)) {
                  return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
                }      
              } else { // Parent link not in module, be we are:
                if (reg != null) {
                  return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
                } 
              }
            }
            currentLinkCandidate_.modTarg = (reg == null) ? null : reg.getKey();              
          }

          ArrayList<Intersection.PadVal> onePad = new ArrayList<Intersection.PadVal>();
          onePad.add(winner);
          currentLinkCandidate_.target = new Intersection(inter.getObjectID(), onePad);
          if (gotRegions) {
            Map<String, Set<String>> okstm = currentLinkCandidate_.okSrcTrgMap;
            if (okstm != null) {
              Set<String> okTargs = okstm.get(currentLinkCandidate_.srcID);
              if (okTargs == null) {
                throw new IllegalStateException();
              }
              if (!okTargs.contains(inter.getObjectID())) {
                return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
              }
            }
          }        
          currentLinkCandidate_.buildState = LinkCandidate.DONE;
          currentLinkCandidate_.slashFlipTrg = flipIt;
          addNewLinkFinish(dacx_, currentLinkCandidate_);

          PlugInManager pluginManager = uics_.getPlugInMgr(); 
          Iterator<ModelBuilderPlugIn> mbIterator = pluginManager.getBuilderIterator();
          if (mbIterator.hasNext()) {
            mbIterator.next().linkJustDrawn(dacx_, currentLinkCandidate_.builtID);
          }        
          uics_.getGenomePresentation().setFloater(null);
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
        }
      }
 
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
    }    
 
    /***************************************************************************
    **
    ** Take the given X,Y location and either add it to a growing link candidate,
    ** or finish the link candidate off if a target is selected.
    */  

    private DialogAndInProcessCmd stepGrowOrFinishNetModuleLink() {
      String currentOverlay = dacx_.getOSO().getCurrentOverlay();
      // Catch state weirdness:
      if (currentModuleLinkCandidate_.buildState == NetModuleLinkCandidate.DONE) {
        throw new IllegalStateException();
      } 
      
      int checkX = UiUtil.forceToGridValueInt(x, UiUtil.GRID_SIZE);
      int checkY = UiUtil.forceToGridValueInt(y, UiUtil.GRID_SIZE);  
      Point checkPt = new Point(checkX, checkY); 
      
      //
      // Reject if point overlaps another
      //

      Iterator<Point> pit = currentModuleLinkCandidate_.points.iterator();
      while (pit.hasNext()) {
        Point pt = pit.next();
        if (pt.equals(checkPt)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
      }
    
      //
      // Go and find if we intersect anything.
      //
  
      //
      // If we are empty and don't intersect anything, we need to bag it.  The
      // same thing applies if we intersect something that can't be a link source.
      //

      boolean gotAModule = true;
      Intersection intersected = uics_.getGenomePresentation().intersectANetModuleElement(x, y, dacx_,
                                                                                              GenomePresentation.NetModuleIntersect.NET_MODULE_LINK_PAD);
      if (intersected == null) {
        gotAModule = false;
        intersected = uics_.getGenomePresentation().intersectNetModuleLinks(x, y, dacx_);
      }
          
      //
      // If we are empty and don't intersect anything, we need to bag it.  The
      // same thing applies if we intersect something that can't be a link source.
      //

      if (currentModuleLinkCandidate_.buildState == NetModuleLinkCandidate.EMPTY) {
        if (intersected == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        } else {
          Object sub = intersected.getSubID();
          if (sub == null) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
          String id = intersected.getObjectID();
          if (!gotAModule) {
            LinkSegmentID[] segIDs = intersected.segmentIDsFromIntersect();
            // We reject unless the user selects a segment end point
            if (!segIDs[0].isTaggedWithEndpoint()) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
            // Note that we are working with tree IDs for link intersection, not link IDs:
            NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(currentOverlay);
            NetModuleLinkageProperties currNmlp = nop.getNetModuleLinkagePropertiesFromTreeID(id);
            currentModuleLinkCandidate_.srcMod = currNmlp.getSourceTag();
            // we have a module intersection
          } else {
            NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersected.getSubID();
            if (ei.type != NetModuleFree.IntersectionExtraInfo.IS_PAD) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
            x = (int)ei.intersectPt.getX();
            y = (int)ei.intersectPt.getY();            
            currentModuleLinkCandidate_.srcMod = id;
          }
          currentModuleLinkCandidate_.source = intersected;
          currentModuleLinkCandidate_.srcIsModule = gotAModule;
          currentModuleLinkCandidate_.buildState = AddLink.LinkCandidate.ADDING;

          x = UiUtil.forceToGridValueInt(x, UiUtil.GRID_SIZE);
          y = UiUtil.forceToGridValueInt(y, UiUtil.GRID_SIZE);
          currentModuleLinkCandidate_.points.add(new Point(x, y));
          uics_.getGenomePresentation().setFloater(currentModuleLinkCandidate_.points);
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
        }
      }

      //
      // If we are adding, we will be done if we intersect something that we are 
      // allowed to target; else we ignore this point and return a balk code.   
      //

      
      if (currentModuleLinkCandidate_.buildState == NetModuleLinkCandidate.ADDING) { 
        if (intersected == null) {
          x = UiUtil.forceToGridValueInt(x, UiUtil.GRID_SIZE);
          y = UiUtil.forceToGridValueInt(y, UiUtil.GRID_SIZE);
          currentModuleLinkCandidate_.points.add(new Point(x, y));
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
        } else if (!gotAModule) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));    
        } else {
          NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersected.getSubID();
          if (ei.type != NetModuleFree.IntersectionExtraInfo.IS_PAD) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
  
          x = (int)ei.intersectPt.getX();
          y = (int)ei.intersectPt.getY();      
          x = UiUtil.forceToGridValueInt(x, UiUtil.GRID_SIZE);
          y = UiUtil.forceToGridValueInt(y, UiUtil.GRID_SIZE);
          Point padPt = new Point(x, y);
          
          //
          // Don't have lots of drawing restrictions.  But we cannot have a feedback
          // loop that starts and ends on the same pad:
          //
          
          if (currentModuleLinkCandidate_.srcMod.equals(intersected.getObjectID())) {
            Point srcPad;
            if (currentModuleLinkCandidate_.srcIsModule) {
              srcPad = currentModuleLinkCandidate_.points.get(0);
            } else {
              NetOverlayProperties nop = dacx_.getCurrentLayout().getNetOverlayProperties(currentOverlay);
              String treeID = currentModuleLinkCandidate_.source.getObjectID();
              NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
              Point2D startPt = nmlp.getSourceStart(0.0);    
              srcPad = new Point((int)startPt.getX(), (int)startPt.getY());    
            }           
            if (srcPad.equals(padPt)) {
              return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
            }
          } 
   
          //
          // Add the last point in (unlike regular links, these drops
          // record the end points);
          //
          
          currentModuleLinkCandidate_.points.add(padPt);             
          currentModuleLinkCandidate_.trgMod = intersected.getObjectID();
          currentModuleLinkCandidate_.target = intersected;
          currentModuleLinkCandidate_.buildState = AddLink.LinkCandidate.DONE;
          uics_.getGenomePresentation().setFloater(null);
          finishDrawNetworkModuleLink(currentModuleLinkCandidate_);          
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
        }
      }
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
    }          
      
    /***************************************************************************
    **
    ** Start adding a link
    */  
   
    private LinkCandidate addNewLinkStart() {
      String existingLinkID = null;
      Map<String, Set<String>> okSrcTrgMap = null;
      
      String newName = null;
      int newSign = Linkage.NONE;       
      if (dacx_.currentGenomeIsRootDBGenome()) {
        //
        // Get the type and the name
        //
        LinkCreationDialog lcd = new LinkCreationDialog(uics_, dacx_);
        lcd.setVisible(true);
        if (!lcd.haveResult()) {
          return (null);
        }
        newSign = lcd.getSign();
        newName = lcd.getName();
        existingLinkID = null;
      } else {
        DrawLinkInstanceCreationDialog lcd = new DrawLinkInstanceCreationDialog(uics_, dacx_, rcxR_.getCurrentGenome(), dacx_.getCurrentGenomeAsInstance());
        lcd.setVisible(true);
        if (!lcd.haveResult()) {
          return (null);
        }
        existingLinkID = lcd.getID();
        if (existingLinkID == null) {
          newSign = lcd.getSign();
          newName = lcd.getName();
        } else {        
          if (lcd.haveImmediateAdd()) {
            LinkCandidate lc = new LinkCandidate(newName, newSign, dacx_.getCurrentLayout(), existingLinkID, okSrcTrgMap);
            addNewLinkToInstanceFinish(dacx_, true, lc);
            return (null);
          } else {
            okSrcTrgMap = lcd.getOkSrcTrgMap();
            Linkage oldLink = rcxR_.getCurrentGenome().getLinkage(existingLinkID);
            newSign = oldLink.getSign();
            newName = oldLink.getName();
          }     
        }
      }
      
      //
      // Bundle the info into a LinkCandidate and return the result
      //
  
      LinkCandidate lc = new LinkCandidate(newName, newSign, dacx_.getCurrentLayout(), existingLinkID, okSrcTrgMap);
      return (lc); 
    } 
     
    /***************************************************************************
    **
    ** Add a link
    */  
   
    private boolean addNewLinkFinish(StaticDataAccessContext rcxI, LinkCandidate lc) {
      if (rcxI.currentGenomeIsRootDBGenome()) {
        return (addNewLinktoRootFinish(lc));
      } else {
        return (addNewLinkToInstanceFinish(rcxI, false, lc));
      }
    }
     
    /***************************************************************************
    **
    ** Add a link to the root genome (NOTE: dacx_ here is the same as rcxR_)
    */  
   
    private boolean addNewLinktoRootFinish(LinkCandidate lc) {
      //
      // Add it to an existing tree:
      //
      String linkID = dacx_.getCurrentGenomeAsDBGenome().getNextKey();
      lc.builtID = linkID;
      String src = lc.source.getObjectID();
      if (dacx_.getCurrentGenome().getLinkage(src) != null) {
        return (addToRootLinkTree(linkID, lc));
      }
  
      //
      // Undo/Redo support
      //
  
      UndoSupport support = uFac_.provideUndoSupport("undo.addLink", dacx_);
      
      int srcPad = ((Intersection.PadVal)lc.source.getSubID()).padNum;
      String targ = lc.target.getObjectID();
      
      //
      // Suggest the user uses links with no arrow or foot for some nodes
      //
      
      switchLinkSign(dacx_, targ, lc);
     
      int trgPad = ((Intersection.PadVal)lc.target.getSubID()).padNum;    
      DBLinkage newLinkage = new DBLinkage(dacx_, lc.label, linkID, src, targ, lc.sign, 
                                           trgPad, srcPad);
      
      GenomeChange gc = dacx_.getCurrentGenomeAsDBGenome().addLinkWithExistingLabel(newLinkage);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
      }
      
      //
      // Create a new LinkProperties; add all the segments
      //
      
      BusProperties lp = convertCandidateToBus(dacx_, lc, linkID, src);   
      
      //
      // Propagate node properties to all interested getLayout()s
      //
        
      PropagateSupport.propagateLinkProperties(dacx_, lp, linkID, support);
      
      
      if (lc.slashFlipSrc) {
        flipASlashNode(dacx_, src, support, null);
      }
      if (lc.slashFlipTrg) {
        flipASlashNode(dacx_, targ, support, null);
      }
           
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      support.finish();
      return (true); 
    }
     
    /***************************************************************************
    **
    ** Flip a slash node
    */  
   
    private int flipASlashNode(StaticDataAccessContext rcx, String nodeID, UndoSupport support, Integer otherOldOrient) {   
      NodeProperties oldProps = rcx.getCurrentLayout().getNodeProperties(nodeID); 
      int oldOrient = oldProps.getOrientation();
      if ((otherOldOrient != null) && (otherOldOrient.intValue() != oldOrient)) {
        return (oldOrient);
      } 
      Layout.PropChange[] lpc = new Layout.PropChange[1];
      NodeProperties newProps = oldProps.clone();
      newProps.setOrientation(NodeProperties.reverseOrient(oldOrient));
      lpc[0] = rcx.getCurrentLayout().replaceNodeProperties(oldProps, newProps);
      if (lpc != null) {
        PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
        support.addEdit(pcc);
      }
      return (oldOrient);
    }

    /***************************************************************************
    **
    ** Suggest the user uses links with no arrow or foot for some nodes
    */  
   
    private void switchLinkSign(StaticDataAccessContext rcxi, String targ, LinkCandidate lc) {
      Genome genome = rcxi.getCurrentGenome();
      Node trgNode = genome.getNode(targ);
      int trgType = trgNode.getNodeType();
      if ((trgType == Node.INTERCELL) || (trgType == Node.SLASH)) {
        if (lc.sign != Linkage.NONE) {
          ResourceManager rMan = dacx_.getRMan();
          int changeSignVal = JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                                            rMan.getString("badSign.changeToNeutral"),
                                                            rMan.getString("badSign.changeToNeutralTitle"),
                                                            JOptionPane.YES_NO_OPTION);
          if (changeSignVal == JOptionPane.YES_OPTION) {
            lc.sign = Linkage.NONE;
          }    
        }
      }
      return;
    }
   
    /***************************************************************************
    **
    ** Add a segment to an existing LinkProperties. (NOTE: dacx_ here is the same as rcxR_)
    */  
   
    private boolean addToRootLinkTree(String linkID, LinkCandidate lc) {
      //
      // We have intersected a link segment as the source of a new link.
      // We find the source of the link and create a new DBLinkage for
      // it.  Then we take the existing LinkProperties and add
      // a new segment on.
      //
  
      String src = lc.source.getObjectID();
      LinkSegmentID[] segIDs = lc.source.segmentIDsFromIntersect();
  
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.addLink", dacx_);   
      
      Linkage existing = dacx_.getCurrentGenome().getLinkage(src);
      int srcPad = existing.getLaunchPad();
      String srcNode = existing.getSource();
      String targ = lc.target.getObjectID();
      int trgPad = ((Intersection.PadVal)lc.target.getSubID()).padNum;
      
      //
      // Suggest the user uses links with no arrow or foot for some nodes
      //
      
      switchLinkSign(dacx_, targ, lc);
    
      // FIX ME?? Don't duplicate links?    
      DBLinkage newLinkage = new DBLinkage(dacx_, lc.label, linkID, srcNode, targ, lc.sign, 
                                           trgPad, srcPad);
      GenomeChange gc = dacx_.getCurrentGenomeAsDBGenome().addLinkWithExistingLabel(newLinkage);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
      }
  
      //
      // Create a new LinkProperties; add all the segments
      //
      
      BusProperties nlp = convertCandidateToBusForMerge(dacx_, lc, linkID);
      Layout.PropChange lpc = dacx_.getCurrentLayout().mergeNewLinkToTreeAtSegment(dacx_, src, nlp, segIDs[0]);
      PropChangeCmd pcc = new PropChangeCmd(dacx_, new Layout.PropChange[] {lpc});
      support.addEdit(pcc);    
  
      //
      // Propagate link properties to all interested layouts
      //
      
      // FIX ME: NO PROPAGATION (HOW WOULD YOU???)
      // BUT INSTALL INTO LAYOUT HANDLED ABOVE
         
      //propagateLinkProperties(db, genome, nlp, linkID);
      
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      support.finish();
      return (true); 
    }
  
    /***************************************************************************
    **
    ** Convert a link candidate to a BusProperties
    */  
   
    private BusProperties convertCandidateToBus(StaticDataAccessContext rcxI, LinkCandidate lc, String linkID, String src) {
      //
      // Create a new LinkProperties; add all the segments
      //    
     
      Point2D textLoc = new Point2D.Double();
      List<LinkSegment> segments = convertCandidateListToSegments(lc.points, textLoc, false);
      
      //
      // Get link to match color of node, or choose a new color
      //
      
      String colorTag = chooseNodeColor(src);
      BusProperties lp = new BusProperties(rcxI.getCurrentGenome(), linkID, (float)textLoc.getX(), (float)textLoc.getY(), colorTag);
      Iterator<LinkSegment> sit = segments.iterator();
      while (sit.hasNext()) {
        LinkSegment ls = sit.next();
        lp.addSegmentInSeries(ls);
      }
      return (lp);
    }
  
    /***************************************************************************
    **
    ** Choose node color
    */  
  
    private String chooseNodeColor(String src) { 
      String colorTag = findNodeColorForLink(src);
      if (colorTag == null) {
        return ("black");
      } else if (colorTag.equals("black")) {
        if (dacx_.currentGenomeIsAnInstance()) {
          colorTag = findGenomeInstanceConsensusColor(src);
          if ((colorTag == null) || colorTag.equals("black")) {
            colorTag = dacx_.getColorResolver().getNextColor(dacx_.getGenomeSource(), dacx_.getLayoutSource());  
          }
        } else {
          colorTag = dacx_.getColorResolver().getNextColor(dacx_.getGenomeSource(), dacx_.getLayoutSource());
        }
      }
      return (colorTag);
    }

    /***************************************************************************
    **
    ** Find the consensus of existing colors for all existing instances of
    ** a node:
    */  
  
    private String findGenomeInstanceConsensusColor(String src) { 
      
      GenomeInstance gi = dacx_.getCurrentGenomeAsInstance();
      GenomeInstance root = gi.getVfgParentRoot();
      if (root != null) {
        gi = root;
      }
      Set<String> instances = gi.getNodeInstances(GenomeItemInstance.getBaseID(src));
      
      HashMap<String, Integer> colorCounts = new HashMap<String, Integer>();
      Iterator<String> iit = instances.iterator();
      while (iit.hasNext()) {
        String nodeID = iit.next();
        if (nodeID.equals(src)) {
          continue;
        }
        String linkColor = findNodeColorForLink(nodeID);
        if ((linkColor == null) || linkColor.equals("black")) {
          continue;
        }
        Integer count = colorCounts.get(linkColor);
        Integer newCount = (count == null) ? new Integer(1) : new Integer(count.intValue() + 1);
        colorCounts.put(linkColor, newCount);
      }
      int maxCount = Integer.MIN_VALUE;
      String maxColor = null;
      Iterator<String> cckit = colorCounts.keySet().iterator();
      while (cckit.hasNext()) {
        String color = cckit.next();
        Integer count = colorCounts.get(color);
        int countVal = count.intValue();
        if (countVal > maxCount) {
          maxCount = countVal;
          maxColor = color;
        }
      }
      return (maxColor);  // may be null..
    }
    
    /***************************************************************************
    **
    ** Find current node color used for links.  Null if node color does not
    ** drive link color
    */  
  
    private String findNodeColorForLink(String src) { 
      String colorTag = null;
      Node srcNode = dacx_.getCurrentGenome().getNode(src);
      if (NodeProperties.setWithLinkColor(srcNode.getNodeType())) {
        NodeProperties srcProp = dacx_.getCurrentLayout().getNodeProperties(src);
        if (srcNode.getNodeType() == Node.INTERCELL) {
          colorTag = srcProp.getSecondColorName();
          if (colorTag == null) {
            colorTag = srcProp.getColorName();
          }
        } else {
          colorTag = srcProp.getColorName();
        }
        if (colorTag == null) {
          colorTag = "black";
        }
      }  
      return (colorTag);
    } 
  
    /***************************************************************************
    **
    ** Convert a link candidate to a BusProperties for merging with existing
    */  
   
    private BusProperties convertCandidateToBusForMerge(StaticDataAccessContext rcx, LinkCandidate lc, String linkID) {  
      //
      // Create a new LinkProperties; add all the segments
      //
      
      int numSeg = lc.points.size() - 1;
      ArrayList<LinkSegment> segments = new ArrayList<LinkSegment>();
      float tx = 0.0F;
      float ty = 0.0F;
      if (numSeg == 0) {
        Point pt = lc.points.get(0);
        Point2D newPt = new Point2D.Float(pt.x, pt.y);
        LinkSegment ls = new LinkSegment(null, null, newPt, null);
        segments.add(ls);            
        tx = pt.x;
        ty = pt.y;  
      } else {
        Point2D lastPt = null;
        Iterator<Point> pit = lc.points.iterator();
        int halfSeg = numSeg / 2;
        int segCount = 0;
        while (pit.hasNext()) {
          Point pt = pit.next();
          Point2D newPt = new Point2D.Float(pt.x, pt.y); 
          if (lastPt != null) {
            LinkSegment ls = new LinkSegment(null, null, lastPt, newPt);
            segments.add(ls);
          }
          lastPt = newPt;
          if (segCount == halfSeg) {
            tx = pt.x;
            ty = pt.y;         
          }
          segCount++;
        }
      }
      BusProperties nlp = new BusProperties(rcx.getCurrentGenome(), linkID, tx, ty, "black");
      Iterator<LinkSegment> sit = segments.iterator();
      while (sit.hasNext()) {
        LinkSegment ls = sit.next();
        // Handles degenerate creation automatically
        nlp.addSegmentInSeries(ls);
      }
      
      return (nlp);
    }

    /***************************************************************************
    **
    ** Finish placing link in an instance, and get it into the
    ** root too if needed.
    */  
    
    private boolean addNewLinkToInstanceFinish(StaticDataAccessContext rcxI, boolean immed, LinkCandidate lc) {
      
      ArrayList<GenomeInstance> ancestry = new ArrayList<GenomeInstance>();
      ancestry.add(rcxI.getCurrentGenomeAsInstance());
      GenomeInstance parent = rcxI.getCurrentGenomeAsInstance().getVfgParent();
      while (parent != null) {
        ancestry.add(parent);
        parent = parent.getVfgParent();
      } 
      int aNum = ancestry.size();
      
      Set<String> rootOptions;
      Map<String, InstanceAndCeiling> instanceOptions;
      InstanceAndCeiling iAndC;
     
      //
      // Suggest the user uses links with no arrow or foot for some nodes
      //

      if (lc.existingLinkID == null) {
        switchLinkSign(rcxI, lc.target.getObjectID(), lc);
      }
      
      //
      // User has drawn a link, i.e. we have a non-null link candidate.
      // This drawn link can occur either without an existing link ID
      // already chosen, or with an existing link ID chosen.  If the
      // existing link was not chosen, we may encounter a case where the
      // user has recapitulated an existing link, and we need to report that.
      //
      
      if (!immed) {
        if (lc.existingLinkID == null) {
          rootOptions = newLinkInstanceHasParent(rcxR_.getCurrentGenome(), lc, rcxI.getCurrentGenomeAsInstance());
          instanceOptions = findInstanceMatches(lc, ancestry);
          iAndC = null;
        } else {
          //
          // This is an immediate add situation, where the user specified a
          // link ID, but needed to also generate a link candidate to give
          // us the link geometry to use.  Note that this means we are dealing
          // with a root link ID!
          rootOptions = null;
          instanceOptions = null;
          iAndC = null;
        }
      } else { 
        // This is an immediate add situation.  User has specified a link ID.
        // Note that this requires that the link existed at at least the root
        // instance layer.
        rootOptions = null;
        instanceOptions = null;
        iAndC = findInstanceMatch(lc.existingLinkID, ancestry);
      }
 
      //
      // If we have some root or parent instance options that fill the requirement, 
      // ask if we are to use that one instead of creating a new one.
      //
      // Note that above logic requires (lc != null) && (existingLinkID_ == null) && (iAndC == null) 
      // to fire this case:
      
      if ((rootOptions != null) || (instanceOptions != null)) {
        Set<String> rootOptionSet = (rootOptions == null) ? new HashSet<String>() : rootOptions;
        Set<String> instanceOptionSet = (instanceOptions == null) ? new HashSet<String>() : instanceOptions.keySet();
      
        DrawLinkInstanceExistingOptionsDialog eod = 
          new DrawLinkInstanceExistingOptionsDialog(uics_, rcxI, rcxR_.getCurrentGenome(), rcxI.getCurrentGenomeAsInstance(), rootOptionSet, instanceOptionSet);
        eod.setVisible(true);
        if (!eod.haveResult()) {
          return (false);
        }
  
        if (!eod.doDraw()) {
          String linkID = eod.getID();
          if (!GenomeItemInstance.isBaseID(linkID)) {
            iAndC = instanceOptions.get(linkID);
          } else {
            lc.existingLinkID = linkID;
          }
          
          // Bug BT-11-30-12:01 says that if we choose the root option and the option
          // is also in the parent instance, we incorrectly duplicate the *same* root link
          // in the parent instance!  Need to fix this...
          if ((lc.existingLinkID != null) && (instanceOptions != null)) {
            Iterator<String> iit = instanceOptions.keySet().iterator();
            while (iit.hasNext()) {
              String intLinkID = iit.next();
              if (GenomeItemInstance.getBaseID(intLinkID).equals(lc.existingLinkID)) {
                iAndC = instanceOptions.get(intLinkID);
                break;
              }
            }
          }
        }   
      }
    
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.addLink", rcxI);        
    
      // Now we work top-down from ceiling:
      Collections.reverse(ancestry);    
      //
      // Need to add link into root instance.  This will add the link into the
      // full genome too if it is not there:
      //
      if (iAndC == null) {
        GenomeInstance gi = ancestry.get(0);
        StaticDataAccessContext irx = new StaticDataAccessContext(rcxI, gi, rcxI.getCurrentLayout());
        String linkInstanceToUse = addNewLinkToRootInstanceFinish(irx, lc, support).getID();
        iAndC = new InstanceAndCeiling(linkInstanceToUse, 0);
      }
       
      //
      // Add to all the kids:
      //
      LinkageInstance pInst = null;    
      for (int i = iAndC.propCeiling; i < aNum; i++) {
        GenomeInstance gi = ancestry.get(i);
        if (i == iAndC.propCeiling) { // first time thru
          pInst = (LinkageInstance)gi.getLinkage(iAndC.instanceToUse);
        } else {
          // Bug BT-11-30-12:01 manifests here as well.  Do not add if already there!
          LinkageInstance checkpI = (LinkageInstance)gi.getLinkage(pInst.getID());
          if (checkpI == null) {
            StaticDataAccessContext irx = new StaticDataAccessContext(rcxI, gi, rcxI.getCurrentLayout());
            pInst = PropagateSupport.addNewLinkToSubsetInstance(irx, pInst, support);
          } else {
            pInst = checkpI;
          }
        }
        if (lc.builtID == null) {
          lc.builtID = pInst.getID();
        }
      }      
  
      support.finish();    
      return (true); 
    }   
  
    /***************************************************************************
    **
    ** Choose which instances we might be able to use
    */  
   
    private Map<String, InstanceAndCeiling> findInstanceMatches(LinkCandidate lc, List<GenomeInstance> ancestry) {    
      HashMap<String, InstanceAndCeiling> retval = new HashMap<String, InstanceAndCeiling>();
      Set<String> kidInstances = null;
      int aNum = ancestry.size();
      for (int i = 0; i < aNum; i++) {
        GenomeInstance gi = ancestry.get(i);
        Set<String> existingInstances = newLinkInstanceExists(lc, gi);
        if (kidInstances != null) {
          HashSet<String> available = new HashSet<String>(existingInstances);
          available.removeAll(kidInstances);
          Iterator<String> ait = available.iterator();
          while (ait.hasNext()) {
            String linkID = ait.next();
            InstanceAndCeiling iAndC = new InstanceAndCeiling(linkID, aNum - i - 1);
            retval.put(linkID, iAndC);
          }
        }
        kidInstances = existingInstances;
      }
      return ((retval.isEmpty()) ? null : retval);  
    }
    
    /***************************************************************************
    **
    ** Choose which instance to use
    */  
   
    private InstanceAndCeiling findInstanceMatch(String linkID, List<GenomeInstance> ancestry) {    
      int aNum = ancestry.size();
      for (int i = 0; i < aNum; i++) {
        GenomeInstance gi = ancestry.get(i);
        if (gi.getLinkage(linkID) != null) {
          // Found at the root instance? (aNum - 1) - (aNum - 1) == 0
          InstanceAndCeiling iAndC = new InstanceAndCeiling(linkID, aNum - i - 1);
          return (iAndC);
        }
      }
      throw new IllegalArgumentException();
    }
    
    /***************************************************************************
    **
    ** Add a link
    */  
   
    private LinkageInstance addNewLinkToRootInstanceFinish(StaticDataAccessContext rcxI,
                                                           LinkCandidate lc, UndoSupport support) {
       
      if (rcxI.getCurrentGenomeAsInstance().getVfgParent() != null) {
        throw new IllegalArgumentException();
      }
      
      //
      // First question:  Does the link we are adding to the instance have a corresponding link
      // in the root model?  If no, we need to add it:
      //
      
      DBLinkage rootLink;
      String src = lc.srcID;
      String targ = lc.target.getObjectID();
      boolean newInRoot = false;
  
      if (lc.existingLinkID == null) {
        String rootSrc = GenomeItemInstance.getBaseID(src);
        String rootTarg = GenomeItemInstance.getBaseID(targ);
        OldPadMapper oldPads = null;
        PadCalculatorToo padCalc = new PadCalculatorToo();
        PadConstraints pc = padCalc.generatePadConstraints(rootSrc, rootTarg, oldPads, null, new HashMap<String, String>());
        String rootName = ((lc.label != null) && (!lc.label.trim().equals(""))) ? lc.label : null;
        rootLink = (DBLinkage)AddCommands.autoAddOldOrNewLinkToRoot(rcxR_, rootName, rootSrc, rootTarg, lc.sign, null, 
                                                                    support, true, null, pc, false, Linkage.LEVEL_NONE, null);
        lc.existingLinkID = rootLink.getID();
        LayoutLinkSupport.autoAddCrudeLinkProperties(rcxR_, lc.existingLinkID, support, null);
        newInRoot = true;
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), rcxR_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE)); 
      } else {
        rootLink = (DBLinkage)rcxR_.getCurrentGenome().getLinkage(lc.existingLinkID);
      }
      
      //
      // Get the linkage instance built and added:
      //    
      boolean startFromNode =  lc.srcID.equals(lc.source.getObjectID());
      Linkage chkLink = (!startFromNode) ? rcxI.getCurrentGenomeAsInstance().getLinkage(lc.source.getObjectID()) : null;
         
      int srcPad = (startFromNode) ? ((Intersection.PadVal)lc.source.getSubID()).padNum : chkLink.getLaunchPad();
      int trgPad = ((Intersection.PadVal)lc.target.getSubID()).padNum;
      int srcInstance = GenomeItemInstance.getInstanceID(src);
      int trgInstance = GenomeItemInstance.getInstanceID(targ);
      int instanceCount = rcxI.getCurrentGenomeAsInstance().getNextLinkInstanceNumber(lc.existingLinkID);   
      LinkageInstance newLink = new LinkageInstance(rcxI, rootLink, instanceCount, srcInstance, trgInstance);
      lc.builtID = newLink.getID();
      newLink.setLaunchPad(srcPad);
      newLink.setLandingPad(trgPad);
      String newLinkID = newLink.getID();
   
      GenomeChange gc = rcxI.getCurrentGenomeAsInstance().addLinkage(newLink);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(rcxI, gc);
        support.addEdit(gcc);
      }     
      
      //
      // Have to either build a fresh link properties or modify an existing one:
      //
     
      if (!startFromNode) {
        LinkSegmentID[] segIDs = lc.source.segmentIDsFromIntersect();
   
        BusProperties nlp = convertCandidateToBusForMerge(rcxI, lc, newLinkID);
        Layout.PropChange lpc = rcxI.getCurrentLayout().mergeNewLinkToTreeAtSegment(rcxI, chkLink.getID(), nlp, segIDs[0]);
        PropChangeCmd pcc = new PropChangeCmd(rcxI, new Layout.PropChange[] {lpc});
        support.addEdit(pcc);
        //
        // Note we do not "Propagate"; but install into layout occurs above
        // Note that color selection for the source depends on colors in the
        // root genome.  So although we may not spend much time doing layout
        // at the root, color does need to be kept consistent.
        //
      } else {
        BusProperties bp = convertCandidateToBus(rcxI, lc, newLinkID, src);
        if (newInRoot) {
          changeRootColor(bp, rcxR_, newLinkID, support);
        }
        if (lc.slashFlipSrc) {
          int oldOrient = flipASlashNode(rcxI, src, support, null);
          String rootSrc = GenomeItemInstance.getBaseID(src);
          flipASlashNode(rcxR_, rootSrc, support, Integer.valueOf(oldOrient));
          
        }
        PropagateSupport.propagateLinkProperties(rcxI, bp, newLinkID, support);   
      }
      
      if (lc.slashFlipTrg) {
        int oldOrient = flipASlashNode(rcxI, targ, support, null);
        String rootTrg = GenomeItemInstance.getBaseID(targ);
        flipASlashNode(rcxR_, rootTrg, support, Integer.valueOf(oldOrient));        
      }

      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), rcxI.getCurrentGenomeAsInstance().getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
  
      return (newLink); 
    }
     
    
   /***************************************************************************
   **
   ** Change root node and link color to match new link
   */  
   
   private void changeRootColor(BusProperties bp, StaticDataAccessContext rcxR, String linkID, UndoSupport support) {
  
      String rootLinkID = GenomeItemInstance.getBaseID(linkID);
      BusProperties rootProps = rcxR.getCurrentLayout().getLinkProperties(rootLinkID);
      String colorKey = bp.getColorName();
      if (!colorKey.equals(rootProps.getColorName())) {
        BusProperties changedProps = rootProps.clone();
        changedProps.setColor(colorKey);
        Layout.PropChange[] lpc = new Layout.PropChange[1];
        lpc[0] = rcxR.getCurrentLayout().replaceLinkProperties(rootProps, changedProps);        
        if (lpc[0] != null) {
          PropChangeCmd mov = new PropChangeCmd(rcxR, lpc);
          support.addEdit(mov);    
          LayoutChangeEvent ev = new LayoutChangeEvent(rcxR.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
          support.addEvent(ev);
          PropagateSupport.changeNodeColor(rcxR, changedProps, rootLinkID, support);
        }
      }
      return;
    }    
       

     
    /***************************************************************************
    **
    ** Find out if a new link candidate actually has valid root genome representations
    */  
   
    private Set<String> newLinkInstanceHasParent(Genome rootGenome, LinkCandidate lc, GenomeInstance gi) {
  
      //
      // Get a set of candidates from the root:
      //
  
      String srcID = lc.srcID;
      if (srcID == null) {
        throw new IllegalArgumentException();
      }    
      String targ = lc.target.getObjectID();
      String rootSrc = GenomeItemInstance.getBaseID(srcID);
      String rootTarg = GenomeItemInstance.getBaseID(targ); 
      Node targNode = rootGenome.getNode(rootTarg);
      Gene targGeneForMod = null;
      if (targNode.getNodeType() == Gene.GENE) {
        if (((Gene)targNode).getNumRegions() > 0) {     
         targGeneForMod = (Gene)targNode;   
        }
      }
         
      HashSet<String> rootCand = new HashSet<String>();
      Iterator<Linkage> lit = rootGenome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        if (!link.getSource().equals(rootSrc)) {
          continue;
        }
        if (!link.getTarget().equals(rootTarg)) {
          continue;
        }
        if (link.getSign() != lc.sign) {
          continue;
        }
        int targPad = link.getLandingPad();
        DBGeneRegion regR = (targGeneForMod != null) ? targGeneForMod.getRegionForPad(targPad) : null;
        if ((regR != null) && regR.isHolder()) {
          regR = null;
        }
        
        if (lc.modTarg != null) {
          if ((regR == null) || !lc.modTarg.equals(regR.getKey())) {
            continue;
          }
        } else if (regR != null) {
          continue;
        }      
        rootCand.add(link.getID());
      }
      
      //
      // If that set is empty, then there is no candidate!
      //
      
      if (rootCand.isEmpty()) {
        return (null);
      }
      
      //
      // We have at least one candidate.  For each candidate, get the set of
      // link instances present in the current model.  If one link instance 
      // matches our source/targ instances, it is no longer a candidate.  If
      // we have any candidates left, we send those back
      //
  
      HashSet<String> rcOrig = new HashSet<String>(rootCand);
      Iterator<String> rcit = rcOrig.iterator();
      while (rcit.hasNext()) {
        String rcid = rcit.next();
        lit = gi.getLinkageIterator();
        while (lit.hasNext()) {
          Linkage link = lit.next();
          String iLinkID = link.getID();
          String iLinkIDBase = GenomeItemInstance.getBaseID(iLinkID);
          if (iLinkIDBase.equals(rcid)) {
            if (srcID.equals(link.getSource()) && targ.equals(link.getTarget())) {
              rootCand.remove(rcid);
              break;
            }
          }
        }
      }
      
      if (rootCand.size() == 0) {
        return (null);
      }
   
      return (rootCand); 
    }  
    
    /***************************************************************************
    **
    ** Find out if a new link candidate already exists in the given instance.  Return
    ** a set of matching instance IDs.
    */  
   
    private Set<String> newLinkInstanceExists(LinkCandidate lc, GenomeInstance gi) {
  
      HashSet<String> retval = new HashSet<String>();
      
      //
      // Get a set of candidates from the root:
      //
  
      String srcID = lc.srcID;
      if (srcID == null) {
        throw new IllegalArgumentException();
      }    
      String targ = lc.target.getObjectID();
      
      Iterator<Linkage> lit = gi.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        if (!link.getSource().equals(srcID)) {
          continue;
        }
        if (!link.getTarget().equals(targ)) {
          continue;
        }
        if (link.getSign() != lc.sign) {
          continue;
        }
        int targPad = link.getLandingPad();
        Node targNode = gi.getNode(link.getTarget());
        Gene targGeneForMod = null;
        if (targNode.getNodeType() == Gene.GENE) {
          if (((Gene)targNode).getNumRegions() > 0) {     
           targGeneForMod = (Gene)targNode;   
          }
        }
        DBGeneRegion regR = (targGeneForMod != null) ? targGeneForMod.getRegionForPad(targPad) : null;
        if ((regR != null) && regR.isHolder()) {
          regR = null;
        } 
        if (lc.modTarg != null) {
          if ((regR == null) || !lc.modTarg.equals(regR.getKey())) {
            continue;
          }
        } else if (regR != null) {
          continue;
        }
        retval.add(link.getID());
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Start to add a new net module link
    */  
   
    private NetModuleLinkCandidate beginDrawNetworkModuleLink(String overlayKey) {
            
      Vector<ChoiceContent> signs = NetModuleLinkage.getLinkSigns(dacx_);
      int numSigns = signs.size();
      Object[] forDisplay = new Object[numSigns];
      for (int i = 0; i < numSigns; i++) {
        forDisplay[i] = signs.get(i);
      }
      ResourceManager rMan = dacx_.getRMan();       
      Object result = JOptionPane.showInputDialog(uics_.getTopFrame(), rMan.getString("drawModLink.chooseLinkSign"),
                                                  rMan.getString("drawModLink.dialogTitle"),
                                                  JOptionPane.QUESTION_MESSAGE, 
                                                  null, forDisplay, forDisplay[NetModuleLinkage.DEFAULT_SIGN_INDEX]);
      if (result == null) {
        return (null);
      }
  
      int linkSign = ((ChoiceContent)result).val;
      return (new NetModuleLinkCandidate(linkSign, overlayKey, dacx_));
    }
    
    /***************************************************************************
    **
    ** Finish adding extra regions to net module
    */  
   
    private boolean finishDrawNetworkModuleLink(NetModuleLinkCandidate nmlc) {
  
      String linkID = rcxR_.getNextKey();
      
      //
      // Add it to an existing tree:
      //
   
      if (!nmlc.srcIsModule) {
        return (addToNetModuleLinkTree(linkID, nmlc));
      }
    
      //NetworkOverlay nov = NetOverlayController.getOverlayOwner(genomeKey).getNetworkOverlay(nmlc.ovrKey);
      //NetModule srcMod = nov.getModule(nmlc.srcMod);
      // NetModule trgMod = nov.getModule(nmlc.trgMod);
   
      UndoSupport support = uFac_.provideUndoSupport("undo.newNetworkModuleLink", rcxR_);
      NetModuleLinkage nml = new NetModuleLinkage(nmlc.rcxC, linkID, nmlc.srcMod, nmlc.trgMod, nmlc.sign);
      NetworkOverlayChange noc = nmlc.rcxC.getGenomeSource().getOverlayOwnerFromGenomeKey(nmlc.rcxC.getCurrentGenomeID()).addNetworkModuleLinkage(nmlc.ovrKey, nml); 
      if (noc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(nmlc.rcxC, noc);
        support.addEdit(gcc);
      }
      NetModuleLinkageProperties nmlp = convertModuleLinkCandidate(nmlc, linkID, rcxR_);
   
      // Match color to source module:
      NetModuleProperties nmp = nmlc.rcxC.getCurrentLayout().getNetOverlayProperties(nmlc.ovrKey).getNetModuleProperties(nmlc.srcMod);
      nmlp.setColor(nmp.getColorTag());
      
      
      Layout.PropChange pc = nmlc.rcxC.getCurrentLayout().setNetModuleLinkageProperties(nmlp.getID(), nmlp, nmlc.ovrKey, nmlc.rcxC);
      if (pc != null) {
        support.addEdit(new PropChangeCmd(nmlc.rcxC, pc));
        Layout.PropChange pc2 = nmlc.rcxC.getCurrentLayout().tieNetModuleLinkToProperties(linkID, nmlp.getID(), nmlc.ovrKey);
        if (pc2 != null) {
          support.addEdit(new PropChangeCmd(nmlc.rcxC, pc2));
        }
      }
  
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), nmlc.rcxC.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));     
      support.addEvent(new LayoutChangeEvent(nmlc.rcxC.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
      support.finish();
      return (true);
    }    
    
    /***************************************************************************
    **
    ** Convert a link candidate to a NetModuleLinkageProperties
    */  
   
    private NetModuleLinkageProperties convertModuleLinkCandidate(NetModuleLinkCandidate lc, String linkID, StaticDataAccessContext rcx) {
     
      // Build the segment list:
     
      List<LinkSegment> segments = convertCandidateListToSegments(lc.points, null, true);
      
      // Build the properties:
      
      NetModuleFree.IntersectionExtraInfo srcEI = (NetModuleFree.IntersectionExtraInfo)lc.source.getSubID();
      Vector2D toStartSide = srcEI.padNorm.scaled(-1.0);
       
      NetModuleFree.IntersectionExtraInfo trgEI = (NetModuleFree.IntersectionExtraInfo)lc.target.getSubID();
      Vector2D toEndSide = trgEI.padNorm.scaled(-1.0);
      
      // We want trees to have globally unique IDs:
      String newKey = rcx.getNextKey();
      NetModuleLinkageProperties nmlp = new NetModuleLinkageProperties(newKey, lc.ovrKey, lc.srcMod, linkID, 
                                                                       lc.points.get(0), 
                                                                       lc.points.get(lc.points.size() - 1), 
                                                                       toStartSide, toEndSide);
      Iterator<LinkSegment> sit = segments.iterator();
      while (sit.hasNext()) {
        LinkSegment ls = sit.next();
        nmlp.addSegmentInSeries(ls);
      }
      return (nmlp);
    } 
    
    /***************************************************************************
    **
    ** Convert a link candidate point list to a list of segments
    */  
   
    private List<LinkSegment> convertCandidateListToSegments(List<Point> points, Point2D textLoc, boolean haveTargetIntersection) {
    
      // Ditch some points
      int numToDitch = (haveTargetIntersection) ? 2 : 1;
      int numSeg = points.size() - numToDitch;
      ArrayList<LinkSegment> segments = new ArrayList<LinkSegment>();
      float tx = 0.0F;
      float ty = 0.0F;
      if (numSeg == 0) {
        Point pt = points.get(0);
        tx = pt.x;
        ty = pt.y;      
      } else if (numSeg == 1) {
        Point pt = points.get(1);
        Point2D newPt = new Point2D.Float(pt.x, pt.y);
        LinkSegment ls = new LinkSegment(null, null, newPt, null);
        segments.add(ls);
        tx = pt.x;
        ty = pt.y;      
      } else {
        Point2D lastPt = null;
        Iterator<Point> pit = points.iterator();
        int halfSeg = numSeg / 2;
        int segCount = 0;
        pit.next();  //ditch first point
        while (pit.hasNext()) {
          Point pt = pit.next();
          // Ditch last point if we have target:
          if (haveTargetIntersection && !pit.hasNext()) {
            break;
          }
          Point2D newPt = new Point2D.Float(pt.x, pt.y); 
          if (lastPt != null) {
            LinkSegment ls = new LinkSegment(null, null, lastPt, newPt);
            segments.add(ls);
          }
          lastPt = newPt;
          if (segCount == halfSeg) {
            tx = pt.x;
            ty = pt.y;         
          }
          segCount++;
        }
      }
      
      if (textLoc != null) {
        textLoc.setLocation(tx, ty);     
      }
      
      return (segments);
    }  
      
    /***************************************************************************
    **
    ** Add a segment to an existing NetModuleLinkageProperties
    */  
   
    private boolean addToNetModuleLinkTree(String linkID, NetModuleLinkCandidate nmlc) {
      
      //
      // We have intersected a link segment as the source of a new link.
      // We find the source of the link and create a new DBLinkage for
      // it.  Then we take the existing LinkProperties and add
      // a new segment on.
      //
  
      NetOverlayOwner noo = nmlc.rcxC.getGenomeSource().getOverlayOwnerFromGenomeKey(nmlc.rcxC.getCurrentGenomeID());
      LinkSegmentID[] segIDs = nmlc.source.segmentIDsFromIntersect();
      
      //
      // Undo/Redo support
      //
      
      UndoSupport support = uFac_.provideUndoSupport("undo.newNetworkModuleLink", nmlc.rcxC);   
      NetModuleLinkage nml = new NetModuleLinkage(nmlc.rcxC, linkID, nmlc.srcMod, nmlc.trgMod, nmlc.sign);
      NetworkOverlayChange noc = noo.addNetworkModuleLinkage(nmlc.ovrKey, nml); 
      if (noc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(nmlc.rcxC, noc);
        support.addEdit(gcc);
      }
    
      //
      // Create a new LinkProperties, glue it into the existing one:
      //
      
      NetModuleLinkageProperties nmlp = convertNetModLinkCandidateToPropsForMerge(nmlc, linkID);        
      String treeID = nmlc.source.getObjectID();
      Layout.PropChange pc = nmlc.rcxC.getCurrentLayout().mergeNewModuleLinkToTreeAtSegment(nmlp, treeID, linkID, nmlc.ovrKey, segIDs[0], nmlc.rcxC);   
      if (pc != null) {
        support.addEdit(new PropChangeCmd(nmlc.rcxC, pc));   
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), nmlc.rcxC.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        support.addEvent(new LayoutChangeEvent(nmlc.rcxC.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
        support.finish();
        return (true);
      }
      return (false);
    }  
  
    /***************************************************************************
    **
    ** Convert a link candidate to a NetModuleLinkProperties for merging with existing
    */  
   
    private NetModuleLinkageProperties convertNetModLinkCandidateToPropsForMerge(NetModuleLinkCandidate nmlc, String linkID) {  
      //
      // Create a new LinkProperties; add all the segments
      //
        
      int numSeg = nmlc.points.size() - 2;  // Net mod links have BOTH start AND end points in the list!
      ArrayList<LinkSegment> segments = new ArrayList<LinkSegment>();
      if (numSeg == 0) {
        Point pt = nmlc.points.get(0);
        Point2D newPt = new Point2D.Float(pt.x, pt.y);
        LinkSegment ls = new LinkSegment(null, null, newPt, null);
        segments.add(ls);            
      } else {
        Point2D lastPt = null;
        Iterator<Point> pit = nmlc.points.iterator();
        while (pit.hasNext()) {
          Point pt = pit.next();
          if (!pit.hasNext()) { // Ditch the last point...
            break;
          }
          Point2D newPt = new Point2D.Float(pt.x, pt.y); 
          if (lastPt != null) {
            LinkSegment ls = new LinkSegment(null, null, lastPt, newPt);
            segments.add(ls);
          }
          lastPt = newPt;
        }
      }
  
      Point2D startPt = new Point2D.Double(0.0, 0.0);  // Doesn't matter: will be ditched anyway
      Vector2D toStartSide = new Vector2D(0.0, 0.0);  // Same deal
      Point targPt = nmlc.points.get(nmlc.points.size() - 1);
      Point2D endPt = new Point2D.Float(targPt.x, targPt.y);
      NetModuleFree.IntersectionExtraInfo trgEI = (NetModuleFree.IntersectionExtraInfo)nmlc.target.getSubID();
      Vector2D toEndSide = trgEI.padNorm.scaled(-1.0);
   
      // This is a temporary holder, and does NOT get a real ID:
      NetModuleLinkageProperties nNmlp = 
        new NetModuleLinkageProperties("fakeID", nmlc.ovrKey, nmlc.srcMod, linkID, startPt, endPt, toStartSide, toEndSide);
      
      Iterator<LinkSegment> sit = segments.iterator();
      while (sit.hasNext()) {
        LinkSegment ls = sit.next();
        // Handles degenerate creation automatically
        nNmlp.addSegmentInSeries(ls);
      }
      
      return (nNmlp);
    }    
  }

  /***************************************************************************
  **
  ** Represents a link that is being built
  */
  
  public static class LinkCandidate {

    public static final int EMPTY  = 0;
    public static final int ADDING = 1;
    public static final int DONE   = 2;    

    public ArrayList<Point> points;
    public int buildState;
    public Intersection source;
    public Intersection target;
    public int sign;
    public String label;
    public String srcID;
    public String builtID;
    public boolean slashFlipSrc;
    public boolean slashFlipTrg;
    public DBGeneRegion.DBRegKey modTarg;
    
    public Layout targetLayout;
    public String existingLinkID;
    public Map<String, Set<String>> okSrcTrgMap;
    
   
    public LinkCandidate(String label, int sign, Layout targetLayout, String existingLinkID, Map<String, Set<String>> okSrcTrgMap) {
      this.label = label;
      this.sign = sign;
      this.targetLayout = targetLayout;
      this.existingLinkID = existingLinkID;
      this.okSrcTrgMap = okSrcTrgMap;      
      buildState = EMPTY;
      points = new ArrayList<Point>();
    }
  }
  
  /***************************************************************************
  **
  ** Represents a network module link that is being built
  */
  
  public static class NetModuleLinkCandidate {

    public static final int EMPTY  = 0;
    public static final int ADDING = 1;
    public static final int DONE   = 2;    

    public ArrayList<Point> points;
    public Intersection source;
    public Intersection target;
    public boolean srcIsModule;
    public int buildState;
    public int sign;
    public String trgMod;
    public String srcMod;
    public String ovrKey;
    public StaticDataAccessContext rcxC;
    
    public NetModuleLinkCandidate(int sign, String ovrKey, StaticDataAccessContext rcxC) {
      buildState = EMPTY;
      this.rcxC = rcxC;
      this.sign = sign;
      points = new ArrayList<Point>();
      this.ovrKey = ovrKey;
    }
  }  
  
  
  /***************************************************************************
  **
  ** Used pass back instance match info
  */
  
  public static class InstanceAndCeiling {    
    public String instanceToUse; 
    public int propCeiling;
    
    InstanceAndCeiling(String instanceToUse, int propCeiling) {
      this.instanceToUse = instanceToUse;
      this.propCeiling = propCeiling;  // propagation ceiling...
    }    
  }   
}
