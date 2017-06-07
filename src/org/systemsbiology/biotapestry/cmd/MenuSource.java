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


package org.systemsbiology.biotapestry.cmd;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNodeToModule;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNodeToSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.add.AddSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.display.DisplayPaths;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditGroupProperties;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.BuilderPluginArg;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveModuleOrPart;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.select.Selection;
import org.systemsbiology.biotapestry.cmd.flow.simulate.SimulationLaunch;
import org.systemsbiology.biotapestry.cmd.flow.userPath.PathStop;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMembership;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.plugin.SimulatorPlugIn;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.menu.XPlatAbstractAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatKeyBindings;
import org.systemsbiology.biotapestry.ui.menu.XPlatKeypressAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenu;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenuBar;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenuItem;
import org.systemsbiology.biotapestry.ui.menu.XPlatMenuItem.XPType;
import org.systemsbiology.biotapestry.ui.menu.XPlatPlaceholder;
import org.systemsbiology.biotapestry.ui.menu.XPlatSeparator;
import org.systemsbiology.biotapestry.ui.menu.XPlatTab;
import org.systemsbiology.biotapestry.ui.menu.XPlatToggleAction;
import org.systemsbiology.biotapestry.ui.menu.XPlatToolBar;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Source for menus
*/

public class MenuSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public enum PopType {GENE, NODE, LINK, LINK_POINT, NOTE, REGION, OVERLAY, MODULE, MODULE_LINK, MODULE_LINK_POINT};
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private final static String MENU_BAR_KEY_ = "MenuBar";  
  private final static String TOOL_BAR_KEY_ = "ToolBar"; 
  private final static String MODEL_TREE_KEY_ = "ModelTreeMenu";
  private final static String TAB_KEY_ = "TabMenu";
  private final static String POPUP_KEY_ = "PopupMenu";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private FlowMeister flom_;
  private boolean viewerOnly_;
  private boolean doGaggle_;
  private PlugInManager pluginManager_;
  private UIComponentSource uics_;
  private TabSource tSrc_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MenuSource(UIComponentSource uics, TabSource tSrc, CmdSource cSrc) {
	  flom_ = cSrc.getFloM();
	  uics_ = uics;
	  viewerOnly_ = !uics_.getIsEditor();
	  doGaggle_ = uics_.getDoGaggle();
	  tSrc_ = tSrc;
	  pluginManager_ = uics_.getPlugInMgr();
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get what is, and is not, supported:
  */ 
      
  public SupportedMenus getSupportedMenuClasses() {
    HashSet<String> menuRet = new HashSet<String>();
    HashSet<PopType> popupTypes = new HashSet<PopType>();
    boolean keyStrokes;
    
    if (viewerOnly_) {
      menuRet.add(getXPlatToolBarKey());
      menuRet.add(getXPlatPopupKey());
      popupTypes.add(PopType.GENE);
      popupTypes.add(PopType.NODE);
      popupTypes.add(PopType.LINK);
      popupTypes.add(PopType.REGION);
      popupTypes.add(PopType.MODULE);
      keyStrokes = false;
    } else {
      menuRet.add(getXPlatMenuBarKey());
      menuRet.add(getXPlatToolBarKey());
      menuRet.add(getXPlatTreeKey());
      menuRet.add(getXPlatPopupKey());
      menuRet.add(getXPlatTabKey());
      popupTypes.add(PopType.GENE);
      popupTypes.add(PopType.NODE);
      popupTypes.add(PopType.LINK);
      popupTypes.add(PopType.LINK_POINT);
      popupTypes.add(PopType.NOTE);
      popupTypes.add(PopType.REGION);
      popupTypes.add(PopType.OVERLAY);
      popupTypes.add(PopType.MODULE);
      popupTypes.add(PopType.MODULE_LINK);
      popupTypes.add(PopType.MODULE_LINK_POINT);
      keyStrokes = true;
    }
    return new SupportedMenus(menuRet, popupTypes, keyStrokes);   
  }
  
  public String getXPlatMenuBarKey() {
    return (MENU_BAR_KEY_);
  }
  
  public String getXPlatToolBarKey() {
    return (TOOL_BAR_KEY_);
  }
  
  public String getXPlatTreeKey() {
    return (MODEL_TREE_KEY_);
  }

  public String getXPlatTabKey() {
    return (TAB_KEY_);
  }
  
  public String getXPlatPopupKey() {
    return (POPUP_KEY_);
  }

  /***************************************************************************
  **
  ** Get key bindings:
  */ 
      
  public XPlatKeyBindings getXPlatKeyBindings() throws InvalidMenuRequestException {
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(true);
    }
    XPlatKeyBindings keyBind = new XPlatKeyBindings();
    
    HashSet<String> cxTags = new HashSet<String>();
    cxTags.add("ESCAPE"); 
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.CANCEL_MODE, cxTags, "BioTapCancel", false));
 
    HashSet<String> delTags = new HashSet<String>();
    delTags.add("DELETE"); 
    delTags.add("BACK_SPACE"); 
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.MULTI_DELETE, delTags, "BioTapDelete", true));
    
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.NUDGE_UP, KeyEvent.VK_UP, InputEvent.SHIFT_MASK, "BioTapNudgeUp", true));
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.NUDGE_DOWN, KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK, "BioTapNudgeDown", true));
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.NUDGE_LEFT, KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, "BioTapNudgeLeft", true));
    keyBind.addPress(new XPlatKeypressAction(FlowMeister.KeyFlowKey.NUDGE_RIGHT, KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK, "BioTapNudgeRight", true));
  
    return (keyBind);
  }
   
   
  /*************
   * getTabs
   ************* 
   * 
   * Returns an Array of the current set of tabs based on tabOrder_
   * which contains XPlatTab objects, which house information needed
   * by the WebClient
   * 
   * 
   */
  public ArrayList<XPlatTab> getTabs() {
    
    ArrayList<XPlatTab> tabs = new ArrayList<XPlatTab>();
    for(TabSource.AnnotatedTabData tab : tSrc_.getTabs()) {
      tabs.add(new XPlatTab(tab.dbID, tab.tnd.getTitle(), tab.tnd.getFullTitle()));
    }
   return tabs; 
    
  }
  
  /***************************************************************************
  **
  ** Get specified menu bar:
  */ 
      
  public XPlatMenuBar getXPlatMenuBar(DataAccessContext dacx) throws InvalidMenuRequestException {
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(MENU_BAR_KEY_);
    }
    return (defineEditorMenuBar(dacx));
  }
  
  /***************************************************************************
  **
  ** Get specified tool bar:
  */ 
        
  public XPlatToolBar getXPlatToolBar(DataAccessContext dacx) {
    if (viewerOnly_) {
      return (defineViewerToolBar(dacx));
    } else {
      return (defineEditorToolBar(dacx));
    }   
  }
    
  /***************************************************************************
  **
  ** Get specified popup:
  */ 
      
   public XPlatMenu getXPlatTreePopup(DataAccessContext dacx) throws InvalidMenuRequestException {
     if (viewerOnly_) {
       throw new InvalidMenuRequestException(MODEL_TREE_KEY_);
     }
     return (defineEditorTreePopup(dacx));
   }
   
   
   public XPlatMenu getXPlatTabPopup(DataAccessContext dacx) throws InvalidMenuRequestException {
     if (viewerOnly_) {
       throw new InvalidMenuRequestException(TAB_KEY_);
     }
     return (defineEditorTabPopup(dacx));
   }
   
   
   /***************************************************************************
   **
   ** Get specified popup. Throws exception if the requested popup is not supported
   ** for the current program state (EDITOR vs VIEWER)
   */ 
          
   public XPlatMenu getXPlatPopup(PopType pType, String objectID, boolean forSelected, DataAccessContext rcx) 
                                    throws InvalidMenuRequestException {
     return (getXPlatPopup(pType, objectID, forSelected, null, rcx));
   }
     
   /***************************************************************************
   **
   ** Get specified popup. Last arg is REQUIRED for MODULE PopType!. Throws exception if
   ** request is not supported for VIEWER versus EDITOR
   */ 
          
   public XPlatMenu getXPlatPopup(PopType pType, String objectID, boolean forSelected, 
                                  NetModuleFree.IntersectionExtraInfo forModule, DataAccessContext rcx)
                                    throws InvalidMenuRequestException {
     Genome genome = rcx.getCurrentGenome();
     Layout layout = rcx.getCurrentLayout();
     String overlayKey = rcx.getOSO().getCurrentOverlay();
     switch (pType) {
       case GENE:         
         return (defineNodeMenuPopup(genome, objectID, true, overlayKey, forSelected, rcx));
       case NODE:
         return (defineNodeMenuPopup(genome, objectID, false, overlayKey, forSelected, rcx));
       case LINK:
         return (defineLinkMenuPopup(forSelected, rcx));
       case LINK_POINT:
         return (defineLinkPointPopup(rcx));
       case NOTE:
         return (defineNotePopup(rcx));
       case REGION:
         if (!(genome instanceof GenomeInstance)) {
           throw new IllegalStateException();
         }
         GenomeInstance gi = (GenomeInstance)genome;
         return (defineRegionPopup(gi, objectID, rcx));
       case OVERLAY:        
         return (defineOverlayPopup(rcx));
       case MODULE:
         return (defineNetModulePopup(layout, overlayKey, objectID, forModule, rcx));
       case MODULE_LINK:
         return (defineModLinkMenuPopup(rcx));
       case MODULE_LINK_POINT:
         return (defineModuleLinkPointPopup(rcx));
       default:
         throw new IllegalArgumentException();    
     }
   }
  
  /***************************************************************************
  **
  ** Fill in if items are active or inactive:
  */ 
  
  public void activateXPlatPopup(XPlatMenu xpMenu, BuildInfo bifo, String objectID, 
                                 Object subID, DataAccessContext rcx, 
                                 UIComponentSource uics, CmdSource cSrc) {
    
    StaticDataAccessContext sdacx = new StaticDataAccessContext(rcx);
    Iterator<XPlatMenuItem> iit = xpMenu.getItems();
    while (iit.hasNext()) {
      XPlatMenuItem xpmi = iit.next();
      // Do not do items tagged for a false condition!
      String condition = xpmi.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not do items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }
      XPType xpt = xpmi.getType();
      switch (xpt) {
        case MENU:
          activateXPlatPopup((XPlatMenu)xpmi, bifo, objectID, subID, rcx, uics, cSrc);
          break;
        case ACTION:
        case CHECKBOX_ACTION:
          XPlatAbstractAction xpa = (XPlatAbstractAction)xpmi;
          FlowMeister.FlowKey key = xpa.getKey();
          ControlFlow.OptArgs args = xpa.getActionArg();
          ControlFlow cf = flom_.getControlFlow(key, args);
          Intersection inter = new Intersection(objectID, subID, 0.0);
          boolean isEnabled = cf.isValid(inter, true, false, sdacx, uics);
          xpa.setEnabled(isEnabled);
          if (xpt == XPType.CHECKBOX_ACTION) {
            XPlatToggleAction xpta = (XPlatToggleAction)xpa;
            if (cf instanceof ControlFlow.FlowForPopToggle) {
              xpta.setChecked(((ControlFlow.FlowForPopToggle)cf).shouldCheck(cSrc));
            }
          }
          break;           
        case SEPARATOR:
        case MENU_PLACEHOLDER:
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Define the editor menu bar
  */ 
     
  public XPlatMenuBar defineEditorMenuBar(DataAccessContext dacx) {
     
    if (viewerOnly_) {
      throw new IllegalStateException();
    }
      
    XPlatMenuBar menuBar = new XPlatMenuBar();
    ResourceManager rMan = dacx.getRMan();
    
    //
    // File Menu
    //
    XPlatMenu fMenu = new XPlatMenu(rMan.getString("command.File"), rMan.getChar("command.FileMnem"));
    menuBar.addMenu(fMenu);
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.NEW_TAB));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.NEW_MODEL));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.LOAD));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.LOAD_AS_NEW_TABS));
    fMenu.addItem(new XPlatPlaceholder("RECENT"));
    fMenu.addItem(new XPlatSeparator());
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SAVE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SAVE_AS));
    fMenu.addItem(new XPlatSeparator());
    
    // Import Submenu
    XPlatMenu importMenu = new XPlatMenu(rMan.getString("command.importMenu"), rMan.getChar("command.importMenuMnem"));
    fMenu.addItem(importMenu);
    
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_GENOME_FROM_SIF));
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_FULL_HIERARCHY_FROM_CSV));    
    importMenu.addItem(new XPlatSeparator());     
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_PERTURB_CSV_FROM_FILE));
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_PERTURBED_EXPRESSION_CSV));  
    importMenu.addItem(new XPlatSeparator());     
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_TIME_COURSE_XML));
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.IMPORT_TEMPORAL_INPUT_XML));
    importMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.COPIES_PER_EMBRYO_IMPORT));
    
    // Export Submenu
    XPlatMenu exportMenu = new XPlatMenu(rMan.getString("command.exportMenu"), rMan.getChar("command.exportMenuMnem"));
    fMenu.addItem(exportMenu);
    
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EXPORT));
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EXPORT_PUBLISH));    
    exportMenu.addItem(new XPlatSeparator());        
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.QPCR_WRITER));
    exportMenu.addItem(new XPlatSeparator());        
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EXPRESSION_TABLES_TO_CSV)); 
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PERTURBATION_TO_CSV)); 
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.BUILD_INSTRUCTIONS_TO_CSV)); 
    exportMenu.addItem(new XPlatSeparator());
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SBML_WRITER));
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GENOME_TO_SIF));    
    exportMenu.addItem(new XPlatSeparator());
    exportMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.WEB));
    
    fMenu.addItem(new XPlatSeparator());
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PRINT));  
    fMenu.addItem(new XPlatSeparator());    
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CLOSE));
    
    //
    // Edit Menu
    //
    
    XPlatMenu eMenu = new XPlatMenu(rMan.getString("command.Edit"), rMan.getChar("command.EditMnem"), "EMENU");
    menuBar.addMenu(eMenu);
    
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.UNDO, "UNDO", null, false));        
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REDO, "REDO", null, false));
    eMenu.addItem(new XPlatSeparator());
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_ALL));
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_NONE));
    
    XPlatMenu topOnlySelMenu = new XPlatMenu(rMan.getString("command.SelectTopOnly"), rMan.getChar("command.SelectTopOnlyMnem"));
    eMenu.addItem(topOnlySelMenu);
    
    topOnlySelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_ROOT_ONLY_NODES));
    topOnlySelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_ROOT_ONLY_LINKS));
   
    XPlatMenu upDownSelMenu = new XPlatMenu(rMan.getString("command.selectUpDown"), rMan.getChar("command.selectUpDownMnem"));
    eMenu.addItem(upDownSelMenu);
    
    upDownSelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_STEP_UPSTREAM));
    upDownSelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_STEP_DOWNSTREAM));

    //
    // Menu to remove specific classes of items:
    //
    
    XPlatMenu dropSelMenu = new XPlatMenu(rMan.getString("command.removeFromSelections"), rMan.getChar("command.removeFromSelectionsMnem"));
    eMenu.addItem(dropSelMenu);
    
    dropSelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DROP_LINK_SELECTIONS));
    dropSelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DROP_NODE_SELECTIONS));
    dropSelMenu.addItem(new XPlatSeparator());
    
    Vector<ChoiceContent> typeChoices = DBNode.getTypeChoices(rMan, true);
    int numC = typeChoices.size();
    for (int j = 0; j < numC; j++) {
      ChoiceContent cc = typeChoices.get(j);
      Selection.TypeArg args = new Selection.TypeArg(new Integer(cc.val), cc.name);
      dropSelMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REMOVE_SELECTIONS_FOR_NODE_TYPE, null, null, false, args));
    }
   
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECTIONS_TO_INACTIVE));
        
    eMenu.addItem(new XPlatPlaceholder("SELECTED"));
    eMenu.addItem(new XPlatPlaceholder("CURRENT_MODEL"));

    eMenu.addItem(new XPlatSeparator());
    XPlatMenu addMenu = new XPlatMenu(rMan.getString("command.addMenu"), rMan.getChar("command.addMenuMnem"));
    eMenu.addItem(addMenu);
    
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_GROUP_IN_INSTANCE)); 
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD));
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NODE));
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_LINK));
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NOTE));
    addMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_EXTRA_PROXY_NODE));    
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CANCEL_ADD_MODE));
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.MULTI_GROUP_COPY)); 
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PROPAGATE_DOWN));   
    eMenu.addItem(new XPlatToggleAction(flom_, rMan, FlowMeister.MainFlow.PULLDOWN, "PULLDOWN", null, false, "pullDownFam"));
    
    eMenu.addItem(new XPlatSeparator());    
 
    
    XPlatMenu overlayMenu = new XPlatMenu(rMan.getString("command.overlayMenu"), rMan.getChar("command.overlayMenuMnem"));
    eMenu.addItem(overlayMenu); 
    
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NETWORK_OVERLAY));
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EDIT_CURR_NETWORK_OVERLAY));
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REMOVE_CURR_NETWORK_OVERLAY));    
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_NETWORK_MODULE));
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK));
    overlayMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY));
 
    eMenu.addItem(new XPlatSeparator());    
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SET_FONT)); 
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EDIT_COLORS));
    eMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SET_DISPLAY_OPTIONS));
       
    //
    // View Menu
    //
    
    XPlatMenu vMenu = new XPlatMenu(rMan.getString("command.View"), rMan.getChar("command.ViewMnem"));
    menuBar.addMenu(vMenu);
    
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TOGGLE_BUBBLES));    
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_OUT));    
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_IN));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_SELECTED));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_PREVIOUS_SELECTED));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_NEXT_SELECTED));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_MODEL));    
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_MODELS));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_SHOW_WORKSPACE));
    vMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_NETWORK_MODULE)); 
    vMenu.addItem(new XPlatSeparator());
    
    XPlatMenu userPathsSubMenu = new XPlatMenu(rMan.getString("command.userPathsSubMenu"), rMan.getChar("command.userPathsSubMenuMnem"));
    vMenu.addItem(userPathsSubMenu);
    
    userPathsSubMenu.addItem(new XPlatPlaceholder("USER_PATH_CHOOSE"));
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_BACK));    
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_FORWARD));    
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_CREATE));    
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_DELETE));    
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_ADD_STOP));    
    userPathsSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_DELETE_STOP));

    //
    // Layout Menu
    //
    
    XPlatMenu loMenu = new XPlatMenu(rMan.getString("command.Layout"), rMan.getChar("command.LayoutMnem"));
    menuBar.addMenu(loMenu);
    
    XPlatMenu specLayoutMenu = new XPlatMenu(rMan.getString("command.specLayoutMenu"), rMan.getChar("command.specLayoutMenuMnem"));
    loMenu.addItem(specLayoutMenu);
    
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_STACKED));
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_DIAGONAL));
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_WORKSHEET));
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_HALO));  
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_SELECTED));
    specLayoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SPECIALTY_LAYOUT_PER_OVERLAY));
        
    loMenu.addItem(new XPlatSeparator());            
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RESIZE_WORKSPACE));    
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SQUASH_GENOME));   
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.EXPAND_GENOME));
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SYNC_ALL_LAYOUTS));
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.APPLY_KID_LAYOUTS_TO_ROOT));
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REPAIR_ALL_TOPOLOGY));
    
    XPlatMenu fixOrthoSubMenu = new XPlatMenu(rMan.getString("command.fixAllOrthoSubMenu"), rMan.getChar("command.fixAllOrthoSubMenuMnem"));
    loMenu.addItem(fixOrthoSubMenu);    
    fixOrthoSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REPAIR_ALL_NON_ORTHO_MIN_SHIFT));    
    fixOrthoSubMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.REPAIR_ALL_NON_ORTHO_MIN_SPLIT));    

    loMenu.addItem(new XPlatSeparator());
    XPlatMenu centeringMenu = new XPlatMenu(rMan.getString("command.centerMenu"), rMan.getChar("command.centerMenuMnem"));
    loMenu.addItem(centeringMenu);
    
    centeringMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_CURRENT_MODEL_ON_WORKSPACE)); 
    centeringMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SHIFT_ALL_MODELS_TO_WORKSPACE));     
    centeringMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ALIGN_ALL_LAYOUTS));        

    loMenu.addItem(new XPlatSeparator());
    XPlatMenu layoutMenu = new XPlatMenu(rMan.getString("command.layoutMenu"), rMan.getChar("command.layoutMenuMnem"));
    loMenu.addItem(layoutMenu);
    layoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RELAYOUT_ALL_LINKS)); 
    layoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RELAYOUT_DIAG_LINKS));
    layoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_NON_ORTHO_SEGS));     
    layoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RECOLOR_LAYOUT));      
    layoutMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.OPTIMIZE_LINKS));
        
    loMenu.addItem(new XPlatSeparator());    
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SET_AUTOLAYOUT_OPTIONS));

    //
    // Tools Menu
    //
    
    XPlatMenu sMenu = new XPlatMenu(rMan.getString("command.Tools"), rMan.getChar("command.ToolsMnem"));
    menuBar.addMenu(sMenu);
    
    sMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.BUILD_FROM_DIALOG));
    sMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DROP_ALL_INSTRUCTIONS));
    sMenu.addItem(new XPlatSeparator());
    sMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_QPCR_TO_ROOT_INSTANCES));
    sMenu.addItem(new XPlatSeparator());  
    sMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.NETWORK_SEARCH));  
    sMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GET_MODEL_COUNTS));
    
    
    sMenu.addItem(new XPlatSeparator());  
    XPlatMenu builderPluginMenu = new XPlatMenu(rMan.getString("command.modelBuilderPluginMenu"), rMan.getChar("command.modelBuilderMnem"));
    sMenu.addItem(builderPluginMenu);
    
    int builderIndex = 0;
    Iterator<ModelBuilderPlugIn> mbIterator = pluginManager_.getBuilderIterator();
    // Only one builder plugin at a time is supported
    if (mbIterator.hasNext()) {
      ModelBuilderPlugIn plugIn = mbIterator.next();
      String name = plugIn.getMenuName();
      XPlatMenu thisPluginMenu = new XPlatMenu(name, Integer.toString(builderIndex).charAt(0));

      BuilderPluginArg args = new BuilderPluginArg(name, builderIndex);
      
      XPlatAction launchBuilderItem = new XPlatAction(flom_, rMan, FlowMeister.MainFlow.LAUNCH_WORKSHEET, null, null, false, args);
      thisPluginMenu.addItem(launchBuilderItem);
      
      XPlatAction launchTrackerItem = new XPlatAction(flom_, rMan, FlowMeister.MainFlow.LAUNCH_LINK_DRAWING_TRACKER, null, null, false, args);
      thisPluginMenu.addItem(launchTrackerItem);
      
      builderPluginMenu.addItem(thisPluginMenu);
    }
    
    sMenu.addItem(new XPlatSeparator());
    XPlatMenu simulatorPluginMenu = new XPlatMenu(rMan.getString("command.simulatorPluginMenu"), rMan.getChar("command.simulatorMnem"));
    sMenu.addItem(simulatorPluginMenu);
    
    {
    	int engineIndex = 0;
    	Iterator<SimulatorPlugIn> simIterator = pluginManager_.getEngineIterator();
	    while (simIterator.hasNext()) {
	    	SimulatorPlugIn plugIn = simIterator.next();
	    	String name = plugIn.getMenuName();
	    	XPlatMenu thisPluginMenu = new XPlatMenu(name, Integer.toString(engineIndex).charAt(0));

	    	SimulationLaunch.SimulationPluginArg args = new SimulationLaunch.SimulationPluginArg(name, engineIndex);
	    	
	    	XPlatAction launchSimItem = new XPlatAction(flom_, rMan, FlowMeister.MainFlow.LAUNCH_SIM_PLUGIN, null, null, false, args);
	    	thisPluginMenu.addItem(launchSimItem);
	    	
	    	XPlatAction recoverSimItem = new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RECOVER_SIMULATION, null, null, false, args);
	    	thisPluginMenu.addItem(recoverSimItem);
	    	
	    	simulatorPluginMenu.addItem(thisPluginMenu);
	    	engineIndex += 1;
	    }
    }
    
    //
    // Data Menu
    //
    
    XPlatMenu dMenu = new XPlatMenu(rMan.getString("command.Data"), rMan.getChar("command.DataMnem"));
    menuBar.addMenu(dMenu);
  
    dMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GET_DATA_SHARING_STATE));
    dMenu.addItem(new XPlatSeparator());
    
    dMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CHANGE_MODEL_DATA));
    dMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DEFINE_TIME_AXIS));    

    XPlatMenu dMenu0 = new XPlatMenu(rMan.getString("command.DataPert"), rMan.getChar("command.DataQpcrMnem"));
    dMenu.addItem(dMenu0);   
    dMenu0.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PERTURB_MANAGE));
   
    XPlatMenu dMenu2 = new XPlatMenu(rMan.getString("command.DataTimeCourse"), rMan.getChar("command.DataTimeCourseMnem"));
    dMenu.addItem(dMenu2);
    dMenu2.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TIME_COURSE_TABLE_SETUP));
    dMenu2.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TIME_COURSE_REGION_HIERARCHY));    
    dMenu2.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TIME_COURSE_REGION_TOPOLOGY));
    
    dMenu2.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TIME_COURSE_TABLE_MANAGE));
       
    XPlatMenu dMenu3 = new XPlatMenu(rMan.getString("command.DataTemporalInputs"), rMan.getChar("command.DataTemporalInputsMnem"));
    dMenu.addItem(dMenu3);
    dMenu3.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TEMPORAL_INPUT_TABLE_MANAGE));
    dMenu3.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TEMPORAL_INPUT_DERIVE));
    dMenu3.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TEMPORAL_INPUT_DROP_ALL));
   
    //
    // Gaggle Menu
    //    
       
    if (doGaggle_) {
      XPlatMenu gMenu = new XPlatMenu(rMan.getString("command.Gaggle"), rMan.getChar("command.GaggleMnem"), null, "DO_GAGGLE");
      menuBar.addMenu(gMenu);
      
      gMenu.addItem(new XPlatPlaceholder("GOOSE_CHOOSE", "DO_GAGGLE"));
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_GOOSE_UPDATE, null, "DO_GAGGLE", false));
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_RAISE_GOOSE, null, "DO_GAGGLE", false));
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_LOWER_GOOSE, null, "DO_GAGGLE", false));
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_SEND_NETWORK, null, "DO_GAGGLE", false));
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_SEND_NAMELIST, null, "DO_GAGGLE", false));    
      gMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_PROCESS_INBOUND, null, "DO_GAGGLE", false));          
    }
      
    //
    // Help Menu
    //
    
    XPlatMenu hMenu = new XPlatMenu(rMan.getString("command.Help"), rMan.getChar("command.HelpMnem"));
    menuBar.addMenu(hMenu);

    hMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.HELP_POINTER));    
    hMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ABOUT));
   
    return (menuBar);
  }
  
  /***************************************************************************
  **
  ** Fill in if menubar items are active or inactive:
  */ 
  
  public void activateXPlatMenuBar(XPlatMenuBar xpMenuBar, BuildInfo bifo, MainCommands mcmd, 
                                   UIComponentSource uics, CmdSource cSrc) throws InvalidMenuRequestException {
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(MENU_BAR_KEY_);
    }
 
    Map<FlowMeister.FlowKey, Boolean> fes = mcmd.getFlowEnabledState();
    Iterator<XPlatMenu> ipt = xpMenuBar.getMenus();
    while (ipt.hasNext()) {
      XPlatMenu xpMenu = ipt.next();
      activateXPlatMenu(xpMenu, bifo, fes, uics, cSrc);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Fill in if menu items are active or inactive:
  */ 
  
  public void activateXPlatMenu(XPlatMenu xpMenu, BuildInfo bifo, Map<FlowMeister.FlowKey, Boolean> fes, 
                                UIComponentSource uics, CmdSource cSrc) {
  
    int itemCount = 0;
    Iterator<XPlatMenuItem> iit = xpMenu.getItems();
    while (iit.hasNext()) {
      XPlatMenuItem xpmi = iit.next();
      // Do not do items tagged for a false condition!
      String condition = xpmi.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not do items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }
      switch (xpmi.getType()) {
        case MENU:
          activateXPlatMenu((XPlatMenu)xpmi, bifo, fes, uics, cSrc);
          break;
        case ACTION:
        case CHECKBOX_ACTION:
          XPlatAbstractAction xpa = (XPlatAbstractAction)xpmi;
          FlowMeister.FlowKey key = xpa.getKey();
          // We assume that if it is not in the map, it is because it takes arguments, so force to true.
          Boolean isActive = fes.get(key);
          xpa.setEnabled((isActive != null) ? isActive.booleanValue() : true);
          if (xpmi.getType() == XPType.CHECKBOX_ACTION) {
            XPlatToggleAction xpta = (XPlatToggleAction)xpa;
            ControlFlow cf = flom_.getControlFlow(key, null);
            if (cf instanceof ControlFlow.FlowForPopToggle) {
              xpta.setChecked(((ControlFlow.FlowForPopToggle)cf).shouldCheck(cSrc));
            } else if (cf instanceof PathStop) {
              xpta.setChecked(uics.getPathController().getCurrentPathIndex() == itemCount);
            }            
          }
          break;           
        case SEPARATOR:
        case MENU_PLACEHOLDER:
          break;
        default:
          throw new IllegalArgumentException();
      }
      itemCount++;
    }
    return;
  }

  /***************************************************************************
  **
  ** Stock the tool bar
  */ 
  
  public XPlatToolBar defineEditorToolBar(DataAccessContext dacx) {
    
    if (viewerOnly_) {
      throw new IllegalStateException();
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatToolBar toolBar = new XPlatToolBar();
 
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SAVE, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PRINT, true));
    toolBar.addItem(new XPlatSeparator());  
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_OUT, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_IN, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_SELECTED, true)); 
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_PREVIOUS_SELECTED, true)); 
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED, true)); 
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_NEXT_SELECTED, true)); 
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_MODEL, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_MODELS, true));
    toolBar.addItem(new XPlatSeparator());  
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.NETWORK_SEARCH, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TOGGLE_BUBBLES, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_NONE, true));
    toolBar.addItem(new XPlatSeparator());  
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CANCEL_ADD_MODE, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_GENOME_INSTANCE, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_GROUP_IN_INSTANCE, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NODE, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_LINK, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NOTE, true));
    toolBar.addItem(new XPlatSeparator());  
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.PROPAGATE_DOWN, true));    
    toolBar.addItem(new XPlatToggleAction(flom_, rMan, FlowMeister.MainFlow.PULLDOWN, true, "pullDownFam"));
    toolBar.addItem(new XPlatSeparator());  

    toolBar.addItem(new XPlatPlaceholder("PATH_COMBO", "SHOW_PATH"));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_BACK, null, "SHOW_PATH", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_FORWARD, null, "SHOW_PATH", true));
    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_CREATE, true));    

    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_DELETE, null, "SHOW_PATH", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_ADD_STOP, null, "SHOW_PATH", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_DELETE_STOP, null, "SHOW_PATH", true));    
    
    toolBar.addItem(new XPlatSeparator());  
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ADD_NETWORK_OVERLAY, true));    

    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_NETWORK_MODULE, null, "SHOW_OVERLAY", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DRAW_NETWORK_MODULE_LINK, null, "SHOW_OVERLAY", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TOGGLE_MODULE_COMPONENT_DISPLAY, null, "SHOW_OVERLAY", true));
    
    if (doGaggle_) {
      toolBar.addItem(new XPlatSeparator("DO_GAGGLE"));  
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_GOOSE_UPDATE, "GOOSE_UP", "DO_GAGGLE", true));
      toolBar.addItem(new XPlatPlaceholder("GOOSE_COMBO", "DO_GAGGLE"));
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_RAISE_GOOSE, null, "DO_GAGGLE", true));
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_LOWER_GOOSE, null, "DO_GAGGLE", true));
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_SEND_NETWORK, null, "DO_GAGGLE", true));
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_SEND_NAMELIST, null, "DO_GAGGLE", true));      
    
      toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.GAGGLE_PROCESS_INBOUND, "GOOSE_PROC", "DO_GAGGLE", true));
    }
    return (toolBar);
  }

  /***************************************************************************
  **
  ** Stock the viewer tool bar
  */ 
  
  public XPlatToolBar defineViewerToolBar(DataAccessContext dacx) {
    
    if (!viewerOnly_) {
      throw new IllegalStateException();
    }
   
    ResourceManager rMan = dacx.getRMan();
    XPlatToolBar toolBar = new XPlatToolBar();

    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_OUT, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_IN, true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_SELECTED, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_PREVIOUS_SELECTED, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.CENTER_ON_NEXT_SELECTED, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_CURRENT_MODEL, true));    
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ZOOM_TO_ALL_MODELS, true));
    toolBar.addItem(new XPlatSeparator());
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.NETWORK_SEARCH, true));
    toolBar.addItem(new XPlatSeparator());
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.SELECT_NONE, true));
    toolBar.addItem(new XPlatSeparator());
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.ABOUT, true));

    toolBar.addItem(new XPlatSeparator("SHOW_PATH"));  
    toolBar.addItem(new XPlatPlaceholder("PATH_COMBO", "SHOW_PATH"));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_BACK, null, "SHOW_PATH", true));
    toolBar.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.TREE_PATH_FORWARD, null, "SHOW_PATH", true));
    return (toolBar);
  }
  
 
  /***************************************************************************
  **
  ** Define the editor tree popup menu
  */ 
     
  public XPlatMenu defineEditorTreePopup(DataAccessContext dacx) {

    if (viewerOnly_) {
      throw new IllegalStateException();
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu(rMan.getString("command.currentModelMenu"), rMan.getChar("command.currentModelMenuMnem"));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ADD_GENOME_INSTANCE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ADD_DYNAMIC_GENOME_INSTANCE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ADD_MODEL_TREE_GROUP_NODE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.EDIT_GROUP_NODE_NAV_MAP));
    
    XPlatMenu imageMenu = new XPlatMenu(rMan.getString("command.imageMenu"), rMan.getChar("command.imageMenuMnem"));
    fMenu.addItem(imageMenu);
    
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ASSIGN_IMAGE_TO_MODEL)); 
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.DROP_IMAGE_FOR_MODEL));
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ASSIGN_IMAGE_TO_GROUPING_NODE)); 
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.DROP_IMAGE_FOR_GROUPING_NODE));
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.ASSIGN_MAPPING_IMAGE_TO_GROUPING_NODE)); 
    imageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.DROP_MAPPING_IMAGE_FOR_GROUPING_NODE));

    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.COPY_GENOME_INSTANCE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.DELETE_GENOME_INSTANCE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.DELETE_GENOME_INSTANCE_KIDS_ONLY));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.EDIT_MODEL_PROPERTIES));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.MOVE_MODEL_NODEUP));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.MOVE_MODEL_NODE_DOWN));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.MAKE_STARTUP_VIEW));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.TreeFlow.SET_CURRENT_OVERLAY_FOR_FIRST_VIEW));
    return (fMenu);
  }
  
  
  
  public XPlatMenu defineEditorTabPopup(DataAccessContext dacx) {

    if (viewerOnly_) {
      throw new IllegalStateException();
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu(rMan.getString("command.currentTabMenu"), rMan.getChar("command.currentTabMenuMnem"));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DROP_THIS_TAB));    
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.DROP_ALL_BUT_THIS_TAB));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.MainFlow.RETITLE_TAB));
    
    return (fMenu);
  }
  
  
  
  
  /***************************************************************************
  **
  ** Define the overlay popup
  */ 
     
  public XPlatMenu defineOverlayPopup(DataAccessContext dacx) throws InvalidMenuRequestException {

    if (viewerOnly_) {
      throw new InvalidMenuRequestException(PopType.OVERLAY);
    }
      
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu();
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_CURRENT_OVERLAY, null, "EDITOR"));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_CURRENT_OVERLAY, null, "EDITOR"));
    return (fMenu);
  }
  
  /***************************************************************************
  **
  ** Define the note popup
  */ 
     
  public XPlatMenu defineNotePopup(DataAccessContext dacx) throws InvalidMenuRequestException {

    if (viewerOnly_) {
      throw new InvalidMenuRequestException(PopType.NOTE);
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu();
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_NOTE, null, "EDITOR"));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_NOTE, null, "EDITOR"));
    return (fMenu);
  }
  
  /***************************************************************************
  **
  ** Define the link point popup
  */ 
     
  public XPlatMenu defineLinkPointPopup(DataAccessContext dacx) throws InvalidMenuRequestException  {
    
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(PopType.LINK_POINT);
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu();
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_LINK_POINT, null, "EDITOR"));
    return (fMenu);
  }
  
 
  /***************************************************************************
  **
  ** Define the module link point popup
  */ 
     
  public XPlatMenu defineModuleLinkPointPopup(DataAccessContext dacx) throws InvalidMenuRequestException  {
    
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(PopType.MODULE_LINK_POINT);
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu fMenu = new XPlatMenu();
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_MODULE_LINK_POINT, null, "EDITOR"));
    return (fMenu);
  }
  
  /***************************************************************************
  **
  ** Define the region popup
  */ 
     
  public XPlatMenu defineRegionPopup(GenomeInstance gi, String groupID, DataAccessContext dacx) {
    XPlatMenu fMenu = new XPlatMenu();
    ResourceManager rMan = dacx.getRMan();
    EditGroupProperties.GroupArg ga = new EditGroupProperties.GroupArg(groupID, false);
    
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.TOGGLE));
    fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ZOOM_TO_GROUP));
    
    if (!viewerOnly_) {
    
      fMenu.addItem(new XPlatSeparator("EDITOR"));
      // non-null groupID indicates sub-group for Group Properties call:
      EditGroupProperties.GroupArg mga = new EditGroupProperties.GroupArg(null, true);
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.GROUP_PROPERTIES, null, "EDITOR", mga));
       
      // Pull everybody down
      fMenu.addItem(new XPlatSeparator("EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INCLUDE_ALL_FOR_GROUP, null, "EDITOR"));
     
      // Raise or Lower or Move
      fMenu.addItem(new XPlatSeparator("EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RAISE_GROUP, null, "EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.LOWER_GROUP, null, "EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_GROUP, null, "EDITOR"));
         
      // Compress/Expand
      fMenu.addItem(new XPlatSeparator("EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.LAYOUT_GROUP, null, "EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.COMPRESS_GROUP, null, "EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EXPAND_GROUP, null, "EDITOR"));
      
      // Copying
      fMenu.addItem(new XPlatSeparator("EDITOR"));
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.COPY_REGION, null, "EDITOR"));
        
      // Editing
     
      Group group = gi.getGroup(groupID);
      XPlatMenu sgm;
      
      if (gi.getVfgParent() == null) {
        sgm = defineSubGroupMenuPopup(gi, groupID, dacx);  
      } else { 
        sgm = defineSubSetSubGroupMenuPopup(gi, group, dacx);
      }       
        
      if (sgm != null) {
        fMenu.addItem(sgm);
      }
      fMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.GROUP_DELETE, null, "EDITOR"));
    
      // Data management
      fMenu.addItem(new XPlatSeparator("EDITOR"));
          
      XPlatMenu tcmMenu = new XPlatMenu(rMan.getString("groupPopup.tcManage"), rMan.getChar("groupPopup.tcManageMnem"), null, "EDITOR");
      fMenu.addItem(tcmMenu);
      
      tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.REGION_MAP, null, "EDITOR", ga));
      tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_REGION_MAP, null, "EDITOR", ga));
      
      XPlatMenu timMenu = new XPlatMenu(rMan.getString("groupPopup.tiManage"), rMan.getChar("groupPopup.tiManageMnem"), null, "EDITOR");
      fMenu.addItem(timMenu);
   
      timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INPUT_REGION_MAP, null, "EDITOR", ga));
      timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_INPUT_REGION_MAP, null, "EDITOR", ga));
    }
    return (fMenu);
  }
  
  /***************************************************************************
  **
  ** Generate a group submenu for subgroups
  */  

  private XPlatMenu defineSubGroupMenuPopup(GenomeInstance gi, String parentID, DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
  
    ResourceManager rMan = dacx.getRMan();
    
    XPlatMenu sgMenu = new XPlatMenu(rMan.getString("groupPopup.SubGroups"), rMan.getChar("groupPopup.SubGroupsMnem"), "SGMG", "EDITOR");
    sgMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.CREATE_SUB_GROUP, null, "EDITOR"));

    //
    // Figure out the subgroups for the given groups, and add them
    //
  
    List<String> subs = gi.getSubgroupsForGroup(parentID);    
    Iterator<String> sit = subs.iterator(); 
    if (sit.hasNext()) {
      sgMenu.addItem(new XPlatSeparator("EDITOR"));
    }
  
    while (sit.hasNext()) {
      String subID = sit.next();
      RemoveSubGroup.SubGroupArgs sga = new RemoveSubGroup.SubGroupArgs(subID, false);
      Group group = gi.getGroup(subID);
      String name = group.getName();
      sgMenu.addItem(generateSubGroupMenuCore(sga, name, dacx));
    }
    
    return (sgMenu);
  } 

  /***************************************************************************
  **
  ** Generate a group submenu for subgroups
  */  

  private XPlatMenu generateSubGroupMenuCore(RemoveSubGroup.SubGroupArgs sga, String name, DataAccessContext dacx) {
      
    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan();
    
    XPlatMenu sgMenu = new XPlatMenu(name, '\0', "sgMenu", "EDITOR");
       
    sgMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.GROUP_PROPERTIES, null, "EDITOR", new EditGroupProperties.GroupArg(sga.getGroupID(), false)));
    
    sgMenu.addItem(new XPlatSeparator("EDITOR"));
    
    sgMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_SUB_GROUP, null, "EDITOR", sga));
    sgMenu.addItem(new XPlatSeparator("EDITOR"));
       
    XPlatMenu tcManageMenu = new XPlatMenu(rMan.getString("groupPopup.tcManage"), rMan.getChar("groupPopup.tcManageMnem"), "tcManageMenu", "EDITOR");
    sgMenu.addItem(tcManageMenu);
    
    EditGroupProperties.GroupArg garg = new EditGroupProperties.GroupArg(sga.getGroupID(), false);
    
    tcManageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.REGION_MAP, null, "EDITOR", garg));
    tcManageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_REGION_MAP, null, "EDITOR", garg));
      
    XPlatMenu tiManageMenu = new XPlatMenu(rMan.getString("groupPopup.tiManage"), rMan.getChar("groupPopup.tiManageMnem"), "tiManageMenu", "EDITOR");
    sgMenu.addItem(tiManageMenu);
  
    tiManageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INPUT_REGION_MAP, null, "EDITOR", garg));
    tiManageMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_INPUT_REGION_MAP, null, "EDITOR", garg));
    
    return (sgMenu);
  }

  /***************************************************************************
  **
  ** Generate a group submenu for subgroups
  */  

  private XPlatMenu defineSubSetSubGroupMenuPopup(GenomeInstance gi, Group parent, DataAccessContext dacx) {
  
    if (viewerOnly_) {
      return (null);
    }
    
    //
    // If we have an activated subgroup, we disable activated entry and include entry,
    // and have one entry for the activated subgroup.  User can deactivate from there.
    // If we have an included subgroup, we disable activation entry.  User can manage
    // already included guys, including uninclude, from main menu entries.  Non-included
    // guys are accessible from included menu.
    //
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu sgMenu = new XPlatMenu(rMan.getString("groupPopup.SubGroups"), rMan.getChar("groupPopup.SubGroupsMnem"), null, "EDITOR");
    
    String active = parent.getActiveSubset();
    boolean haveActive = (active != null);
    MenuResult included = generateIncludeSubGroups(gi, parent, haveActive, dacx);
    boolean haveIncluded = false;
    XPlatMenu subMenuA = null;
    if (included != null) {
      haveIncluded = (included.included.size() != 0);
      subMenuA = generateActivationSubGroups(gi, parent, haveActive, haveIncluded, dacx);
      if (subMenuA != null) {
        sgMenu.addItem(subMenuA);
      }
      sgMenu.addItem(included.canInclude);
    }
    if (haveActive) {
      sgMenu.addItem(new XPlatSeparator("EDITOR"));
      String groupName = gi.getInheritedGroupName(active);
      RemoveSubGroup.SubGroupArgs sga = new RemoveSubGroup.SubGroupArgs(active, true); 
      XPlatMenu sgmc = generateSubGroupMenuCore(sga, groupName, dacx);
      if (sgmc != null) {
        sgMenu.addItem(sgmc);
      }
    } else if (included != null) {
      int intSize = included.included.size();
      if (haveIncluded) {
        sgMenu.addItem(new XPlatSeparator("EDITOR"));
      }
      for (int i = 0; i < intSize; i++) {
        sgMenu.addItem(included.included.get(i));
      }
    }
    sgMenu.setEnabled(haveActive || ((subMenuA != null) && subMenuA.getEnabled().booleanValue()) || haveIncluded || included.canInclude.getEnabled().booleanValue());
    return (sgMenu);
  }   

  /***************************************************************************
  **
  ** Generate a menu for subgroups that can be activated to override the enclosing group
  */  

  private XPlatMenu generateActivationSubGroups(GenomeInstance gi, Group enclosing, 
                                                boolean haveActive, boolean haveIncluded, DataAccessContext dacx) {
    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan(); 
   
    XPlatMenu asgm = new XPlatMenu(rMan.getString("groupPopup.ActivateSubgroup"), rMan.getChar("nodePopup.RemoveFromSubGroupMnem"), null, "EDITOR");
          
    if (haveActive || haveIncluded) {
      asgm.setEnabled(false);
      return (asgm);
    }
    asgm.setEnabled(true);
      
    //
    // We need to get the subgroups present in the parent that can
    // be activated.  We need to have the subgroup IDs from the parent.
    //
  
    String enclosingBase = Group.getBaseID(enclosing.getID());    
    GenomeInstance parentGI = gi.getVfgParent();
    int genCount = parentGI.getGeneration();        
    String parentEnclosing = Group.buildInheritedID(enclosingBase, genCount);
    List<String> subs = parentGI.getSubgroupsForGroup(parentEnclosing);    
  
    int count = 0;
    Iterator<String> sit = subs.iterator();
    while (sit.hasNext()) {
      String subID = sit.next();
      Group group = parentGI.getGroup(subID);
      AddSubGroup.GroupPairArgs gpa = new AddSubGroup.GroupPairArgs(enclosing.getID(), subID, parentGI.getInheritedGroupName(group.getID()));
      asgm.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ACTIVATE_SUB_GROUP, null, "EDITOR", gpa));
      count++;
    }
  
    asgm.setEnabled(count != 0);
    return (asgm);
  } 

  /***************************************************************************
  **
  ** Generate a menu for subgroups that can be included, and menus for groups
  ** already included
  */  

  private MenuResult generateIncludeSubGroups(GenomeInstance gi, Group enclosing, boolean haveActive, DataAccessContext dacx) {
     
    if (viewerOnly_) {
      return (null);
    }
    
    MenuResult retval = new MenuResult();             
    ResourceManager rMan = dacx.getRMan();     
    retval.canInclude = new XPlatMenu(rMan.getString("groupPopup.IncludeSubgroup"), rMan.getChar("groupPopup.IncludeSubgroupMnem"), null, "EDITOR");
    if (haveActive) {
      retval.canInclude.setEnabled(false);
      return (retval);
    }
    retval.canInclude.setEnabled(true);
  
    //
    // We need to get the subgroups present in the parent that can
    // be included, THAT ARE NOT ALREADY THERE.
    // We need to have the subgroup IDs from the parent.
    //
  
    String enclosingBase = Group.getBaseID(enclosing.getID());    
    GenomeInstance parentGI = gi.getVfgParent();
    int genCount = parentGI.getGeneration();        
    String parentEnclosing = Group.buildInheritedID(enclosingBase, genCount);
    List<String> subs = parentGI.getSubgroupsForGroup(parentEnclosing);
  
    int count = 0;
    Iterator<String> sit = subs.iterator();
    while (sit.hasNext()) {
      String subID = sit.next();
      String addGenID = Group.addGeneration(subID);
      Group subgroup = gi.getGroup(addGenID);
      if (subgroup == null) {
        Group group = parentGI.getGroup(subID);
        AddSubGroup.GroupPairArgs gpa = new AddSubGroup.GroupPairArgs(enclosing.getID(), subID, parentGI.getInheritedGroupName(group.getID()));
        retval.canInclude.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INCLUDE_SUB_GROUP, null, "EDITOR", gpa));            
      } else {
        RemoveSubGroup.SubGroupArgs sga = new RemoveSubGroup.SubGroupArgs(addGenID, false);
        String name = gi.getInheritedGroupName(addGenID);     
        retval.included.add(generateSubGroupMenuCore(sga, name, dacx));
      }
      count++;
    }
  
    retval.canInclude.setEnabled(count != 0);
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Build popup for links
  */

  public XPlatMenu defineLinkMenuPopup(boolean forSelMenu, DataAccessContext dacx) {
      
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu rMenu = (forSelMenu) ? new XPlatMenu(rMan.getString("command.selectedItemMenu"), rMan.getChar("command.selectedItemMenuMnem"), null, null) 
                                   : new XPlatMenu("DONT_CARE", 'D', null, null);
    
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DISPLAY_LINK_DATA, null, null));
    rMenu.addItem(new XPlatSeparator());
    
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FULL_SELECTION, null, null));
    rMenu.addItem(new XPlatSeparator());
    
    if (viewerOnly_) {
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ANALYZE_PATHS, null, "VIEWER"));
    } else {   
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ANALYZE_PATHS_WITH_QPCR, null, "EDITOR"));
      int builderIndex = 0;
      Iterator<ModelBuilderPlugIn> mbIterator = pluginManager_.getBuilderIterator();
      if (mbIterator.hasNext()) {
        ModelBuilderPlugIn plugIn = mbIterator.next();
        String name = plugIn.getMenuName();
        BuilderPluginArg args = new BuilderPluginArg(name, builderIndex);
        rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.LINK_WORKSHEET, null, "EDITOR", args));
      }
    }
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_LINK_SOURCE, null, null));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_LINK_TARGETS, null, null));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.LINK_USAGES, null, null)); 
    
    if (viewerOnly_) {
      return (rMenu);
    }
    rMenu.addItem(new XPlatSeparator("EDITOR"));
    
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.LINK_PROPERTIES, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SPECIAL_LINE, null, "EDITOR"));
    rMenu.addItem(new XPlatSeparator("EDITOR"));
        
    XPlatMenu ltom = defineLinkTopoOpsMenuPopup(dacx);
    if (ltom != null) {
      rMenu.addItem(ltom);
    }
    rMenu.addItem(new XPlatSeparator("EDITOR"));
    
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DIVIDE, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_SEGMENT, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_SOURCE_PAD, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_TARGET_PAD, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SWAP_PADS, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.CHANGE_TARGET_GENE_MODULE, null, "EDITOR"));   
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_LINKAGE, null, "EDITOR"));
    rMenu.addItem(new XPlatSeparator("EDITOR"));
 
    // Layout Ops
 
    XPlatMenu llom = defineLinkLayoutOpsMenuPopup(dacx);
    if (llom != null) {
      rMenu.addItem(llom);
    }
    return (rMenu);
  }
   
  /***************************************************************************
  **
  ** Build topo ops for links
  */

  private XPlatMenu defineLinkTopoOpsMenuPopup(DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan();      
    XPlatMenu toMenu = new XPlatMenu(rMan.getString("linkPopup.topoOps"), rMan.getChar("linkPopup.topoOpsMnem"), null, "EDITOR");     
    toMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INSERT_GENE_IN_LINK, null, "EDITOR"));
    toMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.INSERT_NODE_IN_LINK, null, "EDITOR"));
    toMenu.addItem(new XPlatSeparator("EDITOR"));
    toMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.CHANGE_SOURCE_NODE, null, "EDITOR"));    
    toMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.CHANGE_TARGET_NODE, null, "EDITOR"));  
    toMenu.addItem(new XPlatSeparator("EDITOR"));
    toMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MERGE_LINKS));
    return (toMenu);
 }
 
  /***************************************************************************
  **
  ** Build layout ops for links
  */

  private XPlatMenu defineLinkLayoutOpsMenuPopup(DataAccessContext dacx) {

    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan();
      
    XPlatMenu loMenu = new XPlatMenu(rMan.getString("linkPopup.layoutOps"), rMan.getChar("linkPopup.layoutOpsMnem"), null, "EDITOR");
      
    XPlatMenu fosMenu = new XPlatMenu(rMan.getString("command.fixOrthoSubMenu"), rMan.getChar("command.fixOrthoSubMenuMnem"), null, "EDITOR");   
    loMenu.addItem(fosMenu);        
    fosMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT, null, "EDITOR"));
    fosMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT, null, "EDITOR"));   
    
    XPlatMenu fosgsMenu = new XPlatMenu(rMan.getString("command.fixOrthoSegSubMenu"), rMan.getChar("command.fixOrthoSegSubMenuMnem"), null, "EDITOR");   
    loMenu.addItem(fosgsMenu);        
    fosgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_NON_ORTHO_MIN_SHIFT, null, "EDITOR"));
    fosgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_NON_ORTHO_MIN_SPLIT, null, "EDITOR"));
    
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SEG_LAYOUT, null, "EDITOR"));
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SEG_OPTIMIZE, null, "EDITOR"));
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.TOPO_REPAIR, null, "EDITOR"));    
   
   //  olaarp_ = poappState_s_.new OptimizeLinkAction(rMan.getString("linkPopup.SegOptimizeArp"), rMan.getChar("linkPopup.SegOptimizeMnemArp"), true);       
   // Currently optimizer does not allow reparenting when points are pinned!  Sorry!
   //  JMenu optoSubMenu = new JMenu(rMan.getString("command.LinkSegOptoSubMenu"));
   //  optoSubMenu.setMnemonic(rMan.getChar("command.LinkSegOptoSubMenuMnem"));
   //  layoutOpsMenu_.add(optoSubMenu);
    // optoSubMenu.add(ola_);
    // optoSubMenu.add(olaarp_);
     return (loMenu);
   }
  
  /***************************************************************************
  **
  ** Build popup for MODULE links
  */

  public XPlatMenu defineModLinkMenuPopup(DataAccessContext dacx) throws InvalidMenuRequestException {
  
    if (viewerOnly_) {
      throw new InvalidMenuRequestException(PopType.MODULE_LINK);
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu rMenu = new XPlatMenu("DONT_CARE", 'D', null, null);     
     
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MODULE_LINK_PROPERTIES, null, "EDITOR"));    
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DIVIDE_MOD, null, "EDITOR"));     
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_SEGMENT_MOD, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_SOURCE_PAD_MOD, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.RELOCATE_TARGET_PAD_MOD, null, "EDITOR"));
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_MODULE_LINKAGE, null, "EDITOR"));

    XPlatMenu mll = defineModLinkLayoutOpsMenuPopup(dacx);
    if (mll != null) {
      rMenu.addItem(mll);
    }
    return (rMenu);
  }
  
  /***************************************************************************
  **
  ** Build layout ops for MODULE links
  */

  private XPlatMenu defineModLinkLayoutOpsMenuPopup(DataAccessContext dacx) {

    if (viewerOnly_) {
      return (null);
    }
  
    ResourceManager rMan = dacx.getRMan();
           
    XPlatMenu loMenu = new XPlatMenu(rMan.getString("linkPopup.layoutOps"), rMan.getChar("linkPopup.layoutOpsMnem"), null, "EDITOR");
       
    XPlatMenu fosMenu = new XPlatMenu(rMan.getString("command.fixOrthoSubMenu"), rMan.getChar("command.fixOrthoSubMenuMnem"), null, "EDITOR");   
    loMenu.addItem(fosMenu);        
    fosMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT_MOD, null, "EDITOR"));
    fosMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT_MOD, null, "EDITOR"));   
     
    XPlatMenu fosgsMenu = new XPlatMenu(rMan.getString("command.fixOrthoSegSubMenu"), rMan.getChar("command.fixOrthoSegSubMenuMnem"), null, "EDITOR");   
    loMenu.addItem(fosgsMenu);        
    fosgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_NON_ORTHO_MIN_SHIFT_MOD, null, "EDITOR"));
    fosgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.FIX_NON_ORTHO_MIN_SPLIT_MOD, null, "EDITOR"));
     
    loMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.TOPO_REPAIR_MOD, null, "EDITOR"));    
 
    return (loMenu);
  }   
 
  /***************************************************************************
  **
  ** Build popup for node/gene
  */

  public XPlatMenu defineNodeMenuPopup(Genome genome, String nodeID, boolean doGene,
                                       String overlayKey, boolean forSelMenu, DataAccessContext dacx) {
  
    ResourceManager rMan = dacx.getRMan();    
    XPlatMenu rMenu = (forSelMenu) ? new XPlatMenu(rMan.getString("command.selectedItemMenu"), rMan.getChar("command.selectedItemMenuMnem"), null, null) 
                                   : new XPlatMenu("DONT_CARE", 'D', null, null);     
     
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DISPLAY_DATA, null, null));
    rMenu.addItem(new XPlatSeparator());
     
    XPlatMenu srssMenu = new XPlatMenu(rMan.getString("nodePopup.findSrcMenu"), rMan.getChar("nodePopup.findSrcMenuMnem"), null, null);
    rMenu.addItem(srssMenu);
        
    srssMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_SOURCES_GENE_ONLY, null, null));
    srssMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_SOURCES, null, null));
     
    XPlatMenu trgsMenu = new XPlatMenu(rMan.getString("nodePopup.findTrgMenu"), rMan.getChar("nodePopup.findTrgMenuMnem"), null, null);
    rMenu.addItem(trgsMenu);
    
    trgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_TARGETS_GENE_ONLY, null, null));
    trgsMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SELECT_TARGETS, null, null));
  
    rMenu.addItem(new XPlatToggleAction(flom_, rMan, FlowMeister.PopFlow.SELECT_LINKS_TOGGLE, "sltoga_", null, "selectionFam"));
    rMenu.addItem(new XPlatToggleAction(flom_, rMan, FlowMeister.PopFlow.SELECT_QUERY_NODE_TOGGLE, "sqntoga_", null, "selectionFam"));
    rMenu.addItem(new XPlatToggleAction(flom_, rMan, FlowMeister.PopFlow.APPEND_TO_CURRENT_SELECTION_TOGGLE, "sa2ctoga_", null, "selectionFam"));
     
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.NODE_USAGES, null, null));
    rMenu.addItem(new XPlatSeparator());
     
    rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ANALYZE_PATHS_FROM_USER_SELECTED, null, null));
    rMenu.addItem(sourceGeneration(genome, nodeID, dacx));
    
    XPlatMenu qsg = qpcrSourceGeneration(genome, nodeID, dacx);
    if (qsg != null) {
      rMenu.addItem(qsg);
    }
    if (!viewerOnly_) {
      rMenu.addItem(new XPlatSeparator("EDITOR"));
       
      FlowMeister.PopFlow whichKey = (doGene) ? FlowMeister.PopFlow.GENE_PROPERTIES : FlowMeister.PopFlow.NODE_PROPERTIES;
      rMenu.addItem(new XPlatAction(flom_, rMan, whichKey, null, "EDITOR"));     
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SIMULATION_PROPERTIES, null, "EDITOR"));
      rMenu.addItem(new XPlatSeparator("EDITOR"));
       
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.NODE_SUPER_ADD, null, "EDITOR"));
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.NODE_TYPE_CHANGE, null, "EDITOR"));
      
      if (!doGene) {
        rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MERGE_NODES));
      } else {   
        XPlatMenu cregMenu = new XPlatMenu(rMan.getString("genePopup.cisRegMenu"), rMan.getChar("genePopup.cisRegMenuMnem"), null, null);
        rMenu.addItem(cregMenu);
        cregMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DEFINE_CIS_REG_MODULE));  
        cregMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_CIS_REG_MODULE));
      }
      
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.CHANGE_NODE_GROUP_MEMBERSHIP, null, "EDITOR"));
    }
    
    // Add or delete from subregions:
    
    XPlatMenu sgg = subGroupGeneration(genome, nodeID, dacx);
    if (sgg != null) {
      rMenu.addItem(sgg);
    }
    
    if (!viewerOnly_) {
      rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_NODE, null, "EDITOR"));
      rMenu.addItem(new XPlatSeparator("EDITOR"));
    }
    
    XPlatMenu[] nmgm = netModuleGeneration(genome, nodeID, overlayKey, dacx);
    if (nmgm != null) {
      rMenu.addItem(nmgm[0]);
      rMenu.addItem(nmgm[1]);   
      rMenu.addItem(new XPlatSeparator("EDITOR"));
    }

    XPlatMenu[] dmxpm = defineDataManagementPopup(dacx); 
    if (dmxpm != null) {    
      rMenu.addItem(dmxpm[0]);
      rMenu.addItem(dmxpm[1]);
      rMenu.addItem(dmxpm[2]);
    }
    
    return (rMenu);
  }    

  /***************************************************************************
  **
  ** Build data management for nodes
  */

  private XPlatMenu[] defineDataManagementPopup(DataAccessContext dacx) {
    
    //
    // Nothing to do for Viewer:
    //
    
    if (viewerOnly_) {
      return (null);
    }
    
    XPlatMenu[] retval = new XPlatMenu[3];
    
    ResourceManager rMan = dacx.getRMan();
      
    XPlatMenu pmMenu = new XPlatMenu(rMan.getString("genePopup.pertManage"), rMan.getChar("genePopup.pertManageMnem"), null, "EDITOR");
    retval[0] = pmMenu;
        
    pmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_PERT, null, "EDITOR"));
    pmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_PERT, null, "EDITOR"));
    
    pmMenu.addItem(new XPlatSeparator("EDITOR"));
    
    pmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_PERT_MAP, null, "EDITOR"));
    pmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_PERT_MAP, null, "EDITOR"));
  
    //
    // Time course management submenu
    //
    
    XPlatMenu tcmMenu = new XPlatMenu(rMan.getString("genePopup.timeCourseManage"), rMan.getChar("genePopup.timeCourseManageMnem"), null, "EDITOR");
    retval[1] = tcmMenu;
   
    tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_TIME_COURSE, null, "EDITOR"));
    tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_TIME_COURSE, null, "EDITOR"));
    tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_PERTURBED_TIME_COURSE, null, "EDITOR"));
    
    tcmMenu.addItem(new XPlatSeparator("EDITOR"));
    
    tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_TIME_COURSE_MAP, null, "EDITOR"));
    tcmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_TIME_COURSE_MAP, null, "EDITOR"));
    
    //
    // Temporal input management submenu
    //
     
    XPlatMenu timMenu = new XPlatMenu(rMan.getString("genePopup.TemporalInputManage"), rMan.getChar("genePopup.TemporalInputManageMnem"), null, "EDITOR");
    retval[2] = timMenu;
   
    timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_TEMPORAL_INPUT, null, "EDITOR"));
    timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_TEMPORAL_INPUT, null, "EDITOR"));
    
    timMenu.addItem(new XPlatSeparator("EDITOR"));
    
    timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_TEMPORAL_INPUT_MAP, null, "EDITOR"));
    timMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_TEMPORAL_INPUT_MAP, null, "EDITOR"));
    
    return (retval);   
  }


  /***************************************************************************
  **
  ** Generate a menu for listing link sources for a given node
  */  

  private XPlatMenu sourceGeneration(Genome genome, String targNodeID, DataAccessContext dacx) {
      
    ResourceManager rMan = dacx.getRMan(); 
    
    XPlatMenu parpaMenu = new XPlatMenu(rMan.getString("nodePopup.sourceGenes"), rMan.getChar("nodePopup.sourceGenesMnem"), "sourceMenu_", null);
       
    Set<String> sources = genome.getNodeSources(targNodeID);
    if (sources.size() == 0) {
      parpaMenu.setEnabled(false);
      return (parpaMenu);
    }

    Iterator<String> sit = sources.iterator();
    while (sit.hasNext()) {
      String sid = sit.next();
      Node sourceNode = genome.getNode(sid);
      String nodeName = sourceNode.getName();
      if ((nodeName == null) || nodeName.trim().equals("")) {
        nodeName = rMan.getString("tip.noname");
      }
      DisplayPaths.PathAnalysisArgs paa = new DisplayPaths.PathAnalysisArgs(targNodeID, sourceNode.getID(), nodeName);
      parpaMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ANALYZE_PATHS_FOR_NODE, null, null, paa));
    }
    parpaMenu.setEnabled(true);
    return (parpaMenu);
  }
  
  /***************************************************************************
  **
  ** Generate a menu for listing perturbation sources for a given node
  */  

  private XPlatMenu qpcrSourceGeneration(Genome genome, String targNodeID, DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan();
    FlowMeister flom = flom_;
    
    XPlatMenu pertSrcMenu = new XPlatMenu(rMan.getString("nodePopup.qpcrSourceGenes"), rMan.getChar("nodePopup.qpcrSourceGenesMnem"), null, "EDITOR");
    if (genome instanceof GenomeInstance) {
      pertSrcMenu.setEnabled(false);
      return (pertSrcMenu);
    }
  
    PerturbationData pd = dacx.getExpDataSrc().getPertData();
    PerturbationDataMaps pdms = dacx.getDataMapSrc().getPerturbationDataMaps();

    Set<String> sources = pd.getPerturbationSources(targNodeID, pdms);
    if (sources.size() == 0) {
      pertSrcMenu.setEnabled(false);
      return (pertSrcMenu);
    }

    Iterator<String> sit = sources.iterator();
    while (sit.hasNext()) {
      String sid = sit.next();
      Node sourceNode = genome.getNode(sid);
      String nodeName = sourceNode.getName();
      if ((nodeName == null) || nodeName.trim().equals("")) {
        nodeName = rMan.getString("tip.noname");
      }
      DisplayPaths.PathAnalysisArgs paa = new DisplayPaths.PathAnalysisArgs(targNodeID, sourceNode.getID(), nodeName);
      pertSrcMenu.addItem(new XPlatAction(flom, rMan, FlowMeister.PopFlow.PERTURB_PATH_COMPARE, null, "EDITOR", paa));
    }
    pertSrcMenu.setEnabled(true);
    return (pertSrcMenu);
  }

  /***************************************************************************
  **
  ** Generate a menu for listing possible net modules for a given node
  */  
  
  private XPlatMenu[] netModuleGeneration(Genome genome, String nodeID, String overlayKey, DataAccessContext dacx) {
    
    //
    // Nothing to do for Viewer:
    //
    
    if (viewerOnly_) {
      return (null);
    }

    NetworkOverlay netOvr = null;
    if (overlayKey != null) {
      NetOverlayOwner owner = dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genome.getID());
      netOvr = owner.getNetworkOverlay(overlayKey);
    }
     
    XPlatMenu[] retval = new XPlatMenu[2];
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu anmMenu = new XPlatMenu(rMan.getString("nodePopup.addToNetModule"), rMan.getChar("nodePopup.addToNetModuleMnem"), null, "EDITOR");
    retval[0] = anmMenu;
   
    XPlatMenu dnmMenu = new XPlatMenu(rMan.getString("nodePopup.deleteFromNetModule"), rMan.getChar("nodePopup.deleteFromNetModuleMnem"), null, "EDITOR");
    retval[1] = dnmMenu;
    
    if (netOvr == null) {
      anmMenu.setEnabled(false);
      dnmMenu.setEnabled(false);
      return (retval);     
    }
        
    boolean inAModule = false;
    boolean notInAModule = false;
    Iterator<NetModule> moit = netOvr.getModuleIterator();
    while (moit.hasNext()) {
      NetModule mod = moit.next();
      boolean canUseMod = true;
      String groupID = mod.getGroupAttachment();
      if (groupID != null) {
        GenomeInstance gi = (GenomeInstance)genome;
        Group group = gi.getGroup(groupID); 
        if (!group.isInGroup(nodeID, gi)) {
          canUseMod = false;
        }
      }
      if (mod.isAMember(nodeID)) {
        inAModule = true;
        AddNodeToModule.NamedModuleArgs nma = new AddNodeToModule.NamedModuleArgs(mod.getName(), mod.getID());
        dnmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_FROM_MODULE, null, "EDITOR", nma));
      } else if (canUseMod) {
        notInAModule = true;      
        AddNodeToModule.NamedModuleArgs nma = new AddNodeToModule.NamedModuleArgs(mod.getName(), mod.getID());
        anmMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ADD_TO_MODULE, null, "EDITOR", nma));
      }
    }
  
    anmMenu.setEnabled(notInAModule);
    dnmMenu.setEnabled(inAModule);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Generate a menu for listing subgroups for a given node
  */  

  private XPlatMenu subGroupGeneration(Genome genome, String nodeID, DataAccessContext dacx) {
    
    //
    // Nothing to do for Viewer:
    //
    
    if (viewerOnly_) {
      return (null);
    }
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu sgMenu = new XPlatMenu(rMan.getString("nodePopup.SubGroups"), rMan.getChar("nodePopup.SubGroupsMnem"), null, "EDITOR");
        
    boolean haveInstance = genome instanceof GenomeInstance;
    if (haveInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      if (gi.getVfgParentRoot() == null) {       
        NodeInstance ni = (NodeInstance)gi.getNode(nodeID);
        XPlatMenu addMenu = generateSubGroupsForAdd(gi, ni, dacx);
        if (addMenu != null) {
          sgMenu.addItem(addMenu);
        }
        XPlatMenu delMenu = generateSubGroupsForDelete(gi, ni, dacx);
        if (delMenu != null) {
          sgMenu.addItem(delMenu);
        }
        sgMenu.setEnabled(addMenu.getEnabled().booleanValue() || delMenu.getEnabled().booleanValue());
      } else {
        sgMenu.setEnabled(false);
      }
    } else {
      sgMenu.setEnabled(false);
    }
    return (sgMenu);
  }
  
  /***************************************************************************
  **
  ** Generate a menu for subgroups we can add
  */  

  private XPlatMenu generateSubGroupsForAdd(GenomeInstance gi, NodeInstance ni, DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
    
    //
    // Figure out the subgroups in the genome instance, and the group
    // membership for the node:
    //
  
    GroupMembership memb = gi.getNodeGroupMembership(ni);
    if (memb.mainGroups.size() != 1) {
      throw new IllegalStateException();
    }
    String parent = memb.mainGroups.iterator().next(); 
    List<String> subs = gi.getSubgroupsForGroup(parent);
  
    //
    // Only take those subgroups we do not belong to:
    //
    
    ResourceManager rMan = dacx.getRMan();
    XPlatMenu sgaMenu = new XPlatMenu(rMan.getString("nodePopup.AddToSubGroup"), rMan.getChar("nodePopup.AddToSubGroupMnem"), null, "EDITOR");
    
    int count = 0;
    Iterator<String> sit = subs.iterator();
    while (sit.hasNext()) {
      String subID = sit.next();
      if (!memb.subGroups.contains(subID)) {
        Group group = gi.getGroup(subID);
        AddNodeToSubGroup.NodeInGroupArgs paa = new AddNodeToSubGroup.NodeInGroupArgs(ni.getID(), subID, group.getName());
        sgaMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ADD_NODE_TO_SUB_GROUP, null, "EDITOR", paa));
        count++;
      }     
    }
    
    sgaMenu.setEnabled(count != 0);  
    return (sgaMenu);
  }  

  /***************************************************************************
  **
  ** Generate a menu for subgroups we can be deleted from
  */  

  private XPlatMenu generateSubGroupsForDelete(GenomeInstance gi, NodeInstance ni, DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
     
    //
    // Figure out the subgroups in the genome instance, and the group
    // membership for the node:
    //
  
    GroupMembership memb = gi.getNodeGroupMembership(ni);
    if (memb.mainGroups.size() != 1) {
      throw new IllegalStateException();
    }
  
    //
    // Only take those subgroups we do belong to:
    //
    
    ResourceManager rMan = dacx.getRMan(); 
    XPlatMenu sgdMenu = new XPlatMenu(rMan.getString("nodePopup.RemoveFromSubGroup"), rMan.getChar("nodePopup.RemoveFromSubGroupMnem"), null, "EDITOR");
 
    int count = 0;
    Iterator<String> sit = memb.subGroups.iterator();
    while (sit.hasNext()) {
      String subID = sit.next();
      Group group = gi.getGroup(subID);
      AddNodeToSubGroup.NodeInGroupArgs paa = new AddNodeToSubGroup.NodeInGroupArgs(ni.getID(), subID, group.getName());
      sgdMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DELETE_NODE_FROM_SUB_GROUP, null, "EDITOR", paa));
      count++;
    }
    sgdMenu.setEnabled(count != 0);  
    return (sgdMenu);
  }

  /***************************************************************************
  **
  ** Build popup for node/gene
  */

  public XPlatMenu defineNetModulePopup(Layout layout, String overlayKey, String moduleID, 
                                        NetModuleFree.IntersectionExtraInfo ei, DataAccessContext dacx) {
   
     ResourceManager rMan = dacx.getRMan();
     XPlatMenu rMenu = new XPlatMenu("DONT_CARE", 'D', null, null);
             
     rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ZOOM_TO_NET_MODULE, null, null));
     rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.TOGGLE_NET_MODULE_CONTENT_DISPLAY, null, null));
     rMenu.addItem(new XPlatSeparator());
     
     rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SET_AS_SINGLE_CURRENT_NET_MODULE, null, null));
     rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DROP_FROM_CURRENT_NET_MODULES, null, null));
     
     if (!viewerOnly_) {
    	 
         ModuleValidity mv = calcModuleValidity(layout, overlayKey, moduleID, ei);
         
         rMenu.addItem(new XPlatSeparator("EDITOR"));
       
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_SELECTED_NETWORK_MODULE, null, "EDITOR"));
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.ADD_REGION_TO_NET_MODULE, null, "EDITOR"));
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DETACH_MODULE_FROM_GROUP, null, "EDITOR"));
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.REMOVE_THIS_NETWORK_MODULE, null, "EDITOR"));
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.EDIT_MODULE_MEMBERS, null, "EDITOR"));
       rMenu.addItem(new XPlatSeparator("EDITOR"));
          
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.SIZE_CORE_TO_REGION_BOUNDS, "sctrb_", "EDITOR"));
           
       RemoveModuleOrPart.HardwiredEnabledArgs hea = new RemoveModuleOrPart.HardwiredEnabledArgs(mv.validDropNetModuleRegion);
       XPlatAction xpa = new XPlatAction(flom_, rMan, FlowMeister.PopFlow.DROP_NET_MODULE_REGION, "dnmr_", "EDITOR", hea);
       xpa.setEnabled(mv.validDropNetModuleRegion); // Belt and suspenders; handled by HardwiredEnabledArgs
       rMenu.addItem(xpa);
       
       Mover.MoveNetModuleRegionArgs args = new Mover.MoveNetModuleRegionArgs(true, NetModuleProperties.ALL_MODULE_SHAPES, 
                                                                              "modulePopup.fastMove", "modulePopup.fastMoveMnem", true);
       rMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, "simpleMove_", "EDITOR", args));
       
       XPlatMenu moveMenu = new XPlatMenu(rMan.getString("modulePopup.Move"), rMan.getChar("modulePopup.MoveMnem"), "moveMenu", "EDITOR");     
       rMenu.addItem(moveMenu);
   
       XPlatMenu[] mmpos = defineModuleMovePopups(mv, dacx);
       if (mmpos != null) {
         moveMenu.addItem(mmpos[0]);
         moveMenu.addItem(mmpos[1]);
       }
     }
     
     return (rMenu);
   }    
  
  
  /***************************************************************************
  **
  ** Build module move menus
  */

  private XPlatMenu[] defineModuleMovePopups(ModuleValidity mv, DataAccessContext dacx) {
    
    if (viewerOnly_) {
      return (null);
    }
    
    XPlatMenu[] retval = new XPlatMenu[2];
     
    ResourceManager rMan = dacx.getRMan();
      
    XPlatMenu wcMenu = new XPlatMenu(rMan.getString("modulePopup.MoveWithContents"), rMan.getChar("modulePopup.MoveWithContentsMnem"), null, "EDITOR");
    retval[0] = wcMenu;
    wcMenu.setEnabled(mv.moveWithContentsMenuEnabled);
    
    NameInstaller ni = mv.oneShapeWith;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(true, NetModuleProperties.ONE_MODULE_SHAPE, ni.name, ni.mnem, ni.isValid);
      wcMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
    
    ni = mv.contigShapeWith;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(true, NetModuleProperties.CONTIG_MODULE_SHAPES, ni.name, ni.mnem, ni.isValid);
      wcMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
    
    ni = mv.allShapeWith;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(true, NetModuleProperties.ALL_MODULE_SHAPES, ni.name, ni.mnem, ni.isValid);
      wcMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
          
    XPlatMenu wocMenu = new XPlatMenu(rMan.getString("modulePopup.MoveNoContents"), rMan.getChar("modulePopup.MoveNoContentsMnem"), null, "EDITOR");
    retval[1] = wocMenu; 
    wocMenu.setEnabled(mv.moveNoContentsMenuEnabled);
    
    ni = mv.oneShapeNone;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(false, NetModuleProperties.ONE_MODULE_SHAPE, ni.name, ni.mnem, ni.isValid);
      wocMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
    
    ni = mv.contigShapeNone;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(false, NetModuleProperties.CONTIG_MODULE_SHAPES, ni.name, ni.mnem, ni.isValid);
      wocMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
    
    ni = mv.allShapeNone;
    if (ni != null) {
      Mover.MoveNetModuleRegionArgs mnmr = 
        new Mover.MoveNetModuleRegionArgs(false, NetModuleProperties.ALL_MODULE_SHAPES, ni.name, ni.mnem, ni.isValid);
      wocMenu.addItem(new XPlatAction(flom_, rMan, FlowMeister.PopFlow.MOVE_NET_MODULE_REGION, null, "EDITOR", mnmr));
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Show module popup menu
  */
  
  private final static int UNDEFINED_BOUND_       = 0;
  private final static int NON_MEMBER_BOUND_      = 1;
  private final static int AUTO_MEMBER_BOUND_     = 2;
  private final static int DEFINED_SHAPE_BOUND_   = 3;
  private final static int CONTIG_EXTENDED_BOUND_ = 4;
  private final static int OPAQUE_INTERIOR_POINT_ = 5;
  private final static int LABEL_BOUND_           = 6;   

  private static class ModuleValidity {
    int displayType;
    boolean validDropNetModuleRegion;
    boolean moveWithContentsMenuEnabled;
    boolean moveNoContentsMenuEnabled;
    
    NameInstaller oneShapeWith;
    NameInstaller contigShapeWith;
    NameInstaller allShapeWith;
    
    NameInstaller oneShapeNone;
    NameInstaller contigShapeNone;
    NameInstaller allShapeNone;
    
    ModuleValidity() {
    }
  }
 
  ModuleValidity calcModuleValidity(Layout layout, String overlayKey, String moduleID, NetModuleFree.IntersectionExtraInfo ei) { 
    ModuleValidity mv = new ModuleValidity();
    NetOverlayProperties nop = layout.getNetOverlayProperties(overlayKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);

    //
    // We have an intersection.  There are six basic intersection types.  Figure out what we are dealing with.
    // Note that some intersections may overlap to multiple types, but we are dumping them into one
    // category.  FIX ME?
    //
    //     Label shape      
    //     Non-member shape
    //     Auto-member shape
    //     Underlying shape
    //     Contig module auto bound-only shape
    //     Interior point in opaque mode
    
    int intersectType = UNDEFINED_BOUND_;
    boolean labelIsContig = false;
    
    if (ei == null) {
      intersectType = LABEL_BOUND_;         
    } else if ((ei.directShape != null) && (ei.directShape.shapeClass == NetModuleFree.TaggedShape.NON_MEMBER_SHAPE)) {
      intersectType = NON_MEMBER_BOUND_;
    } else if (ei.type == NetModuleFree.IntersectionExtraInfo.IS_INTERNAL) {
      intersectType = OPAQUE_INTERIOR_POINT_;
    } else if (nmp.haveExtraDeltaRect(ei)) {
      intersectType = CONTIG_EXTENDED_BOUND_;
    } else if (!nmp.rectsForDirect(ei).isEmpty()) {
      intersectType = DEFINED_SHAPE_BOUND_;
    } else if ((ei.directShape != null) && (ei.directShape.shapeClass == NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE)) {
      intersectType = AUTO_MEMBER_BOUND_;
    } else if (ei.gotNameBounds) {
      labelIsContig = ((ei.contigInfo != null) && !ei.contigInfo.memberShapes.isEmpty() || !ei.contigInfo.intersectedShape.isEmpty());
      intersectType = LABEL_BOUND_;  
    }
    
    //
    // Get display type
    //
    
    mv.displayType = nmp.getType();
    
    //
    // Can only delete if we are of the correct display type
    // and we have hit an underlying rectangle
    //
    boolean canDelete;
    if (mv.displayType == NetModuleProperties.CONTIG_RECT) {
      canDelete = false;
    } else if (mv.displayType == NetModuleProperties.MEMBERS_ONLY) {
      canDelete = false;
    } else {
      canDelete = !nmp.hasOneShape() && (intersectType == DEFINED_SHAPE_BOUND_);
    } 
    //
    // Makes sense with members only, with multi-rect if we hit underlying rectangles
    // and not a non-member, with contig if we hit the underlying rectangle:
    //
    boolean withMemsOneShape = (((mv.displayType == NetModuleProperties.MEMBERS_ONLY) && (intersectType != LABEL_BOUND_)) ||
                                ((mv.displayType == NetModuleProperties.MULTI_RECT) && (intersectType != NON_MEMBER_BOUND_) && (intersectType != LABEL_BOUND_)) ||
                                ((mv.displayType == NetModuleProperties.CONTIG_RECT) && (intersectType == DEFINED_SHAPE_BOUND_)));    
    //
    // Can move contig region with contents if we are multi-rect, as long as we are not
    // intersecting a non-member bound (may not be connected to the outside so we can figure it out),
    //

    boolean withMemsConShape = false;
    if (mv.displayType == NetModuleProperties.MULTI_RECT) {
      withMemsConShape = (intersectType != NON_MEMBER_BOUND_) && ((intersectType != LABEL_BOUND_) || labelIsContig);
    }
    
    //
    // This always is allowed:
    //
    
    boolean withMemsAllShape = true;
    
    //
    // Can't move modules without contents if we are opaque or if module
    // is members only:
    //    
    boolean noMemsCommon = (intersectType != OPAQUE_INTERIOR_POINT_) &&
                           (intersectType != OPAQUE_INTERIOR_POINT_) && 
                           (mv.displayType != NetModuleProperties.MEMBERS_ONLY);   
    //
    // To move just a single shape, with no contents, we have to intersect
    // an actual rect:
    //
    boolean noMemsOneShape = noMemsCommon && (intersectType == DEFINED_SHAPE_BOUND_);         
    //
    // The only time it make sense to move a contiguous boundary with no contents is
    // the multi-rect type, but not if we are intersecting a non-member boundary or
    // even a member boundary:
    //
    boolean noMemsConShape = noMemsCommon;
    if (mv.displayType == NetModuleProperties.CONTIG_RECT) {
      noMemsConShape = false;
    } else if (mv.displayType == NetModuleProperties.MULTI_RECT) {
      noMemsConShape = noMemsConShape && (intersectType != NON_MEMBER_BOUND_) && 
                                         (intersectType != LABEL_BOUND_) &&
                                         (intersectType != AUTO_MEMBER_BOUND_);
    }  
    //
    // Can't really move all shapes with no members in a contig rect type, since
    // boundaries are set by the members.  User can select one shape option instead:
    //
    
    boolean noMemsAllShape = noMemsCommon;
    if (mv.displayType == NetModuleProperties.CONTIG_RECT) {
      noMemsAllShape = false;
    }
    
    mv.validDropNetModuleRegion = canDelete;
    
    //
    // Build the move menus:
    //
    
    boolean showWithContents = withMemsOneShape || withMemsConShape || withMemsAllShape;
    mv.moveWithContentsMenuEnabled = showWithContents;
     
    boolean showNoContents = noMemsOneShape || noMemsConShape || noMemsAllShape;
    mv.moveNoContentsMenuEnabled = showNoContents;     
   
    if (showWithContents) {
      String oneBlockNameKey;
      String contigNameKey;
      String allRegNameKey;
      String oneBlockNameKeyMnem;
      String contigNameKeyMnem;
      String allRegNameKeyMnem;
      
      switch (mv.displayType) {
        case NetModuleProperties.CONTIG_RECT:
          oneBlockNameKey = "modulePopup.MoveOneBlockWCforCRType";
          contigNameKey = "DONT_CARE";
          allRegNameKey = "modulePopup.MoveAllBlocksWCforCRType";
          oneBlockNameKeyMnem = "modulePopup.MoveOneBlockWCforCRTypeMnem";
          contigNameKeyMnem = "DONT_CARE";
          allRegNameKeyMnem = "modulePopup.MoveAllBlocksWCforCRTypeMnem";
          break;
        case NetModuleProperties.MULTI_RECT:
          oneBlockNameKey = "modulePopup.MoveOneBlockWCforMRType";
          contigNameKey = "modulePopup.MoveContigBlocksWCforMRType";            
          allRegNameKey = "modulePopup.MoveAllBlocksWCforMRType";
          oneBlockNameKeyMnem = "modulePopup.MoveOneBlockWCforMRTypeMnem";
          contigNameKeyMnem = "modulePopup.MoveContigBlocksWCforMRTypeMnem";            
          allRegNameKeyMnem = "modulePopup.MoveAllBlocksWCforMRTypeMnem";
          break;
        case NetModuleProperties.MEMBERS_ONLY:
          oneBlockNameKey = "modulePopup.MoveOneBlockWCforMOType";
          contigNameKey = "DONT_CARE";            
          allRegNameKey = "modulePopup.MoveAllBlocksWCforMOType";
          oneBlockNameKeyMnem = "modulePopup.MoveOneBlockWCforMOTypeMnem";
          contigNameKeyMnem = "DONT_CARE";       
          allRegNameKeyMnem = "modulePopup.MoveOneBlockWCforMOTypeMnem";
          break;
        default:
          throw new IllegalStateException();
      }
      mv.oneShapeWith = new NameInstaller(oneBlockNameKey, oneBlockNameKeyMnem, withMemsOneShape, true, false);
      if (mv.displayType == NetModuleProperties.MULTI_RECT) {
        mv.contigShapeWith = new NameInstaller(contigNameKey, contigNameKeyMnem, withMemsConShape, true, false);
      }    
      mv.allShapeWith = new NameInstaller(allRegNameKey, allRegNameKeyMnem, withMemsAllShape, true, false);
    }
    
    if (showNoContents) {
      String oneBlockNameKey;
      String contigNameKey;
      String allRegNameKey;
      String oneBlockNameKeyMnem;
      String contigNameKeyMnem;
      String allRegNameKeyMnem;
      switch (mv.displayType) {
        case NetModuleProperties.CONTIG_RECT:
          oneBlockNameKey = "modulePopup.MoveOneBlockNCforCRType";
          contigNameKey = "DONT_CARE";
          allRegNameKey = "DONT_CARE";
          oneBlockNameKeyMnem = "modulePopup.MoveOneBlockNCforCRTypeMnem";
          contigNameKeyMnem = "DONT_CARE";
          allRegNameKeyMnem = "DONT_CARE";
          break;
        case NetModuleProperties.MULTI_RECT:
          oneBlockNameKey = "modulePopup.MoveOneBlockNCforMRType";
          contigNameKey = "modulePopup.MoveContigBlocksNCforMRType";            
          allRegNameKey = "modulePopup.MoveAllBlocksNCforMRType";
          oneBlockNameKeyMnem = "modulePopup.MoveOneBlockNCforMRTypeMnem";
          contigNameKeyMnem = "modulePopup.MoveContigBlocksNCforMRTypeMnem";            
          allRegNameKeyMnem= "modulePopup.MoveAllBlocksNCforMRTypeMnem";
          break;
        case NetModuleProperties.MEMBERS_ONLY:
          oneBlockNameKey = "DONT_CARE";
          contigNameKey = "DONT_CARE";            
          allRegNameKey = "DONT_CARE";
          oneBlockNameKeyMnem = "DONT_CARE";
          contigNameKeyMnem = "DONT_CARE";
          allRegNameKeyMnem = "DONT_CARE";
          break;
        default:
          throw new IllegalStateException();
      }
 
      if (mv.displayType != NetModuleProperties.MEMBERS_ONLY) {
        mv.oneShapeNone = new NameInstaller(oneBlockNameKey, oneBlockNameKeyMnem, noMemsOneShape, false, true);    
        if (mv.displayType == NetModuleProperties.MULTI_RECT) {
          mv.contigShapeNone = new NameInstaller(contigNameKey, contigNameKeyMnem, noMemsConShape, false, true);
          mv.allShapeNone = new NameInstaller(allRegNameKey, allRegNameKeyMnem, noMemsAllShape, false, true);
        }
      }
    }
    return (mv);
  }

  /***************************************************************************
  **
  ** Fill control flow map. Note that to be keyed by flow key in a map, this cannot be used
  ** on Control flows that take custom arguments!
  */ 
  
  public Map<FlowMeister.FlowKey, ControlFlow> fillFlowMap(XPlatMenu xpMenu, MenuSource.BuildInfo bifo) {     
    HashMap<FlowMeister.FlowKey, ControlFlow> retval = new HashMap<FlowMeister.FlowKey, ControlFlow>();
    
    Iterator<XPlatMenuItem> iit = xpMenu.getItems();
    while (iit.hasNext()) {
      XPlatMenuItem xpmi = iit.next();
      String condition = xpmi.getCondition();
      if (condition != null) {
        Boolean doit = bifo.conditions.get(condition);
        // Do not build items tagged for a false condition!
        if ((doit != null) && !doit.booleanValue()) {
          continue;
        }
      }      
      XPlatMenuItem.XPType theType = xpmi.getType();
      if (theType.equals(XPType.MENU)) {
        retval.putAll(fillFlowMap((XPlatMenu)xpmi, bifo));
        continue;
      } else if (!(theType.equals(XPType.ACTION) || theType.equals(XPType.CHECKBOX_ACTION))) {
        continue;
      }
      XPlatAbstractAction xpaa = (XPlatAbstractAction)xpmi;
      ControlFlow.OptArgs aarg = xpaa.getActionArg();
      if (aarg != null) {
        throw new IllegalStateException();
      }
      retval.put(xpaa.getKey(), flom_.getControlFlow(xpaa.getKey(), aarg));
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  public static class BuildInfo {
    public Map<String, JMenu> collected;
    public Map<String, AbstractAction> taggedActions;
    public Set<AbstractAction> allActions;
    public Map<String, Boolean> conditions;
    public Map<String, JComponent> components;
    public Map<String, Integer> compPosition;
    
    public BuildInfo() {
      collected = new HashMap<String, JMenu>();
      taggedActions = new HashMap<String, AbstractAction>();
      allActions = new HashSet<AbstractAction>();
      conditions = new HashMap<String, Boolean>();
      components = new HashMap<String, JComponent>();     
      compPosition = new HashMap<String, Integer>();     
    }  
  }    

  /***************************************************************************
  **
  ** Reports what is allowed for EDITOR versus VIEWER modes
  */
  
  public static class SupportedMenus {
    public Set<String> menuTypes;
    public Set<PopType> popupTypes;
    public boolean keyStrokes;
    
    public SupportedMenus(Set<String> menuTypes, Set<PopType> popupTypes, boolean keyStrokes) {
      this.menuTypes = menuTypes;
      this.popupTypes = popupTypes;
      this.keyStrokes = keyStrokes;
    }  
  }    
  
  /***************************************************************************
  **
  ** Used for complex module pop-up moves
  */
 
  public static class NameInstaller {
    String name;
    String mnem;
    boolean isValid;
    public boolean addToContentsMenu;
    public boolean addToNoContentsMenu;
      
    NameInstaller(String name, String mnem, boolean isValid, boolean addToContentsMenu, boolean addToNoContentsMenu) {
      this.name = name;
      this.mnem = mnem;
      this.isValid = isValid;
      this.addToContentsMenu = addToContentsMenu;
      this.addToNoContentsMenu = addToNoContentsMenu;
    }
  }
  
  /***************************************************************************
  **
  ** Better not ask for unsupported menu types
  */
  
  public class InvalidMenuRequestException extends Exception {
  
    public String badMenu;
    public PopType badPopup;
    public boolean badKeyStroke;
    
    
    public InvalidMenuRequestException(String badMenu) {
      this.badMenu = badMenu;    
    }  
  
    public InvalidMenuRequestException(PopType badPopup) {
      this.badPopup = badPopup;    
    }  
    
    public InvalidMenuRequestException(boolean badKeyStroke) {
      this.badKeyStroke = badKeyStroke;    
    }
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  
  /***************************************************************************
  **
  ** Used to return menu results
  */
  
  private static class MenuResult {    
    XPlatMenu canInclude;
    ArrayList<XPlatMenu> included;
    
    MenuResult() {
      included = new ArrayList<XPlatMenu>();
    }
  }
}
