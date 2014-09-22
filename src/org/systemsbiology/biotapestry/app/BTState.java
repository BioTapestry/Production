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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.PopCommands;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.WebServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateDown;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.gaggle.GooseManager;
import org.systemsbiology.biotapestry.nav.DataPopupManager;
import org.systemsbiology.biotapestry.nav.GroupSettingManager;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.LayoutManager;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.OverlayDisplayChange;
import org.systemsbiology.biotapestry.nav.RecentFilesManager;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.UserTreePathManager;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.ui.CursorManager;
import org.systemsbiology.biotapestry.ui.DisplayOptionsManager;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.TextBoxManager;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Essential current application state
*/

public class BTState implements OverlayStateOracle, HandlerAndManagerSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // For server use:
  //
  
  private WebServerControlFlowHarness currCfh_;
  private SerializableDialogPlatform.Dialog lastWebDialog_;
  private SimpleUserFeedback lastSuf_;
  private WebServerControlFlowHarness.PendingMouseClick pendingClick_;
  private File serverBtpDirectory_;
  private File serverConfigDirectory_;
  private String modelListFile_;
  private String fullServletContextPath_;
  
  //
  // General use:
  //
  
  private FlowMeister fm_;
  private File currentFile_;
  private LoadSaveSupport lsSup_;
  private String sid_;
  private Database db_;
  private ResourceManager rMan_;
  private CommonView cView_;
  private SUPanel sup_;
  private PanelCommands pCmds_;
  private PopCommands popCmds_;
  
  //
  // Controls that used to be implemented purely as Swing
  // now can operate in both Swing and Headless mode:
  //
  
  private VirtualModelTree vmTree_;
  private VirtualTimeSlider vtSlider_;
  private VirtualPathControls vpc_;
  private VirtualGaggleControls vgc_;
  private VirtualRecentMenu vrm_;
  private VirtualZoomControls vzc_;
  
  private LayoutManager layoutMgr_;
  private UndoManager undo_;
  private JFrame topFrame_;
  private MainCommands mcmd_;
  private GenomePresentation myGenomePre_;
  private UserTreePathController utpControl_;
  private NetOverlayController noc_; 
  private boolean doGaggle_;
  private String gaggleSpecies_;
  private boolean isEditor_;
  private Map<String, Object> args_;
  private JComponent topContent_;
  private FontRenderContext frc_;
  private FontRenderContext tempFRC_;
  private CursorManager cursorMgr_;
  private boolean isHeadless_;
  private boolean isWebApplication_;
  private TextBoxManager textMgr_;
  private JFrame tempTop_;
  private boolean bigScreen_;
  private ZoomTargetSupport zoomer_;
  private ZoomCommandSupport zcs_;
  private EventManager eventMgr_;
  private DataPopupManager dpm_;
  private DisplayOptionsManager dom_;
  private PlugInManager plum_;
  private UserTreePathManager utpm_;
  
  private String layout_ = null;
  private String genomeKey_ = null; 
  private String currentOverlay_;
  private NetModuleFree.CurrentSettings currOvrSettings_;
  private TaggedSet currentNetMods_;
  private TaggedSet revealed_;
  private boolean showingNetModuleComponents_;
  private PrintWriter out_;
  private ImageManager imageMgr_;
  private RecentFilesManager rfm_;
  private FontManager fom_;
  private GooseManager goomr_;
  private LayoutOptionsManager lopm_;
  private ExceptionHandler exh_;
  
  private GroupSettingManager gsm_;
  private int undoChangeCount_;
  private PropagateDown.DownPropState downProp_; // remember user's last down propagation Genome
  private HashMap<String, Boolean> status_;  // Remember e.g. yes no shutup state
  private SearchModifiers searchMod_; 
  
  private boolean amPulling_;  // Is pulldown mode in effect
  private DynamicDataAccessContext rcx_; // Needed to keep zoom target in sync with current genome.
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor for disaster only
  */ 
   
  public BTState() {
    rMan_ = new ResourceManager(); 
  }
   
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BTState(String session, Map<String, Object> args, boolean isHeadless, boolean isWebApplication) {
    sid_ = session;
    isHeadless_ = isHeadless;
    isWebApplication_ = isWebApplication;
    fm_ = new FlowMeister(this);
    eventMgr_ = new EventManager();
    imageMgr_ = new ImageManager();
    goomr_ = new GooseManager();
    utpm_ = new UserTreePathManager();
    fom_ = new FontManager();
    rfm_ = new RecentFilesManager(this);
    gsm_ = new GroupSettingManager(this);
    lopm_ = new LayoutOptionsManager(this);
    // DB gotta get going before DOM:
    db_ = new Database(this);
    dom_ = new DisplayOptionsManager(this);
    rMan_ = new ResourceManager();
    args_ = args;

    gaggleSpecies_ = ((String)args_.get(ArgParser.GAGGLE));
    doGaggle_ = (gaggleSpecies_ != null);
    Boolean doBig = ((Boolean)args_.get(ArgParser.BIG_SCREEN));
    if (doBig == null) {
      doBig = new Boolean(false);
    }
    bigScreen_ = doBig.booleanValue(); 
    layoutMgr_ = new LayoutManager(this);
    currentNetMods_ = new TaggedSet();
    revealed_ = new TaggedSet();
    showingNetModuleComponents_ = false;    
    dpm_ = new DataPopupManager(this);
    plum_ = new PlugInManager(this);
    undoChangeCount_ = 0;
    downProp_ = new PropagateDown.DownPropState();
    status_ = new HashMap<String, Boolean>();
    searchMod_ = new SearchModifiers();
    amPulling_ = false;
    rcx_ = new DynamicDataAccessContext(this);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  ** 
  ** For setup only! This context is used to drive the zoomer state only
  */
  
  public DynamicDataAccessContext getRenderingContextForZTS() {
    return (rcx_);  
  }     

  /***************************************************************************
  ** 
  ** Get the server BTP directory
  */
  
  public File getServerBtpDirectory() {
    return (serverBtpDirectory_);  
  } 
  
  /***************************************************************************
  ** 
  ** Get the server config files directory
  */
  
  public File getServerConfigDirectory() {
    return (serverConfigDirectory_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the server BTP directory
  */
  
  public void setServerBtpDirectory(String dir) {
    serverBtpDirectory_ = new File(dir); 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Set the server config files directory
  */
  
  public void setServerConfigDirectory(String dir) {
    serverConfigDirectory_ = new File(dir); 
    return;
  } 
 
  /***************************************************************************
  ** 
  ** Get the servlet's context path (needed to access resource files)
  *
  */  
  
  public String getFullServletContextPath() {
	return fullServletContextPath_;
  }

  /***************************************************************************
  ** 
  ** Set the servlet's context path (needed to access resource files)
  *
  */
  
  public void setFullServletContextPath(String fullServletContextPath_) {
	this.fullServletContextPath_ = fullServletContextPath_;
  }

/***************************************************************************
  ** 
  ** Get the list of valid model files (if applicable)
  */
  
  public String getServerBtpFileList() {
    return (modelListFile_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the list of valid model files (if applicable)
  */
  
  public void setServerBtpFileList(String modelListFile) {
    this.modelListFile_ = modelListFile;
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the VirtualZoomControls
  */
  
  public VirtualZoomControls getVirtualZoom() {
    return (vzc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualZoomControls
  */
  
  public void setVirtualZoom(VirtualZoomControls vzc) {
    vzc_ = vzc; 
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get the VirtualRecentMenu
  */
  
  public VirtualRecentMenu getRecentMenu() {
    return (vrm_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualRecentMenu
  */
  
  public void setRecentMenu(VirtualRecentMenu vrm) {
    vrm_ = vrm; 
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the VirtualGaggleControls
  */
  
  public VirtualGaggleControls getGaggleControls() {
    return (vgc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualGaggleControls
  */
  
  public void setGaggleControls(VirtualGaggleControls vgc) {
    vgc_ = vgc; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the VirtualPathControls
  */
  
  public VirtualPathControls getPathControls() {
    return (vpc_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the VirtualPathControls
  */
  
  public void setPathControls(VirtualPathControls vpc) {
    vpc_ = vpc; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the Web Server Control Flow Harness
  */
  
  public WebServerControlFlowHarness getHarness() {
    return (currCfh_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the Web Server Control Flow Harness
  */
  
  public void setHarness(WebServerControlFlowHarness cfh) {
    currCfh_ = cfh; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the SimpleBrowserDialogPlatform.Dialog
  */
  
  public SerializableDialogPlatform.Dialog getDialog() {
    return (lastWebDialog_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the SimpleBrowserDialogPlatform.Dialog
  */
  
  public void setDialog(SerializableDialogPlatform.Dialog lastWebDialog) {
    lastWebDialog_ = lastWebDialog; 
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get the SimpleUserFeedback
  */
  
  public SimpleUserFeedback getSUF() {
    return (lastSuf_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set theSimpleUserFeedback
  */
  
  public void setSUF(SimpleUserFeedback lastSuf) {
    lastSuf_ = lastSuf; 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the PendingMouseClick
  */
  
  public WebServerControlFlowHarness.PendingMouseClick getPendingClick() {
    return (pendingClick_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the PendingMouseClick
  */
  
  public void setPendingClick(WebServerControlFlowHarness.PendingMouseClick pendingClick) {
    pendingClick_ = pendingClick; 
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the Flow meister
  */
  
  public FlowMeister getFloM() {
    return (fm_);  
  }   
  
  /***************************************************************************
  **
  ** Return last down prop target
  */
  
  public PropagateDown.DownPropState getDownProp() {
    return (downProp_);  
  }   

  /***************************************************************************
  **
  ** Are we doing pulldown?
  */
  
  public boolean getAmPulling() {
    return (amPulling_);  
  }   

  /***************************************************************************
  **
  ** Set if we doing pulldown
  */
  
  public void setAmPulling(boolean amPulling) {
    amPulling_ = amPulling;
    return;
  }   
  
    /***************************************************************************
  **
  ** Bump up undo count
  */
  
  public void bumpUndoCount() {
    undoChangeCount_++;  
    return;
  }   

  /***************************************************************************
  **
  ** Clear the change tracking
  */
  
  public void clearUndoTracking() {
    undoChangeCount_ = 0;  
    return;
  }  
 
  /***************************************************************************
  **
  ** Answer if a change has occurred sincce last clear.  FIX ME: We don't
  ** account for undone/redone changes.
  */
  
  public boolean hasAnUndoChange() {
    return (undoChangeCount_ > 0);
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
       
   public LayoutOptionsManager getLayoutOptMgr() {
     return (lopm_);
   } 
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  public UserTreePathManager getPathMgr() {
    return (utpm_);
  } 

  /***************************************************************************
  **
  ** Command
  */ 
      
   public GooseManager getGooseMgr() {
     return (goomr_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   public FontManager getFontMgr() {
     return (fom_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   public RecentFilesManager getRecentFilesMgr() {
     return (rfm_);
   }  
  
  /***************************************************************************
  **
  ** Command
  */ 
      
   public GroupSettingManager getGroupMgr() {
     return (gsm_);
   }  

  /***************************************************************************
  **
  ** Command
  */ 
     
  public void setCurrentPrintWriter(PrintWriter out) {
    out_ = out;
    return;
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
     
  public PrintWriter getCurrentPrintWriter() {
    return (out_);
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
     
  public PlugInManager getPlugInMgr() {
    return (plum_);
  }   
   
  /***************************************************************************
  **
  ** Command
  */ 
     
  public DisplayOptionsManager getDisplayOptMgr() {
    return (dom_);
  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public DataPopupManager getDataPopupMgr() {
    return (dpm_);
  } 
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  public EventManager getEventMgr() {
    return (eventMgr_);
  } 
   
  /***************************************************************************
  **
  ** Command
  */ 
    
  public ImageManager getImageMgr() {
    return (imageMgr_);
  } 
 
  /****************************************************************************
  **
  ** Push down a temp top frame
  */  
   
  public void pushTopFrame(JFrame newTop) {
    tempTop_ = newTop;
    return;
  }  
  
  /****************************************************************************
   **
   ** Pop a temp top frame
   */  
    
   public void popTopFrame() {
     tempTop_ = null;
     return;
   }  
 
   /****************************************************************************
   **
   ** Push down a temp FRC
   */  
     
   public void pushFRC(FontRenderContext tempFRC) {
     tempFRC_ = tempFRC;
     return;
   }  
    
   /****************************************************************************
   **
   ** Pop a temp FRC
   */  
      
   public void popFRC() {
     tempFRC_ = null;
     return;
   }  
    
  /****************************************************************************
  **
  ** Get Database
  */  
   
  public Database getDB() {
    return (db_);
  }
  
  /****************************************************************************
  **
  ** Get Resource Manager
  */  
   
  public ResourceManager getRMan() {
    return (rMan_);
  }
  
  /****************************************************************************
  **
  ** Get Args
  */  
   
  public Map<String, Object> getArgs() {
    return (args_);
  }
  
  /****************************************************************************
  **
  ** Set TopFrame
  */  
  
  public BTState setTopFrame(JFrame topFrame, JComponent contentPane) {
    topFrame_ = topFrame;
    topContent_ = contentPane;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Set ExceptionHandler
  */  
   
  public void setExceptionHandler(ExceptionHandler exh) {
    synchronized (this) {
      exh_ = exh;
    }
    return;
  }
  
  /****************************************************************************
  **
  ** Get ExceptionHandler
  */  
   
  public ExceptionHandler getExceptionHandler() {
    synchronized (this) {
      return (exh_);
    }
  }
  
  /****************************************************************************
  **
  ** Set status value
  */  
   
  public void setStatus(String key, boolean val) {
    status_.put(key, Boolean.valueOf(val));
    return;
  }
  
  /****************************************************************************
  **
  ** Get status value
  */  
   
  public Boolean getStatus(String key) {
    return (status_.get(key));
  }  
   
  /****************************************************************************
  **
  ** Answer if headless
  */  
   
  public boolean isHeadless() {
    return (isHeadless_);
  }
 
  /****************************************************************************
  **
  ** Answer if web application
  */  
   
  public boolean isWebApplication() {
    return (isWebApplication_);
  }
  
  /****************************************************************************
  **
  ** Get TopFrame
  */  
   
  public JFrame getTopFrame() {
    if (tempTop_ != null) {
      return (tempTop_);
    }
    return (topFrame_);
  }
 
  /****************************************************************************
  **
  ** Get top container
  */  
   
  public JComponent getContentPane() {
    // Hack for headless:
    return ((topContent_ == null) ? new JPanel() : topContent_);
  }
   
  /****************************************************************************
  **
  ** Set current file
  */  
  
  public void setCurrentFile(File currentFile) {
    currentFile_ = currentFile;
    return;
  }
  
  /****************************************************************************
  **
  ** Get current file
  */  
   
  public File getCurrentFile() {
    return (currentFile_);
  }
  
  /****************************************************************************
  **
  ** Set load save support
  */  
  
  public void setLSSupport(LoadSaveSupport lsSup) {
    lsSup_ = lsSup;
    return;
  }
  
  /****************************************************************************
  **
  ** Get load save support
  */  
   
  public LoadSaveSupport getLSSupport() {
    return (lsSup_);
  }
  
  /****************************************************************************
   **
   ** Set CommonView
   */  
   
   public BTState setCommonView(CommonView cView) {
     cView_ = cView;
     return (this);
   }
   
   /****************************************************************************
   **
   ** Get CommonView
   */  
    
   public CommonView getCommonView() {
     return (cView_);
   }
  
  /****************************************************************************
  **
  ** Set Cursor Manager
  */  
  
  public BTState setCursorManager(CursorManager cursorMgr) {
    cursorMgr_ = cursorMgr;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Cursor Manager
  */  
   
  public CursorManager getCursorMgr() {
    return (cursorMgr_);
  }
  
  /****************************************************************************
  **
  ** Set Path Controller
  */  
  
  public BTState setPathController(UserTreePathController utpControl) {
    utpControl_ = utpControl;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Path Controller
  */  
   
  public UserTreePathController getPathController() {
    return (utpControl_);
  }

  /****************************************************************************
  **
  ** Set TextBoxManager
  */  
  
  public BTState setTextBoxManager(TextBoxManager textMgr) {
    textMgr_ = textMgr;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get TextBoxManager
  */  
   
  public TextBoxManager getTextBoxMgr() {
    return (textMgr_);
  }

  /****************************************************************************
  **
  ** Get Font render context
  */  
   
  public FontRenderContext getFontRenderContext() {
    if (tempFRC_ != null) {
      return (tempFRC_);
    }
    return (frc_);
  }
  
 /****************************************************************************
  **
  ** Set Font render context
  */  
   
  public void setFontRenderContext(FontRenderContext frc) {
    frc_ = frc;
    return;
  }
  
  /****************************************************************************
  **
  ** Set if is editor
  */  
  
  public BTState setIsEditor(boolean isEdit) {
    isEditor_ = isEdit;
    // needs to be initialized even for viewer since tree navigation creates undos that are tossed away...
    undo_ = new UndoManager();
    undo_.setLimit(50); 
    return (this);
  }
 
  /****************************************************************************
  **
  ** Get if is editor
  */  
  
  public boolean getIsEditor() {
    return (isEditor_);
  }
  
  /****************************************************************************
  **
  ** Get if gaggle-enabled
  */  
  
  public boolean getDoGaggle() {
    return (doGaggle_);
  }
  
  /****************************************************************************
  **
  ** Get if using large fonts
  */  
  
  public boolean doBig() {
    return (bigScreen_);
  }
   
  /****************************************************************************
  **
  ** Get gaggle species
  */  
  
  public String getGaggleSpecies() {
    return (gaggleSpecies_);
  }
   
  /****************************************************************************
  **
  ** set Pop Commands
  */  
  
  public BTState setPopCmds(PopCommands popCmds) {
    popCmds_ = popCmds;
    return (this);
  }
  
  /****************************************************************************
   **
   ** Get Pop Commands
   */  
   
   public PopCommands getPopCmds() {
     return (popCmds_);
   } 
  
  /****************************************************************************
  **
  ** Get Panel Commands
  */  
  
  public PanelCommands getPanelCmds() {
    return (pCmds_);
  }
  
  /****************************************************************************
  **
  ** set Panel Commands
  */  
  
  public BTState setPanelCmds(PanelCommands pCmds) {
    pCmds_ = pCmds;
    return (this);
  }
  
  /****************************************************************************
  **
  ** Get Main Commands
  */  
  
  public MainCommands getMainCmds() {
    return (mcmd_);
  }
  
  /****************************************************************************
  **
  ** set Main Commands
  */  
  
  public BTState setMainCmds(MainCommands mcmd) {
    mcmd_ = mcmd;
    return (this);
  }

  /****************************************************************************
   **
   ** set ZoomCommandSupport
   */  
   
   public BTState setZoomCommandSupport(ZoomCommandSupport zcs) {
     zcs_ = zcs;
     return (this);
   }
   
   /***************************************************************************
   **
   ** Used to get the ZoomCommandSupport
   */
   
   public ZoomCommandSupport getZoomCommandSupport() {
     return (zcs_);
   }
  
  /****************************************************************************
  **
  ** set time slider
  */  
  
  public BTState setVTSlider(VirtualTimeSlider vtSlider) {
    vtSlider_ = vtSlider;
    return (this);
  }
  
  /***************************************************************************
  **
  ** Used to get the time slider
  */
  
  public VirtualTimeSlider getVTSlider() {
    return (vtSlider_);
  }
  
  /***************************************************************************
  **
  ** Used to get the display panel
  */
  
  public SUPanel getSUPanel() {
    return (sup_);
  }
 
  /****************************************************************************
  **
  ** set the display panel
  */  
   
  public BTState setSUPanel(SUPanel sup) {
    sup_ = sup;
    return (this);
  }
  
  /***************************************************************************
  **
  ** Used to get the layout manager
  */
  
  public LayoutManager getLayoutMgr() {
    return (layoutMgr_);
  } 
  
  /****************************************************************************
  **
  ** set the tree
  */  
  
  public BTState setVmTree(VirtualModelTree vmTree) {
    vmTree_ = vmTree;
    return (this);
  }
 
  /***************************************************************************
  **
  ** Used to get the tree
  */
  
  public VirtualModelTree getTree() {
    return (vmTree_);
  }
  
  /****************************************************************************
  **
  ** set the Zoom target
  */  
  
  public BTState setZoomTarget(ZoomTargetSupport zoomer) {
    zoomer_ = zoomer;
    return (this);
  }
 
  /***************************************************************************
  **
  ** Used to get the Zoom target
  */
  
  public ZoomTargetSupport getZoomTarget() {
    return (zoomer_);
  }
    
  /***************************************************************************
  **
  ** Used to get the undo manager
  */
  
  public UndoManager getUndoManager() {
    return (undo_);
  }  

  /****************************************************************************
  **
  ** set the renderer
  */  
    
  public BTState setGenomePresentation(GenomePresentation myGenomePre) {
    myGenomePre_ = myGenomePre;
    return (this);
  }
  
  /****************************************************************************
  **
  ** get the Net Overlay Controller
  */  
    
  public NetOverlayController getNetOverlayController() {
    return (noc_);
  }
  
  /****************************************************************************
  **
  ** Set the NetOverlayController
  */  
    
  public BTState setNetOverlayController(NetOverlayController noc) {
    noc_ = noc;
    return (this);
  }
  
  /****************************************************************************
  **
  ** get the renderer
  */  
    
  public GenomePresentation getGenomePresentation() {
    return (myGenomePre_);
  }
  
  /***************************************************************************
  **
  ** Set the genome without a selection change; used during UNDO/ REDO ONLY!
  */
  
  public void setGenomeForUndo(String key, DataAccessContext dacx) {
    genomeKey_ = key;
     //
     // Gotta keep the DataAccessContext in sync!
     //
     dacx.setGenome((key == null) ? null : dacx.getGenomeSource().getGenome(key));
     dacx.setLayout((key == null) ? null : dacx.lSrc.getLayoutForGenomeKey(key));
    textMgr_.setMessageSource(genomeKey_, TextBoxManager.MODEL_MESSAGE, true);
    
    NetOverlayController noc = getNetOverlayController();
   // if ((key != null) && noc.hasPreloadForNextGenome(key)) {
      noc.setForNewGenome(genomeKey_, null, null, dacx);
  //  }
    rcx_.setGenome((genomeKey_ == null) ? null : rcx_.getGenomeSource().getGenome());
    //zoomer_.setGenome(key); now handled through rcx_ state change above!
    return;
  }  
   
  /***************************************************************************
  **
  ** Used to set the genome
  */
  
  public void setGenome(String key, UndoSupport support, DataAccessContext dacx) {	
    getPanelCmds().cancelAddMode(PanelCommands.CANCEL_ADDS_ALL_MODES);
    boolean updateModules = false;
   
    // Note this omits case of (genomeKey == null) && (key != null), which
    // OK in the context of clearing selections (key == null means nothing is
    // selected and nothing needs clearing:
    if ((key == null) || ((genomeKey_ != null) && !genomeKey_.equals(key))) {
      // FIX ME:  Better yet, cache selections and reinstall later?
      DataAccessContext rcx2 = new DataAccessContext(rcx_, genomeKey_, layout_);
      myGenomePre_.clearSelections(rcx2, support);
      updateModules = true;
    }
     
     //
     // For module updates, we need to catch the extra case, plus when we are waiting for
     // a user path change:
     OverlayDisplayChange odc = null;
     NetOverlayController noc = getNetOverlayController();
     
     if (updateModules || ((genomeKey_ == null) && (key != null)) || noc.hasPreloadForNextGenome(key)) {
       updateModules = true;
       // Gotta send all the old info to the overlay controller so it can generate
       // consistent undo packages:
       odc = new OverlayDisplayChange();
       odc.oldGenomeID = genomeKey_;
       odc.oldOvrKey = currentOverlay_;
       odc.oldModKeys = currentNetMods_.clone();
       odc.oldRevealedKeys = revealed_.clone();
     }
     genomeKey_ = key;
     //
     // Gotta keep the DataAccessContext in sync!
     //
     dacx.setGenome((key == null) ? null : dacx.getGenomeSource().getGenome(key));
     dacx.setLayout((key == null) ? null : dacx.lSrc.getLayoutForGenomeKey(key));
     
     textMgr_.setMessageSource(genomeKey_, TextBoxManager.MODEL_MESSAGE, true);
     if (updateModules) {
       noc.setForNewGenome(genomeKey_, support, odc, dacx);
     }
     rcx_.setGenome((genomeKey_ == null) ? null : rcx_.getGenomeSource().getGenome());
     //zoomer_.setGenome(key); now handled through rcx_ state change above!

     return; 
  } 
  
  /***************************************************************************
  **
  ** Used to get the genome
  */
  
  public String getGenome() {
    return (genomeKey_);
  }
  
  /***************************************************************************
  **
  ** Used to get the layout
  */
   
  public String getLayoutKey() {
    return (layout_);
  } 
  
  /***************************************************************************
  **
  ** Set the layout
  */
  
  public void setGraphLayout(String key) {
    layout_ = key;
    rcx_.setLayout(((layout_ == null) ? null : rcx_.lSrc.getLayout(layout_)));
    //zoomer_.setGraphLayout(key); now handled through rcx_ state change above!
    return;
  }
  
  /***************************************************************************
  **
  ** Get the current network overlay key
  */
  
  public String getCurrentOverlay() {
    return (currentOverlay_);
  } 
  
  /***************************************************************************
  **
  ** Set the current network module
  */
  
  public void setCurrentNetModules(TaggedSet keys, boolean forUndo) {
    currentNetMods_ = new TaggedSet(keys);
    String currKey = (currentNetMods_.set.size() == 1) ? (String)currentNetMods_.set.iterator().next() : null;
    textMgr_.setMessageSource(currKey, TextBoxManager.NETMOD_MESSAGE, forUndo);
    zoomer_.setCurrentNetModules(keys);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the currently revealed modules
  */
  
  public void setRevealedModules(TaggedSet keys, boolean forUndo) {
    this.revealed_ = new TaggedSet(keys);
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the current network module keys
  */
  
  public TaggedSet getCurrentNetModules() {
    return (new TaggedSet(currentNetMods_));
  } 

  /***************************************************************************
  **
  ** Get the current overlay settings
  */
  
  public NetModuleFree.CurrentSettings getCurrentOverlaySettings() {
    return (currOvrSettings_);
  } 

  /***************************************************************************
  **
  ** Get the set of modules that are showing their contents
  */  
  
  public TaggedSet getRevealedModules() {
    return (revealed_);
  }  
  
  /***************************************************************************
  **
  ** Answer if module components are being displayed
  */  
  
  public boolean showingModuleComponents() {
    return (showingNetModuleComponents_);
  } 
  
  /***************************************************************************
  **
  ** Set the current network overlay key
  */
  
  public void installCurrentSettings(NetModuleFree.CurrentSettings settings) { 
    currOvrSettings_ = settings;
  }
  
  /***************************************************************************
  **
  ** Set the current network overlay key
  */
  
  public SelectionChangeCmd.Bundle setCurrentOverlay(String key, boolean forUndo) { 
    
    //
    // Note that during set genome ops, by the time we get here we don't have the
    // old genome ID, and the layout is stale.  So it is involved to figure out if
    // we were showing links before....
    
    currentOverlay_ = key;
    
    Database db = getDB();
    Layout lo = db.getLayout(getLayoutKey());     
    SelectionChangeCmd.Bundle bundle = null;
    if ((currentOverlay_ != null) && !forUndo) {
      NetOverlayProperties nop = lo.getNetOverlayProperties(currentOverlay_);
      if (nop.hideLinks()) {
        DataAccessContext rcx2 = new DataAccessContext(rcx_, genomeKey_, layout_);
        bundle = sup_.dropLinkSelections(true, null, rcx2);
      }
    }
    textMgr_.setMessageSource(currentOverlay_, TextBoxManager.OVERLAY_MESSAGE, forUndo);
    zoomer_.setOverlay(key);
    return (bundle);
  }
  
  /***************************************************************************
  **
  ** Set the state to show module components.
  */
  
  public void toggleModuleComponents() {
    showingNetModuleComponents_ = !showingNetModuleComponents_;
 //   if (isShowBubbleMode(currentMode_)) {
  //    pushedShowBubbles_ = showBubbles_;
  //  }
    return;
  }
  
  /***************************************************************************
  **
  ** Get bounds of drawing canvas
  */
  
  public Dimension getCanvasSize() {
    Rectangle rect = getDB().getWorkspace().getWorkspace();
    return (new Dimension(rect.width, rect.height));
  }

  /***************************************************************************
  **
  ** Answer if we are outside bounds
  */
  
  public boolean modelIsOutsideWorkspaceBounds() {
    return (!getDB().getWorkspace().contains(getZoomTarget().getAllModelBounds()));
  }  

  /***************************************************************************
  **
  ** Get aspectRatio drawing canvas
  */
  
  public double getCanvasAspectRatio() {
    return (Workspace.ASPECT_RATIO);
  }  
  
  /***************************************************************************
  **
  ** Get center of drawing canvas (wrt canvas origin)
  */
  
  public Point2D getCanvasCenter() {
    Rectangle rect = getDB().getWorkspace().getWorkspace();
    return (new Point2D.Double(rect.getWidth() / 2.0, rect.getHeight() / 2.0));
  } 
  
  /***************************************************************************
  **
  ** Go from Genome ID key to Layout
  */
  
  public Layout getLayoutForGenomeKey(String key) {
    Database db = getDB();
    return (db.getLayout(db.mapGenomeKeyToLayoutKey(key)));
  } 
  
  /***************************************************************************
  **
  ** Search modifiers
  */
  
  public SearchModifiers getSearchModifiers() {
    return (searchMod_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Persistent modifiers for search 
  */ 
    
  public static class SearchModifiers  {
    public boolean includeLinks;
    public boolean appendToCurrent;
    public boolean includeQueryNode;                  
 
    SearchModifiers() {
     // Init to legacy options
      includeLinks = true;
      includeQueryNode = true;
      appendToCurrent = false;
    }
  } 
}
