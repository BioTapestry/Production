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

package org.systemsbiology.biotapestry.cmd.flow.modelTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.NavigationChange;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.app.VirtualTimeSlider;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.view.Zoom;
import org.systemsbiology.biotapestry.cmd.undo.NavigationChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
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
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
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
                             GROUP_NODE_SELECTION_ONLY,
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
  private BTState appState_;
  
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
    appState_ = appState;
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(action_, dacx);
    retval.setAppState(appState_, retval.getDACX());
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
        ans = new StepState(action_, cfh);
        ans.setAppState(appState_, ans.getDACX());
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
        
  public static class StepState extends AbstractStepState {

    private SettingAction myAction_;
    private boolean doingUndo_;
    private int value_;
    private boolean doSelectionIgnore_;
    private TreePath lastPath_;
    private TreePath currPath_;  
    private int lastSettingToUse_;
    private String currDip_; 
    private StartupView suv_;
    private String modelID_;
    private String nodeID_;
    private Set<String> nodes_;
    private Set<String> links_;
    private String overlayId_;
    private TaggedSet modules_;
    private TaggedSet revealedModules_;
    private List<Intersection> linkIntersections_;
    private String regionID_;
    private DynamicDataAccessContext ddacx_;
    private BTState appState_;
      
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(SettingAction action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** We have to use appState for the moment...
    */ 
    
    public void setAppState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      // We ignore the static context we are being handed, and use what we are provided here!
      ddacx_ = new DynamicDataAccessContext(appState_);
      if (uics_ == null) {
        BTState.AppSources asrc = appState_.getAppSources();
        uics_ = asrc.uics;
        tSrc_ = asrc.tSrc;
        uFac_ = asrc.uFac;
      }  
      return;
    }

    /***************************************************************************
    **
    ** for preload. We are actually getting used outside normal flows, so we need extra info
    ** that would come from the harness
    */ 
        
    public void setPreloadForTreeClick(TreePath currPath, TreePath lastPath, 
                                       boolean doSelectionIgnore, boolean doingUndo, 
                                       UIComponentSource uics, TabSource tSrc, UndoFactory uFac) {

      uics_ = uics;
      tSrc_ = tSrc;
      uFac_ = uFac;
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
        
    public void setPreloadForSlider(int value, int lastSettingToUse, boolean doingUndo, String currDip,  
                                    UIComponentSource uics, TabSource tSrc, UndoFactory uFac) {      
      uics_ = uics;
      tSrc_ = tSrc;
      uFac_ = uFac;
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
    	this.setPreload(modelID,nodes,links,null);
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
   
    public void setPreloadForGroup(String modelID, String regionID) {
      nodeID_ = null;
      modelID_ = modelID;
      nodes_ = null;
      links_ = null;
      regionID_ = regionID;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */
    
    public void setPreload(String modelID, Set<String> nodes, Set<String> links, String nodeId) {
    	nodeID_ = nodeId;
      modelID_ = modelID;
      nodes_ = nodes;
      links_ = links;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */
    
    public void setPreload(String modelID, Set<String> nodes, Set<String> links, String overlayId, TaggedSet mods, TaggedSet revealedModules) {
      this.setPreload(modelID,nodes,links);
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
      SUPanel sup = uics_.getSUPanel();
      DisplayOptions dop = ddacx_.getDisplayOptsSource().getDisplayOptions();
      DisplayOptions.NavZoom navZoomMode = dop.getNavZoomMode();
      String oldLayoutID = ddacx_.getCurrentLayoutID();
  
      String layoutID = ddacx_.getLayoutSource().mapGenomeKeyToLayoutKey(genomeID); 
      Genome currGenome = ddacx_.getGenomeSource().getGenome(genomeID);
      if (currGenome == null) {
        System.err.println("No genome: " + genomeID);
        throw new IllegalStateException();
      }
      appState_.setGraphLayout(layoutID);
      if ((!doingUndo_) && (support != null)) {
        // This also handles netOverlayController tracking!
        appState_.setGenome(genomeID, support, ddacx_);
      } else {
        appState_.setGenomeForUndo(genomeID, ddacx_);
      }
      if (navZoomMode == DisplayOptions.NavZoom.NAV_ZOOM_TO_EACH_MODEL) {
        if ((oldLayoutID == null) || !oldLayoutID.equals(layoutID)) {
          uics_.getZoomCommandSupport().zoomToModel();
        }  
      }
      sup.drawModel(true);      
      uics_.getCommonView().updateDisplayForGenome(currGenome);
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
        case GROUP_NODE_SELECTION_ONLY:
        	return (stepToProcessGroupNode());
        case MODEL_AND_LINK_SELECTIONS: 
        case MODEL_AND_NODE_SELECTIONS:    
        case MODEL_AND_SELECTIONS:
          StaticDataAccessContext rcx = new StaticDataAccessContext(appState_, modelID_);
          return (stepToProcessModelAndSelections(rcx));
        case FOR_EVENT:
        default:
          throw new IllegalStateException();
      }
    }  
    
    /***************************************************************************
    **
    ** Do the step
    */ 
    
    public DialogAndInProcessCmd stepToProcessGroupNode() {
    	
    	suv_ = new StartupView(null, null, null, null, null, NavTree.KidSuperType.GROUP, nodeID_);
    	installStartupView(uics_, ddacx_, false, suv_);  
    	
    	return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do the step. NOTE THIS IS PUBLIC. It is getting called independently for now, to drive tree selection.
    ** As such, it needs stuff pre-stocked (e.g. uics_, tSrc_) that is usually handled by the harness!
    */ 
 
    public DialogAndInProcessCmd stepToProcessTree() {   
        
      //
      // Previous handler installed on CommonView
      //
      
      VirtualTimeSlider vtSlider = uics_.getVTSlider();
      NavTree nt = ddacx_.getGenomeSource().getModelHierarchy();
      
      //
      // Undo/Redo support
      //
      UserTreePathController.PathNavigationInfo pni = uics_.getPathController().getPendingNavUndo();
      UndoSupport taSupport = tSrc_.getTabAppendUndoSupport();
      UndoSupport support;
      if (pni != null) {
        support = pni.support;
      } else if (taSupport != null) {
        support = taSupport;
      } else {
        support = uFac_.provideUndoSupport("undo.treeSelection", ddacx_);
      }
      
      String genomeID = nt.getGenomeID(currPath_);
      String proxyID = nt.getDynamicProxyID(currPath_);
      String groupNodeID = nt.getGroupNodeID(currPath_);
      uics_.getCommonView().showGroupNode((groupNodeID != null));
      appState_.setGroupNodeSelected((groupNodeID != null));     
      if (genomeID != null) {
        vtSlider.installEmptySlider();
        coreSwitchingSupport(genomeID, support);
      } else if (proxyID != null) {
        vtSlider.manageSliderForProxy(proxyID);
        genomeID = ddacx_.getGenomeSource().getDynamicProxy(proxyID).getKeyForTime(vtSlider.getLastSliderSetting(), true);
        coreSwitchingSupport(genomeID, support);
      } else {     
        if (!this.doSelectionIgnore_) {
          SUPanel sup = uics_.getSUPanel();
          vtSlider.installEmptySlider();
          appState_.setGraphLayout(null);
          if (!doingUndo_) {
            appState_.setGenome(genomeID, support, ddacx_);
          } else {
            appState_.setGenomeForUndo(genomeID, ddacx_);
          }
          sup.drawModel(true);
        }
      }
      //
      // Don't post an undo if this selection change is the result of
      // an undo:
      //
      
      if (!doingUndo_ && !nt.amDoingUndo()) {
        if (nt.getSkipFlag() != NavTree.Skips.SKIP_EVENT) {
          NavigationChange nc = (pni == null) ? new NavigationChange() : pni.nav;
          if (pni == null) { 
            uics_.getPathController().syncPathControls(nc, ddacx_); 
          }
          nc.newPath = currPath_;
          nc.oldPath = lastPath_;
          //lastPath_ = tp;
          support.addEdit(new NavigationChangeCmd(ddacx_, nc));
          if (nt.getSkipFlag() != NavTree.Skips.SKIP_FINISH) { 
            if (pni != null) {
              pni.finish();
            } else if (taSupport != null) {
              support = taSupport;
            } else {
              support.finish();
            }
          }
        }
      } 
      SelectionChangeEvent ev = new SelectionChangeEvent(genomeID, null, SelectionChangeEvent.SELECTED_MODEL);
      uics_.getEventMgr().sendSelectionChangeEvent(ev);
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

      UserTreePathController.PathNavigationInfo pni = uics_.getPathController().getPendingNavUndo();
      UndoSupport support = (pni == null) ? uFac_.provideUndoSupport("undo.sliderPos", ddacx_) : pni.support;
      NavigationChange nc = (pni == null) ? new NavigationChange() : pni.nav;
      if (!doingUndo_) {
        nc.oldHour = lastSettingToUse_;
      }

      GenomeSource db = ddacx_.getGenomeSource();    
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
          uics_.getPathController().syncPathControls(nc, ddacx_); 
        }
        nc.newHour = value_;
        nc.sliderID = dip.getID();
        support.addEdit(new NavigationChangeCmd(ddacx_, nc));
        if (pni == null) {
          support.finish();
        } else {
          pni.finish();
        }
      }      

      SelectionChangeEvent ev = new SelectionChangeEvent(genomeID, null, SelectionChangeEvent.SELECTED_MODEL);
      uics_.getEventMgr().sendSelectionChangeEvent(ev);    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }

    /***************************************************************************
    **
    ** Install the settings in the tree
    ** FIXME: This refactoring is incomplete. Previously tagged as "MAKE THIS NOT PUBLIC". It's working, but
    ** could be better.
    */
    
    public static void installStartupView(UIComponentSource uics, DynamicDataAccessContext dacx, boolean selectRoot, StartupView sv) {
      TreePath tp;
      int timeVal = -1;
      String modelKey = sv.getModel();
      String nodeKey = sv.getNodeId();
      NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
      if (selectRoot) {
        DefaultTreeModel dtm = uics.getTree().getTreeModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot(); 
        DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
        TreeNode[] tn = rootChild.getPath();
        tp = new TreePath(tn);
      } else if (modelKey == null && nodeKey == null) {
        tp = navTree.getDefaultSelection();
      } else {     
        tp = navTree.getStartupSelection(modelKey != null ? modelKey : nodeKey, dacx);
      }
      
      DynamicInstanceProxy dip = null; 
      if ((modelKey != null) && DynamicInstanceProxy.isDynamicInstance(modelKey)) {
        String proxID = DynamicInstanceProxy.extractProxyID(modelKey);
        GenomeSource db = dacx.getGenomeSource();
        dip = db.getDynamicProxy(proxID);
        if (!dip.isSingle()) {
          DynamicGenomeInstance dgi = dip.getProxiedInstance(modelKey);
          timeVal = dgi.getTime().intValue();
        }
      }
      
      NetOverlayController noc = uics.getNetOverlayController();
      String currModel = dacx.getCurrentGenomeID();
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
      
      
      String currOvr = dacx.getOSO().getCurrentOverlay();
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
      
   
      VirtualModelTree vmt = uics.getTree();
      vmt.setDoNotFlow(true);    
      vmt.setTreeSelectionPath(tp);      // Does refresh and validation   
  
      if (timeVal != -1) {
        VirtualTimeSlider vts = uics.getVTSlider();
        vts.setDoNotFlow(true);
        vts.setSliderForTime(dip, timeVal);
        vts.setDoNotFlow(false);
      }
      vmt.setDoNotFlow(false);
      
      String regID = sv.getRegionId();
      if (regID != null) {
        Zoom.zoomToGroup(uics, regID, dacx);
      }
        
      return;
    }
    
   /***************************************************************************
    **
    ** Go to the specified model
    */
    
    private DialogAndInProcessCmd stepToProcessModel() {
    	if ((overlayId_ == null) && (regionID_ == null)) {
    	  NetOverlayController noc = uics_.getNetOverlayController();
    	  suv_ = noc.getCachedStateForGenome(modelID_, ddacx_);    		
    	} else {
    		suv_ = new StartupView(modelID_, overlayId_, modules_, revealedModules_, regionID_, NavTree.KidSuperType.MODEL, null);
    	}

      installStartupView(uics_, ddacx_, false, suv_);
 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 

    /***************************************************************************
    **
    ** Handle common selection tasks
    ** 
    */
    
    private DialogAndInProcessCmd stepToProcessModelAndSelections(StaticDataAccessContext rcx) {
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
      

      linkIntersections_ = (links_.isEmpty()) ? new ArrayList<Intersection>() 
                                              : rcx.getCurrentLayout().getIntersectionsForLinks(rcx, links_, true);  
      NetOverlayController noc = uics_.getNetOverlayController();
      suv_ = noc.getCachedStateForGenome(modelID_, rcx);
      int obs = NetOverlayController.hasObscuredElements(rcx, suv_);
      if (!nodes_.isEmpty() || !linkIntersections_.isEmpty()) {
        if ((obs & checkProps) != 0x00) {   
          ResourceManager rMan = uics_.getRMan();
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
      suv_ = new StartupView(modelID_, null, null, null, null, NavTree.KidSuperType.MODEL, null);
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
      installStartupView(uics_, ddacx_, false, suv_);
      //
      // FIXME: Probably better to defer this operation to a public method from the selection flow!
      //
      StaticDataAccessContext rcx = new StaticDataAccessContext(appState_, suv_.getModel());
      uics_.getGenomePresentation().selectNodesAndLinks(uics_, nodes_, rcx, linkIntersections_, true, uFac_);
      uics_.getSUPanel().drawModel(false);
      uics_.getZoomCommandSupport().zoomToSelected();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
  }
}
