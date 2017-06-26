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

package org.systemsbiology.biotapestry.genome;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.app.ExpansionChange;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.VirtualModelTree;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.util.ModelNodeIDPair;
import org.systemsbiology.biotapestry.util.NodeRegionModelNameTuple;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.analysis.TreeEditor; 
import org.systemsbiology.biotapestry.cmd.flow.modelTree.DeleteGenomeInstance;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.cmd.instruct.GeneralBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.cmd.instruct.InstructionRegions;
import org.systemsbiology.biotapestry.cmd.instruct.LoneNodeBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.SignalBuildInstruction;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.CSVParser;

/****************************************************************************
**
** This handles reading the entire model hierarchy from a CSV file
*/

public class FullHierarchyCSVFormatFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int BAD_VALUES_         = -1;  
  private static final int MODEL_DEFINITION_   =  0;  
  private static final int REGION_DEFINITION_  =  1;    
  private static final int BUILD_INSTRUCTION_  =  2;
    
  private static final String MODEL_DEFINITION_STR_ = "model";
  private static final String REGION_DEFINITION_STR_ = "region"; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final String TOO_FEW_TOKENS                      = "HCSV_TOO_FEW_TOKENS";
  public static final String INCORRECT_INSTRUCTION_DEFINITION    = "HCSV_INCORRECT_INSTRUCTION_DEFINITION";
  public static final String DUPLICATE_MODEL_NAME                = "HCSV_DUPLICATE_MODEL_NAME";
  public static final String TYPE_MISMATCH                       = "HCSV_TYPE_MISMATCH";   
  public static final String INCONSISTENT_REGIONS                = "HCSV_INCONSISTENT_REGIONS";
  public static final String MODELS_NOT_A_TREE                   = "HCSV_MODELS_NOT_A_TREE";
  public static final String BLANK_TOKEN                         = "HCSV_BLANK_TOKEN";
  public static final String UNDEFINED_MODEL_NAME                = "HCSV_UNDEFINED_MODEL_NAME";
  public static final String BAD_REGION_DEF                      = "HCSV_BAD_REGION_DEF";
  public static final String BAD_REGION_HIERARCHY                = "HCSV_BAD_REGION_HIERARCHY";
  public static final String ROOT_MODEL_HAS_REGIONS              = "HCSV_ROOT_MODEL_HAS_REGIONS";
  
  // FIX ME V3.1 public static final String ROOT_LINKAGE_HAS_ACTIVITY           = "HCSV_ROOT_LINKAGE_HAS_ACTIVITY";
  public static final String INCONSISTENT_LINK_EVIDENCE          = "HCSV_INCONSISTENT_LINK_EVIDENCE";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  private FullHierarchyBuilder fhb_;
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

  public FullHierarchyCSVFormatFactory(UIComponentSource uics, TabSource tSrc, DataAccessContext dacx) {
    uics_ = uics;
    dacx_ = dacx;
    tSrc_ = tSrc;
    fhb_ = new FullHierarchyBuilder(dacx_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Build the whole thing from a csv file.  This is the stuff on the foreground thread
  ** that is interacting with swing (to rebuild model tree).
  */

  public FullHierarchyBuilder.BIPData buildFromCSVForeground(File infile, 
                                                            boolean doReplacement,
                                                            UndoSupport support) throws IOException, 
                                                                                        InvalidInputException {
    //
    // Read in the lines.
    //
    
    HashMap<String, List<BuildInstruction>> commands = new HashMap<String, List<BuildInstruction>>();
    HashMap<String, FullHierarchyBuilder.ModelDef> modelDefs = new HashMap<String, FullHierarchyBuilder.ModelDef>();
    HashMap<String, List<InstanceInstructionSet.RegionInfo>> regionDefs = new HashMap<String, List<InstanceInstructionSet.RegionInfo>>();
    ArrayList<String> modelInputOrder = new ArrayList<String>();
 
    readCSV(infile, commands, modelDefs, regionDefs, modelInputOrder);
    return (buildFromCSVForegroundGuts(commands, modelDefs, regionDefs, modelInputOrder, doReplacement, null, support));
  }
  
  /***************************************************************************
  ** 
  ** Build the whole thing from a csv input stream.  This is the stuff on the foreground thread
  ** that is interacting with swing (to rebuild model tree).
  */

  public FullHierarchyBuilder.BIPData buildFromCSVForegroundStream(InputStream stream, 
                                                                   boolean doReplacement, Map<String, String> modelIDMap,
                                                                   UndoSupport support) throws IOException, 
                                                                                        InvalidInputException {
    //
    // Read in the lines.
    //
    
    
    HashMap<String, List<BuildInstruction>> commands = new HashMap<String, List<BuildInstruction>>();
    HashMap<String, FullHierarchyBuilder.ModelDef> modelDefs = new HashMap<String, FullHierarchyBuilder.ModelDef>();
    HashMap<String, List<InstanceInstructionSet.RegionInfo>> regionDefs = new HashMap<String, List<InstanceInstructionSet.RegionInfo>>();
    ArrayList<String> modelInputOrder = new ArrayList<String>();

    readCSV(stream, commands, modelDefs, regionDefs, modelInputOrder);
    return (buildFromCSVForegroundGuts(commands, modelDefs, regionDefs, modelInputOrder, 
                                       doReplacement, modelIDMap, support));
  }  
  
  /***************************************************************************
  ** 
  ** Build the whole thing from a csv file. This is the stuff on the background thread.
  */

  public LinkRouter.RoutingResult buildFromCSVBackground(UIComponentSource uics,
                                                         TabSource tSrc, 
                                                         UndoFactory uFac, 
                                                         StaticDataAccessContext rcxR,
                                                         FullHierarchyBuilder.BIPData bipd,                                                         
                                                         boolean doReplacement,
                                                         UndoSupport support, boolean doOpts, 
                                                         boolean doSquash, int overlayOption, 
                                                         Map<NodeRegionModelNameTuple, ModelNodeIDPair> nodeIDMap, String dbGenomeCSVName,
                                                         BTProgressMonitor monitor, double startFrac, double endFrac, 
                                                         SpecialtyLayout specLayout,
                                                         SpecialtyLayoutEngineParams params)
                                                         throws AsynchExitRequestException {
  
    
    Point2D center = rcxR.getWorkspaceSource().getWorkspace().getCanvasCenter();
    Dimension size = rcxR.getWorkspaceSource().getWorkspace().getCanvasSize();
    
    LayoutOptions options = new LayoutOptions(rcxR.getLayoutOptMgr().getLayoutOptions());
    options.optimizationPasses = (doOpts) ? 1 : 0;
    options.inheritanceSquash = (doSquash) ? LayoutOptions.DO_INHERITANCE_SQUASH 
                                           : LayoutOptions.NO_INHERITANCE_SQUASH;
    options.overlayOption = overlayOption;
    
    BuildInstructionProcessor bip = new BuildInstructionProcessor(uics, rcxR, tSrc, uFac);
    BuildInstructionProcessor.PIHData pihd = new BuildInstructionProcessor.PIHData(bipd.processOrder, bipd.buildCmds, 
                                                                                   bipd.regions, center, 
                                                                                   size, !doReplacement, 
                                                                                   options, nodeIDMap, dbGenomeCSVName, null, false, support, 
                                                                                   monitor, startFrac, endFrac, specLayout, params);
    bip.installPIHData(pihd);
    LinkRouter.RoutingResult retval = bip.processInstructionsForFullHierarchy(rcxR);
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Handles region definition instructions
  */
    
  public static class RegionDef {

    String modelName;
    InstanceInstructionSet.RegionInfo ri;
    
    RegionDef(String model, InstanceInstructionSet.RegionInfo info) {
      modelName = model;
      ri = info;
    }
    
    @Override
    public String toString() {
      return ("RegionDef: model = " + modelName + " ri = " + ri); 
    }    
  }  
   
  /***************************************************************************
  **
  ** Handles model definition instructions
  */ 
    
  public static class AugmentedInstruction {

    String modelName;
    BuildInstruction bi;
    
    AugmentedInstruction(String name, BuildInstruction bi) {
      this.modelName = name;
      this.bi = bi;
    }
    
    @Override
    public String toString() {
      return ("AugmentedInstruction: modelName = " + modelName + " instr = " + bi); 
    }    
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Build the whole thing from a csv file.  This is the stuff on the foreground thread
  ** that is interacting with swing (to rebuild model tree).
  */

  private FullHierarchyBuilder.BIPData buildFromCSVForegroundGuts(HashMap<String, List<BuildInstruction>> commands,
                                                                  HashMap<String, FullHierarchyBuilder.ModelDef> modelDefs,
                                                                  HashMap<String, List<InstanceInstructionSet.RegionInfo>> regionDefs,
                                                                  ArrayList<String> modelInputOrder,
                                                                  boolean doReplacement, Map<String, String> modelIDMap,
                                                                  UndoSupport support) throws IOException, 
                                                                                              InvalidInputException {
    //
    // Collect up all the needed tree objects:
    //
    
    NavTree nt = dacx_.getGenomeSource().getModelHierarchy();
    VirtualModelTree vmtree = uics_.getTree();
    DefaultTreeModel dtm = vmtree.getTreeModel();
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot();    
    
    //
    // Build all the models.  First organize them into build order:
    //
    
    HashMap<String, String> nameToId = new HashMap<String, String>();
    ArrayList<String> bottomUp = new ArrayList<String>();
    ArrayList<String> topDown = new ArrayList<String>();
    String rootName = fhb_.orderModelDefinitions(modelDefs, modelInputOrder, bottomUp, topDown);
    //
    // With full replacement, we just add nodes to an already cleared model
    // set.  Else, we need to also delete missing models first.
    //
    
    ArrayList<String> addedNodes = new ArrayList<String>();
    ArrayList<NavTreeChange> ntcs = new ArrayList<NavTreeChange>();

    ExpansionChange ec = null;
    if (!doReplacement) {
      
      ArrayList<String> removedNodes = new ArrayList<String>();
      if (!compareTreesForReplacement(rootName, modelDefs,  
                                      nameToId, addedNodes, removedNodes)) {
        ResourceManager rMan = dacx_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                      rMan.getString("cSVHierarchy.duplicateModelNameAbort"),
                                      rMan.getString("cSVHierarchy.cannotContinue"),
                                      JOptionPane.WARNING_MESSAGE);                                        
          return (null);                                                                    
      }
      
      //
      // Make sure the existing hierarchy does not include dynamic models, and
      // bail if it does:
      //
      
      Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (gi instanceof DynamicGenomeInstance) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(),
                                        rMan.getString("cSVHierarchy.dynamicInstanceAbort"),
                                        rMan.getString("cSVHierarchy.cannotContinue"),
                                        JOptionPane.WARNING_MESSAGE);
          return (null);
        }
      }
      
      ec = vmtree.doUndoExpansionChange(support, dacx_, true);
         
      //
      // At this point, remove any models that need to be ditched:
      //
       
      int numRem = removedNodes.size();
      Set<String> deadCollection = new HashSet<String>();
      for (int i = 0; i < numRem; i++) {
        String deadOneName = removedNodes.get(i);
        String deadOneID = nameToId.get(deadOneName);
        if (deadCollection.contains(deadOneID)) {
          continue;
        }
        Set<String> deadOnes = DeleteGenomeInstance.deleteGenomeInstance(uics_, dacx_, tSrc_, deadOneID, false, support);
       
        deadCollection.addAll(deadOnes);
        TreeNode tn = nt.nodeForModel(deadOneID);
        nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
        NavTreeChange ntc = nt.deleteNodeAndChildren(tn, true);
        nt.setSkipFlag(NavTree.Skips.NO_FLAG);
        support.addEdit(new NavTreeChangeCmd(dacx_, ntc));
        ntcs.add(ntc);
      }
      if (numRem > 0) {
        nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
        dtm.nodeStructureChanged(rootNode);
        nt.setSkipFlag(NavTree.Skips.NO_FLAG);
      }
      
      //
      // Arrange added nodes top down
      //
      
      ArrayList<String> temp = new ArrayList<String>();
      int numTop = topDown.size();
      for (int i = 0; i < numTop; i++) {
        String nextTop = topDown.get(i);
        if (addedNodes.contains(nextTop)) {
          temp.add(nextTop);
        }
      }
      addedNodes = temp;
      
    } else {  // doing replacement
      int numModels = topDown.size();
      for (int i = 1; i < numModels; i++) {  // skip root
        addedNodes.add(topDown.get(i));
      }
    }
    nameToId.put(rootName, dacx_.getDBGenome().getID());
    int numAdded = addedNodes.size();
    for (int i = 0; i < numAdded; i++) {
      FullHierarchyBuilder.ModelDef md = modelDefs.get(addedNodes.get(i));
      String vfgID = (md.modelParent.equals(rootName)) ? null : nameToId.get(md.modelParent);
      String nextKey = dacx_.getGenomeSource().getNextKey();
      GenomeInstance gi = new GenomeInstance(dacx_, md.modelName, nextKey, vfgID);
      nameToId.put(addedNodes.get(i), nextKey);
      DatabaseChange dc = dacx_.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
      support.addEdit(new DatabaseChangeCmd(dacx_, dc));
      NavTree.Kids nodeType = NavTree.Kids.STATIC_CHILD_INSTANCE;  
      if (vfgID == null) {  // only for instance roots    
        String nextloKey = dacx_.getGenomeSource().getNextKey();
        Layout lo = new Layout(nextloKey, nextKey);
        dc = dacx_.getLayoutSource().addLayout(nextloKey, lo);
        support.addEdit(new DatabaseChangeCmd(dacx_, dc));
        nodeType = NavTree.Kids.ROOT_INSTANCE;
      } 
      TreeNode parNode = nt.nodeForModel(nameToId.get(md.modelParent));     
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
      NavTree.NodeAndChanges nac = nt.addNode(nodeType, md.modelName, parNode, new NavTree.ModelID(nextKey), null, null, dacx_);
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
      support.addEdit(new NavTreeChangeCmd(dacx_, nac.ntc));
      ntcs.add(nac.ntc);
    }

    if (numAdded > 0) {
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
      dtm.nodeStructureChanged(rootNode);
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);
    }
    
    //
    // If the caller wants the map, he gets it:
    //
    
    if (modelIDMap != null) {
      modelIDMap.clear();
      modelIDMap.putAll(nameToId);
    }
   
    //
    // In full replacement mode, there is no undo.  This junk is all handled
    // as part of the standard new model import.  In non-replacement, we have to
    // be able to back out of selection and tree build, so this needs to be
    // done inside this undo transaction:
    //
    
    if (!doReplacement) {
        
      //
      // Expand everybody out
      //
      
      nt.setSkipFlag(NavTree.Skips.SKIP_FINISH);
      List<TreePath> nonleafPaths = nt.getAllPathsToNonLeaves();
      Iterator<TreePath> nlpit = nonleafPaths.iterator();
      while (nlpit.hasNext()) {
        TreePath tp = nlpit.next();
        vmtree.expandTreePath(tp);
      } 
      
      //
      // Have to set selection first, do that the expansion change records the
      // correct selection path
      //
      
      DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
      TreeNode[] tn = rootChild.getPath();
      TreePath tp = new TreePath(tn);
      vmtree.setTreeSelectionPath(tp);
      nt.setSkipFlag(NavTree.Skips.NO_FLAG);      
      
      //
      // Get tree copies from before and after change and fix the maps for expansion
      // xhange.
      
      int ntcSize = ntcs.size();
      if (ntcSize > 0) {
        NavTreeChange ntc = ntcs.get(0);
        ec.expanded = nt.mapAllPaths(ec.expanded, ntc, true);
        ec.selected = nt.mapAPath(ec.selected, ntc, true);
 
        ntc = ntcs.get(ntcSize - 1);
        vmtree.doRedoExpansionChange(support, dacx_, ntc, true);
      }
    }

    //
    // Now we have ids instead of names, change map keys:
    //

    HashMap<String, List<BuildInstruction>> cmdByID = new HashMap<String, List<BuildInstruction>>();
    Iterator<String> mdit = modelDefs.keySet().iterator();    
    while (mdit.hasNext()) {
      String modelName = mdit.next();
      String modelID = nameToId.get(modelName);
      List<BuildInstruction> clist = commands.get(modelName);
      cmdByID.put(modelID, (clist == null) ? new ArrayList<BuildInstruction>() : clist);
    }
    
    HashMap<String, List<InstanceInstructionSet.RegionInfo>> regByID = new HashMap<String, List<InstanceInstructionSet.RegionInfo>>();
    Iterator<String> mrit = modelDefs.keySet().iterator();    
    while (mrit.hasNext()) {
      String modelName = mrit.next();
      if (modelName.equals(rootName)) {
        continue;
      }
      String modelID = nameToId.get(modelName);
      List<InstanceInstructionSet.RegionInfo> rlist = regionDefs.get(modelName);
      regByID.put(modelID, (rlist == null) ? new ArrayList<InstanceInstructionSet.RegionInfo>() : rlist);
    }
    
    ArrayList<String> bottomUpID = new ArrayList<String>();
    int buNum = bottomUp.size();
    for (int i = 0; i < buNum; i++) {
      bottomUpID.add(nameToId.get(bottomUp.get(i)));
    }
    ArrayList<String> topDownID = new ArrayList<String>();
    int tdNum = topDown.size();
    for (int i = 0; i < tdNum; i++) {
      topDownID.add(nameToId.get(topDown.get(i)));
    }
    
    //
    // Build fast equivalence lookup maps:
    //
    
    HashMap<String, Map<BuildInstruction, List<BuildInstruction>>> cmdMapByID = new HashMap<String, Map<BuildInstruction, List<BuildInstruction>>>();
    HashMap<String, Map<BuildInstruction.BIWrapper, List<BuildInstruction>>> cmdWrapMapByID = 
      new HashMap<String, Map<BuildInstruction.BIWrapper, List<BuildInstruction>>>();
    
    fhb_.initBIMaps(cmdByID, cmdMapByID, cmdWrapMapByID);
    
    //
    // Fill in the maps:
    //
    
    Iterator<String> rbiit2 = cmdByID.keySet().iterator();    
    while (rbiit2.hasNext()) {
      String modelID = rbiit2.next();
      List<BuildInstruction> cid = cmdByID.get(modelID);
      int cnum = cid.size();
      for (int i = 0; i < cnum; i++) {
        BuildInstruction bi = cid.get(i);
        fhb_.addInstruction(bi, modelID, null, cmdMapByID);
      } 
    }
 
    //
    // Make sure parents contain links that are referenced in children:
    //
    
    fhb_.populateParentInstances(bottomUpID, cmdByID, cmdMapByID, regByID);    
    
    //
    // Build a unified set of instructions, give a copy to each model:
    //
    
    Map<String, List<BuildInstruction>> uniCmds = fhb_.buildUnifiedCommands(cmdByID, cmdMapByID, cmdWrapMapByID, topDownID);
 
    //
    // Bundle up the stuff used by background thread.  Tool commands can't call
    // dialogs directly, so it gets nulls:
    //

    FullHierarchyBuilder.BIPData retval = new FullHierarchyBuilder.BIPData();
    retval.processOrder = topDownID;
    retval.buildCmds = uniCmds;
    retval.regions = regByID;
    
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Do reading from an input stream:
  */

  private void readCSV(InputStream stream, Map<String, List<BuildInstruction>> commands, 
                       Map<String, FullHierarchyBuilder.ModelDef> modelDefs, 
                       Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, 
                       List<String> modelInputOrder) throws IOException, InvalidInputException {
    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
    readCSV(in, commands, modelDefs, regionDefs, modelInputOrder);
    return;
  }   

  /***************************************************************************
  ** 
  ** Do reading from a file:
  */

  private void readCSV(File infile, Map<String, List<BuildInstruction>> commands, 
                       Map<String, FullHierarchyBuilder.ModelDef> modelDefs, 
                       Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, 
                       List<String> modelInputOrder) throws IOException, InvalidInputException {
    BufferedReader in = new BufferedReader(new FileReader(infile));
    readCSV(in, commands, modelDefs, regionDefs, modelInputOrder);
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Do reading from a buffered reader
  */

  private void readCSV(BufferedReader in, Map<String, List<BuildInstruction>> commands, 
                       Map<String, FullHierarchyBuilder.ModelDef> modelDefs, 
                       Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, 
                       List<String> modelInputOrder) throws IOException, InvalidInputException {
    //
    // Read in the lines.
    //
    
    CSVParser csvp = new CSVParser(true);
      
    String line = null;
    int lineNumber = -1;
    while ((line = in.readLine()) != null) {
      lineNumber++;
      if (line.trim().equals("")) {
        continue;
      }
      List<String> tokens = csvp.processCSVLine(line);
      if (tokens.isEmpty()) {
        continue;
      }
      if ((tokens.get(0)).trim().startsWith("#")) {
        continue;  
      }
      int tokenCategory = getCategory(tokens.get(0));
      try {
        switch (tokenCategory) {
          case MODEL_DEFINITION_:
            FullHierarchyBuilder.ModelDef md = processValuesToModelDefinition(tokens, lineNumber);
            if (modelDefs.get(md.modelName) != null) {
              throw new InvalidInputException(DUPLICATE_MODEL_NAME, lineNumber);
            }
            modelDefs.put(DataUtil.normKey(md.modelName), md);
            modelInputOrder.add(md.modelName);
            break;
          case REGION_DEFINITION_:
            RegionDef rd = processValuesToRegionDefinition(tokens, lineNumber);
            if (modelDefs.get(rd.modelName) == null) {
              throw new InvalidInputException(UNDEFINED_MODEL_NAME, lineNumber);
            }
            List<InstanceInstructionSet.RegionInfo> rList = regionDefs.get(rd.modelName);
            if (!goodRegionDef(rd, rList)) {
              throw new InvalidInputException(BAD_REGION_DEF, lineNumber); 
            }
            if (rList == null) {
              rList = new ArrayList<InstanceInstructionSet.RegionInfo>();
              regionDefs.put(rd.modelName, rList);  // key already normalized
            } 
            rList.add(rd.ri);
            break;          
          case BUILD_INSTRUCTION_:
            AugmentedInstruction ai = processValuesToInstruction(tokens, lineNumber);
            if (modelDefs.get(ai.modelName) == null) {
              throw new InvalidInputException(UNDEFINED_MODEL_NAME, lineNumber);
            }            
            List<BuildInstruction> iList = commands.get(ai.modelName);
            if (iList == null) {
              iList = new ArrayList<BuildInstruction>();
              commands.put(ai.modelName, iList);  // key already normalized
            }
            iList.add(ai.bi);
            break;
          case BAD_VALUES_:
          default:
            in.close();
            throw new IOException();
        }
      } catch (InvalidInputException iaex) {
        in.close();
        throw iaex;
      }
    }
    in.close();
    
 
    
    validateCSV(commands, modelDefs, regionDefs);     

    return;
  }

  /***************************************************************************
  **
  ** Compare tree hierarchies for replacement mode
  */
  
  private boolean compareTreesForReplacement(String newRootName, Map<String, FullHierarchyBuilder.ModelDef> modelDefs,  
                                             Map<String, String> oldNameToId, 
                                             List<String> addedNodes, List<String> removedNodes) {
    TreeEditor te = createHierarchyChecker(newRootName, oldNameToId);
    if (te == null) {
      return (false);
    }
    
    Iterator<String> mdit = modelDefs.keySet().iterator();
    HashSet<Link> linkSet = new HashSet<Link>();
    HashSet<String> nodeSet = new HashSet<String>();
    while (mdit.hasNext()) {
      String key = mdit.next();
      FullHierarchyBuilder.ModelDef md = modelDefs.get(key);
      nodeSet.add(DataUtil.normKey(md.modelName));
      if (md.modelParent != null) {
        linkSet.add(new Link(DataUtil.normKey(md.modelParent), DataUtil.normKey(md.modelName)));
      }
    }
    
    te.getDifferences(nodeSet, linkSet, addedNodes, removedNodes);   
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Process existing models into edit checker.  We return null if we have
  ** duplicate model names -> will not allow replacement to occur.
  */
 
  private TreeEditor createHierarchyChecker(String newRootName, Map<String, String> oldNameToId) {
    HashSet<String> modelNodes = new HashSet<String>();
    HashSet<Link> modelLinks = new HashSet<Link>();
    newRootName = DataUtil.normKey(newRootName);
    modelNodes.add(newRootName);
    Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      String modName = DataUtil.normKey(gi.getName());
      if (modName.equals("") || modelNodes.contains(modName)) {
        return (null);
      }
      modelNodes.add(modName);
      GenomeInstance parent = gi.getVfgParent();
      String parentName = (parent == null) ? newRootName : DataUtil.normKey(parent.getName());
      modelLinks.add(new Link(parentName, modName));
      oldNameToId.put(modName, gi.getID());
    }
    return (new TreeEditor(modelNodes, modelLinks));
  }
  
  /***************************************************************************
  ** 
  ** Figure out token category
  */

  private int getCategory(String tok) {
    if (tok == null) {
      return (BAD_VALUES_);
    } else if (tok.equals(MODEL_DEFINITION_STR_)) {
      return (MODEL_DEFINITION_);
    } else if (tok.equals(GeneralBuildInstruction.CSV_TAG) ||
               tok.equals(LoneNodeBuildInstruction.CSV_TAG) ||            
               tok.equals(SignalBuildInstruction.CSV_TAG)) {
      return (BUILD_INSTRUCTION_);
    } else if (tok.equals(REGION_DEFINITION_STR_)) {
      return (REGION_DEFINITION_);      
    } else {
      return (BAD_VALUES_);
    }
  }

  /***************************************************************************
  **
  ** Process tokens into a model definition
  */
  
  private FullHierarchyBuilder.ModelDef processValuesToModelDefinition(List<String> tokens, int lineNo) throws InvalidInputException {
    if (tokens.size() < 2) {
      throw new InvalidInputException(TOO_FEW_TOKENS, lineNo);
    }
    String modelName = tokens.get(1);
    if (modelName.trim().equals("")) {
      throw new InvalidInputException(BLANK_TOKEN, lineNo);
    }
    String parentName = (tokens.size() == 3) ? tokens.get(2) : null;
    if ((parentName != null) && parentName.trim().equals("")) {
      throw new InvalidInputException(BLANK_TOKEN, lineNo);
    }    
    return (new FullHierarchyBuilder.ModelDef(modelName, (parentName == null) ? null : DataUtil.normKey(parentName)));
  }
  
  /***************************************************************************
  **
  ** Process tokens into a region definition
  */
  
  private RegionDef processValuesToRegionDefinition(List<String> tokens, int lineNo) throws InvalidInputException {
    if (tokens.size() < 4) {
      throw new InvalidInputException(TOO_FEW_TOKENS, lineNo);
    }
    String modelName = tokens.get(1);
    String regionName = tokens.get(2);
    String abbrev = tokens.get(3);

    if (modelName.trim().equals("") ||
        regionName.trim().equals("") ||
        abbrev.trim().equals("")) {
      throw new InvalidInputException(BLANK_TOKEN, lineNo);
    }    
    InstanceInstructionSet.RegionInfo ri = new InstanceInstructionSet.RegionInfo(regionName, abbrev);
    return (new RegionDef(DataUtil.normKey(modelName), ri));
  } 
 
  /***************************************************************************
  **
  ** Process tokens into a Build Instruction
  */
  
  private AugmentedInstruction processValuesToInstruction(List<String> tokens, int lineNo) throws InvalidInputException  {
    if (tokens.size() < 2) {
      throw new InvalidInputException(TOO_FEW_TOKENS, lineNo);
    }    
    String typeStr = tokens.get(0);
    String modelName = tokens.get(1);
    if (modelName.trim().equals("")) {
      throw new InvalidInputException(BLANK_TOKEN, lineNo);
    }
    try {
      BuildInstruction bi = BuildInstruction.buildFromTokens(tokens, typeStr, 2, "");  // use blank ID
      return (new AugmentedInstruction(DataUtil.normKey(modelName), bi));
    } catch (IOException ioex) {
      throw new InvalidInputException(INCORRECT_INSTRUCTION_DEFINITION, lineNo);
    }
  }
  
  /***************************************************************************
  **
  ** Check the CSV for validity errors.  Throws exceptions for invalid input
  */
  
  private void validateCSV(Map<String, List<BuildInstruction>> commands, 
                           Map<String, FullHierarchyBuilder.ModelDef> modelDefs, 
                           Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs) throws InvalidInputException {

    //
    // Model tree must be a tree, with no orphan nodes and a distinguished
    // root.
    //    
    
    if (!modelsFormTree(modelDefs)) {
      throw new InvalidInputException(MODELS_NOT_A_TREE);
    }
    
    //
    // If regions are defined for the root model, we are invalid
    //    
    
    if (!noRegionsForRoot(modelDefs, regionDefs)) {
      throw new InvalidInputException(ROOT_MODEL_HAS_REGIONS);           
    }
    
    //
    // If link activity values are given for root model links, we are invalid
    // FIX ME for V3.1  
    
    //if (activityLevelsForRoot(commands, modelDefs)) {
    //  throw new InvalidInputException(ROOT_LINKAGE_HAS_ACTIVITY);           
    //}    
    
    //
    // If evidence levels for a link are not consistent, we are invalid
    // FIX ME for V3.1  
    
    //if (inconsistentLinkEvidence(commands, modelDefs)) {
    //  throw new InvalidInputException(INCONSISTENT_LINK_EVIDENCE);           
    //}   

    //
    // Region definitions between ancestors must be consistent, though we
    // do not require parents to include regions used in children; we handle
    // that propagation ourselves.  However, the region must be defined at
    // all model levels that issue instructions for those regions.
    //
    
    if (!goodRegionHierarchy(regionDefs, modelDefs)) {
      throw new InvalidInputException(BAD_REGION_HIERARCHY);
    }

    //
    // node types must be consistent in multiple references, regions
    // must be consistent
    //

    HashMap<String, Integer> typeMap = new HashMap<String, Integer>();
    
    Iterator<String> ckit = commands.keySet().iterator();
    while (ckit.hasNext()) {
      String modelName = ckit.next();
      List<BuildInstruction> iList = commands.get(modelName);
      int num = iList.size();
      for (int i = 0; i < num; i++) {
        BuildInstruction bi = iList.get(i);
        if (!bi.typesAreConsistent(typeMap)) {
          // FIX ME!!! Map back to file line number!!!!
          throw new InvalidInputException(TYPE_MISMATCH);  
        }
        if (!regionsAreConsistent(bi, modelName, regionDefs, modelDefs)) {
          throw new InvalidInputException(INCONSISTENT_REGIONS);  
        }
        if (!rootOnlyLinkEvidenceAssignment(bi, modelName, modelDefs)) {
          throw new InvalidInputException(INCONSISTENT_LINK_EVIDENCE);           
        }  
      }
    }
    
    return;
    
  }
 
  /***************************************************************************
  **
  ** If link activity values are given for root model links, we are invalid
  */
  
  //private boolean activityLevelsForRoot(Map commands, Map modelDefs) {
  //  System.out.println("FIX ME: do this check!");
  //  return (false);
  //}    
    
  /***************************************************************************
  **
  ** Rigth now, only general root instructions get evidence levels.
  */
  
  private boolean rootOnlyLinkEvidenceAssignment(BuildInstruction bi, String modelName, 
                                                 Map<String, FullHierarchyBuilder.ModelDef> modelDefs) {
    
    FullHierarchyBuilder.ModelDef md = modelDefs.get(modelName);
    boolean modelIsRoot = (md.modelParent == null);
    
    if (modelIsRoot) {
      return (true);
    }
    
    return (!bi.hasEvidenceLevel());
  }          
  
  /***************************************************************************
  **
  ** Check an instruction for consistent regions
  */
  
  private boolean regionsAreConsistent(BuildInstruction bi, String modelName, 
                                       Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, 
                                       Map<String, FullHierarchyBuilder.ModelDef> modelDefs) {
    //
    // No regions at all is not allowed if there are regions defined for the model.  (This
    // is more strict than dialog-based, but CSV allows those to be defined via the root model
    // anyway).
    //
    // Also, if an instruction references a region, it must be defined for the model, not
    // just created by propagation up from children.
    //
    
    List<InstanceInstructionSet.RegionInfo> rList = regionDefs.get(modelName);
    boolean modelHasRegions = ((rList != null) && !rList.isEmpty());

    InstructionRegions ir = null;
    boolean instructHasRegions = false;
    boolean instructHasTarget = bi.hasTarget();    
    
    if (bi.hasRegions()) {
      instructHasRegions = true;
      ir = bi.getRegions();      
    }
    
    FullHierarchyBuilder.ModelDef md = modelDefs.get(modelName);
    boolean modelNeedsRegions = (md.modelParent != null);    
    
    if (modelHasRegions && !instructHasRegions) {
      return (false);
    }
    if (!modelHasRegions && instructHasRegions) {
      return (false);
    }
    if (!modelHasRegions && modelNeedsRegions) {
      return (false);
    }    
    
    if (!instructHasRegions) {
      return (true);
    }
    
    if (ir.getNumTuples() != 1) {
      return (false);
    }
    
    Iterator<InstructionRegions.RegionTuple> tuit = ir.getRegionTuples();
    InstructionRegions.RegionTuple tup = tuit.next();
    
    boolean haveTarg = false;
    boolean haveSrc = false;
    int numDef = rList.size();
    for (int i = 0; i < numDef; i++) {
      InstanceInstructionSet.RegionInfo ri = rList.get(i);
      if (!haveSrc && DataUtil.keysEqual(ri.abbrev, tup.sourceRegion)) {
        haveSrc = true;
      }
      if (instructHasTarget) {
        if (!haveTarg && DataUtil.keysEqual(ri.abbrev, tup.targetRegion)) {
          haveTarg = true;
        }
      }
      if (haveSrc && (!instructHasTarget || haveTarg)) {
        break;
      }
    }
    
    if (!haveSrc || (instructHasTarget && !haveTarg)) {
      return (false);
    }
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if models form a tree
  */
 
  private boolean modelsFormTree(Map<String, FullHierarchyBuilder.ModelDef> modelDefs) {
    
    Iterator<String> mdit = modelDefs.keySet().iterator();
    HashSet<Link> linkSet = new HashSet<Link>();
    HashSet<String> nodeSet = new HashSet<String>();
    while (mdit.hasNext()) {
      String key = mdit.next();
      FullHierarchyBuilder.ModelDef md = modelDefs.get(key);
      nodeSet.add(DataUtil.normKey(md.modelName));
      if (md.modelParent != null) {
        linkSet.add(new Link(DataUtil.normKey(md.modelParent), DataUtil.normKey(md.modelName)));
      }
    }
    try {
      new TreeEditor(nodeSet, linkSet);
    } catch (IllegalArgumentException iaex) {
      return (false);
    }
    return (true);
  }    
  
  /***************************************************************************
  **
  ** Answer if this is a good region definition
  */
 
  private boolean goodRegionDef(RegionDef rd, List<InstanceInstructionSet.RegionInfo> otherDefs) {
    //
    // The region must be unique, the abbrev must be short and
    // unique.
    //
    
    InstanceInstructionSet.RegionInfo ri = rd.ri; 
    if (ri.abbrev.length() > 3) {
      return (false);
    }
    if (otherDefs == null) {
      return (true);
    }
    int num = otherDefs.size();
    for (int i = 0; i < num; i++) {
      InstanceInstructionSet.RegionInfo chkri = otherDefs.get(i);
      if (DataUtil.keysEqual(ri.abbrev, chkri.abbrev)) {
        return (false);
      }
      if (DataUtil.keysEqual(ri.name, chkri.name)) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Region definitions in child models need to be consistent with definitions
  ** in parent models
  */
 
  private boolean goodRegionHierarchy(Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, 
                                      Map<String, FullHierarchyBuilder.ModelDef> modelDefs) {
    //
    // If a region is defined in both a child and parent model, they
    // need to have consistent naming.  Note that this consistency
    // has to extend across skipped generations also.
    //
    
    Set<String> mdkeys = modelDefs.keySet();
    Iterator<String> mdit1 = mdkeys.iterator();
    Iterator<String> mdit2 = mdkeys.iterator();    
    while (mdit1.hasNext()) {
      String key1 = mdit1.next();
      while (mdit2.hasNext()) {
        String key2 = mdit2.next();
        if (isModelAncestor(modelDefs, key1, key2)) {
          if (!consistentRegionDefs(regionDefs, key1, key2)) {
            return (false);
          }
        }
      } 
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if a model is an ancestor of the other
  */
 
  private boolean isModelAncestor(Map<String, FullHierarchyBuilder.ModelDef> modelDefs, String parent, String child) {
    //
    // If a region is defined in both a child and parent model, they
    // need to have consistent naming.  Note that this consistency
    // has to extend across skipped generations also.
    //
    
    FullHierarchyBuilder.ModelDef cmd = modelDefs.get(child);    
    
    String parKey = cmd.modelParent;
    while (parKey != null) {
      if (DataUtil.keysEqual(parKey, parent)) {
        return (true);
      }
      FullHierarchyBuilder.ModelDef pmd = modelDefs.get(parKey);
      parKey = pmd.modelParent;
    }
 
    return (false);
  }  
 
  /***************************************************************************
  **
  ** Region definitions in child models need to be consistent with definitions
  ** in parent models
  */
 
  private boolean consistentRegionDefs(Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs, String model1Name, String model2Name) {
    //
    // If a region is defined in both a child and parent model, they
    // need to have consistent naming.  Note that this consistency
    // has to extend across skipped generations also.
    //
    
    List<InstanceInstructionSet.RegionInfo> list1 = regionDefs.get(model1Name);
    if ((list1 == null) || list1.isEmpty()) {
      return (true);
    }
    
    List<InstanceInstructionSet.RegionInfo> list2 = regionDefs.get(model2Name);    
    if ((list2 == null) || list2.isEmpty()) {
      return (true);
    }
    
    int num1 = list1.size();
    int num2 = list2.size();
    for (int i = 0; i < num1; i++) {
      InstanceInstructionSet.RegionInfo ri1 = list1.get(i);
      for (int j = 0; j < num2; j++) {
        InstanceInstructionSet.RegionInfo ri2 = list2.get(j);
        if (!ri1.consistent(ri2)) {
          return (false);
        }
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Make sure root definition has no regions defined
  */
 
  private boolean noRegionsForRoot(Map<String, FullHierarchyBuilder.ModelDef> modelDefs,
                                   Map<String, List<InstanceInstructionSet.RegionInfo>> regionDefs) {
    Iterator<String> mdit = modelDefs.keySet().iterator();
    while (mdit.hasNext()) {
      String key = mdit.next();
      FullHierarchyBuilder.ModelDef md = modelDefs.get(key);
      if (md.modelParent == null) {
        List<InstanceInstructionSet.RegionInfo> list = regionDefs.get(md.modelName);
        return ((list == null) || list.isEmpty());
      }
    }
    throw new IllegalStateException();
  }
}
