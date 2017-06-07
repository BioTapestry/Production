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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.InstructionSource;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.cmd.instruct.InstructionRegions;

/****************************************************************************
**
** This is refactored from fullHierarchyCSVFormatFactory
*/

public class FullHierarchyBuilder {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public FullHierarchyBuilder(DataAccessContext dacx) {
    dacx_ = dacx;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Handles model definition instructions
  */ 
    
  public static class BIPData {
    public List<String> processOrder; 
    public Map<String, List<BuildInstruction>> buildCmds;
    public Map<String, List<InstanceInstructionSet.RegionInfo>> regions;
  }
  
  /***************************************************************************
  **
  ** Handles model definition instructions
  */
    
  public static class ModelDef {

    String modelName;
    String modelParent;
    
    ModelDef(String name, String parent) {
      modelName = name;
      modelParent = parent;
    }
    
    public String toString() {
      return ("ModelDef: name = " + modelName + " parent = " + modelParent); 
    }    
  }
  
  /***************************************************************************
  ** 
  ** Init the fast instruction maps:
  */  
  
   public void initBIMaps(Map<String, List<BuildInstruction>> cmdByID, 
                           Map<String, Map<BuildInstruction, List<BuildInstruction>>> cmdMapByID,
                           Map<String, Map<BuildInstruction.BIWrapper, List<BuildInstruction>>> cmdWrapMapByID) {
       
    
    //
    // Want to create a method for fast equivalence lookup. Note that we do not have a set, as
    // there may be multiple copies, so we need to use a map to Lists. This first map considers instructions
    // for links in different regions to be distinct: 
    //
    
    Iterator<String> rbiit = cmdByID.keySet().iterator();    
    while (rbiit.hasNext()) {
      String modelID = rbiit.next();
      Map<BuildInstruction, List<BuildInstruction>> forMod = cmdMapByID.get(modelID);
      if (forMod == null) {
        forMod = new HashMap<BuildInstruction, List<BuildInstruction>>(); 
        cmdMapByID.put(modelID, forMod);
      }
    }
    
    //
    // This map wraps the instruction so that links in different regions are not distinct.
    //
    
    Iterator<String> rbiit2 = cmdByID.keySet().iterator();    
    while (rbiit2.hasNext()) {
      String modelID = rbiit2.next();
      Map<BuildInstruction.BIWrapper, List<BuildInstruction>> forMod = cmdWrapMapByID.get(modelID);
      if (forMod == null) {
        forMod = new HashMap<BuildInstruction.BIWrapper, List<BuildInstruction>>();
        cmdWrapMapByID.put(modelID, forMod);
      }
    }
    
    return;
  }
   
  /***************************************************************************
  ** 
  ** Add instruction to map and lists:
  */  
  
  public void addInstruction(BuildInstruction bi, String modelID,
                             Map<String, List<BuildInstruction>> cmdByID, 
                             Map<String, Map<BuildInstruction, List<BuildInstruction>>> cmdMapByID) {
      
    if (cmdByID != null) {
      List<BuildInstruction> cid = cmdByID.get(modelID);
      cid.add(bi);
    }
    
    Map<BuildInstruction, List<BuildInstruction>> forMod = cmdMapByID.get(modelID);
    List<BuildInstruction> f4bi = forMod.get(bi);
    if (f4bi == null) {
      f4bi = new ArrayList<BuildInstruction>();
      forMod.put(bi, f4bi);
    }
    f4bi.add(bi);

    return;
  } 
 
  /***************************************************************************
  **
  ** Take csv-derived instructions and create the union of all instructions
  */
  
  public List<BuildInstruction> unifyInstructions(Map<String, List<BuildInstruction>> buildCmds, 
                                                  Map<String, Map<BuildInstruction, List<BuildInstruction>>> cmdMapByID) {
    InstructionSource isrc = dacx_.getInstructSrc();
    Iterator<String> bit = buildCmds.keySet().iterator();
    Map<BuildInstruction.BIWrapper, List<BuildInstruction>> biwrmap = new HashMap<BuildInstruction.BIWrapper, List<BuildInstruction>>();
    while (bit.hasNext()) {
      String modelName = bit.next();
      List<BuildInstruction> iList = buildCmds.get(modelName);
      Map<BuildInstruction, List<BuildInstruction>> bimap = cmdMapByID.get(modelName);
      int num = iList.size();
      for (int i = 0; i < num; i++) {
        BuildInstruction bi = iList.get(i);
        BuildInstruction.BIWrapper biw = new BuildInstruction.BIWrapper(bi);
        int need = numberPresentWithSameRegions(bi, bimap);
        int have = numberPresent(biw, biwrmap);
        int diff = need - have;
        for (int j = 0; j < diff; j++) {
          BuildInstruction newBI = bi.clone();
          newBI.setID(isrc.getNextInstructionLabel());
          newBI.setRegions(null);
          List<BuildInstruction> bbiw = biwrmap.get(biw);
          if (bbiw == null) {
            bbiw = new ArrayList<BuildInstruction>();
            biwrmap.put(biw, bbiw);
          }
          bbiw.add(newBI);
        }
      }
    }
    
    ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
      Iterator<BuildInstruction.BIWrapper> wit = biwrmap.keySet().iterator();
      while (wit.hasNext()) {
        BuildInstruction.BIWrapper biw = wit.next();
        List<BuildInstruction> bbiw = biwrmap.get(biw);
        Iterator<BuildInstruction> blit = bbiw.iterator();
        while (blit.hasNext()) {
          BuildInstruction bi = blit.next();
          retval.add(bi);
        }
      }
    
    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Build unified command map
  */

  public Map<String, List<BuildInstruction>> buildUnifiedCommands(Map<String, List<BuildInstruction>> cmdByID, 
                                                                  Map<String, Map<BuildInstruction, List<BuildInstruction>>> cmdMapByID,
                                                                  Map<String, Map<BuildInstruction.BIWrapper, List<BuildInstruction>>> cmdWrapMapByID,
                                                                  List<String> topDownID) {
    
    //
    // Build a unified set of instructions, give a copy to each model:
    //
    
    List<BuildInstruction> unified = unifyInstructions(cmdByID, cmdMapByID);
    int uniNum = unified.size();
    HashMap<String, List<BuildInstruction>> uniCmds = new HashMap<String, List<BuildInstruction>>();   
    Iterator<String> ciit = cmdByID.keySet().iterator();
    while (ciit.hasNext()) {
      String modelID = ciit.next();
      ArrayList<BuildInstruction> cmdList = new ArrayList<BuildInstruction>();
      uniCmds.put(modelID, cmdList);
      Map<BuildInstruction.BIWrapper, List<BuildInstruction>> uniMap = cmdWrapMapByID.get(modelID);
      uniMap.clear();     
      for (int i = 0; i < uniNum; i++) {
        BuildInstruction cmd = unified.get(i);
        BuildInstruction cmdClo = cmd.clone();
        cmdList.add(cmdClo); // <-- This clone means we need to work to keep our map pointing at the same things
        BuildInstruction.BIWrapper biw = new BuildInstruction.BIWrapper(cmd);
        List<BuildInstruction> lbi = uniMap.get(biw);
        if (lbi == null) {
          lbi = new ArrayList<BuildInstruction>();
          uniMap.put(biw, lbi);
        }
        lbi.add(cmdClo);
      }
    }
    
    //
    // Transfer region specs from original lists to unified lists:
    //

    Iterator<String> cit = cmdByID.keySet().iterator();
    while (cit.hasNext()) {
      String modelID = cit.next();
      List<BuildInstruction> origList = cmdByID.get(modelID);
      List<BuildInstruction> uniList = uniCmds.get(modelID);
      Map<BuildInstruction.BIWrapper, List<BuildInstruction>> uniMap = cmdWrapMapByID.get(modelID);
      int olNum = origList.size();
      int ulNum = uniList.size(); 
      for (int j = 0; j < ulNum; j++) {  // we gotta have at least empty regions
        BuildInstruction ubi = uniList.get(j);
        if (!ubi.hasRegions() && !modelID.equals(topDownID.get(0))) { // don't add to root
          ubi.setRegions(new InstructionRegions());
        }
      }
    
      for (int i = 0; i < olNum; i++) {
        BuildInstruction bi = origList.get(i);
        InstructionRegions ir = bi.getRegions();
        if (!bi.hasRegions()) {
          continue;
        }
        BuildInstruction.BIWrapper biw = new BuildInstruction.BIWrapper(bi);
        List<BuildInstruction> lbi = uniMap.get(biw);
        int numLbi = lbi.size();
        Iterator<InstructionRegions.RegionTuple> rtit = ir.getRegionTuples();
        while (rtit.hasNext()) {
          //
          // FIXME???
          // TRUE OR FALSE: Is there NO guarantee that kid gets same instruction as parent?
          //
          InstructionRegions.RegionTuple tup = rtit.next();   
          for (int j = 0; j < numLbi; j++) {
            BuildInstruction ubi = lbi.get(j);
            if (!ubi.sameDefinition(bi)) {
              throw new IllegalArgumentException();
            }
            InstructionRegions uir = ubi.getRegions();
            if (!uir.hasTuple(tup)) {
              uir.addRegionTuple(tup);
              break;
            }
          }
        }
      }
    }
 
    return (uniCmds);
  }
  
  /***************************************************************************
  **
  ** Duplicate src/target region tuples require duplicate instructions
  */
  
  public int numberPresent(BuildInstruction bi, List<BuildInstruction> cmds) {
    int retval = 0;
    int num = cmds.size();
    for (int i = 0; i < num; i++) {
      BuildInstruction chkbi = cmds.get(i);
      if (chkbi.sameDefinition(bi)) {
        retval++;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Duplicate src/target region tuples require duplicate instructions
  */
  
  public int numberPresent(BuildInstruction.BIWrapper biw, Map<BuildInstruction.BIWrapper, List<BuildInstruction>> bcmCmds) {
    if (bcmCmds == null) {
      return (0);
    }
    List<BuildInstruction> cmds = bcmCmds.get(biw);
    return ((cmds == null) ? 0 : cmds.size());
  }

  /***************************************************************************
  **
  ** Duplicate src/target region tuples require duplicate instructions
  */
  
  public int numberPresentWithSameRegions(BuildInstruction bi, List<BuildInstruction> cmds) {
    int retval = 0;
    int num = cmds.size();
    for (int i = 0; i < num; i++) {
      BuildInstruction chkbi = cmds.get(i);
      if (!bi.getID().equals(chkbi.getID())) {
        throw new IllegalArgumentException();  // should all have "" ids
      }
      if (chkbi.equals(bi)) {
        retval++;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Duplicate src/target region tuples require duplicate instructions
  */
  
  public int numberPresentWithSameRegions(BuildInstruction bi, Map<BuildInstruction, List<BuildInstruction>> bcmCmds) {
    if (bcmCmds == null) {
      return (0);
    }
    List<BuildInstruction> cmds = bcmCmds.get(bi);
    return ((cmds == null) ? 0 : cmds.size());
  }
  
  /***************************************************************************
  **
  ** Propagate region instances up from children to parents
  */
  
  public void populateParentInstances(List<String> bottomUp, Map<String, List<BuildInstruction>> buildCmds,
                                                             Map<String, Map<BuildInstruction, List<BuildInstruction>>> bcMap,
                                                             Map<String, List<InstanceInstructionSet.RegionInfo>> regions) {
    GenomeSource gSrc = dacx_.getGenomeSource();
    
    // Ordered list is bottom to top.
    //
    int num = bottomUp.size();
    for (int i = 0; i < num; i++) {
      String id = bottomUp.get(i);
      Genome genome = gSrc.getGenome(id);
      if (!(genome instanceof GenomeInstance)) {
        continue;
      }
      GenomeInstance gi = (GenomeInstance)genome;
      GenomeInstance parent = gi.getVfgParent();
      if (parent == null) {
        continue;
      }

      List<BuildInstruction> cmds = buildCmds.get(id);
      Map<BuildInstruction, List<BuildInstruction>> bcmCmds = bcMap.get(id);
      Map<BuildInstruction, List<BuildInstruction>> pBcmCmds = bcMap.get(parent.getID());
      int nCmds = cmds.size();
      for (int j = 0; j < nCmds; j++) {
        BuildInstruction bi = cmds.get(j);
        int need = numberPresentWithSameRegions(bi, bcmCmds);
        int have = numberPresentWithSameRegions(bi, pBcmCmds);
        int diff = need - have;
        for (int k = 0; k < diff; k++) {
          BuildInstruction newBI = bi.clone();
          addInstruction(newBI, parent.getID(), buildCmds, bcMap);
        }
      }

      List<InstanceInstructionSet.RegionInfo> regs = regions.get(id);
      List<InstanceInstructionSet.RegionInfo> pregs = regions.get(parent.getID());
      int nRegs = regs.size();
      for (int j = 0; j < nRegs; j++) {
        InstanceInstructionSet.RegionInfo ri = regs.get(j);
        if (!pregs.contains(ri)) {
          pregs.add(new InstanceInstructionSet.RegionInfo(ri));
        }
      }
    }
    return;
  }  

 /***************************************************************************
  ** 
  ** Have to order the models to insure that they are built in the correct order
  */

  public String orderModelDefinitions(Map<String, ModelDef> modelDefs, List<String> modelInputOrder, 
                                      List<String> bottomUp, List<String> topDown) {
    String rootName = null;
    ArrayList<Link> linkList = new ArrayList<Link>();
    HashSet<String> nodeSet = new HashSet<String>();
    ArrayList<String> nodeList = new ArrayList<String>();
    
    int numIO = modelInputOrder.size();
    for (int i = 0; i < numIO; i++) {
      String modName = modelInputOrder.get(i); 
      modName = DataUtil.normKey(modName); 
      ModelDef md = modelDefs.get(modName);
      if (!nodeSet.contains(modName)) {
        nodeList.add(modName);
        nodeSet.add(modName);
      }
      if (md.modelParent == null) {
        rootName = md.modelName;       
      } else {
        Link link = new Link(md.modelParent, modName);
        if (!linkList.contains(link)) {
          linkList.add(link);
        }
      }
    }
    
    //
    //
    // Per bug BT-06-26-06:1, we are losing the order of siblings that is
    // specified in the CSV input.  Modify the list creation to preserve
    // This version of the searcher retains ordering of the siblings:
    //
    
    GraphSearcher search = new GraphSearcher(nodeList, linkList);
    List<GraphSearcher.QueueEntry> depthFirst = search.depthSearch();
   
    int num = depthFirst.size();
    for (int i = 0; i < num; i++) {
      topDown.add(depthFirst.get(i).name);
    } 
    for (int i = num - 1; i >= 0; i--) {
      bottomUp.add(depthFirst.get(i).name);
    }
 
    return (DataUtil.normKey(rootName));
  } 
}
