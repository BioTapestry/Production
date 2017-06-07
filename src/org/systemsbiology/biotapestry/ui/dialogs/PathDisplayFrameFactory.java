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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent.XPlatUIElementActionType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Frames for showing a path dialog
*/

public class PathDisplayFrameFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PathDisplayFrameFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    BuildArgs dniba = (BuildArgs)ba;
   
    switch(platform.getPlatform()) {
      case DESKTOP:   
        return (new DesktopPathFrame(cfh.getUI(), cfh.getDataAccessContext(), cfh.getHarnessBuilder(), 
                                     cfh.getTabSource().getCurrentTab(), dniba.genomeID, dniba.srcID, dniba.targID));   
      case WEB:
    	  return new SerializableWindow(dniba.srcID, dniba.targID, cfh.getDataAccessContext().getRMan(), cfh.getCurrDAIPC());
      default:
        throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
    }   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    String genomeID;
    String srcID; 
    String targID;
          
    public BuildArgs(String giKey, String srcID, String targID) {
      super(null);
      this.genomeID = giKey;
      this.srcID = srcID; 
      this.targID = targID;
    }
  }
  
  
  public static class SerializableWindow implements SerializableDialogPlatform.Dialog {
	  
	  private final int DEFAULT_DEPTH_ = 4;
	  
	  private ResourceManager rMan_;
	  private XPlatPrimitiveElementFactory primElemFac_;
	  private XPlatUIDialog dialogWrapper_;
	  
	  XPlatUICollectionElement windowLayoutContainer;
	  int height;
	  int width;

	  public SerializableWindow(String srcId, String trgId, ResourceManager rMan, DialogAndInProcessCmd diagpc) {
		  this.rMan_ = rMan;
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan_);
		  
		  dialogWrapper_ = new XPlatUIDialog(this.rMan_.getString("showPath.title"),250,800);

		  windowLayoutContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  windowLayoutContainer.setParameter("gutters", false);
		  windowLayoutContainer.setParameter("class", "FrameLayoutContainer");
		  windowLayoutContainer.createList("center");
		  windowLayoutContainer.createList("bottom");
		  windowLayoutContainer.setParameter("destroyImmediate", true);
		  
		  dialogWrapper_.addCollection("main", windowLayoutContainer);
		  
		  Map<String,String> postBuildDataLoaded = new HashMap<String,String>();
		  
		  dialogWrapper_.setParameter("postBuildDataLoadElements", postBuildDataLoaded);
		  
		  dialogWrapper_.setParameter("pathSrc", srcId);
		  dialogWrapper_.setParameter("pathTrg", trgId);
		  dialogWrapper_.setParameter("pathDepth", DEFAULT_DEPTH_);
		  
		  XPlatUICollectionElement controlsLayoutContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  controlsLayoutContainer.setParameter("region", "center");
		  
		  controlsLayoutContainer.createList("center");
		  controlsLayoutContainer.createList("bottom");
		  		  
		  windowLayoutContainer.addElement("center", controlsLayoutContainer);
		  
		  XPlatUICollectionElement pathDisplayLayoutContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  pathDisplayLayoutContainer.setParameter("region", "center");
		  pathDisplayLayoutContainer.createList("center");
		  pathDisplayLayoutContainer.createList("bottom");
		  
		  XPlatUICollectionElement lowerControlsLayoutContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  lowerControlsLayoutContainer.setParameter("region", "bottom");
		  lowerControlsLayoutContainer.setParameter("class","pathing_list_container");
		  lowerControlsLayoutContainer.createList("center");
		  
		  XPlatUICollectionElement gridLayoutContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  gridLayoutContainer.setParameter("region", "center");
		  gridLayoutContainer.createList("center");
		  gridLayoutContainer.createList("top");
		  		  
		  lowerControlsLayoutContainer.addElement("center", gridLayoutContainer);
		  
		  lowerControlsLayoutContainer.setParameter("splitter", true);
		  lowerControlsLayoutContainer.setParameter("minSize", 150);

		  controlsLayoutContainer.addElement("bottom", lowerControlsLayoutContainer);
		  controlsLayoutContainer.addElement("center", pathDisplayLayoutContainer);
		  
		  XPlatUIPrimitiveElement modelDrawingArea = this.primElemFac_.makeDrawingArea(200, 200);
		  modelDrawingArea.setParameter("class", "NoWorkspaceCanvas");
		  XPlatUIPrimitiveElement zoomIn = this.primElemFac_.makeBasicButton(this.rMan_.getString("mvpwz.zoomIn"), null, "MAIN_ZOOM_IN", null);
		  XPlatUIPrimitiveElement zoomOut = this.primElemFac_.makeBasicButton(this.rMan_.getString("mvpwz.zoomOut"), null, "MAIN_ZOOM_OUT", null);
		  XPlatUIPrimitiveElement zoomToSelected = this.primElemFac_.makeBasicButton(this.rMan_.getString("mvpwz.zoomToSelected"), null, "MAIN_ZOOM_TO_ALL_SELECTED", null);
		  modelDrawingArea.setParameter("disabledContent", this.rMan_.getString("showPath.noPathAtDepth"));
		  modelDrawingArea.setValidity("havePath", "true");
		  
		  dialogWrapper_.setParameter("canvasId", modelDrawingArea.getParameter("id"));
		  
		  zoomIn.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  zoomIn.setValidity("havePath","true");
		  zoomOut.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  zoomOut.setValidity("havePath","true");
		  zoomToSelected.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  zoomToSelected.setValidity("havePath","true");
		  
		  pathDisplayLayoutContainer.addElement("center", modelDrawingArea);
		  pathDisplayLayoutContainer.addElement("bottom", zoomIn);
		  pathDisplayLayoutContainer.addElement("bottom", zoomOut);
		  pathDisplayLayoutContainer.addElement("bottom", zoomToSelected);
		  
		  Map<String,Object> hopValues = new HashMap<String,Object>();
		  
		  for(int i = 1; i <= Math.max(10,DEFAULT_DEPTH_ + 4); i++) {
			  hopValues.put(Integer.toString(i), Integer.toString(i));
		  }
		  
		  XPlatUIPrimitiveElement hopComboBox = this.primElemFac_.makeTxtComboBox("maxHops", "4", null, this.rMan_.getString("showPath.depth"), hopValues);
		  
		  hopComboBox.setEvent("change",  new XPlatUIEvent("change","CLIENT_PATH_SET_DEPTH"));
		  hopComboBox.getEvent("change").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  hopComboBox.setParameter("bundleAs", "pathDepth");
		  hopComboBox.getEvent("change").addParameter("pathSrc", srcId);
		  hopComboBox.getEvent("change").addParameter("pathTrg", trgId);
		  hopComboBox.setValidity("neverAPath","false");
		  hopComboBox.setParameter("longerOK", true);
		  
		  List<Map<String,Object>> listCols = new ArrayList<Map<String,Object>>();
		  
		  Map<String,Object> display = new HashMap<String,Object>();
		  display.put("label","Display");
		  display.put("field","display");
		  display.put("resizable",false);
		  
		  Map<String,Object> sign = new HashMap<String,Object>();
		  sign.put("label","Sign");
		  sign.put("field","sign");
		  sign.put("width",40);
		  sign.put("resizable",false);
		  
		  Map<String,Object> depth = new HashMap<String,Object>();
		  depth.put("label","Depth");
		  depth.put("field","depth");
		  depth.put("hidden","true");
		  
		  Map<String,Object> end = new HashMap<String,Object>();
		  end.put("label","End");
		  end.put("field","end");
		  end.put("hidden","true");

		  Map<String,Object> start = new HashMap<String,Object>();
		  start.put("label","Start");
		  start.put("field","start");
		  start.put("hidden","true");

		  Map<String,Object> ranking = new HashMap<String,Object>();
		  ranking.put("label","Ranking");
		  ranking.put("field","ranking");
		  ranking.put("hidden","true");
		  
		  Map<String,Object> simpleLoop = new HashMap<String,Object>();
		  simpleLoop.put("label","Simple Loop");
		  simpleLoop.put("field","simpleLoop");
		  simpleLoop.put("hidden","true");
		  
		  Map<String,Object> selections = new HashMap<String,Object>();
		  selections.put("label","Selection Set");
		  selections.put("field","selectionSet");
		  selections.put("hidden","true");
		  
		  Map<String,Object> ids = new HashMap<String,Object>();
		  selections.put("label","ID");
		  selections.put("field","id");
		  selections.put("hidden","true");		 
		  
		  listCols.add(sign);
		  listCols.add(display);
		  listCols.add(depth);
		  listCols.add(end);
		  listCols.add(ranking);
		  listCols.add(start);
		  listCols.add(simpleLoop);
		  
		  XPlatUIPrimitiveElement selectionList = this.primElemFac_.makeListMultiSelection(
			  this.rMan_.getString("showPath.paths"), "path_display_list",null, listCols, null, null,false,false,true
		  );
		  
		  selectionList.setParameter("class", (selectionList.getParameter("class") != null ? selectionList.getParameter("class") : "") + " PathingGrid");
		  selectionList.setEvent("dgrid-select", new XPlatUIEvent("dgrid-select","CLIENT_SELECT_BY_PATH"));
		  selectionList.getEvent("dgrid-select").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  selectionList.setEvent("dgrid-deselect", new XPlatUIEvent("dgrid-deselect","MAIN_SELECT_NONE"));
		  selectionList.getEvent("dgrid-deselect").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  selectionList.setParameter("selectedIndex", 0);
		  selectionList.setValidity("havePath","true");
		  selectionList.setParameter("onDisable", "empty");
		  selectionList.setParameter("cellNavigation", false);
		  
		  postBuildDataLoaded.put((String)selectionList.getParameter("id"),"pathList");
		  postBuildDataLoaded.put((String)hopComboBox.getParameter("id"),"maxDepth");
		  postBuildDataLoaded.put((String)modelDrawingArea.getParameter("id"),"pathMsg");
		  		  
		  gridLayoutContainer.addElement("top", hopComboBox);
		  gridLayoutContainer.addElement("center", selectionList);
		  		  
		  XPlatUIPrimitiveElement closeBtn = this.primElemFac_.makeBasicButton(this.rMan_.getString("dialogs.close"), "pathing_close", "CLIENT_CLOSE_WINDOW", null);
		  closeBtn.addUiElementEventAction("click", XPlatUIElementActionType.DIALOG_CLOSE);
		  closeBtn.getEvent("click").addParameter("windowId", "pathing");
		  
		  windowLayoutContainer.addElement("bottom", closeBtn);
		  
		  dialogWrapper_.addDefaultState_("havePath", "true");
	  }



  	public boolean dialogIsModal() {
  		return false;
  	}
  
  	public SimpleUserFeedback checkForErrors(UserInputs ui) {
  		return null;
  	}
  
  	public XPlatUIDialog getDialog() {
  		return this.getDialog(null);
  	}
  
  	public XPlatUIDialog getDialog(FlowKey keyVal) {
  		return this.dialogWrapper_;
  	}
  
  
  
  	public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
  		// TODO Auto-generated method stub
  		return null;
  	} 
  }
 
  public static class DesktopPathFrame extends JFrame implements DesktopDialogPlatform.Dialog {

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
     
    private PathDisplay pd_;
    private String tabKey_;
    private String genomeKey_;
    private String srcID_;
    private String targID_;
    private UIComponentSource uics_;
    private DataAccessContext dacx_;
    private static final long serialVersionUID = 1L;
     
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    public DesktopPathFrame(UIComponentSource uics, DataAccessContext dacx, HarnessBuilder hBld, String tabKey, String giKey, String srcID, String targID) {
      super();
      uics_ = uics;
      dacx_ = dacx;
      tabKey_ = tabKey;
      URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
      setIconImage(new ImageIcon(ugif).getImage());
      setTitle(dacx_.getRMan().getString("showPath.title"));

      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          uics_.getDataPopupMgr().dropPathWindow();
          if (pd_ != null) {
            pd_.unregister();
          }
        }
        @Override
        public void windowOpened(WindowEvent e) { 
          try {
            pd_.updateNetworkDisplay();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });

      srcID_ = srcID;          
      targID_ = targID;   
      genomeKey_ = giKey;
      boolean showPert = uics_.getIsEditor() && dacx_.getExpDataSrc().getPertData().haveData();
      pd_ = new PathDisplay(uics_, dacx_, hBld, tabKey_, srcID_, targID_, showPert);
          
      ResourceManager rMan = dacx_.getRMan();
      setSize(1200, 550);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      cp.setMinimumSize(new Dimension(200, 200));
      cp.setPreferredSize(new Dimension(200, 200));    
      GridBagConstraints gbc = new GridBagConstraints();    
      UiUtil.gbcSet(gbc, 1, 11, 9, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      cp.add(pd_, gbc);    
      
      //
      // Build the button panel:
      //
  
      FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
      buttonC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            DesktopPathFrame.this.setVisible(false);
            uics_.getDataPopupMgr().dropPathWindow();
            if (pd_ != null) {
              pd_.unregister();
            }
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.add(Box.createHorizontalGlue());    
      buttonPanel.add(buttonC);
      buttonPanel.add(Box.createHorizontalStrut(10));     
  
      //
      // Build the dialog:
      //
      UiUtil.gbcSet(gbc, 0, 21, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      setLocationRelativeTo(uics_.getTopFrame());
    }     

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** These are required. Note we are a frame, i.e. not modal:
    */  
    
    public boolean dialogIsModal() {
      return (false);
    }

    /***************************************************************************
    **
    ** Allows us to stay relevant in a changing world.
    */  
    
    public String getCurrentSource() {
      return (srcID_);         
    }
    public String getCurrentTarget() {
      return (targID_);  
    }
    public String getCurrentModel() {
      return (genomeKey_);  
    }
    public String getTabKey() {
      return (tabKey_);
    } 
   
    public void refresh(String tabKey, String genomeID, String srcID, String targID) {
      genomeKey_ = genomeID;
      targID_ = targID;
      srcID_ = srcID;
      tabKey_ = tabKey;
      pd_.installNewData(null, tabKey, genomeID, srcID,  targID, null);
      return; 
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
  }
}
