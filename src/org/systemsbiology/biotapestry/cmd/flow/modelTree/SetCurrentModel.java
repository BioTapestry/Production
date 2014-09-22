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

package org.systemsbiology.biotapestry.cmd.flow.modelTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.NavigationChange;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.app.VirtualTimeSlider;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.NavigationChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle seeting the current model, either via mouse click, slider op, or program call
*/

public class SetCurrentModel extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SettingAction {VIA_MODEL_TREE, 
                             VIA_SLIDER, 
                             FOR_EVENT,
                             MODEL_SELECTION_ONLY,
                             MODEL_AND_LINK_SELECTIONS, 
                             MODEL_AND_NODE_SELECTIONS, 
                             MODEL_AND_SELECTIONS;}

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private SettingAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SetCurrentModel(BTState appState, SettingAction action) {
    super(appState);
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
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
    StepState retval = new StepState(appState_, action_, dacx);
    return (retval);
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
        ans = new StepState(appState_, action_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else if (ans.getNextStep().equals("stepToHandleResponse")) {
        next = ans.stepToHandleResponse(last);
      } else if (ans.getNextStep().equals("stepToFinish")) {
        next = ans.stepToFinish();
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
  ** Running State
  */
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {

    private String nextStep_;
    private SettingAction myAction_;
    private BTState appState_;
    private boolean doingUndo_;
    private int value_;
    private boolean doSelectionIgnore_;
    private TreePath lastPath_;
    private TreePath currPath_;  
    private int lastSettingToUse_;
    private String currDip_; 
    private StartupView suv_;
    private String modelID_;
    private Set<String> nodes_;
    private Set<String> links_;
    private String overlayId_;
    private TaggedSet modules_;
    private TaggedSet revealedModules_;
    private List<Intersection> linkIntersections_;
    private DataAccessContext dacx_;
      
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, SettingAction action, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
     
    /***************************************************************************
    **
    ** for preload
    */ 
        
    public void setPreloadForTreeClick(TreePath currPath, TreePath lastPath, boolean doSelectionIgnore, boolean doingUndo) {
      doingUndo_ = doingUndo;
      doSelectionIgnore_ = doSelectionIgnore;
      currPath_ = currPath;
      lastPath_ = lastPath;    
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
        
    public void setPreloadForSlider(int value, int lastSettingToUse, boolean doingUndo, String currDip) {
      value_ = value;
      lastSettingToUse_ = lastSettingToUse;
      currDip_ = currDip;
      doingUndo_ = doingUndo;
      return;
    }
 
    /***************************************************************************
    **
    ** for preload
    */ 
        
    public void setPreload(String modelID, Set<String> nodes, Set<String> links) {
      modelID_ = modelID;
      nodes_ = nodes;
      links_ = links;
      return;
    }
    
    
    public void setPreload(String modelID, Set<String> nodes, Set<String> links, String overlayId, TaggedSet mods, TaggedSet revealedModules) {
        setPreload(modelID,nodes,links);
        overlayId_ = overlayId;
        modules_ = mods;
        revealedModules_ = revealedModules;
        return;
      }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    public void coreSwitchingSupport(String genomeID, UndoSupport support) {
      SUPanel sup = appState_.getSUPanel();
      DisplayOptions dop = appState_.getDisplayOptMgr().getDisplayOptions();
      DisplayOptions.NavZoom navZoomMode = dop.getNavZoomMode();
      String oldLayoutID = appState_.getLayoutKey();
  
      String layoutID = appState_.getLayoutMgr().getLayout(genomeID); 
      Genome currGenome = appState_.getDB().getGenome(genomeID);
      if (currGenome == null) {
        System.err.println("No genome: " + genomeID);
        throw new IllegalStateException();
      }
      appState_.setGraphLayout(layoutID);
      if ((!doingUndo_) && (support != null)) {
        // This also handles netOverlayController tracking!
        appState_.setGenome(genomeID, support, dacx_);
      } else {
        appState_.setGenomeForUndo(genomeID, dacx_);
      }
      if (navZoomMode == DisplayOptions.NavZoom.NAV_ZOOM_TO_EACH_MODEL) {
        if ((oldLayoutID == null) || !oldLayoutID.equals(layoutID)) {
          appState_.getZoomCommandSupport().zoomToModel();
        }  
      }
      sup.drawModel(true);      
      appState_.getCommonView().updateDisplayForGenome(currGenome);
      return;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    public DialogAndInProcessCmd stepToProcess() {  
      switch (myAction_) {
        case VIA_MODEL_TREE:
          return (stepToProcessTree());
        case VIA_SLIDER:
          return (stepToProcessSlider());
        case MODEL_SELECTION_ONLY:
          return (stepToProcessModel());
        case MODEL_AND_LINK_SELECTIONS: 
        case MODEL_AND_NODE_SELECTIONS:    
        case MODEL_AND_SELECTIONS:
          return (stepToProcessModelAndSelections());
        case FOR_EVENT:
        default:
          throw new IllegalStateException();
      }
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    public DialogAndInProcessCmd stepToProcessTree() {   
        
      //
      // Previous handler installed on CommonView
      //
      
      VirtualTimeSlider vtSlider = appState_.getVTSlider();

      //
      // Undo/Redo support
      //
      UserTreePathController.PathNavigationInfo pni = appState_.getPathController().getPendingNavUndo();
      UndoSupport support = (pni == null) ? new UndoSupport(appState_, "undo.treeSelection") : pni.support;

      Database db = appState_.getDB();
      String genomeID = db.getModelHierarchy().getGenomeID(currPath_);
      String proxyID = db.getModelHierarchy().getDynamicProxyID(currPath_);
      if (genomeID != null) {
        vtSlider.installEmptySlider();
        coreSwitchingSupport(genomeID, support);
      } else if (proxyID != null) {        
        vtSlider.manageSliderForProxy(proxyID);
        genomeID = db.getDynamicProxy(proxyID).getKeyForTime(vtSlider.getLastSliderSetting(), true);
        coreSwitchingSupport(genomeID, support);
      } else {
        if (!this.doSelectionIgnore_) {
          SUPanel sup = appState_.getSUPanel();
          vtSlider.installEmptySlider();
          appState_.setGraphLayout(null);
          if (!doingUndo_) {
            appState_.setGenome(genomeID, support, dacx_);
          } else {
            appState_.setGenomeForUndo(genomeID, dacx_);
          }
          sup.drawModel(true);
        }
      }
      //
      // Don't post an undo if this selection change is the result of
      // an undo:
      //
      NavTree nt = db.getModelHierarchy();
      if (!doingUndo_ && !nt.amDoingUndo()) {
        if (nt.getSkipFlag() != NavTree.SKIP_EVENT) {
          NavigationChange nc = (pni == null) ? new NavigationChange() : pni.nav;
          if (pni == null) { 
            appState_.getPathController().syncPathControls(nc, dacx_); 
          }
          nc.newPath = currPath_;
          nc.oldPath = lastPath_;
          nc.commonView = appState_.getCommonView();
          //lastPath_ = tp;
          support.addEdit(new NavigationChangeCmd(appState_, dacx_, nc));
          if (nt.getSkipFlag() != NavTree.SKIP_FINISH) {
            if (pni == null) {
              support.finish();
            } else {
              pni.finish();
            }
          }
        }
      } 
      SelectionChangeEvent ev = new SelectionChangeEvent(genomeID, null, SelectionChangeEvent.SELECTED_MODEL);
      appState_.getEventMgr().sendSelectionChangeEvent(ev);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcessSlider() {     

      //
      // Undo/Redo support
      //

      UserTreePathController.PathNavigationInfo pni = appState_.getPathController().getPendingNavUndo();
      UndoSupport support = (pni == null) ? new UndoSupport(appState_, "undo.sliderPos") : pni.support;
      NavigationChange nc = (pni == null) ? new NavigationChange() : pni.nav;
      nc.commonView = appState_.getCommonView();
      if (!doingUndo_) {
        nc.oldHour = lastSettingToUse_;
      }

      Database db = appState_.getDB();    
      DynamicInstanceProxy dip = db.getDynamicProxy(currDip_);
      String genomeID = dip.getKeyForTime(value_, true);
  //    lastSettingToUse_ = value_;
      coreSwitchingSupport(genomeID, support);

      //
      // Don't post an undo if this selection change is the result of
      // an undo:
      //

      if (!doingUndo_) {
        if (pni == null) { 
          appState_.getPathController().syncPathControls(nc, dacx_); 
        }
        nc.newHour = value_;
        nc.sliderID = dip.getID();
        support.addEdit(new NavigationChangeCmd(appState_, dacx_, nc));
        if (pni == null) {
          support.finish();
        } else {
          pni.finish();
        }
      }      

      SelectionChangeEvent ev = new SelectionChangeEvent(genomeID, null, SelectionChangeEvent.SELECTED_MODEL);
      appState_.getEventMgr().sendSelectionChangeEvent(ev);    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }

    /***************************************************************************
    **
    ** Install the settings in the tree
    ** FIXME: This refactoring is incomplete. Previously tagged as "MAKE THIS NOT PUBLIC". It's working, but
    ** could be better.
    */
    
    public static void installStartupView(BTState appState, DataAccessContext dacx, boolean selectRoot, StartupView sv) {
      TreePath tp;
      int timeVal = -1;
      String modelKey = sv.getModel();
      NavTree navTree = appState.getDB().getModelHierarchy();
      if (selectRoot) {
        DefaultTreeModel dtm = appState.getTree().getTreeModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot(); 
        DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
        TreeNode[] tn = rootChild.getPath();
        tp = new TreePath(tn);
      } else if (modelKey == null) {
        tp = navTree.getDefaultSelection();
      } else {     
        tp = navTree.getStartupSelection(modelKey, dacx);
      }
      
      DynamicInstanceProxy dip = null; 
      if ((modelKey != null) && DynamicInstanceProxy.isDynamicInstance(modelKey)) {      
        String proxID = DynamicInstanceProxy.extractProxyID(modelKey);
        Database db = appState.getDB();
        dip = db.getDynamicProxy(proxID);
        if (!dip.isSingle()) {
          DynamicGenomeInstance dgi = dip.getProxiedInstance(modelKey);
          timeVal = dgi.getTime().intValue();
        }
      }
      
      NetOverlayController noc = appState.getNetOverlayController();
      String currModel = appState.getGenome();
      boolean diffModel;
      if (currModel == null) {
        diffModel = (modelKey != null);
      } else {
        diffModel = !currModel.equals(modelKey);
      }
      
      //
      // If model key is different, event notification from the tree selection
      // change will handle any overlay change.  If model key is the same,
      // we do the notification directly:
      //
      
      boolean changeInTargOverlay = false;
      if (modelKey != null) {
        StartupView suv = noc.getCachedStateForGenome(modelKey, dacx);
        if (!suv.equals(sv)) {
          changeInTargOverlay = true;
        }
      }  
        
      String ovrKey = sv.getOverlay();
      TaggedSet modKeys = sv.getModules();
      TaggedSet revKeys = sv.getRevealedModules();
      
      
      String currOvr = appState.getCurrentOverlay();
      if ((ovrKey != null) || (currOvr != null) || changeInTargOverlay) {
        if (diffModel) {  
          noc.preloadForNextGenome(modelKey, ovrKey, modKeys, revKeys);     
        } else {
          noc.setFullOverlayState(ovrKey, modKeys, revKeys, null, dacx);
        }
      }
  
      //
      // Going in through the event handlers for the (virtual) UI components.
      // Gotta make sure they do not consider these to be true user-initiated
      // events that need a new control flow!
      //
      
   
      VirtualModelTree vmt = appState.getTree();
      vmt.setDoNotFlow(true);    
      vmt.setTreeSelectionPath(tp);      // Does refresh and validation   
  
      if (timeVal != -1) {
        VirtualTimeSlider vts = appState.getVTSlider();
        vts.setDoNotFlow(true);
        vts.setSliderForTime(dip, timeVal);
        vts.setDoNotFlow(false);
      }
      vmt.setDoNotFlow(false);
      
      return;
    }
    
   /***************************************************************************
    **
    ** Go to the specified model
    */
    
    private DialogAndInProcessCmd stepToProcessModel() {
    	if(overlayId_ == null) {
    	      NetOverlayController noc = appState_.getNetOverlayController();
    	      suv_ = noc.getCachedStateForGenome(modelID_, dacx_);    		
    	} else {
    		suv_ = new StartupView(modelID_,overlayId_,modules_,revealedModules_);
    	}

      installStartupView(appState_, dacx_, false, suv_);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 

    /***************************************************************************
    **
    ** Handle common selection tasks
    ** 
    */
    
    private DialogAndInProcessCmd stepToProcessModelAndSelections() {
      int checkProps;
      String dialogQ;
      String dialogT;
   
      switch (myAction_) {        
        case MODEL_AND_LINK_SELECTIONS: 
          checkProps = NetOverlayController.LINKS_NOT_SHOWN |  NetOverlayController.OPAQUE_OVERLAY;
          dialogQ = "linkUsage.hiding";
          dialogT = "linkUsage.hidingTitle";
          break;        
        case MODEL_AND_NODE_SELECTIONS:  
          checkProps = NetOverlayController.OPAQUE_OVERLAY;
          dialogQ = "nodeUsage.hiding";
          dialogT = "nodeUsage.hidingTitle";
          break;
        case MODEL_AND_SELECTIONS:
          dialogQ = "externalSelect.hiding";
          dialogT = "externalSelect.hidingTitle";
          checkProps = NetOverlayController.OPAQUE_OVERLAY;
          if (!links_.isEmpty()) {
            checkProps |= NetOverlayController.LINKS_NOT_SHOWN;
          }
          break;
        case FOR_EVENT:
        case VIA_MODEL_TREE:
        case VIA_SLIDER:      
        default:
          throw new IllegalStateException();
      }
      
      Genome genome = appState_.getDB().getGenome(modelID_);
      Layout lo = appState_.getLayoutForGenomeKey(modelID_);
      
      DataAccessContext rcx = new DataAccessContext(appState_, genome, lo);
      linkIntersections_ = (links_.isEmpty()) ? new ArrayList<Intersection>() 
                                              : lo.getIntersectionsForLinks(rcx, links_, true);  
      NetOverlayController noc = appState_.getNetOverlayController();
      suv_ = noc.getCachedStateForGenome(modelID_, rcx);
      int obs = NetOverlayController.hasObscuredElements(rcx, suv_);
      if (!nodes_.isEmpty() || !linkIntersections_.isEmpty()) {
        if ((obs & checkProps) != 0x00) {   
          ResourceManager rMan = appState_.getRMan();
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, rMan.getString(dialogQ), rMan.getString(dialogT));
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
          nextStep_ = "stepToHandleResponse";
          return (retval);      
        }
      } 
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "stepToFinish";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Collect up the viz answer, and change viz if user indicates
    */   
  
    private DialogAndInProcessCmd stepToHandleResponse(DialogAndInProcessCmd daipc) {   
      int choiceVal = daipc.suf.getIntegerResult();     
      if (choiceVal == SimpleUserFeedback.NO) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
       return (retval);
       }
      suv_ = new StartupView(modelID_, null, null, null);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "stepToFinish";
      return (retval);
    }
    
   /***************************************************************************
   **
   ** Finish the job
   */   
  
    private DialogAndInProcessCmd stepToFinish() {   
      //
      // This call actually gets routed to the above slider and tree calls through the event handling of the virtual tree and slider objects!
      // Pretty bogus, huh?
      //
      installStartupView(appState_, dacx_, false, suv_);
      //
      // FIXME: Probably better to defer this operation to a public method from the selection flow!
      //
      DataAccessContext rcx = new DataAccessContext(appState_, suv_.getModel());
      appState_.getGenomePresentation().selectNodesAndLinks(nodes_, rcx, linkIntersections_, true, appState_.getUndoManager());
      appState_.getSUPanel().drawModel(false);
      appState_.getZoomCommandSupport().zoomToSelected();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
  }
}
