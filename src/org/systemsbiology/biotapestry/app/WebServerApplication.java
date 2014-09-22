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


package org.systemsbiology.biotapestry.app;

import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.MenuSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.BatchJobControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd.Progress;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.cmd.flow.WebServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.display.DisplayData;
import org.systemsbiology.biotapestry.cmd.flow.display.PathGenerator;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportModel;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportWeb;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.select.Selection;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.XPlatModelNode;
import org.systemsbiology.biotapestry.nav.XPlatModelTree;
import org.systemsbiology.biotapestry.ui.ImageExporter;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector;
import org.systemsbiology.biotapestry.ui.ViewExporter;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.MultiSubID;
import org.systemsbiology.biotapestry.ui.menu.XPlatCurrentState;
import org.systemsbiology.biotapestry.ui.menu.XPlatGenericMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatKeyBindings;
import org.systemsbiology.biotapestry.ui.menu.XPlatMaskingStatus;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenuBar;
import org.systemsbiology.biotapestry.ui.menu.XPlatToolBar;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.NamedOutputStreamSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback.JOP;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.WebPublisher;

import flexjson.JSONDeserializer;


/****************************************************************************
**
** The top-level application for use in web server:
*/

public class WebServerApplication {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  // SET TO FALSE TO TEST VIEWER MODE; TRUE TO TEST EDITOR MODE
  
  private final boolean compiledAsEditor_ = false; // WEB EDITOR COMPILER FLAG DO NOT REMOVE OR CHANGE THIS COMMENT OR VARIABLE NAME!

  private final String servletPath_;
  private final String fileDir_;
  private final String configDir_ = "/WEB-INF/";
  private final String fullServletContextPath_;
  private final String pluginsDir_;
  
  private static final String DEFAULT_MODEL_FILE_DIR_ = "/WEB-INF/data/";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  ** 
  * Constructor that will take an InputStream (instead of a file path)
  *
  *
  */ 
  
  public WebServerApplication(String servPath) throws GeneratorException { 
	  this(servPath, null);
  } 
  
  public WebServerApplication(String servPath, String fullServletContextPath) throws GeneratorException { 
	  this(servPath,DEFAULT_MODEL_FILE_DIR_,fullServletContextPath);	  
  }

  public WebServerApplication(String servPath, String fileDir, String fullServletContextPath) throws GeneratorException { 
	  this(servPath,fileDir,fullServletContextPath,null);		  
  }
  
  public WebServerApplication(String servPath, String fileDir, String fullServletContextPath,String pluginsDir) throws GeneratorException { 
	  synchronized (MainCommands.class) {
	    System.setProperty("java.awt.headless", "true");     	
	    servletPath_ = servPath;
	    fileDir_ = (fileDir == null ? DEFAULT_MODEL_FILE_DIR_ : fileDir);
	    fullServletContextPath_ = fullServletContextPath;
	    pluginsDir_ = pluginsDir;
	  }			  
  }
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Track down timer-0 leak

  
  private void dumpThreadNames(int num) {  
    int tc = Thread.activeCount();
    Thread[] ta = new Thread[tc];
    int tcc = Thread.enumerate(ta);
    System.out.println("Num: " + num + " " + tc + " tc/tcc  " + tcc);
    for (int i = 0; i < tc; i++) {
      System.out.println(ta[i].getName());  
    }
    return;
  }

  /***************************************************************************
  ** 
  */ 
  
  public void initNewState(BTState appState, String loadFile) throws GeneratorException, FileNotFoundException { 
    initNewState(appState, new FileInputStream(loadFile));
    return;
  }

  /***************************************************************************
  ** 
  ** Initialize a brand new app State
  */

  public void initNewState(BTState appState, InputStream is) throws GeneratorException {  
    synchronized (MainCommands.class) {     
      try {
    	  Map<String,Object> plugins = new HashMap<String,Object>();
          if(pluginsDir_ != null) {
          	plugins.put(ArgParser.PLUG_IN_DIR,
      			this.fullServletContextPath_+((this.pluginsDir_.startsWith("/") || this.pluginsDir_.startsWith("\\") ? this.pluginsDir_.substring(1) : this.pluginsDir_))
  			);
          }
    	  boolean ok = appState.getPlugInMgr().loadDataDisplayPlugIns(plugins);
    	  
        if (!ok) {
          System.err.println("Problems loading plugins");
        }
        DialogAndInProcessCmd daipc0 = null;
        appState.getDB().newModelViaDACX(); // Bogus, but no DACX yet
        appState.setIsEditor(compiledAsEditor_);
        appState.setExceptionHandler(new ExceptionHandler(appState, appState.getRMan(), true));
        appState.setServerBtpDirectory(fileDir_);
        appState.setServerConfigDirectory(configDir_);
        appState.setFullServletContextPath(this.fullServletContextPath_);
        CommonView cview = new CommonView(appState);
        cview.buildTheView();               
        FlowMeister flom = appState.getFloM();
        Object[] osArgs = new Object[2];
        osArgs[0] = Boolean.valueOf(false);
        osArgs[1] = is;
        BatchJobControlFlowHarness dcf0 = new BatchJobControlFlowHarness(appState, null); 
        ControlFlow myFlow0 = flom.getControlFlow(FlowMeister.MainFlow.LOAD, null);
        DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
        LoadSaveOps.StepState pre0 = (LoadSaveOps.StepState)myFlow0.getEmptyStateForPreload(dacx);
        pre0.setParams(osArgs);     
        dcf0.initFlow(myFlow0, dacx);
        daipc0 = dcf0.stepTheFlow(pre0);
        if (daipc0.state != DialogAndInProcessCmd.Progress.DONE) {
          throw new GeneratorException("State is not DONE, it is " + daipc0.state);
        }      
      } catch (Exception ex) {
    	  ex.printStackTrace();
    	  String message = ex.getMessage();
    	  if(ex.getMessage() == null) {
    		  message = ex.toString();
    	  }
    	  throw new GeneratorException("Error on BTState init: " + message,ex);
      }
    }       
  }  

  /***************************************************************************
  ** 
  ** Get image that goes with the particular model
  */

  public String getAnnotationImageType(BTState appState, String modelID) throws GeneratorException {
    synchronized (MainCommands.class) {
      try {
        Genome currGenome = appState.getDB().getGenome(modelID);
        String mi = currGenome.getGenomeImage();
        if (mi == null) {
          return (null);
        }
        String type = appState.getImageMgr().getImageType(mi);
        if (type == null) {
          return (null);
        }
        return("image/" + type.toLowerCase());
      } catch (Exception ex) {
        throw new GeneratorException("imageTypeFailure: " + ex.getMessage(),ex);
      }
    }
  }
  
  /***************************************************************************
  ** 
  ** Get image that goes with the particular model
  */

  public void getAnnotationImage(BTState appState, String modelID, OutputStream output) throws GeneratorException {  
    synchronized (MainCommands.class) {    
      try {
        Genome currGenome = appState.getDB().getGenome(modelID);
        String mi = currGenome.getGenomeImage();
        BufferedImage bi = appState.getImageMgr().getImage(mi);
        String type = appState.getImageMgr().getImageType(mi);
        List<Object[]> resList = ImageExporter.getSupportedResolutions(false);       
        ImageExporter.ResolutionSettings res = new ImageExporter.ResolutionSettings();
        res.dotsPerUnit = (ImageExporter.RationalNumber)resList.get(0)[ImageExporter.INCHES];       
        ImageExporter iex = new ImageExporter();    
        iex.export(output, bi, type, res);
      } catch (Exception ex) {
        throw new GeneratorException("getAnnotationImageFailure: " + ex.getMessage(),ex);
      }
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get image of specified network
  */

  public void getImage(BTState appState, String modelID, OutputStream output) throws GeneratorException {  
    synchronized (MainCommands.class) {    
      try {
        
        if (modelID == null) {
          String genomeID = appState.getGenome();          
          modelID = (genomeID == null) ? appState.getDB().getStartupView().getModel() : genomeID;
        }       
        BogoNamedOutputStreamSource boss = new BogoNamedOutputStreamSource(output);
             
        HashSet<WebPublisher.ModelScale> keysToPublish = new HashSet<WebPublisher.ModelScale>();
        keysToPublish.add(new WebPublisher.ModelScale(modelID, WebPublisher.ModelScale.SMALL));
        
        HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps> intersectionMap = 
          new HashMap<WebPublisher.ModelScale, ViewExporter.BoundsMaps>();

        FlowMeister flom = appState.getFloM();
        Object[] osArgs = new Object[4];
        osArgs[0] = Boolean.valueOf(false);
        osArgs[1] = boss;
        osArgs[2] = intersectionMap;
        osArgs[3] = keysToPublish;
        BatchJobControlFlowHarness dcf2 = new BatchJobControlFlowHarness(appState, null); 
        ControlFlow myFlow2 = flom.getControlFlow(FlowMeister.MainFlow.WEB, null);
        DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
        ExportWeb.StepState pre2 = (ExportWeb.StepState)myFlow2.getEmptyStateForPreload(dacx);
        pre2.setParams(osArgs);     
        dcf2.initFlow(myFlow2, dacx);
        DialogAndInProcessCmd daipc2 = dcf2.stepTheFlow(pre2);          
        if (daipc2.state != DialogAndInProcessCmd.Progress.DONE) {
          throw new GeneratorException("imageExportFailure");
        }
      } catch (GeneratorException gex) {
    	  throw gex;
      } catch (Exception ex) {
    	  	throw new GeneratorException("imageExportFailure: " + ex.getMessage(),ex);
      }
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Get JSON representation of specified network
  */

  public Map<String, Object> getModelMap(BTState appState, String modelID) throws GeneratorException {  
    synchronized (MainCommands.class) {
      try {
        if (modelID == null) {
          String genomeID = appState.getGenome();          
          modelID = (genomeID == null) ? appState.getDB().getStartupView().getModel() : genomeID;
        }

        FlowMeister flom = appState.getFloM();
        BatchJobControlFlowHarness dcf2 = new BatchJobControlFlowHarness(appState, null); 
        ControlFlow myFlow2 = flom.getControlFlow(FlowMeister.MainFlow.MODEL_EXPORT, null);
        DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
        ExportModel.StepState pre2 = (ExportModel.StepState)myFlow2.getEmptyStateForPreload(dacx);
        pre2.setParams(modelID);
        dcf2.initFlow(myFlow2, dacx);
        DialogAndInProcessCmd daipc2 = dcf2.stepTheFlow(pre2);          
        if (daipc2.state != DialogAndInProcessCmd.Progress.DONE) {
          throw new GeneratorException("JSONExportFailure");
        }
                
        return daipc2.commandResults;

      } catch (Exception ex) {
    	  ex.printStackTrace();
        throw new GeneratorException("JSONExportFailure: " + ex.getMessage(),ex);
      }
    }
  } 
  
  /***************************************************************************
  ** 
  ** Get the model tree
  */

  public XPlatModelTree getModelTree(BTState appState) throws GeneratorException { 
    synchronized (MainCommands.class) { 
      try {
        DataAccessContext dacx = new DataAccessContext(appState);
        NavTree navTree = dacx.getGenomeSource().getModelHierarchy();  
        XPlatModelTree xpm = navTree.getXPlatTree(dacx);
        return (xpm);
      } catch (Exception hex) {
    	  hex.printStackTrace();
        throw new GeneratorException("ExceptionFailure: " + hex.getMessage(), hex);
      }
    }   
  }
  
  /***************************************************************************
  ** 
  ** Get the specified icon image
  */

  public void getIcon(BTState appState, String iconName, OutputStream output) throws GeneratorException {    
  
    synchronized (MainCommands.class) {   
      try {
        if (iconName == null) {
          throw new GeneratorException("no iconFile");
        }
        MainCommands mcmd = appState.getMainCmds();
        URL ugif = mcmd.getClass().getResource("/org/systemsbiology/biotapestry/images/" + iconName); 
        Image image = ImageIO.read(ugif);
        ImageIO.write((BufferedImage)image, "png", output);
      } catch (Exception hex) {
        throw new GeneratorException("ExceptionFailure: " + hex.getMessage(),hex);
      }    
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get allowed cross-platform menu requests
  */

  public MenuSource.SupportedMenus getSupportedMenuRequests(BTState appState) throws GeneratorException {     
    synchronized (MainCommands.class) {   
      try { 
        MenuSource mSrc = new MenuSource(appState.getFloM(), !appState.getIsEditor(), appState.getDoGaggle());
        return (mSrc.getSupportedMenuClasses());
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new GeneratorException("ExceptionFailure: " + ex.getMessage(),ex);
      }    
    }
  }
   
  /***************************************************************************
  ** 
  ** Get cross-platform menu definitions
  */

  public XPlatGenericMenu getMenuDefinition(BTState appState, HSRWrapper hsrWrapper) throws GeneratorException {    
  
    synchronized (MainCommands.class) {   
      try {
    	  
    	  // Menus require current client state information
    	  // TODO: move this into POST
    	  
          WebClientState thisState = null;
          if(hsrWrapper.getParameter("clientstate") != null) {
          	thisState = parseClientState(hsrWrapper);
          }
          DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
          UndoSupport undo = new UndoSupport(appState,"command.chooseOverlay",dacx);
          if(thisState != null) {
        	  appState.getNetOverlayController().setFullOverlayState(thisState.currOverlay_, thisState.modsAsTaggedSet(), thisState.shownModsAsTaggedSet(),undo, dacx);
          } else {
        	  appState.getNetOverlayController().clearCurrentOverlay(undo, dacx);
          }
          dacx = new DataAccessContext(appState, appState.getGenome());
    	  
			String menuClass = hsrWrapper.getParameter("menuClass");
			String popClass = hsrWrapper.getParameter("popClass");
			String popObjID = hsrWrapper.getParameter("popObjID");
			String popObjSubIDString = hsrWrapper.getParameter("objSubID");
			Object popObjSubID = null;
			if (popObjSubIDString != null) {
	    	  JSONDeserializer<LinkSegmentID> rezzer = new JSONDeserializer<LinkSegmentID>();
	          LinkSegmentID lsid = (LinkSegmentID)rezzer.deserialize(popObjSubIDString);
			  if (lsid != null) {
			    popObjSubID = new MultiSubID(lsid);
			  }
			}
        if (menuClass == null) {
          throw new GeneratorException("no menuClass");
        }
        MenuSource mSrc = new MenuSource(appState.getFloM(), !appState.getIsEditor(), appState.getDoGaggle());

        if (menuClass.equals(mSrc.getXPlatMenuBarKey())) {         
          XPlatMenuBar xpmb = mSrc.getXPlatMenuBar(dacx);
          MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
          boolean readOnly = !appState.getIsEditor();
          bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
          bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));    
          mSrc.activateXPlatMenuBar(xpmb, bifo, appState.getMainCmds(), appState);
          return (xpmb);
        } else if (menuClass.equals(mSrc.getXPlatToolBarKey())) { 
          XPlatToolBar xptb = mSrc.getXPlatToolBar(dacx); 
          return (xptb);
        } else if (menuClass.equals(mSrc.getXPlatTreeKey())) {
          XPlatMenu xpm = mSrc.getXPlatTreePopup(dacx);
          return (xpm);
        } else if (menuClass.equals(mSrc.getXPlatPopupKey())) {
          if ((popClass == null) || (popObjID == null)) {
            throw new GeneratorException("Missing pop args: " + popClass + " " + popObjID);
          }
          MenuSource.PopType pType = MenuSource.PopType.valueOf(popClass);
          XPlatMenu xpm = mSrc.getXPlatPopup(pType, popObjID, true, dacx);
          MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
          boolean readOnly = !appState.getIsEditor();
          bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
          bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
          mSrc.activateXPlatPopup(xpm, bifo, popObjID, popObjSubID, dacx);
          return (xpm);
        } else {
          throw new GeneratorException("Unexpected menu class: " + menuClass);
        }
      } catch (MenuSource.InvalidMenuRequestException imrex) {
        imrex.printStackTrace();
        throw new GeneratorException("Unsupported menu class result " + imrex.getMessage(), imrex);          
      } catch (Exception ex) {
    	  ex.printStackTrace();
        throw new GeneratorException("ExceptionFailure: " + ex.getMessage(),ex);
      }    
    }   
  }
  
  /***************************************************************************
  ** 
  ** Get cross-platform menu status
  */

  public XPlatCurrentState getMenuStatus(BTState appState) throws GeneratorException {    
  
    synchronized (MainCommands.class) {
      try {
        XPlatCurrentState xpcs = new XPlatCurrentState();
        DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
        
        xpcs.fillInMenu("SELECTED", appState.getSUPanel().getSelectedXPlatMenu(dacx));
        if (appState.getIsEditor()) {
          xpcs.fillInMenu("CURRENT_MODEL", appState.getTree().getCurrentModelXPlatMenu());
        }
        
        if (appState.getRecentMenu().haveRecentMenu()) {
          boolean readOnly = !appState.getIsEditor();
          XPlatMenu xpm = appState.getRecentMenu().getXPlatMenu();
          MenuSource mSrc = new MenuSource(appState.getFloM(), !appState.getIsEditor(), appState.getDoGaggle());
          MenuSource.BuildInfo bifo = new MenuSource.BuildInfo();
          bifo.conditions.put("VIEWER", Boolean.valueOf(readOnly));
          bifo.conditions.put("EDITOR", Boolean.valueOf(!readOnly));
          Map<FlowMeister.FlowKey, Boolean> fes = appState.getMainCmds().getFlowEnabledState();
          mSrc.activateXPlatMenu(xpm, bifo, fes, appState);
          xpcs.fillInMenu("RECENT", xpm);
        }        
        xpcs.fillInMenu("USER_PATH_CHOOSE", appState.getPathControls().getXPlatMenu());
        xpcs.fillInCombo("PATH_COMBO", appState.getPathControls().getXPlatComboBox());
        if (appState.getDoGaggle()) {
          xpcs.fillInMenu("GOOSE_CHOOSE", appState.getGaggleControls().getXPlatMenu());
          xpcs.fillInCombo("GOOSE_COMBO", appState.getGaggleControls().getXPlatComboBox());
        }
        
        xpcs.setConditionalState("SHOW_PATH", (appState.getPathMgr().getPathCount() > 0));
        xpcs.setConditionalState("SHOW_OVERLAY", dacx.fgho.overlayExists());
    
        xpcs.mainCommandEnables(appState.getMainCmds().getFlowEnabledState());
         
        xpcs.setModelTreeState(appState.getTree().getTreeEnables(dacx));
        
        UndoManager undom = appState.getUndoManager();
        boolean canUndo = undom.canUndo();
        boolean canRedo = undom.canRedo();
        ResourceManager rMan = appState.getRMan();
        String desc;
        if (canUndo) {
          desc = undom.getUndoPresentationName();
        } else {
          desc = rMan.getString("command.Undo");
        }
        xpcs.setMutableActionName("UNDO", desc);
        if (canRedo) {
          desc = undom.getRedoPresentationName();
        } else {
          desc = rMan.getString("command.Redo");
        }
        xpcs.setMutableActionName("REDO", desc);
        
        return (xpcs);
      } catch (Exception ex) {
        throw new GeneratorException("ExceptionFailure: " + ex.getMessage(),ex);
      }    
    }   
  }
  
  /***************************************************************************
  ** 
  ** Get cross-platform keyBinding definitions
  */

  public XPlatKeyBindings getKeyBindings(BTState appState) throws GeneratorException {    
  
    synchronized (MainCommands.class) {   
      try { 
        MenuSource mSrc = new MenuSource(appState.getFloM(), !appState.getIsEditor(), appState.getDoGaggle());
        XPlatKeyBindings xpmb = mSrc.getXPlatKeyBindings();
        return (xpmb);
      } catch (Exception ex) {
        throw new GeneratorException("ExceptionFailure: " + ex.getMessage(),ex);
      }    
    }   
  }
  
  /*****************************************
   * Utility to map link IDs to LSIDS
   *****************************************
   * 
   * 
   * @param appState
   * @param req
   * @return
   * @throws GeneratorException
   * 
   */
  public Map<String,Object> mapLinksToIntersections(BTState appState, ParamSource req) throws GeneratorException {
  
    synchronized (MainCommands.class) {   
      try { 
        String modelID = req.getParameter("model");
        if (modelID == null) {
          throw new GeneratorException("A model ID was not provided!");
        }
  
        Genome genome = appState.getDB().getGenome(modelID);
        Layout lo = appState.getLayoutForGenomeKey(modelID);      
        DataAccessContext rcx = new DataAccessContext(appState, genome, lo);
              
        String linkIDParam = req.getParameter("linkID");
        if (linkIDParam == null) {
          throw new GeneratorException("A link ID was not provided!");
        }
        HashSet<String> links = new HashSet<String>();
        links.add(linkIDParam);
   
        Map<String,Object> results = new HashMap<String,Object>();
        SourceAndTargetSelector.SearchResult retval = new SourceAndTargetSelector.SearchResult(true);
        if (!links.isEmpty()) {
          retval.linkIntersections.addAll(lo.getIntersectionsForLinks(rcx, links, true));
          retval.mapToResults(results);
        }
        return (results);
      } catch (Exception ex) {
        throw new GeneratorException("ExceptionFailure: " + ex.getMessage(),ex);
      }
    }
  }
    
  /***************************************************************************
  ** 
  ** In-process processing entry point.  This method is synchronized internally
  ** on a global basis to insure one process call at a time.
  */

  public CommandResult processCommand(BTState appState, ParamSource req) throws GeneratorException {    
  
    //
    // Processing of all requests MUST be globally 
    // serialized in a multi-threaded environment.  At this time, 
    // BioTapestry core code must run on one thread when in batch mode.
    //
    
    synchronized (MainCommands.class) {   
      try {
        
        appState.setCurrentPrintWriter(new PrintWriter(System.out));
        
        //
        // Processing a command means that we are given the command class, the flow key, and optional args. Use this info to
        // go and get the appropriate control flow called for by the request:
        //
        String cmdClass = req.getParameter("cmdClass");
        if (cmdClass == null) {
          throw new GeneratorException("no cmdClass");
        }
        String cmdKey = req.getParameter("cmdKey");
        if (cmdKey == null) {
          throw new GeneratorException("no cmdKey");
        }
        
        WebClientState thisState = null;
        if(req.getParameter("clientstate") != null) {
        	thisState = parseClientState(req);
        }
        
        FlowMeister flom = appState.getFloM();
        FlowMeister.FlowKey keyVal = flom.mapToFlowKey(cmdClass, cmdKey);
        if (keyVal == null) {
          throw new GeneratorException("no keyVal");
        }
        Map<String, String> coa = collectOptArgs(req);
        FlowMeister.OptArgWithStatus oaws = flom.buildCFArgs(keyVal, coa);
        if (!oaws.ok) {
          return (null);
        }
        
        //
        // OK, we have the flow we want to execute:
        //
        
        ControlFlow theFlow = flom.getControlFlow(keyVal, oaws.arg);
        if (theFlow == null) {
          return (null);
        }
     
        //
        // If we have a currently existing harness containing a flow that does not match the incoming flow, clear the harness
        // and toss it out:
        //
        
        if (appState.getHarness() != null) {
          ControlFlow cf = appState.getHarness().getCurrFlow();
          if ((cf == null) || !cf.getName().equals(theFlow.getName())) {
            appState.getHarness().clearFlow();
            appState.setHarness(null);
          }
        }
        
        //
        // If the harness is not in existence, create it.  Then prime the flows with the initialization that is
        // required by the flow type, i.e. popup flows need object ID and click location info:
        //
         
        DialogAndInProcessCmd.CmdState cms = null;
        if (appState.getHarness() == null) {
          appState.setHarness(new WebServerControlFlowHarness(appState, new SerializableDialogPlatform(servletPath_)));
          appState.getHarness().clearFlow();
          
          DataAccessContext dacx = new DataAccessContext(appState, appState.getGenome());
          UndoSupport undo = new UndoSupport(appState,"command.chooseOverlay",dacx);
          
          if(thisState != null) {
        	  // Don't set overlay information if this is a model selection
        	  // because model selection and overlay setting are done in a 
        	  // combined fashion. For everything else, set it first
        	  if(!cmdKey.contains("MODEL_SELECTION")) {
        		  appState.getNetOverlayController().setFullOverlayState(
    				  thisState.currOverlay_, thisState.modsAsTaggedSet(), thisState.shownModsAsTaggedSet(),undo, dacx
				  );
        	  }
          }
          
          dacx = new DataAccessContext(appState, appState.getGenome());
          
          //
          // Prime control flows 
          //
          FlowKey.FlowType ft = keyVal.getFlowType();
          boolean okLoad = false;       
          switch (ft) {
            case MAIN:
              okLoad = true;
              break;
            case MODEL_TREE:
              cms = loadModelTreeFlow(appState, theFlow, req, dacx);
              okLoad = (cms != null);
              break;
            case POP:
              cms = loadPopupFlow(appState, theFlow, req, dacx);
              okLoad = (cms != null);
              break;
            case KEY_BOUND:
              okLoad = true;
              break;
            case OTHER:
              cms = loadOtherFlow(appState, theFlow, req, dacx, thisState);
              okLoad = (cms != null);
              break;              
            default:
              return (null);
          }
          if (!okLoad) {
            throw new GeneratorException("bad Control Flow init");
          }        
          appState.getHarness().initFlow(theFlow, dacx);
          appState.setSUF(null);
          appState.setDialog(null);
          appState.setPendingClick(null);          
        }
        
        //
        // OK, note that if we initialized a flow above, we do NOT have any pending input (e.g. lastWebDialog_ was
        // set to null). If we have previous dialog/pending click stuff, we are waiting for input and need to extract the appropriate
        // parameters and feed them to the flow.  No previous dialog/click stuff means we just step the flow.
        //
       
        CommandResult cmdResult = null;

        // Last thing out the door was a simple user feedback:
        if (appState.getSUF() != null) {
          appState.setSUF(null);
          // The SUF might have been in response to a dialog error request. Send the dialog back out the door:
          if (appState.getDialog() != null) {
            cmdResult = new CommandResult(CommandResult.ResultType.XPLAT_DIALOG);
            cmdResult.addResult("dialog", ((SerializableDialogPlatform.Dialog)appState.getDialog()).getDialog(keyVal));
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
            cmdResult.setNeedsClassAttr(true);
            return cmdResult;
          }
          // SUF is part of normal flow.  Extract and step;
          DialogAndInProcessCmd daipc = appState.getHarness().getCurrDAIPC();
          recordSimpleUserFeedback(daipc.suf, req);
          appState.getHarness().stepTheFlow(daipc.currStateX);
        } else if (appState.getDialog() != null) {
          if (userCancel(req)) {
            appState.getHarness().userCancel();
            appState.setDialog(null);
          } else {
            UserInputs ui = collectInputs(req);
            SimpleUserFeedback suf = appState.getDialog().checkForErrors(ui);
            if (suf != null) {
              appState.setSUF(null);
              cmdResult = new CommandResult(CommandResult.ResultType.XPLAT_DIALOG);
              cmdResult.addResult("dialog", suf.getXPlatDialog());
              cmdResult.addResult("currModel", appState.getGenome());
              cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
              cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
              return cmdResult;
            }
            DialogAndInProcessCmd postReceive = appState.getHarness().receiveUserInputs(ui);
            if(postReceive.state != Progress.SIMPLE_USER_FEEDBACK || postReceive.suf.optionPane != JOP.ERROR) {
          	  appState.setDialog(null);
            }
          }
        } else if (appState.getPendingClick() != null) {
          Map<String, String> pVals = collectRequirements(appState.getPendingClick().getRequiredParameters(), req);
          Point click = appState.getPendingClick().getClickOutput(pVals);
          if (click == null) {
            appState.setPendingClick(null);
            cmdResult = new CommandResult(CommandResult.ResultType.PARAMETER_ERROR);
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
            return (cmdResult);
          }            
          appState.getHarness().handleClick(click, appState.getPendingClick().getShiftOutput(pVals), 10.0);
          appState.setPendingClick(null);
        } else {
          if (cms == null) {
            appState.getHarness().stepTheFlow();
          } else {
            appState.getHarness().stepTheFlow(cms);
          }
        }

        DialogAndInProcessCmd daipc = appState.getHarness().getCurrDAIPC();
        
        switch (daipc.state) {
          // Mouse mode has been installed; return it.        
          case MOUSE_MODE_RESULT:
            switch (daipc.pccr) {
              case CANCELLED:
                appState.getHarness().clearFlow();
                cmdResult = new CommandResult(CommandResult.ResultType.CANCEL);
                cmdResult.addResult("currModel", appState.getGenome());
                cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
                cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));                
                return (cmdResult);
              case ACCEPT:  
                appState.setPendingClick(new WebServerControlFlowHarness.PendingMouseClick()); // More clicks needed for link drawing.... YES it falls through
              case PRESENT: // Never seen here?
              case ACCEPT_DELAYED:            
              case SELECTED:
              case PROCESSED:
                appState.getHarness().clearFlow();
                cmdResult = new CommandResult(CommandResult.ResultType.SUCCESS);
                cmdResult.addResult("currModel", appState.getGenome());
                cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
                cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
                return (cmdResult);
              case UNSELECTED:  // Bogus usage in Pulldown? Not Illegal then, but it never gets here (swallowed first in flow?)
              case ERROR:
              case REJECT:
                appState.setPendingClick(new WebServerControlFlowHarness.PendingMouseClick());
                cmdResult = new CommandResult(CommandResult.ResultType.ILLEGAL_CLICK_PROCESSED);
                cmdResult.addResult("currModel", appState.getGenome());
                cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
                cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
                return (cmdResult);                
              default:
                throw new GeneratorException("Unexpected Click Result: " + daipc.pccr);
            }
            
          case SIMPLE_USER_FEEDBACK:
            appState.setSUF(daipc.suf);
            cmdResult = new CommandResult(CommandResult.ResultType.XPLAT_DIALOG);
            cmdResult.addResult("dialog", appState.getSUF().getXPlatDialog());
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
            return cmdResult;
            
          case USER_CANCEL:
            appState.getHarness().clearFlow();
            cmdResult = new CommandResult(CommandResult.ResultType.CANCEL);
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
            return (cmdResult);
            
          case HAVE_ERROR:
        	  appState.getHarness().clearFlow();
        	  cmdResult = new CommandResult(CommandResult.ResultType.PROCESSING_ERROR);
        	  cmdResult.addResult("currModel", appState.getGenome());
        	  cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
        	  cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
        	  return cmdResult;           
           
        	case HAVE_FRAME_TO_LAUNCH_AND_MOUSE_RESULT:
        	  UiUtil.fixMePrintout("Anything I need to do with the mouse result for web app?");
          case HAVE_FRAME_TO_LAUNCH:          
        	  appState.getHarness().clearFlow();
            cmdResult = new CommandResult(CommandResult.ResultType.XPLAT_FRAME);
            cmdResult.addResult("dialog", ((SerializableDialogPlatform.Dialog)daipc.dialog).getDialog(keyVal));
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
            cmdResult.setNeedsClassAttr(true);
            return cmdResult;
            
          case HAVE_DIALOG_TO_SHOW:
        	  appState.setDialog((SerializableDialogPlatform.Dialog)daipc.dialog);
        	  cmdResult = new CommandResult(CommandResult.ResultType.XPLAT_DIALOG);
        	  cmdResult.addResult("dialog", ((SerializableDialogPlatform.Dialog)appState.getDialog()).getDialog(keyVal));
        	  cmdResult.addResult("currModel", appState.getGenome());
        	  cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
        	  cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState)); 
        	  cmdResult.setNeedsClassAttr(true);
        	  return cmdResult;
            
          case DONE:
            cmdResult = new CommandResult(CommandResult.ResultType.SUCCESS);
            
            Genome currGenome = appState.getDB().getGenome(appState.getGenome());
            String mi = currGenome.getGenomeImage();
            if (mi != null) {
            	cmdResult.addResult("modelAnnotImage", mi);
            }
            
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));
          
            if (daipc.commandResults != null) {
            	for(Map.Entry<String, Object> result : daipc.commandResults.entrySet()) {
            		cmdResult.addResult(result.getKey(), result.getValue());	
            	}
            }
            
            appState.getHarness().clearFlow();
            return (cmdResult);
            
          case INSTALL_MOUSE_MODE:
            DialogAndInProcessCmd.ButtonMaskerCmdState bmcs = (DialogAndInProcessCmd.ButtonMaskerCmdState)daipc.currStateX;
            XPlatMaskingStatus ms = appState.getCommonView().calcDisableControls(bmcs.getMask(), bmcs.pushDisplay());         
            appState.setPendingClick(new WebServerControlFlowHarness.PendingMouseClick());
            cmdResult = new CommandResult(CommandResult.ResultType.WAITING_FOR_CLICK);
            cmdResult.addResult("XPlatMaskingStatus", ms);
            cmdResult.addResult("currModel", appState.getGenome());
            cmdResult.addResult("selectedState", appState.getSUPanel().getSelections());
            cmdResult.addResult("XPlatCurrentState", getMenuStatus(appState));            
            return (cmdResult);
            
          case SIMPLE_USER_FEEDBACK_AND_MOUSE_RESULT:
          case DONE_WITH_SIMPLE_USER_FEEDBACK:
          case DONE_WITH_ERROR_AND_SIMPLE_USER_FEEDBACK:
            // FIXME: Above cases need to be implemented for web app Editor
          case KEEP_PROCESSING: // NOT_EXPECTED
          case NORMAL: // NOT EXPECTED
          case HAVE_USER_INPUTS: // NOT_EXPECTED     
          default:
            throw new GeneratorException("Unexpected State in WebServerApp.processCommand: " + daipc.state);
        }      
      } catch (Exception ex) {
    	  ex.printStackTrace();
    	  String errMsg = ex.getMessage();
    	  if(errMsg == null) {
    		  errMsg = ex.toString();
    	  }
    	  throw new GeneratorException(errMsg,ex);
      }
    }
  }    

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static void main(String argv[]) {
    try {
    
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  
  /***************************************************************************
  **
  ** Fill in simple user feedback
  ** 
  */ 
  
  public void recordSimpleUserFeedback(SimpleUserFeedback suf, ParamSource req) {
    String fb = req.getParameter("formButton");
    String bi = req.getParameter("buttonIndex");
    switch (suf.optionPane) {
      case WARNING:
      case PLAIN:
      case ERROR:
        return;
      case YES_NO_CANCEL_OPTION:
      case YES_NO_OPTION:
        if (fb == null) {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        } else if (fb.equalsIgnoreCase("Cancel")) {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        } else if (fb.equalsIgnoreCase("Yes")) {
          suf.setIntegerResult(SimpleUserFeedback.YES);
        } else if (fb.equalsIgnoreCase("No")) {
          suf.setIntegerResult(SimpleUserFeedback.NO);
        } else {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        }
        return;
      case QUESTION:
        if (fb == null) {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        }
        suf.setObjectResult(fb);
        break;
      case OPTION_OPTION:
        // Caller is responsible for assigning and interpreting YES/NO/CANCEL values
        if (fb == null) {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        }
        Integer iObj = null;
        try {
        iObj = (bi == null) ? null : Integer.valueOf(bi);
        } catch (NumberFormatException nfex) {
          // Just use fact it's still null;
        }
        if (iObj == null) {
          suf.setIntegerResult(SimpleUserFeedback.CANCEL);
        }   
        suf.setIntegerResult(iObj.intValue());  
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Used to load up a ModelTreeCmdState with needed data
  */ 
 
  private DialogAndInProcessCmd.CmdState loadModelTreeFlow(BTState appState, ControlFlow myControlFlow, 
                                                           ParamSource req, DataAccessContext dacx) 
                                                             throws GeneratorException { 
	  
    DialogAndInProcessCmd.CmdState pre = myControlFlow.getEmptyStateForPreload(dacx);
    DialogAndInProcessCmd.ModelTreeCmdState agis = (DialogAndInProcessCmd.ModelTreeCmdState)pre;
    String modelID = req.getParameter("model");
    if (modelID == null) {
      return (null);
    }
    String nodeType = req.getParameter("nodeType");
    if (nodeType == null) {
      return (null);
    }
    
    XPlatModelNode.ModelType mtype = null;
    try {
      mtype = XPlatModelNode.ModelType.valueOf(nodeType.trim());
    } catch (IllegalArgumentException iaex) {
      throw new GeneratorException("Incorrect Model Node Type: " + nodeType);
    }
    
    DataAccessContext dacx2 = new DataAccessContext(dacx, modelID);
    NavTree navTree = dacx2.getGenomeSource().getModelHierarchy();
    TreeNode chosenNode = navTree.resolveNode(new XPlatModelNode.NodeKey(modelID, mtype), dacx2);
    Genome popTarget = null;
    if (chosenNode != null) {
      if (mtype == XPlatModelNode.ModelType.DYNAMIC_PROXY) {
        popTarget = dacx2.getGenomeSource().getDynamicProxy(modelID).getAnInstance();
      } else {
        popTarget = dacx2.getGenomeSource().getGenome(modelID);
      }
    }
    
    if ((popTarget == null) || (chosenNode == null)) {
      return (null);
    }  
    agis.setPreload(popTarget, chosenNode);
    return (pre);
  }
  
  /***************************************************************************
  ** 
  ** Used to load up a PopupCmdState with needed data
  */ 
 
  private DialogAndInProcessCmd.CmdState loadPopupFlow(BTState appState, ControlFlow myControlFlow, ParamSource req, DataAccessContext dacx) {  
    DialogAndInProcessCmd.CmdState pre = myControlFlow.getEmptyStateForPreload(dacx);
    if (pre instanceof DialogAndInProcessCmd.PopupCmdState) {
      DialogAndInProcessCmd.PopupCmdState pcs = (DialogAndInProcessCmd.PopupCmdState)pre;
      String objectID = req.getParameter("objID");
      if (objectID == null) {
        return (null);
      }
      String objSubIDString = req.getParameter("objSubID");
      Object objSubID = null;
      if (objSubIDString != null) {
    	  JSONDeserializer<LinkSegmentID> rezzer = new JSONDeserializer<LinkSegmentID>();
    	  LinkSegmentID lsid = (LinkSegmentID)rezzer.deserialize(objSubIDString);
    	  if (lsid != null) {
    		  objSubID = new MultiSubID(lsid);
    	  }
      }
      Intersection inter = new Intersection(objectID, objSubID, 0.0);

      pcs.setIntersection(inter);
    }
    if (pre instanceof DialogAndInProcessCmd.PopupPointCmdState) {
      DialogAndInProcessCmd.PopupPointCmdState ppcs = (DialogAndInProcessCmd.PopupPointCmdState)pre;
      String popX = req.getParameter("popX");
      String popY = req.getParameter("popY");
      Double xObj = null;
      Double yObj = null;
      try {
        xObj = (popX == null) ? null : Double.valueOf(popX);
        yObj = (popY == null) ? null : Double.valueOf(popY);   
      } catch (NumberFormatException nfex) {
        // Just use fact it's still null;
      }
      if ((xObj == null) || (yObj == null)) {
        return (null);
      }   
      Point2D popupPoint  = new Point2D.Double(xObj.doubleValue(), yObj.doubleValue());
      ppcs.setPopupPoint(popupPoint);
    }   
    if (pre instanceof DialogAndInProcessCmd.AbsolutePopupPointCmdState) {
      DialogAndInProcessCmd.AbsolutePopupPointCmdState appcs = (DialogAndInProcessCmd.AbsolutePopupPointCmdState)pre;
      String popX = req.getParameter("absX");
      String popY = req.getParameter("absY");
      Integer xObj = null;
      Integer yObj = null;
      try {
        xObj = (popX == null) ? null : Integer.valueOf(popX);
        yObj = (popY == null) ? null : Integer.valueOf(popY);   
      } catch (NumberFormatException nfex) {
        // Just use fact it's still null;
      }
      if ((xObj == null) || (yObj == null)) {
        return (null);
      }   
      Point absScreen  = new Point(xObj.intValue(), yObj.intValue());
      appcs.setAbsolutePoint(absScreen);
    }
    if (pre instanceof DialogAndInProcessCmd.MultiSelectCmdState) {
      DialogAndInProcessCmd.MultiSelectCmdState mscs = (DialogAndInProcessCmd.MultiSelectCmdState)pre;
      Database db = appState.getDB();
      String genomeID = appState.getGenome();
      Genome genome = db.getGenome(genomeID);
      String loKey = appState.getLayoutKey();
      Layout layout = db.getLayout(loKey); 
      HashSet<String> genes = new HashSet<String>();
      HashSet<String> nodes = new HashSet<String>();
      HashSet<String> links = new HashSet<String>();
      appState.getSUPanel().getDividedSelections(genome, layout, genes, nodes, links);
      mscs.setMultiSelections(genes, nodes, links);
    }
    return (pre);
  }
  
  /***************************************************************************
  ** 
  ** Used to load up a PopupCmdState with needed data
  */ 
 
  private DialogAndInProcessCmd.CmdState loadOtherFlow(BTState appState, ControlFlow myControlFlow, ParamSource req, DataAccessContext dacx, WebClientState wclState) {  
    DialogAndInProcessCmd.CmdState pre = myControlFlow.getEmptyStateForPreload(dacx);
    if (pre instanceof Selection.StepState) {
      Selection.StepState pcs = (Selection.StepState)pre;
      String objectIDs = req.getParameter("objIDs");
      if (objectIDs == null) {
        return (null);
      }
      ArrayList<Intersection> interList = new ArrayList<Intersection>();
      String[] objs = objectIDs.split("+");
      for (int i = 0; i < objs.length; i++) {
        UiUtil.fixMePrintout("Actually set second argument! Adapt to links as well. Ditch the + argument split bogosity");
        Intersection inter = new Intersection(objs[i], null, 0.0);
        interList.add(inter);    
      }
      pcs.setIntersections(interList);
    } else if (pre instanceof Mover.StepState) {
      Mover.StepState pcs = (Mover.StepState)pre;
      String startX = req.getParameter("startX");
      String startY = req.getParameter("startY");
      String endX = req.getParameter("endX");
      String endY = req.getParameter("endY"); 

      Double sxObj = null;
      Double syObj = null;
      Double exObj = null;
      Double eyObj = null;
      try {
        sxObj = (startX == null) ? null : Double.valueOf(startX);
        syObj = (startY == null) ? null : Double.valueOf(startY);   
        exObj = (endX == null) ? null : Double.valueOf(endX);
        eyObj = (endY == null) ? null : Double.valueOf(endY);   
      } catch (NumberFormatException nfex) {
        // Just use fact it's still null;
      }
      if ((sxObj == null) || (syObj == null) || (exObj == null) || (eyObj == null)){
        return (null);
      }
      UiUtil.fixMePrintout("Give actual pixel diameter"); 
      pcs.setPreload(new Point2D.Double(sxObj.doubleValue(), syObj.doubleValue()), 
                     new Point2D.Double(exObj.doubleValue(), eyObj.doubleValue()), 1.0);      
    } else if (pre instanceof SetCurrentModel.StepState) {
      SetCurrentModel.StepState scm = (SetCurrentModel.StepState)pre;

      String modelID = req.getParameter("modelID");
      if (modelID == null) {
        return (null);
      }
      
      if(modelID.equalsIgnoreCase("default_")) {
        	modelID = dacx.getGenomeSource().getStartupView().getModel();
        	// If there's no startup view...
        	if(modelID == null) {
        	    NavTree navTree = dacx.getGenomeSource().getModelHierarchy();
        	    TreePath dstp = navTree.getDefaultSelection();
        	    modelID = navTree.getGenomeID(dstp);        		
        	}
        }
      
      String nodeIDParma = req.getParameter("nodeIDs");
      String linkIDParam = req.getParameter("linkIDs");
      
      HashSet<String> nodes = new HashSet<String>();
      if (nodeIDParma != null) {
        String[] objs = nodeIDParma.split("\\+");
        for (int i = 0; i < objs.length; i++) {
          UiUtil.fixMePrintout("Ditch the + argument split bogosity");
          nodes.add(objs[i]);    
        }
      }
      
      HashSet<String> links = new HashSet<String>();
      if (linkIDParam != null) {
        String[] objs = linkIDParam.split("\\+");
        for (int i = 0; i < objs.length; i++) {
          UiUtil.fixMePrintout("Ditch the + argument split bogosity");
          links.add(objs[i]);    
        }
      }
      if(wclState == null) {
    	  scm.setPreload(modelID, nodes, links);
      } else {
    	  scm.setPreload(modelID, nodes, links,wclState.getCurrOverlay(),wclState.modsAsTaggedSet(),wclState.shownModsAsTaggedSet());
      }
    } else if (pre instanceof PathGenerator.StepState) {
      PathGenerator.StepState scm = (PathGenerator.StepState)pre;

      String srcParam = req.getParameter("pathSrc");
      String trgParam = req.getParameter("pathTrg");
      String depthParam = req.getParameter("pathDepth");
      String longerParam = req.getParameter("pathInit");
      
      Integer depthObj = null;
      try {
        depthObj = (depthParam == null) ? null : Integer.valueOf(depthParam);
      } catch (NumberFormatException nfex) {
        // Just use fact it's still null;
      }
      Boolean longerObj = (longerParam == null) ? null : Boolean.valueOf(longerParam);
      
      if ((depthObj == null) || (longerObj == null) || (srcParam == null) || (trgParam == null)) {
        return (null);
      }

      scm.setSourceTargAndDepth(srcParam, trgParam, depthObj.intValue(), longerObj.booleanValue());
    } else if (pre instanceof DisplayData.StepState) {
      DisplayData.StepState ddss = (DisplayData.StepState)pre;

      String genomeIDParam = req.getParameter("genomeID");
      String objectIDParam = req.getParameter("objectID");

      if ((genomeIDParam == null) || (objectIDParam == null)) {
        return (null);
      }
      ddss.setLazyData(genomeIDParam, objectIDParam);   
    }
    return (pre);
  }
  
  /***************************************************************************
  ** 
  ** Find the arguments needed to build a ControlFlow.OptionalArgument an add them to a map.
  */ 
  
  private Map<String, String> collectOptArgs(ParamSource req) {
    HashMap<String, String> pVals = new HashMap<String, String>();
    Iterator<String> rpit = req.getKeys();
    while (rpit.hasNext()) {
      String param = rpit.next();
      if (param.indexOf(AbstractOptArgs.OPT_ARG_PREF) == 0) {
        String pVal = req.getParameter(param);
        pVals.put(param, pVal);
      }
    }
    return (pVals);
  }  
 
  /***************************************************************************
  ** 
  ** Find the specific parameters needed from the source
  */
  
  private Map<String, String> collectRequirements(Set<String> reqParams, ParamSource req) {
    HashMap<String, String> pVals = new HashMap<String, String>();
    Iterator<String> rpit = reqParams.iterator();
    while (rpit.hasNext()) {
      String param = rpit.next();
      String pVal = req.getParameter(param);
      if(pVal != null) {
    	  pVals.put(param, pVal);  
      }
    }
    return (pVals);
  }

  /***************************************************************************
  ** 
  ** Answer if we have a dialog cancel
  */
  
  private boolean userCancel(ParamSource req) { 
    return ((req.getParameter("cancel") != null) && req.getParameter("cancel").equalsIgnoreCase("true"));
  }

  /**
   * 
   * 
   * 
   * 
   * 
   * @param req ParamSource containing the HttpRequest
   * @return
   * @throws Exception
   * 
   */
  private WebClientState parseClientState(ParamSource req) throws Exception{
	  JSONDeserializer<WebClientState> rezzer = new JSONDeserializer<WebClientState>();
	  BufferedReader br = null;
	  StringBuilder inputs = null;
	  WebClientState wcs = null;
	  
	  try {
		  InputStream inputStream = ((HSRWrapper)req).getInputStream();
		  inputs = new StringBuilder();
		  if (inputStream != null) {
			  br = new BufferedReader(new InputStreamReader(inputStream));
			  char[] charBuffer = new char[128];
			  int bytesRead = -1;
			  while ((bytesRead = br.read(charBuffer)) > 0) {
				  inputs.append(charBuffer, 0, bytesRead);
			  }
		  } else {
			  inputs.append("");
		  }
		  if(inputs.length() <= 0) {
			  throw new Exception("Nothing to deserialize!");
		  }
		  wcs = rezzer.deserialize(inputs.toString());
		  
	  } catch(Exception e) {
		  if(br != null) {
			  br.close();
		  }
		  throw e;
	  }

	  return wcs;	  
  }
  

  
  
  
  /**
   * 
   * 
   * 
   * 
   * 
   * 
   * @param req
   * @return
   * @throws Exception
   * 
   */
  private UserInputs collectInputs(ParamSource req) throws Exception{
	  	  
	  JSONDeserializer rezzer = new JSONDeserializer();
	  BufferedReader br = null;
	  StringBuilder inputs = null;
	  UserInputs ui = null;
	  
	  if(req.getParameter("cancel") != null && req.getParameter("cancel").equalsIgnoreCase("true")) {
		  return null;
	  }
	  
	  try {
		  InputStream inputStream = ((HSRWrapper)req).getInputStream();
		  
		  inputs = new StringBuilder();
		  
		  if (inputStream != null) {
			  br = new BufferedReader(new InputStreamReader(inputStream));
			  char[] charBuffer = new char[128];
			  int bytesRead = -1;
			  while ((bytesRead = br.read(charBuffer)) > 0) {
				  inputs.append(charBuffer, 0, bytesRead);
			  }
		  } else {
			  inputs.append("");
		  }	  
		  
		  if(inputs.length() <= 0) {
			  throw new Exception("Nothing to deserialize!");
		  }
		  ui = (UserInputs)rezzer.deserialize(inputs.toString());
		  
	  } catch(Exception e) {
		  if(br != null) {
			  br.close();
		  }
		  throw e;
	  }
	  
	  return ui;
  }
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   * A wrapper class for transmitting results from processCommand to a calling method or class.
   * This class contains the ResultType enum, which is used to identify the
   * general kind of response. (This may not necessarily be the object type, though
   * it could also indicate that without a need for reflection.)
   * 
   *
   */
  public static class CommandResult {
	  
	  public enum ResultType {
		  PARAMETER_ERROR,
		  PROCESSING_ERROR,
		  WAITING_FOR_CLICK,
		  ILLEGAL_CLICK_PROCESSED,
		  LEGAL_CLICK_PROCESSED,
		  SUCCESS,
		  CANCEL,
		  XPLAT_FRAME,
		  XPLAT_DIALOG;
	  }
	  
	  private ResultType cmdResultType_;
	  
	  private Map<String,Object> results_;
	  
	  private boolean resultNeedsClassAttr_ = false;
	  
	  // TODO: Object as a type in a Generic is a no-no these days...
	  public CommandResult(ResultType cmdResultType, Map<String,Object> results) {
		  this.cmdResultType_ = cmdResultType;
		  this.results_ = results;
	  }
	  
	  public CommandResult(ResultType cmdResultType) {
		  this.cmdResultType_ = cmdResultType;
	  }
	  
	  public void setNeedsClassAttr(boolean needsClassAttr) {
		  this.resultNeedsClassAttr_ = needsClassAttr;
	  }
	  
	  public boolean resultNeedsClassAttr() {
		  return this.resultNeedsClassAttr_;
	  }
	  
	  public Map<String,Object> getResultsMap() {
		  if(this.results_ == null) {
			  return null;
		  }
		  return Collections.unmodifiableMap(this.results_);
	  }

	  public void setResultsMap(Map<String,Object> results) {
		  this.results_ = results;
	  }

	  public void addResult(String key, Object result) {
		  if(this.results_ == null) {
			  this.results_ = new HashMap<String,Object>();
		  }
		  this.results_.put(key,result);
	  }
	  
	  public Object getResult(String key) {
		  if(this.results_ != null) {
			  return this.results_.get(key);
		  }
		  return null;
	  }
	  
	  public ResultType getResultType() {
		  return this.cmdResultType_;
	  }
	  
	  public void setResultType(ResultType resultType) {
		  this.cmdResultType_ = resultType;
	  }
  }  // CommandResult 
  

  public static class WebClientState {
	  
	  // The current set of selections; this is a Set of objects because
	  // nodes may be identified by strings, but links may be listed as
	  // either Strings or LSIDs.
	  private Set<Object> selections_;
	  
	  // The currently active overlay
	  private String currOverlay_;
	  
	  // The currently enabled modules as a set of NetModuleObjects
	  private Set<NetModuleObject> enabledMods_;
	  
	  public WebClientState() {
		  this(null,null,null);
	  }

	  public WebClientState(Set<Object> selex) {
		  this(selex,null,null);
	  }
	  
	  public WebClientState(String overlay,Set<NetModuleObject> mods) {
		  this(null,overlay,mods);
	  }
	  
	  public WebClientState(String overlay) {
		  this(overlay,null);
	  }
	  
	  public WebClientState(Set<Object> selex,String overlay,Set<NetModuleObject> mods) {
		 this.selections_ = selex;
		 this.currOverlay_ = overlay;
		 this.enabledMods_ = mods;
		 
		 if(this.enabledMods_ != null && this.enabledMods_.size()<=0) {
			 this.enabledMods_.add(new NetModuleObject());
		 }
	  }

	public Set<Object> getSelections() {
		if(this.selections_ != null) {
			return Collections.unmodifiableSet(selections_);	
		}
		return null;
	}

	public void setSelections(Set<Object> selections) {
		this.selections_ = selections;
	}

	public String getCurrOverlay() {
		return this.currOverlay_;
	}

	public void setCurrOverlay(String currOverlay) {
		this.currOverlay_ = currOverlay;
	}
	  
	public Set<NetModuleObject> getEnabledMods() {
		if(this.enabledMods_ != null) {
			return Collections.unmodifiableSet(this.enabledMods_);
		}
		return null;
	}
	
	public void setEnabledMods(Set<NetModuleObject> enabledMods) {
		this.enabledMods_ = enabledMods;
	}
	
	public TaggedSet shownModsAsTaggedSet() {
		TaggedSet thisSet = new TaggedSet();
		HashSet<String> modIds = new HashSet<String>();
		if(this.enabledMods_ != null) {
			for(NetModuleObject mod : this.enabledMods_) {
				if(mod.getShow()) {
					modIds.add(mod.getId());
				}
			}
		}
		thisSet.set = modIds;
		return thisSet;
	}
	
	public TaggedSet modsAsTaggedSet() {
		TaggedSet thisSet = new TaggedSet();
		HashSet<String> modIds = new HashSet<String>();
		if(this.enabledMods_ != null) {
			for(NetModuleObject mod : this.enabledMods_) {
				modIds.add(mod.getId());
			}
		}
		thisSet.set = modIds;
		return thisSet;
	}
	 
	
	public static class NetModuleObject {
		private String id_;
		private boolean show_;
		
		public NetModuleObject() {
			this(null,false);
		}
		
		public NetModuleObject(String id) {
			this(id,false);
		}
		
		public NetModuleObject(String id,boolean show) {
			this.id_ = id;
			this.show_ = show;
		}
		
		public boolean getShow() {
			return this.show_;
		}
		
		public void setShow(boolean show) {
			this.show_ = show;
		}	
		
		public String getId() {
			return this.id_;
		}
		
		public void setId(String id) {
			this.id_ = id;
		}
		
	}
	
  }
   

  
  /***************************************************************************
  ** 
  ** Generic interface allows us to operate outside of server
  */
  
  public interface ParamSource {       
    public String getParameter(String key);
    public Iterator<String> getKeys();
  }
  
  /***************************************************************************
  ** 
  ** Usual way of using interface
  */ 
  
  public static class HSRWrapper implements ParamSource {
     
    private HttpServletRequest hsr_;
     
    public HSRWrapper(HttpServletRequest hsr) {
      hsr_ = hsr;
    }
     
    public String getParameter(String key) {
      return (hsr_.getParameter(key));      
    }

    public Iterator<String> getKeys() {
      HashSet<String> keys = new HashSet<String>();      
      Enumeration e = hsr_.getParameterNames();
      while (e.hasMoreElements()) {
        keys.add((String)e.nextElement());
      }
      return (keys.iterator());
    } 
    
    public InputStream getInputStream() throws Exception{
    	return hsr_.getInputStream();
    }
    
    
  }
  
  /***************************************************************************
  ** 
  ** Testing
  */ 
   
  public static class MapWrapper implements ParamSource {
      
    private Map<String, String> myMap_;
      
    public MapWrapper(Map<String, String> map) {
      myMap_ = map;
    }
      
    public String getParameter(String key) {
      return (myMap_.get(key));      
    }
    
    public Iterator<String> getKeys() {
      return (myMap_.keySet().iterator());      
    }    
    
  }
  
  /***************************************************************************
  ** 
  ** Thrown exceptions include a message as well as an optional wrapped
  ** exception from deep down in the program...
  */

  public static class GeneratorException extends Exception {
    
    private Exception wrapped_;
    private static final long serialVersionUID = 1L;
    
    GeneratorException(String message, Exception wrapped) {
      super(message);
      wrapped_ = wrapped;
    }
    
    GeneratorException(String message) {
      super(message);
      wrapped_ = null;
    }
   
    public Exception getWrappedException() {
      return (wrapped_);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
   
  /****************************************************************************
  **
  ** Implements a NamedOutputStreamSource using nothing at all...
  */

  private class BogoNamedOutputStreamSource implements NamedOutputStreamSource {
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE VARIABLES
    //
    ////////////////////////////////////////////////////////////////////////////
  
    private OutputStream os_;
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
    **
    ** Constructor
    */

    BogoNamedOutputStreamSource(OutputStream os) {
      os_ = os;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /***************************************************************************
    ** 
    ** Implement the interface
    */

    public OutputStream getNamedStream(String streamName) {
      return (os_);
    }
  } 
}