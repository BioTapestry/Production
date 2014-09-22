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
import org.systemsbiology.biotapestry.app.CommonView;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.app.VirtualPathControls;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceSupport;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DatabaseFactory;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.gaggle.SelectionSupport;
import org.systemsbiology.biotapestry.genome.DBGenomeSIFFormatFactory;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
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
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.xml.sax.SAXException;

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

  private BTState appState_;
  private FilePreparer fprep_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LoadSaveSupport(BTState appState, FilePreparer fprep) {
    appState_ = appState;
    fprep_ = fprep;
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
    
  public FilePreparer getFprep() {
    return (fprep_);
  }  
  
  /***************************************************************************
  **
  ** Set the current file; updates the recent file list too
  */ 
    
  public void setCurrentFile(File file) {
    appState_.setCurrentFile(file);
    if (file == null) {
      return;
    }
    if (!appState_.isHeadless()) {
      fprep_.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
      if (appState_.getIsEditor()) {
        if (!appState_.getRecentMenu().haveRecentMenu()) {
          return;
        }
        RecentFilesManager rfm = appState_.getRecentFilesMgr();
        rfm.updateLatestFile(file);
        appState_.getRecentMenu().updateRecentMenu();
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
    CommonView cv = appState_.getCommonView();
    FilePreparer fprep = new FilePreparer(appState_, appState_.isHeadless(), appState_.getTopFrame(), getClass());
    SUPanel sup = appState_.getSUPanel();
    DataAccessContext dacx = new DataAccessContext(appState_, appState_.getGenome());
    
    try {
      if (saltUrl != null) {
        // If user cancels we will not try again
        String args = getPasswordString(saltUrl);
        if (args != null) {
          pEval = processConnectionHeader(url, args);
        }
      }
    } catch (IOException ioe) {
      dacx.drop();
      cv.manageWindowTitle(null);
      newModelOperations(dacx);
      sup.drawModel(true);     
      FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(ioe);
      retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
      return (retval);              
    }

    dacx.drop();
    Database db = appState_.getDB();
    db.setWorkspaceNeedsCenter();
    db.installLegacyTimeAxisDefinition();
    appState_.getGroupMgr().drop();
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(new DatabaseFactory(appState_, isViewer));
    SUParser supr = new SUParser(appState_, alist);

    //
    // This is new code to fix both BT-06-15-05:3 and BT-06-15-05:1
    //
    try {
       if (pEval != null) {
         supr.parse(pEval.stream);
       } else {
         supr.parse(url);
       }
       db.legacyIOFixup(appState_.getTopFrame());
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
       dacx.drop();
       cv.manageWindowTitle(null);
       newModelOperations(dacx);
       sup.drawModel(true);
       FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(ioe);
       retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
       return (retval);  
    }
    if (db.getGenome() == null) {
      dacx.drop();
      cv.manageWindowTitle(null);
      newModelOperations(dacx);
      sup.drawModel(true);
      FilePreparer.FileInputResultClosure retval = fprep.getFileInputError(null);
      retval.setPasswordStatus((pEval != null) ? !pEval.failure : true);
      return (retval);  
    }
    cv.manageWindowTitle(url.toString());
    postLoadOperations(false, dacx);
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
    LoginDialog ld = new LoginDialog(appState_);
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
    
  public FilePreparer.FileInputResultClosure loadFromFile(File file, DataAccessContext dacx) {
    return (loadFromSource(file, null, dacx));
  }  
  
  /***************************************************************************
  **
  ** Common load operations
  */ 
    
  public FilePreparer.FileInputResultClosure loadFromStream(InputStream stream, DataAccessContext dacx) {
    return (loadFromSource(null, stream, dacx));
  }   
  
  /***************************************************************************
  **
  ** Common load operations.  Take your pick of input sources
  */ 
    
  private FilePreparer.FileInputResultClosure loadFromSource(File file, InputStream stream, DataAccessContext dacx) {
    String chosenFileName = (file == null) ? null : file.getName();
    CommonView cv = appState_.getCommonView();
    ModelChangeEvent mcev = new ModelChangeEvent(dacx.getDBGenomeID(),
                                                 ModelChangeEvent.MODEL_DROPPED);     
    appState_.getEventMgr().sendModelChangeEvent(mcev); 
    dacx.drop();
    Database db = appState_.getDB();
    db.setWorkspaceNeedsCenter();
    db.installLegacyTimeAxisDefinition();
    ((GroupSettingManager)dacx.gsm).drop();
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(new DatabaseFactory(appState_, false));
    SUParser sup = new SUParser(appState_, alist);
    setCurrentFile(file);
    try {
      if (file != null) {
        sup.parse(file);
      } else {
        sup.parse(stream);
      }
      db.legacyIOFixup((appState_.isHeadless()) ? null : appState_.getTopFrame());
    } catch (IOException ioe) {
      dacx.drop();
      chosenFileName = null;
      cv.manageWindowTitle(null);
      if (!appState_.isHeadless()) {
        newModelOperations(dacx);
      }
      FilePreparer.FileInputResultClosure retval = fprep_.getFileInputError(ioe);
      return (retval);              
    }
 
    if (dacx.getGenomeSource() == null) {
      dacx.drop();
      chosenFileName = null;
      cv.manageWindowTitle(null);
      if (!appState_.isHeadless()) {
        newModelOperations(dacx);
      }
      FilePreparer.FileInputResultClosure retval = fprep_.getFileInputError(null);
      return (retval);  
    }
    cv.manageWindowTitle(chosenFileName);
    postLoadOperations(false, dacx);
    return (new FilePreparer.FileInputResultClosure());
  }  
   
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  boolean saveToFile(String fileName) {
       
    File file = null;
    if (fileName == null) { 
      ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
      filts.add(new FileExtensionFilters.SimpleFilter(appState_, ".btp", "filterName.btp"));
      ArrayList<String> suffs = new ArrayList<String>();
      suffs.add("btp");
      file = fprep_.getOrCreateWritableFileWithSuffix("LoadDirectory", filts, suffs, "btp");   
      if (file == null) {
        return (true);
      }
    } else {
      // given a name, we do not check overwrite:
      file = new File(fileName);
      if (!fprep_.checkWriteFile(file, false)) {
        return (false);
      }        
    }

    try {
      saveToOutputStream(new FileOutputStream(file));
      setCurrentFile(file);
      appState_.getCommonView().manageWindowTitle(file.getName());
    } catch (IOException ioe) {
      fprep_.displayFileOutputError();
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  void saveToOutputStream(OutputStream stream) throws IOException {
    Database db = appState_.getDB();
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);
    db.writeXML(out, ind);
    out.close();
    appState_.clearUndoTracking();
    return;
  }  
   
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  public void saveToFileOperations(OutputClient client) {
    
    File file = null;
    ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
    filts.add(new FileExtensionFilters.SimpleFilter(appState_, client.getExtension(), client.getFilterName()));
    String noDot = client.getExtension().replace('.', ' ').trim();
    ArrayList<String> suffs = new ArrayList<String>();
    suffs.add(noDot);
    file = fprep_.getOrCreateWritableFileWithSuffix(client.getPrefsTag(), filts, suffs, noDot);   
    if (file == null) {
      return;
    }
       
    try {
      PrintWriter out
        = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
      client.writeOutput(out);
      out.close();
      if (client.isBtpOut()) {
        appState_.clearUndoTracking();
        setCurrentFile(file);
      } else {
        fprep_.setPreference(client.getPrefsTag(), file.getAbsoluteFile().getParent());
      }
    } catch (IOException ioe) {
      fprep_.displayFileOutputError();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void newModelOperations(DataAccessContext dacx) {
    if (dacx.getDBGenome() != null) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx.getDBGenomeID(), 
                                                   ModelChangeEvent.MODEL_DROPPED);     
      appState_.getEventMgr().sendModelChangeEvent(mcev);
    }
    ((GroupSettingManager)dacx.gsm).drop();    
    dacx.newModel();
    setCurrentFile(null);
    // Gotta reset now so it forgets previous settings before tree installation...
    appState_.getNetOverlayController().resetControllerState(dacx);
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();
    VirtualModelTree vmtree = appState_.getTree();      
    vmtree.setTreeSelectionPath(dstp);
    appState_.getPathController().clearControllerState();
    appState_.getCommonView().newModelVisibility();
    VirtualPathControls vpc = appState_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);
    appState_.getZoomCommandSupport().setCurrentZoomForNewModel();
    appState_.getUndoManager().discardAllEdits();
    appState_.clearUndoTracking();  
    return;
  }
  
 /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void newModelTweaks(DataAccessContext dacx) {    
    appState_.getPathController().clearControllerState();
    appState_.getNetOverlayController().resetControllerState(dacx);
    VirtualPathControls vpc = appState_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);
    appState_.getZoomCommandSupport().setCurrentZoomForNewModel();
    return;
  }
  
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  public void postLoadOperations(boolean selectRoot, DataAccessContext dacx) { 
    GroupSettingManager gsm = (GroupSettingManager)dacx.gsm;
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    VirtualModelTree vmtree = appState_.getTree();
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
    appState_.getPathController().clearControllerState();
    appState_.getNetOverlayController().resetControllerState(dacx);
    VirtualPathControls vpc = appState_.getPathControls();
    vpc.updateUserPathActions();
    vpc.setCurrentUserPath(0);    
        
    Iterator<GenomeInstance> giit = dacx.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance gi = giit.next();
      gsm.setGroupVisibilities(gi, GroupSettings.Setting.ACTIVE);        
    }
    preloadWithoutRefresh(dacx);
    Workspace ws = dacx.wSrc.getWorkspace();
    if (ws.needsCenter()) {  // Legacy condition...
      appState_.getZoomTarget().fixCenterPoint(true, null, false);
    }
   
    if (!appState_.isHeadless()) {
      appState_.getCommonView().postLoadVisibility();
      StartupView sv = dacx.getGenomeSource().getStartupView();
      SetCurrentModel.StepState.installStartupView(appState_, dacx, selectRoot, sv);
      DisplayOptions dop = appState_.getDisplayOptMgr().getDisplayOptions();
      switch (dop.getFirstZoomMode()) {
        case FIRST_ZOOM_TO_ALL_MODELS:
          appState_.getZoomCommandSupport().zoomToAllModels();
          break;
        case FIRST_ZOOM_TO_CURRENT_MODEL:
          appState_.getZoomCommandSupport().zoomToModel();
          break;   
        case FIRST_ZOOM_TO_WORKSPACE:
          appState_.getZoomCommandSupport().zoomToFullWorksheet();
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
       
    appState_.getUndoManager().discardAllEdits();
    appState_.clearUndoTracking();
    return;
  }
                    
  /***************************************************************************
  **
  ** Used to load a model into the SUP for centering calcs without 
  ** triggering a refresh
  */
  
  private void preloadWithoutRefresh(DataAccessContext dacx) {
    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();    
    String genomeID = navTree.getGenomeID(dstp);
    String proxyID = navTree.getDynamicProxyID(dstp);    
    if (genomeID != null) {
      String layoutID = dacx.lSrc.mapGenomeKeyToLayoutKey(genomeID);
      appState_.setGraphLayout(layoutID);
      appState_.setGenomeForUndo(genomeID, dacx);
    } else if (proxyID != null) {
      DynamicInstanceProxy dip = dacx.getGenomeSource().getDynamicProxy(proxyID);
      int min = dip.getMinimumTime();
      genomeID = dip.getKeyForTime(min, true);
      String layoutID = dacx.lSrc.mapGenomeKeyToLayoutKey(genomeID);
      appState_.setGraphLayout(layoutID);
      appState_.setGenomeForUndo(genomeID, dacx);
    } else {
      appState_.setGraphLayout(null);
      appState_.setGenomeForUndo(genomeID, dacx);
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
    
    TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(appState_, dacx);
    tasd.setVisible(true);
    
    tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
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
    ResourceManager rMan = appState_.getRMan(); 
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
    if (appState_.isHeadless()) {
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
    private DataAccessContext dacx_;
    
    public void doNetworkBuild(String species, List<BuildInstruction> instruct, DataAccessContext dacx) {
      try {
        CommonView cv = appState_.getCommonView();
        species_ = species;
        dacx_ = dacx.getContextForRoot();
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          ResourceManager rMan = appState_.getRMan();
          if (appState_.hasAnUndoChange()) {
            int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                    rMan.getString("loadAction.warningMessage"), 
                                    rMan.getString("loadAction.warningMessageTitle"),
                                    JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
              return;
            }
          }
        }
        SpecialtyLayout specLayout = new WorksheetLayout(appState_, true);
        SpecialtyLayoutEngineParams params = appState_.getLayoutOptMgr().getDiagLayoutParams();
              
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          setCurrentFile(null);  // since we replace everything
          ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getDBGenomeID(),
                                                       ModelChangeEvent.MODEL_DROPPED);     
          appState_.getEventMgr().sendModelChangeEvent(mcev);
          cv.manageWindowTitle("");
          newModelOperations(dacx_);
        }
        boolean doOpt = false;
        UndoSupport support = new UndoSupport(appState_, "undo.buildFromGaggleNetwork");            
        NewGaggleNetworkRunner runner = new NewGaggleNetworkRunner(appState_, dacx_, instruct, importMode_, doOpt, support, specLayout, params);
        BackgroundWorkerClient bwc = 
          new BackgroundWorkerClient(appState_, this, runner, "linkLayout.waitTitle", "linkLayout.wait", support, true);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
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
        (new LayoutStatusReporter(appState_, result)).doStatusAnnouncements();
      }
      CommonView cv = appState_.getCommonView();
      if (ioEx != null) {
        if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
          dacx_.drop();
          cv.manageWindowTitle(null);
          newModelOperations(dacx_);
        }
        FilePreparer.FileInputResultClosure retval = fprep_.getFileInputError(ioEx);
        return (retval);                
      }
      if (dacx_.getGenomeSource().getGenome() == null) {
        dacx_.drop();
        cv.manageWindowTitle(null);
        newModelOperations(dacx_);                
        FilePreparer.FileInputResultClosure retval = fprep_.getFileInputError(null);
        return (retval);
      }
      GooseAppInterface goose = appState_.getGooseMgr().getGoose();
      if ((goose != null) && goose.isActivated()) {
        SelectionSupport ss = goose.getSelectionSupport();
        ss.setSpecies(species_);
      }
      if (importMode_ == SIFImportChoicesDialogFactory.LayoutModes.REPLACEMENT) {
        ResourceManager rMan = appState_.getRMan();
        cv.manageWindowTitle(rMan.getString("gaggleSupport.gaggle") + ": " + species_);
        postLoadOperations(true, dacx_);
      }
      LayoutLinkSupport.offerColorFixup(appState_, dacx_, result);
      return (new FilePreparer.FileInputResultClosure());
    }
  }
  
  /***************************************************************************
  **
  ** Background Gaggle network import (and layout)
  */ 
    
  private static class NewGaggleNetworkRunner extends BackgroundWorker {
 
    private BTState myAppState_;
    private DataAccessContext myDacx_;
    private UndoSupport support_;
    private SIFImportChoicesDialogFactory.LayoutModes importMode_;
    private List<BuildInstruction> commands_;
    private boolean doOpts_;
    private SpecialtyLayout specLayout_;
    private SpecialtyLayoutEngineParams params_;
    
    public NewGaggleNetworkRunner(BTState appState, DataAccessContext dacx, List<BuildInstruction> commands,
                                  SIFImportChoicesDialogFactory.LayoutModes importMode, boolean doOpts,
                                  UndoSupport support, SpecialtyLayout specLayout, SpecialtyLayoutEngineParams params) {
      super(new LinkRouter.RoutingResult());      
      commands_ = commands;
      myAppState_ = appState;
      myDacx_ = dacx;
      importMode_ = importMode;
      doOpts_ = doOpts;
      support_ = support;
      specLayout_ = specLayout;
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      DBGenomeSIFFormatFactory sff = new DBGenomeSIFFormatFactory();
      try {
        LinkRouter.RoutingResult res = sff.buildForGaggle(myAppState_, myDacx_, commands_,
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
      if (myAppState_.modelIsOutsideWorkspaceBounds()) {
        Rectangle allBounds = myAppState_.getZoomTarget().getAllModelBounds();
        (new WorkspaceSupport(myAppState_, myDacx_)).setWorkspaceToModelBounds(support_, allBounds);
      }
      return (null);
    } 
  }  
}
