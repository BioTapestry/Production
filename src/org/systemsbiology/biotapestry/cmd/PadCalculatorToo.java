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


package org.systemsbiology.biotapestry.cmd;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.INodeRenderer;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkRouter;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.PadDotRanking;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** A class for calculating automatic assignment of node pads
*/

public class PadCalculatorToo {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  public static final int STOP_PROCESSING = 0;
  public static final int NO_AMBIGUITY    = 1;
  public static final int USE_SOURCE      = 2;
  public static final int USE_TARGET      = 3;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor:
  */ 
  
  public PadCalculatorToo() {
  }  

  /***************************************************************************
  ** 
  ** We need to fix slash node pads before 7.0.1
  */

  public List<String> legacyIOFixupForSlashNodes(GenomeSource gSrc, LayoutSource lSrc) {
 
    DBGenome genome = (DBGenome)gSrc.getGenome();
    ArrayList<String> fixList = new ArrayList<String>();
    legacyIOFixupForSlashNodes(genome.getID(), gSrc, lSrc);
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getVfgParent() == null) {
        fixList.addAll(legacyIOFixupForSlashNodes(gi.getID(), gSrc, lSrc));
      }
    }
    return (fixList);  
  }

  /***************************************************************************
  **
  ** Repair starting in 7.0.1 for slash nodes, where we are going to a non-shared-namespace
  */

  private Set<String> legacyIOFixupForSlashNodes(String gID, GenomeSource gSrc, LayoutSource lSrc) {
 
    Genome genome = gSrc.getGenome(gID);
    HashSet<String> fixList = new HashSet<String>();
   
    Layout lo = lSrc.getLayoutForGenomeKey(gID);
    
    HashSet<String> slashIDs = new HashSet<String>();
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node nextNode = nit.next();
      if (nextNode.getNodeType() == Node.SLASH) {
        slashIDs.add(nextNode.getID());
      }
    }
 
    HashSet<String> flipNodes = new HashSet<String>();
    HashSet<String> launchToZeroLinks = new HashSet<String>();
    HashSet<String> landToZeroLinks = new HashSet<String>();
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage nextLinkage = lit.next();
      String srcNode = nextLinkage.getSource();
      String trgNode = nextLinkage.getTarget();
      
      if (slashIDs.contains(srcNode)) {
        int launch = nextLinkage.getLaunchPad();
        if (launch == 1) {
          launchToZeroLinks.add(nextLinkage.getID());
          flipNodes.add(srcNode);
          fixList.add(srcNode);
        }
      }
      if (slashIDs.contains(trgNode)) {
        int landing = nextLinkage.getLandingPad();
        if (landing == 0) {
          flipNodes.add(trgNode);          
        } else if (landing == 1) {
          landToZeroLinks.add(nextLinkage.getID());        
        }
        fixList.add(trgNode);
      }
    }
    
    Iterator<String> fit = flipNodes.iterator();
    while (fit.hasNext()) {
      String flipNode = fit.next();
      NodeProperties np = lo.getNodeProperties(flipNode);
      np.setOrientation(NodeProperties.reverseOrient(np.getOrientation()));
    }
    
    Iterator<String> lazit = launchToZeroLinks.iterator();
    while (lazit.hasNext()) {
      String linkID = lazit.next();
      Linkage link = genome.getLinkage(linkID);
      link.setLaunchPad(0);
    }
    
    Iterator<String> lnzit = landToZeroLinks.iterator();
    while (lnzit.hasNext()) {
      String linkID = lnzit.next();
      Linkage link = genome.getLinkage(linkID);
      link.setLandingPad(0);
    }
 
    return (fixList);
  } 
  
 
  /***************************************************************************
  **
  ** Look for pad assignment errors for IO fixup...
  **
  */

  public List<String> checkForGeneSrcPadErrors(GenomeSource gSrc, LayoutSource lSrc) {      
    DBGenome genome = (DBGenome)gSrc.getGenome();
    ArrayList<String> fixList = new ArrayList<String>();
   
    Layout lo = lSrc.getLayoutForGenomeKey(genome.getID());
    checkForGeneSrcErrorsInGenome(genome, lo, fixList);
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getVfgParent() == null) {
        lo = lSrc.getLayoutForGenomeKey(gi.getID());
        checkForGeneSrcErrorsInGenome(gi, lo, fixList);
      }
    }
   
    return (fixList);   
  }  
  
  /***************************************************************************
  **
  ** Look for non-zero gene source pad assignment errors for IO fixup.
  */

  private void checkForGeneSrcErrorsInGenome(Genome genome, Layout lo, List<String> fixList) {
    //
    // Super simple:  If we start at a gene, we MUST have a zero source pad.  Period.
    //
    Map<Integer, NodeProperties.PadLimits> limMap = NodeProperties.getFixedPadLimits();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcNodeID = link.getSource();
      Node srcNode = genome.getNode(srcNodeID);
      NodeProperties.PadLimits lim = limMap.get(Integer.valueOf(srcNode.getNodeType()));
      if (!lim.sharedNamespace) {
        if (link.getLaunchPad() != 0) {
          link.setLaunchPad(0);
          fixList.add(srcNode.getName());
        }
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Look for pad assignment errors for IO fixup...
  **
  */

  public List<IOFixup> checkForPadErrors(GenomeSource gSrc, LayoutSource lSrc) {      
    Map<Integer, NodeProperties.PadLimits> limMap = NodeProperties.getFixedPadLimits();
    DBGenome genome = (DBGenome)gSrc.getGenome();
    ArrayList<IOFixup> fixList = new ArrayList<IOFixup>();
   
    checkForPadErrorsInGenome(lSrc, genome, limMap, fixList);
    Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      if (gi.getVfgParent() == null) {
        checkForPadErrorsInGenome(lSrc, gi, limMap, fixList);
      }
    }   
    return (fixList);   
  }  
  
  /***************************************************************************
  **
  ** Look for pad assignment errors for IO fixup...
  **
  */

  private void checkForPadErrorsInGenome(LayoutSource lSrc, Genome genome, Map<Integer, NodeProperties.PadLimits> limMap, List<IOFixup> fixList) {   

    HashMap<String, Set<Integer>> landingPadsPerNode = new HashMap<String, Set<Integer>>();
    HashMap<String, Set<Integer>> sourcePadsPerNode = new HashMap<String, Set<Integer>>();
 
    //
    // Gather up pad assignments:
    //
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      int srcPad = link.getLaunchPad();
      Set<Integer> usedPads = sourcePadsPerNode.get(src);
      if (usedPads == null) {
        usedPads = new HashSet<Integer>();
        sourcePadsPerNode.put(src, usedPads);
      }        
      usedPads.add(new Integer(srcPad));
      
      String trg = link.getTarget();   
      int trgPad = link.getLandingPad();
      usedPads = landingPadsPerNode.get(trg);
      if (usedPads == null) {
        usedPads = new HashSet<Integer>();
        landingPadsPerNode.put(trg, usedPads);
      }        
      usedPads.add(new Integer(trgPad));
    }
    
    Iterator<String> snit = sourcePadsPerNode.keySet().iterator();
    while (snit.hasNext()) {
      String src = snit.next();
      Node node = genome.getNode(src);     
      NodeProperties.PadLimits lim = limMap.get(Integer.valueOf(node.getNodeType()));    
      Set<Integer> usedPads = sourcePadsPerNode.get(src);
      Integer srcPad;
      if (usedPads.size() != 1) {
        srcPad = generateLaunchConsensus(genome, src, lim, landingPadsPerNode.get(src), fixList);
      } else {
        srcPad = usedPads.iterator().next();
      }
      if (lim.sharedNamespace) {
        Set<Integer> usedTargPads = landingPadsPerNode.get(src);
        if (usedTargPads == null) {
          continue;
        }
        if (usedTargPads.contains(srcPad)) {
          Layout lo = lSrc.getLayoutForGenomeKey(genome.getID());
          resolveSrgTrgCollision(genome, node, lo, lim, srcPad.intValue(), usedTargPads, fixList);
        }
      }
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** If we have an I/O error with mismatching source pads, find the consensus
  ** pad.  If there is a landing collision at the same time, try to resolve
  ** towards a non-collision pad.
  **
  */

  private Integer generateLaunchConsensus(Genome genome, String nodeID, 
                                          NodeProperties.PadLimits padLimits,
                                          Set<Integer> landPads, List<IOFixup> fixList) {
      
    //
    // Figure out the votes for each source pad:
    //
    
    HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
    HashMap<Integer, Set<String>> linksPerPad = new HashMap<Integer, Set<String>>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      if (src.equals(nodeID)) {
        int srcPad = link.getLaunchPad();
        Integer srcPadObj = new Integer(srcPad);
        Integer padCount = counts.get(srcPadObj);
        padCount = (padCount == null) ? new Integer(1) :  new Integer(padCount.intValue() + 1);
        counts.put(srcPadObj, padCount);
        Set<String> linksForThis = linksPerPad.get(srcPadObj);
        if (linksForThis == null) {
          linksForThis = new HashSet<String>();
          linksPerPad.put(srcPadObj, linksForThis);
        }
        linksForThis.add(link.getID());
      }
    }
     
    int maxCount = 0;
    TreeSet<Integer> maxPads = new TreeSet<Integer>();
    
    Iterator<Integer> cit = counts.keySet().iterator();
    while (cit.hasNext()) {
      Integer srcPadObj = cit.next();
      Integer countObj = counts.get(srcPadObj);
      int count = countObj.intValue();
      if (count > maxCount) {
        maxCount = count;
        maxPads.clear();
        maxPads.add(srcPadObj);
      } else if (count == maxCount) {
        maxPads.add(srcPadObj);
      }
    }
    
    Integer consensus = null;
    
    if ((maxPads.size() == 1) || (!padLimits.sharedNamespace) || (landPads == null)) {
      consensus = maxPads.iterator().next();
    } else {
      Iterator<Integer> mpit = maxPads.iterator();
      while (mpit.hasNext()) {
        Integer aPad = mpit.next();
        if (landPads.contains(aPad)) {
          if (mpit.hasNext()) {
            continue;
          } else {
            consensus = aPad;
            break;
          }
        } else {
          consensus = aPad;
          break;          
        }        
      }
    }
    
    if (consensus == null) {
      throw new IllegalStateException();
    }
    int conVal = consensus.intValue();
    
    //
    // Build the fix list:
    //
    
    Iterator<Integer> pit = linksPerPad.keySet().iterator();
    while (pit.hasNext()) {
      Integer lipad = pit.next();
      if (!lipad.equals(consensus)) {
        Set<String> linksForThis = linksPerPad.get(lipad);
        Iterator<String> ltit = linksForThis.iterator();
        while (ltit.hasNext()) {
          String linkID = ltit.next();
          IOFixup doFix = new IOFixup(genome.getID(), linkID, true, conVal);
          fixList.add(doFix);
        }
      }
    }
    
    return (consensus);
  }
  
  /***************************************************************************
  **
  ** If we have an I/O error with a link landing on a source pad, come up with
  ** a resolution.
  **
  */

  private void resolveSrgTrgCollision(Genome genome, Node node, Layout lo, NodeProperties.PadLimits padLimits,
                                      int sourcePad, Set<Integer> landPads, List<IOFixup> fixList) { 
        
    //
    // If we have a nearby unoccupied pad, go for it.  Else, if we have any unoccupied
    // pad, go for it.  Else, go for any pad.
    //
    
    int newTrgPad = Integer.MAX_VALUE;
    
    NodeProperties np = lo.getNodeProperties(node.getID());
    INodeRenderer trgRenderer = np.getRenderer();
    List<Integer> nearby = trgRenderer.getNearbyPads(node, sourcePad, np);
    if (nearby != null) {
      Iterator<Integer> nit = nearby.iterator();
      while (nit.hasNext()) {
        Integer nearPad = nit.next();
        if (!landPads.contains(nearPad)) {
          newTrgPad = nearPad.intValue();
          break;
        }
      }
    }
      
    //
    // Find out our alternatives for places to go:
    //
    if (newTrgPad == Integer.MAX_VALUE) {    
      int newPadCount = node.getPadCount();
      int minPadNum = 0;
      if (newPadCount > padLimits.defaultPadCount) {  // we have extra pads:
        minPadNum = padLimits.defaultPadCount - newPadCount;
      }
      if ((minPadNum < 0) && !padLimits.landingPadsCanOverflow) {
        throw new IllegalStateException();
      }

      for (int i = minPadNum; i < padLimits.landingPadMax; i++) {
        if (i != sourcePad) {
          Integer iObj = new Integer(i);
          if (!landPads.contains(iObj) || (i == (padLimits.landingPadMax - 1))) {
            newTrgPad = i;
            break;
          }
        }
      }
    }
    
    if (newTrgPad == Integer.MAX_VALUE) {
      throw new IllegalStateException();
    }
       
    String nodeID = node.getID();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String trg = link.getTarget();
      if (trg.equals(nodeID)) {
        int trgPad = link.getLandingPad();
        if (trgPad == sourcePad) {
          IOFixup doFix = new IOFixup(genome.getID(), link.getID(), false, newTrgPad);
          fixList.add(doFix);
        } 
      }
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Fix up pad errors for IO..
  **
  */

  public void fixIOPadErrors(GenomeSource gSrc, List<IOFixup> fixList) {
    
    int numFix = fixList.size();
    for (int i = 0; i < numFix; i++) {
      IOFixup doFix = fixList.get(i);     
      Genome genome = gSrc.getGenome(doFix.genomeID);
      fixIOPadErrorsForGenome(doFix, genome);
      if (genome instanceof GenomeInstance) {
        GenomeInstance rootInstance = (GenomeInstance)genome;
        Iterator<GenomeInstance> git = gSrc.getInstanceIterator();
        while (git.hasNext()) {
          GenomeInstance gi = git.next();
          if ((gi != rootInstance) && (rootInstance.isAncestor(gi))) {
            fixIOPadErrorsForGenome(doFix, gi);
          }
        }
      }
    }
   
    return;   
  }  
  
  /***************************************************************************
  **
  ** Fix up pad errors for IO..
  **
  */

  private void fixIOPadErrorsForGenome(IOFixup doFix, Genome genome) {    
    Linkage link = genome.getLinkage(doFix.linkID);
    if (doFix.isLaunchChange) {
      link.setLaunchPad(doFix.newPad);
    } else {
      link.setLandingPad(doFix.newPad);        
    }
    return;   
  }    

  /***************************************************************************
  **
  ** Fixup pad changes.  Currently used to handle change in node type...
  **
  */

  public void decidePadChanges(Genome genome, String nodeID, NodeProperties.PadLimits padLimits,
                               Map<String, Integer> launchPads, Map<String, Integer> landPads) {   
    //
    // Get the actual pad count for the node:
    //
    
    int newPadCount = genome.getNode(nodeID).getPadCount();
    int minPadNum = 0;
    if (newPadCount > padLimits.defaultPadCount) {  // we have extra pads:
      minPadNum = padLimits.defaultPadCount - newPadCount;
    }
    if ((minPadNum < 0) && !padLimits.landingPadsCanOverflow) {
      throw new IllegalStateException();
    }
    
       
    //
    // Get lists of outbound and inbound links.
    //
    
    ArrayList<Linkage> outbound = new ArrayList<Linkage>();
    ArrayList<Linkage> inbound = new ArrayList<Linkage>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getTarget().equals(nodeID)) {
        inbound.add(link);
      }
      if (link.getSource().equals(nodeID)) {
        outbound.add(link);
      }
    }

    //
    // Find the source pad for the node.  Assign it if possible, else change
    // to maximum.
    //
        
    Integer srcPad = null;
    if (outbound.size() > 0) {
      int srcMax = padLimits.launchPadMax - 1;
      int srcMin = (padLimits.sharedNamespace) ? minPadNum : 0;
      srcPad = genome.getSourcePad(nodeID);
      if (srcPad == null) throw new IllegalStateException(); // better be a source!
      int srcPadVal = srcPad.intValue();
      if (srcPadVal > srcMax) {
        srcPadVal = srcMax;
      } else if (srcPadVal < srcMin) {
        srcPadVal = srcMin;
      }
      srcPad = new Integer(srcPadVal);
      Iterator<Linkage> oit = outbound.iterator();
      while (oit.hasNext()) {
        Linkage link = oit.next();
        launchPads.put(link.getID(), srcPad);
      }
    }
        
    //
    // Go through inbound links and determine occupied slots.  If they
    // are out of bounds or equal the srcPad, add to pending list.
    //
    
    TreeSet<Integer> trgPads = new TreeSet<Integer>();
    for (int i = minPadNum; i < padLimits.landingPadMax; i++) {
      trgPads.add(new Integer(i));
    }
    if (padLimits.sharedNamespace && (srcPad != null)) {
      trgPads.remove(srcPad);
    }
    
    ArrayList<Linkage> pendingList = new ArrayList<Linkage>();
    Iterator<Linkage> iit = inbound.iterator();
    while (iit.hasNext()) {
      Linkage link = iit.next();
      int landPad = link.getLandingPad();
      if ((srcPad != null) && padLimits.sharedNamespace && (landPad == srcPad.intValue())) {
        pendingList.add(link);
      } else if (landPad < minPadNum) {
        pendingList.add(link);
      } else if (landPad >= padLimits.landingPadMax) {
        pendingList.add(link);
      } else {
        Integer lp = new Integer(landPad);
        trgPads.remove(lp);
        landPads.put(link.getID(), lp);
      }
    }
    
    //
    // Go through dangling links and fit them into the available slots.  If none
    // available, take the closest that is not the source pad.
    //
    
    Iterator<Linkage> pit = pendingList.iterator();
    while (pit.hasNext()) {
      Linkage link = pit.next();
      String linkID = link.getID();
      int landPad = link.getLandingPad();
      if (trgPads.isEmpty()) {
        landPad = getClosestOccupiedNonSource(linkID, landPad, padLimits, srcPad, minPadNum);
      } else {
        landPad = getClosestUnoccupiedNonSource(linkID, landPad, padLimits, trgPads, srcPad);
      }
      Integer lp = new Integer(landPad);
      trgPads.remove(lp);
      landPads.put(link.getID(), lp);
    }      
    return;
  }
 
  
  
  /***************************************************************************
  **
  ** Get the closest non-source pad when all are occupied FIX ME!  This only works with
  ** contiguous numbering schemes (genes) not spiral numbering schemes!
  **
  */
  
  private int getClosestOccupiedNonSource(String linkID, int landPad, NodeProperties.PadLimits padLimits,
                                          Integer srcPad, int minPadNum) {
                                           
    
    int retval;
    boolean checkForSource = (srcPad != null) && padLimits.sharedNamespace;
    int srcPadVal = (checkForSource) ? srcPad.intValue() : Integer.MIN_VALUE;
    if (landPad < minPadNum) {
      retval = minPadNum;
      if (checkForSource && (retval == srcPadVal)) {
        retval++;
      }
    } else if (landPad >= padLimits.landingPadMax) {
      retval = padLimits.landingPadMax - 1;
      if (checkForSource && (retval == srcPadVal)) {
        retval--;
      }
    } else if (checkForSource && (landPad == srcPadVal)) {
      if (landPad == minPadNum) {
        retval = landPad + 1;
      } else if (landPad == (padLimits.landingPadMax - 1)) {
        retval = landPad - 1;
      } else {
        retval = landPad + 1;
      }
    } else {
      throw new IllegalStateException();
    }
    if ((retval >= padLimits.landingPadMax) || (retval < minPadNum)) {
      throw new IllegalStateException();
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the closest non-source pad FIX ME!  This only works with
  ** contiguous numbering schemes (genes) not spiral numbering schemes!
  **
  */
  
  private int getClosestUnoccupiedNonSource(String linkID, int landPad, NodeProperties.PadLimits padLimits,
                                            SortedSet<Integer> trgPads, Integer srcPad) {
    
    Integer retval;
    Integer firstPad = trgPads.first();
    if (landPad < firstPad.intValue()) {
      retval = firstPad;
      trgPads.remove(retval);
    } else if (landPad >= padLimits.landingPadMax) {
      retval = trgPads.last();
      trgPads.remove(retval);
    } else if ((srcPad != null) && padLimits.sharedNamespace && (landPad == srcPad.intValue())) {
      Integer greatestMin = null;
      Integer leastMax = null;
      int srcVal = srcPad.intValue();
      Iterator<Integer> tpit = trgPads.iterator();
      while (tpit.hasNext()) {
        Integer curr = tpit.next();
        if (curr.intValue() < srcVal) {
          greatestMin = curr;
        }
        if ((curr.intValue() > srcVal) && (leastMax == null)) {
          leastMax = curr;
        }
      }
      if (leastMax != null) {
        retval = leastMax;
      } else if (greatestMin != null) {
        retval = greatestMin;
      } else {
        throw new IllegalStateException();
      }
    } else {
      throw new IllegalStateException();
    }
    return (retval.intValue());
  }   
  
  /***************************************************************************
  **
  ** Get the closest unoccupied pad.  FIX ME!  This only works with
  ** contiguous numbering schemes (genes) not spiral numbering schemes!
  **
  */
  
  private int getClosestUnoccupied(int landPad, SortedSet<Integer> trgPads) {
    
    Integer retval;
    Integer greatestMin = null;
    Integer leastMax = null;
    Iterator<Integer> tpit = trgPads.iterator();
    while (tpit.hasNext()) {
      Integer curr = tpit.next();
      if (curr.intValue() < landPad) {
        greatestMin = curr;
      }
      if ((curr.intValue() > landPad) && (leastMax == null)) {
        leastMax = curr;
      }
    }
    if (leastMax != null) {
      retval = leastMax;
    } else if (greatestMin != null) {
      retval = greatestMin;
    } else {
      throw new IllegalStateException();
    }
    return (retval.intValue());
  }     
  
/***************************************************************************
  **
  ** Get the closest unoccupied pad.  Note:  The pad set here are the USED pads,
  ** the nearby list is ordered by distance.
  */
  
  private Integer getClosestUnoccupiedFromRankedCandidates(int landPad, SortedSet<Integer> usedPads, List<Integer> nearby) {    
    int numNear = (nearby == null) ? 0 : nearby.size();
    for (int i = 0; i < numNear; i++) {
      Integer check = nearby.get(i);
      if (!usedPads.contains(check)) {
        return (check);
      }
    }
    return (null);     
  }       
  
  /***************************************************************************
  **
  ** Figure out the pad for an autolink, given a set of launch and landing constraints
  */  
 
  public PadResult padCalc(GenomeSource gSrc, String srcID, String targID, PadConstraints padLimits, boolean nodesCanGrow) {
    return (padCalcFast(gSrc, srcID, targID, padLimits, nodesCanGrow, null)); 
  }
  
  /***************************************************************************
  **
  ** Figure out the pad for an autolink, given a set of launch and landing constraints
  */  
 
  public PadResult padCalcFast(GenomeSource gSrc, String srcID, String targID, PadConstraints padLimits, boolean nodesCanGrow, InvertedSrcTrg ist) {
    
    if ((padLimits != null) && padLimits.arePadsForced()) {
      return (new PadResult(padLimits.getForcedLaunch(), padLimits.getForcedLanding()));
    }
 
    DBGenome genome = (DBGenome)gSrc.getGenome();
    
    TreeSet<Integer> trgPads = new TreeSet<Integer>();
    TreeSet<Integer> overflow = new TreeSet<Integer>();
    Node node = genome.getNode(targID);
    INodeRenderer trgRenderer = NodeProperties.buildRenderer(node.getNodeType());
    
    int targMax = trgRenderer.getFixedLandingPadMax();
    int targMin = (trgRenderer.sharedPadNamespaces()) ? 1 : 0;
    for (int i = targMin; i < targMax; i++) {  // skip 0, used for source pad
      trgPads.add(new Integer(i));
    }    
     
    int srcPad = 0;
    
    //
    // For big batch layout operations, providing the inverted src/trg object
    // speeds things up quite a bit when there are many links:
    //
    
    if (ist != null) {
      //
      // The source pad for the link must match the source pad
      // for all other links from that source:
      //
      
      Set<String> outFromSrc = ist.outboundLinkIDs(srcID);
      if (!outFromSrc.isEmpty()) {
        String outLink = outFromSrc.iterator().next();
        Linkage link = genome.getLinkage(outLink);
        srcPad = link.getLaunchPad();
      }
      
      //
      // If the target shares pads between launch and landing, we
      // need to ditch the pad used for launches:
      //  
      if (trgRenderer.sharedPadNamespaces()) {
        Set<String> outFromTrg = ist.outboundLinkIDs(targID);
        if (!outFromTrg.isEmpty()) {
          String outLink = outFromTrg.iterator().next();
          Linkage link = genome.getLinkage(outLink);
          trgPads.remove(new Integer(link.getLaunchPad()));
        }
        // The following is the match with the broken case below.
        // Ditch it:
        //Set<String> inToSrc = ist.inboundLinkIDs(srcID);
        //if (!inToSrc.isEmpty()) {
        //  String inLink = inToSrc.iterator().next();
        //  Linkage link = genome.getLinkage(inLink);
        //  trgPads.remove(new Integer(link.getLaunchPad()));
        //}
      }
      
      //
      // Ditch all landing pads already in use:
      //
      Set<String> inToTrg = ist.inboundLinkIDs(targID);
      Iterator<String> ittit = inToTrg.iterator();
      while (ittit.hasNext()) {
        String inLink = ittit.next();
        Linkage link = genome.getLinkage(inLink); 
        int lpad = link.getLandingPad();
        if (lpad >= 0) {
          trgPads.remove(new Integer(lpad));
        } else {
          overflow.add(new Integer(lpad));
        }
      }
    } else {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        DBLinkage link = (DBLinkage)lit.next();
        String src = link.getSource();
        String trg = link.getTarget();
  
        if (src.equals(srcID)) {
          srcPad = link.getLaunchPad();
        }
        if (src.equals(targID)) {
          if (trgRenderer.sharedPadNamespaces()) {
            trgPads.remove(new Integer(link.getLaunchPad()));
          }
        }
        //
        // 8/21/13 What the heck is this? All the links that terminate on the source node
        // are having their launch pads removed from the target pool? WHY?? DITCH THIS!
        //
        // if (trg.equals(srcID)) {
        //  if (trgRenderer.sharedPadNamespaces()) {
        //     trgPads.remove(new Integer(link.getLaunchPad()));
        //   }
        // }
  
        if (trg.equals(targID)) {
          int lpad = link.getLandingPad();
          if (lpad >= 0) {
            trgPads.remove(new Integer(lpad));
          } else {
            overflow.add(new Integer(lpad));
          }
        }
      }
    }
    
    //
    // Fold in launch pad limits
    //
    
    if (padLimits != null) {
      Iterator<Integer> plit = padLimits.getUsedLaunchPads();
      if (plit != null) {
        while (plit.hasNext()) {
          Integer launch = plit.next();
          srcPad = launch.intValue();
          if (trgRenderer.sharedPadNamespaces()) {
            trgPads.remove(launch);
          }
        }
      }

      plit = padLimits.getUsedLandingPads();
      if (plit != null) {    
        while (plit.hasNext()) {
          Integer landing = plit.next();
          int lpad = landing.intValue();
          if (lpad >= 0) {
            trgPads.remove(landing);
          } else {
            overflow.add(landing);
          }
        }    
      }
    }
    
    //
    // We need to find empty source and target pads
    // for the link.  If any other link is launching from the source, we should
    // reuse that pad and use a link tree to render.  If we run out of target pads,
    // we will just need to reuse the highest number pad (yuk).
    //
    // Note: Child models may be overriding these pads locally:  FIX ME???
    //
    
    int trgPad;
    if (trgPads.isEmpty()) {
      if (trgRenderer.landingPadsCanOverflow() && nodesCanGrow) {
        // Don't bother looking for holes:
        trgPad = (overflow.isEmpty()) ? -1 : overflow.first().intValue() - 1;
      } else {
        trgPad = targMax - 1;
      }
    } else {  
      trgPad = trgPads.iterator().next().intValue();
    }
    return (new PadResult(srcPad, trgPad)); 
  }
  
  /***************************************************************************
  **
  ** For a given node, recommend link landing/launch pad swaps 
  ** based on source/target locations
  */
  
  public Map<String, PadResult> recommendLinkSwaps(DataAccessContext rcx, String nodeID,
                                                   Map<String, PadConstraints> padConstraints) {
    
    //
    // Go through all targets of this node, find the closest one, and suggest a launch
    // pad that's closest.
    //
                                   
    SortedSet<PadDotRanking> bestLaunch = getBestLaunch(rcx, nodeID);
                       
    //
    // For sources to this node, find the closest of sources and siblings and put the
    // landing pad there.  If less than the maximum number of pads, we do not share.
    //
                                   
    Map<String, SortedSet<PadDotRanking>> bestLandings = getBestLandings(rcx, nodeID);    
    Map<String, PadResult> resolved = resolvePadCollisions(rcx, nodeID, bestLaunch, bestLandings, padConstraints);
    return (resolved);
    
  }
  
  /***************************************************************************
  **
  ** Generate required pad constraints for new link
  */
  
  public PadConstraints generatePadConstraints(String sourceID, String targetID, OldPadMapper opm,
                                               String oldLinkID, Map<String, String> newNodeToOldNode) {
      
    PadConstraints retval = new PadConstraints();
    
    if (opm == null) {
      return (retval);
    }
    
    //
    // If we have an old link we are replacing, the pad constraints are forced.  Otherwise, we
    // gather up the forced requirements for the source and target nodes and pass them in.
    //
                                                    
    if (oldLinkID != null) {
      retval.setForcedPads(opm.getLaunch(oldLinkID), opm.getLanding(oldLinkID));
      return (retval);
    }
    
    String oldSource = newNodeToOldNode.get(sourceID);
    if (oldSource != null) {
      Integer launch = opm.getNodeLaunch(oldSource);
      if (launch != null) {
        retval.addUsedLaunchPad(launch.intValue());
      }
    }
    
    String oldTarget = newNodeToOldNode.get(targetID);    
    if (oldTarget != null) {
      Iterator<Integer> olit = opm.getNodeLandings(oldTarget);
      if (olit != null) {
        while (olit.hasNext()) {
          Integer oldLanding = olit.next();
          retval.addUsedLandingPad(oldLanding.intValue());
        }
      }
    }
    return (retval); 
  } 

  /***************************************************************************
  **
  ** Used to figure out how to assign root genome links pads when doing an
  ** upward layout sync.
  */  
   
  public void getDirections(UpwardPadSyncData upsd, DBGenome dbg, 
                            Map<String, Integer> launchDirections, 
                            Map<String, Integer> landDirections, 
                            boolean forceUnique, Set<String> orphanedLinks) {

    //
    // Assign launch pads first:
    //
    
    HashSet<String> targList = new HashSet<String>();
    HashMap<String, Integer> useSourcePadsPerNode = new HashMap<String, Integer>();     
    Iterator<Linkage> lit = dbg.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      String linkID = link.getID();
      Integer launch = useSourcePadsPerNode.get(srcID);
      if (launch == null) {
        launch = calcSourcePad(upsd, srcID);
        if (launch == null) {
          launch = new Integer(link.getLaunchPad());
        }
        useSourcePadsPerNode.put(srcID, launch);
      }
      launchDirections.put(linkID, launch);
      targList.add(link.getTarget());
    }

    //
    // Accumulate orphan data:
    //
    
    UpwardPadSyncData orphUpsd = new UpwardPadSyncData();
    Iterator<String> oit = orphanedLinks.iterator();
    while (oit.hasNext()) {
      String linkID = oit.next();
      Linkage orphLink = dbg.getLinkage(linkID);
      String trgID = orphLink.getTarget();
      int orphPad = orphLink.getLandingPad();
      orphUpsd.setForTarget(linkID, trgID, orphPad);
    }
       
    Map<Integer, NodeProperties.PadLimits> lims = NodeProperties.getFixedPadLimits();    
      
    Iterator<String> tlit = targList.iterator();
    while (tlit.hasNext()) {
      String trgID = tlit.next();
      Integer srcPad = useSourcePadsPerNode.get(trgID);
      figureTargetPads(upsd, orphUpsd, dbg, trgID, lims, srcPad, landDirections, forceUnique);
    }
    
    //
    // Sanity check:
    //
    
    lit = dbg.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (landDirections.get(link.getID()) == null) {
        throw new IllegalStateException();
      }
    }
    

    return;
  } 
 
  
 /***************************************************************************
  **
  ** When we have pad collisions, we need to come up with nearby fake terminal
  ** regions to allow the layout algorithm to succeed.
  */  
   
  public Map<String, LinkPlacementGrid.TerminalRegion> generateTerminalRegionsForCollisions(DataAccessContext rcx,
                                                                                            LinkPlacementGrid grid, Set<String> inboundLinks, 
                                                                                            String targetID, Set<String> needAltTargs, 
                                                                                            Map<String, Set<String>> okGroupMap) {
  
    Genome genome = rcx.getGenome();
    TreeSet<Integer> usedPads = new TreeSet<Integer>();
    HashSet<Point> usedEmerg = new HashSet<Point>();
    HashSet<Integer> grabbedPads = new HashSet<Integer>();
    Node node = genome.getNode(targetID);
    
   
    NodeProperties np = rcx.getLayout().getNodeProperties(targetID);
    INodeRenderer trgRenderer = np.getRenderer();
    boolean dropSource = trgRenderer.sharedPadNamespaces();
      
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (dropSource) {
        String src = link.getSource();        
        if (src.equals(targetID)) {
          int srcPad = link.getLaunchPad();
          usedPads.add(new Integer(srcPad));
        }
      }
      
      String trg = link.getTarget();     
      if (trg.equals(targetID)) {
        int lpad = link.getLandingPad();
        usedPads.add(new Integer(lpad));
      }
    }
 
    HashMap<String, LinkPlacementGrid.TerminalRegion> retval = new HashMap<String, LinkPlacementGrid.TerminalRegion>();

    //
    // First try to find a nearby unoccupied pad that can accept a landing zone.  The
    // pad must actually be present on the node (no phantom pad extensions!).  If no
    // dice, start looking for emergency pads!
    //
      
   
    Iterator<String> ilit = inboundLinks.iterator();
    while (ilit.hasNext()) {
      String inLink = ilit.next();
      if (!needAltTargs.contains(inLink)) {
        continue;
      }
      Linkage link = genome.getLinkage(inLink);
      int lpad = link.getLandingPad();

      List<Integer> nearby = trgRenderer.getNearbyPads(node, lpad, np);
      Integer lpadObj = new Integer(lpad);
      boolean weAreDone = false;
      
      //
      // First thing we do is to try and steal unoccupied target pads:
      //

      Integer closestObj = null;     
      if (!grabbedPads.contains(lpadObj)) {
        grabbedPads.add(lpadObj);
        closestObj = lpadObj;
      } else {          
        closestObj = getClosestUnoccupiedFromRankedCandidates(lpad, usedPads, nearby);
      }

      while (closestObj != null) {
        Set<String> okGroups = okGroupMap.get(inLink);
        LinkPlacementGrid.TerminalRegion treg = LinkRouter.generateArrival(inLink, rcx, grid, closestObj, okGroups);
        usedPads.add(closestObj);
        if (grid.terminalRegionCanBeReserved(treg)) {
          retval.put(inLink, treg);
          okGroupMap.put(inLink, okGroups);
          weAreDone = true;
          break;
        }
        closestObj = getClosestUnoccupiedFromRankedCandidates(lpad, usedPads, nearby);
      }      
      
      //
      // If there are no pads to steal, the next thing we do is to generate
      // emergency candidates:
      //
      
      if (!weAreDone) {
        int count = 0;
        int ystart = 10;
        int xstart = -5;
        int xend = 5;
        int maxCount = 50;
        
        // Migrate the checks up diagonally in a zig-zag:
        
        int yVal = ystart;
        int xVal = xstart;
        while ((count < maxCount) && !weAreDone) {
          Point checkPt = new Point(xVal, yVal);
          if (!usedEmerg.contains(checkPt) && (xVal != 0)) { 
            usedEmerg.add(checkPt);
            HashSet<String> okGroups = new HashSet<String>();
            LinkPlacementGrid.TerminalRegion emerReg = LinkRouter.generateEmergencyArrival(inLink, rcx, 
                                                                                           grid, lpad, xVal, yVal, okGroups);
            if (emerReg != null) {
              okGroupMap.put(inLink, okGroups);
              retval.put(inLink, emerReg);
              weAreDone = true;
            }
          }
          yVal++;
          xVal++;
          if (xVal > xend) {
            xVal = xstart;
          }
          count++;
        }
      }
    }
    return (retval);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Used to return pad results
  */
  
  public static class PadResult implements Cloneable {

    public int launch;
    public int landing;
    
    public PadResult(int launch, int landing) {
      this.launch = launch;
      this.landing = landing;
    }
    
    @Override
    public String toString() {
      return ("PadResult launch = " + launch + " landing = " + landing);
    }
   
    @Override
    public PadResult clone() {
      try {
        PadResult retval = (PadResult)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Used to return pad usage data for nodes
  */
  
  public static class PadUsage {
    
    public Integer sourcePad;
    public Set<Integer> targPads;
    
    public PadUsage() {
      this.targPads = new HashSet<Integer>();
    } 
  }
  
  /***************************************************************************
  **
  ** Used to cache pad usage data for nodes. Not checking correctness, since that involves knowing about
  ** shared namespace stuff, etc.
  */
  
  public static class PadCache {
    
    private Map<String, PadUsage> map_;
    
    public PadCache() {
      map_ = new HashMap<String, PadUsage>();
    } 
    
    public void addSource(String srcID, int srcPad)  {
      Integer launchObj = new Integer(srcPad);
      PadCalculatorToo.PadUsage usage = map_.get(srcID);
      if (usage == null) {
        usage = new PadCalculatorToo.PadUsage();
        map_.put(srcID, usage);
        usage.sourcePad = launchObj;
      } else if (usage.sourcePad != null) {
        if (!usage.sourcePad.equals(launchObj)) {
          throw new IllegalStateException();
        }
      } else {
        usage.sourcePad = launchObj;
      }
      return;
    }
    
    public void addTarget(String trgID, int trgPad)  {
      Integer landingObj = new Integer(trgPad);
      PadCalculatorToo.PadUsage usage = map_.get(trgID);
      if (usage == null) {
        usage = new PadCalculatorToo.PadUsage();
        map_.put(trgID, usage); 
      } 
      usage.targPads.add(landingObj);
      return;
    }
    
    public Integer getSource(String srcID)  {
      PadCalculatorToo.PadUsage usage = map_.get(srcID);
      if (usage == null) {
        return (null);
      }
      return (usage.sourcePad);  // May be null
    }
    
    public Set<Integer> getTargets(String trgID)  {
      PadCalculatorToo.PadUsage usage = map_.get(trgID);
      if (usage == null) {
        return (null);
      } 
      return (usage.targPads);
    }
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Used to store info for fixing I/O pad errors.
  */ 
    
  public static class IOFixup {
    String genomeID;
    String linkID;
    boolean isLaunchChange;
    int newPad;
    
    IOFixup(String genomeID, String linkID, boolean isLaunchChange, int newPad) {
      this.genomeID = genomeID;
      this.linkID = linkID;
      this.isLaunchChange = isLaunchChange;
      this.newPad = newPad;
    }
  }
  
  /***************************************************************************
  **
  ** Used to store info for upward pad syncing
  */ 
    
  public static class UpwardPadSyncData {

    HashMap<String, Map<Integer, Integer>> sourcePadsPerNode_;
    HashMap<String, List<LinkTargPad>> targetPadsPerNode_;
    
    public UpwardPadSyncData() {
      sourcePadsPerNode_ = new HashMap<String, Map<Integer, Integer>>();
      targetPadsPerNode_ = new HashMap<String, List<LinkTargPad>>();
    }
    
    //
    // Track launch pad usage
    //
    
    public void setForSource(String srcID, int launchPad, int weight) {
      Map<Integer, Integer> launches = sourcePadsPerNode_.get(srcID);
      if (launches == null) {
        launches = new HashMap<Integer, Integer>();
        sourcePadsPerNode_.put(srcID, launches);
      }
      Integer padKey = new Integer(launchPad);
      Integer weightForPad = launches.get(padKey);
      Integer nextWeight;
      if (weightForPad == null) {
        nextWeight = new Integer(weight);
      } else {
        int currWeight = weightForPad.intValue();
        if (currWeight == Integer.MAX_VALUE) {
          nextWeight = weightForPad;
        } else {
          nextWeight = new Integer(currWeight + weight);
        }
      }
      launches.put(padKey, nextWeight);
      return;
    }

    //
    // Record the landing pad requests for the target.
    //    
 
    public void setForTarget(String linkID, String trgID, int landingPad) {
      List<LinkTargPad> landings = targetPadsPerNode_.get(trgID);
      if (landings == null) {
        landings = new ArrayList<LinkTargPad>();
        targetPadsPerNode_.put(trgID, landings);
      } 
      landings.add(new LinkTargPad(linkID, trgID, landingPad));
      return;
    }    
  }   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
 /***************************************************************************
  **
  ** Figure out the source pad to use for a node (upwardSync)
  */
    
  private Integer calcSourcePad(UpwardPadSyncData upsd, String srcID) {
    //
    // Use the pad that has the highest weight.
    //

    Map<Integer, Integer> launches = upsd.sourcePadsPerNode_.get(srcID);
    if (launches == null) {
      return (null);
    }
    int maxWeight = Integer.MIN_VALUE;
    Integer winPad = null;
    Iterator<Integer> kit = launches.keySet().iterator();
    while (kit.hasNext()) {
      Integer pad = kit.next();
      Integer weight = launches.get(pad);
      if (weight.intValue() > maxWeight) {
        winPad = pad;
        maxWeight = weight.intValue();
      }
    }
    return (winPad);
  }
    
  /***************************************************************************
  **
  ** Figure out the target pads to use for a target node (upwardSync)
  */ 
    
  private void figureTargetPads(UpwardPadSyncData upsd, UpwardPadSyncData orphUpsd,
                                Genome genome, String nodeID, 
                                Map<Integer, NodeProperties.PadLimits> padLims, Integer srcPad, 
                                Map<String, Integer> landPads, boolean forceUnique) {
    //
    // Get the actual pad count for the node:
    //

    Node trgNode = genome.getNode(nodeID);
    int nodeType = trgNode.getNodeType();
    NodeProperties.PadLimits padLimits = padLims.get(new Integer(nodeType));  
    int newPadCount = trgNode.getPadCount();
    int minPadNum = 0;
    if (newPadCount > padLimits.defaultPadCount) {  // we have extra pads:
      minPadNum = padLimits.defaultPadCount - newPadCount;
    }
    if ((minPadNum < 0) && !padLimits.landingPadsCanOverflow) {
      throw new IllegalStateException();
    }
    
    boolean doNotChange = false;
    if (nodeType == Node.GENE) {
      Gene trgGene = (Gene)trgNode;
      if (trgGene.getNumRegions() > 0) {
        doNotChange = true;
      }
    }
    
    //
    // Figure out what nobody has asked for:
    //

    TreeSet<Integer> trgPads = new TreeSet<Integer>();
    for (int i = minPadNum; i < padLimits.landingPadMax; i++) {
      trgPads.add(new Integer(i));
    }
    if (padLimits.sharedNamespace && (srcPad != null)) {
      trgPads.remove(srcPad);
    }

    List<LinkTargPad> landings = upsd.targetPadsPerNode_.get(nodeID);
    if (landings == null) {
      landings = new ArrayList<LinkTargPad>();
    }
    List<LinkTargPad> orphLandings = orphUpsd.targetPadsPerNode_.get(nodeID);
    if (orphLandings != null) {
      landings.addAll(orphLandings);
    }    
    
    int numLand = landings.size();
    for (int i = 0; i < numLand; i++) {
      LinkTargPad ltp = landings.get(i);
      trgPads.remove(new Integer(ltp.landPad));
    }

    //
    // Go through the requests for the links and hand them out.  If 
    // we have already handed out the pad, find a new one.
    //

    HashSet<Integer> assigned = new HashSet<Integer>();
    boolean skipSrc = (padLimits.sharedNamespace && (srcPad != null));
    if (skipSrc) {
      assigned.add(srcPad);
    }

    for (int i = 0; i < numLand; i++) {
      LinkTargPad ltp = landings.get(i);
      Integer lp = new Integer(ltp.landPad);
      // no problems
      if ((!assigned.contains(lp)) || doNotChange) {
        landPads.put(ltp.linkID, lp);
        assigned.add(lp);
        continue;
      }
      boolean srcCollide = (skipSrc) && (lp.equals(srcPad));
      if (srcCollide || forceUnique) {
        int landPad;
        if (srcCollide && trgPads.isEmpty()) {
          landPad = getClosestOccupiedNonSource(ltp.linkID, ltp.landPad, padLimits, srcPad, minPadNum);
        } else if (!trgPads.isEmpty()) {
          landPad = getClosestUnoccupied(ltp.landPad, trgPads);
        } else {
          landPad = ltp.landPad;
        }
        lp = new Integer(landPad);
        trgPads.remove(lp);
      }
      assigned.add(lp);
      landPads.put(ltp.linkID, lp);
    }      
    return;
  }     
    
  /***************************************************************************
  **
  ** For a given node, suggest the best launch pad
  */
  
  private SortedSet<PadDotRanking> getBestLaunch(DataAccessContext rcx, String srcID) {
    
    //
    // Go through all targets of this node, find the closest one, and suggest a launch
    // pad that's closest.
    //
    
    double minDist = Double.POSITIVE_INFINITY;
    Point2D minDistPt = null;
    NodeProperties srcProp = rcx.getLayout().getNodeProperties(srcID);
    Point2D srcLoc = srcProp.getLocation();
    
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (!link.getSource().equals(srcID)) {
        continue;
      }
      String trgID = link.getTarget();
      Point2D trgLoc = rcx.getLayout().getNodeProperties(trgID).getLocation();
      Vector2D vec = new Vector2D(srcLoc, trgLoc);
      double vecLen = vec.length();
      if (vecLen < minDist) {
        minDist = vecLen;
        minDistPt = trgLoc;
      }
    }
    if (minDistPt == null) {
      return (null);
    }
    
    return (srcProp.getRenderer().suggestLaunchPad(rcx.getGenome().getNode(srcID), rcx.getLayout(), minDistPt));
  }

  /***************************************************************************
  **
  ** For a given node, suggest the best landing pads
  */
  
  private Map<String, SortedSet<PadDotRanking>> getBestLandings(DataAccessContext rcx, String nodeID) {
        
    //
    // For sources to this node, find the closest of sources and siblings and put the
    // landing pad there.  If less than the maximum number of pads, we do not share.
    //
    
    NodeProperties myProp = rcx.getLayout().getNodeProperties(nodeID);
    Point2D myLoc = myProp.getLocation();
    
    //
    // Who are my sources?  How far are they?
    //
    
    HashMap<String, DistanceRank> srcToDist = new HashMap<String, DistanceRank>();
    Iterator<Linkage> lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String trgID = link.getTarget();
      if (!trgID.equals(nodeID)) {
        continue;
      }
      String srcID = link.getSource();
      Point2D srcLoc = rcx.getLayout().getNodeProperties(srcID).getLocation();
      Vector2D vec = new Vector2D(myLoc, srcLoc);
      double vecLen = vec.length();
      DistanceRank rank = new DistanceRank(srcID, vecLen);
      srcToDist.put(srcID, rank);
    }
    
    //
    // Are my siblings closer, but only if that sibling is itself lots closer to the target
    // than me
    //
    
    Set<String> sourceSet = srcToDist.keySet();
    lit = rcx.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      if (!sourceSet.contains(srcID)) {
        continue;
      }
      String trgID = link.getTarget();
      if (trgID.equals(nodeID)) {
        continue;
      }
      Point2D trgLoc = rcx.getLayout().getNodeProperties(trgID).getLocation();
      Vector2D vec = new Vector2D(myLoc, trgLoc);
      double vecLen = vec.length();
      DistanceRank rank = srcToDist.get(srcID);
      if (rank.distance > vecLen) {
        Point2D srcLoc = rcx.getLayout().getNodeProperties(srcID).getLocation();
        Vector2D vecSt = new Vector2D(srcLoc, trgLoc);
        Vector2D vecMy = new Vector2D(srcLoc, myLoc);
        // Sibling must be significantly closer to source than me to win out
        if ((1.25 * vecSt.length()) < vecMy.length()) {  // 1.25 = Magic number
          rank.distance = vecLen;
          rank.id = trgID;
        }
      }
    }
    
    //
    // Build the map we are handing in
    //
    
    HashMap<String, SortedSet<PadDotRanking>> ptForSrc = new HashMap<String, SortedSet<PadDotRanking>>();
    INodeRenderer renderer = myProp.getRenderer();
    Node node = rcx.getGenome().getNode(nodeID);
    Iterator<String> kit = srcToDist.keySet().iterator();
    while (kit.hasNext()) {
      String srcKey = kit.next();
      DistanceRank rank = srcToDist.get(srcKey);
      Point2D srcLoc = rcx.getLayout().getNodeProperties(rank.id).getLocation();
      SortedSet<PadDotRanking> pads = renderer.suggestLandingPads(node, rcx.getLayout(), srcLoc);
      ptForSrc.put(srcKey, pads);
    }
    
    return (ptForSrc);
  } 
  
  /***************************************************************************
  **
  ** Resolve landing pad collisions
  */
  
  private Map<String, PadResult> resolvePadCollisions(DataAccessContext rcx, String nodeID, SortedSet<PadDotRanking> bestLaunch, 
                                                      Map<String, SortedSet<PadDotRanking>> padsForSrc, 
                                                      Map<String, PadConstraints> padConstraints) {
    
    HashMap<String, Integer> retLaunch = new HashMap<String, Integer>();
    HashMap<String, Integer> retLand = new HashMap<String, Integer>();
    
    //
    // Build a model of the node pads:
    //
    
    PadModel model = new PadModel(nodeID, rcx.getLayout());

    //
    // Go through all the links, and for those that impinge on the node,
    // check contraints to see if they are forced.  If so, delete from the
    // pad model.  If not add to source and target waiting lists.  If a source
    // is forced, we drop the source list and apply that source to all the links.
    //
    
    ArrayList<String> waitingSource = new ArrayList<String>();
    ArrayList<String> waitingTarget = new ArrayList<String>();

    resolveForcedLaunch(rcx.getGenome(), nodeID, padConstraints, model, retLaunch, waitingSource);
    resolveForcedLanding(rcx.getGenome(), nodeID, padConstraints, model, retLand, waitingTarget);
    
    assignBestLaunch(rcx.getGenome(), nodeID, model, retLaunch, retLand, 
                     bestLaunch, waitingSource, waitingTarget);    
    assignBestLanding(rcx.getGenome(), model, retLand, padsForSrc, waitingTarget);
    
    //
    // Build up the results:
    //
    
    HashMap<String, PadResult> retval = new HashMap<String, PadResult>();
    Set<String> launchKeys = retLaunch.keySet();
    HashSet<String> allKeys = new HashSet<String>(launchKeys);
    allKeys.addAll(retLand.keySet());

    Iterator<String> kit = allKeys.iterator();
    while (kit.hasNext()) {
      String linkID = kit.next();
      Integer launch = retLaunch.get(linkID);
      int launchPad = (launch == null) ? rcx.getGenome().getLinkage(linkID).getLaunchPad() : launch.intValue();
      Integer land = retLand.get(linkID);
      int landPad = (land == null) ? rcx.getGenome().getLinkage(linkID).getLandingPad() : land.intValue();
      retval.put(linkID, new PadResult(launchPad, landPad));
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle forced launch pad assignments
  */
  
  private void resolveForcedLaunch(Genome genome, String nodeID, Map<String, PadConstraints> padConstraints, 
                                   PadModel model, Map<String, Integer> retLaunch, List<String> waitingSource) {
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(nodeID)) {
        String linkID = link.getID();
        PadConstraints pc = padConstraints.get(linkID);
        if ((pc != null) && pc.arePadsForced()) {
          int launchPad = pc.getForcedLaunch();
          Integer res = new Integer(launchPad);
          retLaunch.put(linkID, res);
          if (model.launchPad != null) {
            if (model.launchPad.intValue() != launchPad) {
              System.err.println("linkID = " + linkID + " modelLaunch = " + model.launchPad + " forced = " + launchPad);
              throw new IllegalStateException();
            }
          } else {
            model.launchPad = new Integer(launchPad);
            if (model.sharedNamespaces) {
              model.trgPads.remove(model.launchPad);
            }
          }
          Iterator<String> wsit = waitingSource.iterator();
          while (wsit.hasNext()) {
            String waitingLink = wsit.next();
            retLaunch.put(waitingLink, model.launchPad);
          }
          waitingSource.clear();
        } else if (model.launchPad != null) {
          retLaunch.put(linkID, model.launchPad);
        } else {
          waitingSource.add(linkID);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle forced landing pad assignments
  */
  
  private void resolveForcedLanding(Genome genome, String nodeID,  Map<String, PadConstraints> padConstraints, 
                                    PadModel model, Map<String, Integer> retLanding, List<String> waitingTarget) {
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getTarget().equals(nodeID)) {
        String linkID = link.getID();
        PadConstraints pc = padConstraints.get(linkID);
        if ((pc != null) && pc.arePadsForced()) {
          int landingPad = pc.getForcedLanding();
          Integer res = new Integer(landingPad);
          retLanding.put(linkID, res);
          if ((model.launchPad != null) && 
              model.sharedNamespaces && 
              (model.launchPad.intValue() == landingPad)) {
              throw new IllegalStateException();
          }
          if (landingPad >= 0) {
            model.trgPads.remove(new Integer(landingPad));
          } else {
            model.overflow.add(new Integer(landingPad));
          }
        } else {
          waitingTarget.add(linkID);
        }
      }
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Assign best launch
  */
  
  private void assignBestLaunch(Genome genome, String nodeID, 
                                PadModel model, Map<String, Integer> retLaunch, 
                                Map<String, Integer> retLanding, 
                                SortedSet<PadDotRanking> bestLaunch, List<String> waitingSource, 
                                List<String> waitingTarget) {

    //
    // At this point, we know what links need assignments, we may have launch pads already
    // assigned, and the model has been reduced by the used pads.  Now, if we have a best
    // launch given to us, and no forced assignment, assign waiting links that launch.
    // If we are in shared namespaces, we have to make sure the desired slot is available.
    // If not, find the closest.
    
    if ((model.launchPad == null) && (bestLaunch != null)) {
      Integer chosenLaunch = null;
      if (model.sharedNamespaces) {
        Iterator<PadDotRanking> blit = bestLaunch.iterator();
        while (blit.hasNext()) {
          PadDotRanking pdr = blit.next();
          Integer launchPad = new Integer(pdr.padNum);
          if (model.trgPads.contains(launchPad)) {
            model.trgPads.remove(launchPad);
            chosenLaunch = launchPad;
            break;
          }
        }
      } else {
        chosenLaunch = new Integer(bestLaunch.first().padNum);
      }
      
      //
      // We MUST have a unique launch pad.  If forced landings hog them all, we
      // trash them!
      //
      
      if (chosenLaunch == null) {
        chosenLaunch = new Integer(bestLaunch.first().padNum);
        backOutForcedLanding(genome, nodeID, retLanding, chosenLaunch, waitingTarget);
      }
      
      Iterator<String> wsit = waitingSource.iterator();
      while (wsit.hasNext()) {
        String waitingLink = wsit.next();
        retLaunch.put(waitingLink, chosenLaunch);
      }
      model.launchPad = chosenLaunch;
      waitingSource.clear();
    }                                   
    return;
  }    
  /***************************************************************************
  **
  ** Assign best landing
  */
  
  @SuppressWarnings("unused")
  private void assignBestLandingOrig(Genome genome, PadModel model, Map<String, Integer> retLanding, 
                                     Map<String, Set<PadDotRanking>> padsForSrc, List<String> waitingTarget) {
    
    //
    // Go through the waiting target links.  Get the link and find the source, then use
    // the pad preference sets to those links to figure out where to go. 
    //
                                   
    //
    // FIX ME!! This is far from optimal!  A better idea would be:
    //
    // Go through first choices and assign based on highest score.  If tied, use the
    // one with the poorer score on the next choice.  If still tied, use arbitrary tie-breaker
    // (e.g. linkID).  After assignment, throw away assigned link request, and upgrade those
    // who lost tie so that their second choice becomes their first.
    //                                    
                                                                 
    Iterator<String> wtit = waitingTarget.iterator();
    while (wtit.hasNext()) {
      String linkID = wtit.next();
      Linkage link = genome.getLinkage(linkID);
      String srcID = link.getSource();
      Set<PadDotRanking> padPrefs = padsForSrc.get(srcID);
      Iterator<PadDotRanking> ppit = padPrefs.iterator();
      Integer bestNonLaunch = null;
      boolean gottaPad = false;
      while (ppit.hasNext()) {
        PadDotRanking pdr = ppit.next();
        Integer landingPad = new Integer(pdr.padNum);
        if (pdr.padNum >= 0) {
          if (model.trgPads.contains(landingPad)) {
            model.trgPads.remove(landingPad);
            retLanding.put(linkID, landingPad);
            gottaPad = true;
            break;
          }
        } else {
          if (!model.overflow.contains(landingPad)) {
            model.overflow.add(landingPad);
            retLanding.put(linkID, landingPad);
            gottaPad = true;
            break;
          }
        }
        if (bestNonLaunch == null) {
          if ((model.launchPad == null) || (!model.launchPad.equals(landingPad))) {
            bestNonLaunch = landingPad;
          }
        }
      }
      
      //
      // Everybody in use?  Just use first choice that is not the
      // launch pad
      //
      if (!gottaPad) {
        if (bestNonLaunch == null) {
          throw new IllegalStateException();
        }
        retLanding.put(linkID, bestNonLaunch);
      }
    }

    return;
  }

  /***************************************************************************
  **
  ** Assign best landing
  */
  
  private void assignBestLanding(Genome genome, PadModel model, Map<String, Integer> retLanding, 
                                 Map<String, SortedSet<PadDotRanking>> padsForSrc, List<String> waitingTarget) {                     

    PadAssign assign = buildPadAssign(genome, model, retLanding, padsForSrc, waitingTarget);                                   
    
    List<PadChoice> assignments = assign.getAssignments();
    int size = assignments.size();
    for (int i = 0; i < size; i++) {
      PadChoice pc = assignments.get(i);
      Integer pad = new Integer(pc.padNum);
      retLanding.put(pc.linkID, pad);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Build a landing pad assignment widget
  */
  
  private PadAssign buildPadAssign(Genome genome, PadModel model, Map<String, Integer> retLanding, 
                                   Map<String, SortedSet<PadDotRanking>> padsForSrc, List<String> waitingTarget) {
    
    PadAssign retval = new PadAssign();                          
                                                                                                     
    Iterator<String> wtit = waitingTarget.iterator();
    while (wtit.hasNext()) {
      String linkID = wtit.next();
      Linkage link = genome.getLinkage(linkID);
      String srcID = link.getSource();
      SortedSet<PadDotRanking> padPrefs = padsForSrc.get(srcID);
      Iterator<PadDotRanking> ppit = padPrefs.iterator();
      int choiceRank = 0;
      while (ppit.hasNext()) {
        PadDotRanking pdr = ppit.next();
        Integer landingPad = new Integer(pdr.padNum); 
        //
        // Don't stash pad values for occupied pads:
        //
        if ((model.launchPad != null) && (model.launchPad.equals(landingPad))) {
          continue;       
        } else if (pdr.padNum >= 0) {
          if (!model.trgPads.contains(landingPad)) {
            continue;
          }
        } else {
          if (model.overflow.contains(landingPad)) {
            continue;
          }
        }
        PadChoice pc = new PadChoice(linkID, pdr.padNum, choiceRank++, pdr.dot);       
        retval.register(pc); 
      }
    }
    return (retval);
  }   

  /***************************************************************************
  **
  ** Handle forced landing pad assignments
  */
  
  private void backOutForcedLanding(Genome genome, String nodeID, 
                                    Map<String, Integer> retLanding, Integer forcedLaunch,
                                    List<String> waitingTarget) {
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getTarget().equals(nodeID)) {
        String linkID = link.getID();
        Integer pad = retLanding.get(linkID);
        if (pad.equals(forcedLaunch)) {
          retLanding.remove(linkID);
          waitingTarget.add(linkID);
        }
      }
    }
    return;
  }   


 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static class LinkTargPad {
    
    String linkID;
    String targID;
    int landPad;
        
    LinkTargPad(String linkID, String targID, int landPad) {
      this.linkID = linkID;
      this.targID = targID;
      this.landPad = landPad;      
    }
  }    

  private class DistanceRank implements Comparable<DistanceRank> {
    
    String id;
    double distance;
        
    DistanceRank(String id, double distance) {
      this.id = id;
      this.distance = distance;
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
  
  private class PadModel {
    
    boolean sharedNamespaces;
    boolean canOverflow;
    Integer launchPad;
    TreeSet<Integer> trgPads;
    TreeSet<Integer> overflow;
        
    PadModel(String nodeID, Layout lo) {
      INodeRenderer trgRenderer = lo.getNodeProperties(nodeID).getRenderer();
         
      sharedNamespaces = trgRenderer.sharedPadNamespaces();
      canOverflow = trgRenderer.landingPadsCanOverflow();
      
      int targMax = trgRenderer.getFixedLandingPadMax();
      trgPads = new TreeSet<Integer>();
      for (int i = 0; i < targMax; i++) {
        trgPads.add(new Integer(i));
      }                  
      if (canOverflow) {      
        overflow = new TreeSet<Integer>();
      }
      launchPad = null;
    }
  } 
  
  private class PadChoice {
    
    String linkID;
    int padNum;
    int choiceRank;
    double dot;
        
    PadChoice(String linkID, int padNum, int choiceRank, double dot) {
      this.linkID = linkID;
      this.padNum = padNum;
      this.choiceRank = choiceRank;
      this.dot = dot;     
    }
    
    public String toString() {
      return ("linkID = " + linkID + " padNum = " + padNum + 
              " choiceRank = " + choiceRank + " dot = " + dot);
    }
  } 
  
  private class PadAssign {
    
    HashMap<Integer, List<PadChoice>> requests;
    HashSet<Integer> unassigned;
        
    PadAssign() {
      requests = new HashMap<Integer, List<PadChoice>>();
      unassigned = new HashSet<Integer>();
    }
    
    void register(PadChoice choice) {
      Integer landingPad = new Integer(choice.padNum);
      List<PadChoice> padChoices = requests.get(landingPad);
      if (padChoices == null) {
        padChoices = new ArrayList<PadChoice>();
        requests.put(landingPad, padChoices);
      }
      padChoices.add(choice);   
      return;
    }
    
    //
    // Get the next assignment.  Find the highest dot among the
    // unassigned pads.  If more than one, find the highest dot
    // with the lowest dot for second choice, etc.  Assign that
    // pad.
    //
        
    List<PadChoice> getAssignments() {
      ArrayList<PadChoice> retval = new ArrayList<PadChoice>();
      while (needAssignment()) {
        if (unassigned.isEmpty()) {
          unassigned = new HashSet<Integer>(requests.keySet());
        }
        PadChoice next = findTopUnassignedChoice();
        retval.add(next);
        flushRequests(next);
        unassigned.remove(new Integer(next.padNum));
      }
      return (retval);
    }

    void flushRequests(PadChoice choice) {
      Iterator<Integer> rkit = requests.keySet().iterator();
      while (rkit.hasNext()) {
        Integer padNum = rkit.next();
        List<PadChoice> padChoices = requests.get(padNum);
        int count = 0;
        while (count < padChoices.size()) {
          PadChoice pc = padChoices.get(count);
          if (pc.linkID.equals(choice.linkID)) {
            padChoices.remove(count);
          } else {
            count++;
          }
        }
      }
    }
    
    boolean needAssignment() {
      Iterator<Integer> rkit = requests.keySet().iterator();
      while (rkit.hasNext()) {
        Integer padNum = rkit.next();
        List<PadChoice> padChoices = requests.get(padNum);
        if (!padChoices.isEmpty()) {
          return (true);
        }
      }
      return (false);
    }
    

    PadChoice findNextInLine(PadChoice choice) {
      int lookingForRank = choice.choiceRank + 1;
      Iterator<Integer> uait = unassigned.iterator();
      while (uait.hasNext()) {
        Integer padNum = uait.next();
        List<PadChoice> padChoices = requests.get(padNum);
        int pcsize = padChoices.size();
        for (int i = 0; i < pcsize; i++) {
          PadChoice pc = padChoices.get(i);
          if (pc.linkID.equals(choice.linkID) && (pc.choiceRank == lookingForRank)) {
            return (pc);
          }
        }
      }
      return (null);
    }
      
    PadChoice findTopUnassignedChoice() {
      double topDot = Double.NEGATIVE_INFINITY;
      PadChoice currentTop = null;
      
      Iterator<Integer> uait = unassigned.iterator();
      while (uait.hasNext()) {
        Integer padNum = uait.next();
        List<PadChoice> padChoices = requests.get(padNum);
        int pcsize = padChoices.size();
        for (int i = 0; i < pcsize; i++) {
          PadChoice pc = padChoices.get(i);
          if ((pc.dot > topDot) || (currentTop == null)) {
            currentTop = pc;
            topDot = pc.dot;
          } else if (pc.dot == topDot) {
            currentTop = breakDotTie(currentTop, pc);
          }
        }
      }
      return (currentTop);
    }
    
    PadChoice breakDotTie(PadChoice pc1, PadChoice pc2) {
      PadChoice next1 = findNextInLine(pc1);
      PadChoice next2 = findNextInLine(pc2);
      if ((next1 != null) && (next2 != null)) {
        if (next1.dot < next2.dot) {
          return (pc1);
        } else if (next1.dot > next2.dot) {
          return (pc2);
        } else {
          return (breakDotTie(next1, next2));
        }
      } else if ((next1 == null) && (next2 == null)) {
        int compVal = pc1.linkID.compareTo(pc2.linkID);
        return ((compVal < 0) ? pc1 : pc2);
      } else {
        return ((next1 == null) ? pc1 : pc2);
      }
    }
  } 
}
