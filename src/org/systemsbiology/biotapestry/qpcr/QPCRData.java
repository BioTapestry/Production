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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.perturb.ConditionDictionary;
import org.systemsbiology.biotapestry.perturb.LegacyPert;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.Experiment;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationData.KeyAndDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This holds QPCR data
*/

class QPCRData {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final String DUMMY_NULL_TARG_KEY_ = "__WJRL_HacKasTic_GenE_NamE__";
  private static final int HEADING_SPACING_ = 18;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ArrayList<TargetGene> genes_;
  private ArrayList<NullPerturb> nullPerturbations_;
  private NullTimeSpan nullPertDefaultSpan_;  
  private ArrayList<Footnote> footnotes_;
  private ArrayList<MinMax> timeSpanCols_;
  private HashMap<String, List<String>> entryMap_;
  private HashMap<String, List<String>> sourceMap_;
  private double threshold_;
  private long serialNumber_;
  private TabPinnedDynamicDataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  QPCRData(TabPinnedDynamicDataAccessContext dacx) {
    dacx_ = dacx;
    genes_ = new ArrayList<TargetGene>();
    nullPerturbations_ = new ArrayList<NullPerturb>();
    nullPertDefaultSpan_ = null;
    footnotes_ = new ArrayList<Footnote>();
    timeSpanCols_ = new ArrayList<MinMax>();
    entryMap_ = new HashMap<String, List<String>>();
    sourceMap_ = new HashMap<String, List<String>>();
    threshold_ = 1.6;
    serialNumber_ = 0L;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Stock the new perturbation system from the old QPCR storage:
  */
  
  void transferFromLegacy() {
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    DataMapSource dms = dacx_.getDataMapSrc();
    PerturbationDataMaps pdms = dms.getPerturbationDataMaps();
    if (pdms == null) {
      pdms = new PerturbationDataMaps(dacx_);
      dms.setPerturbationDataMaps(pdms);
    }

    PertDictionary pDict = pd.getPertDictionary();
    MeasureDictionary mDict = pd.getMeasureDictionary();
    ConditionDictionary cDict = pd.getConditionDictionary();
    String legCondition = cDict.getStandardConditionKey(); 
    String legMeasure = mDict.createLegacyMeasureProps(threshold_);
    
    DisplayOptions dOpt = dacx_.getDisplayOptsSource().getDisplayOptions(); 
    dOpt.setColumns(getColumns());
    
    long timeStamp = System.currentTimeMillis();
    ArrayList<String> unkInvs = new ArrayList<String>();
   
    HashMap<Set<PertSource>, Map<String, Set<String>>> annotsForSrcs = new HashMap<Set<PertSource>, Map<String, Set<String>>>();
    HashMap<Set<PertSource>, Map<String, Set<String>>> annotsToData = new HashMap<Set<PertSource>, Map<String, Set<String>>>();
    factorSourceAnnots(pd, pDict, annotsForSrcs, annotsToData);
      
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TargetGene tgene = genes_.get(i);
      List<String> gnotes = tgene.getTranslatedNotes();
      List<String> gidList = footNumsToIDs(pd, gnotes);    
      String targName = tgene.getName();
      KeyAndDataChange kadc = pd.provideTarget(targName);
      String targKey = kadc.key;
      if (!gidList.isEmpty()) {
        pd.setFootnotesForTargetIO(targKey, gidList);
      }
      Iterator<Perturbation> pit = tgene.getPerturbations();   
      while (pit.hasNext()) {
        Perturbation prt = pit.next();   
        ArrayList<String> srcList = new ArrayList<String>();
        Iterator<Source> sit1 = prt.getSources(); 
        Set<PertSource> keypss = perturbationToKey(pd, pDict, sit1, new ArrayList<String>());
        Set<String> retainNotes = annotsForSrcs.get(keypss).keySet(); 
        Map<String, Set<String>> notesToTargs = annotsToData.get(keypss);
        Iterator<Source> sit = prt.getSources(); 
        while (sit.hasNext()) {
          Source src = sit.next();
          String srcID = sourceToPertSource(src, pd, pDict, retainNotes);
          srcList.add(srcID);
        }
        ArrayList<String> pInvestList = new ArrayList<String>();
        Iterator<String> piit = prt.getInvestigators();
        while (piit.hasNext()) {
          String inv = piit.next();
          pInvestList.add(inv);
        }
        PertSources srcs = new PertSources(srcList);
        Iterator<TimeSpan> tsit = prt.getTimeSpans();
        while (tsit.hasNext()) {
          TimeSpan tspan = tsit.next();
          PerturbationData.RegionRestrict rr = null;
          Iterator<String> rrit = tspan.getRegionRestrictions();
          if (rrit.hasNext()) {
            ArrayList<String> regionList = new ArrayList<String>();
            while (rrit.hasNext()) {
              String regID = rrit.next();
              regionList.add(regID);
            }
            rr = new PerturbationData.RegionRestrict(regionList);
          }         
          MinMax spanTimes = tspan.getMinMaxSpan();
          Iterator<Batch> tbit = tspan.getBatches();
          int subBatchKey = 0;
          while (tbit.hasNext()) {
            Batch batch = tbit.next();
            String invests = batch.getInvestigators();
            List<String> investList = (invests != null) ? Perturbation.unformatInvestigators(invests) : pInvestList;
            ArrayList<String> invKeyList = new ArrayList<String>();
            if (investList.isEmpty()) {
              invKeyList = unkInvs;
            } else {
              int numI = investList.size();
              for (int j = 0; j < numI; j++) {
                String invest  = investList.get(j);
                invKeyList.add(pd.provideInvestigator(invest).key);     
              }
              
            }
            int batchTime = batch.getTimeNumber();
            int legacyMax = Experiment.NO_TIME;
            int useMin = (batchTime == Experiment.NO_TIME) ? spanTimes.min : batchTime;
            if ((batchTime == Experiment.NO_TIME) && (spanTimes.min != spanTimes.max)) {
              legacyMax = spanTimes.max;
            }
            String batchKey = batch.getBatchKey();
            if (batchKey == null) {
              String maxStr = (legacyMax == Experiment.NO_TIME) ? "" : "_" + Integer.toString(legacyMax);
              batchKey = QpcrLegacyPublicExposed.LEGACY_BATCH_PREFIX + Integer.toString(useMin) + maxStr + "::" + subBatchKey++;
            } else {
              batchKey = batchKey.replaceAll(" ", "");
            }
            String date = batch.getDate();
            String psiKey = pd.provideExperiment(srcs, useMin, legacyMax, invKeyList, legCondition).key;
            Iterator<Measurement> bit = batch.getMeasurements();
            while (bit.hasNext()) {
              Measurement mea = bit.next();
              exportMeasurement(mea, pd, srcs, psiKey, timeStamp, targKey, 
                                batchKey, date, legMeasure, invKeyList, rr, 
                                notesToTargs, DataUtil.normKey(targName), legCondition);
            }
          }   
        }     
      }
    }
    
    //
    // Now handle null perturbations:
    //
  
    NullTimeSpan dnts = getLegacyNullPerturbationsDefaultTimeSpan(dacx_);
 
    int numNull = nullPerturbations_.size();
    for (int i = 0; i < numNull; i++) {
      NullPerturb np = nullPerturbations_.get(i);
      Iterator<Source> sit1 = np.getSources();
      Set<PertSource> keypss = perturbationToKey(pd, pDict, sit1, new ArrayList<String>());
      Map<String,Set<String>> notesToTargs = annotsToData.get(keypss);
    
      Set<String> retainNotes = annotsForSrcs.get(keypss).keySet();   
      ArrayList<String> srcList = new ArrayList<String>();
      Iterator<Source> sit = np.getSources();
      while (sit.hasNext()) {
        Source src = sit.next();
        String srcID = sourceToPertSource(src, pd, pDict, retainNotes);
        srcList.add(srcID);
      }
      PertSources srcs = new PertSources(srcList);
      MinMax fallbackTime = new MinMax(dnts.getMin(), dnts.getMax()); 
      Iterator<NullTarget> nit = np.getTargets();
      while (nit.hasNext()) {
        NullTarget nt = nit.next();
        String targName = nt.getTarget();
        KeyAndDataChange kadc = pd.provideTarget(targName);
        String targKey = kadc.key; 
        String regRestrict = nt.getRegionRestriction();
        PerturbationData.RegionRestrict rr = null;
        if (regRestrict != null) {
          rr = new PerturbationData.RegionRestrict(regRestrict);
        }      
              
        //
        // If we do not have times, then the default times will apply:
        //
        
        int numTimes = nt.getTimesCount();
        if (numTimes == 0) {
          exportNoDataNullMeasurement(fallbackTime.min, fallbackTime.max, pd, srcs, 
                                      unkInvs, timeStamp, targKey,legMeasure, rr, notesToTargs, legCondition);
        }
           
        if (nt.getBatchCount() == 0) {    
          if (numTimes > 0) { 
            Iterator<NullTimeSpan> tmit = nt.getTimes();
            while (tmit.hasNext()) {
              NullTimeSpan nts = tmit.next();
              int minTime;
              int maxTime;
              if (nts.isASpan()) {
                minTime = nts.getMin();
                maxTime = nts.getMax();
              } else {
                minTime = nts.getTime();
                maxTime =  Experiment.NO_TIME;
              }
              exportNoDataNullMeasurement(minTime, maxTime, pd, srcs, unkInvs, timeStamp, targKey, legMeasure, 
                                          rr, notesToTargs, legCondition);
            }
          }
        } else {
          if (numTimes > 0) {
            Iterator<Batch> spit = nt.getSupportData();
            HashSet<Integer> dataNums = new HashSet<Integer>();
            while (spit.hasNext()) {
              Batch ntb = spit.next();
              int batchTime = ntb.getTimeNumber();
              dataNums.add(Integer.valueOf(batchTime));
            }
            Iterator<NullTimeSpan> tmit = nt.getTimes();
            while (tmit.hasNext()) {
              NullTimeSpan nts = tmit.next();
              int minTime;
              int maxTime;
              if (nts.isASpan()) {
                minTime = nts.getMin();
                maxTime = nts.getMax();
                if (dataNums.contains(new Integer(minTime)) && dataNums.contains(new Integer(maxTime))) {
                  continue;  // Skip this fake span; we have data for it
                }
              } else {
                minTime = nts.getTime();
                maxTime = Experiment.NO_TIME;
                if (dataNums.contains(new Integer(minTime))) {
                  continue;  // Skip this fake span; we have data for it
                }
              }
              exportNoDataNullMeasurement(minTime, maxTime, pd, srcs, 
                                          unkInvs, timeStamp, targKey, legMeasure, 
                                          rr, notesToTargs, legCondition);
            }
          }
          Iterator<Batch> spit = nt.getSupportData();
          int subBatchKey = 0;
          while (spit.hasNext()) {
            Batch ntb = spit.next();
            String invests = ntb.getInvestigators();
            List<String> investList = Perturbation.unformatInvestigators(invests);
            ArrayList<String> invKeyList = new ArrayList<String>();
            if (investList.isEmpty()) {
              invKeyList = unkInvs;
            } else {
              int numI = investList.size();
              for (int j = 0; j < numI; j++) {
                String invest  = investList.get(j);
                invKeyList.add(pd.provideInvestigator(invest).key);     
              }   
            }
            int batchTime = ntb.getTimeNumber();
            String batchKey = ntb.getBatchKey();
            if (batchKey == null) {
              batchKey = QpcrLegacyPublicExposed.LEGACY_BATCH_PREFIX + Integer.toString(batchTime) + "::" + subBatchKey++;
            } else {
              batchKey = batchKey.replaceAll(" ", "");
            }
            String date = ntb.getDate();
            
            String psiKey = pd.provideExperiment(srcs, batchTime, Experiment.NO_TIME, invKeyList, legCondition).key;
            Iterator<Measurement> bit = ntb.getMeasurements();
            while (bit.hasNext()) {
              Measurement mea = bit.next();
              exportMeasurement(mea, pd, srcs, psiKey, timeStamp, targKey, 
                                batchKey, date, legMeasure, invKeyList, rr, 
                                notesToTargs, DUMMY_NULL_TARG_KEY_, legCondition);
            }
          }
        }
      }     
    }
    
    
    Iterator<String> emit = entryMap_.keySet().iterator();
    while (emit.hasNext()) {
      String eKey = emit.next();
      List<String> forKey = entryMap_.get(eKey);
      pd.importLegacyEntryMapEntry(eKey, forKey, pdms);
    }
    Iterator<String> smit = sourceMap_.keySet().iterator();
    while (smit.hasNext()) {
      String sKey = smit.next();
      List<String> forKey = sourceMap_.get(sKey);
      pd.importLegacySourceMapEntry(sKey, forKey, pdms);
    }
 
    genes_.clear();
    nullPerturbations_.clear();
    entryMap_.clear();
    sourceMap_.clear();
    return;
  }
  
   /***************************************************************************
  **
  ** Export the measurement
  */
  
  private void exportNoDataNullMeasurement(int minTime, int maxTime, PerturbationData pd, PertSources srcs, 
                                           List<String> unkInvs, long timeStamp, String targKey, String legMeasure, 
                                           PerturbationData.RegionRestrict regRestrict, 
                                           Map<String, Set<String>> notesToTargs, String legCondition) {
   
    //
    // Add in annots that really belong with the data, not with the source:
    //
    
    ArrayList<String> idList = new ArrayList<String>(); 
    if (notesToTargs != null) {
      Iterator<String> nttkit = notesToTargs.keySet().iterator();
      while (nttkit.hasNext()) {
        String noteID = nttkit.next();
        Set<String> forTargs = notesToTargs.get(noteID);
        if (forTargs.contains(DUMMY_NULL_TARG_KEY_)) {
          idList.add(noteID);
        }
      }
    }
     
    String psiKey = pd.provideExperiment(srcs, minTime, maxTime, unkInvs, legCondition).key;
    PertDataPoint pdp = new PertDataPoint(pd.getNextDataKey(), timeStamp, psiKey, 
                                          targKey, legMeasure, 0.0); 
    LegacyPert lp = new LegacyPert("NS", true);
   
    String maxStr = (maxTime == Experiment.NO_TIME) ? "" : "_" + Integer.toString(maxTime);
    String batchKey = QpcrLegacyPublicExposed.LEGACY_BATCH_PREFIX + Integer.toString(minTime) + maxStr + "::0";
    
    pdp.setBatchKey(batchKey);
    pdp.setLegacyPert(lp);
    pdp.setIsSig(null);
    pd.addDataPointForIO(pdp);
    if (regRestrict != null) {
      pd.setRegionRestrictionForDataPointForIO(pdp.getID(), regRestrict);
    }
    if (!idList.isEmpty()) {
      pd.setFootnotesForDataPointForIO(pdp.getID(), idList);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Generate a key for footnote transfer
  */
  
  private Set<PertSource> perturbationToKey(PerturbationData pd, PertDictionary pDict, 
                                            Iterator<Source> sit, List<String> allFoots) {
    ArrayList<String> emptyList = new ArrayList<String>();
    HashSet<PertSource> allSrcs = new HashSet<PertSource>();
    while (sit.hasNext()) {
      Source src = sit.next(); 
      String base = src.getBaseType();
      String expr = src.getExpType();
      String pertKey = pDict.getPerturbPropsFromName(expr);
      if (pertKey == null) {
        throw new IllegalStateException();
      }
      List<String> sfoots = src.getFootnoteNumbers();
      allFoots.addAll(footNumsToIDs(pd, sfoots));
      PertSource ps = new PertSource("", DataUtil.normKey(base), pertKey, emptyList);
      allSrcs.add(ps);
    }
    return (allSrcs);
  }

  /***************************************************************************
  **
  ** Many source annotations actually belong with the data points, not the
  ** sources.  If a source annotation is common everywhere, it goes with
  ** the source.  If not, it is tacked onto the data points.
  */
  
  private void factorSourceAnnots(PerturbationData pd, PertDictionary pDict, 
                                  HashMap<Set<PertSource>, Map<String, Set<String>>> annotsForSrcs, 
                                  HashMap<Set<PertSource>, Map<String, Set<String>>> annotsToData) {   
  
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TargetGene tgene = genes_.get(i);
      String normName = DataUtil.normKey(tgene.getName());
      Iterator<Perturbation> pit = tgene.getPerturbations();   
      while (pit.hasNext()) {
        Perturbation prt = pit.next();
        ArrayList<String> allFoots = new ArrayList<String>();
        Set<PertSource> pssk = perturbationToKey(pd, pDict, prt.getSources(), allFoots);
        factorSourceAnnotsGuts(normName, pssk, allFoots, annotsForSrcs, annotsToData);
      }
    }
      
    int numNull = nullPerturbations_.size();
    for (int i = 0; i < numNull; i++) {
      NullPerturb np = nullPerturbations_.get(i);
      ArrayList<String> allFoots = new ArrayList<String>();
      Set<PertSource> pssk = perturbationToKey(pd, pDict, np.getSources(), allFoots);
      factorSourceAnnotsGuts(DUMMY_NULL_TARG_KEY_, pssk, allFoots, annotsForSrcs, annotsToData);
    }
    return;
  }

  /***************************************************************************
  **
  ** Guts of above routine
  **
  */
  
  private void factorSourceAnnotsGuts(String targetName, Set<PertSource> pssk, List<String> allFoots,
                                      HashMap<Set<PertSource>, Map<String, Set<String>>> annotsForSrcs, 
                                      HashMap<Set<PertSource>, Map<String, Set<String>>> annotsToData) {   
   
    HashSet<String> idSet = new HashSet<String>(allFoots);
    Map<String, Set<String>> notes = annotsForSrcs.get(pssk);
    Map<String, Set<String>> goesToData = annotsToData.get(pssk);
    // First time seen:
    if (notes == null) {
      notes = new HashMap<String, Set<String>>();
      annotsForSrcs.put(pssk, notes);
      Iterator<String> idsit = idSet.iterator();
      while (idsit.hasNext()) {
        String noteID = idsit.next();
        HashSet<String> forNote = new HashSet<String>();
        notes.put(noteID, forNote);
        forNote.add(targetName);
      }  
    } else { // only common source notes stay, else they get moved to data entries:
      //
      // If already gone to data, they get transferred ASAP:
      //
      if (goesToData != null) {
        HashSet<String> intersectToData = new HashSet<String>(goesToData.keySet());
        intersectToData.retainAll(idSet);
        Iterator<String> i2dit = intersectToData.iterator();
        while (i2dit.hasNext()) {
          String noteID = i2dit.next();
          Set<String> toDatForTargs = goesToData.get(noteID);
          toDatForTargs.add(targetName);
          idSet.remove(noteID);
        }
      }
      //
      // Guys left get to try to stay with the source:
      //
      HashSet<String> allKeys = new HashSet<String>(notes.keySet());
      HashSet<String> intersect = new HashSet<String>(allKeys);
      intersect.retainAll(idSet);
      if (goesToData == null) {
        goesToData = new HashMap<String, Set<String>>();
        annotsToData.put(pssk, goesToData);
      } 
      Iterator<String> akit = allKeys.iterator();
      while (akit.hasNext()) {
        String noteKey = akit.next();
        if (!intersect.contains(noteKey)) {
          Set<String> forTrgs = notes.remove(noteKey);
          Iterator<String> ftit = forTrgs.iterator();
          while (ftit.hasNext()) {
            String targ = ftit.next();
            Set<String> toDatForTargs = goesToData.get(noteKey);
            if (toDatForTargs == null) {
              toDatForTargs = new HashSet<String>();
              goesToData.put(noteKey, toDatForTargs);
            }
            toDatForTargs.add(targ);
          }
        } else {
          Set<String> forTrgs = notes.get(noteKey);
          forTrgs.add(targetName);
        }
      }
      //
      // Anybody left needs to go to the data-based notes:
      //
      idSet.removeAll(intersect);
      Iterator<String> idit = idSet.iterator();
      while (idit.hasNext()) {
        String noteID = idit.next();
        Set<String> toDatForTargs = goesToData.get(noteID);
        if (toDatForTargs == null) {
          toDatForTargs = new HashSet<String>();
          goesToData.put(noteID, toDatForTargs);
        }
        toDatForTargs.add(targetName);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Convert perturbation sources
  */
  
  private String sourceToPertSource(Source src, PerturbationData pd, PertDictionary pDict, Set<String> retainNotes) { 
    String base = src.getBaseType();
    PerturbationData.KeyAndDataChange kdac = pd.providePertSrcName(base);
    String expr = src.getExpType();
    List<String> sfoots = src.getFootnoteNumbers();
    String pSign = src.getProxySign();
    pSign = PertSource.mapLegacyProxySign(pSign);
    String proxNameID = null;
    if (!pSign.equals(PertSource.NO_PROXY)) {
      String proxyFor = src.getProxiedSpecies();
      PerturbationData.KeyAndDataChange kdac2 = pd.providePertSrcName(proxyFor);
      proxNameID = kdac2.key;
    }
    
    List<String> idList = footNumsToIDs(pd, sfoots);
   
    String pertKey = pDict.getPerturbPropsFromName(expr);
    if (pertKey == null) {
      throw new IllegalStateException();
    }
    ArrayList<String> useList = new ArrayList<String>();
    int numL = idList.size();
    for (int i = 0; i < numL; i++) {
      String annotID = idList.get(i);
      if (retainNotes.contains(annotID)) {
        useList.add(annotID);
      }
    } 
    PerturbationData.KeyAndDataChange kdac3 = pd.providePertSrc(kdac.key, pertKey, proxNameID, pSign, useList, false);
    return (kdac3.key);
  }
  
  /***************************************************************************
  **
  ** process footnote numbers
  */
  
  private List<String> footNumsToIDs(PerturbationData pd, List<String> noteList) { 
    Map<String, String> footMsg = translateFootNumbers(noteList);
    Iterator<String> fmit = footMsg.keySet().iterator();
    ArrayList<String> idList = new ArrayList<String>();
    while (fmit.hasNext()) {
      String key = fmit.next();
      String msg = footMsg.get(key);
      String msgID = pd.addLegacyMessage(key, msg);
      if (msgID == null) {
        throw new IllegalStateException();
      }
      idList.add(msgID);
    }
    return (idList);
  }
 
  /***************************************************************************
  **
  ** Export the measurement
  */
  
  private void exportMeasurement(Measurement mea, PerturbationData pd, PertSources srcs, 
                                 String psiKey, long timeStamp, 
                                 String targKey, String batchKey, 
                                 String date, String legMeasure, List<String> invKeyList, 
                                 PerturbationData.RegionRestrict regRestrict, 
                                 Map<String, Set<String>> notesToTargs, String useKey, String legCondition) {
    Object valObj = getMeANumber(mea);
    double value = 0.0;
    LegacyPert lv = null;
    if (valObj instanceof Double) {
      value = ((Double)valObj).doubleValue();
    } else {
      lv = (LegacyPert)valObj;
    }
    //
    // Drop significance object unless it is used to 
    // force the value:
    //
    Boolean isSig = mea.getIsSignificant();
    if (lv == null) {
      if (Math.abs(value) >= threshold_) {
        if (isSig.booleanValue()) {
          isSig = null;
        }
      } else {
        if (!isSig.booleanValue()) {
          isSig = null;
        }
      } 
    }             
    String ctrl = mea.getControl();
    String comment = mea.getComment();
    Integer legNonStandard = mea.getTime();
    List<String> noteList = mea.getFootnoteNumbers();
    List<String> idList = footNumsToIDs(pd, noteList);
      
    //
    // Add in annots that really belong with the data, not with the source:
    //
     
    if (notesToTargs != null) {
      Iterator<String> nttkit = notesToTargs.keySet().iterator();
      while (nttkit.hasNext()) {
        String noteID = nttkit.next();
        Set<String> forTargs = notesToTargs.get(noteID);
        if (forTargs.contains(useKey)) {
          idList.add(noteID);
        }
      }
    }
   
    String usePsiKey = psiKey;
    if (legNonStandard != null) {
      usePsiKey = pd.provideExperiment(srcs, legNonStandard.intValue(), Experiment.NO_TIME, invKeyList, legCondition).key;
    }
    PertDataPoint pdp = new PertDataPoint(pd.getNextDataKey(), timeStamp, usePsiKey, targKey, legMeasure, value);
    if (!idList.isEmpty()) {
      pd.setFootnotesForDataPointForIO(pdp.getID(), idList);
    }    
    if (lv != null) {
      pdp.setLegacyPert(lv);
      pdp.setIsSig(null);
    } else if (isSig != null) {
      pdp.setIsSig(isSig);
    }
    pdp.setBatchKey(batchKey);
    pdp.setDate(date);
    pdp.setComment(comment);
    if ((ctrl!= null) && !ctrl.trim().equals("")) {
      PerturbationData.KeyAndDataChange kdac = pd.provideExpControl(ctrl);
      pdp.setControl(kdac.key);
    }
    pd.addDataPointForIO(pdp);  
    if (regRestrict != null) {
      pd.setRegionRestrictionForDataPointForIO(pdp.getID(), regRestrict);
    }
    
    return;
  }   
 
  /***************************************************************************
  **
  ** Translate for export
  */
  
  private Map<String, String> translateFootNumbers(List<String> footList) {
    HashMap<String, String> retval = new HashMap<String, String>();
    int numFL = footList.size();
    for (int i = 0; i < numFL; i++) {
      String flNum = footList.get(i);
      int numf = footnotes_.size();
      for (int j = 0; j < numf; j++) {
        Footnote fn = footnotes_.get(j);
        String num = fn.getNumber();
        if (num.equals(flNum)) {
          retval.put(num, fn.getNote());
        }
      }
    }
    return (retval);  
  }
  

  /***************************************************************************
  **
  ** Return either a Legacy Pert or a double val
  */
  
  private Object getMeANumber(Measurement m) {
    // This may be a number or not:
    String val = m.getValue();
    if (val == null) {
      return (new LegacyPert(val));
    }   
    try {
      Double valObj = new Double(val);
      return (valObj);
    } catch (NumberFormatException nfe) {
      return (new LegacyPert(val));
    }
  }
 
  /***************************************************************************
  **
  ** Return the serialNumber
  */
  
   long getSerialNumber() {
    return (serialNumber_);
  }
  
  /***************************************************************************
  **
  ** Set the serialNumber.  Only used in db undo transaction closings!
  */
  
   void setSerialNumber(long serialNumber) {
    serialNumber_ = serialNumber;
    return;
  }
  
  /***************************************************************************
  **
  ** Return the threshold value
  */
  
   double getThresholdValue() {
    return (threshold_);
  }
 
  /***************************************************************************
  **
  ** Answers if the threshold is locked
  **
  */
  
   boolean thresholdLocked() {
    return ((genes_.size() > 0) || (nullPerturbations_.size() > 0));
  }
 
  /***************************************************************************
  **
  ** Answers if we have QPCR data
  **
  */
  
   boolean haveData() {
 
    if (genes_.size() > 0) {
      return (true);
    }

    if (nullPerturbations_.size() > 0) {
      return (true);
    }
 
    if (footnotes_.size() > 0) {
      return (true);
    }

    if ((timeSpanCols_ != null) && (timeSpanCols_.size() > 0)) {
      return (true);
    }

    if (entryMap_.size() > 0) {
      return (true);
    }  
    
    if (sourceMap_.size() > 0) {
      return (true);
    }    
    
    return (false);
  }
  
  /***************************************************************************
  **
  ** Add a gene
  */
  
  @SuppressWarnings("unused")
  void addGene(TargetGene gene, boolean bumpSerial) {
    genes_.add(gene);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the genes
  */
  
   Iterator<TargetGene> getGenes() {
    return (genes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Answer if we have default time span
  */
  
   boolean hasDefaultTimeSpan() {
    return (nullPertDefaultSpan_ != null);
  }  
  
  /***************************************************************************
  **
  ** Answer if we have null perturbations
  */
  
   boolean hasNullPerturbations() {
    return (!nullPerturbations_.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Add a null perturbation
  */
  
  void addNullPerturbation(NullPerturb np) {
    nullPerturbations_.add(np);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the null perturbations
  */
  
   Iterator<NullPerturb> getNullPerturbations() {
    return (nullPerturbations_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the given null perturbation
  */
  
  NullPerturb getNullPerturbation(int index) {
    return (nullPerturbations_.get(index));
  }   

  /***************************************************************************
  **
  ** Get the default time span for null perturbations
  */
  
  NullTimeSpan getLegacyNullPerturbationsDefaultTimeSpan(TabPinnedDynamicDataAccessContext dacx) {
    //
    // Kinda weird; don;t actually set it unless user does so explicitly. Don't
    // want it written out or considered committed unless we actually set it.
    if (nullPertDefaultSpan_ == null) {
      MinMax mm = dacx.getExpDataSrc().getTimeAxisDefinition().getDefaultTimeSpan();
      return (new NullTimeSpan(dacx, mm.min, mm.max));
    } else {
      return (nullPertDefaultSpan_);
    }
  } 
  
  /***************************************************************************
  **
  ** Set the default time span for null perturbations
  */
  
  void setNullPerturbationsDefaultTimeSpan(NullTimeSpan span) {
    nullPertDefaultSpan_ = span;
    return;
  }   
  
  /***************************************************************************
  **
  ** Add a footnote
  */
  
  void addFootnote(Footnote note) {
    footnotes_.add(note);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a footnote
  */
  
   void dropFootnotes() {
    footnotes_.clear();
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the footnote
  */
  
   Iterator<Footnote> getFootnotes() {
    return (footnotes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Set the column headings
  */
  
   void setColumns(ArrayList<MinMax> columns) {
    timeSpanCols_ = columns;
    return;
  }

  /***************************************************************************
  **
  ** Set a column heading
  */
  
   void addColumn(MinMax col) {
    timeSpanCols_.add(col);
    return;
  }
  
  /***************************************************************************
  **
  ** Set a column heading
  */
  
   void addColumn(int min, int max) {
    timeSpanCols_.add(new MinMax(min, max));
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if column definitions are "locked"
  */
  
   boolean columnDefinitionsUsed() {
    Iterator<TargetGene> git = getGenes();
    if (git.hasNext()) {
      TargetGene tg = git.next();
      Iterator<Perturbation> pit = tg.getPerturbations();
      if (pit.hasNext()) {
        Perturbation pert = pit.next();
        Iterator<TimeSpan> sit = pert.getTimeSpans();
        return (sit.hasNext());
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Get the column headings (note we are handing out the actual list DANGER!)
  */
  
   ArrayList<MinMax> getColumns() {
    return (timeSpanCols_);
  }
  
  /***************************************************************************
  **
  ** Get the column headings iterator
  */
  
   Iterator<MinMax> getColumnIterator() {
    return (timeSpanCols_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Answer if we have column headings
  */
  
   boolean hasColumns() {
    return (timeSpanCols_.size() > 0);
  } 
  
  /***************************************************************************
  **
  ** Extract the minimum from the column contents Legacy only
  */
  
   static int getMinimum(String colVal) {
    return (getMaxOrMin(colVal, true));
  }  
  
  /***************************************************************************
  **
  ** Extract the maximum from the column contents Legacy only
  */
  
   static int getMaximum(String colVal) {
    return (getMaxOrMin(colVal, false));
  }  

  /***************************************************************************
  **
  ** Break out the max or min (ICKY)
  */
  
  private static int getMaxOrMin(String col, boolean doMin) {
    //
    // Parse column, look for bounds:
    //
    Pattern colPat = Pattern.compile("\\s*(\\d+)\\s*[hH]?\\s*-\\s*(\\d+)\\s*[hH]?\\s*");
    Matcher m = colPat.matcher(col);
    if (m.matches()) {
      try {
        if (doMin) {
          return (Integer.parseInt(m.group(1)));
        } else {
          return (Integer.parseInt(m.group(2)));
        }
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException();
      }
    }
    throw new IllegalArgumentException();
  }  

  /***************************************************************************
  **
  ** Get the column that matches the given time;
  */
  
   MinMax getEnclosingColumn(int time, int timeMax) {    
    int cols = timeSpanCols_.size();
    if (timeMax != Experiment.NO_TIME) {
      for (int i = 0; i < cols; i++) {
        MinMax col = timeSpanCols_.get(i);
        if ((time == col.min) && (timeMax == col.max)) {
          return (col);  
        }
      }
    } else {
      for (int i = 0; i < cols; i++) {
        MinMax col = timeSpanCols_.get(i);
        if ((time >= col.min) && (time <= col.max)) {
          return (col);
        }
      }
    }  
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Get the column that matches the given time;
  */
  
   MinMax getEnclosingOrClosestColumn(int time, int timeMax) { 
    MinMax exact = getEnclosingColumn(time, timeMax);
    if (exact != null) {
      return (exact);
    }
    if (timeMax != Experiment.NO_TIME) {
      return (getBestSpanColumn(time, timeMax));
    }   
    int minDist = Integer.MAX_VALUE;
    MinMax best = null;
    int cols = timeSpanCols_.size();
    for (int i = 0; i < cols; i++) {
      MinMax col = timeSpanCols_.get(i);
      int chkDistMin = Math.abs(col.min - time);
      int chkDistMax = Math.abs(col.max - time);
      if (chkDistMin < minDist) {
        minDist = chkDistMin;
        best = col;
      }
      if (chkDistMax < minDist) {
        minDist = chkDistMax;
        best = col;
      }
    }
    return (best);
  }  
   
  /***************************************************************************
  **
  ** Get the column that best fits the given SPAN;
  */
  
   MinMax getBestSpanColumn(int time, int timeMax) { 
  
    MinMax checkSpan = new MinMax(time, timeMax);
    SortedSet<Integer> checkSet = checkSpan.getAsSortedSet();
    int minDisjointDist = Integer.MAX_VALUE;
    MinMax bestDisjoint = null;
    int maxOverlap = Integer.MIN_VALUE;
    MinMax bestOverlap = null;
    
    int cols = timeSpanCols_.size();
    for (int i = 0; i < cols; i++) {
      MinMax col = timeSpanCols_.get(i);
      MinMax.SetRel eval = col.evaluate(checkSpan);
      if ((eval == MinMax.SetRel.IS_PROPER_SUBSET) || (eval == MinMax.SetRel.EQUALS)) {
        return (col);
      } else if ((eval == MinMax.SetRel.INTERSECTS) || (eval == MinMax.SetRel.IS_PROPER_SUPERSET)) {
        SortedSet<Integer> colSet = col.getAsSortedSet();
        colSet.retainAll(checkSet);
        int sizeOver = colSet.size();
        if (sizeOver > maxOverlap) {
          maxOverlap = sizeOver;
          bestOverlap = col;
        }
      } else {
        int dist1 = Math.abs(col.min - checkSpan.max);
        int dist2 = Math.abs(checkSpan.min - col.max);
        int min = (dist1 < dist2) ? dist1 : dist2;
        if (min < minDisjointDist) {
          minDisjointDist = min;
          bestDisjoint = col;
        }
      }
    }
   
    if (bestOverlap != null) {
      return (bestOverlap);
    }
    return (bestDisjoint);
  }
 
  /***************************************************************************
  **
  ** Add a map from a gene to a List of target genes
  */
  
   void addDataMaps(String key, List<String> entries, List<String> sources) {
    if ((entries != null) && (entries.size() > 0)) {
      entryMap_.put(key, entries);
    }
    if ((sources != null) && (sources.size() > 0)) {
      sourceMap_.put(key, sources);
    }
    return;
  }

  /***************************************************************************
  **
  ** Add the maps from a gene sources and entries
  */
  
   void addCombinedDataMaps(String key, List<QpcrMapResult> datasets) {
    ArrayList<String> sourceList = new ArrayList<String>();
    ArrayList<String> entryList = new ArrayList<String>();
    Iterator<QpcrMapResult> dit = datasets.iterator();
    while (dit.hasNext()) {
      QpcrMapResult res = dit.next();
      if (res.type == QpcrMapResult.ENTRY_MAP) {
        entryList.add(res.name);
      } else {
        sourceList.add(res.name);
      }
    }
    entryMap_.put(key, entryList);
    sourceMap_.put(key, sourceList);    
    return;
  }  

  /***************************************************************************
  **
  ** Get the list of targets names for the gene ID.  May be empty.
  */
  
  private List<String> getQPCRDataEntryKeysWithDefault(String nodeId, TabPinnedDynamicDataAccessContext dacx) {  
    List<String> retval = entryMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      Node node = dacx.getGenomeSource().getRootDBGenome().getNode(nodeId);
      if (node == null) {
        throw new IllegalStateException();
      }
      String nodeName = node.getRootName();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);  
  
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the gene ID.  May be empty.
  */
  
  private List<String> getQPCRDataSourceKeysWithDefault(String nodeId, TabPinnedDynamicDataAccessContext dacx) {  
    List<String> retval = sourceMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList<String>();
      Node node = dacx.getGenomeSource().getRootDBGenome().getNode(nodeId);
      if (node == null) {
        throw new IllegalStateException();
      }
      String nodeName = node.getRootName();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        return (retval);
      }
      retval.add(nodeName);
    }    
    return (retval);  
  
  }
  
  /***************************************************************************
  **
  ** Get the target gene, with relaxed name matching criteria:
  */
  
  TargetGene getQPCRDataRelaxedMatch(String targetName) {
    Iterator<TargetGene> trgit = genes_.iterator();
    while (trgit.hasNext()) {
      TargetGene trg = trgit.next();
      String name = trg.getName();
      if (DataUtil.keysEqual(name, targetName)) {
        return (trg);
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Get the HTML table for the given gene.  May be null.
  */
  
   String getHTML(String geneId, String sourceID, QpcrTablePublisher qtp, TabPinnedDynamicDataAccessContext dacx) {
    List<String> keys = getQPCRDataEntryKeysWithDefault(geneId, dacx);
    if (keys == null) {
      return (null);
    }
    
    List<String> srcKeys = null;
    if (sourceID != null) {
      srcKeys = getQPCRDataSourceKeysWithDefault(sourceID, dacx);
      if (srcKeys == null) {
        return (null);
      }
    }
      
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    qtp.setOutput(out);
    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);     

    Iterator<TargetGene> trgit = genes_.iterator();
    ArrayList<TargetGene> targetGenes = new ArrayList<TargetGene>();
    while (trgit.hasNext()) {
      TargetGene trg = trgit.next();
      if (DataUtil.containsKey(keys, trg.getName())) {
        targetGenes.add(trg);
      }
    }
    // needs keys for ALL targets, even those that did not map to table data, to fill out
    // null perturbation lists.
    writeHTMLForGenes(out, ind, targetGenes, keys, srcKeys, true, qtp, dacx);
    return (sw.toString());
  }  
  
  /***************************************************************************
  **
  ** Write the QPCR to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, TabPinnedDynamicDataAccessContext dacx) {
    Iterator<TargetGene> trgit = genes_.iterator();
    TreeMap<String, TargetGene> sortedGeneMap = new TreeMap<String, TargetGene>();    
    while (trgit.hasNext()) {
      TargetGene trg = trgit.next();
      sortedGeneMap.put(trg.getName().toLowerCase(), trg);
    }
    ArrayList<String> geneNames = new ArrayList<String>();
    ArrayList<TargetGene> sortedGenes = new ArrayList<TargetGene>();
    Iterator<String> sgmit = sortedGeneMap.keySet().iterator();
    while (sgmit.hasNext()) {
      String name = sgmit.next();
      TargetGene trg = sortedGeneMap.get(name);
      geneNames.add(trg.getName());
      sortedGenes.add(trg);
    }
    writeHTMLForGenes(out, ind, sortedGenes, geneNames, null, false, qtp, dacx);
    return;
  }

  /***************************************************************************
  **
  ** Write the QPCR to HTML
  **
  */
  
   void writeHTMLForGenes(PrintWriter out, Indenter ind, 
                          List<TargetGene> targetGenes, List<String> names, List<String> srcNames,
                          boolean doTrim, QpcrTablePublisher qtp, TabPinnedDynamicDataAccessContext dacx) {
     
    qtp.colorsAndScaling();
    DisplayOptions dopt = dacx.getDisplayOptsSource().getDisplayOptions();
    boolean breakOutInvest = dopt.breakOutInvestigators();
    ind.indent();
    out.println("<center>");   
    out.println("<table width=\"100%\" border=\"1\" bordercolor=\"#000000\" cellpadding=\"7\" cellspacing=\"0\" >");
 
    // Print out table heading row
    outputSpanRow(out, ind, qtp, dacx);
   
    //
    // Crank out the genes
    //
    int rowCount = 0;
    Iterator<TargetGene> git = targetGenes.iterator();
    Set<String> footNumbers = new HashSet<String>();
    while (git.hasNext()) {
      TargetGene tg = git.next();
      rowCount = tg.writeHTML(out, ind, timeSpanCols_, qtp, rowCount, breakOutInvest, srcNames, footNumbers, dacx);
      if ((rowCount > HEADING_SPACING_) && git.hasNext()){
        outputSpanRow(out, ind, qtp, dacx);
        rowCount = 0;
      }
    }
    ind.down().indent(); 
    out.println("</table>");

    qtp.paragraph(false);
    ResourceManager rMan = dacx.getRMan();
    out.print(rMan.getString("qpcrData.qpcrTableNote"));    
    out.println("</p>");
    
    
    // Crank out null perturbations
    Iterator<NullPerturb> pers = getNullPerturbations();
    TreeMap<String, NullPerturb> sortedPert = new TreeMap<String, NullPerturb>();    
    while (pers.hasNext()) {
      NullPerturb per = pers.next();
      if (per.sourcesContainOneOrMore(srcNames)) { 
        String pertName = per.getSourceDisplayString(false);
        sortedPert.put(pertName, per);
      }
    }
    Iterator<String> perout = sortedPert.keySet().iterator();
    NullTimeSpan ndts = new NullTimeSpan(dacx, dopt.getNullPertDefaultSpan());
    boolean doHeading = true;    
    while (perout.hasNext()) {
      String pertName = perout.next();
      NullPerturb per = sortedPert.get(pertName);
      if (!doTrim || per.appliesToTargets(names)) {
        if (doHeading) {
          qtp.writePerturbationHeader(ind, ndts);
          ind.up();
          doHeading = false;
        }  
        per.writeHTML(out, ind, qtp, ndts, dacx);
        if (doTrim) {
          Set<String> foots = per.getFootnoteNumbers();
          footNumbers.addAll(foots);
        }
      }
    }
    if (!doHeading) {
      ind.down().indent(); 
      out.println("</table>");
    }
    out.println("</center>");
    // Crank out footnotes
    ind.indent();
    Iterator<Footnote> fit = getFootnotes();
    doHeading = true;
    while (fit.hasNext()) {
      Footnote fn = fit.next();
      String fnNum = fn.getNumber();
      if (!doTrim || footNumbers.contains(fnNum)) {
        if (doHeading) {
          ind.indent();
          out.println("<h3>Footnotes</h3>");
          doHeading = false;
        }
        ind.indent();       
        fn.writeHTML(out, ind, qtp);
      }
    }
    ind.down(); 
    return;
  }
   
 /***************************************************************************
  **
  ** Output column headers
  */
  
  private void outputSpanRow(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, TabPinnedDynamicDataAccessContext dacx) { 
    ind.up().indent();
    out.println("<tbody>");
    // Print out table heading row
    ind.up().indent();    
    out.println("<tr valign=\"top\">");
    ind.up();
    // Gene heading
    ind.indent();
    out.println("<td bgcolor=\"#EEEEEE\">");
    ind.up().indent();
    qtp.paragraph(false);
    out.println("<b>Gene</b></p>");
    ind.down().indent();
    out.println("</td>");
    // Perturbation
    ind.indent();
    out.println("<td bgcolor=\"#EEEEEE\">");
    ind.up().indent();
    qtp.paragraph(false);
    out.println("<b>Perturbation</b></p>");    
    ind.down().indent();
    out.println("</td>");   
    Iterator<MinMax> cit = timeSpanCols_.iterator();    
    while (cit.hasNext()) {
      MinMax tc = cit.next();
      ind.indent();
      out.println("<td bgcolor=\"#EEEEEE\">");
      ind.up().indent();
      qtp.paragraph(true);    
      out.print("<b>");
      String tdisp;
      if ((tc.min == Integer.MIN_VALUE) && (tc.max == Integer.MAX_VALUE)) {
        tdisp = dacx.getRMan().getString("qpcrDisplay.allTimes");
      } else {     
        tdisp = TimeSpan.spanToString(dacx, tc);
      }
      tdisp = tdisp.replaceAll(" ", "&nbsp;");     
      out.print(tdisp);
      out.println("</b></p>");
      ind.down().indent();
      out.println("</td>");
    }
     // Investigator
    ind.indent();  
    out.println("<td bgcolor=\"#EEEEEE\">");
    ind.up().indent();
    qtp.paragraph(false);  
    out.println("<b>Data of:</b></p>");
    ind.down().indent();
    out.println("</td>");      
    // Finish heading row
    ind.down().indent();
    out.println("</tr>");
    ind.down().indent();    
    out.println("</tbody>");   
    return;
  }
   
  /***************************************************************************
  **
  ** Map link sign to measurement sign
  */
  
   int mapLinkSignToMeasurementSign(int sign) {
    switch (sign) {
      case Linkage.NEGATIVE:
        return (Measurement.REPRESS_LINK); 
      case Linkage.POSITIVE:
        return (Measurement.PROMOTE_LINK);
      case Linkage.NONE:
        return (Measurement.UNSIGNED_LINK);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Sorts Sources.  Different notes are NOT taken into account!
  **
  */
  
   static class SourceComparator implements Comparator<Source> {
     public int compare(Source s1, Source s2) {
      String s1v = s1.getDisplayValue().toUpperCase().replaceAll(" ", "");
      String s2v = s2.getDisplayValue().toUpperCase().replaceAll(" ", "");
      return (s1v.compareTo(s2v));
    }
  }  

  /***************************************************************************
  **
  ** Used to return QPCR map results
  **
  */
  
   static class QpcrMapResult {
     String name;
     int type;
    
     static final int ENTRY_MAP  = 0;
     static final int SOURCE_MAP = 1;

     QpcrMapResult(String name, int type) {
      this.name = name;
      this.type = type;
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
   static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("QPCR"); 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the column keyword
  **
  */
  
   static String columnKeyword() {
    return ("column");
  }
  
  /***************************************************************************
  **
  ** Return the column keyword
  **
  */
  
   static String columnRangeKeyword() {
    return ("columnRange");
  }  
 
  /***************************************************************************
  **
  ** Return the datamap keyword
  **
  */
  
   static String datamapKeyword() {
    return ("datamap");
  }
  
  /***************************************************************************
  **
  ** Return the useqpcr keyword
  **
  */
  
   static String useqpcrKeyword() {
    return ("useqpcr");
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("QPCRData: " +
            " genes = " + genes_ +
            " timeSpanCols = " + timeSpanCols_ + 
            " entryMap = " + entryMap_ + 
            " sourceMap = " + sourceMap_ +             
            " null perturbs = " + nullPerturbations_ +
            " footnotes = " + footnotes_);
  }
  
  /***************************************************************************
  **
  ** Build from XML input. This is for legacy input.
  **
  */
  
   static QPCRData buildFromXML(TabPinnedDynamicDataAccessContext dacx, String elemName, 
                                Attributes attrs, 
                                boolean serialNumberIsIllegal) throws IOException {
    if (!elemName.equals("QPCR")) {
      return (null);
    }
    String threshString = AttributeExtractor.extractAttribute(elemName, attrs, "QPCR", "threshold", false); 
    String serialString = AttributeExtractor.extractAttribute(elemName, attrs, "QPCR", "serialNum", false);
    if (serialNumberIsIllegal && (serialString != null)) {
      throw new IOException();
    }
    
    QPCRData retval = new QPCRData(dacx);
    if (threshString != null) {
      try {
        double thresh = Double.parseDouble(threshString);
        if (thresh < 0.0) {
          throw new IOException();
        }
        retval.threshold_ = thresh;
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    }
    if (serialString != null) {
      try {
        long serialNumber = Long.parseLong(serialString);
        if (serialNumber < 0) {
          throw new IOException();
        }
        retval.serialNumber_ = serialNumber;
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static String extractMapKey(String elemName, 
                                     Attributes attrs) throws IOException {
    String chkStr = "datamap";
    if (!elemName.equals(chkStr)) {
      return (null);
    }
    
    String keyName = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("key")) {
          keyName = val;
        }
      }
    }
    
    if (keyName == null) {
      throw new IOException();
    }
    
    return (keyName);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static QpcrMapResult extractUseQPCR(String elemName, 
                                             Attributes attrs) throws IOException {
    String chkStr = "useqpcr";
    if (!elemName.equals(chkStr)) {
      return (null);
    }
    
    String name = null;
    String type = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = val;
        }
        if (key.equals("type")) {
          type = val;
        }
      }
    }
    
    if ((name == null) || (type == null)) {
      throw new IOException();
    }
    
    int mapType = (type.equals("entry")) ? QpcrMapResult.ENTRY_MAP : QpcrMapResult.SOURCE_MAP;
      
    return (new QpcrMapResult(name, mapType));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static MinMax extractColumnRange(String elemName, Attributes attrs) throws IOException {
    if (!elemName.equals("columnRange")) {
      return (null);
    }
    
    String minValStr = null;
    String maxValStr = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("min")) {
          minValStr = val;
        }
        if (key.equals("max")) {
          maxValStr = val;
        }
      }
    }
    
    try {
      int minVal = Integer.parseInt(minValStr);
      int maxVal = minVal;
      if (maxValStr != null) {
        maxVal = Integer.parseInt(maxValStr);
      }
      return (new MinMax(minVal, maxVal));
    } catch (NumberFormatException ex) {
      throw new IOException();
    } 
  }  

  /***************************************************************************
  **
  ** Write the datamap to XML
  **
  */
  
   static void writeDataMap(PrintWriter out, Indenter ind, 
                            Map<String, List<String>> entryMap, Map<String, List<String>> sourceMap, boolean forQpcr) {
    ind.indent();    
    out.println(forQpcr ? "<datamaps>" : "<tempWorkDatamaps>");
    TreeSet<String> sorted = new TreeSet<String>();
    sorted.addAll(entryMap.keySet());
    sorted.addAll(sourceMap.keySet());
    Iterator<String> mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = mapKeys.next();     
      List<String> elist = entryMap.get(key);
      List<String> slist = sourceMap.get(key);      
      ind.indent();
      out.print(forQpcr ? "<datamap" : "<tempWorkDatamap");
      out.print(" key=\"");      
      out.print(key);
      out.println("\">");
      ind.up();
      if (elist != null) {
        Iterator<String> lit = elist.iterator();
        while (lit.hasNext()) {
          String useqpcr = lit.next();      
          ind.indent();
          out.print(forQpcr ? "<useqpcr" : "<tempWorkUseqpcr");
          out.print(" type=\"entry\" name=\"");          
          out.print(useqpcr);
          out.println("\"/>");
        }
      }
      if (slist != null) {
        Iterator<String> lit = slist.iterator(); 
        while (lit.hasNext()) {
          String useqpcr = lit.next();      
          ind.indent();
          out.print(forQpcr ? "<useqpcr" : "<tempWorkUseqpcr");
          out.print(" type=\"source\" name=\"");          
          out.print(useqpcr);
          out.println("\"/>");
        }
      }
      ind.down().indent(); 
      out.println(forQpcr ? "</datamap>" : "</tempWorkDatamap>");
    }
    ind.down().indent(); 
    out.println(forQpcr ? "</datamaps>" : "</tempWorkDatamaps>");
    return;
  }
  

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
 
}
