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
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
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
  
  public ImportCSV(BTState appState) {
    super(appState);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    StepState retval = new StepState(appState_, dacx);
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState, BackgroundWorkerOwner {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
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
      if (!appState_.isHeadless()) { // not headless
        SIFImportChoicesDialogFactory.BuildArgs ba = new SIFImportChoicesDialogFactory.BuildArgs(false);
        SIFImportChoicesDialogFactory mddf = new SIFImportChoicesDialogFactory(cfh);   
        ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
        nextStep_ = "stepProcessImportChoices";
        return (retval);
      }
      // headless
      specLayout_ = new WorksheetLayout(appState_, true);
      params_ = appState_.getLayoutOptMgr().getDiagLayoutParams();      
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
        specLayout_ = SpecialtyLayoutEngine.getLayout(strat, appState_);          
        boolean showOptions = crq.showOptionDialog;
        if (showOptions) {
          Genome targetGenome = appState_.getDB().getGenome();
          SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = specLayout_.getParameterDialogBuildArgs(targetGenome, null, false);
          SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(cfh);
          ServerControlFlowHarness.Dialog cfhd = cedf.getDialog(ba);
          daipc = new DialogAndInProcessCmd(cfhd, this);
          nextStep_ = "stepExtractParams";
          return (daipc);
        } else {
          params_ = appState_.getLayoutOptMgr().getLayoutParams(strat);   
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
      if (!appState_.isHeadless() && 
          (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) &&
          appState_.hasAnUndoChange()) { 
        ResourceManager rMan = appState_.getRMan();      
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
        
      if (!appState_.isHeadless()) { // not headless      
        FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(appState_, ".csv", "filterName.csv");
        useFile = myLsSup_.getFprep().getExistingImportFile("ImportDirectory", filt);
        if (useFile == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
      } else {
        if (doFile.booleanValue()) {
          if (!myLsSup_.getFprep().checkExistingImportFile(useFile)) {
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
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeID(), 
                                                     ModelChangeEvent.MODEL_DROPPED);     
        appState_.getEventMgr().sendModelChangeEvent(mcev);
        appState_.getCommonView().manageWindowTitle(chosenFile_);
        myLsSup_.newModelOperations(dacx_);
      }
      
      if (!appState_.isHeadless()) { // not headless
        myLsSup_.getFprep().setPreference("ImportDirectory", useFile.getAbsoluteFile().getParent());
        XPlatMaskingStatus xpms = appState_.getCommonView().calcDisableControls(MainCommands.GENERAL_PUSH, true);
        appState_.getCommonView().disableControls(xpms);
      }
      
      UndoSupport support = new UndoSupport(appState_, "undo.buildFromCSV");
      FullHierarchyCSVFormatFactory fhcsv;
      FullHierarchyBuilder.BIPData bipd;
     
      try {
        fhcsv = new FullHierarchyCSVFormatFactory(appState_, dacx_);
        if (useFile != null) {
          bipd = fhcsv.buildFromCSVForeground(useFile, (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT), support);
        } else {
          bipd = fhcsv.buildFromCSVForegroundStream(useStream, (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT), modelIDMap, support);
        }
        if (bipd == null) {
          appState_.getCommonView().enableControls();
          finishedImport(new LinkRouter.RoutingResult(), null, null);
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
      } catch (InvalidInputException iiex) {
        if (support != null) {
          support.finish(); // does this really belong here?
        }
        appState_.getCommonView().enableControls(true);
        finishedImport(null, null, iiex);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));                         
      } catch (IOException ioex) {
        if (support != null) { 
          support.finish(); // does this really belong here?
        }
        appState_.getCommonView().enableControls(true);
        finishedImport(null, ioex, null);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      
      //
      // Need to ge the user CSV name for the top model:
      //
      
      String dbGenomeCSVName = null;
      if (modelIDMap != null) {
        String dbGenomeKey = dacx_.getGenomeID();
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

      CSVImportRunner runner = new CSVImportRunner(appState_, dacx_, fhcsv, bipd, nodeIDMap, dbGenomeCSVName,
                                                   support, importMode_, doOpt, doSquash, overlayOption, appState_.isHeadless(), 
                                                   specLayout_, params_);

      BackgroundWorkerClient bwc;     
      if (!appState_.isHeadless()) { // not headless, true background thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);      
      } else { // headless; on this thread
        bwc = new BackgroundWorkerClient(appState_, this, runner, support);
      }
      runner.setClient(bwc);
      // Already disabled->false arg
      bwc.launchWorker(false);
      // In the server case, this won't execute until thread has returned.  In desktop case, we do not refresh view!
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd((appState_.isHeadless()) ? DialogAndInProcessCmd.Progress.DONE 
                                                                                       : DialogAndInProcessCmd.Progress.DONE_ON_THREAD, this); // Done    
      if (appState_.isHeadless() && headlessFailure_) {
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
        (new LayoutStatusReporter(appState_, result)).doStatusAnnouncements();
      }
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          dacx_.drop();
          appState_.getCommonView().manageWindowTitle(null);
          myLsSup_.newModelOperations(dacx_);
        }
        myLsSup_.getFprep().getFileInputError(ioEx).displayFileInputError();
        return (false);                
      }
      if (iiex != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          dacx_.drop();
          appState_.getCommonView().manageWindowTitle(null);
          myLsSup_.newModelOperations(dacx_);
        }
        // FIXME! Still gotta route this to the screen:
        SimpleUserFeedback errSUF = myLsSup_.displayInvalidInputError(iiex);
        // return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
        if (!appState_.isHeadless()) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), errSUF.message, errSUF.title, JOptionPane.ERROR_MESSAGE);
        }  
        return (false);                
      }
      if (dacx_.getGenomeSource().getGenome() == null) {
        dacx_.drop();
        myLsSup_.newModelOperations(dacx_);
        appState_.getCommonView().manageWindowTitle(null);
        myLsSup_.getFprep().getFileInputError(null).displayFileInputError();
        return (false);
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        appState_.getCommonView().manageWindowTitle(chosenFile_);
        myLsSup_.postLoadOperations(true, dacx_);
      }
      LayoutLinkSupport.offerColorFixup(appState_, dacx_, result);
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
    private BTState myAppState_;
    private DataAccessContext myDacx_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private boolean doOpts_;
    private boolean doSquash_;
    private int overlayOption_;
    private Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap_;
    private String dbGenomeCSVName_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    
    public CSVImportRunner(BTState appState, DataAccessContext dacx,
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
      myAppState_ = appState;
      importMode_ = importMode;
      doOpts_ = doOpts;
      doSquash_ = doSquash; 
      overlayOption_ = overlayOption;
      nodeIDMap_ = nodeIDMap;
      dbGenomeCSVName_ = dbGenomeCSVName;
      specLayout_ = specLayout;
      params_ = params;
      myDacx_ = dacx;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      DataAccessContext rcxR = new DataAccessContext(myAppState_);
      LinkRouter.RoutingResult res = fhcsv_.buildFromCSVBackground(myAppState_, rcxR, bipd_, 
                                                                   (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT),                                                                 
                                                                   support_, doOpts_, doSquash_, overlayOption_, 
                                                                   nodeIDMap_, dbGenomeCSVName_, this, 0.0, 1.0, specLayout_, params_);
      return (res);
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
