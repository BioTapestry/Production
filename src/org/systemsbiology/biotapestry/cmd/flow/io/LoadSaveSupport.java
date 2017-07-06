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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.ArgParser;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.PathAndFileSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabChange;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.app.VirtualPathControls;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.flow.tabs.TabOps;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.undo.TabChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.MasterFactory;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.genome.DBGenomeSIFFormatFactory;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.RecentFilesManager;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.parser.SUParser;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.LoginDialog;
import org.systemsbiology.biotapestry.ui.dialogs.SIFImportChoicesDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.TimeAxisSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.LayoutStatusReporter;
import org.systemsbiology.biotapestry.ui.dialogs.utils.MessageTableReportingDialog;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayout;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Support operations for loads and saves
*/

public class LoadSaveSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private UIComponentSource uics_;
  private Class<?> prefClass_;
  private UndoFactory uFac_;
  private DynamicDataAccessContext ddacx_;
  private TabSource tSrc_;
  private CmdSource cSrc_;
  private PathAndFileSource pafs_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LoadSaveSupport(UIComponentSource uics, DynamicDataAccessContext ddacx, 
                         TabSource tSrc, UndoFactory uFac, CmdSource cSrc,
                         PathAndFileSource pafs, Class<?> prefClass) {
    uics_ = uics;
    uFac_ = uFac;
    tSrc_ = tSrc;
    ddacx_ = ddacx;
    pafs_ = pafs;
    cSrc_ = cSrc;
    prefClass_ = prefClass;
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get a network builder
  */ 
    
  public NetworkBuilder getNetworkBuilder() {
    return (new NetworkBuilder());
  }  
  
  /***************************************************************************
  **
  ** Get file preparer
  */ 
    
  public FilePreparer getFprep(DataAccessContext dacx) {   
    return (new FilePreparer(uics_, ddacx_, uics_.getTopFrame(), prefClass_));
  }  
  
  /***************************************************************************
  **
  ** Set the current file; updates the recent file list too
  */ 
    
  public void setCurrentFile(File file) {
    pafs_.setCurrentFile(file);
    if (file == null) {
      return;
    }
    if (!uics_.isHeadless()) {
      FilePreparer fprep = getFprep(ddacx_);
      fprep.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
      if (uics_.getIsEditor()) {
        if (!uics_.getRecentMenu().haveRecentMenu()) {
          return;
        }
        RecentFilesManager rfm = pafs_.getRecentFilesMgr();
        rfm.updateLatestFile(file);
        uics_.getRecentMenu().updateRecentMenu();
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get figure out if we have a successful password acceptance
  */
  
  public PasswordEvaluation processConnectionHeader(URL url, String args) throws IOException {
    //
    // Send the arguments as POST
    //
        
    URLConnection connect = url.openConnection();
    connect.setDoOutput(true);
    PrintWriter out = new PrintWriter(connect.getOutputStream());
    out.print(args);
    out.close();
    
    //
    // Get the warning header, if present
    //
    
    PasswordEvaluation retval = new PasswordEvaluation();
    String warningValue = connect.getHeaderField("Warning");
    retval.failure = ((warningValue != null) && warningValue.equals("717 Failed Login"));
    retval.stream = connect.getInputStream();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Load the given remote genome.  We return false if we need to try again.
  */  
  
  public FilePreparer.FileInputResultClosure loadRemoteGenome(URL url, URL saltUrl, boolean isViewer) {

    PasswordEvaluation pEval = null;
    CommonView cv = uics_.getCommonView();   
    FilePreparer fprep = getFprep(ddacx_);
    SUPanel sup = uics_.getSUPanel();
    
    try {
      if (saltUrl != null) {
        // If user cancels we will not try again
        String args = getPasswordString(saltUrl);
        if (args != null) {
          pEval = processConnectionHeader(url, args);
        }
      }
    } catch (IOException ioe) {
      ddacx_.drop();
      cv.manageWindowTitle(null);
      newModelOperations(ddacx_);
      sup.drawModel(true);     
      FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(ioe);
      retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
      return (retval);              
    }

    ddacx_.drop();
    ddacx_.getWorkspaceSource().setWorkspaceNeedsCenter();
    ddacx_.getExpDataSrc().installLegacyTimeAxisDefinition(ddacx_);
    ddacx_.getGSM().drop();
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(new MasterFactory(isViewer, false, null, ddacx_, uics_, tSrc_, uFac_));
    SUParser supr = new SUParser(ddacx_, alist);

    //
    // This is new code to fix both BT-06-15-05:3 and BT-06-15-05:1
    //
    try {
       if (pEval != null) {
         supr.parse(pEval.stream);
       } else {
         supr.parse(url);
       }
       ddacx_.getMetabase().legacyIOFixup(uics_.getTopFrame(), 0, 1);
       // If we have a file path, make it the current file
       if (url.getProtocol().toLowerCase().equals("file")) {
         try {
           String path = url.getPath();
           path = URLDecoder.decode(path, "UTF-8");         
           if (!path.trim().equals("")) {
             setCurrentFile(new File(path));
           }
         } catch (UnsupportedEncodingException ueex) {
           throw new IllegalStateException();
         }
       }
    } catch (IOException ioe) {
       ddacx_.drop();
       cv.manageWindowTitle(null);
       newModelOperations(ddacx_);
       sup.drawModel(true);
       FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(ioe);
       retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
       return (retval);  
    }
    if (ddacx_.getDBGenome() == null) {
      UiUtil.fixMePrintout("Gotta kill off dead tabs!");
      ddacx_.drop();
      cv.manageWindowTitle(null);
      newModelOperations(ddacx_);
      sup.drawModel(true);
      FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(null);
      retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
      return (retval);  
    }
    cv.manageWindowTitle(url.toString());
    postLoadOperations(false, 0, true, false, null);
    sup.drawModel(true);
    FilePreparer.FileInputResultClosure retval = new FilePreparer.FileInputResultClosure();
    retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Handle passwording
  */  
  
  public String getPasswordString(URL saltUrl) {
    LoginDialog ld = new LoginDialog(uics_, ddacx_);
    ld.setVisible(true);
    if (!ld.haveResult()) {
      return (null); 
    }

    String user = ld.getUserName();
    char[] password = ld.getPassword();      
    String passAsString = new String(password);  // Not thrilled about this...
    for (int i = 0; i < password.length; i++) {
      password[i] = 0;
    }      
    return (DataUtil.buildPasswordString(saltUrl, user, passAsString));
  }  
 
  /***************************************************************************
  **
  ** Figure out if we have a URL from the command line.  Returns null if no URL
  ** was provided.  Throws exception if ALL provided URLS fail.
  */  
  
  public URL getURLFromArgs(Map<String, Object> args) throws MalformedURLException {
    URL retval = null;
    boolean failure = false;
    String resourceSrc = (String)args.get(ArgParser.RES_URL);
    if (resourceSrc != null) {
      retval = getClass().getResource(resourceSrc);
      if (retval == null) {
        failure = true;
      }
    }
    
    if (retval != null) {
      return (retval);
    }
    
    String remSrc = (String)args.get(ArgParser.REM_URL);
    if (remSrc != null) {
      try {
        retval = new URL(remSrc);
      } catch (MalformedURLException mue) {
        failure = true;
      }
    }
    
    if (retval != null) {
      return (retval);
    }      
      
    String fileSrc = (String)args.get(ArgParser.FILE);
    if (fileSrc != null) {
      File fileArg = new File(fileSrc);
      if (fileArg.exists() && fileArg.isFile() && fileArg.canRead()) {
        retval = fileArg.toURI().toURL();  // As recommended in docs, to create escape chars  
      } else {
        failure = true;
      }
    }
      
    if (retval != null) {
      return (retval);
    }
    
    if (failure) {
      throw new MalformedURLException();
    }
    
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Figure out if we have a salt URL from the command line.  Returns null if no URL
  ** was provided.  Throws exception if URL fails.
  */  
  
  public URL getSaltURLFromArgs(Map<String, Object> args) throws MalformedURLException {
    String saltSrc = (String)args.get(ArgParser.USE_PASSWORD);
    if (saltSrc != null) {
      return (new URL(saltSrc));
    }
    return (null);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Gets back relevant URL results
  */ 
    
  private class PasswordEvaluation {
    InputStream stream;
    boolean failure;
  }

  /***************************************************************************
  **
  ** Common load operations
  */ 
    
  public FilePreparer.FileInputResultClosure loadFromFile(File file, boolean forAppend, UndoSupport appendSupport) {
    return (loadFromSource(file, null, forAppend, appendSupport));
  }  
  
  /***************************************************************************
  **
  ** Common load operations
  */ 
    
  public FilePreparer.FileInputResultClosure loadFromStream(InputStream stream, boolean forAppend, UndoSupport appendSupport) {
    return (loadFromSource(null, stream,forAppend, appendSupport));
  }   
  
  /***************************************************************************
  **
  ** Common load operations.  Take your pick of input sources
  */ 
    
  private FilePreparer.FileInputResultClosure loadFromSource(File file, InputStream stream,
                                                             boolean forAppend, UndoSupport appendSupport) {
    String chosenFileName = (file == null) ? null : file.getName();
    CommonView cv = uics_.getCommonView();
    UiUtil.fixMePrintout("Aren't all the models, one for each tab, getting dropped???");
     UiUtil.fixMePrintout("In fact, doesn't all this stuff imply only one tab?");
    ModelChangeEvent mcev = new ModelChangeEvent(ddacx_.getGenomeSource().getID(), ddacx_.getDBGenomeID(),
                                                 ModelChangeEvent.MODEL_DROPPED);     
    uics_.getEventMgr().sendModelChangeEvent(mcev); 
    ddacx_.drop();
    ddacx_.getWorkspaceSource().setWorkspaceNeedsCenter();
    ddacx_.getExpDataSrc().installLegacyTimeAxisDefinition(ddacx_);
    ddacx_.getGSM().drop();
    
    int preLoadCount = tSrc_.getNumTab();
    
    FilePreparer.FileInputResultClosure retval = loadFromSourceGuts(file, stream, false, 0, null);
    if (!retval.wasSuccessful()) {   
      TabOps.clearOutTabs(tSrc_, uics_, preLoadCount);
 
      ddacx_.drop();
      chosenFileName = null;
      cv.manageWindowTitle(null);
      if (!uics_.isHeadless()) {
        UiUtil.fixMePrintout("NOT CLEAN AFTER Database element read error!");
        newModelOperations(ddacx_);
      }
      return (retval);              
    }
 
    if (ddacx_.getGenomeSource() == null) {
      ddacx_.drop();
      chosenFileName = null;
      cv.manageWindowTitle(null);
      if (!uics_.isHeadless()) {
        newModelOperations(ddacx_);
      }
      retval = getFprep(ddacx_).getFileInputError(null);
      return (retval);  
    }
    cv.manageWindowTitle(chosenFileName);
    postLoadOperations(false, 0, true, forAppend, appendSupport);
    return (new FilePreparer.FileInputResultClosure());
  } 

  /***************************************************************************
  **
  ** Load into new tabs next to existing tabs
  */ 
    
  public FilePreparer.FileInputResultClosure addNewTabs(File file, UndoSupport appendSupport) {
    return (addNewTabsFromSource(file, null, appendSupport));
  }
  
  /***************************************************************************
  **
  ** Load into new tabs next to existing tabs
  */ 
    
  private FilePreparer.FileInputResultClosure addNewTabsFromSource(File file, InputStream stream, UndoSupport appendSupport) {
    int preTabCount = tSrc_.getNumTab();
    FilePreparer.FileInputResultClosure retval = loadFromSourceGuts(file, stream, true, preTabCount, appendSupport);
    if (!retval.wasSuccessful()) {
      TabOps.clearOutTabs(tSrc_, uics_, preTabCount);
      return (retval);              
    }
    
    tSrc_.setTabAppendUndoSupport(appendSupport);  
    postLoadOperations(false, preTabCount, true, true, appendSupport);
    tSrc_.setTabAppendUndoSupport(null);
    appendSupport.finish();
    
    return (new FilePreparer.FileInputResultClosure());
  }
  


  /***************************************************************************
  **
  ** Common load operation guts
  */ 
    
  private FilePreparer.FileInputResultClosure loadFromSourceGuts(File file, InputStream stream, boolean isForAppend, int preTabCount, UndoSupport appendSupport) {
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    MasterFactory mf = new MasterFactory(false, isForAppend, appendSupport, ddacx_, uics_, tSrc_, uFac_);
    alist.add(mf);
    SUParser sup = new SUParser(ddacx_, alist);
    setCurrentFile(file);
    try {
      if (isForAppend) {
        ddacx_.getMetabase().setForTabAppend();
      }
      if (file != null) {
        sup.parse(file);
      } else {
        sup.parse(stream);
      }
      if (isForAppend) {
        List<String> mergeIssues = new ArrayList<String>();
        List<String> mfMergeIssues = mf.getMergeIssues();
        if (mfMergeIssues != null) {
          mergeIssues.addAll(mfMergeIssues);
        }
        List<String> mbMergeIssues = ddacx_.getMetabase().doTabAppendPostMerge();
        if (mbMergeIssues != null) {
          mergeIssues.addAll(mbMergeIssues);
        }
        if ((mergeIssues != null) && !mergeIssues.isEmpty()) {
          MessageTableReportingDialog mtrd = 
                   new MessageTableReportingDialog(uics_,
                                                   mergeIssues, 
                                                   "tabMerge.issuesTitle", 
                                                   "tabMerge.issuesHeader", 
                                                   "tabMerge.issuesColumn", 
                                                   new Dimension(1000, 400), false, true);
          mtrd.setVisible(true);
        }
      }      
      int postTabCount = tSrc_.getNumTab();  
      ddacx_.getMetabase().legacyIOFixup((uics_.isHeadless()) ? null : uics_.getTopFrame(), preTabCount, postTabCount);
    } catch (IOException ioe) {
      FilePreparer.FileInputResultClosure retval = getFprep(ddacx_).getFileInputError(ioe);
      return (retval);              
    }
    return (new FilePreparer.FileInputResultClosure());
  }

   
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  boolean saveToFile(String fileName) {

 //   StaticDataAccessContext dacx = new StaticDataAccessContext(ddacx_);
  
    File file = null;
    if (fileName == null) { 
      ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
      filts.add(new FileExtensionFilters.SimpleFilter(ddacx_.getRMan(), ".btp", "filterName.btp"));
      ArrayList<String> suffs = new ArrayList<String>();
      suffs.add("btp");
      file = getFprep(ddacx_).getOrCreateWritableFileWithSuffix("LoadDirectory", filts, suffs, "btp");   
      if (file == null) {
        return (true);
      }
    } else {
      // given a name, we do not check overwrite:
      file = new File(fileName);
      if (!getFprep(ddacx_).checkWriteFile(file, false)) {
        return (false);
      }        
    }

    try {
      saveToOutputStream(new FileOutputStream(file));
      setCurrentFile(file);
      uics_.getCommonView().manageWindowTitle(file.getName());
    } catch (IOException ioe) {
      getFprep(ddacx_).displayFileOutputError();
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  void saveToOutputStream(OutputStream stream) throws IOException {
    Metabase mb = ddacx_.getMetabase();
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);
    mb.writeXML(out, ind);
    out.close();
    cSrc_.clearUndoTracking();
    return;
  }  
   
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  public void saveToFileOperations(OutputClient client) {
       
    File file = null;
    ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
    filts.add(new FileExtensionFilters.SimpleFilter(ddacx_.getRMan(), client.getExtension(), client.getFilterName()));
    String noDot = client.getExtension().replace('.', ' ').trim();
    ArrayList<String> suffs = new ArrayList<String>();
    suffs.add(noDot);
    file = getFprep(ddacx_).getOrCreateWritableFileWithSuffix(client.getPrefsTag(), filts, suffs, noDot);   
    if (file == null) {
      return;
    }
       
    try {
      PrintWriter out
        = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
      client.writeOutput(out);
      out.close();
      if (client.isBtpOut()) {
        cSrc_.clearUndoTracking();
        setCurrentFile(file);
      } else {
        getFprep(ddacx_).setPreference(client.getPrefsTag(), file.getAbsoluteFile().getParent());
      }
    } catch (IOException ioe) {
      getFprep(ddacx_).displayFileOutputError();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void newModelOperations(DataAccessContext dacx) {
    
 // at org.systemsbiology.biotapestry.app.DynamicDataAccessContext.getDBGenome(DynamicDataAccessContext.java:50)
 // at org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport.newModelOperations(LoadSaveSupport.java:650)
 // at org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps$StepState.doNewModel(LoadSaveOps.java:870)
 // at org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps$StepState.stepToProcessPostWarn(LoadSaveOps.java:427)
    UiUtil.fixMePrintout("FAIL on new model with multiple tabs already up");
    if ((dacx.getGenomeSource() != null) && (dacx.getDBGenome() != null)) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx.getGenomeSource().getID(), dacx.getDBGenomeID(), 
                                                   ModelChangeEvent.MODEL_DROPPED);     
      uics_.getEventMgr().sendModelChangeEvent(mcev);
    }
    dacx.getGSM().drop();    
    dacx.newModel();
    setCurrentFile(null);
    // Gotta reset now so it forgets previous settings before tree installation...
    uics_.getNetOverlayController().resetControllerState(dacx);
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();
    VirtualModelTree vmtree = uics_.getTree();      
    vmtree.setTreeSelectionPath(dstp);
    uics_.getPathController().clearControllerState();     
    uics_.getCommonView().setTabTitleData(tSrc_.getCurrentTabIndex(), dacx.getGenomeSource().getTabNameData());
    uics_.getCommonView().newModelVisibility();
    VirtualPathControls vpc = uics_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);
    uics_.getZoomCommandSupport().setCurrentZoomForNewModel();
    cSrc_.getUndoManager().discardAllEdits();
    cSrc_.clearUndoTracking();  
    return;
  }
  
 /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void newModelTweaks(DataAccessContext dacx) {    
    uics_.getPathController().clearControllerState();
    uics_.getNetOverlayController().resetControllerState(dacx);
    VirtualPathControls vpc = uics_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);
    uics_.getZoomCommandSupport().setCurrentZoomForNewModel();
    return;
  }
  
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  public void postLoadOperations(boolean selectRoot,
                                 int firstIndex, boolean multiTab, boolean forAppend, UndoSupport appendSupport) {    
    if (multiTab) { 
      int numTab = tSrc_.getNumTab();
      //
      // Am seeing that the scroll panes in the hidden tabs are not keeping consistent state.
      // Am needing to track the scrolling for resizes, but this tracking messes stuff up
      // when models are installed. Set the scroll panes to ignore all the resize events
      // happening during construction. They will be cleaned up when the final zoom to
      // model occurs:
      //
      
      for (int i = 0; i < numTab; i++) {  
        uics_.getZoomCommandSupportForTab(i).setIgnoreScroll(true);
      }  
      for (int i = firstIndex; i < numTab; i++) { 
        TabChange tc = tSrc_.setCurrentTabIndex(i);
        // If this is not done after the above statement, the tree and view get all whacked out of sync.
        if ((tc != null) && (appendSupport != null)) {
          appendSupport.addEdit(new TabChangeCmd(ddacx_, tc));
        }
        postLoadOperationsPerTab(selectRoot);
      }   
      for (int i = 0; i < numTab; i++) {  
        uics_.getZoomCommandSupportForTab(i).setIgnoreScroll(false);
      }  
      if (!forAppend) {
        cSrc_.getUndoManager().discardAllEdits();
        cSrc_.clearUndoTracking();
      }
    } else {
      postLoadOperationsPerTab(selectRoot);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  private void postLoadOperationsPerTab(boolean selectRoot) {
    GroupSettingSource gsm = ddacx_.getGSM();
    NavTree navTree = ddacx_.getGenomeSource().getModelHierarchy();
    VirtualModelTree vmtree = uics_.getTree();
    vmtree.setTreeModel(navTree);
    List<TreePath> nonleafPaths = navTree.getAllPathsToNonLeaves();
    Iterator<TreePath> nlpit = nonleafPaths.iterator();
    while (nlpit.hasNext()) {
      TreePath tp = nlpit.next();
      vmtree.expandTreePath(tp);
    }
    
    //
    // Gotta get the overlay stuff cleared out
    // _before_ loading up the new genome!
    //
    uics_.getPathController().clearControllerState();
    uics_.getNetOverlayController().resetControllerState(ddacx_);
    VirtualPathControls vpc = uics_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);    
        
    Iterator<GenomeInstance> giit = ddacx_.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance gi = giit.next();
      gsm.setGroupVisibilities(gi, GroupSettings.Setting.ACTIVE);        
    }
    preloadWithoutRefresh();
    Workspace ws = ddacx_.getWorkspaceSource().getWorkspace();
    if (ws.needsCenter()) {  // Legacy condition...
      ddacx_.getZoomTarget().fixCenterPoint(true, null, false);
    }
   
    if (!uics_.isHeadless()) {
      uics_.getCommonView().postLoadVisibility();
      StartupView sv = ddacx_.getGenomeSource().getStartupView();
      SetCurrentModel.StepState.installStartupView(uics_, ddacx_, selectRoot, sv);
      DisplayOptions dop = ddacx_.getDisplayOptsSource().getDisplayOptions();
      switch (dop.getFirstZoomMode()) {
        case FIRST_ZOOM_TO_ALL_MODELS:
          uics_.getZoomCommandSupport().zoomToAllModels();
          break;
        case FIRST_ZOOM_TO_CURRENT_MODEL:
          uics_.getZoomCommandSupport().zoomToModel();
          break;   
        case FIRST_ZOOM_TO_WORKSPACE:
          uics_.getZoomCommandSupport().zoomToFullWorksheet();
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    return;
  }
                
  /***************************************************************************
  **
  ** Used to load a model into the SUP for centering calcs without 
  ** triggering a refresh
  */
  
  private void preloadWithoutRefresh() {
    NavTree navTree = ddacx_.getGenomeSource().getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();    
    String genomeID = navTree.getGenomeID(dstp);
    String proxyID = navTree.getDynamicProxyID(dstp); 
    Metabase mBase = ddacx_.getMetabase();
    UiUtil.fixMePrintout("HORRIBLE HACK");
    BTState appState = mBase.getAppState();
    if (genomeID != null) {
      String layoutID = ddacx_.getLayoutSource().mapGenomeKeyToLayoutKey(genomeID);
      appState.setGraphLayout(layoutID);
      appState.setGenomeForUndo(genomeID, ddacx_);
    } else if (proxyID != null) {
      DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(proxyID);
      int min = dip.getMinimumTime();
      genomeID = dip.getKeyForTime(min, true);
      String layoutID = ddacx_.getLayoutSource().mapGenomeKeyToLayoutKey(genomeID);
      appState.setGraphLayout(layoutID);
      appState.setGenomeForUndo(genomeID, ddacx_);
    } else {
      appState.setGraphLayout(null);
      appState.setGenomeForUndo(genomeID, ddacx_);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** We need to have the time axis defined before bringing in XML data
  */ 
       
  public boolean prepTimeAxisForDataImport(DataAccessContext dacx) {   
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (tad.isInitialized()) {
      return (true);
    }
    
    TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics_, dacx, dacx.getMetabase(), tSrc_, uFac_, true);
    tasd.setVisible(true);
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = ddacx_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                    rMan.getString("tcsedit.noTimeDefinition"), 
                                    rMan.getString("tcsedit.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    return (true);
  }

  /***************************************************************************
  **
  ** Displays file reading error message for invalid input
  */ 
       
  public SimpleUserFeedback displayInvalidInputError(InvalidInputException iiex) { 
    ResourceManager rMan = ddacx_.getRMan(); 
    String errKey = iiex.getErrorKey();
    boolean haveKey = (errKey != null) && (!errKey.equals(InvalidInputException.UNSPECIFIED_ERROR)); 
    int lineno = iiex.getErrorLineNumber();
    boolean haveLine = (lineno != InvalidInputException.UNSPECIFIED_LINE);
    String outMsg;
    if (haveKey && haveLine) { 
      String format = rMan.getString("fileRead.inputErrorMessageForLineWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {new Integer(lineno + 1), keyedErr});
    } else if (haveKey && !haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {keyedErr});
    } else if (!haveKey && haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageForLine");
      outMsg = MessageFormat.format(format, new Object[] {new Integer(lineno + 1)});      
    } else {
      outMsg = rMan.getString("fileRead.inputErrorMessage");      
    }
    if (uics_.isHeadless()) {
      System.err.println(outMsg);
    }
    return (new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, outMsg, 
                                   rMan.getString("fileRead.errorTitle")));
  }
  
  /***************************************************************************
  **
  ** Interface for output clients
  */
  
  public interface OutputClient {      
    public String getPrefsTag();
    public String getExtension();
    public String getFilterName();
    public void writeOutput(PrintWriter out) throws IOException;
    public boolean isBtpOut();
  }
  
  /***************************************************************************
  **
  ** Class for building networks; used for gaggle
  */ 
    
  public class NetworkBuilder implements BackgroundWorkerOwner {
    
    private SIFImportChoicesDialogFactory.LayoutModes importMode_ = SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT;
    private String species_;
    private StaticDataAccessContext dacx_;
    
    public void doNetworkBuild(String species, List<BuildInstruction> instruct, StaticDataAccessContext dacx) {
      try {
        CommonView cv = uics_.getCommonView();
        species_ = species;
        dacx_ = dacx.getContextForRoot();
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          ResourceManager rMan = dacx.getRMan();
          if (cSrc_.hasAnUndoChange()) {
            int ok = JOptionPane.showConfirmDialog(uics_.getTopFrame(), 
                                    rMan.getString("loadAction.warningMessage"), 
                                    rMan.getString("loadAction.warningMessageTitle"),
                                    JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
              return;
            }
          }
        }
        SpecialtyLayout specLayout = new WorksheetLayout(true, new StaticDataAccessContext(dacx_));
        SpecialtyLayoutEngineParams params = dacx_.getLayoutOptMgr().getDiagLayoutParams();
              
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          setCurrentFile(null);  // since we replace everything
          ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getDBGenomeID(),
                                                       ModelChangeEvent.MODEL_DROPPED);     
          uics_.getEventMgr().sendModelChangeEvent(mcev);
          cv.manageWindowTitle("");
          newModelOperations(dacx_);
        }
        boolean doOpt = false;
        UndoSupport support = uFac_.provideUndoSupport("undo.buildFromGaggleNetwork", dacx_);            
        NewGaggleNetworkRunner runner = new NewGaggleNetworkRunner(uics_, dacx_, tSrc_, uFac_, instruct, importMode_, doOpt, support, specLayout, params);
        BackgroundWorkerClient bwc = 
          new BackgroundWorkerClient(uics_, dacx_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
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
      FilePreparer.FileInputResultClosure firc = finishedImport((LinkRouter.RoutingResult)result, null);
      if (!firc.wasSuccessful()) {
        firc.displayFileInputError();
      }
      return;
    }

    private FilePreparer.FileInputResultClosure finishedImport(LinkRouter.RoutingResult result, IOException ioEx) {   
      if (result != null) {
        (new LayoutStatusReporter(uics_, ddacx_, result)).doStatusAnnouncements();
      }
      CommonView cv = uics_.getCommonView();
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          dacx_.drop();
          cv.manageWindowTitle(null);
          newModelOperations(dacx_);
        }
        FilePreparer.FileInputResultClosure retval = getFprep(ddacx_).getFileInputError(ioEx);
        return (retval);                
      }
      if (dacx_.getGenomeSource().getRootDBGenome() == null) {
        dacx_.drop();
        cv.manageWindowTitle(null);
        newModelOperations(dacx_);                
        FilePreparer.FileInputResultClosure retval = getFprep(ddacx_).getFileInputError(null);
        return (retval);
      }
      GooseAppInterface goose = uics_.getGooseMgr().getGoose();
      if ((goose != null) && goose.isActivated()) {
        SelectionSupport ss = goose.getSelectionSupport();
        ss.setSpecies(species_);
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        ResourceManager rMan = ddacx_.getRMan();
        cv.manageWindowTitle(rMan.getString("gaggleSupport.gaggle") + ": " + species_);
        postLoadOperations(true, 0, false, false, null);
      }
      LayoutLinkSupport.offerColorFixup(uics_, dacx_, result, uFac_);
      return (new FilePreparer.FileInputResultClosure());
    }
  }
  
  /***************************************************************************
  **
  ** Background Gaggle network import (and layout)
  */ 
    
  private static class NewGaggleNetworkRunner extends BackgroundWorker {
 
    private UIComponentSource myUics_;
    private UndoFactory myUFac_;
    private TabSource myTSrc_;
    private DataAccessContext myDacx_;
    private UndoSupport support_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private List<BuildInstruction> commands_;
    private boolean doOpts_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    
    public NewGaggleNetworkRunner(UIComponentSource uics, DataAccessContext dacx, TabSource tSrc, UndoFactory uFac, List<BuildInstruction> commands,
                                  SIFImportChoicesDialogFactory.LayoutModes importMode, boolean doOpts,
                                  UndoSupport support, SpecialtyLayout specLayout, SpecialtyLayoutEngineParams params) {
      super(new LinkRouter.RoutingResult());      
      commands_ = commands;
      myUics_ = uics;
      myUFac_ = uFac;
      myDacx_ = dacx;
      myTSrc_ = tSrc;
      importMode_ = importMode;
      doOpts_ = doOpts;
      support_ = support;
      specLayout_ = specLayout;
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      DBGenomeSIFFormatFactory sff = new DBGenomeSIFFormatFactory();
      try {
        LinkRouter.RoutingResult res = sff.buildForGaggle(myUics_, myDacx_, myTSrc_, myUFac_, commands_,
                                                          (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT), 
                                                          specLayout_, params_,
                                                          support_, doOpts_, this, 0.0, 1.0);
        return (res);
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
