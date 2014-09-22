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

package org.systemsbiology.biotapestry.perturb;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

import org.systemsbiology.biotapestry.app.BTState;
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

  public DependencyAnalyzer(BTState appState, PerturbationData pd) {
    appState_ = appState;
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
 
    HashSet pSUses = new HashSet();  
    
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List aids = chk.getAnnotationIDs();
      if (aids.contains(annotID)) {
        pSUses.add(psdKey);
      }
    }
    
    HashSet pSIUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator ppit = pss.getSources();
      while (ppit.hasNext()) {
        String srcID = (String)ppit.next();
        if (pSUses.contains(srcID)) {
          pSIUses.add(psi.getID());
        }
      }
    }

    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      String id = pdp.getID();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(id);
      } else {
        List dpn = pd_.getDataPointNotes(id);
        if ((dpn != null) && dpn.contains(annotID)) {
          dpUses.add(id);
        }
      }
    }
    
    HashSet targUses = new HashSet();
    
    Iterator tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = (String)tkit.next();
      List tn = pd_.getFootnotesForTarget(targKey);
      if ((tn != null) && tn.contains(annotID)) {
        targUses.add(targKey);
      }
    }
  
    return (new Dependencies(Dependencies.PRUNE_ANNOT, annotID, null, dpUses, pSIUses, pSUses, null, targUses, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the annotation merge set
  */
 
  public Dependencies getAnnotMergeSet(Set joinKeys, String commonKey) {
    
    HashSet pSUses = new HashSet();     
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List aids = chk.getAnnotationIDs();
      int numa = aids.size();
      for (int i = 0; i < numa; i++) {
        String annotKey = (String)aids.get(i);
        if (joinKeys.contains(annotKey)) {
          pSUses.add(psdKey);
          break;
        }
      }
    }
 
    HashSet dpUses = new HashSet();  
    Iterator dpit = pd_.getDataPointNoteKeys();
    while (dpit.hasNext()) {
      String id = (String)dpit.next();
      List aids = pd_.getDataPointNotes(id);
      int numa = aids.size();
      for (int i = 0; i < numa; i++) {
        String annotKey = (String)aids.get(i);
        if (joinKeys.contains(annotKey)) {
          dpUses.add(id);
          break;
        }
      }
    }
    
    HashSet targUses = new HashSet();   
    Iterator tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = (String)tkit.next();
      List aids = pd_.getFootnotesForTarget(targKey);
      if (aids != null) {
        int numa = aids.size();
        for (int i = 0; i < numa; i++) {
          String annotKey = (String)aids.get(i);
          if (joinKeys.contains(annotKey)) {
            aids.add(targKey);
            break;
          }
        }
      }
    }
    
    return (new Dependencies(Dependencies.MERGE_ANNOT, commonKey, new HashSet(joinKeys), dpUses, null, pSUses, null, targUses, null, null));  
  }
  

  /***************************************************************************
  **
  ** Get the count of dependencies referencing all annotations
  */
 
  public Map getAllAnnotReferenceCounts() {
   
    HashMap retval = new HashMap(); 
    
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      List aids = chk.getAnnotationIDs();
      int numAids = aids.size();
      for (int i = 0; i < numAids; i++) {
        String aid = (String)aids.get(i);
        DataUtil.bumpCountMap(retval, aid);
      }
    }   
    
    Iterator dpknit = pd_.getDataPointNoteKeys();   
    while (dpknit.hasNext()) {
      String dpnkey = (String)dpknit.next();
      List dpn = pd_.getDataPointNotes(dpnkey);
      int numDpn = dpn.size();
      for (int i = 0; i < numDpn; i++) {
        String aid = (String)dpn.get(i);
        DataUtil.bumpCountMap(retval, aid);         
      }
    }
    
    Iterator tkit = pd_.getTargetKeys();
    while (tkit.hasNext()) {
      String targKey = (String)tkit.next();
      List tn = pd_.getFootnotesForTarget(targKey);
      if (tn != null) {
        int numTn = tn.size();
        for (int i = 0; i < numTn; i++) {
          String aid = (String)tn.get(i);
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
    HashSet retval = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pdp.getExperimentKey().equals(psiID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the perturbation source
  */
 
  public Dependencies getInvestReferenceSet(String invID) {
        
    HashSet pSIUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      List invests = psi.getInvestigators();
      if (invests.contains(invID)) {
        pSIUses.add(psi.getID());
      }
    }
      
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.PRUNE_INVEST, invID, null, dpUses, pSIUses, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the counts of references of the perturbation source definition
  */
 
  public Map getAllSrcDefReferenceCounts() {
        
    HashMap retval = new HashMap();   
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      PertSources ps = exp.getSources();
      Iterator psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = (String)psit.next();
        DataUtil.bumpCountMap(retval, psdKey);
      }
    }
    
    //
    // Count the perturbed time course references too!
    //
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psd = tcd.getPertSourceDependencies();
    Iterator tcit = psd.keySet().iterator();
    while (tcit.hasNext()) {
      String psdKey = (String)tcit.next();
      Set forKey = (Set)psd.get(psdKey);
      int fks = forKey.size();
      for (int i = 0; i < fks; i++) {
        DataUtil.bumpCountMap(retval, psdKey);    
      }
    }

    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap retvalClone = (HashMap)retval.clone();
    Iterator ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = (String)ekit.next();
      Experiment exp = pd_.getExperiment(eKey);
      PertSources ps = exp.getSources();
      Iterator psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = (String)psit.next();
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
 
  public Map getAllInvestigatorReferenceCounts(boolean primaryOnly) {
        
    HashMap retval = new HashMap();
    
    if (!primaryOnly) {
      Iterator dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = (PertDataPoint)dpit.next();
        Experiment exp = pdp.getExperiment(pd_);
        List invs = exp.getInvestigators();
        int numi = invs.size();
        for (int i = 0; i < numi; i++) {
          String invKey = (String)invs.get(i);
          DataUtil.bumpCountMap(retval, invKey);
        }
      }
    }
  
    //
    // Only count the raw usage if it wasn't used in a data point,
    // or if primary only!
    //
    
    HashMap retvalClone = (primaryOnly) ? null : (HashMap)retval.clone();
    Iterator ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = (String)ekit.next();
      Experiment psi = pd_.getExperiment(eKey);
      List invs = psi.getInvestigators();
      int numi = invs.size();
      for (int i = 0; i < numi; i++) {
        String invKey = (String)invs.get(i);
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
 
  public Dependencies getInvestigatorMergeSet(Set investIDs, String commonKey) {
    HashSet expUses = new HashSet();
    
    Iterator ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = (String)ekit.next();
      Experiment psi = pd_.getExperiment(eKey);
      List invs = psi.getInvestigators();
      int numi = invs.size();
      for (int i = 0; i < numi; i++) {
        String invKey = (String)invs.get(i);
        if (investIDs.contains(invKey)) {
          expUses.add(eKey);     
        }
      }
    }
    
    return (new Dependencies(Dependencies.MERGE_INVEST, commonKey, new HashSet(investIDs), null, expUses, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the experiment
  */
 
  public Map getAllExperimentReferenceCounts() {
        
    HashMap retval = new HashMap();      
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getExperimentKey());
    } 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for experiments
  */
 
  public Dependencies getExperimentMergeSet(Set expIDs, String commonKey) {
    
    HashSet expUsed = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      String expid = pdp.getExperimentKey();
      if (expIDs.contains(expid)) {
        expUsed.add(pdp.getID());
      }
    }
    
    return (new Dependencies(Dependencies.MERGE_EXPERIMENTS, commonKey, new HashSet(expIDs), expUsed, null, null, null, null, null, null));
  }

  /***************************************************************************
  **
  ** Get the counts of references to experimental conditions
  */
 
  public Map getAllExprConditionReferenceCounts() {
    
    HashMap retval = new HashMap();   
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      DataUtil.bumpCountMap(retval, exp.getConditionKey());
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap retvalClone = (HashMap)retval.clone();
    Iterator ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = (String)ekit.next();
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
 
  public Map getAllExprControlReferenceCounts() {    
    HashMap retval = new HashMap();      
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getControl());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the dependencies referencing the experimental control
  */
 
  public Dependencies getExprControlReferenceSet(String ctrl) {
  
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      String id = pdp.getID();
      if (ctrl.equals(pdp.getControl())) {
        dpUses.add(id);
      }
    }  
    return (new Dependencies(Dependencies.PRUNE_CONTROL, ctrl, null, dpUses, null, null, null, null, null, null));
  }
    
  /***************************************************************************
  **
  ** Get the set of references to experimental conditions
  */
 
  public Dependencies getExprConditionReferenceSet(String key) {
    
    HashSet pSIUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      String condKey = psi.getConditionKey();
      if (condKey.equals(key)) {
        pSIUses.add(psi.getID());
      }
    }
      
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DESTROY, null, null, dpUses, pSIUses, null, null, null, null, null));  
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for experimental conditions
  */
 
  public Dependencies getExprConditionMergeSet(Set ecIDs, String commonKey) {
    
    HashSet expUsed = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      if (ecIDs.contains(psi.getConditionKey())) {
        expUsed.add(psiKey);
      }
    }
    
    return (new Dependencies(Dependencies.MERGE_EXPR_COND, commonKey, new HashSet(ecIDs), null, expUsed, null, null, null, null, null));
  }
  
 
  /***************************************************************************
  **
  ** Get the keys of data points referencing the measurement type
  */
 
  public Dependencies getMeasureReferenceSet(String meaID) {
    HashSet retval = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pdp.getMeasurementTypeKey().equals(meaID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of objects referencing the measurement scale
  */
 
  public Dependencies getMeasScaleReferenceSet(String scaleID) {
    
    HashSet mUsed = new HashSet();
    MeasureDictionary md = pd_.getMeasureDictionary();
    Iterator mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mkey = (String)mkit.next();
      MeasureProps mp = (MeasureProps)md.getMeasureProps(mkey);
      if (mp.getScaleKey().equals(scaleID)) {
        mUsed.add(mkey);
      }
    } 

    HashSet dpUsed = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (mUsed.contains(pdp.getMeasurementTypeKey())) {
        dpUsed.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DESTROY, null, null, dpUsed, null, null, mUsed, null, null, null));
  }

  /***************************************************************************
  **
  ** Get the counts of references for all perturbation types
  */
 
  public Map getAllPertPropReferenceCounts() {
  
    HashMap retval = new HashMap();   
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      Experiment exp = pdp.getExperiment(pd_);
      PertSources ps = exp.getSources();
      Iterator psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = (String)psit.next();
        PertSource chk = pd_.getSourceDef(psdKey); 
        DataUtil.bumpCountMap(retval, chk.getExpTypeKey());
      }
    }
    
    //
    // Count the perturbed time course references too!
    //
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psd = tcd.getPertSourceDependencies();
    Iterator tcit = psd.keySet().iterator();
    while (tcit.hasNext()) {
      String psdKey = (String)tcit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      String typeKey = chk.getExpTypeKey();
      Set forKey = (Set)psd.get(psdKey);
      int fks = forKey.size();
      for (int i = 0; i < fks; i++) {
        DataUtil.bumpCountMap(retval, typeKey);    
      }
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point:
    //
    
    HashMap retvalClone = (HashMap)retval.clone();
    Iterator ekit = pd_.getExperimentKeys();
    while (ekit.hasNext()) {
      String eKey = (String)ekit.next();
      Experiment exp = pd_.getExperiment(eKey);
      PertSources ps = exp.getSources();
      Iterator psit = ps.getSources();
      while (psit.hasNext()) {
        String psdKey = (String)psit.next();
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
    
    retvalClone = (HashMap)retval.clone();
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
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
 
  public Map getAllMeasureScaleReferenceCounts(boolean primaryOnly) {
    
    MeasureDictionary md = pd_.getMeasureDictionary();
    HashMap retval = new HashMap();   
    
    if (!primaryOnly) {
      Iterator dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = (PertDataPoint)dpit.next();
        String mType = pdp.getMeasurementTypeKey();
        MeasureProps mp = md.getMeasureProps(mType);
        DataUtil.bumpCountMap(retval, mp.getScaleKey());
      }
    }
    
    //
    // Only count the raw usage if it wasn't used in a data point, or
    // if targeting primary counts:
    //
   
    HashMap retvalClone = (primaryOnly) ? null : (HashMap)retval.clone();
    Iterator mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mKey = (String)mkit.next();
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
 
  public Dependencies getMeasureScaleMergeSet(Set msIDs, String commonKey) {
    
    HashSet mUsed = new HashSet();
    MeasureDictionary md = pd_.getMeasureDictionary();
    Iterator mkit = md.getKeys();
    while (mkit.hasNext()) {
      String mkey = (String)mkit.next();
      MeasureProps mp = (MeasureProps)md.getMeasureProps(mkey);
      if (msIDs.contains(mp.getScaleKey())) {
        mUsed.add(mkey);
      }
    } 
    return (new Dependencies(Dependencies.MERGE_MEASURE_SCALES, commonKey, new HashSet(msIDs), null, null, null, mUsed, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of objects referencing the measurement properties for a merge
  */
 
  public Dependencies getMeasurePropMergeSet(Set mpIDs, String commonKey) {
    HashSet dpUsed = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (mpIDs.contains(pdp.getMeasurementTypeKey())) {
        dpUsed.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.MERGE_MEASURE_PROPS, commonKey, new HashSet(mpIDs), dpUsed, null, null, null, null, null, null));
  } 
   
  /***************************************************************************
  **
  ** Get the keys of objects referencing the perturb properties for a merge
  */
 
  public Dependencies getPertPropMergeSet(Set ppIDs, String commonKey) {
      
    HashSet pPUses = new HashSet();
    
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      String tkey = chk.getExpTypeKey();
      if (ppIDs.contains(tkey)) {
        pPUses.add(psdKey);
      }
    }
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psmd = tcd.getPertSourceMergeDependencies(pPUses);

    return (new Dependencies(Dependencies.MERGE_PERT_PROPS, commonKey, new HashSet(ppIDs), null, null, pPUses, null, null, null, psmd));
  } 
   
  /***************************************************************************
  **
  ** Get the keys of data points referencing the measurement type
  */
 
  public Map getAllMeasurePropReferenceCounts() {
    HashMap retval = new HashMap();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      DataUtil.bumpCountMap(retval, pdp.getMeasurementTypeKey());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points and pertSources referencing the perturbation type
  */
 
  public Dependencies getPertTypeReferenceSets(String pertTypeID) {
    
    HashSet pSUses = new HashSet();
    
    Iterator sdit = pd_.getSourceDefKeys();
    PertDictionary pDict = pd_.getPertDictionary();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      PertProperties pp = chk.getExpType(pDict);
      if (pp.getID().equals(pertTypeID)) {
        pSUses.add(chk.getID());
      }
    }
    
    HashSet pSIUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator ppit = pss.getSources();
      while (ppit.hasNext()) {
        String srcID = (String)ppit.next();
        if (pSUses.contains(srcID)) {
          pSIUses.add(psi.getID());
        }
      }
    }
      
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pSIUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }    
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet tcdUses = new HashSet();   
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psd = tcd.getPertSourceDependencies();
    Iterator psuit = pSUses.iterator();
    while (psuit.hasNext()) {
      String psID = (String)psuit.next();
      Set forPsID = (Set)psd.get(psID);
      if (forPsID != null) {
        tcdUses.add(psID);
      }
    }
     
    return (new Dependencies(Dependencies.DESTROY, null, null, dpUses, pSIUses, pSUses, null, null, tcdUses, null));
  }

  /***************************************************************************
  **
  ** Get the keys of data points referencing the target
  */
 
  public Dependencies getTargetReferenceSet(String targID) {
    HashSet retval = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pdp.getTargetKey().equals(targID)) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.DESTROY, null, null, retval, null, null, null, null, null, null));
  }
  
  /***************************************************************************
  **
  ** Get the keys of data points referencing the targets for a merge
  */
 
  public Dependencies getTargetMergeSet(Set targIDs, String commonKey) {
    HashSet retval = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (targIDs.contains(pdp.getTargetKey())) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.MERGE_TARGETS, commonKey, new HashSet(targIDs), retval, null, null, null, null, null, null));
  }
  
    
  /***************************************************************************
  **
  ** Get the keys of data points referencing the experimental control for a merge
  */
 
  public Dependencies getExprControlMergeSet(Set ctrlIDs, String commonKey) {
    HashSet retval = new HashSet();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (ctrlIDs.contains(pdp.getControl())) {
        retval.add(pdp.getID());
      }
    }
    return (new Dependencies(Dependencies.MERGE_CONTROLS, commonKey, new HashSet(ctrlIDs), retval, null, null, null, null, null, null));
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
 
  public Map getAllTargetReferenceCounts() {
    HashMap retval = new HashMap();
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
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

    HashSet pSInfoUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = (String)pssit.next();
        if (psid.equals(sourceDefID)) {
          pSInfoUses.add(psi.getID());
          break;
        }
      }   
    }
    
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pSInfoUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet tcdUses = new HashSet();   
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psd = tcd.getPertSourceDependencies();
    Set forKey = (Set)psd.get(sourceDefID);
    if (forKey != null) {
      tcdUses.add(sourceDefID);
    } 
    
    return (new Dependencies(Dependencies.DESTROY, null, null, dpUses, pSInfoUses, null, null, null, tcdUses, null));
  }
  
  /***************************************************************************
  **
  ** Get the merge dependencies for pert source definitions
  */
 
  public Set getMultiSourceDefCollapseMergeSet(Set sdIDs, String commonKey) {
    
    HashSet sdUsed = new HashSet();    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator pssit = pss.getSources();
      int numDups = 0;
      while (pssit.hasNext()) {
        String psid = (String)pssit.next();
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
 
  public Dependencies getSourceDefMergeSet(Set sdIDs, String commonKey) {
    
    HashSet sdUsed = new HashSet();    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = (String)pssit.next();
        if (sdIDs.contains(psid)) {
          sdUsed.add(psi.getID());
          break;
        }
      }
    }
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psmd = tcd.getPertSourceMergeDependencies(sdIDs);
 
    return (new Dependencies(Dependencies.MERGE_SOURCE_DEFS, commonKey, new HashSet(sdIDs), null, sdUsed, null, null, null, null, psmd));
  }
 
  /***************************************************************************
  **
  ** Get the keys of sourcw defs referencing the source name for a merge
  */
 
  public Dependencies getSourceNameMergeSet(Set srcIDs, String commonKey) {
    HashSet pSUses = new HashSet();
    
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      if (chk.isAProxy() && srcIDs.contains(chk.getProxiedSpeciesKey())) {
        pSUses.add(chk.getID());
      }
      if (srcIDs.contains(chk.getSourceNameKey())) {
        pSUses.add(chk.getID());     
      }
    }
    
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psmd = tcd.getPertSourceMergeDependencies(pSUses);

    return (new Dependencies(Dependencies.MERGE_SOURCE_NAMES, commonKey, new HashSet(srcIDs), null, null, pSUses, null, null, null, psmd));
  }
      
  /***************************************************************************
  **
  ** Get the keys of elements referencing the source name
  */
 
  public Dependencies getSourceNameReferenceSets(String sourceNameID) {

    HashSet pSUses = new HashSet();
    
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = pd_.getSourceDef(psdKey);
      if (chk.isAProxy() && chk.getProxiedSpeciesKey().equals(sourceNameID)) {
        pSUses.add(chk.getID());
      }
      if (chk.getSourceNameKey().equals(sourceNameID)) {
        pSUses.add(chk.getID());     
      }
    }
    
    HashSet pSInfoUses = new HashSet();
    
    Iterator psit = pd_.getExperimentKeys();
    while (psit.hasNext()) {
      String psiKey = (String)psit.next();
      Experiment psi = pd_.getExperiment(psiKey);
      PertSources pss = psi.getSources();
      Iterator pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid =(String)pssit.next();
        if (pSUses.contains(psid)) {
          pSInfoUses.add(psi.getID());
          break;
        }
      }   
    }
    
    HashSet dpUses = new HashSet();
    
    Iterator dpit = pd_.getDataPoints();
    while (dpit.hasNext()) {
      PertDataPoint pdp = (PertDataPoint)dpit.next();
      if (pSInfoUses.contains(pdp.getExperimentKey())) {
        dpUses.add(pdp.getID());
      }
    }
    
    //
    // Deal with perturbed time course references too!
    //
    
    HashSet tcdUses = new HashSet();   
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
    Map psd = tcd.getPertSourceDependencies();
    Iterator psuit = pSUses.iterator();
    while (psuit.hasNext()) {
      String psID = (String)psuit.next();
      Set forPsID = (Set)psd.get(psID);
      if (forPsID != null) {
        tcdUses.add(psID);
      }
    }

    return (new Dependencies(Dependencies.DESTROY, null, null, dpUses, pSInfoUses, pSUses, null, null, tcdUses, null));
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
 
  public Map getAllSourceNameReferenceCounts(boolean primaryOnly) {

    HashMap retval = new HashMap();   
    
    if (!primaryOnly) {
      Iterator dpit = pd_.getDataPoints();
      while (dpit.hasNext()) {
        PertDataPoint pdp = (PertDataPoint)dpit.next();
        Experiment exp = pdp.getExperiment(pd_);
        PertSources ps = exp.getSources();
        Iterator psit = ps.getSources();
        while (psit.hasNext()) {
          String psdKey = (String)psit.next();
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

      TimeCourseData tcd = appState_.getDB().getTimeCourseData();    
      Map psd = tcd.getPertSourceDependencies();
      Iterator tcit = psd.keySet().iterator();
      while (tcit.hasNext()) {
        String psdKey = (String)tcit.next();
        Set forKey = (Set)psd.get(psdKey);
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

    HashMap retvalClone = (primaryOnly) ? null : (HashMap)retval.clone();
    Iterator sdit = pd_.getSourceDefKeys();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
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
    if (refs.type == Dependencies.MERGE_TARGETS) {   
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointTargetRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }
    } else if (refs.type == Dependencies.MERGE_CONTROLS) {   
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointControlRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }  
    } else if (refs.type == Dependencies.MERGE_SOURCE_NAMES) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(appState_, dacx, tcca[j]));
        }     
      }          
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergePertSourceNameRefs(refs.pertSources, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }

    } else if (refs.type == Dependencies.MERGE_INVEST) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeInvestigatorRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }
    } else if (refs.type == Dependencies.MERGE_MEASURE_SCALES) {
      if ((refs.measureProps != null) && (refs.measureProps.size() > 0)) {
        PertDataChange pdc = pd_.mergeMeasureScaleRefs(refs.measureProps, refs.useKey);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }   
    } else if (refs.type == Dependencies.MERGE_MEASURE_PROPS) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeMeasurePropRefs(refs.dataPoints, refs.useKey);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }       
    } else if (refs.type == Dependencies.MERGE_PERT_PROPS) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(appState_, dacx, tcca[j]));
        }     
      }       
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergePertPropRefs(refs.pertSources, refs.useKey);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }   
    } else if (refs.type == Dependencies.MERGE_SOURCE_DEFS) {
      // gotta come first:
      if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        TimeCourseChange[] tcca = tcd.dropPertSourceMergeIssues(refs.timeCourseMergeRefs);
        for (int j = 0; j < tcca.length; j++) {
          support.addEdit(new TimeCourseChangeCmd(appState_, dacx, tcca[j]));
        }     
      }
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeSourceDefRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }
     
    } else if (refs.type == Dependencies.MERGE_EXPERIMENTS) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeExperimentRefs(refs.dataPoints, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }      
    } else if (refs.type == Dependencies.MERGE_ANNOT) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.mergeDataPointAnnotRefs(refs.dataPoints, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }       
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.mergeSourceDefAnnotRefs(refs.pertSources, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }       
      if ((refs.targets != null) && (refs.targets.size() > 0)) {
        PertDataChange pdc = pd_.mergeTargetAnnotRefs(refs.targets, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }       
      
    } else if (refs.type == Dependencies.MERGE_EXPR_COND) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.mergeExperimentCondRefs(refs.experiments, refs.useKey, refs.abandonKeys);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
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
    
    if (refs.type == Dependencies.DESTROY) {
      
      //
      // Gotta happen early, as undo prep needs valid refs:
      //
      if ((refs.timeCourseRefs != null) && (refs.timeCourseRefs.size() > 0)) {
        TimeCourseData tcd = dacx.getExpDataSrc().getTimeCourseData();
        Iterator tcrit = refs.timeCourseRefs.iterator();
        while (tcrit.hasNext()) {
          String key = (String)tcrit.next();
          TimeCourseChange[] tcca = tcd.dropPertSourceDependencies(key);
          for (int j = 0; j < tcca.length; j++) {
            support.addEdit(new TimeCourseChangeCmd(appState_, dacx, tcca[j]));
          }
        }        
      }
      
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.deleteDataPoints(refs.dataPoints);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }

      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.deleteExperiments(refs.experiments);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }      

      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange[] pdc = pd_.deletePertSourceDefs(refs.pertSources);
        for (int j = 0; j < pdc.length; j++) {
          support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc[j]));
        }
      }
      
      if ((refs.measureProps != null) && (refs.measureProps.size() > 0)) {
        Iterator mpit = refs.measureProps.iterator();
        while (mpit.hasNext()) {
          String key = (String)mpit.next();
          PertDataChange pdc = pd_.deleteMeasureProp(key);
          support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
        }        
      } 
         
    } else if (refs.type == Dependencies.PRUNE_INVEST) {
      if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
        PertDataChange pdc = pd_.dropInvestigator(refs.useKey, refs.experiments);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }       
    } else if (refs.type == Dependencies.PRUNE_ANNOT) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.dropDataPointAnnotations(refs.useKey, refs.dataPoints);
        if (pdc != null) {
          support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
        }
      }
      if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
        PertDataChange pdc = pd_.dropSourceDefAnnotations(refs.useKey, refs.pertSources);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }
      if ((refs.targets != null) && (refs.targets.size() > 0)) {
        PertDataChange pdc = pd_.dropTargetAnnotations(refs.useKey, refs.targets);
        support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
      }     
      
    } else if (refs.type == Dependencies.PRUNE_CONTROL) {
      if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
        PertDataChange pdc = pd_.dropDataPointControls(refs.useKey, refs.dataPoints);
        if (pdc != null) {
          support.addEdit(new PertDataChangeCmd(appState_, dacx, pdc));
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
    
    public static final int DESTROY              = 0;
    public static final int PRUNE_INVEST         = 1;
    public static final int PRUNE_ANNOT          = 2;
    public static final int PRUNE_CONTROL        = 3;
    public static final int MERGE_TARGETS        = 4;
    public static final int MERGE_SOURCE_NAMES   = 5;
    public static final int MERGE_INVEST         = 6;
    public static final int MERGE_CONTROLS       = 7;
    public static final int MERGE_MEASURE_SCALES = 8;
    public static final int MERGE_ANNOT          = 9;
    public static final int MERGE_EXPR_COND      = 10;
    public static final int MERGE_MEASURE_PROPS  = 11;
    public static final int MERGE_SOURCE_DEFS    = 12;
    public static final int MERGE_EXPERIMENTS    = 13;
    public static final int MERGE_PERT_PROPS     = 14;
    
    public Set dataPoints;
    public Set experiments;
    public Set pertSources;
    public Set measureProps;
    public Set targets;
    public int type;
    public String useKey;
    public Set abandonKeys;
    public Set timeCourseRefs;
    public Map timeCourseMergeRefs;
        
    Dependencies(int type, String useKey, Set abandonKeys, Set dataPoints, 
                           Set experiments, Set pertSources, 
                           Set measureProps, Set targets, Set timeCourseRefs, 
                           Map timeCourseMergeRefs) {
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
