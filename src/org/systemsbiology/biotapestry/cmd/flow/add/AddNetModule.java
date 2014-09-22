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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.NetModuleCreationDialog;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.freerender.NodeBounder;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle adding new network modules or adding regions to existing modules
*/

public class AddNetModule extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private boolean forNew_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AddNetModule(BTState appState, boolean forNew) {
    super(appState);  
    forNew_ = forNew;
    name = (forNew_) ? "command.DrawNewNetworkModule" : "modulePopup.addRegionToModule";
    desc = (forNew_) ? "command.DrawNewNetworkModule" : "modulePopup.addRegionToModule";
    icon = (forNew_) ? "CreateNewModule24.gif" : "FIXME24.gif";
    mnem = (forNew_) ? "command.DrawNewNetworkModuleMnem" : "modulePopup.addRegionToModuleMnem"; 
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
    return (true);
  }

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Answer if we are valid for a popup
  ** 
  */
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    String overlayKey = rcx.oso.getCurrentOverlay();
    NetOverlayProperties nop = rcx.getLayout().getNetOverlayProperties(overlayKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(inter.getObjectID());
    int displayType = nmp.getType();
    return (displayType != NetModuleProperties.CONTIG_RECT);
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  @Override    
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, forNew_, dacx);
    return (retval);
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
        StepState ans = new StepState(appState_, forNew_, cfh.getDataAccessContext());
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
        } else if (ans.getNextStep().equals("stepContinueNetModuleDrawing")) {
          next = ans.stepContinueNetModuleDrawing();
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
    ans.nextStep_ = "stepContinueNetModuleDrawing"; 
    return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans));
  }
   
  /***************************************************************************
  **
  ** Running State: Kinda needs cleanup!
  */
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState, DialogAndInProcessCmd.MouseClickCmdState {

    private DataAccessContext rcxT_;
    private boolean myForNew_;
    public CommonNetModuleCandidate nmc_;
    private String currentOverlay;
    private ServerControlFlowHarness cfh;
    private String runningID_;
    private NetworkOverlay targNov_; 
    
    //--------------------
     
    private String nextStep_;
    private int x;
    private int y; 
    private BTState appState_;
   
    /***************************************************************************
    **
    ** step thru
    */
    
    public String getNextStep() {
      return (nextStep_);
    } 
    
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH | MainCommands.SKIP_OVERLAY_VIZ_PUSH);
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
      return (false);
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      return (false);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.RECT_FLOATER);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      return;
    }

    public Object getFloater(int x, int y) {
      return (nmc_.getCurrentRect(x, y));
    }
    
    public Color getFloaterColor() {
      return ((nmc_ != null) ? nmc_.color : null);
    }  
    
    /***************************************************************************
    **
    ** Constructor
    */
       
    public StepState(BTState appState, boolean forNew, DataAccessContext dacx) {
      myForNew_ = forNew;
      appState_ = appState;
      runningID_ = null;
      rcxT_ = dacx;
      currentOverlay = rcxT_.oso.getCurrentOverlay();
      targNov_ = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID()).getNetworkOverlay(currentOverlay); 
      nextStep_ = "stepStart";  
    }
     
    /***************************************************************************
    **
    ** For ongoing adds
    */ 
       
    public void setIntersection(Intersection intersect) {
      runningID_ = intersect.getObjectID();
      return;
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
    ** Kick things off
    */
      
    private DialogAndInProcessCmd stepStart() { 
      if (myForNew_) {
        nmc_ = beginDrawNetworkModule();
        if (nmc_ == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
      } else {
        nmc_ = beginAddRegionsToNetworkModule(runningID_);
        if (nmc_ == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
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
      retval.suPanelMode = (myForNew_) ? PanelCommands.Mode.DRAW_NET_MODULE : PanelCommands.Mode.ADD_TO_NET_MODULE;
      return (retval);
    } 
      
    /***************************************************************************
    **
    ** Start to add a new net module
    */  
   
    private CommonNetModuleCandidate beginDrawNetworkModule() {
      HashSet<String> existingNames = new HashSet<String>();
      Iterator<NetModule> mit = targNov_.getModuleIterator();
      while (mit.hasNext()) {
        NetModule nm = mit.next();
        existingNames.add(nm.getName());
      }  
      boolean askForAttach = !(rcxT_.getGenome() instanceof DBGenome);
      boolean gotNodes = (rcxT_.getGenome().getFullNodeCount() > 0);
      NetModuleCreationDialog nmcd = new NetModuleCreationDialog(appState_, existingNames, askForAttach, gotNodes);
      nmcd.setVisible(true);
      if (!nmcd.haveResult()) {
        return (null);
      }
      String newName = nmcd.getName();
      boolean doNodeAdd = nmcd.doNodeAdd();
      int displayType = nmcd.getDisplayType();
      boolean attachToGroup = nmcd.getAttachToGroup(); 
      return (new CommonNetModuleCandidate(CommonNetModuleCandidate.Mode.DRAWING, newName, doNodeAdd, displayType, attachToGroup));
    }     
    
    /***************************************************************************
    **
    ** Continue drawing regions as needed
    */  
   
    private DialogAndInProcessCmd stepContinueNetModuleDrawing() {

      // If we are starting, then the point becomes our upper left.
      // If we are adding, then a zero-height or width rect is
      // rejected.  Else, the second point defines the rectangle.
      // Build the rectangle, find out what nodes are contained, 
      // and be done. 
      //

      if (nmc_.buildState == CommonNetModuleCandidate.DONE) {
        throw new IllegalStateException();
      } else if (nmc_.buildState == CommonNetModuleCandidate.EMPTY) {
        if (nmc_.currMode == CommonNetModuleCandidate.Mode.DRAWING) {
          if (nmc_.attachToGroup) {
            // *** Note that attachment is ALWAYS occuring to main group, not e.g. activated subgroup.
            Intersection group = appState_.getGenomePresentation().selectGroup(x, y, rcxT_);
            if (group != null) {
              nmc_.attachedGroup = group.getObjectID();
            }
          }
        }
        if (nmc_.displayType == NetModuleProperties.MEMBERS_ONLY) {
          List<Intersection.AugmentedIntersection> augs = appState_.getGenomePresentation().intersectItem(x, y, rcxT_, true, false);
          Intersection.AugmentedIntersection ai = (new IntersectionChooser(true, rcxT_)).selectionRanker(augs);
          if ((ai == null) || (ai.intersect == null)) {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
          String objID = ai.intersect.getObjectID();
          Genome gen = rcxT_.getGenome();
          Node node = gen.getNode(objID);
          if (node != null) {
            if (nmc_.attachedGroup != null) {
              GenomeInstance gi = (GenomeInstance)gen;
              Group group = gi.getGroup(nmc_.attachedGroup); 
              if (group.isInGroup(objID, gi)) {
                nmc_.nodes.add(objID);
              } else {
                return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
              }
            } else {
              nmc_.nodes.add(objID);
            }
          } else {
            return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
          }
          nmc_.buildState = CommonNetModuleCandidate.DONE;
          if (nmc_.currMode == CommonNetModuleCandidate.Mode.DRAWING) {
            drawNetworkModuleFinish();   
          } else {
            finishAddRegionsToNetworkModule();
          }
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
        } else {
          if ((nmc_.currMode == CommonNetModuleCandidate.Mode.ADDING) && (nmc_.attachedGroup != null)) {
            Intersection group = appState_.getGenomePresentation().selectGroup(x, y, rcxT_);
            if (group != null) {
              String checkGroup = group.getObjectID();           
              if (!nmc_.attachedGroup.equals(checkGroup)) {
                return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
              }
            }
          }
          nmc_.points.add(new Point(x, y));
          nmc_.buildState = CommonNetModuleCandidate.ADDING;
          appState_.getGenomePresentation().setFloater(new Rectangle2D.Double(x, y, 0, 0));   
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.ACCEPT, this));
        }
      } else if (nmc_.buildState == CommonNetModuleCandidate.ADDING) {
        Rectangle2D nextRect = nmc_.getCurrentRect(x, y);
        if ((nextRect.getHeight() == 0) || (nextRect.getWidth() == 0)) {
          return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
        }
        if ((nmc_.currMode == CommonNetModuleCandidate.Mode.DRAWING) && (nmc_.attachToGroup)) {
          Intersection group = appState_.getGenomePresentation().selectGroup(x, y, rcxT_);
          if (group != null) {
            String checkGroup = group.getObjectID();
            if (nmc_.attachedGroup != null) {
              if (!nmc_.attachedGroup.equals(checkGroup)) {
                return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.REJECT, this));
              }
            } else {
              nmc_.attachedGroup = checkGroup;
            }
          }
        }
        
        FreezeDriedOverlayOracle fdoo = new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null);
        DataAccessContext rcxF = new DataAccessContext(rcxT_);
        rcxF.oso = fdoo;
        Rectangle rect = new Rectangle((int)nextRect.getX(), (int)nextRect.getY(), 
                                       (int)nextRect.getWidth(), (int)nextRect.getHeight());
        List<Intersection> intersects = appState_.getGenomePresentation().intersectItems(rect, rcxF);
                                                     
        Genome gen = rcxT_.getGenome();
        int numInter = intersects.size();
        for (int i = 0; i < numInter; i++) {
          Intersection inter = intersects.get(i);
          String iID = inter.getObjectID();
          Node node = gen.getNode(iID);
          if (node != null) {
            if (nmc_.attachedGroup != null) {    
              GenomeInstance gi = (GenomeInstance)gen;
              Group group = gi.getGroup(nmc_.attachedGroup); 
              if (group.isInGroup(iID, gi)) {
                nmc_.nodes.add(iID);
              }
             // If asked to attach, but there has been no attachment yet, use the first
             // member to figure that out:
            } else if ((nmc_.currMode == CommonNetModuleCandidate.Mode.DRAWING) && nmc_.attachToGroup) {
              GenomeInstance gi = (GenomeInstance)gen;
              Group grp = gi.getGroupForNode(iID, GenomeInstance.ALWAYS_MAIN_GROUP);
              if (grp != null) {
                nmc_.attachedGroup = grp.getID();
              }
              nmc_.nodes.add(iID);
            } else {
              nmc_.nodes.add(iID);
            }
          }
        }
        nmc_.allRects.add(nextRect);
        nmc_.buildState = CommonNetModuleCandidate.DONE;
        appState_.getGenomePresentation().setFloater(null);
        if (nmc_.currMode == CommonNetModuleCandidate.Mode.DRAWING) {
          drawNetworkModuleFinish();
        } else {
          finishAddRegionsToNetworkModule();
        }
        return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
      }
      throw new IllegalStateException();
    }
     
    /***************************************************************************
    **
    ** Finish adding a new net module.  Caller must close undo transaction
    */  
   
    private void drawNetworkModuleFinish() {
     
      UndoSupport support = new UndoSupport(appState_, "undo.newNetworkModule");
      nmc_.netModuleID = rcxT_.getNextKey();
      Genome targG = rcxT_.getGenome();
      NetModule nmod = new NetModule(nmc_.netModuleID, nmc_.name, null, nmc_.attachedGroup);
      if (nmc_.addNodes) {
        int numMem = nmc_.nodes.size();
        for (int i = 0; i < numMem; i++) {
          String nodeID = nmc_.nodes.get(i);
          NetModuleMember nmm = new NetModuleMember(nodeID);
          nmod.addMember(nmm, targG.getID(), targG.overlayModeForOwner(), currentOverlay);
        }
      }
      Point2D memberOnlyLabelLoc = null;
      if (nmc_.displayType == NetModuleProperties.MEMBERS_ONLY) {
        String firstNodeID = nmc_.nodes.get(0);
        memberOnlyLabelLoc = NodeBounder.prelimLabelLocation(firstNodeID, rcxT_, NodeBounder.DEFAULT_EXTRA_PAD);
      }
     
      NetworkOverlayChange noc = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(targG.getID()).addNetworkModule(currentOverlay, nmod); 
      if (noc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, rcxT_, noc);
        support.addEdit(gcc);
      }
      NetModuleProperties nmp = new NetModuleProperties(nmc_.netModuleID, nmc_.displayType, nmc_.allRects, memberOnlyLabelLoc);
      Layout.PropChange pc = rcxT_.getLayout().setNetModuleProperties(nmc_.netModuleID, nmp, currentOverlay);
      if (pc != null) {
        support.addEdit(new PropChangeCmd(appState_, rcxT_, pc));
      }
      
      support.addEvent(new ModelChangeEvent(targG.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));   
      support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
       
      NetOverlayController noctr = appState_.getNetOverlayController();
      noctr.addToCurrentModules(nmc_.netModuleID, support, rcxT_);
      support.finish();
      return;
    }
    
    /***************************************************************************
    **
    ** Start to add extra regions to net module
    */  
 
    private CommonNetModuleCandidate beginAddRegionsToNetworkModule(String moduleID) {
      NetModule nmod = targNov_.getModule(moduleID);
      NetOverlayProperties nop = rcxT_.getLayout().getNetOverlayProperties(currentOverlay);
      NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
      Color currCol = nmp.getColor(appState_.getDB());
      String attachedGroup = nmod.getGroupAttachment();
      return (new CommonNetModuleCandidate(CommonNetModuleCandidate.Mode.ADDING, moduleID, nmp.getType(), attachedGroup, currCol));
    } 
 
    /***************************************************************************
    **
    ** Finish adding extra regions to net module
    */  
   
    private void finishAddRegionsToNetworkModule() {
    
      UndoSupport support = new UndoSupport(appState_, "undo.addToNetworkModule");
    
      //
      // Adding in a region may mess up existing net module linkages.  Record exisiting
      // state before changing anything:
      //
      
      Layout.PadNeedsForLayout padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);   
      
      NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
      NetworkOverlay nov = owner.getNetworkOverlay(currentOverlay);
      NetModule nmod = nov.getModule(nmc_.netModuleID);
      
      //
      // New members get added, but not if they are already there! (Double adds don't matter,
      // but then undo will remove it incorrectly!)
      //
      
      int numNodes = nmc_.nodes.size();
      for (int i = 0; i < numNodes; i++) {
        String nodeID = nmc_.nodes.get(i);
        if (nmod.isAMember(nodeID)) {
          continue;
        }
        NetModuleMember nmm = new NetModuleMember(nodeID);
        NetModuleChange nmc = nmod.addMember(nmm, owner.getID(), owner.overlayModeForOwner(), currentOverlay);
        if (nmc != null) {
          NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, rcxT_, nmc);
          support.addEdit(gcc);
        }    
      }
      
      support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));   
     
      if (nmc_.displayType != NetModuleProperties.MEMBERS_ONLY) {
        NetOverlayProperties noProps =  rcxT_.getLayout().getNetOverlayProperties(currentOverlay);
        NetModuleProperties nmp = noProps.getNetModuleProperties(nmc_.netModuleID);
        NetModuleProperties modProps = nmp.clone();
        Rectangle2D rect = nmc_.allRects.get(0);
        modProps.addShape(rect);
  
        Layout.PropChange pc =  rcxT_.getLayout().replaceNetModuleProperties(nmc_.netModuleID, modProps, currentOverlay);
        if (pc != null) {
          support.addEdit(new PropChangeCmd(appState_, rcxT_, pc));
          support.addEvent(new LayoutChangeEvent( rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
        }       
        //
        // Module link pad fixups:
        //    
        AddCommands.finishNetModPadFixups(appState_, null, null, rcxT_, padFixups, support);
      }
      support.finish();   
      return;
    } 
  }

  /***************************************************************************
  **
  ** Represents a network module that is being built
  */
  
  public static class CommonNetModuleCandidate {

    
       
    public enum Mode{DRAWING, ADDING};
    
    
    
    public static final int EMPTY  = 0;
    public static final int ADDING = 1;
    public static final int DONE   = 2;    

    public Mode currMode;
    public ArrayList<Point> points;
    public int buildState;
    public String name;
    public ArrayList<Rectangle2D> allRects;
    public ArrayList<String> nodes;
    public int displayType;
    public boolean attachToGroup;
    public String attachedGroup;
    public String netModuleID;

    // For candidate
    public boolean addNodes;
    public boolean keepGoing;
    
    
    // For Modify
    public Color color;
    
    //
    // This is the version for the Initial Drawing of a module
    //
    
    
    public CommonNetModuleCandidate(Mode currMode, String name, boolean addNodes, int displayType, boolean attachToGroup) {
      if (currMode != Mode.DRAWING) {
        throw new IllegalArgumentException();
      }
      this.currMode = currMode;
      this.name = name;
      this.addNodes = addNodes;
      this.displayType = displayType;
      this.attachToGroup = attachToGroup; 
      buildState = EMPTY;
      points = new ArrayList<Point>();
      nodes = new ArrayList<String>();
      allRects = new ArrayList<Rectangle2D>();
      keepGoing = (displayType != NetModuleProperties.CONTIG_RECT);
    }
    
    //
    // This is the version for ADDING elements to a module
    //
    
    public CommonNetModuleCandidate(Mode currMode, String netModuleID, int displayType, String attachedGroup, Color currCol) {
      if (currMode != Mode.ADDING) {
        throw new IllegalArgumentException();
      }
      this.currMode = currMode;
      this.netModuleID = netModuleID;
      this.displayType = displayType;
      this.attachedGroup = attachedGroup;
      this.color = currCol;
      buildState = EMPTY;
      points = new ArrayList<Point>();
      nodes = new ArrayList<String>();
      allRects = new ArrayList<Rectangle2D>(); 
    }
    
    public void changeToAdd() {
      if (!keepGoing) {
        throw new IllegalStateException();
      }
      currMode = Mode.ADDING;
      buildState = EMPTY;
      points.clear();
      nodes.clear();
      allRects.clear();
    }  
    
    public void reset() {
      buildState = EMPTY;
      points.clear();
      nodes.clear();
      allRects.clear();
    }  

    public Rectangle2D getCurrentRect(int x, int y) {
      if (points.size() == 0) {
        return (null);
      }
      Point pt = points.get(0);
      int ulx = pt.x;
      int uly = pt.y;
      int lrx = x;
      int lry = y;
      if (ulx > lrx) {
        ulx = x;
        lrx = pt.x;
      }
      if (uly > lry) {
        uly = y;
        lry = pt.y;        
      }
     
      Rectangle2D retval = new Rectangle2D.Double(ulx, uly, lrx - ulx, lry - uly);
      return (retval);
    }
  }
}
