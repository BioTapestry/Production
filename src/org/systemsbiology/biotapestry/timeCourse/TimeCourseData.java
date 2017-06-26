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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.io.PrintWriter;
import java.io.IOException;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** This holds TimeCourseData
*/

public class TimeCourseData implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int NO_SLICE         = -1;
  public static final int SLICE_BY_TIMES   = 0;
  public static final int SLICE_BY_REGIONS = 1;
  
  public static final String SLICE_BY_TIMES_TAG   = "byTimes";
  public static final String SLICE_BY_REGIONS_TAG = "byRegions";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private List<TimeCourseGene> genes_;
  private ArrayList<GeneTemplateEntry> geneTemplate_;
  private ArrayList<GeneTemplateEntry> tempTemplate_;
  private HashMap<String, String> groupParents_;
  private HashSet<String> groupRoots_;
  private SortedMap<TopoTimeRange, RegionTopology> regionTopologies_;
  private TopoRegionLocator topoLocator_;
  private UniqueLabeller simLabels_;
  private long serialNumber_;
  private long topoSerialNumber_;
  private long linSerialNumber_;
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

  public TimeCourseData(DataAccessContext dacx) {
    dacx_ = dacx;
    genes_ = new ArrayList<TimeCourseGene>();
    groupParents_ = new HashMap<String, String>();
    groupRoots_ = new HashSet<String>();
    regionTopologies_ = new TreeMap<TopoTimeRange, RegionTopology>();
    topoLocator_ = null;
    simLabels_ = new UniqueLabeller();
    serialNumber_ = 0L;
    topoSerialNumber_ = 0L;
    linSerialNumber_ = 0L;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */
  
  @Override
  public TimeCourseData clone() {
    try {
      TimeCourseData retval = (TimeCourseData)super.clone();
  
      int size = this.genes_.size();
      retval.genes_ = new ArrayList<TimeCourseGene>();
      for (int i = 0; i < size; i++) {
        TimeCourseGene tcg = this.genes_.get(i);
        retval.genes_.add(tcg.clone());
      }
      
      // Map of strings to strings, shallow is OK:
      retval.groupParents_ = new HashMap<String, String>(this.groupParents_);
      
      // Set of strings, shallow is OK:
      retval.groupRoots_ = new HashSet<String>(this.groupRoots_);
     
      retval.regionTopologies_ = new TreeMap<TopoTimeRange, RegionTopology>();
      Iterator<TopoTimeRange> rtkit = this.regionTopologies_.keySet().iterator();
      while (rtkit.hasNext()) {
        TopoTimeRange key = rtkit.next();
        RegionTopology retTopo = regionTopologies_.get(key);     
        retval.regionTopologies_.put(key.clone(), retTopo.clone());
      }
   
      if (this.topoLocator_ != null) {   
        retval.topoLocator_ = this.topoLocator_.clone();     
      }
      
      retval.simLabels_ = this.simLabels_.clone();
      
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Get a set of all the used sim keys
  */
  
  public SortedSet<String> getAllSimKeys() {
    TreeSet<String> retval = new TreeSet<String>();
    int size = this.genes_.size();
    for (int i = 0; i < size; i++) {
      TimeCourseGene tcg = this.genes_.get(i);
      retval.addAll(tcg.getAllSimKeys());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the combo of existing options
  ** 
  */
  
  public Vector<ObjChoiceContent> buildSimKeyCombo() {
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    Set<String> skeys = getAllSimKeys();
    Iterator<String> eoit = skeys.iterator();
    while (eoit.hasNext()) {
      String simID = eoit.next();
      retval.add(new ObjChoiceContent(simID, simID));
    }
    return (retval);
  }
  

  /***************************************************************************
  **
  ** Get an unused sim key
  */
  
  public String getNewSimKey() {
    return (simLabels_.getNextLabel());
  }  
  
  /***************************************************************************
  **
  ** Return the serialNumber
  */
  
  public long getSerialNumber() {
    return (serialNumber_);
  }
  
  /***************************************************************************
  **
  ** Return the topology serialNumber
  */
  
  public long getTopoSerialNumber() {
    return (topoSerialNumber_);
  }
  
  /***************************************************************************
  **
  ** Return the lineage serialNumber
  */
  
  public long getLineageSerialNumber() {
    return (linSerialNumber_);
  }
  
  /***************************************************************************
  **
  ** Region Topology support
  **
  */

  public Iterator<TopoTimeRange> getRegionTopologyTimes() {
    initializeRegionTopoData();
    return (regionTopologies_.keySet().iterator());
  }  
   
  public RegionTopology getRegionTopology(TopoTimeRange range) {
    initializeRegionTopoData();
    return (regionTopologies_.get(range));
  }
  
  public TimeCourseChange changeRegionTopologyRegionName(String oldName, String newName) {
    TreeMap<TopoTimeRange, RegionTopology> newTops = new TreeMap<TopoTimeRange, RegionTopology>();   
    Iterator<TopoTimeRange> tmit = regionTopologies_.keySet().iterator();
    while (tmit.hasNext()) {
      TopoTimeRange ttr = tmit.next();
      RegionTopology rt = regionTopologies_.get(ttr);
      RegionTopology modRt = rt.changeRegionNameOnMatch(oldName, newName);
      newTops.put(ttr.clone(), modRt);
    }
    TopoRegionLocator newLoc = null;
    if (topoLocator_ != null) {
      newLoc = topoLocator_.changeRegionNameOnMatch(oldName, newName);
    }
    return ((newTops.isEmpty()) ? null: setRegionTopologiesInfo(newTops, newLoc));
  }
  
  public TimeCourseChange dropRegionTopologyRegionName(String dropName) {
    TreeMap<TopoTimeRange, RegionTopology> newTops = new TreeMap<TopoTimeRange, RegionTopology>();   
    Iterator<TopoTimeRange> tmit = regionTopologies_.keySet().iterator();
    while (tmit.hasNext()) {
      TopoTimeRange ttr = tmit.next();
      RegionTopology rt = regionTopologies_.get(ttr);
      RegionTopology modRt = rt.dropRegionName(dropName);
      newTops.put(ttr.clone(), modRt);
    }
    TopoRegionLocator newLoc = null;
    if (topoLocator_ != null) {
      newLoc = topoLocator_.dropRegionName(dropName);
    }
    return ((newTops.isEmpty()) ? null: setRegionTopologiesInfo(newTops, newLoc));
  }
  
  
   public boolean haveRegionNameInTopology(Set<String> checkNames) { 
    Iterator<TopoTimeRange> tmit = regionTopologies_.keySet().iterator();
    while (tmit.hasNext()) {
      TopoTimeRange ttr = tmit.next();
      RegionTopology rt = regionTopologies_.get(ttr);
      if (rt.hasRegionName(checkNames)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Used for IO only
  */
 
  public void setRegionTopology(TopoTimeRange range, RegionTopology topo) {
    regionTopologies_.put(range, topo);
    return;
  }  
  
  private SortedMap<TopoTimeRange, RegionTopology> duplicateRegionTopologies(SortedMap<TopoTimeRange, RegionTopology> inmap) {
    TreeMap<TopoTimeRange, RegionTopology> retval = new TreeMap<TopoTimeRange, RegionTopology>();
    Iterator<TopoTimeRange> rtksit = inmap.keySet().iterator();
    while (rtksit.hasNext()) {
      TopoTimeRange range = rtksit.next();
      RegionTopology rtopo = inmap.get(range);
      retval.put(range.clone(), rtopo.clone());
    }
    return (retval);
  }

  public void prepareRegionTopologyLocatorForInput() {
    if (topoLocator_ == null) {
      topoLocator_ = new TopoRegionLocator();
    }
    return;
  }   
    
  public TopoRegionLocator getRegionTopologyLocator() {
    if (topoLocator_ == null) {
      initializeRegionTopoData();
      topoLocator_ = new TopoRegionLocator(this);
    }
    return (topoLocator_);
  } 
  
  public TimeCourseChange setRegionTopologiesInfo(SortedMap<TopoTimeRange, RegionTopology> newTopologies, TopoRegionLocator newLoc) {
    
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.TOPO_SERIAL, topoSerialNumber_);
    retval.regionTopologiesOrig = duplicateRegionTopologies(regionTopologies_);
    retval.topoLocatorOrig = (topoLocator_ == null) ? null : topoLocator_.clone();       
        
    regionTopologies_ = newTopologies;
    topoLocator_ = newLoc;
    
    retval.regionTopologiesNew = duplicateRegionTopologies(regionTopologies_);
    retval.topoLocatorNew = (topoLocator_ == null) ? null : topoLocator_.clone();
    retval.topoSerialNumberNew = ++topoSerialNumber_;
 
    return (retval);
  }
  
  private void initializeRegionTopoData() {
    if (!regionTopologies_.isEmpty()) {
      return;
    }    
    List<RootInstanceSuggestions> slices = suggestTimeSlices();
    int num = slices.size();
    for (int i = 0; i < num; i++) {
      RootInstanceSuggestions sugg = slices.get(i);
      TopoTimeRange ttr = new TopoTimeRange(dacx_, sugg.minTime, sugg.maxTime);
      List<String> regs = new ArrayList<String>(sugg.regions);
      RegionTopology rt = new RegionTopology(ttr, regs, new ArrayList<TopoLink>());
      regionTopologies_.put(ttr, rt);
    }
  
    return;
  }
  
  public static String regionTopologyKeyword() {
    return (TopoTimeRange.XML_TAG_TOPO);  
  }
  
  public static String topoRegionKeyword() {
    return (RegionTopology.XML_TOPO_REGION_TAG);    
  }
  
  public static String topoLinkKeyword() {
    return (TopoLink.XML_TAG);
  }
  
  public static String topoLocationsForRangeKeyword() {
    return (TopoTimeRange.XML_TAG_LOC);
  }
  
  public static String topoRegionLocationKeyword() {
    return (TopoRegionLoc.XML_TAG);
  } 
  
  /***************************************************************************
  **
  ** Answer if the data can be merged
  */
  
  public boolean templatesMatch(TimeCourseData otherTCD) {
    Iterator<GeneTemplateEntry> otherTemp = otherTCD.getGeneTemplate();
    Iterator<GeneTemplateEntry> myTemp = getGeneTemplate();
    
    boolean iGottaTemplate = myTemp.hasNext();
    boolean theyGottaTemplate = otherTemp.hasNext();
    
    if (iGottaTemplate && theyGottaTemplate) {
      while (myTemp.hasNext()) {
        if (!otherTemp.hasNext()) {
          return (false);
        }
        GeneTemplateEntry myGte = myTemp.next();
        GeneTemplateEntry otherGte = otherTemp.next();
        if (!myGte.equals(otherGte)) {
          return (false);
        }
      }
      if (otherTemp.hasNext()) {
        return (false);
      }
      return (true);
    } else {
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Merge with another TCD
  */
  
  public Set<String> mergeData(TimeCourseData otherTCD) {

    HashSet<String> retval = new HashSet<String>();
    UiUtil.fixMePrintout("Still lots to do (bump serial no, topo, lineage,...)");
    Iterator<TimeCourseGene> otcdgit = otherTCD.getGenes();
    while (otcdgit.hasNext()) {
      TimeCourseGene otherGene = otcdgit.next();
      TimeCourseGene myGene = getTimeCourseData(otherGene.getName());
      if (myGene == null) {
        addGene(otherGene);       
      } else {
        if (!myGene.equals(otherGene)) {
          retval.add(myGene.getName());
        }
      } 
    }
    return ((retval.isEmpty()) ? null : retval);
  }

  /***************************************************************************
  **
  ** Find problematic merge issues for perturbation sources:
  */
  
  public Map<String, Set<PertSources>> getPertSourceMergeDependencies(Set<String> sdIDs) {
    HashMap<String, Set<PertSources>> retval = new HashMap<String, Set<PertSources>>();
    ArrayList<String> seen = new ArrayList<String>();
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TimeCourseGene tg = getGene(i);
      String geneName = tg.getName();
      seen.clear();
      Iterator<PertSources> pkit = tg.getPertKeys();
      while (pkit.hasNext()) {
        PertSources pss = pkit.next();
        Iterator<String> sit = pss.getSources();
        while (sit.hasNext()) {
          String psid = sit.next();
          if (sdIDs.contains(psid)) {
            seen.add(psid);
          }
        }
      }
      if (seen.size() > 1) {
        HashSet<PertSources> gottaGo = new HashSet<PertSources>();
        retval.put(geneName, gottaGo);
        pkit = tg.getPertKeys();
        while (pkit.hasNext()) {
          PertSources pss = pkit.next();
          Iterator<String> sit = pss.getSources();
          while (sit.hasNext()) {
            String psid = sit.next();
            if (seen.contains(psid)) {
              gottaGo.add(pss);
              break;
            }
          }
        }
      }
    }
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Get pert source dependencies
  */
  
  public Map<String, Set<String>> getPertSourceDependencies() {
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TimeCourseGene tg = getGene(i);
      String geneName = tg.getName();
      Iterator<PertSources> pkit = tg.getPertKeys();
      while (pkit.hasNext()) {
        PertSources pss = pkit.next();
        Iterator<String> sit = pss.getSources();
        while (sit.hasNext()) {
          String psid = sit.next();
          Set<String> forPsid = retval.get(psid);
          if (forPsid == null) {
            forPsid = new HashSet<String>();
            retval.put(psid, forPsid);
          }
          forPsid.add(geneName);
        }
      }
    }
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Drop the pert source dependencies
  */
  
  public TimeCourseChange[] dropPertSourceDependencies(String pertSrcID) {
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    HashSet<PertSources> killset = new HashSet<PertSources>();
    Map<String, Set<String>> myDepend = getPertSourceDependencies();
    Set<String> toDrop = myDepend.get(pertSrcID);
    Iterator<String> tdit = toDrop.iterator();
    while (tdit.hasNext()) {
      String geneName = tdit.next();
      TimeCourseGene gene = getTimeCourseData(geneName);   
      killset.clear();
      Iterator<PertSources> pkit = gene.getPertKeys();
      while (pkit.hasNext()) {
        PertSources pss = pkit.next();
        Iterator<String> sit = pss.getSources();
        while (sit.hasNext()) {
          String psid = sit.next();
          if (psid.equals(pertSrcID)) {
            killset.add(pss);
            break;
          }
        }
      }
      TimeCourseChange tcc = startGeneUndoTransaction(geneName);
      Iterator<PertSources> ksit = killset.iterator();
      while (ksit.hasNext()) {
        PertSources pss = ksit.next();
        gene.dropPerturbedState(pss);
      }
      tcc = finishGeneUndoTransaction(geneName, tcc);
      retvalList.add(tcc);
    } 
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));  
  }
    
   /***************************************************************************
   **
   ** Drop the pert source dependencies
   */
  
  public TimeCourseChange[] dropPertSourceMergeIssues(Map<String, Set<PertSources>> killTargets) {
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    Iterator<String> tdit = killTargets.keySet().iterator();
    while (tdit.hasNext()) {
      String geneName = tdit.next();
      TimeCourseGene gene = getTimeCourseData(geneName);
      Set<PertSources> killSet = killTargets.get(geneName);
      TimeCourseChange tcc = startGeneUndoTransaction(geneName);
      Iterator<PertSources> ksit = killSet.iterator();
      while (ksit.hasNext()) {
        PertSources pss = ksit.next();
        gene.dropPerturbedState(pss);
      }
      tcc = finishGeneUndoTransaction(geneName, tcc);
      retvalList.add(tcc);
    }
  
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));  
  }
 
  /***************************************************************************
  **
  ** Write the expression tables as a csv file:
  **
  */
  
  public void exportCSV(PrintWriter out, boolean groupByTime, 
                        boolean encodeConfidence, boolean exportInternals) { 
    out.print("\"");     
    out.print(dacx_.getRMan().getString("csvTcdExport.geneName"));
    out.print("\"");
    Iterator<GeneTemplateEntry> tempit = getGeneTemplate();
    if (tempit.hasNext()) {
      out.print(",");
    }  
    TemplateComparator tc = new TemplateComparator(groupByTime, tempit);
    TreeSet<GeneTemplateEntry> ordering = new TreeSet<GeneTemplateEntry>(tc);
    tempit = getGeneTemplate();
    while (tempit.hasNext()) {
      GeneTemplateEntry gte = tempit.next();
      ordering.add(gte);
    }
        
    Iterator<GeneTemplateEntry> oit = ordering.iterator(); 
    while (oit.hasNext()) {
      GeneTemplateEntry gte = oit.next();
      out.print("\"");
      out.print(gte.region);
      out.print(":");
      out.print(gte.time); 
      out.print("\"");
      if (oit.hasNext()) {
        out.print(",");
      }
    }
    out.println();
    
    TreeMap<String, Integer> ordered = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TimeCourseGene tg = getGene(i);
      ordered.put(tg.getName(), new Integer(i));
    }
    
    Iterator<Integer> goit = ordered.values().iterator();
    while (goit.hasNext()) {
      Integer indexObj = goit.next();
      TimeCourseGene tg = getGene(indexObj.intValue());
      oit = ordering.iterator(); 
      tg.exportCSV(out, oit, encodeConfidence, exportInternals);
    }
    
    ExpressionEntry.expressionKeyCSV(dacx_, out, encodeConfidence);
    
    return;
  }
 
  /***************************************************************************
  **
  ** Get range of data
  */  
  
  public int[] getDataRange() {
    if (genes_.isEmpty()) {
      return (null);
    }
    
    TimeCourseGene gene = genes_.get(0);    
    HashSet<Integer> rawtimes = new HashSet<Integer>();
    gene.getInterestingTimes(rawtimes);
    TreeSet<Integer> times = new TreeSet<Integer>();
    times.addAll(rawtimes);
    if (times.isEmpty()) {
      return (null);
    }
   
    int[] range = new int[2];
    range[0] = times.first().intValue();
    range[1] = times.last().intValue();               
    return (range);    
  }    

  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNode(String nodeID) {
    if (!haveDataEntries()) {
      return (false);
    }
    List<TimeCourseDataMaps.TCMapping> mapped = 
      dacx_.getDataMapSrc().getTimeCourseDataMaps().getTimeCourseTCMDataKeysWithDefault(nodeID, dacx_.getGenomeSource());
    if (mapped == null) {
      return (false);
    }
    Iterator<TimeCourseDataMaps.TCMapping> mit = mapped.iterator();
    while (mit.hasNext()) {
      TimeCourseDataMaps.TCMapping tcm = mit.next();
      if (getTimeCourseData(tcm.name) != null) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if all the time course data we have is internal only
  **
  */
  
  public boolean isAllInternalForNode(String nodeID, Database db) {
    if (!haveDataEntries()) {
      return (false);
    }
    List<TimeCourseDataMaps.TCMapping> mapped = db.getTimeCourseDataMaps().getTimeCourseTCMDataKeysWithDefault(nodeID, db);
    if (mapped == null) {
      return (false);
    }
    Iterator<TimeCourseDataMaps.TCMapping> mit = mapped.iterator();
    boolean retval = false;
    while (mit.hasNext()) {
      retval = true;
      TimeCourseDataMaps.TCMapping tcm = mit.next();
      TimeCourseGene tcg = getTimeCourseData(tcm.name);
      if (tcg == null) {
        continue;
      }
      if (!tcg.isInternalOnly()) {
        return (false);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answers if we have time course data for the given node
  **
  */
  
  public boolean haveDataForNodeOrName(String nodeID, String deadName, DataMapSource db) {
    if (!haveDataEntries()) {
      return (false);
    }
    List<TimeCourseDataMaps.TCMapping> mapped = db.getTimeCourseDataMaps().getTimeCourseTCMDataKeysWithDefaultGivenName(nodeID, deadName);
    if (mapped == null) {
      return (false);
    }
    Iterator<TimeCourseDataMaps.TCMapping> mit = mapped.iterator();
    while (mit.hasNext()) {
      TimeCourseDataMaps.TCMapping tcm = mit.next();
      if (getTimeCourseData(tcm.name) != null) {
        return (true);
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(TimeCourseChange undo) {
    if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeUndo(undo);
    } else if ((undo.allGenesOrig != null) || (undo.allGenesNew != null)) {
      fullChangeUndo(undo);
    } else if (((undo.groupParentsOrig != null) || (undo.groupParentsNew != null)) ||
               ((undo.groupRootsOrig != null) || (undo.groupRootsNew != null))) {
      hierarchyChangeUndo(undo);
    } else if (((undo.regionTopologiesOrig != null) || (undo.topoLocatorOrig != null)) ||
               ((undo.regionTopologiesNew != null) || (undo.topoLocatorNew != null))) {
      regionTopoUndo(undo);
    }
    if (undo.baseSerialNumberOrig != -1L) {
      serialNumber_ = undo.baseSerialNumberOrig;
    } else if (undo.topoSerialNumberOrig != -1L) {
      topoSerialNumber_ = undo.topoSerialNumberOrig;
    } else if (undo.linSerialNumberOrig != -1L) {
      linSerialNumber_ = undo.linSerialNumberOrig;
    } else {
      throw new IllegalStateException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(TimeCourseChange undo) {
    if ((undo.gOrig != null) || (undo.gNew != null)) {
      geneChangeRedo(undo);
    } else if ((undo.allGenesOrig != null) || (undo.allGenesNew != null)) {
      fullChangeRedo(undo);
    } else if (((undo.groupParentsOrig != null) || (undo.groupParentsNew != null)) ||
               ((undo.groupRootsOrig != null) || (undo.groupRootsNew != null))) {
      hierarchyChangeRedo(undo);
    } else if (((undo.regionTopologiesOrig != null) || (undo.topoLocatorOrig != null)) ||
               ((undo.regionTopologiesNew != null) || (undo.topoLocatorNew != null))) {
      regionTopoRedo(undo);
    }
    if (undo.baseSerialNumberNew != -1L) {
      serialNumber_ = undo.baseSerialNumberNew;
    } else if (undo.topoSerialNumberNew != -1L) {
      topoSerialNumber_ = undo.topoSerialNumberNew;
    } else if (undo.linSerialNumberNew != -1L) {
      linSerialNumber_ = undo.linSerialNumberNew;
    } else {
      throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we have data
  */
  
  public boolean haveDataEntries() {
    return (!genes_.isEmpty());
  }

  /***************************************************************************
  **
  ** Add a gene.  I/o only
  */
  
  public void addGene(TimeCourseGene gene) { 
    if (gene.isArchival()) {
      throw new IllegalArgumentException();
    }
    genes_.add(gene);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a gene
  */
  
  public TimeCourseChange addTimeCourseGene(TimeCourseGene gene) {
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.BASE_SERIAL, serialNumber_);
    if (gene.isArchival()) {
      throw new IllegalArgumentException();
    }
    retval.genePos = genes_.size();      
    genes_.add(gene);
    retval.gNew = new TimeCourseGene(gene);
    retval.gOrig = null;
    retval.baseSerialNumberNew = ++serialNumber_;
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get an iterator over the (non-archival) genes
  */
  
  public Iterator<TimeCourseGene> getGenes() {
    return (genes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get nth gene 
  */
  
  public TimeCourseGene getGene(int n) {
    return (genes_.get(n));
  }
 
  /***************************************************************************
  **
  ** Drop nth gene 
  */
  
  public TimeCourseChange dropGene(int n) {
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.BASE_SERIAL, serialNumber_);
    TimeCourseGene gene = genes_.get(n);
    retval.gOrig = new TimeCourseGene(gene);
    retval.gNew = null;
    retval.genePos = n;
    retval.baseSerialNumberNew = ++serialNumber_;
    genes_.remove(n);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the gene 
  */
  
  public TimeCourseChange dropGene(String nodeID) {
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      TimeCourseGene gene = genes_.get(i);
      if (DataUtil.keysEqual(gene.getName(), nodeID)) {
        TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.BASE_SERIAL, serialNumber_);
        retval.gOrig = new TimeCourseGene(gene);
        retval.gNew = null;
        retval.genePos = i;
        retval.baseSerialNumberNew = ++serialNumber_;
        genes_.remove(i);
        return (retval);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public TimeCourseChange startGeneUndoTransaction(String geneName) {
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.BASE_SERIAL, serialNumber_);
    Iterator<TimeCourseGene> git = getGenes();
    int count = 0;
    while (git.hasNext()) {
      TimeCourseGene tg = git.next();
      if (DataUtil.keysEqual(tg.getName(), geneName)) {
        retval.genePos = count;
        retval.gOrig = new TimeCourseGene(tg);
        retval.baseSerialNumberNew = ++serialNumber_;
        return (retval);
      }
      count++;
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  @SuppressWarnings("unused")
  public TimeCourseChange finishGeneUndoTransaction(String geneName, TimeCourseChange change) {
    TimeCourseGene tg = genes_.get(change.genePos);
    change.gNew = new TimeCourseGene(tg);
    return (change);
  }
  
  /***************************************************************************
  **
  ** Undo a gene change
  */
  
  private void geneChangeUndo(TimeCourseChange undo) {
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.set(undo.genePos, undo.gOrig);
    } else if (undo.gOrig == null) {
      genes_.remove(undo.genePos);
    } else {
      genes_.add(undo.genePos, undo.gOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a gene change
  */
  
  private void geneChangeRedo(TimeCourseChange undo) {
    if ((undo.gOrig != null) && (undo.gNew != null)) {
      genes_.set(undo.genePos, undo.gNew);
    } else if (undo.gNew == null) {
      genes_.remove(undo.genePos);
    } else {
      genes_.add(undo.genePos, undo.gNew);
    }
    return;
  }

  /***************************************************************************
  **
  ** Undo a gene change
  */
  
  private void fullChangeUndo(TimeCourseChange undo) {
    if ((undo.allGenesNew != null) && (undo.allGenesOrig != null)) {
      genes_ = undo.allGenesOrig;
      geneTemplate_ = null;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a gene change
  */
  
  private void fullChangeRedo(TimeCourseChange undo) {
    if ((undo.allGenesNew != null) && (undo.allGenesOrig != null)) {
      genes_ = undo.allGenesNew;
      geneTemplate_ = null;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a hierarchy change
  */
  
  private void hierarchyChangeUndo(TimeCourseChange undo) {
    if ((undo.groupParentsOrig != null) && (undo.groupRootsOrig != null)) {
      groupParents_ = new HashMap<String, String>(undo.groupParentsOrig);
      groupRoots_ = new HashSet<String>(undo.groupRootsOrig);
    }
    return;
  }

  /***************************************************************************
  **
  ** Redo a hierarchy change
  */
  
  private void hierarchyChangeRedo(TimeCourseChange undo) {
   if ((undo.groupParentsNew != null) && (undo.groupRootsNew != null)) {
      groupParents_ = new HashMap<String, String>(undo.groupParentsNew);
      groupRoots_ = new HashSet<String>(undo.groupRootsNew);
    }
    return;
  }    

  /***************************************************************************
  **
  ** Undo a topo change
  */
  
  private void regionTopoUndo(TimeCourseChange undo) {
    if (undo.regionTopologiesOrig != null) {
      regionTopologies_ = duplicateRegionTopologies(undo.regionTopologiesOrig);
    }
    topoLocator_ = (undo.topoLocatorOrig == null) ? null : (TopoRegionLocator)undo.topoLocatorOrig.clone();
    return;
  }

  /***************************************************************************
  **
  ** Redo a topo change
  */
  
  private void regionTopoRedo(TimeCourseChange undo) {
    if (undo.regionTopologiesNew != null) {
      regionTopologies_ = duplicateRegionTopologies(undo.regionTopologiesNew);
    } 
    topoLocator_ = (undo.topoLocatorNew == null) ? null : (TopoRegionLocator)undo.topoLocatorNew.clone();   
    return;
  }   
  
  /***************************************************************************
  **
  ** Return an iterator over the template for the genes
  */
  
  public Iterator<GeneTemplateEntry> getGeneTemplate() {
    if (geneTemplate_ == null) {
      initTemplate();
    }
    return (geneTemplate_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Answer if we have a gene template
  */
  
  public boolean hasGeneTemplate() {
    Iterator<GeneTemplateEntry> gtit = getGeneTemplate();
    return (gtit.hasNext());
  }  

  /***************************************************************************
  **
  ** Return an iterator over the template for the genes
  */
  
  private void initTemplate() {
    if (geneTemplate_ == null) {
      geneTemplate_ = new ArrayList<GeneTemplateEntry>();
      //  FIX ME: This is a hack
      if (genes_.size() > 0) {
        TimeCourseGene gene = genes_.get(0);
        Iterator<ExpressionEntry> eit = gene.getExpressions();
        while (eit.hasNext()) {
          ExpressionEntry exp = eit.next();
          int time = exp.getTime();
          String region = exp.getRegion();
          GeneTemplateEntry gte = new GeneTemplateEntry(time, region);
          geneTemplate_.add(gte);
        }
      } else if (tempTemplate_ != null) {  // bootstrapping
        Iterator<GeneTemplateEntry> ttit = tempTemplate_.iterator();
        while (ttit.hasNext()) {
          GeneTemplateEntry gte = ttit.next();
          geneTemplate_.add(new GeneTemplateEntry(gte));
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Normalize all the region names in a template
  */
  
  public void normalizeTemplate(List<GeneTemplateEntry> template) {
    HashMap<String, String> firstUse = new HashMap<String, String>();
    Iterator<GeneTemplateEntry> ttit = template.iterator();
    while (ttit.hasNext()) {
      GeneTemplateEntry gte = ttit.next();
      String normName = DataUtil.normKey(gte.region);
      String firstName = firstUse.get(normName);
      if (firstName == null) {      
        firstUse.put(normName, gte.region);
        firstName = gte.region;
      }
      gte.region = firstName;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Copy the template
  */
  
  private ArrayList<GeneTemplateEntry> copyTemplate(ArrayList<GeneTemplateEntry> template) {
    ArrayList<GeneTemplateEntry> retval = new ArrayList<GeneTemplateEntry>();
    Iterator<GeneTemplateEntry> ttit = template.iterator();
    while (ttit.hasNext()) {
      GeneTemplateEntry gte = ttit.next();
      retval.add(new GeneTemplateEntry(gte));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return a set of the regions 
  */
  
  public Set<String> getRegions() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<GeneTemplateEntry> tmpit = getGeneTemplate();
    while (tmpit.hasNext()) {
      GeneTemplateEntry gte = tmpit.next();
      retval.add(gte.region);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return a map of the regions to minimum time for the region 
  */
  
  public Map<String, Integer> getRegionsWithMinTimes() {
    Iterator<GeneTemplateEntry> tmpit = getGeneTemplate();
    return (getRegionsWithMinTimes(tmpit));
  }
  
  /***************************************************************************
  **
  ** Return a map of the regions to minimum time for the region 
  */
  
  private Map<String, Integer> getRegionsWithMinTimes(Iterator<GeneTemplateEntry> tmpit) {
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    while (tmpit.hasNext()) {
      GeneTemplateEntry gte = tmpit.next();
      Integer min = retval.get(gte.region);
      if (min == null) {
        retval.put(gte.region, new Integer(gte.time));
      } else if (min.intValue() > gte.time) {
        retval.put(gte.region, new Integer(gte.time));
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Return a list of the regions, retaining the original order used to
  ** specify the template 
  */
  
  public List<String> getRegionsKeepOrder() {
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<GeneTemplateEntry> tmpit = getGeneTemplate();
    while (tmpit.hasNext()) {
      GeneTemplateEntry gte = tmpit.next();
      if (!retval.contains(gte.region)) {
        retval.add(gte.region);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a COMPLETE region hierarchy
  */
  
  public boolean hierarchyIsSet() {
    if (groupRoots_.isEmpty()) {
      return (false);
    }
    int numReg = getRegions().size();
    if ((groupRoots_.size() + groupParents_.size()) < numReg) {
      return (false);
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** Answer if we have hierarchy info for region
  */
  
  public boolean hierarchyIsSetForRegion(String regionID) {
    return (regionIsRoot(regionID) || groupParents_.keySet().contains(regionID));
  }  

  /***************************************************************************
  **
  ** Answer if a region is a root in the hierarchy 
  */
  
  public boolean regionIsRoot(String regionID) {
    return (groupRoots_.contains(regionID));
  }

  /***************************************************************************
  **
  ** Return an entry from the region hierarchy.  Calling this for a root
  ** is illegal; use previous call first.
  */
  
  public String getParentRegion(String regionID) {
    String parent = groupParents_.get(regionID);
    if (parent == null) {
      throw new IllegalStateException();
    }
    return (parent);
  }
  
  /***************************************************************************
  **
  ** Set the region hierarchy
  */
  
  public TimeCourseChange setRegionHierarchy(Map<String, String> children, Set<String> roots, boolean bumpSerial) {
    TimeCourseChange retval = new TimeCourseChange(TimeCourseChange.LINEAGE_SERIAL, linSerialNumber_);
    retval.groupParentsOrig = new HashMap<String, String>(groupParents_);
    retval.groupRootsOrig = new HashSet<String>(groupRoots_);    
 
    groupParents_.clear();
    groupParents_.putAll(children);
    groupRoots_.clear();
    groupRoots_.addAll(roots);
    
    retval.groupParentsNew = new HashMap<String, String>(groupParents_);
    retval.groupRootsNew = new HashSet<String>(groupRoots_);
    retval.linSerialNumberNew = (bumpSerial) ? ++linSerialNumber_ : linSerialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return a list that orders regions by hierarchy
  */
  
  public List<String> getRegionHierarchyList() {
    if (!hierarchyIsSet()) {
      return (null);  
    }
    ArrayList<String> retval = new ArrayList<String>();
    List<String> regions = getRegionsKeepOrder();
    HashSet<String> remainingRegions = new HashSet<String>(regions);
 
    //
    // Add roots first:
    //
    
    int numR = regions.size();
    for (int i = 0; i < numR; i++) {
      String regionID = regions.get(i);  
      if (regionIsRoot(regionID)) {
        retval.add(regionID);
        remainingRegions.remove(regionID);
      }
    }
    
    //
    // Go thru kids and add them after parents:
    //
    
    while (!remainingRegions.isEmpty()) {
      for (int i = numR - 1; i >= 0; i--) {
        String regionID = regions.get(i);
        if (!remainingRegions.contains(regionID)) {
          continue;
        }
        String parent = getParentRegion(regionID);  
        int indexOf = retval.indexOf(parent);
        if (indexOf != -1) {
          retval.add(indexOf + 1, regionID);
          remainingRegions.remove(regionID);
        }
      }
    }
 
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Returns a Tree model 
  */
  
  public TreeModel getRegionHierarchyTree() {
    if (!hierarchyIsSet()) {
      return (null);  
    }
    MutableTreeNode treeRoot = new DefaultMutableTreeNode();
    DefaultTreeModel retval = new DefaultTreeModel(treeRoot);
    List<String> regions = getRegionsKeepOrder();
    HashSet<String> remainingRegions = new HashSet<String>(regions);
    HashMap<String, MutableTreeNode> nodeMap = new HashMap<String, MutableTreeNode>();
 
    //
    // Add roots first:
    //
    
    int numR = regions.size();
    for (int i = 0; i < numR; i++) {
      String regionID = regions.get(i);  
      if (regionIsRoot(regionID)) {
        MutableTreeNode mtn = new DefaultMutableTreeNode(regionID);
        nodeMap.put(regionID, mtn);
        retval.insertNodeInto(mtn, treeRoot, treeRoot.getChildCount());
        remainingRegions.remove(regionID);
      }
    }
    
    //
    // Go thru kids and add them after parents:
    //
    
    while (!remainingRegions.isEmpty()) {
      for (int i = 0; i < numR; i++) {
        String regionID = regions.get(i);
        if (!remainingRegions.contains(regionID)) {
          continue;
        }
        String parent = getParentRegion(regionID);
        MutableTreeNode parentNode = nodeMap.get(parent);
        if (parentNode != null) {
          MutableTreeNode mtn = new DefaultMutableTreeNode(regionID);
          nodeMap.put(regionID, mtn);
          retval.insertNodeInto(mtn, parentNode, parentNode.getChildCount());
          remainingRegions.remove(regionID);
        }
      }
    }
 
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Add a template entry (used in IO when we have no genes, but we do have
  ** a specified template);
  */
  
  public void addTemplateEntry(GeneTemplateEntry gte) { 
    if (genes_.size() > 0) {
      throw new IllegalArgumentException();
    }
    initTemplate();
    geneTemplate_.add(gte);
    
    return;
  }  
         
  /***************************************************************************
  **
  ** Update the data to the new template
  */
  
  public TimeCourseChange updateWithNewGeneTemplate(ArrayList<GeneTemplateEntry> newTemplate, ArrayList<Integer> mapping) {
  
    if (geneTemplate_ == null) {
      initTemplate();
    }
    //
    // Steal a copy and rebuild:
    //
    
    ArrayList<GeneTemplateEntry> localTemplate = geneTemplate_;
    geneTemplate_ = null;
    initTemplate();
    
    //
    // Decide if we have a change:
    //
    
    boolean haveChange = false;
    int mapSize = mapping.size();
    if (mapSize != localTemplate.size()) {
      haveChange = true;
    } else {
      int contigIndex = 0;
      for (int i = 0; i < mapSize; i++) {
        Integer mapEntry = mapping.get(i);
        if (mapEntry == null) {  // an addition
          haveChange = true;
          break;
        }
        int mapVal = mapEntry.intValue();
        if (mapVal != contigIndex++) {  // deletion
          haveChange = true;
          break;
        }
        GeneTemplateEntry oldEntry = localTemplate.get(mapVal);
        GeneTemplateEntry newEntry = newTemplate.get(i);
        if (!oldEntry.equals(newEntry)) {
          haveChange = true;
          break;
        }
      }
    }
    
    if (!haveChange) {
      return (null);
    }
 
    //
    // We have a change.  Go through the map and use it to build a new data set.
    // Start with a new set of data-free copies
    //
    
    TimeCourseChange retval = createFullUndo();    
    
    ArrayList<TimeCourseGene> newGenes = new ArrayList<TimeCourseGene>();
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      newGenes.add(new TimeCourseGene(genes_.get(i), false));
    }    

    for (int i = 0; i < mapSize; i++) {
      GeneTemplateEntry newEntry = newTemplate.get(i);      
      Integer mapEntry = mapping.get(i);
      if (mapEntry == null) {  // an addition
        for (int j = 0; j < size; j++) {        
          TimeCourseGene newGene = newGenes.get(j);
          newGene.addExpressionGlobally(i, new ExpressionEntry(newEntry.region, newEntry.time));
        }
      } else {
        int mapVal = mapEntry.intValue();
        GeneTemplateEntry oldEntry = localTemplate.get(mapVal);
        boolean isSame = oldEntry.equals(newEntry);
        for (int j = 0; j < size; j++) {        
          TimeCourseGene newGene = newGenes.get(j);
          TimeCourseGene oldGene = genes_.get(j); 
          newGene.mapExpressionGlobally(i, newEntry, mapVal, isSame, oldGene);
        }
      }
    }
     
    genes_ = newGenes;  // throw away old stuff
    
    // We can rebuild him...
    
    geneTemplate_ = null;
    tempTemplate_ = copyTemplate(newTemplate);
    initTemplate();
    
    retval = finishFullUndo(retval);
    return (retval);
  }

  /***************************************************************************
  **
  ** As the table template is changed, we need to find out if the hierarchy is
  ** intact.
  */
  
  public PrunedHierarchy havePrunedHierarchy(ArrayList<GeneTemplateEntry> newTemplate) {

    //
    // Any region that no longer exists cannot be a parent,
    // or have a parent.  Any new region must be added to the
    // root list.  Any deleted region must be removed from the
    // root list.  If a remaining kid is older than a parent,
    // if has to go.
    //
    
    if (groupParents_.isEmpty() && groupRoots_.isEmpty()) {
      return (null);
    }
    
    Map<String, Integer> regAndTimes = getRegionsWithMinTimes(newTemplate.iterator());
    HashSet<String> templateRegions = new HashSet<String>(regAndTimes.keySet());
      
    boolean haveHierChange = false;
    boolean rootAdd = false;
    Map<String, String> newParents = new HashMap<String, String>();
    Iterator<String> gpit = groupParents_.keySet().iterator();
    while (gpit.hasNext()) {
      String kid = gpit.next();
      String parent = groupParents_.get(kid);
      if (DataUtil.containsKey(templateRegions, kid) && 
          DataUtil.containsKey(templateRegions, parent)) {
        newParents.put(kid, parent);
      } else {
        haveHierChange = true;      
      }
    }
    
    HashSet<String> newRoots = new HashSet<String>();
    Iterator<String> rit = groupRoots_.iterator();
    while (rit.hasNext()) {
      String root = rit.next();
      if (DataUtil.containsKey(templateRegions, root)) {
        newRoots.add(root);
      } else {
        haveHierChange = true; 
      }
    }

    Map<String, String> prunedParents = templateBreaksHierarchy(regAndTimes, newParents);
    if (prunedParents != null) {
      newParents = prunedParents;
      haveHierChange = true;
    }
    
    //
    // Add dangling regions to root:
    //
       
    HashSet<String> hierRegs = new HashSet<String>(newParents.keySet());
    hierRegs.addAll(newRoots);
    Iterator<String> trit = templateRegions.iterator();
    while (trit.hasNext()) {
      String tReg = trit.next();
      if (!DataUtil.containsKey(hierRegs, tReg)) {
        newRoots.add(tReg);
        rootAdd = true;
      }
    }
      
    if (haveHierChange || rootAdd) {
      PrunedHierarchy retval = new PrunedHierarchy();
      retval.parents = newParents;
      retval.roots = newRoots;
      retval.droppedParents = haveHierChange;
      return (retval);
    }
    
    return (null);
  }
  
  /***************************************************************************
  **
  ** As the table template is changed, we need to delete bad hierarchy entries.
  ** intact.
  */
  
  public Map<String, String> templateBreaksHierarchy(Map<String, Integer> regAndTimes, Map<String, String> newParents) {
    HashMap<String, String> retval = new HashMap<String, String>();
    boolean pruned = false;
    Iterator<String> npit = newParents.keySet().iterator();
    while (npit.hasNext()) {
      String kid = npit.next();
      String parent = newParents.get(kid);
      Integer parentMin = regAndTimes.get(parent);
      Integer kidMin = regAndTimes.get(kid);
      if (kidMin.intValue() >= parentMin.intValue()) {
        retval.put(kid, parent);
      } else {
        pruned = true;
      }
    }
    return ((pruned) ? retval : null);
  }
 
  /***************************************************************************
  **
  ** Modify existing template entry
  */
  
  @SuppressWarnings("unused")
  private void modifyTemplate(int index, GeneTemplateEntry newEntry, 
                                         GeneTemplateEntry oldEntry) {
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      TimeCourseGene gene = genes_.get(i);
      ExpressionEntry entry = gene.getExpression(index);
      if ((entry.getTime() != oldEntry.time) || 
          (!entry.getRegion().equals(oldEntry.region))) {
        System.err.println("entry: " + entry.getTime() + " " + entry.getRegion());
        System.err.println("old: " + oldEntry.time + " " + oldEntry.region);        
        throw new IllegalStateException();
      }
      gene.updateExpression(index, newEntry);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Create a full gene list undo.  This is heavyweight!
  */
  
  private TimeCourseChange createFullUndo() {
    TimeCourseChange tcc = new TimeCourseChange(TimeCourseChange.BASE_SERIAL, serialNumber_);
    tcc.allGenesOrig = new ArrayList<TimeCourseGene>();
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      TimeCourseGene gene = genes_.get(i);
      tcc.allGenesOrig.add(new TimeCourseGene(gene));
    }
    tcc.baseSerialNumberNew = ++serialNumber_;
    return (tcc);
  }
  
  /***************************************************************************
  **
  ** Finish a full gene list undo.  This is heavyweight!
  */
  
  private TimeCourseChange finishFullUndo(TimeCourseChange tcc) {
    tcc.allGenesNew = new ArrayList<TimeCourseGene>();
    int size = genes_.size();
    for (int i = 0; i < size; i++) {
      TimeCourseGene gene = genes_.get(i);
      tcc.allGenesNew.add(new TimeCourseGene(gene));
    }
    return (tcc);
  }
  
  /***************************************************************************
  **
  ** Get the target gene info.
  */
  
  public TimeCourseGene getTimeCourseData(String targetName) {
    return (getTimeCourseDataCaseInsensitive(targetName));
  }
  
  /***************************************************************************
  **
  ** Get the earliest expression time for the given NODE ID
  */
  
  public int getFirstExpressionTime(String nodeID, DataAccessContext dacx) {
    int retval = Integer.MAX_VALUE;
    String baseID = GenomeItemInstance.getBaseID(nodeID);
    TimeCourseDataMaps tcdm = dacx.getDataMapSrc().getTimeCourseDataMaps();
    List<TimeCourseDataMaps.TCMapping> dataKeys = tcdm.getTimeCourseTCMDataKeysWithDefault(baseID, dacx.getGenomeSource());
    Iterator<TimeCourseDataMaps.TCMapping> dkit = dataKeys.iterator();
    while (dkit.hasNext()) {
      TimeCourseDataMaps.TCMapping tcm = dkit.next();
      TimeCourseGene tcg = getTimeCourseData(tcm.name);
      if (tcg == null) {
        continue;
      }
      int min = tcg.getGlobalFirstExpression(tcm.channel);
      if (min < retval) {
        retval = min;
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the target gene info.
  */
  
  public TimeCourseGene getTimeCourseDataCaseInsensitive(String targetName) {
    Iterator<TimeCourseGene> trgit = genes_.iterator();  // FIX ME: use a hash map?
    while (trgit.hasNext()) {
      TimeCourseGene trg = trgit.next();
      if (DataUtil.keysEqual(targetName, trg.getName())) {
        return (trg);
      }
    }
    return (null);
  }  

  /***************************************************************************
  **
  ** Get a set of the genes that are expressed at a certain time and region.
  */
  
  public void getExpressedGenes(String region, int hour, 
                                ExpressionEntry.Source exprSource, Set<String> expressed, double weak) {
    TimeCourseGene.VariableLevel varLev = new TimeCourseGene.VariableLevel();
    Iterator<TimeCourseGene> trgit = genes_.iterator();
    while (trgit.hasNext()) {
      TimeCourseGene trg = trgit.next();
      int expression = trg.getExpressionLevelForSource(region, hour, exprSource, varLev, weak);
      if ((expression == ExpressionEntry.EXPRESSED) ||
          (expression == ExpressionEntry.WEAK_EXPRESSION)) {
        expressed.add(trg.getName());
      } else if ((expression == ExpressionEntry.VARIABLE) && (varLev.level > 0.0)) {
        expressed.add(trg.getName());
      }            
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the set of regions that a gene expresses in
  */

  public Set<String> getGeneRegions(String geneID, ExpressionEntry.Source exprSource, boolean varToo) {
    TimeCourseGene tcg = getTimeCourseDataCaseInsensitive(geneID);
    if (tcg != null) {
      return (tcg.expressesInRegions(exprSource, varToo));
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Answer if the gene expresses in one and only one region.  Returns region
  ** ID if true, null otherwise.
  **
  */

  public String isRegionMarkerGene(String geneID, ExpressionEntry.Source exprSource, boolean varToo) {
    TimeCourseGene tcg = getTimeCourseDataCaseInsensitive(geneID);
    if (tcg != null) {
      String regID = tcg.expressesInOnlyAndOnlyOneRegion(exprSource, varToo);
      return (regID);
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Get the set of "interesting times" (Integers)
  **
  */
  
  public Set<Integer> getInterestingTimes() {
    HashSet<Integer> retval = new HashSet<Integer>();
    Iterator<TimeCourseGene> git = getGenes();
    while (git.hasNext()) {
      TimeCourseGene tg = git.next();
      tg.getInterestingTimes(retval);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the maximum time
  **
  */
  
  public int getMaximumTime() {
    int retval = 0;
    Iterator<TimeCourseGene> git = getGenes();
    while (git.hasNext()) {
      TimeCourseGene tg = git.next();
      int newMax = tg.getMaximumTime();
      if (newMax > retval) {
        retval = newMax;
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the minimum time
  **
  */
  
  public int getMinimumTime() {
    int retval = Integer.MAX_VALUE;
    Iterator<TimeCourseGene> git = getGenes();
    while (git.hasNext()) {
      TimeCourseGene tg = git.next();
      int newMin = tg.getMinimumTime();
      if (newMin < retval) {
        retval = newMin;
      }
    }
    return (retval);
  }   
    
  /***************************************************************************
  **
  ** We are changing the name of a node or gene, and have been told to modify the
  ** data as needed.  We need to change the entry (if any) to the given name
  ** and any custom maps to that name.
  */
  
  public TimeCourseChange[] changeName(String oldName, String newName, List<TimeCourseDataMaps> tcdm) {
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();

    if ((getTimeCourseData(newName) != null) && !DataUtil.keysEqual(oldName, newName)) {
      throw new IllegalArgumentException();
    }
    
    //
    // Do the entry:
    //
    
    TimeCourseGene gene = getTimeCourseData(oldName); 
    if (gene != null) {
      TimeCourseChange tcc = startGeneUndoTransaction(oldName);
      gene.setName(newName);
      tcc = finishGeneUndoTransaction(newName, tcc);
      retvalList.add(tcc);
    }
    
    //
    // Fix any custom entry maps
    //
    
    Iterator<TimeCourseDataMaps> tcdmit = tcdm.iterator();
    while (tcdmit.hasNext()) {
      TimeCourseChange[] tccs = tcdmit.next().changeTimeCourseMapsToName(oldName, newName);
      retvalList.addAll(Arrays.asList(tccs));
    }
    
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** We are changing the name of a region, and have been told to modify the
  ** data as needed.  We need to change the regions.  
  */
  
  public TimeCourseChange[] changeRegionName(String oldName, String newName, List<TimeCourseDataMaps> tcdm) {
    ArrayList<TimeCourseChange> retvalList = new ArrayList<TimeCourseChange>();
    
    //
    // Build a new template with the new name:
    //
    boolean haveChange = false;
    initTemplate();
    ArrayList<GeneTemplateEntry> newTemplate = copyTemplate(geneTemplate_);
    Iterator<GeneTemplateEntry> ntit = newTemplate.iterator();
    while (ntit.hasNext()) {
      GeneTemplateEntry gte = ntit.next();
      if (DataUtil.keysEqual(gte.region, oldName)) {
        gte.region = newName;
        haveChange = true;
      }
    }
 
    if (haveChange) {
      TimeCourseChange retval = createFullUndo();         
      ArrayList<TimeCourseGene> newGenes = new ArrayList<TimeCourseGene>();
      int size = genes_.size();
      for (int i = 0; i < size; i++) {
        TimeCourseGene oldGene = genes_.get(i);
        TimeCourseGene newGene = new TimeCourseGene(oldGene, false);
        newGenes.add(newGene);
        newGene.updateRegionGlobally(oldGene, oldName, newName);
      }    
      genes_ = newGenes;  // throw away old stuff
        
      // We can rebuild him...
    
      geneTemplate_ = null;
      tempTemplate_ = newTemplate;
      initTemplate();
    
      retval = finishFullUndo(retval);
      retvalList.add(retval);    
    }

    //
    // Fix any custom entry maps
    //
    
    Iterator<TimeCourseDataMaps> tcdmit = tcdm.iterator();
    while (tcdmit.hasNext()) {
      TimeCourseChange[] tccs = tcdmit.next().changeRegionNameForMaps(oldName, newName);
      retvalList.addAll(Arrays.asList(tccs));
    } 
   
    //
    // Propagate the name change into the hierarchy:
    //
    
    boolean haveHierChange = false;    
    HashMap<String, String> newParents = new HashMap<String, String>();
    Iterator<String> gpit = groupParents_.keySet().iterator();
    while (gpit.hasNext()) {
      String kid = gpit.next();
      String parent = groupParents_.get(kid);
      if (DataUtil.keysEqual(oldName, kid)) {
        haveHierChange = true;
        kid = newName;
      }
      if (DataUtil.keysEqual(oldName, parent)) {
        haveHierChange = true;
        parent = newName;
      }
      newParents.put(kid, parent);
    }
    
    HashSet<String> newRoots = new HashSet<String>();
    Iterator<String> rit = groupRoots_.iterator();
    while (rit.hasNext()) {
      String root = rit.next();
      if (DataUtil.keysEqual(oldName, root)) {
        haveHierChange = true;
        root = newName;
      }
      newRoots.add(root);
    }
  
    if (haveHierChange) {
      TimeCourseChange htcc = setRegionHierarchy(newParents, newRoots, true);
      retvalList.add(htcc);
    }
    
    TimeCourseChange rtrn = changeRegionTopologyRegionName(oldName, newName);
    if (rtrn != null) {
      retvalList.add(rtrn);
    }
     
    return (retvalList.toArray(new TimeCourseChange[retvalList.size()]));
  } 
   
  /***************************************************************************
  **
  ** Write the Time Course Data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<TimeCourseData");
    if (serialNumber_ != 0L) {
      out.print(" serialNum=\"");
      out.print(serialNumber_);
      out.print("\"");
    }  
    if (topoSerialNumber_ != 0L) {
      out.print(" topoSerialNum=\"");
      out.print(topoSerialNumber_);
      out.print("\"");
    }   
    if (linSerialNumber_ != 0L) {
      out.print(" linSerialNum=\"");
      out.print(linSerialNumber_);
      out.print("\"");
    }    
    out.println(">");
   
    if (genes_.size() > 0) {
      Iterator<TimeCourseGene> git = getGenes();
      ind.up();    
      while (git.hasNext()) {
        TimeCourseGene tg = git.next();
        tg.writeXML(out, ind);
      }
      ind.down();
    } else if (hasGeneTemplate()) {
       ind.up();    
       Iterator<GeneTemplateEntry> tempit = getGeneTemplate();
       TimeCourseGene tg = new TimeCourseGene("___Gene-For-BT-Template__", tempit, true);
       tg.writeXMLForTemplate(out, ind);
       ind.down();
    }
    if ((groupParents_.size() > 0) || (groupRoots_.size() > 0)) {
      writeHierarchy(out, ind);
    }     
    if ((regionTopologies_.size() > 0) || (topoLocator_ != null)) {
      writeTopology(out, ind);
    }     

    ind.indent();
    out.println("</TimeCourseData>");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("TimeCourseData: " + " genes = " + genes_);
  }
  
  /***************************************************************************
  **
  ** Answers if a target gene name is unique
  */
  
  public boolean nameIsUnique(String targName) {
    targName = targName.toUpperCase();
    Iterator<TimeCourseGene> git = getGenes();  
    while (git.hasNext()) {
      TimeCourseGene tg = git.next();
      if (tg.getName().toUpperCase().equals(targName)) {
        return (false);
      }
    }  
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get suggestions for root instances.  Returns a list of 
  ** RootInstanceSuggestions
  */
  
  public List<RootInstanceSuggestions> getRootInstanceSuggestions(int sliceType, Map<String, Set<String>> neighbors) {
    switch (sliceType) {
      case SLICE_BY_TIMES:
        return (suggestTimeSlices());
      case SLICE_BY_REGIONS:
        return (suggestRegionSlices(neighbors));
      default:
        throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Get hours for a region.
  */
  
  public SortedSet<Integer> hoursForRegion(String regionID) {
    Iterator<GeneTemplateEntry> gtit = getGeneTemplate();
    TreeSet<Integer> allTimes = new TreeSet<Integer>();
    SortedSet<Integer> retval = new TreeSet<Integer>();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = gtit.next();
      Integer timeVal = new Integer(gte.time);
      allTimes.add(timeVal);
      String region = gte.region;
      if (!region.equals(regionID)) {
        continue;
      }
      retval.add(timeVal);
    }
    //
    // We want to pad out the end time to the value just before
    // the next non-region time:
    //
    SortedSet<Integer> bogoDownTimes = new TreeSet<Integer>(retval);
    bogoDownTimes.add(allTimes.first());
    bogoDownTimes = DataUtil.fillOutHourly(bogoDownTimes);
    SortedSet<Integer> bogoUpTimes = new TreeSet<Integer>(allTimes);
    bogoUpTimes.removeAll(bogoDownTimes);
    if (!bogoUpTimes.isEmpty()) {
      Integer firstHigher = bogoUpTimes.first();
      retval.add(new Integer(firstHigher.intValue() - 1));
    }
    retval = DataUtil.fillOutHourly(retval);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get suggestions for root instances.  Returns a list of 
  ** RootInstanceSuggestions
  */
  
  private List<RootInstanceSuggestions> suggestRegionSlices(Map<String, Set<String>> neighbors) {

    ArrayList<Integer> allTimes = new ArrayList<Integer>(getInterestingTimes());
    Collections.sort(allTimes);
    int lastIndex = allTimes.size() - 1;
           
    //
    // Bin hours into regions:
    //
    Iterator<GeneTemplateEntry> gtit = getGeneTemplate();
    HashMap<String, Set<Integer>> regionMap = new HashMap<String, Set<Integer>>();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = gtit.next();
      String region = gte.region;
      Set<Integer> hours = regionMap.get(region);
      if (hours == null) {
        hours = new TreeSet<Integer>();
        regionMap.put(region, hours);
      }
      Integer timeVal = new Integer(gte.time);
      int myIndex = allTimes.indexOf(timeVal);
      int nextIndex = (myIndex < lastIndex) ? myIndex + 1 : myIndex;
      int decrement = (myIndex < lastIndex) ? 1 : 0;
      Integer nextTime = allTimes.get(nextIndex);
      Integer nextToLast = new Integer(nextTime.intValue() - decrement);
      hours.add(timeVal);
      hours.add(nextToLast);
    }
    
    //
    // Issue suggestion for each region:
    //
    
    TreeMap<Integer, List<RootInstanceSuggestions>> retvalMap = new TreeMap<Integer, List<RootInstanceSuggestions>>();
    
    Iterator<String> rmit = regionMap.keySet().iterator();
    while (rmit.hasNext()) {
      String regKey = rmit.next();
      Set<Integer> hours = regionMap.get(regKey);
      Iterator<Integer> hit = hours.iterator();
      RootInstanceSuggestions currRis = null;
      while (hit.hasNext()) {
        Integer hour = hit.next();
        int hourVal = hour.intValue();
        if (currRis == null) {
          ArrayList<Integer> times = new ArrayList<Integer>();
          times.add(hour);
          HashSet<String> regions = new HashSet<String>();
          regions.add(regKey);
          if (neighbors != null) {
            regions.addAll(neighbors.get(regKey));
          }
          currRis = new RootInstanceSuggestions(dacx_, hourVal, hourVal, times, regions, regKey, false);
          List<RootInstanceSuggestions> sugsForTime = retvalMap.get(hour);
          if (sugsForTime == null) {
            sugsForTime = new ArrayList<RootInstanceSuggestions>();
            retvalMap.put(hour, sugsForTime);
          }
          sugsForTime.add(currRis);
        } else {
          currRis.times.add(hour);
          currRis.maxTime = hourVal;
        }
      } 
    }
    //
    // Return in order of minimum times:
    //
    
    ArrayList<RootInstanceSuggestions> retval = new ArrayList<RootInstanceSuggestions>();
    Iterator<Integer> rvmit = retvalMap.keySet().iterator();
    while (rvmit.hasNext()) {
      Integer minTime = rvmit.next();    
      List<RootInstanceSuggestions> sugsForTime = retvalMap.get(minTime);
      retval.addAll(sugsForTime);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Get suggestions for root instances.  Returns a list of 
  ** RootInstanceSuggestions
  */
  
  public List<RootInstanceSuggestions> suggestTimeSlices() {
    ArrayList<RootInstanceSuggestions> retval = new ArrayList<RootInstanceSuggestions>();
    //
    // Bin regions into hours:
    //
    Iterator<GeneTemplateEntry> gtit = getGeneTemplate();
    TreeMap<Integer, Set<String>> hourMap = new TreeMap<Integer, Set<String>>();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = gtit.next();
      Integer timeKey = new Integer(gte.time);
      Set<String> regions = hourMap.get(timeKey);
      if (regions == null) {
        regions = new HashSet<String>();
        hourMap.put(timeKey, regions);
      }
      regions.add(gte.region);
    }
    
    //
    // Merge equal bins:
    //

    RootInstanceSuggestions currRis = null;
    
    Iterator<Integer> hmit = hourMap.keySet().iterator();
    while (hmit.hasNext()) {
      Integer hourKey = hmit.next();
      Set<String> regions = hourMap.get(hourKey);
      int hourVal = hourKey.intValue();
      if ((currRis == null) || (!currRis.regions.equals(regions))) {
        if (currRis != null) {
          currRis.maxTime = hourVal - 1;
          retval.add(currRis);
        }
        ArrayList<Integer> times = new ArrayList<Integer>();
        times.add(hourKey);
        currRis = new RootInstanceSuggestions(dacx_, hourVal, hourVal, times, regions, null, true);
      } else {
        currRis.times.add(hourKey);
        currRis.maxTime = hourVal;
      }
    }
    if (currRis != null) {
      retval.add(currRis);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the lineage of regions leading to the given region.  The group ID must be previously
  ** mapped.
  */  
 
  public List<TimeBoundedRegion> genRegionLineage(String groupID) {
    if (!hierarchyIsSetForRegion(groupID)) {
      throw new IllegalStateException();
    }
    ArrayList<TimeBoundedRegion> retval = new ArrayList<TimeBoundedRegion>();
    String currRegion = groupID;
    SortedSet<Integer> currTimes = DataUtil.fillOutHourly(hoursForRegion(groupID));
    while (true) {
      retval.add(new TimeBoundedRegion(dacx_, currTimes, currRegion));
      if (regionIsRoot(currRegion)) {
        return (retval);
      } else {
        currRegion = getParentRegion(currRegion);
        //
        // If a parent region's hours overlap the child region's hours,
        // we cut down the parent region span; parent region max < child
        // region min.
        //
        SortedSet<Integer> hfr = DataUtil.fillOutHourly(hoursForRegion(currRegion));
        int childMin = currTimes.first().intValue();
        int parentMax = hfr.last().intValue();
        for (int i = childMin; i <= parentMax; i++) {
          hfr.remove(new Integer(i));
        }
        currTimes = hfr;
      }
    }
  }
  
  /***************************************************************************
  **
  ** Get the set of regions that are direct lineage children.  The group ID must 
  ** be previously mapped.
  */  
 
  public Set<String> getRegionLineageDirectChildren(String groupID) {
    if (!hierarchyIsSetForRegion(groupID)) {
      throw new IllegalStateException();
    }
    HashSet<String> retval = new HashSet<String>();

    Iterator<String> kit = groupParents_.keySet().iterator();
    while (kit.hasNext()) {
      String kidID = kit.next();
      String parent = groupParents_.get(kidID);
      if (DataUtil.keysEqual(parent, groupID)) {
        retval.add(kidID);
      }
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to specify regions
  **
  */  
 
  public static class TimeBoundedRegion implements Cloneable {
    public SortedSet<Integer> times;
    public String region;
    private List<TimeBoundedRegion> lineage_;
    private DataAccessContext dacx_;
    
    public static final String TBR_XML_KEY = "tcTimedRegion";
       
    public TimeBoundedRegion(DataAccessContext dacx, SortedSet<Integer> times, String region) {
      dacx_ = dacx;
      this.times = DataUtil.fillOutHourly(times);
      this.region = region;
      this.lineage_ = null;
    }
       
    public List<TimeBoundedRegion> getLineage() {
      if (lineage_ == null) {
        lineage_ = dacx_.getExpDataSrc().getTimeCourseData().genRegionLineage(region);     
      }
      return (lineage_);
    }
    
    public List<String> getLineageAsNames() {
      ArrayList<String> retval = new ArrayList<String>();
      List<TimeBoundedRegion> linTbrs = getLineage();
      Iterator<TimeBoundedRegion> ltit = linTbrs.iterator();
      while (ltit.hasNext()) {
        TimeBoundedRegion ntbr = ltit.next();
        retval.add(ntbr.region);       
      }
      return (retval);
    }
 
    public int getRegionStart() {
      return (times.first().intValue());
    }
    
    public int getRegionEnd() {
      return (times.last().intValue());
    }
    
    public TimeBoundedRegion getLineageParent() {
      List<TimeBoundedRegion> lineage = getLineage();
      int numLim = lineage.size();
      if (numLim < 2) {
        return (null);
      }
      return (lineage_.get(numLim - 2));
    }
  
    public SortedSet<Integer> calculateLineageTimes() {
      List<TimeBoundedRegion> myLineage = getLineage();
      TreeSet<Integer> retval = new TreeSet<Integer>();
      int rNum = myLineage.size();
      for (int i = 0; i < rNum; i++) {
        TimeBoundedRegion tbr = myLineage.get(i);
        retval.addAll(tbr.times);
      }
      return (DataUtil.fillOutHourly(retval));
    }
    
    /***************************************************************************
    **
    ** Map of region IDs to times:
    */  

    public Map<String, SortedSet<Integer>> lineageRegionsToTimes() { 
      List<TimeBoundedRegion> lineageList = getLineage();
      HashMap<String, SortedSet<Integer>> retval = new HashMap<String, SortedSet<Integer>>();
      int numL = lineageList.size();
      for (int i = 0; i < numL; i++) {
        TimeBoundedRegion tbr = lineageList.get(i);
        retval.put(tbr.region, tbr.times);      
      }
      return (retval);
    }
  
    @Override
    public TimeBoundedRegion clone() {
      try {
        TimeBoundedRegion retval = (TimeBoundedRegion)super.clone();
        retval.times = new TreeSet<Integer>(this.times);
        retval.lineage_ = null;
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }  
    
    public void writeXML(PrintWriter out, Indenter ind) { 
      ind.indent();
      UiUtil.xmlOpen(TBR_XML_KEY, out, false);
      out.print("region=\"");
      out.print(region); 
      out.print("\" minTime=\"");
      out.print(times.first());
      out.print("\" maxTime=\"");
      out.print(times.last());
      out.println("\" />");        
      return;
    }
      
    public static TimeBoundedRegion buildFromXML(DataAccessContext dacx, String elemName, Attributes attrs) throws IOException {          
      String regName = AttributeExtractor.extractAttribute(elemName, attrs, TBR_XML_KEY, "region", true);
      String minTime = AttributeExtractor.extractAttribute(elemName, attrs, TBR_XML_KEY, "minTime", true);
      String maxTime = AttributeExtractor.extractAttribute(elemName, attrs, TBR_XML_KEY, "maxTime", true); 
      
      SortedSet<Integer> seedSet;
      try {
        Integer minVal = Integer.valueOf(minTime);
        Integer maxVal = Integer.valueOf(maxTime);
        seedSet = new TreeSet<Integer>();
        seedSet.add(minVal);
        seedSet.add(maxVal);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }  
      return (new TimeBoundedRegion(dacx, seedSet, regName));
    } 
  }
 
  /***************************************************************************
  **
  ** For XML I/O (Moving to new framework)
  */ 
 
  public static class TimeBoundedRegionWorker extends AbstractFactoryClient {
  
    private DataAccessContext dacx_;
    
    public TimeBoundedRegionWorker(DataAccessContext dacx, FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(TimeBoundedRegion.TBR_XML_KEY);
      dacx_ = dacx;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(TimeBoundedRegion.TBR_XML_KEY)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currTimeBoundRegion = TimeBoundedRegion.buildFromXML(dacx_, elemName, attrs);
        retval = board.currTimeBoundRegion;
      }
      return (retval);     
    }  
  }
  
  /***************************************************************************
  **
  ** Used to return Vfg Suggestions
  **
  */
  
  public static class RootInstanceSuggestions {
    public int minTime;
    public int maxTime;
    public List<Integer> times;
    public Set<String> regions;
    public String mainRegion;
    public boolean timeSliced;
    private DataAccessContext dacx_;
    
    public RootInstanceSuggestions(DataAccessContext dacx, int minTime, int maxTime, List<Integer> times, Set<String> regions, String mainRegion, boolean timeSliced) {
      dacx_ = dacx;
      this.minTime = minTime;
      this.maxTime = maxTime;      
      this.regions = regions;
      this.mainRegion = mainRegion;
      this.times = times;
      this.timeSliced = timeSliced;
    }
    
    @Override
    public String toString() { 
      if (timeSliced) {
        return (minTime + "-" + maxTime);
      } else {
        return (mainRegion + ":" + minTime + "-" + maxTime);
      }
    } 
       
    public String heavyToString() {
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      String displayUnits = tad.unitDisplayString();
      boolean suffixUnits = tad.unitsAreASuffix();    
      ResourceManager rMan = dacx_.getRMan();    
      String format = (suffixUnits) ? rMan.getString("timeRange.format") 
                                    : rMan.getString("timeRange.formatPrefix"); 
      String timeSpan = MessageFormat.format(format, new Object[] {new Integer(minTime), new Integer(maxTime), displayUnits});      
      if (timeSliced) {
        return (timeSpan);
      } else {
        return (mainRegion + ":" + timeSpan);
      }
    } 
  }

  /***************************************************************************
  **
  ** Used to define region topologies
  **
  */
  
  public static class RegionTopology implements Cloneable {
    public TopoTimeRange times;
    public List<String> regions;
    public List<TopoLink> links;
    
    public static final String XML_TOPO_REGION_TAG = "tcTopoRegion";    

    public RegionTopology(TopoTimeRange times) {
      this.times = times;
      this.regions = new ArrayList<String>();
      this.links = new ArrayList<TopoLink>();
    }    
   
    public RegionTopology(TopoTimeRange times, List<String> regions, List<TopoLink> links) {
      this.times = times;
      this.regions = regions;
      this.links = links;
    }
    
    public RegionTopology changeRegionNameOnMatch(String oldName, String newName) {
      RegionTopology retval = new RegionTopology(this.times);  
      int numReg = regions.size();
      for (int i = 0; i < numReg; i++) {
        String region = regions.get(i);
        if (DataUtil.keysEqual(region, oldName)) {
          retval.regions.add(newName);
        } else {
          retval.regions.add(region);
        }     
      }
      
      int numLinks = links.size();
      for (int i = 0; i < numLinks; i++) {
        TopoLink link = links.get(i);
        TopoLink lc = link.clone();
        if (DataUtil.keysEqual(lc.region1, oldName)) {
          lc.region1 = newName;
        }
        if (DataUtil.keysEqual(lc.region2, oldName)) {
          lc.region2 = newName;
        }
        retval.links.add(lc);  
      }
      return (retval);
    }
    
    public boolean hasRegionName(Set<String> checkNames) {
      int numReg = regions.size();
      for (int i = 0; i < numReg; i++) {
        String region = regions.get(i);
        if (DataUtil.containsKey(checkNames, region)) {
          return (true);
        }   
      }
      // Not needed, but belt and suspenders:
      int numLinks = links.size();
      for (int i = 0; i < numLinks; i++) {
        TopoLink link = links.get(i);    
        if (DataUtil.containsKey(checkNames, link.region1) ||
            DataUtil.containsKey(checkNames, link.region2)) {
          return (true);
        }
      }
      return (false);
    }
 
    public RegionTopology dropRegionName(String dropName) {
      RegionTopology retval = new RegionTopology(this.times);  
      int numReg = regions.size();
      for (int i = 0; i < numReg; i++) {
        String region = regions.get(i);
        if (!DataUtil.keysEqual(region, dropName)) {
          retval.regions.add(region);
        }   
      }
      
      int numLinks = links.size();
      for (int i = 0; i < numLinks; i++) {
        TopoLink link = links.get(i);    
        if (!DataUtil.keysEqual(link.region1, dropName) &&
            !DataUtil.keysEqual(link.region2, dropName)) {
          TopoLink lc = link.clone();
          retval.links.add(lc);
        }
      }
      return (retval);
    }
 
    public void addRegion(String newRegion) {
      regions.add(newRegion);
      return;
    }
    
    public void addLink(TopoLink newLink) {
      links.add(newLink);
      return;
    }
    
    public Iterator<TopoLink> getLinks() {
      return (links.iterator());
    }    
    
    public Iterator<String> getRegions() {
      return (regions.iterator());
    }  
    
    public boolean hasLink(TopoLink link) {
      return (links.contains(link));
    }
    
    public void removeLink(TopoLink link) {
      links.remove(link);
      return;
    }    
    
    @Override
    public RegionTopology clone() {
      try {
        RegionTopology retval = (RegionTopology)super.clone();
        retval.times = this.times.clone();
        retval.regions = new ArrayList<String>(this.regions);
        retval.links = new ArrayList<TopoLink>();
        int numL = this.links.size();
        for (int i = 0; i < numL; i++) {
          TopoLink tl = this.links.get(i);
          retval.links.add(tl.clone());
        }
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public void writeXML(PrintWriter out, Indenter ind) { 
      times.writeXMLOpen(TopoTimeRange.XML_TAG_TOPO, out, ind);
      int numReg = regions.size();
      ind.up().indent();
      out.println("<tcTopoRegions>");
      ind.up();
      for (int i = 0; i < numReg; i++) {
        ind.indent();
        out.print("<");
        out.print(XML_TOPO_REGION_TAG);
        out.print(" name=\"");
        out.print(regions.get(i));
        out.println("\"/>");        
      }
      ind.down().indent();
      out.println("</tcTopoRegions>");
      int numLinks = links.size();
      if (numLinks > 0) {
        ind.indent();
        out.println("<tcTopoLinks>");
        ind.up();
        for (int i = 0; i < numLinks; i++) {
          TopoLink link = links.get(i);
          link.writeXML(out, ind);     
        }
        ind.down().indent();
        out.println("</tcTopoLinks>");
      }
      ind.down();
      times.writeXMLClose(TopoTimeRange.XML_TAG_TOPO, out, ind);
      return;
    }
    
    public static String buildRegionFromXML(String elemName, 
                                            Attributes attrs) throws IOException {          
      String regName = AttributeExtractor.extractAttribute(elemName, attrs, XML_TOPO_REGION_TAG, "name", true);
      return (regName);
    }    
  }
  
  /***************************************************************************
  **
  ** Used to define topology time range
  **
  */
  
  public static class TopoTimeRange implements Cloneable, Comparable<TopoTimeRange> {
    public int minTime;
    public int maxTime;
    private DataAccessContext dacx_;
    
    public static final String XML_TAG_TOPO = "tcRegionTopology";
    public static final String XML_TAG_LOC = "topoLocationsForRange";    

    public TopoTimeRange(DataAccessContext dacx, int minTime, int maxTime) {
      if (minTime > maxTime) {
        throw new IllegalArgumentException();
      }
      dacx_ = dacx;
      this.minTime = minTime;
      this.maxTime = maxTime;      
    }
    
    public SortedSet<Integer> rangeAsHourly() {
      SortedSet<Integer> retval = new TreeSet<Integer>();
      retval.add(new Integer(minTime));
      retval.add(new Integer(maxTime));
      retval = DataUtil.fillOutHourly(retval);
      return (retval);
    }
 
    @Override
    public TopoTimeRange clone() {
      try {
        TopoTimeRange retval = (TopoTimeRange)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public int compareTo(TopoTimeRange other) {   
      if (this.minTime != other.minTime) {
        return (this.minTime - other.minTime);
      }
      return (this.maxTime - other.maxTime);
    }  

    @Override
    public int hashCode() {
      return (minTime + maxTime);
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TopoTimeRange)) {
        return (false);
      }
      
      return (this.compareTo((TopoTimeRange)other) == 0);
    }

    @Override
    public String toString() {
      TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
      String minStr;
      String maxStr;
      if (tad.haveNamedStages()) {
        minStr = tad.getNamedStageForIndex(minTime).name;
        maxStr = tad.getNamedStageForIndex(maxTime).name;        
      } else {      
        minStr = Integer.toString(minTime);
        maxStr = Integer.toString(maxTime);        
      }        
      ResourceManager rMan = dacx_.getRMan();    
      String format = (tad.unitsAreASuffix()) ? rMan.getString("timeRange.format") 
                                              : rMan.getString("timeRange.formatPrefix");
      String displayUnits = tad.unitDisplayAbbrev();
      String desc = MessageFormat.format(format, new Object[] {minStr, maxStr, displayUnits}); 
      return (desc);
    }
    
    public void writeXMLOpen(String tag, PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("<");
      out.print(tag);
      out.print(" min=\"");
      out.print(minTime);
      out.print("\" max=\"");
      out.print(maxTime);
      out.println("\">");
      return;
    }
    
    public void writeXMLClose(String tag, PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("</");
      out.print(tag);
      out.println(">");
      return;
    }
    
    public static TopoTimeRange buildFromXML(DataAccessContext dacx, String elemName, 
                                             Attributes attrs) throws IOException {

      if (!elemName.equals(XML_TAG_TOPO) && !elemName.equals(XML_TAG_LOC)) {
        return (null);
      }

      String minTimeStr = null;
      String maxTimeStr = null;

      if (attrs != null) {
        int count = attrs.getLength();
        for (int i = 0; i < count; i++) {
          String key = attrs.getQName(i);
          if (key == null) {
            continue;
          }
          String val = attrs.getValue(i);
          if (key.equals("min")) {
            minTimeStr = val;
          } else if (key.equals("max")) {
            maxTimeStr = val;
          }       
        }
      }
      
      int minTime;
      int maxTime;      
      try {
        minTime = Integer.parseInt(minTimeStr);
        maxTime = Integer.parseInt(maxTimeStr);        
        if ((minTime < 0) || (maxTime < 0)) {
          throw new IOException();
        }
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }    
      return (new TopoTimeRange(dacx, minTime, maxTime));
    }    
  }
  
  /***************************************************************************
  **
  ** Used to define topology links
  **
  */
  
  public static class TopoLink implements Cloneable {
    public String region1;
    public String region2;
    
   public static final String XML_TAG = "tcTopoLink";

    public TopoLink(String region1, String region2) {
      this.region1 = region1;
      this.region2 = region2;      
    }
    
    @Override
    public TopoLink clone() {
      try {
        TopoLink retval = (TopoLink)super.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    @Override
    public int hashCode() {
      return (region1.hashCode() + region2.hashCode());
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof TopoLink)) {
        return (false);
      }
      TopoLink otherTL = (TopoLink)other;
      
      return (this.region1.equals(otherTL.region1) && this.region2.equals(otherTL.region2));
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("<");
      out.print(XML_TAG);
      out.print(" reg1=\"");
      out.print(region1);
      out.print("\" reg2=\"");
      out.print(region2);
      out.println("\"/>");
      return;
    }
    
   public static TopoLink buildFromXML(String elemName, 
                                            Attributes attrs) throws IOException {
          
     String reg1 = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG, "reg1", true);
     String reg2 = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG, "reg2", true);
     return (new TopoLink(reg1, reg2));
   }
  }  

  /***************************************************************************
  **
  ** Used to hold region topo locations
  **
  */
  
  public static class TopoRegionLocator implements Cloneable {

    private HashMap<TopoTimeRange, Map<String, TopoRegionLoc>> regionTopoLocs_;
    
    public TopoRegionLocator() {
      regionTopoLocs_ = new HashMap<TopoTimeRange, Map<String, TopoRegionLoc>>();
    }

    public TopoRegionLocator(TimeCourseData tcd) {
      regionTopoLocs_ = new HashMap<TopoTimeRange, Map<String, TopoRegionLoc>>();    
      Iterator<TopoTimeRange> timeit = tcd.getRegionTopologyTimes();
      while (timeit.hasNext()) {
        TopoTimeRange ttr = timeit.next();
        RegionTopology regTopo = tcd.getRegionTopology(ttr);
        TopoTimeRange tRange = regTopo.times;
        HashMap<String, TopoRegionLoc> locsPerRange = initializeTopoLocData(regTopo, tRange);
        regionTopoLocs_.put(tRange, locsPerRange);
      }
    }
    
    public Iterator<TopoTimeRange> getRegionTopologyTimes() {
      return (new TreeSet<TopoTimeRange>(regionTopoLocs_.keySet()).iterator());
    }      
     
    public TopoRegionLocator changeRegionNameOnMatch(String oldName, String newName) {
      HashMap<TopoTimeRange, Map<String, TopoRegionLoc>> newMap = new HashMap<TopoTimeRange, Map<String, TopoRegionLoc>>();
      Iterator<TopoTimeRange> it = regionTopoLocs_.keySet().iterator();
      while (it.hasNext()) {
        TopoTimeRange range = it.next(); 
        Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
        HashMap<String, TopoRegionLoc> newLocsPerRange = new HashMap<String, TopoRegionLoc>();
        newMap.put(range, newLocsPerRange);
        Iterator<String> rit = locsPerRange.keySet().iterator();
        while (rit.hasNext()) {
          String regionID = rit.next();
          TopoRegionLoc loc = locsPerRange.get(regionID);
          TopoRegionLoc locClo = loc.clone();
          if (DataUtil.keysEqual(regionID, oldName)) {
            locClo.regionID = newName;
            newLocsPerRange.put(newName, locClo);
          } else {
            newLocsPerRange.put(regionID, locClo);
          }
        }
      }
      TopoRegionLocator retval = new TopoRegionLocator();
      retval.regionTopoLocs_ = newMap;
      return (retval);
    }
    
    public TopoRegionLocator dropRegionName(String dropName) {
      HashMap<TopoTimeRange, Map<String, TopoRegionLoc>> newMap = new HashMap<TopoTimeRange, Map<String, TopoRegionLoc>>();
      Iterator<TopoTimeRange> it = regionTopoLocs_.keySet().iterator();
      while (it.hasNext()) {
        TopoTimeRange range = it.next(); 
        Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
        HashMap<String, TopoRegionLoc> newLocsPerRange = new HashMap<String, TopoRegionLoc>();
        newMap.put(range, newLocsPerRange);
        Iterator<String> rit = locsPerRange.keySet().iterator();
        while (rit.hasNext()) {
          String regionID = rit.next();
          TopoRegionLoc loc = locsPerRange.get(regionID);     
          if (!DataUtil.keysEqual(regionID, dropName)) {
            TopoRegionLoc locClo = loc.clone();
            newLocsPerRange.put(regionID, locClo);
          }
        }
      }
      TopoRegionLocator retval = new TopoRegionLocator();
      retval.regionTopoLocs_ = newMap;
      return (retval);
    }
    
    @Override
    public TopoRegionLocator clone() {
      try {
        TopoRegionLocator retval = (TopoRegionLocator)super.clone();
        retval.regionTopoLocs_ = new HashMap<TopoTimeRange, Map<String, TopoRegionLoc>>();
        Iterator<TopoTimeRange> it = this.regionTopoLocs_.keySet().iterator();
        while (it.hasNext()) {
          TopoTimeRange range = it.next(); 
          Map<String, TopoRegionLoc> locsPerRange = this.regionTopoLocs_.get(range);
          if (locsPerRange != null) {
            HashMap<String, TopoRegionLoc> retLocPerRange = new HashMap<String, TopoRegionLoc>(); 
            Iterator<String> rit = locsPerRange.keySet().iterator();
            while (rit.hasNext()) {
              String regionID = rit.next();
              TopoRegionLoc loc = locsPerRange.get(regionID);
              retLocPerRange.put(regionID, loc.clone());
            }
            retval.regionTopoLocs_.put(range.clone(), retLocPerRange);
          }
        }
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public Point2D getRegionTopologyLocation(TopoTimeRange range, String regionID) {
      Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
      TopoRegionLoc loc = locsPerRange.get(regionID);
      return (loc.center);
    }
    
    public void setRegionTopologyLocation(TopoTimeRange range, String regionID, Point2D point) {
      Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
      TopoRegionLoc loc = locsPerRange.get(regionID);
      loc.center = new Point2D.Double(Math.round(point.getX()), Math.round(point.getY()));
      return;
    } 
    
    public void setRegionTopologyLocation(TopoTimeRange range, TopoRegionLoc loc) {
      Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
      if (locsPerRange == null) {
        locsPerRange = new HashMap<String, TopoRegionLoc>();
        regionTopoLocs_.put(range, locsPerRange);
      }
      locsPerRange.put(loc.regionID, loc);
      return;
    }     
 
    public void transferTopologyLocations(TopoRegionLocator other, TopoTimeRange otherRange, 
                                          TopoTimeRange newRange) {
      Map<String, TopoRegionLoc> locsPerRange = other.regionTopoLocs_.get(otherRange);
      if (locsPerRange != null) {
        HashMap<String, TopoRegionLoc> retLocPerRange = new HashMap<String, TopoRegionLoc>(); 
        Iterator<String> rit = locsPerRange.keySet().iterator();
        while (rit.hasNext()) {
          String regionID = rit.next();
          TopoRegionLoc loc = locsPerRange.get(regionID);
          retLocPerRange.put(regionID, loc.clone());
        }
        this.regionTopoLocs_.put(newRange, retLocPerRange);
      }
      return;
    }    
        
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();    
      out.println("<tcTopoRegionLocations>");
      ind.up();
      Iterator<TopoTimeRange> it = (new TreeSet<TopoTimeRange>(regionTopoLocs_.keySet())).iterator();
      while (it.hasNext()) {
        TopoTimeRange range = it.next(); 
        Map<String, TopoRegionLoc> locsPerRange = regionTopoLocs_.get(range);
        Iterator<String> rit = locsPerRange.keySet().iterator();
        range.writeXMLOpen(TopoTimeRange.XML_TAG_LOC, out, ind);
        ind.up();
        while (rit.hasNext()) {
          String regionID = rit.next();
          TopoRegionLoc loc = locsPerRange.get(regionID);
          loc.writeXML(out, ind);
        }
        ind.down();
        range.writeXMLClose(TopoTimeRange.XML_TAG_LOC, out, ind);
      }
      ind.down().indent(); 
      out.println("</tcTopoRegionLocations>");
      return;
    }
    
    @SuppressWarnings("unused")
    private HashMap<String, TopoRegionLoc> initializeTopoLocData(RegionTopology rt, TopoTimeRange ttr) {
      HashMap<String, TopoRegionLoc> retval = new HashMap<String, TopoRegionLoc>();
      int num = rt.regions.size();
      int numSide = (int)Math.ceil(Math.sqrt(num));
      double firstX = -300.0 * (numSide / 2);
      double firstY = -300.0 * (numSide / 2);      

      for (int i = 0; i < num; i++) {
        String regionID = rt.regions.get(i);
        int col = i % numSide;
        int row = i / numSide;                    
        double x = firstX + (col * 300.0);
        double y = firstY + (row * 300.0);        
        Point2D center = new Point2D.Double(x, y);
        TopoRegionLoc loc = new TopoRegionLoc(regionID, center);
        retval.put(regionID, loc);
      }
      return (retval);
    }
  }  
 
  /***************************************************************************
  **
  ** Used to define topology layout
  **
  */
  
  public static class TopoRegionLoc implements Cloneable {
    public String regionID;    
    public Point2D center;
    
    public static final String XML_TAG = "tcTopoRegionLoc";
   
    public TopoRegionLoc(String regionID, Point2D center) {
      this.regionID = regionID;
      this.center = new Point2D.Double(Math.round(center.getX()), Math.round(center.getY()));      
    }
    
    @Override
    public TopoRegionLoc clone() {
      try {
        TopoRegionLoc retval = (TopoRegionLoc)super.clone();
        retval.center = (Point2D)this.center.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("<");
      out.print(XML_TAG);
      out.print(" region=\"");
      out.print(regionID);
      out.print("\" x=\"");    
      out.print(center.getX());
      out.print("\" y=\"");    
      out.print(center.getY());      
      out.println("\" />");
      return;
    }
    
    public static TopoRegionLoc buildFromXML(String elemName, 
                                             Attributes attrs) throws IOException {
          
     String region = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG, "region", true);
     String xStr = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG, "x", true);
     String yStr = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG, "y", true);
     
 
     double xVal;
     double yVal;      
     try {
       xVal = Double.parseDouble(xStr);
       yVal = Double.parseDouble(yStr);        
     } catch (NumberFormatException nfex) {
       throw new IOException();
     }
     Point2D center = new Point2D.Double(xVal, yVal);    
     return (new TopoRegionLoc(region, center));
   }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("TimeCourseData");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static TimeCourseData buildFromXML(DataAccessContext dacx, String elemName, 
                                            Attributes attrs, 
                                            boolean serialNumberIsIllegal) throws IOException {
    if (!elemName.equals("TimeCourseData")) {
      return (null);
    }
    String serialString = AttributeExtractor.extractAttribute(elemName, attrs, "TimeCourseData", "serialNum", false);
    String topoSerialString = AttributeExtractor.extractAttribute(elemName, attrs, "TimeCourseData", "topoSerialNum", false);
    String linSerialString = AttributeExtractor.extractAttribute(elemName, attrs, "TimeCourseData", "linSerialNum", false);
    if (serialNumberIsIllegal && ((serialString != null) || (topoSerialString != null) || (linSerialString != null))) {
      throw new IOException();
    }
    
    TimeCourseData retval = new TimeCourseData(dacx);
  
    try {
      if (serialString != null) {
        long serialNumber = Long.parseLong(serialString);
        if (serialNumber < 0) {
          throw new IOException();
        }
        retval.serialNumber_ = serialNumber;
      }

      if (topoSerialString != null) {
        long topoSerialNumber = Long.parseLong(topoSerialString);
        if (topoSerialNumber < 0) {
          throw new IOException();
        }
        retval.topoSerialNumber_ = topoSerialNumber;   
      }

      if (linSerialString != null) {
        long linSerialNumber = Long.parseLong(linSerialString);
        if (linSerialNumber < 0) {
          throw new IOException();
        }
        retval.linSerialNumber_ = linSerialNumber;
      }

    } catch (NumberFormatException ex) {
      throw new IOException();
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static String extractRootRegion(String elemName, 
                                         Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(
              elemName, attrs, "tcRootRegion", "name", true));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static void extractRegionParent(String elemName, 
                                         Attributes attrs, Map<String, String> targetMap) throws IOException {
    String region = AttributeExtractor.extractAttribute(
                      elemName, attrs, "tcRegionParent", "region", true);
    String parent = AttributeExtractor.extractAttribute(
                      elemName, attrs, "tcRegionParent", "parent", true);    
    targetMap.put(region, parent);
    return;
  }  

  /***************************************************************************
  **
  ** Return the rootRegion keyword
  **
  */
  
  public static String rootRegionKeyword() {
    return ("tcRootRegion");
  }  
  
  /***************************************************************************
  **
  ** Return the regionParent keyword
  **
  */
  
  public static String regionParentKeyword() {
    return ("tcRegionParent");
  }
  /***************************************************************************
  **
  ** Return the region Hierarchy keyword
  **
  */
  
  public static String regionHierarchyKeyword() {
    return ("tcRegionHierarchy");
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the region hierarchy to XML
  **
  */

  private void writeHierarchy(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<tcRegionHierarchy>");
    ind.up(); 
    TreeSet<String> sorted = new TreeSet<String>(groupRoots_);
    Iterator<String> srit = sorted.iterator();
    while (srit.hasNext()) {
      String key = srit.next();     
      ind.indent();
      out.print("<tcRootRegion name=\"");
      out.print(key);
      out.println("\" />");
    }
    sorted = new TreeSet<String>(groupParents_.keySet());
    srit = sorted.iterator();
    while (srit.hasNext()) {
      String key = srit.next();
      String parent = groupParents_.get(key);
      ind.indent();
      out.print("<tcRegionParent region=\"");
      out.print(key);
      out.print("\" parent=\"");    
      out.print(parent);
      out.println("\" />");
    }
    ind.down().indent(); 
    out.println("</tcRegionHierarchy>");
    ind.down();
    return;
  }
  
  /***************************************************************************
  **
  ** Write the region topologies to XML
  **
  */

  private void writeTopology(PrintWriter out, Indenter ind) {
    ind.up().indent();    
    out.println("<tcRegionTopologyData>");
    ind.up(); 

    Iterator<TopoTimeRange> rtit = regionTopologies_.keySet().iterator();
    ind.indent();    
    out.println("<tcRegionTopologies>");
    ind.up();
    while (rtit.hasNext()) {
      TopoTimeRange key = rtit.next();
      RegionTopology rt = regionTopologies_.get(key);
      rt.writeXML(out, ind);
    }
    ind.down().indent();    
    out.println("</tcRegionTopologies>");
    
    if (topoLocator_ != null) {
      topoLocator_.writeXML(out, ind);
    }
    ind.down().indent(); 
    out.println("</tcRegionTopologyData>");
    ind.down();
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Useful for reporting hierarchy changes
  **
  */
  
  public static class PrunedHierarchy {
    public Map<String, String> parents;
    public Set<String> roots;
    public boolean droppedParents;
  }
  
  /***************************************************************************
  **
  ** Used to sort columns in CSV output
  **
  */
  
  public static class TemplateComparator implements Comparator<GeneTemplateEntry> {
    private boolean groupByTime_;
    private HashMap<String, Integer> regionOrder_;
    
    public TemplateComparator(boolean groupByTime, Iterator<GeneTemplateEntry> tempit) {
      groupByTime_ = groupByTime;
      regionOrder_ = new HashMap<String, Integer>();
      int count = 0;
      while (tempit.hasNext()) {
        GeneTemplateEntry gte = tempit.next();
        String normKey = DataUtil.normKey(gte.region);
        Integer orderNum = regionOrder_.get(normKey);
        if (orderNum == null) {
          orderNum = new Integer(count++);
          regionOrder_.put(normKey, orderNum);
        }
      }
    } 
    
    public int compare(GeneTemplateEntry gte1, GeneTemplateEntry gte2) {
      int ro1 = regionOrder_.get(DataUtil.normKey(gte1.region)).intValue();
      int ro2 = regionOrder_.get(DataUtil.normKey(gte2.region)).intValue();
      
      if (groupByTime_) {
        if (gte1.time != gte2.time) {
          return (gte1.time - gte2.time);
        }
        return (ro1 - ro2);
      } else {
        if (ro1 != ro2) {
          return (ro1 - ro2);
        }
        return (gte1.time - gte2.time);
      }
    }
  }
 
}
