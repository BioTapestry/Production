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

package org.systemsbiology.biotapestry.cmd.flow.netBuild;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateSupport;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Grid;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.InstanceEngine;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.RectangularTreeEngine;
import org.systemsbiology.biotapestry.ui.dialogs.DevelopmentSpecDialog;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handle some simple (mostly obsolete) network builders
*/

public class SimpleBuilds extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum SimpleType {
    
    PERT_TO_ROOT_INST("command.AddQPCRToRootInstances", "command.AddQPCRToRootInstances", "FIXME24.gif", "command.AddQPCRToRootInstancesMnem", null),
    PROP_ROOT("command.PropRootWithExpData", "command.PropRootWithExpData", "FIXME24.gif", "command.PropRootWithExpDataMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    SimpleType(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }  
     
    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private SimpleType action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SimpleBuilds(BTState appState, SimpleType action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    if (!cache.oneEmptyRoot()) {
      return (false);
    }
    return (cache.timeCourseNotEmpty());
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
  
    while (true) {
      StepState ans;
      if (last == null) {
        ans = new StepState(appState_, action_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else {
        throw new IllegalStateException();
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {

    private String nextStep_;
    private SimpleType myAction_;
    private BTState appState_;
    private DataAccessContext dacx_;
    
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, SimpleType action, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {
      DataAccessContext rcxR = dacx_.getContextForRoot();
      switch (myAction_) {
        case PERT_TO_ROOT_INST:      
          p2RI(rcxR);
          break;  
        case PROP_ROOT:      
          pr(rcxR);
          break;
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
    
    private void p2RI(DataAccessContext rcxR) {
      int sliceMode = (new BuildSupport(appState_)).getSliceMode(appState_.getTopFrame());
      if (sliceMode == TimeCourseData.NO_SLICE) {
        return;
      }  
      addNewQPCRGenesToRootInstances(appState_, rcxR, sliceMode);
      // Now zoom it to center:
      appState_.getZoomCommandSupport().zoomToWorksheetCenter();
      return;
    }
         
    /***************************************************************************
    **
    ** Do the step
    */ 
      
    private void pr(DataAccessContext rcxR) {
      LayoutOptions lopt = appState_.getLayoutOptMgr().getLayoutOptions();
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = new FullGenomeHierarchyOracle(appState_).getGlobalNetModuleLinkPadNeeds();
      UndoSupport support = new UndoSupport(appState_, "undo.propagateRootUsingExpressionData");
      propagateRootUsingExpressionData(appState_, rcxR, lopt, globalPadNeeds, support, null, 0.1, 1.0);
      ModificationCommands.repairNetModuleLinkPadsGlobally(appState_, rcxR, globalPadNeeds, false, support); 
      support.finish();
      // Now zoom it to center:
      appState_.getZoomCommandSupport().zoomToWorksheetCenter();
      return;
    }
    
   /***************************************************************************
    **
    ** Create multiple root instances from the root based on time expression data.
    ** 06/22/09: Not used at the moment.  FIX ME! If this changes, check into the proper
    ** use of moduleShapeChange data (not supported at the moment; is this a problem?)
    */  
   
    private boolean propagateRootUsingExpressionData(BTState appState, 
                                                     DataAccessContext rcxR,
                                                     LayoutOptions options,
                                                     Map<String, Layout.PadNeedsForLayout> globalPadNeeds, UndoSupport support,
                                                     BTProgressMonitor monitor, 
                                                     double startFrac, double maxFrac) {
  
   
      try {
        Point2D center = appState.getCanvasCenter(); 
        
        // FIX ME ?? Are we doing any eventing??
       // ModelChangeEvent mcev = null;
       // GeneralChangeEvent gcev = null;    
  
        //
        // Figure out the regions and times for importing:
        //
        TimeCourseData tcd = rcxR.getExpDataSrc().getTimeCourseData();
        List<TimeCourseData.RootInstanceSuggestions> sugg = tcd.getRootInstanceSuggestions(TimeCourseData.SLICE_BY_TIMES, null);
        Map<String, Integer> regAndTimes = tcd.getRegionsWithMinTimes();
  
        DevelopmentSpecDialog dsd = new DevelopmentSpecDialog(appState, regAndTimes);
        dsd.setVisible(true);
        if (!dsd.haveResult()) {
          return (false);
        }    
  
        LayoutRubberStamper lrs = new LayoutRubberStamper(appState);
        NavTree nt = rcxR.getGenomeSource().getModelHierarchy();
        DefaultTreeModel dtm = appState.getTree().getTreeModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot();
       // HashMap newNodeTypes = new HashMap();
        //HashMap instanceContents = new HashMap();
        Iterator<TimeCourseData.RootInstanceSuggestions> sit = sugg.iterator();
        while (sit.hasNext()) {
          TimeCourseData.RootInstanceSuggestions ris = sit.next();
          //
          // Create genome instance:
          //
          String nextKey = rcxR.getGenomeSource().getNextKey();
          GenomeInstance gi = new GenomeInstance(appState, ris.heavyToString(), nextKey, null);
          rcxR.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
          String nextloKey = rcxR.getGenomeSource().getNextKey();
          Layout lo = new Layout(appState, nextloKey, nextKey);
          rcxR.lSrc.addLayout(nextloKey, lo);
          nt.addNode(ris.heavyToString(), null, nextKey);
          dtm.nodeStructureChanged(rootNode);
          DataAccessContext rcxI = new DataAccessContext(rcxR, gi, lo);
          
          //instanceContents.put(nextKey, regions);
          //
          // Iterate for each region:
          //
          Iterator<String> rit = ris.regions.iterator();
          while (rit.hasNext()) {
            String region = rit.next();
            String regionKey = rcxI.getDBGenome().getNextKey();   
            Group newGroup = new Group(rcxI.rMan, regionKey, region);
            GenomeChange gc = gi.addGroupWithExistingLabel(newGroup); 
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxI, gc);
              support.addEdit(gcc);
            }
            int groupCount = gi.groupCount();
            int order = lo.getTopGroupOrder() + 1;
            Layout.PropChange lpc = 
              lo.setGroupProperties(regionKey, new GroupProperties(groupCount, regionKey, lo, center, order, rcxI.cRes));
            if (lpc != null) {
              PropChangeCmd pcc = new PropChangeCmd(appState, rcxI, new Layout.PropChange[] {lpc});
              support.addEdit(pcc);
            }
  
            HashSet<String> regionMembers = new HashSet<String>();        
            //
            // Add all nodes from root that show up in the region
            //
            Iterator<Integer> tiit = ris.times.iterator();
            while (tiit.hasNext()) {
              int time = tiit.next().intValue();
              tcd.getExpressedGenes(region, time, ExpressionEntry.ZYGOTIC_SOURCE, regionMembers);
            }
            Iterator<Node> anit = rcxI.getDBGenome().getAllNodeIterator();
            while (anit.hasNext()) {
              Node node = anit.next();
              List<TimeCourseData.TCMapping> dataKeys = tcd.getTimeCourseTCMDataKeysWithDefault(node.getID());
              if (dataKeys != null) {
                Iterator<TimeCourseData.TCMapping> dkit = dataKeys.iterator();
                while (dkit.hasNext()) {
                  TimeCourseData.TCMapping tcm = dkit.next();
                  if (regionMembers.contains(tcm.name)) {
                    PropagateSupport.propagateNodeNoLayout(appState, (node.getNodeType() == Node.GENE), 
                                                           rcxI, (DBNode)node, newGroup, support, null);
                  }
                }
              }
            }
            Iterator<Linkage> lit = rcxI.getDBGenome().getLinkageIterator();
            GenomeInstance.GroupTuple grpTup = new GenomeInstance.GroupTuple(regionKey, regionKey); 
            while (lit.hasNext()) {
              Linkage link = lit.next();
              String src = link.getSource();
              String trg = link.getTarget();
              int srcInst = gi.getInstanceForNodeInGroup(src, regionKey);
              if (srcInst == -1) {
                continue;
              }
              int trgInst = gi.getInstanceForNodeInGroup(trg, regionKey);          
              if (trgInst == -1) {
                continue;
              }          
              PropagateSupport.propagateLinkageNoLayout(appState, rcxI, (DBLinkage)link, rcxR, grpTup, support, null);
            }
          }
          DatabaseChange dc = rcxI.lSrc.startLayoutUndoTransaction(nextloKey);
          Layout origLayout = new Layout(lo);
          try {
            // FIX ME? Does this application benefit from module shape changes--if so gather
            // the info and use it...
 
            LayoutRubberStamper.RSData rsd = 
              new LayoutRubberStamper.RSData(rcxR, rcxI, options, new Layout.OverlayKeySet(), null, monitor, null, 
                                             origLayout, false, startFrac, maxFrac, null, globalPadNeeds);
            lrs.setRSD(rsd);
            lrs.rubberStampLayout();
          } catch (AsynchExitRequestException ex) {
            rcxI.lSrc.rollbackLayoutUndoTransaction(dc);
            throw ex;
          }
          dc = rcxI.lSrc.finishLayoutUndoTransaction(dc);
          support.addEdit(new DatabaseChangeCmd(appState, rcxI, dc));
        }
  
        DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
        TreeNode[] tn = rootChild.getPath();
        TreePath tp = new TreePath(tn);
        appState.getTree().expandTreePath(tp);    
        appState.getTree().setTreeSelectionPath(tp); 
  
        appState.getZoomTarget().fixCenterPoint(true, support, false);
   
      
      } catch (AsynchExitRequestException ex) {
        support.rollback();
        return (false);
      }
        
      return (true);
    }

    /***************************************************************************
    **
    ** Create multiple root instances from QPCR data and temporal expression data.
    */  
   
    private boolean addNewQPCRGenesToRootInstances(BTState appState, DataAccessContext rcxR, int sliceMode) {
  
      Point2D center = appState.getCanvasCenter(); 
      Dimension size = appState.getCanvasSize();
      
      if (sliceMode == TimeCourseData.NO_SLICE) {
        return (false);
      }
       
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = rcxR.fgho.getGlobalNetModuleLinkPadNeeds();
        
      //
      // Undo/Redo support
      //
      
      UndoSupport support = new UndoSupport(appState, "undo.addNewQPCRGenesToRootInstances"); 
   
      // FIX ME ?? Are we doing any eventing??
      //ModelChangeEvent mcev = null;
      //GeneralChangeEvent gcev = null;    
           
      //
      // Figure out the regions and times for importing:
      //
  
      TimeCourseData tcd = rcxR.getExpDataSrc().getTimeCourseData();
      List<TimeCourseData.RootInstanceSuggestions> sugg = tcd.getRootInstanceSuggestions(sliceMode, null);
      NavTree nt = rcxR.getGenomeSource().getModelHierarchy();
      DefaultTreeModel dtm = appState.getTree().getTreeModel();
      DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)dtm.getRoot();
      HashMap<String, Integer> newNodeTypes = new HashMap<String, Integer>();
      HashMap<String, Map<String, Set<String>>> instanceContents = new HashMap<String, Map<String, Set<String>>>();
      Iterator<TimeCourseData.RootInstanceSuggestions> sit = sugg.iterator();
      while (sit.hasNext()) {
        TimeCourseData.RootInstanceSuggestions ris = sit.next();
        //
        // Create genome instance:
        //
        String nextKey = rcxR.getGenomeSource().getNextKey();
        GenomeInstance gi = new GenomeInstance(appState, ris.heavyToString(), nextKey, null);
        // FIX ME!! None of these changes are being added to undo support!<<<<<<<<
        gi.setTimes(ris.minTime, ris.maxTime);
        rcxR.getGenomeSource().addGenomeInstanceExistingLabel(nextKey, gi);
        String nextloKey = rcxR.getGenomeSource().getNextKey();
        Layout lo = new Layout(appState, nextloKey, nextKey);
        rcxR.lSrc.addLayout(nextloKey, lo);
        nt.addNode(ris.heavyToString(), null, nextKey);
        dtm.nodeStructureChanged(rootNode);
        Map<String, Set<String>> regions = new HashMap<String, Set<String>>();
        instanceContents.put(nextKey, regions);
        DataAccessContext rcxI = new DataAccessContext(rcxR, gi, lo);
        //
        // Iterate for each TCD region:
        //
        int count = 0;
        Iterator<String> rit = ris.regions.iterator();
        while (rit.hasNext()) {
          String region = rit.next();
          count++;
          //
          // Build a group for each region in the current instance:
          //
          Point2D groupCenter = new Point2D.Double(400.0 * count, 500.0);
          String regionKey = (new BuildSupport(appState_)).buildRegion(rcxI, region, groupCenter, support);
          HashSet<String> regionMembers = new HashSet<String>();        
          //
          // Create nodes in root genome:
          //
          Iterator<Integer> tiit = ris.times.iterator();
          while (tiit.hasNext()) {
            int time = tiit.next().intValue();
            tcd.getExpressedGenes(region, time, ExpressionEntry.ZYGOTIC_SOURCE, regionMembers);
          }
          Set<String> newNodeIDs = processExpressedGenes(appState, rcxR, regionMembers, newNodeTypes, support);
          regions.put(regionKey, newNodeIDs);
        }
      }
     
      DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode)rootNode.getChildAt(0);
      TreeNode[] tn = rootChild.getPath();
      TreePath tp = new TreePath(tn);
      appState.getTree().expandTreePath(tp);    
      appState.getTree().setTreeSelectionPath(tp); 
      
      //
      // Now layout the nodes:
      //
      
      RectangularTreeEngine ce = new RectangularTreeEngine();
      String[] positions = ce.layout(newNodeTypes.keySet(), rcxR.getDBGenome(), false);
  
      int posNum = positions.length;
      for (int i = 0; i < posNum; i++) {
        String geneID = positions[i];
        boolean first = true;
        NodeProperties np = null;
        Iterator<Layout> loit = rcxR.lSrc.getLayoutIterator();
        while (loit.hasNext()) {
          Layout lo = loit.next();
          String loTarg = lo.getTarget();
          if (loTarg.equals(rcxR.getDBGenome().getID())) {
            int nodeType = newNodeTypes.get(geneID).intValue();          
            if (first) {
              Point2D loc = ce.placePoint(i, positions, center, size, Grid.FIXED_RECTANGLE);
              np = new NodeProperties(rcxR.cRes, lo, nodeType, geneID, loc.getX(), loc.getY(), false);
              first = false;
            }
            Layout.PropChange[] lpc = new Layout.PropChange[1];    
            lpc[0] = lo.setNodeProperties(geneID, new NodeProperties(np, lo, nodeType));
            if (lpc[0] != null) {
              PropChangeCmd pcc = new PropChangeCmd(appState, rcxR, lpc);
              support.addEdit(pcc);
              LayoutChangeEvent lcev = new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
              support.addEvent(lcev);            
            }
          }
        }
      } 
      
      //
      // Now propagate nodes down
      //
      
   //   Layout lo = appState.getLayoutForGenomeKey(genomeKey);
      Iterator<String> giit = instanceContents.keySet().iterator();
      while (giit.hasNext()) {
        String gikey = giit.next();
        GenomeInstance gi = (GenomeInstance)rcxR.getGenomeSource().getGenome(gikey);      
        Map<String, Set<String>> regions = instanceContents.get(gikey);
        Iterator<String> reit = regions.keySet().iterator();
        while (reit.hasNext()) {
          String grKey = reit.next();
          Group group = gi.getGroup(grKey);
          Set<String> nodes = regions.get(grKey);
          Iterator<String> nit = nodes.iterator();
          while (nit.hasNext()) {
            String nodeID = nit.next();
            DBGene gene = (DBGene)rcxR.getDBGenome().getGene(GenomeItemInstance.getBaseID(nodeID));
            // Catch cases where no nodes imported - icky!
            if (gene != null) {
              DataAccessContext rcxI = new DataAccessContext(rcxR, gi);
              PropagateSupport.propagateNode(appState, true, rcxI, gene, rcxR.getLayout(), new Vector2D(0.0, 0.0), group, support);
            }
          }
        }
      }
      
      //
      // Now fixup the instance region layouts
      //
      
      InstanceEngine ie = new InstanceEngine(appState);
      
      giit = instanceContents.keySet().iterator();
      while (giit.hasNext()) {
        String gikey = giit.next();
        GenomeInstance gi = (GenomeInstance)rcxR.getGenomeSource().getGenome(gikey);
        DataAccessContext rcxI = new DataAccessContext(rcxR, gi);
        Map<String, Set<String>> regions = instanceContents.get(gikey);
        Map<String, Point2D> giPositions = ie.layout(regions, gikey);
        Map<String, List<Point2D>> groupPoints = new HashMap<String, List<Point2D>>();
        Layout.PadNeedsForLayout loPadNeeds = globalPadNeeds.get(rcxI.getLayoutID());
        Iterator<String> nit = giPositions.keySet().iterator();
        while (nit.hasNext()) {
          String nKey = nit.next();
          Point2D newPos = giPositions.get(nKey);        
          Group grp = gi.getGroupForNode(nKey, GenomeInstance.LEGACY_MODE);
          // Catch cases where no nodes imported - icky!
          if (grp == null) {
            continue;
          }
          String grid = grp.getID();
          List<Point2D> points = groupPoints.get(grid);
          if (points == null) {
            points = new ArrayList<Point2D>();
            groupPoints.put(grid, points);
          }
          points.add(newPos);
          Point2D oldPos = rcxI.getLayout().getNodeProperties(nKey).getLocation();
          Layout.PropChange[] lpc = new Layout.PropChange[1];        
          lpc[0] = rcxI.getLayout().moveNode(nKey, newPos.getX() - oldPos.getX(), 
                                        newPos.getY() - oldPos.getY(), loPadNeeds, rcxI);
          if (lpc[0] != null) {
            PropChangeCmd pcc = new PropChangeCmd(appState, rcxI, lpc);
            support.addEdit(pcc);
          }
        }
        //
        // Shift group labels around:
        //
        Iterator<String> gpit = groupPoints.keySet().iterator();
        while (gpit.hasNext()) {
          String grid = gpit.next();
          List<Point2D> points = groupPoints.get(grid);
          Set<Point2D> pointSet = new HashSet<Point2D>(points);
          Point2D newPos = AffineCombination.combination(pointSet, 10.0);
          Point2D oldPos = rcxI.getLayout().getGroupProperties(grid).getLabelLocation();        
          Layout.PropChange[] lpc = new Layout.PropChange[1];        
          lpc[0] = rcxI.getLayout().moveGroup(grid, newPos.getX() - oldPos.getX(), newPos.getY() - oldPos.getY());
          if (lpc[0] != null) {
            PropChangeCmd pcc = new PropChangeCmd(appState, rcxI, lpc);
            support.addEdit(pcc);
          }        
        }      
      }
      
      ModificationCommands.repairNetModuleLinkPadsGlobally(appState, rcxR, globalPadNeeds, false, support);
      appState.getZoomTarget().fixCenterPoint(true, support, false);
      support.finish();
      return (true);
    }
      
   /***************************************************************************
   **
   ** Process expressed genes
   */  
   
    private Set<String> processExpressedGenes(BTState appState, DataAccessContext rcxR, Set<String> expressed, 
                                              HashMap<String, Integer> newNodeTypes, UndoSupport support) {
      
      HashSet<String> retval = new HashSet<String>();
      //
      // Add genes to root if they are not present
      //
      
      TimeCourseData tcd = rcxR.getExpDataSrc().getTimeCourseData();
      Iterator<String> exit = expressed.iterator();
      while (exit.hasNext()) {
        String name = exit.next();
        Set<String> inverses = tcd.getTimeCourseDataKeyInverses(name);
        if (!inverses.isEmpty()) {  // has existing mapped node(s); skip it.
          retval.addAll(inverses);
          continue;
        }
        //
        // Look for existing nodes that are not yet mapped:
        //
        String nodeID;
        DBGene oldGene = rcxR.getDBGenome().getGeneWithName(name);
        Set<Node> oldNodes = rcxR.getDBGenome().getNodesWithName(name);
        int onSize = oldNodes.size();
        if (oldGene != null) {
          nodeID = oldGene.getID();
          if (tcd.haveDataForNode(nodeID)) {
            continue;  // skip this gene; it's mapped to somebody else!
            // FIX ME?  Pop up a dialog window?
          }
          retval.add(nodeID);
        } else if (onSize != 0) {
          if (onSize == 1) {
            DBNode oldNode = (DBNode)oldNodes.iterator().next();
            nodeID = oldNode.getID();
            if (tcd.haveDataForNode(nodeID)) {
              continue;  // skip this node; it's mapped to somebody else!
              // FIX ME?  Pop up a dialog window?
            }
            retval.add(nodeID);
          } else {
            continue;
          }
        } else {
          nodeID = rcxR.getNextKey();
          retval.add(nodeID);
          DBGene newGene = new DBGene(appState, name, nodeID);
          GenomeChange gc = rcxR.getDBGenome().addGeneWithExistingLabel(newGene);
          newNodeTypes.put(nodeID, new Integer(Node.GENE));
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcxR, gc);
            support.addEdit(gcc);
          }
        }
  
        //
        // Add mapping to time course data:
        //
        List<TimeCourseData.TCMapping> targIDs = new ArrayList<TimeCourseData.TCMapping>();
        // FIX ME???  01/23/11 Is this mapp addition stuff obsolete?  Ditch it, correct?
        targIDs.add(new TimeCourseData.TCMapping(name));
        TimeCourseChange tcc = tcd.addTimeCourseTCMMap(nodeID, targIDs, true);      
        if (tcc != null) {
          support.addEdit(new TimeCourseChangeCmd(appState, rcxR, tcc, false));
        }   
      }
      return (retval);
    } 
  }
}
