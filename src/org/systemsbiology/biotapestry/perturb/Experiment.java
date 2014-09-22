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

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.SortedSet;
import java.util.TreeSet;
import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.DoubMinMax;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** This holds a set of results for a specific type of perturbation
*/

public class Experiment implements Cloneable, PertFilterTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public final static int NO_TIME = -1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String id_;
  private PertSources sources_;
  private int time_;
  private String conditionKey_;
  private int legacyMaxTime_; // Used for legacy data with no specific time point
  private ArrayList invest_;
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

  public Experiment(BTState appState, String id, PertSources sources, int time, List investigators, String condKey) {
    appState_ = appState;
    id_ = id;
    sources_ = sources;
    time_ = time;
    legacyMaxTime_ = NO_TIME;
    conditionKey_ = condKey;
    invest_ = (investigators == null) ? new ArrayList() : new ArrayList(investigators);
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public Experiment(BTState appState, String id, int time, int legacyMaxTime, String condKey) {
    appState_ = appState;
    id_ = id;
    sources_ = new PertSources(appState_);
    time_ = time;
    legacyMaxTime_= legacyMaxTime;
    conditionKey_ = condKey;
    invest_ = new ArrayList();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the sign relationship of the perturbation (e.g. SAME_SIGN, OPPOSITE_SIGN)
  */
  
  public PertDictionary.PertLinkRelation getSign(SourceSrc ss) {
    if (!sources_.isSinglePert()) {
      return (PertDictionary.PertLinkRelation.PERT_LINK_NO_RELATION);
    }
    PertSource src = sources_.getSource(0, ss);
    return (src.getSign(ss));
  }
  
  /***************************************************************************
  **
  ** Get the link sign based on the value.  DOES NOT CHECK IF VALUE IS SIGNIFICANT!!
  */
  
  public int resolveLinkSign(double value, Double unchanged, SourceSrc ss) {
    if (!sources_.isSinglePert() || (unchanged == null)) {
      return (PertProperties.UNSIGNED_LINK);
    }
    PertSource src = sources_.getSource(0, ss);
    PertDictionary pDict = ss.getPertDictionary();
    PertProperties pprops = src.getExpType(pDict);
    if (src.isAProxy()) {
      return (pprops.resolveWithProxy(src.getProxySign(), value, unchanged.doubleValue()));
    } else {
      return (pprops.resolve(value, unchanged.doubleValue()));
    }
  }
 
  /***************************************************************************
  **
  ** Get the significance thresholds for the perturbation
  */
  
  public DoubMinMax getThresholds(String measureTypeKey, SourceSrc ss) {
    //
    // Actually should be a combination of the perturbation type as well
    // as measurement technology!
    //    
    MeasureDictionary mDict = ss.getMeasureDictionary();
    MeasureProps mProps = mDict.getMeasureProps(measureTypeKey);
    DoubMinMax retval = new DoubMinMax(mProps.getNegThresh().doubleValue(), mProps.getPosThresh().doubleValue());
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the experimental condition
  */
  
  public String getConditionKey() {
    return (conditionKey_);
  }
   
  /***************************************************************************
  **
  ** Set the experimental condition
  */
  
  public void setConditionKey(String coKey) {
    conditionKey_ = coKey;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the sources
  */
  
  public void setSources(PertSources pss) {
    sources_ = pss;
    return;
  }
  
  /***************************************************************************
  **
  ** Fill in the experiment choice to the set
  */
  
  public void addToExperimentSet(Set sourceInfos, SourceSrc ss) {
    sourceInfos.add(getChoiceContent(ss));
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the choice content
  */
  
  public TrueObjChoiceContent getChoiceContent(SourceSrc ss) {
    String ds = getDisplayString(ss);    
    return (new TrueObjChoiceContent(ds, id_));
  }  
  
  /***************************************************************************
  **
  ** Get the display string
  */
  
  public String getDisplayString(SourceSrc ss) {
    String perts = getPertDisplayString(ss, PertSources.BRACKET_FOOTS);
    String times = getTimeDisplayString(true, true);
    String invest = getInvestigatorDisplayString(ss);
    String ds = getDisplayString(perts, times, invest);    
    return (ds);
  }  
  
  /***************************************************************************
  **
  ** Fill in the sources; sources is a set of PertSources
  */
  
  public void addToSourceSet(Set sources, SourceSrc ss) {
    sources_.addToSourceSet(sources, ss);
    return;
  }  
    
  /***************************************************************************
  **
  ** Fill in the source (an opt. proxy) name keys
  */
  
  public void addToSourceNameSet(Set sources, SourceSrc ss, boolean orProxy) {
    sources_.addToSourceNameSet(sources, ss, orProxy);
    return;
  }  
 
  /***************************************************************************
  **
  ** Fill in the perturbation types
  */
  
  public void addToPertSet(Set pertTypes, SourceSrc ss, PertDictionary pDict) {
    sources_.addToPertSet(pertTypes, ss, pDict);
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the sources
  */
  
  public PertSources getSources() {
    return (sources_);
  }  
    
  /***************************************************************************
  **
  ** Delete a source
  */
  
  public void deleteSource(int i) {
    sources_.deleteSource(i);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a source
  */
  
  public void addSource(PertSource ps) {
    sources_.addSource(ps);
    return;
  }
  
  /***************************************************************************
  **
  ** Get a source
  */
  
  public PertSource getSource(int i, SourceSrc ss) {
    return (sources_.getSource(i, ss));
  }
  
  /***************************************************************************
  **
  ** set the ID
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  public Experiment clone() {
    try {
      Experiment newVal = (Experiment)super.clone();     
      newVal.sources_ = (PertSources)this.sources_.clone();
      // List of immutable strings; just clone:
      newVal.invest_ = (ArrayList)this.invest_.clone();      
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
  /***************************************************************************
  **
  ** Standard hashCode
  **
  */
  
  public int hashCode() {  
    return (id_.hashCode() + sources_.hashCode() + time_ + legacyMaxTime_ + invest_.hashCode());
  }
  
  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) {
    if (!equalsMinusID(other)) {
      return (false);
    }
    Experiment otherPI = (Experiment)other;
    return (this.id_.equals(otherPI.id_));
  }
  
  /***************************************************************************
  **
  ** Equals without ID:
  **
  */
  
  public boolean equalsMinusID(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof Experiment)) {
      return (false);
    }
    Experiment otherPI = (Experiment)other;
 
    if (this.time_ != otherPI.time_) {    
      return (false);
    }
    if (this.legacyMaxTime_ != otherPI.legacyMaxTime_) {
      return (false);
    }
    if (!this.sources_.equals(otherPI.sources_)) {
      return (false);
    }  
    return (this.invest_.equals(otherPI.invest_));
  }
 
  /***************************************************************************
  **
  ** Answer if we meet the single filtering criteria
  */
  
  public boolean matchesFilter(PertFilter pf, SourceSrc ss) {
    switch (pf.getCategory()) {
      case PertFilter.EXPERIMENT:
        return (pf.getStringValue().equals(id_));
      case PertFilter.EXP_CONDITION:
        return (pf.getStringValue().equals(conditionKey_));
      case PertFilter.SOURCE:
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE_OR_PROXY_NAME:
      case PertFilter.ANNOTATION:     
      case PertFilter.PERT:
        return (sources_.matchesFilter(pf, ss));
      case PertFilter.TARGET:
         // We do not care about this question:
        return (true);
      case PertFilter.TIME:
        MinMax filterTimes = pf.getIntRangeValue();
        return (timeRangeMatches(pf.getMatchType(), filterTimes));
      case PertFilter.INVEST:
        String filterInvest = pf.getStringValue();
        int numI = invest_.size();
        for (int i = 0; i < numI; i++) {
          String investKey = (String)invest_.get(i);
          if (filterInvest.equals(investKey)) {
            return (true);
          }
        }
        return (false);
      case PertFilter.INVEST_LIST:
        filterInvest = pf.getStringValue();
        return (filterInvest.equals(getInvestigatorDisplayString(ss)));
      case PertFilter.VALUE:
      case PertFilter.EXP_CONTROL: 
      case PertFilter.MEASURE_SCALE:
      
      case PertFilter.MEASURE_TECH:       
        // We do not care about this question:
        return (true);
      default:
        throw new IllegalArgumentException();
    }
  } 
  
  /***************************************************************************
  **
  ** Asks if ranges match
  **
  */
  
  public boolean timeRangeMatches(int matchType, MinMax filterTimes) {
    int min = time_;
    int max = (legacyMaxTime_ != NO_TIME) ? legacyMaxTime_ : min;
    MinMax mm = new MinMax(min, max);
    if (matchType == PertFilter.RANGE_EQUALS) {
      return (mm.equals(filterTimes));
    } else if (matchType == PertFilter.RANGE_OVERLAPS) {
      return (mm.intersect(filterTimes) != null);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Answers if this is for a single (versus double) perturbation
  */
  
  public boolean isSinglePerturbation() {
    return (sources_.isSinglePert());
  }
  
  /***************************************************************************
  **
  ** Answers if the perturbations in this set matches one of those specified
  */
  
  public boolean sourceMatch(List skeys) {
    return (sources_.sourcesMatch(skeys));
  }
  
  /***************************************************************************
  **
  ** Return if our times matches that given:
  */
  
  public boolean timeMatches(int time) {
    if (legacyMaxTime_ == NO_TIME) {
      return (time == time_);
    }
    return ((time >= time_) && (time <= legacyMaxTime_));
  } 
 
  /***************************************************************************
  **
  ** Set the time
  */
  
  public void setTime(int time) {
    time_ = time;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the legacy max time
  */
  
  public void setLegacyMaxTime(int maxTime) {
    legacyMaxTime_ = maxTime;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the investigator(s)
  */
  
  public void setInvestigators(List invest) {
    invest_.clear();
    invest_.addAll(invest);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add an investigator:
  */
  
  public void addInvestigator(String invest) {
    invest_.add(invest);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the time (may be NO_TIME if no time)
  */
  
  public int getTime() {
    return (time_);
  }
  
  /***************************************************************************
  **
  ** Get the time range
  */
  
  public MinMax getTimeRange() {
    int minTime = time_;
    int maxTime = (legacyMaxTime_ == NO_TIME) ? time_ : legacyMaxTime_;  
    return (new MinMax(minTime, maxTime));   
  }
  
  /***************************************************************************
  **
  ** Get the legacy max time (may be NO_TIME if no time)
  */
  
  public int getLegacyMaxTime() {
    return (legacyMaxTime_);
  }
 
  /***************************************************************************
  **
  ** Get the investigator(s) (may be null if none)
  */
  
  public List getInvestigators() {
    return (invest_);
  }
  
  /***************************************************************************
  **
  ** Delete an investigator
  */
  
  public void deleteInvestigator(int i) {
    invest_.remove(i);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete an investigator
  */
  
  public void deleteInvestigator(String key) {
    invest_.remove(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Replace an investigator
  */
  
  public void replaceInvestigator(int i, String invest) {
    invest_.set(i, invest);
    return;
  }
    
  /***************************************************************************
  **
  ** Get the (possibly multi-person) investigator display string
  **
  */
  
  public String getInvestigatorDisplayString(SourceSrc ss) {
    ArrayList asNames = new ArrayList();
    int numSrc = invest_.size();
    for (int i = 0; i < numSrc; i++) {
      String investKey = (String)invest_.get(i);
      String invest = ss.getInvestigator(investKey);
      asNames.add(invest);
    }
    return (DataUtil.getMultiDisplayString(asNames));
  }

  /***************************************************************************
  **
  ** Get a sorted set of investigators for comparison:
  **
  */
  
  public SortedSet getInvestigatorSortedSet(SourceSrc ss, boolean normalized) {   
    TreeSet retval = new TreeSet();
    int numInv = invest_.size();
    for (int i = 0; i < numInv; i++) {
      String investKey = (String)invest_.get(i);
      String invest = ss.getInvestigator(investKey);
      if (normalized) {
        invest = DataUtil.normKey(invest);
      }
      retval.add(invest);
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Get the experimental conditions display string
  **
  */
  
  public String getCondsDisplayString(SourceSrc ss) {
    return (ss.getConditionDictionary().getExprConditions(conditionKey_).getDisplayString());
  } 
 
  /***************************************************************************
  **
  ** Get the (possibly mult-pert) display string
  **
  */
  
  public String getPertDisplayString(SourceSrc ss, int footnoteMode) {
    return (sources_.getDisplayString(ss, footnoteMode));
  } 
  
  /***************************************************************************
  **
  ** Get the set of perturbs
  **
  */
  
  public SortedSet getPerturbsSortedSet(SourceSrc ss, boolean normalized) {
    return (sources_.getPerturbsSortedSet(ss, normalized));
  } 
 
  /***************************************************************************
  **
  ** Get the display string
  **
  */
  
  public String getTimeDisplayString(boolean showUnits, boolean abbreviate) {
    return (getTimeDisplayString(appState_, getTimeRange(), showUnits, abbreviate));
  } 

  /***************************************************************************
  **
  ** Fill the set of annotation IDs used by this set
  */
  
  public void getAnnotationIDs(Set usedIDs, SourceSrc ss) {
    sources_.getAnnotationIDs(usedIDs, ss);
    return;
  }
  
  /***************************************************************************
  **
  ** Write the result set to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();  
    out.print("<experiment id=\"");
    out.print(id_);
    out.print("\"");
    if (time_ != NO_TIME) {
      out.print(" time=\"");
      out.print(time_);
      out.print("\"");
    }
    
    if (legacyMaxTime_ != NO_TIME) {
      out.print(" legMax=\"");
      out.print(legacyMaxTime_);
      out.print("\"");
    }   
    
    out.print(" cond=\"");
    out.print(conditionKey_);
    out.print("\"");
 
    out.print(" srcs=\"");
    out.print(sources_.sourcesAsString());
    out.print("\"");
    
    int numInvest = invest_.size();
    if (numInvest != 0) {    
      out.print(" invests=\"");
      out.print(Splitter.tokenJoin(invest_, ","));
      out.print("\"");   
    }
    
    out.println("/>");
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Get a display string
  */
  
  public static String getDisplayString(String perts, String times, String invest) {
    StringBuffer buf = new StringBuffer();
    buf.append(perts);
    buf.append(" @ ");
    buf.append(times);
    if (!invest.trim().equals("")) {
      buf.append(" (");
      buf.append(invest);
      buf.append(")");
    }
    return (buf.toString());
  }  
    
  /***************************************************************************
  **
  ** Get the display string
  **
  */
  
  public static String getTimeDisplayString(BTState appState, MinMax mm, boolean showUnits, boolean abbreviate) {
    if (mm == null) {
      return ("");
    }
    String td = PerturbationData.getTimeDisplay(appState, new Integer(mm.min), showUnits, abbreviate);
    if (mm.max == mm.min) {
      return (td);
    }
    String ltd = PerturbationData.getTimeDisplay(appState, new Integer(mm.max), showUnits, abbreviate);
    return (td + " to " + ltd); // FIX ME: resource manager & format
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("Experiment: perturbs = " + sources_.toString() + " time = " + time_ +
                             " invest = " + invest_);
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */  
      
  public static class PertSourcesInfoWorker extends AbstractFactoryClient {
   
    private BTState appState_;
    
    public PertSourcesInfoWorker(BTState appState, FactoryWhiteboard whiteboard) {
      super(whiteboard);
      appState_ = appState;
      myKeys_.add("experiment");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("experiment")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertSrcInfo = buildFromXML(elemName, attrs);
        retval = board.pertSrcInfo;
      }
      return (retval);     
    }  
        
    private Experiment buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "id", true); 
      String time = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "time", false);      
      String legMax = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "legMax", false);
      String cond = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "cond", true); 
      String srcs = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "srcs", true);      
      String invests = AttributeExtractor.extractAttribute(elemName, attrs, "experiment", "invests", false);      
      
      if (id == null) {
        throw new IOException();
      }
      
      int timeNum = NO_TIME;
      int legMaxNum = NO_TIME;

      try {
        if (time != null) {
          timeNum = Integer.parseInt(time); 
        }
        if (legMax != null) {
          legMaxNum = Integer.parseInt(legMax); 
        }
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      Experiment retval = new Experiment(appState_, id, timeNum, legMaxNum, cond);
      
      // Add sources:
      PertSources pss = new PertSources(appState_);
      List srcList = Splitter.stringBreak(srcs, ",", 0, false);
      int numSrc = srcList.size();
      for (int i = 0; i < numSrc; i++){
        String srcID = (String)srcList.get(i);
        pss.addSourceID(srcID);
      }
      retval.setSources(pss);
      
      // Add investigators:
      if (invests != null) {
        List invList = Splitter.stringBreak(invests, ",", 0, false);
        int numI = invList.size();
        for (int i = 0; i < numI; i++){
          String invID = (String)invList.get(i);
          retval.addInvestigator(invID);
        }
      }
      return (retval);
    } 
  }
}
