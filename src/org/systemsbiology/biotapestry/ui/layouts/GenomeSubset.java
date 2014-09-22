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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Represent a subset of a genome that is to be laid out
*/

public class GenomeSubset {

  private String baseGenomeID_;
  private HashSet<String> nodes_;
  private HashSet<String> internalLinks_;
  private HashSet<String> linksIn_;
  private HashSet<String> linksOut_;
  public HashSet<String> externalSourceOnly;
  public HashSet<String> externalTargetOnly;
  public HashSet<String> externalBothSrcTarget;
  private HashSet<String> nodeSuperSet_;
  private HashSet<String> linkSuperSet_;
  private boolean isComplete_;
  private Point2D center_;
  private GenomeSource src_;
  private Rectangle origNodeBounds_;
  private Rectangle origModuleBounds_;  // may be null; overlay-based layouts only!
  private String moduleID_;  // for overlay-based layouts only!
  private String overlayID_;  // for overlay-based layouts only!
  private String groupID_;  // for region-only layout!
  private BTState appState_;
   
  public GenomeSubset(BTState appState, String genomeID, Set<String> selectedNodes, Point2D center) {
    appState_ = appState;
    baseGenomeID_ = genomeID;
    nodes_ = new HashSet<String>(selectedNodes);
    internalLinks_ = new HashSet<String>();
    linksIn_ = new HashSet<String>();
    linksOut_ = new HashSet<String>();
    externalSourceOnly = new HashSet<String>();
    externalTargetOnly = new HashSet<String>();
    externalBothSrcTarget = new HashSet<String>();    
    nodeSuperSet_ = new HashSet<String>();
    linkSuperSet_ = new HashSet<String>(); 
    isComplete_ = false;
    origNodeBounds_ = null;
    origModuleBounds_ = null;
    groupID_ = null;
    center_ = (center == null) ? null : (Point2D)center.clone();
    calculate();
  }
  
  //
  // Sticks everybody into the subset!
  //
  
  public GenomeSubset(BTState appState, String genomeID, Point2D center) {
    this(appState, appState.getDB().getGenome(genomeID), center);
    baseGenomeID_ = genomeID;
  }
  
  //
  // Sticks everybody into the subset!
  //
  
  public GenomeSubset(BTState appState, GenomeSource src, String genomeID, Point2D center) {
    this(appState, src.getGenome(genomeID), center);
    src_ = src;
    baseGenomeID_ = genomeID;
  }
   
  //
  // Sticks everybody into the subset!
  //
  
  private GenomeSubset(BTState appState, Genome baseGenome, Point2D center) {
    appState_ = appState;
    nodes_ = new HashSet<String>();        
    Iterator<Node> nit = baseGenome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      nodes_.add(nodeID);
    }    
    internalLinks_ = new HashSet<String>();        
    Iterator<Linkage> lit = baseGenome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      internalLinks_.add(linkID);
    } 
    linksIn_ = new HashSet<String>();
    linksOut_ = new HashSet<String>();
    externalSourceOnly = new HashSet<String>();
    externalTargetOnly = new HashSet<String>();
    externalBothSrcTarget = new HashSet<String>();
    
    nodeSuperSet_ = new HashSet<String>();
    linkSuperSet_ = new HashSet<String>(); 
    nodeSuperSet_.addAll(nodes_); 
    linkSuperSet_.addAll(internalLinks_);
    isComplete_ = true;
    center_ = (Point2D)center.clone();
  }
  
  public void setOrigBounds(DataAccessContext rcx) { 
    DataAccessContext irx = new DataAccessContext(rcx, getBaseGenome(), rcx.getLayout());
    origNodeBounds_ = irx.getLayout().getPartialBounds(nodes_, false, false, null, null, false, irx);
    center_ = new Point2D.Double(origNodeBounds_.getCenterX(), origNodeBounds_.getCenterY());
    UiUtil.forceToGrid(center_, UiUtil.GRID_SIZE);
    return;
  }
  
  public void setOrigBounds(Rectangle bounds) {
    origNodeBounds_ = (Rectangle)bounds.clone();
    origModuleBounds_ = (Rectangle)bounds.clone();
    center_ = new Point2D.Double(origNodeBounds_.getCenterX(), origNodeBounds_.getCenterY());
    UiUtil.forceToGrid(center_, UiUtil.GRID_SIZE);
    return;
  }
 
  public void setGroupID(String groupID) {
    groupID_ = groupID;
    return;
  }
  
  public String getGroupID() {
    return (groupID_);
  }
 
  public void setModuleID(String moduleID) {
    moduleID_ = moduleID;
    return;
  }
  
  public String getModuleID() {
    return (moduleID_);
  }
  
  public void setOverlayID(String overlayID) {
    overlayID_ = overlayID;
    return;
  }
  
  public String getOverlayID() {
    return (overlayID_);
  }
  
  public boolean isCompleteGenome() {
    return (isComplete_);
  }
  
  public Point2D getPreferredCenter() {
    return (center_);
  }
  
  public Rectangle getOrigBounds() {
    return (origNodeBounds_);
  }
  
  public Rectangle getOrigModuleBounds() {
    return (origModuleBounds_);
  }
  
  public Iterator<String> getNodeIterator() {
    return (nodes_.iterator());
  }
  
  public Iterator<String> getLinkageIterator() {
    return (internalLinks_.iterator());
  }
  
  public Iterator<String> getLinksInIterator() {
    return (linksIn_.iterator());
  }
  
  public Iterator<String> getLinksOutIterator() {
    return (linksOut_.iterator());
  }
  
  /***************************************************************************
  **
  ** Answer if node is in subset
  **
  */
  
  public boolean nodeInSubset(String nodeID) {
    return (nodes_.contains(nodeID));
  }  
  
  /***************************************************************************
  **
  ** Get the genome source
  **
  */
  
  public GenomeSource getGenomeSource() {
    if (src_ == null) {
      return (appState_.getDB());
    } else {
      return (src_);
    }
  }
  

  /***************************************************************************
  **
  ** Get the base genome
  **
  */
  
  public Genome getBaseGenome() {
    if (src_ == null) {
      return (appState_.getDB().getGenome(baseGenomeID_));
    } else if (baseGenomeID_ != null) {
      return (src_.getGenome(baseGenomeID_));
    } else {
      return (src_.getGenome());
    }
  }
  
  /***************************************************************************
  **
  ** Get the pseudo genome
  **
  */
  
  public Genome getPseudoGenome() {
    Genome base = getBaseGenome();
    Genome copy;
    if (base instanceof DBGenome) {
      copy = ((DBGenome)base).clone();     
    } else {
      copy = ((GenomeInstance)base).clone();
    }
    Iterator<Node> bit = base.getAllNodeIterator();
    while (bit.hasNext()) {
      Node nextNode = bit.next();
      if (!nodes_.contains(nextNode.getID())) {
        copy.removeNode(nextNode.getID());
      }
    }
    return (copy);
  }
 
  /***************************************************************************
  **
  ** Iterator over all links associated with the subset 
  **
  */
  
  public Iterator<String> getLinkageSuperSetIterator() {    
    return (linkSuperSet_.iterator());
  }
    
  /***************************************************************************
  **
  ** Get the set of nodes targeted by the node.
  */

  public Set<String> getNodeTargets(String nodeID) {
              
   if (!nodeSuperSet_.contains(nodeID)) {
      throw new IllegalArgumentException();
    }
    Genome baseGenome = getBaseGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> lit = linkSuperSet_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      if (lnk.getSource().equals(nodeID)) {
        retval.add(lnk.getTarget());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of nodes targeting the node.
  **
  */
  
  public Set<String> getNodeSources(String nodeID) {

    if (!nodeSuperSet_.contains(nodeID)) {
      throw new IllegalArgumentException();
    }
    
    Genome baseGenome = getBaseGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> lit = linkSuperSet_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      if (lnk.getTarget().equals(nodeID)) {
        retval.add(lnk.getSource());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the links headed out of the set for the given source:
  */
  
  public Set<String> getLinksHeadedOutForSource(String srcID) {
    HashSet<String> retval = new HashSet<String>();
    if (!nodes_.contains(srcID)) {
      return (retval);
    }
    Genome baseGenome = getBaseGenome();
    Iterator<String> lit = linksOut_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      if (srcID.equals(lnk.getSource())) {
        retval.add(linkID);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of nodes that have outbound targets
  */
  
  public Set<String> getSourcesHeadedOut() {
    Genome baseGenome = getBaseGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> lit = linksOut_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      retval.add(lnk.getSource());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the set of nodes that have inbound targets
  */
  
  public Set<String> getSourcesHeadedIn() {
    Genome baseGenome = getBaseGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> lit = linksIn_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      retval.add(lnk.getSource());
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get all nodes coming in: (dup of above, right?)
 
  
  public Set getAllExternalSourceNodes() {
    HashSet retval = new HashSet(externalBothSrcTarget);
    retval.addAll(externalSourceOnly);
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Get the set of nodes that have internal targets
  */
  
  public Set<String> getSourcesForInternal() {
    Genome baseGenome = getBaseGenome();
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> lit = internalLinks_.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage lnk = baseGenome.getLinkage(linkID);
      retval.add(lnk.getSource());
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Get the target count for the given node
  */
  
  public int getTargetCount(String nodeID) {
    Set<String> targs = getNodeTargets(nodeID);
    return (targs.size());
  }
  
  /***************************************************************************
  **
  ** Prep
  */
  

  private void calculate() {
    Genome baseGenome = getBaseGenome();
    Iterator<Linkage> lit = baseGenome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      String trgID = link.getTarget();
      boolean srcIn = nodes_.contains(srcID);
      boolean trgIn = nodes_.contains(trgID);
      String linkID = link.getID();
      if (srcIn && trgIn) {
        internalLinks_.add(linkID);
      } else if (srcIn) {
        linksOut_.add(linkID);
        externalTargetOnly.add(trgID);
      } else if (trgIn) {
        linksIn_.add(linkID);
        externalSourceOnly.add(srcID);
      }
    }
    DataUtil.intersection(externalSourceOnly, externalTargetOnly, externalBothSrcTarget);
    externalTargetOnly.removeAll(externalBothSrcTarget);
    externalSourceOnly.removeAll(externalBothSrcTarget);
        
    nodeSuperSet_.addAll(nodes_);
    nodeSuperSet_.addAll(externalSourceOnly);
    nodeSuperSet_.addAll(externalTargetOnly);
    nodeSuperSet_.addAll(externalBothSrcTarget);
    
    linkSuperSet_.addAll(internalLinks_);
    linkSuperSet_.addAll(linksIn_);
    linkSuperSet_.addAll(linksOut_);
    return;
  } 
}
