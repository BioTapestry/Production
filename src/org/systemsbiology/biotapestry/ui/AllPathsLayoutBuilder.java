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

package org.systemsbiology.biotapestry.ui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.analysis.AllPathsResult;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.layout.LayoutLinkSupport;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.layouts.ColorTypes;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.ui.layouts.NetModuleLinkExtractor;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayout;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayout;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayout.StackedBlockLayoutParams;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;

/****************************************************************************
**
** Builds a layout for the all paths display
*/

public class AllPathsLayoutBuilder {
  
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

  public AllPathsLayoutBuilder(BTState appState) {
    appState_ = appState;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build a Layout for a neighbor display.  This layout is for temporary
  ** use only, and is not added to the database. The modified layout is contained 
  ** in the rcx!
  */
  
  public void buildNeighborLayout(AllPathsResult allPaths, DataAccessContext rcx, Layout colorRef) {
      
    Layout retval = rcx.getLayout();
    GenomeSource src = rcx.getGenomeSource();
    
    TreeMap<Integer, List<String>> depthMap = new TreeMap<Integer, List<String>>();
      
    //
    // Invert the depth map to access nodes by depth
    //

    int nodeCount = 0;
    Iterator<String> nit = allPaths.getNodes();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Integer depth = allPaths.getDepth(nodeID);
      List<String> nodeList = depthMap.get(depth);
      if (nodeList == null) {
        nodeList = new ArrayList<String>();
        depthMap.put(depth, nodeList);
      }
      nodeList.add(nodeID);
      nodeCount++;
    }
      
    //
    // Place the nodes in partial order; assign colors
    //
    
    int maxCol = rcx.cRes.getNumColors();
    int colCount = 0;

    Iterator<Integer> dit = depthMap.keySet().iterator();
    int depthCount = 0;
    int fullCount = 0;
    HashMap<String, String> colRecover = new HashMap<String, String>();
    while (dit.hasNext()) {
      Integer depth = dit.next();
      List<String> nodes = depthMap.get(depth);
      Iterator<String> dnit = nodes.iterator();
      while (dnit.hasNext()) {
        String nodeID = dnit.next();
        double x = fullCount++ * 250.0;
        int type = rcx.getGenome().getNode(nodeID).getNodeType();
        NodeProperties np = new NodeProperties(rcx.cRes, retval, type, nodeID, x, 300.0, false);
        NodeProperties exNp = (colorRef == null) ? null : colorRef.getNodeProperties(nodeID);
        String existingCol = (exNp == null) ? null : exNp.getColorName();
        if (existingCol == null) {
          existingCol = rcx.cRes.getGeneColor(colCount);
          colCount = (colCount + 1) % maxCol;
        }
        np.setColor(existingCol);
        colRecover.put(nodeID, existingCol);
        retval.setNodeProperties(nodeID, np);
      }
      depthCount++;
    }
    
    //
    // Note the weirdness. We started by assigning links the same color as the source, and
    // this is bad news for white bubble sources. Reason? I think it had to do with link 
    // instances in GenomeInstances, but I am not sure. Trying to keep that functionality in 
    // while doing something more sensible (copying the link color) if appropriate.
    
    
    HashMap<String, String> linkColRecover = new HashMap<String, String>();
    HashSet<String> newLinks = new HashSet<String>();
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      newLinks.add(linkID);
      LinkProperties exLpp = (colorRef == null) ? null : colorRef.getLinkProperties(linkID);
      String existingLinkCol = (exLpp == null) ? null : exLpp.getColorName();
      linkColRecover.put(linkID, existingLinkCol);   
      String lsrc = link.getSource();
      if (colRecover.get(lsrc) == null) {
        LinkProperties exLp = (colorRef == null) ? null : colorRef.getLinkPropertiesForSource(lsrc);
        String existingCol = (exLp == null) ? null : exLp.getColorName();
        if (existingCol == null) {
          existingCol = rcx.cRes.getGeneColor(colCount);
          colCount = (colCount + 1) % maxCol;
        }
        colRecover.put(lsrc, existingCol);
      }
    }
    
    LayoutLinkSupport.gridlessLinkLayoutPrep(rcx, newLinks, null);
    
    // Again, belt and suspenders.
    HashSet<String> done = new HashSet<String>();
    Iterator<String> lcrkit = linkColRecover.keySet().iterator();
    while (lcrkit.hasNext()) {
      String linkTag = lcrkit.next();
      LinkProperties lp = retval.getLinkProperties(linkTag);
      String lpSrc = lp.getSourceTag();
      if (done.contains(lpSrc)) {
        continue;
      }
      done.add(lpSrc);
      // Not all nodes have links out of them (i.e. the target node):
      if (lp != null) {
        lp.setColor(linkColRecover.get(linkTag));
      }
    } 
    
    Iterator<String> crkit = colRecover.keySet().iterator();
    while (crkit.hasNext()) {
      String srcTag = crkit.next();
      if (done.contains(srcTag)) {
        continue;
      }
      LinkProperties lp = retval.getLinkPropertiesForSource(srcTag);
      // Not all nodes have links out of them (i.e. the target node):
      if (lp != null) {
        lp.setColor(colRecover.get(srcTag));
      }
    }
    
    //
    // We have no group properties here at this point.
    //
      
    if (rcx.getGenome() instanceof GenomeInstance) {
      Genome genome = Layout.determineLayoutTarget(rcx.getGenome());
      Iterator<Group> grit = ((GenomeInstance)genome).getGroupIterator();
      while (grit.hasNext()) {
        Group myGroup = grit.next();
        GroupProperties gp = new GroupProperties(0, myGroup.getID(), new Point2D.Double(0.0, 0.0), 0, rcx.cRes);
        gp.setDoNotRender(true);
        retval.setGroupProperties(myGroup.getID(), gp);
      }
    }
    
    SpecialtyLayout wlo;
    SpecialtyLayoutEngineParams sbParams;
    wlo = new StackedBlockLayout(appState_);
    sbParams = StackedBlockLayout.getDefaultParams(false);
    StackedBlockLayoutParams slbParams = (StackedBlockLayoutParams)sbParams;
    slbParams.grouping = StackedBlockLayout.SrcTypes.SRCS_BY_CURR_POSITION;
    slbParams.rowSize = nodeCount + 20;
    slbParams.compressType = StackedBlockLayout.CompressTypes.COMPRESS_PROVIDE_STRING_SPACE;
    slbParams.assignColorMethod = ColorTypes.KEEP_COLORS;
    slbParams.checkColorOverlap = false;
      
    Point2D center = new Point2D.Double(0.0, 0.0);
    try {     
      GenomeSubset subset = new GenomeSubset(appState_, src, rcx.getGenomeID(), center);
      ArrayList<GenomeSubset> sList = new ArrayList<GenomeSubset>();
      sList.add(subset);
      NetModuleLinkExtractor.SubsetAnalysis sa = (new NetModuleLinkExtractor()).analyzeForMods(sList, null, null);
      SpecialtyLayoutEngine sle = new SpecialtyLayoutEngine(appState_, sList, rcx, wlo, sa, center, sbParams, false, false);   
      sle.specialtyLayout(null, null, 0.0, 0.0);
    } catch (AsynchExitRequestException aerex) {
      throw new IllegalStateException();
    }
    return;  
  }
}
