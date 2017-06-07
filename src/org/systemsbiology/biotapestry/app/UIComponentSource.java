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

package org.systemsbiology.biotapestry.app;

import java.io.PrintWriter;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.systemsbiology.biotapestry.cmd.flow.WebServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.gaggle.GooseManager;
import org.systemsbiology.biotapestry.nav.DataPopupManager;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.UserTreePathManager;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.ui.CursorManager;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.GroupPanel;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.TextBoxManager;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Working to get the kitchen-sink dependency on BTState object across the
** program. This provides UI components that are appropriate for the current
** tab setting.
*/

public class UIComponentSource implements HandlerAndManagerSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  UIComponentSource(BTState appState) {
    appState_ = appState;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  ** 
  ** EVIL! DO NOT USE! Only here as temporary support for BTState refactoring.
  */
  
  public BTState getAppState() {
    return (appState_);  
  } 
  
  /****************************************************************************
  **
  ** Get TextBoxManager
  */  
   
  public TextBoxManager getTextBoxMgr() {
    return (appState_.getTextBoxMgrX());
  }
  
  /****************************************************************************
  **
  ** Set TextBoxManager
  */  
  
  public void setTextBoxManager(TextBoxManager textMgr) {
    appState_.setTextBoxManagerX(textMgr);
    return;
  }

  /****************************************************************************
  **
  ** Get if is editor
  */  
  
  public boolean getIsEditor() {
    return (appState_.getIsEditorX());
  }
  
  /***************************************************************************
  **
  ** Used to get the ZoomCommandSupport
  */
   
  public ZoomCommandSupport getZoomCommandSupport() {
    return (appState_.getZoomCommandSupportX());
  }
  
  /***************************************************************************
  **
  ** Used to get the ZoomCommandSupport
  */
   
  public ZoomCommandSupport getZoomCommandSupportForTab(int index) {
    return (appState_.getZoomCommandSupportForTabX(index));
  }

  /****************************************************************************
  **
  ** Get load save support
  */  
   
  public LoadSaveSupport getLSSupport() {
    return (appState_.getLSSupport());
  }
 
  /****************************************************************************
  **
  ** Get Cursor Manager
  */  
   
  public CursorManager getCursorMgr() {
    return (appState_.getCursorMgr());
  }

  /****************************************************************************
  **
  ** Answer if headless
  */  
   
  public boolean isHeadless() {
    return (appState_.isHeadless());
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  public DataPopupManager getDataPopupMgr() {
    return (appState_.getDataPopupMgrX());
  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public ImageManager getImageMgr() {
    return (appState_.getImageMgrX());
  } 
  
  /***************************************************************************
  ** 
  ** Get the PlugIn manager
  */
  
  public PlugInManager getPlugInMgr() {
    return (appState_.getPlugInMgrX());  
  } 
  
  /***************************************************************************
  **
  ** Used to get the Zoom target
  */
  
  public ZoomTargetSupport getZoomTarget() {
    return (appState_.getZoomTargetX());
  }

  
  /****************************************************************************
  **
  ** set the Zoom target
  */  
  
  public void setZoomTarget(ZoomTargetSupport zoomer) {
    appState_.setZoomTargetX(zoomer);
    return;
  }

  /***************************************************************************
  ** 
  ** Get the HandlerAndManagerSource
  */
  
  public HandlerAndManagerSource getHandlerAndManagerSource() {
    return (this);  
  } 
 
  /***************************************************************************
  ** 
  ** Get the VirtualZoomControls
  */
  
  public VirtualZoomControls getVirtualZoom() {
    return (appState_.getVirtualZoomX());  
  }   
 
  /***************************************************************************
  ** 
  ** Set the VirtualZoomControls
  */
  
  public void setVirtualZoom(VirtualZoomControls vzc) {
    appState_.setVirtualZoomX(vzc);
    return;
  }

  /***************************************************************************
  ** 
  ** Get the VirtualRecentMenu
  */
  
  public VirtualRecentMenu getRecentMenu() {
    return (appState_.getRecentMenu());  
  }   
  
  /***************************************************************************
  ** 
  ** Get the VirtualGaggleControls
  */
  
  public VirtualGaggleControls getGaggleControls() {
    return (appState_.getGaggleControls());  
  }   
  
  /***************************************************************************
  ** 
  ** Get the VirtualPathControls
  */
  
  public VirtualPathControls getPathControls() {
    return (appState_.getPathControls());  
  }   
  /***************************************************************************
  **
  ** Command
  */ 
       
   public LayoutOptionsManager getLayoutOptMgr() {
     return (appState_.getLayoutOptMgr());
   } 
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  public UserTreePathManager getPathMgr() {
    return (appState_.getPathMgr());
  } 

  /***************************************************************************
  **
  ** Command
  */ 

  public boolean hasPaths() {
    return (appState_.hasPaths());
  }

  /***************************************************************************
  **
  ** Command
  */ 
      
   public GooseManager getGooseMgr() {
     return (appState_.getGooseMgr());
   }  
  
  /****************************************************************************
  **
  ** Get ExceptionHandler
  */  
   
  public ExceptionHandler getExceptionHandler() {
    return (appState_.getExceptionHandlerX());
  }
  /****************************************************************************
  **
  ** Get TopFrame
  */  
   
  public JFrame getTopFrame() {
    return (appState_.getTopFrame());
  }
 
  /****************************************************************************
  **
  ** Get top container
  */  
   
  public JComponent getContentPane() {
    // Hack for headless:
    return (appState_.getContentPane());
  }
   
   /****************************************************************************
   **
   ** Get CommonView
   */  
    
   public CommonView getCommonView() {
     return (appState_.getCommonView());
   }
 
   /****************************************************************************
   **
   ** Get Path Controller
   */  
   
   public UserTreePathController getPathController() {
    return (appState_.getPathControllerX());
   }
   
  /****************************************************************************
  **
  ** Set Path Controller
  */  
  
  public void setPathController(UserTreePathController utpControl) {
    appState_.setPathControllerX(utpControl);
    return;
  }

  /***************************************************************************
  **
  ** Used to get the time slider
  */
  
  public VirtualTimeSlider getVTSlider() {
    return (appState_.getVTSliderX());
  }
  
  /***************************************************************************
  **
  ** Used to get the display panel
  */
  
  public SUPanel getSUPanel() {
    return (appState_.getSUPanelX());
  }
 
  /****************************************************************************
  **
  ** set the display panel
  */  
   
  public void setSUPanel(SUPanel sup) {
    appState_.setSUPanelX(sup);
    return;
  }

  /***************************************************************************
  **
  ** Used to get the group panel
  */
  
  public GroupPanel getGroupPanel() {
    return (appState_.getGroupPanel());
  }
 
  /***************************************************************************
  **
  ** Used to get the tree
  */
  
  public VirtualModelTree getTree() {
    return (appState_.getTreeX());
  }
 
  /****************************************************************************
  **
  ** set the tree
  */  
  
  public void setVmTree(VirtualModelTree vmTree) {
    appState_.setVmTreeX(vmTree);
    return;
  }

  /****************************************************************************
  **
  ** get the Net Overlay Controller
  */  
    
  public NetOverlayController getNetOverlayController() {
    return (appState_.getNetOverlayControllerX());
  }
  
  /****************************************************************************
  **
  ** Set the NetOverlayController
  */  
    
  public void setNetOverlayController(NetOverlayController noc) {
    appState_.setNetOverlayControllerX(noc);
    return;
  }

  /****************************************************************************
  **
  ** get the renderer
  */  
    
  public GenomePresentation getGenomePresentation() {
    return (appState_.getGenomePresentationX());
  }
 
  /****************************************************************************
  **
  ** set the renderer
  */  
    
  public void setGenomePresentation(GenomePresentation myGenomePre) {
    appState_.setGenomePresentationX(myGenomePre);
    return;
  }

  /****************************************************************************
  **
  ** Get if gaggle-enabled
  */  
  
  public boolean getDoGaggle() {
    return (appState_.getDoGaggleX());
  }
  
  /****************************************************************************
  **
  ** Get if using large fonts
  */  
  
  public boolean doBig() {
    return (appState_.doBigX());
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public EventManager getEventMgr() {
    return (appState_.getEventMgrX());
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
     
  public PrintWriter getCurrentPrintWriter() {
    return (appState_.getCurrentPrintWriterX());
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
     
  public void setCurrentPrintWriter(PrintWriter out) {
    appState_.setCurrentPrintWriterX(out);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the SimpleBrowserDialogPlatform.Dialog
  */
  
  public SerializableDialogPlatform.Dialog getDialog() {
    return (appState_.getDialog());
  }   
  
  /***************************************************************************
  ** 
  ** Set the SimpleBrowserDialogPlatform.Dialog
  */
  
  public void setDialog(SerializableDialogPlatform.Dialog lastWebDialog) {
    appState_.setDialog(lastWebDialog);
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get the SimpleUserFeedback
  */
  
  public SimpleUserFeedback getSUF() {
    return (appState_.getSUF());
  }   
  
  /***************************************************************************
  ** 
  ** Set theSimpleUserFeedback
  */
  
  public void setSUF(SimpleUserFeedback lastSuf) {
    appState_.setSUF(lastSuf);
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Get the PendingMouseClick
  */
  
  public WebServerControlFlowHarness.PendingMouseClick getPendingClick() {
    return (appState_.getPendingClick());
  }   
  
  /***************************************************************************
  ** 
  ** Set the PendingMouseClick
  */
  
  public void setPendingClick(WebServerControlFlowHarness.PendingMouseClick pendingClick) {
    appState_.setPendingClick(pendingClick);
    return;
  } 
  
  /****************************************************************************
  **
  ** Set if is editor
  */  
  
  public void setIsEditor(boolean isEdit) {
    appState_.setIsEditor(isEdit);
    return;
  }
  
  /****************************************************************************
  **
  ** Set ExceptionHandler
  */  
   
  public void setExceptionHandler(ExceptionHandler exh) {
    appState_.setExceptionHandlerX(exh);
    return;
  }
  
  /****************************************************************************
  **
  ** Get Resource Manager
  */  
   
  public ResourceManager getRMan() {
    return (appState_.getRManX());
  }
  
  /****************************************************************************
  **
  ** Set TopFrame
  */  
  
  public void setTopFrame(JFrame topFrame, JComponent contentPane) {
    appState_.setTopFrameX(topFrame, contentPane);
    return;
  }
  
  /****************************************************************************
  **
  ** Set CommonView
  */  
   
  public void setCommonView(CommonView cView) {
    appState_.setCommonView(cView);
    return;
  }
  
  /****************************************************************************
  **
  ** Set Cursor Manager
  */  
  
  public void setCursorManager(CursorManager cursorMgr) {
    appState_.setCursorManager(cursorMgr);
    return;
  }
  
  /****************************************************************************
  **
  ** set ZoomCommandSupport
  */  
   
  public void setZoomCommandSupport(ZoomCommandSupport zcs) {
    appState_.setZoomCommandSupportX(zcs);
    return;
  }
  
  /****************************************************************************
  **
  ** set time slider
  */  
  
  public void setVTSlider(VirtualTimeSlider vtSlider) {
    appState_.setVTSliderX(vtSlider);
    return;
  }
  
  
  
  
 
}
