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

package org.systemsbiology.biotapestry.perturb;

import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData.RegionRestrict;
import org.systemsbiology.biotapestry.qpcr.QpcrLegacyPublicExposed;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.DoubMinMax;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** This holds a perturbation measurement
*/

public class PertDataPoint implements Cloneable, PertFilterTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public final static int ZERO    = 0;
  public final static int PLUS    = 1;  
  public final static int MINUS   = 2; 
  public final static int UNKNOWN = 3; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String idKey_;
  private long timeStamp_;
  
  private String experimentKey_;
  private String target_;
  private double value_; 
  private LegacyPert legacyVal_;  // for old "null perturbations" with unknown # of measurements, or old "NS" value
  private String measureTypeKey_;
  private String control_;
  private Boolean isSignificant_;
  private String comments_;
  private String date_;
  
  //
  // IDS:
  //
  
 // private String plateID_; // Optional, for tying together measurements from the same plate.
  private String batchID_;
 // private int measurementGroup_; // for distinguishing/grouping multiple measurements from the same batch 
 // private double error_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.
  */

  public PertDataPoint(String key, long timeStamp, String srcInfoKey, String target, 
                       String measureTypeKey, double value) {
    idKey_ = key;
    timeStamp_ = timeStamp;
    experimentKey_ = srcInfoKey;
    target_ = target;
    value_ = value;
    measureTypeKey_ = measureTypeKey;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Publish CSV to file:
  */
  
  public boolean publishAsCSV(PrintWriter out, SourceSrc ss, Experiment prs, 
                              ConditionDictionary cdict, MeasureDictionary mdict, 
                              PertAnnotations pAn, RegionRestrict rr, String invests) {  
    
    out.print("\"");
    String bKey = getBatchKey();
    if (bKey.indexOf("_BT") == 0) {
      bKey = bKey + "::" + getTargetName(ss).toLowerCase();
    }
    out.print(bKey);
    out.print("\",\"");
    PertSources ps = prs.getSources();
    out.print(ps.getDisplayString(ss, PertSources.NO_FOOTS).replaceAll(" ", ""));
    out.print("\",\"");
    out.print(getTargetName(ss));
    out.print("\",\"");
    out.print(getTimeDisplayString(ss, true, true).replaceAll(" ", "").replaceAll("to", "-"));
    out.print("\",\"");
    if (legacyVal_ == null) {
      out.print(value_);
    } else {
     // out.print("0.0");
      out.print(legacyVal_.oldValue);
      if (legacyVal_.unknownMultiCount) {
        out.print("+");
      }
    }
   
    out.print("\",\""); 
    MeasureProps mp = mdict.getMeasureProps(measureTypeKey_);
    out.print(mp.getName());

    out.print("\",\"");    
    if (control_ != null) {
      ExperimentControl ctrl = cdict.getExprControl(control_);    
      out.print(ctrl.getDescription());
    }

    out.print("\",\"");
    if (isSignificant_ != null) {
      out.print((isSignificant_.booleanValue()) ? "Yes" : "No");
    }
    out.print("\",\"");
    if (date_ != null) {
      out.print(date_);
    }
    
    out.print("\",\"");
    if (comments_ != null) {
      out.print(comments_);
    }
    
    out.print("\",\"");
    HashSet used = new HashSet();
    getAnnotationIDs(used, ss);
    Iterator uit = used.iterator();
    while (uit.hasNext()) {
      String aid = (String)uit.next();
      out.print(pAn.getTag(aid));
      if (uit.hasNext()) {
        out.print("+");
      }
    }
    
    out.print("\",\"");
    if (invests != null) {
      out.print(invests);
    }
       
    out.print("\",\"");
    if (rr != null) {    
      out.print(rr.getDisplayValue());   
    }
 
    out.println("\"");
    return (true);
  }
 
  /***************************************************************************
  **
  ** Answer if this represents a hazardous duplication
  */
  
  public boolean willFallInSameBatchWithSameVal(PertDataPoint pdpOther) {
    if (!this.experimentKey_.equals(pdpOther.experimentKey_)) {
      return (false);
    }
    if (!this.target_.equals(pdpOther.target_)) {
      return (false);
    }
    if (!this.measureTypeKey_.equals(pdpOther.measureTypeKey_)) {
      return (false);
    }
    
    if (this.batchID_ == null) {
      if (pdpOther.batchID_ != null) {
        return (false);
      }
    } else { 
      if (!this.batchID_.equals(pdpOther.batchID_)) {
        return (false);
      }
    }
     
    if (this.control_ == null) {
      if (pdpOther.control_ != null) {
        return (false);
      }
    } else { 
      if (!this.control_.equals(pdpOther.control_)) {
        return (false);
      }
    }
    
    if (this.legacyVal_ == null) {
      if (pdpOther.legacyVal_ != null) {
        return (false);
      }
    } else { 
      if (!this.legacyVal_.equals(pdpOther.legacyVal_)) {
        return (false);
      }
    }
    
    if (this.isSignificant_ == null) {
      if (pdpOther.isSignificant_ != null) {
        return (false);
      }
    } else { 
      if (!this.isSignificant_.equals(pdpOther.isSignificant_)) {
        return (false);
      }
    }
    
    Double myNumeric = this.extractNumericValue();
    Double otherNumeric = pdpOther.extractNumericValue();
    if (myNumeric == null) {
      if (otherNumeric != null) {
        return (false);
      }
    } else { 
      if (!myNumeric.equals(otherNumeric)) {
        return (false);
      }
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Return potential link information
  */
  
  public int getLink(SourceSrc ss) {
    Double numeric = extractNumericValue();
    if (numeric == null) {
      return (PertProperties.NO_LINK);
    }
    double val = numeric.doubleValue(); 
    Experiment prs = ss.getExperiment(experimentKey_);
    DoubMinMax dmm = prs.getThresholds(measureTypeKey_, ss);
    if (!dmm.outsideOrOnBoundary(val)) {
      return (PertProperties.NO_LINK);
    }
    MeasureDictionary md = ss.getMeasureDictionary();
    MeasureProps mp = md.getMeasureProps(measureTypeKey_);
    String myScaleKey = mp.getScaleKey();  
    //
    // Convert to fold change (standard scale):
    //  
    MeasureScale myScale = md.getMeasureScale(myScaleKey); 
    Double unchanged = myScale.getUnchanged();
    return (prs.resolveLinkSign(val, unchanged, ss));
  }
  
  /***************************************************************************
  **
  ** Return if we are significant.  That is different than outside
  ** thresholds!
  */
  
  public boolean isSignificant(SourceSrc ss) {
    Experiment prs = ss.getExperiment(experimentKey_);
    DoubMinMax dmm = prs.getThresholds(measureTypeKey_, ss);   
    return (isSignificant(dmm.max, dmm.min));
  }

  /***************************************************************************
  **
  ** Return if we are above thresholds
  */
  
  public boolean aboveThresholds(SourceSrc ss) {
    Double numeric = extractNumericValue();
    if (numeric == null) {
      return (false);
    }
  
    double val = numeric.doubleValue(); 
    Experiment prs = ss.getExperiment(experimentKey_);
    DoubMinMax dmm = prs.getThresholds(measureTypeKey_, ss);
    return (dmm.outsideOrOnBoundary(val));
  }

  /***************************************************************************
  **
  ** Get the value for the point per the given parameters
  */
  
  public void getDataValuePerParams(BTState appState, Parameters params, List<ValueAndUnits> results, SourceSrc ss) {
    Double nsd = params.getNotSigDoub(appState);
    Experiment prs = ss.getExperiment(experimentKey_);
    DoubMinMax dmm = prs.getThresholds(measureTypeKey_, ss);
    if (legacyVal_ != null) {
      String val = legacyVal_.oldValue;
      if (val == null) {
        return;
      } else if (isValidForMissingData(val)) {
        return;
      } else if (isNotSignificant(val, isSignificant_, dmm.min, dmm.max)) {
        Double sVal = doScalingOfaVal(params.getConvKey(), ss, nsd.doubleValue());
        double retval = (sVal == null) ? nsd.doubleValue() : sVal.doubleValue();
        boolean isForced = (isSignificant_ != null);
        results.add(new ValueAndUnits(retval, params.getConvKey(), false, isForced, batchID_));
      }
    } else {
      boolean isForced = (isSignificant_ != null);
      Double scaled = doScaling(params.getConvKey(), ss);
      double retval = (scaled == null) ? value_ : scaled.doubleValue();
      results.add(new ValueAndUnits(retval, params.getConvKey(), isSignificant(ss), isForced, batchID_));   
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the value for the point
  */
  
  public Double extractNumericValue() {
    if (legacyVal_ != null) {
      return (null);
    } else if ((isSignificant_ != null) && !isSignificant_.booleanValue()) { 
      return (null);
    } else {
      return (new Double(value_));
    }
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public PertDataPoint clone() {
    try {
      PertDataPoint newVal = (PertDataPoint)super.clone();
      newVal.legacyVal_ = (this.legacyVal_ == null) ? null : (LegacyPert)this.legacyVal_.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
  /***************************************************************************
  **
  ** Get the ID
  **
  */
  
  public String getID() {
    return (idKey_);
  }  
  
  /***************************************************************************
  **
  ** Get the time stamp
  **
  */
  
  public long getTimeStamp() {
    return (timeStamp_);
  }  
  
  /***************************************************************************
  **
  ** Set the legacy perturbation
  **
  */
  
  public void setLegacyPert(LegacyPert legPert) {
    legacyVal_ = legPert;
    return; 
  }
  
  /***************************************************************************
  **
  ** Set the force significance
  **
  */
  
  public void setIsSig(Boolean isSig) {
    this.isSignificant_ = isSig;
    return; 
  }
  
  /***************************************************************************
  **
  ** Set the comment
  **
  */
  
  public void setComment(String comment) {
    comments_ = comment;
    return; 
  }
 
  /***************************************************************************
  **
  ** Set the control
  **
  */
  
  public void setControl(String ctrl) {
    this.control_ = ctrl;
    return; 
  } 
  
  /***************************************************************************
  **
  ** Set the batch Key
  **
  */
  
  public void setBatchKey(String batchKey) {
    batchID_ = batchKey;
    return; 
  }
  
  /***************************************************************************
  **
  ** Set the date
  **
  */
   
   public void setDate(String date) {
    date_ = date;
    return; 
  }
   
  /***************************************************************************
  **
  ** Get the legacy perturbation (may be null)
  **
  */ 
  
  public LegacyPert getLegacyPert() {
    return (legacyVal_);
  }
  
  /***************************************************************************
  **
  ** Get the batch key
  **
  */ 
  
  public String getBatchKey() {
    return (batchID_);
  }
  
  /***************************************************************************
  **
  ** Get a decorated batch key.  Optionally includes control and measurement,
  ** to differentiate multi-point batches!
  **
  */ 
  
  public String getDecoratedBatchKey(SourceSrc ss, boolean superKey) {
    StringBuffer buf = new StringBuffer();
    buf.append(experimentKey_);
    buf.append(":_:");
    buf.append(batchID_);
    if (superKey) {
      buf.append(":_:");
      buf.append((control_ == null) ? "--" : control_);
      buf.append(":_:");
      buf.append(measureTypeKey_);
    }
    return (buf.toString());
    
  }
  
  /***************************************************************************
  **
  ** Answer if our batch key is "legacy"
  */ 
  
  
  public boolean hasLegacyBatchID() {
    return ((batchID_ != null) && (batchID_.indexOf(QpcrLegacyPublicExposed.LEGACY_BATCH_PREFIX) == 0));
  }
    
  /***************************************************************************
  **
  ** Get the measurement type key
  **
  */ 
  
  public String getMeasurementTypeKey() {
    return (measureTypeKey_);
  }
  
  /***************************************************************************
  **
  ** Set the measurement type key
  **
  */ 
  
  public void setMeasurementTypeKey(String meaKey) {
    measureTypeKey_ = meaKey;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the measurement display string
  **
  */
  
  public String getMeasurementDisplayString(SourceSrc sources) {
    MeasureDictionary md = sources.getMeasureDictionary(); 
    return (md.getMeasureProps(measureTypeKey_).getDisplayString(md));
  }
   
  /***************************************************************************
  **
  ** Get the date
  **
  */ 
  
  public String getDate() {
    return (date_);
  }
  
  /***************************************************************************
  **
  ** Get the associated perturbation sources
  **
  */ 
  
  public PertSources getSources(SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs.getSources());
  }
   
  /***************************************************************************
  **
  ** Get the associated Experiment
  **
  */ 
  
  public Experiment getExperiment(SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs);
  }
  
  /***************************************************************************
  **
  ** Get the region restriction (if any)
  **
  */
  
  public PerturbationData.RegionRestrict getRegionRestriction(SourceSrc ss) {
    return (ss.getRegionRestrictionForDataPoint(idKey_)); 
  }  
 
  /***************************************************************************
  **
  ** Get the (possibly mult-pert) display string
  **
  */
  
  public String getInvestigatorDisplayString(SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs.getInvestigatorDisplayString(sources)); 
  }
  
  /***************************************************************************
  **
  ** Get the (possibly mult-pert) display string
  **
  */
  
  public String getPertDisplayString(SourceSrc sources, int footnoteMode) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs.getPertDisplayString(sources, footnoteMode)); 
  } 
  
  /***************************************************************************
  **
  ** Get the time display string
  **
  */
  
  public String getTimeDisplayString(SourceSrc sources, boolean showUnits, boolean abbreviate) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs.getTimeDisplayString(showUnits, abbreviate)); 
  }
  
  /***************************************************************************
  **
  ** Get the time range
  **
  */
  
  public MinMax getTimeRange(SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    return (prs.getTimeRange()); 
  }  
   
  /***************************************************************************
  **
  ** Get the experiment key
  **
  */
  
  public String getExperimentKey() {
    return (experimentKey_); 
  }
  
  /***************************************************************************
  **
  ** Set the experiment key
  **
  */
  
  public void setExperimentKey(String newKey) {
    experimentKey_ = newKey;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get key to the source of a single-source perturbation, null otherwise
  */
  
  public String getSingleSourceKey(SourceSrc ss) {
    Experiment prs = ss.getExperiment(experimentKey_);
    if (!prs.isSinglePerturbation()) {
      return (null);
    }
    PertSource ps = prs.getSources().getSinglePert(ss);
    return (ps.getSourceNameKey());
  }  
  
  /***************************************************************************
  **
  ** Get the candidate values for the given single filter category
  */
  
  public void getCandidates(BTState appState, int filterCat, SortedSet fillUp, SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    switch (filterCat) {
      case PertFilter.EXPERIMENT:
        prs.addToExperimentSet(fillUp, sources);
        return;
      case PertFilter.SOURCE:
        prs.addToSourceSet(fillUp, sources);
        return;
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE_OR_PROXY_NAME:
        prs.addToSourceNameSet(fillUp, sources, (filterCat == PertFilter.SOURCE_OR_PROXY_NAME));
        return;    
      case PertFilter.PERT:
        prs.addToPertSet(fillUp, sources, sources.getPertDictionary());
        return;
      case PertFilter.TARGET:
        fillUp.add(new TrueObjChoiceContent(sources.getTarget(target_), target_));
        return;
      case PertFilter.TIME:
        int min = prs.getTime();
        int legMax = prs.getLegacyMaxTime();
        int max = (legMax != Experiment.NO_TIME) ? legMax : min;
        MinMax times = new MinMax(min, max);
        fillUp.add(new TrueObjChoiceContent(prs.getTimeDisplayString(true, true), times));
        return;
      case PertFilter.INVEST:
        List invests = prs.getInvestigators();
        int numI = invests.size();
        for (int i = 0; i < numI; i++) {
          String investKey = (String)invests.get(i);
          String invest = sources.getInvestigator(investKey);
          fillUp.add(new TrueObjChoiceContent(invest, investKey));
        }
        return;
      case PertFilter.INVEST_LIST:
        String dispStr = prs.getInvestigatorDisplayString(sources);
        fillUp.add(new TrueObjChoiceContent(dispStr, dispStr));
        return;        
      case PertFilter.VALUE:
        if (aboveThresholds(sources)) {
          String above = appState.getRMan().getString("pertCand.aboveThresh");
          fillUp.add(new TrueObjChoiceContent(above, new Integer(PertFilter.ABOVE_THRESH)));
        } 
        if (isSignificant(sources)) {
          String above = appState.getRMan().getString("pertCand.significant");
          fillUp.add(new TrueObjChoiceContent(above, new Integer(PertFilter.IS_SIGNIFICANT))); 
        }
        return;
      case PertFilter.EXP_CONTROL:
      case PertFilter.EXP_CONDITION:
      case PertFilter.MEASURE_SCALE:
      case PertFilter.ANNOTATION:
      case PertFilter.MEASURE_TECH:
      default:
        throw new IllegalArgumentException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Return if our target matches one of those given
  */
  
  public boolean targetMatches(List trgs) {
    return (DataUtil.containsKey(trgs, target_));
  } 
    
  /***************************************************************************
  **
  ** Answer if we meet the single filtering criteria
  */
  
  public boolean matchesFilter(PertFilter pf, SourceSrc sources) {    
    switch (pf.getCategory()) {
      case PertFilter.SOURCE:
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE_OR_PROXY_NAME:
      case PertFilter.PERT:
      case PertFilter.TIME:
      case PertFilter.INVEST:
      case PertFilter.INVEST_LIST:
      case PertFilter.EXPERIMENT:   
      case PertFilter.EXP_CONDITION:   
        Experiment prs = sources.getExperiment(experimentKey_);
        return (prs.matchesFilter(pf, sources));
      case PertFilter.TARGET:
        String filterTarget = pf.getStringValue();
        return (filterTarget.equals(target_));
      case PertFilter.VALUE:
        if (pf.getMatchType() == PertFilter.ABOVE_THRESH) {
          return (aboveThresholds(sources));
        } else if (pf.getMatchType() == PertFilter.IS_SIGNIFICANT) {
          return (isSignificant(sources));
        }
      case PertFilter.EXP_CONTROL:
        if (pf.getMatchType() != PertFilter.STR_EQUALS) {
          throw new IllegalArgumentException();
        }
        return ((control_ != null) && control_.equals(pf.getStringValue()));
      case PertFilter.MEASURE_SCALE:
      case PertFilter.MEASURE_TECH:
        if (pf.getMatchType() != PertFilter.STR_EQUALS) {
          throw new IllegalArgumentException();
        }
        MeasureDictionary md = sources.getMeasureDictionary();
        MeasureProps mp = md.getMeasureProps(measureTypeKey_);
        return (mp.matchesFilter(pf, sources));
      case PertFilter.ANNOTATION:
        //
        // Three possible matches: this point, the target, or one of the sources.
        //
        String filterStr = pf.getStringValue();
        List annotIDs = sources.getDataPointNotes(idKey_);
        if ((annotIDs != null) && annotIDs.contains(filterStr)) {
          return (true);
        }
        annotIDs = sources.getFootnotesForTarget(target_);
        if ((annotIDs != null) && annotIDs.contains(filterStr)) {
          return (true);
        }
        prs = sources.getExperiment(experimentKey_);
        return (prs.matchesFilter(pf, sources));
      default:
        throw new IllegalArgumentException();
    }
  }    
  
  /***************************************************************************
  **
  ** Get the sign.  
  **
  */
  
  public int getSign() {
    if ((isSignificant_ != null) && !isSignificant_.booleanValue() || (legacyVal_ != null)) {
      return (ZERO);
    }
    if (value_ < 0.0) {
      return (MINUS);
    } else if (value_ == 0.0) {
      return (ZERO);
    } else {
      return (PLUS);
    }
  }
  
  /***************************************************************************
  **
  ** Get the perturbation value, for the given sign value.  May be null.
  */
  
  public Double getPerturbValue(SourceSrc sources) {
    if ((isSignificant_ != null) && !isSignificant_.booleanValue() || (legacyVal_ != null)) {
      return (null);
    }
    MeasureDictionary md = sources.getMeasureDictionary();
    String skey = md.getStandardScaleKeys()[MeasureDictionary.DDCT_INDEX]; 
    Double scaled = doScaling(skey, sources);
    return ((scaled == null) ? value_ : scaled);
  }  
  
  /***************************************************************************
  **
  ** Fill the set of annotation IDs used by this point
  */
  
  public void getAnnotationIDs(Set usedIDs, SourceSrc sources) {
    Experiment prs = sources.getExperiment(experimentKey_);
    prs.getAnnotationIDs(usedIDs, sources); 
    List dpn = sources.getDataPointNotes(idKey_);
    if (dpn != null) {
      usedIDs.addAll(dpn);
    }
    return;
  }

  /***************************************************************************
  **
  ** Get the target.
  */
  
  public String getTargetKey() {
    return (target_);
  }
  
  /***************************************************************************
  **
  ** Get the target name.
  */
  
  public String getTargetName(SourceSrc sources) {
    return (sources.getTarget(target_));
  }
 
  /***************************************************************************
  **
  ** Set the target.
  */
  
  public void setTargetKey(String targKey) {
    target_ = targKey;
    return;
  }
  
  /***************************************************************************
  **
  ** Legacy null cases where we do not know the number of data points should
  ** not be displayed in the main QPCR table
  */
  
  public boolean notForTableDisplay() {
    if (legacyVal_ == null) {
      return (false);
    }
    return (legacyVal_.unknownMultiCount);
  }
 
  /***************************************************************************
  **
  ** Get the value.
  */
  
  public double getValue() {
    if (legacyVal_ != null) {
      throw new IllegalStateException();
    }
    return (value_);
  }
  
  /***************************************************************************
  **
  ** Set the value.
  */
  
  public void setValue(double val) {
    if (legacyVal_ != null) {
      throw new IllegalStateException();
    }
    value_ = val;
    return;
  }
  
  
  /***************************************************************************
  **
  ** Get the display value in the given scale
  ** FoldChange = 1.94^DDCT.
  ** DDCT = log(FoldChange,1.94)
  */
  
  public String getScaledDisplayValue(String key, SourceSrc ss, boolean hiRes) {
    if ((key == null) || (legacyVal_ != null)) {
      return (getDisplayValue());
    }
    
    Double scaled = doScaling(key, ss);
    if (scaled == null) {
      return (Double.toString(value_));
    } else {
      return (UiUtil.doubleFormat(scaled.doubleValue(), hiRes));
    }
  } 
  
  /***************************************************************************
  **
  ** Get the numeric value in the given scale
  ** FoldChange = 1.94^DDCT.
  ** DDCT = log(FoldChange,1.94)
  */
  
  public Double getScaledNumericValue(String key, SourceSrc ss) {
    if ((key == null) || (legacyVal_ != null)) {
      return (null);
    } else if ((isSignificant_ != null) && !isSignificant_.booleanValue()) { 
      return (null);
    }
    
    Double scaled = doScaling(key, ss);
    if (scaled == null) {
      return (new Double(value_));
    } else {
      return (scaled);
    }
  }
  
  /***************************************************************************
  **
  ** Get the numeric value in the given scale
  ** >>>>>>>>>>>>> RETURNS NULL IF YOU NEED TO TAKE THE VALUE AS-IS!!!!!
  ** FoldChange = 1.94^DDCT.
  ** DDCT = log(FoldChange,1.94)
  */
  
  private Double doScaling(String key, SourceSrc ss) {  
    return (doScalingOfaVal(key, ss, value_));
  }

  /***************************************************************************
  **
  ** Get the numeric value in the given scale
  ** >>>>>>>>>>>>> RETURNS NULL IF YOU NEED TO TAKE THE VALUE AS-IS!!!!!
  ** FoldChange = 1.94^DDCT.
  ** DDCT = log(FoldChange,1.94)
  */
  
  private Double doScalingOfaVal(String key, SourceSrc ss, double value) {
    MeasureDictionary md = ss.getMeasureDictionary();
    MeasureProps mp = md.getMeasureProps(measureTypeKey_);
    String myScaleKey = mp.getScaleKey();
    if (key.equals(myScaleKey)) {
      return (null);  // just output the actual value as is...
    }
    
    String standard = md.getStandardScaleKeys()[MeasureDictionary.DEFAULT_INDEX];
    
    //
    // Convert to fold change (standard scale):
    //
    
    MeasureScale myScale = md.getMeasureScale(myScaleKey);
    MeasureScale.Conversion convertVal = myScale.getConvToFold();
    if (convertVal == null) {
      throw new IllegalArgumentException();
    }
    
    double asFold = convertVal.toFold(value);
    if (key.equals(standard)) {
      return (new Double(asFold));
    }
    
    MeasureScale destScale = md.getMeasureScale(key);
    MeasureScale.Conversion destConvertVal = destScale.getConvToFold();
    if (destConvertVal == null) {
      throw new IllegalArgumentException();
    }
    double fromFold = destConvertVal.fromFold(asFold);
    return (new Double(fromFold));   
  }
  
  /***************************************************************************
  **
  ** Get the display value.
  */
  
  public String getDisplayValue() {
    if (this.legacyVal_ != null) {
      return (legacyVal_.oldValue);
    } else {
      return (Double.toString(value_));
    }
  }  
  
  /***************************************************************************
  **
  ** Get the value.
  */
  
  public String getScaledDisplayValueOldStyle(String key, SourceSrc ss, boolean hiRes) {
    Experiment psi = ss.getExperiment(experimentKey_);
    DoubMinMax dmm = psi.getThresholds(measureTypeKey_, ss);
    if (isSignificant_ != null) {
      return ((isSignificant_.booleanValue()) ? getScaledDisplayValue(key, ss, hiRes) : "NS");
    } else if (legacyVal_ != null) {
      return (legacyVal_.oldValue);
    } else if (!dmm.outsideOrOnBoundary(value_)) {
      return ("NS");
    } else {
      return (getScaledDisplayValue(key, ss, hiRes));
    }
  } 
 
  /***************************************************************************
  **
  ** Get the significance in the context of given thresholds
  */
  
  public boolean isSignificant(double posThreshold, double negThreshold) {
    if (isSignificant_ != null) {
      return (isSignificant_.booleanValue());
    } else if (legacyVal_ != null) {
      return (false);
    } else if (value_ <= negThreshold) {
      return (true);
    } else if (value_ >= posThreshold) {
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get the significance tag.  May be null if not specified
  */
  
  public Boolean getForcedSignificance() {
    return (isSignificant_);
  } 
  
  /***************************************************************************
  **
  ** Get the significance choice
  */
  
  public TrueObjChoiceContent getForcedSignificanceChoice(BTState appState) {
    return (getSignificanceChoice(appState, isSignificant_));
  } 
  
  /***************************************************************************
  **
  ** Get the control
  */
  
  public String getControl() {
    return (control_);
  } 
  
  /***************************************************************************
  **
  ** Get the comments
  */
  
  public String getComment() {
    return (comments_);
  }   
 
  /***************************************************************************
  **
  ** Write the measurement to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind, SourceSrc src) {
    ind.indent(); 
    out.print("<dataPt id=\"");
    out.print(idKey_);
    out.print("\" trg=\"");
    out.print(CharacterEntityMapper.mapEntities(target_, false));
    out.print("\" src=\"");
    out.print(experimentKey_);    
    out.print("\" meas=\"");
    out.print(measureTypeKey_);    
    out.print("\"");
   
    if (this.legacyVal_ != null) {
      out.print(" ");
      legacyVal_.writeXMLSupport(out);
    } else {
      out.print(" val=\"");
      out.print(value_);
      out.print("\"");
    }
       
    if (date_ != null) {
      out.print(" date=\"");
      out.print(CharacterEntityMapper.mapEntities(date_, false));
      out.print("\"");
    }
    if ((control_ != null) && !control_.trim().equals("")) {
      out.print(" ctrl=\"");
      out.print(CharacterEntityMapper.mapEntities(control_, false));
      out.print("\"");
    }
    if (isSignificant_ != null) {
      out.print(" fSig=\"");
      out.print(isSignificant_);
      out.print("\"");
    }
    if ((batchID_ != null) && !batchID_.trim().equals("")) {
      out.print(" bID=\"");
      out.print(CharacterEntityMapper.mapEntities(batchID_, false));
      out.print("\"");
    } 
    if ((comments_ != null) && !comments_.trim().equals("")) {
      out.print(" cmt=\"");
      out.print(CharacterEntityMapper.mapEntities(comments_, false));
      out.print("\"");
    }
    if (timeStamp_ != 0L) {
      out.print(" ts=\"");
      out.print(timeStamp_);
      out.print("\"");
    }
    
    List notes = src.getDataPointNotes(idKey_);
    if ((notes != null) && !notes.isEmpty()) {
      String notesStr = Splitter.tokenJoin(notes, ",");
      out.print(" notes=\"");
      out.print(notesStr);
      out.print("\"");    
    }
    
    out.println("/>");    
    return;
  }
  
  /***************************************************************************
  **
  ** Write the data point as a single string
  **
  */
  
  public String displayString(double posThreshold, double negThreshold, SourceSrc ss) {  
    StringBuffer buf = new StringBuffer();
    if (!isSignificant(posThreshold, negThreshold)) { 
      buf.append("NS");
    }
    if (value_ > 0.0) {
      buf.append("+");
    }
    buf.append(value_);
    List annots = ss.getDataPointNotes(idKey_);
    if (annots != null) {
      buf.append(" [");
      String fns = ss.getFootnoteListAsString(annots);
      buf.append(fns);
      buf.append("]");
    }
    return (buf.toString());
  }
   
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PertDataPoint: value = " + value_ + " target = " + target_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
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
  ** Check validity of the new CSV standard significance string
  */
  
  public static boolean sigCSVInputOK(String isValid) {
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
  ** For XML I/O
  */ 
  
  public static class AugPertDataPoint {
    public PertDataPoint pdp;
    public List<String> notes;
    
    public AugPertDataPoint(PertDataPoint pdp, List<String> notes) {
      this.pdp = pdp;
      this.notes = notes;
    }
  }

  public static class PertDataPointWorker extends AbstractFactoryClient {
    
    public PertDataPointWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("dataPt");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("dataPt")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.augPertDataPt = buildFromXML(elemName, attrs);
        retval = board.augPertDataPt;
      }
      return (retval);     
    }  
        
    private AugPertDataPoint buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "id", true);
      
      String targ = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "trg", true);
      targ = CharacterEntityMapper.unmapEntities(targ, false);
      
      String src = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "src", true);
      
      LegacyPert lp = LegacyPert.buildFromXMLSupport(elemName, attrs); 
      
      String meas = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "meas", true);
           
      String valStr = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "val", false);
      
      if ((lp != null) && (valStr != null)) {
        throw new IOException();
      }     
      double vNum = 0.0;
      if (valStr != null) {
        try {
          vNum = Double.parseDouble(valStr); 
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      }
      
      String date = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "date", false);
      if (date != null) {
        date = CharacterEntityMapper.unmapEntities(date, false); 
      }
      
      String ctrl = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "ctrl", false);
      if (ctrl != null) {
        ctrl = CharacterEntityMapper.unmapEntities(ctrl, false);
      }
      
      String sigStr = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "fSig", false);
      Boolean isSig = null;
      if (sigStr != null) {
        isSig = Boolean.valueOf(sigStr);
      }
      if ((lp != null) && (isSig != null)) {
        throw new IOException();
      }     
     
      String batchID = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "bID", false);
      if (batchID != null) {
        batchID = CharacterEntityMapper.unmapEntities(batchID, false);
      }
      
      String comment = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "cmt", false);
      if (comment != null) {
        comment = CharacterEntityMapper.unmapEntities(comment, false);
      }
      
      String tsStr = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "ts", false);
      long timeStamp = 0L;
      if (tsStr != null) {
        try {
          timeStamp = Long.parseLong(tsStr); 
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      }
      
      String noteStr = AttributeExtractor.extractAttribute(elemName, attrs, "dataPt", "notes", false);
      List<String> noteList = null;
      if (noteStr != null) {
        noteList = Splitter.stringBreak(noteStr, ",", 0, false);
      }
  
      PertDataPoint pdp = new PertDataPoint(id, timeStamp, src, targ, meas, vNum);
      if (lp != null) {
        pdp.setLegacyPert(lp);
        pdp.setIsSig(null);
      } else {
        if (isSig != null) {
          pdp.setIsSig(isSig);
        }
      }
        
      pdp.setBatchKey(batchID);
      pdp.setDate(date);
      pdp.setComment(comment);
      pdp.setControl(ctrl); 
      return (new AugPertDataPoint(pdp, noteList));
    } 
  }
  
  /***************************************************************************
  **
  ** Answer if the measurement represents missing data.  Ditch me.
  */
  
  public static boolean isValidForMissingData(String val) {
    if (val == null) {
      return (false);
    }
    val = val.trim();
    int len = val.length();
    for (int i = 0; i < len; i++) {
      if (val.charAt(i) != '-') {
        return (false);
      }
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Answers if a numeric value is below the established threshold.  This
  ** makes a numeric entry absolutely equivalent to an NS setting.  Note:
  ** New semantics means that a value can be above threshold but still NS
  ** if the sig tag is non-null and false.
  **
  */
  
  public static boolean isBelowThreshold(String val, Boolean sigTag, double negThresh, double posThresh) {
    if (val == null) {
      return (false);
    }
    String trim = val.trim();
    if (trim.equalsIgnoreCase("NS")) {
      return (true);
    } 
    if (isValidForMissingData(trim)) {
      return (false);
    }
    
    //
    // Numbers above the threshold can be insignificant if not
    // tagged.  Numbers below can be sig if tagged.  If sig is null, we 
    // have a case where the threshold is all that matters.
    //
    
    if (sigTag != null) {
      return (!sigTag.booleanValue());
    }
    
    double doubVal = 0.0;
    try {
      doubVal = Double.parseDouble(trim);
    } catch (NumberFormatException nfe) {
      throw new IllegalStateException(); 
    }
    if (doubVal < 0.0) {
      return (doubVal > negThresh);
    } else { 
      return (doubVal < posThresh);
    }
  }    

  /***************************************************************************
  **
  ** Answer if the measurement represents "not significant"
  */
  
  public static boolean isNotSignificant(String val, Boolean sigTag, double negThresh, double posThresh) {
    return (isBelowThreshold(val, sigTag, negThresh, posThresh));
  }  
  
  /***************************************************************************
  **
  ** Get the choices for significance
  */
  
  public static Vector<TrueObjChoiceContent> getSignificanceOptions(BTState appState) {
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
    retval.add(getSignificanceChoice(appState, null));
    retval.add(getSignificanceChoice(appState, new Boolean(false)));
    retval.add(getSignificanceChoice(appState, new Boolean(true)));
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the choice for significance
  */
  
  public static TrueObjChoiceContent getSignificanceChoice(BTState appState, Boolean sig) {
    ResourceManager rMan = appState.getRMan();
    if (sig == null) {
      return (new TrueObjChoiceContent(rMan.getString("pertData.sigNotSet"), null));
    } else if (!sig.booleanValue()) {
      return (new TrueObjChoiceContent(rMan.getString("pertData.notSig"), new Boolean(false)));
    } else {
      return(new TrueObjChoiceContent(rMan.getString("pertData.isSig"), new Boolean(true)));
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used to specify data acquisition
  */
  
  public static class Parameters {
    private String convKey_;

    public Parameters(BTState appState) {
      MeasureDictionary md = appState.getDB().getPertData().getMeasureDictionary();
      // All data goes out as DDCT!!!
      convKey_ = md.getStandardScaleKeys()[MeasureDictionary.DDCT_INDEX];     
    }
    
    public Double getNotSigDoub(BTState appState) {
      MeasureDictionary md = appState.getDB().getPertData().getMeasureDictionary();
      return (md.getMeasureScale(convKey_).getUnchanged());
    }
    
    public String getConvKey() {
      return (convKey_);
    }    
  }
  
  /***************************************************************************
  **
  ** Value needs scale info 
  */ 
    
  public static class ValueAndUnits {

    private double val_;
    private String scaleKey_;
    private boolean sigPerPertThresh_;
    private boolean sigWasForced_;
    private String batchID_;
     
    public ValueAndUnits(double val, String scaleKey, boolean sigPerPertThresh, boolean wasForced, String batchID) {
      val_ = val;
      scaleKey_ = scaleKey;
      sigPerPertThresh_ = sigPerPertThresh;
      sigWasForced_ = wasForced;
      batchID_ = batchID;
    }
    
    public String getScaleKey() {
      return (scaleKey_);
    }
    
    public double getValue() {
      return (val_);
    }
    
    public boolean getIsSignificant() {
      return (sigPerPertThresh_);
    }
    
    public boolean getIsForced() {
      return (sigWasForced_);
    }
    
    public String getBatchID() {
      return (batchID_);
    }
      
    public static List<Double> convertToFlattenedDoubles(List<ValueAndUnits> aList) {
      ArrayList<Double> retval = new ArrayList<Double>();
      int numA = aList.size();
      for (int i = 0; i < numA; i++) {
        retval.add(new Double(aList.get(i).val_));    
      }
      return (retval);
    }
    
    public static List<List<Double>> convertToBatchedDoubles(List<ValueAndUnits> aList) {
      HashMap<String, List<Double>> batToDoubles = new HashMap<String, List<Double>>();    
      int numA = aList.size();
      for (int i = 0; i < numA; i++) {
        ValueAndUnits vau = aList.get(i);
        List<Double> forBat = batToDoubles.get(vau.batchID_);
        if (forBat == null) {
          forBat = new ArrayList<Double>();
          batToDoubles.put(vau.batchID_, forBat);
        }
        forBat.add(new Double(vau.val_));
      }
      return (new ArrayList<List<Double>>(batToDoubles.values()));
    }
    
    public static List<Boolean> convertToFlattenedBooleans(List<ValueAndUnits> aList) {
      ArrayList<Boolean> retval = new ArrayList<Boolean>();
      int numA = aList.size();
      for (int i = 0; i < numA; i++) {
        ValueAndUnits vau = aList.get(i);       
        retval.add(new Boolean(vau.sigPerPertThresh_));    
      }
      return (retval);
    }
    
    public static List<List<Boolean>> convertToBatchedBooleans(List<ValueAndUnits> aList) {
      HashMap<String, List<Boolean>> batToBools = new HashMap<String, List<Boolean>>();    
      int numA = aList.size();
      for (int i = 0; i < numA; i++) {
        ValueAndUnits vau = aList.get(i);
        List<Boolean> forBat = batToBools.get(vau.batchID_);
        if (forBat == null) {
          forBat = new ArrayList<Boolean>();
          batToBools.put(vau.batchID_, forBat);
        }
        forBat.add(new Boolean(vau.sigPerPertThresh_));
      }
      return (new ArrayList<List<Boolean>>(batToBools.values()));
    }
      
    private static List<List<ValueAndUnits>> convertToBatchedVAUs(List<ValueAndUnits> aList) {
      HashMap<String, List<ValueAndUnits>> batToDoubles = new HashMap<String, List<ValueAndUnits>>();    
      int numA = aList.size();
      for (int i = 0; i < numA; i++) {
        ValueAndUnits vau = aList.get(i);
        List<ValueAndUnits> forBat = batToDoubles.get(vau.batchID_);
        if (forBat == null) {
          forBat = new ArrayList<ValueAndUnits>();
          batToDoubles.put(vau.batchID_, forBat);
        }
        forBat.add(vau);
      }
      return (new ArrayList<List<ValueAndUnits>>(batToDoubles.values()));
    }
    
    //
    // Convert batched VaUs to batched doubles, but respecting the forcing by using fill-in vals:
    //
    
    public static List<List<Double>> convertToBatchedDoublesWithSig(List<ValueAndUnits> aList, Double unch, double neg, double pos) {
      List<List<ValueAndUnits>> bList = convertToBatchedVAUs(aList);
      ArrayList<List<Double>> retval = new ArrayList<List<Double>>();
      int numB = bList.size();
      for (int i = 0; i < numB; i++) {        
        List<ValueAndUnits> vauList = bList.get(i);
        ArrayList<Double> batList = new ArrayList<Double>();
        retval.add(batList);
        int numV = vauList.size();
        for (int j = 0; j < numV; j++) {        
          ValueAndUnits vau = vauList.get(j);
          Double doub = convertVAUWithSig(vau, unch, neg, pos);
          batList.add(doub);
        }
      }
      return (retval);
    }
  
    //
    // Convert to double, but respecting the forcing by using fill-in vals:
    //
    
    public static Double convertVAUWithSig(ValueAndUnits vau, Double unch, double neg, double pos) {
      if (!vau.sigWasForced_) {
        return (new Double(vau.val_));    
      } else {
        if (!vau.sigPerPertThresh_) {
          return (unch);     
        } else if ((vau.val_ <= neg) || (vau.val_ >= pos)) { // force sig and has outside thresh value
          return (new Double(vau.val_));    
        } else {  // forced but under thresh, set it to thresh while keeping sign
          double negDiff = vau.val_ - neg;
          double posDiff = pos - vau.val_;
          return (new Double((negDiff < posDiff) ? neg : pos));
        }
      }
    }
  }
}
