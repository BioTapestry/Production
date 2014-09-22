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


package org.systemsbiology.biotapestry.cmd.flow.io;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportPublish;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.nav.RecentFilesManager;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.parser.SUParser;
import org.systemsbiology.biotapestry.perturb.PerturbCsvFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.PerturbedTimeCourseGeneCSVFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseFormatFactory;
import org.systemsbiology.biotapestry.ui.ImageExporter;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.ExportSettingsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.FileChooserWrapperFactory;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.DesignBatchKeyDialog;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of IO Operations
*/

public class LoadSaveOps extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum IOAction {
    EXPORT("command.Export", "command.Export", "Export24.gif", "command.ExportMnem", null),
    SAVE("command.Save", "command.Save", "Save24.gif", "command.SaveMnem", "command.SaveAccel"),
    SAVE_AS("command.SaveAs", "command.SaveAs", "SaveAs24.gif", "command.SaveAsMnem", null),
    CLEAR_RECENT("command.clearRecent", "command.clearRecent", "FIXME24.gif", "command.clearRecentMnem", null),
    LOAD("command.Load", "command.Load", "FIXME24.gif", "command.LoadMnem", "command.LoadAccel"),
    NEW_MODEL("command.NewModel", "command.NewModel", "New24.gif", "command.NewModelMnem", "command.NewModelAccel"),
    TEMPORAL_INPUT("command.ImportTemporalInputXml", "command.ImportTemporalInputXml", "FIXME24.gif", "command.ImportTemporalInputXmlMnem", null),
    EMBRYO_COUNTS("command.ImportCountsPerEmbryoXml", "command.ImportCountsPerEmbryoXml", "FIXME24.gif", "command.ImportCountsPerEmbryoXmlMnem", null), 
    PERT_EXPRESS("command.ImportPerturbedExpressionCSV", "command.ImportPerturbedExpressionCSV", "FIXME24.gif", "command.ImportPerturbedExpressionCSVMnem", null),
    TIME_COURSE("command.ImportTimeCourseXml", "command.ImportTimeCourseXml", "FIXME24.gif", "command.ImportTimeCourseXmlMnem", null),
    PERTURB("command.ImportPerturbCsv", "command.ImportPerturbCsv", "FIXME24.gif", "command.ImportPerturbCsvMnem", null),
    LOAD_RECENT(),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    IOAction(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    } 
    
    IOAction() {
      this.name_ = null;  
      this.desc_ = null;
      this.icon_ = null;
      this.mnem_ = null;
      this.accel_ = null;
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
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private IOAction action_;
  private String filePath_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LoadSaveOps(BTState appState, IOAction action) {
    super(appState);
    if (action.equals(IOAction.LOAD_RECENT)) {
      throw new IllegalArgumentException();
    }
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
   
  public LoadSaveOps(BTState appState, IOAction action, FileArg args) {
    super(appState);
    if (!action.equals(IOAction.LOAD_RECENT)) {
      throw new IllegalArgumentException();
    }
    this.name = args.getName();
    this.desc = args.getName();
    icon =  null;
    mnem =  null;
    accel =  null;
    action_ = action;
    filePath_ = args.getFilePath();
  }

  /***************************************************************************
  **
  ** Answer if enabling is handled externally, or through the "isEnabled" route:
  ** 
  */
  
  @Override  
  public boolean externallyEnabled() {
    return (action_ == IOAction.CLEAR_RECENT);
  }
   
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case SAVE:
      case SAVE_AS:      
      case LOAD:
      case NEW_MODEL:
      case LOAD_RECENT:
      case TEMPORAL_INPUT:
      case EMBRYO_COUNTS:
      case TIME_COURSE:
      case PERTURB:
        return (true);
      case EXPORT:
        return (cache.genomeNotNull());      
      case PERT_EXPRESS:   
        //
        // Gotta have time course data and perturbations defined for this
        // to work!
        //
        return (cache.haveTimeCourseData() && cache.timeCourseNotEmpty() && 
                cache.hasPerturbationData() && cache.hasPertSources());
      default:
      case CLEAR_RECENT:
        throw new IllegalStateException();
    }
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
    StepState retval = new StepState(appState_, action_, filePath_, dacx);
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
        ans = new StepState(appState_, action_, filePath_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.cfh == null) {
        ans.cfh = cfh;
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else if (ans.getNextStep().equals("stepToProcessPostWarn")) {
        next = ans.stepToProcessPostWarn();               
      } else if (ans.getNextStep().equals("stepToLoad")) {
        next = ans.stepToLoad();
      } else if (ans.getNextStep().equals("stepToCheck")) {
        next = ans.stepToCheck(last);
      } else if (ans.getNextStep().equals("stepExtractFileChoice")) {
        next = ans.stepExtractFileChoice(last);  
      } else if (ans.getNextStep().equals("stepToProcessUsingFile")) {
        next = ans.stepToProcessUsingFile();      
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
    private IOAction myAction_;
    private BTState appState_;
    private LoadSaveSupport myLsSup_;
    private Object[] headlessArgs_;
    private String myFilePath_;
    private ServerControlFlowHarness cfh;
    private File myFile_;
    private DynamicDataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, IOAction action, String filePath, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      myLsSup_ = appState_.getLSSupport();
      myFilePath_ = filePath;
      nextStep_ = "stepToProcess";
      myFile_ = null;
      dacx_ = new DynamicDataAccessContext(dacx, appState);
    }
     
    /***************************************************************************
    **
    ** Set the popup params
    */ 
        
    public void setParams(Object[] args) {
      headlessArgs_ = args;
      return;
    }
       
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {          
      switch (myAction_) {
        //
        // These guys can get right to work
        //
        case EXPORT:
          if (!doAnExport()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          break;
        case SAVE: 
          doASave();
          break;
        case SAVE_AS: 
          if (!doASaveAs()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          break;
        case CLEAR_RECENT: 
          doClearRecent();
          break;
        case PERT_EXPRESS:
          return (doPerturbedExpressionGetFileChoice());
        //
        // These guys might issue warning dialogs before doing any work:
        //
        case NEW_MODEL: 
        case LOAD_RECENT:
        case LOAD:
          return (stepUndoWarning());
        case TEMPORAL_INPUT:
          return (stepTemporalInputWarning());
        case EMBRYO_COUNTS:
          return (stepCopiesPerEmbryoWarning());
        case TIME_COURSE:
          return (stepTimeCourseWarning());
        case PERTURB:
          return (stepPerturbWarning());
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    
    /***************************************************************************
    **
    ** Do the step after doing a warning
    */ 
       
    private DialogAndInProcessCmd stepToProcessPostWarn() {          
      switch (myAction_) { 
        case LOAD_RECENT: 
          return (doLoadRecent()); 
        case LOAD:
          return (stepToLoad());
        case NEW_MODEL: 
          doNewModel();
          break;
        case TEMPORAL_INPUT:
          return (doTemporalInputGetFileChoice());
        case EMBRYO_COUNTS:
          return (doCopiesPerEmbryoGetFileChoice());
        case TIME_COURSE:
          doTimeCourse();
          break;
        case PERTURB:
          return (doPerturbGetFileChoice());
        //
        // These guys did not issue warnings:
        //
        case EXPORT:   
        case SAVE:     
        case SAVE_AS:    
        case CLEAR_RECENT:        
        case PERT_EXPRESS:
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do the step after getting the file!
    */ 
       
    private DialogAndInProcessCmd stepToProcessUsingFile() {          
      switch (myAction_) {
        case EXPORT:
          if (!doAnExport()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          break;
        case SAVE: 
          doASave();
          break;
        case SAVE_AS: 
          if (!doASaveAs()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          break;
        case LOAD_RECENT: 
          doLoadRecent();
          break;  
        case CLEAR_RECENT: 
          doClearRecent();
          break;  
        case LOAD:
          return (stepFinalLoadFromFile());
        case NEW_MODEL: 
          doNewModel();
          break;
        case TEMPORAL_INPUT:
          return (doImportTemporalInputXml());
        case EMBRYO_COUNTS:
          return (doCopiesPerEmbryo());
        case TIME_COURSE:
          doTimeCourse();
          break;
        case PERT_EXPRESS:
          return (doPerturbedExpression());
        case PERTURB:
          return (doPerturb());
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
      
    /***************************************************************************
    **
    ** Command
    */
    
    private boolean doAnExport() {        
      SUPanel sup = appState_.getSUPanel();      
      Dimension dim = appState_.getZoomTarget().getBasicSize(true, true, ZoomTarget.VISIBLE_MODULES);
      ExportPublish.ExportSettings set;
      if (!appState_.isHeadless()) {
        ExportSettingsDialog esd = new ExportSettingsDialog(appState_, appState_.getTopFrame(), dim.width, dim.height);
        esd.setVisible(true);
        set = (ExportPublish.ExportSettings)esd.getUserInputs();
        if (set == null) {
          return (true);
        }
      } else {
        set = (ExportPublish.ExportSettings)headlessArgs_[0];
      }
      
      File file = null;
      OutputStream stream = null;
      
      if (!appState_.isHeadless()) {  // not headless...       
        List<String> supported = ImageExporter.getSupportedFileSuffixes();
        ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
        filts.add(new FileExtensionFilters.MultiExtensionFilter(appState_, supported, "filterName.img"));
        String pref = ImageExporter.getPreferredSuffixForType(set.formatType);
        file = myLsSup_.getFprep().getOrCreateWritableFileWithSuffix("ExportDirectory", filts, supported, pref);   
        if (file == null) {
          return (true);
        }
      } else {
        if (((Boolean)headlessArgs_[1]).booleanValue()) {
          file = new File((String)headlessArgs_[2]);
          if (!myLsSup_.getFprep().checkWriteFile(file, true)) {  // Legacy, but why check if headless?
            return (false);
          }
        } else {
          stream = (OutputStream)headlessArgs_[2];
        }
      }
      try {
        if (file != null) {
          sup.exportToFile(file, false, set.formatType, set.res, set.zoomVal, set.size, appState_);
        } else {
          sup.exportToStream(stream, false, set.formatType, set.res, set.zoomVal, set.size, appState_);
        }
        if (!appState_.isHeadless()) {
          myLsSup_.getFprep().setPreference("ExportDirectory", file.getAbsoluteFile().getParent());
        }
        return (true);
      } catch (IOException ioe) {
        myLsSup_.getFprep().displayFileOutputError();
        return (false);
      }
    }
   
    /***************************************************************************
    **
    ** Command
    */ 
 
    private void doASave() { 
      try {
        File cf = appState_.getCurrentFile();
        if (cf != null) {
           Database db = appState_.getDB();
           PrintWriter out
             = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cf), "UTF-8")));
           Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);
           db.writeXML(out, ind);
           out.close();
           appState_.clearUndoTracking();
        } else {
          myLsSup_.saveToFile(null);
        }
      } catch (IOException ioe) {
        myLsSup_.getFprep().displayFileOutputError();
      }
      return;
    } 
  
    /***************************************************************************
    **
    ** Command
    */ 
   
    public boolean doASaveAs() {
      if (appState_.isWebApplication() || !appState_.isHeadless()) {
        return (myLsSup_.saveToFile(null));
      } else {
        if (((Boolean)headlessArgs_[0]).booleanValue()) {
          String fileName = (String)headlessArgs_[1];
          return (myLsSup_.saveToFile(fileName));
        } else {
          OutputStream stream = (OutputStream)headlessArgs_[1];
          try {
            myLsSup_.saveToOutputStream(stream);
          } catch (IOException ioe) {
            myLsSup_.getFprep().displayFileOutputError(); // Which is kinda bogus...
            return (false);
          }
          return (true);
        }
      }
    } 
    
    /***************************************************************************
    **
    ** Command
    */ 
   
    private DialogAndInProcessCmd doLoadRecent() {
      File file = new File(myFilePath_);
      if (!myLsSup_.getFprep().checkExistingImportFile(file)) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      FilePreparer.FileInputResultClosure firc = myLsSup_.loadFromFile(file, dacx_);
      if (firc.wasSuccessful()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } else {
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      }
    }
  
    /***************************************************************************
    **
    ** Command
    */ 
   
    private void doClearRecent() {    
      RecentFilesManager rfm = appState_.getRecentFilesMgr();
      rfm.drop();
      appState_.getRecentMenu().clearRecentMenu();
      return;
    }
    
    /***************************************************************************
    **
    ** Warn of undo
    */
      
    private DialogAndInProcessCmd stepUndoWarning() {
      DialogAndInProcessCmd daipc;
      if ((appState_.isWebApplication() || !appState_.isHeadless()) && appState_.hasAnUndoChange()) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("loadAction.warningMessage");
        String title = rMan.getString("loadAction.warningMessageTitle");
        Map<String,String> clickActions = new HashMap<String,String>();
        clickActions.put("yes", "MAIN_LOAD");
        clickActions.put("no", "MAIN_LOAD");
        
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title,clickActions);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToProcessPostWarn";
      }       
      return (daipc);     
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepTimeCourseWarning() {
      DialogAndInProcessCmd daipc;    
      TimeCourseData tcdat = dacx_.getExpDataSrc().getTimeCourseData();
      if ((tcdat != null) && (tcdat.haveData())) {
        String message = dacx_.rMan.getString("importTCXml.warningMessage");
        String title = dacx_.rMan.getString("importTCXml.warningMessageTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToProcessPostWarn";
      } 
      return (daipc);     
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepCopiesPerEmbryoWarning() {
      DialogAndInProcessCmd daipc;    
      CopiesPerEmbryoData cpedat = dacx_.getExpDataSrc().getCopiesPerEmbryoData();
      if ((cpedat != null) && (cpedat.haveData())) {
        String message = dacx_.rMan.getString("importCPEXml.warningMessage");
        String title = dacx_.rMan.getString("importCPEXml.warningMessageTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";  
     } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToProcessPostWarn";
      } 
      return (daipc); 
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
  
    private DialogAndInProcessCmd stepTemporalInputWarning() {
      DialogAndInProcessCmd daipc;     
      TemporalInputRangeData tirdat = dacx_.getExpDataSrc().getTemporalInputRangeData();
      if ((tirdat != null) && (tirdat.haveData())) {
        String message = dacx_.rMan.getString("importTIXml.warningMessage");
        String title = dacx_.rMan.getString("importTIXml.warningMessageTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToProcessPostWarn";
      } 
      return (daipc);    
    }
        
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepPerturbWarning() {
      DialogAndInProcessCmd daipc;
      CommonView cview = appState_.getCommonView();  
      if (cview.havePerturbationEditsInProgress()) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("importPertCSV.droppingPendingEdits");
        String title = rMan.getString("importPertCSV.droppingPendingEditsTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToProcessPostWarn";
      } 
      return (daipc);
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepToCheck(DialogAndInProcessCmd daipc) {      
      int choiceVal = daipc.suf.getIntegerResult();     
      if (choiceVal == SimpleUserFeedback.NO) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
        return (retval);
      }
      DialogAndInProcessCmd ret = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToProcessPostWarn";
      return (ret);     
    }
   
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepExtractFileChoice(DialogAndInProcessCmd cmd) {     
      FileChooserWrapperFactory.FileChooserResults crq = (FileChooserWrapperFactory.FileChooserResults)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
      if (appState_.isWebApplication()) {
        String scrubbedName = (crq.fileName.indexOf(File.separatorChar) == -1) ? crq.fileName : null;
        if (scrubbedName == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
        }
        myFile_ = new File(appState_.getFullServletContextPath() + appState_.getServerBtpDirectory(), scrubbedName);
      } else {
        myFile_ = new File(crq.filePath, crq.fileName);
      }
      DialogAndInProcessCmd ret = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToProcessUsingFile";
      return (ret);     
    }
    
    
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepFinalLoadFromFile() {     
      FilePreparer.FileInputResultClosure firc = myLsSup_.loadFromFile(myFile_, dacx_);
      if (firc.wasSuccessful()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } else {
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      }
    }
    
   /***************************************************************************
    **
    ** Command
    *
    */ 
 
    private DialogAndInProcessCmd stepToLoad() {
      File file = null;     
      if (headlessArgs_ == null) { // Note args may be non-null even though not headless (Embedded Viewer Panel)         
        FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(appState_, ".btp", "filterName.btp");
        FileChooserWrapperFactory.BuildArgs ba = 
           new FileChooserWrapperFactory.BuildArgs(FileChooserWrapperFactory.BuildArgs.DialogMode.EXISTING_IMPORT,
                                                   appState_, myLsSup_.getFprep(), filt, "LoadDirectory");
        FileChooserWrapperFactory nopd = new FileChooserWrapperFactory(cfh);
        ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
        nextStep_ = "stepExtractFileChoice";
        return (retval);
      } else {
        if (((Boolean)headlessArgs_[0]).booleanValue()) {
          file = new File((String)headlessArgs_[1]);
          if (!myLsSup_.getFprep().checkExistingImportFile(file)) {           
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          FilePreparer.FileInputResultClosure firc = myLsSup_.loadFromFile(file, dacx_);
          if (firc.wasSuccessful()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
          } else {
            SimpleUserFeedback errSUF = firc.getSUF();
            return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
          }
        } else {
          FilePreparer.FileInputResultClosure firc = myLsSup_.loadFromStream((InputStream)headlessArgs_[1], dacx_);
          if (firc.wasSuccessful()) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
          } else {
            SimpleUserFeedback errSUF = firc.getSUF();
            return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
          }
        }
      }
    }
  
    /***************************************************************************
    **
    ** Command
    */ 
 
    private void doNewModel() {
      appState_.getCommonView().manageWindowTitle(null);
      myLsSup_.newModelOperations(dacx_);
      return;
    }
       
    /***************************************************************************
    **
    ** Command
    */ 
 
    private void doTimeCourse() {
      if (!myLsSup_.prepTimeAxisForDataImport(dacx_)) {
        return;
      }    
      ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
      filters.add(new FileExtensionFilters.SimpleFilter(appState_, ".xml", "filterName.xml"));
      filters.add(new FileExtensionFilters.SimpleFilter(appState_, ".btp", "filterName.btp"));         
      File file = myLsSup_.getFprep().getExistingImportFile("ImportDirectory", filters);
      if (file == null) {
        return;
      }
      
      DatabaseChange dc = null;
      try {
        UndoSupport support = new UndoSupport(appState_, "undo.tcxml");
        dc = dacx_.getExpDataSrc().startTimeCourseUndoTransaction();
        ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
        alist.add(new TimeCourseFormatFactory(appState_, true, true));
        SUParser sup = new SUParser(appState_, alist);
        sup.parse(file);
        dc = dacx_.getExpDataSrc().finishTimeCourseUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
        support.finish();
        myLsSup_.getFprep().setPreference("ImportDirectory", file.getAbsoluteFile().getParent());
      } catch (IOException ioe) {
        if (dc != null) {
          dacx_.getExpDataSrc().rollbackDataUndoTransaction(dc);
        }
        myLsSup_.getFprep().getFileInputError(ioe).displayFileInputError();
      }
      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      // FIX ME!!! Gotta send out event so CommonView knows to refresh current loaded model!   
      return;
    }
  
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doPerturbedExpressionGetFileChoice() { 
      FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(appState_, ".csv", "filterName.csv");
      FileChooserWrapperFactory.BuildArgs ba = 
           new FileChooserWrapperFactory.BuildArgs(FileChooserWrapperFactory.BuildArgs.DialogMode.EXISTING_IMPORT,
                                                   appState_, myLsSup_.getFprep(), filt, "ImportDirectory");
      FileChooserWrapperFactory nopd = new FileChooserWrapperFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractFileChoice";
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doPerturbedExpression() {   
      try {
        PerturbedTimeCourseGeneCSVFormatFactory csvff= new PerturbedTimeCourseGeneCSVFormatFactory(appState_, dacx_);
        if (csvff.readPerturbedExpressionCSV(myFile_, appState_)) {
          myLsSup_.getFprep().setPreference("ImportDirectory", myFile_.getAbsoluteFile().getParent());
        }
      } catch (InvalidInputException iiex) {
        SimpleUserFeedback errSUF = myLsSup_.displayInvalidInputError(iiex); 
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      } catch (IOException ioe) {
        FilePreparer.FileInputResultClosure firc = myLsSup_.getFprep().getFileInputError(ioe);
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }

    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doCopiesPerEmbryoGetFileChoice() {
      if (!myLsSup_.prepTimeAxisForDataImport(dacx_)) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
        return (retval);
      }
      ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
      filts.add(new FileExtensionFilters.SimpleFilter(appState_, ".xml", "filterName.xml"));
      filts.add(new FileExtensionFilters.SimpleFilter(appState_, ".btp", "filterName.btp"));     
      FileChooserWrapperFactory.BuildArgs ba = 
           new FileChooserWrapperFactory.BuildArgs(FileChooserWrapperFactory.BuildArgs.DialogMode.EXISTING_IMPORT,
                                                   appState_, myLsSup_.getFprep(), filts, "ImportDirectory");
      FileChooserWrapperFactory nopd = new FileChooserWrapperFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractFileChoice";
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doCopiesPerEmbryo() {
      DatabaseChange dc = null;
      try {
        UndoSupport support = new UndoSupport(appState_, "undo.cpexml");
        dc = dacx_.getExpDataSrc().startCopiesPerEmbryoUndoTransaction();
        ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
        alist.add(new CopiesPerEmbryoFormatFactory(appState_, true));
        SUParser sup = new SUParser(appState_, alist);
        sup.parse(myFile_);
        dc = dacx_.getExpDataSrc().finishCopiesPerEmbryoUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
        support.finish();
        myLsSup_.getFprep().setPreference("ImportDirectory", myFile_.getAbsoluteFile().getParent());
      } catch (IOException ioe) {
        if (dc != null) {
          dacx_.getExpDataSrc().rollbackDataUndoTransaction(dc);
        }
        FilePreparer.FileInputResultClosure firc = myLsSup_.getFprep().getFileInputError(ioe);
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      }
      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      // FIX ME!!! Gotta send out event so CommonView knows to refresh current loaded model!
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }

    /***************************************************************************
    **
    ** Command
    */ 
  
    private DialogAndInProcessCmd doTemporalInputGetFileChoice() {
      if (!myLsSup_.prepTimeAxisForDataImport(dacx_)) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
        return (retval);
      }            
      ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
      filts.add(new FileExtensionFilters.SimpleFilter(appState_, ".xml", "filterName.xml"));
      filts.add(new FileExtensionFilters.SimpleFilter(appState_, ".btp", "filterName.btp"));     
      FileChooserWrapperFactory.BuildArgs ba = 
           new FileChooserWrapperFactory.BuildArgs(FileChooserWrapperFactory.BuildArgs.DialogMode.EXISTING_IMPORT,
                                                   appState_, myLsSup_.getFprep(), filts, "ImportDirectory");
      FileChooserWrapperFactory nopd = new FileChooserWrapperFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractFileChoice";
      return (retval);
    }
      
   /***************************************************************************
    **
    ** Command
    */ 
  
    private DialogAndInProcessCmd doImportTemporalInputXml() {
      DatabaseChange dc = null;
      try {
        UndoSupport support = new UndoSupport(appState_, "undo.tixml");
        dc = dacx_.getExpDataSrc().startTemporalInputUndoTransaction();
        ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
        alist.add(new TemporalInputRangeFormatFactory(appState_, true));
        SUParser sup = new SUParser(appState_, alist);
        sup.parse(myFile_);
        dc = dacx_.getExpDataSrc().finishTemporalInputUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(appState_, dacx_, dc));
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
        support.finish();
        myLsSup_.getFprep().setPreference("ImportDirectory", myFile_.getAbsoluteFile().getParent());
      } catch (IOException ioe) {
        if (dc != null) {
          dacx_.getExpDataSrc().rollbackDataUndoTransaction(dc);
        }
        FilePreparer.FileInputResultClosure firc = myLsSup_.getFprep().getFileInputError(ioe);
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      }
      // FIX ME??? Use events instead of direct calls.
      dacx_.getGenomeSource().clearAllDynamicProxyCaches();
      // FIX ME!!! Gotta send out event so CommonView knows to refresh current loaded model!
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doPerturbGetFileChoice() {
      CommonView cview = appState_.getCommonView();  
      if (cview.havePerturbationEditsInProgress()) {
        cview.dropAllPendingPerturbationEdits(); 
      }      
      FileExtensionFilters.SimpleFilter filt = new FileExtensionFilters.SimpleFilter(appState_, ".csv", "filterName.csv");
      FileChooserWrapperFactory.BuildArgs ba = 
        new FileChooserWrapperFactory.BuildArgs(FileChooserWrapperFactory.BuildArgs.DialogMode.EXISTING_IMPORT, 
                                                appState_, myLsSup_.getFprep(), filt, "ImportDirectory");
      FileChooserWrapperFactory nopd = new FileChooserWrapperFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = nopd.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractFileChoice";
      return (retval);
    }   

    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd doPerturb() {          
      if (myFile_ == null) {
        throw new IllegalStateException();
      } 
      try {
        // STILL MORE UI TO PORT!
        DesignBatchKeyDialog dbkd = new DesignBatchKeyDialog(appState_);
        dbkd.setVisible(true);
        if (!dbkd.haveResult()) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
        }
        boolean useDate = dbkd.useDate();
        boolean useTime = dbkd.useTime();
        boolean useBatch = dbkd.useBatch();
        boolean useInvest = dbkd.useInvest();  
        boolean useCondition = dbkd.useCondition();
        
        PerturbCsvFormatFactory csvff = new PerturbCsvFormatFactory(appState_, dacx_, useDate, useTime, useBatch, 
                                                                    useInvest, useCondition);
        // SORRY LOTS OF DIALOGS BURIED DOWN IN THIS CALL:
        if (csvff.parsePerturbCSV(myFile_)) {
          myLsSup_.getFprep().setPreference("ImportDirectory", myFile_.getAbsoluteFile().getParent());
        }
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } catch (IOException ioe) {
        FilePreparer.FileInputResultClosure firc = myLsSup_.getFprep().getFileInputError(ioe);
        SimpleUserFeedback errSUF = firc.getSUF();
        return (new DialogAndInProcessCmd(errSUF, this, DialogAndInProcessCmd.Progress.DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK));
      } catch (OutOfMemoryError oom) {
        appState_.getExceptionHandler().displayOutOfMemory(oom);
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }      
    }
  }
    
  /***************************************************************************
  **
  ** Control Flow Argument
  */  

  public static class FileArg extends AbstractOptArgs {
      
    private static String[] keys_;
    
    static {
      keys_ = new String[] {"name", "filePath"};  
    }
    
    public String getName() {
      return (getValue(0));
    }
    
    public String getFilePath() {     
      return (getValue(1));
    }

    public FileArg(Map<String, String> argMap) throws IOException {   
      super(argMap);
      //
      // This is used for the web application. Do not allow a file path. Do not allow the name to contain a file path.
      //
      String name = getValue(0);
      if (name.indexOf(File.separator) != -1) {
        throw new IOException();
      }
      dropValue(1);
      setValue(1, "");
    }
  
    protected String[] getKeys() {
      return (keys_);
    }

    public FileArg(String name, String filePath) {
      super();
      setValue(0, name);
      setValue(1, filePath);
      bundle();
    }
  }  
}
