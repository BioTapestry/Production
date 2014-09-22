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

package org.systemsbiology.biotapestry.qpcr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.perturb.LegacyPert;
import org.systemsbiology.biotapestry.perturb.PertAnnotations;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This handles building up the QPCR display from the new format perturbation data
*/

class QpcrDisplayGenerator {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashMap tempFoots_;
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

  QpcrDisplayGenerator(BTState appState) {
    appState_ = appState;
    tempFoots_ = new HashMap();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Build it
  */

  QPCRData createQPCRFromPerts(PerturbationData pd) {
 
    QPCRData qpcr = new QPCRData(appState_);
    PertDictionary pDict = pd.getPertDictionary();
    ResourceManager rMan = appState_.getRMan();
    String etAl = rMan.getString("qpcrData.andOthers");
      
    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions();
    if (dOpt.hasColumns()) {
      Iterator<MinMax> cit = dOpt.getColumnIterator();
      while (cit.hasNext()) {
        MinMax nextCol = cit.next();
        qpcr.addColumn(nextCol.clone());
      }
    } else {
      qpcr.addColumn(new MinMax(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
    String scaleKey = dOpt.getPerturbDataDisplayScaleKey();
    Map techColors = dOpt.getMeasurementDisplayColors();
 
    PertFilterExpression pfe = new PertFilterExpression(PertFilterExpression.ALWAYS_OP);
    List pertData = pd.getPerturbations(pfe); 
    
    HashMap results = new HashMap();
    HashMap timeBounds = new HashMap();  
    HashMap regRes = new HashMap();
    HashMap footHoistForNull = new HashMap();
    HashMap investForNull = new HashMap();
    HashMap retainFootsForNull = new HashMap();
    
    Iterator pdpit = pertData.iterator();
    while (pdpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)pdpit.next();
      PertSources sources = pdp.getSources(pd);
      String targKey = pdp.getTargetKey();
      PertSourcesAndTargKey sst = new PertSourcesAndTargKey(sources, targKey);
      Experiment psi = pdp.getExperiment(pd);
      int pertTime = psi.getTime();
      int legacyMax = psi.getLegacyMaxTime();
 
      LegacyPert lp = pdp.getLegacyPert();
      Boolean isSig = pdp.getForcedSignificance();
      
      //
      // Gather up region restrictions.
      //
      
      PerturbationData.RegionRestrict rr = pdp.getRegionRestriction(pd);
      RegRestrictAggregation rra = (RegRestrictAggregation)regRes.get(sst);
      if (rra == null) {
        rra = new RegRestrictAggregation();
        regRes.put(sst, rra);
      }
      if (rr == null) {
        rra.noRestrict++;
      } else {
        if (rr.isLegacyNullStyle()) {
          String lVal = rr.getLegacyValue();
          rra.legacyVals.add(lVal);
        } else {
          String key = buildRegResRegions(rr);
          rra.actualFoots.add(key);
        }
      }
    
      //
      // This technique joins together different time points into a
      // single range.  This gives slightly different output from the
      // V4 presentation, but is the most general way to present the
      // info:
      
      
      TreeSet bounds = (TreeSet)timeBounds.get(sst);
      if (bounds == null) {
        bounds = new TreeSet();
        timeBounds.put(sst, bounds);
      }
      bounds.add(new Integer(pertTime));
      if ((legacyMax != Experiment.NO_TIME) && (pertTime != legacyMax)) {
        bounds.add(new Integer(legacyMax));
      }
      
      //
      // Is the measure significant?
      //
      
      boolean goForNull = false;
      if (lp != null) {
        goForNull = true; 
      } else if (isSig != null) {
        goForNull = !isSig.booleanValue();
      } else {
        goForNull = !pdp.aboveThresholds(pd);
      }
      
      Boolean allBelow = (Boolean)results.get(sst);
      if (allBelow == null) {  
        allBelow = new Boolean(goForNull);
        results.put(sst, allBelow);
      } else {
        boolean newVal = allBelow.booleanValue() && goForNull;
        allBelow = new Boolean(newVal);
      }
      results.put(sst, allBelow);
      
      //
      // Build up investigator lists for Null targets:
      //
      
      HashSet i4n = (HashSet)investForNull.get(sst);
      if (i4n == null) {
        i4n = new HashSet();
        investForNull.put(sst, i4n);
      }
      List invests = psi.getInvestigators();
      if ((invests != null) && !invests.isEmpty()) {
        int numI = invests.size();
        for (int i = 0; i < numI; i++) {
          String investKey = (String)invests.get(i);
          String invest = pd.getInvestigator(investKey);
          i4n.add(invest);
        }
      } else {
       i4n.add(etAl); // Add the et al when we have blank investigators! 
      }
    }
    
    resolveFootnotesForNulls(pd, pertData, results, footHoistForNull, retainFootsForNull);
    
    HashMap allTargsForSources = new HashMap();
    HashMap hoistedFootsPerSource = new HashMap();
    Iterator rit = results.keySet().iterator();
    while (rit.hasNext()) {
      PertSourcesAndTargKey sst = (PertSourcesAndTargKey)rit.next();      
      Boolean currentlyAllBelow = (Boolean)results.get(sst);
      // results go into a null perturbation    
      if (currentlyAllBelow.booleanValue()) { 
        List sources = getQPCRSourceList(sst.srcs, pd, pDict);
        NullPerturb npert = nullPerturbationMatchesSource(qpcr, sources);
        HashSet myHoistedFoots = (HashSet)hoistedFootsPerSource.get(sst.srcs);
        if (myHoistedFoots == null) {
          myHoistedFoots = new HashSet();
          hoistedFootsPerSource.put(sst.srcs, myHoistedFoots);
        }
        if (npert == null) {
          npert = new NullPerturb(appState_); 
          int numSrc = sources.size();
          for (int i = 0; i < numSrc; i++) {
            Source src = (Source)sources.get(i);
            ArrayList flist = new ArrayList();
            // last time thru only:
            if (i == (numSrc - 1)) {
              HashSet footsPerSource = (HashSet)footHoistForNull.get(sst.srcs);
              if ((footsPerSource != null) && !footsPerSource.isEmpty()) {                
                flist.addAll(pd.getFootnoteList(new ArrayList(footsPerSource)));
                myHoistedFoots.addAll(footsPerSource);
              }
            }        
            flist.addAll(src.getFootnoteNumbers());
            TreeSet sortedFoots = new TreeSet(new ReadOnlyTable.NumStrComparator());
            sortedFoots.addAll(flist);
            String newNotes = PertAnnotations.convertFootnoteListToString(new ArrayList(sortedFoots));
            src = (Source)src.clone();
            src.setNotes(newNotes);
            npert.addSource(src);
          }
          qpcr.addNullPerturbation(npert);
        }
        //
        // Build and add null target:
        //
        NullTarget targ = buildNullTarget(pd, sst, retainFootsForNull, myHoistedFoots, regRes, timeBounds);
        npert.addTarget(targ);
        //
        // Investigators:
        //
        HashSet i4n = (HashSet)investForNull.get(sst);
        npert.addInvestigators(i4n);
        
      } else {
        HashSet atfs = (HashSet)allTargsForSources.get(sst.srcs);
        if (atfs == null) {
          atfs = new HashSet();
          allTargsForSources.put(sst.srcs, atfs);
        }
        atfs.add(sst.targKey);
      }  
    }
    
    fillInPerturbations(qpcr, allTargsForSources, pertData, pd, pDict, scaleKey, techColors);
    
    
    //
    // Get the maps filled in:
    //
    
    Genome genome = appState_.getDB().getGenome();
    Iterator anit = genome.getAllNodeIterator();
    while (anit.hasNext()) {
      Node node = (Node)anit.next();
      String nodeID = node.getID();
      List entries = pd.getCustomDataEntryKeys(nodeID);
      ArrayList mapped = null;
      if ((entries != null) && !entries.isEmpty()) {
        mapped = new ArrayList();
        int numE = entries.size();
        for (int i = 0; i < numE; i++) {
          String targKey = (String)entries.get(i);
          String targName = pd.getTarget(targKey);
          mapped.add(targName);
        }
      }
          
      List sources = pd.getCustomDataSourceKeys(nodeID);
      ArrayList mappedS = null;
      if ((sources != null) && !sources.isEmpty()) {
        mappedS = new ArrayList();
        int numE = sources.size();
        for (int i = 0; i < numE; i++) {
          String srcKey = (String)sources.get(i);
          String srcName = pd.getSourceName(srcKey);
          mappedS.add(srcName);
        }
      }     
      
      qpcr.addDataMaps(nodeID, mapped, mappedS);    
    }
    
    //
    // Fill out the footnotes:
    //
    
    SortedMap annots = pd.getPertAnnotationsMap();
    Iterator akit = annots.keySet().iterator();
    while (akit.hasNext()) {
      String tag = (String)akit.next();
      String message = (String)annots.get(tag);
      Footnote nextNote = new Footnote(tag);
      nextNote.setNote(message);
      qpcr.addFootnote(nextNote);
    }
    Iterator lit = tempFoots_.keySet().iterator();
    TreeMap sorted = new TreeMap(new ReadOnlyTable.NumStrComparator());
    while (lit.hasNext()) {
      String message = (String)lit.next();
      String tag = (String)tempFoots_.get(message);
      sorted.put(tag, message);
    }
    Iterator sit = sorted.keySet().iterator();
    while (sit.hasNext()) {
      String tag = (String)sit.next();
      String message = (String)sorted.get(tag);
      Footnote nextNote = new Footnote(tag);
      nextNote.setNote(message);
      qpcr.addFootnote(nextNote);
    }     
    return (qpcr);
  }
   
  /***************************************************************************
  ** 
  ** Build a null target, with correct footnotes:
  */
  
  
  private NullTarget buildNullTarget(PerturbationData pd, PertSourcesAndTargKey sst, 
                                     HashMap retainFootsForNull, HashSet myHoistedFoots,
                                     HashMap regRes, HashMap timeBounds) {
  
  
    HashMap retainFootsPerSource = (HashMap)retainFootsForNull.get(sst.srcs);
    List retainFootsPerTarget = (List)retainFootsPerSource.get(sst.targKey);
    NullTarget targ = new NullTarget(pd.getTarget(sst.targKey));  
    List foots = new ArrayList();
    // Get per-target footnotes added:
    HashSet retain = new HashSet(retainFootsPerTarget);
    // Drop hoisted footnotes:
    retain.removeAll(pd.getFootnoteList(new ArrayList(myHoistedFoots)));
    foots.addAll(retain);

    // Add region-restricted based notes:
    RegRestrictAggregation rra = (RegRestrictAggregation)regRes.get(sst);
    if (!rra.legacyVals.isEmpty() || !rra.actualFoots.isEmpty()) {
      foots.addAll(buildRegionFootsForNullTarg(pd, rra));
    }
    String noteStr = PertAnnotations.convertFootnoteListToString(foots);
    targ.setFootsForHTML(noteStr);   
    //
    // Time span info
    //
    TreeSet mm = (TreeSet)timeBounds.get(sst);
    if (mm.size() == 1) {
      NullTimeSpan nts = new NullTimeSpan(appState_, ((Integer)mm.first()).intValue());
      targ.addTimeSpan(nts); 
    } else {
      NullTimeSpan nts = new NullTimeSpan(appState_, ((Integer)mm.first()).intValue(), ((Integer)mm.last()).intValue());
      targ.addTimeSpan(nts);
    }
    return (targ);
  }
  
 
  /***************************************************************************
  ** 
  ** Resolve footnotes for nulls, including hoists:
  */

  private void resolveFootnotesForNulls(PerturbationData pd, List pertList, Map results, 
                                        HashMap hoistFoots, HashMap retainFoots) {
    
    HashMap pointCountsPerTargPerSource = new HashMap();
    
    Iterator plit = pertList.iterator();
    while (plit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)plit.next(); 
      PertSources sources = pdp.getSources(pd);
      String targKey = pdp.getTargetKey();
      PertSourcesAndTargKey sst = new PertSourcesAndTargKey(sources, targKey);
      Boolean currentlyAllBelow = (Boolean)results.get(sst);
      // if not bound for null, we skip:    
      if (!currentlyAllBelow.booleanValue()) { 
        continue;
      }
      List notes = pd.getDataPointNotes(pdp.getID());
      
      //
      // Keep track of the number of data points per target of a source.  We use this
      // to figure out if we have complete, or only partial, footnote coverage for the
      // source:
      //
      
      HashMap pointCountsPerTarg = (HashMap)pointCountsPerTargPerSource.get(sources);
      if (pointCountsPerTarg == null) {
        pointCountsPerTarg = new HashMap();
        pointCountsPerTargPerSource.put(sources, pointCountsPerTarg);
      }
      Integer pointCount = (Integer)pointCountsPerTarg.get(targKey);
      if (pointCount == null) {
        pointCount = new Integer(1);
      } else {
        pointCount = new Integer(pointCount.intValue() + 1);
      }
      pointCountsPerTarg.put(targKey, pointCount);        
  
      //
      // Collect up notes per target of each source.  We count the notes, to
      // see if every data point for the target shares the note!
      //
     
      HashMap retainFootsPerSource = (HashMap)retainFoots.get(sources);
      if (retainFootsPerSource == null) {
        retainFootsPerSource = new HashMap();
        retainFoots.put(sources, retainFootsPerSource);
      }
      HashMap retainFootsPerTarget = (HashMap)retainFootsPerSource.get(targKey);
      if (retainFootsPerTarget == null) {
        retainFootsPerTarget = new HashMap();
        retainFootsPerSource.put(targKey, retainFootsPerTarget);
      }
      if (notes != null) {       
        int numNotes = notes.size();
        for (int i = 0; i < numNotes; i++) {
          String noteKey = (String)notes.get(i);
          Integer count = (Integer)retainFootsPerTarget.get(noteKey);
          if (count == null) {
            count = new Integer(1);
          } else {
            count = new Integer(count.intValue() + 1);
          }
          retainFootsPerTarget.put(noteKey, count);          
        }
      }
      
      //
      // This gathers up the footnotes that can be hoisted up to the
      // source, since everybody shares them:
      //
 
      HashSet footsPerSource = (HashSet)hoistFoots.get(sources);
      if (footsPerSource == null) {
        footsPerSource = new HashSet();
        if (notes != null) {
          footsPerSource.addAll(notes);
        }
        hoistFoots.put(sources, footsPerSource);
      } else {
        // Gotta keep what is common and toss what is missing!
        HashSet intersect = (HashSet)footsPerSource.clone();
        if (notes == null) {
          intersect.clear();
        } else {
          intersect.retainAll(notes);
        }
        hoistFoots.put(sources, intersect);
      }
    }
    
    //
    // Tag targets if they have incomplete footnote coverage.  This step converts the
    // map of keys to counts for a target into just a list of footnote TAGS
    //
   
    Iterator rfit = retainFoots.keySet().iterator();
    while (rfit.hasNext()) {
      PertSources sources = (PertSources)rfit.next();
      HashMap retainFootsPerSource = (HashMap)retainFoots.get(sources);
      HashMap pointCountsPerTarg = (HashMap)pointCountsPerTargPerSource.get(sources);
      // Revising the map as we go:
      Iterator rffsit = new HashSet(retainFootsPerSource.keySet()).iterator(); 
      while (rffsit.hasNext()) {
        String trgKey = (String)rffsit.next();
        HashMap retainFootsPerTarget = (HashMap)retainFootsPerSource.get(trgKey);
        Integer pointCount = (Integer)pointCountsPerTarg.get(trgKey);
        Iterator rfftit = retainFootsPerTarget.keySet().iterator();
        HashSet revised = new HashSet();
        String pnt = null;
        while (rfftit.hasNext()) {
          String footKey = (String)rfftit.next();
          Integer perFootCount = (Integer)retainFootsPerTarget.get(footKey);
          revised.add(footKey);
          if (!perFootCount.equals(pointCount)) {
            pnt = getPartialNullTag(pd);
          }
        }
        List footsPerSource = pd.getFootnoteList(new ArrayList(revised));
        if (pnt != null) {
          footsPerSource.add(pnt);
        }
        retainFootsPerSource.put(trgKey, footsPerSource);      
      }      
    }
    return;
  }
  
  
  /***************************************************************************
  ** 
  ** Build the partial null tag
  */
  
  private String getPartialNullTag(PerturbationData pd) {
    ResourceManager rMan = appState_.getRMan();
    String foot = rMan.getString("qpcrDisplay.incompleteNullFootnoteCoverage");    
    String tag = (String)tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
      tempFoots_.put(foot, tag);
    }
    return (tag);
  }
  
  /***************************************************************************
  ** 
  ** Get sources as list
  */
   
  private List getQPCRSourceList(PertSources pss, PerturbationData pd, PertDictionary pDict) {
    ArrayList retval = new ArrayList();
    Iterator sit = pss.getSources();
    while (sit.hasNext()) {
      String srcID = (String)sit.next();    
      PertSource ps = pd.getSourceDef(srcID);
      PertProperties pProps = ps.getExpType(pDict);
      Source newSrc = new Source(ps.getSourceName(pd), pProps.getLegacyType());
      List srcAnnot = ps.getAnnotationIDs();
      String noteStr = (srcAnnot.isEmpty()) ? null : pd.getFootnoteListAsString(srcAnnot);
      newSrc.setNotes(noteStr); 
      retval.add(newSrc);
    }
    return (retval);
  }
  
 
  /***************************************************************************
  ** 
  ** Answers if we match null perturbations
  */

  private NullPerturb nullPerturbationMatchesSource(QPCRData qpcr, List sources) {  
    Iterator npit = qpcr.getNullPerturbations();
    while (npit.hasNext()) {
      NullPerturb chknp = (NullPerturb)npit.next();
      if (chknp.sourcesMatch(sources)) {
        return (chknp);
      }
    }
    return (null);
  } 
  
  /***************************************************************************
  ** 
  ** Fill measurements into the perturbation
  */

  private boolean fillInPerturbations(QPCRData qpcr, HashMap atfs, List pertList, 
                                      PerturbationData pd, PertDictionary pDict, 
                                      String scaleKey, Map techColors) {
 
    HashMap droppedMultiNull = new HashMap();
    HashMap hoistFoots = new HashMap();
    Iterator plit = pertList.iterator();
    while (plit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)plit.next();
      Experiment psi = pdp.getExperiment(pd);
      PertSources ps = psi.getSources();
      HashSet targs = (HashSet)atfs.get(ps);
      String targKey = pdp.getTargetKey();
      if ((targs == null) || !targs.contains(targKey)) {
        continue;
      }
      //
      // Legacy null perturb entries with an unknown number of NS data points
      // cannot be directly displayed in the table.  They need to be noted with
      // a footnote
      //
      
      if (pdp.notForTableDisplay()) {
        int timeVal = psi.getTime();
        int timeMax = psi.getLegacyMaxTime();
        if ((timeMax != Experiment.NO_TIME) && (timeMax != timeVal)) {
          if (qpcr.getEnclosingColumn(timeVal, timeMax) == null) {
            String regList = null;
            PerturbationData.RegionRestrict rr = pdp.getRegionRestriction(pd);
            if (rr != null) {              
              if (rr.isLegacyNullStyle()) {
                regList = rr.getLegacyValue();
              } else {
                regList = buildRegResRegions(rr);
              }
            }
            HashMap droppedPerSource = (HashMap)droppedMultiNull.get(ps);
            if (droppedPerSource == null) {
              droppedPerSource = new HashMap();
              droppedMultiNull.put(ps, droppedPerSource);
            }
            FormerNullFootData timesPerDrop = (FormerNullFootData)droppedPerSource.get(targKey);
            if (timesPerDrop == null) {
              timesPerDrop = new FormerNullFootData();
              droppedPerSource.put(targKey, timesPerDrop);
            }
            timesPerDrop.times.add(TimeSpan.spanToString(appState_, new MinMax(timeVal, timeMax)));
            if (regList != null) {
              timesPerDrop.regions.add(regList);
            }
            continue;
          }
        }
      }
      
      //
      // We collect up the per-data-point footnotes to see what can be hoisted up to the 
      // perturbation level:
      //
      
      resolveFootHoists(pd, ps, pdp, hoistFoots);
    }
    plit = pertList.iterator();
    while (plit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)plit.next();
      Experiment psi = pdp.getExperiment(pd);
      PertSources ps = psi.getSources();
      HashSet targs = (HashSet)atfs.get(ps);
      String targKey = pdp.getTargetKey();
      if ((targs == null) || !targs.contains(targKey)) {
        continue;
      }
      HashMap footsPerSource = (HashMap)hoistFoots.get(ps);
      HashSet footsPerTarg = (HashSet)footsPerSource.get(targKey);
      HashMap droppedPerSource = (HashMap)droppedMultiNull.get(ps);
      fillForTarg(qpcr, ps, targKey, pdp, pd, pDict, footsPerTarg, droppedPerSource, scaleKey, techColors);
    }
    return (true);
  } 
 
  /***************************************************************************
  ** 
  ** Resolve footnote hoisting
  */

  private void resolveFootHoists(PerturbationData pd, PertSources ps, PertDataPoint pdp, HashMap hoistFoots) {
    String targKey = pdp.getTargetKey();
    List notes = pd.getDataPointNotes(pdp.getID());
    HashMap footsPerSource = (HashMap)hoistFoots.get(ps);
    if (footsPerSource == null) {
      footsPerSource = new HashMap();
      hoistFoots.put(ps, footsPerSource);
    }
    HashSet footsPerTarg = (HashSet)footsPerSource.get(targKey);
    if (footsPerTarg == null) {
      footsPerTarg = new HashSet();
      if (notes != null) {
        footsPerTarg.addAll(notes);
      }
      footsPerSource.put(targKey, footsPerTarg);
    } else {
      // Gotta keep what is common and toss what is missing!
      HashSet intersect = (HashSet)footsPerTarg.clone();
      if (notes == null) {
        intersect.clear();
      } else {
        intersect.retainAll(notes);
      }
      footsPerSource.put(targKey, intersect);
    }
    return;
  }
   
  /***************************************************************************
  ** 
  ** Fill measurements into the perturbation
  */

  private void fillForTarg(QPCRData qpcr, PertSources ps, String targKey, 
                           PertDataPoint pdp, PerturbationData pd, 
                           PertDictionary pDict,
                           HashSet hoistFoots, Map droppedPerSource, 
                           String scaleKey, Map techColors) {
    
   
    List sources = getQPCRSourceList(ps, pd, pDict);

    TargetGene gene = qpcr.getQPCRDataRelaxedMatch(pd.getTarget(targKey));
    if (gene == null) {
      List notes = pd.getFootnotesForTarget(targKey);
      String noteStr = (notes == null) ? null : pd.getFootnoteListAsString(notes);
      gene = new TargetGene(pd.getTarget(targKey), noteStr);
      qpcr.addGene(gene, false);
    }
    
    Perturbation pert = null;
    Iterator pit = gene.getPerturbations();
    while (pit.hasNext()) {
      Perturbation chkPert = (Perturbation)pit.next();
      if (chkPert.sourcesMatch(sources)) {
        pert = chkPert;
        break;
      }
    }
    
    if (pert == null) {
      pert = new Perturbation();
      int numSrc = sources.size();
      for (int i = 0; i < numSrc; i++) {
        Source src = (Source)sources.get(i);
        ArrayList flist = new ArrayList();
        // last time thru only:
        if (i == (numSrc - 1)) {
          if (!hoistFoots.isEmpty()) {
            flist.addAll(pd.getFootnoteList(new ArrayList(hoistFoots)));
          }
          if (droppedPerSource != null) { 
            FormerNullFootData timesPerDrop = (FormerNullFootData)droppedPerSource.get(targKey);
            if (timesPerDrop != null) {      
              ResourceManager rMan = appState_.getRMan();
              String fmt;
              Object[] list;
              if (timesPerDrop.regions.isEmpty()) {
                fmt = rMan.getString("qpcrDisplay.inexactMultiDrop");
                list = new Object[1];
                list[0] = timesPerDrop.getTimesList();
              } else {
                fmt = rMan.getString("qpcrDisplay.inexactMultiDropWithRegion");
                list = new Object[2];
                list[0] = timesPerDrop.getTimesList();
                list[1] = timesPerDrop.getRegionsList();
              }
              String msg = MessageFormat.format(fmt, list);                    
              String tag = (String)tempFoots_.get(msg);
              if (tag == null) {
                tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
                tempFoots_.put(msg, tag);
              }
              flist.add(tag);
            }
          }
        }
        
        flist.addAll(src.getFootnoteNumbers());
        TreeSet sortedFoots = new TreeSet(new ReadOnlyTable.NumStrComparator());
        sortedFoots.addAll(flist);
        String newNotes = PertAnnotations.convertFootnoteListToString(new ArrayList(sortedFoots));
        src = (Source)src.clone();
        src.setNotes(newNotes);
        pert.addSource(src);
      }
      gene.addPerturbation(pert);
    }
    
    //
    // Figure out the time span we are attaching to:
    //
     
    Experiment psi = pdp.getExperiment(pd);
    int timeVal = psi.getTime();
    int timeMax = psi.getLegacyMaxTime();
    
    //
    // Legacy multi-drop cases handled separately:
    //
    
    String exactMultiDropTag = null;
    String nonStdMaxTime = null;
    String nonStdTime = null;
    MinMax column;
    
    if (pdp.notForTableDisplay()) {
      if ((timeMax != Experiment.NO_TIME) && (timeMax != timeVal)) {
        column = qpcr.getEnclosingColumn(timeVal, timeMax);
        if (column == null) {
          return;
        }
      } else {
        column = qpcr.getEnclosingOrClosestColumn(timeVal, timeMax);
        if (column == null) {
          throw new IllegalStateException();
        }   
      }
      ResourceManager rMan = appState_.getRMan();
      String msg = rMan.getString("qpcrDisplay.exactMultiDrop");
      String tag = (String)tempFoots_.get(msg);
      if (tag == null) {
        tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
        tempFoots_.put(msg, tag);
      }
      exactMultiDropTag = tag;
    } else {   
      column = qpcr.getEnclosingOrClosestColumn(timeVal, timeMax);
      if (column == null) {
        throw new IllegalStateException();
      }
    }
    
    if (timeMax == Experiment.NO_TIME) {
      if ((timeVal < column.min) || (timeVal > column.max)) {
        nonStdTime = Integer.toString(timeVal);
      }
    } else {
      if ((timeVal != column.min) || (timeMax != column.max)) {
        nonStdTime = Integer.toString(timeVal);
        nonStdMaxTime = Integer.toString(timeMax);
      }     
    }
      
    TimeSpan span = pert.getTimeSpan(column);
    if (span == null) {
      span = new TimeSpan(column);
      pert.addTime(span);
    }
   
    //
    // If the psi is for a legacy span case, we do NOT want a single time attached!
    //
    
    if (timeMax != Experiment.NO_TIME) {
      if ((timeVal == column.min) && (timeMax == column.max)) {
        timeVal = Batch.NO_TIME;
      }  
    }
    
    //
    // Get data point notes:
    //
    
    List notes = pd.getDataPointNotes(pdp.getID());
    List localTags = new ArrayList();
  
    if (exactMultiDropTag != null) {
      localTags.add(exactMultiDropTag);
    }
    
    //
    // Bring in region restrictions.  While legacy QPCR data stored this with 
    // time span, it was not output.  So skip TimeSpan stocking and add a footnote
    // to the measurement!
    //
      
    PerturbationData.RegionRestrict rr = pdp.getRegionRestriction(pd);
    if (rr != null) {
      String tag = buildRegResFootnote(pd, rr);
      localTags.add(tag);
    }
    
    //
    // remove those that can be hoisted to the pert source:
    //
    
    if (notes != null) {
      notes = new ArrayList(notes);
      notes.removeAll(hoistFoots);
    }
   
    String noteStr = null;     
    if ((notes != null) || !localTags.isEmpty()) {
      PertAnnotations pa = pd.getPertAnnotations();
      List flist = (notes == null) ? new ArrayList() : pa.getFootnoteList(notes);
      TreeSet sorted = new TreeSet(flist);
      sorted.addAll(localTags);
      noteStr = PertAnnotations.convertFootnoteListToString(new ArrayList(sorted)); 
    }
      
    String csvBatch = pdp.getBatchKey();
    String invest = psi.getInvestigatorDisplayString(pd);
    Batch batch = span.getBatchWithIDAndInvest(csvBatch, invest);
    if (batch == null) {
      batch = new Batch(appState_, timeVal, pdp.getDate(), invest, csvBatch);
      span.addBatch(batch);
    }
    Measurement meas = new Measurement(appState_, pdp.getScaledDisplayValueOldStyle(scaleKey, pd, false), nonStdTime, noteStr, 
                                       pdp.getControl(), pdp.getForcedSignificance(), pdp.getComment());
    if (nonStdMaxTime != null) {
      meas.setNonStdMaxTime(nonStdMaxTime);
    }
    
    String colStr = (String)techColors.get(pdp.getMeasurementTypeKey());
    meas.setColorDisplay(colStr);
    batch.addMeasurement(meas);

    List investigators = psi.getInvestigators();
    for (int i = 0; i < investigators.size(); i++) {
      String investKey = (String)investigators.get(i);
      pert.addInvestigatorIfNew(pd.getInvestigator(investKey));
    }
    return;
  }  

 
  /***************************************************************************
  ** 
  ** Build footnotes for null targets:
  */
  
  private List buildRegionFootsForNullTarg(PerturbationData pd, RegRestrictAggregation rra) {
    ArrayList retval = new ArrayList();
    ResourceManager rMan = appState_.getRMan();
    if (rra.noRestrict == 0) {
      if (rra.actualFoots.isEmpty()) {
        Iterator lvit = rra.legacyVals.iterator();
        int size = rra.legacyVals.size(); 
        Object[] lvobj = new Object[1];
        if (size == 1) {      
          lvobj[0] = lvit.next();
          String format = rMan.getString("qpcrDisplay.simpleLegacyRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = (String)tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        } else {
          StringBuffer buf = new StringBuffer();
          while (lvit.hasNext()) {
            buf.append((String)lvit.next());
            if (lvit.hasNext()) {
              buf.append(", ");
            }
          }
          lvobj[0] = buf.toString();
          String format = rMan.getString("qpcrDisplay.mixedLegacyRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = (String)tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        }
      } else if (rra.legacyVals.isEmpty()) {
        Iterator afit = rra.actualFoots.iterator();
        int size = rra.actualFoots.size(); 
        Object[] lvobj = new Object[1];
        if (size == 1) {      
          lvobj[0] = afit.next();
          String format = rMan.getString("qpcrDisplay.simpleRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = (String)tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        } else {
          StringBuffer buf = new StringBuffer();
          while (afit.hasNext()) {
            buf.append((String)afit.next());
            if (afit.hasNext()) {
              buf.append(", ");
            }
          }
          lvobj[0] = buf.toString();
          String format = rMan.getString("qpcrDisplay.mixedRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = (String)tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        }
      }
    }
    //
    // Huge mess!!
    //
    StringBuffer buf = new StringBuffer();
    Iterator lvit = rra.legacyVals.iterator();
    Iterator afit = rra.actualFoots.iterator();
    while (lvit.hasNext()) {
      buf.append("\"");
      buf.append((String)lvit.next());
      buf.append("\"");
      if (lvit.hasNext() || afit.hasNext()) {
        buf.append(", ");
      }
    }
    while (afit.hasNext()) {
      buf.append("\"");
      buf.append((String)afit.next());
      buf.append("\"");
      if (afit.hasNext()) {
        buf.append(", ");
      }
    }
    Object[] lvobj = new Object[1];
    lvobj[0] = buf.toString();
    
    String foot;
    if (rra.noRestrict == 0) {
      String format = rMan.getString("qpcrDisplay.inconsistentRegionRestrictionFormat");
      foot = MessageFormat.format(format, lvobj);
    } else {
      String format = rMan.getString("qpcrDisplay.inconsistentRegionRestrictionFormatWithCount");
      foot = MessageFormat.format(format, lvobj);     
    }
   
    String tag = (String)tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
      tempFoots_.put(foot, tag);
    }
    retval.add(tag);
    return (retval);  
  }
  
  /***************************************************************************
  ** 
  ** Build a region restriction region list
  */  
  
  private String buildRegResRegions(PerturbationData.RegionRestrict rr) {
    StringBuffer regBuf = new StringBuffer();
    if (rr.isLegacyNullStyle()) {
      throw new IllegalArgumentException();
    } else {
      TreeSet sorted = new TreeSet();
      Iterator nrrit = rr.getRegions();
      while (nrrit.hasNext()) {
        String region = (String)nrrit.next();
        sorted.add(region);
      }     
      nrrit = sorted.iterator();
      while (nrrit.hasNext()) {
        String region = (String)nrrit.next();
        regBuf.append(region);
        if (nrrit.hasNext()) {
          regBuf.append(", ");
        }
      } 
    }
    return (regBuf.toString());
   }  
  
  /***************************************************************************
  ** 
  ** Build a region restriction footnote
  */

  private String buildRegResFootnote(PerturbationData pd, PerturbationData.RegionRestrict rr) {
    ResourceManager rMan = appState_.getRMan();
    String regList;
    if (rr.isLegacyNullStyle()) {
      regList = rr.getLegacyValue();
    } else {
      regList = buildRegResRegions(rr);
    }
    
    String format = rMan.getString("qpcrDisplay.regionRestrictionFormat");
    String foot = MessageFormat.format(format, new Object[] {regList});             
    String tag = (String)tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet(tempFoots_.values()));
      tempFoots_.put(foot, tag);
    }
    return (tag);
  }
  
  /***************************************************************************
  ** 
  ** Simple experiment source/targ combos for hashing
  */  
  
  private static class PertSourcesAndTargKey {    
    PertSources srcs;
    String targKey;

    PertSourcesAndTargKey(PertSources srcs, String targ) {
      this.srcs = srcs;
      this.targKey = targ;
    }

     public int hashCode() {
      return (srcs.hashCode() + targKey.hashCode());
    }

     public String toString() {
      return ("PertSourcesAndTarg: " + srcs + " " + targKey);
    }

     public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof PertSourcesAndTargKey)) {
        return (false);
      }
      
      PertSourcesAndTargKey otherSST = (PertSourcesAndTargKey)other; 

      return (this.srcs.equals(otherSST.srcs) && this.targKey.equals(otherSST.targKey));    
    }
  }
  
  /***************************************************************************
  ** 
  ** For gathering up Null pert region restrictions
  */  
  
  private static class RegRestrictAggregation {    
    TreeSet legacyVals;
    TreeSet actualFoots;
    int noRestrict;

    RegRestrictAggregation() {
      legacyVals = new TreeSet();
      actualFoots = new TreeSet();
      noRestrict = 0;
    }
  }
  
  /***************************************************************************
  ** 
  ** For gathering up formerly null data for footnotes
  */  
  
  private static class FormerNullFootData {    
    TreeSet times;
    TreeSet regions;

    FormerNullFootData() {
      times = new TreeSet();
      regions = new TreeSet();
    }
    
    private String getStringList(TreeSet set) {
      StringBuffer buf = new StringBuffer();
      Iterator sit = set.iterator();
      while (sit.hasNext()) {
        String str = (String)sit.next();
        buf.append("(");
        buf.append(str);
        buf.append(")");
        if (sit.hasNext()) {
          buf.append(", ");
        }
      }
      return (buf.toString());
    }
      
    String getTimesList() {    
      return (getStringList(times));
    }  
    
    String getRegionsList() {   
      return (getStringList(regions));
    }
  }
}
