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

package org.systemsbiology.biotapestry.perturb;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Analyzes dependencies
*/

public class DependencyAnalyzer {

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private PerturbationData pd_;
  private DataAccessContext dacx_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DependencyAnalyzer(DataAccessContext dacx, PerturbationData pd) {
    dacx_ = dacx;
    pd_ = pd;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Get the dependencies referencing the annotation
  */
 
  public Dependencies getAnnotReferenceSet(String annotID) {
 
    HashSet<String> pSUses = new HashSet<String>();  
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List<String> aids = chk.getAnnotationIDs();
      if (aids.contains(annotID)) {
        pSUses.add(psdKey);
      }
    }
    
    HashSet<String> pSIUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> ppit = pss.getSources();
      while (ppit.hasNext()) {
        String srcID = ppit.next();
        if (pSUses.contains(srcID)) {
          pSIUses.add(psi.getID());
        }
      }
    }

    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      String id = pdp.getID();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(id);
      } else {
        List<String> dpn = pd_.getDataPointNotes(id);
        if ((dpn != null) && dpn.contains(annotID)) {
          dpUses.add(id);
        }
      }
    }
    
    HashSet<String> targUses = new HashSet<String>();
    
    Iterator<String> tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = tkit.next();
      List<String> tn = pd_.getFootnotesForTarget(targKey);
      if ((tn != null) && tn.contains(annotID)) {
        targUses.add(targKey);
      }
    }
  
    return (new Dependencies(Dependencies.DepType.PRUNE_ANNOT, annotID, null, dpUses, pSIUses, pSUses, null, targUses, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the annotation merge set
  */
 
  public Dependencies getAnnotMergeSet(Set<String> joinKeys, String commonKey) {
    
    HashSet<String> pSUses = new HashSet<String>();     
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List<String> aids = chk.getAnnotationIDs();
      int numa = aids.size();
      for (int i = 0; i < numa; i++) {
        String annotKey = aids.get(i);
        if (joinKeys.contains(annotKey)) {
          pSUses.add(psdKey);
          break;
        }
      }
    }
 
    HashSet<String> dpUses = new HashSet<String>();  
    Iterator<String> dpit = pd_.getDataPointNoteKeys();
    while (dpit.hasNext()) {
      String id = dpit.next();
      List<String> aids = pd_.getDataPointNotes(id);
      int numa = aids.size();
      for (int i = 0; i < numa; i++) {
        String annotKey = aids.get(i);
        if (joinKeys.contains(annotKey)) {
          dpUses.add(id);
          break;
        }
      }
    }
    
    HashSet<String> targUses = new HashSet<String>();   
    Iterator<String> tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = tkit.next();
      List<String> aids = pd_.getFootnotesForTarget(targKey);
      if (aids != null) {
        int numa = aids.size();
        for (int i = 0; i < numa; i++) {
          String annotKey = aids.get(i);
          if (joinKeys.contains(annotKey)) {
            aids.add(targKey);
            break;
          }
        }
      }
    }
    
    return (new Dependencies(Dependencies.DepType.MERGE_ANNOT, commonKey, new HashSet<String>(joinKeys), dpUses, null, pSUses, null, targUses, null, null));
  }
  

  /***************************************************************************
  **
  ** Get the count of dependencies referencing all annotations
  */
 
  public Map<String, Integer> getAllAnnotReferenceCounts() {
   
    HashMap<String, Integer> retval = new HashMap<String, Integer>(); 
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List<String> aids = chk.getAnnotationIDs();
      int numAids = aids.size();
      for (int i = 0; i < numAids; i++) {
        String aid = aids.get(i);
        DataUtil.bumpCountMap(retval, aid);
      }
    }   
    
    Iterator<String> dpknit = pd_.getDataPointNoteKeys();   
    while (dpknit.hasNext()) {
      String dpnkey = dpknit.next();
      List<String> dpn = pd_.getDataPointNotes(dpnkey);
      int numDpn = dpn.size();
      for (int i = 0; i < numDpn; i++) {
        String aid = dpn.get(i);
        DataUtil.bumpCountMap(retval, aid);         
      }
    }
    
    Iterator<String> tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = tkit.next();
      List<String> tn = pd_.getFootnotesForTarget(targKey);
      if (tn != null) {
        int numTn = tn.size();
        for (int i = 0; i < numTn; i++) {
          String aid = tn.get(i);
          DataUtil.bumpCountMap(retval, aid);
        }
      }
    }
    
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the keys of data points referencing the experiment
  */
 
  public Dependencies getExperimentReferenceSet(String psiID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pdp.getExperimentKey().equals(psiID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the perturbation source
  */
 
  public Dependencies getInvestReferenceSet(String invID) {
        
    HashSet<String> pSIUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      List<String> invests = psi.getInvestigators();
      if (invests.contains(invID)) {
        pSIUses.add(psi.getID());
      }
    }
      
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.PRUNE_INVEST, invID, null, dpUses, pSIUses, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the counts of references of the perturbation source definition
  */
 
  public Map<String, Integer> getAllSrcDefReferenceCounts() {
        
    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      PertSources ps = exp.getSources();
      Iterator<String> psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = psit.next();
        DataUtil.bumpCountMap(retval, psdKey);
      }
    }
    
    //
    // Count the perturbed time course references too!
    //
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
    Iterator<String> tcit = psd.keySet().iterator();
    while (tcit.hasNext()) {
      String psdKey = tcit.next();
      Set<String> forKey = psd.get(psdKey);
      int fks = forKey.size();
      for (int i = 0; i < fks; i++) {
        DataUtil.bumpCountMap(retval, psdKey);    
      }
    }

    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap<String, Integer> retvalClone = new HashMap<String, Integer>(retval);
    Iterator<String> ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = ekit.next();
      Experiment exp = pd_.getExperiment(eKey);
      PertSources ps = exp.getSources();
      Iterator<String> psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = psit.next();
        if (retvalClone.get(psdKey) == null) {
          DataUtil.bumpCountMap(retval, psdKey);
        }
      }
    }
    
    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Get the counts of references of the investigator
  */
 
  public Map<String, Integer> getAllInvestigatorReferenceCounts(boolean primaryOnly) {
        
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    
    if (!primaryOnly) {
      Iterator<PertDataPoint> dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = dpit.next();
        Experiment exp = pdp.getExperiment(pd_);
        List<String> invs = exp.getInvestigators();
        int numi = invs.size();
        for (int i = 0; i < numi; i++) {
          String invKey = invs.get(i);
          DataUtil.bumpCountMap(retval, invKey);
        }
      }
    }
  
    //
    // Only count the raw usage if it wasn't used in a data point,
    // or if primary only!
    //
    
    HashMap<String, Integer> retvalClone = (primaryOnly) ? null : new HashMap<String, Integer>(retval);
    Iterator<String> ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = ekit.next();
      Experiment psi = pd_.getExperiment(eKey);
      List<String> invs = psi.getInvestigators();
      int numi = invs.size();
      for (int i = 0; i < numi; i++) {
        String invKey = invs.get(i);
        if (primaryOnly || (retvalClone.get(invKey) == null)) {
          DataUtil.bumpCountMap(retval, invKey);
        }
      }
    }
     
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the keys of experiments referencing the investigator for a merge
  */
 
  public Dependencies getInvestigatorMergeSet(Set<String> investIDs, String commonKey) {
    HashSet<String> expUses = new HashSet<String>();
    
    Iterator<String> ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = ekit.next();
      Experiment psi = pd_.getExperiment(eKey);
      List<String> invs = psi.getInvestigators();
      int numi = invs.size();
      for (int i = 0; i < numi; i++) {
        String invKey = invs.get(i);
        if (investIDs.contains(invKey)) {
          expUses.add(eKey);     
        }
      }
    }
    
    return (new Dependencies(Dependencies.DepType.MERGE_INVEST, commonKey, new HashSet<String>(investIDs), null, expUses, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the experiment
  */
 
  public Map<String, Integer> getAllExperimentReferenceCounts() {
        
    HashMap<String, Integer> retval = new HashMap<String, Integer>();      
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getExperimentKey());
    } 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for experiments
  */
 
  public Dependencies getExperimentMergeSet(Set<String> expIDs, String commonKey) {
    
    HashSet<String> expUsed = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      String expid = pdp.getExperimentKey();
      if (expIDs.contains(expid)) {
        expUsed.add(pdp.getID());
      }
    }
    
    return (new Dependencies(Dependencies.DepType.MERGE_EXPERIMENTS, commonKey, new HashSet<String>(expIDs), expUsed, null, null, null, null, null, null));
  }

  /***************************************************************************
  **
  ** Get the counts of references to experimental conditions
  */
 
  public Map<String, Integer> getAllExprConditionReferenceCounts() {
    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      DataUtil.bumpCountMap(retval, exp.getConditionKey());
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap<String, Integer> retvalClone = new HashMap<String, Integer>(retval);
    Iterator<String> ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = ekit.next();
      Experiment exp = pd_.getExperiment(eKey);
      String condKey = exp.getConditionKey();
      if (retvalClone.get(condKey) == null) {
        DataUtil.bumpCountMap(retval, condKey);
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the counts of references to experimental controls
  */
 
  public Map<String, Integer> getAllExprControlReferenceCounts() {    
    HashMap<String, Integer> retval = new HashMap<String, Integer>();      
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getControl());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the dependencies referencing the experimental control
  */
 
  public Dependencies getExprControlReferenceSet(String ctrl) {
  
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      String id = pdp.getID();
      if (ctrl.equals(pdp.getControl())) {
        dpUses.add(id);
      }
    }  
    return (new Dependencies(Dependencies.DepType.PRUNE_CONTROL, ctrl, null, dpUses, null, null, null, null, null, null));
  }
    
  /***************************************************************************
  **
  ** Get the set of references to experimental conditions
  */
 
  public Dependencies getExprConditionReferenceSet(String key) {
    
    HashSet<String> pSIUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      String condKey = psi.getConditionKey();
      if (condKey.equals(key)) {
        pSIUses.add(psi.getID());
      }
    }
      
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, dpUses, pSIUses, null, null, null, null, null));  
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for experimental conditions
  */
 
  public Dependencies getExprConditionMergeSet(Set<String> ecIDs, String commonKey) {
    
    HashSet<String> expUsed = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      if (ecIDs.contains(psi.getConditionKey())) {
        expUsed.add(psiKey);
      }
    }
    
    return (new Dependencies(Dependencies.DepType.MERGE_EXPR_COND, commonKey, new HashSet<String>(ecIDs), null, expUsed, null, null, null, null, null));
  }
  
 
  /***************************************************************************
  **
  ** Get the keys of data points referencing the measurement type
  */
 
  public Dependencies getMeasureReferenceSet(String meaID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pdp.getMeasurementTypeKey().equals(meaID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of objects referencing the measurement scale
  */
 
  public Dependencies getMeasScaleReferenceSet(String scaleID) {
    
    HashSet<String> mUsed = new HashSet<String>();
    MeasureDictionary md = pd_.getMeasureDictionary();
    Iterator<String> mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mkey = mkit.next();
      MeasureProps mp = md.getMeasureProps(mkey);
      if (mp.getScaleKey().equals(scaleID)) {
        mUsed.add(mkey);
      }
    } 

    HashSet<String> dpUsed = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (mUsed.contains(pdp.getMeasurementTypeKey())) {
        dpUsed.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, dpUsed, null, null, mUsed, null, null, null));
  }

  /***************************************************************************
  **
  ** Get the counts of references for all perturbation types
  */
 
  public Map<String, Integer> getAllPertPropReferenceCounts() {
  
    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      PertSources ps = exp.getSources();
      Iterator<String> psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = psit.next();
        PertSource chk = pd_.getSourceDef(psdKey); 
        DataUtil.bumpCountMap(retval, chk.getExpTypeKey());
      }
    }
    
    //
    // Count the perturbed time course references too!
    //
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
    Iterator<String> tcit = psd.keySet().iterator();
    while (tcit.hasNext()) {
      String psdKey = tcit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      String typeKey = chk.getExpTypeKey();
      Set<String> forKey = psd.get(psdKey);
      int fks = forKey.size();
      for (int i = 0; i < fks; i++) {
        DataUtil.bumpCountMap(retval, typeKey);    
      }
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap<String, Integer> retvalClone = new HashMap<String, Integer>(retval);
    Iterator<String> ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = ekit.next();
      Experiment exp = pd_.getExperiment(eKey);
      PertSources ps = exp.getSources();
      Iterator<String> psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = psit.next();
        PertSource chk = pd_.getSourceDef(psdKey);
        String typeKey = chk.getExpTypeKey();
        if (retvalClone.get(typeKey) == null) {
          DataUtil.bumpCountMap(retval, typeKey);
        }
      }
    }
    
    //
    // Even lower:
    //
    
    retvalClone =  new HashMap<String, Integer>(retval);
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      String typeKey = chk.getExpTypeKey();
      if (retvalClone.get(typeKey) == null) {
        DataUtil.bumpCountMap(retval, typeKey);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the counts of references for all measurement scale types
  */
 
  public Map<String, Integer> getAllMeasureScaleReferenceCounts(boolean primaryOnly) {
    
    MeasureDictionary md = pd_.getMeasureDictionary();
    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    
    if (!primaryOnly) {
      Iterator<PertDataPoint> dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = dpit.next();
        String mType = pdp.getMeasurementTypeKey();
        MeasureProps mp = md.getMeasureProps(mType);
        DataUtil.bumpCountMap(retval, mp.getScaleKey());
      }
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point, or
    // if targeting primary counts:
    //
   
    HashMap<String, Integer> retvalClone = (primaryOnly) ? null : new HashMap<String, Integer>(retval);
    Iterator<String> mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mKey = mkit.next();
      MeasureProps mp = md.getMeasureProps(mKey);
      String scaleKey = mp.getScaleKey();
      if (primaryOnly || (retvalClone.get(scaleKey) == null)) {
        DataUtil.bumpCountMap(retval, scaleKey);
      }
    }
   
    return (retval);
  }
     
  /***************************************************************************
  **
  ** Get the keys of objects referencing the measurement scale for a merge
  */
 
  public Dependencies getMeasureScaleMergeSet(Set<String> msIDs, String commonKey) {
    
    HashSet<String> mUsed = new HashSet<String>();
    MeasureDictionary md = pd_.getMeasureDictionary();
    Iterator<String> mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mkey = mkit.next();
      MeasureProps mp = md.getMeasureProps(mkey);
      if (msIDs.contains(mp.getScaleKey())) {
        mUsed.add(mkey);
      }
    } 
    return (new Dependencies(Dependencies.DepType.MERGE_MEASURE_SCALES, commonKey, new HashSet<String>(msIDs), null, null, null, mUsed, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of objects referencing the measurement properties for a merge
  */
 
  public Dependencies getMeasurePropMergeSet(Set<String> mpIDs, String commonKey) {
    HashSet<String> dpUsed = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (mpIDs.contains(pdp.getMeasurementTypeKey())) {
        dpUsed.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.MERGE_MEASURE_PROPS, commonKey, new HashSet<String>(mpIDs), dpUsed, null, null, null, null, null, null));
  } 
   
  /***************************************************************************
  **
  ** Get the keys of objects referencing the perturb properties for a merge
  */
 
  public Dependencies getPertPropMergeSet(Set<String> ppIDs, String commonKey) {
      
    HashSet<String> pPUses = new HashSet<String>();
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      String tkey = chk.getExpTypeKey();
      if (ppIDs.contains(tkey)) {
        pPUses.add(psdKey);
      }
    }
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<PertSources>> psmd = tcd.getPertSourceMergeDependencies(pPUses);

    return (new Dependencies(Dependencies.DepType.MERGE_PERT_PROPS, commonKey, new HashSet<String>(ppIDs), null, null, pPUses, null, null, null, psmd));
  } 
   
  /***************************************************************************
  **
  ** Get the keys of data points referencing the measurement type
  */
 
  public Map<String, Integer> getAllMeasurePropReferenceCounts() {
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getMeasurementTypeKey());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points and pertSources referencing the perturbation type
  */
 
  public Dependencies getPertTypeReferenceSets(String pertTypeID) {
    
    HashSet<String> pSUses = new HashSet<String>();
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    PertDictionary pDict = pd_.getPertDictionary();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      PertProperties pp = chk.getExpType(pDict);
      if (pp.getID().equals(pertTypeID)) {
        pSUses.add(chk.getID());
      }
    }
    
    HashSet<String> pSIUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> ppit = pss.getSources();
      while (ppit.hasNext()) {
        String srcID = ppit.next();
        if (pSUses.contains(srcID)) {
          pSIUses.add(psi.getID());
        }
      }
    }
      
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }    
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet<String> tcdUses = new HashSet<String>();   
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
    Iterator<String> psuit = pSUses.iterator();
    while (psuit.hasNext()) {
      String psID = psuit.next();
      Set<String> forPsID = psd.get(psID);
      if (forPsID != null) {
        tcdUses.add(psID);
      }
    }
     
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, dpUses, pSIUses, pSUses, null, null, tcdUses, null));
  }

  /***************************************************************************
  **
  ** Get the keys of data points referencing the target
  */
 
  public Dependencies getTargetReferenceSet(String targID) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pdp.getTargetKey().equals(targID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the targets for a merge
  */
 
  public Dependencies getTargetMergeSet(Set<String> targIDs, String commonKey) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (targIDs.contains(pdp.getTargetKey())) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.MERGE_TARGETS, commonKey, new HashSet<String>(targIDs), retval, null, null, null, null, null, null));
  }
  
    
  /***************************************************************************
  **
  ** Get the keys of data points referencing the experimental control for a merge
  */
 
  public Dependencies getExprControlMergeSet(Set<String> ctrlIDs, String commonKey) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (ctrlIDs.contains(pdp.getControl())) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DepType.MERGE_CONTROLS, commonKey, new HashSet<String>(ctrlIDs), retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the target
  */
 
  public int getTargetReferenceCount(String targID) {
    Dependencies dep = getTargetReferenceSet(targID);
    return (dep.dataPoints.size());
  } 
  
  /***************************************************************************
  **
  ** Get the count of all target references
  */
 
  public Map<String, Integer> getAllTargetReferenceCounts() {
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      String targKey = pdp.getTargetKey();
      DataUtil.bumpCountMap(retval, targKey); 
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the keys of elements referencing the source definitions
  */
 
  public Dependencies getSourceDefReferenceSets(String sourceDefID) {

    HashSet<String> pSInfoUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = pssit.next();
        if (psid.equals(sourceDefID)) {
          pSInfoUses.add(psi.getID());
          break;
        }
      }   
    }
    
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pSInfoUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet<String> tcdUses = new HashSet<String>();   
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
    Set<String> forKey = psd.get(sourceDefID);
    if (forKey != null) {
      tcdUses.add(sourceDefID);
    } 
    
    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, dpUses, pSInfoUses, null, null, null, tcdUses, null));
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for pert source definitions
  */
 
  public Set<String> getMultiSourceDefCollapseMergeSet(Set<String> sdIDs) {
    
    HashSet<String> sdUsed = new HashSet<String>();    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> pssit = pss.getSources();
      int numDups = 0;
      while (pssit.hasNext()) {
        String psid = pssit.next();
        if (sdIDs.contains(psid)) {
          numDups++;
        }
      }
      if (numDups > 1) {
        sdUsed.add(psi.getID());       
      }
    } 
    return (sdUsed);
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for pert source definitions
  */
 
  public Dependencies getSourceDefMergeSet(Set<String> sdIDs, String commonKey) {
    
    HashSet<String> sdUsed = new HashSet<String>();    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = pssit.next();
        if (sdIDs.contains(psid)) {
          sdUsed.add(psi.getID());
          break;
        }
      }
    }
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<PertSources>> psmd = tcd.getPertSourceMergeDependencies(sdIDs);
 
    return (new Dependencies(Dependencies.DepType.MERGE_SOURCE_DEFS, commonKey, new HashSet<String>(sdIDs), null, sdUsed, null, null, null, null, psmd));
  }
 
  /***************************************************************************
  **
  ** Get the keys of sourcw defs referencing the source name for a merge
  */
 
  public Dependencies getSourceNameMergeSet(Set<String> srcIDs, String commonKey) {
    HashSet<String> pSUses = new HashSet<String>();
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      if (chk.isAProxy() && srcIDs.contains(chk.getProxiedSpeciesKey())) {
        pSUses.add(chk.getID());
      }
      if (srcIDs.contains(chk.getSourceNameKey())) {
        pSUses.add(chk.getID());     
      }
    }
    
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<PertSources>> psmd = tcd.getPertSourceMergeDependencies(pSUses);

    return (new Dependencies(Dependencies.DepType.MERGE_SOURCE_NAMES, commonKey, new HashSet<String>(srcIDs), null, null, pSUses, null, null, null, psmd));
  }
      
  /***************************************************************************
  **
  ** Get the keys of elements referencing the source name
  */
 
  public Dependencies getSourceNameReferenceSets(String sourceNameID) {

    HashSet<String> pSUses = new HashSet<String>();
    
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      if (chk.isAProxy() && chk.getProxiedSpeciesKey().equals(sourceNameID)) {
        pSUses.add(chk.getID());
      }
      if (chk.getSourceNameKey().equals(sourceNameID)) {
        pSUses.add(chk.getID());     
      }
    }
    
    HashSet<String> pSInfoUses = new HashSet<String>();
    
    Iterator<String> psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator<String> pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = pssit.next();
        if (pSUses.contains(psid)) {
          pSInfoUses.add(psi.getID());
          break;
        }
      }   
    }
    
    HashSet<String> dpUses = new HashSet<String>();
    
    Iterator<PertDataPoint> dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pSInfoUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet<String> tcdUses = new HashSet<String>();   
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
    Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
    Iterator<String> psuit = pSUses.iterator();
    while (psuit.hasNext()) {
      String psID = psuit.next();
      Set<String> forPsID = psd.get(psID);
      if (forPsID != null) {
        tcdUses.add(psID);
      }
    }

    return (new Dependencies(Dependencies.DepType.DESTROY, null, null, dpUses, pSInfoUses, pSUses, null, null, tcdUses, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the target
  */
 
  public int getSourceNameReferenceCount(String srcID) {
    Dependencies dep = getSourceNameReferenceSets(srcID);
    return (dep.dataPoints.size() + dep.experiments.size() + dep.pertSources.size() + dep.timeCourseRefs.size());
  } 
  
  /***************************************************************************
  **
  ** Get the count of all source name refs.
  */
 
  public Map<String, Integer> getAllSourceNameReferenceCounts(boolean primaryOnly) {

    HashMap<String, Integer> retval = new HashMap<String, Integer>();   
    
    if (!primaryOnly) {
      Iterator<PertDataPoint> dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = dpit.next();
        Experiment exp = pdp.getExperiment(pd_);
        PertSources ps = exp.getSources();
        Iterator<String> psit = ps.getSources();
        while (psit.hasNext()) {
          String psdKey = psit.next();
          PertSource chk = pd_.getSourceDef(psdKey);    
          if (chk.isAProxy()) {    
            String proxKey = chk.getProxiedSpeciesKey();
            DataUtil.bumpCountMap(retval, proxKey); 
          }
          DataUtil.bumpCountMap(retval, chk.getSourceNameKey());
        }
      }
      
      //
      // Count the perturbed time course references too!
      //

      TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();    
      Map<String, Set<String>> psd = tcd.getPertSourceDependencies();
      Iterator<String> tcit = psd.keySet().iterator();
      while (tcit.hasNext()) {
        String psdKey = tcit.next();
        Set<String> forKey = psd.get(psdKey);
        int fks = forKey.size();
        for (int i = 0; i < fks; i++) {
          DataUtil.bumpCountMap(retval, psdKey);    
        }
      }
    }
 
    //
    // Only count the raw usage if it wasn't used in a data point, or if we
    // are primary only:
    //

    HashMap<String, Integer> retvalClone = (primaryOnly) ? null : new HashMap<String, Integer>(retval);
    Iterator<String> sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);    
      if (chk.isAProxy()) {    
        String proxKey = chk.getProxiedSpeciesKey();
        if (primaryOnly || (retvalClone.get(proxKey) == null)) {
          DataUtil.bumpCountMap(retval, proxKey);
        }
      }
      String srcKey = chk.getSourceNameKey();
      if (primaryOnly || (retvalClone.get(srcKey) == null)) {
        DataUtil.bumpCountMap(retval, srcKey);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Do merging of dependencies
  */
 
  public boolean mergeDependencies(Dependencies refs, DataAccessContext dacx, UndoSupport support) {
    if (refs.type == Dependencies.DepType.MERGE_TARGETS) {   
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointTargetRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }
    } else if (refs.type == Dependencies.DepType.MERGE_CONTROLS) {   
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointControlRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }  
    } else if (refs.type == Dependencies.DepType.MERGE_SOURCE_NAMES) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(dacx, tcca[j]));
        }     
      }          
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergePertSourceNameRefs(refs.pertSources, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }

    } else if (refs.type == Dependencies.DepType.MERGE_INVEST) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeInvestigatorRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }
    } else if (refs.type == Dependencies.DepType.MERGE_MEASURE_SCALES) {
      if ((refs.measureProps != null) && (refs.measureProps.size() > 0)) {
        PertDataChange pdc = pd_.mergeMeasureScaleRefs(refs.measureProps, refs.useKey);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }   
    } else if (refs.type == Dependencies.DepType.MERGE_MEASURE_PROPS) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeMeasurePropRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }       
    } else if (refs.type == Dependencies.DepType.MERGE_PERT_PROPS) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(dacx, tcca[j]));
        }     
      }       
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergePertPropRefs(refs.pertSources, refs.useKey);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }   
    } else if (refs.type == Dependencies.DepType.MERGE_SOURCE_DEFS) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(dacx, tcca[j]));
        }     
      }
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeSourceDefRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }
     
    } else if (refs.type == Dependencies.DepType.MERGE_EXPERIMENTS) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeExperimentRefs(refs.dataPoints, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }      
    } else if (refs.type == Dependencies.DepType.MERGE_ANNOT) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointAnnotRefs(refs.dataPoints, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }       
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergeSourceDefAnnotRefs(refs.pertSources, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }       
      if ((refs.targets != null) && (refs.targets.size() > 0)) {
        PertDataChange pdc = pd_.mergeTargetAnnotRefs(refs.targets, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }       
      
    } else if (refs.type == Dependencies.DepType.MERGE_EXPR_COND) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeExperimentCondRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }     
    } else {
      throw new IllegalArgumentException();
    }
    
    return (true);
  }

  /***************************************************************************
  **
  ** Kill off dependencies
  */ 
  
  public boolean killOffDependencies(Dependencies refs, DataAccessContext dacx, UndoSupport support) { 
    
    if (refs.type == Dependencies.DepType.DESTROY) {
      
      //
      // Gotta happen early, as undo prep needs valid refs:
      //
      if ((refs.timeCourseRefs != null) && (refs.timeCourseRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        Iterator<String> tcrit = refs.timeCourseRefs.iterator();
        while (tcrit.hasNext()) {
          String key = tcrit.next();
          TimeCourseChange[] tcca = tcd.dropPertSourceDependencies(key);
          for (int j = 0; j < tcca.length; j++) {
            support.addEdit(new TimeCourseChangeCmd(dacx, tcca[j]));
          }
        }        
      }
      
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.deleteDataPoints(refs.dataPoints);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }

      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.deleteExperiments(refs.experiments);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }      

      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange[] pdc = pd_.deletePertSourceDefs(refs.pertSources);
        for (int j = 0; j < pdc.length; j++) {
          support.addEdit(new PertDataChangeCmd(dacx, pdc[j]));
        }
      }
      
      if ((refs.measureProps != null) && (refs.measureProps.size() > 0)) {
        Iterator<String> mpit = refs.measureProps.iterator();
        while (mpit.hasNext()) {
          String key = mpit.next();
          PertDataChange pdc = pd_.deleteMeasureProp(key);
          support.addEdit(new PertDataChangeCmd(dacx, pdc));
        }        
      } 
         
    } else if (refs.type == Dependencies.DepType.PRUNE_INVEST) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.dropInvestigator(refs.useKey, refs.experiments);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }       
    } else if (refs.type == Dependencies.DepType.PRUNE_ANNOT) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.dropDataPointAnnotations(refs.useKey, refs.dataPoints);
        if (pdc != null) {
          support.addEdit(new PertDataChangeCmd(dacx, pdc));
        }
      }
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.dropSourceDefAnnotations(refs.useKey, refs.pertSources);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }
      if ((refs.targets != null) && (refs.targets.size() > 0)) {
        PertDataChange pdc = pd_.dropTargetAnnotations(refs.useKey, refs.targets);
        support.addEdit(new PertDataChangeCmd(dacx, pdc));
      }     
      
    } else if (refs.type == Dependencies.DepType.PRUNE_CONTROL) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.dropDataPointControls(refs.dataPoints);
        if (pdc != null) {
          support.addEdit(new PertDataChangeCmd(dacx, pdc));
        }
      }
    } else {
      throw new IllegalArgumentException();
    }
    return (true);
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static class Dependencies {
    
    public enum DepType {    
      DESTROY,
      PRUNE_INVEST,
      PRUNE_ANNOT,
      PRUNE_CONTROL,
      MERGE_TARGETS,
      MERGE_SOURCE_NAMES,
      MERGE_INVEST,
      MERGE_CONTROLS,
      MERGE_MEASURE_SCALES,
      MERGE_ANNOT,
      MERGE_EXPR_COND,
      MERGE_MEASURE_PROPS,
      MERGE_SOURCE_DEFS,
      MERGE_EXPERIMENTS,
      MERGE_PERT_PROPS}
    
    public Set<String> dataPoints;
    public Set<String> experiments;
    public Set<String> pertSources;
    public Set<String> measureProps;
    public Set<String> targets;
    public DepType type;
    public String useKey;
    public Set<String> abandonKeys;
    public Set<String> timeCourseRefs;
    public Map<String, Set<PertSources>> timeCourseMergeRefs;
        
    Dependencies(DepType type, String useKey, Set<String> abandonKeys, Set<String> dataPoints, 
                           Set<String> experiments, Set<String> pertSources, 
                           Set<String> measureProps, Set<String> targets, Set<String> timeCourseRefs, 
                           Map<String, Set<PertSources>> timeCourseMergeRefs) {
      this.type = type;
      this.useKey = useKey;
      this.dataPoints = dataPoints;
      this.experiments = experiments;
      this.pertSources = pertSources;
      this.measureProps = measureProps;
      this.abandonKeys = abandonKeys;
      this.targets = targets;
      this.timeCourseRefs = timeCourseRefs;
      this.timeCourseMergeRefs = timeCourseMergeRefs;
    }
  }
}
