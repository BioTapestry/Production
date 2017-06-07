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

package org.systemsbiology.biotapestry.simulation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.SifSupport;
import org.systemsbiology.biotapestry.simulation.*;
import org.systemsbiology.biotapestry.timeCourse.ExpressionEntry;
import org.systemsbiology.biotapestry.timeCourse.GeneTemplateEntry;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.util.DataUtil;


/****************************************************************************
**
** API for extracting model data
*/

public class ModelSourceImpl implements ModelSource {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private Genome genome_;
  private TimeCourseData tcd_;
  private HarnessBuilder hBld_;
  private DataAccessContext dacx_;
  private Map<String, String> nodeIDsToUniqueNames_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelSourceImpl(DataAccessContext dacx, HarnessBuilder hBld)  {
    hBld_ = hBld;
    dacx_ = dacx;
    genome_ = dacx_.getDBGenome();
    tcd_ = dacx_.getExpDataSrc().getTimeCourseData();
    nodeIDsToUniqueNames_ = new HashMap<String, String>();
    
    buildUniqueNodeNames();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
	private void buildUniqueNodeNames() {
		DBGenome dbgenome = (DBGenome)genome_;
		SifSupport sfs = new SifSupport(dbgenome);
	
		Iterator<Node> git = genome_.getAllNodeIterator();
		while (git.hasNext()) {
			Node node = git.next();
			sfs.sifNodeName(node, nodeIDsToUniqueNames_, genome_);
		}
	}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<ModelNode> getRootModelNodes() {	
    HashSet<ModelNode> nodeSet = new HashSet<ModelNode>();
    Iterator<Node> git = genome_.getAllNodeIterator();
    while (git.hasNext()) {
      Node node = git.next();
      String name = node.getName();
      String id = node.getID();
      String uniqueName = nodeIDsToUniqueNames_.get(id);
      ModelNode.Type type = mapToType(node.getNodeType());
      ModelNode mNode = new ModelNode(id, name, uniqueName, type);
      nodeSet.add(mNode);
    }
    return (nodeSet.iterator()); 
  }
  
  
  /***************************************************************************
  **
  ** Get the nodes
  */
  
  public Iterator<ModelLink> getRootModelLinks() {
    HashSet<ModelLink> linkSet = new HashSet<ModelLink>();
    Iterator<Linkage> lit = genome_.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      String trgID = link.getTarget();
      String id = link.getID();
      ModelLink.Sign sign = mapToSign(link.getSign());
      ModelLink mLink = new ModelLink(id, srcID, trgID, sign);
      linkSet.add(mLink);
    }
    return (linkSet.iterator()); 
  }
  
  /***************************************************************************
  **
  ** Get a node
  */
  
  public ModelNode getNode(String nodeID) {
    Node node = genome_.getNode(nodeID);
    String name = node.getName();
    String id = node.getID();
    String uniqueName = nodeIDsToUniqueNames_.get(id);
    ModelNode.Type type = mapToType(node.getNodeType());
    ModelNode mNode = new ModelNode(id, name, uniqueName, type);
    return (mNode);
  }
  
  /***************************************************************************
  **
  ** Get region topology
  */
  
  public Iterator<ModelRegionTopologyForTime> getRegionTopology() {
    ArrayList<ModelRegionTopologyForTime> retval = new ArrayList<ModelRegionTopologyForTime>();
    Iterator<TimeCourseData.TopoTimeRange> ttrit = tcd_.getRegionTopologyTimes();
    while (ttrit.hasNext()) {
      TimeCourseData.TopoTimeRange ttr = ttrit.next();
      TimeCourseData.RegionTopology rt = tcd_.getRegionTopology(ttr);
      
      ArrayList<ModelRegionTopologyForTime.TopoLink> lret = new ArrayList<ModelRegionTopologyForTime.TopoLink>();
      Iterator<TimeCourseData.TopoLink> tlit = rt.getLinks();
      while (tlit.hasNext()) {
        TimeCourseData.TopoLink tl = tlit.next();
        ModelRegionTopologyForTime.TopoLink tlo = new ModelRegionTopologyForTime.TopoLink(tl.region1, tl.region2);
        lret.add(tlo);
      }
      ArrayList<String> rret = new ArrayList<String>();
      Iterator<String> rit = rt.getRegions();
      while (rit.hasNext()) {
        String reg = rit.next();
        rret.add(reg);
      }
      ModelRegionTopologyForTime mrt4t = new ModelRegionTopologyForTime(rt.times.minTime, rt.times.maxTime, rret, lret);
      retval.add(mrt4t);
    }
    return (retval.iterator());
  }
  
  /***************************************************************************
  **
  ** Returns the region hierarchy
  */
  
  public List<ModelRegion> getRegions() {
    ArrayList<ModelRegion> retval = new ArrayList<ModelRegion>();
    
    //
    // Get the list of all the time-bounded regions we need to analyze:
    //
    
    SortedSet<String> regions = new TreeSet<String>(tcd_.getRegions());
    
    Iterator<String> arit = regions.iterator();
    while (arit.hasNext()) {
      String regID = arit.next();
      SortedSet<Integer> times = tcd_.hoursForRegion(regID);
      List<TimeCourseData.TimeBoundedRegion> lineage = tcd_.genRegionLineage(regID);
      ArrayList<String> linNames = new ArrayList<String>();
      for (TimeCourseData.TimeBoundedRegion tbr : lineage) {
        linNames.add(0, tbr.region);
      }     
      retval.add(new ModelRegion(times, regID, linNames));
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Returns available gene names for expression data
  */
  
  public Set<String> getExpressionGenes() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<TimeCourseGene> tcgit = tcd_.getGenes();
    while (tcgit.hasNext()) {
      retval.add(tcgit.next().getName());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Returns available times for expression data
  */
  
  public SortedSet<Integer> getExpressionTimes() {
    Set<Integer> times = tcd_.getInterestingTimes();
    return (DataUtil.fillOutHourly(new TreeSet<Integer>(times)));
  }
             
  /***************************************************************************
  **
  ** Returns expression entries. If no entry for the given time/region, returns null
  */
  
  public ModelExpressionEntry getExpressionEntry(String geneName, String region, int time) {
    Set<Integer> trueTimes = tcd_.getInterestingTimes();
    TimeCourseGene tcg = tcd_.getTimeCourseData(geneName);
    TimeCourseGene.VariableLevel vl = new TimeCourseGene.VariableLevel();
    int exLev = tcg.getExpressionLevelForSource(region, time, ExpressionEntry.Source.NO_SOURCE_SPECIFIED, vl);
    if (exLev == ExpressionEntry.NO_REGION) {
      return (null);
    }
    ExpressionEntry.Source src = (trueTimes.contains(Integer.valueOf(time))) ? tcg.getExprSource(region, time) : ExpressionEntry.Source.NO_SOURCE_SPECIFIED;
    Double useVar = (exLev == ExpressionEntry.VARIABLE) ? Double.valueOf(vl.level) : null;
    ModelExpressionEntry mee = new ModelExpressionEntry(region, time, mapExpression(exLev), mapSource(src), useVar);
    return (mee);
  }

  /***************************************************************************
  **
  ** Load in results
  */
  
  public void handleResults(Map<String, List<ModelExpressionEntry>> results) {
   
    ArrayList<GeneTemplateEntry> gteList = new ArrayList<GeneTemplateEntry>();
    HashMap<GeneTemplateEntry, ExpressionEntry> gteMap = new HashMap<GeneTemplateEntry, ExpressionEntry>();
    Iterator<GeneTemplateEntry> gtit = tcd_.getGeneTemplate();
    while (gtit.hasNext()) {
      GeneTemplateEntry gte = gtit.next();
      gteList.add(gte);
    }
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    String skey = tcd.getNewSimKey();
    for (String gene : results.keySet()) {
      TimeCourseGene tcg = tcd.getTimeCourseDataCaseInsensitive(gene);
      if (tcg == null) {
        tcg = new TimeCourseGene(dacx_, gene, TimeCourseGene.NORMAL_CONFIDENCE, false, false);
        tcd.addGene(tcg);
      }
      List<ModelExpressionEntry> forGene = results.get(gene);
      for (ModelExpressionEntry mee : forGene) {
        ExpressionEntry ee = new ExpressionEntry(mee.getRegion(), mee.getTime(), mapFromExpression(mee.getLevel()), TimeCourseGene.USE_BASE_CONFIDENCE);
        GeneTemplateEntry key = new GeneTemplateEntry(mee.getTime(), mee.getRegion());
        gteMap.put(key, ee);         
      }
      for (GeneTemplateEntry gte : gteList) {
        ExpressionEntry ee = gteMap.get(gte);
        if (ee == null) {
          System.err.println("NO ENTRY FOR " + gte);
        } else {
          tcg.addExpressionForSim(skey, ee);
        }
      }
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Map tags to types
  */
  
  private static ModelNode.Type mapToType(int val) {
    switch (val) {
      case Node.BARE:
        return (ModelNode.Type.BARE);
      case Node.BOX:
        return (ModelNode.Type.BOX);
      case Node.BUBBLE:
        return (ModelNode.Type.BUBBLE);
      case Node.INTERCELL:
        return (ModelNode.Type.INTERCELL);
      case Node.SLASH:
        return (ModelNode.Type.SLASH);
      case Node.DIAMOND:
        return (ModelNode.Type.DIAMOND);      
      case Node.GENE:
        return (ModelNode.Type.GENE);              
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Map link signs
  */

  private static ModelLink.Sign mapToSign(int val) {
    switch (val) {
      case Linkage.NEGATIVE:
        return (ModelLink.Sign.NEGATIVE);
      case Linkage.NONE:
        return (ModelLink.Sign.NONE);
      case Linkage.POSITIVE:
        return (ModelLink.Sign.POSITIVE);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map the expression value
  */

  private static ModelExpressionEntry.Level mapExpression(int value) {
    switch (value) {
      case ExpressionEntry.NO_REGION:
        return (ModelExpressionEntry.Level.NO_REGION);
      case ExpressionEntry.NO_DATA:
        return (ModelExpressionEntry.Level.NO_DATA);
      case ExpressionEntry.NOT_EXPRESSED:
        return (ModelExpressionEntry.Level.NOT_EXPRESSED);
      case ExpressionEntry.WEAK_EXPRESSION:
        return (ModelExpressionEntry.Level.WEAK_EXPRESSION);
      case ExpressionEntry.EXPRESSED:
        return (ModelExpressionEntry.Level.EXPRESSED);
      case ExpressionEntry.VARIABLE:
        return (ModelExpressionEntry.Level.VARIABLE);
      default:
        System.err.println("value was " + value);
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Map the expression value
  */

  private static int mapFromExpression(ModelExpressionEntry.Level lev) {
    switch (lev) {
      case NO_REGION:
        return (ExpressionEntry.NO_REGION);
      case NO_DATA:
        return (ExpressionEntry.NO_DATA);
      case NOT_EXPRESSED :
        return (ExpressionEntry.NOT_EXPRESSED);
      case WEAK_EXPRESSION:
        return (ExpressionEntry.WEAK_EXPRESSION);
      case EXPRESSED :
        return (ExpressionEntry.EXPRESSED);
      case VARIABLE:
        return (ExpressionEntry.VARIABLE);
      default:
        System.err.println("value was " + lev);
        throw new IllegalArgumentException();
    }    
  }
 
  /***************************************************************************
  **
  ** Map the source value
  */

  private static ModelExpressionEntry.Source mapSource(ExpressionEntry.Source value) {
    switch (value) {
      case NO_SOURCE_SPECIFIED:
        return (ModelExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      case MATERNAL_SOURCE:
        return (ModelExpressionEntry.Source.MATERNAL_SOURCE);
      case ZYGOTIC_SOURCE:
        return (ModelExpressionEntry.Source.ZYGOTIC_SOURCE);
      case MATERNAL_AND_ZYGOTIC:
        return (ModelExpressionEntry.Source.MATERNAL_AND_ZYGOTIC);
      default:
        System.err.println("value was " + value);
        throw new IllegalArgumentException();
    }    
  }
  
   /***************************************************************************
  **
  ** Go and select the links and nodes
  */ 
  
  public boolean goToModelAndSelect(String modelID, Set<String> nodeIDs, Set<String> linkIDs) {
    synchronized (this) {
      HarnessBuilder.PreHarness pH = hBld_.buildHarness(FlowMeister.OtherFlowKey.MODEL_AND_NODE_LINK_SELECTION);
      SetCurrentModel.StepState agis = (SetCurrentModel.StepState)pH.getCmdState();
      agis.setPreload(modelID, nodeIDs, linkIDs);
      return (hBld_.runHarnessForOOB(pH));
    }
  }

  /***************************************************************************
   **
   ** Finds feedback links
   */

  public Set<ModelLink> findFeedbackEdges() {
    Set<ModelLink> result = new HashSet<ModelLink>();

    Set<String> nodes = new HashSet<String>();
    for (Iterator<ModelNode> nodeIter = getRootModelNodes(); nodeIter.hasNext();) {
      ModelNode node = nodeIter.next();
      String id = node.getUniqueInternalID();
      nodes.add(id);
    }

    Set<Link> links = new HashSet<Link>();
    Map<Link, ModelLink> linkMap = new HashMap<Link, ModelLink>();

    for (Iterator<ModelLink> linkIter = getRootModelLinks(); linkIter.hasNext();) {
      ModelLink modelLink = linkIter.next();

      String src = modelLink.getSrc();
      String trg = modelLink.getTrg();
      Link link = new Link(src, trg);
      links.add(link);

      linkMap.put(link, modelLink);
    }

    Set<String> rootNodeIDs = findRootModelRootNodeIDs();

    GraphFeedbackFinder feedbackFinder = new GraphFeedbackFinder(nodes, links);
    Set<Link> possibleFeedback = feedbackFinder.run(new ArrayList<String>(rootNodeIDs));

    for (Link link : possibleFeedback) {
      result.add(linkMap.get(link));
    }

    return result;
  }

  public Set<String> findRootModelRootNodeIDs() {
    HashSet<String> retval = new HashSet<String>();

    for (Iterator<ModelNode> nodeIter = getRootModelNodes(); nodeIter.hasNext();) {
      ModelNode node = nodeIter.next();
      String id = node.getUniqueInternalID();
      retval.add(id);
    }

    for (Iterator<ModelLink> linkIter = getRootModelLinks(); linkIter.hasNext();) {
      ModelLink modelLink = linkIter.next();
      retval.remove(modelLink.getTrg());
    }

    return retval;
  }
}
