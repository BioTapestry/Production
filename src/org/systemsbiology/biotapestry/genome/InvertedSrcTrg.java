/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.genome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.ui.layouts.GenomeSubset;
import org.systemsbiology.biotapestry.util.DataUtil;

/***************************************************************************
**
** Lots of layout operations want to know the sources and targets of nodes,
** or the counts of links in and out of nodes. Given the way the info is stored,
** those are expensive questions to do on a one-off basis for each node. Do the
** calcs once, hold on to the info, and pass it around.
*/

public class InvertedSrcTrg {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private HashMap<String, Set<String>> inboundLinks_;
  private HashMap<String, Set<String>> outboundLinks_;
  
  private HashMap<String, Integer> inboundCounts_;
  private HashMap<String, Set<String>> targets_;
  private HashMap<String, Integer> outboundCounts_;
  private HashMap<String, Set<String>> sources_;
  private HashSet<String> pureTargets_;
  private HashSet<String> pureSources_;
  private HashSet<String> singletons_;
  private HashMap<Link, Set<String>> between_;
  
  private HashMap<String, String> geneLookup_;
  private HashMap<String, Set<String>> nodeLookup_;
  private HashMap<String, String> keyLookup_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public InvertedSrcTrg(Genome genome) {
    HashSet<String> nodeIDs = new HashSet<String>();
    
    Iterator<Node> nit = genome.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      nodeIDs.add(node.getID());
    }
    buildIt(genome.getLinkageIterator(), nodeIDs.iterator());
    
    if (genome instanceof DBGenome) {
      buildNameLookup(genome);
    }
  } 
  
  /***************************************************************************
  ** 
  ** Constructor
  */
        
  public InvertedSrcTrg(GenomeSubset genomeSub) {
  
    ArrayList<Linkage> links = new ArrayList<Linkage>();
    Genome genome = genomeSub.getBaseGenome();
    Iterator<String> lit = genomeSub.getLinkageIterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      links.add(genome.getLinkage(linkID));
    }
 
    buildIt(links.iterator(), genomeSub.getNodeIterator());
  } 
 

  /***************************************************************************
  ** 
  ** Constructor that also caches node/gene name->key lookups
  */
        
  private void buildNameLookup(Genome genome) {
    if (!(genome instanceof DBGenome)) {
      throw new IllegalArgumentException();
    }
    
    keyLookup_ = new HashMap<String, String>();
    geneLookup_ = new HashMap<String, String>();
    nodeLookup_ = new HashMap<String, Set<String>>();
    
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String testName = gene.getName();
      if ((testName == null) || testName.trim().equals("")) {
        throw new IllegalStateException();
      }
      String testKey = DataUtil.normKey(testName);
      keyLookup_.put(testName, testKey);
      geneLookup_.put(testKey, gene.getID());
    }
 
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String testName = node.getName();
      if (testName == null) {
        testName = "";
      }
      String testKey = DataUtil.normKey(testName);
      Set<String> forName = nodeLookup_.get(testKey);
      if (forName == null) {
        forName = new HashSet<String>();
        nodeLookup_.put(testKey, forName);
      }
      keyLookup_.put(testName, testKey);
      forName.add(node.getID());
    }
  }  
 
  /***************************************************************************
  ** 
  ** Constructor guts
  */
        
  private void buildIt(Iterator<Linkage> liit, Iterator<String> nit) {
   
    inboundLinks_ = new HashMap<String, Set<String>>();
    outboundLinks_ = new HashMap<String, Set<String>>();   
    inboundCounts_ = new HashMap<String, Integer>(); 
    targets_ = new HashMap<String, Set<String>>();
    outboundCounts_ = new HashMap<String, Integer>(); 
    sources_ = new HashMap<String, Set<String>>();
    between_ = new HashMap<Link, Set<String>>();
     
    while (liit.hasNext()) {
      Linkage link = liit.next();
      addNewLinkGuts(link);
    }
    
    pureTargets_ = new HashSet<String>(); 
    pureSources_ = new HashSet<String>();
    singletons_ = new HashSet<String>();
    
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Integer oCount = outboundCounts_.get(nodeID);
      Integer iCount = inboundCounts_.get(nodeID);
      if (oCount == null) {
        if (iCount == null) {
          singletons_.add(nodeID);
        } else {
          pureTargets_.add(nodeID);
        }
        
      }
      if (iCount == null) {
        if (oCount == null) {
          singletons_.add(nodeID);
        } else {
          pureSources_.add(nodeID);
        }      
      }
    }
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Get matching geneID for name
  */
        
  public String getMatchingGene(String geneName) {
    if ((geneName == null) || geneName.trim().equals("")) {
      return (null);
    }
    return (geneLookup_.get(keyLookup_.get(geneName)));
  } 
  
  /***************************************************************************
  ** 
  ** Get matching nodeIDs for name
  */
        
  public Set<String> getMatchingNodes(String nodeName) {
    if ((nodeName == null) || nodeName.trim().equals("")) {
      nodeName = "";
    }
    Set<String> retval = nodeLookup_.get(keyLookup_.get(nodeName));
    return ((retval == null) ? new HashSet<String>() : retval);
  }

  /***************************************************************************
  **
  ** Update with new additions to the genome
  */
  
  public void addNewLink(Linkage link) {
    
    addNewLinkGuts(link);
    String src = link.getSource();
    String trg = link.getTarget();
    
    if (pureTargets_.contains(src)) {
      pureTargets_.remove(src);
    }
    
    if (pureSources_.contains(trg)) {
      pureSources_.remove(trg);
    }
    
    if (singletons_.contains(src)) {
      singletons_.remove(src);
    }
    
    if (singletons_.contains(trg)) {
      singletons_.remove(trg);
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Outbound links
  */
  
  public Set<String> outboundLinkIDs(String srcID) {
    Set<String> ss = outboundLinks_.get(srcID);
    return ((ss == null) ? new HashSet<String>() : ss); 
  }
  
  /***************************************************************************
  **
  ** Inbound links
  */
  
  public Set<String> inboundLinkIDs(String trgID) {
    Set<String> ts = inboundLinks_.get(trgID);
    return ((ts == null) ? new HashSet<String>() : ts); 
  }
   
  /***************************************************************************
  **
  ** Outbound link count (not target count)
  */
  
  public int outboundLinkCount(String srcID) {
    Integer ocobj = outboundCounts_.get(srcID);
    return ((ocobj == null) ? 0 : ocobj.intValue());  
  }
  
  /***************************************************************************
  **
  ** Targets
  */
  
  public Set<String> getTargets(String srcID) {
    Set<String> ts = targets_.get(srcID);
    return ((ts == null) ? new HashSet<String>() : ts); 
  }
  
  /***************************************************************************
  **
  ** Inbound link count (not source count)
  */
  
  public int inboundLinkCount(String trgID) {
    Integer icobj = inboundCounts_.get(trgID);
    return ((icobj == null) ? 0 : icobj.intValue());      
  }
  
  /***************************************************************************
  **
  ** Sources
  */
  
  public Set<String> getSources(String trgID) {
    Set<String> ss = sources_.get(trgID);
    return ((ss == null) ? new HashSet<String>() : ss); 
  }
  
  /***************************************************************************
  **
  ** Links between
  */
  
  public Set<String> getLinksBetween(String srcID, String trgID) {
    Link pair = new Link(srcID, trgID);
    Set<String> bt = between_.get(pair);
    return ((bt == null) ? new HashSet<String>() : bt); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Add new link
  */
  
  private void addNewLinkGuts(Linkage link) {
    String src = link.getSource();
    String trg = link.getTarget();
    String linkID = link.getID();
    
    Set<String> il = inboundLinks_.get(trg);
    if (il == null) {
      il = new HashSet<String>();
      inboundLinks_.put(trg, il);
    }
    il.add(linkID);
    
    Integer ic = inboundCounts_.get(trg);
    if (ic == null) {
      inboundCounts_.put(trg, new Integer(1));
    } else {
      inboundCounts_.put(trg, new Integer(ic.intValue() + 1));
    }
    
    Set<String> ol = outboundLinks_.get(src);
    if (ol == null) {
      ol = new HashSet<String>();
      outboundLinks_.put(src, ol);
    }
    ol.add(linkID);
    
    Integer oc = outboundCounts_.get(src);
    if (oc == null) {
      outboundCounts_.put(src, new Integer(1));
    } else {
      outboundCounts_.put(src, new Integer(oc.intValue() + 1));
    }
    
    Set<String> targsForSrc = targets_.get(src);
    if (targsForSrc == null) {
      targsForSrc = new HashSet<String>();
      targets_.put(src, targsForSrc);
    }
    targsForSrc.add(trg);
    
    Set<String> srcsForTarg = sources_.get(trg);
    if (srcsForTarg == null) {
      srcsForTarg = new HashSet<String>();
      sources_.put(trg, srcsForTarg);
    }
    srcsForTarg.add(src);
    
    Link pair = new Link(src, trg);
    Set<String> betForPair = between_.get(pair);
    if (betForPair == null) {
      betForPair = new HashSet<String>();
      between_.put(pair, betForPair);
    }
    betForPair.add(linkID);
    return; 
  }
  
} 
