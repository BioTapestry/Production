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

package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupChange;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.NetOverlayController;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.Layout.PadNeedsForLayout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of a node
*/

public class RemoveNode extends AbstractControlFlow {

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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor
  */ 
  
  public RemoveNode() {
    name = "nodePopup.NodeDelete";
    desc = "nodePopup.NodeDelete";     
    mnem = "nodePopup.NodeDeleteMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!rcx.currentGenomeIsADynamicInstance()) {
      return (true);
    } else {
      String proxID = ((DynamicGenomeInstance)rcx.getCurrentGenome()).getProxyID();
      DynamicInstanceProxy prox = rcx.getGenomeSource().getDynamicProxy(proxID);
      return (prox.hasAddedNode(inter.getObjectID()));
    }
  }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {   
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepToWarn")) {
          next = ans.stepToWarn();      
        } else if (ans.getNextStep().equals("stepToCheckDataDelete")) {
          next = ans.stepToCheckDataDelete();   
        } else if (ans.getNextStep().equals("registerCheckDataDelete")) {
          next = ans.registerCheckDataDelete(last);       
        } else if (ans.getNextStep().equals("stepToRemove")) {
          next = ans.stepToRemove();       
        } else {
          throw new IllegalStateException();
        }
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState {
    
    private Intersection intersect_;
    private Map<String, Boolean> dataDelete_;
   
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepToWarn";
    }
 
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToWarn() {
      DialogAndInProcessCmd daipc;
      SimpleUserFeedback suf = RemoveSupport.deleteWarningHelperNew(dacx_);
      if (suf != null) {
        daipc = new DialogAndInProcessCmd(suf, this);     
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      } 
      nextStep_ = "stepToCheckDataDelete";   
      return (daipc); 
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToCheckDataDelete() {
           
      if (!dacx_.currentGenomeIsRootDBGenome()) {
        dataDelete_ = null;
        nextStep_ = "stepToRemove";
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));
      }
  
      String deadID = intersect_.getObjectID();
      String deadName = dacx_.getCurrentGenome().getNode(deadID).getRootName();
  
      //
      // Under all circumstances, we delete associated mappings to data.
      // We also give the user the option to delete the underlying data
      // tables, unless previously specified
      //
    
      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
      TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
      TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    
      if (((tcdm != null) && tcdm.haveDataForNodeOrName(tcd, deadID, deadName)) ||
          ((tird != null) && tird.haveDataForNodeOrName(deadID, deadName))) {
        String doData = dacx_.getRMan().getString("nodeDelete.doDataMessage");
        String msg = MessageFormat.format(doData, new Object[] {deadName});    
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, msg, dacx_.getRMan().getString("nodeDelete.messageTitle"));
        DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(suf, this);     
        nextStep_ = "registerCheckDataDelete";   
        return (daipc);
      } 
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToRemove";   
      return (daipc);
    } 
  
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd registerCheckDataDelete(DialogAndInProcessCmd daipc) {
      int deleteUnderlyingTables = daipc.suf.getIntegerResult();
      if (deleteUnderlyingTables == SimpleUserFeedback.CANCEL) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
      } else if (deleteUnderlyingTables == SimpleUserFeedback.YES) { 
        dataDelete_ = new HashMap<String, Boolean>();
        dataDelete_.put(intersect_.getObjectID(), Boolean.valueOf(true));
      } else {
        dataDelete_ = null;
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "stepToRemove";   
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
           
      Map<String, PadNeedsForLayout> globalPadNeeds = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();
      UndoSupport support = uFac_.provideUndoSupport("undo.deleteNode", dacx_); 
      if (deleteNodeFromModelCore(uics_, intersect_.getObjectID(), dacx_, support, dataDelete_, false, uFac_)) {
        uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);   
        if (globalPadNeeds != null) {
          ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds, false, support);
        }               
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
        uics_.getSUPanel().drawModel(false);
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }   
  }
  
  /***************************************************************************
  **
  ** Delete a node from the model
  */  
 
  public static boolean deleteNodeFromModelCore(UIComponentSource uics, String nodeID, StaticDataAccessContext rcx,
                                                UndoSupport support, Map<String, Boolean> dataDelete, boolean keepEmptyMemOnly, UndoFactory uFac) {
 
    //
    // Figure out what node we are dealing with.
    //
      
    Node node = rcx.getCurrentGenome().getNode(nodeID);
    if (node == null) {
      return (false);
    }     

    //
    // Undo/Redo support
    //
    
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.deleteNodeFromModel", rcx);
    }

    //
    // Handle case of removing extra nodes from dynamic instances
    //
    
    if (rcx.currentGenomeIsADynamicInstance()) {
      boolean extraLost = false;
      String proxID = rcx.getCurrentGenomeAsDynamicInstance().getProxyID();
      DynamicInstanceProxy prox = rcx.getGenomeSource().getDynamicProxy(proxID);
      ProxyChange pc = prox.deleteExtraNode(nodeID);
      if (pc != null) {
        ProxyChangeCmd pcc = new ProxyChangeCmd(rcx, pc);
        support.addEdit(pcc);
        support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        extraLost = true;
      }
      //
      // Remove it from child dynamic instances also, if present
      //
      boolean needGeneral = false;
      Iterator<DynamicInstanceProxy> pxit = rcx.getGenomeSource().getDynamicProxyIterator();
      while (pxit.hasNext()) {
        DynamicInstanceProxy dip = pxit.next();
        if (dip.proxyIsAncestor(proxID) && dip.hasAddedNode(nodeID)) {
          pc = dip.deleteExtraNode(nodeID);
          if (pc != null) {
            ProxyChangeCmd pcc = new ProxyChangeCmd(rcx, pc);
            support.addEdit(pcc);
            needGeneral = true;
            extraLost = true;
          }
        }
      }
      
      if (extraLost && !keepEmptyMemOnly) {
        proxyPostExtraNodeDeletionSupport(uics, rcx, support);
      }
      
      // Don't want to enumerate all those dynamic instances.  This is a stand in
      // that forces update of all dynamic models.
      if (needGeneral) {
        support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));
      }
      if (localUndo) {support.finish();}      
      return (true);
    }
    
    //
    // Kill off links to and from the node
    //
     
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    HashSet<String> deadLinkSet = new HashSet<String>();     
    while (lit.hasNext()) {
      Linkage lnk = lit.next();       
      if (lnk.getSource().equals(nodeID) ||
          lnk.getTarget().equals(nodeID)) {
        deadLinkSet.add(lnk.getID());
      }
    }
    Iterator<String> dnit = deadLinkSet.iterator();
    while (dnit.hasNext()) {
      String linkID = dnit.next();       
      RemoveLinkage.doLinkDelete(linkID, rcx, support);
    }

    doNodeDelete(uics, nodeID, rcx, support, dataDelete, keepEmptyMemOnly);    

    if (localUndo) {support.finish();}    
    return (true);
  }  

  /***************************************************************************
  **
  ** Delete an UNCONNECTED node from the given model and all submodels 
  */   
     
  public static void doNodeDelete(UIComponentSource uics, String deadID, StaticDataAccessContext rcx, UndoSupport support, Map<String, Boolean> dataDelete, boolean keepEmptyMemOnly) {
    
    //
    // Figure out if we are dealing with the root, a topInstance, or a subset.
    //
    
    boolean isRoot;
    GenomeInstance inst;
    if (rcx.currentGenomeIsAnInstance()) {
      inst = rcx.getCurrentGenomeAsInstance();
      isRoot = false;
    } else {
      inst = null;
      isRoot = true;
    }     
    
    //
    // Kill off all instances.  Kill off the display properties at the same time.
    // We find all the instances of the same root node in each genome instance,
    // and kill it off from the instance, and all layouts for the instance.
    // We also need to remove the nodes from the groups.
    //
    
    boolean needGeneral = false;
    boolean goodbyeExtras = false;
    Iterator<GenomeInstance> iit = rcx.getGenomeSource().getInstanceIterator();
    HashSet<String> deadSet = new HashSet<String>();
    while (iit.hasNext()) {
      deadSet.clear();
      GenomeInstance gi = iit.next();
      if (isRoot || inst.isAncestor(gi)) {  // We are interested in this instance      
        //
        // Build the dead set:
        //
        Iterator<Node> nit = gi.getNodeIterator();
        while (nit.hasNext()) {
          NodeInstance ni = (NodeInstance)nit.next();
          if (isRoot && (deadID.equals(ni.getBacking().getID())) || (deadID.equals(ni.getID()))) {
            deadSet.add(ni.getID());
          }
        }
        Iterator<Gene> git = gi.getGeneIterator();
        while (git.hasNext()) {
          NodeInstance ni = (NodeInstance)git.next();
          if (isRoot && (deadID.equals(ni.getBacking().getID())) || (deadID.equals(ni.getID()))) {    
            deadSet.add(ni.getID());
          }
        }
      }
      StaticDataAccessContext rcxGI = new StaticDataAccessContext(rcx, gi);
      //
      // Clean out the dead set:
      //
      Iterator<String> dsit = deadSet.iterator();
      while (dsit.hasNext()) {
        String key = dsit.next();
        GenomeChange gc = gi.removeNode(key);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
          support.addEdit(gcc);        
        }
        Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
        while (layit.hasNext()) {
          Layout layout = layit.next();
          if (layout.haveNodeMetadataDependency(gi.getID(), key)) {
            Layout.PropChange lpc = layout.dropNodeMetadataDependency(gi.getID(), key);
            if (lpc != null) {
              PropChangeCmd pcc = new PropChangeCmd(rcxGI, lpc);
              support.addEdit(pcc);        
            }
          } 
          if (layout.getTarget().equals(gi.getID())) {
            NodeProperties np = layout.getNodeProperties(key);
            if (np == null) {
              continue;
            }
            Layout.PropChange[] lpc = new Layout.PropChange[1];    
            lpc[0] = layout.removeNodeProperties(key);
            if (lpc != null) {
              PropChangeCmd pcc = new PropChangeCmd(rcxGI, lpc);
              support.addEdit(pcc);        
            }
          }
        }
        Iterator<Group> git = gi.getGroupIterator();
        // Remove from groups. Inherited groups don't manage membership
        while (git.hasNext()) {
          Group gr = git.next();
          if (!gr.isInherited() && gr.isInGroup(key, gi)) {
            GroupChange grc = gr.removeMember(key, gi.getID());
            if (grc != null) {
              GroupChangeCmd gcc = new GroupChangeCmd(rcxGI, grc);
              support.addEdit(gcc);        
            }
          }
        }
        
        // Remove from overlay modules.
        
        supportNodeDeletionFromOverlays(uics, gi, rcxGI, support, key, keepEmptyMemOnly);

        //
        // Remove from all dynamic proxies underneath the instance if it is
        // included there as an extra node.
        //   
        Iterator<DynamicInstanceProxy> pxit = rcx.getGenomeSource().getDynamicProxyIterator();        
        while (pxit.hasNext()) {
          DynamicInstanceProxy dip = pxit.next();
          if (dip.instanceIsAncestor(gi)) {
            StaticDataAccessContext rcxDP = new StaticDataAccessContext(rcx, dip.getFirstProxiedKey());
            if (dip.hasAddedNode(key)) {
              ProxyChange pc = dip.deleteExtraNode(key);
              if (pc != null) {
                ProxyChangeCmd pcc = new ProxyChangeCmd(rcxDP, pc);
                support.addEdit(pcc);
                // Don't want to enumerate all those dynamic instances for eventing.
                // This is a stand in that forces update of all dynamic models:
                needGeneral = true;
                goodbyeExtras = true;
              }
            } else {
              
              // Proxies handle this step already for extras.  Else we need to kill it off here:
              if (supportNodeDeletionFromOverlays(uics, dip, rcxDP, support, key, keepEmptyMemOnly)) {
                needGeneral = true;
              }
            }
          }        
        }       
      }
    }
    
    // DIP extra deletion does not take care of emptied mem-only modules.  So do that now:
    if (!keepEmptyMemOnly && goodbyeExtras && proxyPostExtraNodeDeletionSupport(uics, rcx, support)) {
      needGeneral = true;
    }
    if (isRoot) {
      rootDeletionSteps(uics, deadID, rcx, support, dataDelete, keepEmptyMemOnly, needGeneral);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle root case
  */  
 
  private static void rootDeletionSteps(UIComponentSource uics, String deadID, StaticDataAccessContext rcx, UndoSupport support, 
                                        Map<String, Boolean> dataDelete, boolean keepEmptyMemOnly, boolean needGeneral) {
     
    //
    // Model builder plugin wants to know about this:
    //
 
    PlugInManager pluginManager = uics.getPlugInMgr(); 
    Iterator<ModelBuilderPlugIn> mbIterator = pluginManager.getBuilderIterator();
    if (mbIterator.hasNext()) {
      mbIterator.next().handleNodeDeletion(rcx, deadID);
    }
  
    String deadName = rcx.getCurrentGenome().getNode(deadID).getRootName();
    GenomeChange gc = rcx.getCurrentGenome().removeNode(deadID);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
      support.addEdit(gcc);        
    }
    
    // Remove from overlay modules.
    supportNodeDeletionFromOverlays(uics, rcx.getCurrentGenome(), rcx, support, deadID, keepEmptyMemOnly);
    
    Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
    while (layit.hasNext()) {
      Layout layout = layit.next();
      if (layout.getTarget().equals(rcx.getCurrentGenomeID())) {
        NodeProperties np = layout.getNodeProperties(deadID);
        if (np == null) {
          continue;
        }
        Layout.PropChange[] lpc = new Layout.PropChange[1];    
        lpc[0] = layout.removeNodeProperties(deadID);
        if (lpc != null) {
          PropChangeCmd mov = new PropChangeCmd(rcx, lpc);
          support.addEdit(mov);        
        }
      }
    }
   
    boolean haveDeleted = false;
    Boolean ddo = (dataDelete != null) ? dataDelete.get(deadID) : null;
    if ((ddo != null) ? ddo.booleanValue() : false) {
      haveDeleted = deleteUnderlyingTables(rcx, support, deadID, deadName);
    } 
    haveDeleted |= deleteDataMaps(rcx, support, deadID);
    
    if (haveDeleted || needGeneral) {
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    }
    return;
  }  
    
 /***************************************************************************
  **
  ** Delete underlying data
  */  
 
  public static boolean deleteUnderlyingTables(StaticDataAccessContext rcx, UndoSupport support, String nodeID, String nodeName) {
    
    boolean haveDelete = false;
    
    //
    // With the advent of flat perturbation data, it no longer really makes
    // sense to "drop" the gene:
    
    TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
    TimeCourseDataMaps tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
    if (tcd != null) {
      List<TimeCourseDataMaps.TCMapping> keys = tcdm.getTimeCourseTCMDataKeysWithDefaultGivenName(nodeID, nodeName);
      if (keys != null) {
        Iterator<TimeCourseDataMaps.TCMapping> kit = keys.iterator();
        while (kit.hasNext()) {
          TimeCourseDataMaps.TCMapping tcm = kit.next();
          TimeCourseChange tchg = tcd.dropGene(tcm.name);
          if (tchg != null) {
            TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(tchg, false);
            support.addEdit(cmd);
            haveDelete = true;
          }
        }
      }
    }
    
    TemporalInputRangeData tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
    if (tird != null) {
      List<String> keys = tird.getTemporalInputRangeEntryKeysWithDefaultGivenName(nodeID, nodeName);
      if (keys != null) {
        Iterator<String> kit = keys.iterator();
        while (kit.hasNext()) {
          String key = kit.next();
          TemporalInputChange tichg = tird.dropEntry(key);
          if (tichg != null) {
            TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(rcx, tichg, false);
            support.addEdit(cmd);
            haveDelete = true;
          }
        }
      }
    }
    return (haveDelete);
  }

  /***************************************************************************
  **
  ** Delete dependent data maps
  */  
 
  public static boolean deleteDataMaps(StaticDataAccessContext rcx, UndoSupport support, String nodeID) {
   
    //
    // FIX ME!!  What about getting rid of entries in OTHER maps that reference the dropped
    // table (see dropDanglingMaps());
    //

    boolean haveDelete = false;
    PerturbationDataMaps pdms = rcx.getDataMapSrc().getPerturbationDataMaps();
   
    PertDataChange chg = pdms.dropDataEntryKeys(nodeID);
    if (chg != null) {
      PertDataChangeCmd cmd = new PertDataChangeCmd(chg, false);
      support.addEdit(cmd);
      haveDelete = true;
    }
    chg = pdms.dropDataSourceKeys(nodeID);
    if (chg != null) {
      PertDataChangeCmd cmd = new PertDataChangeCmd(chg, false);
      support.addEdit(cmd);
      haveDelete = true;
    }

    TimeCourseDataMaps tcdm = rcx.getDataMapSrc().getTimeCourseDataMaps();
    if (tcdm != null) {
      TimeCourseChange tchg = tcdm.dropDataKeys(nodeID);
      if (tchg != null) {
        TimeCourseChangeCmd cmd = new TimeCourseChangeCmd(tchg, false);
        support.addEdit(cmd);
        haveDelete = true;
      }
    }
    
    TemporalInputRangeData tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
    if (tird != null) {
      TemporalInputChange ticChg = tird.dropDataEntryKeys(nodeID);
      if (ticChg != null) {
        TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(rcx, ticChg, false);
        support.addEdit(cmd);
        haveDelete = true;
      }
      ticChg = tird.dropDataSourceKeys(nodeID);
      if (ticChg != null) {
        TemporalInputChangeCmd cmd = new TemporalInputChangeCmd(rcx, ticChg, false);
        support.addEdit(cmd);
        haveDelete = true;
      }
    }
    return (haveDelete);
  }  
  
  /***************************************************************************
  **
  ** When we mess with dropping extra nodes from a proxy, member-only modules
  ** in overlays owned by the proxy may become empty.  Catch this case.  Note
  ** this does what the above method does, without deleting the member form
  ** from the module.  Used where other methods have been used to delete
  ** members from the module.
  */  
  
  public static boolean proxyPostExtraNodeDeletionSupport(UIComponentSource uics, StaticDataAccessContext rcx, UndoSupport support) {  
  
    NetOverlayController noc = uics.getNetOverlayController();
    Iterator<DynamicInstanceProxy> dit = rcx.getGenomeSource().getDynamicProxyIterator();
    boolean didIt = false;
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      Layout glo = rcx.getLayoutSource().getLayoutForGenomeKey(dip.getFirstProxiedKey());
      HashMap<String, Set<String>> deadMods = new HashMap<String, Set<String>>();
      Iterator<NetworkOverlay> oit = dip.getNetworkOverlayIterator();
      while (oit.hasNext()) {
        NetworkOverlay no = oit.next();
        String noID = no.getID();
        Iterator<NetModule> nmit = no.getModuleIterator();
        while (nmit.hasNext()) {
          NetModule nmod = nmit.next();
          String nmID = nmod.getID();
          if (nmod.getMemberCount() == 0) { 
            NetOverlayProperties noProps = glo.getNetOverlayProperties(noID);
            NetModuleProperties nmp = noProps.getNetModuleProperties(nmID);
            if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
              Set<String> deadSet = deadMods.get(noID);  
              if (deadSet == null) {
                deadSet = new HashSet<String>();
                deadMods.put(noID, deadSet);
              }
              deadSet.add(nmID);
            }
          }
        }
      }

      if (!deadMods.isEmpty()) {      
        Iterator<String> dmit = deadMods.keySet().iterator();
        while (dmit.hasNext()) {
          String ovrKey = dmit.next();
          Set<String> deadSet = deadMods.get(ovrKey);
          Iterator<String> dsit = deadSet.iterator();
          while (dsit.hasNext()) {
            String deadModID = dsit.next();
            OverlaySupport.deleteNetworkModule(uics, rcx, ovrKey, deadModID, support);
          }
        }
        noc.cleanUpDeletedModules(deadMods, support, rcx);
        didIt = true;
      }
    }
    return (didIt);
  }
  
  /***************************************************************************
  **
  ** Handle deletion of node from all overlays
  */   
     
  public static boolean supportNodeDeletionFromOverlays(UIComponentSource uics, NetOverlayOwner owner, StaticDataAccessContext rcx, UndoSupport support, 
                                                        String key, boolean keepEmptyMemOnly) {    
    
    boolean retval = false;
    HashMap<String, Set<String>> deadMods = new HashMap<String, Set<String>>();
    // Remove from overlay modules.
    Iterator<NetworkOverlay> oit = owner.getNetworkOverlayIterator();
    while (oit.hasNext()) {
      NetworkOverlay no = oit.next();
      String noID = no.getID();
      Iterator<NetModule> nmit = no.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        String nmID = nmod.getID();
        if (nmod.isAMember(key)) {
          if (!keepEmptyMemOnly && (nmod.getMemberCount() == 1)) { 
            NetOverlayProperties noProps = rcx.getCurrentLayout().getNetOverlayProperties(noID);
            NetModuleProperties nmp = noProps.getNetModuleProperties(nmID);
            if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
              Set<String> deadSet = deadMods.get(noID);  
              if (deadSet == null) {
                deadSet = new HashSet<String>();
                deadMods.put(noID, deadSet);
              }
              deadSet.add(nmID);
            }
          }
          NetModuleChange nmc = owner.deleteMemberFromNetworkModule(noID, nmod, key);
          if (nmc != null) {
            NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, nmc);
            support.addEdit(gcc);
            retval = true;
          }    
        }
      }
    }
    
    //
    // If a auto-node boxing module is left without members, it must be deleted!
    //
    
    if (!deadMods.isEmpty()) {
      NetOverlayController noc = uics.getNetOverlayController();
      Iterator<String> dmit = deadMods.keySet().iterator();
      while (dmit.hasNext()) {
        String ovrKey = dmit.next();
        Set<String> deadSet = deadMods.get(ovrKey);
        Iterator<String> dsit = deadSet.iterator();
        while (dsit.hasNext()) {
          String deadModID = dsit.next();
          OverlaySupport.deleteNetworkModule(uics, rcx, ovrKey, deadModID, support);
        }
      }
      noc.cleanUpDeletedModules(deadMods, support, rcx);   
    }

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a node from a subgroup
  */  
 
  public static boolean deleteNodeFromSubGroup(StaticDataAccessContext dacx, GenomeInstance gi, String groupID, 
                                               String niID, UndoSupport support, UndoFactory uFac) {                                        
    //
    // Undo/Redo support
    //
    
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.deleteNode", dacx);
    }
    
    Group group = gi.getGroup(groupID);
    GroupChange grc = group.removeMember(niID, gi.getID());
    if (grc != null) {
      GroupChangeCmd grcc = new GroupChangeCmd(dacx, grc);
      support.addEdit(grcc);
      support.addEvent(new ModelChangeEvent(dacx.getGenomeSource().getID(), gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));      
    }    

    if (localUndo) {support.finish();}    
    return (true); 
  }
}
