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


package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Point;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.PathAndFileSource;
import org.systemsbiology.biotapestry.app.RememberSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd.Progress;
import org.systemsbiology.biotapestry.cmd.flow.view.Zoom;
import org.systemsbiology.biotapestry.nav.DataPopupManager;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.FileChooserWrapperFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Java Desktop Control Flow
*/

public class DesktopControlFlowHarness extends ServerControlFlowHarness implements ClientControlFlowHarness {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private String currOvr_;
  private GenomePresentation pre_;
  private UserInputs stashedResults_;
  private boolean outOfBandResult_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DesktopControlFlowHarness(HarnessBuilder hBld, UIComponentSource uics, RememberSource rSrc, 
                                   UndoFactory uFac, TabSource tSrc, PathAndFileSource pafs, 
                                   CmdSource cSrc, DesktopDialogPlatform dPlat) {  
    super(dPlat, hBld, uics, rSrc, uFac, tSrc, pafs, cSrc); 
    currFlow = null;
    pre_ = uics_.getGenomePresentation();
    stashedResults_ = null;   
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHOD
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the client side harness
  */ 
  
  @Override
  public ClientControlFlowHarness getClientHarness() {
    return (this);
  }
  
  /***************************************************************************
  **
  ** Init the flow
  */ 
  
  @Override
  public void initFlow(ControlFlow theFlow, StaticDataAccessContext dacx) {
    super.initFlow(theFlow, dacx);
    currOvr_ = dacx.getOSO().getCurrentOverlay();
    return;
  }
  
  /***************************************************************************
  **
  ** Client-side Process an out-of-flow question/response
  */ 

  public RemoteRequest.Result routeRemoteRequest(RemoteRequest qubom) {
    return (receiveRemoteRequest(qubom));   
  }
  
  /***************************************************************************
  **
  ** Direct routing on desktop platform
  */ 
     
  public void sendUserInputs(UserInputs cfhui) {
    receiveUserInputs(cfhui);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle dialog inputs.  This platform returns null!
  */ 
  
  @Override
  public DialogAndInProcessCmd receiveUserInputs(UserInputs cfhui) {
    if ((cfhui != null) && cfhui.isForApply()) {
      currDaipc = new DialogAndInProcessCmd(cfhui, currDaipc.currStateX,currDaipc.dialog);
      stepTheFlow();
      if(currDaipc.state == Progress.SIMPLE_USER_FEEDBACK && currDaipc.suf != null) {
    	  this.showSimpleUserFeedback(currDaipc.suf);
      }  else {
          VisualChangeResult vixR = currFlow.visualizeResults(currDaipc);
          if (vixR != null) {
        	  if(currDaipc.dialog != null && currDaipc.state == Progress.DONE) {
            	  ((JDialog)currDaipc.dialog).setVisible(false);
            	  ((JDialog)currDaipc.dialog).dispose();
              }        		  
    	  }
          installNewView(vixR);
      }
    } else {
      stashedResults_ = cfhui;      
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Handle user click
  */ 
  
  @Override
  public DialogAndInProcessCmd handleClick(Point theClick, boolean isShifted, double pixDiam)  {
    currDaipc = currFlow.processClick(theClick, isShifted, pixDiam, currDaipc.currStateX);
    return (runFlowGuts());
  }
  
  /***************************************************************************
  **
  ** Run the flow with pre-init state
  */ 
  
  public DialogAndInProcessCmd runFlow(DialogAndInProcessCmd.CmdState cms)  {
    currDaipc = (cms != null) ? new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.NORMAL, cms) : null; 
    stepTheFlow(); // To get initial currDaipc processed
    return (runFlowGuts());
  }  
  
  /***************************************************************************
  **
  ** Get oob result
  */ 
  
  public boolean getOOBResult()  {
    return (outOfBandResult_);
  }  
 
  /***************************************************************************
  **
  ** Run the flow
  */ 
  
  public void runFlow()  {
    runFlow(null);
    return;
  }  
  
  /***************************************************************************
  **
  ** Core of flow
  */ 
  
  private DialogAndInProcessCmd runFlowGuts()  { 
    SUPanel sup = uics_.getSUPanel();
    while (currDaipc != null) {
      switch (currDaipc.state) {
      // Mouse mode has been installed; return it.
        case MOUSE_MODE_RESULT:
          if (currDaipc.pccr != null) {
            return (currDaipc);
          }
          break;
        case SIMPLE_USER_FEEDBACK:
        case SIMPLE_USER_FEEDBACK_AND_MOUSE_RESULT:
          if ((currDaipc.suf != null) && (currDaipc.suf.optionPane != SimpleUserFeedback.JOP.NONE)) {
            showSimpleUserFeedback(currDaipc.suf);
          }
          // Show feedback message AND return a mouse result:
          if ((currDaipc.state == DialogAndInProcessCmd.Progress.SIMPLE_USER_FEEDBACK_AND_MOUSE_RESULT) && (currDaipc.pccr != null)) {
            return (currDaipc);
          }
          break;
        case USER_CANCEL:
          clearFlow();
          outOfBandResult_ = false;
          return (null);
        case HAVE_ERROR:
          sup.giveErrorFeedback();
          // Cursor and keyboard beep then return;
          return (currDaipc);
        case HAVE_DIALOG_TO_SHOW:
          if (currDaipc.dialog != null) {
            if (uics_.getIsEditor() && currDaipc.dialog instanceof FileChooserWrapperFactory.DesktopFileChooserWrapper) {
              ((FileChooserWrapperFactory.DesktopFileChooserWrapper)currDaipc.dialog).launch();
            } else {          
              JDialog uijd = (JDialog)currDaipc.dialog;
              uijd.setVisible(true);
            }
            // Blocking here for modal inputs; stashed results filled in FOR OK
            // If doing apply, we will be doing stepTheFlows while receiving stashed results! 
            sup.drawModel(false);
            if (stashedResults_ == null) {
              return (null);
            }
            UserInputs use = stashedResults_;
            stashedResults_ = null;
            currDaipc = new DialogAndInProcessCmd(use, currDaipc.currStateX);
          }
          break;
        case HAVE_FRAME_TO_LAUNCH:
        case HAVE_FRAME_TO_LAUNCH_AND_MOUSE_RESULT:
          if (currDaipc.dialog != null) {
            JFrame uijd = (JFrame)currDaipc.dialog;
            DataPopupManager dpm = uics_.getDataPopupMgr();
            dpm.manageFrame(uijd);
          }
          if (currDaipc.state == DialogAndInProcessCmd.Progress.HAVE_FRAME_TO_LAUNCH_AND_MOUSE_RESULT) {
            if (currDaipc.pccr != null) {
              return (currDaipc);
            }
          } else {
            clearFlow();
            return (null);
          }
        case DONE_WITH_SIMPLE_USER_FEEDBACK:  
        case DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK:  
          if ((currDaipc.suf != null) && (currDaipc.suf.optionPane != SimpleUserFeedback.JOP.NONE)) {
            showSimpleUserFeedback(currDaipc.suf);
          }
          // Yes that's right we have no break statement here we are really falling through to the DONE case!
        case DONE:
          VisualChangeResult vixR = currFlow.visualizeResults(currDaipc);
          if (vixR != null) {
            installNewView(vixR);
          }
          DialogAndInProcessCmd retval = currDaipc;
          clearFlow();
          outOfBandResult_ = true;
          return (retval);
        case DONE_ON_THREAD:  // Viz install will be called on thread completion      
          clearFlow();
          return (null);
        case INSTALL_MOUSE_MODE:
          DialogAndInProcessCmd.ButtonMaskerCmdState bmcs = (DialogAndInProcessCmd.ButtonMaskerCmdState)currDaipc.currStateX;
          XPlatMaskingStatus ms = uics_.getCommonView().calcDisableControls(bmcs.getMask(), bmcs.pushDisplay());
          boolean inMode = cSrc_.getPanelCmds().setToMode(currDaipc, ms);
          if (inMode) {
            cSrc_.getPanelCmds().setHarness(this);
          }
          return (null);
        case KEEP_PROCESSING: // ONLY AFTER MOUSE CLICK!   
          // Keep going, no change
          break;
        case NORMAL: // NOT EXPECTED
        case HAVE_USER_INPUTS: // PROCESSED IN HAVE_DIALOG_TO_SHOW CASE ON DESKTOP
        case APPLY_PROCESSED: // PROCESSED IN HAVE_DIALOG_TO_SHOW CASE ON DESKTOP
        case APPLY_ON_THREAD: // PROCESSED IN HAVE_DIALOG_TO_SHOW CASE ON DESKTOP
        default:
          throw new IllegalStateException("[ERROR] case "+currDaipc.state+" is not supported here.");
      }      
      stepTheFlow();
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Map JOptionPane results to cross-platform results
  ** 
  */

  private int mapJOPReturn(int JOPVal) {
    switch (JOPVal) {
      case JOptionPane.YES_OPTION:
        return (SimpleUserFeedback.YES);
      case JOptionPane.NO_OPTION:
        return (SimpleUserFeedback.NO);
      case JOptionPane.CANCEL_OPTION:
        return (SimpleUserFeedback.CANCEL);
      default:
        return (JOPVal);
    }
  }

  /***************************************************************************
  **
  ** Desktop handling of traditional JOptionPanes
  ** 
  */ 
  
  public void showSimpleUserFeedback(SimpleUserFeedback suf) {
    JFrame topFrame = uics_.getTopFrame();
    switch (suf.optionPane) {
      case WARNING:
        JOptionPane.showMessageDialog(topFrame, suf.message, suf.title, JOptionPane.WARNING_MESSAGE);
        break;
      case PLAIN:
        JOptionPane.showMessageDialog(topFrame, suf.message, suf.title, JOptionPane.PLAIN_MESSAGE);
        break;
      case ERROR:
        JOptionPane.showMessageDialog(topFrame, suf.message, suf.title, JOptionPane.ERROR_MESSAGE);
        break;
      case YES_NO_CANCEL_OPTION:
        suf.setIntegerResult(mapJOPReturn(JOptionPane.showConfirmDialog(topFrame, suf.message, suf.title, JOptionPane.YES_NO_CANCEL_OPTION)));
        break;
      case YES_NO_OPTION:
        suf.setIntegerResult(mapJOPReturn(JOptionPane.showConfirmDialog(topFrame, suf.message, suf.title, JOptionPane.YES_NO_OPTION)));
        break;          
      case QUESTION:  
        suf.setObjectResult(JOptionPane.showInputDialog(topFrame, suf.message, suf.title, JOptionPane.QUESTION_MESSAGE,
                                                        null, suf.jopOptions, suf.jopDefault));
        break;
      case OPTION_OPTION:
        // Caller is responsible for assigning and interpreting YES/NO/CANCEL values
        suf.setIntegerResult(JOptionPane.showOptionDialog(topFrame, suf.message, suf.title, 
                                                          JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, 
                                                          null, suf.jopOptions, suf.jopDefault));  
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** The desktop control flow knows how to select items, clear items, show modules, etc.
  ** 
  */
   
  private void installNewView(VisualChangeResult vixR) {  
    List<VisualChangeResult.ViewChange> vcs = vixR.getOperations();
    for (VisualChangeResult.ViewChange vc: vcs) {
      switch (vc) {   
        case NONE:
          break;
        case NEW_SELECTIONS:
          installSelections(vixR);
          break;
        case CLEAR_SELECTIONS:
          clearSelections(vixR);
          break;
        case OVERLAY_STATE:
          setOverlays(vixR);
          break;
        case SHOW_MESSAGE:
          showMessage(vixR);
          break;
        default:
          throw new IllegalStateException();
      }
    }
    setViewport(vixR);
    if (vixR.isStale()) {
      uics_.getSUPanel().drawModel(false);
    }
    return;
  }
   
  private void installSelections(VisualChangeResult svr) {
    pre_.selectNodesAndLinks(uics_, svr.getSelectedNodes(), dacx, svr.getSelectedLinks(), svr.doClearCurrent(), uFac_);
    return;
  }
    
  private void clearSelections(VisualChangeResult svr) {
    UndoSupport support = uFac_.provideUndoSupport(svr.getUndoString(), dacx);
    pre_.clearSelections(uics_, dacx, support);
    support.finish();
    return;
  }  
    
  private void showMessage(VisualChangeResult svr) {
    showSimpleUserFeedback(svr.getFeedback());  
    return;
  }    
    
  private void setOverlays(VisualChangeResult svr) {
    UndoSupport support = uFac_.provideUndoSupport(svr.getUndoString(), dacx);
    NetOverlayController noc = uics_.getNetOverlayController();
    // Don't change the current revealed settings at all:
    noc.setFullOverlayState(currOvr_, svr.getModuleMatches(), dacx.getOSO().getRevealedModules(), support, dacx);
    Zoom.zoomToCurrentMod(uics_, dacx);
    support.finish();
  }
  
  private void setViewport(VisualChangeResult results) {
    if (results.getViewport() == VisualChangeResult.Viewports.SELECTED) {
      uics_.getZoomCommandSupport().zoomToSelected();
    } 
    return;
  } 
}
