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

package org.systemsbiology.biotapestry.db;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.GeneralChangeListener;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeListener;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.genome.XPlatDisplayText;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;

import org.xml.sax.Attributes;

/****************************************************************************
**
** Database. 
*/

public class Database implements GenomeSource, LayoutSource, 
                                 WorkspaceSource, ModelChangeListener, 
                                 GeneralChangeListener, InstructionSource, 
                                 DataMapSource, TemporalRangeSource, 
                                 ModelBuilder.Source, ExperimentalDataSource, 
                                 ModelDataSource, LocalDataCopyTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private Metabase mb_;
  private String id_;
  private int index_;
  private TabNameData tabData_;
  private DBGenome genome_;
  private HashMap<String, GenomeInstance> instances_;
  private HashMap<String, DynamicInstanceProxy> dynProxies_;  
  private HashMap<String, Layout> layouts_;
  private NavTree navTree_;
  private ModelData modelData_;
 
  private Boolean isDataSharing_; 
  private TimeAxisDefinition localTimeAxis_;
  private PerturbationData localPertData_;
  private TimeCourseData localTimeCourse_;
  private CopiesPerEmbryoData localCopiesPerEmb_;

  private TemporalInputRangeData rangeData_; 
  private TimeCourseDataMaps tcdm_;
  private PerturbationDataMaps pdms_;
  private UniqueLabeller labels_;
  private UniqueLabeller instructionLabels_;  
  private int uniqueNameSuffix_;
  private ArrayList<BuildInstruction> buildInstructions_;
  private HashMap<String, InstanceInstructionSet> instanceInstructionSets_;
  private Workspace workspace_;
  private StartupView startupView_;
  private ModelBuilder.Database mbDataSourceX_;
  private UIComponentSource uics_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Not-null constructor. Gotta ditch the uics:
  */

  public Database(String id, int index, UIComponentSource uics, Metabase mb, TabPinnedDynamicDataAccessContext ddacx) {
    mb_ = mb;
    uics_ = uics;
    id_ = id;
    index_ = index;
    String defName = ddacx.getRMan().getString("database.defaultModelName");
    tabData_ = new TabNameData(defName, defName, defName);
    instances_ = new HashMap<String, GenomeInstance>();
    layouts_ = new HashMap<String, Layout>();
    navTree_ = new NavTree(uics_);
    dynProxies_ = new HashMap<String, DynamicInstanceProxy>();    
    labels_ = new UniqueLabeller();
    instructionLabels_ = new UniqueLabeller();
    buildInstructions_ = new ArrayList<BuildInstruction>();
    workspace_ = new Workspace();
    uniqueNameSuffix_ = 1;
    startupView_ = new StartupView();
    installDataSharing(null, ddacx);

    instanceInstructionSets_ = new HashMap<String, InstanceInstructionSet>();
    uics_.getEventMgr().addModelChangeListener(this);
    uics_.getEventMgr().addGeneralChangeListener(this);    
  }

  /***************************************************************************
  **
  ** Install initialized data sharing policy
  */

  public void installDataSharing(Metabase.DataSharingPolicy dsp, TabPinnedDynamicDataAccessContext ddacx) {
    if (dsp == null) {     
      isDataSharing_ = Boolean.valueOf(false);
       // Actually need the dynamic one, since still building the database: 
      localTimeAxis_ = new TimeAxisDefinition(uics_.getRMan());
      localPertData_ = new PerturbationData();
      localTimeCourse_ = new TimeCourseData(ddacx);
      localCopiesPerEmb_ = new CopiesPerEmbryoData();
      return; 
    }    
    if (!dsp.init) {
      throw new IllegalArgumentException();
    }
    localTimeAxis_ = (dsp.shareTimeUnits) ? null : new TimeAxisDefinition(uics_.getRMan());
    localPertData_ = (dsp.sharePerts) ? null : new PerturbationData();
    localTimeCourse_ = (dsp.shareTimeCourses) ? null : new TimeCourseData(ddacx);
    localCopiesPerEmb_ = (dsp.sharePerEmbryoCounts) ? null : new CopiesPerEmbryoData();
    isDataSharing_ = Boolean.valueOf(dsp.isSpecifyingSharing());
    return;
  }

  /***************************************************************************
  **
  ** Copy constructor
  */

  public Database(Database other) {
    this.copyGuts(other);
    this.uics_.getEventMgr().addModelChangeListener(this);
    this.uics_.getEventMgr().addGeneralChangeListener(this);
  }

  /***************************************************************************
  **
  ** Copy guts
  */

  private void copyGuts(Database other) { 
    this.mb_ = other.mb_;
    this.uics_ = other.uics_;
    this.id_ = other.id_;
    this.index_ = other.index_;
    this.tabData_ = other.tabData_.clone();

    this.genome_ = other.genome_.clone();
    
    this.instances_ = new HashMap<String, GenomeInstance>();
    for (String gik : other.instances_.keySet()) {
      GenomeInstance ogi = other.instances_.get(gik);
      this.instances_.put(gik, ogi.clone());
    }
    
    this.layouts_ = new HashMap<String, Layout>();
    for (String lak : other.layouts_.keySet()) {
      Layout ola = other.layouts_.get(lak);
      this.layouts_.put(lak, ola.clone());
    }
    
    this.navTree_ = new NavTree(other.navTree_);
    
    this.dynProxies_ = new HashMap<String, DynamicInstanceProxy>();
    for (String dipk : other.dynProxies_.keySet()) {
      DynamicInstanceProxy dip = other.dynProxies_.get(dipk);
      this.dynProxies_.put(dipk, dip.clone());
    }
    this.labels_ = other.labels_.clone();
    this.instructionLabels_ = other.instructionLabels_.clone();

    this.buildInstructions_ = new ArrayList<BuildInstruction>();
    for (BuildInstruction bi : other.buildInstructions_) {
      this.buildInstructions_.add(bi.clone());
    }
       
    this.workspace_ = other.workspace_.clone();
    this.uniqueNameSuffix_ = other.uniqueNameSuffix_;
    
    this.startupView_ = other.startupView_.clone();
    
    this.instanceInstructionSets_ = new HashMap<String, InstanceInstructionSet>();
    for (String iisk : other.instanceInstructionSets_.keySet()) {
      InstanceInstructionSet iis = other.instanceInstructionSets_.get(iisk);
      this.instanceInstructionSets_.put(iisk, iis.clone());
    }

    this.tcdm_ = other.tcdm_.clone();
    this.pdms_ = other.pdms_.clone();
    
    this.localTimeAxis_ = (other.localTimeAxis_ != null) ? other.localTimeAxis_.clone() : null;
    this.localPertData_ = (other.localPertData_ != null) ? other.localPertData_.clone() : null;
    this.localTimeCourse_ = (other.localTimeCourse_ != null) ? other.localTimeCourse_.clone() : null;
    this.localCopiesPerEmb_ = (other.localCopiesPerEmb_ != null) ? other.localCopiesPerEmb_.clone() : null;
     
    this.rangeData_ = other.rangeData_.clone();
    this.modelData_ = other.modelData_.clone();
     
    if (other.getMbDB() != null) {
      mbDataSourceX_ = other.getMbDB().deepCopy();
    }
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Get the modelBuilderDatabase
  */

  public ModelBuilder.Database getMbDB() {
    if (mbDataSourceX_ == null) {
      Iterator<ModelBuilderPlugIn> mbIterator = uics_.getPlugInMgr().getBuilderIterator();
      if (mbIterator.hasNext()) {
        ModelBuilderPlugIn plugIn = mbIterator.next();
        mbDataSourceX_ = plugIn.getModelBuilderDatabase();
      }
    }
    return (mbDataSourceX_);
  }
  
  /***************************************************************************
  ** 
  ** Reset ID. Only for IO/Metabase use!
  */

  public void reset(String id, int index) {
    id_ = id;
    index_ = index;
    return;
  }
 
  /***************************************************************************
  ** 
  ** Get tab name data
  */

  public TabNameData getTabNameData() {
    return (tabData_);
  }
  
  /***************************************************************************
  ** 
  ** Set tab name data
  */

  public DatabaseChange setTabNameData(TabNameData tnd) {
    DatabaseChange dc = new DatabaseChange();
    dc.oldTabData = tabData_.clone();
    tabData_ = tnd;
    dc.newTabData = tabData_.clone();
    return (dc);
  }
  
  /***************************************************************************
  ** 
  ** Get ID
  */

  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  ** 
  ** Get index
  */

  public int getIndex() {
    return (index_);
  }
  
  /***************************************************************************
  ** 
  ** Set ID
  */

  public void setID(String id) {
    id_ = id;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Set index
  */

  public void setIndex(int index) {
    index_ = index;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Start a new model
  */

  public void newModelViaDACX(TabPinnedDynamicDataAccessContext ddacx) {
    dropViaDACX(ddacx);
    genome_ = new DBGenome(ddacx, ddacx.getRMan().getString("tree.FullGenome"), "bioTapA");
    labels_.addExistingLabel("bioTapA");
    String layoutID = labels_.getNextLabel();    
    layouts_.put(layoutID, new Layout(layoutID, "bioTapA"));
    navTree_.addNode(NavTree.Kids.ROOT_MODEL, null, null, new NavTree.ModelID("bioTapA"), null, null, ddacx); 
    
    
    modelData_ = null;
    tcdm_ = new TimeCourseDataMaps(ddacx);
    rangeData_ = new TemporalInputRangeData(ddacx);
    pdms_ = new PerturbationDataMaps();
    
    installDataSharing(null, ddacx);
    
    //
    // We do NOT clear out display options, but we do need to drop
    // defs that depend on the time def, so the time axis definition 
    // is not frozen for a new model:
    //

    ddacx.getExpDataSrc().getPertData().getPertDisplayOptions().dropDataBasedOptions();
  
    workspace_ = new Workspace();
    uniqueNameSuffix_ = 1;
    startupView_ = new StartupView();     
    return;
  }  

  /***************************************************************************
  ** 
  ** Drop everything
  */

  public void dropViaDACX(TabPinnedDynamicDataAccessContext ddacx) {
    genome_ = null;
    layouts_.clear();
    instances_.clear();
    dynProxies_.clear();
    String defName = ddacx.getRMan().getString("database.defaultModelName");
    tabData_ = new TabNameData(defName, defName, defName);
    // Have to clear out above before clearing the tree so that
    // the tree clears correctly:
    navTree_.clearOut();
    modelData_ = null;
    tcdm_ = null;
    // Not all input files have perturbation data maps, so this does not
    // get filled in during I/O. Cannot be set to null.
    pdms_ = new PerturbationDataMaps();
    rangeData_ = null;
    installDataSharing(null, ddacx);
    
    workspace_ = new Workspace();
    uniqueNameSuffix_ = 1;
    startupView_ = new StartupView();     
    labels_ = new UniqueLabeller();
    uics_.getPathMgr().dropAllPaths();
    // Note we are not resetting the Display options!
    buildInstructions_.clear();
    instructionLabels_ = new UniqueLabeller();
    instanceInstructionSets_.clear();
    
    if (getMbDB() != null) {
      getMbDB().dropViaDACX();
    }
    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Drop only the guts of the root network. 
  */

  public DatabaseChange dropRootNetworkOnly() {

    DatabaseChange retval = new DatabaseChange();
    retval.oldGenome = genome_;
    retval.oldLayouts = new HashMap<String, Layout>();
    retval.newLayouts = new HashMap<String, Layout>();
    Iterator<Layout> lit = layouts_.values().iterator();
    while (lit.hasNext()) {
      Layout lo = lit.next();
      String targ = lo.getTarget();
      String name = lo.getID();
      if (targ.equals(genome_.getID())) {
        retval.oldLayouts.put(name, new Layout(lo));
        // Note this drops overlay and note info in the layout.  We require later
        // fixup from legacy layout to return this information:
        retval.newLayouts.put(name, new Layout(name, "bioTapA"));
      }
    }
    Iterator<String> nlit = retval.newLayouts.keySet().iterator();
    while (nlit.hasNext()) {
      String name = nlit.next();
      Layout lo = retval.newLayouts.get(name);
      layouts_.put(name, new Layout(lo));
    }
    genome_ = (DBGenome)genome_.getStrippedGenomeCopy();
    retval.newGenome = genome_;
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Drop only the guts of an instance network
  */

  public DatabaseChange dropInstanceNetworkOnly(String gid) {

    GenomeInstance gi = instances_.get(gid);
    DatabaseChange retval = new DatabaseChange();
    retval.oldInstance = gi;
    retval.oldLayouts = new HashMap<String, Layout>();
    retval.newLayouts = new HashMap<String, Layout>();
    Iterator<Layout> lit = layouts_.values().iterator();
    while (lit.hasNext()) {
      Layout lo = lit.next();
      String targ = lo.getTarget();
      String name = lo.getID();
      if (targ.equals(gid)) {
        retval.oldLayouts.put(name, new Layout(lo));
        retval.newLayouts.put(name, new Layout(name, gid));        
      }
    }
    Iterator<String> nlit = retval.newLayouts.keySet().iterator();
    while (nlit.hasNext()) {
      String name = nlit.next();
      Layout lo = retval.newLayouts.get(name);
      layouts_.put(name, new Layout(lo));
    }    
    GenomeInstance newGI = (GenomeInstance)gi.getStrippedGenomeCopy();
    instances_.put(gid, newGI);
    retval.newInstance = newGI;
    return (retval);
  }  
  
    
  /***************************************************************************
  ** 
  ** We need to install hour bounds on parent models of child dynamic models
  */

  List<String> legacyIOFixupForHourBounds() {
    if (dynProxies_.isEmpty()) {
      return (null);
    }
    
    int minTcd = Integer.MAX_VALUE;
    int maxTcd = Integer.MIN_VALUE;    
    TimeCourseData tcd = getTimeCourseData();
    if ((tcd != null) && tcd.haveDataEntries()) {
      minTcd = tcd.getMinimumTime(); 
      maxTcd = tcd.getMaximumTime();
    }

    ArrayList<String> retval = new ArrayList<String>();
    // Two passes: bottom up followed by top-down:
    Iterator<GenomeInstance> iit = getInstanceIterator();    
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      gi.fixupLegacyIOHourBoundsFromProxies(minTcd, maxTcd, retval);
    }
    iit = getInstanceIterator();    
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      gi.fixupLegacyIOHourBoundsFromParents(retval);
    }    

    return (retval);
  } 

  /***************************************************************************
  ** 
  ** We need to install new group ordering values
  */

  void legacyIOFixupForGroupOrdering() {

    String rootKey = getRootDBGenome().getID();

    //
    // Since layouts are only for VfAs, we are only going to deal with them:
    //
    Iterator<String> lokit = layouts_.keySet().iterator();
    while (lokit.hasNext()) {
      String lokey = lokit.next();
      Layout layout = layouts_.get(lokey);
      String targ = layout.getTarget();
      if (targ.equals(rootKey)) {
        continue;
      }
      int order = 1;
      GenomeInstance git = instances_.get(targ);
      Iterator<Group> it = git.getGroupIterator();
      while (it.hasNext()) {
        Group group = it.next();
        String groupID = group.getID();
        GroupProperties gprop = layout.getGroupProperties(groupID);
        if (gprop.getLayer() != 0) {
          continue;
        }
        gprop.setOrder(order++);
      }
      it = git.getGroupIterator();
      while (it.hasNext()) {
        Group group = it.next();
        String groupID = group.getID();
        GroupProperties gprop = layout.getGroupProperties(groupID);
        if (gprop.getLayer() == 0) {
          continue;
        }
        String parentID = group.getParentID();  // This works at VFA level (only)
        GroupProperties gpropParent = layout.getGroupProperties(parentID);
        gprop.setOrder(gpropParent.getOrder());
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** We need to fix genes that are too short and that have module problems
  */
  
  public String legacyIOFixupForGeneLengthAndModules(String message, ResourceManager rMan, String msgDiv, DataAccessContext dacx) {
    boolean doit = false;
    //
    // Gene length must now match link usage:
    //
    FullGenomeHierarchyOracle fgho = dacx.getFGHO();
    Map<String, Integer> repairs = new HashMap<String, Integer>();
    Map<String, Map<String, Set<Integer>>> padsForModGenes = new HashMap<String, Map<String, Set<Integer>>>();
    fgho.legacyIOGeneLengthFixup(repairs, padsForModGenes);
    if (!repairs.isEmpty()) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.geneLengthErrors");
      legacyIOFixupForGeneLength(repairs);
      doit = true;
    }
    //
    // We have tweaked how gene cis-reg domains are modeled and displayed:
    //
    List<String> errs = DBGeneRegion.legacyIOFixup(this, rMan, padsForModGenes);
    if (!errs.isEmpty()) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.moduleErrors");
      for (String msg : errs) {
        message = message + msgDiv + msg;
      }
      doit = true;
    }
    
    //
    // Links into child models must now respect gene cis-reg modules if they are present, so
    // we may need to move link landings. NOTE: Example SpEndomes into Delta, I messed up and
    // landed the r11 VfG link into the pm link. This fixup moves the link target to r11 and issues
    // a warning: it DOES NOT find a VfG link that could fit the bill and replace the existing link.
    //
    
    Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla = fgho.fullyAnalyzeLinksIntoModules();
    if (FullGenomeHierarchyOracle.hasModuleProblems(gla)) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.moduleLinkErrors");
      Set<String> toFix = FullGenomeHierarchyOracle.geneModsNeedFixing(gla);
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx).getContextForRoot();
      for (String geneID : toFix) {
        List<String> lerrs = LinkSupport.fixCisModLinks(null, gla, rcx, geneID, true);
        for (String msg : lerrs) {
          message = message + msgDiv + msg;
        }
      }
      doit = true;
    }
    
    return ((doit) ? message : null);
  }

  /***************************************************************************
  ** 
  ** We need to fix genes that are too short
  */

  private void legacyIOFixupForGeneLength(Map<String, Integer> changes) {
    DBGenome genome = getRootDBGenome();
    for (String geneKey : changes.keySet()) {
      DBGene gene = (DBGene)genome.getGene(geneKey);
      gene.setPadCount(changes.get(geneKey).intValue()); 
    }
    return;
  } 
    
  /***************************************************************************
  ** 
  ** We need to install new line break info
  */

  List<String> legacyIOFixupForLineBreaks(FontManager fMgr) {

    ArrayList<String> fixes = new ArrayList<String>();
    AffineTransform trans = new AffineTransform();
    FontRenderContext frc = new FontRenderContext(trans, true, true);
    
    Iterator<String> lokit = layouts_.keySet().iterator();
    while (lokit.hasNext()) {
      String lokey = lokit.next();
      Layout layout = layouts_.get(lokey);
      String targ = layout.getTarget();
     
      Genome genome = getGenome(targ);
      Iterator<Node> it = genome.getAllNodeIterator();
      while (it.hasNext()) {
        Node node = it.next();
        String nodeID = node.getID();
        NodeProperties np = layout.getNodeProperties(nodeID);
        String breakDef = MultiLineRenderSupport.legacyLineBreaks(np.getHideName(), node.getNodeType(), frc, node.getName(), fMgr);
        if (breakDef != null) {
          System.err.println(node.getName() + " def = " + breakDef);
          np.setLineBreakDef(breakDef);
          fixes.add(node.getName());
        }
      }
    }
    return (fixes);
  }  
  
 /***************************************************************************
  ** 
  ** We need to kill off orphaned note properties
  */

  List<String> legacyIOFixupForOrphanedNotes() {

    ArrayList<String> fixes = new ArrayList<String>();
    
    HashSet<String> goodNoteKeys = new HashSet<String>();
    Iterator<GenomeInstance> iit = getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Iterator<Note> noteIt = gi.getNoteIterator();
      while (noteIt.hasNext()) {
        Note note = noteIt.next();
        goodNoteKeys.add(note.getID());
      }
    }
    Iterator<DynamicInstanceProxy> pit = getDynamicProxyIterator();
    while (pit.hasNext()) {
      DynamicInstanceProxy dip  = pit.next();
      Iterator<Note> noteIt = dip.getNoteIterator();
      while (noteIt.hasNext()) {
        Note note = noteIt.next();
        goodNoteKeys.add(note.getID());
      }
    }
    
    Iterator<String> lokit = layouts_.keySet().iterator();
    while (lokit.hasNext()) {
      String lokey = lokit.next();
      Layout layout = layouts_.get(lokey);
      HashSet<String> noteKeys = new HashSet<String>(layout.getNotePropertiesKeys());
      Iterator<String> nkit = noteKeys.iterator();
      while (nkit.hasNext()) {
        String noteKey = nkit.next();
        if (!goodNoteKeys.contains(noteKey)) {
          layout.removeNoteProperties(noteKey);
          fixes.add(noteKey);
        }       
      }
    }  
      
    return (fixes);
  }    
 
  /***************************************************************************
  ** 
  ** We need to enforce new node activity standards
  */

  List<String> legacyIOFixupForNodeActivities() {
    //
    // Has to proceed top down: 
    //
    ArrayList<String> retval = new ArrayList<String>();
    HashSet<String> processed = new HashSet<String>();
    int pCount = instances_.size(); 
    while (processed.size() < pCount) {
      Iterator<GenomeInstance> it = getInstanceIterator();    
      while (it.hasNext()) {
        GenomeInstance git = it.next();
        String myID = git.getID(); 
        if (processed.contains(myID)) {  // Skip what we have done...
          continue;
        }
        GenomeInstance parent = git.getVfgParent();
        if (parent != null) {
          String parentID = parent.getID();
          if (!processed.contains(parentID)) { // Skip if parent not done...
            continue;
          }
        }
        processed.add(myID);    
        git.fixupLegacyIONodeActivities(retval);
      }
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Get the model data
  */

  public ModelData getModelData() {
    return (modelData_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the model data
  */

  public void setModelData(ModelData md) {
    modelData_ = md;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Answer if we are using shared experimental data
  */

  public boolean amUsingSharedExperimentalData() {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    return (isDataSharing_.booleanValue());
  } 

  /***************************************************************************
  **
  ** Rollback a data undo transaction
  */
    
  public void rollbackDataUndoTransaction(DatabaseChange change) {
    databaseChangeUndo(change);
    return;
  }    
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startTimeCourseUndoTransaction() {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeCourses) {
      return (mb_.startSharedTimeCourseUndoTransaction());
    }
    DatabaseChange dc = new DatabaseChange();
    dc.oldTcd = localTimeCourse_;
    dc.localTcdChange = true;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishTimeCourseUndoTransaction(DatabaseChange change) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeCourses) {
      return (mb_.finishSharedTimeCourseUndoTransaction(change));
    }
    change.newTcd = localTimeCourse_;
    return (change);
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startTemporalInputUndoTransaction() {
    DatabaseChange dc = new DatabaseChange();
    dc.oldTir = rangeData_;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishTemporalInputUndoTransaction(DatabaseChange change) {
    change.newTir = rangeData_;
    return (change);
  }

  /***************************************************************************
  ** 
  ** Get the Temporal Range Data
  */

  public TemporalInputRangeData getTemporalInputRangeData() {
    return (rangeData_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the Temporal Range Data
  */

  public void setTemporalInputRangeData(TemporalInputRangeData rangeData) {
    rangeData_ = rangeData;
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  public PerturbationDataMaps getPerturbationDataMaps() {
    return (pdms_);
  }
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  public void setPerturbationDataMaps(PerturbationDataMaps pdms) {
    pdms_ = pdms;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  
  public TimeCourseDataMaps getTimeCourseDataMaps() {
    return (tcdm_);
  }
  
  /***************************************************************************
  ** 
  ** Now just hold onto data maps, not the underlying data:
  */
  
  
  public void setTimeCourseDataMaps(TimeCourseDataMaps tcdm) {
    tcdm_ = tcdm;
    return;
  }
  
  /***************************************************************************
  **
  ** Answers if we have data attached to our node by default (simple name match)
  ** 
  */
  
  public boolean hasDataAttachedByDefault(String nodeID) {
    nodeID = GenomeItemInstance.getBaseID(nodeID);
    PerturbationData pd = getPertData();
    PerturbationDataMaps pdms = getPerturbationDataMaps();
    TimeCourseData tcd = getTimeCourseData();

    if (pd.haveDataForNode(genome_, nodeID, null, pdms_) && !pdms.haveCustomMapForNode(nodeID)) {
      return (true);
    }
    
    TimeCourseDataMaps tcdm = getTimeCourseDataMaps();
    if (tcdm.haveDataForNode(tcd, nodeID, this) && !tcdm.haveCustomMapForNode(nodeID)) {
      return (true);
    }
    TemporalInputRangeData tird = getTemporalInputRangeData();
    if (tird.haveDataForNode(nodeID, this) && !tird.haveCustomMapForNode(nodeID)) {
      return (true);
    }    
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answers if we have region data attached to our region by default (simple name match)
  ** 
  */
  
  public boolean hasRegionAttachedByDefault(Group group) {
    TimeCourseDataMaps tcdm = getTimeCourseDataMaps();
    TimeCourseData tcd = getTimeCourseData();
    String groupName = group.getName();
    String groupID = group.getID();
    if (DataUtil.containsKey(tcd.getRegions(), groupName) &&     
        !tcdm.haveCustomMapForRegion(groupID)) {
      return (true);
      
    }
    TemporalInputRangeData tird = getTemporalInputRangeData();
    if (DataUtil.containsKey(tird.getRegions(), groupName) && 
        !tird.haveCustomMapForRegion(groupID)) {
      return (true);
    } 
    return (false);
  } 

  /***************************************************************************
  **
  ** Answers if other top-level genome instances have regions named the same
  ** as this one which are attached by default.
  ** 
  */
  
  public boolean hasOtherRegionsAttachedByDefault(GenomeInstance gi, Group group) {
    if (gi.getVfgParent() != null) {
      throw new IllegalArgumentException();
    }
    Iterator<GenomeInstance> iit = getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance tgi = iit.next();
      // root instances only
      if (tgi.getVfgParent() != null) {
        continue;
      }
      if (tgi == gi) {
        continue;
      }
      Iterator<Group> git = tgi.getGroupIterator();
      while (git.hasNext()) {
        Group testGroup = git.next();
        String tgName = testGroup.getName();
        if ((tgName != null) && DataUtil.keysEqual(tgName, group.getName())) {
          if (hasRegionAttachedByDefault(testGroup)) {
            return (true);
          }
        }
      }
    }
    return (false);
  }  

  /***************************************************************************
  ** 
  ** Answer if any model has time bounds set
  */

  public boolean modelsHaveTimeBounds() {
    if (!dynProxies_.isEmpty()) {
      return (true);
    }
    Iterator<GenomeInstance> iit = instances_.values().iterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      if (gi.hasTimeBounds()) {
        return (true);
      } 
    }
    return (false);
  }
  
  /***************************************************************************
  ** 
  ** Get the startupView, with first view overlay preference installed
  */

  public StartupView getStartupView() {
    if (startupView_ == null)  {
      return (null);
    }
    //
    // Do late binding of first overlay preference by finding out
    // now:
    //
    
    String modelID = startupView_.getModel();
    if (modelID != null) {
      NetOverlayOwner owner = getOverlayOwnerFromGenomeKey(modelID);
      TaggedSet modChoice = new TaggedSet();
      TaggedSet revChoice = new TaggedSet();
      String ovrID =  owner.getFirstViewPreference(modChoice, revChoice); // (owner == null ? null : owner.getFirstViewPreference(modChoice, revChoice));
      if (ovrID != null) {
        StartupView modified = new StartupView(modelID, ovrID, modChoice, revChoice, null, startupView_.getNodeType());
        return (modified);
      }
    }
   
    return (startupView_);
  }  
  
  /***************************************************************************
  ** 
  ** Get the startupView
  */

  public DatabaseChange setStartupView(StartupView startupView) {
    DatabaseChange dc = new DatabaseChange();
    dc.oldStartupView = startupView_.clone();
    startupView_ = startupView;
    dc.newStartupView = startupView_.clone();
    return (dc);
  }  

  /***************************************************************************
  ** 
  ** Get the workspace definition
  */

  public Workspace getWorkspace() {
    return (workspace_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the workspace definition with undo.  Do not use.
  */

  public void simpleSetWorkspace(Workspace workspace) {
    throw new UnsupportedOperationException();
  }  
  
  /***************************************************************************
  ** 
  ** Set the workspace definition
  */

  public DatabaseChange setWorkspace(Workspace workspace) {
    DatabaseChange dc = new DatabaseChange();
    dc.oldWorkspace = workspace_.clone();
    workspace_ = workspace;
    dc.newWorkspace = workspace_.clone();
    return (dc);
  }
  
  /***************************************************************************
  ** 
  ** Used for legacy loads
  */

  public void setWorkspaceNeedsCenter() {
    workspace_.setNeedsCenter(true);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the core genome
  */

  public DBGenome getRootDBGenome() {
    return (genome_);
  }  

  /***************************************************************************
  ** 
  ** Get the genome count
  */

  public int getGenomeCount() {
    return (instances_.size() + 1);
  }  
  
  /***************************************************************************
  ** 
  ** Get the given genome.  If a key to a dynamic instance is provided, it
  ** will return it.
  */

  public Genome getGenome(String key) {
    // HACK FIX ME
    if (key.equals("bioTapA")) {
      return (genome_);
    }
    Genome retval = instances_.get(key);
    if (retval != null) {
      return (retval);
    }
    if (DynamicInstanceProxy.isDynamicInstance(key)) {
      String proxKey = DynamicInstanceProxy.extractProxyID(key);
      DynamicInstanceProxy dip = dynProxies_.get(proxKey);
      if (dip == null) {
        return (null);
      }
      DynamicGenomeInstance dgi = dip.getProxiedInstance(key);
      if (dgi != null) {
        return (dgi);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  ** 
  ** Get the given layout
  */

  public Layout getLayout(String key) {
    return (layouts_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get the layout for the DBGenome:
  **
  */
  
  public Layout getRootLayout() {
    return (getLayoutForGenomeKey(getRootDBGenome().getID()));
  } 
  
  /***************************************************************************
  ** 
  ** Get the layout key used for the given genome key
  */
  
  public String mapGenomeKeyToLayoutKey(String genomeKey) {
    if (genomeKey == null) {
      return (null);
    } else {
      Genome gen = getGenome(genomeKey);
      // For subset instances, look for the layout to the source
      if (gen instanceof GenomeInstance) {  // FIX ME
        GenomeInstance parent = ((GenomeInstance)gen).getVfgParentRoot();
        if (parent != null) {
          genomeKey = parent.getID();
        }
      } 
      Iterator<Layout> lit = getLayoutIterator();
      while (lit.hasNext()) {
        Layout lo = lit.next();
        String targ = lo.getTarget();
        if (targ.equals(genomeKey)) {
          return (lo.getID());
        }
      }
      return (null);
    }
  }

  /***************************************************************************
  **
  ** Go from Genome ID key to Layout
  */
  
  public Layout getLayoutForGenomeKey(String key) {
    return (getLayout(mapGenomeKeyToLayoutKey(key)));
  } 
  
  /***************************************************************************
  **
  ** Get Xplat text from a genome key
  */ 
    
  public XPlatDisplayText getTextFromGenomeKey(String key) {
    if (key == null) {
      return (null);
    }
    if (DynamicInstanceProxy.isDynamicInstance(key)) {
      String proxKey = DynamicInstanceProxy.extractProxyID(key);
      DynamicInstanceProxy fromProx = getDynamicProxy(proxKey);
      if (fromProx == null) {
        throw new IllegalArgumentException();
      }
      return (fromProx.getAllDisplayText());
    } else {
      Genome genOwn = getGenome(key);
      if (genOwn == null) {
        throw new IllegalArgumentException();
      }     
      return (genOwn.getAllDisplayText());
    }
  }

  /***************************************************************************
  **
  ** Get the overlay owner from a genome key
  */ 
    
  public NetOverlayOwner getOverlayOwnerFromGenomeKey(String key) {
    if (DynamicInstanceProxy.isDynamicInstance(key)) {
      // Was previously tracking the state here for issue #44, but no longer reproducing.
      String proxKey = DynamicInstanceProxy.extractProxyID(key);
      NetOverlayOwner fromProx = getDynamicProxy(proxKey);
      if (fromProx == null) {
        throw new IllegalArgumentException();
      }
      return (fromProx);
    } else {
      NetOverlayOwner genOwn = getGenome(key);
     // if (genOwn == null) {
     //   throw new IllegalArgumentException();
     // }     
      return (genOwn);
    }
  }
    
  /***************************************************************************
  **
  ** Get the overlay owner from an owner key (e.g. static genome or proxy key)
  */ 
    
  public NetOverlayOwner getOverlayOwnerWithOwnerKey(String key) {
    NetOverlayOwner fromProx = getDynamicProxy(key);
    if (fromProx != null) {
      return (fromProx);
    }
    NetOverlayOwner genOwn = getGenome(key);
    if (genOwn == null) {
      throw new IllegalArgumentException();
    }  
    return (genOwn);
  } 
  
  /***************************************************************************
  ** 
  ** Clear out all dynamic proxies
  */

  public void clearAllDynamicProxyCaches() {
    Iterator<DynamicInstanceProxy> dpit = dynProxies_.values().iterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      dip.clearCache();
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get the given dynamic proxy
  */

  public DynamicInstanceProxy getDynamicProxy(String key) {
    return (dynProxies_.get(key));
  }
  
  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxy(String key, DynamicInstanceProxy dip) {
    DatabaseChange retval = new DatabaseChange();
    retval.newProxy = dip;    
    if (key == null) {
      key = labels_.getNextLabel();
    } else if (!labels_.addExistingLabel(key)) {
      System.err.println("Don't like " + key);
      throw new IllegalArgumentException();
    }
    if (dynProxies_.get(key) != null) {
      System.err.println(labels_ + " key = " + key);
      throw new IllegalArgumentException();
    }
    dynProxies_.put(key, dip);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Add the given dynamic proxy
  */

  public DatabaseChange addDynamicProxyExistingLabel(String key, DynamicInstanceProxy dip) {
    DatabaseChange retval = new DatabaseChange();
    retval.newProxy = dip;     
    if (key == null) {
      key = labels_.getNextLabel();
    } else if (dynProxies_.get(key) != null) {
      System.err.println(labels_ + " key = " + key);
      throw new IllegalArgumentException();
    }
    dynProxies_.put(key, dip);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Remove a dynamic proxy
  */

  public DatabaseChange[] removeDynamicProxy(String key) {
    boolean dropStartup = key.equals(startupView_.getModel());
    DatabaseChange[] retval = new DatabaseChange[(dropStartup) ? 2 : 1];
    retval[0] = new DatabaseChange();
    retval[0].oldProxy = dynProxies_.remove(key);
    if (retval[0].oldProxy == null) {
      throw new IllegalArgumentException();
    }
    
    labels_.removeLabel(key);
    
    if (dropStartup) {
      retval[1] = new DatabaseChange();  
      retval[1].oldStartupView = startupView_.clone();
      startupView_ = startupView_.dropModel();
      retval[1].newStartupView = startupView_.clone();
    }
    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Set the core genome
  */

  public void setGenome(DBGenome genome) {
    if (genome_ != null) {
      throw new IllegalArgumentException();
    } else {
      labels_.addExistingLabel("bioTapA");
      genome_ = genome;
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstanceExistingLabel(String key, GenomeInstance genome) {
    DatabaseChange retval = new DatabaseChange();
    retval.newInstance = genome;     
    if (key == null) {
      key = labels_.getNextLabel();
    }
    if (instances_.get(key) != null) {
      throw new IllegalArgumentException();
    }
    instances_.put(key, genome);
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Add the given genome instance
  */

  public DatabaseChange addGenomeInstance(String key, GenomeInstance genome) {
    DatabaseChange retval = new DatabaseChange();
    retval.newInstance = genome;    
    if (key == null) {
      key = labels_.getNextLabel();
    } else if (!labels_.addExistingLabel(key)) {
      System.err.println("Don't like " + key);
      throw new IllegalArgumentException();
    }
    if (instances_.get(key) != null) {
      throw new IllegalArgumentException();
    }
    instances_.put(key, genome);
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Add the given layout
  */

  public DatabaseChange addLayout(String key, Layout layout) {
    DatabaseChange retval = new DatabaseChange();
    retval.newLayout = layout;
    if (key == null) {
      key = labels_.getNextLabel();
    } else if (layouts_.get(key) != null) {
      throw new IllegalArgumentException();
    }
    // For V8, now seeing that layout brought in from IO is not registering with
    // the label generator. Need to do this, so that system does not hand out a
    // tag already used by a layout. But we will not throw and error if the label
    // has been used by e.g. a genome.
    //
    UiUtil.fixMePrintout("NO causes old IO fails because e.g. proxy loaded later has same key as prev. layout");
  //  labels_.addExistingLabel(key);
 
    layouts_.put(key, layout);
    return (retval);
  }

  /***************************************************************************
  **
  ** Add a build instruction
  */
  
  public void addBuildInstruction(BuildInstruction bi) {
    //
    // Support for legacy IO: assign ID if none given:
    //
    if (bi.getID() == null) {
      bi.setID(instructionLabels_.getNextLabel());
    } else if (!instructionLabels_.addExistingLabel(bi.getID())) {
      throw new IllegalArgumentException();
    }
    buildInstructions_.add(bi);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the next instruction label
  */
  
  public String getNextInstructionLabel() {
    return (instructionLabels_.getNextLabel());    
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the build instructions
  */
  
  public Iterator<BuildInstruction> getBuildInstructions() {
    return (buildInstructions_.iterator());
  }
  
  /***************************************************************************
  **
  ** Answer if we have build instructions
  */
  
  public boolean haveBuildInstructions() {
    return (buildInstructions_.size() > 0);
  }  
  
  /***************************************************************************
  **
  ** Set the build instruction set
  */
  
  public DatabaseChange setBuildInstructions(List<BuildInstruction> inst) {
    DatabaseChange retval = new DatabaseChange();
    retval.oldBuildInst = buildInstructions_;
    buildInstructions_ = deepCopyBuildInstr(inst);
    retval.newBuildInst = deepCopyBuildInstr(inst);
    stockInstructionLabels();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a build instruction (kinda crappy)
  */
  
  public BuildInstruction getBuildInstruction(String idTag) {
    int numbi = buildInstructions_.size();
    for (int i = 0; i < numbi; i++) {
      BuildInstruction bi = buildInstructions_.get(i);
      if (idTag.equals(bi.getID())) {
        return (bi);
      }
    }
    throw new IllegalArgumentException();
  }
   
  /***************************************************************************
  **
  ** Drop the build instruction set
  */
  
  public DatabaseChange dropBuildInstructions() {
    DatabaseChange retval = new DatabaseChange();
    retval.oldBuildInst = buildInstructions_;
    buildInstructions_ = new ArrayList<BuildInstruction>();
    retval.newBuildInst = new ArrayList<BuildInstruction>();
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Bring instruction labels up to date
  */
  
  private void stockInstructionLabels() {
    instructionLabels_ = new UniqueLabeller();
    Iterator<BuildInstruction> biit = getBuildInstructions();
    while (biit.hasNext()) {
      BuildInstruction bi = biit.next();
      instructionLabels_.addExistingLabel(bi.getID());
    }
    return;
  }  

  /***************************************************************************
  **
  ** Add an instance Instruction Set
  */
  
  public void addInstanceInstructionSet(String id, InstanceInstructionSet iis) {
    instanceInstructionSets_.put(id, iis);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the given instance instruction set
  */
  
  public InstanceInstructionSet getInstanceInstructionSet(String key) {
    InstanceInstructionSet retval = instanceInstructionSets_.get(key);
    return (retval);
  }

  /***************************************************************************
  **
  ** Remove the instance instruction set
  */
  
  public DatabaseChange removeInstanceInstructionSet(String key) {
    DatabaseChange retval = new DatabaseChange();    
    retval.instructSetKey = key;
    retval.oldInstructSet = instanceInstructionSets_.get(key);
    instanceInstructionSets_.remove(key);
    retval.newInstructSet = null;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Set the instance instruction set
  */
  
  public DatabaseChange setInstanceInstructionSet(String key, InstanceInstructionSet iis) {
    DatabaseChange retval = new DatabaseChange();    
    retval.instructSetKey = key;
    retval.oldInstructSet = instanceInstructionSets_.get(key);
    instanceInstructionSets_.put(key, new InstanceInstructionSet(iis));
    retval.newInstructSet =  new InstanceInstructionSet(iis);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startLayoutUndoTransaction(String key) {
    DatabaseChange retval = new DatabaseChange();
    Layout lo = layouts_.get(key);
    retval.oldLayout = new Layout(lo);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishLayoutUndoTransaction(DatabaseChange change) {
    String key = change.oldLayout.getID();
    Layout lo = layouts_.get(key);
    change.newLayout = new Layout(lo);
    return (change);
  }
  
  /***************************************************************************
  **
  ** Rollback an undo transaction
  */
  
  public void rollbackLayoutUndoTransaction(DatabaseChange change) {
    databaseChangeUndo(change);
    return;
  }  

  /***************************************************************************
  ** 
  ** Get the Model Tree
  */

  public NavTree getModelHierarchy() {
    return (navTree_);
  }  
  
  /***************************************************************************
  **
  ** Get an Iterator over the genome Instances. DOES NOT iterate over dynamic
  ** instances handled by a proxy.
  **
  */
  
  public Iterator<GenomeInstance> getInstanceIterator() {
    return (instances_.values().iterator());
  }
  
  /***************************************************************************
  ** 
  ** Get an iterator over the dynamic proxies
  */

  public Iterator<DynamicInstanceProxy> getDynamicProxyIterator() {
    return (dynProxies_.values().iterator());
  } 
  
  /***************************************************************************
  **
  ** Get an Iterator over the layouts:
  **
  */
  
  public Iterator<Layout> getLayoutIterator() {
    return (layouts_.values().iterator());
  }
  
  /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getNextKey() {
    return (labels_.getNextLabel());
  }

  /***************************************************************************
  ** 
  ** Get a unique model name
  */

  public String getUniqueModelName(ResourceManager rMan) {
 
    String format = rMan.getString("model.defaultNameFormat");   
    while (true) {
      Integer suffix = new Integer(uniqueNameSuffix_++);
      String tryName = MessageFormat.format(format, new Object[] {suffix});
      if (format.equals(tryName)) { // Avoid infinite loops with punk resource file
        throw new IllegalStateException();
      }
      Iterator<GenomeInstance> iit = getInstanceIterator();
      boolean noMatch = true;
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (gi.getName().equals(tryName)) {
          noMatch = false;
          break;
        }
      }
      if (!noMatch) {
        continue;
      }
      Iterator<DynamicInstanceProxy> dpit = getDynamicProxyIterator();      
      while (dpit.hasNext()) {
        DynamicInstanceProxy dip = dpit.next();
        if (dip.getName().equals(tryName)) {
          noMatch = false;
          break;
        }
      }
      if (noMatch) {
        return (tryName);
      }
    }
  }
  
  /***************************************************************************
  ** 
  ** Remove an instance
  */

  public DatabaseChange[] removeInstance(String key) {
    boolean dropStartup = key.equals(startupView_.getModel());
    DatabaseChange[] retval = new DatabaseChange[(dropStartup) ? 2 : 1]; 
    retval[0] = new DatabaseChange();
    retval[0].oldInstance = instances_.remove(key);
    if (retval[0].oldInstance == null) {
      throw new IllegalArgumentException();
    }
    
    labels_.removeLabel(key);
    
    if (dropStartup) {
      retval[1] = new DatabaseChange();  
      retval[1].oldStartupView = startupView_.clone();
      startupView_ = startupView_.dropModel();
      retval[1].newStartupView = startupView_.clone();
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Remove a layout
  */

  public DatabaseChange removeLayout(String key) {
    DatabaseChange retval = new DatabaseChange();
    retval.oldLayout = layouts_.remove(key);
    labels_.removeLabel(key);
    return (retval);    
  }  
  
  /***************************************************************************
  **
  ** Dump the database to the given file using XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {    
    ind.indent();
    out.print("<Database tabNum=\"");
    out.print(index_);
    out.print("\" id=\"");
    out.print(id_);
    out.print("\" sharing=\"");
    out.print(isDataSharing_.booleanValue());
    out.println("\" >");
    
    tabData_.writeXML(out, ind.up());
    ind.down();
    
    if (modelData_ != null) {
      modelData_.writeXML(out, ind.up());
      ind.down();
    }
    
    if ((localTimeAxis_ != null) && localTimeAxis_.isInitialized()) {
      localTimeAxis_.writeXML(out, ind.up());
      ind.down();
    }   

    if (workspace_ != null) {
      workspace_.writeXML(out, ind.up());
      ind.down();
    }  
    
    if (startupView_ != null) {
      startupView_.writeXML(out, ind.up());
      ind.down();
    }    
    
    uics_.getPathMgr().writeXML(out, ind.up());
    ind.down();
    
    navTree_.writeXML(out, ind.up());
    ind.down();
    
    ind.up().indent();
    out.println("<modelTree>");
    
    // core genome
    genome_.writeXML(out, ind.up());
    ind.indent();
    //
    // Dump the instances out in tree order to preserve it:
    //
    List<String> ordered = navTree_.getPreorderListing(true);
    Iterator<String> oit = ordered.iterator();    
    out.println("<genomeInstances>");
    ind.up();
    while (oit.hasNext()) {
      String gkey = oit.next();
      GenomeInstance gi = (GenomeInstance)getGenome(gkey);
      // Note that dynamic instances override writeXML to output nothing. We lose
      // the tree ordering of dynamic vs. static children on I/O!!!  FIX ME!!!! 
      gi.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</genomeInstances>");
    
    //
    // Dump the single-shot proxies out in tree order to preserve it:
    //
    ArrayList<String> dumpList = new ArrayList<String>();
    ordered = navTree_.getPreorderListing(true);
    oit = ordered.iterator();    
    while (oit.hasNext()) {
      String gkey = oit.next();
      GenomeInstance gi = (GenomeInstance)getGenome(gkey);
      if (gi instanceof DynamicGenomeInstance) {
        DynamicGenomeInstance dgi = (DynamicGenomeInstance)gi;
        DynamicInstanceProxy dip = dynProxies_.get(dgi.getProxyID()); 
        if (!dumpList.contains(dip.getID())) {
          dip.writeXML(out, ind);
          dumpList.add(dip.getID());
        }
      }
    }
    //
    // Dump the multi-hour proxies out in tree order to preserve it:
    //
    dumpList = new ArrayList<String>();
    ordered = navTree_.getProxyPreorderListing();
    oit = ordered.iterator();    
    while (oit.hasNext()) {
      String pkey = oit.next();
      DynamicInstanceProxy dip = dynProxies_.get(pkey); 
      if (!dumpList.contains(dip.getID())) {
        dip.writeXML(out, ind);
        dumpList.add(dip.getID());
      }
    }
 
    ind.down().indent();
    out.println("</modelTree>");
    
    // Layouts
    ind.indent();
    out.println("<layouts>");
    Iterator<Layout> lit = getLayoutIterator();
    ind.up();
    while (lit.hasNext()) {
      Layout lo = lit.next();
      lo.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</layouts>");
    
    if (localPertData_ != null) {
      localPertData_.writeXML(out, ind);
    }
    
    if (localTimeCourse_ != null) {
      localTimeCourse_.writeXML(out, ind);
    }
  
    if ((localCopiesPerEmb_ != null) && localCopiesPerEmb_.haveData()) {
      localCopiesPerEmb_.writeXML(out, ind);
    }    
  
    if (tcdm_ != null) {
      tcdm_.writeXML(out, ind);
    }
    
    if (pdms_ != null) {
      pdms_.writeXML(out, ind);
    }  

    if (rangeData_ != null) {
      rangeData_.writeXML(out, ind);
    }

    //
    // Dump the build instructions:
    //
    
    Iterator<BuildInstruction> biit = getBuildInstructions();
    if (biit.hasNext()) {
      ind.indent();
      out.println("<buildInstructions>");
      ind.up();
      while (biit.hasNext()) {
        BuildInstruction bi = biit.next();
        bi.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</buildInstructions>");
    }
    
    //
    // Dump the instance instruction sets:
    //
    
    Iterator<InstanceInstructionSet> isit = instanceInstructionSets_.values().iterator();
    if (isit.hasNext()) {
      ind.indent();
      out.println("<instructionSets>");
      ind.up();
      while (isit.hasNext()) {
        InstanceInstructionSet iis = isit.next();
        iis.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</instructionSets>");
    } 

    if (getMbDB() != null) {
      getMbDB().writeXML(out, ind);  
    }
  
    ind.down().indent();
    out.println("</Database>");
    return;
  }

  
  /***************************************************************************
  **
  ** Called when model has changed
  */   

  public void modelHasChanged(ModelChangeEvent event, int remaining) {
    if (remaining == 0) {
      modelHasChanged(event);
    }
    return;
  }     
  
  /***************************************************************************
  ** 
  ** Notify listener of model change
  */
  
  public void modelHasChanged(ModelChangeEvent mcev) {
    clearAllDynamicProxyCaches();
    return;
  }

  /***************************************************************************
  ** 
  ** Notify listener of model change
  */
  
  public void generalChangeOccurred(GeneralChangeEvent gcev) {
    clearAllDynamicProxyCaches();
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void databaseChangeUndo(DatabaseChange undo) {
    if (undo.oldInstance != null) {
      String key = undo.oldInstance.getID();
      instances_.put(key, undo.oldInstance);
      labels_.addExistingLabel(key);  
    } else if (undo.newInstance != null) {   
      String key = undo.newInstance.getID();
      instances_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.oldLayout != null) {
      String key = undo.oldLayout.getID();
      layouts_.put(key, new Layout(undo.oldLayout));
      labels_.addExistingLabel(key);
    } else if (undo.newLayout != null) { 
      String key = undo.newLayout.getID();
      layouts_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.oldProxy != null) {
      String key = undo.oldProxy.getID();
      dynProxies_.put(key, undo.oldProxy);
      labels_.addExistingLabel(key);
    } else if (undo.newProxy != null) {   
      String key = undo.newProxy.getID();
      dynProxies_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.oldTir != null) {
      rangeData_ = undo.oldTir;
    } else if (undo.oldBuildInst != null) {
      buildInstructions_ = undo.oldBuildInst;
      stockInstructionLabels();
    } else if (undo.instructSetKey != null) {
      if (undo.oldInstructSet != null) {
        instanceInstructionSets_.put(undo.instructSetKey, undo.oldInstructSet);
      } else {
        instanceInstructionSets_.remove(undo.instructSetKey);
      }
    } else if (undo.oldGenome != null) {
      genome_ = undo.oldGenome;
    } else if (undo.oldWorkspace != null) {
      workspace_ = undo.oldWorkspace;
    } else if (undo.oldStartupView != null) {
      startupView_ = undo.oldStartupView;
    } else if (undo.oldTabData != null) {
      tabData_ = undo.oldTabData;   
    } else if ((undo.newModelBuilderData != null) || (undo.oldModelBuilderData != null)) {
      if (getMbDB() == null) {
        throw new IllegalStateException();
      }
      getMbDB().handleWorksheetRunUndo(undo.newModelBuilderData, undo.oldModelBuilderData);  
    }
    // May be called if oldGenome or oldInstance != null
    if (undo.oldLayouts != null) {
      Iterator<String> olit = undo.oldLayouts.keySet().iterator();
      while (olit.hasNext()) {
        String name = olit.next();
        Layout lo = undo.oldLayouts.get(name);
        Layout nlo = new Layout(lo);
        layouts_.put(name, nlo);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void databaseChangeRedo(DatabaseChange undo) {
    if (undo.newInstance != null) {
      String key = undo.newInstance.getID();
      instances_.put(key, undo.newInstance);
      labels_.addExistingLabel(key);  
    } else if (undo.oldInstance != null) {   
      String key = undo.oldInstance.getID();
      instances_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.newLayout != null) {
      String key = undo.newLayout.getID();
      layouts_.put(key, new Layout(undo.newLayout));
      labels_.addExistingLabel(key);
    } else if (undo.oldLayout != null) {
      String key = undo.oldLayout.getID();
      layouts_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.newProxy != null) {
      String key = undo.newProxy.getID();
      dynProxies_.put(key, undo.newProxy);
      labels_.addExistingLabel(key);  
    } else if (undo.oldProxy != null) {   
      String key = undo.oldProxy.getID();
      dynProxies_.remove(key);
      labels_.removeLabel(key);
    } else if (undo.newTir != null) {
      rangeData_ = undo.newTir;
    } else if (undo.newBuildInst != null) {
      buildInstructions_ = undo.newBuildInst;
      stockInstructionLabels();
    } else if (undo.instructSetKey != null) {
      if (undo.newInstructSet != null) {
        instanceInstructionSets_.put(undo.instructSetKey, undo.newInstructSet);
      } else {
        instanceInstructionSets_.remove(undo.instructSetKey);
      }      
    } else if (undo.newGenome != null) {
      genome_ = undo.newGenome;
    } else if (undo.newWorkspace != null) {
      workspace_ = undo.newWorkspace;
    } else if (undo.newStartupView != null) {
      startupView_ = undo.newStartupView;
    } else if (undo.newTabData != null) {
      tabData_ = undo.newTabData;
    } else if ((undo.newModelBuilderData != null) || (undo.oldModelBuilderData != null)) {
      if (getMbDB() == null) {
        throw new IllegalStateException();
      }
      getMbDB().handleWorksheetRunRedo(undo.newModelBuilderData, undo.oldModelBuilderData);  
    }
    // May be called if newGenome or newInstance != null
    if (undo.newLayouts != null) {
      Iterator<String> nlit = undo.newLayouts.keySet().iterator();
      while (nlit.hasNext()) {
        String name = nlit.next();
        Layout lo = undo.newLayouts.get(name);
        Layout nlo = new Layout(lo);
        layouts_.put(name, nlo);
      }          
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Return the least used colors from the list of colors
  */
  
  public void getRarestColors(List<String> geneColors, List<String> rareColors, Genome genome, Layout layout) {
    
    HashMap<String, Integer> colorCounts = new HashMap<String, Integer>();
    int gcSize = geneColors.size();
    for (int i = 0; i < gcSize; i++) {
      String col = geneColors.get(i);
      colorCounts.put(col, new Integer(0));
    }
   
    //
    // Crank thru all the genes and get their colors.  Figure out the set of colors
    // with the minimum count.  Choose a color from that set.
    //    
 
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      NodeProperties np = layout.getNodeProperties(gene.getID());
      String col = np.getColorName();
      if (geneColors.contains(col)) {
        Integer count = colorCounts.get(col);
        colorCounts.put(col, new Integer(count.intValue() + 1));
      }
    }
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (NodeProperties.setWithLinkColor(node.getNodeType())) {
        NodeProperties srcProp = layout.getNodeProperties(node.getID());
        // FIX ME: Abstract this away!
        String col = "black";
        if (node.getNodeType() == Node.INTERCELL) {
          col = srcProp.getSecondColorName();
          if (col == null) {
            col = srcProp.getColorName();
          }
        } else {
          col = srcProp.getColorName();
        }
        if (geneColors.contains(col)) {
          Integer count = colorCounts.get(col);
          colorCounts.put(col, new Integer(count.intValue() + 1));
        }
      }
    }

    //
    // Minimum count colors:
    //
    
    int minVal = Integer.MAX_VALUE;
    HashSet<String> minSet = new HashSet<String>();
    Iterator<String> cckit = colorCounts.keySet().iterator();
    while (cckit.hasNext()) {
      String col = cckit.next();
      int count = colorCounts.get(col).intValue();
      if (count < minVal) {
        minSet.clear();
        minVal = count;
        minSet.add(col);
      } else if (count == minVal) {
        minSet.add(col);
      }
    }
 
    //
    // get a color from minimum set in the predefined order:
    //
    
    rareColors.clear();
    for (int i = 0; i < gcSize; i++) {
      String retval = geneColors.get(i);
      if (minSet.contains(retval)) {
        rareColors.add(retval);
      }
    }
    
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("Database");
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Return our tab number
  **
  */
  
  public static int extractTab(String elemName, Attributes attrs) throws IOException {
    String tabStr = AttributeExtractor.extractAttribute(elemName, attrs, "Database", "tabNum", true);
    try {
      Integer tabNum = Integer.parseInt(tabStr);
      return (tabNum.intValue());
    } catch (NumberFormatException nfex) {
      throw new IOException();
    }
  }
  
  /***************************************************************************
  **
  ** Return our DBID
  **
  */
  
  public static String extractID(String elemName, Attributes attrs) throws IOException {
    return (AttributeExtractor.extractAttribute(elemName, attrs, "Database", "id", true));
  }

  /***************************************************************************
  **
  ** Return our data sharing state
  **
  */
  
  public static Boolean extractSharing(String elemName, Attributes attrs) throws IOException {
    String shStr = AttributeExtractor.extractAttribute(elemName, attrs, "Database", "sharing", true);
    return (Boolean.valueOf(shStr)); 
  }
  
  /***************************************************************************
  ** 
  ** Get the Time Course Data
  */

  public TimeCourseData getTimeCourseData() {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeCourses) {
      return (mb_.getSharedTimeCourseData());
    } else {  
      return (localTimeCourse_);
    }
  }

  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalTimeCourseData(TimeCourseData timeCourseData) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    // Handles "Empty" sets sitting in XML input
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeCourses) {
      return;
    }
    localTimeCourse_ = timeCourseData;
    return;
  } 
  
   
  /***************************************************************************
  ** 
  ** Set the Time Course Data
  */

  public DatabaseChange setTimeCourseData(TimeCourseData timeCourseData) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeCourses) {
      return (mb_.setSharedTimeCourseData(timeCourseData));
    } else {   
      DatabaseChange dc = new DatabaseChange();
      dc.oldTcd = (localTimeCourse_ == null) ? null : localTimeCourse_.clone();
      this.localTimeCourse_ = timeCourseData;
      dc.newTcd = (localTimeCourse_ == null) ? null : localTimeCourse_.clone();
      return (dc);
    }
  }  

  /***************************************************************************
  ** 
  ** Get the perturbation data
  */

  public PerturbationData getPertData() {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerts) {
      return (mb_.getSharedPertData());
    } else {
      return (localPertData_);
    }
  }
 
  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalPertData(PerturbationData pd) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    // Handles "Empty" sets sitting in XML input
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerts) {
      return;
    }
    localPertData_ = pd;
    return;
  } 

  /***************************************************************************
  ** 
  ** Set the perturbation data
  */

  public DatabaseChange setPertData(PerturbationData pd) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerts) {
      return (mb_.setSharedPertData(pd));
    } else {
      DatabaseChange dc = new DatabaseChange();
      dc.localPertDataChange = true;
      dc.oldSharedPertData = (localPertData_ == null) ? null : localPertData_.clone();
      this.localPertData_ = pd;
      dc.newSharedPertData = (localPertData_ == null) ? null : localPertData_.clone();
      return (dc);
    }
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startCopiesPerEmbryoUndoTransaction() {
    return (mb_.startSharedCopiesPerEmbryoUndoTransaction());  
  }
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishCopiesPerEmbryoUndoTransaction(DatabaseChange change) {
    return (mb_.finishSharedCopiesPerEmbryoUndoTransaction(change));    
  }
  
  /***************************************************************************
  ** 
  ** Get the copies per embryo data
  */

  public CopiesPerEmbryoData getCopiesPerEmbryoData() { 
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerEmbryoCounts) {
      return (mb_.getSharedCopiesPerEmbryoData());
    } else {
      return (localCopiesPerEmb_);
    }
  }
  
  /***************************************************************************
  ** 
  ** Set the local copies for IO
  */

  public void installLocalCopiesPerEmbryoData(CopiesPerEmbryoData copies) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerEmbryoCounts) {
      return;
    }
    localCopiesPerEmb_ = copies;
    return;
  }  

  /***************************************************************************
  ** 
  ** Set the copies per embryo data
  */

  public DatabaseChange setCopiesPerEmbryoData(CopiesPerEmbryoData copies) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().sharePerEmbryoCounts) {
      return (mb_.setSharedCopiesPerEmbryoData(copies));
    } else {
      DatabaseChange dc = new DatabaseChange();
      dc.oldCpe = (localCopiesPerEmb_ == null) ? null : localCopiesPerEmb_.clone();
      localCopiesPerEmb_ = copies;
      dc.newCpe = (localCopiesPerEmb_ == null) ? null : localCopiesPerEmb_.clone();
      return (dc);
    }
  }
  
  /***************************************************************************
  ** 
  ** Get the time axis definition
  */

  public TimeAxisDefinition getTimeAxisDefinition() {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeUnits) {
      return (mb_.getSharedTimeAxisDefinition());
    } else {
      return (localTimeAxis_);
    }
  }

  /***************************************************************************
  ** 
  ** Set the time axis definition for IO
  */

  public void installLocalTimeAxisDefinition(TimeAxisDefinition timeAxis) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue()) {
      return;
    }
    localTimeAxis_ = timeAxis;
    return;
  }  

  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public DatabaseChange setTimeAxisDefinition(TimeAxisDefinition timeAxis) {
    if (isDataSharing_ == null) {
      throw new IllegalStateException();
    }
    if (isDataSharing_.booleanValue() && mb_.getDataSharingPolicy().shareTimeUnits) {
      return (mb_.setSharedTimeAxisDefinition(timeAxis));
    } else {
      DatabaseChange dc = new DatabaseChange();
      dc.oldTimeAxis = (localTimeAxis_ == null) ? null : localTimeAxis_.clone();
      localTimeAxis_ = timeAxis;
      dc.newTimeAxis = (localTimeAxis_ == null) ? null : localTimeAxis_.clone();
      return (dc);
    }
  }
 
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public void installLegacyTimeAxisDefinition(DataAccessContext dacx) {
    localTimeAxis_ = new TimeAxisDefinition(uics_.getRMan()).setToLegacy();
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Deep copy build inst
  */
  
  private ArrayList<BuildInstruction> deepCopyBuildInstr(List<BuildInstruction> inst) {
    ArrayList<BuildInstruction>retval = new ArrayList<BuildInstruction>();
    int size = inst.size();
    for (int i = 0; i < size; i++) {
      retval.add(inst.get(i).clone());
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** When the user starts up, the data sets that can be shared or local are all local to the
  ** first tab. If they create a second tab, and choose a shared data policy, we need to transfer
  ** the data to the Metabase. If that second tab is deleted, we need to suck the data back up to be
  ** local.
  */

  public List<DatabaseChange> modifyDataSharing(Metabase.DataSharingPolicy oldPolicy, Metabase.DataSharingPolicy newPolicy) {
    List<DatabaseChange> retval = new ArrayList<DatabaseChange>();
    UiUtil.fixMePrintout("need undo/redo ability");
    if (!newPolicy.init) {
      throw new IllegalArgumentException();
    }
    if (newPolicy.shareTimeUnits && !oldPolicy.shareTimeUnits) {
      retval.add(mb_.setSharedTimeAxisDefinition(this.localTimeAxis_));
      UiUtil.fixMePrintout("needs to add to retval too...");
      this.localTimeAxis_ = null; // Needs a retval too....
    } else if (!newPolicy.shareTimeUnits && oldPolicy.shareTimeUnits) {
      this.localTimeAxis_ = mb_.getSharedTimeAxisDefinition();
      retval.add(mb_.setSharedTimeAxisDefinition(null));
    }
    
    if (newPolicy.shareTimeCourses && !oldPolicy.shareTimeCourses) {
      retval.add(mb_.setSharedTimeCourseData(this.localTimeCourse_));
      this.localTimeCourse_ = null; // Needs a retval too....
    } else if (!newPolicy.shareTimeCourses && oldPolicy.shareTimeCourses) {
      this.localTimeCourse_ = mb_.getSharedTimeCourseData();
      retval.add(mb_.setSharedTimeCourseData(null));
    }
        
    if (newPolicy.sharePerts && !oldPolicy.sharePerts) {
      retval.add(mb_.setSharedPertData(this.localPertData_));
      this.localPertData_ = null; // Needs a retval too....
    } else if (!newPolicy.sharePerts && oldPolicy.sharePerts) {
      this.localPertData_ = mb_.getSharedPertData();
      retval.add(mb_.setSharedPertData(null));
    }
        
    if (newPolicy.sharePerEmbryoCounts && !oldPolicy.sharePerEmbryoCounts) {
      retval.add(mb_.setSharedCopiesPerEmbryoData(this.localCopiesPerEmb_));
      this.localCopiesPerEmb_ = null; // Needs a retval too....
    } else if (!newPolicy.sharePerEmbryoCounts && oldPolicy.sharePerEmbryoCounts) {
      this.localCopiesPerEmb_ = mb_.getSharedCopiesPerEmbryoData();
      retval.add(mb_.setSharedCopiesPerEmbryoData(null));
    }       
    this.isDataSharing_ = Boolean.valueOf(newPolicy.isSpecifyingSharing()); // Needs a retval too....
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Build a list of dynamic proxies that respect dependencies
  */
/*
  private void buildWriteList(DynamicInstanceProxy dip, ArrayList list) {
    GenomeInstance parentGI = dip.getVfgParent();
    if (parentGI instanceof DynamicGenomeInstance) {
      String parentID = DynamicInstanceProxy.extractProxyID(parentGI.getID());
      DynamicInstanceProxy parent = (DynamicInstanceProxy)dynProxies_.get(parentID);
      if ((parent != null) && (!list.contains(parent.getID()))) {
        buildWriteList(parent, list);
      }
    }
    if (!list.contains(dip.getID())) {
      list.add(dip.getID());
    }    
    return;
  } 
 */
}
