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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Collections;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.db.DatabaseChange;

/****************************************************************************
**
** This tracks duplicate instruction requirements for all models
*/

public class DuplicateInstructionTracker {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
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

  public DuplicateInstructionTracker(BTState appState) {
    appState_ = appState;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Analyze
  */

  public List<CoreData> analyzeAllCores(DataAccessContext dacx, List<DuplicateInstructionTracker.CoreCount> emptyCounts) {

    //
    // Go to the root and make an entry for each core type, tracking all IDs
    // related to a particular instruction core.  Then go through all models,
    // finding out how many copies of each core are needed by that model.
    //
     
    //
    // Build up data structure from original commands:
    //
    
    HashMap<String, BuildInstruction> biMap = new HashMap<String, BuildInstruction>();
    ArrayList<CoreData> cores = new ArrayList<CoreData>(); 
    Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      String bid = bi.getID();
      biMap.put(bid, bi);
      CoreData cd = getDataForCore(bi, cores);
      if (cd == null) {
        cd = new CoreData(bi);
        cores.add(cd);
        CoreCount emptyCount = getCountForCore(bi, emptyCounts);
        cd.emptyRequired = emptyCount.count;
      }
      cd.idCounts.put(bid, new Integer(0));
      cd.emptyForModel = dacx.getGenomeID();
    }

    
    //
    // Crank through each root instance, getting count requirements for each core:
    //
    
    Iterator<GenomeInstance> giit = dacx.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance currGi = giit.next();
      if (currGi.getVfgParent() != null) {
        continue;
      }
      String key = currGi.getID();
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(key);
      if (iis != null) {
        Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
        while (iit.hasNext()) {
          BuildInstructionInstance bii = iit.next();
          String biid = bii.getBaseID();
          BuildInstruction bi = biMap.get(biid);
          CoreData cd = getDataForCore(bi, cores);
          cd.add(bii, key);
        }
      }
    }
    
    //
    // Calculate minimum required core counts and core surpluses:
    //
    
    int cSize = cores.size();
    for (int i = 0; i < cSize; i++) { 
      CoreData cd = cores.get(i);
      cd.calcMinAndSurplus();
    }    
    return (cores);
  }
  
  /***************************************************************************
  ** 
  ** Answer if we have duplicates at all
  */
  
  public boolean haveDuplicateInstructions(List<CoreData> coreAnalysis) {
        
    int cSize = coreAnalysis.size();
    for (int i = 0; i < cSize; i++) { 
      CoreData cd = coreAnalysis.get(i);
      if (cd.isMultiCore()) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Optimize
  */

  public List<BuildInstruction> optimize(List<CoreData> coreAnalysis, UndoSupport support, DataAccessContext dacx) {
     
    if (!haveDuplicateInstructions(coreAnalysis)) {
      return (null);
    }
 
    //
    // Figure out what unused root instructions can be ditched based on optimization
    // strategy.  If the number of surplus instructions > 0, kill of those instances
    // that have the least references.
    //
    
    HashMap<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew = 
      new HashMap<String, Map<BuildInstructionInstance, BuildInstructionInstance>>();
    List<String> removed = removeSurplusInstructions(coreAnalysis, oldToNew);

    compressInstructions(coreAnalysis, oldToNew);    

    //
    // Remove instructions on the surplus list:
    //

    ArrayList<BuildInstruction> newCmds = new ArrayList<BuildInstruction>(); 
    Iterator<BuildInstruction> biit = dacx.getInstructSrc().getBuildInstructions();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      String bid = bi.getID();
      if (!removed.contains(bid)) {
        newCmds.add(bi);
      }
    }
    DatabaseChange dc = dacx.getInstructSrc().setBuildInstructions(newCmds);
    support.addEdit(new DatabaseChangeCmd(appState_, dacx, dc));

    //
    // Use the modified core analysis data to modify the instruction sets
    //

    Iterator<GenomeInstance> giit = dacx.getGenomeSource().getInstanceIterator();
    while (giit.hasNext()) {
      GenomeInstance currGi = giit.next();
      String key = currGi.getID();
      GenomeInstance rootParent = currGi.getVfgParentRoot();
      String parentKey = (rootParent == null) ? key : rootParent.getID();
      Map<BuildInstructionInstance, BuildInstructionInstance> mapForModel = oldToNew.get(parentKey);
      InstanceInstructionSet iis = dacx.getInstructSrc().getInstanceInstructionSet(key);
      if (iis != null) {
        InstanceInstructionSet iisNew = new InstanceInstructionSet(iis);
        iisNew.deleteAllInstructions();
        Iterator<BuildInstructionInstance> iit = iis.getInstructionIterator();
        while (iit.hasNext()) {
          BuildInstructionInstance oldBii = iit.next();
          BuildInstructionInstance newBii = null;
          if (mapForModel != null) {
            newBii = mapForModel.get(oldBii);
          }
          if (newBii == null) {
            iisNew.addInstruction(new BuildInstructionInstance(oldBii));
          } else {
            iisNew.addInstruction(newBii);
          }
        }
        dc = dacx.getInstructSrc().setInstanceInstructionSet(key, iisNew);
        support.addEdit(new DatabaseChangeCmd(appState_, dacx, dc));    
      }
    }    
    return (newCmds);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to hold counts for cores
  */ 
    
  public static class CoreCount {

    public BuildInstruction core;
    public int count;
    
    public CoreCount(BuildInstruction core, int count) {
      this.core = core;
      this.count = count;
    }
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to report on changes
  */ 
    
  public static final class CoreData {
       
    BuildInstruction aCore;
    HashMap<String, Integer> idCounts;
    HashMap<String, Map<InstructionRegions.RegionTuple, Integer>> modelCounts;
    HashMap<String, Set<ModelAndTuple>> globalUsage;
    int emptyRequired;
    int minRequired;
    int surplus;
    String emptyForModel;

    CoreData(BuildInstruction bi) {
      aCore = bi;
      idCounts = new HashMap<String, Integer>();
      modelCounts = new HashMap<String, Map<InstructionRegions.RegionTuple, Integer>>();
      globalUsage = new HashMap<String, Set<ModelAndTuple>>();
    }
    
    boolean isMultiCore() {
      return (idCounts.size() > 1);
    }
    
    void add(BuildInstructionInstance bii, String modelID) {
      // Bump the reference count
      String baseID = bii.getBaseID();
      Integer count = idCounts.get(baseID);
      idCounts.put(baseID, new Integer(count.intValue() + 1));
      // Get the dictionary of tuple counts for the model
      Map<InstructionRegions.RegionTuple, Integer> countsForModel = modelCounts.get(modelID);
      if (countsForModel == null) {
        countsForModel = new HashMap<InstructionRegions.RegionTuple, Integer>();
        modelCounts.put(modelID, countsForModel);
      }
      // Build the tuple, get the count for the tuple and increment it
      InstructionRegions.RegionTuple rTup = 
        new InstructionRegions.RegionTuple(bii.getSourceRegionID(), bii.getTargetRegionID());
      Integer countForTuple = countsForModel.get(rTup);
      int newCount = (countForTuple == null) ? 1 : countForTuple.intValue() + 1;
      countsForModel.put(rTup, new Integer(newCount));
      // Track the global usage of each instance  FIX ME: Can ditch some other calculations above now?
      Set<ModelAndTuple> usage = globalUsage.get(baseID);
      if (usage == null) {
        usage = new HashSet<ModelAndTuple>();
        globalUsage.put(baseID, usage);
      }
      usage.add(new ModelAndTuple(modelID, rTup));
      return;
    }

    int organizeCoresByUsage(SortedMap<Integer, SortedSet<String>> instructPerCount) {
      Iterator<String> idcid = idCounts.keySet().iterator();
      
      int total = 0;
      while (idcid.hasNext()) {
        String id = idcid.next();
        Integer count = idCounts.get(id);
        SortedSet<String> idsForCount = instructPerCount.get(count);
        if (idsForCount == null) {
          idsForCount = new TreeSet<String>();
          instructPerCount.put(count, idsForCount);
        }
        idsForCount.add(id);
        total++;
      }
      return (total);
    }
    
    List<String> flattenUsageList(SortedMap<Integer, SortedSet<String>> instructPerCount) {
      ArrayList<String> retval = new ArrayList<String>();
      Iterator<Integer> ipcid = instructPerCount.keySet().iterator();
      while (ipcid.hasNext()) {
        Integer count = ipcid.next();
        SortedSet<String> idsForCount = instructPerCount.get(count);
        Iterator<String> ifcit = idsForCount.iterator();
        while (ifcit.hasNext()) {
          String key = ifcit.next();
          retval.add(key);
        }
      }
      return (retval);
    }
    
    List<String> getLeastUsed(int num) {
      TreeMap<Integer, SortedSet<String>> instructPerCount = new TreeMap<Integer, SortedSet<String>>();
      int total = organizeCoresByUsage(instructPerCount);
      if (total < num) {
        throw new IllegalArgumentException();
      }
      ArrayList<String> retval = new ArrayList<String>();
      Iterator<Integer> ipcid = instructPerCount.keySet().iterator();
      while (ipcid.hasNext() && (num > 0)) {
        Integer count = ipcid.next();
        SortedSet<String> idsForCount = instructPerCount.get(count);
        int ifcSize = idsForCount.size();
        if (ifcSize < num) {
          retval.addAll(idsForCount);
          num -= ifcSize;
        } else {
          Iterator<String> ifcid = idsForCount.iterator();
          while (ifcid.hasNext()) {
            retval.add(ifcid.next());
            num--;
            if (num == 0) {
              break;
            }
          }
        }
      }
      return (retval);    
    }    
    
    void calcMinAndSurplus() {
      minRequired = 0;
      int minForEmptyModel = 0;
      Iterator<String> mit = modelCounts.keySet().iterator();
      while (mit.hasNext()) {
        String key = mit.next();
        Map<InstructionRegions.RegionTuple, Integer> countsForModel = modelCounts.get(key);
        Iterator<InstructionRegions.RegionTuple> cit = countsForModel.keySet().iterator();
        while (cit.hasNext()) {
          InstructionRegions.RegionTuple tup = cit.next();
          Integer count = countsForModel.get(tup);
          int countVal = count.intValue();
          if (countVal > minRequired) {
            minRequired = countVal;
          }
          if (key.equals(emptyForModel) && (countVal > minForEmptyModel)) {
            minForEmptyModel = countVal;
          }
        }  
      }
      minForEmptyModel += emptyRequired; 
      if (minRequired < minForEmptyModel) { 
        minRequired = minForEmptyModel;
      }
      surplus = idCounts.size() - minRequired;
      return;
    }
    
    void dropSurplusCores(List<String> deadList) {
      Iterator<String> dlit = deadList.iterator();
      while (dlit.hasNext()) {
        String key = dlit.next();
        idCounts.remove(key);
        globalUsage.remove(key);
      }
      return;
    }

    private void processUsageSetToOrphans(String key, Set<ModelAndTuple> usage, 
                                          Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
      if (usage == null) {
        return;
      }
      Iterator<ModelAndTuple> uit = usage.iterator();
      while (uit.hasNext()) {
        ModelAndTuple mat = uit.next();
        HashMap<BuildInstructionInstance, BuildInstructionInstance> mapForModel = 
          (HashMap<BuildInstructionInstance, BuildInstructionInstance>)oldToNew.get(mat.modelID);
        if (mapForModel == null) {
          mapForModel = new HashMap<BuildInstructionInstance, BuildInstructionInstance>();
          oldToNew.put(mat.modelID, mapForModel);
        }
        BuildInstructionInstance oldBii = 
          new BuildInstructionInstance(key, mat.tuple.sourceRegion, mat.tuple.targetRegion);
        // ID not assigned yet!
        BuildInstructionInstance newBii = 
          new BuildInstructionInstance(null, mat.tuple.sourceRegion, mat.tuple.targetRegion);
        //// NOTE USE OF BII HASH CODE HERE......
        mapForModel.put(oldBii, newBii);
      }
      return;
    }
    
    private void assignNewID(String oldID, String newID, ModelAndTuple mat, 
                             Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
      HashMap<BuildInstructionInstance, BuildInstructionInstance> mapForModel = 
        (HashMap<BuildInstructionInstance, BuildInstructionInstance>)oldToNew.get(mat.modelID);
      if (mapForModel == null) {
        throw new IllegalStateException();
      }
      BuildInstructionInstance oldBii = 
        new BuildInstructionInstance(oldID, mat.tuple.sourceRegion, mat.tuple.targetRegion);
      //// NOTE USE OF BII HASH CODE HERE......
      BuildInstructionInstance newBii = mapForModel.get(oldBii);
      newBii.setBaseID(newID);
      return;
    }    
 
    void reallocateOrphanedInstructions(List<String> deadList, 
                                        Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {

      //
      // Collect up the ModelAndTuples orphaned by the loss of surplus instructions
      //

      Iterator<String> dlit = deadList.iterator();
      while (dlit.hasNext()) {
        String key = dlit.next();
        Set<ModelAndTuple> usage = globalUsage.get(key);
        processUsageSetToOrphans(key, usage, oldToNew);
      }  

      //
      // Kill off the records of surplus instructions:
      //
      
      dropSurplusCores(deadList);      
      
      //
      // Add on the MATs residing in the instructions that are to be blank:
      //
      
      TreeMap<Integer, SortedSet<String>> instructPerCount = new TreeMap<Integer, SortedSet<String>>();        
      organizeCoresByUsage(instructPerCount);
      List<String> orderedCores = flattenUsageList(instructPerCount);
      for (int i = 0; i < emptyRequired; i++) {
        String key = orderedCores.get(i);
        Set<ModelAndTuple> usage = globalUsage.get(key);
        processUsageSetToOrphans(key, usage, oldToNew);
        usage.clear();
        idCounts.put(key, new Integer(0));
      }
    
      //
      // Assign these to surviving instructions, using the most heavily populated
      // first:
      //
      
      Iterator<String> otnit = oldToNew.keySet().iterator();
      while (otnit.hasNext()) {
        String modelID = otnit.next();
        Map<BuildInstructionInstance, BuildInstructionInstance> mapForModel = oldToNew.get(modelID);
        Iterator<BuildInstructionInstance> mfmit = mapForModel.keySet().iterator();
        while (mfmit.hasNext()) {
          BuildInstructionInstance oldBii = mfmit.next();
          ModelAndTuple mat = new ModelAndTuple(modelID, oldBii.getSourceRegionID(), oldBii.getTargetRegionID());
          instructPerCount = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());        
          organizeCoresByUsage(instructPerCount);  // Order can change on each iteration!
          orderedCores = flattenUsageList(instructPerCount);
          int ocNum = orderedCores.size();
          for (int i = 0; i < ocNum; i++) {
            String key = orderedCores.get(i);
            Set<ModelAndTuple> usage = globalUsage.get(key);
            if (!usage.contains(mat)) {
              usage.add(mat);
              Integer count = idCounts.get(key);
              idCounts.put(key, new Integer(count.intValue() + 1));
              assignNewID(oldBii.getBaseID(), key, mat, oldToNew);
              break;
            }
          }
        }
      }

      return;
    }
    
    void compressInstructions(Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
      boolean rebuild = false;
      while (true) {
        TreeMap<Integer, SortedSet<String>> instructPerCount = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());        
        organizeCoresByUsage(instructPerCount);  // Order can change on each iteration!
        List<String> orderedCores = flattenUsageList(instructPerCount);
        int ocNum = orderedCores.size();
        for (int i = 0; i < ocNum; i++) {
          String key = orderedCores.get(i);
          List<String> targetCores = new ArrayList<String>();
          for (int j = 0; j < i; j++) {
            targetCores.add(orderedCores.get(j));
          }
          Set<ModelAndTuple> usage = globalUsage.get(key);
          //
          // Once we have a successful transfer, we need to rebuild our usage stats:
          //
          rebuild = compressUsage(key, usage, targetCores, oldToNew);
          if (rebuild) {
            break;
          }
        }
        if (!rebuild) {
          return;
        }
      }
    }    

    private boolean compressUsage(String srcKey, Set<ModelAndTuple> usage, List<String> targetCores, 
                                  Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
      Iterator<ModelAndTuple> uit = usage.iterator();
      while (uit.hasNext()) {
        ModelAndTuple mat = uit.next();
        int tcNum = targetCores.size();
        for (int i = 0; i < tcNum; i++) {
          String targKey = targetCores.get(i);
          Set<ModelAndTuple> targUsage = globalUsage.get(targKey);
          if (!targUsage.contains(mat)) {
            transferUsage(mat, srcKey, usage, targKey, targUsage, oldToNew);
            return (true);
          }
        }
      }
      return (false);
    } 
  
    private void transferUsage(ModelAndTuple mat, String srcKey, Set<ModelAndTuple> usage, 
                               String targKey, Set<ModelAndTuple> targUsage, 
                               Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
      usage.remove(mat);
      targUsage.add(mat);
      Integer sCount = idCounts.get(srcKey);
      idCounts.put(srcKey, new Integer(sCount.intValue() - 1));
      Integer tCount = idCounts.get(targKey);      
      idCounts.put(targKey, new Integer(tCount.intValue() + 1));      
      Map<BuildInstructionInstance, BuildInstructionInstance> mapForModel = oldToNew.get(mat.modelID);
      if (mapForModel == null) {
        mapForModel = new HashMap<BuildInstructionInstance, BuildInstructionInstance>();
        oldToNew.put(mat.modelID, mapForModel);
      }
      BuildInstructionInstance oldBii = 
        new BuildInstructionInstance(srcKey, mat.tuple.sourceRegion, mat.tuple.targetRegion);
      BuildInstructionInstance newBii = 
        new BuildInstructionInstance(targKey, mat.tuple.sourceRegion, mat.tuple.targetRegion);          
      mapForModel.put(oldBii, newBii);
      return;
    }
 
  }
  
  /***************************************************************************
  **
  ** Used to report on changes
  */ 
    
  private static final class ModelAndTuple {
       
    String modelID;
    InstructionRegions.RegionTuple tuple;

    ModelAndTuple(String modelID, InstructionRegions.RegionTuple tuple) {
      this.modelID = modelID;
      this.tuple = new InstructionRegions.RegionTuple(tuple);
    }
    
    ModelAndTuple(String modelID, String srcID, String trgID) {
      this.modelID = modelID;
      this.tuple = new InstructionRegions.RegionTuple(srcID, trgID);
    }    
     
    public String toString() {
      return ("ModelAndTuple: " + modelID + " [" + tuple + "]");
    }
    
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof ModelAndTuple)) {
        return (false);
      }
      ModelAndTuple otherMAT = (ModelAndTuple)other;
      return (this.modelID.equals(otherMAT.modelID) &&
              this.tuple.equals(otherMAT.tuple));
    }
    
    public int hashCode() {
      return (modelID.hashCode() + tuple.hashCode());
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get data for a core.  Using a hash map more efficient, but I don't want
  ** to hassle a version of hashcode / equals stuff that ignores ID, etc.
  */

  private CoreData getDataForCore(BuildInstruction bi, List<CoreData> cores) {
    int cSize = cores.size();
    for (int i = 0; i < cSize; i++) { 
      CoreData cd = cores.get(i);
      if (cd.aCore.sameDefinition(bi)) {
        return (cd);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  ** 
  ** Get count for a core.  Using a hash map more efficient, but I don't want
  ** to hassle a version of hashcode / equals stuff that ignores ID, etc.
  */

  private CoreCount getCountForCore(BuildInstruction bi, List<CoreCount> cores) {
    int cSize = cores.size();
    for (int i = 0; i < cSize; i++) { 
      CoreCount cc = cores.get(i);
      if (cc.core.sameDefinition(bi)) {
        return (cc);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  ** 
  ** Find and eliminate surplus root instructions
  */

  private List<String> removeSurplusInstructions(List<CoreData> coreAnalysis, Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
    
    //
    // Figure out what unused root instructions can be ditched base on optimization
    // strategy.  If the number of surplus instructions > 0, kill of those instances
    // that have the least references.
    //
    //
    // For each core, get a list of instruction IDs mapped from number of references.
    //
    
    ArrayList<String> retval = new ArrayList<String>();
    int cSize = coreAnalysis.size();
    for (int i = 0; i < cSize; i++) { 
      CoreData cd = coreAnalysis.get(i);
      if (cd.surplus > 0) {
        List<String> lu = cd.getLeastUsed(cd.surplus);
        cd.reallocateOrphanedInstructions(lu, oldToNew);
        retval.addAll(lu);
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** 
  */

  private void compressInstructions(List<CoreData> coreAnalysis, Map<String, Map<BuildInstructionInstance, BuildInstructionInstance>> oldToNew) {
    
    //
    // Figure out what unused root instructions can be ditched base on optimization
    // strategy.  If the number of surplus instructions > 0, kill of those instances
    // that have the least references.
    //
    //
    // For each core, get a list of instruction IDs mapped from number of references.
    //
    
    int cSize = coreAnalysis.size();
    for (int i = 0; i < cSize; i++) { 
      CoreData cd = coreAnalysis.get(i);
      cd.compressInstructions(oldToNew);
    }
    return;
  }   
}
