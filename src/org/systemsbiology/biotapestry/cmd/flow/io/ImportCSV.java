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
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.FullHierarchyBuilder;
import org.systemsbiology.biotapestry.genome.FullHierarchyCSVFormatFactory;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.dialogs.SIFImportChoicesDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParamDialogFactory;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayout;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Import from CSV file
*/

public class ImportCSV extends AbstractControlFlow  {

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
  
  public ImportCSV() {
    name = "command.ImportHierarchyCSV";
    desc = "command.ImportHierarchyCSV";
    mnem = "command.ImportHierarchyCSVMnem";
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
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
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
      } else if (ans.getNextStep().equals("getFileOrStream")) {
        next = ans.getFileOrStream();  
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
    private String chosenFile_;
    private boolean headlessFailure_;
    private boolean doOpt;
    private boolean doSquash;
    private int overlayOption;
    private Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap;
    private Map<String, String> modelIDMap;
    private LoadSaveSupport myLsSup_;
    private Boolean doFile;
    private File useFile;
    private InputStream useStream;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    private StaticDataAccessContext rcx_;
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "presentImportChoices";
      // myLsSup_: uics_ not initialized yet!
      rcx_ = dacx.getContextForRoot();
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "presentImportChoices";
      myLsSup_ = uics_.getLSSupport();
      rcx_ = dacx_.getContextForRoot();
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
    ** Set the popup params
    */ 
        
    public void setParams(Object[] args) {
      doOpt = false;
      doSquash = ((Boolean)args[0]).booleanValue();
      importMode_ = (SIFImportChoicesDialogFactory.LayoutModes)args[1];
      overlayOption = ((Integer)args[2]).intValue();
      headlessFailure_ = false;
      // args 3, 4 for input stream:
      doFile = (Boolean)args[3];
      if (doFile.booleanValue()) {
        useFile = new File((String)args[4]);
      } else {
        useStream = (InputStream)args[4];
      }
      nodeIDMap = (Map<NodeRegionModelNameTuple, ModelNodeIDPair>)args[5];
      modelIDMap = (Map<String, String>)args[6];
      return;
    }
    
    /***************************************************************************
    **
    ** First Step. Get import choices (still needs port to dialog factory). 
    */ 
       
    private DialogAndInProcessCmd presentImportChoices() {
      DialogAndInProcessCmd daipc;
      specLayout_ = null;  // Applies in non-replacement case
      params_ = null;
      if (!uics_.isHeadless()) { // not headless
        SIFImportChoicesDialogFactory.BuildArgs ba = new SIFImportChoicesDialogFactory.BuildArgs(false, rcx_);
        SIFImportChoicesDialogFactory mddf = new SIFImportChoicesDialogFactory(cfh_);   
        ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
        nextStep_ = "stepProcessImportChoices";
        return (retval);
      }
      // headless
      specLayout_ = new WorksheetLayout(true, new StaticDataAccessContext(rcx_));
      params_ = rcx_.getLayoutOptMgr().getDiagLayoutParams();      
      daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "getFileOrStream";
      return (daipc);
    }

    /***************************************************************************
    **
    ** First Step. Get import choices 
    */ 
       
    private DialogAndInProcessCmd stepProcessImportChoices(DialogAndInProcessCmd cmd) {
      SIFImportChoicesDialogFactory.SIFImportRequest crq = (SIFImportChoicesDialogFactory.SIFImportRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
        
      importMode_ = crq.mode;
      doOpt = crq.doOptimize;
      doSquash = crq.doSquash;
      overlayOption = crq.overlayOption;
      DialogAndInProcessCmd daipc;
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        SpecialtyLayoutEngine.SpecialtyType strat = crq.layoutType;
        specLayout_ = SpecialtyLayoutEngine.getLayout(strat, new StaticDataAccessContext(rcx_));          
        boolean showOptions = crq.showOptionDialog;
        if (showOptions) {
          Genome targetGenome = rcx_.getDBGenome();
          SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout_.getParameterDialogBuildArgs(uics_, targetGenome, null, false);
          SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh_);
          ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
          daipc = new DialogAndInProcessCmd(cfhd, this);
          nextStep_ = "stepExtractParams";
          return (daipc);
        } else {
          params_ = rcx_.getLayoutOptMgr().getLayoutParams(strat);   
        }
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepWarn";         
      }
  
      daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "getFileOrStream";
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
      if (!uics_.isHeadless() && 
          (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) &&
          cmdSrc_.hasAnUndoChange()) { 
        ResourceManager rMan = rcx_.getRMan();      
        String message = rMan.getString("loadAction.warningMessage");
        String title = rMan.getString("loadAction.warningMessageTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(suf, this);      
        nextStep_ = "handleWarnResponse";
        return (retval);
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going 
      nextStep_ = "getFileOrStream";
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
        nextStep_ = "getFileOrStream";
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Get the file or stream. Still needs port.
    */ 
       
    private DialogAndInProcessCmd getFileOrStream() {
 
      //
      // Going to use a file or a stream:
      //
        
      if (!uics_.isHeadless()) { // not headless      
        FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(rcx_.getRMan(), ".csv", "filterName.csv");
        useFile = myLsSup_.getFprep(dacx_).getExistingImportFile("ImportDirectory", filt);
        if (useFile == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
      } else {
        if (doFile.booleanValue()) {
          if (!myLsSup_.getFprep(dacx_).checkExistingImportFile(useFile)) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }     
        }
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      nextStep_ = "doIt";
      return (retval); 
    }
 
    /***************************************************************************
    **
    ** Final step. Do it
    */ 
       
    private DialogAndInProcessCmd doIt() {
   
      // Have a file now:
      
      chosenFile_ = (useFile == null) ? null : useFile.getName();
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        myLsSup_.setCurrentFile(null);  // since we replace everything
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), rcx_.getCurrentGenomeID(), 
                                                     ModelChangeEvent.MODEL_DROPPED);     
        uics_.getEventMgr().sendModelChangeEvent(mcev);
        uics_.getCommonView().manageWindowTitle(chosenFile_);
        myLsSup_.newModelOperations(rcx_);
      }
      
      if (!uics_.isHeadless()) { // not headless
        myLsSup_.getFprep(dacx_).setPreference("ImportDirectory", useFile.getAbsoluteFile().getParent());
        XPlatMaskingStatus xpms = uics_.getCommonView().calcDisableControls(MainCommands.GENERAL_PUSH, true);
        uics_.getCommonView().disableControls(xpms);
      }
      
      UndoSupport support = uFac_.provideUndoSupport("undo.buildFromCSV", rcx_);
      FullHierarchyCSVFormatFactory fhcsv;
      FullHierarchyBuilder.BIPData bipd;
     
      try {
        fhcsv = new FullHierarchyCSVFormatFactory(uics_, rcx_);
        if (useFile != null) {
          bipd = fhcsv.buildFromCSVForeground(useFile, (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT), support);
        } else {
          bipd = fhcsv.buildFromCSVForegroundStream(useStream, (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT), modelIDMap, support);
        }
        if (bipd == null) {
          uics_.getCommonView().enableControls();
          finishedImport(new LinkRouter.RoutingResult(), null, null);
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
      } catch (InvalidInputException iiex) {
        if (support != null) {
          support.finish(); // does this really belong here?
        }
        uics_.getCommonView().enableControls(true);
        finishedImport(null, null, iiex);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));                         
      } catch (IOException ioex) {
        if (support != null) { 
          support.finish(); // does this really belong here?
        }
        uics_.getCommonView().enableControls(true);
        finishedImport(null, ioex, null);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      
      //
      // Need to ge the user CSV name for the top model:
      //
      
      String dbGenomeCSVName = null;
      if (modelIDMap != null) {
        String dbGenomeKey = rcx_.getCurrentGenomeID();
        Iterator<String> kit = modelIDMap.keySet().iterator();
        while (kit.hasNext()) {
          String cSVName = kit.next();
          String modelID = modelIDMap.get(cSVName);
          if (modelID.equals(dbGenomeKey)) {
            dbGenomeCSVName = cSVName;
            break;
          }
        }
      }

      CSVImportRunner runner = new CSVImportRunner(uics_, uFac_, new StaticDataAccessContext(rcx_), fhcsv, bipd, nodeIDMap, dbGenomeCSVName,
                                                   support, importMode_, doOpt, doSquash, overlayOption, uics_.isHeadless(), 
                                                   specLayout_, params_);

      BackgroundWorkerClient bwc;     
      if (!uics_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(uics_, rcx_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(uics_, rcx_, this, runner, support);
      }
      runner.setClient(bwc);
      // Already disabled->false arg
      bwc.launchWorker(false);
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((uics_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done    
      if (uics_.isHeadless() && headlessFailure_) {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this);  
      }
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
       return;
    }
    
    public void handleCancellation() {
      return;
    }     
      
    public void cleanUpPostRepaint(Object result) {
      headlessFailure_ = !finishedImport((LinkRouter.RoutingResult)result, null, null);
      return;
    }        

    private boolean finishedImport(LinkRouter.RoutingResult result, IOException ioEx, 
                                   InvalidInputException iiex) {
      if (result != null) {
        (new LayoutStatusReporter(uics_, rcx_, result)).doStatusAnnouncements();
      }
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          rcx_.drop();
          uics_.getCommonView().manageWindowTitle(null);
          myLsSup_.newModelOperations(rcx_);
        }
        myLsSup_.getFprep(dacx_).getFileInputError(ioEx).displayFileInputError();
        return (false);                
      }
      if (iiex != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          rcx_.drop();
          uics_.getCommonView().manageWindowTitle(null);
          myLsSup_.newModelOperations(rcx_);
        }
        // FIXME! Still gotta route this to the screen:
        SimpleUserFeedback errSUF = myLsSup_.displayInvalidInputError(iiex);
        // return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
        if (!uics_.isHeadless()) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), errSUF.message, errSUF.title, JOptionPane.ERROR_MESSAGE);
        }  
        return (false);                
      }
      if (rcx_.getGenomeSource().getRootDBGenome() == null) {
        rcx_.drop();
        myLsSup_.newModelOperations(rcx_);
        uics_.getCommonView().manageWindowTitle(null);
        myLsSup_.getFprep(dacx_).getFileInputError(null).displayFileInputError();
        return (false);
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        uics_.getCommonView().manageWindowTitle(chosenFile_);
        myLsSup_.postLoadOperations(true, rcx_, 0, false, false, null);
      }
      LayoutLinkSupport.offerColorFixup(uics_, rcx_, result, uFac_);
      return (true);
    }  
  }   

  /***************************************************************************
  **
  ** Background CSV import (and layout)
  */ 
    
  private static class CSVImportRunner extends BackgroundWorker  {
 
    private FullHierarchyCSVFormatFactory fhcsv_;
    private FullHierarchyBuilder.BIPData bipd_;
    private UndoSupport support_;   
    private StaticDataAccessContext myDacx_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private boolean doOpts_;
    private boolean doSquash_;
    private int overlayOption_;
    private Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap_;
    private String dbGenomeCSVName_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    private UIComponentSource uics_;
    private UndoFactory uFac_;
    
    public CSVImportRunner(UIComponentSource uics,
                           UndoFactory uFac,
                           StaticDataAccessContext dacx,
                           FullHierarchyCSVFormatFactory fhcsv, 
                           FullHierarchyBuilder.BIPData bipd, 
                           Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap, String dbGenomeCSVName,
                           UndoSupport support,
                           SIFImportChoicesDialogFactory.LayoutModes importMode, boolean doOpts, boolean doSquash, 
                           int overlayOption, boolean isHeadless, SpecialtyLayout specLayout, SpecialtyLayoutEngineParams params) {
      super(new LinkRouter.RoutingResult(), isHeadless);
      fhcsv_ = fhcsv;
      bipd_ = bipd;
      support_ = support;
      importMode_ = importMode;
      doOpts_ = doOpts;
      doSquash_ = doSquash; 
      overlayOption_ = overlayOption;
      nodeIDMap_ = nodeIDMap;
      dbGenomeCSVName_ = dbGenomeCSVName;
      specLayout_ = specLayout;
      params_ = params;
      myDacx_ = dacx;
      uics_ = uics;
      uFac_ = uFac;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      // Perhaps not needed, but does allow settings to be changed without messing with myDacx_, so safer to keep for now...
      StaticDataAccessContext rcxR = new StaticDataAccessContext(myDacx_);
      LinkRouter.RoutingResult res = fhcsv_.buildFromCSVBackground(uics_, uFac_, rcxR, bipd_, 
                                                                   (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT),                                                                 
                                                                   support_, doOpts_, doSquash_, overlayOption_, 
                                                                   nodeIDMap_, dbGenomeCSVName_, this, 0.0, 1.0, specLayout_, params_);
      return (res);
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
