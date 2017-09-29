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

package org.systemsbiology.biotapestry.cmd.flow.move;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.IntersectionChooser;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NoteProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle moving tasks
*/

public class Mover extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum Action {
    GROUP("groupPopup.MoveGroup", "groupPopup.MoveGroup", "FIXME24.gif", "groupPopup.MoveGroupMnem", null),
    NUDGE_UP(),
    NUDGE_DOWN(),
    NUDGE_LEFT(),
    NUDGE_RIGHT(),
    MODULES(),
    MOVE_MODEL_ELEMS(),
    ;
  
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    Action(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    } 
    
    Action() {
    } 
       
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }
    
    public boolean isANudge() {
      return ((this == Action.NUDGE_UP) || 
              (this == Action.NUDGE_DOWN) || 
              (this == Action.NUDGE_LEFT) || 
              (this == Action.NUDGE_RIGHT));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private Action action_;
  private MoveNetModuleRegionArgs regArgs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Mover(BTState appState, Action action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Mover(BTState appState, Action action, MoveNetModuleRegionArgs args) {
    super(appState);
    name =  args.getName();
    desc =  args.getName();
    mnem =  args.getMnem();
    action_ = action;
    regArgs_ = args;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Helper for getting running move
  */ 
      
  public static RunningMove generateRMov(GenomePresentation gPre, DataAccessContext rcx, Point2D start) { 
    List<RunningMove> newMoves = (new RunningMoveGenerator(gPre)).getRunningMove(start, rcx);
    return ((new IntersectionChooser(false, rcx)).runningMoveRanker(newMoves));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, action_, regArgs_, dacx);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) { 
    if (action_ == Action.GROUP) {
      return (rcx.getGenomeAsInstance().getVfgParent() == null);
    } else if (action_ == Action.MODULES) {
      return (regArgs_.getIsEnabled());
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        if (action_.isANudge()) {
          ans = new StepState(appState_, action_, regArgs_, cfh.getDataAccessContext());
        } else {
          throw new IllegalStateException();
        }
      } else {
        ans = (StepState)last.currStateX;
      }  
      if (ans.getNextStep().equals("stepSetToMode")) {
        next = ans.stepSetToMode();      
      } else if (ans.getNextStep().equals("stepPlaceGroup")) {
        next = ans.stepPlaceGroup();     
      } else if (ans.getNextStep().equals("stepMoveElems")) {
        next = ans.stepMoveElems();     
      } else if (ans.getNextStep().equals("stepPlaceModule")) {
        next = ans.stepPlaceModule();     
      } else if (ans.getNextStep().equals("nudgeSelected")) {
        next = ans.nudgeSelected();               
      } else {
        throw new IllegalStateException();
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
    // ISSUE #216: Previously, we forced the incoming value to the grid right here. But the final delta is forced to
    // the grid in the final placement step. This double-force causes rounding differences between the visualization and the 
    // placement. Keep the raw position up until the final step.
    ans.x = theClick.x; //UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = theClick.y; //UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    if (action_ == Action.MODULES) {
      ans.nextStep_ = "stepPlaceModule" ;
    } else if (action_ == Action.MOVE_MODEL_ELEMS) { // Not using click handling just yet...
      throw new IllegalStateException(); 
    } else {
      ans.nextStep_ = "stepPlaceGroup"; 
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState implements DialogAndInProcessCmd.AbsolutePopupPointCmdState, DialogAndInProcessCmd.MouseClickCmdState {
    
    private Intersection intersect_;
    private DataAccessContext rcxT_;
    private Action myAction_;
    private String nextStep_;    
    private BTState appState_;
    private int x;
    private int y;
    private RunningMove[] multiMov_;
    private Point multiMovStartPt_; 
    private Point2D popupPoint_;
    private Point absScreen_;
    private MoveNetModuleRegionArgs myRegArgs_;
    private RunningMove rmov_; 
    private Point2D end_;
    private Point2D start_;
    private PanelCommands pc_; // BOGUS HACK!
         
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, Action myAction, MoveNetModuleRegionArgs regArgs, DataAccessContext dacx) {
      appState_ = appState;
      myAction_ = myAction;
      myRegArgs_ = regArgs;
      rcxT_ = dacx;
      if (myAction_.isANudge()) {
        nextStep_ = "nudgeSelected" ;
      } else if (myAction_ == Action.MOVE_MODEL_ELEMS) {
        nextStep_ = "stepMoveElems";
      } else {
        nextStep_ = "stepSetToMode"; 
      }
    }
    
    /***************************************************************************
    **
    ** mouse masking  if (action_ == Action.GROUP) {
      Database db = appState_.getDB();
      String genomeKey = appState_.getGenome();
      GenomeInstance gi = (GenomeInstance)db.getGenome(genomeKey);
      return (gi.getVfgParent() == null);
    } else if (action_ == Action.MODULES) {
      return (regArgs_.isEnabled);
    }
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      if (myAction_ == Action.GROUP) {
        return (true);
      } else {
        return (false);
      }
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      if (myAction_ == Action.GROUP) {
        return (true);
      } else {
        return (false);
      }
    }  
    
    public boolean mustBeDynamic() {
      return (false);
    }
     
    public boolean cannotBeDynamic() {
      if (myAction_ == Action.GROUP) {
        return (true);
      } else {
        return (false);
      }
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
    ** Set the popup params
    */ 
        
    public void setIntersection(Intersection inter) {
      intersect_ = inter;
      return;
    }

    /***************************************************************************
    **
    ** Set the popup params
    */ 
    
    public void setPopupPoint(Point2D point) {
      popupPoint_ = point;
      return;
    }
  
    /***************************************************************************
    **
    ** Set the popup params
    */ 
    
    public void setAbsolutePoint(Point point) {
      absScreen_ = point;
      return;
    }
    
    /***************************************************************************
    **
    ** Get the running move
    */ 
 
    public  RunningMove[] getMultiMov() {
      return (multiMov_);
    }

    /***************************************************************************
    **
    ** Set params for drag moving. Last param is a bogus hack
    */ 
        
    public void setPreload(RunningMove rmov, Point2D end, PanelCommands pc) {
      rmov_ = rmov;
      end_ = end;
      pc_ = pc;
      return;
    }    
    
    /***************************************************************************
    **
    ** Set params for drag moving from the web. 
    */ 
        
    public void setPreload(Point2D start, Point2D end, double pixDiam) {
      rcxT_.pixDiam = pixDiam;
      end_ = new Point2D.Double();
      UiUtil.forceToGrid((int)end.getX(), (int)end.getY(), end_, UiUtil.GRID_SIZE);
      start_ = new Point2D.Double();
      UiUtil.forceToGrid((int)start.getX(), (int)start.getY(), start_, UiUtil.GRID_SIZE);
      List<RunningMove> newMoves = (new RunningMoveGenerator(appState_.getGenomePresentation())).getRunningMove(start_, rcxT_);
      rmov_ = (new IntersectionChooser(false, rcxT_)).runningMoveRanker(newMoves);
    }       

    /***************************************************************************
    **
    ** Nudge selected items
    */
    
    private DialogAndInProcessCmd nudgeSelected() {
      if (!appState_.getCommonView().canAccept()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
      double dx = 0.0;
      double dy = 0.0;
      
      switch (myAction_) {
        case NUDGE_UP:
          dx = 0.0;
          dy = -10.0;
          break;
        case NUDGE_DOWN:
          dx = 0.0;
          dy = 10.0;
          break;
        case NUDGE_LEFT:
          dx = -10.0;
          dy = 0.0;
          break;
        case NUDGE_RIGHT:
          dx = 10.0;
          dy = 0.0;
          break;
        default:
          throw new IllegalArgumentException();
      }
  
      Layout.PadNeedsForLayout padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
      Layout.PropChange[] pca = nudgeSelectedItems(appState_.getGenomePresentation(), dx, dy, rcxT_, padFixups);
      if (pca != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.nudgeSelected");
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, pca);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE)); 
        Map<String, Boolean> orpho = rcxT_.getLayout().orphansOnlyForAll(false);
        Layout.PropChange[] padLpc = rcxT_.getLayout().repairAllNetModuleLinkPadRequirements(rcxT_, padFixups, orpho);
        if ((padLpc != null) && (padLpc.length != 0)) {
          PropChangeCmd padC = new PropChangeCmd(appState_, rcxT_, padLpc);
          support.addEdit(padC);
        }      
        support.finish();
      }    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
       
    /***************************************************************************
    **
    ** Guts of move operation
    */
      
    private static Layout.PropChange moveCore(Point2D start, double dx, double dy, Intersection selection,
                                              DataAccessContext rcxO, Layout.PadNeedsForLayout padNeeds) {
         
      //
      // Get the selection and move it.  Use the parent genome if we are a VFN, 
      // unless we are moving a note:
      //
 
      // FIX ME?? Same problem as group selection below??
      NoteProperties ntp = rcxO.getLayout().getNoteProperties(selection.getObjectID());
      if (ntp != null) {
        return (rcxO.getLayout().moveNote(selection.getObjectID(), rcxO.getGenome(), dx, dy));
      }
      Genome useGenome = rcxO.getGenome();
      
      if (useGenome instanceof GenomeInstance) {
        if (((GenomeInstance)useGenome).getGroup(selection.getObjectID()) != null) {
          GenomeInstance parentRoot = ((GenomeInstance)useGenome).getVfgParentRoot();
          if (parentRoot != null) {
            useGenome = parentRoot;
          }
          String groupID = Intersection.getLabelID(selection);
          if (groupID != null) {
            groupID = Group.getBaseID(groupID);
            GroupProperties grp = rcxO.getLayout().getGroupProperties(groupID);
            if (grp != null) {
              return (rcxO.getLayout().moveGroup(groupID, dx, dy));
            }
          }
        }
      }

      Node node = useGenome.getNode(selection.getObjectID());
      DataAccessContext rcxU = new DataAccessContext(rcxO, useGenome, rcxO.getLayout());
      if (node != null) {      
        return (rcxU.getLayout().moveNode(selection.getObjectID(), dx, dy, padNeeds, rcxU));
      }
      Linkage link = useGenome.getLinkage(selection.getObjectID());
      if (link != null) {        
        LinkProperties lp = rcxU.getLayout().getLinkProperties(link.getID());       
        LinkSegmentID[] segID = selection.segmentIDsFromIntersect();
        if (segID == null) {
          selection = Intersection.fullIntersection(link, rcxU, true);
          segID = selection.segmentIDsFromIntersect();
        }  
        return (rcxU.getLayout().moveBusLink(segID, dx, dy, start, (BusProperties)lp));
      }
      
      return (null);
    }
    
   /***************************************************************************
   **
   ** Move the selected item
   */
     
    public static Layout.PropChange[] nudgeSelectedItems(GenomePresentation gPre, double dx, double dy,
                                                         DataAccessContext rcx, Layout.PadNeedsForLayout padNeeds) {
  
      //
      // Crank through each item and do the move
      //
      
      Map<String, Intersection> selectionKeys = gPre.getRawSelectionKeys();   
      int size = selectionKeys.size();
      if (size == 0) {
        return (null);
      }
      Intersection[] toMove = selectionKeys.values().toArray(new Intersection[size]);   
      toMove = gPre.selectionReduction(toMove, rcx); 
      size = toMove.length;
      Layout.PropChange[] redos = new Layout.PropChange[size];    
      for (int i = 0; i < size; i++) {
       redos[i] = moveCore(null, dx, dy, toMove[i], rcx, padNeeds);
      }                                  
      return (redos);
    }  
     
    /***************************************************************************
    **
    ** Move the selected item
    */
    
    public static Layout.PropChange[] moveItem(RunningMove mov, Point2D end, DataAccessContext rcxO, Layout.PadNeedsForLayout padNeeds) {
      //
      // Current group move semantics.  If we are already selected, then move all selected items
      // as a group.  If not, we just move the current item.  This might change to having the
      // drag click adopt selection semantics (see above).
      //
       
      // ISSUE #216 This is where we get the final grid forcing, by insuring a gridded delta is added to
      // the existing gridded position. 
      double dx = end.getX() - mov.start.getX();
      double dy = end.getY() - mov.start.getY();    
      dx = Math.round(dx / 10.0) * 10.0;
      dy = Math.round(dy / 10.0) * 10.0;


      switch (mov.type) {
        case LINK_LABEL:
          BusProperties lp = rcxO.getLayout().getLinkProperties(mov.linkID);
          Layout.PropChange[] pc = new Layout.PropChange[1];
          pc[0] = rcxO.getLayout().moveLinkLabel(lp, dx, dy);
          return (pc);
        case MODEL_DATA:
          Layout.PropChange[] lpc = new Layout.PropChange[1];
          lpc[0] = rcxO.getLayout().moveDataLocation(mov.modelKey, rcxO.getGenome(), dx, dy);
          return (lpc);
        case INTERSECTIONS:
          int size = mov.toMove.length;
          Layout.PropChange[] redos = new Layout.PropChange[size];
          for (int i = 0; i < size; i++) {
            redos[i] = moveCore(mov.start, dx, dy, mov.toMove[i], rcxO, padNeeds);
          }                                  
          return (redos);
        case NET_MODULE_NAME:
          Layout.PropChange[] pcn = rcxO.getLayout().moveNetModuleName(mov.modIntersect, mov.ovrId, dx, dy);
          return (pcn);
        case NET_MODULE_LINK:
          Layout.PropChange[] pc2 = new Layout.PropChange[1];
          pc2[0] = rcxO.getLayout().moveNetModuleLinks(mov.modIntersect, mov.ovrId, dx, dy, rcxO);
          return (pc2);              
        case NET_MODULE_EDGE:
          NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)mov.modIntersect.getSubID();
          if (ei.contigInfo != null) {
            ei.contigInfo.intersectedShape = null;
          }
          // Yes we fall thru!
        case NET_MODULE_WHOLE:
          return (rcxO.getLayout().moveNetModule(rcxO.getGenomeID(), mov.modIntersect, mov.ovrId, dx, dy, padNeeds, rcxO));
        default:
          throw new IllegalArgumentException();
      }
    }

    /***************************************************************************
    **
    ** Handle mouse motion
    */     
    
    public void handleMouseMotion(Point pt, DataAccessContext rcxO) {

      if (multiMov_ != null) {
        // Make the first cursor pos after we start the start point
        if (multiMovStartPt_ == null) {
          for (int i = 0; i < multiMov_.length; i++) {
            multiMov_[i].resetStart(pt);
          }
          multiMovStartPt_ = pt;
        }
        RunningMove.PadFixup needFixups = RunningMoveGenerator.needPadFixupsForMoves(multiMov_);
        Layout.PadNeedsForLayout padFixups = null;
        if (needFixups != RunningMove.PadFixup.NO_PAD_FIXUP) {
          padFixups = rcxO.getLayout().findAllNetModuleLinkPadRequirementsForOverlay(rcxO);
        }
        Layout multiMoveLayout = new Layout(rcxO.getLayout());
        DataAccessContext rcxM = new DataAccessContext(rcxO);
        rcxM.setLayout(multiMoveLayout);
        appState_.getSUPanel().setMultiMoveLayout(multiMoveLayout); 
        for (int i = 0; i < multiMov_.length; i++) {
          moveItem(multiMov_[i], pt, rcxM, padFixups);
        }
        if (padFixups != null) {
          Map<String, Boolean> orpho = multiMoveLayout.orphansOnlyForAll(false);
          multiMoveLayout.repairAllNetModuleLinkPadRequirements(rcxM, padFixups, orpho);
        }
      }
      appState_.getSUPanel().drawModel(false);
      return;
    }
    
    /***************************************************************************
    **
    ** Common multi-move completion
    */
    
    private Layout.PropChange[] doMultiMoveFinishCore(int x, int y, Layout.PadNeedsForLayout padFixups) {
      Layout.PropChange[] allLpc = null;
      //
      // Do NOT force to grid here.  Forceing to grid happens INSIDE the moveItem anyway, and forcing
      // here causes a mismatch between the drag state and the final state: a "jump" on the click!
      Point end = new Point(x, y);
     // Point end = new Point();
     // forceToGrid(x, y, end);
      ArrayList<Layout.PropChange[]> allChanges = new ArrayList<Layout.PropChange[]>();
      int numPC = 0;
      if (multiMov_ != null) {
        if (multiMovStartPt_ == null) { // catch weird boundary cases...
          for (int i = 0; i < multiMov_.length; i++) {
             multiMov_[i].resetStart(end);
          }
          multiMovStartPt_ = end;
        }
        for (int i = 0; i < multiMov_.length; i++) {
          Layout.PropChange[] lpc = moveItem(multiMov_[i], end, rcxT_, padFixups);
          if (lpc != null) {
            allChanges.add(lpc);
            numPC += lpc.length;
          }
        }
        multiMov_ = null;
        multiMovStartPt_ = null;
      }
      boolean blank = true;
      if (numPC != 0) {
        allLpc = new Layout.PropChange[numPC];
        int numac = allChanges.size();
        int count = 0;
        for (int j = 0; j < numac; j++) {
          Layout.PropChange[] lpc = allChanges.get(j);
          for (int i = 0; i < lpc.length; i++) {
            if (lpc[i] != null) {
              blank = false;
            }
            allLpc[count++] = lpc[i];
          }
        }
      }
      return ((blank) ? null : allLpc);
    }    
    
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {
      boolean didWarp = false;
      try {
        Robot r = new Robot();
        r.mouseMove(absScreen_.x, absScreen_.y);
        didWarp = true;
      } catch (AWTException awtex) {
        // silent fail...only do this if we are allowed to...
      } catch (SecurityException secex) {
        // silent fail...only do this if we are allowed to...
      }
      Point pt = new Point();
      appState_.getZoomTarget().transformClick(popupPoint_.getX(), popupPoint_.getY(), pt);
      GenomePresentation gPre = appState_.getGenomePresentation();
      if (myAction_ == Action.MODULES) {
        String currentOverlay = appState_.getCurrentOverlay();
        NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)intersect_.getSubID();
        if (ei != null) {
          ei.endPt = null;
          ei.startPt = null;
        }
        multiMov_ = new RunningMoveGenerator(gPre).getRunningMovesForNetModule(intersect_, pt, rcxT_, 
                                                                               currentOverlay, myRegArgs_.getMoveGuts(), 
                                                                               myRegArgs_.getMoveMode());
      } else if (myAction_ == Action.GROUP) {  
        multiMov_ =  new RunningMoveGenerator(gPre).getRunningMovesForGroup(pt, rcxT_, intersect_.getObjectID());
      }
      multiMovStartPt_ = (didWarp) ? (Point)pt.clone() : null; 
      
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = (myAction_ == Action.MODULES) ? PanelCommands.Mode.MOVE_NET_MODULE : PanelCommands.Mode.MOVE_GROUP;
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the step for placing a group
    */ 
     
    private DialogAndInProcessCmd stepMoveElems() {       
      
      if (rmov_ == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }    
      if (rmov_.notPermitted) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
         
      boolean retval = false;
      Layout.PropChange[] lpc = null;
      
      Layout.PadNeedsForLayout padFixups = null;  
      RunningMove.PadFixup needFixups = RunningMoveGenerator.needPadFixupsForMove(rmov_);
      if (needFixups == RunningMove.PadFixup.PAD_FIXUP_FULL_LAYOUT) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
      } else if (needFixups == RunningMove.PadFixup.PAD_FIXUP_THIS_OVERLAY) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirementsForOverlay(rcxT_);
      }
      lpc = Mover.StepState.moveItem(rmov_, end_, rcxT_, padFixups);
      if (lpc != null) {
        retval = true;
        for (int i = 0; i < lpc.length; i++) {
          if (lpc[i] != null) {
            UndoSupport support = new UndoSupport(appState_, "undo.moveItem");
            PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, lpc);
            support.addEdit(mov);
            support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
            if (padFixups != null) {
              Map<String, Boolean> orpho = rcxT_.getLayout().orphansOnlyForAll(false);
              Layout.PropChange[] padLpc = rcxT_.getLayout().repairAllNetModuleLinkPadRequirements(rcxT_, padFixups, orpho);
              if ((padLpc != null) && (padLpc.length != 0)) {
                PropChangeCmd padC = new PropChangeCmd(appState_, rcxT_, padLpc);
                support.addEdit(padC);
              }
            }
            support.finish();
            if (pc_ != null) {
              pc_.setMoveResult(retval);
            }
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
          }
        }
      }
      if (pc_ != null) {
        pc_.setMoveResult(retval);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
     
    /***************************************************************************
    **
    ** Do the step for placing a group
    */ 
     
    private DialogAndInProcessCmd stepPlaceGroup() {    
  
      if (!(rcxT_.getGenome() instanceof GenomeInstance)) {
        throw new IllegalArgumentException();
      }          
      RunningMove.PadFixup needFixups = RunningMoveGenerator.needPadFixupsForMoves(multiMov_);
      Layout.PadNeedsForLayout padFixups = null;
      if (needFixups == RunningMove.PadFixup.PAD_FIXUP_THIS_OVERLAY) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirementsForOverlay(rcxT_);
      } else if (needFixups == RunningMove.PadFixup.PAD_FIXUP_FULL_LAYOUT) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
      }
      Layout.PropChange[] allLpc = doMultiMoveFinishCore(x, y, padFixups);
      if (allLpc != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.moveGroup");
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, allLpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        Map<String, Boolean> orpho = rcxT_.getLayout().orphansOnlyForAll(false);
        Layout.PropChange[] padLpc = rcxT_.getLayout().repairAllNetModuleLinkPadRequirements(rcxT_, padFixups, orpho);
        if ((padLpc != null) && (padLpc.length != 0)) {
          PropChangeCmd padC = new PropChangeCmd(appState_, rcxT_, padLpc);
          support.addEdit(padC);
        }
        support.finish();
      }
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }
    
    /***************************************************************************
    **
    ** Do the step for placing a netModule
    */ 
     
    private DialogAndInProcessCmd stepPlaceModule() {
      RunningMove.PadFixup needFixups = RunningMoveGenerator.needPadFixupsForMoves(multiMov_);
      Layout.PadNeedsForLayout padFixups = null;
      if (needFixups == RunningMove.PadFixup.PAD_FIXUP_THIS_OVERLAY) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirementsForOverlay(rcxT_);
      } else if (needFixups == RunningMove.PadFixup.PAD_FIXUP_FULL_LAYOUT) {
        padFixups = rcxT_.getLayout().findAllNetModuleLinkPadRequirements(rcxT_);
      }
      Layout.PropChange[] allLpc = doMultiMoveFinishCore(x, y, padFixups);
      if (allLpc != null) {
        UndoSupport support = new UndoSupport(appState_, "undo.moveNetModule");
        PropChangeCmd mov = new PropChangeCmd(appState_, rcxT_, allLpc);
        support.addEdit(mov);
        support.addEvent(new LayoutChangeEvent(rcxT_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
        if (padFixups != null) {
          Map<String, Boolean> orpho = rcxT_.getLayout().orphansOnlyForAll(false);
          Layout.PropChange[] padLpc = rcxT_.getLayout().repairAllNetModuleLinkPadRequirements(rcxT_, padFixups, orpho);
          if ((padLpc != null) && (padLpc.length != 0)) {
            PropChangeCmd padC = new PropChangeCmd(appState_, rcxT_, padLpc);
            support.addEdit(padC);
          }
        }
        support.finish();
      }  
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    } 
  }

  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class MoveNetModuleRegionArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    private static Class<?>[] classes_;
    
    static {
      keys_ = new String[] {"moveGuts", "moveMode", "name", "mnem", "isEnabled"};  
      classes_ = new Class<?>[] {Boolean.class, Integer.class, String.class, String.class, Boolean.class};  
    }
    
    public boolean getMoveGuts() {
      return (Boolean.valueOf(getValue(0)));
    }
     
    public int getMoveMode() {
      return (Integer.parseInt(getValue(1)));
    }
  
    public String getName() {
      return (getValue(2));
    }
    
    public String getMnem() {
      return (getValue(3));
    }
          
    public boolean getIsEnabled() {
      return (Boolean.valueOf(getValue(4)));
    }
    
    public MoveNetModuleRegionArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    @Override
    protected Class<?>[] getClasses() {
      return (classes_);
    }
    
    public MoveNetModuleRegionArgs(boolean moveGuts, int moveMode, String name, String mnem, boolean isEnabled) {
      super();
      setValue(0, Boolean.toString(moveGuts));
      setValue(1, Integer.toString(moveMode));
      setValue(2, name);
      setValue(3, mnem);
      setValue(4, Boolean.toString(isEnabled));
      bundle();
    }
  }
}
