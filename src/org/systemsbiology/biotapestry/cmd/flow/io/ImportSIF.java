/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.io;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenomeSIFFormatFactory;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.dialogs.SIFImportChoicesDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParamDialogFactory;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Import from CSV file
*/

public class ImportSIF extends AbstractControlFlow  {

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
  
  public ImportSIF(BTState appState) {
    super(appState);
    name = "command.ImportGenomeSIF";
    desc = "command.ImportGenomeSIF";
    mnem = "command.ImportGenomeSIFMnem";
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
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh; 
      } else {
        ans = (StepState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
      }
      if (ans.getNextStep().equals("presentImportChoices")) {
        next = ans.presentImportChoices(); 
      } else if (ans.getNextStep().equals("stepProcessImportChoices")) {
        next = ans.stepProcessImportChoices(last);         
      } else if (ans.getNextStep().equals("stepExtractParams")) {
        next = ans.stepExtractParams(last);      
      } else if (ans.getNextStep().equals("stepWarn")) {
        next = ans.stepWarn();      
      } else if (ans.getNextStep().equals("handleWarnResponse")) {
        next = ans.handleWarnResponse(last);     
      } else if (ans.getNextStep().equals("getFile")) {
        next = ans.getFile();     
      } else if (ans.getNextStep().equals("doIt")) {
        next = ans.doIt();     
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, BackgroundWorkerOwner {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private File chosenFile_;
    private LoadSaveSupport myLsSup_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    private boolean doOpt;
    private int overlayOption;
    private DataAccessContext dacx_;

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      myLsSup_ = appState_.getLSSupport();
      nextStep_ = "presentImportChoices";
      dacx_ = dacx.getContextForRoot();
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
    ** First Step. Get import choices
    */ 
       
    private DialogAndInProcessCmd presentImportChoices() {
      specLayout_ = null;  // Applies in non-replacement case
      params_ = null;
      SIFImportChoicesDialogFactory.BuildArgs ba = new SIFImportChoicesDialogFactory.BuildArgs(true);
      SIFImportChoicesDialogFactory mddf = new SIFImportChoicesDialogFactory(cfh);   
      ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepProcessImportChoices";
      return (retval);
    }

    /***************************************************************************
    **
    ** First Step. Get import choices (still needs port to dialog factory). 
    */ 
       
    private DialogAndInProcessCmd stepProcessImportChoices(DialogAndInProcessCmd cmd) {
      SIFImportChoicesDialogFactory.SIFImportRequest crq = (SIFImportChoicesDialogFactory.SIFImportRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }      
      importMode_ = crq.mode;
      doOpt = crq.doOptimize;
      overlayOption = crq.overlayOption;
      DialogAndInProcessCmd daipc;
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        SpecialtyLayoutEngine.SpecialtyType strat = crq.layoutType;
        specLayout_ = SpecialtyLayoutEngine.getLayout(strat, appState_);          
        boolean showOptions = crq.showOptionDialog;
        if (showOptions) {
          Genome targetGenome = dacx_.getGenomeSource().getGenome();
          SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout_.getParameterDialogBuildArgs(targetGenome, null, false);
          SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh);
          ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
          daipc = new DialogAndInProcessCmd(cfhd, this);
          nextStep_ = "stepExtractParams";
          return (daipc);
        }
      }
      daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepWarn";
      return (daipc);
    }
 
    /***************************************************************************
    **
    ** Extract layout params
    */ 
       
    private DialogAndInProcessCmd stepExtractParams(DialogAndInProcessCmd cmd) {    
      SpecialtyLayoutEngineParamDialogFactory.SpecParamsRequest crq = (SpecialtyLayoutEngineParamDialogFactory.SpecParamsRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }   
      params_ = crq.params;
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going 
      nextStep_ = "stepWarn";
      return (retval); 
    }

   /***************************************************************************
    **
    ** Warn of replacement
    */ 
       
    private DialogAndInProcessCmd stepWarn() {
      if ((importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) && appState_.hasAnUndoChange()) { 
        ResourceManager rMan = appState_.getRMan();      
        String message = rMan.getString("loadAction.warningMessage");
        String title = rMan.getString("loadAction.warningMessageTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
        nextStep_ = "handleWarnResponse";
        return (retval);
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going 
      nextStep_ = "getFile";
      return (retval);      
    }

    /***************************************************************************
    **
    ** Handle warn response, keep going or exit
    */   
   
    private DialogAndInProcessCmd handleWarnResponse(DialogAndInProcessCmd daipc) {   
      DialogAndInProcessCmd retval;
      int changeVizVal = daipc.suf.getIntegerResult();
      if (changeVizVal == SimpleUserFeedback.NO) {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
      } else {
        retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
        nextStep_ = "getFile";
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Get the file or stream. Still needs port.
    */ 
       
    private DialogAndInProcessCmd getFile() {
      chosenFile_ = null;
      FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(appState_, ".sif", "filterName.sif");
      chosenFile_ = myLsSup_.getFprep().getExistingImportFile("ImportDirectory", filt);
      if (chosenFile_ == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "doIt";
      return (retval); 
    }

    /***************************************************************************
    **
    ** Do it in one step
    */ 
       
    private DialogAndInProcessCmd doIt() { 

      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        appState_.setCurrentFile(null);  // since we replace everything
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeID(), 
                                                     ModelChangeEvent.MODEL_DROPPED);     
        appState_.getEventMgr().sendModelChangeEvent(mcev);
        appState_.getCommonView().manageWindowTitle(chosenFile_.getName());
        myLsSup_.newModelOperations(dacx_);
      }
      myLsSup_.getFprep().setPreference("ImportDirectory", chosenFile_.getAbsoluteFile().getParent());
      UndoSupport support = new UndoSupport(appState_, "undo.buildFromSIF");            
      SIFImportRunner runner = new SIFImportRunner(appState_, dacx_, chosenFile_, importMode_, doOpt, overlayOption, support, specLayout_, params_);
      BackgroundWorkerClient bwc;     
      if (!appState_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, support);
      }        
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done    
      return (daipc);
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedImport(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {
      finishedImport((DBGenomeSIFFormatFactory.AugmentedResult)result, null);
      return;
    }

    private void finishedImport(DBGenomeSIFFormatFactory.AugmentedResult result, IOException ioEx) {   
      if ((result != null) && (result.coreResult != null)) {
        (new LayoutStatusReporter(appState_, result.coreResult)).doStatusAnnouncements();
      }
      CommonView cv = appState_.getCommonView();
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          dacx_.drop();
          cv.manageWindowTitle(null);
          myLsSup_.newModelOperations(dacx_);
        }
        myLsSup_.getFprep().getFileInputError(ioEx).displayFileInputError();
        return;                
      }
      if (dacx_.getGenomeSource().getGenome() == null) {
        dacx_.drop();
        cv.manageWindowTitle(null);
        myLsSup_.newModelOperations(dacx_);                
        myLsSup_.getFprep().getFileInputError(null).displayFileInputError();
        return;
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        cv.manageWindowTitle(chosenFile_.getName());
        myLsSup_.postLoadOperations(true, dacx_);
      }
      LayoutLinkSupport.offerColorFixup(appState_, dacx_, result.coreResult);
      
      if ((result != null) && !result.usedSignTag) {
        ResourceManager rMan = appState_.getRMan();
        Object[] tags = DBGenomeSIFFormatFactory.getSignTags();
        String desc = MessageFormat.format(UiUtil.convertMessageToHtml(rMan.getString("loadAction.couldUseTags")), tags); 
        JOptionPane.showMessageDialog(appState_.getTopFrame(),desc, 
                                      rMan.getString("loadAction.couldUseTagsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      return;
    }    
  }

 /***************************************************************************
  **
  ** Background SIF import (and layout)
  */ 
    
  private static class SIFImportRunner extends BackgroundWorker {
 
    private BTState myAppState_;
    private DataAccessContext myDacx_;
    private UndoSupport support_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private File file_;
    private boolean doOpts_;
    private int overlayOption_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    
    public SIFImportRunner(BTState appState, DataAccessContext dacx, File file,
                           SIFImportChoicesDialogFactory.LayoutModes importMode, boolean doOpts, int overlayOption,
                           UndoSupport support, SpecialtyLayout specLayout, SpecialtyLayoutEngineParams params) {
      super(new DBGenomeSIFFormatFactory.AugmentedResult(new LinkRouter.RoutingResult(), false));      
      file_ = file;
      myAppState_ = appState;
      myDacx_ = dacx;
      importMode_ = importMode;
      doOpts_ = doOpts;
      support_ = support;
      overlayOption_ = overlayOption;
      specLayout_ = specLayout;
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      DBGenomeSIFFormatFactory sff = new DBGenomeSIFFormatFactory();
      try {
        DBGenomeSIFFormatFactory.AugmentedResult ar = sff.buildFromSIF(myAppState_, myDacx_, file_,                                                       
                                                                       (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT),
                                                                       specLayout_, params_,
                                                                       support_, doOpts_, overlayOption_, this, 0.0, 1.0);
        return (ar);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      if (myAppState_.modelIsOutsideWorkspaceBounds()) {
        Rectangle allBounds = myAppState_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(myAppState_, myDacx_)).setWorkspaceToModelBounds(support_, allBounds);
      }
      return (null);
    } 
  }
}
