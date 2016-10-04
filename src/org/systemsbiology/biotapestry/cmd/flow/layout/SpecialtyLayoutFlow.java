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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
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
  
  public SpecialtyLayoutFlow(BTState appState, SpecialtyLayout specL, int mode) {
    super(appState);
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    GenomeInstance gi = rcx.getGenomeAsInstance();
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new SpecLayoutStepState(appState_, acfSpecL_, mode_, undoStr_, dacx));  
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
        SpecLayoutStepState ans = new SpecLayoutStepState(appState_, acfSpecL_, mode_, undoStr_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepSetupLayout();
      } else {
        SpecLayoutStepState ans = (SpecLayoutStepState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
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
        
  public static class SpecLayoutStepState implements DialogAndInProcessCmd.PopupCmdState, BackgroundWorkerOwner {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_; 
    private String nextNextStep_;   
    private BTState appState_;
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
    private DataAccessContext rcxT_;
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public SpecLayoutStepState(BTState appState, SpecialtyLayout specL, int mode, String undos, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;  
      mode_ = mode;
      specL_ = specL;
      nextStep_ = "stepSetupLayout";
      undoStr_ = undos;
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
      ResourceManager rMan = appState_.getRMan();  
        
      switch (mode_) {
        case LAYOUT_ALL:
          GenomeSubset subset = new GenomeSubset(appState_, rcxT_.getGenomeID(), appState_.getCanvasCenter());
          subsetList_.add(subset);
          useCenter_ = subset.getPreferredCenter();
          break;
        case LAYOUT_SELECTION:        
          subset = new GenomeSubset(appState_, rcxT_.getGenomeID(), appState_.getSUPanel().getSelectedNodes(), null);
          subset.setOrigBounds(rcxT_);            
          subsetList_.add(subset);
          break;
        case LAYOUT_PER_OVERLAY:
          String overlayKey = rcxT_.oso.getCurrentOverlay();
          if (overlayKey != null) {
            // Make sure Overlay/modules semantics are OK
            OverlayLayoutAnalyzer ola = new OverlayLayoutAnalyzer();
            OverlayLayoutAnalyzer.OverlayReport olaor = ola.canSupport(rcxT_, overlayKey);
            if (olaor.getResult() == OverlayLayoutAnalyzer.OverlayReport.MISSING_LINKS) {
               MessageTableReportingDialog mtrd = 
                 new MessageTableReportingDialog(appState_, olaor.getMissingLinkMessages(), 
                                                 "specLayout.overlayMissingLinksTitle", 
                                                 "specLayout.overlayMissingLinks", 
                                                 "specLayout.overlayMissingLink",
                                                 new Dimension(800, 800), false, true);
               mtrd.setVisible(true);
               return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this)); // We are done no matter what...             
            } else if (olaor.getResult() != OverlayLayoutAnalyzer.OverlayReport.NO_PROBLEMS) {
              String errText = olaor.getResultMessage(appState_);
              String desc = MessageFormat.format(rMan.getString("specLayout.overlayNotAdequate"), new Object[] {errText});
              desc = UiUtil.convertMessageToHtml(desc);
              SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, rMan.getString("specLayout.overlayNotAdequateTitle"));
              nextStep_ = "stepWillExit";
              return (new DialogAndInProcessCmd(suf, this));
            }
       
            needExpansion_ = ola.mustExpandGeometry(rcxT_, overlayKey);     
            NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
            NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
            Map<String, Rectangle> boundsPerMod = rcxT_.getLayout().getLayoutBoundsForEachNetModule(owner, overlayKey, rcxT_);                           
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
              subset = new GenomeSubset(appState_, rcxT_.getGenomeID(), memIds, null);
              subset.setOrigBounds(bounds);
              subset.setModuleID(id);
              subset.setOverlayID(overlayKey);
              subsetList_.add(subset);
            }
            interModPaths_ = (new NetModuleLinkExtractor()).extract(rcxT_, overlayKey, subsetList_);
          }
          break;
        case LAYOUT_REGION:
          HashSet<String> regNodes = new HashSet<String>();
          Group group = rcxT_.getGenomeAsInstance().getGroup(groupID);
          Iterator<GroupMember> mit = group.getMemberIterator();
          while (mit.hasNext()) {
            GroupMember gm = mit.next();
            regNodes.add(gm.getID());
          }     
          // Here we set the original bounds using the current node locations.
          subset = new GenomeSubset(appState_, rcxT_.getGenomeID(), regNodes, null);
          subset.setOrigBounds(rcxT_);         
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
        topoProblems = specL_.topologyIsHandled(rcxT_);
      } else {
        for (int i = 0; i < numSub_; i++) {
          GenomeSubset subset = subsetList_.get(i);
          Genome fake = subset.getPseudoGenome();
          DataAccessContext rcxF = new DataAccessContext(rcxT_, fake);
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
      
      String selectedID = appState_.getGenomePresentation().getSingleNodeSelection(rcxT_);
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specL_.getParameterDialogBuildArgs(rcxT_.getGenome(), selectedID, isForSubset_);
      SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh);
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
      InvertedSrcTrg ist = new InvertedSrcTrg(rcxT_.getGenome());
      for (int i = 0; i < numSub_; i++) {
        GenomeSubset subset = subsetList_.get(i);
        SpecialtyLayoutData sld = new SpecialtyLayoutData(appState_, subset, rcxT_, params_, ist);
        SpecialtyLayout forked = specL_.forkForSubset(sld);
        String setupProblem = forked.setUpIsOK();
        if (setupProblem != null) {
          problem = setupProblem;
          break;
        }
      }
      
      if (problem != null) {
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, problem, rcxT_.rMan.getString("specLayout.setupProblemsTitle"));
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
        
      SUPanel sup = appState_.getSUPanel();
      if (sup.hasASelection()) {
        sup.selectNone(appState_.getUndoManager(), rcxT_);
        sup.drawModel(false);
      }
      NetModuleLinkExtractor.SubsetAnalysis sa = (new NetModuleLinkExtractor()).analyzeForMods(subsetList_, interModPaths_, needExpansion_);
 
      UndoSupport support = new UndoSupport(appState_, undoStr_);
      SpecialtyLayoutEngine sle = 
        new SpecialtyLayoutEngine(appState_, subsetList_, rcxT_, specL_, sa, useCenter_, // null if not centering after we are done
                                  params_, false, false);
      
      SpecialtyLayoutRunner runner = new SpecialtyLayoutRunner(appState_, sle, rcxT_, support);        
      BackgroundWorkerClient bwc = 
        new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
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
       appState_.getZoomCommandSupport().zoomToWorksheetCenter();
       return;
    }
    
    public void handleCancellation() {
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {
      (new LayoutStatusReporter(appState_, (LinkRouter.RoutingResult)result)).doStatusAnnouncements();
      LayoutLinkSupport.offerColorFixup(appState_, rcxT_, (LinkRouter.RoutingResult)result);
      return;
    }  
  } 
  
  /***************************************************************************
  **
  ** Specialty Layout
  */ 
       
  private static class SpecialtyLayoutRunner extends BackgroundWorker {
     
     private SpecialtyLayoutEngine mySle_; 
     private BTState myAppState_;
     private UndoSupport support_;
     private String loKey_;
     private DataAccessContext rcx_;
   
     public SpecialtyLayoutRunner(BTState appState, SpecialtyLayoutEngine sle, DataAccessContext rcx, UndoSupport support) {
       super(new LinkRouter.RoutingResult());
       mySle_ = sle;
       support_ = support;
       myAppState_ = appState;
       rcx_ = rcx;
     }
     
     public Object runCore() throws AsynchExitRequestException {    
       loKey_ = rcx_.getLayoutID();
       Layout.PadNeedsForLayout padNeedsForLayout = rcx_.fgho.getLocalNetModuleLinkPadNeeds(rcx_.getGenomeID());
       // Null means it is happening inside the specialty layout call:
       mySle_.setModuleRecoveryData(padNeedsForLayout, null);
       
       LinkRouter.RoutingResult res = mySle_.specialtyLayout(support_, this, 0.0, 1.0);
       
       //
       // Previously called:
       //   mc.repairNetModuleLinkPadsLocally(padNeeds, frc, genome_.getID(), false, support_);
       // at this point, but this operation is now happening inside the specialty layout call.
       //    
       myAppState_.getZoomTarget().fixCenterPoint(true, support_, false);
       return (res);
     }

     public Object postRunCore() {
       if (myAppState_.modelIsOutsideWorkspaceBounds()) {
         Rectangle allBounds = myAppState_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(myAppState_, rcx_)).setWorkspaceToModelBounds(support_, allBounds);
       }
       support_.addEvent(new LayoutChangeEvent(loKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE));
       return (null);
     }  
  }  
}
