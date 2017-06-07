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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.MessageTableReportingDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.ui.layouts.NetModuleLinkExtractor;
import org.systemsbiology.biotapestry.ui.layouts.OverlayLayoutAnalyzer;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutData;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParamDialogFactory;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle expand and compress layout
*/

public class SpecialtyLayoutFlow extends AbstractControlFlow  {

  public static final int LAYOUT_ALL = 0;
  public static final int LAYOUT_SELECTION = 1;
  public static final int LAYOUT_PER_OVERLAY = 2;
  public static final int LAYOUT_REGION = 3; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private int mode_;
  private String undoStr_;
  private SpecialtyLayout acfSpecL_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SpecialtyLayoutFlow(SpecialtyLayout specL, int mode) {
    mode_ = mode;
    acfSpecL_ = specL;
    switch (mode) {
      case LAYOUT_ALL:
        name = specL.getMenuNameTag();
        desc = specL.getMenuNameTag();
        mnem =  specL.getMenuMnemonicTag();
        undoStr_ = null;
        break;
      case LAYOUT_SELECTION:
        name = "command.LayoutSelected";
        desc = "command.LayoutSelected";
        mnem = "command.LayoutSelectedMnem";
        undoStr_ = "undo.layoutSelected";
        break;
      case LAYOUT_PER_OVERLAY:
        name = "command.LayoutPerOverlay";
        desc = "command.LayoutSelected";
        mnem = "command.LayoutPerOverlayMnem";
        undoStr_ = "undo.layoutPerOverlay";
        break;
      case LAYOUT_REGION:
        name = "groupPopup.LayoutGroup";
        desc = "groupPopup.LayoutGroup";
        mnem = "groupPopup.LayoutGroupMnem";
        undoStr_ = "undo.layoutRegion";
        break;
      default:
        throw new IllegalArgumentException();        
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    if (!cache.genomeNotEmpty()) {
      return (false);
    }
    if (!cache.genomeIsRoot()) {
      return (false);
    }
    if (mode_ == LAYOUT_SELECTION) {
      // FIX ME: Better to base on having a NODE selected!
      return (cache.haveASelection());
    } else if (mode_ == LAYOUT_PER_OVERLAY) {
      return (cache.canLayoutByOverlay());
    } else {
      return (true);
    }  
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    GenomeInstance gi = rcx.getCurrentGenomeAsInstance();
    Group group = gi.getGroup(inter.getObjectID());
    if (gi.getVfgParent() == null) {
      int memCount = group.getMemberCount();
      return (memCount > 1);
    } else { 
      return (false);
    }       
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    return (new SpecLayoutStepState(acfSpecL_, mode_, undoStr_, dacx));  
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
        SpecLayoutStepState ans = new SpecLayoutStepState(acfSpecL_, mode_, undoStr_, cfh);       
        next = ans.stepSetupLayout();
      } else {
        SpecLayoutStepState ans = (SpecLayoutStepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepSetupLayout")) {
          next = ans.stepSetupLayout();
        } else if (ans.getNextStep().equals("stepDoLayout")) {
          next = ans.stepDoLayout();
        } else if (ans.getNextStep().equals("stepWillExit")) {
          next = ans.stepWillExit();         
        } else if (ans.getNextStep().equals("stepGetParamDialog")) {
          next = ans.stepGetParamDialog();
        } else if (ans.getNextStep().equals("stepExtractParams")) {
          next = ans.stepExtractParams(last);
        } else if (ans.getNextStep().equals("handleTopoResponse")) {
          next = ans.handleTopoResponse(last);         
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
  ** Running State
  */
        
  public static class SpecLayoutStepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {
     
    private String nextNextStep_;   
    private String groupID;
    private SpecialtyLayout specL_;
    private int mode_;
    private String undoStr_;
    private ArrayList<GenomeSubset> subsetList_;
    private Map<String, NetModuleLinkExtractor.ExtractResultForSource> interModPaths_;
    private Point2D useCenter_;
    private SpecialtyLayoutEngineParams params_;
    private int numSub_;
    private boolean isForSubset_;
    private Set<String> needExpansion_;
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public SpecLayoutStepState(SpecialtyLayout specL, int mode, String undos, StaticDataAccessContext dacx) {
      super(dacx);
      mode_ = mode;
      specL_ = specL;
      nextStep_ = "stepSetupLayout";
      undoStr_ = undos;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public SpecLayoutStepState(SpecialtyLayout specL, int mode, String undos, ServerControlFlowHarness cfh) {
      super(cfh);
      mode_ = mode;
      specL_ = specL;
      nextStep_ = "stepSetupLayout";
      undoStr_ = undos;
    }
    
    
    
      
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      this.groupID = intersect.getObjectID();
      return;
    }
    
    /***************************************************************************
    **
    ** Get set up
    */
       
    private DialogAndInProcessCmd stepSetupLayout() {
 
      subsetList_ = new ArrayList<GenomeSubset>();
      interModPaths_ = null;
      useCenter_ = null;
      isForSubset_ = (mode_ != LAYOUT_ALL);
      ResourceManager rMan = dacx_.getRMan();  
        
      switch (mode_) {
        case LAYOUT_ALL:
          GenomeSubset subset = new GenomeSubset(dacx_, dacx_.getWorkspaceSource().getWorkspace().getCanvasCenter());
          subsetList_.add(subset);
          useCenter_ = subset.getPreferredCenter();
          break;
        case LAYOUT_SELECTION:        
          subset = new GenomeSubset(dacx_, dacx_.getCurrentGenomeID(), uics_.getSUPanel().getSelectedNodes(dacx_), null);
          subset.setOrigBounds(dacx_);            
          subsetList_.add(subset);
          break;
        case LAYOUT_PER_OVERLAY:
          String overlayKey = dacx_.getOSO().getCurrentOverlay();
          if (overlayKey != null) {
            // Make sure Overlay/modules semantics are OK
            OverlayLayoutAnalyzer ola = new OverlayLayoutAnalyzer();
            OverlayLayoutAnalyzer.OverlayReport olaor = ola.canSupport(dacx_, overlayKey);
            if (olaor.getResult() == OverlayLayoutAnalyzer.OverlayReport.MISSING_LINKS) {
               MessageTableReportingDialog mtrd = 
                 new MessageTableReportingDialog(uics_, dacx_, olaor.getMissingLinkMessages(), 
                                                 "specLayout.overlayMissingLinksTitle", 
                                                 "specLayout.overlayMissingLinks", 
                                                 "specLayout.overlayMissingLink",
                                                 new Dimension(800, 800), false, true);
               mtrd.setVisible(true);
               return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); // We are done no matter what...             
            } else if (olaor.getResult() != OverlayLayoutAnalyzer.OverlayReport.NO_PROBLEMS) {
              String errText = olaor.getResultMessage(dacx_);
              String desc = MessageFormat.format(rMan.getString("specLayout.overlayNotAdequate"), new Object[] {errText});
              desc = UiUtil.convertMessageToHtml(desc);
              SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, rMan.getString("specLayout.overlayNotAdequateTitle"));
              nextStep_ = "stepWillExit";
              return (new DialogAndInProcessCmd(suf, this));
            }
       
            needExpansion_ = ola.mustExpandGeometry(dacx_, overlayKey);     
            NetOverlayOwner owner = dacx_.getGenomeSource().getOverlayOwnerFromGenomeKey(dacx_.getCurrentGenomeID());
            NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
            Map<String, Rectangle> boundsPerMod = dacx_.getCurrentLayout().getLayoutBoundsForEachNetModule(owner, overlayKey, dacx_);                           
            Iterator<NetModule> mit = nov.getModuleIterator();         
            while (mit.hasNext()) {       
              NetModule nmod = mit.next();                          
              Iterator<NetModuleMember> memit = nmod.getMemberIterator();
              HashSet<String> memIds = new HashSet<String>();
              while (memit.hasNext()) { 
                NetModuleMember nmm = memit.next();
                memIds.add(nmm.getID());
              }
              String id = nmod.getID();
              Rectangle bounds = boundsPerMod.get(id);
              subset = new GenomeSubset(dacx_, dacx_.getCurrentGenomeID(), memIds, null);
              subset.setOrigBounds(bounds);
              subset.setModuleID(id);
              subset.setOverlayID(overlayKey);
              subsetList_.add(subset);
            }
            interModPaths_ = (new NetModuleLinkExtractor()).extract(dacx_, overlayKey, subsetList_);
          }
          break;
        case LAYOUT_REGION:
          HashSet<String> regNodes = new HashSet<String>();
          Group group = dacx_.getCurrentGenomeAsInstance().getGroup(groupID);
          Iterator<GroupMember> mit = group.getMemberIterator();
          while (mit.hasNext()) {
            GroupMember gm = mit.next();
            regNodes.add(gm.getID());
          }     
          // Here we set the original bounds using the current node locations.
          subset = new GenomeSubset(dacx_, dacx_.getCurrentGenomeID(), regNodes, null);
          subset.setOrigBounds(dacx_);         
          subset.setGroupID(groupID);            
          subsetList_.add(subset);
          break;  
        default:
          throw new IllegalArgumentException();        
      }
        
      //
      // See if specialty layout has problem with topology:
      //
      
      int topoProblems = SpecialtyLayout.TOPOLOGY_HANDLED;
      numSub_ = subsetList_.size();
      if (!isForSubset_) {
        topoProblems = specL_.topologyIsHandled(dacx_);
      } else {
        for (int i = 0; i < numSub_; i++) {
          GenomeSubset subset = subsetList_.get(i);
          Genome fake = subset.getPseudoGenome();
          StaticDataAccessContext rcxF = new StaticDataAccessContext(dacx_, fake);
          int myTopoProblems = specL_.topologyIsHandled(rcxF);
          if (myTopoProblems != SpecialtyLayout.TOPOLOGY_HANDLED) {              
            topoProblems = myTopoProblems;
            if (myTopoProblems == SpecialtyLayout.TOPOLOGY_NOT_HANDLED_STOP) {
              break;
            }
          } 
        }
      }
      if (topoProblems == SpecialtyLayout.TOPOLOGY_NOT_HANDLED_OK) {
        String message = UiUtil.convertMessageToHtml(rMan.getString("specLayout.topoProblems"));
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, rMan.getString("specLayout.topoProblemsTitle"));
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
        nextStep_ = "handleTopoResponse";
        nextNextStep_ = "stepGetParamDialog";
        return (retval);
      } else if (topoProblems == SpecialtyLayout.TOPOLOGY_NOT_HANDLED_STOP) {
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("specLayout.topoProblemsMustStop"), rMan.getString("specLayout.topoProblemsTitle"));
        nextStep_ = "stepWillExit";
        return (new DialogAndInProcessCmd(suf, this));
      }
      nextStep_ = "stepGetParamDialog";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this)); // Keep going       
    }
    
    /***************************************************************************
    **
    ** Collect up the top answer, bag it or continue
    */   
   
    private DialogAndInProcessCmd handleTopoResponse(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.NO) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = nextNextStep_;
      }
      return (retval);
    }
  
    /***************************************************************************
    **
    ** On to show param dialog
    */
             
    private DialogAndInProcessCmd stepGetParamDialog() {
      
      String selectedID = uics_.getGenomePresentation().getSingleNodeSelection(dacx_);
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specL_.getParameterDialogBuildArgs(uics_, dacx_.getCurrentGenome(), selectedID, isForSubset_);
      SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);
      nextStep_ = "stepExtractParams";
      return (retval);
    }  
       
    /***************************************************************************
    **
    ** Extract params and keep going
    */
             
    private DialogAndInProcessCmd stepExtractParams(DialogAndInProcessCmd cmd) { 
 
      SpecialtyLayoutEngineParamDialogFactory.SpecParamsRequest crq = (SpecialtyLayoutEngineParamDialogFactory.SpecParamsRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }   
      params_ = crq.params;
   
      if (undoStr_ == null) {
        undoStr_ = specL_.getUndoString();
      }

      //
      // With parameters in hand, see if the layout can setup OK before heading off
      // to the background thread:
      //
      
      String problem = null;
      InvertedSrcTrg ist = new InvertedSrcTrg(dacx_.getCurrentGenome());
      for (int i = 0; i < numSub_; i++) {
        GenomeSubset subset = subsetList_.get(i);
        SpecialtyLayoutData sld = new SpecialtyLayoutData(subset, dacx_, params_, ist);
        SpecialtyLayout forked = specL_.forkForSubset(sld);
        String setupProblem = forked.setUpIsOK();
        if (setupProblem != null) {
          problem = setupProblem;
          break;
        }
      }
      
      if (problem != null) {
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, problem, dacx_.getRMan().getString("specLayout.setupProblemsTitle"));
        nextStep_ = "stepWillExit";
        return (new DialogAndInProcessCmd(suf, this));
      }
      
      nextStep_ = "stepDoLayout";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this)); // Keep going     
    }
   
    /***************************************************************************
    **
    ** Done
    */
           
    private DialogAndInProcessCmd stepWillExit() { 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do the layout
    */
          
    private DialogAndInProcessCmd stepDoLayout() {

      //
      // Before starting, we need to clear selections.  This automatically
      // installs a selection undo!  If selections stick around, they are
      // invalid after the layout changes stuff around!
      //
        
      SUPanel sup = uics_.getSUPanel();
      if (sup.hasASelection()) {
        sup.selectNone(uics_, uFac_, dacx_);
        sup.drawModel(false);
      }
      NetModuleLinkExtractor.SubsetAnalysis sa = (new NetModuleLinkExtractor()).analyzeForMods(subsetList_, interModPaths_, needExpansion_);
 
      UndoSupport support = uFac_.provideUndoSupport(undoStr_, dacx_);
      SpecialtyLayoutEngine sle = 
        new SpecialtyLayoutEngine(subsetList_, dacx_, specL_, sa, useCenter_, // null if not centering after we are done
                                  params_, false, false);
      
      SpecialtyLayoutRunner runner = new SpecialtyLayoutRunner(sle, dacx_, support);        
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(uics_, dacx_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done    
      return (daipc);
    }
    
    /***************************************************************************
    **
    ** We can do a background thread in the desktop version
    ** 
    */
  
    public boolean handleRemoteException(Exception remoteEx) {
      return (false);
    }
    
    public void cleanUpPreEnable(Object result) {
       uics_.getZoomCommandSupport().zoomToWorksheetCenter();
       return;
    }
    
    public void handleCancellation() {
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(uics_, dacx_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      LayoutLinkSupport.offerColorFixup(uics_, dacx_, (LinkRouter.RoutingResult)result, uFac_);
      return;
    }  
  } 
  
  /***************************************************************************
  **
  ** Specialty Layout
  */ 
       
  private static class SpecialtyLayoutRunner extends BackgroundWorker {
     
     private SpecialtyLayoutEngine mySle_; 
     private UndoSupport support_;
     private String loKey_;
     private StaticDataAccessContext rcx_;
   
     public SpecialtyLayoutRunner(SpecialtyLayoutEngine sle, StaticDataAccessContext rcx, UndoSupport support) {
       super(new LinkRouter.RoutingResult());
       mySle_ = sle;
       support_ = support;
       rcx_ = rcx;
     }
     
     public Object runCore() throws AsynchExitRequestException {    
       loKey_ = rcx_.getCurrentLayoutID();
       Layout.PadNeedsForLayout padNeedsForLayout = rcx_.getFGHO().getLocalNetModuleLinkPadNeeds(rcx_.getCurrentGenomeID());
       // Null means it is happening inside the specialty layout call:
       mySle_.setModuleRecoveryData(padNeedsForLayout, null);
       
       LinkRouter.RoutingResult res = mySle_.specialtyLayout(support_, this, 0.0, 1.0);
       
       //
       // Previously called:
       //   mc.repairNetModuleLinkPadsLocally(padNeeds, frc, genome_.getID(), false, support_);
       // at this point, but this operation is now happening inside the specialty layout call.
       //    
       rcx_.getZoomTarget().fixCenterPoint(true, support_, false);
       return (res);
     }

     public Object postRunCore() {
       if (rcx_.modelIsOutsideWorkspaceBounds()) {
         Rectangle allBounds = rcx_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(rcx_)).setWorkspaceToModelBounds(support_, allBounds);
       }
       support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
       return (null);
     }  
  }  
}
