/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.awt.Color;
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
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
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
import org.systemsbiology.biotapestry.genome.DBInternalLogic;
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
import org.systemsbiology.biotapestry.parser.NewerVersionIOException;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.xml.sax.Attributes;

/****************************************************************************
**
** Database. 
*/

public class Database implements GenomeSource, LayoutSource, 
                                 WorkspaceSource, ModelChangeListener, 
                                 GeneralChangeListener, ColorResolver, 
                                 ExperimentalDataSource, InstructionSource
                                 {

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final String PREVIOUS_IO_VERSION_A_ = "2.0"; 
  private static final String PREVIOUS_IO_VERSION_B_ = "2.1"; // Actually for version 3.0 too....
  private static final String PREVIOUS_IO_VERSION_C_ = "3.1"; // Actually for version 4.0 too....
  private static final String PREVIOUS_IO_VERSION_D_ = "5.0"; // 5, 6, and 7.0
  
  private static final String CURRENT_IO_VERSION_    = "7.1";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  private ColorGenerator colGen_;
  private Genome genome_;
  private HashMap<String, GenomeInstance> instances_;
  private HashMap<String, DynamicInstanceProxy> dynProxies_;  
  private HashMap<String, Layout> layouts_;
  private NavTree navTree_;
  private ModelData modelData_;
  private PerturbationData pertData_;
  private TimeCourseData timeCourse_;
  private CopiesPerEmbryoData copiesPerEmb_;
  private TemporalInputRangeData rangeData_;  
  private UniqueLabeller labels_;
  private UniqueLabeller instructionLabels_;  
  private int uniqueNameSuffix_;
  private HashMap<String, String> simDefaultsNode_;
  private HashMap<String, String> simDefaultsGeneAnd_;
  private HashMap<String, String> simDefaultsGeneOr_;
  private ArrayList<BuildInstruction> buildInstructions_;
  private HashMap<String, InstanceInstructionSet> instanceInstructionSets_;
  private TimeAxisDefinition timeAxis_;
  private Workspace workspace_;
  private StartupView startupView_;
  
  private String iOVersion_;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Not-null constructor
  */

  public Database(BTState appState) {
    appState_ = appState;
    instances_ = new HashMap<String, GenomeInstance>();
    layouts_ = new HashMap<String, Layout>();
    navTree_ = new NavTree(appState);
    dynProxies_ = new HashMap<String, DynamicInstanceProxy>();    
    labels_ = new UniqueLabeller();
    colGen_ = new ColorGenerator();
    instructionLabels_ = new UniqueLabeller();
    buildInstructions_ = new ArrayList<BuildInstruction>();
    timeAxis_ = new TimeAxisDefinition(appState_);
    workspace_ = new Workspace();
    pertData_ = new PerturbationData(appState);
    uniqueNameSuffix_ = 1;
    iOVersion_ = CURRENT_IO_VERSION_;
    startupView_ = new StartupView();
    instanceInstructionSets_ = new HashMap<String, InstanceInstructionSet>();
    simDefaultsNode_ = new HashMap<String, String>();
    simDefaultsNode_.put("initVal", "200.0");
    simDefaultsNode_.put("KD", "0.005");
    simDefaultsNode_.put("Kmult", "1.0");  // This parameter is for non-gene nodes
                                           // with one or more inputs; the Kmult value
                                           // sets the scale for the output of the node,
                                           // relative to the input values.  
    simDefaultsGeneAnd_ = new HashMap<String, String>();
    simDefaultsGeneAnd_.put("DN", "160000000.0");
    simDefaultsGeneAnd_.put("IM", "5.45");
    simDefaultsGeneAnd_.put("initVal", "0.0");
    simDefaultsGeneAnd_.put("KR", "100000.0");
    simDefaultsGeneAnd_.put("KQ", "10.0");
    simDefaultsGeneAnd_.put("KD", "0.005");
    simDefaultsGeneOr_ = new HashMap<String, String>();
    simDefaultsGeneOr_.put("DN", "160000000.0");
    simDefaultsGeneOr_.put("IM", "5.45");
    simDefaultsGeneOr_.put("initVal", "0.0");
    simDefaultsGeneOr_.put("KR", "100000.0");
    simDefaultsGeneOr_.put("KQ", "1.0");
    simDefaultsGeneOr_.put("KD", "0.005");
    appState.getEventMgr().addModelChangeListener(this);
    appState.getEventMgr().addGeneralChangeListener(this);    
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Start a new model
  */

  public void newModelViaDACX() {
    dropViaDACX();
    genome_ = new DBGenome(appState_, appState_.getRMan().getString("tree.FullGenome"), "bioTapA");
    labels_.addExistingLabel("bioTapA");
    String layoutID = labels_.getNextLabel();    
    layouts_.put(layoutID, new Layout(appState_, layoutID, "bioTapA"));
    navTree_.addNode(null, null, "bioTapA");
    modelData_ = null;
    pertData_ = new PerturbationData(appState_);
    timeCourse_ = new TimeCourseData(appState_);
    copiesPerEmb_ = new CopiesPerEmbryoData(appState_);
    rangeData_ = new TemporalInputRangeData(appState_);
    timeAxis_ = new TimeAxisDefinition(appState_);
    //
    // We do NOT clear out display options, but we do need to drop
    // defs that depend on the time def, so the time axis definition 
    // is not frozen for a new model:
    //

    appState_.getDisplayOptMgr().getDisplayOptions().dropDataBasedOptions();
  
    workspace_ = new Workspace();
    uniqueNameSuffix_ = 1;
    iOVersion_ = CURRENT_IO_VERSION_;
    startupView_ = new StartupView();     
    return;
  }  

  /***************************************************************************
  ** 
  ** Drop everything
  */

  public void dropViaDACX() {
    genome_ = null;
    layouts_.clear();
    instances_.clear();
    dynProxies_.clear();
    // Have to clear out above before clearing the tree so that
    // the tree clears correctly:
    navTree_.clearOut();
    modelData_ = null;
    pertData_ = new PerturbationData(appState_);
    timeCourse_ = null;
    copiesPerEmb_ = null;
    rangeData_ = null;
    timeAxis_ = new TimeAxisDefinition(appState_);
    //
    // We do NOT clear out display options, but we do need to drop
    // defs that depend on the time def, so the time axis definition 
    // is not frozen for a new model:
    //

    appState_.getDisplayOptMgr().getDisplayOptions().dropDataBasedOptions();
    
    workspace_ = new Workspace();
    uniqueNameSuffix_ = 1;
    iOVersion_ = CURRENT_IO_VERSION_;
    startupView_ = new StartupView();     
    labels_ = new UniqueLabeller();
    colGen_.dropColors();
    appState_.getFontMgr().resetToDefaults();
    appState_.getImageMgr().dropAllImages();
    appState_.getPathMgr().dropAllPaths();
    // Note we are not resetting the Display options!
    buildInstructions_.clear();
    instructionLabels_ = new UniqueLabeller();
    instanceInstructionSets_.clear();
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
        retval.newLayouts.put(name, new Layout(appState_, name, "bioTapA"));
      }
    }
    Iterator<String> nlit = retval.newLayouts.keySet().iterator();
    while (nlit.hasNext()) {
      String name = nlit.next();
      Layout lo = retval.newLayouts.get(name);
      layouts_.put(name, new Layout(lo));
    }
    genome_ = ((DBGenome)genome_).getStrippedGenomeCopy();
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
        retval.newLayouts.put(name, new Layout(appState_, name, gid));        
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
  ** Get the simulation defaults
  */

  public Iterator<String> getSimulationDefaults(int nodeType, int logicType) {
    if (nodeType != Node.GENE) {
      return (simDefaultsNode_.keySet().iterator());
    } else if (logicType == DBInternalLogic.AND_FUNCTION) {
      return (simDefaultsGeneAnd_.keySet().iterator());
    } else {
      return (simDefaultsGeneOr_.keySet().iterator());
    }
  }
  
  /***************************************************************************
  ** 
  ** Get a simulation default
  */

  public String getSimulationDefaultValue(String key, int nodeType, int logicType) {
    if (nodeType != Node.GENE) {
      return (simDefaultsNode_.get(key));
    } else if (logicType == DBInternalLogic.AND_FUNCTION) {
      return (simDefaultsGeneAnd_.get(key));
    } else {
      return (simDefaultsGeneOr_.get(key));
    }
  }
  
  /***************************************************************************
  ** 
  ** This gets called following file input to do any needed legacy fixups.
  */

  public void legacyIOFixup(JFrame topWindow) {

    appState_.getFontMgr().fixupLegacyIO();  // Need this starting version 3.1
    ResourceManager rMan = appState_.getRMan();
    String message = rMan.getString("legacyIOFixup.baseChange");
    String msgDiv;
    if (topWindow != null) {
      message = "<html><center>" + message;
      msgDiv = "<br><br>";
    } else {
      msgDiv = " ";
    }
    boolean doit = false;
    
    if (iOVersion_.equals("1.0")) {
      List<String> change = legacyIOFixupForHourBounds();
      if ((change != null) && !change.isEmpty()) {
        int size = change.size();
        String desc;
        if (size == 1) {
          String form = rMan.getString("legacyIOFixup.singleHourBoundsChange");                             
          desc = MessageFormat.format(form, new Object[] {change.get(0)});
        } else {
          String form = rMan.getString("legacyIOFixup.multiHourBoundsChange");
          desc = MessageFormat.format(form, new Object[] {change.get(0), new Integer(size - 1)});                    
        }
        message = message + msgDiv + desc;        
        doit = true;
      }
      
      List<String> nodeCh = legacyIOFixupForNodeActivities();
      if ((nodeCh != null) && !nodeCh.isEmpty()) {
        int size = nodeCh.size();
        String desc;
        if (size == 1) {
          String form = rMan.getString("legacyIOFixup.singleNodeActivityChange");                             
          desc = MessageFormat.format(form, new Object[] {nodeCh.get(0)});
        } else {
          String form = rMan.getString("legacyIOFixup.multiNodeActivityChange");
          desc = MessageFormat.format(form, new Object[] {nodeCh.get(0), new Integer(size - 1)});                    
        }
        message = message + msgDiv + desc;        
        doit = true;
      } 
    }
    
    if (iOVersion_.equals("1.0") || iOVersion_.equals(PREVIOUS_IO_VERSION_A_)) {
      legacyIOFixupForGroupOrdering();
    }
    
    //
    // Slash Node pads have been overhauled in Version 7.0.1. 
    //
    
    if (iOVersion_.equals("1.0") || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_C_) || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_D_)) { 
      PadCalculatorToo pcalc = new PadCalculatorToo();
      pcalc.legacyIOFixupForSlashNodes(this, this);
      
      //
      // Gene length must now match link usage:
      //
      FullGenomeHierarchyOracle fgho = new FullGenomeHierarchyOracle(appState_);
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
        for (String geneID : toFix) {
          List<String> lerrs = LinkSupport.fixCisModLinks(null, gla, appState_, new DataAccessContext(appState_), geneID, true);
          for (String msg : lerrs) {
            message = message + msgDiv + msg;
          }
        }
        doit = true;
      }
    }

    //
    // Turns out V3.0 had potential errors with pad assignments in root when
    // drawing bottom-up.  For kicks, let's _always_ look out for these problems:
    //
    
    PadCalculatorToo pcalc = new PadCalculatorToo();   
    List<PadCalculatorToo.IOFixup> padErrors = pcalc.checkForPadErrors(this, this);
    if (!padErrors.isEmpty()) {
      pcalc.fixIOPadErrors(this, padErrors);
      message = message + msgDiv + rMan.getString("legacyIOFixup.padErrors");
      doit = true;
    }
    
    List<String> srcErrors = pcalc.checkForGeneSrcPadErrors(this, this);
    if (!srcErrors.isEmpty()) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.srcPadErrors");
      doit = true;
    }
    
    //
    // Note properties got orphaned in old versions:
    //
    
    if (iOVersion_.equals("1.0") || iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || iOVersion_.equals(PREVIOUS_IO_VERSION_B_)) {  
      List<String> noteOrphs = legacyIOFixupForOrphanedNotes();
      int numOrphs = noteOrphs.size();
      if (numOrphs > 0) {
        String form = rMan.getString("legacyIOFixup.orphanNoteFixup");                             
        message = message + msgDiv + form;        
        doit = true;
      }
    }    

    //
    // Prior to 3.1, need to fixup line breaks on nodes
    //

    if (iOVersion_.equals("1.0") || iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || iOVersion_.equals(PREVIOUS_IO_VERSION_B_)) {
      List<String> lineBrks = legacyIOFixupForLineBreaks();
      if ((lineBrks != null) && !lineBrks.isEmpty()) {
        int size = lineBrks.size();
        String desc;
        if (size == 1) {
          String form = rMan.getString("legacyIOFixup.singleLineBreakChange");                             
          desc = MessageFormat.format(form, new Object[] {lineBrks.get(0)});
        } else {
          String form = rMan.getString("legacyIOFixup.multiLineBreakChange");
          desc = MessageFormat.format(form, new Object[] {lineBrks.get(0), new Integer(size - 1)});                    
        }
        message = message + msgDiv + desc;        
        doit = true;
      } 
    }
    
    
    //
    // QPCR is no longer used for storage:
    //
    
    if (iOVersion_.equals("1.0") || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
        iOVersion_.equals(PREVIOUS_IO_VERSION_C_)) { 
      pertData_.transferFromLegacy();
    }
    
    

    
    //
    // Finish assembling the message:
    //
    
    if (doit) {
      message = message + msgDiv + rMan.getString("legacyIOFixup.changeSuffix");
      if (topWindow != null) {
        message = message + "</center></html>";        
        JOptionPane.showMessageDialog(topWindow, message,
                                      rMan.getString("legacyIOFixup.dialogTitle"),
                                      JOptionPane.WARNING_MESSAGE); 
      } else {
        System.err.println(message);
      }
    }

    return;
  }
 

  /***************************************************************************
  ** 
  ** We need to install hour bounds on parent models of child dynamic models
  */

  private List<String> legacyIOFixupForHourBounds() {
    if (dynProxies_.isEmpty()) {
      return (null);
    }
    
    int minTcd = Integer.MAX_VALUE;
    int maxTcd = Integer.MIN_VALUE;    
    TimeCourseData tcd = getTimeCourseData();
    if ((tcd != null) && tcd.haveData()) {
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

  private void legacyIOFixupForGroupOrdering() {

    String rootKey = getGenome().getID();

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
  ** We need to fix genes that are too short
  */

  private void legacyIOFixupForGeneLength(Map<String, Integer> changes) {

    DBGenome genome = (DBGenome)getGenome();
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

  private List<String> legacyIOFixupForLineBreaks() {

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
        String breakDef = MultiLineRenderSupport.legacyLineBreaks(np.getHideName(), node.getNodeType(), frc, node.getName(), appState_.getFontMgr());
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

  private List<String> legacyIOFixupForOrphanedNotes() {

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

  private List<String> legacyIOFixupForNodeActivities() {
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
  ** Answers if we have data attached to our node by default (simple name match)
  ** 
  */
  
  public boolean hasDataAttachedByDefault(String nodeID) {
    Database db = appState_.getDB();
    nodeID = GenomeItemInstance.getBaseID(nodeID);

    PerturbationData pdat = db.getPertData();
    if (pdat.haveDataForNode(nodeID, null) && !pdat.haveCustomMapForNode(nodeID)) {
      return (true);
    }
    
    TimeCourseData tcd = db.getTimeCourseData();
    if (tcd.haveDataForNode(nodeID) && !tcd.haveCustomMapForNode(nodeID)) {
      return (true);
    }
    TemporalInputRangeData tird = db.getTemporalInputRangeData();
    if (tird.haveDataForNode(nodeID) && !tird.haveCustomMapForNode(nodeID)) {
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
    Database db = appState_.getDB();
    TimeCourseData tcd = db.getTimeCourseData();
    String groupName = group.getName();
    String groupID = group.getID();
    if (DataUtil.containsKey(tcd.getRegions(), groupName) &&     
        !tcd.haveCustomMapForRegion(groupID)) {
      return (true);
      
    }
    TemporalInputRangeData tird = db.getTemporalInputRangeData();
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
  ** Rollback a data undo transaction
  */
    
  public void rollbackDataUndoTransaction(DatabaseChange change) {
    databaseChangeUndo(change);
    return;
  }    

  /***************************************************************************
  ** 
  ** Get the perturbation data
  */

  public PerturbationData getPertData() {
    return (pertData_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the perturbation data
  */

  public void setPertData(PerturbationData pd) {
    pertData_ = pd;
    return;
  }
 
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startTimeCourseUndoTransaction() {
    DatabaseChange dc = new DatabaseChange();
    dc.oldTcd = timeCourse_;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishTimeCourseUndoTransaction(DatabaseChange change) {
    change.newTcd = timeCourse_;
    return (change);
  }
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startCopiesPerEmbryoUndoTransaction() {
    DatabaseChange dc = new DatabaseChange();
    dc.oldCpe = copiesPerEmb_;
    return (dc);
  }  
  
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishCopiesPerEmbryoUndoTransaction(DatabaseChange change) {
    change.newCpe = copiesPerEmb_;
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
  ** Get the Time course data
  */

  public TimeCourseData getTimeCourseData() {
    return (timeCourse_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the Time Course data
  */

  public void setTimeCourseData(TimeCourseData timeCourse) {
    timeCourse_ = timeCourse;
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Get the Time course data
  */

  public CopiesPerEmbryoData getCopiesPerEmbryoData() {
    return (copiesPerEmb_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the copies per embryo data
  */

  public void setCopiesPerEmbryoData(CopiesPerEmbryoData copies) {
    copiesPerEmb_ = copies;
    return;
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
  ** Get the time axis definition
  */

  public TimeAxisDefinition getTimeAxisDefinition() {
    return (timeAxis_);
  }  
  
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public DatabaseChange setTimeAxisDefinition(TimeAxisDefinition timeAxis) {
    DatabaseChange dc = new DatabaseChange();
    dc.oldTimeAxis = timeAxis_.clone();
    timeAxis_ = timeAxis;
    dc.newTimeAxis = timeAxis_.clone();
    return (dc);
  }
  
  /***************************************************************************
  ** 
  ** Set the time axis definition
  */

  public void installLegacyTimeAxisDefinition() {
    timeAxis_ = new TimeAxisDefinition(appState_).setToLegacy();
    return;
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
      String ovrID = owner.getFirstViewPreference(modChoice, revChoice);
      if (ovrID != null) {
        StartupView modified = new StartupView(modelID, ovrID, modChoice, revChoice);
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

  public Genome getGenome() {
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
    return (getLayoutForGenomeKey(getGenome().getID()));
  } 
  
  /***************************************************************************
  ** 
  ** Get the layout key used for the given genome key
  */
  
  public String mapGenomeKeyToLayoutKey(String genomeKey) {
    return (appState_.getLayoutMgr().getLayout(genomeKey));
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

  public void setGenome(Genome genome) {
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
  ** Get the color
  */

  public Color getColor(String colorKey) {
    return (colGen_.getColor(colorKey));
  }

  /***************************************************************************
  ** 
  ** Get the named color
  */

  public NamedColor getNamedColor(String colorKey) {
    return (colGen_.getNamedColor(colorKey));
  }  

  /***************************************************************************
  **
  ** Update the color set
  */
  
  public GlobalChange updateColors(Map<String, NamedColor> namedColors) {
    return (colGen_.updateColors(namedColors));
  }
  
  /***************************************************************************
  ** 
  ** Get a unique model name
  */

  public String getUniqueModelName() {
 
    String format = appState_.getRMan().getString("model.defaultNameFormat");   
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
    out.print("<BioTapestry version=\"");
    out.print(CURRENT_IO_VERSION_);
    out.println("\" >");
    if (modelData_ != null) {
      modelData_.writeXML(out, ind.up());
      ind.down();
    }
    if ((timeAxis_ != null) && timeAxis_.isInitialized()) {
      timeAxis_.writeXML(out, ind.up());
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

    appState_.getFontMgr().writeXML(out, ind.up());
    ind.down();
    appState_.getDisplayOptMgr().writeXML(out, ind.up());
    ind.down();
    
    colGen_.writeXML(out, ind.up());
    ind.down();
    
    appState_.getImageMgr().writeXML(out, ind.up());
    ind.down();
    
    appState_.getPathMgr().writeXML(out, ind.up());
    ind.down();
    
    // core genome
    ((DBGenome)genome_).writeXML(out, ind.up());
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
    if ((pertData_ != null) && pertData_.haveData()) {
      pertData_.writeXML(out, ind);
    }
    
    if (timeCourse_ != null) {
      timeCourse_.writeXML(out, ind);
    }
    if (rangeData_ != null) {
      rangeData_.writeXML(out, ind);
    }
    if ((copiesPerEmb_ != null) && copiesPerEmb_.haveData()) {
      copiesPerEmb_.writeXML(out, ind);
    }    
    
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

    ind.down().indent();
    out.println("</BioTapestry>");
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
  ** Get the next color label
  */
  
  public String getNextColorLabel() {
    return (colGen_.getNextColorLabel()); 
  }  
  
  /***************************************************************************
  **
  ** Set the color for the given name
  */
  
  public void setColor(String itemId, NamedColor color) {
    colGen_.setColor(itemId, color);
    return;
  }
  
  /***************************************************************************
  **
  ** Return an iterator over all the color keys
  */
  
  public Iterator<String> getColorKeys() {
    return (colGen_.getColorKeys());
  }

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GlobalChange undo) {
    colGen_.changeUndo(undo);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GlobalChange undo) {
    colGen_.changeRedo(undo);
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
    } else if (undo.oldTcd != null) {
      timeCourse_ = undo.oldTcd;
    } else if (undo.oldCpe != null) {
      copiesPerEmb_ = undo.oldCpe;  
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
    } else if (undo.oldTimeAxis != null) {
      timeAxis_ = undo.oldTimeAxis;
    } else if (undo.oldWorkspace != null) {
      workspace_ = undo.oldWorkspace;
    } else if (undo.oldStartupView != null) {
      startupView_ = undo.oldStartupView;
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
    } else if (undo.newTcd != null) {
      timeCourse_ = undo.newTcd;
    } else if (undo.newCpe != null) {
      copiesPerEmb_ = undo.newCpe;        
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
    } else if (undo.newTimeAxis != null) {
      timeAxis_ = undo.newTimeAxis;
    } else if (undo.newWorkspace != null) {
      workspace_ = undo.newWorkspace;
    } else if (undo.newStartupView != null) {
      startupView_ = undo.newStartupView;
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
  ** Return the colors you cannot delete
  */
  
  public Set<String> cannotDeleteColors() {
    return (colGen_.cannotDeleteColors());
  }  

  /***************************************************************************
  **
  ** Return from a cycle of active colors
  */
  
  public String activeColorCycle(int i) {
    return (colGen_.activeColorCycle(i));
  }
  
  /***************************************************************************
  **
  ** A distinct active color
  */
  
  public String distinctActiveColor() { 
    return (colGen_.distinctActiveColor());
  }
  
  /***************************************************************************
  **
  ** Return a distinct inactive color
  */
  
  public String distinctInactiveColor() {  
    return (colGen_.distinctInactiveColor());
  }
   
  /***************************************************************************
  **
  ** Return from a cycle of inactive colors
  */
  
  public String inactiveColorCycle(int i) {
    return (colGen_.inactiveColorCycle(i));
  }
  
  /***************************************************************************
  **
  ** Return from a cycle of gene colors
  */
  
  public String getNextColor() {
    
    List<String> geneColors = colGen_.getGeneColorsAsList();
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
    
 
    Genome root = getGenome();
    String loKey = appState_.getLayoutMgr().getLayout(root.getID());
    Layout lo = getLayout(loKey);    
    Iterator<Gene> git = root.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      NodeProperties np = lo.getNodeProperties(gene.getID());
      String col = np.getColorName();
      if (geneColors.contains(col)) {
        Integer count = colorCounts.get(col);
        colorCounts.put(col, new Integer(count.intValue() + 1));
      }
    }
    Iterator<Node> nit = root.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      if (NodeProperties.setWithLinkColor(node.getNodeType())) {
        NodeProperties srcProp = lo.getNodeProperties(node.getID());
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
    for (int i = 0; i < gcSize; i++) {
      String retval = geneColors.get(i);
      if (minSet.contains(retval)) {
        return (retval);
      }
    }
    
    return ("black");
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
  
  /***************************************************************************
  **
  ** Get Ith available color
  */
  
  public String getGeneColor(int i) {
    return (colGen_.getGeneColor(i));
  }   
  
  /***************************************************************************
  **
  ** Get number of colors
  */
  
  public int getNumColors() {
    return (colGen_.getNumColors());
  }
  
  /***************************************************************************
  **
  ** Set the IO version
  **
  */
  
  public void setIOVersion(String version) throws IOException {
    iOVersion_ = version;
    // Issue 240 FIX: Must not be too new:
    boolean notTooNew = (iOVersion_.equals("1.0") || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_A_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_B_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_C_) || 
                         iOVersion_.equals(PREVIOUS_IO_VERSION_D_) ||
                         iOVersion_.equals(CURRENT_IO_VERSION_));
    if (!notTooNew) {
      throw new NewerVersionIOException(iOVersion_);
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
    retval.add("BioTapestry");
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Parse out IO version
  **
  */  
  
  public static String versionFromXML(String elemName, Attributes attrs) throws IOException {
    String version = AttributeExtractor.extractAttribute(elemName, attrs, "BioTapestry", "version", false);   
    return ((version == null) ? "1.0" : version);
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
