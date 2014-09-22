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

package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
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

import org.systemsbiology.biotapestry.analysis.CenteredGridElement;
import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.analysis.GridGrower;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.LinkBusDrop;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleBusDrop;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.PointAndVec;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Utility for extracting link plans from the links between modules
** in an overlay.
*/

public class NetModuleLinkExtractor {
  
  public NetModuleLinkExtractor() {
  }  
  
  /***************************************************************************
  **
  ** Extract
  */
  
  public Map<String, ExtractResultForSource> extract(DataAccessContext icx, String overlayKey, List<GenomeSubset> subsetList) {
    HashMap<String, ExtractResultForSource> retval = new HashMap<String, ExtractResultForSource>();
    String genomeKey = icx.getGenomeID();
    NetOverlayOwner owner = icx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey);
    NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
    NetOverlayProperties noProps = icx.getLayout().getNetOverlayProperties(overlayKey);
    Iterator<String> lpkit = noProps.getNetModuleLinkagePropertiesKeys();
    HashSet<Point2D> seenPts = new HashSet<Point2D>();
    while (lpkit.hasNext()) {
      String treeID = lpkit.next();
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
      String srcMod = nmlp.getSourceTag();
      ExtractResultForSource er4s = retval.get(srcMod);
      if (er4s == null) {
        er4s = new ExtractResultForSource(srcMod);
        retval.put(srcMod, er4s);
      }

      //
      // Want to track the target modules supported by each link segment:
      //
      
      List<String> allLinks = nmlp.getLinkageList();
      HashMap<String, String> linkToTarget = new HashMap<String, String>();
      int numL = allLinks.size();
      for (int i = 0; i < numL; i++) {
        String linkID = allLinks.get(i);
        NetModuleLinkage nml = nov.getLinkage(linkID);
        linkToTarget.put(linkID, nml.getTarget());
      }

      Map<LinkSegmentID, LinkSegment> segGeoms = nmlp.getAllSegmentGeometries(icx, true);
      NetModuleBusDrop rootDrop = (NetModuleBusDrop)nmlp.getRootDrop();
      Vector2D startDir = new Vector2D(rootDrop.getEnd(0.0), rootDrop.getEnd(1.0));
      Iterator<LinkBusDrop> dit = nmlp.getDrops();
      while (dit.hasNext()) {
        NetModuleBusDrop drop = (NetModuleBusDrop)dit.next();
        if (drop.getDropType() != LinkBusDrop.END_DROP) {
          continue;
        }
        String tRef = drop.getTargetRef();
        NetModuleLinkage nlm = nov.getLinkage(tRef);
        String trgMod = nlm.getTarget();
        ExtractResultForTarg extRes = er4s.getOrPrepResultForTarg(trgMod, treeID);
        if (extRes == null) {
          Vector2D endDir = new Vector2D(drop.getEnd(0.0), drop.getEnd(1.0));
          extRes = new ExtractResultForTarg(trgMod, startDir, endDir);
          er4s.addResultForTarg(trgMod, extRes);
        }
        List<LinkSegmentID> segsToRoot = nmlp.getSegmentIDsToRootForEndDrop(drop);
        int nums2r = segsToRoot.size();
        for (int i = 0; i < nums2r; i++) {
          boolean lastEntry = (i == (nums2r - 1));
          LinkSegmentID lsid = segsToRoot.get(i);
          Set<String> linksThru = nmlp.resolveLinkagesThroughSegment(lsid);
          Iterator<String> ltit = linksThru.iterator();
          HashSet<String> targsThru = new HashSet<String>();
          while (ltit.hasNext()) {
            String linkID = ltit.next();
            String targMod = linkToTarget.get(linkID);
            targsThru.add(targMod);
          }
          //
          // These get added for the end point, with a final
          // entry made for the root:
          //
          extRes.addToTargList(targsThru);
          if (lastEntry) {  // last entry, i.e. root
            extRes.addToTargList(new HashSet<String>(linkToTarget.values()));
          }
 
          // NOTE! Fake geometries for module drops have off-grid endpoints
          // generated for appearance.  DO NOT USE the fake geometry endpoint on the
          // module launch/land!
          LinkSegment fake = segGeoms.get(lsid);
          if (i == 0) {
            Point2D cloneEnd = (Point2D)drop.getEnd(0.0).clone();
            UiUtil.forceToGrid(cloneEnd, UiUtil.GRID_SIZE);
            extRes.addPoint(new PointAndVec(cloneEnd, fake.getRun()), targsThru, false); // force not to flatten
            Point2D key = (Point2D)cloneEnd.clone();
            if (!seenPts.contains(key)) {
              extRes.addElement(new CenteredGridElement(cloneEnd));
              seenPts.add(key);
            }
          }
          
          //
          // Figure out if our targets are taking a right turn:
          //
          
          boolean orthoParent = false;
          Vector2D prun = null;
          if (!lastEntry) {
            LinkSegmentID psid = segsToRoot.get(i + 1);
            LinkSegment fakep = segGeoms.get(psid);
            int myRunCanon = fake.getRun().canonicalDir();
            int parRunCanon = fakep.getRun().canonicalDir();
            prun = fakep.getRun();
            orthoParent = (myRunCanon != Vector2D.NOT_CANONICAL) && 
                          (parRunCanon != Vector2D.NOT_CANONICAL) && 
                          Vector2D.canonicalsAreOrtho(myRunCanon, parRunCanon);
          }

          Point2D cloneStrt = (lastEntry) ? (Point2D)rootDrop.getEnd(0.0).clone() : (Point2D)fake.getStart().clone();
          UiUtil.forceToGrid(cloneStrt, UiUtil.GRID_SIZE);
          extRes.addPoint(new PointAndVec(cloneStrt, prun), targsThru, orthoParent);
          Point2D key = (Point2D)cloneStrt.clone();
          if (!seenPts.contains(key)) {
            extRes.addElement(new CenteredGridElement(cloneStrt));
            seenPts.add(key);
          }
        }
      }
    }
    
    //
    // Reverse lists:
    //
    
    Iterator<ExtractResultForSource> rvit = retval.values().iterator();
    while (rvit.hasNext()) {
      ExtractResultForSource er4s = rvit.next();
      er4s.reverse();   
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Analyze subsets
  */
  
  public SubsetAnalysis analyzeForMods(List<GenomeSubset> subsetList, // of GenomeSubset
                                       Map<String, ExtractResultForSource> interModPaths) {

    int numSub = subsetList.size();   
    if (numSub == 0) {
      return (null);
    }
    
    SubsetAnalysis sa = new SubsetAnalysis(interModPaths);
    
    //
    // Needed to easily interpret the inter-module path map:
    //
    
    for (int i = 0; i < numSub; i++) {
      GenomeSubset subset = subsetList.get(i);
      String modName = subset.getModuleID();
      if (modName != null) {
        sa.mapIndexToModName(i, modName);
      }
    }
     
    //
    // OK, for each source, figure out which GenomeSubset (if any) contains
    // it.  That is the guy who is laid out first.  Then, following subsets
    // are glued on.  If we have recovery geometry for the links between, 
    // we use that.
    //   
   
    for (int i = 0; i < numSub; i++) {
      GenomeSubset subset = subsetList.get(i);
      HashSet<String> allSrc = new HashSet<String>();
      allSrc.addAll(subset.getSourcesHeadedOut());
      allSrc.addAll(subset.getSourcesForInternal());
      Iterator<String> asit = allSrc.iterator();
      while (asit.hasNext()) {
        String srcID = asit.next();
        PrimaryAndOthers pao = new PrimaryAndOthers(srcID, i);   
        sa.mapSourceToModules(srcID, pao);       
      }
    }
    
    //
    // This is to fill in the other subsets that are targeted, as well as
    // inbound links from sources not in a subset.
    //
    for (int i = 0; i < numSub; i++) {
      GenomeSubset subset = subsetList.get(i);
      Set<String> srcsIn = subset.getSourcesHeadedIn();
      Iterator<String> sit = srcsIn.iterator();
      while (sit.hasNext()) {
        String srcID = sit.next();
        PrimaryAndOthers pao = sa.getModulesForSrcID(srcID);
        if (pao == null) {
          pao = new PrimaryAndOthers(srcID, Integer.MIN_VALUE);
          pao.addOtherCode(i);
          sa.mapSourceToModules(srcID, pao);
        } else if (pao.getPrimCode() != i) {
          pao.addOtherCode(i);
        }
      }
    }
    
    sa.allocateInterModulePaths();
        
    //
    // Derive a partial ordering of the modules so we can assign
    // source order in a rational fashion:
    //
  
    sa.generatePartialOrder();
       
    //
    // Inbound ordering for a module is best when we combine inbounds while
    // taking the inbound paths order into account:
    //
    
    sa.generateBorderOrderedInbounds();
 
    return (sa);
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Used to return results
  */
  
  public static class ExtractResultForSource implements Cloneable {

    private HashMap<String, ExtractResultForTree> resultsByTrees_;
    private HashMap<String, String> trgToTree_;
    //private String srcMod_;
  
    ExtractResultForSource(String srcMod) {
      resultsByTrees_ = new HashMap<String, ExtractResultForTree>();
      trgToTree_ = new HashMap<String, String>();
      //srcMod_ = srcMod;
    }
    
    public ExtractResultForSource clone() { 
      try {
        ExtractResultForSource retval = (ExtractResultForSource)super.clone();
        
        retval.resultsByTrees_ = new HashMap<String, ExtractResultForTree>();
        Iterator<String> rbtit = resultsByTrees_.keySet().iterator();  
        while (rbtit.hasNext()) {
          String treeID = rbtit.next();
          ExtractResultForTree er4t = resultsByTrees_.get(treeID);
          ExtractResultForTree retEr4t = er4t.clone();
          retval.resultsByTrees_.put(treeID, retEr4t);
        }
         
        // String->String, shallow:
        retval.trgToTree_ = new HashMap<String, String>(this.trgToTree_);
 
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    //
    // Get the target modules, in the form of a map of targets to
    // link counts.  Link counts can be used to score inter-module
    // links
    
    
    public Map<String, Integer> getTargetMods() {
      HashMap<String, Integer> retval = new HashMap<String, Integer>();
      Iterator<String> mtit = trgToTree_.keySet().iterator();
      while (mtit.hasNext()) {
        String trgMod = mtit.next();
        String useTree = trgToTree_.get(trgMod);
        ExtractResultForTree er4t = resultsByTrees_.get(useTree);  
        Set<String> s2m = er4t.getSourcesToModule(trgMod);
        retval.put(trgMod, new Integer(s2m.size()));
      }
      return (retval);
    }  
               
    public ExtractResultForTarg getResultForTarg(String trgMod) {
      String useTree = trgToTree_.get(trgMod);
      ExtractResultForTree er4t = resultsByTrees_.get(useTree);
      return (er4t.getResultForTarg(trgMod));
    } 
    
    public Map<String, Set<Vector2D>> getAllDepartures() {
      HashMap<String, Set<Vector2D>> retval = new HashMap<String, Set<Vector2D>>();
      Iterator<ExtractResultForTree> rbtit = resultsByTrees_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTree er4t = rbtit.next();
        Set<String> as = er4t.getAllSources();
        Vector2D depDir = new Vector2D(er4t.getDepartureDir());
        Iterator<String> asit = as.iterator();
        while (asit.hasNext()) {
          String src = asit.next();
          Set<Vector2D> all4src = retval.get(src);
          if (all4src == null) {
            all4src = new HashSet<Vector2D>();
            retval.put(src, all4src);
          }
          all4src.add(depDir);          
        }
      }     
      return (retval);
    } 
    
    public Map<String, Vector2D> getArrivalDirs(String trgMod) {
      String useTree = trgToTree_.get(trgMod);
      if (useTree == null) {
        return (null);
      }
      ExtractResultForTree er4t = resultsByTrees_.get(useTree);

      HashMap<String, Vector2D> retval = new HashMap<String, Vector2D>();      
      Vector2D arr = new Vector2D(er4t.getArrivalDir(trgMod));
      Set<String> s2m = er4t.getSourcesToModule(trgMod);
      Iterator<String> s2mit = s2m.iterator();
      while (s2mit.hasNext()) {
        String src = s2mit.next();
        retval.put(src, arr);
      }
      return (retval);
    }

    ExtractResultForTarg getOrPrepResultForTarg(String trgMod, String treeID) {
      String useTree = trgToTree_.get(trgMod);
      ExtractResultForTree er4t;
      if (useTree == null) {
        er4t = resultsByTrees_.get(treeID);
        if (er4t == null) {
          er4t = new ExtractResultForTree(treeID);
          resultsByTrees_.put(er4t.getTreeID(), er4t);
        }
        trgToTree_.put(trgMod, treeID);
      } else if (!useTree.equals(treeID)) {
        throw new IllegalStateException();
      } else {
        er4t = resultsByTrees_.get(useTree);
      }
      return (er4t.getResultForTarg(trgMod));
    } 
    
    void addResultForTarg(String trgMod, ExtractResultForTarg er4g) {    
      String treeID = trgToTree_.get(trgMod);
      ExtractResultForTree er4t = resultsByTrees_.get(treeID);
      er4t.addResultForTarg(er4g);
      return;
    }
    
    void reverse() {    
      Iterator<ExtractResultForTree> rbtit = resultsByTrees_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTree er4t = rbtit.next();
        er4t.reverse();
      }     
      return;
    }
       
    //
    // Given that the source needs to get to the given module, we bump
    // the source sets associated with each point:
    //
    
    void sourceGoesToModule(String src, String module) {
      String useTree = trgToTree_.get(module);
      if (useTree == null) {
        return;
      }
      ExtractResultForTree er4t = resultsByTrees_.get(useTree);
      er4t.sourceGoesToModule(src, module);
      return;
    } 
    
    public void setSourceOrder(SortedMap<Integer, String> sourceOrder) {
      Iterator<ExtractResultForTree> rbtit = resultsByTrees_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTree er4t = rbtit.next();
        er4t.setSourceOrder(sourceOrder);
      }     
      return;
    } 
       
    public void collectGridElements(Set<Point2D> cpIDs, List<GridGrower.GeneralizedGridElement> gridElements, 
                                    Map<String, Map<Point2D, Dimension[]>> modMap) {
      Iterator<ExtractResultForTree> rbtit = resultsByTrees_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTree er4t = rbtit.next();
        er4t.collectGridElements(cpIDs, gridElements, modMap);
      }     
      return;
    } 
    
    public void shiftPaths(Map<Point2D, Point2D> movedPts) {
      Iterator<ExtractResultForTree> rbtit = resultsByTrees_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTree er4t = rbtit.next();
        er4t.shiftPaths(movedPts);
      }     
      return;
    }
    
    public void buildMultiDataMap(Map<String, Map<Point2D, Map<String, Point>>> multiDataMap) {
      Iterator<String> rbtit = resultsByTrees_.keySet().iterator();
      while (rbtit.hasNext()) {
        String treeID = rbtit.next();       
        ExtractResultForTree er4t = resultsByTrees_.get(treeID);
        HashMap<Point2D, Map<String, Point>> myResult = new HashMap<Point2D, Map<String, Point>>();
        multiDataMap.put(treeID, myResult);
        er4t.addToMultiDataMap(myResult);
      }     
      return;
    }
    
    public void buildDepartureData(Map<String, Map<String, Point2D>> pointData, Map<String, Vector2D> vectorData) {
      Iterator<String> rbtit = resultsByTrees_.keySet().iterator();
      while (rbtit.hasNext()) {
        String treeID = rbtit.next();       
        ExtractResultForTree er4t = resultsByTrees_.get(treeID);
        Vector2D depDir = er4t.getDepartureDir();
        vectorData.put(treeID, depDir);
        Map<String, Point2D> depPts = er4t.getDeparturePoints();
        pointData.put(treeID, depPts);
      }     
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Used to return results
  */
  
  static class ExtractResultForTree implements Cloneable {
    
    private HashMap<String, ExtractResultForTarg> resultsByTarget_;
    private String treeID_;
    private TreeMap<Integer, String> globalTrackAssignment_;
    private HashMap<Point2D, Set<String>> globalOrthoOutboundMods_;
    private HashMap<Point2D, Set<String>> globalOrthoOutboundSrcs_;
    private HashMap<Point2D, Set<String>> globalInboundSrcs_;
    private HashMap<String, Integer> globalSrcOrder_;
    private HashSet<String> globalTreeSrcs_;

           
    ExtractResultForTree(String treeID) {
      resultsByTarget_ = new HashMap<String, ExtractResultForTarg>();
      treeID_ = treeID;
      globalTrackAssignment_ = null;
      globalOrthoOutboundMods_= new HashMap<Point2D, Set<String>>();
      globalOrthoOutboundSrcs_ = new HashMap<Point2D, Set<String>>();
      globalInboundSrcs_ = new HashMap<Point2D, Set<String>>();
      globalSrcOrder_ = null;
      globalTreeSrcs_ = new HashSet<String>();
      
    }
    
    public ExtractResultForTree clone() { 
      try {
        ExtractResultForTree retval = (ExtractResultForTree)super.clone();
        
        retval.resultsByTarget_ = new HashMap<String, ExtractResultForTarg>();
        Iterator<String> rbtit = resultsByTarget_.keySet().iterator();  
        while (rbtit.hasNext()) {
          String targID = rbtit.next();
          ExtractResultForTarg er4g = resultsByTarget_.get(targID);
          ExtractResultForTarg retEr4g = er4g.clone();
          retEr4g.shareTrackAssignment(retval);
          retval.resultsByTarget_.put(targID, retEr4g);
        }
  
        retval.globalTrackAssignment_ = (this.globalTrackAssignment_ == null) ? null : new TreeMap<Integer, String>(this.globalTrackAssignment_);  // Integers->strings, shallow
        
        retval.globalOrthoOutboundMods_= new HashMap<Point2D, Set<String>>();
        Iterator<Point2D> goomit = this.globalOrthoOutboundMods_.keySet().iterator();  
        while (goomit.hasNext()) {
          Point2D key = goomit.next();
          Set<String> forOrth = this.globalOrthoOutboundMods_.get(key);
          retval.globalOrthoOutboundMods_.put((Point2D)key.clone(), new HashSet<String>(forOrth));
        }
        
        retval.globalOrthoOutboundSrcs_ = new HashMap<Point2D, Set<String>>();
        Iterator<Point2D> goosit = this.globalOrthoOutboundSrcs_.keySet().iterator();  
        while (goosit.hasNext()) {
          Point2D key = goosit.next();
          Set<String> forOrth = this.globalOrthoOutboundSrcs_.get(key);
          retval.globalOrthoOutboundSrcs_.put((Point2D)key.clone(), new HashSet<String>(forOrth));
        }
               
        retval.globalInboundSrcs_ = new HashMap<Point2D, Set<String>>();
        Iterator<Point2D> gisit = this.globalInboundSrcs_.keySet().iterator();  
        while (gisit.hasNext()) {
          Point2D key = gisit.next();
          Set<String> forSrc = this.globalInboundSrcs_.get(key);
          retval.globalInboundSrcs_.put((Point2D)key.clone(), new HashSet<String>(forSrc));
        }
    
        // Shallow, String->Int
        retval.globalSrcOrder_ = (this.globalSrcOrder_ == null) ? null : new HashMap<String, Integer>(this.globalSrcOrder_);
        
       // Shallow, Strings
        retval.globalTreeSrcs_ = new HashSet<String>(this.globalTreeSrcs_);      
  
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
       
    String getTreeID() {
      return (treeID_);
    }
    
    void setSourceOrder(SortedMap<Integer, String> sourceOrder) {
      globalTrackAssignment_ = new TreeMap<Integer, String>(sourceOrder);
      return;
    } 

    Map<String, Point2D> getDeparturePoints() {
      HashMap<String, Point2D> retval = new HashMap<String, Point2D>();    
      HashSet<String> needed = new HashSet<String>(globalTrackAssignment_.values());
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        Iterator<String> s2mit = er4g.getSourcesToModule().iterator();
        while (s2mit.hasNext()) {
          String srcID = s2mit.next();
          if (!needed.contains(srcID)) {
            continue;
          }
          Point2D ppt = er4g.getDeparturePoint(srcID);
          retval.put(srcID, ppt);
          needed.remove(srcID);
        }
      }
      return (retval);
    }

    Vector2D getDepartureDir() {
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      Vector2D retval = null;
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        Vector2D sd = er4g.getStartDirection();
        if (retval == null) {
          retval = sd.clone();
        } else if (!retval.equals(sd)) { // Just a sanity check...should all match!
          throw new IllegalStateException();
        }
      }     
      return (retval);
    }
    
    Vector2D getArrivalDir(String trgID) {
      ExtractResultForTarg er4g = resultsByTarget_.get(trgID);
      return (er4g.getTerminalDirection());
    } 
    
    Set<String> getAllSources() {
      return (getGlobalSourceOrder().keySet());
    } 
      
    Map<Point2D, Set<String>> getGlobalOrthoOutboundMods() {
      return (globalOrthoOutboundMods_);
    } 
    
    Map<Point2D, Set<String>> getGlobalOrthoOutboundSrcs() {
      return (globalOrthoOutboundSrcs_);
    } 
    
    Set<String> getGlobalTreeSrcs() {
      return (globalTreeSrcs_);
    } 

    Set<String> getSourcesToModule(String trgID) {
      ExtractResultForTarg er4g = resultsByTarget_.get(trgID);
      return (er4g.getSourcesToModule());
    }   
       
    Set<String> getInboundSourcesForPoint(Point2D pt) {
      Set<String> srcs = globalInboundSrcs_.get(pt);
      if (srcs == null) {            
        srcs = new HashSet<String>();
        globalInboundSrcs_.put(pt, srcs);
      }      
      return (srcs);
    }
    
    Map<String, Integer> getGlobalSourceOrder() {
      if (globalTrackAssignment_ == null) {
        throw new IllegalStateException();
      }
      if (globalSrcOrder_ == null) {
        globalSrcOrder_ = new HashMap<String, Integer>();
        Iterator<String> sit = globalTrackAssignment_.values().iterator();
        int size = globalTreeSrcs_.size(); // Based only on links actually in tree!
        int count = -size / 2;
        while (sit.hasNext()) {
          String srcID = sit.next();
          if (globalTreeSrcs_.contains(srcID)) {
            globalSrcOrder_.put(srcID, new Integer(count++));
          }     
        }
      }
      return (globalSrcOrder_);
    }
     
    ExtractResultForTarg getResultForTarg(String trgID) {
      ExtractResultForTarg er4g = resultsByTarget_.get(trgID);
      return (er4g);
    } 

    void addResultForTarg(ExtractResultForTarg er4t) {
      er4t.shareTrackAssignment(this);
      resultsByTarget_.put(er4t.getTrgID(), er4t);
      return;
    }  
       
    void sourceGoesToModule(String src, String trgID) {
      ExtractResultForTarg er4g = resultsByTarget_.get(trgID);
      er4g.sourceGoesToModule(src);
      return;
    } 
    
    void reverse() {    
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        er4g.reverse();
      }     
      return;
    }
    
    void collectGridElements(Set<Point2D> cpIDs, List<GridGrower.GeneralizedGridElement> gridElements, 
                             Map<String, Map<Point2D, Dimension[]>> modMap) {    
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        er4g.collectGridElements(cpIDs, gridElements, modMap);
      }     
      return;
    } 
    
    void shiftPaths(Map<Point2D, Point2D> movedPts) {
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        er4g.shiftPaths(movedPts);
      }     
      return;
    }
    
    void addToMultiDataMap(Map<Point2D, Map<String, Point>> multiDataMap) {
      Iterator<ExtractResultForTarg> rbtit = resultsByTarget_.values().iterator();
      while (rbtit.hasNext()) {
        ExtractResultForTarg er4g = rbtit.next();
        er4g.addToMultiDataMap(multiDataMap);
      }     
      return;            
    }    
  }
  
  /***************************************************************************
  **
  ** Used to return results
  */
  
  public static class ExtractResultForTarg implements Cloneable {
    private ArrayList<PointAndVec> pointList_;
    private ArrayList<CenteredGridElement> elemList_;
    private ArrayList<Set<String>> targList_;
    private Vector2D endDir_;
    private Vector2D startDir_;
    private String trgMod_;
    private ExtractResultForTree myTree_;
    private HashMap<Point2D, Point2D> shifted_;
    private HashSet<String> inboundSrcs_;
    
    ExtractResultForTarg(String trgMod, Vector2D startDir, Vector2D endDir) {
      pointList_ = new ArrayList<PointAndVec>();
      elemList_ = new ArrayList<CenteredGridElement>();
      targList_ = new ArrayList<Set<String>>();
      endDir_ = endDir;
      startDir_ = startDir;
      trgMod_ = trgMod;
      shifted_ = new HashMap<Point2D, Point2D>();
      inboundSrcs_ = new HashSet<String>();
    }
    
    @Override
    public ExtractResultForTarg clone() { 
      try {
        ExtractResultForTarg retval = (ExtractResultForTarg)super.clone();
        
        retval.pointList_ = new ArrayList<PointAndVec>();
        int numPL = this.pointList_.size();
        for (int i = 0; i < numPL; i++) {        
          PointAndVec pAndV = this.pointList_.get(i);
          retval.pointList_.add(pAndV.clone());
        }
        
        //
        // Can't clone...has generic object key which is in this case a point!
        //
        retval.elemList_ = new ArrayList<CenteredGridElement>();
        int numEL = this.elemList_.size();
        for (int i = 0; i < numEL; i++) {        
          CenteredGridElement ge = this.elemList_.get(i);
          CenteredGridElement retGe = new CenteredGridElement(ge);
          retGe.setID(ge.getID());
          retval.elemList_.add(retGe);
        }

        retval.targList_ = new ArrayList<Set<String>>();
        int numTL = this.targList_.size();
        for (int i = 0; i < numTL; i++) {        
          Set<String> trgs = this.targList_.get(i);
          HashSet<String> retTrgs = new HashSet<String>(trgs);  // Shallow...set of strings
          retval.targList_.add(retTrgs);
        }
       
        retval.endDir_ = this.endDir_.clone();
        retval.startDir_ = this.startDir_.clone();
        
        //retval.myTree_ WARNING! GOTTA REPLACE THIS AFTER CLONE! (shareTrackAssignment())
        
        retval.shifted_ = new HashMap<Point2D, Point2D>();
        Iterator<Point2D> skit = this.shifted_.keySet().iterator();
        while (skit.hasNext()) {
          Point2D key = skit.next();
          Point2D val = this.shifted_.get(key);
          retval.shifted_.put((Point2D)key.clone(), (Point2D)val.clone());         
        }       
        retval.inboundSrcs_ = new HashSet<String>(this.inboundSrcs_); // shallow for strings...
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }

    String getTrgID() {
      return (trgMod_);
    }
        
    void shareTrackAssignment(ExtractResultForTree myTree) {
      myTree_ = myTree;
      return;
    }
    
    public Vector2D getTerminalDirection() {
      return (endDir_);
    }
    
    public Vector2D getStartDirection() {
      return (startDir_);
    }
          
    public int size() {
      return (pointList_.size());
    }   
    
    public int numElem() {
      return (elemList_.size());
    }  
    
    public CenteredGridElement getElem(int i) {
      return (elemList_.get(i));
    }
    
    Point2D getOriginalCorePoint(int i) {
      return (pointList_.get(i).origin);
    }  
    
    Point2D getCorePoint(int i) {
      Point2D orig = getOriginalCorePoint(i);
      Point2D shifted = shifted_.get(orig);
      return ((shifted == null) ? orig : shifted);
    }
    
    void addToMultiDataMap(Map<Point2D, Map<String, Point>> multiDataMap) {
      int numPt = pointList_.size();
      for (int i = 0; i < numPt; i++) {
        Point2D keyPt = getCorePoint(i);
        Iterator<String> isit = inboundSrcs_.iterator();
        while (isit.hasNext()) {
          String srcID = isit.next();
          Point trkPt = getSrcTrack(srcID, i);
          Map<String, Point> mapForPt = multiDataMap.get(keyPt);
          if (mapForPt == null) {
            mapForPt = new HashMap<String, Point>();
            multiDataMap.put(keyPt, mapForPt);
          }
          if (!mapForPt.containsKey(srcID)) {
            mapForPt.put(srcID, trkPt);
          }
        }
      }
      return;
    }

    public Point2D getPoint(String srcID, int i) {
      Point trkPt = getSrcTrack(srcID, i);
      
      Point2D corePt = getCorePoint(i);
      Vector2D off = new Vector2D(trkPt.getX() * UiUtil.GRID_SIZE, trkPt.getY() * UiUtil.GRID_SIZE);     
      return (off.add(corePt));
    }   
     
    Point2D getDeparturePoint(String srcID) {
      Point trkPt = getSrcTrack(srcID, 0);      
      Point2D corePt = getCorePoint(0);
      Vector2D off = new Vector2D(trkPt.getX() * UiUtil.GRID_SIZE, trkPt.getY() * UiUtil.GRID_SIZE);     
      return (off.add(corePt));
    }
    
    public Point2D getCoreArrivalPoint() {
      return (getCorePoint(pointList_.size() - 1));
    }    
          
    public Point2D getArrivalPoint(String srcID) {
      Point trkPt = getSrcTrack(srcID, pointList_.size() - 1);      
      Point2D corePt = getCorePoint(pointList_.size() - 1);
      Vector2D off = new Vector2D(trkPt.getX() * UiUtil.GRID_SIZE, trkPt.getY() * UiUtil.GRID_SIZE);     
      return (off.add(corePt));
    }       
    
    public Set<String> getTargs(int i) {
      return (targList_.get(i));
    } 
    
    void addToTargList(Set<String> trgsThru) {
      targList_.add(trgsThru);
      return;
    } 
    
    void addPoint(PointAndVec pAndV, Set<String> trgMods, boolean orthoParent) {
      pointList_.add(pAndV);
      Map<Point2D, Set<String>> globalOrthoOutboundMods = myTree_.getGlobalOrthoOutboundMods();
      if (orthoParent) {  
        Set<String> forOrth = globalOrthoOutboundMods.get(pAndV.origin);
        if (forOrth == null) {
          forOrth = new HashSet<String>();
          globalOrthoOutboundMods.put(pAndV.origin, forOrth);       
        }
        forOrth.addAll(trgMods);
      }
      return;
    }
    
    public void addElement(CenteredGridElement elem) {
      elemList_.add(elem);
      return;
    } 
       
    private int reducedOrder(String cand, Set<String> reducedSet, Map<String, Integer> globalOrder) {
      TreeMap<Integer, String> runSrcs = new TreeMap<Integer, String>();
      Iterator<String> sit = reducedSet.iterator();
      int numNeg = 0;
      while (sit.hasNext()) {
        String nextSrc = sit.next();
        Integer goVal = globalOrder.get(nextSrc);
        if (goVal.intValue() < 0) {
          numNeg++;
        }
        runSrcs.put(goVal, nextSrc);
      }
      
      int outNeg = -numNeg;
      int outPos = 0;
      Iterator<Integer> rit = runSrcs.keySet().iterator();
      while (rit.hasNext()) {
        Integer nextVal = rit.next();
        boolean isNeg = (nextVal.intValue() < 0);
        String nextSrc = runSrcs.get(nextVal);
        if (nextSrc.equals(cand)) {
          return ((isNeg) ? outNeg : outPos);
        }
        if (isNeg) {
          outNeg++;
        } else {
          outPos++;
        }
      }
      throw new IllegalStateException();
    }

    private Point getSrcTrack(String src, int i) {
      PointAndVec myPAndV = pointList_.get(i);
      PointAndVec parPAndV = getTrackTurn(i);
      Point retval = getSrcTrackAtTurn(src, parPAndV, myPAndV);
      // Want to make the start and end butt-ended, not diagonal!
      if (i == 0) {
        int dir = startDir_.canonicalDir();
        if ((dir == Vector2D.EAST) || (dir == Vector2D.WEST)) {
          retval.setLocation(0, retval.y);
        } else {
          retval.setLocation(retval.x, 0);
        }
      } else if (i == pointList_.size() - 1) {
        int dir = endDir_.canonicalDir();
        if ((dir == Vector2D.EAST) || (dir == Vector2D.WEST)) {
          retval.setLocation(0, retval.y);
        } else {
          retval.setLocation(retval.x, 0);
        }
      }
      return (retval);
    }  
    
    private PointAndVec getTrackTurn(int i) {
      PointAndVec pAndV = pointList_.get(i);
      if (i == 0) {
        return (pAndV);
      }
      int canon = pAndV.offset.canonicalDir();
      for (int j = (i - 1); j >= 0; j--) {
        PointAndVec parPAndV = pointList_.get(j);
        if (parPAndV.offset == null) {
          return (parPAndV);
        }
        int parCanon = parPAndV.offset.canonicalDir();
        if ((parCanon != canon) || (j == 0)) {
          return (parPAndV);
        }
      }
      throw new IllegalArgumentException();
    }
      
    private Point getSrcTrackAtTurn(String src, PointAndVec parPAndV, PointAndVec myPAndV) {
      Map<String, Integer>  gso = myTree_.getGlobalSourceOrder();
      Map<Point2D, Set<String>> globalOrthoOutboundSrcs = myTree_.getGlobalOrthoOutboundSrcs();

      //
      // Get the sources for the first ancestor where the inbound run does a right turn:
      //
      Set<String> srcs = globalOrthoOutboundSrcs.get(parPAndV.origin);
      if (srcs == null) {
        srcs = myTree_.getInboundSourcesForPoint(parPAndV.origin);  // never turned...
      }
      
      int r1 = reducedOrder(src, srcs, gso); 
      Set<String> oosrcs = globalOrthoOutboundSrcs.get(myPAndV.origin);      
      int r2 = ((oosrcs != null) && ((myPAndV.offset == null) || oosrcs.contains(src))) ? reducedOrder(src, oosrcs, gso) : r1;
          
      if (r1 == r2) {
        return (new Point(xSign(parPAndV, myPAndV) * r1, ySign(parPAndV, myPAndV) * r2));
      }
      
      int canon = myPAndV.offset.canonicalDir();
      if ((canon == Vector2D.WEST) || (canon == Vector2D.EAST)) {
        return (new Point(xSign(parPAndV, myPAndV) * r2, ySign(parPAndV, myPAndV) * r1));
      } else {
        return (new Point(xSign(parPAndV, myPAndV) * r1, ySign(parPAndV, myPAndV) * r2));
      }
    }
    
    private int xSign(PointAndVec parPAndV, PointAndVec myPAndV) {
      // You can either make ALL the turn points lie on a NW->SE diagonal (1) or
      // SW->NE diagonal (-1).  But if you are going to mix and match, track ordering needs
      // to be relative to the current rund direction, and not absolute (e.g. src 1 cannot
      // always have the minimum y value while traveling left-right, and minimum x going
      // up-down.  Instead, e.g. left travel needs to have it on top, while right travel has
      // it on the bottom.
      
      // 4/9/12: If we actually try to get the traces to have the same orientation with respect
      // to the center trace (e.g. always to the right), we get into inconsistencies at T
      // intersections, where we MUST flip to the opposite side for one of the branches.  Thus,
      // this means that the top/bottom left/right ordering of how a set of traces arrives at a
      // target module would be path dependent!  BAD!   Instead, using a constant x and y sign
      // insures that e.g. a trace is always on top and left sides. 
      return (1);
    }
    
    private int ySign(PointAndVec parPAndV, PointAndVec myPAndV) {
      // See above!
      return (1);
    }

    public Set<String> getSourcesToModule() {
      return (inboundSrcs_);
    }
    
    void sourceGoesToModule(String src) {
      inboundSrcs_.add(src);
      Map<Point2D, Set<String>> globalOrthoOutboundMods = myTree_.getGlobalOrthoOutboundMods();
      Map<Point2D, Set<String>> globalOrthoOutboundSrcs = myTree_.getGlobalOrthoOutboundSrcs();
      Set<String> globalTreeSrcs = myTree_.getGlobalTreeSrcs();
      int numTL = targList_.size();
      for (int i = 0; i < numTL; i++) {        
        Set<String> trgs = targList_.get(i);
        PointAndVec pAndV = pointList_.get(i);
        if (trgs.contains(trgMod_)) {
          Set<String> srcs = myTree_.getInboundSourcesForPoint(pAndV.origin);
          srcs.add(src);
          globalTreeSrcs.add(src);
          Set<String> forOrth = globalOrthoOutboundMods.get(pAndV.origin);
          if ((forOrth != null) && forOrth.contains(trgMod_)) {
            Set<String> oosrcs = globalOrthoOutboundSrcs.get(pAndV.origin);
            if (oosrcs == null) {            
              oosrcs = new HashSet<String>();
              globalOrthoOutboundSrcs.put(pAndV.origin, oosrcs);
            }
            oosrcs.add(src);
          }
        }
      }
      return;
    }
       
    void collectGridElements(Set<Point2D> cpIDs, List<GridGrower.GeneralizedGridElement> gridElements, 
                             Map<String, Map<Point2D, Dimension[]>>  dimChanges) {
      
      if (dimChanges != null) {
        gridElements.addAll(updateElementsViaMap(dimChanges));
        int numel = numElem();
        for (int j = 0; j < numel; j++) {
          CenteredGridElement ge = getElem(j);
          cpIDs.add(ge.getID());
        }
        return;
      }
 
      int numel = numElem();
      for (int j = 0; j < numel; j++) {
        CenteredGridElement ge = getElem(j);
        CenteredGridElement copy = new CenteredGridElement(ge);
        copy.setID(ge.getID());
        gridElements.add(copy); 
        cpIDs.add(ge.getID());
      }
      return;
    }
    
    //
    // Update grid link elements to accomodate increased width requirements
    //
    
    private List<CenteredGridElement> updateElementsViaMap(Map<String, Map<Point2D, Dimension[]>> dimChanges) {
      ArrayList<CenteredGridElement> retval = new ArrayList<CenteredGridElement>();
      Map<Point2D, Dimension[]> forTree = dimChanges.get(myTree_.getTreeID());
      int numel = numElem();
      for (int j = 0; j < numel; j++) {
        CenteredGridElement ge = getElem(j);
        // Note that elems are NOT in sync with the point list, as they are
        // unique across the tree...
        Point2D pt = ge.getID(); //getOriginalCorePoint(j);  NO!!!!
        CenteredGridElement copy = new CenteredGridElement(ge);
        copy.setID(ge.getID());
        retval.add(copy);
        if (forTree == null) {
          continue;
        }
        Dimension[] forPoint = forTree.get(pt);
        if (forPoint != null) {
          copy.setDeltas(forPoint[0].width, forPoint[1].width, forPoint[0].height, forPoint[1].height);
        }
      }
      return (retval);
    }

    //
    // Useful to have around, but we actually never want to grow the
    // module link space by the full link path amount...we will almostCenteredGridElement
    // always have some space to work with anyway, and should use that
    // first...
    //
    /*
    private List<CenteredGridElement> updateElementsForTracks() {
      ArrayList<CenteredGridElement> retval = new ArrayList<CenteredGridElement>();
      int numel = numElem();
      for (int j = 0; j < numel; j++) {
        CenteredGridElement ge = getElem(j);
        CenteredGridElement copy = new CenteredGridElement(ge);
        copy.setID(ge.getID());
        retval.add(copy);
        // Note that the last element is intentionally not modified...
        if (j != (numel - 1)) {
          Point2D pt = ge.getID();
          //Point2D pt = getOriginalCorePoint(j);
          Set<String> srcs = myTree_.getInboundSourcesForPoint(pt);
          MinMax xRange = new MinMax();
          xRange.init();
          MinMax yRange = new MinMax();
          yRange.init();
          Iterator<String> sit = srcs.iterator();
          while (sit.hasNext()) {
            String srcID = sit.next();
            Point trkPt = getSrcTrack(srcID, j);
            xRange.update(trkPt.x);
            yRange.update(trkPt.y);
          }
          int delX = (xRange.max - xRange.min) / 2;
          int delY = (yRange.max - yRange.min) / 2;
          copy.setDeltas(delX, delX, delY, delY);
        }
      }
      return (retval);
    }
    */
    void shiftPaths(Map<Point2D, Point2D> movedPts) {
      int numel = size();
      for (int j = 0; j < numel; j++) {         
        Point2D pt = getOriginalCorePoint(j);
        Point2D moved = movedPts.get(pt);
        if (moved != null) {
          Point2D gridded = (Point2D)moved.clone();
          UiUtil.forceToGrid(gridded, UiUtil.GRID_SIZE);
          shifted_.put((Point2D)pt.clone(), gridded);
        }
      }  
      return;
    }

    void reverse() {
      Collections.reverse(pointList_);
      Collections.reverse(elemList_);
      Collections.reverse(targList_);
      return;
    }    
  }
  
  /***************************************************************************
  **
  ** Used to return results
  */

  public static class SubsetAnalysis {
    private HashMap<Integer, String> indexToModName_;
    private HashMap<String, PrimaryAndOthers> srcToIndex_;
    private Map<String, NetModuleLinkExtractor.ExtractResultForSource> interModPaths_;
    private List<String> partialOrder_;
    private Map<Integer, Integer> topoIndexReorder_;
    private List<GenomeSubset> topoOrderedSubsets_;
    private HashMap<String, FullBorderOrder> modNameToBorderOrderedInbounds_;
    
    SubsetAnalysis(Map<String, NetModuleLinkExtractor.ExtractResultForSource> interModPaths) {
      indexToModName_ = new HashMap<Integer, String>();
      srcToIndex_ = new HashMap<String, PrimaryAndOthers>();
      interModPaths_ = interModPaths;
      partialOrder_ = null; 
    }
    
    public void mapIndexToModName(int i, String modName) {
      indexToModName_.put(new Integer(i), modName);  // original order i...
      return;
    }
    
    public List<String> getBorderOrderedSrcModules(String trgMod) {     
      FullBorderOrder fbo = modNameToBorderOrderedInbounds_.get(trgMod);
      if (fbo == null) {
        return (new ArrayList<String>());
      }
      return (fbo.drainTheOrder());
    }
        
    public void mapSourceToModules(String srcID, PrimaryAndOthers pao) {     
      srcToIndex_.put(srcID, pao);
      return;
    }
    
    public boolean hasInterModulePaths() {     
      return (interModPaths_ != null);
    }
        
    public Iterator<String> getSrcKeys() {     
      return (srcToIndex_.keySet().iterator());
    }
       
    public String getSourceModuleIDForSource(String srcID) { 
      PrimaryAndOthers pao = srcToIndex_.get(srcID);
      return (getPrimaryModuleName(pao));
    }
        
    public Iterator<String> getInterModPathKeys() {
      if (interModPaths_ == null) {
        return (null);
      }
      return (interModPaths_.keySet().iterator());
    }
       
    public PrimaryAndOthers getModulesForSrcID(String srcID) {
      return (srcToIndex_.get(srcID));
    }
    
    public String getPrimaryModuleName(PrimaryAndOthers pao) {
      if (!pao.sourceInModule()) {
        throw new IllegalStateException();
      }
      String modName = indexToModName_.get(new Integer(pao.getPrimCode()));
      return (modName);
    }
    
    public String getTargetModuleName(PrimaryAndOthers pao, Integer which) {
      String modName = indexToModName_.get(which);
      return (modName);
    }

    public ExtractResultForSource getExtractResultForSource(String modName) {
      ExtractResultForSource interModForSrc = interModPaths_.get(modName);
      return (interModForSrc);
    }
      
    public int getMappedPrimaryModuleIndex(PrimaryAndOthers pao) {
      if (!pao.sourceInModule()) {
        throw new IllegalStateException();
      }
      Integer mapped = topoIndexReorder_.get(new Integer(pao.primary_));
      return (mapped.intValue());
    }
    
    public int getMappedTargetModuleIndex(PrimaryAndOthers pao, Integer which) {
      if (!pao.sourceInModule()) {
        throw new IllegalStateException();
      }
      Integer mapped = topoIndexReorder_.get(which);
      return (mapped.intValue());
    }    
    
   /***************************************************************************
   **
   ** Figure out clockwise inbound ordering
   */
  
    public void generateBorderOrderedInbounds() {      
      modNameToBorderOrderedInbounds_ = new HashMap<String, FullBorderOrder>();
      Iterator<String> impit = getInterModPathKeys();
      if (impit == null) {
        return;
      }
      while (impit.hasNext()) {
        String modID = impit.next();
        ExtractResultForSource er4s = getExtractResultForSource(modID);
        Map<String, Integer>  tmods = er4s.getTargetMods();
        Iterator<String> tmodit = tmods.keySet().iterator();
        while (tmodit.hasNext()) {
          String tMod = tmodit.next();
          ExtractResultForTarg er4t = er4s.getResultForTarg(tMod);
          FullBorderOrder fbo = modNameToBorderOrderedInbounds_.get(tMod);
          if (fbo == null) {
            fbo = new FullBorderOrder();
            modNameToBorderOrderedInbounds_.put(tMod, fbo);            
          }
          Point2D coreArrive = er4t.getCoreArrivalPoint();
          Vector2D arrDir = er4t.getTerminalDirection();
          fbo.addAModule(modID, coreArrive, arrDir);
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Having core paths to follow for intermodule links, allocate out specific
    ** tracks around the core:
    */

    private void generatePartialOrder() {

      HashSet<String> nodeSet = new HashSet<String>();
      TreeMap<Integer, SortedSet<Link>> linkMap = new TreeMap<Integer, SortedSet<Link>>(Collections.reverseOrder());
      HashSet<Link> linkSet = new HashSet<Link>();

      Iterator<String> stiit = srcToIndex_.keySet().iterator();
      while (stiit.hasNext()) {
        String srcID = stiit.next();
        PrimaryAndOthers pao = srcToIndex_.get(srcID);     
        if (!pao.sourceInModule()) {
          continue;
        }
        String modName = indexToModName_.get(new Integer(pao.getPrimCode()));
        if (modName == null) {
          continue;
        }
        if (interModPaths_ == null) {
          continue;
        }
        NetModuleLinkExtractor.ExtractResultForSource er4s = interModPaths_.get(modName);
        if (er4s == null) {
          continue;
        }
        nodeSet.add(modName);
        Map<String, Integer> myMods = er4s.getTargetMods();
        Iterator<String> mmit = myMods.keySet().iterator();
        while (mmit.hasNext()) {
          String trg = mmit.next();
          nodeSet.add(trg);
          Integer ranking = myMods.get(trg);
          Link modLink = new Link(modName, trg);
          SortedSet<Link> forRank = linkMap.get(ranking);
          if (forRank == null) {
            forRank = new TreeSet<Link>();
            linkMap.put(ranking, forRank);
          }
          forRank.add(modLink);        
        }       
      }
      
      //
      // Add in links one at a time based on link count ranking.
      // We drop links that create cycles
      //      
      
      Iterator<SortedSet<Link>> vit = linkMap.values().iterator();
      while (vit.hasNext()) {
        SortedSet<Link> forRank = vit.next();
        Iterator<Link> fit = forRank.iterator();
        while (fit.hasNext()) {
          Link modLink = fit.next();   
          linkSet.add(modLink);
          CycleFinder cf = new CycleFinder(nodeSet, linkSet);
          if (cf.hasACycle()) {
            linkSet.remove(modLink);
          }
        }
      }
      
      //
      // Now topo sort:
      //
      
      
      GraphSearcher gs = new GraphSearcher(nodeSet, linkSet);    
      Map<String, Integer> queue = gs.topoSort(false);
      partialOrder_ = gs.topoSortToPartialOrdering(queue);    
      return;
    }
    
    /***************************************************************************
    **
    ** Having core paths to follow for intermodule links, allocate out specific
    ** tracks around the core:
    */

    private void allocateInterModulePaths() {

      //
      // If we are using inter-module overlay links to route links between subsets, now is the time
      // to provide separate tracks for each source using a particular path.
      //

     Iterator<String> stiit = srcToIndex_.keySet().iterator();
      while (stiit.hasNext()) {
        String srcID = stiit.next();
        PrimaryAndOthers pao = srcToIndex_.get(srcID);     
        if (!pao.sourceInModule()) {
          continue;
        }
        String modName = indexToModName_.get(new Integer(pao.getPrimCode()));
        if (modName == null) {
          continue;
        }
        if (interModPaths_ == null) {
          continue;
        }
        NetModuleLinkExtractor.ExtractResultForSource er4s = interModPaths_.get(modName);
        if (er4s == null) {
          continue;
        }

        Iterator<Integer> oit = pao.getTargetModules();
        while (oit.hasNext()) {
          Integer other = oit.next();
          String trgModName = indexToModName_.get(other);
          if (trgModName == null) {
            continue;
          }        
          //
          // We need to count the number of sources passing through each point of each interModule path!
          //        
          er4s.sourceGoesToModule(srcID, trgModName);
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Reorder a list of genome subsets to match the topo sort:
    */

    public List<GenomeSubset> topoReorder(List<GenomeSubset> subsetList) {
      topoIndexReorder_ = new HashMap<Integer, Integer>();
      int numSub = subsetList.size();
      topoOrderedSubsets_ = new ArrayList<GenomeSubset>();   
      SortedSet<Integer> notAssigned = (new MinMax(0, numSub - 1)).getAsSortedSet();
      int numPO = partialOrder_.size();
      for (int j = 0; j < numPO; j++) {
        String modID = partialOrder_.get(j);   
        for (int i = 0; i < numSub; i++) {    
          GenomeSubset subset = subsetList.get(i);
          String smid = subset.getModuleID();
          if ((smid != null) && smid.equals(modID)) {
            Integer matchIndex = new Integer(i);
            topoOrderedSubsets_.add(subset);
            notAssigned.remove(matchIndex);
            topoIndexReorder_.put(matchIndex, new Integer(topoOrderedSubsets_.size() - 1));            
            break;
          }
        }
      }
      Iterator<Integer> nait = notAssigned.iterator();
      while (nait.hasNext()) {
        Integer na = nait.next();
        topoOrderedSubsets_.add(subsetList.get(na.intValue()));
        topoIndexReorder_.put(na, new Integer(topoOrderedSubsets_.size() - 1));        
      }
      return (topoOrderedSubsets_);
    }
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** For tracking source and target modules on a per-source basis:
  */
 
  public class PrimaryAndOthers {
    public String srcID;
    private int primary_;
    private HashSet<Integer> others_;
        
    PrimaryAndOthers(String srcID, int primary) {
      this.primary_ = primary;  // note this refers to original order, and must be mapped follow topo sort!
      this.others_ = new HashSet<Integer>();
    }
    
    int getPrimCode() {
      return (primary_);
    }
    
    void addOtherCode(int otherCode) {
      others_.add(new Integer(otherCode));
      return;
    }    
    
    public boolean sourceInModule() {
      return (primary_ != Integer.MIN_VALUE);
    }
    
    public Iterator<Integer> getTargetModules() {
      return (others_.iterator());
    }
  }  
  
  /***************************************************************************
  **
  ** For tracking clockwise border order, per border
  */
  
  static class OneBorderOrder {
    int border;
    boolean useY;
    TreeMap<Double, SortedSet<String>> srcModuleOrder;
        
    OneBorderOrder(int border) {
      this.border = border;
      this.useY = (border == SpecialtyLayoutLinkData.RIGHT_BORDER) || 
                  (border == SpecialtyLayoutLinkData.LEFT_BORDER);
      if (border == SpecialtyLayoutLinkData.LEFT_BORDER) {
        this.srcModuleOrder = new TreeMap<Double, SortedSet<String>>(Collections.reverseOrder());
      } else {
        this.srcModuleOrder = new TreeMap<Double, SortedSet<String>>();
      }
    }
    
    void addAModule(String srcModuleID, Point2D arrivePt) {
      Double val = new Double((useY) ? arrivePt.getY() : arrivePt.getX());
      SortedSet<String> forVal = srcModuleOrder.get(val);
      if (forVal == null) {
        forVal = new TreeSet<String>();
        srcModuleOrder.put(val, forVal);
      }
      forVal.add(srcModuleID);
      return;
    }
    
    void drainTheOrder(List<String> drainTarg) {
      Iterator<SortedSet<String>> smoit = srcModuleOrder.values().iterator();
      while (smoit.hasNext()) {
        SortedSet<String> forVal = smoit.next();
        drainTarg.addAll(forVal);
      }
      return;
    }
  } 
  
  /***************************************************************************
  **
  ** For tracking border order
  */
  
  static class FullBorderOrder {
    OneBorderOrder[] allOrders;
    private int[] drainOrder_;
    
    FullBorderOrder() {
      drainOrder_ = new int[] {SpecialtyLayoutLinkData.LEFT_BORDER, 
                               SpecialtyLayoutLinkData.TOP_BORDER, 
                               SpecialtyLayoutLinkData.RIGHT_BORDER,                                
                               SpecialtyLayoutLinkData.BOTTOM_BORDER};
      allOrders = new OneBorderOrder[SpecialtyLayoutLinkData.NUM_BORDERS];
      for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {
        allOrders[i] = new OneBorderOrder(i);
      } 
    }
    
    void addAModule(String srcModuleID, Point2D arrivePt, Vector2D arriveDir) {
      int border = SpecialtyLayoutLinkData.vecToBorder(arriveDir);
      OneBorderOrder forBord = allOrders[border];
      forBord.addAModule(srcModuleID, arrivePt);
      return;
    }
    
    List<String> drainTheOrder() {
      ArrayList<String> retval = new ArrayList<String>();
      for (int i = 0; i < SpecialtyLayoutLinkData.NUM_BORDERS; i++) {
        int toDrain = drainOrder_[i];     
        OneBorderOrder drainIt = allOrders[toDrain];
        drainIt.drainTheOrder(retval);
      }
      return (retval);
    }
  } 
}
