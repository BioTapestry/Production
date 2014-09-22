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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveLinkage;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.Layout.PropChange;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.dialogs.ChangeSourceOrTargetNodeReviewDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle changing target or source nodes
*/

public class ChangeNode extends AbstractControlFlow {

  public enum NodeChange {AMBIGUOUS, UNAMBIGUOUS_SIMPLE, UNAMBIGUOUS_REGION_CHANGE, UNAMBIGUOUS_COLLAPSE};
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean isForTarget_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ChangeNode(BTState appState, boolean isForTarget) {
    super(appState);
    isForTarget_ = isForTarget;
    name =  (isForTarget) ? "linkPopup.TargNodeChange" : "linkPopup.SrcNodeChange";
    desc = (isForTarget) ? "linkPopup.TargNodeChange" : "linkPopup.SrcNodeChange";
    mnem =  (isForTarget) ? "linkPopup.TargNodeChangMnem" : "linkPopup.SrcNodeChangeMnem";
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
    Genome genome = rcx.getGenome();
    if (!(genome instanceof DBGenome)) {
      return (false);
    }
    if (!isForTarget_) {
      return (true);
    }
    Layout layout = rcx.getLayout();
    
    LinkSegmentID[] ids = inter.segmentIDsFromIntersect();
    if (ids == null) {
      return (false);
    }
    if (ids.length != 1) {
      return (false);
    }
    String oid = inter.getObjectID();
    if (layout.segmentSynonymousWithTargetDrop(oid, ids[0]) == null) {
      return (false);
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, isForTarget_, dacx);
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
        } else if (ans.getNextStep().equals("stepBiWarning")) {   
          next = ans.stepBiWarning();  
        } else if (ans.getNextStep().equals("stepChangeLinkSourceNode")) {   
          next = ans.stepChangeLinkSourceNode();
        } else if (ans.getNextStep().equals("stepChangeLinkTargetNode")) {   
          next = ans.stepChangeLinkTargetNode(); 
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
    ans.rcxR_.pixDiam = pixDiam;
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = (ans.isForTarget) ? "stepChangeLinkTargetNode" : "stepChangeLinkSourceNode"; 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State:
  */
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private Intersection intersect;
    private DataAccessContext rcxR_;
    private boolean isForTarget; 
    private int x;
    private int y;
    private String nextStep_;   
    private BTState appState_;

    /***************************************************************************
    **
    ** Constructor
    */
     
    private StepState(BTState appState, boolean isForTarget, DataAccessContext dacx) {
      appState_ = appState;
      rcxR_ = dacx; // Gotta be for root genome only!
      if (!rcxR_.genomeIsRootGenome()) {
        throw new IllegalArgumentException();
      }
      nextStep_ = "stepBiWarning";
      this.isForTarget = isForTarget;
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
      return (true);
    }
            
    public boolean showBubbles() {
      return (true);
    }
    
    public boolean mustBeDynamic() {
      return (false);
    }
 
    public boolean noRootModel() {
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
    ** Warn of build instructions and init some values
    */
       
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd daipc;
      if (appState_.getDB().haveBuildInstructions()) {
        String message = rcxR_.rMan.getString("instructWarning.message");
        String title = rcxR_.rMan.getString("instructWarning.title");
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
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (isForTarget) ? PanelCommands.Mode.CHANGE_TARGET_NODE : PanelCommands.Mode.CHANGE_SOURCE_NODE;
      return (retval);
    }
  
    /***************************************************************************
    **
    ** Relocates all the links through the given segment to another source node 
    */  
  
    private DialogAndInProcessCmd stepChangeLinkSourceNode() {

      //
      // Go and find if we intersect anything. Nodes gotta have priority!
      //

      List<Intersection.AugmentedIntersection> augs = 
        appState_.getGenomePresentation().intersectItem(x, y, rcxR_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxR_)).selectionRanker(augs); 
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

      //
      // Things are getting interesting.  Let's resolve which links we are trying to
      // relocate:
      //

      String lid = intersect.getObjectID();
      Linkage currentLinkage = rcxR_.getGenome().getLinkage(lid);
      String currentSource = currentLinkage.getSource();

      LinkSegmentID[] linkIDs = intersect.segmentIDsFromIntersect();
      BusProperties bp = rcxR_.getLayout().getLinkProperties(lid);
      Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);

      //
      // We can intersect either a linkage or a node:
      //

      int padNum;
      String sourceID;
      LinkSegmentID segID = null;
      String id = inter.getObjectID();
      Linkage link = rcxR_.getGenome().getLinkage(id);
      Node node = rcxR_.getGenome().getNode(id);
      // If we have a link, we had better make sure we have an endpoint, and we had
      // better make sure the link source is different from us:
      if (link != null) {
        LinkSegmentID[] segIDs = inter.segmentIDsFromIntersect();
        segID = segIDs[0];
        // We reject unless the user selects a segment end point
        if (!segID.isTaggedWithEndpoint()) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        sourceID = link.getSource();
        if (sourceID.equals(currentSource)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        padNum = link.getLaunchPad();
      } else if (node != null) {
        if (id.equals(currentSource)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        sourceID = id;
        if (!(sub instanceof Intersection.PadVal)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        } 
        INodeRenderer render = rcxR_.getLayout().getNodeProperties(sourceID).getRenderer();
        List<Intersection.PadVal> pads = inter.getPadCand();
        int numCand = pads.size();
        Intersection.PadVal winner = null;
        if (!(rcxR_.getGenome() instanceof DBGenome)) { // SourcePadIsClear needs special treatment in subset GI's
          throw new IllegalArgumentException();
        }
        for (int i = 0; i < numCand; i++) {
          Intersection.PadVal pad = pads.get(i);
          if (!pad.okStart) {
            continue;
          }
          if (!LinkSupport.sourcePadIsClear(appState_, rcxR_.getGenome(), sourceID, pad.padNum, render.sharedPadNamespaces())) {
            continue;
          }
          if ((winner == null) || (winner.distance > pad.distance)) {
            winner = pad;
          }
        }    
        if (winner == null) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        padNum = winner.padNum;
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      if (changeLinkSourceNode(throughSeg, linkIDs[0], inter, padNum, segID, sourceID, rcxR_, appState_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT_DELAYED, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.CANCELLED, this));
      }
    }      
  
    /***************************************************************************
    **
    ** Relocates the single link through the given segment to another target node 
    */   

    private DialogAndInProcessCmd stepChangeLinkTargetNode() {

      //
      // Go and find if we intersect anything.
      //

      List<Intersection.AugmentedIntersection> augs = 
        appState_.getGenomePresentation().intersectItem(x, y, rcxR_, true, false);
      Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxR_)).selectionRanker(augs);
      Intersection inter = ((ai == null) || (ai.intersect == null)) ? null : ai.intersect;

      if (inter == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      Object sub = inter.getSubID();
      if (sub == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      //
      // Things are getting interesting.  Let's resolve which link we are trying to
      // relocate:
      // 
      String lid = intersect.getObjectID();   
      LinkSegmentID[] linkIDs = intersect.segmentIDsFromIntersect();
      BusProperties bp = rcxR_.getLayout().getLinkProperties(lid);
      Set<String> throughSeg = bp.resolveLinkagesThroughSegment(linkIDs[0]);

      //
      // If we have more than one link, we reject, though this should have been
      // caught on the initial mode setting!
      //

      if (throughSeg.size() != 1) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      lid = throughSeg.iterator().next();
      Linkage currentLinkage = rcxR_.getGenome().getLinkage(lid);
      String currentTarget = currentLinkage.getTarget();

      //
      // We can intersect only a node:
      //

      int padNum;

      String id = inter.getObjectID();
      Node node = rcxR_.getGenome().getNode(id);
      if (node == null) {
         return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }            
      if (id.equals(currentTarget)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }

      String targetID = id;
      if (!(sub instanceof Intersection.PadVal)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      } 
      INodeRenderer render = rcxR_.getLayout().getNodeProperties(targetID).getRenderer();
      List<Intersection.PadVal> pads = inter.getPadCand();
      int numCand = pads.size();
      Intersection.PadVal winner = null;
      if (!rcxR_.genomeIsRootGenome()) { // SourcePadIsClear needs special treatment in subset GI's
        throw new IllegalArgumentException();
      }
      for (int i = 0; i < numCand; i++) {
        Intersection.PadVal pad = pads.get(i);
        if (!pad.okEnd) {
          continue;
        }
        if (!LinkSupport.targPadIsClear(rcxR_.getGenome(), targetID, pad.padNum, render.sharedPadNamespaces())) {
          continue;
        }
        if ((winner == null) || (winner.distance > pad.distance)) {
          winner = pad;
        }
      }    
      if (winner == null) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
      }
      padNum = winner.padNum;
      
      if (changeLinkTargetNode(appState_, currentLinkage, targetID, padNum, rcxR_)) {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
      } else {
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.CANCELLED, this));
      }
    }
    
    /***************************************************************************
    **
    ** Change the link target node
    */  
   
    private boolean changeLinkTargetNode(BTState appState, Linkage link, String targetID, int padNum, DataAccessContext rcxR) {
    
      HashSet<String> linkSet = new HashSet<String>();
      linkSet.add(link.getID());
      NodeChange ambiguity = linkNodeChangeIsAmbiguous(appState, linkSet, targetID, false);
      Map<String, Map<String, String>> quickKillMap = null;
      
      ResourceManager rMan = appState.getRMan(); 
      switch (ambiguity) {
        case AMBIGUOUS:
          quickKillMap = new HashMap<String, Map<String, String>>();
          QuickKillResult isQuick = ambiguousLinkNodeChangeIsQuickKill(appState, linkSet, targetID, quickKillMap, false);
          
          if (isQuick.mustDeleteLinks) {
            int resultMDL = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("trgChange.GottaDeleteMsg")),
                                                          rMan.getString("trgChange.GottaDeleteTitle"),
                                                          JOptionPane.YES_NO_OPTION);
            if (resultMDL == 1) {
              return (false);
            }
          }
          
          boolean showReview = false;
          if (isQuick.isQuickKill) {
            int result = JOptionPane.showOptionDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("trgChange.doQuickKill")),
                                                                  rMan.getString("trgChange.messageTitle"),
                                                                  JOptionPane.DEFAULT_OPTION, 
                                                                  JOptionPane.QUESTION_MESSAGE, 
                                                                  null, new Object[] {
                                                                    rMan.getString("trgChange.review"),
                                                                    rMan.getString("trgChange.continue"),
                                                                    rMan.getString("trgChange.cancel"),
                                                                  }, rMan.getString("trgChange.continue"));
  
            if (result == 0) {
              showReview = true;
            } else if (result == 2) {
              return (false);
            }       
          }
          
          if (!isQuick.isQuickKill || showReview) {
            ChangeSourceOrTargetNodeReviewDialog csnrd = new ChangeSourceOrTargetNodeReviewDialog(appState, rcxR_, targetID, linkSet, quickKillMap, false);
            csnrd.setVisible(true);
            if (!csnrd.haveResult()) {
              return (false); 
            }
            quickKillMap = csnrd.getInstanceChoices();
          }
          break;
        case UNAMBIGUOUS_COLLAPSE:
          int resultUC = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("trgChange.unambiguousCollapseMsg")),
                                                       rMan.getString("trgChange.unambiguousCollapseTitle"),
                                                       JOptionPane.YES_NO_OPTION);
          if (resultUC == 1) {
            return (false);
          }
          break;
        case UNAMBIGUOUS_REGION_CHANGE:
          int resultURC = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("trgChange.unambiguousRegionChgMsg")),
                                                        rMan.getString("trgChange.unambiguousRegionChgTitle"),
                                                        JOptionPane.YES_NO_OPTION);
          if (resultURC == 1) {
            return (false);
          }
          break;
        case UNAMBIGUOUS_SIMPLE:
          break;
        default:
          throw new IllegalStateException();
      }
      
      UndoSupport support = new UndoSupport(appState, "undo.changeLinkTargetNode");
      changeLinkTargetNodeComplete(appState, link, targetID, rcxR, padNum, quickKillMap, ambiguity, support);   
      appState.getDB().clearAllDynamicProxyCaches();
      support.finish();
      return (true); 
    }
    
    /***************************************************************************
    **
    ** Change the link target node
    */  
   
    private void changeLinkTargetNodeComplete(BTState appState, Linkage link, String targetID,
                                              DataAccessContext rcxR,
                                              int padNum, Map<String, Map<String, String>> quickKillMap, 
                                              NodeChange ambiguity,
                                              UndoSupport support) {
 
      HashSet<String> changedModels = new HashSet<String>();
      HashMap<String, Set<String>> totalChangeMap = new HashMap<String, Set<String>>();
  
      String linkID = link.getID();
      
      //
      // Have to change target node of the linkage.
      //
      
      //
      // Root model first:
      //
        
      GenomeChange undo = rcxR.getGenome().changeLinkageTargetNode(link, targetID, padNum);
      support.addEdit(new GenomeChangeCmd(appState, rcxR, undo));
      changedModels.add(rcxR.getGenomeID());
         
      //
      // Do the top-level instances first:
      //
  
      HashMap<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
      Iterator<GenomeInstance> giit = rcxR.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }
        DataAccessContext rcxI = new DataAccessContext(rcxR, gi.getID());
        Set<String> nodeInst = gi.getNodeInstances(targetID);
        Map<String, String> quickKillForModel = (quickKillMap == null) ? null : quickKillMap.get(rcxI.getGenomeID());

          
        //
        // Track for use in VFN kids:
        //
        HashSet<String> changeSet = new HashSet<String>();
        changeMap.put(rcxI.getGenomeID(), changeSet);
        Set<String> totalChangeSet = totalChangeMap.get(rcxI.getGenomeID());
        if (totalChangeSet == null) {
          totalChangeSet = new HashSet<String>();
          totalChangeMap.put(rcxI.getGenomeID(), totalChangeSet);
        }
          
        //
        // Crank through each instance of the switched link:
        //
  
        Set<String> linkSet = gi.returnLinkInstanceIDsForBacking(linkID);
        Iterator<String> lit = linkSet.iterator();
        while (lit.hasNext()) {
          String clid = lit.next();
          Linkage childLink = gi.getLinkage(clid);
          //
          // Figure out the new target node:
          //
          String newNodeID;
          switch (ambiguity) {
            case AMBIGUOUS: 
              newNodeID = quickKillForModel.get(clid);
              break;
            case UNAMBIGUOUS_COLLAPSE:
            case UNAMBIGUOUS_REGION_CHANGE:
            case UNAMBIGUOUS_SIMPLE:        
              if (nodeInst.size() != 1) {
                throw new IllegalStateException();
              }
              newNodeID = nodeInst.iterator().next();
              break;
            default:
              throw new IllegalStateException();
          }              
          //
          // No place to go, we need to kill off the link:
          //
          if (newNodeID == null) {
            HashSet<String> killSet = new HashSet<String>();
            killSet.add(clid);
            // THIS HANDLES DELETING LAYOUT INFO!
            RemoveLinkage.deleteLinkSetFromModel(appState, killSet, rcxI, support);
          } else {
            //
            // Get the landing pad figured out, then change the target
            //
            int currPadNum = PropagateSupport.findLandingPad(newNodeID, padNum, rcxI, rcxR, null);
            undo = gi.changeLinkageTargetNode(childLink, newNodeID, currPadNum);
            support.addEdit(new GenomeChangeCmd(appState, rcxI, undo));    
          }
          changeSet.add(clid);
          changedModels.add(rcxI.getGenomeID());
        }
        totalChangeSet.addAll(changeSet);
      } 
        
      //
      // Now do all the VFN kids:
      //
  
      giit = rcxR.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (gi.getVfgParent() == null) {
          continue;
        }
        String giid = gi.getID();
        Map<String, String> quickKillForModel = (quickKillMap == null) ? null : quickKillMap.get(giid);
  
        GenomeInstance rgi = gi.getVfgParentRoot();
        String rgiid = rgi.getID();
        Set<String> changeSet = changeMap.get(rgiid);
        if ((changeSet == null) && (quickKillForModel == null)) {
          continue;
        }
        Iterator<String> csit = changeSet.iterator();
        while (csit.hasNext()) {
          String clid = csit.next();
          Linkage rLink = rgi.getLinkage(clid);
          undo = null;
          if (rLink == null) {
            undo = gi.removeLinkage(clid);
          } else {
            String trgID = rLink.getTarget();
            if ((gi.getNode(trgID) == null) || 
                ((quickKillForModel != null) && (quickKillForModel.get(clid) == null))) {
              undo = gi.removeLinkage(clid);
            } else {
              Linkage cLink = gi.getLinkage(clid);
              if (cLink != null) {
                undo = gi.changeLinkageTargetNode(cLink, trgID, rLink.getLandingPad());
              }
            }
          }
          if (undo != null) {
            support.addEdit(new GenomeChangeCmd(appState, rcxR, undo));
            changedModels.add(giid);
          }
        }
      }       
  
      //
      // Do this at the end
        
      Iterator<String> cmit = changedModels.iterator();
      while (cmit.hasNext()) {
        String modelID = cmit.next();
        ModelChangeEvent mcev = new ModelChangeEvent(modelID, ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }   
  
      return; 
    }
    
    /***************************************************************************
    **
    ** Change the link source node
    */  
   
    private boolean changeLinkSourceNode(Set<String> throughSeg, LinkSegmentID breakSegID, 
                                         Intersection inter, int padNum, 
                                         LinkSegmentID segID, String sourceID,
                                         DataAccessContext rcxR, //Layout layout, DBGenome genome, 
                                         BTState appState) {
      
  
      //
      // 1) We need to change the source node of all the linkages in this model, as well
      // as all child models.
      //
      // 2) We need to change the launch pad as well
      //
      // 3) We need to create or modify the new bus, and delete linkages from the old
      // bus, but retain as much linkage structure as possible.  This also needs to happen
      // in child root instances.
      //
      
      NodeChange ambiguity = linkNodeChangeIsAmbiguous(appState, throughSeg, sourceID, true);
      Map<String, Map<String, String>> quickKillMap = null;
      
      ResourceManager rMan = appState.getRMan(); 
      switch (ambiguity) {
        case AMBIGUOUS:
          quickKillMap = new HashMap<String, Map<String, String>>();
          QuickKillResult isQuick = ambiguousLinkNodeChangeIsQuickKill(appState, throughSeg, sourceID, quickKillMap, true);
          
          if (isQuick.mustDeleteLinks) {
            int resultMDL = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("srcChange.GottaDeleteMsg")),
                                                          rMan.getString("srcChange.GottaDeleteTitle"),
                                                          JOptionPane.YES_NO_OPTION);
            if (resultMDL == 1) {
              return (false);
            }
          }
          
          boolean showReview = false;
          if (isQuick.isQuickKill) {
            int result = JOptionPane.showOptionDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("srcChange.doQuickKill")),
                                                                  rMan.getString("srcChange.messageTitle"),
                                                                  JOptionPane.DEFAULT_OPTION, 
                                                                  JOptionPane.QUESTION_MESSAGE, 
                                                                  null, new Object[] {
                                                                    rMan.getString("srcChange.review"),
                                                                    rMan.getString("srcChange.continue"),
                                                                    rMan.getString("srcChange.cancel"),
                                                                  }, rMan.getString("srcChange.continue"));
  
            if (result == 0) {
              showReview = true;
            } else if (result == 2) {
              return (false);
            }       
          }
          
          if (!isQuick.isQuickKill || showReview) {
            ChangeSourceOrTargetNodeReviewDialog csnrd = new ChangeSourceOrTargetNodeReviewDialog(appState, rcxR_, sourceID, throughSeg, quickKillMap, true);
            csnrd.setVisible(true);
            if (!csnrd.haveResult()) {
              return (false); 
            }
            quickKillMap = csnrd.getInstanceChoices();
          }
          break;
        case UNAMBIGUOUS_COLLAPSE:
          int resultUC = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("srcChange.unambiguousCollapseMsg")),
                                                       rMan.getString("srcChange.unambiguousCollapseTitle"),
                                                       JOptionPane.YES_NO_OPTION);
          if (resultUC == 1) {
            return (false);
          }
          break;
        case UNAMBIGUOUS_REGION_CHANGE:
          int resultURC = JOptionPane.showConfirmDialog(appState.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("srcChange.unambiguousRegionChgMsg")),
                                                        rMan.getString("srcChange.unambiguousRegionChgTitle"),
                                                        JOptionPane.YES_NO_OPTION);
          if (resultURC == 1) {
            return (false);
          }
          break;
        case UNAMBIGUOUS_SIMPLE:
          break;
        default:
          throw new IllegalStateException();
      }
      
      UndoSupport support = new UndoSupport(appState, "undo.changeLinkSourceNode");    
      PostSourceNodeChanger changer = new PostSourceNodeChanger();
      changer.manageTheChange(throughSeg, breakSegID, padNum, quickKillMap, ambiguity,
                              segID, sourceID, rcxR, appState, support);
      return (true); 
    }
    
    /***************************************************************************
    **
    ** Answer if changing a link source/target node is ambiguous
    */  
   
    private NodeChange linkNodeChangeIsAmbiguous(BTState appState, Set<String> throughSeg, String nodeID, boolean isSource) {
      
      //
      // The change is completely unambiguous if every child model that contains an
      // instance of an original link contains exactly one and only one instance of 
      // the new node.  There may be a case for warning if the switch requires
      // a change in the (source, target) region tuple for one or more link instances.
      // Also, if there are many copies of the original source, the change could collapse
      // all links to emerge from the same node; this obviously involves the same (S,T)
      // region tuple change, but is probably more "severe" for the user.
      //
  
      boolean regionSwitch = false;
      boolean haveCollapse = false;
      Database db = appState.getDB();   
      Iterator<GenomeInstance> giit = db.getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        boolean hasLink = false;
        HashSet<String> nodeGroups = new HashSet<String>();
        Iterator<String> alit = throughSeg.iterator();
        while (alit.hasNext()) {
          String nextLinkID = alit.next();
          Set<String> instances = gi.returnLinkInstanceIDsForBacking(nextLinkID);
          if (!instances.isEmpty()) {
            hasLink = true;
            Iterator<String> iit = instances.iterator();
            while (iit.hasNext()) {
              String iid = iit.next();
              Linkage linki = gi.getLinkage(iid);
              String theNode = (isSource) ? linki.getSource() : linki.getTarget();
              Group gfn = gi.getGroupForNode(theNode, GenomeInstance.ALWAYS_MAIN_GROUP);
              if (gfn == null) { // Should not happen; be safe
                return (NodeChange.AMBIGUOUS);
              }  
              String grpID = gfn.getID();
              nodeGroups.add(grpID);
            }
          }
        }
        if (hasLink) {
          Set<String> nodeInst = gi.getNodeInstances(nodeID);
          if (nodeInst.size() != 1) {
            return (NodeChange.AMBIGUOUS);
          }
          if (nodeGroups.size() > 1) {
            haveCollapse = true;
          } else {
            String oldGrpID = nodeGroups.iterator().next();
            String theNode = nodeInst.iterator().next();
            Group gfn = gi.getGroupForNode(theNode, GenomeInstance.ALWAYS_MAIN_GROUP);
            if (gfn == null) { // Should not happen; be safe
              return (NodeChange.AMBIGUOUS);
            }            
            String newGrpID = gfn.getID();
            if (!oldGrpID.equals(newGrpID)) {
              regionSwitch = true;
            }
          }
        }  
      }
      
      if (haveCollapse) {
        return (NodeChange.UNAMBIGUOUS_COLLAPSE);
      } else if (regionSwitch) {
        return (NodeChange.UNAMBIGUOUS_REGION_CHANGE);     
      } else {
        return (NodeChange.UNAMBIGUOUS_SIMPLE);
      }
    }
    
    /***************************************************************************
    **
    ** If there is some ambiguity in the switch, it is possible that the simple rule of
    ** "change to the new source/targ node in the same region as the old source/targ node" may
    ** handle all the ambiguous cases.  See if that is the case.
    */  
    
    private QuickKillResult ambiguousLinkNodeChangeIsQuickKill(BTState appState, Set<String> throughSeg, String nodeID, 
                                                               Map<String, Map<String, String>> quickKillMap, boolean isSource) {
  
      QuickKillResult retval = new QuickKillResult();
      retval.isQuickKill = true;
      retval.mustDeleteLinks = false;
        
      Database db = appState.getDB();   
      Iterator<GenomeInstance> giit = db.getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        
        // FIX ME!! Only care about top-level instances for MAP, but need to look at
        // everybody to see if it works?
        
        String giid = gi.getID();
        HashMap<String, String> mapForModel = new HashMap<String, String>();
        quickKillMap.put(giid, mapForModel);
        HashMap<String, String> groupMap = new HashMap<String, String>();
        Set<String> nodeInst = gi.getNodeInstances(nodeID);
        boolean haveATarget = !nodeInst.isEmpty();
        String lastResort = (haveATarget) ? (String)nodeInst.iterator().next() : null;
        Iterator<String> niit = nodeInst.iterator();
        while (niit.hasNext()) {
          String theNode = niit.next();
          Group gfn = gi.getGroupForNode(theNode, GenomeInstance.ALWAYS_MAIN_GROUP); 
          if (gfn == null) { // should not happen
            retval.isQuickKill = false;
            retval.mustDeleteLinks = (retval.mustDeleteLinks) || !haveATarget;
          } else {
            String newGrpID = gfn.getID();
            groupMap.put(newGrpID, theNode);
          }
        }
      
        Iterator<String> alit = throughSeg.iterator();
        while (alit.hasNext()) {
          String nextLinkID = alit.next();
          Set<String> instances = gi.returnLinkInstanceIDsForBacking(nextLinkID);
          Iterator<String> iit = instances.iterator();
          while (iit.hasNext()) {
            String iid = iit.next();
            Linkage linki = gi.getLinkage(iid);
            String theNode = (isSource) ? linki.getSource() : linki.getTarget();
            Group grp = gi.getGroupForNode(theNode, GenomeInstance.ALWAYS_MAIN_GROUP);
            if (grp == null) { // should not happen
              retval.isQuickKill = false;
              retval.mustDeleteLinks = (retval.mustDeleteLinks) || !haveATarget;
              if (haveATarget) {
                mapForModel.put(iid, lastResort);
              }
            } else {
              String grpID = grp.getID();
              String newNodeID = groupMap.get(grpID);
              if (newNodeID == null) {  // No quick kill switch under the rule
                retval.isQuickKill = false;
                retval.mustDeleteLinks = (retval.mustDeleteLinks) || !haveATarget;
                if (haveATarget) {
                  mapForModel.put(iid, lastResort);
                }
              } else {
                mapForModel.put(iid, newNodeID);
              }
            }
          }
        }
      }
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Quick Kill answer
  **
  */
  
  private static class QuickKillResult {
    boolean isQuickKill;
    boolean mustDeleteLinks;
  }

  /***************************************************************************
  **
  ** Wrapper for changing source nodes
  */
  
  private static class PostSourceNodeChanger implements BackgroundWorkerOwner {
    
    private BTState myAppState_;
    
    void manageTheChange(Set<String> throughSeg, LinkSegmentID breakSegID, 
                         int padNum, Map<String, Map<String, String>> quickKillMap, NodeChange ambiguity,
                         LinkSegmentID segID, String sourceID, DataAccessContext rcxR, 
                         BTState appState, UndoSupport support) { 
      
      myAppState_ = appState;

      SourceNodeChangeRunner runner = 
        new SourceNodeChangeRunner(throughSeg, breakSegID, 
                                   padNum, quickKillMap, ambiguity,
                                   segID, sourceID,
                                   rcxR, appState, support);
      //
      // We may need to do lots of link relayout operations.  This MUST occur on a background
      // thread!
      // 

      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      return;
    }
      

    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }
    
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      return;
    }     
        
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(myAppState_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      return;
    }                
  }  
 
  /***************************************************************************
  **
  ** Background multi-layout source node changer
  */ 
    
  private static class SourceNodeChangeRunner extends BackgroundWorker {

    private Set<String> throughSeg_;
    private LinkSegmentID breakSegID_; 
    private int padNum_;
    private Map<String, Map<String, String>> quickKillMap_; 
    private NodeChange ambiguity_;
    private LinkSegmentID segID_; 
    private String sourceID_;
    private DataAccessContext rcxR_;
    private UndoSupport support_;
    private List<LayoutLinkSupport.GlobalLinkRequest> requestList_;
    private BTState myAppState_;

    public SourceNodeChangeRunner(Set<String> throughSeg, LinkSegmentID breakSegID, 
                                  int padNum, Map<String, Map<String, String>> quickKillMap, NodeChange ambiguity,
                                  LinkSegmentID segID, String sourceID,
                                  DataAccessContext rcxR, BTState appState, UndoSupport support) {
      super(new LinkRouter.RoutingResult());
      throughSeg_ = throughSeg;
      breakSegID_ = breakSegID;
      padNum_ = padNum;
      quickKillMap_ = quickKillMap;
      ambiguity_ = ambiguity;
      segID_ = segID;
      sourceID_ = sourceID;
      rcxR_ = rcxR;
      myAppState_ = appState;
      support_ = support;
      requestList_ = new ArrayList<LayoutLinkSupport.GlobalLinkRequest>();
    }
    
    public Object runCore() throws AsynchExitRequestException {
      requestList_ = 
        changeLinkSourceNodeBackground(throughSeg_, breakSegID_, padNum_, quickKillMap_, 
                                       ambiguity_, segID_, sourceID_, 
                                       support_, rcxR_, myAppState_, this, 0.0, 0.2);
      LayoutOptions lopt = new LayoutOptions(myAppState_.getLayoutOptMgr().getLayoutOptions());
      LinkRouter.RoutingResult result = LayoutLinkSupport.relayoutLinksGlobally(myAppState_, requestList_, support_, lopt, this, 0.2, 1.0);
      int numReq = requestList_.size();
      for (int i = 0; i < numReq; i++) {
        LayoutLinkSupport.GlobalLinkRequest glr = requestList_.get(i);
        support_.addEvent(new LayoutChangeEvent(glr.layout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      }
      myAppState_.getDB().clearAllDynamicProxyCaches();
      return (result);
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }
  
    /***************************************************************************
    **
    ** Change the link source node; this stuff happens on a background thread
    */  
   
    public static List<LayoutLinkSupport.GlobalLinkRequest> changeLinkSourceNodeBackground(Set<String> throughSeg, LinkSegmentID breakSegID, 
                                                                                           int padNum, Map<String, Map<String, String>> quickKillMap, 
                                                                                           NodeChange ambiguity,
                                                                                           LinkSegmentID segID, String sourceID,
                                                                                           UndoSupport support,
                                                                                           DataAccessContext rcxR,
                                                                                         //  DBGenome genome, 
                                                                                           BTState appState, BTProgressMonitor monitor,
                                                                                           double startFrac, double endFrac) throws AsynchExitRequestException {
   //   Database db = appState.getDB();    
      ArrayList<LayoutLinkSupport.GlobalLinkRequest> requestList = new ArrayList<LayoutLinkSupport.GlobalLinkRequest>();     
      HashSet<String> changedModels = new HashSet<String>();
   //   LayoutManager lm = appState.getLayoutMgr();
  
      //
      // Figure out where breaks will occur in kid instances before we start to tear
      // everything apart:
      //
      
   
   //   String rootGid = genome.getID();
    //  Layout rolo = db.getLayout(lm.getLayout(rootGid));
      
       
      Map<String, Map<String, Layout.InheritedInsertionInfo>> matchingSegs = findMatchingLinkSegsInRootInstances(appState, throughSeg, breakSegID, rcxR);
      Map<String, Map<String, Layout.InheritedInsertionInfo>> interSegs = null;
      String segTag = null;
      if (segID != null) {
        if (segID.isTaggedWithEndpoint()) {
          segTag = (segID.startEndpointIsTagged()) ? LinkSegmentID.START : LinkSegmentID.END;
        }
        BusProperties bp = rcxR.getLayout().getBusForSource(sourceID);
        Set<String> interSegLinks = bp.resolveLinkagesThroughSegment(segID);
        interSegs = findMatchingLinkSegsInRootInstances(appState, interSegLinks, segID, rcxR);            
      }
       
      //
      // Have to change source node of all linkages.
      //
  
      HashMap<String, Map<String, Set<String>>> oldSrcToLinkMap = new HashMap<String, Map<String, Set<String>>>();
      Iterator<String> alit = throughSeg.iterator();
      while (alit.hasNext()) {
        String nextLinkID = alit.next();
        Linkage nextLink = rcxR.getGenome().getLinkage(nextLinkID);
  
        //
        // Root model:
        //
        
        GenomeChange undo = rcxR.getGenome().changeLinkageSourceNode(nextLink, sourceID, padNum);
        support.addEdit(new GenomeChangeCmd(appState, rcxR, undo));
        changedModels.add(rcxR.getGenomeID());
         
        //
        // Do the top-level instances first:
        //
        
        // Note changeMap is just for the current link
        HashMap<String, Set<String>> changeMap = new HashMap<String, Set<String>>();
        Iterator<GenomeInstance> giit = rcxR.getGenomeSource().getInstanceIterator();
        while (giit.hasNext()) {
          GenomeInstance gi = giit.next();
          if (gi.getVfgParent() != null) {
            continue;
          }
          DataAccessContext rcxi = new DataAccessContext(rcxR, gi.getID());
          Set<String> nodeInst = gi.getNodeInstances(sourceID);
          Map<String, String> quickKillForModel = (quickKillMap == null) ? null : quickKillMap.get(rcxi.getGenomeID());
          
          //
          // Track for use in VFN kids:
          //
          HashSet<String> changeSet = new HashSet<String>();
          changeMap.put(rcxi.getGenomeID(), changeSet);
          
          //
          // Track src changes for instance layout calc:
          //
          
          Map<String, Set<String>> o2nl4GI = oldSrcToLinkMap.get(rcxi.getGenomeID());
          if (o2nl4GI == null) {
            o2nl4GI = new HashMap<String, Set<String>>();
            oldSrcToLinkMap.put(rcxi.getGenomeID(), o2nl4GI);
          }
   
          //
          // Crank through each instance of the switched link:
          //
          
          Set<String> linkSet = gi.returnLinkInstanceIDsForBacking(nextLinkID);
          Iterator<String> lit = linkSet.iterator();
          while (lit.hasNext()) {
            String clid = lit.next();
            Linkage childLink = gi.getLinkage(clid);
            
            //
            // Get the old src->link map ready:
            //
            
            String oldSrc = childLink.getSource();  // Has not changed yet...
            Set<String> linksForOld = o2nl4GI.get(oldSrc);
            if (linksForOld == null) {
              linksForOld = new HashSet<String>();
              o2nl4GI.put(oldSrc, linksForOld);
            }
            linksForOld.add(clid);
           
            //
            // Figure out the new source node:
            //
            
            String newNodeID;
            switch (ambiguity) {
              case AMBIGUOUS: 
                newNodeID = quickKillForModel.get(clid);
                break;
              case UNAMBIGUOUS_COLLAPSE:
              case UNAMBIGUOUS_REGION_CHANGE:
              case UNAMBIGUOUS_SIMPLE:        
                if (nodeInst.size() != 1) {
                  throw new IllegalStateException();
                }
                newNodeID = nodeInst.iterator().next();
                break;
              default:
                throw new IllegalStateException();
            }              
            
            //
            // No place to go, we need to kill off the link:
            //
            if (newNodeID == null) {
              //Set killSet = gi.returnLinkInstanceIDsForBacking(nextLinkID);
              HashSet<String> killSet = new HashSet<String>();
              killSet.add(clid);
              RemoveLinkage.deleteLinkSetFromModel(appState, killSet, rcxi, support);
            } else {
              //
              // Get the source pad figured out.  We may need to force the issue
              // and change pads around to get a free pad:
              //
              int currPadNum = prepareSourcePad(appState, newNodeID, rcxi, rcxR, padNum, support);      
                  
              undo = gi.changeLinkageSourceNode(childLink, newNodeID, currPadNum);
              support.addEdit(new GenomeChangeCmd(appState, rcxi, undo));    
            }
            changeSet.add(clid);
            changedModels.add(rcxi.getGenomeID());
          }
        } 
        
        //
        // Now do all the VFN kids:
        //
              
        giit = rcxR.getGenomeSource().getInstanceIterator();
        while (giit.hasNext()) {
          GenomeInstance gi = giit.next();
          if (gi.getVfgParent() == null) {
            continue;
          }
          DataAccessContext rcxi = new DataAccessContext(rcxR, gi.getID());
          Map<String, String> quickKillForModel = (quickKillMap == null) ? null : quickKillMap.get(rcxi.getGenomeID());
  
          GenomeInstance rgi = gi.getVfgParentRoot();
          String rgiid = rgi.getID();
          Set<String> changeSet = changeMap.get(rgiid);
          if ((changeSet == null) && (quickKillForModel == null)) {
            continue;
          }
          Iterator<String> csit = changeSet.iterator();
          while (csit.hasNext()) {
            String clid = csit.next();
            Linkage rLink = rgi.getLinkage(clid);
            undo = null;
            if (rLink == null) {
              undo = gi.removeLinkage(clid);
            } else {
              String srcID = rLink.getSource();
              if ((gi.getNode(srcID) == null) || 
                  ((quickKillForModel != null) && (quickKillForModel.get(clid) == null))) {
                undo = gi.removeLinkage(clid);
              } else {
                Linkage cLink = gi.getLinkage(clid);
                if (cLink != null) {
                  undo = gi.changeLinkageSourceNode(cLink, srcID, rLink.getLaunchPad());
                }
              }
            }
            if (undo != null) {
              support.addEdit(new GenomeChangeCmd(appState, rcxi, undo));
              changedModels.add(rcxi.getGenomeID());
            }
          }
        }       
      }
     
      //
      // Do this at the end
        
      Iterator<String> cmit = changedModels.iterator();
      while (cmit.hasNext()) {
        String modelID = cmit.next();
        ModelChangeEvent mcev = new ModelChangeEvent(modelID, ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
      
      //
      // Now get the layouts fixed
      //
      
      Layout.PropChange[] pca = rcxR.getLayout().supportLinkSourceBreakoff(breakSegID, throughSeg, sourceID, segID);
      if ((pca != null) && (pca.length != 0)) {
        PropChangeCmd pcc = new PropChangeCmd(appState, rcxR, pca);
        support.addEdit(pcc);
        LayoutChangeEvent lcev = new LayoutChangeEvent(rcxR.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(lcev);
      }   
      
      //
      // Fix the instance layouts.  We will try to do something rational before giving up:
      //
      
      Iterator<GenomeInstance> giit = rcxR.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }
        DataAccessContext rcxi = new DataAccessContext(rcxR, gi.getID());       
        Map<String, Set<String>> o2nl4GI = oldSrcToLinkMap.get(rcxi.getGenomeID());
        Set<String> needsLayout = breakUpInheritedTree(appState, rcxi, matchingSegs, segTag, interSegs, o2nl4GI, support);
        if (!needsLayout.isEmpty()) {
          LayoutLinkSupport.GlobalLinkRequest glr = new LayoutLinkSupport.GlobalLinkRequest();
          glr.genome = rcxi.getGenome();
          glr.layout = rcxi.getLayout();
          glr.gSrc = rcxi.getGenomeSource();
          glr.badLinks = needsLayout;
          requestList.add(glr);        
        }
      }
      LayoutChangeEvent lcev = new LayoutChangeEvent(rcxR.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);
     
      return (requestList); 
    }
    
    /***************************************************************************
    **
    ** Find places to resource link trees in inherited layouts
    */  
   
    private static Map<String, Map<String, Layout.InheritedInsertionInfo>> findMatchingLinkSegsInRootInstances(BTState appState, Set<String> throughSeg, 
                                                                                                               LinkSegmentID breakSegID, DataAccessContext rcxR) {
                                                                                                           //    DBGenome genome) {
      
      HashMap<String, Map<String, Layout.InheritedInsertionInfo>> retval = new HashMap<String, Map<String, Layout.InheritedInsertionInfo>>();
      if (throughSeg.isEmpty()) {
        return (retval);
      }
   
      String aLinkID = throughSeg.iterator().next();
      BusProperties rootProps = rcxR.getLayout().getLinkProperties(aLinkID);
      
      Point2D rootSplit = rootProps.getSegmentGeometryForID(breakSegID, rcxR, false).pointAtFraction(0.5); 
      
      Iterator<GenomeInstance> giit = rcxR.getGenomeSource().getInstanceIterator();
      while (giit.hasNext()) {
        GenomeInstance gi = giit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }
        DataAccessContext rcxi = new DataAccessContext(rcxR, gi.getID());       
        HashMap<String, Layout.InheritedInsertionInfo> resultsPerSrc = new HashMap<String, Layout.InheritedInsertionInfo>();
        retval.put(gi.getID(), resultsPerSrc);
        
        HashMap<String, Set<String>> linksPerSrc = null;
        //
        // Build up the set from each source in the instance:
        //
        linksPerSrc = new HashMap<String, Set<String>>();
        Iterator<String> tsit = throughSeg.iterator();
        while (tsit.hasNext()) {
          String linkID = tsit.next();
          Set<String> linkSet = gi.returnLinkInstanceIDsForBacking(linkID);
          Iterator<String> lit = linkSet.iterator();
          while (lit.hasNext()) {
            String clid = lit.next();
            Linkage childLink = gi.getLinkage(clid);
            String srcID = childLink.getSource();
            Set<String> links = linksPerSrc.get(srcID);
            if (links == null) {
              links = new HashSet<String>();
              linksPerSrc.put(srcID, links);
            }
            links.add(clid);
          }
        }
        
        Iterator<String> lpskit = linksPerSrc.keySet().iterator();
        while (lpskit.hasNext()) {
          String srcID = lpskit.next();
          Set<String> links = linksPerSrc.get(srcID); 
          Layout.InheritedInsertionInfo iii =  
            rcxi.getLayout().findInheritedMatchingLinkSegment(breakSegID, links, rootProps, rootSplit, rcxR, rcxi);
          if (iii != null) {
            resultsPerSrc.put(srcID, iii);
          }
        }
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Get source pad ready to go on new source node
    */  
   
    private static int prepareSourcePad(BTState appState, String nodeID, DataAccessContext rcxI, DataAccessContext rcxR,
                                      //  GenomeInstance gi, 
                                        int prefPad, 
                                    //    Layout gilo, 
                                        UndoSupport support) {  
   
      Integer srcPad = rcxI.getGenome().getSourcePad(nodeID);
      if (srcPad == null) {
        srcPad = PropagateSupport.findSourcePad(nodeID, prefPad, rcxI, rcxR, null);
        if (srcPad == null) {
          PropagateSupport.emergencyPadForce(appState, nodeID, rcxI, support);  // revert to root configuration
          srcPad = new Integer(prefPad);
        } 
      }
      return (srcPad.intValue());  
    }
  
    /***************************************************************************
    **
    ** Handle link reorganization for top instance layouts
    */  
   
    private static Set<String> breakUpInheritedTree(BTState appState, DataAccessContext rcxI, //String giID, GenomeInstance gi, Layout lo,
                                                    Map<String, Map<String, Layout.InheritedInsertionInfo>> matchingSegs, 
                                                    String segTag, 
                                                    Map<String, Map<String, Layout.InheritedInsertionInfo>> interSegs,
                                                    Map<String, Set<String>> oldSrcToLinks, UndoSupport support) {
  
      Map<String, Layout.InheritedInsertionInfo> segsPerSrc = matchingSegs.get(rcxI.getGenomeID());
      boolean noBreakSegs = (segsPerSrc == null);
      boolean noInterSegs = (interSegs == null);
      Map<String, Layout.InheritedInsertionInfo> interSegsPerSrc = null;
      if (!noInterSegs) {
        interSegsPerSrc = interSegs.get(rcxI.getGenomeID());
        noInterSegs = (interSegsPerSrc == null);
      }
  
      //
      // If the set of targets from an old single source are now being
      // shared by more than one source, we cannot continue with the
      // preservation of old layout.  Identify these cases:
      //
      
      HashMap<String, String> newForOld = new HashMap<String, String>();
      HashSet<String> multiSources = new HashSet<String>();
      Iterator<String> osit = oldSrcToLinks.keySet().iterator();
      while (osit.hasNext()) {
        String oldSrcID = osit.next();
        Set<String> linkIDs = oldSrcToLinks.get(oldSrcID);
        Iterator<String> lidit = linkIDs.iterator();
        while (lidit.hasNext()) {
          String linkID = lidit.next();
          Linkage link = rcxI.getGenome().getLinkage(linkID);
          // Dead links don't count yet:
          if (link != null) {
            String srcID = link.getSource(); // This is the _new_ src by now...
            String newSrcID = newForOld.get(oldSrcID);
            if (newSrcID == null) {
              newForOld.put(oldSrcID, srcID);
            // If we are targeting more than one new source, this approach is hosed...
            } else if (!newSrcID.equals(srcID)) {
              multiSources.add(oldSrcID);
              break;
            }
          }
        }
      }
      
      //
      // Now construct the list of linkages that cannot be handled by this method.  They
      // will be killed off and laid out in another fashion.
      //
      
      HashSet<String> redoSet = new HashSet<String>();
      HashMap<String, Map<String, Set<String>>> survivePerSrc = new HashMap<String, Map<String, Set<String>>>();
      
      osit = oldSrcToLinks.keySet().iterator();
      while (osit.hasNext()) {
        String oldSrcID = osit.next();
        Set<String> linkIDs = oldSrcToLinks.get(oldSrcID);
        boolean gottaRedo = multiSources.contains(oldSrcID);
        boolean noBreakCandidate = noBreakSegs;
        if (!noBreakSegs) {
          Layout.InheritedInsertionInfo iii = segsPerSrc.get(oldSrcID);     
          noBreakCandidate = ((iii == null) || (iii.segID == null));
        }
        Iterator<String> lidit = linkIDs.iterator();
        while (lidit.hasNext()) {
          String linkID = lidit.next();
          Linkage link = rcxI.getGenome().getLinkage(linkID);
          if (link == null) {
            continue;  // Should not happen; dead links deleted already...?
          } else if (gottaRedo || noBreakCandidate) {
            redoSet.add(linkID);
          } else {
            //
            // Survivors go here.  Note that several different trees of links from old sources
            // may converge on a single new source, so we need to keep them segregated into
            // sets per old source:
            //
            String newSrcID = newForOld.get(oldSrcID);     
            Map<String, Set<String>> perOldSrcMap = survivePerSrc.get(newSrcID);
            if (perOldSrcMap == null) {
              perOldSrcMap = new HashMap<String, Set<String>>();
              survivePerSrc.put(newSrcID, perOldSrcMap);
            }
            Set<String> perOldSrcSet = perOldSrcMap.get(oldSrcID);
            if (perOldSrcSet == null) {
              perOldSrcSet = new HashSet<String>();
              perOldSrcMap.put(oldSrcID, perOldSrcSet);
            }          
            perOldSrcSet.add(linkID);
          }
        }
      }
      
      //
      // Kill off the link properties for guys who are disappearing.  We cannot auto add
      // until we repair the links; auto add expects a consistent layout with the genome!
      //
      
      Map<String, BusProperties.RememberProps> remem = rcxI.getLayout().buildRememberProps(rcxI);
      
      Iterator<String> rsit = redoSet.iterator();
      while (rsit.hasNext()) {
        String nextLinkID = rsit.next();
        PropChange pc = rcxI.getLayout().removeLinkProperties(nextLinkID);
        support.addEdit(new PropChangeCmd(appState, rcxI, pc));
      }
      
      //
      // New intersections.  Five cases: 
      //    1) Have an intersection candidate - no problem, we are done
      //    2) No candidate, first one tree to relocate 
      //       a) If no existing outbound links, no problem, we are done
      //       b) If existing links, either choose root or split it first
      //    3) No candidate, multi tree to relocate.
      //       a) First tree is direct, so no place to plug in.  Gotta split it.
      //       b) First tree is segmented, need to choose the root ID after the first.
      //
      
      HashMap<String, LinkSegmentID> taggedIntersegs = new HashMap<String, LinkSegmentID>();
      if (!noInterSegs) {
        Iterator<String> spsit = survivePerSrc.keySet().iterator();
        while (spsit.hasNext()) {
          String newSrcID = spsit.next();
          Layout.InheritedInsertionInfo iiii = interSegsPerSrc.get(newSrcID);
          LinkSegmentID segID = ((iiii == null) || (iiii.segID == null)) ? null : iiii.segID;
          if (segID != null) {
            LinkSegmentID tagged = segID.clone();
            tagged.tagIDWithEndpoint((segTag == null) ? LinkSegmentID.END : segTag);
            taggedIntersegs.put(newSrcID, tagged);
          }
        }
      }
      
      //
      // Now do the changes
      //
      
      Iterator<String> spsit = survivePerSrc.keySet().iterator();
      while (spsit.hasNext()) {
        String newSrcID = spsit.next();
        Map<String, Set<String>> perOldSrcMap = survivePerSrc.get(newSrcID);
        Iterator<String> oskit = perOldSrcMap.keySet().iterator();
        while (oskit.hasNext()) {
          String oldSrcID = oskit.next();
          LinkSegmentID breakSegID = segsPerSrc.get(oldSrcID).segID;
          Set<String> perOldSrcLinks = perOldSrcMap.get(oldSrcID);
          LinkSegmentID interSegID = taggedIntersegs.get(newSrcID);
          // Link will be direct for interSegID == null:
          if ((interSegID == null) || interSegID.isDirect()) {
            BusProperties bp = rcxI.getLayout().getBusForSource(newSrcID);
            if (bp != null) {
              if (bp.isDirect()) { // split it
                Layout.PropChange pc = rcxI.getLayout().splitDirectLinkInHalf(newSrcID, rcxI);
                if (pc != null) {
                  PropChangeCmd pcc = new PropChangeCmd(appState, rcxI, pc);
                  support.addEdit(pcc);
                }            
              }
              LinkSegmentID rootDrop = LinkSegmentID.buildIDForStartDrop();
              rootDrop.tagIDWithEndpoint(LinkSegmentID.START);
              taggedIntersegs.put(newSrcID, rootDrop);
              interSegID = rootDrop;
            }
          }        
          Layout.PropChange[] pca = rcxI.getLayout().supportLinkSourceBreakoff(breakSegID, perOldSrcLinks, newSrcID, interSegID);
          if ((pca != null) && (pca.length != 0)) {
            PropChangeCmd pcc = new PropChangeCmd(appState, rcxI, pca);
            support.addEdit(pcc);
          }
        }
      }
      
      //
      // Do a quick auto add back for guys who need relayout.  AutoAddCrude needs to happen
      // after we have fixed everybody; it expects the layout to be consistent with the genome.
      //
         
      rsit = redoSet.iterator();
      while (rsit.hasNext()) {
        String nextLinkID = rsit.next();
        LayoutLinkSupport.autoAddCrudeLinkProperties(appState, rcxI, nextLinkID, support, remem);
      }
       
      LayoutChangeEvent lcev = new LayoutChangeEvent(rcxI.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);
      return (redoSet);
    }  
}
