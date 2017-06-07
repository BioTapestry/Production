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

package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.Rectangle;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.Set;

import java.util.SortedMap;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Used to automatically lay out links
*/

public class LinkModuleBoundaryBuilder {
  
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
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LinkModuleBoundaryBuilder() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return bounds required for the splice solution, on a per-module basis
  */
  
  public Map<String, Rectangle> spliceBoundsPerModule(Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> spliceSolution) {   
    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    Iterator<String> sskit = spliceSolution.keySet().iterator();
    while (sskit.hasNext()) {
      String modID = sskit.next();
      Map<Integer, LinkBundleSplicer.SpliceSolution> solPerBorder = spliceSolution.get(modID);
      retval.put(modID, spliceBoundsForAllBorders(solPerBorder));
    }    
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Return bounds required for all splice solutions on all borders
  */
  
  public Rectangle spliceBoundsForAllBorders(Map<Integer, LinkBundleSplicer.SpliceSolution> solPerBorder) {
    Rectangle spliceBoundsAllBoarders = null;
    Iterator<Integer> spbit = solPerBorder.keySet().iterator();
    while (spbit.hasNext()) {
      Integer borderObj = spbit.next();   
      LinkBundleSplicer.SpliceSolution sSol = solPerBorder.get(borderObj);
      Rectangle2D perBound = sSol.getBounds();
      Rectangle rfr2d = UiUtil.rectFromRect2D(perBound);
      if (rfr2d == null) {
        //
        // This arises from an inter-module link where the target module as absolutely no inbound links
        // from anywhere.
        //
        continue;
      }
      if (spliceBoundsAllBoarders == null) {
        spliceBoundsAllBoarders = rfr2d;
      } else {          
        Bounds.tweakBounds(spliceBoundsAllBoarders, rfr2d);
      }
    }  
    return (spliceBoundsAllBoarders);  
  }

  /***************************************************************************
  **
  ** Prep for bundle splicing for the non-overlay case
  */
  
  @SuppressWarnings("unused")
  public Map<Integer, LinkBundleSplicer.SpliceSolution> nonOverlaySplicePrep(SpecialtyInstructions spSrc, 
                                                                             List<Map<String, SpecialtyLayoutLinkData>> shiftedCopies, 
                                                                             Map<String, SpecialtyLayoutLinkData> alienSources, 
                                                                             List<Map<String, SpecialtyLayoutLinkData>> forRecoveriesOut) {    
          
    //
    // Border points for our layout:
    //
    
    HashMap<String, Point2D[]> borderPtsPerSrc = new HashMap<String, Point2D[]>();
    HashMap<String, List<Point2D>> leftAlignedPerSource = new HashMap<String, List<Point2D>>();
    
    Iterator<String> stiit = spSrc.getLinkSrcIterator();
    while (stiit.hasNext()) {
      String srcID = stiit.next();
      SpecialtyLayoutLinkData siSrc = spSrc.getLinkPointsForSrc(srcID);
      
      Point2D[] pts = extractBordersPerNonOverlaySource(siSrc);
      if (pts != null) {
        borderPtsPerSrc.put(srcID, pts);
      }      
      List<Point2D> aligned = siSrc.getLeftBorderAlignedPositions();
      if (aligned != null) {
        leftAlignedPerSource.put(srcID, aligned);
      }
    }
        
    //
    // Build external points.  Some are coming from sources outside the subset.  Some are going to
    // targets outside the subset.
    //
    
    TreeMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder = new TreeMap<Integer, LinkBundleSplicer.SpliceProblem>(new BorderComparator());
    
    Iterator<String> asit = alienSources.keySet().iterator();
    while (asit.hasNext()) {
      String srcID = asit.next();
      SpecialtyLayoutLinkData siTrg = spSrc.getLinkPointsForSrc(srcID);
      SpecialtyLayoutLinkData siSrc = alienSources.get(srcID);
      for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {
        buildNonOverlayBorderBundle(i, siSrc, siTrg, spPerBorder, true);
      }      
    }
    
    
    Map<String, SpecialtyLayoutLinkData> forRecovery = forRecoveriesOut.get(0);
    if (!forRecovery.isEmpty()) {
      Iterator<String> sit = forRecovery.keySet().iterator();
      while (sit.hasNext()) {
        String srcID = sit.next();
        SpecialtyLayoutLinkData si = spSrc.getLinkPointsForSrc(srcID);
        SpecialtyLayoutLinkData recSi = forRecovery.get(srcID);
        for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {
          buildNonOverlayBorderBundle(i, si, recSi, spPerBorder, false);
        }      
      }
    }
    
    //
    // Fill in remaining needed data and solve:
    //
      
    LinkBundleSplicer lbs = new LinkBundleSplicer();

    //
    // Have to install the border first, since the following loop modifies
    // the results as it goes
    //
  
    HashSet<Integer> skipIt = new HashSet<Integer>();
    Iterator<Integer> rpbit = spPerBorder.keySet().iterator();
    while (rpbit.hasNext()) {
      Integer borderObj = rpbit.next();
      LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);
      if (!installBorderToSpliceMapGuts(borderPtsPerSrc, leftAlignedPerSource, borderObj.intValue(), sp)) {
        skipIt.add(borderObj);
        continue;
      }
    }
  
    //
    // Solve, tweaking borders for following solutions as we go:
    //
    HashMap<Integer, LinkBundleSplicer.SpliceSolution> solnPerBorder = new HashMap<Integer, LinkBundleSplicer.SpliceSolution>();
    boolean multiBorderSoln = (spPerBorder.size() > 1);
    rpbit = spPerBorder.keySet().iterator();
    while (rpbit.hasNext()) {
      Integer borderObj = rpbit.next();
      LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);  
      LinkBundleSplicer.SpliceSolution sSol = lbs.spliceBundles(sp, multiBorderSoln);
      // This had been commented out, and is now restored.  Are we seeing any issues related to this restoration? 
      tweakPendingSpliceProblems(spPerBorder, borderObj, sSol);
      solnPerBorder.put(borderObj, sSol);      
    }   
   
    return (solnPerBorder);
  }
  
  /***************************************************************************
  **
  ** Build an external bundle for the non-overlay case
  */
  
  private boolean buildNonOverlayBorderBundle(int border, SpecialtyLayoutLinkData slldSrc, 
                                              SpecialtyLayoutLinkData slldTrg, 
                                              SortedMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder, 
                                              boolean srcIsAlien) { 
    
    Point2D pt = (srcIsAlien) ? slldSrc.extractForAlienSplice(slldTrg, border) : slldSrc.extractForNonSubSplice(slldTrg, border);
    if (pt != null) {
      LinkBundleSplicer.SpliceProblem sp = retrieveNonOverlaySP(border, spPerBorder);    
      boolean wantY = sp.wantY();
      String src = slldSrc.getSrcID();      
      int coord = (int)((wantY) ? pt.getY() : pt.getX());
      sp.putExternalBundle(coord / UiUtil.GRID_SIZE_INT, src);
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Prep for bundle splicing.
  */
  
  public Map<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> splicePrep(NetModuleLinkExtractor.SubsetAnalysis sa, 
                                                                                Map<String, NetModuleLinkExtractor.ExtractResultForSource> shiftedIMS, 
                                                                                List<Map<String, SpecialtyLayoutLinkData>> shiftedCopies) {
                  
    
    HashMap<String, SortedMap<Integer, LinkBundleSplicer.SpliceProblem>> problemSets = 
      new HashMap<String, SortedMap<Integer, LinkBundleSplicer.SpliceProblem>>();
 
    HashMap<String, Map<String, Point2D[]>> modToBorderPoints = new HashMap<String, Map<String, Point2D[]>>();
    HashMap<String, Map<String, List<Point2D>>> modToLeftAlignedPoints = new HashMap<String, Map<String, List<Point2D>>>();
 
    HashSet<String> allKeys = new HashSet<String>();
    HashMap<String, NetModuleLinkExtractor.ExtractResultForSource> resultsAsSource = 
      new HashMap<String, NetModuleLinkExtractor.ExtractResultForSource>();
    HashMap<String, List<NetModuleLinkExtractor.ExtractResultForTarg>> resultsAsTarget =
      new HashMap<String, List<NetModuleLinkExtractor.ExtractResultForTarg>>();
    
    Iterator<String> stiit = sa.getSrcKeys();
    while (stiit.hasNext()) {
      String srcID = stiit.next();
      NetModuleLinkExtractor.PrimaryAndOthers pao = sa.getModulesForSrcID(srcID);
      NetModuleLinkExtractor.ExtractResultForSource interModForSrc = null;
      if (pao.sourceInModule()) { 
        String modName = sa.getPrimaryModuleName(pao);
        if (modName != null) {
          interModForSrc = shiftedIMS.get(modName);
          resultsAsSource.put(modName, interModForSrc);
          allKeys.add(modName);
          Map<String, SpecialtyLayoutLinkData> srcCopyMap = shiftedCopies.get(sa.getMappedPrimaryModuleIndex(pao));
          SpecialtyLayoutLinkData siSrc = srcCopyMap.get(srcID);
          // Penultimate nodes in simple fan-ins do not show up in the srcCopyMap, so give null:
          if (siSrc != null) {
            extractBordersPerSource(siSrc, srcID, modName, modToBorderPoints);
            extractLeftBorderAlignedPositions(siSrc, srcID, modName, modToLeftAlignedPoints);
          }
        }
      }
 
      Iterator<Integer> oit = pao.getTargetModules();
      while (oit.hasNext()) {
        Integer other = oit.next();
        String modName = sa.getTargetModuleName(pao, other);
        NetModuleLinkExtractor.ExtractResultForTarg interModPath = (interModForSrc == null) ? null : 
          interModForSrc.getResultForTarg(modName);
        if (interModPath != null) {
          List<NetModuleLinkExtractor.ExtractResultForTarg> targs = resultsAsTarget.get(modName);
          if (targs == null) {
            targs = new ArrayList<NetModuleLinkExtractor.ExtractResultForTarg>();
            resultsAsTarget.put(modName, targs);
            allKeys.add(modName);
          }
          targs.add(interModPath);
        }      
        Map<String, SpecialtyLayoutLinkData> placeCopyMap = shiftedCopies.get(sa.getMappedTargetModuleIndex(pao, other));
        SpecialtyLayoutLinkData siPlace = placeCopyMap.get(srcID);
        // Penultimate nodes in simple fan-ins do not show up in the placeCopyMap, so give null:
        if (siPlace != null) {
          extractBordersPerSource(siPlace, srcID, modName, modToBorderPoints);
          extractLeftBorderAlignedPositions(siPlace, srcID, modName, modToLeftAlignedPoints);
        }
      }
    } 
  
    Iterator<String> akit = allKeys.iterator();
    while (akit.hasNext()) {
      String modName = akit.next();
      //
      // Handle source-related bundles:
      //
      
      NetModuleLinkExtractor.ExtractResultForSource interModForSrc = resultsAsSource.get(modName);
      if (interModForSrc != null) {
        HashMap<String, Map<String, Point2D>> pointData = new HashMap<String, Map<String, Point2D>>();
        HashMap<String, Vector2D> vectorData = new HashMap<String, Vector2D>();
        interModForSrc.buildDepartureData(pointData, vectorData);
        Iterator<String> pdit = pointData.keySet().iterator();
        while (pdit.hasNext()) {
          String treeID = pdit.next();          
          Vector2D dir = vectorData.get(treeID);          
          buildSourceBorderBundle(modName, dir, treeID, pointData, problemSets);
        }
      }
      
      //
      // Handle target-related bundles:
      //
      
      List<NetModuleLinkExtractor.ExtractResultForTarg> targs = resultsAsTarget.get(modName);
      if (targs != null) {
        Iterator<NetModuleLinkExtractor.ExtractResultForTarg> trit = targs.iterator();
        while (trit.hasNext()) {
          NetModuleLinkExtractor.ExtractResultForTarg interModForTarg = trit.next();
          Vector2D termDir = interModForTarg.getTerminalDirection();
          buildTargetBorderBundle(modName, termDir, interModForTarg, problemSets);   
        }
      }     
    }
    
    //
    // Fill in remaining needed data and solve:
    //
      
    HashMap<String, Map<Integer, LinkBundleSplicer.SpliceSolution>> retval = 
      new HashMap<String, Map<Integer, LinkBundleSplicer.SpliceSolution>>();
    LinkBundleSplicer lbs = new LinkBundleSplicer();
    Iterator<String> psmodit = problemSets.keySet().iterator();
    while (psmodit.hasNext()) {
      String modID = psmodit.next();
       SortedMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder = problemSets.get(modID);
      //
      // Have to install the border first, since the following loop modifies
      // the results as it goes
      //
      HashSet<Integer> skipIt = new HashSet<Integer>();
      Iterator<Integer> rpbit = spPerBorder.keySet().iterator();
      while (rpbit.hasNext()) {
        Integer borderObj = rpbit.next();
        LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);
        if (!installBorderToSpliceMaps(modToBorderPoints, modToLeftAlignedPoints, modID, borderObj.intValue(), sp)) {
          skipIt.add(borderObj);
          continue;
        }
      }
      //
      // Solve, tweaking borders for following solutions as we go:
      //
      rpbit = spPerBorder.keySet().iterator();
      boolean multiBorderSoln = (spPerBorder.size() > 1);  // conservative, since we ignore skipIt
      while (rpbit.hasNext()) {
        Integer borderObj = rpbit.next();
        if (skipIt.contains(borderObj)) {
          continue;
        }
        LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);  
        LinkBundleSplicer.SpliceSolution sSol = lbs.spliceBundles(sp, multiBorderSoln);
        tweakPendingSpliceProblems(spPerBorder, borderObj, sSol);        
        Map<Integer, LinkBundleSplicer.SpliceSolution> solPerBorder = retval.get(modID);
        if (solPerBorder == null) {
          solPerBorder = new HashMap<Integer, LinkBundleSplicer.SpliceSolution>();
          retval.put(modID, solPerBorder);
        }
        solPerBorder.put(borderObj, sSol);      
      }   
    }
   
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Gather up the border points
  */
  
  private void extractBordersPerSource(SpecialtyLayoutLinkData siSrc, String srcID, String modName, 
                                       Map<String, Map<String, Point2D[]>> modToBorderPoints) {
    for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {    
      Point2D borderPt = siSrc.getBorderPosition(i);  // these points are hugging the layout node & left intra-node link boundaries
      if (borderPt != null) {
        Map<String, Point2D[]> bordersPerSource = modToBorderPoints.get(modName);
        if (bordersPerSource == null) {
          bordersPerSource = new HashMap<String, Point2D[]>();
          modToBorderPoints.put(modName, bordersPerSource);
        }
        Point2D[] pts = bordersPerSource.get(srcID);
        if (pts == null) {
          pts = new Point2D[SpecialtyLayoutLinkData.NUM_BORDERS];
          bordersPerSource.put(srcID, pts);
        }
        pts[i] = borderPt;
      } 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Gather up the border points
  */
  
  private Point2D[] extractBordersPerNonOverlaySource(SpecialtyLayoutLinkData siSrc) {
    Point2D[] pts = null;
    for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {    
      Point2D borderPt = siSrc.getBorderPosition(i);  // these points are hugging the layout node & left intra-node link boundaries
      if (borderPt != null) {
        if (pts == null) {
          pts = new Point2D[SpecialtyLayoutLinkData.NUM_BORDERS];
        }
        pts[i] = borderPt;
      } 
    }
    return (pts);
  }

  /***************************************************************************
  **
  ** Gather up all the points on the left edge for the given source/module 
  */
  
  private void extractLeftBorderAlignedPositions(SpecialtyLayoutLinkData siSrc, String srcID, String modName, 
                                                 Map<String, Map<String, List<Point2D>>> modToLeftAlignedPoints) {
    List<Point2D> aligned = siSrc.getLeftBorderAlignedPositions();
    Map<String, List<Point2D>> alignedPerSource = modToLeftAlignedPoints.get(modName);
    if (alignedPerSource == null) {
      alignedPerSource = new HashMap<String, List<Point2D>>();
      modToLeftAlignedPoints.put(modName, alignedPerSource);
    }    
    alignedPerSource.put(srcID, aligned);
    return;
  }
  
  /***************************************************************************
  **
  ** Transfer border points into splice map setup
  */
  
  private boolean installBorderToSpliceMaps(Map<String, Map<String, Point2D[]>> modToBorderPoints, 
                                            Map<String, Map<String, List<Point2D>>> modToLeftAlignedPoints, 
                                            String modName, int border, LinkBundleSplicer.SpliceProblem sp) {
    Map<String, Point2D[]> bordersPerSource = modToBorderPoints.get(modName);
    Map<String, List<Point2D>> leftAlignedPerSource = (border == SpecialtyLayoutLinkData.LEFT_BORDER) ? modToLeftAlignedPoints.get(modName) : null;
    return (installBorderToSpliceMapGuts(bordersPerSource, leftAlignedPerSource, border, sp));
  }
   
  /***************************************************************************
  **
  ** Transfer border points into splice map setup
  */
  
  private boolean installBorderToSpliceMapGuts(Map<String, Point2D[]> bordersPerSource, 
                                               Map<String, List<Point2D>> leftAlignedPerSource, 
                                               int border, LinkBundleSplicer.SpliceProblem sp) {
    
    
    Iterator<String> sit = bordersPerSource.keySet().iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      Point2D[] pts = bordersPerSource.get(srcID);
      Point2D borderPt = pts[border];
      ArrayList<Integer> coords = null;
      if (border == SpecialtyLayoutLinkData.LEFT_BORDER) {
        coords = new ArrayList<Integer>();
        List<Point2D> aligned = leftAlignedPerSource.get(srcID);
        Iterator<Point2D> ait = aligned.iterator();
        while (ait.hasNext()) {
          Point2D alignPt = ait.next();
          coords.add(new Integer(((int)alignPt.getY()) / UiUtil.GRID_SIZE_INT));        
        }      
      }      
      
      if (borderPt == null) {
        return (false);
      }
      boolean wantY = sp.wantY();
      // The coords have been derived from node & (intra-node link) hugging layout boundaries
      // On left boundary, matches the link trace, on other borders, matches the single enclosing rect border value.
      // Yes, target trace gets the opposite of the coordinate sp is "wanting".  "Want" refers to internal track:
      int coord = (int)((wantY) ? borderPt.getX() : borderPt.getY());
      sp.putTargetTrace(srcID, coord / UiUtil.GRID_SIZE_INT);
      coord = (int)((wantY) ? borderPt.getY() : borderPt.getX());
      sp.setInternalTrack(srcID, coord / UiUtil.GRID_SIZE_INT);
      if (coords != null) {
        sp.setAdditionalInternalTracks(srcID, coords);
      }
    }
    
    //
    // Whoops! The above code implicitly assumes that the only internal tracks
    // we care about on the left border belong to sources traversing the border.
    // Not so!  We need to account for ALL tracks, even those only used internally!
    //
   
    if (border == SpecialtyLayoutLinkData.LEFT_BORDER) { 
      HashSet<String> seenSrc = new HashSet<String>(bordersPerSource.keySet());
      Iterator<String> lait = leftAlignedPerSource.keySet().iterator();
      while (lait.hasNext()) {
        String srcID = lait.next();
        if (seenSrc.contains(srcID)) {
          continue;          
        }
        ArrayList<Integer> coords = new ArrayList<Integer>();
        List<Point2D> aligned = leftAlignedPerSource.get(srcID);
        Iterator<Point2D> ait = aligned.iterator();
        boolean isFirst = true;
        while (ait.hasNext()) {
          Point2D alignPt = ait.next();
          if (isFirst) {
            sp.putTargetTrace(srcID, (int)alignPt.getX() / UiUtil.GRID_SIZE_INT);
            isFirst = false;
          }
          coords.add(new Integer(((int)alignPt.getY()) / UiUtil.GRID_SIZE_INT));        
        }
        sp.setAdditionalInternalTracks(srcID, coords);   
      }    
    }
    return (true);
  }
    
  /***************************************************************************
  **
  ** Retrieve SpliceProblem for a given module & border
  */
  
  private LinkBundleSplicer.SpliceProblem retrieveSP(String modName, int border, 
                                                     Map<String, SortedMap<Integer, LinkBundleSplicer.SpliceProblem>> problemSets) { 
    Integer borderObj = new Integer(border);
    SortedMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder = problemSets.get(modName);
    if (spPerBorder == null) {
      spPerBorder = new TreeMap<Integer, LinkBundleSplicer.SpliceProblem>(new BorderComparator());
      problemSets.put(modName, spPerBorder);     
    }
    LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);
    if (sp == null) {
      if ((border == SpecialtyLayoutLinkData.LEFT_BORDER)) {
        sp = new LinkBundleSplicer.RightAngleProblem(SpecialtyLayoutLinkData.borderToSpSide(border));
      } else {
        sp = new LinkBundleSplicer.ButtEndProblem(SpecialtyLayoutLinkData.borderToSpSide(border));
      }
      spPerBorder.put(borderObj, sp);     
    }
    return (sp);
  }  
  
   /***************************************************************************
  **
  ** Retrieve SpliceProblem for border of a non-overlay case:
  */
  
  private LinkBundleSplicer.SpliceProblem retrieveNonOverlaySP(int border, SortedMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder) { 
    Integer borderObj = new Integer(border);

    LinkBundleSplicer.SpliceProblem sp = spPerBorder.get(borderObj);
    if (sp == null) {
      if ((border == SpecialtyLayoutLinkData.LEFT_BORDER)) {
        sp = new LinkBundleSplicer.RightAngleProblem(SpecialtyLayoutLinkData.borderToSpSide(border));
      } else {
        sp = new LinkBundleSplicer.ButtEndProblem(SpecialtyLayoutLinkData.borderToSpSide(border));
      }
      spPerBorder.put(borderObj, sp);     
    }
    return (sp);
  }  
 
  /***************************************************************************
  **
  ** Build an external bundle for a given border for tree sources
  */
  
  private void buildSourceBorderBundle(String modName, Vector2D dir, String treeID, 
                                       Map<String, Map<String, Point2D>> pointData, 
                                       Map<String, SortedMap<Integer, LinkBundleSplicer.SpliceProblem>> problemSets) { 

    int border = SpecialtyLayoutLinkData.vecToBorder(dir);  
    LinkBundleSplicer.SpliceProblem sp = retrieveSP(modName, border, problemSets);
    boolean wantY = sp.wantY();
    
    Map<String, Point2D> pts = pointData.get(treeID);
    Iterator<String> pit = pts.keySet().iterator();
    while (pit.hasNext()) {
      String src = pit.next();
      Point2D pt = pts.get(src);
      int coord = (int)((wantY) ? pt.getY() : pt.getX());
      sp.putExternalBundle(coord / UiUtil.GRID_SIZE_INT, src);
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Build a target external bundle for a given border
  */
  
  private void buildTargetBorderBundle(String modName, Vector2D termDir, 
                                       NetModuleLinkExtractor.ExtractResultForTarg interModForTarg, 
                                       Map<String, SortedMap<Integer, LinkBundleSplicer.SpliceProblem>> problemSets) { 
    
    int border = SpecialtyLayoutLinkData.vecToBorder(termDir);  
    LinkBundleSplicer.SpliceProblem sp = retrieveSP(modName, border, problemSets);
    boolean wantY = sp.wantY();
    
    Set<String> srcs = interModForTarg.getSourcesToModule();
    Iterator<String> sit = srcs.iterator();
    while (sit.hasNext()) {
      String srcID = sit.next();
      // these points are tracking the module boundaries....
      // But note we do not care about the _perpendicular_ location (i.e. the location of module boundary), correct? 
      Point2D pt = interModForTarg.getArrivalPoint(srcID);  
      int coord = (int)((wantY) ? pt.getY() : pt.getX());
      sp.putExternalBundle(coord / UiUtil.GRID_SIZE_INT, srcID);
    }   
    return;
  }
 
  /***************************************************************************
  **
  ** A border splice can affect subsequent splices; handle it.
  */
  
  private void tweakPendingSpliceProblems(SortedMap<Integer, LinkBundleSplicer.SpliceProblem> spPerBorder, Integer borderObj, 
                                          LinkBundleSplicer.SpliceSolution sSol) {
    
    
    //
    // Position our iterator to look at only downstream splices:
    //
    
    Iterator<Integer> rpbit = spPerBorder.keySet().iterator();
    while (rpbit.hasNext()) {
      Integer pasttBorderObj = rpbit.next();
      if (pasttBorderObj.equals(borderObj)) {
        break;
      }
    }
    
    Rectangle2D growth = sSol.getBounds();
    
    //
    // OK, apply any splice-induced growth on downstream splices that care:
    //
    
    while (rpbit.hasNext()) {
      Integer futureBorderObj = rpbit.next();
      if (growth != null) {  // no-link modules are null...
        LinkBundleSplicer.SpliceProblem futureSp = spPerBorder.get(futureBorderObj);
        futureSp.updateInterfaceCoord(growth);
      }
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Want to solve left and right borders, then the top and bottom.
  ** This way, any boundary expansion caused by the first can be
  ** transmitted to the second.
  */
  
  public static class BorderComparator implements Comparator<Integer> {
  
    private static final HashMap<Integer, Integer> myOrder_;
   
    static {
      myOrder_ = new HashMap<Integer, Integer>();
      myOrder_.put(new Integer(SpecialtyLayoutLinkData.LEFT_BORDER), new Integer(0));
      myOrder_.put(new Integer(SpecialtyLayoutLinkData.RIGHT_BORDER), new Integer(1));
      myOrder_.put(new Integer(SpecialtyLayoutLinkData.TOP_BORDER), new Integer(2));
      myOrder_.put(new Integer(SpecialtyLayoutLinkData.BOTTOM_BORDER), new Integer(3));    
    }
       
    public BorderComparator() {
    } 
    
    public int compare(Integer border1, Integer border2) { 
      Integer rank1 = myOrder_.get(border1);
      Integer rank2 = myOrder_.get(border2);     
      return (rank1.compareTo(rank2));
    }
  }
}