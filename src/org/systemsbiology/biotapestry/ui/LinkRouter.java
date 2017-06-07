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

package org.systemsbiology.biotapestry.ui;

import java.util.SortedSet;
import java.util.SortedMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutRubberStamper;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.freerender.PlacementGridSupport;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.ui.layouts.LayoutFailureTracker;
import org.systemsbiology.biotapestry.ui.layouts.LinkBundleSplicer;
import org.systemsbiology.biotapestry.ui.layouts.NetModuleLinkExtractor;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyInstructions;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutLinkData;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** Used to automatically lay out links
*/

public class LinkRouter {
  
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
  
  public static final int LAYOUT_OK      = 0x00;    
  public static final int COLOR_PROBLEM  = 0x01;
  public static final int LAYOUT_PROBLEM = 0x02;
  public static final int LAYOUT_COULD_NOT_PROCEED = 0x04;
  public static final int LAYOUT_OVERLAID_SUBSET   = 0x08;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LinkRouter() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Initialize the layout grid for modules
  */
  
  public LinkPlacementGrid initGridForModules(DataAccessContext rcx, 
                                              Set<String> skipLinks, String overID,
                                              BTProgressMonitor monitor) throws AsynchExitRequestException {
    return (rcx.getCurrentLayout().moduleFillLinkPlacementGrid(rcx, skipLinks, overID, monitor));
  }   
     
  /***************************************************************************
  **
  ** Initialize the placement grid for module link checks.  Note this works off of
  ** geometry completely detached from existing layout or genome.  The LinkProperties
  ** are just holding geometry....
  */
  
  public LinkPlacementGrid initGridForModuleGuidedLinks(Map<String, Rectangle2D> moduleShapes, 
                                                        Map<String, Map<Point2D, Map<String, Point>>> multiLinkData, 
                                                        Map<String, NetModuleLinkageProperties> moduleLinkTrees, 
                                                        StaticDataAccessContext rcx) {
    LinkPlacementGrid retval = new LinkPlacementGrid();
    Iterator<String> sit = moduleShapes.keySet().iterator();
    while (sit.hasNext()) {
      String moduleID = sit.next();      
      Rectangle2D rect = moduleShapes.get(moduleID);
      retval.addGroup(UiUtil.rectFromRect2D(rect), moduleID);
    } 
   
    PlacementGridSupport pgs = new PlacementGridSupport();
    Iterator<String> mit = moduleLinkTrees.keySet().iterator();
    while (mit.hasNext()) {
      String treeID = mit.next();      
      NetModuleLinkageProperties lp = moduleLinkTrees.get(treeID); 
      Map<Point2D, Map<String, Point>> multiForTree = multiLinkData.get(treeID);
      if (!multiForTree.isEmpty()) {  // No links?  Then nothing to do!
        pgs.multiRenderToPlacementGrid(lp, rcx, retval, multiForTree);
      }
    } 
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Initialize the layout grid.
  */
  
  public LinkPlacementGrid initGrid(DataAccessContext irx,
                                    Set<String> skipLinks, int strictness,
                                    BTProgressMonitor monitor) throws AsynchExitRequestException {

    return (irx.getCurrentLayout().fillLinkPlacementGrid(irx, skipLinks, strictness, monitor));
  }
  
  /***************************************************************************
  **
  ** Initialize the layout grid.  Call before new links have temporary
  ** LinkProperties assigned!
  */
  
  public LinkPlacementGrid initLimitedGrid(StaticDataAccessContext irx,
                                           Set<String> useNodes, Set<String> skipLinks, int strictness) {

    try {
      return (irx.getCurrentLayout().limitedFillLinkPlacementGrid(irx, useNodes, skipLinks, strictness, null));
    } catch (AsynchExitRequestException ex) {
      throw new IllegalStateException();  // Cannot happen since monitor is null
    }
  }

  
 /***************************************************************************
  **
  ** Layout the given links
  */
  
  public RoutingResult multiPassLayout(Set<String> newLinks, StaticDataAccessContext rcx, 
                                       LayoutOptions options, 
                                       BTProgressMonitor monitor, Set<String> needColors, 
                                       double startFrac, double endFrac, 
                                       Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                       boolean strictOKGroups) 
                                       throws AsynchExitRequestException {  

    double fullFrac = endFrac - startFrac;
    double passFrac = fullFrac / (2.0 * 4.0);
    double currFrac = startFrac;

    //
    // Multipass.  Note that when laying out a set of links, we reserve departure and arrival zones
    // to make sure that all the links have a chance get to their endpoints.  If we are having layout
    // problems, clearing the grid and working with the smaller set can improve the path options.
    // If that is not helping, then going to a LAX grid can help, since we reduce the padding around
    // nodes.
    //

    HashSet<String> linkSet = new HashSet<String>(newLinks);
    RoutingResult nonLinkRetval = new RoutingResult();
    RoutingResult linkRetval = new RoutingResult();
    for (int i = 0; ((i < 2) && !linkSet.isEmpty()); i++) {
      int strictness = (i == 0) ? INodeRenderer.STRICT : INodeRenderer.LAX;
      int lastFailCount = -1;
      for (int j = 0; j < 4; j++) { // limit on how many passes to make
        int numTrying = linkSet.size();
        LinkPlacementGrid routerGrid = initGrid(rcx, linkSet, strictness, monitor);
        double nextFrac = currFrac + passFrac;
        LayoutFailureTracker.recordLayoutPass(i, j);
        RoutingResult holdResult = layout(linkSet, routerGrid, rcx,
                                          options.goodness, monitor, needColors, 
                                          currFrac, nextFrac, recoveryDataMap, strictOKGroups);
        LayoutFailureTracker.clearLayoutPass();
        rcx.getCurrentLayout().dropUselessCorners(rcx, null, nextFrac, nextFrac, monitor);
        currFrac = nextFrac;
        int numFailed = holdResult.failedLinks.size();
        linkSet.clear();
        linkSet.addAll(holdResult.failedLinks);
        nonLinkRetval.merge(holdResult);
        if ((numFailed == 0) || (numFailed == lastFailCount) || (numFailed == numTrying)) {
          break;
        }
        lastFailCount = numFailed;
      }
    }

    if (!linkSet.isEmpty()) {
      linkRetval.linkResult |= LAYOUT_PROBLEM;
      linkRetval.failedLinks.addAll(linkSet);
    }

    nonLinkRetval.linkResult = ((nonLinkRetval.linkResult & COLOR_PROBLEM) != 0x00) ? COLOR_PROBLEM : LAYOUT_OK;
    nonLinkRetval.failedLinks.clear();
    linkRetval.merge(nonLinkRetval);

    if (monitor != null) {
      if (!monitor.updateProgress((int)(endFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }  

    return (linkRetval);   
  }
  
  /***************************************************************************
  **
  ** Generate preexisting departures
  */
  
  private void generatePreExistingDeparts(LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                          Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                          HashMap<String, LinkPlacementGrid.TerminalRegion> departurePerLink, 
                                          HashMap<LinkPlacementGrid.TerminalRegion, Set<String>> linksPerDeparture) {
    
    Genome genome = rcx.getCurrentGenome();
    Iterator<Node> sit = genome.getAllNodeIterator();
    while (sit.hasNext()) {
      Node node = sit.next();
      String srcID = node.getID();
      Map<String, LayoutRubberStamper.EdgeMove> depChops = null;
      if (recoveryDataMap != null) {     
        LinkPlacementGrid.RecoveryDataForSource rdfs = recoveryDataMap.get(srcID);      
        depChops = (rdfs == null) ? null : rdfs.getDepartureRegionChops();
      }
      Set<String> allDrawnLinks = grid.allLinksDrawnForSource(srcID, genome, null);
      Iterator<String> adlit = allDrawnLinks.iterator();
      while (adlit.hasNext()) {
        String linkID = adlit.next();
        LinkPlacementGrid.TerminalRegion depart;
        LayoutRubberStamper.EdgeMove em = (depChops == null) ? null : depChops.get(linkID);
        if (em == null) {       
          depart = generateDeparture(linkID, rcx, grid, null);
        } else {
          depart = generateRegionBoundary(linkID, genome, em, grid, null, false); 
        }
        Set<String> linksForDep = linksPerDeparture.get(depart);
        if (linksForDep == null) {
          linksForDep = new HashSet<String>();
          linksPerDeparture.put(depart, linksForDep);
        }
        linksForDep.add(linkID);
        departurePerLink.put(linkID, depart); // No longer means it is certain to get added to the grid.
      }
    }
    return; 
  }
   
  /***************************************************************************
  **
  ** Generate departures and arrivals based on node pads
  */
  
  private void generateDepartsAndArrives(LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                         SortedSet<DistanceRank> forSource,
                                         Map<String, LayoutRubberStamper.EdgeMove> arrChops, 
                                         Map<String, LayoutRubberStamper.EdgeMove> depChops,
                                         HashMap<String, LinkPlacementGrid.TerminalRegion> departurePerLink, 
                                         HashMap<LinkPlacementGrid.TerminalRegion, Set<String>> linksPerDeparture, 
                                         HashMap<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                         HashMap<String, LinkPlacementGrid.TerminalRegion> regArrivals, 
                                         HashSet<String> unseenLinks, 
                                         HashSet<String> needAltTargs,
                                         HashMap<String, Set<String>> okGroupMap, 
                                         BTProgressMonitor monitor) 
                                           throws AsynchExitRequestException {
      
    
    Genome genome = rcx.getCurrentGenome();
    Iterator<DistanceRank> fsit = forSource.iterator();
    while (fsit.hasNext()) {
      DistanceRank dr = fsit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }

      //
      // Even if we have a link already coming from the source, we
      // need group overlap info, and generate the departure.  
      // But that does not mean we will actually draw it...that depends on
      // whether we need to.
      // 

      String linkID = dr.id;
      HashSet<String> okGroups = null;
      if (genome instanceof GenomeInstance) {
        okGroups = new HashSet<String>();
        okGroupMap.put(linkID, okGroups);
      }
      
      //
      // Note how departures are being generated even when they do not need placement, so that
      // we build the okGroups for each link:
      //
      
      LinkPlacementGrid.TerminalRegion depart;
      LayoutRubberStamper.EdgeMove em = (depChops == null) ? null : depChops.get(linkID);
      if (em == null) {       
        depart = generateDeparture(linkID, rcx, grid, okGroups);
      } else {
        depart = generateRegionBoundary(linkID, genome, em, grid, okGroups, false);
      }
      //
      // 4/23/12 With the advent of possibly generating multiple departures for one source,
      // at region boundaries, we now key by linkID, not source ID:
      //
                
      Set<String> linksForDep = linksPerDeparture.get(depart);
      if (linksForDep == null) {
        linksForDep = new HashSet<String>();
        linksPerDeparture.put(depart, linksForDep);
      }
      linksForDep.add(linkID);
      departurePerLink.put(linkID, depart); // No longer means it is certain to get added to the grid....     
      
      //
      // When we have inbound pad collisions, we do this later:
      //
      
      if (!needAltTargs.contains(linkID)) {
        LinkPlacementGrid.TerminalRegion arrive;
        LayoutRubberStamper.EdgeMove ema = (arrChops == null) ? null : arrChops.get(linkID);
        if (ema == null) {       
          arrive = generateArrival(linkID, rcx, grid, null, okGroups);
          arrivals.put(linkID, arrive);
        } else {
          arrive = generateRegionBoundary(linkID, genome, ema, grid, okGroups, true);
          regArrivals.put(linkID, arrive);
        }       
      }
      unseenLinks.add(linkID);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Reserve terminal regions on the grid
  */
  
  private Set<LinkPlacementGrid.TerminalRegion> reserveTerminalRegions(Set<String> newLinks,
                                                                       LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                                                       HashMap<String, LinkPlacementGrid.TerminalRegion> departures, 
                                                                       HashMap<LinkPlacementGrid.TerminalRegion, Set<String>> linksPerDeparture, 
                                                                       HashMap<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                                                       HashMap<String, LinkPlacementGrid.TerminalRegion> regArrivals, 
                                                                       Set<String> needAltTargs, Map<String, Set<String>> okGroupMap,
                                                                       BTProgressMonitor monitor) 
                                                                         throws AsynchExitRequestException {
   
    
    Genome genome = rcx.getCurrentGenome();   
    HashSet<LinkPlacementGrid.TerminalRegion> reserved = new HashSet<LinkPlacementGrid.TerminalRegion>();
    Iterator<String> dkit = departures.keySet().iterator();
    while (dkit.hasNext()) {
      String linkID = dkit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      LinkPlacementGrid.TerminalRegion depart = departures.get(linkID);
      if (!reserved.contains(depart)) {
        String srcID = genome.getLinkage(linkID).getSource();
        Set<String> drawnLinksForSource = grid.allLinksDrawnForSource(srcID, genome, null);
        Set<String> linksForThisDeparture = linksPerDeparture.get(depart);
        HashSet<String> alreadyHandled = new HashSet<String>();
        DataUtil.intersection(drawnLinksForSource, linksForThisDeparture, alreadyHandled);
        if (alreadyHandled.isEmpty()) {
          grid.reserveTerminalRegion(depart, srcID, false);
          reserved.add(depart);
        }
      }
    }
       
    Iterator<String> akit = arrivals.keySet().iterator();
    while (akit.hasNext()) {
      String linkID = akit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      LinkPlacementGrid.TerminalRegion arrive = arrivals.get(linkID);
      grid.reserveTerminalRegion(arrive, linkID, false);
    }  
    
    Iterator<String> rakit = regArrivals.keySet().iterator();
    while (rakit.hasNext()) {
      String linkID = rakit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      LinkPlacementGrid.TerminalRegion arrive = regArrivals.get(linkID);
      String srcID = genome.getLinkage(linkID).getSource();
      grid.reserveTerminalRegion(arrive, srcID, true);
    } 

    Map<String, LinkPlacementGrid.TerminalRegion> gtr = generateTerminalRegionsForCollisions(rcx, grid, newLinks, needAltTargs, okGroupMap);
    
    Iterator<String> gkit = gtr.keySet().iterator();
    while (gkit.hasNext()) {
      String linkID = gkit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      LinkPlacementGrid.TerminalRegion arrive = gtr.get(linkID);
      arrivals.put(linkID, arrive);
      grid.reserveTerminalRegion(arrive, linkID, false);
    }
    return (reserved);
  }
  
  /***************************************************************************
  **
  ** Layout the given links
  */
  
  private RoutingResult layout(Set<String> newLinks,
                               LinkPlacementGrid grid, StaticDataAccessContext rcx, 
                               LinkPlacementGrid.GoodnessParams goodness,
                               BTProgressMonitor monitor, Set<String> needColors, 
                               double startFrac, double endFrac, 
                               Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                               boolean strictOKGroups) 
                               throws AsynchExitRequestException {
    
    Genome genome = rcx.getCurrentGenome();
    Set<String> targCollide = genome.hasLinkTargetPadCollisions();
    HashSet<String> needAltTargs = new HashSet<String>(targCollide);
    needAltTargs.retainAll(newLinks);
    //
    // Come up with the order in which links will be laid out:
    //
    
    SortedMap<DistanceRank, SortedSet<DistanceRank>> links = organizeLinks(newLinks, genome, rcx.getCurrentLayout(), recoveryDataMap);
    
    HashMap<String, LinkPlacementGrid.TerminalRegion> departurePerLink = new HashMap<String, LinkPlacementGrid.TerminalRegion>();    
    HashMap<LinkPlacementGrid.TerminalRegion, Set<String>> linksPerDeparture = new HashMap<LinkPlacementGrid.TerminalRegion, Set<String>>();
    HashMap<String, LinkPlacementGrid.TerminalRegion> arrivals = new HashMap<String, LinkPlacementGrid.TerminalRegion>();
    HashMap<String, LinkPlacementGrid.TerminalRegion> regArrivals = new HashMap<String, LinkPlacementGrid.TerminalRegion>();
    HashSet<String> unseenLinks = new HashSet<String>();
    if (monitor != null) {
      if (!monitor.updateProgress((int)(startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
    
    //
    // Start by figuring out the departures that have already been placed:
    //
    
    generatePreExistingDeparts(grid, rcx, recoveryDataMap, departurePerLink, linksPerDeparture);
    
    //
    // Gather up departure and arrival regions
    //
        
    HashMap<String, Set<String>> okGroupMap = new HashMap<String, Set<String>>();
    Iterator<DistanceRank> lkit = links.keySet().iterator();
    while (lkit.hasNext()) {
      DistanceRank srcIDDR = lkit.next();
      SortedSet<DistanceRank> forSource = links.get(srcIDDR);
      Map<String, LayoutRubberStamper.EdgeMove> arrChops = null;
      Map<String, LayoutRubberStamper.EdgeMove> depChops = null;
       
      if (recoveryDataMap != null) {     
        LinkPlacementGrid.RecoveryDataForSource rdfs = recoveryDataMap.get(srcIDDR.id);      
        arrChops = rdfs.getArrivalRegionChops();
        depChops = rdfs.getDepartureRegionChops();
      }
      generateDepartsAndArrives(grid, rcx, forSource, arrChops, depChops,
                                departurePerLink, linksPerDeparture, arrivals, regArrivals, unseenLinks, needAltTargs, okGroupMap, monitor);
    }

    //
    // Now reserve those regions on the grid
    //

    Set<LinkPlacementGrid.TerminalRegion> untouchedDepartures = reserveTerminalRegions(newLinks, grid, rcx, departurePerLink, linksPerDeparture, 
                                                                                       arrivals, regArrivals, needAltTargs, okGroupMap, monitor);
    arrivals.putAll(regArrivals);
    
    //
    // Now do the links
    //

    int linkCount = links.size();    
    double currProg = startFrac;
    double progInc = (endFrac - startFrac) / linkCount;

 //   GenomeInstance gi = (genome instanceof GenomeInstance) ? (GenomeInstance)genome : null;
    
    //
    // With complex trees, some links might fail to find a route on the first pass, but
    // have better luck a second or third time as other targets of the source get laid out.
    // Loop until we have no improvement (or hit a hard limit).  Once we have no improvement,
    // we go to FORCE mode if some links are still laying around.
    //
    
    RoutingResult nonLinkRetval = new RoutingResult();
    RoutingResult linkRetval = new RoutingResult();
    lkit = links.keySet().iterator();
    while (lkit.hasNext()) {
      DistanceRank srcIDDR = lkit.next();
      SortedSet<DistanceRank> forSource = links.get(srcIDDR);
      LinkPlacementGrid.RecoveryDataForSource rdfs = null;
      Set<String> linksFromSource = genome.getOutboundLinks(srcIDDR.id);
      if (recoveryDataMap != null) {
        rdfs = recoveryDataMap.get(srcIDDR.id);
      }  
      HashSet<String> workingSet = new HashSet<String>();
      Iterator<DistanceRank> fsvit = forSource.iterator();
      while (fsvit.hasNext()) {
        DistanceRank ndr = fsvit.next();
        workingSet.add(ndr.id);
      }

      for (int i = 0; ((i < 2) && !workingSet.isEmpty()); i++) {
        boolean force = (i == 1);
        int lastFailCount = -1;
        for (int j = 0; j < 4; j++) { // limit on how many passes to make
          RoutingResult holdResult = new RoutingResult();
          if (rdfs == null) {
            layoutForSource(forSource, workingSet, grid, rcx, goodness, departurePerLink, arrivals, okGroupMap,
                            unseenLinks, srcIDDR, untouchedDepartures, force, holdResult, needColors, monitor, linksFromSource);
          } else {
            recoverForSource(forSource, workingSet, grid, rcx, goodness, departurePerLink, arrivals, okGroupMap,
                             unseenLinks, srcIDDR, untouchedDepartures, force, holdResult, needColors, monitor, rdfs, 
                             recoveryDataMap, linksFromSource, strictOKGroups);
          }
          workingSet.clear();
          workingSet.addAll(holdResult.failedLinks);
          nonLinkRetval.merge(holdResult);
          int numFail = holdResult.failedLinks.size();
          if ((numFail == 0) || (numFail == lastFailCount) || (numFail == forSource.size())) {
            break;
          }
          lastFailCount = numFail;
        }
      }   

      if (!workingSet.isEmpty()) {
        linkRetval.linkResult |= LAYOUT_PROBLEM;
        linkRetval.failedLinks.addAll(workingSet);
      }
      
      currProg += progInc; 
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    
    nonLinkRetval.linkResult = ((nonLinkRetval.linkResult & COLOR_PROBLEM) != 0x00) ? COLOR_PROBLEM : LAYOUT_OK;
    nonLinkRetval.failedLinks.clear();
    linkRetval.merge(nonLinkRetval);
    if (monitor != null) {
      if (!monitor.updateProgress((int)(endFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
    return (linkRetval);
  }

/***************************************************************************
  **
  ** When we have pad collisions, we need to come up with nearby fake terminal
  ** regions to allow the layout algorithm to succeed.
  */  
   
  private Map<String, LinkPlacementGrid.TerminalRegion> generateTerminalRegionsForCollisions(StaticDataAccessContext rcx,
                                                                                             LinkPlacementGrid grid, Set<String> links,
                                                                                             Set<String> needAltTargs, Map<String, Set<String>> okGroupMap) {
  
    Genome genome = rcx.getCurrentGenome();
    PadCalculatorToo pct = new PadCalculatorToo();
    HashMap<String, Set<String>> perTarg = new HashMap<String, Set<String>>();
    Iterator<String> lit = links.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      if (!needAltTargs.contains(linkID)) {
        continue;
      }
      Linkage link = genome.getLinkage(linkID);
      String trg = link.getTarget(); 
      Set<String> inlinks = perTarg.get(trg);
      if (inlinks == null) {
        inlinks = new HashSet<String>();
        perTarg.put(trg, inlinks);
      }
      inlinks.add(linkID);
    }
    
    HashMap<String, LinkPlacementGrid.TerminalRegion> retval = new HashMap<String, LinkPlacementGrid.TerminalRegion>();
    Iterator<String> ptkit = perTarg.keySet().iterator();
    while (ptkit.hasNext()) {
      String targID = ptkit.next();
      Set<String> inlinks = perTarg.get(targID);
      Map<String, LinkPlacementGrid.TerminalRegion> not = pct.generateTerminalRegionsForCollisions(rcx, grid, inlinks, 
                                                                                                   targID, needAltTargs, okGroupMap);
      retval.putAll(not);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Figure out the best approach for a target
  */
  
  private int entryQuadrant(Linkage link, Genome genome, Layout lo) {
     String trgID = link.getTarget();
     Node targNode = genome.getNode(trgID);
     int type = targNode.getNodeType();
     if (type == Node.GENE) {
       NodeProperties np = lo.getNodeProperties(trgID);
       if (np.getOrientation() == NodeProperties.RIGHT) {
         return (LinkPlacementGrid.NORTH | LinkPlacementGrid.WEST);
       } else {
         return (LinkPlacementGrid.NORTH | LinkPlacementGrid.EAST);
       }
     } else {
       return (LinkPlacementGrid.NONE);  
     }
  }
  
  /***************************************************************************
  **
  ** Do all the links for a source.  One pass.  If we have failures, we place whatever
  ** we can.
  */
  
  private void layoutForSource(SortedSet<DistanceRank> forSource, Set<String> workingSet, 
                               LinkPlacementGrid grid, 
                               StaticDataAccessContext rcx,
                               LinkPlacementGrid.GoodnessParams goodness,
                               Map<String, LinkPlacementGrid.TerminalRegion> departures, 
                               Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                               Map<String, Set<String>> okGroupMap,
                               HashSet<String> unseenLinks, DistanceRank srcIDDR, 
                               Set<LinkPlacementGrid.TerminalRegion> untouchedDepartures, boolean force,
                               RoutingResult result, Set<String> needColors, BTProgressMonitor monitor, Set<String> linksFromSource)
                                 throws AsynchExitRequestException { 
    
    Iterator<DistanceRank> fsit = forSource.iterator();
    Genome genome = rcx.getCurrentGenome();
    while (fsit.hasNext()) {
      DistanceRank dr = fsit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      String linkID = dr.id;
      if (!workingSet.contains(linkID)) {
        continue;
      }
      Linkage link = genome.getLinkage(linkID);
      int entryPref = entryQuadrant(link, genome, rcx.getCurrentLayout());
      Set<String> okGroups = okGroupMap.get(linkID);
      LinkPlacementGrid.TerminalRegion depart = departures.get(linkID);
      if (untouchedDepartures.contains(depart)) {          
        if (!addFirstForSource(srcIDDR.id, link, grid, rcx, departures, 
                               arrivals, goodness, okGroups, force, entryPref, linksFromSource)) {
          result.failedLinks.add(linkID);
        } else { 
          untouchedDepartures.remove(depart);
          unseenLinks.remove(linkID);
          needColors.add(linkID);
        }
      } else {
        if (!addAdditionalForSource(srcIDDR.id, link, grid, rcx, arrivals, 
                                    unseenLinks, goodness, okGroups, force, entryPref, linksFromSource)) {
          result.failedLinks.add(linkID);
        } else {
          unseenLinks.remove(linkID);
        }
      }
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Do all the links for a source.  One pass.  If we have failures, we place whatever
  ** we can.
  */
  
  private void recoverForSource(SortedSet<DistanceRank> forSource, Set<String> workingSet,                                                              
                                LinkPlacementGrid grid,
                                StaticDataAccessContext rcx, 
                                LinkPlacementGrid.GoodnessParams goodness,
                                Map<String, LinkPlacementGrid.TerminalRegion> departures, 
                                Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                Map<String, Set<String>> okGroupMap,
                                Set<String> unseenLinks, DistanceRank srcIDDR, 
                                Set<LinkPlacementGrid.TerminalRegion> untouchedDepartures, boolean force,
                                RoutingResult result, Set<String> needColors, 
                                BTProgressMonitor monitor, 
                                LinkPlacementGrid.RecoveryDataForSource rdfs, 
                                Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                Set<String> linksFromSource, boolean strictOKGroups)
                                  throws AsynchExitRequestException { 
    
    Iterator<DistanceRank> fsit = forSource.iterator();
    while (fsit.hasNext()) {
      DistanceRank dr = fsit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      String linkID = dr.id;
      if (!workingSet.contains(linkID)) {
        continue;
      }
     
      Genome genome = rcx.getCurrentGenome();
      Linkage link = genome.getLinkage(linkID);
      int entryPref = entryQuadrant(link, genome, rcx.getCurrentLayout());
      Set<String> okGroups = okGroupMap.get(linkID);   
      LinkPlacementGrid.TerminalRegion depart = departures.get(linkID);
      //
      // Keep in mind that a single gene source may have multiple "untouched departures", since
      // these can each be individual, unique departures from one region for the source!
      //

      if (untouchedDepartures.contains(depart)) {       
        if (!recoverFirstForSource(srcIDDR.id, link, grid, rcx, departures, 
                                  arrivals, goodness, okGroups, force, entryPref, rdfs, 
                                  recoveryDataMap, unseenLinks, strictOKGroups)) {
          result.failedLinks.add(linkID);
        } else {
          untouchedDepartures.remove(depart);
          unseenLinks.remove(linkID);
          needColors.add(linkID);
        }
      } else {
        if (!recoverAdditionalForSource(srcIDDR.id, link, grid, rcx, departures, arrivals, 
                                         unseenLinks, goodness, okGroups, force, 
                                         entryPref, rdfs, recoveryDataMap, linksFromSource, strictOKGroups)) {
          result.failedLinks.add(linkID);
        } else {
          unseenLinks.remove(linkID);
        }
      }
    }
    return;
  }  
  

  /***************************************************************************
  **
  ** Layout the given links using specialty layout
  */
  
  public RoutingResult specialtyLayout(List<GenomeSubset> subsetList, // of GenomeSubset
                                       List<SpecialtyInstructions> pointDataList,  // of pointData maps
                                       Map<String, SpecialtyLayoutLinkData> alienSources,
                                       Map<String, SpecialtyLayoutLinkData> srcToBetween, 
                                       NetModuleLinkExtractor.SubsetAnalysis sa,
                                       Set<String> newLinks,
                                       StaticDataAccessContext rcx,
                                       Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> splicePlans,
                                       BTProgressMonitor monitor,
                                       double startFrac, double endFrac)
                                       throws AsynchExitRequestException {

    RoutingResult retval = new RoutingResult();    
    if (monitor != null) {
      if (!monitor.updateProgress((int)(startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
    
    int numSub = subsetList.size();   
    if (numSub == 0) {
      return (retval);
    }
    
    GenomeSubset firstSubset = subsetList.get(0);
    Genome baseGenome = firstSubset.getBaseGenome();
    StaticDataAccessContext rcxb = new StaticDataAccessContext(rcx, baseGenome, rcx.getCurrentLayout());
    
    //
    // Build a single Specialty input for each source from the different
    // components before going off and placing the points!
    //
    
    glueBeforeBuilding(sa, srcToBetween, rcxb, pointDataList, 
                       alienSources, newLinks, splicePlans, monitor, startFrac, endFrac);
    
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Build a single Specialty input for each source from the different
  ** components before going off and placing the points!
  */
  
  private void glueBeforeBuilding(NetModuleLinkExtractor.SubsetAnalysis sa,
                                  Map<String, SpecialtyLayoutLinkData> srcToBetween,
                                  StaticDataAccessContext rcx,
                                  List<SpecialtyInstructions> pointDataList,  // of pointData maps
                                  Map<String, SpecialtyLayoutLinkData> alienSources,
                                  Set<String> newLinks,
                                  Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> splicePlans, 
                                  BTProgressMonitor monitor,
                                  double startFrac, double endFrac)
                                       throws AsynchExitRequestException {
          
 
    ArrayList<SpecialtyLayoutLinkData> placeList = new ArrayList<SpecialtyLayoutLinkData>();
    Iterator<String> stiit = sa.getSrcKeys();
    while (stiit.hasNext()) {
      String srcID = stiit.next();
      NetModuleLinkExtractor.PrimaryAndOthers pao = sa.getModulesForSrcID(srcID);
      NetModuleLinkExtractor.ExtractResultForSource interModForSrc = null;
      Map<Integer, LinkBundleSplicer.SpliceSolution> spSolnPerBorder = null;
      if (pao.sourceInModule()) { 
        String modName = sa.getPrimaryModuleName(pao);
        if (modName != null) {
          interModForSrc = sa.getExtractResultForSource(modName);
          spSolnPerBorder = splicePlans.get(modName);
        }
      }
      SpecialtyLayoutLinkData siBet = srcToBetween.get(srcID);
      SpecialtyInstructions spSrc = (!pao.sourceInModule()) ?  null : pointDataList.get(sa.getMappedPrimaryModuleIndex(pao));
      SpecialtyLayoutLinkData siSrc = (spSrc == null) ? alienSources.get(srcID) : spSrc.getLinkPointsForSrc(srcID);
      // For sources outside the subset list, the results have already been merged in.  All we need to do is merge the
      // siSrc into the placeList we are building.  
      placeList.add(siSrc);
      
      Iterator<Integer> oit = pao.getTargetModules();
      while (oit.hasNext()) {
        Integer other = oit.next();       
        String trgMod = sa.getTargetModuleName(pao, other); // May be null...
        NetModuleLinkExtractor.ExtractResultForTarg interModPath = 
          ((interModForSrc == null) || (trgMod == null)) ? null : interModForSrc.getResultForTarg(trgMod);
 
        SpecialtyLayoutLinkData siPlace;
        if (trgMod != null) {
          SpecialtyInstructions spPlace = pointDataList.get(sa.getMappedTargetModuleIndex(pao, other));       
          siPlace = spPlace.getLinkPointsForSrc(srcID);
        } else {
          siPlace = alienSources.get(srcID);
        }
 
        if (spSrc != null) {  // i.e. not an alien source that has already been merged...
          siPlace.setNeedsExitGlue(siPlace.getLinkList().get(0));
          Map<Integer, LinkBundleSplicer.SpliceSolution> trgSolnPerBorder = splicePlans.get(trgMod);
          if (interModPath != null) {            
            siSrc.mergeToStubsWithPoints(srcID, siPlace, spSolnPerBorder, trgSolnPerBorder, interModPath);
          } else {
            siSrc.mergeToStubsWithBetween(siPlace, siBet, spSolnPerBorder);
          }
        }
      }
    } 
    LayoutFailureTracker.reportGluingDone(placeList);
    
    
    //
    // After glue, then build:
    //
    
    double currProg = startFrac;
    int linkCount = newLinks.size();
    double perLink = (endFrac - startFrac) / linkCount;
    
    // FYI: 8/20/13 placePointData still taking 17 seconds on 6.7K node 28.4K link test network!
    Iterator<SpecialtyLayoutLinkData> plit = placeList.iterator();
    while (plit.hasNext()) {
      SpecialtyLayoutLinkData siSrc = plit.next();
      currProg = placePointData(null, siSrc, true, rcx, currProg, perLink, monitor);
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Place point data for a source
  */
  
  private double placePointData(SpecialtyLayoutLinkData siPlace,
                                SpecialtyLayoutLinkData siSrc,                             
                                boolean isFirst,
                                StaticDataAccessContext rcx,
                                double currProg, double perLink,
                                BTProgressMonitor monitor) throws AsynchExitRequestException {

    boolean isSource = (siPlace == null);
    SpecialtyLayoutLinkData si = (siPlace == null) ? siSrc : siPlace;
 
    //
    // Gotta do this before getting the cleaned position list.   Also, note
    // that the link ordering we are about to use will be changed!
    
    if (si != null) {
      si.normalizeLinks();
      LayoutFailureTracker.reportReorder(si);
    }
    Map<String, Map<String, LinkBusDrop>> cache = new HashMap<String, Map<String, LinkBusDrop>>();
    
    Genome baseGenome = rcx.getCurrentGenome();
    int numLinks = (si == null) ? 0 : si.numLinks();
    for (int i = 0; i < numLinks; i++) {
      String linkID = si.getLink(i);
      Linkage link = baseGenome.getLinkage(linkID);
      SpecialtyLayoutLinkData.NoZeroList ptList = si.getCleanedPositionList(linkID);
      LayoutFailureTracker.reportAssembly(si.getSrcID(), linkID, ptList);
      Point2D lpLoc = new Point2D.Double();
      Vector2D lpDir = new Vector2D(0.0, 0.0);
      launchPadLoc(link, rcx, lpLoc, lpDir);
      Point2D pt = ptList.get(0).getPoint();
      if (pt.equals(lpLoc)) {
        pt = lpDir.scaled(UiUtil.GRID_SIZE).add(pt);
      }
      if (i == 0) { // first link for source in this sip
        // Remember, at a minimum, we have a crude auto-layout that has occurred already!
        BusProperties bp = rcx.getCurrentLayout().getLinkProperties(linkID);
        if (isFirst) { // first time for source globally        
          if (isSource) { // actually are placing the root here
            positionRootSegment(linkID, rcx, pt);
          } else { // gotta glue on somebody else           
            if (bp.isDirect()) {
              LinkSegment newRoot = new LinkSegment(pt, null);
              bp.addSegmentInSeries(newRoot);
            } else {
              relocateFast(linkID, rcx.getCurrentLayout(), bp.getRootSegment().getStart(), cache);
              addAndLocateCorner(link, rcx.getCurrentLayout(), pt);
            }
          }
        } else {  // not first time through
          relocateFast(linkID, rcx.getCurrentLayout(), bp.getRootSegment().getStart(), cache);
          addAndLocateCorner(link, rcx.getCurrentLayout(), pt);          
        }
      } else { // following links just glue wrt the first link
        relocateFast(linkID, rcx.getCurrentLayout(), pt, cache);
      }
      int numPts = ptList.size();
      for (int k = 1; k < numPts; k++) {
        pt = ptList.get(k).getPoint();
        addAndLocateCorner(link, rcx.getCurrentLayout(), pt);
      }
      currProg += perLink;
      if (monitor != null) { 
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }     
    }
    return (currProg);
  }
  
  /***************************************************************************
  **
  ** Get the (forced) launch pad location:
  */
  
  public void launchPadLoc(Linkage link, StaticDataAccessContext rcx, Point2D loc, Vector2D vec) {
    String srcID = link.getSource();
    Node src = rcx.getCurrentGenome().getNode(srcID);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(srcID);    
    INodeRenderer render = np.getRenderer();
    Vector2D launchOff = render.getLaunchPadOffset(link.getLaunchPad(), src, rcx);
    Vector2D departDir = render.getDepartureDirection(link.getLaunchPad(), src, rcx.getCurrentLayout());
    vec.setXY(departDir.getX(), departDir.getY());
    Point2D nodePoint = np.getLocation();
    Point2D start = launchOff.add(nodePoint);
    UiUtil.forceToGrid(start.getX(), start.getY(), loc, 10.0);
    return;
  }
  
  /***************************************************************************
  **
  ** Returns status of a link build
  */
  
  public static class RoutingResult {

    public int linkResult;
    public int colorResult;
    public String collisionSrc1;
    public String collisionSrc2;
    public HashSet<String> failedLinks;

    public RoutingResult() {
      linkResult = LAYOUT_OK;    
      colorResult = ColorAssigner.COLORING_OK;
      collisionSrc1 = null;
      collisionSrc2 = null;
      failedLinks = new HashSet<String>();
    }
    
    public RoutingResult(int linkResult) {
      this.linkResult = linkResult;    
      colorResult = ColorAssigner.COLORING_OK;
      collisionSrc1 = null;
      collisionSrc2 = null;
      failedLinks = new HashSet<String>();
    }
    
    public void merge(RoutingResult other) {
      this.linkResult |= other.linkResult;
      this.colorResult |= other.colorResult;
      // Only one collision survives the merge:
      this.collisionSrc1 = (this.collisionSrc1 == null) ? other.collisionSrc1 : this.collisionSrc1;
      this.collisionSrc2 = (this.collisionSrc2 == null) ? other.collisionSrc2 : this.collisionSrc2;
      this.failedLinks.addAll(other.failedLinks);
      return;
    }
  }

  /***************************************************************************
  **
  ** Add first link for source
  */
  
  private boolean recoverFirstForSource(String srcID, Linkage link, 
                                        LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                        Map<String, LinkPlacementGrid.TerminalRegion> departures, 
                                        Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                        LinkPlacementGrid.GoodnessParams goodness, 
                                        Set<String> okGroups, boolean force, int entryPref, 
                                        LinkPlacementGrid.RecoveryDataForSource rdfs, 
                                        Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                        Set<String> unseenLinks, boolean strictOKGroups) {
    String linkID = link.getID();
    LinkPlacementGrid.TerminalRegion depart = departures.get(linkID);
    LinkPlacementGrid.TerminalRegion arrive = arrivals.get(linkID);
    String targID = link.getTarget();
    LinkPlacementGrid.RecoveryDataForLink rdfl = new LinkPlacementGrid.RecoveryDataForLink(rdfs, linkID);
    LinkPlacementGrid.RecoveryDataForLink useRdfl = rdfl;
    Map<String, LayoutRubberStamper.EdgeMove> depChops = rdfs.getDepartureRegionChops();
    boolean toss = false;
    boolean forBounds = ((depChops != null) && !depChops.isEmpty());
    //
    // We were seeing bad region layout results with departures, which were solved by ignoring the
    // initial departure point in recovery, then adding it back when we are done (below).  Kinda
    // bogus.  Just before release, cases cropped up where this is the wrong thing to do.  Decided
    // that the criteria is whether the first point in inside or outside of region boundary.  This
    // does that...
    //
    if (forBounds) {
      LayoutRubberStamper.EdgeMove emov = depChops.get(linkID);
      Point2D ar = rdfl.getPoint(0);
      toss = (emov == null) || (emov.pas == null) || emov.pas.isBehind(ar);
      if (toss && (rdfl.getPointCount() > 1)) {
        LayoutFailureTracker.recordRecoveryToss(srcID, linkID, emov, ar);
        useRdfl = new LinkPlacementGrid.RecoveryDataForLink(rdfl, 1);
      } 
    }
    List<Point2D> points = grid.recoverValidFirstTrack(srcID, targID, linkID, depart, arrive, 
                                                       goodness, okGroups, force, entryPref, useRdfl, 
                                                       recoveryDataMap, departures, arrivals, unseenLinks, strictOKGroups);
    int numPoints = (points == null) ? 0 : points.size();
    if (numPoints == 0) {
      return (false);
    }
    if (numPoints > 0) {
      if (toss) {
        //
        // This is uninspiring, but it is operational when we have departure region chops....
        //
        Point2D ar = rdfl.getPoint(0);
        points.add(0, (Point2D)ar.clone());
        numPoints++;
      } 
      LayoutFailureTracker.recordRecoveryTrack(srcID, linkID, points);
      positionRootSegment(linkID, rcx, points.get(0));
    }
    for (int i = 1; i < numPoints; i++) {
      addAndLocateCorner(link, rcx.getCurrentLayout(), points.get(i));
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Add another link for source
  */
  
  private boolean recoverAdditionalForSource(String srcID, Linkage link,
                                             LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                             Map<String, LinkPlacementGrid.TerminalRegion> departures,
                                             Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                             Set<String> unseenLinks, 
                                             LinkPlacementGrid.GoodnessParams goodness, 
                                             Set<String> okGroups, boolean force, int entryPref, 
                                             LinkPlacementGrid.RecoveryDataForSource rdfs, 
                                             Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap, 
                                             Set<String> linksFromSource, boolean strictOKGroups) {
                                        
    String linkID = link.getID();
    String targID = link.getTarget();
     
    LinkPlacementGrid.TerminalRegion arrive = arrivals.get(linkID);
    ArrayList<Point2D> points = new ArrayList<Point2D>();
    LinkPlacementGrid.RecoveryDataForLink rdfl = new LinkPlacementGrid.RecoveryDataForLink(rdfs, linkID);
    LinkPlacementGrid.NewCorner newCorner = grid.recoverValidAddOnTrack(rcx, srcID, targID, linkID,  arrive, points, 
                                                                        goodness, okGroups, force, entryPref, rdfl, 
                                                                        recoveryDataMap, departures, 
                                                                        arrivals, unseenLinks,
                                                                        linksFromSource, strictOKGroups);
    int numPoints = points.size();
    if (numPoints <= 0) {
      return (false);
    }
    
    if (newCorner.type == LinkPlacementGrid.IS_RUN)  {
      if (!createCorner(link, newCorner.point, rcx, grid, unseenLinks, newCorner.dir)) {
        return (false);
      }
    } else {
      grid.addCornerDirection(newCorner.point, srcID, newCorner.dir);
    }
    relocate(linkID, rcx.getCurrentLayout(), newCorner.point);
    for (int i = 0; i < numPoints; i++) {
      addAndLocateCorner(link, rcx.getCurrentLayout(), points.get(i));
    }
    LayoutFailureTracker.recordRecoveryTrack(srcID, linkID, points);
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Add first link for source
  */
  
  private boolean addFirstForSource(String srcID, Linkage link,
                                    LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                    Map<String, LinkPlacementGrid.TerminalRegion> departures,
                                    Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                    LinkPlacementGrid.GoodnessParams goodness, 
                                    Set<String> okGroups, boolean force, int entryPref, 
                                    Set<String> linksFromSource) {
    String linkID = link.getID();
    LinkPlacementGrid.TerminalRegion depart = departures.get(linkID);
    LinkPlacementGrid.TerminalRegion arrive = arrivals.get(linkID);
    if (arrive == null) {
      return (false);
    }
    String targID = link.getTarget();
    List<Point2D> points = grid.getValidFirstTrack(srcID, targID, linkID, depart, arrive, goodness, okGroups, force, entryPref, linksFromSource);
    int numPoints = points.size();
    if (numPoints == 0) {
      return (false);
    }
    if (numPoints > 0) {
      positionRootSegment(linkID, rcx, points.get(0));
    }
    for (int i = 1; i < numPoints; i++) {
      addAndLocateCorner(link, rcx.getCurrentLayout(), points.get(i));
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Add another link for source
  */
  
  private boolean addAdditionalForSource(String srcID, Linkage link,
                                         LinkPlacementGrid grid, StaticDataAccessContext rcx,
                                         Map<String, LinkPlacementGrid.TerminalRegion> arrivals, 
                                         Set<String> unseenLinks, 
                                         LinkPlacementGrid.GoodnessParams goodness, 
                                         Set<String> okGroups, boolean force, int entryPref, 
                                         Set<String> linksFromSource) {
                                        
    String linkID = link.getID();
    String targID = link.getTarget();
     
    LinkPlacementGrid.TerminalRegion arrive = arrivals.get(linkID);
    ArrayList<Point2D> points = new ArrayList<Point2D>();
    LinkPlacementGrid.NewCorner newCorner = grid.getValidAddOnTrack(rcx, srcID, targID, linkID, arrive, points, 
                                                                    goodness, okGroups, force, entryPref, unseenLinks, linksFromSource);
    int numPoints = points.size();
    if (numPoints <= 0) {
      return (false);
    }
    
    if (newCorner.type == LinkPlacementGrid.IS_RUN)  {
      if (!createCorner(link, newCorner.point, rcx, grid, unseenLinks, newCorner.dir)) {
        return (false);
      }
    } else {
      grid.addCornerDirection(newCorner.point, srcID, newCorner.dir);
    }
    relocate(linkID, rcx.getCurrentLayout(), newCorner.point);
    for (int i = 0; i < numPoints; i++) {
      addAndLocateCorner(link, rcx.getCurrentLayout(), points.get(i));
    }

    return (true);
  }  

  /***************************************************************************
  **
  ** Generate a departure region
  */
  
  private LinkPlacementGrid.TerminalRegion generateDeparture(String linkID, StaticDataAccessContext rcx,
                                                             LinkPlacementGrid grid, Set<String> okGroups) {     
    
    Genome genome = rcx.getCurrentGenome();
    Linkage link = genome.getLinkage(linkID);
    String srcID = link.getSource();
    if ((genome instanceof GenomeInstance) && (okGroups != null)) {
      GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(linkID);
      okGroups.add(tup.getSourceGroup());
    }
    Node src = genome.getNode(srcID);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(srcID);    
    INodeRenderer render = np.getRenderer();
    Vector2D launchOff = render.getLaunchPadOffset(link.getLaunchPad(), src, rcx);
    Vector2D departDir = render.getDepartureDirection(link.getLaunchPad(), src, rcx.getCurrentLayout());
    Point2D nodePoint = np.getLocation();
    Point2D start = launchOff.add(nodePoint);
    Point2D forced = new Point2D.Double();
    UiUtil.forceToGrid(start.getX(), start.getY(), forced, 10.0);
    Point gridStart = new Point(((int)forced.getX() / 10), ((int)forced.getY() / 10));    
    return (grid.getDepartureRegion(srcID, departDir, okGroups, gridStart, 5));
  }
  
  /***************************************************************************
  **
  ** Generate a terminal region for a region boundary
  */
  
  private LinkPlacementGrid.TerminalRegion generateRegionBoundary(String linkID, Genome genome,
                                                                  LayoutRubberStamper.EdgeMove em,
                                                                  LinkPlacementGrid grid, Set<String> okGroups,
                                                                  boolean forArrival) {     
    Linkage link = genome.getLinkage(linkID);
    String srcID = link.getSource();
    if ((genome instanceof GenomeInstance) && (okGroups != null)) {
      GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(linkID);
      okGroups.add(tup.getSourceGroup());
    }
    Point2D forced = new Point2D.Double();
    UiUtil.forceToGrid(em.getEdgePoint().getX(), em.getEdgePoint().getY(), forced, 10.0);   
    Point gridTerm = new Point(((int)forced.getX() / 10), ((int)forced.getY() / 10));
    if (forArrival) {
      String targID = link.getTarget();
      return (grid.getArrivalRegion(srcID, targID, linkID, okGroups, em.getEdgeDirectionOutbound().scaled(-1.0), gridTerm, 5));
    } else {
      return (grid.getDepartureRegion(srcID, em.getEdgeDirectionOutbound(), okGroups, gridTerm, 5));
    }
  }
  
  /***************************************************************************
  **
  ** Generate an arrival region
  */
  
  public static LinkPlacementGrid.TerminalRegion generateArrival(String linkID, StaticDataAccessContext rcx,
                                                                 LinkPlacementGrid grid, Integer forcePad, 
                                                                 Set<String> okGroups) {     
    Genome genome = rcx.getCurrentGenome();
    Linkage link = genome.getLinkage(linkID);
    int landingPad = (forcePad != null) ? forcePad.intValue() : link.getLandingPad();
    String srcID = link.getSource();
    String targID = link.getTarget();
    if ((genome instanceof GenomeInstance) && (okGroups != null)) {
      GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(linkID);
      okGroups.add(tup.getSourceGroup());
      okGroups.add(tup.getTargetGroup());
    }
    Node trg = genome.getNode(targID);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(targID);    
    INodeRenderer render = np.getRenderer();
    Vector2D landOff = render.getLandingPadOffset(landingPad, trg, link.getSign(), rcx);
    landOff = landOff.quantizeBigger(10.0);    
    Vector2D arriveDir = render.getArrivalDirection(landingPad, trg, rcx.getCurrentLayout());
    Point2D nodePoint = np.getLocation();
    Point2D end = landOff.add(nodePoint);
    Point2D forced = new Point2D.Double();
    UiUtil.forceToGrid(end.getX(), end.getY(), forced, 10.0);
    Point gridEnd = new Point(((int)forced.getX() / 10), ((int)forced.getY() / 10));
    return (grid.getArrivalRegion(srcID, targID, linkID, okGroups, arriveDir, gridEnd, 5));
  } 
  
  /***************************************************************************
  **
  ** Generate an arrival region for emergency conditions.  May be null if it fails
  */
  
  public static LinkPlacementGrid.TerminalRegion generateEmergencyArrival(String linkID, StaticDataAccessContext rcx,
                                                                          LinkPlacementGrid grid, 
                                                                          int landingPad, int horiz, int pass, Set<String> okGroups) {     
    Genome genome = rcx.getCurrentGenome();
    Linkage link = genome.getLinkage(linkID);
    String srcID = link.getSource();
    String targID = link.getTarget();
    if ((genome instanceof GenomeInstance) && (okGroups != null)) {
      GenomeInstance.GroupTuple tup = ((GenomeInstance)genome).getRegionTuple(linkID);
      okGroups.add(tup.getSourceGroup());
      okGroups.add(tup.getTargetGroup());
    }     
    Node trg = genome.getNode(targID);
    NodeProperties np = rcx.getCurrentLayout().getNodeProperties(targID);    
    INodeRenderer render = np.getRenderer();
    
    
    Vector2D landOff = render.getLandingPadOffset(landingPad, trg, link.getSign(), rcx);
    landOff = landOff.quantizeBigger(10.0);    
    Vector2D arriveDir = render.getArrivalDirection(landingPad, trg, rcx.getCurrentLayout());
    Vector2D orthDir = arriveDir.normal();
    Vector2D offsetDir = orthDir.scaled(10.0 * horiz);
    Vector2D passDir = arriveDir.scaled(-10.0 * pass);
    Point2D nodePoint = np.getLocation();
    Point2D end = landOff.add(nodePoint);
    end = offsetDir.add(end);
    Point2D forced = new Point2D.Double();
    UiUtil.forceToGrid(end.getX(), end.getY(), forced, 10.0);
    Point2D passForced = passDir.add(forced);    
    Point gridEnd = new Point(((int)passForced.getX() / 10), ((int)passForced.getY() / 10));
    return (grid.getEmergencyArrivalRegion(srcID, targID, linkID, okGroups, arriveDir, gridEnd));
  }   
  
  /***************************************************************************
  **
  ** Position the root segment to the given point
  */
            
  private void positionRootSegment(String linkID, StaticDataAccessContext rcx, Point2D point) {
                                 
    BusProperties bp = rcx.getCurrentLayout().getLinkProperties(linkID);
    LayoutFailureTracker.forceFailureReport(linkID, "FAILURE_LINK_ID", bp);
    if (bp.isDirect()) {
      bp.splitNoSegmentBus(rcx);
    }
    LinkSegment rootSeg = bp.getRootSegment();
    Point2D start = rootSeg.getStart();
    
    LinkSegmentID[] segIDs = new LinkSegmentID[1];    
    segIDs[0] = LinkSegmentID.buildIDForSegment(rootSeg.getID());
    segIDs[0].tagIDWithEndpoint(LinkSegmentID.START);
    
    double dx = point.getX() - start.getX();
    double dy = point.getY() - start.getY();    
    dx = Math.round(dx / 10.0) * 10.0;
    dy = Math.round(dy / 10.0) * 10.0;     
    rcx.getCurrentLayout().moveBusLink(segIDs, dx, dy, start, bp);
    return;
  }
  
  /***************************************************************************
  **
  ** Add and locate additional corner points
  */
  
  private void addAndLocateCorner(Linkage link, Layout lo, Point2D point) {
    
    String linkID = link.getID();
    LinkSegmentID segID = LinkSegmentID.buildIDForEndDrop(linkID);
    BusProperties bp = lo.getLinkProperties(linkID);
    bp.linkSplitSupport(segID, point);
    //creates undo data at each step:
    lo.splitBusLinkBatchOnlyNoUndo(segID, point, bp);
    return;
  }

  /***************************************************************************
  **
  ** Create a corner point where there is a run
  */
  
  private boolean createCorner(Linkage link, Point2D split, StaticDataAccessContext rcx,
                               LinkPlacementGrid grid, Set<String> omitted, int dir) {

    BusProperties bp = rcx.getCurrentLayout().getLinkProperties(link.getID());
    LinkProperties.DistancedLinkSegID dlsegID = bp.intersectBusSegment(rcx, split, omitted, 5.0);    
    if (dlsegID == null) {  // May happen if we have serious mismatch of grid and reality!
      return (false);
    }
    rcx.getCurrentLayout().splitBusLink(dlsegID.segID, split, bp, null, rcx);
    // FIX ME!!! WHEN DOES THIS GET DONE????
    grid.convertRunToCorner(split, link.getSource(), dir);
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Relocate the start of the link to the given corner point
  */
  
  private void relocateFast(String linkID, Layout lo, Point2D reloc, Map<String, Map<String, LinkBusDrop>> cache) {  
    BusProperties bp = lo.getLinkProperties(linkID);
    Map<String, LinkBusDrop> bdm = null;
    if (cache != null) {
      String srcID = bp.getSourceTag();
      bdm = cache.get(srcID);
      if (bdm == null) {
        bdm = lo.prepForReattach(bp);
        cache.put(srcID, bdm);
      }
    }
    
    LayoutFailureTracker.forceFailureReport(linkID, "FAILURE_LINK_ID", bp);
    LinkSegmentID dropID = LinkSegmentID.buildIDForDrop(linkID);
    Iterator<LinkSegment> sit = bp.getSegments(); 
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      Point2D end = ls.getEnd();
      if (end != null) {
        if (end.equals(reloc)) {
          LinkSegmentID relocSeg = LinkSegmentID.buildIDForSegment(ls.getID());
          relocSeg.tagIDWithEndpoint(LinkSegmentID.END);
          lo.relocateSegmentOnTreeBatchOnlyNoUndo(bp, relocSeg, dropID, bdm);
          return;
        }
      }
    }
    //
    // Need to relocate to root:
    //
    LinkSegmentID relocSeg = LinkSegmentID.buildIDForStartDrop(); 
    LayoutFailureTracker.considerFailureReport(linkID, reloc, bp);
    lo.relocateSegmentOnTreeBatchOnlyNoUndo(bp, relocSeg, dropID, bdm);
    return;
  }
  
  /***************************************************************************
  **
  ** Relocate the start of the link to the given corner point
  */
  
  private void relocate(String linkID, Layout lo, Point2D reloc) {  
    BusProperties bp = lo.getLinkProperties(linkID);
     
    LayoutFailureTracker.forceFailureReport(linkID, "FAILURE_LINK_ID", bp);
    LinkSegmentID dropID = LinkSegmentID.buildIDForDrop(linkID);
    Iterator<LinkSegment> sit = bp.getSegments(); 
    while (sit.hasNext()) {
      LinkSegment ls = sit.next();
      Point2D end = ls.getEnd();
      if (end != null) {
        if (end.equals(reloc)) {
          LinkSegmentID relocSeg = LinkSegmentID.buildIDForSegment(ls.getID());
          relocSeg.tagIDWithEndpoint(LinkSegmentID.END);
          lo.relocateSegmentOnTree(bp, relocSeg, dropID, null, null);
          return;
        }
      }
    }
    //
    // Need to relocate to root:
    //
    LinkSegmentID relocSeg = LinkSegmentID.buildIDForStartDrop(); 
    LayoutFailureTracker.considerFailureReport(linkID, reloc, bp);
    lo.relocateSegmentOnTree(bp, relocSeg, dropID, null, null);
    return;
  }
 
  /***************************************************************************
  **
  ** Organize new links into processing order
 
  
  private Map organizeLinksByDistance(Set newLinks, Genome genome, Layout lo) {  
  
    //
    // Go through links.  For each source, make a list of links that goes from 
    // nearest to farthest!  NOT farthest target to nearest target
    //
  
    HashMap retval = new HashMap();
    Iterator nlit = newLinks.iterator();
    while (nlit.hasNext()) {
      String linkID = (String)nlit.next();
      Linkage link = genome.getLinkage(linkID);
      String srcID = link.getSource();
      String trgID = link.getTarget();
      TreeSet ranking = (TreeSet)retval.get(srcID); 
      if (ranking == null) {
        ranking = new TreeSet();//Collections.reverseOrder());
        retval.put(srcID, ranking);
      }
      NodeProperties srcProp = lo.getNodeProperties(srcID);    
      Point2D srcLoc = srcProp.getLocation();      
      NodeProperties trgProp = lo.getNodeProperties(trgID);    
      Point2D trgLoc = trgProp.getLocation();
      double distance = new Vector2D(srcLoc, trgLoc).length();
      DistanceRank rank = new DistanceRank(linkID, distance);
      ranking.add(rank);
    }
    
    TreeMap byTrgCount = new TreeMap(Collections.reverseOrder());
    Iterator rvit = retval.keySet().iterator();
    while (rvit.hasNext()) {
      String srcID = (String)rvit.next();
      TreeSet forSource = (TreeSet)retval.get(srcID);
      DistanceRank rank = new DistanceRank(srcID, forSource.size());
      byTrgCount.put(rank, forSource);
    }
    
    return (byTrgCount);    
    
    //return (retval);
  }

  /***************************************************************************
  **
  ** Organize new links into processing order
  */
  
  private void generateColumnInfo(Set<String> newLinks, Genome genome, Layout lo, 
                                  HashMap<String, SortedMap<Double, List<String>>> posMap, 
                                  HashMap<String, SortedMap<Double, List<String>>> negMap) {  
  
    //
    // Go through links.  In general, we layout links from nearest to farthest.
    // However, if targets are all in a vertical column, we lay out that column
    // from farthest to nearest to get "clean" link trees.
    //
  
    Iterator<String> nlit = newLinks.iterator();
    while (nlit.hasNext()) {
      String linkID = nlit.next();
      Linkage link = genome.getLinkage(linkID);
      String srcID = link.getSource();
      String trgID = link.getTarget();
      NodeProperties srcProp = lo.getNodeProperties(srcID);    
      Point2D srcLoc = srcProp.getLocation();
      double srcXPos = srcLoc.getX();
      NodeProperties trgProp = lo.getNodeProperties(trgID);    
      Point2D trgLoc = trgProp.getLocation();
      double trgXPos = trgLoc.getX();
      HashMap<String, SortedMap<Double, List<String>>> useMap = (srcXPos > trgXPos) ? negMap : posMap;
      SortedMap<Double, List<String>> cols = useMap.get(srcID); 
      if (cols == null) {
        cols = (srcXPos > trgXPos) ? new TreeMap<Double, List<String>>(Collections.reverseOrder()) : new TreeMap<Double, List<String>>(); 
        useMap.put(srcID, cols);
      }
      Double trgXPosObj = new Double(trgXPos);
      List<String> trgsForCol = cols.get(trgXPosObj); 
      if (trgsForCol == null) {
        trgsForCol = new ArrayList<String>();
        cols.put(trgXPosObj, trgsForCol);
      }
      trgsForCol.add(linkID);      
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Find smallest leap between two columns and sort on distances from it.
  */
  
  private Point2D findSmallestLeap(String srcID, Genome genome, Layout lo, 
                                   List<String> col1, List<String> col2, Point2D lastJump) {  
  
    //
    // If col1 is null, we just look for smallest jump from the source
    //
    
    if (col1 == null) {
      Point2D minLocFromSrc = null;
      double minDist = Double.POSITIVE_INFINITY;
      NodeProperties srcProp = lo.getNodeProperties(srcID);
      Point2D srcLoc = srcProp.getLocation();
      int colSize = col2.size();
      for (int i = 0; i < colSize; i++) {
        String linkID = col2.get(i);
        Linkage link = genome.getLinkage(linkID);
        String trgID = link.getTarget();        
        NodeProperties trgProp = lo.getNodeProperties(trgID);    
        Point2D trgLoc = trgProp.getLocation();
        double distance = new Vector2D(srcLoc, trgLoc).length();
        if (distance < minDist) {
          minLocFromSrc = trgLoc;
          minDist = distance;
        }
      }
      return (minLocFromSrc);
    }
    
    //
    // If more than one point qualifies, take the one closest to the last jump
    // 

    ArrayList<Point2D> minLocs = new ArrayList<Point2D>();
    double minDist = Double.POSITIVE_INFINITY;
    int col1Size = col1.size();
    for (int i = 0; i < col1Size; i++) {      
      String c1linkID = col1.get(i);
      Linkage c1link = genome.getLinkage(c1linkID);
      String c1trgID = c1link.getTarget();        
      NodeProperties c1trgProp = lo.getNodeProperties(c1trgID);    
      Point2D c1trgLoc = c1trgProp.getLocation();      
      int col2Size = col2.size();
      for (int j = 0; j < col2Size; j++) {
        String c2linkID = col2.get(j);
        Linkage c2link = genome.getLinkage(c2linkID);
        String c2trgID = c2link.getTarget();        
        NodeProperties c2trgProp = lo.getNodeProperties(c2trgID);    
        Point2D c2trgLoc = c2trgProp.getLocation();
        double distance = new Vector2D(c1trgLoc, c2trgLoc).length();
        if (distance < minDist) {
          minLocs.clear();
          minLocs.add(c2trgLoc);
          minDist = distance;
        } else if (distance == minDist) {
          minLocs.add(c2trgLoc);
        }
      }
    } 

    Point2D minLoc = null;    
    int minSize = minLocs.size();
    minDist = Double.POSITIVE_INFINITY;
    for (int i = 0; i < minSize; i++) {
      Point2D pt = minLocs.get(i);
      double distance = new Vector2D(lastJump, pt).length();
      if (distance < minDist) {
        minLoc = pt;
        minDist = distance;
      }
    }
    return (minLoc);
  }   
  
  /***************************************************************************
  **
  ** Find smallest leap between two columns and sort on distances from it.
  */
  
  private Point2D sortFromSmallestLeap(String srcID, Genome genome, Layout lo, 
                                       List<String> col1, List<String> col2, 
                                       Point2D lastJump, SortedSet<DistanceRank> results) {  

                                      
    Point2D minLoc = findSmallestLeap(srcID, genome, lo, col1, col2, lastJump);                                       
                                      
    int colSize = col2.size();
    for (int i = 0; i < colSize; i++) {
      String linkID = col2.get(i);
      Linkage link = genome.getLinkage(linkID);
      String trgID = link.getTarget();        
      NodeProperties trgProp = lo.getNodeProperties(trgID);    
      Point2D trgLoc = trgProp.getLocation();
      // FIX ME:  Have seen a crash here with null minLoc/trgLoc.
      // Has not been reproduced yet...
      // Since findSmallestLeap can return null, seems likely.
      // Instead of croaking, make it last and let us know:
      DistanceRank distRank;
      if ((minLoc == null) || (trgLoc == null)) {
        distRank = new DistanceRank(linkID, Double.POSITIVE_INFINITY);
        System.err.println("ERROR generating DistanceRank! minloc=" + minLoc + " trgloc=" + trgLoc + " c1=" + col1 + " c2 =" + col2);      
      } else {
        double distance = new Vector2D(minLoc, trgLoc).length();
        distRank = new DistanceRank(linkID, distance);
      }
      results.add(distRank);
    }
    return (minLoc);
  }  

  /***************************************************************************
  **
  ** Organize new links into processing order
  */
  
  private SortedMap<Double, SortedSet<DistanceRank>> sortAMap(String srcID, Genome genome, Layout lo, 
                                                              SortedMap<Double, List<String>> theMap, boolean reverse) {  

    TreeMap<Double, SortedSet<DistanceRank>> retval = (reverse) ? new TreeMap<Double, SortedSet<DistanceRank>>(Collections.reverseOrder()) 
                                                                : new TreeMap<Double, SortedSet<DistanceRank>>();
    List<String> lastList = null;
    Point2D lastJump = null;
    Iterator<Double> tmkit = theMap.keySet().iterator();
    while (tmkit.hasNext()) {
      Double tmKey = tmkit.next();
      List<String> trgs = theMap.get(tmKey);
      TreeSet<DistanceRank> forCol = new TreeSet<DistanceRank>(Collections.reverseOrder());
      lastJump = sortFromSmallestLeap(srcID, genome, lo, lastList, trgs, lastJump, forCol);
      lastList = trgs;
      retval.put(tmKey, forCol);
    }
    
    return (retval);
  }

  /***************************************************************************
  **
  ** Organize new links into processing order
  */
  
  private double buildDummyRanking(SortedMap<Double, SortedSet<DistanceRank>> ordered, double startDummy, SortedSet<DistanceRank> result) {
    
    double dummyDistance = startDummy;
    Iterator<Double> okit = ordered.keySet().iterator();
    while (okit.hasNext()) {
      Double trgXPos = okit.next();
      SortedSet<DistanceRank> trgsForCol = ordered.get(trgXPos);
      DistanceRank closestForCol = trgsForCol.last(); //first();
      DistanceRank dummyRank = new DistanceRank(closestForCol.id, dummyDistance);
      dummyDistance += 1.0;
      result.add(dummyRank);
      if (trgsForCol.size() > 1) {
        //DistanceRank farthestForCol = (DistanceRank)trgsForCol.last();
        //dummyRank = new DistanceRank(farthestForCol.id, dummyDistance);
        //dummyDistance += 1.0;
        //result.add(dummyRank);    
        Iterator<DistanceRank> tcit = trgsForCol.iterator();
        while (tcit.hasNext()) {
          DistanceRank drank = tcit.next();
          if ((drank != closestForCol)) { // && (drank != farthestForCol)) {
            dummyRank = new DistanceRank(drank.id, dummyDistance);
            dummyDistance += 1.0;
            result.add(dummyRank);
          }
        }
      }
    }
    return (dummyDistance);
  }
  
  /***************************************************************************
  **
  ** Fold positive and negative results together (do negatives first)
  */
  
  private SortedSet<DistanceRank> mergeMapsForSource(SortedMap<Double, SortedSet<DistanceRank>> negOrdered, 
                                                     SortedMap<Double, SortedSet<DistanceRank>> posOrdered) { 
           
    TreeSet<DistanceRank> retval = new TreeSet<DistanceRank>();
    double dummyDistance = 1.0;
    if (negOrdered != null) {
      dummyDistance = buildDummyRanking(negOrdered, dummyDistance, retval);
    }
    if (posOrdered != null) {
      dummyDistance = buildDummyRanking(posOrdered, dummyDistance, retval);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Organize link list to process the most complex hyperedges first
  */
  
  private SortedMap<DistanceRank, SortedSet<DistanceRank>> reorderByTargetCount(Map<String, SortedSet<DistanceRank>> retval) {  

    TreeMap<DistanceRank, SortedSet<DistanceRank>> byTrgCount = new TreeMap<DistanceRank, SortedSet<DistanceRank>>(Collections.reverseOrder());
    Iterator<String> rvit = retval.keySet().iterator();
    while (rvit.hasNext()) {
      String srcID = rvit.next();
      SortedSet<DistanceRank> forSource = retval.get(srcID);
      DistanceRank rank = new DistanceRank(srcID, forSource.size());
      byTrgCount.put(rank, forSource);
    }    
    return (byTrgCount);
  }   

  /***************************************************************************
  **
  ** Organize recovery links into processing order to maximize the use of DOFs
  */
  
  private List<String> buildRecoveryForSourceOrder(LinkPlacementGrid.RecoveryDataForSource rdfs) {  
    //
    // Go down the tree. starting from root.  At each branch, figure out if any kids are
    // needing orhogonal fixup.  If they are, the links through that diagonal kid are
    // placed first.  Then for those links, do the same thing.
    //
    
    LinkSegmentID rootID = rdfs.getRootSeg();
    Map<LinkSegmentID, SortedSet<LinkSegmentID>> kidMap = rdfs.getSegmentToKidsMap();
    
    // direct case:
    
    if (rootID == null) {
      ArrayList<String> retval = new ArrayList<String>();  
      retval.add(rdfs.getDirectLinkID());
      return (retval);
    }
     
    return (recoveryForSourceOrderRecursion(rootID, kidMap, rdfs));   
  }
  
 /***************************************************************************
  **
  ** Organize recovery links into processing order to maximize the use of DOFs: recursion
  */
  
  private List<String> recoveryForSourceOrderRecursion(LinkSegmentID currID, Map<LinkSegmentID, SortedSet<LinkSegmentID>> kidMap,
                                                       LinkPlacementGrid.RecoveryDataForSource rdfs) { 
    
    
    ArrayList<String> retval = new ArrayList<String>();  
    if (currID.isForEndDrop()) {
      retval.add(currID.getEndDropLinkRef());
      return (retval);
    }
      
    Point2D currPt = rdfs.getPoint(currID);
    SortedSet<LinkSegmentID> kidsOfCurr = kidMap.get(currID);
    Iterator<LinkSegmentID> kocit = kidsOfCurr.iterator();  
    while (kocit.hasNext()) {
      LinkSegmentID kidID = kocit.next();
      List<String> listOfKid = recoveryForSourceOrderRecursion(kidID, kidMap, rdfs);     
      Point2D kidPt = rdfs.getPoint(kidID);
      if ((currPt == null) || (kidPt == null)) { // FIX ME!!!!!!!
        retval.addAll(0, listOfKid);
      } else if (rdfs.getInboundOrtho(kidID)) {
        Vector2D toKid = new Vector2D(currPt, kidPt);
        if (!toKid.isCanonical()) {
          retval.addAll(0, listOfKid);
        } else {
          retval.addAll(listOfKid);
        }
      } else {
        retval.addAll(listOfKid);
      }
    }
    return (retval);
  }     
    
  /***************************************************************************
  **
  ** Organize new links into processing order
  */
  
  private SortedMap<DistanceRank, SortedSet<DistanceRank>> organizeLinks(Set<String> newLinks, Genome genome, Layout lo, 
                                                                         Map<String, LinkPlacementGrid.RecoveryDataForSource> recoveryDataMap) {
    
    TreeMap<String, SortedSet<DistanceRank>> retval = new TreeMap<String, SortedSet<DistanceRank>>();
    
    if (recoveryDataMap != null) {     
      Iterator<String> rdmkit = recoveryDataMap.keySet().iterator();
      while (rdmkit.hasNext()) { 
        String srcKey = rdmkit.next();
        double dummyDist = 1.0;
        LinkPlacementGrid.RecoveryDataForSource rdfs = recoveryDataMap.get(srcKey);
        // Cleanup to drop any zero-length segments...
        rdfs.cleanup();
        List<String> toList = buildRecoveryForSourceOrder(rdfs);
        TreeSet<DistanceRank> ordered = new TreeSet<DistanceRank>();
        retval.put(srcKey, ordered);
        int numTarg = toList.size();
        for (int i = 0; i < numTarg; i++) {
          String linkID = toList.get(i);
          if (!newLinks.contains(linkID)) {
            continue;
          }
          DistanceRank dr = new DistanceRank(linkID, dummyDist);
          dummyDist += 1.0;
          ordered.add(dr);
        }
      }
      return (reorderByTargetCount(retval));
    }
        
  
    //
    // Go through links.  In general, we layout links from nearest to farthest.
    // However, if targets are all in a vertical column, we lay out that column
    // from farthest to nearest to get "clean" link trees.
    //
  
    HashMap<String, SortedMap<Double, List<String>>> posMap = new HashMap<String, SortedMap<Double, List<String>>>();
    HashMap<String, SortedMap<Double, List<String>>> negMap = new HashMap<String, SortedMap<Double, List<String>>>();
    generateColumnInfo(newLinks, genome, lo, posMap, negMap);

    HashSet<String> srcKeys = new HashSet<String>(posMap.keySet());
    srcKeys.addAll(negMap.keySet());
    Iterator<String> skit = srcKeys.iterator();
    while (skit.hasNext()) {
      String srcID = skit.next();
      SortedMap<Double, List<String>> pMap = posMap.get(srcID);
      SortedMap<Double, SortedSet<DistanceRank>> pOrdered = (pMap == null) ? null : sortAMap(srcID, genome, lo, pMap, false);
      SortedMap<Double, List<String>> nMap = negMap.get(srcID);
      SortedMap<Double, SortedSet<DistanceRank>> nOrdered = (nMap == null) ? null : sortAMap(srcID, genome, lo, nMap, true);
      SortedSet<DistanceRank> merged = mergeMapsForSource(nOrdered, pOrdered);
      retval.put(srcID, merged);
    }  
    
    return (reorderByTargetCount(retval));
  }    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private class DistanceRank implements Comparable<DistanceRank> {
    
    String id;
    double distance;
        
    DistanceRank(String id, double distance) {
      this.id = id;
      this.distance = distance;
    }
    
    public String toString() {
      return ("DistRank: " + id + " " + distance);
    }
       
    public int compareTo(DistanceRank other) {
      double diff = this.distance - other.distance;
      if (diff < 0.0) {
        return (-1);
      } else if (diff > 0.0) {
        return (1);
      } else {
        return (this.id.compareTo(other.id));
      }
    }    
  }
}