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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.ui.dialogs.TimeAxisSetupDialog;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.BatchDupReportDialog;
import org.systemsbiology.biotapestry.ui.dialogs.pertManage.NewbieReportingDialog;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.CSVParser;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** THIS COMMENT IS OBSOLETE AS OF 09/10!!  Just provides incomplete guidance
** at the moment!
**
** This sucks in Perturb data from a CSV file, using the new Davidson Lab/Qiang Tu
** format standard:
 
Content:

+ Header Lines
+ Column Definitions
+ General Rules
+ Example Table



---+++ Header Lines

Put these header lines at the top of the file before the real data:

Investigator 
Submission Date
(Blank line)
Column Names (see next section)
DATA
...

Check out the example at the end.



---+++ Column Defininitions

1) BatchID    
2) PerturbationAgent   
3) MeasuredGene    
4) Time    
5) DeltaDeltaCt    
6) ExpControl  
7) Significance    
8) Comment

9) Measure type
10) scale?
11) region restrict
12) user_field_name
13) Annot 



   1) BatchID: It is important to distinguish data that come from
      different cDNAs. Assign a unique ID to each batch of cDNA. 

      For the measurement of a certain
      PerturbationAgent/MeasuredGene/Time (P/M/T) combination, you
      might repeat the perturbation experiments several times so
      you have measurements from DIFFERENT batches of cDNAs
      These should all have a different BatchID. In contrast, when you
      use different experimental controls (see ExpControl below) to
      calculate an additional value for a given P/M/T combination
      using THE SAME cDNA, it will have the same BatchID.

      As the name 'BatchID' indicates, it is a good idea to use the direct
      information of the batch as the ID, for example the cDNA
      from the injection of animal A on 07/12/2007 should be called
      "07122006-A". If you did not record this information when you
      collected your data, choose something simple like "repeat1" or
      "batch1". 
   
   2) PerturbationAgent: Perturbation target gene (capitalized) and
      perturbation agent (all upper case letters), for example
      "Alx1MASO".

   3) MeasuredGene: The gene you measured in perturbed embryos by
      QPCR. 

   4) Time: Time post fertilization (in hours) when you collected the
      embryos.

   5) DeltaDeltaCt: This is the value you are really interested in, but
      of course you knew this already. 

   6) ExpControl: Experimental control, which you used to calculate the
      delta-delta-ct (NOT the internal control [usually ubq] used to
      calculate the delta-ct). The choices are "Uninjected", "CtrlMASO"
      etc. 

   7) Significance: The decision of a measurement to be significant or not
      is based on multiple reasons, not only the delta-delta-ct
      value. Mark "Yes" here if you decide it is significant,
      otherwise it will be treated as non-significant even if the
      delta-delta-ct is above our usual cutoff of 1.6 cycles.

   8) Comments: Whatever you feel you need to share ...



---+++ General Rules

   * All data should be submitted. That includes non-significant time
     points and genes which did not show any change in any of the time
     points you checked. This is important for network building. 

   * If you submit a measurement of a P/M/T combination using the same
     cDNA as in a previous submission but with a different
     experimental control, make sure you tell Bill the BatchID and
     submisstion date you used before, so that he can figure out the
     correct batch information by [SubmissionDate, Investigator,
     BatchID]. 
      


---+++ Examples

Here is what the table should look like according to the
specifications above:

---
Shrek
12/24/2006

BatchID    PerturbationAgent MeasuredGene Time DeltaDeltaCt ExpControl Significance  Comment
repeat1    GeneAMASO         GeneB        18hr  -2.2        Uninj      Yes
repeat1    GeneAMASO         GeneB        18hr  -2.0        CtrlMASO   Yes
repeat2    GeneAMASO         GeneB        18hr  -2.1        Uninj      Yes
repeat2    GeneAMASO         GeneB        18hr  -2.3        CtrlMASO   Yes
repeat3    GeneAMASO         GeneB        18hr  -1.5        Uninj         
repeat1    GeneAMASO         GeneC        18hr  2.1         Uninj                    high delta-delta-ct in CtrlMASO
---

The data will be interpreted as:
[GeneAMASO, GeneB, 18hr] = -2.2,-2.0/-2.1,-2.3/NS
[GeneAMASO, GeneC, 18hr] = NS 

*/

public class PerturbCsvFormatFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  //
  // For tracking the introduction of new entities
  //
  
  public static final String NEWBIE_INVEST       = "NewbieInvest";
  public static final String NEWBIE_PERT_SOURCE  = "NewbiePertSource";
  public static final String NEWBIE_TARGET       = "NewbieTarget";
  public static final String NEWBIE_CONTROL      = "NewbieControl";
  public static final String NEWBIE_SCALE        = "NewbieScale";
  public static final String NEWBIE_MEASURE_TYPE = "NewbieMeasureType";
  public static final String NEWBIE_PERT_TYPE    = "NewbiePertType";
  public static final String NEWBIE_EXP_COND     = "NewbieExpCond";
  public static final String NEWBIE_USER_FIELD   = "NewbieUserField";
  public static final String NEWBIE_ANNOTATION   = "NewbieAnnotation";
  public static final String NEWBIE_EXPERIMENT   = "NewbieExperiment";
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  private DataAccessContext dacx_;
  private HashMap newParamsInFile_;
  private HashMap paramNameToPdKeyMap_;
  private boolean useDate_;
  private boolean useTime_; 
  private boolean useBatch_;
  private boolean useInvest_;
  private boolean useCondition_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  public PerturbCsvFormatFactory(BTState appState, DataAccessContext dacx, boolean useDate, boolean useTime, 
                                 boolean useBatch, boolean useInvest, boolean useCondition) {
    appState_ = appState;
    dacx_ = dacx;
    newParamsInFile_ = new HashMap();
    paramNameToPdKeyMap_ = new HashMap();
    useDate_ = useDate;
    useTime_ = useTime;
    useBatch_ = useBatch;
    useInvest_ = useInvest;
    useCondition_ = useCondition;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the Perturb Data
  */

  public boolean parsePerturbCSV(File infile) throws IOException {
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    List<List<String>> blockList = fileToLineBlocks(infile);
    //
    // Separate IO Errors from data errors:
    //
    Iterator<List<String>> blit = blockList.iterator();
    ArrayList<CSVState> csvList = new ArrayList<CSVState>();
    int setNumber = 1;
    int blockNum = 0;
    CSVState csvState = null;
    while (blit.hasNext()) {
      List<String> block = blit.next();
      if ((blockNum % 2) == 0) {
        csvState = new CSVState(appState_, dacx_, false, setNumber++, paramNameToPdKeyMap_, 
                                useDate_, useTime_, useBatch_, useInvest_, useCondition_);
        csvList.add(csvState);
        blockNum = 0;
      }
      processLineBlock(block, blockNum++, csvState, pd);
    }
    
    TaggedTAD ttad = handleTimeDefinition();
    if (!ttad.keepGoing) {
      return (false);
    }
        
    //
    // Look for newbies and batch collisions:
    //
    
    HashMap allNewbies = new HashMap();
    HashMap newbieClosest = new HashMap();
    TreeMap batchDups = new TreeMap();
    boolean haveNP = flagNewbieParams(pd, allNewbies);
    boolean haveN = flagNewbies(csvList, pd, allNewbies, newbieClosest, batchDups, ttad.tad);
         
    if (haveNP || haveN) {
      findNewbieNeighbors(pd, allNewbies, newbieClosest);
      NewbieReportingDialog nrd = new NewbieReportingDialog(appState_, appState_.getTopFrame(), allNewbies, newbieClosest);
      nrd.setVisible(true);
      if (!nrd.keepGoing()) {
        return (false);
      }
    }
    
    if (!batchDups.isEmpty()) {
      BatchDupReportDialog bdrd = new BatchDupReportDialog(appState_, appState_.getTopFrame(), batchDups, "batchDup.CSVDialog", "batchDup.CSVTable");
      bdrd.setVisible(true);
      if (!bdrd.keepGoing()) {
        return (false);
      }
    }

    if (reportBadMeasurements(csvList, ttad.tad)) {
      return (false);
    }    

    //
    // The time for per-checks is done.  Start installing stuff in the DB:
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.pertCsv");
    if (ttad.tad != null) {
      DatabaseChange dc = dacx_.getExpDataSrc().setTimeAxisDefinition(ttad.tad);
      if (dc != null) {
        DatabaseChangeCmd dcc = new DatabaseChangeCmd(appState_, dacx_, dc);
        support.addEdit(dcc);
      }
    }
    
    paramsToDatabase(pd, support);    
    extractMeasurements(csvList, pd, support);
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();
    return (true);
  }  
  
  /***************************************************************************
  ** 
  ** Handle time definition
  */

  private TaggedTAD handleTimeDefinition() throws IOException {
  
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    //
    // If not intialized, do so now via dialog or info from the file:
    //
    TimeAxisDefinition newtad = null;
    
    if (!tad.isInitialized()) {
      Map newTS = (Map)newParamsInFile_.get(CSVState.TIME_SCALE_PARAM_UC_);
      if ((newTS == null) || (newTS.size() != 1)) {
        boolean keepGoing = TimeAxisSetupDialog.timeAxisSetupDialogWrapperWrapper(appState_, dacx_);
        if (!keepGoing) {
          return (new TaggedTAD(null, false));
        }
      } else {
        String timeVal = (String)newTS.values().iterator().next();
        int units;
        try {
          units = TimeAxisDefinition.mapUnitTypeTag(timeVal);        
        } catch (IllegalArgumentException iaex) {
          throw new IOException(buildNoLineErrorMessage("csvInput.badTimeUnits", timeVal));   
        }
        // FIX ME: Support custom stages on CSV input
        if (TimeAxisDefinition.wantsCustomUnits(units)) {
          throw new IOException(buildNoLineErrorMessage("csvInput.customTimeNotSupported", timeVal));   
        }  
        newtad = new TimeAxisDefinition(appState_);
        newtad.setDefinition(units, null, null, false, null);           
      }
    } else {
      Map newTS = (Map)newParamsInFile_.get(CSVState.TIME_SCALE_PARAM_UC_);
      if ((newTS == null) || newTS.isEmpty()) {
        return (new TaggedTAD(null, true));  // Nothing to do...
      } else {
        String timeVal = (String)newTS.values().iterator().next();
        if (newTS.size() > 1) {
          throw new IOException(buildNoLineErrorMessage("csvInput.multipleTimeDefs", timeVal));
        } 
        int units;
        try {
          units = TimeAxisDefinition.mapUnitTypeTag(timeVal);        
        } catch (IllegalArgumentException iaex) {
          throw new IOException(buildNoLineErrorMessage("csvInput.badTimeUnits", timeVal));   
        }
        // FIX ME: Support custom stages on CSV input
        if (TimeAxisDefinition.wantsCustomUnits(units)) {
          throw new IOException(buildNoLineErrorMessage("csvInput.customTimeNotSupported", timeVal));   
        }
        if (tad.getUnits() != units) {
          throw new IOException(buildNoLineErrorMessage("csvInput.timeDefMismatchWithCurrent", timeVal));
        }
      }
    }
    return (new TaggedTAD(newtad, true));
  }

  /***************************************************************************
  **
  ** report newbie parameters
  */  

  private boolean flagNewbieParams(PerturbationData pd, Map retval) { 
    boolean haveNewbie = false;
    
    MeasureDictionary md = pd.getMeasureDictionary();
    PertDictionary pdict = pd.getPertDictionary();
    ConditionDictionary cdict = pd.getConditionDictionary();
    PertAnnotations pa = pd.getPertAnnotations();
    
    //
    // Always have measure scales, so always check: 
    //
    
    HashSet newbieScales = (HashSet)retval.get(NEWBIE_SCALE);
    if (newbieScales == null) {
      newbieScales = new HashSet();
      retval.put(NEWBIE_SCALE, newbieScales);
    }
    Map scales = (Map)newParamsInFile_.get(CSVState.SCALE_PARAM_UC_);
    if (scales != null) {
      Iterator sit = scales.keySet().iterator();
      while (sit.hasNext()) {
        String skey = (String)sit.next();
        MeasureScaleParam sp = (MeasureScaleParam)scales.get(skey);
        newbieScales.add(sp.name);
        haveNewbie = true;
      }
    }
    
    if (md.getMeasurePropsCount() > 0) {
      HashSet newbieMeasProps = (HashSet)retval.get(NEWBIE_MEASURE_TYPE);
      if (newbieMeasProps == null) {
        newbieMeasProps = new HashSet();
        retval.put(NEWBIE_MEASURE_TYPE, newbieMeasProps);      
      }  
      Map measurements = (Map)newParamsInFile_.get(CSVState.MEASURE_TYPE_PARAM_UC_);      
      if (measurements != null) {
        Iterator mit = measurements.keySet().iterator();
        while (mit.hasNext()) {
          String mkey = (String)mit.next();
          MeasureParam mp = (MeasureParam)measurements.get(mkey);   
          newbieMeasProps.add(mp.name);
          haveNewbie = true;
        }
      }
    }
    
    if (pdict.getPerturbPropsCount() > 0) {
      HashSet newbiePertProps = (HashSet)retval.get(NEWBIE_PERT_TYPE);
      if (newbiePertProps == null) {
        newbiePertProps = new HashSet();
        retval.put(NEWBIE_PERT_TYPE, newbiePertProps);      
      }   
      Map pertTypes = (Map)newParamsInFile_.get(CSVState.PERT_TYPE_PARAM_UC_);      
      if (pertTypes != null) {
        Iterator pit = pertTypes.keySet().iterator();
        while (pit.hasNext()) {
          String pkey = (String)pit.next();
          PertPropParam ppp = (PertPropParam)pertTypes.get(pkey);
          newbiePertProps.add(ppp.name);
          haveNewbie = true;
        }
      }
    }  
   
    HashSet newbieExpCond = (HashSet)retval.get(NEWBIE_EXP_COND);
    if (newbieExpCond == null) {
      newbieExpCond = new HashSet();
      retval.put(NEWBIE_EXP_COND, newbieExpCond);      
    }    
    Map exprConds = (Map)newParamsInFile_.get(CSVState.CONDITION_PARAM_UC_);      
    if (exprConds != null) {
      Iterator cit = exprConds.keySet().iterator();
      while (cit.hasNext()) {
        String ekey = (String)cit.next();
        String condName = (String)exprConds.get(ekey);    
        newbieExpCond.add(condName);
        haveNewbie = true;
      }
    }

    if (cdict.getExprControlCount() > 0) {
      HashSet newbieExpControl = (HashSet)retval.get(NEWBIE_CONTROL);
      if (newbieExpControl == null) {
        newbieExpControl = new HashSet();
        retval.put(NEWBIE_CONTROL, newbieExpControl);      
      }   
      Map ctrls = (Map)newParamsInFile_.get(CSVState.CONTROL_PARAM_UC_);
      if (ctrls != null) {
        Iterator cit = ctrls.keySet().iterator();
        while (cit.hasNext()) {
          String tkey = (String)cit.next();
          String ctrlName = (String)ctrls.get(tkey);
          newbieExpControl.add(ctrlName);
          haveNewbie = true;
        }
      }
    }
    
    if (pd.getUserFieldCount() > 0) {
      HashSet newbieFields = (HashSet)retval.get(NEWBIE_USER_FIELD);
      if (newbieFields == null) {
        newbieFields = new HashSet();
        retval.put(NEWBIE_USER_FIELD, newbieFields);      
      }   
      Map uFields = (Map)newParamsInFile_.get(CSVState.USER_FIELD_PARAM_UC_);
      if (uFields != null) {
        Iterator cit = uFields.keySet().iterator();
        while (cit.hasNext()) {
          String fkey = (String)cit.next();
          String fieldName = (String)uFields.get(fkey);
          newbieFields.add(fieldName);
          haveNewbie = true;
        }
      }
    }  
    
    if (pa.getAnnotationCount() > 0) {
      HashSet newbieAnnots = (HashSet)retval.get(NEWBIE_ANNOTATION);
      if (newbieAnnots == null) {
        newbieAnnots = new HashSet();
        retval.put(NEWBIE_ANNOTATION, newbieAnnots);      
      }   
      Map annots = (Map)newParamsInFile_.get(CSVState.ANNOT_PARAM_UC_);
      if (annots != null) {
        Iterator ait = annots.keySet().iterator();
        while (ait.hasNext()) {
          String akey = (String)ait.next();
          AnnotParam aparm = (AnnotParam)annots.get(akey);
          newbieAnnots.add(aparm.num);
          haveNewbie = true;
        }
      }
    }

    return (haveNewbie);
  }
  
  /***************************************************************************
  **
  ** Look for new entities
  */
  
  private boolean flagNewbies(List cssList, PerturbationData pd, Map retval, 
                              Map closest, Map batchDups, TimeAxisDefinition pendingTAD) throws IOException { 
    boolean haveNewbie = false;
 
    Iterator cssit = cssList.iterator();
    while (cssit.hasNext()) {
      CSVState csvs = (CSVState)cssit.next();
      Iterator csvit = csvs.getValues().iterator();
      while (csvit.hasNext()) {
        CSVData csv = (CSVData)csvit.next(); 
        if (pd.getInvestigatorCount() > 0) {
          HashSet newbieInvest = (HashSet)retval.get(NEWBIE_INVEST);
          if (newbieInvest == null) {
            newbieInvest = new HashSet();
            retval.put(NEWBIE_INVEST, newbieInvest);
          }
          List invests = csv.getInvestigators();
          int numInv = invests.size();
          for (int i = 0; i < numInv; i++) {
            String invest = (String)invests.get(i);
            String investKey = pd.getInvestKeyFromName(invest);
            if (investKey == null) {
              newbieInvest.add(invest);
              haveNewbie = true;
            }
          }
        }
        
        if (pd.getTargetCount() > 0) {
          HashSet newbieTargets = (HashSet)retval.get(NEWBIE_TARGET);
          if (newbieTargets == null) {
            newbieTargets = new HashSet();
            retval.put(NEWBIE_TARGET, newbieTargets);  
          }
          Set targets = csv.getTargets();
          Iterator trit = targets.iterator();
          while (trit.hasNext()) {
            String targetKey = (String)trit.next();
            String tkey = pd.getTargetFromName(csv.getOriginalTargetName(targetKey));
            if (tkey == null) {
              newbieTargets.add(csv.getOriginalTargetName(targetKey));
              haveNewbie = true;
            }
          }
        }            

        if (pd.getPertSourceCount() > 0) {
          HashSet newbiePertSource = (HashSet)retval.get(NEWBIE_PERT_SOURCE);
          if (newbiePertSource == null) {
            newbiePertSource = new HashSet();
            retval.put(NEWBIE_PERT_SOURCE, newbiePertSource);
          }
          String multiDisp = multiMatchingSources(csv, pd);
          if (multiDisp != null) {  // multi-match forces an exit
            throw new IOException(buildNoLineErrorMessage("csvInput.multiSourceMatch", multiDisp));
          }
          List srcs = csv.getSources();
          int numSrcs = srcs.size();
          for (int i = 0; i < numSrcs; i++) {
            CSVData.ExperimentTokens etok = (CSVData.ExperimentTokens)srcs.get(i);
            String psKey = pd.getPertSourceFromName(etok.base);
            if (psKey == null) {
              newbiePertSource.add(etok.base);
              haveNewbie = true;
            }
          }
        }
        
        if (pd.getExperimentCount() > 0) {
          HashSet newbieExperiments = (HashSet)retval.get(NEWBIE_EXPERIMENT);
          if (newbieExperiments == null) {
            newbieExperiments = new HashSet();
            retval.put(NEWBIE_EXPERIMENT, newbieExperiments);  
          }
          Set matches = matchingExperiments(csv, pd, pendingTAD, true);
          int numMatch = matches.size();
          if (numMatch != 1) {  // no match or multi-match
            int time = processTime(csv, pendingTAD);
            String times = Experiment.getTimeDisplayString(appState_, new MinMax(time, time), true, true);
            String invests = DataUtil.getMultiDisplayString(csv.getInvestigators());
            ArrayList tokStrs = new ArrayList();
            List srcs = csv.getSources();
            int numSrcs = srcs.size();
            for (int i = 0; i < numSrcs; i++) {
              CSVData.ExperimentTokens etok = (CSVData.ExperimentTokens)srcs.get(i);
              tokStrs.add(etok.orig);
            }          
            String perts = DataUtil.getMultiDisplayString(tokStrs);
            String dispStr = Experiment.getDisplayString(perts, times, invests);
            if (numMatch == 0) {  // no match
              newbieExperiments.add(dispStr);
              findNewbieExperimentNeighbor(dispStr, csv, pd, pendingTAD, closest);
              haveNewbie = true;
            } else { // Multi-match
              throw new IOException(buildNoLineErrorMessage("csvInput.multiMatch", dispStr));
            }
          } else { // Single match, look for batch key collisions
            String expKey = (String)matches.iterator().next();
            Map batchKeys = pd.mergeExperimentBatchCollisions(matches);
            Set targets = csv.getTargets();
            Iterator trit = targets.iterator();
            while (trit.hasNext()) {
              String targetKey = (String)trit.next();
              String targName = csv.getOriginalTargetName(targetKey);
              String tkey = pd.getTargetFromName(targName);
              if (tkey != null) {
                Map forTarg = (Map)batchKeys.get(tkey);
                String batchID = csv.getBatchID();
                if (forTarg != null) {
                  List vals = (List)forTarg.get(batchID);
                  if (vals == null) {
                    continue;
                  }
                  String dispKey = pd.getExperiment(expKey).getDisplayString(pd);
                  TreeMap forExp = (TreeMap)batchDups.get(dispKey);
                  if (forExp == null) {
                    forExp = new TreeMap();
                    batchDups.put(dispKey, forExp);
                  }
                  // use target name for for sorting:
                  TreeMap perTarg = (TreeMap)forExp.get(targetKey);
                  if (perTarg == null) {
                    perTarg = new TreeMap();
                    forExp.put(targetKey, perTarg);
                  }  
                  BatchCollision bc = (BatchCollision)perTarg.get(batchID);
                  if (bc == null) {
                    bc = new BatchCollision(dispKey, targName, batchID);
                    perTarg.put(batchID, bc);
                    bc.vals.addAll(vals);
                  }
                  List mea = csv.getMeasurements(targetKey);
                  int numMea = mea.size();
                  for (int i = 0; i < numMea; i++) {
                    CSVData.DataPoint dp = (CSVData.DataPoint)mea.get(i);
                    bc.vals.add(dp.value);
                  }     
                }  
              }
            }
          }
        }
      }
    }
    return (haveNewbie);
  }
   
  /***************************************************************************
  **
  ** Find closest match for experiment
  */  

  private void findNewbieExperimentNeighbor(String tag, CSVData csv, PerturbationData pd, 
                                            TimeAxisDefinition pendingTAD, Map retval) { 
  
    HashMap closest = (HashMap)retval.get(NEWBIE_EXPERIMENT);
    if (closest == null) {
      closest = new HashMap();
      retval.put(NEWBIE_EXPERIMENT, closest);
    }
    TreeSet csvInv = new TreeSet(DataUtil.normalizeList(csv.getInvestigators()));    
    Set matchExp = matchingExperiments(csv, pd, pendingTAD, false);
    if (!matchExp.isEmpty()) {
      int minDist = Integer.MAX_VALUE;  
      String minKey = null;
      Iterator meit = matchExp.iterator();
      while (meit.hasNext()) {
        String key = (String)meit.next();
        Experiment exp = pd.getExperiment(key);
        SortedSet invSet = exp.getInvestigatorSortedSet(pd, true);
        int distance = DataUtil.setDistance(invSet, csvInv);
        if (distance < minDist) {
          minDist = distance;
          minKey = key;
        }
      }
      if (minKey != null) {
        Experiment exp = pd.getExperiment(minKey);
        closest.put(tag, exp.getDisplayString(pd));
      } 
    }
    return;
  }

  /***************************************************************************
  **
  ** Find closest matches
  */  

  private Map findNewbieNeighbors(PerturbationData pd, Map allNewbies, Map retval) { 
  
    MeasureDictionary md = pd.getMeasureDictionary();
    PertDictionary pdict = pd.getPertDictionary();
    ConditionDictionary cdict = pd.getConditionDictionary();

    // NEWBIE_EXPERIMENTS done previously with all available CSV data    
    // Skip NEWBIE_PERT_SOURCE; two relevant components are being located
    // Skip NEWBIE_ANNOTATIONS; basing on the tag is bogus, should base on message...
    
    closestNewbieMatch(NEWBIE_USER_FIELD, pd.getUserFieldNameSet(), allNewbies, retval, 3);
    closestNewbieMatch(NEWBIE_CONTROL, cdict.getControlNameSet(), allNewbies, retval, 3); 
    closestNewbieMatch(NEWBIE_PERT_TYPE, pdict.getPerturbPropNameSet(), allNewbies, retval, 3); 
    closestNewbieMatch(NEWBIE_SCALE, md.getMeasureScaleNameSet(), allNewbies, retval, 5); 
    closestNewbieMatch(NEWBIE_MEASURE_TYPE, md.getMeasurePropsNameSet(), allNewbies, retval, 3); 
    closestNewbieMatch(NEWBIE_EXP_COND, cdict.getConditionNameSet(), allNewbies, retval, 3);  
    closestNewbieMatch(NEWBIE_PERT_SOURCE, pd.getSourceNameSet(), allNewbies, retval, 3);  
    closestNewbieMatch(NEWBIE_INVEST, pd.getInvestigatorSet(), allNewbies, retval, 4);
    closestNewbieMatch(NEWBIE_TARGET, pd.getTargetSet(), allNewbies, retval, 3);

    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Find the closest newbie match
  */
  
  private void closestNewbieMatch(String whichNewbie, Set matchCand, Map allNewbies, Map results, int tol) {  
    HashSet newbies = (HashSet)allNewbies.get(whichNewbie);
    if ((newbies != null) && !newbies.isEmpty()) {
      HashMap closest = new HashMap();
      results.put(whichNewbie, closest);
      Iterator nskit = newbies.iterator();
      while (nskit.hasNext()) {
        String newbie = (String)nskit.next();
        closest.put(newbie, DataUtil.getClosestStringToName(newbie, tol, matchCand));
      }      
    }
    return;
  }

  /***************************************************************************
  **
  ** Get experiments that match
  */
  
  private Set matchingExperiments(CSVData csv, PerturbationData pd, TimeAxisDefinition pendingTAD, boolean withInvests) {  
 
    HashSet retval = new HashSet();
    int time = processTime(csv, pendingTAD);
    TreeSet csvInv = (withInvests) ? new TreeSet(DataUtil.normalizeList(csv.getInvestigators())) : null;
    TreeSet csvPs = new TreeSet();
    List srcs = csv.getSources();
    int numSrcs = srcs.size();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < numSrcs; i++) {
      CSVData.ExperimentTokens etok = (CSVData.ExperimentTokens)srcs.get(i);
      buf.setLength(0);
      buf.append(etok.base);
      buf.append(etok.expType);
      csvPs.add(DataUtil.normKey(buf.toString()));
    }
    String condition = csv.getCondition();

    Iterator ekit = pd.getExperimentKeys();
    while (ekit.hasNext()) {
      String expKey = (String)ekit.next();
      Experiment exp = pd.getExperiment(expKey);
      if (exp.getTime() != time) {
        continue; 
      }
      if (exp.getLegacyMaxTime() != Experiment.NO_TIME) {
        continue;
      }      
      if (!DataUtil.keysEqual(exp.getCondsDisplayString(pd), condition)) {
        continue;
      }
      
      if (withInvests) {
        SortedSet invSet = exp.getInvestigatorSortedSet(pd, true);
        if (!csvInv.equals(invSet)) {
          continue;
        }
      }
  
      SortedSet pset = exp.getPerturbsSortedSet(pd, true);
      if (!csvPs.equals(pset)) {
        continue;
      }
      retval.add(expKey);      
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get perturbation sources that match
  */
  
  private String multiMatchingSources(CSVData csv, PerturbationData pd) {  
 
    List srcs = csv.getSources();
    int numSrcs = srcs.size();
    StringBuffer buf = new StringBuffer();
    PertDictionary pDict = pd.getPertDictionary();
    for (int i = 0; i < numSrcs; i++) {
      CSVData.ExperimentTokens etok = (CSVData.ExperimentTokens)srcs.get(i);
      buf.setLength(0);
      buf.append(etok.base);
      buf.append(etok.expType);
      String checkStr = DataUtil.normKey(buf.toString());
      int matchCount = 0;
      Iterator sdit = pd.getSourceDefKeys();
      while (sdit.hasNext()) {
        String sdKey = (String)sdit.next();
        PertSource ps = pd.getSourceDef(sdKey);      
        String sn = ps.getSourceName(pd);
        PertProperties pp = ps.getExpType(pDict);
        buf.setLength(0);
        buf.append(sn);
        buf.append(pp.getType());
        String testName = DataUtil.normKey(buf.toString());
        if (checkStr.equals(testName)) {
          matchCount++;
          if (matchCount > 1) {
            return (etok.orig);
          }
        }
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Export the measurement
  */
  
  private void extractMeasurements(List cssList, PerturbationData pd, UndoSupport support) throws IOException {
    
    long timeStamp = System.currentTimeMillis();
    Iterator cssit = cssList.iterator();
    while (cssit.hasNext()) {
      CSVState csvs = (CSVState)cssit.next();
      Iterator csvit = csvs.getValues().iterator();
      while (csvit.hasNext()) {
        CSVData csv = (CSVData)csvit.next();
        ArrayList investIDs = new ArrayList();
        List invests = csv.getInvestigators();
        int numInv = invests.size();
        for (int i = 0; i < numInv; i++) {
          String invest = (String)invests.get(i);
          PerturbationData.KeyAndDataChange kdac = pd.provideInvestigator(invest);
          if (kdac.undoInfo != null) {
            support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
          }
          investIDs.add(kdac.key);
        }

        List srcs = csv.getSources();
        int numSrcs = srcs.size();
        PertSources pss = new PertSources(appState_);
        for (int i = 0; i < numSrcs; i++) {
          CSVData.ExperimentTokens etok = (CSVData.ExperimentTokens)srcs.get(i);
          PerturbationData.KeyAndDataChange kdac = pd.providePertSrcName(etok.base);
          if (kdac.undoInfo != null) {
            support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
          }
          String pertKey = csvs.getPDKey(CSVState.PERT_TYPE_PARAM_, etok.expType);
          kdac = pd.providePertSrc(kdac.key, pertKey, null, PertSource.NO_PROXY, new ArrayList(), true);
          if (kdac.undoInfo != null) {
            support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
          }
          pss.addSourceID(kdac.key);
        }
        int time = processTime(csv, null);
        String condKey = csvs.getPDKey(CSVState.CONDITION_PARAM_, csv.getCondition());
        PerturbationData.KeyAndDataChange kdac = pd.provideExperiment(pss, time, Experiment.NO_TIME, investIDs, condKey);
        if (kdac.undoInfo != null) {
          support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
        }
        String psiKey = kdac.key;
        Set targets = csv.getTargets();
        Iterator trit = targets.iterator();
        while (trit.hasNext()) {
          String targetKey = (String)trit.next();
          kdac = pd.provideTarget(csv.getOriginalTargetName(targetKey));
          String targKey = kdac.key;
          if (kdac.undoInfo != null) {
            support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
          }
          List meas = csv.getMeasurements(targetKey);
          int numM = meas.size();
          for (int i = 0; i < numM; i++) {
            CSVData.DataPoint dp = (CSVData.DataPoint)meas.get(i);
            double measv = Double.NaN;
            try {
              measv = Double.parseDouble(dp.value);
            } catch (NumberFormatException nfex) {
              throw new IllegalStateException();  // Checked previously; should not happen
            }  
            String mKey = csvs.getPDKey(CSVState.MEASURE_TYPE_PARAM_, dp.measurement);
            PertDataPoint pdp = new PertDataPoint(pd.getNextDataKey(), timeStamp, psiKey, targKey, mKey, measv);
            pdp.setBatchKey(csv.getBatchID());
            pdp.setDate(csv.getDate());
            pdp.setComment(dp.comment);
            if ((dp.control != null) && !dp.control.trim().equals("")) {
              kdac = pd.provideExpControl(dp.control);
              if (kdac.undoInfo != null) {
                support.addEdit(new PertDataChangeCmd(appState_, dacx_, kdac.undoInfo));        
              }
              pdp.setControl(kdac.key);
            }
         
            pdp.setIsSig(convertSigInput(dp.isValid));
            PertDataChange pdc = pd.setDataPoint(pdp);      
            support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));
            
            //
            // Annotations
            //
            
            if ((dp.annots != null) && !dp.annots.isEmpty()) {              
              Map aToKey = (Map)paramNameToPdKeyMap_.get(CSVState.ANNOT_PARAM_UC_);
              ArrayList keyList = new ArrayList();
              int numdpa = dp.annots.size();
              for (int j = 0; j < numdpa; j++) {
                String tag = (String)dp.annots.get(j);
                keyList.add(aToKey.get(tag));
              }           
              pdc = pd.setFootnotesForDataPoint(pdp.getID(), keyList);
              support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));
            }
           
            //
            // User Fields
            //
            
            boolean allEmpty = true;
            ArrayList userV = new ArrayList();
            int ufCount = pd.getUserFieldCount();
            for (int j = 0; j < ufCount; j++) {
              String ufName = pd.getUserFieldName(j);
              String ufVal = (String)dp.userFields.get(DataUtil.normKey(ufName));    
              if (ufVal == null) {
                ufVal = "";
              }
              ufVal = ufVal.trim();
              if (!ufVal.equals("")) {
                allEmpty = false;
              }
              userV.add(ufVal);
            }
            pdc = pd.setUserFieldValues(pdp.getID(), (allEmpty) ? null : userV);
            if (pdc != null) {
              support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));
            }
          }
        }
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** convert parameters to database entries, as needed
  */  

  void paramsToDatabase(PerturbationData pd, UndoSupport support) { 

    MeasureDictionary md = pd.getMeasureDictionary();
    PertDictionary pdict = pd.getPertDictionary();
    ConditionDictionary cdict = pd.getConditionDictionary();
    
    Map sToKey = (Map)paramNameToPdKeyMap_.get(CSVState.SCALE_PARAM_UC_);
    Map scales = (Map)newParamsInFile_.get(CSVState.SCALE_PARAM_UC_);
    if (scales != null) {
      Iterator sit = scales.keySet().iterator();
      while (sit.hasNext()) {
        String skey = (String)sit.next();
        MeasureScaleParam sp = (MeasureScaleParam)scales.get(skey);    
        String nextID = md.getNextDataKey();   
        MeasureScale newScale = new MeasureScale(nextID, sp.name, sp.conv, sp.illegal, sp.unchanged);
        PertDataChange pdc = pd.setMeasureScale(newScale);
        sToKey.put(skey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));
      }
    }
    
    Map mToKey = (Map)paramNameToPdKeyMap_.get(CSVState.MEASURE_TYPE_PARAM_UC_);
    Map measurements = (Map)newParamsInFile_.get(CSVState.MEASURE_TYPE_PARAM_UC_);      
    if (measurements != null) {
      Iterator mit = measurements.keySet().iterator();
      while (mit.hasNext()) {
        String mkey = (String)mit.next();
        MeasureParam mp = (MeasureParam)measurements.get(mkey);    
        String nextID = md.getNextDataKey();
        String scKey = (String)sToKey.get(DataUtil.normKey(mp.scaleName));
        MeasureProps mProps = new MeasureProps(nextID, mp.name, scKey, mp.negThresh, mp.posThresh);
        PertDataChange pdc = pd.setMeasureProp(mProps);
        mToKey.put(mkey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }

    Map pToKey = (Map)paramNameToPdKeyMap_.get(CSVState.PERT_TYPE_PARAM_UC_);
    Map pertTypes = (Map)newParamsInFile_.get(CSVState.PERT_TYPE_PARAM_UC_);      
    if (pertTypes != null) {
      Iterator pit = pertTypes.keySet().iterator();
      while (pit.hasNext()) {
        String pkey = (String)pit.next();
        PertPropParam ppp = (PertPropParam)pertTypes.get(pkey);
        String nextID = pdict.getNextDataKey();
        PertProperties pProps = new PertProperties(nextID, ppp.name, ppp.abbrev, ppp.linkRelation);
        PertDataChange pdc = pd.setPerturbationProp(pProps);
        pToKey.put(pkey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }
    
    Map eToKey = (Map)paramNameToPdKeyMap_.get(CSVState.CONDITION_PARAM_UC_);
    Map exprConds = (Map)newParamsInFile_.get(CSVState.CONDITION_PARAM_UC_);      
    if (exprConds != null) {
      Iterator cit = exprConds.keySet().iterator();
      while (cit.hasNext()) {
        String ekey = (String)cit.next();
        String condName = (String)exprConds.get(ekey);    
        String nextID = cdict.getNextDataKey();  
        ExperimentConditions eCond = new ExperimentConditions(nextID, condName);
        PertDataChange pdc = pd.setExperimentConditions(eCond);
        eToKey.put(ekey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }

    Map tToKey = (Map)paramNameToPdKeyMap_.get(CSVState.CONTROL_PARAM_UC_);
    Map ctrls = (Map)newParamsInFile_.get(CSVState.CONTROL_PARAM_UC_);
    if (ctrls != null) {
      Iterator cit = ctrls.keySet().iterator();
      while (cit.hasNext()) {
        String tkey = (String)cit.next();
        String ctrlName = (String)ctrls.get(tkey);
        String nextID = cdict.getNextDataKey();  
        ExperimentControl ctrl = new ExperimentControl(nextID, ctrlName);
        PertDataChange pdc = pd.setExperimentControl(ctrl);
        tToKey.put(tkey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }

    Map uToKey = (Map)paramNameToPdKeyMap_.get(CSVState.USER_FIELD_PARAM_UC_);
    Map uFields = (Map)newParamsInFile_.get(CSVState.USER_FIELD_PARAM_UC_);
    if (uFields != null) {
      Iterator cit = uFields.keySet().iterator();
      while (cit.hasNext()) {
        String fkey = (String)cit.next();
        String fieldName = (String)uFields.get(fkey);
        String nextID = Integer.toString(pd.getUserFieldCount());
        PertDataChange pdc = pd.setUserFieldName(nextID, fieldName);
        uToKey.put(fkey, nextID);
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }

    
    Map aToKey = (Map)paramNameToPdKeyMap_.get(CSVState.ANNOT_PARAM_UC_);
    Map annots = (Map)newParamsInFile_.get(CSVState.ANNOT_PARAM_UC_);
    if (annots != null) {
      Iterator ait = annots.keySet().iterator();
      while (ait.hasNext()) {
        String akey = (String)ait.next();
        AnnotParam aparm = (AnnotParam)annots.get(akey);
        PertDataChange pdc = pd.addAnnotation(aparm.num, aparm.message);
        aToKey.put(akey, pdc.annotKey);  // kinda bogus!
        support.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc));   
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Convert the new CSV standard significance string
  */
  
  public static Boolean convertSigInput(String isValid) {
    if (isValid != null) {
      isValid = isValid.trim();
      if (isValid.equalsIgnoreCase("YES") || isValid.equalsIgnoreCase("Y")) {
        return (new Boolean(true));
      } else if (!isValid.equals("")) {
        return (new Boolean(false));
      } else {
        return (null);
      }
    } else {
      return (null);
    }
  }
    
  /***************************************************************************
  ** 
  ** Report bad measurements
  */

  private boolean reportBadMeasurements(List csvList, TimeAxisDefinition pendingTAD) {
    boolean retval = false;
    Iterator cssit = csvList.iterator();
    while (cssit.hasNext()) {
      CSVState csvs = (CSVState)cssit.next();
      Iterator csvit = csvs.getValues().iterator();
      while (csvit.hasNext()) {
        CSVData csv = (CSVData)csvit.next();
        if (processTime(csv, pendingTAD) == Integer.MIN_VALUE) {
          return (true);
        }
        Set targets = csv.getTargets();
        Iterator trit = targets.iterator();
        while (trit.hasNext()) {
          String target = (String)trit.next();
          List vals = csv.getMeasurements(target);
          int vSize = vals.size();
          for (int i = 0; i < vSize; i++) {
            CSVData.DataPoint dpt = (CSVData.DataPoint)vals.get(i);
            BoundedDoubMinMax illegal = csvs.getIllegalBounds(dpt.measurement);
            if (!CSVData.isValidMeasurement(dpt.value, illegal)) {
              retval = true;
              ResourceManager rMan = appState_.getRMan();
              String desc = MessageFormat.format(rMan.getString("qpcrcsv.badMeasurement"), 
                                                 new Object[] {dpt.value});            
              int result = JOptionPane.showOptionDialog(appState_.getTopFrame(), desc,
                                                        rMan.getString("qpcrcsv.badMeasurementTitle"),
                                                        JOptionPane.DEFAULT_OPTION, 
                                                        JOptionPane.ERROR_MESSAGE, 
                                                        null, new Object[] {
                                                          rMan.getString("dialogs.skipMessages"),
                                                          rMan.getString("dialogs.ok"),
                                                        }, rMan.getString("dialogs.ok"));            
              boolean skipMessages = (result == 0);
              if (skipMessages) {
                return (retval);
              }
            }
          }     
        }  
      }
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Extract time.  Integer.MIN_VALUE if time is bad 
  */

  private int processTime(CSVData csv, TimeAxisDefinition pendingTAD) {
    TimeAxisDefinition tad = (pendingTAD != null) ? pendingTAD : dacx_.getExpDataSrc().getTimeAxisDefinition();
    Integer parsed = tad.timeStringParse(csv.getTime());
    if ((parsed == null) || (parsed.intValue() < 0)) {
      ResourceManager rMan = appState_.getRMan();
      String desc = MessageFormat.format(rMan.getString("qpcrcsv.badTimeValue"), 
                                         new Object[] {csv.getTime()});      
      JOptionPane.showMessageDialog(appState_.getTopFrame(), desc,
                                    rMan.getString("qpcrcsv.badTimeValueTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (Integer.MIN_VALUE);
    } else {
      return (parsed.intValue());
    }
  }
  
  /***************************************************************************
  ** 
  ** Get the CSV file into blocks separated by blank lines.  In the new format,
  ** two blocks will be returned FOR EACH INVESTIGATOR SET
  */

  protected List<List<String>> fileToLineBlocks(File infile) throws IOException {
    //
    // Read in the lines.  Blank lines (or lines with just commas)
    // separate different experiments,
    // so batch up the lines into experiment blocks.
    //
    List<List<String>> blockList = new ArrayList<List<String>>();
    List<String> currExp = new ArrayList<String>();   
    BufferedReader in = new BufferedReader(new FileReader(infile));
    Pattern pat = Pattern.compile("^,+$");
    Matcher commaMatch = pat.matcher("");
    String line = null;
    while ((line = in.readLine()) != null) {
      if (line.trim().equals("") || commaMatch.reset(line).matches())  {
        if (currExp.size() > 0) {
          blockList.add(currExp);
          currExp = new ArrayList<String>();
        }
      } else {
        currExp.add(line);
      }
    }
    if (currExp.size() > 0) {
      blockList.add(currExp);
    }
    in.close();
    return (blockList);
  }
 
  /***************************************************************************
  ** 
  ** Get the perturb data as a list of csvDatas
  */

  private void processLineBlock(List<String> lines, int blockNum, CSVState csvState, PerturbationData pd) throws IOException {
    
    CSVParser csvp = new CSVParser(true);
   
    int rowNum = 0;
    Iterator<String> lit = lines.iterator();
    while (lit.hasNext()) {
      String line = lit.next();
      List argList = csvp.processCSVLine(line);
      if (blockNum == 0) {
        if (rowNum == 0) {
          csvState.startTheBlock(argList);          
        } else {
          csvState.parseParameter(argList, newParamsInFile_);
        }
      } else if (blockNum == 1) {
        if (rowNum == 0) {
          csvState.gatherHeadings(argList, pd, newParamsInFile_);
        } else {
          csvState.readDataLine(argList, rowNum);
        }
      }
      rowNum++;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Error handling
  */

  String buildNoLineErrorMessage(String rStr, String tok) {
    ResourceManager rMan = appState_.getRMan();
    String errStr = rMan.getString(rStr);
    String formStr = rMan.getString("csvInput.errFormatNoLine");
    return (MessageFormat.format(formStr, new Object[] {errStr, tok}));      
  }
   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Collect up state from blocks
  */  
  
  public static class CSVState {    
    // Column headings  
    public static final String BATCH_ID     = "BatchID";
    public static final String PERT_AGENT   = "PerturbationAgent";
    public static final String MEAS_GENE    = "MeasuredGene";
    public static final String TIME         = "Time";
    public static final String DEL_DEL_CT   = "DeltaDeltaCt";
    public static final String CONTROL      = "ExpControl";
    public static final String FORCE        = "ForceSignificance";
    public static final String COMMENTS     = "Comments";
    public static final String CONDITION    = "Condition";
    public static final String MEASURE_TYPE = "MeasureType";
    public static final String MEASUREMENT  = "Measurement";
    public static final String ANNOT        = "Annot";
    
    public static final String BATCH_ID_UC    = BATCH_ID.toUpperCase();
    public static final String PERT_AGENT_UC  = PERT_AGENT.toUpperCase();
    public static final String MEAS_GENE_UC   = MEAS_GENE.toUpperCase();
    public static final String TIME_UC        = TIME.toUpperCase();
    public static final String DEL_DEL_CT_UC  = DEL_DEL_CT.toUpperCase();
    public static final String CONTROL_UC     = CONTROL.toUpperCase();
    public static final String FORCE_UC       = FORCE.toUpperCase();
    public static final String COMMENTS_UC    = COMMENTS.toUpperCase();
    public static final String CONDITION_UC   = CONDITION.toUpperCase();
    public static final String MEASURE_TYPE_UC = MEASURE_TYPE.toUpperCase();
    public static final String MEASUREMENT_UC = MEASUREMENT.toUpperCase();
    public static final String ANNOT_UC       = ANNOT.toUpperCase();
    
    // Parameters
    static final String USER_FIELD_PARAM_ = "UserField";
    static final String TIME_SCALE_PARAM_ = "TimeScale";
    static final String MEASURE_TYPE_PARAM_ = "MeasureType";
    static final String SCALE_PARAM_      = "MeasureScale";
    static final String CONTROL_PARAM_    = "Control";
    static final String CONDITION_PARAM_  = "Condition";
    static final String PERT_TYPE_PARAM_  = "PerturbationType";
    static final String DATE_PARAM_       = "Date";
    static final String ANNOT_PARAM_      = "Annot";
    
    static final String USER_FIELD_PARAM_UC_ = USER_FIELD_PARAM_.toUpperCase();
    static final String TIME_SCALE_PARAM_UC_ = TIME_SCALE_PARAM_.toUpperCase();
    static final String MEASURE_TYPE_PARAM_UC_ = MEASURE_TYPE_PARAM_.toUpperCase();
    static final String SCALE_PARAM_UC_      = SCALE_PARAM_.toUpperCase();
    static final String CONTROL_PARAM_UC_    = CONTROL_PARAM_.toUpperCase();
    static final String CONDITION_PARAM_UC_  = CONDITION_PARAM_.toUpperCase();
    static final String PERT_TYPE_PARAM_UC_  = PERT_TYPE_PARAM_.toUpperCase();
    static final String DATE_PARAM_UC_       = DATE_PARAM_.toUpperCase();
    static final String ANNOT_PARAM_UC_      = ANNOT_PARAM_.toUpperCase();   

    private int setNumber_;
    private ArrayList investigators_;
    private String date_;
    private HashMap userFieldToColMap_;
    private HashMap argToColMap_;
    private HashSet vocabulary_;
    private HashSet paramVocab_;
    private HashSet required_;
    private HashMap paramMap_;
    private Map nameToPdKey_;
    private HashMap csvMap_;
    private boolean legacy_;
    private String singleAnnot_;
    private boolean haveMultiAnnots_;
    private Set requiredUFields_;
    private boolean useDate_;
    private boolean useTime_;
    private boolean useBatch_;
    private boolean useInvest_;
    private boolean useCondition_;
    private BTState appState_;
    private DataAccessContext dacx_;
    
    CSVState(BTState appState, DataAccessContext dacx, boolean legacy, int setNumber, Map nameToPdKey, 
             boolean useDate, boolean useTime,
             boolean useBatch, boolean useInvest, 
             boolean useCondition) {
      appState_ = appState;
      dacx_ = dacx;
      setNumber_ = setNumber;
      investigators_ = new ArrayList();
      date_ = null;
      vocabulary_ = new HashSet();
      required_ = new HashSet();
      paramVocab_ = new HashSet();
      csvMap_ = new HashMap();
      paramMap_ = new HashMap();
      nameToPdKey_ = nameToPdKey;
      legacy_ = legacy;
      useDate_ = useDate;
      useTime_ = useTime;
      useBatch_ = useBatch;
      useInvest_ = useInvest;
      useCondition_ = useCondition;
      buildParamVocabulary();
    }
    
    List getValues() {
      return (new ArrayList(csvMap_.values()));
    }
        
    /***************************************************************************
    **
    ** read a line of data
    */  

    void readDataLine(List argList, int rowNum) throws IOException { 
      int batchCol = ((Integer)argToColMap_.get(BATCH_ID_UC)).intValue();
      int pertCol = ((Integer)argToColMap_.get(PERT_AGENT_UC)).intValue();
      int timeCol = ((Integer)argToColMap_.get(TIME_UC)).intValue();
      int numArgs = argList.size();
      if ((numArgs <= batchCol) || (numArgs <= pertCol) || (numArgs <= timeCol)) {
        throw new IOException(buildTokenErrorMessage("csvInput.badRow", rowNum, Integer.toString(numArgs)));
      }
      
      String time = (String)argList.get(timeCol);
      String batch = (String)argList.get(batchCol);
      List etoks = experimentParse((String)argList.get(pertCol), rowNum);
      String condition = conditionParse(argList, rowNum);
      String fullBatchID = CSVData.buildBatchKey(date_, investigators_, batch, time, condition, 
                                                 useDate_, useTime_, useBatch_, useInvest_, useCondition_);   
      String keyForLine = CSVData.buildRowKey(etoks, date_, investigators_, time, condition, fullBatchID);
  
      CSVData csvMatch = (CSVData)csvMap_.get(keyForLine);
      if (csvMatch == null) {       
        csvMatch = new CSVData(appState_, etoks, date_, investigators_, time, condition, fullBatchID);
        csvMap_.put(keyForLine, csvMatch);
      }
      CSVData.DataPoint dp = buildMeasurement(argList, rowNum);
      csvMatch.addDataPoint(dp);
      return;
    }
  
    /***************************************************************************
    **
    ** Get database key for name
    */  

    String getPDKey(String paramKey, String name) {
      Map toKey = (Map)nameToPdKey_.get(DataUtil.normKey(paramKey));
      return ((String)toKey.get(DataUtil.normKey(name)));     
    }

    /***************************************************************************
    **
    ** Augment provided parameters with existing ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    void augmentParameters(PerturbationData pd, Map newParamsInFile) throws IOException {  
      augmentScaleParameters(pd, newParamsInFile);
      augmentMeasureParameters(pd, newParamsInFile);
      augmentPertPropParameters(pd, newParamsInFile);
      augmentConditionParameters(pd, newParamsInFile);
      augmentAnnotParameters(pd, newParamsInFile);
      augmentControlParameters(pd, newParamsInFile);
      augmentUserFieldParameters(pd, newParamsInFile);
      return;
    }
        
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentScaleParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
       
      MeasureDictionary md = pd.getMeasureDictionary();
        
      HashMap scToKey = (HashMap)nameToPdKey_.get(SCALE_PARAM_UC_);
      if (scToKey == null) {
        scToKey = new HashMap();
        nameToPdKey_.put(SCALE_PARAM_UC_, scToKey);
      }
      
      Map scales = (Map)paramMap_.get(SCALE_PARAM_UC_);
      if (scales == null) {
        scales = new HashMap();
        paramMap_.put(SCALE_PARAM_UC_, scales);
      }       
      Map newScales = (Map)newParamsInFile.get(SCALE_PARAM_UC_);
        
      Iterator skit = md.getScaleKeys();
      while (skit.hasNext()) {
        String key = (String)skit.next();
        MeasureScale scale = md.getMeasureScale(key);
        MeasureScaleParam msp = new MeasureScaleParam(scale);
        String scaleName = DataUtil.normKey(scale.getName());
        MeasureScaleParam nameMatch = (MeasureScaleParam)scales.get(scaleName);
        if (nameMatch != null) {
          if (!nameMatch.equals(msp)) {
            throw new IOException(buildParamErrorMessage("csvInput.measureScaleInconsistent", nameMatch.name));
          } else {
            scToKey.put(scaleName, scale.getID());
            newScales.remove(scaleName);
          }
        } else {
          scales.put(scaleName, msp);
          scToKey.put(scaleName, scale.getID());
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentMeasureParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
 
      MeasureDictionary md = pd.getMeasureDictionary();
        
      HashMap mpToKey = (HashMap)nameToPdKey_.get(MEASURE_TYPE_PARAM_UC_);
      if (mpToKey == null) {
        mpToKey = new HashMap();
        nameToPdKey_.put(MEASURE_TYPE_PARAM_UC_, mpToKey);
      }
      
      Map mPropMap = (Map)paramMap_.get(MEASURE_TYPE_PARAM_UC_);
      if (mPropMap == null) {
        mPropMap = new HashMap();
        paramMap_.put(MEASURE_TYPE_PARAM_UC_, mPropMap);
      }     
      Map newProps = (Map)newParamsInFile.get(MEASURE_TYPE_PARAM_UC_);
 
      Iterator mpkit = md.getKeys();
      while (mpkit.hasNext()) {
        String key = (String)mpkit.next();
        MeasureProps mProps = md.getMeasureProps(key);
        MeasureParam mp = new MeasureParam(mProps, pd);
        String propName = DataUtil.normKey(mProps.getName());
        MeasureParam nameMatch = (MeasureParam)mPropMap.get(propName);
        if (nameMatch != null) {
          if (!nameMatch.equals(mp)) {
            throw new IOException(buildParamErrorMessage("csvInput.measureParamInconsistent", nameMatch.name));
          } else {
            mpToKey.put(propName, mProps.getID());
            newProps.remove(propName);
          }
        } else {
          mPropMap.put(propName, mp);
          mpToKey.put(propName, mProps.getID());
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentPertPropParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
      
      PertDictionary pDict = pd.getPertDictionary();
      
      HashMap pToKey = (HashMap)nameToPdKey_.get(PERT_TYPE_PARAM_UC_);
      if (pToKey == null) {
        pToKey = new HashMap();
        nameToPdKey_.put(PERT_TYPE_PARAM_UC_, pToKey);
      }
      
      Map pertTypes = (Map)paramMap_.get(PERT_TYPE_PARAM_UC_);
      if (pertTypes == null) {
        pertTypes = new HashMap();
        paramMap_.put(PERT_TYPE_PARAM_UC_, pertTypes);
      }   
      Map newPerts = (Map)newParamsInFile.get(PERT_TYPE_PARAM_UC_);
 
      Iterator pdkit = pDict.getKeys();
      while (pdkit.hasNext()) {
        String key = (String)pdkit.next();
        PertProperties pProps = pDict.getPerturbProps(key);
        PertPropParam ppp = new PertPropParam(pProps);
        String propName = DataUtil.normKey(pProps.getType());
        PertPropParam nameMatch = (PertPropParam)pertTypes.get(propName);
        if (nameMatch != null) {
          if (!nameMatch.equals(ppp)) {
            throw new IOException(buildParamErrorMessage("csvInput.pertPropInconsistent", nameMatch.name));
          } else {
            pToKey.put(propName, pProps.getID());
            newPerts.remove(propName);
          }
        } else {
          pertTypes.put(propName, ppp);
          pToKey.put(propName, pProps.getID());
        }
      }
      return;
    }
   
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentConditionParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
      
      ConditionDictionary cDict = pd.getConditionDictionary();
      
      HashMap eToKey = (HashMap)nameToPdKey_.get(CONDITION_PARAM_UC_);
      if (eToKey == null) {
        eToKey = new HashMap();
        nameToPdKey_.put(CONDITION_PARAM_UC_, eToKey);
      }
      
      Map condTypes = (Map)paramMap_.get(CONDITION_PARAM_UC_);
      if (condTypes == null) {
        condTypes = new HashMap();
        paramMap_.put(CONDITION_PARAM_UC_, condTypes);
      }   
      Map newCond = (Map)newParamsInFile.get(CONDITION_PARAM_UC_);
 
      Iterator pdkit = cDict.getKeys();
      while (pdkit.hasNext()) {
        String key = (String)pdkit.next();
        ExperimentConditions eCond = cDict.getExprConditions(key);   
        String condName = DataUtil.normKey(eCond.getDescription());
        String nameMatch = (String)condTypes.get(condName);
        if (nameMatch != null) {
          if (!DataUtil.keysEqual(nameMatch, condName)) {
            throw new IOException(buildParamErrorMessage("csvInput.conditionInconsistent", nameMatch));
          } else {
            eToKey.put(condName, eCond.getID());
            newCond.remove(condName);
          }
        } else {
          condTypes.put(condName, eCond.getDescription());
          eToKey.put(condName, eCond.getID());
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentAnnotParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
      
      PertAnnotations pa = pd.getPertAnnotations();
      
      HashMap aToKey = (HashMap)nameToPdKey_.get(ANNOT_PARAM_UC_);
      if (aToKey == null) {
        aToKey = new HashMap();
        nameToPdKey_.put(ANNOT_PARAM_UC_, aToKey);
      }
      
      Map annotMap = (Map)paramMap_.get(ANNOT_PARAM_UC_);
      if (annotMap == null) {
        annotMap = new HashMap();
        paramMap_.put(ANNOT_PARAM_UC_, annotMap);
      }   
      if (annotMap.size() == 1) {
        singleAnnot_ = (String)annotMap.keySet().iterator().next();
      } else if (annotMap.size() > 1) {
        haveMultiAnnots_ = true;
      }
      Map newAnnot = (Map)newParamsInFile.get(ANNOT_PARAM_UC_);
        
      SortedMap fullMap = pa.getFullMap();
      SortedMap tagToKey = pa.getFootTagToKeyMap();    
      Iterator tkit = fullMap.keySet().iterator();
      while (tkit.hasNext()) {
        String key = (String)tkit.next();
        String message = (String)fullMap.get(key);
        AnnotParam dbAnnot = new AnnotParam(key, message);
        String annotNum = DataUtil.normKey(key);
        AnnotParam nameMatch = (AnnotParam)annotMap.get(annotNum);
        if (nameMatch != null) {
          if (!nameMatch.equals(dbAnnot)) {
            throw new IOException(buildParamErrorMessage("csvInput.annotInconsistent", nameMatch.num));
          } else {
            aToKey.put(annotNum, tagToKey.get(key));
            newAnnot.remove(annotNum);
          }
        } else {
          annotMap.put(annotNum, dbAnnot);
          aToKey.put(annotNum, tagToKey.get(key));
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentControlParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
      
      ConditionDictionary cDict = pd.getConditionDictionary();
      
      HashMap ctToKey = (HashMap)nameToPdKey_.get(CONTROL_PARAM_UC_);
      if (ctToKey == null) {
        ctToKey = new HashMap();
        nameToPdKey_.put(CONTROL_PARAM_UC_, ctToKey);
      }
      
      Map ctrlMap = (Map)paramMap_.get(CONTROL_PARAM_UC_);
      if (ctrlMap == null) {
        ctrlMap = new HashMap();
        paramMap_.put(CONTROL_PARAM_UC_, ctrlMap);
      }   
      Map newCtrl = (Map)newParamsInFile.get(CONTROL_PARAM_UC_);
          
      Iterator ctkit = cDict.getControlKeys();
      while (ctkit.hasNext()) {
        String key = (String)ctkit.next();
        ExperimentControl ectrl = cDict.getExprControl(key);
        String ctrlName = DataUtil.normKey(ectrl.getDescription());
        String nameMatch = (String)ctrlMap.get(ctrlName);
        if (nameMatch != null) {
          if (!DataUtil.normKey(nameMatch).equals(ctrlName)) {
            throw new IOException(buildParamErrorMessage("csvInput.controlInconsistent", nameMatch));
          } else {
            ctToKey.put(ctrlName, ectrl.getID());
            newCtrl.remove(ctrlName);
          }
        } else {
          ctrlMap.put(ctrlName, ectrl.getDescription());
          ctToKey.put(ctrlName, ectrl.getID());
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Augment provided parameters with ones from the database, throw exception
    ** if we are not matching existing stuff
    */  

    private void augmentUserFieldParameters(PerturbationData pd, Map newParamsInFile) throws IOException { 
          
      HashMap ufToKey = (HashMap)nameToPdKey_.get(USER_FIELD_PARAM_UC_);
      if (ufToKey == null) {
        ufToKey = new HashMap();
        nameToPdKey_.put(USER_FIELD_PARAM_UC_, ufToKey);
      }
      
      Map ufMap = (Map)paramMap_.get(USER_FIELD_PARAM_UC_);
      if (ufMap == null) {
        ufMap = new HashMap();
        paramMap_.put(USER_FIELD_PARAM_UC_, ufMap);
      }   
      if ((ufMap != null) && !ufMap.isEmpty()) {
        requiredUFields_ = new HashSet(ufMap.keySet());
      } 
      Map newUF = (Map)newParamsInFile.get(USER_FIELD_PARAM_UC_);

      int numUF = pd.getUserFieldCount();
      for (int i = 0; i < numUF; i++) {
        String ufn = pd.getUserFieldName(i);
        String ufnNorm = DataUtil.normKey(ufn);
        String nameMatch = (String)ufMap.get(ufnNorm);
        if (nameMatch != null) {
          if (!DataUtil.normKey(nameMatch).equals(ufnNorm)) {
            throw new IOException(buildParamErrorMessage("csvInput.userFieldInconsistent", nameMatch));
          } else {
            ufToKey.put(ufnNorm, Integer.toString(i));
            newUF.remove(ufnNorm);
          }
        } else {
          ufMap.put(ufnNorm, ufn);
          ufToKey.put(ufnNorm, Integer.toString(i));
        }
      }
      return;
    }
    
    
    /***************************************************************************
    **
    ** Error handling
    */
      
    String buildTokenErrorMessage(String rStr, int rowNum, String tok) {
      ResourceManager rMan = appState_.getRMan();
      String errStr = rMan.getString(rStr);
      String formStr = rMan.getString("csvInput.tokErrFormat");
      return (MessageFormat.format(formStr, new Object[] {errStr, new Integer(setNumber_), 
                                                                  new Integer(rowNum),
                                                                  tok}));      
    }
    
    /***************************************************************************
    **
    ** Error handling
    */
      
    String buildParamErrorMessage(String rStr, String tok) {
      ResourceManager rMan = appState_.getRMan();
      String errStr = rMan.getString(rStr);
      String formStr = rMan.getString("csvInput.paramErrFormat");
      return (MessageFormat.format(formStr, new Object[] {errStr, new Integer(setNumber_), tok}));      
    }
    
    /***************************************************************************
    **
    ** Error handling
    */
    
    String buildHeadingErrorMessage(String rStr, String tok) {
      ResourceManager rMan = appState_.getRMan();
      String errStr = rMan.getString(rStr);
      String formStr = rMan.getString("csvInput.headingErrFormat");
      return (MessageFormat.format(formStr, new Object[] {errStr, new Integer(setNumber_), tok}));      
    }
  
    /***************************************************************************
    **
    ** Add a measurement from a line of csv perturb data
    */

    private CSVData.DataPoint buildMeasurement(List args, int rowNum) throws IOException {
      int numArgs = args.size();

      int targCol = ((Integer)argToColMap_.get(MEAS_GENE_UC)).intValue();
      String target = (String)args.get(targCol);
      if ((target == null) || target.trim().equals("")) {
        throw new IOException(buildTokenErrorMessage("csvInput.badTarget", rowNum, target));
      }

      String meaKey = (legacy_) ? DEL_DEL_CT_UC : MEASUREMENT_UC;
      int valCol = ((Integer)argToColMap_.get(meaKey)).intValue();
      String value = (String)args.get(valCol);
      if ((value == null) || value.trim().equals("")) {
        throw new IOException(buildTokenErrorMessage("csvInput.badMeasurement", rowNum, value));
      }

      int isValCol = ((Integer)argToColMap_.get(FORCE_UC)).intValue();
      String isValid = (numArgs >= (isValCol + 1)) ? (String)args.get(isValCol) : null;
      if (!sigCSVInputOK(isValid)) {
        throw new IOException(buildTokenErrorMessage("csvInput.badSignificance", rowNum, isValid));      
      }    
      if (isValid != null) {
        isValid = isValid.trim();
        if (isValid.equals("")) {
          isValid = null;
        }
      }

      int commCol = ((Integer)argToColMap_.get(COMMENTS_UC)).intValue();
      String comment = (numArgs >= (commCol + 1)) ? (String)args.get(commCol) : "";
      comment = (comment == null) ? null : comment.trim();
      
      //
      // Controls
      //
      
      String control = null;  
      Map controls = (Map)paramMap_.get(CONTROL_PARAM_UC_);
      if (controls != null) {
        if (controls.size() > 1) {       
          int contCol = ((Integer)argToColMap_.get(CONTROL_UC)).intValue();
          control = (numArgs >= (contCol + 1)) ? (String)args.get(contCol) : "";  
          if (control != null) {
            control = control.trim();
            if (!controls.keySet().contains(DataUtil.normKey(control))) {
              throw new IOException(buildTokenErrorMessage("csvInput.badControl", rowNum, control));
            }
          }
        }
      }
 
      //
      // Measurement type
      //
      
      String measurement = null;  
      Map measurements = (Map)paramMap_.get(MEASURE_TYPE_PARAM_UC_);
      if (measurements != null) {
        if (measurements.size() > 1) {       
          int measCol = ((Integer)argToColMap_.get(MEASURE_TYPE_PARAM_UC_)).intValue();
          measurement = (numArgs >= (measCol + 1)) ? (String)args.get(measCol) : "";  
          if (measurement != null) {
            measurement = measurement.trim();
            if (!measurements.keySet().contains(DataUtil.normKey(measurement))) {
              throw new IOException(buildTokenErrorMessage("csvInput.badMeasureType", rowNum, measurement));
            }
          }
        } else {
          measurement = (String)measurements.keySet().iterator().next();
        }
      }
       
      //
      // Annotations
      //
      
      ArrayList annotList = new ArrayList();
      Integer annotColObj = (Integer)argToColMap_.get(ANNOT_PARAM_UC_);
      if (annotColObj == null) {
        if (singleAnnot_ != null) {
          annotList.add(singleAnnot_);
        }
      } else {
        Map annots = (Map)paramMap_.get(ANNOT_PARAM_UC_);
        int annotCol = annotColObj.intValue();
        String annotListStr = (numArgs >= (annotCol + 1)) ? (String)args.get(annotCol) : "";  
        if (annotListStr != null) {            
          StringTokenizer strTok = new StringTokenizer(annotListStr.trim(), "+");
          while (strTok.hasMoreTokens()) {
            String tok = strTok.nextToken();
            annotList.add(tok);
            if (!annots.keySet().contains(DataUtil.normKey(tok))) {
              throw new IOException(buildTokenErrorMessage("csvInput.badAnnotType", rowNum, tok));
            }
          }
        }
      }  
     
      HashMap ufs = new HashMap(); 
      Iterator ufit = userFieldToColMap_.keySet().iterator();
      while (ufit.hasNext()) {
        String ufname = (String)ufit.next();
        int ufCol = ((Integer)userFieldToColMap_.get(ufname)).intValue();
        String ufVal = (numArgs >= (ufCol + 1)) ? (String)args.get(ufCol) : "";
        ufs.put(ufname, ufVal);
      }

      CSVData.DataPoint dp = new CSVData.DataPoint(target.trim(), value.trim(), control, isValid, comment);
      dp.measurement = measurement;
      dp.annots = annotList;
      dp.userFields.putAll(ufs);
      return (dp);
    } 
    
    /***************************************************************************
    **
    ** Check validity of the new CSV standard significance string
    */

    private boolean sigCSVInputOK(String isValid) {
      isValid = (isValid == null) ? "" : isValid.trim();    
      if (isValid.equals("") || 
          isValid.equalsIgnoreCase("YES") || 
          isValid.equalsIgnoreCase("Y") ||
          isValid.equalsIgnoreCase("NO") ||
          isValid.equalsIgnoreCase("N") ||    
          isValid.equalsIgnoreCase("NS")) {
        return (true);
      }
      return (false);
    }

    /***************************************************************************
    **
    ** Gather headings
    */  

    void gatherHeadings(List argList, PerturbationData pd, Map newParamsInFile) throws IOException {
      extractDate();
      augmentParameters(pd, newParamsInFile);
      buildVocabulary();
      parseHeadingRow(argList, pd);
      return;
    }
    
    /***************************************************************************
    **
    ** Start processing the block
    */  

    void startTheBlock(List argList) throws IOException {
      if (argList.size() < 1) {
        throw new IOException(buildParamErrorMessage("csvInput.badBlockStart", ""));
      }
      String firstInvest = (String)argList.get(0);
      if ((firstInvest == null) || (firstInvest.trim().equals(""))) {
        throw new IOException(buildParamErrorMessage("csvInput.badInvestigator", firstInvest));
      }
      int numInvest = argList.size();
      for (int i = 0; i < numInvest; i++) {
        String nextInvest = (String)argList.get(i);
        if ((nextInvest != null) && (!nextInvest.trim().equals(""))) {
          investigators_.add(nextInvest.trim());
        }
      }
      return;
    }

    /***************************************************************************
    **
    ** get the date out
    */  

    void extractDate() throws IOException { 
      HashMap dates = (HashMap)paramMap_.get(DATE_PARAM_UC_);
      if ((dates == null) || (dates.size() != 1)) {
        throw new IOException(buildParamErrorMessage("csvInput.badDateDef", ""));
      }
      date_ = (String)dates.values().iterator().next();
      if ((date_ == null) || (date_.trim().equals(""))) {
        throw new IOException(buildParamErrorMessage("csvInput.badDate", date_));
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Get illegal bounds
    */  

    BoundedDoubMinMax getIllegalBounds(String meaKey) {
      Map measures = (Map)paramMap_.get(MEASURE_TYPE_PARAM_UC_);
      MeasureParam mp = (MeasureParam)measures.get(DataUtil.normKey(meaKey));
      Map scales = (Map)paramMap_.get(SCALE_PARAM_UC_);
      MeasureScaleParam ms = (MeasureScaleParam)scales.get(DataUtil.normKey(mp.scaleName));
      return (ms.illegal);
    }
    
    /***************************************************************************
    ** 
    ** Break apart the experiment type string.
    */  

    private List experimentParse(String value, int rowNum) throws IOException {

      //
      // Extract out the experiment type:
      //
      
      ArrayList retval = new ArrayList();
      StringTokenizer strTok = new StringTokenizer(value.trim(), "&");
      while (strTok.hasMoreTokens()) {
        String tok = strTok.nextToken();
        CSVData.ExperimentTokens exptok = new CSVData.ExperimentTokens(); 
        Map pertTypes = (Map)paramMap_.get(PERT_TYPE_PARAM_UC_);
        Iterator kit = pertTypes.keySet().iterator();
        while (kit.hasNext()) {
          String ptKey = (String)kit.next();
          PertPropParam ppp = (PertPropParam)pertTypes.get(ptKey); 
          if (exptok.haveAMatch(tok, ppp.name, ppp.abbrev)) {             
            retval.add(exptok);
            break;            
          }
        }
      }
      if (retval.isEmpty()) {
        throw new IOException(buildTokenErrorMessage("csvInput.pertPropMismatch", rowNum, value));  
      }
      return (retval);    
    } 
    
    /***************************************************************************
    ** 
    ** Figure out the condition
    */  

    private String conditionParse(List args, int rowNum) throws IOException {
      int numArgs = args.size();
      String condition;  
      Map conditions = (Map)paramMap_.get(CONDITION_PARAM_UC_);
      if (conditions != null) {
        if (conditions.size() > 1) {       
          int condCol = ((Integer)argToColMap_.get(CONDITION_PARAM_UC_)).intValue();
          condition = (numArgs >= (condCol + 1)) ? (String)args.get(condCol) : "";  
          if (condition != null) {
            condition = condition.trim();
            if (!conditions.keySet().contains(DataUtil.normKey(condition))) {
              throw new IOException(buildTokenErrorMessage("csvInput.badCondition", rowNum, condition));
            }
          }
        } else {
          condition = (String)conditions.keySet().iterator().next();
        }
      } else {    
        ConditionDictionary cDict = dacx_.getExpDataSrc().getPertData().getConditionDictionary();
        condition = cDict.getExprConditions(cDict.getStandardConditionKey()).getDescription();
      }
      return (condition);
    } 
    
    /***************************************************************************
    **
    ** Figure out the ordering of the heading row
    */  

    private void parseHeadingRow(List argList, PerturbationData pd) throws IOException {
      HashSet remaining = new HashSet(vocabulary_);
      HashSet stillRequired = new HashSet(required_);

      userFieldToColMap_ = new HashMap();
      argToColMap_ = new HashMap();
      
      HashSet userFields = new HashSet();
      int numUF = pd.getUserFieldCount();
      for (int i = 0; i < numUF; i++) {
        String ufn = pd.getUserFieldName(i);
        String ufnNorm = DataUtil.normKey(ufn);
        userFields.add(ufnNorm);
      }
      
      if (requiredUFields_ != null) {
        userFields.addAll(requiredUFields_);
      }
                
      int listSize = argList.size();
      for (int i = 0; i < listSize; i++) {
        String arg = ((String)argList.get(i)).trim();
        String normArg = DataUtil.normKey(arg);
        if (!vocabulary_.contains(normArg)) {
          throw new IOException(buildHeadingErrorMessage("csvInput.badHeading", normArg));
        }
        if (!remaining.contains(normArg)) {
          throw new IOException(buildHeadingErrorMessage("csvInput.duplicateHeading", normArg));
        }
        remaining.remove(normArg);
        stillRequired.remove(normArg);
        Integer colObj = new Integer(i);
        if (userFields.contains(normArg)) {
          userFieldToColMap_.put(normArg, colObj);
        } else {
          argToColMap_.put(normArg, colObj);
        }
      }
      if (!stillRequired.isEmpty()) {
        String stillR = (String)stillRequired.iterator().next();
        throw new IOException(buildHeadingErrorMessage("csvInput.missingRequiredHeading", stillR));
      }
      return;
    }

    /***************************************************************************
    **
    ** Build the vocabulary for the heading row
    */  

    void buildVocabulary() {  

      vocabulary_.add(BATCH_ID_UC);
      required_.add(BATCH_ID_UC);

      vocabulary_.add(PERT_AGENT_UC);
      required_.add(PERT_AGENT_UC);

      vocabulary_.add(MEAS_GENE_UC);
      required_.add(MEAS_GENE_UC);

      vocabulary_.add(TIME_UC);
      required_.add(TIME_UC);

      if (legacy_) {
        vocabulary_.add(DEL_DEL_CT_UC);    
        required_.add(DEL_DEL_CT_UC);
      } else {
        vocabulary_.add(MEASUREMENT_UC);    
        required_.add(MEASUREMENT_UC); 
      }

      if (legacy_) {      
        vocabulary_.add(CONTROL_UC);
        required_.add(CONTROL_UC);
      } else {
        Map controls = (Map)paramMap_.get(CONTROL_PARAM_UC_);
        if ((controls != null) && (controls.size() > 1)) {
          vocabulary_.add(CONTROL_UC);
          required_.add(CONTROL_UC);
        }
      }
      
      Map conditions = (Map)paramMap_.get(CONDITION_PARAM_UC_);
      if ((conditions != null) && (conditions.size() > 1)) {
        vocabulary_.add(CONDITION_UC);
        required_.add(CONDITION_UC);
      }

      vocabulary_.add(FORCE_UC);
      vocabulary_.add(COMMENTS_UC);

      Map userFields = (Map)paramMap_.get(USER_FIELD_PARAM_UC_);
      if (userFields != null) {
        Iterator ufit = userFields.values().iterator();
        while (ufit.hasNext()) {
          String ufield = (String)ufit.next();
          vocabulary_.add(DataUtil.normKey(ufield));          
        }
        if (requiredUFields_ != null) {
          required_.addAll(requiredUFields_);
        }
      }

      Map measures = (Map)paramMap_.get(MEASURE_TYPE_PARAM_UC_);
      if ((measures != null) && (measures.size() > 1)) {
        vocabulary_.add(MEASURE_TYPE_UC);
        required_.add(MEASURE_TYPE_UC);
      }

      vocabulary_.add(ANNOT_UC);
      if (haveMultiAnnots_) {
        required_.add(ANNOT_UC);
      }
      return;
    }

    /***************************************************************************
    **
    ** Build the vocabulary for the parameters
    */  

    private void buildParamVocabulary() {      
      paramVocab_.add(USER_FIELD_PARAM_UC_);
      paramVocab_.add(TIME_SCALE_PARAM_UC_);
      paramVocab_.add(MEASURE_TYPE_PARAM_UC_);
      paramVocab_.add(SCALE_PARAM_UC_);
      paramVocab_.add(CONTROL_PARAM_UC_);
      paramVocab_.add(CONDITION_PARAM_UC_);
      paramVocab_.add(PERT_TYPE_PARAM_UC_);
      paramVocab_.add(DATE_PARAM_UC_);
      paramVocab_.add(ANNOT_PARAM_UC_);
      return;
    }

    /***************************************************************************
    **
    ** Parse a parameter line
    */  

    private void parseParameter(List argList, Map newParamsInFile) throws IOException {
      HashMap parms = null;
      ArrayList buildList = new ArrayList();
      String normArg = null;
      int listSize = argList.size();
      for (int i = 0; i < listSize; i++) {
        String arg = ((String)argList.get(i)).trim();
        if (i == 0) {
          normArg = DataUtil.normKey(arg);
          if (!paramVocab_.contains(normArg)) {
            throw new IOException(buildParamErrorMessage("csvInput.badParameterName", arg));
          }
          parms = (HashMap)paramMap_.get(normArg);
          if (parms == null) {
            parms = new HashMap();
            paramMap_.put(normArg, parms);
          }       
        } else {
          buildList.add(arg);
        }
      }
      buildParameter(normArg, buildList, parms, newParamsInFile);
      return;
    }

    /***************************************************************************
    **
    ** Build the parameter
    */  

    private void buildParameter(String normArg, List buildList, HashMap parms, Map newParamsInFile) throws IOException {
      if (buildList.isEmpty()) {
        throw new IOException(buildParamErrorMessage("csvInput.invalidParameterDefinition", normArg));
      }
      String key = DataUtil.normKey((String)buildList.get(0));
      
      if (normArg.equals(MEASURE_TYPE_PARAM_UC_)) {
        buildMeasureParameter(key, buildList, parms, newParamsInFile);
      } else if (normArg.equals(SCALE_PARAM_UC_)) {
        buildScaleParameter(key, buildList, parms, newParamsInFile);
      } else if (normArg.equals(ANNOT_PARAM_UC_)) {
        buildAnnotParameter(key, buildList, parms, newParamsInFile);
      } else if (normArg.equals(CONTROL_PARAM_UC_)) {
        buildSimpleNameParameter(key, buildList, parms, newParamsInFile, CONTROL_PARAM_UC_);
      } else if (normArg.equals(CONDITION_PARAM_UC_)) {
        buildSimpleNameParameter(key, buildList, parms, newParamsInFile, CONDITION_PARAM_UC_);
      } else if (normArg.equals(PERT_TYPE_PARAM_UC_)) {
        buildPertTypeParameter(key, buildList, parms, newParamsInFile);
      } else if (normArg.equals(USER_FIELD_PARAM_UC_)) {
        buildSimpleNameParameter(key, buildList, parms, newParamsInFile, USER_FIELD_PARAM_UC_);
      } else if (normArg.equals(TIME_SCALE_PARAM_UC_)) {
        buildSimpleNameParameter(key, buildList, parms, newParamsInFile, TIME_SCALE_PARAM_UC_);  
      } else if (normArg.equals(DATE_PARAM_UC_)) {
        buildSimpleNameParameter(key, buildList, parms, newParamsInFile, DATE_PARAM_UC_);  
      } else {
        throw new IOException(buildParamErrorMessage("csvInput.badParameterDef", normArg));
      }
    }
  
    /***************************************************************************
    **
    ** Build measure parameter
    */  

    private void buildMeasureParameter(String key, List buildList, HashMap parms, Map newParamsInFile) throws IOException {
      if ((buildList.size() != 4) || (parms.get(key) != null)) {
        throw new IOException(buildParamErrorMessage("csvInput.incorrectParamArgs", key));
      }
      String newMeasureName = (String)buildList.get(0);
      MeasureParam toAdd = new MeasureParam(newMeasureName, (String)buildList.get(1), 
                                            (String)buildList.get(2), (String)buildList.get(3), this);        
      HashMap newMeas = (HashMap)newParamsInFile.get(MEASURE_TYPE_PARAM_UC_);
      MeasureParam alreadySeen;
      if (newMeas == null) {
        newMeas = new HashMap();
        newParamsInFile.put(MEASURE_TYPE_PARAM_UC_, newMeas);
        alreadySeen = null;
      } else {
        alreadySeen = (MeasureParam)newMeas.get(DataUtil.normKey(newMeasureName));
      }
      if (alreadySeen != null) {
        if (!alreadySeen.equals(toAdd)) {
          throw new IOException(buildParamErrorMessage("csvInput.inconsistentParamArgs", key));
        }
      } else {
        newMeas.put(key, toAdd);
      }
      parms.put(key, toAdd);
      return;
    }
    
    /***************************************************************************
    **
    ** Build pert prop parameter
    */  

    private void buildPertTypeParameter(String key, List buildList, HashMap parms, Map newParamsInFile) throws IOException {  
      if ((buildList.size() != 3) || (parms.get(key) != null)) {
        throw new IOException(buildParamErrorMessage("csvInput.incorrectParamArgs", key));
      }
      String newPertPropName = (String)buildList.get(0);
      PertPropParam toAdd = new PertPropParam(newPertPropName, (String)buildList.get(1), (String)buildList.get(2), this);
      HashMap newPP = (HashMap)newParamsInFile.get(PERT_TYPE_PARAM_UC_);
      PertPropParam alreadySeen;
      if (newPP == null) {
        newPP = new HashMap();
        newParamsInFile.put(PERT_TYPE_PARAM_UC_, newPP);
        alreadySeen = null;
      } else {
        alreadySeen = (PertPropParam)newPP.get(key);
      }
      if (alreadySeen != null) {
        if (!alreadySeen.equals(toAdd)) {
          throw new IOException(buildParamErrorMessage("csvInput.inconsistentParamArgs", key));
        }
      } else {
        newPP.put(key, toAdd);
      }
      parms.put(key, toAdd);
      return;
    }
    
    /***************************************************************************
    **
    ** Build a single name parameter
    */  

    private void buildSimpleNameParameter(String key, List buildList, HashMap parms, Map newParamsInFile, String pKey) throws IOException {  
          
      pKey = DataUtil.normKey(pKey);
      
      if ((buildList.size() != 1) || (parms.get(key) != null)) {
        throw new IOException(buildParamErrorMessage("csvInput.incorrectParamArgs", key));
      }      
      String newSimpleName = (String)buildList.get(0);
      HashMap newCP = (HashMap)newParamsInFile.get(pKey);
      String alreadySeen;
      if (newCP == null) {
        newCP = new HashMap();
        newParamsInFile.put(pKey, newCP);
        alreadySeen = null;
      } else {
        alreadySeen = (String)newCP.get(key);
      }
      if (alreadySeen != null) {
        if (!DataUtil.normKey(alreadySeen).equals(key)) {
          throw new IOException(buildParamErrorMessage("csvInput.inconsistentParamArgs", key));
        }
      } else {
        newCP.put(key, newSimpleName);
      }
      parms.put(key, newSimpleName);
      return;
    }
    
    /***************************************************************************
    **
    ** Build the parameter
    */  

    private void buildScaleParameter(String key, List buildList, HashMap parms, Map newParamsInFile) throws IOException {
      int blSize = buildList.size();
      if ((blSize > 8) || (parms.get(key) != null)) {
        throw new IOException(buildParamErrorMessage("csvInput.incorrectParamArgs", key));
      }
      String newScaleName = (String)buildList.get(0);
      String unchangedStr = (blSize > 1) ? (String)buildList.get(1) : null;
      String convToFoldTypeTag = (blSize > 2) ? (String)buildList.get(2) : null;
      String convToFoldFacStr = (blSize > 3) ? (String)buildList.get(3) : null;
      String minIllegalStr = (blSize > 4) ? (String)buildList.get(4) : null;
      String minIncludeStr = (blSize > 5) ? (String)buildList.get(5) : null;
      String maxIllegalStr = (blSize > 6) ? (String)buildList.get(6) : null;
      String maxIncludeStr = (blSize > 7) ? (String)buildList.get(7) : null;      
  
      MeasureScaleParam toAdd = new MeasureScaleParam(newScaleName, convToFoldTypeTag, convToFoldFacStr, 
                                                      minIllegalStr, minIncludeStr, maxIllegalStr, 
                                                      maxIncludeStr, unchangedStr, this);
      HashMap newScales = (HashMap)newParamsInFile.get(SCALE_PARAM_UC_);
      MeasureScaleParam alreadySeen;
      if (newScales == null) {
        newScales = new HashMap();
        newParamsInFile.put(SCALE_PARAM_UC_, newScales);
        alreadySeen = null;
      } else {
        alreadySeen = (MeasureScaleParam)newScales.get(key);
      }
      if (alreadySeen != null) {
        if (!alreadySeen.equals(toAdd)) {
          throw new IOException(buildParamErrorMessage("csvInput.inconsistentParamArgs", key));
        }
      } else {
        newScales.put(key, toAdd);
      }
      parms.put(key, toAdd);
      return;
    }
    
    /***************************************************************************
    **
    ** Build the parameter
    */  

    private void buildAnnotParameter(String key, List buildList, HashMap parms, Map newParamsInFile) throws IOException {
      if ((buildList.size() != 2) || (parms.get(key) != null)) {
        throw new IOException(buildParamErrorMessage("csvInput.incorrectParamArgs", key));
      }
      String newAnnotTag = (String)buildList.get(0);
      AnnotParam toAdd = new AnnotParam(newAnnotTag, (String)buildList.get(1));
      HashMap newAnnots = (HashMap)newParamsInFile.get(ANNOT_PARAM_UC_);
      AnnotParam alreadySeen;
      if (newAnnots == null) {
        newAnnots = new HashMap();
        newParamsInFile.put(ANNOT_PARAM_UC_, newAnnots);
        alreadySeen = null;
      } else {
        alreadySeen = (AnnotParam)newAnnots.get(key);
      }
      if (alreadySeen != null) {
        if (!alreadySeen.equals(toAdd)) {
          throw new IOException(buildParamErrorMessage("csvInput.inconsistentParamArgs", key));
        }
      } else {
        newAnnots.put(key, toAdd);
      }
      parms.put(key, toAdd);
      return;
    }
  }

  /***************************************************************************
  ** 
  ** Annot parameter holder
  */  
  
  private static class AnnotParam {    
    String num;
    String message;
    
    AnnotParam(String num, String message) {
      this.num = num;
      this.message = message;
    }
    
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof AnnotParam)) {
        return (false);
      }
      AnnotParam otherAP = (AnnotParam)other;

      if (!DataUtil.keysEqual(this.num, otherAP.num)) {
        return (false);
      }
      
      if (!DataUtil.keysEqual(this.message, otherAP.message)) {
        return (false);
      }     
      return (true);
    }
  }
  
  /***************************************************************************
  ** 
  ** Measure Param holder
  */  
  
  private static class MeasureParam {    
    String name;
    String scaleName;
    Double negThresh;
    Double posThresh;
    
    MeasureParam(String name, String scaleName, String negThresh, String posThresh, CSVState csvState) throws IOException {
      this.name = name;
      this.scaleName = scaleName;
      try {
        this.negThresh = new Double(negThresh);
        this.posThresh = new Double(posThresh);        
      } catch (NumberFormatException nfex) {
        throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
      }
    }
    
    MeasureParam(MeasureProps props, PerturbationData pd) {
      this.name = props.getName();
      this.scaleName = pd.getMeasureDictionary().getMeasureScale(props.getScaleKey()).getName();
      this.negThresh = props.getNegThresh();
      this.posThresh = props.getPosThresh();        
    }
   
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof MeasureParam)) {
        return (false);
      }
      MeasureParam otherMP = (MeasureParam)other;

      if (!DataUtil.keysEqual(this.name, otherMP.name)) {
        return (false);
      }
      if (!DataUtil.keysEqual(this.scaleName, otherMP.scaleName)) {
        return (false);
      }
      
      if (this.negThresh == null) {
        if (otherMP.negThresh != null) {
          return (false);
        }
      } else if (!this.negThresh.equals(otherMP.negThresh)) {
        return (false);
      }
      
      if (this.posThresh == null) {
        if (otherMP.posThresh != null) {
          return (false);
        }
      } else if (!this.posThresh.equals(otherMP.posThresh)) {
        return (false);
      }
      return (true);
    }

  }
  
  /***************************************************************************
  ** 
  ** Measure Scale Param holder
  */  
  
  private static class MeasureScaleParam {    
    String name;
    MeasureScale.Conversion conv;
    BoundedDoubMinMax illegal;
    Double unchanged;
  
    
    MeasureScaleParam(String name, String convToFoldTypeTag, String convToFoldFacStr, 
                      String minIllegalStr, String minIncludeStr, 
                      String maxIllegalStr, String maxIncludeStr, 
                      String unchangedStr, CSVState csvState) throws IOException {
      
      Double convToFoldFac;
      Integer convToFoldType;
      Double minIllegal;
      Double maxIllegal;
      boolean minInclude;
      boolean maxInclude;

      this.name = name;
      try {
        convToFoldFac = ((convToFoldFacStr == null) || convToFoldFacStr.trim().equals("")) ? null : new Double(convToFoldFacStr);
        minIllegal = (minIllegalStr == null) ? null : new Double(minIllegalStr);
        maxIllegal = (maxIllegalStr == null) ? null : new Double(maxIllegalStr);
        this.unchanged = ((unchangedStr == null) || unchangedStr.trim().equals("")) ? null : new Double(unchangedStr);
      } catch (NumberFormatException nfex) {
        throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
      }
      minInclude = ((minIncludeStr == null) || minIncludeStr.trim().equals("")) ? true : Boolean.valueOf(minIncludeStr).booleanValue();
      maxInclude = ((maxIncludeStr == null) || maxIncludeStr.trim().equals("")) ? true : Boolean.valueOf(maxIncludeStr).booleanValue();
      try { 
        convToFoldType = ((convToFoldTypeTag == null) || convToFoldTypeTag.trim().equals(""))
                            ? null
                            : new Integer(MeasureScale.Conversion.mapTagToType(convToFoldTypeTag));
      } catch (IllegalArgumentException iaex) {
        throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
      }
      
      
      if ((minIllegal != null) || (maxIllegal != null)) {
        if ((minIllegal == null) || (maxIllegal == null)) {
          throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
        }
        this.illegal = new BoundedDoubMinMax(minIllegal.doubleValue(), maxIllegal.doubleValue(), minInclude, maxInclude);
      } else {
        this.illegal = null;
      }
      
      if (convToFoldType == null) {
        this.conv = null;
      } else {
        if (convToFoldType.intValue() == MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE) {
          if (convToFoldFac == null) {
            throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
          }
        }
        this.conv = new MeasureScale.Conversion(convToFoldType.intValue(), convToFoldFac);
      }
    }
    
    MeasureScaleParam(MeasureScale scale) {
      this.name = scale.getName();
      this.conv = scale.getConvToFold();
      this.illegal = scale.getIllegalRange();
      this.unchanged = scale.getUnchanged();
    }
    
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof MeasureScaleParam)) {
        return (false);
      }
      MeasureScaleParam otherMSP = (MeasureScaleParam)other;

      if (!DataUtil.keysEqual(this.name, otherMSP.name)) {
        return (false);
      }

      if (this.unchanged == null) {
        if (otherMSP.unchanged != null) {
          return (false);
        }
      } else if (!this.unchanged.equals(otherMSP.unchanged)) {
        return (false);
      }
 
      if (this.conv == null) {
        if (otherMSP.conv != null) {
          return (false);
        }
      } else if (!this.conv.equals(otherMSP.conv)) {
        return (false);
      }
      
      if (this.illegal == null) {
        if (otherMSP.illegal != null) {
          return (false);
        }
      } else if (!this.illegal.equals(otherMSP.illegal)) {
        return (false);
      }
      return (true);
    }
  }
  
  /***************************************************************************
  ** 
  ** Pert Prop Param holder
  */  
  
  private static class PertPropParam {    
    String name;
    String abbrev;
    PertDictionary.PertLinkRelation linkRelation;
    
    PertPropParam(String name, String abbrev, String pertLinkTag, CSVState csvState) throws IOException {
      this.name = name;
      if (abbrev == null) {
        this.abbrev = null;
      } else if (abbrev.trim().equals("")) {
        this.abbrev = null;
      } else {
        this.abbrev = abbrev;
      }
      try {
        this.linkRelation = PertDictionary.PertLinkRelation.fromTag(pertLinkTag);
      } catch (IllegalArgumentException iaex) {
        throw new IOException(csvState.buildParamErrorMessage("csvInput.incorrectParamArgs", name));
      }
    }
    
    PertPropParam(PertProperties pp) {
      this.name = pp.getType();
      this.abbrev = pp.getAbbrev();
      this.linkRelation = pp.getLinkSignRelationship();
    }
    
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof PertPropParam)) {
        return (false);
      }
      PertPropParam otherPPP = (PertPropParam)other;

      if (!DataUtil.keysEqual(this.name, otherPPP.name)) {
        return (false);
      }
      if (this.abbrev == null) {
        if (otherPPP.abbrev != null) {
          return (false);
        }
      } else if (!DataUtil.keysEqual(this.abbrev, otherPPP.abbrev)) {
        return (false);
      }
      return (this.linkRelation == otherPPP.linkRelation);
    }  
  }
  
  /***************************************************************************
  ** 
  ** Used for time setting result
  */  
  
  private static class TaggedTAD {    
    TimeAxisDefinition tad;
    boolean keepGoing;
 
    TaggedTAD(TimeAxisDefinition tad, boolean keepGoing) {
      this.tad = tad;
      this.keepGoing = keepGoing;    
    }
  }
  
} 
