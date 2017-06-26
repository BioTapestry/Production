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

package org.systemsbiology.biotapestry.cmd.flow.io;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
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
import org.systemsbiology.biotapestry.util.UndoFactory;
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
  
  public ImportSIF() {
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
        ans = new StepState(cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
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
        
  public static class StepState extends AbstractStepState implements BackgroundWorkerOwner {
     
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private File chosenFile_;
    private LoadSaveSupport myLsSup_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    private boolean doOpt;
    private int overlayOption;
    private StaticDataAccessContext rcxT_;

    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
     //  myLsSup_ = uics_.getLSSupport(); Gotta get cfh in
      nextStep_ = "presentImportChoices";
      rcxT_ = dacx_.getContextForRoot();
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      myLsSup_ = uics_.getLSSupport();
      nextStep_ = "presentImportChoices";
      rcxT_ = dacx_.getContextForRoot();
    }
    
    /***************************************************************************
    **
    ** Gotta override to make sure myLsSup_ can get initialized.
    */
    
    @Override
    public void stockCfhIfNeeded(ServerControlFlowHarness cfh) {
      if (cfh_ != null) {
        return;
      }
      super.stockCfhIfNeeded(cfh);
      myLsSup_ = uics_.getLSSupport();
      return;
    }
 
    /***************************************************************************
    **
    ** First Step. Get import choices
    */ 
       
    private DialogAndInProcessCmd presentImportChoices() {
      specLayout_ = null;  // Applies in non-replacement case
      params_ = null;
      SIFImportChoicesDialogFactory.BuildArgs ba = new SIFImportChoicesDialogFactory.BuildArgs(true, rcxT_);
      SIFImportChoicesDialogFactory mddf = new SIFImportChoicesDialogFactory(cfh_);   
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
        specLayout_ = SpecialtyLayoutEngine.getLayout(strat, new StaticDataAccessContext(rcxT_));          
        boolean showOptions = crq.showOptionDialog;
        if (showOptions) {
          Genome targetGenome = rcxT_.getGenomeSource().getRootDBGenome();
          SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout_.getParameterDialogBuildArgs(uics_, targetGenome, null, false);
          SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh_);
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
      if ((importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) && cmdSrc_.hasAnUndoChange()) { 
        ResourceManager rMan = rcxT_.getRMan();      
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
      FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(rcxT_.getRMan(), ".sif", "filterName.sif");
      chosenFile_ = myLsSup_.getFprep(rcxT_).getExistingImportFile("ImportDirectory", filt);
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
        myLsSup_.setCurrentFile(null);  // since we replace everything
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), 
                                                     ModelChangeEvent.MODEL_DROPPED);     
        uics_.getEventMgr().sendModelChangeEvent(mcev);
        uics_.getCommonView().manageWindowTitle(chosenFile_.getName());
        myLsSup_.newModelOperations(rcxT_);
      }
      myLsSup_.getFprep(rcxT_).setPreference("ImportDirectory", chosenFile_.getAbsoluteFile().getParent());
      UndoSupport support = uFac_.provideUndoSupport("undo.buildFromSIF", rcxT_);            
      SIFImportRunner runner = new SIFImportRunner(uics_, tSrc_, rcxT_, uFac_, chosenFile_, importMode_, doOpt, overlayOption, support, specLayout_, params_);
      BackgroundWorkerClient bwc;     
      if (!uics_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(uics_, rcxT_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(uics_, rcxT_, this, runner, support);
      }        
      runner.setClient(bwc);
      bwc.launchWorker();
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
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
        (new LayoutStatusReporter(uics_, rcxT_, result.coreResult)).doStatusAnnouncements();
      }
      CommonView cv = uics_.getCommonView();
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          rcxT_.drop();
          cv.manageWindowTitle(null);
          myLsSup_.newModelOperations(rcxT_);
        }
        myLsSup_.getFprep(rcxT_).getFileInputError(ioEx).displayFileInputError();
        return;                
      }
      if (rcxT_.getGenomeSource().getRootDBGenome() == null) {
        rcxT_.drop();
        cv.manageWindowTitle(null);
        myLsSup_.newModelOperations(rcxT_);                
        myLsSup_.getFprep(rcxT_).getFileInputError(null).displayFileInputError();
        return;
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        cv.manageWindowTitle(chosenFile_.getName());
        myLsSup_.postLoadOperations(true, 0, false, false, null);
      }
      LayoutLinkSupport.offerColorFixup(uics_, rcxT_, result.coreResult, uFac_);
      
      if ((result != null) && !result.usedSignTag) {
        ResourceManager rMan = rcxT_.getRMan();
        Object[] tags = DBGenomeSIFFormatFactory.getSignTags();
        String desc = MessageFormat.format(UiUtil.convertMessageToHtml(rMan.getString("loadAction.couldUseTags")), tags); 
        JOptionPane.showMessageDialog(uics_.getTopFrame(),desc, 
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
 
    private UIComponentSource myUics_;
    private TabSource myTSrc_;
    private UndoFactory myUFac_;
    private StaticDataAccessContext myDacx_;
    private UndoSupport support_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private File file_;
    private boolean doOpts_;
    private int overlayOption_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    
    public SIFImportRunner(UIComponentSource uics, TabSource tSrc, StaticDataAccessContext dacx, UndoFactory uFac, File file,
                           SIFImportChoicesDialogFactory.LayoutModes importMode, boolean doOpts, int overlayOption,
                           UndoSupport support, SpecialtyLayout specLayout, SpecialtyLayoutEngineParams params) {
      super(new DBGenomeSIFFormatFactory.AugmentedResult(new LinkRouter.RoutingResult(), false));      
      file_ = file;
      myUics_ = uics;
      myTSrc_ = tSrc;
      myUFac_ = uFac;
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
        DBGenomeSIFFormatFactory.AugmentedResult ar = sff.buildFromSIF(myUics_,  myDacx_, myTSrc_, myUFac_, file_,                                                       
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
      if (myDacx_.modelIsOutsideWorkspaceBounds()) {
        Rectangle allBounds = myDacx_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(myDacx_)).setWorkspaceToModelBounds(support_, allBounds);
      }
      return (null);
    } 
  }
}
