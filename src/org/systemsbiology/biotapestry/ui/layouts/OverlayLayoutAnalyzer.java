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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.NetModuleLinkageProperties;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;

/***************************************************************************
**
** Decides if an overlay can handle defining a layout strategy
*/

public class OverlayLayoutAnalyzer {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
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
        
  public OverlayLayoutAnalyzer() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** 
  */
  
  public OverlayReport canSupport(DataAccessContext icx, String overlayKey) {
    
    String genomeKey = icx.getGenomeID();
    NetOverlayOwner owner = icx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey);
    NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
    NetOverlayProperties noProps = icx.getLayout().getNetOverlayProperties(overlayKey);
    
    //
    // Every module needs to be of the one-rectangle type:
    //
    
    Iterator<String> nmpkit = noProps.getNetModulePropertiesKeys();
    while (nmpkit.hasNext()) {
      String propID = nmpkit.next();
      NetModuleProperties nmp = noProps.getNetModuleProperties(propID);
      int nmpType = nmp.getType();
      if (nmpType != NetModuleProperties.CONTIG_RECT) {
        return (new OverlayReport(OverlayReport.NOT_ALL_CONTIG_RECT));
      }
    }
    
    //
    // Every link tree must have all orthogonal segments:
    //
    
    Iterator<String> nmlpkit = noProps.getNetModuleLinkagePropertiesKeys();
    while (nmlpkit.hasNext()) {
      String treeID = nmlpkit.next();
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
      Set<LinkSegmentID> nonOrtho = nmlp.getNonOrthoSegments(icx);
      if (!nonOrtho.isEmpty()) {
        return (new OverlayReport(OverlayReport.CROOKED_LINKS));
      }
    } 
    
    //
    // Modules must not overlap.
    //
    
    Map<String, Rectangle> boundsPerMod = icx.getLayout().getLayoutBoundsForEachNetModule(owner, overlayKey, icx);
    HashSet<String> seenOuter = new HashSet<String>();
    Iterator<String> bit = boundsPerMod.keySet().iterator();
    while (bit.hasNext()) {
      String key = bit.next();
      seenOuter.add(key);
      Rectangle bounds = boundsPerMod.get(key);
      Iterator<String> bit2 = boundsPerMod.keySet().iterator();
      while (bit2.hasNext()) {
        String key2 = bit2.next();
        if (seenOuter.contains(key2)) {
          continue;
        }
        Rectangle bounds2 = boundsPerMod.get(key2);
        if (bounds2.intersects(bounds)) {
          return (new OverlayReport(OverlayReport.MODULES_OVERLAP));
        }       
      }     
    }
 
    //
    // Nodes can only belong to at most one module.  At the moment, 
    // all nodes must be in one module!
    //
    Genome genome = icx.getGenome();
    
    HashSet<String> allNodes = new HashSet<String>();
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      allNodes.add(node.getID());
    } 
   
    HashMap<String, String> inMod = new HashMap<String, String>();
    Iterator<NetModule> mit = nov.getModuleIterator();         
    while (mit.hasNext()) {       
      NetModule nmod = mit.next();                          
      Iterator<NetModuleMember> memit = nmod.getMemberIterator();
      while (memit.hasNext()) { 
        NetModuleMember nmm = memit.next();
        String nmmID = nmm.getID(); 
        if (inMod.containsKey(nmmID)) {
          return (new OverlayReport(OverlayReport.NODE_IN_MULTIPLE_MODULES));
        }
        inMod.put(nmmID, nmod.getID());
        allNodes.remove(nmmID);
      }
    }
    
    if (!allNodes.isEmpty()) {
      return (new OverlayReport(OverlayReport.NODE_NOT_IN_MODULE));
    }

    //
    // If there is no link between modules that have interlinks, we record
    // the deficiency:
    //
    
    HashSet<Link> needsForLinks = new HashSet<Link>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      String srcMod = inMod.get(src);
      String trgMod = inMod.get(trg);
      if ((srcMod == null) || (trgMod == null)) {
        continue;
      }
      if (srcMod.equals(trgMod)) {
        continue;
      }
      Link interMod = new Link(srcMod, trgMod);
      needsForLinks.add(interMod);
    }
    
    
    HashSet<Link> duplicated = new HashSet<Link>();
    HashSet<Link> unsatisfied = new HashSet<Link>(needsForLinks);
    Iterator<NetModuleLinkage> mlit = nov.getLinkageIterator();         
    while (mlit.hasNext()) {       
      NetModuleLinkage nmodLink = mlit.next();
      if (nmodLink.getSign() == NetModuleLinkage.NEGATIVE) {
        return (new OverlayReport(OverlayReport.NEGATIVE_LINK_SIGN));       
      }
      String srcMod = nmodLink.getSource();
      String trgMod = nmodLink.getTarget();
      Link interMod = new Link(srcMod, trgMod);
      unsatisfied.remove(interMod);
      if (duplicated.contains(interMod)) {
        return (new OverlayReport(OverlayReport.DUPLICATED_LINKS));
      }
      duplicated.add(interMod);
    }
    
    if (!unsatisfied.isEmpty()) {
      TreeSet<String> msgList = new TreeSet<String>();
      Iterator<Link> unit = unsatisfied.iterator();
      while (unit.hasNext()) {
        Link missing = unit.next();
        String srcName = nov.getModule(missing.getSrc()).getName();
        String trgName = nov.getModule(missing.getTrg()).getName();
        msgList.add(srcName + " -> " + trgName);
      }
      return (new OverlayReport(unsatisfied, new ArrayList<String>(msgList)));          
    }
    
    //
    // We cannot continue if multiple links use the same module link pad:
    //
        
    if (icx.getLayout().netModuleLinksPadCollisions(overlayKey)) {
      return (new OverlayReport(OverlayReport.SHARED_MODULE_LINK_PADS));
    }  
 
    //
    // FIXME?  Previous comment here suggested we test for "link geometry crowding".
    // But current implementation adds space as needed.  Perhaps we need to
    // call an error if link segments actually overlap, but otherwise is
    // that a problem anymore???
    //
    
    return (new OverlayReport(OverlayReport.NO_PROBLEMS));
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
  
  public static class OverlayReport {
 
    public static final int NO_PROBLEMS              = 0;
    public static final int NOT_ALL_CONTIG_RECT      = 1;
    public static final int MODULES_OVERLAP          = 2;
    public static final int NODE_IN_MULTIPLE_MODULES = 3;
    public static final int DUPLICATED_LINKS         = 4;
    public static final int MISSING_LINKS            = 5;
    public static final int CROOKED_LINKS            = 6;
    public static final int NODE_NOT_IN_MODULE       = 7;
    public static final int SHARED_MODULE_LINK_PADS  = 8;
    public static final int NEGATIVE_LINK_SIGN       = 9;
    
    private int result_;
    private Set<Link> missingLinks_;
    private List<String> missingMsgs_;
    
    OverlayReport(int result) {
      result_ = result;  
    }
    
    OverlayReport(Set<Link> missingLinks, List<String> msgs) {
      result_ = MISSING_LINKS;
      missingLinks_ = missingLinks;
      missingMsgs_ = msgs;
    }
    
    public int getResult() {
      return (result_);     
    }
    
    public Set<Link> getMissingLinks() {
      return (missingLinks_);
    } 
    
    public List<String> getMissingLinkMessages() {
      return (missingMsgs_);
    }
    
       
    public String getResultMessage(BTState appState) {
      String suffix = "badNews";
      switch (result_) {
        case NO_PROBLEMS:
          suffix = "noProblem";
          break;
        case NOT_ALL_CONTIG_RECT:
          suffix = "notContig";
          break;
        case MODULES_OVERLAP:
          suffix = "overlap";
          break;
        case NODE_IN_MULTIPLE_MODULES:
          suffix = "multiMod";
          break;
        case DUPLICATED_LINKS:
          suffix = "dupModLinks";
          break;
        case MISSING_LINKS:
          suffix = "missingLinks";
          break;
        case CROOKED_LINKS:
          suffix = "crookedLinks";
          break;
        case NODE_NOT_IN_MODULE:
          suffix = "noModForNode";
          break;
        case SHARED_MODULE_LINK_PADS:
          suffix = "linkPadCollide";
          break;
        case NEGATIVE_LINK_SIGN:
          suffix = "negLinkSign";
          break;
        default:
          throw new IllegalArgumentException();
      }
      return (appState.getRMan().getString("overlayAnalyzer." + suffix));
    }
  }
} 
