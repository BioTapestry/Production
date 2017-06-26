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


package org.systemsbiology.biotapestry.cmd.flow;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;

import org.systemsbiology.biotapestry.cmd.flow.add.AddCisRegModule;
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
import org.systemsbiology.biotapestry.cmd.flow.edit.EditCisRegModule;
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
import org.systemsbiology.biotapestry.cmd.flow.edit.MergeDuplicateLinks;
import org.systemsbiology.biotapestry.cmd.flow.edit.MergeDuplicateNodes;
import org.systemsbiology.biotapestry.cmd.flow.edit.MultiSelectionProperties;
import org.systemsbiology.biotapestry.cmd.flow.edit.ResizeModuleCore;
import org.systemsbiology.biotapestry.cmd.flow.edit.SetSelectedInactive;
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
import org.systemsbiology.biotapestry.cmd.flow.image.ImageOps;
import org.systemsbiology.biotapestry.cmd.flow.info.ShowInfo;
import org.systemsbiology.biotapestry.cmd.flow.io.ExportGroupNode;
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
import org.systemsbiology.biotapestry.cmd.flow.modelTree.AddGroupNode;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.CopyGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.DeleteGenomeInstance;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.EditGroupNodeNavMap;
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
import org.systemsbiology.biotapestry.cmd.flow.simulate.SimulationLaunch;
import org.systemsbiology.biotapestry.cmd.flow.tabs.TabOps;
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

import org.systemsbiology.biotapestry.cmd.flow.netBuild.LaunchLinkDrawTracker;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.LaunchBuilder;
import org.systemsbiology.biotapestry.cmd.flow.netBuild.BuilderPluginArg;

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
    LOAD_AS_NEW_TABS,
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
    GET_DATA_SHARING_STATE,
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
    GROUP_NODE_EXPORT,
    TREE_PATH_SET_CURRENT_USER_PATH,
    SET_CURRENT_GAGGLE_TARGET,
    SELECT_STEP_UPSTREAM,
    SELECT_STEP_DOWNSTREAM,
    LAUNCH_SIM_PLUGIN,
    RECOVER_SIMULATION,
    SELECT_ROOT_ONLY_NODES,
    SELECT_ROOT_ONLY_LINKS,
    SELECTIONS_TO_INACTIVE,
    LAUNCH_LINK_DRAWING_TRACKER,
    LAUNCH_WORKSHEET,
    TIME_COURSE_REGION_TOPOLOGY,
    NEW_TAB,
    DROP_THIS_TAB,
    DROP_ALL_BUT_THIS_TAB, 
    CHANGE_TAB,
    RETITLE_TAB,
    TEMPORAL_INPUT_DERIVE,
    TEMPORAL_INPUT_DROP_ALL,
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
    ADD_MODEL_TREE_GROUP_NODE, 
    EDIT_GROUP_NODE_NAV_MAP, 
    ASSIGN_IMAGE_TO_MODEL,
    DROP_IMAGE_FOR_MODEL,
    ASSIGN_IMAGE_TO_GROUPING_NODE,
    DROP_IMAGE_FOR_GROUPING_NODE,
    ASSIGN_MAPPING_IMAGE_TO_GROUPING_NODE,
    DROP_MAPPING_IMAGE_FOR_GROUPING_NODE,
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
    CHANGE_TARGET_GENE_MODULE,
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
    MERGE_LINKS,
    MERGE_NODES,
    DEFINE_CIS_REG_MODULE,
    EDIT_CIS_REG_MODULE,
    LINK_WORKSHEET,
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
    GROUP_NODE_SELECTION,
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
    GROUP_NODE_CLICK,
    SYNC_WEB_CLIENT_STATE,
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
  private UIComponentSource uics_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public FlowMeister(BTState appState, UIComponentSource uics) {
    appState_ = appState;
    uics_ = uics;
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
        return (new PathStep((PathStep.PathArg)args));    
      case REMOVE_SELECTIONS_FOR_NODE_TYPE:
        return (new Selection(uics_, Selection.SelectAction.DROP_NODE_TYPE, (Selection.TypeArg)args));
      case LOAD_RECENT: 
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.LOAD_RECENT, (LoadSaveOps.FileArg)args));
      case SET_CURRENT_GAGGLE_TARGET:
        return (new GaggleOps(GaggleOps.GaggleAction.SET_CURRENT, (GaggleOps.GaggleArg)args));
      case LAUNCH_SIM_PLUGIN:
    	  return (new SimulationLaunch(appState_, SimulationLaunch.ActionType.LAUNCH, (SimulationLaunch.SimulationPluginArg)args));
      case RECOVER_SIMULATION: 
        return (new SimulationLaunch(appState_, SimulationLaunch.ActionType.LOAD_RESULTS, (SimulationLaunch.SimulationPluginArg)args));    
      case LAUNCH_WORKSHEET:
        return (new LaunchBuilder(false, (BuilderPluginArg)args));
      case LAUNCH_LINK_DRAWING_TRACKER:
        return (new LaunchLinkDrawTracker((BuilderPluginArg)args));               
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
        return (new NetworkSearch()); 
      case GENOME_TO_SIF:
        return (new ExportGenomeToSIF(appState_));           
      case EXPORT_PUBLISH:
        return (new ExportPublish()); 
      case EXPRESSION_TABLES_TO_CSV: 
        return (new ExportExpression(appState_));
      case PERTURBATION_TO_CSV: 
        return (new ExportPerturbCSV(appState_)); 
      case BUILD_INSTRUCTIONS_TO_CSV:
        return (new ExportBuildInstr(appState_));
      case ADD:
        return (new AddNode(true)); 
      case ADD_NODE:
        return (new AddNode(false)); 
      case ADD_NOTE:
        return (new AddNote()); 
      case ADD_NETWORK_OVERLAY:
        return (new AddNetworkOverlay()); 
      case CHANGE_MODEL_DATA:
        return (new EditModelData()); 
      case SQUASH_GENOME:
        return (new SquashExpandGenome(true, false));      
      case EXPAND_GENOME:
        return (new SquashExpandGenome(false, false));
      case ADD_GENOME_INSTANCE:
        return (new AddGenomeInstance()); 
      case DRAW_GROUP_IN_INSTANCE:
        return (new DrawGroup());  
      case EDIT_CURR_NETWORK_OVERLAY:
        return (new EditNetOverlay(false));    
      case TREE_PATH_BACK:          
        return (new PathStep(false, appState_)); 
      case TREE_PATH_FORWARD:
        return (new PathStep(true, appState_));
      case TREE_PATH_CREATE:
        return (new PathManage(true, appState_));
      case TREE_PATH_DELETE:        
        return (new PathManage(false, appState_));
      case SELECT_ALL:
        return (new Selection(Selection.SelectAction.ALL));
      case SELECT_NONE:
        return (new Selection(Selection.SelectAction.NONE)); 
      case SELECT_STEP_UPSTREAM:
        return (new SearchAStep(true));
      case SELECT_STEP_DOWNSTREAM:
        return (new SearchAStep(false));
      case DROP_NODE_SELECTIONS:
        return (new Selection(Selection.SelectAction.DROP_NODES));    
      case DROP_LINK_SELECTIONS:
        return (new Selection(Selection.SelectAction.DROP_LINKS));
      case SELECT_NON_ORTHO_SEGS:
        return (new Selection(Selection.SelectAction.SELECT_NON_ORTHO));
      case SELECT_ROOT_ONLY_NODES:
        return (new Selection(Selection.SelectAction.SELECT_ROOT_ONLY_NODE));
      case SELECT_ROOT_ONLY_LINKS:
        return (new Selection(Selection.SelectAction.SELECT_ROOT_ONLY_LINK));
      case REMOVE_CURR_NETWORK_OVERLAY:  
        return (new RemoveOverlay(false));   
      case SPECIALTY_LAYOUT_HALO:
        return (new SpecialtyLayoutFlow(new HaloLayout(new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_ALL));
      case SPECIALTY_LAYOUT_WORKSHEET:
        return (new SpecialtyLayoutFlow(new WorksheetLayout(false, new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_DIAGONAL:
        return (new SpecialtyLayoutFlow(new WorksheetLayout(true, new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_STACKED:
        return (new SpecialtyLayoutFlow(new StackedBlockLayout(new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_ALL));          
      case SPECIALTY_LAYOUT_SELECTED:
        return (new SpecialtyLayoutFlow(new StackedBlockLayout(new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_SELECTION)); 
      case SPECIALTY_LAYOUT_PER_OVERLAY:
        return (new SpecialtyLayoutFlow(new StackedBlockLayout(new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_PER_OVERLAY));    
      case RELAYOUT_ALL_LINKS:
        return (new RelayoutLinks(true, false));           
      case RELAYOUT_DIAG_LINKS:
        return (new RelayoutLinks(false, false));                
      case REPAIR_ALL_TOPOLOGY:
        return (new LinkRepair(false, false));          
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
        return (new Toggle(Toggle.ToggleAction.PAD_BUBBLES)); 
      case TOGGLE_MODULE_COMPONENT_DISPLAY:
        return (new Toggle(Toggle.ToggleAction.MOD_PARTS)); 
      case ZOOM_TO_CURRENT_NETWORK_MODULE: 
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_CURRENT_MOD));
      case ZOOM_OUT:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_OUT));
      case ZOOM_IN: 
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_IN)); 
      case CANCEL_ADD_MODE:
        return (new CancelAdd());           
      case RECOLOR_LAYOUT:
        return (new Recolor());          
      case ADD_EXTRA_PROXY_NODE:
        return (new AddExtraProxyNode());
      case ADD_LINK:
        return (new AddLink(false));
      case DRAW_NETWORK_MODULE_LINK:
        return (new AddLink(true));
      case DROP_ALL_INSTRUCTIONS:
        return (new RemoveBuildInstruct());
      case GAGGLE_RAISE_GOOSE:
        return (new GaggleOps(GaggleOps.GaggleAction.RAISE));           
      case GAGGLE_LOWER_GOOSE:
        return (new GaggleOps(GaggleOps.GaggleAction.LOWER));   
      case GAGGLE_SEND_NETWORK:
        return (new GaggleOps(GaggleOps.GaggleAction.SEND_NET));         
      case GAGGLE_SEND_NAMELIST:
        return (new GaggleOps(GaggleOps.GaggleAction.SEND_NAMES));
      case GAGGLE_GOOSE_UPDATE:
        return (new GaggleOps(GaggleOps.GaggleAction.GOOSE_UPDATE));
      case GAGGLE_PROCESS_INBOUND:
        return (new GaggleOps(GaggleOps.GaggleAction.PROCESS_INBOUND));
      case UNDO:
        return (new UndoRedoOps(UndoRedoOps.UndoAction.UNDO)); 
      case REDO:
        return (new UndoRedoOps(UndoRedoOps.UndoAction.REDO)); 
      case SET_DISPLAY_OPTIONS:
        return (new DoSettings(DoSettings.SettingAction.DISPLAY));
      case EDIT_COLORS:
        return (new DoSettings(DoSettings.SettingAction.COLORS));
      case SET_FONT:
        return (new DoSettings(DoSettings.SettingAction.FONTS));
      case TIME_COURSE_TABLE_SETUP: 
        return (new DoSettings(DoSettings.SettingAction.TIME_COURSE_SETUP));
      case TIME_COURSE_REGION_HIERARCHY: 
        return (new DoSettings(DoSettings.SettingAction.REGION_HIERARCHY));
      case TIME_COURSE_TABLE_MANAGE: 
        return (new DoSettings(DoSettings.SettingAction.TIME_COURSE_MANAGE));
      case TEMPORAL_INPUT_TABLE_MANAGE: 
        return (new DoSettings(DoSettings.SettingAction.TEMPORAL_INPUT_MANAGE));
      case SET_AUTOLAYOUT_OPTIONS:
        return (new DoSettings(DoSettings.SettingAction.AUTO_LAYOUT));           
      case DEFINE_TIME_AXIS:
        return (new DoSettings(DoSettings.SettingAction.TIME_AXIS)); 
      case PERTURB_MANAGE: 
        return (new DoSettings(DoSettings.SettingAction.PERTURB));
      case IMPORT_FULL_HIERARCHY_FROM_CSV:
        return (new ImportCSV());
      case IMPORT_GENOME_FROM_SIF:
        return (new ImportSIF()); 
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
      case LOAD_AS_NEW_TABS:
        return (new LoadSaveOps(appState_, LoadSaveOps.IOAction.LOAD_AS_NEW_TABS));        
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
        return (new Align()); 
      case TREE_PATH_ADD_STOP:
        return (new PathStop(true, appState_));
      case TREE_PATH_DELETE_STOP:
        return (new PathStop(false, appState_));   
      case REPAIR_ALL_NON_ORTHO_MIN_SHIFT:
        return (new LayoutNonOrthoLink(true, false, false, false));      
      case REPAIR_ALL_NON_ORTHO_MIN_SPLIT:
        return (new LayoutNonOrthoLink(true, true, false, false));
      case OPTIMIZE_LINKS:
        return (new OptimizeLink(false, false));           
      case CENTER_CURRENT_MODEL_ON_WORKSPACE: 
        return (new CenterCurrent()); 
      case RESIZE_WORKSPACE: 
        return (new WorkspaceOps(WorkspaceOps.SpaceAction.RESIZE)); 
      case SHIFT_ALL_MODELS_TO_WORKSPACE: 
        return (new WorkspaceOps(WorkspaceOps.SpaceAction.SHIFT_ALL));
      case GET_MODEL_COUNTS:
        return (new ShowInfo(appState_, ShowInfo.InfoType.COUNTS)); 
      case GET_DATA_SHARING_STATE:
        return (new ShowInfo(appState_, ShowInfo.InfoType.DATA_SHARING));          
      case HELP_POINTER:
        return (new ShowInfo(appState_, ShowInfo.InfoType.HELP)); 
      case ABOUT:
        return (new ShowInfo(appState_, ShowInfo.InfoType.ABOUT)); 
      case BUILD_FROM_DIALOG:
        return (new DialogBuild());           
      case TIME_COURSE_REGION_TOPOLOGY: 
        return (new DoSettings(DoSettings.SettingAction.REGION_TOPOLOGY));  
      case APPLY_KID_LAYOUTS_TO_ROOT:
        return (new UpwardSync());
      case SYNC_ALL_LAYOUTS:
        return (new DownwardSync()); 
      case ADD_QPCR_TO_ROOT_INSTANCES:
        return (new SimpleBuilds(SimpleBuilds.SimpleType.PERT_TO_ROOT_INST));       
      case PROP_ROOT_WITH_EXP_DATA:
        return (new SimpleBuilds(SimpleBuilds.SimpleType.PROP_ROOT)); 
      case PRINT:
        return (new Print()); 
      case MULTI_GROUP_COPY:
        return (new DupGroups(false));
      case PROPAGATE_DOWN:
        return (new PropagateDown()); 
      case MODEL_EXPORT:
        return (new ExportModel(appState_));
      case GROUP_NODE_EXPORT:
          return (new ExportGroupNode());         
      case DRAW_NETWORK_MODULE:
        return (new AddNetModule(true));
      case SELECTIONS_TO_INACTIVE:
        return (new SetSelectedInactive());
      case PULLDOWN:
        return (new PullDown());
      case CLOSE:
        return (new CloseApp());
      case NEW_TAB: 
        return (new TabOps(appState_, TabOps.TabOption.NEW_TAB));        
      case DROP_THIS_TAB: 
        return (new TabOps(appState_, TabOps.TabOption.DROP_THIS));
      case DROP_ALL_BUT_THIS_TAB: 
        return (new TabOps(appState_, TabOps.TabOption.DROP_ALL_BUT_THIS));
      case RETITLE_TAB: 
          return (new TabOps(appState_, TabOps.TabOption.RETITLE_TAB));   
      case CHANGE_TAB: 
        return (new TabOps(appState_, TabOps.TabOption.CHANGE_TAB));
      case TEMPORAL_INPUT_DERIVE: 
        return (new TemporalInput(TemporalInput.Action.TEMPORAL_INPUT_DERIVE));
      case TEMPORAL_INPUT_DROP_ALL: 
        return (new TemporalInput(TemporalInput.Action.TEMPORAL_INPUT_DROP_ALL));
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
        return (new AddGenomeInstance());
      case ADD_DYNAMIC_GENOME_INSTANCE:
        return (new AddDynamicGenomeInstance());
      case ADD_MODEL_TREE_GROUP_NODE:
        return (new AddGroupNode());
      case EDIT_GROUP_NODE_NAV_MAP:
        return (new EditGroupNodeNavMap());
      case ASSIGN_IMAGE_TO_MODEL:
        return (new ImageOps(ImageOps.ImageAction.ADD));
      case DROP_IMAGE_FOR_MODEL:
        return (new ImageOps(ImageOps.ImageAction.DROP));
      case ASSIGN_IMAGE_TO_GROUPING_NODE:
        return (new ImageOps(ImageOps.ImageAction.GROUPING_NODE_ADD));
      case DROP_IMAGE_FOR_GROUPING_NODE:
        return (new ImageOps(ImageOps.ImageAction.GROUPING_NODE_DROP)); 
      case ASSIGN_MAPPING_IMAGE_TO_GROUPING_NODE:
        return (new ImageOps(ImageOps.ImageAction.GROUPING_NODE_ADD_MAP));
      case DROP_MAPPING_IMAGE_FOR_GROUPING_NODE:
        return (new ImageOps(ImageOps.ImageAction.GROUPING_NODE_DROP_MAP));                       
      case DELETE_GENOME_INSTANCE:
        return (new DeleteGenomeInstance(false));
      case DELETE_GENOME_INSTANCE_KIDS_ONLY:
        return (new DeleteGenomeInstance(true));
      case EDIT_MODEL_PROPERTIES:
        return (new EditModelProps());
      case COPY_GENOME_INSTANCE:
        return (new CopyGenomeInstance());
      case MOVE_MODEL_NODEUP:
        return (new MoveModelNode(false));
      case MOVE_MODEL_NODE_DOWN:
        return (new MoveModelNode(true)); 
      case MAKE_STARTUP_VIEW:
        return (new SettingOps(SettingOps.SettingAction.STARTUP_VIEW));
      case SET_CURRENT_OVERLAY_FOR_FIRST_VIEW:
        return (new SettingOps(SettingOps.SettingAction.OVERLAY_FIRST));
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
        return (new RemoveSelections());
      case CANCEL_MODE:
        return (new CancelAdd());
      case NUDGE_UP:
        return (new Mover(Mover.Action.NUDGE_UP));  
      case NUDGE_DOWN:
        return (new Mover(Mover.Action.NUDGE_DOWN));    
      case NUDGE_LEFT:
        return (new Mover(Mover.Action.NUDGE_LEFT));    
      case NUDGE_RIGHT:
        return (new Mover(Mover.Action.NUDGE_RIGHT));    
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
        return (new Selection(Selection.SelectAction.REMOTE_BATCH_SELECT));
      case LOCAL_CLICK_SELECTION:
        return (new Selection(Selection.SelectAction.LOCAL_SINGLE_CLICK_SELECT)); 
      case LOCAL_RECT_SELECTION:
        return (new Selection(Selection.SelectAction.LOCAL_SELECTION_BY_RECT));       
      case MODEL_SELECTION_FROM_TREE: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.VIA_MODEL_TREE));
      case MODEL_SELECTION_FROM_SLIDER:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.VIA_SLIDER));
      case MOVE_ELEMENTS:
        return (new Mover(Mover.Action.MOVE_MODEL_ELEMS));
      case MODEL_SELECTION: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_SELECTION_ONLY));
      case GROUP_NODE_SELECTION: 
          return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.GROUP_NODE_SELECTION_ONLY));
      case MODEL_AND_NODE_SELECTION: 
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_NODE_SELECTIONS));
      case MODEL_AND_LINK_SELECTION:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_LINK_SELECTIONS));
      case MODEL_AND_NODE_LINK_SELECTION:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_AND_SELECTIONS));
      case PATH_MODEL_GENERATION:
        return (new PathGenerator());
      case TARDY_LINK_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.PENDING_LINK)); 
      case TARDY_NODE_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.PENDING_NODE)); 
      case GROUP_NODE_CLICK:
        return (new SetCurrentModel(appState_, SetCurrentModel.SettingAction.MODEL_SELECTION_ONLY));
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
        return (new EditNetOverlay(true));
      case DELETE_CURRENT_OVERLAY:
        return (new RemoveOverlay(true));
      case EDIT_NOTE:
        return (new EditNote(true));
      case DELETE_NOTE:
        return (new RemoveNote());       
      case DELETE_LINK_POINT:
        return (new RemoveLinkPoint());
      case TOGGLE:
        return (new Toggle(Toggle.ToggleAction.GROUP_TOGGLE));
      case ZOOM_TO_GROUP:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_TO_GROUP));
      case GROUP_PROPERTIES:
        return (new EditGroupProperties((EditGroupProperties.GroupArg)args));
      case INCLUDE_ALL_FOR_GROUP:
        return (new IncludeAllForGroup());
      case RAISE_GROUP:
        return (new ReorderGroups(ReorderGroups.Direction.RAISE));
      case LOWER_GROUP:
        return (new ReorderGroups(ReorderGroups.Direction.LOWER));       
      case MOVE_GROUP:
        return (new Mover(Mover.Action.GROUP));
      case LAYOUT_GROUP:
        return (new SpecialtyLayoutFlow(new StackedBlockLayout(new StaticDataAccessContext(appState_)), SpecialtyLayoutFlow.LAYOUT_REGION));  
      case COMPRESS_GROUP:
        return (new SquashExpandGenome(true, true));          
      case EXPAND_GROUP:
        return (new SquashExpandGenome(false, true));
      case COPY_REGION:
        return (new DupGroups(true)); 
      case GROUP_DELETE:
        return (new RemoveGroup());
      case REGION_MAP:
        return (new TimeCourse(TimeCourse.Action.EDIT_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case DELETE_REGION_MAP:
        return (new TimeCourse(TimeCourse.Action.DELETE_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case INPUT_REGION_MAP:
        return (new TemporalInput(TemporalInput.Action.EDIT_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case DELETE_INPUT_REGION_MAP:
        return (new TemporalInput(TemporalInput.Action.DELETE_REGION_MAP, (EditGroupProperties.GroupArg)args));
      case CREATE_SUB_GROUP:
        return (new AddSubGroup(AddSubGroup.Action.CREATE));
      case LINK_PROPERTIES:
        return (new EditLinkProperties()); 
      case SPECIAL_LINE:
        return (new SpecialLineProps());  
      case INSERT_GENE_IN_LINK:
        return (new InsertNodeInLink(true));  
      case INSERT_NODE_IN_LINK:
        return (new InsertNodeInLink(false)); 
      case CHANGE_SOURCE_NODE:
        return (new ChangeNode(false));  
      case CHANGE_TARGET_NODE:
        return (new ChangeNode(true));
      case MERGE_LINKS:
        return (new MergeDuplicateLinks());
      case DIVIDE:
        return (new Divide(false));
      case DIVIDE_MOD:
        return (new Divide(true));        
      case RELOCATE_SEGMENT:
        return (new RelocateSeg(false));
      case RELOCATE_SEGMENT_MOD:
        return (new RelocateSeg(true));        
      case RELOCATE_SOURCE_PAD:
        return (new ChangePad(false, false, false));
      case RELOCATE_SOURCE_PAD_MOD:
        return (new ChangePad(false, true, false));        
      case RELOCATE_TARGET_PAD:
        return (new ChangePad(true, false, false));
      case RELOCATE_TARGET_PAD_MOD:
        return (new ChangePad(true, true, false));
      case CHANGE_TARGET_GENE_MODULE:
        return (new ChangePad(true, false, true));  
      case SWAP_PADS:
        return (new SwapPads());  
      case DELETE_LINKAGE:
        return (new RemoveLinkage()); 
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT:
        return (new LayoutNonOrthoLink(true, false, true, false));
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SHIFT_MOD:
        return (new LayoutNonOrthoLink(true, false, true, true));
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT:
        return (new LayoutNonOrthoLink(true, true, true, false));        
      case FIX_ALL_NON_ORTHO_SEGMENTS_MIN_SPLIT_MOD:
        return (new LayoutNonOrthoLink(true, true, true, true));
      case FIX_NON_ORTHO_MIN_SHIFT:
        return (new LayoutNonOrthoLink(false, false, true, false));
      case FIX_NON_ORTHO_MIN_SHIFT_MOD:
        return (new LayoutNonOrthoLink(false, false, true, true));
      case FIX_NON_ORTHO_MIN_SPLIT:
        return (new LayoutNonOrthoLink(false, true, true, false));
      case FIX_NON_ORTHO_MIN_SPLIT_MOD:
        return (new LayoutNonOrthoLink(false, true, true, true));
      case TOPO_REPAIR:
        return (new LinkRepair(false, true));
      case TOPO_REPAIR_MOD:
        return (new LinkRepair(true, true));
      case SEG_LAYOUT:
        return (new RelayoutLinks(false, true));
      case SEG_OPTIMIZE:
        return (new OptimizeLink(true, false));
      case MODULE_LINK_PROPERTIES:
        return (new EditNetModuleLink()); 
      case DELETE_MODULE_LINKAGE:
        return (new RemoveModuleLinkOrPoint(false));         
      case DISPLAY_LINK_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.LINK));
      case FULL_SELECTION:
        return (new Selection(Selection.SelectAction.SELECT_FULL_LINK));       
      case ANALYZE_PATHS:
      case ANALYZE_PATHS_WITH_QPCR:
        return (new DisplayPaths(DisplayPaths.InfoType.LINK_PATHS)); 
      case ANALYZE_PATHS_FROM_USER_SELECTED:
        return (new DisplayPathsUserSource(true));               
      case LINK_WORKSHEET:
        return (new LaunchBuilder(true, (BuilderPluginArg)args));       
      case SELECT_LINK_SOURCE:
        return (new NodeSrcTargSearch(true));
      case SELECT_LINK_TARGETS:
        return (new NodeSrcTargSearch(false));
      case LINK_USAGES:
        return (new DisplayData(appState_, DisplayData.InfoType.LINK_USAGES));
      case SELECT_SOURCES:
        return (new NodeSrcTargSearch(false, true));
      case SELECT_SOURCES_GENE_ONLY:
        return (new NodeSrcTargSearch(true, true));
      case SELECT_TARGETS:
        return (new NodeSrcTargSearch(false, false));
      case SELECT_TARGETS_GENE_ONLY:
        return (new NodeSrcTargSearch(true, false));
      case SELECT_LINKS_TOGGLE:
        return (new Toggle(Toggle.ToggleAction.SELECT_LINKS));
      case SELECT_QUERY_NODE_TOGGLE:
        return (new Toggle(Toggle.ToggleAction.SELECT_QUERY_NODE));
      case APPEND_TO_CURRENT_SELECTION_TOGGLE:
        return (new Toggle(Toggle.ToggleAction.APPEND_SELECT));
      case NODE_USAGES:
        return (new DisplayData(appState_, DisplayData.InfoType.NODE_USAGES));
      case SIMULATION_PROPERTIES:
        return (new EditSimProperties());
      case NODE_SUPER_ADD:
        return (new SuperAdd());
      case NODE_TYPE_CHANGE:
        return (new ChangeNodeType());
      case CHANGE_NODE_GROUP_MEMBERSHIP:
        return (new ChangeNodeGroup());
      case DELETE_NODE:
        return (new RemoveNode());
      case EDIT_PERT:
        return (new PerturbData(PerturbData.InfoType.EDIT));       
      case DELETE_PERT:
        return (new PerturbData(PerturbData.InfoType.DELETE));        
      case EDIT_PERT_MAP:
        return (new PerturbData(PerturbData.InfoType.PERT_MAP));        
      case DELETE_PERT_MAP:
        return (new PerturbData(PerturbData.InfoType.DELETE_PERT_MAP));
      case EDIT_TIME_COURSE:
        return (new TimeCourse(TimeCourse.Action.EDIT));       
      case DELETE_TIME_COURSE:
        return (new TimeCourse(TimeCourse.Action.DELETE));        
      case EDIT_PERTURBED_TIME_COURSE:
        return (new TimeCourse(TimeCourse.Action.EDIT_PERT));
      case EDIT_TIME_COURSE_MAP:
        return (new TimeCourse(TimeCourse.Action.EDIT_MAP));       
      case DELETE_TIME_COURSE_MAP:
        return (new TimeCourse(TimeCourse.Action.DELETE_MAP));
      case EDIT_TEMPORAL_INPUT:
        return (new TemporalInput(TemporalInput.Action.EDIT));
      case DELETE_TEMPORAL_INPUT:
        return (new TemporalInput(TemporalInput.Action.DELETE));
      case EDIT_TEMPORAL_INPUT_MAP:
        return (new TemporalInput(TemporalInput.Action.EDIT_MAP));
      case DELETE_TEMPORAL_INPUT_MAP:
        return (new TemporalInput(TemporalInput.Action.DELETE_MAP)); 
      case DISPLAY_DATA:
        return (new DisplayData(appState_, DisplayData.InfoType.NODE)); 
      case GENE_PROPERTIES:
        return (new EditNodeProperties(true)); 
      case NODE_PROPERTIES:
        return (new EditNodeProperties(false));         
      case EDIT_MULTI_SELECTIONS:
        return (new MultiSelectionProperties());
      case MERGE_NODES:
        return (new MergeDuplicateNodes());
      case DEFINE_CIS_REG_MODULE:
        return (new AddCisRegModule());    
      case EDIT_CIS_REG_MODULE:
        return (new EditCisRegModule());    
      case ZOOM_TO_NET_MODULE:
        return (new Zoom(appState_, Zoom.ZoomAction.ZOOM_MOD_POPUP));
      case TOGGLE_NET_MODULE_CONTENT_DISPLAY:
        return (new Toggle(Toggle.ToggleAction.NET_MOD_CONTENT));
      case SET_AS_SINGLE_CURRENT_NET_MODULE:
        return (new Modules(Modules.ModAction.SET_AS_SINGLE));
      case DROP_FROM_CURRENT_NET_MODULES:
        return (new Modules(Modules.ModAction.DROP_FROM_CURRENT));
      case EDIT_SELECTED_NETWORK_MODULE:
        return (new EditNetModule()); 
      case ADD_REGION_TO_NET_MODULE:
        return (new AddNetModule(false));
      case DETACH_MODULE_FROM_GROUP:
        return (new RemoveModuleOrPart(RemoveModuleOrPart.ModAction.DETACH_FROM_GROUP, null));
      case REMOVE_THIS_NETWORK_MODULE:
        return (new RemoveModuleOrPart(RemoveModuleOrPart.ModAction.REMOVE_ALL, null));
      case EDIT_MODULE_MEMBERS:
        return (new EditNetModuleMembers());
      case SIZE_CORE_TO_REGION_BOUNDS:
        return (new ResizeModuleCore());
      case DROP_NET_MODULE_REGION:
        return (new RemoveModuleOrPart(RemoveModuleOrPart.ModAction.REMOVE_REGION, (RemoveModuleOrPart.HardwiredEnabledArgs)args));
      case MOVE_NET_MODULE_REGION:
        return (new Mover(Mover.Action.MODULES, (Mover.MoveNetModuleRegionArgs)args));
      case DELETE_MODULE_LINK_POINT:
        return (new RemoveModuleLinkOrPoint(true));
      case DELETE_FROM_MODULE:
        return (new RemoveNodeFromMod((AddNodeToModule.NamedModuleArgs)args));
      case ADD_TO_MODULE:
        return (new AddNodeToModule((AddNodeToModule.NamedModuleArgs)args));
      case ANALYZE_PATHS_FOR_NODE:
        return (new DisplayPaths(DisplayPaths.InfoType.PATHS_FOR_NODE, (DisplayPaths.PathAnalysisArgs)args));
      case PERTURB_PATH_COMPARE:
        return (new DisplayPaths(DisplayPaths.InfoType.PATHS_FOR_NODE, (DisplayPaths.PathAnalysisArgs)args));       
      case DELETE_SUB_GROUP:
        return (new RemoveSubGroup((RemoveSubGroup.SubGroupArgs)args));
      case ADD_NODE_TO_SUB_GROUP:
        return (new AddNodeToSubGroup((AddNodeToSubGroup.NodeInGroupArgs)args)); 
      case DELETE_NODE_FROM_SUB_GROUP:
        return (new RemoveNodeFromSubGroup((AddNodeToSubGroup.NodeInGroupArgs)args)); 
      case INCLUDE_SUB_GROUP:
        return (new AddSubGroup(AddSubGroup.Action.INCLUDE,  (AddSubGroup.GroupPairArgs)args));
      case ACTIVATE_SUB_GROUP:
        return (new AddSubGroup(AddSubGroup.Action.ACTIVATE, (AddSubGroup.GroupPairArgs)args));         
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
