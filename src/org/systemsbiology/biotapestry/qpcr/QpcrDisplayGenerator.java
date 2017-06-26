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

package org.systemsbiology.biotapestry.qpcr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.perturb.LegacyPert;
import org.systemsbiology.biotapestry.perturb.PertAnnotations;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertDisplayOptions;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
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
  
  private HashMap<String, String> tempFoots_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  QpcrDisplayGenerator() {
    tempFoots_ = new HashMap<String, String>();
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

  QPCRData createQPCRFromPerts(PerturbationData pd, TimeAxisDefinition tad,
                               PerturbationDataMaps pdms, DBGenome genome, ResourceManager rMan) {

    QPCRData qpcr = new QPCRData();
    PertDictionary pDict = pd.getPertDictionary();
    String etAl = rMan.getString("qpcrData.andOthers");
      
    PertDisplayOptions dOpt = pd.getPertDisplayOptions();
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
    Map<String, String> techColors = dOpt.getMeasurementDisplayColors();
 
    PertFilterExpression pfe = new PertFilterExpression(PertFilterExpression.Op.ALWAYS_OP);
    List<PertDataPoint> pertData = pd.getPerturbations(pfe); 
    
    HashMap<PertSourcesAndTargKey, Boolean> results = new HashMap<PertSourcesAndTargKey, Boolean>();
    HashMap<PertSourcesAndTargKey, SortedSet<Integer>> timeBounds = new HashMap<PertSourcesAndTargKey, SortedSet<Integer>>();  
    HashMap<PertSourcesAndTargKey, RegRestrictAggregation> regRes = new HashMap<PertSourcesAndTargKey, RegRestrictAggregation>();
    HashMap<PertSourcesAndTargKey, Set<String>> investForNull = new HashMap<PertSourcesAndTargKey, Set<String>>();

    Iterator<PertDataPoint> pdpit = pertData.iterator();
    while (pdpit.hasNext()) {
      PertDataPoint pdp = pdpit.next();
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
      RegRestrictAggregation rra = regRes.get(sst);
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
      
      
      SortedSet<Integer> bounds = timeBounds.get(sst);
      if (bounds == null) {
        bounds = new TreeSet<Integer>();
        timeBounds.put(sst, bounds);
      }
      bounds.add(Integer.valueOf(pertTime));
      if ((legacyMax != Experiment.NO_TIME) && (pertTime != legacyMax)) {
        bounds.add(Integer.valueOf(legacyMax));
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
      
      Boolean allBelow = results.get(sst);
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
      
      Set<String> i4n = investForNull.get(sst);
      if (i4n == null) {
        i4n = new HashSet<String>();
        investForNull.put(sst, i4n);
      }
      List<String> invests = psi.getInvestigators();
      if ((invests != null) && !invests.isEmpty()) {
        int numI = invests.size();
        for (int i = 0; i < numI; i++) {
          String investKey = invests.get(i);
          String invest = pd.getInvestigator(investKey);
          i4n.add(invest);
        }
      } else {
       i4n.add(etAl); // Add the et al when we have blank investigators! 
      }
    }
    
    HashMap<PertSources, Set<String>> footHoistForNull = new HashMap<PertSources, Set<String>>();
    HashMap<PertSources, Map<String, List<String>>> retainFootsForNull = new HashMap<PertSources, Map<String, List<String>>>();
    resolveFootnotesForNulls(pd, pertData, results, footHoistForNull, retainFootsForNull, rMan);
    
    HashMap<PertSources, Set<String>> allTargsForSources = new HashMap<PertSources, Set<String>>();
    HashMap<PertSources, Set<String>> hoistedFootsPerSource = new HashMap<PertSources, Set<String>>();
    Iterator<PertSourcesAndTargKey> rit = results.keySet().iterator();
    while (rit.hasNext()) {
      PertSourcesAndTargKey sst = rit.next();      
      Boolean currentlyAllBelow = results.get(sst);
      // results go into a null perturbation    
      if (currentlyAllBelow.booleanValue()) { 
        List<Source> sources = getQPCRSourceList(sst.srcs, pd, pDict);
        NullPerturb npert = nullPerturbationMatchesSource(qpcr, sources);
        Set<String> myHoistedFoots = hoistedFootsPerSource.get(sst.srcs);
        if (myHoistedFoots == null) {
          myHoistedFoots = new HashSet<String>();
          hoistedFootsPerSource.put(sst.srcs, myHoistedFoots);
        }
        if (npert == null) {
          npert = new NullPerturb(); 
          int numSrc = sources.size();
          for (int i = 0; i < numSrc; i++) {
            Source src = sources.get(i);
            ArrayList<String> flist = new ArrayList<String>();
            // last time thru only:
            if (i == (numSrc - 1)) {
              Set<String> footsPerSource = footHoistForNull.get(sst.srcs);
              if ((footsPerSource != null) && !footsPerSource.isEmpty()) {                
                flist.addAll(pd.getFootnoteList(new ArrayList<String>(footsPerSource)));
                myHoistedFoots.addAll(footsPerSource);
              }
            }        
            flist.addAll(src.getFootnoteNumbers());
            TreeSet<String> sortedFoots = new TreeSet<String>(new ReadOnlyTable.NumStrComparator());
            sortedFoots.addAll(flist);
            String newNotes = PertAnnotations.convertFootnoteListToString(new ArrayList<String>(sortedFoots));
            src = src.clone();
            src.setNotes(newNotes);
            npert.addSource(src);
          }
          qpcr.addNullPerturbation(npert);
        }
        //
        // Build and add null target:
        //
        NullTarget targ = buildNullTarget(pd, sst, retainFootsForNull, myHoistedFoots, regRes, timeBounds, rMan);
        npert.addTarget(targ);
        //
        // Investigators:
        //
        Set<String> i4n = investForNull.get(sst);
        npert.addInvestigators(i4n);
        
      } else {
        Set<String> atfs = allTargsForSources.get(sst.srcs);
        if (atfs == null) {
          atfs = new HashSet<String>();
          allTargsForSources.put(sst.srcs, atfs);
        }
        atfs.add(sst.targKey);
      }  
    }
    
    fillInPerturbations(qpcr, allTargsForSources, pertData, pd, tad, pDict, scaleKey, techColors, rMan);
    
    
    //
    // Get the maps filled in:
    //
    
    
    Iterator<Node> anit = genome.getAllNodeIterator();
    while (anit.hasNext()) {
      Node node = anit.next();
      String nodeID = node.getID();
      List<String> entries = pdms.getCustomDataEntryKeys(nodeID);
      ArrayList<String> mapped = null;
      if ((entries != null) && !entries.isEmpty()) {
        mapped = new ArrayList<String>();
        int numE = entries.size();
        for (int i = 0; i < numE; i++) {
          String targKey = entries.get(i);
          String targName = pd.getTarget(targKey);
          mapped.add(targName);
        }
      }
          
      List<String> sources = pdms.getCustomDataSourceKeys(nodeID);
      ArrayList<String> mappedS = null;
      if ((sources != null) && !sources.isEmpty()) {
        mappedS = new ArrayList<String>();
        int numE = sources.size();
        for (int i = 0; i < numE; i++) {
          String srcKey = sources.get(i);
          String srcName = pd.getSourceName(srcKey);
          mappedS.add(srcName);
        }
      }     
      
      qpcr.addDataMaps(nodeID, mapped, mappedS);    
    }
    
    //
    // Fill out the footnotes:
    //
    
    SortedMap<String, String> annots = pd.getPertAnnotationsMap();
    Iterator<String> akit = annots.keySet().iterator();
    while (akit.hasNext()) {
      String tag = akit.next();
      String message = annots.get(tag);
      Footnote nextNote = new Footnote(tag);
      nextNote.setNote(message);
      qpcr.addFootnote(nextNote);
    }
    Iterator<String> lit = tempFoots_.keySet().iterator();
    TreeMap<String, String> sorted = new TreeMap<String, String>(new ReadOnlyTable.NumStrComparator());
    while (lit.hasNext()) {
      String message = lit.next();
      String tag = tempFoots_.get(message);
      sorted.put(tag, message);
    }
    Iterator<String> sit = sorted.keySet().iterator();
    while (sit.hasNext()) {
      String tag = sit.next();
      String message = sorted.get(tag);
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
                                     Map<PertSources, Map<String, List<String>>> retainFootsForNull, Set<String> myHoistedFoots,
                                     Map<PertSourcesAndTargKey, RegRestrictAggregation> regRes, 
                                     Map<PertSourcesAndTargKey, SortedSet<Integer>> timeBounds, ResourceManager rMan) {
  
  
    Map<String, List<String>> retainFootsPerSource = retainFootsForNull.get(sst.srcs);
    List<String> retainFootsPerTarget = retainFootsPerSource.get(sst.targKey);
    NullTarget targ = new NullTarget(pd.getTarget(sst.targKey));  
    List<String> foots = new ArrayList<String>();
    // Get per-target footnotes added:
    HashSet<String> retain = new HashSet<String>(retainFootsPerTarget);
    // Drop hoisted footnotes:
    retain.removeAll(pd.getFootnoteList(new ArrayList<String>(myHoistedFoots)));
    foots.addAll(retain);

    // Add region-restricted based notes:
    RegRestrictAggregation rra = regRes.get(sst);
    if (!rra.legacyVals.isEmpty() || !rra.actualFoots.isEmpty()) {
      foots.addAll(buildRegionFootsForNullTarg(pd, rra, rMan));
    }
    String noteStr = PertAnnotations.convertFootnoteListToString(foots);
    targ.setFootsForHTML(noteStr);   
    //
    // Time span info
    //
    SortedSet<Integer> mm = timeBounds.get(sst);
    if (mm.size() == 1) {
      NullTimeSpan nts = new NullTimeSpan(mm.first().intValue());
      targ.addTimeSpan(nts); 
    } else {
      NullTimeSpan nts = new NullTimeSpan(mm.first().intValue(), mm.last().intValue());
      targ.addTimeSpan(nts);
    }
    return (targ);
  }
  
 
  /***************************************************************************
  ** 
  ** Resolve footnotes for nulls, including hoists:
  */

  private void resolveFootnotesForNulls(PerturbationData pd, List<PertDataPoint> pertList, 
                                        Map<PertSourcesAndTargKey, Boolean> results, 
                                        Map<PertSources, Set<String>> hoistFoots, 
                                        Map<PertSources, Map<String, List<String>>> retainFoots, ResourceManager rMan) {
      
    HashMap<PertSources, Map<String, Map<String, Integer>>> retainFootCountsForNull = new HashMap<PertSources, Map<String, Map<String, Integer>>>();
    HashMap<PertSources, Map<String, Integer>> pointCountsPerTargPerSource = new HashMap<PertSources, Map<String, Integer>>();
    
    Iterator<PertDataPoint> plit = pertList.iterator();
    while (plit.hasNext()) {
      PertDataPoint pdp = plit.next(); 
      PertSources sources = pdp.getSources(pd);
      String targKey = pdp.getTargetKey();
      PertSourcesAndTargKey sst = new PertSourcesAndTargKey(sources, targKey);
      Boolean currentlyAllBelow = results.get(sst);
      // if not bound for null, we skip:    
      if (!currentlyAllBelow.booleanValue()) { 
        continue;
      }
      List<String> notes = pd.getDataPointNotes(pdp.getID());
      
      //
      // Keep track of the number of data points per target of a source.  We use this
      // to figure out if we have complete, or only partial, footnote coverage for the
      // source:
      //
      
      Map<String, Integer> pointCountsPerTarg = pointCountsPerTargPerSource.get(sources);
      if (pointCountsPerTarg == null) {
        pointCountsPerTarg = new HashMap<String, Integer>();
        pointCountsPerTargPerSource.put(sources, pointCountsPerTarg);
      }
      Integer pointCount = pointCountsPerTarg.get(targKey);
      if (pointCount == null) {
        pointCount = Integer.valueOf(1);
      } else {
        pointCount = Integer.valueOf(pointCount.intValue() + 1);
      }
      pointCountsPerTarg.put(targKey, pointCount);        
  
      //
      // Collect up notes per target of each source.  We count the notes, to
      // see if every data point for the target shares the note!
      //
     
      Map<String, Map<String, Integer>> retainFootCountsPerSource = retainFootCountsForNull.get(sources);
      if (retainFootCountsPerSource == null) {
        retainFootCountsPerSource = new HashMap<String, Map<String, Integer>>();
        retainFootCountsForNull.put(sources, retainFootCountsPerSource);
      }
      Map<String, Integer> retainFootCountsPerTarget = retainFootCountsPerSource.get(targKey);
      if (retainFootCountsPerTarget == null) {
        retainFootCountsPerTarget = new HashMap<String, Integer>();
        retainFootCountsPerSource.put(targKey, retainFootCountsPerTarget);
      }
      
      //
      // 12-10-14: Used to change the type signature of the map during this process below. No more!
      //
      
      Map<String, List<String>> retainFootsPerSource = retainFoots.get(sources);
      if (retainFootsPerSource == null) {
        retainFootsPerSource = new HashMap<String, List<String>>();
        retainFoots.put(sources, retainFootsPerSource);
      }
      
      if (notes != null) {       
        int numNotes = notes.size();
        for (int i = 0; i < numNotes; i++) {
          String noteKey = notes.get(i);
          Integer count = retainFootCountsPerTarget.get(noteKey);
          if (count == null) {
            count = Integer.valueOf(1);
          } else {
            count = Integer.valueOf(count.intValue() + 1);
          }
          retainFootCountsPerTarget.put(noteKey, count);          
        }
      }
      
      //
      // This gathers up the footnotes that can be hoisted up to the
      // source, since everybody shares them:
      //
 
      Set<String> footsPerSource = hoistFoots.get(sources);
      if (footsPerSource == null) {
        footsPerSource = new HashSet<String>();
        if (notes != null) {
          footsPerSource.addAll(notes);
        }
        hoistFoots.put(sources, footsPerSource);
      } else {
        // Gotta keep what is common and toss what is missing!
        HashSet<String> intersect = new HashSet<String>(footsPerSource);
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
   
    Iterator<PertSources> rfit = retainFootCountsForNull.keySet().iterator();
    while (rfit.hasNext()) {
      PertSources sources = rfit.next();
      Map<String, Map<String, Integer>> retainFootCountsPerSource = retainFootCountsForNull.get(sources);
      Map<String, List<String>> retainFootsPerSource = retainFoots.get(sources);
      Map<String, Integer> pointCountsPerTarg = pointCountsPerTargPerSource.get(sources);
      // Revising the map as we go:
      Iterator<String> rffsit = new HashSet<String>(retainFootCountsPerSource.keySet()).iterator(); 
      while (rffsit.hasNext()) {
        String trgKey = rffsit.next();
        Map<String, Integer> retainFootCountsPerTarget = retainFootCountsPerSource.get(trgKey);
        Integer pointCount = pointCountsPerTarg.get(trgKey);
        Iterator<String> rfftit = retainFootCountsPerTarget.keySet().iterator();
        HashSet<String> revised = new HashSet<String>();
        String pnt = null;
        while (rfftit.hasNext()) {
          String footKey = rfftit.next();
          Integer perFootCount = retainFootCountsPerTarget.get(footKey);
          revised.add(footKey);
          if (!perFootCount.equals(pointCount)) {
            pnt = getPartialNullTag(pd, rMan);
          }
        }
        List<String> footsPerSource = pd.getFootnoteList(new ArrayList<String>(revised));
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
  
  private String getPartialNullTag(PerturbationData pd, ResourceManager rMan) {
    String foot = rMan.getString("qpcrDisplay.incompleteNullFootnoteCoverage");    
    String tag = tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
      tempFoots_.put(foot, tag);
    }
    return (tag);
  }
  
  /***************************************************************************
  ** 
  ** Get sources as list
  */
   
  private List<Source> getQPCRSourceList(PertSources pss, PerturbationData pd, PertDictionary pDict) {
    ArrayList<Source> retval = new ArrayList<Source>();
    Iterator<String> sit = pss.getSources();
    while (sit.hasNext()) {
      String srcID = sit.next();    
      PertSource ps = pd.getSourceDef(srcID);
      PertProperties pProps = ps.getExpType(pDict);
      Source newSrc = new Source(ps.getSourceName(pd), pProps.getLegacyType());
      List<String> srcAnnot = ps.getAnnotationIDs();
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

  private NullPerturb nullPerturbationMatchesSource(QPCRData qpcr, List<Source> sources) {  
    Iterator<NullPerturb> npit = qpcr.getNullPerturbations();
    while (npit.hasNext()) {
      NullPerturb chknp = npit.next();
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

  private boolean fillInPerturbations(QPCRData qpcr, HashMap<PertSources, Set<String>> atfs, 
                                      List<PertDataPoint> pertList, 
                                      PerturbationData pd, TimeAxisDefinition tad, PertDictionary pDict,
                                      String scaleKey, Map<String, String> techColors, ResourceManager rMan) {
 
    HashMap<PertSources, Map<String, FormerNullFootData>> droppedMultiNull = new HashMap<PertSources, Map<String, FormerNullFootData>>();
    HashMap<PertSources, Map<String, Set<String>>> hoistFoots = new HashMap<PertSources, Map<String, Set<String>>>();
    Iterator<PertDataPoint> plit = pertList.iterator();
    while (plit.hasNext()) {
      PertDataPoint pdp = plit.next();
      Experiment psi = pdp.getExperiment(pd);
      PertSources ps = psi.getSources();
      Set<String> targs = atfs.get(ps);
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
            Map<String, FormerNullFootData> droppedPerSource = droppedMultiNull.get(ps);
            if (droppedPerSource == null) {
              droppedPerSource = new HashMap<String, FormerNullFootData>();
              droppedMultiNull.put(ps, droppedPerSource);
            }
            FormerNullFootData timesPerDrop = droppedPerSource.get(targKey);
            if (timesPerDrop == null) {
              timesPerDrop = new FormerNullFootData();
              droppedPerSource.put(targKey, timesPerDrop);
            }
            timesPerDrop.times.add(TimeSpan.spanToString(tad, new MinMax(timeVal, timeMax)));
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
      PertDataPoint pdp = plit.next();
      Experiment psi = pdp.getExperiment(pd);
      PertSources ps = psi.getSources();
      Set<String> targs = atfs.get(ps);
      String targKey = pdp.getTargetKey();
      if ((targs == null) || !targs.contains(targKey)) {
        continue;
      }
      Map<String, Set<String>> footsPerSource = hoistFoots.get(ps);
      Set<String> footsPerTarg = footsPerSource.get(targKey);
      Map<String, FormerNullFootData> droppedPerSource = droppedMultiNull.get(ps);
      fillForTarg(qpcr, ps, targKey, pdp, pd, tad, pDict, footsPerTarg, droppedPerSource, scaleKey, techColors, rMan);
    }
    return (true);
  } 
 
  /***************************************************************************
  ** 
  ** Resolve footnote hoisting
  */

  private void resolveFootHoists(PerturbationData pd, PertSources ps, PertDataPoint pdp, Map<PertSources, Map<String, Set<String>>> hoistFoots) {
    String targKey = pdp.getTargetKey();
    List<String> notes = pd.getDataPointNotes(pdp.getID());
    Map<String, Set<String>> footsPerSource = hoistFoots.get(ps);
    if (footsPerSource == null) {
      footsPerSource = new HashMap<String, Set<String>>();
      hoistFoots.put(ps, footsPerSource);
    }
    Set<String> footsPerTarg = footsPerSource.get(targKey);
    if (footsPerTarg == null) {
      footsPerTarg = new HashSet<String>();
      if (notes != null) {
        footsPerTarg.addAll(notes);
      }
      footsPerSource.put(targKey, footsPerTarg);
    } else {
      // Gotta keep what is common and toss what is missing!
      HashSet<String> intersect = new HashSet<String>(footsPerTarg);
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
                           PertDataPoint pdp, PerturbationData pd, TimeAxisDefinition tad, 
                           PertDictionary pDict,
                           Set<String> hoistFoots, Map<String, FormerNullFootData> droppedPerSource, 
                           String scaleKey, Map<String, String> techColors, ResourceManager rMan) {
    
   
    List<Source> sources = getQPCRSourceList(ps, pd, pDict);

    TargetGene gene = qpcr.getQPCRDataRelaxedMatch(pd.getTarget(targKey));
    if (gene == null) {
      List<String> notes = pd.getFootnotesForTarget(targKey);
      String noteStr = (notes == null) ? null : pd.getFootnoteListAsString(notes);
      gene = new TargetGene(pd.getTarget(targKey), noteStr);
      qpcr.addGene(gene, false);
    }
    
    Perturbation pert = null;
    Iterator<Perturbation> pit = gene.getPerturbations();
    while (pit.hasNext()) {
      Perturbation chkPert = pit.next();
      if (chkPert.sourcesMatch(sources)) {
        pert = chkPert;
        break;
      }
    }
    
    if (pert == null) {
      pert = new Perturbation();
      int numSrc = sources.size();
      for (int i = 0; i < numSrc; i++) {
        Source src = sources.get(i);
        ArrayList<String> flist = new ArrayList<String>();
        // last time thru only:
        if (i == (numSrc - 1)) {
          if (!hoistFoots.isEmpty()) {
            flist.addAll(pd.getFootnoteList(new ArrayList<String>(hoistFoots)));
          }
          if (droppedPerSource != null) { 
            FormerNullFootData timesPerDrop = droppedPerSource.get(targKey);
            if (timesPerDrop != null) {
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
              String tag = tempFoots_.get(msg);
              if (tag == null) {
                tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
                tempFoots_.put(msg, tag);
              }
              flist.add(tag);
            }
          }
        }
        
        flist.addAll(src.getFootnoteNumbers());
        TreeSet<String> sortedFoots = new TreeSet<String>(new ReadOnlyTable.NumStrComparator());
        sortedFoots.addAll(flist);
        String newNotes = PertAnnotations.convertFootnoteListToString(new ArrayList<String>(sortedFoots));
        src = src.clone();
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
      String msg = rMan.getString("qpcrDisplay.exactMultiDrop");
      String tag = tempFoots_.get(msg);
      if (tag == null) {
        tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
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
    
    List<String> notes = pd.getDataPointNotes(pdp.getID());
    List<String> localTags = new ArrayList<String>();
  
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
      String tag = buildRegResFootnote(pd, rr, rMan);
      localTags.add(tag);
    }
    
    //
    // remove those that can be hoisted to the pert source:
    //
    
    if (notes != null) {
      notes = new ArrayList<String>(notes);
      notes.removeAll(hoistFoots);
    }
   
    String noteStr = null;     
    if ((notes != null) || !localTags.isEmpty()) {
      PertAnnotations pa = pd.getPertAnnotations();
      List<String> flist = (notes == null) ? new ArrayList<String>() : pa.getFootnoteList(notes);
      TreeSet<String> sorted = new TreeSet<String>(flist);
      sorted.addAll(localTags);
      noteStr = PertAnnotations.convertFootnoteListToString(new ArrayList<String>(sorted)); 
    }
      
    String csvBatch = pdp.getBatchKey();
    String invest = psi.getInvestigatorDisplayString(pd);
    Batch batch = span.getBatchWithIDAndInvest(csvBatch, invest);
    if (batch == null) {
      batch = new Batch(timeVal, pdp.getDate(), invest, csvBatch);
      span.addBatch(batch);
    }
    Measurement meas = new Measurement(tad, pdp.getScaledDisplayValueOldStyle(scaleKey, pd, false), nonStdTime, noteStr, 
                                       pdp.getControl(), pdp.getForcedSignificance(), pdp.getComment());
    if (nonStdMaxTime != null) {
      meas.setNonStdMaxTime(tad, nonStdMaxTime);
    }
    
    String colStr = techColors.get(pdp.getMeasurementTypeKey());
    meas.setColorDisplay(colStr);
    batch.addMeasurement(meas);

    List<String> investigators = psi.getInvestigators();
    for (int i = 0; i < investigators.size(); i++) {
      String investKey = investigators.get(i);
      pert.addInvestigatorIfNew(pd.getInvestigator(investKey));
    }
    return;
  }  

 
  /***************************************************************************
  ** 
  ** Build footnotes for null targets:
  */
  
  private List<String> buildRegionFootsForNullTarg(PerturbationData pd, RegRestrictAggregation rra, ResourceManager rMan) {
    ArrayList<String> retval = new ArrayList<String>();
    if (rra.noRestrict == 0) {
      if (rra.actualFoots.isEmpty()) {
        Iterator<String> lvit = rra.legacyVals.iterator();
        int size = rra.legacyVals.size(); 
        Object[] lvobj = new Object[1];
        if (size == 1) {      
          lvobj[0] = lvit.next();
          String format = rMan.getString("qpcrDisplay.simpleLegacyRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        } else {
          StringBuffer buf = new StringBuffer();
          while (lvit.hasNext()) {
            buf.append(lvit.next());
            if (lvit.hasNext()) {
              buf.append(", ");
            }
          }
          lvobj[0] = buf.toString();
          String format = rMan.getString("qpcrDisplay.mixedLegacyRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        }
      } else if (rra.legacyVals.isEmpty()) {
        Iterator<String> afit = rra.actualFoots.iterator();
        int size = rra.actualFoots.size(); 
        Object[] lvobj = new Object[1];
        if (size == 1) {      
          lvobj[0] = afit.next();
          String format = rMan.getString("qpcrDisplay.simpleRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
            tempFoots_.put(foot, tag);
          }
          retval.add(tag);
          return (retval);
        } else {
          StringBuffer buf = new StringBuffer();
          while (afit.hasNext()) {
            buf.append(afit.next());
            if (afit.hasNext()) {
              buf.append(", ");
            }
          }
          lvobj[0] = buf.toString();
          String format = rMan.getString("qpcrDisplay.mixedRegionRestrictionFormat");
          String foot = MessageFormat.format(format, lvobj);
          String tag = tempFoots_.get(foot);
          if (tag == null) {
            tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
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
    Iterator<String> lvit = rra.legacyVals.iterator();
    Iterator<String> afit = rra.actualFoots.iterator();
    while (lvit.hasNext()) {
      buf.append("\"");
      buf.append(lvit.next());
      buf.append("\"");
      if (lvit.hasNext() || afit.hasNext()) {
        buf.append(", ");
      }
    }
    while (afit.hasNext()) {
      buf.append("\"");
      buf.append(afit.next());
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
   
    String tag = tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
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
      TreeSet<String> sorted = new TreeSet<String>();
      Iterator<String> nrrit = rr.getRegions();
      while (nrrit.hasNext()) {
        String region = nrrit.next();
        sorted.add(region);
      }     
      nrrit = sorted.iterator();
      while (nrrit.hasNext()) {
        String region = nrrit.next();
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

  private String buildRegResFootnote(PerturbationData pd, PerturbationData.RegionRestrict rr, ResourceManager rMan) {
    String regList;
    if (rr.isLegacyNullStyle()) {
      regList = rr.getLegacyValue();
    } else {
      regList = buildRegResRegions(rr);
    }
    
    String format = rMan.getString("qpcrDisplay.regionRestrictionFormat");
    String foot = MessageFormat.format(format, new Object[] {regList});             
    String tag = tempFoots_.get(foot);
    if (tag == null) {
      tag = pd.getPertAnnotations().getNextTempTag(new HashSet<String>(tempFoots_.values()));
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

    @Override
    public int hashCode() {
      return (srcs.hashCode() + targKey.hashCode());
    }
    
    @Override
    public String toString() {
      return ("PertSourcesAndTarg: " + srcs + " " + targKey);
    }
    
    @Override
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
    TreeSet<String> legacyVals;
    TreeSet<String> actualFoots;
    int noRestrict;

    RegRestrictAggregation() {
      legacyVals = new TreeSet<String>();
      actualFoots = new TreeSet<String>();
      noRestrict = 0;
    }
  }
  
  /***************************************************************************
  ** 
  ** For gathering up formerly null data for footnotes
  */  
  
  private static class FormerNullFootData {    
    TreeSet<String> times;
    TreeSet<String> regions;

    FormerNullFootData() {
      times = new TreeSet<String>();
      regions = new TreeSet<String>();
    }
    
    private String getStringList(TreeSet<String> set) {
      StringBuffer buf = new StringBuffer();
      Iterator<String> sit = set.iterator();
      while (sit.hasNext()) {
        String str = sit.next();
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
