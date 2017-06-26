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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.cmd.instruct.DialogBuiltMotif;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.ColorAssigner;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.Grid;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.MinimalDispOptMgr;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NetModuleShapeFixer;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.RectangularTreeEngine;
import org.systemsbiology.biotapestry.ui.SpecialSegmentTracker;
import org.systemsbiology.biotapestry.ui.layouts.ColorTypes;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.ui.layouts.LinkBundleSplicer;
import org.systemsbiology.biotapestry.ui.layouts.NetModuleLinkExtractor;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyInstructions;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutData;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutLinkData;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.DistanceMeasurer;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.PatternPlacerVerticalNesting;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Support for link layout processing
*/

public class LayoutLinkSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor:
  */ 
  
  private LayoutLinkSupport() {
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Run an optimization pass
  */  
 
  public static LinkRouter.RoutingResult optimizeLinks(Set<String> links, Set<Point2D> frozen, 
                                                       StaticDataAccessContext rcx, UndoSupport support,
                                                       boolean allowReroutes, BTProgressMonitor monitor, 
                                                       double startFrac, double endFrac) 
                                                         throws AsynchExitRequestException {
                            
    LinkRouter.RoutingResult retval = new LinkRouter.RoutingResult();

    //
    // Start layout undo operations:
    //
          
    DatabaseChange dc = rcx.getLayoutSource().startLayoutUndoTransaction(rcx.getCurrentLayoutID());
    
    //
    // Init the router placement:
    //
    
    try {    
      LinkRouter router = new LinkRouter();
      LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);
      rcx.getCurrentLayout().optimizeLinks(links, routerGrid, rcx, null, frozen, allowReroutes, monitor, 1, startFrac, endFrac);
   
      //
      // Report ambiguous color overlaps
      //
      ColorAssigner ca= new ColorAssigner();
      ColorAssigner.ColorIssues issues = ca.hasAmbiguousCrossing(rcx, routerGrid); 
      if (issues != null) {
        retval.colorResult = ColorAssigner.COLOR_COLLISION;
        retval.linkResult |= LinkRouter.COLOR_PROBLEM;
        retval.collisionSrc1 = (issues.collisionSrc1 == null) ? retval.collisionSrc1 : issues.collisionSrc1;
        retval.collisionSrc2 = (issues.collisionSrc2 == null) ? retval.collisionSrc2 : issues.collisionSrc2; 
      } 
      
      dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(rcx, dc));
      return (retval);    
    } catch (AsynchExitRequestException ex) {
      rcx.getLayoutSource().rollbackLayoutUndoTransaction(dc);
      throw ex;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public static void offerColorFixup(UIComponentSource uics, DataAccessContext dacx, LinkRouter.RoutingResult result, UndoFactory uFac) {
    if (uics.isHeadless()) {
      return;
    }
    if ((result.linkResult & LinkRouter.COLOR_PROBLEM) != 0x00) {    
      MinimalDispOptMgr dopmgr = dacx.getDisplayOptsSource();
      DisplayOptions dopt = dopmgr.getDisplayOptions();
      if (dopt.getBranchMode() == DisplayOptions.NO_BUS_BRANCHES) {
        ResourceManager rMan = dacx.getRMan();          
        int doBranches = 
          JOptionPane.showConfirmDialog(uics.getTopFrame(), 
                                        rMan.getString("reassignColors.doBranches"), 
                                        rMan.getString("reassignColors.doBranchesTitle"),
                                        JOptionPane.YES_NO_OPTION);        
        if (doBranches == JOptionPane.YES_OPTION) {
          UndoSupport supportTos = uFac.provideUndoSupport("undo.turnOnSpecialBranches", dacx);
          dopmgr.turnOnSpecialLinkBranches(supportTos, dacx, true); 
        }
      }
    }
    return;
  }    
 
  /***************************************************************************
  **
  */  
  
  public static LinkRouter.RoutingResult autoLayoutHierarchical(StaticDataAccessContext rcxR, Point2D center, 
                                                                Map<String, Integer> newNodeTypes, Set<String> newLinks,
                                                                Dimension size, UndoSupport support,
                                                                List<DialogBuiltMotif> motifs, 
                                                                boolean incremental, Map<String, String> colorMap,
                                                                Map<String, PadConstraints> padConstraints, LayoutOptions options,
                                                                BTProgressMonitor monitor, double startFrac, double endFrac,
                                                                Layout.SupplementalDataCoords sdc, 
                                                                Map<String, BusProperties.RememberProps> rememberLinkProps) 
                                                                  throws AsynchExitRequestException {      
     //
     // Now layout the nodes:
     //
     boolean strictOKGroups = false;
     Set<String> newNodes = newNodeTypes.keySet();
    
     RectangularTreeEngine ce = new RectangularTreeEngine();

     int count = rcxR.getCurrentGenome().getFullNodeCount();
     if (newNodes.size() == count) {
       incremental = false;
     }
     
     Map<String, Layout.OverlayKeySet> allKeys = rcxR.getFGHO().fullModuleKeysPerLayout();
     Layout.OverlayKeySet loModKeys = allKeys.get(rcxR.getCurrentLayoutID()); 
  
     Grid grid = ce.layoutMotifsHier(motifs, newNodes, rcxR, options, incremental, null, false);
     int colNum = grid.getNumCols();
     if (colNum == 0) {
       return (new LinkRouter.RoutingResult()); // nothing to do
     }
     int rowNum = grid.getNumRows();
     //
     // Locate new grid wrt old one.  If we are incremental with no previous nodes, drop the
     // incremental call
     //

     LinkRouter router = new LinkRouter();
     Point2D corner = null;
     if (incremental && (!newNodes.isEmpty())) {
       LinkPlacementGrid oldLinkGrid = router.initGrid(rcxR, null, INodeRenderer.STRICT, monitor);
       PatternGrid oldGrid = oldLinkGrid.extractPatternGrid(false);
       MinMax xRange = oldGrid.getMinMaxXForRange(null);
       double leftie = calcIncrementalGridSpacing(rcxR, newNodes, grid, xRange.min * 10, options);
       Pattern pat = grid.buildPattern();
       int pad = (int)Math.round(grid.getRowSpace() / 20.0);
       PatternPlacerVerticalNesting ppt = 
         new PatternPlacerVerticalNesting(oldGrid, pat, (int)leftie / 10, pad, true);
       Point topCorner = ppt.locatePattern();
       if (topCorner == null) {
         corner = findBestVerticalPosition(rcxR, grid, newNodeTypes, leftie);
       } else {      
         Point topCorner10 = new Point(topCorner.x * 10, topCorner.y * 10);
         PatternPlacerVerticalNesting ppb = 
           new PatternPlacerVerticalNesting(oldGrid, pat, (int)leftie / 10, pad, false);
         Point botCorner = ppb.locatePattern();
         Point botCorner10 = new Point(botCorner.x * 10, botCorner.y * 10);
         boolean useTop = topIsBest(rcxR, newNodes, grid, topCorner10, botCorner10);
         if (useTop) {
           corner = new Point2D.Double(topCorner10.x, topCorner10.y);
           ppt.sinkPattern(topCorner);
         } else {
           corner = new Point2D.Double(botCorner10.x, botCorner10.y);
           ppb.sinkPattern(botCorner);
         }
       }
     }

     //
     // Get the points down:
     //
     
     if (!(incremental && (corner == null))) {  // nothing to do...
       for (int i = 0; i < rowNum; i++) {
         for (int j = 0; j < colNum; j++) {      
           String geneID = grid.getCellValue(i, j);
           if (geneID == null) {
             continue;
           }
           int nodeType = newNodeTypes.get(geneID).intValue();          
           Point2D loc = (incremental) ? grid.getGridLocation(i, j, corner)
                                       : ce.placePoint(i * colNum + j, grid, center, size);
           NodeProperties np = new NodeProperties(rcxR.getColorResolver(), nodeType, geneID, 0.0, 0.0, false);
           double yOffset = np.getRenderer().getStraightThroughOffset();
           double xOffset = np.getRenderer().getVerticalOffset();
           np.setLocation(new Point2D.Double(loc.getX() + xOffset, loc.getY() + yOffset));
           Layout.PropChange[] lpc = new Layout.PropChange[1];    
           lpc[0] = rcxR.getCurrentLayout().setNodeProperties(geneID, new NodeProperties(np, nodeType));
           if (lpc != null) {
             PropChangeCmd pcc = new PropChangeCmd(rcxR, lpc);
             support.addEdit(pcc);
             LayoutChangeEvent lcev = new LayoutChangeEvent(rcxR.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
             support.addEvent(lcev);            
           }
         }
       }
     }

     //
     // prep the layout
     //
     
     //
     // Crude autolayout:
     //
     
     StaticDataAccessContext rcxt = new StaticDataAccessContext(rcxR);
     rcxt.setLayout(new Layout(rcxR.getCurrentLayout()));
     gridlessLinkLayoutPrep(rcxt, newLinks, rememberLinkProps);
     
     //
     // Wiggle pad assignments based on node locations:
     //
     
     wigglePads(rcxt, padConstraints, support); 
     
     
     double fullFrac = endFrac - startFrac;
     double frac1 = fullFrac * 0.20;
     double eFrac1 = startFrac + frac1;
     double frac2 = fullFrac * 0.30;
     double eFrac2 = eFrac1 + frac2;
     double frac3 = fullFrac * 0.10;
     double eFrac3 = eFrac2 + frac3;  
     double frac4 = fullFrac * 0.40;
     double eFrac4 = eFrac3 + frac4;     

     //
     // Clean up the links:
     //
     
     DatabaseChange dc = rcxR.getLayoutSource().startLayoutUndoTransaction(rcxR.getCurrentLayoutID()); 
     try {   
       HashSet<String> needColors = new HashSet<String>();
       LinkRouter.RoutingResult retval = router.multiPassLayout(newLinks, rcxt,
                                                                options, monitor, needColors, startFrac, eFrac1, null, false); 
       
       // Only iterate on failed layout from scratch:
       if (((retval.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) || incremental) {
         rcxR.getCurrentLayout().replaceContents(rcxt.getCurrentLayout());
       } else {
         Set<String> targCollide = rcxR.getCurrentGenome().hasLinkTargetPadCollisions();
         if (!targCollide.isEmpty()) {
           rcxR.getCurrentLayout().replaceContents(rcxt.getCurrentLayout());
         } else {
          boolean success = false;
          Layout baseLayout = rcxt.getCurrentLayout();
          int maxPasses = 2;  // (exponential) magic number; too big, and we can run out of memory 
          double fracInc = frac2 / maxPasses;
          double halfFi = fracInc / 2.0;
          double cFrac = eFrac1;
          StaticDataAccessContext rcxE = new StaticDataAccessContext(rcxR);
          for (int mult = 1; mult <= maxPasses; mult++) {      
             rcxE.setLayout(new Layout(baseLayout));
             TreeSet<Integer> insertRows = new TreeSet<Integer>();
             TreeSet<Integer> insertCols = new TreeSet<Integer>();      
             rcxE.getCurrentLayout().chooseExpansionRows(rcxE, 1.0, 1.0, null, loModKeys, insertRows, insertCols, false, monitor);
             rcxE.getCurrentLayout().expand(rcxE, insertRows, insertCols, mult, false, null, null, null, monitor, cFrac, cFrac + halfFi);
             cFrac += halfFi;
             LinkRouter.RoutingResult eres = relayoutLinks(rcxE, null, options, false, null,
                                                           monitor, cFrac, cFrac + halfFi, strictOKGroups);
             cFrac += halfFi;
             if ((eres.linkResult & LinkRouter.LAYOUT_PROBLEM) == 0x00) {
               rcxR.getCurrentLayout().replaceContents(rcxE.getCurrentLayout());
               retval = eres;
               success = true;
               break;
             } 
             baseLayout = rcxE.getCurrentLayout();
           }

           if (!success) {
             rcxR.getCurrentLayout().replaceContents(rcxt.getCurrentLayout()); // zero-expansion version...
           }
         }
       }

       //
       // No matter what, we will drop useless corners (used to be this was only done
       // during link optimization:
       //
       
       LinkPlacementGrid routerGrid = router.initGrid(rcxR, null, INodeRenderer.STRICT, monitor);
       rcxR.getCurrentLayout().dropUselessCorners(rcxR, null, eFrac2, eFrac3, monitor);
       // Empty hash set means no points are being pinned:
       rcxR.getCurrentLayout().optimizeLinks(newLinks, routerGrid, rcxR, null, new HashSet<Point2D>(), true, monitor, 
                                 options.optimizationPasses, eFrac3, eFrac4);

       //
       // We reassign colors, keeping them if possible, but there may be
       // changes to avoid _potential_ (maybe non-existent) link overlaps:
       //
       ColorAssigner ca = new ColorAssigner();
       ColorAssigner.ColorIssues issues = ca.assignColors(rcxR, routerGrid, colorMap, 
                                                          false, incremental, needColors);

       if (issues.status != ColorAssigner.COLORING_OK) {
         retval.linkResult |= LinkRouter.COLOR_PROBLEM;
         retval.colorResult = issues.status;
         retval.collisionSrc1 = (issues.collisionSrc1 == null) ? retval.collisionSrc1 : issues.collisionSrc1;
         retval.collisionSrc2 = (issues.collisionSrc2 == null) ? retval.collisionSrc2 : issues.collisionSrc2;      
       }
       
       rcxR.getCurrentLayout().applySupplementalDataCoords(sdc, rcxR, loModKeys);


       dc = rcxR.getLayoutSource().finishLayoutUndoTransaction(dc);
       support.addEdit(new DatabaseChangeCmd(rcxR, dc));

       return (retval);
     } catch (AsynchExitRequestException ex) {
       rcxR.getLayoutSource().rollbackLayoutUndoTransaction(dc);
       throw ex;
     }    
   }
  
  /***************************************************************************
  **
  ** Answer if top or bottom nesting is best
  */  
   
  private static boolean topIsBest(StaticDataAccessContext rcx, Set<String> newNodes, Grid grid, Point topCorner, Point bottomCorner) {
    if (topCorner.equals(bottomCorner)) {
      return (true);
    }
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    double topDiffSum = 0.0;
    double botDiffSum = 0.0;
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      boolean srcIsNew = newNodes.contains(src);
      boolean trgIsNew = newNodes.contains(trg);
      if (srcIsNew == trgIsNew) {  // skip non-spanning links
        continue;
      }
      Point2D srcLocTop;
      Point2D srcLocBot;
      if (srcIsNew) {
        srcLocTop = grid.getGridLocation(src, topCorner);
        srcLocBot = grid.getGridLocation(src, bottomCorner);
      } else {
        NodeProperties sp = rcx.getCurrentLayout().getNodeProperties(src);
        srcLocTop = sp.getLocation();
        srcLocBot = srcLocTop;
      }
      Point2D trgLocTop;
      Point2D trgLocBot;
      if (trgIsNew) {
        trgLocTop = grid.getGridLocation(trg, topCorner);
        trgLocBot = grid.getGridLocation(trg, bottomCorner);
      } else {
        NodeProperties tp = rcx.getCurrentLayout().getNodeProperties(trg);
        trgLocTop = tp.getLocation();
        trgLocBot = trgLocTop;
      }
      
      topDiffSum += Math.abs(trgLocTop.getY() - srcLocTop.getY());
      botDiffSum += Math.abs(trgLocBot.getY() - srcLocBot.getY());
    }
    return (topDiffSum <= botDiffSum);
  }
     
  /***************************************************************************
  **
  ** Layout requested links across all global requests.
  */  
 
  public static LinkRouter.RoutingResult relayoutLinksGlobally(StaticDataAccessContext dacx, List<GlobalLinkRequest> requests, UndoSupport support,
                                                               LayoutOptions options, 
                                                               BTProgressMonitor monitor,
                                                               double startFrac, double maxFrac) 
                                                                 throws AsynchExitRequestException {
    LinkRouter.RoutingResult retval = new LinkRouter.RoutingResult();
    int numReq = requests.size();
    double currProg = startFrac;
    double perReqFrac = (numReq == 0) ? 0.0 : ((maxFrac - startFrac) / numReq);
    boolean strictOKGroups = false;
 
    for (int i = 0; i < numReq; i++) {
      GlobalLinkRequest glr = requests.get(i);
      Genome genome = glr.genome;
      GenomeSource gSrc = glr.gSrc;
      Layout lo = glr.layout;
      Set<String> badLinks = glr.badLinks;
      StaticDataAccessContext rcx = new StaticDataAccessContext(dacx, gSrc, genome, lo);
      LinkRouter.RoutingResult oneRes = relayoutLinks(rcx, support, options, false, 
                                                      badLinks, monitor, currProg, currProg + perReqFrac, strictOKGroups);
      retval.merge(oneRes);
      currProg += perReqFrac;
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Layout just links.  Can force this to either do all links, some links, or just
  ** "diagonal" links.
  */  
 
  public static LinkRouter.RoutingResult relayoutLinks(StaticDataAccessContext rcx, UndoSupport support,
                                                       LayoutOptions options, 
                                                       boolean doFull, Set<String> badLinks, BTProgressMonitor monitor,
                                                       double startFrac, double endFrac, boolean strictOKGroups) 
                                                         throws AsynchExitRequestException {
  
    //
    // Stash old colors of links for reuse, even if we discard layout:
    //
    
    HashMap<String, String> colorMap = new HashMap<String, String>();
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      BusProperties lp = rcx.getCurrentLayout().getLinkProperties(link.getID());
      String oldColor = lp.getColorName();
      colorMap.put(link.getID(), oldColor);
    }   
    
    LinkRouter router = new LinkRouter(); 
    Set<String> newLinks = null;
    if (!doFull) {
      //
      // FIX ME!  We get spurious bad runs because sometimes non-ortho
      // link corners fall on top of a good run.  Skip rendering
      // non-ortho links!
      //
      
      Set<String> badRuns = null;
      if (badLinks == null) {
        // is this ever called anymore?  need !doFull with no links-> no longer called
        LinkPlacementGrid oldRouterGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);
        newLinks = rcx.getCurrentLayout().discoverNonOrthoLinks(rcx);
        badRuns = oldRouterGrid.findBadRuns();
      } else {
        newLinks = badLinks;
      }
      // is this ever called anymore?  need !doFull with no links-> no longer called
      if (badRuns != null) {
        Iterator<Linkage> brlit = rcx.getCurrentGenome().getLinkageIterator();
        while (brlit.hasNext()) {
          Linkage link = brlit.next();
          if (badRuns.contains(link.getSource())) {
            newLinks.add(link.getID());
          }
        }
      }
    }
   
    //
    // Drop relevant link properties.  For full/all diagonal cases, we drop
    // the whole property.  For specific badLink requests, we prune the properties
    //
          
    DatabaseChange dc = null;
    if (support != null) {
      dc = rcx.getLayoutSource().startLayoutUndoTransaction(rcx.getCurrentLayoutID());
    }

    try {
      Map<String, SpecialSegmentTracker> specials = rcx.getCurrentLayout().rememberSpecialLinks();

      Map<String, BusProperties.RememberProps> rememberProps = null;
      if (badLinks == null) {
        rememberProps = rcx.getCurrentLayout().dropSelectedLinkProperties(newLinks, rcx);
      } else {
        rememberProps = new HashMap<String, BusProperties.RememberProps>();
        Iterator<String> nlit = newLinks.iterator();
        while (nlit.hasNext()) {
          String lid = nlit.next();
          rcx.getCurrentLayout().removeLinkPropertiesAndRemember(lid, rememberProps, rcx);
        }
      }
      
      //
      // For dropping some intervening bad (i.e. non-ortho) segments, WHILE STILL KEEPING THE
      // DOWNSTREAM STRUCTURE, we need to save the downstream structure!
      //
      
      //findChopSegmentIDs();
     // saveAndChop();
     
      //
      // Build list for full drop case:
      //

      if (doFull) {
        newLinks = new HashSet<String>();
        lit = rcx.getCurrentGenome().getLinkageIterator();
        while (lit.hasNext()) {
          Linkage link = lit.next();
          newLinks.add(link.getID());
        }
      }    

      //
      // Now add preliminary new links:
      //

      Iterator<String> nlit = newLinks.iterator();
      while (nlit.hasNext()) {
        String linkID = nlit.next();
        autoAddCrudeLinkProperties(rcx, linkID, null, rememberProps);
      }
      
      double fullFrac = endFrac - startFrac;
      double frac1 = fullFrac * 0.60;
      double eFrac1 = startFrac + frac1;
      double frac2 = fullFrac * 0.40;
      double eFrac2 = eFrac1 + frac2;
      
      //
      // Clean up the links:
      //

      HashSet<String> needColors = new HashSet<String>();
      LinkRouter.RoutingResult retval = router.multiPassLayout(newLinks, rcx,
                                                               options, monitor, needColors, 
                                                               startFrac, eFrac1, null, strictOKGroups); 

      //
      // Makes sense to now do links over again individually, now that some sort of path has been
      // trailblazed.  Lots of reservations for future links are not held anymore.  NOTE: Default
      // options have the pass count set to 0!
      //
      
      LinkPlacementGrid routerGrid = router.initGrid(rcx, retval.failedLinks, INodeRenderer.STRICT, monitor);
      // Empty hash set means no points are being pinned:
      rcx.getCurrentLayout().optimizeLinks(newLinks, routerGrid, rcx, null, new HashSet<Point2D>(), 
                               true, monitor, options.optimizationPasses, eFrac1, eFrac2);

      //
      // We do not reassign colors, but check for link overlaps:
      //    
      ColorAssigner ca = new ColorAssigner();
      ColorAssigner.ColorIssues issues = ca.assignColors(rcx, routerGrid, colorMap, 
                                                         true, true, needColors);

      if (issues.status != ColorAssigner.COLORING_OK) {
        retval.linkResult |= LinkRouter.COLOR_PROBLEM;
        retval.colorResult = issues.status;
        retval.collisionSrc1 = (issues.collisionSrc1 == null) ? retval.collisionSrc1 : issues.collisionSrc1;
        retval.collisionSrc2 = (issues.collisionSrc2 == null) ? retval.collisionSrc2 : issues.collisionSrc2;
      }

      //
      // Get special link segments back up to snuff:
      //

      rcx.getCurrentLayout().restoreSpecialLinks(specials);    

      if (monitor != null) {
        if (!monitor.updateProgress((int)(endFrac * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }        
      
      //
      // Finish up undo transaction:
      //

      if (support != null) {
        dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(rcx, dc));
      }
      return (retval);
    
    } catch (AsynchExitRequestException ex) {
      // Adding conditional to fix BT-10-27-09:5
      if (support != null) {
        rcx.getLayoutSource().rollbackLayoutUndoTransaction(dc);
      }
      throw ex;
    }
  }  
  
  /***************************************************************************
  **
  ** Recolor links to remove ambiguity
  */
 
  public static LinkRouter.RoutingResult reassignColors(StaticDataAccessContext rcx, UndoSupport support) {
  
    //
    // Build maps and sets
    //
    
    HashSet<String> seenSrcs = new HashSet<String>();
    HashSet<String> needColors = new HashSet<String>();    
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      if (!seenSrcs.contains(src)) {
        seenSrcs.add(src);
        needColors.add(link.getID());
      }
    }
    
    LinkRouter.RoutingResult retval = new LinkRouter.RoutingResult();
    
    if (needColors.isEmpty()) {
      return (retval);
    }
    
    DatabaseChange dc = (support != null) ? rcx.getLayoutSource().startLayoutUndoTransaction(rcx.getCurrentLayoutID()) : null;
    LinkRouter router = new LinkRouter(); 
    LinkPlacementGrid routerGrid;
    try {
      routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, null);          
    } catch (AsynchExitRequestException ex) {
      throw new IllegalStateException();  // monitor is null, so never happens
    }
    ColorAssigner ca= new ColorAssigner();
    ColorAssigner.ColorIssues issues = ca.hasAmbiguousCrossing(rcx, routerGrid); 
    if (issues == null) {
      return (retval);
    }  

    issues = ca.assignColors(rcx, routerGrid, null, false, false, needColors);
       
    if (issues.status != ColorAssigner.COLORING_OK) {
      retval.linkResult |= LinkRouter.COLOR_PROBLEM;
      retval.colorResult = issues.status;
      retval.collisionSrc1 = (issues.collisionSrc1 == null) ? retval.collisionSrc1 : issues.collisionSrc1;
      retval.collisionSrc2 = (issues.collisionSrc2 == null) ? retval.collisionSrc2 : issues.collisionSrc2;
    }
    
    if (support != null) {
      dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
      support.addEdit(new DatabaseChangeCmd(rcx, dc));
      support.finish();
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Layout links for specialty cases
  */  
 
  public static LinkRouter.RoutingResult runSpecialtyLinkPlacement(SpecialtyLayoutEngine sle,
                                                                   UndoSupport support, 
                                                                   BTProgressMonitor monitor,                                                         
                                                                   double startFrac, double maxFrac)
                                                                     throws AsynchExitRequestException {
    Vector2D shiftedAmount = sle.getShiftedAmount();
    shiftedAmount.setXY(0.0, 0.0);
    List<GenomeSubset> subsetList = sle.getSortedGenomeSubsets();
    int numSub = subsetList.size();
    if (numSub == 0) {
      return (new LinkRouter.RoutingResult());
    }
    
    SpecialtyLayoutEngine.GlobalSLEState gss = sle.getGSS();
    GenomeSubset firstSub = subsetList.get(0);
    StaticDataAccessContext rcx = sle.getRcx();
    StaticDataAccessContext rcxMod = sle.getWorkingRcx();
      
    List<SpecialtyInstructions> propsList = sle.getInstructions();
    
    boolean isCompleteGenome = (numSub == 1) && (firstSub.isCompleteGenome());
    
    String overlayKey = firstSub.getOverlayID();
     
    //
    // Gather up colors:
    //
    
    ColorTypes colorType;
    boolean bubblesOn = false;
    boolean checkColors = false;
    HashMap<String, String> allNodeColors = new HashMap<String, String>();
    HashMap<String, String> allLinkColors = new HashMap<String, String>();
  
    if (isCompleteGenome) {
      SpecialtyInstructions props = propsList.get(0);
      if (!props.nodeColors.isEmpty() || !props.linkColors.isEmpty()) { // HALO CASE
        colorType = ColorTypes.KEEP_COLORS;
        allNodeColors.putAll(props.nodeColors); 
        allLinkColors.putAll(props.linkColors);
      } else {
        colorType = props.colorStrategy;
      }
      bubblesOn = props.bubblesOn;
      checkColors = props.checkColorOverlap;     
    } else {
      colorType = ColorTypes.KEEP_COLORS;
      for (int i = 0; i < numSub; i++) {
        SpecialtyInstructions props = propsList.get(i);
        if (i == 0) {
          bubblesOn = props.bubblesOn;
          checkColors = props.checkColorOverlap;
        }
        allNodeColors.putAll(props.nodeColors); 
        allLinkColors.putAll(props.linkColors);     
      }
    }
     
    //
    // Stash old colors of links for reuse, even if we discard layout:
    //
    
    int linkCount = 0;    
    HashMap<String, String> colorMap = new HashMap<String, String>();
   
    
    
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      BusProperties lp = rcxMod.getCurrentLayout().getLinkProperties(link.getID());
      String oldColor = lp.getColorName();
      colorMap.put(link.getID(), oldColor);
      linkCount++;
    }
    
    LinkRouter router = new LinkRouter();
    String loID = rcx.getCurrentLayoutID();
    DatabaseChange dc = (support != null) ? rcx.getLayoutSource().startLayoutUndoTransaction(loID) : null;
    try { 
      //System.out.println(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
      rcx.fullLayoutReplacement(loID, rcxMod.getCurrentLayout());
      
      // For non-link layout debugging exit:
      // if (support != null) {
      //  dc = db.finishLayoutUndoTransaction(dc);
      //  support.addEdit(new DatabaseChangeCmd(dc));
      // return (new LinkRouter.RoutingResult());
      // } 
      
      double currStart = startFrac;  
      
      ArrayList<Map<String, SpecialtyLayoutLinkData>> forRecoveriesOut = new ArrayList<Map<String, SpecialtyLayoutLinkData>>();
      HashMap<String, SpecialtyLayoutLinkData> alienSources = new HashMap<String, SpecialtyLayoutLinkData>();
      HashMap<String, SpecialtyLayoutLinkData> srcToBetween = new HashMap<String, SpecialtyLayoutLinkData>();
      HashSet<String> newLinks = new HashSet<String>();
      Map<String, SpecialSegmentTracker> specials = rcx.getCurrentLayout().rememberSpecialLinks();
      // The first call really only works for full-on drops.  The second one is needed
      // to repair partial deletions
      if (isCompleteGenome) { 
        newLinks.addAll(DataUtil.setFromIterator(firstSub.getLinkageSuperSetIterator()));
        rcx.getCurrentLayout().dropSelectedLinkProperties(null, rcx);
      } else {
        HashMap<String, BusProperties.RememberProps> fakeRememberProps = new HashMap<String, BusProperties.RememberProps>();
        forRecoveriesOut.addAll(sle.getForRecoveriesOut());
        alienSources.putAll(sle.getAlienSources()); 
        srcToBetween.putAll(sle.getSrcToBetween());        
        //
        // Ditch the in/out/between links:
        //       
        newLinks.addAll(sle.getNewLinks());      
        Iterator<String> oosit = newLinks.iterator();
        while (oosit.hasNext()) {
          String lid = oosit.next();
          rcx.getCurrentLayout().removeLinkPropertiesAndRemember(lid, fakeRememberProps, rcx);
        }
      }
                 
      //
      // Get ready to go.  We are first creating crude links for the new guys as a starting point
      // for our reconstruction:
      //
     
      Map<String, BusProperties.RememberProps> rememberProps = sle.getRememberProps();
      gridlessLinkLayoutPrep(rcx, newLinks, rememberProps);      

      //
      // Get the splice plans, then do the boundary splicing:
      //
       
      Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> overlaySplicePlans = null;
      if (!isCompleteGenome) {
        overlaySplicePlans = sle.getSplicePlans();
        Map<Integer, LinkBundleSplicer.SpliceSolution> nonOverlaySolutions = sle.getNonOverlaySplicePlans();
        spliceBoundaries(forRecoveriesOut, alienSources, propsList, numSub, nonOverlaySolutions, overlaySplicePlans);
      }      
     
      //
      // Here we go!  Actually install the link routes into the layout!
      //

      double midFrac1 = currStart + ((maxFrac - currStart) * 0.50);
      double midFrac2 = currStart + ((maxFrac - currStart) * 0.55);
      double midFrac3 = currStart + ((maxFrac - currStart) * 0.60);
      double midFrac4 = currStart + ((maxFrac - currStart) * 0.65);
      double midFrac5 = currStart + ((maxFrac - currStart) * 0.70);
      double midFrac6 = currStart + ((maxFrac - currStart) * 0.75);

      NetModuleLinkExtractor.SubsetAnalysis sa = sle.getSubsetAnalysis();
      LinkRouter.RoutingResult retval = router.specialtyLayout(subsetList, propsList, alienSources, 
                                                               srcToBetween, sa, newLinks,
                                                               rcx, overlaySplicePlans, monitor, currStart, midFrac1);
      //
      // OK, for region layouts, we depend upon the LayoutRubberStamper to successfully expand
      // the layout and recover the inter-region links in usable form.  Sometimes, that doesn't
      // happen, since crap happens.  The result is that there are no decent links to chop
      // at the region boundaries, so we end up with bogus stuff.  If that is the case, make a
      // last-ditch attempt here to layout the failed inter-region links.  The results may not
      // be very appealing, but they are orthogonal.
      //
           
      Set<String> ultimateFailures = sle.getUltimateFailures();
      if (!ultimateFailures.isEmpty()) {
        LayoutOptions options = rcx.getLayoutOptMgr().getLayoutOptions(); 
        relayoutLinks(rcx, null, options, false, ultimateFailures, monitor, midFrac1, midFrac2, false);
      }
      
      //
      // Get special link segments back up to snuff:
      //

      rcx.getCurrentLayout().restoreSpecialLinks(specials);
      
      //
      // Get rid of useless corners:
      //
    
      rcx.getCurrentLayout().dropUselessCorners(rcx, null, midFrac2, midFrac3, monitor);
            
      //
      // FIXME
      // Until this operation can be tailored to a subset, we gotta
      // skip it unless we are doing the whole thing!
      // ATTENTION!!! IMPORTANT NOTE!  This is the step that causes direct drops into fanout nodes
      // where the VfA per-region version does NOT go direct; it does the standard dog-leg!  There is
      // no code to do direct into the fan out other than this (I think) 11/26/12.
      //
    
      if (isCompleteGenome && !gss.dataIterator().next().nps.pureCoreNetwork) {
        rcx.getCurrentLayout().eliminateStaggeredRuns(10, rcx, null, null, midFrac3, midFrac4, monitor);
      }
      
      //
      // If a subset, repair non-ortho segs of inbound/outbound link trees.  In all
      // cases, fix the topologies!
      //
      
      if (!isCompleteGenome && (numSub == 1)) {
        Set<String> fixLinks = sle.getNewLinks();
        List<LinkProperties> lps = rcx.getCurrentLayout().listOfProps(rcx.getCurrentGenome(), null, fixLinks);
        int numLps = lps.size();
        for (int i = 0; i < numLps; i++) {
          LinkProperties lp = lps.get(i);
          // Comment out to debug auto layout
          rcx.getCurrentLayout().fixAllNonOrthoForTree(lp, rcx, false, null, monitor, midFrac4, midFrac5);
        }
      }
      // Comment out to debug auto layout
      rcx.getCurrentLayout().repairAllTopologyForAuto(rcx, null, monitor, midFrac5, midFrac6); 
   
      //
      // Assign colors either by map or by auto-assignment.  If user chose not to
      // assign, we check if there is new ambiguity.
      //
      doColorOperations(colorType, checkColors, router, allNodeColors, 
                        allLinkColors, rcx, sle.getGSS(), retval, monitor);
      
      //
      // Do independent per-cluster layout compression steps:
      //
      
      int boundCount = 0;
      for (int i = 0; i < numSub; i++) {
        SpecialtyInstructions sip = propsList.get(i);
        if (sip.lcf != null) {
          boundCount += sip.lcf.boundCount();
        }
      }

      //
      // Note the semantics of bounds!  For example, the X bounds are
      // used to limit what elements are considered while finding empty ROWS, but they
      // do NOT limit what columns are returned!  So we need to trim the returned rows
      // and columns by the bounds.  In other words, it is NOT a no-op to trim the 
      // results returned by the bounds we sent in.
      // Also, there was a bogus pad of 2000 added to the left side of the bounds
      // during development.  This was to insure that link junctions in the left-hand
      // traces were used for compression calcs.  BUT it turns out that we now automatically
      // compress the link traces to minimum distance anyway during maximum compression, so
      // that is no longer relevant.
      //
      // 11/28/12: NOPE!  By using a trace width of 10 for the maximum compressed case, it is
      // true that most of the time we get OK results.  But the left-hand link junctions are
      // actually being placed based on uncompressed row heights.  Thus, if there is any
      // vertical compression to be gained, these junctions are going to start to drift down!
      //
      // The easy way to fix this is to look for the non-ortho connection cases and fix them!
      //
      
      double boundStart = midFrac2;
      double boundInc = (maxFrac - boundStart) / boundCount;
      for (int i = 0; i < numSub; i++) {
        SpecialtyInstructions sip = propsList.get(i);
        if (sip.lcf != null) {
          Iterator<Rectangle> bit = sip.lcf.getBounds();
          while (bit.hasNext()) {
            Rectangle brect = bit.next();
            SortedSet<Integer> useEmptyRows = new TreeSet<Integer>();
            SortedSet<Integer> useEmptyCols = new TreeSet<Integer>();
            brect.x += (int)shiftedAmount.getX();
            brect.y += (int)shiftedAmount.getY();
            rcx.getCurrentLayout().chooseCompressionRows(rcx, 1.0, 1.0, brect, false, null, useEmptyRows, useEmptyCols, monitor);
            useEmptyRows = DataUtil.trimOut(useEmptyRows, brect.y / 10, (brect.y + brect.height) / 10);
            useEmptyCols = DataUtil.trimOut(useEmptyCols, brect.x/ 10, (brect.x + brect.width) / 10);
            double boundEnd = boundStart + boundInc;
            rcx.getCurrentLayout().compress(rcx, useEmptyRows, useEmptyCols, brect, null, null, null, monitor, boundStart, boundEnd);
            boundStart = boundEnd;
          }
          //
          // This is how we deal with the left-hand stack trace errors described above.  We just
          // force the intersection points up as needed.
          //
          rcx.getCurrentLayout().repairStackedCompressionErrors(rcx);
        }
      }
      if (monitor != null) {
        if (!monitor.updateProgress((int)(maxFrac * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
 
      int overlayOption = sle.getOverlayOption();
      
      //
      // Pull in Module shape and pad recovery:
      //
           
      Layout.PadNeedsForLayout padNeedsForLayout = sle.getPadNeedsForLayout();
      Layout.OverlayKeySet loModKeys = (padNeedsForLayout != null) ? padNeedsForLayout.getFullModuleKeys() : null;
      //
      // If there are modules, we may need to recover their shapes.  In the case
      // of file imports, the shape info was gathered up long ago and registered
      // with the SpecialtyLayoutEngine sle directly with setModuleRecoveryData(Map,Map).
      // With solo specialty layouts, that info is generated directly by the sle
      // (making up for a null second arg for setModuleRecoveryData) and is available 
      // to us now to recover module shapes.
      //
      Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> moduleShapeRecovery = sle.getModuleShapeRecovery();
      //
      // With an overlay-based layout, we skip the shifting for THIS overlay, but any _other_ overlays
      // tied to the layout all need to be fixed.
      //
      if ((moduleShapeRecovery != null) && 
          // Empty test is one fix for BT-10-24-12:1
          !moduleShapeRecovery.isEmpty() && 
          (overlayOption != NetOverlayProperties.RELAYOUT_NO_CHANGE)) {
        // Used to handle PropChange and undo support, but we are in an undo transaction.
        Layout.OverlayKeySet reduced = loModKeys.dropOverlay(overlayKey);
        rcx.getCurrentLayout().shiftModuleShapesPerParams(reduced, moduleShapeRecovery, rcx);
      }
      
      //
      // If the layout was generated by an overlay, we want to resize the modules to contain the
      // intra-module links:
      //
      
      if (overlayKey != null) {
        for (int i = 0; i < numSub; i++) {
          SpecialtyInstructions sp = propsList.get(i);
          Rectangle newSize = sp.getStrictGrownBounds();
          if (newSize == null) {
            newSize = sp.getPlacedFullBounds();         
          }
          // Contig rect module cores are getting reset to contain what they need
          rcx.getCurrentLayout().sizeCoreToGivenBounds(overlayKey, sp.moduleID, new Rectangle(newSize));
        }
      }
     
      //
      // If stuff has been shifted around, get the overlay links to match the changes:
      //
      
      if (overlayKey != null) {
        Map<Point2D, Point2D> modLinkCornerMoves = sle.getModLinkCornerMoves();
        rcx.getCurrentLayout().shiftModLinksToNewPositions(overlayKey, modLinkCornerMoves, false, rcx); 
      }
      
      //
      // Time to repair link pads, if necessary.  Note that HERE we actually use the ability to restrict
      // orphans-only to just THIS overlay which is used to set up the layout and which is strictly controlled.
      // In most (all?) other cases, the per-overlay orphans-only are globally set to false.
      //
      
      // Null test is one fix for BT-10-24-12:1
      if (padNeedsForLayout != null) {
        Map<String, Boolean> orpho = rcx.getCurrentLayout().orphansOnlyForAll(false);
        if (overlayKey != null) {
          orpho.put(overlayKey, new Boolean(true));
        }
        rcx.getCurrentLayout().repairAllNetModuleLinkPadRequirements(rcx, padNeedsForLayout, orpho);
      }
      
      //
      // Get labels and supp data where they belong:
      // 
      
      Layout.SupplementalDataCoords sdc = sle.getSupplementalDataCoords();
      Rectangle2D rect = rcx.getCurrentLayout().applySupplementalDataCoords(sdc, rcx, loModKeys);    
      rcx.getCurrentLayout().restoreLabelLocations(rememberProps, rcx, rect);
      Point2D fullGenomeCenter = sle.getFullGenomeCenter();
      if (fullGenomeCenter != null) {
        Vector2D shifted = rcx.getCurrentLayout().recenterLayout(fullGenomeCenter, rcx, true, true, true, 
                                                     null, null, null, loModKeys, padNeedsForLayout);
        shiftedAmount.setXY(shifted.getX(), shifted.getY());
      }
            
      if (sle.doHideNames()) {
        rcx.getCurrentLayout().hideAllMinorNodeNames(rcx);     
      }
      
      //
      // Finish up undo transaction:
      //    

      if (support != null) {
        dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(rcx, dc));
      }
      
      //
      // Turn on bubbles if desired:
      //
      
      if (bubblesOn) {
        MinimalDispOptMgr lbs = rcx.getDisplayOptsSource();
        DisplayOptions dopt = lbs.getDisplayOptions();
        if (dopt.getBranchMode() == DisplayOptions.NO_BUS_BRANCHES) {
          lbs.turnOnSpecialLinkBranches(support, rcx, false);
        }
      }

      return (retval);

    } catch (AsynchExitRequestException ex) {
      rcx.getLayoutSource().rollbackLayoutUndoTransaction(dc);
      throw ex;
    }    
  }   
  
  /***************************************************************************
  **
  ** Do boundary splicing.  This is called in both overlay and selected cases
  */  
 
  private static void spliceBoundaries(List<Map<String, SpecialtyLayoutLinkData>> forRecoveriesOut, 
                                       Map<String, SpecialtyLayoutLinkData> alienSources, 
                                       List<SpecialtyInstructions> propsList, int numSub, 
                                       Map<Integer, LinkBundleSplicer.SpliceSolution> nonOverlaySolutions, 
                                       Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> overlaySplicePlans) {
  
    //
    // Glue recovered geometry from layout in for links that were not handled by subset
    // layouts.  First up, for links going to nodes outside our subsets, we paste the
    // recovered stuff in after the newly generated departure prefixes:
    //
      
    if (!forRecoveriesOut.isEmpty()) {
      for (int i = 0; i < numSub; i++) {
        SpecialtyInstructions sp = propsList.get(i);
        String modID = sp.getModuleID();
        Map<String, SpecialtyLayoutLinkData> forRecovery = forRecoveriesOut.get(i);
        if (!forRecovery.isEmpty()) {
          Iterator<String> sit = sp.getLinkSrcIterator();
          while (sit.hasNext()) {
            String srcID = sit.next();
            SpecialtyLayoutLinkData si = sp.getLinkPointsForSrc(srcID);
            SpecialtyLayoutLinkData recSi = forRecovery.get(srcID);
            if (recSi != null) {
              Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder = (modID == null) ? nonOverlaySolutions : overlaySplicePlans.get(modID);
              si.mergeToStubs(recSi, solnPerBorder);
            }
          }
        }
      }
    }

    //
    // Next, we have links coming from sources outside our subsets.  In this case, we add the
    // recovered departure geometry in as prefixes to the newly generated arrival geometry:
    //

    if (!alienSources.isEmpty()) {
      Iterator<String> asit = alienSources.keySet().iterator();
      while (asit.hasNext()) {
        String srcID = asit.next();
        SpecialtyLayoutLinkData recSi = alienSources.get(srcID);
        for (int i = 0; i < numSub; i++) {
          SpecialtyInstructions sp = propsList.get(i);
          String modID = sp.getModuleID();
          SpecialtyLayoutLinkData si = sp.getLinkPointsForSrc(srcID);
          Map<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder = (modID == null) ? nonOverlaySolutions : overlaySplicePlans.get(modID);
          recSi.mergeToAlienStub(si, solnPerBorder);
        }
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Do layout color operations
  */  
 
  private static void doColorOperations(ColorTypes colorType, boolean checkColors, LinkRouter router, 
                                        Map<String, String> allNodeColors, Map<String, String> allLinkColors, 
                                        StaticDataAccessContext rcx,
                                        SpecialtyLayoutEngine.GlobalSLEState gss,
                                        LinkRouter.RoutingResult retval, 
                                        BTProgressMonitor monitor) throws AsynchExitRequestException {

      
    Genome genome = rcx.getCurrentGenome();
    
    //
    // We do not allow assignment for subsets, just reuse:
    //

    ColorAssigner.ColorIssues issues = null; 
    if (colorType == ColorTypes.COLOR_BY_CYCLE) {
      int numCol = rcx.getColorResolver().getNumColors();
      Iterator<SpecialtyLayoutData> sldit = gss.dataIterator();
      while (sldit.hasNext()) {
        SpecialtyLayoutData sld = sldit.next();
        SortedMap<Integer, String> so = sld.results.getSourceOrder();
        Iterator<String> vit = so.values().iterator();
        int count = 0;
        while (vit.hasNext()) {
          String srcID = vit.next();
          String color = rcx.getColorResolver().getGeneColor(count);
          allNodeColors.put(srcID, color);
          count = (count + 1) % numCol;
        }
      }
      InvertedSrcTrg ist = gss.getInvertedSrcTrg();
      Iterator<Node> nit = genome.getAllNodeIterator();
      int count = 0;
      HashMap<Integer, INodeRenderer> renderCache = new HashMap<Integer, INodeRenderer>();
      while (nit.hasNext()) {
        Node node = nit.next();
        String srcID = node.getID();
        if (allNodeColors.get(srcID) == null) {
          if (ist.outboundLinkCount(srcID) > 0) {
            String color = rcx.getColorResolver().getGeneColor(count);
            allNodeColors.put(srcID, color);
            count = (count + 1) % numCol;
          } else {
            //
            // Code for Issue #244. Set targets to their preferred color, which is usually black
            // except for tablet nodes.
            // 
            int nodeType = node.getNodeType();
            Integer key = Integer.valueOf(nodeType);
            INodeRenderer ir =  renderCache.get(key);
            if (ir == null) {
              ir = NodeProperties.buildRenderer(nodeType);
              renderCache.put(key, ir);
            }
            allNodeColors.put(srcID, ir.getTargetColor());
          }
        }
      }
      
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String src = link.getSource();
        allLinkColors.put(link.getID(), allNodeColors.get(src));
      }
      LinkPlacementGrid routerGrid = null;
      if (checkColors) {
        routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);    
      }     
      ColorAssigner ca = new ColorAssigner();
      issues = ca.assignViaMap(rcx, routerGrid, allNodeColors, allLinkColors);
      
    // Looks for ambiguity and assigns (by grid) if needed:  
    } else if (colorType == ColorTypes.COLOR_BY_GRAPH) {
      HashSet<String> seenSrcs = new HashSet<String>();
      HashSet<String>needColors = new HashSet<String>();    
      Iterator<Linkage> liit = genome.getLinkageIterator();
      while (liit.hasNext()) {
        Linkage link = liit.next();
        String src = link.getSource();
        if (!seenSrcs.contains(src)) {
          seenSrcs.add(src);
          needColors.add(link.getID());
        }
      }
      LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);    
      ColorAssigner ca= new ColorAssigner();
      issues = ca.assignColors(rcx, routerGrid, null, false, false, needColors);
      if (!checkColors) {
        issues = null;
      }
    // Looks for ambiguity and assigns (by grid) if needed:
    } else if (colorType == ColorTypes.COLOR_IF_NEEDED) {
      LinkRouter.RoutingResult result = reassignColors(rcx, null);
      if (checkColors && (result.colorResult != ColorAssigner.COLORING_OK)) {
        issues = new ColorAssigner.ColorIssues(result.collisionSrc1, result.collisionSrc2);
      }
    } else if (colorType == ColorTypes.KEEP_COLORS) {     
      if ((!allNodeColors.isEmpty() || !allLinkColors.isEmpty())) { // Subset case and HALO case: we have a color map!
        // *** Not really "keeping colors" for HALO, actually using the colors >>assigned<< by HALO layout in this situation!
        // Halo options do not include color directive.
        LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);    
        ColorAssigner ca = new ColorAssigner();
        issues = ca.assignViaMap(rcx, routerGrid, allNodeColors, allLinkColors);
        if (!checkColors) {
          issues = null;
        }
      } else { // Don't do anything, but check if asked
        if (checkColors) {
          LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);    
          ColorAssigner ca= new ColorAssigner();
          issues = ca.hasAmbiguousCrossing(rcx, routerGrid);
        }
      }
    }
    if ((issues != null) && (issues.status != ColorAssigner.COLORING_OK)) {
      retval.linkResult |= LinkRouter.COLOR_PROBLEM;
      retval.colorResult = issues.status;
      retval.collisionSrc1 = (issues.collisionSrc1 == null) ? retval.collisionSrc1 : issues.collisionSrc1;
      retval.collisionSrc2 = (issues.collisionSrc2 == null) ? retval.collisionSrc2 : issues.collisionSrc2;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Prepare to add some links
  */
   
  public static void gridlessLinkLayoutPrep(StaticDataAccessContext rcx, 
                                            Set<String> toDo, Map<String, BusProperties.RememberProps> rememberProps) {   
    
    //
    // Cache a map to avoid O(l^2) operation
    //
    
    HashMap<String, String> linkForSrc = new HashMap<String, String>();
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      String src = link.getSource();
      if (linkForSrc.get(src) != null) {
        continue;
      }
      BusProperties testProp = rcx.getCurrentLayout().getLinkProperties(linkID);
      if (testProp != null) {
        linkForSrc.put(src, linkID);
      }
    }    
    
    //
    // Lay links out automatically.
    //
     
    Iterator<String> xrit = toDo.iterator();
    while (xrit.hasNext()) {
      String linkID = xrit.next();
      autoAddCrudeLinkPropertiesFastBatch(rcx, linkID, rememberProps, linkForSrc);
    }   
    return;
  }
   
  /***************************************************************************
  **
  ** Automatically add crude link properties for a link.
  */  
   
  public static boolean autoAddCrudeLinkProperties(StaticDataAccessContext rcx, String linkID, 
                                                   UndoSupport support, Map<String, BusProperties.RememberProps> rememberProps) {
    
    Genome genome = rcx.getCurrentGenome();
    Layout lo = rcx.getCurrentLayout();
    
    //
    // Look for a preexisting link from the source:
    //
    BusProperties lp = null;
    Iterator<Linkage> lit = genome.getLinkageIterator();
    Linkage thisLink = genome.getLinkage(linkID);
    String srcID = thisLink.getSource();
    String trgID = thisLink.getTarget();

    String existingID = null;
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      if (src.equals(srcID)) {
        BusProperties testProp = lo.getLinkProperties(link.getID());
        if (testProp != null) {
          lp = testProp;
          existingID = link.getID();
          // FIX ME?? shouldn't this have a "break"?
        }  
      }
    }    
   
    //
    // Create a new LinkProperties
    //

    BusProperties.RememberProps rp = (rememberProps == null) ? null : rememberProps.get(linkID);
    
    float tx = 0.0F;
    float ty = 0.0F;
    // This is only a very approximate solution, but better than "0, 0":
    if (lp == null) {
      HashSet<Point2D> points = new HashSet<Point2D>();
      points.add(lo.getNodeProperties(srcID).getLocation());
      points.add(lo.getNodeProperties(trgID).getLocation());
      Point2D labelLoc = AffineCombination.combination(points, UiUtil.GRID_SIZE);
      tx = (labelLoc == null) ? 0.0F : (float)labelLoc.getX();
      ty = (labelLoc == null) ? 0.0F : (float)labelLoc.getY();
    }
      
    BusProperties nlp = new BusProperties(genome, linkID, tx, ty, "black");
    if (rp != null) {
      rp.restore(nlp, rcx);
    }
    Layout.PropChange lpc = null;
    if (lp == null) {
      lpc = lo.setLinkPropertiesForUndo(linkID, nlp, null);
    } else {
      // null segID means it merges at the root segment
      lpc = lo.mergeNewLinkToTreeAtSegment(rcx, existingID, nlp, null);
    }
    if (support != null) {
      support.addEdit(new PropChangeCmd(rcx, new Layout.PropChange[] {lpc}));
    }
    
    return (true); 
  }
   
  /***************************************************************************
  **
  ** Automatically add crude link properties for a link.
  */  
   
  public static void autoAddCrudeLinkPropertiesFastBatch(StaticDataAccessContext rcx, String linkID, 
                                                         Map<String, BusProperties.RememberProps> rememberProps, 
                                                         Map<String, String> cacheMap) {
   
    Genome genome = rcx.getCurrentGenome();
    Layout lo = rcx.getCurrentLayout();
    
    Linkage thisLink = genome.getLinkage(linkID);
    String srcID = thisLink.getSource();
    String trgID = thisLink.getTarget();
    String existingLink = cacheMap.get(srcID);
    BusProperties lp = lo.getLinkProperties(existingLink);
    
    //
    // Create a new LinkProperties
    //

    BusProperties.RememberProps rp = (rememberProps == null) ? null : rememberProps.get(linkID);
    
    float tx = 0.0F;
    float ty = 0.0F;
    // This is only a very approximate solution, but better than "0, 0":
    if (lp == null) {
      HashSet<Point2D> points = new HashSet<Point2D>();
      points.add(lo.getNodeProperties(srcID).getLocation());
      points.add(lo.getNodeProperties(trgID).getLocation());
      Point2D labelLoc = AffineCombination.combination(points, UiUtil.GRID_SIZE);
      tx = (labelLoc == null) ? 0.0F : (float)labelLoc.getX();
      ty = (labelLoc == null) ? 0.0F : (float)labelLoc.getY();
    }
      
    BusProperties nlp = new BusProperties(genome, linkID, tx, ty, "black");
    if (rp != null) {
      rp.restore(nlp, rcx);
    }
    if (lp == null) {
      lo.setLinkPropertiesForUndo(linkID, nlp, null);
      cacheMap.put(srcID, linkID);
    } else {
      lo.mergeNewLinkToTreeAtRootBatch(rcx, existingLink, nlp, linkID);
    }
    return; 
  }
 
  /***************************************************************************
  **
  ** Get wiggle pad assignments based on node locations:
  */  
   
  private static void wigglePads(StaticDataAccessContext rcx, Map<String, PadConstraints> padConstraints, UndoSupport support) {
    //
    // Wiggle pad assignments based on node locations:
    //
    Iterator<Node> nit = rcx.getCurrentGenome().getNodeIterator();
    wigglePadCore(nit, rcx, padConstraints, support);
    return;
  }
    
  /***************************************************************************
  **
  ** Core of wiggle pads
  */  
 
  public static void wigglePadCore(Iterator<Node> nit, StaticDataAccessContext rcx, 
                                   Map<String, PadConstraints> padConstraints, UndoSupport support) {
    //
    // Wiggle pad assignments based on node locations:
    //
    
    Genome genome = rcx.getCurrentGenome();
    PadCalculatorToo padCalc = new PadCalculatorToo();
    while (nit.hasNext()) {
      Node node = nit.next();
      Map<String, PadCalculatorToo.PadResult> swaps = padCalc.recommendLinkSwaps(rcx, node.getID(), padConstraints);
      Iterator<String> skit = swaps.keySet().iterator();
      while (skit.hasNext()) {
        String linkID = skit.next();
        Linkage link = genome.getLinkage(linkID);
        PadCalculatorToo.PadResult pres = swaps.get(linkID);
        if (pres.launch != link.getLaunchPad()) {
          GenomeChange gc = genome.changeLinkageSource(link, pres.launch);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
            support.addEdit(gcc);
          }
        }
        link = genome.getLinkage(linkID);  // may have changed in above operation: handle is stale        
        if (pres.landing != link.getLandingPad()) {      
          GenomeChange gc = genome.changeLinkageTarget(link, pres.landing);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
            support.addEdit(gcc);
          }
        }
      }
    }
  }
  
  /***************************************************************************
  **
  ** Get average node spacing:
  */  
  
  private static Vector2D avgDistance(StaticDataAccessContext rcx) {
    ArrayList<Point2D> points = new ArrayList<Point2D>();
    Iterator<Node> nit = rcx.getCurrentGenome().getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      NodeProperties np = rcx.getCurrentLayout().getNodeProperties(node.getID());
      if (np != null) {  // not all nodes placed yet
        points.add(np.getLocation());
      }
    }
    return (DistanceMeasurer.calcDistances(points));
  }
    
    
  /***************************************************************************
  **
  ** Arrange new node grid spacing for new incremental nodes
  */  
    
  private static double calcIncrementalGridSpacing(StaticDataAccessContext rcxR, Set<String> newNodes,
                                                   Grid grid, double defaultLeft, 
                                                   LayoutOptions options) {                                      
    //
    // Figure out average existing column spacing:
    //
                                               
    Vector2D avg = avgDistance(rcxR);     
                                                
    double avgW = avg.getX();
    double colWidth = (avgW == 0.0) ? grid.getColSpace() : (Math.round(avgW / 10.0) * 10.0);
    double avgH = avg.getY();
    double rowHeight = (avgH == 0.0) ? grid.getRowSpace() : (Math.round(avgH / 10.0) * 10.0);
    grid.setRowSpace(rowHeight);
     
    TreeMap<Integer, Double> colPos = new TreeMap<Integer, Double>();
    TreeSet<Integer> empties = new TreeSet<Integer>();
    calcColumnPositions(rcxR, newNodes, grid, colWidth, defaultLeft, colPos, empties, options);

    //
    // Calculate column spacings:
    //
     
    double retval = 0.0;
    double lastval = 0.0;
    int lastCol = 0;
    boolean isFirst = true;
    boolean isSecond = false;
    double tweak = colWidth / 2.0;
    double delta = colWidth;
    Iterator<Integer> ckit = colPos.keySet().iterator();
    while (ckit.hasNext()) {
      Integer colObj = ckit.next();
      int col = colObj.intValue();
      double pos = colPos.get(colObj).doubleValue();
      if (isFirst) {
        retval = pos;
        isFirst = false;
        isSecond = true;
      } else if (isSecond) {
        tweak = (pos - lastval) / 2.0;
        isSecond = false;
        delta = pos - lastval;
        grid.overrideColSpace(lastCol, delta);
      } else {
        delta = pos - lastval;
        grid.overrideColSpace(lastCol, delta);
      }
      lastval = pos;
      lastCol = col;
    }
     
    grid.overrideColSpace(lastCol, delta);
     
    Iterator<Integer> eit = empties.iterator();
    while (eit.hasNext()) {
      Integer colObj = eit.next();
      int col = colObj.intValue();
      grid.overrideColSpace(col, 0.0);
    }
    return (retval - tweak);
  }
  /***************************************************************************
  **
  ** Arrange new node grid spacing for new incremental nodes
  */  
   
  private static void calcColumnPositions(StaticDataAccessContext rcxR, Set<String> newNodes,
                                          Grid grid, double colW, 
                                          double defaultLeft, SortedMap<Integer, Double> colLocs, 
                                          SortedSet<Integer> empties, LayoutOptions options) {

    //
    // For each column, if it contains targets of existing nodes, get the weighted avg.
    // of the x coord of those sources.  Same for targets.  If we end up with both,
    // use the source-based position (i.e. leftmost) unless they are reversed; then
    // take the weighted average as the column location suggestion.
    //
    
    TreeMap<Integer, WeightedAverage> absolutes = new TreeMap<Integer, WeightedAverage>();
    int numCols = grid.getNumCols();
    for (int i = 0; i < numCols; i++) {
      if (grid.columnIsEmpty(i)) {
        empties.add(new Integer(i));
        continue;
      }
      WeightedAverage was = findSourceWeightedAvg(rcxR, newNodes, grid, i);
      WeightedAverage wat = findTargetWeightedAvg(rcxR, newNodes, grid, i);
      double posForCol;
      int countForCol;
      if ((was.count == 0) && (wat.count == 0)) {
        continue;
      } else if ((was.count != 0) && (wat.count != 0)) {
        double posForSrc = was.average + colW;
        double posForTrg = wat.average - colW;
        posForCol = posForSrc;
        countForCol = was.count + wat.count;        
        if (posForSrc > posForTrg) {
          posForCol = ((posForSrc * was.count) + (posForTrg * wat.count)) / countForCol;
        }
      } else if (was.count != 0) {
        posForCol = was.average + colW;
        countForCol = was.count;
      } else if (wat.count != 0) {
        posForCol = wat.average - colW;
        countForCol = wat.count;
      } else {
        throw new IllegalStateException();
      }
      
      absolutes.put(new Integer(i), new WeightedAverage(posForCol, countForCol));
    }
    
    //
    // Figure out each column location.  Take weighted average of suggestions from remaining
    // columns to the right.  If this is more left than current minimum, just butt it up
    // against the previous column.
    //

    Double currPosObj = new Double(0.0);
    double lastColPos = 0.0;
    for (int i = 0; i < numCols; i++) {
      Integer iObj = new Integer(i);
      if (!empties.contains(iObj)) {
        int numFilled = 0;
        double currPos = 0.0;
        int currCount = 0;
        double colWidth = 0.0;
        for (int j = i; j < numCols; j++) {
          Integer jObj = new Integer(j);
          WeightedAverage colPos = absolutes.get(jObj);
          if (colPos == null) {
            if (!options.incrementalCompress || (!empties.contains(jObj))) {
              numFilled++;
            }
            continue;
          }
          currPos += ((colPos.average - (colW * numFilled)) * colPos.count);
          currCount += colPos.count;
          numFilled++;
        }
        colWidth = colW;
        double minPos;
        if (!colLocs.isEmpty()) {
          minPos = lastColPos + colWidth;
        } else if (currCount > 0) {
          minPos = Double.NEGATIVE_INFINITY;
        } else {
          minPos = defaultLeft;
        }
        
        if (currCount == 0) {
          currPos = minPos;
          currPos = varianceReductionTweak(rcxR, currPos, colW * 0.4, true);   
        } else {
          currPos /= currCount;
          if (currPos < minPos) {
            currPos = minPos;
            currPos = varianceReductionTweak(rcxR, currPos, colW * 0.4, true);      
          } else {
            currPos = varianceReductionTweak(rcxR, currPos, colW * 0.4, false);
          }
        }
        currPos = Math.round(currPos / 10.0) * 10.0;
        lastColPos = currPos;
        currPosObj = new Double(currPos);
        colLocs.put(iObj, currPosObj);
      }   
    }
    return;
  }   

  /***************************************************************************
  **
  ** For a given column in the grid, determine the average X coord for the 
  ** pre-existing sources.
  */  
    
   private static WeightedAverage findSourceWeightedAvg(StaticDataAccessContext rcx, Set<String> newNodes, Grid grid, int col) {

     WeightedAverage wa = new WeightedAverage();                                   
     wa.average = 0.0;
     wa.count = 0;
     Map<String, Integer> topoSort = grid.getTopoSort();
     int numRows = grid.getNumRows();
     for (int i = 0; i < numRows; i++) {
       String nodeID = grid.getCellValue(i, col);
       if (nodeID == null) {
         continue;
       }
       int topoColTarg = topoSort.get(nodeID).intValue();
       Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
       while (lit.hasNext()) {
         Linkage link = lit.next();
         String trg = link.getTarget();
         if (!trg.equals(nodeID)) {
           continue;
         }
         String src = link.getSource();
         if (newNodes.contains(src)) {
           continue;
         }
         int topoColSrc = topoSort.get(src).intValue();
         if (topoColSrc >= topoColTarg) {
           continue;
         }
         NodeProperties np = rcx.getCurrentLayout().getNodeProperties(src);
         double xOffset = np.getRenderer().getVerticalOffset();
         double val = np.getLocation().getX() - xOffset;
         wa.average += val;
         wa.count++;
       }
     }
     if (wa.count != 0) {
       wa.average /= wa.count;
     }
     return (wa);
   }
   
   /***************************************************************************
   **
   ** For a given column in the grid, determine the average X coord for the 
   ** pre-existing targets.
   */  

   private static WeightedAverage findTargetWeightedAvg(StaticDataAccessContext rcx, Set<String> newNodes, Grid grid, int col) {
                                        
     WeightedAverage wa = new WeightedAverage();
     wa.average = 0.0;
     wa.count = 0;
     Map<String, Integer> topoSort = grid.getTopoSort();
     int numRows = grid.getNumRows();
     for (int i = 0; i < numRows; i++) {
       String nodeID = grid.getCellValue(i, col);
       if (nodeID == null) {
         continue;
       }
       int topoColSrc = topoSort.get(nodeID).intValue();
       Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
       while (lit.hasNext()) {
         Linkage link = lit.next();
         String src = link.getSource();
         if (!src.equals(nodeID)) {
           continue;
         }
         String trg = link.getTarget();
         if (newNodes.contains(trg)) {
           continue;
         }
         int topoColTarg = topoSort.get(trg).intValue();
         if (topoColSrc >= topoColTarg) {
           continue;
         }
         NodeProperties np = rcx.getCurrentLayout().getNodeProperties(trg);
         double xOffset = np.getRenderer().getVerticalOffset();
         double val = np.getLocation().getX() - xOffset;
         wa.average += val;
         wa.count++;
       }
     }
     if (wa.count != 0) {
       wa.average /= wa.count;
     }
     return (wa);
   }
      
   /***************************************************************************
   **
   ** Tweak column to reduce variance
   */  
    
   private static double varianceReductionTweak(StaticDataAccessContext rcx, double xCenter, double width, boolean plusOnly) {                                      
     //
     // Find all the existing x coords within +- half a column width.  Then take
     // the coord that minimizes the variance.
     //

     double[] xCoords = collectXCoords(rcx, xCenter, width);
     int numCoords = xCoords.length;
     double[] xCoordsPlus = new double[numCoords + 1];
     System.arraycopy(xCoords, 0, xCoordsPlus, 0, numCoords);
     
     double minVariance = Double.POSITIVE_INFINITY;
     int minCoord = Integer.MIN_VALUE;
     int xMin;
     if (!plusOnly) {
       xMin = (int)Math.round(((xCenter - width) / 10.0) * 10.0);
     } else {
       xMin = (int)Math.round((xCenter / 10.0) * 10.0);
     }
     int xMax = (int)Math.ceil(Math.round(((xCenter + width) / 10.0)) * 10.0);
     
     if (numCoords != 0) {
       for (int i = xMin; i <= xMax; i += 10) {
         xCoordsPlus[numCoords] = i;
         double varSq = varianceSq(xCoordsPlus);
         if (varSq < minVariance) {
           minVariance = varSq;
           minCoord = i;
         }
       }
     }
     
     if (minVariance != Double.POSITIVE_INFINITY) {
       return (minCoord);
     }
     return (xCenter);
   }
     
   /***************************************************************************
   **
   ** Calculate square of variance
   */  
  
   private static double varianceSq(double[] values) {
     
     int size = values.length;
     double sum = 0.0;
     double sumsq = 0.0;
     for (int i = 0; i < size; i++) {
       sum += values[i];
       sumsq += (values[i] * values[i]);
     }
     double n = size;
     double retval = (1.0 / (n - 1)) * (sumsq - ((sum * sum) / n));
     return (retval);
   }      
  
   /***************************************************************************
   **
   ** If the new grid is completely to the right of the existing network, determine
   ** the best vertical position by minimizing link displacements.
   */  
  
   private static Point2D findBestVerticalPosition(StaticDataAccessContext rcx, Grid grid,
                                                   Map<String, Integer> newNodeTypes, double leftie) {

     //
     // Stash away position information:
     //
                                                      
     int rows = grid.getNumRows();
     int cols = grid.getNumCols();
     HashMap<String, Point2D> gridPos = new HashMap<String, Point2D>();
     Point2D dummyCorner = new Point2D.Double(0.0, 0.0);
     for (int i = 0; i < rows; i++) {
       for (int j = 0; j < cols; j++) {      
         String geneID = grid.getCellValue(i, j);
         if (geneID == null) {
           continue;
         }          
         Point2D loc = grid.getGridLocation(i, j, dummyCorner);
         gridPos.put(geneID, loc);
       }
     }
     Set<String> gridNodes = gridPos.keySet();
     
     //
     // Calculate the deltas and the number:
     //
     
     int count = 0;
     double deltaSum = 0.0;
     Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
     while (lit.hasNext()) {
       Linkage link = lit.next();
       String trg = link.getTarget();
       String src = link.getSource();
       boolean gridHasTarg = gridNodes.contains(trg);
       if (gridHasTarg == gridNodes.contains(src)) {
         continue;
       }
       String oldNode;
       String newNode;
       if (gridHasTarg) {
         newNode = trg;
         oldNode = src;
       } else {
         newNode = src;
         oldNode = trg;
       }
       NodeProperties np = rcx.getCurrentLayout().getNodeProperties(oldNode);
       Point2D oldPos = np.getLocation();
       Point2D newPos = gridPos.get(newNode);
       int nodeType = newNodeTypes.get(newNode).intValue();          
       np = new NodeProperties(rcx.getColorResolver(), nodeType, newNode, 0.0, 0.0, false);
       double yOffset = np.getRenderer().getStraightThroughOffset();
       double yDiff = oldPos.getY() - (newPos.getY() + yOffset);
       count++;
       deltaSum += yDiff;
     }
        
     return (new Point2D.Double(leftie, deltaSum / count));
   }
  
   /***************************************************************************
   **
   ** Collect all the x coords near the given coord
   */  
  
   private static double[] collectXCoords(StaticDataAccessContext rcx, double xCenter, double width) {
     ArrayList<Point2D> points = new ArrayList<Point2D>();
     Iterator<Node> nit = rcx.getCurrentGenome().getNodeIterator();
     while (nit.hasNext()) {
       Node node = nit.next();
       NodeProperties np = rcx.getCurrentLayout().getNodeProperties(node.getID());
       if (np != null) {  // not all nodes placed yet
         Point2D loc = np.getLocation();
         double xLoc = loc.getX() - np.getRenderer().getVerticalOffset();
         if (Math.abs(xCenter - xLoc) <= width) {          
           points.add(new Point2D.Double(xLoc, loc.getY()));
         }
       }
     }
     Iterator<Gene> git = rcx.getCurrentGenome().getGeneIterator();
     while (git.hasNext()) {
       Node node = git.next();
       NodeProperties np = rcx.getCurrentLayout().getNodeProperties(node.getID());
       if (np != null) {  // not all nodes placed yet
         Point2D loc = np.getLocation();
         double xLoc = loc.getX() - np.getRenderer().getVerticalOffset();
         if (Math.abs(xCenter - xLoc) <= width) {          
           points.add(new Point2D.Double(xLoc, loc.getY()));
         }
       }
     }
     int size = points.size();
     double[] retval = new double[size];
     for (int i = 0; i < size; i++) {
       retval[i] = points.get(i).getX();
     }
     return (retval);
   }
 
   /***************************************************************************
   **
   ** Used to locate columns in incremental placement 
   */
   
   private static class WeightedAverage {    
     int count;
     double average;
     
     WeightedAverage() {
     }
     
     WeightedAverage(double avg, int count) {
       this.count = count;
       this.average = avg;
     }    
   }  
       
   /***************************************************************************
   **
   ** Represents a single request to relayout links
   */
    
   public static class GlobalLinkRequest {   
     public Genome genome;
     public Layout layout;
     public GenomeSource gSrc;
     public Set<String> badLinks;
   } 
}
