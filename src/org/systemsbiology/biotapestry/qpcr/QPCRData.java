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
import org.systemsbiology.biotapestry.app.BTState;
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
  
  private ArrayList genes_;
  private ArrayList nullPerturbations_;
  private NullTimeSpan nullPertDefaultSpan_;  
  private ArrayList footnotes_;
  private ArrayList timeSpanCols_;
  private HashMap entryMap_;
  private HashMap sourceMap_;
  private double threshold_;
  private long serialNumber_;
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

  QPCRData(BTState appState) {
    appState_ = appState;
    genes_ = new ArrayList();
    nullPerturbations_ = new ArrayList();
    nullPertDefaultSpan_ = null;
    footnotes_ = new ArrayList();
    timeSpanCols_ = new ArrayList();
    entryMap_ = new HashMap();
    sourceMap_ = new HashMap();
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
    
    PerturbationData pd = appState_.getDB().getPertData();
    PertDictionary pDict = pd.getPertDictionary();
    MeasureDictionary mDict = pd.getMeasureDictionary();
    ConditionDictionary cDict = pd.getConditionDictionary();
    String legCondition = cDict.getStandardConditionKey(); 
    String legMeasure = mDict.createLegacyMeasureProps(threshold_);
    
    DisplayOptions dOpt = appState_.getDisplayOptMgr().getDisplayOptions(); 
    dOpt.setColumns(getColumns());
    
    long timeStamp = System.currentTimeMillis();
    ArrayList unkInvs = new ArrayList();
   
    HashMap annotsForSrcs = new HashMap();
    HashMap annotsToData = new HashMap();
    factorSourceAnnots(pd, pDict, annotsForSrcs, annotsToData);
      
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TargetGene tgene = (TargetGene)genes_.get(i);
      List gnotes = tgene.getTranslatedNotes();
      List gidList = footNumsToIDs(pd, gnotes);    
      String targName = tgene.getName();
      KeyAndDataChange kadc = pd.provideTarget(targName);
      String targKey = kadc.key;
      if (!gidList.isEmpty()) {
        pd.setFootnotesForTargetIO(targKey, gidList);
      }
      Iterator pit = tgene.getPerturbations();   
      while (pit.hasNext()) {
        Perturbation prt = (Perturbation)pit.next();   
        ArrayList srcList = new ArrayList();
        Iterator sit1 = prt.getSources(); 
        Set keypss = perturbationToKey(pd, pDict, sit1, new ArrayList());
        Set retainNotes = ((HashMap)annotsForSrcs.get(keypss)).keySet(); 
        HashMap notesToTargs = (HashMap)annotsToData.get(keypss);
        Iterator sit = prt.getSources(); 
        while (sit.hasNext()) {
          Source src = (Source)sit.next();
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
        Iterator tsit = prt.getTimeSpans();
        while (tsit.hasNext()) {
          TimeSpan tspan = (TimeSpan)tsit.next();
          PerturbationData.RegionRestrict rr = null;
          Iterator rrit = tspan.getRegionRestrictions();
          if (rrit.hasNext()) {
            ArrayList regionList = new ArrayList();
            while (rrit.hasNext()) {
              String regID = (String)rrit.next();
              regionList.add(regID);
            }
            rr = new PerturbationData.RegionRestrict(regionList);
          }         
          MinMax spanTimes = tspan.getMinMaxSpan();
          Iterator tbit = tspan.getBatches();
          int subBatchKey = 0;
          while (tbit.hasNext()) {
            Batch batch = (Batch)tbit.next();
            String invests = batch.getInvestigators();
            List investList = (invests != null) ? Perturbation.unformatInvestigators(invests) : pInvestList;
            ArrayList invKeyList = new ArrayList();
            if (investList.isEmpty()) {
              invKeyList = unkInvs;
            } else {
              int numI = investList.size();
              for (int j = 0; j < numI; j++) {
                String invest  = (String)investList.get(j);
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
            Iterator bit = batch.getMeasurements();
            while (bit.hasNext()) {
              Measurement mea = (Measurement)bit.next();
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
  
    NullTimeSpan dnts = getLegacyNullPerturbationsDefaultTimeSpan();
 
    int numNull = nullPerturbations_.size();
    for (int i = 0; i < numNull; i++) {
      NullPerturb np = (NullPerturb)nullPerturbations_.get(i);
      Iterator sit1 = np.getSources();
      Set keypss = perturbationToKey(pd, pDict, sit1, new ArrayList());
      HashMap notesToTargs = (HashMap)annotsToData.get(keypss);
    
      Set retainNotes = ((HashMap)annotsForSrcs.get(keypss)).keySet();   
      ArrayList srcList = new ArrayList();
      Iterator sit = np.getSources();
      while (sit.hasNext()) {
        Source src = (Source)sit.next();
        String srcID = sourceToPertSource(src, pd, pDict, retainNotes);
        srcList.add(srcID);
      }
      PertSources srcs = new PertSources(srcList);
      MinMax fallbackTime = new MinMax(dnts.getMin(), dnts.getMax()); 
      Iterator nit = np.getTargets();
      while (nit.hasNext()) {
        NullTarget nt = (NullTarget)nit.next();
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
            Iterator tmit = nt.getTimes();
            while (tmit.hasNext()) {
              NullTimeSpan nts = (NullTimeSpan)tmit.next();
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
            Iterator spit = nt.getSupportData();
            HashSet dataNums = new HashSet();
            while (spit.hasNext()) {
              Batch ntb = (Batch)spit.next();
              int batchTime = ntb.getTimeNumber();
              dataNums.add(new Integer(batchTime));
            }
            Iterator tmit = nt.getTimes();
            while (tmit.hasNext()) {
              NullTimeSpan nts = (NullTimeSpan)tmit.next();
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
          Iterator spit = nt.getSupportData();
          int subBatchKey = 0;
          while (spit.hasNext()) {
            Batch ntb = (Batch)spit.next();
            String invests = ntb.getInvestigators();
            List investList = Perturbation.unformatInvestigators(invests);
            ArrayList invKeyList = new ArrayList();
            if (investList.isEmpty()) {
              invKeyList = unkInvs;
            } else {
              int numI = investList.size();
              for (int j = 0; j < numI; j++) {
                String invest  = (String)investList.get(j);
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
            Iterator bit = ntb.getMeasurements();
            while (bit.hasNext()) {
              Measurement mea = (Measurement)bit.next();
              exportMeasurement(mea, pd, srcs, psiKey, timeStamp, targKey, 
                                batchKey, date, legMeasure, invKeyList, rr, 
                                notesToTargs, DUMMY_NULL_TARG_KEY_, legCondition);
            }
          }
        }
      }     
    }
    
    Iterator emit = entryMap_.keySet().iterator();
    while (emit.hasNext()) {
      String eKey = (String)emit.next();
      ArrayList forKey = (ArrayList)entryMap_.get(eKey);
      pd.importLegacyEntryMapEntry(eKey, forKey);
    }
    Iterator smit = sourceMap_.keySet().iterator();
    while (smit.hasNext()) {
      String sKey = (String)smit.next();
      ArrayList forKey = (ArrayList)sourceMap_.get(sKey);
      pd.importLegacySourceMapEntry(sKey,forKey);
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
                                           List unkInvs, long timeStamp, String targKey, String legMeasure, 
                                           PerturbationData.RegionRestrict regRestrict, 
                                           HashMap<String, Set<String>> notesToTargs, String legCondition) {
   
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
  
  private Set perturbationToKey(PerturbationData pd, PertDictionary pDict, 
                                Iterator sit, List allFoots) {
    ArrayList emptyList = new ArrayList();
    HashSet allSrcs = new HashSet();
    while (sit.hasNext()) {
      Source src = (Source)sit.next(); 
      String base = src.getBaseType();
      String expr = src.getExpType();
      String pertKey = pDict.getPerturbPropsFromName(expr);
      if (pertKey == null) {
        throw new IllegalStateException();
      }
      List sfoots = src.getFootnoteNumbers();
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
                                  HashMap annotsForSrcs, HashMap annotsToData) {   
  
    int numGenes = genes_.size();
    for (int i = 0; i < numGenes; i++) {
      TargetGene tgene = (TargetGene)genes_.get(i);
      String normName = DataUtil.normKey(tgene.getName());
      Iterator pit = tgene.getPerturbations();   
      while (pit.hasNext()) {
        Perturbation prt = (Perturbation)pit.next();
        ArrayList allFoots = new ArrayList();
        Set pssk = perturbationToKey(pd, pDict, prt.getSources(), allFoots);
        factorSourceAnnotsGuts(normName, pssk, allFoots, annotsForSrcs, annotsToData);
      }
    }
      
    int numNull = nullPerturbations_.size();
    for (int i = 0; i < numNull; i++) {
      NullPerturb np = (NullPerturb)nullPerturbations_.get(i);
      ArrayList allFoots = new ArrayList();
      Set pssk = perturbationToKey(pd, pDict, np.getSources(), allFoots);
      factorSourceAnnotsGuts(DUMMY_NULL_TARG_KEY_, pssk, allFoots, annotsForSrcs, annotsToData);
    }
    return;
  }

  /***************************************************************************
  **
  ** Guts of above routine
  **
  */
  
  private void factorSourceAnnotsGuts(String targetName, Set pssk, List allFoots,
                                      HashMap annotsForSrcs, HashMap annotsToData) {   
   
    HashSet idSet = new HashSet(allFoots);
    HashMap notes = (HashMap)annotsForSrcs.get(pssk);
    HashMap goesToData = (HashMap)annotsToData.get(pssk);
    // First time seen:
    if (notes == null) {
      notes = new HashMap();
      annotsForSrcs.put(pssk, notes);
      Iterator idsit = idSet.iterator();
      while (idsit.hasNext()) {
        String noteID = (String)idsit.next();
        HashSet forNote = new HashSet();
        notes.put(noteID, forNote);
        forNote.add(targetName);
      }  
    } else { // only common source notes stay, else they get moved to data entries:
      //
      // If already gone to data, they get transferred ASAP:
      //
      if (goesToData != null) {
        HashSet intersectToData = new HashSet(goesToData.keySet());
        intersectToData.retainAll(idSet);
        Iterator i2dit = intersectToData.iterator();
        while (i2dit.hasNext()) {
          String noteID = (String)i2dit.next();
          HashSet toDatForTargs = (HashSet)goesToData.get(noteID);
          toDatForTargs.add(targetName);
          idSet.remove(noteID);
        }
      }
      //
      // Guys left get to try to stay with the source:
      //
      HashSet allKeys = new HashSet(notes.keySet());
      HashSet intersect = (HashSet)allKeys.clone();
      intersect.retainAll(idSet);
      if (goesToData == null) {
        goesToData = new HashMap();
        annotsToData.put(pssk, goesToData);
      } 
      Iterator akit = allKeys.iterator();
      while (akit.hasNext()) {
        String noteKey = (String)akit.next();
        if (!intersect.contains(noteKey)) {
           HashSet forTrgs = (HashSet)notes.remove(noteKey);
          Iterator ftit = forTrgs.iterator();
          while (ftit.hasNext()) {
            String targ = (String)ftit.next();
            HashSet toDatForTargs = (HashSet)goesToData.get(noteKey);
            if (toDatForTargs == null) {
              toDatForTargs = new HashSet();
              goesToData.put(noteKey, toDatForTargs);
            }
            toDatForTargs.add(targ);
          }
        } else {
          HashSet forTrgs = (HashSet)notes.get(noteKey);
          forTrgs.add(targetName);
        }
      }
      //
      // Anybody left needs to go to the data-based notes:
      //
      idSet.removeAll(intersect);
      Iterator idit = idSet.iterator();
      while (idit.hasNext()) {
        String noteID = (String)idit.next();
        HashSet toDatForTargs = (HashSet)goesToData.get(noteID);
        if (toDatForTargs == null) {
          toDatForTargs = new HashSet();
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
  
  private String sourceToPertSource(Source src, PerturbationData pd, PertDictionary pDict, Set retainNotes) { 
    String base = src.getBaseType();
    PerturbationData.KeyAndDataChange kdac = pd.providePertSrcName(base);
    String expr = src.getExpType();
    List sfoots = src.getFootnoteNumbers();
    String pSign = src.getProxySign();
    pSign = PertSource.mapLegacyProxySign(pSign);
    String proxNameID = null;
    if (!pSign.equals(PertSource.NO_PROXY)) {
      String proxyFor = src.getProxiedSpecies();
      PerturbationData.KeyAndDataChange kdac2 = pd.providePertSrcName(proxyFor);
      proxNameID = kdac2.key;
    }
    
    List idList = footNumsToIDs(pd, sfoots);
   
    String pertKey = pDict.getPerturbPropsFromName(expr);
    if (pertKey == null) {
      throw new IllegalStateException();
    }
    ArrayList useList = new ArrayList();
    int numL = idList.size();
    for (int i = 0; i < numL; i++) {
      String annotID = (String)idList.get(i);
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
  
  private List footNumsToIDs(PerturbationData pd, List noteList) { 
    Map footMsg = translateFootNumbers(noteList);
    Iterator fmit = footMsg.keySet().iterator();
    ArrayList idList = new ArrayList();
    while (fmit.hasNext()) {
      String key = (String)fmit.next();
      String msg = (String)footMsg.get(key);
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
                                 String date, String legMeasure, List invKeyList, 
                                 PerturbationData.RegionRestrict regRestrict, 
                                 HashMap notesToTargs, String useKey, String legCondition) {
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
    List noteList = mea.getFootnoteNumbers();
    List idList = footNumsToIDs(pd, noteList);
      
    //
    // Add in annots that really belong with the data, not with the source:
    //
     
    if (notesToTargs != null) {
      Iterator nttkit = notesToTargs.keySet().iterator();
      while (nttkit.hasNext()) {
        String noteID = (String)nttkit.next();
        HashSet forTargs = (HashSet)notesToTargs.get(noteID);
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
  
  private Map translateFootNumbers(List footList) {
    HashMap retval = new HashMap();
    int numFL = footList.size();
    for (int i = 0; i < numFL; i++) {
      String flNum = (String)footList.get(i);
      int numf = footnotes_.size();
      for (int j = 0; j < numf; j++) {
        Footnote fn = (Footnote)footnotes_.get(j);
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
  
  void addGene(TargetGene gene, boolean bumpSerial) {
    genes_.add(gene);
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the genes
  */
  
   Iterator getGenes() {
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
  
   Iterator getNullPerturbations() {
    return (nullPerturbations_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the given null perturbation
  */
  
  NullPerturb getNullPerturbation(int index) {
    return ((NullPerturb)nullPerturbations_.get(index));
  }   

  /***************************************************************************
  **
  ** Get the default time span for null perturbations
  */
  
  NullTimeSpan getLegacyNullPerturbationsDefaultTimeSpan() {
    //
    // Kinda weird; don;t actually set it unless user does so explicitly. Don't
    // want it written out or considered committed unless we actually set it.
    if (nullPertDefaultSpan_ == null) {
      MinMax mm = appState_.getDB().getTimeAxisDefinition().getDefaultTimeSpan();
      return (new NullTimeSpan(appState_, mm.min, mm.max));
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
  
   Iterator getFootnotes() {
    return (footnotes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Set the column headings
  */
  
   void setColumns(ArrayList columns) {
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
    Iterator git = getGenes();
    if (git.hasNext()) {
      TargetGene tg = (TargetGene)git.next();
      Iterator pit = tg.getPerturbations();
      if (pit.hasNext()) {
        Perturbation pert = (Perturbation)pit.next();
        Iterator sit = pert.getTimeSpans();
        return (sit.hasNext());
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Get the column headings (note we are handing out the actual list DANGER!)
  */
  
   ArrayList getColumns() {
    return (timeSpanCols_);
  }
  
  /***************************************************************************
  **
  ** Get the column headings iterator
  */
  
   Iterator getColumnIterator() {
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
    return (getMaxOrMin((String)colVal, true));
  }  
  
  /***************************************************************************
  **
  ** Extract the maximum from the column contents Legacy only
  */
  
   static int getMaximum(String colVal) {
    return (getMaxOrMin((String)colVal, false));
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
        MinMax col = (MinMax)timeSpanCols_.get(i);
        if ((time == col.min) && (timeMax == col.max)) {
          return (col);  
        }
      }
    } else {
      for (int i = 0; i < cols; i++) {
        MinMax col = (MinMax)timeSpanCols_.get(i);
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
      MinMax col = (MinMax)timeSpanCols_.get(i);
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
    SortedSet checkSet = checkSpan.getAsSortedSet();
    int minDisjointDist = Integer.MAX_VALUE;
    MinMax bestDisjoint = null;
    int maxOverlap = Integer.MIN_VALUE;
    MinMax bestOverlap = null;
    
    int cols = timeSpanCols_.size();
    for (int i = 0; i < cols; i++) {
      MinMax col = (MinMax)timeSpanCols_.get(i);
      MinMax.SetRel eval = col.evaluate(checkSpan);
      if ((eval == MinMax.SetRel.IS_PROPER_SUBSET) || (eval == MinMax.SetRel.EQUALS)) {
        return (col);
      } else if ((eval == MinMax.SetRel.INTERSECTS) || (eval == MinMax.SetRel.IS_PROPER_SUPERSET)) {
        SortedSet colSet = col.getAsSortedSet();
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
  
   void addDataMaps(String key, List entries, List sources) {
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
  
   void addCombinedDataMaps(String key, List datasets) {
    ArrayList sourceList = new ArrayList();
    ArrayList entryList = new ArrayList();
    Iterator dit = datasets.iterator();
    while (dit.hasNext()) {
      QpcrMapResult res = (QpcrMapResult)dit.next();
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
  
  private List getQPCRDataEntryKeysWithDefault(String nodeId) {  
    List retval = (List)entryMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList();
      Node node = appState_.getDB().getGenome().getNode(nodeId);
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
  
  private List getQPCRDataSourceKeysWithDefault(String nodeId) {  
    List retval = (List)sourceMap_.get(nodeId);
    if ((retval == null) || (retval.size() == 0)) {
      retval = new ArrayList();
      Node node = appState_.getDB().getGenome().getNode(nodeId);
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
    Iterator trgit = genes_.iterator();
    while (trgit.hasNext()) {
      TargetGene trg = (TargetGene)trgit.next();
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
  
   String getHTML(String geneId, String sourceID, QpcrTablePublisher qtp) {
    List keys = getQPCRDataEntryKeysWithDefault(geneId);
    if (keys == null) {
      return (null);
    }
    
    List srcKeys = null;
    if (sourceID != null) {
      srcKeys = getQPCRDataSourceKeysWithDefault(sourceID);
      if (srcKeys == null) {
        return (null);
      }
    }
      
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    qtp.setOutput(out);
    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);     

    Iterator trgit = genes_.iterator();
    ArrayList targetGenes = new ArrayList();
    while (trgit.hasNext()) {
      TargetGene trg = (TargetGene)trgit.next();
      if (DataUtil.containsKey(keys, trg.getName())) {
        targetGenes.add(trg);
      }
    }
    // needs keys for ALL targets, even those that did not map to table data, to fill out
    // null perturbation lists.
    writeHTMLForGenes(out, ind, targetGenes, keys, srcKeys, true, qtp);
    return (sw.toString());
  }  
  
  /***************************************************************************
  **
  ** Write the QPCR to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp) {
    Iterator trgit = genes_.iterator();
    TreeMap sortedGeneMap = new TreeMap();    
    while (trgit.hasNext()) {
      TargetGene trg = (TargetGene)trgit.next();
      sortedGeneMap.put(trg.getName().toLowerCase(), trg);
    }
    ArrayList geneNames = new ArrayList();
    ArrayList sortedGenes = new ArrayList();
    Iterator sgmit = sortedGeneMap.keySet().iterator();
    while (sgmit.hasNext()) {
      String name = (String)sgmit.next();
      TargetGene trg = (TargetGene)sortedGeneMap.get(name);
      geneNames.add(trg.getName());
      sortedGenes.add(trg);
    }
    writeHTMLForGenes(out, ind, sortedGenes, geneNames, null, false, qtp);
    return;
  }

  /***************************************************************************
  **
  ** Write the QPCR to HTML
  **
  */
  
   void writeHTMLForGenes(PrintWriter out, Indenter ind, 
                          List targetGenes, List names, List srcNames,
                          boolean doTrim, QpcrTablePublisher qtp) {
     
    qtp.colorsAndScaling();
    DisplayOptions dopt = appState_.getDisplayOptMgr().getDisplayOptions();
    boolean breakOutInvest = dopt.breakOutInvestigators();
    ind.indent();
    out.println("<center>");   
    out.println("<table width=\"100%\" border=\"1\" bordercolor=\"#000000\" cellpadding=\"7\" cellspacing=\"0\" >");
 
    // Print out table heading row
    outputSpanRow(out, ind, qtp);
   
    //
    // Crank out the genes
    //
    int rowCount = 0;
    Iterator git = targetGenes.iterator();
    Set footNumbers = new HashSet();
    while (git.hasNext()) {
      TargetGene tg = (TargetGene)git.next();
      rowCount = tg.writeHTML(out, ind, timeSpanCols_, qtp, rowCount, breakOutInvest, srcNames, footNumbers, appState_);
      if ((rowCount > HEADING_SPACING_) && git.hasNext()){
        outputSpanRow(out, ind, qtp);
        rowCount = 0;
      }
    }
    ind.down().indent(); 
    out.println("</table>");

    qtp.paragraph(false);
    ResourceManager rMan = appState_.getRMan();
    out.print(rMan.getString("qpcrData.qpcrTableNote"));    
    out.println("</p>");
    
    
    // Crank out null perturbations
    Iterator pers = getNullPerturbations();
    TreeMap sortedPert = new TreeMap();    
    while (pers.hasNext()) {
      NullPerturb per = (NullPerturb)pers.next();
      if (per.sourcesContainOneOrMore(srcNames)) { 
        String pertName = per.getSourceDisplayString(false);
        sortedPert.put(pertName, per);
      }
    }
    Iterator perout = sortedPert.keySet().iterator();
    NullTimeSpan ndts = new NullTimeSpan(appState_, dopt.getNullPertDefaultSpan());
    boolean doHeading = true;    
    while (perout.hasNext()) {
      String pertName = (String)perout.next();
      NullPerturb per = (NullPerturb)sortedPert.get(pertName);
      if (!doTrim || per.appliesToTargets(names)) {
        if (doHeading) {
          qtp.writePerturbationHeader(ind, ndts);
          ind.up();
          doHeading = false;
        }  
        per.writeHTML(out, ind, qtp, ndts);
        if (doTrim) {
          Set foots = per.getFootnoteNumbers();
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
    Iterator fit = getFootnotes();
    doHeading = true;
    while (fit.hasNext()) {
      Footnote fn = (Footnote)fit.next();
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
  
  private void outputSpanRow(PrintWriter out, Indenter ind, QpcrTablePublisher qtp) { 
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
    Iterator cit = timeSpanCols_.iterator();    
    while (cit.hasNext()) {
      MinMax tc = (MinMax)cit.next();
      ind.indent();
      out.println("<td bgcolor=\"#EEEEEE\">");
      ind.up().indent();
      qtp.paragraph(true);    
      out.print("<b>");
      String tdisp;
      if ((tc.min == Integer.MIN_VALUE) && (tc.max == Integer.MAX_VALUE)) {
        tdisp = appState_.getRMan().getString("qpcrDisplay.allTimes");
      } else {     
        tdisp = TimeSpan.spanToString(appState_, tc);
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
  
   static class SourceComparator implements Comparator {
     public int compare(Object o1, Object o2) {
      Source s1 = (Source)o1;
      Source s2 = (Source)o2;
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
    HashSet retval = new HashSet();
    retval.add("QPCR");
  //  retval.add("targetGenes");
  //  retval.add("nullPerturbations");
 //   retval.add("footnotes");
  //  retval.add("columns"); 
  //  retval.add("columnRanges");     
  //  retval.add("datamaps");     
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
  ** Handle the attributes for the keyword
  **
  */
  
   static QPCRData buildFromXML(BTState appState, String elemName, 
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
    
    QPCRData retval = new QPCRData(appState);
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
                                  Map entryMap, Map sourceMap, boolean forQpcr) {
    ind.indent();    
    out.println(forQpcr ? "<datamaps>" : "<tempWorkDatamaps>");
    TreeSet sorted = new TreeSet();
    sorted.addAll(entryMap.keySet());
    sorted.addAll(sourceMap.keySet());
    Iterator mapKeys = sorted.iterator();
    ind.up();    
    while (mapKeys.hasNext()) {
      String key = (String)mapKeys.next();     
      List elist = (List)entryMap.get(key);
      List slist = (List)sourceMap.get(key);      
      ind.indent();
      out.print(forQpcr ? "<datamap" : "<tempWorkDatamap");
      out.print(" key=\"");      
      out.print(key);
      out.println("\">");
      ind.up();
      if (elist != null) {
        Iterator lit = elist.iterator();
        while (lit.hasNext()) {
          String useqpcr = (String)lit.next();      
          ind.indent();
          out.print(forQpcr ? "<useqpcr" : "<tempWorkUseqpcr");
          out.print(" type=\"entry\" name=\"");          
          out.print(useqpcr);
          out.println("\"/>");
        }
      }
      if (slist != null) {
        Iterator lit = slist.iterator(); 
        while (lit.hasNext()) {
          String useqpcr = (String)lit.next();      
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
