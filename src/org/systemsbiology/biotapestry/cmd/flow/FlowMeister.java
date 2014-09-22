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


package org.systemsbiology.biotapestry.cmd.flow;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;

import org.systemsbiology.biotapestry.cmd.flow.add.AddExtraProxyNode;
import org.systemsbiology.biotapestry.cmd.flow.add.AddLink;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNetModule;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNetworkOverlay;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNode;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNodeToModule;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNodeToSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.add.AddNote;
import org.systemsbiology.biotapestry.cmd.flow.add.AddSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.add.CancelAdd;
import org.systemsbiology.biotapestry.cmd.flow.add.DrawGroup;
import org.systemsbiology.biotapestry.cmd.flow.add.IncludeAllForGroup;
import org.systemsbiology.biotapestry.cmd.flow.add.InsertNodeInLink;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateDown;
import org.systemsbiology.biotapestry.cmd.flow.add.PullDown;
import org.systemsbiology.biotapestry.cmd.flow.add.SuperAdd;
import org.systemsbiology.biotapestry.cmd.flow.control.CloseApp;
import org.systemsbiology.biotapestry.cmd.flow.display.DisplayData;
import org.systemsbiology.biotapestry.cmd.flow.display.DisplayPaths;
import org.systemsbiology.biotapestry.cmd.flow.display.DisplayPathsUserSource;
import org.systemsbiology.biotapestry.cmd.flow.display.PathGenerator;
import org.systemsbiology.biotapestry.cmd.flow.edit.ChangeNodeGroup;
import org.systemsbiology.biotapestry.cmd.flow.edit.ChangeNodeType;
import org.systemsbiology.biotapestry.cmd.flow.edit.DupGroups;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditGroupProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditLinkProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditModelData;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNetModule;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNetModuleLink;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNetModuleMembers;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNetOverlay;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNodeProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditNote;
import org.systemsbiology.biotapestry.cmd.flow.edit.EditSimProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.MultiSelectionProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.ResizeModuleCore;
import org.systemsbiology.biotapestry.cmd.flow.editData.PerturbData;
import org.systemsbiology.biotapestry.cmd.flow.editData.TemporalInput;
import org.systemsbiology.biotapestry.cmd.flow.editData.TimeCourse;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportBuildInstr;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportExpression;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportGenomeToSIF;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportPerturb;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportPerturbCSV;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportPublish;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportSBML;
import org.systemsbiology.biotapestry.cmd.flow.gaggle.GaggleOps;
import org.systemsbiology.biotapestry.cmd.flow.image.ModelImageOps;
import org.systemsbiology.biotapestry.cmd.flow.info.ShowInfo;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportModel;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportWeb;
import org.systemsbiology.biotapestry.cmd.flow.io.ImportCSV;
import org.systemsbiology.biotapestry.cmd.flow.io.ImportSIF;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveOps;
import org.systemsbiology.biotapestry.cmd.flow.io.Print;
import org.systemsbiology.biotapestry.cmd.flow.layout.Align;
import org.systemsbiology.biotapestry.cmd.flow.layout.CenterCurrent;
import org.systemsbiology.biotapestry.cmd.flow.layout.DownwardSync;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutNonOrthoLink;
import org.systemsbiology.biotapestry.cmd.flow.layout.LinkRepair;
import org.systemsbiology.biotapestry.cmd.flow.layout.OptimizeLink;
import org.systemsbiology.biotapestry.cmd.flow.layout.Recolor;
import org.systemsbiology.biotapestry.cmd.flow.layout.RelayoutLinks;
import org.systemsbiology.biotapestry.cmd.flow.layout.ReorderGroups;
import org.systemsbiology.biotapestry.cmd.flow.layout.SpecialLineProps;
import org.systemsbiology.biotapestry.cmd.flow.layout.SpecialtyLayoutFlow;
import org.systemsbiology.biotapestry.cmd.flow.layout.SquashExpandGenome;
import org.systemsbiology.biotapestry.cmd.flow.layout.UpwardSync;
import org.systemsbiology.biotapestry.cmd.flow.layout.WorkspaceOps;
import org.systemsbiology.biotapestry.cmd.flow.link.ChangeNode;
import org.systemsbiology.biotapestry.cmd.flow.link.ChangePad;
import org.systemsbiology.biotapestry.cmd.flow.link.Divide;
import org.systemsbiology.biotapestry.cmd.flow.link.RelocateSeg;
import org.systemsbiology.biotapestry.cmd.flow.link.SwapPads;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.AddDynamicGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.AddGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.CopyGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.DeleteGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.EditModelProps;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.MoveModelNode;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SettingOps;
import org.systemsbiology.biotapestry.cmd.flow.move.Mover;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.DialogBuild;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.SimpleBuilds;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveBuildInstruct;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveGroup;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveLinkPoint;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveLinkage;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveModuleLinkOrPoint;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveModuleOrPart;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNode;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNodeFromMod;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNodeFromSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNote;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveOverlay;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSelections;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveSubGroup;
import org.systemsbiology.biotapestry.cmd.flow.search.NetworkSearch;
import org.systemsbiology.biotapestry.cmd.flow.search.NodeSrcTargSearch;
import org.systemsbiology.biotapestry.cmd.flow.search.SearchAStep;
import org.systemsbiology.biotapestry.cmd.flow.select.Selection;
import org.systemsbiology.biotapestry.cmd.flow.settings.DoSettings;
import org.systemsbiology.biotapestry.cmd.flow.undo.UndoRedoOps;
import org.systemsbiology.biotapestry.cmd.flow.userPath.PathManage;
import org.systemsbiology.biotapestry.cmd.flow.userPath.PathStep;
import org.systemsbiology.biotapestry.cmd.flow.userPath.PathStop;
import org.systemsbiology.biotapestry.cmd.flow.view.Modules;
import org.systemsbiology.biotapestry.cmd.flow.view.Toggle;
import org.systemsbiology.biotapestry.cmd.flow.view.Zoom;

import org.systemsbiology.biotapestry.ui.layouts.HaloLayout;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayout;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayout;


/****************************************************************************
**
** Generator for primary commands for the application
*/

public class FlowMeister {
 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC ENUMS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Common Enum Interface
  //  
  
  public interface FlowKey {
    public enum FlowType {
      MAIN, MODEL_TREE, POP, KEY_BOUND, OTHER;
    }
    public FlowType getFlowType();
  }
  
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Main menu/toolbar action keys:
  // 
    
  public enum MainFlow implements FlowKey { 
    CLOSE, 
    EXPORT,
    QPCR_WRITER,
    SBML_WRITER,
    WEB,
    SAVE,
    SAVE_AS,
    LOAD,
    NEW_MODEL,
    PRINT,
    SELECT_ALL,
    ADD,
    ADD_LINK,   
    PROPAGATE_DOWN,
    ADD_NODE,
    PULLDOWN,
    TOGGLE_BUBBLES,
    SET_FONT,
    UNDO,
    REDO,   
    ZOOM_OUT,
    ZOOM_IN, 
    HELP_POINTER,
    ABOUT,
    SPECIALTY_LAYOUT_SELECTED,
    ZOOM_TO_CURRENT_MODEL,
    SPECIALTY_LAYOUT_STACKED,
    IMPORT_TIME_COURSE_XML,
    IMPORT_TEMPORAL_INPUT_XML,
    EXPRESSION_TABLES_TO_CSV,
    PERTURBATION_TO_CSV,
    TIME_COURSE_TABLE_SETUP,
    ZOOM_TO_CURRENT_SELECTED,
    TIME_COURSE_TABLE_MANAGE,
    TEMPORAL_INPUT_TABLE_MANAGE,
    CHANGE_MODEL_DATA,
    ADD_NOTE,
    IMPORT_PERTURBED_EXPRESSION_CSV,
    IMPORT_PERTURB_CSV_FROM_FILE,
    EDIT_COLORS,
    ADD_QPCR_TO_ROOT_INSTANCES,
    IMPORT_GENOME_FROM_SIF,
    NETWORK_SEARCH,
    ADD_EXTRA_PROXY_NODE,
    CANCEL_ADD_MODE,
    BUILD_FROM_DIALOG,
    GENOME_TO_SIF,
    SQUASH_GENOME,
    EXPAND_GENOME,
    RELAYOUT_ALL_LINKS,
    SET_AUTOLAYOUT_OPTIONS,
    RELAYOUT_DIAG_LINKS,
    OPTIMIZE_LINKS,
    PROP_ROOT_WITH_EXP_DATA,
    IMPORT_FULL_HIERARCHY_FROM_CSV,
    SYNC_ALL_LAYOUTS,
    ALIGN_ALL_LAYOUTS,
    SET_DISPLAY_OPTIONS,
    DROP_ALL_INSTRUCTIONS,
    APPLY_KID_LAYOUTS_TO_ROOT,
    GET_MODEL_COUNTS,
    COPIES_PER_EMBRYO_IMPORT,
    EXPORT_PUBLISH,
    DROP_NODE_SELECTIONS,
    DROP_LINK_SELECTIONS,
    TIME_COURSE_REGION_HIERARCHY,
    SPECIALTY_LAYOUT_HALO,
    SPECIALTY_LAYOUT_WORKSHEET,
    RECOLOR_LAYOUT,       
    DEFINE_TIME_AXIS,
    SELECT_NON_ORTHO_SEGS,
    ZOOM_TO_ALL_SELECTED,
    ZOOM_TO_ALL_MODELS,
    CENTER_CURRENT_MODEL_ON_WORKSPACE,
    RESIZE_WORKSPACE,
    ZOOM_TO_SHOW_WORKSPACE,
    SHIFT_ALL_MODELS_TO_WORKSPACE,
    SELECT_NONE,
    ASSIGN_IMAGE_TO_MODEL,
    DROP_IMAGE_FOR_MODEL,
    TREE_PATH_BACK, 
    TREE_PATH_FORWARD,
    TREE_PATH_CREATE,
    TREE_PATH_DELETE,
    TREE_PATH_ADD_STOP,
    TREE_PATH_DELETE_STOP,
    GAGGLE_GOOSE_UPDATE,
    GAGGLE_RAISE_GOOSE,
    GAGGLE_LOWER_GOOSE,
    GAGGLE_SEND_NETWORK,
    GAGGLE_SEND_NAMELIST,    
    GAGGLE_PROCESS_INBOUND,
    DRAW_GROUP_IN_INSTANCE,
    ADD_NETWORK_OVERLAY,
    DRAW_NETWORK_MODULE,
    REMOVE_CURR_NETWORK_OVERLAY,
    EDIT_CURR_NETWORK_OVERLAY,
    ZOOM_TO_CURRENT_NETWORK_MODULE,
    MULTI_GROUP_COPY,
    DRAW_NETWORK_MODULE_LINK,
    TOGGLE_MODULE_COMPONENT_DISPLAY,
    ADD_GENOME_INSTANCE,
    PERTURB_MANAGE,
    SPECIALTY_LAYOUT_PER_OVERLAY,
    BUILD_INSTRUCTIONS_TO_CSV,
    REPAIR_ALL_NON_ORTHO_MIN_SHIFT,
    REPAIR_ALL_NON_ORTHO_MIN_SPLIT,
    SPECIALTY_LAYOUT_DIAGONAL,
    CENTER_ON_NEXT_SELECTED,
    CENTER_ON_PREVIOUS_SELECTED,
    REPAIR_ALL_TOPOLOGY,
    REMOVE_SELECTIONS_FOR_NODE_TYPE,
    LOAD_RECENT,
    CLEAR_RECENT,
    MODEL_EXPORT,
    TREE_PATH_SET_CURRENT_USER_PATH,
    SET_CURRENT_GAGGLE_TARGET,
    SELECT_STEP_UPSTREAM,
    SELECT_STEP_DOWNSTREAM,
   ;
    
   public FlowType getFlowType() {
     return (FlowType.MAIN);
   }
   
   public String toString() {
     return (FlowType.MAIN.toString() + "_" + super.toString());    
   }
 
  }
  
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Model tree action keys:
  // 
  
  public enum TreeFlow implements FlowKey {
    ADD_GENOME_INSTANCE, 
    ADD_DYNAMIC_GENOME_INSTANCE, 
    DELETE_GENOME_INSTANCE, 
    DELETE_GENOME_INSTANCE_KIDS_ONLY, 
    EDIT_MODEL_PROPERTIES, 
    COPY_GENOME_INSTANCE, 
    MOVE_MODEL_NODEUP,
    MOVE_MODEL_NODE_DOWN,
    MAKE_STARTUP_VIEW, 
    SET_CURRENT_OVERLAY_FOR_FIRST_VIEW,
    ;
    
    public FlowType getFlowType() {
      return (FlowType.MODEL_TREE);
    }
    
    public String toString() {
      return (FlowType.MODEL_TREE.toString() + "_" + super.toString());    
    }
    
  };
 
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Popup Menu action keys:
  //   
  
  
  public enum PopFlow implements FlowKey {
    EDIT_CURRENT_OVERLAY, 
    DELETE_CURRENT_OVERLAY,
    EDIT_NOTE,
    DELETE_NOTE, 
    DELETE_LINK_POINT,
    TOGGLE,
    ZOOM_TO_GROUP,
    GROUP_PROPERTIES,
    INCLUDE_ALL_FOR_GROUP,
    RAISE_GROUP,
    LOWER_GROUP,
    MOVE_GROUP, 
    LAYOUT_GROUP,
    COMPRESS_GROUP,
    EXPAND_GROUP,   
    COPY_REGION, 
    GROUP_DELETE,
    REGION_MAP,
    DELETE_REGION_MAP,
    INPUT_REGION_MAP,
    DELETE_INPUT_REGION_MAP,
    CREATE_SUB_GROUP,
    LINK_PROPERTIES,
    SPECIAL_LINE,
    INSERT_GENE_IN_LINK,
    INSERT_NODE_IN_LINK,
    CHANGE_SOURCE_NODE,
    CHANGE_TARGET_NODE,
    DIVIDE,
    RELOCATE_SEGMENT,
    RELOCATE_SOURCE_PAD,
    RELOCATE_TARGET_PAD,
    DIVIDE_MOD,
    RELOCATE_SEGMENT_MOD,
    RELOCATE_SOURCE_PAD_MOD,
    RELOCATE_TARGET_PAD_MOD,
    SWAP_PADS,
    DELETE_LINKAGE,
    FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT,
    FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT_MOD,   
    FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT,
    FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT_MOD,  
    FIX_NON_ORTHO_MIN_SHIFT,
    FIX_NON_ORTHO_MIN_SHIFT_MOD,
    FIX_NON_ORTHO_MIN_SPLIT,
    FIX_NON_ORTHO_MIN_SPLIT_MOD,
    TOPO_REPAIR,
    TOPO_REPAIR_MOD,
    SEG_LAYOUT,
    SEG_OPTIMIZE,
    MODULE_LINK_PROPERTIES,
    DELETE_MODULE_LINKAGE,
    DISPLAY_LINK_DATA,
    FULL_SELECTION,
    ANALYZE_PATHS,
    ANALYZE_PATHS_WITH_QPCR,
    ANALYZE_PATHS_FROM_USER_SELECTED,
    SELECT_LINK_SOURCE,
    SELECT_LINK_TARGETS,
    LINK_USAGES,
    SELECT_SOURCES,
    SELECT_SOURCES_GENE_ONLY,
    SELECT_TARGETS,
    SELECT_TARGETS_GENE_ONLY,
    SELECT_LINKS_TOGGLE,
    SELECT_QUERY_NODE_TOGGLE,
    APPEND_TO_CURRENT_SELECTION_TOGGLE,
    NODE_USAGES,
    SIMULATION_PROPERTIES,
    NODE_SUPER_ADD,
    NODE_TYPE_CHANGE,
    CHANGE_NODE_GROUP_MEMBERSHIP,
    DELETE_NODE,
    EDIT_PERT,
    DELETE_PERT,
    EDIT_PERT_MAP,
    DELETE_PERT_MAP,
    EDIT_TIME_COURSE,
    DELETE_TIME_COURSE,
    EDIT_PERTURBED_TIME_COURSE,
    EDIT_TIME_COURSE_MAP,
    DELETE_TIME_COURSE_MAP,
    EDIT_TEMPORAL_INPUT,
    DELETE_TEMPORAL_INPUT,
    EDIT_TEMPORAL_INPUT_MAP,
    DELETE_TEMPORAL_INPUT_MAP,
    DISPLAY_DATA,
    GENE_PROPERTIES,
    NODE_PROPERTIES,
    EDIT_MULTI_SELECTIONS, 
    ZOOM_TO_NET_MODULE,
    TOGGLE_NET_MODULE_CONTENT_DISPLAY,
    SET_AS_SINGLE_CURRENT_NET_MODULE,
    DROP_FROM_CURRENT_NET_MODULES,
    EDIT_SELECTED_NETWORK_MODULE,
    ADD_REGION_TO_NET_MODULE,
    DETACH_MODULE_FROM_GROUP,
    REMOVE_THIS_NETWORK_MODULE,
    EDIT_MODULE_MEMBERS,
    SIZE_CORE_TO_REGION_BOUNDS,
    DROP_NET_MODULE_REGION,
    MOVE_NET_MODULE_REGION,
    DELETE_MODULE_LINK_POINT,
    DELETE_FROM_MODULE,
    ADD_TO_MODULE,
    ANALYZE_PATHS_FOR_NODE,
    PERTURB_PATH_COMPARE,   
    DELETE_SUB_GROUP,
    ADD_NODE_TO_SUB_GROUP,
    DELETE_NODE_FROM_SUB_GROUP,
    INCLUDE_SUB_GROUP,   
    ACTIVATE_SUB_GROUP,
    ;
    
    public FlowType getFlowType() {
      return (FlowType.POP);
    }
    
    public String toString() {
      return (FlowType.POP.toString() + "_" + super.toString());    
    }
    
  };
  
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Key bound action keys:
  // 
  
  public enum KeyFlowKey implements FlowKey {
    MULTI_DELETE,
    CANCEL_MODE, 
    NUDGE_UP,  
    NUDGE_DOWN,   
    NUDGE_LEFT,
    NUDGE_RIGHT,
    ;
    
    public FlowType getFlowType() {
      return (FlowType.KEY_BOUND);
    }
    
    public String toString() {
      return (FlowType.KEY_BOUND.toString() + "_" + super.toString());    
    }    
  };
  
  ////////////////////////////////////////////////////////////////////////////   
  //
  // Misc (triggered by direct, non-modal mouse clicks on desktop)
  // 
  
  public enum OtherFlowKey implements FlowKey {
    REMOTE_BATCH_SELECTION,
    LOCAL_CLICK_SELECTION,
    LOCAL_RECT_SELECTION,
    MODEL_SELECTION,
    MODEL_SELECTION_FROM_TREE, 
    MODEL_SELECTION_FROM_SLIDER,
    MOVE_ELEMENTS,
    MODEL_AND_NODE_SELECTION, 
    MODEL_AND_LINK_SELECTION,
    MODEL_AND_NODE_LINK_SELECTION,
    PATH_MODEL_GENERATION,
    TARDY_LINK_DATA,
    TARDY_NODE_DATA,
    ;
    
    public FlowType getFlowType() {
      return (FlowType.OTHER);
    }
    
    public String toString() {
      return (FlowType.OTHER.toString() + "_" + super.toString());    
    }    
  };
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
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
  
  public FlowMeister(BTState appState) {
    appState_ = appState;
  }  
  
  
   ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get Control Flow key from strings
  */ 
  
  public FlowKey mapToFlowKey(String cmdClass, String keyVal) {
    if ((cmdClass == null) || (keyVal == null)) {
      return (null);
    }
    try {
      FlowKey.FlowType ft = FlowKey.FlowType.valueOf(cmdClass);
      switch (ft) {
        case MAIN:
          return (MainFlow.valueOf(keyVal));
        case MODEL_TREE:
          return (TreeFlow.valueOf(keyVal));
        case POP:
          return (PopFlow.valueOf(keyVal));
        case KEY_BOUND:
          return (KeyFlowKey.valueOf(keyVal));
        case OTHER:
          return (OtherFlowKey.valueOf(keyVal));
        default:
          return (null);
      }
    } catch (IllegalArgumentException iaex) {
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** Get Control Flow Args FIX ME!!
  */ 
  
  public OptArgWithStatus buildCFArgs(FlowKey flowKey, Map<String, String> argMap) {
    if ((argMap == null) || (argMap.size() == 0)) {
      return (new OptArgWithStatus(null, true));
    }
    ControlFlow.OptArgs retval;
    try {
      switch (flowKey.getFlowType()) {
        case MAIN:       
          switch ((MainFlow)flowKey) {
            case REMOVE_SELECTIONS_FOR_NODE_TYPE:
              retval = new Selection.TypeArg(argMap);
              break;
            case LOAD_RECENT:
              retval = new LoadSaveOps.FileArg(argMap);
              break;
            case SET_CURRENT_GAGGLE_TARGET:
              retval = new GaggleOps.GaggleArg(argMap);
              break;
            case TREE_PATH_SET_CURRENT_USER_PATH:
              retval = new PathStep.PathArg(argMap);
              break;  
            default:
              retval = null;
          }
          break;
        case MODEL_TREE:
          retval = null;
          break;
        case POP:
          switch ((PopFlow)flowKey) {
            case GROUP_PROPERTIES:
            case REGION_MAP:   
            case DELETE_REGION_MAP: 
            case INPUT_REGION_MAP:       
            case DELETE_INPUT_REGION_MAP:
              retval = new EditGroupProperties.GroupArg(argMap);
              break;
            case DROP_NET_MODULE_REGION: 
              retval = new RemoveModuleOrPart.HardwiredEnabledArgs(argMap);
              break;
            case MOVE_NET_MODULE_REGION:
              retval = new Mover.MoveNetModuleRegionArgs(argMap);
              break;
            case DELETE_FROM_MODULE:
            case ADD_TO_MODULE:
              retval = new AddNodeToModule.NamedModuleArgs(argMap);
              break;
            case ANALYZE_PATHS_FOR_NODE: 
            case PERTURB_PATH_COMPARE:
              retval = new DisplayPaths.PathAnalysisArgs(argMap);
              break;
            case DELETE_SUB_GROUP: 
              retval = new RemoveSubGroup.SubGroupArgs(argMap);
              break;
            case ADD_NODE_TO_SUB_GROUP:
            case DELETE_NODE_FROM_SUB_GROUP:
              retval = new AddNodeToSubGroup.NodeInGroupArgs(argMap);
              break;
            case INCLUDE_SUB_GROUP:
            case ACTIVATE_SUB_GROUP: 
              retval = new AddSubGroup.GroupPairArgs(argMap);
              break;
            default:
              retval = null;
          }
          break;
        case KEY_BOUND:
        case OTHER:
          retval = null;
          break;
        default:
          throw new IOException();
      }
      return (new OptArgWithStatus(retval, true));
    } catch (IOException ioex) {
      return (new OptArgWithStatus(null, false));
    }
  }
    
  /***************************************************************************
  **
  ** Get Control Flow 
  */ 
  
  public ControlFlow getControlFlow(FlowKey flowKey, ControlFlow.OptArgs args) {
    switch (flowKey.getFlowType()) {
      case MAIN:
        if (args == null) {
          return (getMainControlFlowNoArgs((MainFlow)flowKey));
        } else {
          return (getMainControlFlowWithArgs((MainFlow)flowKey, args));      
        }
      case MODEL_TREE:
        if (args != null) {
          throw new IllegalArgumentException();
        }
        return (getModelTreeFlow((TreeFlow)flowKey));
      case POP:
        return (getPopFlow((PopFlow)flowKey, args));
      case KEY_BOUND:
        if (args != null) {
          throw new IllegalArgumentException();
        }
        return (getKeyBoundFlow((KeyFlowKey)flowKey));     
      case OTHER:
        if (args != null) {
          throw new IllegalArgumentException();
        }
        return (getOtherFlow((OtherFlowKey)flowKey));                    
      default:
        throw new IllegalArgumentException();
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get Main Control Flows (Main Menu and Toolbar buttons) that cannot be cached!
  */ 
  
  private ControlFlow getMainControlFlowWithArgs(MainFlow flowKey, ControlFlow.OptArgs args) {
    switch (flowKey) { 
      case TREE_PATH_SET_CURRENT_USER_PATH:
        return (new PathStep(appState_, (PathStep.PathArg)args));    
      case REMOVE_SELECTIONS_FOR_NODE_TYPE:
        return (new Selection(appState_, Selection.SelectAction.DROP_NODE_TYPE, (Selection.TypeArg)args));
      case LOAD_RECENT: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.LOAD_RECENT, (LoadSaveOps.FileArg)args));
      case SET_CURRENT_GAGGLE_TARGET:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.SET_CURRENT, (GaggleOps.GaggleArg)args));   
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get Main Control Flows (Main Menu and Toolbar buttons). These can be cached.
  */ 
  
  private ControlFlow getMainControlFlowNoArgs(MainFlow flowKey) {
    switch (flowKey) { 
      case QPCR_WRITER: 
        return (new ExportPerturb(appState_)); 
      case SBML_WRITER: 
        return (new ExportSBML(appState_)); 
      case NETWORK_SEARCH:
        return (new NetworkSearch(appState_)); 
      case GENOME_TO_SIF:
        return (new ExportGenomeToSIF(appState_));           
      case EXPORT_PUBLISH:
        return (new ExportPublish(appState_)); 
      case EXPRESSION_TABLES_TO_CSV: 
        return (new ExportExpression(appState_));
      case PERTURBATION_TO_CSV: 
        return (new ExportPerturbCSV(appState_)); 
      case BUILD_INSTRUCTIONS_TO_CSV:
        return (new ExportBuildInstr(appState_));
      case ADD:
        return (new AddNode(appState_, true)); 
      case ADD_NODE:
        return (new AddNode(appState_, false)); 
      case ADD_NOTE:
        return (new AddNote(appState_)); 
      case ADD_NETWORK_OVERLAY:
        return (new AddNetworkOverlay(appState_)); 
      case CHANGE_MODEL_DATA:
        return (new EditModelData(appState_)); 
      case SQUASH_GENOME:
        return (new SquashExpandGenome(appState_, true, false));      
      case EXPAND_GENOME:
        return (new SquashExpandGenome(appState_, false, false));
      case ADD_GENOME_INSTANCE:
        return (new AddGenomeInstance(appState_)); 
      case DRAW_GROUP_IN_INSTANCE:
        return (new DrawGroup(appState_));  
      case EDIT_CURR_NETWORK_OVERLAY:
        return (new EditNetOverlay(appState_, false));    
      case TREE_PATH_BACK:          
        return (new PathStep(appState_, false)); 
      case TREE_PATH_FORWARD:
        return (new PathStep(appState_, true));
      case TREE_PATH_CREATE:
        return (new PathManage(appState_, true));
      case TREE_PATH_DELETE:        
        return (new PathManage(appState_, false));
      case SELECT_ALL:
        return (new Selection(appState_, Selection.SelectAction.ALL));
      case SELECT_NONE:
        return (new Selection(appState_, Selection.SelectAction.NONE)); 
      case SELECT_STEP_UPSTREAM:
        return (new SearchAStep(appState_, true));
      case SELECT_STEP_DOWNSTREAM:
        return (new SearchAStep(appState_, false));
      case DROP_NODE_SELECTIONS:
        return (new Selection(appState_, Selection.SelectAction.DROP_NODES));    
      case DROP_LINK_SELECTIONS:
        return (new Selection(appState_, Selection.SelectAction.DROP_LINKS));
      case SELECT_NON_ORTHO_SEGS:
        return (new Selection(appState_, Selection.SelectAction.SELECT_NON_ORTHO));
      case REMOVE_CURR_NETWORK_OVERLAY:  
        return (new RemoveOverlay(appState_, false));   
      case SPECIALTY_LAYOUT_HALO:
        return (new SpecialtyLayoutFlow(appState_, new HaloLayout(appState_), SpecialtyLayoutFlow.LAYOUT_ALL));
      case SPECIALTY_LAYOUT_WORKSHEET:
        return (new SpecialtyLayoutFlow(appState_, new WorksheetLayout(appState_, false), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_DIAGONAL:
        return (new SpecialtyLayoutFlow(appState_, new WorksheetLayout(appState_, true), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_STACKED:
        return (new SpecialtyLayoutFlow(appState_, new StackedBlockLayout(appState_), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_SELECTED:
        return (new SpecialtyLayoutFlow(appState_, new StackedBlockLayout(appState_), SpecialtyLayoutFlow.LAYOUT_SELECTION)); 
      case SPECIALTY_LAYOUT_PER_OVERLAY:
        return (new SpecialtyLayoutFlow(appState_, new StackedBlockLayout(appState_), SpecialtyLayoutFlow.LAYOUT_PER_OVERLAY));    
      case RELAYOUT_ALL_LINKS:
        return (new RelayoutLinks(appState_, true, false));           
      case RELAYOUT_DIAG_LINKS:
        return (new RelayoutLinks(appState_, false, false));                
      case REPAIR_ALL_TOPOLOGY:
        return (new LinkRepair(appState_, false, false));          
      case ZOOM_TO_ALL_MODELS: 
        return (new Zoom(appState_, Zoom.ZoomAction.ALL_MODELS));          
      case ZOOM_TO_CURRENT_MODEL: 
        return (new Zoom(appState_, Zoom.ZoomAction.CURRENT_MODEL));          
      case ZOOM_TO_SHOW_WORKSPACE: 
        return (new Zoom(appState_, Zoom.ZoomAction.WORKSPACE));  
      case ZOOM_TO_ALL_SELECTED: 
        return (new Zoom(appState_, Zoom.ZoomAction.SELECTED));
      case ZOOM_TO_CURRENT_SELECTED: 
        return (new Zoom(appState_, Zoom.ZoomAction.CURRENT_SELECTED));
      case CENTER_ON_NEXT_SELECTED: 
        return (new Zoom(appState_, Zoom.ZoomAction.CENTER_NEXT_SELECTED));  
      case CENTER_ON_PREVIOUS_SELECTED: 
        return (new Zoom(appState_, Zoom.ZoomAction.CENTER_PREV_SELECTED));
      case TOGGLE_BUBBLES:
        return (new Toggle(appState_, Toggle.ToggleAction.PAD_BUBBLES)); 
      case TOGGLE_MODULE_COMPONENT_DISPLAY:
        return (new Toggle(appState_, Toggle.ToggleAction.MOD_PARTS)); 
      case ZOOM_TO_CURRENT_NETWORK_MODULE: 
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_CURRENT_MOD));
      case ZOOM_OUT:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_OUT));
      case ZOOM_IN: 
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_IN)); 
      case CANCEL_ADD_MODE:
        return (new CancelAdd(appState_));           
      case RECOLOR_LAYOUT:
        return (new Recolor(appState_));          
      case ADD_EXTRA_PROXY_NODE:
        return (new AddExtraProxyNode(appState_));
      case ADD_LINK:
        return (new AddLink(appState_, false));
      case DRAW_NETWORK_MODULE_LINK:
        return (new AddLink(appState_, true));
      case DROP_ALL_INSTRUCTIONS:
        return (new RemoveBuildInstruct(appState_));
      case GAGGLE_RAISE_GOOSE:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.RAISE));           
      case GAGGLE_LOWER_GOOSE:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.LOWER));   
      case GAGGLE_SEND_NETWORK:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.SEND_NET));         
      case GAGGLE_SEND_NAMELIST:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.SEND_NAMES));
      case GAGGLE_GOOSE_UPDATE:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.GOOSE_UPDATE));
      case GAGGLE_PROCESS_INBOUND:
        return (new GaggleOps(appState_, GaggleOps.GaggleAction.PROCESS_INBOUND));
      case UNDO:
        return (new UndoRedoOps(appState_, UndoRedoOps.UndoAction.UNDO)); 
      case REDO:
        return (new UndoRedoOps(appState_, UndoRedoOps.UndoAction.REDO)); 
      case SET_DISPLAY_OPTIONS:
        return (new DoSettings(appState_, DoSettings.SettingAction.DISPLAY));
      case EDIT_COLORS:
        return (new DoSettings(appState_, DoSettings.SettingAction.COLORS));
      case SET_FONT:
        return (new DoSettings(appState_, DoSettings.SettingAction.FONTS));
      case TIME_COURSE_TABLE_SETUP: 
        return (new DoSettings(appState_, DoSettings.SettingAction.TIME_COURSE_SETUP));
      case TIME_COURSE_REGION_HIERARCHY: 
        return (new DoSettings(appState_, DoSettings.SettingAction.REGION_HIERARCHY));
      case TIME_COURSE_TABLE_MANAGE: 
        return (new DoSettings(appState_, DoSettings.SettingAction.TIME_COURSE_MANAGE));
      case TEMPORAL_INPUT_TABLE_MANAGE: 
        return (new DoSettings(appState_, DoSettings.SettingAction.TEMPORAL_INPUT_MANAGE));
      case SET_AUTOLAYOUT_OPTIONS:
        return (new DoSettings(appState_, DoSettings.SettingAction.AUTO_LAYOUT));           
      case DEFINE_TIME_AXIS:
        return (new DoSettings(appState_, DoSettings.SettingAction.TIME_AXIS)); 
      case PERTURB_MANAGE: 
        return (new DoSettings(appState_, DoSettings.SettingAction.PERTURB));
      case IMPORT_FULL_HIERARCHY_FROM_CSV:
        return (new ImportCSV(appState_));
      case IMPORT_GENOME_FROM_SIF:
        return (new ImportSIF(appState_)); 
      case WEB:
        return (new ExportWeb(appState_)); 
      case EXPORT:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.EXPORT)); 
      case SAVE: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.SAVE)); 
      case SAVE_AS: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.SAVE_AS)); 
      case LOAD:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.LOAD)); 
      case NEW_MODEL: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.NEW_MODEL));
      case CLEAR_RECENT: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.CLEAR_RECENT));
      case IMPORT_PERTURBED_EXPRESSION_CSV:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.PERT_EXPRESS)); 
      case IMPORT_TIME_COURSE_XML: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.TIME_COURSE));      
      case IMPORT_TEMPORAL_INPUT_XML: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.TEMPORAL_INPUT));
      case COPIES_PER_EMBRYO_IMPORT:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.EMBRYO_COUNTS));
      case IMPORT_PERTURB_CSV_FROM_FILE:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.PERTURB));
      case ALIGN_ALL_LAYOUTS:
        return (new Align(appState_)); 
      case TREE_PATH_ADD_STOP:
        return (new PathStop(appState_, true));
      case TREE_PATH_DELETE_STOP:
        return (new PathStop(appState_, false));   
      case REPAIR_ALL_NON_ORTHO_MIN_SHIFT:
        return (new LayoutNonOrthoLink(appState_, true, false, false, false));      
      case REPAIR_ALL_NON_ORTHO_MIN_SPLIT:
        return (new LayoutNonOrthoLink(appState_, true, true, false, false));
      case OPTIMIZE_LINKS:
        return (new OptimizeLink(appState_, false, false));           
      case CENTER_CURRENT_MODEL_ON_WORKSPACE: 
        return (new CenterCurrent(appState_)); 
      case RESIZE_WORKSPACE: 
        return (new WorkspaceOps(appState_, WorkspaceOps.SpaceAction.RESIZE)); 
      case SHIFT_ALL_MODELS_TO_WORKSPACE: 
        return (new WorkspaceOps(appState_, WorkspaceOps.SpaceAction.SHIFT_ALL));
      case GET_MODEL_COUNTS:
        return (new ShowInfo(appState_, ShowInfo.InfoType.COUNTS)); 
      case HELP_POINTER:
        return (new ShowInfo(appState_, ShowInfo.InfoType.HELP)); 
      case ABOUT:
        return (new ShowInfo(appState_, ShowInfo.InfoType.ABOUT)); 
      case BUILD_FROM_DIALOG:
        return (new DialogBuild(appState_));           
      case APPLY_KID_LAYOUTS_TO_ROOT:
        return (new UpwardSync(appState_));
      case SYNC_ALL_LAYOUTS:
        return (new DownwardSync(appState_)); 
      case ADD_QPCR_TO_ROOT_INSTANCES:
        return (new SimpleBuilds(appState_, SimpleBuilds.SimpleType.PERT_TO_ROOT_INST));       
      case PROP_ROOT_WITH_EXP_DATA:
        return (new SimpleBuilds(appState_, SimpleBuilds.SimpleType.PROP_ROOT)); 
      case PRINT:
        return (new Print(appState_)); 
      case MULTI_GROUP_COPY:
        return (new DupGroups(appState_, false));
      case PROPAGATE_DOWN:
        return (new PropagateDown(appState_)); 
      case MODEL_EXPORT:
        return (new ExportModel(appState_)); 
      case ASSIGN_IMAGE_TO_MODEL:
        return (new ModelImageOps(appState_, ModelImageOps.ImageAction.ADD));
      case DROP_IMAGE_FOR_MODEL:
        return (new ModelImageOps(appState_, ModelImageOps.ImageAction.DROP));
      case DRAW_NETWORK_MODULE:
        return (new AddNetModule(appState_, true));  
      case PULLDOWN:
        return (new PullDown(appState_));
     case CLOSE:
        return (new CloseApp(appState_)); 
      default:
        throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Get Model Tree Control Flows (Main Menu and Toolbar buttons). These can be cached.
  */ 
  
  private ControlFlow getModelTreeFlow(TreeFlow key) {  
    switch (key) {
      case ADD_GENOME_INSTANCE:
        return (new AddGenomeInstance(appState_));
      case ADD_DYNAMIC_GENOME_INSTANCE:
        return (new AddDynamicGenomeInstance(appState_));
      case DELETE_GENOME_INSTANCE:
        return (new DeleteGenomeInstance(appState_, false));
      case DELETE_GENOME_INSTANCE_KIDS_ONLY:
        return (new DeleteGenomeInstance(appState_, true));
      case EDIT_MODEL_PROPERTIES:
        return (new EditModelProps(appState_));
      case COPY_GENOME_INSTANCE:
        return (new CopyGenomeInstance(appState_));
      case MOVE_MODEL_NODEUP:
        return (new MoveModelNode(appState_, false));
      case MOVE_MODEL_NODE_DOWN:
        return (new MoveModelNode(appState_, true)); 
      case MAKE_STARTUP_VIEW:
        return (new SettingOps(appState_, SettingOps.SettingAction.STARTUP_VIEW));
      case SET_CURRENT_OVERLAY_FOR_FIRST_VIEW:
        return (new SettingOps(appState_, SettingOps.SettingAction.OVERLAY_FIRST));
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get Key Bound Control Flows. These can be cached.
  */ 
  
  private ControlFlow getKeyBoundFlow(KeyFlowKey key) {  
    switch (key) {
      case MULTI_DELETE:
        return (new RemoveSelections(appState_));
      case CANCEL_MODE:
        return (new CancelAdd(appState_));
      case NUDGE_UP:
        return (new Mover(appState_, Mover.Action.NUDGE_UP));  
      case NUDGE_DOWN:
        return (new Mover(appState_, Mover.Action.NUDGE_DOWN));    
      case NUDGE_LEFT:
        return (new Mover(appState_, Mover.Action.NUDGE_LEFT));    
      case NUDGE_RIGHT:
        return (new Mover(appState_, Mover.Action.NUDGE_RIGHT));    
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get Key Bound Control Flows. These can be cached.
  */ 
  
  private ControlFlow getOtherFlow(OtherFlowKey key) {  
    switch (key) {
      case REMOTE_BATCH_SELECTION:
        return (new Selection(appState_, Selection.SelectAction.REMOTE_BATCH_SELECT));
      case LOCAL_CLICK_SELECTION:
        return (new Selection(appState_, Selection.SelectAction.LOCAL_SINGLE_CLICK_SELECT)); 
      case LOCAL_RECT_SELECTION:
        return (new Selection(appState_, Selection.SelectAction.LOCAL_SELECTION_BY_RECT));       
      case MODEL_SELECTION_FROM_TREE: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.VIA_MODEL_TREE));
      case MODEL_SELECTION_FROM_SLIDER:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.VIA_SLIDER));
      case MOVE_ELEMENTS:
        return (new Mover(appState_, Mover.Action.MOVE_MODEL_ELEMS));
      case MODEL_SELECTION: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_SELECTION_ONLY));
      case MODEL_AND_NODE_SELECTION: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_NODE_SELECTIONS));
      case MODEL_AND_LINK_SELECTION:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_LINK_SELECTIONS));
      case MODEL_AND_NODE_LINK_SELECTION:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_SELECTIONS));
      case PATH_MODEL_GENERATION:
        return (new PathGenerator(appState_));
      case TARDY_LINK_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.PENDING_LINK)); 
      case TARDY_NODE_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.PENDING_NODE)); 
      default:
        throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Get Control Flows. Cannot cache!
  */ 
  
  private ControlFlow getPopFlow(PopFlow key, ControlFlow.OptArgs args) {     
    switch (key) {
      case EDIT_CURRENT_OVERLAY:
        return (new EditNetOverlay(appState_, true));
      case DELETE_CURRENT_OVERLAY:
        return (new RemoveOverlay(appState_, true));
      case EDIT_NOTE:
        return (new EditNote(appState_, true));
      case DELETE_NOTE:
        return (new RemoveNote(appState_));       
      case DELETE_LINK_POINT:
        return (new RemoveLinkPoint(appState_));
      case TOGGLE:
        return (new Toggle(appState_, Toggle.ToggleAction.GROUP_TOGGLE));
      case ZOOM_TO_GROUP:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_TO_GROUP));
      case GROUP_PROPERTIES:
        return (new EditGroupProperties(appState_, (EditGroupProperties.GroupArg)args));
      case INCLUDE_ALL_FOR_GROUP:
        return (new IncludeAllForGroup(appState_));
      case RAISE_GROUP:
        return (new ReorderGroups(appState_, ReorderGroups.Direction.RAISE));
      case LOWER_GROUP:
        return (new ReorderGroups(appState_, ReorderGroups.Direction.LOWER));       
      case MOVE_GROUP:
        return (new Mover(appState_, Mover.Action.GROUP));
      case LAYOUT_GROUP:
        return (new SpecialtyLayoutFlow(appState_, new StackedBlockLayout(appState_), SpecialtyLayoutFlow.LAYOUT_REGION));  
      case COMPRESS_GROUP:
        return (new SquashExpandGenome(appState_, true, true));          
      case EXPAND_GROUP:
        return (new SquashExpandGenome(appState_, false, true));
      case COPY_REGION:
        return (new DupGroups(appState_, true)); 
      case GROUP_DELETE:
        return (new RemoveGroup(appState_));
      case REGION_MAP:
        return (new TimeCourse(appState_, TimeCourse.Action.EDIT_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case DELETE_REGION_MAP:
        return (new TimeCourse(appState_, TimeCourse.Action.DELETE_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case INPUT_REGION_MAP:
        return (new TemporalInput(appState_, TemporalInput.Action.EDIT_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case DELETE_INPUT_REGION_MAP:
        return (new TemporalInput(appState_, TemporalInput.Action.DELETE_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case CREATE_SUB_GROUP:
        return (new AddSubGroup(appState_, AddSubGroup.Action.CREATE));
      case LINK_PROPERTIES:
        return (new EditLinkProperties(appState_)); 
      case SPECIAL_LINE:
        return (new SpecialLineProps(appState_));  
      case INSERT_GENE_IN_LINK:
        return (new InsertNodeInLink(appState_, true));  
      case INSERT_NODE_IN_LINK:
        return (new InsertNodeInLink(appState_, false)); 
      case CHANGE_SOURCE_NODE:
        return (new ChangeNode(appState_, false));  
      case CHANGE_TARGET_NODE:
        return (new ChangeNode(appState_, true));
      case DIVIDE:
        return (new Divide(appState_, false));
      case DIVIDE_MOD:
        return (new Divide(appState_, true));        
      case RELOCATE_SEGMENT:
        return (new RelocateSeg(appState_, false));
      case RELOCATE_SEGMENT_MOD:
        return (new RelocateSeg(appState_, true));        
      case RELOCATE_SOURCE_PAD:
        return (new ChangePad(appState_, false, false));
      case RELOCATE_SOURCE_PAD_MOD:
        return (new ChangePad(appState_, false, true));        
      case RELOCATE_TARGET_PAD:
        return (new ChangePad(appState_, true, false));
      case RELOCATE_TARGET_PAD_MOD:
        return (new ChangePad(appState_, true, true));        
      case SWAP_PADS:
        return (new SwapPads(appState_));  
      case DELETE_LINKAGE:
        return (new RemoveLinkage(appState_)); 
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT:
        return (new LayoutNonOrthoLink(appState_, true, false, true, false));
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT_MOD:
        return (new LayoutNonOrthoLink(appState_, true, false, true, true));
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT:
        return (new LayoutNonOrthoLink(appState_, true, true, true, false));        
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT_MOD:
        return (new LayoutNonOrthoLink(appState_, true, true, true, true));
      case FIX_NON_ORTHO_MIN_SHIFT:
        return (new LayoutNonOrthoLink(appState_, false, false, true, false));
      case FIX_NON_ORTHO_MIN_SHIFT_MOD:
        return (new LayoutNonOrthoLink(appState_, false, false, true, true));
      case FIX_NON_ORTHO_MIN_SPLIT:
        return (new LayoutNonOrthoLink(appState_, false, true, true, false));
      case FIX_NON_ORTHO_MIN_SPLIT_MOD:
        return (new LayoutNonOrthoLink(appState_, false, true, true, true));
      case TOPO_REPAIR:
        return (new LinkRepair(appState_, false, true));
      case TOPO_REPAIR_MOD:
        return (new LinkRepair(appState_, true, true));
      case SEG_LAYOUT:
        return (new RelayoutLinks(appState_, false, true));
      case SEG_OPTIMIZE:
        return (new OptimizeLink(appState_, true, false));
      case MODULE_LINK_PROPERTIES:
        return (new EditNetModuleLink(appState_)); 
      case DELETE_MODULE_LINKAGE:
        return (new RemoveModuleLinkOrPoint(appState_, false));         
      case DISPLAY_LINK_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.LINK));
      case FULL_SELECTION:
        return (new Selection(appState_, Selection.SelectAction.SELECT_FULL_LINK));       
      case ANALYZE_PATHS:
      case ANALYZE_PATHS_WITH_QPCR:
        return (new DisplayPaths(appState_, DisplayPaths.InfoType.LINK_PATHS)); 
      case ANALYZE_PATHS_FROM_USER_SELECTED:
        return (new DisplayPathsUserSource(appState_, true));        
      case SELECT_LINK_SOURCE:
        return (new NodeSrcTargSearch(appState_, true));
      case SELECT_LINK_TARGETS:
        return (new NodeSrcTargSearch(appState_, false));
      case LINK_USAGES:
        return (new DisplayData(appState_, DisplayData.InfoType.LINK_USAGES));
      case SELECT_SOURCES:
        return (new NodeSrcTargSearch(appState_, false, true));
      case SELECT_SOURCES_GENE_ONLY:
        return (new NodeSrcTargSearch(appState_, true, true));
      case SELECT_TARGETS:
        return (new NodeSrcTargSearch(appState_, false, false));
      case SELECT_TARGETS_GENE_ONLY:
        return (new NodeSrcTargSearch(appState_, true, false));
      case SELECT_LINKS_TOGGLE:
        return (new Toggle(appState_, Toggle.ToggleAction.SELECT_LINKS));
      case SELECT_QUERY_NODE_TOGGLE:
        return (new Toggle(appState_, Toggle.ToggleAction.SELECT_QUERY_NODE));
      case APPEND_TO_CURRENT_SELECTION_TOGGLE:
        return (new Toggle(appState_, Toggle.ToggleAction.APPEND_SELECT));
      case NODE_USAGES:
        return (new DisplayData(appState_, DisplayData.InfoType.NODE_USAGES));
      case SIMULATION_PROPERTIES:
        return (new EditSimProperties(appState_));
      case NODE_SUPER_ADD:
        return (new SuperAdd(appState_));
      case NODE_TYPE_CHANGE:
        return (new ChangeNodeType(appState_));
      case CHANGE_NODE_GROUP_MEMBERSHIP:
        return (new ChangeNodeGroup(appState_));
      case DELETE_NODE:
        return (new RemoveNode(appState_));
      case EDIT_PERT:
        return (new PerturbData(appState_, PerturbData.InfoType.EDIT));       
      case DELETE_PERT:
        return (new PerturbData(appState_, PerturbData.InfoType.DELETE));        
      case EDIT_PERT_MAP:
        return (new PerturbData(appState_, PerturbData.InfoType.PERT_MAP));        
      case DELETE_PERT_MAP:
        return (new PerturbData(appState_, PerturbData.InfoType.DELETE_PERT_MAP));
      case EDIT_TIME_COURSE:
        return (new TimeCourse(appState_, TimeCourse.Action.EDIT));       
      case DELETE_TIME_COURSE:
        return (new TimeCourse(appState_, TimeCourse.Action.DELETE));        
      case EDIT_PERTURBED_TIME_COURSE:
        return (new TimeCourse(appState_, TimeCourse.Action.EDIT_PERT));
      case EDIT_TIME_COURSE_MAP:
        return (new TimeCourse(appState_, TimeCourse.Action.EDIT_MAP));       
      case DELETE_TIME_COURSE_MAP:
        return (new TimeCourse(appState_, TimeCourse.Action.DELETE_MAP));
      case EDIT_TEMPORAL_INPUT:
        return (new TemporalInput(appState_, TemporalInput.Action.EDIT));
      case DELETE_TEMPORAL_INPUT:
        return (new TemporalInput(appState_, TemporalInput.Action.DELETE));
      case EDIT_TEMPORAL_INPUT_MAP:
        return (new TemporalInput(appState_, TemporalInput.Action.EDIT_MAP));
      case DELETE_TEMPORAL_INPUT_MAP:
        return (new TemporalInput(appState_, TemporalInput.Action.DELETE_MAP)); 
      case DISPLAY_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.NODE)); 
      case GENE_PROPERTIES:
        return (new EditNodeProperties(appState_, true)); 
      case NODE_PROPERTIES:
        return (new EditNodeProperties(appState_, false));         
      case EDIT_MULTI_SELECTIONS:
        return (new MultiSelectionProperties(appState_));
      case ZOOM_TO_NET_MODULE:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_MOD_POPUP));
      case TOGGLE_NET_MODULE_CONTENT_DISPLAY:
        return (new Toggle(appState_, Toggle.ToggleAction.NET_MOD_CONTENT));
      case SET_AS_SINGLE_CURRENT_NET_MODULE:
        return (new Modules(appState_, Modules.ModAction.SET_AS_SINGLE));
      case DROP_FROM_CURRENT_NET_MODULES:
        return (new Modules(appState_, Modules.ModAction.DROP_FROM_CURRENT));
      case EDIT_SELECTED_NETWORK_MODULE:
        return (new EditNetModule(appState_)); 
      case ADD_REGION_TO_NET_MODULE:
        return (new AddNetModule(appState_, false));
      case DETACH_MODULE_FROM_GROUP:
        return (new RemoveModuleOrPart(appState_, RemoveModuleOrPart.ModAction.DETACH_FROM_GROUP, null));
      case REMOVE_THIS_NETWORK_MODULE:
        return (new RemoveModuleOrPart(appState_, RemoveModuleOrPart.ModAction.REMOVE_ALL, null));
      case EDIT_MODULE_MEMBERS:
        return (new EditNetModuleMembers(appState_));
      case SIZE_CORE_TO_REGION_BOUNDS:
        return (new ResizeModuleCore(appState_));
      case DROP_NET_MODULE_REGION:
        return (new RemoveModuleOrPart(appState_, RemoveModuleOrPart.ModAction.REMOVE_REGION, (RemoveModuleOrPart.HardwiredEnabledArgs)args));
      case MOVE_NET_MODULE_REGION:
        return (new Mover(appState_, Mover.Action.MODULES, (Mover.MoveNetModuleRegionArgs)args));
      case DELETE_MODULE_LINK_POINT:
        return (new RemoveModuleLinkOrPoint(appState_, true));
      case DELETE_FROM_MODULE:
        return (new RemoveNodeFromMod(appState_, (AddNodeToModule.NamedModuleArgs)args));
      case ADD_TO_MODULE:
        return (new AddNodeToModule(appState_, (AddNodeToModule.NamedModuleArgs)args));
      case ANALYZE_PATHS_FOR_NODE:
        return (new DisplayPaths(appState_, DisplayPaths.InfoType.PATHS_FOR_NODE, (DisplayPaths.PathAnalysisArgs)args));
      case PERTURB_PATH_COMPARE:
        return (new DisplayPaths(appState_, DisplayPaths.InfoType.PATHS_FOR_NODE, (DisplayPaths.PathAnalysisArgs)args));       
      case DELETE_SUB_GROUP:
        return (new RemoveSubGroup(appState_, (RemoveSubGroup.SubGroupArgs)args));
      case ADD_NODE_TO_SUB_GROUP:
        return (new AddNodeToSubGroup(appState_, (AddNodeToSubGroup.NodeInGroupArgs)args)); 
      case DELETE_NODE_FROM_SUB_GROUP:
        return (new RemoveNodeFromSubGroup(appState_, (AddNodeToSubGroup.NodeInGroupArgs)args)); 
      case INCLUDE_SUB_GROUP:
        return (new AddSubGroup(appState_, AddSubGroup.Action.INCLUDE,  (AddSubGroup.GroupPairArgs)args));
      case ACTIVATE_SUB_GROUP:
        return (new AddSubGroup(appState_, AddSubGroup.Action.ACTIVATE, (AddSubGroup.GroupPairArgs)args));         
      default:
        throw new IllegalArgumentException();
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static class OptArgWithStatus {
    
    public ControlFlow.OptArgs arg;
    public boolean ok;
        
    OptArgWithStatus(ControlFlow.OptArgs arg, boolean ok) {
      this.arg = arg;
      this.ok = ok;
    }
  }    

  
  
}
